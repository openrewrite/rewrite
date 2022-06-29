      *HEADER,COBOL,NC175A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1754.2
000200 PROGRAM-ID.                                                      NC1754.2
000300     NC175A.                                                      NC1754.2
000400****************************************************************  NC1754.2
000500*                                                              *  NC1754.2
000600*    VALIDATION FOR:-                                          *  NC1754.2
000700*                                                              *  NC1754.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1754.2
000900*                                                              *  NC1754.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1754.2
001100*                                                              *  NC1754.2
001200****************************************************************  NC1754.2
001300*                                                              *  NC1754.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1754.2
001500*                                                              *  NC1754.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1754.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1754.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1754.2
001900*                                                              *  NC1754.2
002000****************************************************************  NC1754.2
002100*                                                                 NC1754.2
002200*    PROGRAM NC175A TESTS FORMAT 2 OF THE SUBTRACT                NC1754.2
002300*    STATEMENT.  VARIOUS COMBINATINS OF DATA-ITEMS AND ALL        NC1754.2
002400*    OPTIONAL PHRASES ARE TESTED.                                 NC1754.2
002500*                                                                 NC1754.2
002600                                                                  NC1754.2
002700 ENVIRONMENT DIVISION.                                            NC1754.2
002800 CONFIGURATION SECTION.                                           NC1754.2
002900 SOURCE-COMPUTER.                                                 NC1754.2
003000     XXXXX082.                                                    NC1754.2
003100 OBJECT-COMPUTER.                                                 NC1754.2
003200     XXXXX083.                                                    NC1754.2
003300 INPUT-OUTPUT SECTION.                                            NC1754.2
003400 FILE-CONTROL.                                                    NC1754.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1754.2
003600     XXXXX055.                                                    NC1754.2
003700 DATA DIVISION.                                                   NC1754.2
003800 FILE SECTION.                                                    NC1754.2
003900 FD  PRINT-FILE.                                                  NC1754.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1754.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1754.2
004200 WORKING-STORAGE SECTION.                                         NC1754.2
004300 01  WRK-XN-00001 PIC X.                                          NC1754.2
004400 01  WRK-DU-0V1-1 PIC V9.                                         NC1754.2
004500 01  WRK-DU-2V0-1 PIC 99.                                         NC1754.2
004600 01  WRK-DU-2V0-2 PIC 99.                                         NC1754.2
004700 01  WRK-DU-2V0-3 PIC 99.                                         NC1754.2
004800 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1754.2
004900 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1754.2
005000 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1754.2
005100 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1754.2
005200 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1754.2
005300 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1754.2
005400 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1754.2
005500 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1754.2
005600 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1754.2
005700 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1754.2
005800 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1754.2
005900 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1754.2
006000 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1754.2
006100 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1754.2
006200 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1754.2
006300 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1754.2
006400 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1754.2
006500 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1754.2
006600 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1754.2
006700 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1754.2
006800 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1754.2
006900 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1754.2
007000 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1754.2
007100 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1754.2
007200 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1754.2
007300 01  WRK-NE-2 PIC $**.99.                                         NC1754.2
007400 01  WRK-NE-3 PIC $99.99CR.                                       NC1754.2
007500 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1754.2
007600 01  WRK-NE-5 PIC $.** VALUE ZERO.                                NC1754.2
007700 01  WRK-NE-6 PIC $**.**CR VALUE ZERO.                            NC1754.2
007800 01  WRK-NE-7 PIC $*9.99DB VALUE ZERO.                            NC1754.2
007900 77  SIZE-ERR                     PICTURE X  VALUE SPACE.         NC1754.2
008000 77  SIZE-ERR2                    PICTURE X  VALUE SPACE.         NC1754.2
008100 77  SIZE-ERR3                    PICTURE X  VALUE SPACE.         NC1754.2
008200 77  SIZE-ERR4                    PICTURE X  VALUE SPACE.         NC1754.2
008300 77  A16TWOS-DS-16V00             PICTURE S9(16)                  NC1754.2
008400                                  VALUE 2222222222222222.         NC1754.2
008500 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC1754.2
008600                                  VALUE 111111111111111111.       NC1754.2
008700 77  WRK-DS-10V00                 PICTURE S9(10).                 NC1754.2
008800 77  A10ONES-DS-10V00             PICTURE S9(10)                  NC1754.2
008900                                  VALUE 1111111111.               NC1754.2
009000 77  A05ONES-DS-05V00             PICTURE S9(5)                   NC1754.2
009100                                  VALUE 11111.                    NC1754.2
009200 77  A02ONES-DS-02V00             PICTURE S99                     NC1754.2
009300                                  VALUE 11.                       NC1754.2
009400 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1754.2
009500 77  WRK-DS-18V00 REDEFINES WRK-DS-09V09                          NC1754.2
009600                                  PICTURE S9(18).                 NC1754.2
009700 77  A06THREES-DS-03V03           PICTURE S999V999                NC1754.2
009800                                  VALUE 333.333.                  NC1754.2
009900 77  A12THREES-DS-06V06           PICTURE S9(6)V9(6)              NC1754.2
010000                                  VALUE 333333.333333.            NC1754.2
010100 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC1754.2
010200 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC1754.2
010300                                  PICTURE S9(12).                 NC1754.2
010400 77  A05ONES-DS-00V05             PICTURE SV9(5)                  NC1754.2
010500                                  VALUE .11111.                   NC1754.2
010600 77  WRK-DS-05V00                 PICTURE S9(5).                  NC1754.2
010700 77  WRK-DS-02V00                 PICTURE S99.                    NC1754.2
010800 77  A12ONES-DS-12V00             PICTURE S9(12)                  NC1754.2
010900                                  VALUE 111111111111.             NC1754.2
011000 77  WRK-DS-03V10                 PICTURE S999V9(10).             NC1754.2
011100 77  WRK-DS-13V00-S REDEFINES WRK-DS-03V10                        NC1754.2
011200                                  PICTURE S9(13).                 NC1754.2
011300 77  A99-DS-02V00                 PICTURE S99                     NC1754.2
011400                                  VALUE 99.                       NC1754.2
011500 77  A03ONES-DS-02V01             PICTURE S99V9                   NC1754.2
011600                                  VALUE 11.1.                     NC1754.2
011700 77  A06ONES-DS-03V03             PICTURE S999V999                NC1754.2
011800                                  VALUE 111.111.                  NC1754.2
011900 77  A08TWOS-DS-02V06             PICTURE S99V9(6)                NC1754.2
012000                                  VALUE 22.222222.                NC1754.2
012100 77  A01ONE-DS-P0801              PICTURE SP(8)9                  NC1754.2
012200                                  VALUE .000000001.               NC1754.2
012300 77  WRK-CS-18V00                 PICTURE S9(18) COMPUTATIONAL.   NC1754.2
012400 77  A18ONES-CS-18V00             PICTURE S9(18) COMPUTATIONAL    NC1754.2
012500                                  VALUE 111111111111111111.       NC1754.2
012600 77  WRK-CS-02V02                 PICTURE S99V99 COMPUTATIONAL.   NC1754.2
012700 77  A99-CS-02V00                 PICTURE S99 COMPUTATIONAL       NC1754.2
012800                                  VALUE 99.                       NC1754.2
012900 77  WRK-DS-0201P                 PICTURE S99P.                   NC1754.2
013000 77  WRK-DS-06V00                 PICTURE S9(6).                  NC1754.2
013100 77  AZERO-DS-05V05               PICTURE S9(5)V9(5)              NC1754.2
013200                                  VALUE ZERO.                     NC1754.2
013300 77  NDATA-DS-09V09               PICTURE S9(9)V9(9)              NC1754.2
013400                                  VALUE +012345678.876543210.     NC1754.2
013500 77  XDATA-XN-00018               PICTURE X(18)                   NC1754.2
013600                                  VALUE "00ABCDEFGHI  4321 ".     NC1754.2
013700 77  WRK-XN-00018                 PICTURE X(18).                  NC1754.2
013800 77  ADD-12    PICTURE PP9        VALUE .001.                     NC1754.2
013900 77  ADD-13    PICTURE 9PP        VALUE 100.                      NC1754.2
014000 77  ADD-14    PICTURE 999V999.                                   NC1754.2
014100 77  WRK-CU-18V00      PICTURE 9(18) VALUE ZERO COMPUTATIONAL.    NC1754.2
014200 77  A18EIGHTS-CS-18V00 PICTURE S9(18) VALUE -888888888888888888  NC1754.2
014300             COMPUTATIONAL.                                       NC1754.2
014400 77  A14TWOS-CS-18V00  PICTURE S9(18) VALUE -000022222222222222   NC1754.2
014500             COMPUTATIONAL.                                       NC1754.2
014600 77  A12THREES-CU-18V00 PICTURE 9(18) VALUE 000000333333333333    NC1754.2
014700             COMPUTATIONAL.                                       NC1754.2
014800 77  A16FOURS-CS-18V00 PICTURE S9(18) VALUE 004444444444444444    NC1754.2
014900             COMPUTATIONAL.                                       NC1754.2
015000 77  A18FIVES-CS-18V00 PICTURE S9(18) VALUE -555555555555555555   NC1754.2
015100             COMPUTATIONAL.                                       NC1754.2
015200 77  A18SIXES-CS-18V00 PICTURE S9(18) VALUE 666666666666666666    NC1754.2
015300             COMPUTATIONAL.                                       NC1754.2
015400 77  A12SEVENS-CU-18V00 PICTURE 9(18) VALUE 000000777777777777    NC1754.2
015500             COMPUTATIONAL.                                       NC1754.2
015600 77  WRK-DU-18V00      PICTURE 9(18) VALUE ZERO.                  NC1754.2
015700 77  A18THREES-CS-18V00 PICTURE S9(18) VALUE -333333333333333333  NC1754.2
015800             COMPUTATIONAL.                                       NC1754.2
015900 77  WRK-CS-03V00 PICTURE S999 COMPUTATIONAL SYNCHRONIZED RIGHT.  NC1754.2
016000 01  SUBTRACT-DATA.                                               NC1754.2
016100     02  SUBTR-1    PICTURE 9         VALUE 1.                    NC1754.2
016200     02  SUBTR-2    PICTURE S99       VALUE 99.                   NC1754.2
016300     02  SUBTR-3    PICTURE S9V99     VALUE -1.                   NC1754.2
016400     02  SUBTR-4    PICTURE SPP9      VALUE .001.                 NC1754.2
016500     02  SUBTR-5    PICTURE S9PP      VALUE 100.                  NC1754.2
016600     02  SUBTR-6    PICTURE 9  VALUE 1.                           NC1754.2
016700     02  SUBTR-7    PICTURE S99  VALUE 99.                        NC1754.2
016800     02  SUBTR-8    PICTURE S9V99  VALUE -9.99.                   NC1754.2
016900     02  SUBTR-10    PICTURE S999  VALUE 100.                     NC1754.2
017000     02  SUBTR-11   PICTURE S999V999.                             NC1754.2
017100 01  N-3                                PICTURE IS 99999.         NC1754.2
017200 01  N-4                                PICTURE IS 9(5)           NC1754.2
017300     VALUE IS 52800.                                              NC1754.2
017400 01  N-5                                PICTURE IS S9(9)V99       NC1754.2
017500     VALUE IS 000000001.00.                                       NC1754.2
017600 01  N-7                                PICTURE IS S9(7)V9(4)     NC1754.2
017700     VALUE IS 0000001.0000.                                       NC1754.2
017800 01  X-8 REDEFINES N-7                  PICTURE IS X(11).         NC1754.2
017900 01  N-10                               PICTURE IS S99999V        NC1754.2
018000     VALUE IS -00001.                                             NC1754.2
018100 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1754.2
018200 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1754.2
018300 01  N-13                               PICTURE IS 9(5)           NC1754.2
018400     VALUE IS 99999.                                              NC1754.2
018500 01  N-14                               PICTURE IS 9 VALUE IS 1.  NC1754.2
018600 01  N-15                               PICTURE IS 9(16).         NC1754.2
018700 01  N-16                               PICTURE IS S999999V99     NC1754.2
018800     VALUE IS 5.90.                                               NC1754.2
018900 01  N-17                               PICTURE IS S9(3)V99       NC1754.2
019000     VALUE IS +3.6.                                               NC1754.2
019100 01  N-18                               PICTURE IS S9(10)         NC1754.2
019200     VALUE IS -5.                                                 NC1754.2
019300 01  N-19                               PICTURE IS $9.00.         NC1754.2
019400 01  N-20                               PICTURE IS S9(9)          NC1754.2
019500     VALUE IS -999999999.                                         NC1754.2
019600 01  N-21                               PICTURE IS 9 VALUE IS 5.  NC1754.2
019700 01  N-22                               PICTURE IS 999V99         NC1754.2
019800     VALUE IS 005.55.                                             NC1754.2
019900 01  N-23                               PICTURE IS $$$.99CR.      NC1754.2
020000 01  N-25                               PICTURE IS 9 VALUE IS 1.  NC1754.2
020100 01  N-26               PICTURE 9(5).                             NC1754.2
020200 01  N-27                               PICTURE IS 9999V9         NC1754.2
020300     VALUE IS 9999.9.                                             NC1754.2
020400 01  N-28                               PICTURE IS $9999.00.      NC1754.2
020500 01  N-40                               PICTURE IS 9(7)           NC1754.2
020600     VALUE IS 7777777.                                            NC1754.2
020700 01  N-41                               PICTURE IS 9(7)           NC1754.2
020800     VALUE IS 1111111.                                            NC1754.2
020900 01  N-42                               PICTURE IS 9(3)P(4).      NC1754.2
021000 01  TRUNC-DATA.                                                  NC1754.2
021100     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1754.2
021200     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1754.2
021300     02 N-45  PICTURE S9.                                         NC1754.2
021400 01  MINUS-NAMES.                                                 NC1754.2
021500     02  MINUS-NAME1 PICTURE S9(18) VALUE -999999999999999999.    NC1754.2
021600     02  MINUS-NAME2 PICTURE S9(18) VALUE -999999999999999999.    NC1754.2
021700     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC1754.2
021800     02  PLUS-NAME1  PICTURE S9(18) VALUE +999999999999999999.    NC1754.2
021900     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC1754.2
022000     02  MINUS-NAME3 PICTURE SV9(18) VALUE -.999999999999999999.  NC1754.2
022100     02  MINUS-NAME4 PICTURE SV9(18) VALUE -.999999999999999999.  NC1754.2
022200     02  EVEN-NAME2  PICTURE SV9(18)  VALUE +.1.                  NC1754.2
022300     02  PLUS-NAME3  PICTURE SV9(18) VALUE +.999999999999999999.  NC1754.2
022400     02  PLUS-NAME4  PICTURE SV9(18) VALUE +.999999999999999999.  NC1754.2
022500     02  WHOLE-FIELD   PICTURE S9(18).                            NC1754.2
022600     02  DECMAL-FIELD  PICTURE SV9(18).                           NC1754.2
022700 01  TEST-RESULTS.                                                NC1754.2
022800     02 FILLER                   PIC X      VALUE SPACE.          NC1754.2
022900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1754.2
023000     02 FILLER                   PIC X      VALUE SPACE.          NC1754.2
023100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1754.2
023200     02 FILLER                   PIC X      VALUE SPACE.          NC1754.2
023300     02  PAR-NAME.                                                NC1754.2
023400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1754.2
023500       03  PARDOT-X              PIC X      VALUE SPACE.          NC1754.2
023600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1754.2
023700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1754.2
023800     02 RE-MARK                  PIC X(61).                       NC1754.2
023900 01  TEST-COMPUTED.                                               NC1754.2
024000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1754.2
024100     02 FILLER                   PIC X(17)  VALUE                 NC1754.2
024200            "       COMPUTED=".                                   NC1754.2
024300     02 COMPUTED-X.                                               NC1754.2
024400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1754.2
024500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1754.2
024600                                 PIC -9(9).9(9).                  NC1754.2
024700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1754.2
024800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1754.2
024900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1754.2
025000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1754.2
025100         04 COMPUTED-18V0                    PIC -9(18).          NC1754.2
025200         04 FILLER                           PIC X.               NC1754.2
025300     03 FILLER PIC X(50) VALUE SPACE.                             NC1754.2
025400 01  TEST-CORRECT.                                                NC1754.2
025500     02 FILLER PIC X(30) VALUE SPACE.                             NC1754.2
025600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1754.2
025700     02 CORRECT-X.                                                NC1754.2
025800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1754.2
025900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1754.2
026000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1754.2
026100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1754.2
026200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1754.2
026300     03      CR-18V0 REDEFINES CORRECT-A.                         NC1754.2
026400         04 CORRECT-18V0                     PIC -9(18).          NC1754.2
026500         04 FILLER                           PIC X.               NC1754.2
026600     03 FILLER PIC X(2) VALUE SPACE.                              NC1754.2
026700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1754.2
026800 01  CCVS-C-1.                                                    NC1754.2
026900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1754.2
027000-    "SS  PARAGRAPH-NAME                                          NC1754.2
027100-    "       REMARKS".                                            NC1754.2
027200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1754.2
027300 01  CCVS-C-2.                                                    NC1754.2
027400     02 FILLER                     PIC X        VALUE SPACE.      NC1754.2
027500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1754.2
027600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1754.2
027700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1754.2
027800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1754.2
027900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1754.2
028000 01  REC-CT                        PIC 99       VALUE ZERO.       NC1754.2
028100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1754.2
028200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1754.2
028300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1754.2
028400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1754.2
028500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1754.2
028600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1754.2
028700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1754.2
028800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1754.2
028900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1754.2
029000 01  CCVS-H-1.                                                    NC1754.2
029100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1754.2
029200     02  FILLER                    PIC X(42)    VALUE             NC1754.2
029300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1754.2
029400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1754.2
029500 01  CCVS-H-2A.                                                   NC1754.2
029600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1754.2
029700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1754.2
029800   02  FILLER                        PIC XXXX   VALUE             NC1754.2
029900     "4.2 ".                                                      NC1754.2
030000   02  FILLER                        PIC X(28)  VALUE             NC1754.2
030100            " COPY - NOT FOR DISTRIBUTION".                       NC1754.2
030200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1754.2
030300                                                                  NC1754.2
030400 01  CCVS-H-2B.                                                   NC1754.2
030500   02  FILLER                        PIC X(15)  VALUE             NC1754.2
030600            "TEST RESULT OF ".                                    NC1754.2
030700   02  TEST-ID                       PIC X(9).                    NC1754.2
030800   02  FILLER                        PIC X(4)   VALUE             NC1754.2
030900            " IN ".                                               NC1754.2
031000   02  FILLER                        PIC X(12)  VALUE             NC1754.2
031100     " HIGH       ".                                              NC1754.2
031200   02  FILLER                        PIC X(22)  VALUE             NC1754.2
031300            " LEVEL VALIDATION FOR ".                             NC1754.2
031400   02  FILLER                        PIC X(58)  VALUE             NC1754.2
031500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1754.2
031600 01  CCVS-H-3.                                                    NC1754.2
031700     02  FILLER                      PIC X(34)  VALUE             NC1754.2
031800            " FOR OFFICIAL USE ONLY    ".                         NC1754.2
031900     02  FILLER                      PIC X(58)  VALUE             NC1754.2
032000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1754.2
032100     02  FILLER                      PIC X(28)  VALUE             NC1754.2
032200            "  COPYRIGHT   1985 ".                                NC1754.2
032300 01  CCVS-E-1.                                                    NC1754.2
032400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1754.2
032500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1754.2
032600     02 ID-AGAIN                     PIC X(9).                    NC1754.2
032700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1754.2
032800 01  CCVS-E-2.                                                    NC1754.2
032900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1754.2
033000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1754.2
033100     02 CCVS-E-2-2.                                               NC1754.2
033200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1754.2
033300         03 FILLER                   PIC X      VALUE SPACE.      NC1754.2
033400         03 ENDER-DESC               PIC X(44)  VALUE             NC1754.2
033500            "ERRORS ENCOUNTERED".                                 NC1754.2
033600 01  CCVS-E-3.                                                    NC1754.2
033700     02  FILLER                      PIC X(22)  VALUE             NC1754.2
033800            " FOR OFFICIAL USE ONLY".                             NC1754.2
033900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1754.2
034000     02  FILLER                      PIC X(58)  VALUE             NC1754.2
034100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1754.2
034200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1754.2
034300     02 FILLER                       PIC X(15)  VALUE             NC1754.2
034400             " COPYRIGHT 1985".                                   NC1754.2
034500 01  CCVS-E-4.                                                    NC1754.2
034600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1754.2
034700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1754.2
034800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1754.2
034900     02 FILLER                       PIC X(40)  VALUE             NC1754.2
035000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1754.2
035100 01  XXINFO.                                                      NC1754.2
035200     02 FILLER                       PIC X(19)  VALUE             NC1754.2
035300            "*** INFORMATION ***".                                NC1754.2
035400     02 INFO-TEXT.                                                NC1754.2
035500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1754.2
035600       04 XXCOMPUTED                 PIC X(20).                   NC1754.2
035700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1754.2
035800       04 XXCORRECT                  PIC X(20).                   NC1754.2
035900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1754.2
036000 01  HYPHEN-LINE.                                                 NC1754.2
036100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1754.2
036200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1754.2
036300-    "*****************************************".                 NC1754.2
036400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1754.2
036500-    "******************************".                            NC1754.2
036600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1754.2
036700     "NC175A".                                                    NC1754.2
036800 PROCEDURE DIVISION.                                              NC1754.2
036900 CCVS1 SECTION.                                                   NC1754.2
037000 OPEN-FILES.                                                      NC1754.2
037100     OPEN     OUTPUT PRINT-FILE.                                  NC1754.2
037200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1754.2
037300     MOVE    SPACE TO TEST-RESULTS.                               NC1754.2
037400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1754.2
037500     GO TO CCVS1-EXIT.                                            NC1754.2
037600 CLOSE-FILES.                                                     NC1754.2
037700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1754.2
037800 TERMINATE-CCVS.                                                  NC1754.2
037900*    EXIT PROGRAM.                                                NC1754.2
038000*TERMINATE-CALL.                                                  NC1754.2
038100     STOP     RUN.                                                NC1754.2
038200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1754.2
038300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1754.2
038400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1754.2
038500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1754.2
038600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1754.2
038700 PRINT-DETAIL.                                                    NC1754.2
038800     IF REC-CT NOT EQUAL TO ZERO                                  NC1754.2
038900             MOVE "." TO PARDOT-X                                 NC1754.2
039000             MOVE REC-CT TO DOTVALUE.                             NC1754.2
039100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1754.2
039200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1754.2
039300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1754.2
039400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1754.2
039500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1754.2
039600     MOVE SPACE TO CORRECT-X.                                     NC1754.2
039700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1754.2
039800     MOVE     SPACE TO RE-MARK.                                   NC1754.2
039900 HEAD-ROUTINE.                                                    NC1754.2
040000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1754.2
040100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1754.2
040200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1754.2
040300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1754.2
040400 COLUMN-NAMES-ROUTINE.                                            NC1754.2
040500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1754.2
040600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1754.2
040700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1754.2
040800 END-ROUTINE.                                                     NC1754.2
040900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1754.2
041000 END-RTN-EXIT.                                                    NC1754.2
041100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1754.2
041200 END-ROUTINE-1.                                                   NC1754.2
041300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1754.2
041400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1754.2
041500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1754.2
041600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1754.2
041700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1754.2
041800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1754.2
041900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1754.2
042000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1754.2
042100  END-ROUTINE-12.                                                 NC1754.2
042200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1754.2
042300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1754.2
042400         MOVE "NO " TO ERROR-TOTAL                                NC1754.2
042500         ELSE                                                     NC1754.2
042600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1754.2
042700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1754.2
042800     PERFORM WRITE-LINE.                                          NC1754.2
042900 END-ROUTINE-13.                                                  NC1754.2
043000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1754.2
043100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1754.2
043200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1754.2
043300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1754.2
043400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1754.2
043500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1754.2
043600          MOVE "NO " TO ERROR-TOTAL                               NC1754.2
043700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1754.2
043800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1754.2
043900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1754.2
044000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1754.2
044100 WRITE-LINE.                                                      NC1754.2
044200     ADD 1 TO RECORD-COUNT.                                       NC1754.2
044300*    IF RECORD-COUNT GREATER 42                                   NC1754.2
044400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1754.2
044500*        MOVE SPACE TO DUMMY-RECORD                               NC1754.2
044600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1754.2
044700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1754.2
044800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1754.2
044900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1754.2
045000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1754.2
045100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1754.2
045200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1754.2
045300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1754.2
045400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1754.2
045500*        MOVE ZERO TO RECORD-COUNT.                               NC1754.2
045600     PERFORM WRT-LN.                                              NC1754.2
045700 WRT-LN.                                                          NC1754.2
045800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1754.2
045900     MOVE SPACE TO DUMMY-RECORD.                                  NC1754.2
046000 BLANK-LINE-PRINT.                                                NC1754.2
046100     PERFORM WRT-LN.                                              NC1754.2
046200 FAIL-ROUTINE.                                                    NC1754.2
046300     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1754.2
046400            GO TO FAIL-ROUTINE-WRITE.                             NC1754.2
046500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1754.2
046600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1754.2
046700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1754.2
046800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1754.2
046900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1754.2
047000     GO TO  FAIL-ROUTINE-EX.                                      NC1754.2
047100 FAIL-ROUTINE-WRITE.                                              NC1754.2
047200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1754.2
047300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1754.2
047400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1754.2
047500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1754.2
047600 FAIL-ROUTINE-EX. EXIT.                                           NC1754.2
047700 BAIL-OUT.                                                        NC1754.2
047800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1754.2
047900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1754.2
048000 BAIL-OUT-WRITE.                                                  NC1754.2
048100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1754.2
048200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1754.2
048300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1754.2
048400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1754.2
048500 BAIL-OUT-EX. EXIT.                                               NC1754.2
048600 CCVS1-EXIT.                                                      NC1754.2
048700     EXIT.                                                        NC1754.2
048800 SECT-NC175A-001 SECTION.                                         NC1754.2
048900 SUB-INIT-F2-1.                                                   NC1754.2
049000     MOVE   "VI-134 6.25.4 GR2" TO ANSI-REFERENCE.                NC1754.2
049100     MOVE   "SUBTRACT FROM GIVING" TO FEATURE.                    NC1754.2
049200 SUB-TEST-F2-1.                                                   NC1754.2
049300     MOVE     -2 TO N-10.                                         NC1754.2
049400     SUBTRACT N-10 FROM 0 GIVING N-19.                            NC1754.2
049500     IF       N-19 EQUAL TO "$2.00"                               NC1754.2
049600              PERFORM PASS                                        NC1754.2
049700              GO TO SUB-WRITE-F2-1.                               NC1754.2
049800     GO TO    SUB-FAIL-F2-1.                                      NC1754.2
049900 SUB-DELETE-F2-1.                                                 NC1754.2
050000     PERFORM  DE-LETE.                                            NC1754.2
050100     GO TO    SUB-WRITE-F2-1.                                     NC1754.2
050200 SUB-FAIL-F2-1.                                                   NC1754.2
050300     MOVE     N-19 TO COMPUTED-A.                                 NC1754.2
050400     MOVE "     $2.00" TO CORRECT-A.                              NC1754.2
050500     PERFORM  FAIL.                                               NC1754.2
050600 SUB-WRITE-F2-1.                                                  NC1754.2
050700     MOVE "SUB-TEST-F2-1 " TO PAR-NAME.                           NC1754.2
050800     PERFORM  PRINT-DETAIL.                                       NC1754.2
050900 SUB-TEST-F2-2.                                                   NC1754.2
051000     SUBTRACT N-21 FROM N-22 GIVING N-23 ROUNDED.                 NC1754.2
051100     IF       N-23 EQUAL TO "  $.55  "                            NC1754.2
051200              PERFORM PASS                                        NC1754.2
051300              GO TO SUB-WRITE-F2-2.                               NC1754.2
051400     GO TO    SUB-FAIL-F2-2.                                      NC1754.2
051500 SUB-DELETE-F2-2.                                                 NC1754.2
051600     PERFORM  DE-LETE.                                            NC1754.2
051700     GO TO    SUB-WRITE-F2-2.                                     NC1754.2
051800 SUB-FAIL-F2-2.                                                   NC1754.2
051900     MOVE     N-23 TO COMPUTED-A.                                 NC1754.2
052000     MOVE "      $.55" TO CORRECT-A.                              NC1754.2
052100     PERFORM  FAIL.                                               NC1754.2
052200 SUB-WRITE-F2-2.                                                  NC1754.2
052300     MOVE "SUB-TEST-F2-2 " TO PAR-NAME.                           NC1754.2
052400     PERFORM  PRINT-DETAIL.                                       NC1754.2
052500 SUB-INIT-F2-3-1.                                                 NC1754.2
052600     MOVE    1    TO N-25.                                        NC1754.2
052700     MOVE    ZERO TO N-26.                                        NC1754.2
052800 SUB-TEST-F2-3-1.                                                 NC1754.2
052900     SUBTRACT N-25 FROM -99999 GIVING N-26 ON SIZE ERROR          NC1754.2
053000              PERFORM PASS                                        NC1754.2
053100              GO TO SUB-WRITE-F2-3-1.                             NC1754.2
053200     GO TO    SUB-FAIL-F2-3-1.                                    NC1754.2
053300 SUB-DELETE-F2-3-1.                                               NC1754.2
053400     PERFORM  DE-LETE.                                            NC1754.2
053500     GO TO    SUB-WRITE-F2-3-1.                                   NC1754.2
053600 SUB-FAIL-F2-3-1.                                                 NC1754.2
053700     MOVE     N-26 TO COMPUTED-N.                                 NC1754.2
053800     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1754.2
053900     PERFORM  FAIL.                                               NC1754.2
054000 SUB-WRITE-F2-3-1.                                                NC1754.2
054100     MOVE "SUB-TEST-F2-3-1 " TO PAR-NAME.                         NC1754.2
054200     PERFORM  PRINT-DETAIL.                                       NC1754.2
054300 SUB-TEST-F2-3-2.                                                 NC1754.2
054400     IF       N-26 = ZERO                                         NC1754.2
054500              PERFORM PASS                                        NC1754.2
054600              GO TO SUB-WRITE-F2-3-2.                             NC1754.2
054700     GO TO    SUB-FAIL-F2-3-2.                                    NC1754.2
054800 SUB-DELETE-F2-3-2.                                               NC1754.2
054900     PERFORM  DE-LETE.                                            NC1754.2
055000     GO TO    SUB-WRITE-F2-3-2.                                   NC1754.2
055100 SUB-FAIL-F2-3-2.                                                 NC1754.2
055200     MOVE     N-26 TO COMPUTED-N.                                 NC1754.2
055300     MOVE     ZERO TO CORRECT-N.                                  NC1754.2
055400     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1754.2
055500     PERFORM  FAIL.                                               NC1754.2
055600 SUB-WRITE-F2-3-2.                                                NC1754.2
055700     MOVE "SUB-TEST-F2-3-2 " TO PAR-NAME.                         NC1754.2
055800     PERFORM  PRINT-DETAIL.                                       NC1754.2
055900 SUB-INIT-F2-4-1.                                                 NC1754.2
056000     MOVE    9999.9 TO N-27.                                      NC1754.2
056100     MOVE    ZERO   TO N-28.                                      NC1754.2
056200 SUB-TEST-F2-4-1.                                                 NC1754.2
056300     SUBTRACT -9 FROM N-27 GIVING N-28 ROUNDED ON SIZE ERROR      NC1754.2
056400              PERFORM PASS                                        NC1754.2
056500              GO TO SUB-WRITE-F2-4-1.                             NC1754.2
056600     GO TO    SUB-FAIL-F2-4-1.                                    NC1754.2
056700 SUB-DELETE-F2-4-1.                                               NC1754.2
056800     PERFORM  DE-LETE.                                            NC1754.2
056900     GO TO    SUB-WRITE-F2-4-1.                                   NC1754.2
057000 SUB-FAIL-F2-4-1.                                                 NC1754.2
057100     MOVE     N-28 TO COMPUTED-A.                                 NC1754.2
057200     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1754.2
057300     PERFORM  FAIL.                                               NC1754.2
057400 SUB-WRITE-F2-4-1.                                                NC1754.2
057500     MOVE "SUB-TEST-F2-4-1 " TO PAR-NAME.                         NC1754.2
057600     PERFORM  PRINT-DETAIL.                                       NC1754.2
057700 SUB-TEST-F2-4-2.                                                 NC1754.2
057800     IF      N-28 = "$0000.00"                                    NC1754.2
057900             PERFORM PASS                                         NC1754.2
058000             GO TO   SUB-WRITE-F2-4-2.                            NC1754.2
058100     GO TO   SUB-FAIL-F2-4-2.                                     NC1754.2
058200 SUB-DELETE-F2-4-2.                                               NC1754.2
058300     PERFORM DE-LETE.                                             NC1754.2
058400     GO TO   SUB-WRITE-F2-4-2.                                    NC1754.2
058500 SUB-FAIL-F2-4-2.                                                 NC1754.2
058600     MOVE    N-28 TO COMPUTED-X.                                  NC1754.2
058700     MOVE   "$0000.00"  TO CORRECT-X.                             NC1754.2
058800     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1754.2
058900     PERFORM FAIL.                                                NC1754.2
059000 SUB-WRITE-F2-4-2.                                                NC1754.2
059100     MOVE "SUB-TEST-F2-4-2 " TO PAR-NAME.                         NC1754.2
059200     PERFORM PRINT-DETAIL.                                        NC1754.2
059300 SUB-INIT-F2-5.                                                   NC1754.2
059400     MOVE     "  GIVING" TO FEATURE.                              NC1754.2
059500 SUB-TEST-F2-5.                                                   NC1754.2
059600     MOVE     ZERO TO WRK-DS-09V09.                               NC1754.2
059700     SUBTRACT A06THREES-DS-03V03 FROM A12THREES-DS-06V06          NC1754.2
059800              GIVING WRK-DS-06V06.                                NC1754.2
059900     IF       WRK-DS-06V06 EQUAL TO 333000.000333                 NC1754.2
060000              PERFORM PASS GO TO SUB-WRITE-F2-5.                  NC1754.2
060100     GO       TO SUB-FAIL-F2-5.                                   NC1754.2
060200 SUB-DELETE-F2-5.                                                 NC1754.2
060300     PERFORM  DE-LETE.                                            NC1754.2
060400     GO       TO SUB-WRITE-F2-5.                                  NC1754.2
060500 SUB-FAIL-F2-5.                                                   NC1754.2
060600     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1754.2
060700     MOVE     333000.000333 TO CORRECT-N.                         NC1754.2
060800     PERFORM  FAIL.                                               NC1754.2
060900 SUB-WRITE-F2-5.                                                  NC1754.2
061000     MOVE     "SUB-TEST-F2-5" TO PAR-NAME.                        NC1754.2
061100     PERFORM  PRINT-DETAIL.                                       NC1754.2
061200 SUB-TEST-F2-6.                                                   NC1754.2
061300     MOVE     ZERO TO WRK-DS-06V06.                               NC1754.2
061400     SUBTRACT A05ONES-DS-05V00                                    NC1754.2
061500              A05ONES-DS-00V05                                    NC1754.2
061600              A12THREES-DS-06V06                                  NC1754.2
061700              A06THREES-DS-03V03 FROM ZERO GIVING WRK-DS-06V06.   NC1754.2
061800     IF       WRK-DS-06V06 EQUAL TO -344777.777443                NC1754.2
061900              PERFORM PASS GO TO SUB-WRITE-F2-6.                  NC1754.2
062000     GO       TO SUB-FAIL-F2-6.                                   NC1754.2
062100 SUB-DELETE-F2-6.                                                 NC1754.2
062200     PERFORM  DE-LETE.                                            NC1754.2
062300     GO       TO SUB-WRITE-F2-6.                                  NC1754.2
062400 SUB-FAIL-F2-6.                                                   NC1754.2
062500     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1754.2
062600     MOVE     -344777.777443 TO CORRECT-N.                        NC1754.2
062700     PERFORM  FAIL.                                               NC1754.2
062800 SUB-WRITE-F2-6.                                                  NC1754.2
062900     MOVE     "SUB-TEST-F2-6" TO PAR-NAME.                        NC1754.2
063000     PERFORM  PRINT-DETAIL.                                       NC1754.2
063100 SUB-TEST-F2-7.                                                   NC1754.2
063200     MOVE     -099999.999999 TO WRK-DS-06V06.                     NC1754.2
063300     SUBTRACT A05ONES-DS-05V00                                    NC1754.2
063400              -11111                                              NC1754.2
063500              AZERO-DS-05V05 FROM WRK-DS-06V06                    NC1754.2
063600              GIVING WRK-DS-06V00 ROUNDED.                        NC1754.2
063700     IF       WRK-DS-06V00 EQUAL TO -100000                       NC1754.2
063800              PERFORM PASS GO TO SUB-WRITE-F2-7.                  NC1754.2
063900     GO       TO SUB-FAIL-F2-7.                                   NC1754.2
064000 SUB-DELETE-F2-7.                                                 NC1754.2
064100     PERFORM  DE-LETE.                                            NC1754.2
064200     GO       TO SUB-WRITE-F2-7.                                  NC1754.2
064300 SUB-FAIL-F2-7.                                                   NC1754.2
064400     MOVE     WRK-DS-06V00 TO COMPUTED-N.                         NC1754.2
064500     MOVE    -100000 TO CORRECT-N.                                NC1754.2
064600     PERFORM  FAIL.                                               NC1754.2
064700 SUB-WRITE-F2-7.                                                  NC1754.2
064800     MOVE     "SUB-TEST-F2-7" TO PAR-NAME.                        NC1754.2
064900     PERFORM  PRINT-DETAIL.                                       NC1754.2
065000 SUB-TEST-F2-8-1.                                                 NC1754.2
065100     MOVE     ZERO TO WRK-DS-10V00.                               NC1754.2
065200     SUBTRACT A12ONES-DS-12V00                                    NC1754.2
065300              FROM ZERO GIVING WRK-DS-10V00 ON SIZE ERROR         NC1754.2
065400              PERFORM PASS GO TO SUB-WRITE-F2-8-1.                NC1754.2
065500     GO       TO SUB-FAIL-F2-8-1.                                 NC1754.2
065600 SUB-DELETE-F2-8-1.                                               NC1754.2
065700     PERFORM  DE-LETE.                                            NC1754.2
065800     GO       TO SUB-WRITE-F2-8-1.                                NC1754.2
065900 SUB-FAIL-F2-8-1.                                                 NC1754.2
066000     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1754.2
066100     PERFORM  FAIL.                                               NC1754.2
066200 SUB-WRITE-F2-8-1.                                                NC1754.2
066300     MOVE     "SUB-TEST-F2-8-1" TO PAR-NAME.                      NC1754.2
066400     PERFORM  PRINT-DETAIL.                                       NC1754.2
066500 SUB-TEST-F2-8-2.                                                 NC1754.2
066600     IF       WRK-DS-10V00 EQUAL TO ZERO                          NC1754.2
066700              PERFORM PASS GO TO SUB-WRITE-F2-8-2.                NC1754.2
066800*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F2-8-1   NC1754.2
066900     GO       TO SUB-FAIL-F2-8-2.                                 NC1754.2
067000 SUB-DELETE-F2-8-2.                                               NC1754.2
067100     PERFORM  DE-LETE.                                            NC1754.2
067200     GO       TO SUB-WRITE-F2-8-2.                                NC1754.2
067300 SUB-FAIL-F2-8-2.                                                 NC1754.2
067400     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1754.2
067500     MOVE     WRK-DS-10V00 TO COMPUTED-14V4.                      NC1754.2
067600     MOVE     ZERO TO CORRECT-14V4.                               NC1754.2
067700     PERFORM  FAIL.                                               NC1754.2
067800 SUB-WRITE-F2-8-2.                                                NC1754.2
067900     MOVE     "SUB-TEST-F2-8-2" TO PAR-NAME.                      NC1754.2
068000     PERFORM  PRINT-DETAIL.                                       NC1754.2
068100 SUB-TEST-F2-9-1.                                                 NC1754.2
068200     MOVE ZERO TO WRK-DS-05V00.                                   NC1754.2
068300     SUBTRACT 33333                                               NC1754.2
068400              A06THREES-DS-03V03                                  NC1754.2
068500              A12THREES-DS-06V06                                  NC1754.2
068600              FROM -1000000 GIVING WRK-DS-05V00                   NC1754.2
068700              ROUNDED ON SIZE ERROR                               NC1754.2
068800              PERFORM PASS GO TO SUB-WRITE-F2-9-1.                NC1754.2
068900     GO       TO SUB-FAIL-F2-9-1.                                 NC1754.2
069000 SUB-DELETE-F2-9-1.                                               NC1754.2
069100     PERFORM  DE-LETE.                                            NC1754.2
069200     GO       TO SUB-WRITE-F2-9-1.                                NC1754.2
069300 SUB-FAIL-F2-9-1.                                                 NC1754.2
069400     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1754.2
069500     PERFORM  FAIL.                                               NC1754.2
069600 SUB-WRITE-F2-9-1.                                                NC1754.2
069700     MOVE     "SUB-TEST-F2-9-1" TO PAR-NAME.                      NC1754.2
069800     PERFORM  PRINT-DETAIL.                                       NC1754.2
069900 SUB-TEST-F2-9-2.                                                 NC1754.2
070000     IF       WRK-DS-05V00 EQUAL TO ZERO                          NC1754.2
070100              PERFORM PASS GO TO SUB-WRITE-F2-9-2.                NC1754.2
070200*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F2-9-1   NC1754.2
070300     GO       TO SUB-FAIL-F2-9-2.                                 NC1754.2
070400 SUB-DELETE-F2-9-2.                                               NC1754.2
070500     PERFORM  DE-LETE.                                            NC1754.2
070600     GO       TO SUB-WRITE-F2-9-2.                                NC1754.2
070700 SUB-FAIL-F2-9-2.                                                 NC1754.2
070800     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1754.2
070900     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC1754.2
071000     MOVE     ZERO TO CORRECT-N.                                  NC1754.2
071100     PERFORM  FAIL.                                               NC1754.2
071200 SUB-WRITE-F2-9-2.                                                NC1754.2
071300     MOVE     "SUB-TEST-F2-9-2" TO PAR-NAME.                      NC1754.2
071400     PERFORM  PRINT-DETAIL.                                       NC1754.2
071500 SUB-TEST-F2-10-1.                                                NC1754.2
071600     MOVE     ZERO TO WRK-DS-06V06.                               NC1754.2
071700     SUBTRACT A12THREES-DS-06V06                                  NC1754.2
071800              333333                                              NC1754.2
071900              A06THREES-DS-03V03                                  NC1754.2
072000              -.0000009 FROM 0000000                              NC1754.2
072100              GIVING WRK-DS-06V06 ROUNDED ON SIZE ERROR           NC1754.2
072200              GO TO SUB-FAIL-F2-10-1.                             NC1754.2
072300     PERFORM  PASS.                                               NC1754.2
072400     GO       TO SUB-WRITE-F2-10-1.                               NC1754.2
072500 SUB-DELETE-F2-10-1.                                              NC1754.2
072600     PERFORM  DE-LETE.                                            NC1754.2
072700     GO       TO SUB-WRITE-F2-10-1.                               NC1754.2
072800 SUB-FAIL-F2-10-1.                                                NC1754.2
072900     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1754.2
073000     PERFORM  FAIL.                                               NC1754.2
073100 SUB-WRITE-F2-10-1.                                               NC1754.2
073200     MOVE     "SUB-TEST-F2-10-1" TO PAR-NAME.                     NC1754.2
073300     PERFORM  PRINT-DETAIL.                                       NC1754.2
073400 SUB-TEST-F2-10-2.                                                NC1754.2
073500     IF       WRK-DS-06V06 EQUAL TO -666999.666332                NC1754.2
073600              PERFORM PASS GO TO SUB-WRITE-F2-10-2.               NC1754.2
073700*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F2-10-1  NC1754.2
073800     GO       TO SUB-FAIL-F2-10-2.                                NC1754.2
073900 SUB-DELETE-F2-10-2.                                              NC1754.2
074000     PERFORM  DE-LETE.                                            NC1754.2
074100     GO       TO SUB-WRITE-F2-10-2.                               NC1754.2
074200 SUB-FAIL-F2-10-2.                                                NC1754.2
074300     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1754.2
074400     MOVE     -666999.666332 TO CORRECT-N.                        NC1754.2
074500     PERFORM  FAIL.                                               NC1754.2
074600 SUB-WRITE-F2-10-2.                                               NC1754.2
074700     MOVE     "SUB-TEST-F2-10-2" TO PAR-NAME.                     NC1754.2
074800     PERFORM  PRINT-DETAIL.                                       NC1754.2
074900 SUB-INIT-F2-11.                                                  NC1754.2
075000     MOVE     "  SERIES" TO FEATURE.                              NC1754.2
075100 SUB-TEST-F2-11.                                                  NC1754.2
075200     MOVE     ZERO TO WRK-DS-03V10.                               NC1754.2
075300     SUBTRACT A99-DS-02V00                                        NC1754.2
075400              A03ONES-DS-02V01                                    NC1754.2
075500              A06ONES-DS-03V03                                    NC1754.2
075600              A08TWOS-DS-02V06                                    NC1754.2
075700              -1.1111111                                          NC1754.2
075800              +.11111111                                          NC1754.2
075900              A01ONE-DS-P0801 FROM 0000.000000                    NC1754.2
076000              GIVING WRK-DS-03V10.                                NC1754.2
076100     IF       WRK-DS-03V10 EQUAL TO -242.4332220110               NC1754.2
076200              PERFORM PASS GO TO SUB-WRITE-F2-11.                 NC1754.2
076300     GO       TO SUB-FAIL-F2-11.                                  NC1754.2
076400 SUB-DELETE-F2-11.                                                NC1754.2
076500     PERFORM  DE-LETE.                                            NC1754.2
076600     GO       TO SUB-WRITE-F2-11.                                 NC1754.2
076700 SUB-FAIL-F2-11.                                                  NC1754.2
076800     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1754.2
076900     MOVE     -242.4332220110 TO CORRECT-4V14.                    NC1754.2
077000     PERFORM  FAIL.                                               NC1754.2
077100 SUB-WRITE-F2-11.                                                 NC1754.2
077200     MOVE     "SUB-TEST-F2-11" TO PAR-NAME.                       NC1754.2
077300     PERFORM  PRINT-DETAIL.                                       NC1754.2
077400 SUB-TEST-F2-12.                                                  NC1754.2
077500     MOVE     ZERO TO WRK-DS-03V10.                               NC1754.2
077600     SUBTRACT A01ONE-DS-P0801                                     NC1754.2
077700              +.11111111                                          NC1754.2
077800              -1.1111111                                          NC1754.2
077900              A08TWOS-DS-02V06                                    NC1754.2
078000              A06ONES-DS-03V03                                    NC1754.2
078100              A03ONES-DS-02V01                                    NC1754.2
078200              A99-DS-02V00 FROM 0000.000000 GIVING WRK-DS-03V10.  NC1754.2
078300     IF       WRK-DS-03V10 EQUAL TO -242.4332220110               NC1754.2
078400              PERFORM PASS GO TO SUB-WRITE-F2-12.                 NC1754.2
078500     GO       TO SUB-FAIL-F2-12.                                  NC1754.2
078600 SUB-DELETE-F2-12.                                                NC1754.2
078700     PERFORM  DE-LETE.                                            NC1754.2
078800     GO       TO SUB-WRITE-F2-12.                                 NC1754.2
078900 SUB-FAIL-F2-12.                                                  NC1754.2
079000     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1754.2
079100     MOVE     -242.4332220110 TO CORRECT-4V14.                    NC1754.2
079200     PERFORM  FAIL.                                               NC1754.2
079300 SUB-WRITE-F2-12.                                                 NC1754.2
079400     MOVE     "SUB-TEST-F2-12" TO PAR-NAME.                       NC1754.2
079500     PERFORM  PRINT-DETAIL.                                       NC1754.2
079600 SUB-TEST-F2-13.                                                  NC1754.2
079700     MOVE     ZERO TO WRK-DS-03V10.                               NC1754.2
079800     SUBTRACT A08TWOS-DS-02V06                                    NC1754.2
079900              A99-DS-02V00                                        NC1754.2
080000              -1.1111111                                          NC1754.2
080100              A03ONES-DS-02V01                                    NC1754.2
080200              A01ONE-DS-P0801                                     NC1754.2
080300              +.11111111                                          NC1754.2
080400              A06ONES-DS-03V03 FROM 0000.000000                   NC1754.2
080500              GIVING WRK-DS-03V10.                                NC1754.2
080600     IF       WRK-DS-03V10 EQUAL TO -242.4332220110               NC1754.2
080700              PERFORM PASS GO TO SUB-WRITE-F2-13.                 NC1754.2
080800     GO       TO SUB-FAIL-F2-13.                                  NC1754.2
080900 SUB-DELETE-F2-13.                                                NC1754.2
081000     PERFORM  DE-LETE.                                            NC1754.2
081100     GO       TO SUB-WRITE-F2-13.                                 NC1754.2
081200 SUB-FAIL-F2-13.                                                  NC1754.2
081300     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1754.2
081400     MOVE     -242.4332220110 TO CORRECT-4V14.                    NC1754.2
081500     PERFORM  FAIL.                                               NC1754.2
081600 SUB-WRITE-F2-13.                                                 NC1754.2
081700     MOVE     "SUB-TEST-F2-13" TO PAR-NAME.                       NC1754.2
081800     PERFORM  PRINT-DETAIL.                                       NC1754.2
081900 SUB-TEST-F2-14.                                                  NC1754.2
082000     SUBTRACT SUBTR-4 SUBTR-5 .499 FROM SUBTR-2 GIVING SUBTR-11.  NC1754.2
082100     IF       SUBTR-11 EQUAL TO -1.5                              NC1754.2
082200              PERFORM PASS GO TO SUB-WRITE-F2-14.                 NC1754.2
082300     GO       TO SUB-FAIL-F2-14.                                  NC1754.2
082400 SUB-DELETE-F2-14.                                                NC1754.2
082500     PERFORM DE-LETE.                                             NC1754.2
082600     GO       TO SUB-WRITE-F2-14.                                 NC1754.2
082700 SUB-FAIL-F2-14.                                                  NC1754.2
082800     MOVE     SUBTR-11  TO COMPUTED-N.                            NC1754.2
082900     MOVE     -1.5      TO CORRECT-N.                             NC1754.2
083000     PERFORM FAIL.                                                NC1754.2
083100 SUB-WRITE-F2-14.                                                 NC1754.2
083200     MOVE     "SUB-TEST-F2-14" TO PAR-NAME.                       NC1754.2
083300     PERFORM PRINT-DETAIL.                                        NC1754.2
083400 SUB-TEST-F2-15-1.                                                NC1754.2
083500     SUBTRACT SUBTR-1 SUBTR-3 FROM SUBTR-5 GIVING SUBTR-7 ON      NC1754.2
083600              SIZE ERROR                                          NC1754.2
083700              PERFORM PASS GO TO SUB-WRITE-F2-15-1.               NC1754.2
083800     GO       TO SUB-FAIL-F2-15-1.                                NC1754.2
083900 SUB-DELETE-F2-15-1.                                              NC1754.2
084000     PERFORM DE-LETE.                                             NC1754.2
084100     GO       TO SUB-WRITE-F2-15-1.                               NC1754.2
084200 SUB-FAIL-F2-15-1.                                                NC1754.2
084300     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1754.2
084400     PERFORM FAIL.                                                NC1754.2
084500 SUB-WRITE-F2-15-1.                                               NC1754.2
084600     MOVE     "SUB-TEST-F2-15-1" TO PAR-NAME.                     NC1754.2
084700     PERFORM PRINT-DETAIL.                                        NC1754.2
084800 SUB-TEST-F2-15-2.                                                NC1754.2
084900     IF       SUBTR-7 EQUAL  TO 99                                NC1754.2
085000              PERFORM PASS GO TO SUB-WRITE-F2-15-2.               NC1754.2
085100*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-F2-15-2  NC1754.2
085200     GO       TO SUB-FAIL-F2-15-2.                                NC1754.2
085300 SUB-DELETE-F2-15-2.                                              NC1754.2
085400     PERFORM DE-LETE.                                             NC1754.2
085500     GO       TO SUB-WRITE-F2-15-2.                               NC1754.2
085600 SUB-FAIL-F2-15-2.                                                NC1754.2
085700     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1754.2
085800     MOVE     SUBTR-7   TO COMPUTED-N.                            NC1754.2
085900     MOVE     ZERO      TO CORRECT-N.                             NC1754.2
086000     PERFORM FAIL.                                                NC1754.2
086100 SUB-WRITE-F2-15-2.                                               NC1754.2
086200     MOVE     "SUB-TEST-F2-15-2" TO PAR-NAME.                     NC1754.2
086300     PERFORM PRINT-DETAIL.                                        NC1754.2
086400 SUB-TEST-F2-16-1.                                                NC1754.2
086500     MOVE SPACE TO SIZE-ERR.                                      NC1754.2
086600     SUBTRACT MINUS-NAME1 MINUS-NAME2 -34 -1 PLUS-NAME1           NC1754.2
086700         PLUS-NAME2 EVEN-NAME1 35 FROM EVEN-NAME1 GIVING          NC1754.2
086800         WHOLE-FIELD                                              NC1754.2
086900         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1754.2
087000     IF WHOLE-FIELD EQUAL TO 0                                    NC1754.2
087100         PERFORM PASS                                             NC1754.2
087200         GO TO SUB-WRITE-F2-16-1.                                 NC1754.2
087300     MOVE WHOLE-FIELD TO COMPUTED-18V0.                           NC1754.2
087400     MOVE 0 TO CORRECT-18V0.                                      NC1754.2
087500     PERFORM FAIL.                                                NC1754.2
087600     GO TO SUB-WRITE-F2-16-1.                                     NC1754.2
087700 SUB-DELETE-F2-16-1.                                              NC1754.2
087800     PERFORM DE-LETE.                                             NC1754.2
087900 SUB-WRITE-F2-16-1.                                               NC1754.2
088000     MOVE "SUB-TEST-F2-16-1" TO PAR-NAME.                         NC1754.2
088100     PERFORM PRINT-DETAIL.                                        NC1754.2
088200 SUB-TEST-F2-16-2.                                                NC1754.2
088300     IF SIZE-ERR EQUAL TO "1"                                     NC1754.2
088400         PERFORM FAIL                                             NC1754.2
088500         MOVE SPACE TO CORRECT-A                                  NC1754.2
088600         MOVE 1 TO COMPUTED-A                                     NC1754.2
088700         MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK      NC1754.2
088800         GO TO SUB-WRITE-F2-16-2.                                 NC1754.2
088900     PERFORM PASS.                                                NC1754.2
089000     GO TO SUB-WRITE-F2-16-2.                                     NC1754.2
089100 SUB-DELETE-F2-16-2.                                              NC1754.2
089200     PERFORM DE-LETE.                                             NC1754.2
089300 SUB-WRITE-F2-16-2.                                               NC1754.2
089400     MOVE "SUB-TEST-F2-16-2" TO PAR-NAME.                         NC1754.2
089500     PERFORM PRINT-DETAIL.                                        NC1754.2
089600 SUB-TEST-F2-17-1.                                                NC1754.2
089700     MOVE SPACE TO SIZE-ERR.                                      NC1754.2
089800     SUBTRACT MINUS-NAME3 MINUS-NAME4 -.34 -.01 PLUS-NAME3        NC1754.2
089900         PLUS-NAME4 EVEN-NAME2 .35 FROM EVEN-NAME2                NC1754.2
090000         GIVING DECMAL-FIELD                                      NC1754.2
090100         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1754.2
090200     IF DECMAL-FIELD EQUAL TO .0                                  NC1754.2
090300         PERFORM PASS                                             NC1754.2
090400         GO TO SUB-WRITE-F2-17-1.                                 NC1754.2
090500     MOVE DECMAL-FIELD TO COMPUTED-0V18.                          NC1754.2
090600     MOVE .0 TO CORRECT-0V18.                                     NC1754.2
090700     PERFORM FAIL.                                                NC1754.2
090800     GO TO SUB-WRITE-F2-17-1.                                     NC1754.2
090900 SUB-DELETE-F2-17-1.                                              NC1754.2
091000     PERFORM DE-LETE.                                             NC1754.2
091100 SUB-WRITE-F2-17-1.                                               NC1754.2
091200     MOVE "SUB-TEST-F2-17-1" TO PAR-NAME.                         NC1754.2
091300     PERFORM PRINT-DETAIL.                                        NC1754.2
091400 SUB-TEST-F2-17-2.                                                NC1754.2
091500     IF SIZE-ERR EQUAL TO "1"                                     NC1754.2
091600         PERFORM FAIL                                             NC1754.2
091700         MOVE SPACE TO CORRECT-A                                  NC1754.2
091800         MOVE 1 TO COMPUTED-A                                     NC1754.2
091900         MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK      NC1754.2
092000         GO TO SUB-WRITE-F2-17-2.                                 NC1754.2
092100     PERFORM PASS.                                                NC1754.2
092200     GO TO SUB-WRITE-F2-17-2.                                     NC1754.2
092300 SUB-DELETE-F2-17-2.                                              NC1754.2
092400     PERFORM DE-LETE.                                             NC1754.2
092500 SUB-WRITE-F2-17-2.                                               NC1754.2
092600     MOVE "SUB-TEST-F2-17-2" TO PAR-NAME.                         NC1754.2
092700     PERFORM PRINT-DETAIL.                                        NC1754.2
092800 SUB-TEST-F2-18.                                                  NC1754.2
092900     MOVE ZERO TO WRK-CS-18V00.                                   NC1754.2
093000     SUBTRACT A12THREES-CU-18V00 FROM A14TWOS-CS-18V00            NC1754.2
093100             GIVING WRK-CS-18V00.                                 NC1754.2
093200     IF WRK-CS-18V00 EQUAL TO -000022555555555555                 NC1754.2
093300         PERFORM PASS                                             NC1754.2
093400         GO TO SUB-WRITE-F2-18.                                   NC1754.2
093500     MOVE -000022555555555555 TO CORRECT-18V0.                    NC1754.2
093600     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1754.2
093700     PERFORM FAIL.                                                NC1754.2
093800     GO TO SUB-WRITE-F2-18.                                       NC1754.2
093900 SUB-DELETE-F2-18.                                                NC1754.2
094000     PERFORM DE-LETE.                                             NC1754.2
094100 SUB-WRITE-F2-18.                                                 NC1754.2
094200     MOVE "SUB-TEST-F2-18 " TO PAR-NAME.                          NC1754.2
094300     PERFORM PRINT-DETAIL.                                        NC1754.2
094400 SUB-TEST-F2-19.                                                  NC1754.2
094500     MOVE ZERO TO WRK-DU-18V00.                                   NC1754.2
094600     SUBTRACT A18SIXES-CS-18V00 FROM A18THREES-CS-18V00           NC1754.2
094700             GIVING WRK-DU-18V00.                                 NC1754.2
094800     IF WRK-DU-18V00 EQUAL TO 999999999999999999                  NC1754.2
094900         PERFORM PASS                                             NC1754.2
095000         GO TO SUB-WRITE-F2-19.                                   NC1754.2
095100     MOVE 999999999999999999 TO CORRECT-18V0.                     NC1754.2
095200     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1754.2
095300     PERFORM FAIL.                                                NC1754.2
095400     GO TO SUB-WRITE-F2-19.                                       NC1754.2
095500 SUB-DELETE-F2-19.                                                NC1754.2
095600     PERFORM DE-LETE.                                             NC1754.2
095700 SUB-WRITE-F2-19.                                                 NC1754.2
095800     MOVE "SUB-TEST-F2-19 " TO PAR-NAME.                          NC1754.2
095900     PERFORM PRINT-DETAIL.                                        NC1754.2
096000 SUB-TEST-F2-20.                                                  NC1754.2
096100     MOVE ZERO TO WRK-CS-18V00.                                   NC1754.2
096200     SUBTRACT A16FOURS-CS-18V00 FROM A12THREES-CU-18V00           NC1754.2
096300             GIVING WRK-CS-18V00.                                 NC1754.2
096400     IF WRK-CS-18V00 EQUAL TO -004444111111111111                 NC1754.2
096500         PERFORM PASS                                             NC1754.2
096600         GO TO SUB-WRITE-F2-20.                                   NC1754.2
096700     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1754.2
096800     MOVE -004444111111111111 TO CORRECT-18V0.                    NC1754.2
096900     PERFORM FAIL.                                                NC1754.2
097000     GO TO SUB-WRITE-F2-20.                                       NC1754.2
097100 SUB-DELETE-F2-20.                                                NC1754.2
097200     PERFORM DE-LETE.                                             NC1754.2
097300 SUB-WRITE-F2-20.                                                 NC1754.2
097400     MOVE "SUB-TEST-F2-20 " TO PAR-NAME.                          NC1754.2
097500     PERFORM PRINT-DETAIL.                                        NC1754.2
097600 SUB-TEST-F2-21.                                                  NC1754.2
097700     MOVE ZERO TO WRK-DU-18V00.                                   NC1754.2
097800     SUBTRACT A18THREES-CS-18V00 FROM A18ONES-CS-18V00            NC1754.2
097900             GIVING WRK-DU-18V00.                                 NC1754.2
098000     IF WRK-DU-18V00 EQUAL TO 444444444444444444                  NC1754.2
098100         PERFORM PASS                                             NC1754.2
098200         GO TO SUB-WRITE-F2-21.                                   NC1754.2
098300     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1754.2
098400     MOVE 444444444444444444 TO CORRECT-18V0.                     NC1754.2
098500     PERFORM FAIL.                                                NC1754.2
098600     GO TO SUB-WRITE-F2-21.                                       NC1754.2
098700 SUB-DELETE-F2-21.                                                NC1754.2
098800     PERFORM DE-LETE.                                             NC1754.2
098900 SUB-WRITE-F2-21.                                                 NC1754.2
099000     MOVE "SUB-TEST-F2-21 " TO PAR-NAME.                          NC1754.2
099100     PERFORM PRINT-DETAIL.                                        NC1754.2
099200 SUB-TEST-F2-22.                                                  NC1754.2
099300     MOVE ZERO TO WRK-CS-18V00.                                   NC1754.2
099400     SUBTRACT A18SIXES-CS-18V00 FROM A18THREES-CS-18V00           NC1754.2
099500             GIVING WRK-CS-18V00.                                 NC1754.2
099600     IF WRK-CS-18V00 EQUAL TO -999999999999999999                 NC1754.2
099700         PERFORM PASS                                             NC1754.2
099800         GO TO SUB-WRITE-F2-22.                                   NC1754.2
099900     MOVE -999999999999999999 TO CORRECT-18V0.                    NC1754.2
100000     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1754.2
100100     PERFORM FAIL.                                                NC1754.2
100200     GO TO SUB-WRITE-F2-22.                                       NC1754.2
100300 SUB-DELETE-F2-22.                                                NC1754.2
100400     PERFORM DE-LETE.                                             NC1754.2
100500 SUB-WRITE-F2-22.                                                 NC1754.2
100600     MOVE "SUB-TEST-F2-22 " TO PAR-NAME.                          NC1754.2
100700     PERFORM PRINT-DETAIL.                                        NC1754.2
100800*                                                                 NC1754.2
100900 SUB-INIT-F2-23.                                                  NC1754.2
101000*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC1754.2
101100     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1754.2
101200     MOVE     ZERO TO WRK-DS-10V00.                               NC1754.2
101300 SUB-TEST-F2-23.                                                  NC1754.2
101400     SUBTRACT A12ONES-DS-12V00                                    NC1754.2
101500              FROM ZERO GIVING WRK-DS-10V00                       NC1754.2
101600          NOT ON SIZE ERROR                                       NC1754.2
101700              MOVE   "NOT ON SIZE ERROR SHOULD NOT EXECUTED"      NC1754.2
101800                 TO RE-MARK                                       NC1754.2
101900              PERFORM FAIL GO TO SUB-WRITE-F2-23.                 NC1754.2
102000     GO       TO SUB-PASS-F2-23.                                  NC1754.2
102100 SUB-DELETE-F2-23.                                                NC1754.2
102200     PERFORM  DE-LETE.                                            NC1754.2
102300     GO       TO SUB-WRITE-F2-23.                                 NC1754.2
102400 SUB-PASS-F2-23.                                                  NC1754.2
102500     PERFORM  PASS.                                               NC1754.2
102600 SUB-WRITE-F2-23.                                                 NC1754.2
102700     MOVE     "SUB-TEST-F2-23" TO PAR-NAME.                       NC1754.2
102800     PERFORM  PRINT-DETAIL.                                       NC1754.2
102900*                                                                 NC1754.2
103000 SUB-INIT-F2-24.                                                  NC1754.2
103100*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC1754.2
103200     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1754.2
103300     MOVE     ZERO TO WRK-DS-06V06.                               NC1754.2
103400 SUB-TEST-F2-24.                                                  NC1754.2
103500     SUBTRACT A12THREES-DS-06V06                                  NC1754.2
103600              333333                                              NC1754.2
103700              A06THREES-DS-03V03                                  NC1754.2
103800              -.0000009 FROM 0000000                              NC1754.2
103900              GIVING WRK-DS-06V06 ROUNDED                         NC1754.2
104000          NOT ON SIZE ERROR                                       NC1754.2
104100              PERFORM  PASS                                       NC1754.2
104200              GO TO SUB-WRITE-F2-24.                              NC1754.2
104300     GO TO    SUB-FAIL-F2-24.                                     NC1754.2
104400 SUB-DELETE-F2-24.                                                NC1754.2
104500     PERFORM  DE-LETE.                                            NC1754.2
104600     GO       TO SUB-WRITE-F2-24.                                 NC1754.2
104700 SUB-FAIL-F2-24.                                                  NC1754.2
104800     MOVE    "NOT ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.   NC1754.2
104900     PERFORM  FAIL.                                               NC1754.2
105000 SUB-WRITE-F2-24.                                                 NC1754.2
105100     MOVE     "SUB-TEST-F2-24" TO PAR-NAME.                       NC1754.2
105200     PERFORM  PRINT-DETAIL.                                       NC1754.2
105300*                                                                 NC1754.2
105400 SUB-INIT-F2-25.                                                  NC1754.2
105500*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC1754.2
105600     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1754.2
105700     MOVE     ZERO TO WRK-DS-10V00.                               NC1754.2
105800 SUB-TEST-F2-25.                                                  NC1754.2
105900     SUBTRACT A12ONES-DS-12V00                                    NC1754.2
106000              FROM ZERO GIVING WRK-DS-10V00                       NC1754.2
106100           ON SIZE ERROR                                          NC1754.2
106200              GO TO   SUB-PASS-F2-25                              NC1754.2
106300       NOT ON SIZE ERROR                                          NC1754.2
106400              MOVE   "NOT ON SIZE ERROR SHOULD NOT EXECUTED"      NC1754.2
106500                 TO RE-MARK                                       NC1754.2
106600              PERFORM FAIL GO TO SUB-WRITE-F2-25.                 NC1754.2
106700 SUB-DELETE-F2-25.                                                NC1754.2
106800     PERFORM  DE-LETE.                                            NC1754.2
106900     GO       TO SUB-WRITE-F2-25.                                 NC1754.2
107000 SUB-PASS-F2-25.                                                  NC1754.2
107100     PERFORM  PASS.                                               NC1754.2
107200 SUB-WRITE-F2-25.                                                 NC1754.2
107300     MOVE     "SUB-TEST-F2-25" TO PAR-NAME.                       NC1754.2
107400     PERFORM  PRINT-DETAIL.                                       NC1754.2
107500*                                                                 NC1754.2
107600 SUB-INIT-F2-26.                                                  NC1754.2
107700*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC1754.2
107800     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1754.2
107900     MOVE     ZERO TO WRK-DS-06V06.                               NC1754.2
108000 SUB-TEST-F2-26.                                                  NC1754.2
108100     SUBTRACT A12THREES-DS-06V06                                  NC1754.2
108200              333333                                              NC1754.2
108300              A06THREES-DS-03V03                                  NC1754.2
108400            -.0000009 FROM 0000000                                NC1754.2
108500       GIVING WRK-DS-06V06 ROUNDED                                NC1754.2
108600           ON SIZE ERROR                                          NC1754.2
108700              GO TO    SUB-FAIL-F2-26                             NC1754.2
108800       NOT ON SIZE ERROR                                          NC1754.2
108900              PERFORM PASS                                        NC1754.2
109000              GO TO   SUB-WRITE-F2-26.                            NC1754.2
109100 SUB-DELETE-F2-26.                                                NC1754.2
109200     PERFORM  DE-LETE.                                            NC1754.2
109300     GO       TO SUB-WRITE-F2-26.                                 NC1754.2
109400 SUB-FAIL-F2-26.                                                  NC1754.2
109500     MOVE    "NOT ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.   NC1754.2
109600     PERFORM  FAIL.                                               NC1754.2
109700 SUB-WRITE-F2-26.                                                 NC1754.2
109800     MOVE     "SUB-TEST-F2-26" TO PAR-NAME.                       NC1754.2
109900     PERFORM  PRINT-DETAIL.                                       NC1754.2
110000*                                                                 NC1754.2
110100 SUB-INIT-F2-27.                                                  NC1754.2
110200*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
110300     MOVE   "VI-134 6.25.4 GR2" TO ANSI-REFERENCE.                NC1754.2
110400     MOVE   "SUB-TEST-F2-27"    TO PAR-NAME.                      NC1754.2
110500     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
110600     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
110700     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
110800     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
110900     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
111000     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
111100     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
111200     MOVE    ZERO  TO REC-CT.                                     NC1754.2
111300 SUB-TEST-F2-27-0.                                                NC1754.2
111400     SUBTRACT WRK-DU-2V0-1  2 WRK-DU-0V1-1 .04 FROM WRK-DS-2V2-1  NC1754.2
111500         GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4        NC1754.2
111600                WRK-NE-6 ROUNDED                                  NC1754.2
111700     GO TO   SUB-TEST-F2-27-1.                                    NC1754.2
111800 SUB-DELETE-F2-27.                                                NC1754.2
111900     PERFORM DE-LETE.                                             NC1754.2
112000     PERFORM PRINT-DETAIL.                                        NC1754.2
112100     GO TO   SUB-INIT-F2-28.                                      NC1754.2
112200 SUB-TEST-F2-27-1.                                                NC1754.2
112300     MOVE    "SUB-TEST-F2-27-1" TO PAR-NAME.                      NC1754.2
112400     MOVE 1 TO REC-CT.                                            NC1754.2
112500     IF WRK-DS-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1754.2
112600     ELSE                                                         NC1754.2
112700     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE 09.99      NC1754.2
112800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
112900     ADD 1 TO REC-CT.                                             NC1754.2
113000 SUB-TEST-F2-27-2.                                                NC1754.2
113100     MOVE    "SUB-TEST-F2-27-2" TO PAR-NAME.                      NC1754.2
113200     IF WRK-DS-2V1-1 = 10.0 PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
113300     ELSE                                                         NC1754.2
113400     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE 10.0       NC1754.2
113500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
113600     ADD 1 TO REC-CT.                                             NC1754.2
113700 SUB-TEST-F2-27-3.                                                NC1754.2
113800     MOVE    "SUB-TEST-F2-27-3" TO PAR-NAME.                      NC1754.2
113900     IF WRK-NE-4 = "$*9.99" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
114000     ELSE                                                         NC1754.2
114100     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*0.00" TO    NC1754.2
114200     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
114300     ADD 1 TO REC-CT.                                             NC1754.2
114400 SUB-TEST-F2-27-4.                                                NC1754.2
114500     MOVE    "SUB-TEST-F2-27-4" TO PAR-NAME.                      NC1754.2
114600     IF WRK-NE-6 = "$*9.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1754.2
114700     ELSE                                                         NC1754.2
114800     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "$*9.99  " TO  NC1754.2
114900     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
115000*                                                                 NC1754.2
115100 SUB-INIT-F2-28.                                                  NC1754.2
115200*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
115300*    ==-->          SIZE ERROR        <--==                       NC1754.2
115400     MOVE   "VI-67 6.4.2"       TO ANSI-REFERENCE.                NC1754.2
115500     MOVE   "SUB-TEST-F2-28"    TO PAR-NAME.                      NC1754.2
115600     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
115700     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
115800     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
115900     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
116000     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
116100     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
116200     MOVE    ZERO  TO WRK-NE-5                                    NC1754.2
116300     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
116400     MOVE    ZERO  TO REC-CT.                                     NC1754.2
116500     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
116600 SUB-TEST-F2-28-0.                                                NC1754.2
116700     SUBTRACT A16TWOS-DS-16V00                                    NC1754.2
116800              2 WRK-DU-0V1-1 .04                                  NC1754.2
116900         FROM WRK-DS-2V2-1                                        NC1754.2
117000       GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4          NC1754.2
117100              WRK-NE-5 WRK-NE-6 ROUNDED                           NC1754.2
117200           ON SIZE ERROR                                          NC1754.2
117300              MOVE   "A" TO SIZE-ERR2.                            NC1754.2
117400     GO TO   SUB-TEST-F2-28-1.                                    NC1754.2
117500 SUB-DELETE-F2-28.                                                NC1754.2
117600     PERFORM DE-LETE.                                             NC1754.2
117700     PERFORM PRINT-DETAIL.                                        NC1754.2
117800     GO TO   SUB-INIT-F2-29.                                      NC1754.2
117900 SUB-TEST-F2-28-1.                                                NC1754.2
118000     MOVE    "SUB-TEST-F2-28-1" TO PAR-NAME.                      NC1754.2
118100     MOVE 1 TO REC-CT.                                            NC1754.2
118200     IF WRK-DS-2V2-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
118300     ELSE                                                         NC1754.2
118400     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE ZERO       NC1754.2
118500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
118600     ADD 1 TO REC-CT.                                             NC1754.2
118700 SUB-TEST-F2-28-2.                                                NC1754.2
118800     MOVE    "SUB-TEST-F2-28-2" TO PAR-NAME.                      NC1754.2
118900     IF WRK-DS-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
119000     ELSE                                                         NC1754.2
119100     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE ZERO       NC1754.2
119200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
119300     ADD 1 TO REC-CT.                                             NC1754.2
119400 SUB-TEST-F2-28-3.                                                NC1754.2
119500     MOVE    "SUB-TEST-F2-28-3" TO PAR-NAME.                      NC1754.2
119600     IF WRK-NE-4 = "$*0.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
119700     ELSE                                                         NC1754.2
119800     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*0.00" TO    NC1754.2
119900     CORRECT-X PERFORM PRINT-DETAIL.                              NC1754.2
120000     ADD 1 TO REC-CT.                                             NC1754.2
120100 SUB-TEST-F2-28-4.                                                NC1754.2
120200     MOVE    "SUB-TEST-F2-28-4" TO PAR-NAME.                      NC1754.2
120300     IF WRK-NE-5 = "*.**" PERFORM PASS PERFORM PRINT-DETAIL       NC1754.2
120400     ELSE                                                         NC1754.2
120500     PERFORM FAIL MOVE WRK-NE-5 TO COMPUTED-A MOVE "*.**"         NC1754.2
120600     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
120700     ADD 1 TO REC-CT.                                             NC1754.2
120800 SUB-TEST-F2-28-5.                                                NC1754.2
120900     MOVE   "SUB-TEST-F2-28-5" TO PAR-NAME.                       NC1754.2
121000     IF WRK-NE-6 = "***.****"                                     NC1754.2
121100           PERFORM PASS  PERFORM PRINT-DETAIL                     NC1754.2
121200     ELSE                                                         NC1754.2
121300     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "***.****"     NC1754.2
121400     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
121500     ADD 1 TO REC-CT.                                             NC1754.2
121600 SUB-TEST-F2-28-6.                                                NC1754.2
121700     MOVE   "SUB-TEST-F2-28-6" TO PAR-NAME.                       NC1754.2
121800     IF      SIZE-ERR2 = "A"                                      NC1754.2
121900             PERFORM PASS                                         NC1754.2
122000             PERFORM PRINT-DETAIL                                 NC1754.2
122100     ELSE                                                         NC1754.2
122200             MOVE   "A"        TO CORRECT-X                       NC1754.2
122300             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
122400             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1754.2
122500                  TO RE-MARK                                      NC1754.2
122600             PERFORM FAIL                                         NC1754.2
122700             PERFORM PRINT-DETAIL.                                NC1754.2
122800*                                                                 NC1754.2
122900 SUB-INIT-F2-29.                                                  NC1754.2
123000*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
123100*    ==-->       NO SIZE ERROR        <--==                       NC1754.2
123200     MOVE   "VI-67 6.4.2"       TO ANSI-REFERENCE.                NC1754.2
123300     MOVE   "SUB-TEST-F2-29"    TO PAR-NAME.                      NC1754.2
123400     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
123500     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
123600     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
123700     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
123800     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
123900     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
124000     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
124100     MOVE    ZERO  TO REC-CT.                                     NC1754.2
124200     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
124300 SUB-TEST-F2-29-0.                                                NC1754.2
124400     SUBTRACT WRK-DU-2V0-1  2 WRK-DU-0V1-1 .04 FROM WRK-DS-2V2-1  NC1754.2
124500         GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4        NC1754.2
124600                WRK-NE-6 ROUNDED                                  NC1754.2
124700          ON SIZE ERROR                                           NC1754.2
124800             MOVE   "A" TO SIZE-ERR2.                             NC1754.2
124900     GO TO   SUB-TEST-F2-29-1.                                    NC1754.2
125000 SUB-DELETE-F2-29.                                                NC1754.2
125100     PERFORM DE-LETE.                                             NC1754.2
125200     PERFORM PRINT-DETAIL.                                        NC1754.2
125300     GO TO   SUB-INIT-F2-30.                                      NC1754.2
125400 SUB-TEST-F2-29-1.                                                NC1754.2
125500     MOVE    "SUB-TEST-F2-29-1" TO PAR-NAME.                      NC1754.2
125600     MOVE 1 TO REC-CT.                                            NC1754.2
125700     IF WRK-DS-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1754.2
125800     ELSE                                                         NC1754.2
125900     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE 09.99      NC1754.2
126000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
126100     ADD 1 TO REC-CT.                                             NC1754.2
126200 SUB-TEST-F2-29-2.                                                NC1754.2
126300     MOVE    "SUB-TEST-F2-29-2" TO PAR-NAME.                      NC1754.2
126400     IF WRK-DS-2V1-1 = 10.0 PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
126500     ELSE                                                         NC1754.2
126600     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE 10.0       NC1754.2
126700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
126800     ADD 1 TO REC-CT.                                             NC1754.2
126900 SUB-TEST-F2-29-3.                                                NC1754.2
127000     MOVE    "SUB-TEST-F2-29-3" TO PAR-NAME.                      NC1754.2
127100     IF WRK-NE-4 = "$*9.99" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
127200     ELSE                                                         NC1754.2
127300     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*9.99" TO    NC1754.2
127400     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
127500     ADD 1 TO REC-CT.                                             NC1754.2
127600 SUB-TEST-F2-29-4.                                                NC1754.2
127700     MOVE    "SUB-TEST-F2-29-4" TO PAR-NAME.                      NC1754.2
127800     IF WRK-NE-6 = "$*9.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1754.2
127900     ELSE                                                         NC1754.2
128000     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "$*9.99  " TO  NC1754.2
128100     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
128200     ADD 1 TO REC-CT.                                             NC1754.2
128300 SUB-TEST-F2-29-5.                                                NC1754.2
128400     MOVE    "SUB-TEST-F2-29-5" TO PAR-NAME.                      NC1754.2
128500     IF      SIZE-ERR2  = SPACE                                   NC1754.2
128600             PERFORM PASS                                         NC1754.2
128700             PERFORM PRINT-DETAIL                                 NC1754.2
128800     ELSE                                                         NC1754.2
128900             MOVE    SPACE     TO CORRECT-X                       NC1754.2
129000             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
129100             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1754.2
129200                  TO RE-MARK                                      NC1754.2
129300             PERFORM FAIL                                         NC1754.2
129400             PERFORM PRINT-DETAIL.                                NC1754.2
129500*                                                                 NC1754.2
129600 SUB-INIT-F2-30.                                                  NC1754.2
129700*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
129800*    ==-->          SIZE ERROR        <--==                       NC1754.2
129900     MOVE   "VI-67 6.4.2"       TO ANSI-REFERENCE.                NC1754.2
130000     MOVE   "SUB-TEST-F2-30"    TO PAR-NAME.                      NC1754.2
130100     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
130200     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
130300     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
130400     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
130500     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
130600     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
130700     MOVE    ZERO  TO WRK-NE-5                                    NC1754.2
130800     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
130900     MOVE    ZERO  TO REC-CT.                                     NC1754.2
131000     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
131100 SUB-TEST-F2-30-0.                                                NC1754.2
131200     SUBTRACT A16TWOS-DS-16V00                                    NC1754.2
131300              2 WRK-DU-0V1-1 .04                                  NC1754.2
131400         FROM WRK-DS-2V2-1                                        NC1754.2
131500       GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4          NC1754.2
131600              WRK-NE-5 WRK-NE-6 ROUNDED                           NC1754.2
131700       NOT ON SIZE ERROR                                          NC1754.2
131800              MOVE   "A" TO SIZE-ERR2.                            NC1754.2
131900     GO TO   SUB-TEST-F2-30-1.                                    NC1754.2
132000 SUB-DELETE-F2-30.                                                NC1754.2
132100     PERFORM DE-LETE.                                             NC1754.2
132200     PERFORM PRINT-DETAIL.                                        NC1754.2
132300     GO TO   SUB-INIT-F2-31.                                      NC1754.2
132400 SUB-TEST-F2-30-1.                                                NC1754.2
132500     MOVE    "SUB-TEST-F2-30-1" TO PAR-NAME.                      NC1754.2
132600     MOVE 1 TO REC-CT.                                            NC1754.2
132700     IF WRK-DS-2V2-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
132800     ELSE                                                         NC1754.2
132900     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE ZERO       NC1754.2
133000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
133100     ADD 1 TO REC-CT.                                             NC1754.2
133200 SUB-TEST-F2-30-2.                                                NC1754.2
133300     MOVE    "SUB-TEST-F2-30-2" TO PAR-NAME.                      NC1754.2
133400     IF WRK-DS-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
133500     ELSE                                                         NC1754.2
133600     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE ZERO       NC1754.2
133700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
133800     ADD 1 TO REC-CT.                                             NC1754.2
133900 SUB-TEST-F2-30-3.                                                NC1754.2
134000     MOVE    "SUB-TEST-F2-30-3" TO PAR-NAME.                      NC1754.2
134100     IF WRK-NE-4 = "$*0.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
134200     ELSE                                                         NC1754.2
134300     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*0.00" to    NC1754.2
134400     CORRECT-X PERFORM PRINT-DETAIL.                              NC1754.2
134500     ADD 1 TO REC-CT.                                             NC1754.2
134600 SUB-TEST-F2-30-4.                                                NC1754.2
134700     MOVE    "SUB-TEST-F2-30-4" TO PAR-NAME.                      NC1754.2
134800     IF WRK-NE-5 = "*.**" PERFORM PASS PERFORM PRINT-DETAIL       NC1754.2
134900     ELSE                                                         NC1754.2
135000     PERFORM FAIL MOVE WRK-NE-5 TO COMPUTED-A MOVE "*.**"         NC1754.2
135100     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
135200     ADD 1 TO REC-CT.                                             NC1754.2
135300 SUB-TEST-F2-30-5.                                                NC1754.2
135400     MOVE   "SUB-TEST-F2-30-5" TO PAR-NAME.                       NC1754.2
135500     IF WRK-NE-6 = "***.****"                                     NC1754.2
135600        PERFORM PASS PERFORM PRINT-DETAIL                         NC1754.2
135700     ELSE                                                         NC1754.2
135800     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "***.****"     NC1754.2
135900     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
136000     ADD 1 TO REC-CT.                                             NC1754.2
136100 SUB-TEST-F2-30-6.                                                NC1754.2
136200     MOVE   "SUB-TEST-F2-30-6" TO PAR-NAME.                       NC1754.2
136300     IF      SIZE-ERR2 = SPACE                                    NC1754.2
136400             PERFORM PASS                                         NC1754.2
136500             PERFORM PRINT-DETAIL                                 NC1754.2
136600     ELSE                                                         NC1754.2
136700             MOVE    SPACE     TO CORRECT-X                       NC1754.2
136800             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
136900             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1754.2
137000                  TO RE-MARK                                      NC1754.2
137100             PERFORM FAIL                                         NC1754.2
137200             PERFORM PRINT-DETAIL.                                NC1754.2
137300*                                                                 NC1754.2
137400 SUB-INIT-F2-31.                                                  NC1754.2
137500*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
137600*    ==-->       NO SIZE ERROR        <--==                       NC1754.2
137700     MOVE   "VI-67 6.4.2"       TO ANSI-REFERENCE.                NC1754.2
137800     MOVE   "SUB-TEST-F2-31"    TO PAR-NAME.                      NC1754.2
137900     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
138000     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
138100     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
138200     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
138300     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
138400     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
138500     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
138600     MOVE    ZERO  TO REC-CT.                                     NC1754.2
138700     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
138800 SUB-TEST-F2-31-0.                                                NC1754.2
138900     SUBTRACT WRK-DU-2V0-1  2 WRK-DU-0V1-1 .04 FROM WRK-DS-2V2-1  NC1754.2
139000         GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4        NC1754.2
139100                WRK-NE-6 ROUNDED                                  NC1754.2
139200      NOT ON SIZE ERROR                                           NC1754.2
139300             MOVE   "A" TO SIZE-ERR2.                             NC1754.2
139400     GO TO   SUB-TEST-F2-31-1.                                    NC1754.2
139500 SUB-DELETE-F2-31.                                                NC1754.2
139600     PERFORM DE-LETE.                                             NC1754.2
139700     PERFORM PRINT-DETAIL.                                        NC1754.2
139800     GO TO   SUB-INIT-F2-32.                                      NC1754.2
139900 SUB-TEST-F2-31-1.                                                NC1754.2
140000     MOVE    "SUB-TEST-F2-31-1" TO PAR-NAME.                      NC1754.2
140100     MOVE 1 TO REC-CT.                                            NC1754.2
140200     IF WRK-DS-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1754.2
140300     ELSE                                                         NC1754.2
140400     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE 09.99      NC1754.2
140500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
140600     ADD 1 TO REC-CT.                                             NC1754.2
140700 SUB-TEST-F2-31-2.                                                NC1754.2
140800     MOVE    "SUB-TEST-F2-31-2" TO PAR-NAME.                      NC1754.2
140900     IF WRK-DS-2V1-1 = 10.0 PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
141000     ELSE                                                         NC1754.2
141100     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE 10.0       NC1754.2
141200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
141300     ADD 1 TO REC-CT.                                             NC1754.2
141400 SUB-TEST-F2-31-3.                                                NC1754.2
141500     MOVE    "SUB-TEST-F2-31-3" TO PAR-NAME.                      NC1754.2
141600     IF WRK-NE-4 = "$*9.99" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
141700     ELSE                                                         NC1754.2
141800     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*9.99" TO    NC1754.2
141900     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
142000     ADD 1 TO REC-CT.                                             NC1754.2
142100 SUB-TEST-F2-31-4.                                                NC1754.2
142200     MOVE    "SUB-TEST-F2-31-4" TO PAR-NAME.                      NC1754.2
142300     IF WRK-NE-6 = "$*9.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1754.2
142400     ELSE                                                         NC1754.2
142500     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "$*9.99  " TO  NC1754.2
142600     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
142700     ADD 1 TO REC-CT.                                             NC1754.2
142800 SUB-TEST-F2-31-5.                                                NC1754.2
142900     MOVE    "SUB-TEST-F2-31-5" TO PAR-NAME.                      NC1754.2
143000     IF      SIZE-ERR2  = "A"                                     NC1754.2
143100             PERFORM PASS                                         NC1754.2
143200             PERFORM PRINT-DETAIL                                 NC1754.2
143300     ELSE                                                         NC1754.2
143400             MOVE   "A"        TO CORRECT-X                       NC1754.2
143500             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
143600             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1754.2
143700                  TO RE-MARK                                      NC1754.2
143800             PERFORM FAIL                                         NC1754.2
143900             PERFORM PRINT-DETAIL.                                NC1754.2
144000*                                                                 NC1754.2
144100 SUB-INIT-F2-32.                                                  NC1754.2
144200*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
144300*    ==-->          SIZE ERROR        <--==                       NC1754.2
144400     MOVE   "VI-67 6.4.2"       TO ANSI-REFERENCE.                NC1754.2
144500     MOVE   "SUB-TEST-F2-32"    TO PAR-NAME.                      NC1754.2
144600     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
144700     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
144800     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
144900     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
145000     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
145100     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
145200     MOVE    ZERO  TO WRK-NE-5                                    NC1754.2
145300     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
145400     MOVE    ZERO  TO REC-CT.                                     NC1754.2
145500     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
145600 SUB-TEST-F2-32-0.                                                NC1754.2
145700     SUBTRACT A16TWOS-DS-16V00                                    NC1754.2
145800              2 WRK-DU-0V1-1 .04                                  NC1754.2
145900         FROM WRK-DS-2V2-1                                        NC1754.2
146000       GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4          NC1754.2
146100              WRK-NE-5 WRK-NE-6 ROUNDED                           NC1754.2
146200           ON SIZE ERROR                                          NC1754.2
146300              MOVE   "A" TO SIZE-ERR2                             NC1754.2
146400       NOT ON SIZE ERROR                                          NC1754.2
146500              MOVE   "B" TO SIZE-ERR2.                            NC1754.2
146600     GO TO   SUB-TEST-F2-32-1.                                    NC1754.2
146700 SUB-DELETE-F2-32.                                                NC1754.2
146800     PERFORM DE-LETE.                                             NC1754.2
146900     PERFORM PRINT-DETAIL.                                        NC1754.2
147000     GO TO   SUB-INIT-F2-33.                                      NC1754.2
147100 SUB-TEST-F2-32-1.                                                NC1754.2
147200     MOVE    "SUB-TEST-F2-32-1" TO PAR-NAME.                      NC1754.2
147300     MOVE 1 TO REC-CT.                                            NC1754.2
147400     IF WRK-DS-2V2-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
147500     ELSE                                                         NC1754.2
147600     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE ZERO       NC1754.2
147700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
147800     ADD 1 TO REC-CT.                                             NC1754.2
147900 SUB-TEST-F2-32-2.                                                NC1754.2
148000     MOVE    "SUB-TEST-F2-32-2" TO PAR-NAME.                      NC1754.2
148100     IF WRK-DS-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
148200     ELSE                                                         NC1754.2
148300     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE ZERO       NC1754.2
148400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
148500     ADD 1 TO REC-CT.                                             NC1754.2
148600 SUB-TEST-F2-32-3.                                                NC1754.2
148700     MOVE    "SUB-TEST-F2-32-3" TO PAR-NAME.                      NC1754.2
148800     IF WRK-NE-4 = "$*0.00" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
148900     ELSE                                                         NC1754.2
149000     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*0.00"       NC1754.2
149100     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
149200     ADD 1 TO REC-CT.                                             NC1754.2
149300 SUB-TEST-F2-32-4.                                                NC1754.2
149400     MOVE    "SUB-TEST-F2-32-4" TO PAR-NAME.                      NC1754.2
149500     IF WRK-NE-5 = "*.**" PERFORM PASS PERFORM PRINT-DETAIL       NC1754.2
149600     ELSE                                                         NC1754.2
149700     PERFORM FAIL MOVE WRK-NE-5 TO COMPUTED-A MOVE "*.**"         NC1754.2
149800     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
149900     ADD 1 TO REC-CT.                                             NC1754.2
150000 SUB-TEST-F2-32-5.                                                NC1754.2
150100     MOVE   "SUB-TEST-F2-32-5" TO PAR-NAME.                       NC1754.2
150200     IF WRK-NE-6 = "***.****"                                     NC1754.2
150300        PERFORM PASS  PERFORM PRINT-DETAIL                        NC1754.2
150400     ELSE                                                         NC1754.2
150500     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "***.****"     NC1754.2
150600     TO CORRECT-X PERFORM PRINT-DETAIL.                           NC1754.2
150700     ADD 1 TO REC-CT.                                             NC1754.2
150800 SUB-TEST-F2-32-6.                                                NC1754.2
150900     MOVE   "SUB-TEST-F2-32-6" TO PAR-NAME.                       NC1754.2
151000     IF      SIZE-ERR2 = "A"                                      NC1754.2
151100             PERFORM PASS                                         NC1754.2
151200             PERFORM PRINT-DETAIL                                 NC1754.2
151300     ELSE                                                         NC1754.2
151400             MOVE   "A"        TO CORRECT-X                       NC1754.2
151500             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
151600             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1754.2
151700                  TO RE-MARK                                      NC1754.2
151800             PERFORM FAIL                                         NC1754.2
151900             PERFORM PRINT-DETAIL.                                NC1754.2
152000*                                                                 NC1754.2
152100 SUB-INIT-F2-33.                                                  NC1754.2
152200*    ==-->   MULTIPLE RESULT FIELDS   <--==                       NC1754.2
152300*    ==-->       NO SIZE ERROR        <--==                       NC1754.2
152400     MOVE   "VI-67 6.4.2"       TO ANSI-REFERENCE.                NC1754.2
152500     MOVE   "SUB-TEST-F2-33"    TO PAR-NAME.                      NC1754.2
152600     MOVE    10    TO WRK-DU-2V0-1.                               NC1754.2
152700     MOVE    22.33 TO WRK-DS-2V2-1.                               NC1754.2
152800     MOVE    .3    TO WRK-DU-0V1-1.                               NC1754.2
152900     MOVE    ZERO  TO WRK-DS-2V2-2.                               NC1754.2
153000     MOVE    ZERO  TO WRK-DS-2V1-1.                               NC1754.2
153100     MOVE    ZERO  TO WRK-NE-4.                                   NC1754.2
153200     MOVE    ZERO  TO WRK-NE-6.                                   NC1754.2
153300     MOVE    ZERO  TO REC-CT.                                     NC1754.2
153400     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
153500 SUB-TEST-F2-33-0.                                                NC1754.2
153600     SUBTRACT WRK-DU-2V0-1  2 WRK-DU-0V1-1 .04 FROM WRK-DS-2V2-1  NC1754.2
153700         GIVING WRK-DS-2V2-2 WRK-DS-2V1-1 ROUNDED WRK-NE-4        NC1754.2
153800                WRK-NE-6 ROUNDED                                  NC1754.2
153900          ON SIZE ERROR                                           NC1754.2
154000             MOVE   "A" TO SIZE-ERR2                              NC1754.2
154100      NOT ON SIZE ERROR                                           NC1754.2
154200             MOVE   "B" TO SIZE-ERR2.                             NC1754.2
154300     GO TO   SUB-TEST-F2-33-1.                                    NC1754.2
154400 SUB-DELETE-F2-33.                                                NC1754.2
154500     PERFORM DE-LETE.                                             NC1754.2
154600     PERFORM PRINT-DETAIL.                                        NC1754.2
154700     GO TO   SUB-INIT-F2-34.                                      NC1754.2
154800 SUB-TEST-F2-33-1.                                                NC1754.2
154900     MOVE    "SUB-TEST-F2-33-1" TO PAR-NAME.                      NC1754.2
155000     MOVE 1 TO REC-CT.                                            NC1754.2
155100     IF WRK-DS-2V2-2 = 09.99 PERFORM PASS PERFORM PRINT-DETAIL    NC1754.2
155200     ELSE                                                         NC1754.2
155300     PERFORM FAIL MOVE WRK-DS-2V2-2 TO COMPUTED-N MOVE 09.99      NC1754.2
155400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
155500     ADD 1 TO REC-CT.                                             NC1754.2
155600 SUB-TEST-F2-33-2.                                                NC1754.2
155700     MOVE    "SUB-TEST-F2-33-2" TO PAR-NAME.                      NC1754.2
155800     IF WRK-DS-2V1-1 = 10.0 PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
155900     ELSE                                                         NC1754.2
156000     PERFORM FAIL MOVE WRK-DS-2V1-1 TO COMPUTED-N MOVE 10.0       NC1754.2
156100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1754.2
156200     ADD 1 TO REC-CT.                                             NC1754.2
156300 SUB-TEST-F2-33-3.                                                NC1754.2
156400     MOVE    "SUB-TEST-F2-33-3" TO PAR-NAME.                      NC1754.2
156500     IF WRK-NE-4 = "$*9.99" PERFORM PASS PERFORM PRINT-DETAIL     NC1754.2
156600     ELSE                                                         NC1754.2
156700     PERFORM FAIL MOVE WRK-NE-4 TO COMPUTED-A MOVE "$*9.99" TO    NC1754.2
156800     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
156900     ADD 1 TO REC-CT.                                             NC1754.2
157000 SUB-TEST-F2-33-4.                                                NC1754.2
157100     MOVE    "SUB-TEST-F2-33-4" TO PAR-NAME.                      NC1754.2
157200     IF WRK-NE-6 = "$*9.99  " PERFORM PASS PERFORM PRINT-DETAIL   NC1754.2
157300     ELSE                                                         NC1754.2
157400     PERFORM FAIL MOVE WRK-NE-6 TO COMPUTED-A MOVE "$*9.99  " TO  NC1754.2
157500     CORRECT-A PERFORM PRINT-DETAIL.                              NC1754.2
157600     ADD 1 TO REC-CT.                                             NC1754.2
157700 SUB-TEST-F2-33-5.                                                NC1754.2
157800     MOVE    "SUB-TEST-F2-33-5" TO PAR-NAME.                      NC1754.2
157900     IF      SIZE-ERR2  = "B"                                     NC1754.2
158000             PERFORM PASS                                         NC1754.2
158100             PERFORM PRINT-DETAIL                                 NC1754.2
158200     ELSE                                                         NC1754.2
158300             MOVE   "B"        TO CORRECT-X                       NC1754.2
158400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
158500             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1754.2
158600                  TO RE-MARK                                      NC1754.2
158700             PERFORM FAIL                                         NC1754.2
158800             PERFORM PRINT-DETAIL.                                NC1754.2
158900*                                                                 NC1754.2
159000 SUB-INIT-F2-34.                                                  NC1754.2
159100*    ==-->  EXPLICIT SCOPE TERMINATOR <--==                       NC1754.2
159200*    ==-->          SIZE ERROR        <--==                       NC1754.2
159300     MOVE   "IV-41 6.4.3"       TO ANSI-REFERENCE.                NC1754.2
159400     MOVE     ZERO TO WRK-DS-10V00.                               NC1754.2
159500     MOVE    SPACE TO WRK-XN-00001.                               NC1754.2
159600     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
159700     MOVE    SPACE TO SIZE-ERR3.                                  NC1754.2
159800     MOVE    SPACE TO SIZE-ERR4.                                  NC1754.2
159900     MOVE    1  TO REC-CT.                                        NC1754.2
160000 SUB-TEST-F2-34-0.                                                NC1754.2
160100     SUBTRACT A12ONES-DS-12V00                                    NC1754.2
160200        FROM ZERO GIVING WRK-DS-10V00                             NC1754.2
160300          ON SIZE ERROR                                           NC1754.2
160400             MOVE   "1" TO WRK-XN-00001                           NC1754.2
160500             MOVE   "A" TO SIZE-ERR2                              NC1754.2
160600             MOVE   "B" TO SIZE-ERR3                              NC1754.2
160700     END-SUBTRACT                                                 NC1754.2
160800     MOVE   "C" TO SIZE-ERR4.                                     NC1754.2
160900     GO TO   SUB-TEST-F2-34-1.                                    NC1754.2
161000 SUB-DELETE-F2-34.                                                NC1754.2
161100     PERFORM DE-LETE.                                             NC1754.2
161200     PERFORM PRINT-DETAIL.                                        NC1754.2
161300     GO TO   SUB-INIT-F2-35.                                      NC1754.2
161400 SUB-TEST-F2-34-1.                                                NC1754.2
161500     MOVE   "SUB-TEST-F2-34-1" TO PAR-NAME.                       NC1754.2
161600     IF      WRK-XN-00001 = "1"                                   NC1754.2
161700             PERFORM PASS                                         NC1754.2
161800             PERFORM PRINT-DETAIL                                 NC1754.2
161900     ELSE                                                         NC1754.2
162000             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1754.2
162100                  TO RE-MARK                                      NC1754.2
162200             MOVE   "1"           TO CORRECT-X                    NC1754.2
162300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1754.2
162400             PERFORM FAIL                                         NC1754.2
162500             PERFORM PRINT-DETAIL.                                NC1754.2
162600     ADD     1 TO REC-CT.                                         NC1754.2
162700 SUB-TEST-F2-34-2.                                                NC1754.2
162800     MOVE   "SUB-TEST-F2-34-2" TO PAR-NAME.                       NC1754.2
162900     IF      SIZE-ERR2 = "A"                                      NC1754.2
163000             PERFORM PASS                                         NC1754.2
163100             PERFORM PRINT-DETAIL                                 NC1754.2
163200     ELSE                                                         NC1754.2
163300             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1754.2
163400                  TO RE-MARK                                      NC1754.2
163500             MOVE   "A"        TO CORRECT-X                       NC1754.2
163600             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
163700             PERFORM FAIL                                         NC1754.2
163800             PERFORM PRINT-DETAIL.                                NC1754.2
163900     ADD     1 TO REC-CT.                                         NC1754.2
164000 SUB-TEST-F2-34-3.                                                NC1754.2
164100     MOVE   "SUB-TEST-F2-34-3" TO PAR-NAME.                       NC1754.2
164200     IF      SIZE-ERR3 = "B"                                      NC1754.2
164300             PERFORM PASS                                         NC1754.2
164400             PERFORM PRINT-DETAIL                                 NC1754.2
164500     ELSE                                                         NC1754.2
164600             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1754.2
164700                  TO RE-MARK                                      NC1754.2
164800             MOVE   "B"        TO CORRECT-X                       NC1754.2
164900             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1754.2
165000             PERFORM FAIL                                         NC1754.2
165100             PERFORM PRINT-DETAIL.                                NC1754.2
165200     ADD     1 TO REC-CT.                                         NC1754.2
165300 SUB-TEST-F2-34-4.                                                NC1754.2
165400     MOVE   "SUB-TEST-F2-34-4" TO PAR-NAME.                       NC1754.2
165500     IF      SIZE-ERR4 = "C"                                      NC1754.2
165600             PERFORM PASS                                         NC1754.2
165700             PERFORM PRINT-DETAIL                                 NC1754.2
165800     ELSE                                                         NC1754.2
165900             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1754.2
166000                  TO RE-MARK                                      NC1754.2
166100             MOVE   "C"        TO CORRECT-X                       NC1754.2
166200             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1754.2
166300             PERFORM FAIL                                         NC1754.2
166400             PERFORM PRINT-DETAIL                                 NC1754.2
166500             ADD     1 TO REC-CT.                                 NC1754.2
166600 SUB-TEST-F2-34-5.                                                NC1754.2
166700     MOVE   "SUB-TEST-F2-34-5" TO PAR-NAME.                       NC1754.2
166800     IF      WRK-DS-10V00 = ZERO                                  NC1754.2
166900             PERFORM PASS                                         NC1754.2
167000             PERFORM PRINT-DETAIL                                 NC1754.2
167100     ELSE                                                         NC1754.2
167200             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1754.2
167300                  TO RE-MARK                                      NC1754.2
167400             MOVE    ZERO         TO CORRECT-N                    NC1754.2
167500             MOVE    WRK-DS-10V00 TO COMPUTED-N                   NC1754.2
167600             PERFORM FAIL                                         NC1754.2
167700             PERFORM PRINT-DETAIL.                                NC1754.2
167800*                                                                 NC1754.2
167900 SUB-INIT-F2-35.                                                  NC1754.2
168000*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1754.2
168100     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1754.2
168200     MOVE   "SUB-TEST-F2-35"  TO PAR-NAME.                        NC1754.2
168300     MOVE    SPACE TO WRK-XN-00001.                               NC1754.2
168400     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
168500     MOVE    SPACE TO SIZE-ERR3.                                  NC1754.2
168600     MOVE    SPACE TO SIZE-ERR4.                                  NC1754.2
168700     MOVE    ZERO  TO WRK-DS-06V06.                               NC1754.2
168800     MOVE    1     TO REC-CT.                                     NC1754.2
168900 SUB-TEST-F2-35-0.                                                NC1754.2
169000     SUBTRACT A12THREES-DS-06V06                                  NC1754.2
169100              333333                                              NC1754.2
169200              A06THREES-DS-03V03                                  NC1754.2
169300              -.0000009 FROM 0000000                              NC1754.2
169400      GIVING WRK-DS-06V06 ROUNDED                                 NC1754.2
169500          ON SIZE ERROR                                           NC1754.2
169600             MOVE   "1" TO WRK-XN-00001                           NC1754.2
169700             MOVE   "A" TO SIZE-ERR2                              NC1754.2
169800             MOVE   "B" TO SIZE-ERR3                              NC1754.2
169900     END-SUBTRACT                                                 NC1754.2
170000     MOVE   "C" TO SIZE-ERR4.                                     NC1754.2
170100     GO TO   SUB-TEST-F2-35-1.                                    NC1754.2
170200 SUB-DELETE-F2-35.                                                NC1754.2
170300     PERFORM DE-LETE.                                             NC1754.2
170400     PERFORM PRINT-DETAIL.                                        NC1754.2
170500     GO TO   SUB-INIT-F2-36.                                      NC1754.2
170600 SUB-TEST-F2-35-1.                                                NC1754.2
170700     MOVE   "SUB-TEST-F2-35-1" TO PAR-NAME.                       NC1754.2
170800     IF      WRK-XN-00001 = SPACE                                 NC1754.2
170900             PERFORM PASS                                         NC1754.2
171000             PERFORM PRINT-DETAIL                                 NC1754.2
171100     ELSE                                                         NC1754.2
171200             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1754.2
171300                  TO RE-MARK                                      NC1754.2
171400             MOVE    SPACE        TO CORRECT-X                    NC1754.2
171500             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1754.2
171600             PERFORM FAIL                                         NC1754.2
171700             PERFORM PRINT-DETAIL.                                NC1754.2
171800     ADD     1 TO REC-CT.                                         NC1754.2
171900 SUB-TEST-F2-35-2.                                                NC1754.2
172000     MOVE   "SUB-TEST-F2-35-2" TO PAR-NAME.                       NC1754.2
172100     IF      SIZE-ERR2 = SPACE                                    NC1754.2
172200             PERFORM PASS                                         NC1754.2
172300             PERFORM PRINT-DETAIL                                 NC1754.2
172400     ELSE                                                         NC1754.2
172500             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1754.2
172600                  TO RE-MARK                                      NC1754.2
172700             MOVE    SPACE     TO CORRECT-X                       NC1754.2
172800             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
172900             PERFORM FAIL                                         NC1754.2
173000             PERFORM PRINT-DETAIL.                                NC1754.2
173100     ADD     1 TO REC-CT.                                         NC1754.2
173200 SUB-TEST-F2-35-3.                                                NC1754.2
173300     MOVE   "SUB-TEST-F2-35-3" TO PAR-NAME.                       NC1754.2
173400     IF      SIZE-ERR3 = SPACE                                    NC1754.2
173500             PERFORM PASS                                         NC1754.2
173600             PERFORM PRINT-DETAIL                                 NC1754.2
173700     ELSE                                                         NC1754.2
173800             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1754.2
173900                  TO RE-MARK                                      NC1754.2
174000             MOVE    SPACE     TO CORRECT-X                       NC1754.2
174100             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1754.2
174200             PERFORM FAIL                                         NC1754.2
174300             PERFORM PRINT-DETAIL.                                NC1754.2
174400     ADD     1 TO REC-CT.                                         NC1754.2
174500 SUB-TEST-F2-35-4.                                                NC1754.2
174600     MOVE   "SUB-TEST-F2-35-4" TO PAR-NAME.                       NC1754.2
174700     IF      SIZE-ERR4 = "C"                                      NC1754.2
174800             PERFORM PASS                                         NC1754.2
174900             PERFORM PRINT-DETAIL                                 NC1754.2
175000     ELSE                                                         NC1754.2
175100             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1754.2
175200                  TO RE-MARK                                      NC1754.2
175300             MOVE   "C"        TO CORRECT-X                       NC1754.2
175400             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1754.2
175500             PERFORM FAIL                                         NC1754.2
175600             PERFORM PRINT-DETAIL.                                NC1754.2
175700     ADD     1 TO REC-CT.                                         NC1754.2
175800 SUB-TEST-F2-35-5.                                                NC1754.2
175900     MOVE   "SUB-TEST-F1-35-5" TO PAR-NAME.                       NC1754.2
176000     IF      WRK-DS-06V06 = -666999.666332                        NC1754.2
176100             PERFORM PASS                                         NC1754.2
176200             PERFORM PRINT-DETAIL                                 NC1754.2
176300     ELSE                                                         NC1754.2
176400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1754.2
176500                  TO RE-MARK                                      NC1754.2
176600             MOVE   -666999.666332 TO CORRECT-N                   NC1754.2
176700             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1754.2
176800             PERFORM FAIL                                         NC1754.2
176900             PERFORM PRINT-DETAIL.                                NC1754.2
177000*                                                                 NC1754.2
177100 SUB-INIT-F2-36.                                                  NC1754.2
177200*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1754.2
177300     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1754.2
177400     MOVE   "SUB-TEST-F2-36"  TO PAR-NAME.                        NC1754.2
177500     MOVE   SPACE TO WRK-XN-00001.                                NC1754.2
177600     MOVE   SPACE TO SIZE-ERR2.                                   NC1754.2
177700     MOVE   SPACE TO SIZE-ERR3.                                   NC1754.2
177800     MOVE   SPACE TO SIZE-ERR4.                                   NC1754.2
177900     MOVE   ZERO  TO WRK-DS-10V00.                                NC1754.2
178000     MOVE       1 TO REC-CT.                                      NC1754.2
178100 SUB-TEST-F2-36-0.                                                NC1754.2
178200     SUBTRACT A12ONES-DS-12V00                                    NC1754.2
178300        FROM ZERO GIVING WRK-DS-10V00                             NC1754.2
178400         NOT ON SIZE ERROR                                        NC1754.2
178500             MOVE   "1" TO WRK-XN-00001                           NC1754.2
178600             MOVE   "A" TO SIZE-ERR2                              NC1754.2
178700             MOVE   "B" TO SIZE-ERR3                              NC1754.2
178800     END-SUBTRACT                                                 NC1754.2
178900     MOVE   "C" TO SIZE-ERR4.                                     NC1754.2
179000     GO TO   SUB-TEST-F2-36-1.                                    NC1754.2
179100 SUB-DELETE-F2-36.                                                NC1754.2
179200     PERFORM DE-LETE.                                             NC1754.2
179300     PERFORM PRINT-DETAIL.                                        NC1754.2
179400     GO      TO SUB-INIT-F2-37.                                   NC1754.2
179500 SUB-TEST-F2-36-1.                                                NC1754.2
179600     MOVE   "SUB-TEST-F2-36-1" TO PAR-NAME.                       NC1754.2
179700     IF      WRK-XN-00001 = SPACE                                 NC1754.2
179800             PERFORM PASS                                         NC1754.2
179900             PERFORM PRINT-DETAIL                                 NC1754.2
180000     ELSE                                                         NC1754.2
180100             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1754.2
180200                  TO RE-MARK                                      NC1754.2
180300             MOVE    SPACE        TO CORRECT-X                    NC1754.2
180400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1754.2
180500             PERFORM FAIL                                         NC1754.2
180600             PERFORM PRINT-DETAIL.                                NC1754.2
180700     ADD     1 TO REC-CT.                                         NC1754.2
180800 SUB-TEST-F2-36-2.                                                NC1754.2
180900     MOVE   "SUB-TEST-F2-36-2" TO PAR-NAME.                       NC1754.2
181000     IF      SIZE-ERR2 = SPACE                                    NC1754.2
181100             PERFORM PASS                                         NC1754.2
181200             PERFORM PRINT-DETAIL                                 NC1754.2
181300     ELSE                                                         NC1754.2
181400             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1754.2
181500                  TO RE-MARK                                      NC1754.2
181600             MOVE    SPACE     TO CORRECT-X                       NC1754.2
181700             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
181800             PERFORM FAIL                                         NC1754.2
181900             PERFORM PRINT-DETAIL.                                NC1754.2
182000     ADD     1 TO REC-CT.                                         NC1754.2
182100 SUB-TEST-F2-36-3.                                                NC1754.2
182200     MOVE   "SUB-TEST-F2-36-3" TO PAR-NAME.                       NC1754.2
182300     IF      SIZE-ERR3 = SPACE                                    NC1754.2
182400             PERFORM PASS                                         NC1754.2
182500             PERFORM PRINT-DETAIL                                 NC1754.2
182600     ELSE                                                         NC1754.2
182700             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1754.2
182800                  TO RE-MARK                                      NC1754.2
182900             MOVE    SPACE     TO CORRECT-X                       NC1754.2
183000             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1754.2
183100             PERFORM FAIL                                         NC1754.2
183200             PERFORM PRINT-DETAIL.                                NC1754.2
183300     ADD     1 TO REC-CT.                                         NC1754.2
183400 SUB-TEST-F2-36-4.                                                NC1754.2
183500     MOVE   "SUB-TEST-F2-36-4" TO PAR-NAME.                       NC1754.2
183600     IF      SIZE-ERR4 = "C"                                      NC1754.2
183700             PERFORM PASS                                         NC1754.2
183800             PERFORM PRINT-DETAIL                                 NC1754.2
183900     ELSE                                                         NC1754.2
184000             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1754.2
184100                  TO RE-MARK                                      NC1754.2
184200             MOVE   "C"        TO CORRECT-X                       NC1754.2
184300             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1754.2
184400             PERFORM FAIL                                         NC1754.2
184500             PERFORM PRINT-DETAIL                                 NC1754.2
184600             ADD     1 TO REC-CT.                                 NC1754.2
184700 SUB-TEST-F2-36-5.                                                NC1754.2
184800     MOVE   "SUB-TEST-F2-36-5" TO PAR-NAME.                       NC1754.2
184900     IF      WRK-DS-10V00 = ZERO                                  NC1754.2
185000             PERFORM PASS                                         NC1754.2
185100             PERFORM PRINT-DETAIL                                 NC1754.2
185200     ELSE                                                         NC1754.2
185300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1754.2
185400                  TO RE-MARK                                      NC1754.2
185500             MOVE    ZERO         TO CORRECT-N                    NC1754.2
185600             MOVE    WRK-DS-10V00 TO COMPUTED-N                   NC1754.2
185700             PERFORM FAIL                                         NC1754.2
185800             PERFORM PRINT-DETAIL.                                NC1754.2
185900*                                                                 NC1754.2
186000 SUB-INIT-F2-37.                                                  NC1754.2
186100*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1754.2
186200     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1754.2
186300     MOVE   "SUB-TEST-F2-37"  TO PAR-NAME.                        NC1754.2
186400     MOVE    SPACE TO WRK-XN-00001.                               NC1754.2
186500     MOVE    SPACE TO SIZE-ERR2.                                  NC1754.2
186600     MOVE    SPACE TO SIZE-ERR3.                                  NC1754.2
186700     MOVE    SPACE TO SIZE-ERR4.                                  NC1754.2
186800     MOVE    ZERO  TO WRK-DS-06V06.                               NC1754.2
186900     MOVE    1     TO REC-CT.                                     NC1754.2
187000 SUB-TEST-F2-37-0.                                                NC1754.2
187100     SUBTRACT A12THREES-DS-06V06                                  NC1754.2
187200              333333                                              NC1754.2
187300              A06THREES-DS-03V03                                  NC1754.2
187400              -.0000009 FROM 0000000                              NC1754.2
187500      GIVING WRK-DS-06V06 ROUNDED                                 NC1754.2
187600         NOT ON SIZE ERROR                                        NC1754.2
187700             MOVE   "1" TO WRK-XN-00001                           NC1754.2
187800             MOVE   "A" TO SIZE-ERR2                              NC1754.2
187900             MOVE   "B" TO SIZE-ERR3                              NC1754.2
188000     END-SUBTRACT                                                 NC1754.2
188100     MOVE   "C" TO SIZE-ERR4.                                     NC1754.2
188200     GO TO   SUB-TEST-F2-37-1.                                    NC1754.2
188300 SUB-DELETE-F2-37.                                                NC1754.2
188400     PERFORM DE-LETE.                                             NC1754.2
188500     PERFORM PRINT-DETAIL.                                        NC1754.2
188600     GO TO   SUB-INIT-F2-38.                                      NC1754.2
188700 SUB-TEST-F2-37-1.                                                NC1754.2
188800     MOVE   "SUB-TEST-F2-37-1" TO PAR-NAME.                       NC1754.2
188900     IF      WRK-XN-00001 = "1"                                   NC1754.2
189000             PERFORM PASS                                         NC1754.2
189100             PERFORM PRINT-DETAIL                                 NC1754.2
189200     ELSE                                                         NC1754.2
189300             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1754.2
189400                  TO RE-MARK                                      NC1754.2
189500             MOVE   "1"           TO CORRECT-X                    NC1754.2
189600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1754.2
189700             PERFORM FAIL                                         NC1754.2
189800             PERFORM PRINT-DETAIL.                                NC1754.2
189900     ADD     1 TO REC-CT.                                         NC1754.2
190000 SUB-TEST-F2-37-2.                                                NC1754.2
190100     MOVE   "SUB-TEST-F2-37-2" TO PAR-NAME.                       NC1754.2
190200     IF      SIZE-ERR2 = "A"                                      NC1754.2
190300             PERFORM PASS                                         NC1754.2
190400             PERFORM PRINT-DETAIL                                 NC1754.2
190500     ELSE                                                         NC1754.2
190600             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1754.2
190700                  TO RE-MARK                                      NC1754.2
190800             MOVE   "A"        TO CORRECT-X                       NC1754.2
190900             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1754.2
191000             PERFORM FAIL                                         NC1754.2
191100             PERFORM PRINT-DETAIL.                                NC1754.2
191200     ADD     1 TO REC-CT.                                         NC1754.2
191300 SUB-TEST-F2-37-3.                                                NC1754.2
191400     MOVE   "SUB-TEST-F2-37-3" TO PAR-NAME.                       NC1754.2
191500     IF      SIZE-ERR3 = "B"                                      NC1754.2
191600             PERFORM PASS                                         NC1754.2
191700             PERFORM PRINT-DETAIL                                 NC1754.2
191800     ELSE                                                         NC1754.2
191900             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1754.2
192000                  TO RE-MARK                                      NC1754.2
192100             MOVE   "B"        TO CORRECT-X                       NC1754.2
192200             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1754.2
192300             PERFORM FAIL                                         NC1754.2
192400             PERFORM PRINT-DETAIL.                                NC1754.2
192500     ADD     1 TO REC-CT.                                         NC1754.2
192600 SUB-TEST-F2-37-4.                                                NC1754.2
192700     MOVE   "SUB-TEST-F2-37-4" TO PAR-NAME.                       NC1754.2
192800     IF      SIZE-ERR4 = "C"                                      NC1754.2
192900             PERFORM PASS                                         NC1754.2
193000             PERFORM PRINT-DETAIL                                 NC1754.2
193100     ELSE                                                         NC1754.2
193200             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1754.2
193300                  TO RE-MARK                                      NC1754.2
193400             MOVE   "C"        TO CORRECT-X                       NC1754.2
193500             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1754.2
193600             PERFORM FAIL                                         NC1754.2
193700             PERFORM PRINT-DETAIL.                                NC1754.2
193800     ADD     1 TO REC-CT.                                         NC1754.2
193900 SUB-TEST-F2-37-5.                                                NC1754.2
194000     MOVE   "SUB-TEST-F2-37-5" TO PAR-NAME.                       NC1754.2
194100     IF      WRK-DS-06V06 = -666999.666332                        NC1754.2
194200             PERFORM PASS                                         NC1754.2
194300             PERFORM PRINT-DETAIL                                 NC1754.2
194400     ELSE                                                         NC1754.2
194500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1754.2
194600                  TO RE-MARK                                      NC1754.2
194700             MOVE   -666999.666332 TO CORRECT-N                   NC1754.2
194800             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1754.2
194900             PERFORM FAIL                                         NC1754.2
195000             PERFORM PRINT-DETAIL.                                NC1754.2
195100*                                                                 NC1754.2
195200 SUB-INIT-F2-38.                                                  NC1754.2
195300*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1754.2
195400     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1754.2
195500     MOVE   "SUB-TEST-F2-38"  TO PAR-NAME.                        NC1754.2
195600     MOVE   "0"   TO WRK-XN-00001.                                NC1754.2
195700     MOVE   "0"   TO SIZE-ERR4.                                   NC1754.2
195800     MOVE    ZERO TO WRK-DS-10V00.                                NC1754.2
195900     MOVE    1    TO REC-CT.                                      NC1754.2
196000 SUB-TEST-F2-38-0.                                                NC1754.2
196100     SUBTRACT A12ONES-DS-12V00                                    NC1754.2
196200        FROM ZERO GIVING WRK-DS-10V00                             NC1754.2
196300          ON SIZE ERROR                                           NC1754.2
196400             MOVE   SPACE TO WRK-XN-00001                         NC1754.2
196500      NOT ON SIZE ERROR                                           NC1754.2
196600             MOVE   "1" TO WRK-XN-00001                           NC1754.2
196700     END-SUBTRACT                                                 NC1754.2
196800     MOVE   "C" TO SIZE-ERR4.                                     NC1754.2
196900     GO TO   SUB-TEST-F2-38-1.                                    NC1754.2
197000 SUB-DELETE-F2-38.                                                NC1754.2
197100     PERFORM DE-LETE.                                             NC1754.2
197200     PERFORM PRINT-DETAIL.                                        NC1754.2
197300     GO TO   SUB-INIT-F2-39.                                      NC1754.2
197400 SUB-TEST-F2-38-1.                                                NC1754.2
197500     MOVE   "SUB-TEST-F2-38-1" TO PAR-NAME.                       NC1754.2
197600     IF      WRK-XN-00001 = SPACE                                 NC1754.2
197700             PERFORM PASS                                         NC1754.2
197800             PERFORM PRINT-DETAIL                                 NC1754.2
197900     ELSE                                                         NC1754.2
198000             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1754.2
198100                  TO RE-MARK                                      NC1754.2
198200             MOVE    SPACE        TO CORRECT-X                    NC1754.2
198300             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1754.2
198400             PERFORM FAIL                                         NC1754.2
198500             PERFORM PRINT-DETAIL.                                NC1754.2
198600     ADD     1 TO REC-CT.                                         NC1754.2
198700 SUB-TEST-F2-38-2.                                                NC1754.2
198800     MOVE   "SUB-TEST-F2-38-2" TO PAR-NAME.                       NC1754.2
198900     IF      SIZE-ERR4 = "C"                                      NC1754.2
199000             PERFORM PASS                                         NC1754.2
199100             PERFORM PRINT-DETAIL                                 NC1754.2
199200     ELSE                                                         NC1754.2
199300             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1754.2
199400                  TO RE-MARK                                      NC1754.2
199500             MOVE   "C"        TO CORRECT-X                       NC1754.2
199600             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1754.2
199700             PERFORM FAIL                                         NC1754.2
199800             PERFORM PRINT-DETAIL.                                NC1754.2
199900     ADD     1 TO REC-CT.                                         NC1754.2
200000 SUB-TEST-F2-38-3.                                                NC1754.2
200100     MOVE   "SUB-TEST-F2-38-3" TO PAR-NAME.                       NC1754.2
200200     IF      WRK-DS-10V00 = ZERO                                  NC1754.2
200300             PERFORM PASS                                         NC1754.2
200400             PERFORM PRINT-DETAIL                                 NC1754.2
200500     ELSE                                                         NC1754.2
200600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1754.2
200700                  TO RE-MARK                                      NC1754.2
200800             MOVE    ZERO         TO CORRECT-N                    NC1754.2
200900             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1754.2
201000             PERFORM FAIL                                         NC1754.2
201100             PERFORM PRINT-DETAIL.                                NC1754.2
201200*                                                                 NC1754.2
201300 SUB-INIT-F2-39.                                                  NC1754.2
201400*    ==--> EXPLICIT SCOPE TERMINATOR<--==                         NC1754.2
201500     MOVE   "IV-41 6.4.3"     TO ANSI-REFERENCE.                  NC1754.2
201600     MOVE   "SUB-TEST-F2-39"  TO PAR-NAME.                        NC1754.2
201700     MOVE    SPACE TO WRK-XN-00001.                               NC1754.2
201800     MOVE    SPACE TO SIZE-ERR4.                                  NC1754.2
201900     MOVE    ZERO  TO WRK-DS-06V06.                               NC1754.2
202000     MOVE    1     TO REC-CT.                                     NC1754.2
202100 SUB-TEST-F2-39-0.                                                NC1754.2
202200     SUBTRACT A12THREES-DS-06V06                                  NC1754.2
202300              333333                                              NC1754.2
202400              A06THREES-DS-03V03                                  NC1754.2
202500              -.0000009 FROM 0000000                              NC1754.2
202600      GIVING WRK-DS-06V06 ROUNDED                                 NC1754.2
202700             ON SIZE ERROR                                        NC1754.2
202800             MOVE   "X" TO WRK-XN-00001                           NC1754.2
202900         NOT ON SIZE ERROR                                        NC1754.2
203000             MOVE   "1" TO WRK-XN-00001                           NC1754.2
203100     END-SUBTRACT                                                 NC1754.2
203200     MOVE   "C" TO SIZE-ERR4.                                     NC1754.2
203300     GO TO   SUB-TEST-F2-39-1.                                    NC1754.2
203400 SUB-DELETE-F2-39.                                                NC1754.2
203500     PERFORM DE-LETE.                                             NC1754.2
203600     PERFORM PRINT-DETAIL.                                        NC1754.2
203700     GO TO   CCVS-EXIT.                                           NC1754.2
203800 SUB-TEST-F2-39-1.                                                NC1754.2
203900     MOVE   "SUB-TEST-F2-39-1" TO PAR-NAME.                       NC1754.2
204000     IF      WRK-XN-00001 = "1"                                   NC1754.2
204100             PERFORM PASS                                         NC1754.2
204200             PERFORM PRINT-DETAIL                                 NC1754.2
204300     ELSE                                                         NC1754.2
204400             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1754.2
204500                  TO RE-MARK                                      NC1754.2
204600             MOVE   "1"           TO CORRECT-X                    NC1754.2
204700             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1754.2
204800             PERFORM FAIL                                         NC1754.2
204900             PERFORM PRINT-DETAIL.                                NC1754.2
205000     ADD     1 TO REC-CT.                                         NC1754.2
205100 SUB-TEST-F2-39-2.                                                NC1754.2
205200     MOVE   "SUB-TEST-F2-39-2" TO PAR-NAME.                       NC1754.2
205300     IF      SIZE-ERR4 = "C"                                      NC1754.2
205400             PERFORM PASS                                         NC1754.2
205500             PERFORM PRINT-DETAIL                                 NC1754.2
205600     ELSE                                                         NC1754.2
205700             MOVE   "SCOPE TERMINATOR IGNORED"                    NC1754.2
205800                  TO RE-MARK                                      NC1754.2
205900             MOVE   "C"        TO CORRECT-X                       NC1754.2
206000             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1754.2
206100             PERFORM FAIL                                         NC1754.2
206200             PERFORM PRINT-DETAIL.                                NC1754.2
206300     ADD     1 TO REC-CT.                                         NC1754.2
206400 SUB-TEST-F2-39-3.                                                NC1754.2
206500     MOVE   "SUB-TEST-F2-39-3" TO PAR-NAME.                       NC1754.2
206600     IF      WRK-DS-06V06 = -666999.666332                        NC1754.2
206700             PERFORM PASS                                         NC1754.2
206800             PERFORM PRINT-DETAIL                                 NC1754.2
206900     ELSE                                                         NC1754.2
207000             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1754.2
207100                  TO RE-MARK                                      NC1754.2
207200             MOVE   -666999.666332 TO CORRECT-N                   NC1754.2
207300             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1754.2
207400             PERFORM FAIL                                         NC1754.2
207500             PERFORM PRINT-DETAIL.                                NC1754.2
207600*                                                                 NC1754.2
207700 CCVS-EXIT SECTION.                                               NC1754.2
207800 CCVS-999999.                                                     NC1754.2
207900     GO TO CLOSE-FILES.                                           NC1754.2
      *END-OF,NC175A                                                                  
