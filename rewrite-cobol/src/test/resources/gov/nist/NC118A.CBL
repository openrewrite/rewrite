      *HEADER,COBOL,NC118A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1184.2
000200 PROGRAM-ID.                                                      NC1184.2
000300     NC118A.                                                      NC1184.2
000400****************************************************************  NC1184.2
000500*                                                              *  NC1184.2
000600*    VALIDATION FOR:-                                          *  NC1184.2
000700*                                                              *  NC1184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1184.2
000900*                                                              *  NC1184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1184.2
001100*                                                              *  NC1184.2
001200****************************************************************  NC1184.2
001300*                                                              *  NC1184.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1184.2
001500*                                                              *  NC1184.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1184.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1184.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1184.2
001900*                                                              *  NC1184.2
002000****************************************************************  NC1184.2
002100*                                                                 NC1184.2
002200*    PROGRAM NC118A TESTS THE USE OF THE "SIGN" CLAUSE USING      NC1184.2
002300*    FORMATS 1 AND 2 OF THE ADD STATEMENT.  ALL COMBINATIONS      NC1184.2
002400*    OF THE SIGN CLAUSE PHRASES ARE TESTED USING DATA ITEMS       NC1184.2
002500*    OF VARIOUS LENGTHS.                                          NC1184.2
002600*                                                                 NC1184.2
002700                                                                  NC1184.2
002800 ENVIRONMENT DIVISION.                                            NC1184.2
002900 CONFIGURATION SECTION.                                           NC1184.2
003000 SOURCE-COMPUTER.                                                 NC1184.2
003100     XXXXX082.                                                    NC1184.2
003200 OBJECT-COMPUTER.                                                 NC1184.2
003300     XXXXX083.                                                    NC1184.2
003400 INPUT-OUTPUT SECTION.                                            NC1184.2
003500 FILE-CONTROL.                                                    NC1184.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC1184.2
003700     XXXXX055.                                                    NC1184.2
003800 DATA DIVISION.                                                   NC1184.2
003900 FILE SECTION.                                                    NC1184.2
004000 FD  PRINT-FILE.                                                  NC1184.2
004100 01  PRINT-REC PICTURE X(120).                                    NC1184.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC1184.2
004300 WORKING-STORAGE SECTION.                                         NC1184.2
004400 77  SIZE-ERR                     PICTURE X  VALUE SPACE.         NC1184.2
004500 77  A18TWOS-DS-LS-18V00             PICTURE S9(18)               NC1184.2
004600             SIGN IS LEADING SEPARATE                             NC1184.2
004700                                  VALUE 222222222222222222.       NC1184.2
004800 77  A18ONES-DS-TS-18V00             PICTURE S9(18)               NC1184.2
004900            SIGN IS TRAILING SEPARATE                             NC1184.2
005000                                  VALUE 111111111111111111.       NC1184.2
005100 77  WRK-DS-10V00                 PICTURE S9(10) TRAILING.        NC1184.2
005200 77  A10ONES-DS-T-10V00             PICTURE S9(10)                NC1184.2
005300            SIGN TRAILING                                         NC1184.2
005400                                  VALUE 1111111111.               NC1184.2
005500 77  A05ONES-DS-L-05V00             PICTURE S9(5)                 NC1184.2
005600           SIGN LEADING                                           NC1184.2
005700                                  VALUE 11111.                    NC1184.2
005800 77  A02ONES-DS-LS-02V00             PICTURE S99                  NC1184.2
005900           LEADING SEPARATE                                       NC1184.2
006000                                  VALUE 11.                       NC1184.2
006100 77  WRK-DS-09V09                 PICTURE S9(9)V9(9) TRAILING.    NC1184.2
006200 77  WRK-DS-T-18V00 REDEFINES WRK-DS-09V09                        NC1184.2
006300                                  PICTURE S9(18) TRAILING.        NC1184.2
006400 77  A06THREES-DS-03V03           PICTURE S999V999                NC1184.2
006500                                  VALUE 333.333.                  NC1184.2
006600 77  A12THREES-DS-06V06           PICTURE S9(6)V9(6)              NC1184.2
006700                                  VALUE 333333.333333.            NC1184.2
006800 77  WRK-DS-TS-06V06                 PICTURE S9(6)V9(6)           NC1184.2
006900           SIGN IS TRAILING SEPARATE CHARACTER.                   NC1184.2
007000 77  WRK-DS-TS-12V00-S REDEFINES WRK-DS-TS-06V06                  NC1184.2
007100         TRAILING SEPARATE                                        NC1184.2
007200                                  PICTURE S9(12).                 NC1184.2
007300 77  A05ONES-DS-LS-00V05             PICTURE SV9(5)               NC1184.2
007400         LEADING SEPARATE                                         NC1184.2
007500                                  VALUE .11111.                   NC1184.2
007600 77  WRK-DS-T-05V00              PICTURE S9(5) TRAILING.          NC1184.2
007700 77  WRK-DS-T-06V00                 PICTURE S9(6) TRAILING.       NC1184.2
007800 77  WRK-DS-02V00                 PICTURE S99.                    NC1184.2
007900 77  A12ONES-DS-L-12V00             PICTURE S9(12)                NC1184.2
008000              USAGE IS DISPLAY SIGN IS LEADING                    NC1184.2
008100                                  VALUE 111111111111.             NC1184.2
008200 77  WRK-DS-03V10                 PICTURE S999V9(10).             NC1184.2
008300 77  WRK-DS-13V00-S REDEFINES WRK-DS-03V10                        NC1184.2
008400                                  PICTURE S9(13).                 NC1184.2
008500 77  A99-DS-T-02V00                 PICTURE S99                   NC1184.2
008600             USAGE IS DISPLAY SIGN IS TRAILING                    NC1184.2
008700                                  VALUE 99.                       NC1184.2
008800 77  A03ONES-DS-02V01             PICTURE S99V9                   NC1184.2
008900                                  VALUE 11.1.                     NC1184.2
009000 77  A06ONES-DS-TS-03V03             PICTURE S999V999             NC1184.2
009100             USAGE IS DISPLAY TRAILING SEPARATE                   NC1184.2
009200                                  VALUE 111.111.                  NC1184.2
009300 77  A08TWOS-DS-02V06             PICTURE S99V9(6)                NC1184.2
009400                                  VALUE 22.222222.                NC1184.2
009500 77  A01ONES-DS-LS-P0801              PICTURE SP(8)9              NC1184.2
009600             SIGN IS LEADING SEPARATE                             NC1184.2
009700                                  VALUE .000000001.               NC1184.2
009800 77  WRK-CS-18V00                 PICTURE S9(18) COMPUTATIONAL.   NC1184.2
009900 77  A18ONES-CS-18V00             PICTURE S9(18) COMPUTATIONAL    NC1184.2
010000                                  VALUE 111111111111111111.       NC1184.2
010100 77  WRK-CS-02V02                 PICTURE S99V99 COMPUTATIONAL.   NC1184.2
010200 77  A99-CS-02V00                 PICTURE S99 COMPUTATIONAL       NC1184.2
010300                                  VALUE 99.                       NC1184.2
010400 77  WRK-DS-TS-0201P            PICTURE S99P TRAILING SEPARATE.   NC1184.2
010500 77  WRK-DS-06V00                 PICTURE S9(6).                  NC1184.2
010600 77  AZERO-DS-LS-05V05               PICTURE S9(5)V9(5)           NC1184.2
010700             SIGN IS LEADING SEPARATE USAGE DISPLAY               NC1184.2
010800                                  VALUE ZERO.                     NC1184.2
010900 77  NDATA-DS-09V09               PICTURE S9(9)V9(9)              NC1184.2
011000                                  VALUE +012345678.876543210.     NC1184.2
011100 77  XDATA-XN-00018               PICTURE X(18)                   NC1184.2
011200                                  VALUE "00ABCDEFGHI  4321 ".     NC1184.2
011300 77  WRK-XN-00018                 PICTURE X(18).                  NC1184.2
011400 77  ADD-12    PICTURE PP9        VALUE .001.                     NC1184.2
011500 77  ADD-13    PICTURE 9PP        VALUE 100.                      NC1184.2
011600 77  ADD-14    PICTURE 999V999.                                   NC1184.2
011700 77  WRK-CU-18V00      PICTURE 9(18) VALUE ZERO COMPUTATIONAL.    NC1184.2
011800 77  A18EIGHTS-CS-18V00 PICTURE S9(18) VALUE -888888888888888888  NC1184.2
011900             COMPUTATIONAL.                                       NC1184.2
012000 77  A14TWOS-CS-18V00  PICTURE S9(18) VALUE -000022222222222222   NC1184.2
012100             COMPUTATIONAL.                                       NC1184.2
012200 77  A12THREES-CU-18V00 PICTURE 9(18) VALUE 000000333333333333    NC1184.2
012300             COMPUTATIONAL.                                       NC1184.2
012400 77  A16FOURS-CS-18V00 PICTURE S9(18) VALUE 004444444444444444    NC1184.2
012500             COMPUTATIONAL.                                       NC1184.2
012600 77  A18FIVES-CS-18V00 PICTURE S9(18) VALUE -555555555555555555   NC1184.2
012700             COMPUTATIONAL.                                       NC1184.2
012800 77  A18SIXES-CS-18V00 PICTURE S9(18) VALUE 666666666666666666    NC1184.2
012900             COMPUTATIONAL.                                       NC1184.2
013000 77  A12SEVENS-CU-18V00 PICTURE 9(18) VALUE 000000777777777777    NC1184.2
013100             COMPUTATIONAL.                                       NC1184.2
013200 77  WRK-DU-18V00      PICTURE 9(18) VALUE ZERO.                  NC1184.2
013300 77  A18THREES-CS-18V00 PICTURE S9(18) VALUE -333333333333333333  NC1184.2
013400             COMPUTATIONAL.                                       NC1184.2
013500 77  WRK-CS-03V00 PICTURE S999 COMPUTATIONAL SYNCHRONIZED RIGHT.  NC1184.2
013600 01  SUBTRACT-DATA SIGN IS LEADING SEPARATE DISPLAY.              NC1184.2
013700     02  SUBTR-1    PICTURE 9         VALUE 1.                    NC1184.2
013800     02  SUBTR-2    PICTURE S99       VALUE 99.                   NC1184.2
013900     02  SUBTR-3    PICTURE S9V99     VALUE -1.                   NC1184.2
014000     02  SUBTR-4    PICTURE SPP9      VALUE .001.                 NC1184.2
014100     02  SUBTR-5    PICTURE S9PP      VALUE 100.                  NC1184.2
014200     02  SUBTR-6    PICTURE 9  VALUE 1.                           NC1184.2
014300     02  SUBTR-7    PICTURE S99  VALUE 99.                        NC1184.2
014400     02  SUBTR-8    PICTURE S9V99  VALUE -9.99.                   NC1184.2
014500     02  SUBTR-10    PICTURE S999  VALUE 100.                     NC1184.2
014600     02  SUBTR-11   PICTURE S999V999.                             NC1184.2
014700 01  N-3                                PICTURE IS 99999.         NC1184.2
014800 01  N-4                                PICTURE IS 9(5)           NC1184.2
014900     VALUE IS 52800.                                              NC1184.2
015000 01  N-5                                PICTURE IS S9(9)V99       NC1184.2
015100             SIGN IS LEADING SEPARATE                             NC1184.2
015200     VALUE IS 000000001.00.                                       NC1184.2
015300 01  N-7                                PICTURE IS S9(7)V9(4)     NC1184.2
015400             SIGN IS LEADING SEPARATE CHARACTER                   NC1184.2
015500     VALUE IS 0000001.0000.                                       NC1184.2
015600 01  X-8 REDEFINES N-7                  PICTURE IS X(11).         NC1184.2
015700 01  N-10                               PICTURE IS S99999V        NC1184.2
015800     VALUE IS -00001.                                             NC1184.2
015900 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1184.2
016000 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1184.2
016100 01  N-13                               PICTURE IS 9(5)           NC1184.2
016200     VALUE IS 99999.                                              NC1184.2
016300 01  N-14                               PICTURE IS 9 VALUE IS 1.  NC1184.2
016400 01  N-15                               PICTURE IS 9(16).         NC1184.2
016500 01  N-16                               PICTURE IS S999999V99     NC1184.2
016600     VALUE IS 5.90.                                               NC1184.2
016700 01  N-17                               PICTURE IS S9(3)V99       NC1184.2
016800     VALUE IS +3.6.                                               NC1184.2
016900 01  N-18                               PICTURE IS S9(10)         NC1184.2
017000     VALUE IS -5.                                                 NC1184.2
017100 01  N-19                               PICTURE IS $9.00.         NC1184.2
017200 01  N-20                               PICTURE IS S9(9)          NC1184.2
017300     VALUE IS -999999999.                                         NC1184.2
017400 01  N-21                               PICTURE IS 9 VALUE IS 5.  NC1184.2
017500 01  N-22                               PICTURE IS 999V99         NC1184.2
017600     VALUE IS 005.55.                                             NC1184.2
017700 01  N-23                               PICTURE IS $$$.99CR.      NC1184.2
017800 01  N-25                               PICTURE IS 9 VALUE IS 1.  NC1184.2
017900 01  N-26               PICTURE 9(5).                             NC1184.2
018000 01  N-27                               PICTURE IS 9999V9         NC1184.2
018100     VALUE IS 9999.9.                                             NC1184.2
018200 01  N-28                               PICTURE IS $9999.00.      NC1184.2
018300 01  N-40                               PICTURE IS 9(7)           NC1184.2
018400     VALUE IS 7777777.                                            NC1184.2
018500 01  N-41                               PICTURE IS 9(7)           NC1184.2
018600     VALUE IS 1111111.                                            NC1184.2
018700 01  N-42                               PICTURE IS 9(3)P(4).      NC1184.2
018800 01  TRUNC-DATA.                                                  NC1184.2
018900     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1184.2
019000     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1184.2
019100     02 N-45  PICTURE S9.                                         NC1184.2
019200 01  MINUS-NAMES SIGN IS TRAILING SEPARATE CHARACTER.             NC1184.2
019300     02  MINUS-NAME1 PICTURE S9(18) VALUE -999999999999999999.    NC1184.2
019400     02  MINUS-NAME2 PICTURE S9(18) VALUE -999999999999999999.    NC1184.2
019500     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC1184.2
019600     02  PLUS-NAME1  PICTURE S9(18) VALUE +999999999999999999.    NC1184.2
019700     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC1184.2
019800     02  MINUS-NAME3 PICTURE SV9(18) VALUE -.999999999999999999.  NC1184.2
019900     02  MINUS-NAME4 PICTURE SV9(18) VALUE -.999999999999999999.  NC1184.2
020000     02  EVEN-NAME2  PICTURE SV9(18)  VALUE +.1.                  NC1184.2
020100     02  PLUS-NAME3  PICTURE SV9(18) VALUE +.999999999999999999.  NC1184.2
020200     02  PLUS-NAME4  PICTURE SV9(18) VALUE +.999999999999999999.  NC1184.2
020300     02  WHOLE-FIELD   PICTURE S9(18).                            NC1184.2
020400     02  DECMAL-FIELD  PICTURE SV9(18).                           NC1184.2
020500 01  TEST-RESULTS.                                                NC1184.2
020600     02 FILLER                   PIC X      VALUE SPACE.          NC1184.2
020700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1184.2
020800     02 FILLER                   PIC X      VALUE SPACE.          NC1184.2
020900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1184.2
021000     02 FILLER                   PIC X      VALUE SPACE.          NC1184.2
021100     02  PAR-NAME.                                                NC1184.2
021200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1184.2
021300       03  PARDOT-X              PIC X      VALUE SPACE.          NC1184.2
021400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1184.2
021500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1184.2
021600     02 RE-MARK                  PIC X(61).                       NC1184.2
021700 01  TEST-COMPUTED.                                               NC1184.2
021800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1184.2
021900     02 FILLER                   PIC X(17)  VALUE                 NC1184.2
022000            "       COMPUTED=".                                   NC1184.2
022100     02 COMPUTED-X.                                               NC1184.2
022200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1184.2
022300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1184.2
022400                                 PIC -9(9).9(9).                  NC1184.2
022500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1184.2
022600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1184.2
022700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1184.2
022800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1184.2
022900         04 COMPUTED-18V0                    PIC -9(18).          NC1184.2
023000         04 FILLER                           PIC X.               NC1184.2
023100     03 FILLER PIC X(50) VALUE SPACE.                             NC1184.2
023200 01  TEST-CORRECT.                                                NC1184.2
023300     02 FILLER PIC X(30) VALUE SPACE.                             NC1184.2
023400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1184.2
023500     02 CORRECT-X.                                                NC1184.2
023600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1184.2
023700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1184.2
023800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1184.2
023900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1184.2
024000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1184.2
024100     03      CR-18V0 REDEFINES CORRECT-A.                         NC1184.2
024200         04 CORRECT-18V0                     PIC -9(18).          NC1184.2
024300         04 FILLER                           PIC X.               NC1184.2
024400     03 FILLER PIC X(2) VALUE SPACE.                              NC1184.2
024500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1184.2
024600 01  CCVS-C-1.                                                    NC1184.2
024700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1184.2
024800-    "SS  PARAGRAPH-NAME                                          NC1184.2
024900-    "       REMARKS".                                            NC1184.2
025000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1184.2
025100 01  CCVS-C-2.                                                    NC1184.2
025200     02 FILLER                     PIC X        VALUE SPACE.      NC1184.2
025300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1184.2
025400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1184.2
025500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1184.2
025600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1184.2
025700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1184.2
025800 01  REC-CT                        PIC 99       VALUE ZERO.       NC1184.2
025900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1184.2
026000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1184.2
026100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1184.2
026200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1184.2
026300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1184.2
026400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1184.2
026500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1184.2
026600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1184.2
026700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1184.2
026800 01  CCVS-H-1.                                                    NC1184.2
026900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1184.2
027000     02  FILLER                    PIC X(42)    VALUE             NC1184.2
027100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1184.2
027200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1184.2
027300 01  CCVS-H-2A.                                                   NC1184.2
027400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1184.2
027500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1184.2
027600   02  FILLER                        PIC XXXX   VALUE             NC1184.2
027700     "4.2 ".                                                      NC1184.2
027800   02  FILLER                        PIC X(28)  VALUE             NC1184.2
027900            " COPY - NOT FOR DISTRIBUTION".                       NC1184.2
028000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1184.2
028100                                                                  NC1184.2
028200 01  CCVS-H-2B.                                                   NC1184.2
028300   02  FILLER                        PIC X(15)  VALUE             NC1184.2
028400            "TEST RESULT OF ".                                    NC1184.2
028500   02  TEST-ID                       PIC X(9).                    NC1184.2
028600   02  FILLER                        PIC X(4)   VALUE             NC1184.2
028700            " IN ".                                               NC1184.2
028800   02  FILLER                        PIC X(12)  VALUE             NC1184.2
028900     " HIGH       ".                                              NC1184.2
029000   02  FILLER                        PIC X(22)  VALUE             NC1184.2
029100            " LEVEL VALIDATION FOR ".                             NC1184.2
029200   02  FILLER                        PIC X(58)  VALUE             NC1184.2
029300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1184.2
029400 01  CCVS-H-3.                                                    NC1184.2
029500     02  FILLER                      PIC X(34)  VALUE             NC1184.2
029600            " FOR OFFICIAL USE ONLY    ".                         NC1184.2
029700     02  FILLER                      PIC X(58)  VALUE             NC1184.2
029800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1184.2
029900     02  FILLER                      PIC X(28)  VALUE             NC1184.2
030000            "  COPYRIGHT   1985 ".                                NC1184.2
030100 01  CCVS-E-1.                                                    NC1184.2
030200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1184.2
030300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1184.2
030400     02 ID-AGAIN                     PIC X(9).                    NC1184.2
030500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1184.2
030600 01  CCVS-E-2.                                                    NC1184.2
030700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1184.2
030800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1184.2
030900     02 CCVS-E-2-2.                                               NC1184.2
031000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1184.2
031100         03 FILLER                   PIC X      VALUE SPACE.      NC1184.2
031200         03 ENDER-DESC               PIC X(44)  VALUE             NC1184.2
031300            "ERRORS ENCOUNTERED".                                 NC1184.2
031400 01  CCVS-E-3.                                                    NC1184.2
031500     02  FILLER                      PIC X(22)  VALUE             NC1184.2
031600            " FOR OFFICIAL USE ONLY".                             NC1184.2
031700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1184.2
031800     02  FILLER                      PIC X(58)  VALUE             NC1184.2
031900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1184.2
032000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1184.2
032100     02 FILLER                       PIC X(15)  VALUE             NC1184.2
032200             " COPYRIGHT 1985".                                   NC1184.2
032300 01  CCVS-E-4.                                                    NC1184.2
032400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1184.2
032500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1184.2
032600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1184.2
032700     02 FILLER                       PIC X(40)  VALUE             NC1184.2
032800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1184.2
032900 01  XXINFO.                                                      NC1184.2
033000     02 FILLER                       PIC X(19)  VALUE             NC1184.2
033100            "*** INFORMATION ***".                                NC1184.2
033200     02 INFO-TEXT.                                                NC1184.2
033300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1184.2
033400       04 XXCOMPUTED                 PIC X(20).                   NC1184.2
033500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1184.2
033600       04 XXCORRECT                  PIC X(20).                   NC1184.2
033700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1184.2
033800 01  HYPHEN-LINE.                                                 NC1184.2
033900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1184.2
034000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1184.2
034100-    "*****************************************".                 NC1184.2
034200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1184.2
034300-    "******************************".                            NC1184.2
034400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1184.2
034500     "NC118A".                                                    NC1184.2
034600 PROCEDURE DIVISION.                                              NC1184.2
034700 CCVS1 SECTION.                                                   NC1184.2
034800 OPEN-FILES.                                                      NC1184.2
034900     OPEN     OUTPUT PRINT-FILE.                                  NC1184.2
035000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1184.2
035100     MOVE    SPACE TO TEST-RESULTS.                               NC1184.2
035200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1184.2
035300     GO TO CCVS1-EXIT.                                            NC1184.2
035400 CLOSE-FILES.                                                     NC1184.2
035500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1184.2
035600 TERMINATE-CCVS.                                                  NC1184.2
035700*    EXIT PROGRAM.                                                NC1184.2
035800*TERMINATE-CALL.                                                  NC1184.2
035900     STOP     RUN.                                                NC1184.2
036000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1184.2
036100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1184.2
036200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1184.2
036300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1184.2
036400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1184.2
036500 PRINT-DETAIL.                                                    NC1184.2
036600     IF REC-CT NOT EQUAL TO ZERO                                  NC1184.2
036700             MOVE "." TO PARDOT-X                                 NC1184.2
036800             MOVE REC-CT TO DOTVALUE.                             NC1184.2
036900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1184.2
037000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1184.2
037100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1184.2
037200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1184.2
037300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1184.2
037400     MOVE SPACE TO CORRECT-X.                                     NC1184.2
037500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1184.2
037600     MOVE     SPACE TO RE-MARK.                                   NC1184.2
037700 HEAD-ROUTINE.                                                    NC1184.2
037800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1184.2
037900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1184.2
038000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1184.2
038100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1184.2
038200 COLUMN-NAMES-ROUTINE.                                            NC1184.2
038300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1184.2
038400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1184.2
038500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1184.2
038600 END-ROUTINE.                                                     NC1184.2
038700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1184.2
038800 END-RTN-EXIT.                                                    NC1184.2
038900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1184.2
039000 END-ROUTINE-1.                                                   NC1184.2
039100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1184.2
039200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1184.2
039300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1184.2
039400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1184.2
039500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1184.2
039600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1184.2
039700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1184.2
039800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1184.2
039900  END-ROUTINE-12.                                                 NC1184.2
040000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1184.2
040100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1184.2
040200         MOVE "NO " TO ERROR-TOTAL                                NC1184.2
040300         ELSE                                                     NC1184.2
040400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1184.2
040500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1184.2
040600     PERFORM WRITE-LINE.                                          NC1184.2
040700 END-ROUTINE-13.                                                  NC1184.2
040800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1184.2
040900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1184.2
041000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1184.2
041100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1184.2
041200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1184.2
041300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1184.2
041400          MOVE "NO " TO ERROR-TOTAL                               NC1184.2
041500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1184.2
041600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1184.2
041700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1184.2
041800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1184.2
041900 WRITE-LINE.                                                      NC1184.2
042000     ADD 1 TO RECORD-COUNT.                                       NC1184.2
042100*    IF RECORD-COUNT GREATER 42                                   NC1184.2
042200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1184.2
042300*        MOVE SPACE TO DUMMY-RECORD                               NC1184.2
042400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1184.2
042500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1184.2
042600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1184.2
042700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1184.2
042800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1184.2
042900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1184.2
043000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1184.2
043100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1184.2
043200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1184.2
043300*        MOVE ZERO TO RECORD-COUNT.                               NC1184.2
043400     PERFORM WRT-LN.                                              NC1184.2
043500 WRT-LN.                                                          NC1184.2
043600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1184.2
043700     MOVE SPACE TO DUMMY-RECORD.                                  NC1184.2
043800 BLANK-LINE-PRINT.                                                NC1184.2
043900     PERFORM WRT-LN.                                              NC1184.2
044000 FAIL-ROUTINE.                                                    NC1184.2
044100     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1184.2
044200            GO TO FAIL-ROUTINE-WRITE.                             NC1184.2
044300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1184.2
044400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1184.2
044500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1184.2
044600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1184.2
044700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1184.2
044800     GO TO  FAIL-ROUTINE-EX.                                      NC1184.2
044900 FAIL-ROUTINE-WRITE.                                              NC1184.2
045000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1184.2
045100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1184.2
045200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1184.2
045300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1184.2
045400 FAIL-ROUTINE-EX. EXIT.                                           NC1184.2
045500 BAIL-OUT.                                                        NC1184.2
045600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1184.2
045700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1184.2
045800 BAIL-OUT-WRITE.                                                  NC1184.2
045900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1184.2
046000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1184.2
046100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1184.2
046200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1184.2
046300 BAIL-OUT-EX. EXIT.                                               NC1184.2
046400 CCVS1-EXIT.                                                      NC1184.2
046500     EXIT.                                                        NC1184.2
046600 SECT-NC118A-001 SECTION.                                         NC1184.2
046700 SIG-INIT-GF-1.                                                   NC1184.2
046800     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
046900     MOVE   "ADD " TO FEATURE.                                    NC1184.2
047000     MOVE    000000001.00   TO N-5.                               NC1184.2
047100     MOVE    0000001.0000   TO N-7.                               NC1184.2
047200 SIG-TEST-GF-1-0.                                                 NC1184.2
047300     ADD     N-5 TO N-7.                                          NC1184.2
047400 SIG-TEST-GF-1-1.                                                 NC1184.2
047500     IF      N-7 = 2                                              NC1184.2
047600             PERFORM PASS GO TO SIG-WRITE-GF-1.                   NC1184.2
047700     GO      TO SIG-FAIL-GF-1.                                    NC1184.2
047800 SIG-DELETE-GF-1.                                                 NC1184.2
047900     PERFORM DE-LETE.                                             NC1184.2
048000     GO      TO SIG-WRITE-GF-1.                                   NC1184.2
048100 SIG-FAIL-GF-1.                                                   NC1184.2
048200     MOVE    N-7 TO COMPUTED-N                                    NC1184.2
048300     MOVE    2   TO CORRECT-N.                                    NC1184.2
048400     PERFORM  FAIL.                                               NC1184.2
048500 SIG-WRITE-GF-1.                                                  NC1184.2
048600     MOVE     "SIG-TEST-GF-1" TO PAR-NAME.                        NC1184.2
048700     PERFORM  PRINT-DETAIL.                                       NC1184.2
048800 SIG-INIT-GF-2.                                                   NC1184.2
048900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
049000     MOVE "ADD ---" TO FEATURE.                                   NC1184.2
049100     PERFORM  PRINT-DETAIL.                                       NC1184.2
049200     MOVE "  TO" TO FEATURE.                                      NC1184.2
049300     MOVE     A18TWOS-DS-LS-18V00 TO WRK-DS-T-18V00.              NC1184.2
049400 SIG-TEST-GF-2-0.                                                 NC1184.2
049500     ADD      A18ONES-DS-TS-18V00 TO WRK-DS-T-18V00.              NC1184.2
049600 SIG-TEST-GF-2-1.                                                 NC1184.2
049700     IF       WRK-DS-T-18V00 EQUAL TO 333333333333333333          NC1184.2
049800              PERFORM PASS GO TO SIG-WRITE-GF-2.                  NC1184.2
049900     GO       TO SIG-FAIL-GF-2.                                   NC1184.2
050000 SIG-DELETE-GF-2.                                                 NC1184.2
050100     PERFORM  DE-LETE.                                            NC1184.2
050200     GO       TO SIG-WRITE-GF-2.                                  NC1184.2
050300 SIG-FAIL-GF-2.                                                   NC1184.2
050400     MOVE     WRK-DS-T-18V00 TO COMPUTED-18V0.                    NC1184.2
050500     MOVE     333333333333333333   TO CORRECT-18V0.               NC1184.2
050600     PERFORM  FAIL.                                               NC1184.2
050700 SIG-WRITE-GF-2.                                                  NC1184.2
050800     MOVE     "SIG-TEST-GF-2" TO PAR-NAME.                        NC1184.2
050900     PERFORM  PRINT-DETAIL.                                       NC1184.2
051000 SIG-INIT-GF-3.                                                   NC1184.2
051100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
051200     MOVE     ZERO TO WRK-DS-10V00.                               NC1184.2
051300 SIG-TEST-GF-3-0.                                                 NC1184.2
051400     ADD      A10ONES-DS-T-10V00  A05ONES-DS-L-05V00              NC1184.2
051500                     TO  WRK-DS-10V00.                            NC1184.2
051600 SIG-TEST-GF-3-1.                                                 NC1184.2
051700     IF       WRK-DS-10V00 EQUAL TO 1111122222                    NC1184.2
051800              PERFORM PASS GO TO SIG-WRITE-GF-3.                  NC1184.2
051900     GO       TO SIG-FAIL-GF-3.                                   NC1184.2
052000 SIG-DELETE-GF-3.                                                 NC1184.2
052100     PERFORM  DE-LETE.                                            NC1184.2
052200     GO       TO SIG-WRITE-GF-3.                                  NC1184.2
052300 SIG-FAIL-GF-3.                                                   NC1184.2
052400     MOVE     WRK-DS-10V00 TO COMPUTED-18V0.                      NC1184.2
052500     MOVE     1111122222   TO CORRECT-18V0.                       NC1184.2
052600     PERFORM  FAIL.                                               NC1184.2
052700 SIG-WRITE-GF-3.                                                  NC1184.2
052800     MOVE     "SIG-TEST-GF-3" TO PAR-NAME.                        NC1184.2
052900     PERFORM  PRINT-DETAIL.                                       NC1184.2
053000 SIG-INIT-GF-4.                                                   NC1184.2
053100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
053200     MOVE     ZERO TO WRK-DS-10V00.                               NC1184.2
053300 SIG-TEST-GF-4-0.                                                 NC1184.2
053400     ADD      A02ONES-DS-LS-02V00                                 NC1184.2
053500              A10ONES-DS-T-10V00                                  NC1184.2
053600              A05ONES-DS-L-05V00 TO WRK-DS-10V00.                 NC1184.2
053700 SIG-TEST-GF-4-1.                                                 NC1184.2
053800     IF       WRK-DS-10V00 EQUAL TO 1111122233                    NC1184.2
053900              PERFORM PASS GO TO SIG-WRITE-GF-4.                  NC1184.2
054000     GO       TO SIG-FAIL-GF-4.                                   NC1184.2
054100 SIG-DELETE-GF-4.                                                 NC1184.2
054200     PERFORM  DE-LETE.                                            NC1184.2
054300     GO       TO SIG-WRITE-GF-4.                                  NC1184.2
054400 SIG-FAIL-GF-4.                                                   NC1184.2
054500     MOVE     WRK-DS-10V00 TO COMPUTED-18V0.                      NC1184.2
054600     MOVE     1111122233   TO CORRECT-18V0.                       NC1184.2
054700     PERFORM  FAIL.                                               NC1184.2
054800 SIG-WRITE-GF-4.                                                  NC1184.2
054900     MOVE     "SIG-TEST-GF-4" TO PAR-NAME.                        NC1184.2
055000     PERFORM  PRINT-DETAIL.                                       NC1184.2
055100 SIG-INIT-GF-10.                                                  NC1184.2
055200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
055300     MOVE "  GIVING" TO FEATURE.                                  NC1184.2
055400     MOVE     ZERO TO WRK-DS-09V09.                               NC1184.2
055500 SIG-TEST-GF-10-0.                                                NC1184.2
055600     ADD      A06THREES-DS-03V03                                  NC1184.2
055700              A12THREES-DS-06V06 GIVING WRK-DS-09V09.             NC1184.2
055800 SIG-TEST-GF-10-1.                                                NC1184.2
055900     IF       WRK-DS-09V09 EQUAL TO 000333666.666333000           NC1184.2
056000              PERFORM PASS GO TO SIG-WRITE-GF-10.                 NC1184.2
056100     GO       TO SIG-FAIL-GF-10.                                  NC1184.2
056200 SIG-DELETE-GF-10.                                                NC1184.2
056300     PERFORM  DE-LETE.                                            NC1184.2
056400     GO       TO SIG-WRITE-GF-10.                                 NC1184.2
056500 SIG-FAIL-GF-10.                                                  NC1184.2
056600     MOVE     WRK-DS-09V09 TO COMPUTED-N.                         NC1184.2
056700     MOVE     000333666.666333000 TO CORRECT-N.                   NC1184.2
056800     PERFORM  FAIL.                                               NC1184.2
056900 SIG-WRITE-GF-10.                                                 NC1184.2
057000     MOVE     "SIG-TEST-GF-10" TO PAR-NAME.                       NC1184.2
057100     PERFORM  PRINT-DETAIL.                                       NC1184.2
057200 SIG-INIT-GF-11.                                                  NC1184.2
057300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
057400     MOVE     ZERO TO WRK-DS-TS-06V06.                            NC1184.2
057500 SIG-TEST-GF-11-0.                                                NC1184.2
057600     ADD      A05ONES-DS-L-05V00                                  NC1184.2
057700              A05ONES-DS-LS-00V05                                 NC1184.2
057800              A12THREES-DS-06V06                                  NC1184.2
057900              A06THREES-DS-03V03 GIVING WRK-DS-TS-06V06.          NC1184.2
058000 SIG-TEST-GF-11-1.                                                NC1184.2
058100     IF       WRK-DS-TS-06V06 EQUAL TO 344777.777443              NC1184.2
058200              PERFORM PASS GO TO SIG-WRITE-GF-11.                 NC1184.2
058300     GO       TO SIG-FAIL-GF-11.                                  NC1184.2
058400 SIG-DELETE-GF-11.                                                NC1184.2
058500     PERFORM  DE-LETE.                                            NC1184.2
058600     GO       TO SIG-WRITE-GF-11.                                 NC1184.2
058700 SIG-FAIL-GF-11.                                                  NC1184.2
058800     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1184.2
058900     MOVE     344777.777443 TO CORRECT-N.                         NC1184.2
059000     PERFORM  FAIL.                                               NC1184.2
059100 SIG-WRITE-GF-11.                                                 NC1184.2
059200     MOVE     "SIG-TEST-GF-11" TO PAR-NAME.                       NC1184.2
059300     PERFORM PRINT-DETAIL.                                        NC1184.2
059400 SIG-INIT-GF-5.                                                   NC1184.2
059500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
059600     MOVE "  ROUNDED" TO FEATURE.                                 NC1184.2
059700     MOVE     ZERO TO WRK-DS-T-05V00.                             NC1184.2
059800 SIG-TEST-GF-5-0.                                                 NC1184.2
059900     ADD      55554.5 TO WRK-DS-T-05V00 ROUNDED.                  NC1184.2
060000 SIG-TEST-GF-5-1.                                                 NC1184.2
060100     IF       WRK-DS-T-05V00 EQUAL TO 55555                       NC1184.2
060200              PERFORM PASS GO TO SIG-WRITE-GF-5.                  NC1184.2
060300     GO       TO SIG-FAIL-GF-5.                                   NC1184.2
060400 SIG-DELETE-GF-5.                                                 NC1184.2
060500     PERFORM  DE-LETE.                                            NC1184.2
060600     GO       TO SIG-WRITE-GF-5.                                  NC1184.2
060700 SIG-FAIL-GF-5.                                                   NC1184.2
060800     MOVE     WRK-DS-T-05V00 TO COMPUTED-N.                       NC1184.2
060900     MOVE     55555 TO CORRECT-N.                                 NC1184.2
061000     PERFORM  FAIL.                                               NC1184.2
061100 SIG-WRITE-GF-5.                                                  NC1184.2
061200     MOVE     "SIG-TEST-GF-5" TO PAR-NAME.                        NC1184.2
061300     PERFORM  PRINT-DETAIL.                                       NC1184.2
061400 SIG-INIT-GF-12.                                                  NC1184.2
061500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
061600     MOVE     ZERO TO WRK-DS-T-06V00.                             NC1184.2
061700 SIG-TEST-GF-12-0.                                                NC1184.2
061800     ADD      A05ONES-DS-LS-00V05                                 NC1184.2
061900              A12THREES-DS-06V06                                  NC1184.2
062000              A05ONES-DS-LS-00V05 GIVING WRK-DS-T-06V00 ROUNDED.  NC1184.2
062100 SIG-TEST-GF-12-1.                                                NC1184.2
062200     IF      WRK-DS-T-06V00 EQUAL TO 333334                       NC1184.2
062300     PERFORM PASS GO TO SIG-WRITE-GF-12.                          NC1184.2
062400     GO      TO SIG-FAIL-GF-12.                                   NC1184.2
062500 SIG-DELETE-GF-12.                                                NC1184.2
062600     PERFORM  DE-LETE.                                            NC1184.2
062700     GO       TO SIG-WRITE-GF-12.                                 NC1184.2
062800 SIG-FAIL-GF-12.                                                  NC1184.2
062900     MOVE    WRK-DS-T-06V00 TO COMPUTED-N.                        NC1184.2
063000     MOVE    333334 TO CORRECT-N.                                 NC1184.2
063100     PERFORM FAIL.                                                NC1184.2
063200 SIG-WRITE-GF-12.                                                 NC1184.2
063300     MOVE     "SIG-TEST-GF-12" TO PAR-NAME.                       NC1184.2
063400     PERFORM  PRINT-DETAIL.                                       NC1184.2
063500 SIG-INIT-GF-13.                                                  NC1184.2
063600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
063700     MOVE     ZERO TO WRK-DS-10V00.                               NC1184.2
063800 SIG-TEST-GF-13-1-0.                                              NC1184.2
063900     ADD      A12ONES-DS-L-12V00                                  NC1184.2
064000              ZERO GIVING WRK-DS-10V00 ON SIZE ERROR              NC1184.2
064100              PERFORM PASS GO TO SIG-WRITE-GF-13-1.               NC1184.2
064200     GO       TO SIG-FAIL-GF-13-1.                                NC1184.2
064300 SIG-DELETE-GF-13-1.                                              NC1184.2
064400     PERFORM  DE-LETE.                                            NC1184.2
064500     GO       TO SIG-WRITE-GF-13-1.                               NC1184.2
064600 SIG-FAIL-GF-13-1.                                                NC1184.2
064700     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1184.2
064800     PERFORM  FAIL.                                               NC1184.2
064900 SIG-WRITE-GF-13-1.                                               NC1184.2
065000     MOVE     "SIG-TEST-GF-13-1" TO PAR-NAME.                     NC1184.2
065100     PERFORM  PRINT-DETAIL.                                       NC1184.2
065200 SIG-TEST-GF-13-2.                                                NC1184.2
065300     IF       WRK-DS-10V00 EQUAL TO ZERO                          NC1184.2
065400              PERFORM PASS GO TO SIG-WRITE-GF-13-2.               NC1184.2
065500*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SIG-TEST-GF-13-1  NC1184.2
065600     GO       TO SIG-FAIL-GF-13-2.                                NC1184.2
065700 SIG-DELETE-GF-13-2.                                              NC1184.2
065800     PERFORM  DE-LETE.                                            NC1184.2
065900     GO       TO SIG-WRITE-GF-13-2.                               NC1184.2
066000 SIG-FAIL-GF-13-2.                                                NC1184.2
066100     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1184.2
066200     MOVE     WRK-DS-10V00 TO COMPUTED-14V4.                      NC1184.2
066300     MOVE     ZERO TO CORRECT-14V4.                               NC1184.2
066400     PERFORM  FAIL.                                               NC1184.2
066500 SIG-WRITE-GF-13-2.                                               NC1184.2
066600     MOVE     "SIG-TEST-GF-13-2" TO PAR-NAME.                     NC1184.2
066700     PERFORM  PRINT-DETAIL.                                       NC1184.2
066800 SIG-INIT-GF-6.                                                   NC1184.2
066900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
067000     MOVE "  ROUNDED,SIZE ERROR" TO FEATURE.                      NC1184.2
067100     MOVE     ZERO TO WRK-DS-T-05V00.                             NC1184.2
067200 SIG-TEST-GF-6-1.                                                 NC1184.2
067300     ADD      33333                                               NC1184.2
067400              A06THREES-DS-03V03                                  NC1184.2
067500              A12THREES-DS-06V06                                  NC1184.2
067600              TO WRK-DS-T-05V00 ROUNDED ON SIZE ERROR             NC1184.2
067700              PERFORM PASS GO TO SIG-WRITE-GF-6-1.                NC1184.2
067800     GO       TO SIG-FAIL-GF-6-1.                                 NC1184.2
067900 SIG-DELETE-GF-6-1.                                               NC1184.2
068000     PERFORM  DE-LETE.                                            NC1184.2
068100     GO       TO SIG-WRITE-GF-6-1.                                NC1184.2
068200 SIG-FAIL-GF-6-1.                                                 NC1184.2
068300     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1184.2
068400     PERFORM  FAIL.                                               NC1184.2
068500 SIG-WRITE-GF-6-1.                                                NC1184.2
068600     MOVE     "SIG-TEST-GF-6-1" TO PAR-NAME.                      NC1184.2
068700     PERFORM  PRINT-DETAIL.                                       NC1184.2
068800 SIG-TEST-GF-6-2.                                                 NC1184.2
068900     IF       WRK-DS-T-05V00 EQUAL TO ZERO                        NC1184.2
069000              PERFORM PASS GO TO SIG-WRITE-GF-6-2.                NC1184.2
069100*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SIG-TEST-GF-6-1   NC1184.2
069200     GO       TO SIG-FAIL-GF-6-2.                                 NC1184.2
069300 SIG-DELETE-GF-6-2.                                               NC1184.2
069400     PERFORM  DE-LETE.                                            NC1184.2
069500     GO       TO SIG-WRITE-GF-6-2.                                NC1184.2
069600 SIG-FAIL-GF-6-2.                                                 NC1184.2
069700     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1184.2
069800     MOVE     WRK-DS-T-05V00 TO COMPUTED-N.                       NC1184.2
069900     MOVE     ZERO TO CORRECT-N.                                  NC1184.2
070000     PERFORM  FAIL.                                               NC1184.2
070100 SIG-WRITE-GF-6-2.                                                NC1184.2
070200     MOVE     "SIG-TEST-GF-6-2" TO PAR-NAME.                      NC1184.2
070300     PERFORM  PRINT-DETAIL.                                       NC1184.2
070400 SIG-INIT-GF-7.                                                   NC1184.2
070500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
070600     MOVE     ZERO TO WRK-DS-TS-06V06.                            NC1184.2
070700 SIG-TEST-GF-7-1.                                                 NC1184.2
070800     ADD      A12THREES-DS-06V06                                  NC1184.2
070900              333333                                              NC1184.2
071000              A06THREES-DS-03V03                                  NC1184.2
071100              TO WRK-DS-TS-06V06 ROUNDED ON SIZE ERROR            NC1184.2
071200              GO TO SIG-FAIL-GF-7-1.                              NC1184.2
071300     PERFORM  PASS.                                               NC1184.2
071400     GO TO SIG-WRITE-GF-7-1.                                      NC1184.2
071500 SIG-DELETE-GF-7-1.                                               NC1184.2
071600     PERFORM  DE-LETE.                                            NC1184.2
071700     GO       TO SIG-WRITE-GF-7-1.                                NC1184.2
071800 SIG-FAIL-GF-7-1.                                                 NC1184.2
071900     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1184.2
072000     PERFORM  FAIL.                                               NC1184.2
072100 SIG-WRITE-GF-7-1.                                                NC1184.2
072200     MOVE     "SIG-TEST-GF-7-1" TO PAR-NAME.                      NC1184.2
072300     PERFORM  PRINT-DETAIL.                                       NC1184.2
072400 SIG-TEST-GF-7-2.                                                 NC1184.2
072500     IF       WRK-DS-TS-06V06 EQUAL TO 666999.666333              NC1184.2
072600              PERFORM PASS GO TO SIG-WRITE-GF-7-2.                NC1184.2
072700*    NOTE     THIS TEST DEPENDS UPON THE RESULT OF SIG-TEST-GF-7-1NC1184.2
072800     GO       TO SIG-FAIL-GF-7-2.                                 NC1184.2
072900 SIG-DELETE-GF-7-2.                                               NC1184.2
073000     PERFORM  DE-LETE.                                            NC1184.2
073100     GO       TO SIG-WRITE-GF-7-2.                                NC1184.2
073200 SIG-FAIL-GF-7-2.                                                 NC1184.2
073300     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1184.2
073400     MOVE     666999.666333 TO CORRECT-N.                         NC1184.2
073500     PERFORM  FAIL.                                               NC1184.2
073600 SIG-WRITE-GF-7-2.                                                NC1184.2
073700     MOVE     "SIG-TEST-GF-7-2" TO PAR-NAME.                      NC1184.2
073800     PERFORM  PRINT-DETAIL.                                       NC1184.2
073900 SIG-INIT-GF-14.                                                  NC1184.2
074000     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
074100     MOVE     ZERO TO WRK-DS-T-05V00.                             NC1184.2
074200 SIG-TEST-GF-14-1.                                                NC1184.2
074300     ADD      33333                                               NC1184.2
074400              A06THREES-DS-03V03                                  NC1184.2
074500              A12THREES-DS-06V06                                  NC1184.2
074600              GIVING WRK-DS-T-05V00 ROUNDED ON SIZE ERROR         NC1184.2
074700              PERFORM PASS GO TO SIG-WRITE-GF-14-1.               NC1184.2
074800     GO       TO SIG-FAIL-GF-14-1.                                NC1184.2
074900 SIG-DELETE-GF-14-1.                                              NC1184.2
075000     PERFORM  DE-LETE.                                            NC1184.2
075100     GO       TO SIG-WRITE-GF-14-1.                               NC1184.2
075200 SIG-FAIL-GF-14-1.                                                NC1184.2
075300     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC1184.2
075400     PERFORM  FAIL.                                               NC1184.2
075500 SIG-WRITE-GF-14-1.                                               NC1184.2
075600     MOVE     "SIG-TEST-GF-14-1" TO PAR-NAME.                     NC1184.2
075700     PERFORM  PRINT-DETAIL.                                       NC1184.2
075800 SIG-TEST-GF-14-2.                                                NC1184.2
075900     IF       WRK-DS-T-05V00 EQUAL TO ZERO                        NC1184.2
076000              PERFORM PASS GO TO SIG-WRITE-GF-14-2.               NC1184.2
076100     GO       TO SIG-FAIL-GF-14-2.                                NC1184.2
076200*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SIG-TEST-GF-14-1  NC1184.2
076300 SIG-DELETE-GF-14-2.                                              NC1184.2
076400     PERFORM  DE-LETE.                                            NC1184.2
076500     GO       TO SIG-WRITE-GF-14-2.                               NC1184.2
076600 SIG-FAIL-GF-14-2.                                                NC1184.2
076700     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1184.2
076800     MOVE     WRK-DS-T-05V00 TO COMPUTED-N.                       NC1184.2
076900     MOVE     ZERO TO CORRECT-N.                                  NC1184.2
077000     PERFORM  FAIL.                                               NC1184.2
077100 SIG-WRITE-GF-14-2.                                               NC1184.2
077200     MOVE     "SIG-TEST-GF-14-2" TO PAR-NAME.                     NC1184.2
077300     PERFORM  PRINT-DETAIL.                                       NC1184.2
077400 SIG-INIT-GF-15.                                                  NC1184.2
077500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
077600     MOVE     ZERO TO WRK-DS-TS-06V06.                            NC1184.2
077700 SIG-TEST-GF-15-1-0.                                              NC1184.2
077800     ADD      A12THREES-DS-06V06                                  NC1184.2
077900              333333                                              NC1184.2
078000              A06THREES-DS-03V03                                  NC1184.2
078100              GIVING WRK-DS-TS-06V06 ROUNDED ON SIZE ERROR        NC1184.2
078200              GO TO SIG-FAIL-GF-15-1.                             NC1184.2
078300     PERFORM  PASS.                                               NC1184.2
078400     GO       TO SIG-WRITE-GF-15-1.                               NC1184.2
078500 SIG-DELETE-GF-15-1.                                              NC1184.2
078600     PERFORM  DE-LETE.                                            NC1184.2
078700     GO       TO SIG-WRITE-GF-15-1.                               NC1184.2
078800 SIG-FAIL-GF-15-1.                                                NC1184.2
078900     MOVE     "SIZE ERR SHOULD NOT EXECUTE" TO RE-MARK.           NC1184.2
079000     PERFORM  FAIL.                                               NC1184.2
079100 SIG-WRITE-GF-15-1.                                               NC1184.2
079200     MOVE     "SIG-TEST-GF-15-1" TO PAR-NAME.                     NC1184.2
079300     PERFORM  PRINT-DETAIL.                                       NC1184.2
079400 SIG-TEST-GF-15-2.                                                NC1184.2
079500     IF       WRK-DS-TS-06V06 EQUAL TO 666999.666333              NC1184.2
079600              PERFORM PASS GO TO SIG-WRITE-GF-15-2.               NC1184.2
079700*    NOTE: THIS TEST DEPENDS UPON THE RESULT OF SIG-TEST-GF-15-1  NC1184.2
079800     GO       TO SIG-FAIL-GF-15-2.                                NC1184.2
079900 SIG-DELETE-GF-15-2.                                              NC1184.2
080000     PERFORM  DE-LETE.                                            NC1184.2
080100     GO       TO SIG-WRITE-GF-15-2.                               NC1184.2
080200 SIG-FAIL-GF-15-2.                                                NC1184.2
080300     MOVE     WRK-DS-TS-06V06 TO COMPUTED-N.                      NC1184.2
080400     MOVE     666999.666333 TO CORRECT-N.                         NC1184.2
080500     PERFORM  FAIL.                                               NC1184.2
080600 SIG-WRITE-GF-15-2.                                               NC1184.2
080700     MOVE     "SIG-TEST-GF-15-2" TO PAR-NAME.                     NC1184.2
080800     PERFORM  PRINT-DETAIL.                                       NC1184.2
080900 SIG-INIT-GF-16.                                                  NC1184.2
081000     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
081100     MOVE "  SERIES" TO FEATURE.                                  NC1184.2
081200     MOVE     ZERO TO WRK-DS-03V10.                               NC1184.2
081300 SIG-TEST-GF-16-0.                                                NC1184.2
081400     ADD      A99-DS-T-02V00                                      NC1184.2
081500              A03ONES-DS-02V01                                    NC1184.2
081600              A06ONES-DS-TS-03V03                                 NC1184.2
081700              A08TWOS-DS-02V06                                    NC1184.2
081800              -1.1111111                                          NC1184.2
081900              +.11111111                                          NC1184.2
082000              A01ONES-DS-LS-P0801 GIVING WRK-DS-03V10.            NC1184.2
082100 SIG-TEST-GF-16-1.                                                NC1184.2
082200     IF       WRK-DS-03V10 EQUAL TO 242.4332220110                NC1184.2
082300              PERFORM PASS GO TO SIG-WRITE-GF-16.                 NC1184.2
082400     GO       TO SIG-FAIL-GF-16.                                  NC1184.2
082500 SIG-DELETE-GF-16.                                                NC1184.2
082600     PERFORM  DE-LETE.                                            NC1184.2
082700     GO       TO SIG-WRITE-GF-16.                                 NC1184.2
082800 SIG-FAIL-GF-16.                                                  NC1184.2
082900     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1184.2
083000     MOVE     242.4332220110 TO CORRECT-4V14.                     NC1184.2
083100     PERFORM  FAIL.                                               NC1184.2
083200 SIG-WRITE-GF-16.                                                 NC1184.2
083300     MOVE     "SIG-TEST-GF-16" TO PAR-NAME.                       NC1184.2
083400     PERFORM  PRINT-DETAIL.                                       NC1184.2
083500 SIG-INIT-GF-17.                                                  NC1184.2
083600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
083700     MOVE     ZERO TO WRK-DS-03V10.                               NC1184.2
083800 SIG-TEST-GF-17-0.                                                NC1184.2
083900     ADD      A01ONES-DS-LS-P0801                                 NC1184.2
084000              +.11111111                                          NC1184.2
084100              -1.1111111                                          NC1184.2
084200              A08TWOS-DS-02V06                                    NC1184.2
084300              A06ONES-DS-TS-03V03                                 NC1184.2
084400              A03ONES-DS-02V01                                    NC1184.2
084500              A99-DS-T-02V00 GIVING WRK-DS-03V10.                 NC1184.2
084600 SIG-TEST-GF-17-1.                                                NC1184.2
084700     IF       WRK-DS-03V10 EQUAL TO 242.4332220110                NC1184.2
084800              PERFORM PASS GO TO SIG-WRITE-GF-17.                 NC1184.2
084900     GO       TO SIG-FAIL-GF-17.                                  NC1184.2
085000 SIG-DELETE-GF-17.                                                NC1184.2
085100     PERFORM  DE-LETE.                                            NC1184.2
085200     GO       TO SIG-WRITE-GF-17.                                 NC1184.2
085300 SIG-FAIL-GF-17.                                                  NC1184.2
085400     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1184.2
085500     MOVE     242.4332220110 TO CORRECT-4V14.                     NC1184.2
085600     PERFORM  FAIL.                                               NC1184.2
085700 SIG-WRITE-GF-17.                                                 NC1184.2
085800     MOVE     "SIG-TEST-GF-17" TO PAR-NAME.                       NC1184.2
085900     PERFORM  PRINT-DETAIL.                                       NC1184.2
086000 SIG-INIT-GF-18.                                                  NC1184.2
086100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
086200     MOVE     ZERO TO WRK-DS-03V10.                               NC1184.2
086300 SIG-TEST-GF-18-0.                                                NC1184.2
086400     ADD      A08TWOS-DS-02V06                                    NC1184.2
086500              A99-DS-T-02V00                                      NC1184.2
086600              -1.1111111                                          NC1184.2
086700              A03ONES-DS-02V01                                    NC1184.2
086800              A01ONES-DS-LS-P0801                                 NC1184.2
086900              +.11111111                                          NC1184.2
087000              A06ONES-DS-TS-03V03 GIVING WRK-DS-03V10.            NC1184.2
087100 SIG-TEST-GF-18-1.                                                NC1184.2
087200     IF       WRK-DS-03V10 EQUAL TO 242.4332220110                NC1184.2
087300              PERFORM PASS GO TO SIG-WRITE-GF-18.                 NC1184.2
087400     GO       TO SIG-FAIL-GF-18.                                  NC1184.2
087500 SIG-DELETE-GF-18.                                                NC1184.2
087600     PERFORM  DE-LETE.                                            NC1184.2
087700     GO       TO SIG-WRITE-GF-18.                                 NC1184.2
087800 SIG-FAIL-GF-18.                                                  NC1184.2
087900     MOVE     WRK-DS-03V10 TO COMPUTED-4V14.                      NC1184.2
088000     MOVE     242.4332220110 TO CORRECT-4V14.                     NC1184.2
088100     PERFORM  FAIL.                                               NC1184.2
088200 SIG-WRITE-GF-18.                                                 NC1184.2
088300     MOVE     "SIG-TEST-GF-18" TO PAR-NAME.                       NC1184.2
088400     PERFORM  PRINT-DETAIL.                                       NC1184.2
088500 SIG-INIT-GF-8.                                                   NC1184.2
088600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
088700     MOVE "  COMP VS. DISPLAY"  TO FEATURE.                       NC1184.2
088800     MOVE     A18ONES-DS-TS-18V00 TO WRK-CS-18V00.                NC1184.2
088900 SIG-TEST-GF-8-0.                                                 NC1184.2
089000     ADD      A18ONES-DS-TS-18V00 TO WRK-CS-18V00.                NC1184.2
089100 SIG-TEST-GF-8-1.                                                 NC1184.2
089200     IF       WRK-CS-18V00 EQUAL TO 222222222222222222            NC1184.2
089300              PERFORM PASS GO TO SIG-WRITE-GF-8.                  NC1184.2
089400     GO       TO SIG-FAIL-GF-8.                                   NC1184.2
089500 SIG-DELETE-GF-8.                                                 NC1184.2
089600     PERFORM  DE-LETE.                                            NC1184.2
089700     GO       TO SIG-WRITE-GF-8.                                  NC1184.2
089800 SIG-FAIL-GF-8.                                                   NC1184.2
089900     MOVE     WRK-CS-18V00 TO COMPUTED-18V0.                      NC1184.2
090000     MOVE     222222222222222222 TO CORRECT-18V0.                 NC1184.2
090100     PERFORM  FAIL.                                               NC1184.2
090200 SIG-WRITE-GF-8.                                                  NC1184.2
090300     MOVE     "SIG-TEST-GF-8" TO PAR-NAME.                        NC1184.2
090400     PERFORM  PRINT-DETAIL.                                       NC1184.2
090500 SIG-INIT-GF-9.                                                   NC1184.2
090600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
090700     MOVE     A18ONES-DS-TS-18V00 TO WRK-DS-T-18V00.              NC1184.2
090800 SIG-TEST-GF-9-0.                                                 NC1184.2
090900     ADD      A18ONES-CS-18V00 TO WRK-DS-T-18V00.                 NC1184.2
091000 SIG-TEST-GF-9-1.                                                 NC1184.2
091100     IF       WRK-DS-T-18V00 EQUAL TO 222222222222222222          NC1184.2
091200              PERFORM PASS GO TO SIG-WRITE-GF-9.                  NC1184.2
091300     GO       TO SIG-FAIL-GF-9.                                   NC1184.2
091400 SIG-DELETE-GF-9.                                                 NC1184.2
091500     PERFORM  DE-LETE.                                            NC1184.2
091600     GO       TO SIG-WRITE-GF-9.                                  NC1184.2
091700 SIG-FAIL-GF-9.                                                   NC1184.2
091800     MOVE     WRK-DS-T-18V00 TO COMPUTED-18V0.                    NC1184.2
091900     MOVE     222222222222222222 TO CORRECT-18V0.                 NC1184.2
092000     PERFORM  FAIL.                                               NC1184.2
092100 SIG-WRITE-GF-9.                                                  NC1184.2
092200     MOVE     "SIG-TEST-GF-9" TO PAR-NAME.                        NC1184.2
092300     PERFORM  PRINT-DETAIL.                                       NC1184.2
092400 SIG-INIT-GF-19.                                                  NC1184.2
092500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
092600     MOVE SPACE TO SIZE-ERR.                                      NC1184.2
092700 SIG-TEST-GF-19-0.                                                NC1184.2
092800     ADD MINUS-NAME1  MINUS-NAME2  -34  -1  PLUS-NAME1            NC1184.2
092900         PLUS-NAME2  EVEN-NAME1  35  GIVING WHOLE-FIELD           NC1184.2
093000         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1184.2
093100 SIG-TEST-GF-19-1.                                                NC1184.2
093200     IF WHOLE-FIELD EQUAL TO +1                                   NC1184.2
093300         PERFORM PASS                                             NC1184.2
093400         GO TO SIG-WRITE-GF-19-1.                                 NC1184.2
093500     MOVE WHOLE-FIELD TO COMPUTED-18V0.                           NC1184.2
093600     MOVE +1 TO CORRECT-18V0.                                     NC1184.2
093700     PERFORM FAIL.                                                NC1184.2
093800     GO TO SIG-WRITE-GF-19-1.                                     NC1184.2
093900 SIG-DELETE-GF-19-1.                                              NC1184.2
094000     PERFORM DE-LETE.                                             NC1184.2
094100 SIG-WRITE-GF-19-1.                                               NC1184.2
094200     MOVE "SIG-TEST-GF-19-1" TO PAR-NAME.                         NC1184.2
094300     PERFORM PRINT-DETAIL.                                        NC1184.2
094400 SIG-TEST-GF-19-2.                                                NC1184.2
094500     IF SIZE-ERR EQUAL TO "1"                                     NC1184.2
094600         PERFORM FAIL                                             NC1184.2
094700         MOVE SPACE TO CORRECT-A                                  NC1184.2
094800         MOVE 1 TO COMPUTED-A                                     NC1184.2
094900         MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK      NC1184.2
095000         GO TO SIG-WRITE-GF-19-2.                                 NC1184.2
095100     PERFORM PASS.                                                NC1184.2
095200     GO TO SIG-WRITE-GF-19-2.                                     NC1184.2
095300 SIG-DELETE-GF-19-2.                                              NC1184.2
095400     PERFORM DE-LETE.                                             NC1184.2
095500 SIG-WRITE-GF-19-2.                                               NC1184.2
095600     MOVE "SIG-TEST-GF-19-2" TO PAR-NAME.                         NC1184.2
095700     PERFORM PRINT-DETAIL.                                        NC1184.2
095800 SIG-INIT-GF-20.                                                  NC1184.2
095900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
096000     MOVE SPACE TO SIZE-ERR.                                      NC1184.2
096100     MOVE ZERO  TO DECMAL-FIELD.                                  NC1184.2
096200     MOVE -.999999999999999999 TO MINUS-NAME3.                    NC1184.2
096300     MOVE -.999999999999999999 TO MINUS-NAME4.                    NC1184.2
096400     MOVE +.1                  TO EVEN-NAME2.                     NC1184.2
096500     MOVE +.999999999999999999 TO PLUS-NAME3.                     NC1184.2
096600     MOVE +.999999999999999999 TO PLUS-NAME4.                     NC1184.2
096700 SIG-TEST-GF-20-0.                                                NC1184.2
096800     ADD MINUS-NAME3  MINUS-NAME4  -.34  -.01  PLUS-NAME3         NC1184.2
096900         PLUS-NAME4  EVEN-NAME2  .35  GIVING DECMAL-FIELD         NC1184.2
097000         ON SIZE ERROR  MOVE "1" TO SIZE-ERR.                     NC1184.2
097100 SIG-TEST-GF-20-1.                                                NC1184.2
097200     IF DECMAL-FIELD EQUAL TO +.1                                 NC1184.2
097300         PERFORM PASS                                             NC1184.2
097400         GO TO SIG-WRITE-GF-20-1.                                 NC1184.2
097500     MOVE DECMAL-FIELD TO COMPUTED-0V18.                          NC1184.2
097600     MOVE +.1 TO CORRECT-0V18.                                    NC1184.2
097700     PERFORM FAIL.                                                NC1184.2
097800     GO TO SIG-WRITE-GF-20-1.                                     NC1184.2
097900 SIG-DELETE-GF-20-1.                                              NC1184.2
098000     PERFORM DE-LETE.                                             NC1184.2
098100 SIG-WRITE-GF-20-1.                                               NC1184.2
098200     MOVE "SIG-TEST-GF-20-1" TO PAR-NAME.                         NC1184.2
098300     PERFORM PRINT-DETAIL.                                        NC1184.2
098400 SIG-TEST-GF-20-2.                                                NC1184.2
098500     IF SIZE-ERR EQUAL TO "1"                                     NC1184.2
098600         PERFORM FAIL                                             NC1184.2
098700         MOVE SPACE TO CORRECT-A                                  NC1184.2
098800         MOVE 1 TO COMPUTED-A                                     NC1184.2
098900         MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK      NC1184.2
099000         GO TO SIG-WRITE-GF-20-2.                                 NC1184.2
099100     PERFORM PASS.                                                NC1184.2
099200     GO TO SIG-WRITE-GF-20-2.                                     NC1184.2
099300 SIG-DELETE-GF-20-2.                                              NC1184.2
099400     PERFORM DE-LETE.                                             NC1184.2
099500 SIG-WRITE-GF-20-2.                                               NC1184.2
099600     MOVE "SIG-TEST-GF-20-2" TO PAR-NAME.                         NC1184.2
099700     PERFORM PRINT-DETAIL.                                        NC1184.2
099800 SIG-INIT-GF-21.                                                  NC1184.2
099900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
100000     MOVE ZERO TO WRK-CS-18V00.                                   NC1184.2
100100 SIG-TEST-GF-21-0.                                                NC1184.2
100200     ADD A18ONES-CS-18V00 A18ONES-DS-TS-18V00 GIVING WRK-CS-18V00.NC1184.2
100300 SIG-TEST-GF-21-1.                                                NC1184.2
100400     IF WRK-CS-18V00 EQUAL TO 222222222222222222                  NC1184.2
100500         PERFORM PASS                                             NC1184.2
100600         GO TO SIG-WRITE-GF-21.                                   NC1184.2
100700     MOVE 222222222222222222 TO CORRECT-18V0.                     NC1184.2
100800     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1184.2
100900     PERFORM FAIL.                                                NC1184.2
101000     GO TO SIG-WRITE-GF-21.                                       NC1184.2
101100 SIG-DELETE-GF-21.                                                NC1184.2
101200     PERFORM DE-LETE.                                             NC1184.2
101300 SIG-WRITE-GF-21.                                                 NC1184.2
101400     MOVE "SIG-TEST-GF-21 " TO PAR-NAME.                          NC1184.2
101500     PERFORM PRINT-DETAIL.                                        NC1184.2
101600 SIG-INIT-GF-22.                                                  NC1184.2
101700     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1184.2
101800     MOVE ZERO TO WRK-DS-T-18V00.                                 NC1184.2
101900 SIG-TEST-GF-22-0.                                                NC1184.2
102000     ADD A18SIXES-CS-18V00  A12SEVENS-CU-18V00 GIVING             NC1184.2
102100             WRK-DS-T-18V00.                                      NC1184.2
102200 SIG-TEST-GF-22-1.                                                NC1184.2
102300     IF WRK-DS-T-18V00 EQUAL TO 666667444444444443                NC1184.2
102400         PERFORM PASS                                             NC1184.2
102500         GO TO SIG-WRITE-GF-22.                                   NC1184.2
102600     MOVE 666667444444444443 TO CORRECT-18V0.                     NC1184.2
102700     MOVE WRK-DS-T-18V00 TO COMPUTED-18V0.                        NC1184.2
102800     PERFORM FAIL.                                                NC1184.2
102900     GO TO SIG-WRITE-GF-22.                                       NC1184.2
103000 SIG-DELETE-GF-22.                                                NC1184.2
103100     PERFORM DE-LETE.                                             NC1184.2
103200 SIG-WRITE-GF-22.                                                 NC1184.2
103300     MOVE "SIG-TEST-GF-22 " TO PAR-NAME.                          NC1184.2
103400     PERFORM PRINT-DETAIL.                                        NC1184.2
103500 CCVS-EXIT SECTION.                                               NC1184.2
103600 CCVS-999999.                                                     NC1184.2
103700     GO TO CLOSE-FILES.                                           NC1184.2
      *END-OF,NC118A                                                                  
