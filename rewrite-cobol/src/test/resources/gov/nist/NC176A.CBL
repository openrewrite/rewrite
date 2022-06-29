      *HEADER,COBOL,NC176A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1764.2
000200 PROGRAM-ID.                                                      NC1764.2
000300     NC176A.                                                      NC1764.2
000400****************************************************************  NC1764.2
000500*                                                              *  NC1764.2
000600*    VALIDATION FOR:-                                          *  NC1764.2
000700*                                                              *  NC1764.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1764.2
000900*                                                              *  NC1764.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1764.2
001100*                                                              *  NC1764.2
001200****************************************************************  NC1764.2
001300*                                                              *  NC1764.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1764.2
001500*                                                              *  NC1764.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1764.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1764.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1764.2
001900*                                                              *  NC1764.2
002000****************************************************************  NC1764.2
002100*                                                                 NC1764.2
002200*    PROGRAM NC176A TESTS FORMAT 1 OF THE ADD STATEMENT.          NC1764.2
002300*    VARIOUS COMBINATINS OF DATA-ITEMS AND ALL                    NC1764.2
002400*    OPTIONAL PHRASES ARE TESTED.                                 NC1764.2
002500*                                                                 NC1764.2
002600 ENVIRONMENT DIVISION.                                            NC1764.2
002700 CONFIGURATION SECTION.                                           NC1764.2
002800 SOURCE-COMPUTER.                                                 NC1764.2
002900     XXXXX082.                                                    NC1764.2
003000 OBJECT-COMPUTER.                                                 NC1764.2
003100     XXXXX083.                                                    NC1764.2
003200 INPUT-OUTPUT SECTION.                                            NC1764.2
003300 FILE-CONTROL.                                                    NC1764.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1764.2
003500     XXXXX055.                                                    NC1764.2
003600 DATA DIVISION.                                                   NC1764.2
003700 FILE SECTION.                                                    NC1764.2
003800 FD  PRINT-FILE.                                                  NC1764.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1764.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1764.2
004100 WORKING-STORAGE SECTION.                                         NC1764.2
004200 01  42-DATANAMES.                                                NC1764.2
004300     02  DNAME1   PICTURE 9       VALUE 1  COMPUTATIONAL.         NC1764.2
004400     02  DNAME2   PICTURE 99      VALUE 1  COMPUTATIONAL.         NC1764.2
004500     02  DNAME3   PICTURE 999     VALUE 1  COMPUTATIONAL.         NC1764.2
004600     02  DNAME4   PICTURE 9(4)    VALUE 1  COMPUTATIONAL.         NC1764.2
004700     02  DNAME5   PICTURE 9(5)    VALUE 1  COMPUTATIONAL.         NC1764.2
004800     02  DNAME6   PICTURE 9(6)    VALUE 1  COMPUTATIONAL.         NC1764.2
004900     02  DNAME7   PICTURE 9(7)    VALUE 1  COMPUTATIONAL.         NC1764.2
005000     02  DNAME8   PICTURE 9(8)    VALUE 1  COMPUTATIONAL.         NC1764.2
005100     02  DNAME9   PICTURE 9(9)    VALUE 1  COMPUTATIONAL.         NC1764.2
005200     02  DNAME10  PICTURE 9(10)   VALUE 1.                        NC1764.2
005300     02  DNAME11  PICTURE 9(11)   VALUE 1.                        NC1764.2
005400     02  DNAME12  PICTURE 9(12)   VALUE 1.                        NC1764.2
005500     02  DNAME13  PICTURE 9(13)   VALUE 1.                        NC1764.2
005600     02  DNAME14  PICTURE 9(14)   VALUE 1.                        NC1764.2
005700     02  DNAME15  PICTURE 9(15)   VALUE 1.                        NC1764.2
005800     02  DNAME16  PICTURE 9(16)   VALUE 1.                        NC1764.2
005900     02  DNAME17  PICTURE 9(17)   VALUE 1.                        NC1764.2
006000     02  DNAME18  PICTURE 9(18)   VALUE 1.                        NC1764.2
006100     02  DNAME19  PICTURE 9       VALUE 1.                        NC1764.2
006200     02  DNAME20  PICTURE 99      VALUE 1.                        NC1764.2
006300     02  DNAME21  PICTURE 999     VALUE 1.                        NC1764.2
006400     02  DNAME22  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
006500     02  DNAME23  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
006600     02  DNAME24  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
006700     02  DNAME25  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
006800     02  DNAME26  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
006900     02  DNAME27  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
007000     02  DNAME28  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
007100     02  DNAME29  PICTURE 9(18)  VALUE ZERO.                      NC1764.2
007200     02  DNAME30   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007300     02  DNAME31   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007400     02  DNAME32   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007500     02  DNAME33   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007600     02  DNAME34   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007700     02  DNAME35   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007800     02  DNAME36   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
007900     02  DNAME37   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
008000     02  DNAME38   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
008100     02  DNAME39   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
008200     02  DNAME40   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
008300     02  DNAME41   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
008400     02  DNAME42   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1764.2
008500 77  SIZE-ERR                     PICTURE X  VALUE SPACE.         NC1764.2
008600 77  SIZE-ERR2                    PICTURE X  VALUE SPACE.         NC1764.2
008700 77  SIZE-ERR3                    PICTURE X  VALUE SPACE.         NC1764.2
008800 77  SIZE-ERR4                    PICTURE X  VALUE SPACE.         NC1764.2
008900 77  A18TWOS-DS-18V00             PICTURE S9(18)                  NC1764.2
009000                                  VALUE 222222222222222222.       NC1764.2
009100 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC1764.2
009200                                  VALUE 111111111111111111.       NC1764.2
009300 77  WRK-DS-10V00                 PICTURE S9(10).                 NC1764.2
009400 77  A17TWOS-DS-17V00             PICTURE S9(17)                  NC1764.2
009500                                  VALUE 22222222222222222.        NC1764.2
009600 77  A10ONES-DS-10V00             PICTURE S9(10)                  NC1764.2
009700                                  VALUE 1111111111.               NC1764.2
009800 77  A05ONES-DS-05V00             PICTURE S9(5)                   NC1764.2
009900                                  VALUE 11111.                    NC1764.2
010000 77  A02ONES-DS-02V00             PICTURE S99                     NC1764.2
010100                                  VALUE 11.                       NC1764.2
010200 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1764.2
010300 77  WRK-DS-18V00 REDEFINES WRK-DS-09V09                          NC1764.2
010400                                  PICTURE S9(18).                 NC1764.2
010500 77  A06THREES-DS-03V03           PICTURE S999V999                NC1764.2
010600                                  VALUE 333.333.                  NC1764.2
010700 77  A12THREES-DS-06V06           PICTURE S9(6)V9(6)              NC1764.2
010800                                  VALUE 333333.333333.            NC1764.2
010900 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC1764.2
011000 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC1764.2
011100                                  PICTURE S9(12).                 NC1764.2
011200 77  A05ONES-DS-00V05             PICTURE SV9(5)                  NC1764.2
011300                                  VALUE .11111.                   NC1764.2
011400 77  WRK-DS-05V00                 PICTURE S9(5).                  NC1764.2
011500 77  WRK-DS-02V00                 PICTURE S99.                    NC1764.2
011600 77  A12ONES-DS-12V00             PICTURE S9(12)                  NC1764.2
011700                                  VALUE 111111111111.             NC1764.2
011800 77  WRK-DS-03V10                 PICTURE S999V9(10).             NC1764.2
011900 77  WRK-DS-13V00-S REDEFINES WRK-DS-03V10                        NC1764.2
012000                                  PICTURE S9(13).                 NC1764.2
012100 77  A99-DS-02V00                 PICTURE S99                     NC1764.2
012200                                  VALUE 99.                       NC1764.2
012300 77  A03ONES-DS-02V01             PICTURE S99V9                   NC1764.2
012400                                  VALUE 11.1.                     NC1764.2
012500 77  A06ONES-DS-03V03             PICTURE S999V999                NC1764.2
012600                                  VALUE 111.111.                  NC1764.2
012700 77  A08TWOS-DS-02V06             PICTURE S99V9(6)                NC1764.2
012800                                  VALUE 22.222222.                NC1764.2
012900 77  A01ONE-DS-P0801              PICTURE SP(8)9                  NC1764.2
013000                                  VALUE .000000001.               NC1764.2
013100 77  WRK-CS-18V00                 PICTURE S9(18) COMPUTATIONAL.   NC1764.2
013200 77  A18ONES-CS-18V00             PICTURE S9(18) COMPUTATIONAL    NC1764.2
013300                                  VALUE 111111111111111111.       NC1764.2
013400 77  WRK-CS-02V02                 PICTURE S99V99 COMPUTATIONAL.   NC1764.2
013500 77  A99-CS-02V00                 PICTURE S99 COMPUTATIONAL       NC1764.2
013600                                  VALUE 99.                       NC1764.2
013700 77  WRK-DS-0201P                 PICTURE S99P.                   NC1764.2
013800 77  WRK-DS-06V00                 PICTURE S9(6).                  NC1764.2
013900 77  AZERO-DS-05V05               PICTURE S9(5)V9(5)              NC1764.2
014000                                  VALUE ZERO.                     NC1764.2
014100 77  NDATA-DS-09V09               PICTURE S9(9)V9(9)              NC1764.2
014200                                  VALUE +012345678.876543210.     NC1764.2
014300 77  XDATA-XN-00018               PICTURE X(18)                   NC1764.2
014400                                  VALUE "00ABCDEFGHI  4321 ".     NC1764.2
014500 77  WRK-XN-00018                 PICTURE X(18).                  NC1764.2
014600 77  WRK-XN-00001                 PICTURE X.                      NC1764.2
014700 77  ADD-12    PICTURE PP9        VALUE .001.                     NC1764.2
014800 77  ADD-13    PICTURE 9PP        VALUE 100.                      NC1764.2
014900 77  ADD-14    PICTURE 999V999.                                   NC1764.2
015000 77  WRK-CU-18V00      PICTURE 9(18) VALUE ZERO COMPUTATIONAL.    NC1764.2
015100 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1764.2
015200 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1764.2
015300 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1764.2
015400 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1764.2
015500 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1764.2
015600 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1764.2
015700 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1764.2
015800 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1764.2
015900 01  WRK-DU-2V0-1 PIC 99.                                         NC1764.2
016000 01  WRK-DU-2V0-2 PIC 99.                                         NC1764.2
016100 01  WRK-DU-2V0-3 PIC 99.                                         NC1764.2
016200 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1764.2
016300 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1764.2
016400 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1764.2
016500 77  A18EIGHTS-CS-18V00 PICTURE S9(18) VALUE -888888888888888888  NC1764.2
016600             COMPUTATIONAL.                                       NC1764.2
016700 77  A14TWOS-CS-18V00  PICTURE S9(18) VALUE -000022222222222222   NC1764.2
016800             COMPUTATIONAL.                                       NC1764.2
016900 77  A12THREES-CU-18V00 PICTURE 9(18) VALUE 000000333333333333    NC1764.2
017000             COMPUTATIONAL.                                       NC1764.2
017100 77  A16FOURS-CS-18V00 PICTURE S9(18) VALUE 004444444444444444    NC1764.2
017200             COMPUTATIONAL.                                       NC1764.2
017300 77  A18FIVES-CS-18V00 PICTURE S9(18) VALUE -555555555555555555   NC1764.2
017400             COMPUTATIONAL.                                       NC1764.2
017500 77  A18SIXES-CS-18V00 PICTURE S9(18) VALUE 666666666666666666    NC1764.2
017600             COMPUTATIONAL.                                       NC1764.2
017700 77  A12SEVENS-CU-18V00 PICTURE 9(18) VALUE 000000777777777777    NC1764.2
017800             COMPUTATIONAL.                                       NC1764.2
017900 77  WRK-DU-18V00      PICTURE 9(18) VALUE ZERO.                  NC1764.2
018000 77  A18THREES-CS-18V00 PICTURE S9(18) VALUE -333333333333333333  NC1764.2
018100             COMPUTATIONAL.                                       NC1764.2
018200 77  WRK-CS-03V00 PICTURE S999 COMPUTATIONAL SYNCHRONIZED RIGHT.  NC1764.2
018300 01  SUBTRACT-DATA.                                               NC1764.2
018400     02  SUBTR-1    PICTURE 9         VALUE 1.                    NC1764.2
018500     02  SUBTR-2    PICTURE S99       VALUE 99.                   NC1764.2
018600     02  SUBTR-3    PICTURE S9V99     VALUE -1.                   NC1764.2
018700     02  SUBTR-4    PICTURE SPP9      VALUE .001.                 NC1764.2
018800     02  SUBTR-5    PICTURE S9PP      VALUE 100.                  NC1764.2
018900     02  SUBTR-6    PICTURE 9  VALUE 1.                           NC1764.2
019000     02  SUBTR-7    PICTURE S99  VALUE 99.                        NC1764.2
019100     02  SUBTR-8    PICTURE S9V99  VALUE -9.99.                   NC1764.2
019200     02  SUBTR-10    PICTURE S999  VALUE 100.                     NC1764.2
019300     02  SUBTR-11   PICTURE S999V999.                             NC1764.2
019400 01  N-3                                PICTURE IS 99999.         NC1764.2
019500 01  N-4                                PICTURE IS 9(5)           NC1764.2
019600     VALUE IS 52800.                                              NC1764.2
019700 01  N-5                                PICTURE IS S9(9)V99       NC1764.2
019800     VALUE IS 000000001.00.                                       NC1764.2
019900 01  N-7                                PICTURE IS S9(7)V9(4)     NC1764.2
020000     VALUE IS 0000001.0000.                                       NC1764.2
020100 01  X-8 REDEFINES N-7                  PICTURE IS X(11).         NC1764.2
020200 01  N-10                               PICTURE IS S99999V        NC1764.2
020300     VALUE IS -00001.                                             NC1764.2
020400 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1764.2
020500 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1764.2
020600 01  N-13                               PICTURE IS 9(5)           NC1764.2
020700     VALUE IS 99999.                                              NC1764.2
020800 01  N-14                               PICTURE IS 9 VALUE IS 1.  NC1764.2
020900 01  N-15                               PICTURE IS 9(16).         NC1764.2
021000 01  N-16                               PICTURE IS S999999V99     NC1764.2
021100     VALUE IS 5.90.                                               NC1764.2
021200 01  N-17                               PICTURE IS S9(3)V99       NC1764.2
021300     VALUE IS +3.6.                                               NC1764.2
021400 01  N-18                               PICTURE IS S9(10)         NC1764.2
021500     VALUE IS -5.                                                 NC1764.2
021600 01  N-19                               PICTURE IS $9.00.         NC1764.2
021700 01  N-20                               PICTURE IS S9(9)          NC1764.2
021800     VALUE IS -999999999.                                         NC1764.2
021900 01  N-21                               PICTURE IS 9 VALUE IS 5.  NC1764.2
022000 01  N-22                               PICTURE IS 999V99         NC1764.2
022100     VALUE IS 005.55.                                             NC1764.2
022200 01  N-23                               PICTURE IS $$$.99CR.      NC1764.2
022300 01  N-25                               PICTURE IS 9 VALUE IS 1.  NC1764.2
022400 01  N-26               PICTURE 9(5).                             NC1764.2
022500 01  N-27                               PICTURE IS 9999V9         NC1764.2
022600     VALUE IS 9999.9.                                             NC1764.2
022700 01  N-28                               PICTURE IS $9999.00.      NC1764.2
022800 01  N-40                               PICTURE IS 9(7)           NC1764.2
022900     VALUE IS 7777777.                                            NC1764.2
023000 01  N-41                               PICTURE IS 9(7)           NC1764.2
023100     VALUE IS 1111111.                                            NC1764.2
023200 01  N-42                               PICTURE IS 9(3)P(4).      NC1764.2
023300 01  TRUNC-DATA.                                                  NC1764.2
023400     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1764.2
023500     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1764.2
023600     02 N-45  PICTURE S9.                                         NC1764.2
023700 01  MINUS-NAMES.                                                 NC1764.2
023800     02  MINUS-NAME1 PICTURE S9(18) VALUE -999999999999999999.    NC1764.2
023900     02  MINUS-NAME2 PICTURE S9(18) VALUE -999999999999999999.    NC1764.2
024000     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC1764.2
024100     02  PLUS-NAME1  PICTURE S9(18) VALUE +999999999999999999.    NC1764.2
024200     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC1764.2
024300     02  MINUS-NAME3 PICTURE SV9(18) VALUE -.999999999999999999.  NC1764.2
024400     02  MINUS-NAME4 PICTURE SV9(18) VALUE -.999999999999999999.  NC1764.2
024500     02  EVEN-NAME2  PICTURE SV9(18)  VALUE +.1.                  NC1764.2
024600     02  PLUS-NAME3  PICTURE SV9(18) VALUE +.999999999999999999.  NC1764.2
024700     02  PLUS-NAME4  PICTURE SV9(18) VALUE +.999999999999999999.  NC1764.2
024800     02  WHOLE-FIELD   PICTURE S9(18).                            NC1764.2
024900     02  DECMAL-FIELD  PICTURE SV9(18).                           NC1764.2
025000 01  TEST-RESULTS.                                                NC1764.2
025100     02 FILLER                   PIC X      VALUE SPACE.          NC1764.2
025200     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1764.2
025300     02 FILLER                   PIC X      VALUE SPACE.          NC1764.2
025400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1764.2
025500     02 FILLER                   PIC X      VALUE SPACE.          NC1764.2
025600     02  PAR-NAME.                                                NC1764.2
025700       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1764.2
025800       03  PARDOT-X              PIC X      VALUE SPACE.          NC1764.2
025900       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1764.2
026000     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1764.2
026100     02 RE-MARK                  PIC X(61).                       NC1764.2
026200 01  TEST-COMPUTED.                                               NC1764.2
026300     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1764.2
026400     02 FILLER                   PIC X(17)  VALUE                 NC1764.2
026500            "       COMPUTED=".                                   NC1764.2
026600     02 COMPUTED-X.                                               NC1764.2
026700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1764.2
026800     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1764.2
026900                                 PIC -9(9).9(9).                  NC1764.2
027000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1764.2
027100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1764.2
027200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1764.2
027300     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1764.2
027400         04 COMPUTED-18V0                    PIC -9(18).          NC1764.2
027500         04 FILLER                           PIC X.               NC1764.2
027600     03 FILLER PIC X(50) VALUE SPACE.                             NC1764.2
027700 01  TEST-CORRECT.                                                NC1764.2
027800     02 FILLER PIC X(30) VALUE SPACE.                             NC1764.2
027900     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1764.2
028000     02 CORRECT-X.                                                NC1764.2
028100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1764.2
028200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1764.2
028300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1764.2
028400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1764.2
028500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1764.2
028600     03      CR-18V0 REDEFINES CORRECT-A.                         NC1764.2
028700         04 CORRECT-18V0                     PIC -9(18).          NC1764.2
028800         04 FILLER                           PIC X.               NC1764.2
028900     03 FILLER PIC X(2) VALUE SPACE.                              NC1764.2
029000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1764.2
029100 01  CCVS-C-1.                                                    NC1764.2
029200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1764.2
029300-    "SS  PARAGRAPH-NAME                                          NC1764.2
029400-    "       REMARKS".                                            NC1764.2
029500     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1764.2
029600 01  CCVS-C-2.                                                    NC1764.2
029700     02 FILLER                     PIC X        VALUE SPACE.      NC1764.2
029800     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1764.2
029900     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1764.2
030000     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1764.2
030100     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1764.2
030200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1764.2
030300 01  REC-CT                        PIC 99       VALUE ZERO.       NC1764.2
030400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1764.2
030500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1764.2
030600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1764.2
030700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1764.2
030800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1764.2
030900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1764.2
031000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1764.2
031100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1764.2
031200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1764.2
031300 01  CCVS-H-1.                                                    NC1764.2
031400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1764.2
031500     02  FILLER                    PIC X(42)    VALUE             NC1764.2
031600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1764.2
031700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1764.2
031800 01  CCVS-H-2A.                                                   NC1764.2
031900   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1764.2
032000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1764.2
032100   02  FILLER                        PIC XXXX   VALUE             NC1764.2
032200     "4.2 ".                                                      NC1764.2
032300   02  FILLER                        PIC X(28)  VALUE             NC1764.2
032400            " COPY - NOT FOR DISTRIBUTION".                       NC1764.2
032500   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1764.2
032600                                                                  NC1764.2
032700 01  CCVS-H-2B.                                                   NC1764.2
032800   02  FILLER                        PIC X(15)  VALUE             NC1764.2
032900            "TEST RESULT OF ".                                    NC1764.2
033000   02  TEST-ID                       PIC X(9).                    NC1764.2
033100   02  FILLER                        PIC X(4)   VALUE             NC1764.2
033200            " IN ".                                               NC1764.2
033300   02  FILLER                        PIC X(12)  VALUE             NC1764.2
033400     " HIGH       ".                                              NC1764.2
033500   02  FILLER                        PIC X(22)  VALUE             NC1764.2
033600            " LEVEL VALIDATION FOR ".                             NC1764.2
033700   02  FILLER                        PIC X(58)  VALUE             NC1764.2
033800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1764.2
033900 01  CCVS-H-3.                                                    NC1764.2
034000     02  FILLER                      PIC X(34)  VALUE             NC1764.2
034100            " FOR OFFICIAL USE ONLY    ".                         NC1764.2
034200     02  FILLER                      PIC X(58)  VALUE             NC1764.2
034300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1764.2
034400     02  FILLER                      PIC X(28)  VALUE             NC1764.2
034500            "  COPYRIGHT   1985 ".                                NC1764.2
034600 01  CCVS-E-1.                                                    NC1764.2
034700     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1764.2
034800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1764.2
034900     02 ID-AGAIN                     PIC X(9).                    NC1764.2
035000     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1764.2
035100 01  CCVS-E-2.                                                    NC1764.2
035200     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1764.2
035300     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1764.2
035400     02 CCVS-E-2-2.                                               NC1764.2
035500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1764.2
035600         03 FILLER                   PIC X      VALUE SPACE.      NC1764.2
035700         03 ENDER-DESC               PIC X(44)  VALUE             NC1764.2
035800            "ERRORS ENCOUNTERED".                                 NC1764.2
035900 01  CCVS-E-3.                                                    NC1764.2
036000     02  FILLER                      PIC X(22)  VALUE             NC1764.2
036100            " FOR OFFICIAL USE ONLY".                             NC1764.2
036200     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1764.2
036300     02  FILLER                      PIC X(58)  VALUE             NC1764.2
036400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1764.2
036500     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1764.2
036600     02 FILLER                       PIC X(15)  VALUE             NC1764.2
036700             " COPYRIGHT 1985".                                   NC1764.2
036800 01  CCVS-E-4.                                                    NC1764.2
036900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1764.2
037000     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1764.2
037100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1764.2
037200     02 FILLER                       PIC X(40)  VALUE             NC1764.2
037300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1764.2
037400 01  XXINFO.                                                      NC1764.2
037500     02 FILLER                       PIC X(19)  VALUE             NC1764.2
037600            "*** INFORMATION ***".                                NC1764.2
037700     02 INFO-TEXT.                                                NC1764.2
037800       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1764.2
037900       04 XXCOMPUTED                 PIC X(20).                   NC1764.2
038000       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1764.2
038100       04 XXCORRECT                  PIC X(20).                   NC1764.2
038200     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1764.2
038300 01  HYPHEN-LINE.                                                 NC1764.2
038400     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1764.2
038500     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1764.2
038600-    "*****************************************".                 NC1764.2
038700     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1764.2
038800-    "******************************".                            NC1764.2
038900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1764.2
039000     "NC176A".                                                    NC1764.2
039100 PROCEDURE DIVISION.                                              NC1764.2
039200 CCVS1 SECTION.                                                   NC1764.2
039300 OPEN-FILES.                                                      NC1764.2
039400     OPEN     OUTPUT PRINT-FILE.                                  NC1764.2
039500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1764.2
039600     MOVE    SPACE TO TEST-RESULTS.                               NC1764.2
039700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1764.2
039800     GO TO CCVS1-EXIT.                                            NC1764.2
039900 CLOSE-FILES.                                                     NC1764.2
040000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1764.2
040100 TERMINATE-CCVS.                                                  NC1764.2
040200*    EXIT PROGRAM.                                                NC1764.2
040300*TERMINATE-CALL.                                                  NC1764.2
040400     STOP     RUN.                                                NC1764.2
040500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1764.2
040600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1764.2
040700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1764.2
040800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1764.2
040900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1764.2
041000 PRINT-DETAIL.                                                    NC1764.2
041100     IF REC-CT NOT EQUAL TO ZERO                                  NC1764.2
041200             MOVE "." TO PARDOT-X                                 NC1764.2
041300             MOVE REC-CT TO DOTVALUE.                             NC1764.2
041400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1764.2
041500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1764.2
041600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1764.2
041700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1764.2
041800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1764.2
041900     MOVE SPACE TO CORRECT-X.                                     NC1764.2
042000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1764.2
042100     MOVE     SPACE TO RE-MARK.                                   NC1764.2
042200 HEAD-ROUTINE.                                                    NC1764.2
042300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1764.2
042400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1764.2
042500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1764.2
042600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1764.2
042700 COLUMN-NAMES-ROUTINE.                                            NC1764.2
042800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1764.2
042900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1764.2
043000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1764.2
043100 END-ROUTINE.                                                     NC1764.2
043200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1764.2
043300 END-RTN-EXIT.                                                    NC1764.2
043400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1764.2
043500 END-ROUTINE-1.                                                   NC1764.2
043600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1764.2
043700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1764.2
043800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1764.2
043900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1764.2
044000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1764.2
044100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1764.2
044200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1764.2
044300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1764.2
044400  END-ROUTINE-12.                                                 NC1764.2
044500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1764.2
044600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1764.2
044700         MOVE "NO " TO ERROR-TOTAL                                NC1764.2
044800         ELSE                                                     NC1764.2
044900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1764.2
045000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1764.2
045100     PERFORM WRITE-LINE.                                          NC1764.2
045200 END-ROUTINE-13.                                                  NC1764.2
045300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1764.2
045400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1764.2
045500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1764.2
045600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1764.2
045700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1764.2
045800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1764.2
045900          MOVE "NO " TO ERROR-TOTAL                               NC1764.2
046000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1764.2
046100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1764.2
046200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1764.2
046300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1764.2
046400 WRITE-LINE.                                                      NC1764.2
046500     ADD 1 TO RECORD-COUNT.                                       NC1764.2
046600*    IF RECORD-COUNT GREATER 42                                   NC1764.2
046700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1764.2
046800*        MOVE SPACE TO DUMMY-RECORD                               NC1764.2
046900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1764.2
047000*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1764.2
047100*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1764.2
047200*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1764.2
047300*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1764.2
047400*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1764.2
047500*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1764.2
047600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1764.2
047700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1764.2
047800*        MOVE ZERO TO RECORD-COUNT.                               NC1764.2
047900     PERFORM WRT-LN.                                              NC1764.2
048000 WRT-LN.                                                          NC1764.2
048100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1764.2
048200     MOVE SPACE TO DUMMY-RECORD.                                  NC1764.2
048300 BLANK-LINE-PRINT.                                                NC1764.2
048400     PERFORM WRT-LN.                                              NC1764.2
048500 FAIL-ROUTINE.                                                    NC1764.2
048600     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1764.2
048700            GO TO FAIL-ROUTINE-WRITE.                             NC1764.2
048800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1764.2
048900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1764.2
049000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1764.2
049100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1764.2
049200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1764.2
049300     GO TO  FAIL-ROUTINE-EX.                                      NC1764.2
049400 FAIL-ROUTINE-WRITE.                                              NC1764.2
049500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1764.2
049600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1764.2
049700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1764.2
049800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1764.2
049900 FAIL-ROUTINE-EX. EXIT.                                           NC1764.2
050000 BAIL-OUT.                                                        NC1764.2
050100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1764.2
050200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1764.2
050300 BAIL-OUT-WRITE.                                                  NC1764.2
050400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1764.2
050500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1764.2
050600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1764.2
050700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1764.2
050800 BAIL-OUT-EX. EXIT.                                               NC1764.2
050900 CCVS1-EXIT.                                                      NC1764.2
051000     EXIT.                                                        NC1764.2
051100 SECT-NC176A-001 SECTION.                                         NC1764.2
051200 ADD-INIT-F1-1.                                                   NC1764.2
051300     MOVE "ADD" TO FEATURE.                                       NC1764.2
051400     MOVE "VI-74 6.6.4 GR4" TO ANSI-REFERENCE.                    NC1764.2
051500 ADD-TEST-F1-1.                                                   NC1764.2
051600     ADD      N-5 TO N-7.                                         NC1764.2
051700     IF       N-7 IS EQUAL TO 2                                   NC1764.2
051800              PERFORM PASS                                        NC1764.2
051900              GO TO ADD-WRITE-F1-1.                               NC1764.2
052000     GO TO    ADD-FAIL-F1-1.                                      NC1764.2
052100 ADD-DELETE-F1-1.                                                 NC1764.2
052200     PERFORM  DE-LETE.                                            NC1764.2
052300     GO TO    ADD-WRITE-F1-1.                                     NC1764.2
052400 ADD-FAIL-F1-1.                                                   NC1764.2
052500     MOVE N-7 TO COMPUTED-N.                                      NC1764.2
052600     MOVE     2 TO CORRECT-N.                                     NC1764.2
052700     PERFORM  FAIL.                                               NC1764.2
052800 ADD-WRITE-F1-1.                                                  NC1764.2
052900     MOVE "ADD-TEST-F1-1 " TO PAR-NAME.                           NC1764.2
053000     PERFORM  PRINT-DETAIL.                                       NC1764.2
053100 ADD-TEST-F1-2.                                                   NC1764.2
053200     ADD      -.6 TO N-10 ROUNDED.                                NC1764.2
053300     IF       N-10 EQUAL TO -2                                    NC1764.2
053400              PERFORM PASS                                        NC1764.2
053500              GO TO ADD-WRITE-F1-2.                               NC1764.2
053600     GO TO    ADD-FAIL-F1-2.                                      NC1764.2
053700 ADD-DELETE-F1-2.                                                 NC1764.2
053800     PERFORM  DE-LETE.                                            NC1764.2
053900     GO TO    ADD-WRITE-F1-2.                                     NC1764.2
054000 ADD-FAIL-F1-2.                                                   NC1764.2
054100     MOVE     N-10 TO COMPUTED-N.                                 NC1764.2
054200     MOVE     -2 TO CORRECT-N.                                    NC1764.2
054300     PERFORM  FAIL.                                               NC1764.2
054400 ADD-WRITE-F1-2.                                                  NC1764.2
054500     MOVE "ADD-TEST-F1-2 " TO PAR-NAME.                           NC1764.2
054600     PERFORM  PRINT-DETAIL.                                       NC1764.2
054700     MOVE     -2 TO N-10.                                         NC1764.2
054800 ADD-TEST-F1-3-0.                                                 NC1764.2
054900     ADD      N-11 TO N-12 ON SIZE ERROR                          NC1764.2
055000              PERFORM PASS                                        NC1764.2
055100              GO TO ADD-WRITE-F1-3.                               NC1764.2
055200     GO TO    ADD-FAIL-F1-3.                                      NC1764.2
055300 ADD-DELETE-F1-3.                                                 NC1764.2
055400     PERFORM  DE-LETE.                                            NC1764.2
055500     GO TO    ADD-WRITE-F1-3.                                     NC1764.2
055600 ADD-FAIL-F1-3.                                                   NC1764.2
055700     MOVE     N-12 TO COMPUTED-N.                                 NC1764.2
055800     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1764.2
055900     PERFORM  FAIL.                                               NC1764.2
056000 ADD-WRITE-F1-3.                                                  NC1764.2
056100     MOVE "ADD-TEST-F1-3 " TO PAR-NAME.                           NC1764.2
056200     PERFORM  PRINT-DETAIL.                                       NC1764.2
056300 ADD-TEST-F1-4-1.                                                 NC1764.2
056400     ADD      1.5 TO N-13 ROUNDED ON SIZE ERROR                   NC1764.2
056500              PERFORM PASS                                        NC1764.2
056600              GO TO ADD-WRITE-F1-4-1.                             NC1764.2
056700*    NOTE WHEN SIZE ERROR CONDITION OCCURS, VALUE OF              NC1764.2
056800*    N-13 SHOULD NOT BE CHANGED.                                  NC1764.2
056900     GO TO    ADD-FAIL-F1-4-1.                                    NC1764.2
057000 ADD-DELETE-F1-4-1.                                               NC1764.2
057100     PERFORM  DE-LETE.                                            NC1764.2
057200     GO TO    ADD-WRITE-F1-4-1.                                   NC1764.2
057300 ADD-FAIL-F1-4-1.                                                 NC1764.2
057400     MOVE     N-13 TO COMPUTED-N.                                 NC1764.2
057500     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1764.2
057600     PERFORM  FAIL.                                               NC1764.2
057700 ADD-WRITE-F1-4-1.                                                NC1764.2
057800     MOVE "ADD-TEST-F1-4-1 " TO PAR-NAME.                         NC1764.2
057900     PERFORM  PRINT-DETAIL.                                       NC1764.2
058000 ADD-TEST-F1-4-2.                                                 NC1764.2
058100     IF       N-13 IS EQUAL TO 99999                              NC1764.2
058200              PERFORM PASS                                        NC1764.2
058300              GO TO ADD-WRITE-F1-4-2.                             NC1764.2
058400     GO TO    ADD-FAIL-F1-4-2.                                    NC1764.2
058500 ADD-DELETE-F1-4-2.                                               NC1764.2
058600     PERFORM  DE-LETE.                                            NC1764.2
058700     GO TO    ADD-WRITE-F1-4-2.                                   NC1764.2
058800 ADD-FAIL-F1-4-2.                                                 NC1764.2
058900     MOVE     N-13 TO COMPUTED-N.                                 NC1764.2
059000     MOVE     99999 TO CORRECT-N.                                 NC1764.2
059100     PERFORM  FAIL.                                               NC1764.2
059200 ADD-WRITE-F1-4-2.                                                NC1764.2
059300     MOVE "ADD-TEST-F1-4-2 " TO PAR-NAME.                         NC1764.2
059400     PERFORM  PRINT-DETAIL.                                       NC1764.2
059500 ADD-INIT-F1-5.                                                   NC1764.2
059600     MOVE "ADD ---" TO FEATURE.                                   NC1764.2
059700     PERFORM  PRINT-DETAIL.                                       NC1764.2
059800     MOVE "  TO" TO FEATURE.                                      NC1764.2
059900 ADD-TEST-F1-5.                                                   NC1764.2
060000     MOVE     A18TWOS-DS-18V00 TO WRK-DS-18V00.                   NC1764.2
060100     ADD      A18ONES-DS-18V00 TO WRK-DS-18V00.                   NC1764.2
060200     IF       WRK-DS-18V00 EQUAL TO 333333333333333333            NC1764.2
060300              PERFORM PASS GO TO ADD-WRITE-F1-5.                  NC1764.2
060400     GO       TO ADD-FAIL-F1-5.                                   NC1764.2
060500 ADD-DELETE-F1-5.                                                 NC1764.2
060600     PERFORM  DE-LETE.                                            NC1764.2
060700     GO       TO ADD-WRITE-F1-5.                                  NC1764.2
060800 ADD-FAIL-F1-5.                                                   NC1764.2
060900     MOVE     WRK-DS-18V00 TO COMPUTED-18V0.                      NC1764.2
061000     MOVE     333333333333333333   TO CORRECT-18V0.               NC1764.2
061100     PERFORM  FAIL.                                               NC1764.2
061200 ADD-WRITE-F1-5.                                                  NC1764.2
061300     MOVE     "ADD-TEST-F1-5" TO PAR-NAME.                        NC1764.2
061400     PERFORM  PRINT-DETAIL.                                       NC1764.2
061500 ADD-TEST-F1-6.                                                   NC1764.2
061600     MOVE     ZERO TO WRK-DS-10V00.                               NC1764.2
061700     ADD      A10ONES-DS-10V00 A05ONES-DS-05V00 TO WRK-DS-10V00.  NC1764.2
061800     IF       WRK-DS-10V00 EQUAL TO 1111122222                    NC1764.2
061900              PERFORM PASS GO TO ADD-WRITE-F1-6.                  NC1764.2
062000     GO       TO ADD-FAIL-F1-6.                                   NC1764.2
062100 ADD-DELETE-F1-6.                                                 NC1764.2
062200     PERFORM  DE-LETE.                                            NC1764.2
062300     GO       TO ADD-WRITE-F1-6.                                  NC1764.2
062400 ADD-FAIL-F1-6.                                                   NC1764.2
062500     MOVE     WRK-DS-10V00 TO COMPUTED-18V0.                      NC1764.2
062600     MOVE     1111122222   TO CORRECT-18V0.                       NC1764.2
062700     PERFORM  FAIL.                                               NC1764.2
062800 ADD-WRITE-F1-6.                                                  NC1764.2
062900     MOVE     "ADD-TEST-F1-6" TO PAR-NAME.                        NC1764.2
063000     PERFORM  PRINT-DETAIL.                                       NC1764.2
063100 ADD-TEST-F1-7.                                                   NC1764.2
063200     MOVE     ZERO TO WRK-DS-10V00.                               NC1764.2
063300     ADD      A02ONES-DS-02V00                                    NC1764.2
063400              A10ONES-DS-10V00                                    NC1764.2
063500              A05ONES-DS-05V00 TO WRK-DS-10V00.                   NC1764.2
063600     IF       WRK-DS-10V00 EQUAL TO 1111122233                    NC1764.2
063700              PERFORM PASS GO TO ADD-WRITE-F1-7.                  NC1764.2
063800     GO       TO ADD-FAIL-F1-7.                                   NC1764.2
063900 ADD-DELETE-F1-7.                                                 NC1764.2
064000     PERFORM  DE-LETE.                                            NC1764.2
064100     GO       TO ADD-WRITE-F1-7.                                  NC1764.2
064200 ADD-FAIL-F1-7.                                                   NC1764.2
064300     MOVE     WRK-DS-10V00 TO COMPUTED-18V0.                      NC1764.2
064400     MOVE     1111122233   TO CORRECT-18V0.                       NC1764.2
064500     PERFORM  FAIL.                                               NC1764.2
064600 ADD-WRITE-F1-7.                                                  NC1764.2
064700     MOVE     "ADD-TEST-F1-7" TO PAR-NAME.                        NC1764.2
064800     PERFORM  PRINT-DETAIL.                                       NC1764.2
064900 ADD-INIT-F1-8.                                                   NC1764.2
065000     MOVE "  ROUNDED" TO FEATURE.                                 NC1764.2
065100 ADD-TEST-F1-8.                                                   NC1764.2
065200     MOVE     ZERO TO WRK-DS-05V00.                               NC1764.2
065300     ADD      55554.5 TO WRK-DS-05V00 ROUNDED.                    NC1764.2
065400     IF       WRK-DS-05V00 EQUAL TO 55555                         NC1764.2
065500              PERFORM PASS GO TO ADD-WRITE-F1-8.                  NC1764.2
065600     GO       TO ADD-FAIL-F1-8.                                   NC1764.2
065700 ADD-DELETE-F1-8.                                                 NC1764.2
065800     PERFORM  DE-LETE.                                            NC1764.2
065900     GO       TO ADD-WRITE-F1-8.                                  NC1764.2
066000 ADD-FAIL-F1-8.                                                   NC1764.2
066100     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC1764.2
066200     MOVE     55555 TO CORRECT-N.                                 NC1764.2
066300     PERFORM  FAIL.                                               NC1764.2
066400 ADD-WRITE-F1-8.                                                  NC1764.2
066500     MOVE     "ADD-TEST-F1-8" TO PAR-NAME.                        NC1764.2
066600     PERFORM  PRINT-DETAIL.                                       NC1764.2
066700 ADD-INIT-F1-9-1.                                                 NC1764.2
066800     MOVE "  SIZE ERROR" TO FEATURE.                              NC1764.2
066900     MOVE     -11 TO WRK-DS-02V00.                                NC1764.2
067000 ADD-TEST-F1-9-1.                                                 NC1764.2
067100     ADD      -99 TO WRK-DS-02V00 ON SIZE ERROR                   NC1764.2
067200              PERFORM PASS GO TO ADD-WRITE-F1-9-1.                NC1764.2
067300     GO       TO ADD-FAIL-F1-9-1.                                 NC1764.2
067400 ADD-DELETE-F1-9-1.                                               NC1764.2
067500     PERFORM  DE-LETE.                                            NC1764.2
067600     GO       TO ADD-WRITE-F1-9-1.                                NC1764.2
067700 ADD-FAIL-F1-9-1.                                                 NC1764.2
067800     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1764.2
067900     PERFORM  FAIL.                                               NC1764.2
068000 ADD-WRITE-F1-9-1.                                                NC1764.2
068100     MOVE     "ADD-TEST-F1-9-1" TO PAR-NAME.                      NC1764.2
068200     PERFORM  PRINT-DETAIL.                                       NC1764.2
068300 ADD-TEST-F1-9-2.                                                 NC1764.2
068400     IF       WRK-DS-02V00 EQUAL TO -11                           NC1764.2
068500              PERFORM PASS GO TO ADD-WRITE-F1-9-2.                NC1764.2
068600*    THIS TEST DEPENDS ON THE RESULTS OF TEST-F1-9-1 ABOVE.       NC1764.2
068700     GO       TO ADD-FAIL-F1-9-2.                                 NC1764.2
068800 ADD-DELETE-F1-9-2.                                               NC1764.2
068900     PERFORM  DE-LETE.                                            NC1764.2
069000     GO       TO ADD-WRITE-F1-9-2.                                NC1764.2
069100 ADD-FAIL-F1-9-2.                                                 NC1764.2
069200     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1764.2
069300     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC1764.2
069400     MOVE     -11 TO CORRECT-N.                                   NC1764.2
069500     PERFORM  FAIL.                                               NC1764.2
069600 ADD-WRITE-F1-9-2.                                                NC1764.2
069700     MOVE     "ADD-TEST-F1-9-2" TO PAR-NAME.                      NC1764.2
069800     PERFORM  PRINT-DETAIL.                                       NC1764.2
069900 ADD-INIT-F1-10-1.                                                NC1764.2
070000     MOVE "  ROUNDED,SIZE ERROR" TO FEATURE.                      NC1764.2
070100 ADD-TEST-F1-10-1.                                                NC1764.2
070200     MOVE     ZERO TO WRK-DS-05V00                                NC1764.2
070300     ADD      33333                                               NC1764.2
070400              A06THREES-DS-03V03                                  NC1764.2
070500              A12THREES-DS-06V06                                  NC1764.2
070600              TO WRK-DS-05V00 ROUNDED ON SIZE ERROR               NC1764.2
070700              PERFORM PASS GO TO ADD-WRITE-F1-10-1.               NC1764.2
070800     GO       TO ADD-FAIL-F1-10-1.                                NC1764.2
070900 ADD-DELETE-F1-10-1.                                              NC1764.2
071000     PERFORM  DE-LETE.                                            NC1764.2
071100     GO       TO ADD-WRITE-F1-10-1.                               NC1764.2
071200 ADD-FAIL-F1-10-1.                                                NC1764.2
071300     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1764.2
071400     PERFORM  FAIL.                                               NC1764.2
071500 ADD-WRITE-F1-10-1.                                               NC1764.2
071600     MOVE     "ADD-TEST-F1-10-1" TO PAR-NAME.                     NC1764.2
071700     PERFORM  PRINT-DETAIL.                                       NC1764.2
071800 ADD-TEST-F1-10-2.                                                NC1764.2
071900     IF       WRK-DS-05V00 EQUAL TO ZERO                          NC1764.2
072000              PERFORM PASS GO TO ADD-WRITE-F1-10-2.               NC1764.2
072100*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F1-10-1  NC1764.2
072200     GO       TO ADD-FAIL-F1-10-2.                                NC1764.2
072300 ADD-DELETE-F1-10-2.                                              NC1764.2
072400     PERFORM  DE-LETE.                                            NC1764.2
072500     GO       TO ADD-WRITE-F1-10-2.                               NC1764.2
072600 ADD-FAIL-F1-10-2.                                                NC1764.2
072700     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1764.2
072800     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC1764.2
072900     MOVE     ZERO TO CORRECT-N.                                  NC1764.2
073000     PERFORM  FAIL.                                               NC1764.2
073100 ADD-WRITE-F1-10-2.                                               NC1764.2
073200     MOVE     "ADD-TEST-F1-10-2" TO PAR-NAME.                     NC1764.2
073300     PERFORM  PRINT-DETAIL.                                       NC1764.2
073400 ADD-TEST-F1-11-1.                                                NC1764.2
073500     MOVE     ZERO TO WRK-DS-06V06.                               NC1764.2
073600     ADD      A12THREES-DS-06V06                                  NC1764.2
073700              333333                                              NC1764.2
073800              A06THREES-DS-03V03                                  NC1764.2
073900              TO WRK-DS-06V06 ROUNDED ON SIZE ERROR               NC1764.2
074000              GO TO ADD-FAIL-F1-11-1.                             NC1764.2
074100     PERFORM  PASS.                                               NC1764.2
074200     GO TO ADD-WRITE-F1-11-1.                                     NC1764.2
074300 ADD-DELETE-F1-11-1.                                              NC1764.2
074400     PERFORM  DE-LETE.                                            NC1764.2
074500     GO       TO ADD-WRITE-F1-11-1.                               NC1764.2
074600 ADD-FAIL-F1-11-1.                                                NC1764.2
074700     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1764.2
074800     PERFORM  FAIL.                                               NC1764.2
074900 ADD-WRITE-F1-11-1.                                               NC1764.2
075000     MOVE     "ADD-TEST-F1-11-1" TO PAR-NAME.                     NC1764.2
075100     PERFORM  PRINT-DETAIL.                                       NC1764.2
075200 ADD-TEST-F1-11-2.                                                NC1764.2
075300     IF       WRK-DS-06V06 EQUAL TO 666999.666333                 NC1764.2
075400              PERFORM PASS GO TO ADD-WRITE-F1-11-2.               NC1764.2
075500*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F1-11-1  NC1764.2
075600     GO       TO ADD-FAIL-F1-11-2.                                NC1764.2
075700 ADD-DELETE-F1-11-2.                                              NC1764.2
075800     PERFORM  DE-LETE.                                            NC1764.2
075900     GO       TO ADD-WRITE-F1-11-2.                               NC1764.2
076000 ADD-FAIL-F1-11-2.                                                NC1764.2
076100     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1764.2
076200     MOVE     666999.666333 TO CORRECT-N.                         NC1764.2
076300     PERFORM  FAIL.                                               NC1764.2
076400 ADD-WRITE-F1-11-2.                                               NC1764.2
076500     MOVE     "ADD-TEST-F1-11-2" TO PAR-NAME.                     NC1764.2
076600     PERFORM  PRINT-DETAIL.                                       NC1764.2
076700 ADD-INIT-F1-12.                                                  NC1764.2
076800     MOVE "  COMP VS. DISPLAY"  TO FEATURE.                       NC1764.2
076900 ADD-TEST-F1-12.                                                  NC1764.2
077000     MOVE     A18ONES-DS-18V00 TO WRK-CS-18V00.                   NC1764.2
077100     ADD      A18ONES-DS-18V00 TO WRK-CS-18V00.                   NC1764.2
077200     IF       WRK-CS-18V00 EQUAL TO 222222222222222222            NC1764.2
077300              PERFORM PASS GO TO ADD-WRITE-F1-12.                 NC1764.2
077400     GO       TO ADD-FAIL-F1-12.                                  NC1764.2
077500 ADD-DELETE-F1-12.                                                NC1764.2
077600     PERFORM  DE-LETE.                                            NC1764.2
077700     GO       TO ADD-WRITE-F1-12.                                 NC1764.2
077800 ADD-FAIL-F1-12.                                                  NC1764.2
077900     MOVE     WRK-CS-18V00 TO COMPUTED-18V0.                      NC1764.2
078000     MOVE     222222222222222222 TO CORRECT-18V0.                 NC1764.2
078100     PERFORM  FAIL.                                               NC1764.2
078200 ADD-WRITE-F1-12.                                                 NC1764.2
078300     MOVE     "ADD-TEST-F1-12" TO PAR-NAME.                       NC1764.2
078400     PERFORM  PRINT-DETAIL.                                       NC1764.2
078500 ADD-TEST-F1-13.                                                  NC1764.2
078600     MOVE     A18ONES-DS-18V00 TO WRK-DS-18V00.                   NC1764.2
078700     ADD      A18ONES-CS-18V00 TO WRK-DS-18V00.                   NC1764.2
078800     IF       WRK-DS-18V00 EQUAL TO 222222222222222222            NC1764.2
078900              PERFORM PASS GO TO ADD-WRITE-F1-13.                 NC1764.2
079000     GO       TO ADD-FAIL-F1-13.                                  NC1764.2
079100 ADD-DELETE-F1-13.                                                NC1764.2
079200     PERFORM  DE-LETE.                                            NC1764.2
079300     GO       TO ADD-WRITE-F1-13.                                 NC1764.2
079400 ADD-FAIL-F1-13.                                                  NC1764.2
079500     MOVE     WRK-DS-18V00 TO COMPUTED-18V0.                      NC1764.2
079600     MOVE     222222222222222222 TO CORRECT-18V0.                 NC1764.2
079700     PERFORM  FAIL.                                               NC1764.2
079800 ADD-WRITE-F1-13.                                                 NC1764.2
079900     MOVE     "ADD-TEST-F1-13" TO PAR-NAME.                       NC1764.2
080000     PERFORM  PRINT-DETAIL.                                       NC1764.2
080100 ADD-TEST-F1-14.                                                  NC1764.2
080200     MOVE     ZERO TO WRK-CS-02V02.                               NC1764.2
080300     ADD      A99-CS-02V00 TO WRK-CS-02V02.                       NC1764.2
080400     IF       WRK-CS-02V02 EQUAL TO 99.00                         NC1764.2
080500              PERFORM PASS GO TO ADD-WRITE-F1-14.                 NC1764.2
080600     GO       TO ADD-FAIL-F1-14.                                  NC1764.2
080700 ADD-DELETE-F1-14.                                                NC1764.2
080800     PERFORM  DE-LETE.                                            NC1764.2
080900     GO       TO ADD-WRITE-F1-14.                                 NC1764.2
081000 ADD-FAIL-F1-14.                                                  NC1764.2
081100     MOVE     WRK-CS-02V02 TO COMPUTED-N.                         NC1764.2
081200     MOVE     99.00 TO CORRECT-N.                                 NC1764.2
081300     PERFORM  FAIL.                                               NC1764.2
081400 ADD-WRITE-F1-14.                                                 NC1764.2
081500     MOVE     "ADD-TEST-F1-14" TO PAR-NAME.                       NC1764.2
081600     PERFORM  PRINT-DETAIL.                                       NC1764.2
081700 ADD-TEST-F1-15-1.                                                NC1764.2
081800     MOVE     A99-CS-02V00 TO WRK-CS-02V02.                       NC1764.2
081900     ADD      A99-CS-02V00 TO WRK-CS-02V02 ON SIZE ERROR          NC1764.2
082000              PERFORM PASS GO TO ADD-WRITE-F1-15-1.               NC1764.2
082100     GO       TO ADD-FAIL-F1-15-1.                                NC1764.2
082200 ADD-DELETE-F1-15-1.                                              NC1764.2
082300     PERFORM  DE-LETE.                                            NC1764.2
082400     GO       TO ADD-WRITE-F1-15-1.                               NC1764.2
082500 ADD-FAIL-F1-15-1.                                                NC1764.2
082600     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1764.2
082700     PERFORM  FAIL.                                               NC1764.2
082800 ADD-WRITE-F1-15-1.                                               NC1764.2
082900     MOVE     "ADD-TEST-F1-15-1" TO PAR-NAME.                     NC1764.2
083000     PERFORM  PRINT-DETAIL.                                       NC1764.2
083100 ADD-TEST-F1-15-2.                                                NC1764.2
083200     IF       WRK-CS-02V02 EQUAL TO 99.00                         NC1764.2
083300              PERFORM PASS GO TO ADD-WRITE-F1-15-2.               NC1764.2
083400*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F1-15-1  NC1764.2
083500     GO       TO ADD-FAIL-F1-15-2.                                NC1764.2
083600 ADD-DELETE-F1-15-2.                                              NC1764.2
083700     PERFORM  DE-LETE.                                            NC1764.2
083800     GO       TO ADD-WRITE-F1-15-2.                               NC1764.2
083900 ADD-FAIL-F1-15-2.                                                NC1764.2
084000     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1764.2
084100     MOVE     WRK-CS-02V02 TO COMPUTED-N.                         NC1764.2
084200     MOVE     99.00 TO CORRECT-N.                                 NC1764.2
084300     PERFORM  FAIL.                                               NC1764.2
084400 ADD-WRITE-F1-15-2.                                               NC1764.2
084500     MOVE     "ADD-TEST-F1-15-2" TO PAR-NAME.                     NC1764.2
084600     PERFORM  PRINT-DETAIL.                                       NC1764.2
084700 ADD-TEST-F1-16.                                                  NC1764.2
084800     MOVE A14TWOS-CS-18V00 TO WRK-CS-18V00.                       NC1764.2
084900     ADD A18FIVES-CS-18V00 TO WRK-CS-18V00.                       NC1764.2
085000     IF WRK-CS-18V00 EQUAL TO -555577777777777777                 NC1764.2
085100         PERFORM PASS                                             NC1764.2
085200         GO TO ADD-WRITE-F1-16.                                   NC1764.2
085300     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1764.2
085400     MOVE -555577777777777777 TO CORRECT-18V0.                    NC1764.2
085500     PERFORM FAIL.                                                NC1764.2
085600     GO TO ADD-WRITE-F1-16.                                       NC1764.2
085700 ADD-DELETE-F1-16.                                                NC1764.2
085800     PERFORM DE-LETE.                                             NC1764.2
085900 ADD-WRITE-F1-16.                                                 NC1764.2
086000     MOVE "ADD-TEST-F1-16 " TO PAR-NAME.                          NC1764.2
086100     PERFORM PRINT-DETAIL.                                        NC1764.2
086200 ADD-TEST-F1-17.                                                  NC1764.2
086300     MOVE A12SEVENS-CU-18V00 TO WRK-CS-18V00.                     NC1764.2
086400     ADD A18SIXES-CS-18V00 TO WRK-CS-18V00.                       NC1764.2
086500     IF WRK-CS-18V00 EQUAL TO +666667444444444443                 NC1764.2
086600         PERFORM PASS                                             NC1764.2
086700         GO TO ADD-WRITE-F1-17.                                   NC1764.2
086800     MOVE +666667444444444443 TO CORRECT-18V0.                    NC1764.2
086900     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1764.2
087000     PERFORM FAIL.                                                NC1764.2
087100     GO TO ADD-WRITE-F1-17.                                       NC1764.2
087200 ADD-DELETE-F1-17.                                                NC1764.2
087300     PERFORM DE-LETE.                                             NC1764.2
087400 ADD-WRITE-F1-17.                                                 NC1764.2
087500     MOVE "ADD-TEST-F1-17 " TO PAR-NAME.                          NC1764.2
087600     PERFORM PRINT-DETAIL.                                        NC1764.2
087700 ADD-TEST-F1-18.                                                  NC1764.2
087800     MOVE A12SEVENS-CU-18V00 TO WRK-DU-18V00.                     NC1764.2
087900     ADD A18FIVES-CS-18V00 TO WRK-DU-18V00.                       NC1764.2
088000     IF WRK-DU-18V00 EQUAL TO 555554777777777778                  NC1764.2
088100         PERFORM PASS                                             NC1764.2
088200         GO TO ADD-WRITE-F1-18.                                   NC1764.2
088300     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1764.2
088400     MOVE 555554777777777778 TO CORRECT-18V0.                     NC1764.2
088500     PERFORM FAIL.                                                NC1764.2
088600     GO TO ADD-WRITE-F1-18.                                       NC1764.2
088700 ADD-DELETE-F1-18.                                                NC1764.2
088800     PERFORM DE-LETE.                                             NC1764.2
088900 ADD-WRITE-F1-18.                                                 NC1764.2
089000     MOVE "ADD-TEST-F1-18 " TO PAR-NAME.                          NC1764.2
089100     PERFORM PRINT-DETAIL.                                        NC1764.2
089200 ADD-TEST-F1-19.                                                  NC1764.2
089300     MOVE     +980 TO WRK-CS-03V00.                               NC1764.2
089400     MOVE     SPACE TO SIZE-ERR.                                  NC1764.2
089500*        NOTE IN THIS TEST, 1 IS ADDED TO A 3-DIGIT COMP SYNC     NC1764.2
089600*             FIELD UNTIL A SIZE ERROR OCCURS --- IF THE VALUE OF NC1764.2
089700*             THE FIELD REACHES 1180 WITHOUT A SIZE ERROR THE     NC1764.2
089800*             ATTEMPTED ADDITIONS ARE TERMINATED.                 NC1764.2
089900     PERFORM  ADD-A-F1-19 THRU ADD-B-F1-19 200 TIMES.             NC1764.2
090000     IF       SIZE-ERR EQUAL TO SPACE                             NC1764.2
090100              MOVE "SIZE ERROR NOT ENCOUNTERED" TO RE-MARK        NC1764.2
090200              MOVE "AT LEAST 1180" TO COMPUTED-A                  NC1764.2
090300              MOVE "999 IN PIC 999 FIELD" TO CORRECT-A            NC1764.2
090400              PERFORM FAIL                                        NC1764.2
090500              GO TO ADD-WRITE-F1-19.                              NC1764.2
090600     IF       WRK-CS-03V00 EQUAL TO 999                           NC1764.2
090700              PERFORM PASS GO TO ADD-WRITE-F1-19.                 NC1764.2
090800     PERFORM  FAIL.                                               NC1764.2
090900     MOVE     WRK-CS-03V00 TO COMPUTED-N.                         NC1764.2
091000     MOVE     999 TO CORRECT-N.                                   NC1764.2
091100     GO       TO ADD-WRITE-F1-19.                                 NC1764.2
091200 ADD-DELETE-F1-19.                                                NC1764.2
091300     PERFORM  DE-LETE.                                            NC1764.2
091400     GO       TO ADD-WRITE-F1-19.                                 NC1764.2
091500 ADD-A-F1-19.                                                     NC1764.2
091600     IF       SIZE-ERR EQUAL TO "E" GO TO ADD-B-F1-19.            NC1764.2
091700     ADD      1 TO WRK-CS-03V00 ON SIZE ERROR                     NC1764.2
091800              MOVE "E" TO SIZE-ERR.                               NC1764.2
091900 ADD-B-F1-19.                                                     NC1764.2
092000     EXIT.                                                        NC1764.2
092100 ADD-WRITE-F1-19.                                                 NC1764.2
092200     MOVE   "ADD, COMP, SIZE ERR" TO FEATURE.                     NC1764.2
092300     MOVE   "ADD-TEST-F1-19" TO PAR-NAME.                         NC1764.2
092400     PERFORM PRINT-DETAIL.                                        NC1764.2
092500*                                                                 NC1764.2
092600 ADD-INIT-F1-20.                                                  NC1764.2
092700*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1764.2
092800     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1764.2
092900     MOVE   -11 TO WRK-DS-02V00.                                  NC1764.2
093000 ADD-TEST-F1-20.                                                  NC1764.2
093100     ADD    -99 TO WRK-DS-02V00                                   NC1764.2
093200         NOT ON SIZE ERROR                                        NC1764.2
093300             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
093400                  TO RE-MARK                                      NC1764.2
093500             PERFORM FAIL                                         NC1764.2
093600             GO TO   ADD-WRITE-F1-20.                             NC1764.2
093700     GO TO   ADD-PASS-F1-20.                                      NC1764.2
093800 ADD-DELETE-F1-20.                                                NC1764.2
093900     PERFORM DE-LETE.                                             NC1764.2
094000     GO TO   ADD-WRITE-F1-20.                                     NC1764.2
094100 ADD-PASS-F1-20.                                                  NC1764.2
094200     PERFORM PASS.                                                NC1764.2
094300 ADD-WRITE-F1-20.                                                 NC1764.2
094400     MOVE   "ADD-TEST-F1-20" TO PAR-NAME.                         NC1764.2
094500     PERFORM PRINT-DETAIL.                                        NC1764.2
094600*                                                                 NC1764.2
094700 ADD-INIT-F1-21.                                                  NC1764.2
094800*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1764.2
094900     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1764.2
095000     MOVE    ZERO TO WRK-DS-06V06.                                NC1764.2
095100 ADD-TEST-F1-21-0.                                                NC1764.2
095200     ADD     A12THREES-DS-06V06                                   NC1764.2
095300             333333                                               NC1764.2
095400             A06THREES-DS-03V03                                   NC1764.2
095500          TO WRK-DS-06V06 ROUNDED                                 NC1764.2
095600      NOT ON SIZE ERROR                                           NC1764.2
095700             GO TO ADD-PASS-F1-21.                                NC1764.2
095800     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.    NC1764.2
095900     PERFORM FAIL.                                                NC1764.2
096000     GO TO   ADD-WRITE-F1-21.                                     NC1764.2
096100 ADD-DELETE-F1-21.                                                NC1764.2
096200     PERFORM DE-LETE.                                             NC1764.2
096300     GO TO   ADD-WRITE-F1-21.                                     NC1764.2
096400 ADD-PASS-F1-21.                                                  NC1764.2
096500     PERFORM PASS.                                                NC1764.2
096600 ADD-WRITE-F1-21.                                                 NC1764.2
096700     MOVE   "ADD-TEST-F1-21" TO PAR-NAME.                         NC1764.2
096800     PERFORM PRINT-DETAIL.                                        NC1764.2
096900*                                                                 NC1764.2
097000 ADD-INIT-F1-22.                                                  NC1764.2
097100*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1764.2
097200     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1764.2
097300     MOVE   -11 TO WRK-DS-02V00.                                  NC1764.2
097400 ADD-TEST-F1-22-0.                                                NC1764.2
097500     ADD    -99 TO WRK-DS-02V00                                   NC1764.2
097600          ON SIZE ERROR                                           NC1764.2
097700             PERFORM PASS                                         NC1764.2
097800             GO TO   ADD-WRITE-F1-22                              NC1764.2
097900      NOT ON SIZE ERROR                                           NC1764.2
098000             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
098100                  TO RE-MARK                                      NC1764.2
098200             PERFORM FAIL                                         NC1764.2
098300             GO TO   ADD-WRITE-F1-22.                             NC1764.2
098400 ADD-DELETE-F1-22.                                                NC1764.2
098500     PERFORM DE-LETE.                                             NC1764.2
098600 ADD-WRITE-F1-22.                                                 NC1764.2
098700     MOVE   "ADD-TEST-F1-22" TO PAR-NAME.                         NC1764.2
098800     PERFORM PRINT-DETAIL.                                        NC1764.2
098900*                                                                 NC1764.2
099000 ADD-INIT-F1-23.                                                  NC1764.2
099100*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1764.2
099200     MOVE   "V1-67 6.4.2" TO ANSI-REFERENCE.                      NC1764.2
099300     MOVE    ZERO TO WRK-DS-06V06.                                NC1764.2
099400 ADD-TEST-F1-23-0.                                                NC1764.2
099500     ADD     A12THREES-DS-06V06                                   NC1764.2
099600             333333                                               NC1764.2
099700             A06THREES-DS-03V03                                   NC1764.2
099800          TO WRK-DS-06V06 ROUNDED                                 NC1764.2
099900          ON SIZE ERROR                                           NC1764.2
100000             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1764.2
100100                  TO RE-MARK                                      NC1764.2
100200             PERFORM FAIL                                         NC1764.2
100300             GO TO   ADD-WRITE-F1-23                              NC1764.2
100400      NOT ON SIZE ERROR                                           NC1764.2
100500             GO TO   ADD-PASS-F1-23.                              NC1764.2
100600 ADD-DELETE-F1-23.                                                NC1764.2
100700     PERFORM DE-LETE.                                             NC1764.2
100800     GO TO   ADD-WRITE-F1-23.                                     NC1764.2
100900 ADD-PASS-F1-23.                                                  NC1764.2
101000     PERFORM PASS.                                                NC1764.2
101100 ADD-WRITE-F1-23.                                                 NC1764.2
101200     MOVE   "ADD-TEST-F1-23" TO PAR-NAME.                         NC1764.2
101300     PERFORM PRINT-DETAIL.                                        NC1764.2
101400*                                                                 NC1764.2
101500 ADD-INIT-F1-24.                                                  NC1764.2
101600*    ==-->  MULTIPLE OPERANDS  <--==                              NC1764.2
101700     MOVE   "V1-67 6.4.2"      TO ANSI-REFERENCE.                 NC1764.2
101800     MOVE   "ADD LIMIT TESTS " TO FEATURE.                        NC1764.2
101900     MOVE    1  TO DNAME1  DNAME2  DNAME3  DNAME4  DNAME5.        NC1764.2
102000     MOVE    1  TO DNAME6  DNAME7  DNAME8  DNAME9  DNAME10.       NC1764.2
102100     MOVE    1  TO DNAME11 DNAME12 DNAME13 DNAME14 DNAME15.       NC1764.2
102200     MOVE    1  TO DNAME16 DNAME17 DNAME18 DNAME19 DNAME20.       NC1764.2
102300     MOVE    1  TO DNAME21.                                       NC1764.2
102400     MOVE    0  TO DNAME22.                                       NC1764.2
102500*    THE FOLLOWING 22 TESTS VERIFY THE ABILITY OF THE COMPILER    NC1764.2
102600*    TO HANDLE A MAXIMUM OF 42 OPERANDS.   A DELETION IN THIS     NC1764.2
102700*    PARAGRAPH WILL SKIP THE LIMIT TESTS.                         NC1764.2
102800     GO TO ADD-TEST-F1-24.                                        NC1764.2
102900 ADD-INIT-DELETE.                                                 NC1764.2
103000     PERFORM DE-LETE.                                             NC1764.2
103100     MOVE "ADD-TEST-F1-24 TO F1-45" TO PAR-NAME.                  NC1764.2
103200     MOVE "ADD LIMIT TESTS " TO FEATURE.                          NC1764.2
103300     ADD 21 TO DELETE-COUNTER.                                    NC1764.2
103400     PERFORM PRINT-DETAIL.                                        NC1764.2
103500     GO TO   ADD-INIT-F1-46.                                      NC1764.2
103600 ADD-TEST-F1-24.                                                  NC1764.2
103700     ADD DNAME1                                                   NC1764.2
103800         DNAME2                                                   NC1764.2
103900         DNAME3                                                   NC1764.2
104000         DNAME4                                                   NC1764.2
104100         DNAME5                                                   NC1764.2
104200         DNAME6                                                   NC1764.2
104300         DNAME7                                                   NC1764.2
104400         DNAME8                                                   NC1764.2
104500         DNAME9                                                   NC1764.2
104600         DNAME10                                                  NC1764.2
104700         DNAME11                                                  NC1764.2
104800         DNAME12                                                  NC1764.2
104900         DNAME13                                                  NC1764.2
105000         DNAME14                                                  NC1764.2
105100         DNAME15                                                  NC1764.2
105200         DNAME16                                                  NC1764.2
105300         DNAME17                                                  NC1764.2
105400         DNAME18                                                  NC1764.2
105500         DNAME19                                                  NC1764.2
105600         DNAME20                                                  NC1764.2
105700         DNAME21                                                  NC1764.2
105800              TO DNAME22.                                         NC1764.2
105900*    THE NUMBER OF OPERANDS CAPABLE OF BEING ADDED TO ONE         NC1764.2
106000*    DATANAME WILL BE REFLECTED BY THE COMPUTED-ANSWER.           NC1764.2
106100     IF DNAME22 EQUAL TO 21                                       NC1764.2
106200         PERFORM PASS                                             NC1764.2
106300         GO TO ADD-WRITE-F1-24.                                   NC1764.2
106400     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
106500     MOVE DNAME22 TO COMPUTED-18V0.                               NC1764.2
106600     PERFORM FAIL.                                                NC1764.2
106700     GO TO ADD-WRITE-F1-24.                                       NC1764.2
106800 ADD-DELETE-F1-24.                                                NC1764.2
106900     PERFORM DE-LETE.                                             NC1764.2
107000 ADD-WRITE-F1-24.                                                 NC1764.2
107100     MOVE "ADD-TEST-F1-24 " TO PAR-NAME.                          NC1764.2
107200     PERFORM PRINT-DETAIL.                                        NC1764.2
107300 ADD-INIT-F1-25.                                                  NC1764.2
107400     MOVE ZERO TO DNAME22 DNAME23 DNAME24 DNAME25 DNAME26.        NC1764.2
107500     MOVE ZERO TO DNAME27 DNAME28 DNAME29 DNAME30 DNAME31.        NC1764.2
107600     MOVE ZERO TO DNAME32 DNAME33 DNAME34 DNAME35 DNAME36.        NC1764.2
107700     MOVE ZERO TO DNAME37 DNAME38 DNAME39 DNAME40 DNAME41.        NC1764.2
107800     MOVE ZERO TO DNAME42.                                        NC1764.2
107900 ADD-TEST-F1-25.                                                  NC1764.2
108000     ADD DNAME1                                                   NC1764.2
108100         DNAME2                                                   NC1764.2
108200         DNAME3                                                   NC1764.2
108300         DNAME4                                                   NC1764.2
108400         DNAME5                                                   NC1764.2
108500         DNAME6                                                   NC1764.2
108600         DNAME7                                                   NC1764.2
108700         DNAME8                                                   NC1764.2
108800         DNAME9                                                   NC1764.2
108900         DNAME10                                                  NC1764.2
109000         DNAME11                                                  NC1764.2
109100         DNAME12                                                  NC1764.2
109200         DNAME13                                                  NC1764.2
109300         DNAME14                                                  NC1764.2
109400         DNAME15                                                  NC1764.2
109500         DNAME16                                                  NC1764.2
109600         DNAME17                                                  NC1764.2
109700         DNAME18                                                  NC1764.2
109800         DNAME19                                                  NC1764.2
109900         DNAME20                                                  NC1764.2
110000         DNAME21                                                  NC1764.2
110100              TO DNAME22                                          NC1764.2
110200                 DNAME23                                          NC1764.2
110300                 DNAME24                                          NC1764.2
110400                 DNAME25                                          NC1764.2
110500                 DNAME26                                          NC1764.2
110600                 DNAME27                                          NC1764.2
110700                 DNAME28                                          NC1764.2
110800                 DNAME29                                          NC1764.2
110900                 DNAME30                                          NC1764.2
111000                 DNAME31                                          NC1764.2
111100                 DNAME32                                          NC1764.2
111200                 DNAME33                                          NC1764.2
111300                 DNAME34                                          NC1764.2
111400                 DNAME35                                          NC1764.2
111500                 DNAME36                                          NC1764.2
111600                 DNAME37                                          NC1764.2
111700                 DNAME38                                          NC1764.2
111800                 DNAME39                                          NC1764.2
111900                 DNAME40                                          NC1764.2
112000                 DNAME41                                          NC1764.2
112100                 DNAME42.                                         NC1764.2
112200     IF DNAME22 EQUAL TO 21                                       NC1764.2
112300         PERFORM PASS                                             NC1764.2
112400         GO TO ADD-WRITE-F1-25.                                   NC1764.2
112500     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
112600     MOVE DNAME22 TO COMPUTED-18V0.                               NC1764.2
112700     PERFORM FAIL.                                                NC1764.2
112800     GO TO ADD-WRITE-F1-25.                                       NC1764.2
112900 ADD-DELETE-F1-25.                                                NC1764.2
113000     PERFORM DE-LETE.                                             NC1764.2
113100 ADD-WRITE-F1-25.                                                 NC1764.2
113200     MOVE "ADD-TEST-F1-25 " TO PAR-NAME.                          NC1764.2
113300     PERFORM PRINT-DETAIL.                                        NC1764.2
113400 ADD-TEST-F1-26.                                                  NC1764.2
113500     IF DNAME23 EQUAL TO 21                                       NC1764.2
113600         PERFORM PASS                                             NC1764.2
113700         GO TO ADD-WRITE-F1-26.                                   NC1764.2
113800     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
113900     MOVE DNAME23 TO COMPUTED-18V0.                               NC1764.2
114000     PERFORM FAIL.                                                NC1764.2
114100     GO TO ADD-WRITE-F1-26.                                       NC1764.2
114200 ADD-DELETE-F1-26.                                                NC1764.2
114300     PERFORM DE-LETE.                                             NC1764.2
114400 ADD-WRITE-F1-26.                                                 NC1764.2
114500     MOVE "ADD-TEST-F1-26 " TO PAR-NAME.                          NC1764.2
114600     PERFORM PRINT-DETAIL.                                        NC1764.2
114700 ADD-TEST-F1-27.                                                  NC1764.2
114800     IF DNAME24 EQUAL TO 21                                       NC1764.2
114900         PERFORM PASS                                             NC1764.2
115000         GO TO ADD-WRITE-F1-27.                                   NC1764.2
115100     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
115200     MOVE DNAME24 TO COMPUTED-18V0.                               NC1764.2
115300     PERFORM FAIL.                                                NC1764.2
115400     GO TO ADD-WRITE-F1-27.                                       NC1764.2
115500 ADD-DELETE-F1-27.                                                NC1764.2
115600     PERFORM DE-LETE.                                             NC1764.2
115700 ADD-WRITE-F1-27.                                                 NC1764.2
115800     MOVE "ADD-TEST-F1-27 " TO PAR-NAME.                          NC1764.2
115900     PERFORM PRINT-DETAIL.                                        NC1764.2
116000 ADD-TEST-F1-28.                                                  NC1764.2
116100     IF DNAME25 EQUAL TO 21                                       NC1764.2
116200         PERFORM PASS                                             NC1764.2
116300         GO TO ADD-WRITE-F1-28.                                   NC1764.2
116400     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
116500     MOVE DNAME25 TO COMPUTED-18V0.                               NC1764.2
116600     PERFORM FAIL.                                                NC1764.2
116700     GO TO ADD-WRITE-F1-28.                                       NC1764.2
116800 ADD-DELETE-F1-28.                                                NC1764.2
116900     PERFORM DE-LETE.                                             NC1764.2
117000 ADD-WRITE-F1-28.                                                 NC1764.2
117100     MOVE "ADD-TEST-F1-28 " TO PAR-NAME.                          NC1764.2
117200     PERFORM PRINT-DETAIL.                                        NC1764.2
117300 ADD-TEST-F1-29.                                                  NC1764.2
117400     IF DNAME26 EQUAL TO 21                                       NC1764.2
117500         PERFORM PASS                                             NC1764.2
117600         GO TO ADD-WRITE-F1-29.                                   NC1764.2
117700     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
117800     MOVE DNAME26 TO COMPUTED-18V0.                               NC1764.2
117900     PERFORM FAIL.                                                NC1764.2
118000     GO TO ADD-WRITE-F1-29.                                       NC1764.2
118100 ADD-DELETE-F1-29.                                                NC1764.2
118200     PERFORM DE-LETE.                                             NC1764.2
118300 ADD-WRITE-F1-29.                                                 NC1764.2
118400     MOVE "ADD-TEST-F1-29 " TO PAR-NAME.                          NC1764.2
118500     PERFORM PRINT-DETAIL.                                        NC1764.2
118600 ADD-TEST-F1-30.                                                  NC1764.2
118700     IF DNAME27 EQUAL TO 21                                       NC1764.2
118800         PERFORM PASS                                             NC1764.2
118900         GO TO ADD-WRITE-F1-30.                                   NC1764.2
119000     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
119100     MOVE DNAME27 TO COMPUTED-18V0.                               NC1764.2
119200     PERFORM FAIL.                                                NC1764.2
119300     GO TO ADD-WRITE-F1-30.                                       NC1764.2
119400 ADD-DELETE-F1-30.                                                NC1764.2
119500     PERFORM DE-LETE.                                             NC1764.2
119600 ADD-WRITE-F1-30.                                                 NC1764.2
119700     MOVE "ADD-TEST-F1-30 " TO PAR-NAME.                          NC1764.2
119800     PERFORM PRINT-DETAIL.                                        NC1764.2
119900 ADD-TEST-F1-31.                                                  NC1764.2
120000     IF DNAME28 EQUAL TO 21                                       NC1764.2
120100         PERFORM PASS                                             NC1764.2
120200         GO TO ADD-WRITE-F1-31.                                   NC1764.2
120300     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
120400     MOVE DNAME28 TO COMPUTED-18V0.                               NC1764.2
120500     PERFORM FAIL.                                                NC1764.2
120600     GO TO ADD-WRITE-F1-31.                                       NC1764.2
120700 ADD-DELETE-F1-31.                                                NC1764.2
120800     PERFORM DE-LETE.                                             NC1764.2
120900 ADD-WRITE-F1-31.                                                 NC1764.2
121000     MOVE "ADD-TEST-F1-31 " TO PAR-NAME.                          NC1764.2
121100     PERFORM PRINT-DETAIL.                                        NC1764.2
121200 ADD-TEST-F1-32.                                                  NC1764.2
121300     IF DNAME29 EQUAL TO 21                                       NC1764.2
121400         PERFORM PASS                                             NC1764.2
121500         GO TO ADD-WRITE-F1-32.                                   NC1764.2
121600     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
121700     MOVE DNAME29 TO COMPUTED-18V0.                               NC1764.2
121800     PERFORM FAIL.                                                NC1764.2
121900     GO TO ADD-WRITE-F1-32.                                       NC1764.2
122000 ADD-DELETE-F1-32.                                                NC1764.2
122100     PERFORM DE-LETE.                                             NC1764.2
122200 ADD-WRITE-F1-32.                                                 NC1764.2
122300     MOVE "ADD-TEST-F1-32 " TO PAR-NAME.                          NC1764.2
122400     PERFORM PRINT-DETAIL.                                        NC1764.2
122500 ADD-TEST-F1-33.                                                  NC1764.2
122600     IF DNAME30 EQUAL TO 21                                       NC1764.2
122700         PERFORM PASS                                             NC1764.2
122800         GO TO ADD-WRITE-F1-33.                                   NC1764.2
122900     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
123000     MOVE DNAME30 TO COMPUTED-18V0.                               NC1764.2
123100     PERFORM FAIL.                                                NC1764.2
123200     GO TO ADD-WRITE-F1-33.                                       NC1764.2
123300 ADD-DELETE-F1-33.                                                NC1764.2
123400     PERFORM DE-LETE.                                             NC1764.2
123500 ADD-WRITE-F1-33.                                                 NC1764.2
123600     MOVE "ADD-TEST-F1-33 " TO PAR-NAME.                          NC1764.2
123700     PERFORM PRINT-DETAIL.                                        NC1764.2
123800 ADD-TEST-F1-34.                                                  NC1764.2
123900     IF DNAME31 EQUAL TO 21                                       NC1764.2
124000         PERFORM PASS                                             NC1764.2
124100         GO TO ADD-WRITE-F1-34.                                   NC1764.2
124200     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
124300     MOVE DNAME31 TO COMPUTED-18V0.                               NC1764.2
124400     PERFORM FAIL.                                                NC1764.2
124500     GO TO ADD-WRITE-F1-34.                                       NC1764.2
124600 ADD-DELETE-F1-34.                                                NC1764.2
124700     PERFORM DE-LETE.                                             NC1764.2
124800 ADD-WRITE-F1-34.                                                 NC1764.2
124900     MOVE "ADD-TEST-F1-34 " TO PAR-NAME.                          NC1764.2
125000     PERFORM PRINT-DETAIL.                                        NC1764.2
125100 ADD-TEST-F1-35.                                                  NC1764.2
125200     IF DNAME32 EQUAL TO 21                                       NC1764.2
125300         PERFORM PASS                                             NC1764.2
125400         GO TO ADD-WRITE-F1-35.                                   NC1764.2
125500     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
125600     MOVE DNAME32 TO COMPUTED-18V0.                               NC1764.2
125700     PERFORM FAIL.                                                NC1764.2
125800     GO TO ADD-WRITE-F1-35.                                       NC1764.2
125900 ADD-DELETE-F1-35.                                                NC1764.2
126000     PERFORM DE-LETE.                                             NC1764.2
126100 ADD-WRITE-F1-35.                                                 NC1764.2
126200     MOVE "ADD-TEST-F1-35 " TO PAR-NAME.                          NC1764.2
126300     PERFORM PRINT-DETAIL.                                        NC1764.2
126400 ADD-TEST-F1-36.                                                  NC1764.2
126500     IF DNAME33 EQUAL TO 21                                       NC1764.2
126600         PERFORM PASS                                             NC1764.2
126700         GO TO ADD-WRITE-F1-36.                                   NC1764.2
126800     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
126900     MOVE DNAME33 TO COMPUTED-18V0.                               NC1764.2
127000     PERFORM FAIL.                                                NC1764.2
127100     GO TO ADD-WRITE-F1-36.                                       NC1764.2
127200 ADD-DELETE-F1-36.                                                NC1764.2
127300     PERFORM DE-LETE.                                             NC1764.2
127400 ADD-WRITE-F1-36.                                                 NC1764.2
127500     MOVE "ADD-TEST-F1-36 " TO PAR-NAME.                          NC1764.2
127600     PERFORM PRINT-DETAIL.                                        NC1764.2
127700 ADD-TEST-F1-37.                                                  NC1764.2
127800     IF DNAME34 EQUAL TO 21                                       NC1764.2
127900         PERFORM PASS                                             NC1764.2
128000         GO TO ADD-WRITE-F1-37.                                   NC1764.2
128100     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
128200     MOVE DNAME34 TO COMPUTED-18V0.                               NC1764.2
128300     PERFORM FAIL.                                                NC1764.2
128400     GO TO ADD-WRITE-F1-37.                                       NC1764.2
128500 ADD-DELETE-F1-37.                                                NC1764.2
128600     PERFORM DE-LETE.                                             NC1764.2
128700 ADD-WRITE-F1-37.                                                 NC1764.2
128800     MOVE "ADD-TEST-F1-37 " TO PAR-NAME.                          NC1764.2
128900     PERFORM PRINT-DETAIL.                                        NC1764.2
129000 ADD-TEST-F1-38.                                                  NC1764.2
129100     IF DNAME35 EQUAL TO 21                                       NC1764.2
129200         PERFORM PASS                                             NC1764.2
129300         GO TO ADD-WRITE-F1-38.                                   NC1764.2
129400     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
129500     MOVE DNAME35 TO COMPUTED-18V0.                               NC1764.2
129600     PERFORM FAIL.                                                NC1764.2
129700     GO TO ADD-WRITE-F1-38.                                       NC1764.2
129800 ADD-DELETE-F1-38.                                                NC1764.2
129900     PERFORM DE-LETE.                                             NC1764.2
130000 ADD-WRITE-F1-38.                                                 NC1764.2
130100     MOVE "ADD-TEST-F1-38 " TO PAR-NAME.                          NC1764.2
130200     PERFORM PRINT-DETAIL.                                        NC1764.2
130300 ADD-TEST-F1-39.                                                  NC1764.2
130400     IF DNAME36 EQUAL TO 21                                       NC1764.2
130500         PERFORM PASS                                             NC1764.2
130600         GO TO ADD-WRITE-F1-39.                                   NC1764.2
130700     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
130800     MOVE DNAME36 TO COMPUTED-18V0.                               NC1764.2
130900     PERFORM FAIL.                                                NC1764.2
131000     GO TO ADD-WRITE-F1-39.                                       NC1764.2
131100 ADD-DELETE-F1-39.                                                NC1764.2
131200     PERFORM DE-LETE.                                             NC1764.2
131300 ADD-WRITE-F1-39.                                                 NC1764.2
131400     MOVE "ADD-TEST-F1-39 " TO PAR-NAME.                          NC1764.2
131500     PERFORM PRINT-DETAIL.                                        NC1764.2
131600 ADD-TEST-F1-40.                                                  NC1764.2
131700     IF DNAME37 EQUAL TO 21                                       NC1764.2
131800         PERFORM PASS                                             NC1764.2
131900         GO TO ADD-WRITE-F1-40.                                   NC1764.2
132000     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
132100     MOVE DNAME37 TO COMPUTED-18V0.                               NC1764.2
132200     PERFORM FAIL.                                                NC1764.2
132300     GO TO ADD-WRITE-F1-40.                                       NC1764.2
132400 ADD-DELETE-F1-40.                                                NC1764.2
132500     PERFORM DE-LETE.                                             NC1764.2
132600 ADD-WRITE-F1-40.                                                 NC1764.2
132700     MOVE "ADD-TEST-F1-40 " TO PAR-NAME.                          NC1764.2
132800     PERFORM PRINT-DETAIL.                                        NC1764.2
132900 ADD-TEST-F1-41.                                                  NC1764.2
133000     IF DNAME38 EQUAL TO 21                                       NC1764.2
133100         PERFORM PASS                                             NC1764.2
133200         GO TO ADD-WRITE-F1-41.                                   NC1764.2
133300     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
133400     MOVE DNAME38 TO COMPUTED-18V0.                               NC1764.2
133500     PERFORM FAIL.                                                NC1764.2
133600     GO TO ADD-WRITE-F1-41.                                       NC1764.2
133700 ADD-DELETE-F1-41.                                                NC1764.2
133800     PERFORM DE-LETE.                                             NC1764.2
133900 ADD-WRITE-F1-41.                                                 NC1764.2
134000     MOVE "ADD-TEST-F1-41 " TO PAR-NAME.                          NC1764.2
134100     PERFORM PRINT-DETAIL.                                        NC1764.2
134200 ADD-TEST-F1-42.                                                  NC1764.2
134300     IF DNAME39 EQUAL TO 21                                       NC1764.2
134400         PERFORM PASS                                             NC1764.2
134500         GO TO ADD-WRITE-F1-42.                                   NC1764.2
134600     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
134700     MOVE DNAME39 TO COMPUTED-18V0.                               NC1764.2
134800     PERFORM FAIL.                                                NC1764.2
134900     GO TO ADD-WRITE-F1-42.                                       NC1764.2
135000 ADD-DELETE-F1-42.                                                NC1764.2
135100     PERFORM DE-LETE.                                             NC1764.2
135200 ADD-WRITE-F1-42.                                                 NC1764.2
135300     MOVE "ADD-TEST-F1-42 " TO PAR-NAME.                          NC1764.2
135400     PERFORM PRINT-DETAIL.                                        NC1764.2
135500 ADD-TEST-F1-43.                                                  NC1764.2
135600     IF DNAME40 EQUAL TO 21                                       NC1764.2
135700         PERFORM PASS                                             NC1764.2
135800         GO TO ADD-WRITE-F1-43.                                   NC1764.2
135900     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
136000     MOVE DNAME40 TO COMPUTED-18V0.                               NC1764.2
136100     PERFORM FAIL.                                                NC1764.2
136200     GO TO ADD-WRITE-F1-43.                                       NC1764.2
136300 ADD-DELETE-F1-43.                                                NC1764.2
136400     PERFORM DE-LETE.                                             NC1764.2
136500 ADD-WRITE-F1-43.                                                 NC1764.2
136600     MOVE "ADD-TEST-F1-43 " TO PAR-NAME.                          NC1764.2
136700     PERFORM PRINT-DETAIL.                                        NC1764.2
136800 ADD-TEST-F1-44.                                                  NC1764.2
136900     IF DNAME41 EQUAL TO 21                                       NC1764.2
137000         PERFORM PASS                                             NC1764.2
137100         GO TO ADD-WRITE-F1-44.                                   NC1764.2
137200     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
137300     MOVE DNAME41 TO COMPUTED-18V0.                               NC1764.2
137400     PERFORM FAIL.                                                NC1764.2
137500     GO TO ADD-WRITE-F1-44.                                       NC1764.2
137600 ADD-DELETE-F1-44.                                                NC1764.2
137700     PERFORM DE-LETE.                                             NC1764.2
137800 ADD-WRITE-F1-44.                                                 NC1764.2
137900     MOVE "ADD-TEST-F1-44 " TO PAR-NAME.                          NC1764.2
138000     PERFORM PRINT-DETAIL.                                        NC1764.2
138100 ADD-TEST-F1-45.                                                  NC1764.2
138200     IF DNAME42 EQUAL TO 21                                       NC1764.2
138300         PERFORM PASS                                             NC1764.2
138400         GO TO ADD-WRITE-F1-45.                                   NC1764.2
138500     MOVE 21 TO CORRECT-18V0.                                     NC1764.2
138600     MOVE DNAME42 TO COMPUTED-18V0.                               NC1764.2
138700     PERFORM FAIL.                                                NC1764.2
138800     GO TO ADD-WRITE-F1-45.                                       NC1764.2
138900 ADD-DELETE-F1-45.                                                NC1764.2
139000     PERFORM DE-LETE.                                             NC1764.2
139100 ADD-WRITE-F1-45.                                                 NC1764.2
139200     MOVE "ADD-TEST-F1-45 " TO PAR-NAME.                          NC1764.2
139300     PERFORM PRINT-DETAIL.                                        NC1764.2
139400*                                                                 NC1764.2
139500 ADD-INIT-F1-46.                                                  NC1764.2
139600*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
139700     MOVE   "VI-74 6.6.4 GR1" TO ANSI-REFERENCE.                  NC1764.2
139800     MOVE   "ADD-TEST-F1-46"  TO PAR-NAME.                        NC1764.2
139900     MOVE   "ADD-TO-SERIES"   TO FEATURE.                         NC1764.2
140000     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
140100             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
140200     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
140300     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
140400     MOVE 1 TO REC-CT.                                            NC1764.2
140500 ADD-TEST-F1-46-0.                                                NC1764.2
140600     ADD WRK-DU-1V1-1  WRK-DU-1V1-2  6 TO WRK-DU-2V1-1,           NC1764.2
140700         WRK-DU-2V0-1 ROUNDED  WRK-DU-2V1-2  WRK-DU-2V0-2 ROUNDED NC1764.2
140800         WRK-DU-2V1-3  WRK-DU-2V0-3.                              NC1764.2
140900     GO TO   ADD-TEST-F1-46-1.                                    NC1764.2
141000 ADD-DELETE-F1-46.                                                NC1764.2
141100     PERFORM DE-LETE.                                             NC1764.2
141200     PERFORM PRINT-DETAIL.                                        NC1764.2
141300     GO TO ADD-INIT-F1-47.                                        NC1764.2
141400 ADD-TEST-F1-46-1.                                                NC1764.2
141500     IF WRK-DU-2V1-1 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
141600         ELSE                                                     NC1764.2
141700         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.5    NC1764.2
141800         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
141900     ADD 1 TO REC-CT.                                             NC1764.2
142000 ADD-TEST-F1-46-2.                                                NC1764.2
142100     IF WRK-DU-2V0-1 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
142200         ELSE                                                     NC1764.2
142300         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 10 TO  NC1764.2
142400         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
142500     ADD 1 TO REC-CT.                                             NC1764.2
142600 ADD-TEST-F1-46-3.                                                NC1764.2
142700     IF WRK-DU-2V1-2 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
142800         ELSE                                                     NC1764.2
142900         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.5 TO NC1764.2
143000         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
143100     ADD 1 TO REC-CT.                                             NC1764.2
143200 ADD-TEST-F1-46-4.                                                NC1764.2
143300     IF WRK-DU-2V0-2 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
143400         ELSE                                                     NC1764.2
143500         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 10 TO  NC1764.2
143600         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
143700     ADD 1 TO REC-CT.                                             NC1764.2
143800 ADD-TEST-F1-46-5.                                                NC1764.2
143900     IF WRK-DU-2V1-3 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
144000         ELSE                                                     NC1764.2
144100         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.5 TO NC1764.2
144200         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
144300     ADD 1 TO REC-CT.                                             NC1764.2
144400 ADD-TEST-F1-46-6.                                                NC1764.2
144500     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1764.2
144600         ELSE                                                     NC1764.2
144700         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9 TO   NC1764.2
144800         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
144900*                                                                 NC1764.2
145000 ADD-INIT-F1-47.                                                  NC1764.2
145100*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
145200*    ==-->    NEW SIZE ERROR TESTS  <--==                         NC1764.2
145300     MOVE   "VI-67 6.4.2"     TO ANSI-REFERENCE.                  NC1764.2
145400     MOVE   "ADD-TEST-F1-47"  TO PAR-NAME.                        NC1764.2
145500     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
145600             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
145700     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
145800     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
145900     MOVE   "0"  TO WRK-XN-00001.                                 NC1764.2
146000     MOVE 1 TO REC-CT.                                            NC1764.2
146100 ADD-TEST-F1-47-0.                                                NC1764.2
146200     ADD WRK-DU-1V1-1  WRK-DU-1V1-2  6 TO WRK-DU-2V1-1,           NC1764.2
146300         WRK-DU-2V0-1 ROUNDED  WRK-DU-2V1-2  WRK-DU-2V0-2 ROUNDED NC1764.2
146400         WRK-DU-2V1-3  WRK-DU-2V0-3                               NC1764.2
146500          ON SIZE ERROR                                           NC1764.2
146600             MOVE   "1" TO WRK-XN-00001.                          NC1764.2
146700     GO TO   ADD-TEST-F1-47-1.                                    NC1764.2
146800 ADD-DELETE-F1-47.                                                NC1764.2
146900     PERFORM DE-LETE.                                             NC1764.2
147000     PERFORM PRINT-DETAIL.                                        NC1764.2
147100     GO TO ADD-INIT-F1-48.                                        NC1764.2
147200 ADD-TEST-F1-47-1.                                                NC1764.2
147300     MOVE   "ADD-TEST-F1-47-1"  TO PAR-NAME.                      NC1764.2
147400     IF WRK-DU-2V1-1 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
147500         ELSE                                                     NC1764.2
147600         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.5    NC1764.2
147700         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
147800     ADD 1 TO REC-CT.                                             NC1764.2
147900 ADD-TEST-F1-47-2.                                                NC1764.2
148000     MOVE   "ADD-TEST-F1-47-2"  TO PAR-NAME.                      NC1764.2
148100     IF WRK-DU-2V0-1 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
148200         ELSE                                                     NC1764.2
148300         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 10 TO  NC1764.2
148400         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
148500     ADD 1 TO REC-CT.                                             NC1764.2
148600 ADD-TEST-F1-47-3.                                                NC1764.2
148700     MOVE   "ADD-TEST-F1-47-3"  TO PAR-NAME.                      NC1764.2
148800     IF WRK-DU-2V1-2 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
148900         ELSE                                                     NC1764.2
149000         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.5 TO NC1764.2
149100         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
149200     ADD 1 TO REC-CT.                                             NC1764.2
149300 ADD-TEST-F1-47-4.                                                NC1764.2
149400     MOVE   "ADD-TEST-F1-47-4"  TO PAR-NAME.                      NC1764.2
149500     IF WRK-DU-2V0-2 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
149600         ELSE                                                     NC1764.2
149700         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 10 TO  NC1764.2
149800         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
149900     ADD 1 TO REC-CT.                                             NC1764.2
150000 ADD-TEST-F1-47-5.                                                NC1764.2
150100     MOVE   "ADD-TEST-F1-47-5"  TO PAR-NAME.                      NC1764.2
150200     IF WRK-DU-2V1-3 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
150300         ELSE                                                     NC1764.2
150400         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.5 TO NC1764.2
150500         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
150600     ADD 1 TO REC-CT.                                             NC1764.2
150700 ADD-TEST-F1-47-6.                                                NC1764.2
150800     MOVE   "ADD-TEST-F1-47-6"  TO PAR-NAME.                      NC1764.2
150900     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1764.2
151000         ELSE                                                     NC1764.2
151100         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9 TO   NC1764.2
151200         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
151300     ADD 1 TO REC-CT.                                             NC1764.2
151400 ADD-TEST-F1-47-7.                                                NC1764.2
151500     MOVE   "ADD-TEST-F1-47-7"  TO PAR-NAME.                      NC1764.2
151600     IF      WRK-XN-00001 = "0"                                   NC1764.2
151700             PERFORM PASS                                         NC1764.2
151800             PERFORM PRINT-DETAIL                                 NC1764.2
151900     ELSE                                                         NC1764.2
152000             MOVE   "SIZE ERROR SHOULD NOT BE EXECUTED"           NC1764.2
152100                  TO RE-MARK                                      NC1764.2
152200             MOVE   "0"           TO CORRECT-X                    NC1764.2
152300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
152400             PERFORM FAIL                                         NC1764.2
152500             PERFORM PRINT-DETAIL.                                NC1764.2
152600*                                                                 NC1764.2
152700 ADD-INIT-F1-48.                                                  NC1764.2
152800*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
152900*    ==-->    NEW SIZE ERROR TESTS  <--==                         NC1764.2
153000     MOVE   "VI-67 6.4.2"     TO ANSI-REFERENCE.                  NC1764.2
153100     MOVE   "ADD-TEST-F1-48"  TO PAR-NAME.                        NC1764.2
153200     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
153300             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
153400     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
153500     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
153600     MOVE   "0"  TO WRK-XN-00001.                                 NC1764.2
153700     MOVE 1 TO REC-CT.                                            NC1764.2
153800 ADD-TEST-F1-48-0.                                                NC1764.2
153900     ADD     A17TWOS-DS-17V00                                     NC1764.2
154000             WRK-DU-1V1-2  6                                      NC1764.2
154100          TO WRK-DU-2V1-1                                         NC1764.2
154200             WRK-DU-2V0-1 ROUNDED                                 NC1764.2
154300             WRK-DU-2V1-2                                         NC1764.2
154400             WRK-DU-2V0-2 ROUNDED                                 NC1764.2
154500             WRK-DU-2V1-3                                         NC1764.2
154600             WRK-DU-2V0-3                                         NC1764.2
154700          ON SIZE ERROR                                           NC1764.2
154800             MOVE   "1" TO WRK-XN-00001.                          NC1764.2
154900     GO TO   ADD-TEST-F1-48-1.                                    NC1764.2
155000 ADD-DELETE-F1-48.                                                NC1764.2
155100     PERFORM DE-LETE.                                             NC1764.2
155200     PERFORM PRINT-DETAIL.                                        NC1764.2
155300     GO TO   ADD-INIT-F1-49.                                      NC1764.2
155400 ADD-TEST-F1-48-1.                                                NC1764.2
155500     MOVE   "ADD-TEST-F1-48-1"  TO PAR-NAME.                      NC1764.2
155600     IF WRK-DU-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
155700         ELSE                                                     NC1764.2
155800         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 0      NC1764.2
155900         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
156000     ADD 1 TO REC-CT.                                             NC1764.2
156100 ADD-TEST-F1-48-2.                                                NC1764.2
156200     MOVE   "ADD-TEST-F1-48-2"  TO PAR-NAME.                      NC1764.2
156300     IF WRK-DU-2V0-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
156400         ELSE                                                     NC1764.2
156500         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 0 TO   NC1764.2
156600         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
156700     ADD 1 TO REC-CT.                                             NC1764.2
156800 ADD-TEST-F1-48-3.                                                NC1764.2
156900     MOVE   "ADD-TEST-F1-48-3"  TO PAR-NAME.                      NC1764.2
157000     IF WRK-DU-2V1-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
157100         ELSE                                                     NC1764.2
157200         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 0 TO   NC1764.2
157300         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
157400     ADD 1 TO REC-CT.                                             NC1764.2
157500 ADD-TEST-F1-48-4.                                                NC1764.2
157600     MOVE   "ADD-TEST-F1-48-4"  TO PAR-NAME.                      NC1764.2
157700     IF WRK-DU-2V0-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
157800         ELSE                                                     NC1764.2
157900         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 0 TO   NC1764.2
158000         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
158100     ADD 1 TO REC-CT.                                             NC1764.2
158200 ADD-TEST-F1-48-5.                                                NC1764.2
158300     MOVE   "ADD-TEST-F1-48-5"  TO PAR-NAME.                      NC1764.2
158400     IF WRK-DU-2V1-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
158500         ELSE                                                     NC1764.2
158600         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 0 TO   NC1764.2
158700         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
158800     ADD 1 TO REC-CT.                                             NC1764.2
158900 ADD-TEST-F1-48-6.                                                NC1764.2
159000     MOVE   "ADD-TEST-F1-48-6"  TO PAR-NAME.                      NC1764.2
159100     IF WRK-DU-2V0-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
159200         ELSE                                                     NC1764.2
159300         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 0 TO   NC1764.2
159400         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
159500     ADD 1 TO REC-CT.                                             NC1764.2
159600 ADD-TEST-F1-48-7.                                                NC1764.2
159700     MOVE   "ADD-TEST-F1-48-7"  TO PAR-NAME.                      NC1764.2
159800     IF      WRK-XN-00001 = "1"                                   NC1764.2
159900             PERFORM PASS                                         NC1764.2
160000             PERFORM PRINT-DETAIL                                 NC1764.2
160100     ELSE                                                         NC1764.2
160200             MOVE   "SIZE ERROR NOT EXECUTED" TO RE-MARK          NC1764.2
160300             MOVE   "1"           TO CORRECT-X                    NC1764.2
160400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
160500             PERFORM FAIL                                         NC1764.2
160600             PERFORM PRINT-DETAIL.                                NC1764.2
160700*                                                                 NC1764.2
160800 ADD-INIT-F1-49.                                                  NC1764.2
160900*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
161000*    ==-->    NEW SIZE ERROR TESTS  <--==                         NC1764.2
161100     MOVE   "VI-67 6.4.2"     TO ANSI-REFERENCE.                  NC1764.2
161200     MOVE   "ADD-TEST-F1-49"  TO PAR-NAME.                        NC1764.2
161300     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
161400             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
161500     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
161600     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
161700     MOVE   "0"  TO WRK-XN-00001.                                 NC1764.2
161800     MOVE 1 TO REC-CT.                                            NC1764.2
161900 ADD-TEST-F1-49-0.                                                NC1764.2
162000     ADD WRK-DU-1V1-1  WRK-DU-1V1-2  6 TO WRK-DU-2V1-1,           NC1764.2
162100         WRK-DU-2V0-1 ROUNDED  WRK-DU-2V1-2  WRK-DU-2V0-2 ROUNDED NC1764.2
162200         WRK-DU-2V1-3  WRK-DU-2V0-3                               NC1764.2
162300      NOT ON SIZE ERROR                                           NC1764.2
162400             MOVE   "1" TO WRK-XN-00001.                          NC1764.2
162500     GO TO   ADD-TEST-F1-49-1.                                    NC1764.2
162600 ADD-DELETE-F1-49.                                                NC1764.2
162700     PERFORM DE-LETE.                                             NC1764.2
162800     PERFORM PRINT-DETAIL.                                        NC1764.2
162900     GO TO ADD-INIT-F1-50.                                        NC1764.2
163000 ADD-TEST-F1-49-1.                                                NC1764.2
163100     MOVE   "ADD-TEST-F1-49-1"  TO PAR-NAME.                      NC1764.2
163200     IF WRK-DU-2V1-1 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
163300         ELSE                                                     NC1764.2
163400         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.5    NC1764.2
163500         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
163600     ADD 1 TO REC-CT.                                             NC1764.2
163700 ADD-TEST-F1-49-2.                                                NC1764.2
163800     MOVE   "ADD-TEST-F1-49-2"  TO PAR-NAME.                      NC1764.2
163900     IF WRK-DU-2V0-1 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
164000         ELSE                                                     NC1764.2
164100         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 10 TO  NC1764.2
164200         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
164300     ADD 1 TO REC-CT.                                             NC1764.2
164400 ADD-TEST-F1-49-3.                                                NC1764.2
164500     MOVE   "ADD-TEST-F1-49-3"  TO PAR-NAME.                      NC1764.2
164600     IF WRK-DU-2V1-2 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
164700         ELSE                                                     NC1764.2
164800         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.5 TO NC1764.2
164900         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
165000     ADD 1 TO REC-CT.                                             NC1764.2
165100 ADD-TEST-F1-49-4.                                                NC1764.2
165200     MOVE   "ADD-TEST-F1-49-4"  TO PAR-NAME.                      NC1764.2
165300     IF WRK-DU-2V0-2 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
165400         ELSE                                                     NC1764.2
165500         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 10 TO  NC1764.2
165600         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
165700     ADD 1 TO REC-CT.                                             NC1764.2
165800 ADD-TEST-F1-49-5.                                                NC1764.2
165900     MOVE   "ADD-TEST-F1-49-5"  TO PAR-NAME.                      NC1764.2
166000     IF WRK-DU-2V1-3 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
166100         ELSE                                                     NC1764.2
166200         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.5 TO NC1764.2
166300         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
166400     ADD 1 TO REC-CT.                                             NC1764.2
166500 ADD-TEST-F1-49-6.                                                NC1764.2
166600     MOVE   "ADD-TEST-F1-49-6"  TO PAR-NAME.                      NC1764.2
166700     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1764.2
166800         ELSE                                                     NC1764.2
166900         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9 TO   NC1764.2
167000         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
167100     ADD 1 TO REC-CT.                                             NC1764.2
167200 ADD-TEST-F1-49-7.                                                NC1764.2
167300     MOVE   "ADD-TEST-F1-49-7"  TO PAR-NAME.                      NC1764.2
167400     IF      WRK-XN-00001 = "1"                                   NC1764.2
167500             PERFORM PASS                                         NC1764.2
167600             PERFORM PRINT-DETAIL                                 NC1764.2
167700     ELSE                                                         NC1764.2
167800             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1764.2
167900                  TO RE-MARK                                      NC1764.2
168000             MOVE   "1"           TO CORRECT-X                    NC1764.2
168100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
168200             PERFORM FAIL                                         NC1764.2
168300             PERFORM PRINT-DETAIL.                                NC1764.2
168400*                                                                 NC1764.2
168500 ADD-INIT-F1-50.                                                  NC1764.2
168600*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
168700*    ==-->    NEW SIZE ERROR TESTS  <--==                         NC1764.2
168800     MOVE   "VI-67 6.4.2"     TO ANSI-REFERENCE.                  NC1764.2
168900     MOVE   "ADD-TEST-F1-50"  TO PAR-NAME.                        NC1764.2
169000     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
169100             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
169200     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
169300     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
169400     MOVE   "0"  TO WRK-XN-00001.                                 NC1764.2
169500     MOVE 1 TO REC-CT.                                            NC1764.2
169600 ADD-TEST-F1-50-0.                                                NC1764.2
169700     ADD     A17TWOS-DS-17V00                                     NC1764.2
169800             WRK-DU-1V1-2  6                                      NC1764.2
169900          TO WRK-DU-2V1-1                                         NC1764.2
170000             WRK-DU-2V0-1 ROUNDED                                 NC1764.2
170100             WRK-DU-2V1-2                                         NC1764.2
170200             WRK-DU-2V0-2 ROUNDED                                 NC1764.2
170300             WRK-DU-2V1-3                                         NC1764.2
170400             WRK-DU-2V0-3                                         NC1764.2
170500      NOT ON SIZE ERROR                                           NC1764.2
170600             MOVE   "1" TO WRK-XN-00001.                          NC1764.2
170700     GO TO   ADD-TEST-F1-50-1.                                    NC1764.2
170800 ADD-DELETE-F1-50.                                                NC1764.2
170900     PERFORM DE-LETE.                                             NC1764.2
171000     PERFORM PRINT-DETAIL.                                        NC1764.2
171100     GO TO   ADD-INIT-F1-51.                                      NC1764.2
171200 ADD-TEST-F1-50-1.                                                NC1764.2
171300     MOVE   "ADD-TEST-F1-50-1"  TO PAR-NAME.                      NC1764.2
171400     IF WRK-DU-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
171500         ELSE                                                     NC1764.2
171600         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 0      NC1764.2
171700         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
171800     ADD 1 TO REC-CT.                                             NC1764.2
171900 ADD-TEST-F1-50-2.                                                NC1764.2
172000     MOVE   "ADD-TEST-F1-50-2"  TO PAR-NAME.                      NC1764.2
172100     IF WRK-DU-2V0-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
172200         ELSE                                                     NC1764.2
172300         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 0 TO   NC1764.2
172400         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
172500     ADD 1 TO REC-CT.                                             NC1764.2
172600 ADD-TEST-F1-50-3.                                                NC1764.2
172700     MOVE   "ADD-TEST-F1-50-3"  TO PAR-NAME.                      NC1764.2
172800     IF WRK-DU-2V1-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
172900         ELSE                                                     NC1764.2
173000         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 0 TO   NC1764.2
173100         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
173200     ADD 1 TO REC-CT.                                             NC1764.2
173300 ADD-TEST-F1-50-4.                                                NC1764.2
173400     MOVE   "ADD-TEST-F1-50-4"  TO PAR-NAME.                      NC1764.2
173500     IF WRK-DU-2V0-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
173600         ELSE                                                     NC1764.2
173700         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 0 TO   NC1764.2
173800         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
173900     ADD 1 TO REC-CT.                                             NC1764.2
174000 ADD-TEST-F1-50-5.                                                NC1764.2
174100     MOVE   "ADD-TEST-F1-50-5"  TO PAR-NAME.                      NC1764.2
174200     IF WRK-DU-2V1-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
174300         ELSE                                                     NC1764.2
174400         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 0 TO   NC1764.2
174500         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
174600     ADD 1 TO REC-CT.                                             NC1764.2
174700 ADD-TEST-F1-50-6.                                                NC1764.2
174800     MOVE   "ADD-TEST-F1-50-6"  TO PAR-NAME.                      NC1764.2
174900     IF WRK-DU-2V0-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
175000         ELSE                                                     NC1764.2
175100         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 0 TO   NC1764.2
175200         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
175300     ADD 1 TO REC-CT.                                             NC1764.2
175400 ADD-TEST-F1-50-7.                                                NC1764.2
175500     MOVE   "ADD-TEST-F1-50-7"  TO PAR-NAME.                      NC1764.2
175600     IF      WRK-XN-00001 = "0"                                   NC1764.2
175700             PERFORM PASS                                         NC1764.2
175800             PERFORM PRINT-DETAIL                                 NC1764.2
175900     ELSE                                                         NC1764.2
176000             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
176100                  TO RE-MARK                                      NC1764.2
176200             MOVE   "0"           TO CORRECT-X                    NC1764.2
176300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
176400             PERFORM FAIL                                         NC1764.2
176500             PERFORM PRINT-DETAIL.                                NC1764.2
176600*                                                                 NC1764.2
176700 ADD-INIT-F1-51.                                                  NC1764.2
176800*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
176900*    ==-->    NEW SIZE ERROR TESTS  <--==                         NC1764.2
177000     MOVE   "VI-67 6.4.2"     TO ANSI-REFERENCE.                  NC1764.2
177100     MOVE   "ADD-TEST-F1-51"  TO PAR-NAME.                        NC1764.2
177200     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
177300             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
177400     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
177500     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
177600     MOVE   "0"  TO WRK-XN-00001.                                 NC1764.2
177700     MOVE 1 TO REC-CT.                                            NC1764.2
177800 ADD-TEST-F1-51-0.                                                NC1764.2
177900     ADD WRK-DU-1V1-1  WRK-DU-1V1-2  6 TO WRK-DU-2V1-1,           NC1764.2
178000         WRK-DU-2V0-1 ROUNDED  WRK-DU-2V1-2  WRK-DU-2V0-2 ROUNDED NC1764.2
178100         WRK-DU-2V1-3  WRK-DU-2V0-3                               NC1764.2
178200          ON SIZE ERROR                                           NC1764.2
178300             MOVE   "1" TO WRK-XN-00001                           NC1764.2
178400      NOT ON SIZE ERROR                                           NC1764.2
178500             MOVE   "2" TO WRK-XN-00001.                          NC1764.2
178600     GO TO   ADD-TEST-F1-51-1.                                    NC1764.2
178700 ADD-DELETE-F1-51.                                                NC1764.2
178800     PERFORM DE-LETE.                                             NC1764.2
178900     PERFORM PRINT-DETAIL.                                        NC1764.2
179000     GO TO ADD-INIT-F1-52.                                        NC1764.2
179100 ADD-TEST-F1-51-1.                                                NC1764.2
179200     MOVE   "ADD-TEST-F1-51-1"  TO PAR-NAME.                      NC1764.2
179300     IF WRK-DU-2V1-1 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
179400         ELSE                                                     NC1764.2
179500         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.5    NC1764.2
179600         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
179700     ADD 1 TO REC-CT.                                             NC1764.2
179800 ADD-TEST-F1-51-2.                                                NC1764.2
179900     MOVE   "ADD-TEST-F1-51-2"  TO PAR-NAME.                      NC1764.2
180000     IF WRK-DU-2V0-1 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
180100         ELSE                                                     NC1764.2
180200         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 10 TO  NC1764.2
180300         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
180400     ADD 1 TO REC-CT.                                             NC1764.2
180500 ADD-TEST-F1-51-3.                                                NC1764.2
180600     MOVE   "ADD-TEST-F1-51-3"  TO PAR-NAME.                      NC1764.2
180700     IF WRK-DU-2V1-2 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
180800         ELSE                                                     NC1764.2
180900         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.5 TO NC1764.2
181000         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
181100     ADD 1 TO REC-CT.                                             NC1764.2
181200 ADD-TEST-F1-51-4.                                                NC1764.2
181300     MOVE   "ADD-TEST-F1-51-4"  TO PAR-NAME.                      NC1764.2
181400     IF WRK-DU-2V0-2 = 10 PERFORM PASS PERFORM PRINT-DETAIL       NC1764.2
181500         ELSE                                                     NC1764.2
181600         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 10 TO  NC1764.2
181700         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
181800     ADD 1 TO REC-CT.                                             NC1764.2
181900 ADD-TEST-F1-51-5.                                                NC1764.2
182000     MOVE   "ADD-TEST-F1-51-5"  TO PAR-NAME.                      NC1764.2
182100     IF WRK-DU-2V1-3 = 9.5 PERFORM PASS PERFORM PRINT-DETAIL      NC1764.2
182200         ELSE                                                     NC1764.2
182300         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.5 TO NC1764.2
182400         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
182500     ADD 1 TO REC-CT.                                             NC1764.2
182600 ADD-TEST-F1-51-6.                                                NC1764.2
182700     MOVE   "ADD-TEST-F1-51-6"  TO PAR-NAME.                      NC1764.2
182800     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1764.2
182900         ELSE                                                     NC1764.2
183000         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9 TO   NC1764.2
183100         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
183200     ADD 1 TO REC-CT.                                             NC1764.2
183300 ADD-TEST-F1-51-7.                                                NC1764.2
183400     MOVE   "ADD-TEST-F1-51-7"  TO PAR-NAME.                      NC1764.2
183500     IF      WRK-XN-00001 = "2"                                   NC1764.2
183600             PERFORM PASS                                         NC1764.2
183700             PERFORM PRINT-DETAIL                                 NC1764.2
183800     ELSE                                                         NC1764.2
183900             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1764.2
184000                  TO RE-MARK                                      NC1764.2
184100             MOVE   "2"           TO CORRECT-X                    NC1764.2
184200             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
184300             PERFORM FAIL                                         NC1764.2
184400             PERFORM PRINT-DETAIL.                                NC1764.2
184500*                                                                 NC1764.2
184600 ADD-INIT-F1-52.                                                  NC1764.2
184700*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1764.2
184800*    ==-->    NEW SIZE ERROR TESTS  <--==                         NC1764.2
184900     MOVE   "VI-67 6.4.2"     TO ANSI-REFERENCE.                  NC1764.2
185000     MOVE   "ADD-TEST-F1-52"  TO PAR-NAME.                        NC1764.2
185100     MOVE    ZERO TO WRK-DU-2V1-1 WRK-DU-2V0-1 WRK-DU-2V1-2       NC1764.2
185200             WRK-DU-2V0-2 WRK-DU-2V1-3 WRK-DU-2V0-3.              NC1764.2
185300     MOVE    1.1 TO WRK-DU-1V1-1.                                 NC1764.2
185400     MOVE    2.4 TO WRK-DU-1V1-2.                                 NC1764.2
185500     MOVE   "0"  TO WRK-XN-00001.                                 NC1764.2
185600     MOVE 1 TO REC-CT.                                            NC1764.2
185700 ADD-TEST-F1-52-0.                                                NC1764.2
185800     ADD     A17TWOS-DS-17V00                                     NC1764.2
185900             WRK-DU-1V1-2  6                                      NC1764.2
186000          TO WRK-DU-2V1-1                                         NC1764.2
186100             WRK-DU-2V0-1 ROUNDED                                 NC1764.2
186200             WRK-DU-2V1-2                                         NC1764.2
186300             WRK-DU-2V0-2 ROUNDED                                 NC1764.2
186400             WRK-DU-2V1-3                                         NC1764.2
186500             WRK-DU-2V0-3                                         NC1764.2
186600          ON SIZE ERROR                                           NC1764.2
186700             MOVE   "1" TO WRK-XN-00001                           NC1764.2
186800      NOT ON SIZE ERROR                                           NC1764.2
186900             MOVE   "2" TO WRK-XN-00001.                          NC1764.2
187000     GO TO   ADD-TEST-F1-52-1.                                    NC1764.2
187100 ADD-DELETE-F1-52.                                                NC1764.2
187200     PERFORM DE-LETE.                                             NC1764.2
187300     PERFORM PRINT-DETAIL.                                        NC1764.2
187400     GO TO   ADD-INIT-F1-53.                                      NC1764.2
187500 ADD-TEST-F1-52-1.                                                NC1764.2
187600     MOVE   "ADD-TEST-F1-52-1"  TO PAR-NAME.                      NC1764.2
187700     IF WRK-DU-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
187800         ELSE                                                     NC1764.2
187900         PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 0      NC1764.2
188000         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1764.2
188100     ADD 1 TO REC-CT.                                             NC1764.2
188200 ADD-TEST-F1-52-2.                                                NC1764.2
188300     MOVE   "ADD-TEST-F1-52-2"  TO PAR-NAME.                      NC1764.2
188400     IF WRK-DU-2V0-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
188500         ELSE                                                     NC1764.2
188600         PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 0 TO   NC1764.2
188700         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
188800     ADD 1 TO REC-CT.                                             NC1764.2
188900 ADD-TEST-F1-52-3.                                                NC1764.2
189000     MOVE   "ADD-TEST-F1-52-3"  TO PAR-NAME.                      NC1764.2
189100     IF WRK-DU-2V1-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
189200         ELSE                                                     NC1764.2
189300         PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 0 TO   NC1764.2
189400         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
189500     ADD 1 TO REC-CT.                                             NC1764.2
189600 ADD-TEST-F1-52-4.                                                NC1764.2
189700     MOVE   "ADD-TEST-F1-52-4"  TO PAR-NAME.                      NC1764.2
189800     IF WRK-DU-2V0-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
189900         ELSE                                                     NC1764.2
190000         PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 0 TO   NC1764.2
190100         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
190200     ADD 1 TO REC-CT.                                             NC1764.2
190300 ADD-TEST-F1-52-5.                                                NC1764.2
190400     MOVE   "ADD-TEST-F1-52-5"  TO PAR-NAME.                      NC1764.2
190500     IF WRK-DU-2V1-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
190600         ELSE                                                     NC1764.2
190700         PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 0 TO   NC1764.2
190800         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
190900     ADD 1 TO REC-CT.                                             NC1764.2
191000 ADD-TEST-F1-52-6.                                                NC1764.2
191100     MOVE   "ADD-TEST-F1-52-6"  TO PAR-NAME.                      NC1764.2
191200     IF WRK-DU-2V0-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1764.2
191300         ELSE                                                     NC1764.2
191400         PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 0 TO   NC1764.2
191500         CORRECT-N PERFORM PRINT-DETAIL.                          NC1764.2
191600     ADD 1 TO REC-CT.                                             NC1764.2
191700 ADD-TEST-F1-52-7.                                                NC1764.2
191800     MOVE   "ADD-TEST-F1-52-7"  TO PAR-NAME.                      NC1764.2
191900     IF      WRK-XN-00001 = "1"                                   NC1764.2
192000             PERFORM PASS                                         NC1764.2
192100             PERFORM PRINT-DETAIL                                 NC1764.2
192200     ELSE                                                         NC1764.2
192300             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1764.2
192400                  TO RE-MARK                                      NC1764.2
192500             MOVE   "1"           TO CORRECT-X                    NC1764.2
192600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
192700             PERFORM FAIL                                         NC1764.2
192800             PERFORM PRINT-DETAIL.                                NC1764.2
192900*                                                                 NC1764.2
193000 ADD-INIT-F1-53.                                                  NC1764.2
193100*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1764.2
193200     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1764.2
193300     MOVE   "ADD-TEST-F1-53"  TO PAR-NAME.                        NC1764.2
193400     MOVE    SPACE TO WRK-XN-00001.                               NC1764.2
193500     MOVE    SPACE TO SIZE-ERR2.                                  NC1764.2
193600     MOVE    SPACE TO SIZE-ERR3.                                  NC1764.2
193700     MOVE    SPACE TO SIZE-ERR4.                                  NC1764.2
193800     MOVE   -11 TO WRK-DS-02V00.                                  NC1764.2
193900     MOVE    1  TO REC-CT.                                        NC1764.2
194000 ADD-TEST-F1-53-0.                                                NC1764.2
194100     ADD    -99 TO WRK-DS-02V00                                   NC1764.2
194200             ON SIZE ERROR                                        NC1764.2
194300             MOVE   "1" TO WRK-XN-00001                           NC1764.2
194400             MOVE   "A" TO SIZE-ERR2                              NC1764.2
194500             MOVE   "B" TO SIZE-ERR3                              NC1764.2
194600     END-ADD                                                      NC1764.2
194700     MOVE   "C" TO SIZE-ERR4.                                     NC1764.2
194800     GO TO   ADD-TEST-F1-53-1.                                    NC1764.2
194900 ADD-DELETE-F1-53.                                                NC1764.2
195000     PERFORM DE-LETE.                                             NC1764.2
195100     PERFORM PRINT-DETAIL.                                        NC1764.2
195200     GO TO   ADD-INIT-F1-54.                                      NC1764.2
195300 ADD-TEST-F1-53-1.                                                NC1764.2
195400     MOVE   "ADD-TEST-F1-53-1" TO PAR-NAME.                       NC1764.2
195500     IF      WRK-XN-00001 = "1"                                   NC1764.2
195600             PERFORM PASS                                         NC1764.2
195700             PERFORM PRINT-DETAIL                                 NC1764.2
195800     ELSE                                                         NC1764.2
195900             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1764.2
196000                  TO RE-MARK                                      NC1764.2
196100             MOVE   "1"           TO CORRECT-X                    NC1764.2
196200             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
196300             PERFORM FAIL                                         NC1764.2
196400             PERFORM PRINT-DETAIL.                                NC1764.2
196500     ADD     1 TO REC-CT.                                         NC1764.2
196600 ADD-TEST-F1-53-2.                                                NC1764.2
196700     MOVE   "ADD-TEST-F1-53-2" TO PAR-NAME.                       NC1764.2
196800     IF      SIZE-ERR2 = "A"                                      NC1764.2
196900             PERFORM PASS                                         NC1764.2
197000             PERFORM PRINT-DETAIL                                 NC1764.2
197100     ELSE                                                         NC1764.2
197200             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1764.2
197300                  TO RE-MARK                                      NC1764.2
197400             MOVE   "A"        TO CORRECT-X                       NC1764.2
197500             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1764.2
197600             PERFORM FAIL                                         NC1764.2
197700             PERFORM PRINT-DETAIL.                                NC1764.2
197800     ADD     1 TO REC-CT.                                         NC1764.2
197900 ADD-TEST-F1-53-3.                                                NC1764.2
198000     MOVE   "ADD-TEST-F1-53-3" TO PAR-NAME.                       NC1764.2
198100     IF      SIZE-ERR3 = "B"                                      NC1764.2
198200             PERFORM PASS                                         NC1764.2
198300             PERFORM PRINT-DETAIL                                 NC1764.2
198400     ELSE                                                         NC1764.2
198500             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1764.2
198600                  TO RE-MARK                                      NC1764.2
198700             MOVE   "B"        TO CORRECT-X                       NC1764.2
198800             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1764.2
198900             PERFORM FAIL                                         NC1764.2
199000             PERFORM PRINT-DETAIL.                                NC1764.2
199100     ADD     1 TO REC-CT.                                         NC1764.2
199200 ADD-TEST-F1-53-4.                                                NC1764.2
199300     MOVE   "ADD-TEST-F1-53-4" TO PAR-NAME.                       NC1764.2
199400     IF      SIZE-ERR4 = "C"                                      NC1764.2
199500             PERFORM PASS                                         NC1764.2
199600             PERFORM PRINT-DETAIL                                 NC1764.2
199700     ELSE                                                         NC1764.2
199800             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1764.2
199900                  TO RE-MARK                                      NC1764.2
200000             MOVE   "C"        TO CORRECT-X                       NC1764.2
200100             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1764.2
200200             PERFORM FAIL                                         NC1764.2
200300             PERFORM PRINT-DETAIL.                                NC1764.2
200400     ADD     1 TO REC-CT.                                         NC1764.2
200500 ADD-TEST-F1-53-5.                                                NC1764.2
200600     MOVE   "ADD-TEST-F1-53-5" TO PAR-NAME.                       NC1764.2
200700     IF      WRK-DS-02V00 = -11                                   NC1764.2
200800             PERFORM PASS                                         NC1764.2
200900             PERFORM PRINT-DETAIL                                 NC1764.2
201000     ELSE                                                         NC1764.2
201100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1764.2
201200                  TO RE-MARK                                      NC1764.2
201300             MOVE    -11          TO CORRECT-N                    NC1764.2
201400             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1764.2
201500             PERFORM FAIL                                         NC1764.2
201600             PERFORM PRINT-DETAIL.                                NC1764.2
201700*                                                                 NC1764.2
201800 ADD-INIT-F1-54.                                                  NC1764.2
201900*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1764.2
202000     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1764.2
202100     MOVE   "ADD-TEST-F1-54"  TO PAR-NAME.                        NC1764.2
202200     MOVE    SPACE TO WRK-XN-00001.                               NC1764.2
202300     MOVE    SPACE TO SIZE-ERR2.                                  NC1764.2
202400     MOVE    SPACE TO SIZE-ERR3.                                  NC1764.2
202500     MOVE    SPACE TO SIZE-ERR4.                                  NC1764.2
202600     MOVE    ZERO TO WRK-DS-06V06.                                NC1764.2
202700     MOVE    1    TO REC-CT.                                      NC1764.2
202800 ADD-TEST-F1-54-0.                                                NC1764.2
202900     ADD     A12THREES-DS-06V06                                   NC1764.2
203000             333333                                               NC1764.2
203100             A06THREES-DS-03V03                                   NC1764.2
203200          TO WRK-DS-06V06 ROUNDED                                 NC1764.2
203300             ON SIZE ERROR                                        NC1764.2
203400             MOVE   "1" TO WRK-XN-00001                           NC1764.2
203500             MOVE   "A" TO SIZE-ERR2                              NC1764.2
203600             MOVE   "B" TO SIZE-ERR3                              NC1764.2
203700     END-ADD                                                      NC1764.2
203800     MOVE   "C" TO SIZE-ERR4.                                     NC1764.2
203900     GO TO   ADD-TEST-F1-54-1.                                    NC1764.2
204000 ADD-DELETE-F1-54.                                                NC1764.2
204100     PERFORM DE-LETE.                                             NC1764.2
204200     PERFORM PRINT-DETAIL.                                        NC1764.2
204300     GO TO   ADD-INIT-F1-55.                                      NC1764.2
204400 ADD-TEST-F1-54-1.                                                NC1764.2
204500     MOVE   "ADD-TEST-F1-54-1" TO PAR-NAME.                       NC1764.2
204600     IF      WRK-XN-00001 = SPACE                                 NC1764.2
204700             PERFORM PASS                                         NC1764.2
204800             PERFORM PRINT-DETAIL                                 NC1764.2
204900     ELSE                                                         NC1764.2
205000             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1764.2
205100                  TO RE-MARK                                      NC1764.2
205200             MOVE    SPACE        TO CORRECT-X                    NC1764.2
205300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
205400             PERFORM FAIL                                         NC1764.2
205500             PERFORM PRINT-DETAIL.                                NC1764.2
205600     ADD     1 TO REC-CT.                                         NC1764.2
205700 ADD-TEST-F1-54-2.                                                NC1764.2
205800     MOVE   "ADD-TEST-F1-54-2" TO PAR-NAME.                       NC1764.2
205900     IF      SIZE-ERR2 = SPACE                                    NC1764.2
206000             PERFORM PASS                                         NC1764.2
206100             PERFORM PRINT-DETAIL                                 NC1764.2
206200     ELSE                                                         NC1764.2
206300             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1764.2
206400                  TO RE-MARK                                      NC1764.2
206500             MOVE    SPACE     TO CORRECT-X                       NC1764.2
206600             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1764.2
206700             PERFORM FAIL                                         NC1764.2
206800             PERFORM PRINT-DETAIL.                                NC1764.2
206900     ADD     1 TO REC-CT.                                         NC1764.2
207000 ADD-TEST-F1-54-3.                                                NC1764.2
207100     MOVE   "ADD-TEST-F1-54-3" TO PAR-NAME.                       NC1764.2
207200     IF      SIZE-ERR3 = SPACE                                    NC1764.2
207300             PERFORM PASS                                         NC1764.2
207400             PERFORM PRINT-DETAIL                                 NC1764.2
207500     ELSE                                                         NC1764.2
207600             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
207700                  TO RE-MARK                                      NC1764.2
207800             MOVE    SPACE     TO CORRECT-X                       NC1764.2
207900             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1764.2
208000             PERFORM FAIL                                         NC1764.2
208100             PERFORM PRINT-DETAIL.                                NC1764.2
208200     ADD     1 TO REC-CT.                                         NC1764.2
208300 ADD-TEST-F1-54-4.                                                NC1764.2
208400     MOVE   "ADD-TEST-F1-54-4" TO PAR-NAME.                       NC1764.2
208500     IF      SIZE-ERR4 = "C"                                      NC1764.2
208600             PERFORM PASS                                         NC1764.2
208700             PERFORM PRINT-DETAIL                                 NC1764.2
208800     ELSE                                                         NC1764.2
208900             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1764.2
209000                  TO RE-MARK                                      NC1764.2
209100             MOVE   "C"        TO CORRECT-X                       NC1764.2
209200             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1764.2
209300             PERFORM FAIL                                         NC1764.2
209400             PERFORM PRINT-DETAIL                                 NC1764.2
209500             ADD     1 TO REC-CT.                                 NC1764.2
209600 ADD-TEST-F1-54-5.                                                NC1764.2
209700     MOVE   "ADD-TEST-F1-54-5" TO PAR-NAME.                       NC1764.2
209800     IF      WRK-DS-06V06 = 666999.666333                         NC1764.2
209900             PERFORM PASS                                         NC1764.2
210000             PERFORM PRINT-DETAIL                                 NC1764.2
210100     ELSE                                                         NC1764.2
210200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1764.2
210300                  TO RE-MARK                                      NC1764.2
210400             MOVE    666999.666333 TO CORRECT-N                   NC1764.2
210500             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1764.2
210600             PERFORM FAIL                                         NC1764.2
210700             PERFORM PRINT-DETAIL.                                NC1764.2
210800*                                                                 NC1764.2
210900 ADD-INIT-F1-55.                                                  NC1764.2
211000*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1764.2
211100     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1764.2
211200     MOVE   "ADD-TEST-F1-55"  TO PAR-NAME.                        NC1764.2
211300     MOVE   SPACE TO WRK-XN-00001.                                NC1764.2
211400     MOVE   SPACE TO SIZE-ERR2.                                   NC1764.2
211500     MOVE   SPACE TO SIZE-ERR3.                                   NC1764.2
211600     MOVE   SPACE TO SIZE-ERR4.                                   NC1764.2
211700     MOVE     -11 TO WRK-DS-02V00.                                NC1764.2
211800     MOVE       1 TO REC-CT.                                      NC1764.2
211900 ADD-TEST-F1-55-0.                                                NC1764.2
212000     ADD    -99 TO WRK-DS-02V00                                   NC1764.2
212100             NOT ON SIZE ERROR                                    NC1764.2
212200             MOVE   "1" TO WRK-XN-00001                           NC1764.2
212300             MOVE   "A" TO SIZE-ERR2                              NC1764.2
212400             MOVE   "B" TO SIZE-ERR3                              NC1764.2
212500     END-ADD                                                      NC1764.2
212600     MOVE   "C" TO SIZE-ERR4.                                     NC1764.2
212700     GO TO   ADD-TEST-F1-55-1.                                    NC1764.2
212800 ADD-DELETE-F1-55.                                                NC1764.2
212900     PERFORM DE-LETE.                                             NC1764.2
213000     PERFORM PRINT-DETAIL.                                        NC1764.2
213100     GO      TO ADD-INIT-F1-56.                                   NC1764.2
213200 ADD-TEST-F1-55-1.                                                NC1764.2
213300     MOVE   "ADD-TEST-F1-55-1" TO PAR-NAME.                       NC1764.2
213400     IF      WRK-XN-00001 = SPACE                                 NC1764.2
213500             PERFORM PASS                                         NC1764.2
213600             PERFORM PRINT-DETAIL                                 NC1764.2
213700     ELSE                                                         NC1764.2
213800             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
213900                  TO RE-MARK                                      NC1764.2
214000             MOVE    SPACE        TO CORRECT-X                    NC1764.2
214100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
214200             PERFORM FAIL                                         NC1764.2
214300             PERFORM PRINT-DETAIL.                                NC1764.2
214400     ADD     1 TO REC-CT.                                         NC1764.2
214500 ADD-TEST-F1-55-2.                                                NC1764.2
214600     MOVE   "ADD-TEST-F1-55-2" TO PAR-NAME.                       NC1764.2
214700     IF      SIZE-ERR2 = SPACE                                    NC1764.2
214800             PERFORM PASS                                         NC1764.2
214900             PERFORM PRINT-DETAIL                                 NC1764.2
215000     ELSE                                                         NC1764.2
215100             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
215200                  TO RE-MARK                                      NC1764.2
215300             MOVE    SPACE     TO CORRECT-X                       NC1764.2
215400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1764.2
215500             PERFORM FAIL                                         NC1764.2
215600             PERFORM PRINT-DETAIL.                                NC1764.2
215700     ADD     1 TO REC-CT.                                         NC1764.2
215800 ADD-TEST-F1-55-3.                                                NC1764.2
215900     MOVE   "ADD-TEST-F1-55-3" TO PAR-NAME.                       NC1764.2
216000     IF      SIZE-ERR3 = SPACE                                    NC1764.2
216100             PERFORM PASS                                         NC1764.2
216200             PERFORM PRINT-DETAIL                                 NC1764.2
216300     ELSE                                                         NC1764.2
216400             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1764.2
216500                  TO RE-MARK                                      NC1764.2
216600             MOVE    SPACE     TO CORRECT-X                       NC1764.2
216700             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1764.2
216800             PERFORM FAIL                                         NC1764.2
216900             PERFORM PRINT-DETAIL.                                NC1764.2
217000     ADD     1 TO REC-CT.                                         NC1764.2
217100 ADD-TEST-F1-55-4.                                                NC1764.2
217200     MOVE   "ADD-TEST-F1-55-4" TO PAR-NAME.                       NC1764.2
217300     IF      SIZE-ERR4 = "C"                                      NC1764.2
217400             PERFORM PASS                                         NC1764.2
217500             PERFORM PRINT-DETAIL                                 NC1764.2
217600     ELSE                                                         NC1764.2
217700             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1764.2
217800                  TO RE-MARK                                      NC1764.2
217900             MOVE   "C"        TO CORRECT-X                       NC1764.2
218000             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1764.2
218100             PERFORM FAIL                                         NC1764.2
218200             PERFORM PRINT-DETAIL.                                NC1764.2
218300     ADD     1 TO REC-CT.                                         NC1764.2
218400 ADD-TEST-F1-55-5.                                                NC1764.2
218500     MOVE   "ADD-TEST-F1-55-5" TO PAR-NAME.                       NC1764.2
218600     IF      WRK-DS-02V00 = -11                                   NC1764.2
218700             PERFORM PASS                                         NC1764.2
218800             PERFORM PRINT-DETAIL                                 NC1764.2
218900     ELSE                                                         NC1764.2
219000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1764.2
219100                  TO RE-MARK                                      NC1764.2
219200             MOVE    -11          TO CORRECT-N                    NC1764.2
219300             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1764.2
219400             PERFORM FAIL                                         NC1764.2
219500             PERFORM PRINT-DETAIL.                                NC1764.2
219600*                                                                 NC1764.2
219700 ADD-INIT-F1-56.                                                  NC1764.2
219800*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1764.2
219900     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1764.2
220000     MOVE   "ADD-TEST-F1-56"  TO PAR-NAME.                        NC1764.2
220100     MOVE    SPACE TO WRK-XN-00001.                               NC1764.2
220200     MOVE    SPACE TO SIZE-ERR2.                                  NC1764.2
220300     MOVE    SPACE TO SIZE-ERR3.                                  NC1764.2
220400     MOVE    SPACE TO SIZE-ERR4.                                  NC1764.2
220500     MOVE    ZERO  TO WRK-DS-06V06.                               NC1764.2
220600     MOVE    1     TO REC-CT.                                     NC1764.2
220700 ADD-TEST-F1-56-0.                                                NC1764.2
220800     ADD     A12THREES-DS-06V06                                   NC1764.2
220900             333333                                               NC1764.2
221000             A06THREES-DS-03V03                                   NC1764.2
221100          TO WRK-DS-06V06 ROUNDED                                 NC1764.2
221200         NOT ON SIZE ERROR                                        NC1764.2
221300             MOVE   "1" TO WRK-XN-00001                           NC1764.2
221400             MOVE   "A" TO SIZE-ERR2                              NC1764.2
221500             MOVE   "B" TO SIZE-ERR3                              NC1764.2
221600     END-ADD                                                      NC1764.2
221700     MOVE   "C" TO SIZE-ERR4.                                     NC1764.2
221800     GO TO   ADD-TEST-F1-56-1.                                    NC1764.2
221900 ADD-DELETE-F1-56.                                                NC1764.2
222000     PERFORM DE-LETE.                                             NC1764.2
222100     PERFORM PRINT-DETAIL.                                        NC1764.2
222200     GO TO   ADD-INIT-F1-57.                                      NC1764.2
222300 ADD-TEST-F1-56-1.                                                NC1764.2
222400     MOVE   "ADD-TEST-F1-56-1" TO PAR-NAME.                       NC1764.2
222500     IF      WRK-XN-00001 = "1"                                   NC1764.2
222600             PERFORM PASS                                         NC1764.2
222700             PERFORM PRINT-DETAIL                                 NC1764.2
222800     ELSE                                                         NC1764.2
222900             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1764.2
223000                  TO RE-MARK                                      NC1764.2
223100             MOVE   "1"           TO CORRECT-X                    NC1764.2
223200             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
223300             PERFORM FAIL                                         NC1764.2
223400             PERFORM PRINT-DETAIL.                                NC1764.2
223500             ADD     1 TO REC-CT.                                 NC1764.2
223600 ADD-TEST-F1-56-2.                                                NC1764.2
223700     MOVE   "ADD-TEST-F1-56-2" TO PAR-NAME.                       NC1764.2
223800     IF      SIZE-ERR2 = "A"                                      NC1764.2
223900             PERFORM PASS                                         NC1764.2
224000             PERFORM PRINT-DETAIL                                 NC1764.2
224100     ELSE                                                         NC1764.2
224200             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1764.2
224300                  TO RE-MARK                                      NC1764.2
224400             MOVE   "A"        TO CORRECT-X                       NC1764.2
224500             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1764.2
224600             PERFORM FAIL                                         NC1764.2
224700             PERFORM PRINT-DETAIL.                                NC1764.2
224800     ADD     1 TO REC-CT.                                         NC1764.2
224900 ADD-TEST-F1-56-3.                                                NC1764.2
225000     MOVE   "ADD-TEST-F1-56-3" TO PAR-NAME.                       NC1764.2
225100     IF      SIZE-ERR3 = "B"                                      NC1764.2
225200             PERFORM PASS                                         NC1764.2
225300             PERFORM PRINT-DETAIL                                 NC1764.2
225400     ELSE                                                         NC1764.2
225500             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1764.2
225600                  TO RE-MARK                                      NC1764.2
225700             MOVE   "B"        TO CORRECT-X                       NC1764.2
225800             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1764.2
225900             PERFORM FAIL                                         NC1764.2
226000             PERFORM PRINT-DETAIL.                                NC1764.2
226100     ADD     1 TO REC-CT.                                         NC1764.2
226200 ADD-TEST-F1-56-4.                                                NC1764.2
226300     MOVE   "ADD-TEST-F1-56-4" TO PAR-NAME.                       NC1764.2
226400     IF      SIZE-ERR4 = "C"                                      NC1764.2
226500             PERFORM PASS                                         NC1764.2
226600             PERFORM PRINT-DETAIL                                 NC1764.2
226700     ELSE                                                         NC1764.2
226800             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1764.2
226900                  TO RE-MARK                                      NC1764.2
227000             MOVE   "C"        TO CORRECT-X                       NC1764.2
227100             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1764.2
227200             PERFORM FAIL                                         NC1764.2
227300             PERFORM PRINT-DETAIL.                                NC1764.2
227400     ADD     1 TO REC-CT.                                         NC1764.2
227500 ADD-TEST-F1-56-5.                                                NC1764.2
227600     MOVE   "ADD-TEST-F1-56-5" TO PAR-NAME.                       NC1764.2
227700     IF      WRK-DS-06V06 = 666999.666333                         NC1764.2
227800             PERFORM PASS                                         NC1764.2
227900             PERFORM PRINT-DETAIL                                 NC1764.2
228000     ELSE                                                         NC1764.2
228100             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1764.2
228200                  TO RE-MARK                                      NC1764.2
228300             MOVE    666999.666333 TO CORRECT-N                   NC1764.2
228400             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1764.2
228500             PERFORM FAIL                                         NC1764.2
228600             PERFORM PRINT-DETAIL.                                NC1764.2
228700*                                                                 NC1764.2
228800 ADD-INIT-F1-57.                                                  NC1764.2
228900*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1764.2
229000     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1764.2
229100     MOVE   "ADD-TEST-F1-57"  TO PAR-NAME.                        NC1764.2
229200     MOVE   "0"   TO WRK-XN-00001.                                NC1764.2
229300     MOVE   "0"   TO SIZE-ERR2.                                   NC1764.2
229400     MOVE   "0"   TO SIZE-ERR3.                                   NC1764.2
229500     MOVE   "0"   TO SIZE-ERR4.                                   NC1764.2
229600     MOVE   -11   TO WRK-DS-02V00.                                NC1764.2
229700     MOVE    1    TO REC-CT.                                      NC1764.2
229800 ADD-TEST-F1-57-0.                                                NC1764.2
229900     ADD    -99 TO WRK-DS-02V00                                   NC1764.2
230000          ON SIZE ERROR                                           NC1764.2
230100             MOVE   SPACE TO WRK-XN-00001                         NC1764.2
230200      NOT ON SIZE ERROR                                           NC1764.2
230300             MOVE   "1" TO WRK-XN-00001                           NC1764.2
230400     END-ADD                                                      NC1764.2
230500     MOVE   "C" TO SIZE-ERR4.                                     NC1764.2
230600     GO TO   ADD-TEST-F1-57-1.                                    NC1764.2
230700 ADD-DELETE-F1-57.                                                NC1764.2
230800     PERFORM DE-LETE.                                             NC1764.2
230900     PERFORM PRINT-DETAIL.                                        NC1764.2
231000     GO TO   ADD-INIT-F1-58.                                      NC1764.2
231100 ADD-TEST-F1-57-1.                                                NC1764.2
231200     MOVE   "ADD-TEST-F1-57-1" TO PAR-NAME.                       NC1764.2
231300     IF      WRK-XN-00001 = SPACE                                 NC1764.2
231400             PERFORM PASS                                         NC1764.2
231500             PERFORM PRINT-DETAIL                                 NC1764.2
231600     ELSE                                                         NC1764.2
231700             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1764.2
231800                  TO RE-MARK                                      NC1764.2
231900             MOVE    SPACE        TO CORRECT-X                    NC1764.2
232000             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
232100             PERFORM FAIL                                         NC1764.2
232200             PERFORM PRINT-DETAIL.                                NC1764.2
232300     ADD     1 TO REC-CT.                                         NC1764.2
232400 ADD-TEST-F1-57-2.                                                NC1764.2
232500     MOVE   "ADD-TEST-F1-57-2" TO PAR-NAME.                       NC1764.2
232600     IF      SIZE-ERR4 = "C"                                      NC1764.2
232700             PERFORM PASS                                         NC1764.2
232800             PERFORM PRINT-DETAIL                                 NC1764.2
232900     ELSE                                                         NC1764.2
233000             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1764.2
233100                  TO RE-MARK                                      NC1764.2
233200             MOVE   "C"        TO CORRECT-X                       NC1764.2
233300             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1764.2
233400             PERFORM FAIL                                         NC1764.2
233500             PERFORM PRINT-DETAIL.                                NC1764.2
233600     ADD     1 TO REC-CT.                                         NC1764.2
233700 ADD-TEST-F1-57-3.                                                NC1764.2
233800     MOVE   "ADD-TEST-F1-57-3" TO PAR-NAME.                       NC1764.2
233900     IF      WRK-DS-02V00 = -11                                   NC1764.2
234000             PERFORM PASS                                         NC1764.2
234100             PERFORM PRINT-DETAIL                                 NC1764.2
234200     ELSE                                                         NC1764.2
234300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1764.2
234400                  TO RE-MARK                                      NC1764.2
234500             MOVE    -11          TO CORRECT-N                    NC1764.2
234600             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1764.2
234700             PERFORM FAIL                                         NC1764.2
234800             PERFORM PRINT-DETAIL.                                NC1764.2
234900*                                                                 NC1764.2
235000 ADD-INIT-F1-58.                                                  NC1764.2
235100*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1764.2
235200     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1764.2
235300     MOVE   "ADD-TEST-F1-58"  TO PAR-NAME.                        NC1764.2
235400     MOVE    SPACE TO WRK-XN-00001.                               NC1764.2
235500     MOVE    SPACE TO SIZE-ERR2.                                  NC1764.2
235600     MOVE    SPACE TO SIZE-ERR3.                                  NC1764.2
235700     MOVE    SPACE TO SIZE-ERR4.                                  NC1764.2
235800     MOVE    ZERO  TO WRK-DS-06V06.                               NC1764.2
235900     MOVE    1     TO REC-CT.                                     NC1764.2
236000 ADD-TEST-F1-58-0.                                                NC1764.2
236100     ADD     A12THREES-DS-06V06                                   NC1764.2
236200             333333                                               NC1764.2
236300             A06THREES-DS-03V03                                   NC1764.2
236400          TO WRK-DS-06V06 ROUNDED                                 NC1764.2
236500             ON SIZE ERROR                                        NC1764.2
236600             MOVE   "X" TO WRK-XN-00001                           NC1764.2
236700         NOT ON SIZE ERROR                                        NC1764.2
236800             MOVE   "1" TO WRK-XN-00001                           NC1764.2
236900     END-ADD                                                      NC1764.2
237000     MOVE   "C" TO SIZE-ERR4.                                     NC1764.2
237100     GO TO   ADD-TEST-F1-58-1.                                    NC1764.2
237200 ADD-DELETE-F1-58.                                                NC1764.2
237300     PERFORM DE-LETE.                                             NC1764.2
237400     PERFORM PRINT-DETAIL.                                        NC1764.2
237500     GO TO   CCVS-EXIT.                                           NC1764.2
237600 ADD-TEST-F1-58-1.                                                NC1764.2
237700     MOVE   "ADD-TEST-F1-58-1" TO PAR-NAME.                       NC1764.2
237800     IF      WRK-XN-00001 = "1"                                   NC1764.2
237900             PERFORM PASS                                         NC1764.2
238000             PERFORM PRINT-DETAIL                                 NC1764.2
238100     ELSE                                                         NC1764.2
238200             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1764.2
238300                  TO RE-MARK                                      NC1764.2
238400             MOVE   "1"           TO CORRECT-X                    NC1764.2
238500             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1764.2
238600             PERFORM FAIL                                         NC1764.2
238700             PERFORM PRINT-DETAIL.                                NC1764.2
238800     ADD     1 TO REC-CT.                                         NC1764.2
238900 ADD-TEST-F1-58-2.                                                NC1764.2
239000     MOVE   "ADD-TEST-F1-58-2" TO PAR-NAME.                       NC1764.2
239100     IF      SIZE-ERR4 = "C"                                      NC1764.2
239200             PERFORM PASS                                         NC1764.2
239300             PERFORM PRINT-DETAIL                                 NC1764.2
239400     ELSE                                                         NC1764.2
239500             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1764.2
239600                  TO RE-MARK                                      NC1764.2
239700             MOVE   "C"        TO CORRECT-X                       NC1764.2
239800             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1764.2
239900             PERFORM FAIL                                         NC1764.2
240000             PERFORM PRINT-DETAIL.                                NC1764.2
240100     ADD     1 TO REC-CT.                                         NC1764.2
240200 ADD-TEST-F1-58-3.                                                NC1764.2
240300     MOVE   "ADD-TEST-F1-58-3" TO PAR-NAME.                       NC1764.2
240400     IF      WRK-DS-06V06 = 666999.666333                         NC1764.2
240500             PERFORM PASS                                         NC1764.2
240600             PERFORM PRINT-DETAIL                                 NC1764.2
240700     ELSE                                                         NC1764.2
240800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1764.2
240900                  TO RE-MARK                                      NC1764.2
241000             MOVE    666999.666333 TO CORRECT-N                   NC1764.2
241100             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1764.2
241200             PERFORM FAIL                                         NC1764.2
241300             PERFORM PRINT-DETAIL.                                NC1764.2
241400*                                                                 NC1764.2
241500 CCVS-EXIT SECTION.                                               NC1764.2
241600 CCVS-999999.                                                     NC1764.2
241700     GO TO CLOSE-FILES.                                           NC1764.2
      *END-OF,NC176A                                                                  
