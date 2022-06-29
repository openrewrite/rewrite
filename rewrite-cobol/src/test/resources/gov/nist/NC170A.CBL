      *HEADER,COBOL,NC170A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1704.2
000200 PROGRAM-ID.                                                      NC1704.2
000300     NC170A.                                                      NC1704.2
000400****************************************************************  NC1704.2
000500*                                                              *  NC1704.2
000600*    VALIDATION FOR:-                                          *  NC1704.2
000700*                                                              *  NC1704.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1704.2
000900*                                                              *  NC1704.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1704.2
001100*                                                              *  NC1704.2
001200****************************************************************  NC1704.2
001300*                                                              *  NC1704.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1704.2
001500*                                                              *  NC1704.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1704.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1704.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1704.2
001900*                                                              *  NC1704.2
002000****************************************************************  NC1704.2
002100*                                                                 NC1704.2
002200*    THIS PROGRAM TESTS THE FORMAT 2 MULTIPLY STATEMENT FOUND     NC1704.2
002300*    IN LEVEL 1.   ALL COMBINATIONS OF THE OPTIONAL PHRASES       NC1704.2
002400*    "SIZE ERROR", "NOT SIZE ERROR" AND "END-MULTIPLY" ARE        NC1704.2
002500*    TESTED, AS WELL AS THE ROUNDED OPTION.                       NC1704.2
002600*                                                                 NC1704.2
002700*    VARIOUS COMBINATIONS OF SIGNED AND UNSIGNED NUMERIC          NC1704.2
002800*    LITERALS, DISPLAY AND COMPUTATIONAL FIELDS ARE USED          NC1704.2
002900*    AS OPERANDS.                                                 NC1704.2
003000*                                                                 NC1704.2
003100                                                                  NC1704.2
003200 ENVIRONMENT DIVISION.                                            NC1704.2
003300 CONFIGURATION SECTION.                                           NC1704.2
003400 SOURCE-COMPUTER.                                                 NC1704.2
003500     XXXXX082.                                                    NC1704.2
003600 OBJECT-COMPUTER.                                                 NC1704.2
003700     XXXXX083.                                                    NC1704.2
003800 INPUT-OUTPUT SECTION.                                            NC1704.2
003900 FILE-CONTROL.                                                    NC1704.2
004000     SELECT PRINT-FILE ASSIGN TO                                  NC1704.2
004100     XXXXX055.                                                    NC1704.2
004200 DATA DIVISION.                                                   NC1704.2
004300 FILE SECTION.                                                    NC1704.2
004400 FD  PRINT-FILE.                                                  NC1704.2
004500 01  PRINT-REC PICTURE X(120).                                    NC1704.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 NC1704.2
004700 WORKING-STORAGE SECTION.                                         NC1704.2
004800 77  WRK-DS-18V00                PICTURE S9(18).                  NC1704.2
004900 77  A06THREES-DS-03V03          PICTURE S999V999 VALUE 333.333.  NC1704.2
005000 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC1704.2
005100 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06 PICTURE S9(12).        NC1704.2
005200 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1704.2
005300 77  WRK-DS-10V00                PICTURE S9(10).                  NC1704.2
005400 77  WRK-XN-00001                PICTURE X.                       NC1704.2
005500 77  A10ONES-DS-10V00            PICTURE S9(10)                   NC1704.2
005600                                 VALUE 1111111111.                NC1704.2
005700 77  A12THREES-DS-06V06          PICTURE S9(6)V9(6)               NC1704.2
005800                                 VALUE 333333.333333.             NC1704.2
005900 77  WRK-DS-02V00                PICTURE S99.                     NC1704.2
006000 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1704.2
006100 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1704.2
006200 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1704.2
006300 77  A05ONES-DS-00V05            PICTURE SV9(5) VALUE .11111.     NC1704.2
006400 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1704.2
006500                                 VALUE 111111111111.              NC1704.2
006600 77  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1704.2
006700 77  WRK-DS-09V08                PICTURE S9(9)V9(8).              NC1704.2
006800 77  WRK-DS-17V00-S REDEFINES WRK-DS-09V08 PICTURE S9(17).        NC1704.2
006900 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1704.2
007000                                 VALUE 111111111111111111.        NC1704.2
007100 77  WRK-DS-0201P                PICTURE S99P.                    NC1704.2
007200 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1704.2
007300 77  WRK-DU-18V00                PICTURE 9(18).                   NC1704.2
007400 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1704.2
007500                                 VALUE 99.                        NC1704.2
007600 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1704.2
007700                                 VALUE .1.                        NC1704.2
007800 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1704.2
007900 77  WRK-DS-12V00                PICTURE S9(12).                  NC1704.2
008000 77  WRK-DS-01V00                PICTURE S9.                      NC1704.2
008100 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1704.2
008200 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1704.2
008300                                 VALUE 111111111.111111111.       NC1704.2
008400 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1704.2
008500 77  WRK-DS-05V00                PICTURE S9(5).                   NC1704.2
008600 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1704.2
008700 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1704.2
008800 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1704.2
008900 77  XRAY                        PICTURE X.                       NC1704.2
009000 01  WRK-XN-18-1 PIC X(18).                                       NC1704.2
009100 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1704.2
009200 01  WRK-DU-X-18V0-1; REDEFINES WRK-XN-18-1 PIC 9(18).            NC1704.2
009300 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1704.2
009400 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1704.2
009500 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1704.2
009600 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1704.2
009700 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1704.2
009800 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1704.2
009900 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1704.2
010000 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1704.2
010100 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1704.2
010200 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1704.2
010300 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1704.2
010400 01  WRK-DU-2V0-1 PIC 99.                                         NC1704.2
010500 01  WRK-DU-2V0-2 PIC 99.                                         NC1704.2
010600 01  WRK-DU-2V0-3 PIC 99.                                         NC1704.2
010700 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1704.2
010800 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1704.2
010900 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1704.2
011000 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1704.2
011100 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1704.2
011200 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1704.2
011300 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1704.2
011400 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1704.2
011500 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1704.2
011600 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1704.2
011700 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1704.2
011800 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1704.2
011900 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1704.2
012000 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1704.2
012100 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1704.2
012200 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1704.2
012300 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1704.2
012400 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1704.2
012500 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1704.2
012600 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1704.2
012700 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1704.2
012800 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1704.2
012900 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1704.2
013000 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1704.2
013100 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1704.2
013200 01  WRK-NE-2 PIC $**.99.                                         NC1704.2
013300 01  WRK-NE-3 PIC $99.99CR.                                       NC1704.2
013400 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1704.2
013500 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1704.2
013600             VALUE +000000000000000001.                           NC1704.2
013700 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1704.2
013800             VALUE -000000000000000033.                           NC1704.2
013900 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1704.2
014000             VALUE 666666666666666666.                            NC1704.2
014100 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1704.2
014200             VALUE 009999999999999999.                            NC1704.2
014300 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1704.2
014400             VALUE 000022222222222222.                            NC1704.2
014500 01  MULTIPLY-DATA.                                               NC1704.2
014600     02 MULT1                           PICTURE IS 999V99         NC1704.2
014700     VALUE IS 80.12.                                              NC1704.2
014800     02 MULT2                           PICTURE IS 999V999.       NC1704.2
014900     02 MULT3                           PICTURE IS $$99.99.       NC1704.2
015000     02 MULT4                           PICTURE IS S99            NC1704.2
015100     VALUE IS -56.                                                NC1704.2
015200     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1704.2
015300     02 MULT6                           PICTURE IS 99 VALUE IS    NC1704.2
015400     20.                                                          NC1704.2
015500 01  DIVIDE-DATA.                                                 NC1704.2
015600     02 DIV1                            PICTURE IS 9(4)V99        NC1704.2
015700     VALUE IS 1620.36.                                            NC1704.2
015800     02 DIV2                            PICTURE IS 99V9           NC1704.2
015900     VALUE IS 44.1.                                               NC1704.2
016000     02 DIV3                            PICTURE IS 9(4)V9         NC1704.2
016100     VALUE IS 1661.7.                                             NC1704.2
016200     02 DIV4                            PICTURE IS S9V999         NC1704.2
016300     VALUE IS -9.642.                                             NC1704.2
016400     02 DIV-02LEVEL-1.                                            NC1704.2
016500     03 DIV5                            PICTURE IS V99            NC1704.2
016600     VALUE IS .82.                                                NC1704.2
016700     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1704.2
016800     03 DIV7                            PICTURE IS 9V9            NC1704.2
016900     VALUE IS 9.6.                                                NC1704.2
017000 01  DIV-DATA-2.                                                  NC1704.2
017100     02 DIV8                            PICTURE IS 99V9.          NC1704.2
017200     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1704.2
017300     02 DIV10                           PICTURE IS V999.          NC1704.2
017400 01  TEST-RESULTS.                                                NC1704.2
017500     02 FILLER                   PIC X      VALUE SPACE.          NC1704.2
017600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1704.2
017700     02 FILLER                   PIC X      VALUE SPACE.          NC1704.2
017800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1704.2
017900     02 FILLER                   PIC X      VALUE SPACE.          NC1704.2
018000     02  PAR-NAME.                                                NC1704.2
018100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1704.2
018200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1704.2
018300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1704.2
018400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1704.2
018500     02 RE-MARK                  PIC X(61).                       NC1704.2
018600 01  TEST-COMPUTED.                                               NC1704.2
018700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1704.2
018800     02 FILLER                   PIC X(17)  VALUE                 NC1704.2
018900            "       COMPUTED=".                                   NC1704.2
019000     02 COMPUTED-X.                                               NC1704.2
019100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1704.2
019200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1704.2
019300                                 PIC -9(9).9(9).                  NC1704.2
019400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1704.2
019500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1704.2
019600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1704.2
019700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1704.2
019800         04 COMPUTED-18V0                    PIC -9(18).          NC1704.2
019900         04 FILLER                           PIC X.               NC1704.2
020000     03 FILLER PIC X(50) VALUE SPACE.                             NC1704.2
020100 01  TEST-CORRECT.                                                NC1704.2
020200     02 FILLER PIC X(30) VALUE SPACE.                             NC1704.2
020300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1704.2
020400     02 CORRECT-X.                                                NC1704.2
020500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1704.2
020600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1704.2
020700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1704.2
020800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1704.2
020900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1704.2
021000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1704.2
021100         04 CORRECT-18V0                     PIC -9(18).          NC1704.2
021200         04 FILLER                           PIC X.               NC1704.2
021300     03 FILLER PIC X(2) VALUE SPACE.                              NC1704.2
021400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1704.2
021500 01  CCVS-C-1.                                                    NC1704.2
021600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1704.2
021700-    "SS  PARAGRAPH-NAME                                          NC1704.2
021800-    "       REMARKS".                                            NC1704.2
021900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1704.2
022000 01  CCVS-C-2.                                                    NC1704.2
022100     02 FILLER                     PIC X        VALUE SPACE.      NC1704.2
022200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1704.2
022300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1704.2
022400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1704.2
022500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1704.2
022600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1704.2
022700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1704.2
022800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1704.2
022900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1704.2
023000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1704.2
023100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1704.2
023200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1704.2
023300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1704.2
023400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1704.2
023500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1704.2
023600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1704.2
023700 01  CCVS-H-1.                                                    NC1704.2
023800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1704.2
023900     02  FILLER                    PIC X(42)    VALUE             NC1704.2
024000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1704.2
024100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1704.2
024200 01  CCVS-H-2A.                                                   NC1704.2
024300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1704.2
024400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1704.2
024500   02  FILLER                        PIC XXXX   VALUE             NC1704.2
024600     "4.2 ".                                                      NC1704.2
024700   02  FILLER                        PIC X(28)  VALUE             NC1704.2
024800            " COPY - NOT FOR DISTRIBUTION".                       NC1704.2
024900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1704.2
025000                                                                  NC1704.2
025100 01  CCVS-H-2B.                                                   NC1704.2
025200   02  FILLER                        PIC X(15)  VALUE             NC1704.2
025300            "TEST RESULT OF ".                                    NC1704.2
025400   02  TEST-ID                       PIC X(9).                    NC1704.2
025500   02  FILLER                        PIC X(4)   VALUE             NC1704.2
025600            " IN ".                                               NC1704.2
025700   02  FILLER                        PIC X(12)  VALUE             NC1704.2
025800     " HIGH       ".                                              NC1704.2
025900   02  FILLER                        PIC X(22)  VALUE             NC1704.2
026000            " LEVEL VALIDATION FOR ".                             NC1704.2
026100   02  FILLER                        PIC X(58)  VALUE             NC1704.2
026200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1704.2
026300 01  CCVS-H-3.                                                    NC1704.2
026400     02  FILLER                      PIC X(34)  VALUE             NC1704.2
026500            " FOR OFFICIAL USE ONLY    ".                         NC1704.2
026600     02  FILLER                      PIC X(58)  VALUE             NC1704.2
026700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1704.2
026800     02  FILLER                      PIC X(28)  VALUE             NC1704.2
026900            "  COPYRIGHT   1985 ".                                NC1704.2
027000 01  CCVS-E-1.                                                    NC1704.2
027100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1704.2
027200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1704.2
027300     02 ID-AGAIN                     PIC X(9).                    NC1704.2
027400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1704.2
027500 01  CCVS-E-2.                                                    NC1704.2
027600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1704.2
027700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1704.2
027800     02 CCVS-E-2-2.                                               NC1704.2
027900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1704.2
028000         03 FILLER                   PIC X      VALUE SPACE.      NC1704.2
028100         03 ENDER-DESC               PIC X(44)  VALUE             NC1704.2
028200            "ERRORS ENCOUNTERED".                                 NC1704.2
028300 01  CCVS-E-3.                                                    NC1704.2
028400     02  FILLER                      PIC X(22)  VALUE             NC1704.2
028500            " FOR OFFICIAL USE ONLY".                             NC1704.2
028600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1704.2
028700     02  FILLER                      PIC X(58)  VALUE             NC1704.2
028800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1704.2
028900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1704.2
029000     02 FILLER                       PIC X(15)  VALUE             NC1704.2
029100             " COPYRIGHT 1985".                                   NC1704.2
029200 01  CCVS-E-4.                                                    NC1704.2
029300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1704.2
029400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1704.2
029500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1704.2
029600     02 FILLER                       PIC X(40)  VALUE             NC1704.2
029700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1704.2
029800 01  XXINFO.                                                      NC1704.2
029900     02 FILLER                       PIC X(19)  VALUE             NC1704.2
030000            "*** INFORMATION ***".                                NC1704.2
030100     02 INFO-TEXT.                                                NC1704.2
030200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1704.2
030300       04 XXCOMPUTED                 PIC X(20).                   NC1704.2
030400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1704.2
030500       04 XXCORRECT                  PIC X(20).                   NC1704.2
030600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1704.2
030700 01  HYPHEN-LINE.                                                 NC1704.2
030800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1704.2
030900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1704.2
031000-    "*****************************************".                 NC1704.2
031100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1704.2
031200-    "******************************".                            NC1704.2
031300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1704.2
031400     "NC170A".                                                    NC1704.2
031500 PROCEDURE DIVISION.                                              NC1704.2
031600 CCVS1 SECTION.                                                   NC1704.2
031700 OPEN-FILES.                                                      NC1704.2
031800     OPEN     OUTPUT PRINT-FILE.                                  NC1704.2
031900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1704.2
032000     MOVE    SPACE TO TEST-RESULTS.                               NC1704.2
032100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1704.2
032200     GO TO CCVS1-EXIT.                                            NC1704.2
032300 CLOSE-FILES.                                                     NC1704.2
032400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1704.2
032500 TERMINATE-CCVS.                                                  NC1704.2
032600*    EXIT PROGRAM.                                                NC1704.2
032700*TERMINATE-CALL.                                                  NC1704.2
032800     STOP     RUN.                                                NC1704.2
032900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1704.2
033000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1704.2
033100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1704.2
033200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1704.2
033300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1704.2
033400 PRINT-DETAIL.                                                    NC1704.2
033500     IF REC-CT NOT EQUAL TO ZERO                                  NC1704.2
033600             MOVE "." TO PARDOT-X                                 NC1704.2
033700             MOVE REC-CT TO DOTVALUE.                             NC1704.2
033800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1704.2
033900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1704.2
034000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1704.2
034100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1704.2
034200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1704.2
034300     MOVE SPACE TO CORRECT-X.                                     NC1704.2
034400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1704.2
034500     MOVE     SPACE TO RE-MARK.                                   NC1704.2
034600 HEAD-ROUTINE.                                                    NC1704.2
034700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1704.2
034800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1704.2
034900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1704.2
035000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1704.2
035100 COLUMN-NAMES-ROUTINE.                                            NC1704.2
035200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1704.2
035300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1704.2
035400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1704.2
035500 END-ROUTINE.                                                     NC1704.2
035600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1704.2
035700 END-RTN-EXIT.                                                    NC1704.2
035800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1704.2
035900 END-ROUTINE-1.                                                   NC1704.2
036000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1704.2
036100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1704.2
036200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1704.2
036300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1704.2
036400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1704.2
036500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1704.2
036600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1704.2
036700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1704.2
036800  END-ROUTINE-12.                                                 NC1704.2
036900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1704.2
037000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1704.2
037100         MOVE "NO " TO ERROR-TOTAL                                NC1704.2
037200         ELSE                                                     NC1704.2
037300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1704.2
037400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1704.2
037500     PERFORM WRITE-LINE.                                          NC1704.2
037600 END-ROUTINE-13.                                                  NC1704.2
037700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1704.2
037800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1704.2
037900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1704.2
038000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1704.2
038100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1704.2
038200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1704.2
038300          MOVE "NO " TO ERROR-TOTAL                               NC1704.2
038400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1704.2
038500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1704.2
038600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1704.2
038700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1704.2
038800 WRITE-LINE.                                                      NC1704.2
038900     ADD 1 TO RECORD-COUNT.                                       NC1704.2
039000*    IF RECORD-COUNT GREATER 42                                   NC1704.2
039100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1704.2
039200*        MOVE SPACE TO DUMMY-RECORD                               NC1704.2
039300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1704.2
039400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1704.2
039500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1704.2
039600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1704.2
039700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1704.2
039800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1704.2
039900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1704.2
040000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1704.2
040100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1704.2
040200*        MOVE ZERO TO RECORD-COUNT.                               NC1704.2
040300     PERFORM WRT-LN.                                              NC1704.2
040400 WRT-LN.                                                          NC1704.2
040500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1704.2
040600     MOVE SPACE TO DUMMY-RECORD.                                  NC1704.2
040700 BLANK-LINE-PRINT.                                                NC1704.2
040800     PERFORM WRT-LN.                                              NC1704.2
040900 FAIL-ROUTINE.                                                    NC1704.2
041000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1704.2
041100            GO TO FAIL-ROUTINE-WRITE.                             NC1704.2
041200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1704.2
041300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1704.2
041400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1704.2
041500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1704.2
041600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1704.2
041700     GO TO  FAIL-ROUTINE-EX.                                      NC1704.2
041800 FAIL-ROUTINE-WRITE.                                              NC1704.2
041900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1704.2
042000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1704.2
042100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1704.2
042200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1704.2
042300 FAIL-ROUTINE-EX. EXIT.                                           NC1704.2
042400 BAIL-OUT.                                                        NC1704.2
042500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1704.2
042600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1704.2
042700 BAIL-OUT-WRITE.                                                  NC1704.2
042800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1704.2
042900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1704.2
043000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1704.2
043100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1704.2
043200 BAIL-OUT-EX. EXIT.                                               NC1704.2
043300 CCVS1-EXIT.                                                      NC1704.2
043400     EXIT.                                                        NC1704.2
043500 SECT-NC170A-001 SECTION.                                         NC1704.2
043600                                                                  NC1704.2
043700 MPY-INIT-F2-1.                                                   NC1704.2
043800     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
043900     MOVE   "MULTIPLY BY GIVING" TO FEATURE.                      NC1704.2
044000     MOVE    80.12 TO MULT1.                                      NC1704.2
044100     MOVE    ZERO  TO MULT2.                                      NC1704.2
044200 MPY-TEST-F2-1-0.                                                 NC1704.2
044300     MULTIPLY MULT1 BY 4.3 GIVING MULT2.                          NC1704.2
044400 MPY-TEST-F2-1-1.                                                 NC1704.2
044500     IF      MULT2 NOT EQUAL TO 344.516                           NC1704.2
044600             GO TO MPY-FAIL-F2-1-1.                               NC1704.2
044700     PERFORM PASS                                                 NC1704.2
044800              GO TO MPY-WRITE-F2-1-1.                             NC1704.2
044900 MPY-DELETE-F2-1-1.                                               NC1704.2
045000     PERFORM  DE-LETE.                                            NC1704.2
045100     GO TO    MPY-WRITE-F2-1-1.                                   NC1704.2
045200 MPY-FAIL-F2-1-1.                                                 NC1704.2
045300     PERFORM  FAIL.                                               NC1704.2
045400     MOVE     MULT2  TO COMPUTED-N.                               NC1704.2
045500     MOVE    +344.516 TO CORRECT-N.                               NC1704.2
045600 MPY-WRITE-F2-1-1.                                                NC1704.2
045700     MOVE "MPY-TEST-F2-1-1" TO PAR-NAME.                          NC1704.2
045800     PERFORM  PRINT-DETAIL.                                       NC1704.2
045900                                                                  NC1704.2
046000 MPY-INIT-F2-2.                                                   NC1704.2
046100     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
046200     MOVE    80.12 TO MULT1.                                      NC1704.2
046300     MOVE    ZERO  TO MULT3.                                      NC1704.2
046400 MPY-TEST-F2-2-0.                                                 NC1704.2
046500     MULTIPLY .9 BY MULT1 GIVING MULT3 ROUNDED.                   NC1704.2
046600 MPY-TEST-F2-2-1.                                                 NC1704.2
046700     IF       MULT3 NOT EQUAL TO " $72.11"                        NC1704.2
046800              GO TO MPY-FAIL-F2-2-1.                              NC1704.2
046900     PERFORM  PASS.                                               NC1704.2
047000     GO TO    MPY-WRITE-F2-2-1.                                   NC1704.2
047100 MPY-DELETE-F2-2-1.                                               NC1704.2
047200     PERFORM  DE-LETE.                                            NC1704.2
047300     GO TO    MPY-WRITE-F2-2-1.                                   NC1704.2
047400 MPY-FAIL-F2-2-1.                                                 NC1704.2
047500     PERFORM  FAIL.                                               NC1704.2
047600     MOVE     MULT3 TO COMPUTED-A.                                NC1704.2
047700     MOVE " $72.11" TO CORRECT-A.                                 NC1704.2
047800 MPY-WRITE-F2-2-1.                                                NC1704.2
047900     MOVE "MPY-TEST-F2-2-1" TO PAR-NAME.                          NC1704.2
048000     PERFORM  PRINT-DETAIL.                                       NC1704.2
048100                                                                  NC1704.2
048200 MPY-INIT-F2-3.                                                   NC1704.2
048300     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
048400     MOVE    80.12 TO MULT1.                                      NC1704.2
048500     MOVE   -56    TO MULT4.                                      NC1704.2
048600     MOVE    4     TO MULT5.                                      NC1704.2
048700     MOVE   "A"    TO XRAY.                                       NC1704.2
048800 MPY-TEST-F2-3-0.                                                 NC1704.2
048900     MULTIPLY MULT4 BY MULT1 GIVING MULT5 ON SIZE ERROR           NC1704.2
049000              MOVE "H" TO XRAY.                                   NC1704.2
049100 MPY-TEST-F2-3-1.                                                 NC1704.2
049200     IF       XRAY EQUAL TO "H"                                   NC1704.2
049300              PERFORM PASS                                        NC1704.2
049400     ELSE                                                         NC1704.2
049500              GO TO MPY-FAIL-F2-3-1.                              NC1704.2
049600     GO TO    MPY-WRITE-F2-3-1.                                   NC1704.2
049700 MPY-DELETE-F2-3-1.                                               NC1704.2
049800     PERFORM  DE-LETE.                                            NC1704.2
049900     GO TO    MPY-WRITE-F2-3-1.                                   NC1704.2
050000 MPY-FAIL-F2-3-1.                                                 NC1704.2
050100     MOVE     XRAY  TO COMPUTED-X.                                NC1704.2
050200     MOVE    "H"    TO CORRECT-X.                                 NC1704.2
050300     MOVE    "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.             NC1704.2
050400     PERFORM  FAIL.                                               NC1704.2
050500 MPY-WRITE-F2-3-1.                                                NC1704.2
050600     MOVE "MPY-TEST-F2-3-1 " TO PAR-NAME.                         NC1704.2
050700     PERFORM  PRINT-DETAIL.                                       NC1704.2
050800 MPY-TEST-F2-3-2.                                                 NC1704.2
050900     IF       MULT5 NOT EQUAL TO 4                                NC1704.2
051000              GO TO MPY-FAIL-F2-3-2.                              NC1704.2
051100     PERFORM  PASS.                                               NC1704.2
051200     GO TO    MPY-WRITE-F2-3-2.                                   NC1704.2
051300 MPY-DELETE-F2-3-2.                                               NC1704.2
051400     PERFORM  DE-LETE.                                            NC1704.2
051500     GO TO    MPY-WRITE-F2-3-2.                                   NC1704.2
051600 MPY-FAIL-F2-3-2.                                                 NC1704.2
051700     PERFORM  FAIL.                                               NC1704.2
051800     MOVE     MULT5 TO COMPUTED-N.                                NC1704.2
051900     MOVE    +4 TO CORRECT-N.                                     NC1704.2
052000     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1704.2
052100 MPY-WRITE-F2-3-2.                                                NC1704.2
052200     MOVE "MPY-TEST-F2-3-2 " TO PAR-NAME.                         NC1704.2
052300     PERFORM  PRINT-DETAIL.                                       NC1704.2
052400                                                                  NC1704.2
052500 MPY-INIT-F2-4.                                                   NC1704.2
052600     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
052700     MOVE    80.12 TO MULT1.                                      NC1704.2
052800     MOVE   "A"    TO XRAY.                                       NC1704.2
052900     MOVE    4     TO MULT5.                                      NC1704.2
053000 MPY-TEST-F2-4-0.                                                 NC1704.2
053100     MULTIPLY 3.3 BY -3 GIVING MULT5 ROUNDED ON SIZE ERROR        NC1704.2
053200              MOVE "J" TO XRAY.                                   NC1704.2
053300 MPY-TEST-F2-4-1.                                                 NC1704.2
053400     IF       XRAY NOT EQUAL TO "J"                               NC1704.2
053500              GO TO MPY-FAIL-F2-4-1                               NC1704.2
053600     ELSE                                                         NC1704.2
053700              PERFORM PASS.                                       NC1704.2
053800     GO TO    MPY-WRITE-F2-4-1.                                   NC1704.2
053900 MPY-DELETE-F2-4-1.                                               NC1704.2
054000     PERFORM  DE-LETE.                                            NC1704.2
054100     GO TO    MPY-WRITE-F2-4-1.                                   NC1704.2
054200 MPY-FAIL-F2-4-1.                                                 NC1704.2
054300     MOVE     XRAY  TO COMPUTED-X.                                NC1704.2
054400     MOVE    "J"    TO CORRECT-X.                                 NC1704.2
054500     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1704.2
054600     PERFORM  FAIL.                                               NC1704.2
054700 MPY-WRITE-F2-4-1.                                                NC1704.2
054800     MOVE "MPY-TEST-F2-4-1 " TO PAR-NAME.                         NC1704.2
054900     PERFORM  PRINT-DETAIL.                                       NC1704.2
055000 MPY-TEST-F2-4-2.                                                 NC1704.2
055100     IF       MULT5 EQUAL TO 4                                    NC1704.2
055200              PERFORM PASS                                        NC1704.2
055300     ELSE                                                         NC1704.2
055400              GO TO MPY-FAIL-F2-4-2.                              NC1704.2
055500     GO TO    MPY-WRITE-F2-4-2.                                   NC1704.2
055600 MPY-DELETE-F2-4-2.                                               NC1704.2
055700     PERFORM  DE-LETE.                                            NC1704.2
055800     GO TO    MPY-WRITE-F2-4-2.                                   NC1704.2
055900 MPY-FAIL-F2-4-2.                                                 NC1704.2
056000     PERFORM  FAIL.                                               NC1704.2
056100     MOVE     MULT5 TO COMPUTED-N.                                NC1704.2
056200     MOVE     +4 TO CORRECT-N.                                    NC1704.2
056300     MOVE     4 TO MULT5.                                         NC1704.2
056400     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1704.2
056500 MPY-WRITE-F2-4-2.                                                NC1704.2
056600     MOVE    "MPY-TEST-F2-4-2 " TO PAR-NAME.                      NC1704.2
056700     PERFORM  PRINT-DETAIL.                                       NC1704.2
056800                                                                  NC1704.2
056900 MPY-INIT-F2-5.                                                   NC1704.2
057000     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
057100     MOVE    ZERO TO WRK-DS-09V09.                                NC1704.2
057200 MPY-TEST-F2-5-0.                                                 NC1704.2
057300     MULTIPLY A06THREES-DS-03V03 BY A12THREES-DS-06V06            NC1704.2
057400         GIVING WRK-DS-09V09.                                     NC1704.2
057500 MPY-TEST-F2-5-1.                                                 NC1704.2
057600     IF WRK-DS-18V00-S EQUAL TO 111110999999888889                NC1704.2
057700         PERFORM PASS                                             NC1704.2
057800         GO TO MPY-WRITE-F2-5.                                    NC1704.2
057900     GO TO MPY-FAIL-F2-5.                                         NC1704.2
058000 MPY-DELETE-F2-5.                                                 NC1704.2
058100     PERFORM DE-LETE.                                             NC1704.2
058200     GO TO MPY-WRITE-F2-5.                                        NC1704.2
058300 MPY-FAIL-F2-5.                                                   NC1704.2
058400     MOVE  111110999999888889 TO CORRECT-18V0.                    NC1704.2
058500     MOVE  WRK-DS-18V00-S     TO COMPUTED-18V0.                   NC1704.2
058600     PERFORM FAIL.                                                NC1704.2
058700 MPY-WRITE-F2-5.                                                  NC1704.2
058800     MOVE "MPY-TEST-F2-5         " TO PAR-NAME.                   NC1704.2
058900     PERFORM PRINT-DETAIL.                                        NC1704.2
059000                                                                  NC1704.2
059100 MPY-INIT-F2-6.                                                   NC1704.2
059200     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
059300     MOVE    ZERO TO WRK-DS-10V00.                                NC1704.2
059400 MPY-TEST-F2-6-0.                                                 NC1704.2
059500     MULTIPLY A06THREES-DS-03V03 BY A06THREES-DS-03V03            NC1704.2
059600         GIVING WRK-DS-10V00 ROUNDED.                             NC1704.2
059700 MPY-TEST-F2-6.                                                   NC1704.2
059800     IF WRK-DS-10V00 EQUAL TO 0000111111                          NC1704.2
059900         PERFORM PASS                                             NC1704.2
060000         GO TO MPY-WRITE-F2-6.                                    NC1704.2
060100     GO TO MPY-FAIL-F2-6.                                         NC1704.2
060200 MPY-DELETE-F2-6.                                                 NC1704.2
060300     PERFORM DE-LETE.                                             NC1704.2
060400     GO TO MPY-WRITE-F2-6.                                        NC1704.2
060500 MPY-FAIL-F2-6.                                                   NC1704.2
060600     MOVE  0000111111         TO CORRECT-18V0.                    NC1704.2
060700     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1704.2
060800     PERFORM FAIL.                                                NC1704.2
060900 MPY-WRITE-F2-6.                                                  NC1704.2
061000     MOVE "MPY-TEST-F2-6         " TO PAR-NAME.                   NC1704.2
061100     PERFORM PRINT-DETAIL.                                        NC1704.2
061200                                                                  NC1704.2
061300 MPY-INIT-F2-7.                                                   NC1704.2
061400     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
061500     MOVE   "0"   TO WRK-XN-00001.                                NC1704.2
061600     MOVE    ZERO TO WRK-DS-10V00.                                NC1704.2
061700 MPY-TEST-F2-7-0.                                                 NC1704.2
061800     MULTIPLY A05ONES-DS-00V05 BY A12ONES-DS-12V00                NC1704.2
061900       GIVING WRK-DS-10V00                                        NC1704.2
062000           ON SIZE ERROR                                          NC1704.2
062100              MOVE "1" TO WRK-XN-00001.                           NC1704.2
062200 MPY-TEST-F2-7-1.                                                 NC1704.2
062300     IF WRK-DS-10V00 EQUAL TO 0000000000                          NC1704.2
062400         PERFORM PASS                                             NC1704.2
062500         GO TO MPY-WRITE-F2-7-1.                                  NC1704.2
062600     GO TO MPY-FAIL-F2-7-1.                                       NC1704.2
062700 MPY-DELETE-F2-7-1.                                               NC1704.2
062800     PERFORM DE-LETE.                                             NC1704.2
062900     GO TO MPY-WRITE-F2-7-1.                                      NC1704.2
063000 MPY-FAIL-F2-7-1.                                                 NC1704.2
063100     MOVE  0000000000         TO CORRECT-18V0.                    NC1704.2
063200     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1704.2
063300     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1704.2
063400     PERFORM FAIL.                                                NC1704.2
063500 MPY-WRITE-F2-7-1.                                                NC1704.2
063600     MOVE "MPY-TEST-F2-7-1         " TO PAR-NAME.                 NC1704.2
063700     PERFORM PRINT-DETAIL.                                        NC1704.2
063800 MPY-TEST-F2-7-2.                                                 NC1704.2
063900     IF      WRK-XN-00001 EQUAL TO "1"                            NC1704.2
064000             PERFORM PASS                                         NC1704.2
064100             GO TO MPY-WRITE-F2-7-2.                              NC1704.2
064200     MOVE "1" TO CORRECT-A.                                       NC1704.2
064300     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1704.2
064400     MOVE    "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.             NC1704.2
064500     PERFORM FAIL.                                                NC1704.2
064600     GO TO MPY-WRITE-F2-7-2.                                      NC1704.2
064700 MPY-DELETE-F2-7-2.                                               NC1704.2
064800     PERFORM DE-LETE.                                             NC1704.2
064900 MPY-WRITE-F2-7-2.                                                NC1704.2
065000     MOVE "MPY-TEST-F2-7-2         " TO PAR-NAME.                 NC1704.2
065100     PERFORM PRINT-DETAIL.                                        NC1704.2
065200                                                                  NC1704.2
065300 MPY-INIT-F2-8.                                                   NC1704.2
065400     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
065500     MOVE   "1"   TO WRK-XN-00001.                                NC1704.2
065600     MOVE    ZERO TO WRK-DS-10V00.                                NC1704.2
065700 MPY-TEST-F2-8-0.                                                 NC1704.2
065800     MULTIPLY A01ONE-DS-P0801 BY A12ONES-DS-12V00                 NC1704.2
065900         GIVING WRK-DS-10V00 ON SIZE ERROR                        NC1704.2
066000         MOVE "0" TO WRK-XN-00001.                                NC1704.2
066100 MPY-TEST-F2-8-1.                                                 NC1704.2
066200     IF WRK-DS-10V00 EQUAL TO 0000000111                          NC1704.2
066300         PERFORM PASS                                             NC1704.2
066400         GO TO MPY-WRITE-F2-8-1.                                  NC1704.2
066500     GO TO MPY-FAIL-F2-8-1.                                       NC1704.2
066600 MPY-DELETE-F2-8-1.                                               NC1704.2
066700     PERFORM DE-LETE.                                             NC1704.2
066800     GO TO MPY-WRITE-F2-8-1.                                      NC1704.2
066900 MPY-FAIL-F2-8-1.                                                 NC1704.2
067000     MOVE  0000000111         TO CORRECT-18V0.                    NC1704.2
067100     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1704.2
067200     PERFORM FAIL.                                                NC1704.2
067300 MPY-WRITE-F2-8-1.                                                NC1704.2
067400     MOVE "MPY-TEST-F2-8-1         " TO PAR-NAME.                 NC1704.2
067500     PERFORM PRINT-DETAIL.                                        NC1704.2
067600 MPY-TEST-F2-8-2.                                                 NC1704.2
067700     IF WRK-XN-00001 EQUAL TO "1"                                 NC1704.2
067800         PERFORM PASS                                             NC1704.2
067900         GO TO MPY-WRITE-F2-8-2.                                  NC1704.2
068000     MOVE    "1"           TO CORRECT-A.                          NC1704.2
068100     MOVE     WRK-XN-00001 TO COMPUTED-A.                         NC1704.2
068200     MOVE    "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.   NC1704.2
068300     PERFORM FAIL.                                                NC1704.2
068400     GO TO MPY-WRITE-F2-8-2.                                      NC1704.2
068500 MPY-DELETE-F2-8-2.                                               NC1704.2
068600     PERFORM DE-LETE.                                             NC1704.2
068700 MPY-WRITE-F2-8-2.                                                NC1704.2
068800     MOVE "MPY-TEST-F2-8-2         " TO PAR-NAME.                 NC1704.2
068900     PERFORM PRINT-DETAIL.                                        NC1704.2
069000                                                                  NC1704.2
069100 MPY-INIT-F2-9.                                                   NC1704.2
069200     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
069300     MOVE   "0"   TO WRK-XN-00001.                                NC1704.2
069400     MOVE    ZERO TO WRK-DS-10V00.                                NC1704.2
069500 MPY-TEST-F2-9-0.                                                 NC1704.2
069600     MULTIPLY 9.5 BY A10ONES-DS-10V00                             NC1704.2
069700         GIVING WRK-DS-10V00 ROUNDED ON SIZE ERROR                NC1704.2
069800         MOVE "1" TO WRK-XN-00001.                                NC1704.2
069900 MPY-TEST-F2-9-1.                                                 NC1704.2
070000     IF WRK-DS-10V00 EQUAL TO 0000000000                          NC1704.2
070100         PERFORM PASS                                             NC1704.2
070200         GO TO MPY-WRITE-F2-9-1.                                  NC1704.2
070300     MOVE  0000000000         TO CORRECT-18V0.                    NC1704.2
070400     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1704.2
070500     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1704.2
070600     PERFORM FAIL.                                                NC1704.2
070700     GO TO MPY-WRITE-F2-9-1.                                      NC1704.2
070800 MPY-DELETE-F2-9-1.                                               NC1704.2
070900     PERFORM DE-LETE.                                             NC1704.2
071000 MPY-WRITE-F2-9-1.                                                NC1704.2
071100     MOVE   "MPY-TEST-F2-9-1       " TO PAR-NAME.                 NC1704.2
071200     PERFORM PRINT-DETAIL.                                        NC1704.2
071300 MPY-TEST-F2-9-2.                                                 NC1704.2
071400     IF      WRK-XN-00001 EQUAL TO "1"                            NC1704.2
071500             PERFORM PASS                                         NC1704.2
071600             GO TO MPY-WRITE-F2-9-2.                              NC1704.2
071700 MPY-FAIL-F2-9-2.                                                 NC1704.2
071800     MOVE   "1" TO CORRECT-A.                                     NC1704.2
071900     MOVE    WRK-XN-00001 TO COMPUTED-A.                          NC1704.2
072000     MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.              NC1704.2
072100     PERFORM FAIL.                                                NC1704.2
072200     GO TO   MPY-WRITE-F2-9-2.                                    NC1704.2
072300 MPY-DELETE-F2-9-2.                                               NC1704.2
072400     PERFORM DE-LETE.                                             NC1704.2
072500 MPY-WRITE-F2-9-2.                                                NC1704.2
072600     MOVE   "MPY-TEST-F2-9-2       " TO PAR-NAME.                 NC1704.2
072700     PERFORM PRINT-DETAIL.                                        NC1704.2
072800                                                                  NC1704.2
072900 MPY-INIT-F2-10-1.                                                NC1704.2
073000     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
073100     MOVE   "1"  TO WRK-XN-00001.                                 NC1704.2
073200     MOVE   ZERO TO WRK-DS-09V08.                                 NC1704.2
073300 MPY-TEST-F2-10-0.                                                NC1704.2
073400     MULTIPLY A01ONE-DS-P0801 BY A18ONES-DS-18V00                 NC1704.2
073500         GIVING WRK-DS-09V08 ROUNDED ON SIZE ERROR                NC1704.2
073600         MOVE "0" TO WRK-XN-00001.                                NC1704.2
073700 MPY-TEST-F2-10-1.                                                NC1704.2
073800     IF WRK-DS-17V00-S EQUAL TO 11111111111111111                 NC1704.2
073900         PERFORM PASS                                             NC1704.2
074000         GO TO MPY-WRITE-F2-10-1.                                 NC1704.2
074100     MOVE  11111111111111111  TO CORRECT-18V0.                    NC1704.2
074200     MOVE  WRK-DS-17V00-S     TO COMPUTED-18V0.                   NC1704.2
074300     PERFORM FAIL.                                                NC1704.2
074400     GO TO MPY-WRITE-F2-10-1.                                     NC1704.2
074500 MPY-DELETE-F2-10-1.                                              NC1704.2
074600     PERFORM DE-LETE.                                             NC1704.2
074700 MPY-WRITE-F2-10-1.                                               NC1704.2
074800     MOVE   "MPY-TEST-F2-10-1       " TO PAR-NAME.                NC1704.2
074900     PERFORM PRINT-DETAIL.                                        NC1704.2
075000 MPY-TEST-F2-10-2.                                                NC1704.2
075100     IF      WRK-XN-00001 EQUAL TO "1"                            NC1704.2
075200             PERFORM PASS                                         NC1704.2
075300             GO TO MPY-WRITE-F2-10-2.                             NC1704.2
075400     MOVE   "1" TO CORRECT-A.                                     NC1704.2
075500     MOVE    WRK-XN-00001 TO COMPUTED-A.                          NC1704.2
075600     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.    NC1704.2
075700     PERFORM FAIL.                                                NC1704.2
075800     GO TO   MPY-WRITE-F2-10-2.                                   NC1704.2
075900 MPY-DELETE-F2-10-2.                                              NC1704.2
076000     PERFORM DE-LETE.                                             NC1704.2
076100 MPY-WRITE-F2-10-2.                                               NC1704.2
076200     MOVE   "MPY-TEST-F2-10-2       " TO PAR-NAME.                NC1704.2
076300     PERFORM PRINT-DETAIL.                                        NC1704.2
076400                                                                  NC1704.2
076500 MPY-INIT-F2-11.                                                  NC1704.2
076600     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
076700     MOVE    ZERO TO WRK-CS-02V02.                                NC1704.2
076800 MPY-TEST-F2-11-0.                                                NC1704.2
076900     MULTIPLY A99-CS-02V00 BY A01ONE-CS-00V01 GIVING WRK-CS-02V02.NC1704.2
077000 MPY-TEST-F2-11-1.                                                NC1704.2
077100     MOVE    WRK-CS-02V02 TO WRK-DS-06V06.                        NC1704.2
077200     IF      WRK-DS-12V00-S EQUAL TO 000009900000                 NC1704.2
077300             PERFORM PASS                                         NC1704.2
077400             GO TO   MPY-WRITE-F2-11.                             NC1704.2
077500     MOVE    000009900000     TO CORRECT-18V0.                    NC1704.2
077600     MOVE    WRK-DS-12V00-S   TO COMPUTED-18V0.                   NC1704.2
077700     PERFORM FAIL.                                                NC1704.2
077800     GO TO   MPY-WRITE-F2-11.                                     NC1704.2
077900 MPY-DELETE-F2-11.                                                NC1704.2
078000     PERFORM DE-LETE.                                             NC1704.2
078100 MPY-WRITE-F2-11.                                                 NC1704.2
078200     MOVE   "MPY-TEST-F2-11       " TO PAR-NAME.                  NC1704.2
078300     PERFORM PRINT-DETAIL.                                        NC1704.2
078400                                                                  NC1704.2
078500 MPY-INIT-F2-12.                                                  NC1704.2
078600     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
078700     MOVE    ZERO TO WRK-CS-18V00.                                NC1704.2
078800 MPY-TEST-F2-12-0.                                                NC1704.2
078900     MULTIPLY A01ONES-CS-18V00 BY A02THREES-CS-18V00              NC1704.2
079000             GIVING WRK-CS-18V00.                                 NC1704.2
079100 MPY-TEST-F2-12-1.                                                NC1704.2
079200     IF WRK-CS-18V00 EQUAL TO -000000000000000033                 NC1704.2
079300         PERFORM PASS                                             NC1704.2
079400         GO TO MPY-WRITE-F2-12.                                   NC1704.2
079500     MOVE -000000000000000033 TO CORRECT-18V0.                    NC1704.2
079600     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1704.2
079700     PERFORM FAIL.                                                NC1704.2
079800     GO TO MPY-WRITE-F2-12.                                       NC1704.2
079900 MPY-DELETE-F2-12.                                                NC1704.2
080000     PERFORM DE-LETE.                                             NC1704.2
080100 MPY-WRITE-F2-12.                                                 NC1704.2
080200     MOVE "MPY-TEST-F2-12 " TO PAR-NAME.                          NC1704.2
080300     PERFORM PRINT-DETAIL.                                        NC1704.2
080400                                                                  NC1704.2
080500 MPY-INIT-F2-13.                                                  NC1704.2
080600     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
080700     MOVE    ZERO TO WRK-DU-18V00.                                NC1704.2
080800 MPY-TEST-F2-13-0.                                                NC1704.2
080900     MULTIPLY A02THREES-CS-18V00 BY A14TWOS-CU-18V00              NC1704.2
081000             GIVING WRK-DU-18V00.                                 NC1704.2
081100 MPY-TEST-F2-13.                                                  NC1704.2
081200     IF WRK-DU-18V00 EQUAL TO 000733333333333326                  NC1704.2
081300         PERFORM PASS                                             NC1704.2
081400         GO TO MPY-WRITE-F2-13.                                   NC1704.2
081500     MOVE 000733333333333326 TO CORRECT-18V0.                     NC1704.2
081600     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1704.2
081700     PERFORM FAIL.                                                NC1704.2
081800     GO TO MPY-WRITE-F2-13.                                       NC1704.2
081900 MPY-DELETE-F2-13.                                                NC1704.2
082000     PERFORM DE-LETE.                                             NC1704.2
082100 MPY-WRITE-F2-13.                                                 NC1704.2
082200     MOVE "MPY-TEST-F2-13 " TO PAR-NAME.                          NC1704.2
082300     PERFORM PRINT-DETAIL.                                        NC1704.2
082400                                                                  NC1704.2
082500 MPY-INIT-F2-14.                                                  NC1704.2
082600     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
082700     MOVE    ZERO TO WRK-CS-18V00.                                NC1704.2
082800 MPY-TEST-F2-14-0.                                                NC1704.2
082900     MULTIPLY A02THREES-CS-18V00 BY A16NINES-CU-18V00             NC1704.2
083000             GIVING WRK-CS-18V00.                                 NC1704.2
083100 MPY-TEST-F2-14.                                                  NC1704.2
083200     IF WRK-CS-18V00 EQUAL TO -329999999999999967                 NC1704.2
083300         PERFORM PASS                                             NC1704.2
083400         GO TO MPY-WRITE-F2-14.                                   NC1704.2
083500     MOVE -329999999999999967 TO CORRECT-18V0.                    NC1704.2
083600     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1704.2
083700     PERFORM FAIL.                                                NC1704.2
083800     GO TO MPY-WRITE-F2-14.                                       NC1704.2
083900 MPY-DELETE-F2-14.                                                NC1704.2
084000     PERFORM DE-LETE.                                             NC1704.2
084100 MPY-WRITE-F2-14.                                                 NC1704.2
084200     MOVE "MPY-TEST-F2-14 " TO PAR-NAME.                          NC1704.2
084300     PERFORM PRINT-DETAIL.                                        NC1704.2
084400                                                                  NC1704.2
084500 MPY-INIT-F2-15.                                                  NC1704.2
084600     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
084700     MOVE    ZERO TO WRK-DU-18V00.                                NC1704.2
084800 MPY-TEST-F2-15-0.                                                NC1704.2
084900     MULTIPLY A01ONES-CS-18V00 BY A18SIXES-CU-18V00               NC1704.2
085000         GIVING WRK-DU-18V00.                                     NC1704.2
085100 MPY-TEST-F2-15.                                                  NC1704.2
085200     IF WRK-DU-18V00 EQUAL TO 666666666666666666                  NC1704.2
085300         PERFORM PASS                                             NC1704.2
085400         GO TO MPY-WRITE-F2-15.                                   NC1704.2
085500     MOVE 666666666666666666 TO CORRECT-18V0.                     NC1704.2
085600     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1704.2
085700     PERFORM FAIL.                                                NC1704.2
085800     GO TO MPY-WRITE-F2-15.                                       NC1704.2
085900 MPY-DELETE-F2-15.                                                NC1704.2
086000     PERFORM DE-LETE.                                             NC1704.2
086100 MPY-WRITE-F2-15.                                                 NC1704.2
086200     MOVE "MPY-TEST-F2-15 " TO PAR-NAME.                          NC1704.2
086300     PERFORM PRINT-DETAIL.                                        NC1704.2
086400*                                                                 NC1704.2
086500*                                                                 NC1704.2
086600 MPY-INIT-F2-16.                                                  NC1704.2
086700*    ==-->  NEW SIZE ERROR TESTS    <--==                         NC1704.2
086800     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
086900     MOVE   "H"  TO XRAY.                                         NC1704.2
087000     MOVE  80.12 TO MULT1.                                        NC1704.2
087100     MOVE   -56  TO MULT4.                                        NC1704.2
087200     MOVE    0   TO MULT5.                                        NC1704.2
087300     MOVE    1   TO REC-CT.                                       NC1704.2
087400 MPY-TEST-F2-16-0.                                                NC1704.2
087500     MULTIPLY MULT4 BY MULT1 GIVING MULT5                         NC1704.2
087600          NOT ON SIZE ERROR  MOVE "X" TO XRAY.                    NC1704.2
087700 MPY-TEST-F2-16-1.                                                NC1704.2
087800     GO TO MPY-TEST-F2-16-2.                                      NC1704.2
087900 MPY-DELETE-F2-16-1.                                              NC1704.2
088000     PERFORM DE-LETE.                                             NC1704.2
088100     PERFORM PRINT-DETAIL.                                        NC1704.2
088200     GO TO   MPY-INIT-F2-17.                                      NC1704.2
088300 MPY-TEST-F2-16-2.                                                NC1704.2
088400     MOVE   "MPY-TEST-F2-16-2 " TO PAR-NAME.                      NC1704.2
088500     IF      XRAY    = "H"                                        NC1704.2
088600             PERFORM PASS                                         NC1704.2
088700             PERFORM PRINT-DETAIL                                 NC1704.2
088800     ELSE                                                         NC1704.2
088900             MOVE    XRAY  TO COMPUTED-X                          NC1704.2
089000             MOVE   "H"    TO CORRECT-X                           NC1704.2
089100             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
089200             PERFORM FAIL                                         NC1704.2
089300             PERFORM PRINT-DETAIL.                                NC1704.2
089400     ADD     1 TO REC-CT.                                         NC1704.2
089500 MPY-TEST-F2-16-3.                                                NC1704.2
089600     MOVE   "MPY-TEST-F2-16-3 " TO PAR-NAME.                      NC1704.2
089700     IF      MULT5 = 0                                            NC1704.2
089800             PERFORM PASS                                         NC1704.2
089900             PERFORM PRINT-DETAIL                                 NC1704.2
090000     ELSE                                                         NC1704.2
090100             MOVE    MULT5 TO COMPUTED-N                          NC1704.2
090200             MOVE    0     TO CORRECT-N                           NC1704.2
090300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
090400             PERFORM FAIL                                         NC1704.2
090500             PERFORM PRINT-DETAIL.                                NC1704.2
090600*                                                                 NC1704.2
090700*                                                                 NC1704.2
090800 MPY-INIT-F2-17.                                                  NC1704.2
090900*    ==-->  NEW SIZE ERROR TESTS    <--==                         NC1704.2
091000     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
091100     MOVE   "1" TO WRK-XN-00001.                                  NC1704.2
091200     MOVE    ZERO TO WRK-DS-10V00.                                NC1704.2
091300     MOVE    1   TO REC-CT.                                       NC1704.2
091400 MPY-TEST-F2-17-0.                                                NC1704.2
091500     MULTIPLY A01ONE-DS-P0801 BY A12ONES-DS-12V00                 NC1704.2
091600             GIVING WRK-DS-10V00                                  NC1704.2
091700             NOT ON SIZE ERROR                                    NC1704.2
091800               MOVE "0" TO WRK-XN-00001.                          NC1704.2
091900 MPY-TEST-F2-17-1.                                                NC1704.2
092000     GO TO   MPY-TEST-F2-17-2.                                    NC1704.2
092100 MPY-DELETE-F2-17.                                                NC1704.2
092200     PERFORM DE-LETE.                                             NC1704.2
092300     PERFORM PRINT-DETAIL.                                        NC1704.2
092400     GO TO   MPY-INIT-F2-18.                                      NC1704.2
092500 MPY-TEST-F2-17-2.                                                NC1704.2
092600     MOVE   "MPY-TEST-F2-17-2 " TO PAR-NAME.                      NC1704.2
092700     IF      WRK-XN-00001 = "0"                                   NC1704.2
092800             PERFORM PASS                                         NC1704.2
092900             PERFORM PRINT-DETAIL                                 NC1704.2
093000     ELSE                                                         NC1704.2
093100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1704.2
093200             MOVE   "0"           TO CORRECT-X                    NC1704.2
093300             MOVE   "NOT ON SIZE ERROR SHOULD HAVE BEEN EXECUTED" NC1704.2
093400                  TO RE-MARK                                      NC1704.2
093500             PERFORM FAIL                                         NC1704.2
093600             PERFORM PRINT-DETAIL.                                NC1704.2
093700     ADD     1   TO REC-CT.                                       NC1704.2
093800 MPY-TEST-F2-17-3.                                                NC1704.2
093900     MOVE   "MPY-TEST-F2-17-3 " TO PAR-NAME.                      NC1704.2
094000     IF      WRK-DS-10V00 = 0000000111                            NC1704.2
094100             PERFORM PASS                                         NC1704.2
094200             PERFORM PRINT-DETAIL                                 NC1704.2
094300     ELSE                                                         NC1704.2
094400             MOVE    0000000111       TO CORRECT-18V0             NC1704.2
094500             MOVE    WRK-DS-10V00     TO COMPUTED-18V0            NC1704.2
094600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1704.2
094700             PERFORM FAIL                                         NC1704.2
094800             PERFORM PRINT-DETAIL.                                NC1704.2
094900*                                                                 NC1704.2
095000*                                                                 NC1704.2
095100 MPY-INIT-F2-18.                                                  NC1704.2
095200*    ==-->  NEW SIZE ERROR TESTS    <--==                         NC1704.2
095300     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
095400     MOVE    1   TO REC-CT.                                       NC1704.2
095500     MOVE  80.12 TO MULT1.                                        NC1704.2
095600     MOVE   -56  TO MULT4.                                        NC1704.2
095700     MOVE    0   TO MULT5.                                        NC1704.2
095800     MOVE   "H" TO XRAY.                                          NC1704.2
095900 MPY-TEST-F2-18-0.                                                NC1704.2
096000     MULTIPLY MULT4 BY MULT1 GIVING MULT5                         NC1704.2
096100              ON SIZE ERROR  MOVE "A" TO XRAY                     NC1704.2
096200          NOT ON SIZE ERROR  MOVE "B" TO XRAY.                    NC1704.2
096300 MPY-TEST-F2-18-1.                                                NC1704.2
096400     GO TO MPY-TEST-F2-18-2.                                      NC1704.2
096500 MPY-DELETE-F2-18.                                                NC1704.2
096600     PERFORM DE-LETE.                                             NC1704.2
096700     PERFORM PRINT-DETAIL.                                        NC1704.2
096800     GO TO   MPY-INIT-F2-19.                                      NC1704.2
096900 MPY-TEST-F2-18-2.                                                NC1704.2
097000     MOVE   "MPY-TEST-F2-18-2 " TO PAR-NAME.                      NC1704.2
097100     IF      XRAY    = "A"                                        NC1704.2
097200             PERFORM PASS                                         NC1704.2
097300             PERFORM PRINT-DETAIL                                 NC1704.2
097400     ELSE                                                         NC1704.2
097500             MOVE    XRAY  TO COMPUTED-X                          NC1704.2
097600             MOVE   "A"    TO CORRECT-X                           NC1704.2
097700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
097800             PERFORM FAIL                                         NC1704.2
097900             PERFORM PRINT-DETAIL.                                NC1704.2
098000     ADD     1   TO REC-CT.                                       NC1704.2
098100 MPY-TEST-F2-18-3.                                                NC1704.2
098200     MOVE   "MPY-TEST-F2-18-3 " TO PAR-NAME.                      NC1704.2
098300     IF      MULT5   = 0                                          NC1704.2
098400             PERFORM PASS                                         NC1704.2
098500             PERFORM PRINT-DETAIL                                 NC1704.2
098600     ELSE                                                         NC1704.2
098700             MOVE    MULT5 TO COMPUTED-N                          NC1704.2
098800             MOVE    0     TO CORRECT-N                           NC1704.2
098900             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
099000             PERFORM FAIL                                         NC1704.2
099100             PERFORM PRINT-DETAIL.                                NC1704.2
099200*                                                                 NC1704.2
099300*                                                                 NC1704.2
099400 MPY-INIT-F2-19.                                                  NC1704.2
099500*    ==-->  NEW SIZE ERROR TESTS    <--==                         NC1704.2
099600     MOVE    1   TO REC-CT.                                       NC1704.2
099700     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
099800     MOVE   "0" TO WRK-XN-00001.                                  NC1704.2
099900     MOVE    ZERO TO WRK-DS-10V00.                                NC1704.2
100000 MPY-TEST-F2-19-0.                                                NC1704.2
100100     MULTIPLY A01ONE-DS-P0801 BY A12ONES-DS-12V00                 NC1704.2
100200             GIVING WRK-DS-10V00                                  NC1704.2
100300                 ON SIZE ERROR MOVE "1" TO WRK-XN-00001           NC1704.2
100400             NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.          NC1704.2
100500 MPY-TEST-F2-19-1.                                                NC1704.2
100600     GO TO   MPY-TEST-F2-19-2.                                    NC1704.2
100700 MPY-DELETE-F2-19.                                                NC1704.2
100800     PERFORM DE-LETE.                                             NC1704.2
100900     PERFORM PRINT-DETAIL.                                        NC1704.2
101000     GO TO   MPY-INIT-F2-20.                                      NC1704.2
101100 MPY-TEST-F2-19-2.                                                NC1704.2
101200     MOVE   "MPY-TEST-F2-19-2" TO PAR-NAME.                       NC1704.2
101300     IF      WRK-XN-00001 = "2"                                   NC1704.2
101400             PERFORM PASS                                         NC1704.2
101500             PERFORM PRINT-DETAIL                                 NC1704.2
101600     ELSE                                                         NC1704.2
101700             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1704.2
101800             MOVE   "2"          TO CORRECT-X                     NC1704.2
101900             MOVE   "NOT ON SIZE ERROR SHOULD HAVE BEEN EXECUTED" NC1704.2
102000                  TO RE-MARK                                      NC1704.2
102100             PERFORM FAIL                                         NC1704.2
102200             PERFORM PRINT-DETAIL.                                NC1704.2
102300     ADD     1   TO REC-CT.                                       NC1704.2
102400 MPY-TEST-F2-19-3.                                                NC1704.2
102500     MOVE   "MPY-TEST-F2-19-3 " TO PAR-NAME.                      NC1704.2
102600     IF      WRK-DS-10V00 = 0000000111                            NC1704.2
102700             PERFORM PASS                                         NC1704.2
102800             PERFORM PRINT-DETAIL                                 NC1704.2
102900     ELSE                                                         NC1704.2
103000             MOVE    0000000111       TO CORRECT-18V0             NC1704.2
103100             MOVE    WRK-DS-10V00     TO COMPUTED-18V0            NC1704.2
103200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1704.2
103300             PERFORM FAIL                                         NC1704.2
103400             PERFORM PRINT-DETAIL.                                NC1704.2
103500*                                                                 NC1704.2
103600*                                                                 NC1704.2
103700 MPY-INIT-F2-20.                                                  NC1704.2
103800*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
103900     MOVE   "V1-107 6.19.4 GR2" TO ANSI-REFERENCE.                NC1704.2
104000     MOVE   "MPY-TEST-F2-20" TO PAR-NAME.                         NC1704.2
104100     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
104200     MOVE    .00001 TO WRK-DU-4P1-1.                              NC1704.2
104300     MOVE    0      TO WRK-DU-2V0-1.                              NC1704.2
104400     MOVE    0      TO WRK-DU-2V0-2.                              NC1704.2
104500     MOVE    0      TO WRK-DU-2V5-1.                              NC1704.2
104600     MOVE    0      TO WRK-NE-2.                                  NC1704.2
104700     MOVE    0      TO WRK-NE-3.                                  NC1704.2
104800     MOVE    1 TO REC-CT.                                         NC1704.2
104900 MPY-TEST-F2-20-0.                                                NC1704.2
105000     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-6V0-1 GIVING WRK-DU-2V0-1    NC1704.2
105100         WRK-DU-2V0-2 ROUNDED WRK-DU-2V5-1 WRK-NE-2 ROUNDED       NC1704.2
105200         WRK-NE-3.                                                NC1704.2
105300     GO TO MPY-TEST-F2-20-1.                                      NC1704.2
105400 MPY-DELETE-F2-20.                                                NC1704.2
105500     PERFORM DE-LETE.                                             NC1704.2
105600     GO TO MPY-INIT-F2-21.                                        NC1704.2
105700 MPY-TEST-F2-20-1.                                                NC1704.2
105800     IF WRK-DU-2V0-1  = 9 PERFORM PASS PERFORM PRINT-DETAIL       NC1704.2
105900     ELSE                                                         NC1704.2
106000     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 9 TO       NC1704.2
106100     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
106200     ADD 1 TO REC-CT.                                             NC1704.2
106300 MPY-TEST-F2-20-2.                                                NC1704.2
106400     IF WRK-DU-2V0-2 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1704.2
106500     ELSE                                                         NC1704.2
106600     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 10         NC1704.2
106700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1704.2
106800     ADD 1 TO REC-CT.                                             NC1704.2
106900 MPY-TEST-F2-20-3.                                                NC1704.2
107000     IF WRK-DU-2V5-1 = 9.99999 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
107100     ELSE                                                         NC1704.2
107200     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 9.99999 TO NC1704.2
107300     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
107400     ADD 1 TO REC-CT.                                             NC1704.2
107500 MPY-TEST-F2-20-4.                                                NC1704.2
107600     IF WRK-NE-2 = "$10.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
107700     ELSE                                                         NC1704.2
107800     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$10.00"       NC1704.2
107900     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
108000     ADD 1 TO REC-CT.                                             NC1704.2
108100 MPY-TEST-F2-20-5.                                                NC1704.2
108200     IF WRK-NE-3 = "$09.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
108300     ELSE                                                         NC1704.2
108400     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$09.99  "     NC1704.2
108500     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
108600*                                                                 NC1704.2
108700*                                                                 NC1704.2
108800 MPY-INIT-F2-21.                                                  NC1704.2
108900*    ==-->    SIZE ERROR CONDITION  <--==                         NC1704.2
109000*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
109100     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
109200     MOVE   "MPY-TEST-F2-21" TO PAR-NAME.                         NC1704.2
109300     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
109400     MOVE    .00001 TO WRK-DU-4P1-1.                              NC1704.2
109500     MOVE    0      TO WRK-DU-2V0-1.                              NC1704.2
109600     MOVE    0      TO WRK-DU-2V0-2.                              NC1704.2
109700     MOVE    0      TO WRK-DU-2V5-1.                              NC1704.2
109800     MOVE    0      TO WRK-NE-2.                                  NC1704.2
109900     MOVE    0      TO WRK-NE-3.                                  NC1704.2
110000     MOVE    1      TO REC-CT.                                    NC1704.2
110100     MOVE   "0"     TO WRK-XN-00001.                              NC1704.2
110200 MPY-TEST-F2-21-0.                                                NC1704.2
110300     MULTIPLY WRK-DU-6V0-1 BY WRK-DU-6V0-1                        NC1704.2
110400       GIVING WRK-DU-2V0-1                                        NC1704.2
110500              WRK-DU-2V0-2 ROUNDED                                NC1704.2
110600              WRK-DU-2V5-1                                        NC1704.2
110700              WRK-NE-2 ROUNDED                                    NC1704.2
110800              WRK-NE-3                                            NC1704.2
110900           ON SIZE ERROR MOVE "1" TO WRK-XN-00001.                NC1704.2
111000     GO TO   MPY-TEST-F2-21-1.                                    NC1704.2
111100 MPY-DELETE-F2-21.                                                NC1704.2
111200     PERFORM DE-LETE.                                             NC1704.2
111300     GO TO   MPY-INIT-F2-22.                                      NC1704.2
111400 MPY-TEST-F2-21-1.                                                NC1704.2
111500     IF      WRK-DU-2V0-1  = 0                                    NC1704.2
111600             PERFORM PASS                                         NC1704.2
111700             PERFORM PRINT-DETAIL                                 NC1704.2
111800     ELSE                                                         NC1704.2
111900             PERFORM FAIL                                         NC1704.2
112000             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1704.2
112100             MOVE    0 TO CORRECT-N                               NC1704.2
112200             PERFORM PRINT-DETAIL.                                NC1704.2
112300     ADD 1 TO REC-CT.                                             NC1704.2
112400 MPY-TEST-F2-21-2.                                                NC1704.2
112500     IF      WRK-DU-2V0-2 = 0                                     NC1704.2
112600             PERFORM PASS                                         NC1704.2
112700             PERFORM PRINT-DETAIL                                 NC1704.2
112800     ELSE                                                         NC1704.2
112900             PERFORM FAIL                                         NC1704.2
113000             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1704.2
113100             MOVE    0   TO CORRECT-N                             NC1704.2
113200             PERFORM PRINT-DETAIL.                                NC1704.2
113300     ADD 1 TO REC-CT.                                             NC1704.2
113400 MPY-TEST-F2-21-3.                                                NC1704.2
113500     IF WRK-DU-2V5-1 = 0.00000 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
113600     ELSE                                                         NC1704.2
113700     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 0.00000 TO NC1704.2
113800     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
113900     ADD 1 TO REC-CT.                                             NC1704.2
114000 MPY-TEST-F2-21-4.                                                NC1704.2
114100     IF WRK-NE-2 = "$**.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
114200     ELSE                                                         NC1704.2
114300     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$**.00"       NC1704.2
114400     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
114500     ADD 1 TO REC-CT.                                             NC1704.2
114600 MPY-TEST-F2-21-5.                                                NC1704.2
114700     IF WRK-NE-3 = "$00.00  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
114800     ELSE                                                         NC1704.2
114900     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$00.00  "     NC1704.2
115000     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
115100     ADD 1 TO REC-CT.                                             NC1704.2
115200 MPY-TEST-F2-21-6.                                                NC1704.2
115300     IF      WRK-XN-00001 = "1"                                   NC1704.2
115400             PERFORM PASS                                         NC1704.2
115500             PERFORM PRINT-DETAIL                                 NC1704.2
115600     ELSE                                                         NC1704.2
115700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
115800             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1704.2
115900             MOVE   "1"            TO CORRECT-X                   NC1704.2
116000             PERFORM FAIL                                         NC1704.2
116100             PERFORM PRINT-DETAIL.                                NC1704.2
116200*                                                                 NC1704.2
116300 MPY-INIT-F2-22.                                                  NC1704.2
116400*    ==--> NO SIZE ERROR CONDITION  <--==                         NC1704.2
116500*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
116600     MOVE   "MPY-TEST-F2-22" TO PAR-NAME.                         NC1704.2
116700     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
116800     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
116900     MOVE    .00001 TO WRK-DU-4P1-1.                              NC1704.2
117000     MOVE    0      TO WRK-DU-2V0-1.                              NC1704.2
117100     MOVE    0      TO WRK-DU-2V0-2.                              NC1704.2
117200     MOVE    0      TO WRK-DU-2V5-1.                              NC1704.2
117300     MOVE    0      TO WRK-NE-2.                                  NC1704.2
117400     MOVE    0      TO WRK-NE-3.                                  NC1704.2
117500     MOVE    1      TO REC-CT.                                    NC1704.2
117600     MOVE   "0"     TO WRK-XN-00001.                              NC1704.2
117700 MPY-TEST-F2-22-0.                                                NC1704.2
117800     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-6V0-1                        NC1704.2
117900       GIVING WRK-DU-2V0-1                                        NC1704.2
118000              WRK-DU-2V0-2 ROUNDED                                NC1704.2
118100              WRK-DU-2V5-1                                        NC1704.2
118200              WRK-NE-2 ROUNDED                                    NC1704.2
118300              WRK-NE-3                                            NC1704.2
118400              ON SIZE ERROR MOVE "1" TO WRK-XN-00001.             NC1704.2
118500     GO TO   MPY-TEST-F2-22-1.                                    NC1704.2
118600 MPY-DELETE-F2-22.                                                NC1704.2
118700     PERFORM DE-LETE.                                             NC1704.2
118800     PERFORM PRINT-DETAIL.                                        NC1704.2
118900     GO TO   MPY-INIT-F2-23.                                      NC1704.2
119000 MPY-TEST-F2-22-1.                                                NC1704.2
119100     IF      WRK-DU-2V0-1  = 9                                    NC1704.2
119200             PERFORM PASS                                         NC1704.2
119300             PERFORM PRINT-DETAIL                                 NC1704.2
119400     ELSE                                                         NC1704.2
119500             PERFORM FAIL                                         NC1704.2
119600             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1704.2
119700             MOVE    9 TO CORRECT-N                               NC1704.2
119800             PERFORM PRINT-DETAIL.                                NC1704.2
119900     ADD 1 TO REC-CT.                                             NC1704.2
120000 MPY-TEST-F2-22-2.                                                NC1704.2
120100     IF      WRK-DU-2V0-2 = 10                                    NC1704.2
120200             PERFORM PASS                                         NC1704.2
120300             PERFORM PRINT-DETAIL                                 NC1704.2
120400     ELSE                                                         NC1704.2
120500             PERFORM FAIL                                         NC1704.2
120600             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1704.2
120700             MOVE    10  TO CORRECT-N                             NC1704.2
120800             PERFORM PRINT-DETAIL.                                NC1704.2
120900     ADD 1 TO REC-CT.                                             NC1704.2
121000 MPY-TEST-F2-22-3.                                                NC1704.2
121100     IF WRK-DU-2V5-1 = 9.99999 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
121200     ELSE                                                         NC1704.2
121300     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 9.99999 TO NC1704.2
121400     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
121500     ADD 1 TO REC-CT.                                             NC1704.2
121600 MPY-TEST-F2-22-4.                                                NC1704.2
121700     IF WRK-NE-2 = "$10.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
121800     ELSE                                                         NC1704.2
121900     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$10.00"       NC1704.2
122000     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
122100     ADD 1 TO REC-CT.                                             NC1704.2
122200 MPY-TEST-F2-22-5.                                                NC1704.2
122300     IF WRK-NE-3 = "$09.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
122400     ELSE                                                         NC1704.2
122500     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$09.99  "     NC1704.2
122600     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
122700     ADD 1 TO REC-CT.                                             NC1704.2
122800 MPY-TEST-F2-22-6.                                                NC1704.2
122900     IF      WRK-XN-00001 = "0"                                   NC1704.2
123000             PERFORM PASS                                         NC1704.2
123100             PERFORM PRINT-DETAIL                                 NC1704.2
123200     ELSE                                                         NC1704.2
123300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1704.2
123400             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1704.2
123500             MOVE   "0"            TO CORRECT-X                   NC1704.2
123600             PERFORM FAIL                                         NC1704.2
123700             PERFORM PRINT-DETAIL.                                NC1704.2
123800*                                                                 NC1704.2
123900 MPY-INIT-F2-23.                                                  NC1704.2
124000*    ==-->    SIZE ERROR CONDITION  <--==                         NC1704.2
124100*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
124200     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
124300     MOVE   "MPY-TEST-F2-23" TO PAR-NAME.                         NC1704.2
124400     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
124500     MOVE    1 TO REC-CT.                                         NC1704.2
124600     MOVE    0 TO WRK-DU-2V0-1.                                   NC1704.2
124700     MOVE    0 TO WRK-DU-2V0-2.                                   NC1704.2
124800     MOVE    0 TO WRK-DU-2V5-1.                                   NC1704.2
124900     MOVE    0 TO WRK-NE-2.                                       NC1704.2
125000     MOVE    0 TO WRK-NE-3.                                       NC1704.2
125100     MOVE   "0" TO WRK-XN-00001.                                  NC1704.2
125200 MPY-TEST-F2-23-0.                                                NC1704.2
125300     MULTIPLY WRK-DU-6V0-1 BY WRK-DU-6V0-1                        NC1704.2
125400       GIVING WRK-DU-2V0-1                                        NC1704.2
125500              WRK-DU-2V0-2 ROUNDED                                NC1704.2
125600              WRK-DU-2V5-1                                        NC1704.2
125700              WRK-NE-2 ROUNDED                                    NC1704.2
125800              WRK-NE-3                                            NC1704.2
125900          NOT ON SIZE ERROR MOVE "1" TO WRK-XN-00001.             NC1704.2
126000     GO TO   MPY-TEST-F2-23-1.                                    NC1704.2
126100 MPY-DELETE-F2-23.                                                NC1704.2
126200     PERFORM DE-LETE.                                             NC1704.2
126300     PERFORM PRINT-DETAIL.                                        NC1704.2
126400     GO TO   MPY-INIT-F2-24.                                      NC1704.2
126500 MPY-TEST-F2-23-1.                                                NC1704.2
126600     IF      WRK-DU-2V0-1  = 0                                    NC1704.2
126700             PERFORM PASS                                         NC1704.2
126800             PERFORM PRINT-DETAIL                                 NC1704.2
126900     ELSE                                                         NC1704.2
127000             PERFORM FAIL                                         NC1704.2
127100             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1704.2
127200             MOVE    0 TO CORRECT-N                               NC1704.2
127300             PERFORM PRINT-DETAIL.                                NC1704.2
127400     ADD 1 TO REC-CT.                                             NC1704.2
127500 MPY-TEST-F2-23-2.                                                NC1704.2
127600     IF      WRK-DU-2V0-2 = 0                                     NC1704.2
127700             PERFORM PASS                                         NC1704.2
127800             PERFORM PRINT-DETAIL                                 NC1704.2
127900     ELSE                                                         NC1704.2
128000             PERFORM FAIL                                         NC1704.2
128100             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1704.2
128200             MOVE    0   TO CORRECT-N                             NC1704.2
128300             PERFORM PRINT-DETAIL.                                NC1704.2
128400     ADD 1 TO REC-CT.                                             NC1704.2
128500 MPY-TEST-F2-23-3.                                                NC1704.2
128600     IF WRK-DU-2V5-1 = 0.00000 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
128700     ELSE                                                         NC1704.2
128800     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 0.00000 TO NC1704.2
128900     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
129000     ADD 1 TO REC-CT.                                             NC1704.2
129100 MPY-TEST-F2-23-4.                                                NC1704.2
129200     IF WRK-NE-2 = "$**.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
129300     ELSE                                                         NC1704.2
129400     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$**.00"       NC1704.2
129500     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
129600     ADD 1 TO REC-CT.                                             NC1704.2
129700 MPY-TEST-F2-23-5.                                                NC1704.2
129800     IF WRK-NE-3 = "$00.00  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
129900     ELSE                                                         NC1704.2
130000     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$00.00  "     NC1704.2
130100     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
130200     ADD 1 TO REC-CT.                                             NC1704.2
130300 MPY-TEST-F2-23-6.                                                NC1704.2
130400     IF      WRK-XN-00001 = "0"                                   NC1704.2
130500             PERFORM PASS                                         NC1704.2
130600             PERFORM PRINT-DETAIL                                 NC1704.2
130700     ELSE                                                         NC1704.2
130800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
130900             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1704.2
131000             MOVE   "0"            TO CORRECT-X                   NC1704.2
131100             PERFORM FAIL                                         NC1704.2
131200             PERFORM PRINT-DETAIL.                                NC1704.2
131300*                                                                 NC1704.2
131400 MPY-INIT-F2-24.                                                  NC1704.2
131500*    ==--> NO SIZE ERROR CONDITION  <--==                         NC1704.2
131600*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
131700     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1704.2
131800     MOVE   "MPY-TEST-F2-24" TO PAR-NAME.                         NC1704.2
131900     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
132000     MOVE    .00001 TO WRK-DU-4P1-1.                              NC1704.2
132100     MOVE    0      TO WRK-DU-2V0-1.                              NC1704.2
132200     MOVE    0      TO WRK-DU-2V0-2.                              NC1704.2
132300     MOVE    0      TO WRK-DU-2V5-1.                              NC1704.2
132400     MOVE    0      TO WRK-NE-2.                                  NC1704.2
132500     MOVE    0      TO WRK-NE-3.                                  NC1704.2
132600     MOVE    1      TO REC-CT.                                    NC1704.2
132700     MOVE   "0"     TO WRK-XN-00001.                              NC1704.2
132800 MPY-TEST-F2-24-0.                                                NC1704.2
132900     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-6V0-1                        NC1704.2
133000       GIVING WRK-DU-2V0-1                                        NC1704.2
133100              WRK-DU-2V0-2 ROUNDED                                NC1704.2
133200              WRK-DU-2V5-1                                        NC1704.2
133300              WRK-NE-2 ROUNDED                                    NC1704.2
133400              WRK-NE-3                                            NC1704.2
133500          NOT ON SIZE ERROR MOVE "1" TO WRK-XN-00001.             NC1704.2
133600     GO TO   MPY-TEST-F2-24-1.                                    NC1704.2
133700 MPY-DELETE-F2-24.                                                NC1704.2
133800     PERFORM DE-LETE.                                             NC1704.2
133900     PERFORM PRINT-DETAIL.                                        NC1704.2
134000     GO TO   MPY-INIT-F2-25.                                      NC1704.2
134100 MPY-TEST-F2-24-1.                                                NC1704.2
134200     IF      WRK-DU-2V0-1  = 9                                    NC1704.2
134300             PERFORM PASS                                         NC1704.2
134400             PERFORM PRINT-DETAIL                                 NC1704.2
134500     ELSE                                                         NC1704.2
134600             PERFORM FAIL                                         NC1704.2
134700             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1704.2
134800             MOVE    9 TO CORRECT-N                               NC1704.2
134900             PERFORM PRINT-DETAIL.                                NC1704.2
135000     ADD 1 TO REC-CT.                                             NC1704.2
135100 MPY-TEST-F2-24-2.                                                NC1704.2
135200     IF      WRK-DU-2V0-2 = 10                                    NC1704.2
135300             PERFORM PASS                                         NC1704.2
135400             PERFORM PRINT-DETAIL                                 NC1704.2
135500     ELSE                                                         NC1704.2
135600             PERFORM FAIL                                         NC1704.2
135700             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1704.2
135800             MOVE    10  TO CORRECT-N                             NC1704.2
135900             PERFORM PRINT-DETAIL.                                NC1704.2
136000     ADD 1 TO REC-CT.                                             NC1704.2
136100 MPY-TEST-F2-24-3.                                                NC1704.2
136200     IF WRK-DU-2V5-1 = 9.99999 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
136300     ELSE                                                         NC1704.2
136400     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 9.99999 TO NC1704.2
136500     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
136600     ADD 1 TO REC-CT.                                             NC1704.2
136700 MPY-TEST-F2-24-4.                                                NC1704.2
136800     IF WRK-NE-2 = "$10.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
136900     ELSE                                                         NC1704.2
137000     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$10.00"       NC1704.2
137100     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
137200     ADD 1 TO REC-CT.                                             NC1704.2
137300 MPY-TEST-F2-24-5.                                                NC1704.2
137400     IF WRK-NE-3 = "$09.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
137500     ELSE                                                         NC1704.2
137600     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$09.99  "     NC1704.2
137700     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
137800     ADD 1 TO REC-CT.                                             NC1704.2
137900 MPY-TEST-F2-24-6.                                                NC1704.2
138000     IF      WRK-XN-00001 = "1"                                   NC1704.2
138100             PERFORM PASS                                         NC1704.2
138200             PERFORM PRINT-DETAIL                                 NC1704.2
138300     ELSE                                                         NC1704.2
138400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1704.2
138500             MOVE    WRK-DU-6V0-2  TO COMPUTED-X                  NC1704.2
138600             MOVE   "1"            TO CORRECT-X                   NC1704.2
138700             PERFORM FAIL                                         NC1704.2
138800             PERFORM PRINT-DETAIL.                                NC1704.2
138900*                                                                 NC1704.2
139000 MPY-INIT-F2-25.                                                  NC1704.2
139100*    ==-->   SIZE ERROR CONDITION   <--==                         NC1704.2
139200*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
139300     MOVE   "V1-67 6.4.2"    TO ANSI-REFERENCE.                   NC1704.2
139400     MOVE   "MPY-TEST-F2-25" TO PAR-NAME.                         NC1704.2
139500     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
139600     MOVE    1 TO REC-CT.                                         NC1704.2
139700     MOVE    0 TO WRK-DU-2V0-1.                                   NC1704.2
139800     MOVE    0 TO WRK-DU-2V0-2.                                   NC1704.2
139900     MOVE    0 TO WRK-DU-2V5-1.                                   NC1704.2
140000     MOVE    0 TO WRK-NE-2.                                       NC1704.2
140100     MOVE    0 TO WRK-NE-3.                                       NC1704.2
140200     MOVE   "0" TO WRK-XN-00001.                                  NC1704.2
140300 MPY-TEST-F2-25-0.                                                NC1704.2
140400     MULTIPLY WRK-DU-6V0-1 BY WRK-DU-6V0-1                        NC1704.2
140500       GIVING WRK-DU-2V0-1                                        NC1704.2
140600              WRK-DU-2V0-2 ROUNDED                                NC1704.2
140700              WRK-DU-2V5-1                                        NC1704.2
140800              WRK-NE-2 ROUNDED                                    NC1704.2
140900              WRK-NE-3                                            NC1704.2
141000              ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1704.2
141100          NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.             NC1704.2
141200     GO TO   MPY-TEST-F2-25-1.                                    NC1704.2
141300 MPY-DELETE-F2-25.                                                NC1704.2
141400     PERFORM DE-LETE.                                             NC1704.2
141500     PERFORM PRINT-DETAIL.                                        NC1704.2
141600     GO TO   MPY-INIT-F2-26.                                      NC1704.2
141700 MPY-TEST-F2-25-1.                                                NC1704.2
141800     IF      WRK-DU-2V0-1  = 0                                    NC1704.2
141900             PERFORM PASS                                         NC1704.2
142000             PERFORM PRINT-DETAIL                                 NC1704.2
142100     ELSE                                                         NC1704.2
142200             PERFORM FAIL                                         NC1704.2
142300             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1704.2
142400             MOVE    0 TO CORRECT-N                               NC1704.2
142500             PERFORM PRINT-DETAIL.                                NC1704.2
142600     ADD 1 TO REC-CT.                                             NC1704.2
142700 MPY-TEST-F2-25-2.                                                NC1704.2
142800     IF      WRK-DU-2V0-2 = 00                                    NC1704.2
142900             PERFORM PASS                                         NC1704.2
143000             PERFORM PRINT-DETAIL                                 NC1704.2
143100     ELSE                                                         NC1704.2
143200             PERFORM FAIL                                         NC1704.2
143300             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1704.2
143400             MOVE    00  TO CORRECT-N                             NC1704.2
143500             PERFORM PRINT-DETAIL.                                NC1704.2
143600     ADD 1 TO REC-CT.                                             NC1704.2
143700 MPY-TEST-F2-25-3.                                                NC1704.2
143800     IF WRK-DU-2V5-1 = 0.00000 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
143900     ELSE                                                         NC1704.2
144000     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 0.00000 TO NC1704.2
144100     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
144200     ADD 1 TO REC-CT.                                             NC1704.2
144300 MPY-TEST-F2-25-4.                                                NC1704.2
144400     IF WRK-NE-2 = "$**.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
144500     ELSE                                                         NC1704.2
144600     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$**.00"       NC1704.2
144700     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
144800     ADD 1 TO REC-CT.                                             NC1704.2
144900 MPY-TEST-F2-25-5.                                                NC1704.2
145000     IF WRK-NE-3 = "$00.00  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
145100     ELSE                                                         NC1704.2
145200     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$00.00  "     NC1704.2
145300     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
145400     ADD 1 TO REC-CT.                                             NC1704.2
145500 MPY-TEST-F2-25-6.                                                NC1704.2
145600     IF      WRK-XN-00001 = "1"                                   NC1704.2
145700             PERFORM PASS                                         NC1704.2
145800             PERFORM PRINT-DETAIL                                 NC1704.2
145900     ELSE                                                         NC1704.2
146000             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
146100             MOVE    WRK-DU-6V0-2  TO COMPUTED-X                  NC1704.2
146200             MOVE   "1"            TO CORRECT-X                   NC1704.2
146300             PERFORM FAIL                                         NC1704.2
146400             PERFORM PRINT-DETAIL.                                NC1704.2
146500*                                                                 NC1704.2
146600*                                                                 NC1704.2
146700 MPY-INIT-F2-26.                                                  NC1704.2
146800*    ==-->  NO SIZE ERROR CONDITION <--==                         NC1704.2
146900*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1704.2
147000     MOVE   "MPY-TEST-F2-26" TO PAR-NAME.                         NC1704.2
147100     MOVE   "V1-67 6.4.2"    TO ANSI-REFERENCE.                   NC1704.2
147200     MOVE    999999 TO WRK-DU-6V0-1.                              NC1704.2
147300     MOVE    .00001 TO WRK-DU-4P1-1.                              NC1704.2
147400     MOVE    1 TO REC-CT.                                         NC1704.2
147500     MOVE    0 TO WRK-DU-2V0-1.                                   NC1704.2
147600     MOVE    0 TO WRK-DU-2V5-1.                                   NC1704.2
147700     MOVE    0 TO WRK-NE-2.                                       NC1704.2
147800     MOVE    0 TO WRK-NE-3.                                       NC1704.2
147900     MOVE   "0" TO WRK-XN-00001.                                  NC1704.2
148000 MPY-TEST-F2-26-0.                                                NC1704.2
148100     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-6V0-1                        NC1704.2
148200       GIVING WRK-DU-2V0-1                                        NC1704.2
148300              WRK-DU-2V0-2 ROUNDED                                NC1704.2
148400              WRK-DU-2V5-1                                        NC1704.2
148500              WRK-NE-2 ROUNDED                                    NC1704.2
148600              WRK-NE-3                                            NC1704.2
148700              ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1704.2
148800          NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.             NC1704.2
148900     GO TO   MPY-TEST-F2-26-1.                                    NC1704.2
149000 MPY-DELETE-F2-26.                                                NC1704.2
149100     PERFORM DE-LETE.                                             NC1704.2
149200     PERFORM PRINT-DETAIL.                                        NC1704.2
149300     GO TO   MPY-INIT-F2-27.                                      NC1704.2
149400 MPY-TEST-F2-26-1.                                                NC1704.2
149500     IF      WRK-DU-2V0-1  = 9                                    NC1704.2
149600             PERFORM PASS                                         NC1704.2
149700             PERFORM PRINT-DETAIL                                 NC1704.2
149800     ELSE                                                         NC1704.2
149900             PERFORM FAIL                                         NC1704.2
150000             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1704.2
150100             MOVE    9 TO CORRECT-N                               NC1704.2
150200             PERFORM PRINT-DETAIL.                                NC1704.2
150300     ADD 1 TO REC-CT.                                             NC1704.2
150400 MPY-TEST-F2-26-2.                                                NC1704.2
150500     IF      WRK-DU-2V0-2 = 10                                    NC1704.2
150600             PERFORM PASS                                         NC1704.2
150700             PERFORM PRINT-DETAIL                                 NC1704.2
150800     ELSE                                                         NC1704.2
150900             PERFORM FAIL                                         NC1704.2
151000             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1704.2
151100             MOVE    10  TO CORRECT-N                             NC1704.2
151200             PERFORM PRINT-DETAIL.                                NC1704.2
151300     ADD 1 TO REC-CT.                                             NC1704.2
151400 MPY-TEST-F2-26-3.                                                NC1704.2
151500     IF WRK-DU-2V5-1 = 9.99999 PERFORM PASS PERFORM PRINT-DETAIL  NC1704.2
151600     ELSE                                                         NC1704.2
151700     PERFORM FAIL MOVE WRK-DU-2V5-1 TO COMPUTED-N MOVE 9.99999 TO NC1704.2
151800     CORRECT-N PERFORM PRINT-DETAIL.                              NC1704.2
151900     ADD 1 TO REC-CT.                                             NC1704.2
152000 MPY-TEST-F2-26-4.                                                NC1704.2
152100     IF WRK-NE-2 = "$10.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1704.2
152200     ELSE                                                         NC1704.2
152300     PERFORM FAIL MOVE WRK-NE-2 TO COMPUTED-A MOVE "$10.00"       NC1704.2
152400     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
152500     ADD 1 TO REC-CT.                                             NC1704.2
152600 MPY-TEST-F2-26-5.                                                NC1704.2
152700     IF WRK-NE-3 = "$09.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1704.2
152800     ELSE                                                         NC1704.2
152900     PERFORM FAIL MOVE WRK-NE-3 TO COMPUTED-A MOVE "$09.99  "     NC1704.2
153000     TO CORRECT-A PERFORM PRINT-DETAIL.                           NC1704.2
153100     ADD 1 TO REC-CT.                                             NC1704.2
153200 MPY-TEST-F2-26-6.                                                NC1704.2
153300     IF      WRK-XN-00001 = "2"                                   NC1704.2
153400             PERFORM PASS                                         NC1704.2
153500             PERFORM PRINT-DETAIL                                 NC1704.2
153600     ELSE                                                         NC1704.2
153700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1704.2
153800             MOVE    WRK-DU-6V0-2  TO COMPUTED-X                  NC1704.2
153900             MOVE   "2"            TO CORRECT-X                   NC1704.2
154000             PERFORM FAIL                                         NC1704.2
154100             PERFORM PRINT-DETAIL.                                NC1704.2
154200*                                                                 NC1704.2
154300 MPY-INIT-F2-27.                                                  NC1704.2
154400*    ==--> EXPLICIT SCOPE TERMINATOR <--==                        NC1704.2
154500     MOVE   "1V-41 6.4.3"    TO ANSI-REFERENCE.                   NC1704.2
154600     MOVE    "MPY-TEST-F2-27" TO PAR-NAME.                        NC1704.2
154700     MOVE    "A" TO XRAY.                                         NC1704.2
154800     MOVE 80.12  TO MULT1.                                        NC1704.2
154900     MOVE   -56  TO MULT4.                                        NC1704.2
155000     MOVE     4  TO MULT5.                                        NC1704.2
155100     MOVE     0  TO WRK-DS-10V00.                                 NC1704.2
155200     MOVE     0  TO WRK-DS-02V00.                                 NC1704.2
155300     MOVE     1  TO REC-CT.                                       NC1704.2
155400 MPY-TEST-F2-27-0.                                                NC1704.2
155500     MULTIPLY MULT4 BY MULT1                                      NC1704.2
155600       GIVING MULT5                                               NC1704.2
155700              ON SIZE ERROR                                       NC1704.2
155800              MOVE    "H" TO XRAY                                 NC1704.2
155900              MOVE    28  TO WRK-DS-10V00                         NC1704.2
156000              MOVE   -19  TO WRK-DS-02V00                         NC1704.2
156100     END-MULTIPLY                                                 NC1704.2
156200     MOVE    99 TO WRK-CS-18V00.                                  NC1704.2
156300     GO TO MPY-TEST-F2-27-1.                                      NC1704.2
156400 MPY-DELETE-F2-27-1.                                              NC1704.2
156500     PERFORM DE-LETE.                                             NC1704.2
156600     PERFORM PRINT-DETAIL.                                        NC1704.2
156700     GO TO   MPY-INIT-F2-28.                                      NC1704.2
156800 MPY-TEST-F2-27-1.                                                NC1704.2
156900     MOVE "MPY-TEST-F2-27-1" TO PAR-NAME.                         NC1704.2
157000     IF      XRAY    = "H"                                        NC1704.2
157100             PERFORM PASS                                         NC1704.2
157200             PERFORM PRINT-DETAIL                                 NC1704.2
157300     ELSE                                                         NC1704.2
157400             MOVE  XRAY   TO COMPUTED-X                           NC1704.2
157500             MOVE "H"     TO CORRECT-X                            NC1704.2
157600             MOVE "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK    NC1704.2
157700             PERFORM FAIL                                         NC1704.2
157800             PERFORM PRINT-DETAIL.                                NC1704.2
157900     ADD     1 TO REC-CT.                                         NC1704.2
158000 MPY-TEST-F2-27-2.                                                NC1704.2
158100     MOVE "MPY-TEST-F2-27-2" TO PAR-NAME.                         NC1704.2
158200     IF      WRK-DS-10V00 = 0000000028                            NC1704.2
158300             PERFORM PASS                                         NC1704.2
158400             PERFORM PRINT-DETAIL                                 NC1704.2
158500     ELSE                                                         NC1704.2
158600             MOVE  WRK-DS-10V00 TO COMPUTED-N                     NC1704.2
158700             MOVE  28           TO CORRECT-N                      NC1704.2
158800             MOVE "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK    NC1704.2
158900             PERFORM FAIL                                         NC1704.2
159000             PERFORM PRINT-DETAIL.                                NC1704.2
159100     ADD     1 TO REC-CT.                                         NC1704.2
159200 MPY-TEST-F2-27-3.                                                NC1704.2
159300     MOVE  "MPY-TEST-F2-27-3" TO PAR-NAME.                        NC1704.2
159400     IF      WRK-DS-02V00 = -19                                   NC1704.2
159500             PERFORM PASS                                         NC1704.2
159600             PERFORM PRINT-DETAIL                                 NC1704.2
159700     ELSE                                                         NC1704.2
159800             MOVE  WRK-DS-02V00 TO COMPUTED-N                     NC1704.2
159900             MOVE  -19          TO CORRECT-N                      NC1704.2
160000             MOVE "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK    NC1704.2
160100             PERFORM FAIL                                         NC1704.2
160200             PERFORM PRINT-DETAIL.                                NC1704.2
160300     ADD     1 TO REC-CT.                                         NC1704.2
160400 MPY-TEST-F2-27-4.                                                NC1704.2
160500     MOVE "MPY-TEST-F2-27-4" TO PAR-NAME.                         NC1704.2
160600     IF      WRK-CS-18V00 = 000000000000000099                    NC1704.2
160700             PERFORM PASS                                         NC1704.2
160800             PERFORM PRINT-DETAIL                                 NC1704.2
160900     ELSE                                                         NC1704.2
161000             MOVE  WRK-CS-18V00 TO COMPUTED-N                     NC1704.2
161100             MOVE  99           TO CORRECT-N                      NC1704.2
161200             MOVE "SCOPE TERMINATOR IGNORED" TO RE-MARK           NC1704.2
161300             PERFORM FAIL                                         NC1704.2
161400             PERFORM PRINT-DETAIL.                                NC1704.2
161500     ADD     1 TO REC-CT.                                         NC1704.2
161600 MPY-TEST-F2-27-5.                                                NC1704.2
161700     MOVE "MPY-TEST-F2-27-5" TO PAR-NAME.                         NC1704.2
161800     IF      MULT5 = 4                                            NC1704.2
161900             PERFORM PASS                                         NC1704.2
162000             PERFORM PRINT-DETAIL                                 NC1704.2
162100     ELSE                                                         NC1704.2
162200             MOVE  MULT5 TO COMPUTED-N                            NC1704.2
162300             MOVE  4     TO CORRECT-N                             NC1704.2
162400             MOVE "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK    NC1704.2
162500             PERFORM FAIL                                         NC1704.2
162600             PERFORM PRINT-DETAIL.                                NC1704.2
162700*                                                                 NC1704.2
162800 MPY-INIT-F2-28.                                                  NC1704.2
162900*    ==-->  EXPLICIT SCOPE TERMINATOR  <--==                      NC1704.2
163000     MOVE   "1V-41 6.4.3"    TO ANSI-REFERENCE.                   NC1704.2
163100     MOVE    "MPY-TEST-F2-28" TO PAR-NAME.                        NC1704.2
163200     MOVE   "1"   TO WRK-XN-00001.                                NC1704.2
163300     MOVE    0    TO WRK-DS-05V00.                                NC1704.2
163400     MOVE    0    TO WRK-DS-02V00.                                NC1704.2
163500     MOVE    0    TO WRK-DS-10V00.                                NC1704.2
163600     MOVE    1    TO REC-CT.                                      NC1704.2
163700 MPY-TEST-F2-28-0.                                                NC1704.2
163800     MULTIPLY A01ONE-DS-P0801 BY A12ONES-DS-12V00                 NC1704.2
163900       GIVING WRK-DS-10V00                                        NC1704.2
164000           ON SIZE ERROR                                          NC1704.2
164100              MOVE "0" TO WRK-XN-00001                            NC1704.2
164200              MOVE  38 TO WRK-DS-05V00                            NC1704.2
164300              MOVE -19 TO WRK-DS-02V00                            NC1704.2
164400     END-MULTIPLY                                                 NC1704.2
164500     MOVE    99 TO WRK-CS-18V00.                                  NC1704.2
164600     GO TO   MPY-TEST-F2-28-1.                                    NC1704.2
164700 MPY-DELETE-F2-28-1.                                              NC1704.2
164800     PERFORM DE-LETE.                                             NC1704.2
164900     PERFORM PRINT-DETAIL.                                        NC1704.2
165000     GO TO   MPY-INIT-F2-29.                                      NC1704.2
165100 MPY-TEST-F2-28-1.                                                NC1704.2
165200     MOVE    "MPY-TEST-F2-28-1" TO PAR-NAME.                      NC1704.2
165300     IF      WRK-XN-00001 = "1"                                   NC1704.2
165400             PERFORM PASS                                         NC1704.2
165500             PERFORM PRINT-DETAIL                                 NC1704.2
165600     ELSE                                                         NC1704.2
165700             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1704.2
165800             MOVE   "1"           TO CORRECT-X                    NC1704.2
165900             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1704.2
166000                  TO RE-MARK                                      NC1704.2
166100             PERFORM FAIL                                         NC1704.2
166200             PERFORM PRINT-DETAIL.                                NC1704.2
166300     ADD     1 TO REC-CT.                                         NC1704.2
166400 MPY-TEST-F2-28-2.                                                NC1704.2
166500     MOVE   "MPY-TEST-F2-28-2" TO PAR-NAME.                       NC1704.2
166600     IF      WRK-DS-10V00 = 0000000111                            NC1704.2
166700             PERFORM PASS                                         NC1704.2
166800             PERFORM PRINT-DETAIL                                 NC1704.2
166900     ELSE                                                         NC1704.2
167000             MOVE  WRK-DS-10V00 TO COMPUTED-N                     NC1704.2
167100             MOVE  0000000111   TO CORRECT-N                      NC1704.2
167200             MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK     NC1704.2
167300             PERFORM FAIL                                         NC1704.2
167400             PERFORM PRINT-DETAIL.                                NC1704.2
167500     ADD     1 TO REC-CT.                                         NC1704.2
167600 MPY-TEST-F2-28-3.                                                NC1704.2
167700     MOVE    "MPY-TEST-F2-28-3" TO PAR-NAME.                      NC1704.2
167800     IF      WRK-DS-05V00 = 0                                     NC1704.2
167900             PERFORM PASS                                         NC1704.2
168000             PERFORM PRINT-DETAIL                                 NC1704.2
168100     ELSE                                                         NC1704.2
168200             MOVE  WRK-DS-05V00 TO COMPUTED-N                     NC1704.2
168300             MOVE  0            TO CORRECT-N                      NC1704.2
168400             MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED"          NC1704.2
168500                TO RE-MARK                                        NC1704.2
168600             PERFORM FAIL                                         NC1704.2
168700             PERFORM PRINT-DETAIL.                                NC1704.2
168800     ADD     1 TO REC-CT.                                         NC1704.2
168900 MPY-TEST-F2-28-4.                                                NC1704.2
169000     MOVE   "MPY-TEST-F2-28-4" TO PAR-NAME.                       NC1704.2
169100     IF      WRK-DS-02V00 = 0                                     NC1704.2
169200             PERFORM PASS                                         NC1704.2
169300             PERFORM PRINT-DETAIL                                 NC1704.2
169400     ELSE                                                         NC1704.2
169500             MOVE  WRK-DS-02V00 TO COMPUTED-N                     NC1704.2
169600             MOVE  0            TO CORRECT-N                      NC1704.2
169700             MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED"          NC1704.2
169800                TO RE-MARK                                        NC1704.2
169900             PERFORM FAIL                                         NC1704.2
170000             PERFORM PRINT-DETAIL.                                NC1704.2
170100     ADD     1 TO REC-CT.                                         NC1704.2
170200 MPY-TEST-F2-28-5.                                                NC1704.2
170300     MOVE   "MPY-TEST-F2-28-5" TO PAR-NAME.                       NC1704.2
170400     IF      WRK-CS-18V00 = 000000000000000099                    NC1704.2
170500             PERFORM PASS                                         NC1704.2
170600             PERFORM PRINT-DETAIL                                 NC1704.2
170700     ELSE                                                         NC1704.2
170800             MOVE  WRK-CS-18V00 TO COMPUTED-N                     NC1704.2
170900             MOVE  99           TO CORRECT-N                      NC1704.2
171000             MOVE "SCOPE TERMINATOR IGNORED"                      NC1704.2
171100                TO RE-MARK                                        NC1704.2
171200             PERFORM FAIL                                         NC1704.2
171300             PERFORM PRINT-DETAIL.                                NC1704.2
171400*                                                                 NC1704.2
171500 MPY-INIT-F2-29.                                                  NC1704.2
171600*    ==--> EXPLICIT SCOPE TERMINATOR <--==                        NC1704.2
171700*    ==-->    SIZE ERROR CONDITION   <--==                        NC1704.2
171800     MOVE   "1V-41 6.4.3"    TO ANSI-REFERENCE.                   NC1704.2
171900     MOVE    "MPY-TEST-F2-29" TO PAR-NAME.                        NC1704.2
172000     MOVE    "A" TO XRAY.                                         NC1704.2
172100     MOVE 80.12  TO MULT1.                                        NC1704.2
172200     MOVE   -56  TO MULT4.                                        NC1704.2
172300     MOVE     4  TO MULT5.                                        NC1704.2
172400     MOVE     1  TO REC-CT.                                       NC1704.2
172500     MOVE     0  TO WRK-DS-10V00.                                 NC1704.2
172600     MOVE     0  TO WRK-DS-02V00.                                 NC1704.2
172700 MPY-TEST-F2-29-0.                                                NC1704.2
172800     MULTIPLY MULT4 BY MULT1                                      NC1704.2
172900       GIVING MULT5                                               NC1704.2
173000       NOT ON SIZE ERROR                                          NC1704.2
173100              MOVE    "H" TO XRAY                                 NC1704.2
173200              MOVE    38  TO WRK-DS-10V00                         NC1704.2
173300              MOVE   -19  TO WRK-DS-02V00                         NC1704.2
173400     END-MULTIPLY                                                 NC1704.2
173500     MOVE    99 TO WRK-CS-18V00.                                  NC1704.2
173600      GO TO MPY-TEST-F2-29-1.                                     NC1704.2
173700 MPY-DELETE-F2-29-1.                                              NC1704.2
173800     PERFORM DE-LETE.                                             NC1704.2
173900     PERFORM PRINT-DETAIL.                                        NC1704.2
174000     GO TO   MPY-INIT-F2-30.                                      NC1704.2
174100 MPY-TEST-F2-29-1.                                                NC1704.2
174200     MOVE    "MPY-TEST-F2-29-1" TO PAR-NAME.                      NC1704.2
174300     IF      XRAY    = "A"                                        NC1704.2
174400             PERFORM PASS                                         NC1704.2
174500             PERFORM PRINT-DETAIL                                 NC1704.2
174600     ELSE                                                         NC1704.2
174700             MOVE    XRAY   TO COMPUTED-X                         NC1704.2
174800             MOVE   "A"     TO CORRECT-X                          NC1704.2
174900             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1704.2
175000                  TO RE-MARK                                      NC1704.2
175100             PERFORM FAIL                                         NC1704.2
175200             PERFORM PRINT-DETAIL.                                NC1704.2
175300     ADD     1 TO REC-CT.                                         NC1704.2
175400 MPY-TEST-F2-29-2.                                                NC1704.2
175500     MOVE   "MPY-TEST-F2-29-2" TO PAR-NAME.                       NC1704.2
175600     IF      WRK-DS-10V00 = 0                                     NC1704.2
175700             PERFORM PASS                                         NC1704.2
175800             PERFORM PRINT-DETAIL                                 NC1704.2
175900     ELSE                                                         NC1704.2
176000             MOVE   WRK-DS-10V00 TO COMPUTED-N                    NC1704.2
176100             MOVE   0            TO CORRECT-N                     NC1704.2
176200             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1704.2
176300                  TO RE-MARK                                      NC1704.2
176400             PERFORM FAIL                                         NC1704.2
176500             PERFORM PRINT-DETAIL.                                NC1704.2
176600     ADD     1 TO REC-CT.                                         NC1704.2
176700 MPY-TEST-F2-29-3.                                                NC1704.2
176800     MOVE   "MPY-TEST-F2-29-3" TO PAR-NAME.                       NC1704.2
176900     IF      WRK-DS-02V00 = 0                                     NC1704.2
177000             PERFORM PASS                                         NC1704.2
177100             PERFORM PRINT-DETAIL                                 NC1704.2
177200     ELSE                                                         NC1704.2
177300             MOVE   WRK-DS-02V00 TO COMPUTED-N                    NC1704.2
177400             MOVE   0            TO CORRECT-N                     NC1704.2
177500             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1704.2
177600                  TO RE-MARK                                      NC1704.2
177700             PERFORM FAIL                                         NC1704.2
177800             PERFORM PRINT-DETAIL.                                NC1704.2
177900     ADD     1 TO REC-CT.                                         NC1704.2
178000 MPY-TEST-F2-29-4.                                                NC1704.2
178100     MOVE   "MPY-TEST-F2-29-4" TO PAR-NAME.                       NC1704.2
178200     IF      WRK-CS-18V00 = 000000000000000099                    NC1704.2
178300             PERFORM PASS                                         NC1704.2
178400             PERFORM PRINT-DETAIL                                 NC1704.2
178500     ELSE                                                         NC1704.2
178600             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1704.2
178700             MOVE    99           TO CORRECT-N                    NC1704.2
178800             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1704.2
178900                  TO RE-MARK                                      NC1704.2
179000             PERFORM FAIL                                         NC1704.2
179100             PERFORM PRINT-DETAIL.                                NC1704.2
179200     ADD     1 TO REC-CT.                                         NC1704.2
179300 MPY-TEST-F2-29-5.                                                NC1704.2
179400     MOVE   "MPY-TEST-F2-29-5" TO PAR-NAME.                       NC1704.2
179500     IF      MULT5   = 4                                          NC1704.2
179600             PERFORM PASS                                         NC1704.2
179700             PERFORM PRINT-DETAIL                                 NC1704.2
179800     ELSE                                                         NC1704.2
179900             MOVE   MULT5  TO COMPUTED-N                          NC1704.2
180000             MOVE   4      TO CORRECT-N                           NC1704.2
180100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1704.2
180200                  TO RE-MARK                                      NC1704.2
180300             PERFORM FAIL                                         NC1704.2
180400             PERFORM PRINT-DETAIL.                                NC1704.2
180500*                                                                 NC1704.2
180600 MPY-INIT-F2-30.                                                  NC1704.2
180700*    ==--> EXPLICIT SCOPE TERMINATOR <--==                        NC1704.2
180800*    ==-->  NO SIZE ERROR CONDITION  <--==                        NC1704.2
180900     MOVE   "MPY-TEST-F2-30" TO PAR-NAME                          NC1704.2
181000     MOVE   "1V-41 6.4.3"    TO ANSI-REFERENCE.                   NC1704.2
181100     MOVE   "1"   TO WRK-XN-00001.                                NC1704.2
181200     MOVE    0    TO WRK-DS-05V00.                                NC1704.2
181300     MOVE    0    TO WRK-DS-02V00.                                NC1704.2
181400     MOVE    0    TO WRK-DS-10V00.                                NC1704.2
181500     MOVE    1    TO REC-CT.                                      NC1704.2
181600 MPY-TEST-F2-30-0.                                                NC1704.2
181700     MULTIPLY A01ONE-DS-P0801 BY A12ONES-DS-12V00                 NC1704.2
181800       GIVING WRK-DS-10V00                                        NC1704.2
181900       NOT ON SIZE ERROR                                          NC1704.2
182000              MOVE "0" TO WRK-XN-00001                            NC1704.2
182100              MOVE  38 TO WRK-DS-05V00                            NC1704.2
182200              MOVE -19 TO WRK-DS-02V00                            NC1704.2
182300     END-MULTIPLY                                                 NC1704.2
182400     MOVE    99 TO WRK-CS-18V00.                                  NC1704.2
182500     GO TO   MPY-TEST-F2-30-1.                                    NC1704.2
182600 MPY-DELETE-F2-30-1.                                              NC1704.2
182700     PERFORM DE-LETE.                                             NC1704.2
182800     PERFORM PRINT-DETAIL.                                        NC1704.2
182900     GO TO   MPY-INIT-F2-31.                                      NC1704.2
183000 MPY-TEST-F2-30-1.                                                NC1704.2
183100     MOVE   "MPY-TEST-F2-30-1" TO PAR-NAME.                       NC1704.2
183200     IF      WRK-XN-00001 = "0"                                   NC1704.2
183300             PERFORM PASS                                         NC1704.2
183400             PERFORM PRINT-DETAIL                                 NC1704.2
183500     ELSE                                                         NC1704.2
183600             MOVE  WRK-XN-00001 TO COMPUTED-X                     NC1704.2
183700             MOVE "0"           TO CORRECT-X                      NC1704.2
183800             MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED"          NC1704.2
183900                TO RE-MARK                                        NC1704.2
184000             PERFORM FAIL                                         NC1704.2
184100             PERFORM PRINT-DETAIL.                                NC1704.2
184200     ADD     1 TO REC-CT.                                         NC1704.2
184300 MPY-TEST-F2-30-2.                                                NC1704.2
184400     MOVE   "MPY-TEST-F2-30-2" TO PAR-NAME.                       NC1704.2
184500     IF      WRK-DS-10V00 = 0000000111                            NC1704.2
184600             PERFORM PASS                                         NC1704.2
184700             PERFORM PRINT-DETAIL                                 NC1704.2
184800     ELSE                                                         NC1704.2
184900             MOVE  WRK-DS-10V00 TO COMPUTED-N                     NC1704.2
185000             MOVE  28           TO CORRECT-N                      NC1704.2
185100             MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK     NC1704.2
185200             PERFORM FAIL                                         NC1704.2
185300             PERFORM PRINT-DETAIL.                                NC1704.2
185400     ADD     1 TO REC-CT.                                         NC1704.2
185500 MPY-TEST-F2-30-3.                                                NC1704.2
185600     MOVE   "MPY-TEST-F2-30-3" TO PAR-NAME.                       NC1704.2
185700     IF      WRK-DS-05V00 = 38                                    NC1704.2
185800             PERFORM PASS                                         NC1704.2
185900             PERFORM PRINT-DETAIL                                 NC1704.2
186000     ELSE                                                         NC1704.2
186100             MOVE  WRK-DS-05V00 TO COMPUTED-N                     NC1704.2
186200             MOVE  38           TO CORRECT-N                      NC1704.2
186300             MOVE "SIZE ERROR SHOULD HAVE OCCURRED"               NC1704.2
186400                TO RE-MARK                                        NC1704.2
186500             PERFORM FAIL                                         NC1704.2
186600             PERFORM PRINT-DETAIL.                                NC1704.2
186700     ADD     1 TO REC-CT.                                         NC1704.2
186800 MPY-TEST-F2-30-4.                                                NC1704.2
186900     MOVE   "MPY-TEST-F2-30-4" TO PAR-NAME.                       NC1704.2
187000     IF      WRK-DS-02V00 = -19                                   NC1704.2
187100             PERFORM PASS                                         NC1704.2
187200             PERFORM PRINT-DETAIL                                 NC1704.2
187300     ELSE                                                         NC1704.2
187400             MOVE  WRK-DS-02V00 TO COMPUTED-N                     NC1704.2
187500             MOVE  -19          TO CORRECT-N                      NC1704.2
187600             MOVE "SIZE ERROR SHOULD HAVE OCCURRED"               NC1704.2
187700                TO RE-MARK                                        NC1704.2
187800             PERFORM FAIL                                         NC1704.2
187900             PERFORM PRINT-DETAIL.                                NC1704.2
188000     ADD     1 TO REC-CT.                                         NC1704.2
188100 MPY-TEST-F2-30-5.                                                NC1704.2
188200     MOVE   "MPY-TEST-F2-30-5" TO PAR-NAME.                       NC1704.2
188300     IF      WRK-CS-18V00 = 000000000000000099                    NC1704.2
188400             PERFORM PASS                                         NC1704.2
188500             PERFORM PRINT-DETAIL                                 NC1704.2
188600     ELSE                                                         NC1704.2
188700             MOVE  WRK-CS-18V00 TO COMPUTED-N                     NC1704.2
188800             MOVE  99           TO CORRECT-N                      NC1704.2
188900             MOVE "SCOPE TERMINATOR IGNORED"                      NC1704.2
189000                TO RE-MARK                                        NC1704.2
189100             PERFORM FAIL                                         NC1704.2
189200             PERFORM PRINT-DETAIL.                                NC1704.2
189300*                                                                 NC1704.2
189400 MPY-INIT-F2-31.                                                  NC1704.2
189500*    ==--> EXPLICIT SCOPE TERMINATOR <--==                        NC1704.2
189600*    ==-->  NO SIZE ERROR CONDITION  <--==                        NC1704.2
189700     MOVE   "1V-41 6.4.3"    TO ANSI-REFERENCE.                   NC1704.2
189800     MOVE   "MPY-TEST-F2-31" TO PAR-NAME                          NC1704.2
189900     MOVE 80.12  TO MULT1.                                        NC1704.2
190000     MOVE   -56  TO MULT4.                                        NC1704.2
190100     MOVE     4  TO MULT5.                                        NC1704.2
190200     MOVE     1  TO REC-CT.                                       NC1704.2
190300     MOVE    "A" TO XRAY.                                         NC1704.2
190400 MPY-TEST-F2-31-0.                                                NC1704.2
190500     MULTIPLY MULT4 BY MULT1                                      NC1704.2
190600       GIVING MULT5                                               NC1704.2
190700           ON SIZE ERROR                                          NC1704.2
190800              MOVE    "B" TO XRAY                                 NC1704.2
190900       NOT ON SIZE ERROR                                          NC1704.2
191000              MOVE    "C" TO XRAY                                 NC1704.2
191100     END-MULTIPLY                                                 NC1704.2
191200     MOVE    99 TO WRK-CS-18V00.                                  NC1704.2
191300      GO TO MPY-TEST-F2-31-1.                                     NC1704.2
191400 MPY-DELETE-F2-31-1.                                              NC1704.2
191500     PERFORM DE-LETE.                                             NC1704.2
191600     PERFORM PRINT-DETAIL.                                        NC1704.2
191700     GO TO    MPY-INIT-F2-32.                                     NC1704.2
191800 MPY-TEST-F2-31-1.                                                NC1704.2
191900     MOVE   "MPY-TEST-F2-31-1" TO PAR-NAME.                       NC1704.2
192000     IF      XRAY    = "B"                                        NC1704.2
192100             PERFORM PASS                                         NC1704.2
192200             PERFORM PRINT-DETAIL                                 NC1704.2
192300     ELSE                                                         NC1704.2
192400             MOVE    XRAY  TO COMPUTED-X                          NC1704.2
192500             MOVE   "B"    TO CORRECT-X                           NC1704.2
192600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
192700             PERFORM FAIL                                         NC1704.2
192800             PERFORM PRINT-DETAIL.                                NC1704.2
192900     ADD     1 TO REC-CT.                                         NC1704.2
193000 MPY-TEST-F2-31-2.                                                NC1704.2
193100     MOVE   "MPY-TEST-F2-31-2" TO PAR-NAME.                       NC1704.2
193200     IF      MULT5   = 4                                          NC1704.2
193300             PERFORM PASS                                         NC1704.2
193400             PERFORM PRINT-DETAIL                                 NC1704.2
193500     ELSE                                                         NC1704.2
193600             MOVE    MULT5 TO COMPUTED-N                          NC1704.2
193700             MOVE    4     TO CORRECT-N                           NC1704.2
193800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1704.2
193900             PERFORM FAIL                                         NC1704.2
194000             PERFORM PRINT-DETAIL.                                NC1704.2
194100     ADD     1 TO REC-CT.                                         NC1704.2
194200 MPY-TEST-F2-31-3.                                                NC1704.2
194300     MOVE   "MPY-TEST-F2-31-3" TO PAR-NAME.                       NC1704.2
194400     IF      WRK-CS-18V00 = 000000000000000099                    NC1704.2
194500             PERFORM PASS                                         NC1704.2
194600             PERFORM PRINT-DETAIL                                 NC1704.2
194700     ELSE                                                         NC1704.2
194800             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1704.2
194900             MOVE    99           TO CORRECT-N                    NC1704.2
195000             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1704.2
195100             PERFORM FAIL                                         NC1704.2
195200             PERFORM PRINT-DETAIL.                                NC1704.2
195300*                                                                 NC1704.2
195400 MPY-INIT-F2-32.                                                  NC1704.2
195500*    ==--> EXPLICIT SCOPE TERMINATOR <--==                        NC1704.2
195600     MOVE   "1V-41 6.4.3"    TO ANSI-REFERENCE.                   NC1704.2
195700     MOVE   "MPY-TEST-F2-32" TO PAR-NAME                          NC1704.2
195800     MOVE   "0"   TO WRK-XN-00001.                                NC1704.2
195900     MOVE    0    TO WRK-DS-10V00.                                NC1704.2
196000     MOVE    1    TO REC-CT.                                      NC1704.2
196100 MPY-TEST-F2-32-0.                                                NC1704.2
196200     MULTIPLY A01ONE-DS-P0801 BY A12ONES-DS-12V00                 NC1704.2
196300       GIVING WRK-DS-10V00                                        NC1704.2
196400           ON SIZE ERROR                                          NC1704.2
196500              MOVE "1" TO WRK-XN-00001                            NC1704.2
196600       NOT ON SIZE ERROR                                          NC1704.2
196700              MOVE "2" TO WRK-XN-00001                            NC1704.2
196800     END-MULTIPLY                                                 NC1704.2
196900     MOVE    99 TO WRK-CS-18V00.                                  NC1704.2
197000     GO TO   MPY-TEST-F2-32-1.                                    NC1704.2
197100 MPY-DELETE-F2-32-1.                                              NC1704.2
197200     PERFORM DE-LETE.                                             NC1704.2
197300     PERFORM PRINT-DETAIL.                                        NC1704.2
197400     GO TO   CCVS-EXIT.                                           NC1704.2
197500 MPY-TEST-F2-32-1.                                                NC1704.2
197600     MOVE   "MPY-TEST-F2-32-1" TO PAR-NAME.                       NC1704.2
197700     IF      WRK-DS-10V00 = 0000000111                            NC1704.2
197800             PERFORM PASS                                         NC1704.2
197900             PERFORM PRINT-DETAIL                                 NC1704.2
198000     ELSE                                                         NC1704.2
198100             MOVE    0000000111       TO CORRECT-18V0             NC1704.2
198200             MOVE    WRK-DS-10V00     TO COMPUTED-18V0            NC1704.2
198300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1704.2
198400             PERFORM FAIL                                         NC1704.2
198500             PERFORM PRINT-DETAIL.                                NC1704.2
198600     ADD     1 TO REC-CT.                                         NC1704.2
198700 MPY-TEST-F2-32-2.                                                NC1704.2
198800     MOVE   "MPY-TEST-F2-32-2" TO PAR-NAME.                       NC1704.2
198900     IF      WRK-XN-00001 = "2"                                   NC1704.2
199000             PERFORM PASS                                         NC1704.2
199100             PERFORM PRINT-DETAIL                                 NC1704.2
199200     ELSE                                                         NC1704.2
199300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1704.2
199400             MOVE   "2"           TO CORRECT-X                    NC1704.2
199500             MOVE   "SIZE ERROR SHOULD NOT BE EXECUTED"           NC1704.2
199600                  TO RE-MARK                                      NC1704.2
199700             PERFORM FAIL                                         NC1704.2
199800             PERFORM PRINT-DETAIL.                                NC1704.2
199900     ADD     1 TO REC-CT.                                         NC1704.2
200000 MPY-TEST-F2-32-3.                                                NC1704.2
200100     MOVE   "MPY-TEST-F2-32-3" TO PAR-NAME.                       NC1704.2
200200     IF      WRK-CS-18V00 = 000000000000000099                    NC1704.2
200300             PERFORM PASS                                         NC1704.2
200400             PERFORM PRINT-DETAIL                                 NC1704.2
200500     ELSE                                                         NC1704.2
200600             MOVE    WRK-CS-18V00 TO COMPUTED-N                   NC1704.2
200700             MOVE    99           TO CORRECT-N                    NC1704.2
200800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1704.2
200900             PERFORM FAIL                                         NC1704.2
201000             PERFORM PRINT-DETAIL.                                NC1704.2
201100*                                                                 NC1704.2
201200*                                                                 NC1704.2
201300 CCVS-EXIT SECTION.                                               NC1704.2
201400 CCVS-999999.                                                     NC1704.2
201500     GO TO CLOSE-FILES.                                           NC1704.2
      *END-OF,NC170A                                                                  
