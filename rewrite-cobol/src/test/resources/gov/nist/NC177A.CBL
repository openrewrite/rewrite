      *HEADER,COBOL,NC177A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1774.2
000200 PROGRAM-ID.                                                      NC1774.2
000300     NC177A.                                                      NC1774.2
000400****************************************************************  NC1774.2
000500*                                                              *  NC1774.2
000600*    VALIDATION FOR:-                                          *  NC1774.2
000700*                                                              *  NC1774.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1774.2
000900*                                                              *  NC1774.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1774.2
001100*                                                              *  NC1774.2
001200****************************************************************  NC1774.2
001300*                                                              *  NC1774.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1774.2
001500*                                                              *  NC1774.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1774.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1774.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1774.2
001900*                                                              *  NC1774.2
002000****************************************************************  NC1774.2
002100*                                                                 NC1774.2
002200*    PROGRAM NC177A TESTS FORMAT 2 OF THE ADD STATEMENT.          NC1774.2
002300*    VARIOUS COMBINATINS OF DATA-ITEMS AND ALL                    NC1774.2
002400*    OPTIONAL PHRASES ARE TESTED.                                 NC1774.2
002500*                                                                 NC1774.2
002600 ENVIRONMENT DIVISION.                                            NC1774.2
002700 CONFIGURATION SECTION.                                           NC1774.2
002800 SOURCE-COMPUTER.                                                 NC1774.2
002900     XXXXX082.                                                    NC1774.2
003000 OBJECT-COMPUTER.                                                 NC1774.2
003100     XXXXX083.                                                    NC1774.2
003200 INPUT-OUTPUT SECTION.                                            NC1774.2
003300 FILE-CONTROL.                                                    NC1774.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1774.2
003500     XXXXX055.                                                    NC1774.2
003600 DATA DIVISION.                                                   NC1774.2
003700 FILE SECTION.                                                    NC1774.2
003800 FD  PRINT-FILE.                                                  NC1774.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1774.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1774.2
004100 WORKING-STORAGE SECTION.                                         NC1774.2
004200 77  SIZE-ERR                     PICTURE X  VALUE SPACE.         NC1774.2
004300 77  SIZE-ERR2                    PICTURE X  VALUE SPACE.         NC1774.2
004400 77  SIZE-ERR3                    PICTURE X  VALUE SPACE.         NC1774.2
004500 77  SIZE-ERR4                    PICTURE X  VALUE SPACE.         NC1774.2
004600 77  A17TWOS-DS-17V00             PICTURE S9(17)                  NC1774.2
004700                                  VALUE 22222222222222222.        NC1774.2
004800 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC1774.2
004900                                  VALUE 111111111111111111.       NC1774.2
005000 77  WRK-DS-10V00                 PICTURE S9(10).                 NC1774.2
005100 77  A10ONES-DS-10V00             PICTURE S9(10)                  NC1774.2
005200                                  VALUE 1111111111.               NC1774.2
005300 77  A05ONES-DS-05V00             PICTURE S9(5)                   NC1774.2
005400                                  VALUE 11111.                    NC1774.2
005500 77  A02ONES-DS-02V00             PICTURE S99                     NC1774.2
005600                                  VALUE 11.                       NC1774.2
005700 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1774.2
005800 77  WRK-DS-18V00 REDEFINES WRK-DS-09V09                          NC1774.2
005900                                  PICTURE S9(18).                 NC1774.2
006000 77  A06THREES-DS-03V03           PICTURE S999V999                NC1774.2
006100                                  VALUE 333.333.                  NC1774.2
006200 77  A12THREES-DS-06V06           PICTURE S9(6)V9(6)              NC1774.2
006300                                  VALUE 333333.333333.            NC1774.2
006400 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC1774.2
006500 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC1774.2
006600                                  PICTURE S9(12).                 NC1774.2
006700 77  A05ONES-DS-00V05             PICTURE SV9(5)                  NC1774.2
006800                                  VALUE .11111.                   NC1774.2
006900 77  WRK-DS-05V00                 PICTURE S9(5).                  NC1774.2
007000 77  WRK-DS-02V00                 PICTURE S99.                    NC1774.2
007100 77  A12ONES-DS-12V00             PICTURE S9(12)                  NC1774.2
007200                                  VALUE 111111111111.             NC1774.2
007300 77  WRK-DS-03V10                 PICTURE S999V9(10).             NC1774.2
007400 77  WRK-DS-13V00-S REDEFINES WRK-DS-03V10                        NC1774.2
007500                                  PICTURE S9(13).                 NC1774.2
007600 77  A99-DS-02V00                 PICTURE S99                     NC1774.2
007700                                  VALUE 99.                       NC1774.2
007800 77  A03ONES-DS-02V01             PICTURE S99V9                   NC1774.2
007900                                  VALUE 11.1.                     NC1774.2
008000 77  A06ONES-DS-03V03             PICTURE S999V999                NC1774.2
008100                                  VALUE 111.111.                  NC1774.2
008200 77  A08TWOS-DS-02V06             PICTURE S99V9(6)                NC1774.2
008300                                  VALUE 22.222222.                NC1774.2
008400 77  A01ONE-DS-P0801              PICTURE SP(8)9                  NC1774.2
008500                                  VALUE .000000001.               NC1774.2
008600 77  WRK-CS-18V00                 PICTURE S9(18) COMPUTATIONAL.   NC1774.2
008700 77  A18ONES-CS-18V00             PICTURE S9(18) COMPUTATIONAL    NC1774.2
008800                                  VALUE 111111111111111111.       NC1774.2
008900 77  WRK-CS-02V02                 PICTURE S99V99 COMPUTATIONAL.   NC1774.2
009000 77  A99-CS-02V00                 PICTURE S99 COMPUTATIONAL       NC1774.2
009100                                  VALUE 99.                       NC1774.2
009200 77  WRK-DS-0201P                 PICTURE S99P.                   NC1774.2
009300 77  WRK-DS-06V00                 PICTURE S9(6).                  NC1774.2
009400 77  AZERO-DS-05V05               PICTURE S9(5)V9(5)              NC1774.2
009500                                  VALUE ZERO.                     NC1774.2
009600 77  NDATA-DS-09V09               PICTURE S9(9)V9(9)              NC1774.2
009700                                  VALUE +012345678.876543210.     NC1774.2
009800 77  XDATA-XN-00018               PICTURE X(18)                   NC1774.2
009900                                  VALUE "00ABCDEFGHI  4321 ".     NC1774.2
010000 77  WRK-XN-00018                 PICTURE X(18).                  NC1774.2
010100 77  WRK-XN-00001                 PICTURE X.                      NC1774.2
010200 77  ADD-12    PICTURE PP9        VALUE .001.                     NC1774.2
010300 77  ADD-13    PICTURE 9PP        VALUE 100.                      NC1774.2
010400 77  ADD-14    PICTURE 999V999.                                   NC1774.2
010500 77  WRK-CU-18V00      PICTURE 9(18) VALUE ZERO COMPUTATIONAL.    NC1774.2
010600 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1774.2
010700 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1774.2
010800 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1774.2
010900 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1774.2
011000 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1774.2
011100 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1774.2
011200 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1774.2
011300 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1774.2
011400 01  WRK-DU-2V0-1 PIC 99.                                         NC1774.2
011500 01  WRK-DU-2V0-2 PIC 99.                                         NC1774.2
011600 01  WRK-DU-2V0-3 PIC 99.                                         NC1774.2
011700 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1774.2
011800 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1774.2
011900 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1774.2
012000 77  A18EIGHTS-CS-18V00 PICTURE S9(18) VALUE -888888888888888888  NC1774.2
012100             COMPUTATIONAL.                                       NC1774.2
012200 77  A14TWOS-CS-18V00  PICTURE S9(18) VALUE -000022222222222222   NC1774.2
012300             COMPUTATIONAL.                                       NC1774.2
012400 77  A12THREES-CU-18V00 PICTURE 9(18) VALUE 000000333333333333    NC1774.2
012500             COMPUTATIONAL.                                       NC1774.2
012600 77  A16FOURS-CS-18V00 PICTURE S9(18) VALUE 004444444444444444    NC1774.2
012700             COMPUTATIONAL.                                       NC1774.2
012800 77  A18FIVES-CS-18V00 PICTURE S9(18) VALUE -555555555555555555   NC1774.2
012900             COMPUTATIONAL.                                       NC1774.2
013000 77  A18SIXES-CS-18V00 PICTURE S9(18) VALUE 666666666666666666    NC1774.2
013100             COMPUTATIONAL.                                       NC1774.2
013200 77  A12SEVENS-CU-18V00 PICTURE 9(18) VALUE 000000777777777777    NC1774.2
013300             COMPUTATIONAL.                                       NC1774.2
013400 77  WRK-DU-18V00      PICTURE 9(18) VALUE ZERO.                  NC1774.2
013500 77  A18THREES-CS-18V00 PICTURE S9(18) VALUE -333333333333333333  NC1774.2
013600             COMPUTATIONAL.                                       NC1774.2
013700 77  WRK-CS-03V00 PICTURE S999 COMPUTATIONAL SYNCHRONIZED RIGHT.  NC1774.2
013800 01  SUBTRACT-DATA.                                               NC1774.2
013900     02  SUBTR-1    PICTURE 9         VALUE 1.                    NC1774.2
014000     02  SUBTR-2    PICTURE S99       VALUE 99.                   NC1774.2
014100     02  SUBTR-3    PICTURE S9V99     VALUE -1.                   NC1774.2
014200     02  SUBTR-4    PICTURE SPP9      VALUE .001.                 NC1774.2
014300     02  SUBTR-5    PICTURE S9PP      VALUE 100.                  NC1774.2
014400     02  SUBTR-6    PICTURE 9  VALUE 1.                           NC1774.2
014500     02  SUBTR-7    PICTURE S99  VALUE 99.                        NC1774.2
014600     02  SUBTR-8    PICTURE S9V99  VALUE -9.99.                   NC1774.2
014700     02  SUBTR-10    PICTURE S999  VALUE 100.                     NC1774.2
014800     02  SUBTR-11   PICTURE S999V999.                             NC1774.2
014900 01  N-3                                PICTURE IS 99999.         NC1774.2
015000 01  N-4                                PICTURE IS 9(5)           NC1774.2
015100     VALUE IS 52800.                                              NC1774.2
015200 01  N-5                                PICTURE IS S9(9)V99       NC1774.2
015300     VALUE IS 000000001.00.                                       NC1774.2
015400 01  N-7                                PICTURE IS S9(7)V9(4)     NC1774.2
015500     VALUE IS 0000001.0000.                                       NC1774.2
015600 01  X-8 REDEFINES N-7                  PICTURE IS X(11).         NC1774.2
015700 01  N-10                               PICTURE IS S99999V        NC1774.2
015800     VALUE IS -00001.                                             NC1774.2
015900 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1774.2
016000 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1774.2
016100 01  N-13                               PICTURE IS 9(5)           NC1774.2
016200     VALUE IS 99999.                                              NC1774.2
016300 01  N-14                               PICTURE IS 9 VALUE IS 1.  NC1774.2
016400 01  N-15                               PICTURE IS 9(16).         NC1774.2
016500 01  N-16                               PICTURE IS S999999V99     NC1774.2
016600     VALUE IS 5.90.                                               NC1774.2
016700 01  N-17                               PICTURE IS S9(3)V99       NC1774.2
016800     VALUE IS +3.6.                                               NC1774.2
016900 01  N-18                               PICTURE IS S9(10)         NC1774.2
017000     VALUE IS -5.                                                 NC1774.2
017100 01  N-19                               PICTURE IS $9.00.         NC1774.2
017200 01  N-20                               PICTURE IS S9(9)          NC1774.2
017300     VALUE IS -999999999.                                         NC1774.2
017400 01  N-21                               PICTURE IS 9 VALUE IS 5.  NC1774.2
017500 01  N-22                               PICTURE IS 999V99         NC1774.2
017600     VALUE IS 005.55.                                             NC1774.2
017700 01  N-23                               PICTURE IS $$$.99CR.      NC1774.2
017800 01  N-25                               PICTURE IS 9 VALUE IS 1.  NC1774.2
017900 01  N-26               PICTURE 9(5).                             NC1774.2
018000 01  N-27                               PICTURE IS 9999V9         NC1774.2
018100     VALUE IS 9999.9.                                             NC1774.2
018200 01  N-28                               PICTURE IS $9999.00.      NC1774.2
018300 01  N-40                               PICTURE IS 9(7)           NC1774.2
018400     VALUE IS 7777777.                                            NC1774.2
018500 01  N-41                               PICTURE IS 9(7)           NC1774.2
018600     VALUE IS 1111111.                                            NC1774.2
018700 01  N-42                               PICTURE IS 9(3)P(4).      NC1774.2
018800 01  TRUNC-DATA.                                                  NC1774.2
018900     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1774.2
019000     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1774.2
019100     02 N-45  PICTURE S9.                                         NC1774.2
019200 01  MINUS-NAMES.                                                 NC1774.2
019300     02  MINUS-NAME1 PICTURE S9(18) VALUE -999999999999999999.    NC1774.2
019400     02  MINUS-NAME2 PICTURE S9(18) VALUE -999999999999999999.    NC1774.2
019500     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC1774.2
019600     02  PLUS-NAME1  PICTURE S9(18) VALUE +999999999999999999.    NC1774.2
019700     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC1774.2
019800     02  MINUS-NAME3 PICTURE SV9(18) VALUE -.999999999999999999.  NC1774.2
019900     02  MINUS-NAME4 PICTURE SV9(18) VALUE -.999999999999999999.  NC1774.2
020000     02  EVEN-NAME2  PICTURE SV9(18)  VALUE +.1.                  NC1774.2
020100     02  PLUS-NAME3  PICTURE SV9(18) VALUE +.999999999999999999.  NC1774.2
020200     02  PLUS-NAME4  PICTURE SV9(18) VALUE +.999999999999999999.  NC1774.2
020300     02  WHOLE-FIELD   PICTURE S9(18).                            NC1774.2
020400     02  DECMAL-FIELD  PICTURE SV9(18).                           NC1774.2
020500 01  TEST-RESULTS.                                                NC1774.2
020600     02 FILLER                   PIC X      VALUE SPACE.          NC1774.2
020700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1774.2
020800     02 FILLER                   PIC X      VALUE SPACE.          NC1774.2
020900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1774.2
021000     02 FILLER                   PIC X      VALUE SPACE.          NC1774.2
021100     02  PAR-NAME.                                                NC1774.2
021200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1774.2
021300       03  PARDOT-X              PIC X      VALUE SPACE.          NC1774.2
021400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1774.2
021500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1774.2
021600     02 RE-MARK                  PIC X(61).                       NC1774.2
021700 01  TEST-COMPUTED.                                               NC1774.2
021800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1774.2
021900     02 FILLER                   PIC X(17)  VALUE                 NC1774.2
022000            "       COMPUTED=".                                   NC1774.2
022100     02 COMPUTED-X.                                               NC1774.2
022200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1774.2
022300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1774.2
022400                                 PIC -9(9).9(9).                  NC1774.2
022500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1774.2
022600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1774.2
022700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1774.2
022800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1774.2
022900         04 COMPUTED-18V0                    PIC -9(18).          NC1774.2
023000         04 FILLER                           PIC X.               NC1774.2
023100     03 FILLER PIC X(50) VALUE SPACE.                             NC1774.2
023200 01  TEST-CORRECT.                                                NC1774.2
023300     02 FILLER PIC X(30) VALUE SPACE.                             NC1774.2
023400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1774.2
023500     02 CORRECT-X.                                                NC1774.2
023600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1774.2
023700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1774.2
023800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1774.2
023900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1774.2
024000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1774.2
024100     03      CR-18V0 REDEFINES CORRECT-A.                         NC1774.2
024200         04 CORRECT-18V0                     PIC -9(18).          NC1774.2
024300         04 FILLER                           PIC X.               NC1774.2
024400     03 FILLER PIC X(2) VALUE SPACE.                              NC1774.2
024500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1774.2
024600 01  CCVS-C-1.                                                    NC1774.2
024700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1774.2
024800-    "SS  PARAGRAPH-NAME                                          NC1774.2
024900-    "       REMARKS".                                            NC1774.2
025000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1774.2
025100 01  CCVS-C-2.                                                    NC1774.2
025200     02 FILLER                     PIC X        VALUE SPACE.      NC1774.2
025300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1774.2
025400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1774.2
025500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1774.2
025600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1774.2
025700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1774.2
025800 01  REC-CT                        PIC 99       VALUE ZERO.       NC1774.2
025900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1774.2
026000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1774.2
026100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1774.2
026200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1774.2
026300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1774.2
026400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1774.2
026500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1774.2
026600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1774.2
026700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1774.2
026800 01  CCVS-H-1.                                                    NC1774.2
026900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1774.2
027000     02  FILLER                    PIC X(42)    VALUE             NC1774.2
027100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1774.2
027200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1774.2
027300 01  CCVS-H-2A.                                                   NC1774.2
027400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1774.2
027500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1774.2
027600   02  FILLER                        PIC XXXX   VALUE             NC1774.2
027700     "4.2 ".                                                      NC1774.2
027800   02  FILLER                        PIC X(28)  VALUE             NC1774.2
027900            " COPY - NOT FOR DISTRIBUTION".                       NC1774.2
028000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1774.2
028100                                                                  NC1774.2
028200 01  CCVS-H-2B.                                                   NC1774.2
028300   02  FILLER                        PIC X(15)  VALUE             NC1774.2
028400            "TEST RESULT OF ".                                    NC1774.2
028500   02  TEST-ID                       PIC X(9).                    NC1774.2
028600   02  FILLER                        PIC X(4)   VALUE             NC1774.2
028700            " IN ".                                               NC1774.2
028800   02  FILLER                        PIC X(12)  VALUE             NC1774.2
028900     " HIGH       ".                                              NC1774.2
029000   02  FILLER                        PIC X(22)  VALUE             NC1774.2
029100            " LEVEL VALIDATION FOR ".                             NC1774.2
029200   02  FILLER                        PIC X(58)  VALUE             NC1774.2
029300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1774.2
029400 01  CCVS-H-3.                                                    NC1774.2
029500     02  FILLER                      PIC X(34)  VALUE             NC1774.2
029600            " FOR OFFICIAL USE ONLY    ".                         NC1774.2
029700     02  FILLER                      PIC X(58)  VALUE             NC1774.2
029800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1774.2
029900     02  FILLER                      PIC X(28)  VALUE             NC1774.2
030000            "  COPYRIGHT   1985 ".                                NC1774.2
030100 01  CCVS-E-1.                                                    NC1774.2
030200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1774.2
030300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1774.2
030400     02 ID-AGAIN                     PIC X(9).                    NC1774.2
030500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1774.2
030600 01  CCVS-E-2.                                                    NC1774.2
030700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1774.2
030800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1774.2
030900     02 CCVS-E-2-2.                                               NC1774.2
031000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1774.2
031100         03 FILLER                   PIC X      VALUE SPACE.      NC1774.2
031200         03 ENDER-DESC               PIC X(44)  VALUE             NC1774.2
031300            "ERRORS ENCOUNTERED".                                 NC1774.2
031400 01  CCVS-E-3.                                                    NC1774.2
031500     02  FILLER                      PIC X(22)  VALUE             NC1774.2
031600            " FOR OFFICIAL USE ONLY".                             NC1774.2
031700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1774.2
031800     02  FILLER                      PIC X(58)  VALUE             NC1774.2
031900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1774.2
032000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1774.2
032100     02 FILLER                       PIC X(15)  VALUE             NC1774.2
032200             " COPYRIGHT 1985".                                   NC1774.2
032300 01  CCVS-E-4.                                                    NC1774.2
032400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1774.2
032500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1774.2
032600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1774.2
032700     02 FILLER                       PIC X(40)  VALUE             NC1774.2
032800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1774.2
032900 01  XXINFO.                                                      NC1774.2
033000     02 FILLER                       PIC X(19)  VALUE             NC1774.2
033100            "*** INFORMATION ***".                                NC1774.2
033200     02 INFO-TEXT.                                                NC1774.2
033300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1774.2
033400       04 XXCOMPUTED                 PIC X(20).                   NC1774.2
033500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1774.2
033600       04 XXCORRECT                  PIC X(20).                   NC1774.2
033700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1774.2
033800 01  HYPHEN-LINE.                                                 NC1774.2
033900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1774.2
034000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1774.2
034100-    "*****************************************".                 NC1774.2
034200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1774.2
034300-    "******************************".                            NC1774.2
034400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1774.2
034500     "NC177A".                                                    NC1774.2
034600 PROCEDURE DIVISION.                                              NC1774.2
034700 CCVS1 SECTION.                                                   NC1774.2
034800 OPEN-FILES.                                                      NC1774.2
034900     OPEN     OUTPUT PRINT-FILE.                                  NC1774.2
035000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1774.2
035100     MOVE    SPACE TO TEST-RESULTS.                               NC1774.2
035200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1774.2
035300     GO TO CCVS1-EXIT.                                            NC1774.2
035400 CLOSE-FILES.                                                     NC1774.2
035500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1774.2
035600 TERMINATE-CCVS.                                                  NC1774.2
035700*    EXIT PROGRAM.                                                NC1774.2
035800*TERMINATE-CALL.                                                  NC1774.2
035900     STOP     RUN.                                                NC1774.2
036000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1774.2
036100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1774.2
036200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1774.2
036300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1774.2
036400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1774.2
036500 PRINT-DETAIL.                                                    NC1774.2
036600     IF REC-CT NOT EQUAL TO ZERO                                  NC1774.2
036700             MOVE "." TO PARDOT-X                                 NC1774.2
036800             MOVE REC-CT TO DOTVALUE.                             NC1774.2
036900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1774.2
037000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1774.2
037100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1774.2
037200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1774.2
037300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1774.2
037400     MOVE SPACE TO CORRECT-X.                                     NC1774.2
037500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1774.2
037600     MOVE     SPACE TO RE-MARK.                                   NC1774.2
037700 HEAD-ROUTINE.                                                    NC1774.2
037800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1774.2
037900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1774.2
038000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1774.2
038100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1774.2
038200 COLUMN-NAMES-ROUTINE.                                            NC1774.2
038300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1774.2
038400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1774.2
038500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1774.2
038600 END-ROUTINE.                                                     NC1774.2
038700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1774.2
038800 END-RTN-EXIT.                                                    NC1774.2
038900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1774.2
039000 END-ROUTINE-1.                                                   NC1774.2
039100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1774.2
039200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1774.2
039300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1774.2
039400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1774.2
039500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1774.2
039600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1774.2
039700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1774.2
039800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1774.2
039900  END-ROUTINE-12.                                                 NC1774.2
040000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1774.2
040100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1774.2
040200         MOVE "NO " TO ERROR-TOTAL                                NC1774.2
040300         ELSE                                                     NC1774.2
040400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1774.2
040500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1774.2
040600     PERFORM WRITE-LINE.                                          NC1774.2
040700 END-ROUTINE-13.                                                  NC1774.2
040800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1774.2
040900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1774.2
041000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1774.2
041100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1774.2
041200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1774.2
041300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1774.2
041400          MOVE "NO " TO ERROR-TOTAL                               NC1774.2
041500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1774.2
041600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1774.2
041700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1774.2
041800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1774.2
041900 WRITE-LINE.                                                      NC1774.2
042000     ADD 1 TO RECORD-COUNT.                                       NC1774.2
042100*    IF RECORD-COUNT GREATER 42                                   NC1774.2
042200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1774.2
042300*        MOVE SPACE TO DUMMY-RECORD                               NC1774.2
042400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1774.2
042500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1774.2
042600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1774.2
042700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1774.2
042800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1774.2
042900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1774.2
043000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1774.2
043100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1774.2
043200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1774.2
043300*        MOVE ZERO TO RECORD-COUNT.                               NC1774.2
043400     PERFORM WRT-LN.                                              NC1774.2
043500 WRT-LN.                                                          NC1774.2
043600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1774.2
043700     MOVE SPACE TO DUMMY-RECORD.                                  NC1774.2
043800 BLANK-LINE-PRINT.                                                NC1774.2
043900     PERFORM WRT-LN.                                              NC1774.2
044000 FAIL-ROUTINE.                                                    NC1774.2
044100     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1774.2
044200            GO TO FAIL-ROUTINE-WRITE.                             NC1774.2
044300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1774.2
044400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1774.2
044500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1774.2
044600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1774.2
044700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1774.2
044800     GO TO  FAIL-ROUTINE-EX.                                      NC1774.2
044900 FAIL-ROUTINE-WRITE.                                              NC1774.2
045000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1774.2
045100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1774.2
045200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1774.2
045300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1774.2
045400 FAIL-ROUTINE-EX. EXIT.                                           NC1774.2
045500 BAIL-OUT.                                                        NC1774.2
045600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1774.2
045700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1774.2
045800 BAIL-OUT-WRITE.                                                  NC1774.2
045900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1774.2
046000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1774.2
046100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1774.2
046200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1774.2
046300 BAIL-OUT-EX. EXIT.                                               NC1774.2
046400 CCVS1-EXIT.                                                      NC1774.2
046500     EXIT.                                                        NC1774.2
046600 SECT-NC177A-001 SECTION.                                         NC1774.2
046700 ADD-INIT-F2-1.                                                   NC1774.2
046800     MOVE "ADD GIVING" TO FEATURE.                                NC1774.2
046900     MOVE "VI-74 6.6.4 GR2" TO ANSI-REFERENCE.                    NC1774.2
047000 ADD-TEST-F2-1.                                                   NC1774.2
047100     ADD      1 N-14 GIVING N-15.                                 NC1774.2
047200     IF       N-15 EQUAL TO 2                                     NC1774.2
047300              PERFORM PASS                                        NC1774.2
047400              GO TO ADD-WRITE-F2-1.                               NC1774.2
047500     GO TO    ADD-FAIL-F2-1.                                      NC1774.2
047600 ADD-DELETE-F2-1.                                                 NC1774.2
047700     PERFORM  DE-LETE.                                            NC1774.2
047800     GO TO    ADD-WRITE-F2-1.                                     NC1774.2
047900 ADD-FAIL-F2-1.                                                   NC1774.2
048000     MOVE     N-15 TO COMPUTED-N.                                 NC1774.2
048100     MOVE     2 TO CORRECT-N.                                     NC1774.2
048200     PERFORM  FAIL.                                               NC1774.2
048300 ADD-WRITE-F2-1.                                                  NC1774.2
048400     MOVE "ADD-TEST-F2-1 " TO PAR-NAME.                           NC1774.2
048500     PERFORM  PRINT-DETAIL.                                       NC1774.2
048600 ADD-TEST-F2-2.                                                   NC1774.2
048700     ADD      N-16 N-4 GIVING N-3 ROUNDED.                        NC1774.2
048800     IF       N-3 EQUAL TO 52806                                  NC1774.2
048900              PERFORM PASS                                        NC1774.2
049000              GO TO ADD-WRITE-F2-2.                               NC1774.2
049100     GO TO    ADD-FAIL-F2-2.                                      NC1774.2
049200 ADD-DELETE-F2-2.                                                 NC1774.2
049300     PERFORM  DE-LETE.                                            NC1774.2
049400     GO TO    ADD-WRITE-F2-2.                                     NC1774.2
049500 ADD-FAIL-F2-2.                                                   NC1774.2
049600     MOVE     N-3 TO COMPUTED-N.                                  NC1774.2
049700     MOVE     52806 TO CORRECT-N.                                 NC1774.2
049800     PERFORM  FAIL.                                               NC1774.2
049900 ADD-WRITE-F2-2.                                                  NC1774.2
050000     MOVE "ADD-TEST-F2-2 " TO PAR-NAME.                           NC1774.2
050100     PERFORM  PRINT-DETAIL.                                       NC1774.2
050200     MOVE     52806 TO N-3.                                       NC1774.2
050300 ADD-TEST-F2-3-1.                                                 NC1774.2
050400     ADD      N-13 1 GIVING N-3 ON SIZE ERROR                     NC1774.2
050500              PERFORM PASS                                        NC1774.2
050600              GO TO ADD-WRITE-F2-3-1.                             NC1774.2
050700*    NOTE WHEN SIZE ERROR CONDITION OCCURS, VALUE OF              NC1774.2
050800*    N-3  SHOULD NOT BE CHANGED.                                  NC1774.2
050900     GO TO    ADD-FAIL-F2-3-1.                                    NC1774.2
051000 ADD-DELETE-F2-3-1.                                               NC1774.2
051100     PERFORM  DE-LETE.                                            NC1774.2
051200     GO TO    ADD-WRITE-F2-3-1.                                   NC1774.2
051300 ADD-FAIL-F2-3-1.                                                 NC1774.2
051400     MOVE     N-3 TO COMPUTED-N.                                  NC1774.2
051500     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1774.2
051600     PERFORM  FAIL.                                               NC1774.2
051700 ADD-WRITE-F2-3-1.                                                NC1774.2
051800     MOVE "ADD-TEST-F2-3-1 " TO PAR-NAME.                         NC1774.2
051900     PERFORM  PRINT-DETAIL.                                       NC1774.2
052000 ADD-TEST-F2-3-2.                                                 NC1774.2
052100     IF      N-3 NOT = 52806                                      NC1774.2
052200             MOVE    N-3   TO COMPUTED-N                          NC1774.2
052300             MOVE    42806 TO CORRECT-N                           NC1774.2
052400             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1774.2
052500             MOVE   "ADD-TEST-F2-3-2 " TO PAR-NAME                NC1774.2
052600             PERFORM FAIL                                         NC1774.2
052700             PERFORM PRINT-DETAIL.                                NC1774.2
052800 ADD-TEST-F2-4-1.                                                 NC1774.2
052900     ADD      1.6 N-13 GIVING N-3 ROUNDED ON SIZE ERROR           NC1774.2
053000              PERFORM PASS                                        NC1774.2
053100              GO TO ADD-WRITE-F2-4-1.                             NC1774.2
053200     GO TO    ADD-FAIL-F2-4-1.                                    NC1774.2
053300 ADD-DELETE-F2-4-1.                                               NC1774.2
053400     PERFORM  DE-LETE.                                            NC1774.2
053500     GO TO    ADD-WRITE-F2-4-1.                                   NC1774.2
053600 ADD-FAIL-F2-4-1.                                                 NC1774.2
053700     MOVE     N-3 TO COMPUTED-N.                                  NC1774.2
053800     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1774.2
053900     PERFORM  FAIL.                                               NC1774.2
054000 ADD-WRITE-F2-4-1.                                                NC1774.2
054100     MOVE "ADD-TEST-F2-4-1 " TO PAR-NAME.                         NC1774.2
054200     PERFORM  PRINT-DETAIL.                                       NC1774.2
054300 ADD-TEST-F2-4-2.                                                 NC1774.2
054400     IF       N-3 EQUAL TO 52806                                  NC1774.2
054500              PERFORM PASS                                        NC1774.2
054600              GO TO ADD-WRITE-F2-4-2.                             NC1774.2
054700     GO TO    ADD-FAIL-F2-4-2.                                    NC1774.2
054800 ADD-DELETE-F2-4-2.                                               NC1774.2
054900     PERFORM  DE-LETE.                                            NC1774.2
055000     GO TO    ADD-WRITE-F2-4-2.                                   NC1774.2
055100 ADD-FAIL-F2-4-2.                                                 NC1774.2
055200     MOVE     N-3 TO COMPUTED-N.                                  NC1774.2
055300     MOVE     52806 TO CORRECT-N.                                 NC1774.2
055400     PERFORM  FAIL.                                               NC1774.2
055500 ADD-WRITE-F2-4-2.                                                NC1774.2
055600     MOVE "ADD-TEST-F2-4-2" TO PAR-NAME.                          NC1774.2
055700     PERFORM  PRINT-DETAIL.                                       NC1774.2
055800 ADD-INIT-F2-5.                                                   NC1774.2
055900     MOVE "  GIVING" TO FEATURE.                                  NC1774.2
056000 ADD-TEST-F2-5.                                                   NC1774.2
056100     MOVE     ZERO TO WRK-DS-09V09.                               NC1774.2
056200     ADD      A06THREES-DS-03V03                                  NC1774.2
056300              A12THREES-DS-06V06 GIVING WRK-DS-09V09.             NC1774.2
056400     IF       WRK-DS-09V09 EQUAL TO 000333666.666333000           NC1774.2
056500              PERFORM PASS GO TO ADD-WRITE-F2-5.                  NC1774.2
056600     GO       TO ADD-FAIL-F2-5.                                   NC1774.2
056700 ADD-DELETE-F2-5.                                                 NC1774.2
056800     PERFORM  DE-LETE.                                            NC1774.2
056900     GO       TO ADD-WRITE-F2-5.                                  NC1774.2
057000 ADD-FAIL-F2-5.                                                   NC1774.2
057100     MOVE     WRK-DS-09V09 TO COMPUTED-N.                         NC1774.2
057200     MOVE     000333666.666333000 TO CORRECT-N.                   NC1774.2
057300     PERFORM  FAIL.                                               NC1774.2
057400 ADD-WRITE-F2-5.                                                  NC1774.2
057500     MOVE     "ADD-TEST-F2-5" TO PAR-NAME.                        NC1774.2
057600     PERFORM  PRINT-DETAIL.                                       NC1774.2
057700 ADD-TEST-F2-6.                                                   NC1774.2
057800     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
057900     ADD      A05ONES-DS-05V00                                    NC1774.2
058000              A05ONES-DS-00V05                                    NC1774.2
058100              A12THREES-DS-06V06                                  NC1774.2
058200              A06THREES-DS-03V03 GIVING WRK-DS-06V06.             NC1774.2
058300     IF       WRK-DS-06V06 EQUAL TO 344777.777443                 NC1774.2
058400              PERFORM PASS GO TO ADD-WRITE-F2-6.                  NC1774.2
058500     GO       TO ADD-FAIL-F2-6.                                   NC1774.2
058600 ADD-DELETE-F2-6.                                                 NC1774.2
058700     PERFORM  DE-LETE.                                            NC1774.2
058800     GO       TO ADD-WRITE-F2-6.                                  NC1774.2
058900 ADD-FAIL-F2-6.                                                   NC1774.2
059000     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1774.2
059100     MOVE     344777.777443 TO CORRECT-N.                         NC1774.2
059200     PERFORM  FAIL.                                               NC1774.2
059300 ADD-WRITE-F2-6.                                                  NC1774.2
059400     MOVE     "ADD-TEST-F2-6" TO PAR-NAME.                        NC1774.2
059500     PERFORM PRINT-DETAIL.                                        NC1774.2
059600 ADD-TEST-F2-7.                                                   NC1774.2
059700     MOVE     ZERO TO WRK-DS-06V00.                               NC1774.2
059800     ADD      A05ONES-DS-00V05                                    NC1774.2
059900              A12THREES-DS-06V06                                  NC1774.2
060000              A05ONES-DS-00V05 GIVING WRK-DS-06V00 ROUNDED.       NC1774.2
060100     IF      WRK-DS-06V00 EQUAL TO 333334                         NC1774.2
060200     PERFORM PASS GO TO ADD-WRITE-F2-7.                           NC1774.2
060300     GO      TO ADD-FAIL-F2-7.                                    NC1774.2
060400 ADD-DELETE-F2-7.                                                 NC1774.2
060500     PERFORM  DE-LETE.                                            NC1774.2
060600     GO       TO ADD-WRITE-F2-7.                                  NC1774.2
060700 ADD-FAIL-F2-7.                                                   NC1774.2
060800     MOVE     WRK-DS-06V00 TO COMPUTED-N.                         NC1774.2
060900     MOVE    333334 TO CORRECT-N.                                 NC1774.2
061000     PERFORM FAIL.                                                NC1774.2
061100 ADD-WRITE-F2-7.                                                  NC1774.2
061200     MOVE     "ADD-TEST-F2-7" TO PAR-NAME.                        NC1774.2
061300     PERFORM  PRINT-DETAIL.                                       NC1774.2
061400 ADD-INIT-F2-8-1.                                                 NC1774.2
061500     MOVE     ZERO TO WRK-DS-10V00.                               NC1774.2
061600 ADD-TEST-F2-8-1.                                                 NC1774.2
061700     ADD      A12ONES-DS-12V00                                    NC1774.2
061800              ZERO GIVING WRK-DS-10V00 ON SIZE ERROR              NC1774.2
061900              PERFORM PASS GO TO ADD-WRITE-F2-8-1.                NC1774.2
062000     GO       TO ADD-FAIL-F2-8-1.                                 NC1774.2
062100 ADD-DELETE-F2-8-1.                                               NC1774.2
062200     PERFORM  DE-LETE.                                            NC1774.2
062300     GO       TO ADD-WRITE-F2-8-1.                                NC1774.2
062400 ADD-FAIL-F2-8-1.                                                 NC1774.2
062500     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1774.2
062600     PERFORM  FAIL.                                               NC1774.2
062700 ADD-WRITE-F2-8-1.                                                NC1774.2
062800     MOVE     "ADD-TEST-F2-8-1" TO PAR-NAME.                      NC1774.2
062900     PERFORM  PRINT-DETAIL.                                       NC1774.2
063000 ADD-TEST-F2-8-2.                                                 NC1774.2
063100     IF       WRK-DS-10V00 EQUAL TO ZERO                          NC1774.2
063200              PERFORM PASS GO TO ADD-WRITE-F2-8-2.                NC1774.2
063300*    NOTE     THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F2-8-1NC1774.2
063400     GO       TO ADD-FAIL-F2-8-2.                                 NC1774.2
063500 ADD-DELETE-F2-8-2.                                               NC1774.2
063600     PERFORM  DE-LETE.                                            NC1774.2
063700     GO       TO ADD-WRITE-F2-8-2.                                NC1774.2
063800 ADD-FAIL-F2-8-2.                                                 NC1774.2
063900     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1774.2
064000     MOVE     WRK-DS-10V00 TO COMPUTED-14V4.                      NC1774.2
064100     MOVE     ZERO TO CORRECT-14V4.                               NC1774.2
064200     PERFORM  FAIL.                                               NC1774.2
064300 ADD-WRITE-F2-8-2.                                                NC1774.2
064400     MOVE     "ADD-TEST-F2-8-2" TO PAR-NAME.                      NC1774.2
064500     PERFORM  PRINT-DETAIL.                                       NC1774.2
064600 ADD-TEST-F2-9-1.                                                 NC1774.2
064700     MOVE     ZERO TO WRK-DS-05V00                                NC1774.2
064800     ADD      33333                                               NC1774.2
064900              A06THREES-DS-03V03                                  NC1774.2
065000              A12THREES-DS-06V06                                  NC1774.2
065100              GIVING WRK-DS-05V00 ROUNDED ON SIZE ERROR           NC1774.2
065200              PERFORM PASS GO TO ADD-WRITE-F2-9-1.                NC1774.2
065300     GO       TO ADD-FAIL-F2-9-1.                                 NC1774.2
065400 ADD-DELETE-F2-9-1.                                               NC1774.2
065500     PERFORM  DE-LETE.                                            NC1774.2
065600     GO       TO ADD-WRITE-F2-9-1.                                NC1774.2
065700 ADD-FAIL-F2-9-1.                                                 NC1774.2
065800     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1774.2
065900     PERFORM  FAIL.                                               NC1774.2
066000 ADD-WRITE-F2-9-1.                                                NC1774.2
066100     MOVE     "ADD-TEST-F2-9-1" TO PAR-NAME.                      NC1774.2
066200     PERFORM  PRINT-DETAIL.                                       NC1774.2
066300 ADD-TEST-F2-9-2.                                                 NC1774.2
066400     IF       WRK-DS-05V00 EQUAL TO ZERO                          NC1774.2
066500              PERFORM PASS GO TO ADD-WRITE-F2-9-2.                NC1774.2
066600     GO       TO ADD-FAIL-F2-9-2.                                 NC1774.2
066700*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F2-9-1   NC1774.2
066800 ADD-DELETE-F2-9-2.                                               NC1774.2
066900     PERFORM  DE-LETE.                                            NC1774.2
067000     GO       TO ADD-WRITE-F2-9-2.                                NC1774.2
067100 ADD-FAIL-F2-9-2.                                                 NC1774.2
067200     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1774.2
067300     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC1774.2
067400     MOVE     ZERO TO CORRECT-N.                                  NC1774.2
067500     PERFORM  FAIL.                                               NC1774.2
067600 ADD-WRITE-F2-9-2.                                                NC1774.2
067700     MOVE     "ADD-TEST-F2-9-2" TO PAR-NAME.                      NC1774.2
067800     PERFORM  PRINT-DETAIL.                                       NC1774.2
067900 ADD-INIT-F2-10.                                                  NC1774.2
068000     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
068100 ADD-TEST-F2-10-1.                                                NC1774.2
068200     ADD      A12THREES-DS-06V06                                  NC1774.2
068300              333333                                              NC1774.2
068400              A06THREES-DS-03V03                                  NC1774.2
068500              GIVING WRK-DS-06V06 ROUNDED ON SIZE ERROR           NC1774.2
068600              GO TO ADD-FAIL-F2-10-1.                             NC1774.2
068700     PERFORM  PASS.                                               NC1774.2
068800     GO       TO ADD-WRITE-F2-10-1.                               NC1774.2
068900 ADD-DELETE-F2-10-1.                                              NC1774.2
069000     PERFORM  DE-LETE.                                            NC1774.2
069100     GO       TO ADD-WRITE-F2-10-1.                               NC1774.2
069200 ADD-FAIL-F2-10-1.                                                NC1774.2
069300     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1774.2
069400     PERFORM  FAIL.                                               NC1774.2
069500 ADD-WRITE-F2-10-1.                                               NC1774.2
069600     MOVE     "ADD-TEST-F2-10-1" TO PAR-NAME.                     NC1774.2
069700     PERFORM  PRINT-DETAIL.                                       NC1774.2
069800 ADD-TEST-F2-10-2.                                                NC1774.2
069900     IF       WRK-DS-06V06 EQUAL TO 666999.666333                 NC1774.2
070000              PERFORM PASS GO TO ADD-WRITE-F2-10-2.               NC1774.2
070100*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-F2-10-1  NC1774.2
070200     GO       TO ADD-FAIL-F2-10-2.                                NC1774.2
070300 ADD-DELETE-F2-10-2.                                              NC1774.2
070400     PERFORM  DE-LETE.                                            NC1774.2
070500     GO       TO ADD-WRITE-F2-10-2.                               NC1774.2
070600 ADD-FAIL-F2-10-2.                                                NC1774.2
070700     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1774.2
070800     MOVE     666999.666333 TO CORRECT-N.                         NC1774.2
070900     PERFORM  FAIL.                                               NC1774.2
071000 ADD-WRITE-F2-10-2.                                               NC1774.2
071100     MOVE     "ADD-TEST-F2-10-2" TO PAR-NAME.                     NC1774.2
071200     PERFORM  PRINT-DETAIL.                                       NC1774.2
071300 ADD-INIT-F2-11.                                                  NC1774.2
071400     MOVE "  SERIES" TO FEATURE.                                  NC1774.2
071500 ADD-TEST-F2-11.                                                  NC1774.2
071600     MOVE     ZERO TO WRK-DS-03V10.                               NC1774.2
071700     ADD      A99-DS-02V00                                        NC1774.2
071800              A03ONES-DS-02V01                                    NC1774.2
071900              A06ONES-DS-03V03                                    NC1774.2
072000              A08TWOS-DS-02V06                                    NC1774.2
072100              -1.1111111                                          NC1774.2
072200              +.11111111                                          NC1774.2
072300              A01ONE-DS-P0801 GIVING WRK-DS-03V10.                NC1774.2
072400     IF       WRK-DS-03V10 EQUAL TO 242.4332220110                NC1774.2
072500              PERFORM PASS GO TO ADD-WRITE-F2-11.                 NC1774.2
072600     GO       TO ADD-FAIL-F2-11.                                  NC1774.2
072700 ADD-DELETE-F2-11.                                                NC1774.2
072800     PERFORM  DE-LETE.                                            NC1774.2
072900     GO       TO ADD-WRITE-F2-11.                                 NC1774.2
073000 ADD-FAIL-F2-11.                                                  NC1774.2
073100     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1774.2
073200     MOVE     242.4332220110 TO CORRECT-4V14.                     NC1774.2
073300     PERFORM  FAIL.                                               NC1774.2
073400 ADD-WRITE-F2-11.                                                 NC1774.2
073500     MOVE     "ADD-TEST-F2-11" TO PAR-NAME.                       NC1774.2
073600     PERFORM  PRINT-DETAIL.                                       NC1774.2
073700 ADD-TEST-F2-12.                                                  NC1774.2
073800     MOVE     ZERO TO WRK-DS-03V10.                               NC1774.2
073900     ADD      A01ONE-DS-P0801                                     NC1774.2
074000              +.11111111                                          NC1774.2
074100              -1.1111111                                          NC1774.2
074200              A08TWOS-DS-02V06                                    NC1774.2
074300              A06ONES-DS-03V03                                    NC1774.2
074400              A03ONES-DS-02V01                                    NC1774.2
074500              A99-DS-02V00 GIVING WRK-DS-03V10.                   NC1774.2
074600     IF       WRK-DS-03V10 EQUAL TO 242.4332220110                NC1774.2
074700              PERFORM PASS GO TO ADD-WRITE-F2-12.                 NC1774.2
074800     GO       TO ADD-FAIL-F2-12.                                  NC1774.2
074900 ADD-DELETE-F2-12.                                                NC1774.2
075000     PERFORM  DE-LETE.                                            NC1774.2
075100     GO       TO ADD-WRITE-F2-12.                                 NC1774.2
075200 ADD-FAIL-F2-12.                                                  NC1774.2
075300     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1774.2
075400     MOVE     242.4332220110 TO CORRECT-4V14.                     NC1774.2
075500     PERFORM  FAIL.                                               NC1774.2
075600 ADD-WRITE-F2-12.                                                 NC1774.2
075700     MOVE     "ADD-TEST-F2-12" TO PAR-NAME.                       NC1774.2
075800     PERFORM  PRINT-DETAIL.                                       NC1774.2
075900 ADD-TEST-F2-13.                                                  NC1774.2
076000     MOVE     ZERO TO WRK-DS-03V10.                               NC1774.2
076100     ADD      A08TWOS-DS-02V06                                    NC1774.2
076200              A99-DS-02V00                                        NC1774.2
076300              -1.1111111                                          NC1774.2
076400              A03ONES-DS-02V01                                    NC1774.2
076500              A01ONE-DS-P0801                                     NC1774.2
076600              +.11111111                                          NC1774.2
076700              A06ONES-DS-03V03 GIVING WRK-DS-03V10.               NC1774.2
076800     IF       WRK-DS-03V10 EQUAL TO 242.4332220110                NC1774.2
076900              PERFORM PASS GO TO ADD-WRITE-F2-13.                 NC1774.2
077000     GO       TO ADD-FAIL-F2-13.                                  NC1774.2
077100 ADD-DELETE-F2-13.                                                NC1774.2
077200     PERFORM  DE-LETE.                                            NC1774.2
077300     GO       TO ADD-WRITE-F2-13.                                 NC1774.2
077400 ADD-FAIL-F2-13.                                                  NC1774.2
077500     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1774.2
077600     MOVE     242.4332220110 TO CORRECT-4V14.                     NC1774.2
077700     PERFORM  FAIL.                                               NC1774.2
077800 ADD-WRITE-F2-13.                                                 NC1774.2
077900     MOVE     "ADD-TEST-F2-13" TO PAR-NAME.                       NC1774.2
078000     PERFORM  PRINT-DETAIL.                                       NC1774.2
078100 ADD-TEST-F2-14.                                                  NC1774.2
078200     ADD ADD-12 ADD-13 GIVING ADD-14.                             NC1774.2
078300     IF       ADD-14 EQUAL TO 100.001                             NC1774.2
078400              PERFORM PASS GO TO ADD-WRITE-F2-14.                 NC1774.2
078500     GO       TO ADD-FAIL-F2-14.                                  NC1774.2
078600 ADD-DELETE-F2-14.                                                NC1774.2
078700     PERFORM DE-LETE.                                             NC1774.2
078800     GO       TO ADD-WRITE-F2-14.                                 NC1774.2
078900 ADD-FAIL-F2-14.                                                  NC1774.2
079000     MOVE     ADD-14  TO COMPUTED-N.                              NC1774.2
079100     MOVE     100.001   TO CORRECT-N.                             NC1774.2
079200     PERFORM FAIL.                                                NC1774.2
079300 ADD-WRITE-F2-14.                                                 NC1774.2
079400     MOVE     "ADD-TEST-F2-14" TO PAR-NAME.                       NC1774.2
079500     PERFORM PRINT-DETAIL.                                        NC1774.2
079600 ADD-TEST-F2-15-1.                                                NC1774.2
079700     MOVE SPACE TO SIZE-ERR.                                      NC1774.2
079800     ADD MINUS-NAME1 MINUS-NAME2 -34 -1 PLUS-NAME1                NC1774.2
079900         PLUS-NAME2 EVEN-NAME1 35  GIVING WHOLE-FIELD             NC1774.2
080000         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1774.2
080100     IF WHOLE-FIELD EQUAL TO +1                                   NC1774.2
080200         PERFORM PASS                                             NC1774.2
080300         GO TO ADD-WRITE-F2-15-1.                                 NC1774.2
080400     MOVE WHOLE-FIELD TO COMPUTED-18V0.                           NC1774.2
080500     MOVE +1 TO CORRECT-18V0.                                     NC1774.2
080600     PERFORM FAIL.                                                NC1774.2
080700     GO TO ADD-WRITE-F2-15-1.                                     NC1774.2
080800 ADD-DELETE-F2-15-1.                                              NC1774.2
080900     PERFORM DE-LETE.                                             NC1774.2
081000 ADD-WRITE-F2-15-1.                                               NC1774.2
081100     MOVE "ADD-TEST-F2-15-1" TO PAR-NAME.                         NC1774.2
081200     PERFORM PRINT-DETAIL.                                        NC1774.2
081300 ADD-TEST-F2-15-2.                                                NC1774.2
081400     IF SIZE-ERR EQUAL TO "1"                                     NC1774.2
081500         PERFORM FAIL                                             NC1774.2
081600         MOVE SPACE TO CORRECT-A                                  NC1774.2
081700         MOVE 1 TO COMPUTED-A                                     NC1774.2
081800         MOVE    "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1774.2
081900         GO TO ADD-WRITE-F2-15-2.                                 NC1774.2
082000     PERFORM PASS.                                                NC1774.2
082100     GO TO ADD-WRITE-F2-15-2.                                     NC1774.2
082200 ADD-DELETE-F2-15-2.                                              NC1774.2
082300     PERFORM DE-LETE.                                             NC1774.2
082400 ADD-WRITE-F2-15-2.                                               NC1774.2
082500     MOVE "ADD-TEST-F2-15-2" TO PAR-NAME.                         NC1774.2
082600     PERFORM PRINT-DETAIL.                                        NC1774.2
082700 ADD-TEST-F2-16-1.                                                NC1774.2
082800     MOVE SPACE TO SIZE-ERR.                                      NC1774.2
082900     ADD MINUS-NAME3 MINUS-NAME4 -.34 -.01 PLUS-NAME3             NC1774.2
083000         PLUS-NAME4 EVEN-NAME2 .35  GIVING DECMAL-FIELD           NC1774.2
083100         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1774.2
083200     IF DECMAL-FIELD EQUAL TO +.1                                 NC1774.2
083300         PERFORM PASS                                             NC1774.2
083400         GO TO ADD-WRITE-F2-16-1.                                 NC1774.2
083500     MOVE DECMAL-FIELD TO COMPUTED-0V18.                          NC1774.2
083600     MOVE +.1 TO CORRECT-0V18.                                    NC1774.2
083700     PERFORM FAIL.                                                NC1774.2
083800     GO TO ADD-WRITE-F2-16-1.                                     NC1774.2
083900 ADD-DELETE-F2-16-1.                                              NC1774.2
084000     PERFORM DE-LETE.                                             NC1774.2
084100 ADD-WRITE-F2-16-1.                                               NC1774.2
084200     MOVE "ADD-TEST-F2-16-1" TO PAR-NAME.                         NC1774.2
084300     PERFORM PRINT-DETAIL.                                        NC1774.2
084400 ADD-TEST-F2-16-2.                                                NC1774.2
084500     IF SIZE-ERR EQUAL TO "1"                                     NC1774.2
084600         PERFORM FAIL                                             NC1774.2
084700         MOVE SPACE TO CORRECT-A                                  NC1774.2
084800         MOVE 1 TO COMPUTED-A                                     NC1774.2
084900         MOVE "SIZE ERROR PRECEDING TEST  " TO RE-MARK            NC1774.2
085000         GO TO ADD-WRITE-F2-16-2.                                 NC1774.2
085100     PERFORM PASS.                                                NC1774.2
085200     GO TO ADD-WRITE-F2-16-2.                                     NC1774.2
085300 ADD-DELETE-F2-16-2.                                              NC1774.2
085400     PERFORM DE-LETE.                                             NC1774.2
085500 ADD-WRITE-F2-16-2.                                               NC1774.2
085600     MOVE "ADD-TEST-F2-16-2" TO PAR-NAME.                         NC1774.2
085700     PERFORM PRINT-DETAIL.                                        NC1774.2
085800 ADD-TEST-F2-17.                                                  NC1774.2
085900     MOVE ZERO TO WRK-CS-18V00.                                   NC1774.2
086000     ADD A18ONES-CS-18V00  A18ONES-DS-18V00 GIVING WRK-CS-18V00.  NC1774.2
086100     IF WRK-CS-18V00 EQUAL TO 222222222222222222                  NC1774.2
086200         PERFORM PASS                                             NC1774.2
086300         GO TO ADD-WRITE-F2-17.                                   NC1774.2
086400     MOVE 222222222222222222 TO CORRECT-18V0.                     NC1774.2
086500     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1774.2
086600     PERFORM FAIL.                                                NC1774.2
086700     GO TO ADD-WRITE-F2-17.                                       NC1774.2
086800 ADD-DELETE-F2-17.                                                NC1774.2
086900     PERFORM DE-LETE.                                             NC1774.2
087000 ADD-WRITE-F2-17.                                                 NC1774.2
087100     MOVE "ADD-TEST-F2-17 " TO PAR-NAME.                          NC1774.2
087200     PERFORM PRINT-DETAIL.                                        NC1774.2
087300 ADD-TEST-F2-18.                                                  NC1774.2
087400     MOVE ZERO TO WRK-CS-18V00.                                   NC1774.2
087500     ADD A18FIVES-CS-18V00  A18SIXES-CS-18V00 GIVING              NC1774.2
087600             WRK-CS-18V00.                                        NC1774.2
087700     IF WRK-CS-18V00 EQUAL TO 111111111111111111                  NC1774.2
087800         PERFORM PASS                                             NC1774.2
087900         GO TO ADD-WRITE-F2-18.                                   NC1774.2
088000     MOVE 111111111111111111 TO CORRECT-18V0.                     NC1774.2
088100     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1774.2
088200     PERFORM FAIL.                                                NC1774.2
088300     GO TO ADD-WRITE-F2-18.                                       NC1774.2
088400 ADD-DELETE-F2-18.                                                NC1774.2
088500     PERFORM DE-LETE.                                             NC1774.2
088600 ADD-WRITE-F2-18.                                                 NC1774.2
088700     MOVE "ADD-TEST-F2-18 " TO PAR-NAME.                          NC1774.2
088800     PERFORM PRINT-DETAIL.                                        NC1774.2
088900 ADD-TEST-F2-19.                                                  NC1774.2
089000     MOVE ZERO TO WRK-DS-18V00.                                   NC1774.2
089100     ADD A18SIXES-CS-18V00  A12SEVENS-CU-18V00 GIVING             NC1774.2
089200             WRK-DS-18V00.                                        NC1774.2
089300     IF WRK-DS-18V00 EQUAL TO 666667444444444443                  NC1774.2
089400         PERFORM PASS                                             NC1774.2
089500         GO TO ADD-WRITE-F2-19.                                   NC1774.2
089600     MOVE 666667444444444443 TO CORRECT-18V0.                     NC1774.2
089700     MOVE WRK-DS-18V00 TO COMPUTED-18V0.                          NC1774.2
089800     PERFORM FAIL.                                                NC1774.2
089900     GO TO ADD-WRITE-F2-19.                                       NC1774.2
090000 ADD-DELETE-F2-19.                                                NC1774.2
090100     PERFORM DE-LETE.                                             NC1774.2
090200 ADD-WRITE-F2-19.                                                 NC1774.2
090300     MOVE "ADD-TEST-F2-19 " TO PAR-NAME.                          NC1774.2
090400     PERFORM PRINT-DETAIL.                                        NC1774.2
090500 ADD-TEST-F2-20.                                                  NC1774.2
090600     MOVE ZERO TO WRK-CS-18V00.                                   NC1774.2
090700     ADD A14TWOS-CS-18V00  A12THREES-CU-18V00 GIVING              NC1774.2
090800             WRK-CS-18V00 ROUNDED.                                NC1774.2
090900     IF WRK-CS-18V00 EQUAL TO -000021888888888889                 NC1774.2
091000         PERFORM PASS                                             NC1774.2
091100         GO TO ADD-WRITE-F2-20.                                   NC1774.2
091200     MOVE -000021888888888889 TO CORRECT-18V0.                    NC1774.2
091300     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1774.2
091400     PERFORM FAIL.                                                NC1774.2
091500     GO TO ADD-WRITE-F2-20.                                       NC1774.2
091600 ADD-DELETE-F2-20.                                                NC1774.2
091700     PERFORM DE-LETE.                                             NC1774.2
091800 ADD-WRITE-F2-20.                                                 NC1774.2
091900     MOVE "ADD-TEST-F2-20 " TO PAR-NAME.                          NC1774.2
092000     PERFORM PRINT-DETAIL.                                        NC1774.2
092100 ADD-TEST-F2-21.                                                  NC1774.2
092200     MOVE ZERO TO WRK-CS-18V00.                                   NC1774.2
092300     ADD A14TWOS-CS-18V00  A14TWOS-CS-18V00                       NC1774.2
092400             GIVING WRK-CS-18V00.                                 NC1774.2
092500     IF WRK-CS-18V00 EQUAL TO -000044444444444444                 NC1774.2
092600         PERFORM PASS                                             NC1774.2
092700         GO TO ADD-WRITE-F2-21.                                   NC1774.2
092800     MOVE -000044444444444444 TO CORRECT-18V0.                    NC1774.2
092900     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1774.2
093000     PERFORM FAIL.                                                NC1774.2
093100     GO TO ADD-WRITE-F2-21.                                       NC1774.2
093200 ADD-DELETE-F2-21.                                                NC1774.2
093300     PERFORM DE-LETE.                                             NC1774.2
093400 ADD-WRITE-F2-21.                                                 NC1774.2
093500     MOVE "ADD-TEST-F2-21 " TO PAR-NAME.                          NC1774.2
093600     PERFORM PRINT-DETAIL.                                        NC1774.2
093700 ADD-TEST-F2-22.                                                  NC1774.2
093800     MOVE ZERO TO WRK-DU-18V00.                                   NC1774.2
093900     ADD A14TWOS-CS-18V00  A18FIVES-CS-18V00 GIVING               NC1774.2
094000             WRK-DU-18V00.                                        NC1774.2
094100     IF WRK-DU-18V00 EQUAL TO 555577777777777777                  NC1774.2
094200         PERFORM PASS                                             NC1774.2
094300         GO TO ADD-WRITE-F2-22.                                   NC1774.2
094400     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1774.2
094500     MOVE 555577777777777777 TO CORRECT-18V0.                     NC1774.2
094600     PERFORM FAIL.                                                NC1774.2
094700     GO TO ADD-WRITE-F2-22.                                       NC1774.2
094800 ADD-DELETE-F2-22.                                                NC1774.2
094900     PERFORM DE-LETE.                                             NC1774.2
095000 ADD-WRITE-F2-22.                                                 NC1774.2
095100     MOVE "ADD-TEST-F2-22 " TO PAR-NAME.                          NC1774.2
095200     PERFORM PRINT-DETAIL.                                        NC1774.2
095300*                                                                 NC1774.2
095400 ADD-INIT-F2-23.                                                  NC1774.2
095500*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
095600     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
095700     MOVE     ZERO TO WRK-DS-10V00.                               NC1774.2
095800 ADD-TEST-F2-23.                                                  NC1774.2
095900     ADD      A12ONES-DS-12V00                                    NC1774.2
096000              ZERO GIVING WRK-DS-10V00                            NC1774.2
096100              NOT ON SIZE ERROR                                   NC1774.2
096200              MOVE "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"     NC1774.2
096300                   TO RE-MARK                                     NC1774.2
096400              PERFORM FAIL GO TO ADD-WRITE-F2-23.                 NC1774.2
096500     GO       TO ADD-PASS-F2-23.                                  NC1774.2
096600 ADD-DELETE-F2-23.                                                NC1774.2
096700     PERFORM  DE-LETE.                                            NC1774.2
096800     GO       TO ADD-WRITE-F2-23.                                 NC1774.2
096900 ADD-PASS-F2-23.                                                  NC1774.2
097000     PERFORM  PASS.                                               NC1774.2
097100 ADD-WRITE-F2-23.                                                 NC1774.2
097200     MOVE     "ADD-TEST-F2-23" TO PAR-NAME.                       NC1774.2
097300     PERFORM  PRINT-DETAIL.                                       NC1774.2
097400*                                                                 NC1774.2
097500 ADD-INIT-F2-24.                                                  NC1774.2
097600*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
097700     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
097800     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
097900 ADD-TEST-F2-24.                                                  NC1774.2
098000     ADD      A12THREES-DS-06V06                                  NC1774.2
098100              333333                                              NC1774.2
098200              A06THREES-DS-03V03                                  NC1774.2
098300              GIVING WRK-DS-06V06 ROUNDED                         NC1774.2
098400              NOT ON SIZE ERROR                                   NC1774.2
098500              PERFORM PASS                                        NC1774.2
098600              GO TO   ADD-WRITE-F2-24.                            NC1774.2
098700     GO TO    ADD-FAIL-F2-24.                                     NC1774.2
098800 ADD-DELETE-F2-24.                                                NC1774.2
098900     PERFORM  DE-LETE.                                            NC1774.2
099000     GO       TO ADD-WRITE-F2-24.                                 NC1774.2
099100 ADD-FAIL-F2-24.                                                  NC1774.2
099200     MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED" TO RE-MARK.  NC1774.2
099300     PERFORM FAIL.                                                NC1774.2
099400 ADD-WRITE-F2-24.                                                 NC1774.2
099500     MOVE     "ADD-TEST-F2-24" TO PAR-NAME.                       NC1774.2
099600     PERFORM  PRINT-DETAIL.                                       NC1774.2
099700*                                                                 NC1774.2
099800 ADD-INIT-F2-25.                                                  NC1774.2
099900*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
100000     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
100100     MOVE     ZERO TO WRK-DS-10V00.                               NC1774.2
100200 ADD-TEST-F2-25.                                                  NC1774.2
100300     ADD      A12ONES-DS-12V00                                    NC1774.2
100400              ZERO GIVING WRK-DS-10V00                            NC1774.2
100500              ON SIZE ERROR                                       NC1774.2
100600              GO TO ADD-PASS-F2-25                                NC1774.2
100700              NOT ON SIZE ERROR                                   NC1774.2
100800              MOVE "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"     NC1774.2
100900                   TO RE-MARK                                     NC1774.2
101000              PERFORM FAIL GO TO ADD-WRITE-F2-25.                 NC1774.2
101100 ADD-DELETE-F2-25.                                                NC1774.2
101200     PERFORM  DE-LETE.                                            NC1774.2
101300     GO       TO ADD-WRITE-F2-25.                                 NC1774.2
101400 ADD-PASS-F2-25.                                                  NC1774.2
101500     PERFORM  PASS.                                               NC1774.2
101600 ADD-WRITE-F2-25.                                                 NC1774.2
101700     MOVE     "ADD-TEST-F2-25" TO PAR-NAME.                       NC1774.2
101800     PERFORM  PRINT-DETAIL.                                       NC1774.2
101900*                                                                 NC1774.2
102000 ADD-INIT-F2-26.                                                  NC1774.2
102100*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
102200     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
102300     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
102400 ADD-TEST-F2-26.                                                  NC1774.2
102500     ADD      A12THREES-DS-06V06                                  NC1774.2
102600              333333                                              NC1774.2
102700              A06THREES-DS-03V03                                  NC1774.2
102800              GIVING WRK-DS-06V06 ROUNDED                         NC1774.2
102900              ON SIZE ERROR                                       NC1774.2
103000              GO TO    ADD-FAIL-F2-26                             NC1774.2
103100              NOT ON SIZE ERROR                                   NC1774.2
103200              PERFORM PASS                                        NC1774.2
103300              GO TO   ADD-WRITE-F2-26.                            NC1774.2
103400 ADD-DELETE-F2-26.                                                NC1774.2
103500     PERFORM  DE-LETE.                                            NC1774.2
103600     GO       TO ADD-WRITE-F2-26.                                 NC1774.2
103700 ADD-FAIL-F2-26.                                                  NC1774.2
103800     MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED" TO RE-MARK.  NC1774.2
103900     PERFORM FAIL.                                                NC1774.2
104000 ADD-WRITE-F2-26.                                                 NC1774.2
104100     MOVE     "ADD-TEST-F2-26" TO PAR-NAME.                       NC1774.2
104200     PERFORM  PRINT-DETAIL.                                       NC1774.2
104300*                                                                 NC1774.2
104400 ADD-INIT-F2-27.                                                  NC1774.2
104500*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
104600     MOVE   "VI-74 6.6.4 GR2" TO ANSI-REFERENCE.                  NC1774.2
104700     MOVE   "ADD-TEST-F2-27" TO PAR-NAME.                         NC1774.2
104800     MOVE    1    TO REC-CT.                                      NC1774.2
104900     MOVE    1.1  TO WRK-DU-1V1-1.                                NC1774.2
105000     MOVE    2.3  TO WRK-DU-1V1-2.                                NC1774.2
105100     MOVE    ZERO TO WRK-DU-2V0-1.                                NC1774.2
105200     MOVE    ZERO TO WRK-DU-2V1-1.                                NC1774.2
105300     MOVE    ZERO TO WRK-DU-2V0-2.                                NC1774.2
105400     MOVE    ZERO TO WRK-DU-2V1-2.                                NC1774.2
105500     MOVE    ZERO TO WRK-DU-2V0-3.                                NC1774.2
105600     MOVE    ZERO TO WRK-DU-2V1-3.                                NC1774.2
105700 ADD-GIVING-TEST-F2-27-0.                                         NC1774.2
105800     ADD WRK-DU-1V1-1 6 WRK-DU-1V1-2 GIVING WRK-DU-2V1-1          NC1774.2
105900         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
106000         WRK-DU-2V1-3 WRK-DU-2V0-3.                               NC1774.2
106100     GO TO ADD-TEST-F2-27-1.                                      NC1774.2
106200 ADD-DELETE-F2-27.                                                NC1774.2
106300     PERFORM DE-LETE.                                             NC1774.2
106400     PERFORM PRINT-DETAIL.                                        NC1774.2
106500     GO TO   ADD-INIT-F2-28.                                      NC1774.2
106600 ADD-TEST-F2-27-1.                                                NC1774.2
106700     IF WRK-DU-2V1-1 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
106800     ELSE                                                         NC1774.2
106900     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.4        NC1774.2
107000         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
107100     ADD 1 TO REC-CT.                                             NC1774.2
107200 ADD-TEST-F2-27-2.                                                NC1774.2
107300     IF WRK-DU-2V0-1 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
107400     ELSE                                                         NC1774.2
107500     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 9          NC1774.2
107600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
107700     ADD 1 TO REC-CT.                                             NC1774.2
107800 ADD-ADD-TEST-F2-27-3.                                            NC1774.2
107900     IF WRK-DU-2V1-2 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
108000     ELSE                                                         NC1774.2
108100     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.4        NC1774.2
108200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
108300     ADD 1 TO REC-CT.                                             NC1774.2
108400 ADD-TEST-F2-27-4.                                                NC1774.2
108500     IF WRK-DU-2V0-2 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
108600     ELSE                                                         NC1774.2
108700     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 9          NC1774.2
108800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
108900     ADD 1 TO REC-CT.                                             NC1774.2
109000 ADD-TEST-F2-27-5.                                                NC1774.2
109100     IF WRK-DU-2V1-3 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
109200     ELSE                                                         NC1774.2
109300     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.4        NC1774.2
109400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
109500     ADD 1 TO REC-CT.                                             NC1774.2
109600 ADD-TEST-F2-27-6.                                                NC1774.2
109700     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
109800     ELSE                                                         NC1774.2
109900     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9          NC1774.2
110000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
110100*                                                                 NC1774.2
110200 ADD-INIT-F2-28.                                                  NC1774.2
110300*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
110400*    ==-->      SIZE ERROR        <--==                           NC1774.2
110500     MOVE   "VI-67 6.4.2"    TO ANSI-REFERENCE.                   NC1774.2
110600     MOVE    1     TO REC-CT.                                     NC1774.2
110700     MOVE    1.1   TO WRK-DU-1V1-1.                               NC1774.2
110800     MOVE    2.3   TO WRK-DU-1V1-2.                               NC1774.2
110900     MOVE    ZERO  TO WRK-DU-2V0-1.                               NC1774.2
111000     MOVE    ZERO  TO WRK-DU-2V1-1.                               NC1774.2
111100     MOVE    ZERO  TO WRK-DU-2V0-2.                               NC1774.2
111200     MOVE    ZERO  TO WRK-DU-2V1-2.                               NC1774.2
111300     MOVE    ZERO  TO WRK-DU-2V0-3.                               NC1774.2
111400     MOVE    ZERO  TO WRK-DU-2V1-3.                               NC1774.2
111500     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
111600 ADD-GIVING-TEST-F2-28-0.                                         NC1774.2
111700     ADD     A17TWOS-DS-17V00                                     NC1774.2
111800             WRK-DU-1V1-1                                         NC1774.2
111900             6                                                    NC1774.2
112000             WRK-DU-1V1-2                                         NC1774.2
112100      GIVING WRK-DU-2V1-1                                         NC1774.2
112200         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
112300         WRK-DU-2V1-3 WRK-DU-2V0-3                                NC1774.2
112400         ON SIZE ERROR                                            NC1774.2
112500            MOVE "A" TO SIZE-ERR2.                                NC1774.2
112600     GO TO  ADD-TEST-F2-28-1.                                     NC1774.2
112700 ADD-DELETE-F2-28.                                                NC1774.2
112800     PERFORM DE-LETE.                                             NC1774.2
112900     PERFORM PRINT-DETAIL.                                        NC1774.2
113000     GO TO   ADD-INIT-F2-29.                                      NC1774.2
113100 ADD-TEST-F2-28-1.                                                NC1774.2
113200     MOVE   "ADD-TEST-F2-28-1" TO PAR-NAME.                       NC1774.2
113300     IF WRK-DU-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
113400     ELSE                                                         NC1774.2
113500     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE ZERO       NC1774.2
113600         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
113700     ADD 1 TO REC-CT.                                             NC1774.2
113800 ADD-TEST-F2-28-2.                                                NC1774.2
113900     MOVE   "ADD-TEST-F2-28-2" TO PAR-NAME.                       NC1774.2
114000     IF WRK-DU-2V0-1 = ZERO  PERFORM PASS PERFORM PRINT-DETAIL    NC1774.2
114100     ELSE                                                         NC1774.2
114200     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE ZERO       NC1774.2
114300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
114400     ADD 1 TO REC-CT.                                             NC1774.2
114500 ADD-ADD-TEST-F2-28-3.                                            NC1774.2
114600     MOVE   "ADD-TEST-F2-28-3" TO PAR-NAME.                       NC1774.2
114700     IF WRK-DU-2V1-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
114800     ELSE                                                         NC1774.2
114900     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE ZERO       NC1774.2
115000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
115100     ADD 1 TO REC-CT.                                             NC1774.2
115200 ADD-TEST-F2-28-4.                                                NC1774.2
115300     MOVE   "ADD-TEST-F2-28-4" TO PAR-NAME.                       NC1774.2
115400     IF WRK-DU-2V0-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
115500     ELSE                                                         NC1774.2
115600     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE ZERO       NC1774.2
115700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
115800     ADD 1 TO REC-CT.                                             NC1774.2
115900 ADD-TEST-F2-28-5.                                                NC1774.2
116000     MOVE   "ADD-TEST-F2-28-5" TO PAR-NAME.                       NC1774.2
116100     IF WRK-DU-2V1-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
116200     ELSE                                                         NC1774.2
116300     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE ZERO       NC1774.2
116400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
116500     ADD 1 TO REC-CT.                                             NC1774.2
116600 ADD-TEST-F2-28-6.                                                NC1774.2
116700     MOVE   "ADD-TEST-F2-28-6" TO PAR-NAME.                       NC1774.2
116800     IF WRK-DU-2V0-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
116900     ELSE                                                         NC1774.2
117000     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE ZERO       NC1774.2
117100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
117200     ADD 1 TO REC-CT.                                             NC1774.2
117300 ADD-TEST-F2-28-7.                                                NC1774.2
117400     MOVE   "ADD-TEST-F2-28-7" TO PAR-NAME.                       NC1774.2
117500     IF      SIZE-ERR2 = "A"                                      NC1774.2
117600             PERFORM PASS                                         NC1774.2
117700             PERFORM PRINT-DETAIL                                 NC1774.2
117800     ELSE                                                         NC1774.2
117900             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1774.2
118000                  TO RE-MARK                                      NC1774.2
118100             MOVE   "A"        TO CORRECT-X                       NC1774.2
118200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
118300             PERFORM FAIL                                         NC1774.2
118400             PERFORM PRINT-DETAIL.                                NC1774.2
118500*                                                                 NC1774.2
118600 ADD-INIT-F2-29.                                                  NC1774.2
118700*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
118800*    ==-->   NO SIZE ERROR        <--==                           NC1774.2
118900     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
119000     MOVE    1     TO REC-CT.                                     NC1774.2
119100     MOVE    1.1   TO WRK-DU-1V1-1.                               NC1774.2
119200     MOVE    2.3   TO WRK-DU-1V1-2.                               NC1774.2
119300     MOVE    ZERO  TO WRK-DU-2V0-1.                               NC1774.2
119400     MOVE    ZERO  TO WRK-DU-2V1-1.                               NC1774.2
119500     MOVE    ZERO  TO WRK-DU-2V0-2.                               NC1774.2
119600     MOVE    ZERO  TO WRK-DU-2V1-2.                               NC1774.2
119700     MOVE    ZERO  TO WRK-DU-2V0-3.                               NC1774.2
119800     MOVE    ZERO  TO WRK-DU-2V1-3.                               NC1774.2
119900     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
120000 ADD-GIVING-TEST-F2-29-0.                                         NC1774.2
120100     ADD WRK-DU-1V1-1 6 WRK-DU-1V1-2 GIVING WRK-DU-2V1-1          NC1774.2
120200         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
120300         WRK-DU-2V1-3 WRK-DU-2V0-3                                NC1774.2
120400         ON SIZE ERROR                                            NC1774.2
120500            MOVE "A" TO SIZE-ERR2.                                NC1774.2
120600     GO TO  ADD-TEST-F2-29-1.                                     NC1774.2
120700 ADD-DELETE-F2-29.                                                NC1774.2
120800     PERFORM DE-LETE.                                             NC1774.2
120900     PERFORM PRINT-DETAIL.                                        NC1774.2
121000     GO TO   ADD-INIT-F2-30.                                      NC1774.2
121100 ADD-TEST-F2-29-1.                                                NC1774.2
121200     MOVE   "ADD-TEST-F2-29-1" TO PAR-NAME.                       NC1774.2
121300     IF WRK-DU-2V1-1 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
121400     ELSE                                                         NC1774.2
121500     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.4        NC1774.2
121600         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
121700     ADD 1 TO REC-CT.                                             NC1774.2
121800 ADD-TEST-F2-29-2.                                                NC1774.2
121900     MOVE   "ADD-TEST-F2-29-2" TO PAR-NAME.                       NC1774.2
122000     IF WRK-DU-2V0-1 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
122100     ELSE                                                         NC1774.2
122200     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 9          NC1774.2
122300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
122400     ADD 1 TO REC-CT.                                             NC1774.2
122500 ADD-ADD-TEST-F2-29-3.                                            NC1774.2
122600     MOVE   "ADD-TEST-F2-29-3" TO PAR-NAME.                       NC1774.2
122700     IF WRK-DU-2V1-2 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
122800     ELSE                                                         NC1774.2
122900     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.4        NC1774.2
123000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
123100     ADD 1 TO REC-CT.                                             NC1774.2
123200 ADD-TEST-F2-29-4.                                                NC1774.2
123300     MOVE   "ADD-TEST-F2-29-4" TO PAR-NAME.                       NC1774.2
123400     IF WRK-DU-2V0-2 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
123500     ELSE                                                         NC1774.2
123600     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 9          NC1774.2
123700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
123800     ADD 1 TO REC-CT.                                             NC1774.2
123900 ADD-TEST-F2-29-5.                                                NC1774.2
124000     MOVE   "ADD-TEST-F2-29-5" TO PAR-NAME.                       NC1774.2
124100     IF WRK-DU-2V1-3 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
124200     ELSE                                                         NC1774.2
124300     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.4        NC1774.2
124400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
124500     ADD 1 TO REC-CT.                                             NC1774.2
124600 ADD-TEST-F2-29-6.                                                NC1774.2
124700     MOVE   "ADD-TEST-F2-29-6" TO PAR-NAME.                       NC1774.2
124800     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
124900     ELSE                                                         NC1774.2
125000     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9          NC1774.2
125100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
125200     ADD 1 TO REC-CT.                                             NC1774.2
125300 ADD-TEST-F2-29-7.                                                NC1774.2
125400     MOVE   "ADD-TEST-F2-29-7" TO PAR-NAME.                       NC1774.2
125500     IF      SIZE-ERR2 = SPACE                                    NC1774.2
125600             PERFORM PASS                                         NC1774.2
125700             PERFORM PRINT-DETAIL                                 NC1774.2
125800     ELSE                                                         NC1774.2
125900             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1774.2
126000                  TO RE-MARK                                      NC1774.2
126100             MOVE    SPACE     TO CORRECT-X                       NC1774.2
126200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
126300             PERFORM FAIL                                         NC1774.2
126400             PERFORM PRINT-DETAIL.                                NC1774.2
126500*                                                                 NC1774.2
126600 ADD-INIT-F2-30.                                                  NC1774.2
126700*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
126800*    ==-->      SIZE ERROR        <--==                           NC1774.2
126900     MOVE   "VI-67 6.4.2"    TO ANSI-REFERENCE.                   NC1774.2
127000     MOVE    1     TO REC-CT.                                     NC1774.2
127100     MOVE    1.1   TO WRK-DU-1V1-1.                               NC1774.2
127200     MOVE    2.3   TO WRK-DU-1V1-2.                               NC1774.2
127300     MOVE    ZERO  TO WRK-DU-2V0-1.                               NC1774.2
127400     MOVE    ZERO  TO WRK-DU-2V1-1.                               NC1774.2
127500     MOVE    ZERO  TO WRK-DU-2V0-2.                               NC1774.2
127600     MOVE    ZERO  TO WRK-DU-2V1-2.                               NC1774.2
127700     MOVE    ZERO  TO WRK-DU-2V0-3.                               NC1774.2
127800     MOVE    ZERO  TO WRK-DU-2V1-3.                               NC1774.2
127900     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
128000 ADD-GIVING-TEST-F2-30-0.                                         NC1774.2
128100     ADD     A17TWOS-DS-17V00                                     NC1774.2
128200             WRK-DU-1V1-1                                         NC1774.2
128300             6                                                    NC1774.2
128400             WRK-DU-1V1-2                                         NC1774.2
128500      GIVING WRK-DU-2V1-1                                         NC1774.2
128600         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
128700         WRK-DU-2V1-3 WRK-DU-2V0-3                                NC1774.2
128800      NOT ON SIZE ERROR                                           NC1774.2
128900             MOVE "A" TO SIZE-ERR2.                               NC1774.2
129000     GO TO   ADD-TEST-F2-30-1.                                    NC1774.2
129100 ADD-DELETE-F2-30.                                                NC1774.2
129200     PERFORM DE-LETE.                                             NC1774.2
129300     PERFORM PRINT-DETAIL.                                        NC1774.2
129400     GO TO   ADD-INIT-F2-31.                                      NC1774.2
129500 ADD-TEST-F2-30-1.                                                NC1774.2
129600     MOVE   "ADD-TEST-F2-30-1" TO PAR-NAME.                       NC1774.2
129700     IF WRK-DU-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
129800     ELSE                                                         NC1774.2
129900     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE ZERO       NC1774.2
130000         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
130100     ADD 1 TO REC-CT.                                             NC1774.2
130200 ADD-TEST-F2-30-2.                                                NC1774.2
130300     MOVE   "ADD-TEST-F2-30-2" TO PAR-NAME.                       NC1774.2
130400     IF WRK-DU-2V0-1 = ZERO  PERFORM PASS PERFORM PRINT-DETAIL    NC1774.2
130500     ELSE                                                         NC1774.2
130600     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE ZERO       NC1774.2
130700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
130800     ADD 1 TO REC-CT.                                             NC1774.2
130900 ADD-ADD-TEST-F2-30-3.                                            NC1774.2
131000     MOVE   "ADD-TEST-F2-30-3" TO PAR-NAME.                       NC1774.2
131100     IF WRK-DU-2V1-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
131200     ELSE                                                         NC1774.2
131300     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE ZERO       NC1774.2
131400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
131500     ADD 1 TO REC-CT.                                             NC1774.2
131600 ADD-TEST-F2-30-4.                                                NC1774.2
131700     MOVE   "ADD-TEST-F2-30-4" TO PAR-NAME.                       NC1774.2
131800     IF WRK-DU-2V0-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
131900     ELSE                                                         NC1774.2
132000     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE ZERO       NC1774.2
132100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
132200     ADD 1 TO REC-CT.                                             NC1774.2
132300 ADD-TEST-F2-30-5.                                                NC1774.2
132400     MOVE   "ADD-TEST-F2-30-5" TO PAR-NAME.                       NC1774.2
132500     IF WRK-DU-2V1-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
132600     ELSE                                                         NC1774.2
132700     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE ZERO       NC1774.2
132800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
132900     ADD 1 TO REC-CT.                                             NC1774.2
133000 ADD-TEST-F2-30-6.                                                NC1774.2
133100     MOVE   "ADD-TEST-F2-30-6" TO PAR-NAME.                       NC1774.2
133200     IF WRK-DU-2V0-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
133300     ELSE                                                         NC1774.2
133400     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE ZERO       NC1774.2
133500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
133600     ADD 1 TO REC-CT.                                             NC1774.2
133700 ADD-TEST-F2-30-7.                                                NC1774.2
133800     MOVE   "ADD-TEST-F2-30-7" TO PAR-NAME.                       NC1774.2
133900     IF      SIZE-ERR2 = SPACE                                    NC1774.2
134000             PERFORM PASS                                         NC1774.2
134100             PERFORM PRINT-DETAIL                                 NC1774.2
134200     ELSE                                                         NC1774.2
134300             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1774.2
134400                  TO RE-MARK                                      NC1774.2
134500             MOVE    SPACE     TO CORRECT-X                       NC1774.2
134600             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
134700             PERFORM FAIL                                         NC1774.2
134800             PERFORM PRINT-DETAIL.                                NC1774.2
134900*                                                                 NC1774.2
135000 ADD-INIT-F2-31.                                                  NC1774.2
135100*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
135200*    ==-->   NO SIZE ERROR        <--==                           NC1774.2
135300     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
135400     MOVE    1     TO REC-CT.                                     NC1774.2
135500     MOVE    1.1   TO WRK-DU-1V1-1.                               NC1774.2
135600     MOVE    2.3   TO WRK-DU-1V1-2.                               NC1774.2
135700     MOVE    ZERO  TO WRK-DU-2V0-1.                               NC1774.2
135800     MOVE    ZERO  TO WRK-DU-2V1-1.                               NC1774.2
135900     MOVE    ZERO  TO WRK-DU-2V0-2.                               NC1774.2
136000     MOVE    ZERO  TO WRK-DU-2V1-2.                               NC1774.2
136100     MOVE    ZERO  TO WRK-DU-2V0-3.                               NC1774.2
136200     MOVE    ZERO  TO WRK-DU-2V1-3.                               NC1774.2
136300     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
136400 ADD-GIVING-TEST-F2-31-0.                                         NC1774.2
136500     ADD WRK-DU-1V1-1 6 WRK-DU-1V1-2 GIVING WRK-DU-2V1-1          NC1774.2
136600         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
136700         WRK-DU-2V1-3 WRK-DU-2V0-3                                NC1774.2
136800      NOT ON SIZE ERROR                                           NC1774.2
136900             MOVE "A" TO SIZE-ERR2.                               NC1774.2
137000     GO TO   ADD-TEST-F2-31-1.                                    NC1774.2
137100 ADD-DELETE-F2-31.                                                NC1774.2
137200     PERFORM DE-LETE.                                             NC1774.2
137300     PERFORM PRINT-DETAIL.                                        NC1774.2
137400     GO TO   ADD-INIT-F2-32.                                      NC1774.2
137500 ADD-TEST-F2-31-1.                                                NC1774.2
137600     MOVE   "ADD-TEST-F2-31-1" TO PAR-NAME.                       NC1774.2
137700     IF WRK-DU-2V1-1 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
137800     ELSE                                                         NC1774.2
137900     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.4        NC1774.2
138000         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
138100     ADD 1 TO REC-CT.                                             NC1774.2
138200 ADD-TEST-F2-31-2.                                                NC1774.2
138300     MOVE   "ADD-TEST-F2-31-2" TO PAR-NAME.                       NC1774.2
138400     IF WRK-DU-2V0-1 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
138500     ELSE                                                         NC1774.2
138600     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 9          NC1774.2
138700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
138800     ADD 1 TO REC-CT.                                             NC1774.2
138900 ADD-ADD-TEST-F2-31-3.                                            NC1774.2
139000     MOVE   "ADD-TEST-F2-31-3" TO PAR-NAME.                       NC1774.2
139100     IF WRK-DU-2V1-2 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
139200     ELSE                                                         NC1774.2
139300     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.4        NC1774.2
139400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
139500     ADD 1 TO REC-CT.                                             NC1774.2
139600 ADD-TEST-F2-31-4.                                                NC1774.2
139700     MOVE   "ADD-TEST-F2-31-4" TO PAR-NAME.                       NC1774.2
139800     IF WRK-DU-2V0-2 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
139900     ELSE                                                         NC1774.2
140000     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 9          NC1774.2
140100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
140200     ADD 1 TO REC-CT.                                             NC1774.2
140300 ADD-TEST-F2-31-5.                                                NC1774.2
140400     MOVE   "ADD-TEST-F2-31-5" TO PAR-NAME.                       NC1774.2
140500     IF WRK-DU-2V1-3 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
140600     ELSE                                                         NC1774.2
140700     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.4        NC1774.2
140800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
140900     ADD 1 TO REC-CT.                                             NC1774.2
141000 ADD-TEST-F2-31-6.                                                NC1774.2
141100     MOVE   "ADD-TEST-F2-31-6" TO PAR-NAME.                       NC1774.2
141200     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
141300     ELSE                                                         NC1774.2
141400     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9          NC1774.2
141500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
141600     ADD 1 TO REC-CT.                                             NC1774.2
141700 ADD-TEST-F2-31-7.                                                NC1774.2
141800     MOVE   "ADD-TEST-F2-31-7" TO PAR-NAME.                       NC1774.2
141900     IF      SIZE-ERR2 = "A"                                      NC1774.2
142000             PERFORM PASS                                         NC1774.2
142100             PERFORM PRINT-DETAIL                                 NC1774.2
142200     ELSE                                                         NC1774.2
142300             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1774.2
142400                  TO RE-MARK                                      NC1774.2
142500             MOVE   "A"        TO CORRECT-X                       NC1774.2
142600             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
142700             PERFORM FAIL                                         NC1774.2
142800             PERFORM PRINT-DETAIL.                                NC1774.2
142900*                                                                 NC1774.2
143000 ADD-INIT-F2-32.                                                  NC1774.2
143100*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
143200*    ==-->      SIZE ERROR        <--==                           NC1774.2
143300     MOVE   "VI-67 6.4.2"    TO ANSI-REFERENCE.                   NC1774.2
143400     MOVE    1     TO REC-CT.                                     NC1774.2
143500     MOVE    1.1   TO WRK-DU-1V1-1.                               NC1774.2
143600     MOVE    2.3   TO WRK-DU-1V1-2.                               NC1774.2
143700     MOVE    ZERO  TO WRK-DU-2V0-1.                               NC1774.2
143800     MOVE    ZERO  TO WRK-DU-2V1-1.                               NC1774.2
143900     MOVE    ZERO  TO WRK-DU-2V0-2.                               NC1774.2
144000     MOVE    ZERO  TO WRK-DU-2V1-2.                               NC1774.2
144100     MOVE    ZERO  TO WRK-DU-2V0-3.                               NC1774.2
144200     MOVE    ZERO  TO WRK-DU-2V1-3.                               NC1774.2
144300     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
144400 ADD-GIVING-TEST-F2-32-0.                                         NC1774.2
144500     ADD     A17TWOS-DS-17V00                                     NC1774.2
144600             WRK-DU-1V1-1                                         NC1774.2
144700             6                                                    NC1774.2
144800             WRK-DU-1V1-2                                         NC1774.2
144900      GIVING WRK-DU-2V1-1                                         NC1774.2
145000         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
145100         WRK-DU-2V1-3 WRK-DU-2V0-3                                NC1774.2
145200          ON SIZE ERROR                                           NC1774.2
145300             MOVE "A" TO SIZE-ERR2                                NC1774.2
145400      NOT ON SIZE ERROR                                           NC1774.2
145500             MOVE "B" TO SIZE-ERR2.                               NC1774.2
145600     GO TO   ADD-TEST-F2-32-1.                                    NC1774.2
145700 ADD-DELETE-F2-32.                                                NC1774.2
145800     PERFORM DE-LETE.                                             NC1774.2
145900     PERFORM PRINT-DETAIL.                                        NC1774.2
146000     GO TO   ADD-INIT-F2-33.                                      NC1774.2
146100 ADD-TEST-F2-32-1.                                                NC1774.2
146200     MOVE   "ADD-TEST-F2-32-1" TO PAR-NAME.                       NC1774.2
146300     IF WRK-DU-2V1-1 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
146400     ELSE                                                         NC1774.2
146500     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE ZERO       NC1774.2
146600         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
146700     ADD 1 TO REC-CT.                                             NC1774.2
146800 ADD-TEST-F2-32-2.                                                NC1774.2
146900     MOVE   "ADD-TEST-F2-32-2" TO PAR-NAME.                       NC1774.2
147000     IF WRK-DU-2V0-1 = ZERO  PERFORM PASS PERFORM PRINT-DETAIL    NC1774.2
147100     ELSE                                                         NC1774.2
147200     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE ZERO       NC1774.2
147300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
147400     ADD 1 TO REC-CT.                                             NC1774.2
147500 ADD-ADD-TEST-F2-32-3.                                            NC1774.2
147600     MOVE   "ADD-TEST-F2-32-3" TO PAR-NAME.                       NC1774.2
147700     IF WRK-DU-2V1-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
147800     ELSE                                                         NC1774.2
147900     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE ZERO       NC1774.2
148000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
148100     ADD 1 TO REC-CT.                                             NC1774.2
148200 ADD-TEST-F2-32-4.                                                NC1774.2
148300     MOVE   "ADD-TEST-F2-32-4" TO PAR-NAME.                       NC1774.2
148400     IF WRK-DU-2V0-2 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
148500     ELSE                                                         NC1774.2
148600     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE ZERO       NC1774.2
148700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
148800     ADD 1 TO REC-CT.                                             NC1774.2
148900 ADD-TEST-F2-32-5.                                                NC1774.2
149000     MOVE   "ADD-TEST-F2-32-5" TO PAR-NAME.                       NC1774.2
149100     IF WRK-DU-2V1-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
149200     ELSE                                                         NC1774.2
149300     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE ZERO       NC1774.2
149400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
149500     ADD 1 TO REC-CT.                                             NC1774.2
149600 ADD-TEST-F2-32-6.                                                NC1774.2
149700     MOVE   "ADD-TEST-F2-32-6" TO PAR-NAME.                       NC1774.2
149800     IF WRK-DU-2V0-3 = ZERO PERFORM PASS PERFORM PRINT-DETAIL     NC1774.2
149900     ELSE                                                         NC1774.2
150000     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE ZERO       NC1774.2
150100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
150200     ADD 1 TO REC-CT.                                             NC1774.2
150300 ADD-TEST-F2-32-7.                                                NC1774.2
150400     MOVE   "ADD-TEST-F2-32-7" TO PAR-NAME.                       NC1774.2
150500     IF      SIZE-ERR2 = "A"                                      NC1774.2
150600             PERFORM PASS                                         NC1774.2
150700             PERFORM PRINT-DETAIL                                 NC1774.2
150800     ELSE                                                         NC1774.2
150900             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1774.2
151000                  TO RE-MARK                                      NC1774.2
151100             MOVE   "A"        TO CORRECT-X                       NC1774.2
151200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
151300             PERFORM FAIL                                         NC1774.2
151400             PERFORM PRINT-DETAIL.                                NC1774.2
151500*                                                                 NC1774.2
151600 ADD-INIT-F2-33.                                                  NC1774.2
151700*    ==-->  NEW SIZE ERROR TESTS  <--==                           NC1774.2
151800*    ==-->   NO SIZE ERROR        <--==                           NC1774.2
151900     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1774.2
152000     MOVE    1     TO REC-CT.                                     NC1774.2
152100     MOVE    1.1   TO WRK-DU-1V1-1.                               NC1774.2
152200     MOVE    2.3   TO WRK-DU-1V1-2.                               NC1774.2
152300     MOVE    ZERO  TO WRK-DU-2V0-1.                               NC1774.2
152400     MOVE    ZERO  TO WRK-DU-2V1-1.                               NC1774.2
152500     MOVE    ZERO  TO WRK-DU-2V0-2.                               NC1774.2
152600     MOVE    ZERO  TO WRK-DU-2V1-2.                               NC1774.2
152700     MOVE    ZERO  TO WRK-DU-2V0-3.                               NC1774.2
152800     MOVE    ZERO  TO WRK-DU-2V1-3.                               NC1774.2
152900     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
153000 ADD-GIVING-TEST-F2-33-0.                                         NC1774.2
153100     ADD WRK-DU-1V1-1 6 WRK-DU-1V1-2 GIVING WRK-DU-2V1-1          NC1774.2
153200         WRK-DU-2V0-1 ROUNDED WRK-DU-2V1-2 WRK-DU-2V0-2 ROUNDED   NC1774.2
153300         WRK-DU-2V1-3 WRK-DU-2V0-3                                NC1774.2
153400          ON SIZE ERROR                                           NC1774.2
153500             MOVE "A" TO SIZE-ERR2                                NC1774.2
153600      NOT ON SIZE ERROR                                           NC1774.2
153700             MOVE "B" TO SIZE-ERR2.                               NC1774.2
153800     GO TO   ADD-TEST-F2-33-1.                                    NC1774.2
153900 ADD-DELETE-F2-33.                                                NC1774.2
154000     PERFORM DE-LETE.                                             NC1774.2
154100     PERFORM PRINT-DETAIL.                                        NC1774.2
154200     GO TO   ADD-INIT-F2-34.                                      NC1774.2
154300 ADD-TEST-F2-33-1.                                                NC1774.2
154400     MOVE   "ADD-TEST-F2-33-1" TO PAR-NAME.                       NC1774.2
154500     IF WRK-DU-2V1-1 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
154600     ELSE                                                         NC1774.2
154700     PERFORM FAIL MOVE WRK-DU-2V1-1 TO COMPUTED-N MOVE 9.4        NC1774.2
154800         TO CORRECT-N PERFORM PRINT-DETAIL.                       NC1774.2
154900     ADD 1 TO REC-CT.                                             NC1774.2
155000 ADD-TEST-F2-33-2.                                                NC1774.2
155100     MOVE   "ADD-TEST-F2-33-2" TO PAR-NAME.                       NC1774.2
155200     IF WRK-DU-2V0-1 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
155300     ELSE                                                         NC1774.2
155400     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 9          NC1774.2
155500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
155600     ADD 1 TO REC-CT.                                             NC1774.2
155700 ADD-ADD-TEST-F2-33-3.                                            NC1774.2
155800     MOVE   "ADD-TEST-F2-33-3" TO PAR-NAME.                       NC1774.2
155900     IF WRK-DU-2V1-2 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
156000     ELSE                                                         NC1774.2
156100     PERFORM FAIL MOVE WRK-DU-2V1-2 TO COMPUTED-N MOVE 9.4        NC1774.2
156200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
156300     ADD 1 TO REC-CT.                                             NC1774.2
156400 ADD-TEST-F2-33-4.                                                NC1774.2
156500     MOVE   "ADD-TEST-F2-33-4" TO PAR-NAME.                       NC1774.2
156600     IF WRK-DU-2V0-2 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
156700     ELSE                                                         NC1774.2
156800     PERFORM FAIL MOVE WRK-DU-2V0-2 TO COMPUTED-N MOVE 9          NC1774.2
156900     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
157000     ADD 1 TO REC-CT.                                             NC1774.2
157100 ADD-TEST-F2-33-5.                                                NC1774.2
157200     MOVE   "ADD-TEST-F2-33-5" TO PAR-NAME.                       NC1774.2
157300     IF WRK-DU-2V1-3 = 9.4 PERFORM PASS PERFORM PRINT-DETAIL      NC1774.2
157400     ELSE                                                         NC1774.2
157500     PERFORM FAIL MOVE WRK-DU-2V1-3 TO COMPUTED-N MOVE 9.4        NC1774.2
157600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
157700     ADD 1 TO REC-CT.                                             NC1774.2
157800 ADD-TEST-F2-33-6.                                                NC1774.2
157900     MOVE   "ADD-TEST-F2-33-6" TO PAR-NAME.                       NC1774.2
158000     IF WRK-DU-2V0-3 = 9 PERFORM PASS PERFORM PRINT-DETAIL        NC1774.2
158100     ELSE                                                         NC1774.2
158200     PERFORM FAIL MOVE WRK-DU-2V0-3 TO COMPUTED-N MOVE 9          NC1774.2
158300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1774.2
158400     ADD 1 TO REC-CT.                                             NC1774.2
158500 ADD-TEST-F2-33-7.                                                NC1774.2
158600     MOVE   "ADD-TEST-F2-33-7" TO PAR-NAME.                       NC1774.2
158700     IF      SIZE-ERR2 = "B"                                      NC1774.2
158800             PERFORM PASS                                         NC1774.2
158900             PERFORM PRINT-DETAIL                                 NC1774.2
159000     ELSE                                                         NC1774.2
159100             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1774.2
159200                  TO RE-MARK                                      NC1774.2
159300             MOVE   "B"        TO CORRECT-X                       NC1774.2
159400             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
159500             PERFORM FAIL                                         NC1774.2
159600             PERFORM PRINT-DETAIL.                                NC1774.2
159700*                                                                 NC1774.2
159800 ADD-INIT-F2-34.                                                  NC1774.2
159900*    ==-->EXPLICIT SCOPE TERMINATOR<--==                          NC1774.2
160000*    ==-->      SIZE ERROR        <--==                           NC1774.2
160100     MOVE   "VI-67 6.4.3" TO ANSI-REFERENCE.                      NC1774.2
160200     MOVE   "ADD-TEST-F2-34" TO PAR-NAME.                         NC1774.2
160300     MOVE    ZERO  TO WRK-DS-10V00.                               NC1774.2
160400     MOVE    1     TO REC-CT.                                     NC1774.2
160500     MOVE    SPACE TO WRK-XN-00001.                               NC1774.2
160600     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
160700     MOVE    SPACE TO SIZE-ERR3.                                  NC1774.2
160800     MOVE    SPACE TO SIZE-ERR4.                                  NC1774.2
160900 ADD-TEST-F2-34-0.                                                NC1774.2
161000     ADD     A12ONES-DS-12V00                                     NC1774.2
161100             ZERO                                                 NC1774.2
161200      GIVING WRK-DS-10V00                                         NC1774.2
161300          ON SIZE ERROR                                           NC1774.2
161400             MOVE "A" TO SIZE-ERR2                                NC1774.2
161500             MOVE "B" TO SIZE-ERR3                                NC1774.2
161600             MOVE "C" TO SIZE-ERR4                                NC1774.2
161700     END-ADD                                                      NC1774.2
161800     MOVE   "1" TO WRK-XN-00001.                                  NC1774.2
161900     GO TO   ADD-TEST-F2-34-1.                                    NC1774.2
162000 ADD-DELETE-F2-34.                                                NC1774.2
162100     PERFORM DE-LETE.                                             NC1774.2
162200     PERFORM PRINT-DETAIL.                                        NC1774.2
162300     GO TO   ADD-INIT-F2-35.                                      NC1774.2
162400 ADD-TEST-F2-34-1.                                                NC1774.2
162500     MOVE   "ADD-TEST-F2-34-1" TO PAR-NAME.                       NC1774.2
162600     IF      SIZE-ERR2 = "A"                                      NC1774.2
162700             PERFORM PASS                                         NC1774.2
162800             PERFORM PRINT-DETAIL                                 NC1774.2
162900     ELSE                                                         NC1774.2
163000             MOVE   "A"        TO CORRECT-X                       NC1774.2
163100             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
163200             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1774.2
163300                  TO RE-MARK                                      NC1774.2
163400             PERFORM FAIL                                         NC1774.2
163500             PERFORM PRINT-DETAIL.                                NC1774.2
163600     ADD     1 TO REC-CT.                                         NC1774.2
163700 ADD-TEST-F2-34-2.                                                NC1774.2
163800     MOVE   "ADD-TEST-F2-34-2" TO PAR-NAME.                       NC1774.2
163900     IF      SIZE-ERR3 = "B"                                      NC1774.2
164000             PERFORM PASS                                         NC1774.2
164100             PERFORM PRINT-DETAIL                                 NC1774.2
164200     ELSE                                                         NC1774.2
164300             MOVE   "B"        TO CORRECT-X                       NC1774.2
164400             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1774.2
164500             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1774.2
164600                  TO RE-MARK                                      NC1774.2
164700             PERFORM FAIL                                         NC1774.2
164800             PERFORM PRINT-DETAIL.                                NC1774.2
164900     ADD     1 TO REC-CT.                                         NC1774.2
165000 ADD-TEST-F2-34-3.                                                NC1774.2
165100     MOVE   "ADD-TEST-F2-34-3" TO PAR-NAME.                       NC1774.2
165200     IF      SIZE-ERR4 = "C"                                      NC1774.2
165300             PERFORM PASS                                         NC1774.2
165400             PERFORM PRINT-DETAIL                                 NC1774.2
165500     ELSE                                                         NC1774.2
165600             MOVE   "C"        TO CORRECT-X                       NC1774.2
165700             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1774.2
165800             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1774.2
165900                  TO RE-MARK                                      NC1774.2
166000             PERFORM FAIL                                         NC1774.2
166100             PERFORM PRINT-DETAIL.                                NC1774.2
166200     ADD     1 TO REC-CT.                                         NC1774.2
166300 ADD-TEST-F2-34-4.                                                NC1774.2
166400     MOVE   "ADD-TEST-F2-34-4" TO PAR-NAME.                       NC1774.2
166500     IF      WRK-XN-00001 = "1"                                   NC1774.2
166600             PERFORM PASS                                         NC1774.2
166700             PERFORM PRINT-DETAIL                                 NC1774.2
166800     ELSE                                                         NC1774.2
166900             MOVE   "1"           TO CORRECT-X                    NC1774.2
167000             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1774.2
167100             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1774.2
167200             PERFORM FAIL                                         NC1774.2
167300             PERFORM PRINT-DETAIL.                                NC1774.2
167400     ADD     1 TO REC-CT.                                         NC1774.2
167500 ADD-TEST-F2-34-5.                                                NC1774.2
167600     MOVE   "ADD-TEST-F2-34-5" TO PAR-NAME.                       NC1774.2
167700     IF      WRK-DS-10V00  = ZERO                                 NC1774.2
167800             PERFORM PASS                                         NC1774.2
167900             PERFORM PRINT-DETAIL                                 NC1774.2
168000     ELSE                                                         NC1774.2
168100             MOVE    ZERO         TO CORRECT-N                    NC1774.2
168200             MOVE    WRK-DS-10V00 TO COMPUTED-N                   NC1774.2
168300             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1774.2
168400                  TO RE-MARK                                      NC1774.2
168500             PERFORM FAIL                                         NC1774.2
168600             PERFORM PRINT-DETAIL.                                NC1774.2
168700*                                                                 NC1774.2
168800 ADD-INIT-F2-35.                                                  NC1774.2
168900*    ==-->EXPLICIT SCOPE TERMINATOR<--==                          NC1774.2
169000*    ==-->   NO SIZE ERROR        <--==                           NC1774.2
169100     MOVE   "VI-67 6.4.3" TO ANSI-REFERENCE.                      NC1774.2
169200     MOVE   "ADD-TEST-F2-35" TO PAR-NAME.                         NC1774.2
169300     MOVE    1     TO REC-CT.                                     NC1774.2
169400     MOVE    SPACE TO WRK-XN-00001.                               NC1774.2
169500     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
169600     MOVE    SPACE TO SIZE-ERR3.                                  NC1774.2
169700     MOVE    SPACE TO SIZE-ERR4.                                  NC1774.2
169800     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
169900 ADD-TEST-F2-35-0.                                                NC1774.2
170000     ADD     A12THREES-DS-06V06                                   NC1774.2
170100             333333                                               NC1774.2
170200             A06THREES-DS-03V03                                   NC1774.2
170300      GIVING WRK-DS-06V06 ROUNDED                                 NC1774.2
170400          ON SIZE ERROR                                           NC1774.2
170500             MOVE "A" TO SIZE-ERR2                                NC1774.2
170600             MOVE "B" TO SIZE-ERR3                                NC1774.2
170700             MOVE "C" TO SIZE-ERR4                                NC1774.2
170800     END-ADD                                                      NC1774.2
170900     MOVE   "1" TO WRK-XN-00001.                                  NC1774.2
171000     GO TO   ADD-TEST-F2-35-1.                                    NC1774.2
171100 ADD-DELETE-F2-35.                                                NC1774.2
171200     PERFORM DE-LETE.                                             NC1774.2
171300     PERFORM PRINT-DETAIL.                                        NC1774.2
171400     GO TO   ADD-INIT-F2-36.                                      NC1774.2
171500 ADD-TEST-F2-35-1.                                                NC1774.2
171600     MOVE   "ADD-TEST-F2-35-1" TO PAR-NAME.                       NC1774.2
171700     IF      SIZE-ERR2 = SPACE                                    NC1774.2
171800             PERFORM PASS                                         NC1774.2
171900             PERFORM PRINT-DETAIL                                 NC1774.2
172000     ELSE                                                         NC1774.2
172100             MOVE    SPACE     TO CORRECT-X                       NC1774.2
172200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
172300             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1774.2
172400                  TO RE-MARK                                      NC1774.2
172500             PERFORM FAIL                                         NC1774.2
172600             PERFORM PRINT-DETAIL.                                NC1774.2
172700     ADD     1 TO REC-CT.                                         NC1774.2
172800 ADD-TEST-F2-35-2.                                                NC1774.2
172900     MOVE   "ADD-TEST-F2-35-2" TO PAR-NAME.                       NC1774.2
173000     IF      SIZE-ERR3 = SPACE                                    NC1774.2
173100             PERFORM PASS                                         NC1774.2
173200             PERFORM PRINT-DETAIL                                 NC1774.2
173300     ELSE                                                         NC1774.2
173400             MOVE    SPACE     TO CORRECT-X                       NC1774.2
173500             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1774.2
173600             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1774.2
173700                  TO RE-MARK                                      NC1774.2
173800             PERFORM FAIL                                         NC1774.2
173900             PERFORM PRINT-DETAIL.                                NC1774.2
174000     ADD     1 TO REC-CT.                                         NC1774.2
174100 ADD-TEST-F2-35-3.                                                NC1774.2
174200     MOVE   "ADD-TEST-F2-35-3" TO PAR-NAME.                       NC1774.2
174300     IF      SIZE-ERR4 = SPACE                                    NC1774.2
174400             PERFORM PASS                                         NC1774.2
174500             PERFORM PRINT-DETAIL                                 NC1774.2
174600     ELSE                                                         NC1774.2
174700             MOVE   "C"        TO CORRECT-X                       NC1774.2
174800             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1774.2
174900             MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"        NC1774.2
175000                  TO RE-MARK                                      NC1774.2
175100             PERFORM FAIL                                         NC1774.2
175200             PERFORM PRINT-DETAIL.                                NC1774.2
175300     ADD     1 TO REC-CT.                                         NC1774.2
175400 ADD-TEST-F2-35-4.                                                NC1774.2
175500     MOVE   "ADD-TEST-F2-35-4" TO PAR-NAME.                       NC1774.2
175600     IF      WRK-XN-00001 = "1"                                   NC1774.2
175700             PERFORM PASS                                         NC1774.2
175800             PERFORM PRINT-DETAIL                                 NC1774.2
175900     ELSE                                                         NC1774.2
176000             MOVE   "1"           TO CORRECT-X                    NC1774.2
176100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1774.2
176200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1774.2
176300             PERFORM FAIL                                         NC1774.2
176400             PERFORM PRINT-DETAIL                                 NC1774.2
176500     ADD     1 TO REC-CT.                                         NC1774.2
176600 ADD-TEST-F2-35-5.                                                NC1774.2
176700     MOVE   "ADD-TEST-F2-35-5" TO PAR-NAME.                       NC1774.2
176800     IF      WRK-DS-06V06 = 666999.666333                         NC1774.2
176900             PERFORM PASS                                         NC1774.2
177000             PERFORM PRINT-DETAIL                                 NC1774.2
177100     ELSE                                                         NC1774.2
177200             MOVE    666999.666333 TO CORRECT-N                   NC1774.2
177300             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1774.2
177400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1774.2
177500                  TO RE-MARK                                      NC1774.2
177600             PERFORM FAIL                                         NC1774.2
177700             PERFORM PRINT-DETAIL.                                NC1774.2
177800*                                                                 NC1774.2
177900 ADD-INIT-F2-36.                                                  NC1774.2
178000*    ==-->EXPLICIT SCOPE TERMINATOR<--==                          NC1774.2
178100*    ==-->      SIZE ERROR        <--==                           NC1774.2
178200     MOVE   "VI-67 6.4.3" TO ANSI-REFERENCE.                      NC1774.2
178300     MOVE   "ADD-TEST-F2-36" TO PAR-NAME.                         NC1774.2
178400     MOVE    ZERO  TO WRK-DS-10V00.                               NC1774.2
178500     MOVE    1     TO REC-CT.                                     NC1774.2
178600     MOVE    SPACE TO WRK-XN-00001.                               NC1774.2
178700     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
178800     MOVE    SPACE TO SIZE-ERR3.                                  NC1774.2
178900     MOVE    SPACE TO SIZE-ERR4.                                  NC1774.2
179000 ADD-TEST-F2-36-0.                                                NC1774.2
179100     ADD     A12ONES-DS-12V00                                     NC1774.2
179200             ZERO                                                 NC1774.2
179300      GIVING WRK-DS-10V00                                         NC1774.2
179400      NOT ON SIZE ERROR                                           NC1774.2
179500             MOVE "A" TO SIZE-ERR2                                NC1774.2
179600             MOVE "B" TO SIZE-ERR3                                NC1774.2
179700             MOVE "C" TO SIZE-ERR4                                NC1774.2
179800     END-ADD                                                      NC1774.2
179900     MOVE   "1" TO WRK-XN-00001.                                  NC1774.2
180000     GO TO   ADD-TEST-F2-36-1.                                    NC1774.2
180100 ADD-DELETE-F2-36.                                                NC1774.2
180200     PERFORM DE-LETE.                                             NC1774.2
180300     PERFORM PRINT-DETAIL.                                        NC1774.2
180400     GO TO   ADD-INIT-F2-37.                                      NC1774.2
180500 ADD-TEST-F2-36-1.                                                NC1774.2
180600     MOVE   "ADD-TEST-F2-36-1" TO PAR-NAME.                       NC1774.2
180700     IF      SIZE-ERR2 = SPACE                                    NC1774.2
180800             PERFORM PASS                                         NC1774.2
180900             PERFORM PRINT-DETAIL                                 NC1774.2
181000     ELSE                                                         NC1774.2
181100             MOVE    SPACE     TO CORRECT-X                       NC1774.2
181200             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
181300             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1774.2
181400                  TO RE-MARK                                      NC1774.2
181500             PERFORM FAIL                                         NC1774.2
181600             PERFORM PRINT-DETAIL.                                NC1774.2
181700     ADD     1 TO REC-CT.                                         NC1774.2
181800 ADD-TEST-F2-36-2.                                                NC1774.2
181900     MOVE   "ADD-TEST-F2-36-2" TO PAR-NAME.                       NC1774.2
182000     IF      SIZE-ERR3 = SPACE                                    NC1774.2
182100             PERFORM PASS                                         NC1774.2
182200             PERFORM PRINT-DETAIL                                 NC1774.2
182300     ELSE                                                         NC1774.2
182400             MOVE    SPACE     TO CORRECT-X                       NC1774.2
182500             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1774.2
182600             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1774.2
182700                  TO RE-MARK                                      NC1774.2
182800             PERFORM FAIL                                         NC1774.2
182900             PERFORM PRINT-DETAIL.                                NC1774.2
183000     ADD     1 TO REC-CT.                                         NC1774.2
183100 ADD-TEST-F2-36-3.                                                NC1774.2
183200     MOVE   "ADD-TEST-F2-36-3" TO PAR-NAME.                       NC1774.2
183300     IF      SIZE-ERR4 = SPACE                                    NC1774.2
183400             PERFORM PASS                                         NC1774.2
183500             PERFORM PRINT-DETAIL                                 NC1774.2
183600     ELSE                                                         NC1774.2
183700             MOVE    SPACE     TO CORRECT-X                       NC1774.2
183800             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1774.2
183900             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1774.2
184000                  TO RE-MARK                                      NC1774.2
184100             PERFORM FAIL                                         NC1774.2
184200             PERFORM PRINT-DETAIL.                                NC1774.2
184300     ADD     1 TO REC-CT.                                         NC1774.2
184400 ADD-TEST-F2-36-4.                                                NC1774.2
184500     MOVE   "ADD-TEST-F2-36-4" TO PAR-NAME.                       NC1774.2
184600     IF      WRK-XN-00001 = "1"                                   NC1774.2
184700             PERFORM PASS                                         NC1774.2
184800             PERFORM PRINT-DETAIL                                 NC1774.2
184900     ELSE                                                         NC1774.2
185000             MOVE   "1"           TO CORRECT-X                    NC1774.2
185100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1774.2
185200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1774.2
185300             PERFORM FAIL                                         NC1774.2
185400             PERFORM PRINT-DETAIL.                                NC1774.2
185500     ADD     1 TO REC-CT.                                         NC1774.2
185600 ADD-TEST-F2-36-5.                                                NC1774.2
185700     MOVE   "ADD-TEST-F2-36-5" TO PAR-NAME.                       NC1774.2
185800     IF      WRK-DS-10V00 = ZERO                                  NC1774.2
185900             PERFORM PASS                                         NC1774.2
186000             PERFORM PRINT-DETAIL                                 NC1774.2
186100     ELSE                                                         NC1774.2
186200             MOVE    ZERO         TO CORRECT-N                    NC1774.2
186300             MOVE    WRK-DS-10V00 TO COMPUTED-N                   NC1774.2
186400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1774.2
186500                  TO RE-MARK                                      NC1774.2
186600             PERFORM FAIL                                         NC1774.2
186700             PERFORM PRINT-DETAIL.                                NC1774.2
186800*                                                                 NC1774.2
186900 ADD-INIT-F2-37.                                                  NC1774.2
187000*    ==-->EXPLICIT SCOPE TERMINATOR<--==                          NC1774.2
187100*    ==-->   NO SIZE ERROR        <--==                           NC1774.2
187200     MOVE   "VI-67 6.4.3" TO ANSI-REFERENCE.                      NC1774.2
187300     MOVE   "ADD-TEST-F2-37" TO PAR-NAME.                         NC1774.2
187400     MOVE    1     TO REC-CT.                                     NC1774.2
187500     MOVE    SPACE TO WRK-XN-00001.                               NC1774.2
187600     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
187700     MOVE    SPACE TO SIZE-ERR3.                                  NC1774.2
187800     MOVE    SPACE TO SIZE-ERR4.                                  NC1774.2
187900     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
188000 ADD-TEST-F2-37-0.                                                NC1774.2
188100     ADD     A12THREES-DS-06V06                                   NC1774.2
188200             333333                                               NC1774.2
188300             A06THREES-DS-03V03                                   NC1774.2
188400      GIVING WRK-DS-06V06 ROUNDED                                 NC1774.2
188500      NOT ON SIZE ERROR                                           NC1774.2
188600             MOVE "A" TO SIZE-ERR2                                NC1774.2
188700             MOVE "B" TO SIZE-ERR3                                NC1774.2
188800             MOVE "C" TO SIZE-ERR4                                NC1774.2
188900     END-ADD                                                      NC1774.2
189000     MOVE   "1" TO WRK-XN-00001.                                  NC1774.2
189100     GO TO   ADD-TEST-F2-37-1.                                    NC1774.2
189200 ADD-DELETE-F2-37.                                                NC1774.2
189300     PERFORM DE-LETE.                                             NC1774.2
189400     PERFORM PRINT-DETAIL.                                        NC1774.2
189500     GO TO   ADD-INIT-F2-38.                                      NC1774.2
189600 ADD-TEST-F2-37-1.                                                NC1774.2
189700     MOVE   "ADD-TEST-F2-37-1" TO PAR-NAME.                       NC1774.2
189800     IF      SIZE-ERR2 = "A"                                      NC1774.2
189900             PERFORM PASS                                         NC1774.2
190000             PERFORM PRINT-DETAIL                                 NC1774.2
190100     ELSE                                                         NC1774.2
190200             MOVE   "A"        TO CORRECT-X                       NC1774.2
190300             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
190400             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1774.2
190500                  TO RE-MARK                                      NC1774.2
190600             PERFORM FAIL                                         NC1774.2
190700             PERFORM PRINT-DETAIL.                                NC1774.2
190800     ADD     1 TO REC-CT.                                         NC1774.2
190900 ADD-TEST-F2-37-2.                                                NC1774.2
191000     MOVE   "ADD-TEST-F2-37-2" TO PAR-NAME.                       NC1774.2
191100     IF      SIZE-ERR3 = "B"                                      NC1774.2
191200             PERFORM PASS                                         NC1774.2
191300             PERFORM PRINT-DETAIL                                 NC1774.2
191400     ELSE                                                         NC1774.2
191500             MOVE   "B"        TO CORRECT-X                       NC1774.2
191600             MOVE    SIZE-ERR3 TO COMPUTED-X                      NC1774.2
191700             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1774.2
191800                  TO RE-MARK                                      NC1774.2
191900             PERFORM FAIL                                         NC1774.2
192000             PERFORM PRINT-DETAIL.                                NC1774.2
192100     ADD     1 TO REC-CT.                                         NC1774.2
192200 ADD-TEST-F2-37-3.                                                NC1774.2
192300     MOVE   "ADD-TEST-F2-37-3" TO PAR-NAME                        NC1774.2
192400     IF      SIZE-ERR4 = "C"                                      NC1774.2
192500             PERFORM PASS                                         NC1774.2
192600             PERFORM PRINT-DETAIL                                 NC1774.2
192700     ELSE                                                         NC1774.2
192800             MOVE   "C"        TO CORRECT-X                       NC1774.2
192900             MOVE    SIZE-ERR4 TO COMPUTED-X                      NC1774.2
193000             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1774.2
193100                  TO RE-MARK                                      NC1774.2
193200             PERFORM FAIL                                         NC1774.2
193300             PERFORM PRINT-DETAIL.                                NC1774.2
193400     ADD     1 TO REC-CT.                                         NC1774.2
193500 ADD-TEST-F2-37-4.                                                NC1774.2
193600     MOVE   "ADD-TEST-F2-37-4" TO PAR-NAME.                       NC1774.2
193700     IF      WRK-XN-00001 = "1"                                   NC1774.2
193800             PERFORM PASS                                         NC1774.2
193900             PERFORM PRINT-DETAIL                                 NC1774.2
194000     ELSE                                                         NC1774.2
194100             MOVE   "1"           TO CORRECT-X                    NC1774.2
194200             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1774.2
194300             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1774.2
194400             PERFORM FAIL                                         NC1774.2
194500             PERFORM PRINT-DETAIL.                                NC1774.2
194600     ADD     1 TO REC-CT.                                         NC1774.2
194700 ADD-TEST-F2-37-5.                                                NC1774.2
194800     MOVE   "ADD-TEST-F2-37-5" TO PAR-NAME.                       NC1774.2
194900     IF      WRK-DS-06V06 = 666999.666333                         NC1774.2
195000             PERFORM PASS                                         NC1774.2
195100             PERFORM PRINT-DETAIL                                 NC1774.2
195200     ELSE                                                         NC1774.2
195300             MOVE    666999.666333 TO CORRECT-N                   NC1774.2
195400             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1774.2
195500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1774.2
195600                  TO RE-MARK                                      NC1774.2
195700             PERFORM FAIL                                         NC1774.2
195800             PERFORM PRINT-DETAIL.                                NC1774.2
195900*                                                                 NC1774.2
196000 ADD-INIT-F2-38.                                                  NC1774.2
196100*    ==-->EXPLICIT SCOPE TERMINATOR<--==                          NC1774.2
196200*    ==-->      SIZE ERROR        <--==                           NC1774.2
196300     MOVE   "VI-67 6.4.3" TO ANSI-REFERENCE.                      NC1774.2
196400     MOVE   "ADD-TEST-F2-38" TO PAR-NAME.                         NC1774.2
196500     MOVE    ZERO  TO WRK-DS-10V00.                               NC1774.2
196600     MOVE    1     TO REC-CT.                                     NC1774.2
196700     MOVE    SPACE TO WRK-XN-00001.                               NC1774.2
196800     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
196900     MOVE    SPACE TO SIZE-ERR3.                                  NC1774.2
197000     MOVE    SPACE TO SIZE-ERR4.                                  NC1774.2
197100 ADD-TEST-F2-38-0.                                                NC1774.2
197200     ADD     A12ONES-DS-12V00                                     NC1774.2
197300             ZERO                                                 NC1774.2
197400      GIVING WRK-DS-10V00                                         NC1774.2
197500          ON SIZE ERROR                                           NC1774.2
197600             MOVE "A" TO SIZE-ERR2                                NC1774.2
197700      NOT ON SIZE ERROR                                           NC1774.2
197800             MOVE "X" TO SIZE-ERR2                                NC1774.2
197900     END-ADD                                                      NC1774.2
198000     MOVE   "1" TO WRK-XN-00001.                                  NC1774.2
198100     GO TO   ADD-TEST-F2-38-1.                                    NC1774.2
198200 ADD-DELETE-F2-38.                                                NC1774.2
198300     PERFORM DE-LETE.                                             NC1774.2
198400     PERFORM PRINT-DETAIL.                                        NC1774.2
198500     GO TO   ADD-INIT-F2-39.                                      NC1774.2
198600 ADD-TEST-F2-38-1.                                                NC1774.2
198700     MOVE   "ADD-TEST-F2-38-1" TO PAR-NAME.                       NC1774.2
198800     IF      SIZE-ERR2 = "A"                                      NC1774.2
198900             PERFORM PASS                                         NC1774.2
199000             PERFORM PRINT-DETAIL                                 NC1774.2
199100     ELSE                                                         NC1774.2
199200             MOVE   "A"        TO CORRECT-X                       NC1774.2
199300             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
199400             MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"            NC1774.2
199500                  TO RE-MARK                                      NC1774.2
199600             PERFORM FAIL                                         NC1774.2
199700             PERFORM PRINT-DETAIL.                                NC1774.2
199800     ADD     1 TO REC-CT.                                         NC1774.2
199900 ADD-TEST-F2-38-2.                                                NC1774.2
200000     MOVE   "ADD-TEST-F2-38-2" TO PAR-NAME.                       NC1774.2
200100     IF      WRK-XN-00001 = "1"                                   NC1774.2
200200             PERFORM PASS                                         NC1774.2
200300             PERFORM PRINT-DETAIL                                 NC1774.2
200400     ELSE                                                         NC1774.2
200500             MOVE   "1"           TO CORRECT-X                    NC1774.2
200600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1774.2
200700             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1774.2
200800             PERFORM FAIL                                         NC1774.2
200900             PERFORM PRINT-DETAIL.                                NC1774.2
201000     ADD     1 TO REC-CT.                                         NC1774.2
201100 ADD-TEST-F2-38-3.                                                NC1774.2
201200     MOVE   "ADD-TEST-F2-38-3" TO PAR-NAME.                       NC1774.2
201300     IF      WRK-DS-10V00 = ZERO                                  NC1774.2
201400             PERFORM PASS                                         NC1774.2
201500             PERFORM PRINT-DETAIL                                 NC1774.2
201600     ELSE                                                         NC1774.2
201700             MOVE    ZERO         TO CORRECT-N                    NC1774.2
201800             MOVE    WRK-DS-10V00 TO COMPUTED-N                   NC1774.2
201900             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1774.2
202000                  TO RE-MARK                                      NC1774.2
202100             PERFORM FAIL                                         NC1774.2
202200             PERFORM PRINT-DETAIL.                                NC1774.2
202300*                                                                 NC1774.2
202400 ADD-INIT-F2-39.                                                  NC1774.2
202500*    ==-->EXPLICIT SCOPE TERMINATOR<--==                          NC1774.2
202600*    ==-->    NO SIZE ERROR        <--==                          NC1774.2
202700     MOVE   "VI-67 6.4.3" TO ANSI-REFERENCE.                      NC1774.2
202800     MOVE   "ADD-TEST-F2-39" TO PAR-NAME.                         NC1774.2
202900     MOVE    1     TO REC-CT.                                     NC1774.2
203000     MOVE    SPACE TO WRK-XN-00001.                               NC1774.2
203100     MOVE    SPACE TO SIZE-ERR2.                                  NC1774.2
203200     MOVE    SPACE TO SIZE-ERR3.                                  NC1774.2
203300     MOVE    SPACE TO SIZE-ERR4.                                  NC1774.2
203400     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
203500 ADD-TEST-F2-39-0.                                                NC1774.2
203600     ADD     A12THREES-DS-06V06                                   NC1774.2
203700             333333                                               NC1774.2
203800             A06THREES-DS-03V03                                   NC1774.2
203900      GIVING WRK-DS-06V06 ROUNDED                                 NC1774.2
204000          ON SIZE ERROR                                           NC1774.2
204100             MOVE "1" TO SIZE-ERR2                                NC1774.2
204200      NOT ON SIZE ERROR                                           NC1774.2
204300             MOVE "A" TO SIZE-ERR2                                NC1774.2
204400     END-ADD                                                      NC1774.2
204500     MOVE   "1" TO WRK-XN-00001.                                  NC1774.2
204600     GO TO   ADD-TEST-F2-39-1.                                    NC1774.2
204700 ADD-DELETE-F2-39.                                                NC1774.2
204800     PERFORM DE-LETE.                                             NC1774.2
204900     PERFORM PRINT-DETAIL.                                        NC1774.2
205000     GO TO   ADD-INIT-F2-40.                                      NC1774.2
205100 ADD-TEST-F2-39-1.                                                NC1774.2
205200     MOVE   "ADD-TEST-F2-39-1" TO PAR-NAME.                       NC1774.2
205300     IF      SIZE-ERR2 = "A"                                      NC1774.2
205400             PERFORM PASS                                         NC1774.2
205500             PERFORM PRINT-DETAIL                                 NC1774.2
205600     ELSE                                                         NC1774.2
205700             MOVE   "A"        TO CORRECT-X                       NC1774.2
205800             MOVE    SIZE-ERR2 TO COMPUTED-X                      NC1774.2
205900             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1774.2
206000                  TO RE-MARK                                      NC1774.2
206100             PERFORM FAIL                                         NC1774.2
206200             PERFORM PRINT-DETAIL.                                NC1774.2
206300     ADD     1 TO REC-CT.                                         NC1774.2
206400 ADD-TEST-F2-39-2.                                                NC1774.2
206500     MOVE   "ADD-TEST-F2-39-2" TO PAR-NAME.                       NC1774.2
206600     IF      WRK-XN-00001 = "1"                                   NC1774.2
206700             PERFORM PASS                                         NC1774.2
206800             PERFORM PRINT-DETAIL                                 NC1774.2
206900     ELSE                                                         NC1774.2
207000             MOVE   "1"           TO CORRECT-X                    NC1774.2
207100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1774.2
207200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1774.2
207300             PERFORM FAIL                                         NC1774.2
207400             PERFORM PRINT-DETAIL.                                NC1774.2
207500     ADD     1 TO REC-CT.                                         NC1774.2
207600 ADD-TEST-F2-39-3.                                                NC1774.2
207700     MOVE   "ADD-TEST-F2-39-3" TO PAR-NAME.                       NC1774.2
207800     IF      WRK-DS-06V06 = 666999.666333                         NC1774.2
207900             PERFORM PASS                                         NC1774.2
208000             PERFORM PRINT-DETAIL                                 NC1774.2
208100     ELSE                                                         NC1774.2
208200             MOVE    666999.666333 TO CORRECT-N                   NC1774.2
208300             MOVE    WRK-DS-06V06  TO COMPUTED-N                  NC1774.2
208400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1774.2
208500                  TO RE-MARK                                      NC1774.2
208600             PERFORM FAIL                                         NC1774.2
208700             PERFORM PRINT-DETAIL.                                NC1774.2
208800*                                                                 NC1774.2
208900 ADD-INIT-F2-40.                                                  NC1774.2
209000     MOVE   "VI-73 6.6.2" TO ANSI-REFERENCE.                      NC1774.2
209100*    ==-->  OPTIONAL WORD "TO"  <--==                             NC1774.2
209200     MOVE     ZERO TO WRK-DS-09V09.                               NC1774.2
209300 ADD-TEST-F2-40-0.                                                NC1774.2
209400     ADD      A06THREES-DS-03V03                                  NC1774.2
209500           TO A12THREES-DS-06V06 GIVING WRK-DS-09V09.             NC1774.2
209600 ADD-TEST-F2-40-1.                                                NC1774.2
209700     IF       WRK-DS-09V09 EQUAL TO 000333666.666333000           NC1774.2
209800              PERFORM PASS GO TO ADD-WRITE-F2-40.                 NC1774.2
209900     GO       TO ADD-FAIL-F2-40.                                  NC1774.2
210000 ADD-DELETE-F2-40.                                                NC1774.2
210100     PERFORM  DE-LETE.                                            NC1774.2
210200     GO       TO ADD-WRITE-F2-40.                                 NC1774.2
210300 ADD-FAIL-F2-40.                                                  NC1774.2
210400     MOVE     WRK-DS-09V09 TO COMPUTED-N.                         NC1774.2
210500     MOVE     000333666.666333000 TO CORRECT-N.                   NC1774.2
210600     PERFORM  FAIL.                                               NC1774.2
210700 ADD-WRITE-F2-40.                                                 NC1774.2
210800     MOVE     "ADD-TEST-F2-40" TO PAR-NAME.                       NC1774.2
210900     PERFORM  PRINT-DETAIL.                                       NC1774.2
211000*                                                                 NC1774.2
211100 ADD-INIT-F2-41.                                                  NC1774.2
211200     MOVE   "VI-73 6.6.2" TO ANSI-REFERENCE.                      NC1774.2
211300*    ==-->  OPTIONAL WORD "TO"  <--==                             NC1774.2
211400     MOVE     ZERO TO WRK-DS-06V06.                               NC1774.2
211500 ADD-TEST-F2-41-0.                                                NC1774.2
211600     ADD      A05ONES-DS-05V00                                    NC1774.2
211700              A05ONES-DS-00V05                                    NC1774.2
211800              A12THREES-DS-06V06                                  NC1774.2
211900          TO  A06THREES-DS-03V03 GIVING WRK-DS-06V06.             NC1774.2
212000 ADD-TEST-F2-41-1.                                                NC1774.2
212100     IF       WRK-DS-06V06 EQUAL TO 344777.777443                 NC1774.2
212200              PERFORM PASS GO TO ADD-WRITE-F2-41.                 NC1774.2
212300     GO       TO ADD-FAIL-F2-41.                                  NC1774.2
212400 ADD-DELETE-F2-41.                                                NC1774.2
212500     PERFORM  DE-LETE.                                            NC1774.2
212600     GO       TO ADD-WRITE-F2-41.                                 NC1774.2
212700 ADD-FAIL-F2-41.                                                  NC1774.2
212800     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC1774.2
212900     MOVE     344777.777443 TO CORRECT-N.                         NC1774.2
213000     PERFORM  FAIL.                                               NC1774.2
213100 ADD-WRITE-F2-41.                                                 NC1774.2
213200     MOVE     "ADD-TEST-F2-41" TO PAR-NAME.                       NC1774.2
213300     PERFORM PRINT-DETAIL.                                        NC1774.2
213400*                                                                 NC1774.2
213500 CCVS-EXIT SECTION.                                               NC1774.2
213600 CCVS-999999.                                                     NC1774.2
213700     GO TO CLOSE-FILES.                                           NC1774.2
      *END-OF,NC177A                                                                  
