      *HEADER,COBOL,NC250A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2504.2
000200 PROGRAM-ID.                                                      NC2504.2
000300     NC250A.                                                      NC2504.2
000400                                                                  NC2504.2
000500****************************************************************  NC2504.2
000600*                                                              *  NC2504.2
000700*    VALIDATION FOR:-                                          *  NC2504.2
000800*                                                              *  NC2504.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2504.2
001000*                                                              *  NC2504.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2504.2
001200*                                                              *  NC2504.2
001300****************************************************************  NC2504.2
001400*                                                              *  NC2504.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2504.2
001600*                                                              *  NC2504.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2504.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2504.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2504.2
002000*                                                              *  NC2504.2
002100****************************************************************  NC2504.2
002200*                                                                 NC2504.2
002300*    PROGRAM NC250A TESTS THE GENERAL FORMAT OF THE "IF" STATEMENTNC2504.2
002400*    A VARIETY OF QUALIFIED DATA-NAMES AND CONDITION-NAMES        NC2504.2
002500*    ARE USED.                                                    NC2504.2
002600*                                                                 NC2504.2
002700                                                                  NC2504.2
002800 ENVIRONMENT DIVISION.                                            NC2504.2
002900 CONFIGURATION SECTION.                                           NC2504.2
003000 SOURCE-COMPUTER.                                                 NC2504.2
003100     XXXXX082.                                                    NC2504.2
003200 OBJECT-COMPUTER.                                                 NC2504.2
003300     XXXXX083.                                                    NC2504.2
003400 INPUT-OUTPUT SECTION.                                            NC2504.2
003500 FILE-CONTROL.                                                    NC2504.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2504.2
003700     XXXXX055.                                                    NC2504.2
003800 DATA DIVISION.                                                   NC2504.2
003900 FILE SECTION.                                                    NC2504.2
004000 FD  PRINT-FILE.                                                  NC2504.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2504.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2504.2
004300 WORKING-STORAGE SECTION.                                         NC2504.2
004400 01  WRK-DU-1V0-1 PIC 9 VALUE 1.                                  NC2504.2
004500 01  WRK-DU-1V0-2 PIC 9 VALUE 2.                                  NC2504.2
004600 01  WRK-DU-1V0-3 PIC 9 VALUE 3.                                  NC2504.2
004700 01  WRK-DU-1V0-4 PIC 9 VALUE ZERO.                               NC2504.2
004800 01  WRK-DU-2V0-1 PIC 99 VALUE 10.                                NC2504.2
004900 01  WRK-DU-2V0-2 PIC 99 VALUE 11.                                NC2504.2
005000 01  WRK-DU-2V0-3 PIC 99 VALUE 12.                                NC2504.2
005100 77  SMALL-VALU   PICTURE 99 VALUE 7.                             NC2504.2
005200 77  SMALLER-VALU PICTURE 99 VALUE 6.                             NC2504.2
005300 77  SMALLEST-VALU   PICTURE 99 VALUE 5.                          NC2504.2
005400 77  EVEN-SMALLER PICTURE 99 VALUE 1.                             NC2504.2
005500 77  WRK-DS-02V00                 PICTURE S99.                    NC2504.2
005600     88 TEST-2NUC-COND-99         VALUE 99.                       NC2504.2
005700 77  WRK-DS-06V06     PICTURE S9(6)V9(6).                         NC2504.2
005800 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC2504.2
005900                                  PICTURE S9(12).                 NC2504.2
006000 77  A02TWOS-DS-02V00             PICTURE S99    VALUE 22.        NC2504.2
006100 77  WRK-DS-01V00                 PICTURE S9.                     NC2504.2
006200 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  NC2504.2
006300 77  A990-DS-0201P                PICTURE S99P   VALUE 990.       NC2504.2
006400 77  A02ONES-DS-02V00             PICTURE S99    VALUE 11.        NC2504.2
006500 77  A01ONE-DS-P0801              PICTURE SP(8)9 VALUE .000000001.NC2504.2
006600 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         NC2504.2
006700 77  WRK-XN-00001                 PICTURE X.                      NC2504.2
006800 77  WRK-XN-00005                 PICTURE X(5).                   NC2504.2
006900 77  TWO  PICTURE 9 VALUE 2.                                      NC2504.2
007000 77  THREE PICTURE 9 VALUE 3.                                     NC2504.2
007100 77  SEVEN PICTURE 9 VALUE 7.                                     NC2504.2
007200 77  EIGHT PICTURE 9 VALUE 8.                                     NC2504.2
007300 77  NINE  PICTURE 9 VALUE 9.                                     NC2504.2
007400 77  TEN  PICTURE 99 VALUE 10.                                    NC2504.2
007500 77  TWENTY PICTURE 99 VALUE 20.                                  NC2504.2
007600 77  ALTERCOUNT PICTURE 999 VALUE ZERO.                           NC2504.2
007700 77  XRAY PICTURE IS X.                                           NC2504.2
007800 77  IF-D1 PICTURE S9(4)V9(2) VALUE 0.                            NC2504.2
007900 77  IF-D2 PICTURE S9(4)V9(2) VALUE ZERO.                         NC2504.2
008000 77  IF-D3 PICTURE X(10) VALUE "0000000000".                      NC2504.2
008100 77  IF-D4 PICTURE X(15) VALUE "               ".                 NC2504.2
008200 77  IF-D5 PICTURE X(10) VALUE ALL QUOTE.                         NC2504.2
008300 77  IF-D6 PICTURE A(10) VALUE "BABABABABA".                      NC2504.2
008400 77  IF-D7 PICTURE S9(6)V9(4) VALUE +123.45.                      NC2504.2
008500 77  IF-D8 PICTURE 9(6)V9(4) VALUE 12300.                         NC2504.2
008600 77  IF-D9 PICTURE X(3) VALUE "123".                              NC2504.2
008700 77  IF-D11 PICTURE X(6) VALUE "ABCDEF".                          NC2504.2
008800 77  IF-D13 PICTURE 9(6)V9(4) VALUE 12300.                        NC2504.2
008900 77  IF-D14 PICTURE S9(4)V9(2) VALUE +123.45.                     NC2504.2
009000 77  IF-D15 PICTURE S999PP VALUE 12300.                           NC2504.2
009100 77  IF-D16 PICTURE PP99 VALUE .0012.                             NC2504.2
009200 77  IF-D17 PICTURE SV9(4) VALUE .0012.                           NC2504.2
009300 77  IF-D18 PICTURE X(10) VALUE "BABABABABA".                     NC2504.2
009400 77  IF-D19 PICTURE X(10) VALUE "ABCDEF    ".                     NC2504.2
009500 77  IF-D23 PICTURE $9,9B9.90+.                                   NC2504.2
009600 77  IF-D24 PICTURE X(10) VALUE "$1,2 3.40+".                     NC2504.2
009700 77  IF-D25 PICTURE ABABX0A.                                      NC2504.2
009800 77  IF-D26 PICTURE X(8) VALUE "A C D0E".                         NC2504.2
009900 77  IF-D27 PICTURE IS 9(6)V9(4) VALUE IS 2137.45                 NC2504.2
010000     USAGE IS COMPUTATIONAL.                                      NC2504.2
010100 77  IF-D28 PICTURE IS 999999V9999 VALUE IS 2137.45.              NC2504.2
010200 77  IF-D31 PICTURE S9(6) VALUE -123.                             NC2504.2
010300 77  IF-D32 PICTURE S9(4)V99.                                     NC2504.2
010400     88  A  VALUE 1.                                              NC2504.2
010500     88  B VALUES ARE 2 THRU 4.                                   NC2504.2
010600     88  C VALUE IS ZERO.                                         NC2504.2
010700     88  D VALUE IS +12.34.                                       NC2504.2
010800     88  E  VALUE IS .01, .11, .21 .81.                           NC2504.2
010900     88  F  VALUE IS 100 THRU 128 1000 THRU 1280 -9 THRU -2.      NC2504.2
011000     88  G  VALUE IS 8765.43 1234 THRU 5678 5 -9999 THRU 10.      NC2504.2
011100 77  IF-D33 PICTURE X(4).                                         NC2504.2
011200     88  B   VALUE QUOTE.                                         NC2504.2
011300     88  C   VALUE SPACE.                                         NC2504.2
011400     88 D VALUE ALL "BAC".                                        NC2504.2
011500 77  IF-D34 PICTURE A(4).                                         NC2504.2
011600     88  B VALUE "A A ".                                          NC2504.2
011700 77  IF-D37 PICTURE 9(5) VALUE 12345.                             NC2504.2
011800 77  IF-D38 PICTURE X(9) VALUE "12345    ".                       NC2504.2
011900 77  CCON-1 PICTURE 99 VALUE 11.                                  NC2504.2
012000 77  CCON-2 PICTURE 99 VALUE 12.                                  NC2504.2
012100 77  CCON-3 PICTURE 99 VALUE 13.                                  NC2504.2
012200 77  COMP-SGN1  PICTURE S9(1) VALUE +9 COMPUTATIONAL.             NC2504.2
012300 77  COMP-SGN2  PICTURE S9(18) VALUE +3 COMPUTATIONAL.            NC2504.2
012400 77  COMP-SGN3  PICTURE S9(1) VALUE -5 COMPUTATIONAL.             NC2504.2
012500 77  COMP-SGN4  PICTURE S9(18) VALUE -3167598765431 COMPUTATIONAL.NC2504.2
012600 77  START-POINT        PICTURE 9(6) COMPUTATIONAL.               NC2504.2
012700 77  INC-VALUE          PICTURE 9(6) COMPUTATIONAL.               NC2504.2
012800 77  SWITCH-PFM-1 PICTURE 9 VALUE ZERO.                           NC2504.2
012900 77  SWITCH-PFM-2 PICTURE 9 VALUE ZERO.                           NC2504.2
013000 77  PFM-11-COUNTER PICTURE 999 VALUE ZERO.                       NC2504.2
013100 77  PFM-12-COUNTER  PICTURE 999 VALUE 100.                       NC2504.2
013200 77  PFM-12-ANS1  PICTURE 999 VALUE ZERO.                         NC2504.2
013300 77  PFM-12-ANS2  PICTURE 999 VALUE ZERO.                         NC2504.2
013400 01  SUBSCRIPT-6 PICTURE 99999 VALUE ZERO.                        NC2504.2
013500 01  IF-TABLE.                                                    NC2504.2
013600     02 IF-ELEM PICTURE X OCCURS 12 TIMES.                        NC2504.2
013700 01  QUOTE-DATA.                                                  NC2504.2
013800     02 QU-1 PICTURE X(3) VALUE "123".                            NC2504.2
013900     02 QU-2 PICTURE X VALUE QUOTE.                               NC2504.2
014000     02 QU-3 PICTURE X(6) VALUE "ABC456".                         NC2504.2
014100 01  IF-D10.                                                      NC2504.2
014200     02  D1 PICTURE X(2) VALUE "01".                              NC2504.2
014300     02  D2 PICTURE X(2) VALUE "23".                              NC2504.2
014400     02  D3.                                                      NC2504.2
014500     03  D4 PICTURE X(4) VALUE "4567".                            NC2504.2
014600     03 D5 PICTURE X(4) VALUE "8912".                             NC2504.2
014700 01  IF-D12.                                                      NC2504.2
014800     02  D1 PICTURE X(3) VALUE "ABC".                             NC2504.2
014900     02  D2.                                                      NC2504.2
015000     03  D3.                                                      NC2504.2
015100     04  D4      PICTURE XX     VALUE "DE".                       NC2504.2
015200     04  D5 PICTURE X VALUE "F".                                  NC2504.2
015300 01  IF-D20.                                                      NC2504.2
015400     02  FILLER    PICTURE 9(5)   VALUE ZERO.                     NC2504.2
015500     02  D1 PICTURE 9(2) VALUE 12.                                NC2504.2
015600     02  D2 PICTURE 9 VALUE 3.                                    NC2504.2
015700     02  D3 PICTURE 9(2) VALUE 45.                                NC2504.2
015800 01  IF-D21.                                                      NC2504.2
015900     02  D1 PICTURE 9(5) VALUE ZEROS.                             NC2504.2
016000     02  D2 PICTURE 9(5) VALUE 12345.                             NC2504.2
016100 01  IF-D22.                                                      NC2504.2
016200     02  D1 PICTURE A(2) VALUE "AB".                              NC2504.2
016300     02  D2 PICTURE A(4) VALUE "CDEF".                            NC2504.2
016400 01  IF-D35.                                                      NC2504.2
016500     02  AA PICTURE X(2).                                         NC2504.2
016600     88  A1 VALUE "AA".                                           NC2504.2
016700     88  A2 VALUE "AB".                                           NC2504.2
016800     02  BB PICTURE IS X(2).                                      NC2504.2
016900     88  B1 VALUE "CC".                                           NC2504.2
017000     88  B2 VALUE "CD".                                           NC2504.2
017100     02 BB-2 REDEFINES BB.                                        NC2504.2
017200     03  AAA PICTURE X.                                           NC2504.2
017300     88  AA1 VALUE "A".                                           NC2504.2
017400     88  AA2 VALUE "C".                                           NC2504.2
017500     03  BBB PICTURE X.                                           NC2504.2
017600     88  BB1    VALUE "B".                                        NC2504.2
017700     88  BB2 VALUE "D".                                           NC2504.2
017800 01  IF-D36  PICTURE X(120) VALUE IS    "ABCDEFGHIJKLMNOPQRSTUVWXYNC2504.2
017900-    "Z1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890ABCDEFGHIJKLMNC2504.2
018000-    "NOPQRSTUVWXYZ1234567890ABCDEFGHIJKL".                       NC2504.2
018100 01  IF-D40 PICTURE 9(5) VALUE 12345                              NC2504.2
018200              COMPUTATIONAL SYNCHRONIZED RIGHT.                   NC2504.2
018300     88 IF-D40A VALUE ZERO THRU 10000.                            NC2504.2
018400     88 IF-D40B VALUE 10001 THRU 99999.                           NC2504.2
018500     88 IF-D40C VALUE 99999.                                      NC2504.2
018600 01  PERFORM1  PICTURE XXX  VALUE SPACES.                         NC2504.2
018700 01  PERFORM2  PICTURE S999 VALUE 20.                             NC2504.2
018800 01  PERFORM3  PICTURE  9  VALUE  5.                              NC2504.2
018900 01  PERFORM4  PICTURE S99V9.                                     NC2504.2
019000 01  PERFORM5  PICTURE S99V9  VALUE 10.0.                         NC2504.2
019100 01  PERFORM6  PICTURE  99V9.                                     NC2504.2
019200 01  PERFORM7.                                                    NC2504.2
019300     02  PERFORM8  OCCURS 7 TIMES  PICTURE  99V9.                 NC2504.2
019400 01  PERFORM9  PICTURE 9   VALUE 3.                               NC2504.2
019500 01  PERFORM10  PICTURE  S9  VALUE -1.                            NC2504.2
019600 01  PERFORM11  PICTURE  99  VALUE 6.                             NC2504.2
019700 01  PERFORM12.                                                   NC2504.2
019800     02 PERFORM13  OCCURS 4 TIMES.                                NC2504.2
019900         03 PERFORM14  OCCURS 20 TIMES  PICTURE  99V9.            NC2504.2
020000         03 PERFORM15  OCCURS 10 TIMES.                           NC2504.2
020100             04 PERFORM16  OCCURS 5 TIMES  PICTURE 99V9.          NC2504.2
020200 01  PERFORM17          PICTURE 9(6) COMPUTATIONAL.               NC2504.2
020300 01  PERFORM18          PICTURE 9(6) COMPUTATIONAL.               NC2504.2
020400 01  PERFORM-KEY  PICTURE 9.                                      NC2504.2
020500 01  PERFORM-SEVEN-LEVEL-TABLE.                                   NC2504.2
020600   03   PFM71                OCCURS 2.                            NC2504.2
020700     05  PFM72               OCCURS 2.                            NC2504.2
020800       07  PFM73             OCCURS 2.                            NC2504.2
020900         09  PFM74           OCCURS 2.                            NC2504.2
021000           11  PFM75         OCCURS 2.                            NC2504.2
021100             13  PFM76       OCCURS 2.                            NC2504.2
021200               15  PFM77     OCCURS 2.                            NC2504.2
021300                 17  PFM77-1 PIC X.                               NC2504.2
021400 01  S1                      PIC S9(3) COMP.                      NC2504.2
021500 01  S2                      PIC S9(3) COMP.                      NC2504.2
021600 01  S3                      PIC S9(3) COMP.                      NC2504.2
021700 01  S4                      PIC S9(3) COMP.                      NC2504.2
021800 01  S5                      PIC S9(3) COMP.                      NC2504.2
021900 01  S6                      PIC S9(3) COMP.                      NC2504.2
022000 01  S7                      PIC S9(3) COMP.                      NC2504.2
022100 01  PFM-7-TOT               PIC S9(3) COMP.                      NC2504.2
022200 01  PFM-F4-24-TOT           PIC S9(3) COMP.                      NC2504.2
022300 01  PFM-A                   PIC S9(3) COMP.                      NC2504.2
022400 01  PFM-B                   PIC S9(3) COMP.                      NC2504.2
022500 01  FILLER-A.                                                    NC2504.2
022600   03  PFM-F4-25-A           PIC S9(3) COMP OCCURS 10.            NC2504.2
022700 01  FILLER-B.                                                    NC2504.2
022800   03  PFM-F4-25-B           PIC S9(3) COMP OCCURS 10.            NC2504.2
022900 01  FILLER-C.                                                    NC2504.2
023000   03  PFM-F4-25-C           PIC S9(3) COMP OCCURS 10.            NC2504.2
023100 01  RECEIVING-TABLE.                                             NC2504.2
023200     03 TBL-ELEMEN-A.                                             NC2504.2
023300         05 TBL-ELEMEN-B          PICTURE X(18).                  NC2504.2
023400         05 TBL-ELEMEN-C          PICTURE X(18).                  NC2504.2
023500     03  TBL-ELEMEN-D.                                            NC2504.2
023600         05 TBL-ELEMEN-E          PICTURE X OCCURS 36 TIMES.      NC2504.2
023700 01  LITERAL-SPLITTER.                                            NC2504.2
023800     02 PART1                     PICTURE X(20).                  NC2504.2
023900     02 PART2                     PICTURE X(20).                  NC2504.2
024000     02 PART3                     PICTURE X(20).                  NC2504.2
024100     02 PART4                     PICTURE X(20).                  NC2504.2
024200 01  LITERAL-TABLE REDEFINES LITERAL-SPLITTER.                    NC2504.2
024300     02 80PARTS                   PICTURE X      OCCURS 80 TIMES. NC2504.2
024400 01  GRP-FOR-88-LEVELS.                                           NC2504.2
024500     03 WRK-DS-02V00-COND         PICTURE 99.                     NC2504.2
024600         88 COND-1                VALUE IS 01 THRU 05.            NC2504.2
024700         88 COND-2                VALUES ARE 06 THRU 10           NC2504.2
024800                                           16 THRU 20  00.        NC2504.2
024900         88 COND-3                VALUES 11 THRU 15.              NC2504.2
025000 01  GRP-MOVE-CONSTANTS.                                          NC2504.2
025100     03 GRP-GROUP-MOVE-FROM.                                      NC2504.2
025200         04 GRP-ALPHABETIC.                                       NC2504.2
025300             05 ALPHABET-AN-00026 PICTURE A(26)                   NC2504.2
025400                        VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ".       NC2504.2
025500         04 GRP-NUMERIC.                                          NC2504.2
025600             05 DIGITS-DV-10V00   PICTURE 9(10) VALUE 0123456789. NC2504.2
025700         05 DIGITS-DU-06V04-S REDEFINES DIGITS-DV-10V00           NC2504.2
025800                                  PICTURE 9(6)V9999.              NC2504.2
025900         04 GRP-ALPHANUMERIC.                                     NC2504.2
026000             05 ALPHANUMERIC-XN-00049 PICTURE X(50)               NC2504.2
026100     VALUE  "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-><=$,;.()/* 0123456789". NC2504.2
026200             05 FILLER                PICTURE X  VALUE QUOTE.     NC2504.2
026300 01  GRP-FOR-2N058.                                               NC2504.2
026400     02 SUB-GRP-FOR-2N058-A.                                      NC2504.2
026500         03 ELEM-FOR-2N058-A PICTURE 999  VALUE ZEROES.           NC2504.2
026600         03 ELEM-FOR-2N058-B PICTURE XXX  VALUE ZEROS.            NC2504.2
026700         03 ELEM-FOR-2N058-C PICTURE XXX  VALUE SPACES.           NC2504.2
026800         03 ELEM-FOR-2N058-D PICTURE X(6) VALUE ALL "ABC".        NC2504.2
026900         03 ELEM-FOR-2N058-E PICTURE XXX  VALUE ALL "Z".          NC2504.2
027000         03 ELEM-FOR-2N058-F PICTURE XXX  VALUE ALL SPACES.       NC2504.2
027100         03 ELEM-FOR-2N058-G PICTURE XXX  VALUE ALL ZEROES.       NC2504.2
027200         03 ELEM-FOR-2N058-H PICTURE 999  VALUE ALL ZEROS.        NC2504.2
027300         03 ELEM-FOR-2N058-I PICTURE XXX  VALUE QUOTES.           NC2504.2
027400         03 ELEM-FOR-2N058-J PICTURE XXX  VALUE ALL QUOTES.       NC2504.2
027500         03 ELEM-FOR-2N058-K PICTURE XXX  VALUE ALL HIGH-VALUES.  NC2504.2
027600         03 ELEM-FOR-2N058-L PICTURE XXX  VALUE ALL LOW-VALUES.   NC2504.2
027700         03 ELEM-FOR-2N058-M PICTURE XXX  VALUE HIGH-VALUES.      NC2504.2
027800         03 ELEM-FOR-2N058-N PICTURE XXX  VALUE LOW-VALUES.       NC2504.2
027900     02 SUB-GRP-FOR-2N058-B.                                      NC2504.2
028000         03 SUB-SUB-BA.                                           NC2504.2
028100             04 ELEM-FOR-2N058-A  PICTURE 999.                    NC2504.2
028200             04 ELEM-FOR-2N058-B  PICTURE XXX.                    NC2504.2
028300             04 ELEM-FOR-2N058-C  PICTURE XXX.                    NC2504.2
028400             04 ELEM-FOR-2N058-D  PICTURE X(6).                   NC2504.2
028500         03 SUB-SUB-BB.                                           NC2504.2
028600             04 ELEM-FOR-2N058-E  PICTURE XXX.                    NC2504.2
028700             04 ELEM-FOR-2N058-F  PICTURE XXX.                    NC2504.2
028800             04 ELEM-FOR-2N058-G  PICTURE XXX.                    NC2504.2
028900             04 ELEM-FOR-2N058-H  PICTURE 999.                    NC2504.2
029000         03 SUB-SUB-BC.                                           NC2504.2
029100             04 ELEM-FOR-2N058-I  PICTURE XXX.                    NC2504.2
029200             04 ELEM-FOR-2N058-J  PICTURE XXX.                    NC2504.2
029300             04 ELEM-FOR-2N058-K  PICTURE XXX.                    NC2504.2
029400             04 ELEM-FOR-2N058-L  PICTURE XXX.                    NC2504.2
029500             04 ELEM-FOR-2N058-M  PICTURE XXX.                    NC2504.2
029600             04 ELEM-FOR-2N058-N  PICTURE XXX.                    NC2504.2
029700 01  CHARACTER-BREAKDOWN-S.                                       NC2504.2
029800     02   FIRST-20S PICTURE X(20).                                NC2504.2
029900     02  SECOND-20S PICTURE X(20).                                NC2504.2
030000     02   THIRD-20S PICTURE X(20).                                NC2504.2
030100     02  FOURTH-20S PICTURE X(20).                                NC2504.2
030200     02   FIFTH-20S PICTURE X(20).                                NC2504.2
030300     02   SIXTH-20S PICTURE X(20).                                NC2504.2
030400     02 SEVENTH-20S PICTURE X(20).                                NC2504.2
030500     02  EIGHTH-20S PICTURE X(20).                                NC2504.2
030600     02   NINTH-20S PICTURE X(20).                                NC2504.2
030700     02   TENTH-20S PICTURE X(20).                                NC2504.2
030800 01  CHARACTER-BREAKDOWN-R.                                       NC2504.2
030900     02   FIRST-20R PICTURE X(20).                                NC2504.2
031000     02  SECOND-20R PICTURE X(20).                                NC2504.2
031100     02   THIRD-20R PICTURE X(20).                                NC2504.2
031200     02  FOURTH-20R PICTURE X(20).                                NC2504.2
031300     02   FIFTH-20R PICTURE X(20).                                NC2504.2
031400     02   SIXTH-20R PICTURE X(20).                                NC2504.2
031500     02 SEVENTH-20R PICTURE X(20).                                NC2504.2
031600     02  EIGHTH-20R PICTURE X(20).                                NC2504.2
031700     02   NINTH-20R PICTURE X(20).                                NC2504.2
031800     02   TENTH-20R PICTURE X(20).                                NC2504.2
031900 01  TABLE-80.                                                    NC2504.2
032000     02  ELMT OCCURS 3 TIMES PIC 9.                               NC2504.2
032100     88  A80  VALUES ARE ZERO THRU 7.                             NC2504.2
032200     88  B80  VALUE 8.                                            NC2504.2
032300     88  C80  VALUES ARE 7, 8 THROUGH 9.                          NC2504.2
032400                                                                  NC2504.2
032500 01  TABLE-86.                                                    NC2504.2
032600     88  A86  VALUE "ABC".                                        NC2504.2
032700     88  B86  VALUE "ABCABC".                                     NC2504.2
032800     88  C86  VALUE "   ABC".                                     NC2504.2
032900     02  DATANAME-86  PIC XXX  VALUE "ABC".                       NC2504.2
033000     02  DNAME-86.                                                NC2504.2
033100         03  FILLER  PIC X  VALUE "A".                            NC2504.2
033200         03  FILLER  PIC X  VALUE "B".                            NC2504.2
033300         03  FILLER  PIC X   VALUE "C".                           NC2504.2
033400 01  FIGCON-DATA.                                                 NC2504.2
033500     02 SPACE-X         PICTURE X(10) VALUE "          ".         NC2504.2
033600     02 QUOTE-X         PICTURE X(5)  VALUE QUOTE.                NC2504.2
033700     02 LOW-VAL         PICTURE X(5)  VALUE LOW-VALUE.            NC2504.2
033800     02 ABC PICTURE XXX VALUE "ABC".                              NC2504.2
033900     02 ONE23           PICTURE 9999  VALUE 123.                  NC2504.2
034000     02 ZERO-C          PICTURE 9(10) VALUE 0 COMPUTATIONAL.      NC2504.2
034100     02 ZERO-D          PICTURE 9     VALUE ZERO USAGE DISPLAY.   NC2504.2
034200 01  TEST-RESULTS.                                                NC2504.2
034300     02 FILLER                   PIC X      VALUE SPACE.          NC2504.2
034400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2504.2
034500     02 FILLER                   PIC X      VALUE SPACE.          NC2504.2
034600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2504.2
034700     02 FILLER                   PIC X      VALUE SPACE.          NC2504.2
034800     02  PAR-NAME.                                                NC2504.2
034900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2504.2
035000       03  PARDOT-X              PIC X      VALUE SPACE.          NC2504.2
035100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2504.2
035200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2504.2
035300     02 RE-MARK                  PIC X(61).                       NC2504.2
035400 01  TEST-COMPUTED.                                               NC2504.2
035500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2504.2
035600     02 FILLER                   PIC X(17)  VALUE                 NC2504.2
035700            "       COMPUTED=".                                   NC2504.2
035800     02 COMPUTED-X.                                               NC2504.2
035900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2504.2
036000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2504.2
036100                                 PIC -9(9).9(9).                  NC2504.2
036200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2504.2
036300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2504.2
036400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2504.2
036500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2504.2
036600         04 COMPUTED-18V0                    PIC -9(18).          NC2504.2
036700         04 FILLER                           PIC X.               NC2504.2
036800     03 FILLER PIC X(50) VALUE SPACE.                             NC2504.2
036900 01  TEST-CORRECT.                                                NC2504.2
037000     02 FILLER PIC X(30) VALUE SPACE.                             NC2504.2
037100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2504.2
037200     02 CORRECT-X.                                                NC2504.2
037300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2504.2
037400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2504.2
037500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2504.2
037600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2504.2
037700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2504.2
037800     03      CR-18V0 REDEFINES CORRECT-A.                         NC2504.2
037900         04 CORRECT-18V0                     PIC -9(18).          NC2504.2
038000         04 FILLER                           PIC X.               NC2504.2
038100     03 FILLER PIC X(2) VALUE SPACE.                              NC2504.2
038200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2504.2
038300 01  CCVS-C-1.                                                    NC2504.2
038400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2504.2
038500-    "SS  PARAGRAPH-NAME                                          NC2504.2
038600-    "       REMARKS".                                            NC2504.2
038700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2504.2
038800 01  CCVS-C-2.                                                    NC2504.2
038900     02 FILLER                     PIC X        VALUE SPACE.      NC2504.2
039000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2504.2
039100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2504.2
039200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2504.2
039300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2504.2
039400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2504.2
039500 01  REC-CT                        PIC 99       VALUE ZERO.       NC2504.2
039600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2504.2
039700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2504.2
039800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2504.2
039900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2504.2
040000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2504.2
040100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2504.2
040200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2504.2
040300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2504.2
040400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2504.2
040500 01  CCVS-H-1.                                                    NC2504.2
040600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2504.2
040700     02  FILLER                    PIC X(42)    VALUE             NC2504.2
040800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2504.2
040900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2504.2
041000 01  CCVS-H-2A.                                                   NC2504.2
041100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2504.2
041200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2504.2
041300   02  FILLER                        PIC XXXX   VALUE             NC2504.2
041400     "4.2 ".                                                      NC2504.2
041500   02  FILLER                        PIC X(28)  VALUE             NC2504.2
041600            " COPY - NOT FOR DISTRIBUTION".                       NC2504.2
041700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2504.2
041800                                                                  NC2504.2
041900 01  CCVS-H-2B.                                                   NC2504.2
042000   02  FILLER                        PIC X(15)  VALUE             NC2504.2
042100            "TEST RESULT OF ".                                    NC2504.2
042200   02  TEST-ID                       PIC X(9).                    NC2504.2
042300   02  FILLER                        PIC X(4)   VALUE             NC2504.2
042400            " IN ".                                               NC2504.2
042500   02  FILLER                        PIC X(12)  VALUE             NC2504.2
042600     " HIGH       ".                                              NC2504.2
042700   02  FILLER                        PIC X(22)  VALUE             NC2504.2
042800            " LEVEL VALIDATION FOR ".                             NC2504.2
042900   02  FILLER                        PIC X(58)  VALUE             NC2504.2
043000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2504.2
043100 01  CCVS-H-3.                                                    NC2504.2
043200     02  FILLER                      PIC X(34)  VALUE             NC2504.2
043300            " FOR OFFICIAL USE ONLY    ".                         NC2504.2
043400     02  FILLER                      PIC X(58)  VALUE             NC2504.2
043500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2504.2
043600     02  FILLER                      PIC X(28)  VALUE             NC2504.2
043700            "  COPYRIGHT   1985 ".                                NC2504.2
043800 01  CCVS-E-1.                                                    NC2504.2
043900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2504.2
044000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2504.2
044100     02 ID-AGAIN                     PIC X(9).                    NC2504.2
044200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2504.2
044300 01  CCVS-E-2.                                                    NC2504.2
044400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2504.2
044500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2504.2
044600     02 CCVS-E-2-2.                                               NC2504.2
044700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2504.2
044800         03 FILLER                   PIC X      VALUE SPACE.      NC2504.2
044900         03 ENDER-DESC               PIC X(44)  VALUE             NC2504.2
045000            "ERRORS ENCOUNTERED".                                 NC2504.2
045100 01  CCVS-E-3.                                                    NC2504.2
045200     02  FILLER                      PIC X(22)  VALUE             NC2504.2
045300            " FOR OFFICIAL USE ONLY".                             NC2504.2
045400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2504.2
045500     02  FILLER                      PIC X(58)  VALUE             NC2504.2
045600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2504.2
045700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2504.2
045800     02 FILLER                       PIC X(15)  VALUE             NC2504.2
045900             " COPYRIGHT 1985".                                   NC2504.2
046000 01  CCVS-E-4.                                                    NC2504.2
046100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2504.2
046200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2504.2
046300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2504.2
046400     02 FILLER                       PIC X(40)  VALUE             NC2504.2
046500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2504.2
046600 01  XXINFO.                                                      NC2504.2
046700     02 FILLER                       PIC X(19)  VALUE             NC2504.2
046800            "*** INFORMATION ***".                                NC2504.2
046900     02 INFO-TEXT.                                                NC2504.2
047000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2504.2
047100       04 XXCOMPUTED                 PIC X(20).                   NC2504.2
047200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2504.2
047300       04 XXCORRECT                  PIC X(20).                   NC2504.2
047400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2504.2
047500 01  HYPHEN-LINE.                                                 NC2504.2
047600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2504.2
047700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2504.2
047800-    "*****************************************".                 NC2504.2
047900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2504.2
048000-    "******************************".                            NC2504.2
048100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2504.2
048200     "NC250A".                                                    NC2504.2
048300 PROCEDURE DIVISION.                                              NC2504.2
048400 CCVS1 SECTION.                                                   NC2504.2
048500 OPEN-FILES.                                                      NC2504.2
048600     OPEN     OUTPUT PRINT-FILE.                                  NC2504.2
048700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2504.2
048800     MOVE    SPACE TO TEST-RESULTS.                               NC2504.2
048900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2504.2
049000     GO TO CCVS1-EXIT.                                            NC2504.2
049100 CLOSE-FILES.                                                     NC2504.2
049200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2504.2
049300 TERMINATE-CCVS.                                                  NC2504.2
049400     STOP     RUN.                                                NC2504.2
049500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2504.2
049600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2504.2
049700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2504.2
049800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2504.2
049900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2504.2
050000 PRINT-DETAIL.                                                    NC2504.2
050100     IF REC-CT NOT EQUAL TO ZERO                                  NC2504.2
050200             MOVE "." TO PARDOT-X                                 NC2504.2
050300             MOVE REC-CT TO DOTVALUE.                             NC2504.2
050400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2504.2
050500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2504.2
050600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2504.2
050700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2504.2
050800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2504.2
050900     MOVE SPACE TO CORRECT-X.                                     NC2504.2
051000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2504.2
051100     MOVE     SPACE TO RE-MARK.                                   NC2504.2
051200 HEAD-ROUTINE.                                                    NC2504.2
051300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2504.2
051400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2504.2
051500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2504.2
051600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2504.2
051700 COLUMN-NAMES-ROUTINE.                                            NC2504.2
051800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2504.2
051900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2504.2
052000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2504.2
052100 END-ROUTINE.                                                     NC2504.2
052200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2504.2
052300 END-RTN-EXIT.                                                    NC2504.2
052400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2504.2
052500 END-ROUTINE-1.                                                   NC2504.2
052600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2504.2
052700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2504.2
052800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2504.2
052900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2504.2
053000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2504.2
053100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2504.2
053200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2504.2
053300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2504.2
053400  END-ROUTINE-12.                                                 NC2504.2
053500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2504.2
053600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2504.2
053700         MOVE "NO " TO ERROR-TOTAL                                NC2504.2
053800         ELSE                                                     NC2504.2
053900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2504.2
054000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2504.2
054100     PERFORM WRITE-LINE.                                          NC2504.2
054200 END-ROUTINE-13.                                                  NC2504.2
054300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2504.2
054400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2504.2
054500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2504.2
054600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2504.2
054700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2504.2
054800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2504.2
054900          MOVE "NO " TO ERROR-TOTAL                               NC2504.2
055000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2504.2
055100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2504.2
055200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2504.2
055300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2504.2
055400 WRITE-LINE.                                                      NC2504.2
055500     ADD 1 TO RECORD-COUNT.                                       NC2504.2
055600*    IF RECORD-COUNT GREATER 50                                   NC2504.2
055700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2504.2
055800*        MOVE SPACE TO DUMMY-RECORD                               NC2504.2
055900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2504.2
056000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2504.2
056100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2504.2
056200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2504.2
056300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2504.2
056400*        MOVE ZERO TO RECORD-COUNT.                               NC2504.2
056500     PERFORM WRT-LN.                                              NC2504.2
056600 WRT-LN.                                                          NC2504.2
056700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2504.2
056800     MOVE SPACE TO DUMMY-RECORD.                                  NC2504.2
056900 BLANK-LINE-PRINT.                                                NC2504.2
057000     PERFORM WRT-LN.                                              NC2504.2
057100 FAIL-ROUTINE.                                                    NC2504.2
057200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2504.2
057300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2504.2
057400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2504.2
057500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2504.2
057600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2504.2
057700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2504.2
057800     GO TO  FAIL-ROUTINE-EX.                                      NC2504.2
057900 FAIL-ROUTINE-WRITE.                                              NC2504.2
058000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2504.2
058100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2504.2
058200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2504.2
058300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2504.2
058400 FAIL-ROUTINE-EX. EXIT.                                           NC2504.2
058500 BAIL-OUT.                                                        NC2504.2
058600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2504.2
058700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2504.2
058800 BAIL-OUT-WRITE.                                                  NC2504.2
058900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2504.2
059000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2504.2
059100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2504.2
059200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2504.2
059300 BAIL-OUT-EX. EXIT.                                               NC2504.2
059400 CCVS1-EXIT.                                                      NC2504.2
059500     EXIT.                                                        NC2504.2
059600 SECT-NC201A-001 SECTION.                                         NC2504.2
059700*                                                                 NC2504.2
059800 IF--INIT-A.                                                      NC2504.2
059900     MOVE   "VI-89 6.15" TO ANSI-REFERENCE.                       NC2504.2
060000     PERFORM END-ROUTINE.                                         NC2504.2
060100     MOVE SPACE TO TEST-RESULTS.                                  NC2504.2
060200     MOVE "THE FOLLOWING TESTS        " TO RE-MARK.               NC2504.2
060300     PERFORM PRINT-DETAIL.                                        NC2504.2
060400     MOVE "COMPARE FIGURATIVE         " TO RE-MARK.               NC2504.2
060500     PERFORM PRINT-DETAIL.                                        NC2504.2
060600     MOVE "CONSTANTS, SIGN OF DATA,   " TO RE-MARK.               NC2504.2
060700     PERFORM PRINT-DETAIL.                                        NC2504.2
060800     MOVE "AND CONDITION-NAMES        " TO RE-MARK.               NC2504.2
060900     PERFORM PRINT-DETAIL.                                        NC2504.2
061000     MOVE "IN VARYING COMBINATIONS.   " TO RE-MARK.               NC2504.2
061100     PERFORM PRINT-DETAIL.                                        NC2504.2
061200     MOVE "COMPARE--           " TO FEATURE.                      NC2504.2
061300     PERFORM PRINT-DETAIL.                                        NC2504.2
061400     MOVE "  FIG. CONSTANTS    " TO FEATURE.                      NC2504.2
061500 IF--TEST-1.                                                      NC2504.2
061600     IF ZEROES IS EQUAL TO IF-D3 PERFORM PASS ELSE PERFORM FAIL.  NC2504.2
061700*        NOTE FIGURATIVE ZEROES VS ALPHANUMERIC FIELD.            NC2504.2
061800     GO TO IF--WRITE-1.                                           NC2504.2
061900 IF--DELETE-1.                                                    NC2504.2
062000     PERFORM DE-LETE.                                             NC2504.2
062100 IF--WRITE-1.                                                     NC2504.2
062200     MOVE "IF--TEST-1 " TO PAR-NAME.                              NC2504.2
062300     PERFORM PRINT-DETAIL.                                        NC2504.2
062400 IF--TEST-2.                                                      NC2504.2
062500     IF SPACES EQUAL TO IF-D4 PERFORM PASS ELSE PERFORM FAIL.     NC2504.2
062600*        NOTE FIGURATIVE SPACES VS ALPHANUMERIC FIELD.            NC2504.2
062700     GO TO IF--WRITE-2.                                           NC2504.2
062800 IF--DELETE-2.                                                    NC2504.2
062900     PERFORM DE-LETE.                                             NC2504.2
063000 IF--WRITE-2.                                                     NC2504.2
063100     MOVE "IF--TEST-2 " TO PAR-NAME.                              NC2504.2
063200     PERFORM PRINT-DETAIL.                                        NC2504.2
063300 IF--TEST-3.                                                      NC2504.2
063400     IF QUOTES EQUAL TO IF-D5 PERFORM PASS ELSE PERFORM FAIL.     NC2504.2
063500*        NOTE FIGURATIVE QUOTES VS ALPHANUMERIC FIELD.            NC2504.2
063600     GO TO IF--WRITE-3.                                           NC2504.2
063700 IF--DELETE-3.                                                    NC2504.2
063800     PERFORM DE-LETE.                                             NC2504.2
063900 IF--WRITE-3.                                                     NC2504.2
064000     MOVE "IF--TEST-3 " TO PAR-NAME.                              NC2504.2
064100     PERFORM PRINT-DETAIL.                                        NC2504.2
064200 IF--TEST-4.                                                      NC2504.2
064300     IF IF-D6 EQUAL TO ALL "BA" PERFORM PASS ELSE PERFORM FAIL.   NC2504.2
064400*        NOTE ALL ANY LITERAL VS ALPHANUMERIC FIELD.              NC2504.2
064500     GO TO IF--WRITE-4.                                           NC2504.2
064600 IF--DELETE-4.                                                    NC2504.2
064700     PERFORM DE-LETE.                                             NC2504.2
064800 IF--WRITE-4.                                                     NC2504.2
064900     MOVE "IF--TEST-4 " TO PAR-NAME.                              NC2504.2
065000     PERFORM PRINT-DETAIL.                                        NC2504.2
065100 IF--TEST-5.                                                      NC2504.2
065200     IF IF-D4 GREATER THAN SPACES PERFORM FAIL ELSE               NC2504.2
065300     PERFORM PASS.                                                NC2504.2
065400*        NOTE FIG-SPACES VS ALPHANUMERIC FIELD.                   NC2504.2
065500     GO TO IF--WRITE-5.                                           NC2504.2
065600 IF--DELETE-5.                                                    NC2504.2
065700     PERFORM DE-LETE.                                             NC2504.2
065800 IF--WRITE-5.                                                     NC2504.2
065900     MOVE "IF--TEST-5 " TO PAR-NAME.                              NC2504.2
066000     PERFORM PRINT-DETAIL.                                        NC2504.2
066100 IF--TEST-6.                                                      NC2504.2
066200     IF QUOTES GREATER THAN IF-D5 PERFORM FAIL ELSE PERFORM PASS. NC2504.2
066300*        NOTE FIG-QUOTES VS ALPHANUMERIC FIELD.                   NC2504.2
066400     GO TO IF--WRITE-6.                                           NC2504.2
066500 IF--DELETE-6.                                                    NC2504.2
066600     PERFORM DE-LETE.                                             NC2504.2
066700 IF--WRITE-6.                                                     NC2504.2
066800     MOVE "IF--TEST-6 " TO PAR-NAME.                              NC2504.2
066900     PERFORM PRINT-DETAIL.                                        NC2504.2
067000 IF--TEST-7.                                                      NC2504.2
067100     IF ALL "BA" GREATER THAN IF-D6 PERFORM FAIL                  NC2504.2
067200     ELSE PERFORM PASS.                                           NC2504.2
067300*    NOTE ALL ANY LITERAL VS ALPHA FIELD.                         NC2504.2
067400     GO TO IF--WRITE-7.                                           NC2504.2
067500 IF--DELETE-7.                                                    NC2504.2
067600     PERFORM DE-LETE.                                             NC2504.2
067700 IF--WRITE-7.                                                     NC2504.2
067800     MOVE "IF--TEST-7 " TO PAR-NAME.                              NC2504.2
067900     PERFORM PRINT-DETAIL.                                        NC2504.2
068000 IF--INIT-B.                                                      NC2504.2
068100     MOVE "  UNEQUAL LENGTHS   " TO FEATURE.                      NC2504.2
068200 IF--TEST-8.                                                      NC2504.2
068300     IF IF-D22 GREATER THAN IF-D19 PERFORM FAIL ELSE PERFORM PASS.NC2504.2
068400*        NOTE ALPHANUMERIC GROUP VS ALPHANUMERIC FIELD.           NC2504.2
068500*    NOTE UNEQUAL LENGTHS.                                        NC2504.2
068600     GO TO IF--WRITE-8.                                           NC2504.2
068700 IF--DELETE-8.                                                    NC2504.2
068800     PERFORM DE-LETE.                                             NC2504.2
068900 IF--WRITE-8.                                                     NC2504.2
069000     MOVE "IF--TEST-8 " TO PAR-NAME.                              NC2504.2
069100     PERFORM PRINT-DETAIL.                                        NC2504.2
069200 IF--INIT-C.                                                      NC2504.2
069300     MOVE "  POSITIVE          " TO FEATURE.                      NC2504.2
069400 IF--TEST-9.                                                      NC2504.2
069500     IF IF-D1 IS NOT POSITIVE PERFORM PASS ELSE PERFORM FAIL.     NC2504.2
069600*        NOTE POSITIVE TEST ON ZERO VALUE.                        NC2504.2
069700     GO TO IF--WRITE-9.                                           NC2504.2
069800 IF--DELETE-9.                                                    NC2504.2
069900     PERFORM DE-LETE.                                             NC2504.2
070000 IF--WRITE-9.                                                     NC2504.2
070100     MOVE "IF--TEST-9 " TO PAR-NAME.                              NC2504.2
070200     PERFORM PRINT-DETAIL.                                        NC2504.2
070300 IF--TEST-10.                                                     NC2504.2
070400     IF IF-D8 POSITIVE PERFORM PASS ELSE      PERFORM FAIL.       NC2504.2
070500*        NOTE POSITIVE TEST ON UNSIGNED VALUE.                    NC2504.2
070600     GO TO IF--WRITE-10.                                          NC2504.2
070700 IF--DELETE-10.                                                   NC2504.2
070800     PERFORM DE-LETE.                                             NC2504.2
070900 IF--WRITE-10.                                                    NC2504.2
071000     MOVE "IF--TEST-10" TO PAR-NAME.                              NC2504.2
071100     PERFORM PRINT-DETAIL.                                        NC2504.2
071200 IF--TEST-11.                                                     NC2504.2
071300     IF IF-D16 POSITIVE PERFORM PASS ELSE PERFORM FAIL.           NC2504.2
071400*        NOTE POSITIVE TEST ON SCALED VALUE.                      NC2504.2
071500     GO TO IF--WRITE-11.                                          NC2504.2
071600 IF--DELETE-11.                                                   NC2504.2
071700     PERFORM DE-LETE.                                             NC2504.2
071800 IF--WRITE-11.                                                    NC2504.2
071900     MOVE "IF--TEST-11" TO PAR-NAME.                              NC2504.2
072000     PERFORM PRINT-DETAIL.                                        NC2504.2
072100 IF--TEST-12.                                                     NC2504.2
072200     IF IF-D27 POSITIVE PERFORM PASS ELSE PERFORM FAIL.           NC2504.2
072300*        NOTE POSITIVE TEST ON COMPUTATIONAL FIELD.               NC2504.2
072400     GO TO IF--WRITE-12.                                          NC2504.2
072500 IF--DELETE-12.                                                   NC2504.2
072600     PERFORM DE-LETE.                                             NC2504.2
072700 IF--WRITE-12.                                                    NC2504.2
072800     MOVE "IF--TEST-12" TO PAR-NAME.                              NC2504.2
072900     PERFORM PRINT-DETAIL.                                        NC2504.2
073000 IF--TEST-13.                                                     NC2504.2
073100     IF IF-D28 POSITIVE PERFORM PASS ELSE PERFORM FAIL.           NC2504.2
073200*        NOTE POSITIVE TEST ON NUMERIC DISPLAY IFELD.             NC2504.2
073300     GO TO IF--WRITE-13.                                          NC2504.2
073400 IF--DELETE-13.                                                   NC2504.2
073500     PERFORM DE-LETE.                                             NC2504.2
073600 IF--WRITE-13.                                                    NC2504.2
073700     MOVE "IF--TEST-13" TO PAR-NAME.                              NC2504.2
073800     PERFORM PRINT-DETAIL.                                        NC2504.2
073900 IF--TEST-14.                                                     NC2504.2
074000     IF IF-D31 IS POSITIVE PERFORM FAIL ELSE PERFORM PASS.        NC2504.2
074100*        NOTE POSITIVE TEST ON NEGATIVE FIELD.                    NC2504.2
074200     GO TO IF--WRITE-14.                                          NC2504.2
074300 IF--DELETE-14.                                                   NC2504.2
074400     PERFORM DE-LETE.                                             NC2504.2
074500 IF--WRITE-14.                                                    NC2504.2
074600     MOVE "IF--TEST-14" TO PAR-NAME.                              NC2504.2
074700     PERFORM PRINT-DETAIL.                                        NC2504.2
074800 IF--TEST-15.                                                     NC2504.2
074900     IF IF-D31 IS NOT POSITIVE PERFORM PASS ELSE PERFORM FAIL.    NC2504.2
075000*        NOTE NOT POSITIVE TEST ON NEGATIVE VALUE.                NC2504.2
075100     GO TO IF--WRITE-15.                                          NC2504.2
075200 IF--DELETE-15.                                                   NC2504.2
075300     PERFORM DE-LETE.                                             NC2504.2
075400 IF--WRITE-15.                                                    NC2504.2
075500     MOVE "IF--TEST-15" TO PAR-NAME.                              NC2504.2
075600     PERFORM PRINT-DETAIL.                                        NC2504.2
075700 IF--TEST-16.                                                     NC2504.2
075800     IF IF-D28 IS NOT POSITIVE PERFORM FAIL ELSE PERFORM PASS.    NC2504.2
075900*        NOTE NOT POSITIVE TEST ON UNSIGNED FIELD.                NC2504.2
076000     GO TO IF--WRITE-16.                                          NC2504.2
076100 IF--DELETE-16.                                                   NC2504.2
076200     PERFORM DE-LETE.                                             NC2504.2
076300 IF--WRITE-16.                                                    NC2504.2
076400     MOVE "IF--TEST-16" TO PAR-NAME.                              NC2504.2
076500     PERFORM PRINT-DETAIL.                                        NC2504.2
076600 IF--INIT-D.                                                      NC2504.2
076700     MOVE "  NEGATIVE          " TO FEATURE.                      NC2504.2
076800 IF--TEST-17.                                                     NC2504.2
076900     IF IF-D31 IS NEGATIVE PERFORM PASS ELSE PERFORM FAIL.        NC2504.2
077000*        NOTE NEGATIVE TEST ON NEGATIVE VALUE.                    NC2504.2
077100     GO TO IF--WRITE-17.                                          NC2504.2
077200 IF--DELETE-17.                                                   NC2504.2
077300     PERFORM DE-LETE.                                             NC2504.2
077400 IF--WRITE-17.                                                    NC2504.2
077500     MOVE "IF--TEST-17" TO PAR-NAME.                              NC2504.2
077600     PERFORM PRINT-DETAIL.                                        NC2504.2
077700 IF--TEST-18.                                                     NC2504.2
077800     IF IF-D31 IS NOT NEGATIVE PERFORM FAIL ELSE PERFORM PASS.    NC2504.2
077900*        NOTE NOT NEGATIVE TEST ON NEGATIVE VALUE.                NC2504.2
078000     GO TO IF--WRITE-18.                                          NC2504.2
078100 IF--DELETE-18.                                                   NC2504.2
078200     PERFORM DE-LETE.                                             NC2504.2
078300 IF--WRITE-18.                                                    NC2504.2
078400     MOVE "IF--TEST-18" TO PAR-NAME.                              NC2504.2
078500     PERFORM PRINT-DETAIL.                                        NC2504.2
078600 IF--TEST-19.                                                     NC2504.2
078700     IF IF-D16 NOT NEGATIVE PERFORM PASS ELSE PERFORM FAIL.       NC2504.2
078800*        NOTE NOT NEGATIVE TEST ON UNSIGNED FIELD.                NC2504.2
078900     GO TO IF--WRITE-19.                                          NC2504.2
079000 IF--DELETE-19.                                                   NC2504.2
079100     PERFORM DE-LETE.                                             NC2504.2
079200 IF--WRITE-19.                                                    NC2504.2
079300     MOVE "IF--TEST-19" TO PAR-NAME.                              NC2504.2
079400     PERFORM PRINT-DETAIL.                                        NC2504.2
079500 IF--INIT-E.                                                      NC2504.2
079600     MOVE "  ZERO              " TO FEATURE.                      NC2504.2
079700 IF--TEST-20.                                                     NC2504.2
079800     IF IF-D1 IS ZERO PERFORM PASS ELSE PERFORM FAIL.             NC2504.2
079900*        NOTE ZERO TEST ON ZERO VALUE.                            NC2504.2
080000     GO TO IF--WRITE-20.                                          NC2504.2
080100 IF--DELETE-20.                                                   NC2504.2
080200     PERFORM DE-LETE.                                             NC2504.2
080300 IF--WRITE-20.                                                    NC2504.2
080400     MOVE "IF--TEST-20" TO PAR-NAME.                              NC2504.2
080500     PERFORM PRINT-DETAIL.                                        NC2504.2
080600 IF--TEST-21.                                                     NC2504.2
080700     IF IF-D10 NOT EQUAL TO ZERO                                  NC2504.2
080800         PERFORM PASS  ELSE                                       NC2504.2
080900     MOVE IF-D10 TO COMPUTED-A                                    NC2504.2
081000     MOVE ZERO TO CORRECT-N                                       NC2504.2
081100     PERFORM FAIL.                                                NC2504.2
081200*    NOTE NOT EQUAL TO ZERO TEST ON NON-ZERO VALUE.               NC2504.2
081300     GO TO IF--WRITE-21.                                          NC2504.2
081400 IF--DELETE-21.                                                   NC2504.2
081500     PERFORM DE-LETE.                                             NC2504.2
081600 IF--WRITE-21.                                                    NC2504.2
081700     MOVE "IF--TEST-21" TO PAR-NAME.                              NC2504.2
081800     PERFORM PRINT-DETAIL.                                        NC2504.2
081900 IF--INIT-F.                                                      NC2504.2
082000     MOVE "  CONDITION-NAMES   " TO FEATURE.                      NC2504.2
082100 IF--TEST-22.                                                     NC2504.2
082200     MOVE 1 TO IF-D32. IF A OF IF-D32 PERFORM PASS                NC2504.2
082300     ELSE PERFORM FAIL.                                           NC2504.2
082400*        NOTE TEST OF SIGNED NUMERIC FIELD FOR SINGLE VALUE.      NC2504.2
082500     GO TO IF--WRITE-22.                                          NC2504.2
082600 IF--DELETE-22.                                                   NC2504.2
082700     PERFORM DE-LETE.                                             NC2504.2
082800 IF--WRITE-22.                                                    NC2504.2
082900     MOVE "IF--TEST-22" TO PAR-NAME.                              NC2504.2
083000     PERFORM PRINT-DETAIL.                                        NC2504.2
083100 IF--TEST-23.                                                     NC2504.2
083200     MOVE 3 TO IF-D32. IF B OF IF-D32 PERFORM PASS                NC2504.2
083300     ELSE PERFORM FAIL.                                           NC2504.2
083400*        NOTE TEST OF SIGNED NUMERIC FIELD FOR MULTIPLE VALUES.   NC2504.2
083500     GO TO IF--WRITE-23.                                          NC2504.2
083600 IF--DELETE-23.                                                   NC2504.2
083700     PERFORM DE-LETE.                                             NC2504.2
083800 IF--WRITE-23.                                                    NC2504.2
083900     MOVE "IF--TEST-23" TO PAR-NAME.                              NC2504.2
084000     PERFORM PRINT-DETAIL.                                        NC2504.2
084100 IF--TEST-24.                                                     NC2504.2
084200             MOVE ZERO   TO IF-D32. IF C OF IF-D32 PERFORM PASS   NC2504.2
084300     ELSE PERFORM FAIL.                                           NC2504.2
084400*        NOTE TEST OF SIGNED NUMERIC FIELD FOR FIG-ZERO.          NC2504.2
084500     GO TO IF--WRITE-24.                                          NC2504.2
084600 IF--DELETE-24.                                                   NC2504.2
084700     PERFORM DE-LETE.                                             NC2504.2
084800 IF--WRITE-24.                                                    NC2504.2
084900     MOVE "IF--TEST-24" TO PAR-NAME.                              NC2504.2
085000     PERFORM PRINT-DETAIL.                                        NC2504.2
085100 IF--TEST-25.                                                     NC2504.2
085200     MOVE +12.34 TO IF-D32.                                       NC2504.2
085300     IF D OF IF-D32 PERFORM PASS ELSE PERFORM FAIL.               NC2504.2
085400*    NOTE SIGNED CONDITION-NAME.                                  NC2504.2
085500     GO TO IF--WRITE-25.                                          NC2504.2
085600 IF--DELETE-25.                                                   NC2504.2
085700     PERFORM DE-LETE.                                             NC2504.2
085800 IF--WRITE-25.                                                    NC2504.2
085900     MOVE "IF--TEST-25" TO PAR-NAME.                              NC2504.2
086000     PERFORM PRINT-DETAIL.                                        NC2504.2
086100 IF--TEST-26.                                                     NC2504.2
086200     MOVE QUOTE  TO IF-D33. IF B OF IF-D33 AND NOT B OF IF-D32    NC2504.2
086300     PERFORM PASS ELSE PERFORM FAIL.                              NC2504.2
086400*        NOTE TEST OF ALPHANUMERIC FIELD FOR FIG-QUOTES.          NC2504.2
086500     GO TO IF--WRITE-26.                                          NC2504.2
086600 IF--DELETE-26.                                                   NC2504.2
086700     PERFORM DE-LETE.                                             NC2504.2
086800 IF--WRITE-26.                                                    NC2504.2
086900     MOVE "IF--TEST-26" TO PAR-NAME.                              NC2504.2
087000     PERFORM PRINT-DETAIL.                                        NC2504.2
087100 IF--TEST-27.                                                     NC2504.2
087200     MOVE SPACE TO IF-D33. IF C OF IF-D33 PERFORM PASS            NC2504.2
087300     ELSE PERFORM FAIL.                                           NC2504.2
087400*        NOTE TEST OF ALPHANUMERIC FIELD FOR FIG-SPACES.          NC2504.2
087500     GO TO IF--WRITE-27.                                          NC2504.2
087600 IF--DELETE-27.                                                   NC2504.2
087700     PERFORM DE-LETE.                                             NC2504.2
087800 IF--WRITE-27.                                                    NC2504.2
087900     MOVE "IF--TEST-27" TO PAR-NAME.                              NC2504.2
088000     PERFORM PRINT-DETAIL.                                        NC2504.2
088100 IF--TEST-28.                                                     NC2504.2
088200     MOVE "BACB" TO IF-D33. IF D OF IF-D33 PERFORM PASS           NC2504.2
088300     ELSE PERFORM FAIL.                                           NC2504.2
088400*        NOTE TEST OF ALPHANUMERIC FIELD FOR ALL ANY LITERAL.     NC2504.2
088500     GO TO IF--WRITE-28.                                          NC2504.2
088600 IF--DELETE-28.                                                   NC2504.2
088700     PERFORM DE-LETE.                                             NC2504.2
088800 IF--WRITE-28.                                                    NC2504.2
088900     MOVE "IF--TEST-28" TO PAR-NAME.                              NC2504.2
089000     PERFORM PRINT-DETAIL.                                        NC2504.2
089100 IF--TEST-29.                                                     NC2504.2
089200     IF NOT B OF IF-D34 PERFORM PASS ELSE PERFORM FAIL.           NC2504.2
089300     GO TO IF--WRITE-29.                                          NC2504.2
089400 IF--DELETE-29.                                                   NC2504.2
089500     PERFORM DE-LETE.                                             NC2504.2
089600 IF--WRITE-29.                                                    NC2504.2
089700     MOVE "IF--TEST-29" TO PAR-NAME.                              NC2504.2
089800     PERFORM PRINT-DETAIL.                                        NC2504.2
089900 IF--TEST-30.                                                     NC2504.2
090000     MOVE "ABCD" TO IF-D35.                                       NC2504.2
090100     IF A2 AND B2 PERFORM PASS ELSE PERFORM FAIL.                 NC2504.2
090200     GO TO IF--WRITE-30.                                          NC2504.2
090300 IF--DELETE-30.                                                   NC2504.2
090400     PERFORM DE-LETE.                                             NC2504.2
090500 IF--WRITE-30.                                                    NC2504.2
090600     MOVE "IF--TEST-30" TO PAR-NAME.                              NC2504.2
090700     PERFORM PRINT-DETAIL.                                        NC2504.2
090800 IF--TEST-31.                                                     NC2504.2
090900     MOVE .21 TO IF-D32.                                          NC2504.2
091000     IF E PERFORM PASS ELSE PERFORM FAIL.                         NC2504.2
091100*    NOTE TESTS VALUE SERIES.                                     NC2504.2
091200     GO TO IF--WRITE-31.                                          NC2504.2
091300 IF--DELETE-31.                                                   NC2504.2
091400     PERFORM DE-LETE.                                             NC2504.2
091500 IF--WRITE-31.                                                    NC2504.2
091600     MOVE "IF--TEST-31" TO PAR-NAME.                              NC2504.2
091700     PERFORM PRINT-DETAIL.                                        NC2504.2
091800 IF--TEST-32.                                                     NC2504.2
091900     MOVE 1279.99 TO IF-D32.                                      NC2504.2
092000     IF F PERFORM PASS ELSE PERFORM FAIL.                         NC2504.2
092100*    NOTE TESTS VALUE RANGE SERIES.                               NC2504.2
092200     GO TO IF--WRITE-32.                                          NC2504.2
092300 IF--DELETE-32.                                                   NC2504.2
092400     PERFORM DE-LETE.                                             NC2504.2
092500 IF--WRITE-32.                                                    NC2504.2
092600     MOVE "IF--TEST-32" TO PAR-NAME.                              NC2504.2
092700     PERFORM PRINT-DETAIL.                                        NC2504.2
092800 IF--TEST-33.                                                     NC2504.2
092900     MOVE -4321.88 TO IF-D32.                                     NC2504.2
093000     IF G PERFORM PASS ELSE PERFORM FAIL.                         NC2504.2
093100*    NOTE TESTS VALUE SERIES RANGE SERIES.                        NC2504.2
093200     GO TO IF--WRITE-33.                                          NC2504.2
093300 IF--DELETE-33.                                                   NC2504.2
093400     PERFORM DE-LETE.                                             NC2504.2
093500 IF--WRITE-33.                                                    NC2504.2
093600     MOVE "IF--TEST-33" TO PAR-NAME.                              NC2504.2
093700     PERFORM PRINT-DETAIL.                                        NC2504.2
093800 IF--INIT-G.                                                      NC2504.2
093900     PERFORM END-ROUTINE.                                         NC2504.2
094000     MOVE SPACES TO FEATURE.                                      NC2504.2
094100     MOVE "THE FOLLOWING TESTS USE ARITHMETIC-EXPRESSIONS"        NC2504.2
094200     TO RE-MARK.                                                  NC2504.2
094300     PERFORM PRINT-DETAIL.                                        NC2504.2
094400     MOVE "IN RELATION OR SIGN CONDITIONS."                       NC2504.2
094500     TO RE-MARK.                                                  NC2504.2
094600     PERFORM PRINT-DETAIL.                                        NC2504.2
094700     MOVE "  EQUAL             " TO FEATURE.                      NC2504.2
094800 IF--TEST-34.                                                     NC2504.2
094900     IF  1 + (TWO * 3) EQUAL TO (TWO * 3) + 1                     NC2504.2
095000               PERFORM PASS                                       NC2504.2
095100     ELSE                                                         NC2504.2
095200               PERFORM FAIL.                                      NC2504.2
095300     GO TO IF--WRITE-34.                                          NC2504.2
095400 IF--DELETE-34.                                                   NC2504.2
095500     PERFORM DE-LETE.                                             NC2504.2
095600 IF--WRITE-34.                                                    NC2504.2
095700     MOVE "IF--TEST-34" TO PAR-NAME.                              NC2504.2
095800     PERFORM PRINT-DETAIL.                                        NC2504.2
095900 IF--TEST-35.                                                     NC2504.2
096000     IF 9 + TWO + 2 * 3 EQUAL TO 2 * 3 + TWO + 9                  NC2504.2
096100         PERFORM PASS                                             NC2504.2
096200     ELSE                                                         NC2504.2
096300         PERFORM FAIL.                                            NC2504.2
096400     GO TO IF--WRITE-35.                                          NC2504.2
096500 IF--DELETE-35.                                                   NC2504.2
096600     PERFORM DE-LETE.                                             NC2504.2
096700 IF--WRITE-35.                                                    NC2504.2
096800     MOVE "IF--TEST-35" TO PAR-NAME.                              NC2504.2
096900     PERFORM PRINT-DETAIL.                                        NC2504.2
097000 IF--TEST-36.                                                     NC2504.2
097100     IF  NINE ** 2 EQUAL TO 9 ** 2                                NC2504.2
097200              PERFORM PASS                                        NC2504.2
097300     ELSE                                                         NC2504.2
097400              PERFORM FAIL.                                       NC2504.2
097500     GO TO IF--WRITE-36.                                          NC2504.2
097600 IF--DELETE-36.                                                   NC2504.2
097700     PERFORM DE-LETE.                                             NC2504.2
097800 IF--WRITE-36.                                                    NC2504.2
097900     MOVE "IF--TEST-36" TO PAR-NAME.                              NC2504.2
098000     PERFORM PRINT-DETAIL.                                        NC2504.2
098100 IF--TEST-37.                                                     NC2504.2
098200     IF 100 + (TWENTY + 3.4) + .05 EQUAL TO                       NC2504.2
098300        .05 + (100 + TWENTY) + 3.4                                NC2504.2
098400            PERFORM PASS                                          NC2504.2
098500     ELSE                                                         NC2504.2
098600            PERFORM FAIL.                                         NC2504.2
098700     GO TO IF--WRITE-37.                                          NC2504.2
098800 IF--DELETE-37.                                                   NC2504.2
098900     PERFORM DE-LETE.                                             NC2504.2
099000 IF--WRITE-37.                                                    NC2504.2
099100     MOVE "IF--TEST-37" TO PAR-NAME.                              NC2504.2
099200     PERFORM PRINT-DETAIL.                                        NC2504.2
099300 IF--INIT-H.                                                      NC2504.2
099400     MOVE "  GREATER           " TO FEATURE.                      NC2504.2
099500 IF--TEST-38.                                                     NC2504.2
099600     IF NINE * 8 IS GREATER THAN  9 * 7 + 8 PERFORM PASS          NC2504.2
099700     ELSE PERFORM FAIL.                                           NC2504.2
099800     GO TO IF--WRITE-38.                                          NC2504.2
099900 IF--DELETE-38.                                                   NC2504.2
100000     PERFORM DE-LETE.                                             NC2504.2
100100 IF--WRITE-38.                                                    NC2504.2
100200     MOVE "IF--TEST-38" TO PAR-NAME.                              NC2504.2
100300     PERFORM PRINT-DETAIL.                                        NC2504.2
100400 IF--TEST-39.                                                     NC2504.2
100500     IF  10 ** 2 + 25 GREATER THAN IF-D14 PERFORM PASS ELSE       NC2504.2
100600     PERFORM FAIL.                                                NC2504.2
100700     GO TO IF--WRITE-39.                                          NC2504.2
100800 IF--DELETE-39.                                                   NC2504.2
100900     PERFORM DE-LETE.                                             NC2504.2
101000 IF--WRITE-39.                                                    NC2504.2
101100     MOVE "IF--TEST-39" TO PAR-NAME.                              NC2504.2
101200     PERFORM PRINT-DETAIL.                                        NC2504.2
101300 IF--TEST-40.                                                     NC2504.2
101400     IF 1000 GREATER THAN TEN ** 3 - 1 PERFORM PASS ELSE PERFORM  NC2504.2
101500     FAIL.                                                        NC2504.2
101600     GO TO IF--WRITE-40.                                          NC2504.2
101700 IF--DELETE-40.                                                   NC2504.2
101800     PERFORM DE-LETE.                                             NC2504.2
101900 IF--WRITE-40.                                                    NC2504.2
102000     MOVE "IF--TEST-40" TO PAR-NAME.                              NC2504.2
102100     PERFORM PRINT-DETAIL.                                        NC2504.2
102200 IF--INIT-I.                                                      NC2504.2
102300     MOVE "  LESS              " TO FEATURE.                      NC2504.2
102400 IF--TEST-41.                                                     NC2504.2
102500     IF 1000 LESS THAN 10 ** THREE + 1 PERFORM PASS ELSE          NC2504.2
102600     PERFORM FAIL.                                                NC2504.2
102700     GO TO IF--WRITE-41.                                          NC2504.2
102800 IF--DELETE-41.                                                   NC2504.2
102900     PERFORM DE-LETE.                                             NC2504.2
103000 IF--WRITE-41.                                                    NC2504.2
103100     MOVE "IF--TEST-41" TO PAR-NAME.                              NC2504.2
103200     PERFORM PRINT-DETAIL.                                        NC2504.2
103300 IF--TEST-42.                                                     NC2504.2
103400     IF 10 ** 2 + 20 LESS THAN IF-D14 PERFORM PASS ELSE           NC2504.2
103500     PERFORM FAIL.                                                NC2504.2
103600     GO TO IF--WRITE-42.                                          NC2504.2
103700 IF--DELETE-42.                                                   NC2504.2
103800     PERFORM DE-LETE.                                             NC2504.2
103900 IF--WRITE-42.                                                    NC2504.2
104000     MOVE "IF--TEST-42" TO PAR-NAME.                              NC2504.2
104100     PERFORM PRINT-DETAIL.                                        NC2504.2
104200 IF--TEST-43.                                                     NC2504.2
104300     IF 9 * 8 LESS THAN 9 * 7 + TEN PERFORM PASS ELSE PERFORM     NC2504.2
104400     FAIL.                                                        NC2504.2
104500     GO TO IF--WRITE-43.                                          NC2504.2
104600 IF--DELETE-43.                                                   NC2504.2
104700     PERFORM DE-LETE.                                             NC2504.2
104800 IF--WRITE-43.                                                    NC2504.2
104900     MOVE "IF--TEST-43" TO PAR-NAME.                              NC2504.2
105000     PERFORM PRINT-DETAIL.                                        NC2504.2
105100 IF--TEST-44-45.                                                  NC2504.2
105200     MOVE SPACES TO TEST-RESULTS.                                 NC2504.2
105300     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
105400     MOVE "IF--TEST-44" TO PAR-NAME.                              NC2504.2
105500     PERFORM PRINT-DETAIL.                                        NC2504.2
105600     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
105700     MOVE "IF--TEST-45" TO PAR-NAME.                              NC2504.2
105800     PERFORM PRINT-DETAIL.                                        NC2504.2
105900 IF--INIT-J.                                                      NC2504.2
106000     MOVE "  NOT EQUAL          " TO FEATURE.                     NC2504.2
106100     PERFORM PRINT-DETAIL.                                        NC2504.2
106200 IF--TEST-46.                                                     NC2504.2
106300     IF NINE * 9 - 7 * SEVEN NOT EQUAL - (SEVEN * 7) + 9 * NINE   NC2504.2
106400                PERFORM FAIL                                      NC2504.2
106500     ELSE                                                         NC2504.2
106600                PERFORM PASS.                                     NC2504.2
106700                                                                  NC2504.2
106800     GO TO IF--WRITE-46.                                          NC2504.2
106900 IF--DELETE-46.                                                   NC2504.2
107000     PERFORM DE-LETE.                                             NC2504.2
107100 IF--WRITE-46.                                                    NC2504.2
107200     MOVE "IF--TEST-46" TO PAR-NAME.                              NC2504.2
107300     PERFORM PRINT-DETAIL.                                        NC2504.2
107400 IF--TEST-47.                                                     NC2504.2
107500     IF IF-D14 - IF-D7 NOT EQUAL - IF-D7 + IF-D14                 NC2504.2
107600     PERFORM FAIL ELSE PERFORM PASS.                              NC2504.2
107700     GO TO IF--WRITE-47.                                          NC2504.2
107800 IF--DELETE-47.                                                   NC2504.2
107900     PERFORM DE-LETE.                                             NC2504.2
108000 IF--WRITE-47.                                                    NC2504.2
108100     MOVE "IF--TEST-47" TO PAR-NAME.                              NC2504.2
108200     PERFORM PRINT-DETAIL.                                        NC2504.2
108300 IF--INIT-K.                                                      NC2504.2
108400     MOVE "  NOT GREATER       " TO FEATURE.                      NC2504.2
108500 IF--TEST-48.                                                     NC2504.2
108600     IF NINE * 8 IS NOT GREATER THAN 9 * SEVEN + 8 THEN           NC2504.2
108700                   PERFORM FAIL                                   NC2504.2
108800     ELSE                                                         NC2504.2
108900                   PERFORM PASS.                                  NC2504.2
109000     GO TO IF--WRITE-48.                                          NC2504.2
109100 IF--DELETE-48.                                                   NC2504.2
109200     PERFORM DE-LETE.                                             NC2504.2
109300 IF--WRITE-48.                                                    NC2504.2
109400     MOVE "IF--TEST-48" TO PAR-NAME.                              NC2504.2
109500     PERFORM PRINT-DETAIL.                                        NC2504.2
109600 IF--TEST-49.                                                     NC2504.2
109700     IF 10 ** 2 + 25 NOT GREATER THAN IF-D14 PERFORM FAIL ELSE    NC2504.2
109800     PERFORM PASS.                                                NC2504.2
109900     GO TO IF--WRITE-49.                                          NC2504.2
110000 IF--DELETE-49.                                                   NC2504.2
110100     PERFORM DE-LETE.                                             NC2504.2
110200 IF--WRITE-49.                                                    NC2504.2
110300     MOVE "IF--TEST-49" TO PAR-NAME.                              NC2504.2
110400     PERFORM PRINT-DETAIL.                                        NC2504.2
110500 IF--TEST-50.                                                     NC2504.2
110600     IF 1000 NOT GREATER THAN 10 ** THREE - 1 PERFORM FAIL ELSE   NC2504.2
110700     PERFORM PASS.                                                NC2504.2
110800     GO TO IF--WRITE-50.                                          NC2504.2
110900 IF--DELETE-50.                                                   NC2504.2
111000     PERFORM DE-LETE.                                             NC2504.2
111100 IF--WRITE-50.                                                    NC2504.2
111200     MOVE "IF--TEST-50" TO PAR-NAME.                              NC2504.2
111300     PERFORM PRINT-DETAIL.                                        NC2504.2
111400 IF--INIT-L.                                                      NC2504.2
111500     MOVE "  NOT LESS          " TO FEATURE.                      NC2504.2
111600 IF--TEST-51.                                                     NC2504.2
111700     IF 1000 NOT LESS THAN TEN ** 3 + 1 PERFORM FAIL ELSE         NC2504.2
111800     PERFORM PASS.                                                NC2504.2
111900     GO TO IF--WRITE-51.                                          NC2504.2
112000 IF--DELETE-51.                                                   NC2504.2
112100     PERFORM DE-LETE.                                             NC2504.2
112200 IF--WRITE-51.                                                    NC2504.2
112300     MOVE "IF--TEST-51" TO PAR-NAME.                              NC2504.2
112400     PERFORM PRINT-DETAIL.                                        NC2504.2
112500 IF--TEST-52.                                                     NC2504.2
112600     IF 10 ** 2 + 20 NOT LESS THAN IF-D14 PERFORM FAIL ELSE       NC2504.2
112700     PERFORM PASS.                                                NC2504.2
112800     GO TO IF--WRITE-52.                                          NC2504.2
112900 IF--DELETE-52.                                                   NC2504.2
113000     PERFORM DE-LETE.                                             NC2504.2
113100 IF--WRITE-52.                                                    NC2504.2
113200     MOVE "IF--TEST-52" TO PAR-NAME.                              NC2504.2
113300     PERFORM PRINT-DETAIL.                                        NC2504.2
113400 IF--TEST-53.                                                     NC2504.2
113500     IF NINE * 8 NOT LESS THAN 9 * 7 + TEN PERFORM FAIL ELSE      NC2504.2
113600     PERFORM PASS.                                                NC2504.2
113700     GO TO IF--WRITE-53.                                          NC2504.2
113800 IF--DELETE-53.                                                   NC2504.2
113900     PERFORM DE-LETE.                                             NC2504.2
114000 IF--WRITE-53.                                                    NC2504.2
114100     MOVE "IF--TEST-53" TO PAR-NAME.                              NC2504.2
114200     PERFORM PRINT-DETAIL.                                        NC2504.2
114300 IF--INIT-M.                                                      NC2504.2
114400     MOVE "  POS, NEG, ZERO    " TO FEATURE.                      NC2504.2
114500 IF--TEST-54.                                                     NC2504.2
114600     IF 9 ** TWO + (180 - 90) IS NOT POSITIVE PERFORM FAIL ELSE   NC2504.2
114700     PERFORM PASS.                                                NC2504.2
114800     GO TO IF--WRITE-54.                                          NC2504.2
114900 IF--DELETE-54.                                                   NC2504.2
115000     PERFORM DE-LETE.                                             NC2504.2
115100 IF--WRITE-54.                                                    NC2504.2
115200     MOVE "IF--TEST-54" TO PAR-NAME.                              NC2504.2
115300     PERFORM PRINT-DETAIL.                                        NC2504.2
115400 IF--TEST-55.                                                     NC2504.2
115500     IF NINE ** 2 + (90 - 180) IS POSITIVE PERFORM FAIL ELSE      NC2504.2
115600     PERFORM PASS.                                                NC2504.2
115700     GO TO IF--WRITE-55.                                          NC2504.2
115800 IF--DELETE-55.                                                   NC2504.2
115900     PERFORM DE-LETE.                                             NC2504.2
116000 IF--WRITE-55.                                                    NC2504.2
116100     MOVE "IF--TEST-55" TO PAR-NAME.                              NC2504.2
116200     PERFORM PRINT-DETAIL.                                        NC2504.2
116300 IF--TEST-56.                                                     NC2504.2
116400     IF 8 * EIGHT - 8 * 8 NOT ZERO                                NC2504.2
116500             PERFORM FAIL ELSE PERFORM PASS.                      NC2504.2
116600     GO TO IF--WRITE-56.                                          NC2504.2
116700 IF--DELETE-56.                                                   NC2504.2
116800     PERFORM DE-LETE.                                             NC2504.2
116900 IF--WRITE-56.                                                    NC2504.2
117000     MOVE "IF--TEST-56" TO PAR-NAME.                              NC2504.2
117100     PERFORM PRINT-DETAIL.                                        NC2504.2
117200 IF--TEST-57-58.                                                  NC2504.2
117300     MOVE SPACES TO TEST-RESULTS.                                 NC2504.2
117400     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
117500     MOVE "IF--TEST-57" TO PAR-NAME.                              NC2504.2
117600     PERFORM PRINT-DETAIL.                                        NC2504.2
117700     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
117800     MOVE "IF--TEST-58" TO PAR-NAME.                              NC2504.2
117900     PERFORM PRINT-DETAIL.                                        NC2504.2
118000     MOVE "  POS, NEG, ZERO     " TO FEATURE.                     NC2504.2
118100 IF--TEST-59.                                                     NC2504.2
118200     IF 10 ** THREE + 99 - (1500 - 400) IS NEGATIVE PERFORM PASS  NC2504.2
118300     ELSE PERFORM FAIL.                                           NC2504.2
118400     GO TO IF--WRITE-59.                                          NC2504.2
118500 IF--DELETE-59.                                                   NC2504.2
118600     PERFORM DE-LETE.                                             NC2504.2
118700 IF--WRITE-59.                                                    NC2504.2
118800     MOVE "IF--TEST-59" TO PAR-NAME.                              NC2504.2
118900     PERFORM PRINT-DETAIL.                                        NC2504.2
119000 IF--TEST-60.                                                     NC2504.2
119100     IF TEN ** 3 + 99 - (1500 - 400) IS NOT POSITIVE PERFORM PASS NC2504.2
119200     ELSE PERFORM FAIL.                                           NC2504.2
119300     GO TO IF--WRITE-60.                                          NC2504.2
119400 IF--DELETE-60.                                                   NC2504.2
119500     PERFORM DE-LETE.                                             NC2504.2
119600 IF--WRITE-60.                                                    NC2504.2
119700     MOVE "IF--TEST-60" TO PAR-NAME.                              NC2504.2
119800     PERFORM PRINT-DETAIL.                                        NC2504.2
119900 IF--TEST-61.                                                     NC2504.2
120000     IF 8 * EIGHT - 8 * 8 IS ZERO                                 NC2504.2
120100             PERFORM PASS ELSE PERFORM FAIL.                      NC2504.2
120200     GO TO IF--WRITE-61.                                          NC2504.2
120300 IF--DELETE-61.                                                   NC2504.2
120400     PERFORM DE-LETE.                                             NC2504.2
120500 IF--WRITE-61.                                                    NC2504.2
120600     MOVE "IF--TEST-61" TO PAR-NAME.                              NC2504.2
120700     PERFORM PRINT-DETAIL.                                        NC2504.2
120800 IF--TEST-62.                                                     NC2504.2
120900     MOVE SPACES TO TEST-RESULTS.                                 NC2504.2
121000     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
121100     MOVE "IF--TEST-62" TO PAR-NAME.                              NC2504.2
121200     PERFORM PRINT-DETAIL.                                        NC2504.2
121300     MOVE "POS, NEG, ZERO      " TO FEATURE.                      NC2504.2
121400 IF--TEST-63.                                                     NC2504.2
121500     IF 10 ** THREE + 99 - (1500 - 400) IS NOT NEGATIVE           NC2504.2
121600              PERFORM FAIL ELSE PERFORM PASS.                     NC2504.2
121700     GO TO IF--WRITE-63.                                          NC2504.2
121800 IF--DELETE-63.                                                   NC2504.2
121900     PERFORM DE-LETE.                                             NC2504.2
122000 IF--WRITE-63.                                                    NC2504.2
122100     MOVE "IF--TEST-63" TO PAR-NAME.                              NC2504.2
122200     PERFORM PRINT-DETAIL.                                        NC2504.2
122300 IF--INIT-N.                                                      NC2504.2
122400     MOVE "  SYMBOLS > < =     " TO FEATURE.                      NC2504.2
122500 IF--TEST-64.                                                     NC2504.2
122600     IF TEN * 10 - 10 * 10 = - TEN * 10 + 10 * 10                 NC2504.2
122700               PERFORM PASS                                       NC2504.2
122800     ELSE                                                         NC2504.2
122900               PERFORM FAIL.                                      NC2504.2
123000     GO TO IF--WRITE-64.                                          NC2504.2
123100 IF--DELETE-64.                                                   NC2504.2
123200     PERFORM DE-LETE.                                             NC2504.2
123300 IF--WRITE-64.                                                    NC2504.2
123400     MOVE "IF--TEST-64" TO PAR-NAME.                              NC2504.2
123500     PERFORM PRINT-DETAIL.                                        NC2504.2
123600 IF--TEST-65.                                                     NC2504.2
123700     IF NINE * 8 > 9 * 7 + 8 PERFORM PASS ELSE PERFORM FAIL.      NC2504.2
123800     GO TO IF--WRITE-65.                                          NC2504.2
123900 IF--DELETE-65.                                                   NC2504.2
124000     PERFORM DE-LETE.                                             NC2504.2
124100 IF--WRITE-65.                                                    NC2504.2
124200     MOVE "IF--TEST-65" TO PAR-NAME.                              NC2504.2
124300     PERFORM PRINT-DETAIL.                                        NC2504.2
124400 IF--TEST-66.                                                     NC2504.2
124500     IF 1000 < 10 ** THREE + 1 PERFORM PASS ELSE PERFORM FAIL.    NC2504.2
124600     GO TO IF--WRITE-66.                                          NC2504.2
124700 IF--DELETE-66.                                                   NC2504.2
124800     PERFORM DE-LETE.                                             NC2504.2
124900 IF--WRITE-66.                                                    NC2504.2
125000     MOVE "IF--TEST-66" TO PAR-NAME.                              NC2504.2
125100     PERFORM PRINT-DETAIL.                                        NC2504.2
125200 IF--TEST-67.                                                     NC2504.2
125300     IF 100 + TWENTY + 3.4 + .05 NOT = 100 + TWENTY + 3.4 + 0.6   NC2504.2
125400          PERFORM PASS                                            NC2504.2
125500     ELSE                                                         NC2504.2
125600          PERFORM FAIL.                                           NC2504.2
125700     GO TO IF--WRITE-67.                                          NC2504.2
125800 IF--DELETE-67.                                                   NC2504.2
125900     PERFORM DE-LETE.                                             NC2504.2
126000 IF--WRITE-67.                                                    NC2504.2
126100     MOVE "IF--TEST-67" TO PAR-NAME.                              NC2504.2
126200     PERFORM PRINT-DETAIL.                                        NC2504.2
126300 IF--TEST-68.                                                     NC2504.2
126400     IF NINE * 8 NOT > 9 * 7 + 8 PERFORM FAIL ELSE PERFORM PASS.  NC2504.2
126500     GO TO IF--WRITE-68.                                          NC2504.2
126600 IF--DELETE-68.                                                   NC2504.2
126700     PERFORM DE-LETE.                                             NC2504.2
126800 IF--WRITE-68.                                                    NC2504.2
126900     MOVE "IF--TEST-68" TO PAR-NAME.                              NC2504.2
127000     PERFORM PRINT-DETAIL.                                        NC2504.2
127100 IF--TEST-69.                                                     NC2504.2
127200     IF 1000 NOT < 10 ** THREE + 1 PERFORM FAIL ELSE PERFORM PASS.NC2504.2
127300     GO TO IF--WRITE-69.                                          NC2504.2
127400 IF--DELETE-69.                                                   NC2504.2
127500     PERFORM DE-LETE.                                             NC2504.2
127600 IF--WRITE-69.                                                    NC2504.2
127700     MOVE "IF--TEST-69" TO PAR-NAME.                              NC2504.2
127800     PERFORM PRINT-DETAIL.                                        NC2504.2
127900 IF--TEST-70.                                                     NC2504.2
128000     MOVE SPACES TO TEST-RESULTS.                                 NC2504.2
128100     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
128200     MOVE "IF--TEST-70" TO PAR-NAME.                              NC2504.2
128300     PERFORM PRINT-DETAIL.                                        NC2504.2
128400 IF--INIT-N1.                                                     NC2504.2
128500     PERFORM END-ROUTINE.                                         NC2504.2
128600     MOVE SPACES TO FEATURE.                                      NC2504.2
128700     MOVE "THE FOLLOWING TESTS COMBINATIONS OF"                   NC2504.2
128800             TO RE-MARK.                                          NC2504.2
128900     PERFORM PRINT-DETAIL.                                        NC2504.2
129000     MOVE "RELATIONAL AND SIZE ERROR CONDITIONS."                 NC2504.2
129100             TO RE-MARK.                                          NC2504.2
129200     PERFORM PRINT-DETAIL.                                        NC2504.2
129300 IF--TEST-71.                                                     NC2504.2
129400     MOVE     "X" TO WRK-XN-00001.                                NC2504.2
129500     MOVE     ZERO TO WRK-DS-01V00.                               NC2504.2
129600     IF       WRK-XN-00001 IS EQUAL TO "X"                        NC2504.2
129700              MOVE "Z" TO WRK-XN-00001                            NC2504.2
129800              ADD 1 TO WRK-DS-01V00 ON SIZE ERROR                 NC2504.2
129900              MOVE "Y" TO WRK-XN-00001                            NC2504.2
130000              ELSE                                                NC2504.2
130100              ADD 2 TO WRK-DS-01V00 ON SIZE ERROR                 NC2504.2
130200              MOVE "W" TO WRK-XN-00001.                           NC2504.2
130300     IF  WRK-XN-00001 EQUAL TO "Z" AND                            NC2504.2
130400              WRK-DS-01V00 EQUAL TO 1                             NC2504.2
130500              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
130600*    NOTE     COMBINATION OF RELATIONAL AND SIZE ERROR CONDITIONS.NC2504.2
130700     GO       TO IF--WRITE-71.                                    NC2504.2
130800 IF--DELETE-71.                                                   NC2504.2
130900     PERFORM  DE-LETE.                                            NC2504.2
131000 IF--WRITE-71.                                                    NC2504.2
131100     MOVE     "  INCL SIZE ERROR" TO FEATURE.                     NC2504.2
131200     MOVE     "IF--TEST-71" TO PAR-NAME.                          NC2504.2
131300     PERFORM  PRINT-DETAIL.                                       NC2504.2
131400 IF--INIT-O.                                                      NC2504.2
131500     MOVE     "  UNEQUAL LENGTHS" TO FEATURE.                     NC2504.2
131600 IF--TEST-73.                                                     NC2504.2
131700     MOVE     "X" TO WRK-XN-00001.                                NC2504.2
131800     MOVE     "X    " TO WRK-XN-00005.                            NC2504.2
131900     IF       WRK-XN-00001 IS EQUAL TO WRK-XN-00005               NC2504.2
132000              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
132100*    NOTE     EQUAL QUANTITIES IN UNEQUAL LENGTH FIELDS.          NC2504.2
132200     GO       TO IF--WRITE-73.                                    NC2504.2
132300 IF--DELETE-73.                                                   NC2504.2
132400     PERFORM  DE-LETE.                                            NC2504.2
132500 IF--WRITE-73.                                                    NC2504.2
132600     MOVE     "IF--TEST-73" TO PAR-NAME.                          NC2504.2
132700     PERFORM  PRINT-DETAIL.                                       NC2504.2
132800 IF--TEST-74.                                                     NC2504.2
132900     MOVE     "X" TO WRK-XN-00001.                                NC2504.2
133000     MOVE     "Y    " TO WRK-XN-00005.                            NC2504.2
133100     IF       WRK-XN-00001 IS NOT EQUAL TO WRK-XN-00005           NC2504.2
133200              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
133300*    NOTE     UNEQUAL QUANTITIES IN UNEQUAL LENGTH FIELDS.        NC2504.2
133400     GO       TO IF--WRITE-74.                                    NC2504.2
133500 IF--DELETE-74.                                                   NC2504.2
133600     PERFORM  DE-LETE.                                            NC2504.2
133700 IF--WRITE-74.                                                    NC2504.2
133800     MOVE     "IF--TEST-74" TO PAR-NAME.                          NC2504.2
133900     PERFORM  PRINT-DETAIL.                                       NC2504.2
134000 IF--TEST-75.                                                     NC2504.2
134100     MOVE     "X" TO WRK-XN-00001.                                NC2504.2
134200     MOVE     "X   X" TO WRK-XN-00005.                            NC2504.2
134300     IF       WRK-XN-00001 IS NOT EQUAL TO WRK-XN-00005           NC2504.2
134400              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
134500*    NOTE     UNEQUAL QUANTITIES IN UNEQUAL LENGTH FIELDS.        NC2504.2
134600     GO       TO IF--WRITE-75.                                    NC2504.2
134700 IF--DELETE-75.                                                   NC2504.2
134800     PERFORM  DE-LETE.                                            NC2504.2
134900 IF--WRITE-75.                                                    NC2504.2
135000     MOVE     "IF--TEST-75" TO PAR-NAME.                          NC2504.2
135100     PERFORM PRINT-DETAIL.                                        NC2504.2
135200 IF--INIT-P.                                                      NC2504.2
135300     MOVE     "  UNEQUAL LENGTHS" TO FEATURE.                     NC2504.2
135400 IF--TEST-77.                                                     NC2504.2
135500     IF       IF-D37 NOT EQUAL TO IF-D21                          NC2504.2
135600              PERFORM PASS GO TO IF--WRITE-77.                    NC2504.2
135700*    NOTE     NUMERIC VS GROUP COMPARISON, UNEQUAL LENGTHS.       NC2504.2
135800     GO       TO IF--FAIL-77.                                     NC2504.2
135900 IF--DELETE-77.                                                   NC2504.2
136000     PERFORM DE-LETE.                                             NC2504.2
136100     GO       TO IF--WRITE-11.                                    NC2504.2
136200 IF--FAIL-77.                                                     NC2504.2
136300     PERFORM  FAIL.                                               NC2504.2
136400     MOVE     "IF-D37 SHOULD PAD ON RIGHT" TO RE-MARK.            NC2504.2
136500 IF--WRITE-77.                                                    NC2504.2
136600     MOVE     "IF--TEST-77" TO PAR-NAME.                          NC2504.2
136700     PERFORM  PRINT-DETAIL.                                       NC2504.2
136800 IF--TEST-78.                                                     NC2504.2
136900     IF       IF-D37 EQUAL TO IF-D38                              NC2504.2
137000              PERFORM PASS GO TO IF--WRITE-78.                    NC2504.2
137100*    NOTE     NUMERIC VS ALPHANUMERIC COMPARISON, UNEQUAL LENGTHS.NC2504.2
137200     GO       TO IF--FAIL-78.                                     NC2504.2
137300 IF--DELETE-78.                                                   NC2504.2
137400     PERFORM  DE-LETE.                                            NC2504.2
137500     GO       TO IF--WRITE-78.                                    NC2504.2
137600 IF--FAIL-78.                                                     NC2504.2
137700     PERFORM  FAIL.                                               NC2504.2
137800     MOVE     "IF-D37 SHOULD PAD ON RIGHT" TO RE-MARK.            NC2504.2
137900 IF--WRITE-78.                                                    NC2504.2
138000     MOVE     "IF--TEST-78" TO PAR-NAME.                          NC2504.2
138100     PERFORM  PRINT-DETAIL.                                       NC2504.2
138200 IF--TEST-79.                                                     NC2504.2
138300     MOVE ZERO TO IF-D10.                                         NC2504.2
138400     IF D3 OF IF-D10 EQUAL TO "00000000"                          NC2504.2
138500         PERFORM PASS                                             NC2504.2
138600         GO TO IF-WRITE-79.                                       NC2504.2
138700     MOVE D3 IN IF-D10 TO COMPUTED-A.                             NC2504.2
138800     MOVE "00000000" TO CORRECT-A.                                NC2504.2
138900     PERFORM FAIL.                                                NC2504.2
139000     GO TO IF-WRITE-79.                                           NC2504.2
139100 IF-DELETE-79.                                                    NC2504.2
139200     PERFORM DE-LETE.                                             NC2504.2
139300 IF-WRITE-79.                                                     NC2504.2
139400     MOVE "QUALIFIED GROUP   " TO FEATURE.                        NC2504.2
139500     MOVE "IF--TEST-79 " TO PAR-NAME.                             NC2504.2
139600     PERFORM PRINT-DETAIL.                                        NC2504.2
139700 IF--INIT-80.                                                     NC2504.2
139800     PERFORM END-ROUTINE.                                         NC2504.2
139900     MOVE SPACES TO FEATURE.                                      NC2504.2
140000     MOVE "THESE SPECIAL CONDITION-   " TO RE-MARK.               NC2504.2
140100     PERFORM PRINT-DETAIL.                                        NC2504.2
140200     MOVE "NAME TESTS VERIFY THE    " TO RE-MARK.                 NC2504.2
140300     PERFORM PRINT-DETAIL.                                        NC2504.2
140400     MOVE "ABILITY OF THE COMPILER TO " TO RE-MARK.               NC2504.2
140500     PERFORM PRINT-DETAIL.                                        NC2504.2
140600     MOVE "ACCEPT SUBSCRIPTED 88 LEVEL" TO RE-MARK.               NC2504.2
140700     PERFORM PRINT-DETAIL.                                        NC2504.2
140800*    NOTE *******                  ******                *********NC2504.2
140900*        *****  A NOTE AS THE FIRST STATEMENT IN THIS ******      NC2504.2
141000*               PARAGRAPH WILL BYPASS ALL THE SPECIAL *****       NC2504.2
141100*               CONDITION-NAME TESTS, BUT A NOTE STATEMENT        NC2504.2
141200*               MIGHT NEED TO BE INSERTED IN EACH TEST            NC2504.2
141300*               SO THE SYNTAX WOULD BE IGNORED BY THE COMPILER.   NC2504.2
141400     MOVE "OCCURS WITH 88 LEVEL" TO FEATURE.                      NC2504.2
141500     MOVE 123 TO TABLE-80.                                        NC2504.2
141600     GO TO IF--TEST-80.                                           NC2504.2
141700 IF-DELETE-80.                                                    NC2504.2
141800     PERFORM DE-LETE.                                             NC2504.2
141900     MOVE "IF--TEST-80" TO PAR-NAME.                              NC2504.2
142000     MOVE "TEST-80 THRU 85 DELETED  " TO RE-MARK.                 NC2504.2
142100     PERFORM PRINT-DETAIL.                                        NC2504.2
142200     ADD 5 TO DELETE-COUNTER.                                     NC2504.2
142300     GO TO IF--TEST-86.                                           NC2504.2
142400 IF--TEST-80.                                                     NC2504.2
142500     IF A80 (2)                                                   NC2504.2
142600         PERFORM PASS ELSE                                        NC2504.2
142700     PERFORM FAIL.                                                NC2504.2
142800*    NOTE ELMT(2) SHOULD CONTAIN A 2 WHICH IS CONTAINED IN        NC2504.2
142900*        THE VALUE OF THE A80 88 LEVEL.                           NC2504.2
143000     GO TO IF-WRITE-80.                                           NC2504.2
143100 IF--DELETE-80.                                                   NC2504.2
143200     PERFORM DE-LETE.                                             NC2504.2
143300 IF-WRITE-80.                                                     NC2504.2
143400     MOVE "IF--TEST-80" TO PAR-NAME.                              NC2504.2
143500     PERFORM PRINT-DETAIL.                                        NC2504.2
143600 IF--TEST-81.                                                     NC2504.2
143700     IF C80 (1)                                                   NC2504.2
143800         PERFORM FAIL ELSE                                        NC2504.2
143900     PERFORM PASS.                                                NC2504.2
144000*    NOTE ELMT(1) SHOULD CONTAIN A 1 WHICH IS NOT CONTAINED       NC2504.2
144100*        IN THE VALUE OF THE C80 88 LEVEL.                        NC2504.2
144200     GO TO IF-WRITE-81.                                           NC2504.2
144300 IF-DELETE-81.                                                    NC2504.2
144400     PERFORM DE-LETE.                                             NC2504.2
144500 IF-WRITE-81.                                                     NC2504.2
144600     MOVE "IF--TEST-81" TO PAR-NAME.                              NC2504.2
144700     PERFORM PRINT-DETAIL.                                        NC2504.2
144800 IF--TEST-82.                                                     NC2504.2
144900     IF B80 (3)                                                   NC2504.2
145000         PERFORM FAIL ELSE                                        NC2504.2
145100     PERFORM PASS.                                                NC2504.2
145200*    NOTE ELMT(3) SHOULD CONTAIN A 3 WHICH IS NOT CONTAINED       NC2504.2
145300*        IN THE VALUE OF THE B80 88 LEVEL.                        NC2504.2
145400     GO TO IF-WRITE-82.                                           NC2504.2
145500 IF-DELETE-82.                                                    NC2504.2
145600     PERFORM DE-LETE.                                             NC2504.2
145700 IF-WRITE-82.                                                     NC2504.2
145800     MOVE "IF--TEST-82" TO PAR-NAME.                              NC2504.2
145900     PERFORM PRINT-DETAIL.                                        NC2504.2
146000 IF--TEST-83.                                                     NC2504.2
146100     IF NOT A80 OF TABLE-80 (3)                                   NC2504.2
146200         PERFORM FAIL ELSE                                        NC2504.2
146300     PERFORM PASS.                                                NC2504.2
146400*    NOTE ELMT(3) SHOULD CONTAIN A 3 BUT THE NOT CONDITION        NC2504.2
146500*        SHOULD CAUSE THE TEST TO FAIL EVEN THOUGH THE A80        NC2504.2
146600*        VALUE INCLUDES THE VALUE 3.                              NC2504.2
146700     GO TO IF-WRITE-83.                                           NC2504.2
146800 IF-DELETE-83.                                                    NC2504.2
146900     PERFORM DE-LETE.                                             NC2504.2
147000 IF-WRITE-83.                                                     NC2504.2
147100     MOVE "IF--TEST-83" TO PAR-NAME.                              NC2504.2
147200     PERFORM PRINT-DETAIL.                                        NC2504.2
147300 IF--TEST-84.                                                     NC2504.2
147400     IF NOT B80 (1)                                               NC2504.2
147500         PERFORM PASS ELSE                                        NC2504.2
147600     PERFORM FAIL.                                                NC2504.2
147700*    NOTE ELMT(1) CONTAINS A 1 AND THE VALUE OF B80 IS 8          NC2504.2
147800*        SO, SAYING NOT 8 IS TRUE.                                NC2504.2
147900     GO TO IF-WRITE-84.                                           NC2504.2
148000 IF-DELETE-84.                                                    NC2504.2
148100     PERFORM DE-LETE.                                             NC2504.2
148200 IF-WRITE-84.                                                     NC2504.2
148300     MOVE "IF--TEST-84" TO PAR-NAME.                              NC2504.2
148400     PERFORM PRINT-DETAIL.                                        NC2504.2
148500 IF--TEST-85.                                                     NC2504.2
148600     IF C80 OF TABLE-80 (2)                                       NC2504.2
148700         PERFORM FAIL ELSE                                        NC2504.2
148800     PERFORM PASS.                                                NC2504.2
148900*    NOTE ELMT(2) IS 2 AND THE VALUES OF C80 DO NOT CONTAIN A 2.  NC2504.2
149000     GO TO IF-WRITE-85.                                           NC2504.2
149100 IF-DELETE-85.                                                    NC2504.2
149200     PERFORM DE-LETE.                                             NC2504.2
149300 IF-WRITE-85.                                                     NC2504.2
149400     MOVE "IF--TEST-85" TO PAR-NAME.                              NC2504.2
149500     PERFORM PRINT-DETAIL.                                        NC2504.2
149600 IF--TEST-86.                                                     NC2504.2
149700     IF A86                                                       NC2504.2
149800         PERFORM FAIL ELSE                                        NC2504.2
149900     PERFORM PASS.                                                NC2504.2
150000*    NOTE A86 (ABC   ) SHOULD NOT EQUAL TABLE-86 (ABCABC).        NC2504.2
150100     GO TO IF-WRITE-86.                                           NC2504.2
150200 IF-DELETE-86.                                                    NC2504.2
150300     PERFORM DE-LETE.                                             NC2504.2
150400 IF-WRITE-86.                                                     NC2504.2
150500     MOVE "IF--TEST-86" TO PAR-NAME.                              NC2504.2
150600     PERFORM PRINT-DETAIL.                                        NC2504.2
150700 IF--TEST-87.                                                     NC2504.2
150800     IF NOT B86                                                   NC2504.2
150900         PERFORM FAIL ELSE                                        NC2504.2
151000     PERFORM PASS.                                                NC2504.2
151100*    NOTE B86 (ABCABC) SHOULD EQUAL TABLE-86 (ABCABC) THUS        NC2504.2
151200*        FAILING THE TEST.                                        NC2504.2
151300     GO TO IF-WRITE-87.                                           NC2504.2
151400 IF-DELETE-87.                                                    NC2504.2
151500     PERFORM DE-LETE.                                             NC2504.2
151600 IF-WRITE-87.                                                     NC2504.2
151700     MOVE "IF--TEST-87" TO PAR-NAME.                              NC2504.2
151800     PERFORM PRINT-DETAIL.                                        NC2504.2
151900 IF--TEST-88.                                                     NC2504.2
152000     MOVE SPACES TO DATANAME-86.                                  NC2504.2
152100     IF C86                                                       NC2504.2
152200         PERFORM PASS ELSE                                        NC2504.2
152300     PERFORM FAIL.                                                NC2504.2
152400*    NOTE TABLE-86 (   ABC) SHOULD EQUAL C86 (   ABC).            NC2504.2
152500     GO TO IF-WRITE-88.                                           NC2504.2
152600 IF-DELETE-88.                                                    NC2504.2
152700     PERFORM DE-LETE.                                             NC2504.2
152800 IF-WRITE-88.                                                     NC2504.2
152900     MOVE "IF--TEST-88" TO PAR-NAME.                              NC2504.2
153000     PERFORM PRINT-DETAIL.                                        NC2504.2
153100 IF--INIT-R.                                                      NC2504.2
153200     MOVE     "FIGCON < = > D-NAME" TO FEATURE.                   NC2504.2
153300 IF--TEST-89.                                                     NC2504.2
153400     IF       ZEROS NOT < LOW-VAL                                 NC2504.2
153500              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
153600     GO       TO IF--WRITE-89.                                    NC2504.2
153700 IF--DELETE-89.                                                   NC2504.2
153800     PERFORM DE-LETE.                                             NC2504.2
153900 IF--WRITE-89.                                                    NC2504.2
154000     MOVE     "IF--TEST-89 " TO PAR-NAME.                         NC2504.2
154100     PERFORM  PRINT-DETAIL.                                       NC2504.2
154200 IF--TEST-90.                                                     NC2504.2
154300     IF       ZEROS < ONE23                                       NC2504.2
154400              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
154500     GO       TO IF--WRITE-90.                                    NC2504.2
154600 IF--DELETE-90.                                                   NC2504.2
154700     PERFORM DE-LETE.                                             NC2504.2
154800 IF--WRITE-90.                                                    NC2504.2
154900     MOVE     "IF--TEST-90 " TO PAR-NAME.                         NC2504.2
155000     PERFORM  PRINT-DETAIL.                                       NC2504.2
155100 IF--TEST-91.                                                     NC2504.2
155200     IF       ZEROS = ZERO-C                                      NC2504.2
155300              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
155400     GO       TO IF--WRITE-91.                                    NC2504.2
155500 IF--DELETE-91.                                                   NC2504.2
155600     PERFORM DE-LETE.                                             NC2504.2
155700 IF--WRITE-91.                                                    NC2504.2
155800     MOVE     "IF--TEST-91 " TO PAR-NAME.                         NC2504.2
155900     PERFORM PRINT-DETAIL.                                        NC2504.2
156000 IF--TEST-92.                                                     NC2504.2
156100     IF       ZEROS NOT = ZERO-D                                  NC2504.2
156200              PERFORM FAIL ELSE PERFORM PASS.                     NC2504.2
156300     GO       TO IF--WRITE-92.                                    NC2504.2
156400 IF--DELETE-92.                                                   NC2504.2
156500     PERFORM DE-LETE.                                             NC2504.2
156600 IF--WRITE-92.                                                    NC2504.2
156700     MOVE     "IF--TEST-92 " TO PAR-NAME.                         NC2504.2
156800     PERFORM  PRINT-DETAIL.                                       NC2504.2
156900 IF--TEST-93.                                                     NC2504.2
157000     IF       SPACES = SPACE-X                                    NC2504.2
157100              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
157200     GO       TO IF--WRITE-93.                                    NC2504.2
157300 IF--DELETE-93.                                                   NC2504.2
157400     PERFORM DE-LETE.                                             NC2504.2
157500 IF--WRITE-93.                                                    NC2504.2
157600     MOVE     "IF--TEST-93 " TO PAR-NAME.                         NC2504.2
157700     PERFORM  PRINT-DETAIL.                                       NC2504.2
157800 IF--TEST-94.                                                     NC2504.2
157900     IF       SPACES NOT = QUOTE-X                                NC2504.2
158000              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
158100     GO       TO IF--WRITE-94.                                    NC2504.2
158200 IF--DELETE-94.                                                   NC2504.2
158300     PERFORM DE-LETE.                                             NC2504.2
158400 IF--WRITE-94.                                                    NC2504.2
158500     MOVE     "IF--TEST-94 " TO PAR-NAME.                         NC2504.2
158600     PERFORM  PRINT-DETAIL.                                       NC2504.2
158700 IF--TEST-95.                                                     NC2504.2
158800     IF       SPACES > ABC OR < ABC                               NC2504.2
158900              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
159000     GO       TO IF--WRITE-95.                                    NC2504.2
159100 IF--DELETE-95.                                                   NC2504.2
159200     PERFORM DE-LETE.                                             NC2504.2
159300 IF--WRITE-95.                                                    NC2504.2
159400     MOVE     "IF--TEST-95 " TO PAR-NAME.                         NC2504.2
159500     PERFORM  PRINT-DETAIL.                                       NC2504.2
159600 IF--TEST-96.                                                     NC2504.2
159700     IF       QUOTES NOT > QUOTE-X                                NC2504.2
159800              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
159900     GO       TO IF--WRITE-96.                                    NC2504.2
160000 IF--DELETE-96.                                                   NC2504.2
160100     PERFORM DE-LETE.                                             NC2504.2
160200 IF--WRITE-96.                                                    NC2504.2
160300     MOVE     "IF--TEST-96 " TO PAR-NAME.                         NC2504.2
160400     PERFORM  PRINT-DETAIL.                                       NC2504.2
160500 IF--TEST-97.                                                     NC2504.2
160600     IF       QUOTES NOT = ZERO-D                                 NC2504.2
160700              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
160800     GO       TO IF--WRITE-97.                                    NC2504.2
160900 IF--DELETE-97.                                                   NC2504.2
161000     PERFORM DE-LETE.                                             NC2504.2
161100 IF--WRITE-97.                                                    NC2504.2
161200     MOVE     "IF--TEST-97 " TO PAR-NAME.                         NC2504.2
161300     PERFORM  PRINT-DETAIL.                                       NC2504.2
161400 IF--TEST-98.                                                     NC2504.2
161500     IF       HIGH-VALUES > LOW-VAL                               NC2504.2
161600              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
161700     GO       TO IF--WRITE-98.                                    NC2504.2
161800 IF--DELETE-98.                                                   NC2504.2
161900     PERFORM DE-LETE.                                             NC2504.2
162000 IF--WRITE-98.                                                    NC2504.2
162100     MOVE     "IF--TEST-98 " TO PAR-NAME.                         NC2504.2
162200     PERFORM  PRINT-DETAIL.                                       NC2504.2
162300 IF--TEST-99.                                                     NC2504.2
162400     IF       HIGH-VALUES > ABC                                   NC2504.2
162500              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
162600     GO       TO IF--WRITE-99.                                    NC2504.2
162700 IF--DELETE-99.                                                   NC2504.2
162800     PERFORM DE-LETE.                                             NC2504.2
162900 IF--WRITE-99.                                                    NC2504.2
163000     MOVE     "IF--TEST-99 " TO PAR-NAME.                         NC2504.2
163100     PERFORM  PRINT-DETAIL.                                       NC2504.2
163200 IF--TEST-100.                                                    NC2504.2
163300     IF       HIGH-VALUES NOT > ONE23                             NC2504.2
163400              PERFORM FAIL ELSE PERFORM PASS.                     NC2504.2
163500     GO       TO IF--WRITE-100.                                   NC2504.2
163600 IF--DELETE-100.                                                  NC2504.2
163700     PERFORM DE-LETE.                                             NC2504.2
163800 IF--WRITE-100.                                                   NC2504.2
163900     MOVE     "IF--TEST-100" TO PAR-NAME.                         NC2504.2
164000     PERFORM  PRINT-DETAIL.                                       NC2504.2
164100 IF--TEST-101.                                                    NC2504.2
164200     IF       HIGH-VALUES = ZERO-D                                NC2504.2
164300              PERFORM FAIL ELSE PERFORM PASS.                     NC2504.2
164400     GO       TO IF--WRITE-101.                                   NC2504.2
164500 IF--DELETE-101.                                                  NC2504.2
164600     PERFORM DE-LETE.                                             NC2504.2
164700 IF--WRITE-101.                                                   NC2504.2
164800     MOVE     "IF--TEST-101" TO PAR-NAME.                         NC2504.2
164900     PERFORM  PRINT-DETAIL.                                       NC2504.2
165000 IF--TEST-102.                                                    NC2504.2
165100     IF       LOW-VALUES = LOW-VAL                                NC2504.2
165200              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
165300     GO       TO IF--WRITE-102.                                   NC2504.2
165400 IF--DELETE-102.                                                  NC2504.2
165500     PERFORM DE-LETE.                                             NC2504.2
165600 IF--WRITE-102.                                                   NC2504.2
165700     MOVE     "IF--TEST-102" TO PAR-NAME.                         NC2504.2
165800     PERFORM  PRINT-DETAIL.                                       NC2504.2
165900 IF--TEST-103.                                                    NC2504.2
166000     IF       LOW-VALUES < ABC                                    NC2504.2
166100              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
166200     GO       TO IF--WRITE-103.                                   NC2504.2
166300 IF--DELETE-103.                                                  NC2504.2
166400     PERFORM DE-LETE.                                             NC2504.2
166500 IF--WRITE-103.                                                   NC2504.2
166600     MOVE     "IF--TEST-103" TO PAR-NAME.                         NC2504.2
166700     PERFORM  PRINT-DETAIL.                                       NC2504.2
166800 IF--TEST-104.                                                    NC2504.2
166900     IF       ALL "00" < ONE23                                    NC2504.2
167000              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
167100     GO       TO IF--WRITE-104.                                   NC2504.2
167200 IF--DELETE-104.                                                  NC2504.2
167300     PERFORM DE-LETE.                                             NC2504.2
167400 IF--WRITE-104.                                                   NC2504.2
167500     MOVE     "IF--TEST-104" TO PAR-NAME.                         NC2504.2
167600     PERFORM  PRINT-DETAIL.                                       NC2504.2
167700 IF--TEST-105.                                                    NC2504.2
167800     IF       ALL ZEROES = ZERO-D                                 NC2504.2
167900              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
168000     GO       TO IF--WRITE-105.                                   NC2504.2
168100 IF--DELETE-105.                                                  NC2504.2
168200     PERFORM DE-LETE.                                             NC2504.2
168300 IF--WRITE-105.                                                   NC2504.2
168400     MOVE     "IF--TEST-105" TO PAR-NAME.                         NC2504.2
168500     PERFORM  PRINT-DETAIL.                                       NC2504.2
168600 IF--TEST-106.                                                    NC2504.2
168700     IF       ALL "00" NOT > ZERO-D                               NC2504.2
168800              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
168900     GO       TO IF--WRITE-106.                                   NC2504.2
169000 IF--DELETE-106.                                                  NC2504.2
169100     PERFORM DE-LETE.                                             NC2504.2
169200 IF--WRITE-106.                                                   NC2504.2
169300     MOVE     "IF--TEST-106" TO PAR-NAME.                         NC2504.2
169400     PERFORM  PRINT-DETAIL.                                       NC2504.2
169500 IF--TEST-107.                                                    NC2504.2
169600     IF       ALL "A" = SPACE-X                                   NC2504.2
169700              PERFORM FAIL ELSE PERFORM PASS.                     NC2504.2
169800     GO       TO IF--WRITE-107.                                   NC2504.2
169900 IF--DELETE-107.                                                  NC2504.2
170000     PERFORM DE-LETE.                                             NC2504.2
170100 IF--WRITE-107.                                                   NC2504.2
170200     MOVE     "IF--TEST-107" TO PAR-NAME.                         NC2504.2
170300     PERFORM  PRINT-DETAIL.                                       NC2504.2
170400 IF--TEST-108.                                                    NC2504.2
170500     IF       ALL "A" > ABC                                       NC2504.2
170600              PERFORM FAIL ELSE PERFORM PASS.                     NC2504.2
170700     GO       TO IF--WRITE-108.                                   NC2504.2
170800 IF--DELETE-108.                                                  NC2504.2
170900     PERFORM DE-LETE.                                             NC2504.2
171000 IF--WRITE-108.                                                   NC2504.2
171100     MOVE     "IF--TEST-108" TO PAR-NAME.                         NC2504.2
171200     PERFORM  PRINT-DETAIL.                                       NC2504.2
171300 IF--TEST-109.                                                    NC2504.2
171400     IF       IF-D4 ALPHABETIC                                    NC2504.2
171500              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
171600     GO       TO IF--WRITE-109.                                   NC2504.2
171700 IF--DELETE-109.                                                  NC2504.2
171800     PERFORM  DE-LETE.                                            NC2504.2
171900 IF--WRITE-109.                                                   NC2504.2
172000     MOVE     "CLASS --- ALPHABETIC" TO FEATURE.                  NC2504.2
172100     MOVE     "IF--TEST-109" TO PAR-NAME.                         NC2504.2
172200     PERFORM  PRINT-DETAIL.                                       NC2504.2
172300 IF--INIT-S.                                                      NC2504.2
172400     MOVE     "SIGN --- ZERO" TO FEATURE.                         NC2504.2
172500 IF--TEST-110.                                                    NC2504.2
172600     IF      SMALLEST-VALU GREATER THAN SMALL-VALU                NC2504.2
172700         AND IS NOT LESS THAN EVEN-SMALLER OR SMALLER-VALU        NC2504.2
172800             MOVE "CONDITION FALSE" TO CORRECT-A                  NC2504.2
172900             MOVE "CONDITION TRUE " TO COMPUTED-A                 NC2504.2
173000             PERFORM FAIL                                         NC2504.2
173100             GO TO IF--WRITE-110.                                 NC2504.2
173200     PERFORM PASS.                                                NC2504.2
173300     GO TO IF--WRITE-110.                                         NC2504.2
173400 IF--DELETE-110.                                                  NC2504.2
173500     PERFORM  DE-LETE.                                            NC2504.2
173600 IF--WRITE-110.                                                   NC2504.2
173700     MOVE     "IF--TEST-110" TO PAR-NAME.                         NC2504.2
173800     MOVE "ABBREV CONDITIONS" TO FEATURE.                         NC2504.2
173900     PERFORM  PRINT-DETAIL.                                       NC2504.2
174000 IF--TEST-111.                                                    NC2504.2
174100     IF SMALLEST-VALU LESS THAN SMALL-VALU AND                    NC2504.2
174200     (SMALLEST-VALU GREATER THAN EVEN-SMALLER OR SMALLER-VALU)    NC2504.2
174300              PERFORM PASS GO TO IF--WRITE-111.                   NC2504.2
174400     MOVE "CONDITION TRUE" TO CORRECT-A.                          NC2504.2
174500     MOVE "CONDITION FALSE" TO COMPUTED-A.                        NC2504.2
174600     PERFORM  FAIL.                                               NC2504.2
174700     GO       TO IF--WRITE-111.                                   NC2504.2
174800 IF--DELETE-111.                                                  NC2504.2
174900     PERFORM  DE-LETE.                                            NC2504.2
175000 IF--WRITE-111.                                                   NC2504.2
175100     MOVE     "IF--TEST-111" TO PAR-NAME.                         NC2504.2
175200     PERFORM  PRINT-DETAIL.                                       NC2504.2
175300 IF--TEST-112.                                                    NC2504.2
175400     IF       IF-D40B                                             NC2504.2
175500              PERFORM PASS ELSE PERFORM FAIL.                     NC2504.2
175600     GO       TO IF--WRITE-112.                                   NC2504.2
175700 IF--DELETE-112.                                                  NC2504.2
175800     PERFORM  DE-LETE.                                            NC2504.2
175900 IF--WRITE-112.                                                   NC2504.2
176000     MOVE "CONDITION---NAME" TO FEATURE.                          NC2504.2
176100     MOVE     "IF--TEST-112" TO PAR-NAME.                         NC2504.2
176200     PERFORM  PRINT-DETAIL.                                       NC2504.2
176300 IF--INIT-T.                                                      NC2504.2
176400     MOVE "ABBREV---CONDITION" TO FEATURE.                        NC2504.2
176500 IF--TEST-113.                                                    NC2504.2
176600     IF SMALLEST-VALU LESS THAN SMALL-VALU AND (SMALLEST-VALU NOT NC2504.2
176700     GREATER THAN EVEN-SMALLER OR SMALLER-VALU)                   NC2504.2
176800             PERFORM PASS                                         NC2504.2
176900         GO TO IF--WRITE-113.                                     NC2504.2
177000     MOVE "CONDITION TRUE" TO CORRECT-A.                          NC2504.2
177100     MOVE "CONDITION FALSE" TO COMPUTED-A.                        NC2504.2
177200     PERFORM FAIL.                                                NC2504.2
177300     GO TO IF--WRITE-113.                                         NC2504.2
177400 IF--DELETE-113.                                                  NC2504.2
177500     PERFORM  DE-LETE.                                            NC2504.2
177600 IF--WRITE-113.                                                   NC2504.2
177700     MOVE     "IF--TEST-113" TO PAR-NAME.                         NC2504.2
177800     PERFORM  PRINT-DETAIL.                                       NC2504.2
177900 IF--TEST-114.                                                    NC2504.2
178000     IF      SMALLEST-VALU LESS THAN SMALL-VALU                   NC2504.2
178100             AND NOT EVEN-SMALLER OR SMALLER-VALU                 NC2504.2
178200             PERFORM PASS                                         NC2504.2
178300             GO TO IF--WRITE-114                                  NC2504.2
178400             ELSE                                                 NC2504.2
178500             PERFORM FAIL                                         NC2504.2
178600             MOVE "CONDITION FALSE" TO CORRECT-A                  NC2504.2
178700             MOVE "CONDITION TRUE" TO COMPUTED-A                  NC2504.2
178800             GO TO IF--WRITE-114.                                 NC2504.2
178900 IF--DELETE-114.                                                  NC2504.2
179000     PERFORM  DE-LETE.                                            NC2504.2
179100 IF--WRITE-114.                                                   NC2504.2
179200     MOVE     "IF--TEST-114" TO PAR-NAME.                         NC2504.2
179300     PERFORM  PRINT-DETAIL.                                       NC2504.2
179400 IF--TEST-115.                                                    NC2504.2
179500     IF COMP-SGN1 IS POSITIVE                                     NC2504.2
179600         PERFORM PASS                                             NC2504.2
179700         GO TO IF--WRITE-115.                                     NC2504.2
179800     MOVE "POSITIVE EXPECTED" TO CORRECT-A.                       NC2504.2
179900     MOVE COMP-SGN1 TO COMPUTED-14V4.                             NC2504.2
180000     PERFORM FAIL.                                                NC2504.2
180100     GO TO IF--WRITE-115.                                         NC2504.2
180200 IF--DELETE-115.                                                  NC2504.2
180300     PERFORM DE-LETE.                                             NC2504.2
180400 IF--WRITE-115.                                                   NC2504.2
180500     MOVE "POS/NEG SIGN TEST" TO FEATURE.                         NC2504.2
180600     MOVE "IF--TEST-115" TO PAR-NAME.                             NC2504.2
180700     PERFORM PRINT-DETAIL.                                        NC2504.2
180800 IF--TEST-116.                                                    NC2504.2
180900     IF COMP-SGN2 NOT POSITIVE                                    NC2504.2
181000         MOVE COMP-SGN2 TO COMPUTED-14V4                          NC2504.2
181100         MOVE "POSITIVE EXPECTED" TO CORRECT-A                    NC2504.2
181200         PERFORM FAIL                                             NC2504.2
181300         GO TO IF--WRITE-116.                                     NC2504.2
181400     PERFORM PASS.                                                NC2504.2
181500     GO TO IF--WRITE-116.                                         NC2504.2
181600 IF--DELETE-116.                                                  NC2504.2
181700     PERFORM DE-LETE.                                             NC2504.2
181800 IF--WRITE-116.                                                   NC2504.2
181900     MOVE "IF--TEST-116" TO PAR-NAME.                             NC2504.2
182000     PERFORM PRINT-DETAIL.                                        NC2504.2
182100 IF--TEST-117.                                                    NC2504.2
182200     IF COMP-SGN3 NOT NEGATIVE                                    NC2504.2
182300         MOVE COMP-SGN3 TO COMPUTED-14V4                          NC2504.2
182400         MOVE "NEGATIVE EXPECTED" TO CORRECT-A                    NC2504.2
182500         PERFORM FAIL                                             NC2504.2
182600         GO TO IF--WRITE-117.                                     NC2504.2
182700     PERFORM PASS.                                                NC2504.2
182800     GO TO IF--WRITE-117.                                         NC2504.2
182900 IF--DELETE-117.                                                  NC2504.2
183000     PERFORM DE-LETE.                                             NC2504.2
183100 IF--WRITE-117.                                                   NC2504.2
183200     MOVE "IF--TEST-117" TO PAR-NAME.                             NC2504.2
183300     PERFORM PRINT-DETAIL.                                        NC2504.2
183400 IF--TEST-118.                                                    NC2504.2
183500     IF COMP-SGN4 NOT POSITIVE                                    NC2504.2
183600         PERFORM PASS                                             NC2504.2
183700         GO TO IF--WRITE-118.                                     NC2504.2
183800     MOVE COMP-SGN4 TO COMPUTED-14V4.                             NC2504.2
183900     MOVE "NEGATIVE EXPECTED" TO CORRECT-A.                       NC2504.2
184000     PERFORM FAIL.                                                NC2504.2
184100     GO TO IF--WRITE-118.                                         NC2504.2
184200 IF--DELETE-118.                                                  NC2504.2
184300     PERFORM DE-LETE.                                             NC2504.2
184400 IF--WRITE-118.                                                   NC2504.2
184500     MOVE "IF--TEST-118" TO PAR-NAME.                             NC2504.2
184600     PERFORM PRINT-DETAIL.                                        NC2504.2
184700 IF--TEST-119.                                                    NC2504.2
184800     MOVE SPACES TO TEST-RESULTS.                                 NC2504.2
184900     MOVE "NOT USED" TO RE-MARK.                                  NC2504.2
185000     MOVE "IF--TEST-119" TO PAR-NAME.                             NC2504.2
185100     PERFORM PRINT-DETAIL.                                        NC2504.2
185200 IF--TEST-120.                                                    NC2504.2
185300     MOVE     -10 TO WRK-DS-06V06.                                NC2504.2
185400     ADD      +10 TO WRK-DS-06V06.                                NC2504.2
185500     IF       WRK-DS-06V06 NEGATIVE                               NC2504.2
185600              PERFORM FAIL-120-121                                NC2504.2
185700              MOVE "NEGATIVE ZERO DETECTED" TO RE-MARK            NC2504.2
185800              GO TO IF--WRITE-120.                                NC2504.2
185900     IF       WRK-DS-06V06 POSITIVE                               NC2504.2
186000              PERFORM FAIL-120-121                                NC2504.2
186100              MOVE "POSITIVE ZERO DETECTED" TO RE-MARK            NC2504.2
186200              GO TO IF--WRITE-120.                                NC2504.2
186300     IF       WRK-DS-06V06 ZERO                                   NC2504.2
186400              PERFORM PASS GO TO IF--WRITE-120.                   NC2504.2
186500     PERFORM  FAIL-120-121.                                       NC2504.2
186600     MOVE     "NEITHER POS, NEG, NOR ZERO" TO RE-MARK.            NC2504.2
186700     GO       TO IF--WRITE-120.                                   NC2504.2
186800 IF--DELETE-120.                                                  NC2504.2
186900     PERFORM  DE-LETE.                                            NC2504.2
187000 IF--WRITE-120.                                                   NC2504.2
187100     MOVE     "SIGN TEST ON ZERO" TO FEATURE.                     NC2504.2
187200     MOVE     "IF--TEST-120" TO PAR-NAME.                         NC2504.2
187300     PERFORM  PRINT-DETAIL.                                       NC2504.2
187400     GO       TO IF--EXIT-120.                                    NC2504.2
187500 FAIL-120-121.                                                    NC2504.2
187600     PERFORM  FAIL.                                               NC2504.2
187700     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC2504.2
187800     MOVE     ZERO TO CORRECT-N.                                  NC2504.2
187900 IF--EXIT-120.                                                    NC2504.2
188000     EXIT.                                                        NC2504.2
188100 IF--TEST-121.                                                    NC2504.2
188200     MOVE     10 TO WRK-DS-06V06.                                 NC2504.2
188300     SUBTRACT 10 FROM WRK-DS-06V06.                               NC2504.2
188400     IF       WRK-DS-06V06 NEGATIVE                               NC2504.2
188500              PERFORM FAIL-120-121                                NC2504.2
188600              MOVE "NEGATIVE ZERO DETECTED" TO RE-MARK            NC2504.2
188700              GO TO IF--WRITE-121.                                NC2504.2
188800     IF       WRK-DS-06V06 POSITIVE                               NC2504.2
188900              PERFORM FAIL-120-121                                NC2504.2
189000              MOVE "POSITIVE ZERO DETECTED" TO RE-MARK            NC2504.2
189100              GO TO IF--WRITE-121.                                NC2504.2
189200                                                                  NC2504.2
189300     IF       WRK-DS-06V06 ZERO                                   NC2504.2
189400              PERFORM PASS GO TO IF--WRITE-121.                   NC2504.2
189500     PERFORM  FAIL-120-121.                                       NC2504.2
189600     MOVE     "NEITHER POS, NEG, NOR ZERO" TO RE-MARK.            NC2504.2
189700     GO       TO IF--WRITE-120.                                   NC2504.2
189800 IF--DELETE-121.                                                  NC2504.2
189900     PERFORM  DE-LETE.                                            NC2504.2
190000 IF--WRITE-121.                                                   NC2504.2
190100     MOVE     "IF--TEST-121" TO PAR-NAME.                         NC2504.2
190200     PERFORM  PRINT-DETAIL.                                       NC2504.2
190300 IF-INIT-122.                                                     NC2504.2
190400     MOVE   "VI-89 6.15" TO ANSI-REFERENCE.                       NC2504.2
190500     MOVE    1 TO WRK-DU-1V0-1.                                   NC2504.2
190600     MOVE    2 TO WRK-DU-1V0-2.                                   NC2504.2
190700     MOVE    3 TO WRK-DU-1V0-3.                                   NC2504.2
190800     MOVE    0 TO WRK-DU-1V0-4.                                   NC2504.2
190900 IF-TEST-122.                                                     NC2504.2
191000     IF NOT (WRK-DU-1V0-1 NOT GREATER WRK-DU-1V0-2 AND            NC2504.2
191100         WRK-DU-1V0-3 AND NOT WRK-DU-1V0-4) GO TO BUMMER-122      NC2504.2
191200         ELSE NEXT SENTENCE.                                      NC2504.2
191300     PERFORM PASS.                                                NC2504.2
191400     GO TO IF-WRITE-122.                                          NC2504.2
191500 IF-DELETE-122.                                                   NC2504.2
191600     PERFORM DE-LETE.                                             NC2504.2
191700     GO TO IF-WRITE-122.                                          NC2504.2
191800 BUMMER-122.                                                      NC2504.2
191900     PERFORM FAIL.                                                NC2504.2
192000     MOVE "RESULT TRUE" TO COMPUTED-A.                            NC2504.2
192100     MOVE "SHOULD BE FALSE" TO CORRECT-A.                         NC2504.2
192200 IF-WRITE-122.                                                    NC2504.2
192300     MOVE "IF-TEST-122" TO PAR-NAME.                              NC2504.2
192400     MOVE "ABR. COM. REL. CONDT" TO FEATURE.                      NC2504.2
192500     PERFORM PRINT-DETAIL.                                        NC2504.2
192600 IF-INIT-123.                                                     NC2504.2
192700     MOVE   "VI-89 6.15" TO ANSI-REFERENCE.                       NC2504.2
192800     MOVE 9 TO WRK-DU-1V0-1.                                      NC2504.2
192900     MOVE 8 TO WRK-DU-1V0-2.                                      NC2504.2
193000     MOVE 7 TO WRK-DU-1V0-3.                                      NC2504.2
193100 IF-LOGICAL-CONN-TEST-123.                                        NC2504.2
193200     IF WRK-DU-1V0-1 > WRK-DU-1V0-2 AND NOT < WRK-DU-2V0-1 OR     NC2504.2
193300             WRK-DU-2V0-2 OR NOT WRK-DU-2V0-3 AND WRK-DU-1V0-3    NC2504.2
193400             PERFORM PASS                                         NC2504.2
193500     ELSE                                                         NC2504.2
193600             PERFORM FAIL MOVE "FALSE RESULT FOUND" TO COMPUTED-A NC2504.2
193700             MOVE "SHOULD BE TRUE" TO CORRECT-A.                  NC2504.2
193800     GO TO IF-WRITE-123.                                          NC2504.2
193900 IF-DELETE-123.                                                   NC2504.2
194000     PERFORM DE-LETE.                                             NC2504.2
194100 IF-WRITE-123.                                                    NC2504.2
194200     MOVE "IF-TEST-123" TO PAR-NAME.                              NC2504.2
194300     MOVE "LOGICAL CONNECTIVES" TO FEATURE.                       NC2504.2
194400     PERFORM PRINT-DETAIL.                                        NC2504.2
194500     PERFORM END-ROUTINE.                                         NC2504.2
194600     MOVE    " COLLATING-AND-ALPHABET-TEST-9  SYNTAX CHECK IN OBJENC2504.2
194700-    "CT-COMPUTER AND SPECIAL-NAMES" TO TEST-RESULTS.             NC2504.2
194800     PERFORM PRINT-DETAIL.                                        NC2504.2
194900     MOVE SPACE TO TEST-RESULTS.                                  NC2504.2
195000 IF-INIT-124.                                                     NC2504.2
195100*    ===-->  ARITHMETIC EXPRESSION CONTAINING ZERO  <--===        NC2504.2
195200     MOVE   "VI-58 6.3.1.5 AND VI-51 6.2" TO ANSI-REFERENCE.      NC2504.2
195300     MOVE    4 TO WRK-DU-1V0-1.                                   NC2504.2
195400     MOVE   "IF-TEST-124" TO PAR-NAME.                            NC2504.2
195500 IF-TEST-124.                                                     NC2504.2
195600     IF      ZERO - WRK-DU-1V0-1 IS NEGATIVE                      NC2504.2
195700             PERFORM PASS                                         NC2504.2
195800     ELSE                                                         NC2504.2
195900             PERFORM FAIL                                         NC2504.2
196000             MOVE "POSITIVE RESULT FOUND" TO COMPUTED-A           NC2504.2
196100             MOVE "SHOULD BE NEGATIVE" TO CORRECT-A.              NC2504.2
196200     GO TO IF-WRITE-124.                                          NC2504.2
196300 IF-DELETE-124.                                                   NC2504.2
196400     PERFORM DE-LETE.                                             NC2504.2
196500 IF-WRITE-124.                                                    NC2504.2
196600     MOVE "IF-TEST-124" TO PAR-NAME.                              NC2504.2
196700     MOVE "LOGICAL CONNECTIVES" TO FEATURE.                       NC2504.2
196800     PERFORM PRINT-DETAIL.                                        NC2504.2
196900 CCVS-EXIT SECTION.                                               NC2504.2
197000 CCVS-999999.                                                     NC2504.2
197100     GO TO CLOSE-FILES.                                           NC2504.2
      *END-OF,NC250A                                                                  
