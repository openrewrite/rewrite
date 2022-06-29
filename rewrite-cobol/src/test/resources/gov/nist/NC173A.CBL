      *HEADER,COBOL,NC173A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1734.2
000200 PROGRAM-ID.                                                      NC1734.2
000300     NC173A.                                                      NC1734.2
000400****************************************************************  NC1734.2
000500*                                                              *  NC1734.2
000600*    VALIDATION FOR:-                                          *  NC1734.2
000700*                                                              *  NC1734.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1734.2
000900*                                                              *  NC1734.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1734.2
001100*                                                              *  NC1734.2
001200****************************************************************  NC1734.2
001300*                                                              *  NC1734.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1734.2
001500*                                                              *  NC1734.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1734.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1734.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1734.2
001900*                                                              *  NC1734.2
002000****************************************************************  NC1734.2
002100*    THIS PROGRAM TESTS THE FORMAT 3 DIVIDE STATEMENT FOUND       NC1734.2
002200*    IN LEVEL 1.   ALL COMBINATIONS OF THE OPTIONAL PHRASES       NC1734.2
002300*    "SIZE ERROR", "NOT SIZE ERROR" AND "END-MULTIPLY" ARE        NC1734.2
002400*    TESTED, AS WELL AS THE ROUNDED OPTION.                       NC1734.2
002500*                                                                 NC1734.2
002600*    VARIOUS COMBINATIONS OF SIGNED AND UNSIGNED NUMERIC          NC1734.2
002700*    LITERALS, DISPLAY AND COMPUTATIONAL FIELDS ARE USED          NC1734.2
002800*    AS OPERANDS.                                                 NC1734.2
002900*                                                                 NC1734.2
003000*                                                                 NC1734.2
003100 ENVIRONMENT DIVISION.                                            NC1734.2
003200 CONFIGURATION SECTION.                                           NC1734.2
003300 SOURCE-COMPUTER.                                                 NC1734.2
003400     XXXXX082.                                                    NC1734.2
003500 OBJECT-COMPUTER.                                                 NC1734.2
003600     XXXXX083.                                                    NC1734.2
003700 INPUT-OUTPUT SECTION.                                            NC1734.2
003800 FILE-CONTROL.                                                    NC1734.2
003900     SELECT PRINT-FILE ASSIGN TO                                  NC1734.2
004000     XXXXX055.                                                    NC1734.2
004100 DATA DIVISION.                                                   NC1734.2
004200 FILE SECTION.                                                    NC1734.2
004300 FD  PRINT-FILE.                                                  NC1734.2
004400 01  PRINT-REC PICTURE X(120).                                    NC1734.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 NC1734.2
004600 WORKING-STORAGE SECTION.                                         NC1734.2
004700 77  WRK-DS-18V00                PICTURE S9(18).                  NC1734.2
004800 77  A06THREES-DS-03V03          PICTURE S999V999 VALUE 333.333.  NC1734.2
004900 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC1734.2
005000 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06 PICTURE S9(12).        NC1734.2
005100 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1734.2
005200 77  WRK-DS-10V00                PICTURE S9(10).                  NC1734.2
005300 77  WRK-XN-00001                PICTURE X.                       NC1734.2
005400 77  A10ONES-DS-10V00            PICTURE S9(10)                   NC1734.2
005500                                 VALUE 1111111111.                NC1734.2
005600 77  A12THREES-DS-06V06          PICTURE S9(6)V9(6)               NC1734.2
005700                                 VALUE 333333.333333.             NC1734.2
005800 77  WRK-DS-02V00                PICTURE S99.                     NC1734.2
005900 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1734.2
006000 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1734.2
006100 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1734.2
006200 77  A05ONES-DS-00V05            PICTURE SV9(5) VALUE .11111.     NC1734.2
006300 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1734.2
006400                                 VALUE 111111111111.              NC1734.2
006500 77  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1734.2
006600 77  WRK-DS-09V08                PICTURE S9(9)V9(8).              NC1734.2
006700 77  WRK-DS-17V00-S REDEFINES WRK-DS-09V08 PICTURE S9(17).        NC1734.2
006800 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1734.2
006900                                 VALUE 111111111111111111.        NC1734.2
007000 77  WRK-DS-0201P                PICTURE S99P.                    NC1734.2
007100 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1734.2
007200 77  WRK-DU-18V00                PICTURE 9(18).                   NC1734.2
007300 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1734.2
007400                                 VALUE 99.                        NC1734.2
007500 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1734.2
007600                                 VALUE .1.                        NC1734.2
007700 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1734.2
007800 77  WRK-DS-12V00                PICTURE S9(12).                  NC1734.2
007900 77  WRK-DS-01V00                PICTURE S9.                      NC1734.2
008000 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1734.2
008100 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1734.2
008200                                 VALUE 111111111.111111111.       NC1734.2
008300 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1734.2
008400 77  WRK-DS-05V00                PICTURE S9(5).                   NC1734.2
008500 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1734.2
008600 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1734.2
008700 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1734.2
008800 77  XRAY                        PICTURE X.                       NC1734.2
008900 01  WRK-XN-18-1 PIC X(18).                                       NC1734.2
009000 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1734.2
009100 01  WRK-DU-X-18V0-1; REDEFINES WRK-XN-18-1 PIC 9(18).            NC1734.2
009200 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1734.2
009300 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1734.2
009400 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1734.2
009500 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1734.2
009600 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1734.2
009700 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1734.2
009800 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1734.2
009900 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1734.2
010000 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1734.2
010100 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1734.2
010200 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1734.2
010300 01  WRK-DU-2V0-1 PIC 99.                                         NC1734.2
010400 01  WRK-DU-2V0-2 PIC 99.                                         NC1734.2
010500 01  WRK-DU-2V0-3 PIC 99.                                         NC1734.2
010600 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1734.2
010700 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1734.2
010800 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1734.2
010900 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1734.2
011000 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1734.2
011100 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1734.2
011200 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1734.2
011300 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1734.2
011400 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1734.2
011500 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1734.2
011600 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1734.2
011700 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1734.2
011800 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1734.2
011900 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1734.2
012000 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1734.2
012100 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1734.2
012200 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1734.2
012300 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1734.2
012400 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1734.2
012500 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1734.2
012600 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1734.2
012700 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1734.2
012800 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1734.2
012900 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1734.2
013000 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1734.2
013100 01  WRK-NE-2 PIC $**.99.                                         NC1734.2
013200 01  WRK-NE-3 PIC $99.99CR.                                       NC1734.2
013300 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1734.2
013400 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1734.2
013500             VALUE +000000000000000001.                           NC1734.2
013600 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1734.2
013700             VALUE -000000000000000033.                           NC1734.2
013800 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1734.2
013900             VALUE 666666666666666666.                            NC1734.2
014000 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1734.2
014100             VALUE 009999999999999999.                            NC1734.2
014200 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1734.2
014300             VALUE 000022222222222222.                            NC1734.2
014400 01  MULTIPLY-DATA.                                               NC1734.2
014500     02 MULT1                           PICTURE IS 999V99         NC1734.2
014600     VALUE IS 80.12.                                              NC1734.2
014700     02 MULT2                           PICTURE IS 999V999.       NC1734.2
014800     02 MULT3                           PICTURE IS $$99.99.       NC1734.2
014900     02 MULT4                           PICTURE IS S99            NC1734.2
015000     VALUE IS -56.                                                NC1734.2
015100     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1734.2
015200     02 MULT6                           PICTURE IS 99 VALUE IS    NC1734.2
015300     20.                                                          NC1734.2
015400 01  DIVIDE-DATA.                                                 NC1734.2
015500     02 DIV1                            PICTURE IS 9(4)V99        NC1734.2
015600     VALUE IS 1620.36.                                            NC1734.2
015700     02 DIV2                            PICTURE IS 99V9           NC1734.2
015800     VALUE IS 44.1.                                               NC1734.2
015900     02 DIV3                            PICTURE IS 9(4)V9         NC1734.2
016000     VALUE IS 1661.7.                                             NC1734.2
016100     02 DIV4                            PICTURE IS S9V999         NC1734.2
016200     VALUE IS -9.642.                                             NC1734.2
016300     02 DIV-02LEVEL-1.                                            NC1734.2
016400     03 DIV5                            PICTURE IS V99            NC1734.2
016500     VALUE IS .82.                                                NC1734.2
016600     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1734.2
016700     03 DIV7                            PICTURE IS 9V9            NC1734.2
016800     VALUE IS 9.6.                                                NC1734.2
016900 01  DIV-DATA-2.                                                  NC1734.2
017000     02 DIV8                            PICTURE IS 99V9.          NC1734.2
017100     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1734.2
017200     02 DIV10                           PICTURE IS V999.          NC1734.2
017300 01  TEST-RESULTS.                                                NC1734.2
017400     02 FILLER                   PIC X      VALUE SPACE.          NC1734.2
017500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1734.2
017600     02 FILLER                   PIC X      VALUE SPACE.          NC1734.2
017700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1734.2
017800     02 FILLER                   PIC X      VALUE SPACE.          NC1734.2
017900     02  PAR-NAME.                                                NC1734.2
018000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1734.2
018100       03  PARDOT-X              PIC X      VALUE SPACE.          NC1734.2
018200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1734.2
018300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1734.2
018400     02 RE-MARK                  PIC X(61).                       NC1734.2
018500 01  TEST-COMPUTED.                                               NC1734.2
018600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1734.2
018700     02 FILLER                   PIC X(17)  VALUE                 NC1734.2
018800            "       COMPUTED=".                                   NC1734.2
018900     02 COMPUTED-X.                                               NC1734.2
019000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1734.2
019100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1734.2
019200                                 PIC -9(9).9(9).                  NC1734.2
019300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1734.2
019400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1734.2
019500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1734.2
019600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1734.2
019700         04 COMPUTED-18V0                    PIC -9(18).          NC1734.2
019800         04 FILLER                           PIC X.               NC1734.2
019900     03 FILLER PIC X(50) VALUE SPACE.                             NC1734.2
020000 01  TEST-CORRECT.                                                NC1734.2
020100     02 FILLER PIC X(30) VALUE SPACE.                             NC1734.2
020200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1734.2
020300     02 CORRECT-X.                                                NC1734.2
020400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1734.2
020500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1734.2
020600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1734.2
020700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1734.2
020800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1734.2
020900     03      CR-18V0 REDEFINES CORRECT-A.                         NC1734.2
021000         04 CORRECT-18V0                     PIC -9(18).          NC1734.2
021100         04 FILLER                           PIC X.               NC1734.2
021200     03 FILLER PIC X(2) VALUE SPACE.                              NC1734.2
021300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1734.2
021400 01  CCVS-C-1.                                                    NC1734.2
021500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1734.2
021600-    "SS  PARAGRAPH-NAME                                          NC1734.2
021700-    "       REMARKS".                                            NC1734.2
021800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1734.2
021900 01  CCVS-C-2.                                                    NC1734.2
022000     02 FILLER                     PIC X        VALUE SPACE.      NC1734.2
022100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1734.2
022200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1734.2
022300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1734.2
022400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1734.2
022500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1734.2
022600 01  REC-CT                        PIC 99       VALUE ZERO.       NC1734.2
022700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1734.2
022800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1734.2
022900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1734.2
023000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1734.2
023100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1734.2
023200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1734.2
023300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1734.2
023400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1734.2
023500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1734.2
023600 01  CCVS-H-1.                                                    NC1734.2
023700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1734.2
023800     02  FILLER                    PIC X(42)    VALUE             NC1734.2
023900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1734.2
024000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1734.2
024100 01  CCVS-H-2A.                                                   NC1734.2
024200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1734.2
024300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1734.2
024400   02  FILLER                        PIC XXXX   VALUE             NC1734.2
024500     "4.2 ".                                                      NC1734.2
024600   02  FILLER                        PIC X(28)  VALUE             NC1734.2
024700            " COPY - NOT FOR DISTRIBUTION".                       NC1734.2
024800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1734.2
024900                                                                  NC1734.2
025000 01  CCVS-H-2B.                                                   NC1734.2
025100   02  FILLER                        PIC X(15)  VALUE             NC1734.2
025200            "TEST RESULT OF ".                                    NC1734.2
025300   02  TEST-ID                       PIC X(9).                    NC1734.2
025400   02  FILLER                        PIC X(4)   VALUE             NC1734.2
025500            " IN ".                                               NC1734.2
025600   02  FILLER                        PIC X(12)  VALUE             NC1734.2
025700     " HIGH       ".                                              NC1734.2
025800   02  FILLER                        PIC X(22)  VALUE             NC1734.2
025900            " LEVEL VALIDATION FOR ".                             NC1734.2
026000   02  FILLER                        PIC X(58)  VALUE             NC1734.2
026100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1734.2
026200 01  CCVS-H-3.                                                    NC1734.2
026300     02  FILLER                      PIC X(34)  VALUE             NC1734.2
026400            " FOR OFFICIAL USE ONLY    ".                         NC1734.2
026500     02  FILLER                      PIC X(58)  VALUE             NC1734.2
026600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1734.2
026700     02  FILLER                      PIC X(28)  VALUE             NC1734.2
026800            "  COPYRIGHT   1985 ".                                NC1734.2
026900 01  CCVS-E-1.                                                    NC1734.2
027000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1734.2
027100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1734.2
027200     02 ID-AGAIN                     PIC X(9).                    NC1734.2
027300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1734.2
027400 01  CCVS-E-2.                                                    NC1734.2
027500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1734.2
027600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1734.2
027700     02 CCVS-E-2-2.                                               NC1734.2
027800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1734.2
027900         03 FILLER                   PIC X      VALUE SPACE.      NC1734.2
028000         03 ENDER-DESC               PIC X(44)  VALUE             NC1734.2
028100            "ERRORS ENCOUNTERED".                                 NC1734.2
028200 01  CCVS-E-3.                                                    NC1734.2
028300     02  FILLER                      PIC X(22)  VALUE             NC1734.2
028400            " FOR OFFICIAL USE ONLY".                             NC1734.2
028500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1734.2
028600     02  FILLER                      PIC X(58)  VALUE             NC1734.2
028700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1734.2
028800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1734.2
028900     02 FILLER                       PIC X(15)  VALUE             NC1734.2
029000             " COPYRIGHT 1985".                                   NC1734.2
029100 01  CCVS-E-4.                                                    NC1734.2
029200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1734.2
029300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1734.2
029400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1734.2
029500     02 FILLER                       PIC X(40)  VALUE             NC1734.2
029600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1734.2
029700 01  XXINFO.                                                      NC1734.2
029800     02 FILLER                       PIC X(19)  VALUE             NC1734.2
029900            "*** INFORMATION ***".                                NC1734.2
030000     02 INFO-TEXT.                                                NC1734.2
030100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1734.2
030200       04 XXCOMPUTED                 PIC X(20).                   NC1734.2
030300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1734.2
030400       04 XXCORRECT                  PIC X(20).                   NC1734.2
030500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1734.2
030600 01  HYPHEN-LINE.                                                 NC1734.2
030700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1734.2
030800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1734.2
030900-    "*****************************************".                 NC1734.2
031000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1734.2
031100-    "******************************".                            NC1734.2
031200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1734.2
031300     "NC173A".                                                    NC1734.2
031400 PROCEDURE DIVISION.                                              NC1734.2
031500 CCVS1 SECTION.                                                   NC1734.2
031600 OPEN-FILES.                                                      NC1734.2
031700     OPEN     OUTPUT PRINT-FILE.                                  NC1734.2
031800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1734.2
031900     MOVE    SPACE TO TEST-RESULTS.                               NC1734.2
032000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1734.2
032100     GO TO CCVS1-EXIT.                                            NC1734.2
032200 CLOSE-FILES.                                                     NC1734.2
032300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1734.2
032400 TERMINATE-CCVS.                                                  NC1734.2
032500*    EXIT PROGRAM.                                                NC1734.2
032600*TERMINATE-CALL.                                                  NC1734.2
032700     STOP     RUN.                                                NC1734.2
032800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1734.2
032900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1734.2
033000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1734.2
033100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1734.2
033200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1734.2
033300 PRINT-DETAIL.                                                    NC1734.2
033400     IF REC-CT NOT EQUAL TO ZERO                                  NC1734.2
033500             MOVE "." TO PARDOT-X                                 NC1734.2
033600             MOVE REC-CT TO DOTVALUE.                             NC1734.2
033700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1734.2
033800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1734.2
033900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1734.2
034000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1734.2
034100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1734.2
034200     MOVE SPACE TO CORRECT-X.                                     NC1734.2
034300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1734.2
034400     MOVE     SPACE TO RE-MARK.                                   NC1734.2
034500 HEAD-ROUTINE.                                                    NC1734.2
034600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1734.2
034700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1734.2
034800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1734.2
034900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1734.2
035000 COLUMN-NAMES-ROUTINE.                                            NC1734.2
035100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1734.2
035200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1734.2
035300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1734.2
035400 END-ROUTINE.                                                     NC1734.2
035500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1734.2
035600 END-RTN-EXIT.                                                    NC1734.2
035700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1734.2
035800 END-ROUTINE-1.                                                   NC1734.2
035900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1734.2
036000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1734.2
036100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1734.2
036200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1734.2
036300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1734.2
036400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1734.2
036500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1734.2
036600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1734.2
036700  END-ROUTINE-12.                                                 NC1734.2
036800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1734.2
036900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1734.2
037000         MOVE "NO " TO ERROR-TOTAL                                NC1734.2
037100         ELSE                                                     NC1734.2
037200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1734.2
037300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1734.2
037400     PERFORM WRITE-LINE.                                          NC1734.2
037500 END-ROUTINE-13.                                                  NC1734.2
037600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1734.2
037700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1734.2
037800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1734.2
037900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1734.2
038000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1734.2
038100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1734.2
038200          MOVE "NO " TO ERROR-TOTAL                               NC1734.2
038300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1734.2
038400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1734.2
038500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1734.2
038600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1734.2
038700 WRITE-LINE.                                                      NC1734.2
038800     ADD 1 TO RECORD-COUNT.                                       NC1734.2
038900*    IF RECORD-COUNT GREATER 42                                   NC1734.2
039000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1734.2
039100*        MOVE SPACE TO DUMMY-RECORD                               NC1734.2
039200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1734.2
039300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1734.2
039400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1734.2
039500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1734.2
039600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1734.2
039700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1734.2
039800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1734.2
039900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1734.2
040000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1734.2
040100*        MOVE ZERO TO RECORD-COUNT.                               NC1734.2
040200     PERFORM WRT-LN.                                              NC1734.2
040300 WRT-LN.                                                          NC1734.2
040400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1734.2
040500     MOVE SPACE TO DUMMY-RECORD.                                  NC1734.2
040600 BLANK-LINE-PRINT.                                                NC1734.2
040700     PERFORM WRT-LN.                                              NC1734.2
040800 FAIL-ROUTINE.                                                    NC1734.2
040900     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1734.2
041000            GO TO FAIL-ROUTINE-WRITE.                             NC1734.2
041100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1734.2
041200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1734.2
041300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1734.2
041400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1734.2
041500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1734.2
041600     GO TO  FAIL-ROUTINE-EX.                                      NC1734.2
041700 FAIL-ROUTINE-WRITE.                                              NC1734.2
041800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1734.2
041900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1734.2
042000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1734.2
042100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1734.2
042200 FAIL-ROUTINE-EX. EXIT.                                           NC1734.2
042300 BAIL-OUT.                                                        NC1734.2
042400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1734.2
042500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1734.2
042600 BAIL-OUT-WRITE.                                                  NC1734.2
042700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1734.2
042800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1734.2
042900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1734.2
043000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1734.2
043100 BAIL-OUT-EX. EXIT.                                               NC1734.2
043200 CCVS1-EXIT.                                                      NC1734.2
043300     EXIT.                                                        NC1734.2
043400 SECT-NC173A-001 SECTION.                                         NC1734.2
043500 DIV-INIT-F3-1.                                                   NC1734.2
043600     MOVE    "DIVIDE BY GIVING" TO FEATURE.                       NC1734.2
043700     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
043800     MOVE     44.1 TO DIV2.                                       NC1734.2
043900     MOVE     0    TO DIV8.                                       NC1734.2
044000 DIV-TEST-F3-1-0.                                                 NC1734.2
044100     DIVIDE   864.36 BY DIV2 GIVING DIV8.                         NC1734.2
044200 DIV-TEST-F3-1.                                                   NC1734.2
044300     IF       DIV8 EQUAL TO 19.6                                  NC1734.2
044400              PERFORM PASS                                        NC1734.2
044500              ELSE                                                NC1734.2
044600              GO TO DIV-FAIL-F3-1.                                NC1734.2
044700     GO TO    DIV-WRITE-F3-1.                                     NC1734.2
044800 DIV-DELETE-F3-1.                                                 NC1734.2
044900     PERFORM  DE-LETE.                                            NC1734.2
045000     GO TO    DIV-WRITE-F3-1.                                     NC1734.2
045100 DIV-FAIL-F3-1.                                                   NC1734.2
045200     PERFORM  FAIL.                                               NC1734.2
045300     MOVE     DIV8 TO COMPUTED-N.                                 NC1734.2
045400     MOVE     19.6 TO CORRECT-N.                                  NC1734.2
045500 DIV-WRITE-F3-1.                                                  NC1734.2
045600     MOVE "DIV-TEST-F3-1" TO PAR-NAME.                            NC1734.2
045700     PERFORM  PRINT-DETAIL.                                       NC1734.2
045800 DIV-INIT-F3-2.                                                   NC1734.2
045900     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
046000     MOVE     0    TO DIV9.                                       NC1734.2
046100 DIV-TEST-F3-2.                                                   NC1734.2
046200     DIVIDE   DIV1 BY 0.533 GIVING DIV9 ROUNDED.                  NC1734.2
046300     IF       DIV9 EQUAL TO " 3,040.1"                            NC1734.2
046400              PERFORM PASS                                        NC1734.2
046500              ELSE                                                NC1734.2
046600              GO TO DIV-FAIL-F3-2.                                NC1734.2
046700     GO TO    DIV-WRITE-F3-2.                                     NC1734.2
046800 DIV-DELETE-F3-2.                                                 NC1734.2
046900     PERFORM  DE-LETE.                                            NC1734.2
047000     GO TO    DIV-WRITE-F3-2.                                     NC1734.2
047100 DIV-FAIL-F3-2.                                                   NC1734.2
047200     PERFORM  FAIL.                                               NC1734.2
047300     MOVE     DIV9 TO COMPUTED-A.                                 NC1734.2
047400     MOVE    " 3,040.1" TO CORRECT-A.                             NC1734.2
047500 DIV-WRITE-F3-2.                                                  NC1734.2
047600     MOVE    "DIV-TEST-F3-2" TO PAR-NAME.                         NC1734.2
047700     PERFORM  PRINT-DETAIL.                                       NC1734.2
047800 DIV-INIT-F3-3.                                                   NC1734.2
047900     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
048000     MOVE     44.1 TO DIV2.                                       NC1734.2
048100     MOVE   -9.642 TO DIV4.                                       NC1734.2
048200     MOVE     0    TO DIV10.                                      NC1734.2
048300     MOVE     1    TO REC-CT.                                     NC1734.2
048400 DIV-TEST-F3-3-0.                                                 NC1734.2
048500     DIVIDE  DIV2 BY DIV4 GIVING DIV10 ON SIZE ERROR              NC1734.2
048600             MOVE "P" TO XRAY.                                    NC1734.2
048700     GO TO DIV-TEST-F3-3-1.                                       NC1734.2
048800 DIV-DELETE-F3-3-1.                                               NC1734.2
048900     PERFORM DE-LETE.                                             NC1734.2
049000     PERFORM PRINT-DETAIL.                                        NC1734.2
049100     GO TO   DIV-INIT-F3-4.                                       NC1734.2
049200 DIV-TEST-F3-3-1.                                                 NC1734.2
049300     MOVE   "DIV-TEST-F3-3-1" TO PAR-NAME.                        NC1734.2
049400     IF      XRAY   = "P"                                         NC1734.2
049500             PERFORM PASS                                         NC1734.2
049600             PERFORM PRINT-DETAIL                                 NC1734.2
049700     ELSE                                                         NC1734.2
049800             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
049900             MOVE   "P"    TO CORRECT-X                           NC1734.2
050000             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
050100             PERFORM FAIL                                         NC1734.2
050200             PERFORM PRINT-DETAIL.                                NC1734.2
050300     ADD     1 TO REC-CT.                                         NC1734.2
050400 DIV-TEST-F3-3-2.                                                 NC1734.2
050500     MOVE   "DIV-TEST-F3-3-2" TO PAR-NAME.                        NC1734.2
050600     IF      DIV10   = 0                                          NC1734.2
050700             PERFORM PASS                                         NC1734.2
050800             PERFORM PRINT-DETAIL                                 NC1734.2
050900     ELSE                                                         NC1734.2
051000             MOVE    DIV10 TO COMPUTED-N                          NC1734.2
051100             MOVE    0     TO CORRECT-N                           NC1734.2
051200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
051300             PERFORM FAIL                                         NC1734.2
051400             PERFORM PRINT-DETAIL.                                NC1734.2
051500 DIV-INIT-F3-4.                                                   NC1734.2
051600     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
051700     MOVE     0    TO DIV8.                                       NC1734.2
051800     MOVE     1    TO REC-CT.                                     NC1734.2
051900 DIV-TEST-F3-4-0.                                                 NC1734.2
052000     DIVIDE  100.50 BY 1.0051 GIVING DIV8 ROUNDED ON SIZE ERROR   NC1734.2
052100             MOVE "Q" TO XRAY.                                    NC1734.2
052200     GO TO DIV-TEST-F3-4-1.                                       NC1734.2
052300 DIV-DELETE-F3-4.                                                 NC1734.2
052400     PERFORM DE-LETE.                                             NC1734.2
052500     PERFORM PRINT-DETAIL.                                        NC1734.2
052600     GO TO   DIV-INIT-F3-5.                                       NC1734.2
052700 DIV-TEST-F3-4-1.                                                 NC1734.2
052800     MOVE   "DIV-TEST-F3-4-1" TO PAR-NAME.                        NC1734.2
052900     IF      XRAY    = "Q"                                        NC1734.2
053000             PERFORM PASS                                         NC1734.2
053100             PERFORM PRINT-DETAIL                                 NC1734.2
053200     ELSE                                                         NC1734.2
053300             MOVE    XRAY TO COMPUTED-X                           NC1734.2
053400             MOVE   "Q"   TO CORRECT-X                            NC1734.2
053500             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
053600             PERFORM FAIL                                         NC1734.2
053700             PERFORM PRINT-DETAIL.                                NC1734.2
053800     ADD     1 TO REC-CT.                                         NC1734.2
053900 DIV-TEST-F3-4-2.                                                 NC1734.2
054000     MOVE   "DIV-TEST-F3-4-2" TO PAR-NAME.                        NC1734.2
054100     IF      DIV8    =  0                                         NC1734.2
054200             PERFORM PASS                                         NC1734.2
054300             PERFORM PRINT-DETAIL                                 NC1734.2
054400     ELSE                                                         NC1734.2
054500             MOVE    DIV8 TO COMPUTED-N                           NC1734.2
054600             MOVE    0    TO CORRECT-N                            NC1734.2
054700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
054800             PERFORM FAIL                                         NC1734.2
054900             PERFORM PRINT-DETAIL.                                NC1734.2
055000 DIV-INIT-F3-5.                                                   NC1734.2
055100     MOVE   "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.               NC1734.2
055200     MOVE    ZERO TO REC-CT.                                      NC1734.2
055300     MOVE    ZERO TO WRK-DS-01V00.                                NC1734.2
055400 DIV-TEST-F3-5-0.                                                 NC1734.2
055500     DIVIDE A02TWOS-DU-02V00 BY -10.9 GIVING WRK-DS-01V00.        NC1734.2
055600 DIV-TEST-F3-5-1.                                                 NC1734.2
055700     IF WRK-DS-01V00 EQUAL TO -2                                  NC1734.2
055800         PERFORM PASS                                             NC1734.2
055900         GO TO DIV-WRITE-F3-5.                                    NC1734.2
056000     GO TO DIV-FAIL-F3-5.                                         NC1734.2
056100 DIV-DELETE-F3-5.                                                 NC1734.2
056200     PERFORM DE-LETE.                                             NC1734.2
056300     GO TO DIV-WRITE-F3-5.                                        NC1734.2
056400 DIV-FAIL-F3-5.                                                   NC1734.2
056500     MOVE  -2                 TO CORRECT-N.                       NC1734.2
056600     MOVE  WRK-DS-01V00       TO COMPUTED-N.                      NC1734.2
056700     PERFORM FAIL.                                                NC1734.2
056800 DIV-WRITE-F3-5.                                                  NC1734.2
056900     MOVE "DIV-TEST-F3-5         " TO PAR-NAME.                   NC1734.2
057000     PERFORM PRINT-DETAIL.                                        NC1734.2
057100 DIV-INIT-F3-6.                                                   NC1734.2
057200     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
057300     MOVE 0.0000000001 TO WRK-DS-03V10.                           NC1734.2
057400     MOVE ZERO TO WRK-DS-18V00.                                   NC1734.2
057500 DIV-TEST-F3-6-0.                                                 NC1734.2
057600     DIVIDE A01ONE-DS-P0801 BY WRK-DS-03V10 GIVING                NC1734.2
057700         WRK-DS-18V00 ROUNDED.                                    NC1734.2
057800 DIV-TEST-F3-6-1.                                                 NC1734.2
057900     IF WRK-DS-18V00 EQUAL TO 000000000000000010                  NC1734.2
058000         PERFORM PASS                                             NC1734.2
058100         GO TO DIV-WRITE-F3-6.                                    NC1734.2
058200     GO TO DIV-FAIL-F3-6.                                         NC1734.2
058300 DIV-DELETE-F3-6.                                                 NC1734.2
058400     PERFORM DE-LETE.                                             NC1734.2
058500     GO TO DIV-WRITE-F3-6.                                        NC1734.2
058600 DIV-FAIL-F3-6.                                                   NC1734.2
058700     MOVE  000000000000000010 TO CORRECT-18V0.                    NC1734.2
058800     MOVE  WRK-DS-18V00       TO COMPUTED-18V0.                   NC1734.2
058900     PERFORM FAIL.                                                NC1734.2
059000 DIV-WRITE-F3-6.                                                  NC1734.2
059100     MOVE "DIV-TEST-F3-6         " TO PAR-NAME.                   NC1734.2
059200     PERFORM PRINT-DETAIL.                                        NC1734.2
059300 DIV-INIT-F3-7.                                                   NC1734.2
059400     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
059500     MOVE ZERO TO WRK-DS-18V00.                                   NC1734.2
059600     MOVE "0" TO WRK-XN-00001.                                    NC1734.2
059700     MOVE    1    TO REC-CT.                                      NC1734.2
059800 DIV-TEST-F3-7-0.                                                 NC1734.2
059900     DIVIDE A99-DS-02V00 BY AZERO-DS-05V05 GIVING                 NC1734.2
060000         WRK-DS-18V00 ON SIZE ERROR MOVE "1" TO WRK-XN-00001.     NC1734.2
060100 DIV-TEST-F3-7-1.                                                 NC1734.2
060200     IF WRK-DS-18V00 EQUAL TO 000000000000000000                  NC1734.2
060300         PERFORM PASS                                             NC1734.2
060400         GO TO DIV-WRITE-F3-7-1.                                  NC1734.2
060500     MOVE  000000000000000000 TO CORRECT-18V0.                    NC1734.2
060600     MOVE  WRK-DS-18V00       TO COMPUTED-18V0.                   NC1734.2
060700     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1734.2
060800     PERFORM FAIL.                                                NC1734.2
060900     GO TO DIV-WRITE-F3-7-1.                                      NC1734.2
061000 DIV-DELETE-F3-7-1.                                               NC1734.2
061100     PERFORM DE-LETE.                                             NC1734.2
061200 DIV-WRITE-F3-7-1.                                                NC1734.2
061300     MOVE "DIV-TEST-F3-7-1" TO PAR-NAME.                          NC1734.2
061400     PERFORM PRINT-DETAIL.                                        NC1734.2
061500     ADD 1 TO REC-CT.                                             NC1734.2
061600 DIV-TEST-F3-7-2.                                                 NC1734.2
061700     IF WRK-XN-00001 EQUAL TO "1"                                 NC1734.2
061800         PERFORM PASS                                             NC1734.2
061900         GO TO DIV-WRITE-F3-7-2.                                  NC1734.2
062000     MOVE "1" TO CORRECT-A.                                       NC1734.2
062100     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1734.2
062200     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1734.2
062300     PERFORM FAIL.                                                NC1734.2
062400     GO TO DIV-WRITE-F3-7-2.                                      NC1734.2
062500 DIV-DELETE-F3-7-2.                                               NC1734.2
062600     PERFORM DE-LETE.                                             NC1734.2
062700 DIV-WRITE-F3-7-2.                                                NC1734.2
062800     MOVE "DIV-TEST-F3-7-2         " TO PAR-NAME.                 NC1734.2
062900     PERFORM PRINT-DETAIL.                                        NC1734.2
063000 DIV-INIT-F3-8.                                                   NC1734.2
063100     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
063200     MOVE A18ONES-DS-09V09 TO WRK-DS-09V09.                       NC1734.2
063300     MOVE "1" TO WRK-XN-00001.                                    NC1734.2
063400     MOVE  1  TO REC-CT.                                          NC1734.2
063500 DIV-TEST-F3-8-1.                                                 NC1734.2
063600     DIVIDE WRK-DS-09V09 BY A18ONES-DS-09V09 GIVING               NC1734.2
063700         WRK-DS-09V09 ON SIZE ERROR MOVE "0" TO WRK-XN-00001.     NC1734.2
063800     IF WRK-DS-18V00-S EQUAL TO 000000001000000000                NC1734.2
063900         PERFORM PASS                                             NC1734.2
064000         GO TO DIV-WRITE-F3-8-1.                                  NC1734.2
064100 DIV-FAIL-F3-8-1.                                                 NC1734.2
064200     MOVE  000000001000000000 TO CORRECT-18V0.                    NC1734.2
064300     MOVE  WRK-DS-18V00-S     TO COMPUTED-18V0.                   NC1734.2
064400     PERFORM FAIL.                                                NC1734.2
064500     GO TO DIV-WRITE-F3-8-1.                                      NC1734.2
064600 DIV-DELETE-F3-8-1.                                               NC1734.2
064700     PERFORM DE-LETE.                                             NC1734.2
064800 DIV-WRITE-F3-8-1.                                                NC1734.2
064900     MOVE "DIV-TEST-F3-8-1         " TO PAR-NAME.                 NC1734.2
065000     PERFORM PRINT-DETAIL.                                        NC1734.2
065100     ADD 1 TO REC-CT.                                             NC1734.2
065200 DIV-TEST-F3-8-2.                                                 NC1734.2
065300     IF WRK-XN-00001 EQUAL TO "0"                                 NC1734.2
065400         MOVE WRK-XN-00001 TO COMPUTED-A                          NC1734.2
065500         MOVE "1" TO CORRECT-A                                    NC1734.2
065600         MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1734.2
065700         PERFORM FAIL                                             NC1734.2
065800         GO TO DIV-WRITE-F3-8-2.                                  NC1734.2
065900     PERFORM PASS.                                                NC1734.2
066000     GO TO DIV-WRITE-F3-8-2.                                      NC1734.2
066100 DIV-DELETE-F3-8-2.                                               NC1734.2
066200     PERFORM DE-LETE.                                             NC1734.2
066300 DIV-WRITE-F3-8-2.                                                NC1734.2
066400     MOVE "DIV-TEST-F3-8-2         " TO PAR-NAME.                 NC1734.2
066500     PERFORM PRINT-DETAIL.                                        NC1734.2
066600 DIV-INIT-F3-9.                                                   NC1734.2
066700     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
066800     MOVE ZERO TO WRK-DS-0201P.                                   NC1734.2
066900     MOVE -0.005 TO WRK-DS-09V09.                                 NC1734.2
067000     MOVE "0" TO WRK-XN-00001.                                    NC1734.2
067100     MOVE  1 TO REC-CT.                                           NC1734.2
067200 DIV-TEST-F3-9-1-0.                                               NC1734.2
067300     DIVIDE A05ONES-DS-00V05 BY WRK-DS-09V09 GIVING               NC1734.2
067400         WRK-DS-0201P ROUNDED ON SIZE ERROR                       NC1734.2
067500         MOVE "1" TO WRK-XN-00001.                                NC1734.2
067600 DIV-TEST-F3-9-1-1.                                               NC1734.2
067700     MOVE WRK-DS-0201P TO WRK-DS-05V00.                           NC1734.2
067800     IF  WRK-DS-05V00 EQUAL TO -00020                             NC1734.2
067900         PERFORM PASS                                             NC1734.2
068000         GO TO DIV-WRITE-F3-9-1.                                  NC1734.2
068100     MOVE  -00020             TO CORRECT-N.                       NC1734.2
068200     MOVE  WRK-DS-05V00       TO COMPUTED-N.                      NC1734.2
068300     PERFORM FAIL.                                                NC1734.2
068400     GO TO DIV-WRITE-F3-9-1.                                      NC1734.2
068500 DIV-DELETE-F3-9-1.                                               NC1734.2
068600     PERFORM DE-LETE.                                             NC1734.2
068700 DIV-WRITE-F3-9-1.                                                NC1734.2
068800     MOVE "DIV-TEST-F3-9-1         " TO PAR-NAME.                 NC1734.2
068900     PERFORM PRINT-DETAIL.                                        NC1734.2
069000     ADD 1 TO REC-CT.                                             NC1734.2
069100 DIV-TEST-F3-9-2-1.                                               NC1734.2
069200     IF WRK-XN-00001 EQUAL TO "0"                                 NC1734.2
069300         PERFORM PASS                                             NC1734.2
069400         GO TO DIV-WRITE-F3-9-2.                                  NC1734.2
069500     MOVE "0" TO CORRECT-A.                                       NC1734.2
069600     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1734.2
069700     MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.      NC1734.2
069800     PERFORM FAIL.                                                NC1734.2
069900     GO TO DIV-WRITE-F3-9-2.                                      NC1734.2
070000 DIV-DELETE-F3-9-2.                                               NC1734.2
070100     PERFORM DE-LETE.                                             NC1734.2
070200 DIV-WRITE-F3-9-2.                                                NC1734.2
070300     MOVE "DIV-TEST-F3-9-2         " TO PAR-NAME.                 NC1734.2
070400     PERFORM PRINT-DETAIL.                                        NC1734.2
070500 DIV-INIT-F3-10.                                                  NC1734.2
070600     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
070700     MOVE "1" TO WRK-XN-00001.                                    NC1734.2
070800     MOVE ZERO TO WRK-DS-01V00.                                   NC1734.2
070900     MOVE  1 TO REC-CT.                                           NC1734.2
071000 DIV-TEST-F3-10-0.                                                NC1734.2
071100     DIVIDE A02TWOS-DS-03V02 BY A02TWOS-DU-02V00 GIVING           NC1734.2
071200         WRK-DS-01V00 ROUNDED ON SIZE ERROR                       NC1734.2
071300         MOVE "0" TO WRK-XN-00001.                                NC1734.2
071400 DIV-TEST-F3-10-1.                                                NC1734.2
071500     IF WRK-DS-01V00 EQUAL TO +1                                  NC1734.2
071600         PERFORM PASS                                             NC1734.2
071700         GO TO DIV-WRITE-F3-10-1.                                 NC1734.2
071800     MOVE  +1                 TO CORRECT-N.                       NC1734.2
071900     MOVE  WRK-DS-01V00       TO COMPUTED-N.                      NC1734.2
072000     PERFORM FAIL.                                                NC1734.2
072100     GO TO DIV-WRITE-F3-10-1.                                     NC1734.2
072200 DIV-DELETE-F3-10-1.                                              NC1734.2
072300     PERFORM DE-LETE.                                             NC1734.2
072400 DIV-WRITE-F3-10-1.                                               NC1734.2
072500     MOVE "DIV-TEST-F3-10-1" TO PAR-NAME.                         NC1734.2
072600     PERFORM PRINT-DETAIL.                                        NC1734.2
072700     ADD 1 TO REC-CT.                                             NC1734.2
072800 DIV-TEST-F3-10-2.                                                NC1734.2
072900     IF WRK-XN-00001 EQUAL TO "0"                                 NC1734.2
073000         MOVE "0" TO COMPUTED-A                                   NC1734.2
073100         MOVE "1" TO CORRECT-A                                    NC1734.2
073200         MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1734.2
073300         PERFORM FAIL                                             NC1734.2
073400        GO TO DIV-WRITE-F3-10-2.                                  NC1734.2
073500     PERFORM PASS.                                                NC1734.2
073600     GO TO DIV-WRITE-F3-10-2.                                     NC1734.2
073700 DIV-DELETE-F3-10-2.                                              NC1734.2
073800     PERFORM DE-LETE.                                             NC1734.2
073900 DIV-WRITE-F3-10-2.                                               NC1734.2
074000     MOVE "DIV-TEST-F3-10-2         " TO PAR-NAME.                NC1734.2
074100     PERFORM PRINT-DETAIL.                                        NC1734.2
074200 DIV-INIT-F3-11.                                                  NC1734.2
074300     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
074400     MOVE    0 TO WRK-DS-05V00.                                   NC1734.2
074500 DIV-TEST-F3-11-0.                                                NC1734.2
074600     DIVIDE A99-CS-02V00 BY A01ONE-CS-00V01 GIVING                NC1734.2
074700         WRK-DS-05V00.                                            NC1734.2
074800 DIV-TEST-F3-11-1.                                                NC1734.2
074900     IF WRK-DS-05V00 EQUAL TO 00990                               NC1734.2
075000         PERFORM PASS                                             NC1734.2
075100         GO TO DIV-WRITE-F3-11.                                   NC1734.2
075200     GO TO DIV-FAIL-F3-11.                                        NC1734.2
075300 DIV-DELETE-F3-11.                                                NC1734.2
075400     PERFORM DE-LETE.                                             NC1734.2
075500     GO TO DIV-WRITE-F3-11.                                       NC1734.2
075600 DIV-FAIL-F3-11.                                                  NC1734.2
075700     MOVE  00990              TO CORRECT-N.                       NC1734.2
075800     MOVE  WRK-DS-05V00       TO COMPUTED-N.                      NC1734.2
075900     PERFORM FAIL.                                                NC1734.2
076000 DIV-WRITE-F3-11.                                                 NC1734.2
076100     MOVE "DIV-TEST-F3-11         " TO PAR-NAME.                  NC1734.2
076200     PERFORM PRINT-DETAIL.                                        NC1734.2
076300 DIV-INIT-F3-12.                                                  NC1734.2
076400     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
076500     MOVE ZERO TO WRK-CS-18V00.                                   NC1734.2
076600 DIV-TEST-F3-12-0.                                                NC1734.2
076700     DIVIDE A16NINES-CU-18V00 BY A02THREES-CS-18V00               NC1734.2
076800             GIVING WRK-CS-18V00.                                 NC1734.2
076900 DIV-TEST-F3-12-1.                                                NC1734.2
077000     IF WRK-CS-18V00 EQUAL TO -000303030303030303                 NC1734.2
077100         PERFORM PASS                                             NC1734.2
077200         GO TO DIV-WRITE-F3-12.                                   NC1734.2
077300     MOVE -00303030303030303 TO CORRECT-18V0.                     NC1734.2
077400     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1734.2
077500     PERFORM FAIL.                                                NC1734.2
077600     GO TO DIV-WRITE-F3-12.                                       NC1734.2
077700 DIV-DELETE-F3-12.                                                NC1734.2
077800     PERFORM DE-LETE.                                             NC1734.2
077900 DIV-WRITE-F3-12.                                                 NC1734.2
078000     MOVE "DIV-TEST-F3-12 " TO PAR-NAME.                          NC1734.2
078100     PERFORM PRINT-DETAIL.                                        NC1734.2
078200 DIV-INIT-F3-13.                                                  NC1734.2
078300     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
078400     MOVE ZERO TO WRK-DU-18V00.                                   NC1734.2
078500 DIV-TEST-F3-13-0.                                                NC1734.2
078600     DIVIDE A18SIXES-CU-18V00 BY A14TWOS-CU-18V00 GIVING          NC1734.2
078700             WRK-DU-18V00.                                        NC1734.2
078800 DIV-TEST-F3-13-1.                                                NC1734.2
078900     IF WRK-DU-18V00 EQUAL TO 000000000000030000                  NC1734.2
079000         PERFORM PASS                                             NC1734.2
079100         GO TO DIV-WRITE-F3-13.                                   NC1734.2
079200     MOVE 000000000000030000 TO CORRECT-18V0.                     NC1734.2
079300     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1734.2
079400     PERFORM FAIL.                                                NC1734.2
079500     GO TO DIV-WRITE-F3-13.                                       NC1734.2
079600 DIV-DELETE-F3-13.                                                NC1734.2
079700     PERFORM DE-LETE.                                             NC1734.2
079800 DIV-WRITE-F3-13.                                                 NC1734.2
079900     MOVE "DIV-TEST-F3-13 " TO PAR-NAME.                          NC1734.2
080000     PERFORM PRINT-DETAIL.                                        NC1734.2
080100 DIV-INIT-F3-14.                                                  NC1734.2
080200     MOVE    "V1-82 6.11.4 GR3"   TO ANSI-REFERENCE.              NC1734.2
080300     MOVE ZERO TO WRK-CS-18V00.                                   NC1734.2
080400 DIV-TEST-F3-14-0.                                                NC1734.2
080500     DIVIDE A02THREES-CS-18V00 BY A01ONES-CS-18V00 GIVING         NC1734.2
080600             WRK-CS-18V00 ROUNDED.                                NC1734.2
080700 DIV-TEST-F3-14-1.                                                NC1734.2
080800     IF WRK-CS-18V00 EQUAL TO -000000000000000033                 NC1734.2
080900         PERFORM PASS                                             NC1734.2
081000         GO TO DIV-WRITE-F3-14.                                   NC1734.2
081100     MOVE -000000000000000033 TO CORRECT-18V0.                    NC1734.2
081200     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1734.2
081300     PERFORM FAIL.                                                NC1734.2
081400     GO TO DIV-WRITE-F3-14.                                       NC1734.2
081500 DIV-DELETE-F3-14.                                                NC1734.2
081600     PERFORM DE-LETE.                                             NC1734.2
081700 DIV-WRITE-F3-14.                                                 NC1734.2
081800     MOVE "DIV-TEST-F3-14 " TO PAR-NAME.                          NC1734.2
081900     PERFORM PRINT-DETAIL.                                        NC1734.2
082000*                                                                 NC1734.2
082100*                                                                 NC1734.2
082200 DIV-INIT-F3-15.                                                  NC1734.2
082300*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
082400*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
082500     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1734.2
082600     MOVE   "DIV-TEST-F3-15" TO PAR-NAME.                         NC1734.2
082700     MOVE    44.1  TO DIV2.                                       NC1734.2
082800     MOVE   -9.642 TO DIV4.                                       NC1734.2
082900     MOVE    0     TO DIV10.                                      NC1734.2
083000     MOVE    1     TO REC-CT.                                     NC1734.2
083100     MOVE   "A"    TO XRAY.                                       NC1734.2
083200 DIV-TEST-F3-15-0.                                                NC1734.2
083300     DIVIDE  DIV2 BY DIV4                                         NC1734.2
083400      GIVING DIV10                                                NC1734.2
083500      NOT ON SIZE ERROR                                           NC1734.2
083600             MOVE "P" TO XRAY.                                    NC1734.2
083700     GO TO   DIV-TEST-F3-15-1.                                    NC1734.2
083800 DIV-DELETE-F3-15-1.                                              NC1734.2
083900     PERFORM DE-LETE.                                             NC1734.2
084000     PERFORM PRINT-DETAIL.                                        NC1734.2
084100     GO TO   DIV-INIT-F3-16.                                      NC1734.2
084200 DIV-TEST-F3-15-1.                                                NC1734.2
084300     MOVE   "DIV-TEST-F3-15-1" TO PAR-NAME.                       NC1734.2
084400     IF      XRAY    = "A"                                        NC1734.2
084500             PERFORM PASS                                         NC1734.2
084600             PERFORM PRINT-DETAIL                                 NC1734.2
084700     ELSE                                                         NC1734.2
084800             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
084900             MOVE   "A"    TO CORRECT-X                           NC1734.2
085000             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
085100             PERFORM FAIL                                         NC1734.2
085200             PERFORM PRINT-DETAIL.                                NC1734.2
085300     ADD     1     TO REC-CT.                                     NC1734.2
085400 DIV-TEST-F3-15-2.                                                NC1734.2
085500     MOVE   "DIV-TEST-F3-15-2" TO PAR-NAME.                       NC1734.2
085600     IF      DIV10   = 0                                          NC1734.2
085700             PERFORM PASS                                         NC1734.2
085800             PERFORM PRINT-DETAIL                                 NC1734.2
085900     ELSE                                                         NC1734.2
086000             MOVE    DIV10 TO COMPUTED-N                          NC1734.2
086100             MOVE    0     TO CORRECT-N                           NC1734.2
086200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
086300             PERFORM FAIL                                         NC1734.2
086400             PERFORM PRINT-DETAIL.                                NC1734.2
086500*                                                                 NC1734.2
086600*                                                                 NC1734.2
086700 DIV-INIT-F3-16.                                                  NC1734.2
086800     MOVE   "DIV-TEST-F3-16" TO PAR-NAME.                         NC1734.2
086900     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1734.2
087000*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
087100*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
087200     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1734.2
087300     MOVE    1     TO REC-CT.                                     NC1734.2
087400     MOVE   "1" TO WRK-XN-00001.                                  NC1734.2
087500 DIV-TEST-F3-16-0.                                                NC1734.2
087600     DIVIDE  WRK-DS-09V09 BY A18ONES-DS-09V09                     NC1734.2
087700      GIVING WRK-DS-09V09                                         NC1734.2
087800      NOT ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                 NC1734.2
087900     GO TO   DIV-TEST-F3-16-1.                                    NC1734.2
088000 DIV-DELETE-F3-16-1.                                              NC1734.2
088100     PERFORM DE-LETE.                                             NC1734.2
088200     PERFORM PRINT-DETAIL.                                        NC1734.2
088300     GO TO   DIV-INIT-F3-17.                                      NC1734.2
088400 DIV-TEST-F3-16-1.                                                NC1734.2
088500     MOVE   "DIV-TEST-F3-16-1" TO PAR-NAME.                       NC1734.2
088600     IF      WRK-DS-18V00-S EQUAL TO 000000001000000000           NC1734.2
088700             PERFORM PASS                                         NC1734.2
088800             PERFORM PRINT-DETAIL                                 NC1734.2
088900     ELSE                                                         NC1734.2
089000             MOVE    000000001000000000 TO CORRECT-18V0           NC1734.2
089100             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1734.2
089200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
089300             PERFORM FAIL                                         NC1734.2
089400             PERFORM PRINT-DETAIL.                                NC1734.2
089500     ADD     1     TO REC-CT.                                     NC1734.2
089600 DIV-TEST-F3-16-2.                                                NC1734.2
089700     MOVE   "DIV-TEST-F3-16-2" TO PAR-NAME.                       NC1734.2
089800     IF      WRK-XN-00001 = "0"                                   NC1734.2
089900             PERFORM PASS                                         NC1734.2
090000             PERFORM PRINT-DETAIL                                 NC1734.2
090100     ELSE                                                         NC1734.2
090200             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
090300             MOVE   "0"           TO CORRECT-X                    NC1734.2
090400             MOVE   "NOT ON SIZE ERROR NOT EXECUTED" TO RE-MARK   NC1734.2
090500             PERFORM FAIL                                         NC1734.2
090600             PERFORM PRINT-DETAIL.                                NC1734.2
090700*                                                                 NC1734.2
090800*                                                                 NC1734.2
090900 DIV-INIT-F3-17.                                                  NC1734.2
091000*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
091100*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
091200     MOVE   "DIV-TEST-F3-17" TO PAR-NAME.                         NC1734.2
091300     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1734.2
091400     MOVE    44.1  TO DIV2.                                       NC1734.2
091500     MOVE   -9.642 TO DIV4.                                       NC1734.2
091600     MOVE    0     TO DIV10.                                      NC1734.2
091700     MOVE    1     TO REC-CT.                                     NC1734.2
091800     MOVE   "A"    TO XRAY.                                       NC1734.2
091900 DIV-TEST-F3-17-0.                                                NC1734.2
092000     DIVIDE  DIV2 BY DIV4                                         NC1734.2
092100      GIVING DIV10                                                NC1734.2
092200          ON SIZE ERROR                                           NC1734.2
092300             MOVE "E" TO XRAY                                     NC1734.2
092400      NOT ON SIZE ERROR                                           NC1734.2
092500             MOVE "N" TO XRAY.                                    NC1734.2
092600     GO TO DIV-TEST-F3-17-1.                                      NC1734.2
092700 DIV-DELETE-F3-17-1.                                              NC1734.2
092800     PERFORM DE-LETE.                                             NC1734.2
092900     PERFORM PRINT-DETAIL.                                        NC1734.2
093000     GO TO   DIV-INIT-F3-18.                                      NC1734.2
093100 DIV-TEST-F3-17-1.                                                NC1734.2
093200     MOVE   "DIV-TEST-F3-17-1" TO PAR-NAME.                       NC1734.2
093300     IF      XRAY    = "E"                                        NC1734.2
093400             PERFORM PASS                                         NC1734.2
093500             PERFORM PRINT-DETAIL                                 NC1734.2
093600     ELSE                                                         NC1734.2
093700             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
093800             MOVE   "E"    TO CORRECT-X                           NC1734.2
093900             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
094000             PERFORM FAIL                                         NC1734.2
094100             PERFORM PRINT-DETAIL.                                NC1734.2
094200     ADD     1     TO REC-CT.                                     NC1734.2
094300 DIV-TEST-F3-17-2.                                                NC1734.2
094400     MOVE   "DIV-TEST-F3-17-2" TO PAR-NAME.                       NC1734.2
094500     IF      DIV10   = 0                                          NC1734.2
094600             PERFORM PASS                                         NC1734.2
094700             PERFORM PRINT-DETAIL                                 NC1734.2
094800     ELSE                                                         NC1734.2
094900             MOVE    DIV10 TO COMPUTED-N                          NC1734.2
095000             MOVE    0     TO CORRECT-N                           NC1734.2
095100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
095200             PERFORM FAIL                                         NC1734.2
095300             PERFORM PRINT-DETAIL.                                NC1734.2
095400*                                                                 NC1734.2
095500*                                                                 NC1734.2
095600 DIV-INIT-F3-18.                                                  NC1734.2
095700*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
095800*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
095900     MOVE   "V1-67 6.4.2 "   TO ANSI-REFERENCE.                   NC1734.2
096000     MOVE    1     TO REC-CT.                                     NC1734.2
096100     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1734.2
096200     MOVE   "1" TO WRK-XN-00001.                                  NC1734.2
096300 DIV-TEST-F3-18-0.                                                NC1734.2
096400     DIVIDE  WRK-DS-09V09 BY A18ONES-DS-09V09                     NC1734.2
096500      GIVING WRK-DS-09V09                                         NC1734.2
096600          ON SIZE ERROR                                           NC1734.2
096700             MOVE "1" TO WRK-XN-00001                             NC1734.2
096800      NOT ON SIZE ERROR                                           NC1734.2
096900             MOVE "2" TO WRK-XN-00001.                            NC1734.2
097000     GO TO   DIV-TEST-F3-18-1.                                    NC1734.2
097100 DIV-DELETE-F3-18-1.                                              NC1734.2
097200     PERFORM DE-LETE.                                             NC1734.2
097300     PERFORM PRINT-DETAIL.                                        NC1734.2
097400     GO TO   DIV-INIT-F3-19.                                      NC1734.2
097500 DIV-TEST-F3-18-1.                                                NC1734.2
097600     MOVE   "DIV-TEST-F3-18-1" TO PAR-NAME.                       NC1734.2
097700     IF      WRK-DS-09V09   EQUAL TO 1                            NC1734.2
097800             PERFORM PASS                                         NC1734.2
097900             PERFORM PRINT-DETAIL                                 NC1734.2
098000     ELSE                                                         NC1734.2
098100             MOVE    000000001000000000 TO CORRECT-18V0           NC1734.2
098200             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1734.2
098300             MOVE   "DIV-TEST-F3-18-2" TO PAR-NAME                NC1734.2
098400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
098500             PERFORM FAIL                                         NC1734.2
098600             PERFORM PRINT-DETAIL.                                NC1734.2
098700     MOVE    1     TO REC-CT.                                     NC1734.2
098800 DIV-TEST-F3-18-2.                                                NC1734.2
098900     MOVE   "DIV-TEST-F3-18-2" TO PAR-NAME.                       NC1734.2
099000     IF      WRK-XN-00001 = "2"                                   NC1734.2
099100             PERFORM PASS                                         NC1734.2
099200             PERFORM PRINT-DETAIL                                 NC1734.2
099300     ELSE                                                         NC1734.2
099400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
099500             MOVE   "2"           TO CORRECT-X                    NC1734.2
099600             MOVE   "NOT ON SIZE ERROR NOT EXECUTED" TO RE-MARK   NC1734.2
099700             PERFORM FAIL                                         NC1734.2
099800             PERFORM PRINT-DETAIL.                                NC1734.2
099900*                                                                 NC1734.2
100000*                                                                 NC1734.2
100100 DIV-INIT-F3-19.                                                  NC1734.2
100200     MOVE   "DIV-TEST-F3-19"     TO PAR-NAME.                     NC1734.2
100300*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
100400*            ==-->  MULTIPLE RESULT FIELDS <--==                  NC1734.2
100500     MOVE   "V1-82 6.11.4 GR3" TO ANSI-REFERENCE.                 NC1734.2
100600     MOVE    0 TO WRK-DU-2V1-1.                                   NC1734.2
100700     MOVE    0 TO WRK-DU-2V1-2.                                   NC1734.2
100800     MOVE    0 TO WRK-DU-2V0-2.                                   NC1734.2
100900     MOVE    0 TO WRK-DU-2V1-3.                                   NC1734.2
101000     MOVE    0 TO WRK-DU-2V0-3.                                   NC1734.2
101100     MOVE    1 TO REC-CT.                                         NC1734.2
101200     MOVE   10 TO WRK-DU-2V0-1.                                   NC1734.2
101300     MOVE  3.9 TO WRK-DU-1V1-2.                                   NC1734.2
101400 DIV-TEST-F3-19-0.                                                NC1734.2
101500     DIVIDE  WRK-DU-2V0-1 BY WRK-DU-1V1-2                         NC1734.2
101600      GIVING WRK-DU-2V1-1                                         NC1734.2
101700             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
101800             WRK-DU-2V1-2                                         NC1734.2
101900             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
102000             WRK-DU-2V1-3                                         NC1734.2
102100             WRK-DU-2V0-3.                                        NC1734.2
102200     GO TO DIV-TEST-F3-19-1.                                      NC1734.2
102300 DIV-DELETE-F3-19.                                                NC1734.2
102400     PERFORM DE-LETE.                                             NC1734.2
102500     PERFORM PRINT-DETAIL.                                        NC1734.2
102600     GO TO   DIV-INIT-F3-20.                                      NC1734.2
102700 DIV-TEST-F3-19-1.                                                NC1734.2
102800     IF      WRK-DU-2V1-1 = 2.5                                   NC1734.2
102900             PERFORM PASS                                         NC1734.2
103000             PERFORM PRINT-DETAIL                                 NC1734.2
103100     ELSE                                                         NC1734.2
103200             PERFORM FAIL                                         NC1734.2
103300             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
103400             MOVE    2.5          TO CORRECT-N                    NC1734.2
103500             PERFORM PRINT-DETAIL.                                NC1734.2
103600     ADD 1 TO REC-CT.                                             NC1734.2
103700 DIV-TEST-F3-19-2.                                                NC1734.2
103800     IF      WRK-DU-2V0-1 = 3                                     NC1734.2
103900             PERFORM PASS                                         NC1734.2
104000             PERFORM PRINT-DETAIL                                 NC1734.2
104100     ELSE                                                         NC1734.2
104200             PERFORM FAIL                                         NC1734.2
104300             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
104400             MOVE    3            TO CORRECT-N                    NC1734.2
104500             PERFORM PRINT-DETAIL.                                NC1734.2
104600     ADD 1 TO REC-CT.                                             NC1734.2
104700 DIV-TEST-F3-19-3.                                                NC1734.2
104800     IF      WRK-DU-2V1-2 = 2.5                                   NC1734.2
104900             PERFORM PASS                                         NC1734.2
105000             PERFORM PRINT-DETAIL                                 NC1734.2
105100     ELSE                                                         NC1734.2
105200             PERFORM FAIL                                         NC1734.2
105300             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
105400             MOVE    2.5          TO CORRECT-N                    NC1734.2
105500             PERFORM PRINT-DETAIL.                                NC1734.2
105600     ADD 1 TO REC-CT.                                             NC1734.2
105700 DIV-TEST-F3-19-4.                                                NC1734.2
105800     IF      WRK-DU-2V0-2 = 3                                     NC1734.2
105900             PERFORM PASS                                         NC1734.2
106000             PERFORM PRINT-DETAIL                                 NC1734.2
106100     ELSE                                                         NC1734.2
106200             PERFORM FAIL                                         NC1734.2
106300             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
106400             MOVE    3            TO CORRECT-N                    NC1734.2
106500             PERFORM PRINT-DETAIL.                                NC1734.2
106600     ADD 1 TO REC-CT.                                             NC1734.2
106700 DIV-TEST-F3-19-5.                                                NC1734.2
106800     IF      WRK-DU-2V1-3 = 2.5                                   NC1734.2
106900             PERFORM PASS                                         NC1734.2
107000             PERFORM PRINT-DETAIL                                 NC1734.2
107100     ELSE                                                         NC1734.2
107200             PERFORM FAIL                                         NC1734.2
107300             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
107400             MOVE    2.5          TO CORRECT-N                    NC1734.2
107500             PERFORM PRINT-DETAIL.                                NC1734.2
107600     ADD 1 TO REC-CT.                                             NC1734.2
107700 DIV-TEST-F3-19-6.                                                NC1734.2
107800     IF      WRK-DU-2V0-3 = 2                                     NC1734.2
107900             PERFORM PASS                                         NC1734.2
108000             PERFORM PRINT-DETAIL                                 NC1734.2
108100     ELSE                                                         NC1734.2
108200             PERFORM FAIL                                         NC1734.2
108300             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
108400             MOVE    2            TO CORRECT-N                    NC1734.2
108500             PERFORM PRINT-DETAIL.                                NC1734.2
108600*                                                                 NC1734.2
108700*                                                                 NC1734.2
108800 DIV-INIT-F3-20.                                                  NC1734.2
108900*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
109000*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
109100     MOVE   "DIV-TEST-F3-20" TO PAR-NAME.                         NC1734.2
109200     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1734.2
109300     MOVE   "0"  TO WRK-XN-00001.                                 NC1734.2
109400     MOVE    0   TO WRK-DU-2V1-1.                                 NC1734.2
109500     MOVE    0   TO WRK-DU-2V0-1.                                 NC1734.2
109600     MOVE    0   TO WRK-DU-2V1-2.                                 NC1734.2
109700     MOVE    0   TO WRK-DU-2V0-2.                                 NC1734.2
109800     MOVE    0   TO WRK-DU-2V1-3.                                 NC1734.2
109900     MOVE    0   TO WRK-DU-2V0-3.                                 NC1734.2
110000     MOVE    1   TO REC-CT.                                       NC1734.2
110100     MOVE    99  TO WRK-DU-2V0-1.                                 NC1734.2
110200 DIV-TEST-F3-20-0.                                                NC1734.2
110300     DIVIDE  WRK-DU-2V0-1 BY A01ONE-CS-00V01                      NC1734.2
110400      GIVING WRK-DU-2V1-1                                         NC1734.2
110500             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
110600             WRK-DU-2V1-2                                         NC1734.2
110700             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
110800             WRK-DU-2V1-3                                         NC1734.2
110900             WRK-DU-2V0-3                                         NC1734.2
111000          ON SIZE ERROR                                           NC1734.2
111100             MOVE  "1" TO WRK-XN-00001.                           NC1734.2
111200     GO TO DIV-TEST-F3-20-1.                                      NC1734.2
111300 DIV-DELETE-F3-20.                                                NC1734.2
111400     PERFORM DE-LETE.                                             NC1734.2
111500     PERFORM PRINT-DETAIL.                                        NC1734.2
111600     GO TO   DIV-INIT-F3-21.                                      NC1734.2
111700 DIV-TEST-F3-20-1.                                                NC1734.2
111800     IF      WRK-DU-2V1-1 = 0                                     NC1734.2
111900             PERFORM PASS                                         NC1734.2
112000             PERFORM PRINT-DETAIL                                 NC1734.2
112100     ELSE                                                         NC1734.2
112200             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
112300             PERFORM FAIL                                         NC1734.2
112400             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
112500             MOVE    0            TO CORRECT-N                    NC1734.2
112600             PERFORM PRINT-DETAIL.                                NC1734.2
112700     ADD 1 TO REC-CT.                                             NC1734.2
112800 DIV-TEST-F3-20-2.                                                NC1734.2
112900     IF      WRK-DU-2V0-1 = 99                                    NC1734.2
113000             PERFORM PASS                                         NC1734.2
113100             PERFORM PRINT-DETAIL                                 NC1734.2
113200     ELSE                                                         NC1734.2
113300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
113400             PERFORM FAIL                                         NC1734.2
113500             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
113600             MOVE    99           TO CORRECT-N                    NC1734.2
113700             PERFORM PRINT-DETAIL.                                NC1734.2
113800     ADD 1 TO REC-CT.                                             NC1734.2
113900 DIV-TEST-F3-20-3.                                                NC1734.2
114000     IF      WRK-DU-2V1-2 = 0                                     NC1734.2
114100             PERFORM PASS                                         NC1734.2
114200             PERFORM PRINT-DETAIL                                 NC1734.2
114300     ELSE                                                         NC1734.2
114400             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
114500             PERFORM FAIL                                         NC1734.2
114600             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
114700             MOVE    0            TO CORRECT-N                    NC1734.2
114800             PERFORM PRINT-DETAIL.                                NC1734.2
114900     ADD 1 TO REC-CT.                                             NC1734.2
115000 DIV-TEST-F3-20-4.                                                NC1734.2
115100     IF      WRK-DU-2V0-2 = 0                                     NC1734.2
115200             PERFORM PASS                                         NC1734.2
115300             PERFORM PRINT-DETAIL                                 NC1734.2
115400     ELSE                                                         NC1734.2
115500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
115600             PERFORM FAIL                                         NC1734.2
115700             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
115800             MOVE    0            TO CORRECT-N                    NC1734.2
115900             PERFORM PRINT-DETAIL.                                NC1734.2
116000     ADD 1 TO REC-CT.                                             NC1734.2
116100 DIV-TEST-F3-20-5.                                                NC1734.2
116200     IF      WRK-DU-2V1-3 = 0                                     NC1734.2
116300             PERFORM PASS                                         NC1734.2
116400             PERFORM PRINT-DETAIL                                 NC1734.2
116500     ELSE                                                         NC1734.2
116600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
116700             PERFORM FAIL                                         NC1734.2
116800             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
116900             MOVE    0            TO CORRECT-N                    NC1734.2
117000             PERFORM PRINT-DETAIL.                                NC1734.2
117100     ADD 1 TO REC-CT.                                             NC1734.2
117200 DIV-TEST-F3-20-6.                                                NC1734.2
117300     IF      WRK-DU-2V0-3 = 0                                     NC1734.2
117400             PERFORM PASS                                         NC1734.2
117500             PERFORM PRINT-DETAIL                                 NC1734.2
117600     ELSE                                                         NC1734.2
117700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
117800             PERFORM FAIL                                         NC1734.2
117900             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
118000             MOVE    0            TO CORRECT-N                    NC1734.2
118100             PERFORM PRINT-DETAIL.                                NC1734.2
118200     ADD 1 TO REC-CT.                                             NC1734.2
118300 DIV-TEST-F3-20-7.                                                NC1734.2
118400     IF      WRK-XN-00001  = "1"                                  NC1734.2
118500             PERFORM PASS                                         NC1734.2
118600             PERFORM PRINT-DETAIL                                 NC1734.2
118700     ELSE                                                         NC1734.2
118800             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
118900             MOVE   "1"           TO CORRECT-X                    NC1734.2
119000             MOVE   "DIV-TEST-F3-20-7" TO PAR-NAME                NC1734.2
119100             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
119200             PERFORM FAIL                                         NC1734.2
119300             PERFORM PRINT-DETAIL.                                NC1734.2
119400*                                                                 NC1734.2
119500*                                                                 NC1734.2
119600 DIV-INIT-F3-21.                                                  NC1734.2
119700*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
119800*            ==-->  MULTIPLE RESULT FIELDS <--==                  NC1734.2
119900     MOVE   "DIV-TEST-F3-21"     TO PAR-NAME.                     NC1734.2
120000     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1734.2
120100     MOVE   "0"  TO WRK-XN-00001.                                 NC1734.2
120200     MOVE    0   TO WRK-DU-2V1-1.                                 NC1734.2
120300     MOVE    0   TO WRK-DU-2V0-1.                                 NC1734.2
120400     MOVE    0   TO WRK-DU-2V1-2.                                 NC1734.2
120500     MOVE    0   TO WRK-DU-2V0-2.                                 NC1734.2
120600     MOVE    0   TO WRK-DU-2V1-3.                                 NC1734.2
120700     MOVE    0   TO WRK-DU-2V0-3.                                 NC1734.2
120800     MOVE    1   TO REC-CT.                                       NC1734.2
120900     MOVE   10   TO WRK-DU-2V0-1.                                 NC1734.2
121000     MOVE  3.9   TO WRK-DU-1V1-2.                                 NC1734.2
121100 DIV-TEST-F3-21-0.                                                NC1734.2
121200     DIVIDE  WRK-DU-2V0-1 BY WRK-DU-1V1-2                         NC1734.2
121300      GIVING WRK-DU-2V1-1                                         NC1734.2
121400             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
121500             WRK-DU-2V1-2                                         NC1734.2
121600             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
121700             WRK-DU-2V1-3                                         NC1734.2
121800             WRK-DU-2V0-3                                         NC1734.2
121900          ON SIZE ERROR                                           NC1734.2
122000             MOVE   "1" TO WRK-XN-00001.                          NC1734.2
122100     GO TO DIV-TEST-F3-21-1.                                      NC1734.2
122200 DIV-DELETE-F3-21.                                                NC1734.2
122300     PERFORM DE-LETE.                                             NC1734.2
122400     PERFORM PRINT-DETAIL.                                        NC1734.2
122500     GO TO   DIV-INIT-F3-22.                                      NC1734.2
122600 DIV-TEST-F3-21-1.                                                NC1734.2
122700     IF      WRK-DU-2V1-1 = 2.5                                   NC1734.2
122800             PERFORM PASS                                         NC1734.2
122900             PERFORM PRINT-DETAIL                                 NC1734.2
123000     ELSE                                                         NC1734.2
123100             PERFORM FAIL                                         NC1734.2
123200             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
123300             MOVE    2.5          TO CORRECT-N                    NC1734.2
123400             PERFORM PRINT-DETAIL.                                NC1734.2
123500     ADD 1 TO REC-CT.                                             NC1734.2
123600 DIV-TEST-F3-21-2.                                                NC1734.2
123700     IF      WRK-DU-2V0-1 = 3                                     NC1734.2
123800             PERFORM PASS                                         NC1734.2
123900             PERFORM PRINT-DETAIL                                 NC1734.2
124000     ELSE                                                         NC1734.2
124100             PERFORM FAIL                                         NC1734.2
124200             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
124300             MOVE    3            TO CORRECT-N                    NC1734.2
124400             PERFORM PRINT-DETAIL.                                NC1734.2
124500     ADD 1 TO REC-CT.                                             NC1734.2
124600 DIV-TEST-F3-21-3.                                                NC1734.2
124700     IF      WRK-DU-2V1-2 = 2.5                                   NC1734.2
124800             PERFORM PASS                                         NC1734.2
124900             PERFORM PRINT-DETAIL                                 NC1734.2
125000     ELSE                                                         NC1734.2
125100             PERFORM FAIL                                         NC1734.2
125200             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
125300             MOVE    2.5          TO CORRECT-N                    NC1734.2
125400             PERFORM PRINT-DETAIL.                                NC1734.2
125500     ADD 1 TO REC-CT.                                             NC1734.2
125600 DIV-TEST-F3-21-4.                                                NC1734.2
125700     IF      WRK-DU-2V0-2 = 3                                     NC1734.2
125800             PERFORM PASS                                         NC1734.2
125900             PERFORM PRINT-DETAIL                                 NC1734.2
126000     ELSE                                                         NC1734.2
126100             PERFORM FAIL                                         NC1734.2
126200             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
126300             MOVE    3            TO CORRECT-N                    NC1734.2
126400             PERFORM PRINT-DETAIL.                                NC1734.2
126500     ADD 1 TO REC-CT.                                             NC1734.2
126600 DIV-TEST-F3-21-5.                                                NC1734.2
126700     IF      WRK-DU-2V1-3 = 2.5                                   NC1734.2
126800             PERFORM PASS                                         NC1734.2
126900             PERFORM PRINT-DETAIL                                 NC1734.2
127000     ELSE                                                         NC1734.2
127100             PERFORM FAIL                                         NC1734.2
127200             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
127300             MOVE    2.5          TO CORRECT-N                    NC1734.2
127400             PERFORM PRINT-DETAIL.                                NC1734.2
127500     ADD 1 TO REC-CT.                                             NC1734.2
127600 DIV-TEST-F3-21-6.                                                NC1734.2
127700     IF      WRK-DU-2V0-3 = 2                                     NC1734.2
127800             PERFORM PASS                                         NC1734.2
127900             PERFORM PRINT-DETAIL                                 NC1734.2
128000     ELSE                                                         NC1734.2
128100             PERFORM FAIL                                         NC1734.2
128200             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
128300             MOVE    2            TO CORRECT-N                    NC1734.2
128400             PERFORM PRINT-DETAIL.                                NC1734.2
128500     ADD 1 TO REC-CT.                                             NC1734.2
128600 DIV-TEST-F3-21-7.                                                NC1734.2
128700     IF      WRK-XN-00001  = "0"                                  NC1734.2
128800             PERFORM PASS                                         NC1734.2
128900             PERFORM PRINT-DETAIL                                 NC1734.2
129000     ELSE                                                         NC1734.2
129100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
129200             MOVE   "0"           TO CORRECT-X                    NC1734.2
129300             MOVE   "DIV-TEST-F3-21-7" TO PAR-NAME                NC1734.2
129400             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
129500                  TO RE-MARK                                      NC1734.2
129600             PERFORM FAIL                                         NC1734.2
129700             PERFORM PRINT-DETAIL.                                NC1734.2
129800*                                                                 NC1734.2
129900*                                                                 NC1734.2
130000 DIV-INIT-F3-22.                                                  NC1734.2
130100*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
130200*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
130300     MOVE   "DIV-TEST-F3-22" TO PAR-NAME.                         NC1734.2
130400     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1734.2
130500     MOVE   "0"  TO WRK-XN-00001.                                 NC1734.2
130600     MOVE    0   TO WRK-DU-2V1-1.                                 NC1734.2
130700     MOVE    0   TO WRK-DU-2V0-1.                                 NC1734.2
130800     MOVE    0   TO WRK-DU-2V1-2.                                 NC1734.2
130900     MOVE    0   TO WRK-DU-2V0-2.                                 NC1734.2
131000     MOVE    0   TO WRK-DU-2V1-3.                                 NC1734.2
131100     MOVE    0   TO WRK-DU-2V0-3.                                 NC1734.2
131200     MOVE    1   TO REC-CT.                                       NC1734.2
131300     MOVE    99  TO WRK-DU-2V0-1.                                 NC1734.2
131400 DIV-TEST-F3-22-0.                                                NC1734.2
131500     DIVIDE  WRK-DU-2V0-1 BY A01ONE-CS-00V01                      NC1734.2
131600      GIVING WRK-DU-2V1-1                                         NC1734.2
131700             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
131800             WRK-DU-2V1-2                                         NC1734.2
131900             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
132000             WRK-DU-2V1-3                                         NC1734.2
132100             WRK-DU-2V0-3                                         NC1734.2
132200      NOT ON SIZE ERROR                                           NC1734.2
132300             MOVE  "1" TO WRK-XN-00001.                           NC1734.2
132400     GO TO   DIV-TEST-F3-22-1.                                    NC1734.2
132500 DIV-DELETE-F3-22.                                                NC1734.2
132600     PERFORM DE-LETE.                                             NC1734.2
132700     PERFORM PRINT-DETAIL.                                        NC1734.2
132800     GO TO   DIV-INIT-F3-23.                                      NC1734.2
132900 DIV-TEST-F3-22-1.                                                NC1734.2
133000     IF      WRK-DU-2V1-1 = 0                                     NC1734.2
133100             PERFORM PASS                                         NC1734.2
133200             PERFORM PRINT-DETAIL                                 NC1734.2
133300     ELSE                                                         NC1734.2
133400             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
133500             PERFORM FAIL                                         NC1734.2
133600             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
133700             MOVE    0            TO CORRECT-N                    NC1734.2
133800             PERFORM PRINT-DETAIL.                                NC1734.2
133900     ADD 1 TO REC-CT.                                             NC1734.2
134000 DIV-TEST-F3-22-2.                                                NC1734.2
134100     IF      WRK-DU-2V0-1 = 99                                    NC1734.2
134200             PERFORM PASS                                         NC1734.2
134300             PERFORM PRINT-DETAIL                                 NC1734.2
134400     ELSE                                                         NC1734.2
134500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
134600             PERFORM FAIL                                         NC1734.2
134700             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
134800             MOVE    99           TO CORRECT-N                    NC1734.2
134900             PERFORM PRINT-DETAIL.                                NC1734.2
135000     ADD 1 TO REC-CT.                                             NC1734.2
135100 DIV-TEST-F3-22-3.                                                NC1734.2
135200     IF      WRK-DU-2V1-2 = 0                                     NC1734.2
135300             PERFORM PASS                                         NC1734.2
135400             PERFORM PRINT-DETAIL                                 NC1734.2
135500     ELSE                                                         NC1734.2
135600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
135700             PERFORM FAIL                                         NC1734.2
135800             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
135900             MOVE    0            TO CORRECT-N                    NC1734.2
136000             PERFORM PRINT-DETAIL.                                NC1734.2
136100     ADD 1 TO REC-CT.                                             NC1734.2
136200 DIV-TEST-F3-22-4.                                                NC1734.2
136300     IF      WRK-DU-2V0-2 = 0                                     NC1734.2
136400             PERFORM PASS                                         NC1734.2
136500             PERFORM PRINT-DETAIL                                 NC1734.2
136600     ELSE                                                         NC1734.2
136700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
136800             PERFORM FAIL                                         NC1734.2
136900             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
137000             MOVE    0            TO CORRECT-N                    NC1734.2
137100             PERFORM PRINT-DETAIL.                                NC1734.2
137200     ADD 1 TO REC-CT.                                             NC1734.2
137300 DIV-TEST-F3-22-5.                                                NC1734.2
137400     IF      WRK-DU-2V1-3 = 0                                     NC1734.2
137500             PERFORM PASS                                         NC1734.2
137600             PERFORM PRINT-DETAIL                                 NC1734.2
137700     ELSE                                                         NC1734.2
137800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
137900             PERFORM FAIL                                         NC1734.2
138000             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
138100             MOVE    0            TO CORRECT-N                    NC1734.2
138200             PERFORM PRINT-DETAIL.                                NC1734.2
138300     ADD 1 TO REC-CT.                                             NC1734.2
138400 DIV-TEST-F3-22-6.                                                NC1734.2
138500     IF      WRK-DU-2V0-3 = 0                                     NC1734.2
138600             PERFORM PASS                                         NC1734.2
138700             PERFORM PRINT-DETAIL                                 NC1734.2
138800     ELSE                                                         NC1734.2
138900             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
139000             PERFORM FAIL                                         NC1734.2
139100             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
139200             MOVE    0            TO CORRECT-N                    NC1734.2
139300             PERFORM PRINT-DETAIL.                                NC1734.2
139400     ADD 1 TO REC-CT.                                             NC1734.2
139500 DIV-TEST-F3-22-7.                                                NC1734.2
139600     IF      WRK-XN-00001  = "0"                                  NC1734.2
139700             PERFORM PASS                                         NC1734.2
139800             PERFORM PRINT-DETAIL                                 NC1734.2
139900     ELSE                                                         NC1734.2
140000             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
140100             MOVE   "0"           TO CORRECT-X                    NC1734.2
140200             MOVE   "DIV-TEST-F3-22-7" TO PAR-NAME                NC1734.2
140300             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1734.2
140400                  TO RE-MARK                                      NC1734.2
140500             PERFORM FAIL                                         NC1734.2
140600             PERFORM PRINT-DETAIL.                                NC1734.2
140700*                                                                 NC1734.2
140800*                                                                 NC1734.2
140900 DIV-INIT-F3-23.                                                  NC1734.2
141000*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
141100*            ==-->  NEW SIZE ERROR TESTS   <--==                  NC1734.2
141200     MOVE   "DIV-TEST-F3-23" TO PAR-NAME.                         NC1734.2
141300     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1734.2
141400     MOVE   "0"  TO WRK-XN-00001.                                 NC1734.2
141500     MOVE    0   TO WRK-DU-2V1-1.                                 NC1734.2
141600     MOVE    0   TO WRK-DU-2V1-2.                                 NC1734.2
141700     MOVE    0   TO WRK-DU-2V0-2.                                 NC1734.2
141800     MOVE    0   TO WRK-DU-2V1-3.                                 NC1734.2
141900     MOVE    0   TO WRK-DU-2V0-3.                                 NC1734.2
142000     MOVE    1   TO REC-CT.                                       NC1734.2
142100     MOVE    10  TO WRK-DU-2V0-1.                                 NC1734.2
142200     MOVE    3.9 TO WRK-DU-1V1-2.                                 NC1734.2
142300 DIV-TEST-F3-23-0.                                                NC1734.2
142400     DIVIDE  WRK-DU-2V0-1 BY WRK-DU-1V1-2                         NC1734.2
142500      GIVING WRK-DU-2V1-1                                         NC1734.2
142600             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
142700             WRK-DU-2V1-2                                         NC1734.2
142800             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
142900             WRK-DU-2V1-3                                         NC1734.2
143000             WRK-DU-2V0-3                                         NC1734.2
143100      NOT ON SIZE ERROR                                           NC1734.2
143200             MOVE   "1" TO WRK-XN-00001.                          NC1734.2
143300     GO TO DIV-TEST-F3-23-1.                                      NC1734.2
143400 DIV-DELETE-F3-23.                                                NC1734.2
143500     PERFORM DE-LETE.                                             NC1734.2
143600     PERFORM PRINT-DETAIL.                                        NC1734.2
143700     GO TO   DIV-INIT-F3-24.                                      NC1734.2
143800 DIV-TEST-F3-23-1.                                                NC1734.2
143900     IF      WRK-DU-2V1-1 = 2.5                                   NC1734.2
144000             PERFORM PASS                                         NC1734.2
144100             PERFORM PRINT-DETAIL                                 NC1734.2
144200     ELSE                                                         NC1734.2
144300             PERFORM FAIL                                         NC1734.2
144400             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
144500             MOVE    2.5          TO CORRECT-N                    NC1734.2
144600             PERFORM PRINT-DETAIL.                                NC1734.2
144700     ADD 1 TO REC-CT.                                             NC1734.2
144800 DIV-TEST-F3-23-2.                                                NC1734.2
144900     IF      WRK-DU-2V0-1 = 3                                     NC1734.2
145000             PERFORM PASS                                         NC1734.2
145100             PERFORM PRINT-DETAIL                                 NC1734.2
145200     ELSE                                                         NC1734.2
145300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
145400             PERFORM FAIL                                         NC1734.2
145500             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
145600             MOVE    3            TO CORRECT-N                    NC1734.2
145700             PERFORM PRINT-DETAIL.                                NC1734.2
145800     ADD 1 TO REC-CT.                                             NC1734.2
145900 DIV-TEST-F3-23-3.                                                NC1734.2
146000     IF      WRK-DU-2V1-2 = 2.5                                   NC1734.2
146100             PERFORM PASS                                         NC1734.2
146200             PERFORM PRINT-DETAIL                                 NC1734.2
146300     ELSE                                                         NC1734.2
146400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
146500             PERFORM FAIL                                         NC1734.2
146600             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
146700             MOVE    2.5          TO CORRECT-N                    NC1734.2
146800             PERFORM PRINT-DETAIL.                                NC1734.2
146900     ADD 1 TO REC-CT.                                             NC1734.2
147000 DIV-TEST-F3-23-4.                                                NC1734.2
147100     IF      WRK-DU-2V0-2 = 3                                     NC1734.2
147200             PERFORM PASS                                         NC1734.2
147300             PERFORM PRINT-DETAIL                                 NC1734.2
147400     ELSE                                                         NC1734.2
147500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
147600             PERFORM FAIL                                         NC1734.2
147700             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
147800             MOVE    3            TO CORRECT-N                    NC1734.2
147900             PERFORM PRINT-DETAIL.                                NC1734.2
148000     ADD 1 TO REC-CT.                                             NC1734.2
148100 DIV-TEST-F3-23-5.                                                NC1734.2
148200     IF      WRK-DU-2V1-3 = 2.5                                   NC1734.2
148300             PERFORM PASS                                         NC1734.2
148400             PERFORM PRINT-DETAIL                                 NC1734.2
148500     ELSE                                                         NC1734.2
148600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
148700             PERFORM FAIL                                         NC1734.2
148800             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
148900             MOVE    2.5          TO CORRECT-N                    NC1734.2
149000             PERFORM PRINT-DETAIL.                                NC1734.2
149100     ADD 1 TO REC-CT.                                             NC1734.2
149200 DIV-TEST-F3-23-6.                                                NC1734.2
149300     IF      WRK-DU-2V0-3 = 2                                     NC1734.2
149400             PERFORM PASS                                         NC1734.2
149500             PERFORM PRINT-DETAIL                                 NC1734.2
149600     ELSE                                                         NC1734.2
149700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
149800             PERFORM FAIL                                         NC1734.2
149900             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
150000             MOVE    2            TO CORRECT-N                    NC1734.2
150100             PERFORM PRINT-DETAIL.                                NC1734.2
150200     ADD 1 TO REC-CT.                                             NC1734.2
150300 DIV-TEST-F3-23-7.                                                NC1734.2
150400     IF      WRK-XN-00001  = "1"                                  NC1734.2
150500             PERFORM PASS                                         NC1734.2
150600             PERFORM PRINT-DETAIL                                 NC1734.2
150700     ELSE                                                         NC1734.2
150800             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
150900             MOVE   "1"           TO CORRECT-X                    NC1734.2
151000             MOVE   "DIV-TEST-F3-23-7" TO PAR-NAME                NC1734.2
151100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1734.2
151200                  TO RE-MARK                                      NC1734.2
151300             PERFORM FAIL                                         NC1734.2
151400             PERFORM PRINT-DETAIL.                                NC1734.2
151500*                                                                 NC1734.2
151600*                                                                 NC1734.2
151700 DIV-INIT-F3-24.                                                  NC1734.2
151800*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
151900*            ==-->    NEW SIZE ERROR TESTS <--==                  NC1734.2
152000     MOVE   "DIV-TEST-F3-24" TO PAR-NAME.                         NC1734.2
152100     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1734.2
152200     MOVE   "0"  TO WRK-XN-00001.                                 NC1734.2
152300     MOVE    0   TO WRK-DU-2V1-1.                                 NC1734.2
152400     MOVE    0   TO WRK-DU-2V0-1.                                 NC1734.2
152500     MOVE    0   TO WRK-DU-2V1-2.                                 NC1734.2
152600     MOVE    0   TO WRK-DU-2V0-2.                                 NC1734.2
152700     MOVE    0   TO WRK-DU-2V1-3.                                 NC1734.2
152800     MOVE    0   TO WRK-DU-2V0-3.                                 NC1734.2
152900     MOVE    1   TO REC-CT.                                       NC1734.2
153000     MOVE    99  TO WRK-DU-2V0-1.                                 NC1734.2
153100 DIV-TEST-F3-24-0.                                                NC1734.2
153200     DIVIDE  WRK-DU-2V0-1 BY A01ONE-CS-00V01                      NC1734.2
153300      GIVING WRK-DU-2V1-1                                         NC1734.2
153400             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
153500             WRK-DU-2V1-2                                         NC1734.2
153600             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
153700             WRK-DU-2V1-3                                         NC1734.2
153800             WRK-DU-2V0-3                                         NC1734.2
153900          ON SIZE ERROR                                           NC1734.2
154000             MOVE  "1" TO WRK-XN-00001                            NC1734.2
154100      NOT ON SIZE ERROR                                           NC1734.2
154200             MOVE  "2" TO WRK-XN-00001.                           NC1734.2
154300     GO TO   DIV-TEST-F3-24-1.                                    NC1734.2
154400 DIV-DELETE-F3-24.                                                NC1734.2
154500     PERFORM DE-LETE.                                             NC1734.2
154600     PERFORM PRINT-DETAIL.                                        NC1734.2
154700     GO TO   DIV-INIT-F3-25.                                      NC1734.2
154800 DIV-TEST-F3-24-1.                                                NC1734.2
154900     IF      WRK-DU-2V1-1 = 0                                     NC1734.2
155000             PERFORM PASS                                         NC1734.2
155100             PERFORM PRINT-DETAIL                                 NC1734.2
155200     ELSE                                                         NC1734.2
155300             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
155400             PERFORM FAIL                                         NC1734.2
155500             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
155600             MOVE    0            TO CORRECT-N                    NC1734.2
155700             PERFORM PRINT-DETAIL.                                NC1734.2
155800     ADD 1 TO REC-CT.                                             NC1734.2
155900 DIV-TEST-F3-24-2.                                                NC1734.2
156000     IF      WRK-DU-2V0-1 = 99                                    NC1734.2
156100             PERFORM PASS                                         NC1734.2
156200             PERFORM PRINT-DETAIL                                 NC1734.2
156300     ELSE                                                         NC1734.2
156400             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
156500             PERFORM FAIL                                         NC1734.2
156600             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
156700             MOVE    99           TO CORRECT-N                    NC1734.2
156800             PERFORM PRINT-DETAIL.                                NC1734.2
156900     ADD 1 TO REC-CT.                                             NC1734.2
157000 DIV-TEST-F3-24-3.                                                NC1734.2
157100     IF      WRK-DU-2V1-2 = 0                                     NC1734.2
157200             PERFORM PASS                                         NC1734.2
157300             PERFORM PRINT-DETAIL                                 NC1734.2
157400     ELSE                                                         NC1734.2
157500             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
157600             PERFORM FAIL                                         NC1734.2
157700             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
157800             MOVE    0            TO CORRECT-N                    NC1734.2
157900             PERFORM PRINT-DETAIL.                                NC1734.2
158000     ADD 1 TO REC-CT.                                             NC1734.2
158100 DIV-TEST-F3-24-4.                                                NC1734.2
158200     IF      WRK-DU-2V0-2 = 0                                     NC1734.2
158300             PERFORM PASS                                         NC1734.2
158400             PERFORM PRINT-DETAIL                                 NC1734.2
158500     ELSE                                                         NC1734.2
158600             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
158700             PERFORM FAIL                                         NC1734.2
158800             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
158900             MOVE    0            TO CORRECT-N                    NC1734.2
159000             PERFORM PRINT-DETAIL.                                NC1734.2
159100     ADD 1 TO REC-CT.                                             NC1734.2
159200 DIV-TEST-F3-24-5.                                                NC1734.2
159300     IF      WRK-DU-2V1-3 = 0                                     NC1734.2
159400             PERFORM PASS                                         NC1734.2
159500             PERFORM PRINT-DETAIL                                 NC1734.2
159600     ELSE                                                         NC1734.2
159700             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
159800             PERFORM FAIL                                         NC1734.2
159900             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
160000             MOVE    0            TO CORRECT-N                    NC1734.2
160100             PERFORM PRINT-DETAIL.                                NC1734.2
160200     ADD 1 TO REC-CT.                                             NC1734.2
160300 DIV-TEST-F3-24-6.                                                NC1734.2
160400     IF      WRK-DU-2V0-3 = 0                                     NC1734.2
160500             PERFORM PASS                                         NC1734.2
160600             PERFORM PRINT-DETAIL                                 NC1734.2
160700     ELSE                                                         NC1734.2
160800             MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK   NC1734.2
160900             PERFORM FAIL                                         NC1734.2
161000             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
161100             MOVE    0            TO CORRECT-N                    NC1734.2
161200             PERFORM PRINT-DETAIL.                                NC1734.2
161300     ADD 1 TO REC-CT.                                             NC1734.2
161400 DIV-TEST-F3-24-7.                                                NC1734.2
161500     IF      WRK-XN-00001  = "1"                                  NC1734.2
161600             PERFORM PASS                                         NC1734.2
161700             PERFORM PRINT-DETAIL                                 NC1734.2
161800     ELSE                                                         NC1734.2
161900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
162000             MOVE   "1"           TO CORRECT-X                    NC1734.2
162100             MOVE   "DIV-TEST-F3-24-7" TO PAR-NAME                NC1734.2
162200             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1734.2
162300                  TO RE-MARK                                      NC1734.2
162400             PERFORM FAIL                                         NC1734.2
162500             PERFORM PRINT-DETAIL.                                NC1734.2
162600*                                                                 NC1734.2
162700*                                                                 NC1734.2
162800 DIV-INIT-F3-25.                                                  NC1734.2
162900*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
163000*            ==-->  NEW SIZE ERROR TESTS   <--==                  NC1734.2
163100     MOVE   "DIV-TEST-F3-25" TO PAR-NAME.                         NC1734.2
163200     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1734.2
163300     MOVE   "0"  TO WRK-XN-00001.                                 NC1734.2
163400     MOVE    0   TO WRK-DU-2V1-1.                                 NC1734.2
163500     MOVE    0   TO WRK-DU-2V1-2.                                 NC1734.2
163600     MOVE    0   TO WRK-DU-2V0-2.                                 NC1734.2
163700     MOVE    0   TO WRK-DU-2V1-3.                                 NC1734.2
163800     MOVE    0   TO WRK-DU-2V0-3.                                 NC1734.2
163900     MOVE    1   TO REC-CT.                                       NC1734.2
164000     MOVE    10  TO WRK-DU-2V0-1.                                 NC1734.2
164100     MOVE    3.9 TO WRK-DU-1V1-2.                                 NC1734.2
164200 DIV-TEST-F3-25-0.                                                NC1734.2
164300     DIVIDE  WRK-DU-2V0-1 BY WRK-DU-1V1-2                         NC1734.2
164400      GIVING WRK-DU-2V1-1                                         NC1734.2
164500             WRK-DU-2V0-1 ROUNDED                                 NC1734.2
164600             WRK-DU-2V1-2                                         NC1734.2
164700             WRK-DU-2V0-2 ROUNDED                                 NC1734.2
164800             WRK-DU-2V1-3                                         NC1734.2
164900             WRK-DU-2V0-3                                         NC1734.2
165000          ON SIZE ERROR                                           NC1734.2
165100             MOVE   "1" TO WRK-XN-00001                           NC1734.2
165200      NOT ON SIZE ERROR                                           NC1734.2
165300             MOVE   "2" TO WRK-XN-00001.                          NC1734.2
165400     GO TO DIV-TEST-F3-25-1.                                      NC1734.2
165500 DIV-DELETE-F3-25.                                                NC1734.2
165600     PERFORM DE-LETE.                                             NC1734.2
165700     PERFORM PRINT-DETAIL.                                        NC1734.2
165800     GO TO   DIV-INIT-F3-26.                                      NC1734.2
165900 DIV-TEST-F3-25-1.                                                NC1734.2
166000     IF      WRK-DU-2V1-1 = 2.5                                   NC1734.2
166100             PERFORM PASS                                         NC1734.2
166200             PERFORM PRINT-DETAIL                                 NC1734.2
166300     ELSE                                                         NC1734.2
166400             PERFORM FAIL                                         NC1734.2
166500             MOVE    WRK-DU-2V1-1 TO COMPUTED-N                   NC1734.2
166600             MOVE    2.5          TO CORRECT-N                    NC1734.2
166700             PERFORM PRINT-DETAIL.                                NC1734.2
166800     ADD 1 TO REC-CT.                                             NC1734.2
166900 DIV-TEST-F3-25-2.                                                NC1734.2
167000     IF      WRK-DU-2V0-1 = 3                                     NC1734.2
167100             PERFORM PASS                                         NC1734.2
167200             PERFORM PRINT-DETAIL                                 NC1734.2
167300     ELSE                                                         NC1734.2
167400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
167500             PERFORM FAIL                                         NC1734.2
167600             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1734.2
167700             MOVE    3            TO CORRECT-N                    NC1734.2
167800             PERFORM PRINT-DETAIL.                                NC1734.2
167900     ADD 1 TO REC-CT.                                             NC1734.2
168000 DIV-TEST-F3-25-3.                                                NC1734.2
168100     IF      WRK-DU-2V1-2 = 2.5                                   NC1734.2
168200             PERFORM PASS                                         NC1734.2
168300             PERFORM PRINT-DETAIL                                 NC1734.2
168400     ELSE                                                         NC1734.2
168500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
168600             PERFORM FAIL                                         NC1734.2
168700             MOVE    WRK-DU-2V1-2 TO COMPUTED-N                   NC1734.2
168800             MOVE    2.5          TO CORRECT-N                    NC1734.2
168900             PERFORM PRINT-DETAIL.                                NC1734.2
169000     ADD 1 TO REC-CT.                                             NC1734.2
169100 DIV-TEST-F3-25-4.                                                NC1734.2
169200     IF      WRK-DU-2V0-2 = 3                                     NC1734.2
169300             PERFORM PASS                                         NC1734.2
169400             PERFORM PRINT-DETAIL                                 NC1734.2
169500     ELSE                                                         NC1734.2
169600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
169700             PERFORM FAIL                                         NC1734.2
169800             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1734.2
169900             MOVE    3            TO CORRECT-N                    NC1734.2
170000             PERFORM PRINT-DETAIL.                                NC1734.2
170100     ADD 1 TO REC-CT.                                             NC1734.2
170200 DIV-TEST-F3-25-5.                                                NC1734.2
170300     IF      WRK-DU-2V1-3 = 2.5                                   NC1734.2
170400             PERFORM PASS                                         NC1734.2
170500             PERFORM PRINT-DETAIL                                 NC1734.2
170600     ELSE                                                         NC1734.2
170700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
170800             PERFORM FAIL                                         NC1734.2
170900             MOVE    WRK-DU-2V1-3 TO COMPUTED-N                   NC1734.2
171000             MOVE    2.5          TO CORRECT-N                    NC1734.2
171100             PERFORM PRINT-DETAIL.                                NC1734.2
171200     ADD 1 TO REC-CT.                                             NC1734.2
171300 DIV-TEST-F3-25-6.                                                NC1734.2
171400     IF      WRK-DU-2V0-3 = 2                                     NC1734.2
171500             PERFORM PASS                                         NC1734.2
171600             PERFORM PRINT-DETAIL                                 NC1734.2
171700     ELSE                                                         NC1734.2
171800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
171900             PERFORM FAIL                                         NC1734.2
172000             MOVE    WRK-DU-2V0-3 TO COMPUTED-N                   NC1734.2
172100             MOVE    2            TO CORRECT-N                    NC1734.2
172200             PERFORM PRINT-DETAIL.                                NC1734.2
172300     ADD 1 TO REC-CT.                                             NC1734.2
172400 DIV-TEST-F3-25-7.                                                NC1734.2
172500     IF      WRK-XN-00001  = "2"                                  NC1734.2
172600             PERFORM PASS                                         NC1734.2
172700             PERFORM PRINT-DETAIL                                 NC1734.2
172800     ELSE                                                         NC1734.2
172900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1734.2
173000             MOVE   "2"           TO CORRECT-X                    NC1734.2
173100             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
173200                  TO RE-MARK                                      NC1734.2
173300             PERFORM FAIL                                         NC1734.2
173400             PERFORM PRINT-DETAIL.                                NC1734.2
173500*                                                                 NC1734.2
173600*                                                                 NC1734.2
173700 DIV-INIT-F3-26.                                                  NC1734.2
173800*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
173900*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1734.2
174000     MOVE   "DIV-TEST-F3-26" TO PAR-NAME.                         NC1734.2
174100     MOVE   "1V-41 6.4.3" TO ANSI-REFERENCE.                      NC1734.2
174200     MOVE    44.1  TO DIV2.                                       NC1734.2
174300     MOVE   -9.642 TO DIV4.                                       NC1734.2
174400     MOVE    0     TO DIV10.                                      NC1734.2
174500     MOVE    0     TO WRK-XN-00001.                               NC1734.2
174600     MOVE    0     TO WRK-DS-05V00.                               NC1734.2
174700     MOVE   "A"    TO XRAY.                                       NC1734.2
174800     MOVE    1     TO REC-CT.                                     NC1734.2
174900 DIV-TEST-F3-26-0.                                                NC1734.2
175000     DIVIDE  DIV2 BY DIV4                                         NC1734.2
175100      GIVING DIV10                                                NC1734.2
175200          ON SIZE ERROR                                           NC1734.2
175300             MOVE "P" TO XRAY                                     NC1734.2
175400             MOVE "1" TO WRK-XN-00001                             NC1734.2
175500             MOVE  23 TO WRK-DS-05V00                             NC1734.2
175600     END-DIVIDE                                                   NC1734.2
175700     MOVE    99 TO WRK-CS-18V00.                                  NC1734.2
175800     GO TO DIV-TEST-F3-26-1.                                      NC1734.2
175900 DIV-DELETE-F3-26.                                                NC1734.2
176000     PERFORM DE-LETE.                                             NC1734.2
176100     PERFORM PRINT-DETAIL.                                        NC1734.2
176200     GO TO   DIV-INIT-F3-27.                                      NC1734.2
176300 DIV-TEST-F3-26-1.                                                NC1734.2
176400     MOVE   "DIV-TEST-F3-26-1" TO PAR-NAME.                       NC1734.2
176500     IF      DIV10  = 0                                           NC1734.2
176600             PERFORM PASS                                         NC1734.2
176700             PERFORM PRINT-DETAIL                                 NC1734.2
176800     ELSE                                                         NC1734.2
176900             MOVE    DIV10 TO COMPUTED-N                          NC1734.2
177000             MOVE    0     TO CORRECT-N                           NC1734.2
177100             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
177200             PERFORM FAIL                                         NC1734.2
177300             PERFORM PRINT-DETAIL.                                NC1734.2
177400     ADD     1 TO REC-CT.                                         NC1734.2
177500 DIV-TEST-F3-26-2.                                                NC1734.2
177600     MOVE   "DIV-TEST-F3-26-2" TO PAR-NAME.                       NC1734.2
177700     IF      XRAY    = "P"                                        NC1734.2
177800             PERFORM PASS                                         NC1734.2
177900             PERFORM PRINT-DETAIL                                 NC1734.2
178000     ELSE                                                         NC1734.2
178100             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
178200             MOVE   "P"    TO CORRECT-X                           NC1734.2
178300             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
178400             PERFORM FAIL                                         NC1734.2
178500             PERFORM PRINT-DETAIL.                                NC1734.2
178600     ADD     1 TO REC-CT.                                         NC1734.2
178700 DIV-TEST-F3-26-3.                                                NC1734.2
178800     MOVE   "DIV-TEST-F3-26-3" TO PAR-NAME.                       NC1734.2
178900     IF      WRK-XN-00001 = "1"                                   NC1734.2
179000             PERFORM PASS                                         NC1734.2
179100             PERFORM PRINT-DETAIL                                 NC1734.2
179200     ELSE                                                         NC1734.2
179300             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1734.2
179400             MOVE   "1"            TO CORRECT-X                   NC1734.2
179500             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
179600             PERFORM FAIL                                         NC1734.2
179700             PERFORM PRINT-DETAIL.                                NC1734.2
179800     ADD     1 TO REC-CT.                                         NC1734.2
179900 DIV-TEST-F3-26-4.                                                NC1734.2
180000     MOVE   "DIV-TEST-F3-26-4" TO PAR-NAME.                       NC1734.2
180100     IF      WRK-DS-05V00 = 23                                    NC1734.2
180200             PERFORM PASS                                         NC1734.2
180300             PERFORM PRINT-DETAIL                                 NC1734.2
180400     ELSE                                                         NC1734.2
180500             MOVE    WRK-DS-05V00  TO COMPUTED-N                  NC1734.2
180600             MOVE    23            TO CORRECT-N                   NC1734.2
180700             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1734.2
180800             PERFORM FAIL                                         NC1734.2
180900             PERFORM PRINT-DETAIL.                                NC1734.2
181000     ADD     1 TO REC-CT.                                         NC1734.2
181100 DIV-TEST-F3-26-5.                                                NC1734.2
181200     MOVE   "DIV-TEST-F3-26-5" TO PAR-NAME.                       NC1734.2
181300     IF      WRK-CS-18V00 = 000000000000000099                    NC1734.2
181400             PERFORM PASS                                         NC1734.2
181500             PERFORM PRINT-DETAIL                                 NC1734.2
181600     ELSE                                                         NC1734.2
181700             MOVE    WRK-CS-18V00  TO COMPUTED-N                  NC1734.2
181800             MOVE    000000000000000099  TO CORRECT-N             NC1734.2
181900             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1734.2
182000             PERFORM FAIL                                         NC1734.2
182100             PERFORM PRINT-DETAIL.                                NC1734.2
182200*                                                                 NC1734.2
182300*                                                                 NC1734.2
182400 DIV-INIT-F3-27.                                                  NC1734.2
182500*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
182600*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1734.2
182700     MOVE   "DIV-TEST-F3-27" TO PAR-NAME.                         NC1734.2
182800     MOVE   "1V-41 6.4.3" TO ANSI-REFERENCE.                      NC1734.2
182900     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1734.2
183000     MOVE   "0" TO WRK-XN-00001.                                  NC1734.2
183100     MOVE    0  TO WRK-DS-05V00.                                  NC1734.2
183200     MOVE    0  TO WRK-DS-02V00.                                  NC1734.2
183300     MOVE    0  TO WRK-CS-18V00.                                  NC1734.2
183400     MOVE    1  TO REC-CT.                                        NC1734.2
183500 DIV-TEST-F3-27-0.                                                NC1734.2
183600     DIVIDE  WRK-DS-09V09 BY A18ONES-DS-09V09                     NC1734.2
183700      GIVING WRK-DS-09V09                                         NC1734.2
183800          ON SIZE ERROR                                           NC1734.2
183900             MOVE "1" TO WRK-XN-00001                             NC1734.2
184000             MOVE  23 TO WRK-DS-05V00                             NC1734.2
184100             MOVE  -4 TO WRK-DS-02V00                             NC1734.2
184200     END-DIVIDE                                                   NC1734.2
184300     MOVE    99 TO WRK-CS-18V00.                                  NC1734.2
184400     GO TO   DIV-TEST-F3-27-1.                                    NC1734.2
184500 DIV-DELETE-F3-27-1.                                              NC1734.2
184600     PERFORM DE-LETE.                                             NC1734.2
184700     PERFORM PRINT-DETAIL.                                        NC1734.2
184800     GO TO   DIV-INIT-F3-28.                                      NC1734.2
184900 DIV-TEST-F3-27-1.                                                NC1734.2
185000     MOVE   "DIV-TEST-F3-27-1" TO PAR-NAME.                       NC1734.2
185100     IF      WRK-DS-18V00-S = 000000001000000000                  NC1734.2
185200             PERFORM PASS                                         NC1734.2
185300             PERFORM PRINT-DETAIL                                 NC1734.2
185400     ELSE                                                         NC1734.2
185500             MOVE    000000001000000000 TO CORRECT-18V0           NC1734.2
185600             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1734.2
185700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
185800             PERFORM FAIL                                         NC1734.2
185900             PERFORM PRINT-DETAIL.                                NC1734.2
186000     ADD     1 TO REC-CT.                                         NC1734.2
186100 DIV-TEST-F3-27-2.                                                NC1734.2
186200     MOVE   "DIV-TEST-F3-27-2" TO PAR-NAME.                       NC1734.2
186300     IF      WRK-DS-02V00 = 00                                    NC1734.2
186400             PERFORM PASS                                         NC1734.2
186500             PERFORM PRINT-DETAIL                                 NC1734.2
186600     ELSE                                                         NC1734.2
186700             MOVE    WRK-DS-02V00  TO COMPUTED-N                  NC1734.2
186800             MOVE    00            TO CORRECT-N                   NC1734.2
186900             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
187000                  TO RE-MARK                                      NC1734.2
187100             PERFORM FAIL                                         NC1734.2
187200             PERFORM PRINT-DETAIL.                                NC1734.2
187300     ADD     1 TO REC-CT.                                         NC1734.2
187400 DIV-TEST-F3-27-3.                                                NC1734.2
187500     MOVE   "DIV-TEST-F3-27-3" TO PAR-NAME.                       NC1734.2
187600     IF      WRK-XN-00001 = "0"                                   NC1734.2
187700             PERFORM PASS                                         NC1734.2
187800             PERFORM PRINT-DETAIL                                 NC1734.2
187900     ELSE                                                         NC1734.2
188000             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1734.2
188100             MOVE   "0"            TO CORRECT-X                   NC1734.2
188200             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
188300                  TO RE-MARK                                      NC1734.2
188400             PERFORM FAIL                                         NC1734.2
188500             PERFORM PRINT-DETAIL.                                NC1734.2
188600     ADD     1 TO REC-CT.                                         NC1734.2
188700 DIV-TEST-F3-27-4.                                                NC1734.2
188800     MOVE   "DIV-TEST-F3-27-4" TO PAR-NAME.                       NC1734.2
188900     IF      WRK-DS-05V00 = 0                                     NC1734.2
189000             PERFORM PASS                                         NC1734.2
189100             PERFORM PRINT-DETAIL                                 NC1734.2
189200     ELSE                                                         NC1734.2
189300             MOVE    WRK-DS-05V00  TO COMPUTED-N                  NC1734.2
189400             MOVE    0             TO CORRECT-N                   NC1734.2
189500             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
189600                  TO RE-MARK                                      NC1734.2
189700             PERFORM FAIL                                         NC1734.2
189800             PERFORM PRINT-DETAIL.                                NC1734.2
189900     ADD     1 TO REC-CT.                                         NC1734.2
190000 DIV-TEST-F3-27-5.                                                NC1734.2
190100     MOVE   "DIV-TEST-F3-27-5" TO PAR-NAME.                       NC1734.2
190200     IF      WRK-CS-18V00 = 000000000000000099                    NC1734.2
190300             PERFORM PASS                                         NC1734.2
190400             PERFORM PRINT-DETAIL                                 NC1734.2
190500     ELSE                                                         NC1734.2
190600             MOVE    WRK-DS-18V00  TO COMPUTED-N                  NC1734.2
190700             MOVE    000000000000000099  TO CORRECT-N             NC1734.2
190800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1734.2
190900             PERFORM FAIL                                         NC1734.2
191000             PERFORM PRINT-DETAIL.                                NC1734.2
191100*                                                                 NC1734.2
191200 DIV-INIT-F3-28.                                                  NC1734.2
191300*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
191400*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1734.2
191500     MOVE   "DIV-TEST-F3-28" TO PAR-NAME.                         NC1734.2
191600     MOVE   "1V-41 6.4.3" TO ANSI-REFERENCE.                      NC1734.2
191700     MOVE    44.1  TO DIV2.                                       NC1734.2
191800     MOVE   -9.642 TO DIV4.                                       NC1734.2
191900     MOVE    0     TO DIV10.                                      NC1734.2
192000     MOVE   "0"    TO WRK-XN-00001.                               NC1734.2
192100     MOVE    0     TO WRK-DS-05V00.                               NC1734.2
192200     MOVE   "A"    TO XRAY.                                       NC1734.2
192300     MOVE    1 TO REC-CT.                                         NC1734.2
192400 DIV-TEST-F3-28-0.                                                NC1734.2
192500     DIVIDE  DIV2 BY DIV4                                         NC1734.2
192600      GIVING DIV10                                                NC1734.2
192700      NOT ON SIZE ERROR                                           NC1734.2
192800             MOVE "P" TO XRAY                                     NC1734.2
192900             MOVE "1" TO WRK-XN-00001                             NC1734.2
193000             MOVE  23 TO WRK-DS-05V00                             NC1734.2
193100     END-DIVIDE                                                   NC1734.2
193200     MOVE    99 TO WRK-CS-18V00.                                  NC1734.2
193300     GO TO DIV-TEST-F3-28-1.                                      NC1734.2
193400 DIV-DELETE-F3-28-1.                                              NC1734.2
193500     PERFORM DE-LETE.                                             NC1734.2
193600     PERFORM PRINT-DETAIL.                                        NC1734.2
193700     GO TO   DIV-INIT-F3-29.                                      NC1734.2
193800 DIV-TEST-F3-28-1.                                                NC1734.2
193900     MOVE   "DIV-TEST-F3-28-1" TO PAR-NAME.                       NC1734.2
194000     IF      DIV10   = 0                                          NC1734.2
194100             PERFORM PASS                                         NC1734.2
194200             PERFORM PRINT-DETAIL                                 NC1734.2
194300     ELSE                                                         NC1734.2
194400             MOVE    DIV10 TO COMPUTED-N                          NC1734.2
194500             MOVE    0     TO CORRECT-N                           NC1734.2
194600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1734.2
194700                  TO RE-MARK                                      NC1734.2
194800             PERFORM FAIL                                         NC1734.2
194900             PERFORM PRINT-DETAIL.                                NC1734.2
195000     ADD     1 TO REC-CT.                                         NC1734.2
195100 DIV-TEST-F3-28-2.                                                NC1734.2
195200     MOVE   "DIV-TEST-F3-28-2" TO PAR-NAME.                       NC1734.2
195300     IF      XRAY    = "A"                                        NC1734.2
195400             PERFORM PASS                                         NC1734.2
195500             PERFORM PRINT-DETAIL                                 NC1734.2
195600     ELSE                                                         NC1734.2
195700             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
195800             MOVE   "A"    TO CORRECT-X                           NC1734.2
195900             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1734.2
196000                  TO RE-MARK                                      NC1734.2
196100             PERFORM FAIL                                         NC1734.2
196200             PERFORM PRINT-DETAIL.                                NC1734.2
196300     ADD     1 TO REC-CT.                                         NC1734.2
196400 DIV-TEST-F3-28-3.                                                NC1734.2
196500     MOVE   "DIV-TEST-F3-28-3" TO PAR-NAME.                       NC1734.2
196600     IF      WRK-XN-00001 = "0"                                   NC1734.2
196700             PERFORM PASS                                         NC1734.2
196800             PERFORM PRINT-DETAIL                                 NC1734.2
196900     ELSE                                                         NC1734.2
197000             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1734.2
197100             MOVE   "0"            TO CORRECT-X                   NC1734.2
197200             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1734.2
197300                  TO RE-MARK                                      NC1734.2
197400             PERFORM FAIL                                         NC1734.2
197500             PERFORM PRINT-DETAIL.                                NC1734.2
197600     ADD     1 TO REC-CT.                                         NC1734.2
197700 DIV-TEST-F3-28-4.                                                NC1734.2
197800     MOVE   "DIV-TEST-F3-28-4" TO PAR-NAME.                       NC1734.2
197900     IF      WRK-DS-05V00 = 00000                                 NC1734.2
198000             PERFORM PASS                                         NC1734.2
198100             PERFORM PRINT-DETAIL                                 NC1734.2
198200     ELSE                                                         NC1734.2
198300             MOVE    WRK-DS-05V00  TO COMPUTED-N                  NC1734.2
198400             MOVE    00000         TO CORRECT-N                   NC1734.2
198500             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1734.2
198600                  TO RE-MARK                                      NC1734.2
198700             PERFORM FAIL                                         NC1734.2
198800             PERFORM PRINT-DETAIL.                                NC1734.2
198900     ADD     1 TO REC-CT.                                         NC1734.2
199000 DIV-TEST-F3-28-5.                                                NC1734.2
199100     MOVE   "DIV-TEST-F3-28-5" TO PAR-NAME.                       NC1734.2
199200     IF      WRK-CS-18V00 = 000000000000000099                    NC1734.2
199300             PERFORM PASS                                         NC1734.2
199400             PERFORM PRINT-DETAIL                                 NC1734.2
199500     ELSE                                                         NC1734.2
199600             MOVE    WRK-DS-18V00  TO COMPUTED-N                  NC1734.2
199700             MOVE    000000000000000099  TO CORRECT-N             NC1734.2
199800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1734.2
199900             PERFORM FAIL                                         NC1734.2
200000             PERFORM PRINT-DETAIL.                                NC1734.2
200100*                                                                 NC1734.2
200200*                                                                 NC1734.2
200300 DIV-INIT-F3-29.                                                  NC1734.2
200400*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
200500*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1734.2
200600     MOVE   "DIV-TEST-F3-29" TO PAR-NAME.                         NC1734.2
200700     MOVE   "1V-41 6.4.3" TO ANSI-REFERENCE.                      NC1734.2
200800     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1734.2
200900     MOVE   "0" TO WRK-XN-00001.                                  NC1734.2
201000     MOVE    0  TO WRK-DS-05V00.                                  NC1734.2
201100     MOVE    0  TO WRK-DS-02V00.                                  NC1734.2
201200     MOVE    0  TO WRK-CS-18V00.                                  NC1734.2
201300     MOVE    1 TO REC-CT.                                         NC1734.2
201400 DIV-TEST-F3-29-0.                                                NC1734.2
201500     DIVIDE  WRK-DS-09V09 BY A18ONES-DS-09V09                     NC1734.2
201600      GIVING WRK-DS-09V09                                         NC1734.2
201700      NOT ON SIZE ERROR                                           NC1734.2
201800             MOVE "1" TO WRK-XN-00001                             NC1734.2
201900             MOVE  23 TO WRK-DS-05V00                             NC1734.2
202000             MOVE  -4 TO WRK-DS-02V00                             NC1734.2
202100     END-DIVIDE                                                   NC1734.2
202200     MOVE    99 TO WRK-CS-18V00.                                  NC1734.2
202300     GO TO   DIV-TEST-F3-29-1.                                    NC1734.2
202400 DIV-DELETE-F3-29-1.                                              NC1734.2
202500     PERFORM DE-LETE.                                             NC1734.2
202600     PERFORM PRINT-DETAIL.                                        NC1734.2
202700     GO TO   DIV-INIT-F3-30.                                      NC1734.2
202800 DIV-TEST-F3-29-1.                                                NC1734.2
202900     MOVE   "DIV-TEST-F3-29-1" TO PAR-NAME.                       NC1734.2
203000     IF      WRK-DS-18V00-S = 000000001000000000                  NC1734.2
203100             PERFORM PASS                                         NC1734.2
203200             PERFORM PRINT-DETAIL                                 NC1734.2
203300     ELSE                                                         NC1734.2
203400             MOVE    000000001000000000 TO CORRECT-18V0           NC1734.2
203500             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1734.2
203600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
203700             PERFORM FAIL                                         NC1734.2
203800             PERFORM PRINT-DETAIL.                                NC1734.2
203900     ADD     1 TO REC-CT.                                         NC1734.2
204000 DIV-TEST-F3-29-2.                                                NC1734.2
204100     MOVE   "DIV-TEST-F3-29-2" TO PAR-NAME.                       NC1734.2
204200     IF      WRK-DS-02V00 = -4                                    NC1734.2
204300             PERFORM PASS                                         NC1734.2
204400             PERFORM PRINT-DETAIL                                 NC1734.2
204500     ELSE                                                         NC1734.2
204600             MOVE    WRK-DS-02V00  TO COMPUTED-N                  NC1734.2
204700             MOVE   -4             TO CORRECT-N                   NC1734.2
204800             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
204900                  TO RE-MARK                                      NC1734.2
205000             PERFORM FAIL                                         NC1734.2
205100             PERFORM PRINT-DETAIL.                                NC1734.2
205200     ADD     1 TO REC-CT.                                         NC1734.2
205300 DIV-TEST-F3-29-3.                                                NC1734.2
205400     MOVE   "DIV-TEST-F3-29-3" TO PAR-NAME.                       NC1734.2
205500     IF      WRK-XN-00001 = "1"                                   NC1734.2
205600             PERFORM PASS                                         NC1734.2
205700             PERFORM PRINT-DETAIL                                 NC1734.2
205800     ELSE                                                         NC1734.2
205900             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1734.2
206000             MOVE   "1"            TO CORRECT-X                   NC1734.2
206100             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
206200                  TO RE-MARK                                      NC1734.2
206300             PERFORM FAIL                                         NC1734.2
206400             PERFORM PRINT-DETAIL.                                NC1734.2
206500     ADD     1 TO REC-CT.                                         NC1734.2
206600 DIV-TEST-F3-29-4.                                                NC1734.2
206700     MOVE   "DIV-TEST-F3-29-4" TO PAR-NAME.                       NC1734.2
206800     IF      WRK-DS-05V00 = 23                                    NC1734.2
206900             PERFORM PASS                                         NC1734.2
207000             PERFORM PRINT-DETAIL                                 NC1734.2
207100     ELSE                                                         NC1734.2
207200             MOVE    WRK-DS-05V00  TO COMPUTED-N                  NC1734.2
207300             MOVE    23            TO CORRECT-N                   NC1734.2
207400             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1734.2
207500                  TO RE-MARK                                      NC1734.2
207600             PERFORM FAIL                                         NC1734.2
207700             PERFORM PRINT-DETAIL.                                NC1734.2
207800     ADD     1 TO REC-CT.                                         NC1734.2
207900 DIV-TEST-F3-29-5.                                                NC1734.2
208000     MOVE   "DIV-TEST-F3-29-5" TO PAR-NAME.                       NC1734.2
208100     IF      WRK-CS-18V00 = 000000000000000099                    NC1734.2
208200             PERFORM PASS                                         NC1734.2
208300             PERFORM PRINT-DETAIL                                 NC1734.2
208400     ELSE                                                         NC1734.2
208500             MOVE    WRK-DS-18V00  TO COMPUTED-N                  NC1734.2
208600             MOVE    000000000000000099  TO CORRECT-N             NC1734.2
208700             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1734.2
208800             PERFORM FAIL                                         NC1734.2
208900             PERFORM PRINT-DETAIL.                                NC1734.2
209000*                                                                 NC1734.2
209100 DIV-INIT-F3-30.                                                  NC1734.2
209200*            ==-->    SIZE ERROR CONDITION <--==                  NC1734.2
209300*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1734.2
209400     MOVE   "DIV-TEST-F3-30" TO PAR-NAME.                         NC1734.2
209500     MOVE   "1V-41 6.4.3" TO ANSI-REFERENCE.                      NC1734.2
209600     MOVE    44.1  TO DIV2.                                       NC1734.2
209700     MOVE   -9.642 TO DIV4.                                       NC1734.2
209800     MOVE    0     TO DIV10.                                      NC1734.2
209900     MOVE    0     TO WRK-CS-18V00.                               NC1734.2
210000     MOVE   "A"    TO XRAY.                                       NC1734.2
210100     MOVE    1     TO REC-CT.                                     NC1734.2
210200 DIV-TEST-F3-30-0.                                                NC1734.2
210300     DIVIDE  DIV2 BY DIV4                                         NC1734.2
210400      GIVING DIV10                                                NC1734.2
210500          ON SIZE ERROR                                           NC1734.2
210600             MOVE "E" TO XRAY                                     NC1734.2
210700      NOT ON SIZE ERROR                                           NC1734.2
210800             MOVE "N" TO XRAY                                     NC1734.2
210900     END-DIVIDE                                                   NC1734.2
211000     MOVE    99 TO WRK-CS-18V00.                                  NC1734.2
211100     GO TO DIV-TEST-F3-30-1.                                      NC1734.2
211200 DIV-DELETE-F3-30.                                                NC1734.2
211300     PERFORM DE-LETE.                                             NC1734.2
211400     PERFORM PRINT-DETAIL.                                        NC1734.2
211500     GO TO   DIV-INIT-F3-31.                                      NC1734.2
211600 DIV-TEST-F3-30-1.                                                NC1734.2
211700     MOVE   "DIV-TEST-F3-30-1" TO PAR-NAME.                       NC1734.2
211800     IF      DIV10   = 0                                          NC1734.2
211900             PERFORM PASS                                         NC1734.2
212000             PERFORM PRINT-DETAIL                                 NC1734.2
212100     ELSE                                                         NC1734.2
212200             MOVE    DIV10 TO COMPUTED-N                          NC1734.2
212300             MOVE    0     TO CORRECT-N                           NC1734.2
212400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1734.2
212500                  TO RE-MARK                                      NC1734.2
212600             PERFORM FAIL                                         NC1734.2
212700             PERFORM PRINT-DETAIL.                                NC1734.2
212800     ADD     1 TO REC-CT.                                         NC1734.2
212900 DIV-TEST-F3-30-2.                                                NC1734.2
213000     MOVE   "DIV-TEST-F3-30-2" TO PAR-NAME.                       NC1734.2
213100     IF      XRAY    = "E"                                        NC1734.2
213200             PERFORM PASS                                         NC1734.2
213300             PERFORM PRINT-DETAIL                                 NC1734.2
213400     ELSE                                                         NC1734.2
213500             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
213600             MOVE   "E"    TO CORRECT-X                           NC1734.2
213700             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1734.2
213800                  TO RE-MARK                                      NC1734.2
213900             PERFORM FAIL                                         NC1734.2
214000             PERFORM PRINT-DETAIL.                                NC1734.2
214100     ADD     1 TO REC-CT.                                         NC1734.2
214200 DIV-TEST-F3-30-3.                                                NC1734.2
214300     MOVE   "DIV-TEST-F3-30-3" TO PAR-NAME.                       NC1734.2
214400     IF      WRK-CS-18V00 = 000000000000000099                    NC1734.2
214500             PERFORM PASS                                         NC1734.2
214600             PERFORM PRINT-DETAIL                                 NC1734.2
214700     ELSE                                                         NC1734.2
214800             MOVE    WRK-DS-18V00  TO COMPUTED-N                  NC1734.2
214900             MOVE    000000000000000099  TO CORRECT-N             NC1734.2
215000             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1734.2
215100             PERFORM FAIL                                         NC1734.2
215200             PERFORM PRINT-DETAIL.                                NC1734.2
215300*                                                                 NC1734.2
215400 DIV-INIT-F3-31.                                                  NC1734.2
215500*            ==--> NO SIZE ERROR CONDITION <--==                  NC1734.2
215600*            ==-->EXPLICIT SCOPE TERMINATOR<--==                  NC1734.2
215700     MOVE   "DIV-TEST-F3-31" TO PAR-NAME.                         NC1734.2
215800     MOVE   "1V-41 6.4.3" TO ANSI-REFERENCE.                      NC1734.2
215900     MOVE    0 TO WRK-CS-18V00.                                   NC1734.2
216000     MOVE   "A" TO XRAY.                                          NC1734.2
216100     MOVE    1  TO REC-CT.                                        NC1734.2
216200     MOVE    A18ONES-DS-09V09 TO WRK-DS-09V09.                    NC1734.2
216300 DIV-TEST-F3-31-0.                                                NC1734.2
216400     DIVIDE  WRK-DS-09V09 BY A18ONES-DS-09V09                     NC1734.2
216500      GIVING WRK-DS-09V09                                         NC1734.2
216600          ON SIZE ERROR                                           NC1734.2
216700             MOVE "E" TO XRAY                                     NC1734.2
216800      NOT ON SIZE ERROR                                           NC1734.2
216900             MOVE "N" TO XRAY                                     NC1734.2
217000     END-DIVIDE                                                   NC1734.2
217100     MOVE    99 TO WRK-CS-18V00.                                  NC1734.2
217200     GO TO   DIV-TEST-F3-31-1.                                    NC1734.2
217300 DIV-DELETE-F3-31.                                                NC1734.2
217400     PERFORM DE-LETE.                                             NC1734.2
217500     PERFORM PRINT-DETAIL.                                        NC1734.2
217600     GO TO   CCVS-EXIT.                                           NC1734.2
217700 DIV-TEST-F3-31-1.                                                NC1734.2
217800     MOVE   "DIV-TEST-F3-31-1" TO PAR-NAME.                       NC1734.2
217900     IF      WRK-DS-18V00-S = 000000001000000000                  NC1734.2
218000             PERFORM PASS                                         NC1734.2
218100             PERFORM PRINT-DETAIL                                 NC1734.2
218200     ELSE                                                         NC1734.2
218300             MOVE    000000001000000000 TO CORRECT-18V0           NC1734.2
218400             MOVE    WRK-DS-18V00-S     TO COMPUTED-18V0          NC1734.2
218500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1734.2
218600             PERFORM FAIL                                         NC1734.2
218700             PERFORM PRINT-DETAIL.                                NC1734.2
218800     ADD     1 TO REC-CT.                                         NC1734.2
218900 DIV-TEST-F3-31-2.                                                NC1734.2
219000     MOVE   "DIV-TEST-F3-31-2" TO PAR-NAME.                       NC1734.2
219100     IF      XRAY    = "N"                                        NC1734.2
219200             PERFORM PASS                                         NC1734.2
219300             PERFORM PRINT-DETAIL                                 NC1734.2
219400     ELSE                                                         NC1734.2
219500             MOVE    XRAY  TO COMPUTED-X                          NC1734.2
219600             MOVE   "N"    TO CORRECT-X                           NC1734.2
219700             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1734.2
219800                  TO RE-MARK                                      NC1734.2
219900             PERFORM FAIL                                         NC1734.2
220000             PERFORM PRINT-DETAIL.                                NC1734.2
220100     ADD     1 TO REC-CT.                                         NC1734.2
220200 DIV-TEST-F3-31-3.                                                NC1734.2
220300     MOVE   "DIV-TEST-F3-31-3" TO PAR-NAME.                       NC1734.2
220400     IF      WRK-CS-18V00 = 000000000000000099                    NC1734.2
220500             PERFORM PASS                                         NC1734.2
220600             PERFORM PRINT-DETAIL                                 NC1734.2
220700     ELSE                                                         NC1734.2
220800             MOVE    WRK-DS-18V00  TO COMPUTED-N                  NC1734.2
220900             MOVE    000000000000000099  TO CORRECT-N             NC1734.2
221000             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1734.2
221100             PERFORM FAIL                                         NC1734.2
221200             PERFORM PRINT-DETAIL.                                NC1734.2
221300*                                                                 NC1734.2
221400*                                                                 NC1734.2
221500*                                                                 NC1734.2
221600 CCVS-EXIT SECTION.                                               NC1734.2
221700 CCVS-999999.                                                     NC1734.2
221800     GO TO CLOSE-FILES.                                           NC1734.2
      *END-OF,NC173A                                                                  
