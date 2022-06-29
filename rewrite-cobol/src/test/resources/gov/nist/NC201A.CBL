      *HEADER,COBOL,NC201A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2014.2
000200 PROGRAM-ID.                                                      NC2014.2
000300     NC201A.                                                      NC2014.2
000400****************************************************************  NC2014.2
000500*                                                              *  NC2014.2
000600*    VALIDATION FOR:-                                          *  NC2014.2
000700*                                                              *  NC2014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2014.2
000900*                                                              *  NC2014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2014.2
001100*                                                              *  NC2014.2
001200****************************************************************  NC2014.2
001300*                                                              *  NC2014.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2014.2
001500*                                                              *  NC2014.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2014.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2014.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2014.2
001900*                                                              *  NC2014.2
002000****************************************************************  NC2014.2
002100*    PROGRAM NC201A TESTS FORMAT 3 AND 4 OF THE "PERFORM"         NC2014.2
002200*    STATEMENT.                                                   NC2014.2
002300*    A VARIETY OF QUALIFIED DATA-NAMES AND CONDITION-NAMES        NC2014.2
002400*    ARE USED.                                                    NC2014.2
002500*                                                                 NC2014.2
002600*                                                                 NC2014.2
002700                                                                  NC2014.2
002800 ENVIRONMENT DIVISION.                                            NC2014.2
002900 CONFIGURATION SECTION.                                           NC2014.2
003000 SOURCE-COMPUTER.                                                 NC2014.2
003100     XXXXX082.                                                    NC2014.2
003200 OBJECT-COMPUTER.                                                 NC2014.2
003300     XXXXX083.                                                    NC2014.2
003400 INPUT-OUTPUT SECTION.                                            NC2014.2
003500 FILE-CONTROL.                                                    NC2014.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2014.2
003700     XXXXX055.                                                    NC2014.2
003800 DATA DIVISION.                                                   NC2014.2
003900 FILE SECTION.                                                    NC2014.2
004000 FD  PRINT-FILE.                                                  NC2014.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2014.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2014.2
004300 WORKING-STORAGE SECTION.                                         NC2014.2
004400 01  WRK-DU-2V1-1 PIC 99V9 VALUE ZERO.                            NC2014.2
004500 01  WRK-DU-0V1-1 PIC V9 VALUE .1.                                NC2014.2
004600 01  WRK-DU-2V1-2 PIC 99V9 VALUE 0.1.                             NC2014.2
004700 01  WRK-DU-2V1-3  PIC 99V9  VALUE  11.1.                         NC2014.2
004800 01  WRK-DU-1V0-1 PIC 9 VALUE 1.                                  NC2014.2
004900 01  WRK-DU-1V0-2 PIC 9 VALUE 2.                                  NC2014.2
005000 01  WRK-DU-1V0-3 PIC 9 VALUE 3.                                  NC2014.2
005100 01  WRK-DU-1V0-4 PIC 9 VALUE ZERO.                               NC2014.2
005200 01  WRK-DU-2V0-1 PIC 99 VALUE 10.                                NC2014.2
005300 01  WRK-DU-2V0-2 PIC 99 VALUE 11.                                NC2014.2
005400 01  WRK-DU-2V0-3 PIC 99 VALUE 12.                                NC2014.2
005500 01  COUNT-DU-6V0  PIC 9(6).                                      NC2014.2
005600 77  SMALL-VALU   PICTURE 99 VALUE 7.                             NC2014.2
005700 77  SMALLER-VALU PICTURE 99 VALUE 6.                             NC2014.2
005800 77  SMALLEST-VALU   PICTURE 99 VALUE 5.                          NC2014.2
005900 77  EVEN-SMALLER PICTURE 99 VALUE 1.                             NC2014.2
006000 77  WRK-DS-02V00                 PICTURE S99.                    NC2014.2
006100     88 TEST-2NUC-COND-99         VALUE 99.                       NC2014.2
006200 77  WRK-DS-06V06     PICTURE S9(6)V9(6).                         NC2014.2
006300 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC2014.2
006400                                  PICTURE S9(12).                 NC2014.2
006500 77  A02TWOS-DS-02V00             PICTURE S99    VALUE 22.        NC2014.2
006600 77  WRK-DS-01V00                 PICTURE S9.                     NC2014.2
006700 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  NC2014.2
006800 77  A990-DS-0201P                PICTURE S99P   VALUE 990.       NC2014.2
006900 77  A02ONES-DS-02V00             PICTURE S99    VALUE 11.        NC2014.2
007000 77  A01ONE-DS-P0801              PICTURE SP(8)9 VALUE .000000001.NC2014.2
007100 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         NC2014.2
007200 77  WRK-XN-00001                 PICTURE X.                      NC2014.2
007300 77  WRK-XN-00005                 PICTURE X(5).                   NC2014.2
007400 77  TWO  PICTURE 9 VALUE 2.                                      NC2014.2
007500 77  THREE PICTURE 9 VALUE 3.                                     NC2014.2
007600 77  SEVEN PICTURE 9 VALUE 7.                                     NC2014.2
007700 77  NINE  PICTURE 9 VALUE 9.                                     NC2014.2
007800 77  TEN  PICTURE 99 VALUE 10.                                    NC2014.2
007900 77  ALTERCOUNT PICTURE 999 VALUE ZERO.                           NC2014.2
008000 77  XRAY PICTURE IS X.                                           NC2014.2
008100 77  IF-D1 PICTURE S9(4)V9(2) VALUE 0.                            NC2014.2
008200 77  IF-D2 PICTURE S9(4)V9(2) VALUE ZERO.                         NC2014.2
008300 77  IF-D3 PICTURE X(10) VALUE "0000000000".                      NC2014.2
008400 77  IF-D4 PICTURE X(15) VALUE "               ".                 NC2014.2
008500 77  IF-D5 PICTURE X(10) VALUE ALL QUOTE.                         NC2014.2
008600 77  IF-D6 PICTURE A(10) VALUE "BABABABABA".                      NC2014.2
008700 77  IF-D7 PICTURE S9(6)V9(4) VALUE +123.45.                      NC2014.2
008800 77  IF-D8 PICTURE 9(6)V9(4) VALUE 12300.                         NC2014.2
008900 77  IF-D9 PICTURE X(3) VALUE "123".                              NC2014.2
009000 77  IF-D11 PICTURE X(6) VALUE "ABCDEF".                          NC2014.2
009100 77  IF-D13 PICTURE 9(6)V9(4) VALUE 12300.                        NC2014.2
009200 77  IF-D14 PICTURE S9(4)V9(2) VALUE +123.45.                     NC2014.2
009300 77  IF-D15 PICTURE S999PP VALUE 12300.                           NC2014.2
009400 77  IF-D16 PICTURE PP99 VALUE .0012.                             NC2014.2
009500 77  IF-D17 PICTURE SV9(4) VALUE .0012.                           NC2014.2
009600 77  IF-D18 PICTURE X(10) VALUE "BABABABABA".                     NC2014.2
009700 77  IF-D19 PICTURE X(10) VALUE "ABCDEF    ".                     NC2014.2
009800 77  IF-D23 PICTURE $9,9B9.90+.                                   NC2014.2
009900 77  IF-D24 PICTURE X(10) VALUE "$1,2 3.40+".                     NC2014.2
010000 77  IF-D25 PICTURE ABABX0A.                                      NC2014.2
010100 77  IF-D26 PICTURE X(8) VALUE "A C D0E".                         NC2014.2
010200 77  IF-D27 PICTURE IS 9(6)V9(4) VALUE IS 2137.45                 NC2014.2
010300     USAGE IS COMPUTATIONAL.                                      NC2014.2
010400 77  IF-D28 PICTURE IS 999999V9999 VALUE IS 2137.45.              NC2014.2
010500 77  IF-D31 PICTURE S9(6) VALUE -123.                             NC2014.2
010600 77  IF-D32 PICTURE S9(4)V99.                                     NC2014.2
010700     88  A  VALUE 1.                                              NC2014.2
010800     88  B VALUES ARE 2 THRU 4.                                   NC2014.2
010900     88  C VALUE IS ZERO.                                         NC2014.2
011000     88  D VALUE IS +12.34.                                       NC2014.2
011100     88  E  VALUE IS .01, .11, .21 .81.                           NC2014.2
011200     88  F  VALUE IS 100 THRU 128 1000 THRU 1280 -9 THRU -2.      NC2014.2
011300     88  G  VALUE IS 8765.43 1234 THRU 5678 5 -9999 THRU 10.      NC2014.2
011400 77  IF-D33 PICTURE X(4).                                         NC2014.2
011500     88  B   VALUE QUOTE.                                         NC2014.2
011600     88  C   VALUE SPACE.                                         NC2014.2
011700     88 D VALUE ALL "BAC".                                        NC2014.2
011800 77  IF-D34 PICTURE A(4).                                         NC2014.2
011900     88  B VALUE "A A ".                                          NC2014.2
012000 77  IF-D37 PICTURE 9(5) VALUE 12345.                             NC2014.2
012100 77  IF-D38 PICTURE X(9) VALUE "12345    ".                       NC2014.2
012200 77  CCON-1 PICTURE 99 VALUE 11.                                  NC2014.2
012300 77  CCON-2 PICTURE 99 VALUE 12.                                  NC2014.2
012400 77  CCON-3 PICTURE 99 VALUE 13.                                  NC2014.2
012500 77  COMP-SGN1  PICTURE S9(1) VALUE +9 COMPUTATIONAL.             NC2014.2
012600 77  COMP-SGN2  PICTURE S9(18) VALUE +3 COMPUTATIONAL.            NC2014.2
012700 77  COMP-SGN3  PICTURE S9(1) VALUE -5 COMPUTATIONAL.             NC2014.2
012800 77  COMP-SGN4  PICTURE S9(18) VALUE -3167598765431 COMPUTATIONAL.NC2014.2
012900 77  START-POINT        PICTURE 9(6) COMPUTATIONAL.               NC2014.2
013000 77  INC-VALUE          PICTURE 9(6) COMPUTATIONAL.               NC2014.2
013100 77  SWITCH-PFM-1 PICTURE 9 VALUE ZERO.                           NC2014.2
013200 77  SWITCH-PFM-2 PICTURE 9 VALUE ZERO.                           NC2014.2
013300 77  PFM-11-COUNTER PICTURE 999 VALUE ZERO.                       NC2014.2
013400 77  PFM-12-COUNTER  PICTURE 999 VALUE 100.                       NC2014.2
013500 77  PFM-12-ANS1  PICTURE 999 VALUE ZERO.                         NC2014.2
013600 77  PFM-12-ANS2  PICTURE 999 VALUE ZERO.                         NC2014.2
013700 01  SUBSCRIPT-6 PICTURE 99999 VALUE ZERO.                        NC2014.2
013800 01  IF-TABLE.                                                    NC2014.2
013900     02 IF-ELEM PICTURE X OCCURS 12 TIMES.                        NC2014.2
014000 01  QUOTE-DATA.                                                  NC2014.2
014100     02 QU-1 PICTURE X(3) VALUE "123".                            NC2014.2
014200     02 QU-2 PICTURE X VALUE QUOTE.                               NC2014.2
014300     02 QU-3 PICTURE X(6) VALUE "ABC456".                         NC2014.2
014400 01  IF-D10.                                                      NC2014.2
014500     02  D1 PICTURE X(2) VALUE "01".                              NC2014.2
014600     02  D2 PICTURE X(2) VALUE "23".                              NC2014.2
014700     02  D3.                                                      NC2014.2
014800     03  D4 PICTURE X(4) VALUE "4567".                            NC2014.2
014900     03 D5 PICTURE X(4) VALUE "8912".                             NC2014.2
015000 01  IF-D12.                                                      NC2014.2
015100     02  D1 PICTURE X(3) VALUE "ABC".                             NC2014.2
015200     02  D2.                                                      NC2014.2
015300     03  D3.                                                      NC2014.2
015400     04  D4      PICTURE XX     VALUE "DE".                       NC2014.2
015500     04  D5 PICTURE X VALUE "F".                                  NC2014.2
015600 01  IF-D20.                                                      NC2014.2
015700     02  FILLER    PICTURE 9(5)   VALUE ZERO.                     NC2014.2
015800     02  D1 PICTURE 9(2) VALUE 12.                                NC2014.2
015900     02  D2 PICTURE 9 VALUE 3.                                    NC2014.2
016000     02  D3 PICTURE 9(2) VALUE 45.                                NC2014.2
016100 01  IF-D21.                                                      NC2014.2
016200     02  D1 PICTURE 9(5) VALUE ZEROS.                             NC2014.2
016300     02  D2 PICTURE 9(5) VALUE 12345.                             NC2014.2
016400 01  IF-D22.                                                      NC2014.2
016500     02  D1 PICTURE A(2) VALUE "AB".                              NC2014.2
016600     02  D2 PICTURE A(4) VALUE "CDEF".                            NC2014.2
016700 01  IF-D35.                                                      NC2014.2
016800     02  AA PICTURE X(2).                                         NC2014.2
016900     88  A1 VALUE "AA".                                           NC2014.2
017000     88  A2 VALUE "AB".                                           NC2014.2
017100     02  BB PICTURE IS X(2).                                      NC2014.2
017200     88  B1 VALUE "CC".                                           NC2014.2
017300     88  B2 VALUE "CD".                                           NC2014.2
017400     02 BB-2 REDEFINES BB.                                        NC2014.2
017500     03  AAA PICTURE X.                                           NC2014.2
017600     88  AA1 VALUE "A".                                           NC2014.2
017700     88  AA2 VALUE "C".                                           NC2014.2
017800     03  BBB PICTURE X.                                           NC2014.2
017900     88  BB1    VALUE "B".                                        NC2014.2
018000     88  BB2 VALUE "D".                                           NC2014.2
018100 01  IF-D36  PICTURE X(120) VALUE IS    "ABCDEFGHIJKLMNOPQRSTUVWXYNC2014.2
018200-    "Z1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890ABCDEFGHIJKLMNC2014.2
018300-    "NOPQRSTUVWXYZ1234567890ABCDEFGHIJKL".                       NC2014.2
018400 01  IF-D40 PICTURE 9(5) VALUE 12345                              NC2014.2
018500              COMPUTATIONAL SYNCHRONIZED RIGHT.                   NC2014.2
018600     88 IF-D40A VALUE ZERO THRU 10000.                            NC2014.2
018700     88 IF-D40B VALUE 10001 THRU 99999.                           NC2014.2
018800     88 IF-D40C VALUE 99999.                                      NC2014.2
018900 01  PERFORM1  PICTURE XXX  VALUE SPACES.                         NC2014.2
019000 01  PERFORM2  PICTURE S999 VALUE 20.                             NC2014.2
019100 01  PERFORM3  PICTURE  9  VALUE  5.                              NC2014.2
019200 01  PERFORM4  PICTURE S99V9.                                     NC2014.2
019300 01  PERFORM5  PICTURE S99V9  VALUE 10.0.                         NC2014.2
019400 01  PERFORM6  PICTURE  99V9.                                     NC2014.2
019500 01  PERFORM7.                                                    NC2014.2
019600     02  PERFORM8  OCCURS 7 TIMES  PICTURE  99V9.                 NC2014.2
019700 01  PERFORM9  PICTURE 9   VALUE 3.                               NC2014.2
019800 01  PERFORM10  PICTURE  S9  VALUE -1.                            NC2014.2
019900 01  PERFORM11  PICTURE  99  VALUE 6.                             NC2014.2
020000 01  PERFORM12.                                                   NC2014.2
020100     02 PERFORM13  OCCURS 4 TIMES.                                NC2014.2
020200         03 PERFORM14  OCCURS 20 TIMES  PICTURE  99V9.            NC2014.2
020300         03 PERFORM15  OCCURS 10 TIMES.                           NC2014.2
020400             04 PERFORM16  OCCURS 5 TIMES  PICTURE 99V9.          NC2014.2
020500 01  PERFORM17          PICTURE 9(6) COMPUTATIONAL.               NC2014.2
020600 01  PERFORM18          PICTURE 9(6) COMPUTATIONAL.               NC2014.2
020700 01  PERFORM-KEY  PICTURE 9.                                      NC2014.2
020800 01  PERFORM-SEVEN-LEVEL-TABLE.                                   NC2014.2
020900   03   PFM71                OCCURS 2.                            NC2014.2
021000     05  PFM72               OCCURS 2.                            NC2014.2
021100       07  PFM73             OCCURS 2.                            NC2014.2
021200         09  PFM74           OCCURS 2.                            NC2014.2
021300           11  PFM75         OCCURS 2.                            NC2014.2
021400             13  PFM76       OCCURS 2.                            NC2014.2
021500               15  PFM77     OCCURS 2.                            NC2014.2
021600                 17  PFM77-1 PIC X.                               NC2014.2
021700 01  S1                      PIC S9(5) COMP.                      NC2014.2
021800 01  S2                      PIC S9(5) COMP.                      NC2014.2
021900 01  S3                      PIC S9(5) COMP.                      NC2014.2
022000 01  S4                      PIC S9(5) COMP.                      NC2014.2
022100 01  S5                      PIC S9(5) COMP.                      NC2014.2
022200 01  S6                      PIC S9(5) COMP.                      NC2014.2
022300 01  S7                      PIC S9(5) COMP.                      NC2014.2
022400 01  PFM-7-TOT               PIC S9(5) COMP.                      NC2014.2
022500 01  PFM-F4-23-TOT           PIC S9(5) COMP.                      NC2014.2
022600 01  PFM-A1                  PIC S9(5) COMP.                      NC2014.2
022700 01  PFM-B1                  PIC S9(5) COMP.                      NC2014.2
022800 01  FILLER-A.                                                    NC2014.2
022900   03  PFM-F4-24-A           PIC S9(3) COMP OCCURS 10.            NC2014.2
023000 01  FILLER-B.                                                    NC2014.2
023100   03  PFM-F4-24-B           PIC S9(3) COMP OCCURS 10.            NC2014.2
023200 01  FILLER-C.                                                    NC2014.2
023300   03  PFM-F4-24-C           PIC S9(3) COMP OCCURS 10.            NC2014.2
023400 01  RECEIVING-TABLE.                                             NC2014.2
023500     03 TBL-ELEMEN-A.                                             NC2014.2
023600         05 TBL-ELEMEN-B          PICTURE X(18).                  NC2014.2
023700         05 TBL-ELEMEN-C          PICTURE X(18).                  NC2014.2
023800     03  TBL-ELEMEN-D.                                            NC2014.2
023900         05 TBL-ELEMEN-E          PICTURE X OCCURS 36 TIMES.      NC2014.2
024000 01  LITERAL-SPLITTER.                                            NC2014.2
024100     02 PART1                     PICTURE X(20).                  NC2014.2
024200     02 PART2                     PICTURE X(20).                  NC2014.2
024300     02 PART3                     PICTURE X(20).                  NC2014.2
024400     02 PART4                     PICTURE X(20).                  NC2014.2
024500 01  LITERAL-TABLE REDEFINES LITERAL-SPLITTER.                    NC2014.2
024600     02 80PARTS                   PICTURE X      OCCURS 80 TIMES. NC2014.2
024700 01  GRP-FOR-88-LEVELS.                                           NC2014.2
024800     03 WRK-DS-02V00-COND         PICTURE 99.                     NC2014.2
024900         88 COND-1                VALUE IS 01 THRU 05.            NC2014.2
025000         88 COND-2                VALUES ARE 06 THRU 10           NC2014.2
025100                                           16 THRU 20  00.        NC2014.2
025200         88 COND-3                VALUES 11 THRU 15.              NC2014.2
025300 01  GRP-MOVE-CONSTANTS.                                          NC2014.2
025400     03 GRP-GROUP-MOVE-FROM.                                      NC2014.2
025500         04 GRP-ALPHABETIC.                                       NC2014.2
025600             05 ALPHABET-AN-00026 PICTURE A(26)                   NC2014.2
025700                        VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ".       NC2014.2
025800         04 GRP-NUMERIC.                                          NC2014.2
025900             05 DIGITS-DV-10V00   PICTURE 9(10) VALUE 0123456789. NC2014.2
026000         05 DIGITS-DU-06V04-S REDEFINES DIGITS-DV-10V00           NC2014.2
026100                                  PICTURE 9(6)V9999.              NC2014.2
026200         04 GRP-ALPHANUMERIC.                                     NC2014.2
026300             05 ALPHANUMERIC-XN-00049 PICTURE X(50)               NC2014.2
026400     VALUE  "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-><=$,;.()/* 0123456789". NC2014.2
026500             05 FILLER                PICTURE X  VALUE QUOTE.     NC2014.2
026600 01  GRP-FOR-2N058.                                               NC2014.2
026700     02 SUB-GRP-FOR-2N058-A.                                      NC2014.2
026800         03 ELEM-FOR-2N058-A PICTURE 999  VALUE ZEROES.           NC2014.2
026900         03 ELEM-FOR-2N058-B PICTURE XXX  VALUE ZEROS.            NC2014.2
027000         03 ELEM-FOR-2N058-C PICTURE XXX  VALUE SPACES.           NC2014.2
027100         03 ELEM-FOR-2N058-D PICTURE X(6) VALUE ALL "ABC".        NC2014.2
027200         03 ELEM-FOR-2N058-E PICTURE XXX  VALUE ALL "Z".          NC2014.2
027300         03 ELEM-FOR-2N058-F PICTURE XXX  VALUE ALL SPACES.       NC2014.2
027400         03 ELEM-FOR-2N058-G PICTURE XXX  VALUE ALL ZEROES.       NC2014.2
027500         03 ELEM-FOR-2N058-H PICTURE 999  VALUE ALL ZEROS.        NC2014.2
027600         03 ELEM-FOR-2N058-I PICTURE XXX  VALUE QUOTES.           NC2014.2
027700         03 ELEM-FOR-2N058-J PICTURE XXX  VALUE ALL QUOTES.       NC2014.2
027800         03 ELEM-FOR-2N058-K PICTURE XXX  VALUE ALL HIGH-VALUES.  NC2014.2
027900         03 ELEM-FOR-2N058-L PICTURE XXX  VALUE ALL LOW-VALUES.   NC2014.2
028000         03 ELEM-FOR-2N058-M PICTURE XXX  VALUE HIGH-VALUES.      NC2014.2
028100         03 ELEM-FOR-2N058-N PICTURE XXX  VALUE LOW-VALUES.       NC2014.2
028200     02 SUB-GRP-FOR-2N058-B.                                      NC2014.2
028300         03 SUB-SUB-BA.                                           NC2014.2
028400             04 ELEM-FOR-2N058-A  PICTURE 999.                    NC2014.2
028500             04 ELEM-FOR-2N058-B  PICTURE XXX.                    NC2014.2
028600             04 ELEM-FOR-2N058-C  PICTURE XXX.                    NC2014.2
028700             04 ELEM-FOR-2N058-D  PICTURE X(6).                   NC2014.2
028800         03 SUB-SUB-BB.                                           NC2014.2
028900             04 ELEM-FOR-2N058-E  PICTURE XXX.                    NC2014.2
029000             04 ELEM-FOR-2N058-F  PICTURE XXX.                    NC2014.2
029100             04 ELEM-FOR-2N058-G  PICTURE XXX.                    NC2014.2
029200             04 ELEM-FOR-2N058-H  PICTURE 999.                    NC2014.2
029300         03 SUB-SUB-BC.                                           NC2014.2
029400             04 ELEM-FOR-2N058-I  PICTURE XXX.                    NC2014.2
029500             04 ELEM-FOR-2N058-J  PICTURE XXX.                    NC2014.2
029600             04 ELEM-FOR-2N058-K  PICTURE XXX.                    NC2014.2
029700             04 ELEM-FOR-2N058-L  PICTURE XXX.                    NC2014.2
029800             04 ELEM-FOR-2N058-M  PICTURE XXX.                    NC2014.2
029900             04 ELEM-FOR-2N058-N  PICTURE XXX.                    NC2014.2
030000 01  CHARACTER-BREAKDOWN-S.                                       NC2014.2
030100     02   FIRST-20S PICTURE X(20).                                NC2014.2
030200     02  SECOND-20S PICTURE X(20).                                NC2014.2
030300     02   THIRD-20S PICTURE X(20).                                NC2014.2
030400     02  FOURTH-20S PICTURE X(20).                                NC2014.2
030500     02   FIFTH-20S PICTURE X(20).                                NC2014.2
030600     02   SIXTH-20S PICTURE X(20).                                NC2014.2
030700     02 SEVENTH-20S PICTURE X(20).                                NC2014.2
030800     02  EIGHTH-20S PICTURE X(20).                                NC2014.2
030900     02   NINTH-20S PICTURE X(20).                                NC2014.2
031000     02   TENTH-20S PICTURE X(20).                                NC2014.2
031100 01  CHARACTER-BREAKDOWN-R.                                       NC2014.2
031200     02   FIRST-20R PICTURE X(20).                                NC2014.2
031300     02  SECOND-20R PICTURE X(20).                                NC2014.2
031400     02   THIRD-20R PICTURE X(20).                                NC2014.2
031500     02  FOURTH-20R PICTURE X(20).                                NC2014.2
031600     02   FIFTH-20R PICTURE X(20).                                NC2014.2
031700     02   SIXTH-20R PICTURE X(20).                                NC2014.2
031800     02 SEVENTH-20R PICTURE X(20).                                NC2014.2
031900     02  EIGHTH-20R PICTURE X(20).                                NC2014.2
032000     02   NINTH-20R PICTURE X(20).                                NC2014.2
032100     02   TENTH-20R PICTURE X(20).                                NC2014.2
032200 01  TABLE-80.                                                    NC2014.2
032300     02  ELMT OCCURS 3 TIMES PIC 9.                               NC2014.2
032400     88  A80  VALUES ARE ZERO THRU 7.                             NC2014.2
032500     88  B80  VALUE 8.                                            NC2014.2
032600     88  C80  VALUES ARE 7, 8 THROUGH 9.                          NC2014.2
032700                                                                  NC2014.2
032800 01  TABLE-86.                                                    NC2014.2
032900     88  A86  VALUE "ABC".                                        NC2014.2
033000     88  B86  VALUE "ABCABC".                                     NC2014.2
033100     88  C86  VALUE "   ABC".                                     NC2014.2
033200     02  DATANAME-86  PIC XXX  VALUE "ABC".                       NC2014.2
033300     02  DNAME-86.                                                NC2014.2
033400         03  FILLER  PIC X  VALUE "A".                            NC2014.2
033500         03  FILLER  PIC X  VALUE "B".                            NC2014.2
033600         03  FILLER  PIC X   VALUE "C".                           NC2014.2
033700 01  FIGCON-DATA.                                                 NC2014.2
033800     02 SPACE-X         PICTURE X(10) VALUE "          ".         NC2014.2
033900     02 QUOTE-X         PICTURE X(5)  VALUE QUOTE.                NC2014.2
034000     02 LOW-VAL         PICTURE X(5)  VALUE LOW-VALUE.            NC2014.2
034100     02 ABC PICTURE XXX VALUE "ABC".                              NC2014.2
034200     02 ONE23           PICTURE 9999  VALUE 123.                  NC2014.2
034300     02 ZERO-C          PICTURE 9(10) VALUE 0 COMPUTATIONAL.      NC2014.2
034400     02 ZERO-D          PICTURE 9     VALUE ZERO USAGE DISPLAY.   NC2014.2
034500 01  TEST-RESULTS.                                                NC2014.2
034600     02 FILLER                   PIC X      VALUE SPACE.          NC2014.2
034700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2014.2
034800     02 FILLER                   PIC X      VALUE SPACE.          NC2014.2
034900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2014.2
035000     02 FILLER                   PIC X      VALUE SPACE.          NC2014.2
035100     02  PAR-NAME.                                                NC2014.2
035200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2014.2
035300       03  PARDOT-X              PIC X      VALUE SPACE.          NC2014.2
035400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2014.2
035500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2014.2
035600     02 RE-MARK                  PIC X(61).                       NC2014.2
035700 01  TEST-COMPUTED.                                               NC2014.2
035800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2014.2
035900     02 FILLER                   PIC X(17)  VALUE                 NC2014.2
036000            "       COMPUTED=".                                   NC2014.2
036100     02 COMPUTED-X.                                               NC2014.2
036200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2014.2
036300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2014.2
036400                                 PIC -9(9).9(9).                  NC2014.2
036500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2014.2
036600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2014.2
036700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2014.2
036800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2014.2
036900         04 COMPUTED-18V0                    PIC -9(18).          NC2014.2
037000         04 FILLER                           PIC X.               NC2014.2
037100     03 FILLER PIC X(50) VALUE SPACE.                             NC2014.2
037200 01  TEST-CORRECT.                                                NC2014.2
037300     02 FILLER PIC X(30) VALUE SPACE.                             NC2014.2
037400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2014.2
037500     02 CORRECT-X.                                                NC2014.2
037600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2014.2
037700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2014.2
037800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2014.2
037900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2014.2
038000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2014.2
038100     03      CR-18V0 REDEFINES CORRECT-A.                         NC2014.2
038200         04 CORRECT-18V0                     PIC -9(18).          NC2014.2
038300         04 FILLER                           PIC X.               NC2014.2
038400     03 FILLER PIC X(2) VALUE SPACE.                              NC2014.2
038500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2014.2
038600 01  CCVS-C-1.                                                    NC2014.2
038700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2014.2
038800-    "SS  PARAGRAPH-NAME                                          NC2014.2
038900-    "       REMARKS".                                            NC2014.2
039000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2014.2
039100 01  CCVS-C-2.                                                    NC2014.2
039200     02 FILLER                     PIC X        VALUE SPACE.      NC2014.2
039300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2014.2
039400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2014.2
039500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2014.2
039600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2014.2
039700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2014.2
039800 01  REC-CT                        PIC 99       VALUE ZERO.       NC2014.2
039900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2014.2
040000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2014.2
040100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2014.2
040200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2014.2
040300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2014.2
040400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2014.2
040500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2014.2
040600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2014.2
040700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2014.2
040800 01  CCVS-H-1.                                                    NC2014.2
040900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2014.2
041000     02  FILLER                    PIC X(42)    VALUE             NC2014.2
041100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2014.2
041200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2014.2
041300 01  CCVS-H-2A.                                                   NC2014.2
041400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2014.2
041500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2014.2
041600   02  FILLER                        PIC XXXX   VALUE             NC2014.2
041700     "4.2 ".                                                      NC2014.2
041800   02  FILLER                        PIC X(28)  VALUE             NC2014.2
041900            " COPY - NOT FOR DISTRIBUTION".                       NC2014.2
042000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2014.2
042100                                                                  NC2014.2
042200 01  CCVS-H-2B.                                                   NC2014.2
042300   02  FILLER                        PIC X(15)  VALUE             NC2014.2
042400            "TEST RESULT OF ".                                    NC2014.2
042500   02  TEST-ID                       PIC X(9).                    NC2014.2
042600   02  FILLER                        PIC X(4)   VALUE             NC2014.2
042700            " IN ".                                               NC2014.2
042800   02  FILLER                        PIC X(12)  VALUE             NC2014.2
042900     " HIGH       ".                                              NC2014.2
043000   02  FILLER                        PIC X(22)  VALUE             NC2014.2
043100            " LEVEL VALIDATION FOR ".                             NC2014.2
043200   02  FILLER                        PIC X(58)  VALUE             NC2014.2
043300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2014.2
043400 01  CCVS-H-3.                                                    NC2014.2
043500     02  FILLER                      PIC X(34)  VALUE             NC2014.2
043600            " FOR OFFICIAL USE ONLY    ".                         NC2014.2
043700     02  FILLER                      PIC X(58)  VALUE             NC2014.2
043800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2014.2
043900     02  FILLER                      PIC X(28)  VALUE             NC2014.2
044000            "  COPYRIGHT   1985 ".                                NC2014.2
044100 01  CCVS-E-1.                                                    NC2014.2
044200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2014.2
044300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2014.2
044400     02 ID-AGAIN                     PIC X(9).                    NC2014.2
044500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2014.2
044600 01  CCVS-E-2.                                                    NC2014.2
044700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2014.2
044800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2014.2
044900     02 CCVS-E-2-2.                                               NC2014.2
045000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2014.2
045100         03 FILLER                   PIC X      VALUE SPACE.      NC2014.2
045200         03 ENDER-DESC               PIC X(44)  VALUE             NC2014.2
045300            "ERRORS ENCOUNTERED".                                 NC2014.2
045400 01  CCVS-E-3.                                                    NC2014.2
045500     02  FILLER                      PIC X(22)  VALUE             NC2014.2
045600            " FOR OFFICIAL USE ONLY".                             NC2014.2
045700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2014.2
045800     02  FILLER                      PIC X(58)  VALUE             NC2014.2
045900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2014.2
046000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2014.2
046100     02 FILLER                       PIC X(15)  VALUE             NC2014.2
046200             " COPYRIGHT 1985".                                   NC2014.2
046300 01  CCVS-E-4.                                                    NC2014.2
046400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2014.2
046500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2014.2
046600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2014.2
046700     02 FILLER                       PIC X(40)  VALUE             NC2014.2
046800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2014.2
046900 01  XXINFO.                                                      NC2014.2
047000     02 FILLER                       PIC X(19)  VALUE             NC2014.2
047100            "*** INFORMATION ***".                                NC2014.2
047200     02 INFO-TEXT.                                                NC2014.2
047300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2014.2
047400       04 XXCOMPUTED                 PIC X(20).                   NC2014.2
047500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2014.2
047600       04 XXCORRECT                  PIC X(20).                   NC2014.2
047700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2014.2
047800 01  HYPHEN-LINE.                                                 NC2014.2
047900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2014.2
048000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2014.2
048100-    "*****************************************".                 NC2014.2
048200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2014.2
048300-    "******************************".                            NC2014.2
048400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2014.2
048500     "NC201A".                                                    NC2014.2
048600 PROCEDURE DIVISION.                                              NC2014.2
048700 CCVS1 SECTION.                                                   NC2014.2
048800 OPEN-FILES.                                                      NC2014.2
048900     OPEN     OUTPUT PRINT-FILE.                                  NC2014.2
049000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2014.2
049100     MOVE    SPACE TO TEST-RESULTS.                               NC2014.2
049200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2014.2
049300     GO TO CCVS1-EXIT.                                            NC2014.2
049400 CLOSE-FILES.                                                     NC2014.2
049500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2014.2
049600 TERMINATE-CCVS.                                                  NC2014.2
049700*    EXIT PROGRAM.                                                NC2014.2
049800*TERMINATE-CALL.                                                  NC2014.2
049900     STOP     RUN.                                                NC2014.2
050000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2014.2
050100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2014.2
050200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2014.2
050300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2014.2
050400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2014.2
050500 PRINT-DETAIL.                                                    NC2014.2
050600     IF REC-CT NOT EQUAL TO ZERO                                  NC2014.2
050700             MOVE "." TO PARDOT-X                                 NC2014.2
050800             MOVE REC-CT TO DOTVALUE.                             NC2014.2
050900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2014.2
051000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2014.2
051100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2014.2
051200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2014.2
051300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2014.2
051400     MOVE SPACE TO CORRECT-X.                                     NC2014.2
051500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2014.2
051600     MOVE     SPACE TO RE-MARK.                                   NC2014.2
051700 HEAD-ROUTINE.                                                    NC2014.2
051800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2014.2
051900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2014.2
052000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2014.2
052100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2014.2
052200 COLUMN-NAMES-ROUTINE.                                            NC2014.2
052300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2014.2
052400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2014.2
052500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2014.2
052600 END-ROUTINE.                                                     NC2014.2
052700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2014.2
052800 END-RTN-EXIT.                                                    NC2014.2
052900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2014.2
053000 END-ROUTINE-1.                                                   NC2014.2
053100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2014.2
053200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2014.2
053300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2014.2
053400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2014.2
053500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2014.2
053600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2014.2
053700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2014.2
053800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2014.2
053900  END-ROUTINE-12.                                                 NC2014.2
054000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2014.2
054100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2014.2
054200         MOVE "NO " TO ERROR-TOTAL                                NC2014.2
054300         ELSE                                                     NC2014.2
054400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2014.2
054500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2014.2
054600     PERFORM WRITE-LINE.                                          NC2014.2
054700 END-ROUTINE-13.                                                  NC2014.2
054800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2014.2
054900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2014.2
055000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2014.2
055100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2014.2
055200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2014.2
055300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2014.2
055400          MOVE "NO " TO ERROR-TOTAL                               NC2014.2
055500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2014.2
055600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2014.2
055700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2014.2
055800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2014.2
055900 WRITE-LINE.                                                      NC2014.2
056000     ADD 1 TO RECORD-COUNT.                                       NC2014.2
056100*    IF RECORD-COUNT GREATER 50                                   NC2014.2
056200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2014.2
056300*        MOVE SPACE TO DUMMY-RECORD                               NC2014.2
056400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2014.2
056500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2014.2
056600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2014.2
056700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2014.2
056800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2014.2
056900*        MOVE ZERO TO RECORD-COUNT.                               NC2014.2
057000     PERFORM WRT-LN.                                              NC2014.2
057100 WRT-LN.                                                          NC2014.2
057200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2014.2
057300     MOVE SPACE TO DUMMY-RECORD.                                  NC2014.2
057400 BLANK-LINE-PRINT.                                                NC2014.2
057500     PERFORM WRT-LN.                                              NC2014.2
057600 FAIL-ROUTINE.                                                    NC2014.2
057700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2014.2
057800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2014.2
057900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2014.2
058000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2014.2
058100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2014.2
058200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2014.2
058300     GO TO  FAIL-ROUTINE-EX.                                      NC2014.2
058400 FAIL-ROUTINE-WRITE.                                              NC2014.2
058500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2014.2
058600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2014.2
058700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2014.2
058800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2014.2
058900 FAIL-ROUTINE-EX. EXIT.                                           NC2014.2
059000 BAIL-OUT.                                                        NC2014.2
059100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2014.2
059200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2014.2
059300 BAIL-OUT-WRITE.                                                  NC2014.2
059400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2014.2
059500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2014.2
059600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2014.2
059700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2014.2
059800 BAIL-OUT-EX. EXIT.                                               NC2014.2
059900 CCVS1-EXIT.                                                      NC2014.2
060000     EXIT.                                                        NC2014.2
060100 SECT-NC201A-001 SECTION.                                         NC2014.2
060200 PFM-INIT-F3-1.                                                   NC2014.2
060300     MOVE "PFM-TEST-F3-1" TO PAR-NAME.                            NC2014.2
060400     MOVE   "VI-112 6.20.4 GR10(d)" TO ANSI-REFERENCE.            NC2014.2
060500     MOVE "PERFORM UNTIL" TO FEATURE.                             NC2014.2
060600     MOVE 1 TO PERFORM2.                                          NC2014.2
060700 PFM-TEST-F3-0.                                                   NC2014.2
060800     PERFORM  PFM-A THRU PFM-AA UNTIL PERFORM2 EQUAL TO 48.       NC2014.2
060900*    NOTE IN THIS TEST THE CONDITION IS NOT SATISFIED             NC2014.2
061000*    ORIGINALLY WHEN THE PERFORM IS ENTERED.                      NC2014.2
061100 PFM-TEST-F3-1.                                                   NC2014.2
061200     IF PERFORM2 EQUAL TO 48 PERFORM PASS                         NC2014.2
061300                             GO TO PFM-WRITE-F3-1.                NC2014.2
061400     GO TO PFM-FAIL-F3-1.                                         NC2014.2
061500 PFM-DELETE-F3-1.                                                 NC2014.2
061600     PERFORM DE-LETE.                                             NC2014.2
061700     GO TO PFM-WRITE-F3-1.                                        NC2014.2
061800 PFM-FAIL-F3-1.                                                   NC2014.2
061900     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
062000     MOVE 48 TO CORRECT-N.                                        NC2014.2
062100     PERFORM FAIL.                                                NC2014.2
062200 PFM-WRITE-F3-1.                                                  NC2014.2
062300     PERFORM PRINT-DETAIL.                                        NC2014.2
062400*                                                                 NC2014.2
062500 PFM-INIT-F3-2.                                                   NC2014.2
062600     MOVE "PFM-TEST-F3-2" TO PAR-NAME.                            NC2014.2
062700     MOVE 50 TO PERFORM2.                                         NC2014.2
062800*    NOTE IN THIS TEST CONDITION IS SATISFIED WHEN PERFORM IS     NC2014.2
062900*    ENTERED AND CONTROL SHOULD NOT BE PASSED TO PFM-C.           NC2014.2
063000 PFM-TEST-F3-2.                                                   NC2014.2
063100     PERFORM PFM-C UNTIL PERFORM2 GREATER THAN 25.                NC2014.2
063200     IF PERFORM2 EQUAL TO 50 PERFORM PASS                         NC2014.2
063300                             GO TO PFM-WRITE-F3-2.                NC2014.2
063400     GO TO PFM-FAIL-F3-2.                                         NC2014.2
063500 PFM-DELETE-F3-2.                                                 NC2014.2
063600     PERFORM DE-LETE.                                             NC2014.2
063700     GO TO PFM-WRITE-F3-2.                                        NC2014.2
063800 PFM-FAIL-F3-2.                                                   NC2014.2
063900     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
064000     MOVE 50 TO CORRECT-N.                                        NC2014.2
064100     PERFORM FAIL.                                                NC2014.2
064200 PFM-WRITE-F3-2.                                                  NC2014.2
064300     PERFORM PRINT-DETAIL.                                        NC2014.2
064400*                                                                 NC2014.2
064500 PFM-INIT-F4-1.                                                   NC2014.2
064600     MOVE "PFM-TEST-F4-1" TO PAR-NAME.                            NC2014.2
064700     MOVE "PERFORM VARYING" TO FEATURE.                           NC2014.2
064800 PFM-TEST-F4-1.                                                   NC2014.2
064900     PERFORM PFM-E VARYING PERFORM4 FROM PERFORM5 BY -0.2         NC2014.2
065000         UNTIL PERFORM4 LESS THAN 9.0.                            NC2014.2
065100     IF PERFORM4 EQUAL TO 8.8 AND PERFORM6 EQUAL TO 12.5          NC2014.2
065200               PERFORM PASS                                       NC2014.2
065300               GO TO PFM-WRITE-F4-1.                              NC2014.2
065400     GO TO PFM-FAIL-F4-1.                                         NC2014.2
065500 PFM-DELETE-F4-1.                                                 NC2014.2
065600     PERFORM PRINT-DETAIL.                                        NC2014.2
065700     GO TO PFM-WRITE-F4-1.                                        NC2014.2
065800 PFM-FAIL-F4-1.                                                   NC2014.2
065900     MOVE PERFORM4 TO COMPUTED-N.                                 NC2014.2
066000     MOVE 8.8 TO CORRECT-N.                                       NC2014.2
066100     PERFORM FAIL.                                                NC2014.2
066200     PERFORM PRINT-DETAIL.                                        NC2014.2
066300     MOVE SPACE TO P-OR-F.                                        NC2014.2
066400     MOVE PERFORM6 TO COMPUTED-N.                                 NC2014.2
066500     MOVE 12.5 TO CORRECT-N.                                      NC2014.2
066600 PFM-WRITE-F4-1.                                                  NC2014.2
066700     PERFORM PRINT-DETAIL.                                        NC2014.2
066800*                                                                 NC2014.2
066900 PFM-INIT-F4-2.                                                   NC2014.2
067000     MOVE "PFM-TEST-F4-2" TO PAR-NAME.                            NC2014.2
067100     MOVE 5.5 TO PERFORM4.                                        NC2014.2
067200     MOVE 5.5 TO PERFORM8 (7).                                    NC2014.2
067300 PFM-TEST-F4-2.                                                   NC2014.2
067400*    NOTE IN THIS TEST ONE SUBSCRIPT IS VARIED.                   NC2014.2
067500*    NOTE  THIS ALSO TESTS THAT WHEN THE CONDITION IS TRUE,       NC2014.2
067600*    CONTROL FALLS THRU AND THE PROCEDURE IS NOT EXECUTED.        NC2014.2
067700     PERFORM PFM-G VARYING PERFORM3 FROM 1 BY 2 UNTIL             NC2014.2
067800     PERFORM3 GREATER THAN 5.                                     NC2014.2
067900     IF PERFORM8 (1) EQUAL TO 13.5 AND PERFORM8 (3) EQUAL TO 13.8 NC2014.2
068000     AND PERFORM8 (5) EQUAL TO 14.1 AND PERFORM8 (7) EQUAL TO     NC2014.2
068100     5.5 AND PERFORM3 EQUAL TO 7                                  NC2014.2
068200               PERFORM PASS                                       NC2014.2
068300               GO TO PFM-WRITE-F4-2.                              NC2014.2
068400*    NOTE THE OCCURS CLAUSE IS NEEDED IN THE DATA DESCRIPTION     NC2014.2
068500*        FOR THESE PERFORM TESTS --- MORE EXHAUSTIVE TESTS OF THE NC2014.2
068600*        OCCURS CLAUSE CAN BE FOUND IN THE TABLE HANDLING TESTS.  NC2014.2
068700     GO TO PFM-FAIL-F4-2.                                         NC2014.2
068800 PFM-DELETE-F4-2.                                                 NC2014.2
068900     PERFORM DE-LETE.                                             NC2014.2
069000     GO TO PFM-WRITE-F4-2.                                        NC2014.2
069100 PFM-FAIL-F4-2.                                                   NC2014.2
069200     MOVE PERFORM8 (1) TO COMPUTED-N.                             NC2014.2
069300     MOVE 13.5 TO CORRECT-N.                                      NC2014.2
069400     PERFORM FAIL.                                                NC2014.2
069500     PERFORM PRINT-DETAIL.                                        NC2014.2
069600     MOVE SPACE TO P-OR-F.                                        NC2014.2
069700     MOVE PERFORM8 (3) TO COMPUTED-N.                             NC2014.2
069800     MOVE 13.8 TO CORRECT-N.                                      NC2014.2
069900     MOVE "PFM-TEST-F4-2" TO PAR-NAME.                            NC2014.2
070000     PERFORM PRINT-DETAIL.                                        NC2014.2
070100     MOVE PERFORM8 (5) TO COMPUTED-N.                             NC2014.2
070200     MOVE 14.1 TO CORRECT-N.                                      NC2014.2
070300     MOVE "PFM-TEST-F4-2" TO PAR-NAME.                            NC2014.2
070400     PERFORM PRINT-DETAIL.                                        NC2014.2
070500     MOVE PERFORM8 (7) TO COMPUTED-N.                             NC2014.2
070600     MOVE  5.5 TO CORRECT-N.                                      NC2014.2
070700     MOVE "PFM-TEST-F4-2" TO PAR-NAME.                            NC2014.2
070800     PERFORM PRINT-DETAIL.                                        NC2014.2
070900     MOVE PERFORM3 TO COMPUTED-N.                                 NC2014.2
071000     MOVE 7 TO CORRECT-N.                                         NC2014.2
071100     MOVE "PFM-TEST-F4-2" TO PAR-NAME.                            NC2014.2
071200 PFM-WRITE-F4-2.                                                  NC2014.2
071300     PERFORM PRINT-DETAIL.                                        NC2014.2
071400*                                                                 NC2014.2
071500 PFM-INIT-F4-3.                                                   NC2014.2
071600     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
071700     MOVE 1.5 TO PERFORM4.                                        NC2014.2
071800 PFM-TEST-F4-3.                                                   NC2014.2
071900*    NOTE IN THIS TEST TWO SUBSCRIPTS ARE VARIED.                 NC2014.2
072000     PERFORM PFM-I THRU PFM-J VARYING PERFORM3                    NC2014.2
072100     FROM PERFORM9 BY PERFORM10 UNTIL PERFORM3 EQUAL TO 1         NC2014.2
072200     AFTER PERFORM2 FROM 2 BY PERFORM11 UNTIL PERFORM2            NC2014.2
072300     GREATER THAN 20.                                             NC2014.2
072400     IF PERFORM14 (3, 2) EQUAL TO 15.0 AND PERFORM14 (3, 8)       NC2014.2
072500     EQUAL TO 20.0 AND PERFORM14 (3, 14) EQUAL TO 25.0            NC2014.2
072600     AND PERFORM14 (3, 20) EQUAL TO 30.0 AND PERFORM14 (2, 2)     NC2014.2
072700     EQUAL TO 35.0 MOVE "A" TO XRAY.                              NC2014.2
072800     IF PERFORM14 (2, 8) EQUAL TO 40.0 AND                        NC2014.2
072900     PERFORM14 (2, 14) EQUAL TO 45.0 AND PERFORM14 (2, 20)        NC2014.2
073000     EQUAL TO 50.0 AND PERFORM2 EQUAL TO 2 AND PERFORM3           NC2014.2
073100     EQUAL TO 1 AND XRAY EQUAL TO "A" PERFORM PASS                NC2014.2
073200         GO TO PFM-WRITE-F4-3.                                    NC2014.2
073300     GO TO PFM-FAIL-F4-3.                                         NC2014.2
073400 PFM-DELETE-F4-3.                                                 NC2014.2
073500     PERFORM DE-LETE.                                             NC2014.2
073600     GO TO PFM-WRITE-F4-3.                                        NC2014.2
073700 PFM-FAIL-F4-3.                                                   NC2014.2
073800     MOVE PERFORM14 (3,  2) TO COMPUTED-N.                        NC2014.2
073900     MOVE 15.0 TO CORRECT-N.                                      NC2014.2
074000     PERFORM FAIL.                                                NC2014.2
074100     PERFORM PRINT-DETAIL.                                        NC2014.2
074200     MOVE SPACE TO P-OR-F.                                        NC2014.2
074300     MOVE PERFORM14 (3,  8) TO COMPUTED-N.                        NC2014.2
074400     MOVE 20.0 TO CORRECT-N.                                      NC2014.2
074500     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
074600     PERFORM PRINT-DETAIL.                                        NC2014.2
074700     MOVE PERFORM14 (3, 14) TO COMPUTED-N.                        NC2014.2
074800     MOVE 25.0 TO CORRECT-N.                                      NC2014.2
074900     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
075000     PERFORM PRINT-DETAIL.                                        NC2014.2
075100     MOVE PERFORM14 (3, 20) TO COMPUTED-N.                        NC2014.2
075200     MOVE 30.0 TO CORRECT-N.                                      NC2014.2
075300     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
075400     PERFORM PRINT-DETAIL.                                        NC2014.2
075500     MOVE PERFORM14 (2,  2) TO COMPUTED-N.                        NC2014.2
075600     MOVE 35.0 TO CORRECT-N.                                      NC2014.2
075700     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
075800     PERFORM PRINT-DETAIL.                                        NC2014.2
075900     MOVE PERFORM14 (2,  8) TO COMPUTED-N.                        NC2014.2
076000     MOVE 40.0 TO CORRECT-N.                                      NC2014.2
076100     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
076200     PERFORM PRINT-DETAIL.                                        NC2014.2
076300     MOVE PERFORM14 (2, 14) TO COMPUTED-N.                        NC2014.2
076400     MOVE 45.0 TO CORRECT-N.                                      NC2014.2
076500     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
076600     PERFORM PRINT-DETAIL.                                        NC2014.2
076700     MOVE PERFORM14 (2, 20) TO COMPUTED-N.                        NC2014.2
076800     MOVE 50.0 TO CORRECT-N.                                      NC2014.2
076900     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
077000     PERFORM PRINT-DETAIL.                                        NC2014.2
077100     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
077200     MOVE    2 TO CORRECT-N.                                      NC2014.2
077300     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
077400     PERFORM PRINT-DETAIL.                                        NC2014.2
077500     MOVE PERFORM3 TO COMPUTED-N.                                 NC2014.2
077600     MOVE    1 TO CORRECT-N.                                      NC2014.2
077700     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
077800     PERFORM PRINT-DETAIL.                                        NC2014.2
077900     MOVE XRAY TO COMPUTED-A.                                     NC2014.2
078000     MOVE "A" TO CORRECT-A.                                       NC2014.2
078100     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2014.2
078200 PFM-WRITE-F4-3.                                                  NC2014.2
078300     PERFORM PRINT-DETAIL.                                        NC2014.2
078400*                                                                 NC2014.2
078500 PFM-INIT-F4-4.                                                   NC2014.2
078600     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
078700     MOVE 2 TO PERFORM9.                                          NC2014.2
078800     MOVE 2 TO PERFORM10.                                         NC2014.2
078900 PFM-TEST-F4-4.                                                   NC2014.2
079000*    NOTE IN THIS TEST THREE SUBSCRIPTS ARE VARIED.               NC2014.2
079100     PERFORM PFM-L VARYING PERFORM3 FROM PERFORM9 BY 2            NC2014.2
079200     UNTIL PERFORM3 GREATER THAN 4 AFTER PERFORM2 FROM 10         NC2014.2
079300     BY -5 UNTIL PERFORM2 EQUAL TO 0  AFTER PERFORM11             NC2014.2
079400     FROM 3 BY PERFORM10 UNTIL PERFORM11 GREATER THAN 5.          NC2014.2
079500     IF PERFORM16 (2, 10, 3) EQUAL TO 5.0 AND PERFORM16 (2, 10, 5)NC2014.2
079600     EQUAL TO 5.7 AND PERFORM16 (2, 5, 3) EQUAL TO 6.4 AND        NC2014.2
079700     PERFORM16 (2, 5, 5) EQUAL TO 7.1 AND PERFORM16 (4, 10, 3)    NC2014.2
079800     EQUAL TO 7.8 AND PERFORM16 (4, 10, 5) EQUAL TO 8.5           NC2014.2
079900     MOVE "B" TO XRAY. IF                                         NC2014.2
080000     PERFORM16 (4, 5, 3) EQUAL TO 9.2 AND PERFORM16 (4, 5, 5)     NC2014.2
080100     EQUAL TO 9.9 AND PERFORM11 EQUAL TO 3 AND PERFORM2 EQUAL     NC2014.2
080200     TO 10 AND PERFORM3 EQUAL TO 6 AND XRAY EQUAL TO "B"          NC2014.2
080300     PERFORM PASS GO TO PFM-WRITE-F4-4.                           NC2014.2
080400     GO TO PFM-FAIL-F4-4.                                         NC2014.2
080500 PFM-DELETE-F4-4.                                                 NC2014.2
080600     PERFORM DE-LETE.                                             NC2014.2
080700     GO TO PFM-WRITE-F4-4.                                        NC2014.2
080800 PFM-FAIL-F4-4.                                                   NC2014.2
080900     MOVE PERFORM16 (2, 10, 3) TO COMPUTED-N.                     NC2014.2
081000     MOVE 5.0 TO CORRECT-N.                                       NC2014.2
081100     PERFORM FAIL.                                                NC2014.2
081200     PERFORM PRINT-DETAIL.                                        NC2014.2
081300     MOVE SPACE TO P-OR-F.                                        NC2014.2
081400     MOVE PERFORM16 (2, 10, 3) TO COMPUTED-N.                     NC2014.2
081500     MOVE 5.0 TO CORRECT-N.                                       NC2014.2
081600     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
081700     PERFORM PRINT-DETAIL.                                        NC2014.2
081800     MOVE PERFORM16 (2, 10, 5) TO COMPUTED-N.                     NC2014.2
081900     MOVE 5.7 TO CORRECT-N.                                       NC2014.2
082000     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
082100     PERFORM PRINT-DETAIL.                                        NC2014.2
082200     MOVE PERFORM16 (2,  5, 3) TO COMPUTED-N.                     NC2014.2
082300     MOVE 6.4 TO CORRECT-N.                                       NC2014.2
082400     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
082500     PERFORM PRINT-DETAIL.                                        NC2014.2
082600     MOVE PERFORM16 (2,  5, 5) TO COMPUTED-N.                     NC2014.2
082700     MOVE 7.1 TO CORRECT-N.                                       NC2014.2
082800     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
082900     PERFORM PRINT-DETAIL.                                        NC2014.2
083000     MOVE PERFORM16 (4, 10, 3) TO COMPUTED-N.                     NC2014.2
083100     MOVE 7.8 TO CORRECT-N.                                       NC2014.2
083200     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
083300     PERFORM PRINT-DETAIL.                                        NC2014.2
083400     MOVE PERFORM16 (4, 10, 5) TO COMPUTED-N.                     NC2014.2
083500     MOVE 8.5 TO CORRECT-N.                                       NC2014.2
083600     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
083700     PERFORM PRINT-DETAIL.                                        NC2014.2
083800     MOVE PERFORM16 (4,  5, 3) TO COMPUTED-N.                     NC2014.2
083900     MOVE 9.2 TO CORRECT-N.                                       NC2014.2
084000     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
084100     PERFORM PRINT-DETAIL.                                        NC2014.2
084200     MOVE PERFORM16 (4,  5, 5) TO COMPUTED-N.                     NC2014.2
084300     MOVE 9.9 TO CORRECT-N.                                       NC2014.2
084400     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
084500     PERFORM PRINT-DETAIL.                                        NC2014.2
084600     MOVE PERFORM11            TO COMPUTED-N.                     NC2014.2
084700     MOVE 3   TO CORRECT-N.                                       NC2014.2
084800     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
084900     PERFORM PRINT-DETAIL.                                        NC2014.2
085000     MOVE PERFORM2             TO COMPUTED-N.                     NC2014.2
085100     MOVE 10  TO CORRECT-N.                                       NC2014.2
085200     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
085300     PERFORM PRINT-DETAIL.                                        NC2014.2
085400     MOVE PERFORM3             TO COMPUTED-N.                     NC2014.2
085500     MOVE 6   TO CORRECT-N.                                       NC2014.2
085600     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
085700     PERFORM PRINT-DETAIL.                                        NC2014.2
085800     MOVE XRAY TO COMPUTED-A.                                     NC2014.2
085900     MOVE "B" TO CORRECT-A.                                       NC2014.2
086000     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2014.2
086100 PFM-WRITE-F4-4.                                                  NC2014.2
086200     PERFORM PRINT-DETAIL.                                        NC2014.2
086300     GO TO PFM-CONTINUE.                                          NC2014.2
086400 PFM-A.                                                           NC2014.2
086500     MULTIPLY PERFORM3 BY 6 GIVING PERFORM2.                      NC2014.2
086600 PFM-AA.                                                          NC2014.2
086700     ADD 1 TO PERFORM3.                                           NC2014.2
086800 PFM-B.                                                           NC2014.2
086900     PERFORM FAIL.                                                NC2014.2
087000     MOVE "PFM-B ERRONIOUSLY ENTERED" TO RE-MARK.                 NC2014.2
087100*    NOTE CONTROL SHOULD NEVER FALL THRU TO THIS PARAGRAPH FROM   NC2014.2
087200*        THE PREVIOUS ONE.                                        NC2014.2
087300     GO TO PFM-WRITE-F3-1.                                        NC2014.2
087400 PFM-C.                                                           NC2014.2
087500     ADD 1 TO PERFORM2.                                           NC2014.2
087600 PFM-D.                                                           NC2014.2
087700     PERFORM FAIL.                                                NC2014.2
087800     MOVE "PFM-D ERRONIOUSLY ENTERED" TO RE-MARK.                 NC2014.2
087900*    NOTE CONTROL SHOULD NEVER FALL THRU TO THIS PARAGRAPH FROM   NC2014.2
088000*        THE PREVIOUS ONE.                                        NC2014.2
088100     GO TO PFM-WRITE-F3-2.                                        NC2014.2
088200 PFM-E.                                                           NC2014.2
088300     ADD PERFORM4 3.5 GIVING PERFORM6.                            NC2014.2
088400 PFM-F.                                                           NC2014.2
088500     PERFORM FAIL.                                                NC2014.2
088600     MOVE "PFM-F ERRONIOUSLY ENTERED" TO RE-MARK.                 NC2014.2
088700*    NOTE CONTROL SHOULD NEVER FALL THRU TO THIS PARAGRAPH FROM   NC2014.2
088800*        THE PREVIOUS ONE.                                        NC2014.2
088900     GO TO PFM-WRITE-F4-1.                                        NC2014.2
089000 PFM-G.                                                           NC2014.2
089100     ADD PERFORM4 8 GIVING PERFORM8 (PERFORM3).                   NC2014.2
089200     ADD .3 TO PERFORM4.                                          NC2014.2
089300 PFM-H.                                                           NC2014.2
089400     PERFORM FAIL.                                                NC2014.2
089500     MOVE "PFM-H ERRONIOUSLY ENTERED" TO RE-MARK.                 NC2014.2
089600*    NOTE CONTROL SHOULD NEVER FALL THRU TO THIS PARAGRAPH FROM   NC2014.2
089700*        THE PREVIOUS ONE.                                        NC2014.2
089800     GO TO PFM-WRITE-F4-2.                                        NC2014.2
089900 PFM-I.                                                           NC2014.2
090000     MULTIPLY PERFORM4 BY 10 GIVING PERFORM14                     NC2014.2
090100         (PERFORM3, PERFORM2).                                    NC2014.2
090200 PFM-J.                                                           NC2014.2
090300     ADD .5 TO PERFORM4.                                          NC2014.2
090400 PFM-K.                                                           NC2014.2
090500     PERFORM FAIL.                                                NC2014.2
090600     MOVE "PFM-K ERRONIOUSLY ENTERED" TO RE-MARK.                 NC2014.2
090700*    NOTE CONTROL SHOULD NEVER FALL THRU TO THIS PARAGRAPH FROM   NC2014.2
090800*        THE PREVIOUS ONE.                                        NC2014.2
090900     GO TO PFM-WRITE-F4-3.                                        NC2014.2
091000 PFM-L.                                                           NC2014.2
091100     SUBTRACT 5.0 FROM PERFORM5 GIVING PERFORM16                  NC2014.2
091200         (PERFORM3, PERFORM2, PERFORM11).                         NC2014.2
091300     ADD .7 TO PERFORM5.                                          NC2014.2
091400 PFM-M.                                                           NC2014.2
091500     PERFORM FAIL.                                                NC2014.2
091600     MOVE "PFM-M ERRONIOUSLY ENTERED" TO RE-MARK.                 NC2014.2
091700*    NOTE CONTROL SHOULD NEVER FALL THRU TO THIS PARAGRAPH FROM   NC2014.2
091800*        THE PREVIOUS ONE.                                        NC2014.2
091900     GO TO PFM-WRITE-F4-4.                                        NC2014.2
092000 PFM-CONTINUE.                                                    NC2014.2
092100     EXIT.                                                        NC2014.2
092200*                                                                 NC2014.2
092300 PFM-INIT-F3-3.                                                   NC2014.2
092400     MOVE "PFM-TEST-F3-3  " TO PAR-NAME.                          NC2014.2
092500     MOVE "VI-122 6.20.4 GR10(C)" TO ANSI-REFERENCE.              NC2014.2
092600     MOVE ZERO TO WRK-DS-02V00.                                   NC2014.2
092700     GO TO PFM-TEST-F3-3.                                         NC2014.2
092800 PFM-A-3-3.                                                       NC2014.2
092900     EXIT.                                                        NC2014.2
093000 PFM-B-3-3.                                                       NC2014.2
093100     ADD      1 TO WRK-DS-02V00.                                  NC2014.2
093200 PFM-TEST-F3-3.                                                   NC2014.2
093300     PERFORM PFM-A-3-3 THROUGH PFM-B-3-3 UNTIL TEST-2NUC-COND-99. NC2014.2
093400     IF WRK-DS-02V00 EQUAL TO 99                                  NC2014.2
093500              PERFORM PASS GO TO PFM-WRITE-F3-3.                  NC2014.2
093600     GO TO PFM-FAIL-F3-3.                                         NC2014.2
093700 PFM-DELETE-F3-3.                                                 NC2014.2
093800     PERFORM  DE-LETE.                                            NC2014.2
093900     GO TO PFM-WRITE-F3-3.                                        NC2014.2
094000 PFM-FAIL-F3-3.                                                   NC2014.2
094100     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC2014.2
094200     MOVE     99 TO CORRECT-N.                                    NC2014.2
094300     PERFORM  FAIL.                                               NC2014.2
094400 PFM-WRITE-F3-3.                                                  NC2014.2
094500     PERFORM  PRINT-DETAIL.                                       NC2014.2
094600*                                                                 NC2014.2
094700 PFM-INIT-F4-5.                                                   NC2014.2
094800     MOVE     "PFM-TEST-F4-5" TO PAR-NAME.                        NC2014.2
094900     MOVE     ZERO TO WRK-DS-02V00.                               NC2014.2
095000     MOVE     ZERO TO WRK-DS-06V06.                               NC2014.2
095100     PERFORM  PFM-A-4-5 THROUGH PFM-C-4-5 VARYING WRK-DS-02V00    NC2014.2
095200              FROM 1 BY 1 UNTIL TEST-2NUC-COND-99.                NC2014.2
095300     GO TO PFM-TEST-F4-5.                                         NC2014.2
095400 PFM-A-4-5.                                                       NC2014.2
095500     ADD      0.000001 TO WRK-DS-06V06.                           NC2014.2
095600 PFM-B-4-5.                                                       NC2014.2
095700     ADD      1 TO WRK-DS-06V06.                                  NC2014.2
095800 PFM-C-4-5.                                                       NC2014.2
095900     SUBTRACT 1 FROM WRK-DS-06V06.                                NC2014.2
096000 PFM-TEST-F4-5.                                                   NC2014.2
096100     ADD      WRK-DS-02V00 TO WRK-DS-06V06.                       NC2014.2
096200     IF   WRK-DS-06V06 EQUAL TO 99.000098                         NC2014.2
096300              PERFORM PASS GO TO PFM-WRITE-F4-5.                  NC2014.2
096400     GO       TO PFM-FAIL-F4-5.                                   NC2014.2
096500 PFM-DELETE-F4-5.                                                 NC2014.2
096600     PERFORM  DE-LETE.                                            NC2014.2
096700     GO TO    PFM-WRITE-F4-5.                                     NC2014.2
096800 PFM-FAIL-F4-5.                                                   NC2014.2
096900     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC2014.2
097000     MOVE 99.000098 TO CORRECT-N.                                 NC2014.2
097100     PERFORM  FAIL.                                               NC2014.2
097200 PFM-WRITE-F4-5.                                                  NC2014.2
097300     PERFORM  PRINT-DETAIL.                                       NC2014.2
097400*                                                                 NC2014.2
097500 PFM-INIT-F4-6.                                                   NC2014.2
097600     MOVE     "PFM-TEST-F4-6" TO PAR-NAME.                        NC2014.2
097700     MOVE     ZERO TO WRK-DS-02V00.                               NC2014.2
097800     MOVE     ZERO TO WRK-DS-06V06.                               NC2014.2
097900     PERFORM  PFM-A-4-6 VARYING WRK-DS-02V00 FROM A02TWOS-DS-02V00NC2014.2
098000              BY A02TWOS-DS-02V00 UNTIL (WRK-DS-02V00 + 12) = 100.NC2014.2
098100 PFM-A-4-6.                                                       NC2014.2
098200     ADD      0.000001 TO WRK-DS-06V06.                           NC2014.2
098300 PFM-TEST-F4-6.                                                   NC2014.2
098400     ADD      WRK-DS-02V00 TO WRK-DS-06V06.                       NC2014.2
098500     IF       WRK-DS-06V06 EQUAL TO 88.000004                     NC2014.2
098600              PERFORM PASS GO TO PFM-WRITE-F4-6.                  NC2014.2
098700     GO       TO PFM-FAIL-F4-6.                                   NC2014.2
098800 PFM-DELETE-F4-6.                                                 NC2014.2
098900     PERFORM  DE-LETE.                                            NC2014.2
099000     GO       TO PFM-WRITE-F4-6.                                  NC2014.2
099100 PFM-FAIL-F4-6.                                                   NC2014.2
099200     MOVE     WRK-DS-06V06 TO COMPUTED-N.                         NC2014.2
099300     MOVE     88.000004 TO CORRECT-N.                             NC2014.2
099400     PERFORM  FAIL.                                               NC2014.2
099500 PFM-WRITE-F4-6.                                                  NC2014.2
099600     PERFORM  PRINT-DETAIL.                                       NC2014.2
099700*                                                                 NC2014.2
099800 PFM-INIT-F4-7.                                                   NC2014.2
099900     MOVE     "PFM-TEST-F4-7" TO PAR-NAME.                        NC2014.2
100000     GO TO PFM-TEST-F4-7.                                         NC2014.2
100100 PFM-A-10.                                                        NC2014.2
100200     EXIT.                                                        NC2014.2
100300 PFM-TEST-F4-7.                                                   NC2014.2
100400     PERFORM  PFM-A-10                                            NC2014.2
100500              VARYING PERFORM4                                    NC2014.2
100600              FROM -5.5                                           NC2014.2
100700              BY 0.1                                              NC2014.2
100800              UNTIL PERFORM4 > 90.                                NC2014.2
100900     IF       PERFORM4 EQUAL TO 90.1                              NC2014.2
101000              PERFORM PASS GO TO PFM-WRITE-F4-7.                  NC2014.2
101100     GO       TO PFM-FAIL-F4-7.                                   NC2014.2
101200*        NOTE PFM-A-10 SHOULD BE "EXECUTED" UNTIL PERFORM4 IS 90.1NC2014.2
101300*             EVEN THOUGH PFM-A-10 IS NOTHING BUT AN EXIT.        NC2014.2
101400 PFM-DELETE-F4-7.                                                 NC2014.2
101500     PERFORM  DE-LETE.                                            NC2014.2
101600     GO       TO PFM-WRITE-F4-7.                                  NC2014.2
101700 PFM-FAIL-F4-7.                                                   NC2014.2
101800     PERFORM  FAIL.                                               NC2014.2
101900     MOVE PERFORM4 TO COMPUTED-N.                                 NC2014.2
102000     MOVE 90.1 TO CORRECT-N.                                      NC2014.2
102100 PFM-WRITE-F4-7.                                                  NC2014.2
102200     PERFORM  PRINT-DETAIL.                                       NC2014.2
102300*                                                                 NC2014.2
102400 PFM-INIT-F4-8.                                                   NC2014.2
102500     MOVE "PFM-TEST-F4-8" TO PAR-NAME.                            NC2014.2
102600     MOVE ZERO TO PFM-11-COUNTER.                                 NC2014.2
102700     MOVE ZERO TO SWITCH-PFM-1.                                   NC2014.2
102800*    NOTE  THIS AUDIT ROUTINE TESTS NESTED PERFORMS               NC2014.2
102900*        IF THE PROGRAM CANNOT SET RETURNS AT THE PROPER PLACE    NC2014.2
103000*             OR  EXECUTE THEM IN PROPER SEQUENCE A FAIL WILL     NC2014.2
103100*                 RESULT.                                         NC2014.2
103200 PFM-TEST-F4-8.                                                   NC2014.2
103300     GO TO PFM-PART-A.                                            NC2014.2
103400 PFM-DELETE-F4-8.                                                 NC2014.2
103500     PERFORM DE-LETE.                                             NC2014.2
103600     GO TO PFM-WRITE-F4-8.                                        NC2014.2
103700 PFM-PART-A SECTION.                                              NC2014.2
103800 PARA-PART-A.                                                     NC2014.2
103900     IF SWITCH-PFM-1 = 1                                          NC2014.2
104000         GO TO PFM-SEC-A3.                                        NC2014.2
104100 PFM-SEC-A2.                                                      NC2014.2
104200     PERFORM PFM-SEC-B1 THRU PFM-SEC-B6.                          NC2014.2
104300     GO TO PFM-SEC-A4.                                            NC2014.2
104400 PFM-SEC-A3.                                                      NC2014.2
104500     ADD 2 TO PFM-11-COUNTER.                                     NC2014.2
104600     MOVE 1 TO SWITCH-PFM-2.                                      NC2014.2
104700          PERFORM PFM-SEC-B1 THRU PFM-SEC-B5.                     NC2014.2
104800 PFM-SEC-A4.                                                      NC2014.2
104900     EXIT.                                                        NC2014.2
105000 PFM-PART-B SECTION.                                              NC2014.2
105100 PFM-SEC-B1.                                                      NC2014.2
105200     MULTIPLY  PFM-11-COUNTER BY 10 GIVING PFM-11-COUNTER.        NC2014.2
105300     IF SWITCH-PFM-2 EQUAL TO 1                                   NC2014.2
105400              GO TO PFM-SEC-B5.                                   NC2014.2
105500 PFM-SEC-B2.                                                      NC2014.2
105600     MOVE 1 TO SWITCH-PFM-1.                                      NC2014.2
105700 PFM-SEC-B3.                                                      NC2014.2
105800     PERFORM PFM-PART-A.                                          NC2014.2
105900 PFM-SEC-B4.                                                      NC2014.2
106000     EXIT.                                                        NC2014.2
106100 PFM-SEC-B5.                                                      NC2014.2
106200     EXIT.                                                        NC2014.2
106300 PFM-SEC-B6.                                                      NC2014.2
106400     EXIT.                                                        NC2014.2
106500 PFM-SEC-B7.                                                      NC2014.2
106600     EXIT.                                                        NC2014.2
106700 PFM-SEC-STOP.                                                    NC2014.2
106800     IF PFM-11-COUNTER EQUAL TO 200                               NC2014.2
106900              PERFORM PASS                                        NC2014.2
107000              GO TO PFM-WRITE-F4-8.                               NC2014.2
107100     PERFORM FAIL.                                                NC2014.2
107200     MOVE "200" TO CORRECT-A.                                     NC2014.2
107300     MOVE PFM-11-COUNTER TO COMPUTED-A.                           NC2014.2
107400 PFM-WRITE-F4-8.                                                  NC2014.2
107500     PERFORM PRINT-DETAIL.                                        NC2014.2
107600*                                                                 NC2014.2
107700 PFM-INIT-F4-9.                                                   NC2014.2
107800     MOVE "PFM-TEST-F4-9" TO PAR-NAME.                            NC2014.2
107900     ADD 44 TO PFM-12-ANS1.                                       NC2014.2
108000     ADD 46 TO PFM-12-ANS2.                                       NC2014.2
108100*    NOTE  THIS PROGRAM TESTS THE ABILITY OF THE COMPILER TO      NC2014.2
108200*        PERFORM A STATEMENT WITH A VARYING CLAUSE INCLUDED.      NC2014.2
108300     GO TO PFM-TEST-F4-9.                                         NC2014.2
108400 PFM-F4-9-A.                                                      NC2014.2
108500     ADD 1 TO PFM-12-ANS2.                                        NC2014.2
108600     SUBTRACT 2 FROM PFM-12-ANS1.                                 NC2014.2
108700     IF PFM-12-ANS2 LESS THAN PFM-12-ANS1                         NC2014.2
108800         GO TO PFM-F4-9-B ELSE                                    NC2014.2
108900     DIVIDE PFM-12-COUNTER BY 2 GIVING PFM-12-COUNTER.            NC2014.2
109000     IF PFM-12-COUNTER LESS THAN 36 SUBTRACT 4 FROM               NC2014.2
109100     PFM-12-COUNTER.                                              NC2014.2
109200 PFM-F4-9-B.                                                      NC2014.2
109300     EXIT.                                                        NC2014.2
109400 PFM-TEST-F4-9.                                                   NC2014.2
109500     PERFORM PFM-F4-9-A VARYING PFM-12-COUNTER FROM 100 BY 4      NC2014.2
109600       UNTIL PFM-12-COUNTER NOT GREATER THAN 15                   NC2014.2
109700             AND PFM-12-ANS1 LESS THAN PFM-12-ANS2                NC2014.2
109800             OR PFM-12-ANS2 GREATER THAN 50.                      NC2014.2
109900     IF PFM-12-COUNTER EQUAL TO 13                                NC2014.2
110000         PERFORM PASS                                             NC2014.2
110100         GO TO PFM-WRITE-F4-9.                                    NC2014.2
110200     GO TO PFM-FAIL-F4-9.                                         NC2014.2
110300 PFM-DELETE-F4-9.                                                 NC2014.2
110400     PERFORM DE-LETE.                                             NC2014.2
110500     GO TO PFM-WRITE-F4-9.                                        NC2014.2
110600 PFM-FAIL-F4-9.                                                   NC2014.2
110700     PERFORM FAIL.                                                NC2014.2
110800     MOVE PFM-12-COUNTER TO COMPUTED-A.                           NC2014.2
110900     MOVE "13" TO CORRECT-A.                                      NC2014.2
111000 PFM-WRITE-F4-9.                                                  NC2014.2
111100     PERFORM PRINT-DETAIL.                                        NC2014.2
111200*                                                                 NC2014.2
111300 PFM-INIT-F4-10.                                                  NC2014.2
111400     MOVE     "PFM-TEST-F4-10" TO PAR-NAME.                       NC2014.2
111500     MOVE     0 TO PERFORM18.                                     NC2014.2
111600     MOVE     1 TO START-POINT.                                   NC2014.2
111700     MOVE     3 TO INC-VALUE.                                     NC2014.2
111800     GO TO PFM-TEST-F4-10.                                        NC2014.2
111900 PFM-F4-10-A.                                                     NC2014.2
112000     ADD      1 TO PERFORM18.                                     NC2014.2
112100     ADD      3 TO PERFORM17.                                     NC2014.2
112200*        NOTE MANIPULATING PERFORM17 IS SUPPOSED TO AFFECT THE    NC2014.2
112300*             NUMBER OF TIMES THIS PARAGRAPH IS PERFORMED --- IN  NC2014.2
112400*             PARTICULAR PFM-F4-10-A WOULD HAVE BEEN EXECUTED 15  NC2014.2
112500*             TIMES WITHOUT THE ABOVE ADDITION TO PERFORM17, BUT  NC2014.2
112600*             IN FACT IT SHOULD NOW BE EXECUTED ONLY 8 TIMES.     NC2014.2
112700 PFM-TEST-F4-10.                                                  NC2014.2
112800     PERFORM  PFM-F4-10-A                                         NC2014.2
112900              VARYING PERFORM17                                   NC2014.2
113000              FROM START-POINT                                    NC2014.2
113100              BY INC-VALUE                                        NC2014.2
113200              UNTIL PERFORM17 GREATER THAN 45                     NC2014.2
113300     IF       PERFORM18 EQUAL TO 8 PERFORM PASS                   NC2014.2
113400                                   GO TO PFM-WRITE-F4-10.         NC2014.2
113500     GO TO PFM-FAIL-F4-10.                                        NC2014.2
113600 PFM-DELETE-F4-10.                                                NC2014.2
113700     PERFORM  DE-LETE.                                            NC2014.2
113800     GO       TO PFM-WRITE-F4-10.                                 NC2014.2
113900 PFM-FAIL-F4-10.                                                  NC2014.2
114000     PERFORM  FAIL.                                               NC2014.2
114100     MOVE     PERFORM18 TO COMPUTED-N.                            NC2014.2
114200     MOVE     8 TO CORRECT-N.                                     NC2014.2
114300 PFM-WRITE-F4-10.                                                 NC2014.2
114400     PERFORM  PRINT-DETAIL.                                       NC2014.2
114500*                                                                 NC2014.2
114600 PFM-INIT-F4-11.                                                  NC2014.2
114700     MOVE     "PFM-TEST-F4-11" TO PAR-NAME.                       NC2014.2
114800     MOVE     0 TO PERFORM18.                                     NC2014.2
114900     MOVE     1 TO START-POINT.                                   NC2014.2
115000     MOVE     3 TO INC-VALUE.                                     NC2014.2
115100     GO TO PFM-TEST-F4-11.                                        NC2014.2
115200 PFM-F4-11-A.                                                     NC2014.2
115300     ADD      1 TO PERFORM18.                                     NC2014.2
115400     MOVE     46 TO START-POINT.                                  NC2014.2
115500*        NOTE THE ABOVE MOVE HAS NO EFFECT ON THE NUMBER OF TIMES NC2014.2
115600*             PFM-F4-11-A IS EXECUTED (15).                       NC2014.2
115700 PFM-TEST-F4-11.                                                  NC2014.2
115800     PERFORM  PFM-F4-11-A                                         NC2014.2
115900              VARYING PERFORM17                                   NC2014.2
116000              FROM START-POINT                                    NC2014.2
116100              BY INC-VALUE                                        NC2014.2
116200              UNTIL PERFORM17 GREATER THAN 45                     NC2014.2
116300     IF       PERFORM18 EQUAL TO 15 PERFORM PASS                  NC2014.2
116400                                    GO TO PFM-WRITE-F4-11.        NC2014.2
116500     GO TO PFM-FAIL-F4-11.                                        NC2014.2
116600 PFM-DELETE-F4-11.                                                NC2014.2
116700     PERFORM  DE-LETE.                                            NC2014.2
116800     GO       TO PFM-WRITE-F4-11.                                 NC2014.2
116900 PFM-FAIL-F4-11.                                                  NC2014.2
117000     PERFORM  FAIL.                                               NC2014.2
117100     MOVE     PERFORM18 TO COMPUTED-N.                            NC2014.2
117200     MOVE     15 TO CORRECT-N.                                    NC2014.2
117300 PFM-WRITE-F4-11.                                                 NC2014.2
117400     PERFORM  PRINT-DETAIL.                                       NC2014.2
117500*                                                                 NC2014.2
117600 PFM-INIT-F4-12.                                                  NC2014.2
117700     MOVE     "PFM-TEST-F4-12" TO PAR-NAME.                       NC2014.2
117800     MOVE     0 TO PERFORM18.                                     NC2014.2
117900     MOVE     1 TO START-POINT.                                   NC2014.2
118000     MOVE     3 TO INC-VALUE.                                     NC2014.2
118100     GO TO PFM-TEST-F4-12.                                        NC2014.2
118200 PFM-F4-12-A.                                                     NC2014.2
118300     ADD      1 TO PERFORM18.                                     NC2014.2
118400     ADD      1 TO INC-VALUE.                                     NC2014.2
118500*        NOTE THE ABOVE ADD TO INC-VALUE SHOULD ACCELERATE THE    NC2014.2
118600*             SPEED AT WHICH PERFORM17 APPROACHES 46 --- THEREFORENC2014.2
118700*             PFM-F4-12-A IS EXECUTED ONLY 7 TIMES INSTEAD OF 15. NC2014.2
118800 PFM-TEST-F4-12.                                                  NC2014.2
118900     PERFORM  PFM-F4-12-A                                         NC2014.2
119000              VARYING PERFORM17                                   NC2014.2
119100              FROM START-POINT                                    NC2014.2
119200              BY INC-VALUE                                        NC2014.2
119300              UNTIL PERFORM17 GREATER THAN 45                     NC2014.2
119400     IF       PERFORM18 EQUAL TO 7 PERFORM PASS                   NC2014.2
119500                                   GO TO PFM-WRITE-F4-12.         NC2014.2
119600     GO TO PFM-FAIL-F4-12.                                        NC2014.2
119700 PFM-DELETE-F4-12.                                                NC2014.2
119800     PERFORM  DE-LETE.                                            NC2014.2
119900     GO       TO PFM-WRITE-F4-12.                                 NC2014.2
120000 PFM-FAIL-F4-12.                                                  NC2014.2
120100     PERFORM  FAIL.                                               NC2014.2
120200     MOVE     PERFORM18 TO COMPUTED-N.                            NC2014.2
120300     MOVE     7 TO CORRECT-N.                                     NC2014.2
120400 PFM-WRITE-F4-12.                                                 NC2014.2
120500     PERFORM  PRINT-DETAIL.                                       NC2014.2
120600 PFM-INIT-F3-4.                                                   NC2014.2
120700*    ===--> "TEST BEFORE" PHRASE <--===                           NC2014.2
120800     MOVE   "VI-112 6.20.2 GR10" TO ANSI-REFERENCE.               NC2014.2
120900     MOVE 1 TO PERFORM2.                                          NC2014.2
121000*    NOTE IN THIS TEST THE CONDITION IS NOT SATISFIED             NC2014.2
121100*    ORIGINALLY WHEN THE PERFORM IS ENTERED.                      NC2014.2
121200 PFM-TEST-F3-4-0.                                                 NC2014.2
121300     PERFORM PFM-A THRU PFM-AA TEST BEFORE                        NC2014.2
121400             UNTIL PERFORM2 EQUAL TO 48.                          NC2014.2
121500 PFM-TEST-F3-4-1.                                                 NC2014.2
121600     IF PERFORM2 EQUAL TO 48 PERFORM PASS GO TO PFM-WRITE-F3-4.   NC2014.2
121700     GO TO PFM-FAIL-F3-4.                                         NC2014.2
121800 PFM-DELETE-F3-4.                                                 NC2014.2
121900     PERFORM DE-LETE.                                             NC2014.2
122000     GO TO PFM-WRITE-F3-4.                                        NC2014.2
122100 PFM-FAIL-F3-4.                                                   NC2014.2
122200     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
122300     MOVE 48 TO CORRECT-N.                                        NC2014.2
122400     PERFORM FAIL.                                                NC2014.2
122500 PFM-WRITE-F3-4.                                                  NC2014.2
122600     MOVE "PFM-TEST-F3-4" TO PAR-NAME.                            NC2014.2
122700     PERFORM PRINT-DETAIL.                                        NC2014.2
122800 PFM-INIT-F3-5.                                                   NC2014.2
122900*    ===--> "TEST BEFORE" PHRASE <--===                           NC2014.2
123000     MOVE   "VI-112 6.20.2 GR10" TO ANSI-REFERENCE.               NC2014.2
123100     MOVE 50 TO PERFORM2.                                         NC2014.2
123200*    NOTE IN THIS TEST CONDITION IS SATISFIED WHEN PERFORM IS     NC2014.2
123300*    ENTERED AND CONTROL SHOULD NOT BE PASSED TO PFM-C.           NC2014.2
123400 PFM-TEST-F3-5-0.                                                 NC2014.2
123500     PERFORM PFM-F3-5-C TEST BEFORE                               NC2014.2
123600             UNTIL PERFORM2 GREATER THAN 25.                      NC2014.2
123700 PFM-TEST-F3-5-1.                                                 NC2014.2
123800     IF PERFORM2 EQUAL TO 50 PERFORM PASS GO TO PFM-WRITE-F3-5.   NC2014.2
123900     GO TO PFM-FAIL-F3-5.                                         NC2014.2
124000 PFM-DELETE-F3-5.                                                 NC2014.2
124100     PERFORM DE-LETE.                                             NC2014.2
124200     GO TO PFM-WRITE-F3-5.                                        NC2014.2
124300 PFM-F3-5-C.                                                      NC2014.2
124400     ADD 1 TO PERFORM2.                                           NC2014.2
124500 PFM-FAIL-F3-5.                                                   NC2014.2
124600     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
124700     MOVE 50 TO CORRECT-N.                                        NC2014.2
124800     PERFORM FAIL.                                                NC2014.2
124900 PFM-WRITE-F3-5.                                                  NC2014.2
125000     MOVE "PFM-TEST-F3-5" TO PAR-NAME.                            NC2014.2
125100     PERFORM PRINT-DETAIL.                                        NC2014.2
125200 PFM-INIT-F3-6.                                                   NC2014.2
125300*    ===--> "TEST BEFORE" PHRASE <--===                           NC2014.2
125400     MOVE   "VI-112 6.20.2 GR10" TO ANSI-REFERENCE.               NC2014.2
125500     MOVE    ZERO TO WRK-DS-02V00.                                NC2014.2
125600 PFM-TEST-F3-6-0.                                                 NC2014.2
125700     PERFORM PFM-A-F3-6 THROUGH PFM-B-F3-6                        NC2014.2
125800             WITH TEST BEFORE                                     NC2014.2
125900             UNTIL TEST-2NUC-COND-99.                             NC2014.2
126000 PFM-TEST-F3-6.                                                   NC2014.2
126100     GO TO   PFM-TESTT-F3-6.                                      NC2014.2
126200 PFM-A-F3-6.                                                      NC2014.2
126300     EXIT.                                                        NC2014.2
126400 PFM-B-F3-6.                                                      NC2014.2
126500     ADD     1 TO WRK-DS-02V00.                                   NC2014.2
126600 PFM-TESTT-F3-6.                                                  NC2014.2
126700     IF      WRK-DS-02V00 EQUAL TO 99                             NC2014.2
126800             PERFORM PASS GO TO PFM-WRITE-F3-6.                   NC2014.2
126900     GO TO   PFM-FAIL-F3-6.                                       NC2014.2
127000 PFM-DELETE-F3-6.                                                 NC2014.2
127100     PERFORM DE-LETE.                                             NC2014.2
127200     GO      TO PFM-WRITE-F3-6.                                   NC2014.2
127300 PFM-FAIL-F3-6.                                                   NC2014.2
127400     MOVE    WRK-DS-02V00 TO COMPUTED-N.                          NC2014.2
127500     MOVE    99 TO CORRECT-N.                                     NC2014.2
127600     PERFORM  FAIL.                                               NC2014.2
127700 PFM-WRITE-F3-6.                                                  NC2014.2
127800     MOVE     "PFM-TEST-F3-6  " TO PAR-NAME.                      NC2014.2
127900     PERFORM  PRINT-DETAIL.                                       NC2014.2
128000 PFM-INIT-F3-7.                                                   NC2014.2
128100*    ===--> "TEST AFTER"  PHRASE <--===                           NC2014.2
128200     MOVE   "VI-112 6.20.4 GR10(C)" TO ANSI-REFERENCE.            NC2014.2
128300     MOVE 1 TO PERFORM2.                                          NC2014.2
128400     MOVE 5 TO PERFORM3.                                          NC2014.2
128500*    NOTE IN THIS TEST THE CONDITION IS NOT SATISFIED             NC2014.2
128600*    ORIGINALLY WHEN THE PERFORM IS ENTERED.                      NC2014.2
128700 PFM-TEST-F3-7-0.                                                 NC2014.2
128800     PERFORM PFM-A THRU PFM-AA TEST AFTER                         NC2014.2
128900             UNTIL PERFORM2 EQUAL TO 48.                          NC2014.2
129000 PFM-TEST-F3-7-1.                                                 NC2014.2
129100     IF PERFORM2 EQUAL TO 48 PERFORM PASS GO TO PFM-WRITE-F3-7.   NC2014.2
129200     GO TO PFM-FAIL-F3-7.                                         NC2014.2
129300 PFM-DELETE-F3-7.                                                 NC2014.2
129400     PERFORM DE-LETE.                                             NC2014.2
129500     GO TO PFM-WRITE-F3-7.                                        NC2014.2
129600 PFM-FAIL-F3-7.                                                   NC2014.2
129700     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
129800     MOVE 48 TO CORRECT-N.                                        NC2014.2
129900     PERFORM FAIL.                                                NC2014.2
130000 PFM-WRITE-F3-7.                                                  NC2014.2
130100     MOVE "PFM-TEST-F3-7" TO PAR-NAME.                            NC2014.2
130200     PERFORM PRINT-DETAIL.                                        NC2014.2
130300 PFM-INIT-F3-8.                                                   NC2014.2
130400*    ===--> "TEST AFTER"  PHRASE <--===                           NC2014.2
130500     MOVE   "VI-112 6.20.2 GR10" TO ANSI-REFERENCE.               NC2014.2
130600     MOVE 50 TO PERFORM2.                                         NC2014.2
130700*    NOTE IN THIS TEST CONDITION IS SATISFIED AFTER PERFORM IS    NC2014.2
130800*    ENTERED AND CONTROL SHOULD BE PASSED TO PFM-C ONCE.          NC2014.2
130900 PFM-TEST-F3-8-0.                                                 NC2014.2
131000     PERFORM PFM-F3-8-C TEST AFTER                                NC2014.2
131100             UNTIL PERFORM2 GREATER THAN 25.                      NC2014.2
131200 PFM-TEST-F3-8-1.                                                 NC2014.2
131300     IF PERFORM2 EQUAL TO 51 PERFORM PASS GO TO PFM-WRITE-F3-8.   NC2014.2
131400     GO TO PFM-FAIL-F3-8.                                         NC2014.2
131500 PFM-DELETE-F3-8.                                                 NC2014.2
131600     PERFORM DE-LETE.                                             NC2014.2
131700     GO TO PFM-WRITE-F3-8.                                        NC2014.2
131800 PFM-F3-8-C.                                                      NC2014.2
131900     ADD 1 TO PERFORM2.                                           NC2014.2
132000 PFM-FAIL-F3-8.                                                   NC2014.2
132100     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
132200     MOVE 51 TO CORRECT-N.                                        NC2014.2
132300     PERFORM FAIL.                                                NC2014.2
132400 PFM-WRITE-F3-8.                                                  NC2014.2
132500     MOVE "PFM-TEST-F3-8" TO PAR-NAME.                            NC2014.2
132600     PERFORM PRINT-DETAIL.                                        NC2014.2
132700 PFM-INIT-F3-9.                                                   NC2014.2
132800*    ===--> "TEST AFTER " PHRASE <--===                           NC2014.2
132900     MOVE   "VI-112 6.20.2 GR10" TO ANSI-REFERENCE.               NC2014.2
133000     MOVE    ZERO TO WRK-DS-02V00.                                NC2014.2
133100 PFM-TEST-F3-9-0.                                                 NC2014.2
133200     PERFORM PFM-A-F3-9 THROUGH PFM-B-F3-9                        NC2014.2
133300             WITH TEST AFTER                                      NC2014.2
133400             UNTIL TEST-2NUC-COND-99.                             NC2014.2
133500 PFM-TEST-F3-9.                                                   NC2014.2
133600     GO TO   PFM-TESTT-F3-9.                                      NC2014.2
133700 PFM-A-F3-9.                                                      NC2014.2
133800     EXIT.                                                        NC2014.2
133900 PFM-B-F3-9.                                                      NC2014.2
134000     ADD     1 TO WRK-DS-02V00.                                   NC2014.2
134100 PFM-TESTT-F3-9.                                                  NC2014.2
134200     IF      WRK-DS-02V00 EQUAL TO 99                             NC2014.2
134300             PERFORM PASS GO TO PFM-WRITE-F3-9.                   NC2014.2
134400     GO TO   PFM-FAIL-F3-9.                                       NC2014.2
134500 PFM-DELETE-F3-9.                                                 NC2014.2
134600     PERFORM DE-LETE.                                             NC2014.2
134700     GO      TO PFM-WRITE-F3-9.                                   NC2014.2
134800 PFM-FAIL-F3-9.                                                   NC2014.2
134900     MOVE    WRK-DS-02V00 TO COMPUTED-N.                          NC2014.2
135000     MOVE    99 TO CORRECT-N.                                     NC2014.2
135100     PERFORM  FAIL.                                               NC2014.2
135200 PFM-WRITE-F3-9.                                                  NC2014.2
135300     MOVE     "PFM-TEST-F3-9" TO PAR-NAME.                        NC2014.2
135400     PERFORM  PRINT-DETAIL.                                       NC2014.2
135500*                                                                 NC2014.2
135600 PFM-INIT-F4-13.                                                  NC2014.2
135700*    ===--> "WITH TEST BEFORE" PHRASE <--===                      NC2014.2
135800     MOVE   "VI-114 6.20.4 GR10(d)1" TO ANSI-REFERENCE.           NC2014.2
135900     MOVE    20  TO PERFORM2.                                     NC2014.2
136000     MOVE     9  TO PERFORM3.                                     NC2014.2
136100     MOVE    1.5 TO PERFORM4.                                     NC2014.2
136200     MOVE     3  TO PERFORM9.                                     NC2014.2
136300     MOVE    -1  TO PERFORM10.                                    NC2014.2
136400     MOVE     6  TO PERFORM11.                                    NC2014.2
136500     MOVE  ZEROS TO PERFORM12.                                    NC2014.2
136600     MOVE    SPACE TO XRAY.                                       NC2014.2
136700*    NOTE IN THIS TEST TWO SUBSCRIPTS ARE VARIED.                 NC2014.2
136800 PFM-TEST-F4-13-0.                                                NC2014.2
136900     PERFORM PFM-I-F4-13 THRU PFM-J-F4-13 WITH TEST BEFORE        NC2014.2
137000             VARYING PERFORM3 FROM PERFORM9 BY PERFORM10          NC2014.2
137100               UNTIL PERFORM3 EQUAL TO 1                          NC2014.2
137200               AFTER PERFORM2 FROM 2 BY PERFORM11                 NC2014.2
137300               UNTIL PERFORM2 GREATER THAN 20.                    NC2014.2
137400     GO TO     PFM-TEST-F4-13-1.                                  NC2014.2
137500 PFM-I-F4-13.                                                     NC2014.2
137600     MULTIPLY PERFORM4 BY 10 GIVING PERFORM14                     NC2014.2
137700         (PERFORM3, PERFORM2).                                    NC2014.2
137800 PFM-J-F4-13.                                                     NC2014.2
137900     ADD .5 TO PERFORM4.                                          NC2014.2
138000 PFM-TEST-F4-13-1.                                                NC2014.2
138100     IF PERFORM14 (3, 2) EQUAL TO 15.0 AND PERFORM14 (3, 8)       NC2014.2
138200     EQUAL TO 20.0 AND PERFORM14 (3, 14) EQUAL TO 25.0            NC2014.2
138300     AND PERFORM14 (3, 20) EQUAL TO 30.0 AND PERFORM14 (2, 2)     NC2014.2
138400     EQUAL TO 35.0 MOVE "A" TO XRAY.                              NC2014.2
138500     IF PERFORM14 (2, 8) EQUAL TO 40.0 AND                        NC2014.2
138600     PERFORM14 (2, 14) EQUAL TO 45.0 AND PERFORM14 (2, 20)        NC2014.2
138700     EQUAL TO 50.0 AND PERFORM2 EQUAL TO 2 AND PERFORM3           NC2014.2
138800     EQUAL TO 1 AND XRAY EQUAL TO "A" PERFORM PASS                NC2014.2
138900         GO TO PFM-WRITE-F4-13.                                   NC2014.2
139000     GO TO PFM-FAIL-F4-13.                                        NC2014.2
139100 PFM-DELETE-F4-13.                                                NC2014.2
139200     PERFORM DE-LETE.                                             NC2014.2
139300     GO TO PFM-WRITE-F4-13.                                       NC2014.2
139400 PFM-FAIL-F4-13.                                                  NC2014.2
139500     MOVE PERFORM14 (3,  2) TO COMPUTED-N.                        NC2014.2
139600     MOVE 15.0 TO CORRECT-N.                                      NC2014.2
139700     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
139800     PERFORM FAIL.                                                NC2014.2
139900     PERFORM PRINT-DETAIL.                                        NC2014.2
140000     MOVE SPACE TO P-OR-F.                                        NC2014.2
140100     MOVE PERFORM14 (3,  8) TO COMPUTED-N.                        NC2014.2
140200     MOVE 20.0 TO CORRECT-N.                                      NC2014.2
140300     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
140400     PERFORM PRINT-DETAIL.                                        NC2014.2
140500     MOVE PERFORM14 (3, 14) TO COMPUTED-N.                        NC2014.2
140600     MOVE 25.0 TO CORRECT-N.                                      NC2014.2
140700     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
140800     PERFORM PRINT-DETAIL.                                        NC2014.2
140900     MOVE PERFORM14 (3, 20) TO COMPUTED-N.                        NC2014.2
141000     MOVE 30.0 TO CORRECT-N.                                      NC2014.2
141100     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
141200     PERFORM PRINT-DETAIL.                                        NC2014.2
141300     MOVE PERFORM14 (2,  2) TO COMPUTED-N.                        NC2014.2
141400     MOVE 35.0 TO CORRECT-N.                                      NC2014.2
141500     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
141600     PERFORM PRINT-DETAIL.                                        NC2014.2
141700     MOVE PERFORM14 (2,  8) TO COMPUTED-N.                        NC2014.2
141800     MOVE 40.0 TO CORRECT-N.                                      NC2014.2
141900     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
142000     PERFORM PRINT-DETAIL.                                        NC2014.2
142100     MOVE PERFORM14 (2, 14) TO COMPUTED-N.                        NC2014.2
142200     MOVE 45.0 TO CORRECT-N.                                      NC2014.2
142300     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
142400     PERFORM PRINT-DETAIL.                                        NC2014.2
142500     MOVE PERFORM14 (2, 20) TO COMPUTED-N.                        NC2014.2
142600     MOVE 50.0 TO CORRECT-N.                                      NC2014.2
142700     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
142800     PERFORM PRINT-DETAIL.                                        NC2014.2
142900     MOVE PERFORM2 TO COMPUTED-N.                                 NC2014.2
143000     MOVE    2 TO CORRECT-N.                                      NC2014.2
143100     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
143200     PERFORM PRINT-DETAIL.                                        NC2014.2
143300     MOVE PERFORM3 TO COMPUTED-N.                                 NC2014.2
143400     MOVE    1 TO CORRECT-N.                                      NC2014.2
143500     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
143600     PERFORM PRINT-DETAIL.                                        NC2014.2
143700     MOVE XRAY TO COMPUTED-A.                                     NC2014.2
143800     MOVE "A" TO CORRECT-A.                                       NC2014.2
143900 PFM-WRITE-F4-13.                                                 NC2014.2
144000     MOVE "PFM-TEST-F4-13" TO PAR-NAME.                           NC2014.2
144100     PERFORM PRINT-DETAIL.                                        NC2014.2
144200*                                                                 NC2014.2
144300 PFM-INIT-F4-14.                                                  NC2014.2
144400*    ===--> "WITH TEST AFTER" PHRASE <--===                       NC2014.2
144500     MOVE   "VI-117/8 6.20.4 GR10(d)2" TO ANSI-REFERENCE.         NC2014.2
144600     MOVE   "PFM-TEST-F4-14" TO PAR-NAME.                         NC2014.2
144700     MOVE    ZEROS TO PERFORM12.                                  NC2014.2
144800     MOVE    SPACE TO XRAY.                                       NC2014.2
144900     MOVE    6  TO PERFORM11.                                     NC2014.2
145000     MOVE   -1  TO PERFORM10.                                     NC2014.2
145100     MOVE    2  TO PERFORM9.                                      NC2014.2
145200     MOVE    1  TO PERFORM4.                                      NC2014.2
145300     MOVE    2  TO PERFORM3.                                      NC2014.2
145400     MOVE    20 TO PERFORM2.                                      NC2014.2
145500     MOVE    1  TO REC-CT.                                        NC2014.2
145600*    NOTE:   IN THIS TEST TWO SUBSCRIPTS ARE VARIED.              NC2014.2
145700 PFM-TEST-F4-14-0.                                                NC2014.2
145800     PERFORM PFM-I-F4-14 THRU PFM-J-F4-14 WITH TEST AFTER         NC2014.2
145900             VARYING PERFORM3 FROM PERFORM9 BY PERFORM10          NC2014.2
146000               UNTIL PERFORM3 EQUAL TO 1                          NC2014.2
146100               AFTER PERFORM2 FROM 2 BY PERFORM11                 NC2014.2
146200               UNTIL PERFORM2 GREATER THAN 19.                    NC2014.2
146300     GO TO   PFM-TEST-F4-14-1.                                    NC2014.2
146400 PFM-I-F4-14.                                                     NC2014.2
146500     MULTIPLY PERFORM4 BY 10 GIVING PERFORM14                     NC2014.2
146600         (PERFORM3, PERFORM2).                                    NC2014.2
146700 PFM-J-F4-14.                                                     NC2014.2
146800     ADD .5 TO PERFORM4.                                          NC2014.2
146900     MOVE    1 TO PERFORM3.                                       NC2014.2
147000     MOVE    99 TO PERFORM2.                                      NC2014.2
147100 PFM-DELETE-F4-14.                                                NC2014.2
147200     PERFORM DE-LETE.                                             NC2014.2
147300     PERFORM PRINT-DETAIL.                                        NC2014.2
147400     GO TO   PFM-INIT-F4-15.                                      NC2014.2
147500 PFM-TEST-F4-14-1.                                                NC2014.2
147600     MOVE   "PFM-TEST-F4-14-1" TO PAR-NAME.                       NC2014.2
147700     IF      PERFORM14 (2, 2) NOT EQUAL TO 10.0                   NC2014.2
147800             MOVE PERFORM14 (2, 2) TO COMPUTED-N                  NC2014.2
147900             MOVE 10.0 TO CORRECT-N                               NC2014.2
148000             PERFORM FAIL                                         NC2014.2
148100             PERFORM PRINT-DETAIL                                 NC2014.2
148200     ELSE                                                         NC2014.2
148300             PERFORM PASS                                         NC2014.2
148400             PERFORM PRINT-DETAIL.                                NC2014.2
148500     MOVE    SPACE TO P-OR-F.                                     NC2014.2
148600     ADD     1 TO REC-CT.                                         NC2014.2
148700 PFM-TEST-F4-14-2.                                                NC2014.2
148800     MOVE   "PFM-TEST-F4-14-2" TO PAR-NAME.                       NC2014.2
148900     IF      PERFORM4  NOT = 1.5                                  NC2014.2
149000             MOVE PERFORM4  TO COMPUTED-N                         NC2014.2
149100             MOVE 1.5  TO CORRECT-N                               NC2014.2
149200             PERFORM FAIL                                         NC2014.2
149300             PERFORM PRINT-DETAIL                                 NC2014.2
149400     ELSE                                                         NC2014.2
149500             PERFORM PASS                                         NC2014.2
149600             PERFORM PRINT-DETAIL.                                NC2014.2
149700*                                                                 NC2014.2
149800 PFM-INIT-F4-15.                                                  NC2014.2
149900*    ===--> "     TEST AFTER " PHRASE <--===                      NC2014.2
150000     MOVE   "VI-117/8 6.20.4 GR10(d)2" TO ANSI-REFERENCE.         NC2014.2
150100     MOVE   "PFM-TEST-F4-15"           TO PAR-NAME.               NC2014.2
150200     MOVE    ZEROS TO PERFORM12.                                  NC2014.2
150300     MOVE     1 TO REC-CT.                                        NC2014.2
150400     MOVE    20 TO PERFORM2.                                      NC2014.2
150500     MOVE     5 TO PERFORM3.                                      NC2014.2
150600     MOVE     1 TO PERFORM4.                                      NC2014.2
150700     MOVE     3 TO PERFORM9.                                      NC2014.2
150800     MOVE    -1 TO PERFORM10.                                     NC2014.2
150900     MOVE     6 TO PERFORM11.                                     NC2014.2
151000*    NOTE IN THIS TEST TWO SUBSCRIPTS ARE VARIED.                 NC2014.2
151100 PFM-TEST-F4-15-0.                                                NC2014.2
151200     PERFORM PFM-I-F4-15 THRU PFM-J-F4-15 TEST AFTER              NC2014.2
151300             VARYING PERFORM3 FROM PERFORM9 BY PERFORM10          NC2014.2
151400               UNTIL PERFORM3 EQUAL TO 2                          NC2014.2
151500               AFTER PERFORM2 FROM 2 BY PERFORM11                 NC2014.2
151600               UNTIL PERFORM2 GREATER THAN 19.                    NC2014.2
151700     GO TO   PFM-TEST-F4-15-1.                                    NC2014.2
151800 PFM-I-F4-15.                                                     NC2014.2
151900     MULTIPLY PERFORM4 BY 10 GIVING PERFORM14                     NC2014.2
152000         (PERFORM3, PERFORM2).                                    NC2014.2
152100 PFM-J-F4-15.                                                     NC2014.2
152200     ADD .5 TO PERFORM4.                                          NC2014.2
152300     MOVE 20 TO PERFORM2.                                         NC2014.2
152400 PFM-DELETE-F4-15.                                                NC2014.2
152500     PERFORM DE-LETE.                                             NC2014.2
152600     PERFORM PRINT-DETAIL.                                        NC2014.2
152700     GO TO   PFM-INIT-F4-16.                                      NC2014.2
152800 PFM-TEST-F4-15-1.                                                NC2014.2
152900     IF      PERFORM14 (2, 2) NOT = 15.0                          NC2014.2
153000             MOVE    PERFORM14 (2, 2) TO COMPUTED-N               NC2014.2
153100             MOVE    15.0 TO CORRECT-N                            NC2014.2
153200             PERFORM FAIL                                         NC2014.2
153300             PERFORM PRINT-DETAIL                                 NC2014.2
153400     ELSE                                                         NC2014.2
153500             PERFORM PASS                                         NC2014.2
153600             PERFORM PRINT-DETAIL.                                NC2014.2
153700     ADD     1 TO REC-CT.                                         NC2014.2
153800 PFM-TEST-F4-15-2.                                                NC2014.2
153900     IF      PERFORM14 (3, 2) NOT = 10.0                          NC2014.2
154000             MOVE    PERFORM14 (3, 2) TO COMPUTED-N               NC2014.2
154100             MOVE    10.0 TO CORRECT-N                            NC2014.2
154200             PERFORM FAIL                                         NC2014.2
154300             PERFORM PRINT-DETAIL                                 NC2014.2
154400     ELSE                                                         NC2014.2
154500             PERFORM PASS                                         NC2014.2
154600             PERFORM PRINT-DETAIL.                                NC2014.2
154700*                                                                 NC2014.2
154800 PFM-INIT-F4-16.                                                  NC2014.2
154900*    ===-->  6 AFTER PHRASES          <--===                      NC2014.2
155000     MOVE   "VI-110 6.20.3 SR12" TO ANSI-REFERENCE.               NC2014.2
155100     MOVE   "PFM-TEST-F4-16"           TO PAR-NAME.               NC2014.2
155200     MOVE    SPACES TO PERFORM-SEVEN-LEVEL-TABLE.                 NC2014.2
155300     MOVE    0 TO PFM-7-TOT.                                      NC2014.2
155400     MOVE     1 TO REC-CT.                                        NC2014.2
155500     MOVE     1 TO S1 S2 S3 S4 S5 S6 S7.                          NC2014.2
155600*    NOTE IN THIS TEST SEVEN SUBSCRIPTS ARE VARIED.               NC2014.2
155700 PFM-TEST-F4-16-0.                                                NC2014.2
155800     PERFORM PFM-I-F4-16 THRU PFM-J-F4-16                         NC2014.2
155900             VARYING S1 FROM 1 BY 1                               NC2014.2
156000               UNTIL S1 = 3                                       NC2014.2
156100               AFTER S2 FROM 1 BY 1                               NC2014.2
156200               UNTIL S2 = 3                                       NC2014.2
156300               AFTER S3 FROM 1 BY 1                               NC2014.2
156400               UNTIL S3 = 3                                       NC2014.2
156500               AFTER S4 FROM 1 BY 1                               NC2014.2
156600               UNTIL S4 = 3                                       NC2014.2
156700               AFTER S5 FROM 1 BY 1                               NC2014.2
156800               UNTIL S5 = 3                                       NC2014.2
156900               AFTER S6 FROM 1 BY 1                               NC2014.2
157000               UNTIL S6 = 3                                       NC2014.2
157100               AFTER S7 FROM 1 BY 1                               NC2014.2
157200               UNTIL S7 = 3.                                      NC2014.2
157300     GO TO   PFM-TEST-F4-16-1.                                    NC2014.2
157400 PFM-I-F4-16.                                                     NC2014.2
157500     MOVE   "*" TO PFM77-1 (S1 S2 S3 S4 S5 S6 S7).                NC2014.2
157600 PFM-J-F4-16.                                                     NC2014.2
157700     ADD     1  TO PFM-7-TOT.                                     NC2014.2
157800 PFM-DELETE-F4-16.                                                NC2014.2
157900     PERFORM DE-LETE.                                             NC2014.2
158000     PERFORM PRINT-DETAIL.                                        NC2014.2
158100     GO TO   PFM-INIT-F4-17.                                      NC2014.2
158200 PFM-TEST-F4-16-1.                                                NC2014.2
158300     IF      PFM77-1 (1 1 1 1 1 1 1) NOT = "*"                    NC2014.2
158400             MOVE    PFM77-1 (1 1 1 1 1 1 1) TO COMPUTED-A        NC2014.2
158500             MOVE   "*" TO CORRECT-A                              NC2014.2
158600             PERFORM FAIL                                         NC2014.2
158700             PERFORM PRINT-DETAIL                                 NC2014.2
158800     ELSE                                                         NC2014.2
158900             PERFORM PASS                                         NC2014.2
159000             PERFORM PRINT-DETAIL.                                NC2014.2
159100     ADD     1 TO REC-CT.                                         NC2014.2
159200 PFM-TEST-F4-16-2.                                                NC2014.2
159300     IF      PFM77-1 (1 1 1 1 1 1 2) NOT = "*"                    NC2014.2
159400             MOVE    PFM77-1 (1 1 1 1 1 1 2) TO COMPUTED-A        NC2014.2
159500             MOVE   "*" TO CORRECT-A                              NC2014.2
159600             PERFORM FAIL                                         NC2014.2
159700             PERFORM PRINT-DETAIL                                 NC2014.2
159800     ELSE                                                         NC2014.2
159900             PERFORM PASS                                         NC2014.2
160000             PERFORM PRINT-DETAIL.                                NC2014.2
160100     ADD     1 TO REC-CT.                                         NC2014.2
160200 PFM-TEST-F4-16-3.                                                NC2014.2
160300     IF      PFM77-1 (1 1 1 1 1 2 1) NOT = "*"                    NC2014.2
160400             MOVE    PFM77-1 (1 1 1 1 1 2 1) TO COMPUTED-A        NC2014.2
160500             MOVE   "*" TO CORRECT-A                              NC2014.2
160600             PERFORM FAIL                                         NC2014.2
160700             PERFORM PRINT-DETAIL                                 NC2014.2
160800     ELSE                                                         NC2014.2
160900             PERFORM PASS                                         NC2014.2
161000             PERFORM PRINT-DETAIL.                                NC2014.2
161100     ADD     1 TO REC-CT.                                         NC2014.2
161200 PFM-TEST-F4-16-4.                                                NC2014.2
161300     IF      PFM77-1 (1 1 1 1 1 2 2) NOT = "*"                    NC2014.2
161400             MOVE    PFM77-1 (1 1 1 1 1 2 2) TO COMPUTED-A        NC2014.2
161500             MOVE   "*" TO CORRECT-A                              NC2014.2
161600             PERFORM FAIL                                         NC2014.2
161700             PERFORM PRINT-DETAIL                                 NC2014.2
161800     ELSE                                                         NC2014.2
161900             PERFORM PASS                                         NC2014.2
162000             PERFORM PRINT-DETAIL.                                NC2014.2
162100     ADD     1 TO REC-CT.                                         NC2014.2
162200 PFM-TEST-F4-16-5.                                                NC2014.2
162300     IF      PFM77-1 (1 1 1 1 2 1 1) NOT = "*"                    NC2014.2
162400             MOVE    PFM77-1 (1 1 1 1 2 1 1) TO COMPUTED-A        NC2014.2
162500             MOVE   "*" TO CORRECT-A                              NC2014.2
162600             PERFORM FAIL                                         NC2014.2
162700             PERFORM PRINT-DETAIL                                 NC2014.2
162800     ELSE                                                         NC2014.2
162900             PERFORM PASS                                         NC2014.2
163000             PERFORM PRINT-DETAIL.                                NC2014.2
163100     ADD     1 TO REC-CT.                                         NC2014.2
163200 PFM-TEST-F4-16-6.                                                NC2014.2
163300     IF      PFM77-1 (1 1 1 1 2 1 2) NOT = "*"                    NC2014.2
163400             MOVE    PFM77-1 (1 1 1 1 2 1 2) TO COMPUTED-A        NC2014.2
163500             MOVE   "*" TO CORRECT-A                              NC2014.2
163600             PERFORM FAIL                                         NC2014.2
163700             PERFORM PRINT-DETAIL                                 NC2014.2
163800     ELSE                                                         NC2014.2
163900             PERFORM PASS                                         NC2014.2
164000             PERFORM PRINT-DETAIL.                                NC2014.2
164100     ADD     1 TO REC-CT.                                         NC2014.2
164200 PFM-TEST-F4-16-7.                                                NC2014.2
164300     IF      PFM77-1 (1 1 1 2 1 1 1) NOT = "*"                    NC2014.2
164400             MOVE    PFM77-1 (1 1 1 2 1 1 1) TO COMPUTED-A        NC2014.2
164500             MOVE   "*" TO CORRECT-A                              NC2014.2
164600             PERFORM FAIL                                         NC2014.2
164700             PERFORM PRINT-DETAIL                                 NC2014.2
164800     ELSE                                                         NC2014.2
164900             PERFORM PASS                                         NC2014.2
165000             PERFORM PRINT-DETAIL.                                NC2014.2
165100     ADD     1 TO REC-CT.                                         NC2014.2
165200 PFM-TEST-F4-16-9.                                                NC2014.2
165300     IF      PFM77-1 (1 1 1 2 1 1 2) NOT = "*"                    NC2014.2
165400             MOVE    PFM77-1 (1 1 1 2 1 1 2) TO COMPUTED-A        NC2014.2
165500             MOVE   "*" TO CORRECT-A                              NC2014.2
165600             PERFORM FAIL                                         NC2014.2
165700             PERFORM PRINT-DETAIL                                 NC2014.2
165800     ELSE                                                         NC2014.2
165900             PERFORM PASS                                         NC2014.2
166000             PERFORM PRINT-DETAIL.                                NC2014.2
166100     ADD     1 TO REC-CT.                                         NC2014.2
166200 PFM-TEST-F4-16-10.                                               NC2014.2
166300     IF      PFM-7-TOT NOT = 128                                  NC2014.2
166400             MOVE    PFM-7-TOT TO COMPUTED-18V0                   NC2014.2
166500             MOVE    128 TO CORRECT-18V0                          NC2014.2
166600             PERFORM FAIL                                         NC2014.2
166700             PERFORM PRINT-DETAIL                                 NC2014.2
166800     ELSE                                                         NC2014.2
166900             PERFORM PASS                                         NC2014.2
167000             PERFORM PRINT-DETAIL.                                NC2014.2
167100*                                                                 NC2014.2
167200 PFM-INIT-F4-17.                                                  NC2014.2
167300*    ===-->  6 AFTER PHRASES          <--===                      NC2014.2
167400*    ===--> "WITH TEST BEFORE" PHRASE <--===                      NC2014.2
167500     MOVE   "PFM-TEST-F4-17"           TO PAR-NAME.               NC2014.2
167600     MOVE   "VI-114 6.20.4 GR10(d)2" TO ANSI-REFERENCE.           NC2014.2
167700     MOVE    SPACES TO PERFORM-SEVEN-LEVEL-TABLE.                 NC2014.2
167800     MOVE    101 TO COUNT-DU-6V0.                                 NC2014.2
167900     MOVE    0   TO PFM-7-TOT.                                    NC2014.2
168000     MOVE    1 TO REC-CT.                                         NC2014.2
168100*    NOTE IN THIS TEST SEVEN SUBSCRIPTS ARE VARIED.               NC2014.2
168200 PFM-TEST-F4-17-0.                                                NC2014.2
168300     PERFORM PFM-I-F4-17 THRU PFM-J-F4-17 WITH TEST BEFORE        NC2014.2
168400             VARYING S1 FROM 1 BY 1                               NC2014.2
168500               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
168600               AFTER S2 FROM 1 BY 1                               NC2014.2
168700               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
168800               AFTER S3 FROM 1 BY 1                               NC2014.2
168900               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
169000               AFTER S4 FROM 1 BY 1                               NC2014.2
169100               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
169200               AFTER S5 FROM 1 BY 1                               NC2014.2
169300               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
169400               AFTER S6 FROM 1 BY 1                               NC2014.2
169500               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
169600               AFTER S7 FROM 1 BY 1                               NC2014.2
169700               UNTIL COUNT-DU-6V0 > 100.                          NC2014.2
169800     GO TO   PFM-TEST-F4-17-1.                                    NC2014.2
169900 PFM-I-F4-17.                                                     NC2014.2
170000     MOVE   "*" TO PFM77-1 (S1 S2 S3 S4 S5 S6 S7).                NC2014.2
170100 PFM-J-F4-17.                                                     NC2014.2
170200     ADD     1  TO PFM-7-TOT.                                     NC2014.2
170300 PFM-DELETE-F4-17.                                                NC2014.2
170400     PERFORM DE-LETE.                                             NC2014.2
170500     PERFORM PRINT-DETAIL.                                        NC2014.2
170600     GO TO   PFM-INIT-F4-18.                                      NC2014.2
170700 PFM-TEST-F4-17-1.                                                NC2014.2
170800     IF      PFM77-1 (1 1 1 1 1 1 1) NOT = SPACE                  NC2014.2
170900             MOVE    PFM77-1 (1 1 1 1 1 1 1) TO COMPUTED-A        NC2014.2
171000             MOVE    SPACE TO CORRECT-A                           NC2014.2
171100             PERFORM FAIL                                         NC2014.2
171200             PERFORM PRINT-DETAIL                                 NC2014.2
171300     ELSE                                                         NC2014.2
171400             PERFORM PASS                                         NC2014.2
171500             PERFORM PRINT-DETAIL.                                NC2014.2
171600     ADD     1 TO REC-CT.                                         NC2014.2
171700 PFM-TEST-F4-17-2.                                                NC2014.2
171800     IF      PFM77-1 (1 1 1 1 1 1 2) NOT = SPACE                  NC2014.2
171900             MOVE    PFM77-1 (1 1 1 1 1 1 2) TO COMPUTED-A        NC2014.2
172000             MOVE    SPACE TO CORRECT-A                           NC2014.2
172100             PERFORM FAIL                                         NC2014.2
172200             PERFORM PRINT-DETAIL                                 NC2014.2
172300     ELSE                                                         NC2014.2
172400             PERFORM PASS                                         NC2014.2
172500             PERFORM PRINT-DETAIL.                                NC2014.2
172600     ADD     1 TO REC-CT.                                         NC2014.2
172700 PFM-TEST-F4-17-3.                                                NC2014.2
172800     IF      PFM77-1 (1 1 1 1 1 2 1) NOT = SPACE                  NC2014.2
172900             MOVE    PFM77-1 (1 1 1 1 1 2 1) TO COMPUTED-A        NC2014.2
173000             MOVE    SPACE TO CORRECT-A                           NC2014.2
173100             PERFORM FAIL                                         NC2014.2
173200             PERFORM PRINT-DETAIL                                 NC2014.2
173300     ELSE                                                         NC2014.2
173400             PERFORM PASS                                         NC2014.2
173500             PERFORM PRINT-DETAIL.                                NC2014.2
173600     ADD     1 TO REC-CT.                                         NC2014.2
173700 PFM-TEST-F4-17-4.                                                NC2014.2
173800     IF      PFM77-1 (1 1 1 1 1 2 2) NOT = SPACE                  NC2014.2
173900             MOVE    PFM77-1 (1 1 1 1 1 2 2) TO COMPUTED-A        NC2014.2
174000             MOVE    SPACE TO CORRECT-A                           NC2014.2
174100             PERFORM FAIL                                         NC2014.2
174200             PERFORM PRINT-DETAIL                                 NC2014.2
174300     ELSE                                                         NC2014.2
174400             PERFORM PASS                                         NC2014.2
174500             PERFORM PRINT-DETAIL.                                NC2014.2
174600     ADD     1 TO REC-CT.                                         NC2014.2
174700 PFM-TEST-F4-17-5.                                                NC2014.2
174800     IF      PFM77-1 (1 1 1 1 2 1 1) NOT = SPACE                  NC2014.2
174900             MOVE    PFM77-1 (1 1 1 1 2 1 1) TO COMPUTED-A        NC2014.2
175000             MOVE    SPACE TO CORRECT-A                           NC2014.2
175100             PERFORM FAIL                                         NC2014.2
175200             PERFORM PRINT-DETAIL                                 NC2014.2
175300     ELSE                                                         NC2014.2
175400             PERFORM PASS                                         NC2014.2
175500             PERFORM PRINT-DETAIL.                                NC2014.2
175600     ADD     1 TO REC-CT.                                         NC2014.2
175700 PFM-TEST-F4-17-6.                                                NC2014.2
175800     IF      PFM77-1 (1 1 1 1 2 1 2) NOT = SPACE                  NC2014.2
175900             MOVE    PFM77-1 (1 1 1 1 2 1 2) TO COMPUTED-A        NC2014.2
176000             MOVE    SPACE TO CORRECT-A                           NC2014.2
176100             PERFORM FAIL                                         NC2014.2
176200             PERFORM PRINT-DETAIL                                 NC2014.2
176300     ELSE                                                         NC2014.2
176400             PERFORM PASS                                         NC2014.2
176500             PERFORM PRINT-DETAIL.                                NC2014.2
176600     ADD     1 TO REC-CT.                                         NC2014.2
176700 PFM-TEST-F4-17-7.                                                NC2014.2
176800     IF      PFM77-1 (1 1 1 2 1 1 1) NOT = SPACE                  NC2014.2
176900             MOVE    PFM77-1 (1 1 1 2 1 1 1) TO COMPUTED-A        NC2014.2
177000             MOVE    SPACE TO CORRECT-A                           NC2014.2
177100             PERFORM FAIL                                         NC2014.2
177200             PERFORM PRINT-DETAIL                                 NC2014.2
177300     ELSE                                                         NC2014.2
177400             PERFORM PASS                                         NC2014.2
177500             PERFORM PRINT-DETAIL.                                NC2014.2
177600     ADD     1 TO REC-CT.                                         NC2014.2
177700 PFM-TEST-F4-17-8.                                                NC2014.2
177800     IF      PFM77-1 (1 1 1 2 1 1 2) NOT = SPACE                  NC2014.2
177900             MOVE    PFM77-1 (1 1 1 2 1 1 2) TO COMPUTED-A        NC2014.2
178000             MOVE    SPACE TO CORRECT-A                           NC2014.2
178100             PERFORM FAIL                                         NC2014.2
178200             PERFORM PRINT-DETAIL                                 NC2014.2
178300     ELSE                                                         NC2014.2
178400             PERFORM PASS                                         NC2014.2
178500             PERFORM PRINT-DETAIL.                                NC2014.2
178600     ADD     1 TO REC-CT.                                         NC2014.2
178700 PFM-TEST-F4-17-9.                                                NC2014.2
178800     IF      PFM-7-TOT NOT = ZERO                                 NC2014.2
178900             MOVE    PFM-7-TOT TO COMPUTED-18V0                   NC2014.2
179000             MOVE    ZERO TO CORRECT-18V0                         NC2014.2
179100             PERFORM FAIL                                         NC2014.2
179200             PERFORM PRINT-DETAIL                                 NC2014.2
179300     ELSE                                                         NC2014.2
179400             PERFORM PASS                                         NC2014.2
179500             PERFORM PRINT-DETAIL.                                NC2014.2
179600*                                                                 NC2014.2
179700 PFM-INIT-F4-18.                                                  NC2014.2
179800*    ===-->  6 AFTER PHRASES          <--===                      NC2014.2
179900*    ===-->  TEST AFTER" PHRASE       <--===                      NC2014.2
180000     MOVE   "PFM-TEST-F4-18"           TO PAR-NAME.               NC2014.2
180100     MOVE   "VI-119 6.20.4 GR10(d)2" TO ANSI-REFERENCE.           NC2014.2
180200     MOVE    SPACES TO PERFORM-SEVEN-LEVEL-TABLE.                 NC2014.2
180300     MOVE    0 TO COUNT-DU-6V0.                                   NC2014.2
180400     MOVE    0 TO PFM-7-TOT.                                      NC2014.2
180500     MOVE    1 TO REC-CT.                                         NC2014.2
180600*    NOTE IN THIS TEST SEVEN SUBSCRIPTS ARE VARIED.               NC2014.2
180700 PFM-TEST-F4-18-0.                                                NC2014.2
180800     PERFORM PFM-I-F4-18 THRU PFM-J-F4-18 TEST AFTER              NC2014.2
180900             VARYING S1 FROM 1 BY 1                               NC2014.2
181000               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
181100               AFTER S2 FROM 1 BY 1                               NC2014.2
181200               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
181300               AFTER S3 FROM 1 BY 1                               NC2014.2
181400               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
181500               AFTER S4 FROM 1 BY 1                               NC2014.2
181600               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
181700               AFTER S5 FROM 1 BY 1                               NC2014.2
181800               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
181900               AFTER S6 FROM 1 BY 1                               NC2014.2
182000               UNTIL COUNT-DU-6V0 > 100                           NC2014.2
182100               AFTER S7 FROM 1 BY 1                               NC2014.2
182200               UNTIL COUNT-DU-6V0 > 100.                          NC2014.2
182300     GO TO   PFM-TEST-F4-18-1.                                    NC2014.2
182400 PFM-I-F4-18.                                                     NC2014.2
182500     MOVE   "*" TO PFM77-1 (S1 S2 S3 S4 S5 S6 S7).                NC2014.2
182600     MOVE    101 TO COUNT-DU-6V0.                                 NC2014.2
182700 PFM-J-F4-18.                                                     NC2014.2
182800     ADD     1  TO PFM-7-TOT.                                     NC2014.2
182900 PFM-DELETE-F4-18.                                                NC2014.2
183000     PERFORM DE-LETE.                                             NC2014.2
183100     PERFORM PRINT-DETAIL.                                        NC2014.2
183200     GO TO   PFM-INIT-F4-20.                                      NC2014.2
183300 PFM-TEST-F4-18-1.                                                NC2014.2
183400     IF      PFM77-1 (1 1 1 1 1 1 1) NOT = "*"                    NC2014.2
183500             MOVE    PFM77-1 (1 1 1 1 1 1 1) TO COMPUTED-A        NC2014.2
183600             MOVE   "*"  TO CORRECT-A                             NC2014.2
183700             PERFORM FAIL                                         NC2014.2
183800             PERFORM PRINT-DETAIL                                 NC2014.2
183900     ELSE                                                         NC2014.2
184000             PERFORM PASS                                         NC2014.2
184100             PERFORM PRINT-DETAIL.                                NC2014.2
184200     ADD     1 TO REC-CT.                                         NC2014.2
184300 PFM-TEST-F4-18-2.                                                NC2014.2
184400     IF      PFM77-1 (1 1 1 1 1 1 2) NOT = SPACE                  NC2014.2
184500             MOVE    PFM77-1 (1 1 1 1 1 1 2) TO COMPUTED-A        NC2014.2
184600             MOVE    SPACE TO CORRECT-A                           NC2014.2
184700             PERFORM FAIL                                         NC2014.2
184800             PERFORM PRINT-DETAIL                                 NC2014.2
184900     ELSE                                                         NC2014.2
185000             PERFORM PASS                                         NC2014.2
185100             PERFORM PRINT-DETAIL.                                NC2014.2
185200     ADD     1 TO REC-CT.                                         NC2014.2
185300 PFM-TEST-F4-18-3.                                                NC2014.2
185400     IF      PFM77-1 (1 1 1 1 1 2 1) NOT = SPACE                  NC2014.2
185500             MOVE    PFM77-1 (1 1 1 1 1 2 1) TO COMPUTED-A        NC2014.2
185600             MOVE    SPACE TO CORRECT-A                           NC2014.2
185700             PERFORM FAIL                                         NC2014.2
185800             PERFORM PRINT-DETAIL                                 NC2014.2
185900     ELSE                                                         NC2014.2
186000             PERFORM PASS                                         NC2014.2
186100             PERFORM PRINT-DETAIL.                                NC2014.2
186200     ADD     1 TO REC-CT.                                         NC2014.2
186300 PFM-TEST-F4-18-4.                                                NC2014.2
186400     IF      PFM77-1 (1 1 1 1 1 2 2) NOT = SPACE                  NC2014.2
186500             MOVE    PFM77-1 (1 1 1 1 1 2 2) TO COMPUTED-A        NC2014.2
186600             MOVE    SPACE TO CORRECT-A                           NC2014.2
186700             PERFORM FAIL                                         NC2014.2
186800             PERFORM PRINT-DETAIL                                 NC2014.2
186900     ELSE                                                         NC2014.2
187000             PERFORM PASS                                         NC2014.2
187100             PERFORM PRINT-DETAIL.                                NC2014.2
187200     ADD     1 TO REC-CT.                                         NC2014.2
187300 PFM-TEST-F4-18-5.                                                NC2014.2
187400     IF      PFM77-1 (1 1 1 1 2 1 1) NOT = SPACE                  NC2014.2
187500             MOVE    PFM77-1 (1 1 1 1 2 1 1) TO COMPUTED-A        NC2014.2
187600             MOVE    SPACE TO CORRECT-A                           NC2014.2
187700             PERFORM FAIL                                         NC2014.2
187800             PERFORM PRINT-DETAIL                                 NC2014.2
187900     ELSE                                                         NC2014.2
188000             PERFORM PASS                                         NC2014.2
188100             PERFORM PRINT-DETAIL.                                NC2014.2
188200     ADD     1 TO REC-CT.                                         NC2014.2
188300 PFM-TEST-F4-18-6.                                                NC2014.2
188400     IF      PFM77-1 (1 1 1 1 2 1 2) NOT = SPACE                  NC2014.2
188500             MOVE    PFM77-1 (1 1 1 1 2 1 2) TO COMPUTED-A        NC2014.2
188600             MOVE    SPACE TO CORRECT-A                           NC2014.2
188700             PERFORM FAIL                                         NC2014.2
188800             PERFORM PRINT-DETAIL                                 NC2014.2
188900     ELSE                                                         NC2014.2
189000             PERFORM PASS                                         NC2014.2
189100             PERFORM PRINT-DETAIL.                                NC2014.2
189200     ADD     1 TO REC-CT.                                         NC2014.2
189300 PFM-TEST-F4-18-7.                                                NC2014.2
189400     IF      PFM77-1 (1 1 1 2 1 1 1) NOT = SPACE                  NC2014.2
189500             MOVE    PFM77-1 (1 1 1 2 1 1 1) TO COMPUTED-A        NC2014.2
189600             MOVE    SPACE TO CORRECT-A                           NC2014.2
189700             MOVE   "PFM-TEST-F4-18-8" TO PAR-NAME                NC2014.2
189800             PERFORM FAIL                                         NC2014.2
189900             PERFORM PRINT-DETAIL                                 NC2014.2
190000     ELSE                                                         NC2014.2
190100             PERFORM PASS                                         NC2014.2
190200             PERFORM PRINT-DETAIL.                                NC2014.2
190300     ADD     1 TO REC-CT.                                         NC2014.2
190400 PFM-TEST-F4-18-8.                                                NC2014.2
190500     IF      PFM77-1 (1 1 1 2 1 1 2) NOT = SPACE                  NC2014.2
190600             MOVE    PFM77-1 (1 1 1 2 1 1 2) TO COMPUTED-A        NC2014.2
190700             MOVE    SPACE TO CORRECT-A                           NC2014.2
190800             PERFORM FAIL                                         NC2014.2
190900             PERFORM PRINT-DETAIL                                 NC2014.2
191000     ELSE                                                         NC2014.2
191100             PERFORM PASS                                         NC2014.2
191200             PERFORM PRINT-DETAIL.                                NC2014.2
191300     ADD     1 TO REC-CT.                                         NC2014.2
191400 PFM-TEST-F4-18-9.                                                NC2014.2
191500     IF      PFM-7-TOT NOT = 1                                    NC2014.2
191600             MOVE    PFM-7-TOT TO COMPUTED-18V0                   NC2014.2
191700             MOVE    1  TO CORRECT-18V0                           NC2014.2
191800             PERFORM FAIL                                         NC2014.2
191900             PERFORM PRINT-DETAIL                                 NC2014.2
192000     ELSE                                                         NC2014.2
192100             PERFORM PASS                                         NC2014.2
192200             PERFORM PRINT-DETAIL.                                NC2014.2
192300*                                                                 NC2014.2
192400*                                                                 NC2014.2
192500 PFM-INIT-F4-20.                                                  NC2014.2
192600     MOVE   "VI-112 6.20.4 GR10(d)" TO ANSI-REFERENCE             NC2014.2
192700     MOVE   "PFM-TEST-F4-20"        TO PAR-NAME.                  NC2014.2
192800     MOVE    SPACES TO PERFORM-SEVEN-LEVEL-TABLE.                 NC2014.2
192900     MOVE   "VARYING BY FRAC." TO FEATURE.                        NC2014.2
193000     MOVE   "PERFORM VARYING"  TO RE-MARK.                        NC2014.2
193100     MOVE    ZERO TO COUNT-DU-6V0.                                NC2014.2
193200     MOVE    ZERO TO REC-CT.                                      NC2014.2
193300 PFM-TEST-F4-20-0.                                                NC2014.2
193400     PERFORM PFM-LOOP-F4-20 THROUGH PFM-LOOP-F4-20-EXIT           NC2014.2
193500     VARYING WRK-DU-2V1-1                                         NC2014.2
193600        FROM WRK-DU-0V1-1 BY .1                                   NC2014.2
193700       UNTIL WRK-DU-2V1-1 + WRK-DU-2V1-3 > 12.1.                  NC2014.2
193800     GO TO   PFM-TEST-F4-20-1.                                    NC2014.2
193900 PFM-DELETE-F4-20.                                                NC2014.2
194000     PERFORM DE-LETE.                                             NC2014.2
194100     PERFORM PRINT-DETAIL.                                        NC2014.2
194200     GO      TO PFM-INIT-F4-21.                                   NC2014.2
194300 PFM-LOOP-F4-20.                                                  NC2014.2
194400     ADD     1 TO COUNT-DU-6V0.                                   NC2014.2
194500 PFM-LOOP-F4-20-EXIT.                                             NC2014.2
194600     EXIT.                                                        NC2014.2
194700 PFM-TEST-F4-20-1.                                                NC2014.2
194800     IF      COUNT-DU-6V0 = 10                                    NC2014.2
194900             PERFORM PASS                                         NC2014.2
195000             PERFORM PRINT-DETAIL                                 NC2014.2
195100     ELSE                                                         NC2014.2
195200             PERFORM FAIL                                         NC2014.2
195300             MOVE    COUNT-DU-6V0 TO COMPUTED-N                   NC2014.2
195400             MOVE    10 TO CORRECT-N                              NC2014.2
195500             PERFORM PRINT-DETAIL.                                NC2014.2
195600*                                                                 NC2014.2
195700 PFM-INIT-F4-21.                                                  NC2014.2
195800     MOVE   "VI-112 6.20.4 GR10(d)" TO ANSI-REFERENCE             NC2014.2
195900     MOVE   "PFM-TEST-F4-21"        TO PAR-NAME.                  NC2014.2
196000     MOVE   "CHANGE BY INCR." TO FEATURE.                         NC2014.2
196100     MOVE   "PERFORM VARYING" TO RE-MARK.                         NC2014.2
196200     MOVE    ZERO TO COUNT-DU-6V0.                                NC2014.2
196300 PFM-TEST-F4-21-0.                                                NC2014.2
196400     PERFORM LOOP-FOR-F4-21 THRU LOOP-F4-21-EXIT                  NC2014.2
196500     VARYING WRK-DU-2V1-1 FROM WRK-DU-0V1-1 BY WRK-DU-2V1-2       NC2014.2
196600       UNTIL WRK-DU-2V1-1 + 11.1 > 12.1.                          NC2014.2
196700     GO TO   PFM-TEST-F4-21-1.                                    NC2014.2
196800 PFM-DELETE-F4-21.                                                NC2014.2
196900     PERFORM DE-LETE.                                             NC2014.2
197000     PERFORM PRINT-DETAIL.                                        NC2014.2
197100     GO TO   PFM-INIT-F4-22.                                      NC2014.2
197200 LOOP-FOR-F4-21.                                                  NC2014.2
197300     ADD    1 TO COUNT-DU-6V0.                                    NC2014.2
197400     ADD   .1 TO WRK-DU-2V1-2.                                    NC2014.2
197500 LOOP-F4-21-EXIT.                                                 NC2014.2
197600     EXIT.                                                        NC2014.2
197700 PFM-TEST-F4-21-1.                                                NC2014.2
197800     IF      COUNT-DU-6V0 = 4                                     NC2014.2
197900             PERFORM PASS                                         NC2014.2
198000             PERFORM PRINT-DETAIL                                 NC2014.2
198100     ELSE                                                         NC2014.2
198200             PERFORM FAIL                                         NC2014.2
198300             MOVE    COUNT-DU-6V0 TO COMPUTED-N                   NC2014.2
198400             MOVE    4 TO CORRECT-N                               NC2014.2
198500             PERFORM PRINT-DETAIL.                                NC2014.2
198600*                                                                 NC2014.2
198700 PFM-INIT-F4-22.                                                  NC2014.2
198800*    ===-->  EXPLICIT SCOPE TERMINATOR <--===                     NC2014.2
198900     MOVE   "VI-110 6.20.4 GR5" TO ANSI-REFERENCE.                NC2014.2
199000     MOVE   "PFM-TEST-F4-22"    TO PAR-NAME.                      NC2014.2
199100     MOVE   "CHANGE BY INCR."   TO FEATURE.                       NC2014.2
199200     MOVE   "PERFORM VARYING"   TO RE-MARK.                       NC2014.2
199300     MOVE    44 TO PFM-12-ANS1.                                   NC2014.2
199400     MOVE    46 TO PFM-12-ANS2.                                   NC2014.2
199500*    NOTE  THIS PROGRAM TESTS THE ABILITY OF THE COMPILER TO      NC2014.2
199600*        PERFORM A STATEMENT WITH A VARYING CLAUSE INCLUDED.      NC2014.2
199700 PFM-TEST-F4-22-0.                                                NC2014.2
199800     PERFORM VARYING PFM-12-COUNTER FROM 100 BY 4                 NC2014.2
199900       UNTIL PFM-12-COUNTER NOT GREATER THAN 15                   NC2014.2
200000         AND PFM-12-ANS1 LESS THAN PFM-12-ANS2                    NC2014.2
200100          OR PFM-12-ANS2 GREATER THAN 50                          NC2014.2
200200             ADD      1   TO PFM-12-ANS2                          NC2014.2
200300             SUBTRACT 2 FROM PFM-12-ANS1                          NC2014.2
200400             IF       PFM-12-ANS2 GREATER THAN OR EQUAL TO        NC2014.2
200500                      PFM-12-ANS1                                 NC2014.2
200600                      DIVIDE PFM-12-COUNTER BY 2                  NC2014.2
200700                      GIVING PFM-12-COUNTER                       NC2014.2
200800                      IF       PFM-12-COUNTER LESS THAN 36        NC2014.2
200900                               SUBTRACT 4 FROM PFM-12-COUNTER     NC2014.2
201000                      END-IF                                      NC2014.2
201100             END-IF                                               NC2014.2
201200     END-PERFORM.                                                 NC2014.2
201300     GO TO   PFM-TEST-F4-22-1.                                    NC2014.2
201400 PFM-DELETE-F4-22.                                                NC2014.2
201500     PERFORM DE-LETE.                                             NC2014.2
201600     GO TO   PFM-WRITE-F4-22.                                     NC2014.2
201700 PFM-TEST-F4-22-1.                                                NC2014.2
201800     IF      PFM-12-COUNTER EQUAL TO 13                           NC2014.2
201900             PERFORM PASS                                         NC2014.2
202000             GO TO   PFM-WRITE-F4-22.                             NC2014.2
202100     PERFORM FAIL.                                                NC2014.2
202200     MOVE    PFM-12-COUNTER TO COMPUTED-A.                        NC2014.2
202300     MOVE   "13" TO CORRECT-A.                                    NC2014.2
202400 PFM-WRITE-F4-22.                                                 NC2014.2
202500     MOVE   "PFM-TEST-F4-22" TO PAR-NAME.                         NC2014.2
202600     PERFORM PRINT-DETAIL.                                        NC2014.2
202700*                                                                 NC2014.2
202800*                                                                 NC2014.2
202900 PFM-INIT-F4-23.                                                  NC2014.2
203000*    ===-->  ORDER OF INITIALISATION   <--===                     NC2014.2
203100*    ===-->  OF VARYING IDENTIFIERS.   <--===                     NC2014.2
203200     MOVE   "VI-114 6.20.4 GR10(d)1" TO ANSI-REFERENCE.           NC2014.2
203300     MOVE   "PFM-TEST-F4-23"         TO PAR-NAME.                 NC2014.2
203400     MOVE    SPACES TO PERFORM-SEVEN-LEVEL-TABLE.                 NC2014.2
203500     MOVE    0 TO PFM-F4-23-TOT.                                  NC2014.2
203600 PFM-TEST-F4-23-0.                                                NC2014.2
203700     PERFORM PFM-F4-23-PROC                                       NC2014.2
203800             VARYING PFM-A1 FROM 1 BY 1                           NC2014.2
203900               UNTIL PFM-A1 > 3                                   NC2014.2
204000               AFTER PFM-B1 FROM PFM-A1 BY 1                      NC2014.2
204100               UNTIL PFM-B1 > 3.                                  NC2014.2
204200     GO TO   PFM-TEST-F4-23-1.                                    NC2014.2
204300 PFM-DELETE-F4-23.                                                NC2014.2
204400     PERFORM DE-LETE.                                             NC2014.2
204500     PERFORM PRINT-DETAIL.                                        NC2014.2
204600     GO TO   PFM-INIT-F4-24.                                      NC2014.2
204700 PFM-F4-23-PROC.                                                  NC2014.2
204800     ADD     1 TO PFM-F4-23-TOT.                                  NC2014.2
204900 PFM-TEST-F4-23-1.                                                NC2014.2
205000     IF      PFM-F4-23-TOT = 6                                    NC2014.2
205100             PERFORM PASS                                         NC2014.2
205200             PERFORM PRINT-DETAIL                                 NC2014.2
205300     ELSE                                                         NC2014.2
205400             MOVE    6      TO CORRECT-18V0                       NC2014.2
205500             MOVE    PFM-F4-23-TOT TO COMPUTED-18V0               NC2014.2
205600             PERFORM FAIL                                         NC2014.2
205700             PERFORM PRINT-DETAIL.                                NC2014.2
205800     MOVE 2 TO PERFORM9.                                          NC2014.2
205900     MOVE 2 TO PERFORM10.                                         NC2014.2
206000*                                                                 NC2014.2
206100 PFM-INIT-F4-24.                                                  NC2014.2
206200*    ===-->  MANIPULATING SUBSCRIPTS   <--===                     NC2014.2
206300     MOVE   "VI-112 6.20.4 GR10(d)" TO ANSI-REFERENCE.            NC2014.2
206400     MOVE   "PFM-TEST-F4-24"        TO PAR-NAME.                  NC2014.2
206500     INITIALIZE FILLER-A.                                         NC2014.2
206600     MOVE    1  TO S1 S2 S3.                                      NC2014.2
206700     MOVE    10 TO PFM-F4-24-B (1) MOVE 20  TO PFM-F4-24-B (2).   NC2014.2
206800     MOVE    30 TO PFM-F4-24-B (3) MOVE 40  TO PFM-F4-24-B (4).   NC2014.2
206900     MOVE    50 TO PFM-F4-24-B (5) MOVE 60  TO PFM-F4-24-B (6).   NC2014.2
207000     MOVE    70 TO PFM-F4-24-B (7) MOVE 80  TO PFM-F4-24-B (8).   NC2014.2
207100     MOVE    90 TO PFM-F4-24-B (9) MOVE 100 TO PFM-F4-24-B (10).  NC2014.2
207200     MOVE    10 TO PFM-F4-24-C (1) MOVE 20  TO PFM-F4-24-C (2).   NC2014.2
207300     MOVE    30 TO PFM-F4-24-C (3) MOVE 40  TO PFM-F4-24-C (4).   NC2014.2
207400     MOVE    50 TO PFM-F4-24-C (5) MOVE 60  TO PFM-F4-24-C (6).   NC2014.2
207500     MOVE    70 TO PFM-F4-24-C (7) MOVE 80  TO PFM-F4-24-C (8).   NC2014.2
207600     MOVE    90 TO PFM-F4-24-C (9) MOVE 100 TO PFM-F4-24-C (10).  NC2014.2
207700     MOVE    0 TO PERFORM18.                                      NC2014.2
207800 PFM-TEST-F4-24-0.                                                NC2014.2
207900     PERFORM PFM-A-F4-24                                          NC2014.2
208000             VARYING PFM-F4-24-A (S1)                             NC2014.2
208100                FROM 10                                           NC2014.2
208200                  BY PFM-F4-24-C (S2)                             NC2014.2
208300               UNTIL PFM-F4-24-A (S1) > 70.                       NC2014.2
208400 PFM-TEST-F4-24-1.                                                NC2014.2
208500     IF      PFM-F4-24-A (S1) EQUAL TO 80                         NC2014.2
208600             PERFORM PASS GO TO PFM-WRITE-F4-24-1.                NC2014.2
208700     PERFORM FAIL.                                                NC2014.2
208800     MOVE    PFM-F4-24-A (S1) TO COMPUTED-N.                      NC2014.2
208900     MOVE    80  TO CORRECT-N.                                    NC2014.2
209000     GO TO   PFM-WRITE-F4-24-1.                                   NC2014.2
209100 PFM-DELETE-F4-24-1.                                              NC2014.2
209200     PERFORM DE-LETE.                                             NC2014.2
209300     GO      TO PFM-WRITE-F4-24-1.                                NC2014.2
209400 PFM-A-F4-24.                                                     NC2014.2
209500     ADD     1 TO PERFORM18.                                      NC2014.2
209600     MULTIPLY 2 BY S2.                                            NC2014.2
209700     ADD     1 TO S1 S3.                                          NC2014.2
209800 PFM-WRITE-F4-24-1.                                               NC2014.2
209900     MOVE   "PFM-TEST-F4-24" TO PAR-NAME.                         NC2014.2
210000     PERFORM PRINT-DETAIL.                                        NC2014.2
210100 PFM-TEST-F4-24-2.                                                NC2014.2
210200     IF      S1 EQUAL TO 4                                        NC2014.2
210300             PERFORM PASS GO TO PFM-WRITE-F4-24-2.                NC2014.2
210400     PERFORM FAIL.                                                NC2014.2
210500     MOVE    S1  TO COMPUTED-N.                                   NC2014.2
210600     MOVE    4   TO CORRECT-N.                                    NC2014.2
210700     GO TO   PFM-WRITE-F4-24-2.                                   NC2014.2
210800 PFM-DELETE-F4-24-2.                                              NC2014.2
210900     PERFORM DE-LETE.                                             NC2014.2
211000     GO      TO PFM-WRITE-F4-24-2.                                NC2014.2
211100 PFM-A-F4-24-2.                                                   NC2014.2
211200     ADD     1 TO PERFORM18.                                      NC2014.2
211300     MULTIPLY 2 BY S2.                                            NC2014.2
211400     ADD     1 TO S1 S3.                                          NC2014.2
211500 PFM-WRITE-F4-24-2.                                               NC2014.2
211600     MOVE   "PFM-TEST-F4-24" TO PAR-NAME.                         NC2014.2
211700     PERFORM PRINT-DETAIL.                                        NC2014.2
211800 CCVS-EXIT SECTION.                                               NC2014.2
211900 CCVS-999999.                                                     NC2014.2
212000     GO TO CLOSE-FILES.                                           NC2014.2
      *END-OF,NC201A                                                                  
