      *HEADER,COBOL,NC171A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1714.2
000200 PROGRAM-ID.                                                      NC1714.2
000300     NC171A.                                                      NC1714.2
000400****************************************************************  NC1714.2
000500*                                                              *  NC1714.2
000600*    VALIDATION FOR:-                                          *  NC1714.2
000700*                                                              *  NC1714.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1714.2
000900*                                                              *  NC1714.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1714.2
001100*                                                              *  NC1714.2
001200****************************************************************  NC1714.2
001300*                                                              *  NC1714.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1714.2
001500*                                                              *  NC1714.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1714.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1714.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1714.2
001900*                                                              *  NC1714.2
002000****************************************************************  NC1714.2
002100*    THIS PROGRAM TESTS THE FORMAT 1 DIVIDE STATEMENT FOUND       NC1714.2
002200*    IN LEVEL 1.   ALL COMBINATIONS OF THE OPTIONAL PHRASES       NC1714.2
002300*    "SIZE ERROR", "NOT SIZE ERROR" AND "END-MULTIPLY" ARE        NC1714.2
002400*    TESTED, AS WELL AS THE ROUNDED OPTION.                       NC1714.2
002500*                                                                 NC1714.2
002600*    VARIOUS COMBINATIONS OF SIGNED AND UNSIGNED NUMERIC          NC1714.2
002700*    LITERALS, DISPLAY AND COMPUTATIONAL FIELDS ARE USED          NC1714.2
002800*    AS OPERANDS.                                                 NC1714.2
002900*                                                                 NC1714.2
003000                                                                  NC1714.2
003100                                                                  NC1714.2
003200 ENVIRONMENT DIVISION.                                            NC1714.2
003300 CONFIGURATION SECTION.                                           NC1714.2
003400 SOURCE-COMPUTER.                                                 NC1714.2
003500     XXXXX082.                                                    NC1714.2
003600 OBJECT-COMPUTER.                                                 NC1714.2
003700     XXXXX083.                                                    NC1714.2
003800 INPUT-OUTPUT SECTION.                                            NC1714.2
003900 FILE-CONTROL.                                                    NC1714.2
004000     SELECT PRINT-FILE ASSIGN TO                                  NC1714.2
004100     XXXXX055.                                                    NC1714.2
004200 DATA DIVISION.                                                   NC1714.2
004300 FILE SECTION.                                                    NC1714.2
004400 FD  PRINT-FILE.                                                  NC1714.2
004500 01  PRINT-REC PICTURE X(120).                                    NC1714.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 NC1714.2
004700 WORKING-STORAGE SECTION.                                         NC1714.2
004800 77  WRK-DS-18V00                PICTURE S9(18).                  NC1714.2
004900 77  A06THREES-DS-03V03          PICTURE S999V999 VALUE 333.333.  NC1714.2
005000 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC1714.2
005100 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06 PICTURE S9(12).        NC1714.2
005200 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1714.2
005300 77  WRK-DS-10V00                PICTURE S9(10).                  NC1714.2
005400 77  WRK-XN-00001                PICTURE X.                       NC1714.2
005500 77  A10ONES-DS-10V00            PICTURE S9(10)                   NC1714.2
005600                                 VALUE 1111111111.                NC1714.2
005700 77  A12THREES-DS-06V06          PICTURE S9(6)V9(6)               NC1714.2
005800                                 VALUE 333333.333333.             NC1714.2
005900 77  WRK-DS-02V00                PICTURE S99.                     NC1714.2
006000 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1714.2
006100 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1714.2
006200 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1714.2
006300 77  A05ONES-DS-00V05            PICTURE SV9(5) VALUE .11111.     NC1714.2
006400 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1714.2
006500                                 VALUE 111111111111.              NC1714.2
006600 77  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1714.2
006700 77  WRK-DS-09V08                PICTURE S9(9)V9(8).              NC1714.2
006800 77  WRK-DS-17V00-S REDEFINES WRK-DS-09V08 PICTURE S9(17).        NC1714.2
006900 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1714.2
007000                                 VALUE 111111111111111111.        NC1714.2
007100 77  WRK-DS-0201P                PICTURE S99P.                    NC1714.2
007200 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1714.2
007300 77  WRK-DU-18V00                PICTURE 9(18).                   NC1714.2
007400 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1714.2
007500                                 VALUE 99.                        NC1714.2
007600 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1714.2
007700                                 VALUE .1.                        NC1714.2
007800 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1714.2
007900 77  WRK-DS-12V00                PICTURE S9(12).                  NC1714.2
008000 77  WRK-DS-01V00                PICTURE S9.                      NC1714.2
008100 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1714.2
008200 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1714.2
008300                                 VALUE 111111111.111111111.       NC1714.2
008400 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1714.2
008500 77  WRK-DS-05V00                PICTURE S9(5).                   NC1714.2
008600 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1714.2
008700 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1714.2
008800 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1714.2
008900 77  XRAY                        PICTURE X.                       NC1714.2
009000 01  WRK-XN-18-1 PIC X(18).                                       NC1714.2
009100 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1714.2
009200 01  WRK-DU-X-18V0-1; REDEFINES WRK-XN-18-1 PIC 9(18).            NC1714.2
009300 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1714.2
009400 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1714.2
009500 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1714.2
009600 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1714.2
009700 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1714.2
009800 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1714.2
009900 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1714.2
010000 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1714.2
010100 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1714.2
010200 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1714.2
010300 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1714.2
010400 01  WRK-DU-2V0-1 PIC 99.                                         NC1714.2
010500 01  WRK-DU-2V0-2 PIC 99.                                         NC1714.2
010600 01  WRK-DU-2V0-3 PIC 99.                                         NC1714.2
010700 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1714.2
010800 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1714.2
010900 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1714.2
011000 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1714.2
011100 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1714.2
011200 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1714.2
011300 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1714.2
011400 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1714.2
011500 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1714.2
011600 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1714.2
011700 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1714.2
011800 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1714.2
011900 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1714.2
012000 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1714.2
012100 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1714.2
012200 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1714.2
012300 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1714.2
012400 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1714.2
012500 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1714.2
012600 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1714.2
012700 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1714.2
012800 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1714.2
012900 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1714.2
013000 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1714.2
013100 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1714.2
013200 01  WRK-NE-2 PIC $**.99.                                         NC1714.2
013300 01  WRK-NE-3 PIC $99.99CR.                                       NC1714.2
013400 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1714.2
013500 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1714.2
013600             VALUE +000000000000000001.                           NC1714.2
013700 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1714.2
013800             VALUE -000000000000000033.                           NC1714.2
013900 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1714.2
014000             VALUE 666666666666666666.                            NC1714.2
014100 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1714.2
014200             VALUE 009999999999999999.                            NC1714.2
014300 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1714.2
014400             VALUE 000022222222222222.                            NC1714.2
014500 01  MULTIPLY-DATA.                                               NC1714.2
014600     02 MULT1                           PICTURE IS 999V99         NC1714.2
014700     VALUE IS 80.12.                                              NC1714.2
014800     02 MULT2                           PICTURE IS 999V999.       NC1714.2
014900     02 MULT3                           PICTURE IS $$99.99.       NC1714.2
015000     02 MULT4                           PICTURE IS S99            NC1714.2
015100     VALUE IS -56.                                                NC1714.2
015200     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1714.2
015300     02 MULT6                           PICTURE IS 99 VALUE IS    NC1714.2
015400     20.                                                          NC1714.2
015500 01  DIVIDE-DATA.                                                 NC1714.2
015600     02 DIV1                            PICTURE IS 9(4)V99        NC1714.2
015700     VALUE IS 1620.36.                                            NC1714.2
015800     02 DIV2                            PICTURE IS 99V9           NC1714.2
015900     VALUE IS 44.1.                                               NC1714.2
016000     02 DIV3                            PICTURE IS 9(4)V9         NC1714.2
016100     VALUE IS 1661.7.                                             NC1714.2
016200     02 DIV4                            PICTURE IS S9V999         NC1714.2
016300     VALUE IS -9.642.                                             NC1714.2
016400     02 DIV-02LEVEL-1.                                            NC1714.2
016500     03 DIV5                            PICTURE IS V99            NC1714.2
016600     VALUE IS .82.                                                NC1714.2
016700     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1714.2
016800     03 DIV7                            PICTURE IS 9V9            NC1714.2
016900     VALUE IS 9.6.                                                NC1714.2
017000 01  DIV-DATA-2.                                                  NC1714.2
017100     02 DIV8                            PICTURE IS 99V9.          NC1714.2
017200     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1714.2
017300     02 DIV10                           PICTURE IS V999.          NC1714.2
017400 01  TEST-RESULTS.                                                NC1714.2
017500     02 FILLER                   PIC X      VALUE SPACE.          NC1714.2
017600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1714.2
017700     02 FILLER                   PIC X      VALUE SPACE.          NC1714.2
017800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1714.2
017900     02 FILLER                   PIC X      VALUE SPACE.          NC1714.2
018000     02  PAR-NAME.                                                NC1714.2
018100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1714.2
018200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1714.2
018300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1714.2
018400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1714.2
018500     02 RE-MARK                  PIC X(61).                       NC1714.2
018600 01  TEST-COMPUTED.                                               NC1714.2
018700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1714.2
018800     02 FILLER                   PIC X(17)  VALUE                 NC1714.2
018900            "       COMPUTED=".                                   NC1714.2
019000     02 COMPUTED-X.                                               NC1714.2
019100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1714.2
019200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1714.2
019300                                 PIC -9(9).9(9).                  NC1714.2
019400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1714.2
019500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1714.2
019600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1714.2
019700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1714.2
019800         04 COMPUTED-18V0                    PIC -9(18).          NC1714.2
019900         04 FILLER                           PIC X.               NC1714.2
020000     03 FILLER PIC X(50) VALUE SPACE.                             NC1714.2
020100 01  TEST-CORRECT.                                                NC1714.2
020200     02 FILLER PIC X(30) VALUE SPACE.                             NC1714.2
020300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1714.2
020400     02 CORRECT-X.                                                NC1714.2
020500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1714.2
020600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1714.2
020700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1714.2
020800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1714.2
020900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1714.2
021000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1714.2
021100         04 CORRECT-18V0                     PIC -9(18).          NC1714.2
021200         04 FILLER                           PIC X.               NC1714.2
021300     03 FILLER PIC X(2) VALUE SPACE.                              NC1714.2
021400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1714.2
021500 01  CCVS-C-1.                                                    NC1714.2
021600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1714.2
021700-    "SS  PARAGRAPH-NAME                                          NC1714.2
021800-    "       REMARKS".                                            NC1714.2
021900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1714.2
022000 01  CCVS-C-2.                                                    NC1714.2
022100     02 FILLER                     PIC X        VALUE SPACE.      NC1714.2
022200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1714.2
022300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1714.2
022400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1714.2
022500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1714.2
022600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1714.2
022700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1714.2
022800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1714.2
022900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1714.2
023000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1714.2
023100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1714.2
023200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1714.2
023300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1714.2
023400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1714.2
023500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1714.2
023600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1714.2
023700 01  CCVS-H-1.                                                    NC1714.2
023800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1714.2
023900     02  FILLER                    PIC X(42)    VALUE             NC1714.2
024000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1714.2
024100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1714.2
024200 01  CCVS-H-2A.                                                   NC1714.2
024300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1714.2
024400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1714.2
024500   02  FILLER                        PIC XXXX   VALUE             NC1714.2
024600     "4.2 ".                                                      NC1714.2
024700   02  FILLER                        PIC X(28)  VALUE             NC1714.2
024800            " COPY - NOT FOR DISTRIBUTION".                       NC1714.2
024900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1714.2
025000                                                                  NC1714.2
025100 01  CCVS-H-2B.                                                   NC1714.2
025200   02  FILLER                        PIC X(15)  VALUE             NC1714.2
025300            "TEST RESULT OF ".                                    NC1714.2
025400   02  TEST-ID                       PIC X(9).                    NC1714.2
025500   02  FILLER                        PIC X(4)   VALUE             NC1714.2
025600            " IN ".                                               NC1714.2
025700   02  FILLER                        PIC X(12)  VALUE             NC1714.2
025800     " HIGH       ".                                              NC1714.2
025900   02  FILLER                        PIC X(22)  VALUE             NC1714.2
026000            " LEVEL VALIDATION FOR ".                             NC1714.2
026100   02  FILLER                        PIC X(58)  VALUE             NC1714.2
026200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1714.2
026300 01  CCVS-H-3.                                                    NC1714.2
026400     02  FILLER                      PIC X(34)  VALUE             NC1714.2
026500            " FOR OFFICIAL USE ONLY    ".                         NC1714.2
026600     02  FILLER                      PIC X(58)  VALUE             NC1714.2
026700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1714.2
026800     02  FILLER                      PIC X(28)  VALUE             NC1714.2
026900            "  COPYRIGHT   1985 ".                                NC1714.2
027000 01  CCVS-E-1.                                                    NC1714.2
027100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1714.2
027200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1714.2
027300     02 ID-AGAIN                     PIC X(9).                    NC1714.2
027400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1714.2
027500 01  CCVS-E-2.                                                    NC1714.2
027600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1714.2
027700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1714.2
027800     02 CCVS-E-2-2.                                               NC1714.2
027900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1714.2
028000         03 FILLER                   PIC X      VALUE SPACE.      NC1714.2
028100         03 ENDER-DESC               PIC X(44)  VALUE             NC1714.2
028200            "ERRORS ENCOUNTERED".                                 NC1714.2
028300 01  CCVS-E-3.                                                    NC1714.2
028400     02  FILLER                      PIC X(22)  VALUE             NC1714.2
028500            " FOR OFFICIAL USE ONLY".                             NC1714.2
028600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1714.2
028700     02  FILLER                      PIC X(58)  VALUE             NC1714.2
028800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1714.2
028900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1714.2
029000     02 FILLER                       PIC X(15)  VALUE             NC1714.2
029100             " COPYRIGHT 1985".                                   NC1714.2
029200 01  CCVS-E-4.                                                    NC1714.2
029300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1714.2
029400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1714.2
029500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1714.2
029600     02 FILLER                       PIC X(40)  VALUE             NC1714.2
029700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1714.2
029800 01  XXINFO.                                                      NC1714.2
029900     02 FILLER                       PIC X(19)  VALUE             NC1714.2
030000            "*** INFORMATION ***".                                NC1714.2
030100     02 INFO-TEXT.                                                NC1714.2
030200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1714.2
030300       04 XXCOMPUTED                 PIC X(20).                   NC1714.2
030400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1714.2
030500       04 XXCORRECT                  PIC X(20).                   NC1714.2
030600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1714.2
030700 01  HYPHEN-LINE.                                                 NC1714.2
030800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1714.2
030900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1714.2
031000-    "*****************************************".                 NC1714.2
031100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1714.2
031200-    "******************************".                            NC1714.2
031300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1714.2
031400     "NC171A".                                                    NC1714.2
031500 PROCEDURE DIVISION.                                              NC1714.2
031600 CCVS1 SECTION.                                                   NC1714.2
031700 OPEN-FILES.                                                      NC1714.2
031800     OPEN     OUTPUT PRINT-FILE.                                  NC1714.2
031900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1714.2
032000     MOVE    SPACE TO TEST-RESULTS.                               NC1714.2
032100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1714.2
032200     GO TO CCVS1-EXIT.                                            NC1714.2
032300 CLOSE-FILES.                                                     NC1714.2
032400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1714.2
032500 TERMINATE-CCVS.                                                  NC1714.2
032600*    EXIT PROGRAM.                                                NC1714.2
032700*TERMINATE-CALL.                                                  NC1714.2
032800     STOP     RUN.                                                NC1714.2
032900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1714.2
033000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1714.2
033100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1714.2
033200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1714.2
033300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1714.2
033400 PRINT-DETAIL.                                                    NC1714.2
033500     IF REC-CT NOT EQUAL TO ZERO                                  NC1714.2
033600             MOVE "." TO PARDOT-X                                 NC1714.2
033700             MOVE REC-CT TO DOTVALUE.                             NC1714.2
033800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1714.2
033900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1714.2
034000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1714.2
034100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1714.2
034200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1714.2
034300     MOVE SPACE TO CORRECT-X.                                     NC1714.2
034400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1714.2
034500     MOVE     SPACE TO RE-MARK.                                   NC1714.2
034600 HEAD-ROUTINE.                                                    NC1714.2
034700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1714.2
034800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1714.2
034900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1714.2
035000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1714.2
035100 COLUMN-NAMES-ROUTINE.                                            NC1714.2
035200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1714.2
035300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1714.2
035400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1714.2
035500 END-ROUTINE.                                                     NC1714.2
035600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1714.2
035700 END-RTN-EXIT.                                                    NC1714.2
035800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1714.2
035900 END-ROUTINE-1.                                                   NC1714.2
036000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1714.2
036100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1714.2
036200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1714.2
036300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1714.2
036400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1714.2
036500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1714.2
036600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1714.2
036700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1714.2
036800  END-ROUTINE-12.                                                 NC1714.2
036900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1714.2
037000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1714.2
037100         MOVE "NO " TO ERROR-TOTAL                                NC1714.2
037200         ELSE                                                     NC1714.2
037300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1714.2
037400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1714.2
037500     PERFORM WRITE-LINE.                                          NC1714.2
037600 END-ROUTINE-13.                                                  NC1714.2
037700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1714.2
037800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1714.2
037900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1714.2
038000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1714.2
038100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1714.2
038200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1714.2
038300          MOVE "NO " TO ERROR-TOTAL                               NC1714.2
038400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1714.2
038500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1714.2
038600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1714.2
038700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1714.2
038800 WRITE-LINE.                                                      NC1714.2
038900     ADD 1 TO RECORD-COUNT.                                       NC1714.2
039000*    IF RECORD-COUNT GREATER 42                                   NC1714.2
039100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1714.2
039200*        MOVE SPACE TO DUMMY-RECORD                               NC1714.2
039300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1714.2
039400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1714.2
039500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1714.2
039600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1714.2
039700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1714.2
039800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1714.2
039900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1714.2
040000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1714.2
040100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1714.2
040200*        MOVE ZERO TO RECORD-COUNT.                               NC1714.2
040300     PERFORM WRT-LN.                                              NC1714.2
040400 WRT-LN.                                                          NC1714.2
040500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1714.2
040600     MOVE SPACE TO DUMMY-RECORD.                                  NC1714.2
040700 BLANK-LINE-PRINT.                                                NC1714.2
040800     PERFORM WRT-LN.                                              NC1714.2
040900 FAIL-ROUTINE.                                                    NC1714.2
041000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1714.2
041100            GO TO FAIL-ROUTINE-WRITE.                             NC1714.2
041200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1714.2
041300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1714.2
041400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1714.2
041500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1714.2
041600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1714.2
041700     GO TO  FAIL-ROUTINE-EX.                                      NC1714.2
041800 FAIL-ROUTINE-WRITE.                                              NC1714.2
041900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1714.2
042000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1714.2
042100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1714.2
042200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1714.2
042300 FAIL-ROUTINE-EX. EXIT.                                           NC1714.2
042400 BAIL-OUT.                                                        NC1714.2
042500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1714.2
042600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1714.2
042700 BAIL-OUT-WRITE.                                                  NC1714.2
042800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1714.2
042900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1714.2
043000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1714.2
043100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1714.2
043200 BAIL-OUT-EX. EXIT.                                               NC1714.2
043300 CCVS1-EXIT.                                                      NC1714.2
043400     EXIT.                                                        NC1714.2
043500 SECT-NC171A-001 SECTION.                                         NC1714.2
043600*                                                                 NC1714.2
043700*                                                                 NC1714.2
043800 DIV-INIT-F1-1.                                                   NC1714.2
043900     MOVE "DIVIDE INTO" TO FEATURE.                               NC1714.2
044000     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
044100     MOVE    1620.36 TO DIV1.                                     NC1714.2
044200 DIV-TEST-F1-1.                                                   NC1714.2
044300     DIVIDE   64.3 INTO DIV1.                                     NC1714.2
044400     IF       DIV1 EQUAL TO 25.2                                  NC1714.2
044500              PERFORM PASS                                        NC1714.2
044600              ELSE                                                NC1714.2
044700              GO TO DIV-FAIL-F1-1.                                NC1714.2
044800     GO TO    DIV-WRITE-F1-1.                                     NC1714.2
044900 DIV-DELETE-F1-1.                                                 NC1714.2
045000     PERFORM  DE-LETE.                                            NC1714.2
045100     GO TO    DIV-WRITE-F1-1.                                     NC1714.2
045200 DIV-FAIL-F1-1.                                                   NC1714.2
045300     PERFORM  FAIL.                                               NC1714.2
045400     MOVE     DIV1 TO COMPUTED-N.                                 NC1714.2
045500     MOVE     +25.2 TO CORRECT-N.                                 NC1714.2
045600 DIV-WRITE-F1-1.                                                  NC1714.2
045700     MOVE "DIV-TEST-F1-1" TO PAR-NAME.                            NC1714.2
045800     PERFORM  PRINT-DETAIL.                                       NC1714.2
045900 DIV-INIT-F1-2.                                                   NC1714.2
046000     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
046100     MOVE      44.1  TO DIV2.                                     NC1714.2
046200     MOVE    1661.7  TO DIV3.                                     NC1714.2
046300 DIV-TEST-F1-2.                                                   NC1714.2
046400     DIVIDE   DIV2 INTO DIV3 ROUNDED.                             NC1714.2
046500     IF       DIV3 EQUAL TO 37.7                                  NC1714.2
046600              PERFORM PASS                                        NC1714.2
046700              ELSE                                                NC1714.2
046800              GO TO DIV-FAIL-F1-2.                                NC1714.2
046900     GO TO    DIV-WRITE-F1-2.                                     NC1714.2
047000 DIV-DELETE-F1-2.                                                 NC1714.2
047100     PERFORM  DE-LETE.                                            NC1714.2
047200     GO TO    DIV-WRITE-F1-2.                                     NC1714.2
047300 DIV-FAIL-F1-2.                                                   NC1714.2
047400     PERFORM  FAIL.                                               NC1714.2
047500     MOVE     DIV3 TO COMPUTED-N.                                 NC1714.2
047600     MOVE     +37.7 TO CORRECT-N.                                 NC1714.2
047700 DIV-WRITE-F1-2.                                                  NC1714.2
047800     MOVE "DIV-TEST-F1-2 " TO PAR-NAME.                           NC1714.2
047900     PERFORM  PRINT-DETAIL.                                       NC1714.2
048000 DIV-INIT-F1-3.                                                   NC1714.2
048100     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
048200     MOVE   -9.642   TO DIV4.                                     NC1714.2
048300     MOVE     .82    TO DIV5.                                     NC1714.2
048400 DIV-TEST-F1-3-1.                                                 NC1714.2
048500     DIVIDE   DIV5 INTO DIV4 ON SIZE ERROR                        NC1714.2
048600              MOVE "M" TO XRAY.                                   NC1714.2
048700     IF       XRAY EQUAL TO "M"                                   NC1714.2
048800              PERFORM PASS                                        NC1714.2
048900              ELSE                                                NC1714.2
049000              GO TO DIV-FAIL-F1-3-1.                              NC1714.2
049100     GO TO    DIV-WRITE-F1-3-1.                                   NC1714.2
049200 DIV-DELETE-F1-3-1.                                               NC1714.2
049300     PERFORM  DE-LETE.                                            NC1714.2
049400     GO TO    DIV-WRITE-F1-3-1.                                   NC1714.2
049500 DIV-FAIL-F1-3-1.                                                 NC1714.2
049600     MOVE     XRAY TO COMPUTED-X.                                 NC1714.2
049700     MOVE    "M"   TO CORRECT-X.                                  NC1714.2
049800     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1714.2
049900     PERFORM  FAIL.                                               NC1714.2
050000 DIV-WRITE-F1-3-1.                                                NC1714.2
050100     MOVE "DIV-TEST-F1-3-1 " TO PAR-NAME.                         NC1714.2
050200     PERFORM  PRINT-DETAIL.                                       NC1714.2
050300 DIV-TEST-F1-3-2.                                                 NC1714.2
050400     IF       DIV4 EQUAL TO -9.642                                NC1714.2
050500              PERFORM PASS                                        NC1714.2
050600              ELSE                                                NC1714.2
050700              GO TO DIV-FAIL-F1-3-2.                              NC1714.2
050800     GO TO    DIV-WRITE-F1-3-2.                                   NC1714.2
050900 DIV-DELETE-F1-3-2.                                               NC1714.2
051000     PERFORM  DE-LETE.                                            NC1714.2
051100     GO TO    DIV-WRITE-F1-3-2.                                   NC1714.2
051200 DIV-FAIL-F1-3-2.                                                 NC1714.2
051300     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1714.2
051400     PERFORM  FAIL.                                               NC1714.2
051500     MOVE    DIV4 TO COMPUTED-N.                                  NC1714.2
051600     MOVE    -9.642 TO CORRECT-N.                                 NC1714.2
051700 DIV-WRITE-F1-3-2.                                                NC1714.2
051800     MOVE "DIV-TEST-F1-3-2 " TO PAR-NAME.                         NC1714.2
051900     PERFORM PRINT-DETAIL.                                        NC1714.2
052000 DIV-INIT-F1-4.                                                   NC1714.2
052100     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
052200     MOVE    44.1   TO DIV2.                                      NC1714.2
052300     MOVE     0     TO DIV6.                                      NC1714.2
052400     MOVE    "A"    TO XRAY.                                      NC1714.2
052500 DIV-TEST-F1-4-0.                                                 NC1714.2
052600     DIVIDE  DIV6 INTO DIV2 ON SIZE ERROR                         NC1714.2
052700             MOVE "N" TO XRAY.                                    NC1714.2
052800 DIV-TEST-F1-4-1.                                                 NC1714.2
052900     IF      XRAY EQUAL TO "N"                                    NC1714.2
053000             PERFORM PASS                                         NC1714.2
053100     ELSE                                                         NC1714.2
053200             GO TO DIV-FAIL-F1-4-1.                               NC1714.2
053300     GO TO   DIV-WRITE-F1-4-1.                                    NC1714.2
053400 DIV-DELETE-F1-4-1.                                               NC1714.2
053500     PERFORM DE-LETE.                                             NC1714.2
053600     GO TO   DIV-WRITE-F1-4-1.                                    NC1714.2
053700 DIV-FAIL-F1-4-1.                                                 NC1714.2
053800     MOVE    XRAY TO COMPUTED-X.                                  NC1714.2
053900     MOVE   "N"   TO CORRECT-X.                                   NC1714.2
054000     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1714.2
054100     PERFORM FAIL.                                                NC1714.2
054200 DIV-WRITE-F1-4-1.                                                NC1714.2
054300     MOVE   "DIV-TEST-F1-4-1 " TO PAR-NAME.                       NC1714.2
054400     PERFORM PRINT-DETAIL.                                        NC1714.2
054500 DIV-TEST-F1-4-2.                                                 NC1714.2
054600     IF      DIV2 EQUAL TO 44.1                                   NC1714.2
054700             PERFORM PASS                                         NC1714.2
054800     ELSE                                                         NC1714.2
054900             GO TO DIV-FAIL-F1-4-2.                               NC1714.2
055000     GO TO   DIV-WRITE-F1-4-2.                                    NC1714.2
055100 DIV-DELETE-F1-4-2.                                               NC1714.2
055200     PERFORM  DE-LETE.                                            NC1714.2
055300     GO TO   DIV-WRITE-F1-4-2.                                    NC1714.2
055400 DIV-FAIL-F1-4-2.                                                 NC1714.2
055500     PERFORM  FAIL.                                               NC1714.2
055600     MOVE    DIV2 TO COMPUTED-N.                                  NC1714.2
055700     MOVE   +44.1000 TO CORRECT-N.                                NC1714.2
055800     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1714.2
055900 DIV-WRITE-F1-4-2.                                                NC1714.2
056000     MOVE   "DIV-TEST-F1-4-2 " TO PAR-NAME.                       NC1714.2
056100     PERFORM PRINT-DETAIL.                                        NC1714.2
056200 DIV-INIT-F1-5.                                                   NC1714.2
056300     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
056400     MOVE    9.6    TO DIV7.                                      NC1714.2
056500     MOVE   "B"     TO XRAY.                                      NC1714.2
056600 DIV-TEST-F1-5-1.                                                 NC1714.2
056700     DIVIDE  0.097 INTO DIV7 ROUNDED ON SIZE ERROR                NC1714.2
056800             MOVE "N" TO XRAY.                                    NC1714.2
056900     IF      XRAY EQUAL TO "N"                                    NC1714.2
057000             PERFORM PASS                                         NC1714.2
057100     ELSE                                                         NC1714.2
057200             GO TO DIV-FAIL-F1-5-1.                               NC1714.2
057300     GO TO   DIV-WRITE-F1-5-1.                                    NC1714.2
057400 DIV-DELETE-F1-5-1.                                               NC1714.2
057500     PERFORM DE-LETE.                                             NC1714.2
057600     GO TO   DIV-WRITE-F1-5-1.                                    NC1714.2
057700 DIV-FAIL-F1-5-1.                                                 NC1714.2
057800     MOVE    XRAY TO COMPUTED-X.                                  NC1714.2
057900     MOVE   "N"   TO CORRECT-X.                                   NC1714.2
058000     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1714.2
058100     PERFORM FAIL.                                                NC1714.2
058200 DIV-WRITE-F1-5-1.                                                NC1714.2
058300     MOVE "DIV-TEST-F1-5-1 " TO PAR-NAME.                         NC1714.2
058400     PERFORM PRINT-DETAIL.                                        NC1714.2
058500 DIV-TEST-F1-5-2.                                                 NC1714.2
058600     IF       DIV7 NOT EQUAL TO 9.6                               NC1714.2
058700              GO TO DIV-FAIL-F1-5-2.                              NC1714.2
058800     PERFORM  PASS.                                               NC1714.2
058900     GO TO    DIV-WRITE-F1-5-2.                                   NC1714.2
059000 DIV-DELETE-F1-5-2.                                               NC1714.2
059100     PERFORM  DE-LETE.                                            NC1714.2
059200     GO TO    DIV-WRITE-F1-5-2.                                   NC1714.2
059300 DIV-FAIL-F1-5-2.                                                 NC1714.2
059400     PERFORM  FAIL.                                               NC1714.2
059500     MOVE     DIV7 TO COMPUTED-N.                                 NC1714.2
059600     MOVE     +9.6 TO CORRECT-N.                                  NC1714.2
059700     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1714.2
059800 DIV-WRITE-F1-5-2.                                                NC1714.2
059900     MOVE "DIV-TEST-F1-5-2 " TO PAR-NAME.                         NC1714.2
060000     PERFORM  PRINT-DETAIL.                                       NC1714.2
060100 DIV-INIT-F1-6.                                                   NC1714.2
060200     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
060300     MOVE    99 TO WRK-DS-18V00.                                  NC1714.2
060400     MOVE    99 TO A99-DS-02V00.                                  NC1714.2
060500 DIV-TEST-F1-6-0.                                                 NC1714.2
060600     DIVIDE  A99-DS-02V00 INTO WRK-DS-18V00.                      NC1714.2
060700 DIV-TEST-F1-6-1.                                                 NC1714.2
060800     IF      WRK-DS-18V00 EQUAL TO 000000000000000001             NC1714.2
060900             PERFORM PASS                                         NC1714.2
061000             GO TO   DIV-WRITE-F1-6.                              NC1714.2
061100     GO TO   DIV-FAIL-F1-6.                                       NC1714.2
061200 DIV-DELETE-F1-6.                                                 NC1714.2
061300     PERFORM DE-LETE.                                             NC1714.2
061400     GO TO   DIV-WRITE-F1-6.                                      NC1714.2
061500 DIV-FAIL-F1-6.                                                   NC1714.2
061600     MOVE  000000000000000001 TO CORRECT-18V0.                    NC1714.2
061700     MOVE  WRK-DS-18V00       TO COMPUTED-18V0.                   NC1714.2
061800     PERFORM FAIL.                                                NC1714.2
061900 DIV-WRITE-F1-6.                                                  NC1714.2
062000     MOVE "DIV-TEST-F1-6         " TO PAR-NAME.                   NC1714.2
062100     PERFORM PRINT-DETAIL.                                        NC1714.2
062200 DIV-INIT-F1-7.                                                   NC1714.2
062300     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
062400     MOVE 2 TO WRK-DS-12V00.                                      NC1714.2
062500 DIV-TEST-F1-7-0.                                                 NC1714.2
062600     DIVIDE 4 INTO WRK-DS-12V00 ROUNDED.                          NC1714.2
062700 DIV-TEST-F1-7-1.                                                 NC1714.2
062800     IF WRK-DS-12V00 EQUAL TO 000000000001                        NC1714.2
062900         PERFORM PASS                                             NC1714.2
063000         GO TO DIV-WRITE-F1-7.                                    NC1714.2
063100     GO TO DIV-FAIL-F1-7.                                         NC1714.2
063200 DIV-DELETE-F1-7.                                                 NC1714.2
063300     PERFORM DE-LETE.                                             NC1714.2
063400     GO TO DIV-WRITE-F1-7.                                        NC1714.2
063500 DIV-FAIL-F1-7.                                                   NC1714.2
063600     MOVE  WRK-DS-12V00       TO COMPUTED-18V0.                   NC1714.2
063700     MOVE  000000000001       TO CORRECT-18V0.                    NC1714.2
063800     PERFORM FAIL.                                                NC1714.2
063900 DIV-WRITE-F1-7.                                                  NC1714.2
064000     MOVE "DIV-TEST-F1-7         " TO PAR-NAME.                   NC1714.2
064100     PERFORM PRINT-DETAIL.                                        NC1714.2
064200 DIV-INIT-F1-8.                                                   NC1714.2
064300     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
064400     MOVE  1 TO WRK-DS-01V00.                                     NC1714.2
064500     MOVE "0" TO WRK-XN-00001.                                    NC1714.2
064600 DIV-TEST-F1-8-0.                                                 NC1714.2
064700     DIVIDE 0.1 INTO WRK-DS-01V00 ON SIZE ERROR                   NC1714.2
064800         MOVE "1" TO WRK-XN-00001.                                NC1714.2
064900 DIV-TEST-F1-8-1.                                                 NC1714.2
065000     IF WRK-DS-01V00 EQUAL TO 1                                   NC1714.2
065100         PERFORM PASS                                             NC1714.2
065200         GO TO DIV-WRITE-F1-8-1.                                  NC1714.2
065300     GO TO DIV-FAIL-F1-8-1.                                       NC1714.2
065400 DIV-DELETE-F1-8-1.                                               NC1714.2
065500     PERFORM DE-LETE.                                             NC1714.2
065600     GO TO DIV-WRITE-F1-8-1.                                      NC1714.2
065700 DIV-FAIL-F1-8-1.                                                 NC1714.2
065800     MOVE  1                  TO CORRECT-N.                       NC1714.2
065900     MOVE  WRK-DS-01V00       TO COMPUTED-N.                      NC1714.2
066000     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1714.2
066100     PERFORM FAIL.                                                NC1714.2
066200 DIV-WRITE-F1-8-1.                                                NC1714.2
066300     MOVE "DIV-TEST-F1-8-1         " TO PAR-NAME.                 NC1714.2
066400     PERFORM PRINT-DETAIL.                                        NC1714.2
066500 DIV-TEST-F1-8-2.                                                 NC1714.2
066600     IF WRK-XN-00001 EQUAL TO "1"                                 NC1714.2
066700         PERFORM PASS                                             NC1714.2
066800         GO TO DIV-WRITE-F1-8-2.                                  NC1714.2
066900     MOVE "1" TO CORRECT-A.                                       NC1714.2
067000     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1714.2
067100     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1714.2
067200     PERFORM FAIL.                                                NC1714.2
067300     GO TO DIV-WRITE-F1-8-2.                                      NC1714.2
067400 DIV-DELETE-F1-8-2.                                               NC1714.2
067500     PERFORM DE-LETE.                                             NC1714.2
067600 DIV-WRITE-F1-8-2.                                                NC1714.2
067700     MOVE "DIV-TEST-F1-8-2         " TO PAR-NAME.                 NC1714.2
067800     PERFORM PRINT-DETAIL.                                        NC1714.2
067900 DIV-INIT-F1-9.                                                   NC1714.2
068000     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
068100     MOVE -.000000001 TO WRK-DS-09V09.                            NC1714.2
068200     MOVE "1" TO WRK-XN-00001.                                    NC1714.2
068300 DIV-TEST-F1-9-0.                                                 NC1714.2
068400     DIVIDE A01ONE-DS-P0801 INTO WRK-DS-09V09 ON SIZE ERROR       NC1714.2
068500         MOVE "0" TO WRK-XN-00001.                                NC1714.2
068600 DIV-TEST-F1-9-1.                                                 NC1714.2
068700     IF WRK-DS-18V00-S EQUAL TO -000000001000000000               NC1714.2
068800         PERFORM PASS                                             NC1714.2
068900         GO TO DIV-WRITE-F1-9-1.                                  NC1714.2
069000     GO TO DIV-FAIL-F1-9-1.                                       NC1714.2
069100 DIV-DELETE-F1-9-1.                                               NC1714.2
069200     PERFORM DE-LETE.                                             NC1714.2
069300     GO TO DIV-WRITE-F1-9-1.                                      NC1714.2
069400 DIV-FAIL-F1-9-1.                                                 NC1714.2
069500     MOVE -000000001000000000 TO CORRECT-18V0.                    NC1714.2
069600     MOVE WRK-DS-18V00-S TO COMPUTED-18V0.                        NC1714.2
069700     PERFORM FAIL.                                                NC1714.2
069800 DIV-WRITE-F1-9-1.                                                NC1714.2
069900     MOVE "DIV-TEST-F1-9-1         " TO PAR-NAME.                 NC1714.2
070000     PERFORM PRINT-DETAIL.                                        NC1714.2
070100 DIV-TEST-F1-9-2.                                                 NC1714.2
070200     IF WRK-XN-00001 EQUAL TO "0"                                 NC1714.2
070300         MOVE "1" TO CORRECT-A                                    NC1714.2
070400         MOVE "0" TO COMPUTED-A                                   NC1714.2
070500         MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK NC1714.2
070600         PERFORM FAIL                                             NC1714.2
070700         GO TO DIV-WRITE-F1-9-2.                                  NC1714.2
070800     PERFORM PASS.                                                NC1714.2
070900     GO TO DIV-WRITE-F1-9-2.                                      NC1714.2
071000 DIV-DELETE-F1-9-2.                                               NC1714.2
071100     PERFORM DE-LETE.                                             NC1714.2
071200 DIV-WRITE-F1-9-2.                                                NC1714.2
071300     MOVE "DIV-TEST-F1-9-2         " TO PAR-NAME.                 NC1714.2
071400     PERFORM PRINT-DETAIL.                                        NC1714.2
071500 DIV-INIT-F1-10.                                                  NC1714.2
071600     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
071700     MOVE ZERO TO WRK-DS-01V00  AZERO-DS-05V05.                   NC1714.2
071800     MOVE "0" TO WRK-XN-00001.                                    NC1714.2
071900 DIV-TEST-F1-10-0.                                                NC1714.2
072000     DIVIDE AZERO-DS-05V05 INTO WRK-DS-01V00 ROUNDED              NC1714.2
072100         ON SIZE ERROR  MOVE "1" TO WRK-XN-00001.                 NC1714.2
072200 DIV-TEST-F1-10-1.                                                NC1714.2
072300     IF WRK-DS-01V00 EQUAL TO 0                                   NC1714.2
072400         PERFORM PASS                                             NC1714.2
072500         GO TO DIV-WRITE-F1-10-1.                                 NC1714.2
072600     GO TO DIV-FAIL-F1-10-1.                                      NC1714.2
072700 DIV-DELETE-F1-10-1.                                              NC1714.2
072800     PERFORM DE-LETE.                                             NC1714.2
072900     GO TO DIV-WRITE-F1-10-1.                                     NC1714.2
073000 DIV-FAIL-F1-10-1.                                                NC1714.2
073100     MOVE  0                  TO CORRECT-N.                       NC1714.2
073200     MOVE  WRK-DS-01V00       TO COMPUTED-N.                      NC1714.2
073300     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1714.2
073400     PERFORM FAIL.                                                NC1714.2
073500 DIV-WRITE-F1-10-1.                                               NC1714.2
073600     MOVE "DIV-TEST-F1-10-1         " TO PAR-NAME.                NC1714.2
073700     PERFORM PRINT-DETAIL.                                        NC1714.2
073800 DIV-TEST-F1-10-2.                                                NC1714.2
073900     IF WRK-XN-00001 EQUAL TO "1"                                 NC1714.2
074000         PERFORM PASS                                             NC1714.2
074100         GO TO DIV-WRITE-F1-10-2.                                 NC1714.2
074200 DIV-FAIL-F1-10-2.                                                NC1714.2
074300     MOVE "1" TO CORRECT-A.                                       NC1714.2
074400     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1714.2
074500     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1714.2
074600     PERFORM FAIL.                                                NC1714.2
074700     GO TO DIV-WRITE-F1-10-2.                                     NC1714.2
074800 DIV-DELETE-F1-10-2.                                              NC1714.2
074900     PERFORM DE-LETE.                                             NC1714.2
075000 DIV-WRITE-F1-10-2.                                               NC1714.2
075100     MOVE "DIV-TEST-F1-10-2         " TO PAR-NAME.                NC1714.2
075200     PERFORM PRINT-DETAIL.                                        NC1714.2
075300 DIV-INIT-F1-11.                                                  NC1714.2
075400     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
075500     MOVE A18ONES-DS-09V09 TO WRK-DS-09V09.                       NC1714.2
075600     MOVE "1" TO WRK-XN-00001.                                    NC1714.2
075700 DIV-TEST-F1-11-0.                                                NC1714.2
075800     DIVIDE A18ONES-DS-09V09 INTO WRK-DS-09V09 ROUNDED            NC1714.2
075900         ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                  NC1714.2
076000 DIV-TEST-F1-11-1.                                                NC1714.2
076100     IF WRK-DS-18V00-S EQUAL TO 000000001000000000                NC1714.2
076200         PERFORM PASS                                             NC1714.2
076300         GO TO DIV-WRITE-F1-11-1.                                 NC1714.2
076400     GO TO DIV-FAIL-F1-11-1.                                      NC1714.2
076500 DIV-DELETE-F1-11-1.                                              NC1714.2
076600     PERFORM DE-LETE.                                             NC1714.2
076700     GO TO DIV-WRITE-F1-11-1.                                     NC1714.2
076800 DIV-FAIL-F1-11-1.                                                NC1714.2
076900     MOVE  000000001000000000 TO CORRECT-18V0.                    NC1714.2
077000     MOVE  WRK-DS-18V00       TO COMPUTED-18V0.                   NC1714.2
077100     PERFORM FAIL.                                                NC1714.2
077200 DIV-WRITE-F1-11-1.                                               NC1714.2
077300     MOVE "DIV-TEST-F1-11-1         " TO PAR-NAME.                NC1714.2
077400     PERFORM PRINT-DETAIL.                                        NC1714.2
077500 DIV-TEST-F1-11-2.                                                NC1714.2
077600     IF WRK-XN-00001 EQUAL TO "0"                                 NC1714.2
077700         MOVE WRK-XN-00001 TO COMPUTED-A                          NC1714.2
077800         MOVE "1" TO CORRECT-A                                    NC1714.2
077900         MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1714.2
078000         PERFORM FAIL                                             NC1714.2
078100         GO TO DIV-WRITE-F1-11-2.                                 NC1714.2
078200     PERFORM PASS.                                                NC1714.2
078300     GO TO DIV-WRITE-F1-11-2.                                     NC1714.2
078400 DIV-DELETE-F1-11-2.                                              NC1714.2
078500     PERFORM DE-LETE.                                             NC1714.2
078600 DIV-WRITE-F1-11-2.                                               NC1714.2
078700     MOVE "DIV-TEST-F1-11-2         " TO PAR-NAME.                NC1714.2
078800     PERFORM PRINT-DETAIL.                                        NC1714.2
078900 DIV-INIT-F1-12.                                                  NC1714.2
079000     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
079100     MOVE -99 TO WRK-DS-02V00.                                    NC1714.2
079200 DIV-TEST-F1-12-0.                                                NC1714.2
079300     DIVIDE A99-DS-02V00 INTO WRK-DS-02V00.                       NC1714.2
079400 DIV-TEST-F1-12-1.                                                NC1714.2
079500     IF WRK-DS-02V00 EQUAL TO -01                                 NC1714.2
079600         PERFORM PASS                                             NC1714.2
079700         GO TO DIV-WRITE-F1-12.                                   NC1714.2
079800 DIV-FAIL-F1-12.                                                  NC1714.2
079900     MOVE  -01                TO CORRECT-N.                       NC1714.2
080000     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1714.2
080100     PERFORM FAIL.                                                NC1714.2
080200     GO TO DIV-WRITE-F1-12.                                       NC1714.2
080300 DIV-DELETE-F1-12.                                                NC1714.2
080400     PERFORM DE-LETE.                                             NC1714.2
080500 DIV-WRITE-F1-12.                                                 NC1714.2
080600     MOVE "DIV-TEST-F1-12         " TO PAR-NAME.                  NC1714.2
080700     PERFORM PRINT-DETAIL.                                        NC1714.2
080800 DIV-INIT-F1-13.                                                  NC1714.2
080900     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
081000     MOVE -99.00 TO WRK-CS-02V02.                                 NC1714.2
081100 DIV-TEST-F1-13-0.                                                NC1714.2
081200     DIVIDE A990-DS-0201P INTO WRK-CS-02V02.                      NC1714.2
081300 DIV-TEST-F1-13-1.                                                NC1714.2
081400     MOVE WRK-CS-02V02 TO WRK-DS-06V06.                           NC1714.2
081500     IF WRK-DS-12V00-S EQUAL TO -000000100000                     NC1714.2
081600         PERFORM PASS                                             NC1714.2
081700     GO TO DIV-WRITE-F1-13.                                       NC1714.2
081800     MOVE -000000.100000 TO CORRECT-N.                            NC1714.2
081900     MOVE WRK-DS-06V06 TO COMPUTED-N.                             NC1714.2
082000     PERFORM FAIL.                                                NC1714.2
082100         GO TO DIV-WRITE-F1-13.                                   NC1714.2
082200 DIV-DELETE-F1-13.                                                NC1714.2
082300     PERFORM DE-LETE.                                             NC1714.2
082400 DIV-WRITE-F1-13.                                                 NC1714.2
082500     MOVE "DIV-TEST-F1-13         " TO PAR-NAME.                  NC1714.2
082600     PERFORM PRINT-DETAIL.                                        NC1714.2
082700*                                                                 NC1714.2
082800*                                                                 NC1714.2
082900 DIV-INIT-F1-14.                                                  NC1714.2
083000*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1714.2
083100     MOVE   "V1-67 6.4.2"  TO ANSI-REFERENCE.                     NC1714.2
083200     MOVE   "DIV-TEST-F1-14         " TO PAR-NAME                 NC1714.2
083300     MOVE   "Z" TO XRAY.                                          NC1714.2
083400     MOVE    1  TO REC-CT.                                        NC1714.2
083500     MOVE    1620.36 TO DIV1.                                     NC1714.2
083600     MOVE    44.1    TO DIV2.                                     NC1714.2
083700 DIV-TEST-F1-14-0.                                                NC1714.2
083800     DIVIDE  DIV2 INTO DIV1                                       NC1714.2
083900      NOT ON SIZE ERROR                                           NC1714.2
084000             MOVE "N" TO XRAY.                                    NC1714.2
084100     GO TO   DIV-TEST-F1-14-1.                                    NC1714.2
084200 DIV-DELETE-F1-14-1.                                              NC1714.2
084300     PERFORM DE-LETE.                                             NC1714.2
084400     PERFORM PRINT-DETAIL.                                        NC1714.2
084500     GO TO   DIV-INIT-F1-15.                                      NC1714.2
084600 DIV-TEST-F1-14-1.                                                NC1714.2
084700     MOVE   "DIV-TEST-F1-14-1 " TO PAR-NAME.                      NC1714.2
084800     IF      DIV1    = 36.74                                      NC1714.2
084900             PERFORM PASS                                         NC1714.2
085000             PERFORM PRINT-DETAIL                                 NC1714.2
085100     ELSE                                                         NC1714.2
085200             MOVE    DIV1 TO COMPUTED-N                           NC1714.2
085300             MOVE    36.74 TO CORRECT-N                           NC1714.2
085400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
085500             PERFORM FAIL                                         NC1714.2
085600             PERFORM PRINT-DETAIL.                                NC1714.2
085700     ADD     1  TO REC-CT.                                        NC1714.2
085800 DIV-TEST-F1-14-2.                                                NC1714.2
085900     MOVE   "DIV-TEST-F1-14-2 " TO PAR-NAME.                      NC1714.2
086000     IF      XRAY    = "N"                                        NC1714.2
086100             PERFORM PASS                                         NC1714.2
086200             PERFORM PRINT-DETAIL                                 NC1714.2
086300     ELSE                                                         NC1714.2
086400             MOVE    XRAY TO COMPUTED-X                           NC1714.2
086500             MOVE   "N"   TO CORRECT-X                            NC1714.2
086600             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1714.2
086700                  TO RE-MARK                                      NC1714.2
086800             PERFORM FAIL                                         NC1714.2
086900             PERFORM PRINT-DETAIL.                                NC1714.2
087000*                                                                 NC1714.2
087100*                                                                 NC1714.2
087200 DIV-INIT-F1-15.                                                  NC1714.2
087300*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1714.2
087400     MOVE   "V1-67 6.4.2"  TO ANSI-REFERENCE.                     NC1714.2
087500     MOVE   "DIV-TEST-F1-15         " TO PAR-NAME                 NC1714.2
087600     MOVE A18ONES-DS-09V09 TO WRK-DS-09V09.                       NC1714.2
087700     MOVE "1" TO WRK-XN-00001.                                    NC1714.2
087800     MOVE  1    TO REC-CT.                                        NC1714.2
087900 DIV-TEST-F1-15-0.                                                NC1714.2
088000     DIVIDE A18ONES-DS-09V09                                      NC1714.2
088100       INTO WRK-DS-09V09 ROUNDED                                  NC1714.2
088200     NOT ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                  NC1714.2
088300     GO TO   DIV-TEST-F1-15-1.                                    NC1714.2
088400 DIV-DELETE-F1-15-1.                                              NC1714.2
088500     PERFORM DE-LETE.                                             NC1714.2
088600     PERFORM PRINT-DETAIL.                                        NC1714.2
088700     GO TO   DIV-INIT-F1-16.                                      NC1714.2
088800 DIV-TEST-F1-15-1.                                                NC1714.2
088900     MOVE   "DIV-TEST-F1-15-1         " TO PAR-NAME.              NC1714.2
089000     IF      WRK-XN-00001 = "0"                                   NC1714.2
089100             PERFORM PASS                                         NC1714.2
089200             PERFORM PRINT-DETAIL                                 NC1714.2
089300     ELSE                                                         NC1714.2
089400             MOVE   "0"           TO CORRECT-X                    NC1714.2
089500             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
089600             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1714.2
089700                  TO RE-MARK                                      NC1714.2
089800             PERFORM FAIL                                         NC1714.2
089900             PERFORM PRINT-DETAIL.                                NC1714.2
090000     ADD     1 TO REC-CT.                                         NC1714.2
090100 DIV-TEST-F1-15-2.                                                NC1714.2
090200     MOVE   "DIV-TEST-F1-15-2         " TO PAR-NAME.              NC1714.2
090300     IF      WRK-DS-09V09 =  1                                    NC1714.2
090400             PERFORM PASS                                         NC1714.2
090500             PERFORM PRINT-DETAIL                                 NC1714.2
090600     ELSE                                                         NC1714.2
090700             MOVE    1             TO CORRECT-N                   NC1714.2
090800             MOVE    WRK-DS-09V09  TO COMPUTED-18V0               NC1714.2
090900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1714.2
091000                  TO RE-MARK                                      NC1714.2
091100             PERFORM FAIL                                         NC1714.2
091200             PERFORM PRINT-DETAIL.                                NC1714.2
091300*                                                                 NC1714.2
091400*                                                                 NC1714.2
091500 DIV-INIT-F1-16.                                                  NC1714.2
091600*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1714.2
091700     MOVE   "V1-67 6.4.2"  TO ANSI-REFERENCE.                     NC1714.2
091800     MOVE   "DIV-TEST-F1-16         " TO PAR-NAME.                NC1714.2
091900     MOVE   "Z" TO XRAY.                                          NC1714.2
092000     MOVE   1620.36 TO DIV1.                                      NC1714.2
092100     MOVE     44.1  TO DIV2.                                      NC1714.2
092200     MOVE    1    TO REC-CT.                                      NC1714.2
092300 DIV-TEST-F1-16-0.                                                NC1714.2
092400     DIVIDE  DIV2 INTO DIV1                                       NC1714.2
092500          ON SIZE ERROR                                           NC1714.2
092600             MOVE "E" TO XRAY                                     NC1714.2
092700      NOT ON SIZE ERROR                                           NC1714.2
092800             MOVE "N" TO XRAY.                                    NC1714.2
092900     GO TO   DIV-TEST-F1-16-1.                                    NC1714.2
093000 DIV-DELETE-F1-16-1.                                              NC1714.2
093100     PERFORM DE-LETE.                                             NC1714.2
093200     PERFORM PRINT-DETAIL.                                        NC1714.2
093300     GO TO   DIV-INIT-F1-17.                                      NC1714.2
093400 DIV-TEST-F1-16-1.                                                NC1714.2
093500     MOVE   "DIV-TEST-F1-16-1 " TO PAR-NAME.                      NC1714.2
093600     IF      XRAY    = "N"                                        NC1714.2
093700             PERFORM PASS                                         NC1714.2
093800             PERFORM PRINT-DETAIL                                 NC1714.2
093900     ELSE                                                         NC1714.2
094000             MOVE   "N"   TO CORRECT-X                            NC1714.2
094100             MOVE    XRAY TO COMPUTED-X                           NC1714.2
094200             MOVE  "NOT SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK NC1714.2
094300             PERFORM FAIL                                         NC1714.2
094400             PERFORM PRINT-DETAIL.                                NC1714.2
094500     ADD     1 TO REC-CT.                                         NC1714.2
094600 DIV-TEST-F1-16-2.                                                NC1714.2
094700     MOVE   "DIV-TEST-F1-16-2" TO PAR-NAME.                       NC1714.2
094800     IF      DIV1    = 36.74                                      NC1714.2
094900             PERFORM PASS                                         NC1714.2
095000             PERFORM PRINT-DETAIL                                 NC1714.2
095100     ELSE                                                         NC1714.2
095200             ADD     1 TO REC-CT                                  NC1714.2
095300             MOVE    DIV1 TO COMPUTED-N                           NC1714.2
095400             MOVE    36.74 TO CORRECT-N                           NC1714.2
095500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
095600             PERFORM FAIL                                         NC1714.2
095700             PERFORM PRINT-DETAIL.                                NC1714.2
095800*                                                                 NC1714.2
095900*                                                                 NC1714.2
096000 DIV-INIT-F1-17.                                                  NC1714.2
096100*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1714.2
096200     MOVE   "DIV-TEST-F1-17         " TO PAR-NAME                 NC1714.2
096300     MOVE   "V1-67 6.4.2"  TO ANSI-REFERENCE.                     NC1714.2
096400     MOVE A18ONES-DS-09V09 TO WRK-DS-09V09.                       NC1714.2
096500     MOVE "0" TO WRK-XN-00001.                                    NC1714.2
096600     MOVE    1    TO REC-CT.                                      NC1714.2
096700 DIV-TEST-F1-17-0.                                                NC1714.2
096800     DIVIDE A18ONES-DS-09V09                                      NC1714.2
096900       INTO WRK-DS-09V09 ROUNDED                                  NC1714.2
097000         ON SIZE ERROR                                            NC1714.2
097100            MOVE "1" TO WRK-XN-00001                              NC1714.2
097200     NOT ON SIZE ERROR                                            NC1714.2
097300            MOVE "2" TO WRK-XN-00001.                             NC1714.2
097400     GO TO   DIV-TEST-F1-17-1.                                    NC1714.2
097500 DIV-DELETE-F1-17-1.                                              NC1714.2
097600     PERFORM DE-LETE.                                             NC1714.2
097700     PERFORM PRINT-DETAIL.                                        NC1714.2
097800     GO TO   DIV-INIT-F1-18.                                      NC1714.2
097900 DIV-TEST-F1-17-1.                                                NC1714.2
098000     MOVE   "DIV-TEST-F1-17-1 " TO PAR-NAME.                      NC1714.2
098100     IF      WRK-XN-00001 = "2"                                   NC1714.2
098200             PERFORM PASS                                         NC1714.2
098300             PERFORM PRINT-DETAIL                                 NC1714.2
098400     ELSE                                                         NC1714.2
098500             MOVE   "2"           TO CORRECT-X                    NC1714.2
098600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
098700             PERFORM FAIL                                         NC1714.2
098800             PERFORM PRINT-DETAIL.                                NC1714.2
098900     ADD     1 TO REC-CT.                                         NC1714.2
099000 DIV-TEST-F1-17-2.                                                NC1714.2
099100     MOVE   "DIV-TEST-F1-17-2 " TO PAR-NAME.                      NC1714.2
099200     IF      WRK-DS-09V09 =  1                                    NC1714.2
099300             PERFORM PASS                                         NC1714.2
099400             PERFORM PRINT-DETAIL                                 NC1714.2
099500     ELSE                                                         NC1714.2
099600             MOVE    1             TO CORRECT-18V0                NC1714.2
099700             MOVE    WRK-DS-09V09  TO COMPUTED-18V0               NC1714.2
099800             PERFORM FAIL                                         NC1714.2
099900             PERFORM PRINT-DETAIL.                                NC1714.2
100000*                                                                 NC1714.2
100100*                                                                 NC1714.2
100200 DIV-INIT-F1-18.                                                  NC1714.2
100300*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1714.2
100400     MOVE   "V1-81 6.11.4 GR1"  TO ANSI-REFERENCE.                NC1714.2
100500     MOVE   "DIV-TEST-F1-18" TO PAR-NAME.                         NC1714.2
100600     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
100700     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
100800     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
100900     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
101000     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
101100     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
101200     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
101300     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
101400     MOVE    1 TO REC-CT.                                         NC1714.2
101500 DIV-TEST-F1-18-0.                                                NC1714.2
101600     DIVIDE  WRK-DU-1V3-1                                         NC1714.2
101700       INTO  WRK-DU-2V2-1                                         NC1714.2
101800             WRK-DU-2V2-2 ROUNDED                                 NC1714.2
101900             WRK-DU-2V2-3                                         NC1714.2
102000             WRK-DU-2V2-4 ROUNDED                                 NC1714.2
102100             WRK-DU-1V3-2                                         NC1714.2
102200             WRK-DU-2V2-5                                         NC1714.2
102300             WRK-DU-2V1-1 ROUNDED.                                NC1714.2
102400     GO TO   DIV-TEST-F1-18-1.                                    NC1714.2
102500 DIV-DELETE-F1-18.                                                NC1714.2
102600     PERFORM DE-LETE.                                             NC1714.2
102700     PERFORM PRINT-DETAIL.                                        NC1714.2
102800     GO TO   DIV-INIT-F1-19.                                      NC1714.2
102900 DIV-TEST-F1-18-1.                                                NC1714.2
103000     IF      WRK-DU-2V2-1 = 15.42                                 NC1714.2
103100             PERFORM PASS                                         NC1714.2
103200             PERFORM PRINT-DETAIL                                 NC1714.2
103300     ELSE                                                         NC1714.2
103400             PERFORM FAIL                                         NC1714.2
103500             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
103600             MOVE    15.42        TO CORRECT-N                    NC1714.2
103700             PERFORM PRINT-DETAIL.                                NC1714.2
103800     ADD 1 TO REC-CT.                                             NC1714.2
103900 DIV-TEST-F1-18-2.                                                NC1714.2
104000     IF      WRK-DU-2V2-2 = 60.83                                 NC1714.2
104100             PERFORM PASS                                         NC1714.2
104200             PERFORM PRINT-DETAIL                                 NC1714.2
104300     ELSE                                                         NC1714.2
104400             PERFORM FAIL                                         NC1714.2
104500             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
104600             MOVE    60.83        TO CORRECT-N                    NC1714.2
104700             PERFORM PRINT-DETAIL.                                NC1714.2
104800     ADD 1 TO REC-CT.                                             NC1714.2
104900 DIV-TEST-F1-18-3.                                                NC1714.2
105000     IF      WRK-DU-2V2-3 = 60.92                                 NC1714.2
105100             PERFORM PASS                                         NC1714.2
105200             PERFORM PRINT-DETAIL                                 NC1714.2
105300     ELSE                                                         NC1714.2
105400             PERFORM FAIL                                         NC1714.2
105500             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
105600             MOVE    60.92        TO CORRECT-N                    NC1714.2
105700             PERFORM PRINT-DETAIL.                                NC1714.2
105800     ADD 1 TO REC-CT.                                             NC1714.2
105900 DIV-TEST-F1-18-4.                                                NC1714.2
106000     IF      WRK-DU-2V2-4 = 60.93                                 NC1714.2
106100             PERFORM PASS                                         NC1714.2
106200             PERFORM PRINT-DETAIL                                 NC1714.2
106300     ELSE                                                         NC1714.2
106400             PERFORM FAIL                                         NC1714.2
106500             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
106600             MOVE    60.93        TO CORRECT-N                    NC1714.2
106700             PERFORM PRINT-DETAIL.                                NC1714.2
106800     ADD 1 TO REC-CT.                                             NC1714.2
106900 DIV-TEST-F1-18-5.                                                NC1714.2
107000     IF      WRK-DU-1V3-2 = 1.000                                 NC1714.2
107100             PERFORM PASS                                         NC1714.2
107200             PERFORM PRINT-DETAIL                                 NC1714.2
107300     ELSE                                                         NC1714.2
107400             PERFORM FAIL                                         NC1714.2
107500             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
107600             MOVE    1.000        TO CORRECT-N                    NC1714.2
107700             PERFORM PRINT-DETAIL.                                NC1714.2
107800     ADD 1 TO REC-CT.                                             NC1714.2
107900 DIV-TEST-F1-18-6.                                                NC1714.2
108000     IF      WRK-DU-2V2-5 = 09.99                                 NC1714.2
108100             PERFORM PASS                                         NC1714.2
108200             PERFORM PRINT-DETAIL                                 NC1714.2
108300     ELSE                                                         NC1714.2
108400             PERFORM FAIL                                         NC1714.2
108500             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
108600             MOVE    09.99        TO CORRECT-N                    NC1714.2
108700             PERFORM PRINT-DETAIL.                                NC1714.2
108800     ADD 1 TO REC-CT.                                             NC1714.2
108900 DIV-TEST-F1-18-7.                                                NC1714.2
109000     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
109100             PERFORM PASS                                         NC1714.2
109200             PERFORM PRINT-DETAIL                                 NC1714.2
109300     ELSE                                                         NC1714.2
109400             PERFORM FAIL                                         NC1714.2
109500             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
109600             MOVE    10.00        TO CORRECT-N                    NC1714.2
109700             PERFORM PRINT-DETAIL.                                NC1714.2
109800*                                                                 NC1714.2
109900*                                                                 NC1714.2
110000 DIV-INIT-F1-19.                                                  NC1714.2
110100*            ==-->   SIZE ERROR CONDITION  <--==                  NC1714.2
110200*            ==-->   NEW SIZE ERRROR TESTS <--==                  NC1714.2
110300     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1714.2
110400     MOVE   "DIV-TEST-F1-19" TO PAR-NAME.                         NC1714.2
110500     MOVE      .01           TO WRK-DU-0V2-1.                     NC1714.2
110600     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
110700     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
110800     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
110900     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
111000     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
111100     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
111200     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
111300     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
111400     MOVE     "0"            TO WRK-XN-00001.                     NC1714.2
111500     MOVE    1 TO REC-CT.                                         NC1714.2
111600 DIV-TEST-F1-19-0.                                                NC1714.2
111700     DIVIDE  WRK-DU-0V2-1                                         NC1714.2
111800       INTO  WRK-DU-2V2-1                                         NC1714.2
111900             WRK-DU-2V2-2                                         NC1714.2
112000             WRK-DU-2V2-3                                         NC1714.2
112100             WRK-DU-2V2-4                                         NC1714.2
112200             WRK-DU-1V3-2                                         NC1714.2
112300             WRK-DU-2V2-5                                         NC1714.2
112400             WRK-DU-2V1-1                                         NC1714.2
112500          ON SIZE ERROR                                           NC1714.2
112600             MOVE   "1"  TO WRK-XN-00001.                         NC1714.2
112700     GO TO   DIV-TEST-F1-19-1.                                    NC1714.2
112800 DIV-DELETE-F1-19.                                                NC1714.2
112900     PERFORM DE-LETE.                                             NC1714.2
113000     PERFORM PRINT-DETAIL.                                        NC1714.2
113100     GO TO   DIV-INIT-F1-20.                                      NC1714.2
113200 DIV-TEST-F1-19-1.                                                NC1714.2
113300     IF      WRK-DU-2V2-1 = 15.44                                 NC1714.2
113400             PERFORM PASS                                         NC1714.2
113500             PERFORM PRINT-DETAIL                                 NC1714.2
113600     ELSE                                                         NC1714.2
113700             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
113800             PERFORM FAIL                                         NC1714.2
113900             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
114000             MOVE    15.44        TO CORRECT-N                    NC1714.2
114100             PERFORM PRINT-DETAIL.                                NC1714.2
114200     ADD 1 TO REC-CT.                                             NC1714.2
114300 DIV-TEST-F1-19-2.                                                NC1714.2
114400     IF      WRK-DU-2V2-2 = 60.89                                 NC1714.2
114500             PERFORM PASS                                         NC1714.2
114600             PERFORM PRINT-DETAIL                                 NC1714.2
114700     ELSE                                                         NC1714.2
114800             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
114900             PERFORM FAIL                                         NC1714.2
115000             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
115100             MOVE    60.89        TO CORRECT-N                    NC1714.2
115200             PERFORM PRINT-DETAIL.                                NC1714.2
115300     ADD 1 TO REC-CT.                                             NC1714.2
115400 DIV-TEST-F1-19-3.                                                NC1714.2
115500     IF      WRK-DU-2V2-3 = 60.99                                 NC1714.2
115600             PERFORM PASS                                         NC1714.2
115700             PERFORM PRINT-DETAIL                                 NC1714.2
115800     ELSE                                                         NC1714.2
115900             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
116000             PERFORM FAIL                                         NC1714.2
116100             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
116200             MOVE    60.92        TO CORRECT-N                    NC1714.2
116300             PERFORM PRINT-DETAIL.                                NC1714.2
116400     ADD 1 TO REC-CT.                                             NC1714.2
116500 DIV-TEST-F1-19-4.                                                NC1714.2
116600     IF      WRK-DU-2V2-4 = 60.99                                 NC1714.2
116700             PERFORM PASS                                         NC1714.2
116800             PERFORM PRINT-DETAIL                                 NC1714.2
116900     ELSE                                                         NC1714.2
117000             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
117100             PERFORM FAIL                                         NC1714.2
117200             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
117300             MOVE    60.99        TO CORRECT-N                    NC1714.2
117400             PERFORM PRINT-DETAIL.                                NC1714.2
117500     ADD 1 TO REC-CT.                                             NC1714.2
117600 DIV-TEST-F1-19-5.                                                NC1714.2
117700     IF      WRK-DU-1V3-2 = 1.001                                 NC1714.2
117800             PERFORM PASS                                         NC1714.2
117900             PERFORM PRINT-DETAIL                                 NC1714.2
118000     ELSE                                                         NC1714.2
118100             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
118200             PERFORM FAIL                                         NC1714.2
118300             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
118400             MOVE    1.001        TO CORRECT-N                    NC1714.2
118500             PERFORM PRINT-DETAIL.                                NC1714.2
118600     ADD 1 TO REC-CT.                                             NC1714.2
118700 DIV-TEST-F1-19-6.                                                NC1714.2
118800     IF      WRK-DU-2V2-5 = 10.00                                 NC1714.2
118900             PERFORM PASS                                         NC1714.2
119000             PERFORM PRINT-DETAIL                                 NC1714.2
119100     ELSE                                                         NC1714.2
119200             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
119300             PERFORM FAIL                                         NC1714.2
119400             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
119500             MOVE    09.99        TO CORRECT-N                    NC1714.2
119600             PERFORM PRINT-DETAIL.                                NC1714.2
119700     ADD 1 TO REC-CT.                                             NC1714.2
119800 DIV-TEST-F1-19-7.                                                NC1714.2
119900     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
120000             PERFORM PASS                                         NC1714.2
120100             PERFORM PRINT-DETAIL                                 NC1714.2
120200     ELSE                                                         NC1714.2
120300             MOVE   "NOT AFFECTED BY SIZE ERROR" TO RE-MARK       NC1714.2
120400             PERFORM FAIL                                         NC1714.2
120500             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
120600             MOVE    10.0         TO CORRECT-N                    NC1714.2
120700             PERFORM PRINT-DETAIL.                                NC1714.2
120800     ADD 1 TO REC-CT.                                             NC1714.2
120900 DIV-TEST-F1-19-8.                                                NC1714.2
121000     IF      WRK-XN-00001 = "1"                                   NC1714.2
121100             PERFORM PASS                                         NC1714.2
121200             PERFORM PRINT-DETAIL                                 NC1714.2
121300     ELSE                                                         NC1714.2
121400             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1714.2
121500             PERFORM FAIL                                         NC1714.2
121600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
121700             MOVE    "1"          TO CORRECT-X                    NC1714.2
121800             PERFORM PRINT-DETAIL.                                NC1714.2
121900*                                                                 NC1714.2
122000*                                                                 NC1714.2
122100 DIV-INIT-F1-20.                                                  NC1714.2
122200*            ==-->  NO SIZE ERROR CONDITION  <--==                NC1714.2
122300*            ==-->   NEW SIZE ERRROR TESTS <--==                  NC1714.2
122400     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1714.2
122500     MOVE   "DIV-TEST-F1-20" TO PAR-NAME.                         NC1714.2
122600     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
122700     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
122800     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
122900     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
123000     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
123100     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
123200     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
123300     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
123400     MOVE     "0"            TO WRK-XN-00001.                     NC1714.2
123500     MOVE    1 TO REC-CT.                                         NC1714.2
123600 DIV-TEST-F1-20-0.                                                NC1714.2
123700     DIVIDE  WRK-DU-1V3-1                                         NC1714.2
123800       INTO  WRK-DU-2V2-1                                         NC1714.2
123900             WRK-DU-2V2-2 ROUNDED                                 NC1714.2
124000             WRK-DU-2V2-3                                         NC1714.2
124100             WRK-DU-2V2-4 ROUNDED                                 NC1714.2
124200             WRK-DU-1V3-2                                         NC1714.2
124300             WRK-DU-2V2-5                                         NC1714.2
124400             WRK-DU-2V1-1 ROUNDED                                 NC1714.2
124500          ON SIZE ERROR                                           NC1714.2
124600             MOVE   "1"  TO WRK-XN-00001.                         NC1714.2
124700     GO TO   DIV-TEST-F1-20-1.                                    NC1714.2
124800 DIV-DELETE-F1-20.                                                NC1714.2
124900     PERFORM DE-LETE.                                             NC1714.2
125000     PERFORM PRINT-DETAIL.                                        NC1714.2
125100     GO TO   DIV-INIT-F1-21.                                      NC1714.2
125200 DIV-TEST-F1-20-1.                                                NC1714.2
125300     IF      WRK-DU-2V2-1 = 15.42                                 NC1714.2
125400             PERFORM PASS                                         NC1714.2
125500             PERFORM PRINT-DETAIL                                 NC1714.2
125600     ELSE                                                         NC1714.2
125700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
125800             PERFORM FAIL                                         NC1714.2
125900             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
126000             MOVE    15.42        TO CORRECT-N                    NC1714.2
126100             PERFORM PRINT-DETAIL.                                NC1714.2
126200     ADD 1 TO REC-CT.                                             NC1714.2
126300 DIV-TEST-F1-20-2.                                                NC1714.2
126400     IF      WRK-DU-2V2-2 = 60.83                                 NC1714.2
126500             PERFORM PASS                                         NC1714.2
126600             PERFORM PRINT-DETAIL                                 NC1714.2
126700     ELSE                                                         NC1714.2
126800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
126900             PERFORM FAIL                                         NC1714.2
127000             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
127100             MOVE    60.83        TO CORRECT-N                    NC1714.2
127200             PERFORM PRINT-DETAIL.                                NC1714.2
127300     ADD 1 TO REC-CT.                                             NC1714.2
127400 DIV-TEST-F1-20-3.                                                NC1714.2
127500     IF      WRK-DU-2V2-3 = 60.92                                 NC1714.2
127600             PERFORM PASS                                         NC1714.2
127700             PERFORM PRINT-DETAIL                                 NC1714.2
127800     ELSE                                                         NC1714.2
127900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
128000             PERFORM FAIL                                         NC1714.2
128100             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
128200             MOVE    60.92        TO CORRECT-N                    NC1714.2
128300             PERFORM PRINT-DETAIL.                                NC1714.2
128400     ADD 1 TO REC-CT.                                             NC1714.2
128500 DIV-TEST-F1-20-4.                                                NC1714.2
128600     IF      WRK-DU-2V2-4 = 60.93                                 NC1714.2
128700             PERFORM PASS                                         NC1714.2
128800             PERFORM PRINT-DETAIL                                 NC1714.2
128900     ELSE                                                         NC1714.2
129000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
129100             PERFORM FAIL                                         NC1714.2
129200             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
129300             MOVE    60.93        TO CORRECT-N                    NC1714.2
129400             PERFORM PRINT-DETAIL.                                NC1714.2
129500     ADD 1 TO REC-CT.                                             NC1714.2
129600 DIV-TEST-F1-20-5.                                                NC1714.2
129700     IF      WRK-DU-1V3-2 = 1.000                                 NC1714.2
129800             PERFORM PASS                                         NC1714.2
129900             PERFORM PRINT-DETAIL                                 NC1714.2
130000     ELSE                                                         NC1714.2
130100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
130200             PERFORM FAIL                                         NC1714.2
130300             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
130400             MOVE    1.000        TO CORRECT-N                    NC1714.2
130500             PERFORM PRINT-DETAIL.                                NC1714.2
130600     ADD 1 TO REC-CT.                                             NC1714.2
130700 DIV-TEST-F1-20-6.                                                NC1714.2
130800     IF      WRK-DU-2V2-5 = 09.99                                 NC1714.2
130900             PERFORM PASS                                         NC1714.2
131000             PERFORM PRINT-DETAIL                                 NC1714.2
131100     ELSE                                                         NC1714.2
131200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
131300             PERFORM FAIL                                         NC1714.2
131400             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
131500             MOVE    09.99        TO CORRECT-N                    NC1714.2
131600             PERFORM PRINT-DETAIL.                                NC1714.2
131700     ADD 1 TO REC-CT.                                             NC1714.2
131800 DIV-TEST-F1-20-7.                                                NC1714.2
131900     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
132000             PERFORM PASS                                         NC1714.2
132100             PERFORM PRINT-DETAIL                                 NC1714.2
132200     ELSE                                                         NC1714.2
132300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
132400             PERFORM FAIL                                         NC1714.2
132500             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
132600             MOVE    10.00        TO CORRECT-N                    NC1714.2
132700             PERFORM PRINT-DETAIL.                                NC1714.2
132800     ADD 1 TO REC-CT.                                             NC1714.2
132900 DIV-TEST-F1-20-8.                                                NC1714.2
133000     IF      WRK-XN-00001 = "0"                                   NC1714.2
133100             PERFORM PASS                                         NC1714.2
133200             PERFORM PRINT-DETAIL                                 NC1714.2
133300     ELSE                                                         NC1714.2
133400             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
133500                  TO RE-MARK                                      NC1714.2
133600             PERFORM FAIL                                         NC1714.2
133700             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
133800             MOVE    "0"          TO CORRECT-X                    NC1714.2
133900             PERFORM PRINT-DETAIL.                                NC1714.2
134000*                                                                 NC1714.2
134100*                                                                 NC1714.2
134200 DIV-INIT-F1-21.                                                  NC1714.2
134300*            ==-->   SIZE ERROR CONDITION  <--==                  NC1714.2
134400*            ==-->   NEW SIZE ERRROR TESTS <--==                  NC1714.2
134500     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1714.2
134600     MOVE   "DIV-TEST-F1-21" TO PAR-NAME.                         NC1714.2
134700     MOVE      .01           TO WRK-DU-0V2-1.                     NC1714.2
134800     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
134900     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
135000     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
135100     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
135200     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
135300     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
135400     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
135500     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
135600     MOVE     "0"            TO WRK-XN-00001.                     NC1714.2
135700     MOVE    1 TO REC-CT.                                         NC1714.2
135800 DIV-TEST-F1-21-0.                                                NC1714.2
135900     DIVIDE  WRK-DU-0V2-1                                         NC1714.2
136000       INTO  WRK-DU-2V2-1                                         NC1714.2
136100             WRK-DU-2V2-2                                         NC1714.2
136200             WRK-DU-2V2-3                                         NC1714.2
136300             WRK-DU-2V2-4                                         NC1714.2
136400             WRK-DU-1V3-2                                         NC1714.2
136500             WRK-DU-2V2-5                                         NC1714.2
136600             WRK-DU-2V1-1                                         NC1714.2
136700      NOT ON SIZE ERROR                                           NC1714.2
136800             MOVE   "1"  TO WRK-XN-00001.                         NC1714.2
136900     GO TO   DIV-TEST-F1-21-1.                                    NC1714.2
137000 DIV-DELETE-F1-21.                                                NC1714.2
137100     PERFORM DE-LETE.                                             NC1714.2
137200     PERFORM PRINT-DETAIL.                                        NC1714.2
137300     GO TO   DIV-INIT-F1-22.                                      NC1714.2
137400 DIV-TEST-F1-21-1.                                                NC1714.2
137500     IF      WRK-DU-2V2-1 = 15.44                                 NC1714.2
137600             PERFORM PASS                                         NC1714.2
137700             PERFORM PRINT-DETAIL                                 NC1714.2
137800     ELSE                                                         NC1714.2
137900             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
138000             PERFORM FAIL                                         NC1714.2
138100             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
138200             MOVE    15.44        TO CORRECT-N                    NC1714.2
138300             PERFORM PRINT-DETAIL.                                NC1714.2
138400     ADD 1 TO REC-CT.                                             NC1714.2
138500 DIV-TEST-F1-21-2.                                                NC1714.2
138600     IF      WRK-DU-2V2-2 = 60.89                                 NC1714.2
138700             PERFORM PASS                                         NC1714.2
138800             PERFORM PRINT-DETAIL                                 NC1714.2
138900     ELSE                                                         NC1714.2
139000             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
139100             PERFORM FAIL                                         NC1714.2
139200             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
139300             MOVE    60.89        TO CORRECT-N                    NC1714.2
139400             PERFORM PRINT-DETAIL.                                NC1714.2
139500     ADD 1 TO REC-CT.                                             NC1714.2
139600 DIV-TEST-F1-21-3.                                                NC1714.2
139700     IF      WRK-DU-2V2-3 = 60.99                                 NC1714.2
139800             PERFORM PASS                                         NC1714.2
139900             PERFORM PRINT-DETAIL                                 NC1714.2
140000     ELSE                                                         NC1714.2
140100             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
140200             PERFORM FAIL                                         NC1714.2
140300             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
140400             MOVE    60.99        TO CORRECT-N                    NC1714.2
140500             PERFORM PRINT-DETAIL.                                NC1714.2
140600     ADD 1 TO REC-CT.                                             NC1714.2
140700 DIV-TEST-F1-21-4.                                                NC1714.2
140800     IF      WRK-DU-2V2-4 = 60.99                                 NC1714.2
140900             PERFORM PASS                                         NC1714.2
141000             PERFORM PRINT-DETAIL                                 NC1714.2
141100     ELSE                                                         NC1714.2
141200             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
141300             PERFORM FAIL                                         NC1714.2
141400             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
141500             MOVE    60.99        TO CORRECT-N                    NC1714.2
141600             PERFORM PRINT-DETAIL.                                NC1714.2
141700     ADD 1 TO REC-CT.                                             NC1714.2
141800 DIV-TEST-F1-21-5.                                                NC1714.2
141900     IF      WRK-DU-1V3-2 = 1.001                                 NC1714.2
142000             PERFORM PASS                                         NC1714.2
142100             PERFORM PRINT-DETAIL                                 NC1714.2
142200     ELSE                                                         NC1714.2
142300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
142400             PERFORM FAIL                                         NC1714.2
142500             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
142600             MOVE    1.001        TO CORRECT-N                    NC1714.2
142700             PERFORM PRINT-DETAIL.                                NC1714.2
142800     ADD 1 TO REC-CT.                                             NC1714.2
142900 DIV-TEST-F1-21-6.                                                NC1714.2
143000     IF      WRK-DU-2V2-5 = 10.00                                 NC1714.2
143100             PERFORM PASS                                         NC1714.2
143200             PERFORM PRINT-DETAIL                                 NC1714.2
143300     ELSE                                                         NC1714.2
143400             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
143500             PERFORM FAIL                                         NC1714.2
143600             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
143700             MOVE    10.00        TO CORRECT-N                    NC1714.2
143800             PERFORM PRINT-DETAIL.                                NC1714.2
143900     ADD 1 TO REC-CT.                                             NC1714.2
144000 DIV-TEST-F1-21-7.                                                NC1714.2
144100     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
144200             PERFORM PASS                                         NC1714.2
144300             PERFORM PRINT-DETAIL                                 NC1714.2
144400     ELSE                                                         NC1714.2
144500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
144600             PERFORM FAIL                                         NC1714.2
144700             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
144800             MOVE    10.0         TO CORRECT-N                    NC1714.2
144900             PERFORM PRINT-DETAIL.                                NC1714.2
145000     ADD 1 TO REC-CT.                                             NC1714.2
145100 DIV-TEST-F1-21-8.                                                NC1714.2
145200     IF      WRK-XN-00001 = "0"                                   NC1714.2
145300             PERFORM PASS                                         NC1714.2
145400             PERFORM PRINT-DETAIL                                 NC1714.2
145500     ELSE                                                         NC1714.2
145600             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1714.2
145700                  TO RE-MARK                                      NC1714.2
145800             PERFORM FAIL                                         NC1714.2
145900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
146000             MOVE    "0"          TO CORRECT-X                    NC1714.2
146100             PERFORM PRINT-DETAIL.                                NC1714.2
146200*                                                                 NC1714.2
146300*                                                                 NC1714.2
146400 DIV-INIT-F1-22.                                                  NC1714.2
146500*            ==-->  NO SIZE ERROR CONDITION <--==                 NC1714.2
146600*            ==-->   NEW SIZE ERRROR TESTS <--==                  NC1714.2
146700     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1714.2
146800     MOVE   "DIV-TEST-F1-22" TO PAR-NAME.                         NC1714.2
146900     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
147000     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
147100     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
147200     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
147300     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
147400     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
147500     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
147600     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
147700     MOVE   "0"              TO WRK-XN-00001.                     NC1714.2
147800     MOVE    1 TO REC-CT.                                         NC1714.2
147900 DIV-TEST-F1-22-0.                                                NC1714.2
148000     DIVIDE  WRK-DU-1V3-1                                         NC1714.2
148100       INTO  WRK-DU-2V2-1                                         NC1714.2
148200             WRK-DU-2V2-2 ROUNDED                                 NC1714.2
148300             WRK-DU-2V2-3                                         NC1714.2
148400             WRK-DU-2V2-4 ROUNDED                                 NC1714.2
148500             WRK-DU-1V3-2                                         NC1714.2
148600             WRK-DU-2V2-5                                         NC1714.2
148700             WRK-DU-2V1-1 ROUNDED                                 NC1714.2
148800      NOT ON SIZE ERROR                                           NC1714.2
148900             MOVE   "1"  TO WRK-XN-00001.                         NC1714.2
149000     GO TO   DIV-TEST-F1-22-1.                                    NC1714.2
149100 DIV-DELETE-F1-22.                                                NC1714.2
149200     PERFORM DE-LETE.                                             NC1714.2
149300     PERFORM PRINT-DETAIL.                                        NC1714.2
149400     GO TO   DIV-INIT-F1-23.                                      NC1714.2
149500 DIV-TEST-F1-22-1.                                                NC1714.2
149600     IF      WRK-DU-2V2-1 = 15.42                                 NC1714.2
149700             PERFORM PASS                                         NC1714.2
149800             PERFORM PRINT-DETAIL                                 NC1714.2
149900     ELSE                                                         NC1714.2
150000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
150100             PERFORM FAIL                                         NC1714.2
150200             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
150300             MOVE    15.42        TO CORRECT-N                    NC1714.2
150400             PERFORM PRINT-DETAIL.                                NC1714.2
150500     ADD 1 TO REC-CT.                                             NC1714.2
150600 DIV-TEST-F1-22-2.                                                NC1714.2
150700     IF      WRK-DU-2V2-2 = 60.83                                 NC1714.2
150800             PERFORM PASS                                         NC1714.2
150900             PERFORM PRINT-DETAIL                                 NC1714.2
151000     ELSE                                                         NC1714.2
151100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
151200             PERFORM FAIL                                         NC1714.2
151300             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
151400             MOVE    60.83        TO CORRECT-N                    NC1714.2
151500             PERFORM PRINT-DETAIL.                                NC1714.2
151600     ADD 1 TO REC-CT.                                             NC1714.2
151700 DIV-TEST-F1-22-3.                                                NC1714.2
151800     IF      WRK-DU-2V2-3 = 60.92                                 NC1714.2
151900             PERFORM PASS                                         NC1714.2
152000             PERFORM PRINT-DETAIL                                 NC1714.2
152100     ELSE                                                         NC1714.2
152200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
152300             PERFORM FAIL                                         NC1714.2
152400             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
152500             MOVE    60.92        TO CORRECT-N                    NC1714.2
152600             PERFORM PRINT-DETAIL.                                NC1714.2
152700     ADD 1 TO REC-CT.                                             NC1714.2
152800 DIV-TEST-F1-22-4.                                                NC1714.2
152900     IF      WRK-DU-2V2-4 = 60.93                                 NC1714.2
153000             PERFORM PASS                                         NC1714.2
153100             PERFORM PRINT-DETAIL                                 NC1714.2
153200     ELSE                                                         NC1714.2
153300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
153400             PERFORM FAIL                                         NC1714.2
153500             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
153600             MOVE    60.93        TO CORRECT-N                    NC1714.2
153700             PERFORM PRINT-DETAIL.                                NC1714.2
153800     ADD 1 TO REC-CT.                                             NC1714.2
153900 DIV-TEST-F1-22-5.                                                NC1714.2
154000     IF      WRK-DU-1V3-2 = 1.000                                 NC1714.2
154100             PERFORM PASS                                         NC1714.2
154200             PERFORM PRINT-DETAIL                                 NC1714.2
154300     ELSE                                                         NC1714.2
154400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
154500             PERFORM FAIL                                         NC1714.2
154600             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
154700             MOVE    1.000        TO CORRECT-N                    NC1714.2
154800             PERFORM PRINT-DETAIL.                                NC1714.2
154900     ADD 1 TO REC-CT.                                             NC1714.2
155000 DIV-TEST-F1-22-6.                                                NC1714.2
155100     IF      WRK-DU-2V2-5 = 09.99                                 NC1714.2
155200             PERFORM PASS                                         NC1714.2
155300             PERFORM PRINT-DETAIL                                 NC1714.2
155400     ELSE                                                         NC1714.2
155500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
155600             PERFORM FAIL                                         NC1714.2
155700             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
155800             MOVE    09.99        TO CORRECT-N                    NC1714.2
155900             PERFORM PRINT-DETAIL.                                NC1714.2
156000     ADD 1 TO REC-CT.                                             NC1714.2
156100 DIV-TEST-F1-22-7.                                                NC1714.2
156200     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
156300             PERFORM PASS                                         NC1714.2
156400             PERFORM PRINT-DETAIL                                 NC1714.2
156500     ELSE                                                         NC1714.2
156600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
156700             PERFORM FAIL                                         NC1714.2
156800             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
156900             MOVE    10.00        TO CORRECT-N                    NC1714.2
157000             PERFORM PRINT-DETAIL.                                NC1714.2
157100     ADD 1 TO REC-CT.                                             NC1714.2
157200 DIV-TEST-F1-22-8.                                                NC1714.2
157300     IF      WRK-XN-00001 = "1"                                   NC1714.2
157400             PERFORM PASS                                         NC1714.2
157500             PERFORM PRINT-DETAIL                                 NC1714.2
157600     ELSE                                                         NC1714.2
157700             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1714.2
157800                  TO RE-MARK                                      NC1714.2
157900             PERFORM FAIL                                         NC1714.2
158000             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
158100             MOVE    "1"          TO CORRECT-X                    NC1714.2
158200             PERFORM PRINT-DETAIL.                                NC1714.2
158300*                                                                 NC1714.2
158400*                                                                 NC1714.2
158500 DIV-INIT-F1-23.                                                  NC1714.2
158600*            ==-->    SIZE ERROR CONDITION <--==                  NC1714.2
158700*            ==-->   NEW SIZE ERRROR TESTS <--==                  NC1714.2
158800     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1714.2
158900     MOVE   "DIV-TEST-F1-23" TO PAR-NAME.                         NC1714.2
159000     MOVE      .01           TO WRK-DU-0V2-1.                     NC1714.2
159100     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
159200     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
159300     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
159400     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
159500     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
159600     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
159700     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
159800     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
159900     MOVE     "0"            TO WRK-XN-00001.                     NC1714.2
160000     MOVE    1 TO REC-CT.                                         NC1714.2
160100 DIV-TEST-F1-23-0.                                                NC1714.2
160200     DIVIDE  WRK-DU-0V2-1                                         NC1714.2
160300       INTO  WRK-DU-2V2-1                                         NC1714.2
160400             WRK-DU-2V2-2                                         NC1714.2
160500             WRK-DU-2V2-3                                         NC1714.2
160600             WRK-DU-2V2-4                                         NC1714.2
160700             WRK-DU-1V3-2                                         NC1714.2
160800             WRK-DU-2V2-5                                         NC1714.2
160900             WRK-DU-2V1-1                                         NC1714.2
161000          ON SIZE ERROR                                           NC1714.2
161100             MOVE   "1"  TO WRK-XN-00001                          NC1714.2
161200      NOT ON SIZE ERROR                                           NC1714.2
161300             MOVE   "2"  TO WRK-XN-00001.                         NC1714.2
161400     GO TO   DIV-TEST-F1-23-1.                                    NC1714.2
161500 DIV-DELETE-F1-23.                                                NC1714.2
161600     PERFORM DE-LETE.                                             NC1714.2
161700     PERFORM PRINT-DETAIL.                                        NC1714.2
161800     GO TO   DIV-INIT-F1-24.                                      NC1714.2
161900 DIV-TEST-F1-23-1.                                                NC1714.2
162000     IF      WRK-DU-2V2-1 = 15.44                                 NC1714.2
162100             PERFORM PASS                                         NC1714.2
162200             PERFORM PRINT-DETAIL                                 NC1714.2
162300     ELSE                                                         NC1714.2
162400             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
162500             PERFORM FAIL                                         NC1714.2
162600             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
162700             MOVE    15.44        TO CORRECT-N                    NC1714.2
162800             PERFORM PRINT-DETAIL.                                NC1714.2
162900     ADD 1 TO REC-CT.                                             NC1714.2
163000 DIV-TEST-F1-23-2.                                                NC1714.2
163100     IF      WRK-DU-2V2-2 = 60.89                                 NC1714.2
163200             PERFORM PASS                                         NC1714.2
163300             PERFORM PRINT-DETAIL                                 NC1714.2
163400     ELSE                                                         NC1714.2
163500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
163600             PERFORM FAIL                                         NC1714.2
163700             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
163800             MOVE    60.89        TO CORRECT-N                    NC1714.2
163900             PERFORM PRINT-DETAIL.                                NC1714.2
164000     ADD 1 TO REC-CT.                                             NC1714.2
164100 DIV-TEST-F1-23-3.                                                NC1714.2
164200     IF      WRK-DU-2V2-3 = 60.99                                 NC1714.2
164300             PERFORM PASS                                         NC1714.2
164400             PERFORM PRINT-DETAIL                                 NC1714.2
164500     ELSE                                                         NC1714.2
164600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
164700             PERFORM FAIL                                         NC1714.2
164800             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
164900             MOVE    60.99        TO CORRECT-N                    NC1714.2
165000             PERFORM PRINT-DETAIL.                                NC1714.2
165100     ADD 1 TO REC-CT.                                             NC1714.2
165200 DIV-TEST-F1-23-4.                                                NC1714.2
165300     IF      WRK-DU-2V2-4 = 60.99                                 NC1714.2
165400             PERFORM PASS                                         NC1714.2
165500             PERFORM PRINT-DETAIL                                 NC1714.2
165600     ELSE                                                         NC1714.2
165700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
165800             PERFORM FAIL                                         NC1714.2
165900             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
166000             MOVE    60.99        TO CORRECT-N                    NC1714.2
166100             PERFORM PRINT-DETAIL.                                NC1714.2
166200     ADD 1 TO REC-CT.                                             NC1714.2
166300 DIV-TEST-F1-23-5.                                                NC1714.2
166400     IF      WRK-DU-1V3-2 = 1.001                                 NC1714.2
166500             PERFORM PASS                                         NC1714.2
166600             PERFORM PRINT-DETAIL                                 NC1714.2
166700     ELSE                                                         NC1714.2
166800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
166900             PERFORM FAIL                                         NC1714.2
167000             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
167100             MOVE    1.001        TO CORRECT-N                    NC1714.2
167200             PERFORM PRINT-DETAIL.                                NC1714.2
167300     ADD 1 TO REC-CT.                                             NC1714.2
167400 DIV-TEST-F1-23-6.                                                NC1714.2
167500     IF      WRK-DU-2V2-5 = 10.00                                 NC1714.2
167600             PERFORM PASS                                         NC1714.2
167700             PERFORM PRINT-DETAIL                                 NC1714.2
167800     ELSE                                                         NC1714.2
167900             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
168000             PERFORM FAIL                                         NC1714.2
168100             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
168200             MOVE    10.00        TO CORRECT-N                    NC1714.2
168300             PERFORM PRINT-DETAIL.                                NC1714.2
168400     ADD 1 TO REC-CT.                                             NC1714.2
168500 DIV-TEST-F1-23-7.                                                NC1714.2
168600     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
168700             PERFORM PASS                                         NC1714.2
168800             PERFORM PRINT-DETAIL                                 NC1714.2
168900     ELSE                                                         NC1714.2
169000             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1714.2
169100             PERFORM FAIL                                         NC1714.2
169200             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
169300             MOVE    10.0         TO CORRECT-N                    NC1714.2
169400             PERFORM PRINT-DETAIL.                                NC1714.2
169500     ADD 1 TO REC-CT.                                             NC1714.2
169600 DIV-TEST-F1-23-8.                                                NC1714.2
169700     IF      WRK-XN-00001 = "1"                                   NC1714.2
169800             PERFORM PASS                                         NC1714.2
169900             PERFORM PRINT-DETAIL                                 NC1714.2
170000     ELSE                                                         NC1714.2
170100             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1714.2
170200             PERFORM FAIL                                         NC1714.2
170300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
170400             MOVE    "1"          TO CORRECT-X                    NC1714.2
170500             PERFORM PRINT-DETAIL.                                NC1714.2
170600*                                                                 NC1714.2
170700*                                                                 NC1714.2
170800 DIV-INIT-F1-24.                                                  NC1714.2
170900*            ==--> NO SIZE ERROR CONDITION <--==                  NC1714.2
171000*            ==-->   NEW SIZE ERRROR TESTS <--==                  NC1714.2
171100     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1714.2
171200     MOVE   "DIV-TEST-F1-24" TO PAR-NAME.                         NC1714.2
171300     MOVE     1.001          TO WRK-DU-1V3-1.                     NC1714.2
171400     MOVE    15.44           TO WRK-DU-2V2-1.                     NC1714.2
171500     MOVE    60.89           TO WRK-DU-2V2-2.                     NC1714.2
171600     MOVE    60.99           TO WRK-DU-2V2-3.                     NC1714.2
171700     MOVE    60.99           TO WRK-DU-2V2-4.                     NC1714.2
171800     MOVE    10.0            TO WRK-DU-2V2-5.                     NC1714.2
171900     MOVE     1.001          TO WRK-DU-1V3-2.                     NC1714.2
172000     MOVE    10.0            TO WRK-DU-2V1-1.                     NC1714.2
172100     MOVE    1 TO REC-CT.                                         NC1714.2
172200 DIV-TEST-F1-24-0.                                                NC1714.2
172300     DIVIDE  WRK-DU-1V3-1                                         NC1714.2
172400       INTO  WRK-DU-2V2-1                                         NC1714.2
172500             WRK-DU-2V2-2 ROUNDED                                 NC1714.2
172600             WRK-DU-2V2-3                                         NC1714.2
172700             WRK-DU-2V2-4 ROUNDED                                 NC1714.2
172800             WRK-DU-1V3-2                                         NC1714.2
172900             WRK-DU-2V2-5                                         NC1714.2
173000             WRK-DU-2V1-1 ROUNDED                                 NC1714.2
173100          ON SIZE ERROR                                           NC1714.2
173200             MOVE   "1"  TO WRK-XN-00001                          NC1714.2
173300      NOT ON SIZE ERROR                                           NC1714.2
173400             MOVE   "2"  TO WRK-XN-00001.                         NC1714.2
173500     GO TO   DIV-TEST-F1-24-1.                                    NC1714.2
173600 DIV-DELETE-F1-24.                                                NC1714.2
173700     PERFORM DE-LETE.                                             NC1714.2
173800     PERFORM PRINT-DETAIL.                                        NC1714.2
173900     GO TO   DIV-INIT-F1-25.                                      NC1714.2
174000 DIV-TEST-F1-24-1.                                                NC1714.2
174100     IF      WRK-DU-2V2-1 = 15.42                                 NC1714.2
174200             PERFORM PASS                                         NC1714.2
174300             PERFORM PRINT-DETAIL                                 NC1714.2
174400     ELSE                                                         NC1714.2
174500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
174600             PERFORM FAIL                                         NC1714.2
174700             MOVE    WRK-DU-2V2-1 TO COMPUTED-N                   NC1714.2
174800             MOVE    15.42        TO CORRECT-N                    NC1714.2
174900             PERFORM PRINT-DETAIL.                                NC1714.2
175000     ADD 1 TO REC-CT.                                             NC1714.2
175100 DIV-TEST-F1-24-2.                                                NC1714.2
175200     IF      WRK-DU-2V2-2 = 60.83                                 NC1714.2
175300             PERFORM PASS                                         NC1714.2
175400             PERFORM PRINT-DETAIL                                 NC1714.2
175500     ELSE                                                         NC1714.2
175600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
175700             PERFORM FAIL                                         NC1714.2
175800             MOVE    WRK-DU-2V2-2 TO COMPUTED-N                   NC1714.2
175900             MOVE    60.83        TO CORRECT-N                    NC1714.2
176000             PERFORM PRINT-DETAIL.                                NC1714.2
176100     ADD 1 TO REC-CT.                                             NC1714.2
176200 DIV-TEST-F1-24-3.                                                NC1714.2
176300     IF      WRK-DU-2V2-3 = 60.92                                 NC1714.2
176400             PERFORM PASS                                         NC1714.2
176500             PERFORM PRINT-DETAIL                                 NC1714.2
176600     ELSE                                                         NC1714.2
176700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
176800             PERFORM FAIL                                         NC1714.2
176900             MOVE    WRK-DU-2V2-3 TO COMPUTED-N                   NC1714.2
177000             MOVE    60.92        TO CORRECT-N                    NC1714.2
177100             PERFORM PRINT-DETAIL.                                NC1714.2
177200     ADD 1 TO REC-CT.                                             NC1714.2
177300 DIV-TEST-F1-24-4.                                                NC1714.2
177400     IF      WRK-DU-2V2-4 = 60.93                                 NC1714.2
177500             PERFORM PASS                                         NC1714.2
177600             PERFORM PRINT-DETAIL                                 NC1714.2
177700     ELSE                                                         NC1714.2
177800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
177900             PERFORM FAIL                                         NC1714.2
178000             MOVE    WRK-DU-2V2-4 TO COMPUTED-N                   NC1714.2
178100             MOVE    60.93        TO CORRECT-N                    NC1714.2
178200             PERFORM PRINT-DETAIL.                                NC1714.2
178300     ADD 1 TO REC-CT.                                             NC1714.2
178400 DIV-TEST-F1-24-5.                                                NC1714.2
178500     IF      WRK-DU-1V3-2 = 1.000                                 NC1714.2
178600             PERFORM PASS                                         NC1714.2
178700             PERFORM PRINT-DETAIL                                 NC1714.2
178800     ELSE                                                         NC1714.2
178900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
179000             PERFORM FAIL                                         NC1714.2
179100             MOVE    WRK-DU-1V3-2 TO COMPUTED-N                   NC1714.2
179200             MOVE    1.000        TO CORRECT-N                    NC1714.2
179300             PERFORM PRINT-DETAIL.                                NC1714.2
179400     ADD 1 TO REC-CT.                                             NC1714.2
179500 DIV-TEST-F1-24-6.                                                NC1714.2
179600     IF      WRK-DU-2V2-5 = 09.99                                 NC1714.2
179700             PERFORM PASS                                         NC1714.2
179800             PERFORM PRINT-DETAIL                                 NC1714.2
179900     ELSE                                                         NC1714.2
180000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
180100             PERFORM FAIL                                         NC1714.2
180200             MOVE    WRK-DU-2V2-5 TO COMPUTED-N                   NC1714.2
180300             MOVE    09.99        TO CORRECT-N                    NC1714.2
180400             PERFORM PRINT-DETAIL.                                NC1714.2
180500     ADD 1 TO REC-CT.                                             NC1714.2
180600 DIV-TEST-F1-24-7.                                                NC1714.2
180700     IF      WRK-DU-2V1-1 = 10.0                                  NC1714.2
180800             PERFORM PASS                                         NC1714.2
180900             PERFORM PRINT-DETAIL                                 NC1714.2
181000     ELSE                                                         NC1714.2
181100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
181200             PERFORM FAIL                                         NC1714.2
181300             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1714.2
181400             MOVE    10.00        TO CORRECT-N                    NC1714.2
181500             PERFORM PRINT-DETAIL.                                NC1714.2
181600     ADD 1 TO REC-CT.                                             NC1714.2
181700 DIV-TEST-F1-24-8.                                                NC1714.2
181800     IF      WRK-XN-00001 = "2"                                   NC1714.2
181900             PERFORM PASS                                         NC1714.2
182000             PERFORM PRINT-DETAIL                                 NC1714.2
182100     ELSE                                                         NC1714.2
182200             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
182300                  TO RE-MARK                                      NC1714.2
182400             PERFORM FAIL                                         NC1714.2
182500             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
182600             MOVE    "2"          TO CORRECT-X                    NC1714.2
182700             PERFORM PRINT-DETAIL.                                NC1714.2
182800*                                                                 NC1714.2
182900*                                                                 NC1714.2
183000 DIV-INIT-F1-25.                                                  NC1714.2
183100*            ==--> NO SIZE ERROR CONDITION <--==                  NC1714.2
183200*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1714.2
183300     MOVE   "1V-41 6.4.3 "   TO ANSI-REFERENCE.                   NC1714.2
183400     MOVE   "DIV-TEST-F1-25       " TO PAR-NAME.                  NC1714.2
183500     MOVE   "0" TO WRK-XN-00001.                                  NC1714.2
183600     MOVE    0  TO WRK-DS-05V00.                                  NC1714.2
183700     MOVE    0  TO WRK-DS-02V00.                                  NC1714.2
183800     MOVE    0  TO WRK-CS-18V00.                                  NC1714.2
183900     MOVE    1620.36 TO DIV1.                                     NC1714.2
184000     MOVE    44.1    TO DIV2.                                     NC1714.2
184100     MOVE    1 TO REC-CT.                                         NC1714.2
184200 DIV-TEST-F1-25-0.                                                NC1714.2
184300     DIVIDE  DIV2 INTO DIV1                                       NC1714.2
184400          ON SIZE ERROR                                           NC1714.2
184500             MOVE   "1" TO WRK-XN-00001                           NC1714.2
184600             MOVE   23  TO WRK-DS-05V00                           NC1714.2
184700             MOVE   -4  TO WRK-DS-02V00                           NC1714.2
184800     END-DIVIDE                                                   NC1714.2
184900     MOVE    99 TO WRK-CS-18V00.                                  NC1714.2
185000     GO TO   DIV-TEST-F1-25-1.                                    NC1714.2
185100 DIV-DELETE-F1-25-1.                                              NC1714.2
185200     PERFORM DE-LETE.                                             NC1714.2
185300     PERFORM PRINT-DETAIL.                                        NC1714.2
185400     GO TO   DIV-INIT-F1-26.                                      NC1714.2
185500 DIV-TEST-F1-25-1.                                                NC1714.2
185600     IF      WRK-XN-00001  = "0"                                  NC1714.2
185700             PERFORM PASS                                         NC1714.2
185800             PERFORM PRINT-DETAIL                                 NC1714.2
185900     ELSE                                                         NC1714.2
186000             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
186100             MOVE   "0"           TO CORRECT-X                    NC1714.2
186200             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
186300                 TO RE-MARK                                       NC1714.2
186400             PERFORM FAIL                                         NC1714.2
186500             PERFORM PRINT-DETAIL.                                NC1714.2
186600     ADD     1 TO REC-CT.                                         NC1714.2
186700 DIV-TEST-F1-25-2.                                                NC1714.2
186800     IF      WRK-DS-05V00  =  0                                   NC1714.2
186900             PERFORM PASS                                         NC1714.2
187000             PERFORM PRINT-DETAIL                                 NC1714.2
187100     ELSE                                                         NC1714.2
187200             MOVE    WRK-DS-05V00 TO COMPUTED-N                   NC1714.2
187300             MOVE    0            TO CORRECT-N                    NC1714.2
187400             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
187500                  TO RE-MARK                                      NC1714.2
187600             PERFORM FAIL                                         NC1714.2
187700             PERFORM PRINT-DETAIL.                                NC1714.2
187800     ADD     1 TO REC-CT.                                         NC1714.2
187900 DIV-TEST-F1-25-3.                                                NC1714.2
188000     IF      WRK-DS-02V00  =  0                                   NC1714.2
188100             PERFORM PASS                                         NC1714.2
188200             PERFORM PRINT-DETAIL                                 NC1714.2
188300     ELSE                                                         NC1714.2
188400             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1714.2
188500             MOVE    0            TO CORRECT-N                    NC1714.2
188600             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
188700                  TO RE-MARK                                      NC1714.2
188800             PERFORM FAIL                                         NC1714.2
188900             PERFORM PRINT-DETAIL.                                NC1714.2
189000 DIV-TEST-F1-25-4.                                                NC1714.2
189100     IF      WRK-CS-18V00  = 000000000000000099                   NC1714.2
189200             PERFORM PASS                                         NC1714.2
189300             PERFORM PRINT-DETAIL                                 NC1714.2
189400     ELSE                                                         NC1714.2
189500             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1714.2
189600             MOVE    0            TO CORRECT-N                    NC1714.2
189700             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1714.2
189800             PERFORM FAIL                                         NC1714.2
189900             PERFORM PRINT-DETAIL.                                NC1714.2
190000*                                                                 NC1714.2
190100*                                                                 NC1714.2
190200 DIV-INIT-F1-26.                                                  NC1714.2
190300*            ==--> NO SIZE ERROR CONDITION <--==                  NC1714.2
190400*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1714.2
190500     MOVE   "1V-41 6.4.3 "   TO ANSI-REFERENCE.                   NC1714.2
190600     MOVE   "DIV-TEST-F1-26       " TO PAR-NAME.                  NC1714.2
190700     MOVE    111111111.111111111 TO A18ONES-DS-09V09.             NC1714.2
190800     MOVE    A18ONES-DS-09V09    TO WRK-DS-09V09.                 NC1714.2
190900     MOVE   "0" TO WRK-XN-00001.                                  NC1714.2
191000     MOVE    0  TO WRK-DS-05V00.                                  NC1714.2
191100     MOVE    0  TO WRK-DS-02V00.                                  NC1714.2
191200     MOVE    0  TO WRK-CS-18V00.                                  NC1714.2
191300     MOVE    1 TO REC-CT.                                         NC1714.2
191400 DIV-TEST-F1-26-0.                                                NC1714.2
191500     DIVIDE  A18ONES-DS-09V09                                     NC1714.2
191600        INTO WRK-DS-09V09 ROUNDED                                 NC1714.2
191700          ON SIZE ERROR                                           NC1714.2
191800             MOVE   "1" TO WRK-XN-00001                           NC1714.2
191900             MOVE   23  TO WRK-DS-05V00                           NC1714.2
192000             MOVE   -4  TO WRK-DS-02V00                           NC1714.2
192100     END-DIVIDE                                                   NC1714.2
192200     MOVE    99 TO WRK-CS-18V00.                                  NC1714.2
192300     GO TO   DIV-TEST-F1-26-1.                                    NC1714.2
192400 DIV-DELETE-F1-26-1.                                              NC1714.2
192500     PERFORM DE-LETE.                                             NC1714.2
192600     PERFORM PRINT-DETAIL.                                        NC1714.2
192700     GO TO   DIV-INIT-F1-27.                                      NC1714.2
192800 DIV-TEST-F1-26-1.                                                NC1714.2
192900     IF      WRK-DS-18V00-S = 000000001000000000                  NC1714.2
193000             PERFORM PASS                                         NC1714.2
193100             PERFORM PRINT-DETAIL                                 NC1714.2
193200     ELSE                                                         NC1714.2
193300             MOVE    000000001000000000 TO CORRECT-18V0           NC1714.2
193400             MOVE    WRK-DS-18V00       TO COMPUTED-18V0          NC1714.2
193500             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
193600                  TO RE-MARK                                      NC1714.2
193700             PERFORM FAIL                                         NC1714.2
193800             PERFORM PRINT-DETAIL.                                NC1714.2
193900     ADD     1 TO REC-CT.                                         NC1714.2
194000 DIV-TEST-F1-26-2.                                                NC1714.2
194100     IF      WRK-XN-00001  = "0"                                  NC1714.2
194200             PERFORM PASS                                         NC1714.2
194300             PERFORM PRINT-DETAIL                                 NC1714.2
194400     ELSE                                                         NC1714.2
194500             MOVE   "0"           TO CORRECT-X                    NC1714.2
194600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
194700             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
194800                  TO RE-MARK                                      NC1714.2
194900             PERFORM FAIL                                         NC1714.2
195000             PERFORM PRINT-DETAIL.                                NC1714.2
195100     ADD     1 TO REC-CT.                                         NC1714.2
195200 DIV-TEST-F1-26-3.                                                NC1714.2
195300     IF      WRK-DS-05V00  =  0                                   NC1714.2
195400             PERFORM PASS                                         NC1714.2
195500             PERFORM PRINT-DETAIL                                 NC1714.2
195600     ELSE                                                         NC1714.2
195700             MOVE    0            TO CORRECT-18V0                 NC1714.2
195800             MOVE    WRK-DS-05V00 TO COMPUTED-18V0                NC1714.2
195900             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
196000                  TO RE-MARK                                      NC1714.2
196100             PERFORM FAIL                                         NC1714.2
196200             PERFORM PRINT-DETAIL.                                NC1714.2
196300     ADD     1 TO REC-CT.                                         NC1714.2
196400 DIV-TEST-F1-26-4.                                                NC1714.2
196500     IF      WRK-DS-02V00  =  0                                   NC1714.2
196600             PERFORM PASS                                         NC1714.2
196700             PERFORM PRINT-DETAIL                                 NC1714.2
196800     ELSE                                                         NC1714.2
196900             MOVE    0            TO CORRECT-18V0                 NC1714.2
197000             MOVE    WRK-DS-02V00 TO COMPUTED-18V0                NC1714.2
197100             PERFORM FAIL                                         NC1714.2
197200             PERFORM PRINT-DETAIL.                                NC1714.2
197300     ADD     1 TO REC-CT.                                         NC1714.2
197400 DIV-TEST-F1-26-5.                                                NC1714.2
197500     IF      WRK-CS-18V00  = 000000000000000099                   NC1714.2
197600             PERFORM PASS                                         NC1714.2
197700             PERFORM PRINT-DETAIL                                 NC1714.2
197800     ELSE                                                         NC1714.2
197900             MOVE    000000000000000099 TO CORRECT-18V0           NC1714.2
198000             MOVE    WRK-CS-18V00       TO COMPUTED-18V0          NC1714.2
198100             PERFORM FAIL                                         NC1714.2
198200             PERFORM PRINT-DETAIL.                                NC1714.2
198300*                                                                 NC1714.2
198400*                                                                 NC1714.2
198500 DIV-INIT-F1-27.                                                  NC1714.2
198600*            ==--> NO SIZE ERROR CONDITION <--==                  NC1714.2
198700*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1714.2
198800     MOVE   "DIV-TEST-F1-27 " TO PAR-NAME.                        NC1714.2
198900     MOVE   "1V-41 6.4.3 "   TO ANSI-REFERENCE.                   NC1714.2
199000     MOVE   "0" TO WRK-XN-00001.                                  NC1714.2
199100     MOVE    0  TO WRK-DS-05V00.                                  NC1714.2
199200     MOVE    0  TO WRK-DS-02V00.                                  NC1714.2
199300     MOVE    0  TO WRK-CS-18V00.                                  NC1714.2
199400     MOVE    1620.36 TO DIV1.                                     NC1714.2
199500     MOVE    44.1    TO DIV2.                                     NC1714.2
199600     MOVE    1 TO REC-CT.                                         NC1714.2
199700 DIV-TEST-F1-27-0.                                                NC1714.2
199800     DIVIDE  DIV2 INTO DIV1                                       NC1714.2
199900      NOT ON SIZE ERROR                                           NC1714.2
200000             MOVE   "1" TO WRK-XN-00001                           NC1714.2
200100             MOVE   23  TO WRK-DS-05V00                           NC1714.2
200200             MOVE   -4  TO WRK-DS-02V00                           NC1714.2
200300     END-DIVIDE                                                   NC1714.2
200400     MOVE    99 TO WRK-CS-18V00.                                  NC1714.2
200500     GO TO   DIV-TEST-F1-27-1.                                    NC1714.2
200600 DIV-DELETE-F1-27-1.                                              NC1714.2
200700     PERFORM DE-LETE.                                             NC1714.2
200800     PERFORM PRINT-DETAIL.                                        NC1714.2
200900     GO TO   DIV-INIT-F1-28.                                      NC1714.2
201000 DIV-TEST-F1-27-1.                                                NC1714.2
201100     IF      WRK-XN-00001  = "1"                                  NC1714.2
201200             PERFORM PASS                                         NC1714.2
201300             PERFORM PRINT-DETAIL                                 NC1714.2
201400     ELSE                                                         NC1714.2
201500             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
201600             MOVE   "1"           TO CORRECT-X                    NC1714.2
201700             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
201800                  TO RE-MARK                                      NC1714.2
201900             PERFORM FAIL                                         NC1714.2
202000             PERFORM PRINT-DETAIL.                                NC1714.2
202100     ADD     1 TO REC-CT.                                         NC1714.2
202200 DIV-TEST-F1-27-2.                                                NC1714.2
202300     IF      WRK-DS-05V00 = 23                                    NC1714.2
202400             PERFORM PASS                                         NC1714.2
202500             PERFORM PRINT-DETAIL                                 NC1714.2
202600     ELSE                                                         NC1714.2
202700             MOVE    23           TO CORRECT-18V0                 NC1714.2
202800             MOVE    WRK-DS-05V00 TO COMPUTED-18V0                NC1714.2
202900             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1714.2
203000                  TO RE-MARK                                      NC1714.2
203100             PERFORM FAIL                                         NC1714.2
203200             PERFORM PRINT-DETAIL.                                NC1714.2
203300     ADD     1 TO REC-CT.                                         NC1714.2
203400 DIV-TEST-F1-27-3.                                                NC1714.2
203500     IF      WRK-DS-02V00  = -4                                   NC1714.2
203600             PERFORM PASS                                         NC1714.2
203700             PERFORM PRINT-DETAIL                                 NC1714.2
203800     ELSE                                                         NC1714.2
203900             MOVE    -4           TO CORRECT-18V0                 NC1714.2
204000             MOVE    WRK-DS-02V00 TO COMPUTED-18V0                NC1714.2
204100             PERFORM FAIL                                         NC1714.2
204200             PERFORM PRINT-DETAIL.                                NC1714.2
204300     ADD     1 TO REC-CT.                                         NC1714.2
204400 DIV-TEST-F1-27-4.                                                NC1714.2
204500     IF      WRK-CS-18V00  = 000000000000000099                   NC1714.2
204600             PERFORM PASS                                         NC1714.2
204700             PERFORM PRINT-DETAIL                                 NC1714.2
204800     ELSE                                                         NC1714.2
204900             MOVE    000000000000000099 TO CORRECT-18V0           NC1714.2
205000             MOVE    WRK-CS-18V00       TO COMPUTED-18V0          NC1714.2
205100             PERFORM FAIL                                         NC1714.2
205200             PERFORM PRINT-DETAIL.                                NC1714.2
205300     ADD     1 TO REC-CT.                                         NC1714.2
205400 DIV-TEST-F1-27-5.                                                NC1714.2
205500     IF      DIV1 = 36.74                                         NC1714.2
205600             PERFORM PASS                                         NC1714.2
205700             PERFORM PRINT-DETAIL                                 NC1714.2
205800     ELSE                                                         NC1714.2
205900             MOVE    DIV1  TO COMPUTED-N                          NC1714.2
206000             MOVE    36.74 TO CORRECT-N                           NC1714.2
206100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
206200             PERFORM FAIL                                         NC1714.2
206300             PERFORM PRINT-DETAIL.                                NC1714.2
206400*                                                                 NC1714.2
206500*                                                                 NC1714.2
206600 DIV-INIT-F1-28.                                                  NC1714.2
206700     MOVE   "DIV-TEST-F1-28 " TO PAR-NAME.                        NC1714.2
206800     MOVE   "1V-41 6.4.3 "   TO ANSI-REFERENCE.                   NC1714.2
206900     MOVE   A18ONES-DS-09V09 TO WRK-DS-09V09.                     NC1714.2
207000     MOVE   "0" TO WRK-XN-00001.                                  NC1714.2
207100     MOVE    0  TO WRK-DS-05V00.                                  NC1714.2
207200     MOVE    0  TO WRK-DS-02V00.                                  NC1714.2
207300     MOVE    0  TO WRK-CS-18V00.                                  NC1714.2
207400     MOVE    1 TO REC-CT.                                         NC1714.2
207500 DIV-TEST-F1-28-0.                                                NC1714.2
207600     DIVIDE  A18ONES-DS-09V09                                     NC1714.2
207700        INTO WRK-DS-09V09 ROUNDED                                 NC1714.2
207800      NOT ON SIZE ERROR                                           NC1714.2
207900             MOVE   "1" TO WRK-XN-00001                           NC1714.2
208000             MOVE   23  TO WRK-DS-05V00                           NC1714.2
208100             MOVE   -4  TO WRK-DS-02V00                           NC1714.2
208200     END-DIVIDE                                                   NC1714.2
208300     MOVE    99 TO WRK-CS-18V00.                                  NC1714.2
208400     GO TO   DIV-TEST-F1-28-1.                                    NC1714.2
208500 DIV-DELETE-F1-28-1.                                              NC1714.2
208600     PERFORM DE-LETE.                                             NC1714.2
208700     PERFORM PRINT-DETAIL.                                        NC1714.2
208800     GO TO   DIV-INIT-F1-29.                                      NC1714.2
208900 DIV-TEST-F1-28-1.                                                NC1714.2
209000     IF      WRK-XN-00001  = "1"                                  NC1714.2
209100             PERFORM PASS                                         NC1714.2
209200             PERFORM PRINT-DETAIL                                 NC1714.2
209300     ELSE                                                         NC1714.2
209400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
209500             MOVE   "1"           TO CORRECT-X                    NC1714.2
209600             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1714.2
209700                  TO RE-MARK                                      NC1714.2
209800             PERFORM FAIL                                         NC1714.2
209900             PERFORM PRINT-DETAIL.                                NC1714.2
210000     ADD     1 TO REC-CT.                                         NC1714.2
210100 DIV-TEST-F1-28-2.                                                NC1714.2
210200     IF      WRK-DS-05V00 = 23                                    NC1714.2
210300             PERFORM PASS                                         NC1714.2
210400             PERFORM PRINT-DETAIL                                 NC1714.2
210500     ELSE                                                         NC1714.2
210600             MOVE    23           TO CORRECT-18V0                 NC1714.2
210700             MOVE    WRK-DS-05V00 TO COMPUTED-18V0                NC1714.2
210800             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1714.2
210900                  TO RE-MARK                                      NC1714.2
211000             PERFORM FAIL                                         NC1714.2
211100             PERFORM PRINT-DETAIL.                                NC1714.2
211200     ADD     1 TO REC-CT.                                         NC1714.2
211300 DIV-TEST-F1-28-3.                                                NC1714.2
211400     IF      WRK-DS-02V00  = -4                                   NC1714.2
211500             PERFORM PASS                                         NC1714.2
211600             PERFORM PRINT-DETAIL                                 NC1714.2
211700     ELSE                                                         NC1714.2
211800             MOVE    -4           TO CORRECT-18V0                 NC1714.2
211900             MOVE    WRK-DS-02V00 TO COMPUTED-18V0                NC1714.2
212000             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1714.2
212100                  TO RE-MARK                                      NC1714.2
212200             PERFORM FAIL                                         NC1714.2
212300             PERFORM PRINT-DETAIL.                                NC1714.2
212400     ADD     1 TO REC-CT.                                         NC1714.2
212500 DIV-TEST-F1-28-4.                                                NC1714.2
212600     IF      WRK-CS-18V00  = 000000000000000099                   NC1714.2
212700             PERFORM PASS                                         NC1714.2
212800             PERFORM PRINT-DETAIL                                 NC1714.2
212900     ELSE                                                         NC1714.2
213000             MOVE    000000000000000099 TO CORRECT-18V0           NC1714.2
213100             MOVE    WRK-CS-18V00       TO COMPUTED-18V0          NC1714.2
213200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1714.2
213300             PERFORM FAIL                                         NC1714.2
213400             PERFORM PRINT-DETAIL.                                NC1714.2
213500     ADD     1 TO REC-CT.                                         NC1714.2
213600 DIV-TEST-F1-28-5.                                                NC1714.2
213700     IF      WRK-DS-09V09 = 1                                     NC1714.2
213800             PERFORM PASS                                         NC1714.2
213900             PERFORM PRINT-DETAIL                                 NC1714.2
214000     ELSE                                                         NC1714.2
214100             MOVE    1            TO CORRECT-18V0                 NC1714.2
214200             MOVE    WRK-DS-09V09 TO COMPUTED-18V0                NC1714.2
214300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1714.2
214400                  TO RE-MARK                                      NC1714.2
214500             PERFORM FAIL                                         NC1714.2
214600             PERFORM PRINT-DETAIL.                                NC1714.2
214700*                                                                 NC1714.2
214800*                                                                 NC1714.2
214900 DIV-INIT-F1-29.                                                  NC1714.2
215000*            ==--> NO SIZE ERROR CONDITION <--==                  NC1714.2
215100*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1714.2
215200     MOVE   "DIV-TEST-F1-29 " TO PAR-NAME.                        NC1714.2
215300     MOVE   "1V-41 6.4.3 "    TO ANSI-REFERENCE.                  NC1714.2
215400     MOVE   "0" TO WRK-XN-00001.                                  NC1714.2
215500     MOVE    0  TO WRK-CS-18V00.                                  NC1714.2
215600     MOVE    1620.36 TO DIV1.                                     NC1714.2
215700     MOVE    44.1    TO DIV2.                                     NC1714.2
215800     MOVE    1 TO REC-CT.                                         NC1714.2
215900 DIV-TEST-F1-29-0.                                                NC1714.2
216000     DIVIDE  DIV2 INTO DIV1                                       NC1714.2
216100          ON SIZE ERROR                                           NC1714.2
216200             MOVE   "1" TO WRK-XN-00001                           NC1714.2
216300      NOT ON SIZE ERROR                                           NC1714.2
216400             MOVE   "2" TO WRK-XN-00001                           NC1714.2
216500     END-DIVIDE                                                   NC1714.2
216600     MOVE    99 TO WRK-CS-18V00.                                  NC1714.2
216700     GO TO   DIV-TEST-F1-29-1.                                    NC1714.2
216800 DIV-DELETE-F1-29-1.                                              NC1714.2
216900     PERFORM DE-LETE.                                             NC1714.2
217000     PERFORM PRINT-DETAIL.                                        NC1714.2
217100     GO TO   DIV-INIT-F1-30.                                      NC1714.2
217200 DIV-TEST-F1-29-1.                                                NC1714.2
217300     IF      WRK-XN-00001  = "2"                                  NC1714.2
217400             PERFORM PASS                                         NC1714.2
217500             PERFORM PRINT-DETAIL                                 NC1714.2
217600     ELSE                                                         NC1714.2
217700             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
217800             MOVE   "2"           TO CORRECT-X                    NC1714.2
217900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1714.2
218000                  TO RE-MARK                                      NC1714.2
218100             PERFORM FAIL                                         NC1714.2
218200             PERFORM PRINT-DETAIL.                                NC1714.2
218300     ADD     1 TO REC-CT.                                         NC1714.2
218400 DIV-TEST-F1-29-2.                                                NC1714.2
218500     IF      WRK-CS-18V00  = 000000000000000099                   NC1714.2
218600             PERFORM PASS                                         NC1714.2
218700             PERFORM PRINT-DETAIL                                 NC1714.2
218800     ELSE                                                         NC1714.2
218900             MOVE    000000000000000099 TO CORRECT-18V0           NC1714.2
219000             MOVE    WRK-CS-18V00       TO COMPUTED-18V0          NC1714.2
219100             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1714.2
219200             PERFORM FAIL                                         NC1714.2
219300             PERFORM PRINT-DETAIL.                                NC1714.2
219400     ADD     1 TO REC-CT.                                         NC1714.2
219500 DIV-TEST-F1-29-3.                                                NC1714.2
219600     IF      DIV1 = 36.74                                         NC1714.2
219700             PERFORM PASS                                         NC1714.2
219800             PERFORM PRINT-DETAIL                                 NC1714.2
219900     ELSE                                                         NC1714.2
220000             MOVE    DIV1  TO COMPUTED-N                          NC1714.2
220100             MOVE    36.74 TO CORRECT-N                           NC1714.2
220200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
220300             PERFORM FAIL                                         NC1714.2
220400             PERFORM PRINT-DETAIL.                                NC1714.2
220500*                                                                 NC1714.2
220600*                                                                 NC1714.2
220700 DIV-INIT-F1-30.                                                  NC1714.2
220800*            ==--> NO SIZE ERROR CONDITION <--==                  NC1714.2
220900*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1714.2
221000     MOVE   "DIV-TEST-F1-30 " TO PAR-NAME.                        NC1714.2
221100     MOVE   "1V-41 6.4.3 "    TO ANSI-REFERENCE.                  NC1714.2
221200     MOVE    111111111.111111111 TO A18ONES-DS-09V09.             NC1714.2
221300     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1714.2
221400     MOVE   "0" TO WRK-XN-00001.                                  NC1714.2
221500     MOVE    0  TO WRK-CS-18V00.                                  NC1714.2
221600     MOVE    1 TO REC-CT.                                         NC1714.2
221700 DIV-TEST-F1-30-0.                                                NC1714.2
221800     DIVIDE  A18ONES-DS-09V09                                     NC1714.2
221900       INTO  WRK-DS-09V09 ROUNDED                                 NC1714.2
222000          ON SIZE ERROR                                           NC1714.2
222100             MOVE   "1" TO WRK-XN-00001                           NC1714.2
222200      NOT ON SIZE ERROR                                           NC1714.2
222300             MOVE   "2" TO WRK-XN-00001                           NC1714.2
222400     END-DIVIDE                                                   NC1714.2
222500     MOVE    99 TO WRK-CS-18V00.                                  NC1714.2
222600     GO TO   DIV-TEST-F1-30-1.                                    NC1714.2
222700 DIV-DELETE-F1-30-1.                                              NC1714.2
222800     PERFORM DE-LETE.                                             NC1714.2
222900     PERFORM PRINT-DETAIL.                                        NC1714.2
223000     GO TO   CCVS-EXIT.                                           NC1714.2
223100 DIV-TEST-F1-30-1.                                                NC1714.2
223200     IF      WRK-XN-00001  = "2"                                  NC1714.2
223300             PERFORM PASS                                         NC1714.2
223400             PERFORM PRINT-DETAIL                                 NC1714.2
223500     ELSE                                                         NC1714.2
223600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1714.2
223700             MOVE   "2"           TO CORRECT-X                    NC1714.2
223800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1714.2
223900                  TO RE-MARK                                      NC1714.2
224000             PERFORM FAIL                                         NC1714.2
224100             PERFORM PRINT-DETAIL.                                NC1714.2
224200     ADD     1 TO REC-CT.                                         NC1714.2
224300 DIV-TEST-F1-30-2.                                                NC1714.2
224400     IF      WRK-CS-18V00  = 000000000000000099                   NC1714.2
224500             PERFORM PASS                                         NC1714.2
224600             PERFORM PRINT-DETAIL                                 NC1714.2
224700     ELSE                                                         NC1714.2
224800             MOVE    000000000000000099 TO CORRECT-18V0           NC1714.2
224900             MOVE    WRK-CS-18V00       TO COMPUTED-18V0          NC1714.2
225000             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1714.2
225100             PERFORM FAIL                                         NC1714.2
225200             PERFORM PRINT-DETAIL.                                NC1714.2
225300     ADD     1 TO REC-CT.                                         NC1714.2
225400 DIV-TEST-F1-30-3.                                                NC1714.2
225500     IF      WRK-DS-18V00 = 1                                     NC1714.2
225600             PERFORM PASS                                         NC1714.2
225700             PERFORM PRINT-DETAIL                                 NC1714.2
225800      ELSE                                                        NC1714.2
225900             MOVE    1            TO CORRECT-18V0                 NC1714.2
226000             MOVE    WRK-DS-18V00 TO COMPUTED-18V0                NC1714.2
226100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1714.2
226200             PERFORM FAIL                                         NC1714.2
226300             PERFORM PRINT-DETAIL.                                NC1714.2
226400*                                                                 NC1714.2
226500*                                                                 NC1714.2
226600 CCVS-EXIT SECTION.                                               NC1714.2
226700 CCVS-999999.                                                     NC1714.2
226800     GO TO CLOSE-FILES.                                           NC1714.2
      *END-OF,NC171A                                                                  
