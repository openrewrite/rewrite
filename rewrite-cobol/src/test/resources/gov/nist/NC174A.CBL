      *HEADER,COBOL,NC174A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1744.2
000200 PROGRAM-ID.                                                      NC1744.2
000300     NC174A.                                                      NC1744.2
000400****************************************************************  NC1744.2
000500*                                                              *  NC1744.2
000600*    VALIDATION FOR:-                                          *  NC1744.2
000700*                                                              *  NC1744.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1744.2
000900*                                                              *  NC1744.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1744.2
001100*                                                              *  NC1744.2
001200****************************************************************  NC1744.2
001300*                                                              *  NC1744.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1744.2
001500*                                                              *  NC1744.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1744.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1744.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1744.2
001900*                                                              *  NC1744.2
002000****************************************************************  NC1744.2
002100*                                                                 NC1744.2
002200*    PROGRAM NC174A TESTS THE FOLLOWING GENERAL FEATURES:         NC1744.2
002300*              RELATIONAL OPERATORS                               NC1744.2
002400*              CLASS CONDITIONS                                   NC1744.2
002500*              SWITCH SETTINGS                                    NC1744.2
002600*                                                                 NC1744.2
002700 ENVIRONMENT DIVISION.                                            NC1744.2
002800 CONFIGURATION SECTION.                                           NC1744.2
002900 SOURCE-COMPUTER.                                                 NC1744.2
003000     XXXXX082.                                                    NC1744.2
003100 OBJECT-COMPUTER.                                                 NC1744.2
003200     XXXXX083.                                                    NC1744.2
003300ASPECIAL-NAMES.                                                   NC1744.2
003400A    XXXXX051                                                     NC1744.2
003500A    IS SW-1                                                      NC1744.2
003600A        ON STATUS IS ON-SWITCH-1                                 NC1744.2
003700A        OFF STATUS IS OFF-SWITCH-1                               NC1744.2
003800A    XXXXX052                                                     NC1744.2
003900A    IS SW-2                                                      NC1744.2
004000A          ON IS ON-SWITCH-2                                      NC1744.2
004100A          OFF IS OFF-SWITCH-2                                    NC1744.2
004200     CLASS   ORDINAL-A-ONLY IS                                    NC1744.2
004300     XXXXX090                                                     NC1744.2
004400     CLASS   ORDINAL-A-THROUGH-D IS                               NC1744.2
004500     XXXXX090                                                     NC1744.2
004600     THROUGH                                                      NC1744.2
004700     XXXXX091                                                     NC1744.2
004800     CLASS   ORDINAL-D-THRU-A                                     NC1744.2
004900     XXXXX091                                                     NC1744.2
005000     THRU                                                         NC1744.2
005100     XXXXX090                                                     NC1744.2
005200     CLASS   ACTUAL-A-ONLY "A"                                    NC1744.2
005300     CLASS   ACTUAL-A-THRU-D IS "A" THRU "D"                      NC1744.2
005400     CLASS   ACTUAL-D-THROUGH-A IS "D" THROUGH "A"                NC1744.2
005500     CLASS   ACTUAL-ABCD "ABCD".                                  NC1744.2
005600 INPUT-OUTPUT SECTION.                                            NC1744.2
005700 FILE-CONTROL.                                                    NC1744.2
005800     SELECT PRINT-FILE ASSIGN TO                                  NC1744.2
005900     XXXXX055.                                                    NC1744.2
006000 DATA DIVISION.                                                   NC1744.2
006100 FILE SECTION.                                                    NC1744.2
006200 FD  PRINT-FILE.                                                  NC1744.2
006300 01  PRINT-REC PICTURE X(120).                                    NC1744.2
006400 01  DUMMY-RECORD PICTURE X(120).                                 NC1744.2
006500 WORKING-STORAGE SECTION.                                         NC1744.2
006600 01  WS-A                        PIC X.                           NC1744.2
006700 01  WS-B                        PIC X(5).                        NC1744.2
006800 01  IF-D1                              PICTURE IS S9(4)V9(2)     NC1744.2
006900     VALUE IS 0.                                                  NC1744.2
007000 01  IF-D2                              PICTURE IS S9(4)V9(2)     NC1744.2
007100     VALUE IS ZERO.                                               NC1744.2
007200 01  IF-D3                              PICTURE IS X(10)          NC1744.2
007300     VALUE IS "0000000000".                                       NC1744.2
007400 01  IF-D4                              PICTURE IS X(15)          NC1744.2
007500     VALUE IS "               ".                                  NC1744.2
007600 01  IF-D6                              PICTURE IS A(10)          NC1744.2
007700     VALUE IS "BABABABABA".                                       NC1744.2
007800 01  IF-D7                              PICTURE IS S9(6)V9(4)     NC1744.2
007900     VALUE IS +123.45.                                            NC1744.2
008000 01  IF-D8                              PICTURE IS 9(6)V9(4)      NC1744.2
008100     VALUE IS 12300.                                              NC1744.2
008200 01  IF-D9                              PICTURE IS X(3)           NC1744.2
008300     VALUE IS "123".                                              NC1744.2
008400 01  IF-D11                             PICTURE IS X(6)           NC1744.2
008500     VALUE IS "ABCDEF".                                           NC1744.2
008600 01  IF-D13                             PICTURE IS 9(6)V9(4)      NC1744.2
008700     VALUE IS 12300.                                              NC1744.2
008800 01  IF-D14                             PICTURE IS S9(4)V9(2)     NC1744.2
008900     VALUE IS +123.45.                                            NC1744.2
009000 01  IF-D15                             PICTURE IS S999PP         NC1744.2
009100     VALUE IS 12300.                                              NC1744.2
009200 01  IF-D16                             PICTURE IS PP99           NC1744.2
009300     VALUE IS .0012.                                              NC1744.2
009400 01  IF-D17                             PICTURE IS SV9(4)         NC1744.2
009500     VALUE IS .0012.                                              NC1744.2
009600 01  IF-D18                             PICTURE IS X(10)          NC1744.2
009700     VALUE IS "BABABABABA".                                       NC1744.2
009800 01  IF-D19                             PICTURE IS X(10)          NC1744.2
009900     VALUE IS "ABCDEF    ".                                       NC1744.2
010000 01  IF-D23                             PICTURE IS $9,9B9.90+.    NC1744.2
010100 01  IF-D24                             PICTURE IS X(10)          NC1744.2
010200     VALUE IS "$1,2 3.40+".                                       NC1744.2
010300 01  IF-D25                             PICTURE IS ABABX0A.       NC1744.2
010400 01  IF-D26  PIC X(7)                                             NC1744.2
010500     VALUE IS "A C D0E".                                          NC1744.2
010600 01  IF-D27             PICTURE 9(6)V9(4)  VALUE 2137.45          NC1744.2
010700     USAGE IS COMPUTATIONAL.                                      NC1744.2
010800 01  IF-D28                             PICTURE IS 999999V9999    NC1744.2
010900     VALUE IS 2137.45.                                            NC1744.2
011000 01  IF-D32                             PICTURE IS 9 VALUE IS 0.  NC1744.2
011100 01  IF-D33 PICTURE S9 VALUE -0.                                  NC1744.2
011200 01  IF-D34 PICTURE S9 VALUE +0.                                  NC1744.2
011300 01  IF-D37             PICTURE 9(5)  VALUE 0001234.              NC1744.2
011400 01  IF-D38             PICTURE X(20) VALUE " BABBAGE".           NC1744.2
011500 01  ALPHA-UPPER        PIC X(20)     VALUE " UPPERCASE CHARS".   NC1744.2
011600 01  ALPHA-LOWER        PIC X(20)     VALUE " lowercase chars".   NC1744.2
011700 01  NON-COBOL-CHARACTERS  PICTURE X(8) VALUE                     NC1744.2
011800     XXXXX081.                                                    NC1744.2
011900 01  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1744.2
012000 01  A18ONES-DS-18V00            PICTURE S9(18)                   NC1744.2
012100                                 VALUE 111111111111111111.        NC1744.2
012200 01  ONES-XN-00018               PICTURE X(18)                    NC1744.2
012300     VALUE "111111111111111111".                                  NC1744.2
012400 01  A99-DS-02V00                PICTURE S99  VALUE 99.           NC1744.2
012500 01  WRK-DU-02V00                PICTURE 99.                      NC1744.2
012600 01  TWOS-XN-00002               PICTURE XX   VALUE "22".         NC1744.2
012700 01  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1744.2
012800                                 VALUE 111111111.111111111.       NC1744.2
012900 01  ONES-XN-00002               PICTURE XX   VALUE "11".         NC1744.2
013000 01  A02TWOS-DU-02V00            PICTURE 99   VALUE 22.           NC1744.2
013100 01  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1744.2
013200 01  A990-DS-0201P               PICTURE S99P  VALUE +990.        NC1744.2
013300 01  XDATA-XN-00018              PICTURE X(18)                    NC1744.2
013400                                 VALUE "00ABCDEFGHI  4321 ".      NC1744.2
013500 01  XDATA-DS-18V00-S REDEFINES XDATA-XN-00018 PICTURE S9(18).    NC1744.2
013600 01  YADATA-XN-00010             PICTURE X(10) VALUE "ABCDEFGHIJ".NC1744.2
013700 01  YADATA-XN-00010-U-AND-L     PICTURE X(10) VALUE "AbCdEfGhIj".NC1744.2
013800 01  DUMMY-DS-00001     PICTURE S9 VALUE -1.                      NC1744.2
013900 01  A02TWOS-DS-03V02            PICTURE S999V99  VALUE +022.00.  NC1744.2
014000 01  WRK-DS-18V0-1               PIC S9(18)     VALUE             NC1744.2
014100            -123456789012345678.                                  NC1744.2
014200 01  WRK-XN-18-2                 PIC  X(18)     VALUE             NC1744.2
014300            "123456789012345678".                                 NC1744.2
014400                                                                  NC1744.2
014500 01  IF-D10.                                                      NC1744.2
014600     02 FILLER          PICTURE XX VALUE "01".                    NC1744.2
014700     02 FILLER          PICTURE XX VALUE "23".                    NC1744.2
014800     02 IF-D10A.                                                  NC1744.2
014900       03 FILLER        PICTURE XXXX VALUE "4567".                NC1744.2
015000       03 FILLER        PICTURE XXXX VALUE "8912".                NC1744.2
015100 01  IF-D12.                                                      NC1744.2
015200     02 FILLER          PICTURE XXX VALUE "ABC".                  NC1744.2
015300     02 IF-D12A.                                                  NC1744.2
015400       03 IF-D12B.                                                NC1744.2
015500         04 FILLER      PICTURE XX VALUE "DE".                    NC1744.2
015600         04 FILLER      PICTURE X  VALUE "F".                     NC1744.2
015700 01  IF-D20.                                                      NC1744.2
015800     02 FILLER          PICTURE 9(5) VALUE ZERO.                  NC1744.2
015900     02 FILLER          PICTURE 99   VALUE 12.                    NC1744.2
016000     02 FILLER          PICTURE 9    VALUE 3.                     NC1744.2
016100     02 FILLER          PICTURE 99   VALUE 45.                    NC1744.2
016200 01  IF-D21.                                                      NC1744.2
016300     02 FILLER          PICTURE 9(5) VALUE ZERO.                  NC1744.2
016400     02 FILLER          PICTURE 9(5) VALUE 12345.                 NC1744.2
016500 01  IF-D22.                                                      NC1744.2
016600     02 FILLER          PICTURE AA   VALUE "AB".                  NC1744.2
016700     02 FILLER          PICTURE AAAA VALUE "CDEF".                NC1744.2
016800 01  IF-D35.                                                      NC1744.2
016900     02 IF-D35A                             VALUE "*ASTERISK".    NC1744.2
017000       03 FILLER        PICTURE A(6).                             NC1744.2
017100       03 FILLER        PICTURE AAA.                              NC1744.2
017200     02 IF-D35B                            VALUE "/SLASH".        NC1744.2
017300       03 FILLER        PICTURE 9(6).                             NC1744.2
017400 01  IF-D36 REDEFINES IF-D35.                                     NC1744.2
017500     02 IF-D36A         PICTURE X(6).                             NC1744.2
017600     02 IF-D36B         PICTURE XXX.                              NC1744.2
017700     02 IF-D36C         PICTURE X(6).                             NC1744.2
017800 01  IF-D39.                                                      NC1744.2
017900     02  FILLER   PICTURE A(6) VALUE "ABCDEF".                    NC1744.2
018000     02  FILLER  PICTURE A(4) VALUE SPACE.                        NC1744.2
018100 01  LEVEL-01.                                                    NC1744.2
018200     02 LEVEL-02.                                                 NC1744.2
018300     03 LEVEL-03.                                                 NC1744.2
018400     04 LEVEL-04.                                                 NC1744.2
018500     05 LEVEL-05.                                                 NC1744.2
018600     06 LEVEL-06.                                                 NC1744.2
018700     07 LEVEL-07.                                                 NC1744.2
018800     08 LEVEL-08.                                                 NC1744.2
018900     09 LEVEL-09.                                                 NC1744.2
019000     10 LEVEL-10                        PICTURE IS X VALUE IS "R".NC1744.2
019100 01  LEVEL-RECEIVER                     PICTURE IS X VALUE IS     NC1744.2
019200     SPACE.                                                       NC1744.2
019300 01  LEVEL-SENDER PICTURE X VALUE "S".                            NC1744.2
019400 01  VAL                                PICTURE IS 9 VALUE IS 0.  NC1744.2
019500 01  A-2                                PICTURE IS A VALUE IS "A".NC1744.2
019600 01  N-27                               PICTURE IS 9999V9         NC1744.2
019700     VALUE IS 9999.9.                                             NC1744.2
019800 01  N-30                               PICTURE IS 9V9            NC1744.2
019900     VALUE IS 2.                                                  NC1744.2
020000 01  N-31                               PICTURE IS 9(6).          NC1744.2
020100 01  X-32 REDEFINES N-31                PICTURE IS X(6).          NC1744.2
020200 01  N-33                               PICTURE IS 9(5)           NC1744.2
020300     VALUE IS 29.                                                 NC1744.2
020400 01  A-37                               PICTURE IS A VALUE IS "X".NC1744.2
020500 01  X-38 REDEFINES A-37                PICTURE IS X.             NC1744.2
020600 01  X-43 PIC X(10) VALUE "    l75.63".                           NC1744.2
020700 01  N-84                               PICTURE IS 9999999999.    NC1744.2
020800 01  NUMERIC-GRP-TEST.                                            NC1744.2
020900     02  NUMERIC-1                PICTURE 9 VALUE 0.              NC1744.2
021000     02  NUMERIC-2.                                               NC1744.2
021100         03  NUMERIC-3            PICTURE 9(1)V9(1) VALUE ZERO.   NC1744.2
021200         03  NUMERIC-4.                                           NC1744.2
021300             04  NUMERIC-5       PICTURE 9(18) VALUE 1.           NC1744.2
021400     02  NUMERIC-6.                                               NC1744.2
021500         03  NUMERIC-7            PICTURE X VALUE "7".            NC1744.2
021600         03  NUMERIC-8            PICTURE 9  VALUE 8.             NC1744.2
021700 01  NUM-GRP.                                                     NC1744.2
021800     02  NUM-SUB-GRP  PIC 9.                                      NC1744.2
021900 01  GROUP-1000.                                                  NC1744.2
022000     02  FILLER  PIC X.                                           NC1744.2
022100     02  GROUP-X1000.                                             NC1744.2
022200         03  GROUP-1000-1 PIC X(500) VALUE ZERO.                  NC1744.2
022300         03  XNAME        PICTURE X(100) VALUE QUOTE.             NC1744.2
022400         03  GROUP-1000-2 PICTURE X(399) VALUE SPACE.             NC1744.2
022500         03  GROUP-1000-3 PICTURE X VALUE ".".                    NC1744.2
022600     02  GROUP-X500-2.                                            NC1744.2
022700         03  GROUP-X500-A        PICTURE X(500) VALUE ZERO.       NC1744.2
022800         03  GROUP-X500-1.                                        NC1744.2
022900             04  GROUP-X500-1-1  PICTURE X(50) VALUE QUOTE.       NC1744.2
023000             04  GROUP-X500-1-2  PICTURE X(50) VALUE QUOTE.       NC1744.2
023100             04  GROUP-X500-1-3  PICTURE X(398) VALUE SPACE.      NC1744.2
023200             04  GROUP-X500-1-4  PICTURE XX VALUE " .".           NC1744.2
023300 01  HI-LO-VALUES.                                                NC1744.2
023400     02  LOW-VAL  PIC X VALUE LOW-VALUE.                          NC1744.2
023500     02 ZERO-01  PICTURE 9(18) VALUE 1.                           NC1744.2
023600     02  ABC      PICTURE XXX VALUE "ABC".                        NC1744.2
023700     02  NINE-17-8 PICTURE 9(18) VALUE 999999999999999998.        NC1744.2
023800     02  ZERO-NULL PIC 9(9) VALUE 0.                              NC1744.2
023900     02  ZERO-ZERO PICTURE 9(9)V9(9) VALUE 0.0.                   NC1744.2
024000 01  COMP-DATA.                                                   NC1744.2
024100     02  COMP-DATA1 PICTURE 9(18) COMPUTATIONAL VALUE 300.        NC1744.2
024200     02  COMP-DATA2  PICTURE 9(10) COMPUTATIONAL VALUE  100000.   NC1744.2
024300     02  COMP-DATA3  PICTURE 9     COMPUTATIONAL VALUE 9.         NC1744.2
024400     02  COMP-DATA4  PICTURE 9(9)V9(7) COMPUTATIONAL VALUE 3.3.   NC1744.2
024500     02  COMP-DATA5  PICTURE 9(5)V9(2) COMPUTATIONAL VALUE 52.25. NC1744.2
024600     02  COMP-DATA6  PICTURE 9V9       COMPUTATIONAL VALUE 8.8.   NC1744.2
024700     02  COMP-DATA7  PICTURE 9(3)V9(2) COMPUTATIONAL VALUE 300.00.NC1744.2
024800     02  COMP-DATA8  PICTURE 9V9(9) COMPUTATIONAL VALUE 3.3000000.NC1744.2
024900     02  COMP-DATA9  PICTURE 9(8)  COMPUTATIONAL VALUE 100000.    NC1744.2
025000 01  DISP-DATA.                                                   NC1744.2
025100     02  DISP-DATA1  PICTURE 9(18) VALUE 300.                     NC1744.2
025200     02  DISP-DATA2  PICTURE 9(8)  VALUE 100000.                  NC1744.2
025300     02  DISP-DATA3  PICTURE 9     VALUE 9.                       NC1744.2
025400     02  DISP-DATA4  PICTURE 9(7)V9(9) VALUE 3.3.                 NC1744.2
025500     02  DISP-DATA5  PICTURE 9(2)V9(2) VALUE 52.25.               NC1744.2
025600     02  DISP-DATA6  PICTURE 9V9   VALUE 8.8.                     NC1744.2
025700 01  DATA-5          PICTURE 9     VALUE 5.                       NC1744.2
025800 01  DATA-99999      PICTURE S9(5) VALUE +99999.                  NC1744.2
025900 01  DATA-Z          PICTURE X     VALUE "Z".                     NC1744.2
026000 01  DATA-4          PICTURE 9     VALUE 4.                       NC1744.2
026100 01  DATA-Y          PICTURE X     VALUE "Y".                     NC1744.2
026200 01  DATA-VWXYZ      PICTURE X(5)  VALUE "VWXYZ".                 NC1744.2
026300 01  DATA-ADCBA      PICTURE X(5)  VALUE "ADCBA".                 NC1744.2
026400 01  TEST-RESULTS.                                                NC1744.2
026500     02 FILLER                   PIC X      VALUE SPACE.          NC1744.2
026600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1744.2
026700     02 FILLER                   PIC X      VALUE SPACE.          NC1744.2
026800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1744.2
026900     02 FILLER                   PIC X      VALUE SPACE.          NC1744.2
027000     02  PAR-NAME.                                                NC1744.2
027100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1744.2
027200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1744.2
027300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1744.2
027400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1744.2
027500     02 RE-MARK                  PIC X(61).                       NC1744.2
027600 01  TEST-COMPUTED.                                               NC1744.2
027700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1744.2
027800     02 FILLER                   PIC X(17)  VALUE                 NC1744.2
027900            "       COMPUTED=".                                   NC1744.2
028000     02 COMPUTED-X.                                               NC1744.2
028100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1744.2
028200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1744.2
028300                                 PIC -9(9).9(9).                  NC1744.2
028400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1744.2
028500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1744.2
028600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1744.2
028700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1744.2
028800         04 COMPUTED-18V0                    PIC -9(18).          NC1744.2
028900         04 FILLER                           PIC X.               NC1744.2
029000     03 FILLER PIC X(50) VALUE SPACE.                             NC1744.2
029100 01  TEST-CORRECT.                                                NC1744.2
029200     02 FILLER PIC X(30) VALUE SPACE.                             NC1744.2
029300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1744.2
029400     02 CORRECT-X.                                                NC1744.2
029500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1744.2
029600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1744.2
029700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1744.2
029800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1744.2
029900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1744.2
030000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1744.2
030100         04 CORRECT-18V0                     PIC -9(18).          NC1744.2
030200         04 FILLER                           PIC X.               NC1744.2
030300     03 FILLER PIC X(2) VALUE SPACE.                              NC1744.2
030400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1744.2
030500 01  CCVS-C-1.                                                    NC1744.2
030600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1744.2
030700-    "SS  PARAGRAPH-NAME                                          NC1744.2
030800-    "       REMARKS".                                            NC1744.2
030900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1744.2
031000 01  CCVS-C-2.                                                    NC1744.2
031100     02 FILLER                     PIC X        VALUE SPACE.      NC1744.2
031200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1744.2
031300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1744.2
031400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1744.2
031500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1744.2
031600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1744.2
031700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1744.2
031800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1744.2
031900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1744.2
032000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1744.2
032100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1744.2
032200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1744.2
032300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1744.2
032400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1744.2
032500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1744.2
032600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1744.2
032700 01  CCVS-H-1.                                                    NC1744.2
032800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1744.2
032900     02  FILLER                    PIC X(42)    VALUE             NC1744.2
033000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1744.2
033100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1744.2
033200 01  CCVS-H-2A.                                                   NC1744.2
033300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1744.2
033400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1744.2
033500   02  FILLER                        PIC XXXX   VALUE             NC1744.2
033600     "4.2 ".                                                      NC1744.2
033700   02  FILLER                        PIC X(28)  VALUE             NC1744.2
033800            " COPY - NOT FOR DISTRIBUTION".                       NC1744.2
033900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1744.2
034000                                                                  NC1744.2
034100 01  CCVS-H-2B.                                                   NC1744.2
034200   02  FILLER                        PIC X(15)  VALUE             NC1744.2
034300            "TEST RESULT OF ".                                    NC1744.2
034400   02  TEST-ID                       PIC X(9).                    NC1744.2
034500   02  FILLER                        PIC X(4)   VALUE             NC1744.2
034600            " IN ".                                               NC1744.2
034700   02  FILLER                        PIC X(12)  VALUE             NC1744.2
034800     " HIGH       ".                                              NC1744.2
034900   02  FILLER                        PIC X(22)  VALUE             NC1744.2
035000            " LEVEL VALIDATION FOR ".                             NC1744.2
035100   02  FILLER                        PIC X(58)  VALUE             NC1744.2
035200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1744.2
035300 01  CCVS-H-3.                                                    NC1744.2
035400     02  FILLER                      PIC X(34)  VALUE             NC1744.2
035500            " FOR OFFICIAL USE ONLY    ".                         NC1744.2
035600     02  FILLER                      PIC X(58)  VALUE             NC1744.2
035700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1744.2
035800     02  FILLER                      PIC X(28)  VALUE             NC1744.2
035900            "  COPYRIGHT   1985 ".                                NC1744.2
036000 01  CCVS-E-1.                                                    NC1744.2
036100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1744.2
036200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1744.2
036300     02 ID-AGAIN                     PIC X(9).                    NC1744.2
036400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1744.2
036500 01  CCVS-E-2.                                                    NC1744.2
036600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1744.2
036700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1744.2
036800     02 CCVS-E-2-2.                                               NC1744.2
036900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1744.2
037000         03 FILLER                   PIC X      VALUE SPACE.      NC1744.2
037100         03 ENDER-DESC               PIC X(44)  VALUE             NC1744.2
037200            "ERRORS ENCOUNTERED".                                 NC1744.2
037300 01  CCVS-E-3.                                                    NC1744.2
037400     02  FILLER                      PIC X(22)  VALUE             NC1744.2
037500            " FOR OFFICIAL USE ONLY".                             NC1744.2
037600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1744.2
037700     02  FILLER                      PIC X(58)  VALUE             NC1744.2
037800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1744.2
037900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1744.2
038000     02 FILLER                       PIC X(15)  VALUE             NC1744.2
038100             " COPYRIGHT 1985".                                   NC1744.2
038200 01  CCVS-E-4.                                                    NC1744.2
038300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1744.2
038400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1744.2
038500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1744.2
038600     02 FILLER                       PIC X(40)  VALUE             NC1744.2
038700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1744.2
038800 01  XXINFO.                                                      NC1744.2
038900     02 FILLER                       PIC X(19)  VALUE             NC1744.2
039000            "*** INFORMATION ***".                                NC1744.2
039100     02 INFO-TEXT.                                                NC1744.2
039200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1744.2
039300       04 XXCOMPUTED                 PIC X(20).                   NC1744.2
039400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1744.2
039500       04 XXCORRECT                  PIC X(20).                   NC1744.2
039600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1744.2
039700 01  HYPHEN-LINE.                                                 NC1744.2
039800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1744.2
039900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1744.2
040000-    "*****************************************".                 NC1744.2
040100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1744.2
040200-    "******************************".                            NC1744.2
040300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1744.2
040400     "NC174A".                                                    NC1744.2
040500 PROCEDURE DIVISION.                                              NC1744.2
040600 CCVS1 SECTION.                                                   NC1744.2
040700 OPEN-FILES.                                                      NC1744.2
040800     OPEN     OUTPUT PRINT-FILE.                                  NC1744.2
040900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1744.2
041000     MOVE    SPACE TO TEST-RESULTS.                               NC1744.2
041100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1744.2
041200     GO TO CCVS1-EXIT.                                            NC1744.2
041300 CLOSE-FILES.                                                     NC1744.2
041400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1744.2
041500 TERMINATE-CCVS.                                                  NC1744.2
041600*    EXIT PROGRAM.                                                NC1744.2
041700*TERMINATE-CALL.                                                  NC1744.2
041800     STOP     RUN.                                                NC1744.2
041900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1744.2
042000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1744.2
042100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1744.2
042200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1744.2
042300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1744.2
042400 PRINT-DETAIL.                                                    NC1744.2
042500     IF REC-CT NOT EQUAL TO ZERO                                  NC1744.2
042600             MOVE "." TO PARDOT-X                                 NC1744.2
042700             MOVE REC-CT TO DOTVALUE.                             NC1744.2
042800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1744.2
042900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1744.2
043000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1744.2
043100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1744.2
043200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1744.2
043300     MOVE SPACE TO CORRECT-X.                                     NC1744.2
043400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1744.2
043500     MOVE     SPACE TO RE-MARK.                                   NC1744.2
043600 HEAD-ROUTINE.                                                    NC1744.2
043700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1744.2
043800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1744.2
043900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1744.2
044000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1744.2
044100 COLUMN-NAMES-ROUTINE.                                            NC1744.2
044200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1744.2
044300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1744.2
044400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1744.2
044500 END-ROUTINE.                                                     NC1744.2
044600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1744.2
044700 END-RTN-EXIT.                                                    NC1744.2
044800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1744.2
044900 END-ROUTINE-1.                                                   NC1744.2
045000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1744.2
045100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1744.2
045200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1744.2
045300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1744.2
045400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1744.2
045500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1744.2
045600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1744.2
045700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1744.2
045800  END-ROUTINE-12.                                                 NC1744.2
045900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1744.2
046000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1744.2
046100         MOVE "NO " TO ERROR-TOTAL                                NC1744.2
046200         ELSE                                                     NC1744.2
046300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1744.2
046400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1744.2
046500     PERFORM WRITE-LINE.                                          NC1744.2
046600 END-ROUTINE-13.                                                  NC1744.2
046700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1744.2
046800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1744.2
046900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1744.2
047000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1744.2
047100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1744.2
047200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1744.2
047300          MOVE "NO " TO ERROR-TOTAL                               NC1744.2
047400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1744.2
047500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1744.2
047600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1744.2
047700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1744.2
047800 WRITE-LINE.                                                      NC1744.2
047900     ADD 1 TO RECORD-COUNT.                                       NC1744.2
048000*    IF RECORD-COUNT GREATER 42                                   NC1744.2
048100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1744.2
048200*        MOVE SPACE TO DUMMY-RECORD                               NC1744.2
048300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1744.2
048400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1744.2
048500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1744.2
048600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1744.2
048700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1744.2
048800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1744.2
048900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1744.2
049000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1744.2
049100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1744.2
049200*        MOVE ZERO TO RECORD-COUNT.                               NC1744.2
049300     PERFORM WRT-LN.                                              NC1744.2
049400 WRT-LN.                                                          NC1744.2
049500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1744.2
049600     MOVE SPACE TO DUMMY-RECORD.                                  NC1744.2
049700 BLANK-LINE-PRINT.                                                NC1744.2
049800     PERFORM WRT-LN.                                              NC1744.2
049900 FAIL-ROUTINE.                                                    NC1744.2
050000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1744.2
050100            GO TO FAIL-ROUTINE-WRITE.                             NC1744.2
050200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1744.2
050300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1744.2
050400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1744.2
050500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1744.2
050600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1744.2
050700     GO TO  FAIL-ROUTINE-EX.                                      NC1744.2
050800 FAIL-ROUTINE-WRITE.                                              NC1744.2
050900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1744.2
051000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1744.2
051100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1744.2
051200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1744.2
051300 FAIL-ROUTINE-EX. EXIT.                                           NC1744.2
051400 BAIL-OUT.                                                        NC1744.2
051500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1744.2
051600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1744.2
051700 BAIL-OUT-WRITE.                                                  NC1744.2
051800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1744.2
051900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1744.2
052000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1744.2
052100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1744.2
052200 BAIL-OUT-EX. EXIT.                                               NC1744.2
052300 CCVS1-EXIT.                                                      NC1744.2
052400     EXIT.                                                        NC1744.2
052500 SECT-NC174A-001 SECTION.                                         NC1744.2
052600*                                                                 NC1744.2
052700*                                                                 NC1744.2
052800 NEXT-INIT-GF-1.                                                  NC1744.2
052900*    ==-->     NEXT SENTENCE           <--==                      NC1744.2
053000     MOVE   "V1-89 6.15.4 GR2 " TO ANSI-REFERENCE.                NC1744.2
053100     MOVE    "A" TO A-2.                                          NC1744.2
053200 NEXT-TEST-GF-1.                                                  NC1744.2
053300     IF       A-2 EQUAL TO "A"                                    NC1744.2
053400              NEXT SENTENCE                                       NC1744.2
053500              ELSE                                                NC1744.2
053600              NEXT SENTENCE.                                      NC1744.2
053700     PERFORM  PASS.                                               NC1744.2
053800     GO TO    NEXT-WRITE-GF-1.                                    NC1744.2
053900 NEXT-DELETE-GF-1.                                                NC1744.2
054000     PERFORM  DE-LETE.                                            NC1744.2
054100 NEXT-WRITE-GF-1.                                                 NC1744.2
054200     MOVE "NEXT-TEST-1" TO PAR-NAME.                              NC1744.2
054300     PERFORM  PRINT-DETAIL.                                       NC1744.2
054400*                                                                 NC1744.2
054500*                                                                 NC1744.2
054600 ANOTHER-REMARK.                                                  NC1744.2
054700     MOVE     SPACE TO TEST-RESULTS.                              NC1744.2
054800     MOVE "THE FOLLOWING TESTS        " TO RE-MARK.               NC1744.2
054900     PERFORM  PRINT-DETAIL.                                       NC1744.2
055000     MOVE "TEST THE COMPARISONS IN    " TO RE-MARK.               NC1744.2
055100     PERFORM  PRINT-DETAIL.                                       NC1744.2
055200     MOVE "SWITCH-STATUS, RELATION    " TO RE-MARK.               NC1744.2
055300     PERFORM  PRINT-DETAIL.                                       NC1744.2
055400     MOVE "AND CLASS CONDITIONALS.    " TO RE-MARK.               NC1744.2
055500     PERFORM  PRINT-DETAIL.                                       NC1744.2
055600 SWH-INIT-GF-1.                                                   NC1744.2
055700     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC1744.2
055800     MOVE "SWITCH-STATUS" TO FEATURE.                             NC1744.2
055900 SWH-TEST-GF-1.                                                   NC1744.2
056000A    IF      ON-SWITCH-1                                          NC1744.2
056100A            PERFORM PASS                                         NC1744.2
056200A    ELSE                                                         NC1744.2
056300A            PERFORM FAIL.                                        NC1744.2
056400A    GO TO   SWH-WRITE-GF-1.                                      NC1744.2
056500 SWH-DELETE-GF-1.                                                 NC1744.2
056600B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
056700     PERFORM  DE-LETE.                                            NC1744.2
056800 SWH-WRITE-GF-1.                                                  NC1744.2
056900     MOVE "SWH-TEST-GF-1" TO PAR-NAME.                            NC1744.2
057000     PERFORM  PRINT-DETAIL.                                       NC1744.2
057100 SWH-INIT-GF-2.                                                   NC1744.2
057200     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC1744.2
057300 SWH-TEST-GF-2.                                                   NC1744.2
057400A    IF       OFF-SWITCH-1                                        NC1744.2
057500A             PERFORM FAIL                                        NC1744.2
057600A             ELSE                                                NC1744.2
057700A             PERFORM PASS.                                       NC1744.2
057800A    GO TO    SWH-WRITE-GF-2.                                     NC1744.2
057900 SWH-DELETE-GF-2.                                                 NC1744.2
058000B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
058100     PERFORM  DE-LETE.                                            NC1744.2
058200 SWH-WRITE-GF-2.                                                  NC1744.2
058300     MOVE "SWH-TEST-GF-2" TO PAR-NAME.                            NC1744.2
058400     PERFORM  PRINT-DETAIL.                                       NC1744.2
058500 SWH-INIT-GF-3.                                                   NC1744.2
058600     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC1744.2
058700 SWH-TEST-GF-3.                                                   NC1744.2
058800A    IF       OFF-SWITCH-2                                        NC1744.2
058900A             PERFORM PASS                                        NC1744.2
059000A             ELSE                                                NC1744.2
059100A             PERFORM FAIL.                                       NC1744.2
059200A    GO TO    SWH-WRITE-GF-3.                                     NC1744.2
059300 SWH-DELETE-GF-3.                                                 NC1744.2
059400B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
059500     PERFORM  DE-LETE.                                            NC1744.2
059600 SWH-WRITE-GF-3.                                                  NC1744.2
059700     MOVE "SWH-TEST-GF-3" TO PAR-NAME.                            NC1744.2
059800     PERFORM  PRINT-DETAIL.                                       NC1744.2
059900 SWH-INIT-GF-4.                                                   NC1744.2
060000     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC1744.2
060100 SWH-TEST-GF-4.                                                   NC1744.2
060200A    IF       ON-SWITCH-2                                         NC1744.2
060300A             PERFORM FAIL                                        NC1744.2
060400A             ELSE                                                NC1744.2
060500A             PERFORM PASS.                                       NC1744.2
060600A    GO TO    SWH-WRITE-GF-4.                                     NC1744.2
060700 SWH-DELETE-GF-4.                                                 NC1744.2
060800B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
060900     PERFORM  DE-LETE.                                            NC1744.2
061000 SWH-WRITE-GF-4.                                                  NC1744.2
061100     MOVE "SWH-TEST-GF-4" TO PAR-NAME.                            NC1744.2
061200     PERFORM  PRINT-DETAIL.                                       NC1744.2
061300 SWH-TEST-5.                                                      NC1744.2
061400* DELETE THE NEXT LINE TO DELETE THIS TEST                        NC1744.2
061500     GO TO SWH-TEST-5-B.                                          NC1744.2
061600 SWH-TEST-5-A.                                                    NC1744.2
061700     GO TO SWH-DELETE-5.                                          NC1744.2
061800 SWH-TEST-5-B.                                                    NC1744.2
061900A    IF NOT ON-SWITCH-1                                           NC1744.2
062000A        MOVE "SWITCH-1  OFF " TO COMPUTED-A                      NC1744.2
062100A        MOVE "SWITCH-1 EXPECTED ON" TO CORRECT-A                 NC1744.2
062200A        PERFORM FAIL                                             NC1744.2
062300A        GO TO SWH-WRITE-5.                                       NC1744.2
062400A    PERFORM PASS.                                                NC1744.2
062500A    GO TO SWH-WRITE-5.                                           NC1744.2
062600 SWH-DELETE-5.                                                    NC1744.2
062700B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
062800     PERFORM DE-LETE.                                             NC1744.2
062900 SWH-WRITE-5.                                                     NC1744.2
063000     MOVE "SWH-TEST-5" TO PAR-NAME.                               NC1744.2
063100     PERFORM PRINT-DETAIL.                                        NC1744.2
063200 SWH-TEST-6.                                                      NC1744.2
063300* DELETE THE NEXT LINE TO DELETE THIS TEST                        NC1744.2
063400     GO TO SWH-TEST-6-B.                                          NC1744.2
063500 SWH-TEST-6-A.                                                    NC1744.2
063600     GO TO SWH-DELETE-6.                                          NC1744.2
063700 SWH-TEST-6-B.                                                    NC1744.2
063800A    IF NOT OFF-SWITCH-1                                          NC1744.2
063900A        PERFORM PASS                                             NC1744.2
064000A        GO TO SWH-WRITE-6.                                       NC1744.2
064100A    MOVE "SWITCH-1  OFF " TO COMPUTED-A.                         NC1744.2
064200A    MOVE "SWITCH-1 EXPECTED ON" TO CORRECT-A.                    NC1744.2
064300A    PERFORM FAIL.                                                NC1744.2
064400A    GO TO SWH-WRITE-6.                                           NC1744.2
064500 SWH-DELETE-6.                                                    NC1744.2
064600B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
064700     PERFORM DE-LETE.                                             NC1744.2
064800 SWH-WRITE-6.                                                     NC1744.2
064900     MOVE "SWH-TEST-6" TO PAR-NAME.                               NC1744.2
065000     PERFORM PRINT-DETAIL.                                        NC1744.2
065100 SWH-TEST-7.                                                      NC1744.2
065200     GO TO SWH-DELETE-7.                                          NC1744.2
065300A    IF NOT ON-SWITCH-2                                           NC1744.2
065400A        PERFORM PASS                                             NC1744.2
065500A        GO TO SWH-WRITE-7.                                       NC1744.2
065600A    MOVE "SWITCH-2  ON " TO COMPUTED-A.                          NC1744.2
065700A    MOVE "SWITCH2 EXPECTED OFF" TO CORRECT-A.                    NC1744.2
065800A    PERFORM FAIL.                                                NC1744.2
065900A    GO TO SWH-WRITE-7.                                           NC1744.2
066000 SWH-DELETE-7.                                                    NC1744.2
066100B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC1744.2
066200     PERFORM DE-LETE.                                             NC1744.2
066300 SWH-WRITE-7.                                                     NC1744.2
066400     MOVE "SWH-TEST-7" TO PAR-NAME.                               NC1744.2
066500     PERFORM PRINT-DETAIL.                                        NC1744.2
066600 SWH-TEST-8.                                                      NC1744.2
066700* DELETE THE NEXT LINE TO DELETE THIS TEST                        NC1744.2
066800     GO TO SWH-TEST-8-B.                                          NC1744.2
066900 SWH-TEST-8-A.                                                    NC1744.2
067000     GO TO SWH-DELETE-8.                                          NC1744.2
067100 SWH-TEST-8-B.                                                    NC1744.2
067200A    IF NOT OFF-SWITCH-2                                          NC1744.2
067300A        MOVE "SWITCH-2  ON " TO COMPUTED-A                       NC1744.2
067400A        MOVE "SWITCH2 EXPECTED OFF" TO CORRECT-A                 NC1744.2
067500A        PERFORM FAIL                                             NC1744.2
067600A        GO TO SWH-WRITE-8.                                       NC1744.2
067700A    PERFORM PASS.                                                NC1744.2
067800A    GO TO  SWH-WRITE-8.                                          NC1744.2
067900 SWH-DELETE-8.                                                    NC1744.2
068000B    MOVE  "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                 NC1744.2
068100     PERFORM DE-LETE.                                             NC1744.2
068200 SWH-WRITE-8.                                                     NC1744.2
068300     MOVE  "SWH-TEST-8" TO PAR-NAME.                              NC1744.2
068400     PERFORM PRINT-DETAIL.                                        NC1744.2
068500*                                                                 NC1744.2
068600*                                                                 NC1744.2
068700 SWH-INIT-GF-9.                                                   NC1744.2
068800     MOVE   "SET SWITCH ON/OFF" TO FEATURE.                       NC1744.2
068900     MOVE   "V1-126 6.22.1(2)"  TO ANSI-REFERENCE.                NC1744.2
069000     SET     SW-1 TO ON.                                          NC1744.2
069100 SWH-TEST-GF-9.                                                   NC1744.2
069200A    IF      ON-SWITCH-1                                          NC1744.2
069300A            PERFORM PASS                                         NC1744.2
069400A    ELSE                                                         NC1744.2
069500A            PERFORM FAIL.                                        NC1744.2
069600A    GO TO   SWH-WRITE-GF-9.                                      NC1744.2
069700 SWH-DELETE-GF-9.                                                 NC1744.2
069800B    MOVE   "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                NC1744.2
069900     PERFORM DE-LETE.                                             NC1744.2
070000 SWH-WRITE-GF-9.                                                  NC1744.2
070100     MOVE   "SWH-TEST-GF-9" TO PAR-NAME.                          NC1744.2
070200     PERFORM PRINT-DETAIL.                                        NC1744.2
070300*                                                                 NC1744.2
070400*                                                                 NC1744.2
070500 SWH-INIT-GF-10.                                                  NC1744.2
070600     MOVE   "SET SWITCH ON/OFF" TO FEATURE.                       NC1744.2
070700     MOVE   "V1-126 6.22.1(2)"  TO ANSI-REFERENCE.                NC1744.2
070800     SET     SW-1 SW-2 TO OFF.                                    NC1744.2
070900 SWH-TEST-GF-10-1.                                                NC1744.2
071000A    IF      OFF-SWITCH-1                                         NC1744.2
071100A            PERFORM PASS                                         NC1744.2
071200A    ELSE                                                         NC1744.2
071300A            PERFORM FAIL.                                        NC1744.2
071400A    GO TO   SWH-WRITE-GF-10-1.                                   NC1744.2
071500 SWH-DELETE-GF-10-1.                                              NC1744.2
071600B    MOVE   "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                NC1744.2
071700     PERFORM DE-LETE.                                             NC1744.2
071800 SWH-WRITE-GF-10-1.                                               NC1744.2
071900     MOVE   "SWH-TEST-GF-10-1" TO PAR-NAME.                       NC1744.2
072000     PERFORM PRINT-DETAIL.                                        NC1744.2
072100 SWH-TEST-GF-10-2.                                                NC1744.2
072200A    IF      OFF-SWITCH-2                                         NC1744.2
072300A            PERFORM PASS                                         NC1744.2
072400A    ELSE                                                         NC1744.2
072500A            PERFORM FAIL.                                        NC1744.2
072600A    GO TO   SWH-WRITE-GF-10-2.                                   NC1744.2
072700 SWH-DELETE-GF-10-2.                                              NC1744.2
072800B    MOVE   "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                NC1744.2
072900     PERFORM DE-LETE.                                             NC1744.2
073000 SWH-WRITE-GF-10-2.                                               NC1744.2
073100     MOVE   "SWH-TEST-GF-10-2" TO PAR-NAME.                       NC1744.2
073200     PERFORM PRINT-DETAIL.                                        NC1744.2
073300*                                                                 NC1744.2
073400*                                                                 NC1744.2
073500 SWH-INIT-GF-11.                                                  NC1744.2
073600     MOVE   "SET SWITCH ON/OFF" TO FEATURE.                       NC1744.2
073700     MOVE   "V1-126 6.22.1(2)"  TO ANSI-REFERENCE.                NC1744.2
073800     SET     SW-1 TO ON                                           NC1744.2
073900             SW-2 TO OFF.                                         NC1744.2
074000 SWH-TEST-GF-11-1.                                                NC1744.2
074100A    IF      ON-SWITCH-1                                          NC1744.2
074200A            PERFORM PASS                                         NC1744.2
074300A    ELSE                                                         NC1744.2
074400A            PERFORM FAIL.                                        NC1744.2
074500A    GO TO   SWH-WRITE-GF-11-1.                                   NC1744.2
074600 SWH-DELETE-GF-11-1.                                              NC1744.2
074700B    MOVE   "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                NC1744.2
074800     PERFORM DE-LETE.                                             NC1744.2
074900 SWH-WRITE-GF-11-1.                                               NC1744.2
075000     MOVE   "SWH-TEST-GF-11-1" TO PAR-NAME.                       NC1744.2
075100     PERFORM PRINT-DETAIL.                                        NC1744.2
075200 SWH-TEST-GF-11-2.                                                NC1744.2
075300A    IF      OFF-SWITCH-2                                         NC1744.2
075400A            PERFORM PASS                                         NC1744.2
075500A    ELSE                                                         NC1744.2
075600A            PERFORM FAIL.                                        NC1744.2
075700A    GO TO   SWH-WRITE-GF-11-2.                                   NC1744.2
075800 SWH-DELETE-GF-11-2.                                              NC1744.2
075900B    MOVE   "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                NC1744.2
076000     PERFORM DE-LETE.                                             NC1744.2
076100 SWH-WRITE-GF-11-2.                                               NC1744.2
076200     MOVE   "SWH-TEST-GF-11-2" TO PAR-NAME.                       NC1744.2
076300     PERFORM PRINT-DETAIL.                                        NC1744.2
076400*                                                                 NC1744.2
076500*                                                                 NC1744.2
076600 SWH-INIT-GF-12.                                                  NC1744.2
076700     MOVE   "SET SWITCH ON/OFF" TO FEATURE.                       NC1744.2
076800     MOVE   "V1-126 6.22.1(2)"  TO ANSI-REFERENCE.                NC1744.2
076900     SET     SW-2 TO OFF.                                         NC1744.2
077000 SWH-TEST-GF-12.                                                  NC1744.2
077100A    IF      OFF-SWITCH-2                                         NC1744.2
077200A            PERFORM PASS                                         NC1744.2
077300A    ELSE                                                         NC1744.2
077400A            PERFORM FAIL.                                        NC1744.2
077500A    GO TO   SWH-WRITE-GF-12.                                     NC1744.2
077600 SWH-DELETE-GF-12.                                                NC1744.2
077700B    MOVE   "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                NC1744.2
077800     PERFORM DE-LETE.                                             NC1744.2
077900 SWH-WRITE-GF-12.                                                 NC1744.2
078000     MOVE   "SWH-TEST-GF-12" TO PAR-NAME.                         NC1744.2
078100     PERFORM PRINT-DETAIL.                                        NC1744.2
078200*                                                                 NC1744.2
078300*                                                                 NC1744.2
078400 RELAT-INIT-GF-1.                                                 NC1744.2
078500     MOVE   "RELATION"          TO FEATURE.                       NC1744.2
078600     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
078700     MOVE     ZERO TO VAL.                                        NC1744.2
078800     MOVE    "A"   TO A-2.                                        NC1744.2
078900 RELAT-TEST-GF-1.                                                 NC1744.2
079000     IF "Z"   GREATER THAN A-2                                    NC1744.2
079100              ADD 1 VAL GIVING VAL.                               NC1744.2
079200                                                                  NC1744.2
079300     IF       A-2 GREATER THAN "Z"                                NC1744.2
079400              GO TO RELAT-CHECK-GF-1.                             NC1744.2
079500     ADD      2 VAL GIVING VAL.                                   NC1744.2
079600     GO TO    RELAT-CHECK-GF-1.                                   NC1744.2
079700 RELAT-DELETE-GF-1.                                               NC1744.2
079800     PERFORM  DE-LETE.                                            NC1744.2
079900     GO TO    RELAT-WRITE-GF-1.                                   NC1744.2
080000 RELAT-CHECK-GF-1.                                                NC1744.2
080100     IF       VAL EQUAL TO 3                                      NC1744.2
080200             PERFORM PASS                                         NC1744.2
080300             GO TO RELAT-WRITE-GF-1.                              NC1744.2
080400     MOVE VAL TO COMPUTED-A.                                      NC1744.2
080500     MOVE 3 TO CORRECT-A.                                         NC1744.2
080600     PERFORM FAIL.                                                NC1744.2
080700 RELAT-WRITE-GF-1.                                                NC1744.2
080800     MOVE "RELAT-TEST-GF-1" TO PAR-NAME.                          NC1744.2
080900     PERFORM PRINT-DETAIL.                                        NC1744.2
081000 RELAT-INIT-GF-2.                                                 NC1744.2
081100     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
081200     MOVE     ZERO TO VAL.                                        NC1744.2
081300     MOVE    "A"   TO A-2.                                        NC1744.2
081400 RELAT-TEST-GF-2.                                                 NC1744.2
081500     IF      A-2 NOT GREATER THAN "Z"                             NC1744.2
081600             ADD 1 VAL GIVING VAL.                                NC1744.2
081700     IF   "Z" NOT GREATER THAN A-2                                NC1744.2
081800             GO TO    RELAT-CHECK-GF-2.                           NC1744.2
081900     ADD     2 VAL GIVING VAL.                                    NC1744.2
082000     GO TO   RELAT-CHECK-GF-2.                                    NC1744.2
082100 RELAT-DELETE-GF-2.                                               NC1744.2
082200     PERFORM DE-LETE.                                             NC1744.2
082300     GO TO   RELAT-WRITE-GF-2.                                    NC1744.2
082400 RELAT-CHECK-GF-2.                                                NC1744.2
082500     IF       VAL EQUAL TO 3                                      NC1744.2
082600              PERFORM PASS                                        NC1744.2
082700              GO TO RELAT-WRITE-GF-2.                             NC1744.2
082800     MOVE VAL TO COMPUTED-A.                                      NC1744.2
082900     MOVE 3 TO CORRECT-A.                                         NC1744.2
083000     PERFORM  FAIL.                                               NC1744.2
083100 RELAT-WRITE-GF-2.                                                NC1744.2
083200     MOVE "RELAT-TEST-GF-2" TO PAR-NAME.                          NC1744.2
083300     PERFORM  PRINT-DETAIL.                                       NC1744.2
083400 RELAT-INIT-GF-3.                                                 NC1744.2
083500     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
083600     MOVE     ZERO TO VAL.                                        NC1744.2
083700     MOVE     2    TO N-30.                                       NC1744.2
083800     MOVE     29   TO N-33.                                       NC1744.2
083900 RELAT-TEST-GF-3.                                                 NC1744.2
084000     IF       N-30 LESS THAN N-33                                 NC1744.2
084100              ADD 1 VAL GIVING VAL.                               NC1744.2
084200     IF       N-33 LESS THAN N-30                                 NC1744.2
084300              GO TO RELAT-CHECK-GF-3.                             NC1744.2
084400     ADD      2 VAL GIVING VAL.                                   NC1744.2
084500     GO TO    RELAT-CHECK-GF-3.                                   NC1744.2
084600 RELAT-DELETE-GF-3.                                               NC1744.2
084700     PERFORM  DE-LETE.                                            NC1744.2
084800     GO TO    RELAT-WRITE-GF-3.                                   NC1744.2
084900 RELAT-CHECK-GF-3.                                                NC1744.2
085000     IF       VAL EQUAL TO 3                                      NC1744.2
085100              PERFORM PASS                                        NC1744.2
085200              GO TO RELAT-WRITE-GF-3.                             NC1744.2
085300     MOVE VAL TO COMPUTED-A.                                      NC1744.2
085400     MOVE 3 TO CORRECT-A.                                         NC1744.2
085500     PERFORM  FAIL.                                               NC1744.2
085600 RELAT-WRITE-GF-3.                                                NC1744.2
085700     MOVE "RELAT-TEST-GF-3" TO PAR-NAME.                          NC1744.2
085800     PERFORM  PRINT-DETAIL.                                       NC1744.2
085900 RELAT-INIT-GF-4.                                                 NC1744.2
086000     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
086100     MOVE     ZERO TO VAL.                                        NC1744.2
086200     MOVE     9999.9 TO N-27.                                     NC1744.2
086300 RELAT-TEST-GF-4.                                                 NC1744.2
086400     IF       5280 NOT LESS THAN N-27                             NC1744.2
086500              ADD 1 VAL GIVING VAL.                               NC1744.2
086600     IF       N-27 NOT LESS THAN 5280                             NC1744.2
086700              GO TO RELAT-CHECK-GF-4.                             NC1744.2
086800     ADD      2 VAL GIVING VAL.                                   NC1744.2
086900     GO TO    RELAT-CHECK-GF-4.                                   NC1744.2
087000 RELAT-DELETE-GF-4.                                               NC1744.2
087100     PERFORM  DE-LETE.                                            NC1744.2
087200     GO TO    RELAT-WRITE-GF-4.                                   NC1744.2
087300 RELAT-CHECK-GF-4.                                                NC1744.2
087400     IF       VAL EQUAL TO ZERO                                   NC1744.2
087500              PERFORM PASS                                        NC1744.2
087600              GO TO RELAT-WRITE-GF-4.                             NC1744.2
087700     MOVE VAL TO COMPUTED-A.                                      NC1744.2
087800     MOVE ZERO TO CORRECT-A.                                      NC1744.2
087900     PERFORM  FAIL.                                               NC1744.2
088000 RELAT-WRITE-GF-4.                                                NC1744.2
088100     MOVE "RELAT-TEST-GF-4" TO PAR-NAME.                          NC1744.2
088200     PERFORM  PRINT-DETAIL.                                       NC1744.2
088300 RELAT-INIT-GF-5.                                                 NC1744.2
088400     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
088500     MOVE     ZERO TO VAL.                                        NC1744.2
088600     MOVE     29   TO N-33.                                       NC1744.2
088700     MOVE    "    $75.63" TO X-43.                                NC1744.2
088800 RELAT-TEST-GF-5.                                                 NC1744.2
088900     MOVE     N-33 TO N-84.                                       NC1744.2
089000     IF       N-33 EQUAL TO N-84                                  NC1744.2
089100              ADD 1 VAL GIVING VAL.                               NC1744.2
089200     IF       N-84 EQUAL TO X-43                                  NC1744.2
089300              GO TO RELAT-CHECK-GF-5.                             NC1744.2
089400     ADD      2 VAL GIVING VAL.                                   NC1744.2
089500     GO TO    RELAT-CHECK-GF-5.                                   NC1744.2
089600 RELAT-DELETE-GF-5.                                               NC1744.2
089700     PERFORM  DE-LETE.                                            NC1744.2
089800     GO TO    RELAT-WRITE-GF-5.                                   NC1744.2
089900 RELAT-CHECK-GF-5.                                                NC1744.2
090000     IF       VAL EQUAL TO 3                                      NC1744.2
090100              PERFORM PASS                                        NC1744.2
090200              GO TO RELAT-WRITE-GF-5.                             NC1744.2
090300     MOVE VAL TO COMPUTED-A.                                      NC1744.2
090400     MOVE 3 TO CORRECT-A.                                         NC1744.2
090500     PERFORM  FAIL.                                               NC1744.2
090600 RELAT-WRITE-GF-5.                                                NC1744.2
090700     MOVE "RELAT-TEST-GF-5" TO PAR-NAME.                          NC1744.2
090800     PERFORM  PRINT-DETAIL.                                       NC1744.2
090900 RELAT-INIT-GF-6.                                                 NC1744.2
091000     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
091100     MOVE     ZERO TO VAL.                                        NC1744.2
091200     MOVE     29   TO N-33.                                       NC1744.2
091300     MOVE     29   TO N-84.                                       NC1744.2
091400     MOVE    "    $75.63" TO X-43.                                NC1744.2
091500 RELAT-TEST-GF-6.                                                 NC1744.2
091600     IF       N-84 NOT EQUAL TO X-43                              NC1744.2
091700              ADD 1 VAL GIVING VAL.                               NC1744.2
091800     IF       N-33 NOT EQUAL TO N-84                              NC1744.2
091900              GO TO RELAT-CHECK-GF-6.                             NC1744.2
092000     ADD      2 VAL GIVING VAL.                                   NC1744.2
092100     GO TO    RELAT-CHECK-GF-6.                                   NC1744.2
092200 RELAT-DELETE-GF-6.                                               NC1744.2
092300     PERFORM  DE-LETE.                                            NC1744.2
092400     GO TO    RELAT-WRITE-GF-6.                                   NC1744.2
092500 RELAT-CHECK-GF-6.                                                NC1744.2
092600     IF       VAL EQUAL TO 3                                      NC1744.2
092700              PERFORM PASS                                        NC1744.2
092800              GO TO RELAT-WRITE-GF-6.                             NC1744.2
092900     MOVE VAL TO COMPUTED-A.                                      NC1744.2
093000     MOVE 3 TO CORRECT-A.                                         NC1744.2
093100     PERFORM  FAIL.                                               NC1744.2
093200 RELAT-WRITE-GF-6.                                                NC1744.2
093300     MOVE "RELAT-TEST-GF-6" TO PAR-NAME.                          NC1744.2
093400     PERFORM  PRINT-DETAIL.                                       NC1744.2
093500 RELAT-INIT-GF-7.                                                 NC1744.2
093600     MOVE    "ABBREV. RELATION"  TO FEATURE.                      NC1744.2
093700     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
093800     MOVE     ZERO TO VAL.                                        NC1744.2
093900     MOVE     29   TO N-33.                                       NC1744.2
094000 RELAT-TEST-GF-7.                                                 NC1744.2
094100     IF       N-33 GREATER 2                                      NC1744.2
094200              PERFORM PASS                                        NC1744.2
094300              ELSE                                                NC1744.2
094400              PERFORM FAIL.                                       NC1744.2
094500     GO TO    RELAT-WRITE-GF-7.                                   NC1744.2
094600 RELAT-DELETE-GF-7.                                               NC1744.2
094700     PERFORM  DE-LETE.                                            NC1744.2
094800 RELAT-WRITE-GF-7.                                                NC1744.2
094900     MOVE "RELAT-TEST-GF-7" TO PAR-NAME.                          NC1744.2
095000     PERFORM  PRINT-DETAIL.                                       NC1744.2
095100 RELAT-INIT-GF-8.                                                 NC1744.2
095200     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
095300     MOVE     29   TO N-33.                                       NC1744.2
095400 RELAT-TEST-GF-8.                                                 NC1744.2
095500     IF       2 LESS N-33                                         NC1744.2
095600              PERFORM PASS                                        NC1744.2
095700              ELSE                                                NC1744.2
095800              PERFORM FAIL.                                       NC1744.2
095900     GO TO    RELAT-WRITE-GF-8.                                   NC1744.2
096000 RELAT-DELETE-GF-8.                                               NC1744.2
096100     PERFORM  DE-LETE.                                            NC1744.2
096200 RELAT-WRITE-GF-8.                                                NC1744.2
096300     MOVE "RELAT-TEST-GF-8" TO PAR-NAME.                          NC1744.2
096400     PERFORM  PRINT-DETAIL.                                       NC1744.2
096500*                                                                 NC1744.2
096600*                                                                 NC1744.2
096700 RELAT-INIT-GF-9.                                                 NC1744.2
096800     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
096900     MOVE     29   TO N-33.                                       NC1744.2
097000 RELAT-TEST-GF-9.                                                 NC1744.2
097100     IF      N-33 >= 2                                            NC1744.2
097200             PERFORM PASS                                         NC1744.2
097300     ELSE                                                         NC1744.2
097400             PERFORM FAIL.                                        NC1744.2
097500     GO TO   RELAT-WRITE-GF-9.                                    NC1744.2
097600 RELAT-DELETE-GF-9.                                               NC1744.2
097700     PERFORM DE-LETE.                                             NC1744.2
097800 RELAT-WRITE-GF-9.                                                NC1744.2
097900     MOVE  "RELAT-TEST-GF-9" TO PAR-NAME.                         NC1744.2
098000     PERFORM PRINT-DETAIL.                                        NC1744.2
098100*                                                                 NC1744.2
098200*                                                                 NC1744.2
098300 RELAT-INIT-GF-10.                                                NC1744.2
098400     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
098500 RELAT-TEST-GF-10.                                                NC1744.2
098600     IF      DATA-5 IS GREATER THAN OR EQUAL TO 4                 NC1744.2
098700             PERFORM PASS                                         NC1744.2
098800     ELSE                                                         NC1744.2
098900             PERFORM FAIL.                                        NC1744.2
099000     GO TO   RELAT-WRITE-GF-10.                                   NC1744.2
099100 RELAT-DELETE-GF-10.                                              NC1744.2
099200     PERFORM DE-LETE.                                             NC1744.2
099300 RELAT-WRITE-GF-10.                                               NC1744.2
099400     MOVE   "RELAT-TEST-GF-10" TO PAR-NAME.                       NC1744.2
099500     PERFORM PRINT-DETAIL.                                        NC1744.2
099600*                                                                 NC1744.2
099700*                                                                 NC1744.2
099800 RELAT-INIT-GF-11.                                                NC1744.2
099900     MOVE    "V1-54 6.3.1.1"     TO ANSI-REFERENCE.               NC1744.2
100000     MOVE    "X"    TO A-37.                                      NC1744.2
100100 RELAT-TEST-GF-11.                                                NC1744.2
100200     IF      A-37 GREATER OR EQUAL "A"                            NC1744.2
100300             PERFORM PASS                                         NC1744.2
100400     ELSE                                                         NC1744.2
100500             PERFORM FAIL.                                        NC1744.2
100600     GO TO   RELAT-WRITE-GF-11.                                   NC1744.2
100700 RELAT-DELETE-GF-11.                                              NC1744.2
100800     PERFORM DE-LETE.                                             NC1744.2
100900 RELAT-WRITE-GF-11.                                               NC1744.2
101000     MOVE   "RELAT-TEST-GF-11" TO PAR-NAME.                       NC1744.2
101100     PERFORM PRINT-DETAIL.                                        NC1744.2
101200*                                                                 NC1744.2
101300*                                                                 NC1744.2
101400 RELAT-INIT-GF-12.                                                NC1744.2
101500     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
101600     MOVE    29    TO N-33.                                       NC1744.2
101700 RELAT-TEST-GF-12.                                                NC1744.2
101800     IF      N-33 GREATER THAN OR EQUAL 2                         NC1744.2
101900             PERFORM PASS                                         NC1744.2
102000     ELSE                                                         NC1744.2
102100             PERFORM FAIL.                                        NC1744.2
102200     GO TO   RELAT-WRITE-GF-12.                                   NC1744.2
102300 RELAT-DELETE-GF-12.                                              NC1744.2
102400     PERFORM DE-LETE.                                             NC1744.2
102500 RELAT-WRITE-GF-12.                                               NC1744.2
102600     MOVE  "RELAT-TEST-GF-12" TO PAR-NAME.                        NC1744.2
102700     PERFORM PRINT-DETAIL.                                        NC1744.2
102800*                                                                 NC1744.2
102900*                                                                 NC1744.2
103000 RELAT-INIT-GF-13.                                                NC1744.2
103100     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
103200 RELAT-TEST-GF-13.                                                NC1744.2
103300     IF      DATA-99999 GREATER THAN OR EQUAL TO +99999           NC1744.2
103400             PERFORM PASS                                         NC1744.2
103500     ELSE                                                         NC1744.2
103600             PERFORM FAIL.                                        NC1744.2
103700     GO TO   RELAT-WRITE-GF-13.                                   NC1744.2
103800 RELAT-DELETE-GF-13.                                              NC1744.2
103900     PERFORM DE-LETE.                                             NC1744.2
104000 RELAT-WRITE-GF-13.                                               NC1744.2
104100     MOVE   "RELAT-TEST-GF-13" TO PAR-NAME.                       NC1744.2
104200     PERFORM PRINT-DETAIL.                                        NC1744.2
104300*                                                                 NC1744.2
104400*                                                                 NC1744.2
104500 RELAT-INIT-GF-14.                                                NC1744.2
104600     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
104700     MOVE   "X"  TO A-37.                                         NC1744.2
104800 RELAT-TEST-GF-14.                                                NC1744.2
104900     IF      A-37 IS GREATER OR EQUAL "A"                         NC1744.2
105000             PERFORM PASS                                         NC1744.2
105100     ELSE                                                         NC1744.2
105200             PERFORM FAIL.                                        NC1744.2
105300     GO TO   RELAT-WRITE-GF-14.                                   NC1744.2
105400 RELAT-DELETE-GF-14.                                              NC1744.2
105500     PERFORM DE-LETE.                                             NC1744.2
105600 RELAT-WRITE-GF-14.                                               NC1744.2
105700     MOVE   "RELAT-TEST-GF-14" TO PAR-NAME.                       NC1744.2
105800     PERFORM PRINT-DETAIL.                                        NC1744.2
105900*                                                                 NC1744.2
106000*                                                                 NC1744.2
106100 RELAT-INIT-GF-15.                                                NC1744.2
106200     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
106300     MOVE   "A"  TO A-2.                                          NC1744.2
106400     MOVE   "X"  TO A-37.                                         NC1744.2
106500 RELAT-TEST-GF-15.                                                NC1744.2
106600     IF      A-37 IS GREATER THAN OR EQUAL A-2                    NC1744.2
106700             PERFORM PASS                                         NC1744.2
106800     ELSE                                                         NC1744.2
106900             PERFORM FAIL.                                        NC1744.2
107000     GO TO   RELAT-WRITE-GF-15.                                   NC1744.2
107100 RELAT-DELETE-GF-15.                                              NC1744.2
107200     PERFORM DE-LETE.                                             NC1744.2
107300 RELAT-WRITE-GF-15.                                               NC1744.2
107400     MOVE   "RELAT-TEST-GF-15" TO PAR-NAME.                       NC1744.2
107500     PERFORM PRINT-DETAIL.                                        NC1744.2
107600*                                                                 NC1744.2
107700*                                                                 NC1744.2
107800 RELAT-INIT-GF-16.                                                NC1744.2
107900     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
108000 RELAT-TEST-GF-16.                                                NC1744.2
108100     IF      DATA-Z  IS >= "Y"                                    NC1744.2
108200             PERFORM PASS                                         NC1744.2
108300     ELSE                                                         NC1744.2
108400             PERFORM FAIL.                                        NC1744.2
108500     GO TO   RELAT-WRITE-GF-16.                                   NC1744.2
108600 RELAT-DELETE-GF-16.                                              NC1744.2
108700     PERFORM DE-LETE.                                             NC1744.2
108800 RELAT-WRITE-GF-16.                                               NC1744.2
108900     MOVE  "RELAT-TEST-GF-16" TO PAR-NAME.                        NC1744.2
109000     PERFORM PRINT-DETAIL.                                        NC1744.2
109100*                                                                 NC1744.2
109200*                                                                 NC1744.2
109300 RELAT-INIT-GF-17.                                                NC1744.2
109400     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
109500     MOVE    29 TO N-33.                                          NC1744.2
109600 RELAT-TEST-GF-17.                                                NC1744.2
109700     IF      2 <= N-33                                            NC1744.2
109800             PERFORM PASS                                         NC1744.2
109900     ELSE                                                         NC1744.2
110000             PERFORM FAIL.                                        NC1744.2
110100     GO TO   RELAT-WRITE-GF-17.                                   NC1744.2
110200 RELAT-DELETE-GF-17.                                              NC1744.2
110300     PERFORM DE-LETE.                                             NC1744.2
110400 RELAT-WRITE-GF-17.                                               NC1744.2
110500     MOVE  "RELAT-TEST-GF-17" TO PAR-NAME.                        NC1744.2
110600     PERFORM PRINT-DETAIL.                                        NC1744.2
110700*                                                                 NC1744.2
110800*                                                                 NC1744.2
110900 RELAT-INIT-GF-18.                                                NC1744.2
111000     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
111100 RELAT-TEST-GF-18.                                                NC1744.2
111200     IF      DATA-4 IS LESS THAN OR EQUAL TO 5                    NC1744.2
111300             PERFORM PASS                                         NC1744.2
111400     ELSE                                                         NC1744.2
111500             PERFORM FAIL.                                        NC1744.2
111600     GO TO   RELAT-WRITE-GF-18.                                   NC1744.2
111700 RELAT-DELETE-GF-18.                                              NC1744.2
111800     PERFORM DE-LETE.                                             NC1744.2
111900 RELAT-WRITE-GF-18.                                               NC1744.2
112000     MOVE   "RELAT-TEST-GF-18" TO PAR-NAME.                       NC1744.2
112100     PERFORM PRINT-DETAIL.                                        NC1744.2
112200*                                                                 NC1744.2
112300*                                                                 NC1744.2
112400 RELAT-INIT-GF-19.                                                NC1744.2
112500     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
112600     MOVE   "X" TO A-37.                                          NC1744.2
112700 RELAT-TEST-GF-19.                                                NC1744.2
112800     IF     "A" LESS OR EQUAL A-37                                NC1744.2
112900             PERFORM PASS                                         NC1744.2
113000     ELSE                                                         NC1744.2
113100             PERFORM FAIL.                                        NC1744.2
113200     GO TO   RELAT-WRITE-GF-19.                                   NC1744.2
113300 RELAT-DELETE-GF-19.                                              NC1744.2
113400     PERFORM DE-LETE.                                             NC1744.2
113500 RELAT-WRITE-GF-19.                                               NC1744.2
113600     MOVE   "RELAT-TEST-GF-19" TO PAR-NAME.                       NC1744.2
113700     PERFORM PRINT-DETAIL.                                        NC1744.2
113800*                                                                 NC1744.2
113900*                                                                 NC1744.2
114000 RELAT-INIT-GF-20.                                                NC1744.2
114100     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
114200     MOVE    29 TO N-33.                                          NC1744.2
114300 RELAT-TEST-GF-20.                                                NC1744.2
114400     IF      2 LESS THAN OR EQUAL N-33                            NC1744.2
114500             PERFORM PASS                                         NC1744.2
114600     ELSE                                                         NC1744.2
114700             PERFORM FAIL.                                        NC1744.2
114800     GO TO   RELAT-WRITE-GF-20.                                   NC1744.2
114900 RELAT-DELETE-GF-20.                                              NC1744.2
115000     PERFORM DE-LETE.                                             NC1744.2
115100 RELAT-WRITE-GF-20.                                               NC1744.2
115200     MOVE  "RELAT-TEST-GF-20" TO PAR-NAME.                        NC1744.2
115300     PERFORM PRINT-DETAIL.                                        NC1744.2
115400*                                                                 NC1744.2
115500*                                                                 NC1744.2
115600 RELAT-INIT-GF-21.                                                NC1744.2
115700     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
115800 RELAT-TEST-GF-21.                                                NC1744.2
115900     IF      DATA-99999 LESS THAN OR EQUAL TO +99999              NC1744.2
116000             PERFORM PASS                                         NC1744.2
116100     ELSE                                                         NC1744.2
116200             PERFORM FAIL.                                        NC1744.2
116300     GO TO   RELAT-WRITE-GF-21.                                   NC1744.2
116400 RELAT-DELETE-GF-21.                                              NC1744.2
116500     PERFORM DE-LETE.                                             NC1744.2
116600 RELAT-WRITE-GF-21.                                               NC1744.2
116700     MOVE   "RELAT-TEST-GF-21" TO PAR-NAME.                       NC1744.2
116800     PERFORM PRINT-DETAIL.                                        NC1744.2
116900*                                                                 NC1744.2
117000*                                                                 NC1744.2
117100 RELAT-INIT-GF-22.                                                NC1744.2
117200     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
117300     MOVE   "X" TO A-37.                                          NC1744.2
117400 RELAT-TEST-GF-22.                                                NC1744.2
117500     IF     "A" IS LESS OR EQUAL A-37                             NC1744.2
117600             PERFORM PASS                                         NC1744.2
117700     ELSE                                                         NC1744.2
117800             PERFORM FAIL.                                        NC1744.2
117900     GO TO   RELAT-WRITE-GF-22.                                   NC1744.2
118000 RELAT-DELETE-GF-22.                                              NC1744.2
118100     PERFORM DE-LETE.                                             NC1744.2
118200 RELAT-WRITE-GF-22.                                               NC1744.2
118300     MOVE   "RELAT-TEST-GF-22" TO PAR-NAME.                       NC1744.2
118400     PERFORM PRINT-DETAIL.                                        NC1744.2
118500*                                                                 NC1744.2
118600*                                                                 NC1744.2
118700 RELAT-INIT-GF-23.                                                NC1744.2
118800     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
118900     MOVE   "A" TO A-2.                                           NC1744.2
119000     MOVE   "X" TO A-37.                                          NC1744.2
119100 RELAT-TEST-GF-23.                                                NC1744.2
119200     IF      A-2 IS LESS THAN OR EQUAL A-37                       NC1744.2
119300             PERFORM PASS                                         NC1744.2
119400     ELSE                                                         NC1744.2
119500             PERFORM FAIL.                                        NC1744.2
119600     GO TO   RELAT-WRITE-GF-23.                                   NC1744.2
119700 RELAT-DELETE-GF-23.                                              NC1744.2
119800     PERFORM DE-LETE.                                             NC1744.2
119900 RELAT-WRITE-GF-23.                                               NC1744.2
120000     MOVE   "RELAT-TEST-GF-23" TO PAR-NAME.                       NC1744.2
120100     PERFORM PRINT-DETAIL.                                        NC1744.2
120200*                                                                 NC1744.2
120300*                                                                 NC1744.2
120400 RELAT-INIT-GF-24.                                                NC1744.2
120500     MOVE   "V1-54 6.3.1.1"     TO ANSI-REFERENCE.                NC1744.2
120600 RELAT-TEST-GF-24.                                                NC1744.2
120700     IF     DATA-Y IS <= "Z"                                      NC1744.2
120800             PERFORM PASS                                         NC1744.2
120900     ELSE                                                         NC1744.2
121000             PERFORM FAIL.                                        NC1744.2
121100     GO TO   RELAT-WRITE-GF-24.                                   NC1744.2
121200 RELAT-DELETE-GF-24.                                              NC1744.2
121300     PERFORM DE-LETE.                                             NC1744.2
121400 RELAT-WRITE-GF-24.                                               NC1744.2
121500     MOVE   "RELAT-TEST-GF-24" TO PAR-NAME.                       NC1744.2
121600     PERFORM PRINT-DETAIL.                                        NC1744.2
121700*                                                                 NC1744.2
121800*                                                                 NC1744.2
121900 CLASS-INIT-GF-1.                                                 NC1744.2
122000     PERFORM END-ROUTINE.                                         NC1744.2
122100     MOVE   "CLASS ---" TO FEATURE.                               NC1744.2
122200     MOVE   "V1-56 6.3.1.2"     TO ANSI-REFERENCE.                NC1744.2
122300     PERFORM PRINT-DETAIL.                                        NC1744.2
122400     MOVE  ZERO TO N-31.                                          NC1744.2
122500 CLASS-TEST-GF-1.                                                 NC1744.2
122600     IF     X-32 NUMERIC                                          NC1744.2
122700            PERFORM PASS                                          NC1744.2
122800            GO TO CLASS-WRITE-GF-1.                               NC1744.2
122900     PERFORM FAIL.                                                NC1744.2
123000     GO TO  CLASS-WRITE-GF-1.                                     NC1744.2
123100 CLASS-DELETE-GF-1.                                               NC1744.2
123200     PERFORM DE-LETE.                                             NC1744.2
123300 CLASS-WRITE-GF-1.                                                NC1744.2
123400     MOVE  "  NUMERIC           " TO FEATURE.                     NC1744.2
123500     MOVE  "CLASS-TEST-GF-1" TO PAR-NAME.                         NC1744.2
123600     PERFORM PRINT-DETAIL.                                        NC1744.2
123700 CLASS-INIT-GF-2.                                                 NC1744.2
123800     MOVE   "V1-56 6.3.1.2"     TO ANSI-REFERENCE.                NC1744.2
123900     MOVE   "X" TO A-37.                                          NC1744.2
124000 CLASS-TEST-GF-2.                                                 NC1744.2
124100     IF X-38 NOT NUMERIC                                          NC1744.2
124200             PERFORM PASS                                         NC1744.2
124300             GO TO CLASS-WRITE-GF-2.                              NC1744.2
124400     PERFORM FAIL.                                                NC1744.2
124500     GO TO  CLASS-WRITE-GF-2.                                     NC1744.2
124600 CLASS-DELETE-GF-2.                                               NC1744.2
124700     PERFORM DE-LETE.                                             NC1744.2
124800 CLASS-WRITE-GF-2.                                                NC1744.2
124900     MOVE  "  NOT NUMERIC       " TO FEATURE.                     NC1744.2
125000     MOVE   "CLASS-TEST-GF-2" TO PAR-NAME.                        NC1744.2
125100     PERFORM PRINT-DETAIL.                                        NC1744.2
125200 CLASS-INIT-GF-3.                                                 NC1744.2
125300     MOVE   "V1-56 6.3.1.2"     TO ANSI-REFERENCE.                NC1744.2
125400     MOVE   "X" TO A-37.                                          NC1744.2
125500 CLASS-TEST-GF-3.                                                 NC1744.2
125600     IF      X-38 IS NOT NUMERIC                                  NC1744.2
125700             PERFORM PASS                                         NC1744.2
125800             GO TO CLASS-WRITE-GF-3.                              NC1744.2
125900     PERFORM FAIL.                                                NC1744.2
126000     GO TO  CLASS-WRITE-GF-3.                                     NC1744.2
126100 CLASS-DELETE-GF-3.                                               NC1744.2
126200     PERFORM DE-LETE.                                             NC1744.2
126300 CLASS-WRITE-GF-3.                                                NC1744.2
126400     MOVE   "  NOT NUMERIC       " TO FEATURE.                    NC1744.2
126500     MOVE   "CLASS-TEST-GF-3" TO PAR-NAME.                        NC1744.2
126600     PERFORM PRINT-DETAIL.                                        NC1744.2
126700 CLASS-INIT-GF-4.                                                 NC1744.2
126800     MOVE   "V1-56 6.3.1.2"     TO ANSI-REFERENCE.                NC1744.2
126900     MOVE  -1 TO DUMMY-DS-00001.                                  NC1744.2
127000 CLASS-TEST-GF-4.                                                 NC1744.2
127100     IF DUMMY-DS-00001 IS NUMERIC                                 NC1744.2
127200              PERFORM PASS                                        NC1744.2
127300              GO TO CLASS-WRITE-GF-4.                             NC1744.2
127400     PERFORM  FAIL.                                               NC1744.2
127500     GO TO CLASS-WRITE-GF-4.                                      NC1744.2
127600 CLASS-DELETE-GF-4.                                               NC1744.2
127700     PERFORM DE-LETE.                                             NC1744.2
127800 CLASS-WRITE-GF-4.                                                NC1744.2
127900     MOVE "CLASS-TEST-GF-4" TO PAR-NAME.                          NC1744.2
128000     PERFORM  PRINT-DETAIL.                                       NC1744.2
128100 CLASS-INIT-GF-5.                                                 NC1744.2
128200     MOVE "  NUMERIC           " TO FEATURE.                      NC1744.2
128300     MOVE   "V1-56 6.3.1.2"     TO ANSI-REFERENCE.                NC1744.2
128400     MOVE   "111111111111111111" TO ONES-XN-00018.                NC1744.2
128500 CLASS-TEST-GF-5.                                                 NC1744.2
128600     IF ONES-XN-00018 IS NUMERIC                                  NC1744.2
128700         PERFORM PASS                                             NC1744.2
128800         GO TO CLASS-WRITE-GF-5.                                  NC1744.2
128900     MOVE ONES-XN-00018 TO COMPUTED-A.                            NC1744.2
129000     MOVE "NUMERIC EXPECTED" TO CORRECT-A.                        NC1744.2
129100     PERFORM FAIL.                                                NC1744.2
129200     GO TO CLASS-WRITE-GF-5.                                      NC1744.2
129300 CLASS-DELETE-GF-5.                                               NC1744.2
129400     PERFORM DE-LETE.                                             NC1744.2
129500 CLASS-WRITE-GF-5.                                                NC1744.2
129600     MOVE "CLASS-TEST-GF-5 " TO PAR-NAME.                         NC1744.2
129700     PERFORM PRINT-DETAIL.                                        NC1744.2
129800 CLASS-INIT-GF-6.                                                 NC1744.2
129900     MOVE   "V1-56 6.3.1.2"     TO ANSI-REFERENCE.                NC1744.2
130000     MOVE   +022.00             TO A02TWOS-DS-03V02.              NC1744.2
130100 CLASS-TEST-GF-6.                                                 NC1744.2
130200     IF A02TWOS-DS-03V02 IS NUMERIC                               NC1744.2
130300         PERFORM PASS                                             NC1744.2
130400         GO TO CLASS-WRITE-GF-6.                                  NC1744.2
130500     MOVE A02TWOS-DS-03V02 TO COMPUTED-N.                         NC1744.2
130600     MOVE "NUMERIC EXPECTED" TO CORRECT-A.                        NC1744.2
130700     PERFORM FAIL.                                                NC1744.2
130800     GO TO CLASS-WRITE-GF-6.                                      NC1744.2
130900 CLASS-DELETE-GF-6.                                               NC1744.2
131000     PERFORM DE-LETE.                                             NC1744.2
131100 CLASS-WRITE-GF-6.                                                NC1744.2
131200     MOVE "CLASS-TEST-GF-6 " TO PAR-NAME.                         NC1744.2
131300     PERFORM PRINT-DETAIL.                                        NC1744.2
131400 CLASS-INIT-GF-7.                                                 NC1744.2
131500     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
131600     MOVE   "00ABCDEFGHI  4321 " TO XDATA-XN-00018.               NC1744.2
131700 CLASS-TEST-GF-7.                                                 NC1744.2
131800     IF XDATA-XN-00018 IS NUMERIC                                 NC1744.2
131900         MOVE XDATA-XN-00018 TO COMPUTED-A                        NC1744.2
132000         MOVE "ALPHABETIC EXPECTED" TO CORRECT-A                  NC1744.2
132100         PERFORM FAIL                                             NC1744.2
132200         GO TO CLASS-WRITE-GF-7.                                  NC1744.2
132300     PERFORM PASS.                                                NC1744.2
132400     GO TO CLASS-WRITE-GF-7.                                      NC1744.2
132500 CLASS-DELETE-GF-7.                                               NC1744.2
132600     PERFORM DE-LETE.                                             NC1744.2
132700 CLASS-WRITE-GF-7.                                                NC1744.2
132800     MOVE "CLASS-TEST-GF-7 " TO PAR-NAME.                         NC1744.2
132900     PERFORM PRINT-DETAIL.                                        NC1744.2
133000 CLASS-INIT-GF-8.                                                 NC1744.2
133100     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
133200     MOVE   "00ABCDEFGHI  4321 " TO XDATA-XN-00018.               NC1744.2
133300 CLASS-TEST-GF-8.                                                 NC1744.2
133400     IF XDATA-DS-18V00-S IS NUMERIC                               NC1744.2
133500         MOVE XDATA-DS-18V00-S TO COMPUTED-A                      NC1744.2
133600         MOVE "ALPHABETIC EXPECTED" TO CORRECT-A                  NC1744.2
133700         PERFORM FAIL                                             NC1744.2
133800         GO TO CLASS-WRITE-GF-8.                                  NC1744.2
133900     PERFORM PASS.                                                NC1744.2
134000     GO TO CLASS-WRITE-GF-8.                                      NC1744.2
134100 CLASS-DELETE-GF-8.                                               NC1744.2
134200     PERFORM DE-LETE.                                             NC1744.2
134300 CLASS-WRITE-GF-8.                                                NC1744.2
134400     MOVE "CLASS-TEST-GF-8 " TO PAR-NAME.                         NC1744.2
134500     PERFORM PRINT-DETAIL.                                        NC1744.2
134600 CLASS-INIT-GF-9.                                                 NC1744.2
134700     MOVE "  NOT NUMERIC       " TO FEATURE.                      NC1744.2
134800     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
134900     MOVE SPACE TO CORRECT-A.                                     NC1744.2
135000 CLASS-TEST-GF-9.                                                 NC1744.2
135100     IF CORRECT-A NOT NUMERIC                                     NC1744.2
135200         PERFORM PASS                                             NC1744.2
135300         GO TO CLASS-WRITE-GF-9.                                  NC1744.2
135400     MOVE CORRECT-A TO COMPUTED-A.                                NC1744.2
135500     MOVE "ALPHABETIC EXPECTED" TO CORRECT-A.                     NC1744.2
135600     PERFORM FAIL.                                                NC1744.2
135700     GO TO CLASS-WRITE-GF-9.                                      NC1744.2
135800 CLASS-DELETE-GF-9.                                               NC1744.2
135900     PERFORM DE-LETE.                                             NC1744.2
136000 CLASS-WRITE-GF-9.                                                NC1744.2
136100     MOVE "CLASS-TEST-GF-9 " TO PAR-NAME.                         NC1744.2
136200     PERFORM PRINT-DETAIL.                                        NC1744.2
136300 CLASS-INIT-GF-10.                                                NC1744.2
136400     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
136500     MOVE   "00ABCDEFGHI  4321 " TO XDATA-XN-00018.               NC1744.2
136600 CLASS-TEST-GF-10.                                                NC1744.2
136700     IF  XDATA-DS-18V00-S NOT NUMERIC                             NC1744.2
136800         PERFORM PASS                                             NC1744.2
136900         GO TO CLASS-WRITE-GF-10.                                 NC1744.2
137000     MOVE  XDATA-DS-18V00-S TO COMPUTED-A                         NC1744.2
137100     MOVE  "NONNUMERIC EXPECTED" TO CORRECT-A.                    NC1744.2
137200     PERFORM FAIL.                                                NC1744.2
137300     GO TO  CLASS-WRITE-GF-10.                                    NC1744.2
137400 CLASS-DELETE-GF-10.                                              NC1744.2
137500     PERFORM DE-LETE.                                             NC1744.2
137600 CLASS-WRITE-GF-10.                                               NC1744.2
137700     MOVE "CLASS-TEST-GF-10" TO PAR-NAME.                         NC1744.2
137800     PERFORM PRINT-DETAIL.                                        NC1744.2
137900 CLASS-INIT-GF-11.                                                NC1744.2
138000     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
138100     MOVE ZERO TO CORRECT-A.                                      NC1744.2
138200 CLASS-TEST-GF-11.                                                NC1744.2
138300     IF CORRECT-A IS NOT NUMERIC                                  NC1744.2
138400         MOVE CORRECT-A TO COMPUTED-A                             NC1744.2
138500         MOVE "NUMERIC EXPECTED" TO CORRECT-A                     NC1744.2
138600         PERFORM FAIL                                             NC1744.2
138700         GO TO CLASS-WRITE-GF-11.                                 NC1744.2
138800     PERFORM PASS.                                                NC1744.2
138900     MOVE SPACE TO CORRECT-A.                                     NC1744.2
139000     GO TO CLASS-WRITE-GF-11.                                     NC1744.2
139100 CLASS-DELETE-GF-11.                                              NC1744.2
139200     PERFORM DE-LETE.                                             NC1744.2
139300 CLASS-WRITE-GF-11.                                               NC1744.2
139400     MOVE "CLASS-TEST-GF-11" TO PAR-NAME.                         NC1744.2
139500     PERFORM PRINT-DETAIL.                                        NC1744.2
139600 CLASS-INIT-GF-12.                                                NC1744.2
139700     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
139800     MOVE    +990 TO A990-DS-0201P.                               NC1744.2
139900 CLASS-TEST-GF-12.                                                NC1744.2
140000     IF A990-DS-0201P IS NOT NUMERIC                              NC1744.2
140100         MOVE A990-DS-0201P TO CORRECT-N                          NC1744.2
140200         MOVE "NUMERIC EXPECTED" TO COMPUTED-A                    NC1744.2
140300         PERFORM FAIL                                             NC1744.2
140400         GO TO CLASS-WRITE-GF-12.                                 NC1744.2
140500     PERFORM PASS.                                                NC1744.2
140600     GO TO CLASS-WRITE-GF-12.                                     NC1744.2
140700 CLASS-DELETE-GF-12.                                              NC1744.2
140800     PERFORM DE-LETE.                                             NC1744.2
140900 CLASS-WRITE-GF-12.                                               NC1744.2
141000     MOVE "CLASS-TEST-GF-12" TO PAR-NAME.                         NC1744.2
141100     PERFORM PRINT-DETAIL.                                        NC1744.2
141200 CLASS-INIT-13.                                                   NC1744.2
141300     MOVE "  ALPHABETIC        " TO FEATURE.                      NC1744.2
141400     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
141500     MOVE   "ABCDEFGHIJ"         TO YADATA-XN-00010.              NC1744.2
141600 CLASS-TEST-GF-13.                                                NC1744.2
141700     IF YADATA-XN-00010 IS ALPHABETIC                             NC1744.2
141800         PERFORM PASS                                             NC1744.2
141900         GO TO CLASS-WRITE-GF-13.                                 NC1744.2
142000     MOVE YADATA-XN-00010 TO COMPUTED-A.                          NC1744.2
142100     MOVE "ALPHABETIC EXPECTED" TO CORRECT-A.                     NC1744.2
142200     PERFORM FAIL.                                                NC1744.2
142300     GO TO CLASS-WRITE-GF-13.                                     NC1744.2
142400 CLASS-DELETE-GF-13.                                              NC1744.2
142500     PERFORM DE-LETE.                                             NC1744.2
142600 CLASS-WRITE-GF-13.                                               NC1744.2
142700     MOVE "CLASS-TEST-GF-13" TO PAR-NAME.                         NC1744.2
142800     PERFORM PRINT-DETAIL.                                        NC1744.2
142900 CLASS-INIT-14.                                                   NC1744.2
143000     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
143100     MOVE  ZERO TO CORRECT-A.                                     NC1744.2
143200 CLASS-TEST-GF-14.                                                NC1744.2
143300     IF CORRECT-A ALPHABETIC                                      NC1744.2
143400         MOVE CORRECT-A TO COMPUTED-A                             NC1744.2
143500         MOVE "NUMERIC EXPECTED"    TO CORRECT-A                  NC1744.2
143600         PERFORM FAIL                                             NC1744.2
143700         GO TO CLASS-WRITE-GF-14.                                 NC1744.2
143800     PERFORM PASS.                                                NC1744.2
143900     MOVE SPACE TO CORRECT-A                                      NC1744.2
144000     GO TO CLASS-WRITE-GF-14.                                     NC1744.2
144100 CLASS-DELETE-GF-14.                                              NC1744.2
144200     PERFORM DE-LETE.                                             NC1744.2
144300 CLASS-WRITE-GF-14.                                               NC1744.2
144400     MOVE "CLASS-TEST-GF-14" TO PAR-NAME.                         NC1744.2
144500     PERFORM PRINT-DETAIL.                                        NC1744.2
144600 CLASS-INIT-GF-15.                                                NC1744.2
144700     MOVE "  NOT ALPHABETIC    " TO FEATURE.                      NC1744.2
144800     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
144900     MOVE   "00ABCDEFGHI  4321 " TO XDATA-XN-00018.               NC1744.2
145000 CLASS-TEST-GF-15.                                                NC1744.2
145100     IF XDATA-XN-00018 IS NOT ALPHABETIC                          NC1744.2
145200         PERFORM PASS                                             NC1744.2
145300         GO TO CLASS-WRITE-GF-15.                                 NC1744.2
145400     MOVE XDATA-XN-00018 TO COMPUTED-A.                           NC1744.2
145500     MOVE "NUMERIC EXPECTED" TO CORRECT-A.                        NC1744.2
145600     PERFORM FAIL.                                                NC1744.2
145700     GO TO CLASS-WRITE-GF-15.                                     NC1744.2
145800 CLASS-DELETE-GF-15.                                              NC1744.2
145900     PERFORM DE-LETE.                                             NC1744.2
146000 CLASS-WRITE-GF-15.                                               NC1744.2
146100     MOVE "CLASS-TEST-GF-15" TO PAR-NAME.                         NC1744.2
146200     PERFORM PRINT-DETAIL.                                        NC1744.2
146300 CLASS-INIT-GF-16.                                                NC1744.2
146400     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
146500     MOVE   "ABCDEFGHIJ"         TO YADATA-XN-00010.              NC1744.2
146600 CLASS-TEST-GF-16.                                                NC1744.2
146700     IF YADATA-XN-00010 IS NOT ALPHABETIC                         NC1744.2
146800         MOVE YADATA-XN-00010 TO COMPUTED-A                       NC1744.2
146900         MOVE "ALPHABETIC EXPECTED" TO CORRECT-A                  NC1744.2
147000         PERFORM FAIL                                             NC1744.2
147100         GO TO CLASS-WRITE-GF-16.                                 NC1744.2
147200     PERFORM PASS.                                                NC1744.2
147300     GO TO CLASS-WRITE-GF-16.                                     NC1744.2
147400 CLASS-DELETE-GF-16.                                              NC1744.2
147500     PERFORM DE-LETE.                                             NC1744.2
147600 CLASS-WRITE-GF-16.                                               NC1744.2
147700     MOVE "CLASS-TEST-GF-16" TO PAR-NAME.                         NC1744.2
147800     PERFORM PRINT-DETAIL.                                        NC1744.2
147900*CLASS-TEST-17.                                                   NC1744.2
148000*    THIS TEST WAS DELETED BY THE FCCTS SINCE IT DID NOT          NC1744.2
148100*    APPLY TO THE REVISED FEDERAL STANDARD - FIPS PUB 21-1.       NC1744.2
148200 CLASS-INIT-GF-17.                                                NC1744.2
148300     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
148400     MOVE    0 TO NUMERIC-1.                                      NC1744.2
148500     MOVE ZERO TO NUMERIC-3.                                      NC1744.2
148600     MOVE    1 TO NUMERIC-5.                                      NC1744.2
148700     MOVE  "7" TO NUMERIC-7.                                      NC1744.2
148800     MOVE    8 TO NUMERIC-8.                                      NC1744.2
148900 CLASS-TEST-GF-17.                                                NC1744.2
149000     IF NUMERIC-GRP-TEST NUMERIC                                  NC1744.2
149100         PERFORM PASS                                             NC1744.2
149200         GO TO CLASS-WRITE-GF-17.                                 NC1744.2
149300     MOVE "NUMERIC EXPECTED " TO CORRECT-A.                       NC1744.2
149400     MOVE "SEE PROGRAM FOR RESULTS    " TO RE-MARK.               NC1744.2
149500     PERFORM FAIL.                                                NC1744.2
149600     GO TO CLASS-WRITE-GF-17.                                     NC1744.2
149700 CLASS-DELETE-GF-17.                                              NC1744.2
149800     PERFORM DE-LETE.                                             NC1744.2
149900 CLASS-WRITE-GF-17.                                               NC1744.2
150000     MOVE "CLASS-TEST-GF-17" TO PAR-NAME.                         NC1744.2
150100     PERFORM PRINT-DETAIL.                                        NC1744.2
150200 CLASS-INIT-GF-18.                                                NC1744.2
150300     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
150400     MOVE ZERO TO NUMERIC-3.                                      NC1744.2
150500     MOVE    1 TO NUMERIC-5.                                      NC1744.2
150600 CLASS-TEST-GF-18.                                                NC1744.2
150700     IF NUMERIC-2 NUMERIC                                         NC1744.2
150800         PERFORM PASS                                             NC1744.2
150900         GO TO CLASS-WRITE-GF-18.                                 NC1744.2
151000     MOVE "NUMERIC EXPECTED " TO CORRECT-A.                       NC1744.2
151100     MOVE "SEE PROGRAM FOR RESULTS  " TO RE-MARK.                 NC1744.2
151200     PERFORM FAIL.                                                NC1744.2
151300     GO TO CLASS-WRITE-GF-18.                                     NC1744.2
151400 CLASS-DELETE-GF-18.                                              NC1744.2
151500     PERFORM DE-LETE.                                             NC1744.2
151600 CLASS-WRITE-GF-18.                                               NC1744.2
151700     MOVE "CLASS-TEST-GF-18" TO PAR-NAME.                         NC1744.2
151800     PERFORM PRINT-DETAIL.                                        NC1744.2
151900 CLASS-INIT-GF-19.                                                NC1744.2
152000     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
152100     MOVE    1 TO NUMERIC-5.                                      NC1744.2
152200 CLASS-TEST-GF-19.                                                NC1744.2
152300     IF NUMERIC-4 NUMERIC                                         NC1744.2
152400         PERFORM PASS                                             NC1744.2
152500         GO TO CLASS-WRITE-GF-19.                                 NC1744.2
152600     MOVE "NUMERIC EXPECTED  " TO CORRECT-A.                      NC1744.2
152700     MOVE "SEE PROGRAM FOR RESULTS " TO RE-MARK.                  NC1744.2
152800     PERFORM FAIL.                                                NC1744.2
152900     GO TO CLASS-WRITE-GF-19.                                     NC1744.2
153000 CLASS-DELETE-GF-19.                                              NC1744.2
153100     PERFORM DE-LETE.                                             NC1744.2
153200 CLASS-WRITE-GF-19.                                               NC1744.2
153300     MOVE "CLASS-TEST-GF-19" TO PAR-NAME.                         NC1744.2
153400     PERFORM PRINT-DETAIL.                                        NC1744.2
153500 CLASS-INIT-GF-20.                                                NC1744.2
153600     MOVE   "V1-56 6.3.1.2"      TO ANSI-REFERENCE.               NC1744.2
153700     MOVE  "7" TO NUMERIC-7.                                      NC1744.2
153800     MOVE    8 TO NUMERIC-8.                                      NC1744.2
153900 CLASS-TEST-GF-20.                                                NC1744.2
154000     IF NUMERIC-6 NUMERIC                                         NC1744.2
154100         PERFORM PASS                                             NC1744.2
154200         GO TO CLASS-WRITE-GF-20.                                 NC1744.2
154300     MOVE "NUMERIC EXPECTED " TO CORRECT-A.                       NC1744.2
154400     MOVE "SEE PROGRAM FOR RESULTS " TO RE-MARK.                  NC1744.2
154500     PERFORM FAIL.                                                NC1744.2
154600     GO TO CLASS-WRITE-GF-20.                                     NC1744.2
154700 CLASS-DELETE-GF-20.                                              NC1744.2
154800     PERFORM DE-LETE.                                             NC1744.2
154900 CLASS-WRITE-GF-20.                                               NC1744.2
155000     MOVE "CLASS-TEST-GF-20" TO PAR-NAME.                         NC1744.2
155100     PERFORM PRINT-DETAIL.                                        NC1744.2
155200*CLASS-TEST-22.                                                   NC1744.2
155300*    THIS TEST WAS DELETED BY THE FCCTS SINCE IT DID NOT          NC1744.2
155400*    APPLY TO THE REVISED FEDERAL STANDARD - FIPS PUB 21-1.       NC1744.2
155500*                                                                 NC1744.2
155600*                                                                 NC1744.2
155700 CLASS-INIT-35.                                                   NC1744.2
155800     MOVE   "V1-56 6.3.1.2(3,4)" TO ANSI-REFERENCE.               NC1744.2
155900     MOVE "  ALPHABETIC-UPPER  " TO FEATURE.                      NC1744.2
156000     MOVE " UPPERCASE CHARS"     TO ALPHA-UPPER.                  NC1744.2
156100 CLASS-TEST-GF-35.                                                NC1744.2
156200     IF      ALPHA-UPPER ALPHABETIC-UPPER                         NC1744.2
156300             PERFORM PASS                                         NC1744.2
156400             GO TO   CLASS-WRITE-GF-35.                           NC1744.2
156500     MOVE   "SEE PROGRAM FOR RESULTS " TO CORRECT-A.              NC1744.2
156600     MOVE   "UPPERCASE CHARS " TO COMPUTED-A.                     NC1744.2
156700     MOVE   "UPPERCASE CHARS NOT ACCEPTED AS ALPHABETIC-UPPER"    NC1744.2
156800          TO RE-MARK.                                             NC1744.2
156900     PERFORM FAIL.                                                NC1744.2
157000     GO TO   CLASS-WRITE-GF-35.                                   NC1744.2
157100 CLASS-DELETE-GF-35.                                              NC1744.2
157200     PERFORM DE-LETE.                                             NC1744.2
157300 CLASS-WRITE-GF-35.                                               NC1744.2
157400     MOVE   "CLASS-TEST-GF-35" TO PAR-NAME.                       NC1744.2
157500     PERFORM PRINT-DETAIL.                                        NC1744.2
157600*                                                                 NC1744.2
157700*                                                                 NC1744.2
157800*                                                                 NC1744.2
157900*                                                                 NC1744.2
158000 CLASS-INIT-36.                                                   NC1744.2
158100     MOVE "  ALPHABETIC-LOWER  " TO FEATURE.                      NC1744.2
158200     MOVE " lowercase chars"     TO ALPHA-LOWER.                  NC1744.2
158300     MOVE   "V1-56 6.3.1.2(3,4)" TO ANSI-REFERENCE.               NC1744.2
158400 CLASS-TEST-GF-36.                                                NC1744.2
158500     IF      ALPHA-LOWER ALPHABETIC-LOWER                         NC1744.2
158600             PERFORM PASS                                         NC1744.2
158700             GO TO   CLASS-WRITE-GF-36.                           NC1744.2
158800     MOVE   "SEE PROGRAM"              TO CORRECT-A.              NC1744.2
158900     MOVE   "lowercase chars"          TO COMPUTED-A.             NC1744.2
159000     MOVE   "LOWERCASE CHARS NOT ACCEPTED AS ALPHABETIC-LOWER"    NC1744.2
159100          TO RE-MARK.                                             NC1744.2
159200     PERFORM FAIL.                                                NC1744.2
159300     GO TO   CLASS-WRITE-GF-36.                                   NC1744.2
159400 CLASS-DELETE-GF-36.                                              NC1744.2
159500     PERFORM DE-LETE.                                             NC1744.2
159600 CLASS-WRITE-GF-36.                                               NC1744.2
159700     MOVE   "CLASS-TEST-GF-36" TO PAR-NAME.                       NC1744.2
159800     PERFORM PRINT-DETAIL.                                        NC1744.2
159900*                                                                 NC1744.2
160000*                                                                 NC1744.2
160100 CLASS-INIT-37.                                                   NC1744.2
160200     MOVE   "V1-56 6.3.1.2(2)"     TO ANSI-REFERENCE.             NC1744.2
160300     MOVE   "UPPER & LOWER CASE  " TO FEATURE.                    NC1744.2
160400     move   "AbCdEfGhIj"           TO YADATA-XN-00010-U-AND-L.    NC1744.2
160500 CLASS-TEST-GF-37.                                                NC1744.2
160600     IF      YADATA-XN-00010-U-AND-L IS ALPHABETIC                NC1744.2
160700             PERFORM PASS                                         NC1744.2
160800             GO TO   CLASS-WRITE-GF-37.                           NC1744.2
160900     MOVE    YADATA-XN-00010-U-AND-L TO COMPUTED-A.               NC1744.2
161000     MOVE    "ALPHABETIC EXPECTED" TO CORRECT-A.                  NC1744.2
161100     MOVE   "UPPER & LOWER CASE NOT ACCEPTED AS ALPHABETIC"       NC1744.2
161200          TO RE-MARK.                                             NC1744.2
161300     PERFORM FAIL.                                                NC1744.2
161400     GO TO   CLASS-WRITE-GF-37.                                   NC1744.2
161500 CLASS-DELETE-GF-37.                                              NC1744.2
161600     PERFORM DE-LETE.                                             NC1744.2
161700 CLASS-WRITE-GF-37.                                               NC1744.2
161800     MOVE   "CLASS-TEST-GF-37" TO PAR-NAME.                       NC1744.2
161900     PERFORM PRINT-DETAIL.                                        NC1744.2
162000*                                                                 NC1744.2
162100*                                                                 NC1744.2
162200 CLASS-INIT-38.                                                   NC1744.2
162300     MOVE   "V1-56 6.3.1.2(2)"     TO ANSI-REFERENCE.             NC1744.2
162400     move   "AbCdEfGhIj"           TO YADATA-XN-00010-U-AND-L.    NC1744.2
162500 CLASS-TEST-GF-38.                                                NC1744.2
162600     IF      YADATA-XN-00010-U-AND-L IS NOT ALPHABETIC            NC1744.2
162700             MOVE    YADATA-XN-00010-U-AND-L TO COMPUTED-A        NC1744.2
162800             MOVE   "ALPHABETIC EXPECTED" TO CORRECT-A            NC1744.2
162900             MOVE  "UPPER & LOWER CASE NOT ACCEPTED AS ALPHABETIC"NC1744.2
163000                  TO RE-MARK                                      NC1744.2
163100             PERFORM FAIL                                         NC1744.2
163200             GO TO   CLASS-WRITE-GF-38.                           NC1744.2
163300     PERFORM PASS.                                                NC1744.2
163400     GO TO   CLASS-WRITE-GF-38.                                   NC1744.2
163500 CLASS-DELETE-GF-38.                                              NC1744.2
163600     PERFORM DE-LETE.                                             NC1744.2
163700 CLASS-WRITE-GF-38.                                               NC1744.2
163800     MOVE   "CLASS-TEST-GF-38" TO PAR-NAME.                       NC1744.2
163900     PERFORM PRINT-DETAIL.                                        NC1744.2
164000*                                                                 NC1744.2
164100*                                                                 NC1744.2
164200 CLASS-INIT-39.                                                   NC1744.2
164300     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
164400          TO ANSI-REFERENCE.                                      NC1744.2
164500     MOVE   "NEW CLASS TESTS"  TO FEATURE.                        NC1744.2
164600     MOVE   "CLASS-TEST-GF-39" TO PAR-NAME.                       NC1744.2
164700     MOVE   "A" TO WS-A.                                          NC1744.2
164800     GO TO   CLASS-TEST-GF-39.                                    NC1744.2
164900 CLASS-DELETE-GF-39.                                              NC1744.2
165000     PERFORM DE-LETE.                                             NC1744.2
165100     PERFORM PRINT-DETAIL.                                        NC1744.2
165200     GO TO   CLASS-INIT-40.                                       NC1744.2
165300 CLASS-TEST-GF-39.                                                NC1744.2
165400     IF      WS-A ORDINAL-A-ONLY                                  NC1744.2
165500             PERFORM PASS                                         NC1744.2
165600             PERFORM PRINT-DETAIL                                 NC1744.2
165700     ELSE                                                         NC1744.2
165800             MOVE   "LETTER 'A' SHOULD BE ORDINAL-A-ONLY"         NC1744.2
165900                  TO RE-MARK                                      NC1744.2
166000             PERFORM FAIL                                         NC1744.2
166100             PERFORM PRINT-DETAIL.                                NC1744.2
166200*                                                                 NC1744.2
166300*                                                                 NC1744.2
166400 CLASS-INIT-40.                                                   NC1744.2
166500     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
166600          TO ANSI-REFERENCE.                                      NC1744.2
166700     MOVE   "CLASS-TEST-GF-40" TO PAR-NAME.                       NC1744.2
166800     MOVE   "Z" TO WS-A.                                          NC1744.2
166900     GO TO   CLASS-TEST-GF-40.                                    NC1744.2
167000 CLASS-DELETE-GF-40.                                              NC1744.2
167100     PERFORM DE-LETE.                                             NC1744.2
167200     PERFORM PRINT-DETAIL.                                        NC1744.2
167300     GO TO   CLASS-INIT-41.                                       NC1744.2
167400 CLASS-TEST-GF-40.                                                NC1744.2
167500     IF      WS-A NOT ORDINAL-A-ONLY                              NC1744.2
167600             PERFORM PASS                                         NC1744.2
167700             PERFORM PRINT-DETAIL                                 NC1744.2
167800     ELSE                                                         NC1744.2
167900             MOVE   "LETTER 'Z' SHOULD NOT BE ORDINAL-A-ONLY"     NC1744.2
168000                  TO RE-MARK                                      NC1744.2
168100             PERFORM FAIL                                         NC1744.2
168200             PERFORM PRINT-DETAIL.                                NC1744.2
168300*                                                                 NC1744.2
168400*                                                                 NC1744.2
168500 CLASS-INIT-41.                                                   NC1744.2
168600     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
168700          TO ANSI-REFERENCE.                                      NC1744.2
168800     MOVE   "CLASS-TEST-GF-41" TO PAR-NAME.                       NC1744.2
168900     MOVE   "ADCBA" TO WS-B.                                      NC1744.2
169000     GO TO   CLASS-TEST-GF-41.                                    NC1744.2
169100 CLASS-DELETE-GF-41.                                              NC1744.2
169200     PERFORM DE-LETE.                                             NC1744.2
169300     PERFORM PRINT-DETAIL.                                        NC1744.2
169400     GO TO   CLASS-INIT-42.                                       NC1744.2
169500 CLASS-TEST-GF-41.                                                NC1744.2
169600     IF      WS-B ORDINAL-A-THROUGH-D                             NC1744.2
169700             PERFORM PASS                                         NC1744.2
169800             PERFORM PRINT-DETAIL                                 NC1744.2
169900     ELSE                                                         NC1744.2
170000             MOVE   "'ADCBA' SHOULD BE ORDINAL-A-THROUGH-D"       NC1744.2
170100                  TO RE-MARK                                      NC1744.2
170200             PERFORM FAIL                                         NC1744.2
170300             PERFORM PRINT-DETAIL.                                NC1744.2
170400*                                                                 NC1744.2
170500*                                                                 NC1744.2
170600 CLASS-INIT-42.                                                   NC1744.2
170700     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
170800          TO ANSI-REFERENCE.                                      NC1744.2
170900     MOVE   "CLASS-TEST-GF-42" TO PAR-NAME.                       NC1744.2
171000     MOVE   "VWXYZ" TO WS-B.                                      NC1744.2
171100     GO TO   CLASS-TEST-GF-42.                                    NC1744.2
171200 CLASS-DELETE-GF-42.                                              NC1744.2
171300     PERFORM DE-LETE.                                             NC1744.2
171400     PERFORM PRINT-DETAIL.                                        NC1744.2
171500     GO TO   CLASS-INIT-43.                                       NC1744.2
171600 CLASS-TEST-GF-42.                                                NC1744.2
171700     IF      WS-B NOT ORDINAL-A-THROUGH-D                         NC1744.2
171800             PERFORM PASS                                         NC1744.2
171900             PERFORM PRINT-DETAIL                                 NC1744.2
172000     ELSE                                                         NC1744.2
172100             MOVE   "'VWXYZ' SHOULD NOT BE ORDINAL-A-THROUGH-D"   NC1744.2
172200                  TO RE-MARK                                      NC1744.2
172300             PERFORM FAIL                                         NC1744.2
172400             PERFORM PRINT-DETAIL.                                NC1744.2
172500*                                                                 NC1744.2
172600*                                                                 NC1744.2
172700 CLASS-INIT-43.                                                   NC1744.2
172800     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
172900          TO ANSI-REFERENCE.                                      NC1744.2
173000     MOVE   "CLASS-TEST-GF-43" TO PAR-NAME.                       NC1744.2
173100     MOVE   "ADCBA" TO WS-B.                                      NC1744.2
173200     GO TO   CLASS-TEST-GF-43.                                    NC1744.2
173300 CLASS-DELETE-GF-43.                                              NC1744.2
173400     PERFORM DE-LETE.                                             NC1744.2
173500     PERFORM PRINT-DETAIL.                                        NC1744.2
173600     GO TO   CLASS-INIT-44.                                       NC1744.2
173700 CLASS-TEST-GF-43.                                                NC1744.2
173800     IF      WS-B ORDINAL-D-THRU-A                                NC1744.2
173900             PERFORM PASS                                         NC1744.2
174000             PERFORM PRINT-DETAIL                                 NC1744.2
174100     ELSE                                                         NC1744.2
174200             MOVE   "'ADCBA' SHOULD BE ORDINAL-D-THRU-A"          NC1744.2
174300                  TO RE-MARK                                      NC1744.2
174400             PERFORM FAIL                                         NC1744.2
174500             PERFORM PRINT-DETAIL.                                NC1744.2
174600*                                                                 NC1744.2
174700*                                                                 NC1744.2
174800 CLASS-INIT-44.                                                   NC1744.2
174900     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
175000          TO ANSI-REFERENCE.                                      NC1744.2
175100     MOVE   "CLASS-TEST-GF-44" TO PAR-NAME.                       NC1744.2
175200     MOVE   "VWXYZ" TO WS-B.                                      NC1744.2
175300     GO TO   CLASS-TEST-GF-44.                                    NC1744.2
175400 CLASS-DELETE-GF-44.                                              NC1744.2
175500     PERFORM DE-LETE.                                             NC1744.2
175600     PERFORM PRINT-DETAIL.                                        NC1744.2
175700     GO TO   CLASS-INIT-45.                                       NC1744.2
175800 CLASS-TEST-GF-44.                                                NC1744.2
175900     IF      WS-B NOT ORDINAL-D-THRU-A                            NC1744.2
176000             PERFORM PASS                                         NC1744.2
176100             PERFORM PRINT-DETAIL                                 NC1744.2
176200     ELSE                                                         NC1744.2
176300             MOVE   "'VWXYZ' SHOULD NOT BE ORDINAL-D-THRU-A"      NC1744.2
176400                  TO RE-MARK                                      NC1744.2
176500             PERFORM FAIL                                         NC1744.2
176600             PERFORM PRINT-DETAIL.                                NC1744.2
176700*                                                                 NC1744.2
176800*                                                                 NC1744.2
176900 CLASS-INIT-45.                                                   NC1744.2
177000     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
177100          TO ANSI-REFERENCE.                                      NC1744.2
177200     MOVE   "CLASS-TEST-GF-45" TO PAR-NAME.                       NC1744.2
177300     MOVE   "A" TO WS-A.                                          NC1744.2
177400     GO TO   CLASS-TEST-GF-45.                                    NC1744.2
177500 CLASS-DELETE-GF-45.                                              NC1744.2
177600     PERFORM DE-LETE.                                             NC1744.2
177700     PERFORM PRINT-DETAIL.                                        NC1744.2
177800     GO TO   CLASS-INIT-46.                                       NC1744.2
177900 CLASS-TEST-GF-45.                                                NC1744.2
178000     IF      WS-A ACTUAL-A-ONLY                                   NC1744.2
178100             PERFORM PASS                                         NC1744.2
178200             PERFORM PRINT-DETAIL                                 NC1744.2
178300     ELSE                                                         NC1744.2
178400             MOVE   "'A' SHOULD BE ACTUAL-A-ONLY"                 NC1744.2
178500                  TO RE-MARK                                      NC1744.2
178600             PERFORM FAIL                                         NC1744.2
178700             PERFORM PRINT-DETAIL.                                NC1744.2
178800*                                                                 NC1744.2
178900*                                                                 NC1744.2
179000 CLASS-INIT-46.                                                   NC1744.2
179100     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
179200          TO ANSI-REFERENCE.                                      NC1744.2
179300     MOVE   "CLASS-TEST-GF-46" TO PAR-NAME.                       NC1744.2
179400     GO TO   CLASS-TEST-GF-46.                                    NC1744.2
179500 CLASS-DELETE-GF-46.                                              NC1744.2
179600     PERFORM DE-LETE.                                             NC1744.2
179700     PERFORM PRINT-DETAIL.                                        NC1744.2
179800     GO TO   CLASS-INIT-47.                                       NC1744.2
179900 CLASS-TEST-GF-46.                                                NC1744.2
180000     IF      DATA-Z NOT ACTUAL-A-ONLY                             NC1744.2
180100             PERFORM PASS                                         NC1744.2
180200             PERFORM PRINT-DETAIL                                 NC1744.2
180300     ELSE                                                         NC1744.2
180400             MOVE   "'Z' SHOULD NOT BE ACTUAL-A-ONLY"             NC1744.2
180500                  TO RE-MARK                                      NC1744.2
180600             PERFORM FAIL                                         NC1744.2
180700             PERFORM PRINT-DETAIL.                                NC1744.2
180800*                                                                 NC1744.2
180900*                                                                 NC1744.2
181000 CLASS-INIT-47.                                                   NC1744.2
181100     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
181200          TO ANSI-REFERENCE.                                      NC1744.2
181300     MOVE   "CLASS-TEST-GF-47" TO PAR-NAME.                       NC1744.2
181400     MOVE   "ADCBA" TO WS-B.                                      NC1744.2
181500     GO TO   CLASS-TEST-GF-47.                                    NC1744.2
181600 CLASS-DELETE-GF-47.                                              NC1744.2
181700     PERFORM DE-LETE.                                             NC1744.2
181800     PERFORM PRINT-DETAIL.                                        NC1744.2
181900     GO TO   CLASS-INIT-48.                                       NC1744.2
182000 CLASS-TEST-GF-47.                                                NC1744.2
182100     IF      WS-B ACTUAL-A-THRU-D                                 NC1744.2
182200             PERFORM PASS                                         NC1744.2
182300             PERFORM PRINT-DETAIL                                 NC1744.2
182400     ELSE                                                         NC1744.2
182500             MOVE   "'ADCBA' SHOULD BE ACTUAL-A-THRU-D"           NC1744.2
182600                  TO RE-MARK                                      NC1744.2
182700             PERFORM FAIL                                         NC1744.2
182800             PERFORM PRINT-DETAIL.                                NC1744.2
182900*                                                                 NC1744.2
183000*                                                                 NC1744.2
183100 CLASS-INIT-48.                                                   NC1744.2
183200     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
183300          TO ANSI-REFERENCE.                                      NC1744.2
183400     MOVE   "CLASS-TEST-GF-48" TO PAR-NAME.                       NC1744.2
183500     GO TO   CLASS-TEST-GF-48.                                    NC1744.2
183600 CLASS-DELETE-GF-48.                                              NC1744.2
183700     PERFORM DE-LETE.                                             NC1744.2
183800     PERFORM PRINT-DETAIL.                                        NC1744.2
183900     GO TO   CLASS-INIT-49.                                       NC1744.2
184000 CLASS-TEST-GF-48.                                                NC1744.2
184100     IF      DATA-VWXYZ NOT ACTUAL-A-THRU-D                       NC1744.2
184200             PERFORM PASS                                         NC1744.2
184300             PERFORM PRINT-DETAIL                                 NC1744.2
184400     ELSE                                                         NC1744.2
184500             MOVE   "'VWXYZ' SHOULD NOT BE ACTUAL-A-THRU-D"       NC1744.2
184600                  TO RE-MARK                                      NC1744.2
184700             PERFORM FAIL                                         NC1744.2
184800             PERFORM PRINT-DETAIL.                                NC1744.2
184900*                                                                 NC1744.2
185000*                                                                 NC1744.2
185100 CLASS-INIT-49.                                                   NC1744.2
185200     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
185300          TO ANSI-REFERENCE.                                      NC1744.2
185400     MOVE   "CLASS-TEST-GF-49" TO PAR-NAME.                       NC1744.2
185500     GO TO   CLASS-TEST-GF-49.                                    NC1744.2
185600 CLASS-DELETE-GF-49.                                              NC1744.2
185700     PERFORM DE-LETE.                                             NC1744.2
185800     PERFORM PRINT-DETAIL.                                        NC1744.2
185900     GO TO   CLASS-INIT-50.                                       NC1744.2
186000 CLASS-TEST-GF-49.                                                NC1744.2
186100     IF      DATA-ADCBA ACTUAL-D-THROUGH-A                        NC1744.2
186200             PERFORM PASS                                         NC1744.2
186300             PERFORM PRINT-DETAIL                                 NC1744.2
186400     ELSE                                                         NC1744.2
186500             MOVE   "'ADCBA' SHOULD BE ACTUAL-D-THROUGH-A"        NC1744.2
186600                  TO RE-MARK                                      NC1744.2
186700             PERFORM FAIL                                         NC1744.2
186800             PERFORM PRINT-DETAIL.                                NC1744.2
186900*                                                                 NC1744.2
187000*                                                                 NC1744.2
187100 CLASS-INIT-50.                                                   NC1744.2
187200     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
187300          TO ANSI-REFERENCE.                                      NC1744.2
187400     MOVE   "CLASS-TEST-GF-50" TO PAR-NAME.                       NC1744.2
187500     MOVE   "VWXYZ" TO WS-B.                                      NC1744.2
187600     GO TO   CLASS-TEST-GF-50.                                    NC1744.2
187700 CLASS-DELETE-GF-50.                                              NC1744.2
187800     PERFORM DE-LETE.                                             NC1744.2
187900     PERFORM PRINT-DETAIL.                                        NC1744.2
188000     GO TO   CLASS-INIT-51.                                       NC1744.2
188100 CLASS-TEST-GF-50.                                                NC1744.2
188200     IF      WS-B NOT ACTUAL-D-THROUGH-A                          NC1744.2
188300             PERFORM PASS                                         NC1744.2
188400             PERFORM PRINT-DETAIL                                 NC1744.2
188500     ELSE                                                         NC1744.2
188600             MOVE   "'VWXYZ' SHOULD NOT BE ACTUAL-D-THROUGH-A"    NC1744.2
188700                  TO RE-MARK                                      NC1744.2
188800             PERFORM FAIL                                         NC1744.2
188900             PERFORM PRINT-DETAIL.                                NC1744.2
189000*                                                                 NC1744.2
189100*                                                                 NC1744.2
189200 CLASS-INIT-51.                                                   NC1744.2
189300     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
189400          TO ANSI-REFERENCE.                                      NC1744.2
189500     MOVE   "CLASS-TEST-GF-51" TO PAR-NAME.                       NC1744.2
189600     MOVE   "ADCBA" TO WS-B.                                      NC1744.2
189700     GO TO   CLASS-TEST-GF-51.                                    NC1744.2
189800 CLASS-DELETE-GF-51.                                              NC1744.2
189900     PERFORM DE-LETE.                                             NC1744.2
190000     PERFORM PRINT-DETAIL.                                        NC1744.2
190100     GO TO   CLASS-INIT-52.                                       NC1744.2
190200 CLASS-TEST-GF-51.                                                NC1744.2
190300     IF      WS-B    ACTUAL-ABCD                                  NC1744.2
190400             PERFORM PASS                                         NC1744.2
190500             PERFORM PRINT-DETAIL                                 NC1744.2
190600     ELSE                                                         NC1744.2
190700             MOVE   "'ADCBA' SHOULD BE ACTUAL-ABCD"               NC1744.2
190800                  TO RE-MARK                                      NC1744.2
190900             PERFORM FAIL                                         NC1744.2
191000             PERFORM PRINT-DETAIL.                                NC1744.2
191100*                                                                 NC1744.2
191200*                                                                 NC1744.2
191300 CLASS-INIT-52.                                                   NC1744.2
191400     MOVE   "V1-17 4.5.4 (GR10) & VI-57 6.3.1.2"                  NC1744.2
191500          TO ANSI-REFERENCE.                                      NC1744.2
191600     MOVE   "CLASS-TEST-GF-52" TO PAR-NAME.                       NC1744.2
191700     GO TO   CLASS-TEST-GF-52.                                    NC1744.2
191800 CLASS-DELETE-GF-52.                                              NC1744.2
191900     PERFORM DE-LETE.                                             NC1744.2
192000     PERFORM PRINT-DETAIL.                                        NC1744.2
192100     GO TO   CCVS-EXIT.                                           NC1744.2
192200 CLASS-TEST-GF-52.                                                NC1744.2
192300     IF      DATA-VWXYZ NOT ACTUAL-ABCD                           NC1744.2
192400             PERFORM PASS                                         NC1744.2
192500             PERFORM PRINT-DETAIL                                 NC1744.2
192600     ELSE                                                         NC1744.2
192700             MOVE   "'VWXYZ' SHOULD NOT BE ACTUAL-ABCD"           NC1744.2
192800                  TO RE-MARK                                      NC1744.2
192900             PERFORM FAIL                                         NC1744.2
193000             PERFORM PRINT-DETAIL.                                NC1744.2
193100*                                                                 NC1744.2
193200*                                                                 NC1744.2
193300 CCVS-EXIT SECTION.                                               NC1744.2
193400 CCVS-999999.                                                     NC1744.2
193500     GO TO CLOSE-FILES.                                           NC1744.2
      *END-OF,NC174A                                                                  
