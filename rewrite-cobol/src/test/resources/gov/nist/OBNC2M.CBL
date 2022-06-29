      *HEADER,COBOL,OBNC2M                                                            
000100 IDENTIFICATION DIVISION.                                         OBNC24.2
000200 PROGRAM-ID.                                                      OBNC24.2
000300     OBNC2M.                                                      OBNC24.2
000400                                                                  OBNC24.2
000500****************************************************************  OBNC24.2
000600*                                                              *  OBNC24.2
000700*    VALIDATION FOR:-                                          *  OBNC24.2
000800*                                                              *  OBNC24.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBNC24.2
001000*                                                              *  OBNC24.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBNC24.2
001200*                                                              *  OBNC24.2
001300****************************************************************  OBNC24.2
001400*                                                              *  OBNC24.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  OBNC24.2
001600*                                                              *  OBNC24.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  OBNC24.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  OBNC24.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  OBNC24.2
002000*                                                              *  OBNC24.2
002100****************************************************************  OBNC24.2
002200*    PROGRAM OBNC2M CONTAINS CCVS74 TESTS OF LANGUAGE ELEMENTS *  OBNC24.2
002300*    DEFINED AS OBSOLETE IN THE 198X STANDARDS.                *  OBNC24.2
002400****************************************************************  OBNC24.2
002500 DATE-COMPILED.                                                   OBNC24.2
002600*    THIS COMMENT LINE SHOULD NOT BE REPLACED                     OBNC24.2
002700*    THIS COMMENT ENTRY SHOULD APPEAR AS THE LAST LINE BEFORE     OBNC24.2
002800*    THE ENVIRONMENT DIVISION.                                    OBNC24.2
002900 ENVIRONMENT DIVISION.                                            OBNC24.2
003000 CONFIGURATION SECTION.                                           OBNC24.2
003100 SOURCE-COMPUTER.                                                 OBNC24.2
003200     XXXXX082.                                                    OBNC24.2
003300 OBJECT-COMPUTER.                                                 OBNC24.2
003400     XXXXX083.                                                    OBNC24.2
003500 INPUT-OUTPUT SECTION.                                            OBNC24.2
003600 FILE-CONTROL.                                                    OBNC24.2
003700     SELECT PRINT-FILE ASSIGN TO                                  OBNC24.2
003800     XXXXX055.                                                    OBNC24.2
003900 DATA DIVISION.                                                   OBNC24.2
004000 FILE SECTION.                                                    OBNC24.2
004100 FD  PRINT-FILE.                                                  OBNC24.2
004200 01  PRINT-REC PICTURE X(120).                                    OBNC24.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 OBNC24.2
004400 WORKING-STORAGE SECTION.                                         OBNC24.2
004500 77  SMALL-VALU   PICTURE 99 VALUE 7.                             OBNC24.2
004600 77  SMALLER-VALU PICTURE 99 VALUE 6.                             OBNC24.2
004700 77  SMALLEST-VALU   PICTURE 99 VALUE 5.                          OBNC24.2
004800 77  EVEN-SMALLER PICTURE 99 VALUE 1.                             OBNC24.2
004900 77  WRK-DS-02V00                 PICTURE S99.                    OBNC24.2
005000     88 TEST-2NUC-COND-99         VALUE 99.                       OBNC24.2
005100 77  WRK-DS-06V06     PICTURE S9(6)V9(6).                         OBNC24.2
005200 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        OBNC24.2
005300                                  PICTURE S9(12).                 OBNC24.2
005400 77  A02TWOS-DS-02V00             PICTURE S99    VALUE 22.        OBNC24.2
005500 77  WRK-DS-01V00                 PICTURE S9.                     OBNC24.2
005600 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  OBNC24.2
005700 77  A990-DS-0201P                PICTURE S99P   VALUE 990.       OBNC24.2
005800 77  A02ONES-DS-02V00             PICTURE S99    VALUE 11.        OBNC24.2
005900 77  A01ONE-DS-P0801              PICTURE SP(8)9 VALUE .000000001.OBNC24.2
006000 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         OBNC24.2
006100 77  WRK-XN-00001                 PICTURE X.                      OBNC24.2
006200 77  WRK-XN-00005                 PICTURE X(5).                   OBNC24.2
006300 77  TWO  PICTURE 9 VALUE 2.                                      OBNC24.2
006400 77  THREE PICTURE 9 VALUE 3.                                     OBNC24.2
006500 77  SEVEN PICTURE 9 VALUE 7.                                     OBNC24.2
006600 77  NINE  PICTURE 9 VALUE 9.                                     OBNC24.2
006700 77  TEN  PICTURE 99 VALUE 10.                                    OBNC24.2
006800 77  ALTERCOUNT PICTURE 999 VALUE ZERO.                           OBNC24.2
006900 77  QT5    PIC X(4) VALUE SPACE.                                 OBNC24.2
007000 77  XRAY PICTURE IS X.                                           OBNC24.2
007100 77  IF-D1 PICTURE S9(4)V9(2) VALUE 0.                            OBNC24.2
007200 77  IF-D2 PICTURE S9(4)V9(2) VALUE ZERO.                         OBNC24.2
007300 77  IF-D3 PICTURE X(10) VALUE "0000000000".                      OBNC24.2
007400 77  IF-D4 PICTURE X(15) VALUE "               ".                 OBNC24.2
007500 77  IF-D5 PICTURE X(10) VALUE ALL QUOTE.                         OBNC24.2
007600 77  IF-D6 PICTURE A(10) VALUE "BABABABABA".                      OBNC24.2
007700 77  IF-D7 PICTURE S9(6)V9(4) VALUE +123.45.                      OBNC24.2
007800 77  IF-D8 PICTURE 9(6)V9(4) VALUE 12300.                         OBNC24.2
007900 77  IF-D9 PICTURE X(3) VALUE "123".                              OBNC24.2
008000 77  IF-D11 PICTURE X(6) VALUE "ABCDEF".                          OBNC24.2
008100 77  IF-D13 PICTURE 9(6)V9(4) VALUE 12300.                        OBNC24.2
008200 77  IF-D14 PICTURE S9(4)V9(2) VALUE +123.45.                     OBNC24.2
008300 77  IF-D15 PICTURE S999PP VALUE 12300.                           OBNC24.2
008400 77  IF-D16 PICTURE PP99 VALUE .0012.                             OBNC24.2
008500 77  IF-D17 PICTURE SV9(4) VALUE .0012.                           OBNC24.2
008600 77  IF-D18 PICTURE X(10) VALUE "BABABABABA".                     OBNC24.2
008700 77  IF-D19 PICTURE X(10) VALUE "ABCDEF    ".                     OBNC24.2
008800 77  IF-D23 PICTURE $9,9B9.90+.                                   OBNC24.2
008900 77  IF-D24 PICTURE X(10) VALUE "$1,2 3.40+".                     OBNC24.2
009000 77  IF-D25 PICTURE ABABX0A.                                      OBNC24.2
009100 77  IF-D26 PICTURE X(8) VALUE "A C D0E".                         OBNC24.2
009200 77  IF-D27 PICTURE IS 9(6)V9(4) VALUE IS 2137.45                 OBNC24.2
009300     USAGE IS COMPUTATIONAL.                                      OBNC24.2
009400 77  IF-D28 PICTURE IS 999999V9999 VALUE IS 2137.45.              OBNC24.2
009500 77  IF-D31 PICTURE S9(6) VALUE -123.                             OBNC24.2
009600 77  IF-D32 PICTURE S9(4)V99.                                     OBNC24.2
009700     88  A  VALUE 1.                                              OBNC24.2
009800     88  B VALUES ARE 2 THRU 4.                                   OBNC24.2
009900     88  C VALUE IS ZERO.                                         OBNC24.2
010000     88  D VALUE IS +12.34.                                       OBNC24.2
010100     88  E  VALUE IS .01, .11, .21 .81.                           OBNC24.2
010200     88  F  VALUE IS 100 THRU 128 1000 THRU 1280 -9 THRU -2.      OBNC24.2
010300     88  G  VALUE IS 8765.43 1234 THRU 5678 5 -9999 THRU 10.      OBNC24.2
010400 77  IF-D33 PICTURE X(4).                                         OBNC24.2
010500     88  B   VALUE QUOTE.                                         OBNC24.2
010600     88  C   VALUE SPACE.                                         OBNC24.2
010700     88 D VALUE ALL "BAC".                                        OBNC24.2
010800 77  IF-D34 PICTURE A(4).                                         OBNC24.2
010900     88  B VALUE "A A ".                                          OBNC24.2
011000 77  IF-D37 PICTURE 9(5) VALUE 12345.                             OBNC24.2
011100 77  IF-D38 PICTURE X(9) VALUE "12345    ".                       OBNC24.2
011200 77  CCON-1 PICTURE 99 VALUE 11.                                  OBNC24.2
011300 77  CCON-2 PICTURE 99 VALUE 12.                                  OBNC24.2
011400 77  CCON-3 PICTURE 99 VALUE 13.                                  OBNC24.2
011500 77  COMP-SGN1  PICTURE S9(1) VALUE +9 COMPUTATIONAL.             OBNC24.2
011600 77  COMP-SGN2  PICTURE S9(18) VALUE +3 COMPUTATIONAL.            OBNC24.2
011700 77  COMP-SGN3  PICTURE S9(1) VALUE -5 COMPUTATIONAL.             OBNC24.2
011800 77  COMP-SGN4  PICTURE S9(18) VALUE -3167598765431 COMPUTATIONAL.OBNC24.2
011900 77  START-POINT        PICTURE 9(6) COMPUTATIONAL.               OBNC24.2
012000 77  INC-VALUE          PICTURE 9(6) COMPUTATIONAL.               OBNC24.2
012100 77  SWITCH-PFM-1 PICTURE 9 VALUE ZERO.                           OBNC24.2
012200 77  SWITCH-PFM-2 PICTURE 9 VALUE ZERO.                           OBNC24.2
012300 77  PFM-11-COUNTER PICTURE 999 VALUE ZERO.                       OBNC24.2
012400 77  PFM-12-COUNTER  PICTURE 999 VALUE 100.                       OBNC24.2
012500 77  PFM-12-ANS1  PICTURE 999 VALUE ZERO.                         OBNC24.2
012600 77  PFM-12-ANS2  PICTURE 999 VALUE ZERO.                         OBNC24.2
012700 01  SUBSCRIPT-6 PICTURE 99999 VALUE ZERO.                        OBNC24.2
012800 01  IF-TABLE.                                                    OBNC24.2
012900     02 IF-ELEM PICTURE X OCCURS 12 TIMES.                        OBNC24.2
013000 01  QUOTE-DATA.                                                  OBNC24.2
013100     02 QU-1 PICTURE X(3) VALUE "123".                            OBNC24.2
013200     02 QU-2 PICTURE X VALUE QUOTE.                               OBNC24.2
013300     02 QU-3 PICTURE X(6) VALUE "ABC456".                         OBNC24.2
013400 01  IF-D10.                                                      OBNC24.2
013500     02  D1 PICTURE X(2) VALUE "01".                              OBNC24.2
013600     02  D2 PICTURE X(2) VALUE "23".                              OBNC24.2
013700     02  D3.                                                      OBNC24.2
013800     03  D4 PICTURE X(4) VALUE "4567".                            OBNC24.2
013900     03 D5 PICTURE X(4) VALUE "8912".                             OBNC24.2
014000 01  IF-D12.                                                      OBNC24.2
014100     02  D1 PICTURE X(3) VALUE "ABC".                             OBNC24.2
014200     02  D2.                                                      OBNC24.2
014300     03  D3.                                                      OBNC24.2
014400     04  D4      PICTURE XX     VALUE "DE".                       OBNC24.2
014500     04  D5 PICTURE X VALUE "F".                                  OBNC24.2
014600 01  IF-D20.                                                      OBNC24.2
014700     02  FILLER    PICTURE 9(5)   VALUE ZERO.                     OBNC24.2
014800     02  D1 PICTURE 9(2) VALUE 12.                                OBNC24.2
014900     02  D2 PICTURE 9 VALUE 3.                                    OBNC24.2
015000     02  D3 PICTURE 9(2) VALUE 45.                                OBNC24.2
015100 01  IF-D21.                                                      OBNC24.2
015200     02  D1 PICTURE 9(5) VALUE ZEROS.                             OBNC24.2
015300     02  D2 PICTURE 9(5) VALUE 12345.                             OBNC24.2
015400 01  IF-D22.                                                      OBNC24.2
015500     02  D1 PICTURE A(2) VALUE "AB".                              OBNC24.2
015600     02  D2 PICTURE A(4) VALUE "CDEF".                            OBNC24.2
015700 01  IF-D35.                                                      OBNC24.2
015800     02  AA PICTURE X(2).                                         OBNC24.2
015900     88  A1 VALUE "AA".                                           OBNC24.2
016000     88  A2 VALUE "AB".                                           OBNC24.2
016100     02  BB PICTURE IS X(2).                                      OBNC24.2
016200     88  B1 VALUE "CC".                                           OBNC24.2
016300     88  B2 VALUE "CD".                                           OBNC24.2
016400     02 BB-2 REDEFINES BB.                                        OBNC24.2
016500     03  AAA PICTURE X.                                           OBNC24.2
016600     88  AA1 VALUE "A".                                           OBNC24.2
016700     88  AA2 VALUE "C".                                           OBNC24.2
016800     03  BBB PICTURE X.                                           OBNC24.2
016900     88  BB1    VALUE "B".                                        OBNC24.2
017000     88  BB2 VALUE "D".                                           OBNC24.2
017100 01  IF-D36  PICTURE X(120) VALUE IS    "ABCDEFGHIJKLMNOPQRSTUVWXYOBNC24.2
017200-    "Z1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890ABCDEFGHIJKLMOBNC24.2
017300-    "NOPQRSTUVWXYZ1234567890ABCDEFGHIJKL".                       OBNC24.2
017400 01  IF-D40 PICTURE 9(5) VALUE 12345                              OBNC24.2
017500              COMPUTATIONAL SYNCHRONIZED RIGHT.                   OBNC24.2
017600     88 IF-D40A VALUE ZERO THRU 10000.                            OBNC24.2
017700     88 IF-D40B VALUE 10001 THRU 99999.                           OBNC24.2
017800     88 IF-D40C VALUE 99999.                                      OBNC24.2
017900 01  PERFORM1  PICTURE XXX  VALUE SPACES.                         OBNC24.2
018000 01  PERFORM2  PICTURE S999 VALUE 20.                             OBNC24.2
018100 01  PERFORM3  PICTURE  9  VALUE  5.                              OBNC24.2
018200 01  PERFORM4  PICTURE S99V9.                                     OBNC24.2
018300 01  PERFORM5  PICTURE S99V9  VALUE 10.0.                         OBNC24.2
018400 01  PERFORM6  PICTURE  99V9.                                     OBNC24.2
018500 01  PERFORM7.                                                    OBNC24.2
018600     02  PERFORM8  OCCURS 7 TIMES  PICTURE  99V9.                 OBNC24.2
018700 01  PERFORM9  PICTURE 9   VALUE 3.                               OBNC24.2
018800 01  PERFORM10  PICTURE  S9  VALUE -1.                            OBNC24.2
018900 01  PERFORM11  PICTURE  99  VALUE 6.                             OBNC24.2
019000 01  PERFORM12.                                                   OBNC24.2
019100     02 PERFORM13  OCCURS 4 TIMES.                                OBNC24.2
019200         03 PERFORM14  OCCURS 20 TIMES  PICTURE  99V9.            OBNC24.2
019300         03 PERFORM15  OCCURS 10 TIMES.                           OBNC24.2
019400             04 PERFORM16  OCCURS 5 TIMES  PICTURE 99V9.          OBNC24.2
019500 01  PERFORM17          PICTURE 9(6) COMPUTATIONAL.               OBNC24.2
019600 01  PERFORM18          PICTURE 9(6) COMPUTATIONAL.               OBNC24.2
019700 01  PERFORM-KEY  PICTURE 9.                                      OBNC24.2
019800 01  PERFORM-SEVEN-LEVEL-TABLE.                                   OBNC24.2
019900   03   PFM71                OCCURS 2.                            OBNC24.2
020000     05  PFM72               OCCURS 2.                            OBNC24.2
020100       07  PFM73             OCCURS 2.                            OBNC24.2
020200         09  PFM74           OCCURS 2.                            OBNC24.2
020300           11  PFM75         OCCURS 2.                            OBNC24.2
020400             13  PFM76       OCCURS 2.                            OBNC24.2
020500               15  PFM77     OCCURS 2.                            OBNC24.2
020600                 17  PFM77-1 PIC X.                               OBNC24.2
020700 01  S1                      PIC S9(3) COMP.                      OBNC24.2
020800 01  S2                      PIC S9(3) COMP.                      OBNC24.2
020900 01  S3                      PIC S9(3) COMP.                      OBNC24.2
021000 01  S4                      PIC S9(3) COMP.                      OBNC24.2
021100 01  S5                      PIC S9(3) COMP.                      OBNC24.2
021200 01  S6                      PIC S9(3) COMP.                      OBNC24.2
021300 01  S7                      PIC S9(3) COMP.                      OBNC24.2
021400 01  PFM-7-TOT               PIC S9(3) COMP.                      OBNC24.2
021500 01  PFM-F4-24-TOT           PIC S9(3) COMP.                      OBNC24.2
021600 01  PFM-A                   PIC S9(3) COMP.                      OBNC24.2
021700 01  PFM-B                   PIC S9(3) COMP.                      OBNC24.2
021800 01  FILLER-A.                                                    OBNC24.2
021900   03  PFM-F4-25-A           PIC S9(3) COMP OCCURS 10.            OBNC24.2
022000 01  FILLER-B.                                                    OBNC24.2
022100   03  PFM-F4-25-B           PIC S9(3) COMP OCCURS 10.            OBNC24.2
022200 01  FILLER-C.                                                    OBNC24.2
022300   03  PFM-F4-25-C           PIC S9(3) COMP OCCURS 10.            OBNC24.2
022400 01  RECEIVING-TABLE.                                             OBNC24.2
022500     03 TBL-ELEMEN-A.                                             OBNC24.2
022600         05 TBL-ELEMEN-B          PICTURE X(18).                  OBNC24.2
022700         05 TBL-ELEMEN-C          PICTURE X(18).                  OBNC24.2
022800     03  TBL-ELEMEN-D.                                            OBNC24.2
022900         05 TBL-ELEMEN-E          PICTURE X OCCURS 36 TIMES.      OBNC24.2
023000 01  LITERAL-SPLITTER.                                            OBNC24.2
023100     02 PART1                     PICTURE X(20).                  OBNC24.2
023200     02 PART2                     PICTURE X(20).                  OBNC24.2
023300     02 PART3                     PICTURE X(20).                  OBNC24.2
023400     02 PART4                     PICTURE X(20).                  OBNC24.2
023500 01  LITERAL-TABLE REDEFINES LITERAL-SPLITTER.                    OBNC24.2
023600     02 80PARTS                   PICTURE X      OCCURS 80 TIMES. OBNC24.2
023700 01  GRP-FOR-88-LEVELS.                                           OBNC24.2
023800     03 WRK-DS-02V00-COND         PICTURE 99.                     OBNC24.2
023900         88 COND-1                VALUE IS 01 THRU 05.            OBNC24.2
024000         88 COND-2                VALUES ARE 06 THRU 10           OBNC24.2
024100                                           16 THRU 20  00.        OBNC24.2
024200         88 COND-3                VALUES 11 THRU 15.              OBNC24.2
024300 01  GRP-MOVE-CONSTANTS.                                          OBNC24.2
024400     03 GRP-GROUP-MOVE-FROM.                                      OBNC24.2
024500         04 GRP-ALPHABETIC.                                       OBNC24.2
024600             05 ALPHABET-AN-00026 PICTURE A(26)                   OBNC24.2
024700                        VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ".       OBNC24.2
024800         04 GRP-NUMERIC.                                          OBNC24.2
024900             05 DIGITS-DV-10V00   PICTURE 9(10) VALUE 0123456789. OBNC24.2
025000         05 DIGITS-DU-06V04-S REDEFINES DIGITS-DV-10V00           OBNC24.2
025100                                  PICTURE 9(6)V9999.              OBNC24.2
025200         04 GRP-ALPHANUMERIC.                                     OBNC24.2
025300             05 ALPHANUMERIC-XN-00049 PICTURE X(50)               OBNC24.2
025400     VALUE  "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-><=l,;.()/* 0123456789". OBNC24.2
025500             05 FILLER                PICTURE X  VALUE QUOTE.     OBNC24.2
025600 01  GRP-FOR-2N058.                                               OBNC24.2
025700     02 SUB-GRP-FOR-2N058-A.                                      OBNC24.2
025800         03 ELEM-FOR-2N058-A PICTURE 999  VALUE ZEROES.           OBNC24.2
025900         03 ELEM-FOR-2N058-B PICTURE XXX  VALUE ZEROS.            OBNC24.2
026000         03 ELEM-FOR-2N058-C PICTURE XXX  VALUE SPACES.           OBNC24.2
026100         03 ELEM-FOR-2N058-D PICTURE X(6) VALUE ALL "ABC".        OBNC24.2
026200         03 ELEM-FOR-2N058-E PICTURE XXX  VALUE ALL "Z".          OBNC24.2
026300         03 ELEM-FOR-2N058-F PICTURE XXX  VALUE ALL SPACES.       OBNC24.2
026400         03 ELEM-FOR-2N058-G PICTURE XXX  VALUE ALL ZEROES.       OBNC24.2
026500         03 ELEM-FOR-2N058-H PICTURE 999  VALUE ALL ZEROS.        OBNC24.2
026600         03 ELEM-FOR-2N058-I PICTURE XXX  VALUE QUOTES.           OBNC24.2
026700         03 ELEM-FOR-2N058-J PICTURE XXX  VALUE ALL QUOTES.       OBNC24.2
026800         03 ELEM-FOR-2N058-K PICTURE XXX  VALUE ALL HIGH-VALUES.  OBNC24.2
026900         03 ELEM-FOR-2N058-L PICTURE XXX  VALUE ALL LOW-VALUES.   OBNC24.2
027000         03 ELEM-FOR-2N058-M PICTURE XXX  VALUE HIGH-VALUES.      OBNC24.2
027100         03 ELEM-FOR-2N058-N PICTURE XXX  VALUE LOW-VALUES.       OBNC24.2
027200     02 SUB-GRP-FOR-2N058-B.                                      OBNC24.2
027300         03 SUB-SUB-BA.                                           OBNC24.2
027400             04 ELEM-FOR-2N058-A  PICTURE 999.                    OBNC24.2
027500             04 ELEM-FOR-2N058-B  PICTURE XXX.                    OBNC24.2
027600             04 ELEM-FOR-2N058-C  PICTURE XXX.                    OBNC24.2
027700             04 ELEM-FOR-2N058-D  PICTURE X(6).                   OBNC24.2
027800         03 SUB-SUB-BB.                                           OBNC24.2
027900             04 ELEM-FOR-2N058-E  PICTURE XXX.                    OBNC24.2
028000             04 ELEM-FOR-2N058-F  PICTURE XXX.                    OBNC24.2
028100             04 ELEM-FOR-2N058-G  PICTURE XXX.                    OBNC24.2
028200             04 ELEM-FOR-2N058-H  PICTURE 999.                    OBNC24.2
028300         03 SUB-SUB-BC.                                           OBNC24.2
028400             04 ELEM-FOR-2N058-I  PICTURE XXX.                    OBNC24.2
028500             04 ELEM-FOR-2N058-J  PICTURE XXX.                    OBNC24.2
028600             04 ELEM-FOR-2N058-K  PICTURE XXX.                    OBNC24.2
028700             04 ELEM-FOR-2N058-L  PICTURE XXX.                    OBNC24.2
028800             04 ELEM-FOR-2N058-M  PICTURE XXX.                    OBNC24.2
028900             04 ELEM-FOR-2N058-N  PICTURE XXX.                    OBNC24.2
029000 01  CHARACTER-BREAKDOWN-S.                                       OBNC24.2
029100     02   FIRST-20S PICTURE X(20).                                OBNC24.2
029200     02  SECOND-20S PICTURE X(20).                                OBNC24.2
029300     02   THIRD-20S PICTURE X(20).                                OBNC24.2
029400     02  FOURTH-20S PICTURE X(20).                                OBNC24.2
029500     02   FIFTH-20S PICTURE X(20).                                OBNC24.2
029600     02   SIXTH-20S PICTURE X(20).                                OBNC24.2
029700     02 SEVENTH-20S PICTURE X(20).                                OBNC24.2
029800     02  EIGHTH-20S PICTURE X(20).                                OBNC24.2
029900     02   NINTH-20S PICTURE X(20).                                OBNC24.2
030000     02   TENTH-20S PICTURE X(20).                                OBNC24.2
030100 01  CHARACTER-BREAKDOWN-R.                                       OBNC24.2
030200     02   FIRST-20R PICTURE X(20).                                OBNC24.2
030300     02  SECOND-20R PICTURE X(20).                                OBNC24.2
030400     02   THIRD-20R PICTURE X(20).                                OBNC24.2
030500     02  FOURTH-20R PICTURE X(20).                                OBNC24.2
030600     02   FIFTH-20R PICTURE X(20).                                OBNC24.2
030700     02   SIXTH-20R PICTURE X(20).                                OBNC24.2
030800     02 SEVENTH-20R PICTURE X(20).                                OBNC24.2
030900     02  EIGHTH-20R PICTURE X(20).                                OBNC24.2
031000     02   NINTH-20R PICTURE X(20).                                OBNC24.2
031100     02   TENTH-20R PICTURE X(20).                                OBNC24.2
031200 01  TABLE-80.                                                    OBNC24.2
031300     02  ELMT OCCURS 3 TIMES PIC 9.                               OBNC24.2
031400     88  A80  VALUES ARE ZERO THRU 7.                             OBNC24.2
031500     88  B80  VALUE 8.                                            OBNC24.2
031600     88  C80  VALUES ARE 7, 8 THROUGH 9.                          OBNC24.2
031700                                                                  OBNC24.2
031800 01  TABLE-86.                                                    OBNC24.2
031900     88  A86  VALUE "ABC".                                        OBNC24.2
032000     88  B86  VALUE "ABCABC".                                     OBNC24.2
032100     88  C86  VALUE "   ABC".                                     OBNC24.2
032200     02  DATANAME-86  PIC XXX  VALUE "ABC".                       OBNC24.2
032300     02  DNAME-86.                                                OBNC24.2
032400         03  FILLER  PIC X  VALUE "A".                            OBNC24.2
032500         03  FILLER  PIC X  VALUE "B".                            OBNC24.2
032600         03  FILLER  PIC X   VALUE "C".                           OBNC24.2
032700 01  FIGCON-DATA.                                                 OBNC24.2
032800     02 SPACE-X         PICTURE X(10) VALUE "          ".         OBNC24.2
032900     02 QUOTE-X         PICTURE X(5)  VALUE QUOTE.                OBNC24.2
033000     02 LOW-VAL         PICTURE X(5)  VALUE LOW-VALUE.            OBNC24.2
033100     02 ABC PICTURE XXX VALUE "ABC".                              OBNC24.2
033200     02 ONE23           PICTURE 9999  VALUE 123.                  OBNC24.2
033300     02 ZERO-C          PICTURE 9(10) VALUE 0 COMPUTATIONAL.      OBNC24.2
033400     02 ZERO-D          PICTURE 9     VALUE ZERO USAGE DISPLAY.   OBNC24.2
033500 01  TEST-RESULTS.                                                OBNC24.2
033600     02 FILLER                   PIC X      VALUE SPACE.          OBNC24.2
033700     02 FEATURE                  PIC X(20)  VALUE SPACE.          OBNC24.2
033800     02 FILLER                   PIC X      VALUE SPACE.          OBNC24.2
033900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          OBNC24.2
034000     02 FILLER                   PIC X      VALUE SPACE.          OBNC24.2
034100     02  PAR-NAME.                                                OBNC24.2
034200       03 FILLER                 PIC X(19)  VALUE SPACE.          OBNC24.2
034300       03  PARDOT-X              PIC X      VALUE SPACE.          OBNC24.2
034400       03 DOTVALUE               PIC 99     VALUE ZERO.           OBNC24.2
034500     02 FILLER                   PIC X(8)   VALUE SPACE.          OBNC24.2
034600     02 RE-MARK                  PIC X(61).                       OBNC24.2
034700 01  TEST-COMPUTED.                                               OBNC24.2
034800     02 FILLER                   PIC X(30)  VALUE SPACE.          OBNC24.2
034900     02 FILLER                   PIC X(17)  VALUE                 OBNC24.2
035000            "       COMPUTED=".                                   OBNC24.2
035100     02 COMPUTED-X.                                               OBNC24.2
035200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          OBNC24.2
035300     03 COMPUTED-N               REDEFINES COMPUTED-A             OBNC24.2
035400                                 PIC -9(9).9(9).                  OBNC24.2
035500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         OBNC24.2
035600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     OBNC24.2
035700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     OBNC24.2
035800     03       CM-18V0 REDEFINES COMPUTED-A.                       OBNC24.2
035900         04 COMPUTED-18V0                    PIC -9(18).          OBNC24.2
036000         04 FILLER                           PIC X.               OBNC24.2
036100     03 FILLER PIC X(50) VALUE SPACE.                             OBNC24.2
036200 01  TEST-CORRECT.                                                OBNC24.2
036300     02 FILLER PIC X(30) VALUE SPACE.                             OBNC24.2
036400     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBNC24.2
036500     02 CORRECT-X.                                                OBNC24.2
036600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         OBNC24.2
036700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      OBNC24.2
036800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         OBNC24.2
036900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     OBNC24.2
037000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     OBNC24.2
037100     03      CR-18V0 REDEFINES CORRECT-A.                         OBNC24.2
037200         04 CORRECT-18V0                     PIC -9(18).          OBNC24.2
037300         04 FILLER                           PIC X.               OBNC24.2
037400     03 FILLER PIC X(2) VALUE SPACE.                              OBNC24.2
037500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     OBNC24.2
037600 01  CCVS-C-1.                                                    OBNC24.2
037700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAOBNC24.2
037800-    "SS  PARAGRAPH-NAME                                          OBNC24.2
037900-    "       REMARKS".                                            OBNC24.2
038000     02 FILLER                     PIC X(20)    VALUE SPACE.      OBNC24.2
038100 01  CCVS-C-2.                                                    OBNC24.2
038200     02 FILLER                     PIC X        VALUE SPACE.      OBNC24.2
038300     02 FILLER                     PIC X(6)     VALUE "TESTED".   OBNC24.2
038400     02 FILLER                     PIC X(15)    VALUE SPACE.      OBNC24.2
038500     02 FILLER                     PIC X(4)     VALUE "FAIL".     OBNC24.2
038600     02 FILLER                     PIC X(94)    VALUE SPACE.      OBNC24.2
038700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       OBNC24.2
038800 01  REC-CT                        PIC 99       VALUE ZERO.       OBNC24.2
038900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       OBNC24.2
039000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       OBNC24.2
039100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       OBNC24.2
039200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       OBNC24.2
039300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       OBNC24.2
039400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       OBNC24.2
039500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      OBNC24.2
039600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       OBNC24.2
039700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     OBNC24.2
039800 01  CCVS-H-1.                                                    OBNC24.2
039900     02  FILLER                    PIC X(39)    VALUE SPACES.     OBNC24.2
040000     02  FILLER                    PIC X(42)    VALUE             OBNC24.2
040100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 OBNC24.2
040200     02  FILLER                    PIC X(39)    VALUE SPACES.     OBNC24.2
040300 01  CCVS-H-2A.                                                   OBNC24.2
040400   02  FILLER                        PIC X(40)  VALUE SPACE.      OBNC24.2
040500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  OBNC24.2
040600   02  FILLER                        PIC XXXX   VALUE             OBNC24.2
040700     "4.2 ".                                                      OBNC24.2
040800   02  FILLER                        PIC X(28)  VALUE             OBNC24.2
040900            " COPY - NOT FOR DISTRIBUTION".                       OBNC24.2
041000   02  FILLER                        PIC X(41)  VALUE SPACE.      OBNC24.2
041100                                                                  OBNC24.2
041200 01  CCVS-H-2B.                                                   OBNC24.2
041300   02  FILLER                        PIC X(15)  VALUE             OBNC24.2
041400            "TEST RESULT OF ".                                    OBNC24.2
041500   02  TEST-ID                       PIC X(9).                    OBNC24.2
041600   02  FILLER                        PIC X(4)   VALUE             OBNC24.2
041700            " IN ".                                               OBNC24.2
041800   02  FILLER                        PIC X(12)  VALUE             OBNC24.2
041900     " HIGH       ".                                              OBNC24.2
042000   02  FILLER                        PIC X(22)  VALUE             OBNC24.2
042100            " LEVEL VALIDATION FOR ".                             OBNC24.2
042200   02  FILLER                        PIC X(58)  VALUE             OBNC24.2
042300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBNC24.2
042400 01  CCVS-H-3.                                                    OBNC24.2
042500     02  FILLER                      PIC X(34)  VALUE             OBNC24.2
042600            " FOR OFFICIAL USE ONLY    ".                         OBNC24.2
042700     02  FILLER                      PIC X(58)  VALUE             OBNC24.2
042800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBNC24.2
042900     02  FILLER                      PIC X(28)  VALUE             OBNC24.2
043000            "  COPYRIGHT   1985 ".                                OBNC24.2
043100 01  CCVS-E-1.                                                    OBNC24.2
043200     02 FILLER                       PIC X(52)  VALUE SPACE.      OBNC24.2
043300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              OBNC24.2
043400     02 ID-AGAIN                     PIC X(9).                    OBNC24.2
043500     02 FILLER                       PIC X(45)  VALUE SPACES.     OBNC24.2
043600 01  CCVS-E-2.                                                    OBNC24.2
043700     02  FILLER                      PIC X(31)  VALUE SPACE.      OBNC24.2
043800     02  FILLER                      PIC X(21)  VALUE SPACE.      OBNC24.2
043900     02 CCVS-E-2-2.                                               OBNC24.2
044000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      OBNC24.2
044100         03 FILLER                   PIC X      VALUE SPACE.      OBNC24.2
044200         03 ENDER-DESC               PIC X(44)  VALUE             OBNC24.2
044300            "ERRORS ENCOUNTERED".                                 OBNC24.2
044400 01  CCVS-E-3.                                                    OBNC24.2
044500     02  FILLER                      PIC X(22)  VALUE             OBNC24.2
044600            " FOR OFFICIAL USE ONLY".                             OBNC24.2
044700     02  FILLER                      PIC X(12)  VALUE SPACE.      OBNC24.2
044800     02  FILLER                      PIC X(58)  VALUE             OBNC24.2
044900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBNC24.2
045000     02  FILLER                      PIC X(13)  VALUE SPACE.      OBNC24.2
045100     02 FILLER                       PIC X(15)  VALUE             OBNC24.2
045200             " COPYRIGHT 1985".                                   OBNC24.2
045300 01  CCVS-E-4.                                                    OBNC24.2
045400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      OBNC24.2
045500     02 FILLER                       PIC X(4)   VALUE " OF ".     OBNC24.2
045600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      OBNC24.2
045700     02 FILLER                       PIC X(40)  VALUE             OBNC24.2
045800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBNC24.2
045900 01  XXINFO.                                                      OBNC24.2
046000     02 FILLER                       PIC X(19)  VALUE             OBNC24.2
046100            "*** INFORMATION ***".                                OBNC24.2
046200     02 INFO-TEXT.                                                OBNC24.2
046300       04 FILLER                     PIC X(8)   VALUE SPACE.      OBNC24.2
046400       04 XXCOMPUTED                 PIC X(20).                   OBNC24.2
046500       04 FILLER                     PIC X(5)   VALUE SPACE.      OBNC24.2
046600       04 XXCORRECT                  PIC X(20).                   OBNC24.2
046700     02 INF-ANSI-REFERENCE           PIC X(48).                   OBNC24.2
046800 01  HYPHEN-LINE.                                                 OBNC24.2
046900     02 FILLER  PIC IS X VALUE IS SPACE.                          OBNC24.2
047000     02 FILLER  PIC IS X(65)    VALUE IS "************************OBNC24.2
047100-    "*****************************************".                 OBNC24.2
047200     02 FILLER  PIC IS X(54)    VALUE IS "************************OBNC24.2
047300-    "******************************".                            OBNC24.2
047400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             OBNC24.2
047500     "OBNC2M".                                                    OBNC24.2
047600 PROCEDURE DIVISION.                                              OBNC24.2
047700 CCVS1 SECTION.                                                   OBNC24.2
047800 OPEN-FILES.                                                      OBNC24.2
047900     OPEN     OUTPUT PRINT-FILE.                                  OBNC24.2
048000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBNC24.2
048100     MOVE    SPACE TO TEST-RESULTS.                               OBNC24.2
048200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBNC24.2
048300     GO TO CCVS1-EXIT.                                            OBNC24.2
048400 CLOSE-FILES.                                                     OBNC24.2
048500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBNC24.2
048600 TERMINATE-CCVS.                                                  OBNC24.2
048700*    EXIT PROGRAM.                                                OBNC24.2
048800*TERMINATE-CALL.                                                  OBNC24.2
048900     STOP     RUN.                                                OBNC24.2
049000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBNC24.2
049100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBNC24.2
049200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBNC24.2
049300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      OBNC24.2
049400     MOVE "****TEST DELETED****" TO RE-MARK.                      OBNC24.2
049500 PRINT-DETAIL.                                                    OBNC24.2
049600     IF REC-CT NOT EQUAL TO ZERO                                  OBNC24.2
049700             MOVE "." TO PARDOT-X                                 OBNC24.2
049800             MOVE REC-CT TO DOTVALUE.                             OBNC24.2
049900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBNC24.2
050000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBNC24.2
050100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBNC24.2
050200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBNC24.2
050300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBNC24.2
050400     MOVE SPACE TO CORRECT-X.                                     OBNC24.2
050500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBNC24.2
050600     MOVE     SPACE TO RE-MARK.                                   OBNC24.2
050700 HEAD-ROUTINE.                                                    OBNC24.2
050800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  OBNC24.2
050900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  OBNC24.2
051000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  OBNC24.2
051100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  OBNC24.2
051200 COLUMN-NAMES-ROUTINE.                                            OBNC24.2
051300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBNC24.2
051400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC24.2
051500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBNC24.2
051600 END-ROUTINE.                                                     OBNC24.2
051700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBNC24.2
051800 END-RTN-EXIT.                                                    OBNC24.2
051900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC24.2
052000 END-ROUTINE-1.                                                   OBNC24.2
052100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBNC24.2
052200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               OBNC24.2
052300      ADD PASS-COUNTER TO ERROR-HOLD.                             OBNC24.2
052400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBNC24.2
052500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBNC24.2
052600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBNC24.2
052700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBNC24.2
052800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBNC24.2
052900  END-ROUTINE-12.                                                 OBNC24.2
053000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBNC24.2
053100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBNC24.2
053200         MOVE "NO " TO ERROR-TOTAL                                OBNC24.2
053300         ELSE                                                     OBNC24.2
053400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBNC24.2
053500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBNC24.2
053600     PERFORM WRITE-LINE.                                          OBNC24.2
053700 END-ROUTINE-13.                                                  OBNC24.2
053800     IF DELETE-COUNTER IS EQUAL TO ZERO                           OBNC24.2
053900         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBNC24.2
054000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      OBNC24.2
054100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBNC24.2
054200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBNC24.2
054300      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBNC24.2
054400          MOVE "NO " TO ERROR-TOTAL                               OBNC24.2
054500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBNC24.2
054600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBNC24.2
054700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBNC24.2
054800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBNC24.2
054900 WRITE-LINE.                                                      OBNC24.2
055000     ADD 1 TO RECORD-COUNT.                                       OBNC24.2
055100*    IF RECORD-COUNT GREATER 50                                   OBNC24.2
055200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBNC24.2
055300*        MOVE SPACE TO DUMMY-RECORD                               OBNC24.2
055400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBNC24.2
055500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             OBNC24.2
055600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     OBNC24.2
055700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBNC24.2
055800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBNC24.2
055900*        MOVE ZERO TO RECORD-COUNT.                               OBNC24.2
056000     PERFORM WRT-LN.                                              OBNC24.2
056100 WRT-LN.                                                          OBNC24.2
056200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBNC24.2
056300     MOVE SPACE TO DUMMY-RECORD.                                  OBNC24.2
056400 BLANK-LINE-PRINT.                                                OBNC24.2
056500     PERFORM WRT-LN.                                              OBNC24.2
056600 FAIL-ROUTINE.                                                    OBNC24.2
056700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. OBNC24.2
056800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.OBNC24.2
056900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 OBNC24.2
057000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   OBNC24.2
057100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC24.2
057200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         OBNC24.2
057300     GO TO  FAIL-ROUTINE-EX.                                      OBNC24.2
057400 FAIL-ROUTINE-WRITE.                                              OBNC24.2
057500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         OBNC24.2
057600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 OBNC24.2
057700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. OBNC24.2
057800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         OBNC24.2
057900 FAIL-ROUTINE-EX. EXIT.                                           OBNC24.2
058000 BAIL-OUT.                                                        OBNC24.2
058100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   OBNC24.2
058200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           OBNC24.2
058300 BAIL-OUT-WRITE.                                                  OBNC24.2
058400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBNC24.2
058500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 OBNC24.2
058600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC24.2
058700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         OBNC24.2
058800 BAIL-OUT-EX. EXIT.                                               OBNC24.2
058900 CCVS1-EXIT.                                                      OBNC24.2
059000     EXIT.                                                        OBNC24.2
059100 SECT-OBNC2M-001 SECTION.                                         OBNC24.2
059200*                                                                 OBNC24.2
059300 GO--TEST-1.                                                      OBNC24.2
059400     ALTER GO--A TO PROCEED TO GO--C.                             OBNC24.2
059500*    NOTE THE GO STATEMENT IN GO--A IS NOT LEGAL UNLESS IT IS     OBNC24.2
059600*        ALTERED AS SHOWN ABOVE BEFORE CONTROL PASSES TO IT.      OBNC24.2
059700     GO TO GO--A.                                                 OBNC24.2
059800 GO--DELETE-1.                                                    OBNC24.2
059900     PERFORM DE-LETE.                                             OBNC24.2
060000     GO TO GO--WRITE-1.                                           OBNC24.2
060100 GO--A.                                                           OBNC24.2
060200     GO TO.                                                       OBNC24.2
060300 GO--B.                                                           OBNC24.2
060400     PERFORM FAIL.                                                OBNC24.2
060500     GO TO GO--WRITE-1.                                           OBNC24.2
060600 GO--C.                                                           OBNC24.2
060700     PERFORM PASS.                                                OBNC24.2
060800 GO--WRITE-1.                                                     OBNC24.2
060900     PERFORM END-ROUTINE.                                         OBNC24.2
061000     MOVE "UNFINISHED GO TO" TO FEATURE.                          OBNC24.2
061100     MOVE "GO--TEST-1" TO PAR-NAME.                               OBNC24.2
061200     PERFORM PRINT-DETAIL.                                        OBNC24.2
061300 ALTER-TEST-1.                                                    OBNC24.2
061400     ALTER ALTER-A TO PROCEED TO ALTER-C                          OBNC24.2
061500           ALTER-D TO PROCEED TO ALTER-F                          OBNC24.2
061600           ALTER-F TO PROCEED TO ALTER-H.                         OBNC24.2
061700     GO TO ALTER-A.                                               OBNC24.2
061800 ALTER-DELETE-1.                                                  OBNC24.2
061900     PERFORM DE-LETE.                                             OBNC24.2
062000     GO TO ALTER-WRITE-1.                                         OBNC24.2
062100 ALTER-A.                                                         OBNC24.2
062200     GO TO ALTER-B.                                               OBNC24.2
062300 ALTER-B.                                                         OBNC24.2
062400     ADD 1 TO ALTERCOUNT.                                         OBNC24.2
062500     GO TO ALTER-FAIL-1.                                          OBNC24.2
062600 ALTER-C.                                                         OBNC24.2
062700     PERFORM PASS.                                                OBNC24.2
062800 ALTER-D.                                                         OBNC24.2
062900     GO TO ALTER-E.                                               OBNC24.2
063000 ALTER-E.                                                         OBNC24.2
063100     ADD 10 TO ALTERCOUNT.                                        OBNC24.2
063200     GO TO ALTER-FAIL-1.                                          OBNC24.2
063300 ALTER-F.                                                         OBNC24.2
063400     GO TO ALTER-G.                                               OBNC24.2
063500 ALTER-G.                                                         OBNC24.2
063600     ADD 100 TO ALTERCOUNT.                                       OBNC24.2
063700     GO TO ALTER-FAIL-1.                                          OBNC24.2
063800 ALTER-H.                                                         OBNC24.2
063900     GO TO ALTER-WRITE-1.                                         OBNC24.2
064000 ALTER-FAIL-1.                                                    OBNC24.2
064100     MOVE ALTERCOUNT TO COMPUTED-N.                               OBNC24.2
064200     MOVE ZERO TO CORRECT-N.                                      OBNC24.2
064300     PERFORM FAIL.                                                OBNC24.2
064400 ALTER-WRITE-1.                                                   OBNC24.2
064500     PERFORM END-ROUTINE.                                         OBNC24.2
064600     MOVE "SERIES ALTER" TO FEATURE.                              OBNC24.2
064700     MOVE "ALTER-TEST-1" TO PAR-NAME.                             OBNC24.2
064800     PERFORM PRINT-DETAIL.                                        OBNC24.2
064900 ALTER-INIT-B.                                                    OBNC24.2
065000     MOVE     "SERIES ALTER" TO FEATURE.                          OBNC24.2
065100 ALTER-TEST-2.                                                    OBNC24.2
065200     MOVE     ZERO TO SUBSCRIPT-6.                                OBNC24.2
065300     MOVE     SPACE TO RECEIVING-TABLE.                           OBNC24.2
065400 ALTER-TESTT-2.                                                   OBNC24.2
065500     GO       TO ALTER-TESTTT-2.                                  OBNC24.2
065600 ALTER-A-2.                                                       OBNC24.2
065700     GO       TO ALTER-C-2.                                       OBNC24.2
065800 ALTER-B-2.                                                       OBNC24.2
065900     MOVE     "M" TO WRK-XN-00001.                                OBNC24.2
066000     PERFORM  ALTER-G-2.                                          OBNC24.2
066100 ALTER-C-2.                                                       OBNC24.2
066200     MOVE     "N" TO WRK-XN-00001.                                OBNC24.2
066300     PERFORM  ALTER-G-2.                                          OBNC24.2
066400     MOVE     " " TO WRK-XN-00001.                                OBNC24.2
066500     PERFORM  ALTER-G-2.                                          OBNC24.2
066600 ALTER-D-2.                                                       OBNC24.2
066700     GO       TO ALTER-F-2.                                       OBNC24.2
066800 ALTER-E-2.                                                       OBNC24.2
066900     MOVE     "O" TO WRK-XN-00001.                                OBNC24.2
067000     PERFORM  ALTER-G-2.                                          OBNC24.2
067100 ALTER-F-2.                                                       OBNC24.2
067200     MOVE     "P" TO WRK-XN-00001.                                OBNC24.2
067300     PERFORM  ALTER-G-2.                                          OBNC24.2
067400     MOVE     " " TO WRK-XN-00001.                                OBNC24.2
067500     PERFORM  ALTER-G-2.                                          OBNC24.2
067600 ALTER-G-2.                                                       OBNC24.2
067700     ADD      1 TO SUBSCRIPT-6.                                   OBNC24.2
067800     MOVE     WRK-XN-00001 TO TBL-ELEMEN-E (SUBSCRIPT-6).         OBNC24.2
067900 ALTER-TESTTT-2.                                                  OBNC24.2
068000     PERFORM  ALTER-A-2 THRU ALTER-F-2.                           OBNC24.2
068100     ALTER    ALTER-A-2 TO PROCEED TO ALTER-B-2                   OBNC24.2
068200              ALTER-TESTT-2 TO PROCEED TO ALTER-TESTT-2           OBNC24.2
068300              ALTER-D-2 TO PROCEED TO ALTER-E-2.                  OBNC24.2
068400     PERFORM  ALTER-A-2 THRU ALTER-F-2.                           OBNC24.2
068500     PERFORM  ALTER-A-2 THRU ALTER-F-2.                           OBNC24.2
068600     MOVE     TBL-ELEMEN-D TO TBL-ELEMEN-B.                       OBNC24.2
068700     IF       TBL-ELEMEN-B EQUAL TO "N P MN OP MN OP "            OBNC24.2
068800              PERFORM PASS GO TO ALTER-WRITE-2.                   OBNC24.2
068900     GO       TO ALTER-FAIL-2.                                    OBNC24.2
069000 ALTER-DELETE-2.                                                  OBNC24.2
069100     PERFORM  DE-LETE.                                            OBNC24.2
069200     GO       TO ALTER-WRITE-2.                                   OBNC24.2
069300 ALTER-FAIL-2.                                                    OBNC24.2
069400     MOVE     TBL-ELEMEN-B TO COMPUTED-A.                         OBNC24.2
069500     MOVE     "N P MN OP MN OP " TO CORRECT-A.                    OBNC24.2
069600     PERFORM  FAIL.                                               OBNC24.2
069700 ALTER-WRITE-2.                                                   OBNC24.2
069800     MOVE     "ALTER-TEST-2" TO PAR-NAME.                         OBNC24.2
069900     PERFORM  PRINT-DETAIL.                                       OBNC24.2
070000 ALTER-INIT-3.                                                    OBNC24.2
070100*    NOTE  THE FOLLOWING TESTS UTILIZE THE ALTER STATEMENT WITH   OBNC24.2
070200*          11 OPERANDS  A DELETE IN ALTER-TEST-3 WILL CAUSE THE   OBNC24.2
070300*         REST OF THE ALTER TESTS TO BE BYPASSED.                 OBNC24.2
070400 ALTER-TEST-3.                                                    OBNC24.2
070500     ALTER TEST-3A TO PROCEED TO TEST-3C  TEST-4A TO TEST-4C      OBNC24.2
070600         TEST-5A TO TEST-5B  TEST-6A TO TEST-6C  TEST-7A TO       OBNC24.2
070700         TEST-7B  TEST-8B TO PROCEED TO TEST-8C  TEST-9A TO       OBNC24.2
070800         TEST-9C  TEST-10A TO TEST-10C  TEST-11A TO TEST-11C      OBNC24.2
070900         TEST-12B TO PROCEED TO TEST-12C  TEST-13A TO TEST-13B.   OBNC24.2
071000     GO TO TEST-3A.                                               OBNC24.2
071100 ALTER-DELETE-3.                                                  OBNC24.2
071200     PERFORM DE-LETE.                                             OBNC24.2
071300     MOVE "ALTER-TEST-3 THRU 13" TO PAR-NAME.                     OBNC24.2
071400     PERFORM PRINT-DETAIL.                                        OBNC24.2
071500     GO TO ALTER-EXIT.                                            OBNC24.2
071600 TEST-3A.                                                         OBNC24.2
071700     GO TO TEST-3B.                                               OBNC24.2
071800 TEST-3B.                                                         OBNC24.2
071900     MOVE "TEST-3C " TO CORRECT-A.                                OBNC24.2
072000     MOVE "TEST-3B " TO COMPUTED-A.                               OBNC24.2
072100     PERFORM FAIL.                                                OBNC24.2
072200     GO TO ALTER-WRITE-3.                                         OBNC24.2
072300 TEST-3C.                                                         OBNC24.2
072400     PERFORM PASS.                                                OBNC24.2
072500 ALTER-WRITE-3.                                                   OBNC24.2
072600     MOVE "ALTER-TEST-3 " TO PAR-NAME.                            OBNC24.2
072700     PERFORM PRINT-DETAIL.                                        OBNC24.2
072800 ALTER-TEST-4.                                                    OBNC24.2
072900     GO TO TEST-4A.                                               OBNC24.2
073000 TEST-4A.                                                         OBNC24.2
073100     GO TO TEST-4B.                                               OBNC24.2
073200 TEST-4B.                                                         OBNC24.2
073300     MOVE "TEST-4B " TO COMPUTED-A.                               OBNC24.2
073400     MOVE "TEST-4C " TO CORRECT-A.                                OBNC24.2
073500     PERFORM FAIL.                                                OBNC24.2
073600     GO TO ALTER-WRITE-4.                                         OBNC24.2
073700 TEST-4C.                                                         OBNC24.2
073800     PERFORM PASS.                                                OBNC24.2
073900 ALTER-WRITE-4.                                                   OBNC24.2
074000     MOVE "ALTER-TEST-4 " TO PAR-NAME.                            OBNC24.2
074100     PERFORM PRINT-DETAIL.                                        OBNC24.2
074200 ALTER-TEST-5.                                                    OBNC24.2
074300     GO TO TEST-5A.                                               OBNC24.2
074400 TEST-5B.                                                         OBNC24.2
074500     PERFORM PASS                                                 OBNC24.2
074600     GO TO ALTER-WRITE-5.                                         OBNC24.2
074700 TEST-5A.                                                         OBNC24.2
074800     GO TO TEST-5C.                                               OBNC24.2
074900 TEST-5C.                                                         OBNC24.2
075000     MOVE "TEST-5C " TO COMPUTED-A.                               OBNC24.2
075100     MOVE "TEST-5B " TO CORRECT-A.                                OBNC24.2
075200     PERFORM FAIL.                                                OBNC24.2
075300 ALTER-WRITE-5.                                                   OBNC24.2
075400     MOVE "ALTER-TEST-5 " TO PAR-NAME.                            OBNC24.2
075500     PERFORM PRINT-DETAIL.                                        OBNC24.2
075600 ALTER-TEST-6.                                                    OBNC24.2
075700     GO TO TEST-6A.                                               OBNC24.2
075800 TEST-6B.                                                         OBNC24.2
075900     MOVE "TEST-6B " TO COMPUTED-A.                               OBNC24.2
076000     MOVE "TEST-6C " TO CORRECT-A.                                OBNC24.2
076100     PERFORM FAIL.                                                OBNC24.2
076200     GO TO ALTER-WRITE-6.                                         OBNC24.2
076300 TEST-6A.                                                         OBNC24.2
076400     GO TO TEST-6B.                                               OBNC24.2
076500 TEST-6C.                                                         OBNC24.2
076600     PERFORM PASS.                                                OBNC24.2
076700 ALTER-WRITE-6.                                                   OBNC24.2
076800     MOVE "ALTER-TEST-6 " TO PAR-NAME.                            OBNC24.2
076900     PERFORM PRINT-DETAIL.                                        OBNC24.2
077000 ALTER-TEST-7.                                                    OBNC24.2
077100     GO TO TEST-7A.                                               OBNC24.2
077200 TEST-7B.                                                         OBNC24.2
077300     PERFORM PASS.                                                OBNC24.2
077400     GO TO ALTER-WRITE-7.                                         OBNC24.2
077500 TEST-7A.                                                         OBNC24.2
077600     GO TO TEST-7C.                                               OBNC24.2
077700 TEST-7C.                                                         OBNC24.2
077800     MOVE "TEST-7C " TO COMPUTED-A.                               OBNC24.2
077900     MOVE "TEST-7B " TO CORRECT-A.                                OBNC24.2
078000     PERFORM FAIL.                                                OBNC24.2
078100 ALTER-WRITE-7.                                                   OBNC24.2
078200     MOVE "ALTER-TEST-7 " TO PAR-NAME.                            OBNC24.2
078300     PERFORM PRINT-DETAIL.                                        OBNC24.2
078400 ALTER-TEST-8.                                                    OBNC24.2
078500     GO TO TEST-8B.                                               OBNC24.2
078600 TEST-8B.                                                         OBNC24.2
078700     GO TO TEST-8A.                                               OBNC24.2
078800 TEST-8C.                                                         OBNC24.2
078900     PERFORM PASS.                                                OBNC24.2
079000     GO TO ALTER-WRITE-8.                                         OBNC24.2
079100 TEST-8A.                                                         OBNC24.2
079200     MOVE "TEST-8A " TO COMPUTED-A.                               OBNC24.2
079300     MOVE "TEST-8C " TO CORRECT-A.                                OBNC24.2
079400     PERFORM FAIL.                                                OBNC24.2
079500 ALTER-WRITE-8.                                                   OBNC24.2
079600     MOVE "ALTER-TEST-8 " TO PAR-NAME.                            OBNC24.2
079700     PERFORM PRINT-DETAIL.                                        OBNC24.2
079800 ALTER-TEST-9.                                                    OBNC24.2
079900     GO TO TEST-9A.                                               OBNC24.2
080000 TEST-9B.                                                         OBNC24.2
080100     MOVE "TEST-9B " TO COMPUTED-A.                               OBNC24.2
080200     MOVE "TEST-9C " TO CORRECT-A.                                OBNC24.2
080300     PERFORM FAIL.                                                OBNC24.2
080400     GO TO ALTER-WRITE-9.                                         OBNC24.2
080500 TEST-9A.                                                         OBNC24.2
080600     GO TO TEST-9B.                                               OBNC24.2
080700 TEST-9C.                                                         OBNC24.2
080800     PERFORM PASS.                                                OBNC24.2
080900 ALTER-WRITE-9.                                                   OBNC24.2
081000     MOVE "ALTER-TEST-9 " TO PAR-NAME.                            OBNC24.2
081100     PERFORM PRINT-DETAIL.                                        OBNC24.2
081200 ALTER-TEST-10.                                                   OBNC24.2
081300     GO TO TEST-10A.                                              OBNC24.2
081400 TEST-10B.                                                        OBNC24.2
081500     MOVE "TEST-10B " TO COMPUTED-A.                              OBNC24.2
081600     MOVE "TEST-10C " TO CORRECT-A.                               OBNC24.2
081700     PERFORM FAIL.                                                OBNC24.2
081800 ALTER-WRITE-10.                                                  OBNC24.2
081900     MOVE "ALTER-TEST-10 " TO PAR-NAME.                           OBNC24.2
082000     PERFORM PRINT-DETAIL.                                        OBNC24.2
082100 ALTER-TEST-11.                                                   OBNC24.2
082200     GO TO TEST-11A.                                              OBNC24.2
082300 TEST-10A.                                                        OBNC24.2
082400     GO TO TEST-10B.                                              OBNC24.2
082500 TEST-10C.                                                        OBNC24.2
082600     PERFORM PASS.                                                OBNC24.2
082700     GO TO ALTER-WRITE-10.                                        OBNC24.2
082800 TEST-11A.                                                        OBNC24.2
082900     GO TO TEST-11B.                                              OBNC24.2
083000 TEST-11B.                                                        OBNC24.2
083100     MOVE "TEST-11B " TO COMPUTED-A.                              OBNC24.2
083200     MOVE "TEST-11C " TO CORRECT-A.                               OBNC24.2
083300     PERFORM FAIL.                                                OBNC24.2
083400     GO TO ALTER-WRITE-11.                                        OBNC24.2
083500 TEST-11C.                                                        OBNC24.2
083600     PERFORM PASS.                                                OBNC24.2
083700 ALTER-WRITE-11.                                                  OBNC24.2
083800     MOVE "ALTER-TEST-11 " TO PAR-NAME.                           OBNC24.2
083900     PERFORM PRINT-DETAIL.                                        OBNC24.2
084000 ALTER-TEST-12.                                                   OBNC24.2
084100     GO TO TEST-12B.                                              OBNC24.2
084200 TEST-12A.                                                        OBNC24.2
084300     MOVE "TEST-12A " TO COMPUTED-A.                              OBNC24.2
084400     MOVE "TEST-12C " TO CORRECT-A.                               OBNC24.2
084500     PERFORM FAIL.                                                OBNC24.2
084600     GO TO ALTER-WRITE-12.                                        OBNC24.2
084700 TEST-12B.                                                        OBNC24.2
084800     GO TO TEST-12A.                                              OBNC24.2
084900 TEST-12C.                                                        OBNC24.2
085000     PERFORM PASS.                                                OBNC24.2
085100 ALTER-WRITE-12.                                                  OBNC24.2
085200     MOVE "ALTER-TEST-12 " TO PAR-NAME.                           OBNC24.2
085300     PERFORM PRINT-DETAIL.                                        OBNC24.2
085400 ALTER-TEST-13.                                                   OBNC24.2
085500     GO TO TEST-13A.                                              OBNC24.2
085600 TEST-13C.                                                        OBNC24.2
085700     MOVE "TEST-13C " TO COMPUTED-A.                              OBNC24.2
085800     MOVE "TEST-13B " TO CORRECT-A.                               OBNC24.2
085900     PERFORM FAIL.                                                OBNC24.2
086000     GO TO ALTER-WRITE-13.                                        OBNC24.2
086100 TEST-13A.                                                        OBNC24.2
086200     GO TO TEST-13C.                                              OBNC24.2
086300 TEST-13B.                                                        OBNC24.2
086400     PERFORM PASS.                                                OBNC24.2
086500 ALTER-WRITE-13.                                                  OBNC24.2
086600     MOVE "ALTER-TEST-13 " TO PAR-NAME.                           OBNC24.2
086700     PERFORM PRINT-DETAIL.                                        OBNC24.2
086800 ALTER-EXIT.                                                      OBNC24.2
086900     EXIT.                                                        OBNC24.2
087000 DATE-TEST-1.                                                     OBNC24.2
087100     MOVE     "DATE-COMPILED" TO FEATURE, PAR-NAME.               OBNC24.2
087200     MOVE     "SEE SOURCE LISTING" TO COMPUTED-A.                 OBNC24.2
087300     MOVE     "COMMENT SHOULD BE DELETED" TO RE-MARK.             OBNC24.2
087400     PERFORM  PRINT-DETAIL.                                       OBNC24.2
087500 QUAL-SECTION-1 SECTION.                                          OBNC24.2
087600 PARA-TEST-5.                                                     OBNC24.2
087700     ALTER PARA-5A IN QUAL-SECTION-1 TO PROCEED TO PARA-5C OF     OBNC24.2
087800     QUAL-SECTION-2.                                              OBNC24.2
087900 PARA-5A.                                                         OBNC24.2
088000     GO TO PARA-5C OF QUAL-SECTION-1.                             OBNC24.2
088100 PARA-5B.                                                         OBNC24.2
088200     MOVE "FAIL" TO QT5.                                          OBNC24.2
088300     GO TO PARA-5D.                                               OBNC24.2
088400 PARA-5C.                                                         OBNC24.2
088500     MOVE "FAIL" TO QT5.                                          OBNC24.2
088600 PARA-5D.                                                         OBNC24.2
088700                   IF QT5 EQUAL TO "PASS"                         OBNC24.2
088800     PERFORM PASS ELSE                                            OBNC24.2
088900     PERFORM FAIL.                                                OBNC24.2
089000     MOVE "PARA-TEST-5" TO PAR-NAME.                              OBNC24.2
089100     PERFORM PRINT-DETAIL.                                        OBNC24.2
089200 PARA-5-EXIT.                                                     OBNC24.2
089300     EXIT.                                                        OBNC24.2
089400 PARA-TEST-6.                                                     OBNC24.2
089500     ALTER PARA-6B IN QUAL-SECTION-2 TO PROCEED TO PARA-6C OF     OBNC24.2
089600     QUAL-SECTION-1.                                              OBNC24.2
089700 PARA-6A.                                                         OBNC24.2
089800     GO TO PARA-6B OF QUAL-SECTION-2.                             OBNC24.2
089900 PARA-6B.                                                         OBNC24.2
090000     PERFORM FAIL                                                 OBNC24.2
090100     GO TO PARA-6-EXIT.                                           OBNC24.2
090200 PARA-6C.                                                         OBNC24.2
090300     PERFORM PASS.                                                OBNC24.2
090400     GO TO   PARA-6-EXIT.                                         OBNC24.2
090500 PARA-6D.                                                         OBNC24.2
090600     PERFORM FAIL.                                                OBNC24.2
090700 PARA-6-EXIT.                                                     OBNC24.2
090800     EXIT.                                                        OBNC24.2
090900 PARA-6-LAST.                                                     OBNC24.2
091000     GO TO   CCVS-EXIT.                                           OBNC24.2
091100 PARA-TEST-FINISH.                                                OBNC24.2
091200     MOVE   "PARA-TEST-6" TO PAR-NAME.                            OBNC24.2
091300     PERFORM PRINT-DETAIL.                                        OBNC24.2
091400 QUAL-SECTION-2 SECTION.                                          OBNC24.2
091500 PARA-5C.                                                         OBNC24.2
091600     MOVE   "PASS" TO QT5.                                        OBNC24.2
091700     GO TO   PARA-5D.                                             OBNC24.2
091800 PARA-6B.                                                         OBNC24.2
091900     GO TO   PARA-6D OF QUAL-SECTION-2.                           OBNC24.2
092000 PARA-6C.                                                         OBNC24.2
092100     PERFORM FAIL.                                                OBNC24.2
092200     GO TO   PARA-6-EXIT.                                         OBNC24.2
092300 PARA-6D.                                                         OBNC24.2
092400     GO TO   PARA-6D IN QUAL-SECTION-1.                           OBNC24.2
092500 QUAL-EXIT.                                                       OBNC24.2
092600     EXIT.                                                        OBNC24.2
092700 CCVS-EXIT SECTION.                                               OBNC24.2
092800 CCVS-999999.                                                     OBNC24.2
092900     GO TO CLOSE-FILES.                                           OBNC24.2
      *END-OF,OBNC2M                                                          TES09870
