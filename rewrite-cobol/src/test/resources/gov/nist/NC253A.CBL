      *HEADER,COBOL,NC253A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2534.2
000200 PROGRAM-ID.                                                      NC2534.2
000300     NC253A.                                                      NC2534.2
000400****************************************************************  NC2534.2
000500*                                                              *  NC2534.2
000600*    VALIDATION FOR:-                                          *  NC2534.2
000700*                                                              *  NC2534.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2534.2
000900*                                                              *  NC2534.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2534.2
001100*                                                              *  NC2534.2
001200****************************************************************  NC2534.2
001300*                                                              *  NC2534.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2534.2
001500*                                                              *  NC2534.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2534.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2534.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2534.2
001900*                                                              *  NC2534.2
002000****************************************************************  NC2534.2
002100                                                                  NC2534.2
002200*                                                                 NC2534.2
002300*    PROGRAM NC202A TESTS FORMAT3 OF THE SUBTRACT STATEMENT.      NC2534.2
002400*                                                                 NC2534.2
002500*                                                                 NC2534.2
002600 ENVIRONMENT DIVISION.                                            NC2534.2
002700 CONFIGURATION SECTION.                                           NC2534.2
002800 SOURCE-COMPUTER.                                                 NC2534.2
002900     XXXXX082.                                                    NC2534.2
003000 OBJECT-COMPUTER.                                                 NC2534.2
003100     XXXXX083.                                                    NC2534.2
003200 INPUT-OUTPUT SECTION.                                            NC2534.2
003300 FILE-CONTROL.                                                    NC2534.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC2534.2
003500     XXXXX055.                                                    NC2534.2
003600 DATA DIVISION.                                                   NC2534.2
003700 FILE SECTION.                                                    NC2534.2
003800 FD  PRINT-FILE.                                                  NC2534.2
003900 01  PRINT-REC PICTURE X(120).                                    NC2534.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC2534.2
004100 WORKING-STORAGE SECTION.                                         NC2534.2
004200 01  TABLE1.                                                      NC2534.2
004300     02  RECORD1                 PICTURE 99.                      NC2534.2
004400     02  RECORD2                 PICTURE 99                       NC2534.2
004500                                 OCCURS 2 TIMES                   NC2534.2
004600                                 INDEXED BY INDEX1.               NC2534.2
004700     02  RECORD3                 PICTURE 99.                      NC2534.2
004800 01  TABLE2.                                                      NC2534.2
004900     02  RECORD1                 PICTURE 99.                      NC2534.2
005000     02  RECORD2                 PICTURE 99                       NC2534.2
005100                                 OCCURS 2 TIMES                   NC2534.2
005200                                 INDEXED BY INDEX2.               NC2534.2
005300     02  RECORD3                 PICTURE 99.                      NC2534.2
005400 77  WRK-AN-00001                PICTURE A.                       NC2534.2
005500 77  WRK-XN-00001                PICTURE X.                       NC2534.2
005600 77  WRK-DS-01V00                PICTURE S9.                      NC2534.2
005700 77  WRK-DS-02V00                PICTURE S99.                     NC2534.2
005800 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC2534.2
005900 77  WRK-DS-05V00                PICTURE S9(5).                   NC2534.2
006000 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC2534.2
006100 77  WRK-DS-09V09                PICTURE S9(9)V9(9).              NC2534.2
006200 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC2534.2
006300 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC2534.2
006400                                 VALUE 111111111.111111111.       NC2534.2
006500 77  WRK-DS-18V00       PICTURE S9(18) VALUE 111111111111111111.  NC2534.2
006600 77  A05ONES-DS-05V00            PICTURE S9(5) VALUE 11111.       NC2534.2
006700 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC2534.2
006800 77  WRK-DS-03V00                PICTURE S999.                    NC2534.2
006900 77  WRK-DS-06V00                PICTURE S9(6).                   NC2534.2
007000 77  WRK-DS-0201P                PICTURE S99P.                    NC2534.2
007100 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC2534.2
007200 77  ADD-1                   PICTURE S9(8)V99  VALUE 1.           NC2534.2
007300 77  ADD-2                   PICTURE S9(6)V9(4) VALUE 1.          NC2534.2
007400 77  ADD-3                   PICTURE S9(5)     VALUE -1.          NC2534.2
007500 77  ADD-4                   PICTURE 9         VALUE 9.           NC2534.2
007600 77  ADD-5                   PICTURE 9         VALUE 9.           NC2534.2
007700 77  ADD-6                   PICTURE 9(5)      VALUE 99999.       NC2534.2
007800 77  ADD-7                   PICTURE 9         VALUE 1.           NC2534.2
007900 77  ADD-8                   PICTURE 9.                           NC2534.2
008000 77  ADD-9                   PICTURE S9(8)V99  VALUE 5.9.         NC2534.2
008100 77  ADD-10                  PICTURE 9(5)      VALUE 52800.       NC2534.2
008200 77  ADD-11                  PICTURE 99999.                       NC2534.2
008300 77  ADD-12                  PICTURE PP9       VALUE .001.        NC2534.2
008400 77  ADD-13                  PICTURE 9PP       VALUE 100.         NC2534.2
008500 77  ADD-14                  PICTURE 999V999.                     NC2534.2
008600 77  W-1                     PICTURE IS 9.                        NC2534.2
008700 77  W-2                     PICTURE IS 99.                       NC2534.2
008800 77  W-3                     PICTURE IS 999.                      NC2534.2
008900 77  W-4                PICTURE 9  VALUE 0.                       NC2534.2
009000 77  W-6                     PICTURE IS 999    VALUE IS ZERO.     NC2534.2
009100 77  W-9                     PICTURE 999.                         NC2534.2
009200 77  D-5                PICTURE S999  VALUE -1.                   NC2534.2
009300 77  D-9                PICTURE 9(4)V9(4)  VALUE 111.1189.        NC2534.2
009400 77  ONE                PICTURE 9  VALUE 1.                       NC2534.2
009500 77  TWO                PICTURE S9  VALUE 2.                      NC2534.2
009600 77  THREE              PICTURE S9  VALUE 3.                      NC2534.2
009700 77  FOUR               PICTURE S9  VALUE 4.                      NC2534.2
009800 77  FIVE               PICTURE S9  VALUE 5.                      NC2534.2
009900 77  SIX                PICTURE S9  VALUE 6.                      NC2534.2
010000 77  SEVEN              PICTURE S9  VALUE 7.                      NC2534.2
010100 77  EIGHT              PICTURE 9  VALUE 8.                       NC2534.2
010200 77  NINE               PICTURE S9  VALUE 9.                      NC2534.2
010300 77  TEN                PICTURE S99  VALUE 10.                    NC2534.2
010400 77  FIFTEEN            PICTURE S99  VALUE 15.                    NC2534.2
010500 77  TWENTY             PICTURE S99  VALUE 20.                    NC2534.2
010600 77  TWENTY-5           PICTURE S99  VALUE 25.                    NC2534.2
010700 01  WRK-DS-09V00                 PICTURE S9(9)  VALUE ZERO.      NC2534.2
010800 01  GRP-FOR-ADD-CORR-1.                                          NC2534.2
010900     02  GRP-SUBTRACT-CORR-1.                                     NC2534.2
011000     03  FILLER                  PICTURE S99  VALUE 91.           NC2534.2
011100     03  ADD-CORR-2              PICTURE S99  VALUE 22.           NC2534.2
011200     03  ADD-CORR-1              PICTURE S99 VALUE 11.            NC2534.2
011300     03  ADD-CORR-A              PICTURE S99 VALUE 93.            NC2534.2
011400     03  ADD-CORR-4              PICTURE S99 VALUE 44.            NC2534.2
011500     03  ADD-CORR-3              PICTURE S99 VALUE 33.            NC2534.2
011600     03  ADD-CORR-6              PICTURE S99 VALUE 66.            NC2534.2
011700     03  ADD-CORR-5              PICTURE S99 VALUE 55.            NC2534.2
011800     03  ADD-CORR-8              PICTURE S99 VALUE 88.            NC2534.2
011900     03  ADD-CORR-7              PICTURE S99 VALUE 77.            NC2534.2
012000     03  ADD-CORR-9              PICTURE S99 VALUE 99.            NC2534.2
012100 01  GRP-FOR-ADD-CORR-R.                                          NC2534.2
012200     02  GRP-SUBTRACT-CORR-1.                                     NC2534.2
012300     05  ADD-CORR-1              PICTURE 99.                      NC2534.2
012400     05  ADD-CORR-2              PICTURE 99.                      NC2534.2
012500     05  ADD-CORR-3              PICTURE 99.                      NC2534.2
012600     05  ADD-CORR-4              PICTURE 99.                      NC2534.2
012700     05  ADD-CORR-5              PICTURE 9P.                      NC2534.2
012800     05  ADD-CORR-6              PICTURE 999.                     NC2534.2
012900     05  ADD-CORR-7              PICTURE 99.                      NC2534.2
013000     05  ADD-CORR-8              PICTURE 99.                      NC2534.2
013100     05  ADD-CORR-9              PICTURE 99.                      NC2534.2
013200     05  FILLER                  PICTURE 99.                      NC2534.2
013300 01  GRP-FOR-ADD-CORR-2.                                          NC2534.2
013400     02  GRP-ADD-SUB-CORR.                                        NC2534.2
013500     03  GRP-SUBTRACT-CORR-1.                                     NC2534.2
013600     04  ADD-CORR-1              PICTURE S99  VALUE 11.           NC2534.2
013700     04  ADD-CORR-2              PICTURE S99  VALUE 22.           NC2534.2
013800     04  ADD-CORR-5              PICTURE S99  VALUE 55.           NC2534.2
013900     04  ADD-CORR-4              PICTURE S99  VALUE 44.           NC2534.2
014000     04  ADD-CORR-3              PICTURE S99  VALUE 33.           NC2534.2
014100     04  ADD-CORR-6              PICTURE S99  VALUE 66.           NC2534.2
014200     04  ADD-CORR-7              PICTURE S99  VALUE 77.           NC2534.2
014300     04  ADD-CORR-8              PICTURE S99  VALUE 88.           NC2534.2
014400     04  ADD-CORR-9              PICTURE S99  VALUE 99.           NC2534.2
014500     04  ADD-CORR-B              PICTURE S99  VALUE 92.           NC2534.2
014600     04  ADD-CORR-0              PICTURE S99  VALUE 00.           NC2534.2
014700 01  GRP-FOR-ADD-CORR-A.                                          NC2534.2
014800     02  GRP-SUBTRACT-CORR-3.                                     NC2534.2
014900         03  GRP-SUBTRACT-CORR-1.                                 NC2534.2
015000             05  ADD-CORR-4      PICTURE S999   VALUE 044.        NC2534.2
015100             05  ADD-CORR-3      PICTURE S999   VALUE 033.        NC2534.2
015200             05  ADD-CORR-2      PICTURE S999   VALUE 022.        NC2534.2
015300             05  ADD-CORR-1      PICTURE S999   VALUE 111.        NC2534.2
015400 01  ADD-15.                                                      NC2534.2
015500     02 FIELD1               PICTURE 99999     VALUE 1.           NC2534.2
015600     02 FIELD2               PICTURE 999V99    VALUE 32.1.        NC2534.2
015700     02 FIELD3               PICTURE 999V9     VALUE 123.4.       NC2534.2
015800 01  ADD-16.                                                      NC2534.2
015900     02 FIELD1               PICTURE 99999     VALUE 99999.       NC2534.2
016000     02 FIELD2               PICTURE 999V99    VALUE 745.67.      NC2534.2
016100     02 FIELD3               PICTURE 999V9     VALUE 432.1.       NC2534.2
016200 01  SUBTRACT-DATA.                                               NC2534.2
016300     02 SUBTR-1              PICTURE 9         VALUE 1.           NC2534.2
016400     02 SUBTR-2              PICTURE S99       VALUE 99.          NC2534.2
016500     02 SUBTR-3              PICTURE S9V99     VALUE -1.          NC2534.2
016600     02 SUBTR-4              PICTURE SPP9      VALUE .001.        NC2534.2
016700     02 SUBTR-5              PICTURE S9PP      VALUE 100.         NC2534.2
016800     02  SUBTR-6                  PICTURE 9 VALUE 1.              NC2534.2
016900     02  SUBTR-7                  PICTURE S99  VALUE 99.          NC2534.2
017000     02  SUBTR-8                  PICTURE S9V99  VALUE -9.99.     NC2534.2
017100     02 SUBTR-9              PICTURE SV999.                       NC2534.2
017200     02  SUBTR-10                 PICTURE S999  VALUE 100.        NC2534.2
017300     02 SUBTR-11             PICTURE S999V999.                    NC2534.2
017400     02 SUBTR-12.                                                 NC2534.2
017500     03 SUBTR-13             PICTURE 9         VALUE 1.           NC2534.2
017600     03 SUBTR-14             PICTURE S9V999    VALUE -1.725.      NC2534.2
017700     03 SUBTR-15             PICTURE S99V99    VALUE 76.76.       NC2534.2
017800     02 SUBTR-16.                                                 NC2534.2
017900     03 SUBTR-13             PICTURE 9         VALUE 2.           NC2534.2
018000     03 SUBTR-14             PICTURE S9V99     VALUE .23.         NC2534.2
018100     03 SUBTR-15             PICTURE S9V99     VALUE 1.           NC2534.2
018200 01  CORR-DATA-1.                                                 NC2534.2
018300     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
018400     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
018500     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
018600     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
018700     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
018800     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
018900 01  CORR-DATA-2.                                                 NC2534.2
019000     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019100     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019200     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019300     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019400     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019500     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019600 01  CORR-DATA-3.                                                 NC2534.2
019700     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019800     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
019900     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
020000     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
020100     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
020200     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2534.2
020300 01  CORR-DATA-4.                                                 NC2534.2
020400     03 XYZ-11               PICTURE IS 99.                       NC2534.2
020500     03 XYZ-12               PICTURE IS 99.                       NC2534.2
020600     03 XYZ-13               PICTURE IS 99.                       NC2534.2
020700     03 XYZ-14               PICTURE IS 99.                       NC2534.2
020800     03 XYZ-15               PICTURE IS 99.                       NC2534.2
020900     03 XYZ-16               PICTURE IS 99.                       NC2534.2
021000 01  CORR-DATA-5.                                                 NC2534.2
021100     03 XYZ-1                PICTURE 99.                          NC2534.2
021200     03 XYZ-2                PICTURE 99.                          NC2534.2
021300     03 XYZ-13               PICTURE IS 99.                       NC2534.2
021400     03 XYZ-14               PICTURE IS 99.                       NC2534.2
021500     03 FILLER               PICTURE IS 99.                       NC2534.2
021600     03 XYZ-11               PICTURE IS 99.                       NC2534.2
021700     03 XYZ-12               PICTURE IS 99.                       NC2534.2
021800 01  CORR-DATA-6.                                                 NC2534.2
021900     03 XYZ-11               PICTURE IS 99.                       NC2534.2
022000     03 XYZ-12               PICTURE IS 99.                       NC2534.2
022100     03 FILLER               PICTURE IS 99.                       NC2534.2
022200     03 XYZ-1                PICTURE IS 99.                       NC2534.2
022300     03 XYZ-2                PICTURE IS 9(2).                     NC2534.2
022400     03 FILLER               PICTURE IS 99.                       NC2534.2
022500 01  CORR-DATA-7.                                                 NC2534.2
022600     02 XYZ-1                PICTURE 99V99     VALUE 10.45.       NC2534.2
022700     02 XYZ-6                PICTURE 999V9     VALUE 100.5.       NC2534.2
022800     02 XYZ-11               PICTURE 99V9      VALUE ZERO.        NC2534.2
022900     02 XYZ-2                PICTURE 99V9      VALUE 0.9.         NC2534.2
023000 01  42-DATANAMES.                                                NC2534.2
023100     02  DNAME1   PICTURE 9      VALUE 1        COMPUTATIONAL.    NC2534.2
023200     02  DNAME2   PICTURE 99      VALUE 1  COMPUTATIONAL.         NC2534.2
023300     02  DNAME3   PICTURE 999     VALUE 1  COMPUTATIONAL.         NC2534.2
023400     02  DNAME4   PICTURE 9(4)    VALUE 1  COMPUTATIONAL.         NC2534.2
023500     02  DNAME5   PICTURE 9(5)    VALUE 1  COMPUTATIONAL.         NC2534.2
023600     02  DNAME6   PICTURE 9(6)    VALUE 1  COMPUTATIONAL.         NC2534.2
023700     02  DNAME7   PICTURE 9(7)    VALUE 1  COMPUTATIONAL.         NC2534.2
023800     02  DNAME8   PICTURE 9(8)    VALUE 1  COMPUTATIONAL.         NC2534.2
023900     02  DNAME9   PICTURE 9(9)    VALUE 1  COMPUTATIONAL.         NC2534.2
024000     02  DNAME10  PICTURE 9(10)   VALUE 1.                        NC2534.2
024100     02  DNAME11  PICTURE 9(11)   VALUE 1.                        NC2534.2
024200     02  DNAME12  PICTURE 9(12)   VALUE 1.                        NC2534.2
024300     02  DNAME13  PICTURE 9(13)   VALUE 1.                        NC2534.2
024400     02  DNAME14  PICTURE 9(14)   VALUE 1.                        NC2534.2
024500     02  DNAME15  PICTURE 9(15)   VALUE 1.                        NC2534.2
024600     02  DNAME16  PICTURE 9(16)   VALUE 1.                        NC2534.2
024700     02  DNAME17  PICTURE 9(17)   VALUE 1.                        NC2534.2
024800     02  DNAME18  PICTURE 9(18)   VALUE 1.                        NC2534.2
024900     02  DNAME19  PICTURE 9       VALUE 1.                        NC2534.2
025000     02  DNAME20  PICTURE 99      VALUE 1.                        NC2534.2
025100     02  DNAME21  PICTURE 999     VALUE 1.                        NC2534.2
025200     02  DNAME22  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025300     02  DNAME23  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025400     02  DNAME24  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025500     02  DNAME25  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025600     02  DNAME26  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025700     02  DNAME27  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025800     02  DNAME28  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
025900     02  DNAME29  PICTURE 9(18)  VALUE ZERO.                      NC2534.2
026000     02  DNAME30   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026100     02  DNAME31   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026200     02  DNAME32   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026300     02  DNAME33   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026400     02  DNAME34   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026500     02  DNAME35   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026600     02  DNAME36   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026700     02  DNAME37   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026800     02  DNAME38   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
026900     02  DNAME39   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
027000     02  DNAME40   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
027100     02  DNAME41   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
027200     02  DNAME42   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2534.2
027300 01  TEST-RESULTS.                                                NC2534.2
027400     02 FILLER                   PIC X      VALUE SPACE.          NC2534.2
027500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2534.2
027600     02 FILLER                   PIC X      VALUE SPACE.          NC2534.2
027700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2534.2
027800     02 FILLER                   PIC X      VALUE SPACE.          NC2534.2
027900     02  PAR-NAME.                                                NC2534.2
028000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2534.2
028100       03  PARDOT-X              PIC X      VALUE SPACE.          NC2534.2
028200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2534.2
028300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2534.2
028400     02 RE-MARK                  PIC X(61).                       NC2534.2
028500 01  TEST-COMPUTED.                                               NC2534.2
028600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2534.2
028700     02 FILLER                   PIC X(17)  VALUE                 NC2534.2
028800            "       COMPUTED=".                                   NC2534.2
028900     02 COMPUTED-X.                                               NC2534.2
029000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2534.2
029100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2534.2
029200                                 PIC -9(9).9(9).                  NC2534.2
029300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2534.2
029400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2534.2
029500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2534.2
029600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2534.2
029700         04 COMPUTED-18V0                    PIC -9(18).          NC2534.2
029800         04 FILLER                           PIC X.               NC2534.2
029900     03 FILLER PIC X(50) VALUE SPACE.                             NC2534.2
030000 01  TEST-CORRECT.                                                NC2534.2
030100     02 FILLER PIC X(30) VALUE SPACE.                             NC2534.2
030200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2534.2
030300     02 CORRECT-X.                                                NC2534.2
030400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2534.2
030500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2534.2
030600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2534.2
030700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2534.2
030800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2534.2
030900     03      CR-18V0 REDEFINES CORRECT-A.                         NC2534.2
031000         04 CORRECT-18V0                     PIC -9(18).          NC2534.2
031100         04 FILLER                           PIC X.               NC2534.2
031200     03 FILLER PIC X(2) VALUE SPACE.                              NC2534.2
031300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2534.2
031400 01  CCVS-C-1.                                                    NC2534.2
031500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2534.2
031600-    "SS  PARAGRAPH-NAME                                          NC2534.2
031700-    "       REMARKS".                                            NC2534.2
031800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2534.2
031900 01  CCVS-C-2.                                                    NC2534.2
032000     02 FILLER                     PIC X        VALUE SPACE.      NC2534.2
032100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2534.2
032200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2534.2
032300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2534.2
032400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2534.2
032500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2534.2
032600 01  REC-CT                        PIC 99       VALUE ZERO.       NC2534.2
032700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2534.2
032800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2534.2
032900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2534.2
033000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2534.2
033100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2534.2
033200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2534.2
033300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2534.2
033400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2534.2
033500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2534.2
033600 01  CCVS-H-1.                                                    NC2534.2
033700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2534.2
033800     02  FILLER                    PIC X(42)    VALUE             NC2534.2
033900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2534.2
034000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2534.2
034100 01  CCVS-H-2A.                                                   NC2534.2
034200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2534.2
034300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2534.2
034400   02  FILLER                        PIC XXXX   VALUE             NC2534.2
034500     "4.2 ".                                                      NC2534.2
034600   02  FILLER                        PIC X(28)  VALUE             NC2534.2
034700            " COPY - NOT FOR DISTRIBUTION".                       NC2534.2
034800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2534.2
034900                                                                  NC2534.2
035000 01  CCVS-H-2B.                                                   NC2534.2
035100   02  FILLER                        PIC X(15)  VALUE             NC2534.2
035200            "TEST RESULT OF ".                                    NC2534.2
035300   02  TEST-ID                       PIC X(9).                    NC2534.2
035400   02  FILLER                        PIC X(4)   VALUE             NC2534.2
035500            " IN ".                                               NC2534.2
035600   02  FILLER                        PIC X(12)  VALUE             NC2534.2
035700     " HIGH       ".                                              NC2534.2
035800   02  FILLER                        PIC X(22)  VALUE             NC2534.2
035900            " LEVEL VALIDATION FOR ".                             NC2534.2
036000   02  FILLER                        PIC X(58)  VALUE             NC2534.2
036100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2534.2
036200 01  CCVS-H-3.                                                    NC2534.2
036300     02  FILLER                      PIC X(34)  VALUE             NC2534.2
036400            " FOR OFFICIAL USE ONLY    ".                         NC2534.2
036500     02  FILLER                      PIC X(58)  VALUE             NC2534.2
036600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2534.2
036700     02  FILLER                      PIC X(28)  VALUE             NC2534.2
036800            "  COPYRIGHT   1985 ".                                NC2534.2
036900 01  CCVS-E-1.                                                    NC2534.2
037000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2534.2
037100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2534.2
037200     02 ID-AGAIN                     PIC X(9).                    NC2534.2
037300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2534.2
037400 01  CCVS-E-2.                                                    NC2534.2
037500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2534.2
037600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2534.2
037700     02 CCVS-E-2-2.                                               NC2534.2
037800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2534.2
037900         03 FILLER                   PIC X      VALUE SPACE.      NC2534.2
038000         03 ENDER-DESC               PIC X(44)  VALUE             NC2534.2
038100            "ERRORS ENCOUNTERED".                                 NC2534.2
038200 01  CCVS-E-3.                                                    NC2534.2
038300     02  FILLER                      PIC X(22)  VALUE             NC2534.2
038400            " FOR OFFICIAL USE ONLY".                             NC2534.2
038500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2534.2
038600     02  FILLER                      PIC X(58)  VALUE             NC2534.2
038700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2534.2
038800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2534.2
038900     02 FILLER                       PIC X(15)  VALUE             NC2534.2
039000             " COPYRIGHT 1985".                                   NC2534.2
039100 01  CCVS-E-4.                                                    NC2534.2
039200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2534.2
039300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2534.2
039400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2534.2
039500     02 FILLER                       PIC X(40)  VALUE             NC2534.2
039600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2534.2
039700 01  XXINFO.                                                      NC2534.2
039800     02 FILLER                       PIC X(19)  VALUE             NC2534.2
039900            "*** INFORMATION ***".                                NC2534.2
040000     02 INFO-TEXT.                                                NC2534.2
040100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2534.2
040200       04 XXCOMPUTED                 PIC X(20).                   NC2534.2
040300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2534.2
040400       04 XXCORRECT                  PIC X(20).                   NC2534.2
040500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2534.2
040600 01  HYPHEN-LINE.                                                 NC2534.2
040700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2534.2
040800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2534.2
040900-    "*****************************************".                 NC2534.2
041000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2534.2
041100-    "******************************".                            NC2534.2
041200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2534.2
041300     "NC253A".                                                    NC2534.2
041400 PROCEDURE DIVISION.                                              NC2534.2
041500 CCVS1 SECTION.                                                   NC2534.2
041600 OPEN-FILES.                                                      NC2534.2
041700     OPEN     OUTPUT PRINT-FILE.                                  NC2534.2
041800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2534.2
041900     MOVE    SPACE TO TEST-RESULTS.                               NC2534.2
042000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2534.2
042100     GO TO CCVS1-EXIT.                                            NC2534.2
042200 CLOSE-FILES.                                                     NC2534.2
042300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2534.2
042400 TERMINATE-CCVS.                                                  NC2534.2
042500*    EXIT PROGRAM.                                                NC2534.2
042600*TERMINATE-CALL.                                                  NC2534.2
042700     STOP     RUN.                                                NC2534.2
042800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2534.2
042900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2534.2
043000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2534.2
043100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2534.2
043200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2534.2
043300 PRINT-DETAIL.                                                    NC2534.2
043400     IF REC-CT NOT EQUAL TO ZERO                                  NC2534.2
043500             MOVE "." TO PARDOT-X                                 NC2534.2
043600             MOVE REC-CT TO DOTVALUE.                             NC2534.2
043700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2534.2
043800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2534.2
043900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2534.2
044000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2534.2
044100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2534.2
044200     MOVE SPACE TO CORRECT-X.                                     NC2534.2
044300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2534.2
044400     MOVE     SPACE TO RE-MARK.                                   NC2534.2
044500 HEAD-ROUTINE.                                                    NC2534.2
044600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2534.2
044700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2534.2
044800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2534.2
044900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2534.2
045000 COLUMN-NAMES-ROUTINE.                                            NC2534.2
045100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2534.2
045200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2534.2
045300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2534.2
045400 END-ROUTINE.                                                     NC2534.2
045500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2534.2
045600 END-RTN-EXIT.                                                    NC2534.2
045700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2534.2
045800 END-ROUTINE-1.                                                   NC2534.2
045900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2534.2
046000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2534.2
046100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2534.2
046200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2534.2
046300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2534.2
046400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2534.2
046500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2534.2
046600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2534.2
046700  END-ROUTINE-12.                                                 NC2534.2
046800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2534.2
046900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2534.2
047000         MOVE "NO " TO ERROR-TOTAL                                NC2534.2
047100         ELSE                                                     NC2534.2
047200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2534.2
047300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2534.2
047400     PERFORM WRITE-LINE.                                          NC2534.2
047500 END-ROUTINE-13.                                                  NC2534.2
047600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2534.2
047700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2534.2
047800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2534.2
047900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2534.2
048000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2534.2
048100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2534.2
048200          MOVE "NO " TO ERROR-TOTAL                               NC2534.2
048300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2534.2
048400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2534.2
048500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2534.2
048600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2534.2
048700 WRITE-LINE.                                                      NC2534.2
048800     ADD 1 TO RECORD-COUNT.                                       NC2534.2
048900*    IF RECORD-COUNT GREATER 50                                   NC2534.2
049000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2534.2
049100*        MOVE SPACE TO DUMMY-RECORD                               NC2534.2
049200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2534.2
049300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2534.2
049400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2534.2
049500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2534.2
049600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2534.2
049700*        MOVE ZERO TO RECORD-COUNT.                               NC2534.2
049800     PERFORM WRT-LN.                                              NC2534.2
049900 WRT-LN.                                                          NC2534.2
050000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2534.2
050100     MOVE SPACE TO DUMMY-RECORD.                                  NC2534.2
050200 BLANK-LINE-PRINT.                                                NC2534.2
050300     PERFORM WRT-LN.                                              NC2534.2
050400 FAIL-ROUTINE.                                                    NC2534.2
050500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2534.2
050600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2534.2
050700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2534.2
050800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2534.2
050900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2534.2
051000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2534.2
051100     GO TO  FAIL-ROUTINE-EX.                                      NC2534.2
051200 FAIL-ROUTINE-WRITE.                                              NC2534.2
051300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2534.2
051400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2534.2
051500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2534.2
051600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2534.2
051700 FAIL-ROUTINE-EX. EXIT.                                           NC2534.2
051800 BAIL-OUT.                                                        NC2534.2
051900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2534.2
052000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2534.2
052100 BAIL-OUT-WRITE.                                                  NC2534.2
052200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2534.2
052300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2534.2
052400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2534.2
052500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2534.2
052600 BAIL-OUT-EX. EXIT.                                               NC2534.2
052700 CCVS1-EXIT.                                                      NC2534.2
052800     EXIT.                                                        NC2534.2
052900*                                                                 NC2534.2
053000 SECT-NC253A-001 SECTION.                                         NC2534.2
053100 BUILD-TABLE1.                                                    NC2534.2
053200     MOVE 06 TO RECORD1 OF TABLE1.                                NC2534.2
053300     MOVE 01 TO RECORD2 OF TABLE1 (1).                            NC2534.2
053400     MOVE 02 TO RECORD2 OF TABLE1 (2).                            NC2534.2
053500     MOVE 07 TO RECORD3 OF TABLE1.                                NC2534.2
053600 BUILD-TABLE2.                                                    NC2534.2
053700     MOVE 08 TO RECORD1 OF TABLE2.                                NC2534.2
053800     MOVE 03 TO RECORD2 OF TABLE2 (1).                            NC2534.2
053900     MOVE 04 TO RECORD2 OF TABLE2 (2).                            NC2534.2
054000     MOVE 09 TO RECORD3 OF TABLE2.                                NC2534.2
054100*                                                                 NC2534.2
054200 SUB-INIT-F3-1.                                                   NC2534.2
054300     PERFORM END-ROUTINE.                                         NC2534.2
054400     MOVE "SUB-TEST-F3-1" TO PAR-NAME.                            NC2534.2
054500     MOVE   "VI-134 6.25.4 GR3" TO ANSI-REFERENCE.                NC2534.2
054600     MOVE   "SUBTRACT SERIES " TO FEATURE.                        NC2534.2
054700     MOVE ZERO TO GRP-FOR-ADD-CORR-R.                             NC2534.2
054800     MOVE   11  TO  ADD-CORR-1 OF GRP-FOR-ADD-CORR-1.             NC2534.2
054900     MOVE   22  TO  ADD-CORR-2 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055000     MOVE   33  TO  ADD-CORR-3 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055100     MOVE   44  TO  ADD-CORR-4 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055200     MOVE   55  TO  ADD-CORR-5 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055300     MOVE   66  TO  ADD-CORR-6 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055400     MOVE   77  TO  ADD-CORR-7 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055500     MOVE   88  TO  ADD-CORR-8 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055600     MOVE   99  TO  ADD-CORR-9 OF GRP-FOR-ADD-CORR-1.             NC2534.2
055700 SUB-TEST-F3-1.                                                   NC2534.2
055800     SUBTRACT CORRESPONDING GRP-FOR-ADD-CORR-1 FROM               NC2534.2
055900         GRP-FOR-ADD-CORR-R.                                      NC2534.2
056000     IF GRP-FOR-ADD-CORR-R EQUAL TO "11223344506677889900"        NC2534.2
056100         PERFORM PASS                                             NC2534.2
056200         GO TO SUB-WRITE-F3-1.                                    NC2534.2
056300     GO TO SUB-FAIL-F3-1.                                         NC2534.2
056400 SUB-DELETE-F3-1.                                                 NC2534.2
056500     PERFORM DE-LETE.                                             NC2534.2
056600     GO TO SUB-WRITE-F3-1.                                        NC2534.2
056700 SUB-FAIL-F3-1.                                                   NC2534.2
056800     MOVE GRP-FOR-ADD-CORR-R TO COMPUTED-A.                       NC2534.2
056900     MOVE "11223344506677889900" TO CORRECT-A.                    NC2534.2
057000     PERFORM FAIL.                                                NC2534.2
057100 SUB-WRITE-F3-1.                                                  NC2534.2
057200     PERFORM PRINT-DETAIL.                                        NC2534.2
057300*                                                                 NC2534.2
057400 SUB-INIT-F3-2.                                                   NC2534.2
057500     MOVE "SUB-TEST-F3-2" TO PAR-NAME.                            NC2534.2
057600     MOVE ZERO TO GRP-FOR-ADD-CORR-R.                             NC2534.2
057700 SUB-TEST-F3-2.                                                   NC2534.2
057800     SUBTRACT CORRESPONDING GRP-ADD-SUB-CORR FROM                 NC2534.2
057900         GRP-FOR-ADD-CORR-R ROUNDED.                              NC2534.2
058000     IF GRP-FOR-ADD-CORR-R EQUAL TO "11223344606677889900"        NC2534.2
058100         PERFORM PASS                                             NC2534.2
058200         GO TO SUB-WRITE-F3-2.                                    NC2534.2
058300     GO TO SUB-FAIL-F3-2.                                         NC2534.2
058400 SUB-DELETE-F3-2.                                                 NC2534.2
058500     PERFORM DE-LETE.                                             NC2534.2
058600     GO TO SUB-WRITE-F3-2.                                        NC2534.2
058700 SUB-FAIL-F3-2.                                                   NC2534.2
058800     MOVE GRP-FOR-ADD-CORR-R TO COMPUTED-A.                       NC2534.2
058900     MOVE "11223344606677889900" TO CORRECT-A.                    NC2534.2
059000     PERFORM FAIL.                                                NC2534.2
059100 SUB-WRITE-F3-2.                                                  NC2534.2
059200     PERFORM PRINT-DETAIL.                                        NC2534.2
059300*                                                                 NC2534.2
059400 SUB-INIT-F3-3.                                                   NC2534.2
059500     MOVE  1      TO SUBTR-13 OF SUBTR-12.                        NC2534.2
059600     MOVE -1.725  TO SUBTR-14 OF SUBTR-12.                        NC2534.2
059700     MOVE 76.76   TO SUBTR-15 OF SUBTR-12.                        NC2534.2
059800     MOVE  2      TO SUBTR-13 OF SUBTR-16.                        NC2534.2
059900     MOVE  0.23   TO SUBTR-14 OF SUBTR-16.                        NC2534.2
060000     MOVE  1      TO SUBTR-15 OF SUBTR-16.                        NC2534.2
060100 SUB-INIT-F3-3-1.                                                 NC2534.2
060200     MOVE "SUB-TEST-F3-3-1" TO PAR-NAME.                          NC2534.2
060300     MOVE SPACE TO WRK-AN-00001.                                  NC2534.2
060400 SUB-TEST-F3-3-1.                                                 NC2534.2
060500     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED ON     NC2534.2
060600     SIZE ERROR                                                   NC2534.2
060700           MOVE "G" TO WRK-AN-00001.                              NC2534.2
060800     IF WRK-AN-00001 EQUAL TO "G"                                 NC2534.2
060900           PERFORM PASS                                           NC2534.2
061000           GO TO SUB-WRITE-F3-3-1.                                NC2534.2
061100     GO TO SUB-FAIL-F3-3-1.                                       NC2534.2
061200 SUB-DELETE-F3-3-1.                                               NC2534.2
061300     PERFORM DE-LETE.                                             NC2534.2
061400     GO TO SUB-WRITE-F3-3-1.                                      NC2534.2
061500 SUB-FAIL-F3-3-1.                                                 NC2534.2
061600     PERFORM FAIL.                                                NC2534.2
061700     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC2534.2
061800 SUB-WRITE-F3-3-1.                                                NC2534.2
061900     PERFORM PRINT-DETAIL.                                        NC2534.2
062000*                                                                 NC2534.2
062100 SUB-INIT-F3-3-2.                                                 NC2534.2
062200     MOVE "SUB-TEST-F3-3-2" TO PAR-NAME.                          NC2534.2
062300 SUB-TEST-F3-3-2.                                                 NC2534.2
062400     IF SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                       NC2534.2
062500             GO TO SUB-FAIL-F3-3-2.                               NC2534.2
062600     PERFORM PASS.                                                NC2534.2
062700     GO TO SUB-WRITE-F3-3-2.                                      NC2534.2
062800 SUB-DELETE-F3-3-2.                                               NC2534.2
062900     PERFORM DE-LETE.                                             NC2534.2
063000     GO TO SUB-WRITE-F3-3-2.                                      NC2534.2
063100 SUB-FAIL-F3-3-2.                                                 NC2534.2
063200     PERFORM FAIL.                                                NC2534.2
063300     MOVE SUBTR-13 OF SUBTR-16 TO COMPUTED-N.                     NC2534.2
063400     MOVE "+1" TO CORRECT-A.                                      NC2534.2
063500     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
063600        TO RE-MARK.                                               NC2534.2
063700 SUB-WRITE-F3-3-2.                                                NC2534.2
063800     PERFORM PRINT-DETAIL.                                        NC2534.2
063900*                                                                 NC2534.2
064000 SUB-INIT-F3-3-3.                                                 NC2534.2
064100     MOVE "SUB-TEST-F3-3-3" TO PAR-NAME.                          NC2534.2
064200 SUB-TEST-F3-3-3.                                                 NC2534.2
064300     IF SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96                 NC2534.2
064400             GO TO SUB-FAIL-F3-3-3.                               NC2534.2
064500     PERFORM PASS.                                                NC2534.2
064600     GO TO SUB-WRITE-F3-3-3.                                      NC2534.2
064700 SUB-DELETE-F3-3-3.                                               NC2534.2
064800     PERFORM DE-LETE.                                             NC2534.2
064900     GO TO SUB-WRITE-F3-3-3.                                      NC2534.2
065000 SUB-FAIL-F3-3-3.                                                 NC2534.2
065100     PERFORM FAIL.                                                NC2534.2
065200     MOVE SUBTR-14 OF SUBTR-16 TO COMPUTED-N.                     NC2534.2
065300     MOVE "+1.96" TO CORRECT-A.                                   NC2534.2
065400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
065500        TO RE-MARK.                                               NC2534.2
065600 SUB-WRITE-F3-3-3.                                                NC2534.2
065700     PERFORM PRINT-DETAIL.                                        NC2534.2
065800*                                                                 NC2534.2
065900 SUB-INIT-3-3-4.                                                  NC2534.2
066000     MOVE "SUB-TEST-3-3-4" TO PAR-NAME.                           NC2534.2
066100 SUB-TEST-3-3-4.                                                  NC2534.2
066200     IF SUBTR-15 OF SUBTR-16 NOT EQUAL TO 1                       NC2534.2
066300            GO TO SUB-FAIL-3-3-4.                                 NC2534.2
066400     PERFORM PASS                                                 NC2534.2
066500     GO TO SUB-WRITE-3-3-4.                                       NC2534.2
066600 SUB-DELETE-3-3-4.                                                NC2534.2
066700     PERFORM DE-LETE.                                             NC2534.2
066800     GO TO SUB-WRITE-3-3-4.                                       NC2534.2
066900 SUB-FAIL-3-3-4.                                                  NC2534.2
067000     PERFORM FAIL.                                                NC2534.2
067100     MOVE SUBTR-15 OF SUBTR-16 TO COMPUTED-N.                     NC2534.2
067200     MOVE "+1" TO CORRECT-A.                                      NC2534.2
067300     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC2534.2
067400 SUB-WRITE-3-3-4.                                                 NC2534.2
067500     PERFORM PRINT-DETAIL.                                        NC2534.2
067600*                                                                 NC2534.2
067700 SUB-INIT-F3-4.                                                   NC2534.2
067800     MOVE "SUB-TEST-F3-4" TO PAR-NAME.                            NC2534.2
067900     MOVE "050506060000" TO CORR-DATA-2.                          NC2534.2
068000     MOVE "999999999999" TO CORR-DATA-3.                          NC2534.2
068100 SUB-TEST-F3-4.                                                   NC2534.2
068200     SUBTRACT CORRESPONDING CORR-DATA-2 FROM CORR-DATA-3.         NC2534.2
068300     IF CORR-DATA-3 EQUAL TO "939399999494"                       NC2534.2
068400         PERFORM PASS                                             NC2534.2
068500         GO TO SUB-WRITE-F3-4.                                    NC2534.2
068600     GO TO SUB-FAIL-F3-4.                                         NC2534.2
068700 SUB-DELETE-F3-4.                                                 NC2534.2
068800     PERFORM DE-LETE.                                             NC2534.2
068900     GO TO SUB-WRITE-F3-4.                                        NC2534.2
069000 SUB-FAIL-F3-4.                                                   NC2534.2
069100     MOVE 939399999494 TO CORRECT-A.                              NC2534.2
069200     MOVE CORR-DATA-3 TO COMPUTED-A.                              NC2534.2
069300     PERFORM FAIL.                                                NC2534.2
069400 SUB-WRITE-F3-4.                                                  NC2534.2
069500     PERFORM PRINT-DETAIL.                                        NC2534.2
069600*                                                                 NC2534.2
069700 SUB-INIT-F3-5.                                                   NC2534.2
069800     MOVE "SUB-TEST-F3-5" TO PAR-NAME.                            NC2534.2
069900     MOVE 999955995511 TO CORR-DATA-1.                            NC2534.2
070000     MOVE 123456107890 TO CORR-DATA-6.                            NC2534.2
070100 SUB-TEST-F3-5.                                                   NC2534.2
070200     SUBTRACT CORRESPONDING CORR-DATA-6 FROM CORR-DATA-1.         NC2534.2
070300     IF CORR-DATA-1 EQUAL TO "892155995511"                       NC2534.2
070400         PERFORM PASS                                             NC2534.2
070500         GO TO SUB-WRITE-F3-5.                                    NC2534.2
070600     GO TO SUB-FAIL-F3-5.                                         NC2534.2
070700 SUB-DELETE-F3-5.                                                 NC2534.2
070800     PERFORM DE-LETE.                                             NC2534.2
070900     GO TO SUB-WRITE-F3-5.                                        NC2534.2
071000 SUB-FAIL-F3-5.                                                   NC2534.2
071100     MOVE 892155995511 TO CORRECT-A.                              NC2534.2
071200     MOVE CORR-DATA-1 TO COMPUTED-A.                              NC2534.2
071300     PERFORM FAIL.                                                NC2534.2
071400 SUB-WRITE-F3-5.                                                  NC2534.2
071500     PERFORM PRINT-DETAIL.                                        NC2534.2
071600*                                                                 NC2534.2
071700 SUB-INIT-F3-6.                                                   NC2534.2
071800     MOVE "555555000055" TO CORR-DATA-6.                          NC2534.2
071900     MOVE "SUB-TEST-F3-6" TO PAR-NAME.                            NC2534.2
072000 SUB-TEST-F3-6.                                                   NC2534.2
072100     SUBTRACT CORRESPONDING CORR-DATA-6 FROM CORR-DATA-1          NC2534.2
072200     IF CORR-DATA-1 EQUAL TO 892155995511                         NC2534.2
072300         PERFORM PASS                                             NC2534.2
072400         GO TO SUB-WRITE-F3-6.                                    NC2534.2
072500     GO TO SUB-FAIL-F3-6.                                         NC2534.2
072600 SUB-DELETE-F3-6.                                                 NC2534.2
072700     PERFORM DE-LETE.                                             NC2534.2
072800     GO TO SUB-WRITE-F3-6.                                        NC2534.2
072900 SUB-FAIL-F3-6.                                                   NC2534.2
073000     MOVE 892155995511 TO CORRECT-A.                              NC2534.2
073100     MOVE CORR-DATA-1 TO COMPUTED-A.                              NC2534.2
073200     PERFORM FAIL.                                                NC2534.2
073300 SUB-WRITE-F3-6.                                                  NC2534.2
073400     PERFORM PRINT-DETAIL.                                        NC2534.2
073500*                                                                 NC2534.2
073600 SUB-INIT-F3-7.                                                   NC2534.2
073700     MOVE "SUB-TEST-F3-7" TO PAR-NAME.                            NC2534.2
073800     MOVE 99999999999999 TO CORR-DATA-5.                          NC2534.2
073900     MOVE 111111111111 TO CORR-DATA-1.                            NC2534.2
074000 SUB-TEST-F3-7.                                                   NC2534.2
074100     SUBTRACT CORRESPONDING CORR-DATA-1 FROM CORR-DATA-5.         NC2534.2
074200     IF CORR-DATA-5 EQUAL TO "88889999999999"                     NC2534.2
074300         PERFORM PASS                                             NC2534.2
074400         GO TO SUB-WRITE-F3-7.                                    NC2534.2
074500     GO TO SUB-FAIL-F3-7.                                         NC2534.2
074600 SUB-DELETE-F3-7.                                                 NC2534.2
074700     PERFORM DE-LETE.                                             NC2534.2
074800     GO TO SUB-WRITE-F3-7.                                        NC2534.2
074900 SUB-FAIL-F3-7.                                                   NC2534.2
075000     PERFORM FAIL.                                                NC2534.2
075100     MOVE CORR-DATA-5 TO COMPUTED-A.                              NC2534.2
075200     MOVE "88889999999999" TO CORRECT-A.                          NC2534.2
075300 SUB-WRITE-F3-7.                                                  NC2534.2
075400     PERFORM PRINT-DETAIL.                                        NC2534.2
075500*                                                                 NC2534.2
075600 SUB-INIT-F3-8.                                                   NC2534.2
075700     MOVE   "SUB-TEST-F3-8" TO PAR-NAME.                          NC2534.2
075800     MOVE   "VI-134 6.25.4 GR3" TO ANSI-REFERENCE.                NC2534.2
075900     PERFORM BUILD-TABLE1.                                        NC2534.2
076000     PERFORM BUILD-TABLE2.                                        NC2534.2
076100 SUB-TEST-F3-8-0.                                                 NC2534.2
076200     SUBTRACT CORRESPONDING TABLE1 FROM TABLE2.                   NC2534.2
076300 SUB-TEST-F3-8-1.                                                 NC2534.2
076400     IF      RECORD1 OF TABLE2 = 02                               NC2534.2
076500         AND RECORD2 OF TABLE2 (1) = 03                           NC2534.2
076600         AND RECORD2 OF TABLE2 (2) = 04                           NC2534.2
076700         AND RECORD3 OF TABLE2 = 02                               NC2534.2
076800             PERFORM PASS                                         NC2534.2
076900             GO TO  SUB-WRITE-F3-8.                               NC2534.2
077000     GO TO   SUB-FAIL-F3-8.                                       NC2534.2
077100 SUB-DELETE-F3-8.                                                 NC2534.2
077200     PERFORM DE-LETE.                                             NC2534.2
077300     GO TO   SUB-WRITE-F3-8.                                      NC2534.2
077400 SUB-FAIL-F3-8.                                                   NC2534.2
077500     PERFORM FAIL.                                                NC2534.2
077600     MOVE    TABLE2 TO COMPUTED-A.                                NC2534.2
077700     MOVE  "02030402" TO CORRECT-A.                               NC2534.2
077800 SUB-WRITE-F3-8.                                                  NC2534.2
077900     PERFORM PRINT-DETAIL.                                        NC2534.2
078000*                                                                 NC2534.2
078100 SUB-INIT-F3-9.                                                   NC2534.2
078200*    ===-->  NO SIZE ERROR  <--===                                NC2534.2
078300     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
078400     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
078500     MOVE    0      TO REC-CT.                                    NC2534.2
078600     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
078700     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
078800     MOVE    6.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
078900     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
079000     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
079100     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
079200 SUB-TEST-F3-9-0.                                                 NC2534.2
079300     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
079400             ON SIZE ERROR                                        NC2534.2
079500             MOVE   "G" TO WRK-AN-00001.                          NC2534.2
079600*                                                                 NC2534.2
079700 SUB-INIT-F3-9-1.                                                 NC2534.2
079800     MOVE   "SUB-TEST-F3-9-1" TO PAR-NAME.                        NC2534.2
079900     ADD     1 TO REC-CT.                                         NC2534.2
080000 SUB-TEST-F3-9-1.                                                 NC2534.2
080100     IF      WRK-AN-00001    NOT = SPACE                          NC2534.2
080200             GO TO SUB-FAIL-F3-9-1.                               NC2534.2
080300     PERFORM PASS                                                 NC2534.2
080400     GO TO SUB-WRITE-F3-9-1.                                      NC2534.2
080500 SUB-DELETE-F3-9-1.                                               NC2534.2
080600     PERFORM DE-LETE.                                             NC2534.2
080700     GO TO SUB-WRITE-F3-9-1.                                      NC2534.2
080800 SUB-FAIL-F3-9-1.                                                 NC2534.2
080900     MOVE   "SUBTRACT CORRESPONDING FAILED"                       NC2534.2
081000          TO RE-MARK                                              NC2534.2
081100     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
081200     MOVE    SPACE TO CORRECT-X                                   NC2534.2
081300     PERFORM FAIL.                                                NC2534.2
081400 SUB-WRITE-F3-9-1.                                                NC2534.2
081500     PERFORM PRINT-DETAIL.                                        NC2534.2
081600*                                                                 NC2534.2
081700 SUB-INIT-F3-9-2.                                                 NC2534.2
081800     MOVE  "SUB-TEST-F3-9-2" TO PAR-NAME.                         NC2534.2
081900     ADD    1 TO REC-CT.                                          NC2534.2
082000 SUB-TEST-F3-9-2.                                                 NC2534.2
082100     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
082200            GO TO SUB-FAIL-F3-9-2.                                NC2534.2
082300     PERFORM PASS                                                 NC2534.2
082400     GO TO SUB-WRITE-F3-9-2.                                      NC2534.2
082500 SUB-DELETE-F3-9-2.                                               NC2534.2
082600     PERFORM DE-LETE.                                             NC2534.2
082700     GO TO SUB-WRITE-F3-9-2.                                      NC2534.2
082800 SUB-FAIL-F3-9-2.                                                 NC2534.2
082900     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
083000     MOVE   "+1" TO CORRECT-A                                     NC2534.2
083100     MOVE "SUBTRACT CORRESPONDING FAILED"                         NC2534.2
083200           TO RE-MARK                                             NC2534.2
083300     PERFORM FAIL.                                                NC2534.2
083400 SUB-WRITE-F3-9-2.                                                NC2534.2
083500     PERFORM PRINT-DETAIL.                                        NC2534.2
083600*                                                                 NC2534.2
083700 SUB-INIT-F3-9-3.                                                 NC2534.2
083800     MOVE  "SUB-TEST-F3-9-3" TO PAR-NAME.                         NC2534.2
083900     ADD    1 TO REC-CT.                                          NC2534.2
084000 SUB-TEST-F3-9-3.                                                 NC2534.2
084100     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
084200            GO TO SUB-FAIL-F3-9-3.                                NC2534.2
084300     PERFORM PASS                                                 NC2534.2
084400     GO TO SUB-WRITE-F3-9-3.                                      NC2534.2
084500 SUB-DELETE-F3-9-3.                                               NC2534.2
084600     PERFORM DE-LETE.                                             NC2534.2
084700     GO TO SUB-WRITE-F3-9-3.                                      NC2534.2
084800 SUB-FAIL-F3-9-3.                                                 NC2534.2
084900     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
085000     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
085100     MOVE "SUBTRACT CORRESPONDING FAILED"                         NC2534.2
085200          TO RE-MARK                                              NC2534.2
085300     PERFORM FAIL.                                                NC2534.2
085400 SUB-WRITE-F3-9-3.                                                NC2534.2
085500     PERFORM PRINT-DETAIL.                                        NC2534.2
085600*                                                                 NC2534.2
085700 SUB-INIT-F3-9-4.                                                 NC2534.2
085800     MOVE  "SUB-TEST-F3-9-4" TO PAR-NAME.                         NC2534.2
085900     ADD    1 TO REC-CT.                                          NC2534.2
086000 SUB-TEST-F3-9-4.                                                 NC2534.2
086100     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO -5.76               NC2534.2
086200            GO TO SUB-FAIL-F3-9-4.                                NC2534.2
086300     PERFORM PASS                                                 NC2534.2
086400     GO TO SUB-WRITE-F3-9-4.                                      NC2534.2
086500 SUB-DELETE-F3-9-4.                                               NC2534.2
086600     PERFORM DE-LETE.                                             NC2534.2
086700     GO TO SUB-WRITE-F3-9-4.                                      NC2534.2
086800 SUB-FAIL-F3-9-4.                                                 NC2534.2
086900     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
087000     MOVE   "-5.76" TO CORRECT-A                                  NC2534.2
087100     MOVE "SUBRACT CORRESPONDING FAILED"                          NC2534.2
087200          TO RE-MARK                                              NC2534.2
087300     PERFORM FAIL.                                                NC2534.2
087400 SUB-WRITE-F3-9-4.                                                NC2534.2
087500     PERFORM PRINT-DETAIL.                                        NC2534.2
087600*                                                                 NC2534.2
087700 SUB-INIT-F3-10.                                                  NC2534.2
087800*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
087900*    ===-->      SIZE ERROR        <--===                         NC2534.2
088000     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
088100     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
088200     MOVE    0      TO REC-CT.                                    NC2534.2
088300     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
088400     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
088500     MOVE   76.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
088600     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
088700     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
088800     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
088900 SUB-TEST-F3-10-0.                                                NC2534.2
089000     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
089100         NOT ON SIZE ERROR                                        NC2534.2
089200             MOVE   "G" TO WRK-AN-00001.                          NC2534.2
089300*                                                                 NC2534.2
089400 SUB-INIT-F3-10-1.                                                NC2534.2
089500     MOVE   "SUB-TEST-F3-10-1" TO PAR-NAME.                       NC2534.2
089600     ADD     1 TO REC-CT.                                         NC2534.2
089700 SUB-TEST-F3-10-1.                                                NC2534.2
089800     IF      WRK-AN-00001 EQUAL TO "G"                            NC2534.2
089900             GO TO SUB-FAIL-F3-10-1.                              NC2534.2
090000     PERFORM PASS                                                 NC2534.2
090100     GO TO SUB-WRITE-F3-10-1.                                     NC2534.2
090200 SUB-DELETE-F3-10-1.                                              NC2534.2
090300     PERFORM DE-LETE.                                             NC2534.2
090400     GO TO SUB-WRITE-F3-10-1.                                     NC2534.2
090500 SUB-FAIL-F3-10-1.                                                NC2534.2
090600     MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"            NC2534.2
090700          TO RE-MARK                                              NC2534.2
090800     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
090900     MOVE    SPACE TO CORRECT-X                                   NC2534.2
091000     PERFORM FAIL.                                                NC2534.2
091100 SUB-WRITE-F3-10-1.                                               NC2534.2
091200     PERFORM PRINT-DETAIL.                                        NC2534.2
091300*                                                                 NC2534.2
091400 SUB-INIT-F3-10-2.                                                NC2534.2
091500     MOVE  "SUB-TEST-F3-10-2" TO PAR-NAME.                        NC2534.2
091600     ADD    1 TO REC-CT.                                          NC2534.2
091700 SUB-TEST-F3-10-2.                                                NC2534.2
091800     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
091900            GO TO SUB-FAIL-F3-10-2.                               NC2534.2
092000     PERFORM PASS                                                 NC2534.2
092100     GO TO SUB-WRITE-F3-10-2.                                     NC2534.2
092200 SUB-DELETE-F3-10-2.                                              NC2534.2
092300     PERFORM DE-LETE.                                             NC2534.2
092400     GO TO SUB-WRITE-F3-10-2.                                     NC2534.2
092500 SUB-FAIL-F3-10-2.                                                NC2534.2
092600     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
092700     MOVE   "+1" TO CORRECT-A                                     NC2534.2
092800     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
092900          TO RE-MARK                                              NC2534.2
093000     PERFORM FAIL.                                                NC2534.2
093100 SUB-WRITE-F3-10-2.                                               NC2534.2
093200     PERFORM PRINT-DETAIL.                                        NC2534.2
093300*                                                                 NC2534.2
093400 SUB-INIT-F3-10-3.                                                NC2534.2
093500     MOVE  "SUB-TEST-F3-10-3" TO PAR-NAME.                        NC2534.2
093600     ADD    1 TO REC-CT.                                          NC2534.2
093700 SUB-TEST-F3-10-3.                                                NC2534.2
093800     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
093900            GO TO SUB-FAIL-F3-10-3.                               NC2534.2
094000     PERFORM PASS                                                 NC2534.2
094100     GO TO SUB-WRITE-F3-10-3.                                     NC2534.2
094200 SUB-DELETE-F3-10-3.                                              NC2534.2
094300     PERFORM DE-LETE.                                             NC2534.2
094400     GO TO SUB-WRITE-F3-10-3.                                     NC2534.2
094500 SUB-FAIL-F3-10-3.                                                NC2534.2
094600     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
094700     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
094800     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
094900          TO RE-MARK                                              NC2534.2
095000     PERFORM FAIL.                                                NC2534.2
095100 SUB-WRITE-F3-10-3.                                               NC2534.2
095200     PERFORM PRINT-DETAIL.                                        NC2534.2
095300*                                                                 NC2534.2
095400 SUB-INIT-F3-10-4.                                                NC2534.2
095500     MOVE  "SUB-TEST-F3-10-4" TO PAR-NAME.                        NC2534.2
095600     ADD    1 TO REC-CT.                                          NC2534.2
095700 SUB-TEST-F3-10-4.                                                NC2534.2
095800     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
095900            GO TO SUB-FAIL-F3-10-4.                               NC2534.2
096000     PERFORM PASS                                                 NC2534.2
096100     GO TO SUB-WRITE-F3-10-4.                                     NC2534.2
096200 SUB-DELETE-F3-10-4.                                              NC2534.2
096300     PERFORM DE-LETE.                                             NC2534.2
096400     GO TO SUB-WRITE-F3-10-4.                                     NC2534.2
096500 SUB-FAIL-F3-10-4.                                                NC2534.2
096600     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
096700     MOVE   "+1" TO CORRECT-A                                     NC2534.2
096800     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK             NC2534.2
096900     PERFORM FAIL.                                                NC2534.2
097000 SUB-WRITE-F3-10-4.                                               NC2534.2
097100     PERFORM PRINT-DETAIL.                                        NC2534.2
097200*                                                                 NC2534.2
097300 SUB-INIT-F3-11.                                                  NC2534.2
097400*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
097500*    ===-->   NO SIZE ERROR        <--===                         NC2534.2
097600     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
097700     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
097800     MOVE    0      TO REC-CT.                                    NC2534.2
097900     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
098000     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
098100     MOVE    6.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
098200     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
098300     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
098400     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
098500 SUB-TEST-F3-11-0.                                                NC2534.2
098600     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
098700         NOT ON SIZE ERROR                                        NC2534.2
098800             MOVE   "G" TO WRK-AN-00001.                          NC2534.2
098900*                                                                 NC2534.2
099000 SUB-INIT-F3-11-1.                                                NC2534.2
099100     MOVE   "SUB-TEST-F3-11-1" TO PAR-NAME.                       NC2534.2
099200     ADD     1 TO REC-CT.                                         NC2534.2
099300 SUB-TEST-F3-11-1.                                                NC2534.2
099400     IF      WRK-AN-00001 EQUAL TO SPACE                          NC2534.2
099500             GO TO SUB-FAIL-F3-11-1.                              NC2534.2
099600     PERFORM PASS                                                 NC2534.2
099700     GO TO SUB-WRITE-F3-11-1.                                     NC2534.2
099800 SUB-DELETE-F3-11-1.                                              NC2534.2
099900     PERFORM DE-LETE.                                             NC2534.2
100000     GO TO SUB-WRITE-F3-11-1.                                     NC2534.2
100100 SUB-FAIL-F3-11-1.                                                NC2534.2
100200     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"                NC2534.2
100300           TO RE-MARK                                             NC2534.2
100400     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
100500     MOVE   "G"    TO CORRECT-X                                   NC2534.2
100600     PERFORM FAIL.                                                NC2534.2
100700 SUB-WRITE-F3-11-1.                                               NC2534.2
100800     PERFORM PRINT-DETAIL.                                        NC2534.2
100900*                                                                 NC2534.2
101000 SUB-INIT-F3-11-2.                                                NC2534.2
101100     MOVE  "SUB-TEST-F3-11-1" TO PAR-NAME.                        NC2534.2
101200     ADD    1 TO REC-CT.                                          NC2534.2
101300 SUB-TEST-F3-11-2.                                                NC2534.2
101400     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
101500            GO TO SUB-FAIL-F3-11-2.                               NC2534.2
101600     PERFORM PASS                                                 NC2534.2
101700     GO TO SUB-WRITE-F3-11-2.                                     NC2534.2
101800 SUB-DELETE-F3-11-2.                                              NC2534.2
101900     PERFORM DE-LETE.                                             NC2534.2
102000     GO TO SUB-WRITE-F3-11-2.                                     NC2534.2
102100 SUB-FAIL-F3-11-2.                                                NC2534.2
102200     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
102300     MOVE   "+1" TO CORRECT-A                                     NC2534.2
102400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
102500          TO RE-MARK                                              NC2534.2
102600     PERFORM FAIL.                                                NC2534.2
102700 SUB-WRITE-F3-11-2.                                               NC2534.2
102800     PERFORM PRINT-DETAIL.                                        NC2534.2
102900*                                                                 NC2534.2
103000 SUB-INIT-F3-11-3.                                                NC2534.2
103100     MOVE  "SUB-TEST-F3-11-3" TO PAR-NAME.                        NC2534.2
103200     ADD    1 TO REC-CT.                                          NC2534.2
103300 SUB-TEST-F3-11-3.                                                NC2534.2
103400     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
103500            GO TO SUB-FAIL-F3-11-3.                               NC2534.2
103600     PERFORM PASS                                                 NC2534.2
103700     GO TO SUB-WRITE-F3-11-3.                                     NC2534.2
103800 SUB-DELETE-F3-11-3.                                              NC2534.2
103900     PERFORM DE-LETE.                                             NC2534.2
104000     GO TO SUB-WRITE-F3-11-3.                                     NC2534.2
104100 SUB-FAIL-F3-11-3.                                                NC2534.2
104200     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
104300     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
104400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
104500          TO RE-MARK                                              NC2534.2
104600     PERFORM FAIL.                                                NC2534.2
104700 SUB-WRITE-F3-11-3.                                               NC2534.2
104800     PERFORM PRINT-DETAIL.                                        NC2534.2
104900*                                                                 NC2534.2
105000 SUB-INIT-F3-11-4.                                                NC2534.2
105100     MOVE  "SUB-TEST-F3-11-4" TO PAR-NAME.                        NC2534.2
105200     ADD    1 TO REC-CT.                                          NC2534.2
105300 SUB-TEST-F3-11-4.                                                NC2534.2
105400     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO -5.76               NC2534.2
105500            GO TO SUB-FAIL-F3-11-4.                               NC2534.2
105600     PERFORM PASS                                                 NC2534.2
105700     GO TO SUB-WRITE-F3-11-4.                                     NC2534.2
105800 SUB-DELETE-F3-11-4.                                              NC2534.2
105900     PERFORM DE-LETE.                                             NC2534.2
106000     GO TO SUB-WRITE-F3-11-4.                                     NC2534.2
106100 SUB-FAIL-F3-11-4.                                                NC2534.2
106200     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
106300     MOVE   "-5.76" TO CORRECT-A                                  NC2534.2
106400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
106500          TO RE-MARK                                              NC2534.2
106600     PERFORM FAIL.                                                NC2534.2
106700 SUB-WRITE-F3-11-4.                                               NC2534.2
106800     PERFORM PRINT-DETAIL.                                        NC2534.2
106900*                                                                 NC2534.2
107000 SUB-INIT-F3-12.                                                  NC2534.2
107100*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
107200*    ===-->      SIZE ERROR        <--===                         NC2534.2
107300     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
107400     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
107500     MOVE    0      TO REC-CT.                                    NC2534.2
107600     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
107700     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
107800     MOVE   76.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
107900     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
108000     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
108100     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
108200 SUB-TEST-F3-12-0.                                                NC2534.2
108300     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
108400             ON SIZE ERROR                                        NC2534.2
108500             MOVE   "A" TO WRK-AN-00001                           NC2534.2
108600         NOT ON SIZE ERROR                                        NC2534.2
108700             MOVE   "B" TO WRK-AN-00001.                          NC2534.2
108800*                                                                 NC2534.2
108900 SUB-INIT-F3-12-1.                                                NC2534.2
109000     MOVE   "SUB-TEST-F3-12-1" TO PAR-NAME.                       NC2534.2
109100     ADD     1 TO REC-CT.                                         NC2534.2
109200 SUB-TEST-F3-12-1.                                                NC2534.2
109300     IF      WRK-AN-00001    NOT = "A"                            NC2534.2
109400             GO TO SUB-FAIL-F3-12-1.                              NC2534.2
109500     PERFORM PASS                                                 NC2534.2
109600     GO TO SUB-WRITE-F3-12-1.                                     NC2534.2
109700 SUB-DELETE-F3-12-1.                                              NC2534.2
109800     PERFORM DE-LETE.                                             NC2534.2
109900     GO TO SUB-WRITE-F3-12-1.                                     NC2534.2
110000 SUB-FAIL-F3-12-1.                                                NC2534.2
110100     MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"                    NC2534.2
110200          TO RE-MARK                                              NC2534.2
110300     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
110400     MOVE   "A"    TO CORRECT-X                                   NC2534.2
110500     PERFORM FAIL.                                                NC2534.2
110600 SUB-WRITE-F3-12-1.                                               NC2534.2
110700     PERFORM PRINT-DETAIL.                                        NC2534.2
110800*                                                                 NC2534.2
110900 SUB-INIT-F3-12-2.                                                NC2534.2
111000     MOVE  "SUB-TEST-F3-12-2" TO PAR-NAME.                        NC2534.2
111100     ADD    1 TO REC-CT.                                          NC2534.2
111200 SUB-TEST-F3-12-2.                                                NC2534.2
111300     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
111400            GO TO SUB-FAIL-F3-12-2.                               NC2534.2
111500     PERFORM PASS                                                 NC2534.2
111600     GO TO SUB-WRITE-F3-12-2.                                     NC2534.2
111700 SUB-DELETE-F3-12-2.                                              NC2534.2
111800     PERFORM DE-LETE.                                             NC2534.2
111900     GO TO SUB-WRITE-F3-12-2.                                     NC2534.2
112000 SUB-FAIL-F3-12-2.                                                NC2534.2
112100     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
112200     MOVE   "+1" TO CORRECT-A                                     NC2534.2
112300     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
112400          TO RE-MARK                                              NC2534.2
112500     PERFORM FAIL.                                                NC2534.2
112600 SUB-WRITE-F3-12-2.                                               NC2534.2
112700     PERFORM PRINT-DETAIL.                                        NC2534.2
112800*                                                                 NC2534.2
112900 SUB-INIT-F3-12-3.                                                NC2534.2
113000     MOVE  "SUB-TEST-F3-12-3" TO PAR-NAME.                        NC2534.2
113100     ADD    1 TO REC-CT.                                          NC2534.2
113200 SUB-TEST-F3-12-3.                                                NC2534.2
113300     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
113400            GO TO SUB-FAIL-F3-12-3.                               NC2534.2
113500     PERFORM PASS                                                 NC2534.2
113600     GO TO SUB-WRITE-F3-12-3.                                     NC2534.2
113700 SUB-DELETE-F3-12-3.                                              NC2534.2
113800     PERFORM DE-LETE.                                             NC2534.2
113900     GO TO SUB-WRITE-F3-12-3.                                     NC2534.2
114000 SUB-FAIL-F3-12-3.                                                NC2534.2
114100     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
114200     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
114300     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
114400          TO RE-MARK                                              NC2534.2
114500     PERFORM FAIL.                                                NC2534.2
114600 SUB-WRITE-F3-12-3.                                               NC2534.2
114700     PERFORM PRINT-DETAIL.                                        NC2534.2
114800*                                                                 NC2534.2
114900 SUB-INIT-F3-12-4.                                                NC2534.2
115000     MOVE  "SUB-TEST-F3-12-4" TO PAR-NAME.                        NC2534.2
115100     ADD    1 TO REC-CT.                                          NC2534.2
115200 SUB-TEST-F3-12-4.                                                NC2534.2
115300     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
115400            GO TO SUB-FAIL-F3-12-4.                               NC2534.2
115500     PERFORM PASS                                                 NC2534.2
115600     GO TO SUB-WRITE-F3-12-4.                                     NC2534.2
115700 SUB-DELETE-F3-12-4.                                              NC2534.2
115800     PERFORM DE-LETE.                                             NC2534.2
115900     GO TO SUB-WRITE-F3-12-4.                                     NC2534.2
116000 SUB-FAIL-F3-12-4.                                                NC2534.2
116100     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
116200     MOVE   "+1" TO CORRECT-A                                     NC2534.2
116300     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK             NC2534.2
116400     PERFORM FAIL.                                                NC2534.2
116500 SUB-WRITE-F3-12-4.                                               NC2534.2
116600     PERFORM PRINT-DETAIL.                                        NC2534.2
116700*                                                                 NC2534.2
116800 SUB-INIT-F3-13.                                                  NC2534.2
116900*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
117000*    ===-->   NO SIZE ERROR        <--===                         NC2534.2
117100     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
117200     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
117300     MOVE    0      TO REC-CT.                                    NC2534.2
117400     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
117500     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
117600     MOVE    6.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
117700     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
117800     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
117900     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
118000 SUB-TEST-F3-13-0.                                                NC2534.2
118100     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
118200             ON SIZE ERROR                                        NC2534.2
118300             MOVE   "A" TO WRK-AN-00001                           NC2534.2
118400         NOT ON SIZE ERROR                                        NC2534.2
118500             MOVE   "B" TO WRK-AN-00001.                          NC2534.2
118600*                                                                 NC2534.2
118700 SUB-INIT-F3-13-1.                                                NC2534.2
118800     MOVE   "SUB-TEST-F3-13-1" TO PAR-NAME.                       NC2534.2
118900     ADD     1 TO REC-CT.                                         NC2534.2
119000 SUB-TEST-F3-13-1.                                                NC2534.2
119100     IF      WRK-AN-00001    NOT = "B"                            NC2534.2
119200             GO TO SUB-FAIL-F3-13-1.                              NC2534.2
119300     PERFORM PASS                                                 NC2534.2
119400     GO TO SUB-WRITE-F3-13-1.                                     NC2534.2
119500 SUB-DELETE-F3-13-1.                                              NC2534.2
119600     PERFORM DE-LETE.                                             NC2534.2
119700     GO TO SUB-WRITE-F3-13-1.                                     NC2534.2
119800 SUB-FAIL-F3-13-1.                                                NC2534.2
119900     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"                NC2534.2
120000           TO RE-MARK                                             NC2534.2
120100     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
120200     MOVE   "B"    TO CORRECT-X                                   NC2534.2
120300     PERFORM FAIL.                                                NC2534.2
120400 SUB-WRITE-F3-13-1.                                               NC2534.2
120500     PERFORM PRINT-DETAIL.                                        NC2534.2
120600*                                                                 NC2534.2
120700 SUB-INIT-F3-13-2.                                                NC2534.2
120800     MOVE  "SUB-TEST-F3-13-2" TO PAR-NAME.                        NC2534.2
120900     ADD    1 TO REC-CT.                                          NC2534.2
121000 SUB-TEST-F3-13-2.                                                NC2534.2
121100     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
121200            GO TO SUB-FAIL-F3-13-2.                               NC2534.2
121300     PERFORM PASS                                                 NC2534.2
121400     GO TO SUB-WRITE-F3-13-2.                                     NC2534.2
121500 SUB-DELETE-F3-13-2.                                              NC2534.2
121600     PERFORM DE-LETE.                                             NC2534.2
121700     GO TO SUB-WRITE-F3-13-2.                                     NC2534.2
121800 SUB-FAIL-F3-13-2.                                                NC2534.2
121900            MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N            NC2534.2
122000            MOVE   "+1" TO CORRECT-A                              NC2534.2
122100            MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"NC2534.2
122200                 TO RE-MARK                                       NC2534.2
122300            PERFORM FAIL.                                         NC2534.2
122400 SUB-WRITE-F3-13-2.                                               NC2534.2
122500            PERFORM PRINT-DETAIL.                                 NC2534.2
122600*                                                                 NC2534.2
122700 SUB-INIT-F3-13-3.                                                NC2534.2
122800     MOVE  "SUB-TEST-F3-13-3" TO PAR-NAME.                        NC2534.2
122900     ADD    1 TO REC-CT.                                          NC2534.2
123000 SUB-TEST-F3-13-3.                                                NC2534.2
123100     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
123200            GO TO SUB-FAIL-F3-13-3.                               NC2534.2
123300     PERFORM PASS                                                 NC2534.2
123400     GO TO SUB-WRITE-F3-13-3.                                     NC2534.2
123500 SUB-DELETE-F3-13-3.                                              NC2534.2
123600     PERFORM DE-LETE.                                             NC2534.2
123700     GO TO SUB-WRITE-F3-13-3.                                     NC2534.2
123800 SUB-FAIL-F3-13-3.                                                NC2534.2
123900     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
124000     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
124100     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
124200          TO RE-MARK                                              NC2534.2
124300     PERFORM FAIL.                                                NC2534.2
124400 SUB-WRITE-F3-13-3.                                               NC2534.2
124500     PERFORM PRINT-DETAIL.                                        NC2534.2
124600*                                                                 NC2534.2
124700 SUB-INIT-F3-13-4.                                                NC2534.2
124800     MOVE  "SUB-TEST-F3-13-4" TO PAR-NAME.                        NC2534.2
124900     ADD    1 TO REC-CT.                                          NC2534.2
125000 SUB-TEST-F3-13-4.                                                NC2534.2
125100     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO -5.76               NC2534.2
125200            GO TO SUB-FAIL-F3-13-4.                               NC2534.2
125300     PERFORM PASS                                                 NC2534.2
125400     GO TO SUB-WRITE-F3-13-4.                                     NC2534.2
125500 SUB-FAIL-F3-13-4.                                                NC2534.2
125600     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
125700     MOVE   "-5.76" TO CORRECT-A                                  NC2534.2
125800     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
125900        TO RE-MARK                                                NC2534.2
126000     PERFORM FAIL.                                                NC2534.2
126100 SUB-WRITE-F3-13-4.                                               NC2534.2
126200     PERFORM PRINT-DETAIL.                                        NC2534.2
126300*                                                                 NC2534.2
126400 SUB-INIT-F3-14.                                                  NC2534.2
126500*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
126600*    ===-->      SIZE ERROR        <--===                         NC2534.2
126700     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
126800     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
126900     MOVE    SPACE  TO WRK-XN-00001.                              NC2534.2
127000     MOVE    0      TO REC-CT.                                    NC2534.2
127100     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
127200     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
127300     MOVE   76.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
127400     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
127500     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
127600     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
127700 SUB-TEST-F3-14-0.                                                NC2534.2
127800     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
127900             ON SIZE ERROR                                        NC2534.2
128000             MOVE   "A" TO WRK-AN-00001                           NC2534.2
128100     END-SUBTRACT                                                 NC2534.2
128200     MOVE   "Z"  TO WRK-XN-00001.                                 NC2534.2
128300*                                                                 NC2534.2
128400 SUB-INIT-F3-14-1.                                                NC2534.2
128500     MOVE   "SUB-TEST-F3-14-1" TO PAR-NAME.                       NC2534.2
128600     ADD     1 TO REC-CT.                                         NC2534.2
128700 SUB-TEST-F3-14-1.                                                NC2534.2
128800     IF      WRK-AN-00001    NOT = "A"                            NC2534.2
128900             GO TO SUB-FAIL-F3-14-1.                              NC2534.2
129000     PERFORM PASS                                                 NC2534.2
129100     GO TO SUB-WRITE-F3-14-1.                                     NC2534.2
129200 SUB-DELETE-F3-14-1.                                              NC2534.2
129300     PERFORM DE-LETE.                                             NC2534.2
129400     GO TO SUB-WRITE-F3-14-1.                                     NC2534.2
129500 SUB-FAIL-F3-14-1.                                                NC2534.2
129600     MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"                    NC2534.2
129700          TO RE-MARK                                              NC2534.2
129800     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
129900     MOVE   "A"    TO CORRECT-X                                   NC2534.2
130000     PERFORM FAIL.                                                NC2534.2
130100 SUB-WRITE-F3-14-1.                                               NC2534.2
130200     PERFORM PRINT-DETAIL.                                        NC2534.2
130300*                                                                 NC2534.2
130400 SUB-INIT-F3-14-2.                                                NC2534.2
130500     MOVE  "SUB-TEST-F3-14-2" TO PAR-NAME.                        NC2534.2
130600     ADD    1 TO REC-CT.                                          NC2534.2
130700 SUB-TEST-F3-14-2.                                                NC2534.2
130800     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
130900            GO TO SUB-FAIL-F3-14-2.                               NC2534.2
131000     PERFORM PASS                                                 NC2534.2
131100     GO TO SUB-WRITE-F3-14-2.                                     NC2534.2
131200 SUB-DELETE-F3-14-2.                                              NC2534.2
131300     PERFORM DE-LETE.                                             NC2534.2
131400     GO TO SUB-WRITE-F3-14-2.                                     NC2534.2
131500 SUB-FAIL-F3-14-2.                                                NC2534.2
131600     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
131700     MOVE   "+1" TO CORRECT-A                                     NC2534.2
131800     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
131900          TO RE-MARK                                              NC2534.2
132000     PERFORM FAIL.                                                NC2534.2
132100 SUB-WRITE-F3-14-2.                                               NC2534.2
132200     PERFORM PRINT-DETAIL.                                        NC2534.2
132300*                                                                 NC2534.2
132400 SUB-INIT-F3-14-3.                                                NC2534.2
132500     MOVE  "SUB-TEST-F3-14-3" TO PAR-NAME.                        NC2534.2
132600     ADD    1 TO REC-CT.                                          NC2534.2
132700 SUB-TEST-F3-14-3.                                                NC2534.2
132800     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
132900            GO TO SUB-FAIL-F3-14-3.                               NC2534.2
133000     PERFORM PASS                                                 NC2534.2
133100     GO TO SUB-WRITE-F3-14-3.                                     NC2534.2
133200 SUB-DELETE-F3-14-3.                                              NC2534.2
133300     PERFORM DE-LETE.                                             NC2534.2
133400     GO TO SUB-WRITE-F3-14-3.                                     NC2534.2
133500 SUB-FAIL-F3-14-3.                                                NC2534.2
133600     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
133700     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
133800     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
133900          TO RE-MARK                                              NC2534.2
134000     PERFORM FAIL.                                                NC2534.2
134100 SUB-WRITE-F3-14-3.                                               NC2534.2
134200     PERFORM PRINT-DETAIL.                                        NC2534.2
134300*                                                                 NC2534.2
134400 SUB-INIT-F3-14-4.                                                NC2534.2
134500     MOVE  "SUB-TEST-F3-14-4" TO PAR-NAME.                        NC2534.2
134600     ADD     1 TO REC-CT.                                         NC2534.2
134700 SUB-TEST-F3-14-4.                                                NC2534.2
134800     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
134900            GO TO SUB-FAIL-F3-14-4.                               NC2534.2
135000     PERFORM PASS                                                 NC2534.2
135100     GO TO SUB-WRITE-F3-14-4.                                     NC2534.2
135200 SUB-DELETE-F3-14-4.                                              NC2534.2
135300     PERFORM DE-LETE.                                             NC2534.2
135400     GO TO SUB-WRITE-F3-14-4.                                     NC2534.2
135500 SUB-FAIL-F3-14-4.                                                NC2534.2
135600     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
135700     MOVE   "+1" TO CORRECT-A                                     NC2534.2
135800     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK           NC2534.2
135900     PERFORM FAIL.                                                NC2534.2
136000 SUB-WRITE-F3-14-4.                                               NC2534.2
136100     PERFORM PRINT-DETAIL.                                        NC2534.2
136200*                                                                 NC2534.2
136300 SUB-INIT-F3-14-5.                                                NC2534.2
136400     MOVE   "SUB-TEST-F3-14-5" TO PAR-NAME.                       NC2534.2
136500     ADD     1 TO REC-CT.                                         NC2534.2
136600 SUB-TEST-F3-14-5.                                                NC2534.2
136700     IF      WRK-XN-00001 NOT = "Z"                               NC2534.2
136800             GO TO SUB-FAIL-F3-14-5.                              NC2534.2
136900     PERFORM PASS                                                 NC2534.2
137000     GO TO SUB-WRITE-F3-14-5.                                     NC2534.2
137100 SUB-DELETE-F3-14-5.                                              NC2534.2
137200     PERFORM DE-LETE.                                             NC2534.2
137300     GO TO SUB-WRITE-F3-14-5.                                     NC2534.2
137400 SUB-FAIL-F3-14-5.                                                NC2534.2
137500     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2534.2
137600     MOVE   "Z"   TO COMPUTED-X                                   NC2534.2
137700     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2534.2
137800     PERFORM FAIL.                                                NC2534.2
137900 SUB-WRITE-F3-14-5.                                               NC2534.2
138000     PERFORM PRINT-DETAIL.                                        NC2534.2
138100*                                                                 NC2534.2
138200 SUB-INIT-F3-15.                                                  NC2534.2
138300*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
138400*    ===-->   NO SIZE ERROR        <--===                         NC2534.2
138500     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
138600     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
138700     MOVE    SPACE  TO WRK-XN-00001.                              NC2534.2
138800     MOVE    0      TO REC-CT.                                    NC2534.2
138900     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
139000     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
139100     MOVE    6.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
139200     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
139300     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
139400     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
139500 SUB-TEST-F3-15-0.                                                NC2534.2
139600     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
139700             ON SIZE ERROR                                        NC2534.2
139800             MOVE   "A" TO WRK-AN-00001                           NC2534.2
139900     END-SUBTRACT                                                 NC2534.2
140000     MOVE   "Z"  TO WRK-XN-00001.                                 NC2534.2
140100*                                                                 NC2534.2
140200 SUB-INIT-F3-15-1.                                                NC2534.2
140300     MOVE   "SUB-TEST-F3-15-1" TO PAR-NAME.                       NC2534.2
140400     ADD     1 TO REC-CT.                                         NC2534.2
140500 SUB-TEST-F3-15-1.                                                NC2534.2
140600     IF      WRK-AN-00001    = "A"                                NC2534.2
140700             GO TO SUB-FAIL-F3-15-1.                              NC2534.2
140800     PERFORM PASS                                                 NC2534.2
140900     GO TO SUB-WRITE-F3-15-1.                                     NC2534.2
141000 SUB-FAIL-F3-15-1.                                                NC2534.2
141100     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"                NC2534.2
141200          TO RE-MARK                                              NC2534.2
141300     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
141400     MOVE    SPACE TO CORRECT-X                                   NC2534.2
141500     PERFORM FAIL.                                                NC2534.2
141600 SUB-WRITE-F3-15-1.                                               NC2534.2
141700     PERFORM PRINT-DETAIL.                                        NC2534.2
141800*                                                                 NC2534.2
141900 SUB-INIT-F3-15-2.                                                NC2534.2
142000     MOVE  "SUB-TEST-F3-15-2" TO PAR-NAME.                        NC2534.2
142100     ADD    1 TO REC-CT.                                          NC2534.2
142200 SUB-TEST-F3-15-2.                                                NC2534.2
142300     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
142400            GO TO SUB-FAIL-F3-15-2.                               NC2534.2
142500     PERFORM PASS                                                 NC2534.2
142600     GO TO SUB-WRITE-F3-15-2.                                     NC2534.2
142700 SUB-DELETE-F3-15-2.                                              NC2534.2
142800     PERFORM DE-LETE.                                             NC2534.2
142900     GO TO SUB-WRITE-F3-15-2.                                     NC2534.2
143000 SUB-FAIL-F3-15-2.                                                NC2534.2
143100     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
143200     MOVE   "+1" TO CORRECT-A                                     NC2534.2
143300     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
143400          TO RE-MARK                                              NC2534.2
143500     PERFORM FAIL.                                                NC2534.2
143600 SUB-WRITE-F3-15-2.                                               NC2534.2
143700     PERFORM PRINT-DETAIL.                                        NC2534.2
143800*                                                                 NC2534.2
143900 SUB-INIT-F3-15-3.                                                NC2534.2
144000     MOVE  "SUB-TEST-F3-15-3" TO PAR-NAME.                        NC2534.2
144100     ADD    1 TO REC-CT.                                          NC2534.2
144200 SUB-TEST-F3-15-3.                                                NC2534.2
144300     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
144400            GO TO SUB-FAIL-F3-15-3.                               NC2534.2
144500     PERFORM PASS                                                 NC2534.2
144600     GO TO SUB-WRITE-F3-15-3.                                     NC2534.2
144700 SUB-DELETE-F3-15-3.                                              NC2534.2
144800     PERFORM DE-LETE.                                             NC2534.2
144900     GO TO SUB-WRITE-F3-15-3.                                     NC2534.2
145000 SUB-FAIL-F3-15-3.                                                NC2534.2
145100     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
145200     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
145300     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
145400          TO RE-MARK                                              NC2534.2
145500     PERFORM FAIL.                                                NC2534.2
145600 SUB-WRITE-F3-15-3.                                               NC2534.2
145700     PERFORM PRINT-DETAIL.                                        NC2534.2
145800*                                                                 NC2534.2
145900 SUB-INIT-F3-15-4.                                                NC2534.2
146000     MOVE  "SUB-TEST-F3-15-4" TO PAR-NAME.                        NC2534.2
146100     ADD     1 TO REC-CT.                                         NC2534.2
146200 SUB-TEST-F3-15-4.                                                NC2534.2
146300     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO -5.76               NC2534.2
146400            GO TO SUB-FAIL-F3-15-4.                               NC2534.2
146500     PERFORM PASS                                                 NC2534.2
146600     GO TO SUB-WRITE-F3-15-4.                                     NC2534.2
146700 SUB-DELETE-F3-15-4.                                              NC2534.2
146800     PERFORM DE-LETE.                                             NC2534.2
146900     GO TO SUB-WRITE-F3-15-4.                                     NC2534.2
147000 SUB-FAIL-F3-15-4.                                                NC2534.2
147100     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
147200     MOVE   "-5.76" TO CORRECT-A                                  NC2534.2
147300     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
147400          TO RE-MARK                                              NC2534.2
147500     PERFORM FAIL.                                                NC2534.2
147600 SUB-WRITE-F3-15-4.                                               NC2534.2
147700     PERFORM PRINT-DETAIL.                                        NC2534.2
147800*                                                                 NC2534.2
147900 SUB-INIT-F3-15-5.                                                NC2534.2
148000     MOVE   "SUB-TEST-F3-15-5" TO PAR-NAME.                       NC2534.2
148100     ADD     1 TO REC-CT.                                         NC2534.2
148200 SUB-TEST-F3-15-5.                                                NC2534.2
148300     IF      WRK-XN-00001 NOT = "Z"                               NC2534.2
148400             GO TO SUB-FAIL-F3-15-5.                              NC2534.2
148500     PERFORM PASS                                                 NC2534.2
148600     GO TO SUB-WRITE-F3-15-5.                                     NC2534.2
148700 SUB-DELETE-F3-15-5.                                              NC2534.2
148800     PERFORM DE-LETE.                                             NC2534.2
148900     GO TO SUB-WRITE-F3-15-5.                                     NC2534.2
149000 SUB-FAIL-F3-15-5.                                                NC2534.2
149100     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2534.2
149200     MOVE   "Z"   TO COMPUTED-X                                   NC2534.2
149300     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2534.2
149400     PERFORM FAIL.                                                NC2534.2
149500 SUB-WRITE-F3-15-5.                                               NC2534.2
149600     PERFORM PRINT-DETAIL.                                        NC2534.2
149700*                                                                 NC2534.2
149800 SUB-INIT-F3-16.                                                  NC2534.2
149900*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
150000*    ===-->      SIZE ERROR        <--===                         NC2534.2
150100     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
150200     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
150300     MOVE    SPACE  TO WRK-XN-00001.                              NC2534.2
150400     MOVE    0      TO REC-CT.                                    NC2534.2
150500     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
150600     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
150700     MOVE   76.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
150800     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
150900     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
151000     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
151100 SUB-TEST-F3-16-0.                                                NC2534.2
151200     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
151300             ON SIZE ERROR                                        NC2534.2
151400             MOVE   "A" TO WRK-AN-00001                           NC2534.2
151500         NOT ON SIZE ERROR                                        NC2534.2
151600             MOVE   "B" TO WRK-AN-00001                           NC2534.2
151700     END-SUBTRACT                                                 NC2534.2
151800     MOVE   "Z" TO WRK-XN-00001.                                  NC2534.2
151900*                                                                 NC2534.2
152000 SUB-INIT-F3-16-1.                                                NC2534.2
152100     MOVE   "SUB-TEST-F3-16-1" TO PAR-NAME.                       NC2534.2
152200     ADD     1 TO REC-CT.                                         NC2534.2
152300 SUB-TEST-F3-16-1.                                                NC2534.2
152400     IF      WRK-AN-00001    NOT = "A"                            NC2534.2
152500             GO TO SUB-FAIL-F3-16-1.                              NC2534.2
152600     PERFORM PASS                                                 NC2534.2
152700     GO TO SUB-WRITE-F3-16-1.                                     NC2534.2
152800 SUB-DELETE-F3-16-1.                                              NC2534.2
152900     PERFORM DE-LETE.                                             NC2534.2
153000     GO TO SUB-WRITE-F3-16-1.                                     NC2534.2
153100 SUB-FAIL-F3-16-1.                                                NC2534.2
153200     MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"                    NC2534.2
153300          TO RE-MARK                                              NC2534.2
153400     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
153500     MOVE   "A"    TO CORRECT-X                                   NC2534.2
153600     PERFORM FAIL.                                                NC2534.2
153700 SUB-WRITE-F3-16-1.                                               NC2534.2
153800     PERFORM PRINT-DETAIL.                                        NC2534.2
153900*                                                                 NC2534.2
154000 SUB-INIT-F3-16-2.                                                NC2534.2
154100     MOVE  "SUB-TEST-F3-16-2" TO PAR-NAME.                        NC2534.2
154200     ADD    1 TO REC-CT.                                          NC2534.2
154300 SUB-TEST-F3-16-2.                                                NC2534.2
154400     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
154500            GO TO SUB-FAIL-F3-16-2.                               NC2534.2
154600     PERFORM PASS                                                 NC2534.2
154700     GO TO SUB-WRITE-F3-16-2.                                     NC2534.2
154800 SUB-DELETE-F3-16-2.                                              NC2534.2
154900     PERFORM DE-LETE.                                             NC2534.2
155000     GO TO SUB-WRITE-F3-16-2.                                     NC2534.2
155100 SUB-FAIL-F3-16-2.                                                NC2534.2
155200     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
155300     MOVE   "+1" TO CORRECT-A                                     NC2534.2
155400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
155500          TO RE-MARK                                              NC2534.2
155600     PERFORM FAIL.                                                NC2534.2
155700 SUB-WRITE-F3-16-2.                                               NC2534.2
155800     PERFORM PRINT-DETAIL.                                        NC2534.2
155900*                                                                 NC2534.2
156000 SUB-INIT-F3-16-3.                                                NC2534.2
156100     MOVE  "SUB-TEST-F3-16-3" TO PAR-NAME.                        NC2534.2
156200     ADD    1 TO REC-CT.                                          NC2534.2
156300 SUB-TEST-F3-16-3.                                                NC2534.2
156400     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
156500            GO TO SUB-FAIL-F3-16-3.                               NC2534.2
156600     PERFORM PASS                                                 NC2534.2
156700     GO TO SUB-WRITE-F3-16-3.                                     NC2534.2
156800 SUB-DELETE-F3-16-3.                                              NC2534.2
156900     PERFORM DE-LETE.                                             NC2534.2
157000     GO TO SUB-WRITE-F3-16-3.                                     NC2534.2
157100 SUB-FAIL-F3-16-3.                                                NC2534.2
157200     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
157300     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
157400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
157500          TO RE-MARK                                              NC2534.2
157600     PERFORM FAIL.                                                NC2534.2
157700 SUB-WRITE-F3-16-3.                                               NC2534.2
157800     PERFORM PRINT-DETAIL.                                        NC2534.2
157900*                                                                 NC2534.2
158000 SUB-INIT-F3-16-4.                                                NC2534.2
158100     MOVE  "SUB-TEST-F3-16-4" TO PAR-NAME.                        NC2534.2
158200     ADD     1 TO REC-CT.                                         NC2534.2
158300 SUB-TEST-F3-16-4.                                                NC2534.2
158400     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
158500            GO TO SUB-FAIL-F3-16-4.                               NC2534.2
158600     PERFORM PASS                                                 NC2534.2
158700     GO TO SUB-WRITE-F3-16-4.                                     NC2534.2
158800 SUB-DELETE-F3-16-4.                                              NC2534.2
158900     PERFORM DE-LETE.                                             NC2534.2
159000     GO TO SUB-WRITE-F3-16-4.                                     NC2534.2
159100 SUB-FAIL-F3-16-4.                                                NC2534.2
159200     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
159300     MOVE   "+1" TO CORRECT-A                                     NC2534.2
159400     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK           NC2534.2
159500     PERFORM FAIL.                                                NC2534.2
159600 SUB-WRITE-F3-16-4.                                               NC2534.2
159700     PERFORM PRINT-DETAIL.                                        NC2534.2
159800*                                                                 NC2534.2
159900 SUB-INIT-F3-16-5.                                                NC2534.2
160000     MOVE   "SUB-TEST-F3-16-5" TO PAR-NAME.                       NC2534.2
160100     ADD     1 TO REC-CT.                                         NC2534.2
160200 SUB-TEST-F3-16-5.                                                NC2534.2
160300     IF      WRK-XN-00001 NOT = "Z"                               NC2534.2
160400             GO TO SUB-FAIL-F3-16-5.                              NC2534.2
160500     PERFORM PASS                                                 NC2534.2
160600     GO TO SUB-WRITE-F3-16-5.                                     NC2534.2
160700 SUB-DELETE-F3-16-5.                                              NC2534.2
160800     PERFORM DE-LETE.                                             NC2534.2
160900     GO TO SUB-WRITE-F3-16-5.                                     NC2534.2
161000 SUB-FAIL-F3-16-5.                                                NC2534.2
161100     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2534.2
161200     MOVE   "Z"   TO COMPUTED-X                                   NC2534.2
161300     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2534.2
161400     PERFORM FAIL.                                                NC2534.2
161500 SUB-WRITE-F3-16-5.                                               NC2534.2
161600     PERFORM PRINT-DETAIL.                                        NC2534.2
161700*                                                                 NC2534.2
161800 SUB-INIT-F3-17.                                                  NC2534.2
161900*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
162000*    ===-->   NO SIZE ERROR        <--===                         NC2534.2
162100     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
162200     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
162300     MOVE    SPACE  TO WRK-XN-00001.                              NC2534.2
162400     MOVE    0      TO REC-CT.                                    NC2534.2
162500     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
162600     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
162700     MOVE    6.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
162800     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
162900     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
163000     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
163100 SUB-TEST-F3-17-0.                                                NC2534.2
163200     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
163300             ON SIZE ERROR                                        NC2534.2
163400             MOVE   "A" TO WRK-AN-00001                           NC2534.2
163500         NOT ON SIZE ERROR                                        NC2534.2
163600             MOVE   "B" TO WRK-AN-00001                           NC2534.2
163700     END-SUBTRACT                                                 NC2534.2
163800     MOVE   "Z" TO WRK-XN-00001.                                  NC2534.2
163900*                                                                 NC2534.2
164000 SUB-INIT-F3-17-1.                                                NC2534.2
164100     MOVE   "SUB-TEST-F3-17-1" TO PAR-NAME.                       NC2534.2
164200     ADD     1 TO REC-CT.                                         NC2534.2
164300 SUB-TEST-F3-17-1.                                                NC2534.2
164400     IF      WRK-AN-00001    NOT = "B"                            NC2534.2
164500             GO TO SUB-FAIL-F3-17-1.                              NC2534.2
164600     PERFORM PASS                                                 NC2534.2
164700     GO TO SUB-WRITE-F3-17-1.                                     NC2534.2
164800 SUB-DELETE-F3-17-1.                                              NC2534.2
164900     PERFORM DE-LETE.                                             NC2534.2
165000     GO TO SUB-WRITE-F3-17-1.                                     NC2534.2
165100 SUB-FAIL-F3-17-1.                                                NC2534.2
165200     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"                NC2534.2
165300           TO RE-MARK                                             NC2534.2
165400     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
165500     MOVE   "B"    TO CORRECT-X                                   NC2534.2
165600     PERFORM FAIL.                                                NC2534.2
165700 SUB-WRITE-F3-17-1.                                               NC2534.2
165800     PERFORM PRINT-DETAIL.                                        NC2534.2
165900*                                                                 NC2534.2
166000 SUB-INIT-F3-17-2.                                                NC2534.2
166100     MOVE  "SUB-TEST-F3-17-2" TO PAR-NAME.                        NC2534.2
166200     ADD    1 TO REC-CT.                                          NC2534.2
166300 SUB-TEST-F3-17-2.                                                NC2534.2
166400     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
166500            GO TO SUB-FAIL-F3-17-2.                               NC2534.2
166600     PERFORM PASS                                                 NC2534.2
166700     GO TO SUB-WRITE-F3-17-2.                                     NC2534.2
166800 SUB-DELETE-F3-17-2.                                              NC2534.2
166900     PERFORM DE-LETE.                                             NC2534.2
167000     GO TO SUB-WRITE-F3-17-2.                                     NC2534.2
167100 SUB-FAIL-F3-17-2.                                                NC2534.2
167200     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
167300     MOVE   "+1" TO CORRECT-A                                     NC2534.2
167400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
167500          TO RE-MARK                                              NC2534.2
167600     PERFORM FAIL.                                                NC2534.2
167700 SUB-WRITE-F3-17-2.                                               NC2534.2
167800     PERFORM PRINT-DETAIL.                                        NC2534.2
167900*                                                                 NC2534.2
168000 SUB-INIT-F3-17-3.                                                NC2534.2
168100     MOVE  "SUB-TEST-F3-17-3" TO PAR-NAME.                        NC2534.2
168200     ADD    1 TO REC-CT.                                          NC2534.2
168300 SUB-TEST-F3-17-3.                                                NC2534.2
168400     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
168500            GO TO SUB-FAIL-F3-17-3.                               NC2534.2
168600     PERFORM PASS                                                 NC2534.2
168700     GO TO SUB-WRITE-F3-17-3.                                     NC2534.2
168800 SUB-DELETE-F3-17-3.                                              NC2534.2
168900     PERFORM DE-LETE.                                             NC2534.2
169000     GO TO SUB-WRITE-F3-17-3.                                     NC2534.2
169100 SUB-FAIL-F3-17-3.                                                NC2534.2
169200     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
169300     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
169400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
169500          TO RE-MARK                                              NC2534.2
169600     PERFORM FAIL.                                                NC2534.2
169700 SUB-WRITE-F3-17-3.                                               NC2534.2
169800     PERFORM PRINT-DETAIL.                                        NC2534.2
169900*                                                                 NC2534.2
170000 SUB-INIT-F3-17-4.                                                NC2534.2
170100     MOVE  "SUB-TEST-F3-17-4" TO PAR-NAME.                        NC2534.2
170200     ADD     1 TO REC-CT.                                         NC2534.2
170300 SUB-TEST-F3-17-4.                                                NC2534.2
170400     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO -5.76               NC2534.2
170500            GO TO SUB-FAIL-F3-17-4.                               NC2534.2
170600     PERFORM PASS                                                 NC2534.2
170700     GO TO SUB-WRITE-F3-17-4.                                     NC2534.2
170800 SUB-DELETE-F3-17-4.                                              NC2534.2
170900     PERFORM DE-LETE.                                             NC2534.2
171000     GO TO SUB-WRITE-F3-17-4.                                     NC2534.2
171100 SUB-FAIL-F3-17-4.                                                NC2534.2
171200     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
171300     MOVE   "-5.76" TO CORRECT-A                                  NC2534.2
171400     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
171500          TO RE-MARK                                              NC2534.2
171600     PERFORM FAIL.                                                NC2534.2
171700 SUB-WRITE-F3-17-4.                                               NC2534.2
171800     PERFORM PRINT-DETAIL.                                        NC2534.2
171900*                                                                 NC2534.2
172000 SUB-INIT-F3-17-5.                                                NC2534.2
172100     MOVE   "SUB-TEST-F3-17-5" TO PAR-NAME.                       NC2534.2
172200     ADD     1 TO REC-CT.                                         NC2534.2
172300 SUB-TEST-F3-17-5.                                                NC2534.2
172400     IF      WRK-XN-00001 NOT = "Z"                               NC2534.2
172500             GO TO SUB-FAIL-F3-17-5.                              NC2534.2
172600     PERFORM PASS                                                 NC2534.2
172700     GO TO SUB-WRITE-F3-17-5.                                     NC2534.2
172800 SUB-DELETE-F3-17-5.                                              NC2534.2
172900     PERFORM DE-LETE.                                             NC2534.2
173000     GO TO SUB-WRITE-F3-17-5.                                     NC2534.2
173100 SUB-FAIL-F3-17-5.                                                NC2534.2
173200     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2534.2
173300     MOVE   "Z"   TO COMPUTED-X                                   NC2534.2
173400     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2534.2
173500     PERFORM FAIL.                                                NC2534.2
173600 SUB-WRITE-F3-17-5.                                               NC2534.2
173700     PERFORM PRINT-DETAIL.                                        NC2534.2
173800*                                                                 NC2534.2
173900 SUB-INIT-F3-18.                                                  NC2534.2
174000*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
174100*    ===-->      SIZE ERROR        <--===                         NC2534.2
174200     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
174300     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
174400     MOVE    SPACE  TO WRK-XN-00001.                              NC2534.2
174500     MOVE    0      TO REC-CT.                                    NC2534.2
174600     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
174700     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
174800     MOVE   76.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
174900     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
175000     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
175100     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
175200 SUB-TEST-F3-18-0.                                                NC2534.2
175300     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
175400             ON SIZE ERROR                                        NC2534.2
175500             MOVE   "A" TO WRK-AN-00001                           NC2534.2
175600         NOT ON SIZE ERROR                                        NC2534.2
175700             MOVE   "B" TO WRK-AN-00001                           NC2534.2
175800     END-SUBTRACT                                                 NC2534.2
175900     MOVE   "Z" TO WRK-XN-00001.                                  NC2534.2
176000*                                                                 NC2534.2
176100 SUB-INIT-F3-18-1.                                                NC2534.2
176200     MOVE   "SUB-TEST-F3-18-1" TO PAR-NAME.                       NC2534.2
176300     ADD     1 TO REC-CT.                                         NC2534.2
176400 SUB-TEST-F3-18-1.                                                NC2534.2
176500     IF      WRK-AN-00001    NOT = "A"                            NC2534.2
176600             GO TO SUB-FAIL-F3-18-1.                              NC2534.2
176700     PERFORM PASS                                                 NC2534.2
176800     GO TO SUB-WRITE-F3-18-1.                                     NC2534.2
176900 SUB-DELETE-F3-18-1.                                              NC2534.2
177000     PERFORM DE-LETE.                                             NC2534.2
177100     GO TO SUB-WRITE-F3-18-1.                                     NC2534.2
177200 SUB-FAIL-F3-18-1.                                                NC2534.2
177300     MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"                    NC2534.2
177400          TO RE-MARK                                              NC2534.2
177500     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
177600     MOVE   "A"    TO CORRECT-X                                   NC2534.2
177700     PERFORM FAIL.                                                NC2534.2
177800 SUB-WRITE-F3-18-1.                                               NC2534.2
177900     PERFORM PRINT-DETAIL.                                        NC2534.2
178000*                                                                 NC2534.2
178100 SUB-INIT-F3-18-2.                                                NC2534.2
178200     MOVE  "SUB-TEST-F3-18-2" TO PAR-NAME.                        NC2534.2
178300     ADD    1 TO REC-CT.                                          NC2534.2
178400 SUB-TEST-F3-18-2.                                                NC2534.2
178500     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
178600            GO TO SUB-FAIL-F3-18-2.                               NC2534.2
178700     PERFORM PASS                                                 NC2534.2
178800     GO TO SUB-WRITE-F3-18-2.                                     NC2534.2
178900 SUB-FAIL-F3-18-2.                                                NC2534.2
179000     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
179100     MOVE   "+1" TO CORRECT-A                                     NC2534.2
179200     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
179300          TO RE-MARK                                              NC2534.2
179400     PERFORM FAIL.                                                NC2534.2
179500 SUB-WRITE-F3-18-2.                                               NC2534.2
179600     PERFORM PRINT-DETAIL.                                        NC2534.2
179700*                                                                 NC2534.2
179800 SUB-INIT-F3-18-3.                                                NC2534.2
179900     MOVE  "SUB-TEST-F3-18-3" TO PAR-NAME.                        NC2534.2
180000     ADD    1 TO REC-CT.                                          NC2534.2
180100 SUB-TEST-F3-18-3.                                                NC2534.2
180200     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
180300            GO TO SUB-FAIL-F3-18-3.                               NC2534.2
180400     PERFORM PASS                                                 NC2534.2
180500     GO TO SUB-WRITE-F3-18-3.                                     NC2534.2
180600 SUB-DELETE-F3-18-3.                                              NC2534.2
180700     PERFORM DE-LETE.                                             NC2534.2
180800     GO TO SUB-WRITE-F3-18-3.                                     NC2534.2
180900 SUB-FAIL-F3-18-3.                                                NC2534.2
181000     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
181100     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
181200     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
181300          TO RE-MARK                                              NC2534.2
181400     PERFORM FAIL.                                                NC2534.2
181500 SUB-WRITE-F3-18-3.                                               NC2534.2
181600     PERFORM PRINT-DETAIL.                                        NC2534.2
181700*                                                                 NC2534.2
181800 SUB-INIT-F3-18-4.                                                NC2534.2
181900     MOVE  "SUB-TEST-F3-18-4" TO PAR-NAME.                        NC2534.2
182000     ADD     1 TO REC-CT.                                         NC2534.2
182100 SUB-TEST-F3-18-4.                                                NC2534.2
182200     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
182300            GO TO SUB-FAIL-F3-18-4.                               NC2534.2
182400     PERFORM PASS                                                 NC2534.2
182500     GO TO SUB-WRITE-F3-18-4.                                     NC2534.2
182600 SUB-DELETE-F3-18-4.                                              NC2534.2
182700     PERFORM DE-LETE.                                             NC2534.2
182800     GO TO SUB-WRITE-F3-18-4.                                     NC2534.2
182900 SUB-FAIL-F3-18-4.                                                NC2534.2
183000     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
183100     MOVE   "+1" TO CORRECT-A                                     NC2534.2
183200     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK             NC2534.2
183300     PERFORM FAIL.                                                NC2534.2
183400 SUB-WRITE-F3-18-4.                                               NC2534.2
183500     PERFORM PRINT-DETAIL.                                        NC2534.2
183600*                                                                 NC2534.2
183700 SUB-INIT-F3-18-5.                                                NC2534.2
183800     MOVE   "SUB-TEST-F3-18-5" TO PAR-NAME.                       NC2534.2
183900     ADD     1 TO REC-CT.                                         NC2534.2
184000 SUB-TEST-F3-18-5.                                                NC2534.2
184100     IF      WRK-XN-00001 NOT = "Z"                               NC2534.2
184200             GO TO SUB-FAIL-F3-18-5.                              NC2534.2
184300     PERFORM PASS                                                 NC2534.2
184400     GO TO SUB-WRITE-F3-18-5.                                     NC2534.2
184500 SUB-DELETE-F3-18-5.                                              NC2534.2
184600     PERFORM DE-LETE.                                             NC2534.2
184700     GO TO SUB-WRITE-F3-18-5.                                     NC2534.2
184800 SUB-FAIL-F3-18-5.                                                NC2534.2
184900     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2534.2
185000     MOVE   "Z"   TO COMPUTED-X                                   NC2534.2
185100     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2534.2
185200     PERFORM FAIL.                                                NC2534.2
185300 SUB-WRITE-F3-18-5.                                               NC2534.2
185400     PERFORM PRINT-DETAIL.                                        NC2534.2
185500*                                                                 NC2534.2
185600 SUB-INIT-F3-19.                                                  NC2534.2
185700*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2534.2
185800*    ===-->   NO SIZE ERROR        <--===                         NC2534.2
185900     MOVE   "VI-133 6.25" TO ANSI-REFERENCE.                      NC2534.2
186000     MOVE    SPACE  TO WRK-AN-00001.                              NC2534.2
186100     MOVE    0      TO REC-CT.                                    NC2534.2
186200     MOVE    1      TO SUBTR-13 OF SUBTR-12.                      NC2534.2
186300     MOVE   -1.725  TO SUBTR-14 OF SUBTR-12.                      NC2534.2
186400     MOVE    6.76   TO SUBTR-15 OF SUBTR-12.                      NC2534.2
186500     MOVE    2      TO SUBTR-13 OF SUBTR-16.                      NC2534.2
186600     MOVE     .23   TO SUBTR-14 OF SUBTR-16.                      NC2534.2
186700     MOVE    1      TO SUBTR-15 OF SUBTR-16.                      NC2534.2
186800 SUB-TEST-F3-19-0.                                                NC2534.2
186900     SUBTRACT CORRESPONDING SUBTR-12 FROM SUBTR-16 ROUNDED        NC2534.2
187000             ON SIZE ERROR                                        NC2534.2
187100             MOVE   "A" TO WRK-AN-00001                           NC2534.2
187200         NOT ON SIZE ERROR                                        NC2534.2
187300             MOVE   "B" TO WRK-AN-00001                           NC2534.2
187400     END-SUBTRACT                                                 NC2534.2
187500     MOVE   "Z" TO WRK-XN-00001.                                  NC2534.2
187600*                                                                 NC2534.2
187700 SUB-INIT-F3-19-1.                                                NC2534.2
187800     MOVE   "SUB-TEST-F3-19-1" TO PAR-NAME.                       NC2534.2
187900     ADD     1 TO REC-CT.                                         NC2534.2
188000 SUB-TEST-F3-19-1.                                                NC2534.2
188100     IF      WRK-AN-00001    NOT = "B"                            NC2534.2
188200             GO TO SUB-FAIL-F3-19-1.                              NC2534.2
188300     PERFORM PASS                                                 NC2534.2
188400     GO TO SUB-WRITE-F3-19-1.                                     NC2534.2
188500 SUB-DELETE-F3-19-1.                                              NC2534.2
188600     PERFORM DE-LETE.                                             NC2534.2
188700     GO TO SUB-WRITE-F3-19-1.                                     NC2534.2
188800 SUB-FAIL-F3-19-1.                                                NC2534.2
188900     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"                NC2534.2
189000                TO RE-MARK                                        NC2534.2
189100     MOVE    WRK-AN-00001  TO COMPUTED-X                          NC2534.2
189200     MOVE   "B"    TO CORRECT-X                                   NC2534.2
189300     PERFORM FAIL.                                                NC2534.2
189400 SUB-WRITE-F3-19-1.                                               NC2534.2
189500     PERFORM PRINT-DETAIL.                                        NC2534.2
189600*                                                                 NC2534.2
189700 SUB-INIT-F3-19-2.                                                NC2534.2
189800     MOVE  "SUB-TEST-F3-19-2" TO PAR-NAME.                        NC2534.2
189900     ADD    1 TO REC-CT.                                          NC2534.2
190000 SUB-TEST-F3-19-2.                                                NC2534.2
190100     IF     SUBTR-13 OF SUBTR-16 NOT EQUAL TO 1                   NC2534.2
190200            GO TO SUB-FAIL-F3-19-2.                               NC2534.2
190300     PERFORM PASS                                                 NC2534.2
190400     GO TO SUB-WRITE-F3-19-2.                                     NC2534.2
190500 SUB-DELETE-F3-19-2.                                              NC2534.2
190600     PERFORM DE-LETE.                                             NC2534.2
190700     GO TO SUB-WRITE-F3-19-2.                                     NC2534.2
190800 SUB-FAIL-F3-19-2.                                                NC2534.2
190900     MOVE    SUBTR-13 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
191000     MOVE   "+1" TO CORRECT-A                                     NC2534.2
191100     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
191200          TO RE-MARK                                              NC2534.2
191300     PERFORM FAIL.                                                NC2534.2
191400 SUB-WRITE-F3-19-2.                                               NC2534.2
191500     PERFORM PRINT-DETAIL.                                        NC2534.2
191600*                                                                 NC2534.2
191700 SUB-INIT-F3-19-3.                                                NC2534.2
191800     MOVE  "SUB-TEST-F3-19-3" TO PAR-NAME.                        NC2534.2
191900     ADD    1 TO REC-CT.                                          NC2534.2
192000 SUB-TEST-F3-19-3.                                                NC2534.2
192100     IF     SUBTR-14 OF SUBTR-16 IS NOT EQUAL TO 1.96             NC2534.2
192200            GO TO SUB-FAIL-F3-19-3.                               NC2534.2
192300     PERFORM PASS                                                 NC2534.2
192400     GO TO SUB-WRITE-F3-19-3.                                     NC2534.2
192500 SUB-DELETE-F3-19-3.                                              NC2534.2
192600     PERFORM DE-LETE.                                             NC2534.2
192700     GO TO SUB-WRITE-F3-19-3.                                     NC2534.2
192800 SUB-FAIL-F3-19-3.                                                NC2534.2
192900     MOVE   SUBTR-14 OF SUBTR-16 TO COMPUTED-N                    NC2534.2
193000     MOVE   "+1.96" TO CORRECT-A                                  NC2534.2
193100     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
193200          TO RE-MARK                                              NC2534.2
193300     PERFORM FAIL.                                                NC2534.2
193400 SUB-WRITE-F3-19-3.                                               NC2534.2
193500     PERFORM PRINT-DETAIL.                                        NC2534.2
193600*                                                                 NC2534.2
193700 SUB-INIT-F3-19-4.                                                NC2534.2
193800     MOVE  "SUB-TEST-F3-19-4" TO PAR-NAME.                        NC2534.2
193900     ADD     1 TO REC-CT.                                         NC2534.2
194000 SUB-TEST-F3-19-4.                                                NC2534.2
194100     IF     SUBTR-15 OF SUBTR-16 NOT EQUAL TO -5.76               NC2534.2
194200            GO TO SUB-FAIL-F3-19-4.                               NC2534.2
194300     PERFORM PASS                                                 NC2534.2
194400     GO TO SUB-WRITE-F3-19-4.                                     NC2534.2
194500 SUB-DELETE-F3-19-4.                                              NC2534.2
194600     PERFORM DE-LETE.                                             NC2534.2
194700     GO TO SUB-WRITE-F3-19-4.                                     NC2534.2
194800 SUB-FAIL-F3-19-4.                                                NC2534.2
194900     MOVE    SUBTR-15 OF SUBTR-16 TO COMPUTED-N                   NC2534.2
195000     MOVE   "-5.76" TO CORRECT-A                                  NC2534.2
195100     MOVE "WRONGLY AFFECTED BY SIZE ERROR ON OTHER OPERAND"       NC2534.2
195200          TO RE-MARK                                              NC2534.2
195300     PERFORM FAIL.                                                NC2534.2
195400 SUB-WRITE-F3-19-4.                                               NC2534.2
195500     PERFORM PRINT-DETAIL.                                        NC2534.2
195600*                                                                 NC2534.2
195700 SUB-INIT-F3-19-5.                                                NC2534.2
195800     MOVE   "SUB-TEST-F3-19-5" TO PAR-NAME.                       NC2534.2
195900     ADD     1 TO REC-CT.                                         NC2534.2
196000 SUB-TEST-F3-19-5.                                                NC2534.2
196100     IF      WRK-XN-00001 NOT = "Z"                               NC2534.2
196200             GO TO SUB-FAIL-F3-19-5.                              NC2534.2
196300     PERFORM PASS                                                 NC2534.2
196400     GO TO SUB-WRITE-F3-19-5.                                     NC2534.2
196500 SUB-DELETE-F3-19-5.                                              NC2534.2
196600     PERFORM DE-LETE.                                             NC2534.2
196700     GO TO SUB-WRITE-F3-19-5.                                     NC2534.2
196800 SUB-FAIL-F3-19-5.                                                NC2534.2
196900     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2534.2
197000     MOVE   "Z"   TO COMPUTED-X                                   NC2534.2
197100     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2534.2
197200     PERFORM FAIL.                                                NC2534.2
197300 SUB-WRITE-F3-19-5.                                               NC2534.2
197400     PERFORM PRINT-DETAIL.                                        NC2534.2
197500*                                                                 NC2534.2
197600 CCVS-EXIT SECTION.                                               NC2534.2
197700 CCVS-999999.                                                     NC2534.2
197800     GO TO CLOSE-FILES.                                           NC2534.2
      *END-OF,NC253A                                                                  
