      *HEADER,COBOL,NC208A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2084.2
000200 PROGRAM-ID.                                                      NC2084.2
000300     NC208A.                                                      NC2084.2
000400****************************************************************  NC2084.2
000500*                                                              *  NC2084.2
000600*    VALIDATION FOR:-                                          *  NC2084.2
000700*                                                              *  NC2084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2084.2
000900*                                                              *  NC2084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2084.2
001100*                                                              *  NC2084.2
001200****************************************************************  NC2084.2
001300*                                                              *  NC2084.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2084.2
001500*                                                              *  NC2084.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2084.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2084.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2084.2
001900*                                                              *  NC2084.2
002000****************************************************************  NC2084.2
002100                                                                  NC2084.2
002200*                                                              *  NC2084.2
002300*    PROGRAM NC208A TESTS FORMATS 1 AND 2 OF QUALIFICATION*       NC2084.2
002400*    USING FORMATS 1 AND 2 OF THE "MOVE" STATEMENT, FORMAT 1 OF*  NC2084.2
002500*    THE "ADD" STATEMENT AND THE FORMAT 2 "MULTIPLY" STATEMENT.*  NC2084.2
002600*                                                              *  NC2084.2
002700****************************************************************  NC2084.2
002800 ENVIRONMENT DIVISION.                                            NC2084.2
002900 CONFIGURATION SECTION.                                           NC2084.2
003000 SOURCE-COMPUTER.                                                 NC2084.2
003100     XXXXX082.                                                    NC2084.2
003200 OBJECT-COMPUTER.                                                 NC2084.2
003300     XXXXX083.                                                    NC2084.2
003400 INPUT-OUTPUT SECTION.                                            NC2084.2
003500 FILE-CONTROL.                                                    NC2084.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2084.2
003700     XXXXX055.                                                    NC2084.2
003800 DATA DIVISION.                                                   NC2084.2
003900 FILE SECTION.                                                    NC2084.2
004000 FD  PRINT-FILE.                                                  NC2084.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2084.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2084.2
004300 WORKING-STORAGE SECTION.                                         NC2084.2
004400 77  QT1      PICTURE XXXX VALUE SPACE.                           NC2084.2
004500 77  QT2      PICTURE XXXX VALUE SPACE.                           NC2084.2
004600 77  QT3      PICTURE XXXX VALUE SPACE.                           NC2084.2
004700 77  QT4      PICTURE XXXX VALUE SPACE.                           NC2084.2
004800 77  QT5      PICTURE XXXX VALUE SPACE.                           NC2084.2
004900 77  WRK-XN-00001                 PICTURE X.                      NC2084.2
005000 77  WRK-DS-01V00                PICTURE S9.                      NC2084.2
005100 77  WRK-DS-02V00                PICTURE S99.                     NC2084.2
005200 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC2084.2
005300 77  WRK-DS-05V00                PICTURE S9(5).                   NC2084.2
005400 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC2084.2
005500 77  WRK-DS-09V09                PICTURE S9(9)V9(9).              NC2084.2
005600 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC2084.2
005700 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC2084.2
005800                                 VALUE 111111111.111111111.       NC2084.2
005900 77  WRK-DS-18V00       PICTURE S9(18) VALUE 111111111111111111.  NC2084.2
006000 77  A05ONES-DS-05V00            PICTURE S9(5) VALUE 11111.       NC2084.2
006100 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC2084.2
006200 77  WRK-DS-03V00                PICTURE S999.                    NC2084.2
006300 77  WRK-DS-06V00                PICTURE S9(6).                   NC2084.2
006400 77  WRK-DS-0201P                PICTURE S99P.                    NC2084.2
006500 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC2084.2
006600 77  XRAY                    PICTURE IS X.                        NC2084.2
006700 77  W-1                     PICTURE IS 9.                        NC2084.2
006800 77  W-2                     PICTURE IS 99.                       NC2084.2
006900 77  W-3                     PICTURE IS 999.                      NC2084.2
007000 77  W-4                PICTURE 9  VALUE 0.                       NC2084.2
007100 77  W-6                     PICTURE IS 999    VALUE IS ZERO.     NC2084.2
007200 77  W-9                     PICTURE 999.                         NC2084.2
007300 77  D-5                PICTURE S999  VALUE -1.                   NC2084.2
007400 77  D-9                PICTURE 9(4)V9(4)  VALUE 111.1189.        NC2084.2
007500 77  ONE                PICTURE 9  VALUE 1.                       NC2084.2
007600 77  TWO                PICTURE S9  VALUE 2.                      NC2084.2
007700 77  THREE              PICTURE S9  VALUE 3.                      NC2084.2
007800 77  FOUR               PICTURE S9  VALUE 4.                      NC2084.2
007900 77  FIVE               PICTURE S9  VALUE 5.                      NC2084.2
008000 77  SIX                PICTURE S9  VALUE 6.                      NC2084.2
008100 77  SEVEN              PICTURE S9  VALUE 7.                      NC2084.2
008200 77  EIGHT              PICTURE 9  VALUE 8.                       NC2084.2
008300 77  NINE               PICTURE S9  VALUE 9.                      NC2084.2
008400 77  TEN                PICTURE S99  VALUE 10.                    NC2084.2
008500 77  FIFTEEN            PICTURE S99  VALUE 15.                    NC2084.2
008600 77  TWENTY             PICTURE S99  VALUE 20.                    NC2084.2
008700 77  TWENTY-5           PICTURE S99  VALUE 25.                    NC2084.2
008800 01  MOVE54.                                                      NC2084.2
008900     02 MOVE55 PICTURE X VALUE "W".                               NC2084.2
009000     02 MOVE56 PICTURE X VALUE "X".                               NC2084.2
009100     02 MOVE57.                                                   NC2084.2
009200         03 MOVE58 PICTURE X VALUE "Y".                           NC2084.2
009300         03 MOVE59 PICTURE X VALUE "Z".                           NC2084.2
009400 01  MOVE60.                                                      NC2084.2
009500     02 MOVE56 PICTURE X.                                         NC2084.2
009600     02 MOVE57.                                                   NC2084.2
009700         03 MOVE58 PICTURE X.                                     NC2084.2
009800         03 MOVE64.                                               NC2084.2
009900             04 MOVE65 PICTURE X VALUE "A".                       NC2084.2
010000 01  SEND-BREAKDOWN.                                              NC2084.2
010100     02  SEND-20  PIC X(20).                                      NC2084.2
010200     02  SEND-40  PIC X(20).                                      NC2084.2
010300     02  SEND-60  PIC X(20).                                      NC2084.2
010400 01  RECEIVE-BREAKDOWN.                                           NC2084.2
010500     02  RECEIVE-20  PIC X(20).                                   NC2084.2
010600     02  RECEIVE-40  PIC X(20).                                   NC2084.2
010700     02  RECEIVE-60  PIC X(20).                                   NC2084.2
010800 01  GRP-FOR-QUAL-FROM.                                           NC2084.2
010900     02  QUAL-TEST-SUB-GRP-1.                                     NC2084.2
011000     03  QUAL-TEST-1             PICTURE X(26) VALUE              NC2084.2
011100     "ABCDEFGHIJKLMNOPQRSTUVWXYZ".                                NC2084.2
011200     03  QUAL-TEST-1-FROM REDEFINES QUAL-TEST-1 PICTURE X(26).    NC2084.2
011300     03  QUAL-TEST-2             PICTURE S9(10) VALUE +9999999999.NC2084.2
011400     03  QUAL-TEST-2-FROM REDEFINES QUAL-TEST-2 PICTURE S9(10).   NC2084.2
011500     03  QUAL-TEST-3             PICTURE S999  VALUE 2.           NC2084.2
011600     03  QUAL-TEST-3-FROM REDEFINES QUAL-TEST-3 PICTURE S999.     NC2084.2
011700     02  QUAL-TEST-SUB-GRP-2.                                     NC2084.2
011800     03  QUAL-TEST-4             PICTURE X OCCURS 5 TIMES.        NC2084.2
011900     03  QUAL-TEST-4-FROM        PICTURE X OCCURS 4 TIMES.        NC2084.2
012000 01  GRP-FOR-QUAL-TO.                                             NC2084.2
012100     02  DUMMY-LEVELZ.                                            NC2084.2
012200     03  QUAL-TEST-1             PICTURE X(26).                   NC2084.2
012300     03  QUAL-TEST-1-TO REDEFINES QUAL-TEST-1 PICTURE X(26).      NC2084.2
012400     03  QUAL-TEST-2             PICTURE S9(10).                  NC2084.2
012500     03  QUAL-TEST-2-TO REDEFINES QUAL-TEST-2 PICTURE S9(10).     NC2084.2
012600     03  QUAL-TEST-3             PICTURE S999.                    NC2084.2
012700     03  QUAL-TEST-3-TO REDEFINES QUAL-TEST-3 PICTURE S999.       NC2084.2
012800     02  QUAL-TEST1.                                              NC2084.2
012900     03  QUAL-TEST-4             PICTURE X OCCURS 5 TIMES.        NC2084.2
013000     02  QUAL-TEST2.                                              NC2084.2
013100     03  QUAL-TEST-4-TO          PICTURE X OCCURS 4 TIMES.        NC2084.2
013200 01  GRP-MOVE-CORR-1.                                             NC2084.2
013300     09  MOVE-CORR-5              PICTURE 999    VALUE 555.       NC2084.2
013400     09  MOVE-CORR-3              PICTURE 999    VALUE 333.       NC2084.2
013500     09  MOVE-CORR-2              PICTURE 999    VALUE 222.       NC2084.2
013600     09  MOVE-CORR-1              PICTURE 999    VALUE 111.       NC2084.2
013700     09  FILLER                   PICTURE XXX    VALUE ZEROS.     NC2084.2
013800     09  MOVE-CORR-4              PICTURE XXX    VALUE "XYZ".     NC2084.2
013900     09  MOVE-CORR-6              PICTURE XXX    VALUE ALL "6".   NC2084.2
014000     09  MOVE-CORR-7              PICTURE 999    VALUE 777.       NC2084.2
014100 01  GRP-MOVE-CORR-R.                                             NC2084.2
014200     05  FILLER                   PICTURE XXX.                    NC2084.2
014300     05  MOVE-CORR-1              PICTURE XXX.                    NC2084.2
014400     05  MOVE-CORR-2              PICTURE 999.                    NC2084.2
014500     05  MOVE-CORR-3              PICTURE ZZZ.                    NC2084.2
014600     05  MOVE-CORR-4.                                             NC2084.2
014700         06 FILLER                PICTURE 999.                    NC2084.2
014800         06 FILLER                PICTURE XXX.                    NC2084.2
014900 01  GRP-TO-MOVE-CORR.                                            NC2084.2
015000     03  GRP-TO-MOVE-CORR-1.                                      NC2084.2
015100         05  MOVE-CORR-G1.                                        NC2084.2
015200             06  MOVE-CORR-G2.                                    NC2084.2
015300                 09  MOVE-CORR-E1 PICTURE 999    VALUE 111.       NC2084.2
015400                 09  MOVE-CORR-E2 PICTURE 999    VALUE 222.       NC2084.2
015500                 09  FILLER       PICTURE 999    VALUE 333.       NC2084.2
015600             06  MOVE-CORR-G3.                                    NC2084.2
015700                 07  MOVE-CORR-E3 PICTURE XXX    VALUE "123".     NC2084.2
015800                 07  MOVE-CORR-G4.                                NC2084.2
015900                     08  MOVE-CORR-G5.                            NC2084.2
016000                         09  MOVE-CORR-E4 PICTURE XXX VALUE "ABC".NC2084.2
016100                         09  MOVE-CORR-E5 PICTURE 99  VALUE 45.   NC2084.2
016200 01  GRP-TO-MOVE-CORR-TO.                                         NC2084.2
016300     02 MOVE-CORR-G1.                                             NC2084.2
016400         04 MOVE-CORR-G2.                                         NC2084.2
016500             05 MOVE-CORR-E1      PICTURE XXX.                    NC2084.2
016600             05 MOVE-CORR-E2      PICTURE 999    OCCURS 2.        NC2084.2
016700             05 FILLER            PICTURE 999.                    NC2084.2
016800         04 MOVE-CORR-G3.                                         NC2084.2
016900             06 MOVE-CORR-E3      PICTURE 999.                    NC2084.2
017000             06 MOVE-CORR-G4.                                     NC2084.2
017100                 07 MOVE-CORR-G5  PICTURE X(5).                   NC2084.2
017200 01  GRP-FOR-MULT-REC-A.                                          NC2084.2
017300     03  WRK-DS-01V00-IN-GRP     PICTURE S9   VALUE ZERO.         NC2084.2
017400     03  WRK-DS-05V00-IN-GRP     PICTURE S9(5)  VALUE ZERO.       NC2084.2
017500     03  WRK-DS-06V06-IN-GRP     PICTURE S9(6)V9(6)  VALUE ZERO.  NC2084.2
017600 01  GRP-FOR-MULT-REC-B.                                          NC2084.2
017700     03  WRK-DS-03V10-IN-GRP     PICTURE S9(3)V9(10) VALUE ZERO.  NC2084.2
017800     03  WRK-DS-0201P-IN-GRP     PICTURE S99P        VALUE ZERO.  NC2084.2
017900     03  WRK-DS-03V00-IN-GRP     PICTURE S999        VALUE ZERO.  NC2084.2
018000 01  GRP-FOR-MULT-REC-C.                                          NC2084.2
018100     03  WRK-DS-02V00-IN-GRP     PICTURE S99  VALUE ZERO.         NC2084.2
018200     03  WRK-DS-18V00-IN-GRP     PICTURE S9(18) VALUE ZERO.       NC2084.2
018300     03  WRK-DS-09V09-IN-GRP     PICTURE S9(9)V9(9) VALUE ZERO.   NC2084.2
018400 01  WRK-DS-09V00                 PICTURE S9(9)  VALUE ZERO.      NC2084.2
018500 01  CORR-DATA-1.                                                 NC2084.2
018600     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
018700     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
018800     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
018900     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019000     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019100     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019200 01  CORR-DATA-2.                                                 NC2084.2
019300     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019400     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019500     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019600     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019700     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019800     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
019900 01  CORR-DATA-3.                                                 NC2084.2
020000     03 XYZ-4                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
020100     03 XYZ-3                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
020200     03 XYZ-6                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
020300     03 XYZ-5                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
020400     03 XYZ-2                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
020500     03 XYZ-1                PICTURE IS 99     VALUE IS ZERO.     NC2084.2
020600 01  CORR-DATA-4.                                                 NC2084.2
020700     03 XYZ-11               PICTURE IS 99.                       NC2084.2
020800     03 XYZ-12               PICTURE IS 99.                       NC2084.2
020900     03 XYZ-13               PICTURE IS 99.                       NC2084.2
021000     03 XYZ-14               PICTURE IS 99.                       NC2084.2
021100     03 XYZ-15               PICTURE IS 99.                       NC2084.2
021200     03 XYZ-16               PICTURE IS 99.                       NC2084.2
021300 01  CORR-DATA-5.                                                 NC2084.2
021400     03 XYZ-1                PICTURE 99.                          NC2084.2
021500     03 XYZ-2                PICTURE 99.                          NC2084.2
021600     03 XYZ-13               PICTURE IS 99.                       NC2084.2
021700     03 XYZ-14               PICTURE IS 99.                       NC2084.2
021800     03 FILLER               PICTURE IS 99.                       NC2084.2
021900     03 XYZ-11               PICTURE IS 99.                       NC2084.2
022000     03 XYZ-12               PICTURE IS 99.                       NC2084.2
022100 01  CORR-DATA-6.                                                 NC2084.2
022200     03 XYZ-11               PICTURE IS 99.                       NC2084.2
022300     03 XYZ-12               PICTURE IS 99.                       NC2084.2
022400     03 FILLER               PICTURE IS 99.                       NC2084.2
022500     03 XYZ-1                PICTURE IS 99.                       NC2084.2
022600     03 XYZ-2                PICTURE IS 9(2).                     NC2084.2
022700     03 FILLER               PICTURE IS 99.                       NC2084.2
022800 01  CORR-DATA-7.                                                 NC2084.2
022900     02 XYZ-1                PICTURE 99V99     VALUE 10.45.       NC2084.2
023000     02 XYZ-6                PICTURE 999V9     VALUE 100.5.       NC2084.2
023100     02 XYZ-11               PICTURE 99V9      VALUE ZERO.        NC2084.2
023200     02 XYZ-2                PICTURE 99V9      VALUE 0.9.         NC2084.2
023300 01  AN-DATANAMES.                                                NC2084.2
023400     02  ANDATA1       PICTURE X     VALUE SPACE.                 NC2084.2
023500     02  ANDATA2       PICTURE XX    VALUE SPACE.                 NC2084.2
023600     02  ANDATA3       PICTURE XXX   VALUE SPACE.                 NC2084.2
023700     02  ANDATA4       PICTURE X(4)  VALUE SPACE.                 NC2084.2
023800     02  ANDATA5       PICTURE X(5)  VALUE SPACE.                 NC2084.2
023900     02  ANDATA6       PICTURE X(6)  VALUE SPACE.                 NC2084.2
024000     02  ANDATA7       PICTURE X(7)  VALUE SPACE.                 NC2084.2
024100     02  ANDATA8       PICTURE X(8)  VALUE SPACE.                 NC2084.2
024200     02  ANDATA9       PICTURE X(9)  VALUE SPACE.                 NC2084.2
024300     02  ANDATA10      PICTURE X(10) VALUE SPACE.                 NC2084.2
024400     02  ANDATA11      PICTURE X(11) VALUE SPACE.                 NC2084.2
024500     02  ANDATA12      PICTURE X(12) VALUE SPACE.                 NC2084.2
024600     02  ANDATA13      PICTURE X(13) VALUE SPACE.                 NC2084.2
024700     02  ANDATA14      PICTURE X(14) VALUE SPACE.                 NC2084.2
024800     02  ANDATA15      PICTURE X(15) VALUE SPACE.                 NC2084.2
024900     02  ANDATA16      PICTURE X(16) VALUE SPACE.                 NC2084.2
025000     02  ANDATA17      PICTURE X(17) VALUE SPACE.                 NC2084.2
025100     02  ANDATA18      PICTURE X(18) VALUE SPACE.                 NC2084.2
025200     02  ANDATA19      PICTURE X(19) VALUE SPACE.                 NC2084.2
025300     02  ANDATA20      PICTURE X(20) VALUE SPACE.                 NC2084.2
025400     02  ANDATA21      PICTURE X(120) VALUE SPACE.                NC2084.2
025500 01  42-DATANAMES.                                                NC2084.2
025600     02  DNAME1   PICTURE 9      VALUE 1        COMPUTATIONAL.    NC2084.2
025700     02  DNAME2   PICTURE 99     VALUE 01       COMPUTATIONAL.    NC2084.2
025800     02  DNAME3   PICTURE 999    VALUE 001      COMPUTATIONAL.    NC2084.2
025900     02  DNAME4   PICTURE 9(4)   VALUE 0001     COMPUTATIONAL.    NC2084.2
026000     02  DNAME5   PICTURE 9(5)   VALUE 00001    COMPUTATIONAL.    NC2084.2
026100     02  DNAME6   PICTURE 9(6)   VALUE 000001   COMPUTATIONAL.    NC2084.2
026200     02  DNAME7   PICTURE 9(7)   VALUE 0000001  COMPUTATIONAL.    NC2084.2
026300     02  DNAME8   PICTURE 9(8)   VALUE 00000001 COMPUTATIONAL.    NC2084.2
026400     02  DNAME9   PICTURE 9(9)   VALUE 000000001.                 NC2084.2
026500     02  DNAME10  PICTURE 9(10)   VALUE 0000000001.               NC2084.2
026600     02  DNAME11  PICTURE 9(11)   VALUE 00000000001.              NC2084.2
026700     02  DNAME12  PICTURE 9(12)   VALUE 000000000001.             NC2084.2
026800     02  DNAME13  PICTURE 9(13)   VALUE 0000000000001.            NC2084.2
026900     02  DNAME14  PICTURE 9(14)   VALUE 00000000000001.           NC2084.2
027000     02  DNAME15  PICTURE 9(15)   VALUE 000000000000001.          NC2084.2
027100     02  DNAME16  PICTURE 9(16)   VALUE 0000000000000001.         NC2084.2
027200     02  DNAME17  PICTURE 9(17)   VALUE 00000000000000001.        NC2084.2
027300     02  DNAME18  PICTURE 9(18)   VALUE 000000000000000001.       NC2084.2
027400     02  DNAME19  PICTURE 9      VALUE 1.                         NC2084.2
027500     02  DNAME20  PICTURE 99     VALUE 11.                        NC2084.2
027600     02  DNAME21  PICTURE 999    VALUE 111.                       NC2084.2
027700     02  DNAME22  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
027800     02  DNAME23  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
027900     02  DNAME24  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
028000     02  DNAME25  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
028100     02  DNAME26  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
028200     02  DNAME27  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
028300     02  DNAME28  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
028400     02  DNAME29  PICTURE 9(18)  VALUE ZERO.                      NC2084.2
028500     02  DNAME30   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
028600     02  DNAME31   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
028700     02  DNAME32   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
028800     02  DNAME33   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
028900     02  DNAME34   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029000     02  DNAME35   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029100     02  DNAME36   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029200     02  DNAME37   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029300     02  DNAME38   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029400     02  DNAME39   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029500     02  DNAME40   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029600     02  DNAME41   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029700     02  DNAME42   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC2084.2
029800 01  TEST-RESULTS.                                                NC2084.2
029900     02 FILLER                   PIC X      VALUE SPACE.          NC2084.2
030000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2084.2
030100     02 FILLER                   PIC X      VALUE SPACE.          NC2084.2
030200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2084.2
030300     02 FILLER                   PIC X      VALUE SPACE.          NC2084.2
030400     02  PAR-NAME.                                                NC2084.2
030500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2084.2
030600       03  PARDOT-X              PIC X      VALUE SPACE.          NC2084.2
030700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2084.2
030800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2084.2
030900     02 RE-MARK                  PIC X(61).                       NC2084.2
031000 01  TEST-COMPUTED.                                               NC2084.2
031100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2084.2
031200     02 FILLER                   PIC X(17)  VALUE                 NC2084.2
031300            "       COMPUTED=".                                   NC2084.2
031400     02 COMPUTED-X.                                               NC2084.2
031500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2084.2
031600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2084.2
031700                                 PIC -9(9).9(9).                  NC2084.2
031800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2084.2
031900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2084.2
032000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2084.2
032100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2084.2
032200         04 COMPUTED-18V0                    PIC -9(18).          NC2084.2
032300         04 FILLER                           PIC X.               NC2084.2
032400     03 FILLER PIC X(50) VALUE SPACE.                             NC2084.2
032500 01  TEST-CORRECT.                                                NC2084.2
032600     02 FILLER PIC X(30) VALUE SPACE.                             NC2084.2
032700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2084.2
032800     02 CORRECT-X.                                                NC2084.2
032900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2084.2
033000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2084.2
033100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2084.2
033200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2084.2
033300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2084.2
033400     03      CR-18V0 REDEFINES CORRECT-A.                         NC2084.2
033500         04 CORRECT-18V0                     PIC -9(18).          NC2084.2
033600         04 FILLER                           PIC X.               NC2084.2
033700     03 FILLER PIC X(2) VALUE SPACE.                              NC2084.2
033800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2084.2
033900 01  CCVS-C-1.                                                    NC2084.2
034000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2084.2
034100-    "SS  PARAGRAPH-NAME                                          NC2084.2
034200-    "       REMARKS".                                            NC2084.2
034300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2084.2
034400 01  CCVS-C-2.                                                    NC2084.2
034500     02 FILLER                     PIC X        VALUE SPACE.      NC2084.2
034600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2084.2
034700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2084.2
034800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2084.2
034900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2084.2
035000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2084.2
035100 01  REC-CT                        PIC 99       VALUE ZERO.       NC2084.2
035200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2084.2
035300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2084.2
035400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2084.2
035500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2084.2
035600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2084.2
035700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2084.2
035800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2084.2
035900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2084.2
036000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2084.2
036100 01  CCVS-H-1.                                                    NC2084.2
036200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2084.2
036300     02  FILLER                    PIC X(42)    VALUE             NC2084.2
036400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2084.2
036500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2084.2
036600 01  CCVS-H-2A.                                                   NC2084.2
036700   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2084.2
036800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2084.2
036900   02  FILLER                        PIC XXXX   VALUE             NC2084.2
037000     "4.2 ".                                                      NC2084.2
037100   02  FILLER                        PIC X(28)  VALUE             NC2084.2
037200            " COPY - NOT FOR DISTRIBUTION".                       NC2084.2
037300   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2084.2
037400                                                                  NC2084.2
037500 01  CCVS-H-2B.                                                   NC2084.2
037600   02  FILLER                        PIC X(15)  VALUE             NC2084.2
037700            "TEST RESULT OF ".                                    NC2084.2
037800   02  TEST-ID                       PIC X(9).                    NC2084.2
037900   02  FILLER                        PIC X(4)   VALUE             NC2084.2
038000            " IN ".                                               NC2084.2
038100   02  FILLER                        PIC X(12)  VALUE             NC2084.2
038200     " HIGH       ".                                              NC2084.2
038300   02  FILLER                        PIC X(22)  VALUE             NC2084.2
038400            " LEVEL VALIDATION FOR ".                             NC2084.2
038500   02  FILLER                        PIC X(58)  VALUE             NC2084.2
038600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2084.2
038700 01  CCVS-H-3.                                                    NC2084.2
038800     02  FILLER                      PIC X(34)  VALUE             NC2084.2
038900            " FOR OFFICIAL USE ONLY    ".                         NC2084.2
039000     02  FILLER                      PIC X(58)  VALUE             NC2084.2
039100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2084.2
039200     02  FILLER                      PIC X(28)  VALUE             NC2084.2
039300            "  COPYRIGHT   1985 ".                                NC2084.2
039400 01  CCVS-E-1.                                                    NC2084.2
039500     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2084.2
039600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2084.2
039700     02 ID-AGAIN                     PIC X(9).                    NC2084.2
039800     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2084.2
039900 01  CCVS-E-2.                                                    NC2084.2
040000     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2084.2
040100     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2084.2
040200     02 CCVS-E-2-2.                                               NC2084.2
040300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2084.2
040400         03 FILLER                   PIC X      VALUE SPACE.      NC2084.2
040500         03 ENDER-DESC               PIC X(44)  VALUE             NC2084.2
040600            "ERRORS ENCOUNTERED".                                 NC2084.2
040700 01  CCVS-E-3.                                                    NC2084.2
040800     02  FILLER                      PIC X(22)  VALUE             NC2084.2
040900            " FOR OFFICIAL USE ONLY".                             NC2084.2
041000     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2084.2
041100     02  FILLER                      PIC X(58)  VALUE             NC2084.2
041200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2084.2
041300     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2084.2
041400     02 FILLER                       PIC X(15)  VALUE             NC2084.2
041500             " COPYRIGHT 1985".                                   NC2084.2
041600 01  CCVS-E-4.                                                    NC2084.2
041700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2084.2
041800     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2084.2
041900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2084.2
042000     02 FILLER                       PIC X(40)  VALUE             NC2084.2
042100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2084.2
042200 01  XXINFO.                                                      NC2084.2
042300     02 FILLER                       PIC X(19)  VALUE             NC2084.2
042400            "*** INFORMATION ***".                                NC2084.2
042500     02 INFO-TEXT.                                                NC2084.2
042600       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2084.2
042700       04 XXCOMPUTED                 PIC X(20).                   NC2084.2
042800       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2084.2
042900       04 XXCORRECT                  PIC X(20).                   NC2084.2
043000     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2084.2
043100 01  HYPHEN-LINE.                                                 NC2084.2
043200     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2084.2
043300     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2084.2
043400-    "*****************************************".                 NC2084.2
043500     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2084.2
043600-    "******************************".                            NC2084.2
043700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2084.2
043800     "NC208A".                                                    NC2084.2
043900 PROCEDURE DIVISION.                                              NC2084.2
044000 CCVS1 SECTION.                                                   NC2084.2
044100 OPEN-FILES.                                                      NC2084.2
044200     OPEN     OUTPUT PRINT-FILE.                                  NC2084.2
044300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2084.2
044400     MOVE    SPACE TO TEST-RESULTS.                               NC2084.2
044500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2084.2
044600     GO TO CCVS1-EXIT.                                            NC2084.2
044700 CLOSE-FILES.                                                     NC2084.2
044800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2084.2
044900 TERMINATE-CCVS.                                                  NC2084.2
045000*    EXIT PROGRAM.                                                NC2084.2
045100*TERMINATE-CALL.                                                  NC2084.2
045200     STOP     RUN.                                                NC2084.2
045300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2084.2
045400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2084.2
045500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2084.2
045600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2084.2
045700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2084.2
045800 PRINT-DETAIL.                                                    NC2084.2
045900     IF REC-CT NOT EQUAL TO ZERO                                  NC2084.2
046000             MOVE "." TO PARDOT-X                                 NC2084.2
046100             MOVE REC-CT TO DOTVALUE.                             NC2084.2
046200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2084.2
046300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2084.2
046400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2084.2
046500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2084.2
046600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2084.2
046700     MOVE SPACE TO CORRECT-X.                                     NC2084.2
046800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2084.2
046900     MOVE     SPACE TO RE-MARK.                                   NC2084.2
047000 HEAD-ROUTINE.                                                    NC2084.2
047100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2084.2
047200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2084.2
047300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2084.2
047400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2084.2
047500 COLUMN-NAMES-ROUTINE.                                            NC2084.2
047600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2084.2
047700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2084.2
047800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2084.2
047900 END-ROUTINE.                                                     NC2084.2
048000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2084.2
048100 END-RTN-EXIT.                                                    NC2084.2
048200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2084.2
048300 END-ROUTINE-1.                                                   NC2084.2
048400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2084.2
048500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2084.2
048600      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2084.2
048700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2084.2
048800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2084.2
048900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2084.2
049000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2084.2
049100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2084.2
049200  END-ROUTINE-12.                                                 NC2084.2
049300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2084.2
049400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2084.2
049500         MOVE "NO " TO ERROR-TOTAL                                NC2084.2
049600         ELSE                                                     NC2084.2
049700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2084.2
049800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2084.2
049900     PERFORM WRITE-LINE.                                          NC2084.2
050000 END-ROUTINE-13.                                                  NC2084.2
050100     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2084.2
050200         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2084.2
050300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2084.2
050400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2084.2
050500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2084.2
050600      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2084.2
050700          MOVE "NO " TO ERROR-TOTAL                               NC2084.2
050800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2084.2
050900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2084.2
051000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2084.2
051100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2084.2
051200 WRITE-LINE.                                                      NC2084.2
051300     ADD 1 TO RECORD-COUNT.                                       NC2084.2
051400*    IF RECORD-COUNT GREATER 50                                   NC2084.2
051500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2084.2
051600*        MOVE SPACE TO DUMMY-RECORD                               NC2084.2
051700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2084.2
051800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2084.2
051900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2084.2
052000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2084.2
052100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2084.2
052200*        MOVE ZERO TO RECORD-COUNT.                               NC2084.2
052300     PERFORM WRT-LN.                                              NC2084.2
052400 WRT-LN.                                                          NC2084.2
052500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2084.2
052600     MOVE SPACE TO DUMMY-RECORD.                                  NC2084.2
052700 BLANK-LINE-PRINT.                                                NC2084.2
052800     PERFORM WRT-LN.                                              NC2084.2
052900 FAIL-ROUTINE.                                                    NC2084.2
053000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2084.2
053100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2084.2
053200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2084.2
053300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2084.2
053400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2084.2
053500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2084.2
053600     GO TO  FAIL-ROUTINE-EX.                                      NC2084.2
053700 FAIL-ROUTINE-WRITE.                                              NC2084.2
053800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2084.2
053900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2084.2
054000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2084.2
054100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2084.2
054200 FAIL-ROUTINE-EX. EXIT.                                           NC2084.2
054300 BAIL-OUT.                                                        NC2084.2
054400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2084.2
054500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2084.2
054600 BAIL-OUT-WRITE.                                                  NC2084.2
054700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2084.2
054800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2084.2
054900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2084.2
055000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2084.2
055100 BAIL-OUT-EX. EXIT.                                               NC2084.2
055200 CCVS1-EXIT.                                                      NC2084.2
055300     EXIT.                                                        NC2084.2
055400 QUAL-SECTION-1 SECTION.                                          NC2084.2
055500 PAR-INIT-F2-1.                                                   NC2084.2
055600     MOVE "PAR-TEST-F2-1         " TO PAR-NAME.                   NC2084.2
055700     MOVE "IV-20 4.3.8.1 RULE 6" TO ANSI-REFERENCE.               NC2084.2
055800 PAR-TEST-F2-1.                                                   NC2084.2
055900     PERFORM PAR-1A OF QUAL-SECTION-1.                            NC2084.2
056000     IF QT1 EQUAL TO "PASS"                                       NC2084.2
056100                   PERFORM PASS                                   NC2084.2
056200                   GO TO PAR-WRITE-F2-1.                          NC2084.2
056300     GO TO PAR-FAIL-F2-1.                                         NC2084.2
056400 PAR-DELETE-F2-1.                                                 NC2084.2
056500     PERFORM DE-LETE.                                             NC2084.2
056600     GO TO PAR-WRITE-F2-1.                                        NC2084.2
056700 PAR-FAIL-F2-1.                                                   NC2084.2
056800     PERFORM FAIL.                                                NC2084.2
056900*    NOTE                                                         NC2084.2
057000*    PERFORM PARAGRAPH IN SAME SECTION.                           NC2084.2
057100 PAR-WRITE-F2-1.                                                  NC2084.2
057200     PERFORM PRINT-DETAIL.                                        NC2084.2
057300     GO TO PAR-1-EXIT.                                            NC2084.2
057400*                                                                 NC2084.2
057500 PAR-1A.                                                          NC2084.2
057600     MOVE "PASS" TO QT1.                                          NC2084.2
057700 PAR-1-EXIT.                                                      NC2084.2
057800     EXIT.                                                        NC2084.2
057900*                                                                 NC2084.2
058000 PAR-INIT-F2-2.                                                   NC2084.2
058100     MOVE "PAR-TEST-F2-2" TO PAR-NAME.                            NC2084.2
058200     MOVE "IV-20 4.3.8.1 RULE 6" TO ANSI-REFERENCE.               NC2084.2
058300 PAR-TEST-F2-2.                                                   NC2084.2
058400     PERFORM PAR-2A OF QUAL-SECTION-2.                            NC2084.2
058500     IF QT2 EQUAL TO "PASS"                                       NC2084.2
058600                PERFORM PASS                                      NC2084.2
058700                GO TO PAR-WRITE-F2-2.                             NC2084.2
058800     GO TO PAR-FAIL-F2-2.                                         NC2084.2
058900 PAR-DELETE-F2-2.                                                 NC2084.2
059000     PERFORM DE-LETE.                                             NC2084.2
059100     GO TO PAR-WRITE-F2-2.                                        NC2084.2
059200 PAR-FAIL-F2-2.                                                   NC2084.2
059300     PERFORM FAIL.                                                NC2084.2
059400*    NOTE                                                         NC2084.2
059500*    PERFORM PARAGRAPH IN A DIFFERENT SECTION.                    NC2084.2
059600 PAR-WRITE-F2-2.                                                  NC2084.2
059700     PERFORM PRINT-DETAIL.                                        NC2084.2
059800     GO TO PAR-2-EXIT.                                            NC2084.2
059900 PAR-2A.                                                          NC2084.2
060000     MOVE "FAIL" TO QT2.                                          NC2084.2
060100 PAR-2-EXIT.                                                      NC2084.2
060200     EXIT.                                                        NC2084.2
060300*                                                                 NC2084.2
060400 PAR-INIT-F2-3.                                                   NC2084.2
060500     MOVE "PAR-TEST-F2-3" TO PAR-NAME.                            NC2084.2
060600     MOVE "IV-20 4.3.8.1 RULE 6" TO ANSI-REFERENCE.               NC2084.2
060700 PAR-TEST-F2-3.                                                   NC2084.2
060800     GO TO PAR-3B IN QUAL-SECTION-1.                              NC2084.2
060900*    NOTE                                                         NC2084.2
061000*    GO TO IN SAME SECTION.                                       NC2084.2
061100 PAR-3A.                                                          NC2084.2
061200     MOVE "FAIL" TO QT3.                                          NC2084.2
061300     GO TO PAR-3C.                                                NC2084.2
061400 PAR-3B.                                                          NC2084.2
061500     MOVE "PASS" TO QT3.                                          NC2084.2
061600 PAR-3C.                                                          NC2084.2
061700     IF QT3 EQUAL TO "PASS"                                       NC2084.2
061800               PERFORM PASS                                       NC2084.2
061900               GO TO PAR-WRITE-F2-3.                              NC2084.2
062000     GO TO PAR-FAIL-F2-3.                                         NC2084.2
062100 PAR-DELETE-F2-3.                                                 NC2084.2
062200     PERFORM DE-LETE.                                             NC2084.2
062300     GO TO PAR-WRITE-F2-3.                                        NC2084.2
062400 PAR-FAIL-F2-3.                                                   NC2084.2
062500     PERFORM FAIL.                                                NC2084.2
062600 PAR-WRITE-F2-3.                                                  NC2084.2
062700     PERFORM PRINT-DETAIL.                                        NC2084.2
062800 PAR-3-EXIT.                                                      NC2084.2
062900     EXIT.                                                        NC2084.2
063000*                                                                 NC2084.2
063100 PAR-INIT-F2-4.                                                   NC2084.2
063200     MOVE "PAR-TEST-F2-4" TO PAR-NAME.                            NC2084.2
063300     MOVE "IV-20 4.3.8.1 RULE 6" TO ANSI-REFERENCE.               NC2084.2
063400 PAR-TEST-F2-4.                                                   NC2084.2
063500     GO TO PAR-4B IN QUAL-SECTION-2.                              NC2084.2
063600*    NOTE                                                         NC2084.2
063700*    GO TO IN DIFFERENT SECTION.                                  NC2084.2
063800 PAR-4A.                                                          NC2084.2
063900     MOVE "FAIL" TO QT4.                                          NC2084.2
064000     GO TO PAR-4C.                                                NC2084.2
064100 PAR-4B.                                                          NC2084.2
064200     MOVE "FAIL" TO QT4.                                          NC2084.2
064300 PAR-4C.                                                          NC2084.2
064400     IF QT4 EQUAL TO "PASS"                                       NC2084.2
064500             PERFORM PASS                                         NC2084.2
064600             GO TO PAR-WRITE-F2-4.                                NC2084.2
064700     GO TO PAR-FAIL-F2-4.                                         NC2084.2
064800 PAR-DELETE-F2-4.                                                 NC2084.2
064900     PERFORM DE-LETE.                                             NC2084.2
065000     GO TO PAR-WRITE-F2-4.                                        NC2084.2
065100 PAR-FAIL-F2-4.                                                   NC2084.2
065200     PERFORM FAIL.                                                NC2084.2
065300 PAR-WRITE-F2-4.                                                  NC2084.2
065400     PERFORM PRINT-DETAIL.                                        NC2084.2
065500 PAR-4-EXIT.                                                      NC2084.2
065600     PERFORM END-ROUTINE.                                         NC2084.2
065700     GO TO QUAL-EXIT.                                             NC2084.2
065800 QUAL-SECTION-2 SECTION.                                          NC2084.2
065900 PAR-1A.                                                          NC2084.2
066000     MOVE "FAIL" TO QT1.                                          NC2084.2
066100 PAR-2A.                                                          NC2084.2
066200     MOVE "PASS" TO QT2.                                          NC2084.2
066300 PAR-3B.                                                          NC2084.2
066400     MOVE "FAIL" TO QT3.                                          NC2084.2
066500     GO TO PAR-3C OF QUAL-SECTION-1.                              NC2084.2
066600 PAR-3C.                                                          NC2084.2
066700     PERFORM FAIL.                                                NC2084.2
066800*    NOTE  THIS PARAGRAPH SHOULD NEVER BE ENTERED.                NC2084.2
066900     GO TO PAR-INIT-F2-4 IN QUAL-SECTION-1.                       NC2084.2
067000 PAR-4.                                                           NC2084.2
067100     GO TO QUAL-EXIT.                                             NC2084.2
067200*    NOTE                                                         NC2084.2
067300*                  IF                                             NC2084.2
067400*    GO TO DIFFERENT SECTION FAILS END QUALIFICATION TEST.        NC2084.2
067500 PAR-4B.                                                          NC2084.2
067600     MOVE "PASS" TO QT4.                                          NC2084.2
067700     GO TO PAR-4C IN QUAL-SECTION-1.                              NC2084.2
067800 QUAL-EXIT.                                                       NC2084.2
067900     EXIT.                                                        NC2084.2
068000 DATA-NAME-QUAL SECTION.                                          NC2084.2
068100 QAL-INIT-F1-1.                                                   NC2084.2
068200     MOVE "QAL-TEST-F1-1      " TO PAR-NAME.                      NC2084.2
068300     MOVE SPACE TO TEST-RESULTS.                                  NC2084.2
068400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
068500     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
068600 QAL-TEST-F1-1.                                                   NC2084.2
068700     MOVE "123456789" TO QUAL-TEST-SUB-GRP-2.                     NC2084.2
068800     MOVE ZERO TO GRP-FOR-QUAL-TO.                                NC2084.2
068900     MOVE 2 TO WRK-DS-01V00.                                      NC2084.2
069000     MOVE QUAL-TEST-1 OF GRP-FOR-QUAL-FROM                        NC2084.2
069100         TO QUAL-TEST-1 OF GRP-FOR-QUAL-TO.                       NC2084.2
069200     IF QUAL-TEST-1 OF GRP-FOR-QUAL-TO EQUAL TO                   NC2084.2
069300         "ABCDEFGHIJKLMNOPQRSTUVWXYZ"                             NC2084.2
069400         PERFORM PASS                                             NC2084.2
069500         GO TO QAL-WRITE-F1-1.                                    NC2084.2
069600     GO TO QAL-FAIL-F1-1.                                         NC2084.2
069700 QAL-DELETE-F1-1.                                                 NC2084.2
069800     PERFORM DE-LETE.                                             NC2084.2
069900     GO TO QAL-WRITE-F1-1.                                        NC2084.2
070000 QAL-FAIL-F1-1.                                                   NC2084.2
070100     MOVE "ABCDEFGGHIJKLMNOPQRSTUVWXYZ" TO SEND-BREAKDOWN         NC2084.2
070200     MOVE QUAL-TEST-1 OF GRP-FOR-QUAL-TO TO RECEIVE-BREAKDOWN     NC2084.2
070300     PERFORM FAIL                                                 NC2084.2
070400     MOVE SEND-20 TO CORRECT-A                                    NC2084.2
070500     MOVE RECEIVE-20 TO COMPUTED-A                                NC2084.2
070600     MOVE "1ST 20 POSITIONS OF ANSWERS" TO RE-MARK                NC2084.2
070700     MOVE TEST-RESULTS TO PRINT-REC.                              NC2084.2
070800     WRITE PRINT-REC                                              NC2084.2
070900     MOVE SPACES TO P-OR-F                                        NC2084.2
071000     MOVE SEND-40 TO CORRECT-A                                    NC2084.2
071100     MOVE RECEIVE-40 TO COMPUTED-A                                NC2084.2
071200     MOVE "QAL-TEST-F1-1      " TO PAR-NAME.                      NC2084.2
071300     MOVE "2ND 20 POSITIONS OF ANSWERS" TO RE-MARK.               NC2084.2
071400 QAL-WRITE-F1-1.                                                  NC2084.2
071500     PERFORM PRINT-DETAIL.                                        NC2084.2
071600*                                                                 NC2084.2
071700 QAL-INIT-F1-2.                                                   NC2084.2
071800     MOVE "QAL-TEST-F1-2      " TO PAR-NAME.                      NC2084.2
071900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
072000     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
072100     MOVE ZERO TO QUAL-TEST-2-TO.                                 NC2084.2
072200 QAL-TEST-F1-2.                                                   NC2084.2
072300     ADD QUAL-TEST-2 OF GRP-FOR-QUAL-FROM                         NC2084.2
072400         TO QUAL-TEST-2 OF GRP-FOR-QUAL-TO.                       NC2084.2
072500     IF QUAL-TEST-2 OF GRP-FOR-QUAL-TO EQUAL TO 9999999999        NC2084.2
072600         PERFORM PASS                                             NC2084.2
072700         GO TO QAL-WRITE-F1-2.                                    NC2084.2
072800     GO TO QAL-FAIL-F1-2.                                         NC2084.2
072900 QAL-DELETE-F1-2.                                                 NC2084.2
073000     PERFORM DE-LETE.                                             NC2084.2
073100     GO TO QAL-WRITE-F1-2.                                        NC2084.2
073200 QAL-FAIL-F1-2.                                                   NC2084.2
073300     MOVE 9999999999 TO CORRECT-18V0.                             NC2084.2
073400     MOVE QUAL-TEST-2 OF GRP-FOR-QUAL-TO TO COMPUTED-18V0.        NC2084.2
073500     PERFORM FAIL.                                                NC2084.2
073600 QAL-WRITE-F1-2.                                                  NC2084.2
073700     PERFORM PRINT-DETAIL.                                        NC2084.2
073800*                                                                 NC2084.2
073900 QAL-INIT-F1-3.                                                   NC2084.2
074000     MOVE "QAL-TEST-F1-3      " TO PAR-NAME.                      NC2084.2
074100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
074200     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
074300 QAL-TEST-F1-3.                                                   NC2084.2
074400     MULTIPLY QUAL-TEST-3 OF GRP-FOR-QUAL-FROM BY WRK-DS-01V00    NC2084.2
074500         GIVING QUAL-TEST-3 OF GRP-FOR-QUAL-TO.                   NC2084.2
074600     IF QUAL-TEST-3 OF GRP-FOR-QUAL-TO EQUAL TO 004               NC2084.2
074700         PERFORM PASS                                             NC2084.2
074800         GO TO QAL-WRITE-F1-3.                                    NC2084.2
074900     GO TO QAL-FAIL-F1-3.                                         NC2084.2
075000 QAL-DELETE-F1-3.                                                 NC2084.2
075100     PERFORM DE-LETE.                                             NC2084.2
075200     GO TO QAL-WRITE-F1-3.                                        NC2084.2
075300 QAL-FAIL-F1-3.                                                   NC2084.2
075400     MOVE 004 TO CORRECT-N.                                       NC2084.2
075500     MOVE QUAL-TEST-3 OF GRP-FOR-QUAL-TO TO COMPUTED-N.           NC2084.2
075600     PERFORM FAIL.                                                NC2084.2
075700 QAL-WRITE-F1-3.                                                  NC2084.2
075800     PERFORM PRINT-DETAIL.                                        NC2084.2
075900*                                                                 NC2084.2
076000 QAL-INIT-F1-4.                                                   NC2084.2
076100     MOVE "QAL-TEST-F1-4      " TO PAR-NAME.                      NC2084.2
076200     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
076300     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
076400 QAL-TEST-F1-4.                                                   NC2084.2
076500     MOVE QUAL-TEST-4 OF GRP-FOR-QUAL-FROM (WRK-DS-01V00)         NC2084.2
076600         TO QUAL-TEST-4 OF GRP-FOR-QUAL-TO (WRK-DS-01V00).        NC2084.2
076700     IF QUAL-TEST1  OF GRP-FOR-QUAL-TO EQUAL TO "02000"           NC2084.2
076800         PERFORM PASS                                             NC2084.2
076900         GO TO QAL-WRITE-F1-4.                                    NC2084.2
077000     GO TO QAL-FAIL-F1-4.                                         NC2084.2
077100 QAL-DELETE-F1-4.                                                 NC2084.2
077200     PERFORM DE-LETE.                                             NC2084.2
077300     GO TO QAL-WRITE-F1-4.                                        NC2084.2
077400 QAL-FAIL-F1-4.                                                   NC2084.2
077500     MOVE "02000" TO CORRECT-A.                                   NC2084.2
077600     MOVE QUAL-TEST1  OF GRP-FOR-QUAL-TO TO COMPUTED-A.           NC2084.2
077700     PERFORM FAIL.                                                NC2084.2
077800 QAL-WRITE-F1-4.                                                  NC2084.2
077900     PERFORM PRINT-DETAIL.                                        NC2084.2
078000*                                                                 NC2084.2
078100 QAL-INIT-F1-5.                                                   NC2084.2
078200     MOVE "QAL-TEST-F1-5      " TO PAR-NAME.                      NC2084.2
078300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
078400     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
078500 QAL-TEST-F1-5.                                                   NC2084.2
078600     MOVE QUAL-TEST-1-FROM IN GRP-FOR-QUAL-FROM TO                NC2084.2
078700         QUAL-TEST-1-TO IN GRP-FOR-QUAL-TO.                       NC2084.2
078800     IF QUAL-TEST-1-TO IN GRP-FOR-QUAL-TO EQUAL TO                NC2084.2
078900         "ABCDEFGHIJKLMNOPQRSTUVWXYZ"                             NC2084.2
079000         PERFORM PASS                                             NC2084.2
079100         GO TO QAL-WRITE-F1-5.                                    NC2084.2
079200     GO TO QAL-FAIL-F1-5.                                         NC2084.2
079300 QAL-DELETE-F1-5.                                                 NC2084.2
079400     PERFORM DE-LETE.                                             NC2084.2
079500     GO TO QAL-WRITE-F1-5.                                        NC2084.2
079600 QAL-FAIL-F1-5.                                                   NC2084.2
079700     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO SEND-BREAKDOWN.         NC2084.2
079800     MOVE QUAL-TEST-1-TO IN GRP-FOR-QUAL-TO TO RECEIVE-BREAKDOWN. NC2084.2
079900     PERFORM FAIL.                                                NC2084.2
080000     MOVE SEND-20 TO CORRECT-A.                                   NC2084.2
080100     MOVE RECEIVE-20 TO COMPUTED-A.                               NC2084.2
080200     PERFORM QAL-WRITE-F1-5.                                      NC2084.2
080300     MOVE SPACES TO P-OR-F.                                       NC2084.2
080400     MOVE SEND-40 TO CORRECT-A.                                   NC2084.2
080500     MOVE RECEIVE-40 TO COMPUTED-A.                               NC2084.2
080600     MOVE "2ND 20 POSITIONS OF ANSWERS" TO RE-MARK.               NC2084.2
080700     MOVE TEST-RESULTS TO PRINT-REC.                              NC2084.2
080800     WRITE PRINT-REC.                                             NC2084.2
080900 QAL-WRITE-F1-5.                                                  NC2084.2
081000     PERFORM PRINT-DETAIL.                                        NC2084.2
081100*                                                                 NC2084.2
081200 QAL-INIT-F1-6.                                                   NC2084.2
081300     MOVE "QAL-TEST-F1-6      " TO PAR-NAME.                      NC2084.2
081400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
081500     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
081600 QAL-TEST-F1-6.                                                   NC2084.2
081700     MOVE 0000000000 TO QUAL-TEST-2-TO IN GRP-FOR-QUAL-TO.        NC2084.2
081800     ADD QUAL-TEST-2-FROM IN GRP-FOR-QUAL-FROM TO                 NC2084.2
081900         QUAL-TEST-2-TO IN GRP-FOR-QUAL-TO.                       NC2084.2
082000     IF QUAL-TEST-2-TO IN GRP-FOR-QUAL-TO EQUAL TO 9999999999     NC2084.2
082100         PERFORM PASS                                             NC2084.2
082200         GO TO QAL-WRITE-F1-6.                                    NC2084.2
082300     GO TO QAL-FAIL-F1-6.                                         NC2084.2
082400 QAL-DELETE-F1-6.                                                 NC2084.2
082500     PERFORM DE-LETE.                                             NC2084.2
082600     GO TO QAL-WRITE-F1-6.                                        NC2084.2
082700 QAL-FAIL-F1-6.                                                   NC2084.2
082800     MOVE 9999999999 TO CORRECT-18V0.                             NC2084.2
082900     MOVE QUAL-TEST-2-TO IN GRP-FOR-QUAL-TO TO COMPUTED-18V0.     NC2084.2
083000     PERFORM FAIL.                                                NC2084.2
083100 QAL-WRITE-F1-6.                                                  NC2084.2
083200     PERFORM PRINT-DETAIL.                                        NC2084.2
083300*                                                                 NC2084.2
083400 QAL-INIT-F1-7.                                                   NC2084.2
083500     MOVE "QAL-TEST-F1-7      " TO PAR-NAME.                      NC2084.2
083600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
083700     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
083800 QAL-TEST-F1-7.                                                   NC2084.2
083900     MULTIPLY QUAL-TEST-3-FROM IN GRP-FOR-QUAL-FROM BY            NC2084.2
084000         WRK-DS-01V00 GIVING QUAL-TEST-3-TO IN GRP-FOR-QUAL-TO.   NC2084.2
084100     IF QUAL-TEST-3-TO IN GRP-FOR-QUAL-TO EQUAL TO 004            NC2084.2
084200         PERFORM PASS                                             NC2084.2
084300         GO TO QAL-WRITE-F1-7.                                    NC2084.2
084400     GO TO QAL-FAIL-F1-7.                                         NC2084.2
084500 QAL-DELETE-F1-7.                                                 NC2084.2
084600     PERFORM DE-LETE.                                             NC2084.2
084700     GO TO QAL-WRITE-F1-7.                                        NC2084.2
084800 QAL-FAIL-F1-7.                                                   NC2084.2
084900     MOVE 004 TO CORRECT-N.                                       NC2084.2
085000     MOVE QUAL-TEST-3-TO IN GRP-FOR-QUAL-TO TO COMPUTED-N.        NC2084.2
085100     PERFORM FAIL.                                                NC2084.2
085200 QAL-WRITE-F1-7.                                                  NC2084.2
085300     PERFORM PRINT-DETAIL.                                        NC2084.2
085400*                                                                 NC2084.2
085500 QAL-INIT-F1-8.                                                   NC2084.2
085600     MOVE "QAL-TEST-F1-8      " TO PAR-NAME.                      NC2084.2
085700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
085800     MOVE "DATA-NAME QUAL      " TO FEATURE.                      NC2084.2
085900 QAL-TEST-F1-8.                                                   NC2084.2
086000     MOVE QUAL-TEST-4-FROM IN GRP-FOR-QUAL-FROM (WRK-DS-01V00)    NC2084.2
086100         TO QUAL-TEST-4-TO IN GRP-FOR-QUAL-TO (WRK-DS-01V00).     NC2084.2
086200     IF QUAL-TEST2     IN GRP-FOR-QUAL-TO EQUAL TO "0700"         NC2084.2
086300         PERFORM PASS                                             NC2084.2
086400         GO TO QAL-WRITE-F1-8.                                    NC2084.2
086500     GO TO QAL-FAIL-F1-8.                                         NC2084.2
086600 QAL-DELETE-F1-8.                                                 NC2084.2
086700     PERFORM DE-LETE.                                             NC2084.2
086800     GO TO QAL-WRITE-F1-8.                                        NC2084.2
086900 QAL-FAIL-F1-8.                                                   NC2084.2
087000     MOVE "0700" TO CORRECT-A.                                    NC2084.2
087100     MOVE QUAL-TEST2 TO COMPUTED-A.                               NC2084.2
087200     PERFORM FAIL.                                                NC2084.2
087300 QAL-WRITE-F1-8.                                                  NC2084.2
087400     PERFORM PRINT-DETAIL.                                        NC2084.2
087500     PERFORM END-ROUTINE.                                         NC2084.2
087600*                                                                 NC2084.2
087700 MOVE-CORR-ROUTINE SECTION.                                       NC2084.2
087800 MOV-INIT-F1-1.                                                   NC2084.2
087900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
088000     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
088100     MOVE THREE TO XYZ-1 OF CORR-DATA-1.                          NC2084.2
088200     MOVE FOUR TO XYZ-2 OF CORR-DATA-1.                           NC2084.2
088300     MOVE TEN TO XYZ-3 OF CORR-DATA-1.                            NC2084.2
088400     MOVE ZERO TO XYZ-4 OF CORR-DATA-1.                           NC2084.2
088500     MOVE 01 TO XYZ-5 OF CORR-DATA-1.                             NC2084.2
088600     MOVE 00 TO XYZ-6 OF CORR-DATA-1.                             NC2084.2
088700 MOV-INIT-F1-1-1.                                                 NC2084.2
088800     MOVE "MOV-TEST-F1-1-1" TO PAR-NAME.                          NC2084.2
088900 MOV-TEST-F1-1-1.                                                 NC2084.2
089000     MOVE CORRESPONDING CORR-DATA-1 TO CORR-DATA-2.               NC2084.2
089100                   IF XYZ-2 OF CORR-DATA-2 EQUAL TO 4             NC2084.2
089200     PERFORM PASS ELSE                                            NC2084.2
089300     GO TO MOV-FAIL-F1-1-1.                                       NC2084.2
089400     GO TO MOV-WRITE-F1-1-1.                                      NC2084.2
089500 MOV-DELETE-F1-1-1.                                               NC2084.2
089600     PERFORM DE-LETE.                                             NC2084.2
089700     GO TO MOV-WRITE-F1-1-1.                                      NC2084.2
089800 MOV-FAIL-F1-1-1.                                                 NC2084.2
089900     PERFORM FAIL.                                                NC2084.2
090000     MOVE XYZ-2 OF CORR-DATA-2 TO COMPUTED-A.                     NC2084.2
090100     MOVE 04 TO CORRECT-A.                                        NC2084.2
090200 MOV-WRITE-F1-1-1.                                                NC2084.2
090300     PERFORM PRINT-DETAIL.                                        NC2084.2
090400*                                                                 NC2084.2
090500 MOV-INIT-F1-1-2.                                                 NC2084.2
090600     MOVE "MOV-TEST-F1-1-2" TO PAR-NAME.                          NC2084.2
090700 MOV-TEST-F1-1-2.                                                 NC2084.2
090800     IF XYZ-1 OF CORR-DATA-2 EQUAL TO THREE                       NC2084.2
090900              PERFORM PASS ELSE                                   NC2084.2
091000              GO TO MOV-FAIL-F1-1-2.                              NC2084.2
091100     GO TO MOV-WRITE-F1-1-2.                                      NC2084.2
091200 MOV-DELETE-F1-1-2.                                               NC2084.2
091300     PERFORM DE-LETE.                                             NC2084.2
091400     GO TO MOV-WRITE-F1-1-2.                                      NC2084.2
091500 MOV-FAIL-F1-1-2.                                                 NC2084.2
091600     PERFORM FAIL.                                                NC2084.2
091700     MOVE XYZ-1 OF CORR-DATA-2 TO COMPUTED-A.                     NC2084.2
091800     MOVE THREE TO CORRECT-A.                                     NC2084.2
091900 MOV-WRITE-F1-1-2.                                                NC2084.2
092000     PERFORM PRINT-DETAIL.                                        NC2084.2
092100*                                                                 NC2084.2
092200 MOV-INIT-F1-1-3.                                                 NC2084.2
092300     MOVE "MOV-TEST-F1-1-3" TO PAR-NAME.                          NC2084.2
092400 MOV-TEST-F1-1-3.                                                 NC2084.2
092500     IF XYZ-3 OF CORR-DATA-2 EQUAL TO TEN                         NC2084.2
092600            PERFORM PASS ELSE                                     NC2084.2
092700            GO TO MOV-FAIL-F1-1-3.                                NC2084.2
092800     GO TO MOV-WRITE-F1-1-3.                                      NC2084.2
092900 MOV-DELETE-F1-1-3.                                               NC2084.2
093000     PERFORM DE-LETE.                                             NC2084.2
093100     GO TO MOV-WRITE-F1-1-3.                                      NC2084.2
093200 MOV-FAIL-F1-1-3.                                                 NC2084.2
093300     MOVE XYZ-3 OF CORR-DATA-2 TO COMPUTED-A.                     NC2084.2
093400     MOVE "10" TO CORRECT-A.                                      NC2084.2
093500     PERFORM FAIL.                                                NC2084.2
093600 MOV-WRITE-F1-1-3.                                                NC2084.2
093700     PERFORM PRINT-DETAIL.                                        NC2084.2
093800*                                                                 NC2084.2
093900 MOV-INIT-F1-1-4.                                                 NC2084.2
094000     MOVE "MOV-TEST-F1-1-4" TO PAR-NAME.                          NC2084.2
094100 MOV-TEST-F1-1-4.                                                 NC2084.2
094200     IF XYZ-4 OF CORR-DATA-2 EQUAL TO XYZ-4 OF                    NC2084.2
094300     CORR-DATA-1                                                  NC2084.2
094400             PERFORM PASS ELSE                                    NC2084.2
094500             GO TO MOV-FAIL-F1-1-4.                               NC2084.2
094600     GO TO MOV-WRITE-F1-1-4.                                      NC2084.2
094700 MOV-DELETE-F1-1-4.                                               NC2084.2
094800     PERFORM DE-LETE.                                             NC2084.2
094900     GO TO MOV-WRITE-F1-1-4.                                      NC2084.2
095000 MOV-FAIL-F1-1-4.                                                 NC2084.2
095100     PERFORM FAIL.                                                NC2084.2
095200     MOVE XYZ-4 OF CORR-DATA-2 TO COMPUTED-A.                     NC2084.2
095300     MOVE XYZ-4 OF CORR-DATA-1 TO CORRECT-A.                      NC2084.2
095400 MOV-WRITE-F1-1-4.                                                NC2084.2
095500     PERFORM PRINT-DETAIL.                                        NC2084.2
095600*                                                                 NC2084.2
095700 MOV-INIT-F1-1-5.                                                 NC2084.2
095800     MOVE "MOV-TEST-F1-1-5" TO PAR-NAME.                          NC2084.2
095900 MOV-TEST-F1-1-5.                                                 NC2084.2
096000     IF XYZ-5 OF CORR-DATA-2 EQUAL TO 01                          NC2084.2
096100             PERFORM PASS ELSE                                    NC2084.2
096200             GO TO MOV-FAIL-F1-1-5.                               NC2084.2
096300     GO TO MOV-WRITE-F1-1-5.                                      NC2084.2
096400 MOV-DELETE-F1-1-5.                                               NC2084.2
096500     PERFORM DE-LETE.                                             NC2084.2
096600     GO TO MOV-WRITE-F1-1-5.                                      NC2084.2
096700 MOV-FAIL-F1-1-5.                                                 NC2084.2
096800     MOVE XYZ-5 OF CORR-DATA-2 TO COMPUTED-A.                     NC2084.2
096900     MOVE "01" TO CORRECT-A.                                      NC2084.2
097000     PERFORM FAIL.                                                NC2084.2
097100 MOV-WRITE-F1-1-5.                                                NC2084.2
097200     PERFORM PRINT-DETAIL.                                        NC2084.2
097300*                                                                 NC2084.2
097400 MOV-INIT-F1-2.                                                   NC2084.2
097500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
097600     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
097700     MOVE THREE TO XYZ-1 OF CORR-DATA-1.                          NC2084.2
097800     MOVE FOUR TO XYZ-2 OF CORR-DATA-1.                           NC2084.2
097900     MOVE TEN TO XYZ-3 OF CORR-DATA-1.                            NC2084.2
098000     MOVE ZERO TO XYZ-4 OF CORR-DATA-1.                           NC2084.2
098100     MOVE 01 TO XYZ-5 OF CORR-DATA-1.                             NC2084.2
098200     MOVE 00 TO XYZ-6 OF CORR-DATA-1.                             NC2084.2
098300     MOVE CORRESPONDING CORR-DATA-1 TO CORR-DATA-3.               NC2084.2
098400 MOV-INIT-F1-2-1.                                                 NC2084.2
098500     MOVE "MOV-TEST-F1-2-1" TO PAR-NAME.                          NC2084.2
098600 MOV-TEST-F1-2-1.                                                 NC2084.2
098700     IF XYZ-1 OF CORR-DATA-3 EQUAL TO 03                          NC2084.2
098800            PERFORM PASS ELSE                                     NC2084.2
098900            GO TO MOV-FAIL-F1-2-1.                                NC2084.2
099000     GO TO MOV-WRITE-F1-2-1.                                      NC2084.2
099100 MOV-DELETE-F1-2-1.                                               NC2084.2
099200     PERFORM DE-LETE.                                             NC2084.2
099300     GO TO MOV-WRITE-F1-2-1.                                      NC2084.2
099400 MOV-FAIL-F1-2-1.                                                 NC2084.2
099500     PERFORM FAIL.                                                NC2084.2
099600     MOVE XYZ-1 OF CORR-DATA-3 TO COMPUTED-A.                     NC2084.2
099700     MOVE "03" TO CORRECT-A.                                      NC2084.2
099800 MOV-WRITE-F1-2-1.                                                NC2084.2
099900     PERFORM PRINT-DETAIL.                                        NC2084.2
100000*                                                                 NC2084.2
100100 MOV-INIT-F1-2-2.                                                 NC2084.2
100200     MOVE "MOV-TEST-F1-2-2" TO PAR-NAME.                          NC2084.2
100300 MOV-TEST-F1-2-2.                                                 NC2084.2
100400     IF XYZ-3 OF CORR-DATA-3 EQUAL TO 10                          NC2084.2
100500              PERFORM PASS ELSE                                   NC2084.2
100600              GO TO MOV-FAIL-F1-2-2.                              NC2084.2
100700     GO TO MOV-WRITE-F1-2-2.                                      NC2084.2
100800 MOV-DELETE-F1-2-2.                                               NC2084.2
100900     PERFORM DE-LETE.                                             NC2084.2
101000     GO TO MOV-WRITE-F1-2-2.                                      NC2084.2
101100 MOV-FAIL-F1-2-2.                                                 NC2084.2
101200     PERFORM FAIL.                                                NC2084.2
101300     MOVE XYZ-3 OF CORR-DATA-3 TO COMPUTED-A.                     NC2084.2
101400     MOVE "10" TO CORRECT-A.                                      NC2084.2
101500 MOV-WRITE-F1-2-2.                                                NC2084.2
101600     PERFORM PRINT-DETAIL.                                        NC2084.2
101700*                                                                 NC2084.2
101800 MOV-INIT-F1-3.                                                   NC2084.2
101900     MOVE "MOV-TEST-F1-3" TO PAR-NAME.                            NC2084.2
102000     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
102100     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
102200     MOVE ZERO TO CORR-DATA-5.                                    NC2084.2
102300     MOVE 123456789012 TO CORR-DATA-3.                            NC2084.2
102400     MOVE CORRESPONDING CORR-DATA-3 TO CORR-DATA-5.               NC2084.2
102500 MOV-TEST-F1-3.                                                   NC2084.2
102600     IF XYZ-1 OF CORR-DATA-5 EQUAL TO 12 NEXT                     NC2084.2
102700             SENTENCE ELSE                                        NC2084.2
102800             GO TO MOV-FAIL-F1-3.                                 NC2084.2
102900     IF XYZ-2 OF CORR-DATA-5 EQUAL TO 90 NEXT                     NC2084.2
103000             SENTENCE ELSE                                        NC2084.2
103100             GO TO MOV-FAIL-F1-3.                                 NC2084.2
103200     IF XYZ-13 OF CORR-DATA-5 EQUAL TO 0                          NC2084.2
103300             PERFORM PASS ELSE                                    NC2084.2
103400             GO TO MOV-FAIL-F1-3.                                 NC2084.2
103500     GO TO MOV-WRITE-F1-3.                                        NC2084.2
103600 MOV-DELETE-F1-3.                                                 NC2084.2
103700     PERFORM DE-LETE.                                             NC2084.2
103800     GO TO MOV-WRITE-F1-3.                                        NC2084.2
103900 MOV-FAIL-F1-3.                                                   NC2084.2
104000     MOVE CORR-DATA-5 TO COMPUTED-A.                              NC2084.2
104100     MOVE "9012000000000000" TO CORRECT-A.                        NC2084.2
104200     PERFORM FAIL.                                                NC2084.2
104300 MOV-WRITE-F1-3.                                                  NC2084.2
104400     PERFORM PRINT-DETAIL.                                        NC2084.2
104500*                                                                 NC2084.2
104600 MOV-INIT-F1-4.                                                   NC2084.2
104700     MOVE     "MOV-TEST-F1-4 " TO PAR-NAME.                       NC2084.2
104800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
104900     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
105000     MOVE     SPACE TO GRP-MOVE-CORR-R.                           NC2084.2
105100     MOVE     CORRESPONDING GRP-MOVE-CORR-1 TO GRP-MOVE-CORR-R.   NC2084.2
105200 MOV-TEST-F1-4.                                                   NC2084.2
105300     IF       GRP-MOVE-CORR-R EQUAL TO "   111222333XYZ   "       NC2084.2
105400              PERFORM PASS GO TO MOV-WRITE-F1-4.                  NC2084.2
105500     GO       TO MOVE-FAIL-F1-4.                                  NC2084.2
105600 MOV-DELETE-F1-4.                                                 NC2084.2
105700     PERFORM  DE-LETE.                                            NC2084.2
105800     GO       TO MOV-WRITE-F1-4.                                  NC2084.2
105900 MOVE-FAIL-F1-4.                                                  NC2084.2
106000     MOVE     GRP-MOVE-CORR-R TO COMPUTED-A.                      NC2084.2
106100     MOVE     "   111222333XYZ   " TO CORRECT-A.                  NC2084.2
106200     PERFORM  FAIL.                                               NC2084.2
106300 MOV-WRITE-F1-4.                                                  NC2084.2
106400     PERFORM  PRINT-DETAIL.                                       NC2084.2
106500*                                                                 NC2084.2
106600 MOV-INIT-F1-5.                                                   NC2084.2
106700     MOVE     "MOV-TEST-F1-5" TO PAR-NAME.                        NC2084.2
106800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
106900     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
107000     MOVE     SPACE TO GRP-TO-MOVE-CORR-TO.                       NC2084.2
107100     MOVE     CORRESPONDING GRP-TO-MOVE-CORR-1 TO                 NC2084.2
107200              GRP-TO-MOVE-CORR-TO.                                NC2084.2
107300 MOV-TEST-F1-5.                                                   NC2084.2
107400     IF       GRP-TO-MOVE-CORR-TO EQUAL TO "111         123ABC45" NC2084.2
107500              PERFORM PASS GO TO MOV-WRITE-F1-5.                  NC2084.2
107600     GO       TO MOVE-FAIL-F1-5.                                  NC2084.2
107700 MOV-DELETE-F1-5.                                                 NC2084.2
107800     PERFORM  DE-LETE.                                            NC2084.2
107900     GO       TO MOV-WRITE-F1-5.                                  NC2084.2
108000 MOVE-FAIL-F1-5.                                                  NC2084.2
108100     MOVE     GRP-TO-MOVE-CORR-TO TO COMPUTED-A.                  NC2084.2
108200     MOVE     "111         123ABC45" TO CORRECT-A.                NC2084.2
108300     PERFORM  FAIL.                                               NC2084.2
108400 MOV-WRITE-F1-5.                                                  NC2084.2
108500     PERFORM  PRINT-DETAIL.                                       NC2084.2
108600*                                                                 NC2084.2
108700 MOV-INIT-F1-6.                                                   NC2084.2
108800     MOVE "MOV-TEST-F1-6" TO PAR-NAME.                            NC2084.2
108900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
109000     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
109100     MOVE CORRESPONDING MOVE54 TO MOVE60.                         NC2084.2
109200 MOV-TEST-F1-6.                                                   NC2084.2
109300     IF MOVE60 EQUAL TO "XYA"                                     NC2084.2
109400            PERFORM PASS                                          NC2084.2
109500            GO TO MOV-WRITE-F1-6.                                 NC2084.2
109600     GO TO MOV-FAIL-F1-6.                                         NC2084.2
109700 MOV-DELETE-F1-6.                                                 NC2084.2
109800     PERFORM DE-LETE.                                             NC2084.2
109900     GO TO  MOV-WRITE-F1-6.                                       NC2084.2
110000 MOV-FAIL-F1-6.                                                   NC2084.2
110100     MOVE MOVE60 TO COMPUTED-A                                    NC2084.2
110200     MOVE "XYA" TO CORRECT-A                                      NC2084.2
110300     PERFORM FAIL.                                                NC2084.2
110400 MOV-WRITE-F1-6.                                                  NC2084.2
110500     PERFORM PRINT-DETAIL.                                        NC2084.2
110600*                                                                 NC2084.2
110700 MOV-INIT-F1-7.                                                   NC2084.2
110800     MOVE "MOV-TEST-F1-7" TO PAR-NAME.                            NC2084.2
110900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2084.2
111000     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2084.2
111100     MOVE "*" TO MOVE56 OF MOVE60 MOVE58 OF MOVE60 MOVE65.        NC2084.2
111200 MOV-TEST-F1-7.                                                   NC2084.2
111300     IF MOVE60 EQUAL TO "***"                                     NC2084.2
111400            PERFORM PASS                                          NC2084.2
111500            GO TO MOV-WRITE-F1-7.                                 NC2084.2
111600     GO TO MOV-FAIL-F1-7.                                         NC2084.2
111700 MOV-DELETE-F1-7.                                                 NC2084.2
111800     PERFORM DE-LETE.                                             NC2084.2
111900     GO TO MOV-WRITE-F1-7.                                        NC2084.2
112000 MOV-FAIL-F1-7.                                                   NC2084.2
112100     MOVE MOVE60 TO COMPUTED-A                                    NC2084.2
112200     MOVE "***" TO CORRECT-A                                      NC2084.2
112300     PERFORM FAIL.                                                NC2084.2
112400 MOV-WRITE-F1-7.                                                  NC2084.2
112500     PERFORM PRINT-DETAIL.                                        NC2084.2
112600     PERFORM END-ROUTINE.                                         NC2084.2
112700 NUMERIC-OPERAND-LIMITS-TESTS SECTION.                            NC2084.2
112800 CCVS-EXIT SECTION.                                               NC2084.2
112900 CCVS-999999.                                                     NC2084.2
113000     GO TO CLOSE-FILES.                                           NC2084.2
      *END-OF,NC208A                                                                  
