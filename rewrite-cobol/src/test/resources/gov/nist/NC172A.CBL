      *HEADER,COBOL,NC172A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1724.2
000200 PROGRAM-ID.                                                      NC1724.2
000300     NC172A.                                                      NC1724.2
000400****************************************************************  NC1724.2
000500*                                                              *  NC1724.2
000600*    VALIDATION FOR:-                                          *  NC1724.2
000700*                                                              *  NC1724.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1724.2
000900*                                                              *  NC1724.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1724.2
001100*                                                              *  NC1724.2
001200****************************************************************  NC1724.2
001300*                                                              *  NC1724.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1724.2
001500*                                                              *  NC1724.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1724.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1724.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1724.2
001900*                                                              *  NC1724.2
002000****************************************************************  NC1724.2
002100*    THIS PROGRAM TESTS THE FORMAT 2 DIVIDE STATEMENT FOUND       NC1724.2
002200*    IN LEVEL 1.   ALL COMBINATIONS OF THE OPTIONAL PHRASES       NC1724.2
002300*    "SIZE ERROR", "NOT SIZE ERROR" AND "END-MULTIPLY" ARE        NC1724.2
002400*    TESTED, AS WELL AS THE ROUNDED OPTION.                       NC1724.2
002500*                                                                 NC1724.2
002600*    VARIOUS COMBINATIONS OF SIGNED AND UNSIGNED NUMERIC          NC1724.2
002700*    LITERALS, DISPLAY AND COMPUTATIONAL FIELDS ARE USED          NC1724.2
002800*    AS OPERANDS.                                                 NC1724.2
002900*                                                                 NC1724.2
003000                                                                  NC1724.2
003100                                                                  NC1724.2
003200 ENVIRONMENT DIVISION.                                            NC1724.2
003300 CONFIGURATION SECTION.                                           NC1724.2
003400 SOURCE-COMPUTER.                                                 NC1724.2
003500     XXXXX082.                                                    NC1724.2
003600 OBJECT-COMPUTER.                                                 NC1724.2
003700     XXXXX083.                                                    NC1724.2
003800 INPUT-OUTPUT SECTION.                                            NC1724.2
003900 FILE-CONTROL.                                                    NC1724.2
004000     SELECT PRINT-FILE ASSIGN TO                                  NC1724.2
004100     XXXXX055.                                                    NC1724.2
004200 DATA DIVISION.                                                   NC1724.2
004300 FILE SECTION.                                                    NC1724.2
004400 FD  PRINT-FILE.                                                  NC1724.2
004500 01  PRINT-REC PICTURE X(120).                                    NC1724.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 NC1724.2
004700 WORKING-STORAGE SECTION.                                         NC1724.2
004800 77  WRK-DS-18V00                PICTURE S9(18).                  NC1724.2
004900 77  A06THREES-DS-03V03          PICTURE S999V999 VALUE 333.333.  NC1724.2
005000 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC1724.2
005100 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06 PICTURE S9(12).        NC1724.2
005200 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1724.2
005300 77  WRK-DS-10V00                PICTURE S9(10).                  NC1724.2
005400 77  WRK-XN-00001                PICTURE X.                       NC1724.2
005500 77  A10ONES-DS-10V00            PICTURE S9(10)                   NC1724.2
005600                                 VALUE 1111111111.                NC1724.2
005700 77  A12THREES-DS-06V06          PICTURE S9(6)V9(6)               NC1724.2
005800                                 VALUE 333333.333333.             NC1724.2
005900 77  WRK-DS-02V00                PICTURE S99.                     NC1724.2
006000 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1724.2
006100 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1724.2
006200 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1724.2
006300 77  A05ONES-DS-00V05            PICTURE SV9(5) VALUE .11111.     NC1724.2
006400 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1724.2
006500                                 VALUE 111111111111.              NC1724.2
006600 77  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1724.2
006700 77  WRK-DS-09V08                PICTURE S9(9)V9(8).              NC1724.2
006800 77  WRK-DS-17V00-S REDEFINES WRK-DS-09V08 PICTURE S9(17).        NC1724.2
006900 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1724.2
007000                                 VALUE 111111111111111111.        NC1724.2
007100 77  WRK-DS-0201P                PICTURE S99P.                    NC1724.2
007200 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1724.2
007300 77  WRK-DU-18V00                PICTURE 9(18).                   NC1724.2
007400 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1724.2
007500                                 VALUE 99.                        NC1724.2
007600 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1724.2
007700                                 VALUE .1.                        NC1724.2
007800 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1724.2
007900 77  WRK-DS-12V00                PICTURE S9(12).                  NC1724.2
008000 77  WRK-DS-01V00                PICTURE S9.                      NC1724.2
008100 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1724.2
008200 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1724.2
008300                                 VALUE 111111111.111111111.       NC1724.2
008400 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1724.2
008500 77  WRK-DS-05V00                PICTURE S9(5).                   NC1724.2
008600 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1724.2
008700 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1724.2
008800 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1724.2
008900 77  XRAY                        PICTURE X.                       NC1724.2
009000 01  WRK-XN-18-1 PIC X(18).                                       NC1724.2
009100 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1724.2
009200 01  WRK-DU-X-18V0-1; REDEFINES WRK-XN-18-1 PIC 9(18).            NC1724.2
009300 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1724.2
009400 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1724.2
009500 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1724.2
009600 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1724.2
009700 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1724.2
009800 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1724.2
009900 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1724.2
010000 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1724.2
010100 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1724.2
010200 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1724.2
010300 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1724.2
010400 01  WRK-DU-2V0-1 PIC 99.                                         NC1724.2
010500 01  WRK-DU-2V0-2 PIC 99.                                         NC1724.2
010600 01  WRK-DU-2V0-3 PIC 99.                                         NC1724.2
010700 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1724.2
010800 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1724.2
010900 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1724.2
011000 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1724.2
011100 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1724.2
011200 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1724.2
011300 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1724.2
011400 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1724.2
011500 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1724.2
011600 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1724.2
011700 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1724.2
011800 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1724.2
011900 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1724.2
012000 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1724.2
012100 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1724.2
012200 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1724.2
012300 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1724.2
012400 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1724.2
012500 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1724.2
012600 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1724.2
012700 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1724.2
012800 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1724.2
012900 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1724.2
013000 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1724.2
013100 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1724.2
013200 01  WRK-NE-2 PIC $**.99.                                         NC1724.2
013300 01  WRK-NE-3 PIC $99.99CR.                                       NC1724.2
013400 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1724.2
013500 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1724.2
013600             VALUE +000000000000000001.                           NC1724.2
013700 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1724.2
013800             VALUE -000000000000000033.                           NC1724.2
013900 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1724.2
014000             VALUE 666666666666666666.                            NC1724.2
014100 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1724.2
014200             VALUE 009999999999999999.                            NC1724.2
014300 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1724.2
014400             VALUE 000022222222222222.                            NC1724.2
014500 01  MULTIPLY-DATA.                                               NC1724.2
014600     02 MULT1                           PICTURE IS 999V99         NC1724.2
014700     VALUE IS 80.12.                                              NC1724.2
014800     02 MULT2                           PICTURE IS 999V999.       NC1724.2
014900     02 MULT3                           PICTURE IS $$99.99.       NC1724.2
015000     02 MULT4                           PICTURE IS S99            NC1724.2
015100     VALUE IS -56.                                                NC1724.2
015200     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1724.2
015300     02 MULT6                           PICTURE IS 99 VALUE IS    NC1724.2
015400     20.                                                          NC1724.2
015500 01  DIVIDE-DATA.                                                 NC1724.2
015600     02 DIV1                            PICTURE IS 9(4)V99        NC1724.2
015700     VALUE IS 1620.36.                                            NC1724.2
015800     02 DIV2                            PICTURE IS 99V9           NC1724.2
015900     VALUE IS 44.1.                                               NC1724.2
016000     02 DIV3                            PICTURE IS 9(4)V9         NC1724.2
016100     VALUE IS 1661.7.                                             NC1724.2
016200     02 DIV4                            PICTURE IS S9V999         NC1724.2
016300     VALUE IS -9.642.                                             NC1724.2
016400     02 DIV-02LEVEL-1.                                            NC1724.2
016500     03 DIV5                            PICTURE IS V99            NC1724.2
016600     VALUE IS .82.                                                NC1724.2
016700     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1724.2
016800     03 DIV7                            PICTURE IS 9V9            NC1724.2
016900     VALUE IS 9.6.                                                NC1724.2
017000 01  DIV-DATA-2.                                                  NC1724.2
017100     02 DIV8                            PICTURE IS 99V9.          NC1724.2
017200     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1724.2
017300     02 DIV10                           PICTURE IS V999.          NC1724.2
017400 01  TEST-RESULTS.                                                NC1724.2
017500     02 FILLER                   PIC X      VALUE SPACE.          NC1724.2
017600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1724.2
017700     02 FILLER                   PIC X      VALUE SPACE.          NC1724.2
017800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1724.2
017900     02 FILLER                   PIC X      VALUE SPACE.          NC1724.2
018000     02  PAR-NAME.                                                NC1724.2
018100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1724.2
018200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1724.2
018300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1724.2
018400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1724.2
018500     02 RE-MARK                  PIC X(61).                       NC1724.2
018600 01  TEST-COMPUTED.                                               NC1724.2
018700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1724.2
018800     02 FILLER                   PIC X(17)  VALUE                 NC1724.2
018900            "       COMPUTED=".                                   NC1724.2
019000     02 COMPUTED-X.                                               NC1724.2
019100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1724.2
019200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1724.2
019300                                 PIC -9(9).9(9).                  NC1724.2
019400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1724.2
019500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1724.2
019600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1724.2
019700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1724.2
019800         04 COMPUTED-18V0                    PIC -9(18).          NC1724.2
019900         04 FILLER                           PIC X.               NC1724.2
020000     03 FILLER PIC X(50) VALUE SPACE.                             NC1724.2
020100 01  TEST-CORRECT.                                                NC1724.2
020200     02 FILLER PIC X(30) VALUE SPACE.                             NC1724.2
020300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1724.2
020400     02 CORRECT-X.                                                NC1724.2
020500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1724.2
020600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1724.2
020700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1724.2
020800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1724.2
020900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1724.2
021000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1724.2
021100         04 CORRECT-18V0                     PIC -9(18).          NC1724.2
021200         04 FILLER                           PIC X.               NC1724.2
021300     03 FILLER PIC X(2) VALUE SPACE.                              NC1724.2
021400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1724.2
021500 01  CCVS-C-1.                                                    NC1724.2
021600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1724.2
021700-    "SS  PARAGRAPH-NAME                                          NC1724.2
021800-    "       REMARKS".                                            NC1724.2
021900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1724.2
022000 01  CCVS-C-2.                                                    NC1724.2
022100     02 FILLER                     PIC X        VALUE SPACE.      NC1724.2
022200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1724.2
022300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1724.2
022400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1724.2
022500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1724.2
022600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1724.2
022700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1724.2
022800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1724.2
022900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1724.2
023000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1724.2
023100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1724.2
023200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1724.2
023300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1724.2
023400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1724.2
023500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1724.2
023600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1724.2
023700 01  CCVS-H-1.                                                    NC1724.2
023800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1724.2
023900     02  FILLER                    PIC X(42)    VALUE             NC1724.2
024000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1724.2
024100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1724.2
024200 01  CCVS-H-2A.                                                   NC1724.2
024300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1724.2
024400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1724.2
024500   02  FILLER                        PIC XXXX   VALUE             NC1724.2
024600     "4.2 ".                                                      NC1724.2
024700   02  FILLER                        PIC X(28)  VALUE             NC1724.2
024800            " COPY - NOT FOR DISTRIBUTION".                       NC1724.2
024900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1724.2
025000                                                                  NC1724.2
025100 01  CCVS-H-2B.                                                   NC1724.2
025200   02  FILLER                        PIC X(15)  VALUE             NC1724.2
025300            "TEST RESULT OF ".                                    NC1724.2
025400   02  TEST-ID                       PIC X(9).                    NC1724.2
025500   02  FILLER                        PIC X(4)   VALUE             NC1724.2
025600            " IN ".                                               NC1724.2
025700   02  FILLER                        PIC X(12)  VALUE             NC1724.2
025800     " HIGH       ".                                              NC1724.2
025900   02  FILLER                        PIC X(22)  VALUE             NC1724.2
026000            " LEVEL VALIDATION FOR ".                             NC1724.2
026100   02  FILLER                        PIC X(58)  VALUE             NC1724.2
026200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1724.2
026300 01  CCVS-H-3.                                                    NC1724.2
026400     02  FILLER                      PIC X(34)  VALUE             NC1724.2
026500            " FOR OFFICIAL USE ONLY    ".                         NC1724.2
026600     02  FILLER                      PIC X(58)  VALUE             NC1724.2
026700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1724.2
026800     02  FILLER                      PIC X(28)  VALUE             NC1724.2
026900            "  COPYRIGHT   1985 ".                                NC1724.2
027000 01  CCVS-E-1.                                                    NC1724.2
027100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1724.2
027200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1724.2
027300     02 ID-AGAIN                     PIC X(9).                    NC1724.2
027400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1724.2
027500 01  CCVS-E-2.                                                    NC1724.2
027600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1724.2
027700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1724.2
027800     02 CCVS-E-2-2.                                               NC1724.2
027900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1724.2
028000         03 FILLER                   PIC X      VALUE SPACE.      NC1724.2
028100         03 ENDER-DESC               PIC X(44)  VALUE             NC1724.2
028200            "ERRORS ENCOUNTERED".                                 NC1724.2
028300 01  CCVS-E-3.                                                    NC1724.2
028400     02  FILLER                      PIC X(22)  VALUE             NC1724.2
028500            " FOR OFFICIAL USE ONLY".                             NC1724.2
028600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1724.2
028700     02  FILLER                      PIC X(58)  VALUE             NC1724.2
028800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1724.2
028900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1724.2
029000     02 FILLER                       PIC X(15)  VALUE             NC1724.2
029100             " COPYRIGHT 1985".                                   NC1724.2
029200 01  CCVS-E-4.                                                    NC1724.2
029300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1724.2
029400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1724.2
029500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1724.2
029600     02 FILLER                       PIC X(40)  VALUE             NC1724.2
029700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1724.2
029800 01  XXINFO.                                                      NC1724.2
029900     02 FILLER                       PIC X(19)  VALUE             NC1724.2
030000            "*** INFORMATION ***".                                NC1724.2
030100     02 INFO-TEXT.                                                NC1724.2
030200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1724.2
030300       04 XXCOMPUTED                 PIC X(20).                   NC1724.2
030400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1724.2
030500       04 XXCORRECT                  PIC X(20).                   NC1724.2
030600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1724.2
030700 01  HYPHEN-LINE.                                                 NC1724.2
030800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1724.2
030900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1724.2
031000-    "*****************************************".                 NC1724.2
031100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1724.2
031200-    "******************************".                            NC1724.2
031300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1724.2
031400     "NC172A".                                                    NC1724.2
031500 PROCEDURE DIVISION.                                              NC1724.2
031600 CCVS1 SECTION.                                                   NC1724.2
031700 OPEN-FILES.                                                      NC1724.2
031800     OPEN     OUTPUT PRINT-FILE.                                  NC1724.2
031900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1724.2
032000     MOVE    SPACE TO TEST-RESULTS.                               NC1724.2
032100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1724.2
032200     GO TO CCVS1-EXIT.                                            NC1724.2
032300 CLOSE-FILES.                                                     NC1724.2
032400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1724.2
032500 TERMINATE-CCVS.                                                  NC1724.2
032600*    EXIT PROGRAM.                                                NC1724.2
032700*TERMINATE-CALL.                                                  NC1724.2
032800     STOP     RUN.                                                NC1724.2
032900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1724.2
033000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1724.2
033100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1724.2
033200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1724.2
033300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1724.2
033400 PRINT-DETAIL.                                                    NC1724.2
033500     IF REC-CT NOT EQUAL TO ZERO                                  NC1724.2
033600             MOVE "." TO PARDOT-X                                 NC1724.2
033700             MOVE REC-CT TO DOTVALUE.                             NC1724.2
033800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1724.2
033900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1724.2
034000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1724.2
034100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1724.2
034200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1724.2
034300     MOVE SPACE TO CORRECT-X.                                     NC1724.2
034400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1724.2
034500     MOVE     SPACE TO RE-MARK.                                   NC1724.2
034600 HEAD-ROUTINE.                                                    NC1724.2
034700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1724.2
034800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1724.2
034900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1724.2
035000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1724.2
035100 COLUMN-NAMES-ROUTINE.                                            NC1724.2
035200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1724.2
035300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1724.2
035400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1724.2
035500 END-ROUTINE.                                                     NC1724.2
035600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1724.2
035700 END-RTN-EXIT.                                                    NC1724.2
035800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1724.2
035900 END-ROUTINE-1.                                                   NC1724.2
036000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1724.2
036100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1724.2
036200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1724.2
036300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1724.2
036400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1724.2
036500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1724.2
036600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1724.2
036700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1724.2
036800  END-ROUTINE-12.                                                 NC1724.2
036900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1724.2
037000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1724.2
037100         MOVE "NO " TO ERROR-TOTAL                                NC1724.2
037200         ELSE                                                     NC1724.2
037300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1724.2
037400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1724.2
037500     PERFORM WRITE-LINE.                                          NC1724.2
037600 END-ROUTINE-13.                                                  NC1724.2
037700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1724.2
037800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1724.2
037900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1724.2
038000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1724.2
038100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1724.2
038200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1724.2
038300          MOVE "NO " TO ERROR-TOTAL                               NC1724.2
038400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1724.2
038500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1724.2
038600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1724.2
038700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1724.2
038800 WRITE-LINE.                                                      NC1724.2
038900     ADD 1 TO RECORD-COUNT.                                       NC1724.2
039000*    IF RECORD-COUNT GREATER 42                                   NC1724.2
039100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1724.2
039200*        MOVE SPACE TO DUMMY-RECORD                               NC1724.2
039300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1724.2
039400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1724.2
039500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1724.2
039600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1724.2
039700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1724.2
039800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1724.2
039900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1724.2
040000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1724.2
040100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1724.2
040200*        MOVE ZERO TO RECORD-COUNT.                               NC1724.2
040300     PERFORM WRT-LN.                                              NC1724.2
040400 WRT-LN.                                                          NC1724.2
040500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1724.2
040600     MOVE SPACE TO DUMMY-RECORD.                                  NC1724.2
040700 BLANK-LINE-PRINT.                                                NC1724.2
040800     PERFORM WRT-LN.                                              NC1724.2
040900 FAIL-ROUTINE.                                                    NC1724.2
041000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1724.2
041100            GO TO FAIL-ROUTINE-WRITE.                             NC1724.2
041200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1724.2
041300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1724.2
041400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1724.2
041500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1724.2
041600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1724.2
041700     GO TO  FAIL-ROUTINE-EX.                                      NC1724.2
041800 FAIL-ROUTINE-WRITE.                                              NC1724.2
041900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1724.2
042000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1724.2
042100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1724.2
042200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1724.2
042300 FAIL-ROUTINE-EX. EXIT.                                           NC1724.2
042400 BAIL-OUT.                                                        NC1724.2
042500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1724.2
042600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1724.2
042700 BAIL-OUT-WRITE.                                                  NC1724.2
042800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1724.2
042900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1724.2
043000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1724.2
043100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1724.2
043200 BAIL-OUT-EX. EXIT.                                               NC1724.2
043300 CCVS1-EXIT.                                                      NC1724.2
043400     EXIT.                                                        NC1724.2
043500 SECT-NC172A-001 SECTION.                                         NC1724.2
043600 DIV-INIT-F2-1.                                                   NC1724.2
043700     MOVE   "DIVIDE INTO GIVING" TO FEATURE.                      NC1724.2
043800     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
043900     MOVE      44.1   TO DIV2.                                    NC1724.2
044000     MOVE    ZERO     TO DIV8.                                    NC1724.2
044100 DIV-TEST-F2-0.                                                   NC1724.2
044200     DIVIDE  DIV2 INTO 864.36 GIVING DIV8.                        NC1724.2
044300 DIV-TEST-F2-1.                                                   NC1724.2
044400     IF      DIV8 EQUAL TO 19.6                                   NC1724.2
044500             PERFORM PASS                                         NC1724.2
044600     ELSE                                                         NC1724.2
044700             GO TO DIV-FAIL-F2-1.                                 NC1724.2
044800     GO TO   DIV-WRITE-F2-1.                                      NC1724.2
044900 DIV-DELETE-F2-1.                                                 NC1724.2
045000     PERFORM  DE-LETE.                                            NC1724.2
045100     GO TO    DIV-WRITE-F2-1.                                     NC1724.2
045200 DIV-FAIL-F2-1.                                                   NC1724.2
045300     PERFORM  FAIL.                                               NC1724.2
045400     MOVE     DIV8 TO COMPUTED-N.                                 NC1724.2
045500     MOVE     +19.6 TO CORRECT-N.                                 NC1724.2
045600 DIV-WRITE-F2-1.                                                  NC1724.2
045700     MOVE "DIV-TEST-F2-1 " TO PAR-NAME.                           NC1724.2
045800     PERFORM  PRINT-DETAIL.                                       NC1724.2
045900 DIV-INIT-F2-2.                                                   NC1724.2
046000     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
046100     MOVE    1620.36  TO DIV1.                                    NC1724.2
046200     MOVE    ZERO     TO DIV9.                                    NC1724.2
046300 DIV-TEST-F2-2-0.                                                 NC1724.2
046400     DIVIDE   0.533 INTO DIV1 GIVING DIV9 ROUNDED.                NC1724.2
046500 DIV-TEST-F2-2-1.                                                 NC1724.2
046600     IF       DIV9 EQUAL TO " 3,040.1"                            NC1724.2
046700              PERFORM PASS                                        NC1724.2
046800              ELSE                                                NC1724.2
046900              GO TO DIV-FAIL-F2-2.                                NC1724.2
047000     GO TO    DIV-WRITE-F2-2.                                     NC1724.2
047100 DIV-DELETE-F2-2.                                                 NC1724.2
047200     PERFORM  DE-LETE.                                            NC1724.2
047300     GO TO    DIV-WRITE-F2-2.                                     NC1724.2
047400 DIV-FAIL-F2-2.                                                   NC1724.2
047500     PERFORM  FAIL.                                               NC1724.2
047600     MOVE     DIV9 TO COMPUTED-A.                                 NC1724.2
047700     MOVE " 3,040.1" TO CORRECT-A.                                NC1724.2
047800 DIV-WRITE-F2-2.                                                  NC1724.2
047900     MOVE "DIV-TEST-F2-2" TO PAR-NAME.                            NC1724.2
048000     PERFORM  PRINT-DETAIL.                                       NC1724.2
048100 DIV-INIT-F2-3.                                                   NC1724.2
048200     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
048300     MOVE   -9.642    TO DIV4.                                    NC1724.2
048400     MOVE   44.1      TO DIV2.                                    NC1724.2
048500     MOVE    ZERO     TO DIV10.                                   NC1724.2
048600     MOVE   "A"       TO XRAY.                                    NC1724.2
048700 DIV-TEST-F2-3-0.                                                 NC1724.2
048800     DIVIDE   DIV4 INTO DIV2 GIVING DIV10 ON SIZE ERROR           NC1724.2
048900              MOVE "P" TO XRAY.                                   NC1724.2
049000 DIV-TEST-F2-3-1.                                                 NC1724.2
049100     IF       XRAY EQUAL TO "P"                                   NC1724.2
049200              PERFORM PASS                                        NC1724.2
049300              ELSE                                                NC1724.2
049400              GO TO DIV-FAIL-F2-3-1.                              NC1724.2
049500     GO TO    DIV-WRITE-F2-3-1.                                   NC1724.2
049600 DIV-DELETE-F2-3-1.                                               NC1724.2
049700     PERFORM DE-LETE.                                             NC1724.2
049800     GO TO   DIV-WRITE-F2-3-1.                                    NC1724.2
049900 DIV-FAIL-F2-3-1.                                                 NC1724.2
050000     MOVE    XRAY  TO COMPUTED-X.                                 NC1724.2
050100     MOVE   "A"    TO CORRECT-X.                                  NC1724.2
050200     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1724.2
050300     PERFORM FAIL.                                                NC1724.2
050400 DIV-WRITE-F2-3-1.                                                NC1724.2
050500     MOVE "DIV-TEST-F2-3-1" TO PAR-NAME.                          NC1724.2
050600     PERFORM PRINT-DETAIL.                                        NC1724.2
050700 DIV-TEST-F2-3-2.                                                 NC1724.2
050800     IF      DIV10 NOT EQUAL TO ZERO                              NC1724.2
050900             GO TO DIV-FAIL-F2-3-2.                               NC1724.2
051000     PERFORM PASS.                                                NC1724.2
051100     GO TO   DIV-WRITE-F2-3-2.                                    NC1724.2
051200 DIV-DELETE-F2-3-2.                                               NC1724.2
051300     PERFORM DE-LETE.                                             NC1724.2
051400     GO TO   DIV-WRITE-F2-3-2.                                    NC1724.2
051500 DIV-FAIL-F2-3-2.                                                 NC1724.2
051600     PERFORM FAIL.                                                NC1724.2
051700     MOVE    DIV10 TO COMPUTED-N.                                 NC1724.2
051800     MOVE    ZERO TO CORRECT-N.                                   NC1724.2
051900     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1724.2
052000 DIV-WRITE-F2-3-2.                                                NC1724.2
052100     MOVE "DIV-TEST-F2-3-2" TO PAR-NAME.                          NC1724.2
052200     PERFORM  PRINT-DETAIL.                                       NC1724.2
052300 DIV-INIT-F2-4.                                                   NC1724.2
052400     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
052500     MOVE    ZERO TO DIV8.                                        NC1724.2
052600     MOVE   "B" TO XRAY.                                          NC1724.2
052700 DIV-TEST-F2-4-0.                                                 NC1724.2
052800     DIVIDE  1.0051 INTO 100.50 GIVING DIV8 ROUNDED               NC1724.2
052900          ON SIZE ERROR                                           NC1724.2
053000              MOVE "Q" TO XRAY.                                   NC1724.2
053100 DIV-TEST-F2-4-1.                                                 NC1724.2
053200     IF       XRAY EQUAL TO "Q"                                   NC1724.2
053300              PERFORM PASS                                        NC1724.2
053400              ELSE                                                NC1724.2
053500              GO TO DIV-FAIL-F2-4-1.                              NC1724.2
053600     GO TO    DIV-WRITE-F2-4-1.                                   NC1724.2
053700 DIV-DELETE-F2-4-1.                                               NC1724.2
053800     PERFORM  DE-LETE.                                            NC1724.2
053900     GO TO    DIV-WRITE-F2-4-1.                                   NC1724.2
054000 DIV-FAIL-F2-4-1.                                                 NC1724.2
054100     MOVE    XRAY TO COMPUTED-X.                                  NC1724.2
054200     MOVE   "B"   TO COMPUTED-X.                                  NC1724.2
054300     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1724.2
054400     PERFORM FAIL.                                                NC1724.2
054500 DIV-WRITE-F2-4-1.                                                NC1724.2
054600     MOVE "DIV-TEST-F2-4-1" TO PAR-NAME.                          NC1724.2
054700     PERFORM PRINT-DETAIL.                                        NC1724.2
054800 DIV-TEST-F2-4-2.                                                 NC1724.2
054900     IF      DIV8 NOT EQUAL TO ZERO                               NC1724.2
055000             GO TO DIV-FAIL-F2-4-2.                               NC1724.2
055100     PERFORM PASS.                                                NC1724.2
055200     GO TO   DIV-WRITE-F2-4-2.                                    NC1724.2
055300 DIV-DELETE-F2-4-2.                                               NC1724.2
055400     PERFORM DE-LETE.                                             NC1724.2
055500     GO TO   DIV-WRITE-F2-4-2.                                    NC1724.2
055600 DIV-FAIL-F2-4-2.                                                 NC1724.2
055700     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1724.2
055800     PERFORM FAIL.                                                NC1724.2
055900     MOVE    DIV8 TO COMPUTED-N.                                  NC1724.2
056000     MOVE     000 TO CORRECT-N.                                   NC1724.2
056100 DIV-WRITE-F2-4-2.                                                NC1724.2
056200     MOVE   "DIV-TEST-F2-4-2" TO PAR-NAME.                        NC1724.2
056300     PERFORM PRINT-DETAIL.                                        NC1724.2
056400 DIV-INIT-F2-5.                                                   NC1724.2
056500     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
056600     MOVE    ZERO TO WRK-DS-01V00.                                NC1724.2
056700 DIV-TEST-F2-5-0.                                                 NC1724.2
056800     DIVIDE -10.9 INTO A02TWOS-DU-02V00 GIVING WRK-DS-01V00.      NC1724.2
056900 DIV-TEST-F2-5-1.                                                 NC1724.2
057000     IF      WRK-DS-01V00 EQUAL TO -2                             NC1724.2
057100             PERFORM PASS                                         NC1724.2
057200             GO TO DIV-WRITE-F2-5.                                NC1724.2
057300     GO TO   DIV-FAIL-F2-5.                                       NC1724.2
057400 DIV-DELETE-F2-5.                                                 NC1724.2
057500     PERFORM DE-LETE.                                             NC1724.2
057600     GO TO   DIV-WRITE-F2-5.                                      NC1724.2
057700 DIV-FAIL-F2-5.                                                   NC1724.2
057800     MOVE  -2                 TO CORRECT-N.                       NC1724.2
057900     MOVE  WRK-DS-01V00       TO COMPUTED-N.                      NC1724.2
058000     PERFORM FAIL.                                                NC1724.2
058100 DIV-WRITE-F2-5.                                                  NC1724.2
058200     MOVE "DIV-TEST-F2-5         " TO PAR-NAME.                   NC1724.2
058300     PERFORM PRINT-DETAIL.                                        NC1724.2
058400 DIV-INIT-F2-6.                                                   NC1724.2
058500     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
058600     MOVE 0.0000000001 TO WRK-DS-03V10.                           NC1724.2
058700     MOVE ZERO TO WRK-DS-18V00.                                   NC1724.2
058800 DIV-TEST-F2-6-0.                                                 NC1724.2
058900     DIVIDE WRK-DS-03V10 INTO A01ONE-DS-P0801                     NC1724.2
059000         GIVING WRK-DS-18V00 ROUNDED.                             NC1724.2
059100 DIV-TEST-F2-6-1.                                                 NC1724.2
059200     IF WRK-DS-18V00 EQUAL TO 000000000000000010                  NC1724.2
059300         PERFORM PASS                                             NC1724.2
059400         GO TO DIV-WRITE-F2-6.                                    NC1724.2
059500     GO TO DIV-FAIL-F2-6.                                         NC1724.2
059600 DIV-DELETE-F2-6.                                                 NC1724.2
059700     PERFORM DE-LETE.                                             NC1724.2
059800     GO TO DIV-WRITE-F2-6.                                        NC1724.2
059900 DIV-FAIL-F2-6.                                                   NC1724.2
060000     MOVE  000000000000000010 TO CORRECT-18V0.                    NC1724.2
060100     MOVE  WRK-DS-18V00       TO COMPUTED-18V0.                   NC1724.2
060200     PERFORM FAIL.                                                NC1724.2
060300 DIV-WRITE-F2-6.                                                  NC1724.2
060400     MOVE "DIV-TEST-F2-6         " TO PAR-NAME.                   NC1724.2
060500     PERFORM PRINT-DETAIL.                                        NC1724.2
060600 DIV-INIT-F2-7.                                                   NC1724.2
060700     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
060800     MOVE ZERO TO WRK-DS-18V00  AZERO-DS-05V05.                   NC1724.2
060900     MOVE "0" TO WRK-XN-00001.                                    NC1724.2
061000     MOVE  99 TO A99-DS-02V00.                                    NC1724.2
061100 DIV-TEST-F2-7-0.                                                 NC1724.2
061200     DIVIDE AZERO-DS-05V05 INTO A99-DS-02V00                      NC1724.2
061300         GIVING WRK-DS-18V00 ON SIZE ERROR                        NC1724.2
061400         MOVE "1" TO WRK-XN-00001.                                NC1724.2
061500 DIV-TEST-F2-7-1.                                                 NC1724.2
061600     IF WRK-DS-18V00 EQUAL TO 000000000000000000                  NC1724.2
061700         PERFORM PASS                                             NC1724.2
061800         GO TO DIV-WRITE-F2-7-1.                                  NC1724.2
061900     GO TO DIV-FAIL-F2-7-1.                                       NC1724.2
062000 DIV-DELETE-F2-7-1.                                               NC1724.2
062100     PERFORM DE-LETE.                                             NC1724.2
062200     GO TO DIV-WRITE-F2-7-1.                                      NC1724.2
062300 DIV-FAIL-F2-7-1.                                                 NC1724.2
062400     MOVE  000000000000000000 TO CORRECT-18V0.                    NC1724.2
062500     MOVE  WRK-DS-18V00       TO COMPUTED-18V0.                   NC1724.2
062600     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1724.2
062700     PERFORM FAIL.                                                NC1724.2
062800 DIV-WRITE-F2-7-1.                                                NC1724.2
062900     MOVE "DIV-TEST-F2-7-1         " TO PAR-NAME.                 NC1724.2
063000     PERFORM PRINT-DETAIL.                                        NC1724.2
063100 DIV-TEST-F2-7-2.                                                 NC1724.2
063200     IF WRK-XN-00001 EQUAL TO "1"                                 NC1724.2
063300         PERFORM PASS                                             NC1724.2
063400         GO TO DIV-WRITE-F2-7-2.                                  NC1724.2
063500     MOVE "1" TO CORRECT-A.                                       NC1724.2
063600     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1724.2
063700     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1724.2
063800     PERFORM FAIL.                                                NC1724.2
063900     GO TO DIV-WRITE-F2-7-2.                                      NC1724.2
064000 DIV-DELETE-F2-7-2.                                               NC1724.2
064100     PERFORM DE-LETE.                                             NC1724.2
064200 DIV-WRITE-F2-7-2.                                                NC1724.2
064300     MOVE "DIV-TEST-F2-7-2         " TO PAR-NAME.                 NC1724.2
064400     PERFORM PRINT-DETAIL.                                        NC1724.2
064500 DIV-INIT-F2-8.                                                   NC1724.2
064600     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
064700     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1724.2
064800     MOVE   "1" TO WRK-XN-00001.                                  NC1724.2
064900 DIV-TEST-F2-8-0.                                                 NC1724.2
065000     DIVIDE  A18ONES-DS-09V09 INTO WRK-DS-09V09 GIVING            NC1724.2
065100         WRK-DS-09V09 ON SIZE ERROR MOVE "0" TO WRK-XN-00001.     NC1724.2
065200 DIV-TEST-F2-8-1.                                                 NC1724.2
065300     IF WRK-DS-18V00-S EQUAL TO 000000001000000000                NC1724.2
065400         PERFORM PASS                                             NC1724.2
065500         GO TO DIV-WRITE-F2-8-1.                                  NC1724.2
065600     GO TO DIV-FAIL-F2-8-1.                                       NC1724.2
065700 DIV-DELETE-F2-8-1.                                               NC1724.2
065800     PERFORM DE-LETE.                                             NC1724.2
065900     GO TO DIV-WRITE-F2-8-1.                                      NC1724.2
066000 DIV-FAIL-F2-8-1.                                                 NC1724.2
066100     MOVE  000000001000000000 TO CORRECT-18V0.                    NC1724.2
066200     MOVE WRK-DS-18V00-S TO COMPUTED-18V0.                        NC1724.2
066300     PERFORM FAIL.                                                NC1724.2
066400 DIV-WRITE-F2-8-1.                                                NC1724.2
066500     MOVE "DIV-TEST-F2-8-1         " TO PAR-NAME.                 NC1724.2
066600     PERFORM PRINT-DETAIL.                                        NC1724.2
066700 DIV-TEST-F2-8-2.                                                 NC1724.2
066800     IF WRK-XN-00001 EQUAL TO "0"                                 NC1724.2
066900         MOVE "0" TO COMPUTED-A                                   NC1724.2
067000         MOVE "1" TO CORRECT-A                                    NC1724.2
067100         MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1724.2
067200         PERFORM FAIL                                             NC1724.2
067300         GO TO DIV-WRITE-F2-8-2.                                  NC1724.2
067400     PERFORM PASS.                                                NC1724.2
067500     GO TO DIV-WRITE-F2-8-2.                                      NC1724.2
067600 DIV-DELETE-F2-8-2.                                               NC1724.2
067700     PERFORM DE-LETE.                                             NC1724.2
067800 DIV-WRITE-F2-8-2.                                                NC1724.2
067900     MOVE "DIV-TEST-F2-8-2         " TO PAR-NAME.                 NC1724.2
068000     PERFORM PRINT-DETAIL.                                        NC1724.2
068100 DIV-INIT-F2-9.                                                   NC1724.2
068200     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
068300     MOVE ZERO TO WRK-DS-0201P.                                   NC1724.2
068400     MOVE -0.005 TO WRK-DS-09V09.                                 NC1724.2
068500     MOVE "0" TO WRK-XN-00001.                                    NC1724.2
068600 DIV-TEST-F2-9-0.                                                 NC1724.2
068700     DIVIDE WRK-DS-09V09 INTO A05ONES-DS-00V05 GIVING             NC1724.2
068800         WRK-DS-0201P ROUNDED ON SIZE ERROR                       NC1724.2
068900         MOVE "1" TO WRK-XN-00001.                                NC1724.2
069000 DIV-TEST-F2-9-1.                                                 NC1724.2
069100     MOVE WRK-DS-0201P TO WRK-DS-05V00.                           NC1724.2
069200     IF WRK-DS-05V00 EQUAL TO  -00020                             NC1724.2
069300         PERFORM PASS                                             NC1724.2
069400         GO TO DIV-WRITE-F2-9-1.                                  NC1724.2
069500     GO TO DIV-FAIL-F2-9-1.                                       NC1724.2
069600 DIV-DELETE-F2-9-1.                                               NC1724.2
069700     PERFORM DE-LETE.                                             NC1724.2
069800     GO TO DIV-WRITE-F2-9-1.                                      NC1724.2
069900 DIV-FAIL-F2-9-1.                                                 NC1724.2
070000     MOVE -00020 TO CORRECT-N.                                    NC1724.2
070100     MOVE WRK-DS-05V00 TO COMPUTED-N.                             NC1724.2
070200     PERFORM FAIL.                                                NC1724.2
070300 DIV-WRITE-F2-9-1.                                                NC1724.2
070400     MOVE "DIV-TEST-F2-9-1         " TO PAR-NAME.                 NC1724.2
070500     PERFORM PRINT-DETAIL.                                        NC1724.2
070600 DIV-TEST-F2-9-2.                                                 NC1724.2
070700     IF WRK-XN-00001 EQUAL TO "0"                                 NC1724.2
070800         PERFORM PASS                                             NC1724.2
070900         GO TO DIV-WRITE-F2-9-2.                                  NC1724.2
071000     MOVE "0" TO CORRECT-A.                                       NC1724.2
071100     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1724.2
071200     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.    NC1724.2
071300     PERFORM FAIL.                                                NC1724.2
071400     GO TO DIV-WRITE-F2-9-2.                                      NC1724.2
071500 DIV-DELETE-F2-9-2.                                               NC1724.2
071600     PERFORM DE-LETE.                                             NC1724.2
071700 DIV-WRITE-F2-9-2.                                                NC1724.2
071800     MOVE "DIV-TEST-F2-9-2         " TO PAR-NAME.                 NC1724.2
071900     PERFORM PRINT-DETAIL.                                        NC1724.2
072000 DIV-INIT-F2-10.                                                  NC1724.2
072100     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
072200     MOVE "1" TO WRK-XN-00001.                                    NC1724.2
072300     MOVE ZERO TO WRK-DS-01V00.                                   NC1724.2
072400 DIV-TEST-F2-10-1.                                                NC1724.2
072500     DIVIDE A02TWOS-DU-02V00 INTO A02TWOS-DS-03V02  GIVING        NC1724.2
072600         WRK-DS-01V00 ROUNDED ON SIZE ERROR                       NC1724.2
072700         MOVE "0" TO WRK-XN-00001.                                NC1724.2
072800     IF WRK-DS-01V00 EQUAL TO +1                                  NC1724.2
072900         PERFORM PASS                                             NC1724.2
073000         GO TO DIV-WRITE-F2-10-1.                                 NC1724.2
073100     GO TO DIV-FAIL-F2-10-1.                                      NC1724.2
073200 DIV-DELETE-F2-10-1.                                              NC1724.2
073300     PERFORM DE-LETE.                                             NC1724.2
073400     GO TO DIV-WRITE-F2-10-1.                                     NC1724.2
073500 DIV-FAIL-F2-10-1.                                                NC1724.2
073600     MOVE  +1                 TO CORRECT-N.                       NC1724.2
073700     MOVE  WRK-DS-01V00       TO COMPUTED-N.                      NC1724.2
073800     PERFORM FAIL.                                                NC1724.2
073900 DIV-WRITE-F2-10-1.                                               NC1724.2
074000     MOVE "DIV-TEST-F2-10-1         " TO PAR-NAME.                NC1724.2
074100     PERFORM PRINT-DETAIL.                                        NC1724.2
074200 DIV-TEST-F2-10-2.                                                NC1724.2
074300     IF WRK-XN-00001 EQUAL TO "1"                                 NC1724.2
074400         PERFORM PASS                                             NC1724.2
074500         GO TO DIV-WRITE-F2-10-2.                                 NC1724.2
074600     MOVE "1" TO CORRECT-A.                                       NC1724.2
074700     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1724.2
074800     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.    NC1724.2
074900     PERFORM FAIL.                                                NC1724.2
075000     GO TO DIV-WRITE-F2-10-2.                                     NC1724.2
075100 DIV-DELETE-F2-10-2.                                              NC1724.2
075200     PERFORM DE-LETE.                                             NC1724.2
075300 DIV-WRITE-F2-10-2.                                               NC1724.2
075400     MOVE "DIV-TEST-F2-10-2         " TO PAR-NAME.                NC1724.2
075500     PERFORM PRINT-DETAIL.                                        NC1724.2
075600 DIV-INIT-F2-11.                                                  NC1724.2
075700     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
075800     MOVE     0 TO WRK-DS-05V00.                                  NC1724.2
075900 DIV-TEST-F2-11.                                                  NC1724.2
076000     DIVIDE A01ONE-CS-00V01 INTO A99-CS-02V00 GIVING              NC1724.2
076100         WRK-DS-05V00.                                            NC1724.2
076200     IF WRK-DS-05V00 EQUAL TO 00990                               NC1724.2
076300        PERFORM PASS                                              NC1724.2
076400         GO TO DIV-WRITE-F2-11.                                   NC1724.2
076500     GO TO DIV-FAIL-F2-11.                                        NC1724.2
076600 DIV-DELETE-F2-11.                                                NC1724.2
076700     PERFORM DE-LETE.                                             NC1724.2
076800     GO TO DIV-WRITE-F2-11.                                       NC1724.2
076900 DIV-FAIL-F2-11.                                                  NC1724.2
077000     MOVE  00990              TO CORRECT-N.                       NC1724.2
077100     MOVE  WRK-DS-05V00       TO COMPUTED-N.                      NC1724.2
077200     PERFORM FAIL.                                                NC1724.2
077300 DIV-WRITE-F2-11.                                                 NC1724.2
077400     MOVE "DIVIDE INTO GIVING  " TO FEATURE.                      NC1724.2
077500     MOVE "DIV-TEST-F2-11         " TO PAR-NAME.                  NC1724.2
077600     PERFORM PRINT-DETAIL.                                        NC1724.2
077700 DIV-INIT-F2-12.                                                  NC1724.2
077800     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
077900     MOVE  ZERO TO WRK-CS-18V00.                                  NC1724.2
078000 DIV-TEST-F2-12-0.                                                NC1724.2
078100     DIVIDE A02THREES-CS-18V00 INTO A18SIXES-CU-18V00 GIVING      NC1724.2
078200             WRK-CS-18V00.                                        NC1724.2
078300 DIV-TEST-F2-12-1.                                                NC1724.2
078400     IF WRK-CS-18V00 EQUAL TO -020202020202020202                 NC1724.2
078500         PERFORM PASS                                             NC1724.2
078600         GO TO DIV-WRITE-F2-12.                                   NC1724.2
078700     MOVE -020202020202020202 TO CORRECT-18V0.                    NC1724.2
078800     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1724.2
078900     PERFORM FAIL.                                                NC1724.2
079000     GO TO DIV-WRITE-F2-12.                                       NC1724.2
079100 DIV-DELETE-F2-12.                                                NC1724.2
079200     PERFORM DE-LETE.                                             NC1724.2
079300 DIV-WRITE-F2-12.                                                 NC1724.2
079400     MOVE "DIV-TEST-F2-12 " TO PAR-NAME.                          NC1724.2
079500     PERFORM PRINT-DETAIL.                                        NC1724.2
079600 DIV-INIT-F2-13.                                                  NC1724.2
079700     MOVE   "V1-81 6.11.4 GR2"   TO ANSI-REFERENCE.               NC1724.2
079800     MOVE ZERO TO WRK-CS-18V00.                                   NC1724.2
079900 DIV-TEST-F2-13.                                                  NC1724.2
080000     DIVIDE A02THREES-CS-18V00 INTO A02THREES-CS-18V00            NC1724.2
080100             GIVING WRK-CS-18V00.                                 NC1724.2
080200     IF WRK-CS-18V00 EQUAL TO 000000000000000001                  NC1724.2
080300         PERFORM PASS                                             NC1724.2
080400         GO TO DIV-WRITE-F2-13.                                   NC1724.2
080500     MOVE 000000000000000001 TO CORRECT-18V0.                     NC1724.2
080600     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1724.2
080700     PERFORM FAIL.                                                NC1724.2
080800     GO TO DIV-WRITE-F2-13.                                       NC1724.2
080900 DIV-DELETE-F2-13.                                                NC1724.2
081000     PERFORM DE-LETE.                                             NC1724.2
081100 DIV-WRITE-F2-13.                                                 NC1724.2
081200     MOVE "DIV-TEST-F2-13 " TO PAR-NAME.                          NC1724.2
081300     PERFORM PRINT-DETAIL.                                        NC1724.2
081400*                                                                 NC1724.2
081500*                                                                 NC1724.2
081600 DIV-INIT-F2-14.                                                  NC1724.2
081700*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
081800*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
081900     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
082000     MOVE   "DIV-TEST-F2-14-1" TO PAR-NAME.                       NC1724.2
082100     MOVE   "P"     TO XRAY.                                      NC1724.2
082200     MOVE    0      TO DIV10.                                     NC1724.2
082300     MOVE    1      TO REC-CT.                                    NC1724.2
082400     MOVE    44.1   TO DIV2.                                      NC1724.2
082500     MOVE   -9.642  TO DIV4.                                      NC1724.2
082600 DIV-TEST-F2-14-0.                                                NC1724.2
082700     DIVIDE  DIV4 INTO DIV2                                       NC1724.2
082800      GIVING DIV10                                                NC1724.2
082900      NOT ON SIZE ERROR                                           NC1724.2
083000             MOVE   "N" TO XRAY.                                  NC1724.2
083100     GO TO   DIV-TEST-F2-14-1.                                    NC1724.2
083200 DIV-DELETE-F2-14-1.                                              NC1724.2
083300     PERFORM DE-LETE.                                             NC1724.2
083400     PERFORM PRINT-DETAIL.                                        NC1724.2
083500     GO TO   DIV-INIT-F2-15.                                      NC1724.2
083600 DIV-TEST-F2-14-1.                                                NC1724.2
083700     MOVE   "DIV-TEST-F2-14-1" TO PAR-NAME.                       NC1724.2
083800     IF      DIV10 NOT = 0                                        NC1724.2
083900             MOVE    DIV10 TO COMPUTED-N                          NC1724.2
084000             MOVE    0 TO CORRECT-N                               NC1724.2
084100             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
084200             PERFORM FAIL                                         NC1724.2
084300             PERFORM PRINT-DETAIL                                 NC1724.2
084400     ELSE                                                         NC1724.2
084500             PERFORM PASS                                         NC1724.2
084600             PERFORM PRINT-DETAIL.                                NC1724.2
084700     ADD     1 TO REC-CT.                                         NC1724.2
084800 DIV-TEST-F2-14-2.                                                NC1724.2
084900     MOVE   "DIV-TEST-F2-14-2" TO PAR-NAME.                       NC1724.2
085000     IF      XRAY  NOT = "P"                                      NC1724.2
085100             MOVE    XRAY  TO COMPUTED-X                          NC1724.2
085200             MOVE   "P"    TO CORRECT-X                           NC1724.2
085300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
085400             PERFORM FAIL                                         NC1724.2
085500             PERFORM PRINT-DETAIL                                 NC1724.2
085600     ELSE                                                         NC1724.2
085700             PERFORM PASS                                         NC1724.2
085800             PERFORM PRINT-DETAIL.                                NC1724.2
085900*                                                                 NC1724.2
086000*                                                                 NC1724.2
086100 DIV-INIT-F2-15.                                                  NC1724.2
086200*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
086300*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
086400     MOVE   "DIV-TEST-F2-15-1" TO PAR-NAME.                       NC1724.2
086500     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
086600     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1724.2
086700     MOVE    1  TO REC-CT.                                        NC1724.2
086800     MOVE   "1" TO WRK-XN-00001.                                  NC1724.2
086900 DIV-TEST-F2-15-0.                                                NC1724.2
087000     DIVIDE  A18ONES-DS-09V09 INTO WRK-DS-09V09                   NC1724.2
087100      GIVING WRK-DS-09V09                                         NC1724.2
087200      NOT ON SIZE ERROR                                           NC1724.2
087300             MOVE   "0" TO WRK-XN-00001.                          NC1724.2
087400     GO TO   DIV-TEST-F2-15-1.                                    NC1724.2
087500 DIV-DELETE-F2-15-1.                                              NC1724.2
087600     PERFORM DE-LETE.                                             NC1724.2
087700     PERFORM PRINT-DETAIL.                                        NC1724.2
087800     GO TO   DIV-INIT-F2-16.                                      NC1724.2
087900 DIV-TEST-F2-15-1.                                                NC1724.2
088000     MOVE   "DIV-TEST-F2-15-1 " TO PAR-NAME.                      NC1724.2
088100     IF      WRK-DS-18V00-S NOT = 000000001000000000              NC1724.2
088200             MOVE    000000001000000000 TO CORRECT-18V0           NC1724.2
088300             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1724.2
088400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1724.2
088500                  TO RE-MARK                                      NC1724.2
088600             PERFORM FAIL                                         NC1724.2
088700             PERFORM PRINT-DETAIL                                 NC1724.2
088800     ELSE                                                         NC1724.2
088900             PERFORM PASS                                         NC1724.2
089000             PERFORM PRINT-DETAIL.                                NC1724.2
089100     ADD     1 TO REC-CT.                                         NC1724.2
089200 DIV-TEST-F2-15-2.                                                NC1724.2
089300     MOVE   "DIV-TEST-F2-15-2" TO PAR-NAME.                       NC1724.2
089400     IF      WRK-XN-00001 NOT = "0"                               NC1724.2
089500             MOVE   "0"           TO CORRECT-X                    NC1724.2
089600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
089700             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
089800                  TO RE-MARK                                      NC1724.2
089900             PERFORM FAIL                                         NC1724.2
090000             PERFORM PRINT-DETAIL                                 NC1724.2
090100     ELSE                                                         NC1724.2
090200             PERFORM PASS                                         NC1724.2
090300             PERFORM PRINT-DETAIL.                                NC1724.2
090400*                                                                 NC1724.2
090500*                                                                 NC1724.2
090600 DIV-INIT-F2-16.                                                  NC1724.2
090700*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
090800*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
090900     MOVE   "DIV-TEST-F2-16-1" TO PAR-NAME.                       NC1724.2
091000     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
091100     MOVE   "P"     TO XRAY.                                      NC1724.2
091200     MOVE    0      TO DIV10.                                     NC1724.2
091300     MOVE    1      TO REC-CT.                                    NC1724.2
091400     MOVE    44.1   TO DIV2.                                      NC1724.2
091500     MOVE   -9.642  TO DIV4.                                      NC1724.2
091600 DIV-TEST-F2-16-0.                                                NC1724.2
091700     DIVIDE  DIV4 INTO DIV2                                       NC1724.2
091800      GIVING DIV10                                                NC1724.2
091900          ON SIZE ERROR                                           NC1724.2
092000             MOVE   "E" TO XRAY                                   NC1724.2
092100      NOT ON SIZE ERROR                                           NC1724.2
092200             MOVE   "N" TO XRAY.                                  NC1724.2
092300     GO TO   DIV-TEST-F2-16-1.                                    NC1724.2
092400 DIV-DELETE-F2-16-1.                                              NC1724.2
092500     PERFORM DE-LETE.                                             NC1724.2
092600     PERFORM PRINT-DETAIL.                                        NC1724.2
092700     GO TO   DIV-INIT-F2-17.                                      NC1724.2
092800 DIV-TEST-F2-16-1.                                                NC1724.2
092900     MOVE   "DIV-TEST-F2-16-1" TO PAR-NAME.                       NC1724.2
093000     IF      DIV10 NOT = 0                                        NC1724.2
093100             MOVE    DIV10 TO COMPUTED-N                          NC1724.2
093200             MOVE    0 TO CORRECT-N                               NC1724.2
093300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
093400             PERFORM FAIL                                         NC1724.2
093500             PERFORM PRINT-DETAIL                                 NC1724.2
093600     ELSE                                                         NC1724.2
093700             PERFORM PASS                                         NC1724.2
093800             PERFORM PRINT-DETAIL.                                NC1724.2
093900     ADD     1 TO REC-CT.                                         NC1724.2
094000 DIV-TEST-F2-16-2.                                                NC1724.2
094100     MOVE   "DIV-TEST-F2-16-2" TO PAR-NAME.                       NC1724.2
094200     IF      XRAY  NOT = "E"                                      NC1724.2
094300             MOVE    XRAY  TO COMPUTED-X                          NC1724.2
094400             MOVE   "E"    TO CORRECT-X                           NC1724.2
094500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
094600             PERFORM FAIL                                         NC1724.2
094700             PERFORM PRINT-DETAIL                                 NC1724.2
094800     ELSE                                                         NC1724.2
094900             PERFORM PASS                                         NC1724.2
095000             PERFORM PRINT-DETAIL.                                NC1724.2
095100*                                                                 NC1724.2
095200*                                                                 NC1724.2
095300 DIV-INIT-F2-17.                                                  NC1724.2
095400*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
095500*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
095600     MOVE   "DIV-TEST-F2-17-1" TO PAR-NAME.                       NC1724.2
095700     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
095800     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1724.2
095900     MOVE   "0" TO WRK-XN-00001.                                  NC1724.2
096000     MOVE    1      TO REC-CT.                                    NC1724.2
096100 DIV-TEST-F2-17-0.                                                NC1724.2
096200     DIVIDE  A18ONES-DS-09V09 INTO WRK-DS-09V09                   NC1724.2
096300      GIVING WRK-DS-09V09                                         NC1724.2
096400          ON SIZE ERROR                                           NC1724.2
096500             MOVE   "1" TO WRK-XN-00001                           NC1724.2
096600      NOT ON SIZE ERROR                                           NC1724.2
096700             MOVE   "2" TO WRK-XN-00001.                          NC1724.2
096800     GO TO   DIV-TEST-F2-17-1.                                    NC1724.2
096900 DIV-DELETE-F2-17-1.                                              NC1724.2
097000     PERFORM DE-LETE.                                             NC1724.2
097100     PERFORM PRINT-DETAIL.                                        NC1724.2
097200     GO TO   DIV-INIT-F2-18.                                      NC1724.2
097300 DIV-TEST-F2-17-1.                                                NC1724.2
097400     MOVE   "DIV-TEST-F2-17-1" TO PAR-NAME.                       NC1724.2
097500     IF      WRK-DS-18V00-S NOT = 000000001000000000              NC1724.2
097600             MOVE    000000001000000000 TO CORRECT-18V0           NC1724.2
097700             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1724.2
097800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1724.2
097900                  TO RE-MARK                                      NC1724.2
098000             PERFORM FAIL                                         NC1724.2
098100             PERFORM PRINT-DETAIL                                 NC1724.2
098200     ELSE                                                         NC1724.2
098300             PERFORM PASS                                         NC1724.2
098400             PERFORM PRINT-DETAIL.                                NC1724.2
098500     ADD     1 TO REC-CT.                                         NC1724.2
098600 DIV-TEST-F2-17-2.                                                NC1724.2
098700     MOVE   "DIV-TEST-F2-17-2" TO PAR-NAME.                       NC1724.2
098800     IF      WRK-XN-00001 NOT = "2"                               NC1724.2
098900             MOVE   "2"           TO CORRECT-X                    NC1724.2
099000             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
099100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
099200                  TO RE-MARK                                      NC1724.2
099300             PERFORM FAIL                                         NC1724.2
099400             PERFORM PRINT-DETAIL                                 NC1724.2
099500     ELSE                                                         NC1724.2
099600             PERFORM PASS                                         NC1724.2
099700             PERFORM PRINT-DETAIL.                                NC1724.2
099800*                                                                 NC1724.2
099900*                                                                 NC1724.2
100000 DIV-INIT-F2-18.                                                  NC1724.2
100100     MOVE "DIVIDE-INTO-GIVING" TO FEATURE.                        NC1724.2
100200*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
100300*            ==-->  MULTIPLE RESULT FIELDS <--==                  NC1724.2
100400     MOVE   "DIV-TEST-F2-18" TO PAR-NAME.                         NC1724.2
100500     MOVE   "V1-81 6.11.4 GR2" TO ANSI-REFERENCE.                 NC1724.2
100600     MOVE    1 TO REC-CT.                                         NC1724.2
100700     MOVE   10 TO WRK-DU-2V0-1.                                   NC1724.2
100800     MOVE  3.9 TO WRK-DU-1V1-2.                                   NC1724.2
100900     MOVE    0 TO WRK-DU-2V1-1.                                   NC1724.2
101000     MOVE    0 TO WRK-DU-2V1-2.                                   NC1724.2
101100     MOVE    0 TO WRK-DU-2V0-2.                                   NC1724.2
101200     MOVE    0 TO WRK-DU-2V1-3.                                   NC1724.2
101300     MOVE    0 TO WRK-DU-2V0-3.                                   NC1724.2
101400 DIV-TEST-F2-18-0.                                                NC1724.2
101500     DIVIDE  WRK-DU-1V1-2 INTO WRK-DU-2V0-1                       NC1724.2
101600      GIVING WRK-DU-2V1-1                                         NC1724.2
101700             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
101800             WRK-DU-2V1-2                                         NC1724.2
101900             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
102000             WRK-DU-2V1-3                                         NC1724.2
102100             WRK-DU-2V0-3.                                        NC1724.2
102200     GO TO   DIV-TEST-F2-18-1.                                    NC1724.2
102300 DIV-DELETE-F2-18.                                                NC1724.2
102400     PERFORM DE-LETE.                                             NC1724.2
102500     PERFORM PRINT-DETAIL.                                        NC1724.2
102600     GO TO   DIV-INIT-F2-19.                                      NC1724.2
102700 DIV-TEST-F2-18-1.                                                NC1724.2
102800     IF      WRK-DU-2V1-1 = 2.5                                   NC1724.2
102900             PERFORM PASS                                         NC1724.2
103000             PERFORM PRINT-DETAIL                                 NC1724.2
103100     ELSE                                                         NC1724.2
103200             PERFORM FAIL                                         NC1724.2
103300             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
103400             MOVE    2.5          TO CORRECT-N                    NC1724.2
103500             PERFORM PRINT-DETAIL.                                NC1724.2
103600     ADD 1 TO REC-CT.                                             NC1724.2
103700 DIV-TEST-F2-18-2.                                                NC1724.2
103800     IF      WRK-DU-2V0-1 = 3                                     NC1724.2
103900             PERFORM PASS                                         NC1724.2
104000             PERFORM PRINT-DETAIL                                 NC1724.2
104100     ELSE                                                         NC1724.2
104200             PERFORM FAIL                                         NC1724.2
104300             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
104400             MOVE    3            TO CORRECT-N                    NC1724.2
104500             PERFORM PRINT-DETAIL.                                NC1724.2
104600     ADD 1 TO REC-CT.                                             NC1724.2
104700 DIV-TEST-F2-18-3.                                                NC1724.2
104800     IF      WRK-DU-2V1-2 = 2.5                                   NC1724.2
104900             PERFORM PASS                                         NC1724.2
105000             PERFORM PRINT-DETAIL                                 NC1724.2
105100     ELSE                                                         NC1724.2
105200             PERFORM FAIL                                         NC1724.2
105300             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
105400             MOVE    2.5          TO CORRECT-N                    NC1724.2
105500             PERFORM PRINT-DETAIL.                                NC1724.2
105600     ADD 1 TO REC-CT.                                             NC1724.2
105700 DIV-TEST-F2-18-4.                                                NC1724.2
105800     IF      WRK-DU-2V0-2 = 3                                     NC1724.2
105900             PERFORM PASS                                         NC1724.2
106000             PERFORM PRINT-DETAIL                                 NC1724.2
106100     ELSE                                                         NC1724.2
106200             PERFORM FAIL                                         NC1724.2
106300             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
106400             MOVE    3            TO CORRECT-N                    NC1724.2
106500             PERFORM PRINT-DETAIL.                                NC1724.2
106600     ADD 1 TO REC-CT.                                             NC1724.2
106700 DIV-TEST-F2-18-5.                                                NC1724.2
106800     IF      WRK-DU-2V1-3 = 2.5                                   NC1724.2
106900             PERFORM PASS                                         NC1724.2
107000             PERFORM PRINT-DETAIL                                 NC1724.2
107100     ELSE                                                         NC1724.2
107200             PERFORM FAIL                                         NC1724.2
107300             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
107400             MOVE    2.5          TO CORRECT-N                    NC1724.2
107500             PERFORM PRINT-DETAIL.                                NC1724.2
107600     ADD 1 TO REC-CT.                                             NC1724.2
107700 DIV-TEST-F2-18-6.                                                NC1724.2
107800     IF      WRK-DU-2V0-3 = 2                                     NC1724.2
107900             PERFORM PASS                                         NC1724.2
108000             PERFORM PRINT-DETAIL                                 NC1724.2
108100     ELSE                                                         NC1724.2
108200             PERFORM FAIL                                         NC1724.2
108300             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
108400             MOVE    2            TO CORRECT-N                    NC1724.2
108500             PERFORM PRINT-DETAIL.                                NC1724.2
108600*                                                                 NC1724.2
108700*                                                                 NC1724.2
108800 DIV-INIT-F2-19.                                                  NC1724.2
108900*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
109000*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
109100     MOVE   "DIV-TEST-F2-19" TO PAR-NAME.                         NC1724.2
109200     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
109300     MOVE   "0"  TO WRK-XN-00001.                                 NC1724.2
109400     MOVE    0   TO WRK-DU-2V1-1.                                 NC1724.2
109500     MOVE    0   TO WRK-DU-2V0-1.                                 NC1724.2
109600     MOVE    0   TO WRK-DU-2V1-2.                                 NC1724.2
109700     MOVE    0   TO WRK-DU-2V0-2.                                 NC1724.2
109800     MOVE    0   TO WRK-DU-2V1-3.                                 NC1724.2
109900     MOVE    0   TO WRK-DU-2V0-3.                                 NC1724.2
110000     MOVE    1   TO REC-CT.                                       NC1724.2
110100     MOVE    99  TO WRK-DU-2V0-1.                                 NC1724.2
110200     MOVE    .1  TO A01ONE-CS-00V01.                              NC1724.2
110300 DIV-TEST-F2-19-0.                                                NC1724.2
110400     DIVIDE  A01ONE-CS-00V01 INTO WRK-DU-2V0-1                    NC1724.2
110500      GIVING WRK-DU-2V1-1                                         NC1724.2
110600             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
110700             WRK-DU-2V1-2                                         NC1724.2
110800             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
110900             WRK-DU-2V1-3                                         NC1724.2
111000             WRK-DU-2V0-3                                         NC1724.2
111100          ON SIZE ERROR                                           NC1724.2
111200             MOVE  "1" TO WRK-XN-00001.                           NC1724.2
111300     GO TO DIV-TEST-F2-19-1.                                      NC1724.2
111400 DIV-DELETE-F2-19.                                                NC1724.2
111500     PERFORM DE-LETE.                                             NC1724.2
111600     PERFORM PRINT-DETAIL.                                        NC1724.2
111700     GO TO   DIV-INIT-F2-20.                                      NC1724.2
111800 DIV-TEST-F2-19-1.                                                NC1724.2
111900     IF      WRK-DU-2V1-1 = 0                                     NC1724.2
112000             PERFORM PASS                                         NC1724.2
112100             PERFORM PRINT-DETAIL                                 NC1724.2
112200     ELSE                                                         NC1724.2
112300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
112400             PERFORM FAIL                                         NC1724.2
112500             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
112600             MOVE    0            TO CORRECT-N                    NC1724.2
112700             PERFORM PRINT-DETAIL.                                NC1724.2
112800     ADD 1 TO REC-CT.                                             NC1724.2
112900 DIV-TEST-F2-19-2.                                                NC1724.2
113000     IF      WRK-DU-2V0-1 = 99                                    NC1724.2
113100             PERFORM PASS                                         NC1724.2
113200             PERFORM PRINT-DETAIL                                 NC1724.2
113300     ELSE                                                         NC1724.2
113400             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
113500             PERFORM FAIL                                         NC1724.2
113600             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
113700             MOVE    99           TO CORRECT-N                    NC1724.2
113800             PERFORM PRINT-DETAIL.                                NC1724.2
113900     ADD 1 TO REC-CT.                                             NC1724.2
114000 DIV-TEST-F2-19-3.                                                NC1724.2
114100     IF      WRK-DU-2V1-2 = 0                                     NC1724.2
114200             PERFORM PASS                                         NC1724.2
114300             PERFORM PRINT-DETAIL                                 NC1724.2
114400     ELSE                                                         NC1724.2
114500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
114600             PERFORM FAIL                                         NC1724.2
114700             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
114800             MOVE    0            TO CORRECT-N                    NC1724.2
114900             PERFORM PRINT-DETAIL.                                NC1724.2
115000     ADD 1 TO REC-CT.                                             NC1724.2
115100 DIV-TEST-F2-19-4.                                                NC1724.2
115200     IF      WRK-DU-2V0-2 = 0                                     NC1724.2
115300             PERFORM PASS                                         NC1724.2
115400             PERFORM PRINT-DETAIL                                 NC1724.2
115500     ELSE                                                         NC1724.2
115600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
115700             PERFORM FAIL                                         NC1724.2
115800             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
115900             MOVE    0            TO CORRECT-N                    NC1724.2
116000             PERFORM PRINT-DETAIL.                                NC1724.2
116100     ADD 1 TO REC-CT.                                             NC1724.2
116200 DIV-TEST-F2-19-5.                                                NC1724.2
116300     IF      WRK-DU-2V1-3 = 0                                     NC1724.2
116400             PERFORM PASS                                         NC1724.2
116500             PERFORM PRINT-DETAIL                                 NC1724.2
116600     ELSE                                                         NC1724.2
116700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
116800             PERFORM FAIL                                         NC1724.2
116900             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
117000             MOVE    0            TO CORRECT-N                    NC1724.2
117100             PERFORM PRINT-DETAIL.                                NC1724.2
117200     ADD 1 TO REC-CT.                                             NC1724.2
117300 DIV-TEST-F2-19-6.                                                NC1724.2
117400     IF      WRK-DU-2V0-3 = 0                                     NC1724.2
117500             PERFORM PASS                                         NC1724.2
117600             PERFORM PRINT-DETAIL                                 NC1724.2
117700     ELSE                                                         NC1724.2
117800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
117900             PERFORM FAIL                                         NC1724.2
118000             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
118100             MOVE    0            TO CORRECT-N                    NC1724.2
118200             PERFORM PRINT-DETAIL.                                NC1724.2
118300     ADD 1 TO REC-CT.                                             NC1724.2
118400 DIV-TEST-F2-19-7.                                                NC1724.2
118500     IF      WRK-XN-00001 = "1"                                   NC1724.2
118600             PERFORM PASS                                         NC1724.2
118700             PERFORM PRINT-DETAIL                                 NC1724.2
118800     ELSE                                                         NC1724.2
118900             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1724.2
119000             PERFORM FAIL                                         NC1724.2
119100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
119200             MOVE   "1"           TO CORRECT-X                    NC1724.2
119300             PERFORM PRINT-DETAIL.                                NC1724.2
119400*                                                                 NC1724.2
119500*                                                                 NC1724.2
119600 DIV-INIT-F2-20.                                                  NC1724.2
119700*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
119800*            ==-->  NEW SIZE ERROR TESTS   <--==                  NC1724.2
119900     MOVE   "DIV-TEST-F2-20" TO PAR-NAME.                         NC1724.2
120000     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
120100     MOVE   "0"  TO WRK-XN-00001.                                 NC1724.2
120200     MOVE    0   TO WRK-DU-2V1-1.                                 NC1724.2
120300     MOVE    0   TO WRK-DU-2V0-1.                                 NC1724.2
120400     MOVE    0   TO WRK-DU-2V1-2.                                 NC1724.2
120500     MOVE    0   TO WRK-DU-2V0-2.                                 NC1724.2
120600     MOVE    0   TO WRK-DU-2V1-3.                                 NC1724.2
120700     MOVE    0   TO WRK-DU-2V0-3.                                 NC1724.2
120800     MOVE    10  TO WRK-DU-2V0-1.                                 NC1724.2
120900     MOVE    3.9 TO WRK-DU-1V1-2.                                 NC1724.2
121000     MOVE    1   TO REC-CT.                                       NC1724.2
121100 DIV-TEST-F2-20-0.                                                NC1724.2
121200     DIVIDE  WRK-DU-1V1-2 INTO WRK-DU-2V0-1                       NC1724.2
121300      GIVING WRK-DU-2V1-1                                         NC1724.2
121400             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
121500             WRK-DU-2V1-2                                         NC1724.2
121600             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
121700             WRK-DU-2V1-3                                         NC1724.2
121800             WRK-DU-2V0-3                                         NC1724.2
121900          ON SIZE ERROR                                           NC1724.2
122000             MOVE   "1" TO WRK-XN-00001.                          NC1724.2
122100     GO TO DIV-TEST-F2-20-1.                                      NC1724.2
122200 DIV-DELETE-F2-20.                                                NC1724.2
122300     PERFORM DE-LETE.                                             NC1724.2
122400     PERFORM PRINT-DETAIL.                                        NC1724.2
122500     GO TO   DIV-INIT-F2-21.                                      NC1724.2
122600 DIV-TEST-F2-20-1.                                                NC1724.2
122700     IF      WRK-DU-2V1-1 = 2.5                                   NC1724.2
122800             PERFORM PASS                                         NC1724.2
122900             PERFORM PRINT-DETAIL                                 NC1724.2
123000     ELSE                                                         NC1724.2
123100             PERFORM FAIL                                         NC1724.2
123200             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
123300             MOVE    2.5          TO CORRECT-N                    NC1724.2
123400             PERFORM PRINT-DETAIL.                                NC1724.2
123500     ADD 1 TO REC-CT.                                             NC1724.2
123600 DIV-TEST-F2-20-2.                                                NC1724.2
123700     IF      WRK-DU-2V0-1 = 3                                     NC1724.2
123800             PERFORM PASS                                         NC1724.2
123900             PERFORM PRINT-DETAIL                                 NC1724.2
124000     ELSE                                                         NC1724.2
124100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
124200             PERFORM FAIL                                         NC1724.2
124300             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
124400             MOVE    3            TO CORRECT-N                    NC1724.2
124500             PERFORM PRINT-DETAIL.                                NC1724.2
124600     ADD 1 TO REC-CT.                                             NC1724.2
124700 DIV-TEST-F2-20-3.                                                NC1724.2
124800     IF      WRK-DU-2V1-2 = 2.5                                   NC1724.2
124900             PERFORM PASS                                         NC1724.2
125000             PERFORM PRINT-DETAIL                                 NC1724.2
125100     ELSE                                                         NC1724.2
125200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
125300             PERFORM FAIL                                         NC1724.2
125400             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
125500             MOVE    2.5          TO CORRECT-N                    NC1724.2
125600             PERFORM PRINT-DETAIL.                                NC1724.2
125700     ADD 1 TO REC-CT.                                             NC1724.2
125800 DIV-TEST-F2-20-4.                                                NC1724.2
125900     IF      WRK-DU-2V0-2 = 3                                     NC1724.2
126000             PERFORM PASS                                         NC1724.2
126100             PERFORM PRINT-DETAIL                                 NC1724.2
126200     ELSE                                                         NC1724.2
126300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
126400             PERFORM FAIL                                         NC1724.2
126500             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
126600             MOVE    3            TO CORRECT-N                    NC1724.2
126700             PERFORM PRINT-DETAIL.                                NC1724.2
126800     ADD 1 TO REC-CT.                                             NC1724.2
126900 DIV-TEST-F2-20-5.                                                NC1724.2
127000     IF      WRK-DU-2V1-3 = 2.5                                   NC1724.2
127100             PERFORM PASS                                         NC1724.2
127200             PERFORM PRINT-DETAIL                                 NC1724.2
127300     ELSE                                                         NC1724.2
127400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
127500             PERFORM FAIL                                         NC1724.2
127600             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
127700             MOVE    2.5          TO CORRECT-N                    NC1724.2
127800             PERFORM PRINT-DETAIL.                                NC1724.2
127900     ADD 1 TO REC-CT.                                             NC1724.2
128000 DIV-TEST-F2-20-6.                                                NC1724.2
128100     IF      WRK-DU-2V0-3 = 2                                     NC1724.2
128200             PERFORM PASS                                         NC1724.2
128300             PERFORM PRINT-DETAIL                                 NC1724.2
128400     ELSE                                                         NC1724.2
128500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
128600             PERFORM FAIL                                         NC1724.2
128700             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
128800             MOVE    2            TO CORRECT-N                    NC1724.2
128900             PERFORM PRINT-DETAIL.                                NC1724.2
129000     ADD 1 TO REC-CT.                                             NC1724.2
129100 DIV-TEST-F2-20-7.                                                NC1724.2
129200     IF      WRK-XN-00001 = "0"                                   NC1724.2
129300             PERFORM PASS                                         NC1724.2
129400             PERFORM PRINT-DETAIL                                 NC1724.2
129500     ELSE                                                         NC1724.2
129600             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1724.2
129700                  TO RE-MARK                                      NC1724.2
129800             PERFORM FAIL                                         NC1724.2
129900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
130000             MOVE   "0"           TO CORRECT-X                    NC1724.2
130100             PERFORM PRINT-DETAIL.                                NC1724.2
130200*                                                                 NC1724.2
130300*                                                                 NC1724.2
130400 DIV-INIT-F2-21.                                                  NC1724.2
130500*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
130600*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
130700     MOVE   "DIV-TEST-F2-21" TO PAR-NAME.                         NC1724.2
130800     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1724.2
130900     MOVE   "0"  TO WRK-XN-00001.                                 NC1724.2
131000     MOVE    0   TO WRK-DU-2V1-1.                                 NC1724.2
131100     MOVE    0   TO WRK-DU-2V1-2.                                 NC1724.2
131200     MOVE    0   TO WRK-DU-2V0-2.                                 NC1724.2
131300     MOVE    0   TO WRK-DU-2V1-3.                                 NC1724.2
131400     MOVE    0   TO WRK-DU-2V0-3.                                 NC1724.2
131500     MOVE    99  TO WRK-DU-2V0-1.                                 NC1724.2
131600     MOVE    1   TO REC-CT.                                       NC1724.2
131700 DIV-TEST-F2-21-0.                                                NC1724.2
131800     DIVIDE  A01ONE-CS-00V01 INTO WRK-DU-2V0-1                    NC1724.2
131900      GIVING WRK-DU-2V1-1                                         NC1724.2
132000             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
132100             WRK-DU-2V1-2                                         NC1724.2
132200             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
132300             WRK-DU-2V1-3                                         NC1724.2
132400             WRK-DU-2V0-3                                         NC1724.2
132500      NOT ON SIZE ERROR                                           NC1724.2
132600             MOVE  "1" TO WRK-XN-00001.                           NC1724.2
132700     GO TO DIV-TEST-F2-21-1.                                      NC1724.2
132800 DIV-DELETE-F2-21.                                                NC1724.2
132900     PERFORM DE-LETE.                                             NC1724.2
133000     PERFORM PRINT-DETAIL.                                        NC1724.2
133100     GO TO   DIV-INIT-F2-22.                                      NC1724.2
133200 DIV-TEST-F2-21-1.                                                NC1724.2
133300     IF      WRK-DU-2V1-1 = 0                                     NC1724.2
133400             PERFORM PASS                                         NC1724.2
133500             PERFORM PRINT-DETAIL                                 NC1724.2
133600     ELSE                                                         NC1724.2
133700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
133800             PERFORM FAIL                                         NC1724.2
133900             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
134000             MOVE    0            TO CORRECT-N                    NC1724.2
134100             PERFORM PRINT-DETAIL.                                NC1724.2
134200     ADD 1 TO REC-CT.                                             NC1724.2
134300 DIV-TEST-F2-21-2.                                                NC1724.2
134400     IF      WRK-DU-2V0-1 = 99                                    NC1724.2
134500             PERFORM PASS                                         NC1724.2
134600             PERFORM PRINT-DETAIL                                 NC1724.2
134700     ELSE                                                         NC1724.2
134800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
134900             PERFORM FAIL                                         NC1724.2
135000             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
135100             MOVE    99           TO CORRECT-N                    NC1724.2
135200             PERFORM PRINT-DETAIL.                                NC1724.2
135300     ADD 1 TO REC-CT.                                             NC1724.2
135400 DIV-TEST-F2-21-3.                                                NC1724.2
135500     IF      WRK-DU-2V1-2 = 0                                     NC1724.2
135600             PERFORM PASS                                         NC1724.2
135700             PERFORM PRINT-DETAIL                                 NC1724.2
135800     ELSE                                                         NC1724.2
135900             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
136000             PERFORM FAIL                                         NC1724.2
136100             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
136200             MOVE    0            TO CORRECT-N                    NC1724.2
136300             PERFORM PRINT-DETAIL.                                NC1724.2
136400     ADD 1 TO REC-CT.                                             NC1724.2
136500 DIV-TEST-F2-21-4.                                                NC1724.2
136600     IF      WRK-DU-2V0-2 = 0                                     NC1724.2
136700             PERFORM PASS                                         NC1724.2
136800             PERFORM PRINT-DETAIL                                 NC1724.2
136900     ELSE                                                         NC1724.2
137000             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
137100             PERFORM FAIL                                         NC1724.2
137200             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
137300             MOVE    0            TO CORRECT-N                    NC1724.2
137400             PERFORM PRINT-DETAIL.                                NC1724.2
137500     ADD 1 TO REC-CT.                                             NC1724.2
137600 DIV-TEST-F2-21-5.                                                NC1724.2
137700     IF      WRK-DU-2V1-3 = 0                                     NC1724.2
137800             PERFORM PASS                                         NC1724.2
137900             PERFORM PRINT-DETAIL                                 NC1724.2
138000     ELSE                                                         NC1724.2
138100             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
138200             PERFORM FAIL                                         NC1724.2
138300             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
138400             MOVE    0            TO CORRECT-N                    NC1724.2
138500             PERFORM PRINT-DETAIL.                                NC1724.2
138600     ADD 1 TO REC-CT.                                             NC1724.2
138700 DIV-TEST-F2-21-6.                                                NC1724.2
138800     IF      WRK-DU-2V0-3 = 0                                     NC1724.2
138900             PERFORM PASS                                         NC1724.2
139000             PERFORM PRINT-DETAIL                                 NC1724.2
139100     ELSE                                                         NC1724.2
139200             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
139300             PERFORM FAIL                                         NC1724.2
139400             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
139500             MOVE    0            TO CORRECT-N                    NC1724.2
139600             PERFORM PRINT-DETAIL.                                NC1724.2
139700     ADD 1 TO REC-CT.                                             NC1724.2
139800 DIV-TEST-F2-21-7.                                                NC1724.2
139900     IF      WRK-XN-00001 = "0"                                   NC1724.2
140000             PERFORM PASS                                         NC1724.2
140100             PERFORM PRINT-DETAIL                                 NC1724.2
140200     ELSE                                                         NC1724.2
140300             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1724.2
140400                  TO RE-MARK                                      NC1724.2
140500             PERFORM FAIL                                         NC1724.2
140600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
140700             MOVE   "0"           TO CORRECT-X                    NC1724.2
140800             PERFORM PRINT-DETAIL.                                NC1724.2
140900*                                                                 NC1724.2
141000*                                                                 NC1724.2
141100 DIV-INIT-F2-22.                                                  NC1724.2
141200*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
141300*            ==-->  NEW SIZE ERROR TESTS   <--==                  NC1724.2
141400     MOVE   "DIV-TEST-F2-22" TO PAR-NAME.                         NC1724.2
141500     MOVE   "V1-67 6.4.2"    TO ANSI-REFERENCE.                   NC1724.2
141600     MOVE   "0"  TO WRK-XN-00001.                                 NC1724.2
141700     MOVE    0   TO WRK-DU-2V1-1.                                 NC1724.2
141800     MOVE    0   TO WRK-DU-2V0-1.                                 NC1724.2
141900     MOVE    0   TO WRK-DU-2V1-2.                                 NC1724.2
142000     MOVE    0   TO WRK-DU-2V0-2.                                 NC1724.2
142100     MOVE    0   TO WRK-DU-2V1-3.                                 NC1724.2
142200     MOVE    0   TO WRK-DU-2V0-3.                                 NC1724.2
142300     MOVE    1   TO REC-CT.                                       NC1724.2
142400     MOVE    10  TO WRK-DU-2V0-1.                                 NC1724.2
142500     MOVE    3.9 TO WRK-DU-1V1-2.                                 NC1724.2
142600 DIV-TEST-F2-22-0.                                                NC1724.2
142700     DIVIDE  WRK-DU-1V1-2 INTO WRK-DU-2V0-1                       NC1724.2
142800      GIVING WRK-DU-2V1-1                                         NC1724.2
142900             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
143000             WRK-DU-2V1-2                                         NC1724.2
143100             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
143200             WRK-DU-2V1-3                                         NC1724.2
143300             WRK-DU-2V0-3                                         NC1724.2
143400      NOT ON SIZE ERROR                                           NC1724.2
143500             MOVE   "1" TO WRK-XN-00001.                          NC1724.2
143600     GO TO DIV-TEST-F2-22-1.                                      NC1724.2
143700 DIV-DELETE-F2-22.                                                NC1724.2
143800     PERFORM DE-LETE.                                             NC1724.2
143900     PERFORM PRINT-DETAIL.                                        NC1724.2
144000     GO TO   DIV-INIT-F2-23.                                      NC1724.2
144100 DIV-TEST-F2-22-1.                                                NC1724.2
144200     IF      WRK-DU-2V1-1 = 2.5                                   NC1724.2
144300             PERFORM PASS                                         NC1724.2
144400             PERFORM PRINT-DETAIL                                 NC1724.2
144500     ELSE                                                         NC1724.2
144600             PERFORM FAIL                                         NC1724.2
144700             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
144800             MOVE    2.5          TO CORRECT-N                    NC1724.2
144900             PERFORM PRINT-DETAIL.                                NC1724.2
145000     ADD 1 TO REC-CT.                                             NC1724.2
145100 DIV-TEST-F2-22-2.                                                NC1724.2
145200     IF      WRK-DU-2V0-1 = 3                                     NC1724.2
145300             PERFORM PASS                                         NC1724.2
145400             PERFORM PRINT-DETAIL                                 NC1724.2
145500     ELSE                                                         NC1724.2
145600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
145700             PERFORM FAIL                                         NC1724.2
145800             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
145900             MOVE    3            TO CORRECT-N                    NC1724.2
146000             PERFORM PRINT-DETAIL.                                NC1724.2
146100     ADD 1 TO REC-CT.                                             NC1724.2
146200 DIV-TEST-F2-22-3.                                                NC1724.2
146300     IF      WRK-DU-2V1-2 = 2.5                                   NC1724.2
146400             PERFORM PASS                                         NC1724.2
146500             PERFORM PRINT-DETAIL                                 NC1724.2
146600     ELSE                                                         NC1724.2
146700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
146800             PERFORM FAIL                                         NC1724.2
146900             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
147000             MOVE    2.5          TO CORRECT-N                    NC1724.2
147100             PERFORM PRINT-DETAIL.                                NC1724.2
147200     ADD 1 TO REC-CT.                                             NC1724.2
147300 DIV-TEST-F2-22-4.                                                NC1724.2
147400     IF      WRK-DU-2V0-2 = 3                                     NC1724.2
147500             PERFORM PASS                                         NC1724.2
147600             PERFORM PRINT-DETAIL                                 NC1724.2
147700     ELSE                                                         NC1724.2
147800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
147900             PERFORM FAIL                                         NC1724.2
148000             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
148100             MOVE    3            TO CORRECT-N                    NC1724.2
148200             PERFORM PRINT-DETAIL.                                NC1724.2
148300     ADD 1 TO REC-CT.                                             NC1724.2
148400 DIV-TEST-F2-22-5.                                                NC1724.2
148500     IF      WRK-DU-2V1-3 = 2.5                                   NC1724.2
148600             PERFORM PASS                                         NC1724.2
148700             PERFORM PRINT-DETAIL                                 NC1724.2
148800     ELSE                                                         NC1724.2
148900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
149000             PERFORM FAIL                                         NC1724.2
149100             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
149200             MOVE    2.5          TO CORRECT-N                    NC1724.2
149300             PERFORM PRINT-DETAIL.                                NC1724.2
149400     ADD 1 TO REC-CT.                                             NC1724.2
149500 DIV-TEST-F2-22-6.                                                NC1724.2
149600     IF      WRK-DU-2V0-3 = 2                                     NC1724.2
149700             PERFORM PASS                                         NC1724.2
149800             PERFORM PRINT-DETAIL                                 NC1724.2
149900     ELSE                                                         NC1724.2
150000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
150100             PERFORM FAIL                                         NC1724.2
150200             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
150300             MOVE    2            TO CORRECT-N                    NC1724.2
150400             PERFORM PRINT-DETAIL.                                NC1724.2
150500     ADD 1 TO REC-CT.                                             NC1724.2
150600 DIV-TEST-F2-22-7.                                                NC1724.2
150700     IF      WRK-XN-00001 = "1"                                   NC1724.2
150800             PERFORM PASS                                         NC1724.2
150900             PERFORM PRINT-DETAIL                                 NC1724.2
151000     ELSE                                                         NC1724.2
151100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
151200                  TO RE-MARK                                      NC1724.2
151300             PERFORM FAIL                                         NC1724.2
151400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
151500             MOVE   "0"           TO CORRECT-X                    NC1724.2
151600             PERFORM PRINT-DETAIL.                                NC1724.2
151700*                                                                 NC1724.2
151800*                                                                 NC1724.2
151900 DIV-INIT-F2-23.                                                  NC1724.2
152000*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
152100*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1724.2
152200     MOVE   "DIV-TEST-F2-23" TO PAR-NAME.                         NC1724.2
152300     MOVE   "V1-67 6.4.2"    TO ANSI-REFERENCE.                   NC1724.2
152400     MOVE   "0"  TO WRK-XN-00001.                                 NC1724.2
152500     MOVE    0   TO WRK-DU-2V1-1.                                 NC1724.2
152600     MOVE    0   TO WRK-DU-2V1-2.                                 NC1724.2
152700     MOVE    0   TO WRK-DU-2V0-2.                                 NC1724.2
152800     MOVE    0   TO WRK-DU-2V1-3.                                 NC1724.2
152900     MOVE    0   TO WRK-DU-2V0-3.                                 NC1724.2
153000     MOVE    1   TO REC-CT.                                       NC1724.2
153100     MOVE    99  TO WRK-DU-2V0-1.                                 NC1724.2
153200     MOVE    .1  TO A01ONE-CS-00V01.                              NC1724.2
153300 DIV-TEST-F2-23-0.                                                NC1724.2
153400     DIVIDE  A01ONE-CS-00V01 INTO WRK-DU-2V0-1                    NC1724.2
153500      GIVING WRK-DU-2V1-1                                         NC1724.2
153600             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
153700             WRK-DU-2V1-2                                         NC1724.2
153800             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
153900             WRK-DU-2V1-3                                         NC1724.2
154000             WRK-DU-2V0-3                                         NC1724.2
154100          ON SIZE ERROR                                           NC1724.2
154200             MOVE  "1" TO WRK-XN-00001                            NC1724.2
154300      NOT ON SIZE ERROR                                           NC1724.2
154400             MOVE  "2" TO WRK-XN-00001.                           NC1724.2
154500     GO TO DIV-TEST-F2-23-1.                                      NC1724.2
154600 DIV-DELETE-F2-23.                                                NC1724.2
154700     PERFORM DE-LETE.                                             NC1724.2
154800     PERFORM PRINT-DETAIL.                                        NC1724.2
154900     GO TO   DIV-INIT-F2-24.                                      NC1724.2
155000 DIV-TEST-F2-23-1.                                                NC1724.2
155100     IF      WRK-DU-2V1-1 = 0                                     NC1724.2
155200             PERFORM PASS                                         NC1724.2
155300             PERFORM PRINT-DETAIL                                 NC1724.2
155400     ELSE                                                         NC1724.2
155500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
155600             PERFORM FAIL                                         NC1724.2
155700             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
155800             MOVE    0            TO CORRECT-N                    NC1724.2
155900             PERFORM PRINT-DETAIL.                                NC1724.2
156000     ADD 1 TO REC-CT.                                             NC1724.2
156100 DIV-TEST-F2-23-2.                                                NC1724.2
156200     IF      WRK-DU-2V0-1 = 99                                    NC1724.2
156300             PERFORM PASS                                         NC1724.2
156400             PERFORM PRINT-DETAIL                                 NC1724.2
156500     ELSE                                                         NC1724.2
156600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
156700             PERFORM FAIL                                         NC1724.2
156800             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
156900             MOVE    99           TO CORRECT-N                    NC1724.2
157000             PERFORM PRINT-DETAIL.                                NC1724.2
157100     ADD 1 TO REC-CT.                                             NC1724.2
157200 DIV-TEST-F2-23-3.                                                NC1724.2
157300     IF      WRK-DU-2V1-2 = 0                                     NC1724.2
157400             PERFORM PASS                                         NC1724.2
157500             PERFORM PRINT-DETAIL                                 NC1724.2
157600     ELSE                                                         NC1724.2
157700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
157800             PERFORM FAIL                                         NC1724.2
157900             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
158000             MOVE    0            TO CORRECT-N                    NC1724.2
158100             PERFORM PRINT-DETAIL.                                NC1724.2
158200     ADD 1 TO REC-CT.                                             NC1724.2
158300 DIV-TEST-F2-23-4.                                                NC1724.2
158400     IF      WRK-DU-2V0-2 = 0                                     NC1724.2
158500             PERFORM PASS                                         NC1724.2
158600             PERFORM PRINT-DETAIL                                 NC1724.2
158700     ELSE                                                         NC1724.2
158800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
158900             PERFORM FAIL                                         NC1724.2
159000             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
159100             MOVE    0            TO CORRECT-N                    NC1724.2
159200             PERFORM PRINT-DETAIL.                                NC1724.2
159300     ADD 1 TO REC-CT.                                             NC1724.2
159400 DIV-TEST-F2-23-5.                                                NC1724.2
159500     IF      WRK-DU-2V1-3 = 0                                     NC1724.2
159600             PERFORM PASS                                         NC1724.2
159700             PERFORM PRINT-DETAIL                                 NC1724.2
159800     ELSE                                                         NC1724.2
159900             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
160000             PERFORM FAIL                                         NC1724.2
160100             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
160200             MOVE    0            TO CORRECT-N                    NC1724.2
160300             PERFORM PRINT-DETAIL.                                NC1724.2
160400     ADD 1 TO REC-CT.                                             NC1724.2
160500 DIV-TEST-F2-23-6.                                                NC1724.2
160600     IF      WRK-DU-2V0-3 = 0                                     NC1724.2
160700             PERFORM PASS                                         NC1724.2
160800             PERFORM PRINT-DETAIL                                 NC1724.2
160900     ELSE                                                         NC1724.2
161000             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1724.2
161100             PERFORM FAIL                                         NC1724.2
161200             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
161300             MOVE    0            TO CORRECT-N                    NC1724.2
161400             PERFORM PRINT-DETAIL.                                NC1724.2
161500     ADD 1 TO REC-CT.                                             NC1724.2
161600 DIV-TEST-F2-23-7.                                                NC1724.2
161700     IF      WRK-XN-00001 = "1"                                   NC1724.2
161800             PERFORM PASS                                         NC1724.2
161900             PERFORM PRINT-DETAIL                                 NC1724.2
162000     ELSE                                                         NC1724.2
162100             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1724.2
162200                  TO RE-MARK                                      NC1724.2
162300             PERFORM FAIL                                         NC1724.2
162400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
162500             MOVE   "0"           TO CORRECT-X                    NC1724.2
162600             PERFORM PRINT-DETAIL.                                NC1724.2
162700*                                                                 NC1724.2
162800*                                                                 NC1724.2
162900 DIV-INIT-F2-24.                                                  NC1724.2
163000*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
163100*            ==-->  NEW SIZE ERROR TESTS   <--==                  NC1724.2
163200     MOVE   "DIV-TEST-F2-24" TO PAR-NAME.                         NC1724.2
163300     MOVE   "V1-67 6.4.2"    TO ANSI-REFERENCE.                   NC1724.2
163400     MOVE   "0"  TO WRK-XN-00001.                                 NC1724.2
163500     MOVE    0   TO WRK-DU-2V1-1.                                 NC1724.2
163600     MOVE    0   TO WRK-DU-2V0-1.                                 NC1724.2
163700     MOVE    0   TO WRK-DU-2V1-2.                                 NC1724.2
163800     MOVE    0   TO WRK-DU-2V0-2.                                 NC1724.2
163900     MOVE    0   TO WRK-DU-2V1-3.                                 NC1724.2
164000     MOVE    0   TO WRK-DU-2V0-3.                                 NC1724.2
164100     MOVE    1   TO REC-CT.                                       NC1724.2
164200     MOVE    10  TO WRK-DU-2V0-1.                                 NC1724.2
164300     MOVE    3.9 TO WRK-DU-1V1-2.                                 NC1724.2
164400 DIV-TEST-F2-24-0.                                                NC1724.2
164500     DIVIDE  WRK-DU-1V1-2 INTO WRK-DU-2V0-1                       NC1724.2
164600      GIVING WRK-DU-2V1-1                                         NC1724.2
164700             WRK-DU-2V0-1 ROUNDED                                 NC1724.2
164800             WRK-DU-2V1-2                                         NC1724.2
164900             WRK-DU-2V0-2 ROUNDED                                 NC1724.2
165000             WRK-DU-2V1-3                                         NC1724.2
165100             WRK-DU-2V0-3                                         NC1724.2
165200          ON SIZE ERROR                                           NC1724.2
165300             MOVE   "1" TO WRK-XN-00001                           NC1724.2
165400      NOT ON SIZE ERROR                                           NC1724.2
165500             MOVE   "2" TO WRK-XN-00001.                          NC1724.2
165600     GO TO DIV-TEST-F2-24-1.                                      NC1724.2
165700 DIV-DELETE-F2-24.                                                NC1724.2
165800     PERFORM DE-LETE.                                             NC1724.2
165900     PERFORM PRINT-DETAIL.                                        NC1724.2
166000     GO TO   DIV-INIT-F2-25.                                      NC1724.2
166100 DIV-TEST-F2-24-1.                                                NC1724.2
166200     IF      WRK-DU-2V1-1 = 2.5                                   NC1724.2
166300             PERFORM PASS                                         NC1724.2
166400             PERFORM PRINT-DETAIL                                 NC1724.2
166500     ELSE                                                         NC1724.2
166600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
166700             PERFORM FAIL                                         NC1724.2
166800             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1724.2
166900             MOVE    2.5          TO CORRECT-N                    NC1724.2
167000             PERFORM PRINT-DETAIL.                                NC1724.2
167100     ADD 1 TO REC-CT.                                             NC1724.2
167200 DIV-TEST-F2-24-2.                                                NC1724.2
167300     IF      WRK-DU-2V0-1 = 3                                     NC1724.2
167400             PERFORM PASS                                         NC1724.2
167500             PERFORM PRINT-DETAIL                                 NC1724.2
167600     ELSE                                                         NC1724.2
167700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
167800             PERFORM FAIL                                         NC1724.2
167900             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1724.2
168000             MOVE    3            TO CORRECT-N                    NC1724.2
168100             PERFORM PRINT-DETAIL.                                NC1724.2
168200     ADD 1 TO REC-CT.                                             NC1724.2
168300 DIV-TEST-F2-24-3.                                                NC1724.2
168400     IF      WRK-DU-2V1-2 = 2.5                                   NC1724.2
168500             PERFORM PASS                                         NC1724.2
168600             PERFORM PRINT-DETAIL                                 NC1724.2
168700     ELSE                                                         NC1724.2
168800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
168900             PERFORM FAIL                                         NC1724.2
169000             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1724.2
169100             MOVE    2.5          TO CORRECT-N                    NC1724.2
169200             PERFORM PRINT-DETAIL.                                NC1724.2
169300     ADD 1 TO REC-CT.                                             NC1724.2
169400 DIV-TEST-F2-24-4.                                                NC1724.2
169500     IF      WRK-DU-2V0-2 = 3                                     NC1724.2
169600             PERFORM PASS                                         NC1724.2
169700             PERFORM PRINT-DETAIL                                 NC1724.2
169800     ELSE                                                         NC1724.2
169900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
170000             PERFORM FAIL                                         NC1724.2
170100             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1724.2
170200             MOVE    3            TO CORRECT-N                    NC1724.2
170300             PERFORM PRINT-DETAIL.                                NC1724.2
170400     ADD 1 TO REC-CT.                                             NC1724.2
170500 DIV-TEST-F2-24-5.                                                NC1724.2
170600     IF      WRK-DU-2V1-3 = 2.5                                   NC1724.2
170700             PERFORM PASS                                         NC1724.2
170800             PERFORM PRINT-DETAIL                                 NC1724.2
170900     ELSE                                                         NC1724.2
171000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
171100             PERFORM FAIL                                         NC1724.2
171200             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1724.2
171300             MOVE    2.5          TO CORRECT-N                    NC1724.2
171400             PERFORM PRINT-DETAIL.                                NC1724.2
171500     ADD 1 TO REC-CT.                                             NC1724.2
171600 DIV-TEST-F2-24-6.                                                NC1724.2
171700     IF      WRK-DU-2V0-3 = 2                                     NC1724.2
171800             PERFORM PASS                                         NC1724.2
171900             PERFORM PRINT-DETAIL                                 NC1724.2
172000     ELSE                                                         NC1724.2
172100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
172200             PERFORM FAIL                                         NC1724.2
172300             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1724.2
172400             MOVE    2            TO CORRECT-N                    NC1724.2
172500             PERFORM PRINT-DETAIL.                                NC1724.2
172600     ADD 1 TO REC-CT.                                             NC1724.2
172700 DIV-TEST-F2-24-7.                                                NC1724.2
172800     IF      WRK-XN-00001 = "2"                                   NC1724.2
172900             PERFORM PASS                                         NC1724.2
173000             PERFORM PRINT-DETAIL                                 NC1724.2
173100     ELSE                                                         NC1724.2
173200             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
173300                  TO RE-MARK                                      NC1724.2
173400             PERFORM FAIL                                         NC1724.2
173500             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
173600             MOVE   "2"           TO CORRECT-X                    NC1724.2
173700             PERFORM PRINT-DETAIL.                                NC1724.2
173800*                                                                 NC1724.2
173900*                                                                 NC1724.2
174000 DIV-INIT-F2-25.                                                  NC1724.2
174100*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
174200*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1724.2
174300     MOVE   "DIV-TEST-F2-25" TO PAR-NAME.                         NC1724.2
174400     MOVE   "1V-41 6.4.3"      TO ANSI-REFERENCE.                 NC1724.2
174500     MOVE   "0"     TO WRK-XN-00001.                              NC1724.2
174600     MOVE    0      TO WRK-DS-05V00.                              NC1724.2
174700     MOVE    0      TO WRK-DS-02V00.                              NC1724.2
174800     MOVE    0      TO WRK-CS-18V00.                              NC1724.2
174900     MOVE    0      TO DIV10.                                     NC1724.2
175000     MOVE    1      TO REC-CT.                                    NC1724.2
175100     MOVE    44.1   TO DIV2.                                      NC1724.2
175200     MOVE   -9.642  TO DIV4.                                      NC1724.2
175300 DIV-TEST-F2-25-0.                                                NC1724.2
175400     DIVIDE  DIV4 INTO DIV2                                       NC1724.2
175500      GIVING DIV10                                                NC1724.2
175600          ON SIZE ERROR                                           NC1724.2
175700             MOVE   "1" TO WRK-XN-00001                           NC1724.2
175800             MOVE   23  TO WRK-DS-05V00                           NC1724.2
175900             MOVE   -4  TO WRK-DS-02V00                           NC1724.2
176000     END-DIVIDE                                                   NC1724.2
176100     MOVE    99    TO WRK-CS-18V00.                               NC1724.2
176200     GO TO   DIV-TEST-F2-25-1.                                    NC1724.2
176300 DIV-DELETE-F2-25-1.                                              NC1724.2
176400     PERFORM DE-LETE.                                             NC1724.2
176500     PERFORM PRINT-DETAIL.                                        NC1724.2
176600     GO TO   DIV-INIT-F2-26.                                      NC1724.2
176700 DIV-TEST-F2-25-1.                                                NC1724.2
176800     MOVE   "DIV-TEST-F2-25-1" TO PAR-NAME.                       NC1724.2
176900     IF      WRK-XN-00001 = "1"                                   NC1724.2
177000             PERFORM PASS                                         NC1724.2
177100             PERFORM PRINT-DETAIL                                 NC1724.2
177200     ELSE                                                         NC1724.2
177300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
177400             MOVE   "1"           TO CORRECT-X                    NC1724.2
177500             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1724.2
177600             PERFORM FAIL                                         NC1724.2
177700             PERFORM PRINT-DETAIL.                                NC1724.2
177800     ADD 1 TO REC-CT.                                             NC1724.2
177900 DIV-TEST-F2-25-2.                                                NC1724.2
178000     MOVE   "DIV-TEST-F2-25-2" TO PAR-NAME.                       NC1724.2
178100     IF      WRK-DS-02V00 = -4                                    NC1724.2
178200             PERFORM PASS                                         NC1724.2
178300             PERFORM PRINT-DETAIL                                 NC1724.2
178400     ELSE                                                         NC1724.2
178500             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1724.2
178600             MOVE   -4            TO CORRECT-N                    NC1724.2
178700             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1724.2
178800             PERFORM FAIL                                         NC1724.2
178900             PERFORM PRINT-DETAIL.                                NC1724.2
179000     ADD     1 TO REC-CT.                                         NC1724.2
179100 DIV-TEST-F2-25-3.                                                NC1724.2
179200     MOVE   "DIV-TEST-F2-25-3" TO PAR-NAME.                       NC1724.2
179300     IF      WRK-DS-05V00 = 23                                    NC1724.2
179400             PERFORM PASS                                         NC1724.2
179500             PERFORM PRINT-DETAIL                                 NC1724.2
179600     ELSE                                                         NC1724.2
179700             MOVE    WRK-DS-05V00 TO COMPUTED-N                   NC1724.2
179800             MOVE    23           TO CORRECT-N                    NC1724.2
179900             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1724.2
180000             PERFORM FAIL                                         NC1724.2
180100             PERFORM PRINT-DETAIL.                                NC1724.2
180200     ADD     1 TO REC-CT.                                         NC1724.2
180300 DIV-TEST-F2-25-4.                                                NC1724.2
180400     MOVE   "DIV-TEST-F2-25-4" TO PAR-NAME.                       NC1724.2
180500     IF      DIV10   = 0                                          NC1724.2
180600             PERFORM PASS                                         NC1724.2
180700             PERFORM PRINT-DETAIL                                 NC1724.2
180800     ELSE                                                         NC1724.2
180900             MOVE    DIV10        TO COMPUTED-N                   NC1724.2
181000             MOVE    0            TO CORRECT-N                    NC1724.2
181100             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1724.2
181200             PERFORM FAIL                                         NC1724.2
181300             PERFORM PRINT-DETAIL.                                NC1724.2
181400     ADD     1 TO REC-CT.                                         NC1724.2
181500 DIV-TEST-F2-25-5.                                                NC1724.2
181600     MOVE   "DIV-TEST-F2-25-5" TO PAR-NAME.                       NC1724.2
181700     IF      WRK-CS-18V00 = 000000000000000099                    NC1724.2
181800             PERFORM PASS                                         NC1724.2
181900             PERFORM PRINT-DETAIL                                 NC1724.2
182000     ELSE                                                         NC1724.2
182100             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1724.2
182200             MOVE    99           TO CORRECT-N                    NC1724.2
182300             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1724.2
182400             PERFORM FAIL                                         NC1724.2
182500             PERFORM PRINT-DETAIL.                                NC1724.2
182600*                                                                 NC1724.2
182700*                                                                 NC1724.2
182800 DIV-INIT-F2-26.                                                  NC1724.2
182900*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
183000*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1724.2
183100     MOVE   "DIV-TEST-F2-26" TO PAR-NAME.                         NC1724.2
183200     MOVE   "1V-41 6.4.3"      TO ANSI-REFERENCE.                 NC1724.2
183300     MOVE    1      TO REC-CT.                                    NC1724.2
183400     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1724.2
183500     MOVE    0      TO WRK-DS-05V00.                              NC1724.2
183600     MOVE    0      TO WRK-DS-02V00.                              NC1724.2
183700     MOVE   "0"     TO WRK-XN-00001.                              NC1724.2
183800     MOVE    0      TO WRK-CS-18V00.                              NC1724.2
183900 DIV-TEST-F2-26-0.                                                NC1724.2
184000     DIVIDE  A18ONES-DS-09V09 INTO WRK-DS-09V09                   NC1724.2
184100      GIVING WRK-DS-09V09                                         NC1724.2
184200          ON SIZE ERROR                                           NC1724.2
184300             MOVE   "1" TO WRK-XN-00001                           NC1724.2
184400             MOVE   23  TO WRK-DS-05V00                           NC1724.2
184500             MOVE   -4  TO WRK-DS-02V00                           NC1724.2
184600      END-DIVIDE                                                  NC1724.2
184700      MOVE   99 TO WRK-CS-18V00.                                  NC1724.2
184800     GO TO   DIV-TEST-F2-26-1.                                    NC1724.2
184900 DIV-DELETE-F2-26-1.                                              NC1724.2
185000     PERFORM DE-LETE.                                             NC1724.2
185100     PERFORM PRINT-DETAIL.                                        NC1724.2
185200     GO TO   DIV-INIT-F2-27.                                      NC1724.2
185300 DIV-TEST-F2-26-1.                                                NC1724.2
185400     MOVE   "DIV-TEST-F2-26-1" TO PAR-NAME.                       NC1724.2
185500     IF      WRK-XN-00001 = "0"                                   NC1724.2
185600             PERFORM PASS                                         NC1724.2
185700             PERFORM PRINT-DETAIL                                 NC1724.2
185800     ELSE                                                         NC1724.2
185900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
186000             MOVE   "0"           TO CORRECT-X                    NC1724.2
186100             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1724.2
186200                  TO RE-MARK                                      NC1724.2
186300             PERFORM FAIL                                         NC1724.2
186400             PERFORM PRINT-DETAIL.                                NC1724.2
186500     ADD     1 TO REC-CT.                                         NC1724.2
186600 DIV-TEST-F2-26-2.                                                NC1724.2
186700     MOVE   "DIV-TEST-F2-26-2" TO PAR-NAME.                       NC1724.2
186800     IF      WRK-DS-02V00  = 0                                    NC1724.2
186900             PERFORM PASS                                         NC1724.2
187000             PERFORM PRINT-DETAIL                                 NC1724.2
187100     ELSE                                                         NC1724.2
187200             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1724.2
187300             MOVE    0            TO CORRECT-N                    NC1724.2
187400             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1724.2
187500                  TO RE-MARK                                      NC1724.2
187600             PERFORM FAIL                                         NC1724.2
187700             PERFORM PRINT-DETAIL.                                NC1724.2
187800     ADD     1 TO REC-CT.                                         NC1724.2
187900 DIV-TEST-F2-26-3.                                                NC1724.2
188000     MOVE   "DIV-TEST-F2-26-3" TO PAR-NAME.                       NC1724.2
188100     IF      WRK-DS-05V00 = 0                                     NC1724.2
188200             PERFORM PASS                                         NC1724.2
188300             PERFORM PRINT-DETAIL                                 NC1724.2
188400     ELSE                                                         NC1724.2
188500             MOVE    WRK-DS-05V00 TO COMPUTED-N                   NC1724.2
188600             MOVE    0            TO CORRECT-N                    NC1724.2
188700             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1724.2
188800                  TO RE-MARK                                      NC1724.2
188900             PERFORM FAIL                                         NC1724.2
189000             PERFORM PRINT-DETAIL.                                NC1724.2
189100     ADD     1 TO REC-CT.                                         NC1724.2
189200 DIV-TEST-F2-26-4.                                                NC1724.2
189300     MOVE   "DIV-TEST-F2-26-4" TO PAR-NAME.                       NC1724.2
189400     IF      WRK-DS-18V00-S  =   000000001000000000               NC1724.2
189500             PERFORM PASS                                         NC1724.2
189600             PERFORM PRINT-DETAIL                                 NC1724.2
189700     ELSE                                                         NC1724.2
189800             MOVE    WRK-DS-18V00-S     TO COMPUTED-N             NC1724.2
189900             MOVE    000000001000000000 TO CORRECT-18V0           NC1724.2
190000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
190100             PERFORM FAIL                                         NC1724.2
190200             PERFORM PRINT-DETAIL.                                NC1724.2
190300     ADD     1 TO REC-CT.                                         NC1724.2
190400 DIV-TEST-F2-26-5.                                                NC1724.2
190500     MOVE   "DIV-TEST-F2-26-5" TO PAR-NAME.                       NC1724.2
190600     IF      WRK-CS-18V00 = 000000000000000099                    NC1724.2
190700             PERFORM PASS                                         NC1724.2
190800             PERFORM PRINT-DETAIL                                 NC1724.2
190900     ELSE                                                         NC1724.2
191000             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1724.2
191100             MOVE    99           TO CORRECT-N                    NC1724.2
191200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1724.2
191300             PERFORM FAIL                                         NC1724.2
191400             PERFORM PRINT-DETAIL.                                NC1724.2
191500*                                                                 NC1724.2
191600*                                                                 NC1724.2
191700 DIV-INIT-F2-27.                                                  NC1724.2
191800     MOVE   "DIVIDE INTO GIVING" TO FEATURE.                      NC1724.2
191900*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
192000*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1724.2
192100     MOVE    0      TO WRK-CS-18V00.                              NC1724.2
192200     MOVE   "0"     TO WRK-XN-00001.                              NC1724.2
192300     MOVE    0      TO WRK-DS-05V00.                              NC1724.2
192400     MOVE    0      TO WRK-DS-02V00.                              NC1724.2
192500     MOVE    0      TO DIV10.                                     NC1724.2
192600     MOVE    1      TO REC-CT.                                    NC1724.2
192700     MOVE    44.1   TO DIV2.                                      NC1724.2
192800     MOVE   -9.642  TO DIV4.                                      NC1724.2
192900 DIV-TEST-F2-27-0.                                                NC1724.2
193000     DIVIDE  DIV4 INTO DIV2                                       NC1724.2
193100      GIVING DIV10                                                NC1724.2
193200      NOT ON SIZE ERROR                                           NC1724.2
193300             MOVE   "1" TO WRK-XN-00001                           NC1724.2
193400             MOVE   23  TO WRK-DS-05V00                           NC1724.2
193500             MOVE   -4  TO WRK-DS-02V00                           NC1724.2
193600     END-DIVIDE                                                   NC1724.2
193700     MOVE    99    TO WRK-CS-18V00.                               NC1724.2
193800     GO TO   DIV-TEST-F2-27-1.                                    NC1724.2
193900 DIV-DELETE-F2-27-1.                                              NC1724.2
194000     PERFORM DE-LETE.                                             NC1724.2
194100     PERFORM PRINT-DETAIL.                                        NC1724.2
194200     GO TO   DIV-INIT-F2-28.                                      NC1724.2
194300 DIV-TEST-F2-27-1.                                                NC1724.2
194400     MOVE   "DIV-TEST-F2-27-1" TO PAR-NAME.                       NC1724.2
194500     IF      WRK-XN-00001 = "0"                                   NC1724.2
194600             PERFORM PASS                                         NC1724.2
194700             PERFORM PRINT-DETAIL                                 NC1724.2
194800     ELSE                                                         NC1724.2
194900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
195000             MOVE   "0"           TO CORRECT-X                    NC1724.2
195100             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1724.2
195200                  TO RE-MARK                                      NC1724.2
195300             PERFORM FAIL                                         NC1724.2
195400             PERFORM PRINT-DETAIL.                                NC1724.2
195500     ADD     1 TO REC-CT.                                         NC1724.2
195600 DIV-TEST-F2-27-2.                                                NC1724.2
195700     MOVE   "DIV-TEST-F2-27-2" TO PAR-NAME.                       NC1724.2
195800     IF      WRK-DS-02V00 = 0                                     NC1724.2
195900             PERFORM PASS                                         NC1724.2
196000             PERFORM PRINT-DETAIL                                 NC1724.2
196100     ELSE                                                         NC1724.2
196200             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1724.2
196300             MOVE    0            TO CORRECT-N                    NC1724.2
196400             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1724.2
196500                  TO RE-MARK                                      NC1724.2
196600             PERFORM FAIL                                         NC1724.2
196700             PERFORM PRINT-DETAIL.                                NC1724.2
196800     ADD     1 TO REC-CT.                                         NC1724.2
196900 DIV-TEST-F2-27-3.                                                NC1724.2
197000     MOVE   "DIV-TEST-F2-27-3" TO PAR-NAME.                       NC1724.2
197100     IF      WRK-DS-05V00 = 0                                     NC1724.2
197200             PERFORM PASS                                         NC1724.2
197300             PERFORM PRINT-DETAIL                                 NC1724.2
197400     ELSE                                                         NC1724.2
197500             MOVE    WRK-DS-05V00 TO COMPUTED-N                   NC1724.2
197600             MOVE    0            TO CORRECT-N                    NC1724.2
197700             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1724.2
197800                  TO RE-MARK                                      NC1724.2
197900             PERFORM FAIL                                         NC1724.2
198000             PERFORM PRINT-DETAIL.                                NC1724.2
198100     ADD     1 TO REC-CT.                                         NC1724.2
198200 DIV-TEST-F2-27-4.                                                NC1724.2
198300     MOVE   "DIV-TEST-F2-27-4" TO PAR-NAME.                       NC1724.2
198400     IF      DIV10    = 0                                         NC1724.2
198500             PERFORM PASS                                         NC1724.2
198600             PERFORM PRINT-DETAIL                                 NC1724.2
198700     ELSE                                                         NC1724.2
198800             MOVE    DIV10 TO COMPUTED-N                          NC1724.2
198900             MOVE    0     TO CORRECT-N                           NC1724.2
199000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
199100             PERFORM FAIL                                         NC1724.2
199200             PERFORM PRINT-DETAIL.                                NC1724.2
199300     ADD     1 TO REC-CT.                                         NC1724.2
199400 DIV-TEST-F2-27-5.                                                NC1724.2
199500     MOVE   "DIV-TEST-F2-27-5" TO PAR-NAME.                       NC1724.2
199600     IF      WRK-CS-18V00 = 000000000000000099                    NC1724.2
199700             PERFORM PASS                                         NC1724.2
199800             PERFORM PRINT-DETAIL                                 NC1724.2
199900     ELSE                                                         NC1724.2
200000             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1724.2
200100             MOVE    99           TO CORRECT-N                    NC1724.2
200200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1724.2
200300             PERFORM FAIL                                         NC1724.2
200400             PERFORM PRINT-DETAIL.                                NC1724.2
200500*                                                                 NC1724.2
200600*                                                                 NC1724.2
200700 DIV-INIT-F2-28.                                                  NC1724.2
200800*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
200900*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1724.2
201000     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1724.2
201100     MOVE    1      TO REC-CT.                                    NC1724.2
201200     MOVE    0      TO WRK-DS-05V00.                              NC1724.2
201300     MOVE    0      TO WRK-DS-02V00.                              NC1724.2
201400     MOVE   "0"     TO WRK-XN-00001.                              NC1724.2
201500     MOVE    0      TO WRK-CS-18V00.                              NC1724.2
201600 DIV-TEST-F2-28-0.                                                NC1724.2
201700     DIVIDE  A18ONES-DS-09V09 INTO WRK-DS-09V09                   NC1724.2
201800      GIVING WRK-DS-09V09                                         NC1724.2
201900      NOT ON SIZE ERROR                                           NC1724.2
202000             MOVE   "1" TO WRK-XN-00001                           NC1724.2
202100             MOVE   23  TO WRK-DS-05V00                           NC1724.2
202200             MOVE   -4  TO WRK-DS-02V00                           NC1724.2
202300      END-DIVIDE                                                  NC1724.2
202400      MOVE   99 TO WRK-CS-18V00.                                  NC1724.2
202500     GO TO   DIV-TEST-F2-28-1.                                    NC1724.2
202600 DIV-DELETE-F2-28-1.                                              NC1724.2
202700     PERFORM DE-LETE.                                             NC1724.2
202800     PERFORM PRINT-DETAIL.                                        NC1724.2
202900     GO TO   DIV-INIT-F2-29.                                      NC1724.2
203000 DIV-TEST-F2-28-1.                                                NC1724.2
203100     MOVE   "DIV-TEST-F2-28-1" TO PAR-NAME.                       NC1724.2
203200     IF      WRK-XN-00001 = "1"                                   NC1724.2
203300             PERFORM PASS                                         NC1724.2
203400             PERFORM PRINT-DETAIL                                 NC1724.2
203500     ELSE                                                         NC1724.2
203600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
203700             MOVE   "1"           TO CORRECT-X                    NC1724.2
203800             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
203900                  TO RE-MARK                                      NC1724.2
204000             PERFORM FAIL                                         NC1724.2
204100             PERFORM PRINT-DETAIL.                                NC1724.2
204200     ADD     1 TO REC-CT.                                         NC1724.2
204300 DIV-TEST-F2-28-2.                                                NC1724.2
204400     MOVE   "DIV-TEST-F2-28-2" TO PAR-NAME.                       NC1724.2
204500     IF      WRK-DS-02V00 = -4                                    NC1724.2
204600             PERFORM PASS                                         NC1724.2
204700             PERFORM PRINT-DETAIL                                 NC1724.2
204800     ELSE                                                         NC1724.2
204900             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1724.2
205000             MOVE    -4           TO CORRECT-N                    NC1724.2
205100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
205200                  TO RE-MARK                                      NC1724.2
205300             PERFORM FAIL                                         NC1724.2
205400             PERFORM PRINT-DETAIL.                                NC1724.2
205500     ADD     1 TO REC-CT.                                         NC1724.2
205600 DIV-TEST-F2-28-3.                                                NC1724.2
205700     MOVE   "DIV-TEST-F2-28-3" TO PAR-NAME.                       NC1724.2
205800     IF      WRK-DS-05V00 = 23                                    NC1724.2
205900             PERFORM PASS                                         NC1724.2
206000             PERFORM PRINT-DETAIL                                 NC1724.2
206100     ELSE                                                         NC1724.2
206200             MOVE    WRK-DS-05V00 TO COMPUTED-N                   NC1724.2
206300             MOVE    23           TO CORRECT-N                    NC1724.2
206400             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
206500                  TO RE-MARK                                      NC1724.2
206600             PERFORM FAIL                                         NC1724.2
206700             PERFORM PRINT-DETAIL.                                NC1724.2
206800     ADD     1 TO REC-CT.                                         NC1724.2
206900 DIV-TEST-F2-28-4.                                                NC1724.2
207000     MOVE   "DIV-TEST-F2-28-4" TO PAR-NAME.                       NC1724.2
207100     IF      WRK-DS-18V00-S  =   000000001000000000               NC1724.2
207200             PERFORM PASS                                         NC1724.2
207300             PERFORM PRINT-DETAIL                                 NC1724.2
207400     ELSE                                                         NC1724.2
207500             MOVE    WRK-DS-18V00-S     TO COMPUTED-N             NC1724.2
207600             MOVE    000000001000000000 TO CORRECT-18V0           NC1724.2
207700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
207800             PERFORM FAIL                                         NC1724.2
207900             PERFORM PRINT-DETAIL.                                NC1724.2
208000     ADD     1 TO REC-CT.                                         NC1724.2
208100 DIV-TEST-F2-28-5.                                                NC1724.2
208200     MOVE   "DIV-TEST-F2-28-5" TO PAR-NAME.                       NC1724.2
208300     IF      WRK-CS-18V00 = 000000000000000099                    NC1724.2
208400             PERFORM PASS                                         NC1724.2
208500             PERFORM PRINT-DETAIL                                 NC1724.2
208600     ELSE                                                         NC1724.2
208700             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1724.2
208800             MOVE    99           TO CORRECT-N                    NC1724.2
208900             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1724.2
209000             PERFORM FAIL                                         NC1724.2
209100             PERFORM PRINT-DETAIL.                                NC1724.2
209200*                                                                 NC1724.2
209300*                                                                 NC1724.2
209400 DIV-INIT-F2-29.                                                  NC1724.2
209500*            ==-->    SIZE ERROR CONDITION <--==                  NC1724.2
209600*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1724.2
209700     MOVE    1      TO REC-CT.                                    NC1724.2
209800     MOVE    0      TO WRK-CS-18V00.                              NC1724.2
209900     MOVE   "0"     TO WRK-XN-00001.                              NC1724.2
210000     MOVE    0      TO DIV10.                                     NC1724.2
210100     MOVE    44.1   TO DIV2.                                      NC1724.2
210200     MOVE   -9.642  TO DIV4.                                      NC1724.2
210300 DIV-TEST-F2-29-0.                                                NC1724.2
210400     DIVIDE  DIV4 INTO DIV2                                       NC1724.2
210500      GIVING DIV10                                                NC1724.2
210600          ON SIZE ERROR                                           NC1724.2
210700             MOVE   "1" TO WRK-XN-00001                           NC1724.2
210800      NOT ON SIZE ERROR                                           NC1724.2
210900             MOVE   "2" TO WRK-XN-00001                           NC1724.2
211000     END-DIVIDE                                                   NC1724.2
211100     MOVE    99    TO WRK-CS-18V00.                               NC1724.2
211200     GO TO   DIV-TEST-F2-29-1.                                    NC1724.2
211300 DIV-DELETE-F2-29-1.                                              NC1724.2
211400     PERFORM DE-LETE.                                             NC1724.2
211500     PERFORM PRINT-DETAIL.                                        NC1724.2
211600     GO TO   DIV-INIT-F2-30.                                      NC1724.2
211700 DIV-TEST-F2-29-1.                                                NC1724.2
211800     MOVE   "DIV-TEST-F2-29-1" TO PAR-NAME.                       NC1724.2
211900     IF      WRK-XN-00001 = "1"                                   NC1724.2
212000             PERFORM PASS                                         NC1724.2
212100             PERFORM PRINT-DETAIL                                 NC1724.2
212200     ELSE                                                         NC1724.2
212300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
212400             MOVE   "1"           TO CORRECT-X                    NC1724.2
212500             MOVE   "ON SIZE ERROR NOT EXECUTED"                  NC1724.2
212600                  TO RE-MARK                                      NC1724.2
212700             PERFORM FAIL                                         NC1724.2
212800             PERFORM PRINT-DETAIL.                                NC1724.2
212900     ADD     1 TO REC-CT.                                         NC1724.2
213000 DIV-TEST-F2-29-2.                                                NC1724.2
213100     MOVE   "DIV-TEST-F2-29-2" TO PAR-NAME.                       NC1724.2
213200     IF      DIV10   =   0                                        NC1724.2
213300             PERFORM PASS                                         NC1724.2
213400             PERFORM PRINT-DETAIL                                 NC1724.2
213500     ELSE                                                         NC1724.2
213600             MOVE    DIV10 TO COMPUTED-N                          NC1724.2
213700             MOVE    0     TO CORRECT-N                           NC1724.2
213800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
213900             PERFORM FAIL                                         NC1724.2
214000             PERFORM PRINT-DETAIL.                                NC1724.2
214100     ADD     1 TO REC-CT.                                         NC1724.2
214200 DIV-TEST-F2-29-3.                                                NC1724.2
214300     MOVE   "DIV-TEST-F2-29-3" TO PAR-NAME.                       NC1724.2
214400     IF      WRK-CS-18V00 = 000000000000000099                    NC1724.2
214500             PERFORM PASS                                         NC1724.2
214600             PERFORM PRINT-DETAIL                                 NC1724.2
214700     ELSE                                                         NC1724.2
214800             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1724.2
214900             MOVE    99           TO CORRECT-N                    NC1724.2
215000             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1724.2
215100             PERFORM FAIL                                         NC1724.2
215200             PERFORM PRINT-DETAIL.                                NC1724.2
215300*                                                                 NC1724.2
215400*                                                                 NC1724.2
215500 DIV-INIT-F2-30.                                                  NC1724.2
215600*            ==--> NO SIZE ERROR CONDITION <--==                  NC1724.2
215700*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1724.2
215800     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1724.2
215900     MOVE   "0"     TO WRK-XN-00001.                              NC1724.2
216000     MOVE    0      TO WRK-CS-18V00.                              NC1724.2
216100 DIV-TEST-F2-30-0.                                                NC1724.2
216200     DIVIDE  A18ONES-DS-09V09 INTO WRK-DS-09V09                   NC1724.2
216300      GIVING WRK-DS-09V09                                         NC1724.2
216400          ON SIZE ERROR                                           NC1724.2
216500             MOVE   "1" TO WRK-XN-00001                           NC1724.2
216600      NOT ON SIZE ERROR                                           NC1724.2
216700             MOVE   "2" TO WRK-XN-00001                           NC1724.2
216800     END-DIVIDE                                                   NC1724.2
216900     MOVE   99 TO WRK-CS-18V00.                                   NC1724.2
217000     GO TO   DIV-TEST-F2-30-1.                                    NC1724.2
217100 DIV-DELETE-F2-30-1.                                              NC1724.2
217200     PERFORM DE-LETE.                                             NC1724.2
217300     PERFORM PRINT-DETAIL.                                        NC1724.2
217400     GO TO   CCVS-EXIT.                                           NC1724.2
217500 DIV-TEST-F2-30-1.                                                NC1724.2
217600     MOVE   "DIV-TEST-F2-30-1" TO PAR-NAME.                       NC1724.2
217700     IF      WRK-XN-00001 = "2"                                   NC1724.2
217800             PERFORM PASS                                         NC1724.2
217900             PERFORM PRINT-DETAIL                                 NC1724.2
218000     ELSE                                                         NC1724.2
218100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1724.2
218200             MOVE   "2"           TO CORRECT-X                    NC1724.2
218300             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1724.2
218400                  TO RE-MARK                                      NC1724.2
218500             PERFORM FAIL                                         NC1724.2
218600             PERFORM PRINT-DETAIL.                                NC1724.2
218700     ADD     1 TO REC-CT.                                         NC1724.2
218800 DIV-TEST-F2-30-2.                                                NC1724.2
218900     MOVE   "DIV-TEST-F2-30-2" TO PAR-NAME.                       NC1724.2
219000     IF      WRK-DS-18V00-S  =   000000001000000000               NC1724.2
219100             PERFORM PASS                                         NC1724.2
219200             PERFORM PRINT-DETAIL                                 NC1724.2
219300     ELSE                                                         NC1724.2
219400             MOVE    WRK-DS-18V00-S     TO COMPUTED-N             NC1724.2
219500             MOVE    000000001000000000 TO CORRECT-18V0           NC1724.2
219600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1724.2
219700             PERFORM FAIL                                         NC1724.2
219800             PERFORM PRINT-DETAIL.                                NC1724.2
219900     ADD     1 TO REC-CT.                                         NC1724.2
220000 DIV-TEST-F2-30-3.                                                NC1724.2
220100     MOVE   "DIV-TEST-F2-30-3" TO PAR-NAME.                       NC1724.2
220200     IF      WRK-CS-18V00 = 000000000000000099                    NC1724.2
220300             PERFORM PASS                                         NC1724.2
220400             PERFORM PRINT-DETAIL                                 NC1724.2
220500     ELSE                                                         NC1724.2
220600             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1724.2
220700             MOVE    99           TO CORRECT-N                    NC1724.2
220800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1724.2
220900             PERFORM FAIL                                         NC1724.2
221000             PERFORM PRINT-DETAIL.                                NC1724.2
221100*                                                                 NC1724.2
221200*                                                                 NC1724.2
221300 CCVS-EXIT SECTION.                                               NC1724.2
221400 CCVS-999999.                                                     NC1724.2
221500     GO TO CLOSE-FILES.                                           NC1724.2
      *END-OF,NC172A                                                                  
