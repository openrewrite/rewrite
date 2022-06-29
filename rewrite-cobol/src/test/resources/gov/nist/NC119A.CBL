      *HEADER,COBOL,NC119A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1194.2
000200 PROGRAM-ID.                                                      NC1194.2
000300     NC119A.                                                      NC1194.2
000400****************************************************************  NC1194.2
000500*                                                              *  NC1194.2
000600*    VALIDATION FOR:-                                          *  NC1194.2
000700*                                                              *  NC1194.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1194.2
000900*                                                              *  NC1194.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1194.2
001100*                                                              *  NC1194.2
001200****************************************************************  NC1194.2
001300*                                                              *  NC1194.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1194.2
001500*                                                              *  NC1194.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1194.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1194.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1194.2
001900*                                                              *  NC1194.2
002000****************************************************************  NC1194.2
002100*                                                                 NC1194.2
002200*      PROGRAM NC119A TESTS THE USE OF THE "SIGN" CLAUSE WITH     NC1194.2
002300*      FORMATS 1 AND 2 OF THE SUBTRACT STATEMENT.                 NC1194.2
002400*      ALL COMBINATIONS OF THE SIGN CLAUSE PHRASES ARE USED       NC1194.2
002500*      WITH DATA ITEMS OF VARIOUS LENGTHS.                        NC1194.2
002600*                                                                 NC1194.2
002700*                                                                 NC1194.2
002800                                                                  NC1194.2
002900 ENVIRONMENT DIVISION.                                            NC1194.2
003000 CONFIGURATION SECTION.                                           NC1194.2
003100 SOURCE-COMPUTER.                                                 NC1194.2
003200     XXXXX082.                                                    NC1194.2
003300 OBJECT-COMPUTER.                                                 NC1194.2
003400     XXXXX083.                                                    NC1194.2
003500 INPUT-OUTPUT SECTION.                                            NC1194.2
003600 FILE-CONTROL.                                                    NC1194.2
003700     SELECT PRINT-FILE ASSIGN TO                                  NC1194.2
003800     XXXXX055.                                                    NC1194.2
003900 DATA DIVISION.                                                   NC1194.2
004000 FILE SECTION.                                                    NC1194.2
004100 FD  PRINT-FILE.                                                  NC1194.2
004200 01  PRINT-REC PICTURE X(120).                                    NC1194.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 NC1194.2
004400 WORKING-STORAGE SECTION.                                         NC1194.2
004500 77  SIZE-ERR                     PICTURE X  VALUE SPACE.         NC1194.2
004600 77  A18TWOS-DS-LS-18V00             PICTURE S9(18)               NC1194.2
004700             SIGN IS LEADING SEPARATE                             NC1194.2
004800                                  VALUE 222222222222222222.       NC1194.2
004900 77  A18ONES-DS-TS-18V00             PICTURE S9(18)               NC1194.2
005000            SIGN IS TRAILING SEPARATE                             NC1194.2
005100                                  VALUE 111111111111111111.       NC1194.2
005200 77  WRK-DS-10V00                 PICTURE S9(10) TRAILING.        NC1194.2
005300 77  A10ONES-DS-T-10V00             PICTURE S9(10)                NC1194.2
005400            SIGN TRAILING                                         NC1194.2
005500                                  VALUE 1111111111.               NC1194.2
005600 77  A05ONES-DS-L-05V00             PICTURE S9(5)                 NC1194.2
005700           SIGN LEADING                                           NC1194.2
005800                                  VALUE 11111.                    NC1194.2
005900 77  A02ONES-DS-LS-02V00             PICTURE S99                  NC1194.2
006000           LEADING SEPARATE                                       NC1194.2
006100                                  VALUE 11.                       NC1194.2
006200 77  WRK-DS-09V09                 PICTURE S9(9)V9(9)   TRAILING.  NC1194.2
006300 77  WRK-DS-T-18V00 REDEFINES WRK-DS-09V09                        NC1194.2
006400                                  PICTURE S9(18) TRAILING.        NC1194.2
006500 77  A06THREES-DS-03V03           PICTURE S999V999                NC1194.2
006600                                  VALUE 333.333.                  NC1194.2
006700 77  A12THREES-DS-06V06           PICTURE S9(6)V9(6)              NC1194.2
006800                                  VALUE 333333.333333.            NC1194.2
006900 77  WRK-DS-TS-06V06                 PICTURE S9(6)V9(6)           NC1194.2
007000           SIGN IS TRAILING SEPARATE CHARACTER.                   NC1194.2
007100 77  WRK-DS-TS-12V00-S REDEFINES WRK-DS-TS-06V06                  NC1194.2
007200         TRAILING SEPARATE                                        NC1194.2
007300                                  PICTURE S9(12).                 NC1194.2
007400 77  A05ONES-DS-LS-00V05             PICTURE SV9(5)               NC1194.2
007500         LEADING SEPARATE                                         NC1194.2
007600                                  VALUE .11111.                   NC1194.2
007700 77  WRK-DS-T-05V00                 PICTURE S9(5) TRAILING.       NC1194.2
007800 77  WRK-DS-02V00                 PICTURE S99.                    NC1194.2
007900 77  A12ONES-DS-L-12V00             PICTURE S9(12)                NC1194.2
008000              USAGE IS DISPLAY SIGN IS LEADING                    NC1194.2
008100                                  VALUE 111111111111.             NC1194.2
008200 77  WRK-DS-03V10                 PICTURE S999V9(10).             NC1194.2
008300 77  WRK-DS-13V00-S REDEFINES WRK-DS-03V10                        NC1194.2
008400                                  PICTURE S9(13).                 NC1194.2
008500 77  A99-DS-T-02V00                 PICTURE S99                   NC1194.2
008600             USAGE IS DISPLAY SIGN IS TRAILING                    NC1194.2
008700                                  VALUE 99.                       NC1194.2
008800 77  A03ONES-DS-02V01             PICTURE S99V9                   NC1194.2
008900                                  VALUE 11.1.                     NC1194.2
009000 77  A06ONES-DS-TS-03V03             PICTURE S999V999             NC1194.2
009100             USAGE IS DISPLAY TRAILING SEPARATE                   NC1194.2
009200                                  VALUE 111.111.                  NC1194.2
009300 77  A08TWOS-DS-02V06             PICTURE S99V9(6)                NC1194.2
009400                                  VALUE 22.222222.                NC1194.2
009500 77  A01ONES-DS-LS-P0801              PICTURE SP(8)9              NC1194.2
009600             SIGN IS LEADING SEPARATE                             NC1194.2
009700                                  VALUE .000000001.               NC1194.2
009800 77  WRK-CS-18V00                 PICTURE S9(18) COMPUTATIONAL.   NC1194.2
009900 77  A18ONES-CS-18V00             PICTURE S9(18) COMPUTATIONAL    NC1194.2
010000                                  VALUE 111111111111111111.       NC1194.2
010100 77  WRK-CS-02V02                 PICTURE S99V99 COMPUTATIONAL.   NC1194.2
010200 77  A99-CS-02V00                 PICTURE S99 COMPUTATIONAL       NC1194.2
010300                                  VALUE 99.                       NC1194.2
010400 77  WRK-DS-TS-0201P            PICTURE S99P TRAILING SEPARATE.   NC1194.2
010500 77  WRK-DS-06V00                 PICTURE S9(6).                  NC1194.2
010600 77  AZERO-DS-LS-05V05               PICTURE S9(5)V9(5)           NC1194.2
010700             SIGN IS LEADING SEPARATE USAGE DISPLAY               NC1194.2
010800                                  VALUE ZERO.                     NC1194.2
010900 77  NDATA-DS-09V09               PICTURE S9(9)V9(9)              NC1194.2
011000                                  VALUE +012345678.876543210.     NC1194.2
011100 77  XDATA-XN-00018               PICTURE X(18)                   NC1194.2
011200                                  VALUE "00ABCDEFGHI  4321 ".     NC1194.2
011300 77  WRK-XN-00018                 PICTURE X(18).                  NC1194.2
011400 77  ADD-12    PICTURE PP9        VALUE .001.                     NC1194.2
011500 77  ADD-13    PICTURE 9PP        VALUE 100.                      NC1194.2
011600 77  ADD-14    PICTURE 999V999.                                   NC1194.2
011700 77  WRK-CU-18V00      PICTURE 9(18) VALUE ZERO COMPUTATIONAL.    NC1194.2
011800 77  A18EIGHTS-CS-18V00 PICTURE S9(18) VALUE -888888888888888888  NC1194.2
011900             COMPUTATIONAL.                                       NC1194.2
012000 77  A14TWOS-CS-18V00  PICTURE S9(18) VALUE -000022222222222222   NC1194.2
012100             COMPUTATIONAL.                                       NC1194.2
012200 77  A12THREES-CU-18V00 PICTURE 9(18) VALUE 000000333333333333    NC1194.2
012300             COMPUTATIONAL.                                       NC1194.2
012400 77  A16FOURS-CS-18V00 PICTURE S9(18) VALUE 004444444444444444    NC1194.2
012500             COMPUTATIONAL.                                       NC1194.2
012600 77  A18FIVES-CS-18V00 PICTURE S9(18) VALUE -555555555555555555   NC1194.2
012700             COMPUTATIONAL.                                       NC1194.2
012800 77  A18SIXES-CS-18V00 PICTURE S9(18) VALUE 666666666666666666    NC1194.2
012900             COMPUTATIONAL.                                       NC1194.2
013000 77  A12SEVENS-CU-18V00 PICTURE 9(18) VALUE 000000777777777777    NC1194.2
013100             COMPUTATIONAL.                                       NC1194.2
013200 77  WRK-DU-18V00      PICTURE 9(18) VALUE ZERO.                  NC1194.2
013300 77  A18THREES-CS-18V00 PICTURE S9(18) VALUE -333333333333333333  NC1194.2
013400             COMPUTATIONAL.                                       NC1194.2
013500 77  WRK-CS-03V00 PICTURE S999 COMPUTATIONAL SYNCHRONIZED RIGHT.  NC1194.2
013600 01  SUBTRACT-DATA SIGN IS LEADING SEPARATE DISPLAY.              NC1194.2
013700     02  SUBTR-1    PICTURE 9         VALUE 1.                    NC1194.2
013800     02  SUBTR-2    PICTURE S99       VALUE 99.                   NC1194.2
013900     02  SUBTR-3    PICTURE S9V99     VALUE -1.                   NC1194.2
014000     02  SUBTR-4    PICTURE SPP9      VALUE .001.                 NC1194.2
014100     02  SUBTR-5    PICTURE S9PP      VALUE 100.                  NC1194.2
014200     02  SUBTR-6    PICTURE 9  VALUE 1.                           NC1194.2
014300     02  SUBTR-7    PICTURE S99  VALUE 99.                        NC1194.2
014400     02  SUBTR-8    PICTURE S9V99  VALUE -9.99.                   NC1194.2
014500     02  SUBTR-10    PICTURE S999  VALUE 100.                     NC1194.2
014600     02  SUBTR-11   PICTURE S999V999.                             NC1194.2
014700 01  N-3                                PICTURE IS 99999.         NC1194.2
014800 01  N-4                                PICTURE IS 9(5)           NC1194.2
014900     VALUE IS 52800.                                              NC1194.2
015000 01  N-5                                PICTURE IS S9(9)V99       NC1194.2
015100             SIGN IS LEADING SEPARATE                             NC1194.2
015200     VALUE IS 000000001.00.                                       NC1194.2
015300 01  N-7                                PICTURE IS S9(7)V9(4)     NC1194.2
015400             SIGN IS LEADING SEPARATE CHARACTER                   NC1194.2
015500     VALUE IS 0000001.0000.                                       NC1194.2
015600 01  X-8 REDEFINES N-7                  PICTURE IS X(11).         NC1194.2
015700 01  N-10                               PICTURE IS S99999V        NC1194.2
015800     VALUE IS -00001.                                             NC1194.2
015900 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1194.2
016000 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1194.2
016100 01  N-13                               PICTURE IS 9(5)           NC1194.2
016200     VALUE IS 99999.                                              NC1194.2
016300 01  N-14                               PICTURE IS 9 VALUE IS 1.  NC1194.2
016400 01  N-15                               PICTURE IS 9(16).         NC1194.2
016500 01  N-16                               PICTURE IS S999999V99     NC1194.2
016600     VALUE IS 5.90.                                               NC1194.2
016700 01  N-17                               PICTURE IS S9(3)V99       NC1194.2
016800     VALUE IS +3.6.                                               NC1194.2
016900 01  N-18                               PICTURE IS S9(10)         NC1194.2
017000     VALUE IS -5.                                                 NC1194.2
017100 01  N-19                               PICTURE IS $9.00.         NC1194.2
017200 01  N-20                               PICTURE IS S9(9)          NC1194.2
017300     VALUE IS -999999999.                                         NC1194.2
017400 01  N-21                               PICTURE IS 9 VALUE IS 5.  NC1194.2
017500 01  N-22                               PICTURE IS 999V99         NC1194.2
017600     VALUE IS 005.55.                                             NC1194.2
017700 01  N-23                               PICTURE IS $$$.99CR.      NC1194.2
017800 01  N-25                               PICTURE IS 9 VALUE IS 1.  NC1194.2
017900 01  N-26               PICTURE 9(5).                             NC1194.2
018000 01  N-27                               PICTURE IS 9999V9         NC1194.2
018100     VALUE IS 9999.9.                                             NC1194.2
018200 01  N-28                               PICTURE IS $9999.00.      NC1194.2
018300 01  N-40                               PICTURE IS 9(7)           NC1194.2
018400     VALUE IS 7777777.                                            NC1194.2
018500 01  N-41                               PICTURE IS 9(7)           NC1194.2
018600     VALUE IS 1111111.                                            NC1194.2
018700 01  N-42                               PICTURE IS 9(3)P(4).      NC1194.2
018800 01  TRUNC-DATA.                                                  NC1194.2
018900     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1194.2
019000     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1194.2
019100     02 N-45  PICTURE S9.                                         NC1194.2
019200 01  MINUS-NAMES SIGN IS LEADING SEPARATE CHARACTER.              NC1194.2
019300     02  MINUS-NAME1 PICTURE S9(18) VALUE -999999999999999999.    NC1194.2
019400     02  MINUS-NAME2 PICTURE S9(18) VALUE -999999999999999999.    NC1194.2
019500     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC1194.2
019600     02  PLUS-NAME1  PICTURE S9(18) VALUE +999999999999999999.    NC1194.2
019700     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC1194.2
019800     02  MINUS-NAME3 PICTURE SV9(18) VALUE -.999999999999999999.  NC1194.2
019900     02  MINUS-NAME4 PICTURE SV9(18) VALUE -.999999999999999999.  NC1194.2
020000     02  EVEN-NAME2  PICTURE SV9(18)  VALUE +.1.                  NC1194.2
020100     02  PLUS-NAME3  PICTURE SV9(18) VALUE +.999999999999999999.  NC1194.2
020200     02  PLUS-NAME4  PICTURE SV9(18) VALUE +.999999999999999999.  NC1194.2
020300     02  WHOLE-FIELD   PICTURE S9(18).                            NC1194.2
020400     02  DECMAL-FIELD  PICTURE SV9(18).                           NC1194.2
020500 01  TEST-RESULTS.                                                NC1194.2
020600     02 FILLER                   PIC X      VALUE SPACE.          NC1194.2
020700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1194.2
020800     02 FILLER                   PIC X      VALUE SPACE.          NC1194.2
020900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1194.2
021000     02 FILLER                   PIC X      VALUE SPACE.          NC1194.2
021100     02  PAR-NAME.                                                NC1194.2
021200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1194.2
021300       03  PARDOT-X              PIC X      VALUE SPACE.          NC1194.2
021400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1194.2
021500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1194.2
021600     02 RE-MARK                  PIC X(61).                       NC1194.2
021700 01  TEST-COMPUTED.                                               NC1194.2
021800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1194.2
021900     02 FILLER                   PIC X(17)  VALUE                 NC1194.2
022000            "       COMPUTED=".                                   NC1194.2
022100     02 COMPUTED-X.                                               NC1194.2
022200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1194.2
022300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1194.2
022400                                 PIC -9(9).9(9).                  NC1194.2
022500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1194.2
022600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1194.2
022700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1194.2
022800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1194.2
022900         04 COMPUTED-18V0                    PIC -9(18).          NC1194.2
023000         04 FILLER                           PIC X.               NC1194.2
023100     03 FILLER PIC X(50) VALUE SPACE.                             NC1194.2
023200 01  TEST-CORRECT.                                                NC1194.2
023300     02 FILLER PIC X(30) VALUE SPACE.                             NC1194.2
023400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1194.2
023500     02 CORRECT-X.                                                NC1194.2
023600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1194.2
023700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1194.2
023800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1194.2
023900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1194.2
024000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1194.2
024100     03      CR-18V0 REDEFINES CORRECT-A.                         NC1194.2
024200         04 CORRECT-18V0                     PIC -9(18).          NC1194.2
024300         04 FILLER                           PIC X.               NC1194.2
024400     03 FILLER PIC X(2) VALUE SPACE.                              NC1194.2
024500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1194.2
024600 01  CCVS-C-1.                                                    NC1194.2
024700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1194.2
024800-    "SS  PARAGRAPH-NAME                                          NC1194.2
024900-    "       REMARKS".                                            NC1194.2
025000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1194.2
025100 01  CCVS-C-2.                                                    NC1194.2
025200     02 FILLER                     PIC X        VALUE SPACE.      NC1194.2
025300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1194.2
025400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1194.2
025500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1194.2
025600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1194.2
025700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1194.2
025800 01  REC-CT                        PIC 99       VALUE ZERO.       NC1194.2
025900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1194.2
026000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1194.2
026100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1194.2
026200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1194.2
026300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1194.2
026400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1194.2
026500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1194.2
026600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1194.2
026700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1194.2
026800 01  CCVS-H-1.                                                    NC1194.2
026900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1194.2
027000     02  FILLER                    PIC X(42)    VALUE             NC1194.2
027100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1194.2
027200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1194.2
027300 01  CCVS-H-2A.                                                   NC1194.2
027400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1194.2
027500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1194.2
027600   02  FILLER                        PIC XXXX   VALUE             NC1194.2
027700     "4.2 ".                                                      NC1194.2
027800   02  FILLER                        PIC X(28)  VALUE             NC1194.2
027900            " COPY - NOT FOR DISTRIBUTION".                       NC1194.2
028000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1194.2
028100                                                                  NC1194.2
028200 01  CCVS-H-2B.                                                   NC1194.2
028300   02  FILLER                        PIC X(15)  VALUE             NC1194.2
028400            "TEST RESULT OF ".                                    NC1194.2
028500   02  TEST-ID                       PIC X(9).                    NC1194.2
028600   02  FILLER                        PIC X(4)   VALUE             NC1194.2
028700            " IN ".                                               NC1194.2
028800   02  FILLER                        PIC X(12)  VALUE             NC1194.2
028900     " HIGH       ".                                              NC1194.2
029000   02  FILLER                        PIC X(22)  VALUE             NC1194.2
029100            " LEVEL VALIDATION FOR ".                             NC1194.2
029200   02  FILLER                        PIC X(58)  VALUE             NC1194.2
029300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1194.2
029400 01  CCVS-H-3.                                                    NC1194.2
029500     02  FILLER                      PIC X(34)  VALUE             NC1194.2
029600            " FOR OFFICIAL USE ONLY    ".                         NC1194.2
029700     02  FILLER                      PIC X(58)  VALUE             NC1194.2
029800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1194.2
029900     02  FILLER                      PIC X(28)  VALUE             NC1194.2
030000            "  COPYRIGHT   1985 ".                                NC1194.2
030100 01  CCVS-E-1.                                                    NC1194.2
030200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1194.2
030300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1194.2
030400     02 ID-AGAIN                     PIC X(9).                    NC1194.2
030500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1194.2
030600 01  CCVS-E-2.                                                    NC1194.2
030700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1194.2
030800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1194.2
030900     02 CCVS-E-2-2.                                               NC1194.2
031000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1194.2
031100         03 FILLER                   PIC X      VALUE SPACE.      NC1194.2
031200         03 ENDER-DESC               PIC X(44)  VALUE             NC1194.2
031300            "ERRORS ENCOUNTERED".                                 NC1194.2
031400 01  CCVS-E-3.                                                    NC1194.2
031500     02  FILLER                      PIC X(22)  VALUE             NC1194.2
031600            " FOR OFFICIAL USE ONLY".                             NC1194.2
031700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1194.2
031800     02  FILLER                      PIC X(58)  VALUE             NC1194.2
031900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1194.2
032000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1194.2
032100     02 FILLER                       PIC X(15)  VALUE             NC1194.2
032200             " COPYRIGHT 1985".                                   NC1194.2
032300 01  CCVS-E-4.                                                    NC1194.2
032400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1194.2
032500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1194.2
032600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1194.2
032700     02 FILLER                       PIC X(40)  VALUE             NC1194.2
032800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1194.2
032900 01  XXINFO.                                                      NC1194.2
033000     02 FILLER                       PIC X(19)  VALUE             NC1194.2
033100            "*** INFORMATION ***".                                NC1194.2
033200     02 INFO-TEXT.                                                NC1194.2
033300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1194.2
033400       04 XXCOMPUTED                 PIC X(20).                   NC1194.2
033500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1194.2
033600       04 XXCORRECT                  PIC X(20).                   NC1194.2
033700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1194.2
033800 01  HYPHEN-LINE.                                                 NC1194.2
033900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1194.2
034000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1194.2
034100-    "*****************************************".                 NC1194.2
034200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1194.2
034300-    "******************************".                            NC1194.2
034400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1194.2
034500     "NC119A".                                                    NC1194.2
034600 PROCEDURE DIVISION.                                              NC1194.2
034700 CCVS1 SECTION.                                                   NC1194.2
034800 OPEN-FILES.                                                      NC1194.2
034900     OPEN     OUTPUT PRINT-FILE.                                  NC1194.2
035000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1194.2
035100     MOVE    SPACE TO TEST-RESULTS.                               NC1194.2
035200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1194.2
035300     GO TO CCVS1-EXIT.                                            NC1194.2
035400 CLOSE-FILES.                                                     NC1194.2
035500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1194.2
035600 TERMINATE-CCVS.                                                  NC1194.2
035700*    EXIT PROGRAM.                                                NC1194.2
035800*TERMINATE-CALL.                                                  NC1194.2
035900     STOP     RUN.                                                NC1194.2
036000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1194.2
036100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1194.2
036200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1194.2
036300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1194.2
036400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1194.2
036500 PRINT-DETAIL.                                                    NC1194.2
036600     IF REC-CT NOT EQUAL TO ZERO                                  NC1194.2
036700             MOVE "." TO PARDOT-X                                 NC1194.2
036800             MOVE REC-CT TO DOTVALUE.                             NC1194.2
036900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1194.2
037000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1194.2
037100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1194.2
037200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1194.2
037300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1194.2
037400     MOVE SPACE TO CORRECT-X.                                     NC1194.2
037500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1194.2
037600     MOVE     SPACE TO RE-MARK.                                   NC1194.2
037700 HEAD-ROUTINE.                                                    NC1194.2
037800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1194.2
037900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1194.2
038000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1194.2
038100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1194.2
038200 COLUMN-NAMES-ROUTINE.                                            NC1194.2
038300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1194.2
038400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1194.2
038500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1194.2
038600 END-ROUTINE.                                                     NC1194.2
038700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1194.2
038800 END-RTN-EXIT.                                                    NC1194.2
038900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1194.2
039000 END-ROUTINE-1.                                                   NC1194.2
039100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1194.2
039200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1194.2
039300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1194.2
039400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1194.2
039500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1194.2
039600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1194.2
039700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1194.2
039800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1194.2
039900  END-ROUTINE-12.                                                 NC1194.2
040000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1194.2
040100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1194.2
040200         MOVE "NO " TO ERROR-TOTAL                                NC1194.2
040300         ELSE                                                     NC1194.2
040400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1194.2
040500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1194.2
040600     PERFORM WRITE-LINE.                                          NC1194.2
040700 END-ROUTINE-13.                                                  NC1194.2
040800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1194.2
040900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1194.2
041000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1194.2
041100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1194.2
041200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1194.2
041300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1194.2
041400          MOVE "NO " TO ERROR-TOTAL                               NC1194.2
041500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1194.2
041600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1194.2
041700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1194.2
041800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1194.2
041900 WRITE-LINE.                                                      NC1194.2
042000     ADD 1 TO RECORD-COUNT.                                       NC1194.2
042100*    IF RECORD-COUNT GREATER 42                                   NC1194.2
042200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1194.2
042300*        MOVE SPACE TO DUMMY-RECORD                               NC1194.2
042400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1194.2
042500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1194.2
042600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1194.2
042700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1194.2
042800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1194.2
042900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1194.2
043000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1194.2
043100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1194.2
043200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1194.2
043300*        MOVE ZERO TO RECORD-COUNT.                               NC1194.2
043400     PERFORM WRT-LN.                                              NC1194.2
043500 WRT-LN.                                                          NC1194.2
043600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1194.2
043700     MOVE SPACE TO DUMMY-RECORD.                                  NC1194.2
043800 BLANK-LINE-PRINT.                                                NC1194.2
043900     PERFORM WRT-LN.                                              NC1194.2
044000 FAIL-ROUTINE.                                                    NC1194.2
044100     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1194.2
044200            GO TO FAIL-ROUTINE-WRITE.                             NC1194.2
044300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1194.2
044400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1194.2
044500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1194.2
044600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1194.2
044700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1194.2
044800     GO TO  FAIL-ROUTINE-EX.                                      NC1194.2
044900 FAIL-ROUTINE-WRITE.                                              NC1194.2
045000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1194.2
045100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1194.2
045200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1194.2
045300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1194.2
045400 FAIL-ROUTINE-EX. EXIT.                                           NC1194.2
045500 BAIL-OUT.                                                        NC1194.2
045600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1194.2
045700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1194.2
045800 BAIL-OUT-WRITE.                                                  NC1194.2
045900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1194.2
046000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1194.2
046100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1194.2
046200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1194.2
046300 BAIL-OUT-EX. EXIT.                                               NC1194.2
046400 CCVS1-EXIT.                                                      NC1194.2
046500     EXIT.                                                        NC1194.2
046600 SECT-NC119A-001 SECTION.                                         NC1194.2
046700 SUB-INIT-GF-1.                                                   NC1194.2
046800     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
046900     PERFORM END-ROUTINE.                                         NC1194.2
047000     MOVE "SUBTRACT" TO FEATURE.                                  NC1194.2
047100     MOVE    1 TO N-5.                                            NC1194.2
047200 SUB-TEST-GF-1-0.                                                 NC1194.2
047300     SUBTRACT 1 FROM N-5.                                         NC1194.2
047400 SUB-TEST-GF-1-1.                                                 NC1194.2
047500     IF       N-5 EQUAL TO 0                                      NC1194.2
047600              PERFORM PASS                                        NC1194.2
047700              GO TO SUB-WRITE-GF-1.                               NC1194.2
047800     GO TO    SUB-FAIL-GF-1.                                      NC1194.2
047900 SUB-DELETE-GF-1.                                                 NC1194.2
048000     PERFORM  DE-LETE.                                            NC1194.2
048100     GO TO    SUB-WRITE-GF-1.                                     NC1194.2
048200 SUB-FAIL-GF-1.                                                   NC1194.2
048300     MOVE     N-5 TO COMPUTED-N.                                  NC1194.2
048400     MOVE     0 TO CORRECT-N.                                     NC1194.2
048500     PERFORM  FAIL.                                               NC1194.2
048600 SUB-WRITE-GF-1.                                                  NC1194.2
048700     MOVE "SUB-TEST-GF-1 " TO PAR-NAME.                           NC1194.2
048800     PERFORM  PRINT-DETAIL.                                       NC1194.2
048900 SUB-INIT-GF-2.                                                   NC1194.2
049000     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
049100     MOVE     A18TWOS-DS-LS-18V00 TO WRK-DS-T-18V00.              NC1194.2
049200 SUB-TEST-GF-2-0.                                                 NC1194.2
049300     SUBTRACT A18ONES-DS-TS-18V00 FROM WRK-DS-T-18V00.            NC1194.2
049400 SUB-TEST-GF-2-1.                                                 NC1194.2
049500     IF       WRK-DS-T-18V00 EQUAL TO 111111111111111111          NC1194.2
049600              PERFORM PASS GO TO SUB-WRITE-GF-2.                  NC1194.2
049700     GO       TO SUB-FAIL-GF-2.                                   NC1194.2
049800 SUB-DELETE-GF-2.                                                 NC1194.2
049900     PERFORM  DE-LETE.                                            NC1194.2
050000     GO       TO SUB-WRITE-GF-2.                                  NC1194.2
050100 SUB-FAIL-GF-2.                                                   NC1194.2
050200     MOVE     WRK-DS-T-18V00    TO COMPUTED-18V0.                 NC1194.2
050300     MOVE     111111111111111111 TO CORRECT-18V0.                 NC1194.2
050400     PERFORM  FAIL.                                               NC1194.2
050500 SUB-WRITE-GF-2.                                                  NC1194.2
050600     MOVE     "SUB-TEST-GF-2" TO PAR-NAME.                        NC1194.2
050700     PERFORM  PRINT-DETAIL.                                       NC1194.2
050800 SUB-INIT-GF-3.                                                   NC1194.2
050900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
051000     MOVE     A12THREES-DS-06V06 TO WRK-DS-TS-06V06.              NC1194.2
051100 SUB-TEST-GF-3-0.                                                 NC1194.2
051200     SUBTRACT A05ONES-DS-L-05V00                                  NC1194.2
051300              A05ONES-DS-LS-00V05                                 NC1194.2
051400              A06ONES-DS-TS-03V03 FROM WRK-DS-TS-06V06.           NC1194.2
051500 SUB-TEST-GF-3-1.                                                 NC1194.2
051600     IF       WRK-DS-TS-06V06 EQUAL TO 322111.111223              NC1194.2
051700              PERFORM PASS GO TO SUB-WRITE-GF-3.                  NC1194.2
051800     GO       TO SUB-FAIL-GF-3.                                   NC1194.2
051900 SUB-DELETE-GF-3.                                                 NC1194.2
052000     PERFORM  DE-LETE.                                            NC1194.2
052100     GO       TO SUB-WRITE-GF-3.                                  NC1194.2
052200 SUB-FAIL-GF-3.                                                   NC1194.2
052300     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1194.2
052400     MOVE     322111.111223 TO CORRECT-N.                         NC1194.2
052500     PERFORM  FAIL.                                               NC1194.2
052600 SUB-WRITE-GF-3.                                                  NC1194.2
052700     MOVE     "SUB-TEST-GF-3" TO PAR-NAME.                        NC1194.2
052800     PERFORM  PRINT-DETAIL.                                       NC1194.2
052900 SUB-INIT-GF-13.                                                  NC1194.2
053000     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
053100     MOVE     "  GIVING" TO FEATURE.                              NC1194.2
053200     MOVE     ZERO TO WRK-DS-09V09.                               NC1194.2
053300 SUB-TEST-GF-13-0.                                                NC1194.2
053400     SUBTRACT A06THREES-DS-03V03 FROM A12THREES-DS-06V06          NC1194.2
053500              GIVING WRK-DS-TS-06V06.                             NC1194.2
053600 SUB-TEST-GF-13-1.                                                NC1194.2
053700     IF       WRK-DS-TS-06V06 EQUAL TO 333000.000333              NC1194.2
053800              PERFORM PASS GO TO SUB-WRITE-GF-13.                 NC1194.2
053900     GO       TO SUB-FAIL-GF-13.                                  NC1194.2
054000 SUB-DELETE-GF-13.                                                NC1194.2
054100     PERFORM  DE-LETE.                                            NC1194.2
054200     GO       TO SUB-WRITE-GF-13.                                 NC1194.2
054300 SUB-FAIL-GF-13.                                                  NC1194.2
054400     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1194.2
054500     MOVE     333000.000333 TO CORRECT-N.                         NC1194.2
054600     PERFORM  FAIL.                                               NC1194.2
054700 SUB-WRITE-GF-13.                                                 NC1194.2
054800     MOVE     "SUB-TEST-GF-13" TO PAR-NAME.                       NC1194.2
054900     PERFORM  PRINT-DETAIL.                                       NC1194.2
055000 SUB-INIT-GF-14.                                                  NC1194.2
055100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
055200     MOVE     ZERO TO WRK-DS-TS-06V06.                            NC1194.2
055300 SUB-TEST-GF-14.                                                  NC1194.2
055400     SUBTRACT A05ONES-DS-L-05V00                                  NC1194.2
055500              A05ONES-DS-LS-00V05                                 NC1194.2
055600              A12THREES-DS-06V06                                  NC1194.2
055700              A06THREES-DS-03V03 FROM ZERO GIVING WRK-DS-TS-06V06.NC1194.2
055800     IF       WRK-DS-TS-06V06 EQUAL TO -344777.777443             NC1194.2
055900              PERFORM PASS GO TO SUB-WRITE-GF-14.                 NC1194.2
056000     GO       TO SUB-FAIL-GF-14.                                  NC1194.2
056100 SUB-DELETE-GF-14.                                                NC1194.2
056200     PERFORM  DE-LETE.                                            NC1194.2
056300     GO       TO SUB-WRITE-GF-14.                                 NC1194.2
056400 SUB-FAIL-GF-14.                                                  NC1194.2
056500     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1194.2
056600     MOVE     -344777.777443 TO CORRECT-N.                        NC1194.2
056700     PERFORM  FAIL.                                               NC1194.2
056800 SUB-WRITE-GF-14.                                                 NC1194.2
056900     MOVE     "SUB-TEST-GF-14" TO PAR-NAME.                       NC1194.2
057000     PERFORM  PRINT-DETAIL.                                       NC1194.2
057100 SUB-INIT-GF-4.                                                   NC1194.2
057200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
057300     MOVE     "  ROUNDED" TO FEATURE.                             NC1194.2
057400     MOVE     ZERO TO WRK-DS-TS-0201P.                            NC1194.2
057500 SUB-TEST-GF-4-0.                                                 NC1194.2
057600     SUBTRACT A99-DS-T-02V00 FROM WRK-DS-TS-0201P ROUNDED.        NC1194.2
057700 SUB-TEST-GF-4-1.                                                 NC1194.2
057800     IF       WRK-DS-TS-0201P EQUAL TO -100                       NC1194.2
057900              PERFORM PASS GO TO SUB-WRITE-GF-4.                  NC1194.2
058000     GO       TO SUB-FAIL-GF-4.                                   NC1194.2
058100 SUB-DELETE-GF-4.                                                 NC1194.2
058200     PERFORM  DE-LETE.                                            NC1194.2
058300     GO       TO SUB-WRITE-GF-4.                                  NC1194.2
058400 SUB-FAIL-GF-4.                                                   NC1194.2
058500     MOVE     WRK-DS-TS-0201P TO COMPUTED-N.                      NC1194.2
058600     MOVE    -100 TO CORRECT-N.                                   NC1194.2
058700     PERFORM  FAIL.                                               NC1194.2
058800 SUB-WRITE-GF-4.                                                  NC1194.2
058900     MOVE     "SUB-TEST-GF-4" TO PAR-NAME.                        NC1194.2
059000     PERFORM  PRINT-DETAIL.                                       NC1194.2
059100 SUB-INIT-GF-15.                                                  NC1194.2
059200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
059300     MOVE     -099999.999999 TO WRK-DS-TS-06V06.                  NC1194.2
059400     MOVE    ZERO TO WRK-DS-06V00.                                NC1194.2
059500 SUB-TEST-GF-15-0.                                                NC1194.2
059600     SUBTRACT A05ONES-DS-L-05V00                                  NC1194.2
059700              -11111                                              NC1194.2
059800              AZERO-DS-LS-05V05 FROM WRK-DS-TS-06V06              NC1194.2
059900              GIVING WRK-DS-06V00 ROUNDED.                        NC1194.2
060000 SUB-TEST-GF-15-1.                                                NC1194.2
060100     IF       WRK-DS-06V00 EQUAL TO -100000                       NC1194.2
060200              PERFORM PASS GO TO SUB-WRITE-GF-15.                 NC1194.2
060300     GO       TO SUB-FAIL-GF-15.                                  NC1194.2
060400 SUB-DELETE-GF-15.                                                NC1194.2
060500     PERFORM  DE-LETE.                                            NC1194.2
060600     GO       TO SUB-WRITE-GF-15.                                 NC1194.2
060700 SUB-FAIL-GF-15.                                                  NC1194.2
060800     MOVE     WRK-DS-06V00 TO COMPUTED-N.                         NC1194.2
060900     MOVE    -100000 TO CORRECT-N.                                NC1194.2
061000     PERFORM  FAIL.                                               NC1194.2
061100 SUB-WRITE-GF-15.                                                 NC1194.2
061200     MOVE     "SUB-TEST-GF-15" TO PAR-NAME.                       NC1194.2
061300     PERFORM  PRINT-DETAIL.                                       NC1194.2
061400 SUB-INIT-GF-5-1.                                                 NC1194.2
061500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
061600     MOVE     "  SIZE ERROR" TO FEATURE.                          NC1194.2
061700     MOVE     -11 TO WRK-DS-02V00.                                NC1194.2
061800 SUB-TEST-GF-5-1-0.                                               NC1194.2
061900     SUBTRACT A99-DS-T-02V00 FROM WRK-DS-02V00 ON SIZE ERROR      NC1194.2
062000              PERFORM PASS GO TO SUB-WRITE-GF-5-1.                NC1194.2
062100     GO       TO SUB-FAIL-GF-5-1.                                 NC1194.2
062200 SUB-DELETE-GF-5-1.                                               NC1194.2
062300     PERFORM  DE-LETE.                                            NC1194.2
062400     GO       TO SUB-WRITE-GF-5-1.                                NC1194.2
062500 SUB-FAIL-GF-5-1.                                                 NC1194.2
062600     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1194.2
062700     PERFORM  FAIL.                                               NC1194.2
062800 SUB-WRITE-GF-5-1.                                                NC1194.2
062900     MOVE     "SUB-TEST-GF-5-1" TO PAR-NAME.                      NC1194.2
063000     PERFORM  PRINT-DETAIL.                                       NC1194.2
063100 SUB-TEST-GF-5-2.                                                 NC1194.2
063200     IF       WRK-DS-02V00 EQUAL TO -11                           NC1194.2
063300              PERFORM PASS GO TO SUB-WRITE-GF-5-2.                NC1194.2
063400*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-GF-5-1   NC1194.2
063500     GO       TO SUB-FAIL-GF-5-2.                                 NC1194.2
063600 SUB-DELETE-GF-5-2.                                               NC1194.2
063700     PERFORM  DE-LETE.                                            NC1194.2
063800     GO       TO SUB-WRITE-GF-5-2.                                NC1194.2
063900 SUB-FAIL-GF-5-2.                                                 NC1194.2
064000     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC1194.2
064100     MOVE     -11 TO CORRECT-N.                                   NC1194.2
064200     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1194.2
064300     PERFORM  FAIL.                                               NC1194.2
064400 SUB-WRITE-GF-5-2.                                                NC1194.2
064500     MOVE     "SUB-TEST-GF-5-2" TO PAR-NAME.                      NC1194.2
064600     PERFORM  PRINT-DETAIL.                                       NC1194.2
064700 SUB-INIT-GF-16-1.                                                NC1194.2
064800     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
064900     MOVE     ZERO TO WRK-DS-10V00.                               NC1194.2
065000 SUB-TEST-GF-16-1-0.                                              NC1194.2
065100     SUBTRACT A12ONES-DS-L-12V00                                  NC1194.2
065200              FROM ZERO GIVING WRK-DS-10V00 ON SIZE ERROR         NC1194.2
065300              PERFORM PASS GO TO SUB-WRITE-GF-16-1.               NC1194.2
065400     GO       TO SUB-FAIL-GF-16-1.                                NC1194.2
065500 SUB-DELETE-GF-16-1.                                              NC1194.2
065600     PERFORM  DE-LETE.                                            NC1194.2
065700     GO       TO SUB-WRITE-GF-16-1.                               NC1194.2
065800 SUB-FAIL-GF-16-1.                                                NC1194.2
065900     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1194.2
066000     PERFORM  FAIL.                                               NC1194.2
066100 SUB-WRITE-GF-16-1.                                               NC1194.2
066200     MOVE     "SUB-TEST-GF-16-1" TO PAR-NAME.                     NC1194.2
066300     PERFORM  PRINT-DETAIL.                                       NC1194.2
066400 SUB-TEST-GF-16-2.                                                NC1194.2
066500     IF       WRK-DS-10V00 EQUAL TO ZERO                          NC1194.2
066600              PERFORM PASS GO TO SUB-WRITE-GF-16-2.               NC1194.2
066700*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF ADD-TEST-GF-16-1  NC1194.2
066800     GO       TO SUB-FAIL-GF-16-2.                                NC1194.2
066900 SUB-DELETE-GF-16-2.                                              NC1194.2
067000     PERFORM  DE-LETE.                                            NC1194.2
067100     GO       TO SUB-WRITE-GF-16-2.                               NC1194.2
067200 SUB-FAIL-GF-16-2.                                                NC1194.2
067300     MOVE     WRK-DS-10V00 TO COMPUTED-14V4.                      NC1194.2
067400     MOVE     ZERO TO CORRECT-14V4.                               NC1194.2
067500     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1194.2
067600     PERFORM  FAIL.                                               NC1194.2
067700 SUB-WRITE-GF-16-2.                                               NC1194.2
067800     MOVE     "SUB-TEST-GF-16-2" TO PAR-NAME.                     NC1194.2
067900     PERFORM  PRINT-DETAIL.                                       NC1194.2
068000 SUB-INIT-GF-17-1.                                                NC1194.2
068100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
068200     MOVE     "  ROUNDED,SIZE ERROR" TO FEATURE.                  NC1194.2
068300     MOVE     ZERO TO WRK-DS-T-05V00.                             NC1194.2
068400 SUB-TEST-GF-17-1-0.                                              NC1194.2
068500     SUBTRACT 33333                                               NC1194.2
068600              A06THREES-DS-03V03                                  NC1194.2
068700              A12THREES-DS-06V06                                  NC1194.2
068800              FROM WRK-DS-T-05V00 ROUNDED ON SIZE ERROR           NC1194.2
068900              PERFORM PASS GO TO SUB-WRITE-GF-17-1.               NC1194.2
069000     GO       TO SUB-FAIL-GF-17-1.                                NC1194.2
069100 SUB-DELETE-GF-17-1.                                              NC1194.2
069200     PERFORM  DE-LETE.                                            NC1194.2
069300     GO       TO SUB-WRITE-GF-17-1.                               NC1194.2
069400 SUB-FAIL-GF-17-1.                                                NC1194.2
069500     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1194.2
069600     PERFORM  FAIL.                                               NC1194.2
069700 SUB-WRITE-GF-17-1.                                               NC1194.2
069800     MOVE     "SUB-TEST-GF-17-1" TO PAR-NAME.                     NC1194.2
069900     PERFORM  PRINT-DETAIL.                                       NC1194.2
070000 SUB-TEST-GF-17-2.                                                NC1194.2
070100     IF       WRK-DS-T-05V00 EQUAL TO ZERO                        NC1194.2
070200              PERFORM PASS GO TO SUB-WRITE-GF-17-2.               NC1194.2
070300*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-GF-17-1  NC1194.2
070400     GO       TO SUB-FAIL-GF-17-2.                                NC1194.2
070500 SUB-DELETE-GF-17-2.                                              NC1194.2
070600     PERFORM  DE-LETE.                                            NC1194.2
070700     GO       TO SUB-WRITE-GF-17-2.                               NC1194.2
070800 SUB-FAIL-GF-17-2.                                                NC1194.2
070900     MOVE     WRK-DS-T-05V00 TO COMPUTED-N.                       NC1194.2
071000     MOVE     ZERO TO CORRECT-N.                                  NC1194.2
071100     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1194.2
071200     PERFORM  FAIL.                                               NC1194.2
071300 SUB-WRITE-GF-17-2.                                               NC1194.2
071400     MOVE     "SUB-TEST-GF-17-2" TO PAR-NAME.                     NC1194.2
071500     PERFORM  PRINT-DETAIL.                                       NC1194.2
071600 SUB-INIT-GF-6-1.                                                 NC1194.2
071700     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
071800     MOVE     ZERO TO WRK-DS-TS-06V06.                            NC1194.2
071900 SUB-TEST-GF-6-1.                                                 NC1194.2
072000     SUBTRACT A12THREES-DS-06V06                                  NC1194.2
072100              333333                                              NC1194.2
072200              A06THREES-DS-03V03                                  NC1194.2
072300              -0000009                                            NC1194.2
072400              FROM WRK-DS-TS-06V06 ROUNDED ON SIZE ERROR          NC1194.2
072500              GO TO SUB-FAIL-GF-6-1.                              NC1194.2
072600     PERFORM  PASS.                                               NC1194.2
072700     GO       TO SUB-WRITE-GF-6-1.                                NC1194.2
072800 SUB-DELETE-GF-6-1.                                               NC1194.2
072900     PERFORM  DE-LETE.                                            NC1194.2
073000     GO       TO SUB-WRITE-GF-6-1.                                NC1194.2
073100 SUB-FAIL-GF-6-1.                                                 NC1194.2
073200     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1194.2
073300     PERFORM  FAIL.                                               NC1194.2
073400 SUB-WRITE-GF-6-1.                                                NC1194.2
073500     MOVE     "SUB-TEST-GF-6-1" TO PAR-NAME.                      NC1194.2
073600     PERFORM  PRINT-DETAIL.                                       NC1194.2
073700 SUB-TEST-GF-6-2.                                                 NC1194.2
073800     IF       WRK-DS-TS-06V06 EQUAL TO -666990.666333             NC1194.2
073900              PERFORM PASS GO TO SUB-WRITE-GF-6-2.                NC1194.2
074000*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-GF-6-1   NC1194.2
074100     GO       TO SUB-FAIL-GF-6-2.                                 NC1194.2
074200 SUB-DELETE-GF-6-2.                                               NC1194.2
074300     PERFORM  DE-LETE.                                            NC1194.2
074400     GO       TO SUB-WRITE-GF-6-2.                                NC1194.2
074500 SUB-FAIL-GF-6-2.                                                 NC1194.2
074600     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1194.2
074700     MOVE     -666990.666333 TO CORRECT-N.                        NC1194.2
074800     PERFORM  FAIL.                                               NC1194.2
074900 SUB-WRITE-GF-6-2.                                                NC1194.2
075000     MOVE     "SUB-TEST-GF-6-2" TO PAR-NAME.                      NC1194.2
075100     PERFORM  PRINT-DETAIL.                                       NC1194.2
075200 SUB-INIT-GF-18-1.                                                NC1194.2
075300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
075400     MOVE ZERO TO WRK-DS-T-05V00.                                 NC1194.2
075500 SUB-TEST-GF-18-1.                                                NC1194.2
075600     SUBTRACT 33333                                               NC1194.2
075700              A06THREES-DS-03V03                                  NC1194.2
075800              A12THREES-DS-06V06                                  NC1194.2
075900              FROM -1000000 GIVING WRK-DS-T-05V00                 NC1194.2
076000              ROUNDED ON SIZE ERROR                               NC1194.2
076100              PERFORM PASS GO TO SUB-WRITE-GF-18-1.               NC1194.2
076200     GO       TO SUB-FAIL-GF-18-1.                                NC1194.2
076300 SUB-DELETE-GF-18-1.                                              NC1194.2
076400     PERFORM  DE-LETE.                                            NC1194.2
076500     GO       TO SUB-WRITE-GF-18-1.                               NC1194.2
076600 SUB-FAIL-GF-18-1.                                                NC1194.2
076700     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1194.2
076800     PERFORM  FAIL.                                               NC1194.2
076900 SUB-WRITE-GF-18-1.                                               NC1194.2
077000     MOVE     "SUB-TEST-GF-18-1" TO PAR-NAME.                     NC1194.2
077100     PERFORM  PRINT-DETAIL.                                       NC1194.2
077200 SUB-TEST-GF-18-2.                                                NC1194.2
077300     IF       WRK-DS-T-05V00 EQUAL TO ZERO                        NC1194.2
077400              PERFORM PASS GO TO SUB-WRITE-GF-18-2.               NC1194.2
077500*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-GF-18-1  NC1194.2
077600     GO       TO SUB-FAIL-GF-18-2.                                NC1194.2
077700 SUB-DELETE-GF-18-2.                                              NC1194.2
077800     PERFORM  DE-LETE.                                            NC1194.2
077900     GO       TO SUB-WRITE-GF-18-2.                               NC1194.2
078000 SUB-FAIL-GF-18-2.                                                NC1194.2
078100     MOVE     WRK-DS-T-05V00 TO COMPUTED-N.                       NC1194.2
078200     MOVE     ZERO TO CORRECT-N.                                  NC1194.2
078300     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1194.2
078400     PERFORM  FAIL.                                               NC1194.2
078500 SUB-WRITE-GF-18-2.                                               NC1194.2
078600     MOVE     "SUB-TEST-GF-18-2" TO PAR-NAME.                     NC1194.2
078700     PERFORM  PRINT-DETAIL.                                       NC1194.2
078800 SUB-INIT-GF-19-1.                                                NC1194.2
078900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
079000     MOVE     ZERO TO WRK-DS-TS-06V06.                            NC1194.2
079100 SUB-TEST-GF-19-1.                                                NC1194.2
079200     SUBTRACT A12THREES-DS-06V06                                  NC1194.2
079300              333333                                              NC1194.2
079400              A06THREES-DS-03V03                                  NC1194.2
079500              -.0000009 FROM 0000000                              NC1194.2
079600              GIVING WRK-DS-TS-06V06 ROUNDED ON SIZE ERROR        NC1194.2
079700              GO TO SUB-FAIL-GF-19-1.                             NC1194.2
079800     PERFORM  PASS.                                               NC1194.2
079900     GO       TO SUB-WRITE-GF-19-1.                               NC1194.2
080000 SUB-DELETE-GF-19-1.                                              NC1194.2
080100     PERFORM  DE-LETE.                                            NC1194.2
080200     GO       TO SUB-WRITE-GF-19-1.                               NC1194.2
080300 SUB-FAIL-GF-19-1.                                                NC1194.2
080400     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1194.2
080500     PERFORM  FAIL.                                               NC1194.2
080600 SUB-WRITE-GF-19-1.                                               NC1194.2
080700     MOVE     "SUB-TEST-GF-19-1" TO PAR-NAME.                     NC1194.2
080800     PERFORM  PRINT-DETAIL.                                       NC1194.2
080900 SUB-TEST-GF-19-2.                                                NC1194.2
081000     IF       WRK-DS-TS-06V06 EQUAL TO -666999.666332             NC1194.2
081100              PERFORM PASS GO TO SUB-WRITE-GF-19-2.               NC1194.2
081200*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-19-1     NC1194.2
081300     GO       TO SUB-FAIL-GF-19-2.                                NC1194.2
081400 SUB-DELETE-GF-19-2.                                              NC1194.2
081500     PERFORM  DE-LETE.                                            NC1194.2
081600     GO       TO SUB-WRITE-GF-19-2.                               NC1194.2
081700 SUB-FAIL-GF-19-2.                                                NC1194.2
081800     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1194.2
081900     MOVE     -666999.666332 TO CORRECT-N.                        NC1194.2
082000     PERFORM  FAIL.                                               NC1194.2
082100 SUB-WRITE-GF-19-2.                                               NC1194.2
082200     MOVE     "SUB-TEST-GF-19-2" TO PAR-NAME.                     NC1194.2
082300     PERFORM  PRINT-DETAIL.                                       NC1194.2
082400 SUB-INIT-GF-20.                                                  NC1194.2
082500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
082600     MOVE     "  SERIES" TO FEATURE.                              NC1194.2
082700     MOVE     ZERO TO WRK-DS-03V10.                               NC1194.2
082800 SUB-TEST-GF-20.                                                  NC1194.2
082900     SUBTRACT A99-DS-T-02V00                                      NC1194.2
083000              A03ONES-DS-02V01                                    NC1194.2
083100              A06ONES-DS-TS-03V03                                 NC1194.2
083200              A08TWOS-DS-02V06                                    NC1194.2
083300              -1.1111111                                          NC1194.2
083400              +.11111111                                          NC1194.2
083500              A01ONES-DS-LS-P0801 FROM 0000.000000                NC1194.2
083600              GIVING WRK-DS-03V10.                                NC1194.2
083700     IF       WRK-DS-03V10 EQUAL TO -242.4332220110               NC1194.2
083800              PERFORM PASS GO TO SUB-WRITE-GF-20.                 NC1194.2
083900     GO       TO SUB-FAIL-GF-20.                                  NC1194.2
084000 SUB-DELETE-GF-20.                                                NC1194.2
084100     PERFORM  DE-LETE.                                            NC1194.2
084200     GO       TO SUB-WRITE-GF-20.                                 NC1194.2
084300 SUB-FAIL-GF-20.                                                  NC1194.2
084400     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1194.2
084500     MOVE     -242.4332220110 TO CORRECT-4V14.                    NC1194.2
084600     PERFORM  FAIL.                                               NC1194.2
084700 SUB-WRITE-GF-20.                                                 NC1194.2
084800     MOVE     "SUB-TEST-GF-20" TO PAR-NAME.                       NC1194.2
084900     PERFORM  PRINT-DETAIL.                                       NC1194.2
085000 SUB-INIT-GF-21.                                                  NC1194.2
085100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
085200     MOVE     ZERO TO WRK-DS-03V10.                               NC1194.2
085300 SUB-TEST-GF-21-0.                                                NC1194.2
085400     SUBTRACT A01ONES-DS-LS-P0801                                 NC1194.2
085500              +.11111111                                          NC1194.2
085600              -1.1111111                                          NC1194.2
085700              A08TWOS-DS-02V06                                    NC1194.2
085800              A06ONES-DS-TS-03V03                                 NC1194.2
085900              A03ONES-DS-02V01                                    NC1194.2
086000              A99-DS-T-02V00 FROM 0000.000000 GIVING WRK-DS-03V10.NC1194.2
086100 SUB-TEST-GF-21-1.                                                NC1194.2
086200     IF       WRK-DS-03V10 EQUAL TO -242.4332220110               NC1194.2
086300              PERFORM PASS GO TO SUB-WRITE-GF-21.                 NC1194.2
086400     GO       TO SUB-FAIL-GF-21.                                  NC1194.2
086500 SUB-DELETE-GF-21.                                                NC1194.2
086600     PERFORM  DE-LETE.                                            NC1194.2
086700     GO       TO SUB-WRITE-GF-21.                                 NC1194.2
086800 SUB-FAIL-GF-21.                                                  NC1194.2
086900     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1194.2
087000     MOVE     -242.4332220110 TO CORRECT-4V14.                    NC1194.2
087100     PERFORM  FAIL.                                               NC1194.2
087200 SUB-WRITE-GF-21.                                                 NC1194.2
087300     MOVE     "SUB-TEST-GF-21" TO PAR-NAME.                       NC1194.2
087400     PERFORM  PRINT-DETAIL.                                       NC1194.2
087500 SUB-INIT-GF-22.                                                  NC1194.2
087600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
087700     MOVE     ZERO TO WRK-DS-03V10.                               NC1194.2
087800 SUB-TEST-GF-22-0.                                                NC1194.2
087900     SUBTRACT A08TWOS-DS-02V06                                    NC1194.2
088000              A99-DS-T-02V00                                      NC1194.2
088100              -1.1111111                                          NC1194.2
088200              A03ONES-DS-02V01                                    NC1194.2
088300              A01ONES-DS-LS-P0801                                 NC1194.2
088400              +.11111111                                          NC1194.2
088500              A06ONES-DS-TS-03V03 FROM 0000.000000                NC1194.2
088600              GIVING WRK-DS-03V10.                                NC1194.2
088700 SUB-TEST-GF-22-1.                                                NC1194.2
088800     IF       WRK-DS-03V10 EQUAL TO -242.4332220110               NC1194.2
088900              PERFORM PASS GO TO SUB-WRITE-GF-22.                 NC1194.2
089000     GO       TO SUB-FAIL-GF-22.                                  NC1194.2
089100 SUB-DELETE-GF-22.                                                NC1194.2
089200     PERFORM  DE-LETE.                                            NC1194.2
089300     GO       TO SUB-WRITE-GF-22.                                 NC1194.2
089400 SUB-FAIL-GF-22.                                                  NC1194.2
089500     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1194.2
089600     MOVE     -242.4332220110 TO CORRECT-4V14.                    NC1194.2
089700     PERFORM  FAIL.                                               NC1194.2
089800 SUB-WRITE-GF-22.                                                 NC1194.2
089900     MOVE     "SUB-TEST-GF-22" TO PAR-NAME.                       NC1194.2
090000     PERFORM  PRINT-DETAIL.                                       NC1194.2
090100 SUB-INIT-GF-7.                                                   NC1194.2
090200     MOVE     "  COMP VS. DISPLAY" TO FEATURE.                    NC1194.2
090300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
090400     MOVE     ZERO TO WRK-CS-18V00.                               NC1194.2
090500 SUB-TEST-GF-7-0.                                                 NC1194.2
090600     SUBTRACT A18ONES-DS-TS-18V00 FROM WRK-CS-18V00.              NC1194.2
090700 SUB-TEST-GF-7-1.                                                 NC1194.2
090800     IF       WRK-CS-18V00 EQUAL TO -111111111111111111           NC1194.2
090900              PERFORM PASS GO TO SUB-WRITE-GF-7.                  NC1194.2
091000     GO       TO SUB-FAIL-GF-7.                                   NC1194.2
091100 SUB-DELETE-GF-7.                                                 NC1194.2
091200     PERFORM  DE-LETE.                                            NC1194.2
091300     GO       TO SUB-WRITE-GF-7.                                  NC1194.2
091400 SUB-FAIL-GF-7.                                                   NC1194.2
091500     MOVE     WRK-CS-18V00 TO COMPUTED-18V0.                      NC1194.2
091600     MOVE     -111111111111111111 TO CORRECT-18V0.                NC1194.2
091700     PERFORM  FAIL.                                               NC1194.2
091800 SUB-WRITE-GF-7.                                                  NC1194.2
091900     MOVE     "SUB-TEST-GF-7" TO PAR-NAME.                        NC1194.2
092000     PERFORM  PRINT-DETAIL.                                       NC1194.2
092100 SUB-INIT-GF-8.                                                   NC1194.2
092200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
092300     MOVE     ZERO TO WRK-DS-T-18V00.                             NC1194.2
092400 SUB-TEST-GF-8-0.                                                 NC1194.2
092500     SUBTRACT A18ONES-CS-18V00 FROM WRK-DS-T-18V00.               NC1194.2
092600 SUB-TEST-GF-8-1.                                                 NC1194.2
092700     IF       WRK-DS-T-18V00 EQUAL TO -111111111111111111         NC1194.2
092800              PERFORM PASS GO TO SUB-WRITE-GF-8.                  NC1194.2
092900     GO       TO SUB-FAIL-GF-8.                                   NC1194.2
093000 SUB-DELETE-GF-8.                                                 NC1194.2
093100     PERFORM  DE-LETE.                                            NC1194.2
093200     GO       TO SUB-WRITE-GF-8.                                  NC1194.2
093300 SUB-FAIL-GF-8.                                                   NC1194.2
093400     MOVE     WRK-DS-T-18V00 TO COMPUTED-18V0.                    NC1194.2
093500     MOVE     -111111111111111111 TO CORRECT-18V0.                NC1194.2
093600     PERFORM  FAIL.                                               NC1194.2
093700 SUB-WRITE-GF-8.                                                  NC1194.2
093800     MOVE     "SUB-TEST-GF-8" TO PAR-NAME.                        NC1194.2
093900     PERFORM  PRINT-DETAIL.                                       NC1194.2
094000 SUB-INIT-GF-9.                                                   NC1194.2
094100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
094200     MOVE    1 TO SUBTR-1.                                        NC1194.2
094300     MOVE   -1 TO SUBTR-3.                                        NC1194.2
094400     MOVE   99 TO SUBTR-7.                                        NC1194.2
094500 SUB-TEST-GF-9-0.                                                 NC1194.2
094600     SUBTRACT SUBTR-1 SUBTR-3 FROM SUBTR-7.                       NC1194.2
094700 SUB-TEST-GF-9-1.                                                 NC1194.2
094800     IF       SUBTR-7 EQUAL TO 99                                 NC1194.2
094900              PERFORM PASS GO TO SUB-WRITE-GF-9.                  NC1194.2
095000     GO       TO SUB-FAIL-GF-9.                                   NC1194.2
095100 SUB-DELETE-GF-9.                                                 NC1194.2
095200     PERFORM DE-LETE.                                             NC1194.2
095300     GO       TO SUB-WRITE-GF-9.                                  NC1194.2
095400 SUB-FAIL-GF-9.                                                   NC1194.2
095500     MOVE     SUBTR-7 TO COMPUTED-N.                              NC1194.2
095600     MOVE     99        TO CORRECT-N.                             NC1194.2
095700     PERFORM FAIL.                                                NC1194.2
095800 SUB-WRITE-GF-9.                                                  NC1194.2
095900     MOVE     "SUB-TEST-GF-9" TO PAR-NAME.                        NC1194.2
096000     PERFORM PRINT-DETAIL.                                        NC1194.2
096100 SUB-INIT-GF-10.                                                  NC1194.2
096200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
096300     MOVE  100 TO SUBTR-5.                                        NC1194.2
096400     MOVE   -1 TO SUBTR-3.                                        NC1194.2
096500     MOVE  100 TO SUBTR-10.                                       NC1194.2
096600 SUB-TEST-GF-10-0.                                                NC1194.2
096700     SUBTRACT SUBTR-5 -98 SUBTR-3 -1 FROM SUBTR-10.               NC1194.2
096800 SUB-TEST-GF-10-1.                                                NC1194.2
096900     IF       SUBTR-10 EQUAL TO 100                               NC1194.2
097000              PERFORM PASS GO TO SUB-WRITE-GF-10.                 NC1194.2
097100     GO       TO SUB-FAIL-GF-10.                                  NC1194.2
097200 SUB-DELETE-GF-10.                                                NC1194.2
097300     PERFORM DE-LETE.                                             NC1194.2
097400     GO       TO SUB-WRITE-GF-10.                                 NC1194.2
097500 SUB-FAIL-GF-10.                                                  NC1194.2
097600     MOVE     SUBTR-10  TO COMPUTED-N.                            NC1194.2
097700     MOVE     100       TO CORRECT-N.                             NC1194.2
097800     PERFORM FAIL.                                                NC1194.2
097900 SUB-WRITE-GF-10.                                                 NC1194.2
098000     MOVE     "SUB-TEST-GF-10" TO PAR-NAME.                       NC1194.2
098100     PERFORM PRINT-DETAIL.                                        NC1194.2
098200 SUB-INIT-GF-23.                                                  NC1194.2
098300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
098400     MOVE  100 TO SUBTR-5.                                        NC1194.2
098500     MOVE .001 TO SUBTR-4.                                        NC1194.2
098600     MOVE   99 TO SUBTR-2.                                        NC1194.2
098700     MOVE    0 TO SUBTR-11.                                       NC1194.2
098800 SUB-TEST-GF-23-0.                                                NC1194.2
098900     SUBTRACT SUBTR-4 SUBTR-5 .499 FROM SUBTR-2 GIVING SUBTR-11.  NC1194.2
099000 SUB-TEST-GF-23-1.                                                NC1194.2
099100     IF       SUBTR-11 EQUAL TO -1.5                              NC1194.2
099200              PERFORM PASS GO TO SUB-WRITE-GF-23.                 NC1194.2
099300     GO       TO SUB-FAIL-GF-23.                                  NC1194.2
099400 SUB-DELETE-GF-23.                                                NC1194.2
099500     PERFORM DE-LETE.                                             NC1194.2
099600     GO       TO SUB-WRITE-GF-23.                                 NC1194.2
099700 SUB-FAIL-GF-23.                                                  NC1194.2
099800     MOVE     SUBTR-11  TO COMPUTED-N.                            NC1194.2
099900     MOVE     -1.5      TO CORRECT-N.                             NC1194.2
100000     PERFORM FAIL.                                                NC1194.2
100100 SUB-WRITE-GF-23.                                                 NC1194.2
100200     MOVE     "SUB-TEST-GF-23" TO PAR-NAME.                       NC1194.2
100300     PERFORM PRINT-DETAIL.                                        NC1194.2
100400 SUB-INIT-GF-11.                                                  NC1194.2
100500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
100600     MOVE    1 TO SUBTR-6.                                        NC1194.2
100700     MOVE .001 TO SUBTR-4.                                        NC1194.2
100800 SUB-TEST-GF-11-0.                                                NC1194.2
100900     SUBTRACT SUBTR-4 FROM SUBTR-6 ROUNDED.                       NC1194.2
101000 SUB-TEST-GF-11-1.                                                NC1194.2
101100     IF       SUBTR-6 EQUAL TO 1                                  NC1194.2
101200              PERFORM PASS GO TO SUB-WRITE-GF-11.                 NC1194.2
101300     GO       TO SUB-FAIL-GF-11.                                  NC1194.2
101400 SUB-DELETE-GF-11.                                                NC1194.2
101500     PERFORM DE-LETE.                                             NC1194.2
101600     GO       TO SUB-WRITE-GF-11.                                 NC1194.2
101700 SUB-FAIL-GF-11.                                                  NC1194.2
101800     MOVE     SUBTR-6   TO COMPUTED-N.                            NC1194.2
101900     MOVE     1         TO CORRECT-N.                             NC1194.2
102000     PERFORM FAIL.                                                NC1194.2
102100 SUB-WRITE-GF-11.                                                 NC1194.2
102200     MOVE     "SUB-TEST-GF-11" TO PAR-NAME.                       NC1194.2
102300     PERFORM PRINT-DETAIL.                                        NC1194.2
102400 SUB-INIT-GF-12.                                                  NC1194.2
102500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
102600     MOVE -9.99 TO SUBTR-8.                                       NC1194.2
102700 SUB-TEST-GF-12-0.                                                NC1194.2
102800     SUBTRACT .01 FROM SUBTR-8 ON SIZE ERROR                      NC1194.2
102900              PERFORM PASS GO TO SUB-WRITE-GF-12-1.               NC1194.2
103000     GO       TO SUB-FAIL-GF-12-1.                                NC1194.2
103100 SUB-DELETE-GF-12-1.                                              NC1194.2
103200     PERFORM DE-LETE.                                             NC1194.2
103300     GO       TO SUB-WRITE-GF-12-1.                               NC1194.2
103400 SUB-FAIL-GF-12-1.                                                NC1194.2
103500     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1194.2
103600     PERFORM FAIL.                                                NC1194.2
103700 SUB-WRITE-GF-12-1.                                               NC1194.2
103800     MOVE     "SUB-TEST-GF-12-1" TO PAR-NAME.                     NC1194.2
103900     PERFORM PRINT-DETAIL.                                        NC1194.2
104000 SUB-TEST-GF-12-2.                                                NC1194.2
104100     IF       SUBTR-8 EQUAL TO -9.99                              NC1194.2
104200              PERFORM PASS GO TO SUB-WRITE-GF-12-2.               NC1194.2
104300*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-GF-12-1  NC1194.2
104400     GO       TO SUB-FAIL-GF-12-2.                                NC1194.2
104500 SUB-DELETE-GF-12-2.                                              NC1194.2
104600     PERFORM DE-LETE.                                             NC1194.2
104700     GO       TO SUB-WRITE-GF-12-2.                               NC1194.2
104800 SUB-FAIL-GF-12-2.                                                NC1194.2
104900     MOVE     SUBTR-8   TO COMPUTED-N.                            NC1194.2
105000     MOVE     -9.99     TO CORRECT-N.                             NC1194.2
105100     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1194.2
105200     PERFORM FAIL.                                                NC1194.2
105300 SUB-WRITE-GF-12-2.                                               NC1194.2
105400     MOVE     "SUB-TEST-GF-12-2" TO PAR-NAME.                     NC1194.2
105500     PERFORM PRINT-DETAIL.                                        NC1194.2
105600 SUB-INIT-GF-24.                                                  NC1194.2
105700     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
105800     MOVE    1  TO SUBTR-1.                                       NC1194.2
105900     MOVE   -1  TO SUBTR-3.                                       NC1194.2
106000     MOVE  100  TO SUBTR-5.                                       NC1194.2
106100     MOVE   99  TO SUBTR-7.                                       NC1194.2
106200 SUB-TEST-GF-24-1.                                                NC1194.2
106300     SUBTRACT SUBTR-1 SUBTR-3 FROM SUBTR-5 GIVING SUBTR-7 ON      NC1194.2
106400              SIZE ERROR                                          NC1194.2
106500              PERFORM PASS GO TO SUB-WRITE-GF-24-1.               NC1194.2
106600     GO       TO SUB-FAIL-GF-24-1.                                NC1194.2
106700 SUB-DELETE-GF-24-1.                                              NC1194.2
106800     PERFORM DE-LETE.                                             NC1194.2
106900     GO       TO SUB-WRITE-GF-24-1.                               NC1194.2
107000 SUB-FAIL-GF-24-1.                                                NC1194.2
107100     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1194.2
107200     PERFORM FAIL.                                                NC1194.2
107300 SUB-WRITE-GF-24-1.                                               NC1194.2
107400     MOVE     "SUB-TEST-GF-24-1" TO PAR-NAME.                     NC1194.2
107500     PERFORM PRINT-DETAIL.                                        NC1194.2
107600 SUB-TEST-GF-24-2.                                                NC1194.2
107700     IF       SUBTR-7 EQUAL  TO 99                                NC1194.2
107800              PERFORM PASS GO TO SUB-WRITE-GF-24-2.               NC1194.2
107900*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SUB-TEST-GF-24-1  NC1194.2
108000     GO       TO SUB-FAIL-GF-24-2.                                NC1194.2
108100 SUB-DELETE-GF-24-2.                                              NC1194.2
108200     PERFORM DE-LETE.                                             NC1194.2
108300     GO       TO SUB-WRITE-GF-24-2.                               NC1194.2
108400 SUB-FAIL-GF-24-2.                                                NC1194.2
108500     MOVE     SUBTR-7   TO COMPUTED-N.                            NC1194.2
108600     MOVE      99       TO CORRECT-N.                             NC1194.2
108700     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1194.2
108800     PERFORM FAIL.                                                NC1194.2
108900 SUB-WRITE-GF-24-2.                                               NC1194.2
109000     MOVE     "SUB-TEST-GF-24-2" TO PAR-NAME.                     NC1194.2
109100     PERFORM PRINT-DETAIL.                                        NC1194.2
109200 SUB-INIT-GF-25.                                                  NC1194.2
109300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
109400     MOVE    -999999999999999999 TO MINUS-NAME1.                  NC1194.2
109500     MOVE    -999999999999999999 TO MINUS-NAME2.                  NC1194.2
109600     MOVE    +999999999999999999 TO PLUS-NAME1.                   NC1194.2
109700     MOVE    +999999999999999999 TO PLUS-NAME1.                   NC1194.2
109800     MOVE     +1 TO EVEN-NAME1.                                   NC1194.2
109900     MOVE    0 TO WHOLE-FIELD.                                    NC1194.2
110000     MOVE SPACE TO SIZE-ERR.                                      NC1194.2
110100 SUB-TEST-GF-25-0.                                                NC1194.2
110200     SUBTRACT MINUS-NAME1  MINUS-NAME2  -34  -1  PLUS-NAME1       NC1194.2
110300         PLUS-NAME2  EVEN-NAME1  35 FROM EVEN-NAME1 GIVING        NC1194.2
110400         WHOLE-FIELD                                              NC1194.2
110500         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1194.2
110600 SUB-TEST-GF-25-1.                                                NC1194.2
110700     IF WHOLE-FIELD EQUAL TO 0                                    NC1194.2
110800         PERFORM PASS                                             NC1194.2
110900         GO TO SUB-WRITE-GF-25-1.                                 NC1194.2
111000     MOVE WHOLE-FIELD TO COMPUTED-18V0.                           NC1194.2
111100     MOVE 0 TO CORRECT-18V0.                                      NC1194.2
111200     PERFORM FAIL.                                                NC1194.2
111300     GO TO SUB-WRITE-GF-25-1.                                     NC1194.2
111400 SUB-DELETE-GF-25-1.                                              NC1194.2
111500     PERFORM DE-LETE.                                             NC1194.2
111600 SUB-WRITE-GF-25-1.                                               NC1194.2
111700     MOVE "SUB-TEST-GF-25-1" TO PAR-NAME.                         NC1194.2
111800     PERFORM PRINT-DETAIL.                                        NC1194.2
111900 SUB-TEST-GF-25-2.                                                NC1194.2
112000     IF SIZE-ERR EQUAL TO "1"                                     NC1194.2
112100         PERFORM FAIL                                             NC1194.2
112200         MOVE SPACE TO CORRECT-A                                  NC1194.2
112300         MOVE 1 TO COMPUTED-A                                     NC1194.2
112400         MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK      NC1194.2
112500         GO TO SUB-WRITE-GF-25-2.                                 NC1194.2
112600     PERFORM PASS.                                                NC1194.2
112700     GO TO SUB-WRITE-GF-25-2.                                     NC1194.2
112800 SUB-DELETE-GF-25-2.                                              NC1194.2
112900     PERFORM DE-LETE.                                             NC1194.2
113000 SUB-WRITE-GF-25-2.                                               NC1194.2
113100     MOVE "SUB-TEST-GF-25-2" TO PAR-NAME.                         NC1194.2
113200     PERFORM PRINT-DETAIL.                                        NC1194.2
113300 SUB-INIT-GF-26.                                                  NC1194.2
113400     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1194.2
113500     MOVE SPACE TO SIZE-ERR.                                      NC1194.2
113600     MOVE   -.999999999999999999 TO MINUS-NAME3.                  NC1194.2
113700     MOVE   -.999999999999999999 TO MINUS-NAME4.                  NC1194.2
113800     MOVE   +.999999999999999999 TO PLUS-NAME3.                   NC1194.2
113900     MOVE   +.999999999999999999 TO PLUS-NAME4.                   NC1194.2
114000     MOVE   +1 TO EVEN-NAME2.                                     NC1194.2
114100     MOVE    0 TO DECMAL-FIELD.                                   NC1194.2
114200 SUB-TEST-GF-26-0.                                                NC1194.2
114300     SUBTRACT MINUS-NAME3  MINUS-NAME4  -.34  -.01  PLUS-NAME3    NC1194.2
114400         PLUS-NAME4  EVEN-NAME2  .35 FROM EVEN-NAME2              NC1194.2
114500         GIVING DECMAL-FIELD                                      NC1194.2
114600         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1194.2
114700 SUB-TEST-GF-26-1.                                                NC1194.2
114800     IF DECMAL-FIELD EQUAL TO .0                                  NC1194.2
114900         PERFORM PASS                                             NC1194.2
115000         GO TO SUB-WRITE-GF-26-1.                                 NC1194.2
115100     MOVE DECMAL-FIELD TO COMPUTED-0V18.                          NC1194.2
115200     MOVE .0 TO CORRECT-0V18.                                     NC1194.2
115300     PERFORM FAIL.                                                NC1194.2
115400     GO TO SUB-WRITE-GF-26-1.                                     NC1194.2
115500 SUB-DELETE-GF-26-1.                                              NC1194.2
115600     PERFORM DE-LETE.                                             NC1194.2
115700 SUB-WRITE-GF-26-1.                                               NC1194.2
115800     MOVE "SUB-TEST-GF-26-1" TO PAR-NAME.                         NC1194.2
115900     PERFORM PRINT-DETAIL.                                        NC1194.2
116000 SUB-TEST-GF-26-2.                                                NC1194.2
116100     IF SIZE-ERR EQUAL TO "1"                                     NC1194.2
116200         PERFORM FAIL                                             NC1194.2
116300         MOVE SPACE TO CORRECT-A                                  NC1194.2
116400         MOVE 1 TO COMPUTED-A                                     NC1194.2
116500         MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK      NC1194.2
116600         GO TO SUB-WRITE-GF-26-2.                                 NC1194.2
116700     PERFORM PASS.                                                NC1194.2
116800     GO TO SUB-WRITE-GF-26-2.                                     NC1194.2
116900 SUB-DELETE-GF-26-2.                                              NC1194.2
117000     PERFORM DE-LETE.                                             NC1194.2
117100 SUB-WRITE-GF-26-2.                                               NC1194.2
117200     MOVE "SUB-TEST-GF-26-2" TO PAR-NAME.                         NC1194.2
117300     PERFORM PRINT-DETAIL.                                        NC1194.2
117400 CCVS-EXIT SECTION.                                               NC1194.2
117500 CCVS-999999.                                                     NC1194.2
117600     GO TO CLOSE-FILES.                                           NC1194.2
      *END-OF,NC119A                                                                  
