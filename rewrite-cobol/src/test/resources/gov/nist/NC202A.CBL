      *HEADER,COBOL,NC202A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2024.2
000200 PROGRAM-ID.                                                      NC2024.2
000300     NC202A.                                                      NC2024.2
000400                                                                  NC2024.2
000500****************************************************************  NC2024.2
000600*                                                              *  NC2024.2
000700*    VALIDATION FOR:-                                          *  NC2024.2
000800*                                                              *  NC2024.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2024.2
001000*                                                              *  NC2024.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2024.2
001200*                                                              *  NC2024.2
001300****************************************************************  NC2024.2
001400*                                                              *  NC2024.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2024.2
001600*                                                              *  NC2024.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2024.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2024.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2024.2
002000*                                                              *  NC2024.2
002100****************************************************************  NC2024.2
002200*                                                                 NC2024.2
002300*    PROGRAM NC202A TESTS FORMAT3 OF THE ADD STATEMENT.           NC2024.2
002400*                                                                 NC2024.2
002500 ENVIRONMENT DIVISION.                                            NC2024.2
002600 CONFIGURATION SECTION.                                           NC2024.2
002700 SOURCE-COMPUTER.                                                 NC2024.2
002800     XXXXX082.                                                    NC2024.2
002900 OBJECT-COMPUTER.                                                 NC2024.2
003000     XXXXX083.                                                    NC2024.2
003100 INPUT-OUTPUT SECTION.                                            NC2024.2
003200 FILE-CONTROL.                                                    NC2024.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC2024.2
003400     XXXXX055.                                                    NC2024.2
003500 DATA DIVISION.                                                   NC2024.2
003600 FILE SECTION.                                                    NC2024.2
003700 FD  PRINT-FILE.                                                  NC2024.2
003800 01  PRINT-REC PICTURE X(120).                                    NC2024.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC2024.2
004000 WORKING-STORAGE SECTION.                                         NC2024.2
004100 01  TABLE1.                                                      NC2024.2
004200     02  RECORD1                 PICTURE 99.                      NC2024.2
004300     02  RECORD2                 PICTURE 99                       NC2024.2
004400                                 OCCURS 2 TIMES                   NC2024.2
004500                                 INDEXED BY INDEX1.               NC2024.2
004600     02  RECORD3                 PICTURE 99.                      NC2024.2
004700 01  TABLE2.                                                      NC2024.2
004800     02  RECORD1                 PICTURE 99.                      NC2024.2
004900     02  RECORD2                 PICTURE 99                       NC2024.2
005000                                 OCCURS 2 TIMES                   NC2024.2
005100                                 INDEXED BY INDEX2.               NC2024.2
005200     02  RECORD3                 PICTURE 99.                      NC2024.2
005300 77  WRK-AN-00001                PICTURE X.                       NC2024.2
005400 77  WRK-XN-00001                PICTURE X.                       NC2024.2
005500 77  WRK-DS-01V00                PICTURE S9.                      NC2024.2
005600 77  WRK-DS-02V00                PICTURE S99.                     NC2024.2
005700 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC2024.2
005800 77  WRK-DS-05V00                PICTURE S9(5).                   NC2024.2
005900 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC2024.2
006000 77  WRK-DS-09V09                PICTURE S9(9)V9(9).              NC2024.2
006100 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC2024.2
006200 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC2024.2
006300                                 VALUE 111111111.111111111.       NC2024.2
006400 77  WRK-DS-18V00       PICTURE S9(18) VALUE 111111111111111111.  NC2024.2
006500 77  A05ONES-DS-05V00            PICTURE S9(5) VALUE 11111.       NC2024.2
006600 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC2024.2
006700 77  WRK-DS-03V00                PICTURE S999.                    NC2024.2
006800 77  WRK-DS-06V00                PICTURE S9(6).                   NC2024.2
006900 77  WRK-DS-0201P                PICTURE S99P.                    NC2024.2
007000 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC2024.2
007100 77  ADD-1                   PICTURE S9(8)V99  VALUE 1.           NC2024.2
007200 77  ADD-2                   PICTURE S9(6)V9(4) VALUE 1.          NC2024.2
007300 77  ADD-3                   PICTURE S9(5)     VALUE -1.          NC2024.2
007400 77  ADD-4                   PICTURE 9         VALUE 9.           NC2024.2
007500 77  ADD-5                   PICTURE 9         VALUE 9.           NC2024.2
007600 77  ADD-6                   PICTURE 9(5)      VALUE 99999.       NC2024.2
007700 77  ADD-7                   PICTURE 9         VALUE 1.           NC2024.2
007800 77  ADD-8                   PICTURE 9.                           NC2024.2
007900 77  ADD-9                   PICTURE S9(8)V99  VALUE 5.9.         NC2024.2
008000 77  ADD-10                  PICTURE 9(5)      VALUE 52800.       NC2024.2
008100 77  ADD-11                  PICTURE 99999.                       NC2024.2
008200 77  ADD-12                  PICTURE PP9       VALUE .001.        NC2024.2
008300 77  ADD-13                  PICTURE 9PP       VALUE 100.         NC2024.2
008400 77  ADD-14                  PICTURE 999V999.                     NC2024.2
008500 77  W-1                     PICTURE IS 9.                        NC2024.2
008600 77  W-2                     PICTURE IS 99.                       NC2024.2
008700 77  W-3                     PICTURE IS 999.                      NC2024.2
008800 77  W-4                PICTURE 9  VALUE 0.                       NC2024.2
008900 77  W-6                     PICTURE IS 999    VALUE IS ZERO.     NC2024.2
009000 77  W-9                     PICTURE 999.                         NC2024.2
009100 77  D-5                PICTURE S999  VALUE -1.                   NC2024.2
009200 77  D-9                PICTURE 9(4)V9(4)  VALUE 111.1189.        NC2024.2
009300 77  ONE                PICTURE 9  VALUE 1.                       NC2024.2
009400 77  TWO                PICTURE S9  VALUE 2.                      NC2024.2
009500 77  THREE              PICTURE S9  VALUE 3.                      NC2024.2
009600 77  FOUR               PICTURE S9  VALUE 4.                      NC2024.2
009700 77  FIVE               PICTURE S9  VALUE 5.                      NC2024.2
009800 77  SIX                PICTURE S9  VALUE 6.                      NC2024.2
009900 77  SEVEN              PICTURE S9  VALUE 7.                      NC2024.2
010000 77  EIGHT              PICTURE 9  VALUE 8.                       NC2024.2
010100 77  NINE               PICTURE S9  VALUE 9.                      NC2024.2
010200 77  TEN                PICTURE S99  VALUE 10.                    NC2024.2
010300 77  FIFTEEN            PICTURE S99  VALUE 15.                    NC2024.2
010400 77  TWENTY             PICTURE S99  VALUE 20.                    NC2024.2
010500 77  TWENTY-5           PICTURE S99  VALUE 25.                    NC2024.2
010600 01  WRK-DS-09V00                 PICTURE S9(9)  VALUE ZERO.      NC2024.2
010700 01  GRP-FOR-ADD-CORR-1.                                          NC2024.2
010800     02  GRP-SUBTRACT-CORR-1.                                     NC2024.2
010900     03  FILLER                  PICTURE S99  VALUE 91.           NC2024.2
011000     03  ADD-CORR-2              PICTURE S99  VALUE 22.           NC2024.2
011100     03  ADD-CORR-1              PICTURE S99 VALUE 11.            NC2024.2
011200     03  ADD-CORR-A              PICTURE S99 VALUE 93.            NC2024.2
011300     03  ADD-CORR-4              PICTURE S99 VALUE 44.            NC2024.2
011400     03  ADD-CORR-3              PICTURE S99 VALUE 33.            NC2024.2
011500     03  ADD-CORR-6              PICTURE S99 VALUE 66.            NC2024.2
011600     03  ADD-CORR-5              PICTURE S99 VALUE 55.            NC2024.2
011700     03  ADD-CORR-8              PICTURE S99 VALUE 88.            NC2024.2
011800     03  ADD-CORR-7              PICTURE S99 VALUE 77.            NC2024.2
011900     03  ADD-CORR-9              PICTURE S99 VALUE 99.            NC2024.2
012000 01  GRP-FOR-ADD-CORR-R.                                          NC2024.2
012100     02  GRP-SUBTRACT-CORR-1.                                     NC2024.2
012200     05  ADD-CORR-1              PICTURE 99.                      NC2024.2
012300     05  ADD-CORR-2              PICTURE 99.                      NC2024.2
012400     05  ADD-CORR-3              PICTURE 99.                      NC2024.2
012500     05  ADD-CORR-4              PICTURE 99.                      NC2024.2
012600     05  ADD-CORR-5              PICTURE 9P.                      NC2024.2
012700     05  ADD-CORR-6              PICTURE 999.                     NC2024.2
012800     05  ADD-CORR-7              PICTURE 99.                      NC2024.2
012900     05  ADD-CORR-8              PICTURE 99.                      NC2024.2
013000     05  ADD-CORR-9              PICTURE 99.                      NC2024.2
013100     05  FILLER                  PICTURE 99.                      NC2024.2
013200 01  GRP-FOR-ADD-CORR-2.                                          NC2024.2
013300     02  GRP-ADD-SUB-CORR.                                        NC2024.2
013400     03  GRP-SUBTRACT-CORR-1.                                     NC2024.2
013500     04  ADD-CORR-1              PICTURE S99  VALUE 11.           NC2024.2
013600     04  ADD-CORR-2              PICTURE S99  VALUE 22.           NC2024.2
013700     04  ADD-CORR-5              PICTURE S99  VALUE 55.           NC2024.2
013800     04  ADD-CORR-4              PICTURE S99  VALUE 44.           NC2024.2
013900     04  ADD-CORR-3              PICTURE S99  VALUE 33.           NC2024.2
014000     04  ADD-CORR-6              PICTURE S99  VALUE 66.           NC2024.2
014100     04  ADD-CORR-7              PICTURE S99  VALUE 77.           NC2024.2
014200     04  ADD-CORR-8              PICTURE S99  VALUE 88.           NC2024.2
014300     04  ADD-CORR-9              PICTURE S99  VALUE 99.           NC2024.2
014400     04  ADD-CORR-B              PICTURE S99  VALUE 92.           NC2024.2
014500     04  ADD-CORR-0              PICTURE S99  VALUE 00.           NC2024.2
014600 01  GRP-FOR-ADD-CORR-A.                                          NC2024.2
014700     02  GRP-SUBTRACT-CORR-3.                                     NC2024.2
014800         03  GRP-SUBTRACT-CORR-1.                                 NC2024.2
014900             05  ADD-CORR-4      PICTURE S999   VALUE 044.        NC2024.2
015000             05  ADD-CORR-3      PICTURE S999   VALUE 033.        NC2024.2
015100             05  ADD-CORR-2      PICTURE S999   VALUE 022.        NC2024.2
015200             05  ADD-CORR-1      PICTURE S999   VALUE 111.        NC2024.2
015300 01  ADD-15.                                                      NC2024.2
015400     02 FIELD1               PICTURE 99999     VALUE 1.           NC2024.2
015500     02 FIELD2               PICTURE 999V99    VALUE 32.1.        NC2024.2
015600     02 FIELD3               PICTURE 999V9     VALUE 123.4.       NC2024.2
015700 01  ADD-16.                                                      NC2024.2
015800     02 FIELD1               PICTURE 99999     VALUE 99999.       NC2024.2
015900     02 FIELD2               PICTURE 999V99    VALUE 745.67.      NC2024.2
016000     02 FIELD3               PICTURE 999V9     VALUE 432.1.       NC2024.2
016100 01  SUBTRACT-DATA.                                               NC2024.2
016200     02 SUBTR-1              PICTURE 9         VALUE 1.           NC2024.2
016300     02 SUBTR-2              PICTURE S99       VALUE 99.          NC2024.2
016400     02 SUBTR-3              PICTURE S9V99     VALUE -1.          NC2024.2
016500     02 SUBTR-4              PICTURE SPP9      VALUE .001.        NC2024.2
016600     02 SUBTR-5              PICTURE S9PP      VALUE 100.         NC2024.2
016700     02  SUBTR-6                  PICTURE 9 VALUE 1.              NC2024.2
016800     02  SUBTR-7                  PICTURE S99  VALUE 99.          NC2024.2
016900     02  SUBTR-8                  PICTURE S9V99  VALUE -9.99.     NC2024.2
017000     02 SUBTR-9              PICTURE SV999.                       NC2024.2
017100     02  SUBTR-10                 PICTURE S999  VALUE 100.        NC2024.2
017200     02 SUBTR-11             PICTURE S999V999.                    NC2024.2
017300     02 SUBTR-12.                                                 NC2024.2
017400     03 SUBTR-13             PICTURE 9         VALUE 1.           NC2024.2
017500     03 SUBTR-14             PICTURE S9V999    VALUE -1.725.      NC2024.2
017600     03 SUBTR-15             PICTURE S99V99    VALUE 76.76.       NC2024.2
017700     02 SUBTR-16.                                                 NC2024.2
017800     03 SUBTR-13             PICTURE 9         VALUE 2.           NC2024.2
017900     03 SUBTR-14             PICTURE S9V99     VALUE .23.         NC2024.2
018000     03 SUBTR-15             PICTURE S9V99     VALUE 1.           NC2024.2
018100 01  CORR-DATA-1.                                                 NC2024.2
018200     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
018300     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
018400     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
018500     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
018600     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
018700     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
018800 01  CORR-DATA-2.                                                 NC2024.2
018900     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019000     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019100     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019200     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019300     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019400     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019500 01  CORR-DATA-3.                                                 NC2024.2
019600     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019700     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019800     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
019900     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
020000     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
020100     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2024.2
020200 01  CORR-DATA-4.                                                 NC2024.2
020300     03 XYZ-11               PICTURE IS 99.                       NC2024.2
020400     03 XYZ-12               PICTURE IS 99.                       NC2024.2
020500     03 XYZ-13               PICTURE IS 99.                       NC2024.2
020600     03 XYZ-14               PICTURE IS 99.                       NC2024.2
020700     03 XYZ-15               PICTURE IS 99.                       NC2024.2
020800     03 XYZ-16               PICTURE IS 99.                       NC2024.2
020900 01  CORR-DATA-5.                                                 NC2024.2
021000     03 XYZ-1                PICTURE 99.                          NC2024.2
021100     03 XYZ-2                PICTURE 99.                          NC2024.2
021200     03 XYZ-13               PICTURE IS 99.                       NC2024.2
021300     03 XYZ-14               PICTURE IS 99.                       NC2024.2
021400     03 FILLER               PICTURE IS 99.                       NC2024.2
021500     03 XYZ-11               PICTURE IS 99.                       NC2024.2
021600     03 XYZ-12               PICTURE IS 99.                       NC2024.2
021700 01  CORR-DATA-6.                                                 NC2024.2
021800     03 XYZ-11               PICTURE IS 99.                       NC2024.2
021900     03 XYZ-12               PICTURE IS 99.                       NC2024.2
022000     03 FILLER               PICTURE IS 99.                       NC2024.2
022100     03 XYZ-1                PICTURE IS 99.                       NC2024.2
022200     03 XYZ-2                PICTURE IS 9(2).                     NC2024.2
022300     03 FILLER               PICTURE IS 99.                       NC2024.2
022400 01  CORR-DATA-7.                                                 NC2024.2
022500     02 XYZ-1                PICTURE 99V99     VALUE 10.45.       NC2024.2
022600     02 XYZ-6                PICTURE 999V9     VALUE 100.5.       NC2024.2
022700     02 XYZ-11               PICTURE 99V9      VALUE ZERO.        NC2024.2
022800     02 XYZ-2                PICTURE 99V9      VALUE 0.9.         NC2024.2
022900 01  42-DATANAMES.                                                NC2024.2
023000     02  DNAME1   PICTURE 9      VALUE 1        COMPUTATIONAL.    NC2024.2
023100     02  DNAME2   PICTURE 99      VALUE 1  COMPUTATIONAL.         NC2024.2
023200     02  DNAME3   PICTURE 999     VALUE 1  COMPUTATIONAL.         NC2024.2
023300     02  DNAME4   PICTURE 9(4)    VALUE 1  COMPUTATIONAL.         NC2024.2
023400     02  DNAME5   PICTURE 9(5)    VALUE 1  COMPUTATIONAL.         NC2024.2
023500     02  DNAME6   PICTURE 9(6)    VALUE 1  COMPUTATIONAL.         NC2024.2
023600     02  DNAME7   PICTURE 9(7)    VALUE 1  COMPUTATIONAL.         NC2024.2
023700     02  DNAME8   PICTURE 9(8)    VALUE 1  COMPUTATIONAL.         NC2024.2
023800     02  DNAME9   PICTURE 9(9)    VALUE 1  COMPUTATIONAL.         NC2024.2
023900     02  DNAME10  PICTURE 9(10)   VALUE 1.                        NC2024.2
024000     02  DNAME11  PICTURE 9(11)   VALUE 1.                        NC2024.2
024100     02  DNAME12  PICTURE 9(12)   VALUE 1.                        NC2024.2
024200     02  DNAME13  PICTURE 9(13)   VALUE 1.                        NC2024.2
024300     02  DNAME14  PICTURE 9(14)   VALUE 1.                        NC2024.2
024400     02  DNAME15  PICTURE 9(15)   VALUE 1.                        NC2024.2
024500     02  DNAME16  PICTURE 9(16)   VALUE 1.                        NC2024.2
024600     02  DNAME17  PICTURE 9(17)   VALUE 1.                        NC2024.2
024700     02  DNAME18  PICTURE 9(18)   VALUE 1.                        NC2024.2
024800     02  DNAME19  PICTURE 9       VALUE 1.                        NC2024.2
024900     02  DNAME20  PICTURE 99      VALUE 1.                        NC2024.2
025000     02  DNAME21  PICTURE 999     VALUE 1.                        NC2024.2
025100     02  DNAME22  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025200     02  DNAME23  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025300     02  DNAME24  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025400     02  DNAME25  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025500     02  DNAME26  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025600     02  DNAME27  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025700     02  DNAME28  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025800     02  DNAME29  PICTURE 9(18)  VALUE ZERO.                      NC2024.2
025900     02  DNAME30   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026000     02  DNAME31   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026100     02  DNAME32   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026200     02  DNAME33   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026300     02  DNAME34   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026400     02  DNAME35   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026500     02  DNAME36   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026600     02  DNAME37   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026700     02  DNAME38   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026800     02  DNAME39   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
026900     02  DNAME40   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
027000     02  DNAME41   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
027100     02  DNAME42   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2024.2
027200 01  TEST-RESULTS.                                                NC2024.2
027300     02 FILLER                   PIC X      VALUE SPACE.          NC2024.2
027400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2024.2
027500     02 FILLER                   PIC X      VALUE SPACE.          NC2024.2
027600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2024.2
027700     02 FILLER                   PIC X      VALUE SPACE.          NC2024.2
027800     02  PAR-NAME.                                                NC2024.2
027900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2024.2
028000       03  PARDOT-X              PIC X      VALUE SPACE.          NC2024.2
028100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2024.2
028200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2024.2
028300     02 RE-MARK                  PIC X(61).                       NC2024.2
028400 01  TEST-COMPUTED.                                               NC2024.2
028500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2024.2
028600     02 FILLER                   PIC X(17)  VALUE                 NC2024.2
028700            "       COMPUTED=".                                   NC2024.2
028800     02 COMPUTED-X.                                               NC2024.2
028900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2024.2
029000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2024.2
029100                                 PIC -9(9).9(9).                  NC2024.2
029200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2024.2
029300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2024.2
029400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2024.2
029500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2024.2
029600         04 COMPUTED-18V0                    PIC -9(18).          NC2024.2
029700         04 FILLER                           PIC X.               NC2024.2
029800     03 FILLER PIC X(50) VALUE SPACE.                             NC2024.2
029900 01  TEST-CORRECT.                                                NC2024.2
030000     02 FILLER PIC X(30) VALUE SPACE.                             NC2024.2
030100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2024.2
030200     02 CORRECT-X.                                                NC2024.2
030300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2024.2
030400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2024.2
030500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2024.2
030600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2024.2
030700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2024.2
030800     03      CR-18V0 REDEFINES CORRECT-A.                         NC2024.2
030900         04 CORRECT-18V0                     PIC -9(18).          NC2024.2
031000         04 FILLER                           PIC X.               NC2024.2
031100     03 FILLER PIC X(2) VALUE SPACE.                              NC2024.2
031200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2024.2
031300 01  CCVS-C-1.                                                    NC2024.2
031400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2024.2
031500-    "SS  PARAGRAPH-NAME                                          NC2024.2
031600-    "       REMARKS".                                            NC2024.2
031700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2024.2
031800 01  CCVS-C-2.                                                    NC2024.2
031900     02 FILLER                     PIC X        VALUE SPACE.      NC2024.2
032000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2024.2
032100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2024.2
032200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2024.2
032300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2024.2
032400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2024.2
032500 01  REC-CT                        PIC 99       VALUE ZERO.       NC2024.2
032600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2024.2
032700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2024.2
032800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2024.2
032900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2024.2
033000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2024.2
033100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2024.2
033200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2024.2
033300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2024.2
033400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2024.2
033500 01  CCVS-H-1.                                                    NC2024.2
033600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2024.2
033700     02  FILLER                    PIC X(42)    VALUE             NC2024.2
033800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2024.2
033900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2024.2
034000 01  CCVS-H-2A.                                                   NC2024.2
034100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2024.2
034200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2024.2
034300   02  FILLER                        PIC XXXX   VALUE             NC2024.2
034400     "4.2 ".                                                      NC2024.2
034500   02  FILLER                        PIC X(28)  VALUE             NC2024.2
034600            " COPY - NOT FOR DISTRIBUTION".                       NC2024.2
034700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2024.2
034800                                                                  NC2024.2
034900 01  CCVS-H-2B.                                                   NC2024.2
035000   02  FILLER                        PIC X(15)  VALUE             NC2024.2
035100            "TEST RESULT OF ".                                    NC2024.2
035200   02  TEST-ID                       PIC X(9).                    NC2024.2
035300   02  FILLER                        PIC X(4)   VALUE             NC2024.2
035400            " IN ".                                               NC2024.2
035500   02  FILLER                        PIC X(12)  VALUE             NC2024.2
035600     " HIGH       ".                                              NC2024.2
035700   02  FILLER                        PIC X(22)  VALUE             NC2024.2
035800            " LEVEL VALIDATION FOR ".                             NC2024.2
035900   02  FILLER                        PIC X(58)  VALUE             NC2024.2
036000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2024.2
036100 01  CCVS-H-3.                                                    NC2024.2
036200     02  FILLER                      PIC X(34)  VALUE             NC2024.2
036300            " FOR OFFICIAL USE ONLY    ".                         NC2024.2
036400     02  FILLER                      PIC X(58)  VALUE             NC2024.2
036500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2024.2
036600     02  FILLER                      PIC X(28)  VALUE             NC2024.2
036700            "  COPYRIGHT   1985 ".                                NC2024.2
036800 01  CCVS-E-1.                                                    NC2024.2
036900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2024.2
037000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2024.2
037100     02 ID-AGAIN                     PIC X(9).                    NC2024.2
037200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2024.2
037300 01  CCVS-E-2.                                                    NC2024.2
037400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2024.2
037500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2024.2
037600     02 CCVS-E-2-2.                                               NC2024.2
037700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2024.2
037800         03 FILLER                   PIC X      VALUE SPACE.      NC2024.2
037900         03 ENDER-DESC               PIC X(44)  VALUE             NC2024.2
038000            "ERRORS ENCOUNTERED".                                 NC2024.2
038100 01  CCVS-E-3.                                                    NC2024.2
038200     02  FILLER                      PIC X(22)  VALUE             NC2024.2
038300            " FOR OFFICIAL USE ONLY".                             NC2024.2
038400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2024.2
038500     02  FILLER                      PIC X(58)  VALUE             NC2024.2
038600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2024.2
038700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2024.2
038800     02 FILLER                       PIC X(15)  VALUE             NC2024.2
038900             " COPYRIGHT 1985".                                   NC2024.2
039000 01  CCVS-E-4.                                                    NC2024.2
039100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2024.2
039200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2024.2
039300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2024.2
039400     02 FILLER                       PIC X(40)  VALUE             NC2024.2
039500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2024.2
039600 01  XXINFO.                                                      NC2024.2
039700     02 FILLER                       PIC X(19)  VALUE             NC2024.2
039800            "*** INFORMATION ***".                                NC2024.2
039900     02 INFO-TEXT.                                                NC2024.2
040000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2024.2
040100       04 XXCOMPUTED                 PIC X(20).                   NC2024.2
040200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2024.2
040300       04 XXCORRECT                  PIC X(20).                   NC2024.2
040400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2024.2
040500 01  HYPHEN-LINE.                                                 NC2024.2
040600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2024.2
040700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2024.2
040800-    "*****************************************".                 NC2024.2
040900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2024.2
041000-    "******************************".                            NC2024.2
041100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2024.2
041200     "NC202A".                                                    NC2024.2
041300 PROCEDURE DIVISION.                                              NC2024.2
041400 CCVS1 SECTION.                                                   NC2024.2
041500 OPEN-FILES.                                                      NC2024.2
041600     OPEN     OUTPUT PRINT-FILE.                                  NC2024.2
041700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2024.2
041800     MOVE    SPACE TO TEST-RESULTS.                               NC2024.2
041900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2024.2
042000     GO TO CCVS1-EXIT.                                            NC2024.2
042100 CLOSE-FILES.                                                     NC2024.2
042200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2024.2
042300 TERMINATE-CCVS.                                                  NC2024.2
042400*    EXIT PROGRAM.                                                NC2024.2
042500*TERMINATE-CALL.                                                  NC2024.2
042600     STOP     RUN.                                                NC2024.2
042700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2024.2
042800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2024.2
042900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2024.2
043000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2024.2
043100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2024.2
043200 PRINT-DETAIL.                                                    NC2024.2
043300     IF REC-CT NOT EQUAL TO ZERO                                  NC2024.2
043400             MOVE "." TO PARDOT-X                                 NC2024.2
043500             MOVE REC-CT TO DOTVALUE.                             NC2024.2
043600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2024.2
043700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2024.2
043800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2024.2
043900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2024.2
044000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2024.2
044100     MOVE SPACE TO CORRECT-X.                                     NC2024.2
044200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2024.2
044300     MOVE     SPACE TO RE-MARK.                                   NC2024.2
044400 HEAD-ROUTINE.                                                    NC2024.2
044500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2024.2
044600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2024.2
044700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2024.2
044800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2024.2
044900 COLUMN-NAMES-ROUTINE.                                            NC2024.2
045000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2024.2
045100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2024.2
045200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2024.2
045300 END-ROUTINE.                                                     NC2024.2
045400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2024.2
045500 END-RTN-EXIT.                                                    NC2024.2
045600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2024.2
045700 END-ROUTINE-1.                                                   NC2024.2
045800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2024.2
045900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2024.2
046000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2024.2
046100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2024.2
046200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2024.2
046300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2024.2
046400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2024.2
046500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2024.2
046600  END-ROUTINE-12.                                                 NC2024.2
046700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2024.2
046800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2024.2
046900         MOVE "NO " TO ERROR-TOTAL                                NC2024.2
047000         ELSE                                                     NC2024.2
047100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2024.2
047200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2024.2
047300     PERFORM WRITE-LINE.                                          NC2024.2
047400 END-ROUTINE-13.                                                  NC2024.2
047500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2024.2
047600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2024.2
047700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2024.2
047800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2024.2
047900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2024.2
048000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2024.2
048100          MOVE "NO " TO ERROR-TOTAL                               NC2024.2
048200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2024.2
048300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2024.2
048400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2024.2
048500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2024.2
048600 WRITE-LINE.                                                      NC2024.2
048700     ADD 1 TO RECORD-COUNT.                                       NC2024.2
048800*    IF RECORD-COUNT GREATER 50                                   NC2024.2
048900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2024.2
049000*        MOVE SPACE TO DUMMY-RECORD                               NC2024.2
049100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2024.2
049200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2024.2
049300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2024.2
049400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2024.2
049500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2024.2
049600*        MOVE ZERO TO RECORD-COUNT.                               NC2024.2
049700     PERFORM WRT-LN.                                              NC2024.2
049800 WRT-LN.                                                          NC2024.2
049900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2024.2
050000     MOVE SPACE TO DUMMY-RECORD.                                  NC2024.2
050100 BLANK-LINE-PRINT.                                                NC2024.2
050200     PERFORM WRT-LN.                                              NC2024.2
050300 FAIL-ROUTINE.                                                    NC2024.2
050400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2024.2
050500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2024.2
050600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2024.2
050700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2024.2
050800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2024.2
050900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2024.2
051000     GO TO  FAIL-ROUTINE-EX.                                      NC2024.2
051100 FAIL-ROUTINE-WRITE.                                              NC2024.2
051200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2024.2
051300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2024.2
051400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2024.2
051500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2024.2
051600 FAIL-ROUTINE-EX. EXIT.                                           NC2024.2
051700 BAIL-OUT.                                                        NC2024.2
051800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2024.2
051900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2024.2
052000 BAIL-OUT-WRITE.                                                  NC2024.2
052100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2024.2
052200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2024.2
052300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2024.2
052400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2024.2
052500 BAIL-OUT-EX. EXIT.                                               NC2024.2
052600 CCVS1-EXIT.                                                      NC2024.2
052700     EXIT.                                                        NC2024.2
052800 SECT-NC202A-001 SECTION.                                         NC2024.2
052900 ADD-INIT-F3-1.                                                   NC2024.2
053000     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
053100     MOVE "ADD CORRESPONDING " TO FEATURE.                        NC2024.2
053200     MOVE "ADD-TEST-F3-1" TO PAR-NAME.                            NC2024.2
053300     MOVE ZERO TO GRP-FOR-ADD-CORR-R.                             NC2024.2
053400     MOVE   11  TO  ADD-CORR-1 OF GRP-FOR-ADD-CORR-1.             NC2024.2
053500     MOVE   22  TO  ADD-CORR-2 OF GRP-FOR-ADD-CORR-1.             NC2024.2
053600     MOVE   33  TO  ADD-CORR-3 OF GRP-FOR-ADD-CORR-1.             NC2024.2
053700     MOVE   44  TO  ADD-CORR-4 OF GRP-FOR-ADD-CORR-1.             NC2024.2
053800     MOVE   55  TO  ADD-CORR-5 OF GRP-FOR-ADD-CORR-1.             NC2024.2
053900     MOVE   66  TO  ADD-CORR-6 OF GRP-FOR-ADD-CORR-1.             NC2024.2
054000     MOVE   77  TO  ADD-CORR-7 OF GRP-FOR-ADD-CORR-1.             NC2024.2
054100     MOVE   88  TO  ADD-CORR-8 OF GRP-FOR-ADD-CORR-1.             NC2024.2
054200     MOVE   99  TO  ADD-CORR-9 OF GRP-FOR-ADD-CORR-1.             NC2024.2
054300 ADD-TEST-F3-1.                                                   NC2024.2
054400     ADD CORRESPONDING GRP-FOR-ADD-CORR-1 TO GRP-FOR-ADD-CORR-R.  NC2024.2
054500     IF GRP-FOR-ADD-CORR-R EQUAL TO "11223344506677889900"        NC2024.2
054600         PERFORM PASS                                             NC2024.2
054700         GO TO ADD-WRITE-F3-1.                                    NC2024.2
054800     GO TO ADD-FAIL-F3-1.                                         NC2024.2
054900 ADD-DELETE-F3-1.                                                 NC2024.2
055000     PERFORM DE-LETE.                                             NC2024.2
055100     GO TO ADD-WRITE-F3-1.                                        NC2024.2
055200 ADD-FAIL-F3-1.                                                   NC2024.2
055300     MOVE GRP-FOR-ADD-CORR-R TO COMPUTED-A.                       NC2024.2
055400     MOVE "11223344506677889900" TO CORRECT-A.                    NC2024.2
055500     PERFORM FAIL.                                                NC2024.2
055600 ADD-WRITE-F3-1.                                                  NC2024.2
055700     PERFORM PRINT-DETAIL.                                        NC2024.2
055800*                                                                 NC2024.2
055900 ADD-INIT-F3-2.                                                   NC2024.2
056000     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
056100     MOVE "ADD-TEST-F3-2" TO PAR-NAME.                            NC2024.2
056200     MOVE ZERO TO GRP-FOR-ADD-CORR-R.                             NC2024.2
056300     MOVE   11  TO  ADD-CORR-1 OF GRP-ADD-SUB-CORR.               NC2024.2
056400     MOVE   22  TO  ADD-CORR-2 OF GRP-ADD-SUB-CORR.               NC2024.2
056500     MOVE   33  TO  ADD-CORR-3 OF GRP-ADD-SUB-CORR.               NC2024.2
056600     MOVE   44  TO  ADD-CORR-4 OF GRP-ADD-SUB-CORR.               NC2024.2
056700     MOVE   55  TO  ADD-CORR-5 OF GRP-ADD-SUB-CORR.               NC2024.2
056800     MOVE   66  TO  ADD-CORR-6 OF GRP-ADD-SUB-CORR.               NC2024.2
056900     MOVE   77  TO  ADD-CORR-7 OF GRP-ADD-SUB-CORR.               NC2024.2
057000     MOVE   88  TO  ADD-CORR-8 OF GRP-ADD-SUB-CORR.               NC2024.2
057100     MOVE   99  TO  ADD-CORR-9 OF GRP-ADD-SUB-CORR.               NC2024.2
057200 ADD-TEST-F3-2.                                                   NC2024.2
057300     ADD CORRESPONDING GRP-ADD-SUB-CORR TO                        NC2024.2
057400         GRP-FOR-ADD-CORR-R ROUNDED.                              NC2024.2
057500     IF GRP-FOR-ADD-CORR-R EQUAL TO "11223344606677889900"        NC2024.2
057600               PERFORM PASS                                       NC2024.2
057700               GO TO ADD-WRITE-F3-2.                              NC2024.2
057800     GO TO ADD-FAIL-F3-2.                                         NC2024.2
057900 ADD-DELETE-F3-2.                                                 NC2024.2
058000     PERFORM DE-LETE.                                             NC2024.2
058100     GO TO ADD-WRITE-F3-2.                                        NC2024.2
058200 ADD-FAIL-F3-2.                                                   NC2024.2
058300     MOVE GRP-FOR-ADD-CORR-R TO COMPUTED-A.                       NC2024.2
058400     MOVE "11223344606677889900" TO CORRECT-A.                    NC2024.2
058500     PERFORM FAIL.                                                NC2024.2
058600 ADD-WRITE-F3-2.                                                  NC2024.2
058700     PERFORM PRINT-DETAIL.                                        NC2024.2
058800*                                                                 NC2024.2
058900 ADD-INIT-F3-3.                                                   NC2024.2
059000     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
059100     MOVE ZERO TO GRP-FOR-ADD-CORR-R,  WRK-XN-00001.              NC2024.2
059200     MOVE 111    TO  ADD-CORR-1 OF GRP-SUBTRACT-CORR-3.           NC2024.2
059300     MOVE  22    TO  ADD-CORR-2 OF GRP-SUBTRACT-CORR-3.           NC2024.2
059400     MOVE  33    TO  ADD-CORR-3 OF GRP-SUBTRACT-CORR-3.           NC2024.2
059500     MOVE  44    TO  ADD-CORR-4 OF GRP-SUBTRACT-CORR-3.           NC2024.2
059600 ADD-INIT-F3-3-1.                                                 NC2024.2
059700     MOVE "ADD-TEST-F3-3-1" TO PAR-NAME.                          NC2024.2
059800 ADD-TEST-F3-3-1.                                                 NC2024.2
059900     ADD CORRESPONDING GRP-SUBTRACT-CORR-3 TO GRP-FOR-ADD-CORR-R  NC2024.2
060000         ON SIZE ERROR MOVE "1" TO WRK-XN-00001.                  NC2024.2
060100     IF GRP-FOR-ADD-CORR-R EQUAL TO "00223344000000000000"        NC2024.2
060200                   PERFORM PASS                                   NC2024.2
060300                   GO TO ADD-WRITE-F3-3-1.                        NC2024.2
060400     GO TO ADD-FAIL-F3-3-1.                                       NC2024.2
060500 ADD-DELETE-F3-3-1.                                               NC2024.2
060600     PERFORM DE-LETE.                                             NC2024.2
060700     GO TO ADD-WRITE-F3-3-1.                                      NC2024.2
060800 ADD-FAIL-F3-3-1.                                                 NC2024.2
060900     MOVE "00223344000000000000" TO CORRECT-A.                    NC2024.2
061000     MOVE GRP-FOR-ADD-CORR-R TO COMPUTED-A.                       NC2024.2
061100     PERFORM FAIL.                                                NC2024.2
061200 ADD-WRITE-F3-3-1.                                                NC2024.2
061300     PERFORM PRINT-DETAIL.                                        NC2024.2
061400*                                                                 NC2024.2
061500 ADD-INIT-F3-3-2.                                                 NC2024.2
061600     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
061700     MOVE "ADD-TEST-F3-3-2" TO PAR-NAME.                          NC2024.2
061800 ADD-TEST-F3-3-2.                                                 NC2024.2
061900     IF       WRK-XN-00001 EQUAL TO "1"                           NC2024.2
062000               PERFORM PASS                                       NC2024.2
062100               GO TO ADD-WRITE-F3-3-2.                            NC2024.2
062200     GO TO ADD-FAIL-F3-3-2.                                       NC2024.2
062300 ADD-DELETE-F3-3-2.                                               NC2024.2
062400     PERFORM DE-LETE.                                             NC2024.2
062500     GO TO ADD-WRITE-F3-3-2.                                      NC2024.2
062600 ADD-FAIL-F3-3-2.                                                 NC2024.2
062700     MOVE 1 TO CORRECT-A.                                         NC2024.2
062800     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC2024.2
062900     PERFORM FAIL.                                                NC2024.2
063000 ADD-WRITE-F3-3-2.                                                NC2024.2
063100     PERFORM PRINT-DETAIL.                                        NC2024.2
063200*                                                                 NC2024.2
063300 ADD-INIT-F3-4.                                                   NC2024.2
063400     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
063500     MOVE "ADD-TEST-F3-4" TO PAR-NAME.                            NC2024.2
063600     MOVE ZERO TO GRP-FOR-ADD-CORR-R.                             NC2024.2
063700     MOVE ZERO TO ADD-CORR-1 OF GRP-FOR-ADD-CORR-A.               NC2024.2
063800 ADD-TEST-F3-4.                                                   NC2024.2
063900     ADD CORRESPONDING GRP-SUBTRACT-CORR-1 OF GRP-SUBTRACT-CORR-3 NC2024.2
064000             TO GRP-SUBTRACT-CORR-1 OF GRP-FOR-ADD-CORR-R.        NC2024.2
064100     IF GRP-FOR-ADD-CORR-R EQUAL TO "00223344000000000000"        NC2024.2
064200                  PERFORM PASS                                    NC2024.2
064300                  GO TO ADD-WRITE-F3-4.                           NC2024.2
064400     GO TO ADD-FAIL-F3-4.                                         NC2024.2
064500 ADD-DELETE-F3-4.                                                 NC2024.2
064600     PERFORM DE-LETE.                                             NC2024.2
064700     GO TO ADD-WRITE-F3-4.                                        NC2024.2
064800 ADD-FAIL-F3-4.                                                   NC2024.2
064900     MOVE "00223344000000000000" TO CORRECT-A.                    NC2024.2
065000     MOVE GRP-FOR-ADD-CORR-R TO COMPUTED-A.                       NC2024.2
065100     PERFORM FAIL.                                                NC2024.2
065200 ADD-WRITE-F3-4.                                                  NC2024.2
065300     PERFORM PRINT-DETAIL.                                        NC2024.2
065400*                                                                 NC2024.2
065500 ADD-INIT-F3-5-1.                                                 NC2024.2
065600     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
065700     MOVE "ADD-TEST-F3-5-1" TO PAR-NAME.                          NC2024.2
065800     MOVE     1    TO FIELD1 OF ADD-15.                           NC2024.2
065900     MOVE    32.1  TO FIELD2 OF ADD-15.                           NC2024.2
066000     MOVE   123.4  TO FIELD3 OF ADD-15.                           NC2024.2
066100     MOVE 99999    TO FIELD1 OF ADD-16.                           NC2024.2
066200     MOVE   745.67 TO FIELD2 OF ADD-16.                           NC2024.2
066300     MOVE   432.1  TO FIELD3 OF ADD-16.                           NC2024.2
066400 ADD-TEST-F3-5-1.                                                 NC2024.2
066500     ADD CORRESPONDING ADD-15 TO ADD-16 ON SIZE ERROR             NC2024.2
066600               PERFORM PASS                                       NC2024.2
066700               GO TO ADD-WRITE-F3-5-1.                            NC2024.2
066800     GO TO ADD-FAIL-F3-5-1.                                       NC2024.2
066900 ADD-DELETE-F3-5-1.                                               NC2024.2
067000     PERFORM DE-LETE.                                             NC2024.2
067100     GO TO ADD-WRITE-F3-5-1.                                      NC2024.2
067200 ADD-FAIL-F3-5-1.                                                 NC2024.2
067300     PERFORM FAIL.                                                NC2024.2
067400     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC2024.2
067500 ADD-WRITE-F3-5-1.                                                NC2024.2
067600     PERFORM PRINT-DETAIL.                                        NC2024.2
067700*                                                                 NC2024.2
067800 ADD-INIT-F3-5-2.                                                 NC2024.2
067900     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
068000     MOVE "ADD-TEST-F3-5-2" TO PAR-NAME.                          NC2024.2
068100 ADD-TEST-F3-5-2.                                                 NC2024.2
068200     IF       FIELD1 OF ADD-16 EQUAL TO 99999                     NC2024.2
068300              PERFORM PASS                                        NC2024.2
068400              GO TO ADD-WRITE-F3-5-2.                             NC2024.2
068500     GO TO ADD-FAIL-F3-5-2.                                       NC2024.2
068600 ADD-DELETE-F3-5-2.                                               NC2024.2
068700     PERFORM DE-LETE.                                             NC2024.2
068800     GO TO ADD-WRITE-F3-5-2.                                      NC2024.2
068900 ADD-FAIL-F3-5-2.                                                 NC2024.2
069000     MOVE FIELD1 OF ADD-16 TO COMPUTED-N.                         NC2024.2
069100     MOVE     99999 TO CORRECT-N.                                 NC2024.2
069200     PERFORM FAIL.                                                NC2024.2
069300 ADD-WRITE-F3-5-2.                                                NC2024.2
069400     PERFORM PRINT-DETAIL.                                        NC2024.2
069500*                                                                 NC2024.2
069600 ADD-INIT-F3-5-3.                                                 NC2024.2
069700     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
069800     MOVE "ADD-TEST-F3-5-3" TO PAR-NAME.                          NC2024.2
069900 ADD-TEST-F3-5-3.                                                 NC2024.2
070000     IF FIELD2 OF ADD-16 IS EQUAL TO 777.77                       NC2024.2
070100            PERFORM PASS                                          NC2024.2
070200            GO TO ADD-WRITE-F3-5-3.                               NC2024.2
070300     GO TO ADD-FAIL-F3-5-3.                                       NC2024.2
070400 ADD-DELETE-F3-5-3.                                               NC2024.2
070500     PERFORM DE-LETE.                                             NC2024.2
070600     GO TO ADD-WRITE-F3-5-3.                                      NC2024.2
070700 ADD-FAIL-F3-5-3.                                                 NC2024.2
070800     PERFORM FAIL.                                                NC2024.2
070900     MOVE FIELD2 OF ADD-16 TO COMPUTED-N.                         NC2024.2
071000     MOVE "+777.77" TO CORRECT-A.                                 NC2024.2
071100 ADD-WRITE-F3-5-3.                                                NC2024.2
071200     PERFORM PRINT-DETAIL.                                        NC2024.2
071300*                                                                 NC2024.2
071400 ADD-INIT-F3-5-4.                                                 NC2024.2
071500     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
071600     MOVE "ADD-TEST-F3-5-4" TO PAR-NAME.                          NC2024.2
071700 ADD-TEST-F3-5-4.                                                 NC2024.2
071800     IF FIELD3 OF ADD-16 IS EQUAL TO 555.5                        NC2024.2
071900                  PERFORM PASS                                    NC2024.2
072000                  GO TO ADD-WRITE-F3-5-4.                         NC2024.2
072100     GO TO ADD-FAIL-F3-5-4.                                       NC2024.2
072200 ADD-DELETE-F3-5-4.                                               NC2024.2
072300     PERFORM DE-LETE.                                             NC2024.2
072400     GO TO ADD-WRITE-F3-5-4.                                      NC2024.2
072500 ADD-FAIL-F3-5-4.                                                 NC2024.2
072600     PERFORM FAIL.                                                NC2024.2
072700     MOVE FIELD3 OF ADD-16 TO COMPUTED-N.                         NC2024.2
072800     MOVE 555.5 TO CORRECT-N.                                     NC2024.2
072900 ADD-WRITE-F3-5-4.                                                NC2024.2
073000     PERFORM PRINT-DETAIL.                                        NC2024.2
073100*                                                                 NC2024.2
073200 ADD-INIT-F3-6.                                                   NC2024.2
073300     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
073400     MOVE "ADD-TEST-F3-6" TO PAR-NAME.                            NC2024.2
073500     MOVE "ADD CORRESPONDING   " TO FEATURE.                      NC2024.2
073600     MOVE 03   TO XYZ-1 OF CORR-DATA-1.                           NC2024.2
073700     MOVE 04   TO XYZ-2 OF CORR-DATA-1                            NC2024.2
073800     MOVE TEN  TO XYZ-3 OF CORR-DATA-1.                           NC2024.2
073900     MOVE ZERO TO XYZ-4 OF CORR-DATA-1.                           NC2024.2
074000     MOVE 01   TO XYZ-5 OF CORR-DATA-1.                           NC2024.2
074100     MOVE 00   TO XYZ-6 OF CORR-DATA-1.                           NC2024.2
074200     MOVE ZEROES TO CORR-DATA-2.                                  NC2024.2
074300 ADD-TEST-F3-6.                                                   NC2024.2
074400     ADD CORRESPONDING CORR-DATA-1 TO CORR-DATA-2.                NC2024.2
074500     IF XYZ-4 OF CORR-DATA-2 EQUAL TO ZERO                        NC2024.2
074600                PERFORM PASS                                      NC2024.2
074700                GO TO ADD-WRITE-F3-6.                             NC2024.2
074800     GO TO ADD-FAIL-F3-6.                                         NC2024.2
074900 ADD-DELETE-F3-6.                                                 NC2024.2
075000     PERFORM DE-LETE.                                             NC2024.2
075100     GO TO ADD-WRITE-F3-6.                                        NC2024.2
075200 ADD-FAIL-F3-6.                                                   NC2024.2
075300     PERFORM FAIL.                                                NC2024.2
075400     MOVE XYZ-4 OF CORR-DATA-2 TO COMPUTED-A.                     NC2024.2
075500     MOVE 00 TO CORRECT-A.                                        NC2024.2
075600 ADD-WRITE-F3-6.                                                  NC2024.2
075700     PERFORM PRINT-DETAIL.                                        NC2024.2
075800*                                                                 NC2024.2
075900 ADD-INIT-F3-7.                                                   NC2024.2
076000     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
076100     MOVE 03   TO XYZ-1 OF CORR-DATA-1.                           NC2024.2
076200     MOVE 04   TO XYZ-2 OF CORR-DATA-1                            NC2024.2
076300     MOVE TEN  TO XYZ-3 OF CORR-DATA-1.                           NC2024.2
076400     MOVE ZERO TO XYZ-4 OF CORR-DATA-1.                           NC2024.2
076500     MOVE 01   TO XYZ-5 OF CORR-DATA-1.                           NC2024.2
076600     MOVE 00   TO XYZ-6 OF CORR-DATA-1.                           NC2024.2
076700     MOVE 060820000200 TO CORR-DATA-2.                            NC2024.2
076800 ADD-INIT-F3-7-1.                                                 NC2024.2
076900     MOVE "ADD-TEST-F3-7-1" TO PAR-NAME.                          NC2024.2
077000 ADD-TEST-F3-7-1.                                                 NC2024.2
077100     ADD CORRESPONDING CORR-DATA-1 TO CORR-DATA-2.                NC2024.2
077200     IF XYZ-1 OF CORR-DATA-2 EQUAL TO 09                          NC2024.2
077300                 PERFORM PASS                                     NC2024.2
077400                 GO TO ADD-WRITE-F3-7-1.                          NC2024.2
077500     GO TO ADD-FAIL-F3-7-1.                                       NC2024.2
077600 ADD-DELETE-F3-7-1.                                               NC2024.2
077700     PERFORM DE-LETE.                                             NC2024.2
077800     GO TO ADD-WRITE-F3-7-1.                                      NC2024.2
077900 ADD-FAIL-F3-7-1.                                                 NC2024.2
078000     PERFORM FAIL.                                                NC2024.2
078100     MOVE XYZ-1 OF CORR-DATA-2 TO COMPUTED-A.                     NC2024.2
078200     MOVE "09" TO CORRECT-A.                                      NC2024.2
078300 ADD-WRITE-F3-7-1.                                                NC2024.2
078400     PERFORM PRINT-DETAIL.                                        NC2024.2
078500*                                                                 NC2024.2
078600 ADD-INIT-F3-7-2.                                                 NC2024.2
078700     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
078800     MOVE "ADD-TEST-F3-7-2" TO PAR-NAME.                          NC2024.2
078900 ADD-TEST-F3-7-2.                                                 NC2024.2
079000     IF XYZ-2 OF CORR-DATA-2 EQUAL TO 12                          NC2024.2
079100                 PERFORM PASS                                     NC2024.2
079200                 GO TO ADD-WRITE-F3-7-2.                          NC2024.2
079300     GO TO ADD-FAIL-F3-7-2.                                       NC2024.2
079400 ADD-DELETE-F3-7-2.                                               NC2024.2
079500     PERFORM DE-LETE.                                             NC2024.2
079600     GO TO ADD-WRITE-F3-7-2.                                      NC2024.2
079700 ADD-FAIL-F3-7-2.                                                 NC2024.2
079800     PERFORM FAIL.                                                NC2024.2
079900     MOVE XYZ-2 OF CORR-DATA-2 TO COMPUTED-A.                     NC2024.2
080000     MOVE "12" TO CORRECT-A.                                      NC2024.2
080100 ADD-WRITE-F3-7-2.                                                NC2024.2
080200     PERFORM PRINT-DETAIL.                                        NC2024.2
080300*                                                                 NC2024.2
080400 ADD-INIT-F3-7-3.                                                 NC2024.2
080500     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
080600     MOVE "ADD-TEST-F3-7-3" TO PAR-NAME.                          NC2024.2
080700 ADD-TEST-F3-7-3.                                                 NC2024.2
080800     IF XYZ-3 OF CORR-DATA-2 EQUAL TO 30                          NC2024.2
080900                 PERFORM PASS                                     NC2024.2
081000                 GO TO ADD-WRITE-F3-7-3.                          NC2024.2
081100     GO TO ADD-FAIL-F3-7-3.                                       NC2024.2
081200 ADD-DELETE-F3-7-3.                                               NC2024.2
081300     PERFORM DE-LETE.                                             NC2024.2
081400     GO TO ADD-WRITE-F3-7-3.                                      NC2024.2
081500 ADD-FAIL-F3-7-3.                                                 NC2024.2
081600     PERFORM FAIL.                                                NC2024.2
081700     MOVE XYZ-3 OF CORR-DATA-2 TO COMPUTED-A.                     NC2024.2
081800     MOVE "30" TO CORRECT-A.                                      NC2024.2
081900 ADD-WRITE-F3-7-3.                                                NC2024.2
082000     PERFORM PRINT-DETAIL.                                        NC2024.2
082100*                                                                 NC2024.2
082200 ADD-INIT-F3-7-4.                                                 NC2024.2
082300     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
082400     MOVE "ADD-TEST-F3-7-4" TO PAR-NAME.                          NC2024.2
082500 ADD-TEST-F3-7-4.                                                 NC2024.2
082600     IF XYZ-4 OF CORR-DATA-2 EQUAL TO 00                          NC2024.2
082700                 PERFORM PASS                                     NC2024.2
082800                 GO TO ADD-WRITE-F3-7-4.                          NC2024.2
082900     GO TO ADD-FAIL-F3-7-4.                                       NC2024.2
083000 ADD-DELETE-F3-7-4.                                               NC2024.2
083100     PERFORM DE-LETE.                                             NC2024.2
083200     GO TO ADD-WRITE-F3-7-4.                                      NC2024.2
083300 ADD-FAIL-F3-7-4.                                                 NC2024.2
083400     PERFORM FAIL.                                                NC2024.2
083500     MOVE XYZ-4 OF CORR-DATA-2 TO COMPUTED-A.                     NC2024.2
083600     MOVE "00" TO CORRECT-A.                                      NC2024.2
083700 ADD-WRITE-F3-7-4.                                                NC2024.2
083800     PERFORM PRINT-DETAIL.                                        NC2024.2
083900*                                                                 NC2024.2
084000 ADD-INIT-F3-7-5.                                                 NC2024.2
084100     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
084200     MOVE "ADD-TEST-F3-7-5" TO PAR-NAME.                          NC2024.2
084300 ADD-TEST-F3-7-5.                                                 NC2024.2
084400     IF XYZ-5 IN CORR-DATA-2 EQUAL TO 03                          NC2024.2
084500               PERFORM PASS                                       NC2024.2
084600               GO TO ADD-WRITE-F3-7-5.                            NC2024.2
084700     GO TO ADD-FAIL-F3-7-5.                                       NC2024.2
084800 DELETE-F3-7-5.                                                   NC2024.2
084900     PERFORM DE-LETE.                                             NC2024.2
085000     GO TO ADD-WRITE-F3-7-5.                                      NC2024.2
085100 ADD-FAIL-F3-7-5.                                                 NC2024.2
085200     MOVE XYZ-5 OF CORR-DATA-2 TO COMPUTED-A.                     NC2024.2
085300     MOVE "03" TO CORRECT-A.                                      NC2024.2
085400     PERFORM FAIL.                                                NC2024.2
085500 ADD-WRITE-F3-7-5.                                                NC2024.2
085600     PERFORM PRINT-DETAIL.                                        NC2024.2
085700*                                                                 NC2024.2
085800 ADD-INIT-F3-8.                                                   NC2024.2
085900     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
086000     MOVE ZERO TO WRK-AN-00001.                                   NC2024.2
086100     MOVE 03   TO XYZ-1 OF CORR-DATA-1.                           NC2024.2
086200     MOVE 04   TO XYZ-2 OF CORR-DATA-1                            NC2024.2
086300     MOVE TEN  TO XYZ-3 OF CORR-DATA-1.                           NC2024.2
086400     MOVE ZERO TO XYZ-4 OF CORR-DATA-1.                           NC2024.2
086500     MOVE 01   TO XYZ-5 OF CORR-DATA-1.                           NC2024.2
086600     MOVE 00   TO XYZ-6 OF CORR-DATA-1.                           NC2024.2
086700     MOVE 999999999999 TO CORR-DATA-2.                            NC2024.2
086800 ADD-INIT-F3-8-1.                                                 NC2024.2
086900     MOVE "ADD-TEST-F3-8-1" TO PAR-NAME.                          NC2024.2
087000 ADD-TEST-F3-8-1.                                                 NC2024.2
087100     ADD CORRESPONDING CORR-DATA-1 TO CORR-DATA-2 ON SIZE ERROR   NC2024.2
087200              MOVE 4 TO WRK-AN-00001.                             NC2024.2
087300     IF WRK-AN-00001 EQUAL TO "4"                                 NC2024.2
087400              PERFORM PASS                                        NC2024.2
087500              GO TO ADD-WRITE-F3-8-1.                             NC2024.2
087600     GO TO ADD-FAIL-F3-8-1.                                       NC2024.2
087700 ADD-DELETE-F3-8-1.                                               NC2024.2
087800     PERFORM DE-LETE.                                             NC2024.2
087900     GO TO ADD-WRITE-F3-8-1.                                      NC2024.2
088000 ADD-FAIL-F3-8-1.                                                 NC2024.2
088100     PERFORM FAIL.                                                NC2024.2
088200     MOVE WRK-AN-00001 TO COMPUTED-A.                             NC2024.2
088300     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC2024.2
088400 ADD-WRITE-F3-8-1.                                                NC2024.2
088500     PERFORM PRINT-DETAIL.                                        NC2024.2
088600*                                                                 NC2024.2
088700 ADD-INIT-F3-8-2.                                                 NC2024.2
088800     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
088900     MOVE "ADD-TEST-F3-8-2" TO PAR-NAME.                          NC2024.2
089000 ADD-TEST-F3-8-2.                                                 NC2024.2
089100     IF CORR-DATA-2 EQUAL TO "999999999999"                       NC2024.2
089200                 PERFORM PASS                                     NC2024.2
089300                 GO TO ADD-WRITE-F3-8-2.                          NC2024.2
089400     GO TO ADD-FAIL-F3-8-2.                                       NC2024.2
089500 ADD-DELETE-F3-8-2.                                               NC2024.2
089600     PERFORM DE-LETE.                                             NC2024.2
089700     GO TO ADD-WRITE-F3-8-2.                                      NC2024.2
089800 ADD-FAIL-F3-8-2.                                                 NC2024.2
089900     PERFORM FAIL.                                                NC2024.2
090000     MOVE CORR-DATA-2 TO COMPUTED-A.                              NC2024.2
090100     MOVE "999999999999" TO CORRECT-A.                            NC2024.2
090200     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC2024.2
090300 ADD-WRITE-F3-8-2.                                                NC2024.2
090400     PERFORM PRINT-DETAIL.                                        NC2024.2
090500*                                                                 NC2024.2
090600 ADD-INIT-F3-9-1.                                                 NC2024.2
090700     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
090800     MOVE "ADD-TEST-F3-9-1" TO PAR-NAME.                          NC2024.2
090900     MOVE ZEROES TO CORR-DATA-5.                                  NC2024.2
091000     MOVE 222222222222 TO CORR-DATA-1.                            NC2024.2
091100 ADD-TEST-F3-9-1.                                                 NC2024.2
091200     ADD CORRESPONDING CORR-DATA-1 TO CORR-DATA-5.                NC2024.2
091300     IF XYZ-1 OF CORR-DATA-5 EQUAL TO 22                          NC2024.2
091400                PERFORM PASS                                      NC2024.2
091500                GO TO ADD-WRITE-F3-9-1.                           NC2024.2
091600     GO TO ADD-FAIL-F3-9-1.                                       NC2024.2
091700 ADD-DELETE-F3-9-1.                                               NC2024.2
091800     PERFORM DE-LETE.                                             NC2024.2
091900     GO TO ADD-WRITE-F3-9-1.                                      NC2024.2
092000 ADD-FAIL-F3-9-1.                                                 NC2024.2
092100     MOVE XYZ-1 OF CORR-DATA-5 TO COMPUTED-A.                     NC2024.2
092200     MOVE "22" TO CORRECT-A.                                      NC2024.2
092300     PERFORM FAIL.                                                NC2024.2
092400 ADD-WRITE-F3-9-1.                                                NC2024.2
092500     PERFORM PRINT-DETAIL.                                        NC2024.2
092600*                                                                 NC2024.2
092700 ADD-INIT-F3-9-2.                                                 NC2024.2
092800     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
092900     MOVE "ADD-TEST-F3-9-2" TO PAR-NAME.                          NC2024.2
093000 ADD-TEST-F3-9-2.                                                 NC2024.2
093100     IF XYZ-13 OF CORR-DATA-5 EQUAL TO 00                         NC2024.2
093200              PERFORM PASS                                        NC2024.2
093300              GO TO ADD-WRITE-F3-9-2.                             NC2024.2
093400     GO TO ADD-FAIL-F3-9-2.                                       NC2024.2
093500 ADD-DELETE-F3-9-2.                                               NC2024.2
093600     PERFORM DE-LETE.                                             NC2024.2
093700     GO TO ADD-WRITE-F3-9-2.                                      NC2024.2
093800 ADD-FAIL-F3-9-2.                                                 NC2024.2
093900     MOVE XYZ-13 OF CORR-DATA-5 TO COMPUTED-A.                    NC2024.2
094000     MOVE "00" TO CORRECT-A.                                      NC2024.2
094100     PERFORM FAIL.                                                NC2024.2
094200 ADD-WRITE-F3-9-2.                                                NC2024.2
094300     PERFORM PRINT-DETAIL.                                        NC2024.2
094400*                                                                 NC2024.2
094500 ADD-INIT-F3-9-3.                                                 NC2024.2
094600     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
094700     MOVE "ADD-TEST-F3-9-3" TO PAR-NAME.                          NC2024.2
094800 ADD-TEST-F3-9-3.                                                 NC2024.2
094900     IF XYZ-11 OF CORR-DATA-5 EQUAL TO 00                         NC2024.2
095000                  PERFORM PASS                                    NC2024.2
095100                  GO TO ADD-WRITE-F3-9-3.                         NC2024.2
095200     GO TO ADD-FAIL-F3-9-3.                                       NC2024.2
095300 ADD-DELETE-F3-9-3.                                               NC2024.2
095400     PERFORM DE-LETE.                                             NC2024.2
095500     GO TO ADD-WRITE-F3-9-3.                                      NC2024.2
095600 ADD-FAIL-F3-9-3.                                                 NC2024.2
095700     MOVE XYZ-11 OF CORR-DATA-5 TO COMPUTED-A.                    NC2024.2
095800     MOVE "00" TO CORRECT-A.                                      NC2024.2
095900     PERFORM FAIL.                                                NC2024.2
096000 ADD-WRITE-F3-9-3.                                                NC2024.2
096100     PERFORM PRINT-DETAIL.                                        NC2024.2
096200*                                                                 NC2024.2
096300 ADD-INIT-F3-10.                                                  NC2024.2
096400     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
096500     MOVE ZERO  TO           CORR-DATA-5.                         NC2024.2
096600     MOVE 10    TO XYZ-1  OF CORR-DATA-5.                         NC2024.2
096700     MOVE 98    TO XYZ-2  OF CORR-DATA-5.                         NC2024.2
096800     MOVE 01    TO XYZ-11 OF CORR-DATA-5.                         NC2024.2
096900     MOVE 10.45 TO XYZ-1  OF CORR-DATA-7.                         NC2024.2
097000     MOVE  0.9  TO XYZ-2  OF CORR-DATA-7.                         NC2024.2
097100     MOVE ZERO  TO XYZ-11 OF CORR-DATA-7.                         NC2024.2
097200 ADD-INIT-F3-10-1.                                                NC2024.2
097300     MOVE "ADD-TEST-F3-10-1" TO PAR-NAME.                         NC2024.2
097400 ADD-TEST-F3-10-1.                                                NC2024.2
097500     ADD CORRESPONDING CORR-DATA-7 TO CORR-DATA-5.                NC2024.2
097600     IF XYZ-1 IN CORR-DATA-5 EQUAL TO 20                          NC2024.2
097700                 PERFORM PASS                                     NC2024.2
097800                 GO TO ADD-WRITE-F3-10-1.                         NC2024.2
097900     GO TO ADD-FAIL-F3-10-1.                                      NC2024.2
098000 ADD-DELETE-F3-10-1.                                              NC2024.2
098100     PERFORM DE-LETE.                                             NC2024.2
098200     GO TO ADD-WRITE-F3-10-1.                                     NC2024.2
098300 ADD-FAIL-F3-10-1.                                                NC2024.2
098400     PERFORM FAIL.                                                NC2024.2
098500     MOVE XYZ-1 OF CORR-DATA-5 TO COMPUTED-N.                     NC2024.2
098600     MOVE "+20.0000" TO CORRECT-A.                                NC2024.2
098700 ADD-WRITE-F3-10-1.                                               NC2024.2
098800     PERFORM PRINT-DETAIL.                                        NC2024.2
098900*                                                                 NC2024.2
099000 ADD-INIT-F3-10-2.                                                NC2024.2
099100     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
099200     MOVE "ADD-TEST-F3-10-2" TO PAR-NAME.                         NC2024.2
099300 ADD-TEST-F3-10-2.                                                NC2024.2
099400     IF XYZ-2 IN CORR-DATA-5 EQUAL TO 98                          NC2024.2
099500                 PERFORM PASS                                     NC2024.2
099600                 GO TO ADD-WRITE-F3-10-2.                         NC2024.2
099700     GO TO ADD-FAIL-F3-10-2.                                      NC2024.2
099800 ADD-DELETE-F3-10-2.                                              NC2024.2
099900     PERFORM DE-LETE.                                             NC2024.2
100000     GO TO ADD-WRITE-F3-10-2.                                     NC2024.2
100100 ADD-FAIL-F3-10-2.                                                NC2024.2
100200     PERFORM FAIL.                                                NC2024.2
100300     MOVE XYZ-2 OF CORR-DATA-5 TO COMPUTED-N.                     NC2024.2
100400     MOVE "98.0000" TO CORRECT-A.                                 NC2024.2
100500 ADD-WRITE-F3-10-2.                                               NC2024.2
100600     PERFORM PRINT-DETAIL.                                        NC2024.2
100700*                                                                 NC2024.2
100800 ADD-INIT-F3-10-3.                                                NC2024.2
100900     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
101000     MOVE "ADD-TEST-F3-10-3" TO PAR-NAME.                         NC2024.2
101100 ADD-TEST-F3-10-3.                                                NC2024.2
101200     IF XYZ-11 OF CORR-DATA-5 EQUAL TO 01                         NC2024.2
101300                  PERFORM PASS                                    NC2024.2
101400                  GO TO ADD-WRITE-F3-10-3.                        NC2024.2
101500     GO TO ADD-FAIL-F3-10-3.                                      NC2024.2
101600 ADD-DELETE-F3-10-3.                                              NC2024.2
101700     PERFORM DE-LETE.                                             NC2024.2
101800     GO TO ADD-WRITE-F3-10-3.                                     NC2024.2
101900 ADD-FAIL-F3-10-3.                                                NC2024.2
102000     PERFORM FAIL.                                                NC2024.2
102100     MOVE XYZ-11 OF CORR-DATA-5 TO COMPUTED-N.                    NC2024.2
102200     MOVE "+01.0000" TO CORRECT-A.                                NC2024.2
102300 ADD-WRITE-F3-10-3.                                               NC2024.2
102400     PERFORM PRINT-DETAIL.                                        NC2024.2
102500*                                                                 NC2024.2
102600 ADD-INIT-F3-11.                                                  NC2024.2
102700     MOVE ZERO  TO           CORR-DATA-5.                         NC2024.2
102800     MOVE 10    TO XYZ-1  OF CORR-DATA-5.                         NC2024.2
102900     MOVE 98    TO XYZ-2  OF CORR-DATA-5.                         NC2024.2
103000     MOVE 01    TO XYZ-11 OF CORR-DATA-5.                         NC2024.2
103100     MOVE 10.45 TO XYZ-1  OF CORR-DATA-7.                         NC2024.2
103200     MOVE  0.9  TO XYZ-2  OF CORR-DATA-7.                         NC2024.2
103300     MOVE ZERO  TO XYZ-11 OF CORR-DATA-7.                         NC2024.2
103400 ADD-INIT-F3-11-1.                                                NC2024.2
103500     MOVE "ADD-TEST-F3-11-1" TO PAR-NAME.                         NC2024.2
103600 ADD-TEST-F3-11-1.                                                NC2024.2
103700     ADD CORRESPONDING CORR-DATA-7 TO CORR-DATA-5 ROUNDED.        NC2024.2
103800     IF XYZ-1 OF CORR-DATA-5 EQUAL TO 20                          NC2024.2
103900                 PERFORM PASS                                     NC2024.2
104000                 GO TO ADD-WRITE-F3-11-1.                         NC2024.2
104100     GO TO ADD-FAIL-F3-11-1.                                      NC2024.2
104200 ADD-DELETE-F3-11-1.                                              NC2024.2
104300     PERFORM DE-LETE.                                             NC2024.2
104400     GO TO ADD-WRITE-F3-11-1.                                     NC2024.2
104500 ADD-FAIL-F3-11-1.                                                NC2024.2
104600     PERFORM FAIL.                                                NC2024.2
104700     MOVE XYZ-1 OF CORR-DATA-5 TO COMPUTED-N.                     NC2024.2
104800     MOVE 20.0000 TO CORRECT-N.                                   NC2024.2
104900 ADD-WRITE-F3-11-1.                                               NC2024.2
105000     PERFORM PRINT-DETAIL.                                        NC2024.2
105100*                                                                 NC2024.2
105200 ADD-INIT-F3-11-2.                                                NC2024.2
105300     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
105400     MOVE "ADD-TEST-F3-11-2" TO PAR-NAME.                         NC2024.2
105500 ADD-TEST-F3-11-2.                                                NC2024.2
105600     IF XYZ-2 OF CORR-DATA-5 EQUAL TO 99                          NC2024.2
105700                 PERFORM PASS                                     NC2024.2
105800                 GO TO ADD-WRITE-F3-11-2.                         NC2024.2
105900     GO TO ADD-FAIL-F3-11-2.                                      NC2024.2
106000 ADD-DELETE-F3-11-2.                                              NC2024.2
106100     PERFORM DE-LETE.                                             NC2024.2
106200     GO TO ADD-WRITE-F3-11-2.                                     NC2024.2
106300 ADD-FAIL-F3-11-2.                                                NC2024.2
106400     MOVE XYZ-2 OF CORR-DATA-5 TO COMPUTED-N.                     NC2024.2
106500     MOVE "+99.0000" TO CORRECT-A.                                NC2024.2
106600     PERFORM FAIL.                                                NC2024.2
106700 ADD-WRITE-F3-11-2.                                               NC2024.2
106800     PERFORM PRINT-DETAIL.                                        NC2024.2
106900*                                                                 NC2024.2
107000 ADD-INIT-F3-12.                                                  NC2024.2
107100     MOVE   "VI-74 6.6.4 GR3" TO ANSI-REFERENCE.                  NC2024.2
107200     MOVE "ADD-TEST-F3-12" TO PAR-NAME.                           NC2024.2
107300     MOVE ZERO  TO           CORR-DATA-5.                         NC2024.2
107400     MOVE 10    TO XYZ-1  OF CORR-DATA-5.                         NC2024.2
107500     MOVE 99    TO XYZ-2  OF CORR-DATA-5.                         NC2024.2
107600     MOVE 01    TO XYZ-11 OF CORR-DATA-5.                         NC2024.2
107700     MOVE 10.45 TO XYZ-1  OF CORR-DATA-7.                         NC2024.2
107800     MOVE  0.9  TO XYZ-2  OF CORR-DATA-7.                         NC2024.2
107900     MOVE ZERO  TO XYZ-11 OF CORR-DATA-7.                         NC2024.2
108000     MOVE SPACE TO WRK-AN-00001.                                  NC2024.2
108100 ADD-TEST-F3-12.                                                  NC2024.2
108200     ADD CORRESPONDING CORR-DATA-7 TO CORR-DATA-5 ROUNDED         NC2024.2
108300       ON SIZE ERROR MOVE "W" TO WRK-AN-00001.                    NC2024.2
108400     IF WRK-AN-00001 EQUAL TO "W"                                 NC2024.2
108500                  PERFORM PASS                                    NC2024.2
108600                  GO TO ADD-WRITE-F3-12.                          NC2024.2
108700     GO TO ADD-FAIL-F3-12.                                        NC2024.2
108800 ADD-DELETE-F3-12.                                                NC2024.2
108900     PERFORM DE-LETE.                                             NC2024.2
109000     GO TO ADD-WRITE-F3-12.                                       NC2024.2
109100 ADD-FAIL-F3-12.                                                  NC2024.2
109200     PERFORM FAIL.                                                NC2024.2
109300     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC2024.2
109400 ADD-WRITE-F3-12.                                                 NC2024.2
109500     PERFORM PRINT-DETAIL.                                        NC2024.2
109600*                                                                 NC2024.2
109700 ADD-INIT-F3-13.                                                  NC2024.2
109800     MOVE "ADD-TEST-F3-13" TO PAR-NAME.                           NC2024.2
109900     MOVE   "VI-74 6.6.4" TO ANSI-REFERENCE.                      NC2024.2
110000     MOVE  "ADD CORRESPONDING" TO FEATURE.                        NC2024.2
110100 BUILD-TABLE1.                                                    NC2024.2
110200     MOVE 06 TO RECORD1 OF TABLE1.                                NC2024.2
110300     MOVE 01 TO RECORD2 OF TABLE1 (1).                            NC2024.2
110400     MOVE 02 TO RECORD2 OF TABLE1 (2).                            NC2024.2
110500     MOVE 07 TO RECORD3 OF TABLE1.                                NC2024.2
110600 BUILD-TABLE2.                                                    NC2024.2
110700     MOVE 08 TO RECORD1 OF TABLE2.                                NC2024.2
110800     MOVE 03 TO RECORD2 OF TABLE2 (1).                            NC2024.2
110900     MOVE 04 TO RECORD2 OF TABLE2 (2).                            NC2024.2
111000     MOVE 09 TO RECORD3 OF TABLE2.                                NC2024.2
111100 ADD-TEST-F3-13.                                                  NC2024.2
111200     ADD CORRESPONDING TABLE1 TO TABLE2.                          NC2024.2
111300     IF RECORD1 OF TABLE2 = 14 AND                                NC2024.2
111400        RECORD2 OF TABLE2 (1) = 03 AND                            NC2024.2
111500        RECORD2 OF TABLE2 (2) = 04 AND                            NC2024.2
111600        RECORD3 OF TABLE2 = 16                                    NC2024.2
111700           PERFORM PASS                                           NC2024.2
111800           GO TO ADD-WRITE-F3-13.                                 NC2024.2
111900     GO TO ADD-FAIL-F3-13.                                        NC2024.2
112000 ADD-DELETE-F3-13.                                                NC2024.2
112100     PERFORM DE-LETE.                                             NC2024.2
112200     GO TO ADD-WRITE-F3-13.                                       NC2024.2
112300 ADD-FAIL-F3-13.                                                  NC2024.2
112400     PERFORM FAIL.                                                NC2024.2
112500     MOVE TABLE2 TO COMPUTED-A.                                   NC2024.2
112600     MOVE "14030416" TO CORRECT-A.                                NC2024.2
112700 ADD-WRITE-F3-13.                                                 NC2024.2
112800     PERFORM PRINT-DETAIL.                                        NC2024.2
112900*                                                                 NC2024.2
113000 ADD-INIT-F3-14.                                                  NC2024.2
113100     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
113200*    ===-->  NO SIZE ERROR  <--===                                NC2024.2
113300     MOVE    1 TO REC-CT.                                         NC2024.2
113400     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
113500     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
113600     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
113700     MOVE    88888  TO FIELD1 OF ADD-16.                          NC2024.2
113800     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
113900     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
114000     MOVE   "ADD-TEST-F3-14-0" TO PAR-NAME.                       NC2024.2
114100 ADD-TEST-F3-14-1.                                                NC2024.2
114200     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
114300             ON SIZE ERROR                                        NC2024.2
114400             GO TO ADD-FAIL-F3-14-1.                              NC2024.2
114500     PERFORM PASS.                                                NC2024.2
114600     GO TO   ADD-WRITE-F3-14-1.                                   NC2024.2
114700 ADD-DELETE-F3-14-1.                                              NC2024.2
114800     PERFORM DE-LETE.                                             NC2024.2
114900     GO TO ADD-WRITE-F3-14-1.                                     NC2024.2
115000 ADD-FAIL-F3-14-1.                                                NC2024.2
115100     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"                NC2024.2
115200            TO RE-MARK                                            NC2024.2
115300     PERFORM FAIL.                                                NC2024.2
115400 ADD-WRITE-F3-14-1.                                               NC2024.2
115500     PERFORM PRINT-DETAIL.                                        NC2024.2
115600*                                                                 NC2024.2
115700 ADD-INIT-F3-14-2.                                                NC2024.2
115800     MOVE   "ADD-TEST-F3-14-2" TO PAR-NAME.                       NC2024.2
115900     ADD     1 TO REC-CT.                                         NC2024.2
116000 ADD-TEST-F3-14-2.                                                NC2024.2
116100     IF      FIELD1 OF ADD-16 EQUAL TO 88889                      NC2024.2
116200             PERFORM PASS                                         NC2024.2
116300             GO TO ADD-WRITE-F3-14-2.                             NC2024.2
116400     GO TO ADD-FAIL-F3-14-2.                                      NC2024.2
116500 ADD-DELETE-F3-14-2.                                              NC2024.2
116600     PERFORM DE-LETE                                              NC2024.2
116700     GO TO ADD-WRITE-F3-14-2.                                     NC2024.2
116800 ADD-FAIL-F3-14-2.                                                NC2024.2
116900     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
117000     MOVE    88889 TO CORRECT-N                                   NC2024.2
117100     PERFORM FAIL.                                                NC2024.2
117200 ADD-WRITE-F3-14-2.                                               NC2024.2
117300     PERFORM PRINT-DETAIL.                                        NC2024.2
117400*                                                                 NC2024.2
117500 ADD-INIT-F3-14-3.                                                NC2024.2
117600     MOVE   "ADD-TEST-F3-14-3" TO PAR-NAME.                       NC2024.2
117700     ADD     1 TO REC-CT.                                         NC2024.2
117800 ADD-TEST-F3-14-3.                                                NC2024.2
117900     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
118000             PERFORM PASS                                         NC2024.2
118100             GO TO ADD-WRITE-F3-14-3.                             NC2024.2
118200     GO TO ADD-FAIL-F3-14-3.                                      NC2024.2
118300 ADD-DELETE-F3-14-3.                                              NC2024.2
118400     PERFORM DE-LETE.                                             NC2024.2
118500     GO TO ADD-WRITE-F3-14-3.                                     NC2024.2
118600 ADD-FAIL-F3-14-3.                                                NC2024.2
118700     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
118800     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
118900     PERFORM FAIL.                                                NC2024.2
119000 ADD-WRITE-F3-14-3.                                               NC2024.2
119100     PERFORM PRINT-DETAIL.                                        NC2024.2
119200*                                                                 NC2024.2
119300 ADD-INIT-F3-14-4.                                                NC2024.2
119400     MOVE   "ADD-TEST-F3-14-4" TO PAR-NAME.                       NC2024.2
119500     ADD     1 TO REC-CT.                                         NC2024.2
119600 ADD-TEST-F3-14-4.                                                NC2024.2
119700     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
119800             PERFORM PASS                                         NC2024.2
119900             GO TO ADD-WRITE-F3-14-4.                             NC2024.2
120000     GO TO ADD-FAIL-F3-14-4.                                      NC2024.2
120100 ADD-DELETE-F3-14-4.                                              NC2024.2
120200     PERFORM DE-LETE.                                             NC2024.2
120300     GO TO ADD-WRITE-F3-14-4.                                     NC2024.2
120400 ADD-FAIL-F3-14-4.                                                NC2024.2
120500     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
120600     MOVE 555.5 TO CORRECT-N                                      NC2024.2
120700     PERFORM FAIL.                                                NC2024.2
120800 ADD-WRITE-F3-14-4.                                               NC2024.2
120900     PERFORM PRINT-DETAIL.                                        NC2024.2
121000*                                                                 NC2024.2
121100 ADD-INIT-F3-15.                                                  NC2024.2
121200*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2024.2
121300*    ===-->      SIZE ERROR        <--===                         NC2024.2
121400     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
121500     MOVE    0 TO REC-CT.                                         NC2024.2
121600     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
121700     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
121800     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
121900     MOVE    99999  TO FIELD1 OF ADD-16.                          NC2024.2
122000     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
122100     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
122200*                                                                 NC2024.2
122300 ADD-INIT-F3-15-1.                                                NC2024.2
122400     MOVE   "ADD-TEST-F3-15-1" TO PAR-NAME.                       NC2024.2
122500     ADD     1 TO REC-CT.                                         NC2024.2
122600 ADD-TEST-F3-15-1.                                                NC2024.2
122700     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
122800         NOT ON SIZE ERROR                                        NC2024.2
122900             GO TO ADD-FAIL-F3-15-1.                              NC2024.2
123000     PERFORM PASS.                                                NC2024.2
123100     GO TO ADD-WRITE-F3-15-1.                                     NC2024.2
123200 ADD-DELETE-F3-15-1.                                              NC2024.2
123300     PERFORM DE-LETE.                                             NC2024.2
123400     GO TO   ADD-WRITE-F3-15-1.                                   NC2024.2
123500 ADD-FAIL-F3-15-1.                                                NC2024.2
123600     MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"            NC2024.2
123700     TO RE-MARK                                                   NC2024.2
123800     PERFORM FAIL.                                                NC2024.2
123900 ADD-WRITE-F3-15-1.                                               NC2024.2
124000     PERFORM PRINT-DETAIL.                                        NC2024.2
124100*                                                                 NC2024.2
124200 ADD-INIT-F3-15-2.                                                NC2024.2
124300     MOVE   "ADD-TEST-F3-15-2" TO PAR-NAME.                       NC2024.2
124400     ADD     1 TO REC-CT.                                         NC2024.2
124500 ADD-TEST-F3-15-2.                                                NC2024.2
124600     IF      FIELD1 OF ADD-16 EQUAL TO 99999                      NC2024.2
124700             PERFORM PASS                                         NC2024.2
124800             GO TO ADD-WRITE-F3-15-2.                             NC2024.2
124900     GO TO ADD-FAIL-F3-15-2.                                      NC2024.2
125000 ADD-DELETE-F3-15-2.                                              NC2024.2
125100     PERFORM DE-LETE.                                             NC2024.2
125200     GO TO ADD-WRITE-F3-15-2.                                     NC2024.2
125300 ADD-FAIL-F3-15-2.                                                NC2024.2
125400     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
125500     MOVE    99999 TO CORRECT-N                                   NC2024.2
125600     PERFORM FAIL.                                                NC2024.2
125700 ADD-WRITE-F3-15-2.                                               NC2024.2
125800     PERFORM PRINT-DETAIL.                                        NC2024.2
125900*                                                                 NC2024.2
126000 ADD-INIT-F3-15-3.                                                NC2024.2
126100     MOVE   "ADD-TEST-F3-15-3" TO PAR-NAME.                       NC2024.2
126200     ADD     1 TO REC-CT.                                         NC2024.2
126300 ADD-TEST-F3-15-3.                                                NC2024.2
126400     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
126500             PERFORM PASS                                         NC2024.2
126600             GO TO ADD-WRITE-F3-15-3.                             NC2024.2
126700     GO TO ADD-FAIL-F3-15-3.                                      NC2024.2
126800 ADD-DELETE-F3-15-3.                                              NC2024.2
126900     PERFORM DE-LETE.                                             NC2024.2
127000     GO TO ADD-WRITE-F3-15-3.                                     NC2024.2
127100 ADD-FAIL-F3-15-3.                                                NC2024.2
127200     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
127300     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
127400     PERFORM FAIL.                                                NC2024.2
127500 ADD-WRITE-F3-15-3.                                               NC2024.2
127600     PERFORM PRINT-DETAIL.                                        NC2024.2
127700*                                                                 NC2024.2
127800 ADD-INIT-F3-15-4.                                                NC2024.2
127900     MOVE   "ADD-TEST-F3-15-4" TO PAR-NAME.                       NC2024.2
128000     ADD     1 TO REC-CT.                                         NC2024.2
128100 ADD-TEST-F3-15-4.                                                NC2024.2
128200     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
128300             PERFORM PASS                                         NC2024.2
128400             GO TO ADD-WRITE-F3-15-4.                             NC2024.2
128500     GO TO ADD-FAIL-F3-15-4.                                      NC2024.2
128600 ADD-DELETE-F3-15-4.                                              NC2024.2
128700     PERFORM DE-LETE.                                             NC2024.2
128800     GO TO ADD-WRITE-F3-15-4.                                     NC2024.2
128900 ADD-FAIL-F3-15-4.                                                NC2024.2
129000     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
129100     MOVE 555.5 TO CORRECT-N                                      NC2024.2
129200     PERFORM FAIL.                                                NC2024.2
129300 ADD-WRITE-F3-15-4.                                               NC2024.2
129400             PERFORM PRINT-DETAIL.                                NC2024.2
129500*                                                                 NC2024.2
129600 ADD-INIT-F3-16.                                                  NC2024.2
129700*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2024.2
129800*    ===-->   NO SIZE ERROR        <--===                         NC2024.2
129900     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
130000     MOVE    1 TO REC-CT.                                         NC2024.2
130100     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
130200     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
130300     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
130400     MOVE    88888  TO FIELD1 OF ADD-16.                          NC2024.2
130500     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
130600     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
130700 ADD-INIT-F3-16-1.                                                NC2024.2
130800     MOVE   "ADD-TEST-F3-16-1" TO PAR-NAME.                       NC2024.2
130900 ADD-TEST-F3-16-1.                                                NC2024.2
131000     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
131100         NOT ON SIZE ERROR                                        NC2024.2
131200             PERFORM PASS                                         NC2024.2
131300             GO TO ADD-WRITE-F3-16-1.                             NC2024.2
131400     GO TO   ADD-FAIL-F3-16-1.                                    NC2024.2
131500 ADD-DELETE-F3-16-1.                                              NC2024.2
131600     PERFORM DE-LETE.                                             NC2024.2
131700     GO TO ADD-WRITE-F3-16-1.                                     NC2024.2
131800 ADD-FAIL-F3-16-1.                                                NC2024.2
131900     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.    NC2024.2
132000     PERFORM FAIL.                                                NC2024.2
132100 ADD-WRITE-F3-16-1.                                               NC2024.2
132200     PERFORM PRINT-DETAIL.                                        NC2024.2
132300*                                                                 NC2024.2
132400 ADD-INIT-F3-16-2.                                                NC2024.2
132500     MOVE   "ADD-TEST-F3-16-2" TO PAR-NAME.                       NC2024.2
132600     ADD     1 TO REC-CT.                                         NC2024.2
132700 ADD-TEST-F3-16-2.                                                NC2024.2
132800     IF      FIELD1 OF ADD-16 EQUAL TO 88889                      NC2024.2
132900             PERFORM PASS                                         NC2024.2
133000             GO TO ADD-WRITE-F3-16-2.                             NC2024.2
133100     GO TO ADD-FAIL-F3-16-2.                                      NC2024.2
133200 ADD-DELETE-F3-16-2.                                              NC2024.2
133300     PERFORM DE-LETE.                                             NC2024.2
133400     GO TO ADD-WRITE-F3-16-2.                                     NC2024.2
133500 ADD-FAIL-F3-16-2.                                                NC2024.2
133600     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
133700     MOVE    88889 TO CORRECT-N                                   NC2024.2
133800     PERFORM FAIL.                                                NC2024.2
133900 ADD-WRITE-F3-16-2.                                               NC2024.2
134000             PERFORM PRINT-DETAIL.                                NC2024.2
134100*                                                                 NC2024.2
134200 ADD-INIT-F3-16-3.                                                NC2024.2
134300     MOVE   "ADD-TEST-F3-16-3" TO PAR-NAME.                       NC2024.2
134400     ADD     1 TO REC-CT.                                         NC2024.2
134500 ADD-TEST-F3-16-3.                                                NC2024.2
134600     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
134700             PERFORM PASS                                         NC2024.2
134800             GO TO ADD-WRITE-F3-16-3.                             NC2024.2
134900     GO TO ADD-FAIL-F3-16-3.                                      NC2024.2
135000 ADD-DELETE-F3-16-3.                                              NC2024.2
135100     PERFORM DE-LETE.                                             NC2024.2
135200     GO TO ADD-WRITE-F3-16-3.                                     NC2024.2
135300 ADD-FAIL-F3-16-3.                                                NC2024.2
135400     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
135500     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
135600     PERFORM FAIL.                                                NC2024.2
135700 ADD-WRITE-F3-16-3.                                               NC2024.2
135800             PERFORM PRINT-DETAIL.                                NC2024.2
135900*                                                                 NC2024.2
136000 ADD-INIT-F3-16-4.                                                NC2024.2
136100     MOVE   "ADD-TEST-F3-16-4" TO PAR-NAME.                       NC2024.2
136200     ADD     1 TO REC-CT.                                         NC2024.2
136300 ADD-TEST-F3-16-4.                                                NC2024.2
136400     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
136500             PERFORM PASS                                         NC2024.2
136600             GO TO ADD-WRITE-F3-16-4.                             NC2024.2
136700     GO TO ADD-FAIL-F3-16-4.                                      NC2024.2
136800 ADD-DELETE-F3-16-4.                                              NC2024.2
136900     PERFORM DE-LETE.                                             NC2024.2
137000     GO TO ADD-WRITE-F3-16-4.                                     NC2024.2
137100 ADD-FAIL-F3-16-4.                                                NC2024.2
137200     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
137300     MOVE 555.5 TO CORRECT-N                                      NC2024.2
137400     PERFORM FAIL.                                                NC2024.2
137500 ADD-WRITE-F3-16-4.                                               NC2024.2
137600     PERFORM PRINT-DETAIL.                                        NC2024.2
137700*                                                                 NC2024.2
137800 ADD-INIT-F3-17.                                                  NC2024.2
137900*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2024.2
138000*    ===-->      SIZE ERROR        <--===                         NC2024.2
138100     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
138200     MOVE    1 TO REC-CT.                                         NC2024.2
138300     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
138400     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
138500     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
138600     MOVE    99999  TO FIELD1 OF ADD-16.                          NC2024.2
138700     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
138800     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
138900 ADD-INIT-F3-17-1.                                                NC2024.2
139000     MOVE   "ADD-TEST-F3-17-1" TO PAR-NAME.                       NC2024.2
139100 ADD-TEST-F3-17-1.                                                NC2024.2
139200     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
139300             ON SIZE ERROR                                        NC2024.2
139400             PERFORM PASS                                         NC2024.2
139500             GO TO ADD-WRITE-F3-17-1                              NC2024.2
139600         NOT ON SIZE ERROR                                        NC2024.2
139700             GO TO ADD-FAIL-F3-17-1.                              NC2024.2
139800 ADD-DELETE-F3-17.                                                NC2024.2
139900     PERFORM DE-LETE.                                             NC2024.2
140000     GO TO   ADD-WRITE-F3-17-1.                                   NC2024.2
140100 ADD-FAIL-F3-17-1.                                                NC2024.2
140200     MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"                    NC2024.2
140300          TO RE-MARK                                              NC2024.2
140400     PERFORM FAIL.                                                NC2024.2
140500 ADD-WRITE-F3-17-1.                                               NC2024.2
140600     PERFORM PRINT-DETAIL.                                        NC2024.2
140700*                                                                 NC2024.2
140800 ADD-INIT-F3-17-2.                                                NC2024.2
140900     MOVE   "ADD-TEST-F3-17-2" TO PAR-NAME.                       NC2024.2
141000     ADD     1 TO REC-CT.                                         NC2024.2
141100 ADD-TEST-F3-17-2.                                                NC2024.2
141200     IF      FIELD1 OF ADD-16 EQUAL TO 99999                      NC2024.2
141300             PERFORM PASS                                         NC2024.2
141400             GO TO ADD-WRITE-F3-17-2.                             NC2024.2
141500     GO TO ADD-FAIL-F3-17-2.                                      NC2024.2
141600 ADD-DELETE-F3-17-2.                                              NC2024.2
141700     PERFORM DE-LETE.                                             NC2024.2
141800     GO TO ADD-WRITE-F3-17-2.                                     NC2024.2
141900 ADD-FAIL-F3-17-2.                                                NC2024.2
142000     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
142100     MOVE    99999 TO CORRECT-N                                   NC2024.2
142200     PERFORM FAIL.                                                NC2024.2
142300 ADD-WRITE-F3-17-2.                                               NC2024.2
142400     PERFORM PRINT-DETAIL.                                        NC2024.2
142500*                                                                 NC2024.2
142600 ADD-INIT-F3-17-3.                                                NC2024.2
142700     MOVE   "ADD-TEST-F3-17-3" TO PAR-NAME.                       NC2024.2
142800     ADD     1 TO REC-CT.                                         NC2024.2
142900 ADD-TEST-F3-17-3.                                                NC2024.2
143000     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
143100             PERFORM PASS                                         NC2024.2
143200             GO TO ADD-WRITE-F3-17-3.                             NC2024.2
143300     GO TO ADD-FAIL-F3-17-3.                                      NC2024.2
143400 ADD-DELETE-F3-17-3.                                              NC2024.2
143500     PERFORM DE-LETE.                                             NC2024.2
143600     GO TO ADD-WRITE-F3-17-3.                                     NC2024.2
143700 ADD-FAIL-F3-17-3.                                                NC2024.2
143800     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
143900     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
144000     PERFORM FAIL.                                                NC2024.2
144100 ADD-WRITE-F3-17-3.                                               NC2024.2
144200     PERFORM PRINT-DETAIL.                                        NC2024.2
144300*                                                                 NC2024.2
144400 ADD-INIT-F3-17-4.                                                NC2024.2
144500     MOVE   "ADD-TEST-F3-17-4" TO PAR-NAME.                       NC2024.2
144600     ADD     1 TO REC-CT.                                         NC2024.2
144700 ADD-TEST-F3-17-4.                                                NC2024.2
144800     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
144900             PERFORM PASS                                         NC2024.2
145000             GO TO ADD-WRITE-F3-17-4.                             NC2024.2
145100     GO TO ADD-FAIL-F3-17-4.                                      NC2024.2
145200 ADD-DELETE-F3-17-4.                                              NC2024.2
145300     PERFORM DE-LETE.                                             NC2024.2
145400     GO TO ADD-WRITE-F3-17-4.                                     NC2024.2
145500 ADD-FAIL-F3-17-4.                                                NC2024.2
145600     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
145700     MOVE 555.5 TO CORRECT-N                                      NC2024.2
145800     PERFORM FAIL.                                                NC2024.2
145900 ADD-WRITE-F3-17-4.                                               NC2024.2
146000     PERFORM PRINT-DETAIL.                                        NC2024.2
146100*                                                                 NC2024.2
146200 ADD-INIT-F3-18.                                                  NC2024.2
146300*    ===-->  NEW SIZE ERROR TESTS  <--===                         NC2024.2
146400*    ===-->   NO SIZE ERROR        <--===                         NC2024.2
146500     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
146600     MOVE    1 TO REC-CT.                                         NC2024.2
146700     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
146800     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
146900     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
147000     MOVE    88888  TO FIELD1 OF ADD-16.                          NC2024.2
147100     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
147200     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
147300 ADD-INIT-F3-18-1.                                                NC2024.2
147400     MOVE   "ADD-TEST-F3-18-1" TO PAR-NAME.                       NC2024.2
147500 ADD-TEST-F3-18-1.                                                NC2024.2
147600     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
147700             ON SIZE ERROR                                        NC2024.2
147800             GO TO ADD-FAIL-F3-18-1                               NC2024.2
147900         NOT ON SIZE ERROR                                        NC2024.2
148000             PERFORM PASS                                         NC2024.2
148100             GO TO ADD-WRITE-F3-18-1.                             NC2024.2
148200 ADD-DELETE-F3-18-1.                                              NC2024.2
148300     PERFORM DE-LETE.                                             NC2024.2
148400     GO TO ADD-WRITE-F3-18-1.                                     NC2024.2
148500 ADD-FAIL-F3-18-1.                                                NC2024.2
148600     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"                NC2024.2
148700         TO RE-MARK                                               NC2024.2
148800     PERFORM FAIL.                                                NC2024.2
148900 ADD-WRITE-F3-18-1.                                               NC2024.2
149000             PERFORM PRINT-DETAIL.                                NC2024.2
149100*                                                                 NC2024.2
149200 ADD-INIT-F3-18-2.                                                NC2024.2
149300     MOVE   "ADD-TEST-F3-18-2" TO PAR-NAME.                       NC2024.2
149400     ADD     1 TO REC-CT.                                         NC2024.2
149500 ADD-TEST-F3-18-2.                                                NC2024.2
149600     IF      FIELD1 OF ADD-16 EQUAL TO 88889                      NC2024.2
149700             PERFORM PASS                                         NC2024.2
149800             GO TO ADD-WRITE-F3-18-2.                             NC2024.2
149900     GO TO ADD-FAIL-F3-18-2.                                      NC2024.2
150000 ADD-DELETE-F3-18-2.                                              NC2024.2
150100     PERFORM DE-LETE                                              NC2024.2
150200     GO TO ADD-WRITE-F3-18-2.                                     NC2024.2
150300 ADD-FAIL-F3-18-2.                                                NC2024.2
150400     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
150500     MOVE    88889 TO CORRECT-N                                   NC2024.2
150600     PERFORM FAIL.                                                NC2024.2
150700 ADD-WRITE-F3-18-2.                                               NC2024.2
150800     PERFORM PRINT-DETAIL.                                        NC2024.2
150900*                                                                 NC2024.2
151000 ADD-INIT-F3-18-3.                                                NC2024.2
151100     MOVE   "ADD-TEST-F3-18-3" TO PAR-NAME.                       NC2024.2
151200     ADD     1 TO REC-CT.                                         NC2024.2
151300 ADD-TEST-F3-18-3.                                                NC2024.2
151400     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
151500             PERFORM PASS                                         NC2024.2
151600             GO TO ADD-WRITE-F3-18-3.                             NC2024.2
151700     GO TO ADD-FAIL-F3-18-3.                                      NC2024.2
151800 ADD-DELETE-F3-18-3.                                              NC2024.2
151900     PERFORM DE-LETE.                                             NC2024.2
152000     GO TO ADD-WRITE-F3-18-3.                                     NC2024.2
152100 ADD-FAIL-F3-18-3.                                                NC2024.2
152200     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
152300     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
152400     PERFORM FAIL.                                                NC2024.2
152500 ADD-WRITE-F3-18-3.                                               NC2024.2
152600     PERFORM PRINT-DETAIL.                                        NC2024.2
152700*                                                                 NC2024.2
152800 ADD-INIT-F3-18-4.                                                NC2024.2
152900     MOVE   "ADD-TEST-F3-18-4" TO PAR-NAME.                       NC2024.2
153000     ADD     1 TO REC-CT.                                         NC2024.2
153100 ADD-TEST-F3-18-4.                                                NC2024.2
153200     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
153300             PERFORM PASS                                         NC2024.2
153400             GO TO ADD-WRITE-F3-18-4.                             NC2024.2
153500     GO TO ADD-FAIL-F3-18-4.                                      NC2024.2
153600 ADD-DELETE-F3-18-4.                                              NC2024.2
153700     PERFORM DE-LETE.                                             NC2024.2
153800     GO TO ADD-WRITE-F3-18-4.                                     NC2024.2
153900 ADD-FAIL-F3-18-4.                                                NC2024.2
154000     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
154100     MOVE 555.5 TO CORRECT-N                                      NC2024.2
154200     PERFORM FAIL.                                                NC2024.2
154300 ADD-WRITE-F3-18-4.                                               NC2024.2
154400     PERFORM PRINT-DETAIL.                                        NC2024.2
154500*                                                                 NC2024.2
154600 ADD-INIT-F3-19.                                                  NC2024.2
154700*    ===-->  EXPLICIT SCOPE TERMINATOR<--===                      NC2024.2
154800*    ===-->        SIZE ERROR         <--===                      NC2024.2
154900     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
155000     MOVE    SPACE  TO WRK-XN-00001.                              NC2024.2
155100     MOVE    SPACE  TO WRK-AN-00001.                              NC2024.2
155200     MOVE    0      TO REC-CT.                                    NC2024.2
155300     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
155400     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
155500     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
155600     MOVE    99999  TO FIELD1 OF ADD-16.                          NC2024.2
155700     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
155800     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
155900     MOVE   "ADD-TEST-F3-19-0" TO PAR-NAME.                       NC2024.2
156000 ADD-TEST-F3-19-0.                                                NC2024.2
156100     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
156200             ON SIZE ERROR                                        NC2024.2
156300             MOVE   "A" TO WRK-AN-00001                           NC2024.2
156400     END-ADD                                                      NC2024.2
156500     MOVE   "B" TO WRK-XN-00001.                                  NC2024.2
156600*                                                                 NC2024.2
156700 ADD-INIT-F3-19-1.                                                NC2024.2
156800     MOVE   "ADD-TEST-F3-19-1" TO PAR-NAME.                       NC2024.2
156900     ADD     1 TO REC-CT.                                         NC2024.2
157000 ADD-TEST-F3-19-1.                                                NC2024.2
157100     IF      FIELD1 OF ADD-16 EQUAL TO 99999                      NC2024.2
157200             PERFORM PASS                                         NC2024.2
157300             GO TO ADD-WRITE-F3-19-1.                             NC2024.2
157400     GO TO ADD-FAIL-F3-19-1.                                      NC2024.2
157500 ADD-DELETE-F3-19-1.                                              NC2024.2
157600     PERFORM DE-LETE.                                             NC2024.2
157700     GO TO ADD-WRITE-F3-19-1.                                     NC2024.2
157800 ADD-FAIL-F3-19-1.                                                NC2024.2
157900     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
158000     MOVE    99999 TO CORRECT-N                                   NC2024.2
158100     PERFORM FAIL.                                                NC2024.2
158200 ADD-WRITE-F3-19-1.                                               NC2024.2
158300     PERFORM PRINT-DETAIL.                                        NC2024.2
158400*                                                                 NC2024.2
158500 ADD-INIT-F3-19-2.                                                NC2024.2
158600     MOVE   "ADD-TEST-F3-19-2" TO PAR-NAME.                       NC2024.2
158700     ADD     1 TO REC-CT.                                         NC2024.2
158800 ADD-TEST-F3-19-2.                                                NC2024.2
158900     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
159000             PERFORM PASS                                         NC2024.2
159100             GO TO ADD-WRITE-F3-19-2.                             NC2024.2
159200     GO TO ADD-FAIL-F3-19-2.                                      NC2024.2
159300 ADD-DELETE-F3-19-2.                                              NC2024.2
159400     PERFORM DE-LETE.                                             NC2024.2
159500     GO TO ADD-WRITE-F3-19-2.                                     NC2024.2
159600 ADD-FAIL-F3-19-2.                                                NC2024.2
159700     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
159800     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
159900     PERFORM FAIL.                                                NC2024.2
160000 ADD-WRITE-F3-19-2.                                               NC2024.2
160100     PERFORM PRINT-DETAIL.                                        NC2024.2
160200*                                                                 NC2024.2
160300 ADD-INIT-F3-19-3.                                                NC2024.2
160400     MOVE   "ADD-TEST-F3-19-3" TO PAR-NAME.                       NC2024.2
160500     ADD     1 TO REC-CT.                                         NC2024.2
160600 ADD-TEST-F3-19-3.                                                NC2024.2
160700     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
160800             PERFORM PASS                                         NC2024.2
160900             GO TO ADD-WRITE-F3-19-3.                             NC2024.2
161000     GO TO ADD-FAIL-F3-19-3.                                      NC2024.2
161100 ADD-DELETE-F3-19-3.                                              NC2024.2
161200     PERFORM DE-LETE.                                             NC2024.2
161300     GO TO ADD-WRITE-F3-19-3.                                     NC2024.2
161400 ADD-FAIL-F3-19-3.                                                NC2024.2
161500     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
161600     MOVE 555.5 TO CORRECT-N                                      NC2024.2
161700     PERFORM FAIL.                                                NC2024.2
161800 ADD-WRITE-F3-19-3.                                               NC2024.2
161900     PERFORM PRINT-DETAIL.                                        NC2024.2
162000*                                                                 NC2024.2
162100 ADD-INIT-F3-19-4.                                                NC2024.2
162200     MOVE   "ADD-TEST-F3-19-4" TO PAR-NAME.                       NC2024.2
162300     ADD     1 TO REC-CT.                                         NC2024.2
162400 ADD-TEST-F3-19-4.                                                NC2024.2
162500     IF      WRK-AN-00001 = SPACE                                 NC2024.2
162600             GO TO ADD-FAIL-F3-19-4.                              NC2024.2
162700     PERFORM PASS                                                 NC2024.2
162800     GO TO ADD-WRITE-F3-19-4.                                     NC2024.2
162900 ADD-DELETE-F3-19-4.                                              NC2024.2
163000     PERFORM DE-LETE.                                             NC2024.2
163100     GO TO ADD-WRITE-F3-19-4.                                     NC2024.2
163200 ADD-FAIL-F3-19-4.                                                NC2024.2
163300     MOVE   "ON SIZE ERROR SHOULD BE EXECUTED"                    NC2024.2
163400          TO RE-MARK                                              NC2024.2
163500     MOVE   "A"   TO COMPUTED-X                                   NC2024.2
163600     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
163700     PERFORM FAIL.                                                NC2024.2
163800 ADD-WRITE-F3-19-4.                                               NC2024.2
163900     PERFORM PRINT-DETAIL.                                        NC2024.2
164000*                                                                 NC2024.2
164100 ADD-INIT-F3-19-5.                                                NC2024.2
164200     MOVE   "ADD-TEST-F3-19-5" TO PAR-NAME.                       NC2024.2
164300     ADD     1 TO REC-CT.                                         NC2024.2
164400 ADD-TEST-F3-19-5.                                                NC2024.2
164500     IF      WRK-XN-00001 = SPACE                                 NC2024.2
164600             GO TO ADD-FAIL-F3-19-5.                              NC2024.2
164700     PERFORM PASS                                                 NC2024.2
164800     GO TO ADD-WRITE-F3-19-5.                                     NC2024.2
164900 ADD-DELETE-F3-19-5.                                              NC2024.2
165000     PERFORM DE-LETE.                                             NC2024.2
165100     GO TO ADD-WRITE-F3-19-5.                                     NC2024.2
165200 ADD-FAIL-F3-19-5.                                                NC2024.2
165300     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2024.2
165400     MOVE   "B"   TO COMPUTED-X                                   NC2024.2
165500     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
165600     PERFORM FAIL.                                                NC2024.2
165700 ADD-WRITE-F3-19-5.                                               NC2024.2
165800     PERFORM PRINT-DETAIL.                                        NC2024.2
165900*                                                                 NC2024.2
166000 ADD-INIT-F3-20.                                                  NC2024.2
166100*    ===-->  EXPLICIT SCOPE TERMINATOR<--===                      NC2024.2
166200*    ===-->     NO SIZE ERROR         <--===                      NC2024.2
166300     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
166400     MOVE   "ADD-TEST-F3-20-0" TO PAR-NAME.                       NC2024.2
166500     MOVE    SPACE  TO WRK-XN-00001.                              NC2024.2
166600     MOVE    SPACE  TO WRK-AN-00001.                              NC2024.2
166700     MOVE    0      TO REC-CT.                                    NC2024.2
166800     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
166900     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
167000     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
167100     MOVE    88888  TO FIELD1 OF ADD-16.                          NC2024.2
167200     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
167300     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
167400 ADD-TEST-F3-20-0.                                                NC2024.2
167500     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
167600             ON SIZE ERROR                                        NC2024.2
167700             MOVE   "A" TO WRK-AN-00001                           NC2024.2
167800     END-ADD                                                      NC2024.2
167900     MOVE   "B" TO WRK-XN-00001.                                  NC2024.2
168000*                                                                 NC2024.2
168100 ADD-INIT-F3-20-1.                                                NC2024.2
168200     MOVE   "ADD-TEST-F3-20-1" TO PAR-NAME.                       NC2024.2
168300     ADD     1 TO REC-CT.                                         NC2024.2
168400 ADD-TEST-F3-20-1.                                                NC2024.2
168500     IF      FIELD1 OF ADD-16 EQUAL TO 88889                      NC2024.2
168600             PERFORM PASS                                         NC2024.2
168700             GO TO ADD-WRITE-F3-20-1.                             NC2024.2
168800     GO TO ADD-FAIL-F3-20-1.                                      NC2024.2
168900 ADD-DELETE-F3-20-1.                                              NC2024.2
169000     PERFORM DE-LETE.                                             NC2024.2
169100     GO TO ADD-WRITE-F3-20-1.                                     NC2024.2
169200 ADD-FAIL-F3-20-1.                                                NC2024.2
169300     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
169400     MOVE    88889 TO CORRECT-N                                   NC2024.2
169500     PERFORM FAIL.                                                NC2024.2
169600 ADD-WRITE-F3-20-1.                                               NC2024.2
169700     PERFORM PRINT-DETAIL.                                        NC2024.2
169800*                                                                 NC2024.2
169900 ADD-INIT-F3-20-2.                                                NC2024.2
170000     MOVE   "ADD-TEST-F3-20-2" TO PAR-NAME.                       NC2024.2
170100     ADD     1 TO REC-CT.                                         NC2024.2
170200 ADD-TEST-F3-20-2.                                                NC2024.2
170300     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
170400             PERFORM PASS                                         NC2024.2
170500             GO TO ADD-WRITE-F3-20-2.                             NC2024.2
170600     GO TO ADD-FAIL-F3-20-2.                                      NC2024.2
170700 ADD-DELETE-F3-20-2.                                              NC2024.2
170800     PERFORM DE-LETE.                                             NC2024.2
170900     GO TO ADD-WRITE-F3-20-2.                                     NC2024.2
171000 ADD-FAIL-F3-20-2.                                                NC2024.2
171100     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
171200     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
171300     PERFORM FAIL.                                                NC2024.2
171400 ADD-WRITE-F3-20-2.                                               NC2024.2
171500     PERFORM PRINT-DETAIL.                                        NC2024.2
171600*                                                                 NC2024.2
171700 ADD-INIT-F3-20-3.                                                NC2024.2
171800     MOVE   "ADD-TEST-F3-20-3" TO PAR-NAME.                       NC2024.2
171900     ADD     1 TO REC-CT.                                         NC2024.2
172000 ADD-TEST-F3-20-3.                                                NC2024.2
172100     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
172200             PERFORM PASS                                         NC2024.2
172300             GO TO ADD-WRITE-F3-20-3.                             NC2024.2
172400     GO TO ADD-FAIL-F3-20-3.                                      NC2024.2
172500 ADD-DELETE-F3-20-3.                                              NC2024.2
172600     PERFORM DE-LETE.                                             NC2024.2
172700     GO TO ADD-WRITE-F3-20-3.                                     NC2024.2
172800 ADD-FAIL-F3-20-3.                                                NC2024.2
172900     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
173000     MOVE 555.5 TO CORRECT-N                                      NC2024.2
173100     PERFORM FAIL.                                                NC2024.2
173200 ADD-WRITE-F3-20-3.                                               NC2024.2
173300     PERFORM PRINT-DETAIL.                                        NC2024.2
173400*                                                                 NC2024.2
173500 ADD-INIT-F3-20-4.                                                NC2024.2
173600     MOVE   "ADD-TEST-F3-20-4" TO PAR-NAME.                       NC2024.2
173700     ADD     1 TO REC-CT.                                         NC2024.2
173800 ADD-TEST-F3-20-4.                                                NC2024.2
173900     IF      WRK-AN-00001 = SPACE                                 NC2024.2
174000             PERFORM PASS                                         NC2024.2
174100             GO TO ADD-WRITE-F3-20-4.                             NC2024.2
174200     GO TO ADD-FAIL-F3-20-4.                                      NC2024.2
174300 ADD-DELETE-F3-20-4.                                              NC2024.2
174400     PERFORM DE-LETE.                                             NC2024.2
174500     GO TO ADD-WRITE-F3-20-4.                                     NC2024.2
174600 ADD-FAIL-F3-20-4.                                                NC2024.2
174700     MOVE   "ON SIZE ERROR SHOULD NOT BE EXECUTED"                NC2024.2
174800          TO RE-MARK.                                             NC2024.2
174900     MOVE    SPACE TO CORRECT-X.                                  NC2024.2
175000     MOVE    WRK-AN-00001  TO COMPUTED-X.                         NC2024.2
175100     PERFORM FAIL.                                                NC2024.2
175200 ADD-WRITE-F3-20-4.                                               NC2024.2
175300     PERFORM PRINT-DETAIL.                                        NC2024.2
175400*                                                                 NC2024.2
175500 ADD-INIT-F3-20-5.                                                NC2024.2
175600     MOVE   "ADD-TEST-F3-20-5" TO PAR-NAME.                       NC2024.2
175700     ADD     1 TO REC-CT.                                         NC2024.2
175800 ADD-TEST-F3-20-5.                                                NC2024.2
175900     IF      WRK-XN-00001 = SPACE                                 NC2024.2
176000             GO TO ADD-FAIL-F3-20-5.                              NC2024.2
176100     PERFORM PASS                                                 NC2024.2
176200     GO TO ADD-WRITE-F3-20-5.                                     NC2024.2
176300 ADD-DELETE-F3-20-5.                                              NC2024.2
176400     PERFORM DE-LETE.                                             NC2024.2
176500     GO TO ADD-WRITE-F3-20-5.                                     NC2024.2
176600 ADD-FAIL-F3-20-5.                                                NC2024.2
176700     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2024.2
176800     MOVE   "B"   TO COMPUTED-X                                   NC2024.2
176900     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
177000     PERFORM FAIL.                                                NC2024.2
177100 ADD-WRITE-F3-20-5.                                               NC2024.2
177200     PERFORM PRINT-DETAIL.                                        NC2024.2
177300*                                                                 NC2024.2
177400 ADD-INIT-F3-21.                                                  NC2024.2
177500*    ===-->  EXPLICIT SCOPE TERMINATOR<--===                      NC2024.2
177600*    ===-->        SIZE ERROR         <--===                      NC2024.2
177700     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
177800     MOVE   "ADD-TEST-F3-21-0" TO PAR-NAME.                       NC2024.2
177900     MOVE    SPACE  TO WRK-XN-00001.                              NC2024.2
178000     MOVE    SPACE  TO WRK-AN-00001.                              NC2024.2
178100     MOVE    1      TO REC-CT.                                    NC2024.2
178200     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
178300     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
178400     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
178500     MOVE    99999  TO FIELD1 OF ADD-16.                          NC2024.2
178600     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
178700     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
178800 ADD-TEST-F3-21-0.                                                NC2024.2
178900     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
179000         NOT ON SIZE ERROR                                        NC2024.2
179100             MOVE   "A" TO WRK-AN-00001                           NC2024.2
179200     END-ADD                                                      NC2024.2
179300     MOVE   "B" TO WRK-XN-00001.                                  NC2024.2
179400*                                                                 NC2024.2
179500 ADD-INIT-F3-21-1.                                                NC2024.2
179600     MOVE   "ADD-TEST-F3-21-1" TO PAR-NAME.                       NC2024.2
179700     ADD     1 TO REC-CT.                                         NC2024.2
179800 ADD-TEST-F3-21-1.                                                NC2024.2
179900     IF      FIELD1 OF ADD-16 EQUAL TO 99999                      NC2024.2
180000             PERFORM PASS                                         NC2024.2
180100             GO TO ADD-WRITE-F3-21-1.                             NC2024.2
180200     GO TO ADD-FAIL-F3-21-1.                                      NC2024.2
180300 ADD-DELETE-F3-21-1.                                              NC2024.2
180400     PERFORM DE-LETE.                                             NC2024.2
180500     GO TO ADD-WRITE-F3-21-1.                                     NC2024.2
180600 ADD-FAIL-F3-21-1.                                                NC2024.2
180700     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
180800     MOVE    99999 TO CORRECT-N                                   NC2024.2
180900     PERFORM FAIL.                                                NC2024.2
181000 ADD-WRITE-F3-21-1.                                               NC2024.2
181100     PERFORM PRINT-DETAIL.                                        NC2024.2
181200*                                                                 NC2024.2
181300 ADD-INIT-F3-21-2.                                                NC2024.2
181400     MOVE   "ADD-TEST-F3-21-2" TO PAR-NAME.                       NC2024.2
181500     ADD     1 TO REC-CT.                                         NC2024.2
181600 ADD-TEST-F3-21-2.                                                NC2024.2
181700     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
181800             PERFORM PASS                                         NC2024.2
181900             GO TO ADD-WRITE-F3-21-2.                             NC2024.2
182000     GO TO ADD-FAIL-F3-21-2.                                      NC2024.2
182100 ADD-DELETE-F3-21-2.                                              NC2024.2
182200     PERFORM DE-LETE.                                             NC2024.2
182300     GO TO ADD-WRITE-F3-21-2.                                     NC2024.2
182400 ADD-FAIL-F3-21-2.                                                NC2024.2
182500     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
182600     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
182700     PERFORM FAIL.                                                NC2024.2
182800 ADD-WRITE-F3-21-2.                                               NC2024.2
182900     PERFORM PRINT-DETAIL.                                        NC2024.2
183000*                                                                 NC2024.2
183100 ADD-INIT-F3-21-3.                                                NC2024.2
183200     MOVE   "ADD-TEST-F3-21-3" TO PAR-NAME.                       NC2024.2
183300     ADD     1 TO REC-CT.                                         NC2024.2
183400 ADD-TEST-F3-21-3.                                                NC2024.2
183500     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
183600             PERFORM PASS                                         NC2024.2
183700             GO TO ADD-WRITE-F3-21-3.                             NC2024.2
183800     GO TO ADD-FAIL-F3-21-3.                                      NC2024.2
183900 ADD-DELETE-F3-21-3.                                              NC2024.2
184000     PERFORM DE-LETE.                                             NC2024.2
184100     GO TO ADD-WRITE-F3-21-3.                                     NC2024.2
184200 ADD-FAIL-F3-21-3.                                                NC2024.2
184300             MOVE FIELD3 OF ADD-16 TO COMPUTED-N                  NC2024.2
184400             MOVE 555.5 TO CORRECT-N                              NC2024.2
184500             PERFORM FAIL.                                        NC2024.2
184600 ADD-WRITE-F3-21-3.                                               NC2024.2
184700             PERFORM PRINT-DETAIL.                                NC2024.2
184800*                                                                 NC2024.2
184900 ADD-INIT-F3-21-4.                                                NC2024.2
185000     MOVE   "ADD-TEST-F3-21-4" TO PAR-NAME.                       NC2024.2
185100     ADD     1 TO REC-CT.                                         NC2024.2
185200 ADD-TEST-F3-21-4.                                                NC2024.2
185300     IF      WRK-AN-00001 = "A"                                   NC2024.2
185400             GO TO ADD-FAIL-F3-21-4.                              NC2024.2
185500     PERFORM PASS                                                 NC2024.2
185600     GO TO ADD-WRITE-F3-21-4.                                     NC2024.2
185700 ADD-DELETE-F3-21-4.                                              NC2024.2
185800     PERFORM DE-LETE.                                             NC2024.2
185900     GO TO ADD-WRITE-F3-21-4.                                     NC2024.2
186000 ADD-FAIL-F3-21-4.                                                NC2024.2
186100     MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"            NC2024.2
186200          TO RE-MARK                                              NC2024.2
186300     MOVE    SPACE TO COMPUTED-X                                  NC2024.2
186400     MOVE    WRK-AN-00001  TO CORRECT-X                           NC2024.2
186500     PERFORM FAIL.                                                NC2024.2
186600 ADD-WRITE-F3-21-4.                                               NC2024.2
186700     PERFORM PRINT-DETAIL.                                        NC2024.2
186800*                                                                 NC2024.2
186900 ADD-INIT-F3-21-5.                                                NC2024.2
187000     MOVE   "ADD-TEST-F3-21-5" TO PAR-NAME.                       NC2024.2
187100     ADD     1 TO REC-CT.                                         NC2024.2
187200 ADD-TEST-F3-21-5.                                                NC2024.2
187300     IF      WRK-XN-00001 = SPACE                                 NC2024.2
187400             GO TO ADD-FAIL-F3-21-5.                              NC2024.2
187500     PERFORM PASS                                                 NC2024.2
187600     GO TO ADD-WRITE-F3-21-5.                                     NC2024.2
187700 ADD-DELETE-F3-21-5.                                              NC2024.2
187800     PERFORM DE-LETE.                                             NC2024.2
187900     GO TO ADD-WRITE-F3-21-5.                                     NC2024.2
188000 ADD-FAIL-F3-21-5.                                                NC2024.2
188100     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2024.2
188200     MOVE   "B"   TO COMPUTED-X                                   NC2024.2
188300     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
188400     PERFORM FAIL.                                                NC2024.2
188500 ADD-WRITE-F3-21-5.                                               NC2024.2
188600     PERFORM PRINT-DETAIL.                                        NC2024.2
188700*                                                                 NC2024.2
188800 ADD-INIT-F3-22.                                                  NC2024.2
188900*    ===-->  EXPLICIT SCOPE TERMINATOR<--===                      NC2024.2
189000*    ===-->     NO SIZE ERROR         <--===                      NC2024.2
189100     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
189200     MOVE   "ADD-TEST-F3-22-0" TO PAR-NAME.                       NC2024.2
189300     MOVE    SPACE  TO WRK-XN-00001.                              NC2024.2
189400     MOVE    SPACE  TO WRK-AN-00001.                              NC2024.2
189500     MOVE    0      TO REC-CT.                                    NC2024.2
189600     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
189700     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
189800     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
189900     MOVE    88888  TO FIELD1 OF ADD-16.                          NC2024.2
190000     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
190100     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
190200 ADD-TEST-F3-22-0.                                                NC2024.2
190300     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
190400         NOT ON SIZE ERROR                                        NC2024.2
190500             MOVE   "A" TO WRK-AN-00001                           NC2024.2
190600             PERFORM PASS                                         NC2024.2
190700             PERFORM PRINT-DETAIL                                 NC2024.2
190800     END-ADD                                                      NC2024.2
190900     MOVE   "B" TO WRK-XN-00001.                                  NC2024.2
191000*                                                                 NC2024.2
191100 ADD-INIT-F3-22-1.                                                NC2024.2
191200     MOVE   "ADD-TEST-F3-22-1" TO PAR-NAME.                       NC2024.2
191300     ADD     1 TO REC-CT.                                         NC2024.2
191400 ADD-TEST-F3-22-1.                                                NC2024.2
191500     IF      FIELD1 OF ADD-16 EQUAL TO 88889                      NC2024.2
191600             PERFORM PASS                                         NC2024.2
191700             GO TO ADD-WRITE-F3-22-1.                             NC2024.2
191800     GO TO ADD-FAIL-F3-22-1.                                      NC2024.2
191900 ADD-DELETE-F3-22-1.                                              NC2024.2
192000     PERFORM DE-LETE.                                             NC2024.2
192100     GO TO ADD-WRITE-F3-22-1.                                     NC2024.2
192200 ADD-FAIL-F3-22-1.                                                NC2024.2
192300     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
192400     MOVE    88889 TO CORRECT-N                                   NC2024.2
192500     PERFORM FAIL.                                                NC2024.2
192600 ADD-WRITE-F3-22-1.                                               NC2024.2
192700     PERFORM PRINT-DETAIL.                                        NC2024.2
192800*                                                                 NC2024.2
192900 ADD-INIT-F3-22-2.                                                NC2024.2
193000     MOVE   "ADD-TEST-F3-22-2" TO PAR-NAME.                       NC2024.2
193100     ADD     1 TO REC-CT.                                         NC2024.2
193200 ADD-TEST-F3-22-2.                                                NC2024.2
193300     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
193400             PERFORM PASS                                         NC2024.2
193500             GO TO ADD-WRITE-F3-22-2.                             NC2024.2
193600     GO TO ADD-FAIL-F3-22-2.                                      NC2024.2
193700 ADD-DELETE-F3-22-2.                                              NC2024.2
193800     PERFORM DE-LETE.                                             NC2024.2
193900     GO TO ADD-WRITE-F3-22-2.                                     NC2024.2
194000 ADD-FAIL-F3-22-2.                                                NC2024.2
194100     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
194200     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
194300     PERFORM FAIL.                                                NC2024.2
194400 ADD-WRITE-F3-22-2.                                               NC2024.2
194500     PERFORM PRINT-DETAIL.                                        NC2024.2
194600*                                                                 NC2024.2
194700 ADD-INIT-F3-22-3.                                                NC2024.2
194800     MOVE   "ADD-TEST-F3-22-3" TO PAR-NAME.                       NC2024.2
194900     ADD     1 TO REC-CT.                                         NC2024.2
195000 ADD-TEST-F3-22-3.                                                NC2024.2
195100     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
195200             PERFORM PASS                                         NC2024.2
195300             GO TO ADD-WRITE-F3-22-3.                             NC2024.2
195400     GO TO ADD-FAIL-F3-22-3.                                      NC2024.2
195500 ADD-DELETE-F3-22-3.                                              NC2024.2
195600     PERFORM DE-LETE.                                             NC2024.2
195700     GO TO ADD-WRITE-F3-22-3.                                     NC2024.2
195800 ADD-FAIL-F3-22-3.                                                NC2024.2
195900     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
196000     MOVE 555.5 TO CORRECT-N                                      NC2024.2
196100     PERFORM FAIL.                                                NC2024.2
196200 ADD-WRITE-F3-22-3.                                               NC2024.2
196300     PERFORM PRINT-DETAIL.                                        NC2024.2
196400*                                                                 NC2024.2
196500 ADD-INIT-F3-22-4.                                                NC2024.2
196600     MOVE   "ADD-TEST-F3-22-4" TO PAR-NAME.                       NC2024.2
196700     ADD     1 TO REC-CT.                                         NC2024.2
196800 ADD-TEST-F3-22-4.                                                NC2024.2
196900     IF      WRK-XN-00001 = SPACE                                 NC2024.2
197000             GO TO ADD-FAIL-F3-22-4.                              NC2024.2
197100     PERFORM PASS                                                 NC2024.2
197200     GO TO ADD-WRITE-F3-22-4.                                     NC2024.2
197300 ADD-DELETE-F3-22-4.                                              NC2024.2
197400     PERFORM DE-LETE.                                             NC2024.2
197500     GO TO ADD-WRITE-F3-22-4.                                     NC2024.2
197600 ADD-FAIL-F3-22-4.                                                NC2024.2
197700     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2024.2
197800     MOVE   "B"   TO COMPUTED-X                                   NC2024.2
197900     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
198000     PERFORM FAIL.                                                NC2024.2
198100 ADD-WRITE-F3-22-4.                                               NC2024.2
198200     PERFORM PRINT-DETAIL.                                        NC2024.2
198300*                                                                 NC2024.2
198400 ADD-INIT-F3-23.                                                  NC2024.2
198500*    ===-->  EXPLICIT SCOPE TERMINATOR<--===                      NC2024.2
198600*    ===-->        SIZE ERROR         <--===                      NC2024.2
198700     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
198800     MOVE   "ADD-TEST-F3-23-0" TO PAR-NAME.                       NC2024.2
198900     MOVE    SPACE  TO WRK-XN-00001.                              NC2024.2
199000     MOVE    SPACE  TO WRK-AN-00001.                              NC2024.2
199100     MOVE    0      TO REC-CT.                                    NC2024.2
199200     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
199300     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
199400     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
199500     MOVE    99999  TO FIELD1 OF ADD-16.                          NC2024.2
199600     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
199700     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
199800 ADD-TEST-F3-23-0.                                                NC2024.2
199900     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
200000             ON SIZE ERROR                                        NC2024.2
200100             MOVE   "A" TO WRK-AN-00001                           NC2024.2
200200         NOT ON SIZE ERROR                                        NC2024.2
200300             MOVE   "B" TO WRK-AN-00001                           NC2024.2
200400     END-ADD                                                      NC2024.2
200500     MOVE   "B" TO WRK-XN-00001.                                  NC2024.2
200600     GO TO   ADD-TEST-F3-23-1.                                    NC2024.2
200700*                                                                 NC2024.2
200800 ADD-INIT-F3-23-1.                                                NC2024.2
200900     MOVE   "ADD-TEST-F3-23-1" TO PAR-NAME.                       NC2024.2
201000     ADD     1 TO REC-CT.                                         NC2024.2
201100 ADD-TEST-F3-23-1.                                                NC2024.2
201200     IF      FIELD1 OF ADD-16 EQUAL TO 99999                      NC2024.2
201300             PERFORM PASS                                         NC2024.2
201400             GO TO ADD-WRITE-F3-23-1.                             NC2024.2
201500     GO TO ADD-FAIL-F3-23-1.                                      NC2024.2
201600 ADD-DELETE-F3-23-1.                                              NC2024.2
201700     PERFORM DE-LETE                                              NC2024.2
201800     GO TO ADD-WRITE-F3-23-1.                                     NC2024.2
201900 ADD-FAIL-F3-23-1.                                                NC2024.2
202000     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
202100     MOVE    99999 TO CORRECT-N                                   NC2024.2
202200     PERFORM FAIL.                                                NC2024.2
202300 ADD-WRITE-F3-23-1.                                               NC2024.2
202400     PERFORM PRINT-DETAIL.                                        NC2024.2
202500*                                                                 NC2024.2
202600 ADD-INIT-F3-23-2.                                                NC2024.2
202700     MOVE   "ADD-TEST-F3-23-2" TO PAR-NAME.                       NC2024.2
202800     ADD     1 TO REC-CT.                                         NC2024.2
202900 ADD-TEST-F3-23-2.                                                NC2024.2
203000     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
203100             PERFORM PASS                                         NC2024.2
203200             GO TO ADD-WRITE-F3-23-2.                             NC2024.2
203300     GO TO ADD-FAIL-F3-23-2.                                      NC2024.2
203400 ADD-DELETE-F3-23-2.                                              NC2024.2
203500     PERFORM DE-LETE.                                             NC2024.2
203600     GO TO ADD-WRITE-F3-23-2.                                     NC2024.2
203700 ADD-FAIL-F3-23-2.                                                NC2024.2
203800     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
203900     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
204000     PERFORM FAIL.                                                NC2024.2
204100 ADD-WRITE-F3-23-2.                                               NC2024.2
204200     PERFORM PRINT-DETAIL.                                        NC2024.2
204300*                                                                 NC2024.2
204400 ADD-INIT-F3-23-3.                                                NC2024.2
204500     MOVE   "ADD-TEST-F3-23-3" TO PAR-NAME.                       NC2024.2
204600     ADD     1 TO REC-CT.                                         NC2024.2
204700 ADD-TEST-F3-23-3.                                                NC2024.2
204800     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
204900             PERFORM PASS                                         NC2024.2
205000             GO TO ADD-WRITE-F3-23-3.                             NC2024.2
205100     GO TO ADD-FAIL-F3-23-3.                                      NC2024.2
205200 ADD-DELETE-F3-23-3.                                              NC2024.2
205300     PERFORM DE-LETE.                                             NC2024.2
205400     GO TO ADD-WRITE-F3-23-3.                                     NC2024.2
205500 ADD-FAIL-F3-23-3.                                                NC2024.2
205600     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
205700     MOVE 555.5 TO CORRECT-N                                      NC2024.2
205800     PERFORM FAIL.                                                NC2024.2
205900 ADD-WRITE-F3-23-3.                                               NC2024.2
206000     PERFORM PRINT-DETAIL.                                        NC2024.2
206100*                                                                 NC2024.2
206200 ADD-INIT-F3-23-4.                                                NC2024.2
206300     MOVE   "ADD-TEST-F3-23-4" TO PAR-NAME.                       NC2024.2
206400     ADD     1 TO REC-CT.                                         NC2024.2
206500 ADD-TEST-F3-23-4.                                                NC2024.2
206600     IF      WRK-AN-00001 = "B"                                   NC2024.2
206700             GO TO ADD-FAIL-F3-23-4.                              NC2024.2
206800     PERFORM PASS                                                 NC2024.2
206900     GO TO ADD-WRITE-F3-23-4.                                     NC2024.2
207000 ADD-DELETE-F3-23-4.                                              NC2024.2
207100     PERFORM DE-LETE.                                             NC2024.2
207200     GO TO ADD-WRITE-F3-23-4.                                     NC2024.2
207300 ADD-FAIL-F3-23-4.                                                NC2024.2
207400     MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"            NC2024.2
207500          TO RE-MARK                                              NC2024.2
207600     MOVE   "B"    TO COMPUTED-X                                  NC2024.2
207700     MOVE    WRK-AN-00001  TO CORRECT-X                           NC2024.2
207800     PERFORM FAIL.                                                NC2024.2
207900 ADD-WRITE-F3-23-4.                                               NC2024.2
208000     PERFORM PRINT-DETAIL.                                        NC2024.2
208100*                                                                 NC2024.2
208200 ADD-INIT-F3-23-5.                                                NC2024.2
208300     MOVE   "ADD-TEST-F3-23-5" TO PAR-NAME.                       NC2024.2
208400     ADD     1 TO REC-CT.                                         NC2024.2
208500 ADD-TEST-F3-23-5.                                                NC2024.2
208600     IF      WRK-XN-00001 = SPACE                                 NC2024.2
208700             GO TO ADD-FAIL-F3-23-5.                              NC2024.2
208800     PERFORM PASS                                                 NC2024.2
208900     GO TO ADD-WRITE-F3-23-5.                                     NC2024.2
209000 ADD-DELETE-F3-23-5.                                              NC2024.2
209100     PERFORM DE-LETE.                                             NC2024.2
209200     GO TO ADD-WRITE-F3-23-5.                                     NC2024.2
209300 ADD-FAIL-F3-23-5.                                                NC2024.2
209400     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2024.2
209500     MOVE   "B"   TO COMPUTED-X                                   NC2024.2
209600     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
209700     PERFORM FAIL.                                                NC2024.2
209800 ADD-WRITE-F3-23-5.                                               NC2024.2
209900     PERFORM PRINT-DETAIL.                                        NC2024.2
210000*                                                                 NC2024.2
210100 ADD-INIT-F3-24.                                                  NC2024.2
210200*    ===-->  EXPLICIT SCOPE TERMINATOR<--===                      NC2024.2
210300*    ===-->     NO SIZE ERROR         <--===                      NC2024.2
210400     MOVE   "VI-73 6.6" TO ANSI-REFERENCE.                        NC2024.2
210500     MOVE   "ADD-TEST-F3-24-0" TO PAR-NAME.                       NC2024.2
210600     MOVE    SPACE  TO WRK-XN-00001.                              NC2024.2
210700     MOVE    SPACE  TO WRK-AN-00001.                              NC2024.2
210800     MOVE    0      TO REC-CT.                                    NC2024.2
210900     MOVE    1      TO FIELD1 OF ADD-15.                          NC2024.2
211000     MOVE    32.1   TO FIELD2 OF ADD-15.                          NC2024.2
211100     MOVE    123.4  TO FIELD3 OF ADD-15.                          NC2024.2
211200     MOVE    88888  TO FIELD1 OF ADD-16.                          NC2024.2
211300     MOVE    745.67 TO FIELD2 OF ADD-16.                          NC2024.2
211400     MOVE    432.1  TO FIELD3 OF ADD-16.                          NC2024.2
211500 ADD-TEST-F3-24-0.                                                NC2024.2
211600     ADD     CORRESPONDING ADD-15 TO ADD-16                       NC2024.2
211700             ON SIZE ERROR                                        NC2024.2
211800             MOVE   "A" TO WRK-AN-00001                           NC2024.2
211900         NOT ON SIZE ERROR                                        NC2024.2
212000             MOVE   "B" TO WRK-AN-00001                           NC2024.2
212100     END-ADD                                                      NC2024.2
212200     MOVE   "B" TO WRK-XN-00001.                                  NC2024.2
212300*                                                                 NC2024.2
212400 ADD-INIT-F3-24-1.                                                NC2024.2
212500     MOVE   "ADD-TEST-F3-24-1" TO PAR-NAME.                       NC2024.2
212600     ADD     1 TO REC-CT.                                         NC2024.2
212700 ADD-TEST-F3-24-1.                                                NC2024.2
212800     IF      FIELD1 OF ADD-16 EQUAL TO 88889                      NC2024.2
212900             PERFORM PASS                                         NC2024.2
213000             GO TO ADD-WRITE-F3-24-1.                             NC2024.2
213100     GO TO ADD-FAIL-F3-24-1.                                      NC2024.2
213200 ADD-DELETE-F3-24-1.                                              NC2024.2
213300     PERFORM DE-LETE.                                             NC2024.2
213400     GO TO ADD-WRITE-F3-24-1.                                     NC2024.2
213500 ADD-FAIL-F3-24-1.                                                NC2024.2
213600     MOVE    FIELD1 OF ADD-16 TO COMPUTED-N                       NC2024.2
213700     MOVE    88889 TO CORRECT-N                                   NC2024.2
213800     PERFORM FAIL.                                                NC2024.2
213900 ADD-WRITE-F3-24-1.                                               NC2024.2
214000     PERFORM PRINT-DETAIL.                                        NC2024.2
214100*                                                                 NC2024.2
214200 ADD-INIT-F3-24-2.                                                NC2024.2
214300     MOVE   "ADD-TEST-F3-24-2" TO PAR-NAME.                       NC2024.2
214400     ADD     1 TO REC-CT.                                         NC2024.2
214500 ADD-TEST-F3-24-2.                                                NC2024.2
214600     IF      FIELD2 OF ADD-16 IS EQUAL TO 777.77                  NC2024.2
214700             PERFORM PASS                                         NC2024.2
214800             GO TO ADD-WRITE-F3-24-2.                             NC2024.2
214900     GO TO ADD-FAIL-F3-24-2.                                      NC2024.2
215000 ADD-DELETE-F3-24-2.                                              NC2024.2
215100     PERFORM DE-LETE.                                             NC2024.2
215200     GO TO ADD-WRITE-F3-24-2.                                     NC2024.2
215300 ADD-FAIL-F3-24-2.                                                NC2024.2
215400     MOVE    FIELD2 OF ADD-16 TO COMPUTED-N                       NC2024.2
215500     MOVE   "+777.77" TO CORRECT-A                                NC2024.2
215600     PERFORM FAIL.                                                NC2024.2
215700 ADD-WRITE-F3-24-2.                                               NC2024.2
215800     PERFORM PRINT-DETAIL.                                        NC2024.2
215900*                                                                 NC2024.2
216000 ADD-INIT-F3-24-3.                                                NC2024.2
216100     MOVE   "ADD-TEST-F3-24-3" TO PAR-NAME.                       NC2024.2
216200     ADD     1 TO REC-CT.                                         NC2024.2
216300 ADD-TEST-F3-24-3.                                                NC2024.2
216400     IF      FIELD3 OF ADD-16 IS EQUAL TO 555.5                   NC2024.2
216500             PERFORM PASS                                         NC2024.2
216600             GO TO ADD-WRITE-F3-24-3.                             NC2024.2
216700     GO TO ADD-FAIL-F3-24-3.                                      NC2024.2
216800 ADD-DELETE-F3-24-3.                                              NC2024.2
216900     PERFORM DE-LETE.                                             NC2024.2
217000     GO TO ADD-WRITE-F3-24-3.                                     NC2024.2
217100 ADD-FAIL-F3-24-3.                                                NC2024.2
217200     MOVE FIELD3 OF ADD-16 TO COMPUTED-N                          NC2024.2
217300     MOVE 555.5 TO CORRECT-N                                      NC2024.2
217400     PERFORM FAIL.                                                NC2024.2
217500 ADD-WRITE-F3-24-3.                                               NC2024.2
217600     PERFORM PRINT-DETAIL.                                        NC2024.2
217700*                                                                 NC2024.2
217800 ADD-INIT-F3-24-4.                                                NC2024.2
217900     MOVE   "ADD-TEST-F3-24-4" TO PAR-NAME.                       NC2024.2
218000     ADD     1 TO REC-CT.                                         NC2024.2
218100 ADD-TEST-F3-24-4.                                                NC2024.2
218200     IF      WRK-AN-00001 = "B"                                   NC2024.2
218300             PERFORM PASS                                         NC2024.2
218400             GO TO ADD-WRITE-F3-24-4.                             NC2024.2
218500     GO TO ADD-FAIL-F3-24-4.                                      NC2024.2
218600 ADD-DELETE-F3-24-4.                                              NC2024.2
218700     PERFORM DE-LETE.                                             NC2024.2
218800     GO TO ADD-WRITE-F3-24-4.                                     NC2024.2
218900 ADD-FAIL-F3-24-4.                                                NC2024.2
219000     MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"                NC2024.2
219100          TO RE-MARK                                              NC2024.2
219200     MOVE   "B"    TO COMPUTED-X                                  NC2024.2
219300     MOVE    WRK-AN-00001  TO CORRECT-X                           NC2024.2
219400     PERFORM FAIL.                                                NC2024.2
219500 ADD-WRITE-F3-24-4.                                               NC2024.2
219600     PERFORM PRINT-DETAIL.                                        NC2024.2
219700*                                                                 NC2024.2
219800 ADD-INIT-F3-24-5.                                                NC2024.2
219900     MOVE   "ADD-TEST-F3-24-5" TO PAR-NAME.                       NC2024.2
220000     ADD     1 TO REC-CT.                                         NC2024.2
220100 ADD-TEST-F3-24-5.                                                NC2024.2
220200     IF      WRK-XN-00001 = SPACE                                 NC2024.2
220300             GO TO ADD-FAIL-F3-24-5.                              NC2024.2
220400     PERFORM PASS                                                 NC2024.2
220500     GO TO ADD-WRITE-F3-24-5.                                     NC2024.2
220600 ADD-DELETE-F3-24-5.                                              NC2024.2
220700     PERFORM DE-LETE.                                             NC2024.2
220800     GO TO ADD-WRITE-F3-24-5.                                     NC2024.2
220900 ADD-FAIL-F3-24-5.                                                NC2024.2
221000     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2024.2
221100     MOVE   "B"   TO COMPUTED-X                                   NC2024.2
221200     MOVE    WRK-AN-00001 TO CORRECT-X                            NC2024.2
221300     PERFORM FAIL.                                                NC2024.2
221400 ADD-WRITE-F3-24-5.                                               NC2024.2
221500     PERFORM PRINT-DETAIL.                                        NC2024.2
221600*                                                                 NC2024.2
221700 CCVS-EXIT SECTION.                                               NC2024.2
221800 CCVS-999999.                                                     NC2024.2
221900     GO TO CLOSE-FILES.                                           NC2024.2
      *END-OF,NC202A                                                                  
