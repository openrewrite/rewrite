      *HEADER,COBOL,NC254A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2544.2
000200 PROGRAM-ID.                                                      NC2544.2
000300     NC254A.                                                      NC2544.2
000400****************************************************************  NC2544.2
000500*                                                              *  NC2544.2
000600*    VALIDATION FOR:-                                          *  NC2544.2
000700*                                                              *  NC2544.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2544.2
000900*                                                              *  NC2544.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2544.2
001100*                                                              *  NC2544.2
001200****************************************************************  NC2544.2
001300*                                                              *  NC2544.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2544.2
001500*                                                              *  NC2544.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2544.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2544.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2544.2
001900*                                                              *  NC2544.2
002000****************************************************************  NC2544.2
002100*                                                                 NC2544.2
002200*    PROGRAM NC254A TESTS SWITCH SETTINGS USING LEVEL 2 FEATURES  NC2544.2
002300*    LOGICAL OPERATORS AND, OR, NOT.                              NC2544.2
002400*                                                                 NC2544.2
002500 ENVIRONMENT DIVISION.                                            NC2544.2
002600 CONFIGURATION SECTION.                                           NC2544.2
002700 SOURCE-COMPUTER.                                                 NC2544.2
002800     XXXXX082.                                                    NC2544.2
002900 OBJECT-COMPUTER.                                                 NC2544.2
003000     XXXXX083.                                                    NC2544.2
003100ASPECIAL-NAMES.                                                   NC2544.2
003200A    XXXXX051                                                     NC2544.2
003300A    IS SW-1                                                      NC2544.2
003400A        ON STATUS IS ON-SWITCH-1                                 NC2544.2
003500A        OFF STATUS IS OFF-SWITCH-1                               NC2544.2
003600A    XXXXX052                                                     NC2544.2
003700A    IS SW-2                                                      NC2544.2
003800A          ON IS ON-SWITCH-2                                      NC2544.2
003900A          OFF IS OFF-SWITCH-2                                    NC2544.2
004000     CLASS   ORDINAL-A-ONLY IS                                    NC2544.2
004100     XXXXX090                                                     NC2544.2
004200     CLASS   ORDINAL-A-THROUGH-D IS                               NC2544.2
004300     XXXXX090                                                     NC2544.2
004400     THROUGH                                                      NC2544.2
004500     XXXXX091                                                     NC2544.2
004600     CLASS   ORDINAL-D-THRU-A                                     NC2544.2
004700     XXXXX091                                                     NC2544.2
004800     THRU                                                         NC2544.2
004900     XXXXX090                                                     NC2544.2
005000     CLASS   ACTUAL-A-ONLY "A"                                    NC2544.2
005100     CLASS   ACTUAL-A-THRU-D IS "A" THRU "D"                      NC2544.2
005200     CLASS   ACTUAL-D-THROUGH-A IS "D" THROUGH "A"                NC2544.2
005300     CLASS   ACTUAL-ABCD "ABCD".                                  NC2544.2
005400 INPUT-OUTPUT SECTION.                                            NC2544.2
005500 FILE-CONTROL.                                                    NC2544.2
005600     SELECT PRINT-FILE ASSIGN TO                                  NC2544.2
005700     XXXXX055.                                                    NC2544.2
005800 DATA DIVISION.                                                   NC2544.2
005900 FILE SECTION.                                                    NC2544.2
006000 FD  PRINT-FILE.                                                  NC2544.2
006100 01  PRINT-REC PICTURE X(120).                                    NC2544.2
006200 01  DUMMY-RECORD PICTURE X(120).                                 NC2544.2
006300 WORKING-STORAGE SECTION.                                         NC2544.2
006400 01  WS-A                        PIC X.                           NC2544.2
006500 01  WS-B                        PIC X(5).                        NC2544.2
006600 01  IF-D1                              PICTURE IS S9(4)V9(2)     NC2544.2
006700     VALUE IS 0.                                                  NC2544.2
006800 01  IF-D2                              PICTURE IS S9(4)V9(2)     NC2544.2
006900     VALUE IS ZERO.                                               NC2544.2
007000 01  IF-D3                              PICTURE IS X(10)          NC2544.2
007100     VALUE IS "0000000000".                                       NC2544.2
007200 01  IF-D4                              PICTURE IS X(15)          NC2544.2
007300     VALUE IS "               ".                                  NC2544.2
007400 01  IF-D6                              PICTURE IS A(10)          NC2544.2
007500     VALUE IS "BABABABABA".                                       NC2544.2
007600 01  IF-D7                              PICTURE IS S9(6)V9(4)     NC2544.2
007700     VALUE IS +123.45.                                            NC2544.2
007800 01  IF-D8                              PICTURE IS 9(6)V9(4)      NC2544.2
007900     VALUE IS 12300.                                              NC2544.2
008000 01  IF-D9                              PICTURE IS X(3)           NC2544.2
008100     VALUE IS "123".                                              NC2544.2
008200 01  IF-D11                             PICTURE IS X(6)           NC2544.2
008300     VALUE IS "ABCDEF".                                           NC2544.2
008400 01  IF-D13                             PICTURE IS 9(6)V9(4)      NC2544.2
008500     VALUE IS 12300.                                              NC2544.2
008600 01  IF-D14                             PICTURE IS S9(4)V9(2)     NC2544.2
008700     VALUE IS +123.45.                                            NC2544.2
008800 01  IF-D15                             PICTURE IS S999PP         NC2544.2
008900     VALUE IS 12300.                                              NC2544.2
009000 01  IF-D16                             PICTURE IS PP99           NC2544.2
009100     VALUE IS .0012.                                              NC2544.2
009200 01  IF-D17                             PICTURE IS SV9(4)         NC2544.2
009300     VALUE IS .0012.                                              NC2544.2
009400 01  IF-D18                             PICTURE IS X(10)          NC2544.2
009500     VALUE IS "BABABABABA".                                       NC2544.2
009600 01  IF-D19                             PICTURE IS X(10)          NC2544.2
009700     VALUE IS "ABCDEF    ".                                       NC2544.2
009800 01  IF-D23                             PICTURE IS $9,9B9.90+.    NC2544.2
009900 01  IF-D24                             PICTURE IS X(10)          NC2544.2
010000     VALUE IS "$1,2 3.40+".                                       NC2544.2
010100 01  IF-D25                             PICTURE IS ABABX0A.       NC2544.2
010200 01  IF-D26  PIC X(7)                                             NC2544.2
010300     VALUE IS "A C D0E".                                          NC2544.2
010400 01  IF-D27             PICTURE 9(6)V9(4)  VALUE 2137.45          NC2544.2
010500     USAGE IS COMPUTATIONAL.                                      NC2544.2
010600 01  IF-D28                             PICTURE IS 999999V9999    NC2544.2
010700     VALUE IS 2137.45.                                            NC2544.2
010800 01  IF-D32                             PICTURE IS 9 VALUE IS 0.  NC2544.2
010900 01  IF-D33 PICTURE S9 VALUE -0.                                  NC2544.2
011000 01  IF-D34 PICTURE S9 VALUE +0.                                  NC2544.2
011100 01  IF-D37             PICTURE 9(5)  VALUE 0001234.              NC2544.2
011200 01  IF-D38             PICTURE X(20) VALUE " BABBAGE".           NC2544.2
011300 01  ALPHA-UPPER        PIC X(20)     VALUE " UPPERCASE CHARS".   NC2544.2
011400 01  ALPHA-LOWER        PIC X(20)     VALUE " lowercase chars".   NC2544.2
011500 01  NON-COBOL-CHARACTERS  PICTURE X(8) VALUE                     NC2544.2
011600     XXXXX081.                                                    NC2544.2
011700 01  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC2544.2
011800 01  A18ONES-DS-18V00            PICTURE S9(18)                   NC2544.2
011900                                 VALUE 111111111111111111.        NC2544.2
012000 01  ONES-XN-00018               PICTURE X(18)                    NC2544.2
012100     VALUE "111111111111111111".                                  NC2544.2
012200 01  A99-DS-02V00                PICTURE S99  VALUE 99.           NC2544.2
012300 01  WRK-DU-02V00                PICTURE 99.                      NC2544.2
012400 01  TWOS-XN-00002               PICTURE XX   VALUE "22".         NC2544.2
012500 01  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC2544.2
012600                                 VALUE 111111111.111111111.       NC2544.2
012700 01  ONES-XN-00002               PICTURE XX   VALUE "11".         NC2544.2
012800 01  A02TWOS-DU-02V00            PICTURE 99   VALUE 22.           NC2544.2
012900 01  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC2544.2
013000 01  A990-DS-0201P               PICTURE S99P  VALUE +990.        NC2544.2
013100 01  XDATA-XN-00018              PICTURE X(18)                    NC2544.2
013200                                 VALUE "00ABCDEFGHI  4321 ".      NC2544.2
013300 01  XDATA-DS-18V00-S REDEFINES XDATA-XN-00018 PICTURE S9(18).    NC2544.2
013400 01  YADATA-XN-00010             PICTURE X(10) VALUE "ABCDEFGHIJ".NC2544.2
013500 01  YADATA-XN-00010-U-AND-L     PICTURE X(10) VALUE "AbCdEfGhIj".NC2544.2
013600 01  DUMMY-DS-00001     PICTURE S9 VALUE -1.                      NC2544.2
013700 01  A02TWOS-DS-03V02            PICTURE S999V99  VALUE +022.00.  NC2544.2
013800 01  WRK-DS-18V0-1               PIC S9(18)     VALUE             NC2544.2
013900            -123456789012345678.                                  NC2544.2
014000 01  WRK-XN-18-2                 PIC  X(18)     VALUE             NC2544.2
014100            "123456789012345678".                                 NC2544.2
014200                                                                  NC2544.2
014300 01  IF-D10.                                                      NC2544.2
014400     02 FILLER          PICTURE XX VALUE "01".                    NC2544.2
014500     02 FILLER          PICTURE XX VALUE "23".                    NC2544.2
014600     02 IF-D10A.                                                  NC2544.2
014700       03 FILLER        PICTURE XXXX VALUE "4567".                NC2544.2
014800       03 FILLER        PICTURE XXXX VALUE "8912".                NC2544.2
014900 01  IF-D12.                                                      NC2544.2
015000     02 FILLER          PICTURE XXX VALUE "ABC".                  NC2544.2
015100     02 IF-D12A.                                                  NC2544.2
015200       03 IF-D12B.                                                NC2544.2
015300         04 FILLER      PICTURE XX VALUE "DE".                    NC2544.2
015400         04 FILLER      PICTURE X  VALUE "F".                     NC2544.2
015500 01  IF-D20.                                                      NC2544.2
015600     02 FILLER          PICTURE 9(5) VALUE ZERO.                  NC2544.2
015700     02 FILLER          PICTURE 99   VALUE 12.                    NC2544.2
015800     02 FILLER          PICTURE 9    VALUE 3.                     NC2544.2
015900     02 FILLER          PICTURE 99   VALUE 45.                    NC2544.2
016000 01  IF-D21.                                                      NC2544.2
016100     02 FILLER          PICTURE 9(5) VALUE ZERO.                  NC2544.2
016200     02 FILLER          PICTURE 9(5) VALUE 12345.                 NC2544.2
016300 01  IF-D22.                                                      NC2544.2
016400     02 FILLER          PICTURE AA   VALUE "AB".                  NC2544.2
016500     02 FILLER          PICTURE AAAA VALUE "CDEF".                NC2544.2
016600 01  IF-D35.                                                      NC2544.2
016700     02 IF-D35A                             VALUE "*ASTERISK".    NC2544.2
016800       03 FILLER        PICTURE A(6).                             NC2544.2
016900       03 FILLER        PICTURE AAA.                              NC2544.2
017000     02 IF-D35B                            VALUE "/SLASH".        NC2544.2
017100       03 FILLER        PICTURE 9(6).                             NC2544.2
017200 01  IF-D36 REDEFINES IF-D35.                                     NC2544.2
017300     02 IF-D36A         PICTURE X(6).                             NC2544.2
017400     02 IF-D36B         PICTURE XXX.                              NC2544.2
017500     02 IF-D36C         PICTURE X(6).                             NC2544.2
017600 01  IF-D39.                                                      NC2544.2
017700     02  FILLER   PICTURE A(6) VALUE "ABCDEF".                    NC2544.2
017800     02  FILLER  PICTURE A(4) VALUE SPACE.                        NC2544.2
017900 01  LEVEL-01.                                                    NC2544.2
018000     02 LEVEL-02.                                                 NC2544.2
018100     03 LEVEL-03.                                                 NC2544.2
018200     04 LEVEL-04.                                                 NC2544.2
018300     05 LEVEL-05.                                                 NC2544.2
018400     06 LEVEL-06.                                                 NC2544.2
018500     07 LEVEL-07.                                                 NC2544.2
018600     08 LEVEL-08.                                                 NC2544.2
018700     09 LEVEL-09.                                                 NC2544.2
018800     10 LEVEL-10                        PICTURE IS X VALUE IS "R".NC2544.2
018900 01  LEVEL-RECEIVER                     PICTURE IS X VALUE IS     NC2544.2
019000     SPACE.                                                       NC2544.2
019100 01  LEVEL-SENDER PICTURE X VALUE "S".                            NC2544.2
019200 01  VAL                                PICTURE IS 9 VALUE IS 0.  NC2544.2
019300 01  A-2                                PICTURE IS A VALUE IS "A".NC2544.2
019400 01  N-27                               PICTURE IS 9999V9         NC2544.2
019500     VALUE IS 9999.9.                                             NC2544.2
019600 01  N-30                               PICTURE IS 9V9            NC2544.2
019700     VALUE IS 2.                                                  NC2544.2
019800 01  N-31                               PICTURE IS 9(6).          NC2544.2
019900 01  X-32 REDEFINES N-31                PICTURE IS X(6).          NC2544.2
020000 01  N-33                               PICTURE IS 9(5)           NC2544.2
020100     VALUE IS 29.                                                 NC2544.2
020200 01  A-37                               PICTURE IS A VALUE IS "X".NC2544.2
020300 01  X-38 REDEFINES A-37                PICTURE IS X.             NC2544.2
020400 01  X-43 PIC X(10) VALUE "    l75.63".                           NC2544.2
020500 01  N-84                               PICTURE IS 9999999999.    NC2544.2
020600 01  NUMERIC-GRP-TEST.                                            NC2544.2
020700     02  NUMERIC-1                PICTURE 9 VALUE 0.              NC2544.2
020800     02  NUMERIC-2.                                               NC2544.2
020900         03  NUMERIC-3            PICTURE 9(1)V9(1) VALUE ZERO.   NC2544.2
021000         03  NUMERIC-4.                                           NC2544.2
021100             04  NUMERIC-5       PICTURE 9(18) VALUE 1.           NC2544.2
021200     02  NUMERIC-6.                                               NC2544.2
021300         03  NUMERIC-7            PICTURE X VALUE "7".            NC2544.2
021400         03  NUMERIC-8            PICTURE 9  VALUE 8.             NC2544.2
021500 01  NUM-GRP.                                                     NC2544.2
021600     02  NUM-SUB-GRP  PIC 9.                                      NC2544.2
021700 01  GROUP-1000.                                                  NC2544.2
021800     02  FILLER  PIC X.                                           NC2544.2
021900     02  GROUP-X1000.                                             NC2544.2
022000         03  GROUP-1000-1 PIC X(500) VALUE ZERO.                  NC2544.2
022100         03  XNAME        PICTURE X(100) VALUE QUOTE.             NC2544.2
022200         03  GROUP-1000-2 PICTURE X(399) VALUE SPACE.             NC2544.2
022300         03  GROUP-1000-3 PICTURE X VALUE ".".                    NC2544.2
022400     02  GROUP-X500-2.                                            NC2544.2
022500         03  GROUP-X500-A        PICTURE X(500) VALUE ZERO.       NC2544.2
022600         03  GROUP-X500-1.                                        NC2544.2
022700             04  GROUP-X500-1-1  PICTURE X(50) VALUE QUOTE.       NC2544.2
022800             04  GROUP-X500-1-2  PICTURE X(50) VALUE QUOTE.       NC2544.2
022900             04  GROUP-X500-1-3  PICTURE X(398) VALUE SPACE.      NC2544.2
023000             04  GROUP-X500-1-4  PICTURE XX VALUE " .".           NC2544.2
023100 01  HI-LO-VALUES.                                                NC2544.2
023200     02  LOW-VAL  PIC X VALUE LOW-VALUE.                          NC2544.2
023300     02 ZERO-01  PICTURE 9(18) VALUE 1.                           NC2544.2
023400     02  ABC      PICTURE XXX VALUE "ABC".                        NC2544.2
023500     02  NINE-17-8 PICTURE 9(18) VALUE 999999999999999998.        NC2544.2
023600     02  ZERO-NULL PIC 9(9) VALUE 0.                              NC2544.2
023700     02  ZERO-ZERO PICTURE 9(9)V9(9) VALUE 0.0.                   NC2544.2
023800 01  COMP-DATA.                                                   NC2544.2
023900     02  COMP-DATA1 PICTURE 9(18) COMPUTATIONAL VALUE 300.        NC2544.2
024000     02  COMP-DATA2  PICTURE 9(10) COMPUTATIONAL VALUE  100000.   NC2544.2
024100     02  COMP-DATA3  PICTURE 9     COMPUTATIONAL VALUE 9.         NC2544.2
024200     02  COMP-DATA4  PICTURE 9(9)V9(7) COMPUTATIONAL VALUE 3.3.   NC2544.2
024300     02  COMP-DATA5  PICTURE 9(5)V9(2) COMPUTATIONAL VALUE 52.25. NC2544.2
024400     02  COMP-DATA6  PICTURE 9V9       COMPUTATIONAL VALUE 8.8.   NC2544.2
024500     02  COMP-DATA7  PICTURE 9(3)V9(2) COMPUTATIONAL VALUE 300.00.NC2544.2
024600     02  COMP-DATA8  PICTURE 9V9(9) COMPUTATIONAL VALUE 3.3000000.NC2544.2
024700     02  COMP-DATA9  PICTURE 9(8)  COMPUTATIONAL VALUE 100000.    NC2544.2
024800 01  DISP-DATA.                                                   NC2544.2
024900     02  DISP-DATA1  PICTURE 9(18) VALUE 300.                     NC2544.2
025000     02  DISP-DATA2  PICTURE 9(8)  VALUE 100000.                  NC2544.2
025100     02  DISP-DATA3  PICTURE 9     VALUE 9.                       NC2544.2
025200     02  DISP-DATA4  PICTURE 9(7)V9(9) VALUE 3.3.                 NC2544.2
025300     02  DISP-DATA5  PICTURE 9(2)V9(2) VALUE 52.25.               NC2544.2
025400     02  DISP-DATA6  PICTURE 9V9   VALUE 8.8.                     NC2544.2
025500 01  DATA-5          PICTURE 9     VALUE 5.                       NC2544.2
025600 01  DATA-99999      PICTURE S9(5) VALUE +99999.                  NC2544.2
025700 01  DATA-Z          PICTURE X     VALUE "Z".                     NC2544.2
025800 01  DATA-4          PICTURE 9     VALUE 4.                       NC2544.2
025900 01  DATA-Y          PICTURE X     VALUE "Y".                     NC2544.2
026000 01  DATA-VWXYZ      PICTURE X(5)  VALUE "VWXYZ".                 NC2544.2
026100 01  DATA-ADCBA      PICTURE X(5)  VALUE "ADCBA".                 NC2544.2
026200 01  TEST-RESULTS.                                                NC2544.2
026300     02 FILLER                   PIC X      VALUE SPACE.          NC2544.2
026400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2544.2
026500     02 FILLER                   PIC X      VALUE SPACE.          NC2544.2
026600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2544.2
026700     02 FILLER                   PIC X      VALUE SPACE.          NC2544.2
026800     02  PAR-NAME.                                                NC2544.2
026900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2544.2
027000       03  PARDOT-X              PIC X      VALUE SPACE.          NC2544.2
027100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2544.2
027200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2544.2
027300     02 RE-MARK                  PIC X(61).                       NC2544.2
027400 01  TEST-COMPUTED.                                               NC2544.2
027500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2544.2
027600     02 FILLER                   PIC X(17)  VALUE                 NC2544.2
027700            "       COMPUTED=".                                   NC2544.2
027800     02 COMPUTED-X.                                               NC2544.2
027900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2544.2
028000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2544.2
028100                                 PIC -9(9).9(9).                  NC2544.2
028200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2544.2
028300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2544.2
028400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2544.2
028500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2544.2
028600         04 COMPUTED-18V0                    PIC -9(18).          NC2544.2
028700         04 FILLER                           PIC X.               NC2544.2
028800     03 FILLER PIC X(50) VALUE SPACE.                             NC2544.2
028900 01  TEST-CORRECT.                                                NC2544.2
029000     02 FILLER PIC X(30) VALUE SPACE.                             NC2544.2
029100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2544.2
029200     02 CORRECT-X.                                                NC2544.2
029300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2544.2
029400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2544.2
029500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2544.2
029600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2544.2
029700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2544.2
029800     03      CR-18V0 REDEFINES CORRECT-A.                         NC2544.2
029900         04 CORRECT-18V0                     PIC -9(18).          NC2544.2
030000         04 FILLER                           PIC X.               NC2544.2
030100     03 FILLER PIC X(2) VALUE SPACE.                              NC2544.2
030200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2544.2
030300 01  CCVS-C-1.                                                    NC2544.2
030400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2544.2
030500-    "SS  PARAGRAPH-NAME                                          NC2544.2
030600-    "       REMARKS".                                            NC2544.2
030700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2544.2
030800 01  CCVS-C-2.                                                    NC2544.2
030900     02 FILLER                     PIC X        VALUE SPACE.      NC2544.2
031000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2544.2
031100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2544.2
031200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2544.2
031300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2544.2
031400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2544.2
031500 01  REC-CT                        PIC 99       VALUE ZERO.       NC2544.2
031600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2544.2
031700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2544.2
031800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2544.2
031900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2544.2
032000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2544.2
032100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2544.2
032200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2544.2
032300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2544.2
032400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2544.2
032500 01  CCVS-H-1.                                                    NC2544.2
032600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2544.2
032700     02  FILLER                    PIC X(42)    VALUE             NC2544.2
032800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2544.2
032900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2544.2
033000 01  CCVS-H-2A.                                                   NC2544.2
033100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2544.2
033200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2544.2
033300   02  FILLER                        PIC XXXX   VALUE             NC2544.2
033400     "4.2 ".                                                      NC2544.2
033500   02  FILLER                        PIC X(28)  VALUE             NC2544.2
033600            " COPY - NOT FOR DISTRIBUTION".                       NC2544.2
033700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2544.2
033800                                                                  NC2544.2
033900 01  CCVS-H-2B.                                                   NC2544.2
034000   02  FILLER                        PIC X(15)  VALUE             NC2544.2
034100            "TEST RESULT OF ".                                    NC2544.2
034200   02  TEST-ID                       PIC X(9).                    NC2544.2
034300   02  FILLER                        PIC X(4)   VALUE             NC2544.2
034400            " IN ".                                               NC2544.2
034500   02  FILLER                        PIC X(12)  VALUE             NC2544.2
034600     " HIGH       ".                                              NC2544.2
034700   02  FILLER                        PIC X(22)  VALUE             NC2544.2
034800            " LEVEL VALIDATION FOR ".                             NC2544.2
034900   02  FILLER                        PIC X(58)  VALUE             NC2544.2
035000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2544.2
035100 01  CCVS-H-3.                                                    NC2544.2
035200     02  FILLER                      PIC X(34)  VALUE             NC2544.2
035300            " FOR OFFICIAL USE ONLY    ".                         NC2544.2
035400     02  FILLER                      PIC X(58)  VALUE             NC2544.2
035500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2544.2
035600     02  FILLER                      PIC X(28)  VALUE             NC2544.2
035700            "  COPYRIGHT   1985 ".                                NC2544.2
035800 01  CCVS-E-1.                                                    NC2544.2
035900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2544.2
036000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2544.2
036100     02 ID-AGAIN                     PIC X(9).                    NC2544.2
036200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2544.2
036300 01  CCVS-E-2.                                                    NC2544.2
036400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2544.2
036500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2544.2
036600     02 CCVS-E-2-2.                                               NC2544.2
036700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2544.2
036800         03 FILLER                   PIC X      VALUE SPACE.      NC2544.2
036900         03 ENDER-DESC               PIC X(44)  VALUE             NC2544.2
037000            "ERRORS ENCOUNTERED".                                 NC2544.2
037100 01  CCVS-E-3.                                                    NC2544.2
037200     02  FILLER                      PIC X(22)  VALUE             NC2544.2
037300            " FOR OFFICIAL USE ONLY".                             NC2544.2
037400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2544.2
037500     02  FILLER                      PIC X(58)  VALUE             NC2544.2
037600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2544.2
037700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2544.2
037800     02 FILLER                       PIC X(15)  VALUE             NC2544.2
037900             " COPYRIGHT 1985".                                   NC2544.2
038000 01  CCVS-E-4.                                                    NC2544.2
038100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2544.2
038200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2544.2
038300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2544.2
038400     02 FILLER                       PIC X(40)  VALUE             NC2544.2
038500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2544.2
038600 01  XXINFO.                                                      NC2544.2
038700     02 FILLER                       PIC X(19)  VALUE             NC2544.2
038800            "*** INFORMATION ***".                                NC2544.2
038900     02 INFO-TEXT.                                                NC2544.2
039000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2544.2
039100       04 XXCOMPUTED                 PIC X(20).                   NC2544.2
039200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2544.2
039300       04 XXCORRECT                  PIC X(20).                   NC2544.2
039400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2544.2
039500 01  HYPHEN-LINE.                                                 NC2544.2
039600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2544.2
039700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2544.2
039800-    "*****************************************".                 NC2544.2
039900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2544.2
040000-    "******************************".                            NC2544.2
040100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2544.2
040200     "NC254A".                                                    NC2544.2
040300 PROCEDURE DIVISION.                                              NC2544.2
040400 CCVS1 SECTION.                                                   NC2544.2
040500 OPEN-FILES.                                                      NC2544.2
040600     OPEN     OUTPUT PRINT-FILE.                                  NC2544.2
040700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2544.2
040800     MOVE    SPACE TO TEST-RESULTS.                               NC2544.2
040900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2544.2
041000     GO TO CCVS1-EXIT.                                            NC2544.2
041100 CLOSE-FILES.                                                     NC2544.2
041200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2544.2
041300 TERMINATE-CCVS.                                                  NC2544.2
041400*    EXIT PROGRAM.                                                NC2544.2
041500*TERMINATE-CALL.                                                  NC2544.2
041600     STOP     RUN.                                                NC2544.2
041700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2544.2
041800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2544.2
041900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2544.2
042000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2544.2
042100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2544.2
042200 PRINT-DETAIL.                                                    NC2544.2
042300     IF REC-CT NOT EQUAL TO ZERO                                  NC2544.2
042400             MOVE "." TO PARDOT-X                                 NC2544.2
042500             MOVE REC-CT TO DOTVALUE.                             NC2544.2
042600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2544.2
042700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2544.2
042800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2544.2
042900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2544.2
043000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2544.2
043100     MOVE SPACE TO CORRECT-X.                                     NC2544.2
043200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2544.2
043300     MOVE     SPACE TO RE-MARK.                                   NC2544.2
043400 HEAD-ROUTINE.                                                    NC2544.2
043500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2544.2
043600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2544.2
043700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2544.2
043800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2544.2
043900 COLUMN-NAMES-ROUTINE.                                            NC2544.2
044000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2544.2
044100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2544.2
044200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2544.2
044300 END-ROUTINE.                                                     NC2544.2
044400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2544.2
044500 END-RTN-EXIT.                                                    NC2544.2
044600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2544.2
044700 END-ROUTINE-1.                                                   NC2544.2
044800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2544.2
044900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2544.2
045000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2544.2
045100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2544.2
045200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2544.2
045300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2544.2
045400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2544.2
045500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2544.2
045600  END-ROUTINE-12.                                                 NC2544.2
045700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2544.2
045800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2544.2
045900         MOVE "NO " TO ERROR-TOTAL                                NC2544.2
046000         ELSE                                                     NC2544.2
046100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2544.2
046200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2544.2
046300     PERFORM WRITE-LINE.                                          NC2544.2
046400 END-ROUTINE-13.                                                  NC2544.2
046500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2544.2
046600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2544.2
046700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2544.2
046800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2544.2
046900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2544.2
047000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2544.2
047100          MOVE "NO " TO ERROR-TOTAL                               NC2544.2
047200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2544.2
047300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2544.2
047400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2544.2
047500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2544.2
047600 WRITE-LINE.                                                      NC2544.2
047700     ADD 1 TO RECORD-COUNT.                                       NC2544.2
047800*    IF RECORD-COUNT GREATER 42                                   NC2544.2
047900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2544.2
048000*        MOVE SPACE TO DUMMY-RECORD                               NC2544.2
048100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2544.2
048200*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC2544.2
048300*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC2544.2
048400*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC2544.2
048500*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC2544.2
048600*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC2544.2
048700*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC2544.2
048800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2544.2
048900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2544.2
049000*        MOVE ZERO TO RECORD-COUNT.                               NC2544.2
049100     PERFORM WRT-LN.                                              NC2544.2
049200 WRT-LN.                                                          NC2544.2
049300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2544.2
049400     MOVE SPACE TO DUMMY-RECORD.                                  NC2544.2
049500 BLANK-LINE-PRINT.                                                NC2544.2
049600     PERFORM WRT-LN.                                              NC2544.2
049700 FAIL-ROUTINE.                                                    NC2544.2
049800     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC2544.2
049900            GO TO FAIL-ROUTINE-WRITE.                             NC2544.2
050000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2544.2
050100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2544.2
050200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2544.2
050300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2544.2
050400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2544.2
050500     GO TO  FAIL-ROUTINE-EX.                                      NC2544.2
050600 FAIL-ROUTINE-WRITE.                                              NC2544.2
050700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2544.2
050800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2544.2
050900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2544.2
051000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2544.2
051100 FAIL-ROUTINE-EX. EXIT.                                           NC2544.2
051200 BAIL-OUT.                                                        NC2544.2
051300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2544.2
051400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2544.2
051500 BAIL-OUT-WRITE.                                                  NC2544.2
051600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2544.2
051700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2544.2
051800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2544.2
051900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2544.2
052000 BAIL-OUT-EX. EXIT.                                               NC2544.2
052100 CCVS1-EXIT.                                                      NC2544.2
052200     EXIT.                                                        NC2544.2
052300 SECT-NC254A-001 SECTION.                                         NC2544.2
052400*                                                                 NC2544.2
052500*                                                                 NC2544.2
052600 NEXT-INIT-GF-1.                                                  NC2544.2
052700*    ==-->     NEXT SENTENCE           <--==                      NC2544.2
052800     MOVE   "V1-89 6.15.4 GR2 " TO ANSI-REFERENCE.                NC2544.2
052900     MOVE    "A" TO A-2.                                          NC2544.2
053000 NEXT-TEST-GF-1.                                                  NC2544.2
053100     IF       A-2 EQUAL TO "A"                                    NC2544.2
053200              NEXT SENTENCE                                       NC2544.2
053300              ELSE                                                NC2544.2
053400              NEXT SENTENCE.                                      NC2544.2
053500     PERFORM  PASS.                                               NC2544.2
053600     GO TO    NEXT-WRITE-GF-1.                                    NC2544.2
053700 NEXT-DELETE-GF-1.                                                NC2544.2
053800     PERFORM  DE-LETE.                                            NC2544.2
053900 NEXT-WRITE-GF-1.                                                 NC2544.2
054000     MOVE "NEXT-TEST-1" TO PAR-NAME.                              NC2544.2
054100     PERFORM  PRINT-DETAIL.                                       NC2544.2
054200*                                                                 NC2544.2
054300*                                                                 NC2544.2
054400 ANOTHER-REMARK.                                                  NC2544.2
054500     MOVE     SPACE TO TEST-RESULTS.                              NC2544.2
054600     MOVE "THE FOLLOWING TESTS        " TO RE-MARK.               NC2544.2
054700     PERFORM  PRINT-DETAIL.                                       NC2544.2
054800     MOVE "TEST THE COMPARISONS IN    " TO RE-MARK.               NC2544.2
054900     PERFORM  PRINT-DETAIL.                                       NC2544.2
055000     MOVE "SWITCH-STATUS, RELATION    " TO RE-MARK.               NC2544.2
055100     PERFORM  PRINT-DETAIL.                                       NC2544.2
055200     MOVE "AND CLASS CONDITIONALS.    " TO RE-MARK.               NC2544.2
055300     PERFORM  PRINT-DETAIL.                                       NC2544.2
055400 SWH-INIT-GF-1.                                                   NC2544.2
055500     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC2544.2
055600     MOVE "SWITCH-STATUS" TO FEATURE.                             NC2544.2
055700 SWH-TEST-GF-1.                                                   NC2544.2
055800A    IF      ON-SWITCH-1                                          NC2544.2
055900A            PERFORM PASS                                         NC2544.2
056000A    ELSE                                                         NC2544.2
056100A            PERFORM FAIL.                                        NC2544.2
056200A    GO TO   SWH-WRITE-GF-1.                                      NC2544.2
056300 SWH-DELETE-GF-1.                                                 NC2544.2
056400B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
056500     PERFORM  DE-LETE.                                            NC2544.2
056600 SWH-WRITE-GF-1.                                                  NC2544.2
056700     MOVE "SWH-TEST-GF-1" TO PAR-NAME.                            NC2544.2
056800     PERFORM  PRINT-DETAIL.                                       NC2544.2
056900 SWH-INIT-GF-2.                                                   NC2544.2
057000     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC2544.2
057100 SWH-TEST-GF-2.                                                   NC2544.2
057200A    IF       OFF-SWITCH-1                                        NC2544.2
057300A             PERFORM FAIL                                        NC2544.2
057400A             ELSE                                                NC2544.2
057500A             PERFORM PASS.                                       NC2544.2
057600A    GO TO    SWH-WRITE-GF-2.                                     NC2544.2
057700 SWH-DELETE-GF-2.                                                 NC2544.2
057800B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
057900     PERFORM  DE-LETE.                                            NC2544.2
058000 SWH-WRITE-GF-2.                                                  NC2544.2
058100     MOVE "SWH-TEST-GF-2" TO PAR-NAME.                            NC2544.2
058200     PERFORM  PRINT-DETAIL.                                       NC2544.2
058300 SWH-INIT-GF-3.                                                   NC2544.2
058400     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC2544.2
058500 SWH-TEST-GF-3.                                                   NC2544.2
058600A    IF       OFF-SWITCH-2                                        NC2544.2
058700A             PERFORM PASS                                        NC2544.2
058800A             ELSE                                                NC2544.2
058900A             PERFORM FAIL.                                       NC2544.2
059000A    GO TO    SWH-WRITE-GF-3.                                     NC2544.2
059100 SWH-DELETE-GF-3.                                                 NC2544.2
059200B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
059300     PERFORM  DE-LETE.                                            NC2544.2
059400 SWH-WRITE-GF-3.                                                  NC2544.2
059500     MOVE "SWH-TEST-GF-3" TO PAR-NAME.                            NC2544.2
059600     PERFORM  PRINT-DETAIL.                                       NC2544.2
059700 SWH-INIT-GF-4.                                                   NC2544.2
059800     MOVE   "V1-13 4.5.2" TO ANSI-REFERENCE.                      NC2544.2
059900 SWH-TEST-GF-4.                                                   NC2544.2
060000A    IF       ON-SWITCH-2                                         NC2544.2
060100A             PERFORM FAIL                                        NC2544.2
060200A             ELSE                                                NC2544.2
060300A             PERFORM PASS.                                       NC2544.2
060400A    GO TO    SWH-WRITE-GF-4.                                     NC2544.2
060500 SWH-DELETE-GF-4.                                                 NC2544.2
060600B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
060700     PERFORM  DE-LETE.                                            NC2544.2
060800 SWH-WRITE-GF-4.                                                  NC2544.2
060900     MOVE "SWH-TEST-GF-4" TO PAR-NAME.                            NC2544.2
061000     PERFORM  PRINT-DETAIL.                                       NC2544.2
061100 SWH-TEST-5.                                                      NC2544.2
061200A    IF NOT ON-SWITCH-1                                           NC2544.2
061300A        MOVE "SWITCH-1  OFF " TO COMPUTED-A                      NC2544.2
061400A        MOVE "SWITCH-1 EXPECTED ON" TO CORRECT-A                 NC2544.2
061500A        PERFORM FAIL                                             NC2544.2
061600A        GO TO SWH-WRITE-5.                                       NC2544.2
061700A    PERFORM PASS.                                                NC2544.2
061800A    GO TO SWH-WRITE-5.                                           NC2544.2
061900 SWH-DELETE-5.                                                    NC2544.2
062000B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
062100     PERFORM DE-LETE.                                             NC2544.2
062200 SWH-WRITE-5.                                                     NC2544.2
062300     MOVE "SWH-TEST-5" TO PAR-NAME.                               NC2544.2
062400     PERFORM PRINT-DETAIL.                                        NC2544.2
062500 SWH-TEST-6.                                                      NC2544.2
062600A    IF NOT OFF-SWITCH-1                                          NC2544.2
062700A        PERFORM PASS                                             NC2544.2
062800A        GO TO SWH-WRITE-6.                                       NC2544.2
062900A    MOVE "SWITCH-1  OFF " TO COMPUTED-A.                         NC2544.2
063000A    MOVE "SWITCH-1 EXPECTED ON" TO CORRECT-A.                    NC2544.2
063100A    PERFORM FAIL.                                                NC2544.2
063200A    GO TO SWH-WRITE-6.                                           NC2544.2
063300 SWH-DELETE-6.                                                    NC2544.2
063400B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
063500     PERFORM DE-LETE.                                             NC2544.2
063600 SWH-WRITE-6.                                                     NC2544.2
063700     MOVE "SWH-TEST-6" TO PAR-NAME.                               NC2544.2
063800     PERFORM PRINT-DETAIL.                                        NC2544.2
063900 SWH-TEST-7.                                                      NC2544.2
064000A    IF NOT ON-SWITCH-2                                           NC2544.2
064100A        PERFORM PASS                                             NC2544.2
064200A        GO TO SWH-WRITE-7.                                       NC2544.2
064300A    MOVE "SWITCH-2  ON " TO COMPUTED-A.                          NC2544.2
064400A    MOVE "SWITCH2 EXPECTED OFF" TO CORRECT-A.                    NC2544.2
064500A    PERFORM FAIL.                                                NC2544.2
064600A    GO TO SWH-WRITE-7.                                           NC2544.2
064700 SWH-DELETE-7.                                                    NC2544.2
064800B    MOVE "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                  NC2544.2
064900     PERFORM DE-LETE.                                             NC2544.2
065000 SWH-WRITE-7.                                                     NC2544.2
065100     MOVE "SWH-TEST-7" TO PAR-NAME.                               NC2544.2
065200     PERFORM PRINT-DETAIL.                                        NC2544.2
065300 SWH-TEST-8.                                                      NC2544.2
065400A    IF NOT OFF-SWITCH-2                                          NC2544.2
065500A        MOVE "SWITCH-2  ON " TO COMPUTED-A                       NC2544.2
065600A        MOVE "SWITCH2 EXPECTED OFF" TO CORRECT-A                 NC2544.2
065700A        PERFORM FAIL                                             NC2544.2
065800A        GO TO SWH-WRITE-8.                                       NC2544.2
065900A    PERFORM PASS.                                                NC2544.2
066000A    GO TO  SWH-WRITE-8.                                          NC2544.2
066100 SWH-DELETE-8.                                                    NC2544.2
066200B    MOVE  "SWITCHES NOT IMPLEMENTED" TO RE-MARK.                 NC2544.2
066300     PERFORM DE-LETE.                                             NC2544.2
066400 SWH-WRITE-8.                                                     NC2544.2
066500     MOVE  "SWH-TEST-8" TO PAR-NAME.                              NC2544.2
066600     PERFORM PRINT-DETAIL.                                        NC2544.2
066700*                                                                 NC2544.2
066800*                                                                 NC2544.2
066900 CCVS-EXIT SECTION.                                               NC2544.2
067000 CCVS-999999.                                                     NC2544.2
067100     GO TO CLOSE-FILES.                                           NC2544.2
      *END-OF,NC254A                                                                  
