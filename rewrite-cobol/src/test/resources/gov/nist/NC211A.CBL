      *HEADER,COBOL,NC211A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2114.2
000200 PROGRAM-ID.                                                      NC2114.2
000300     NC211A.                                                      NC2114.2
000400****************************************************************  NC2114.2
000500*                                                              *  NC2114.2
000600*    VALIDATION FOR:-                                          *  NC2114.2
000700*                                                              *  NC2114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2114.2
000900*                                                              *  NC2114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2114.2
001100*                                                              *  NC2114.2
001200****************************************************************  NC2114.2
001300*                                                              *  NC2114.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2114.2
001500*                                                              *  NC2114.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2114.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2114.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2114.2
001900*                                                              *  NC2114.2
002000****************************************************************  NC2114.2
002100*                                                                 NC2114.2
002200*                                                              *  NC2114.2
002300*    PROGRAM NC211A TESTS THE GENERAL FORMAT OF THE "IF"       *  NC2114.2
002400*    STATEMENT USING COMPOUND CONDITIONAL STATEMENTS WITH      *  NC2114.2
002500*    ABREVIATED CONDITIONS, CONDITION NAMES AND QUALIFIED      *  NC2114.2
002600*    DATA-NAMES.                                               *  NC2114.2
002700*                                                              *  NC2114.2
002800****************************************************************  NC2114.2
002900 ENVIRONMENT DIVISION.                                            NC2114.2
003000 CONFIGURATION SECTION.                                           NC2114.2
003100 SOURCE-COMPUTER.                                                 NC2114.2
003200     XXXXX082.                                                    NC2114.2
003300 OBJECT-COMPUTER.                                                 NC2114.2
003400     XXXXX083.                                                    NC2114.2
003500ASPECIAL-NAMES.                                                   NC2114.2
003600A    XXXXX051                                                     NC2114.2
003700A    IS WRK-SWITCH-1                                              NC2114.2
003800A    ON STATUS IS ON-WRK-SWITCH-1                                 NC2114.2
003900A    OFF STATUS IS OFF-WRK-SWITCH-1                               NC2114.2
004000A    XXXXX052                                                     NC2114.2
004100A    IS WRK-SWITCH-2                                              NC2114.2
004200A    OFF STATUS IS OFF-WRK-SWITCH-2.                              NC2114.2
004300 INPUT-OUTPUT SECTION.                                            NC2114.2
004400 FILE-CONTROL.                                                    NC2114.2
004500     SELECT PRINT-FILE ASSIGN TO                                  NC2114.2
004600     XXXXX055.                                                    NC2114.2
004700 DATA DIVISION.                                                   NC2114.2
004800 FILE SECTION.                                                    NC2114.2
004900 FD  PRINT-FILE.                                                  NC2114.2
005000 01  PRINT-REC PICTURE X(120).                                    NC2114.2
005100 01  DUMMY-RECORD PICTURE X(120).                                 NC2114.2
005200 WORKING-STORAGE SECTION.                                         NC2114.2
005300 77  WRK-DS-02V00                 PICTURE S99.                    NC2114.2
005400     88 TEST-2NUC-COND-99         VALUE 99.                       NC2114.2
005500 77  WRK-DS-06V06                 PICTURE 9(6)V9(6).              NC2114.2
005600 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06                        NC2114.2
005700                                  PICTURE S9(12).                 NC2114.2
005800 77  A02TWOS-DS-02V00             PICTURE S99    VALUE 22.        NC2114.2
005900 77  WRK-DS-01V00                 PICTURE S9.                     NC2114.2
006000 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  NC2114.2
006100 77  A990-DS-0201P                PICTURE S99P   VALUE 990.       NC2114.2
006200 77  A02ONES-DS-02V00             PICTURE S99    VALUE 11.        NC2114.2
006300 77  A01ONE-DS-P0801              PICTURE SP(8)9 VALUE .000000001.NC2114.2
006400 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         NC2114.2
006500 77  WRK-XN-00001                 PICTURE X.                      NC2114.2
006600 77  WRK-XN-00005                 PICTURE X(5).                   NC2114.2
006700 77  MINUS-TWO          PICTURE S9 VALUE -2.                      NC2114.2
006800 77  MINUS-ONE          PICTURE S9 VALUE -1.                      NC2114.2
006900 77  MINUS-UNO          PICTURE S9 VALUE -1.                      NC2114.2
007000 77  NAUGHT             PICTURE S9 VALUE  0.                      NC2114.2
007100 77  NOTHING            PICTURE S9 VALUE ZERO.                    NC2114.2
007200 77  ONE                PICTURE S9 VALUE  1.                      NC2114.2
007300 77  UNO                PICTURE S9 VALUE +1.                      NC2114.2
007400 77  TWO  PICTURE 9 VALUE 2.                                      NC2114.2
007500 77  DOS                PICTURE S9 VALUE +2.                      NC2114.2
007600 77  THREE PICTURE 9 VALUE 3.                                     NC2114.2
007700 77  TRES               PICTURE S9 VALUE +3.                      NC2114.2
007800 77  FOUR               PICTURE S9 VALUE  4.                      NC2114.2
007900 77  QUATROS            PICTURE S9 VALUE +4.                      NC2114.2
008000 77  FIVE               PICTURE S9 VALUE  5.                      NC2114.2
008100 77  SIX                PICTURE S9 VALUE  6.                      NC2114.2
008200 77  SEVEN PICTURE 9 VALUE 7.                                     NC2114.2
008300 77  EIGHT              PICTURE S9 VALUE  8.                      NC2114.2
008400 77  NINE  PICTURE 9 VALUE 9.                                     NC2114.2
008500 77  TEN  PICTURE 99 VALUE 10.                                    NC2114.2
008600 77  ONE-THIRD PIC SV9(18) VALUE +.333333333333333333.            NC2114.2
008700 77  THREE-SEVENTHS PIC SV9(10) VALUE +.4285714286.               NC2114.2
008800 77  ALTERCOUNT PICTURE 999 VALUE ZERO.                           NC2114.2
008900 77  XRAY PICTURE IS X.                                           NC2114.2
009000 77  IF-D1 PICTURE S9(4)V9(2) VALUE 0.                            NC2114.2
009100 77  IF-D2 PICTURE S9(4)V9(2) VALUE ZERO.                         NC2114.2
009200 77  IF-D3 PICTURE X(10) VALUE "0000000000".                      NC2114.2
009300 77  IF-D4 PICTURE X(15) VALUE "               ".                 NC2114.2
009400 77  IF-D5 PICTURE X(10) VALUE ALL QUOTE.                         NC2114.2
009500 77  IF-D6 PICTURE A(10) VALUE "BABABABABA".                      NC2114.2
009600 77  IF-D7 PICTURE S9(6)V9(4) VALUE +123.45.                      NC2114.2
009700 77  IF-D8 PICTURE 9(6)V9(4) VALUE 12300.                         NC2114.2
009800 77  IF-D9 PICTURE X(3) VALUE "123".                              NC2114.2
009900 77  IF-D11 PICTURE X(6) VALUE "ABCDEF".                          NC2114.2
010000 77  IF-D13 PICTURE 9(6)V9(4) VALUE 12300.                        NC2114.2
010100 77  IF-D14 PICTURE S9(4)V9(2) VALUE +123.45.                     NC2114.2
010200 77  IF-D15 PICTURE S999PP VALUE 12300.                           NC2114.2
010300 77  IF-D16 PICTURE PP99 VALUE .0012.                             NC2114.2
010400 77  IF-D17 PICTURE SV9(4) VALUE .0012.                           NC2114.2
010500 77  IF-D18 PICTURE X(10) VALUE "BABABABABA".                     NC2114.2
010600 77  IF-D19 PICTURE X(10) VALUE "ABCDEF    ".                     NC2114.2
010700 77  IF-D23 PICTURE $9,9B9.90+.                                   NC2114.2
010800 77  IF-D24 PICTURE X(10) VALUE "l1,2 3.40+".                     NC2114.2
010900 77  IF-D25 PICTURE ABABX0A.                                      NC2114.2
011000 77  IF-D26 PICTURE X(8) VALUE "A C D0E".                         NC2114.2
011100 77  IF-D27 PICTURE IS 9(6)V9(4) VALUE IS 2137.45                 NC2114.2
011200     USAGE IS COMPUTATIONAL.                                      NC2114.2
011300 77  IF-D28 PICTURE IS 999999V9999 VALUE IS 2137.45.              NC2114.2
011400 77  IF-D31 PICTURE S9(6) VALUE -123.                             NC2114.2
011500 77  IF-D32 PICTURE S9(4)V99.                                     NC2114.2
011600     88  A; VALUE 1.                                              NC2114.2
011700     88  B VALUES ARE 2 THRU 4.                                   NC2114.2
011800     88  C VALUE IS ZERO.                                         NC2114.2
011900     88  D VALUE IS +12.34.                                       NC2114.2
012000     88  E  VALUE IS .01, .11, .21 .81.                           NC2114.2
012100     88  F  VALUE IS 100 THRU 128 1000 THRU 1280 -9 THRU -2.      NC2114.2
012200     88  G  VALUE IS 8765.43 1234 THRU 5678 5 -9999 THRU 10.      NC2114.2
012300 77  IF-D33 PICTURE X(4).                                         NC2114.2
012400     88  B   VALUE QUOTE.                                         NC2114.2
012500     88  C   VALUE SPACE.                                         NC2114.2
012600     88 D VALUE ALL "BAC".                                        NC2114.2
012700 77  IF-D34 PICTURE A(4).                                         NC2114.2
012800     88  B VALUE "A A ".                                          NC2114.2
012900 77  IF-D37 PICTURE 9(5) VALUE 12345.                             NC2114.2
013000 77  IF-D38 PICTURE X(9) VALUE "12345    ".                       NC2114.2
013100 77  CCON-1 PICTURE 99 VALUE 11.                                  NC2114.2
013200 77  CCON-2 PICTURE 99 VALUE 12.                                  NC2114.2
013300 77  CCON-3 PICTURE 99 VALUE 13.                                  NC2114.2
013400 77  CCON-4   PICTURE 99 VALUE 14.                                NC2114.2
013500 77  CLASS-1            PICTURE X(5).                             NC2114.2
013600 77  CLASS-2            PICTURE X(5).                             NC2114.2
013700 77  CLASS-3            PICTURE X(5).                             NC2114.2
013800 77  SIGN-1             PICTURE S9(5).                            NC2114.2
013900 77  SIGN-2             PICTURE S9(5).                            NC2114.2
014000 77  SIGN-3             PICTURE S9(5).                            NC2114.2
014100 77  AZE                PICTURE X(5) VALUE "AAAAA".               NC2114.2
014200 77  BEEZE              PICTURE X(5) VALUE "BBBBB".               NC2114.2
014300 77  CEEZE              PICTURE X(5) VALUE "CCCCC".               NC2114.2
014400 01  SUBSCRIPT-6 PICTURE 99999 VALUE ZERO.                        NC2114.2
014500 01  IF-TABLE.                                                    NC2114.2
014600     02 IF-ELEM PICTURE X OCCURS 12 TIMES.                        NC2114.2
014700 01  QUOTE-DATA.                                                  NC2114.2
014800     02 QU-1 PICTURE X(3) VALUE "123".                            NC2114.2
014900     02 QU-2 PICTURE X VALUE QUOTE.                               NC2114.2
015000     02 QU-3 PICTURE X(6) VALUE "ABC456".                         NC2114.2
015100 01  IF-D10.                                                      NC2114.2
015200     02  D1 PICTURE X(2) VALUE "01".                              NC2114.2
015300     02  D2 PICTURE X(2) VALUE "23".                              NC2114.2
015400     02  D3.                                                      NC2114.2
015500     03  D4 PICTURE X(4) VALUE "4567".                            NC2114.2
015600     03 D5 PICTURE X(4) VALUE "8912".                             NC2114.2
015700 01  IF-D12.                                                      NC2114.2
015800     02  D1 PICTURE X(3) VALUE "ABC".                             NC2114.2
015900     02  D2.                                                      NC2114.2
016000     03  D3.                                                      NC2114.2
016100     04  D4      PICTURE XX     VALUE "DE".                       NC2114.2
016200     04  D5 PICTURE X VALUE "F".                                  NC2114.2
016300 01  IF-D20.                                                      NC2114.2
016400     02  FILLER    PICTURE 9(5)   VALUE ZERO.                     NC2114.2
016500     02  D1 PICTURE 9(2) VALUE 12.                                NC2114.2
016600     02  D2 PICTURE 9 VALUE 3.                                    NC2114.2
016700     02  D3 PICTURE 9(2) VALUE 45.                                NC2114.2
016800 01  IF-D21.                                                      NC2114.2
016900     02  D1 PICTURE 9(5) VALUE ZEROS.                             NC2114.2
017000     02  D2 PICTURE 9(5) VALUE 12345.                             NC2114.2
017100 01  IF-D22.                                                      NC2114.2
017200     02  D1 PICTURE A(2) VALUE "AB".                              NC2114.2
017300     02  D2 PICTURE A(4) VALUE "CDEF".                            NC2114.2
017400 01  IF-D35.                                                      NC2114.2
017500     02  AA PICTURE X(2).                                         NC2114.2
017600     88  A1 VALUE "AA".                                           NC2114.2
017700     88  A2 VALUE "AB".                                           NC2114.2
017800     02  BB PICTURE IS X(2).                                      NC2114.2
017900     88  B1 VALUE "CC".                                           NC2114.2
018000     88  B2 VALUE "CD".                                           NC2114.2
018100     02 BB-2 REDEFINES BB.                                        NC2114.2
018200     03  AAA PICTURE X.                                           NC2114.2
018300     88  AA1 VALUE "A".                                           NC2114.2
018400     88  AA2 VALUE "C".                                           NC2114.2
018500     03  BBB PICTURE X.                                           NC2114.2
018600     88  BB1    VALUE "B".                                        NC2114.2
018700     88  BB2 VALUE "D".                                           NC2114.2
018800 01  IF-D36  PICTURE X(120) VALUE IS    "ABCDEFGHIJKLMNOPQRSTUVWXYNC2114.2
018900-    "Z1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890ABCDEFGHIJKLMNC2114.2
019000-    "NOPQRSTUVWXYZ1234567890ABCDEFGHIJKL".                       NC2114.2
019100 01  IF-D40 PICTURE 9(5) VALUE 12345                              NC2114.2
019200              COMPUTATIONAL SYNCHRONIZED RIGHT.                   NC2114.2
019300     88 IF-D40A VALUE ZERO THRU 10000.                            NC2114.2
019400     88 IF-D40B VALUE 10001 THRU 99999.                           NC2114.2
019500     88 IF-D40C VALUE 99999.                                      NC2114.2
019600 01  PERFORM1  PICTURE XXX  VALUE SPACES.                         NC2114.2
019700 01  PERFORM2  PICTURE S999 VALUE 20.                             NC2114.2
019800 01  PERFORM3  PICTURE  9  VALUE  5.                              NC2114.2
019900 01  PERFORM4  PICTURE S99V9.                                     NC2114.2
020000 01  PERFORM5  PICTURE S99V9  VALUE 10.0.                         NC2114.2
020100 01  PERFORM6  PICTURE  99V9.                                     NC2114.2
020200 01  PERFORM7.                                                    NC2114.2
020300     02  PERFORM8  OCCURS 7 TIMES  PICTURE  99V9.                 NC2114.2
020400 01  PERFORM9  PICTURE 9   VALUE 3.                               NC2114.2
020500 01  PERFORM10  PICTURE  S9  VALUE -1.                            NC2114.2
020600 01  PERFORM11  PICTURE  99  VALUE 6.                             NC2114.2
020700 01  PERFORM12.                                                   NC2114.2
020800     02 PERFORM13  OCCURS 4 TIMES.                                NC2114.2
020900         03 PERFORM14  OCCURS 20 TIMES  PICTURE  99V9.            NC2114.2
021000         03 PERFORM15  OCCURS 10 TIMES.                           NC2114.2
021100             04 PERFORM16  OCCURS 5 TIMES  PICTURE 99V9.          NC2114.2
021200 01  PERFORM-KEY  PICTURE 9.                                      NC2114.2
021300 01  RECEIVING-TABLE.                                             NC2114.2
021400     03 TBL-ELEMEN-A.                                             NC2114.2
021500         05 TBL-ELEMEN-B          PICTURE X(18).                  NC2114.2
021600         05 TBL-ELEMEN-C          PICTURE X(18).                  NC2114.2
021700     03  TBL-ELEMEN-D.                                            NC2114.2
021800         05 TBL-ELEMEN-E          PICTURE X OCCURS 36 TIMES.      NC2114.2
021900 01  LITERAL-SPLITTER.                                            NC2114.2
022000     02 PART1                     PICTURE X(20).                  NC2114.2
022100     02 PART2                     PICTURE X(20).                  NC2114.2
022200     02 PART3                     PICTURE X(20).                  NC2114.2
022300     02 PART4                     PICTURE X(20).                  NC2114.2
022400 01  LITERAL-TABLE REDEFINES LITERAL-SPLITTER.                    NC2114.2
022500     02 80PARTS                   PICTURE X      OCCURS 80 TIMES. NC2114.2
022600 01  GRP-FOR-88-LEVELS.                                           NC2114.2
022700     03 WRK-DS-02V00-COND         PICTURE 99.                     NC2114.2
022800         88 COND-1                VALUE IS 01 THRU 05.            NC2114.2
022900         88 COND-2                VALUES ARE 06 THRU 10           NC2114.2
023000                                           16 THRU 20  00.        NC2114.2
023100         88 COND-3                VALUES 11 THRU 15.              NC2114.2
023200 01  GRP-MOVE-CONSTANTS.                                          NC2114.2
023300     03 GRP-GROUP-MOVE-FROM.                                      NC2114.2
023400         04 GRP-ALPHABETIC.                                       NC2114.2
023500             05 ALPHABET-AN-00026 PICTURE A(26)                   NC2114.2
023600                        VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ".       NC2114.2
023700         04 GRP-NUMERIC.                                          NC2114.2
023800             05 DIGITS-DV-10V00   PICTURE 9(10) VALUE 0123456789. NC2114.2
023900         05 DIGITS-DU-06V04-S REDEFINES DIGITS-DV-10V00           NC2114.2
024000                                  PICTURE 9(6)V9999.              NC2114.2
024100         04 GRP-ALPHANUMERIC.                                     NC2114.2
024200             05 ALPHANUMERIC-XN-00049 PICTURE X(50)               NC2114.2
024300     VALUE  "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-><=l,;.()/* 0123456789". NC2114.2
024400             05 FILLER                PICTURE X  VALUE QUOTE.     NC2114.2
024500 01  GRP-FOR-2N058.                                               NC2114.2
024600     02 SUB-GRP-FOR-2N058-A.                                      NC2114.2
024700         03 ELEM-FOR-2N058-A PICTURE 999  VALUE ZEROES.           NC2114.2
024800         03 ELEM-FOR-2N058-B PICTURE XXX  VALUE ZEROS.            NC2114.2
024900         03 ELEM-FOR-2N058-C PICTURE XXX  VALUE SPACES.           NC2114.2
025000         03 ELEM-FOR-2N058-D PICTURE X(6) VALUE ALL "ABC".        NC2114.2
025100         03 ELEM-FOR-2N058-E PICTURE XXX  VALUE ALL "Z".          NC2114.2
025200         03 ELEM-FOR-2N058-F PICTURE XXX  VALUE ALL SPACES.       NC2114.2
025300         03 ELEM-FOR-2N058-G PICTURE XXX  VALUE ALL ZEROES.       NC2114.2
025400         03 ELEM-FOR-2N058-H PICTURE 999  VALUE ALL ZEROS.        NC2114.2
025500         03 ELEM-FOR-2N058-I PICTURE XXX  VALUE QUOTES.           NC2114.2
025600         03 ELEM-FOR-2N058-J PICTURE XXX  VALUE ALL QUOTES.       NC2114.2
025700         03 ELEM-FOR-2N058-K PICTURE XXX  VALUE ALL HIGH-VALUES.  NC2114.2
025800         03 ELEM-FOR-2N058-L PICTURE XXX  VALUE ALL LOW-VALUES.   NC2114.2
025900         03 ELEM-FOR-2N058-M PICTURE XXX  VALUE HIGH-VALUES.      NC2114.2
026000         03 ELEM-FOR-2N058-N PICTURE XXX  VALUE LOW-VALUES.       NC2114.2
026100     02 SUB-GRP-FOR-2N058-B.                                      NC2114.2
026200         03 SUB-SUB-BA.                                           NC2114.2
026300             04 ELEM-FOR-2N058-A  PICTURE 999.                    NC2114.2
026400             04 ELEM-FOR-2N058-B  PICTURE XXX.                    NC2114.2
026500             04 ELEM-FOR-2N058-C  PICTURE XXX.                    NC2114.2
026600             04 ELEM-FOR-2N058-D  PICTURE X(6).                   NC2114.2
026700         03 SUB-SUB-BB.                                           NC2114.2
026800             04 ELEM-FOR-2N058-E  PICTURE XXX.                    NC2114.2
026900             04 ELEM-FOR-2N058-F  PICTURE XXX.                    NC2114.2
027000             04 ELEM-FOR-2N058-G  PICTURE XXX.                    NC2114.2
027100             04 ELEM-FOR-2N058-H  PICTURE 999.                    NC2114.2
027200         03 SUB-SUB-BC.                                           NC2114.2
027300             04 ELEM-FOR-2N058-I  PICTURE XXX.                    NC2114.2
027400             04 ELEM-FOR-2N058-J  PICTURE XXX.                    NC2114.2
027500             04 ELEM-FOR-2N058-K  PICTURE XXX.                    NC2114.2
027600             04 ELEM-FOR-2N058-L  PICTURE XXX.                    NC2114.2
027700             04 ELEM-FOR-2N058-M  PICTURE XXX.                    NC2114.2
027800             04 ELEM-FOR-2N058-N  PICTURE XXX.                    NC2114.2
027900 01  CHARACTER-BREAKDOWN-S.                                       NC2114.2
028000     02   FIRST-20S PICTURE X(20).                                NC2114.2
028100     02  SECOND-20S PICTURE X(20).                                NC2114.2
028200     02   THIRD-20S PICTURE X(20).                                NC2114.2
028300     02  FOURTH-20S PICTURE X(20).                                NC2114.2
028400     02   FIFTH-20S PICTURE X(20).                                NC2114.2
028500     02   SIXTH-20S PICTURE X(20).                                NC2114.2
028600     02 SEVENTH-20S PICTURE X(20).                                NC2114.2
028700     02  EIGHTH-20S PICTURE X(20).                                NC2114.2
028800     02   NINTH-20S PICTURE X(20).                                NC2114.2
028900     02   TENTH-20S PICTURE X(20).                                NC2114.2
029000 01  CHARACTER-BREAKDOWN-R.                                       NC2114.2
029100     02   FIRST-20R PICTURE X(20).                                NC2114.2
029200     02  SECOND-20R PICTURE X(20).                                NC2114.2
029300     02   THIRD-20R PICTURE X(20).                                NC2114.2
029400     02  FOURTH-20R PICTURE X(20).                                NC2114.2
029500     02   FIFTH-20R PICTURE X(20).                                NC2114.2
029600     02   SIXTH-20R PICTURE X(20).                                NC2114.2
029700     02 SEVENTH-20R PICTURE X(20).                                NC2114.2
029800     02  EIGHTH-20R PICTURE X(20).                                NC2114.2
029900     02   NINTH-20R PICTURE X(20).                                NC2114.2
030000     02   TENTH-20R PICTURE X(20).                                NC2114.2
030100 01  TABLE-80.                                                    NC2114.2
030200     02  ELMT OCCURS 3 TIMES PIC 9.                               NC2114.2
030300     88  A80  VALUES ARE ZERO THRU 7.                             NC2114.2
030400     88  B80  VALUE 8.                                            NC2114.2
030500     88  C80  VALUES ARE 7, 8 THROUGH 9.                          NC2114.2
030600                                                                  NC2114.2
030700 01  TABLE-86.                                                    NC2114.2
030800     88  A86  VALUE "ABC".                                        NC2114.2
030900     88  B86  VALUE "ABCABC".                                     NC2114.2
031000     88  C86  VALUE "   ABC".                                     NC2114.2
031100     02  DATANAME-86  PIC XXX  VALUE "ABC".                       NC2114.2
031200     02  DNAME-86.                                                NC2114.2
031300         03  FILLER  PIC X  VALUE "A".                            NC2114.2
031400         03  FILLER  PIC X  VALUE "B".                            NC2114.2
031500         03  FILLER  PIC X   VALUE "C".                           NC2114.2
031600B01  DNAME-SWITCH       PICTURE 9 VALUE 1.                        NC2114.2
031700B    88 ON-WRK-SWITCH-1     VALUE 1.                              NC2114.2
031800B    88 OFF-WRK-SWITCH-1    VALUE 0.                              NC2114.2
031900B01  DNAME-SWITCH2  PICTURE 9 VALUE 0.                            NC2114.2
032000B    88  ON-WRK-SWITCH-2  VALUE 1.                                NC2114.2
032100B    88  OFF-WRK-SWITCH-2  VALUE 0.                               NC2114.2
032200 01  FIGCON-DATA.                                                 NC2114.2
032300     02 SPACE-X         PICTURE X(10) VALUE "          ".         NC2114.2
032400     02 QUOTE-X         PICTURE X(5)  VALUE QUOTE.                NC2114.2
032500     02 LOW-VAL         PICTURE X(5)  VALUE LOW-VALUE.            NC2114.2
032600     02 ABC PICTURE XXX VALUE "ABC".                              NC2114.2
032700     02 ONE23           PICTURE 9999  VALUE 123.                  NC2114.2
032800     02 ZERO-C          PICTURE 9(10) VALUE 0 COMPUTATIONAL.      NC2114.2
032900     02 ZERO-D          PICTURE 9     VALUE ZERO USAGE DISPLAY.   NC2114.2
033000 01  XX-TALLY PIC S9(5) USAGE COMP.                               NC2114.2
033100 01  TEST-RESULTS.                                                NC2114.2
033200     02 FILLER                   PIC X      VALUE SPACE.          NC2114.2
033300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2114.2
033400     02 FILLER                   PIC X      VALUE SPACE.          NC2114.2
033500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2114.2
033600     02 FILLER                   PIC X      VALUE SPACE.          NC2114.2
033700     02  PAR-NAME.                                                NC2114.2
033800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2114.2
033900       03  PARDOT-X              PIC X      VALUE SPACE.          NC2114.2
034000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2114.2
034100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2114.2
034200     02 RE-MARK                  PIC X(61).                       NC2114.2
034300 01  TEST-COMPUTED.                                               NC2114.2
034400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2114.2
034500     02 FILLER                   PIC X(17)  VALUE                 NC2114.2
034600            "       COMPUTED=".                                   NC2114.2
034700     02 COMPUTED-X.                                               NC2114.2
034800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2114.2
034900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2114.2
035000                                 PIC -9(9).9(9).                  NC2114.2
035100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2114.2
035200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2114.2
035300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2114.2
035400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2114.2
035500         04 COMPUTED-18V0                    PIC -9(18).          NC2114.2
035600         04 FILLER                           PIC X.               NC2114.2
035700     03 FILLER PIC X(50) VALUE SPACE.                             NC2114.2
035800 01  TEST-CORRECT.                                                NC2114.2
035900     02 FILLER PIC X(30) VALUE SPACE.                             NC2114.2
036000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2114.2
036100     02 CORRECT-X.                                                NC2114.2
036200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2114.2
036300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2114.2
036400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2114.2
036500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2114.2
036600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2114.2
036700     03      CR-18V0 REDEFINES CORRECT-A.                         NC2114.2
036800         04 CORRECT-18V0                     PIC -9(18).          NC2114.2
036900         04 FILLER                           PIC X.               NC2114.2
037000     03 FILLER PIC X(2) VALUE SPACE.                              NC2114.2
037100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2114.2
037200 01  CCVS-C-1.                                                    NC2114.2
037300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2114.2
037400-    "SS  PARAGRAPH-NAME                                          NC2114.2
037500-    "       REMARKS".                                            NC2114.2
037600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2114.2
037700 01  CCVS-C-2.                                                    NC2114.2
037800     02 FILLER                     PIC X        VALUE SPACE.      NC2114.2
037900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2114.2
038000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2114.2
038100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2114.2
038200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2114.2
038300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2114.2
038400 01  REC-CT                        PIC 99       VALUE ZERO.       NC2114.2
038500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2114.2
038600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2114.2
038700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2114.2
038800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2114.2
038900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2114.2
039000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2114.2
039100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2114.2
039200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2114.2
039300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2114.2
039400 01  CCVS-H-1.                                                    NC2114.2
039500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2114.2
039600     02  FILLER                    PIC X(42)    VALUE             NC2114.2
039700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2114.2
039800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2114.2
039900 01  CCVS-H-2A.                                                   NC2114.2
040000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2114.2
040100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2114.2
040200   02  FILLER                        PIC XXXX   VALUE             NC2114.2
040300     "4.2 ".                                                      NC2114.2
040400   02  FILLER                        PIC X(28)  VALUE             NC2114.2
040500            " COPY - NOT FOR DISTRIBUTION".                       NC2114.2
040600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2114.2
040700                                                                  NC2114.2
040800 01  CCVS-H-2B.                                                   NC2114.2
040900   02  FILLER                        PIC X(15)  VALUE             NC2114.2
041000            "TEST RESULT OF ".                                    NC2114.2
041100   02  TEST-ID                       PIC X(9).                    NC2114.2
041200   02  FILLER                        PIC X(4)   VALUE             NC2114.2
041300            " IN ".                                               NC2114.2
041400   02  FILLER                        PIC X(12)  VALUE             NC2114.2
041500     " HIGH       ".                                              NC2114.2
041600   02  FILLER                        PIC X(22)  VALUE             NC2114.2
041700            " LEVEL VALIDATION FOR ".                             NC2114.2
041800   02  FILLER                        PIC X(58)  VALUE             NC2114.2
041900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2114.2
042000 01  CCVS-H-3.                                                    NC2114.2
042100     02  FILLER                      PIC X(34)  VALUE             NC2114.2
042200            " FOR OFFICIAL USE ONLY    ".                         NC2114.2
042300     02  FILLER                      PIC X(58)  VALUE             NC2114.2
042400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2114.2
042500     02  FILLER                      PIC X(28)  VALUE             NC2114.2
042600            "  COPYRIGHT   1985 ".                                NC2114.2
042700 01  CCVS-E-1.                                                    NC2114.2
042800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2114.2
042900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2114.2
043000     02 ID-AGAIN                     PIC X(9).                    NC2114.2
043100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2114.2
043200 01  CCVS-E-2.                                                    NC2114.2
043300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2114.2
043400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2114.2
043500     02 CCVS-E-2-2.                                               NC2114.2
043600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2114.2
043700         03 FILLER                   PIC X      VALUE SPACE.      NC2114.2
043800         03 ENDER-DESC               PIC X(44)  VALUE             NC2114.2
043900            "ERRORS ENCOUNTERED".                                 NC2114.2
044000 01  CCVS-E-3.                                                    NC2114.2
044100     02  FILLER                      PIC X(22)  VALUE             NC2114.2
044200            " FOR OFFICIAL USE ONLY".                             NC2114.2
044300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2114.2
044400     02  FILLER                      PIC X(58)  VALUE             NC2114.2
044500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2114.2
044600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2114.2
044700     02 FILLER                       PIC X(15)  VALUE             NC2114.2
044800             " COPYRIGHT 1985".                                   NC2114.2
044900 01  CCVS-E-4.                                                    NC2114.2
045000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2114.2
045100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2114.2
045200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2114.2
045300     02 FILLER                       PIC X(40)  VALUE             NC2114.2
045400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2114.2
045500 01  XXINFO.                                                      NC2114.2
045600     02 FILLER                       PIC X(19)  VALUE             NC2114.2
045700            "*** INFORMATION ***".                                NC2114.2
045800     02 INFO-TEXT.                                                NC2114.2
045900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2114.2
046000       04 XXCOMPUTED                 PIC X(20).                   NC2114.2
046100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2114.2
046200       04 XXCORRECT                  PIC X(20).                   NC2114.2
046300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2114.2
046400 01  HYPHEN-LINE.                                                 NC2114.2
046500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2114.2
046600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2114.2
046700-    "*****************************************".                 NC2114.2
046800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2114.2
046900-    "******************************".                            NC2114.2
047000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2114.2
047100            "NC211A".                                             NC2114.2
047200 PROCEDURE DIVISION.                                              NC2114.2
047300 CCVS1 SECTION.                                                   NC2114.2
047400 OPEN-FILES.                                                      NC2114.2
047500     OPEN     OUTPUT PRINT-FILE.                                  NC2114.2
047600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2114.2
047700     MOVE    SPACE TO TEST-RESULTS.                               NC2114.2
047800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2114.2
047900     GO TO CCVS1-EXIT.                                            NC2114.2
048000 CLOSE-FILES.                                                     NC2114.2
048100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2114.2
048200 TERMINATE-CCVS.                                                  NC2114.2
048300*    EXIT PROGRAM.                                                NC2114.2
048400*TERMINATE-CALL.                                                  NC2114.2
048500     STOP     RUN.                                                NC2114.2
048600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2114.2
048700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2114.2
048800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2114.2
048900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2114.2
049000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2114.2
049100 PRINT-DETAIL.                                                    NC2114.2
049200     IF REC-CT NOT EQUAL TO ZERO                                  NC2114.2
049300             MOVE "." TO PARDOT-X                                 NC2114.2
049400             MOVE REC-CT TO DOTVALUE.                             NC2114.2
049500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2114.2
049600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2114.2
049700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2114.2
049800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2114.2
049900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2114.2
050000     MOVE SPACE TO CORRECT-X.                                     NC2114.2
050100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2114.2
050200     MOVE     SPACE TO RE-MARK.                                   NC2114.2
050300 HEAD-ROUTINE.                                                    NC2114.2
050400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2114.2
050500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2114.2
050600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2114.2
050700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2114.2
050800 COLUMN-NAMES-ROUTINE.                                            NC2114.2
050900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2114.2
051000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2114.2
051100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2114.2
051200 END-ROUTINE.                                                     NC2114.2
051300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2114.2
051400 END-RTN-EXIT.                                                    NC2114.2
051500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2114.2
051600 END-ROUTINE-1.                                                   NC2114.2
051700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2114.2
051800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2114.2
051900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2114.2
052000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2114.2
052100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2114.2
052200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2114.2
052300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2114.2
052400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2114.2
052500  END-ROUTINE-12.                                                 NC2114.2
052600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2114.2
052700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2114.2
052800         MOVE "NO " TO ERROR-TOTAL                                NC2114.2
052900         ELSE                                                     NC2114.2
053000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2114.2
053100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2114.2
053200     PERFORM WRITE-LINE.                                          NC2114.2
053300 END-ROUTINE-13.                                                  NC2114.2
053400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2114.2
053500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2114.2
053600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2114.2
053700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2114.2
053800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2114.2
053900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2114.2
054000          MOVE "NO " TO ERROR-TOTAL                               NC2114.2
054100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2114.2
054200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2114.2
054300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2114.2
054400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2114.2
054500 WRITE-LINE.                                                      NC2114.2
054600     ADD 1 TO RECORD-COUNT.                                       NC2114.2
054700*    IF RECORD-COUNT GREATER 50                                   NC2114.2
054800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2114.2
054900*        MOVE SPACE TO DUMMY-RECORD                               NC2114.2
055000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2114.2
055100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2114.2
055200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2114.2
055300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2114.2
055400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2114.2
055500*        MOVE ZERO TO RECORD-COUNT.                               NC2114.2
055600     PERFORM WRT-LN.                                              NC2114.2
055700 WRT-LN.                                                          NC2114.2
055800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2114.2
055900     MOVE SPACE TO DUMMY-RECORD.                                  NC2114.2
056000 BLANK-LINE-PRINT.                                                NC2114.2
056100     PERFORM WRT-LN.                                              NC2114.2
056200 FAIL-ROUTINE.                                                    NC2114.2
056300     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2114.2
056400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2114.2
056500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2114.2
056600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2114.2
056700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2114.2
056800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2114.2
056900     GO TO  FAIL-ROUTINE-EX.                                      NC2114.2
057000 FAIL-ROUTINE-WRITE.                                              NC2114.2
057100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2114.2
057200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2114.2
057300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2114.2
057400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2114.2
057500 FAIL-ROUTINE-EX. EXIT.                                           NC2114.2
057600 BAIL-OUT.                                                        NC2114.2
057700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2114.2
057800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2114.2
057900 BAIL-OUT-WRITE.                                                  NC2114.2
058000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2114.2
058100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2114.2
058200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2114.2
058300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2114.2
058400 BAIL-OUT-EX. EXIT.                                               NC2114.2
058500 CCVS1-EXIT.                                                      NC2114.2
058600     EXIT.                                                        NC2114.2
058700 SECT-NC211A-001 SECTION.                                         NC2114.2
058800 NC-211A-001.                                                     NC2114.2
058900 CC--INIT-GF-1.                                                   NC2114.2
059000     MOVE "CC--TEST-GF-1 " TO PAR-NAME.                           NC2114.2
059100     MOVE "COMPOUND CONDITIONS" TO FEATURE.                       NC2114.2
059200     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
059300     PERFORM PRINT-DETAIL.                                        NC2114.2
059400     MOVE "  NOT ABBREVIATED   " TO FEATURE.                      NC2114.2
059500     MOVE 11 TO CCON-1.                                           NC2114.2
059600     MOVE 12 TO CCON-2.                                           NC2114.2
059700     MOVE 13 TO CCON-3.                                           NC2114.2
059800 CC--TEST-GF-1.                                                   NC2114.2
059900     IF CCON-1 IS LESS THAN CCON-2 AND CCON-3 IS GREATER THAN 10  NC2114.2
060000            PERFORM PASS                                          NC2114.2
060100            GO TO CC--WRITE-GF-1.                                 NC2114.2
060200     GO TO CC--FAIL-GF-1.                                         NC2114.2
060300 CC--DELETE-GF-1.                                                 NC2114.2
060400     PERFORM DE-LETE.                                             NC2114.2
060500     GO TO CC--WRITE-GF-1.                                        NC2114.2
060600 CC--FAIL-GF-1.                                                   NC2114.2
060700     PERFORM FAIL.                                                NC2114.2
060800 CC--WRITE-GF-1.                                                  NC2114.2
060900     PERFORM PRINT-DETAIL.                                        NC2114.2
061000*                                                                 NC2114.2
061100 CC--INIT-GF-2.                                                   NC2114.2
061200     MOVE "CC--TEST-GF-2 " TO PAR-NAME.                           NC2114.2
061300     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
061400     MOVE 11 TO CCON-1.                                           NC2114.2
061500     MOVE 12 TO CCON-2.                                           NC2114.2
061600     MOVE 13 TO CCON-3.                                           NC2114.2
061700 CC--TEST-GF-2.                                                   NC2114.2
061800     IF CCON-2 GREATER THAN CCON-1 AND 20 LESS THAN CCON-3        NC2114.2
061900            GO TO CC--FAIL-GF-2.                                  NC2114.2
062000     PERFORM PASS.                                                NC2114.2
062100     GO TO CC--WRITE-GF-2.                                        NC2114.2
062200 CC--DELETE-GF-2.                                                 NC2114.2
062300     PERFORM DE-LETE.                                             NC2114.2
062400     GO TO CC--WRITE-GF-2.                                        NC2114.2
062500 CC--FAIL-GF-2.                                                   NC2114.2
062600     PERFORM FAIL.                                                NC2114.2
062700 CC--WRITE-GF-2.                                                  NC2114.2
062800     PERFORM PRINT-DETAIL.                                        NC2114.2
062900*                                                                 NC2114.2
063000 CC--INIT-GF-3.                                                   NC2114.2
063100     MOVE "CC--TEST-GF-3 " TO PAR-NAME.                           NC2114.2
063200     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
063300     MOVE 11 TO CCON-1.                                           NC2114.2
063400     MOVE 12 TO CCON-2.                                           NC2114.2
063500     MOVE 13 TO CCON-3.                                           NC2114.2
063600 CC--TEST-GF-3.                                                   NC2114.2
063700     IF CCON-1 GREATER THAN CCON-2 AND 20 GREATER THAN CCON-3     NC2114.2
063800             GO TO CC--FAIL-GF-3.                                 NC2114.2
063900     PERFORM PASS.                                                NC2114.2
064000     GO TO CC--WRITE-GF-3.                                        NC2114.2
064100 CC--DELETE-GF-3.                                                 NC2114.2
064200     PERFORM DE-LETE.                                             NC2114.2
064300     GO TO CC--WRITE-GF-3.                                        NC2114.2
064400 CC--FAIL-GF-3.                                                   NC2114.2
064500     PERFORM FAIL.                                                NC2114.2
064600 CC--WRITE-GF-3.                                                  NC2114.2
064700     PERFORM PRINT-DETAIL.                                        NC2114.2
064800*                                                                 NC2114.2
064900 CC--INIT-GF-4.                                                   NC2114.2
065000     MOVE "CC--TEST-GF-4 " TO PAR-NAME.                           NC2114.2
065100     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
065200     MOVE 11 TO CCON-1.                                           NC2114.2
065300     MOVE 13 TO CCON-3.                                           NC2114.2
065400 CC--TEST-GF-4.                                                   NC2114.2
065500     IF CCON-1 GREATER THAN 10 OR 20 LESS THAN CCON-3             NC2114.2
065600           PERFORM PASS                                           NC2114.2
065700           GO TO CC--WRITE-GF-4.                                  NC2114.2
065800     GO TO CC--FAIL-GF-4.                                         NC2114.2
065900 CC--DELETE-GF-4.                                                 NC2114.2
066000     PERFORM DE-LETE.                                             NC2114.2
066100     GO TO CC--WRITE-GF-4.                                        NC2114.2
066200 CC--FAIL-GF-4.                                                   NC2114.2
066300     PERFORM FAIL.                                                NC2114.2
066400 CC--WRITE-GF-4.                                                  NC2114.2
066500     PERFORM PRINT-DETAIL.                                        NC2114.2
066600*                                                                 NC2114.2
066700 CC--INIT-GF-5.                                                   NC2114.2
066800     MOVE "CC--TEST-GF-5 " TO PAR-NAME.                           NC2114.2
066900     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
067000     MOVE 11 TO CCON-1.                                           NC2114.2
067100     MOVE 12 TO CCON-2.                                           NC2114.2
067200     MOVE 13 TO CCON-3.                                           NC2114.2
067300 CC--TEST-GF-5.                                                   NC2114.2
067400     IF CCON-3 LESS THAN CCON-2 OR 20 GREATER THAN CCON-1         NC2114.2
067500              PERFORM PASS                                        NC2114.2
067600              GO TO CC--WRITE-GF-5.                               NC2114.2
067700     GO TO CC--FAIL-GF-5.                                         NC2114.2
067800 CC--DELETE-GF-5.                                                 NC2114.2
067900     PERFORM DE-LETE.                                             NC2114.2
068000     GO TO CC--WRITE-GF-5.                                        NC2114.2
068100 CC--FAIL-GF-5.                                                   NC2114.2
068200     PERFORM FAIL.                                                NC2114.2
068300 CC--WRITE-GF-5.                                                  NC2114.2
068400     PERFORM PRINT-DETAIL.                                        NC2114.2
068500*                                                                 NC2114.2
068600 CC--INIT-GF-6.                                                   NC2114.2
068700     MOVE "CC--TEST-GF-6 " TO PAR-NAME.                           NC2114.2
068800     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
068900     MOVE 11 TO CCON-1.                                           NC2114.2
069000     MOVE 12 TO CCON-2.                                           NC2114.2
069100     MOVE 13 TO CCON-3.                                           NC2114.2
069200 CC--TEST-GF-6.                                                   NC2114.2
069300     IF CCON-1 EQUAL TO 11 AND CCON-3 GREATER THAN 12 OR CCON-2   NC2114.2
069400     LESS THAN 20 AND CCON-1 GREATER THAN 12                      NC2114.2
069500              PERFORM PASS                                        NC2114.2
069600              GO TO CC--WRITE-GF-6.                               NC2114.2
069700     GO TO CC--FAIL-GF-6.                                         NC2114.2
069800 CC--DELETE-GF-6.                                                 NC2114.2
069900     PERFORM DE-LETE.                                             NC2114.2
070000     GO TO CC--WRITE-GF-6.                                        NC2114.2
070100 CC--FAIL-GF-6.                                                   NC2114.2
070200     PERFORM FAIL.                                                NC2114.2
070300 CC--WRITE-GF-6.                                                  NC2114.2
070400     PERFORM PRINT-DETAIL.                                        NC2114.2
070500*                                                                 NC2114.2
070600 CC--INIT-GF-7.                                                   NC2114.2
070700     MOVE "CC--TEST-GF-7 " TO PAR-NAME.                           NC2114.2
070800     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
070900     MOVE 11 TO CCON-1.                                           NC2114.2
071000     MOVE 12 TO CCON-2.                                           NC2114.2
071100     MOVE 13 TO CCON-3.                                           NC2114.2
071200 CC--TEST-GF-7.                                                   NC2114.2
071300     IF CCON-1 LESS THAN 9 AND CCON-3 GREATER THAN 12 OR CCON-2   NC2114.2
071400     GREATER THAN 10 AND CCON-1 GREATER THAN 8                    NC2114.2
071500           PERFORM PASS                                           NC2114.2
071600           GO TO CC--WRITE-GF-7.                                  NC2114.2
071700     GO TO CC--FAIL-GF-7.                                         NC2114.2
071800 CC--DELETE-GF-7.                                                 NC2114.2
071900     PERFORM DE-LETE.                                             NC2114.2
072000     GO TO CC--WRITE-GF-7.                                        NC2114.2
072100 CC--FAIL-GF-7.                                                   NC2114.2
072200     PERFORM FAIL.                                                NC2114.2
072300 CC--WRITE-GF-7.                                                  NC2114.2
072400     PERFORM PRINT-DETAIL.                                        NC2114.2
072500*                                                                 NC2114.2
072600 CC--INIT-GF-8.                                                   NC2114.2
072700     MOVE "CC--TEST-GF-8 " TO PAR-NAME.                           NC2114.2
072800     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
072900     MOVE 11 TO CCON-1.                                           NC2114.2
073000     MOVE 12 TO CCON-2.                                           NC2114.2
073100 CC--TEST-GF-8.                                                   NC2114.2
073200     IF CCON-1 NOT EQUAL TO 11 OR CCON-2 NOT LESS THAN 10         NC2114.2
073300     PERFORM PASS ELSE PERFORM FAIL.                              NC2114.2
073400     GO TO CC--WRITE-GF-8.                                        NC2114.2
073500 CC--DELETE-GF-8.                                                 NC2114.2
073600     PERFORM DE-LETE.                                             NC2114.2
073700     GO TO CC--WRITE-GF-8.                                        NC2114.2
073800 CC--FAIL-GF-8.                                                   NC2114.2
073900     PERFORM FAIL.                                                NC2114.2
074000 CC--WRITE-GF-8.                                                  NC2114.2
074100     PERFORM PRINT-DETAIL.                                        NC2114.2
074200*                                                                 NC2114.2
074300 CC--INIT-GF-9.                                                   NC2114.2
074400     MOVE "CC--TEST-GF-9 " TO PAR-NAME.                           NC2114.2
074500     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
074600     MOVE 11 TO CCON-1.                                           NC2114.2
074700     MOVE 12 TO CCON-2.                                           NC2114.2
074800     MOVE 13 TO CCON-3.                                           NC2114.2
074900 CC--TEST-GF-9.                                                   NC2114.2
075000     IF CCON-2 NOT EQUAL TO CCON-3 AND CCON-1 NOT GREATER THAN 12 NC2114.2
075100              PERFORM PASS                                        NC2114.2
075200              GO TO CC--WRITE-GF-9.                               NC2114.2
075300     GO TO CC--FAIL-GF-9.                                         NC2114.2
075400 CC--DELETE-GF-9.                                                 NC2114.2
075500     PERFORM DE-LETE.                                             NC2114.2
075600     GO TO CC--WRITE-GF-9.                                        NC2114.2
075700 CC--FAIL-GF-9.                                                   NC2114.2
075800     PERFORM FAIL.                                                NC2114.2
075900 CC--WRITE-GF-9.                                                  NC2114.2
076000     PERFORM PRINT-DETAIL.                                        NC2114.2
076100*                                                                 NC2114.2
076200 CC--INIT-GF-10.                                                  NC2114.2
076300     MOVE "CC--TEST-GF-10" TO PAR-NAME.                           NC2114.2
076400     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
076500     MOVE 12 TO CCON-2.                                           NC2114.2
076600     MOVE 13 TO CCON-3.                                           NC2114.2
076700 CC--TEST-GF-10.                                                  NC2114.2
076800     IF CCON-3 NOT EQUAL TO 13 OR CCON-2 NOT LESS THAN 13         NC2114.2
076900            GO TO CC--FAIL-GF-10.                                 NC2114.2
077000     PERFORM PASS.                                                NC2114.2
077100     GO TO CC--WRITE-GF-10.                                       NC2114.2
077200 CC--DELETE-GF-10.                                                NC2114.2
077300     PERFORM DE-LETE.                                             NC2114.2
077400     GO TO CC--WRITE-GF-10.                                       NC2114.2
077500 CC--FAIL-GF-10.                                                  NC2114.2
077600     PERFORM FAIL.                                                NC2114.2
077700 CC--WRITE-GF-10.                                                 NC2114.2
077800     PERFORM PRINT-DETAIL.                                        NC2114.2
077900*                                                                 NC2114.2
078000 CC--INIT-GF-11.                                                  NC2114.2
078100     MOVE "CC--TEST-GF-11" TO PAR-NAME.                           NC2114.2
078200     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
078300     MOVE "  ABBREVIATED       " TO FEATURE.                      NC2114.2
078400     MOVE 13 TO CCON-3.                                           NC2114.2
078500 CC--TEST-GF-11.                                                  NC2114.2
078600     IF CCON-3 NOT EQUAL TO 12 AND GREATER THAN 10                NC2114.2
078700             PERFORM PASS                                         NC2114.2
078800             GO TO CC--WRITE-GF-11.                               NC2114.2
078900     GO TO CC--FAIL-GF-11.                                        NC2114.2
079000 CC--DELETE-GF-11.                                                NC2114.2
079100     PERFORM DE-LETE.                                             NC2114.2
079200     GO TO CC--WRITE-GF-11.                                       NC2114.2
079300 CC--FAIL-GF-11.                                                  NC2114.2
079400     PERFORM FAIL.                                                NC2114.2
079500 CC--WRITE-GF-11.                                                 NC2114.2
079600     PERFORM PRINT-DETAIL.                                        NC2114.2
079700*                                                                 NC2114.2
079800 CC--INIT-GF-12.                                                  NC2114.2
079900     MOVE "CC--TEST-GF-12" TO PAR-NAME.                           NC2114.2
080000     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
080100     MOVE 12 TO CCON-2.                                           NC2114.2
080200 CC--TEST-GF-12.                                                  NC2114.2
080300     IF CCON-2 LESS THAN 10 OR EQUAL TO 12 PERFORM PASS           NC2114.2
080400               GO TO CC--WRITE-GF-12.                             NC2114.2
080500     GO TO CC--FAIL-GF-12.                                        NC2114.2
080600 CC--DELETE-GF-12.                                                NC2114.2
080700     PERFORM DE-LETE.                                             NC2114.2
080800     GO TO CC--WRITE-GF-12.                                       NC2114.2
080900 CC--FAIL-GF-12.                                                  NC2114.2
081000     PERFORM FAIL.                                                NC2114.2
081100 CC--WRITE-GF-12.                                                 NC2114.2
081200     PERFORM PRINT-DETAIL.                                        NC2114.2
081300*                                                                 NC2114.2
081400 CC--INIT-GF-13.                                                  NC2114.2
081500     MOVE "CC--TEST-GF-13" TO PAR-NAME.                           NC2114.2
081600     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
081700     MOVE 11 TO CCON-1.                                           NC2114.2
081800     MOVE 12 TO CCON-2.                                           NC2114.2
081900 CC--TEST-GF-13.                                                  NC2114.2
082000     IF CCON-1 EQUAL TO CCON-2 OR 10 OR 11 PERFORM PASS           NC2114.2
082100             GO TO CC--WRITE-GF-13.                               NC2114.2
082200     GO TO CC--FAIL-GF-13.                                        NC2114.2
082300 CC--DELETE-GF-13.                                                NC2114.2
082400     PERFORM DE-LETE.                                             NC2114.2
082500     GO TO CC--WRITE-GF-13.                                       NC2114.2
082600 CC--FAIL-GF-13.                                                  NC2114.2
082700     PERFORM FAIL.                                                NC2114.2
082800 CC--WRITE-GF-13.                                                 NC2114.2
082900     PERFORM PRINT-DETAIL.                                        NC2114.2
083000*                                                                 NC2114.2
083100 CC--INIT-GF-14.                                                  NC2114.2
083200     MOVE "CC--TEST-GF-14" TO PAR-NAME.                           NC2114.2
083300     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
083400     MOVE 11 TO CCON-1.                                           NC2114.2
083500     MOVE 12 TO CCON-2.                                           NC2114.2
083600     MOVE 13 TO CCON-3.                                           NC2114.2
083700 CC--TEST-GF-14.                                                  NC2114.2
083800     IF CCON-2 GREATER THAN CCON-3 OR EQUAL TO CCON-1 OR 8 OR     NC2114.2
083900              CCON-3 - 1; PERFORM PASS                            NC2114.2
084000              GO TO CC--WRITE-GF-14.                              NC2114.2
084100     GO TO CC--FAIL-GF-14.                                        NC2114.2
084200 CC--DELETE-GF-14.                                                NC2114.2
084300     PERFORM DE-LETE.                                             NC2114.2
084400     GO TO CC--WRITE-GF-14.                                       NC2114.2
084500 CC--FAIL-GF-14.                                                  NC2114.2
084600     PERFORM FAIL.                                                NC2114.2
084700 CC--WRITE-GF-14.                                                 NC2114.2
084800     PERFORM PRINT-DETAIL.                                        NC2114.2
084900*                                                                 NC2114.2
085000 CC--INIT-GF-15.                                                  NC2114.2
085100     MOVE "CC--TEST-GF-15" TO PAR-NAME.                           NC2114.2
085200     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
085300     MOVE "ABCDEF" TO IF-D11.                                     NC2114.2
085400     MOVE "ABC"    TO D1 OF IF-D12.                               NC2114.2
085500     MOVE "DE"     TO D4 OF IF-D12.                               NC2114.2
085600     MOVE "F"      TO D5 OF IF-D12.                               NC2114.2
085700     MOVE "AB"     TO D1 OF IF-D22.                               NC2114.2
085800     MOVE "CDEF"   TO D2 OF IF-D22.                               NC2114.2
085900 CC--TEST-GF-15.                                                  NC2114.2
086000     IF IF-D11 EQUAL TO IF-D12 OR IF-D22 AND "ABCDEF"             NC2114.2
086100             PERFORM PASS                                         NC2114.2
086200             GO TO CC--WRITE-GF-15.                               NC2114.2
086300     GO TO CC--FAIL-GF-15.                                        NC2114.2
086400 CC--DELETE-GF-15.                                                NC2114.2
086500     PERFORM DE-LETE.                                             NC2114.2
086600     GO TO CC--WRITE-GF-15.                                       NC2114.2
086700 CC--FAIL-GF-15.                                                  NC2114.2
086800     PERFORM FAIL.                                                NC2114.2
086900 CC--WRITE-GF-15.                                                 NC2114.2
087000     PERFORM PRINT-DETAIL.                                        NC2114.2
087100*                                                                 NC2114.2
087200 CC--INIT-GF-16.                                                  NC2114.2
087300     MOVE "CC--TEST-GF-16" TO PAR-NAME.                           NC2114.2
087400     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
087500     MOVE "ABCDEF" TO IF-D11.                                     NC2114.2
087600     MOVE "ABC"    TO D1 OF IF-D12.                               NC2114.2
087700     MOVE "DE"     TO D4 OF IF-D12.                               NC2114.2
087800     MOVE "F"      TO D5 OF IF-D12.                               NC2114.2
087900     MOVE "AB"     TO D1 OF IF-D22.                               NC2114.2
088000     MOVE "CDEF"   TO D2 OF IF-D22.                               NC2114.2
088100 CC--TEST-GF-16.                                                  NC2114.2
088200     IF IF-D11 NOT EQUAL TO IF-D12 AND IF-D22 OR "ABCDEF"         NC2114.2
088300              PERFORM FAIL                                        NC2114.2
088400              GO TO CC--WRITE-GF-16.                              NC2114.2
088500     GO TO CC--PASS-GF-16.                                        NC2114.2
088600 CC--DELETE-GF-16.                                                NC2114.2
088700     PERFORM DE-LETE.                                             NC2114.2
088800     GO TO CC--WRITE-GF-16.                                       NC2114.2
088900 CC--PASS-GF-16.                                                  NC2114.2
089000     PERFORM PASS.                                                NC2114.2
089100 CC--WRITE-GF-16.                                                 NC2114.2
089200     PERFORM PRINT-DETAIL.                                        NC2114.2
089300*                                                                 NC2114.2
089400 CC--INIT-GF-17.                                                  NC2114.2
089500     MOVE "CC--TEST-GF-17" TO PAR-NAME.                           NC2114.2
089600     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
089700     MOVE  +123.45 TO IF-D7.                                      NC2114.2
089800     MOVE 12300    TO IF-D13.                                     NC2114.2
089900     MOVE  2137.45 TO IF-D27.                                     NC2114.2
090000     MOVE  2137.45 TO IF-D28.                                     NC2114.2
090100 CC--TEST-GF-17.                                                  NC2114.2
090200     IF IF-D27 GREATER THAN IF-D13 OR (IF-D27 IS EQUAL TO IF-D28  NC2114.2
090300     AND IF-D27 NOT LESS THAN IF-D7) PERFORM PASS                 NC2114.2
090400                GO TO CC--WRITE-GF-17.                            NC2114.2
090500     GO TO CC--FAIL-GF-17.                                        NC2114.2
090600 CC--DELETE-GF-17.                                                NC2114.2
090700     PERFORM DE-LETE.                                             NC2114.2
090800     GO TO CC--WRITE-GF-17.                                       NC2114.2
090900 CC--FAIL-GF-17.                                                  NC2114.2
091000     PERFORM FAIL.                                                NC2114.2
091100 CC--WRITE-GF-17.                                                 NC2114.2
091200     PERFORM PRINT-DETAIL.                                        NC2114.2
091300*                                                                 NC2114.2
091400 CC--INIT-GF-18.                                                  NC2114.2
091500     MOVE "CC--TEST-GF-18" TO PAR-NAME.                           NC2114.2
091600     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
091700     MOVE 11 TO CCON-1.                                           NC2114.2
091800     MOVE 12 TO CCON-2.                                           NC2114.2
091900     MOVE 13 TO CCON-3.                                           NC2114.2
092000 CC--TEST-GF-18.                                                  NC2114.2
092100     IF CCON-2 GREATER THAN CCON-1 AND NOT GREATER THAN CCON-3 OR NC2114.2
092200         CCON-1      PERFORM PASS                                 NC2114.2
092300                     GO TO CC--WRITE-GF-18.                       NC2114.2
092400     GO TO CC--FAIL-GF-18.                                        NC2114.2
092500*    NOTE THE STANDARD SAYS THAT THE ABOVE IS EQUIVALENT TO ---   NC2114.2
092600*        IF CCON-2 GREATER THAN CCON-1 AND CCON-2 NOT GREATER THANNC2114.2
092700*        CCON-3 OR CCON-2 NOT GREATER THAN CCON-1 PERFORM PASS    NC2114.2
092800*        ELSE PERFORM FAIL.                                       NC2114.2
092900 CC--DELETE-GF-18.                                                NC2114.2
093000     PERFORM DE-LETE.                                             NC2114.2
093100     GO TO CC--WRITE-GF-18.                                       NC2114.2
093200 CC--FAIL-GF-18.                                                  NC2114.2
093300     PERFORM FAIL.                                                NC2114.2
093400 CC--WRITE-GF-18.                                                 NC2114.2
093500     PERFORM PRINT-DETAIL.                                        NC2114.2
093600*                                                                 NC2114.2
093700 CC--INIT-GF-19.                                                  NC2114.2
093800     MOVE "CC--TEST-GF-19" TO PAR-NAME.                           NC2114.2
093900     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
094000     MOVE "  ABBREV. W/PARENS  " TO FEATURE.                      NC2114.2
094100     MOVE 11 TO CCON-1.                                           NC2114.2
094200     MOVE 12 TO CCON-2.                                           NC2114.2
094300     MOVE 13 TO CCON-3.                                           NC2114.2
094400 CC--TEST-GF-19.                                                  NC2114.2
094500     IF CCON-1 NOT LESS THAN 9 AND (CCON-3 GREATER THAN 12 OR     NC2114.2
094600     CCON-2 GREATER THAN 10) AND CCON-1 GREATER THAN 8            NC2114.2
094700             PERFORM PASS                                         NC2114.2
094800             GO TO CC--WRITE-GF-19.                               NC2114.2
094900     GO TO CC--FAIL-GF-19.                                        NC2114.2
095000 CC--DELETE-GF-19.                                                NC2114.2
095100     PERFORM DE-LETE.                                             NC2114.2
095200     GO TO CC--WRITE-GF-19.                                       NC2114.2
095300 CC--FAIL-GF-19.                                                  NC2114.2
095400     PERFORM FAIL.                                                NC2114.2
095500 CC--WRITE-GF-19.                                                 NC2114.2
095600     PERFORM PRINT-DETAIL.                                        NC2114.2
095700*                                                                 NC2114.2
095800 CC--INIT-GF-20.                                                  NC2114.2
095900     MOVE "CC--TEST-GF-20" TO PAR-NAME.                           NC2114.2
096000     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
096100     MOVE "  ABBREV. W/PARENS  " TO FEATURE.                      NC2114.2
096200     MOVE     0             TO IF-D1.                             NC2114.2
096300     MOVE  ZERO             TO IF-D2.                             NC2114.2
096400     MOVE "               " TO IF-D4.                             NC2114.2
096500     MOVE      .0012        TO IF-D16.                            NC2114.2
096600     MOVE      .0012        TO IF-D17.                            NC2114.2
096700     MOVE    12             TO D1 OF IF-D20.                      NC2114.2
096800     MOVE     3             TO D2 OF IF-D20.                      NC2114.2
096900     MOVE    45             TO D3 OF IF-D20.                      NC2114.2
097000     MOVE ZEROS             TO D1 OF IF-D21.                      NC2114.2
097100     MOVE 12345             TO D2 OF IF-D21.                      NC2114.2
097200 CC--TEST-GF-20.                                                  NC2114.2
097300     IF IF-D4 EQUAL TO ZEROS OR (IF-D1 NOT LESS THAN              NC2114.2
097400     IF-D2 AND (IF-D16 GREATER THAN IF-D17 OR IF-D20 EQUAL TO     NC2114.2
097500     IF-D21)) PERFORM PASS                                        NC2114.2
097600              GO TO CC--WRITE-GF-20.                              NC2114.2
097700     GO TO CC--FAIL-GF-20.                                        NC2114.2
097800 CC--DELETE-GF-20.                                                NC2114.2
097900     PERFORM DE-LETE.                                             NC2114.2
098000     GO TO CC--WRITE-GF-20.                                       NC2114.2
098100 CC--FAIL-GF-20.                                                  NC2114.2
098200     PERFORM FAIL.                                                NC2114.2
098300 CC--WRITE-GF-20.                                                 NC2114.2
098400     PERFORM PRINT-DETAIL.                                        NC2114.2
098500*                                                                 NC2114.2
098600 CC--INIT-GF-21.                                                  NC2114.2
098700     MOVE "CC--TEST-GF-21" TO PAR-NAME.                           NC2114.2
098800     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
098900     MOVE "  NESTED IF         " TO FEATURE.                      NC2114.2
099000     MOVE      .0012 TO IF-D16.                                   NC2114.2
099100     MOVE      .0012 TO IF-D17.                                   NC2114.2
099200     MOVE    12      TO D1 OF IF-D20.                             NC2114.2
099300     MOVE     3      TO D2 OF IF-D20.                             NC2114.2
099400     MOVE    45      TO D3 OF IF-D20.                             NC2114.2
099500     MOVE ZEROS      TO D1 OF IF-D21.                             NC2114.2
099600     MOVE 12345      TO D2 OF IF-D21.                             NC2114.2
099700 CC--TEST-GF-21.                                                  NC2114.2
099800     IF IF-D20 NOT LESS THAN IF-D21                               NC2114.2
099900         IF IF-D16 EQUAL TO IF-D17                                NC2114.2
100000             PERFORM PASS                                         NC2114.2
100100             GO TO CC--WRITE-GF-21                                NC2114.2
100200         ELSE                                                     NC2114.2
100300             PERFORM CC--FAIL-GF-21                               NC2114.2
100400     ELSE                                                         NC2114.2
100500         NEXT SENTENCE.                                           NC2114.2
100600     GO TO CC--FAIL-GF-21.                                        NC2114.2
100700 CC--DELETE-GF-21.                                                NC2114.2
100800     PERFORM DE-LETE.                                             NC2114.2
100900     GO TO CC--WRITE-GF-21.                                       NC2114.2
101000 CC--FAIL-GF-21.                                                  NC2114.2
101100     PERFORM FAIL.                                                NC2114.2
101200 CC--WRITE-GF-21.                                                 NC2114.2
101300     PERFORM PRINT-DETAIL.                                        NC2114.2
101400*                                                                 NC2114.2
101500 CC--INIT-GF-22.                                                  NC2114.2
101600     MOVE "CC--TEST-GF-22" TO PAR-NAME.                           NC2114.2
101700     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
101800     MOVE "  NESTED IF         " TO FEATURE.                      NC2114.2
101900     MOVE    12      TO D1 OF IF-D20.                             NC2114.2
102000     MOVE     3      TO D2 OF IF-D20.                             NC2114.2
102100     MOVE    45      TO D3 OF IF-D20.                             NC2114.2
102200     MOVE ZEROS      TO D1 OF IF-D21.                             NC2114.2
102300     MOVE 12345      TO D2 OF IF-D21.                             NC2114.2
102400 CC--TEST-GF-22.                                                  NC2114.2
102500     IF IF-D20 NOT EQUAL TO IF-D21                                NC2114.2
102600         NEXT SENTENCE                                            NC2114.2
102700     ELSE                                                         NC2114.2
102800         IF IF-D20 NOT GREATER THAN IF-D21                        NC2114.2
102900             PERFORM PASS                                         NC2114.2
103000             GO TO CC--WRITE-GF-22                                NC2114.2
103100         ELSE                                                     NC2114.2
103200             GO TO CC--FAIL-GF-22.                                NC2114.2
103300*                                                                 NC2114.2
103400     GO TO CC--FAIL-GF-22.                                        NC2114.2
103500 CC--DELETE-GF-22.                                                NC2114.2
103600     PERFORM DE-LETE.                                             NC2114.2
103700     GO TO CC--WRITE-GF-22.                                       NC2114.2
103800 CC--FAIL-GF-22.                                                  NC2114.2
103900     PERFORM FAIL.                                                NC2114.2
104000 CC--WRITE-GF-22.                                                 NC2114.2
104100     PERFORM PRINT-DETAIL.                                        NC2114.2
104200*                                                                 NC2114.2
104300 CC--INIT-GF-23.                                                  NC2114.2
104400     MOVE     "CC--TEST-GF-23" TO PAR-NAME.                       NC2114.2
104500     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
104600     MOVE "  NESTED IF         " TO FEATURE.                      NC2114.2
104700     MOVE     "X" TO WRK-XN-00001.                                NC2114.2
104800     MOVE     ZERO TO WRK-DS-01V00.                               NC2114.2
104900     MOVE     1 TO XX-TALLY.                                      NC2114.2
105000       MOVE SPACE TO IF-TABLE.                                    NC2114.2
105100     PERFORM  CC--TEST-GF-23.                                     NC2114.2
105200 CC--TEST-GF-23.                                                  NC2114.2
105300     IF       WRK-XN-00001 IS EQUAL TO "X"                        NC2114.2
105400          MOVE "Z" TO WRK-XN-00001                                NC2114.2
105500          IF WRK-DS-01V00 IS EQUAL TO ZERO                        NC2114.2
105600                MOVE 1 TO WRK-DS-01V00                            NC2114.2
105700          ELSE                                                    NC2114.2
105800                MOVE 2 TO WRK-DS-01V00                            NC2114.2
105900     ELSE                                                         NC2114.2
106000          MOVE "W" TO WRK-XN-00001                                NC2114.2
106100          IF WRK-DS-01V00 IS GREATER THAN ZERO                    NC2114.2
106200                MOVE "1" TO IF-ELEM (7).                          NC2114.2
106300     MOVE     WRK-XN-00001 TO IF-ELEM (XX-TALLY).                 NC2114.2
106400     ADD      1 TO XX-TALLY.                                      NC2114.2
106500     MOVE     WRK-DS-01V00 TO IF-ELEM (XX-TALLY).                 NC2114.2
106600     ADD      1 TO XX-TALLY.                                      NC2114.2
106700     MOVE     SPACE TO IF-ELEM (XX-TALLY)                         NC2114.2
106800     ADD      1 TO XX-TALLY.                                      NC2114.2
106900 CC--TEST-GF-23-1.                                                NC2114.2
107000     IF       IF-TABLE EQUAL TO "Z1 W1 1     "                    NC2114.2
107100              PERFORM PASS GO TO CC--WRITE-GF-23.                 NC2114.2
107200     GO       TO CC--FAIL-GF-23.                                  NC2114.2
107300 CC--DELETE-GF-23.                                                NC2114.2
107400     PERFORM  DE-LETE.                                            NC2114.2
107500     GO       TO CC--WRITE-GF-23.                                 NC2114.2
107600 CC--FAIL-GF-23.                                                  NC2114.2
107700     MOVE     IF-TABLE TO COMPUTED-A.                             NC2114.2
107800     MOVE     "Z1 W1 1" TO CORRECT-A.                             NC2114.2
107900     PERFORM FAIL.                                                NC2114.2
108000 CC--WRITE-GF-23.                                                 NC2114.2
108100     PERFORM  PRINT-DETAIL.                                       NC2114.2
108200*                                                                 NC2114.2
108300 CC--INIT-GF-24.                                                  NC2114.2
108400     MOVE "CC--TEST-GF-24" TO PAR-NAME.                           NC2114.2
108500     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
108600     MOVE     "  NOT ABBREVIATED   " TO FEATURE.                  NC2114.2
108700     MOVE     SPACE TO WRK-XN-00001.                              NC2114.2
108800     MOVE ZERO TO WRK-DS-01V00.                                   NC2114.2
108900 CC--TEST-GF-24.                                                  NC2114.2
109000     IF   WRK-XN-00001 EQUAL TO SPACE                             NC2114.2
109100          OR                                                      NC2114.2
109200          WRK-DS-01V00 EQUAL TO ZERO                              NC2114.2
109300          PERFORM PASS                                            NC2114.2
109400     ELSE                                                         NC2114.2
109500          GO TO CC--FAIL-GF-24.                                   NC2114.2
109600*    NOTE     BOTH CONDITIONS ARE TRUE.                           NC2114.2
109700     GO TO CC--WRITE-GF-24.                                       NC2114.2
109800 CC--DELETE-GF-24.                                                NC2114.2
109900     PERFORM  DE-LETE.                                            NC2114.2
110000     GO TO CC--WRITE-GF-24.                                       NC2114.2
110100 CC--FAIL-GF-24.                                                  NC2114.2
110200     PERFORM FAIL.                                                NC2114.2
110300 CC--WRITE-GF-24.                                                 NC2114.2
110400     PERFORM PRINT-DETAIL.                                        NC2114.2
110500*                                                                 NC2114.2
110600 CC--INIT-GF-25.                                                  NC2114.2
110700     MOVE   "VI-89 6.15.3/4" TO ANSI-REFERENCE.                   NC2114.2
110800     MOVE     "CC--TEST-GF-25" TO PAR-NAME.                       NC2114.2
110900     MOVE     "0" TO WRK-XN-00001.                                NC2114.2
111000     MOVE     0 TO WRK-DS-01V00.                                  NC2114.2
111100 CC--TEST-GF-25.                                                  NC2114.2
111200     IF   WRK-XN-00001 EQUAL TO "0"                               NC2114.2
111300          AND                                                     NC2114.2
111400          WRK-DS-01V00 EQUAL TO 0                                 NC2114.2
111500          PERFORM PASS                                            NC2114.2
111600     ELSE                                                         NC2114.2
111700          GO TO CC--FAIL-GF-25.                                   NC2114.2
111800*    NOTE     BOTH CONDITIONS ARE TRUE.                           NC2114.2
111900     GO TO CC--WRITE-GF-25.                                       NC2114.2
112000 CC--DELETE-GF-25.                                                NC2114.2
112100     PERFORM  DE-LETE.                                            NC2114.2
112200     GO TO CC--WRITE-GF-25.                                       NC2114.2
112300 CC--FAIL-GF-25.                                                  NC2114.2
112400     PERFORM FAIL.                                                NC2114.2
112500 CC--WRITE-GF-25.                                                 NC2114.2
112600     PERFORM  PRINT-DETAIL.                                       NC2114.2
112700*                                                                 NC2114.2
112800 CC--INIT-GF-26.                                                  NC2114.2
112900     MOVE     "CC--TEST-GF-26" TO PAR-NAME.                       NC2114.2
113000     MOVE     "VI-89 6.15.3/4" TO ANSI-REFERENCE.                 NC2114.2
113100     MOVE     "  NOT ABBR, W/PARENS" TO FEATURE.                  NC2114.2
113200     MOVE     SPACE TO IF-TABLE.                                  NC2114.2
113300     MOVE     ZERO TO WRK-DS-01V00.                               NC2114.2
113400     MOVE     ZERO TO WRK-XN-00001.                               NC2114.2
113500     PERFORM  CC--TEST-GF-26.                                     NC2114.2
113600     MOVE     "X" TO IF-ELEM (5).                                 NC2114.2
113700 CC--TEST-GF-26.                                                  NC2114.2
113800     IF (WRK-DS-01V00 IS EQUAL TO 0                               NC2114.2
113900                    OR                                            NC2114.2
114000         WRK-XN-00001 EQUAL TO "0")                               NC2114.2
114100                    AND                                           NC2114.2
114200         SPACE IS EQUAL TO IF-TABLE                               NC2114.2
114300          MOVE "1" TO IF-ELEM (1)                                 NC2114.2
114400     ELSE                                                         NC2114.2
114500          MOVE "1" TO IF-ELEM (3).                                NC2114.2
114600*    NOTE     ALL CONDITIONS ARE TRUE THE FIRST TIME, THEN THE    NC2114.2
114700*             FIRST TWO ARE TRUE THE SECOND TIME.                 NC2114.2
114800 CC--TEST-GF-26-1.                                                NC2114.2
114900     IF       IF-TABLE EQUAL TO "1 1 X"                           NC2114.2
115000              PERFORM PASS GO TO CC--WRITE-GF-26.                 NC2114.2
115100     GO       TO CC--FAIL-GF-26.                                  NC2114.2
115200 CC--DELETE-GF-26.                                                NC2114.2
115300     PERFORM  DE-LETE.                                            NC2114.2
115400     GO       TO CC--WRITE-GF-26.                                 NC2114.2
115500 CC--FAIL-GF-26.                                                  NC2114.2
115600     MOVE     IF-TABLE TO COMPUTED-A.                             NC2114.2
115700     MOVE     "1 1 X" TO CORRECT-A.                               NC2114.2
115800     PERFORM FAIL.                                                NC2114.2
115900 CC--WRITE-GF-26.                                                 NC2114.2
116000     PERFORM  PRINT-DETAIL.                                       NC2114.2
116100*                                                                 NC2114.2
116200 CC--INIT-GF-27.                                                  NC2114.2
116300     MOVE     "CC--TEST-GF-27" TO PAR-NAME.                       NC2114.2
116400     MOVE     "VI-89 6.15.3/4" TO ANSI-REFERENCE.                 NC2114.2
116500     MOVE     "  NOT ABBR, W/PARENS" TO FEATURE.                  NC2114.2
116600     MOVE     SPACE TO IF-TABLE.                                  NC2114.2
116700     MOVE     ZERO TO WRK-DS-01V00.                               NC2114.2
116800     MOVE     ZERO TO WRK-XN-00001.                               NC2114.2
116900     PERFORM  CC--TEST-GF-27.                                     NC2114.2
117000     MOVE     "X" TO IF-ELEM (5).                                 NC2114.2
117100 CC--TEST-GF-27.                                                  NC2114.2
117200     IF   NOT (WRK-DS-01V00 IS EQUAL TO 0                         NC2114.2
117300                         AND                                      NC2114.2
117400               WRK-XN-00001 IS EQUAL TO "0")                      NC2114.2
117500                     OR                                           NC2114.2
117600          SPACE IS EQUAL TO IF-TABLE                              NC2114.2
117700          MOVE "1" TO IF-ELEM (1)                                 NC2114.2
117800     ELSE                                                         NC2114.2
117900          MOVE "1" TO IF-ELEM (3).                                NC2114.2
118000*    NOTE     THE FIRST PART IS ALWAYS FALSE, AND THE PORTION     NC2114.2
118100*             AFTER THE "OR" IS FIRST TRUE, THEN FALSE.           NC2114.2
118200 CC--TEST-GF-27-1.                                                NC2114.2
118300     IF   IF-TABLE EQUAL TO "1 1 X"                               NC2114.2
118400          PERFORM PASS GO TO CC--WRITE-GF-27.                     NC2114.2
118500     GO TO CC--FAIL-GF-27.                                        NC2114.2
118600 CC--DELETE-GF-27.                                                NC2114.2
118700     PERFORM  DE-LETE.                                            NC2114.2
118800     GO       TO CC--WRITE-GF-27.                                 NC2114.2
118900 CC--FAIL-GF-27.                                                  NC2114.2
119000     MOVE     IF-TABLE TO COMPUTED-A.                             NC2114.2
119100     MOVE     "1 1 X" TO CORRECT-A.                               NC2114.2
119200     PERFORM FAIL.                                                NC2114.2
119300 CC--WRITE-GF-27.                                                 NC2114.2
119400     PERFORM  PRINT-DETAIL.                                       NC2114.2
119500*                                                                 NC2114.2
119600 CC--INIT-GF-28.                                                  NC2114.2
119700     MOVE     "CC--TEST-GF-28" TO PAR-NAME.                       NC2114.2
119800     MOVE     "VI-89 6.15.3/4" TO ANSI-REFERENCE.                 NC2114.2
119900     MOVE     "  ABBREVIATED       " TO FEATURE.                  NC2114.2
120000     MOVE     ZERO TO IF-TABLE                                    NC2114.2
120100     MOVE     ZERO TO WRK-XN-00001.                               NC2114.2
120200     MOVE     1 TO XX-TALLY.                                      NC2114.2
120300     PERFORM  CC--TEST-GF-28.                                     NC2114.2
120400     MOVE     "X" TO IF-ELEM (5).                                 NC2114.2
120500 CC--TEST-GF-28.                                                  NC2114.2
120600     IF   WRK-XN-00001 = "0" OR = "1" OR = IF-TABLE               NC2114.2
120700          AND = IF-ELEM (5)                                       NC2114.2
120800          MOVE "1" TO IF-ELEM (XX-TALLY)                          NC2114.2
120900          ADD 1 TO XX-TALLY                                       NC2114.2
121000     ELSE                                                         NC2114.2
121100          MOVE "2" TO IF-ELEM (XX-TALLY)                          NC2114.2
121200          ADD 1 TO XX-TALLY.                                      NC2114.2
121300 CC--TEST-GF-28-1.                                                NC2114.2
121400     IF IF-TABLE EQUAL TO "1100X0000000"                          NC2114.2
121500          PERFORM PASS GO TO CC--WRITE-GF-28.                     NC2114.2
121600     GO TO CC--FAIL-GF-28.                                        NC2114.2
121700 CC--DELETE-GF-28.                                                NC2114.2
121800     PERFORM  DE-LETE.                                            NC2114.2
121900     GO       TO CC--WRITE-GF-28.                                 NC2114.2
122000 CC--FAIL-GF-28.                                                  NC2114.2
122100     MOVE     IF-TABLE TO COMPUTED-A.                             NC2114.2
122200     MOVE     "1100X0000000" TO CORRECT-A.                        NC2114.2
122300     PERFORM  FAIL.                                               NC2114.2
122400 CC--WRITE-GF-28.                                                 NC2114.2
122500     PERFORM  PRINT-DETAIL.                                       NC2114.2
122600*                                                                 NC2114.2
122700 CC--INIT-GF-29.                                                  NC2114.2
122800     MOVE     "CC--TEST-GF-29" TO PAR-NAME.                       NC2114.2
122900     MOVE     "VI-89 6.15.3/4" TO ANSI-REFERENCE.                 NC2114.2
123000     MOVE     ZERO TO IF-TABLE.                                   NC2114.2
123100     MOVE     ZERO TO WRK-XN-00001.                               NC2114.2
123200     MOVE     1 TO XX-TALLY.                                      NC2114.2
123300     PERFORM  CC--TEST-GF-29.                                     NC2114.2
123400     MOVE     "X" TO WRK-XN-00001.                                NC2114.2
123500 CC--TEST-GF-29.                                                  NC2114.2
123600     IF  WRK-XN-00001 = "0" OR "1" OR "2" OR IF-TABLE OR "3"      NC2114.2
123700         MOVE "1" TO IF-ELEM (XX-TALLY)                           NC2114.2
123800         ADD 1 TO XX-TALLY                                        NC2114.2
123900     ELSE                                                         NC2114.2
124000         MOVE "2" TO IF-ELEM (XX-TALLY)                           NC2114.2
124100         ADD 1 TO XX-TALLY.                                       NC2114.2
124200 CC--TEST-GF-29-1.                                                NC2114.2
124300     IF IF-TABLE EQUAL TO "120000000000"                          NC2114.2
124400         PERFORM PASS GO TO CC--WRITE-GF-29.                      NC2114.2
124500     GO TO CC--FAIL-GF-29.                                        NC2114.2
124600 CC--DELETE-GF-29.                                                NC2114.2
124700     PERFORM  DE-LETE.                                            NC2114.2
124800     GO       TO CC--WRITE-GF-29.                                 NC2114.2
124900 CC--FAIL-GF-29.                                                  NC2114.2
125000     MOVE     IF-TABLE TO COMPUTED-A.                             NC2114.2
125100     MOVE     "120000000000" TO CORRECT-A.                        NC2114.2
125200     PERFORM  FAIL.                                               NC2114.2
125300 CC--WRITE-GF-29.                                                 NC2114.2
125400     PERFORM  PRINT-DETAIL.                                       NC2114.2
125500*                                                                 NC2114.2
125600 CC--INIT-GF-30.                                                  NC2114.2
125700     MOVE "CC--TEST-GF-30"  TO PAR-NAME.                          NC2114.2
125800     MOVE "VI-89 6.15.3/4"  TO ANSI-REFERENCE.                    NC2114.2
125900     MOVE "  LOGICAL *NOT*" TO FEATURE.                           NC2114.2
126000     MOVE "AAAAA"           TO AZE.                               NC2114.2
126100     MOVE -2                TO MINUS-TWO.                         NC2114.2
126200     MOVE  2                TO TWO.                               NC2114.2
126300 CC--TEST-GF-30.                                                  NC2114.2
126400     IF  NOT AZE < "AAAAA"                                        NC2114.2
126500               AND                                                NC2114.2
126600         MINUS-TWO < TWO                                          NC2114.2
126700          PERFORM PASS                                            NC2114.2
126800     ELSE                                                         NC2114.2
126900          GO TO CC--FAIL-GF-30.                                   NC2114.2
127000*        NOTE CC--TEST-GF-30 TESTS LOGICAL "NOT" PLUS "AND" ---   NC2114.2
127100*             FIRST LINE TRUE,                                    NC2114.2
127200*             SECOND LINE TRUE.                                   NC2114.2
127300     GO TO CC--WRITE-GF-30.                                       NC2114.2
127400 CC--DELETE-GF-30.                                                NC2114.2
127500     PERFORM  DE-LETE.                                            NC2114.2
127600     GO TO CC--WRITE-GF-30.                                       NC2114.2
127700 CC--FAIL-GF-30.                                                  NC2114.2
127800     PERFORM FAIL.                                                NC2114.2
127900 CC--WRITE-GF-30.                                                 NC2114.2
128000     PERFORM  PRINT-DETAIL.                                       NC2114.2
128100*                                                                 NC2114.2
128200 CC--INIT-GF-31.                                                  NC2114.2
128300     MOVE     "CC--TEST-GF-31" TO PAR-NAME.                       NC2114.2
128400     MOVE "VI-89 6.15.3/4"  TO ANSI-REFERENCE.                    NC2114.2
128500     MOVE 10  TO TEN.                                             NC2114.2
128600     MOVE  3  TO THREE.                                           NC2114.2
128700 CC--TEST-GF-31.                                                  NC2114.2
128800     IF NOT TEN = 10.00000000                                     NC2114.2
128900               OR                                                 NC2114.2
129000          THREE = TEN                                             NC2114.2
129100          GO TO CC--FAIL-GF-31                                    NC2114.2
129200     ELSE                                                         NC2114.2
129300          PERFORM PASS.                                           NC2114.2
129400*        NOTE CC--TEST-GF-31 TESTS LOGICAL "NOT" PLUS "OR" ---    NC2114.2
129500*             FIRST LINE FALSE,                                   NC2114.2
129600*             SECOND LINE FALSE.                                  NC2114.2
129700     GO TO CC--WRITE-GF-31.                                       NC2114.2
129800 CC--DELETE-GF-31.                                                NC2114.2
129900     PERFORM  DE-LETE.                                            NC2114.2
130000     GO TO CC--WRITE-GF-31.                                       NC2114.2
130100 CC--FAIL-GF-31.                                                  NC2114.2
130200     PERFORM FAIL.                                                NC2114.2
130300 CC--WRITE-GF-31.                                                 NC2114.2
130400     PERFORM  PRINT-DETAIL.                                       NC2114.2
130500*                                                                 NC2114.2
130600 CC--INIT-GF-32.                                                  NC2114.2
130700     MOVE  "CC--TEST-GF-32" TO PAR-NAME.                          NC2114.2
130800     MOVE "VI-89 6.15.3/4"  TO ANSI-REFERENCE.                    NC2114.2
130900     MOVE  0                TO NAUGHT.                            NC2114.2
131000     MOVE  3                TO THREE.                             NC2114.2
131100     MOVE  6                TO SIX.                               NC2114.2
131200 CC--TEST-GF-32.                                                  NC2114.2
131300     IF      NOT (NAUGHT > THREE                                  NC2114.2
131400                        OR                                        NC2114.2
131500                  NAUGHT < ZERO)                                  NC2114.2
131600                    AND                                           NC2114.2
131700             6.00000000000000001 NOT EQUAL TO SIX                 NC2114.2
131800          PERFORM PASS                                            NC2114.2
131900     ELSE                                                         NC2114.2
132000          GO TO CC--FAIL-GF-32.                                   NC2114.2
132100*        NOTE CC--TEST-GF-32 TESTS LOGICAL "NOT" OF PARENTHESIZED NC2114.2
132200*             CONDITION PLUS "AND" ---                            NC2114.2
132300*             FIRST LINE (WITHIN PARENTHESES) FALSE               NC2114.2
132400*             SECOND LINE (WITHIN PARENTHESES) FALSE              NC2114.2
132500*             FIRST PLUS SECOND LINE (WITHIN PARENS) FALSE        NC2114.2
132600*             FIRST PLUS SECOND LINE TRUE                         NC2114.2
132700*             THIRD LINE TRUE.                                    NC2114.2
132800     GO TO CC--WRITE-GF-32.                                       NC2114.2
132900 CC--DELETE-GF-32.                                                NC2114.2
133000     PERFORM  DE-LETE.                                            NC2114.2
133100     GO TO CC--WRITE-GF-32.                                       NC2114.2
133200 CC--FAIL-GF-32.                                                  NC2114.2
133300     PERFORM FAIL.                                                NC2114.2
133400 CC--WRITE-GF-32.                                                 NC2114.2
133500     PERFORM  PRINT-DETAIL.                                       NC2114.2
133600*                                                                 NC2114.2
133700 CC--INIT-GF-33.                                                  NC2114.2
133800     MOVE "CC--TEST-GF-33"     TO PAR-NAME.                       NC2114.2
133900     MOVE "VI-89 6.15.3/4"     TO ANSI-REFERENCE.                 NC2114.2
134000     MOVE +.333333333333333333 TO ONE-THIRD.                      NC2114.2
134100     MOVE "AAAAA"              TO AZE.                            NC2114.2
134200     MOVE "CCCCC"              TO CEEZE.                          NC2114.2
134300 CC--TEST-GF-33.                                                  NC2114.2
134400     IF      ( .3703703333 ) EQUAL TO ONE-THIRD                   NC2114.2
134500          AND NOT AZE EQUAL TO CEEZE                              NC2114.2
134600              GO TO CC--FAIL-GF-33                                NC2114.2
134700     ELSE                                                         NC2114.2
134800              PERFORM PASS.                                       NC2114.2
134900*       NOTE CC--TEST-GF-33 TESTS LOGICAL "NOT" FOLLOWING AN "AND"NC2114.2
135000*             FIRST LINE FALSE,                                   NC2114.2
135100*             SECOND LINE TRUE.                                   NC2114.2
135200     GO       TO CC--WRITE-GF-33.                                 NC2114.2
135300 CC--DELETE-GF-33.                                                NC2114.2
135400     PERFORM DE-LETE.                                             NC2114.2
135500     GO TO CC--WRITE-GF-33.                                       NC2114.2
135600 CC--FAIL-GF-33.                                                  NC2114.2
135700     PERFORM FAIL.                                                NC2114.2
135800 CC--WRITE-GF-33.                                                 NC2114.2
135900     PERFORM  PRINT-DETAIL.                                       NC2114.2
136000*                                                                 NC2114.2
136100 CC--INIT-GF-34.                                                  NC2114.2
136200     MOVE "CC--TEST-GF-34" TO PAR-NAME.                           NC2114.2
136300     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
136400     MOVE 11  TO CCON-1.                                          NC2114.2
136500     MOVE 12  TO CCON-2.                                          NC2114.2
136600     MOVE 13  TO CCON-3.                                          NC2114.2
136700     MOVE 14  TO CCON-4.                                          NC2114.2
136800 CC--TEST-GF-34.                                                  NC2114.2
136900     IF NOT (CCON-4 NOT GREATER THAN CCON-2  AND CCON-3           NC2114.2
137000              AND NOT CCON-1)                                     NC2114.2
137100           PERFORM PASS                                           NC2114.2
137200     ELSE                                                         NC2114.2
137300           GO TO CC--FAIL-GF-34.                                  NC2114.2
137400*                                                                 NC2114.2
137500*    NOTE     THE ABOVE STATEMENT TESTS THE USE OF A COMPLEX      NC2114.2
137600*             CONDITION  WITH  COMBINATIONS  OF LOGICAL OPERATORS NC2114.2
137700*             ABREVIATED RELATIONAL OPERATORS AND  OMITTED        NC2114.2
137800*             CONDITION SUBJECTS.                                 NC2114.2
137900*             THE EXPANDED EQUIVALENT OF THIS STATEMENT IS -      NC2114.2
138000*               "NOT  (((CCON-4 NOT > CCON-2) AND (CCON-4 NOT >   NC2114.2
138100*                    CCON-3)) AND (NOT (CCON-4 NOT > CCON-1)))"   NC2114.2
138200     GO TO CC--WRITE-GF-34.                                       NC2114.2
138300 CC--DELETE-GF-34.                                                NC2114.2
138400     PERFORM  DE-LETE.                                            NC2114.2
138500     GO TO CC--WRITE-GF-34.                                       NC2114.2
138600 CC--FAIL-GF-34.                                                  NC2114.2
138700     PERFORM FAIL.                                                NC2114.2
138800 CC--WRITE-GF-34.                                                 NC2114.2
138900     PERFORM  PRINT-DETAIL.                                       NC2114.2
139000*                                                                 NC2114.2
139100 CC--INIT-GF-35.                                                  NC2114.2
139200     MOVE "CC--TEST-GF-35" TO PAR-NAME.                           NC2114.2
139300     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
139400     MOVE -1            TO MINUS-ONE.                             NC2114.2
139500     MOVE  +.4285714286 TO THREE-SEVENTHS.                        NC2114.2
139600     MOVE  1            TO ONE.                                   NC2114.2
139700     MOVE  2            TO TWO.                                   NC2114.2
139800     MOVE +2            TO DOS.                                   NC2114.2
139900     MOVE  5            TO FIVE.                                  NC2114.2
140000 CC--TEST-GF-35.                                                  NC2114.2
140100     IF           TWO > DOS                                       NC2114.2
140200                     OR                                           NC2114.2
140300               NOT ( THREE-SEVENTHS ) EQUAL TO FIVE               NC2114.2
140400                     AND                                          NC2114.2
140500                MINUS-ONE = ONE                                   NC2114.2
140600          GO TO CC--FAIL-GF-35                                    NC2114.2
140700     ELSE                                                         NC2114.2
140800          PERFORM PASS.                                           NC2114.2
140900*        NOTE CC--TEST-GF-35 TESTS LOGICAL "NOT" WHICH FOLLOWS AN NC2114.2
141000*             "OR" AND PRECEDES AN "AND" ---                      NC2114.2
141100*             FIRST LINE FALSE                                    NC2114.2
141200*             SECOND LINE TRUE                                    NC2114.2
141300*             THIRD LINE FALSE.                                   NC2114.2
141400     GO TO CC--WRITE-GF-35.                                       NC2114.2
141500 CC--DELETE-GF-35.                                                NC2114.2
141600     PERFORM  DE-LETE.                                            NC2114.2
141700     GO TO CC--WRITE-GF-35.                                       NC2114.2
141800 CC--FAIL-GF-35.                                                  NC2114.2
141900     PERFORM FAIL.                                                NC2114.2
142000 CC--WRITE-GF-35.                                                 NC2114.2
142100     PERFORM  PRINT-DETAIL.                                       NC2114.2
142200*                                                                 NC2114.2
142300 CC--INIT-GF-36.                                                  NC2114.2
142400     MOVE "CC--TEST-GF-36" TO PAR-NAME.                           NC2114.2
142500     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
142600     MOVE "AAAAA" TO AZE.                                         NC2114.2
142700     MOVE "BBBBB" TO BEEZE.                                       NC2114.2
142800     MOVE  5      TO FIVE.                                        NC2114.2
142900 CC--TEST-GF-36.                                                  NC2114.2
143000     IF           AZE = BEEZE                                     NC2114.2
143100           OR NOT (5 > FIVE       AND                             NC2114.2
143200              NOT  5 > FIVE)                                      NC2114.2
143300          PERFORM PASS                                            NC2114.2
143400     ELSE                                                         NC2114.2
143500          GO TO CC--FAIL-GF-36.                                   NC2114.2
143600*        NOTE CC--TEST-GF-36 TESTS LOGICAL "NOT" WHICH FOLLOWS AN NC2114.2
143700*             "OR" AND PRECEDES A PARENTHESIZED CONDITION ---     NC2114.2
143800*             FIRST LINE FALSE,                                   NC2114.2
143900*             SECOND LINE (WITHIN PARENS) FALSE                   NC2114.2
144000*             THIRD LINE (WITHIN PARENS) TRUE                     NC2114.2
144100*             SECOND PLUS THIRD LINE (WITHIN PARENS) FALSE        NC2114.2
144200*             SECOND PLUS THIRD LINE TRUE.                        NC2114.2
144300     GO TO CC--WRITE-GF-36.                                       NC2114.2
144400 CC--DELETE-GF-36.                                                NC2114.2
144500     PERFORM  DE-LETE.                                            NC2114.2
144600     GO TO CC--WRITE-GF-36.                                       NC2114.2
144700 CC--FAIL-GF-36.                                                  NC2114.2
144800     PERFORM FAIL.                                                NC2114.2
144900 CC--WRITE-GF-36.                                                 NC2114.2
145000     PERFORM  PRINT-DETAIL.                                       NC2114.2
145100*                                                                 NC2114.2
145200 CC--INIT-GF-37.                                                  NC2114.2
145300     MOVE "CC--TEST-GF-37" TO PAR-NAME.                           NC2114.2
145400     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
145500     MOVE  0 TO NAUGHT.                                           NC2114.2
145600     MOVE  1 TO ONE.                                              NC2114.2
145700     MOVE +1 TO UNO.                                              NC2114.2
145800     MOVE  2 TO TWO.                                              NC2114.2
145900     MOVE +2 TO DOS.                                              NC2114.2
146000 CC--TEST-GF-37.                                                  NC2114.2
146100     IF          ((NAUGHT EQUAL TO ONE)                           NC2114.2
146200           OR (NOT ((UNO = ONE)         OR                        NC2114.2
146300                    (TWO = DOS))))                                NC2114.2
146400         GO TO CC--FAIL-GF-37                                     NC2114.2
146500     ELSE                                                         NC2114.2
146600         PERFORM PASS.                                            NC2114.2
146700*        NOTE CC--TEST-GF-37 TESTS LOGICAL "NOT" THAT IS CONTAINEDNC2114.2
146800*             PARENTHESES AND WHICH PRECEDES A PARENTHESIZED      NC2114.2
146900*             CONDITION ---                                       NC2114.2
147000*             FIRST LINE (IN INNER PARENS) FALSE                  NC2114.2
147100*             SECOND LINE (IN INNER PARENS) TRUE                  NC2114.2
147200*             THIRD LINE (IN INNER PARENS) FALSE                  NC2114.2
147300*             SECOND PLUS THIRD LINE (IN MIDDLE PARENS) TRUE      NC2114.2
147400*             SECOND PLUS THIRD LINE (IN OUTER PARENS) FALSE      NC2114.2
147500*             PARENS AROUND ENTIRE CONDITION ARE REDUNDANT.       NC2114.2
147600     GO TO CC--WRITE-GF-37.                                       NC2114.2
147700 CC--DELETE-GF-37.                                                NC2114.2
147800     PERFORM  DE-LETE.                                            NC2114.2
147900     GO TO CC--WRITE-GF-37.                                       NC2114.2
148000 CC--FAIL-GF-37.                                                  NC2114.2
148100     PERFORM  FAIL.                                               NC2114.2
148200 CC--WRITE-GF-37.                                                 NC2114.2
148300     PERFORM  PRINT-DETAIL.                                       NC2114.2
148400*                                                                 NC2114.2
148500 CC--INIT-GF-38.                                                  NC2114.2
148600     MOVE "CC--TEST-GF-38" TO PAR-NAME.                           NC2114.2
148700     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
148800     MOVE "AAAAA" TO AZE.                                         NC2114.2
148900     MOVE  1      TO ONE.                                         NC2114.2
149000     MOVE  2      TO TWO.                                         NC2114.2
149100     MOVE  3      TO THREE.                                       NC2114.2
149200 CC--TEST-GF-38.                                                  NC2114.2
149300     IF       NOT AZE LESS THAN ONE      AND                      NC2114.2
149400              NOT ONE < AZE                                       NC2114.2
149500           OR           TWO              AND                      NC2114.2
149600              NOT THREE LESS THAN TWO                             NC2114.2
149700         PERFORM PASS                                             NC2114.2
149800     ELSE                                                         NC2114.2
149900         GO TO CC--FAIL-GF-38.                                    NC2114.2
150000*        NOTE CC--TEST-GF-38 TESTS LOGICAL "NOT" FOLLOWING "AND" ANC2114.2
150100*             IN COMBINATION WITH AN ABBREVIATION ---             NC2114.2
150200*             EITHER FIRST LINE OR SECOND LINE MUST BE FALSE,     NC2114.2
150300*             THEREFORE,                                          NC2114.2
150400*             FIRST PLUS SECOND LINES FALSE                       NC2114.2
150500*             ABBREVIATED THIRD LINE TRUE                         NC2114.2
150600*             FOURTH LINE TRUE                                    NC2114.2
150700*             THIRD PLUS FOURTH LINES TRUE.                       NC2114.2
150800     GO TO CC--WRITE-GF-38.                                       NC2114.2
150900 CC--DELETE-GF-38.                                                NC2114.2
151000     PERFORM  DE-LETE.                                            NC2114.2
151100     GO TO CC--WRITE-GF-38.                                       NC2114.2
151200 CC--FAIL-GF-38.                                                  NC2114.2
151300     PERFORM  FAIL.                                               NC2114.2
151400 CC--WRITE-GF-38.                                                 NC2114.2
151500     PERFORM  PRINT-DETAIL.                                       NC2114.2
151600*                                                                 NC2114.2
151700 CC--INIT-GF-39.                                                  NC2114.2
151800     MOVE "CC--TEST-GF-39" TO PAR-NAME.                           NC2114.2
151900     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
152000     MOVE "  SIGN CONDITIONS" TO FEATURE.                         NC2114.2
152100     MOVE 0 TO SIGN-1.                                            NC2114.2
152200     MOVE 0 TO SIGN-2.                                            NC2114.2
152300     MOVE 9 TO SIGN-3.                                            NC2114.2
152400 CC--TEST-GF-39.                                                  NC2114.2
152500     IF       NOT (SIGN-1 POSITIVE      OR                        NC2114.2
152600                   SIGN-2 NEGATIVE)                               NC2114.2
152700          AND     SIGN-3 NOT ZERO                                 NC2114.2
152800         PERFORM PASS                                             NC2114.2
152900     ELSE                                                         NC2114.2
153000         GO TO CC--FAIL-GF-39.                                    NC2114.2
153100*        NOTE CC--TEST-GF-39 TESTS SIGN CONDITIONS WITH SEVERAL TYNC2114.2
153200*             OF LOGICAL CONNECTORS INCLUDING PARENTHESES ---     NC2114.2
153300*             FIRST LINE FALSE                                    NC2114.2
153400*             SECOND LINE FALSE                                   NC2114.2
153500*             FIRST PLUS SECOND LINES (WITHIN PARENS) FALSE       NC2114.2
153600*             FIRST PLUS SECOND LINES TRUE                        NC2114.2
153700*             THIRD LINE TRUE.                                    NC2114.2
153800     GO TO CC--WRITE-GF-39.                                       NC2114.2
153900 CC--DELETE-GF-39.                                                NC2114.2
154000     PERFORM  DE-LETE.                                            NC2114.2
154100     GO TO CC--WRITE-GF-39.                                       NC2114.2
154200 CC--FAIL-GF-39.                                                  NC2114.2
154300     PERFORM  FAIL.                                               NC2114.2
154400 CC--WRITE-GF-39.                                                 NC2114.2
154500     PERFORM  PRINT-DETAIL.                                       NC2114.2
154600*                                                                 NC2114.2
154700 CC--INIT-GF-40.                                                  NC2114.2
154800     MOVE "CC--TEST-GF-40" TO PAR-NAME.                           NC2114.2
154900     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
155000     MOVE "  SIGN CONDITIONS" TO FEATURE.                         NC2114.2
155100     MOVE -5 TO SIGN-1.                                           NC2114.2
155200     MOVE -1 TO SIGN-2.                                           NC2114.2
155300     MOVE  0 TO SIGN-3.                                           NC2114.2
155400 CC--TEST-GF-40.                                                  NC2114.2
155500     IF           SIGN-1 IS POSITIVE                              NC2114.2
155600           OR NOT SIGN-2 IS NEGATIVE                              NC2114.2
155700          AND     SIGN-3 IS ZERO                                  NC2114.2
155800         GO TO CC--FAIL-GF-40                                     NC2114.2
155900     ELSE                                                         NC2114.2
156000         PERFORM PASS.                                            NC2114.2
156100*        NOTE CC--TEST-GF-40 TESTS SIGN CONDITIONS WITH SEVERAL TYNC2114.2
156200*             OF LOGICAL CONNECTORS BUT NO PARENTHESES ---        NC2114.2
156300*             FIRST LINE FALSE                                    NC2114.2
156400*             SECOND LINE FALSE                                   NC2114.2
156500*             THIRD LINE TRUE.                                    NC2114.2
156600     GO TO CC--WRITE-GF-40.                                       NC2114.2
156700 CC--DELETE-GF-40.                                                NC2114.2
156800     PERFORM  DE-LETE.                                            NC2114.2
156900     GO TO CC--WRITE-GF-40.                                       NC2114.2
157000 CC--FAIL-GF-40.                                                  NC2114.2
157100     PERFORM  FAIL.                                               NC2114.2
157200 CC--WRITE-GF-40.                                                 NC2114.2
157300     PERFORM  PRINT-DETAIL.                                       NC2114.2
157400*                                                                 NC2114.2
157500 CC--INIT-GF-41.                                                  NC2114.2
157600     MOVE "CC--TEST-GF-41" TO PAR-NAME.                           NC2114.2
157700     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
157800     MOVE "  CLASS CONDITIONS" TO FEATURE.                        NC2114.2
157900     MOVE SPACE TO CLASS-1.                                       NC2114.2
158000     MOVE ZERO  TO CLASS-2.                                       NC2114.2
158100     MOVE ZERO  TO CLASS-3.                                       NC2114.2
158200 CC--TEST-GF-41.                                                  NC2114.2
158300     IF       NOT (CLASS-1 NUMERIC      OR                        NC2114.2
158400                   CLASS-2 ALPHABETIC)                            NC2114.2
158500          AND     CLASS-3 NOT NUMERIC                             NC2114.2
158600         GO TO CC--FAIL-GF-41                                     NC2114.2
158700     ELSE                                                         NC2114.2
158800         PERFORM PASS.                                            NC2114.2
158900*        NOTE CC--TEST-GF-41 TESTS CLASS CONDITIONS WITH SEVERAL  NC2114.2
159000*             TYPES OF LOGICAL CONNECTORS INCLUDING PARENTHESES --NC2114.2
159100*             FIRST LINE FALSE                                    NC2114.2
159200*             SECOND LINE FALSE                                   NC2114.2
159300*             FIRST PLUS SECOND LINES (WITHIN PARENS) FALSE       NC2114.2
159400*             FIRST PLUS SECOND LINES TRUE                        NC2114.2
159500*             THIRD LINE FALSE.                                   NC2114.2
159600     GO TO CC--WRITE-GF-41.                                       NC2114.2
159700 CC--DELETE-GF-41.                                                NC2114.2
159800     PERFORM  DE-LETE.                                            NC2114.2
159900     GO TO CC--WRITE-GF-41.                                       NC2114.2
160000 CC--FAIL-GF-41.                                                  NC2114.2
160100     PERFORM  FAIL.                                               NC2114.2
160200 CC--WRITE-GF-41.                                                 NC2114.2
160300     PERFORM  PRINT-DETAIL.                                       NC2114.2
160400*                                                                 NC2114.2
160500 CC--INIT-GF-42.                                                  NC2114.2
160600     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
160700     MOVE "  CLASS CONDITIONS" TO FEATURE.                        NC2114.2
160800     MOVE "CC--TEST-GF-42" TO PAR-NAME.                           NC2114.2
160900     MOVE 12345 TO CLASS-1.                                       NC2114.2
161000     MOVE 12345 TO CLASS-2.                                       NC2114.2
161100     MOVE 12345 TO CLASS-3.                                       NC2114.2
161200 CC--TEST-GF-42.                                                  NC2114.2
161300     IF           CLASS-1 NUMERIC                                 NC2114.2
161400           OR NOT CLASS-2 ALPHABETIC                              NC2114.2
161500          AND     CLASS-3 NUMERIC                                 NC2114.2
161600         PERFORM PASS                                             NC2114.2
161700     ELSE                                                         NC2114.2
161800         GO TO CC--FAIL-GF-42.                                    NC2114.2
161900*        NOTE CC--TEST-GF-42 TESTS CLASS CONDITIONS WITH SEVERAL  NC2114.2
162000*             TYPES OF LOGICAL CONNECTORS BUT NO PARENTHESES ---  NC2114.2
162100*             FIRST LINE TRUE                                     NC2114.2
162200*             SECOND LINE TRUE                                    NC2114.2
162300*             THIRD LINE TRUE.                                    NC2114.2
162400     GO TO CC--WRITE-GF-42.                                       NC2114.2
162500 CC--DELETE-GF-42.                                                NC2114.2
162600     PERFORM  DE-LETE.                                            NC2114.2
162700     GO TO CC--WRITE-GF-42.                                       NC2114.2
162800 CC--FAIL-GF-42.                                                  NC2114.2
162900     PERFORM  FAIL.                                               NC2114.2
163000 CC--WRITE-GF-42.                                                 NC2114.2
163100     PERFORM  PRINT-DETAIL.                                       NC2114.2
163200*                                                                 NC2114.2
163300 CC--INIT-GF-43.                                                  NC2114.2
163400     MOVE "CC--TEST-GF-43" TO PAR-NAME.                           NC2114.2
163500     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
163600     MOVE "  SWITCH CONDITIONS" TO FEATURE.                       NC2114.2
163700 CC--TEST-GF-43.                                                  NC2114.2
163800     IF       NOT (ON-WRK-SWITCH-1       OR                       NC2114.2
163900                   OFF-WRK-SWITCH-2)                              NC2114.2
164000          AND NOT OFF-WRK-SWITCH-1                                NC2114.2
164100         GO TO CC--FAIL-GF-43                                     NC2114.2
164200     ELSE                                                         NC2114.2
164300         PERFORM PASS.                                            NC2114.2
164400*    NOTE ***     ***   *** IF SWITCHES ARE NOT IMPLEMENTED       NC2114.2
164500*        THE CONDITION-NAMES WILL AUTOMATICALLY BE ASSIGNED TO AN NC2114.2
164600*        01 IN WORKING-STORAGE THEREBY SATISFYING THE TEST.       NC2114.2
164700*        NOTE CC--TEST-GF-43 TESTS SWITCH-STATUS CONDITIONS WITH  NC2114.2
164800*             SEVERAL TYPES OF LOGICAL CONNECTORS INCLUDING       NC2114.2
164900*             PARENTHESES ---                                     NC2114.2
165000*             FIRST LINE TRUE                                     NC2114.2
165100*             SECOND LINE TRUE                                    NC2114.2
165200*             FIRST PLUS SECOND LINES (WITHIN PARENS) TRUE        NC2114.2
165300*             FIRST PLUS SECOND LINES FALSE                       NC2114.2
165400*             THIRD LINE TRUE.                                    NC2114.2
165500     GO TO CC--WRITE-GF-43.                                       NC2114.2
165600 CC--DELETE-GF-43.                                                NC2114.2
165700     PERFORM  DE-LETE.                                            NC2114.2
165800     GO TO CC--WRITE-GF-43.                                       NC2114.2
165900 CC--FAIL-GF-43.                                                  NC2114.2
166000     PERFORM  FAIL.                                               NC2114.2
166100 CC--WRITE-GF-43.                                                 NC2114.2
166200     PERFORM  PRINT-DETAIL.                                       NC2114.2
166300*                                                                 NC2114.2
166400 CC--INIT-GF-44.                                                  NC2114.2
166500     MOVE "CC--TEST-GF-44" TO PAR-NAME.                           NC2114.2
166600     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
166700     MOVE "  SWITCH CONDITIONS" TO FEATURE.                       NC2114.2
166800 CC--TEST-GF-44.                                                  NC2114.2
166900     IF           ON-WRK-SWITCH-1                                 NC2114.2
167000           OR NOT OFF-WRK-SWITCH-2                                NC2114.2
167100          AND     OFF-WRK-SWITCH-1                                NC2114.2
167200         PERFORM PASS                                             NC2114.2
167300     ELSE                                                         NC2114.2
167400         GO TO CC--FAIL-GF-44.                                    NC2114.2
167500*        NOTE CC--TEST-GF-44 TESTS SWITCH-STATUS CONDITIONS WITH  NC2114.2
167600*             SEVERAL TYPES OF LOGICAL CONNECTORS BUT WITHOUT     NC2114.2
167700*             PARENTHESES ---                                     NC2114.2
167800*             FIRST LINE TRUE                                     NC2114.2
167900*             SECOND LINE FALSE                                   NC2114.2
168000*             THIRD LINE FALSE.                                   NC2114.2
168100     GO TO CC--WRITE-GF-44.                                       NC2114.2
168200 CC--DELETE-GF-44.                                                NC2114.2
168300     PERFORM  DE-LETE.                                            NC2114.2
168400     GO TO CC--WRITE-GF-44.                                       NC2114.2
168500 CC--FAIL-GF-44.                                                  NC2114.2
168600     PERFORM  FAIL.                                               NC2114.2
168700 CC--WRITE-GF-44.                                                 NC2114.2
168800     PERFORM  PRINT-DETAIL.                                       NC2114.2
168900*                                                                 NC2114.2
169000 CC--INIT-GF-45.                                                  NC2114.2
169100     MOVE "CC--TEST-GF-45" TO PAR-NAME.                           NC2114.2
169200     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
169300     MOVE "  CONDITION-NAMES" TO FEATURE.                         NC2114.2
169400     MOVE "AA" TO AA.                                             NC2114.2
169500     MOVE "CD" TO BB.                                             NC2114.2
169600     MOVE "C"  TO AAA.                                            NC2114.2
169700 CC--TEST-GF-45.                                                  NC2114.2
169800     IF       NOT (A1     OR                                      NC2114.2
169900                   B1)                                            NC2114.2
170000          AND NOT AA1                                             NC2114.2
170100         GO TO CC--FAIL-GF-45                                     NC2114.2
170200     ELSE                                                         NC2114.2
170300         PERFORM PASS.                                            NC2114.2
170400*        NOTE CC--TEST-GF-45 TESTS CONDITION-NAME CONDITIONS WITH NC2114.2
170500*             SEVERAL TYPES OF LOGICAL CONNECTORS INCLUDING       NC2114.2
170600*             PARENTHESES ---                                     NC2114.2
170700*             FIRST LINE TRUE                                     NC2114.2
170800*             SECOND LINE FALSE                                   NC2114.2
170900*             FIRST PLUS SECOND LINE (WITHIN PARENS) TRUE         NC2114.2
171000*             FIRST PLUS SECOND LINE FALSE                        NC2114.2
171100*             THIRD LINE TRUE.                                    NC2114.2
171200     GO TO CC--WRITE-GF-45.                                       NC2114.2
171300 CC--DELETE-GF-45.                                                NC2114.2
171400     PERFORM  DE-LETE.                                            NC2114.2
171500     GO TO CC--WRITE-GF-45.                                       NC2114.2
171600 CC--FAIL-GF-45.                                                  NC2114.2
171700     PERFORM  FAIL.                                               NC2114.2
171800 CC--WRITE-GF-45.                                                 NC2114.2
171900     PERFORM  PRINT-DETAIL.                                       NC2114.2
172000*                                                                 NC2114.2
172100 CC--INIT-GF-46.                                                  NC2114.2
172200     MOVE "CC--TEST-GF-46" TO PAR-NAME.                           NC2114.2
172300     MOVE "VI-89 6.15.3/4" TO ANSI-REFERENCE.                     NC2114.2
172400     MOVE "  CONDITION-NAMES" TO FEATURE.                         NC2114.2
172500     MOVE     "AB" TO AA.                                         NC2114.2
172600     MOVE     "CD" TO BB.                                         NC2114.2
172700     MOVE     "A" TO AAA.                                         NC2114.2
172800 CC--TEST-GF-46.                                                  NC2114.2
172900     IF           A1                                              NC2114.2
173000           OR NOT B1                                              NC2114.2
173100          AND     AA1                                             NC2114.2
173200         PERFORM PASS                                             NC2114.2
173300     ELSE                                                         NC2114.2
173400         GO TO CC--FAIL-GF-46.                                    NC2114.2
173500*        NOTE CC--TEST-GF-46 TESTS CONDITION-NAME CONDITIONS WITH NC2114.2
173600*             SEVERAL TYPES OF LOGICAL CONNECTORS BUT NO          NC2114.2
173700*             PARENTHESES ---                                     NC2114.2
173800*             FIRST LINE FALSE                                    NC2114.2
173900*             SECOND LINE TRUE                                    NC2114.2
174000*             THIRD LINE TRUE.                                    NC2114.2
174100     GO TO CC--WRITE-GF-46.                                       NC2114.2
174200 CC--DELETE-GF-46.                                                NC2114.2
174300     PERFORM  DE-LETE.                                            NC2114.2
174400     GO TO CC--WRITE-GF-46.                                       NC2114.2
174500 CC--FAIL-GF-46.                                                  NC2114.2
174600     PERFORM  FAIL.                                               NC2114.2
174700 CC--WRITE-GF-46.                                                 NC2114.2
174800     PERFORM  PRINT-DETAIL.                                       NC2114.2
174900*  PAUL WOZ UPTO HERE.                                            NC2114.2
175000 CC--INIT-GF-47.                                                  NC2114.2
175100     MOVE     "  MIXED CONDITIONS" TO FEATURE.                    NC2114.2
175200     MOVE     -1 TO IF-D32.                                       NC2114.2
175300     MOVE     "ABCD4" TO CLASS-1.                                 NC2114.2
175400     MOVE     -1 TO SIGN-1.                                       NC2114.2
175500 CC--TEST-GF-47.                                                  NC2114.2
175600*    NOTE IF SWITCHES ARE NOT IMPLEMENTED SWITCH-1 WILL           NC2114.2
175700*        BE AUTOMATICALLY TURNED ON TO FULFILL THE REQUIREMENTS   NC2114.2
175800*        OF THIS TEST.                                            NC2114.2
175900     IF       NOT TWO > THREE                                     NC2114.2
176000          AND NOT (ON-WRK-SWITCH-1              AND               NC2114.2
176100                   F                        OR                    NC2114.2
176200                   CLASS-1 ALPHABETIC)                            NC2114.2
176300           OR     TWO = THREE                                     NC2114.2
176400          AND     SIGN-1 ZERO                                     NC2114.2
176500              PERFORM PASS ELSE PERFORM FAIL.                     NC2114.2
176600*        NOTE CC--TEST-GF-47 TESTS A COMPOUND CONDITION WHICH     NC2114.2
176700*             CONTAINS ALL OF THE TYPES OF SIMPLE CONDITIONS AND  NC2114.2
176800*             SEVERAL TYPES OF LOGICAL CONNECTORS ---             NC2114.2
176900*             FIRST LINE TRUE                                     NC2114.2
177000*             SECOND LINE TRUE                                    NC2114.2
177100*             THIRD LINE FALSE                                    NC2114.2
177200*             FOURTH LINE FALSE                                   NC2114.2
177300*             SECOND THRU FOURTH LINES (WITHIN PARENS) FALSE      NC2114.2
177400*             SECOND THRU FOURTH LINES TRUE                       NC2114.2
177500*             FIRST THRU FOURTH LINES TRUE                        NC2114.2
177600*             FIFTH LINE FALSE                                    NC2114.2
177700*             SIXTH LINE FALSE                                    NC2114.2
177800*             FIFTH THRU SIXTH LINES FALSE.                       NC2114.2
177900     GO       TO CC--WRITE-GF-47.                                 NC2114.2
178000 CC--DELETE-GF-47.                                                NC2114.2
178100     PERFORM  DE-LETE.                                            NC2114.2
178200 CC--WRITE-GF-47.                                                 NC2114.2
178300     MOVE     "CC--TEST-GF-47" TO PAR-NAME.                       NC2114.2
178400     PERFORM  PRINT-DETAIL.                                       NC2114.2
178500 CC--TEST-GF-48.                                                  NC2114.2
178600     MOVE     +9 TO SIGN-1.                                       NC2114.2
178700     MOVE     -5 TO SIGN-2.                                       NC2114.2
178800     MOVE     "+1234" TO CLASS-1.                                 NC2114.2
178900     MOVE     1235 TO IF-D32.                                     NC2114.2
179000*    NOTE IF SWITCHES ARE NOT IMPLEMENTED SWITCH-1 WILL BE        NC2114.2
179100*        AUTOMATICALLY TURNED ON TO FULFILL THE REQUIREMENTS      NC2114.2
179200*        OF THIS TEST.                                            NC2114.2
179300     IF      FOUR GREATER THAN 2.5                                NC2114.2
179400          AND          EQUAL TO QUATROS                           NC2114.2
179500          AND     (FOUR     = TEN      OR                         NC2114.2
179600                        NOT < TEN      OR                         NC2114.2
179700                   SIGN-1 POSITIVE     AND                        NC2114.2
179800                   (SIGN-2 NOT NEGATIVE          OR               NC2114.2
179900                    CLASS-1 NOT NUMERIC))                         NC2114.2
180000          AND NOT OFF-WRK-SWITCH-1                                NC2114.2
180100           OR     E                                               NC2114.2
180200          AND     F                                               NC2114.2
180300           OR NOT G                                               NC2114.2
180400              PERFORM PASS ELSE PERFORM FAIL.                     NC2114.2
180500*        NOTE CC--TEST-GF-48 TESTS A COMPOUND CONDITION WHICH     NC2114.2
180600*             CONTAINS ALL OF THE TYPES OF SIMPLE CONDITIONS AND  NC2114.2
180700*             SEVERAL TYPES OF LOGICAL CONNECTORS ---             NC2114.2
180800*         *   FIRST LINE TRUE                                     NC2114.2
180900*         *   SECOND ABBREVIATED LINE TRUE                        NC2114.2
181000*             THIRD LINE FALSE                                    NC2114.2
181100*             FOURTH LINE FALSE                                   NC2114.2
181200*             FIFTH LINE TRUE                                     NC2114.2
181300*             SIXTH LINE FALSE                                    NC2114.2
181400*             SEVENTH LINE TRUE                                   NC2114.2
181500*             SIXTH PLUS SEVENTH LINES TRUE                       NC2114.2
181600*         *   THIRD THRU SEVENTH LINES TRUE                       NC2114.2
181700*         *   EIGHTH LINE TRUE                                    NC2114.2
181800*        **   FIRST THRU EIGHTH LINES TRUE                        NC2114.2
181900*             NINTH LINE FALSE                                    NC2114.2
182000*             TENTH LINE TRUE                                     NC2114.2
182100*        **   NINTH PLUS TENTH LINES FALSE                        NC2114.2
182200*        **   ELEVENTH LINE FALSE.                                NC2114.2
182300     GO       TO CC--WRITE-GF-48.                                 NC2114.2
182400 CC--DELETE-GF-48.                                                NC2114.2
182500     PERFORM  DE-LETE.                                            NC2114.2
182600 CC--WRITE-GF-48.                                                 NC2114.2
182700     MOVE     "CC--TEST-GF-48" TO PAR-NAME.                       NC2114.2
182800     PERFORM  PRINT-DETAIL.                                       NC2114.2
182900 FIG-INIT-A.                                                      NC2114.2
183000     PERFORM END-ROUTINE.                                         NC2114.2
183100     MOVE     "FIGURATIVE CONSTANTS" TO FEATURE.                  NC2114.2
183200 FIG-TEST-1.                                                      NC2114.2
183300     MOVE     SUB-GRP-FOR-2N058-A TO SUB-GRP-FOR-2N058-B.         NC2114.2
183400     IF       SUB-SUB-BA EQUAL TO "000000   ABCABC"               NC2114.2
183500              PERFORM PASS GO TO FIG-WRITE-1.                     NC2114.2
183600     GO       TO FIG-FAIL-1.                                      NC2114.2
183700 FIG-DELETE-1.                                                    NC2114.2
183800     PERFORM  DE-LETE.                                            NC2114.2
183900     GO       TO FIG-WRITE-1.                                     NC2114.2
184000 FIG-FAIL-1.                                                      NC2114.2
184100     MOVE     SUB-SUB-BA TO COMPUTED-A.                           NC2114.2
184200     MOVE     "000000   ABCABC" TO CORRECT-A.                     NC2114.2
184300     PERFORM  FAIL.                                               NC2114.2
184400 FIG-WRITE-1.                                                     NC2114.2
184500     MOVE     "FIG-TEST-1" TO PAR-NAME.                           NC2114.2
184600     PERFORM  PRINT-DETAIL.                                       NC2114.2
184700 FIG-TEST-2.                                                      NC2114.2
184800     IF       SUB-SUB-BB EQUAL TO "ZZZ   000000"                  NC2114.2
184900              PERFORM PASS GO TO FIG-WRITE-2.                     NC2114.2
185000*    NOTE     THIS TEST DEPENDS UPON THE RESULT OF FIG-TEST-1.    NC2114.2
185100     GO       TO FIG-FAIL-2.                                      NC2114.2
185200 FIG-DELETE-2.                                                    NC2114.2
185300     PERFORM  DE-LETE.                                            NC2114.2
185400     GO       TO FIG-WRITE-2.                                     NC2114.2
185500 FIG-FAIL-2.                                                      NC2114.2
185600     MOVE     SUB-SUB-BB TO COMPUTED-A.                           NC2114.2
185700     MOVE     "ZZZ   000000" TO CORRECT-A.                        NC2114.2
185800     PERFORM  FAIL.                                               NC2114.2
185900 FIG-WRITE-2.                                                     NC2114.2
186000     MOVE "FIG-TEST-2" TO PAR-NAME.                               NC2114.2
186100     PERFORM  PRINT-DETAIL.                                       NC2114.2
186200 FIG-TEST-3.                                                      NC2114.2
186300     IF       ELEM-FOR-2N058-I OF SUB-SUB-BC NOT EQUAL TO QUOTE   NC2114.2
186400              GO TO FIG-FAIL-3.                                   NC2114.2
186500     IF ELEM-FOR-2N058-J OF SUB-SUB-BC NOT EQUAL TO QUOTE         NC2114.2
186600              GO TO FIG-FAIL-3.                                   NC2114.2
186700     IF ELEM-FOR-2N058-K OF SUB-SUB-BC NOT EQUAL TO HIGH-VALUE    NC2114.2
186800              GO TO FIG-FAIL-3.                                   NC2114.2
186900     IF ELEM-FOR-2N058-L OF SUB-SUB-BC NOT EQUAL TO LOW-VALUE     NC2114.2
187000              GO TO FIG-FAIL-3.                                   NC2114.2
187100     IF ELEM-FOR-2N058-M OF SUB-SUB-BC NOT EQUAL TO HIGH-VALUE    NC2114.2
187200              GO TO FIG-FAIL-3.                                   NC2114.2
187300     IF ELEM-FOR-2N058-N OF SUB-SUB-BC NOT EQUAL TO LOW-VALUE     NC2114.2
187400              GO TO FIG-FAIL-3.                                   NC2114.2
187500     PERFORM  PASS.                                               NC2114.2
187600     GO       TO FIG-WRITE-3.                                     NC2114.2
187700 FIG-DELETE-3.                                                    NC2114.2
187800     PERFORM  DE-LETE.                                            NC2114.2
187900     GO       TO FIG-WRITE-3.                                     NC2114.2
188000 FIG-FAIL-3.                                                      NC2114.2
188100     MOVE     SPACE TO FEATURE.                                   NC2114.2
188200     MOVE     "6 QUOTES, 3 HIGH-VALUES," TO RE-MARK.              NC2114.2
188300     PERFORM  PRINT-DETAIL.                                       NC2114.2
188400     MOVE     "3 LOW-VALUES, 3 HIGH-VALUES" TO RE-MARK.           NC2114.2
188500     PERFORM  PRINT-DETAIL.                                       NC2114.2
188600     MOVE     "FIGURATIVE CONSTANTS" TO FEATURE.                  NC2114.2
188700     MOVE     SUB-SUB-BC TO COMPUTED-A.                           NC2114.2
188800     MOVE     "SEE REMARKS" TO CORRECT-A.                         NC2114.2
188900     MOVE     "3 LOW-VALUES" TO RE-MARK.                          NC2114.2
189000     PERFORM  FAIL.                                               NC2114.2
189100 FIG-WRITE-3.                                                     NC2114.2
189200     MOVE     "FIG-TEST-3" TO PAR-NAME.                           NC2114.2
189300     PERFORM  PRINT-DETAIL.                                       NC2114.2
189400 CCVS-EXIT SECTION.                                               NC2114.2
189500 CCVS-999999.                                                     NC2114.2
189600     GO TO CLOSE-FILES.                                           NC2114.2
      *END-OF,NC211A                                                                  
