      *HEADER,COBOL,NC106A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1064.2
000200 PROGRAM-ID.                                                      NC1064.2
000300     NC106A.                                                      NC1064.2
000400****************************************************************  NC1064.2
000500*                                                              *  NC1064.2
000600*    VALIDATION FOR:-                                          *  NC1064.2
000700*                                                              *  NC1064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1064.2
000900*                                                              *  NC1064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1064.2
001100*                                                              *  NC1064.2
001200****************************************************************  NC1064.2
001300*                                                              *  NC1064.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1064.2
001500*                                                              *  NC1064.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1064.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1064.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1064.2
001900*                                                              *  NC1064.2
002000****************************************************************  NC1064.2
002100*                                                                 NC1064.2
002200*    PROGRAM NC106A TESTS FORMAT 1  OF THE SUBTRACT               NC1064.2
002300*    STATEMENT.  VARIOUS COMBINATINS OF DATA-ITEMS AND ALL        NC1064.2
002400*    OPTIONAL PHRASES ARE TESTED.                                 NC1064.2
002500*                                                                 NC1064.2
002600                                                                  NC1064.2
002700 ENVIRONMENT DIVISION.                                            NC1064.2
002800 CONFIGURATION SECTION.                                           NC1064.2
002900 SOURCE-COMPUTER.                                                 NC1064.2
003000     XXXXX082.                                                    NC1064.2
003100 OBJECT-COMPUTER.                                                 NC1064.2
003200     XXXXX083.                                                    NC1064.2
003300 INPUT-OUTPUT SECTION.                                            NC1064.2
003400 FILE-CONTROL.                                                    NC1064.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1064.2
003600     XXXXX055.                                                    NC1064.2
003700 DATA DIVISION.                                                   NC1064.2
003800 FILE SECTION.                                                    NC1064.2
003900 FD  PRINT-FILE.                                                  NC1064.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1064.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1064.2
004200 WORKING-STORAGE SECTION.                                         NC1064.2
004300 01  WRK-NE-X-1     PIC  9(16).99.                                NC1064.2
004400 01  WRK-NE-X-2     PIC -9(16).99.                                NC1064.2
004500 01  WRK-XN-00001 PIC X.                                          NC1064.2
004600 01  WRK-XN-18-1 PIC X(18).                                       NC1064.2
004700 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1064.2
004800 01  WRK-DU-X-18V0-1; REDEFINES WRK-XN-18-1 PIC 9(18).            NC1064.2
004900 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1064.2
005000 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1064.2
005100 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1064.2
005200 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1064.2
005300 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1064.2
005400 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1064.2
005500 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1064.2
005600 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1064.2
005700 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1064.2
005800 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1064.2
005900 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1064.2
006000 01  WRK-DU-2V0-1 PIC 99.                                         NC1064.2
006100 01  WRK-DU-2V0-2 PIC 99.                                         NC1064.2
006200 01  WRK-DU-2V0-3 PIC 99.                                         NC1064.2
006300 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1064.2
006400 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1064.2
006500 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1064.2
006600 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1064.2
006700 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1064.2
006800 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1064.2
006900 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1064.2
007000 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1064.2
007100 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1064.2
007200 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1064.2
007300 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1064.2
007400 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1064.2
007500 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1064.2
007600 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1064.2
007700 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1064.2
007800 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1064.2
007900 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1064.2
008000 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1064.2
008100 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1064.2
008200 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1064.2
008300 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1064.2
008400 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1064.2
008500 01  42-DATANAMES.                                                NC1064.2
008600     02  DNAME1   PICTURE 9      VALUE 1        COMPUTATIONAL.    NC1064.2
008700     02  DNAME2   PICTURE 99      VALUE 1  COMPUTATIONAL.         NC1064.2
008800     02  DNAME3   PICTURE 999     VALUE 1  COMPUTATIONAL.         NC1064.2
008900     02  DNAME4   PICTURE 9(4)    VALUE 1  COMPUTATIONAL.         NC1064.2
009000     02  DNAME5   PICTURE 9(5)    VALUE 1  COMPUTATIONAL.         NC1064.2
009100     02  DNAME6   PICTURE 9(6)    VALUE 1  COMPUTATIONAL.         NC1064.2
009200     02  DNAME7   PICTURE 9(7)    VALUE 1  COMPUTATIONAL.         NC1064.2
009300     02  DNAME8   PICTURE 9(8)    VALUE 1  COMPUTATIONAL.         NC1064.2
009400     02  DNAME9   PICTURE 9(9)    VALUE 1  COMPUTATIONAL.         NC1064.2
009500     02  DNAME10  PICTURE 9(10)   VALUE 1.                        NC1064.2
009600     02  DNAME11  PICTURE 9(11)   VALUE 1.                        NC1064.2
009700     02  DNAME12  PICTURE 9(12)   VALUE 1.                        NC1064.2
009800     02  DNAME13  PICTURE 9(13)   VALUE 1.                        NC1064.2
009900     02  DNAME14  PICTURE 9(14)   VALUE 1.                        NC1064.2
010000     02  DNAME15  PICTURE 9(15)   VALUE 1.                        NC1064.2
010100     02  DNAME16  PICTURE 9(16)   VALUE 1.                        NC1064.2
010200     02  DNAME17  PICTURE 9(17)   VALUE 1.                        NC1064.2
010300     02  DNAME18  PICTURE 9(18)   VALUE 1.                        NC1064.2
010400     02  DNAME19  PICTURE 9       VALUE 1.                        NC1064.2
010500     02  DNAME20  PICTURE 99      VALUE 1.                        NC1064.2
010600     02  DNAME21  PICTURE 999     VALUE 1.                        NC1064.2
010700     02  DNAME22  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
010800     02  DNAME23  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
010900     02  DNAME24  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
011000     02  DNAME25  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
011100     02  DNAME26  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
011200     02  DNAME27  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
011300     02  DNAME28  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
011400     02  DNAME29  PICTURE 9(18)  VALUE ZERO.                      NC1064.2
011500     02  DNAME30   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
011600     02  DNAME31   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
011700     02  DNAME32   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
011800     02  DNAME33   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
011900     02  DNAME34   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012000     02  DNAME35   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012100     02  DNAME36   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012200     02  DNAME37   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012300     02  DNAME38   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012400     02  DNAME39   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012500     02  DNAME40   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012600     02  DNAME41   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012700     02  DNAME42   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1064.2
012800 77  SIZE-ERR                     PICTURE X  VALUE SPACE.         NC1064.2
012900 77  SIZE-ERR2                    PICTURE X  VALUE SPACE.         NC1064.2
013000 77  SIZE-ERR3                    PICTURE X  VALUE SPACE.         NC1064.2
013100 77  SIZE-ERR4                    PICTURE X  VALUE SPACE.         NC1064.2
013200 77  A18TWOS-DS-18V00             PICTURE S9(18)                  NC1064.2
013300                                  VALUE 222222222222222222.       NC1064.2
013400 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC1064.2
013500                                  VALUE 111111111111111111.       NC1064.2
013600 77  WRK-DS-10V00                 PICTURE S9(10).                 NC1064.2
013700 77  A10ONES-DS-10V00             PICTURE S9(10)                  NC1064.2
013800                                  VALUE 1111111111.               NC1064.2
013900 77  A05ONES-DS-05V00             PICTURE S9(5)                   NC1064.2
014000                                  VALUE 11111.                    NC1064.2
014100 77  A02ONES-DS-02V00             PICTURE S99                     NC1064.2
014200                                  VALUE 11.                       NC1064.2
014300 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1064.2
014400 77  WRK-DS-18V00 REDEFINES WRK-DS-09V09                          NC1064.2
014500                                  PICTURE S9(18).                 NC1064.2
014600 77  A06THREES-DS-03V03           PICTURE S999V999                NC1064.2
014700                                  VALUE 333.333.                  NC1064.2
014800 77  A12THREES-DS-06V06           PICTURE S9(6)V9(6)              NC1064.2
014900                                  VALUE 333333.333333.            NC1064.2
015000 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC1064.2
015100 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC1064.2
015200                                  PICTURE S9(12).                 NC1064.2
015300 77  A05ONES-DS-00V05             PICTURE SV9(5)                  NC1064.2
015400                                  VALUE .11111.                   NC1064.2
015500 77  WRK-DS-05V00                 PICTURE S9(5).                  NC1064.2
015600 77  WRK-DS-02V00                 PICTURE S99.                    NC1064.2
015700 77  A12ONES-DS-12V00             PICTURE S9(12)                  NC1064.2
015800                                  VALUE 111111111111.             NC1064.2
015900 77  WRK-DS-03V10                 PICTURE S999V9(10).             NC1064.2
016000 77  WRK-DS-13V00-S REDEFINES WRK-DS-03V10                        NC1064.2
016100                                  PICTURE S9(13).                 NC1064.2
016200 77  A99-DS-02V00                 PICTURE S99                     NC1064.2
016300                                  VALUE 99.                       NC1064.2
016400 77  A03ONES-DS-02V01             PICTURE S99V9                   NC1064.2
016500                                  VALUE 11.1.                     NC1064.2
016600 77  A06ONES-DS-03V03             PICTURE S999V999                NC1064.2
016700                                  VALUE 111.111.                  NC1064.2
016800 77  A08TWOS-DS-02V06             PICTURE S99V9(6)                NC1064.2
016900                                  VALUE 22.222222.                NC1064.2
017000 77  A01ONE-DS-P0801              PICTURE SP(8)9                  NC1064.2
017100                                  VALUE .000000001.               NC1064.2
017200 77  WRK-CS-18V00                 PICTURE S9(18) COMPUTATIONAL.   NC1064.2
017300 77  A18ONES-CS-18V00             PICTURE S9(18) COMPUTATIONAL    NC1064.2
017400                                  VALUE 111111111111111111.       NC1064.2
017500 77  WRK-CS-02V02                 PICTURE S99V99 COMPUTATIONAL.   NC1064.2
017600 77  A99-CS-02V00                 PICTURE S99 COMPUTATIONAL       NC1064.2
017700                                  VALUE 99.                       NC1064.2
017800 77  WRK-DS-0201P                 PICTURE S99P.                   NC1064.2
017900 77  WRK-DS-06V00                 PICTURE S9(6).                  NC1064.2
018000 77  AZERO-DS-05V05               PICTURE S9(5)V9(5)              NC1064.2
018100                                  VALUE ZERO.                     NC1064.2
018200 77  NDATA-DS-09V09               PICTURE S9(9)V9(9)              NC1064.2
018300                                  VALUE +012345678.876543210.     NC1064.2
018400 77  XDATA-XN-00018               PICTURE X(18)                   NC1064.2
018500                                  VALUE "00ABCDEFGHI  4321 ".     NC1064.2
018600 77  WRK-XN-00018                 PICTURE X(18).                  NC1064.2
018700 77  ADD-12    PICTURE PP9        VALUE .001.                     NC1064.2
018800 77  ADD-13    PICTURE 9PP        VALUE 100.                      NC1064.2
018900 77  ADD-14    PICTURE 999V999.                                   NC1064.2
019000 77  WRK-CU-18V00      PICTURE 9(18) VALUE ZERO COMPUTATIONAL.    NC1064.2
019100 77  A18EIGHTS-CS-18V00 PICTURE S9(18) VALUE -888888888888888888  NC1064.2
019200             COMPUTATIONAL.                                       NC1064.2
019300 77  A14TWOS-CS-18V00  PICTURE S9(18) VALUE -000022222222222222   NC1064.2
019400             COMPUTATIONAL.                                       NC1064.2
019500 77  A12THREES-CU-18V00 PICTURE 9(18) VALUE 000000333333333333    NC1064.2
019600             COMPUTATIONAL.                                       NC1064.2
019700 77  A16FOURS-CS-18V00 PICTURE S9(18) VALUE 004444444444444444    NC1064.2
019800             COMPUTATIONAL.                                       NC1064.2
019900 77  A18FIVES-CS-18V00 PICTURE S9(18) VALUE -555555555555555555   NC1064.2
020000             COMPUTATIONAL.                                       NC1064.2
020100 77  A18SIXES-CS-18V00 PICTURE S9(18) VALUE 666666666666666666    NC1064.2
020200             COMPUTATIONAL.                                       NC1064.2
020300 77  A12SEVENS-CU-18V00 PICTURE 9(18) VALUE 000000777777777777    NC1064.2
020400             COMPUTATIONAL.                                       NC1064.2
020500 77  WRK-DU-18V00      PICTURE 9(18) VALUE ZERO.                  NC1064.2
020600 77  A18THREES-CS-18V00 PICTURE S9(18) VALUE -333333333333333333  NC1064.2
020700             COMPUTATIONAL.                                       NC1064.2
020800 77  WRK-CS-03V00 PICTURE S999 COMPUTATIONAL SYNCHRONIZED RIGHT.  NC1064.2
020900 01  SUBTRACT-DATA.                                               NC1064.2
021000     02  SUBTR-1    PICTURE 9         VALUE 1.                    NC1064.2
021100     02  SUBTR-2    PICTURE S99       VALUE 99.                   NC1064.2
021200     02  SUBTR-3    PICTURE S9V99     VALUE -1.                   NC1064.2
021300     02  SUBTR-4    PICTURE SPP9      VALUE .001.                 NC1064.2
021400     02  SUBTR-5    PICTURE S9PP      VALUE 100.                  NC1064.2
021500     02  SUBTR-6    PICTURE 9  VALUE 1.                           NC1064.2
021600     02  SUBTR-7    PICTURE S99  VALUE 99.                        NC1064.2
021700     02  SUBTR-8    PICTURE S9V99  VALUE -9.99.                   NC1064.2
021800     02  SUBTR-10    PICTURE S999  VALUE 100.                     NC1064.2
021900     02  SUBTR-11   PICTURE S999V999.                             NC1064.2
022000 01  N-3                                PICTURE IS 99999.         NC1064.2
022100 01  N-4                                PICTURE IS 9(5)           NC1064.2
022200     VALUE IS 52800.                                              NC1064.2
022300 01  N-5                                PICTURE IS S9(9)V99       NC1064.2
022400     VALUE IS 000000001.00.                                       NC1064.2
022500 01  N-7                                PICTURE IS S9(7)V9(4)     NC1064.2
022600     VALUE IS 0000001.0000.                                       NC1064.2
022700 01  X-8 REDEFINES N-7                  PICTURE IS X(11).         NC1064.2
022800 01  N-10                               PICTURE IS S99999V        NC1064.2
022900     VALUE IS -00001.                                             NC1064.2
023000 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1064.2
023100 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1064.2
023200 01  N-13                               PICTURE IS 9(5)           NC1064.2
023300     VALUE IS 99999.                                              NC1064.2
023400 01  N-14                               PICTURE IS 9 VALUE IS 1.  NC1064.2
023500 01  N-15                               PICTURE IS 9(16).         NC1064.2
023600 01  N-16                               PICTURE IS S999999V99     NC1064.2
023700     VALUE IS 5.90.                                               NC1064.2
023800 01  N-17                               PICTURE IS S9(3)V99       NC1064.2
023900     VALUE IS +3.6.                                               NC1064.2
024000 01  N-18                               PICTURE IS S9(10)         NC1064.2
024100     VALUE IS -5.                                                 NC1064.2
024200 01  N-19                               PICTURE IS $9.00.         NC1064.2
024300 01  N-20                               PICTURE IS S9(9)          NC1064.2
024400     VALUE IS -999999999.                                         NC1064.2
024500 01  N-21                               PICTURE IS 9 VALUE IS 5.  NC1064.2
024600 01  N-22                               PICTURE IS 999V99         NC1064.2
024700     VALUE IS 005.55.                                             NC1064.2
024800 01  N-23                               PICTURE IS $$$.99CR.      NC1064.2
024900 01  N-25                               PICTURE IS 9 VALUE IS 1.  NC1064.2
025000 01  N-26               PICTURE 9(5).                             NC1064.2
025100 01  N-27                               PICTURE IS 9999V9         NC1064.2
025200     VALUE IS 9999.9.                                             NC1064.2
025300 01  N-28                               PICTURE IS $9999.00.      NC1064.2
025400 01  N-40                               PICTURE IS 9(7)           NC1064.2
025500     VALUE IS 7777777.                                            NC1064.2
025600 01  N-41                               PICTURE IS 9(7)           NC1064.2
025700     VALUE IS 1111111.                                            NC1064.2
025800 01  N-42                               PICTURE IS 9(3)P(4).      NC1064.2
025900 01  TRUNC-DATA.                                                  NC1064.2
026000     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1064.2
026100     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1064.2
026200     02 N-45  PICTURE S9.                                         NC1064.2
026300 01  MINUS-NAMES.                                                 NC1064.2
026400     02  MINUS-NAME1 PICTURE S9(18) VALUE -999999999999999999.    NC1064.2
026500     02  MINUS-NAME2 PICTURE S9(18) VALUE -999999999999999999.    NC1064.2
026600     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC1064.2
026700     02  PLUS-NAME1  PICTURE S9(18) VALUE +999999999999999999.    NC1064.2
026800     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC1064.2
026900     02  MINUS-NAME3 PICTURE SV9(18) VALUE -.999999999999999999.  NC1064.2
027000     02  MINUS-NAME4 PICTURE SV9(18) VALUE -.999999999999999999.  NC1064.2
027100     02  EVEN-NAME2  PICTURE SV9(18)  VALUE +.1.                  NC1064.2
027200     02  PLUS-NAME3  PICTURE SV9(18) VALUE +.999999999999999999.  NC1064.2
027300     02  PLUS-NAME4  PICTURE SV9(18) VALUE +.999999999999999999.  NC1064.2
027400     02  WHOLE-FIELD   PICTURE S9(18).                            NC1064.2
027500     02  DECMAL-FIELD  PICTURE SV9(18).                           NC1064.2
027600 01  TEST-RESULTS.                                                NC1064.2
027700     02 FILLER                   PIC X      VALUE SPACE.          NC1064.2
027800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1064.2
027900     02 FILLER                   PIC X      VALUE SPACE.          NC1064.2
028000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1064.2
028100     02 FILLER                   PIC X      VALUE SPACE.          NC1064.2
028200     02  PAR-NAME.                                                NC1064.2
028300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1064.2
028400       03  PARDOT-X              PIC X      VALUE SPACE.          NC1064.2
028500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1064.2
028600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1064.2
028700     02 RE-MARK                  PIC X(61).                       NC1064.2
028800 01  TEST-COMPUTED.                                               NC1064.2
028900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1064.2
029000     02 FILLER                   PIC X(17)  VALUE                 NC1064.2
029100            "       COMPUTED=".                                   NC1064.2
029200     02 COMPUTED-X.                                               NC1064.2
029300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1064.2
029400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1064.2
029500                                 PIC -9(9).9(9).                  NC1064.2
029600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1064.2
029700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1064.2
029800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1064.2
029900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1064.2
030000         04 COMPUTED-18V0                    PIC -9(18).          NC1064.2
030100         04 FILLER                           PIC X.               NC1064.2
030200     03 FILLER PIC X(50) VALUE SPACE.                             NC1064.2
030300 01  TEST-CORRECT.                                                NC1064.2
030400     02 FILLER PIC X(30) VALUE SPACE.                             NC1064.2
030500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1064.2
030600     02 CORRECT-X.                                                NC1064.2
030700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1064.2
030800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1064.2
030900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1064.2
031000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1064.2
031100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1064.2
031200     03      CR-18V0 REDEFINES CORRECT-A.                         NC1064.2
031300         04 CORRECT-18V0                     PIC -9(18).          NC1064.2
031400         04 FILLER                           PIC X.               NC1064.2
031500     03 FILLER PIC X(2) VALUE SPACE.                              NC1064.2
031600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1064.2
031700 01  CCVS-C-1.                                                    NC1064.2
031800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1064.2
031900-    "SS  PARAGRAPH-NAME                                          NC1064.2
032000-    "       REMARKS".                                            NC1064.2
032100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1064.2
032200 01  CCVS-C-2.                                                    NC1064.2
032300     02 FILLER                     PIC X        VALUE SPACE.      NC1064.2
032400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1064.2
032500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1064.2
032600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1064.2
032700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1064.2
032800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1064.2
032900 01  REC-CT                        PIC 99       VALUE ZERO.       NC1064.2
033000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1064.2
033100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1064.2
033200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1064.2
033300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1064.2
033400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1064.2
033500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1064.2
033600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1064.2
033700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1064.2
033800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1064.2
033900 01  CCVS-H-1.                                                    NC1064.2
034000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1064.2
034100     02  FILLER                    PIC X(42)    VALUE             NC1064.2
034200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1064.2
034300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1064.2
034400 01  CCVS-H-2A.                                                   NC1064.2
034500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1064.2
034600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1064.2
034700   02  FILLER                        PIC XXXX   VALUE             NC1064.2
034800     "4.2 ".                                                      NC1064.2
034900   02  FILLER                        PIC X(28)  VALUE             NC1064.2
035000            " COPY - NOT FOR DISTRIBUTION".                       NC1064.2
035100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1064.2
035200                                                                  NC1064.2
035300 01  CCVS-H-2B.                                                   NC1064.2
035400   02  FILLER                        PIC X(15)  VALUE             NC1064.2
035500            "TEST RESULT OF ".                                    NC1064.2
035600   02  TEST-ID                       PIC X(9).                    NC1064.2
035700   02  FILLER                        PIC X(4)   VALUE             NC1064.2
035800            " IN ".                                               NC1064.2
035900   02  FILLER                        PIC X(12)  VALUE             NC1064.2
036000     " HIGH       ".                                              NC1064.2
036100   02  FILLER                        PIC X(22)  VALUE             NC1064.2
036200            " LEVEL VALIDATION FOR ".                             NC1064.2
036300   02  FILLER                        PIC X(58)  VALUE             NC1064.2
036400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1064.2
036500 01  CCVS-H-3.                                                    NC1064.2
036600     02  FILLER                      PIC X(34)  VALUE             NC1064.2
036700            " FOR OFFICIAL USE ONLY    ".                         NC1064.2
036800     02  FILLER                      PIC X(58)  VALUE             NC1064.2
036900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1064.2
037000     02  FILLER                      PIC X(28)  VALUE             NC1064.2
037100            "  COPYRIGHT   1985 ".                                NC1064.2
037200 01  CCVS-E-1.                                                    NC1064.2
037300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1064.2
037400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1064.2
037500     02 ID-AGAIN                     PIC X(9).                    NC1064.2
037600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1064.2
037700 01  CCVS-E-2.                                                    NC1064.2
037800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1064.2
037900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1064.2
038000     02 CCVS-E-2-2.                                               NC1064.2
038100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1064.2
038200         03 FILLER                   PIC X      VALUE SPACE.      NC1064.2
038300         03 ENDER-DESC               PIC X(44)  VALUE             NC1064.2
038400            "ERRORS ENCOUNTERED".                                 NC1064.2
038500 01  CCVS-E-3.                                                    NC1064.2
038600     02  FILLER                      PIC X(22)  VALUE             NC1064.2
038700            " FOR OFFICIAL USE ONLY".                             NC1064.2
038800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1064.2
038900     02  FILLER                      PIC X(58)  VALUE             NC1064.2
039000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1064.2
039100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1064.2
039200     02 FILLER                       PIC X(15)  VALUE             NC1064.2
039300             " COPYRIGHT 1985".                                   NC1064.2
039400 01  CCVS-E-4.                                                    NC1064.2
039500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1064.2
039600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1064.2
039700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1064.2
039800     02 FILLER                       PIC X(40)  VALUE             NC1064.2
039900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1064.2
040000 01  XXINFO.                                                      NC1064.2
040100     02 FILLER                       PIC X(19)  VALUE             NC1064.2
040200            "*** INFORMATION ***".                                NC1064.2
040300     02 INFO-TEXT.                                                NC1064.2
040400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1064.2
040500       04 XXCOMPUTED                 PIC X(20).                   NC1064.2
040600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1064.2
040700       04 XXCORRECT                  PIC X(20).                   NC1064.2
040800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1064.2
040900 01  HYPHEN-LINE.                                                 NC1064.2
041000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1064.2
041100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1064.2
041200-    "*****************************************".                 NC1064.2
041300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1064.2
041400-    "******************************".                            NC1064.2
041500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1064.2
041600     "NC106A".                                                    NC1064.2
041700 PROCEDURE DIVISION.                                              NC1064.2
041800 CCVS1 SECTION.                                                   NC1064.2
041900 OPEN-FILES.                                                      NC1064.2
042000     OPEN     OUTPUT PRINT-FILE.                                  NC1064.2
042100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1064.2
042200     MOVE    SPACE TO TEST-RESULTS.                               NC1064.2
042300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1064.2
042400     GO TO CCVS1-EXIT.                                            NC1064.2
042500 CLOSE-FILES.                                                     NC1064.2
042600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1064.2
042700 TERMINATE-CCVS.                                                  NC1064.2
042800*    EXIT PROGRAM.                                                NC1064.2
042900*TERMINATE-CALL.                                                  NC1064.2
043000     STOP     RUN.                                                NC1064.2
043100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1064.2
043200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1064.2
043300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1064.2
043400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1064.2
043500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1064.2
043600 PRINT-DETAIL.                                                    NC1064.2
043700     IF REC-CT NOT EQUAL TO ZERO                                  NC1064.2
043800             MOVE "." TO PARDOT-X                                 NC1064.2
043900             MOVE REC-CT TO DOTVALUE.                             NC1064.2
044000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1064.2
044100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1064.2
044200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1064.2
044300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1064.2
044400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1064.2
044500     MOVE SPACE TO CORRECT-X.                                     NC1064.2
044600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1064.2
044700     MOVE     SPACE TO RE-MARK.                                   NC1064.2
044800 HEAD-ROUTINE.                                                    NC1064.2
044900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1064.2
045000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1064.2
045100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1064.2
045200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1064.2
045300 COLUMN-NAMES-ROUTINE.                                            NC1064.2
045400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1064.2
045500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1064.2
045600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1064.2
045700 END-ROUTINE.                                                     NC1064.2
045800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1064.2
045900 END-RTN-EXIT.                                                    NC1064.2
046000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1064.2
046100 END-ROUTINE-1.                                                   NC1064.2
046200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1064.2
046300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1064.2
046400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1064.2
046500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1064.2
046600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1064.2
046700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1064.2
046800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1064.2
046900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1064.2
047000  END-ROUTINE-12.                                                 NC1064.2
047100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1064.2
047200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1064.2
047300         MOVE "NO " TO ERROR-TOTAL                                NC1064.2
047400         ELSE                                                     NC1064.2
047500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1064.2
047600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1064.2
047700     PERFORM WRITE-LINE.                                          NC1064.2
047800 END-ROUTINE-13.                                                  NC1064.2
047900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1064.2
048000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1064.2
048100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1064.2
048200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1064.2
048300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1064.2
048400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1064.2
048500          MOVE "NO " TO ERROR-TOTAL                               NC1064.2
048600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1064.2
048700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1064.2
048800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1064.2
048900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1064.2
049000 WRITE-LINE.                                                      NC1064.2
049100     ADD 1 TO RECORD-COUNT.                                       NC1064.2
049200*    IF RECORD-COUNT GREATER 42                                   NC1064.2
049300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1064.2
049400*        MOVE SPACE TO DUMMY-RECORD                               NC1064.2
049500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1064.2
049600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1064.2
049700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1064.2
049800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1064.2
049900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1064.2
050000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1064.2
050100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1064.2
050200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1064.2
050300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1064.2
050400*        MOVE ZERO TO RECORD-COUNT.                               NC1064.2
050500     PERFORM WRT-LN.                                              NC1064.2
050600 WRT-LN.                                                          NC1064.2
050700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1064.2
050800     MOVE SPACE TO DUMMY-RECORD.                                  NC1064.2
050900 BLANK-LINE-PRINT.                                                NC1064.2
051000     PERFORM WRT-LN.                                              NC1064.2
051100 FAIL-ROUTINE.                                                    NC1064.2
051200     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1064.2
051300            GO TO FAIL-ROUTINE-WRITE.                             NC1064.2
051400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1064.2
051500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1064.2
051600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1064.2
051700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1064.2
051800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1064.2
051900     GO TO  FAIL-ROUTINE-EX.                                      NC1064.2
052000 FAIL-ROUTINE-WRITE.                                              NC1064.2
052100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1064.2
052200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1064.2
052300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1064.2
052400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1064.2
052500 FAIL-ROUTINE-EX. EXIT.                                           NC1064.2
052600 BAIL-OUT.                                                        NC1064.2
052700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1064.2
052800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1064.2
052900 BAIL-OUT-WRITE.                                                  NC1064.2
053000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1064.2
053100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1064.2
053200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1064.2
053300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1064.2
053400 BAIL-OUT-EX. EXIT.                                               NC1064.2
053500 CCVS1-EXIT.                                                      NC1064.2
053600     EXIT.                                                        NC1064.2
053700 SECT-NC106A-001 SECTION.                                         NC1064.2
053800 SUB-INIT-F1-1.                                                   NC1064.2
053900     MOVE "SUBTRACT" TO FEATURE.                                  NC1064.2
054000     MOVE "VI-134 6.25.4 GR1" TO ANSI-REFERENCE.                  NC1064.2
054100 SUB-TEST-F1-1.                                                   NC1064.2
054200     SUBTRACT 1 FROM N-5.                                         NC1064.2
054300     IF       N-5 EQUAL TO 0                                      NC1064.2
054400              PERFORM PASS                                        NC1064.2
054500              GO TO SUB-WRITE-F1-1.                               NC1064.2
054600     GO TO    SUB-FAIL-F1-1.                                      NC1064.2
054700 SUB-DELETE-F1-1.                                                 NC1064.2
054800     PERFORM  DE-LETE.                                            NC1064.2
054900     GO TO    SUB-WRITE-F1-1.                                     NC1064.2
055000 SUB-FAIL-F1-1.                                                   NC1064.2
055100     MOVE     N-5 TO COMPUTED-N.                                  NC1064.2
055200     MOVE     0 TO CORRECT-N.                                     NC1064.2
055300     PERFORM  FAIL.                                               NC1064.2
055400 SUB-WRITE-F1-1.                                                  NC1064.2
055500     MOVE "SUB-TEST-F1-1 " TO PAR-NAME.                           NC1064.2
055600     PERFORM  PRINT-DETAIL.                                       NC1064.2
055700 SUB-TEST-F1-2.                                                   NC1064.2
055800     SUBTRACT N-17 FROM N-18 ROUNDED.                             NC1064.2
055900     IF       N-18 EQUAL TO -9                                    NC1064.2
056000              PERFORM PASS                                        NC1064.2
056100              GO TO SUB-WRITE-F1-2.                               NC1064.2
056200     GO TO    SUB-FAIL-F1-2.                                      NC1064.2
056300 SUB-DELETE-F1-2.                                                 NC1064.2
056400     PERFORM  DE-LETE.                                            NC1064.2
056500     GO TO    SUB-WRITE-F1-2.                                     NC1064.2
056600 SUB-FAIL-F1-2.                                                   NC1064.2
056700     MOVE     N-18 TO COMPUTED-N.                                 NC1064.2
056800     MOVE     -9 TO CORRECT-N.                                    NC1064.2
056900     PERFORM  FAIL.                                               NC1064.2
057000 SUB-WRITE-F1-2.                                                  NC1064.2
057100     MOVE "SUB-TEST-F1-2 " TO PAR-NAME.                           NC1064.2
057200     PERFORM  PRINT-DETAIL.                                       NC1064.2
057300 SUB-INIT-F1-3.                                                   NC1064.2
057400     MOVE     -00001 TO N-10.                                     NC1064.2
057500     MOVE      99999 TO N-13.                                     NC1064.2
057600 SUB-TEST-F1-3-0.                                                 NC1064.2
057700     SUBTRACT N-10 FROM N-13 ON SIZE ERROR                        NC1064.2
057800              PERFORM PASS                                        NC1064.2
057900              GO TO SUB-WRITE-F1-3-1.                             NC1064.2
058000     GO TO    SUB-FAIL-F1-3-1.                                    NC1064.2
058100 SUB-DELETE-F1-3-1.                                               NC1064.2
058200     PERFORM  DE-LETE.                                            NC1064.2
058300     GO TO    SUB-WRITE-F1-3-1.                                   NC1064.2
058400 SUB-FAIL-F1-3-1.                                                 NC1064.2
058500     MOVE     N-13 TO COMPUTED-N.                                 NC1064.2
058600     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1064.2
058700     PERFORM  FAIL.                                               NC1064.2
058800 SUB-WRITE-F1-3-1.                                                NC1064.2
058900     MOVE "SUB-TEST-F1-3-1 " TO PAR-NAME.                         NC1064.2
059000     PERFORM  PRINT-DETAIL.                                       NC1064.2
059100 SUB-TEST-F1-3-2.                                                 NC1064.2
059200     IF       N-13 = 99999                                        NC1064.2
059300              PERFORM PASS                                        NC1064.2
059400              GO TO SUB-WRITE-F1-3-2.                             NC1064.2
059500     GO TO    SUB-FAIL-F1-3-2.                                    NC1064.2
059600 SUB-DELETE-F1-3-2.                                               NC1064.2
059700     PERFORM  DE-LETE.                                            NC1064.2
059800     GO TO    SUB-WRITE-F1-3-2.                                   NC1064.2
059900 SUB-FAIL-F1-3-2.                                                 NC1064.2
060000     MOVE     N-13  TO COMPUTED-N.                                NC1064.2
060100     MOVE     99999 TO CORRECT-N.                                 NC1064.2
060200     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1064.2
060300     PERFORM  FAIL.                                               NC1064.2
060400 SUB-WRITE-F1-3-2.                                                NC1064.2
060500     MOVE "SUB-TEST-F1-3-2 " TO PAR-NAME.                         NC1064.2
060600     PERFORM  PRINT-DETAIL.                                       NC1064.2
060700 SUB-INIT-F1-4-1.                                                 NC1064.2
060800     MOVE  -999999999 TO N-20.                                    NC1064.2
060900 SUB-TEST-F1-4-1.                                                 NC1064.2
061000     SUBTRACT .7 FROM N-20 ROUNDED ON SIZE ERROR                  NC1064.2
061100              PERFORM PASS                                        NC1064.2
061200              GO TO SUB-WRITE-F1-4-1.                             NC1064.2
061300     GO TO    SUB-FAIL-F1-4-1.                                    NC1064.2
061400 SUB-DELETE-F1-4-1.                                               NC1064.2
061500     PERFORM  DE-LETE.                                            NC1064.2
061600     GO TO    SUB-WRITE-F1-4-1.                                   NC1064.2
061700 SUB-FAIL-F1-4-1.                                                 NC1064.2
061800     MOVE     N-20 TO COMPUTED-N.                                 NC1064.2
061900     MOVE     -999999999 TO CORRECT-N.                            NC1064.2
062000     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1064.2
062100     PERFORM  FAIL.                                               NC1064.2
062200 SUB-WRITE-F1-4-1.                                                NC1064.2
062300     MOVE "SUB-TEST-F1-4-1 " TO PAR-NAME.                         NC1064.2
062400     PERFORM  PRINT-DETAIL.                                       NC1064.2
062500 SUB-TEST-F1-4-2.                                                 NC1064.2
062600     IF       N-20 = -999999999                                   NC1064.2
062700              PERFORM PASS                                        NC1064.2
062800              GO TO SUB-WRITE-F1-4-2.                             NC1064.2
062900     GO TO    SUB-FAIL-F1-4-2.                                    NC1064.2
063000 SUB-DELETE-F1-4-2.                                               NC1064.2
063100     PERFORM  DE-LETE.                                            NC1064.2
063200     GO TO    SUB-WRITE-F1-4-2.                                   NC1064.2
063300 SUB-FAIL-F1-4-2.                                                 NC1064.2
063400     MOVE     N-20 TO COMPUTED-N.                                 NC1064.2
063500     MOVE     -999999999 TO CORRECT-N.                            NC1064.2
063600     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1064.2
063700     PERFORM  FAIL.                                               NC1064.2
063800 SUB-WRITE-F1-4-2.                                                NC1064.2
063900     MOVE "SUB-TEST-F1-4-2 " TO PAR-NAME.                         NC1064.2
064000     PERFORM  PRINT-DETAIL.                                       NC1064.2
064100 SUB-INIT-F1-5.                                                   NC1064.2
064200     MOVE     "SUBTRACT ---" TO FEATURE.                          NC1064.2
064300     PERFORM  PRINT-DETAIL.                                       NC1064.2
064400     MOVE     "  FROM" TO FEATURE.                                NC1064.2
064500 SUB-TEST-F1-5.                                                   NC1064.2
064600     MOVE     A18TWOS-DS-18V00 TO WRK-DS-18V00.                   NC1064.2
064700     SUBTRACT A18ONES-DS-18V00 FROM WRK-DS-18V00.                 NC1064.2
064800     IF       WRK-DS-18V00 EQUAL TO 111111111111111111            NC1064.2
064900              PERFORM PASS GO TO SUB-WRITE-F1-5.                  NC1064.2
065000     GO       TO SUB-FAIL-F1-5.                                   NC1064.2
065100 SUB-DELETE-F1-5.                                                 NC1064.2
065200     PERFORM  DE-LETE.                                            NC1064.2
065300     GO       TO SUB-WRITE-F1-5.                                  NC1064.2
065400 SUB-FAIL-F1-5.                                                   NC1064.2
065500     MOVE     WRK-DS-18V00 TO COMPUTED-18V0.                      NC1064.2
065600     MOVE     111111111111111111 TO CORRECT-18V0.                 NC1064.2
065700     PERFORM  FAIL.                                               NC1064.2
065800 SUB-WRITE-F1-5.                                                  NC1064.2
065900     MOVE     "SUB-TEST-F1-5" TO PAR-NAME.                        NC1064.2
066000     PERFORM  PRINT-DETAIL.                                       NC1064.2
066100 SUB-TEST-F1-6.                                                   NC1064.2
066200     MOVE     A12THREES-DS-06V06 TO WRK-DS-06V06.                 NC1064.2
066300     SUBTRACT A05ONES-DS-05V00                                    NC1064.2
066400              A05ONES-DS-00V05                                    NC1064.2
066500              A06ONES-DS-03V03 FROM WRK-DS-06V06.                 NC1064.2
066600     IF       WRK-DS-06V06 EQUAL TO 322111.111223                 NC1064.2
066700              PERFORM PASS GO TO SUB-WRITE-F1-6.                  NC1064.2
066800     GO       TO SUB-FAIL-F1-6.                                   NC1064.2
066900 SUB-DELETE-F1-6.                                                 NC1064.2
067000     PERFORM  DE-LETE.                                            NC1064.2
067100     GO       TO SUB-WRITE-F1-6.                                  NC1064.2
067200 SUB-FAIL-F1-6.                                                   NC1064.2
067300     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1064.2
067400     MOVE     322111.111223 TO CORRECT-N.                         NC1064.2
067500     PERFORM  FAIL.                                               NC1064.2
067600 SUB-WRITE-F1-6.                                                  NC1064.2
067700     MOVE     "SUB-TEST-F1-6" TO PAR-NAME.                        NC1064.2
067800     PERFORM  PRINT-DETAIL.                                       NC1064.2
067900 SUB-INIT-F1-7.                                                   NC1064.2
068000     MOVE     "  ROUNDED" TO FEATURE.                             NC1064.2
068100 SUB-TEST-F1-7.                                                   NC1064.2
068200     MOVE     ZERO TO WRK-DS-0201P.                               NC1064.2
068300     SUBTRACT A99-DS-02V00 FROM WRK-DS-0201P ROUNDED.             NC1064.2
068400     IF       WRK-DS-0201P EQUAL TO -100                          NC1064.2
068500              PERFORM PASS GO TO SUB-WRITE-F1-7.                  NC1064.2
068600     GO       TO SUB-FAIL-F1-7.                                   NC1064.2
068700 SUB-DELETE-F1-7.                                                 NC1064.2
068800     PERFORM  DE-LETE.                                            NC1064.2
068900     GO       TO SUB-WRITE-F1-7.                                  NC1064.2
069000 SUB-FAIL-F1-7.                                                   NC1064.2
069100     MOVE     WRK-DS-0201P TO COMPUTED-N.                         NC1064.2
069200     MOVE    -100 TO CORRECT-N.                                   NC1064.2
069300     PERFORM  FAIL.                                               NC1064.2
069400 SUB-WRITE-F1-7.                                                  NC1064.2
069500     MOVE     "SUB-TEST-F1-7" TO PAR-NAME.                        NC1064.2
069600     PERFORM  PRINT-DETAIL.                                       NC1064.2
069700 SUB-INIT-F1-8-1.                                                 NC1064.2
069800     MOVE     "  SIZE ERROR" TO FEATURE.                          NC1064.2
069900     MOVE     -11 TO WRK-DS-02V00.                                NC1064.2
070000 SUB-TEST-F1-8-1.                                                 NC1064.2
070100     SUBTRACT A99-DS-02V00 FROM WRK-DS-02V00 ON SIZE ERROR        NC1064.2
070200              PERFORM PASS GO TO SUB-WRITE-F1-8-1.                NC1064.2
070300     GO       TO SUB-FAIL-F1-8-1.                                 NC1064.2
070400 SUB-DELETE-F1-8-1.                                               NC1064.2
070500     PERFORM  DE-LETE.                                            NC1064.2
070600     GO       TO SUB-WRITE-F1-8-1.                                NC1064.2
070700 SUB-FAIL-F1-8-1.                                                 NC1064.2
070800     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1064.2
070900     PERFORM  FAIL.                                               NC1064.2
071000 SUB-WRITE-F1-8-1.                                                NC1064.2
071100     MOVE     "SUB-TEST-F1-8-1" TO PAR-NAME.                      NC1064.2
071200     PERFORM  PRINT-DETAIL.                                       NC1064.2
071300 SUB-TEST-F1-8-2.                                                 NC1064.2
071400     IF       WRK-DS-02V00 EQUAL TO -11                           NC1064.2
071500              PERFORM PASS GO TO SUB-WRITE-F1-8-2.                NC1064.2
071600*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F1-8-1   NC1064.2
071700     GO       TO SUB-FAIL-F1-8-2.                                 NC1064.2
071800 SUB-DELETE-F1-8-2.                                               NC1064.2
071900     PERFORM  DE-LETE.                                            NC1064.2
072000     GO       TO SUB-WRITE-F1-8-2.                                NC1064.2
072100 SUB-FAIL-F1-8-2.                                                 NC1064.2
072200     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1064.2
072300     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC1064.2
072400     MOVE     -11 TO CORRECT-N.                                   NC1064.2
072500     PERFORM  FAIL.                                               NC1064.2
072600 SUB-WRITE-F1-8-2.                                                NC1064.2
072700     MOVE     "SUB-TEST-F1-8-2" TO PAR-NAME.                      NC1064.2
072800     PERFORM  PRINT-DETAIL.                                       NC1064.2
072900 SUB-INIT-F1-9-1.                                                 NC1064.2
073000     MOVE     "  ROUNDED,SIZE ERROR" TO FEATURE.                  NC1064.2
073100 SUB-TEST-F1-9-1.                                                 NC1064.2
073200     MOVE     ZERO TO WRK-DS-05V00.                               NC1064.2
073300     SUBTRACT 33333                                               NC1064.2
073400              A06THREES-DS-03V03                                  NC1064.2
073500              A12THREES-DS-06V06                                  NC1064.2
073600              FROM WRK-DS-05V00 ROUNDED ON SIZE ERROR             NC1064.2
073700              PERFORM PASS GO TO SUB-WRITE-F1-9-1.                NC1064.2
073800     GO       TO SUB-FAIL-F1-9-1.                                 NC1064.2
073900 SUB-DELETE-F1-9-1.                                               NC1064.2
074000     PERFORM  DE-LETE.                                            NC1064.2
074100     GO       TO SUB-WRITE-F1-9-1.                                NC1064.2
074200 SUB-FAIL-F1-9-1.                                                 NC1064.2
074300     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1064.2
074400     PERFORM  FAIL.                                               NC1064.2
074500 SUB-WRITE-F1-9-1.                                                NC1064.2
074600     MOVE     "SUB-TEST-F1-9-1" TO PAR-NAME.                      NC1064.2
074700     PERFORM  PRINT-DETAIL.                                       NC1064.2
074800 SUB-TEST-F1-9-2.                                                 NC1064.2
074900     IF       WRK-DS-05V00 EQUAL TO ZERO                          NC1064.2
075000              PERFORM PASS GO TO SUB-WRITE-F1-9-2.                NC1064.2
075100*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F1-9-1   NC1064.2
075200     GO       TO SUB-FAIL-F1-9-2.                                 NC1064.2
075300 SUB-DELETE-F1-9-2.                                               NC1064.2
075400     PERFORM  DE-LETE.                                            NC1064.2
075500     GO       TO SUB-WRITE-F1-9-2.                                NC1064.2
075600 SUB-FAIL-F1-9-2.                                                 NC1064.2
075700     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1064.2
075800     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC1064.2
075900     MOVE     ZERO TO CORRECT-N.                                  NC1064.2
076000     PERFORM  FAIL.                                               NC1064.2
076100 SUB-WRITE-F1-9-2.                                                NC1064.2
076200     MOVE     "SUB-TEST-F1-9-2" TO PAR-NAME.                      NC1064.2
076300     PERFORM  PRINT-DETAIL.                                       NC1064.2
076400 SUB-INIT-F1-10.                                                  NC1064.2
076500     MOVE     ZERO TO WRK-DS-06V06.                               NC1064.2
076600 SUB-TEST-F1-10-1.                                                NC1064.2
076700     SUBTRACT A12THREES-DS-06V06                                  NC1064.2
076800              333333                                              NC1064.2
076900              A06THREES-DS-03V03                                  NC1064.2
077000              -0000009                                            NC1064.2
077100              FROM WRK-DS-06V06 ROUNDED ON SIZE ERROR             NC1064.2
077200              GO TO SUB-FAIL-F1-10-1.                             NC1064.2
077300     PERFORM  PASS.                                               NC1064.2
077400     GO       TO SUB-WRITE-F1-10-1.                               NC1064.2
077500 SUB-DELETE-F1-10-1.                                              NC1064.2
077600     PERFORM  DE-LETE.                                            NC1064.2
077700     GO       TO SUB-WRITE-F1-10-1.                               NC1064.2
077800 SUB-FAIL-F1-10-1.                                                NC1064.2
077900     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1064.2
078000     PERFORM  FAIL.                                               NC1064.2
078100 SUB-WRITE-F1-10-1.                                               NC1064.2
078200     MOVE     "SUB-TEST-F1-10-1" TO PAR-NAME.                     NC1064.2
078300     PERFORM  PRINT-DETAIL.                                       NC1064.2
078400 SUB-TEST-F1-10-2.                                                NC1064.2
078500     IF       WRK-DS-06V06 EQUAL TO -666990.666333                NC1064.2
078600              PERFORM PASS GO TO SUB-WRITE-F1-10-2.               NC1064.2
078700*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F1-10-1  NC1064.2
078800     GO       TO SUB-FAIL-F1-10-2.                                NC1064.2
078900 SUB-DELETE-F1-10-2.                                              NC1064.2
079000     PERFORM  DE-LETE.                                            NC1064.2
079100     GO       TO SUB-WRITE-F1-10-2.                               NC1064.2
079200 SUB-FAIL-F1-10-2.                                                NC1064.2
079300     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1064.2
079400     MOVE     -666990.666333 TO CORRECT-N.                        NC1064.2
079500     PERFORM  FAIL.                                               NC1064.2
079600 SUB-WRITE-F1-10-2.                                               NC1064.2
079700     MOVE     "SUB-TEST-F1-10-2" TO PAR-NAME.                     NC1064.2
079800     PERFORM  PRINT-DETAIL.                                       NC1064.2
079900 SUB-INIT-F1-11.                                                  NC1064.2
080000     MOVE     "  COMP VS. DISPLAY" TO FEATURE.                    NC1064.2
080100 SUB-TEST-F1-11.                                                  NC1064.2
080200     MOVE     ZERO TO WRK-CS-18V00                                NC1064.2
080300     SUBTRACT A18ONES-DS-18V00 FROM WRK-CS-18V00.                 NC1064.2
080400     IF       WRK-CS-18V00 EQUAL TO -111111111111111111           NC1064.2
080500              PERFORM PASS GO TO SUB-WRITE-F1-11.                 NC1064.2
080600     GO       TO SUB-FAIL-F1-11.                                  NC1064.2
080700 SUB-DELETE-F1-11.                                                NC1064.2
080800     PERFORM  DE-LETE.                                            NC1064.2
080900     GO       TO SUB-WRITE-F1-11.                                 NC1064.2
081000 SUB-FAIL-F1-11.                                                  NC1064.2
081100     MOVE     WRK-CS-18V00 TO COMPUTED-18V0.                      NC1064.2
081200     MOVE     -111111111111111111 TO CORRECT-18V0.                NC1064.2
081300     PERFORM  FAIL.                                               NC1064.2
081400 SUB-WRITE-F1-11.                                                 NC1064.2
081500     MOVE     "SUB-TEST-F1-11" TO PAR-NAME.                       NC1064.2
081600     PERFORM  PRINT-DETAIL.                                       NC1064.2
081700 SUB-TEST-F1-12.                                                  NC1064.2
081800     MOVE     ZERO TO WRK-DS-18V00.                               NC1064.2
081900     SUBTRACT A18ONES-CS-18V00 FROM WRK-DS-18V00.                 NC1064.2
082000     IF       WRK-DS-18V00 EQUAL TO -111111111111111111           NC1064.2
082100              PERFORM PASS GO TO SUB-WRITE-F1-12.                 NC1064.2
082200     GO       TO SUB-FAIL-F1-12.                                  NC1064.2
082300 SUB-DELETE-F1-12.                                                NC1064.2
082400     PERFORM  DE-LETE.                                            NC1064.2
082500     GO       TO SUB-WRITE-F1-12.                                 NC1064.2
082600 SUB-FAIL-F1-12.                                                  NC1064.2
082700     MOVE     WRK-DS-18V00 TO COMPUTED-18V0.                      NC1064.2
082800     MOVE     -111111111111111111 TO CORRECT-18V0.                NC1064.2
082900     PERFORM  FAIL.                                               NC1064.2
083000 SUB-WRITE-F1-12.                                                 NC1064.2
083100     MOVE     "SUB-TEST-F1-12" TO PAR-NAME.                       NC1064.2
083200     PERFORM  PRINT-DETAIL.                                       NC1064.2
083300 SUB-TEST-F1-13.                                                  NC1064.2
083400     MOVE     ZERO TO WRK-CS-02V02.                               NC1064.2
083500     SUBTRACT A99-CS-02V00 FROM WRK-CS-02V02.                     NC1064.2
083600     IF       WRK-CS-02V02 EQUAL TO -99.00                        NC1064.2
083700              PERFORM PASS GO TO SUB-WRITE-F1-13.                 NC1064.2
083800     GO       TO SUB-FAIL-F1-13.                                  NC1064.2
083900 SUB-DELETE-F1-13.                                                NC1064.2
084000     PERFORM  DE-LETE.                                            NC1064.2
084100     GO       TO SUB-WRITE-F1-13.                                 NC1064.2
084200 SUB-FAIL-F1-13.                                                  NC1064.2
084300     MOVE     WRK-CS-02V02 TO COMPUTED-N.                         NC1064.2
084400     MOVE     -99.00 TO CORRECT-N.                                NC1064.2
084500     PERFORM  FAIL.                                               NC1064.2
084600 SUB-WRITE-F1-13.                                                 NC1064.2
084700     MOVE     "SUB-TEST-F1-13" TO PAR-NAME.                       NC1064.2
084800     PERFORM  PRINT-DETAIL.                                       NC1064.2
084900 SUB-TEST-F1-14-1.                                                NC1064.2
085000     MOVE     A99-CS-02V00 TO WRK-CS-02V02.                       NC1064.2
085100     SUBTRACT -99 FROM WRK-CS-02V02 ON SIZE ERROR                 NC1064.2
085200              PERFORM PASS GO TO SUB-WRITE-F1-14-1.               NC1064.2
085300     GO       TO SUB-FAIL-F1-14-1.                                NC1064.2
085400 SUB-DELETE-F1-14-1.                                              NC1064.2
085500     PERFORM  DE-LETE.                                            NC1064.2
085600     GO       TO SUB-WRITE-F1-14-1.                               NC1064.2
085700 SUB-FAIL-F1-14-1.                                                NC1064.2
085800     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1064.2
085900     PERFORM  FAIL.                                               NC1064.2
086000 SUB-WRITE-F1-14-1.                                               NC1064.2
086100     MOVE     "SUB-TEST-F1-14-1" TO PAR-NAME.                     NC1064.2
086200     PERFORM  PRINT-DETAIL.                                       NC1064.2
086300 SUB-TEST-F1-14-2.                                                NC1064.2
086400     IF       WRK-CS-02V02 EQUAL TO 99                            NC1064.2
086500              PERFORM PASS GO TO SUB-WRITE-F1-14-2.               NC1064.2
086600*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F1-14-1  NC1064.2
086700     GO       TO SUB-FAIL-F1-14-2.                                NC1064.2
086800 SUB-DELETE-F1-14-2.                                              NC1064.2
086900     PERFORM  DE-LETE.                                            NC1064.2
087000     GO       TO SUB-WRITE-F1-14-2.                               NC1064.2
087100 SUB-FAIL-F1-14-2.                                                NC1064.2
087200     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1064.2
087300     MOVE     WRK-CS-02V02 TO COMPUTED-N.                         NC1064.2
087400     MOVE     99 TO CORRECT-N.                                    NC1064.2
087500     PERFORM  FAIL.                                               NC1064.2
087600 SUB-WRITE-F1-14-2.                                               NC1064.2
087700     MOVE     "SUB-TEST-F1-14-2" TO PAR-NAME.                     NC1064.2
087800     PERFORM  PRINT-DETAIL.                                       NC1064.2
087900 SUB-TEST-F1-15.                                                  NC1064.2
088000     SUBTRACT SUBTR-1 SUBTR-3 FROM SUBTR-7.                       NC1064.2
088100     IF       SUBTR-7 EQUAL TO 99                                 NC1064.2
088200              PERFORM PASS GO TO SUB-WRITE-F1-15.                 NC1064.2
088300     GO       TO SUB-FAIL-F1-15.                                  NC1064.2
088400 SUB-DELETE-F1-15.                                                NC1064.2
088500     PERFORM DE-LETE.                                             NC1064.2
088600     GO       TO SUB-WRITE-F1-15.                                 NC1064.2
088700 SUB-FAIL-F1-15.                                                  NC1064.2
088800     MOVE     SUBTR-7 TO COMPUTED-N.                              NC1064.2
088900     MOVE     99        TO CORRECT-N.                             NC1064.2
089000     PERFORM FAIL.                                                NC1064.2
089100 SUB-WRITE-F1-15.                                                 NC1064.2
089200     MOVE     "SUB-TEST-F1-15" TO PAR-NAME.                       NC1064.2
089300     PERFORM PRINT-DETAIL.                                        NC1064.2
089400 SUB-TEST-F1-16.                                                  NC1064.2
089500     SUBTRACT SUBTR-5 -98 SUBTR-3 -1 FROM SUBTR-10.               NC1064.2
089600     IF       SUBTR-10 EQUAL TO 100                               NC1064.2
089700              PERFORM PASS GO TO SUB-WRITE-F1-16.                 NC1064.2
089800     GO       TO SUB-FAIL-F1-16.                                  NC1064.2
089900 SUB-DELETE-F1-16.                                                NC1064.2
090000     PERFORM DE-LETE.                                             NC1064.2
090100     GO       TO SUB-WRITE-F1-16.                                 NC1064.2
090200 SUB-FAIL-F1-16.                                                  NC1064.2
090300     MOVE     SUBTR-10  TO COMPUTED-N.                            NC1064.2
090400     MOVE     100       TO CORRECT-N.                             NC1064.2
090500     PERFORM FAIL.                                                NC1064.2
090600 SUB-WRITE-F1-16.                                                 NC1064.2
090700     MOVE     "SUB-TEST-F1-16" TO PAR-NAME.                       NC1064.2
090800     PERFORM PRINT-DETAIL.                                        NC1064.2
090900 SUB-TEST-F1-17.                                                  NC1064.2
091000     SUBTRACT SUBTR-4 FROM SUBTR-6 ROUNDED.                       NC1064.2
091100     IF       SUBTR-6 EQUAL TO 1                                  NC1064.2
091200              PERFORM PASS GO TO SUB-WRITE-F1-17.                 NC1064.2
091300     GO       TO SUB-FAIL-F1-17.                                  NC1064.2
091400 SUB-DELETE-F1-17.                                                NC1064.2
091500     PERFORM DE-LETE.                                             NC1064.2
091600     GO       TO SUB-WRITE-F1-17.                                 NC1064.2
091700 SUB-FAIL-F1-17.                                                  NC1064.2
091800     MOVE     SUBTR-6   TO COMPUTED-N.                            NC1064.2
091900     MOVE     1         TO CORRECT-N.                             NC1064.2
092000     PERFORM FAIL.                                                NC1064.2
092100 SUB-WRITE-F1-17.                                                 NC1064.2
092200     MOVE     "SUB-TEST-F1-17" TO PAR-NAME.                       NC1064.2
092300     PERFORM PRINT-DETAIL.                                        NC1064.2
092400 SUB-TEST-F1-18-1.                                                NC1064.2
092500     SUBTRACT .01 FROM SUBTR-8 ON SIZE ERROR                      NC1064.2
092600              PERFORM PASS GO TO SUB-WRITE-F1-18-1.               NC1064.2
092700     GO       TO SUB-FAIL-F1-18-1.                                NC1064.2
092800 SUB-DELETE-F1-18-1.                                              NC1064.2
092900     PERFORM DE-LETE.                                             NC1064.2
093000     GO       TO SUB-WRITE-F1-18-1.                               NC1064.2
093100 SUB-FAIL-F1-18-1.                                                NC1064.2
093200     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1064.2
093300     PERFORM FAIL.                                                NC1064.2
093400 SUB-WRITE-F1-18-1.                                               NC1064.2
093500     MOVE     "SUB-TEST-F1-18-1" TO PAR-NAME.                     NC1064.2
093600     PERFORM PRINT-DETAIL.                                        NC1064.2
093700 SUB-TEST-F1-18-2.                                                NC1064.2
093800     IF       SUBTR-8 EQUAL TO -9.99                              NC1064.2
093900              PERFORM PASS GO TO SUB-WRITE-F1-18-2.               NC1064.2
094000*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F1-18-1  NC1064.2
094100     GO       TO SUB-FAIL-F1-18-2.                                NC1064.2
094200 SUB-DELETE-F1-18-2.                                              NC1064.2
094300     PERFORM DE-LETE.                                             NC1064.2
094400     GO       TO SUB-WRITE-F1-18-2.                               NC1064.2
094500 SUB-FAIL-F1-18-2.                                                NC1064.2
094600     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1064.2
094700     MOVE     SUBTR-8   TO COMPUTED-N.                            NC1064.2
094800     MOVE     -9.99     TO CORRECT-N.                             NC1064.2
094900     PERFORM FAIL.                                                NC1064.2
095000 SUB-WRITE-F1-18-2.                                               NC1064.2
095100     MOVE     "SUB-TEST-F1-18-2" TO PAR-NAME.                     NC1064.2
095200     PERFORM PRINT-DETAIL.                                        NC1064.2
095300 SUB-TEST-F1-19.                                                  NC1064.2
095400     MOVE A18FIVES-CS-18V00 TO WRK-CS-18V00.                      NC1064.2
095500     SUBTRACT A18THREES-CS-18V00 FROM WRK-CS-18V00.               NC1064.2
095600     IF WRK-CS-18V00 EQUAL TO -222222222222222222                 NC1064.2
095700         PERFORM PASS                                             NC1064.2
095800         GO TO SUB-WRITE-F1-19.                                   NC1064.2
095900     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1064.2
096000     MOVE -222222222222222222 TO CORRECT-18V0.                    NC1064.2
096100     PERFORM FAIL.                                                NC1064.2
096200     GO TO SUB-WRITE-F1-19.                                       NC1064.2
096300 SUB-DELETE-F1-19.                                                NC1064.2
096400     PERFORM DE-LETE.                                             NC1064.2
096500 SUB-WRITE-F1-19.                                                 NC1064.2
096600     MOVE "SUB-TEST-F1-19 " TO PAR-NAME.                          NC1064.2
096700     PERFORM PRINT-DETAIL.                                        NC1064.2
096800 SUB-TEST-F1-20.                                                  NC1064.2
096900     MOVE     -980 TO WRK-CS-03V00.                               NC1064.2
097000     MOVE     SPACE TO SIZE-ERR.                                  NC1064.2
097100*        NOTE IN THIS TEST, 1 IS SUBTRACTED FROM A 3-DIGIT COMP   NC1064.2
097200*             SYNC FIELD UNTIL A SIZE ERROR OCCURS --- IF THE     NC1064.2
097300*             VALUE OF THE FIELD REACHES -1180 WITHOUT A SIZE     NC1064.2
097400*             ERROR, THEN THE ATTEMPTED SUBTRACTIONS ARE STOPPED. NC1064.2
097500     PERFORM  SUB-A-F1-20 THRU SUB-B-F1-20 200 TIMES.             NC1064.2
097600     IF       SIZE-ERR EQUAL TO SPACE                             NC1064.2
097700              MOVE "SIZE ERROR NOT ENCOUNTERED" TO RE-MARK        NC1064.2
097800              MOVE "-1180 OR LESS" TO COMPUTED-A                  NC1064.2
097900              MOVE "-999 IN S999 FIELD" TO CORRECT-A              NC1064.2
098000              PERFORM FAIL                                        NC1064.2
098100              GO TO SUB-WRITE-F1-20.                              NC1064.2
098200     IF       WRK-CS-03V00 EQUAL TO -999                          NC1064.2
098300              PERFORM PASS GO TO SUB-WRITE-F1-20.                 NC1064.2
098400     PERFORM  FAIL.                                               NC1064.2
098500     MOVE     WRK-CS-03V00 TO COMPUTED-N.                         NC1064.2
098600     MOVE     -999 TO CORRECT-N.                                  NC1064.2
098700     GO       TO SUB-WRITE-F1-20.                                 NC1064.2
098800 SUB-DELETE-F1-20.                                                NC1064.2
098900     PERFORM  DE-LETE.                                            NC1064.2
099000     GO       TO SUB-WRITE-F1-20.                                 NC1064.2
099100 SUB-A-F1-20.                                                     NC1064.2
099200     IF       SIZE-ERR EQUAL TO "E" GO TO SUB-B-F1-20.            NC1064.2
099300     SUBTRACT 1 FROM WRK-CS-03V00 ON SIZE ERROR                   NC1064.2
099400              MOVE "E" TO SIZE-ERR.                               NC1064.2
099500 SUB-B-F1-20.                                                     NC1064.2
099600     EXIT.                                                        NC1064.2
099700 SUB-WRITE-F1-20.                                                 NC1064.2
099800     MOVE     "SUBT, COMP, SIZE ERR" TO FEATURE.                  NC1064.2
099900     MOVE     "SUB-TEST-F1-20" TO PAR-NAME.                       NC1064.2
100000     PERFORM  PRINT-DETAIL.                                       NC1064.2
100100*                                                                 NC1064.2
100200 SUB-INIT-F1-21.                                                  NC1064.2
100300*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1064.2
100400     MOVE     "VI-67 6.4.2" TO ANSI-REFERENCE.                    NC1064.2
100500     MOVE     -11 TO WRK-DS-02V00.                                NC1064.2
100600 SUB-TEST-F1-21.                                                  NC1064.2
100700     SUBTRACT A99-DS-02V00 FROM WRK-DS-02V00                      NC1064.2
100800          NOT ON SIZE ERROR                                       NC1064.2
100900              GO TO SUB-FAIL-F1-21.                               NC1064.2
101000     PERFORM  PASS GO TO SUB-WRITE-F1-21.                         NC1064.2
101100 SUB-DELETE-F1-21.                                                NC1064.2
101200     PERFORM  DE-LETE.                                            NC1064.2
101300     GO       TO SUB-WRITE-F1-21.                                 NC1064.2
101400 SUB-FAIL-F1-21.                                                  NC1064.2
101500     MOVE  "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK. NC1064.2
101600     PERFORM  FAIL.                                               NC1064.2
101700 SUB-WRITE-F1-21.                                                 NC1064.2
101800     MOVE     "SUB-TEST-F1-21" TO PAR-NAME.                       NC1064.2
101900     PERFORM  PRINT-DETAIL.                                       NC1064.2
102000*                                                                 NC1064.2
102100 SUB-INIT-F1-22.                                                  NC1064.2
102200*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1064.2
102300     MOVE     "VI-67 6.4.2" TO ANSI-REFERENCE.                    NC1064.2
102400     MOVE     ZERO TO WRK-DS-06V06.                               NC1064.2
102500 SUB-TEST-F1-22.                                                  NC1064.2
102600     SUBTRACT A12THREES-DS-06V06                                  NC1064.2
102700              333333                                              NC1064.2
102800              A06THREES-DS-03V03                                  NC1064.2
102900              -0000009                                            NC1064.2
103000              FROM WRK-DS-06V06 ROUNDED                           NC1064.2
103100          NOT ON SIZE ERROR                                       NC1064.2
103200              PERFORM PASS                                        NC1064.2
103300              GO TO   SUB-WRITE-F1-22.                            NC1064.2
103400     GO TO    SUB-FAIL-F1-22.                                     NC1064.2
103500 SUB-DELETE-F1-22.                                                NC1064.2
103600     PERFORM  DE-LETE.                                            NC1064.2
103700     GO       TO SUB-WRITE-F1-22.                                 NC1064.2
103800 SUB-FAIL-F1-22.                                                  NC1064.2
103900     MOVE     "NOT ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.  NC1064.2
104000     PERFORM  FAIL.                                               NC1064.2
104100 SUB-WRITE-F1-22.                                                 NC1064.2
104200     MOVE     "SUB-TEST-F1-22" TO PAR-NAME.                       NC1064.2
104300     PERFORM  PRINT-DETAIL.                                       NC1064.2
104400*                                                                 NC1064.2
104500 SUB-INIT-F1-23.                                                  NC1064.2
104600*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1064.2
104700     MOVE     "VI-67 6.4.2" TO ANSI-REFERENCE.                    NC1064.2
104800     MOVE     -11 TO WRK-DS-02V00.                                NC1064.2
104900 SUB-TEST-F1-23.                                                  NC1064.2
105000     SUBTRACT A99-DS-02V00 FROM WRK-DS-02V00                      NC1064.2
105100              ON SIZE ERROR                                       NC1064.2
105200              PERFORM  PASS GO TO SUB-WRITE-F1-23                 NC1064.2
105300          NOT ON SIZE ERROR                                       NC1064.2
105400              GO TO SUB-FAIL-F1-23.                               NC1064.2
105500 SUB-DELETE-F1-23.                                                NC1064.2
105600     PERFORM  DE-LETE.                                            NC1064.2
105700     GO       TO SUB-WRITE-F1-23.                                 NC1064.2
105800 SUB-FAIL-F1-23.                                                  NC1064.2
105900     MOVE  "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.         NC1064.2
106000     PERFORM  FAIL.                                               NC1064.2
106100 SUB-WRITE-F1-23.                                                 NC1064.2
106200     MOVE     "SUB-TEST-F1-23" TO PAR-NAME.                       NC1064.2
106300     PERFORM  PRINT-DETAIL.                                       NC1064.2
106400*                                                                 NC1064.2
106500 SUB-INIT-F1-24.                                                  NC1064.2
106600*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1064.2
106700     MOVE     "VI-67 6.4.2" TO ANSI-REFERENCE.                    NC1064.2
106800     MOVE     ZERO TO WRK-DS-06V06.                               NC1064.2
106900 SUB-TEST-F1-24.                                                  NC1064.2
107000     SUBTRACT A12THREES-DS-06V06                                  NC1064.2
107100              333333                                              NC1064.2
107200              A06THREES-DS-03V03                                  NC1064.2
107300              -0000009                                            NC1064.2
107400              FROM WRK-DS-06V06 ROUNDED                           NC1064.2
107500              ON SIZE ERROR                                       NC1064.2
107600              GO TO    SUB-FAIL-F1-24                             NC1064.2
107700          NOT ON SIZE ERROR                                       NC1064.2
107800              PERFORM PASS                                        NC1064.2
107900              GO TO    SUB-WRITE-F1-24.                           NC1064.2
108000 SUB-DELETE-F1-24.                                                NC1064.2
108100     PERFORM  DE-LETE.                                            NC1064.2
108200     GO       TO SUB-WRITE-F1-24.                                 NC1064.2
108300 SUB-FAIL-F1-24.                                                  NC1064.2
108400     MOVE     "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.      NC1064.2
108500     PERFORM  FAIL.                                               NC1064.2
108600 SUB-WRITE-F1-24.                                                 NC1064.2
108700     MOVE     "SUB-TEST-F1-24" TO PAR-NAME.                       NC1064.2
108800     PERFORM  PRINT-DETAIL.                                       NC1064.2
108900*                                                                 NC1064.2
109000 SUB-INIT-F1-25.                                                  NC1064.2
109100*    ==-->  MULTIPLE OPERANDS  <--==                              NC1064.2
109200     MOVE "VI-67 6.4.2" TO ANSI-REFERENCE.                        NC1064.2
109300     MOVE "SUBTR LIMIT TESTS " TO FEATURE.                        NC1064.2
109400     MOVE  1 TO DNAME1   DNAME2   DNAME3   DNAME4   DNAME5.       NC1064.2
109500     MOVE  1 TO DNAME6   DNAME7   DNAME8   DNAME9   DNAME10.      NC1064.2
109600     MOVE  1 TO DNAME11  DNAME12  DNAME13  DNAME14  DNAME14.      NC1064.2
109700     MOVE  1 TO DNAME16  DNAME17  DNAME18  DNAME19  DNAME20.      NC1064.2
109800     MOVE  1 TO DNAME21.                                          NC1064.2
109900     MOVE 21 TO DNAME22  DNAME23  DNAME24  DNAME25  DNAME26.      NC1064.2
110000     MOVE 21 TO DNAME27  DNAME28  DNAME29  DNAME30  DNAME31.      NC1064.2
110100     MOVE 21 TO DNAME32  DNAME33  DNAME34  DNAME35  DNAME36.      NC1064.2
110200     MOVE 21 TO DNAME37  DNAME38  DNAME39  DNAME40  DNAME41.      NC1064.2
110300     MOVE 21 TO DNAME42.                                          NC1064.2
110400*    THE FOLLOWING 22 TESTS VERIFY THE ABILITY OF THE COMPILER    NC1064.2
110500*    TO HANDLE A MAXIMUM OF 42 OPERANDS IN A SUBTRACT STATEMENT.  NC1064.2
110600*    A DELETION IN THIS PARAGRAPH WILL SKIP THE LIMIT TESTS.      NC1064.2
110700     GO TO SUB-TEST-F1-25-0.                                      NC1064.2
110800 SUB-DELETE-F1-25-0.                                              NC1064.2
110900     PERFORM DE-LETE.                                             NC1064.2
111000     MOVE "SUB-TEST-F1-25 - 26 " TO PAR-NAME.                     NC1064.2
111100     MOVE "SUBTR LIMITS TESTS " TO FEATURE.                       NC1064.2
111200     ADD 21 TO DELETE-COUNTER.                                    NC1064.2
111300     PERFORM PRINT-DETAIL.                                        NC1064.2
111400     GO TO SUB-INIT-F1-26.                                        NC1064.2
111500 SUB-TEST-F1-25-0.                                                NC1064.2
111600     SUBTRACT DNAME1                                              NC1064.2
111700              DNAME2                                              NC1064.2
111800              DNAME3                                              NC1064.2
111900              DNAME4                                              NC1064.2
112000              DNAME5                                              NC1064.2
112100              DNAME6                                              NC1064.2
112200              DNAME7                                              NC1064.2
112300              DNAME8                                              NC1064.2
112400              DNAME9                                              NC1064.2
112500              DNAME10                                             NC1064.2
112600              DNAME11                                             NC1064.2
112700              DNAME12                                             NC1064.2
112800              DNAME13                                             NC1064.2
112900              DNAME14                                             NC1064.2
113000              DNAME15                                             NC1064.2
113100              DNAME16                                             NC1064.2
113200              DNAME17                                             NC1064.2
113300              DNAME18                                             NC1064.2
113400              DNAME19                                             NC1064.2
113500              DNAME20                                             NC1064.2
113600              DNAME21                                             NC1064.2
113700         FROM DNAME22.                                            NC1064.2
113800 SUB-TEST-F1-25-1.                                                NC1064.2
113900     IF DNAME22 EQUAL TO ZERO                                     NC1064.2
114000         PERFORM PASS                                             NC1064.2
114100         GO TO SUB-WRITE-F1-25.                                   NC1064.2
114200     MOVE DNAME22 TO COMPUTED-18V0.                               NC1064.2
114300     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
114400     PERFORM FAIL.                                                NC1064.2
114500     GO TO SUB-WRITE-F1-25.                                       NC1064.2
114600 SUB-DELETE-F1-25.                                                NC1064.2
114700     PERFORM DE-LETE.                                             NC1064.2
114800 SUB-WRITE-F1-25.                                                 NC1064.2
114900     MOVE   "SUB-TEST-F1-25" TO PAR-NAME.                         NC1064.2
115000     PERFORM PRINT-DETAIL.                                        NC1064.2
115100 SUB-INIT-F1-26.                                                  NC1064.2
115200     MOVE 21 TO DNAME22  DNAME23  DNAME24  DNAME25  DNAME26.      NC1064.2
115300     MOVE 21 TO DNAME27  DNAME28  DNAME29  DNAME30  DNAME31.      NC1064.2
115400     MOVE 21 TO DNAME32  DNAME33  DNAME34  DNAME35  DNAME36.      NC1064.2
115500     MOVE 21 TO DNAME37  DNAME38  DNAME39  DNAME40  DNAME41.      NC1064.2
115600     MOVE 21 TO DNAME42.                                          NC1064.2
115700 SUB-TEST-F1-26-0.                                                NC1064.2
115800     SUBTRACT DNAME1                                              NC1064.2
115900              DNAME2                                              NC1064.2
116000              DNAME3                                              NC1064.2
116100              DNAME4                                              NC1064.2
116200              DNAME5                                              NC1064.2
116300              DNAME6                                              NC1064.2
116400              DNAME7                                              NC1064.2
116500              DNAME8                                              NC1064.2
116600              DNAME9                                              NC1064.2
116700              DNAME10                                             NC1064.2
116800              DNAME11                                             NC1064.2
116900              DNAME12                                             NC1064.2
117000              DNAME13                                             NC1064.2
117100              DNAME14                                             NC1064.2
117200              DNAME15                                             NC1064.2
117300              DNAME16                                             NC1064.2
117400              DNAME17                                             NC1064.2
117500              DNAME18                                             NC1064.2
117600              DNAME19                                             NC1064.2
117700              DNAME20                                             NC1064.2
117800              DNAME21                                             NC1064.2
117900         FROM DNAME22                                             NC1064.2
118000              DNAME23                                             NC1064.2
118100              DNAME24                                             NC1064.2
118200              DNAME25                                             NC1064.2
118300              DNAME26                                             NC1064.2
118400              DNAME27                                             NC1064.2
118500              DNAME28                                             NC1064.2
118600              DNAME29                                             NC1064.2
118700              DNAME30                                             NC1064.2
118800              DNAME31                                             NC1064.2
118900              DNAME32                                             NC1064.2
119000              DNAME33                                             NC1064.2
119100              DNAME34                                             NC1064.2
119200              DNAME35                                             NC1064.2
119300              DNAME36                                             NC1064.2
119400              DNAME37                                             NC1064.2
119500              DNAME38                                             NC1064.2
119600              DNAME39                                             NC1064.2
119700              DNAME40                                             NC1064.2
119800              DNAME41                                             NC1064.2
119900              DNAME42.                                            NC1064.2
120000 SUB-TEST-F1-26-1.                                                NC1064.2
120100     IF DNAME22 EQUAL TO ZERO                                     NC1064.2
120200         PERFORM PASS                                             NC1064.2
120300         GO TO SUB-WRITE-F1-26-1.                                 NC1064.2
120400     MOVE DNAME22 TO COMPUTED-18V0.                               NC1064.2
120500     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
120600     PERFORM FAIL.                                                NC1064.2
120700     GO TO SUB-WRITE-F1-26-1.                                     NC1064.2
120800 SUB-DELETE-F1-26-1.                                              NC1064.2
120900     PERFORM DE-LETE.                                             NC1064.2
121000 SUB-WRITE-F1-26-1.                                               NC1064.2
121100     MOVE "SUB-TEST-F1-26-1" TO PAR-NAME.                         NC1064.2
121200     PERFORM PRINT-DETAIL.                                        NC1064.2
121300 SUB-TEST-F1-26-2.                                                NC1064.2
121400     IF DNAME23 EQUAL TO ZERO                                     NC1064.2
121500         PERFORM PASS                                             NC1064.2
121600         GO TO SUB-WRITE-F1-26-2.                                 NC1064.2
121700     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
121800     MOVE DNAME23 TO COMPUTED-18V0.                               NC1064.2
121900     PERFORM FAIL.                                                NC1064.2
122000     GO TO SUB-WRITE-F1-26-2.                                     NC1064.2
122100 SUB-DELETE-F1-26-2.                                              NC1064.2
122200     PERFORM DE-LETE.                                             NC1064.2
122300 SUB-WRITE-F1-26-2.                                               NC1064.2
122400     MOVE "SUB-TEST-F1-26-2 " TO PAR-NAME.                        NC1064.2
122500     PERFORM PRINT-DETAIL.                                        NC1064.2
122600 SUB-TEST-F1-26-3.                                                NC1064.2
122700     IF DNAME24 EQUAL TO ZERO                                     NC1064.2
122800         PERFORM PASS                                             NC1064.2
122900         GO TO SUB-WRITE-F1-26-3.                                 NC1064.2
123000     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
123100     MOVE DNAME24 TO COMPUTED-18V0.                               NC1064.2
123200     PERFORM FAIL.                                                NC1064.2
123300     GO TO SUB-WRITE-F1-26-3.                                     NC1064.2
123400 SUB-DELETE-F1-26-3.                                              NC1064.2
123500     PERFORM DE-LETE.                                             NC1064.2
123600 SUB-WRITE-F1-26-3.                                               NC1064.2
123700     MOVE "SUB-TEST-F1-26-3 " TO PAR-NAME.                        NC1064.2
123800     PERFORM PRINT-DETAIL.                                        NC1064.2
123900 SUB-TEST-F1-26-4.                                                NC1064.2
124000     IF DNAME25 EQUAL TO ZERO                                     NC1064.2
124100         PERFORM PASS                                             NC1064.2
124200         GO TO SUB-WRITE-F1-26-4.                                 NC1064.2
124300     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
124400     MOVE DNAME25 TO COMPUTED-18V0.                               NC1064.2
124500     PERFORM FAIL.                                                NC1064.2
124600     GO TO SUB-WRITE-F1-26-4.                                     NC1064.2
124700 SUB-DELETE-F1-26-4.                                              NC1064.2
124800     PERFORM DE-LETE.                                             NC1064.2
124900 SUB-WRITE-F1-26-4.                                               NC1064.2
125000     MOVE "SUB-TEST-F1-26-4 " TO PAR-NAME.                        NC1064.2
125100     PERFORM PRINT-DETAIL.                                        NC1064.2
125200 SUB-TEST-F1-26-5.                                                NC1064.2
125300     IF DNAME26 EQUAL TO ZERO                                     NC1064.2
125400         PERFORM PASS                                             NC1064.2
125500         GO TO SUB-WRITE-F1-26-5.                                 NC1064.2
125600     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
125700     MOVE DNAME26 TO COMPUTED-18V0.                               NC1064.2
125800     PERFORM FAIL.                                                NC1064.2
125900     GO TO SUB-WRITE-F1-26-5.                                     NC1064.2
126000 SUB-DELETE-F1-26-5.                                              NC1064.2
126100     PERFORM DE-LETE.                                             NC1064.2
126200 SUB-WRITE-F1-26-5.                                               NC1064.2
126300     MOVE "SUB-TEST-F1-26-5 " TO PAR-NAME.                        NC1064.2
126400     PERFORM PRINT-DETAIL.                                        NC1064.2
126500 SUB-TEST-F1-26-6.                                                NC1064.2
126600     IF DNAME27 EQUAL TO ZERO                                     NC1064.2
126700         PERFORM PASS                                             NC1064.2
126800         GO TO SUB-WRITE-F1-26-6.                                 NC1064.2
126900     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
127000     MOVE DNAME27 TO COMPUTED-18V0.                               NC1064.2
127100     PERFORM FAIL.                                                NC1064.2
127200     GO TO SUB-WRITE-F1-26-6.                                     NC1064.2
127300 SUB-DELETE-F1-26-6.                                              NC1064.2
127400     PERFORM DE-LETE.                                             NC1064.2
127500 SUB-WRITE-F1-26-6.                                               NC1064.2
127600     MOVE "SUB-TEST-F1-26-6 " TO PAR-NAME.                        NC1064.2
127700     PERFORM PRINT-DETAIL.                                        NC1064.2
127800 SUB-TEST-F1-26-7.                                                NC1064.2
127900     IF DNAME28 EQUAL TO ZERO                                     NC1064.2
128000         PERFORM PASS                                             NC1064.2
128100         GO TO SUB-WRITE-F1-26-7.                                 NC1064.2
128200     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
128300     MOVE DNAME28 TO COMPUTED-18V0.                               NC1064.2
128400     PERFORM FAIL.                                                NC1064.2
128500     GO TO SUB-WRITE-F1-26-7.                                     NC1064.2
128600 SUB-DELETE-F1-26-7.                                              NC1064.2
128700     PERFORM DE-LETE.                                             NC1064.2
128800 SUB-WRITE-F1-26-7.                                               NC1064.2
128900     MOVE "SUB-TEST-F1-26-7 " TO PAR-NAME.                        NC1064.2
129000     PERFORM PRINT-DETAIL.                                        NC1064.2
129100 SUB-TEST-F1-26-8.                                                NC1064.2
129200     IF DNAME29 EQUAL TO ZERO                                     NC1064.2
129300         PERFORM PASS                                             NC1064.2
129400         GO TO SUB-WRITE-F1-26-8.                                 NC1064.2
129500     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
129600     MOVE DNAME29 TO COMPUTED-18V0.                               NC1064.2
129700     PERFORM FAIL.                                                NC1064.2
129800     GO TO SUB-WRITE-F1-26-8.                                     NC1064.2
129900 SUB-DELETE-F1-26-8.                                              NC1064.2
130000     PERFORM DE-LETE.                                             NC1064.2
130100 SUB-WRITE-F1-26-8.                                               NC1064.2
130200     MOVE "SUB-TEST-F1-26-8 " TO PAR-NAME.                        NC1064.2
130300     PERFORM PRINT-DETAIL.                                        NC1064.2
130400 SUB-TEST-F1-26-9.                                                NC1064.2
130500     IF DNAME30 EQUAL TO ZERO                                     NC1064.2
130600         PERFORM PASS                                             NC1064.2
130700         GO TO SUB-WRITE-F1-26-9.                                 NC1064.2
130800     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
130900     MOVE DNAME30 TO COMPUTED-18V0.                               NC1064.2
131000     PERFORM FAIL.                                                NC1064.2
131100     GO TO SUB-WRITE-F1-26-9.                                     NC1064.2
131200 SUB-DELETE-F1-26-9.                                              NC1064.2
131300     PERFORM DE-LETE.                                             NC1064.2
131400 SUB-WRITE-F1-26-9.                                               NC1064.2
131500     MOVE "SUB-TEST-F1-26-9 " TO PAR-NAME.                        NC1064.2
131600     PERFORM PRINT-DETAIL.                                        NC1064.2
131700 SUB-TEST-F1-26-10.                                               NC1064.2
131800     IF DNAME31 EQUAL TO ZERO                                     NC1064.2
131900         PERFORM PASS                                             NC1064.2
132000         GO TO SUB-WRITE-F1-26-10.                                NC1064.2
132100     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
132200     MOVE DNAME31 TO COMPUTED-18V0.                               NC1064.2
132300     PERFORM FAIL.                                                NC1064.2
132400     GO TO SUB-WRITE-F1-26-10.                                    NC1064.2
132500 SUB-DELETE-F1-26-10.                                             NC1064.2
132600     PERFORM DE-LETE.                                             NC1064.2
132700 SUB-WRITE-F1-26-10.                                              NC1064.2
132800     MOVE "SUB-TEST-F1-26-10 " TO PAR-NAME.                       NC1064.2
132900     PERFORM PRINT-DETAIL.                                        NC1064.2
133000 SUB-TEST-F1-26-11.                                               NC1064.2
133100     IF DNAME32 EQUAL TO ZERO                                     NC1064.2
133200         PERFORM PASS                                             NC1064.2
133300         GO TO SUB-WRITE-F1-26-11.                                NC1064.2
133400     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
133500     MOVE DNAME32 TO COMPUTED-18V0.                               NC1064.2
133600     PERFORM FAIL.                                                NC1064.2
133700     GO TO SUB-WRITE-F1-26-11.                                    NC1064.2
133800 SUB-DELETE-F1-26-11.                                             NC1064.2
133900     PERFORM DE-LETE.                                             NC1064.2
134000 SUB-WRITE-F1-26-11.                                              NC1064.2
134100     MOVE "SUB-TEST-F1-26-11 " TO PAR-NAME.                       NC1064.2
134200     PERFORM PRINT-DETAIL.                                        NC1064.2
134300 SUB-TEST-F1-26-12.                                               NC1064.2
134400     IF DNAME33 EQUAL TO ZERO                                     NC1064.2
134500         PERFORM PASS                                             NC1064.2
134600         GO TO SUB-WRITE-F1-26-12.                                NC1064.2
134700     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
134800     MOVE DNAME33 TO COMPUTED-18V0.                               NC1064.2
134900     PERFORM FAIL.                                                NC1064.2
135000     GO TO SUB-WRITE-F1-26-12.                                    NC1064.2
135100 SUB-DELETE-F1-26-12.                                             NC1064.2
135200     PERFORM DE-LETE.                                             NC1064.2
135300 SUB-WRITE-F1-26-12.                                              NC1064.2
135400     MOVE "SUB-TEST-F1-26-12 " TO PAR-NAME.                       NC1064.2
135500     PERFORM PRINT-DETAIL.                                        NC1064.2
135600 SUB-TEST-F1-26-13.                                               NC1064.2
135700     IF DNAME34 EQUAL TO ZERO                                     NC1064.2
135800         PERFORM PASS                                             NC1064.2
135900         GO TO SUB-WRITE-F1-26-13.                                NC1064.2
136000     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
136100     MOVE DNAME34 TO COMPUTED-18V0.                               NC1064.2
136200     PERFORM FAIL.                                                NC1064.2
136300     GO TO SUB-WRITE-F1-26-13.                                    NC1064.2
136400 SUB-DELETE-F1-26-13.                                             NC1064.2
136500     PERFORM DE-LETE.                                             NC1064.2
136600 SUB-WRITE-F1-26-13.                                              NC1064.2
136700     MOVE "SUB-TEST-F1-26-13 " TO PAR-NAME.                       NC1064.2
136800     PERFORM PRINT-DETAIL.                                        NC1064.2
136900 SUB-TEST-F1-26-14.                                               NC1064.2
137000     IF DNAME35 EQUAL TO ZERO                                     NC1064.2
137100         PERFORM PASS                                             NC1064.2
137200         GO TO SUB-WRITE-F1-26-14.                                NC1064.2
137300     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
137400     MOVE DNAME35 TO COMPUTED-18V0.                               NC1064.2
137500     PERFORM FAIL.                                                NC1064.2
137600     GO TO SUB-WRITE-F1-26-14.                                    NC1064.2
137700 SUB-DELETE-F1-26-14.                                             NC1064.2
137800     PERFORM DE-LETE.                                             NC1064.2
137900 SUB-WRITE-F1-26-14.                                              NC1064.2
138000     MOVE "SUB-TEST-F1-26-14 " TO PAR-NAME.                       NC1064.2
138100     PERFORM PRINT-DETAIL.                                        NC1064.2
138200 SUB-TEST-F1-26-15.                                               NC1064.2
138300     IF DNAME36 EQUAL TO ZERO                                     NC1064.2
138400         PERFORM PASS                                             NC1064.2
138500         GO TO SUB-WRITE-F1-26-15.                                NC1064.2
138600     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
138700     MOVE DNAME36 TO COMPUTED-18V0.                               NC1064.2
138800     PERFORM FAIL.                                                NC1064.2
138900     GO TO SUB-WRITE-F1-26-15.                                    NC1064.2
139000 SUB-DELETE-F1-26-15.                                             NC1064.2
139100     PERFORM DE-LETE.                                             NC1064.2
139200 SUB-WRITE-F1-26-15.                                              NC1064.2
139300     MOVE "SUB-TEST-F1-26-15 " TO PAR-NAME.                       NC1064.2
139400     PERFORM PRINT-DETAIL.                                        NC1064.2
139500 SUB-TEST-F1-26-16.                                               NC1064.2
139600     IF DNAME37 EQUAL TO ZERO                                     NC1064.2
139700         PERFORM PASS                                             NC1064.2
139800         GO TO SUB-WRITE-F1-26-16.                                NC1064.2
139900     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
140000     MOVE DNAME37 TO COMPUTED-18V0.                               NC1064.2
140100     PERFORM FAIL.                                                NC1064.2
140200     GO TO SUB-WRITE-F1-26-16.                                    NC1064.2
140300 SUB-DELETE-F1-26-16.                                             NC1064.2
140400     PERFORM DE-LETE.                                             NC1064.2
140500 SUB-WRITE-F1-26-16.                                              NC1064.2
140600     MOVE "SUB-TEST-F1-26-16 " TO PAR-NAME.                       NC1064.2
140700     PERFORM PRINT-DETAIL.                                        NC1064.2
140800 SUB-TEST-F1-26-17.                                               NC1064.2
140900     IF DNAME38 EQUAL TO ZERO                                     NC1064.2
141000         PERFORM PASS                                             NC1064.2
141100         GO TO SUB-WRITE-F1-26-17.                                NC1064.2
141200     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
141300     MOVE DNAME38 TO COMPUTED-18V0.                               NC1064.2
141400     PERFORM FAIL.                                                NC1064.2
141500     GO TO SUB-WRITE-F1-26-17.                                    NC1064.2
141600 SUB-DELETE-F1-26-17.                                             NC1064.2
141700     PERFORM DE-LETE.                                             NC1064.2
141800 SUB-WRITE-F1-26-17.                                              NC1064.2
141900     MOVE "SUB-TEST-F1-26-17 " TO PAR-NAME.                       NC1064.2
142000     PERFORM PRINT-DETAIL.                                        NC1064.2
142100 SUB-TEST-F1-26-18.                                               NC1064.2
142200     IF DNAME39 EQUAL TO ZERO                                     NC1064.2
142300         PERFORM PASS                                             NC1064.2
142400         GO TO SUB-WRITE-F1-26-18.                                NC1064.2
142500     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
142600     MOVE DNAME39 TO COMPUTED-18V0.                               NC1064.2
142700     PERFORM FAIL.                                                NC1064.2
142800     GO TO SUB-WRITE-F1-26-18.                                    NC1064.2
142900 SUB-DELETE-F1-26-18.                                             NC1064.2
143000     PERFORM DE-LETE.                                             NC1064.2
143100 SUB-WRITE-F1-26-18.                                              NC1064.2
143200     MOVE "SUB-TEST-F1-26-18 " TO PAR-NAME.                       NC1064.2
143300     PERFORM PRINT-DETAIL.                                        NC1064.2
143400 SUB-TEST-F1-26-19.                                               NC1064.2
143500     IF DNAME40 EQUAL TO ZERO                                     NC1064.2
143600         PERFORM PASS                                             NC1064.2
143700         GO TO SUB-WRITE-F1-26-19.                                NC1064.2
143800     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
143900     MOVE DNAME40 TO COMPUTED-18V0.                               NC1064.2
144000     PERFORM FAIL.                                                NC1064.2
144100     GO TO SUB-WRITE-F1-26-19.                                    NC1064.2
144200 SUB-DELETE-F1-26-19.                                             NC1064.2
144300     PERFORM DE-LETE.                                             NC1064.2
144400 SUB-WRITE-F1-26-19.                                              NC1064.2
144500     MOVE "SUB-TEST-F1-26-19 " TO PAR-NAME.                       NC1064.2
144600     PERFORM PRINT-DETAIL.                                        NC1064.2
144700 SUB-TEST-F1-26-20.                                               NC1064.2
144800     IF DNAME41 EQUAL TO ZERO                                     NC1064.2
144900     PERFORM PASS                                                 NC1064.2
145000         GO TO SUB-WRITE-F1-26-20.                                NC1064.2
145100     MOVE DNAME41 TO COMPUTED-18V0.                               NC1064.2
145200     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
145300     PERFORM FAIL.                                                NC1064.2
145400     GO TO SUB-WRITE-F1-26-20.                                    NC1064.2
145500 SUB-DELETE-F1-26-20.                                             NC1064.2
145600     PERFORM DE-LETE.                                             NC1064.2
145700 SUB-WRITE-F1-26-20.                                              NC1064.2
145800     MOVE "SUB-TEST-F1-26-20 " TO PAR-NAME.                       NC1064.2
145900     PERFORM PRINT-DETAIL.                                        NC1064.2
146000 SUB-TEST-F1-26-21.                                               NC1064.2
146100     IF DNAME42 EQUAL TO ZERO                                     NC1064.2
146200     PERFORM PASS                                                 NC1064.2
146300         GO TO SUB-WRITE-F1-26-21.                                NC1064.2
146400     MOVE DNAME42 TO COMPUTED-18V0.                               NC1064.2
146500     MOVE ZERO TO CORRECT-18V0.                                   NC1064.2
146600     PERFORM FAIL.                                                NC1064.2
146700     GO TO SUB-WRITE-F1-26-21.                                    NC1064.2
146800 SUB-DELETE-F1-26-21.                                             NC1064.2
146900     PERFORM DE-LETE.                                             NC1064.2
147000 SUB-WRITE-F1-26-21.                                              NC1064.2
147100     MOVE "SUB-TEST-F1-26-21 " TO PAR-NAME.                       NC1064.2
147200     PERFORM PRINT-DETAIL.                                        NC1064.2
147300*                                                                 NC1064.2
147400 SUB-INIT-F1-27.                                                  NC1064.2
147500*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
147600     MOVE   "VI-134 6.25.4 GR1" TO ANSI-REFERENCE.                NC1064.2
147700     MOVE   "SUB-TEST-F1-27" TO PAR-NAME.                         NC1064.2
147800     MOVE    ZERO  TO REC-CT.                                     NC1064.2
147900     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
148000     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
148100     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
148200     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
148300     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
148400     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
148500     MOVE   -9999999999999999.99 TO WRK-DS-16V2-1.                NC1064.2
148600     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
148700 SUB-TEST-F1-27-0.                                                NC1064.2
148800     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
148900         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
149000         ROUNDED WRK-DU-16V2-1.                                   NC1064.2
149100     GO TO   SUB-TEST-F1-27-1.                                    NC1064.2
149200 SUB-DELETE-F1-27.                                                NC1064.2
149300     PERFORM DE-LETE.                                             NC1064.2
149400     PERFORM PRINT-DETAIL.                                        NC1064.2
149500     GO TO   SUB-INIT-F1-28.                                      NC1064.2
149600 SUB-TEST-F1-27-1.                                                NC1064.2
149700     MOVE    "SUB-TEST-F1-27-1" TO PAR-NAME.                      NC1064.2
149800     MOVE 1 TO REC-CT.                                            NC1064.2
149900     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
150000     ELSE                                                         NC1064.2
150100     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
150200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
150300     ADD 1 TO REC-CT.                                             NC1064.2
150400 SUB-TEST-F1-27-2.                                                NC1064.2
150500     MOVE    "SUB-TEST-F1-27-2" TO PAR-NAME.                      NC1064.2
150600     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
150700     ELSE                                                         NC1064.2
150800     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
150900     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
151000     ADD 1 TO REC-CT.                                             NC1064.2
151100 SUB-TEST-F1-27-3.                                                NC1064.2
151200     MOVE    "SUB-TEST-F1-27-3" TO PAR-NAME.                      NC1064.2
151300     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
151400     ELSE                                                         NC1064.2
151500     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
151600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
151700     ADD 1 TO REC-CT.                                             NC1064.2
151800 SUB-TEST-F1-27-4.                                                NC1064.2
151900     MOVE    "SUB-TEST-F1-27-4" TO PAR-NAME.                      NC1064.2
152000     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
152100     ELSE                                                         NC1064.2
152200     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
152300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
152400     ADD 1 TO REC-CT.                                             NC1064.2
152500 SUB-TEST-F1-27-5.                                                NC1064.2
152600     MOVE    "SUB-TEST-F1-27-5" TO PAR-NAME.                      NC1064.2
152700     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
152800     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
152900     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
153000     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
153100     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
153200*                                                                 NC1064.2
153300 SUB-INIT-F1-28.                                                  NC1064.2
153400*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
153500*    ==-->      NO SIZE ERROR       <--==                         NC1064.2
153600     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1064.2
153700     MOVE   "SUB-TEST-F1-28" TO PAR-NAME.                         NC1064.2
153800     MOVE    ZERO  TO REC-CT.                                     NC1064.2
153900     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
154000     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
154100     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
154200     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
154300     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
154400     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
154500     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
154600     MOVE   -8888888888888888.88 TO WRK-DS-16V2-1.                NC1064.2
154700     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
154800 SUB-TEST-F1-28-0.                                                NC1064.2
154900     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
155000         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
155100         ROUNDED WRK-DU-16V2-1 WRK-DS-16V2-1                      NC1064.2
155200          ON SIZE ERROR                                           NC1064.2
155300             MOVE "A" TO SIZE-ERR2.                               NC1064.2
155400     GO TO   SUB-TEST-F1-28-1.                                    NC1064.2
155500 SUB-DELETE-F1-28.                                                NC1064.2
155600     PERFORM DE-LETE.                                             NC1064.2
155700     PERFORM PRINT-DETAIL.                                        NC1064.2
155800     GO TO   SUB-INIT-F1-29.                                      NC1064.2
155900 SUB-TEST-F1-28-1.                                                NC1064.2
156000     MOVE    "SUB-TEST-F1-28-1" TO PAR-NAME.                      NC1064.2
156100     MOVE 1 TO REC-CT.                                            NC1064.2
156200     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
156300     ELSE                                                         NC1064.2
156400     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
156500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
156600     ADD 1 TO REC-CT.                                             NC1064.2
156700 SUB-TEST-F1-28-2.                                                NC1064.2
156800     MOVE    "SUB-TEST-F1-28-2" TO PAR-NAME.                      NC1064.2
156900     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
157000     ELSE                                                         NC1064.2
157100     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
157200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
157300     ADD 1 TO REC-CT.                                             NC1064.2
157400 SUB-TEST-F1-28-3.                                                NC1064.2
157500     MOVE    "SUB-TEST-F1-28-3" TO PAR-NAME.                      NC1064.2
157600     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
157700     ELSE                                                         NC1064.2
157800     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
157900     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
158000     ADD 1 TO REC-CT.                                             NC1064.2
158100 SUB-TEST-F1-28-4.                                                NC1064.2
158200     MOVE    "SUB-TEST-F1-28-4" TO PAR-NAME.                      NC1064.2
158300     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
158400     ELSE                                                         NC1064.2
158500     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
158600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
158700     ADD 1 TO REC-CT.                                             NC1064.2
158800 SUB-TEST-F1-28-5.                                                NC1064.2
158900     MOVE    "SUB-TEST-F1-28-5" TO PAR-NAME.                      NC1064.2
159000     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
159100     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
159200     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
159300     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
159400     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
159500     ADD 1 TO REC-CT.                                             NC1064.2
159600 SUB-TEST-F1-28-6.                                                NC1064.2
159700     MOVE    "SUB-TEST-F1-28-6" TO PAR-NAME.                      NC1064.2
159800     IF WRK-DS-16V2-1 = -8888888888888901.22 PERFORM PASS         NC1064.2
159900     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
160000     PERFORM FAIL MOVE WRK-DS-16V2-1 TO WRK-NE-X-2                NC1064.2
160100     MOVE WRK-NE-X-2 TO COMPUTED-A                                NC1064.2
160200     MOVE "-8888888888888901.22" TO CORRECT-A                     NC1064.2
160300     PERFORM PRINT-DETAIL.                                        NC1064.2
160400     ADD 1 TO REC-CT.                                             NC1064.2
160500 SUB-TEST-F1-28-7.                                                NC1064.2
160600     MOVE   "SUB-TEST-F1-28-7" TO PAR-NAME.                       NC1064.2
160700     IF      SIZE-ERR2 = SPACE                                    NC1064.2
160800             PERFORM PASS                                         NC1064.2
160900             PERFORM PRINT-DETAIL                                 NC1064.2
161000     ELSE                                                         NC1064.2
161100             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1064.2
161200                  TO RE-MARK                                      NC1064.2
161300             MOVE    SPACE     TO CORRECT-X                       NC1064.2
161400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
161500             PERFORM FAIL                                         NC1064.2
161600             PERFORM PRINT-DETAIL.                                NC1064.2
161700*                                                                 NC1064.2
161800 SUB-INIT-F1-29.                                                  NC1064.2
161900*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
162000*    ==-->       SIZE ERROR         <--==                         NC1064.2
162100     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1064.2
162200     MOVE   "SUB-TEST-F1-29" TO PAR-NAME.                         NC1064.2
162300     MOVE    ZERO  TO REC-CT.                                     NC1064.2
162400     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
162500     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
162600     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
162700     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
162800     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
162900     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
163000     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
163100     MOVE   -9999999999999999.99 TO WRK-DS-16V2-1.                NC1064.2
163200     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
163300 SUB-TEST-F1-29-0.                                                NC1064.2
163400     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
163500         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
163600         ROUNDED WRK-DU-16V2-1 WRK-DS-16V2-1                      NC1064.2
163700          ON SIZE ERROR                                           NC1064.2
163800             MOVE "A" TO SIZE-ERR2.                               NC1064.2
163900     GO TO   SUB-TEST-F1-29-1.                                    NC1064.2
164000 SUB-DELETE-F1-29.                                                NC1064.2
164100     PERFORM DE-LETE.                                             NC1064.2
164200     PERFORM PRINT-DETAIL.                                        NC1064.2
164300     GO TO   SUB-INIT-F1-30.                                      NC1064.2
164400 SUB-TEST-F1-29-1.                                                NC1064.2
164500     MOVE    "SUB-TEST-F1-29-1" TO PAR-NAME.                      NC1064.2
164600     MOVE 1 TO REC-CT.                                            NC1064.2
164700     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
164800     ELSE                                                         NC1064.2
164900     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
165000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
165100     ADD 1 TO REC-CT.                                             NC1064.2
165200 SUB-TEST-F1-29-2.                                                NC1064.2
165300     MOVE    "SUB-TEST-F1-29-2" TO PAR-NAME.                      NC1064.2
165400     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
165500     ELSE                                                         NC1064.2
165600     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
165700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
165800     ADD 1 TO REC-CT.                                             NC1064.2
165900 SUB-TEST-F1-29-3.                                                NC1064.2
166000     MOVE    "SUB-TEST-F1-29-3" TO PAR-NAME.                      NC1064.2
166100     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
166200     ELSE                                                         NC1064.2
166300     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
166400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
166500     ADD 1 TO REC-CT.                                             NC1064.2
166600 SUB-TEST-F1-29-4.                                                NC1064.2
166700     MOVE    "SUB-TEST-F1-29-4" TO PAR-NAME.                      NC1064.2
166800     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
166900     ELSE                                                         NC1064.2
167000     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
167100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
167200     ADD 1 TO REC-CT.                                             NC1064.2
167300 SUB-TEST-F1-29-5.                                                NC1064.2
167400     MOVE    "SUB-TEST-F1-29-5" TO PAR-NAME.                      NC1064.2
167500     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
167600     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
167700     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
167800     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
167900     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
168000     ADD 1 TO REC-CT.                                             NC1064.2
168100 SUB-TEST-F1-29-6.                                                NC1064.2
168200     MOVE    "SUB-TEST-F1-29-6" TO PAR-NAME.                      NC1064.2
168300     IF WRK-DS-16V2-1 = -9999999999999999.99 PERFORM PASS         NC1064.2
168400     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
168500     PERFORM FAIL MOVE WRK-DS-16V2-1 TO WRK-NE-X-2                NC1064.2
168600     MOVE WRK-NE-X-2 TO COMPUTED-A                                NC1064.2
168700     MOVE "-9999999999999999.99" TO CORRECT-A                     NC1064.2
168800     PERFORM PRINT-DETAIL.                                        NC1064.2
168900     ADD 1 TO REC-CT.                                             NC1064.2
169000 SUB-TEST-F1-29-7.                                                NC1064.2
169100     MOVE   "SUB-TEST-F1-29-7" TO PAR-NAME.                       NC1064.2
169200     IF      SIZE-ERR2 = "A"                                      NC1064.2
169300             PERFORM PASS                                         NC1064.2
169400             PERFORM PRINT-DETAIL                                 NC1064.2
169500     ELSE                                                         NC1064.2
169600             MOVE   "ON SIZE ERROR SHOULD HAVE OCCURED"           NC1064.2
169700                  TO RE-MARK                                      NC1064.2
169800             MOVE   "A"        TO CORRECT-X                       NC1064.2
169900             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
170000             PERFORM FAIL                                         NC1064.2
170100             PERFORM PRINT-DETAIL.                                NC1064.2
170200*                                                                 NC1064.2
170300 SUB-INIT-F1-30.                                                  NC1064.2
170400*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
170500*    ==-->      NO SIZE ERROR       <--==                         NC1064.2
170600     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1064.2
170700     MOVE   "SUB-TEST-F1-30" TO PAR-NAME.                         NC1064.2
170800     MOVE    ZERO  TO REC-CT.                                     NC1064.2
170900     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
171000     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
171100     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
171200     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
171300     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
171400     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
171500     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
171600     MOVE   -8888888888888888.88 TO WRK-DS-16V2-1.                NC1064.2
171700     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
171800 SUB-TEST-F1-30-0.                                                NC1064.2
171900     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
172000         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
172100         ROUNDED WRK-DU-16V2-1 WRK-DS-16V2-1                      NC1064.2
172200      NOT ON SIZE ERROR                                           NC1064.2
172300             MOVE "A" TO SIZE-ERR2.                               NC1064.2
172400     GO TO   SUB-TEST-F1-30-1.                                    NC1064.2
172500 SUB-DELETE-F1-30.                                                NC1064.2
172600     PERFORM DE-LETE.                                             NC1064.2
172700     PERFORM PRINT-DETAIL.                                        NC1064.2
172800     GO TO   SUB-INIT-F1-31.                                      NC1064.2
172900 SUB-TEST-F1-30-1.                                                NC1064.2
173000     MOVE    "SUB-TEST-F1-30-1" TO PAR-NAME.                      NC1064.2
173100     MOVE 1 TO REC-CT.                                            NC1064.2
173200     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
173300     ELSE                                                         NC1064.2
173400     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
173500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
173600     ADD 1 TO REC-CT.                                             NC1064.2
173700 SUB-TEST-F1-30-2.                                                NC1064.2
173800     MOVE    "SUB-TEST-F1-30-2" TO PAR-NAME.                      NC1064.2
173900     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
174000     ELSE                                                         NC1064.2
174100     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
174200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
174300     ADD 1 TO REC-CT.                                             NC1064.2
174400 SUB-TEST-F1-30-3.                                                NC1064.2
174500     MOVE    "SUB-TEST-F1-30-3" TO PAR-NAME.                      NC1064.2
174600     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
174700     ELSE                                                         NC1064.2
174800     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
174900     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
175000     ADD 1 TO REC-CT.                                             NC1064.2
175100 SUB-TEST-F1-30-4.                                                NC1064.2
175200     MOVE    "SUB-TEST-F1-30-4" TO PAR-NAME.                      NC1064.2
175300     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
175400     ELSE                                                         NC1064.2
175500     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
175600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
175700     ADD 1 TO REC-CT.                                             NC1064.2
175800 SUB-TEST-F1-30-5.                                                NC1064.2
175900     MOVE    "SUB-TEST-F1-30-5" TO PAR-NAME.                      NC1064.2
176000     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
176100     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
176200     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
176300     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
176400     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
176500     ADD 1 TO REC-CT.                                             NC1064.2
176600 SUB-TEST-F1-30-6.                                                NC1064.2
176700     MOVE    "SUB-TEST-F1-30-6" TO PAR-NAME.                      NC1064.2
176800     IF WRK-DS-16V2-1 = -8888888888888901.22 PERFORM PASS         NC1064.2
176900     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
177000     PERFORM FAIL MOVE WRK-DS-16V2-1 TO WRK-NE-X-2                NC1064.2
177100     MOVE WRK-NE-X-2 TO COMPUTED-A                                NC1064.2
177200     MOVE "-8888888888888901.22" TO CORRECT-A                     NC1064.2
177300     PERFORM PRINT-DETAIL.                                        NC1064.2
177400     ADD 1 TO REC-CT.                                             NC1064.2
177500 SUB-TEST-F1-30-7.                                                NC1064.2
177600     MOVE   "SUB-TEST-F1-30-7" TO PAR-NAME.                       NC1064.2
177700     IF      SIZE-ERR2 = "A"                                      NC1064.2
177800             PERFORM PASS                                         NC1064.2
177900             PERFORM PRINT-DETAIL                                 NC1064.2
178000     ELSE                                                         NC1064.2
178100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1064.2
178200                  TO RE-MARK                                      NC1064.2
178300             MOVE   "A"        TO CORRECT-X                       NC1064.2
178400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
178500             PERFORM FAIL                                         NC1064.2
178600             PERFORM PRINT-DETAIL.                                NC1064.2
178700*                                                                 NC1064.2
178800 SUB-INIT-F1-31.                                                  NC1064.2
178900*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
179000*    ==-->       SIZE ERROR         <--==                         NC1064.2
179100     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1064.2
179200     MOVE   "SUB-TEST-F1-31" TO PAR-NAME.                         NC1064.2
179300     MOVE    ZERO  TO REC-CT.                                     NC1064.2
179400     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
179500     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
179600     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
179700     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
179800     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
179900     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
180000     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
180100     MOVE   -9999999999999999.99 TO WRK-DS-16V2-1.                NC1064.2
180200     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
180300 SUB-TEST-F1-31-0.                                                NC1064.2
180400     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
180500         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
180600         ROUNDED WRK-DU-16V2-1 WRK-DS-16V2-1                      NC1064.2
180700      NOT ON SIZE ERROR                                           NC1064.2
180800             MOVE "A" TO SIZE-ERR2.                               NC1064.2
180900     GO TO   SUB-TEST-F1-31-1.                                    NC1064.2
181000 SUB-DELETE-F1-31.                                                NC1064.2
181100     PERFORM DE-LETE.                                             NC1064.2
181200     PERFORM PRINT-DETAIL.                                        NC1064.2
181300     GO TO   SUB-INIT-F1-32.                                      NC1064.2
181400 SUB-TEST-F1-31-1.                                                NC1064.2
181500     MOVE    "SUB-TEST-F1-31-1" TO PAR-NAME.                      NC1064.2
181600     MOVE 1 TO REC-CT.                                            NC1064.2
181700     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
181800     ELSE                                                         NC1064.2
181900     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
182000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
182100     ADD 1 TO REC-CT.                                             NC1064.2
182200 SUB-TEST-F1-31-2.                                                NC1064.2
182300     MOVE    "SUB-TEST-F1-31-2" TO PAR-NAME.                      NC1064.2
182400     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
182500     ELSE                                                         NC1064.2
182600     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
182700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
182800     ADD 1 TO REC-CT.                                             NC1064.2
182900 SUB-TEST-F1-31-3.                                                NC1064.2
183000     MOVE    "SUB-TEST-F1-31-3" TO PAR-NAME.                      NC1064.2
183100     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
183200     ELSE                                                         NC1064.2
183300     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
183400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
183500     ADD 1 TO REC-CT.                                             NC1064.2
183600 SUB-TEST-F1-31-4.                                                NC1064.2
183700     MOVE    "SUB-TEST-F1-31-4" TO PAR-NAME.                      NC1064.2
183800     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
183900     ELSE                                                         NC1064.2
184000     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
184100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
184200     ADD 1 TO REC-CT.                                             NC1064.2
184300 SUB-TEST-F1-31-5.                                                NC1064.2
184400     MOVE    "SUB-TEST-F1-31-5" TO PAR-NAME.                      NC1064.2
184500     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
184600     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
184700     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
184800     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
184900     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
185000     ADD 1 TO REC-CT.                                             NC1064.2
185100 SUB-TEST-F1-31-6.                                                NC1064.2
185200     MOVE    "SUB-TEST-F1-31-6" TO PAR-NAME.                      NC1064.2
185300     IF WRK-DS-16V2-1 = -9999999999999999.99 PERFORM PASS         NC1064.2
185400     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
185500     PERFORM FAIL MOVE WRK-DS-16V2-1 TO WRK-NE-X-2                NC1064.2
185600     MOVE WRK-NE-X-2 TO COMPUTED-A                                NC1064.2
185700     MOVE "-9999999999999999.99" TO CORRECT-A                     NC1064.2
185800     PERFORM PRINT-DETAIL.                                        NC1064.2
185900     ADD 1 TO REC-CT.                                             NC1064.2
186000 SUB-TEST-F1-31-7.                                                NC1064.2
186100     MOVE   "SUB-TEST-F1-31-7" TO PAR-NAME.                       NC1064.2
186200     IF      SIZE-ERR2 = SPACE                                    NC1064.2
186300             PERFORM PASS                                         NC1064.2
186400             PERFORM PRINT-DETAIL                                 NC1064.2
186500     ELSE                                                         NC1064.2
186600             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1064.2
186700                  TO RE-MARK                                      NC1064.2
186800             MOVE    SPACE     TO CORRECT-X                       NC1064.2
186900             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
187000             PERFORM FAIL                                         NC1064.2
187100             PERFORM PRINT-DETAIL.                                NC1064.2
187200*                                                                 NC1064.2
187300 SUB-INIT-F1-32.                                                  NC1064.2
187400*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
187500*    ==-->      NO SIZE ERROR       <--==                         NC1064.2
187600     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1064.2
187700     MOVE   "SUB-TEST-F1-32" TO PAR-NAME.                         NC1064.2
187800     MOVE    ZERO  TO REC-CT.                                     NC1064.2
187900     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
188000     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
188100     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
188200     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
188300     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
188400     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
188500     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
188600     MOVE   -8888888888888888.88 TO WRK-DS-16V2-1.                NC1064.2
188700     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
188800 SUB-TEST-F1-32-0.                                                NC1064.2
188900     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
189000         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
189100         ROUNDED WRK-DU-16V2-1 WRK-DS-16V2-1                      NC1064.2
189200          ON SIZE ERROR                                           NC1064.2
189300             MOVE "Z" TO SIZE-ERR2                                NC1064.2
189400      NOT ON SIZE ERROR                                           NC1064.2
189500             MOVE "A" TO SIZE-ERR2.                               NC1064.2
189600     GO TO   SUB-TEST-F1-32-1.                                    NC1064.2
189700 SUB-DELETE-F1-32.                                                NC1064.2
189800     PERFORM DE-LETE.                                             NC1064.2
189900     PERFORM PRINT-DETAIL.                                        NC1064.2
190000     GO TO   SUB-INIT-F1-33.                                      NC1064.2
190100 SUB-TEST-F1-32-1.                                                NC1064.2
190200     MOVE    "SUB-TEST-F1-32-1" TO PAR-NAME.                      NC1064.2
190300     MOVE 1 TO REC-CT.                                            NC1064.2
190400     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
190500     ELSE                                                         NC1064.2
190600     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
190700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
190800     ADD 1 TO REC-CT.                                             NC1064.2
190900 SUB-TEST-F1-32-2.                                                NC1064.2
191000     MOVE    "SUB-TEST-F1-32-2" TO PAR-NAME.                      NC1064.2
191100     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
191200     ELSE                                                         NC1064.2
191300     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
191400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
191500     ADD 1 TO REC-CT.                                             NC1064.2
191600 SUB-TEST-F1-32-3.                                                NC1064.2
191700     MOVE    "SUB-TEST-F1-32-3" TO PAR-NAME.                      NC1064.2
191800     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
191900     ELSE                                                         NC1064.2
192000     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
192100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
192200     ADD 1 TO REC-CT.                                             NC1064.2
192300 SUB-TEST-F1-32-4.                                                NC1064.2
192400     MOVE    "SUB-TEST-F1-32-4" TO PAR-NAME.                      NC1064.2
192500     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
192600     ELSE                                                         NC1064.2
192700     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
192800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
192900     ADD 1 TO REC-CT.                                             NC1064.2
193000 SUB-TEST-F1-32-5.                                                NC1064.2
193100     MOVE    "SUB-TEST-F1-32-5" TO PAR-NAME.                      NC1064.2
193200     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
193300     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
193400     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
193500     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
193600     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
193700     ADD 1 TO REC-CT.                                             NC1064.2
193800 SUB-TEST-F1-32-6.                                                NC1064.2
193900     MOVE    "SUB-TEST-F1-32-6" TO PAR-NAME.                      NC1064.2
194000     IF WRK-DS-16V2-1 = -8888888888888901.22 PERFORM PASS         NC1064.2
194100     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
194200     PERFORM FAIL MOVE WRK-DS-16V2-1 TO WRK-NE-X-2                NC1064.2
194300     MOVE WRK-NE-X-2 TO COMPUTED-A                                NC1064.2
194400     MOVE "-8888888888888901.22" TO CORRECT-A                     NC1064.2
194500     PERFORM PRINT-DETAIL.                                        NC1064.2
194600     ADD 1 TO REC-CT.                                             NC1064.2
194700 SUB-TEST-F1-32-7.                                                NC1064.2
194800     MOVE   "SUB-TEST-F1-32-7" TO PAR-NAME.                       NC1064.2
194900     IF      SIZE-ERR2 = "A"                                      NC1064.2
195000             PERFORM PASS                                         NC1064.2
195100             PERFORM PRINT-DETAIL                                 NC1064.2
195200     ELSE                                                         NC1064.2
195300             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1064.2
195400                  TO RE-MARK                                      NC1064.2
195500             MOVE   "A"        TO CORRECT-X                       NC1064.2
195600             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
195700             PERFORM FAIL                                         NC1064.2
195800             PERFORM PRINT-DETAIL.                                NC1064.2
195900*                                                                 NC1064.2
196000 SUB-INIT-F1-33.                                                  NC1064.2
196100*    ==-->  MULTIPLE RESULT FIELDS  <--==                         NC1064.2
196200*    ==-->       SIZE ERROR         <--==                         NC1064.2
196300     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1064.2
196400     MOVE   "SUB-TEST-F1-33" TO PAR-NAME.                         NC1064.2
196500     MOVE    ZERO  TO REC-CT.                                     NC1064.2
196600     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
196700     MOVE    10    TO WRK-DU-2V0-1.                               NC1064.2
196800     MOVE    .3    TO WRK-DU-0V1-1.                               NC1064.2
196900     MOVE    12.34 TO WRK-DU-2V2-1.                               NC1064.2
197000     MOVE    ZERO  TO WRK-DS-2V2-1.                               NC1064.2
197100     MOVE   -12.34 TO WRK-DS-2V2-2.                               NC1064.2
197200     MOVE    22.33 TO WRK-DU-2V2-2.                               NC1064.2
197300     MOVE   -9999999999999999.99 TO WRK-DS-16V2-1.                NC1064.2
197400     MOVE    9999999999999999.99 TO WRK-DU-16V2-1.                NC1064.2
197500 SUB-TEST-F1-33-0.                                                NC1064.2
197600     SUBTRACT WRK-DU-2V0-1 2 WRK-DU-0V1-1 .04 FROM WRK-DU-2V2-1   NC1064.2
197700         WRK-DS-2V2-1 ROUNDED WRK-DS-2V2-2 WRK-DU-2V2-2           NC1064.2
197800         ROUNDED WRK-DU-16V2-1 WRK-DS-16V2-1                      NC1064.2
197900          ON SIZE ERROR                                           NC1064.2
198000             MOVE "A" TO SIZE-ERR2                                NC1064.2
198100      NOT ON SIZE ERROR                                           NC1064.2
198200             MOVE "Z" TO SIZE-ERR2.                               NC1064.2
198300     GO TO   SUB-TEST-F1-33-1.                                    NC1064.2
198400 SUB-DELETE-F1-33.                                                NC1064.2
198500     PERFORM DE-LETE.                                             NC1064.2
198600     PERFORM PRINT-DETAIL.                                        NC1064.2
198700     GO TO   SUB-INIT-F1-34.                                      NC1064.2
198800 SUB-TEST-F1-33-1.                                                NC1064.2
198900     MOVE    "SUB-TEST-F1-33-1" TO PAR-NAME.                      NC1064.2
199000     MOVE 1 TO REC-CT.                                            NC1064.2
199100     IF WRK-DU-2V2-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1064.2
199200     ELSE                                                         NC1064.2
199300     PERFORM FAIL MOVE WRK-DU-2V2-1 TO COMPUTED-N MOVE ZERO       NC1064.2
199400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
199500     ADD 1 TO REC-CT.                                             NC1064.2
199600 SUB-TEST-F1-33-2.                                                NC1064.2
199700     MOVE    "SUB-TEST-F1-33-2" TO PAR-NAME.                      NC1064.2
199800     IF WRK-DS-2V2-1 = -12.34 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
199900     ELSE                                                         NC1064.2
200000     PERFORM FAIL MOVE WRK-DS-2V2-1 TO COMPUTED-N MOVE -12.34     NC1064.2
200100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
200200     ADD 1 TO REC-CT.                                             NC1064.2
200300 SUB-TEST-F1-33-3.                                                NC1064.2
200400     MOVE    "SUB-TEST-F1-33-3" TO PAR-NAME.                      NC1064.2
200500     IF WRK-DS-2V2-2 = -24.68 PERFORM PASS PERFORM PRINT-DETAIL   NC1064.2
200600     ELSE                                                         NC1064.2
200700     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE -24.68     NC1064.2
200800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
200900     ADD 1 TO REC-CT.                                             NC1064.2
201000 SUB-TEST-F1-33-4.                                                NC1064.2
201100     MOVE    "SUB-TEST-F1-33-4" TO PAR-NAME.                      NC1064.2
201200     IF WRK-DU-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1064.2
201300     ELSE                                                         NC1064.2
201400     PERFORM FAIL MOVE WRK-DU-2V2-2 TO COMPUTED-N MOVE 09.99      NC1064.2
201500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1064.2
201600     ADD 1 TO REC-CT.                                             NC1064.2
201700 SUB-TEST-F1-33-5.                                                NC1064.2
201800     MOVE    "SUB-TEST-F1-33-5" TO PAR-NAME.                      NC1064.2
201900     IF WRK-DU-16V2-1 = 9999999999999987.65 PERFORM PASS          NC1064.2
202000     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
202100     PERFORM FAIL MOVE WRK-DU-16V2-1 TO WRK-NE-X-1                NC1064.2
202200     MOVE WRK-NE-X-1 TO COMPUTED-A                                NC1064.2
202300     MOVE "9999999999999987.65" TO CORRECT-A PERFORM PRINT-DETAIL.NC1064.2
202400     ADD 1 TO REC-CT.                                             NC1064.2
202500 SUB-TEST-F1-33-6.                                                NC1064.2
202600     MOVE    "SUB-TEST-F1-33-6" TO PAR-NAME.                      NC1064.2
202700     IF WRK-DS-16V2-1 = -9999999999999999.99 PERFORM PASS         NC1064.2
202800     PERFORM PRINT-DETAIL ELSE                                    NC1064.2
202900     PERFORM FAIL MOVE WRK-DS-16V2-1 TO WRK-NE-X-2                NC1064.2
203000     MOVE WRK-NE-X-2 TO COMPUTED-A                                NC1064.2
203100     MOVE "-9999999999999999.99" TO CORRECT-A                     NC1064.2
203200     PERFORM PRINT-DETAIL.                                        NC1064.2
203300     ADD 1 TO REC-CT.                                             NC1064.2
203400 SUB-TEST-F1-33-7.                                                NC1064.2
203500     MOVE   "SUB-TEST-F1-33-7" TO PAR-NAME.                       NC1064.2
203600     IF      SIZE-ERR2 = "A"                                      NC1064.2
203700             PERFORM PASS                                         NC1064.2
203800             PERFORM PRINT-DETAIL                                 NC1064.2
203900     ELSE                                                         NC1064.2
204000             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1064.2
204100                  TO RE-MARK                                      NC1064.2
204200             MOVE   "A"        TO CORRECT-X                       NC1064.2
204300             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
204400             PERFORM FAIL                                         NC1064.2
204500             PERFORM PRINT-DETAIL.                                NC1064.2
204600*                                                                 NC1064.2
204700 SUB-INIT-F1-34.                                                  NC1064.2
204800*    ==-->  EXPLICIT SCOPE TERMINATOR <--==                       NC1064.2
204900*    ==-->          SIZE ERROR        <--==                       NC1064.2
205000     MOVE   "IV-41 6.4.3"       TO ANSI-REFERENCE.                NC1064.2
205100     MOVE     -11 TO WRK-DS-02V00.                                NC1064.2
205200     MOVE    SPACE TO WRK-XN-00001.                               NC1064.2
205300     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
205400     MOVE    SPACE TO SIZE-ERR3.                                  NC1064.2
205500     MOVE    SPACE TO SIZE-ERR4.                                  NC1064.2
205600     MOVE    1  TO REC-CT.                                        NC1064.2
205700 SUB-TEST-F1-34-0.                                                NC1064.2
205800     SUBTRACT A99-DS-02V00 FROM WRK-DS-02V00                      NC1064.2
205900          ON SIZE ERROR                                           NC1064.2
206000             MOVE   "1" TO WRK-XN-00001                           NC1064.2
206100             MOVE   "A" TO SIZE-ERR2                              NC1064.2
206200             MOVE   "B" TO SIZE-ERR3                              NC1064.2
206300     END-SUBTRACT                                                 NC1064.2
206400     MOVE   "C" TO SIZE-ERR4.                                     NC1064.2
206500     GO TO   SUB-TEST-F1-34-1.                                    NC1064.2
206600 SUB-DELETE-F1-34.                                                NC1064.2
206700     PERFORM DE-LETE.                                             NC1064.2
206800     PERFORM PRINT-DETAIL.                                        NC1064.2
206900     GO TO   SUB-INIT-F1-35.                                      NC1064.2
207000 SUB-TEST-F1-34-1.                                                NC1064.2
207100     MOVE   "SUB-TEST-F1-34-1" TO PAR-NAME.                       NC1064.2
207200     IF      WRK-XN-00001 = "1"                                   NC1064.2
207300             PERFORM PASS                                         NC1064.2
207400             PERFORM PRINT-DETAIL                                 NC1064.2
207500     ELSE                                                         NC1064.2
207600             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1064.2
207700                  TO RE-MARK                                      NC1064.2
207800             MOVE   "1"           TO CORRECT-X                    NC1064.2
207900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1064.2
208000             PERFORM FAIL                                         NC1064.2
208100             PERFORM PRINT-DETAIL.                                NC1064.2
208200     ADD     1 TO REC-CT.                                         NC1064.2
208300 SUB-TEST-F1-34-2.                                                NC1064.2
208400     MOVE   "SUB-TEST-F1-34-2" TO PAR-NAME.                       NC1064.2
208500     IF      SIZE-ERR2 = "A"                                      NC1064.2
208600             PERFORM PASS                                         NC1064.2
208700             PERFORM PRINT-DETAIL                                 NC1064.2
208800     ELSE                                                         NC1064.2
208900             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1064.2
209000                  TO RE-MARK                                      NC1064.2
209100             MOVE   "A"        TO CORRECT-X                       NC1064.2
209200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
209300             PERFORM FAIL                                         NC1064.2
209400             PERFORM PRINT-DETAIL.                                NC1064.2
209500     ADD     1 TO REC-CT.                                         NC1064.2
209600 SUB-TEST-F1-34-3.                                                NC1064.2
209700     MOVE   "SUB-TEST-F1-34-3" TO PAR-NAME.                       NC1064.2
209800     IF      SIZE-ERR3 = "B"                                      NC1064.2
209900             PERFORM PASS                                         NC1064.2
210000             PERFORM PRINT-DETAIL                                 NC1064.2
210100     ELSE                                                         NC1064.2
210200             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1064.2
210300                  TO RE-MARK                                      NC1064.2
210400             MOVE   "B"        TO CORRECT-X                       NC1064.2
210500             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1064.2
210600             PERFORM FAIL                                         NC1064.2
210700             PERFORM PRINT-DETAIL.                                NC1064.2
210800     ADD     1 TO REC-CT.                                         NC1064.2
210900 SUB-TEST-F1-34-4.                                                NC1064.2
211000     MOVE   "SUB-TEST-F1-34-4" TO PAR-NAME.                       NC1064.2
211100     IF      SIZE-ERR4 = "C"                                      NC1064.2
211200             PERFORM PASS                                         NC1064.2
211300             PERFORM PRINT-DETAIL                                 NC1064.2
211400     ELSE                                                         NC1064.2
211500             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1064.2
211600                  TO RE-MARK                                      NC1064.2
211700             MOVE   "C"        TO CORRECT-X                       NC1064.2
211800             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1064.2
211900             PERFORM FAIL                                         NC1064.2
212000             PERFORM PRINT-DETAIL.                                NC1064.2
212100     ADD     1 TO REC-CT.                                         NC1064.2
212200 SUB-TEST-F1-34-5.                                                NC1064.2
212300     MOVE   "SUB-TEST-F1-34-5" TO PAR-NAME.                       NC1064.2
212400     IF      WRK-DS-02V00 = -11                                   NC1064.2
212500             PERFORM PASS                                         NC1064.2
212600             PERFORM PRINT-DETAIL                                 NC1064.2
212700     ELSE                                                         NC1064.2
212800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1064.2
212900                  TO RE-MARK                                      NC1064.2
213000             MOVE    -11          TO CORRECT-N                    NC1064.2
213100             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1064.2
213200             PERFORM FAIL                                         NC1064.2
213300             PERFORM PRINT-DETAIL.                                NC1064.2
213400*                                                                 NC1064.2
213500 SUB-INIT-F1-35.                                                  NC1064.2
213600*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1064.2
213700     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1064.2
213800     MOVE   "SUB-TEST-F1-35"  TO PAR-NAME.                        NC1064.2
213900     MOVE    SPACE TO WRK-XN-00001.                               NC1064.2
214000     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
214100     MOVE    SPACE TO SIZE-ERR3.                                  NC1064.2
214200     MOVE    SPACE TO SIZE-ERR4.                                  NC1064.2
214300     MOVE    ZERO  TO WRK-DS-06V06.                               NC1064.2
214400     MOVE    1     TO REC-CT.                                     NC1064.2
214500 SUB-TEST-F1-35-0.                                                NC1064.2
214600     SUBTRACT A12THREES-DS-06V06                                  NC1064.2
214700              333333                                              NC1064.2
214800              A06THREES-DS-03V03                                  NC1064.2
214900              -0000009                                            NC1064.2
215000              FROM WRK-DS-06V06 ROUNDED                           NC1064.2
215100          ON SIZE ERROR                                           NC1064.2
215200             MOVE   "1" TO WRK-XN-00001                           NC1064.2
215300             MOVE   "A" TO SIZE-ERR2                              NC1064.2
215400             MOVE   "B" TO SIZE-ERR3                              NC1064.2
215500     END-SUBTRACT                                                 NC1064.2
215600     MOVE   "C" TO SIZE-ERR4.                                     NC1064.2
215700     GO TO   SUB-TEST-F1-35-1.                                    NC1064.2
215800 SUB-DELETE-F1-35.                                                NC1064.2
215900     PERFORM DE-LETE.                                             NC1064.2
216000     PERFORM PRINT-DETAIL.                                        NC1064.2
216100     GO TO   SUB-INIT-F1-36.                                      NC1064.2
216200 SUB-TEST-F1-35-1.                                                NC1064.2
216300     MOVE   "SUB-TEST-F1-35-1" TO PAR-NAME.                       NC1064.2
216400     IF      WRK-XN-00001 = SPACE                                 NC1064.2
216500             PERFORM PASS                                         NC1064.2
216600             PERFORM PRINT-DETAIL                                 NC1064.2
216700     ELSE                                                         NC1064.2
216800             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1064.2
216900                  TO RE-MARK                                      NC1064.2
217000             MOVE    SPACE        TO CORRECT-X                    NC1064.2
217100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1064.2
217200             PERFORM FAIL                                         NC1064.2
217300             PERFORM PRINT-DETAIL.                                NC1064.2
217400     ADD     1 TO REC-CT.                                         NC1064.2
217500 SUB-TEST-F1-35-2.                                                NC1064.2
217600     MOVE   "SUB-TEST-F1-35-2" TO PAR-NAME.                       NC1064.2
217700     IF      SIZE-ERR2 = SPACE                                    NC1064.2
217800             PERFORM PASS                                         NC1064.2
217900             PERFORM PRINT-DETAIL                                 NC1064.2
218000     ELSE                                                         NC1064.2
218100             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1064.2
218200                  TO RE-MARK                                      NC1064.2
218300             MOVE    SPACE     TO CORRECT-X                       NC1064.2
218400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
218500             PERFORM FAIL                                         NC1064.2
218600             PERFORM PRINT-DETAIL.                                NC1064.2
218700     ADD     1 TO REC-CT.                                         NC1064.2
218800 SUB-TEST-F1-35-3.                                                NC1064.2
218900     MOVE   "SUB-TEST-F1-35-3" TO PAR-NAME.                       NC1064.2
219000     IF      SIZE-ERR3 = SPACE                                    NC1064.2
219100             PERFORM PASS                                         NC1064.2
219200             PERFORM PRINT-DETAIL                                 NC1064.2
219300     ELSE                                                         NC1064.2
219400             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1064.2
219500                  TO RE-MARK                                      NC1064.2
219600             MOVE    SPACE     TO CORRECT-X                       NC1064.2
219700             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1064.2
219800             PERFORM FAIL                                         NC1064.2
219900             PERFORM PRINT-DETAIL.                                NC1064.2
220000     ADD     1 TO REC-CT.                                         NC1064.2
220100 SUB-TEST-F1-35-4.                                                NC1064.2
220200     MOVE   "SUB-TEST-F1-35-4" TO PAR-NAME.                       NC1064.2
220300     IF      SIZE-ERR4 = "C"                                      NC1064.2
220400             PERFORM PASS                                         NC1064.2
220500             PERFORM PRINT-DETAIL                                 NC1064.2
220600     ELSE                                                         NC1064.2
220700             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1064.2
220800                  TO RE-MARK                                      NC1064.2
220900             MOVE   "C"        TO CORRECT-X                       NC1064.2
221000             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1064.2
221100             PERFORM FAIL                                         NC1064.2
221200             PERFORM PRINT-DETAIL.                                NC1064.2
221300     ADD     1 TO REC-CT.                                         NC1064.2
221400 SUB-TEST-F1-35-5.                                                NC1064.2
221500     MOVE   "SUB-TEST-F1-35-5" TO PAR-NAME.                       NC1064.2
221600     IF      WRK-DS-06V06 = -666990.666333                        NC1064.2
221700             PERFORM PASS                                         NC1064.2
221800             PERFORM PRINT-DETAIL                                 NC1064.2
221900     ELSE                                                         NC1064.2
222000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1064.2
222100                  TO RE-MARK                                      NC1064.2
222200             MOVE   -666990.666333 TO CORRECT-N                   NC1064.2
222300             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1064.2
222400             PERFORM FAIL                                         NC1064.2
222500             PERFORM PRINT-DETAIL.                                NC1064.2
222600*                                                                 NC1064.2
222700 SUB-INIT-F1-36.                                                  NC1064.2
222800*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1064.2
222900     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1064.2
223000     MOVE   "SUB-TEST-F1-36"  TO PAR-NAME.                        NC1064.2
223100     MOVE   SPACE TO WRK-XN-00001.                                NC1064.2
223200     MOVE   SPACE TO SIZE-ERR2.                                   NC1064.2
223300     MOVE   SPACE TO SIZE-ERR3.                                   NC1064.2
223400     MOVE   SPACE TO SIZE-ERR4.                                   NC1064.2
223500     MOVE   -11   TO WRK-DS-02V00.                                NC1064.2
223600     MOVE       1 TO REC-CT.                                      NC1064.2
223700 SUB-TEST-F1-36-0.                                                NC1064.2
223800     SUBTRACT A99-DS-02V00 FROM WRK-DS-02V00                      NC1064.2
223900         NOT ON SIZE ERROR                                        NC1064.2
224000             MOVE   "1" TO WRK-XN-00001                           NC1064.2
224100             MOVE   "A" TO SIZE-ERR2                              NC1064.2
224200             MOVE   "B" TO SIZE-ERR3                              NC1064.2
224300     END-SUBTRACT                                                 NC1064.2
224400     MOVE   "C" TO SIZE-ERR4.                                     NC1064.2
224500     GO TO   SUB-TEST-F1-36-1.                                    NC1064.2
224600 SUB-DELETE-F1-36.                                                NC1064.2
224700     PERFORM DE-LETE.                                             NC1064.2
224800     PERFORM PRINT-DETAIL.                                        NC1064.2
224900     GO      TO SUB-INIT-F1-37.                                   NC1064.2
225000 SUB-TEST-F1-36-1.                                                NC1064.2
225100     MOVE   "SUB-TEST-F1-36-1" TO PAR-NAME.                       NC1064.2
225200     IF      WRK-XN-00001 = SPACE                                 NC1064.2
225300             PERFORM PASS                                         NC1064.2
225400             PERFORM PRINT-DETAIL                                 NC1064.2
225500     ELSE                                                         NC1064.2
225600             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1064.2
225700                  TO RE-MARK                                      NC1064.2
225800             MOVE    SPACE        TO CORRECT-X                    NC1064.2
225900             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1064.2
226000             PERFORM FAIL                                         NC1064.2
226100             PERFORM PRINT-DETAIL.                                NC1064.2
226200     ADD     1 TO REC-CT.                                         NC1064.2
226300 SUB-TEST-F1-36-2.                                                NC1064.2
226400     MOVE   "SUB-TEST-F1-36-2" TO PAR-NAME.                       NC1064.2
226500     IF      SIZE-ERR2 = SPACE                                    NC1064.2
226600             PERFORM PASS                                         NC1064.2
226700             PERFORM PRINT-DETAIL                                 NC1064.2
226800     ELSE                                                         NC1064.2
226900             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1064.2
227000                  TO RE-MARK                                      NC1064.2
227100             MOVE    SPACE     TO CORRECT-X                       NC1064.2
227200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
227300             PERFORM FAIL                                         NC1064.2
227400             PERFORM PRINT-DETAIL.                                NC1064.2
227500     ADD     1 TO REC-CT.                                         NC1064.2
227600 SUB-TEST-F1-36-3.                                                NC1064.2
227700     MOVE   "SUB-TEST-F1-36-3" TO PAR-NAME.                       NC1064.2
227800     IF      SIZE-ERR3 = SPACE                                    NC1064.2
227900             PERFORM PASS                                         NC1064.2
228000             PERFORM PRINT-DETAIL                                 NC1064.2
228100     ELSE                                                         NC1064.2
228200             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1064.2
228300                  TO RE-MARK                                      NC1064.2
228400             MOVE    SPACE     TO CORRECT-X                       NC1064.2
228500             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1064.2
228600             PERFORM FAIL                                         NC1064.2
228700             PERFORM PRINT-DETAIL                                 NC1064.2
228800     ADD     1 TO REC-CT.                                         NC1064.2
228900 SUB-TEST-F1-36-4.                                                NC1064.2
229000     MOVE   "SUB-TEST-F1-36-4" TO PAR-NAME.                       NC1064.2
229100     IF      SIZE-ERR4 = "C"                                      NC1064.2
229200             PERFORM PASS                                         NC1064.2
229300             PERFORM PRINT-DETAIL                                 NC1064.2
229400     ELSE                                                         NC1064.2
229500             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1064.2
229600                  TO RE-MARK                                      NC1064.2
229700             MOVE   "C"        TO CORRECT-X                       NC1064.2
229800             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1064.2
229900             PERFORM FAIL                                         NC1064.2
230000             PERFORM PRINT-DETAIL.                                NC1064.2
230100     ADD     1 TO REC-CT.                                         NC1064.2
230200 SUB-TEST-F1-36-5.                                                NC1064.2
230300     MOVE   "SUB-TEST-F1-36-5" TO PAR-NAME.                       NC1064.2
230400     IF      WRK-DS-02V00 = -11                                   NC1064.2
230500             PERFORM PASS                                         NC1064.2
230600             PERFORM PRINT-DETAIL                                 NC1064.2
230700     ELSE                                                         NC1064.2
230800             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1064.2
230900                  TO RE-MARK                                      NC1064.2
231000             MOVE    -11          TO CORRECT-N                    NC1064.2
231100             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1064.2
231200             PERFORM FAIL                                         NC1064.2
231300             PERFORM PRINT-DETAIL.                                NC1064.2
231400*                                                                 NC1064.2
231500 SUB-INIT-F1-37.                                                  NC1064.2
231600*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1064.2
231700     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1064.2
231800     MOVE   "SUB-TEST-F1-37"  TO PAR-NAME.                        NC1064.2
231900     MOVE    SPACE TO WRK-XN-00001.                               NC1064.2
232000     MOVE    SPACE TO SIZE-ERR2.                                  NC1064.2
232100     MOVE    SPACE TO SIZE-ERR3.                                  NC1064.2
232200     MOVE    SPACE TO SIZE-ERR4.                                  NC1064.2
232300     MOVE    ZERO  TO WRK-DS-06V06.                               NC1064.2
232400     MOVE    1     TO REC-CT.                                     NC1064.2
232500 SUB-TEST-F1-37-0.                                                NC1064.2
232600     SUBTRACT A12THREES-DS-06V06                                  NC1064.2
232700              333333                                              NC1064.2
232800              A06THREES-DS-03V03                                  NC1064.2
232900              -0000009                                            NC1064.2
233000              FROM WRK-DS-06V06 ROUNDED                           NC1064.2
233100         NOT ON SIZE ERROR                                        NC1064.2
233200             MOVE   "1" TO WRK-XN-00001                           NC1064.2
233300             MOVE   "A" TO SIZE-ERR2                              NC1064.2
233400             MOVE   "B" TO SIZE-ERR3                              NC1064.2
233500     END-SUBTRACT                                                 NC1064.2
233600     MOVE   "C" TO SIZE-ERR4.                                     NC1064.2
233700     GO TO   SUB-TEST-F1-37-1.                                    NC1064.2
233800 SUB-DELETE-F1-37.                                                NC1064.2
233900     PERFORM DE-LETE.                                             NC1064.2
234000     PERFORM PRINT-DETAIL.                                        NC1064.2
234100     GO TO   SUB-INIT-F1-38.                                      NC1064.2
234200 SUB-TEST-F1-37-1.                                                NC1064.2
234300     MOVE   "SUB-TEST-F1-37-1" TO PAR-NAME.                       NC1064.2
234400     IF      WRK-XN-00001 = "1"                                   NC1064.2
234500             PERFORM PASS                                         NC1064.2
234600             PERFORM PRINT-DETAIL                                 NC1064.2
234700     ELSE                                                         NC1064.2
234800             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1064.2
234900                  TO RE-MARK                                      NC1064.2
235000             MOVE   "1"           TO CORRECT-X                    NC1064.2
235100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1064.2
235200             PERFORM FAIL                                         NC1064.2
235300             PERFORM PRINT-DETAIL.                                NC1064.2
235400     ADD     1 TO REC-CT.                                         NC1064.2
235500 SUB-TEST-F1-37-2.                                                NC1064.2
235600     MOVE   "SUB-TEST-F1-37-2" TO PAR-NAME.                       NC1064.2
235700     IF      SIZE-ERR2 = "A"                                      NC1064.2
235800             PERFORM PASS                                         NC1064.2
235900             PERFORM PRINT-DETAIL                                 NC1064.2
236000     ELSE                                                         NC1064.2
236100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1064.2
236200                  TO RE-MARK                                      NC1064.2
236300             MOVE   "A"        TO CORRECT-X                       NC1064.2
236400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1064.2
236500             PERFORM FAIL                                         NC1064.2
236600             PERFORM PRINT-DETAIL.                                NC1064.2
236700     ADD     1 TO REC-CT.                                         NC1064.2
236800 SUB-TEST-F1-37-3.                                                NC1064.2
236900     MOVE   "SUB-TEST-F1-37-3" TO PAR-NAME.                       NC1064.2
237000     IF      SIZE-ERR3 = "B"                                      NC1064.2
237100             PERFORM PASS                                         NC1064.2
237200             PERFORM PRINT-DETAIL                                 NC1064.2
237300     ELSE                                                         NC1064.2
237400             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1064.2
237500                  TO RE-MARK                                      NC1064.2
237600             MOVE   "B"        TO CORRECT-X                       NC1064.2
237700             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1064.2
237800             PERFORM FAIL                                         NC1064.2
237900             PERFORM PRINT-DETAIL.                                NC1064.2
238000     ADD     1 TO REC-CT.                                         NC1064.2
238100 SUB-TEST-F1-37-4.                                                NC1064.2
238200     MOVE   "SUB-TEST-F1-37-4" TO PAR-NAME.                       NC1064.2
238300     IF      SIZE-ERR4 = "C"                                      NC1064.2
238400             PERFORM PASS                                         NC1064.2
238500             PERFORM PRINT-DETAIL                                 NC1064.2
238600     ELSE                                                         NC1064.2
238700             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1064.2
238800                  TO RE-MARK                                      NC1064.2
238900             MOVE   "C"        TO CORRECT-X                       NC1064.2
239000             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1064.2
239100             PERFORM FAIL                                         NC1064.2
239200             PERFORM PRINT-DETAIL.                                NC1064.2
239300     ADD     1 TO REC-CT.                                         NC1064.2
239400 SUB-TEST-F1-37-5.                                                NC1064.2
239500     MOVE   "SUB-TEST-F1-37-5" TO PAR-NAME.                       NC1064.2
239600     IF      WRK-DS-06V06 = -666990.666333                        NC1064.2
239700             PERFORM PASS                                         NC1064.2
239800             PERFORM PRINT-DETAIL                                 NC1064.2
239900     ELSE                                                         NC1064.2
240000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1064.2
240100                  TO RE-MARK                                      NC1064.2
240200             MOVE   -666990.666333 TO CORRECT-N                   NC1064.2
240300             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1064.2
240400             PERFORM FAIL                                         NC1064.2
240500             PERFORM PRINT-DETAIL.                                NC1064.2
240600*                                                                 NC1064.2
240700 SUB-INIT-F1-38.                                                  NC1064.2
240800*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1064.2
240900     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1064.2
241000     MOVE   "SUB-TEST-F1-38"  TO PAR-NAME.                        NC1064.2
241100     MOVE   "0"   TO WRK-XN-00001.                                NC1064.2
241200     MOVE   "0"   TO SIZE-ERR4.                                   NC1064.2
241300     MOVE    -11  TO WRK-DS-02V00.                                NC1064.2
241400     MOVE    1    TO REC-CT.                                      NC1064.2
241500 SUB-TEST-F1-38-0.                                                NC1064.2
241600     SUBTRACT A99-DS-02V00 FROM WRK-DS-02V00                      NC1064.2
241700          ON SIZE ERROR                                           NC1064.2
241800             MOVE   SPACE TO WRK-XN-00001                         NC1064.2
241900      NOT ON SIZE ERROR                                           NC1064.2
242000             MOVE   "1" TO WRK-XN-00001                           NC1064.2
242100     END-SUBTRACT                                                 NC1064.2
242200     MOVE   "C" TO SIZE-ERR4.                                     NC1064.2
242300     GO TO   SUB-TEST-F1-38-1.                                    NC1064.2
242400 SUB-DELETE-F1-38.                                                NC1064.2
242500     PERFORM DE-LETE.                                             NC1064.2
242600     PERFORM PRINT-DETAIL.                                        NC1064.2
242700     GO TO   SUB-INIT-F1-39.                                      NC1064.2
242800 SUB-TEST-F1-38-1.                                                NC1064.2
242900     MOVE   "SUB-TEST-F1-38-1" TO PAR-NAME.                       NC1064.2
243000     IF      WRK-XN-00001 = SPACE                                 NC1064.2
243100             PERFORM PASS                                         NC1064.2
243200             PERFORM PRINT-DETAIL                                 NC1064.2
243300     ELSE                                                         NC1064.2
243400             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1064.2
243500                  TO RE-MARK                                      NC1064.2
243600             MOVE    SPACE        TO CORRECT-X                    NC1064.2
243700             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1064.2
243800             PERFORM FAIL                                         NC1064.2
243900             PERFORM PRINT-DETAIL.                                NC1064.2
244000     ADD     1 TO REC-CT.                                         NC1064.2
244100 SUB-TEST-F1-38-2.                                                NC1064.2
244200     MOVE   "SUB-TEST-F1-38-2" TO PAR-NAME.                       NC1064.2
244300     IF      SIZE-ERR4 = "C"                                      NC1064.2
244400             PERFORM PASS                                         NC1064.2
244500             PERFORM PRINT-DETAIL                                 NC1064.2
244600     ELSE                                                         NC1064.2
244700             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1064.2
244800                  TO RE-MARK                                      NC1064.2
244900             MOVE   "C"        TO CORRECT-X                       NC1064.2
245000             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1064.2
245100             PERFORM FAIL                                         NC1064.2
245200             PERFORM PRINT-DETAIL.                                NC1064.2
245300     ADD     1 TO REC-CT.                                         NC1064.2
245400 SUB-TEST-F1-38-3.                                                NC1064.2
245500     MOVE   "SUB-TEST-F1-38-3" TO PAR-NAME.                       NC1064.2
245600     IF      WRK-DS-02V00 = -11                                   NC1064.2
245700             PERFORM PASS                                         NC1064.2
245800             PERFORM PRINT-DETAIL                                 NC1064.2
245900     ELSE                                                         NC1064.2
246000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1064.2
246100                  TO RE-MARK                                      NC1064.2
246200             MOVE    -11          TO CORRECT-N                    NC1064.2
246300             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1064.2
246400             PERFORM FAIL                                         NC1064.2
246500             PERFORM PRINT-DETAIL.                                NC1064.2
246600*                                                                 NC1064.2
246700 SUB-INIT-F1-39.                                                  NC1064.2
246800*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1064.2
246900     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1064.2
247000     MOVE   "SUB-TEST-F1-39"  TO PAR-NAME.                        NC1064.2
247100     MOVE    SPACE TO WRK-XN-00001.                               NC1064.2
247200     MOVE    SPACE TO SIZE-ERR4.                                  NC1064.2
247300     MOVE    ZERO  TO WRK-DS-06V06.                               NC1064.2
247400     MOVE    1     TO REC-CT.                                     NC1064.2
247500 SUB-TEST-F1-39-0.                                                NC1064.2
247600     SUBTRACT A12THREES-DS-06V06                                  NC1064.2
247700              333333                                              NC1064.2
247800              A06THREES-DS-03V03                                  NC1064.2
247900              -0000009                                            NC1064.2
248000        FROM WRK-DS-06V06 ROUNDED                                 NC1064.2
248100             ON SIZE ERROR                                        NC1064.2
248200             MOVE   "X" TO WRK-XN-00001                           NC1064.2
248300         NOT ON SIZE ERROR                                        NC1064.2
248400             MOVE   "1" TO WRK-XN-00001                           NC1064.2
248500     END-SUBTRACT                                                 NC1064.2
248600     MOVE   "C" TO SIZE-ERR4.                                     NC1064.2
248700     GO TO   SUB-TEST-F1-39-1.                                    NC1064.2
248800 SUB-DELETE-F1-39.                                                NC1064.2
248900     PERFORM DE-LETE.                                             NC1064.2
249000     PERFORM PRINT-DETAIL.                                        NC1064.2
249100     GO TO   CCVS-EXIT.                                           NC1064.2
249200 SUB-TEST-F1-39-1.                                                NC1064.2
249300     MOVE   "SUB-TEST-F1-39-1" TO PAR-NAME.                       NC1064.2
249400     IF      WRK-XN-00001 = "1"                                   NC1064.2
249500             PERFORM PASS                                         NC1064.2
249600             PERFORM PRINT-DETAIL                                 NC1064.2
249700     ELSE                                                         NC1064.2
249800             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1064.2
249900                  TO RE-MARK                                      NC1064.2
250000             MOVE   "1"           TO CORRECT-X                    NC1064.2
250100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1064.2
250200             PERFORM FAIL                                         NC1064.2
250300             PERFORM PRINT-DETAIL.                                NC1064.2
250400     ADD     1 TO REC-CT.                                         NC1064.2
250500 SUB-TEST-F1-39-2.                                                NC1064.2
250600     MOVE   "SUB-TEST-F1-39-2" TO PAR-NAME.                       NC1064.2
250700     IF      SIZE-ERR4 = "C"                                      NC1064.2
250800             PERFORM PASS                                         NC1064.2
250900             PERFORM PRINT-DETAIL                                 NC1064.2
251000     ELSE                                                         NC1064.2
251100             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1064.2
251200                  TO RE-MARK                                      NC1064.2
251300             MOVE   "C"        TO CORRECT-X                       NC1064.2
251400             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1064.2
251500             PERFORM FAIL                                         NC1064.2
251600             PERFORM PRINT-DETAIL.                                NC1064.2
251700     ADD     1 TO REC-CT.                                         NC1064.2
251800 SUB-TEST-F1-39-3.                                                NC1064.2
251900     MOVE   "SUB-TEST-F1-39-3" TO PAR-NAME.                       NC1064.2
252000     IF      WRK-DS-06V06 = -666990.666333                        NC1064.2
252100             PERFORM PASS                                         NC1064.2
252200             PERFORM PRINT-DETAIL                                 NC1064.2
252300     ELSE                                                         NC1064.2
252400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1064.2
252500                  TO RE-MARK                                      NC1064.2
252600             MOVE   -666990.666333 TO CORRECT-N                   NC1064.2
252700             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1064.2
252800             PERFORM FAIL                                         NC1064.2
252900             PERFORM PRINT-DETAIL.                                NC1064.2
253000*                                                                 NC1064.2
253100 CCVS-EXIT SECTION.                                               NC1064.2
253200 CCVS-999999.                                                     NC1064.2
253300     GO TO CLOSE-FILES.                                           NC1064.2
      *END-OF,NC106A                                                                  
