      *HEADER,COBOL,NC103A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1034.2
000200 PROGRAM-ID.                                                      NC1034.2
000300     NC103A.                                                      NC1034.2
000400****************************************************************  NC1034.2
000500*                                                              *  NC1034.2
000600*    VALIDATION FOR:-                                          *  NC1034.2
000700*                                                              *  NC1034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1034.2
000900*                                                              *  NC1034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1034.2
001100*                                                              *  NC1034.2
001200****************************************************************  NC1034.2
001300*                                                              *  NC1034.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1034.2
001500*                                                              *  NC1034.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1034.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1034.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1034.2
001900*                                                              *  NC1034.2
002000****************************************************************  NC1034.2
002100*                                                                 NC1034.2
002200*    PROGRAM NC103A TESTS THE GENERAL FORMAT OF THE "IF"          NC1034.2
002300*    STATEMENT AND  "NEXT SENTENCE".                              NC1034.2
002400*                                                                 NC1034.2
002500 ENVIRONMENT DIVISION.                                            NC1034.2
002600 CONFIGURATION SECTION.                                           NC1034.2
002700 SOURCE-COMPUTER.                                                 NC1034.2
002800     XXXXX082.                                                    NC1034.2
002900 OBJECT-COMPUTER.                                                 NC1034.2
003000     XXXXX083.                                                    NC1034.2
003100 INPUT-OUTPUT SECTION.                                            NC1034.2
003200 FILE-CONTROL.                                                    NC1034.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC1034.2
003400     XXXXX055.                                                    NC1034.2
003500 DATA DIVISION.                                                   NC1034.2
003600 FILE SECTION.                                                    NC1034.2
003700 FD  PRINT-FILE.                                                  NC1034.2
003800 01  PRINT-REC PICTURE X(120).                                    NC1034.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC1034.2
004000 WORKING-STORAGE SECTION.                                         NC1034.2
004100 01  TEST-RESULTS.                                                NC1034.2
004200     02 FILLER                   PIC X      VALUE SPACE.          NC1034.2
004300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1034.2
004400     02 FILLER                   PIC X      VALUE SPACE.          NC1034.2
004500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1034.2
004600     02 FILLER                   PIC X      VALUE SPACE.          NC1034.2
004700     02  PAR-NAME.                                                NC1034.2
004800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1034.2
004900       03  PARDOT-X              PIC X      VALUE SPACE.          NC1034.2
005000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1034.2
005100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1034.2
005200     02 RE-MARK                  PIC X(61).                       NC1034.2
005300 01  TEST-COMPUTED.                                               NC1034.2
005400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1034.2
005500     02 FILLER                   PIC X(17)  VALUE                 NC1034.2
005600            "       COMPUTED=".                                   NC1034.2
005700     02 COMPUTED-X.                                               NC1034.2
005800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1034.2
005900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1034.2
006000                                 PIC -9(9).9(9).                  NC1034.2
006100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1034.2
006200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1034.2
006300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1034.2
006400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1034.2
006500         04 COMPUTED-18V0                    PIC -9(18).          NC1034.2
006600         04 FILLER                           PIC X.               NC1034.2
006700     03 FILLER PIC X(50) VALUE SPACE.                             NC1034.2
006800 01  TEST-CORRECT.                                                NC1034.2
006900     02 FILLER PIC X(30) VALUE SPACE.                             NC1034.2
007000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1034.2
007100     02 CORRECT-X.                                                NC1034.2
007200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1034.2
007300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1034.2
007400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1034.2
007500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1034.2
007600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1034.2
007700     03      CR-18V0 REDEFINES CORRECT-A.                         NC1034.2
007800         04 CORRECT-18V0                     PIC -9(18).          NC1034.2
007900         04 FILLER                           PIC X.               NC1034.2
008000     03 FILLER PIC X(2) VALUE SPACE.                              NC1034.2
008100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1034.2
008200 01  CCVS-C-1.                                                    NC1034.2
008300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1034.2
008400-    "SS  PARAGRAPH-NAME                                          NC1034.2
008500-    "       REMARKS".                                            NC1034.2
008600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1034.2
008700 01  CCVS-C-2.                                                    NC1034.2
008800     02 FILLER                     PIC X        VALUE SPACE.      NC1034.2
008900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1034.2
009000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1034.2
009100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1034.2
009200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1034.2
009300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1034.2
009400 01  REC-CT                        PIC 99       VALUE ZERO.       NC1034.2
009500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1034.2
009600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1034.2
009700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1034.2
009800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1034.2
009900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1034.2
010000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1034.2
010100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1034.2
010200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1034.2
010300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1034.2
010400 01  CCVS-H-1.                                                    NC1034.2
010500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1034.2
010600     02  FILLER                    PIC X(42)    VALUE             NC1034.2
010700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1034.2
010800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1034.2
010900 01  CCVS-H-2A.                                                   NC1034.2
011000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1034.2
011100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1034.2
011200   02  FILLER                        PIC XXXX   VALUE             NC1034.2
011300     "4.2 ".                                                      NC1034.2
011400   02  FILLER                        PIC X(28)  VALUE             NC1034.2
011500            " COPY - NOT FOR DISTRIBUTION".                       NC1034.2
011600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1034.2
011700                                                                  NC1034.2
011800 01  CCVS-H-2B.                                                   NC1034.2
011900   02  FILLER                        PIC X(15)  VALUE             NC1034.2
012000            "TEST RESULT OF ".                                    NC1034.2
012100   02  TEST-ID                       PIC X(9).                    NC1034.2
012200   02  FILLER                        PIC X(4)   VALUE             NC1034.2
012300            " IN ".                                               NC1034.2
012400   02  FILLER                        PIC X(12)  VALUE             NC1034.2
012500     " HIGH       ".                                              NC1034.2
012600   02  FILLER                        PIC X(22)  VALUE             NC1034.2
012700            " LEVEL VALIDATION FOR ".                             NC1034.2
012800   02  FILLER                        PIC X(58)  VALUE             NC1034.2
012900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1034.2
013000 01  CCVS-H-3.                                                    NC1034.2
013100     02  FILLER                      PIC X(34)  VALUE             NC1034.2
013200            " FOR OFFICIAL USE ONLY    ".                         NC1034.2
013300     02  FILLER                      PIC X(58)  VALUE             NC1034.2
013400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1034.2
013500     02  FILLER                      PIC X(28)  VALUE             NC1034.2
013600            "  COPYRIGHT   1985 ".                                NC1034.2
013700 01  CCVS-E-1.                                                    NC1034.2
013800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1034.2
013900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1034.2
014000     02 ID-AGAIN                     PIC X(9).                    NC1034.2
014100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1034.2
014200 01  CCVS-E-2.                                                    NC1034.2
014300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1034.2
014400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1034.2
014500     02 CCVS-E-2-2.                                               NC1034.2
014600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1034.2
014700         03 FILLER                   PIC X      VALUE SPACE.      NC1034.2
014800         03 ENDER-DESC               PIC X(44)  VALUE             NC1034.2
014900            "ERRORS ENCOUNTERED".                                 NC1034.2
015000 01  CCVS-E-3.                                                    NC1034.2
015100     02  FILLER                      PIC X(22)  VALUE             NC1034.2
015200            " FOR OFFICIAL USE ONLY".                             NC1034.2
015300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1034.2
015400     02  FILLER                      PIC X(58)  VALUE             NC1034.2
015500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1034.2
015600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1034.2
015700     02 FILLER                       PIC X(15)  VALUE             NC1034.2
015800             " COPYRIGHT 1985".                                   NC1034.2
015900 01  CCVS-E-4.                                                    NC1034.2
016000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1034.2
016100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1034.2
016200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1034.2
016300     02 FILLER                       PIC X(40)  VALUE             NC1034.2
016400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1034.2
016500 01  XXINFO.                                                      NC1034.2
016600     02 FILLER                       PIC X(19)  VALUE             NC1034.2
016700            "*** INFORMATION ***".                                NC1034.2
016800     02 INFO-TEXT.                                                NC1034.2
016900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1034.2
017000       04 XXCOMPUTED                 PIC X(20).                   NC1034.2
017100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1034.2
017200       04 XXCORRECT                  PIC X(20).                   NC1034.2
017300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1034.2
017400 01  HYPHEN-LINE.                                                 NC1034.2
017500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1034.2
017600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1034.2
017700-    "*****************************************".                 NC1034.2
017800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1034.2
017900-    "******************************".                            NC1034.2
018000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1034.2
018100     "NC103A".                                                    NC1034.2
018200 01  IF-D1                              PICTURE IS S9(4)V9(2)     NC1034.2
018300     VALUE IS 0.                                                  NC1034.2
018400 01  IF-D2                              PICTURE IS S9(4)V9(2)     NC1034.2
018500     VALUE IS ZERO.                                               NC1034.2
018600 01  IF-D3                              PICTURE IS X(10)          NC1034.2
018700     VALUE IS "0000000000".                                       NC1034.2
018800 01  IF-D4                              PICTURE IS X(15)          NC1034.2
018900     VALUE IS "               ".                                  NC1034.2
019000 01  IF-D6                              PICTURE IS A(10)          NC1034.2
019100     VALUE IS "BABABABABA".                                       NC1034.2
019200 01  IF-D7                              PICTURE IS S9(6)V9(4)     NC1034.2
019300     VALUE IS +123.45.                                            NC1034.2
019400 01  IF-D8                              PICTURE IS 9(6)V9(4)      NC1034.2
019500     VALUE IS 12300.                                              NC1034.2
019600 01  IF-D9                              PICTURE IS X(3)           NC1034.2
019700     VALUE IS "123".                                              NC1034.2
019800 01  IF-D11                             PICTURE IS X(6)           NC1034.2
019900     VALUE IS "ABCDEF".                                           NC1034.2
020000 01  IF-D13                             PICTURE IS 9(6)V9(4)      NC1034.2
020100     VALUE IS 12300.                                              NC1034.2
020200 01  IF-D14                             PICTURE IS S9(4)V9(2)     NC1034.2
020300     VALUE IS +123.45.                                            NC1034.2
020400 01  IF-D15                             PICTURE IS S999PP         NC1034.2
020500     VALUE IS 12300.                                              NC1034.2
020600 01  IF-D16                             PICTURE IS PP99           NC1034.2
020700     VALUE IS .0012.                                              NC1034.2
020800 01  IF-D17                             PICTURE IS SV9(4)         NC1034.2
020900     VALUE IS .0012.                                              NC1034.2
021000 01  IF-D18                             PICTURE IS X(10)          NC1034.2
021100     VALUE IS "BABABABABA".                                       NC1034.2
021200 01  IF-D19                             PICTURE IS X(10)          NC1034.2
021300     VALUE IS "ABCDEF    ".                                       NC1034.2
021400 01  IF-D23                             PICTURE IS $9,9B9.90+.    NC1034.2
021500 01  IF-D24                             PICTURE IS X(10)          NC1034.2
021600     VALUE IS "$1,2 3.40+".                                       NC1034.2
021700 01  IF-D25                             PICTURE IS ABABX0A.       NC1034.2
021800 01  IF-D26  PIC X(7)                                             NC1034.2
021900     VALUE IS "A C D0E".                                          NC1034.2
022000 01  IF-D27             PICTURE 9(6)V9(4)  VALUE 2137.45          NC1034.2
022100     USAGE IS COMPUTATIONAL.                                      NC1034.2
022200 01  IF-D28                             PICTURE IS 999999V9999    NC1034.2
022300     VALUE IS 2137.45.                                            NC1034.2
022400 01  IF-D32                             PICTURE IS 9 VALUE IS 0.  NC1034.2
022500 01  IF-D33 PICTURE S9 VALUE -0.                                  NC1034.2
022600 01  IF-D34 PICTURE S9 VALUE +0.                                  NC1034.2
022700 01  IF-D37             PICTURE 9(5)  VALUE 0001234.              NC1034.2
022800 01  IF-D38             PICTURE X(20) VALUE " BABBAGE".           NC1034.2
022900 01  ALPHA-UPPER        PIC X(20)     VALUE " UPPERCASE CHARS".   NC1034.2
023000 01  ALPHA-LOWER        PIC X(20)     VALUE " lowercase chars".   NC1034.2
023100 01  NON-COBOL-CHARACTERS  PICTURE X(8) VALUE                     NC1034.2
023200     XXXXX081.                                                    NC1034.2
023300 01  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1034.2
023400 01  A18ONES-DS-18V00            PICTURE S9(18)                   NC1034.2
023500                                 VALUE 111111111111111111.        NC1034.2
023600 01  ONES-XN-00018               PICTURE X(18)                    NC1034.2
023700     VALUE "111111111111111111".                                  NC1034.2
023800 01  A99-DS-02V00                PICTURE S99  VALUE 99.           NC1034.2
023900 01  WRK-DU-02V00                PICTURE 99.                      NC1034.2
024000 01  TWOS-XN-00002               PICTURE XX   VALUE "22".         NC1034.2
024100 01  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1034.2
024200                                 VALUE 111111111.111111111.       NC1034.2
024300 01  ONES-XN-00002               PICTURE XX   VALUE "11".         NC1034.2
024400 01  A02TWOS-DU-02V00            PICTURE 99   VALUE 22.           NC1034.2
024500 01  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1034.2
024600 01  A990-DS-0201P               PICTURE S99P  VALUE +990.        NC1034.2
024700 01  XDATA-XN-00018              PICTURE X(18)                    NC1034.2
024800                                 VALUE "00ABCDEFGHI  4321 ".      NC1034.2
024900 01  XDATA-DS-18V00-S REDEFINES XDATA-XN-00018 PICTURE S9(18).    NC1034.2
025000 01  YADATA-XN-00010             PICTURE X(10) VALUE "ABCDEFGHIJ".NC1034.2
025100 01  YADATA-XN-00010-U-AND-L     PICTURE X(10) VALUE "AbCdEfGhIj".NC1034.2
025200 01  DUMMY-DS-00001     PICTURE S9 VALUE -1.                      NC1034.2
025300 01  A02TWOS-DS-03V02            PICTURE S999V99  VALUE +022.00.  NC1034.2
025400 01  WRK-DS-18V0-1               PIC S9(18)     VALUE             NC1034.2
025500            -123456789012345678.                                  NC1034.2
025600 01  WRK-XN-18-2                 PIC  X(18)     VALUE             NC1034.2
025700            "123456789012345678".                                 NC1034.2
025800                                                                  NC1034.2
025900 01  IF-D10.                                                      NC1034.2
026000     02 FILLER          PICTURE XX VALUE "01".                    NC1034.2
026100     02 FILLER          PICTURE XX VALUE "23".                    NC1034.2
026200     02 IF-D10A.                                                  NC1034.2
026300       03 FILLER        PICTURE XXXX VALUE "4567".                NC1034.2
026400       03 FILLER        PICTURE XXXX VALUE "8912".                NC1034.2
026500 01  IF-D12.                                                      NC1034.2
026600     02 FILLER          PICTURE XXX VALUE "ABC".                  NC1034.2
026700     02 IF-D12A.                                                  NC1034.2
026800       03 IF-D12B.                                                NC1034.2
026900         04 FILLER      PICTURE XX VALUE "DE".                    NC1034.2
027000         04 FILLER      PICTURE X  VALUE "F".                     NC1034.2
027100 01  IF-D20.                                                      NC1034.2
027200     02 FILLER          PICTURE 9(5) VALUE ZERO.                  NC1034.2
027300     02 FILLER          PICTURE 99   VALUE 12.                    NC1034.2
027400     02 FILLER          PICTURE 9    VALUE 3.                     NC1034.2
027500     02 FILLER          PICTURE 99   VALUE 45.                    NC1034.2
027600 01  IF-D21.                                                      NC1034.2
027700     02 FILLER          PICTURE 9(5) VALUE ZERO.                  NC1034.2
027800     02 FILLER          PICTURE 9(5) VALUE 12345.                 NC1034.2
027900 01  IF-D22.                                                      NC1034.2
028000     02 FILLER          PICTURE AA   VALUE "AB".                  NC1034.2
028100     02 FILLER          PICTURE AAAA VALUE "CDEF".                NC1034.2
028200 01  IF-D35.                                                      NC1034.2
028300     02 IF-D35A                             VALUE "*ASTERISK".    NC1034.2
028400       03 FILLER        PICTURE A(6).                             NC1034.2
028500       03 FILLER        PICTURE AAA.                              NC1034.2
028600     02 IF-D35B                            VALUE "/SLASH".        NC1034.2
028700       03 FILLER        PICTURE 9(6).                             NC1034.2
028800 01  IF-D36 REDEFINES IF-D35.                                     NC1034.2
028900     02 IF-D36A         PICTURE X(6).                             NC1034.2
029000     02 IF-D36B         PICTURE XXX.                              NC1034.2
029100     02 IF-D36C         PICTURE X(6).                             NC1034.2
029200 01  IF-D39.                                                      NC1034.2
029300     02  FILLER   PICTURE A(6) VALUE "ABCDEF".                    NC1034.2
029400     02  FILLER  PICTURE A(4) VALUE SPACE.                        NC1034.2
029500 01  LEVEL-01.                                                    NC1034.2
029600     02 LEVEL-02.                                                 NC1034.2
029700     03 LEVEL-03.                                                 NC1034.2
029800     04 LEVEL-04.                                                 NC1034.2
029900     05 LEVEL-05.                                                 NC1034.2
030000     06 LEVEL-06.                                                 NC1034.2
030100     07 LEVEL-07.                                                 NC1034.2
030200     08 LEVEL-08.                                                 NC1034.2
030300     09 LEVEL-09.                                                 NC1034.2
030400     10 LEVEL-10                        PICTURE IS X VALUE IS "R".NC1034.2
030500 01  LEVEL-RECEIVER                     PICTURE IS X VALUE IS     NC1034.2
030600     SPACE.                                                       NC1034.2
030700 01  LEVEL-SENDER PICTURE X VALUE "S".                            NC1034.2
030800 01  VAL                                PICTURE IS 9 VALUE IS 0.  NC1034.2
030900 01  A-2                                PICTURE IS A VALUE IS "A".NC1034.2
031000 01  N-27                               PICTURE IS 9999V9         NC1034.2
031100     VALUE IS 9999.9.                                             NC1034.2
031200 01  N-30                               PICTURE IS 9V9            NC1034.2
031300     VALUE IS 2.                                                  NC1034.2
031400 01  N-31                               PICTURE IS 9(6).          NC1034.2
031500 01  X-32 REDEFINES N-31                PICTURE IS X(6).          NC1034.2
031600 01  N-33                               PICTURE IS 9(5)           NC1034.2
031700     VALUE IS 29.                                                 NC1034.2
031800 01  A-37                               PICTURE IS A VALUE IS "X".NC1034.2
031900 01  X-38 REDEFINES A-37                PICTURE IS X.             NC1034.2
032000 01  X-43 PIC X(10) VALUE "    l75.63".                           NC1034.2
032100 01  N-84                               PICTURE IS 9999999999.    NC1034.2
032200 01  NUMERIC-GRP-TEST.                                            NC1034.2
032300     02  NUMERIC-1                PICTURE 9 VALUE 0.              NC1034.2
032400     02  NUMERIC-2.                                               NC1034.2
032500         03  NUMERIC-3            PICTURE 9(1)V9(1) VALUE ZERO.   NC1034.2
032600         03  NUMERIC-4.                                           NC1034.2
032700             04  NUMERIC-5       PICTURE 9(18) VALUE 1.           NC1034.2
032800     02  NUMERIC-6.                                               NC1034.2
032900         03  NUMERIC-7            PICTURE X VALUE "7".            NC1034.2
033000         03  NUMERIC-8            PICTURE 9  VALUE 8.             NC1034.2
033100 01  NUM-GRP.                                                     NC1034.2
033200     02  NUM-SUB-GRP  PIC 9.                                      NC1034.2
033300 01  GROUP-1000.                                                  NC1034.2
033400     02  FILLER  PIC X.                                           NC1034.2
033500     02  GROUP-X1000.                                             NC1034.2
033600         03  GROUP-1000-1 PIC X(500) VALUE ZERO.                  NC1034.2
033700         03  XNAME        PICTURE X(100) VALUE QUOTE.             NC1034.2
033800         03  GROUP-1000-2 PICTURE X(399) VALUE SPACE.             NC1034.2
033900         03  GROUP-1000-3 PICTURE X VALUE ".".                    NC1034.2
034000     02  GROUP-X500-2.                                            NC1034.2
034100         03  GROUP-X500-A        PICTURE X(500) VALUE ZERO.       NC1034.2
034200         03  GROUP-X500-1.                                        NC1034.2
034300             04  GROUP-X500-1-1  PICTURE X(50) VALUE QUOTE.       NC1034.2
034400             04  GROUP-X500-1-2  PICTURE X(50) VALUE QUOTE.       NC1034.2
034500             04  GROUP-X500-1-3  PICTURE X(398) VALUE SPACE.      NC1034.2
034600             04  GROUP-X500-1-4  PICTURE XX VALUE " .".           NC1034.2
034700 01  HI-LO-VALUES.                                                NC1034.2
034800     02  LOW-VAL  PIC X VALUE LOW-VALUE.                          NC1034.2
034900     02 ZERO-01  PICTURE 9(18) VALUE 1.                           NC1034.2
035000     02  ABC      PICTURE XXX VALUE "ABC".                        NC1034.2
035100     02  NINE-17-8 PICTURE 9(18) VALUE 999999999999999998.        NC1034.2
035200     02  ZERO-NULL PIC 9(9) VALUE 0.                              NC1034.2
035300     02  ZERO-ZERO PICTURE 9(9)V9(9) VALUE 0.0.                   NC1034.2
035400 01  COMP-DATA.                                                   NC1034.2
035500     02  COMP-DATA1 PICTURE 9(18) COMPUTATIONAL VALUE 300.        NC1034.2
035600     02  COMP-DATA2  PICTURE 9(10) COMPUTATIONAL VALUE  100000.   NC1034.2
035700     02  COMP-DATA3  PICTURE 9     COMPUTATIONAL VALUE 9.         NC1034.2
035800     02  COMP-DATA4  PICTURE 9(9)V9(7) COMPUTATIONAL VALUE 3.3.   NC1034.2
035900     02  COMP-DATA5  PICTURE 9(5)V9(2) COMPUTATIONAL VALUE 52.25. NC1034.2
036000     02  COMP-DATA6  PICTURE 9V9       COMPUTATIONAL VALUE 8.8.   NC1034.2
036100     02  COMP-DATA7  PICTURE 9(3)V9(2) COMPUTATIONAL VALUE 300.00.NC1034.2
036200     02  COMP-DATA8  PICTURE 9V9(9) COMPUTATIONAL VALUE 3.3000000.NC1034.2
036300     02  COMP-DATA9  PICTURE 9(8)  COMPUTATIONAL VALUE 100000.    NC1034.2
036400 01  DISP-DATA.                                                   NC1034.2
036500     02  DISP-DATA1  PICTURE 9(18) VALUE 300.                     NC1034.2
036600     02  DISP-DATA2  PICTURE 9(8)  VALUE 100000.                  NC1034.2
036700     02  DISP-DATA3  PICTURE 9     VALUE 9.                       NC1034.2
036800     02  DISP-DATA4  PICTURE 9(7)V9(9) VALUE 3.3.                 NC1034.2
036900     02  DISP-DATA5  PICTURE 9(2)V9(2) VALUE 52.25.               NC1034.2
037000     02  DISP-DATA6  PICTURE 9V9   VALUE 8.8.                     NC1034.2
037100 PROCEDURE DIVISION.                                              NC1034.2
037200 CCVS1 SECTION.                                                   NC1034.2
037300 OPEN-FILES.                                                      NC1034.2
037400     OPEN     OUTPUT PRINT-FILE.                                  NC1034.2
037500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1034.2
037600     MOVE    SPACE TO TEST-RESULTS.                               NC1034.2
037700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1034.2
037800     GO TO CCVS1-EXIT.                                            NC1034.2
037900 CLOSE-FILES.                                                     NC1034.2
038000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1034.2
038100 TERMINATE-CCVS.                                                  NC1034.2
038200*    EXIT PROGRAM.                                                NC1034.2
038300*TERMINATE-CALL.                                                  NC1034.2
038400     STOP     RUN.                                                NC1034.2
038500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1034.2
038600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1034.2
038700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1034.2
038800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1034.2
038900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1034.2
039000 PRINT-DETAIL.                                                    NC1034.2
039100     IF REC-CT NOT EQUAL TO ZERO                                  NC1034.2
039200             MOVE "." TO PARDOT-X                                 NC1034.2
039300             MOVE REC-CT TO DOTVALUE.                             NC1034.2
039400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1034.2
039500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1034.2
039600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1034.2
039700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1034.2
039800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1034.2
039900     MOVE SPACE TO CORRECT-X.                                     NC1034.2
040000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1034.2
040100     MOVE     SPACE TO RE-MARK.                                   NC1034.2
040200 HEAD-ROUTINE.                                                    NC1034.2
040300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1034.2
040400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1034.2
040500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1034.2
040600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1034.2
040700 COLUMN-NAMES-ROUTINE.                                            NC1034.2
040800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1034.2
040900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1034.2
041000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1034.2
041100 END-ROUTINE.                                                     NC1034.2
041200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1034.2
041300 END-RTN-EXIT.                                                    NC1034.2
041400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1034.2
041500 END-ROUTINE-1.                                                   NC1034.2
041600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1034.2
041700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1034.2
041800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1034.2
041900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1034.2
042000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1034.2
042100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1034.2
042200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1034.2
042300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1034.2
042400  END-ROUTINE-12.                                                 NC1034.2
042500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1034.2
042600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1034.2
042700         MOVE "NO " TO ERROR-TOTAL                                NC1034.2
042800         ELSE                                                     NC1034.2
042900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1034.2
043000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1034.2
043100     PERFORM WRITE-LINE.                                          NC1034.2
043200 END-ROUTINE-13.                                                  NC1034.2
043300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1034.2
043400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1034.2
043500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1034.2
043600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1034.2
043700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1034.2
043800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1034.2
043900          MOVE "NO " TO ERROR-TOTAL                               NC1034.2
044000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1034.2
044100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1034.2
044200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1034.2
044300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1034.2
044400 WRITE-LINE.                                                      NC1034.2
044500     ADD 1 TO RECORD-COUNT.                                       NC1034.2
044600*    IF RECORD-COUNT GREATER 42                                   NC1034.2
044700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1034.2
044800*        MOVE SPACE TO DUMMY-RECORD                               NC1034.2
044900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1034.2
045000*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1034.2
045100*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1034.2
045200*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1034.2
045300*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1034.2
045400*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1034.2
045500*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1034.2
045600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1034.2
045700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1034.2
045800*        MOVE ZERO TO RECORD-COUNT.                               NC1034.2
045900     PERFORM WRT-LN.                                              NC1034.2
046000 WRT-LN.                                                          NC1034.2
046100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1034.2
046200     MOVE SPACE TO DUMMY-RECORD.                                  NC1034.2
046300 BLANK-LINE-PRINT.                                                NC1034.2
046400     PERFORM WRT-LN.                                              NC1034.2
046500 FAIL-ROUTINE.                                                    NC1034.2
046600     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1034.2
046700            GO TO FAIL-ROUTINE-WRITE.                             NC1034.2
046800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1034.2
046900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1034.2
047000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1034.2
047100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1034.2
047200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1034.2
047300     GO TO  FAIL-ROUTINE-EX.                                      NC1034.2
047400 FAIL-ROUTINE-WRITE.                                              NC1034.2
047500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1034.2
047600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1034.2
047700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1034.2
047800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1034.2
047900 FAIL-ROUTINE-EX. EXIT.                                           NC1034.2
048000 BAIL-OUT.                                                        NC1034.2
048100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1034.2
048200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1034.2
048300 BAIL-OUT-WRITE.                                                  NC1034.2
048400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1034.2
048500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1034.2
048600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1034.2
048700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1034.2
048800 BAIL-OUT-EX. EXIT.                                               NC1034.2
048900 CCVS1-EXIT.                                                      NC1034.2
049000     EXIT.                                                        NC1034.2
049100 SECT-NC103A-001 SECTION.                                         NC1034.2
049200 NC-03-001.                                                       NC1034.2
049300     MOVE "THE FOLLOWING TESTS        " TO RE-MARK.               NC1034.2
049400     PERFORM  PRINT-DETAIL.                                       NC1034.2
049500     MOVE "COMPARE NUMERIC, ALPHA-    " TO RE-MARK.               NC1034.2
049600     PERFORM  PRINT-DETAIL.                                       NC1034.2
049700     MOVE "NUMERIC, ALPHABETIC, AND   " TO RE-MARK.               NC1034.2
049800     PERFORM  PRINT-DETAIL.                                       NC1034.2
049900     MOVE  "GROUP ITEMS IN VARYING     " TO RE-MARK.              NC1034.2
050000     PERFORM PRINT-DETAIL.                                        NC1034.2
050100     MOVE  "COMBINATIONS.              " TO RE-MARK.              NC1034.2
050200     PERFORM PRINT-DETAIL.                                        NC1034.2
050300     MOVE    SPACE TO TEST-RESULTS.                               NC1034.2
050400 IF--INIT-GF-1.                                                   NC1034.2
050500     MOVE  "COMPARE--EQUAL" TO FEATURE.                           NC1034.2
050600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
050700     MOVE   0 TO IF-D1.                                           NC1034.2
050800 IF--TEST-GF-1.                                                   NC1034.2
050900     IF      ZERO IS EQUAL TO IF-D1                               NC1034.2
051000             PERFORM PASS                                         NC1034.2
051100     ELSE                                                         NC1034.2
051200             PERFORM FAIL.                                        NC1034.2
051300     GO TO   IF--WRITE-GF-1.                                      NC1034.2
051400 IF--DELETE-GF-1.                                                 NC1034.2
051500     PERFORM DE-LETE.                                             NC1034.2
051600 IF--WRITE-GF-1.                                                  NC1034.2
051700     MOVE  "IF--TEST-GF-1 " TO PAR-NAME.                          NC1034.2
051800     PERFORM PRINT-DETAIL.                                        NC1034.2
051900 IF--INIT-GF-2.                                                   NC1034.2
052000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
052100     MOVE   ZERO TO IF-D2.                                        NC1034.2
052200 IF--TEST-GF-2.                                                   NC1034.2
052300     IF      ZERO IS EQUAL TO IF-D2                               NC1034.2
052400             PERFORM PASS                                         NC1034.2
052500     ELSE                                                         NC1034.2
052600             PERFORM FAIL.                                        NC1034.2
052700     GO TO   IF--WRITE-GF-2.                                      NC1034.2
052800 IF--DELETE-GF-2.                                                 NC1034.2
052900     PERFORM DE-LETE.                                             NC1034.2
053000 IF--WRITE-GF-2.                                                  NC1034.2
053100     MOVE "IF--TEST-GF-2 " TO PAR-NAME.                           NC1034.2
053200     PERFORM PRINT-DETAIL.                                        NC1034.2
053300 IF--INIT-GF-3.                                                   NC1034.2
053400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
053500     MOVE   "123" TO IF-D9.                                       NC1034.2
053600 IF--TEST-GF-3.                                                   NC1034.2
053700     IF      IF-D9 EQUAL TO 123                                   NC1034.2
053800             PERFORM PASS                                         NC1034.2
053900     ELSE                                                         NC1034.2
054000             PERFORM FAIL.                                        NC1034.2
054100     GO TO   IF--WRITE-GF-3.                                      NC1034.2
054200 IF--DELETE-GF-3.                                                 NC1034.2
054300     PERFORM DE-LETE.                                             NC1034.2
054400 IF--WRITE-GF-3.                                                  NC1034.2
054500     MOVE  "IF--TEST-GF-3 " TO PAR-NAME.                          NC1034.2
054600     PERFORM PRINT-DETAIL.                                        NC1034.2
054700 IF--INIT-GF-4.                                                   NC1034.2
054800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
054900     MOVE   "012345678912" TO IF-D10.                             NC1034.2
055000 IF--TEST-GF-4.                                                   NC1034.2
055100     IF      IF-D10 EQUAL TO 012345678912                         NC1034.2
055200             PERFORM PASS                                         NC1034.2
055300     ELSE                                                         NC1034.2
055400             PERFORM FAIL.                                        NC1034.2
055500     GO TO   IF--WRITE-GF-4.                                      NC1034.2
055600 IF--DELETE-GF-4.                                                 NC1034.2
055700     PERFORM DE-LETE.                                             NC1034.2
055800 IF--WRITE-GF-4.                                                  NC1034.2
055900     MOVE  "IF--TEST-GF-4 " TO PAR-NAME.                          NC1034.2
056000     PERFORM PRINT-DETAIL.                                        NC1034.2
056100 IF--INIT-GF-5.                                                   NC1034.2
056200     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
056300     MOVE   "ABCDEF" TO IF-D11.                                   NC1034.2
056400 IF--TEST-GF-5.                                                   NC1034.2
056500     IF      IF-D11 EQUAL TO "ABCDEF"                             NC1034.2
056600             PERFORM PASS                                         NC1034.2
056700     ELSE                                                         NC1034.2
056800             PERFORM FAIL.                                        NC1034.2
056900     GO TO    IF--WRITE-GF-5.                                     NC1034.2
057000 IF--DELETE-GF-5.                                                 NC1034.2
057100     PERFORM  DE-LETE.                                            NC1034.2
057200 IF--WRITE-GF-5.                                                  NC1034.2
057300     MOVE "IF--TEST-GF-5 " TO PAR-NAME.                           NC1034.2
057400     PERFORM  PRINT-DETAIL.                                       NC1034.2
057500 IF--INIT-GF-6.                                                   NC1034.2
057600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
057700     MOVE   "ABCDEF" TO IF-D12.                                   NC1034.2
057800 IF--TEST-GF-6.                                                   NC1034.2
057900     IF       IF-D12 EQUAL TO "ABCDEF"                            NC1034.2
058000              PERFORM PASS                                        NC1034.2
058100     ELSE                                                         NC1034.2
058200              PERFORM FAIL.                                       NC1034.2
058300     GO TO    IF--WRITE-GF-6.                                     NC1034.2
058400 IF--DELETE-GF-6.                                                 NC1034.2
058500     PERFORM  DE-LETE.                                            NC1034.2
058600 IF--WRITE-GF-6.                                                  NC1034.2
058700     MOVE "IF--TEST-GF-6 " TO PAR-NAME.                           NC1034.2
058800     PERFORM  PRINT-DETAIL.                                       NC1034.2
058900 IF--INIT-GF-7.                                                   NC1034.2
059000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
059100     MOVE   +123.45 TO IF-D7.                                     NC1034.2
059200 IF--TEST-GF-7.                                                   NC1034.2
059300     IF       IF-D7 EQUAL TO +123.45                              NC1034.2
059400              PERFORM PASS                                        NC1034.2
059500              ELSE                                                NC1034.2
059600              PERFORM FAIL.                                       NC1034.2
059700     GO TO    IF--WRITE-GF-7.                                     NC1034.2
059800 IF--DELETE-GF-7.                                                 NC1034.2
059900     PERFORM  DE-LETE.                                            NC1034.2
060000 IF--WRITE-GF-7.                                                  NC1034.2
060100     MOVE "IF--TEST-GF-7 " TO PAR-NAME.                           NC1034.2
060200     PERFORM  PRINT-DETAIL.                                       NC1034.2
060300 IF--INIT-GF-8.                                                   NC1034.2
060400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
060500     MOVE    12300 TO IF-D8.                                      NC1034.2
060600 IF--TEST-GF-8.                                                   NC1034.2
060700     IF       IF-D8 EQUAL TO 12300                                NC1034.2
060800              PERFORM PASS                                        NC1034.2
060900              ELSE                                                NC1034.2
061000              PERFORM FAIL.                                       NC1034.2
061100     GO TO    IF--WRITE-GF-8.                                     NC1034.2
061200 IF--DELETE-GF-8.                                                 NC1034.2
061300     PERFORM  DE-LETE.                                            NC1034.2
061400 IF--WRITE-GF-8.                                                  NC1034.2
061500     MOVE "IF--TEST-GF-8 " TO PAR-NAME.                           NC1034.2
061600     PERFORM  PRINT-DETAIL.                                       NC1034.2
061700 IF--INIT-GF-9.                                                   NC1034.2
061800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
061900     MOVE    12300 TO IF-D8.                                      NC1034.2
062000     MOVE    12300 TO IF-D13.                                     NC1034.2
062100 IF--TEST-GF-9.                                                   NC1034.2
062200     IF       IF-D13 EQUAL TO IF-D8                               NC1034.2
062300              PERFORM PASS                                        NC1034.2
062400              ELSE                                                NC1034.2
062500              PERFORM FAIL.                                       NC1034.2
062600     GO TO    IF--WRITE-GF-9.                                     NC1034.2
062700 IF--DELETE-GF-9.                                                 NC1034.2
062800     PERFORM  DE-LETE.                                            NC1034.2
062900 IF--WRITE-GF-9.                                                  NC1034.2
063000     MOVE "IF--TEST-GF-9 " TO PAR-NAME.                           NC1034.2
063100     PERFORM  PRINT-DETAIL.                                       NC1034.2
063200 IF--INIT-GF-10.                                                  NC1034.2
063300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
063400     MOVE    .0012 TO IF-D16.                                     NC1034.2
063500     MOVE    .0012 TO IF-D17.                                     NC1034.2
063600 IF--TEST-GF-10.                                                  NC1034.2
063700     IF       IF-D16 EQUAL TO IF-D17                              NC1034.2
063800              PERFORM PASS                                        NC1034.2
063900              ELSE                                                NC1034.2
064000              PERFORM FAIL.                                       NC1034.2
064100     GO TO    IF--WRITE-GF-10.                                    NC1034.2
064200 IF--DELETE-GF-10.                                                NC1034.2
064300     PERFORM  DE-LETE.                                            NC1034.2
064400 IF--WRITE-GF-10.                                                 NC1034.2
064500     MOVE "IF--TEST-GF-10" TO PAR-NAME.                           NC1034.2
064600     PERFORM  PRINT-DETAIL.                                       NC1034.2
064700 IF--INIT-GF-11.                                                  NC1034.2
064800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
064900     MOVE    2137.45 TO IF-D27.                                   NC1034.2
065000     MOVE    2137.45 TO IF-D28.                                   NC1034.2
065100 IF--TEST-GF-11.                                                  NC1034.2
065200     IF       IF-D27 EQUAL TO IF-D28                              NC1034.2
065300              PERFORM PASS ELSE PERFORM FAIL.                     NC1034.2
065400     GO       TO IF-WRITE-GF-11.                                  NC1034.2
065500 IF-DELETE-GF-11.                                                 NC1034.2
065600     PERFORM  DE-LETE.                                            NC1034.2
065700 IF-WRITE-GF-11.                                                  NC1034.2
065800     MOVE "IF--TEST-GF-11" TO PAR-NAME.                           NC1034.2
065900     PERFORM  PRINT-DETAIL.                                       NC1034.2
066000 IF--INIT-GF-12.                                                  NC1034.2
066100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
066200     MOVE   +123.45 TO IF-D14.                                    NC1034.2
066300     MOVE   +123.45 TO IF-D7.                                     NC1034.2
066400 IF--TEST-GF-12.                                                  NC1034.2
066500     IF       IF-D14 EQUAL TO IF-D7                               NC1034.2
066600              PERFORM PASS                                        NC1034.2
066700              ELSE                                                NC1034.2
066800              PERFORM FAIL.                                       NC1034.2
066900     GO TO    IF--WRITE-GF-12.                                    NC1034.2
067000 IF--DELETE-GF-12.                                                NC1034.2
067100     PERFORM  DE-LETE.                                            NC1034.2
067200 IF--WRITE-GF-12.                                                 NC1034.2
067300     MOVE "IF--TEST-GF-12" TO PAR-NAME.                           NC1034.2
067400     PERFORM  PRINT-DETAIL.                                       NC1034.2
067500 IF--INIT-GF-13.                                                  NC1034.2
067600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
067700     MOVE    12300  TO IF-D15.                                    NC1034.2
067800     MOVE    12300  TO IF-D8.                                     NC1034.2
067900 IF--TEST-GF-13.                                                  NC1034.2
068000     IF       IF-D15 EQUAL TO IF-D8                               NC1034.2
068100              PERFORM PASS                                        NC1034.2
068200              ELSE                                                NC1034.2
068300              PERFORM FAIL.                                       NC1034.2
068400     GO TO    IF--WRITE-GF-13.                                    NC1034.2
068500 IF--DELETE-GF-13.                                                NC1034.2
068600     PERFORM  DE-LETE.                                            NC1034.2
068700 IF--WRITE-GF-13.                                                 NC1034.2
068800     MOVE "IF--TEST-GF-13" TO PAR-NAME.                           NC1034.2
068900     PERFORM  PRINT-DETAIL.                                       NC1034.2
069000 IF--INIT-GF-14.                                                  NC1034.2
069100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
069200     MOVE    0000012345  TO IF-D20.                               NC1034.2
069300     MOVE    0000012345  TO IF-D21.                               NC1034.2
069400 IF--TEST-GF-14.                                                  NC1034.2
069500     IF       IF-D20 EQUAL TO IF-D21                              NC1034.2
069600              PERFORM PASS                                        NC1034.2
069700              ELSE                                                NC1034.2
069800              PERFORM FAIL.                                       NC1034.2
069900     GO TO    IF--WRITE-GF-14.                                    NC1034.2
070000 IF--DELETE-GF-14.                                                NC1034.2
070100     PERFORM  DE-LETE.                                            NC1034.2
070200 IF--WRITE-GF-14.                                                 NC1034.2
070300     MOVE "IF--TEST-GF-14" TO PAR-NAME.                           NC1034.2
070400     PERFORM  PRINT-DETAIL.                                       NC1034.2
070500 IF--INIT-GF-15.                                                  NC1034.2
070600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
070700     MOVE    "$1,2 3.40+" TO IF-D24.                              NC1034.2
070800     MOVE     +123.4 TO IF-D23.                                   NC1034.2
070900 IF--TEST-GF-15.                                                  NC1034.2
071000     IF       IF-D23 EQUAL TO IF-D24                              NC1034.2
071100              PERFORM PASS                                        NC1034.2
071200              ELSE                                                NC1034.2
071300              PERFORM FAIL.                                       NC1034.2
071400     GO TO    IF--WRITE-GF-15.                                    NC1034.2
071500 IF--DELETE-GF-15.                                                NC1034.2
071600     PERFORM  DE-LETE.                                            NC1034.2
071700 IF--WRITE-GF-15.                                                 NC1034.2
071800     MOVE "IF--TEST-GF-15" TO PAR-NAME.                           NC1034.2
071900     PERFORM  PRINT-DETAIL.                                       NC1034.2
072000 IF--INIT-GF-16.                                                  NC1034.2
072100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
072200     MOVE    "A C D0E" TO IF-D26.                                 NC1034.2
072300     MOVE    "ACDE" TO IF-D25.                                    NC1034.2
072400 IF--TEST-GF-16.                                                  NC1034.2
072500     IF       IF-D25 EQUAL TO IF-D26                              NC1034.2
072600              PERFORM PASS                                        NC1034.2
072700              ELSE                                                NC1034.2
072800              PERFORM FAIL.                                       NC1034.2
072900     GO TO    IF--WRITE-GF-16.                                    NC1034.2
073000 IF--DELETE-GF-16.                                                NC1034.2
073100     PERFORM  DE-LETE.                                            NC1034.2
073200 IF--WRITE-GF-16.                                                 NC1034.2
073300     MOVE "IF--TEST-GF-16" TO PAR-NAME.                           NC1034.2
073400     PERFORM  PRINT-DETAIL.                                       NC1034.2
073500 IF--INIT-GF-17.                                                  NC1034.2
073600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
073700     MOVE    "BABABABABA" TO IF-D6.                               NC1034.2
073800     MOVE    "BABABABABA" TO IF-D18.                              NC1034.2
073900 IF--TEST-GF-17.                                                  NC1034.2
074000     IF       IF-D6 EQUAL TO IF-D18                               NC1034.2
074100              PERFORM PASS                                        NC1034.2
074200              ELSE                                                NC1034.2
074300              PERFORM FAIL.                                       NC1034.2
074400     GO TO    IF--WRITE-GF-17.                                    NC1034.2
074500 IF--DELETE-GF-17.                                                NC1034.2
074600     PERFORM  DE-LETE.                                            NC1034.2
074700 IF--WRITE-GF-17.                                                 NC1034.2
074800     MOVE "IF--TEST-GF-17" TO PAR-NAME.                           NC1034.2
074900     PERFORM  PRINT-DETAIL.                                       NC1034.2
075000 IF--INIT-GF-18.                                                  NC1034.2
075100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
075200     MOVE    "ABCDEF" TO IF-D22.                                  NC1034.2
075300     MOVE    "ABCDEF" TO IF-D12.                                  NC1034.2
075400 IF--TEST-GF-18.                                                  NC1034.2
075500     IF       IF-D22 EQUAL TO IF-D12                              NC1034.2
075600              PERFORM PASS                                        NC1034.2
075700              ELSE                                                NC1034.2
075800              PERFORM FAIL.                                       NC1034.2
075900     GO TO    IF--WRITE-GF-18.                                    NC1034.2
076000 IF--DELETE-GF-18.                                                NC1034.2
076100     PERFORM  DE-LETE.                                            NC1034.2
076200 IF--WRITE-GF-18.                                                 NC1034.2
076300     MOVE "IF--TEST-GF-18" TO PAR-NAME.                           NC1034.2
076400     PERFORM  PRINT-DETAIL.                                       NC1034.2
076500 IF--INIT-GF-19.                                                  NC1034.2
076600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
076700     MOVE    "ABCDEF    " TO IF-D39.                              NC1034.2
076800     MOVE    "ABCDEF    " TO IF-D19.                              NC1034.2
076900 IF--TEST-GF-19.                                                  NC1034.2
077000     IF IF-D39 EQUAL TO IF-D19                                    NC1034.2
077100              PERFORM PASS                                        NC1034.2
077200              ELSE                                                NC1034.2
077300              PERFORM FAIL.                                       NC1034.2
077400     GO TO    IF--WRITE-GF-19.                                    NC1034.2
077500 IF--DELETE-GF-19.                                                NC1034.2
077600     PERFORM  DE-LETE.                                            NC1034.2
077700 IF--WRITE-GF-19.                                                 NC1034.2
077800     MOVE "IF--TEST-GF-19" TO PAR-NAME.                           NC1034.2
077900     PERFORM  PRINT-DETAIL.                                       NC1034.2
078000 IF--INIT-GF-20.                                                  NC1034.2
078100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
078200     MOVE "COMPARE--GREATER" TO FEATURE.                          NC1034.2
078300     MOVE     0 TO IF-D1.                                         NC1034.2
078400 IF--TEST-GF-20.                                                  NC1034.2
078500     IF       IF-D1 IS GREATER THAN ZERO                          NC1034.2
078600              PERFORM FAIL                                        NC1034.2
078700              ELSE                                                NC1034.2
078800              PERFORM PASS.                                       NC1034.2
078900     GO TO    IF--WRITE-GF-20.                                    NC1034.2
079000 IF--DELETE-GF-20.                                                NC1034.2
079100     PERFORM  DE-LETE.                                            NC1034.2
079200 IF--WRITE-GF-20.                                                 NC1034.2
079300     MOVE "IF--TEST-GF-20" TO PAR-NAME.                           NC1034.2
079400     PERFORM  PRINT-DETAIL.                                       NC1034.2
079500 IF--INIT-GF-21.                                                  NC1034.2
079600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
079700     MOVE    "123" TO IF-D9.                                      NC1034.2
079800 IF--TEST-GF-21.                                                  NC1034.2
079900     IF       IF-D9 GREATER THAN 123                              NC1034.2
080000              PERFORM FAIL                                        NC1034.2
080100              ELSE                                                NC1034.2
080200              PERFORM PASS.                                       NC1034.2
080300     GO TO    IF--WRITE-GF-21.                                    NC1034.2
080400 IF--DELETE-GF-21.                                                NC1034.2
080500     PERFORM  DE-LETE.                                            NC1034.2
080600 IF--WRITE-GF-21.                                                 NC1034.2
080700     MOVE "IF--TEST-GF-21" TO PAR-NAME.                           NC1034.2
080800     PERFORM  PRINT-DETAIL.                                       NC1034.2
080900 IF--INIT-GF-22.                                                  NC1034.2
081000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
081100     MOVE    "012345678912" TO IF-D10.                            NC1034.2
081200 IF--TEST-GF-22.                                                  NC1034.2
081300     IF       IF-D10 GREATER THAN 012345678912                    NC1034.2
081400              PERFORM FAIL                                        NC1034.2
081500              ELSE                                                NC1034.2
081600              PERFORM PASS.                                       NC1034.2
081700     GO TO    IF--WRITE-GF-22.                                    NC1034.2
081800 IF--DELETE-GF-22.                                                NC1034.2
081900     PERFORM  DE-LETE.                                            NC1034.2
082000 IF--WRITE-GF-22.                                                 NC1034.2
082100     MOVE "IF--TEST-GF-22" TO PAR-NAME.                           NC1034.2
082200     PERFORM  PRINT-DETAIL.                                       NC1034.2
082300 IF--INIT-GF-23.                                                  NC1034.2
082400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
082500     MOVE    "ABCDEF" TO IF-D10.                                  NC1034.2
082600 IF--TEST-GF-23.                                                  NC1034.2
082700     IF       IF-D11 GREATER THAN "ABCDEF"                        NC1034.2
082800              PERFORM FAIL                                        NC1034.2
082900              ELSE                                                NC1034.2
083000              PERFORM PASS.                                       NC1034.2
083100     GO TO    IF--WRITE-GF-23.                                    NC1034.2
083200 IF--DELETE-GF-23.                                                NC1034.2
083300     PERFORM  DE-LETE.                                            NC1034.2
083400 IF--WRITE-GF-23.                                                 NC1034.2
083500     MOVE "IF--TEST-GF-23" TO PAR-NAME.                           NC1034.2
083600     PERFORM  PRINT-DETAIL.                                       NC1034.2
083700 IF--INIT-GF-24.                                                  NC1034.2
083800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
083900     MOVE    "ABCDEF" TO IF-D12.                                  NC1034.2
084000 IF--TEST-GF-24.                                                  NC1034.2
084100     IF       IF-D12 GREATER THAN "ABCDEF"                        NC1034.2
084200              PERFORM FAIL                                        NC1034.2
084300              ELSE                                                NC1034.2
084400              PERFORM PASS.                                       NC1034.2
084500     GO TO    IF--WRITE-GF-24.                                    NC1034.2
084600 IF--DELETE-GF-24.                                                NC1034.2
084700     PERFORM  DE-LETE.                                            NC1034.2
084800 IF--WRITE-GF-24.                                                 NC1034.2
084900     MOVE "IF--TEST-GF-24" TO PAR-NAME.                           NC1034.2
085000     PERFORM  PRINT-DETAIL.                                       NC1034.2
085100 IF--INIT-GF-25.                                                  NC1034.2
085200     MOVE    +123.45 TO IF-D7.                                    NC1034.2
085300 IF--TEST-GF-25.                                                  NC1034.2
085400     IF       IF-D7 GREATER THAN +123.45                          NC1034.2
085500              PERFORM FAIL                                        NC1034.2
085600              ELSE                                                NC1034.2
085700              PERFORM PASS.                                       NC1034.2
085800     GO TO    IF--WRITE-GF-25.                                    NC1034.2
085900 IF--DELETE-GF-25.                                                NC1034.2
086000     PERFORM  DE-LETE.                                            NC1034.2
086100 IF--WRITE-GF-25.                                                 NC1034.2
086200     MOVE "IF--TEST-GF-25" TO PAR-NAME.                           NC1034.2
086300     PERFORM  PRINT-DETAIL.                                       NC1034.2
086400 IF--INIT-GF-26.                                                  NC1034.2
086500     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
086600     MOVE     12300 TO IF-D8.                                     NC1034.2
086700 IF--TEST-GF-26.                                                  NC1034.2
086800     IF       IF-D8 GREATER THAN 12300                            NC1034.2
086900              PERFORM FAIL                                        NC1034.2
087000              ELSE                                                NC1034.2
087100              PERFORM PASS.                                       NC1034.2
087200     GO TO    IF--WRITE-GF-26.                                    NC1034.2
087300 IF--DELETE-GF-26.                                                NC1034.2
087400     PERFORM  DE-LETE.                                            NC1034.2
087500 IF--WRITE-GF-26.                                                 NC1034.2
087600     MOVE "IF--TEST-GF-26" TO PAR-NAME.                           NC1034.2
087700     PERFORM  PRINT-DETAIL.                                       NC1034.2
087800 IF--INIT-GF-27.                                                  NC1034.2
087900     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
088000     MOVE     12300 TO IF-D8.                                     NC1034.2
088100     MOVE     12300 TO IF-D13.                                    NC1034.2
088200 IF--TEST-GF-27.                                                  NC1034.2
088300     IF       IF-D13 GREATER THAN IF-D8                           NC1034.2
088400              PERFORM FAIL                                        NC1034.2
088500              ELSE                                                NC1034.2
088600              PERFORM PASS.                                       NC1034.2
088700     GO TO    IF--WRITE-GF-27.                                    NC1034.2
088800 IF--DELETE-GF-27.                                                NC1034.2
088900     PERFORM  DE-LETE.                                            NC1034.2
089000 IF--WRITE-GF-27.                                                 NC1034.2
089100     MOVE "IF--TEST-GF-27" TO PAR-NAME.                           NC1034.2
089200     PERFORM  PRINT-DETAIL.                                       NC1034.2
089300 IF--INIT-GF-28.                                                  NC1034.2
089400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
089500     MOVE     .0012 TO IF-D16.                                    NC1034.2
089600     MOVE     .0012 TO IF-D17.                                    NC1034.2
089700 IF--TEST-GF-28.                                                  NC1034.2
089800     IF       IF-D16 GREATER THAN IF-D17                          NC1034.2
089900              PERFORM FAIL                                        NC1034.2
090000              ELSE                                                NC1034.2
090100              PERFORM PASS.                                       NC1034.2
090200     GO TO    IF--WRITE-GF-28.                                    NC1034.2
090300 IF--DELETE-GF-28.                                                NC1034.2
090400     PERFORM  DE-LETE.                                            NC1034.2
090500 IF--WRITE-GF-28.                                                 NC1034.2
090600     MOVE "IF--TEST-GF-28" TO PAR-NAME.                           NC1034.2
090700     PERFORM  PRINT-DETAIL.                                       NC1034.2
090800 IF--INIT-GF-29.                                                  NC1034.2
090900     MOVE    2137.45 TO IF-D27.                                   NC1034.2
091000     MOVE    2137.45 TO IF-D28.                                   NC1034.2
091100 IF--TEST-GF-29.                                                  NC1034.2
091200     IF       IF-D27 GREATER THAN IF-D28                          NC1034.2
091300              PERFORM FAIL                                        NC1034.2
091400              ELSE                                                NC1034.2
091500              PERFORM PASS.                                       NC1034.2
091600     GO       TO IF-WRITE-GF-29.                                  NC1034.2
091700 IF-DELETE-GF-29.                                                 NC1034.2
091800     PERFORM  DE-LETE.                                            NC1034.2
091900 IF-WRITE-GF-29.                                                  NC1034.2
092000     MOVE "IF--TEST-GF-29" TO PAR-NAME.                           NC1034.2
092100     PERFORM  PRINT-DETAIL.                                       NC1034.2
092200 IF--INIT-GF-30.                                                  NC1034.2
092300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
092400     MOVE    +123.45 TO IF-D7.                                    NC1034.2
092500     MOVE    +123.45 TO IF-D14.                                   NC1034.2
092600 IF--TEST-GF-30.                                                  NC1034.2
092700     IF       IF-D14 GREATER THAN IF-D7                           NC1034.2
092800              PERFORM FAIL                                        NC1034.2
092900              ELSE                                                NC1034.2
093000              PERFORM PASS.                                       NC1034.2
093100     GO TO    IF--WRITE-GF-30.                                    NC1034.2
093200 IF--DELETE-GF-30.                                                NC1034.2
093300     PERFORM  DE-LETE.                                            NC1034.2
093400 IF--WRITE-GF-30.                                                 NC1034.2
093500     MOVE "IF--TEST-GF-30" TO PAR-NAME.                           NC1034.2
093600     PERFORM  PRINT-DETAIL.                                       NC1034.2
093700 IF--INIT-GF-31.                                                  NC1034.2
093800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
093900     MOVE     12300  TO IF-D8.                                    NC1034.2
094000     MOVE     12300  TO IF-D15.                                   NC1034.2
094100 IF--TEST-GF-31.                                                  NC1034.2
094200     IF       IF-D15 GREATER THAN IF-D8                           NC1034.2
094300              PERFORM FAIL                                        NC1034.2
094400              ELSE                                                NC1034.2
094500              PERFORM PASS.                                       NC1034.2
094600     GO TO    IF--WRITE-GF-31.                                    NC1034.2
094700 IF--DELETE-GF-31.                                                NC1034.2
094800     PERFORM  DE-LETE.                                            NC1034.2
094900 IF--WRITE-GF-31.                                                 NC1034.2
095000     MOVE "IF--TEST-GF-31" TO PAR-NAME.                           NC1034.2
095100     PERFORM  PRINT-DETAIL.                                       NC1034.2
095200 IF--INIT-GF-32.                                                  NC1034.2
095300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
095400     MOVE     0000012345  TO IF-D20.                              NC1034.2
095500     MOVE     0000012345  TO IF-D21.                              NC1034.2
095600 IF--TEST-GF-32.                                                  NC1034.2
095700     IF       IF-D20 GREATER THAN IF-D21                          NC1034.2
095800              PERFORM FAIL                                        NC1034.2
095900              ELSE                                                NC1034.2
096000              PERFORM PASS.                                       NC1034.2
096100     GO TO    IF--WRITE-GF-32.                                    NC1034.2
096200 IF--DELETE-GF-32.                                                NC1034.2
096300     PERFORM  DE-LETE.                                            NC1034.2
096400 IF--WRITE-GF-32.                                                 NC1034.2
096500     MOVE "IF--TEST-GF-32" TO PAR-NAME.                           NC1034.2
096600     PERFORM  PRINT-DETAIL.                                       NC1034.2
096700 IF--INIT-GF-33.                                                  NC1034.2
096800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
096900     MOVE    "A C D0E"    TO IF-D26.                              NC1034.2
097000     MOVE    "ABCD" TO IF-D25.                                    NC1034.2
097100 IF--TEST-GF-33.                                                  NC1034.2
097200     IF       IF-D26 GREATER THAN IF-D25                          NC1034.2
097300              PERFORM PASS                                        NC1034.2
097400              ELSE                                                NC1034.2
097500              PERFORM FAIL.                                       NC1034.2
097600     GO TO    IF--WRITE-GF-33.                                    NC1034.2
097700 IF--DELETE-GF-33.                                                NC1034.2
097800     PERFORM  DE-LETE.                                            NC1034.2
097900 IF--WRITE-GF-33.                                                 NC1034.2
098000     MOVE "IF--TEST-GF-33" TO PAR-NAME.                           NC1034.2
098100     PERFORM  PRINT-DETAIL.                                       NC1034.2
098200 IF--INIT-GF-34.                                                  NC1034.2
098300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
098400     MOVE    "A C D0E"    TO IF-D26.                              NC1034.2
098500     MOVE    "ABCD" TO IF-D25.                                    NC1034.2
098600 IF--TEST-GF-34.                                                  NC1034.2
098700     IF       IF-D25 GREATER THAN IF-D26                          NC1034.2
098800              PERFORM FAIL                                        NC1034.2
098900              ELSE                                                NC1034.2
099000              PERFORM PASS.                                       NC1034.2
099100     GO TO    IF--WRITE-GF-34.                                    NC1034.2
099200 IF--DELETE-GF-34.                                                NC1034.2
099300     PERFORM  DE-LETE.                                            NC1034.2
099400 IF--WRITE-GF-34.                                                 NC1034.2
099500     MOVE "IF--TEST-GF-34" TO PAR-NAME.                           NC1034.2
099600     PERFORM  PRINT-DETAIL.                                       NC1034.2
099700 IF--INIT-GF-35.                                                  NC1034.2
099800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
099900     MOVE    "BABABABABA" TO IF-D6.                               NC1034.2
100000     MOVE    "BABABABABA" TO IF-D18.                              NC1034.2
100100 IF--TEST-GF-35.                                                  NC1034.2
100200     IF       IF-D6 GREATER THAN IF-D18                           NC1034.2
100300              PERFORM FAIL                                        NC1034.2
100400              ELSE                                                NC1034.2
100500              PERFORM PASS.                                       NC1034.2
100600     GO TO    IF--WRITE-GF-35.                                    NC1034.2
100700 IF--DELETE-GF-35.                                                NC1034.2
100800     PERFORM  DE-LETE.                                            NC1034.2
100900 IF--WRITE-GF-35.                                                 NC1034.2
101000     MOVE "IF--TEST-GF-35" TO PAR-NAME.                           NC1034.2
101100     PERFORM  PRINT-DETAIL.                                       NC1034.2
101200 IF--INIT-GF-36.                                                  NC1034.2
101300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
101400     MOVE    "ABCDEF" TO IF-D12.                                  NC1034.2
101500     MOVE    "ABCDEF" TO IF-D22.                                  NC1034.2
101600 IF--TEST-GF-36.                                                  NC1034.2
101700     IF       IF-D22 GREATER THAN IF-D12                          NC1034.2
101800              PERFORM FAIL                                        NC1034.2
101900              ELSE                                                NC1034.2
102000              PERFORM PASS.                                       NC1034.2
102100     GO TO    IF--WRITE-GF-36.                                    NC1034.2
102200 IF--DELETE-GF-36.                                                NC1034.2
102300     PERFORM  DE-LETE.                                            NC1034.2
102400 IF--WRITE-GF-36.                                                 NC1034.2
102500     MOVE "IF--TEST-GF-36" TO PAR-NAME.                           NC1034.2
102600     PERFORM  PRINT-DETAIL.                                       NC1034.2
102700 IF--INIT-GF-37.                                                  NC1034.2
102800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
102900     MOVE    "COMPARE--LESS THAN" TO FEATURE.                     NC1034.2
103000     MOVE     +123.45 TO IF-D7.                                   NC1034.2
103100 IF--TEST-GF-37.                                                  NC1034.2
103200     IF       IF-D7 IS LESS THAN 123.45                           NC1034.2
103300              PERFORM FAIL                                        NC1034.2
103400              ELSE                                                NC1034.2
103500              PERFORM PASS.                                       NC1034.2
103600     GO TO    IF--WRITE-GF-37.                                    NC1034.2
103700 IF--DELETE-GF-37.                                                NC1034.2
103800     PERFORM  DE-LETE.                                            NC1034.2
103900 IF--WRITE-GF-37.                                                 NC1034.2
104000     MOVE "IF--TEST-GF-37" TO PAR-NAME.                           NC1034.2
104100     PERFORM  PRINT-DETAIL.                                       NC1034.2
104200 IF--INIT-GF-38.                                                  NC1034.2
104300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
104400     MOVE    "ABCDEF" TO IF-D11.                                  NC1034.2
104500 IF--TEST-GF-38.                                                  NC1034.2
104600     IF       IF-D11 LESS THAN "ABCDEF"                           NC1034.2
104700              PERFORM FAIL                                        NC1034.2
104800              ELSE                                                NC1034.2
104900              PERFORM PASS.                                       NC1034.2
105000     GO TO    IF--WRITE-GF-38.                                    NC1034.2
105100 IF--DELETE-GF-38.                                                NC1034.2
105200     PERFORM  DE-LETE.                                            NC1034.2
105300 IF--WRITE-GF-38.                                                 NC1034.2
105400     MOVE "IF--TEST-GF-38" TO PAR-NAME.                           NC1034.2
105500     PERFORM  PRINT-DETAIL.                                       NC1034.2
105600 IF--INIT-GF-39.                                                  NC1034.2
105700     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
105800     MOVE    "BABABABABA" TO IF-D6.                               NC1034.2
105900     MOVE    "BABABABABA" TO IF-D18.                              NC1034.2
106000 IF--TEST-GF-39.                                                  NC1034.2
106100     IF       IF-D6 LESS THAN IF-D18                              NC1034.2
106200              PERFORM FAIL                                        NC1034.2
106300              ELSE                                                NC1034.2
106400              PERFORM PASS.                                       NC1034.2
106500     GO TO    IF--WRITE-GF-39.                                    NC1034.2
106600 IF--DELETE-GF-39.                                                NC1034.2
106700     PERFORM  DE-LETE.                                            NC1034.2
106800 IF--WRITE-GF-39.                                                 NC1034.2
106900     MOVE "IF--TEST-GF-39" TO PAR-NAME.                           NC1034.2
107000     PERFORM  PRINT-DETAIL.                                       NC1034.2
107100 IF--INIT-GF-40.                                                  NC1034.2
107200     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
107300     MOVE    0000012345   TO IF-D20.                              NC1034.2
107400     MOVE    0000012345   TO IF-D21.                              NC1034.2
107500 IF--TEST-GF-40.                                                  NC1034.2
107600     IF       IF-D20 LESS THAN IF-D21                             NC1034.2
107700              PERFORM FAIL                                        NC1034.2
107800              ELSE                                                NC1034.2
107900              PERFORM PASS.                                       NC1034.2
108000     GO TO    IF--WRITE-GF-40.                                    NC1034.2
108100 IF--DELETE-GF-40.                                                NC1034.2
108200     PERFORM  DE-LETE.                                            NC1034.2
108300 IF--WRITE-GF-40.                                                 NC1034.2
108400     MOVE "IF--TEST-GF-40" TO PAR-NAME.                           NC1034.2
108500     PERFORM  PRINT-DETAIL.                                       NC1034.2
108600 IF--INIT-D.                                                      NC1034.2
108700     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
108800     MOVE "COMPARE--NOT EQUAL" TO FEATURE.                        NC1034.2
108900     MOVE  +123.45 TO IF-D7.                                      NC1034.2
109000 IF--TEST-GF-41.                                                  NC1034.2
109100     IF       IF-D7 IS NOT EQUAL TO 23.45                         NC1034.2
109200              PERFORM PASS                                        NC1034.2
109300              ELSE                                                NC1034.2
109400              PERFORM FAIL.                                       NC1034.2
109500     GO TO    IF--WRITE-GF-41.                                    NC1034.2
109600 IF--DELETE-GF-41.                                                NC1034.2
109700     PERFORM  DE-LETE.                                            NC1034.2
109800 IF--WRITE-GF-41.                                                 NC1034.2
109900     MOVE "IF--TEST-GF-41" TO PAR-NAME.                           NC1034.2
110000     PERFORM  PRINT-DETAIL.                                       NC1034.2
110100 IF--INIT-GF-42.                                                  NC1034.2
110200     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
110300     MOVE    "ABCDEF"   TO IF-D11.                                NC1034.2
110400 IF--TEST-GF-42.                                                  NC1034.2
110500     IF       IF-D11 NOT EQUAL TO "ABCDE "                        NC1034.2
110600              PERFORM PASS                                        NC1034.2
110700              ELSE                                                NC1034.2
110800              PERFORM FAIL.                                       NC1034.2
110900     GO TO    IF--WRITE-GF-42.                                    NC1034.2
111000 IF--DELETE-GF-42.                                                NC1034.2
111100     PERFORM  DE-LETE.                                            NC1034.2
111200 IF--WRITE-GF-42.                                                 NC1034.2
111300     MOVE "IF--TEST-GF-42" TO PAR-NAME.                           NC1034.2
111400     PERFORM  PRINT-DETAIL.                                       NC1034.2
111500 IF--INIT-GF-43.                                                  NC1034.2
111600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
111700     MOVE    "BABABABABA" TO IF-D6.                               NC1034.2
111800     MOVE    "BABABABABA" TO IF-D18.                              NC1034.2
111900 IF--TEST-GF-43.                                                  NC1034.2
112000     IF       IF-D6 NOT EQUAL TO IF-D18                           NC1034.2
112100              PERFORM FAIL                                        NC1034.2
112200              ELSE                                                NC1034.2
112300              PERFORM PASS.                                       NC1034.2
112400     GO TO    IF--WRITE-GF-43.                                    NC1034.2
112500 IF--DELETE-GF-43.                                                NC1034.2
112600     PERFORM  DE-LETE.                                            NC1034.2
112700 IF--WRITE-GF-43.                                                 NC1034.2
112800     MOVE "IF--TEST-GF-43" TO PAR-NAME.                           NC1034.2
112900     PERFORM  PRINT-DETAIL.                                       NC1034.2
113000 IF--INIT-GF-44.                                                  NC1034.2
113100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
113200     MOVE    0000012345   TO IF-D20.                              NC1034.2
113300     MOVE    0000012345   TO IF-D21.                              NC1034.2
113400 IF--TEST-GF-44.                                                  NC1034.2
113500     IF       IF-D20 NOT EQUAL TO IF-D21                          NC1034.2
113600              PERFORM FAIL                                        NC1034.2
113700              ELSE                                                NC1034.2
113800              PERFORM PASS.                                       NC1034.2
113900     GO TO    IF--WRITE-GF-44.                                    NC1034.2
114000 IF--DELETE-GF-44.                                                NC1034.2
114100     PERFORM  DE-LETE.                                            NC1034.2
114200 IF--WRITE-GF-44.                                                 NC1034.2
114300     MOVE "IF--TEST-GF-44" TO PAR-NAME.                           NC1034.2
114400     PERFORM  PRINT-DETAIL.                                       NC1034.2
114500 IF--INIT-GF-45.                                                  NC1034.2
114600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
114700     MOVE "COMPARE--NOT LESS" TO FEATURE.                         NC1034.2
114800     MOVE  +123.45 TO IF-D7.                                      NC1034.2
114900 IF--TEST-GF-45.                                                  NC1034.2
115000     IF       IF-D7 IS NOT LESS THAN 123.45                       NC1034.2
115100              PERFORM PASS                                        NC1034.2
115200              ELSE                                                NC1034.2
115300              PERFORM FAIL.                                       NC1034.2
115400     GO TO    IF--WRITE-GF-45.                                    NC1034.2
115500 IF--DELETE-GF-45.                                                NC1034.2
115600     PERFORM  DE-LETE.                                            NC1034.2
115700 IF--WRITE-GF-45.                                                 NC1034.2
115800     MOVE "IF--TEST-GF-45" TO PAR-NAME.                           NC1034.2
115900     PERFORM  PRINT-DETAIL.                                       NC1034.2
116000 IF--INIT-GF-46.                                                  NC1034.2
116100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
116200     MOVE    "ABCDEF"     TO IF-D11.                              NC1034.2
116300 IF--TEST-GF-46.                                                  NC1034.2
116400     IF       IF-D11 IS NOT LESS THAN "ABCDEF"                    NC1034.2
116500              PERFORM PASS                                        NC1034.2
116600              ELSE                                                NC1034.2
116700              PERFORM FAIL.                                       NC1034.2
116800     GO TO    IF--WRITE-GF-46.                                    NC1034.2
116900 IF--DELETE-GF-46.                                                NC1034.2
117000     PERFORM  DE-LETE.                                            NC1034.2
117100 IF--WRITE-GF-46.                                                 NC1034.2
117200     MOVE "IF--TEST-GF-46" TO PAR-NAME.                           NC1034.2
117300     PERFORM  PRINT-DETAIL.                                       NC1034.2
117400 IF--INIT-GF-47.                                                  NC1034.2
117500     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
117600     MOVE    "BABABABABA" TO IF-D6.                               NC1034.2
117700     MOVE    "BABABABABA" TO IF-D18.                              NC1034.2
117800 IF--TEST-GF-47.                                                  NC1034.2
117900     IF       IF-D6 IS NOT LESS THAN IF-D18                       NC1034.2
118000              PERFORM PASS                                        NC1034.2
118100              ELSE                                                NC1034.2
118200              PERFORM FAIL.                                       NC1034.2
118300     GO TO    IF--WRITE-GF-47.                                    NC1034.2
118400 IF--DELETE-GF-47.                                                NC1034.2
118500     PERFORM  DE-LETE.                                            NC1034.2
118600 IF--WRITE-GF-47.                                                 NC1034.2
118700     MOVE "IF--TEST-GF-47" TO PAR-NAME.                           NC1034.2
118800     PERFORM  PRINT-DETAIL.                                       NC1034.2
118900 IF--INIT-GF-48.                                                  NC1034.2
119000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
119100     MOVE    0000012345   TO IF-D20.                              NC1034.2
119200     MOVE    0000012345   TO IF-D21.                              NC1034.2
119300 IF--TEST-GF-48.                                                  NC1034.2
119400     IF       IF-D20 NOT LESS THAN IF-D21                         NC1034.2
119500              PERFORM PASS                                        NC1034.2
119600              ELSE                                                NC1034.2
119700              PERFORM FAIL.                                       NC1034.2
119800     GO TO    IF--WRITE-GF-48.                                    NC1034.2
119900 IF--DELETE-GF-48.                                                NC1034.2
120000     PERFORM  DE-LETE.                                            NC1034.2
120100 IF--WRITE-GF-48.                                                 NC1034.2
120200     MOVE "IF--TEST-GF-48" TO PAR-NAME.                           NC1034.2
120300     PERFORM  PRINT-DETAIL.                                       NC1034.2
120400 IF--INIT-GF-49.                                                  NC1034.2
120500     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
120600     MOVE "COMPARE--NOT GREATER" TO FEATURE.                      NC1034.2
120700     MOVE   +123.45 TO IF-D7.                                     NC1034.2
120800 IF--TEST-GF-49.                                                  NC1034.2
120900     IF       IF-D7 NOT GREATER THAN 123.45                       NC1034.2
121000              PERFORM PASS                                        NC1034.2
121100              ELSE                                                NC1034.2
121200              PERFORM FAIL.                                       NC1034.2
121300     GO TO    IF--WRITE-GF-49.                                    NC1034.2
121400 IF--DELETE-GF-49.                                                NC1034.2
121500     PERFORM  DE-LETE.                                            NC1034.2
121600 IF--WRITE-GF-49.                                                 NC1034.2
121700     MOVE "IF--TEST-GF-49" TO PAR-NAME.                           NC1034.2
121800     PERFORM  PRINT-DETAIL.                                       NC1034.2
121900 IF--INIT-GF-50.                                                  NC1034.2
122000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
122100     MOVE    "ABCDEF"     TO IF-D11.                              NC1034.2
122200 IF--TEST-GF-50.                                                  NC1034.2
122300     IF IF-D11 IS NOT GREATER THAN "ABCD  "                       NC1034.2
122400              PERFORM FAIL                                        NC1034.2
122500              ELSE                                                NC1034.2
122600              PERFORM PASS.                                       NC1034.2
122700*        THIS TEST ASSUMES THAT BLANK PRECEDES THE LETTERS OF     NC1034.2
122800*    THE ALPHABET IN THE COLLATING SEQUENCE.                      NC1034.2
122900     GO TO    IF--WRITE-GF-50.                                    NC1034.2
123000 IF--DELETE-GF-50.                                                NC1034.2
123100     PERFORM  DE-LETE.                                            NC1034.2
123200 IF--WRITE-GF-50.                                                 NC1034.2
123300     MOVE "IF--TEST-GF-50" TO PAR-NAME.                           NC1034.2
123400     PERFORM  PRINT-DETAIL.                                       NC1034.2
123500 IF--INIT-GF-51.                                                  NC1034.2
123600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
123700     MOVE    "BABABABABA" TO IF-D6.                               NC1034.2
123800     MOVE    "BABABABABA" TO IF-D18.                              NC1034.2
123900 IF--TEST-GF-51.                                                  NC1034.2
124000     IF       IF-D6 NOT GREATER THAN IF-D18                       NC1034.2
124100              PERFORM PASS                                        NC1034.2
124200              ELSE                                                NC1034.2
124300              PERFORM FAIL.                                       NC1034.2
124400     GO TO    IF--WRITE-GF-51.                                    NC1034.2
124500 IF--DELETE-GF-51.                                                NC1034.2
124600     PERFORM  DE-LETE.                                            NC1034.2
124700 IF--WRITE-GF-51.                                                 NC1034.2
124800     MOVE "IF--TEST-GF-51" TO PAR-NAME.                           NC1034.2
124900     PERFORM  PRINT-DETAIL.                                       NC1034.2
125000 IF--INIT-GF-52.                                                  NC1034.2
125100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
125200     MOVE    "ABCDEF"     TO IF-D11.                              NC1034.2
125300     MOVE    "ABCDEF"     TO IF-D12.                              NC1034.2
125400 IF--TEST-GF-52.                                                  NC1034.2
125500     IF       IF-D12 NOT GREATER THAN IF-D11                      NC1034.2
125600              PERFORM PASS                                        NC1034.2
125700              ELSE                                                NC1034.2
125800              PERFORM FAIL.                                       NC1034.2
125900     GO TO    IF--WRITE-GF-52.                                    NC1034.2
126000 IF--DELETE-GF-52.                                                NC1034.2
126100     PERFORM  DE-LETE.                                            NC1034.2
126200 IF--WRITE-GF-52.                                                 NC1034.2
126300     MOVE "IF--TEST-GF-52" TO PAR-NAME.                           NC1034.2
126400     PERFORM  PRINT-DETAIL.                                       NC1034.2
126500 IF--INIT-GF-53.                                                  NC1034.2
126600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
126700     MOVE "COMPARE--HIGH LOW " TO FEATURE.                        NC1034.2
126800     MOVE   LOW-VALUE TO LOW-VAL.                                 NC1034.2
126900 IF--TEST-GF-53.                                                  NC1034.2
127000     IF HIGH-VALUE NOT GREATER THAN LOW-VAL                       NC1034.2
127100              PERFORM FAIL                                        NC1034.2
127200              ELSE                                                NC1034.2
127300              PERFORM PASS.                                       NC1034.2
127400     GO TO    IF--WRITE-GF-53.                                    NC1034.2
127500 IF--DELETE-GF-53.                                                NC1034.2
127600     PERFORM  DE-LETE.                                            NC1034.2
127700 IF--WRITE-GF-53.                                                 NC1034.2
127800     MOVE "IF--TEST-GF-53" TO PAR-NAME.                           NC1034.2
127900     PERFORM  PRINT-DETAIL.                                       NC1034.2
128000 IF--INIT-GF-54.                                                  NC1034.2
128100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
128200     MOVE   LOW-VALUE TO LOW-VAL.                                 NC1034.2
128300 IF--TEST-GF-54.                                                  NC1034.2
128400     IF LOW-VAL LESS THAN HIGH-VALUE                              NC1034.2
128500              PERFORM PASS                                        NC1034.2
128600              ELSE                                                NC1034.2
128700              PERFORM FAIL.                                       NC1034.2
128800     GO TO    IF--WRITE-GF-54.                                    NC1034.2
128900 IF--DELETE-GF-54.                                                NC1034.2
129000     PERFORM  DE-LETE.                                            NC1034.2
129100 IF--WRITE-GF-54.                                                 NC1034.2
129200     MOVE "IF--TEST-GF-54" TO PAR-NAME.                           NC1034.2
129300     PERFORM  PRINT-DETAIL.                                       NC1034.2
129400 IF--INIT-GF-55.                                                  NC1034.2
129500     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
129600     MOVE   LOW-VALUE TO LOW-VAL.                                 NC1034.2
129700     MOVE   1 TO ZERO-01.                                         NC1034.2
129800 IF--TEST-GF-55.                                                  NC1034.2
129900     IF ZERO-01 GREATER THAN LOW-VALUE                            NC1034.2
130000              PERFORM PASS                                        NC1034.2
130100              ELSE                                                NC1034.2
130200              PERFORM FAIL.                                       NC1034.2
130300     GO TO    IF--WRITE-GF-55.                                    NC1034.2
130400 IF--DELETE-GF-55.                                                NC1034.2
130500     PERFORM  DE-LETE.                                            NC1034.2
130600 IF--WRITE-GF-55.                                                 NC1034.2
130700     MOVE "IF--TEST-GF-55" TO PAR-NAME.                           NC1034.2
130800     PERFORM  PRINT-DETAIL.                                       NC1034.2
130900 IF--INIT-GF-56.                                                  NC1034.2
131000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
131100     MOVE    "ABC"   TO ABC.                                      NC1034.2
131200 IF--TEST-GF-56.                                                  NC1034.2
131300     IF ABC GREATER THAN HIGH-VALUE                               NC1034.2
131400              PERFORM FAIL                                        NC1034.2
131500              ELSE                                                NC1034.2
131600              PERFORM PASS.                                       NC1034.2
131700     GO TO    IF--WRITE-GF-56.                                    NC1034.2
131800 IF--DELETE-GF-56.                                                NC1034.2
131900     PERFORM  DE-LETE.                                            NC1034.2
132000 IF--WRITE-GF-56.                                                 NC1034.2
132100     MOVE "IF--TEST-GF-56" TO PAR-NAME.                           NC1034.2
132200     PERFORM  PRINT-DETAIL.                                       NC1034.2
132300 IF--INIT-GF-57.                                                  NC1034.2
132400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
132500     MOVE     999999999999999998 TO NINE-17-8.                    NC1034.2
132600 IF--TEST-GF-57.                                                  NC1034.2
132700     IF NINE-17-8 LESS THAN HIGH-VALUE                            NC1034.2
132800              PERFORM PASS                                        NC1034.2
132900              ELSE                                                NC1034.2
133000              PERFORM FAIL.                                       NC1034.2
133100     GO TO    IF--WRITE-GF-57.                                    NC1034.2
133200 IF--DELETE-GF-57.                                                NC1034.2
133300     PERFORM  DE-LETE.                                            NC1034.2
133400 IF--WRITE-GF-57.                                                 NC1034.2
133500     MOVE "IF--TEST-GF-57" TO PAR-NAME.                           NC1034.2
133600     PERFORM  PRINT-DETAIL.                                       NC1034.2
133700 IF--INIT-GF-58.                                                  NC1034.2
133800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
133900     MOVE     0 TO ZERO-NULL.                                     NC1034.2
134000 IF--TEST-GF-58.                                                  NC1034.2
134100     IF       ZERO-NULL NOT EQUAL TO HIGH-VALUE                   NC1034.2
134200              PERFORM PASS                                        NC1034.2
134300              ELSE                                                NC1034.2
134400              PERFORM FAIL.                                       NC1034.2
134500     GO TO    IF--WRITE-GF-58.                                    NC1034.2
134600 IF--DELETE-GF-58.                                                NC1034.2
134700     PERFORM  DE-LETE.                                            NC1034.2
134800 IF--WRITE-GF-58.                                                 NC1034.2
134900     MOVE "IF--TEST-GF-58" TO PAR-NAME.                           NC1034.2
135000     PERFORM  PRINT-DETAIL.                                       NC1034.2
135100 IF--INIT-GF-59.                                                  NC1034.2
135200     MOVE "ABC" TO ABC.                                           NC1034.2
135300 IF--TEST-GF-59.                                                  NC1034.2
135400     IF  ABC LESS THAN LOW-VALUE                                  NC1034.2
135500         PERFORM FAIL                                             NC1034.2
135600         GO TO IF--WRITE-GF-59.                                   NC1034.2
135700     PERFORM PASS.                                                NC1034.2
135800     GO TO  IF--WRITE-GF-59.                                      NC1034.2
135900 IF--DELETE-GF-59.                                                NC1034.2
136000     PERFORM DE-LETE.                                             NC1034.2
136100 IF--WRITE-GF-59.                                                 NC1034.2
136200     MOVE  "IF--TEST-GF-59" TO PAR-NAME.                          NC1034.2
136300     PERFORM PRINT-DETAIL.                                        NC1034.2
136400 IF--INIT-GF-60.                                                  NC1034.2
136500     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
136600     MOVE  "COMPARE--EQUAL" TO FEATURE.                           NC1034.2
136700     MOVE   0 TO IF-D32.                                          NC1034.2
136800     MOVE  -0 TO IF-D33.                                          NC1034.2
136900 IF--TEST-GF-60.                                                  NC1034.2
137000     IF      IF-D32 EQUAL TO IF-D33                               NC1034.2
137100             PERFORM PASS                                         NC1034.2
137200             ELSE                                                 NC1034.2
137300             PERFORM FAIL.                                        NC1034.2
137400     GO TO   IF--WRITE-GF-60.                                     NC1034.2
137500 IF--DELETE-GF-60.                                                NC1034.2
137600     PERFORM  DE-LETE.                                            NC1034.2
137700 IF--WRITE-GF-60.                                                 NC1034.2
137800     MOVE "IF--TEST-GF-60" TO PAR-NAME.                           NC1034.2
137900     PERFORM  PRINT-DETAIL.                                       NC1034.2
138000 IF--INIT-GF-61.                                                  NC1034.2
138100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
138200     MOVE   0 TO IF-D32.                                          NC1034.2
138300     MOVE  +0 TO IF-D34.                                          NC1034.2
138400 IF--TEST-GF-61.                                                  NC1034.2
138500     IF       IF-D32 EQUAL TO IF-D34                              NC1034.2
138600              PERFORM PASS                                        NC1034.2
138700              ELSE                                                NC1034.2
138800              PERFORM FAIL.                                       NC1034.2
138900     GO TO    IF--WRITE-GF-61.                                    NC1034.2
139000 IF--DELETE-GF-61.                                                NC1034.2
139100     PERFORM  DE-LETE.                                            NC1034.2
139200 IF--WRITE-GF-61.                                                 NC1034.2
139300     MOVE "IF--TEST-GF-61" TO PAR-NAME.                           NC1034.2
139400     PERFORM  PRINT-DETAIL.                                       NC1034.2
139500 IF--INIT-GF-62.                                                  NC1034.2
139600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
139700     MOVE  -0 TO IF-D33.                                          NC1034.2
139800     MOVE  +0 TO IF-D34.                                          NC1034.2
139900 IF--TEST-GF-62.                                                  NC1034.2
140000     IF       IF-D33 EQUAL TO IF-D34                              NC1034.2
140100              PERFORM PASS                                        NC1034.2
140200              ELSE                                                NC1034.2
140300              PERFORM FAIL.                                       NC1034.2
140400     GO TO    IF--WRITE-GF-62.                                    NC1034.2
140500 IF--DELETE-GF-62.                                                NC1034.2
140600     PERFORM  DE-LETE.                                            NC1034.2
140700 IF--WRITE-GF-62.                                                 NC1034.2
140800     MOVE "IF--TEST-GF-62" TO PAR-NAME.                           NC1034.2
140900     PERFORM  PRINT-DETAIL.                                       NC1034.2
141000 IF--INIT-GF-63.                                                  NC1034.2
141100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
141200     MOVE   ZERO TO AZERO-DS-05V05.                               NC1034.2
141300 IF--TEST-GF-63.                                                  NC1034.2
141400     IF AZERO-DS-05V05 EQUAL TO ZERO                              NC1034.2
141500         PERFORM PASS                                             NC1034.2
141600         GO TO IF-WRITE-GF-63.                                    NC1034.2
141700     GO TO IF-FAIL-GF-63.                                         NC1034.2
141800 IF-DELETE-GF-63.                                                 NC1034.2
141900     PERFORM DE-LETE.                                             NC1034.2
142000     GO TO IF-WRITE-GF-63.                                        NC1034.2
142100 IF-FAIL-GF-63.                                                   NC1034.2
142200     MOVE 00000.00000 TO CORRECT-N.                               NC1034.2
142300     MOVE AZERO-DS-05V05 TO COMPUTED-N.                           NC1034.2
142400     PERFORM FAIL.                                                NC1034.2
142500 IF-WRITE-GF-63.                                                  NC1034.2
142600     MOVE "IF--TEST-GF-63         " TO PAR-NAME.                  NC1034.2
142700     PERFORM PRINT-DETAIL.                                        NC1034.2
142800 IF--INIT-GF-64.                                                  NC1034.2
142900     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
143000     MOVE SPACE TO CORRECT-A.                                     NC1034.2
143100 IF--TEST-GF-64.                                                  NC1034.2
143200     IF SPACE EQUAL TO CORRECT-A                                  NC1034.2
143300         PERFORM PASS                                             NC1034.2
143400         GO TO IF-WRITE-GF-64.                                    NC1034.2
143500     GO TO IF-FAIL-GF-64.                                         NC1034.2
143600 IF-DELETE-GF-64.                                                 NC1034.2
143700     PERFORM DE-LETE.                                             NC1034.2
143800     GO TO IF-WRITE-GF-64.                                        NC1034.2
143900 IF-FAIL-GF-64.                                                   NC1034.2
144000     MOVE CORRECT-A TO COMPUTED-A.                                NC1034.2
144100     MOVE SPACE TO CORRECT-A.                                     NC1034.2
144200     PERFORM FAIL.                                                NC1034.2
144300 IF-WRITE-GF-64.                                                  NC1034.2
144400     MOVE "IF--TEST-GF-64         " TO PAR-NAME.                  NC1034.2
144500     PERFORM PRINT-DETAIL.                                        NC1034.2
144600 IF--INIT-GF-65.                                                  NC1034.2
144700     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
144800     MOVE 111111111111111111 TO A18ONES-DS-18V00.                 NC1034.2
144900     MOVE "111111111111111111" TO ONES-XN-00018.                  NC1034.2
145000 IF--TEST-GF-65.                                                  NC1034.2
145100     IF A18ONES-DS-18V00 EQUAL TO ONES-XN-00018                   NC1034.2
145200         PERFORM PASS                                             NC1034.2
145300         GO TO IF-WRITE-GF-65.                                    NC1034.2
145400     GO TO IF-FAIL-GF-65.                                         NC1034.2
145500 IF-DELETE-GF-65.                                                 NC1034.2
145600     PERFORM DE-LETE.                                             NC1034.2
145700     GO TO IF-WRITE-GF-65.                                        NC1034.2
145800 IF-FAIL-GF-65.                                                   NC1034.2
145900     MOVE ONES-XN-00018 TO CORRECT-A.                             NC1034.2
146000     MOVE A18ONES-DS-18V00 TO COMPUTED-A.                         NC1034.2
146100     MOVE "FIELDS DIDNT COMPARE EQUAL" TO RE-MARK                 NC1034.2
146200     PERFORM FAIL.                                                NC1034.2
146300 IF-WRITE-GF-65.                                                  NC1034.2
146400     MOVE "IF--TEST-GF-65 " TO PAR-NAME.                          NC1034.2
146500     PERFORM PRINT-DETAIL.                                        NC1034.2
146600 IF--INIT-GF-66.                                                  NC1034.2
146700     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
146800     MOVE 22 TO TWOS-XN-00002.                                    NC1034.2
146900     MOVE 99 TO A99-DS-02V00.                                     NC1034.2
147000 IF--TEST-GF-66.                                                  NC1034.2
147100     IF TWOS-XN-00002 IS EQUAL TO A99-DS-02V00                    NC1034.2
147200         MOVE TWOS-XN-00002 TO COMPUTED-A                         NC1034.2
147300         MOVE A99-DS-02V00 TO CORRECT-A                           NC1034.2
147400         PERFORM FAIL                                             NC1034.2
147500         GO TO IF-WRITE-GF-66.                                    NC1034.2
147600     PERFORM PASS.                                                NC1034.2
147700     GO TO IF-WRITE-GF-66.                                        NC1034.2
147800 IF-DELETE-GF-66.                                                 NC1034.2
147900     PERFORM DE-LETE.                                             NC1034.2
148000 IF-WRITE-GF-66.                                                  NC1034.2
148100     MOVE "IF--TEST-GF-66 " TO PAR-NAME.                          NC1034.2
148200     PERFORM PRINT-DETAIL.                                        NC1034.2
148300 IF--INIT-GF-67.                                                  NC1034.2
148400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
148500     MOVE     "COMPARE--LESS THAN  " TO FEATURE.                  NC1034.2
148600     MOVE 111111111.111111111 TO A18ONES-DS-09V09.                NC1034.2
148700     MOVE 99 TO A99-DS-02V00.                                     NC1034.2
148800 IF--TEST-GF-67.                                                  NC1034.2
148900     IF A99-DS-02V00 LESS THAN A18ONES-DS-09V09                   NC1034.2
149000         PERFORM PASS                                             NC1034.2
149100         GO TO IF-WRITE-GF-67 ELSE                                NC1034.2
149200     GO TO IF-FAIL-GF-67.                                         NC1034.2
149300 IF-DELETE-GF-67.                                                 NC1034.2
149400     PERFORM DE-LETE.                                             NC1034.2
149500     GO TO IF-WRITE-GF-67.                                        NC1034.2
149600 IF-FAIL-GF-67.                                                   NC1034.2
149700     MOVE A99-DS-02V00 TO CORRECT-A.                              NC1034.2
149800     MOVE A18ONES-DS-09V09 TO COMPUTED-N.                         NC1034.2
149900     PERFORM FAIL.                                                NC1034.2
150000 IF-WRITE-GF-67.                                                  NC1034.2
150100     MOVE "IF--TEST-GF-67 " TO PAR-NAME.                          NC1034.2
150200     PERFORM PRINT-DETAIL.                                        NC1034.2
150300 IF--INIT-GF-68.                                                  NC1034.2
150400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
150500     MOVE   "11" TO ONES-XN-00002.                                NC1034.2
150600 IF--TEST-GF-68.                                                  NC1034.2
150700     IF "11" LESS THAN ONES-XN-00002                              NC1034.2
150800         MOVE "11" TO CORRECT-A                                   NC1034.2
150900         MOVE ONES-XN-00002 TO COMPUTED-A                         NC1034.2
151000         PERFORM FAIL                                             NC1034.2
151100         GO TO IF-WRITE-GF-68 ELSE                                NC1034.2
151200     PERFORM PASS                                                 NC1034.2
151300     GO TO IF-WRITE-GF-68.                                        NC1034.2
151400 IF-DELETE-GF-68.                                                 NC1034.2
151500     PERFORM DE-LETE.                                             NC1034.2
151600 IF-WRITE-GF-68.                                                  NC1034.2
151700     MOVE "IF--TEST-GF-68 " TO PAR-NAME.                          NC1034.2
151800     PERFORM PRINT-DETAIL.                                        NC1034.2
151900 IF--INIT-GF-69.                                                  NC1034.2
152000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
152100     MOVE   "11" TO ONES-XN-00002.                                NC1034.2
152200     MOVE    22  TO A02TWOS-DU-02V00.                             NC1034.2
152300 IF--TEST-GF-69.                                                  NC1034.2
152400     IF  A02TWOS-DU-02V00 LESS THAN ONES-XN-00002                 NC1034.2
152500         MOVE ONES-XN-00002 TO CORRECT-A                          NC1034.2
152600         MOVE A02TWOS-DU-02V00 TO COMPUTED-A                      NC1034.2
152700         PERFORM FAIL                                             NC1034.2
152800         GO TO IF-WRITE-GF-69 ELSE                                NC1034.2
152900     PERFORM PASS                                                 NC1034.2
153000     GO TO IF-WRITE-GF-69.                                        NC1034.2
153100 IF-DELETE-GF-69.                                                 NC1034.2
153200     PERFORM DE-LETE.                                             NC1034.2
153300 IF-WRITE-GF-69.                                                  NC1034.2
153400     MOVE "IF--TEST-GF-69 " TO PAR-NAME.                          NC1034.2
153500     PERFORM PRINT-DETAIL.                                        NC1034.2
153600 IF--INIT-GF-70.                                                  NC1034.2
153700     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
153800     MOVE   "22" TO TWOS-XN-00002.                                NC1034.2
153900     MOVE    22  TO A02TWOS-DU-02V00.                             NC1034.2
154000 IF--TEST-GF-70.                                                  NC1034.2
154100     IF TWOS-XN-00002 LESS THAN A02TWOS-DU-02V00                  NC1034.2
154200         MOVE TWOS-XN-00002 TO CORRECT-A                          NC1034.2
154300         MOVE A02TWOS-DU-02V00 TO COMPUTED-A                      NC1034.2
154400         PERFORM FAIL                                             NC1034.2
154500         GO TO IF-WRITE-GF-70  ELSE                               NC1034.2
154600     PERFORM PASS                                                 NC1034.2
154700     GO TO IF-WRITE-GF-70.                                        NC1034.2
154800 IF-DELETE-GF-70.                                                 NC1034.2
154900     PERFORM DE-LETE.                                             NC1034.2
155000 IF-WRITE-GF-70.                                                  NC1034.2
155100     MOVE "IF--TEST-70 " TO PAR-NAME.                             NC1034.2
155200     PERFORM PRINT-DETAIL.                                        NC1034.2
155300 IF--INIT-GF-71.                                                  NC1034.2
155400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
155500     MOVE   "COMPARE--GREATER    " TO FEATURE.                    NC1034.2
155600     MOVE    99  TO A99-DS-02V00.                                 NC1034.2
155700 IF--TEST-GF-71.                                                  NC1034.2
155800     IF A99-DS-02V00 GREATER THAN 88.9 NEXT SENTENCE  ELSE        NC1034.2
155900         MOVE A99-DS-02V00 TO CORRECT-A                           NC1034.2
156000         MOVE "88.9" TO COMPUTED-A                                NC1034.2
156100         PERFORM FAIL                                             NC1034.2
156200         GO TO IF-WRITE-GF-71.                                    NC1034.2
156300     PERFORM PASS.                                                NC1034.2
156400     GO TO IF-WRITE-GF-71.                                        NC1034.2
156500 IF-DELETE-GF-71.                                                 NC1034.2
156600     PERFORM DE-LETE.                                             NC1034.2
156700 IF-WRITE-GF-71.                                                  NC1034.2
156800     MOVE "IF--TEST-GF-71 " TO PAR-NAME.                          NC1034.2
156900     PERFORM PRINT-DETAIL.                                        NC1034.2
157000 IF--INIT-GF-72.                                                  NC1034.2
157100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
157200     MOVE   "11" TO ONES-XN-00002.                                NC1034.2
157300     MOVE   "22" TO TWOS-XN-00002.                                NC1034.2
157400 IF--TEST-GF-72.                                                  NC1034.2
157500     IF ONES-XN-00002 GREATER THAN TWOS-XN-00002 NEXT SENTENCE    NC1034.2
157600         ELSE PERFORM PASS                                        NC1034.2
157700         GO TO IF-WRITE-GF-72.                                    NC1034.2
157800     MOVE ONES-XN-00002 TO COMPUTED-A.                            NC1034.2
157900     MOVE TWOS-XN-00002 TO CORRECT-A.                             NC1034.2
158000     PERFORM FAIL.                                                NC1034.2
158100     GO TO IF-WRITE-GF-72.                                        NC1034.2
158200 IF-DELETE-GF-72.                                                 NC1034.2
158300     PERFORM DE-LETE.                                             NC1034.2
158400 IF-WRITE-GF-72.                                                  NC1034.2
158500     MOVE "IF--TEST-GF-72 " TO PAR-NAME.                          NC1034.2
158600     PERFORM PRINT-DETAIL.                                        NC1034.2
158700 IF--INIT-GF-73.                                                  NC1034.2
158800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
158900     MOVE   "11" TO ONES-XN-00002.                                NC1034.2
159000     MOVE    22  TO A02TWOS-DU-02V00.                             NC1034.2
159100 IF--TEST-GF-73.                                                  NC1034.2
159200     IF A02TWOS-DU-02V00 GREATER THAN ONES-XN-00002               NC1034.2
159300         NEXT SENTENCE  ELSE                                      NC1034.2
159400         MOVE A02TWOS-DU-02V00 TO CORRECT-A                       NC1034.2
159500         MOVE ONES-XN-00002 TO COMPUTED-A                         NC1034.2
159600         PERFORM FAIL                                             NC1034.2
159700         GO TO IF-WRITE-GF-73.                                    NC1034.2
159800     PERFORM PASS.                                                NC1034.2
159900     GO TO IF-WRITE-GF-73.                                        NC1034.2
160000 IF-DELETE-GF-73.                                                 NC1034.2
160100     PERFORM DE-LETE.                                             NC1034.2
160200 IF-WRITE-GF-73.                                                  NC1034.2
160300     MOVE "IF--TEST-GF-73 " TO PAR-NAME.                          NC1034.2
160400     PERFORM PRINT-DETAIL.                                        NC1034.2
160500 IF--INIT-GF-74.                                                  NC1034.2
160600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
160700     MOVE   "22" TO TWOS-XN-00002.                                NC1034.2
160800     MOVE    22  TO A02TWOS-DU-02V00.                             NC1034.2
160900 IF--TEST-GF-74.                                                  NC1034.2
161000     IF TWOS-XN-00002 GREATER THAN A02TWOS-DU-02V00               NC1034.2
161100         NEXT SENTENCE  ELSE                                      NC1034.2
161200         PERFORM PASS                                             NC1034.2
161300         GO TO IF-WRITE-GF-74.                                    NC1034.2
161400     MOVE TWOS-XN-00002 TO CORRECT-A.                             NC1034.2
161500     MOVE A02TWOS-DU-02V00 TO COMPUTED-A.                         NC1034.2
161600     PERFORM FAIL.                                                NC1034.2
161700     GO TO IF-WRITE-GF-74.                                        NC1034.2
161800 IF-DELETE-GF-74.                                                 NC1034.2
161900     PERFORM DE-LETE.                                             NC1034.2
162000 IF-WRITE-GF-74.                                                  NC1034.2
162100     MOVE "IF--TEST-GF-74 " TO PAR-NAME.                          NC1034.2
162200     PERFORM PRINT-DETAIL.                                        NC1034.2
162300 IF--INIT-GF-75.                                                  NC1034.2
162400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
162500     MOVE     "COMPARE--NOT EQUAL  " TO FEATURE.                  NC1034.2
162600     MOVE SPACE TO CORRECT-A.                                     NC1034.2
162700 IF--TEST-GF-75.                                                  NC1034.2
162800     IF ZERO IS NOT EQUAL TO CORRECT-A                            NC1034.2
162900         PERFORM PASS                                             NC1034.2
163000         GO TO IF-WRITE-GF-75.                                    NC1034.2
163100     MOVE ZERO TO COMPUTED-A.                                     NC1034.2
163200     PERFORM FAIL.                                                NC1034.2
163300     GO TO IF-WRITE-GF-75.                                        NC1034.2
163400 IF-DELETE-GF-75.                                                 NC1034.2
163500     PERFORM DE-LETE.                                             NC1034.2
163600 IF-WRITE-GF-75.                                                  NC1034.2
163700     MOVE "IF--TEST-75 " TO PAR-NAME.                             NC1034.2
163800     PERFORM PRINT-DETAIL.                                        NC1034.2
163900 IF--INIT-GF-76.                                                  NC1034.2
164000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
164100     MOVE   +022.00 TO A02TWOS-DS-03V02.                          NC1034.2
164200     MOVE    22  TO A02TWOS-DU-02V00.                             NC1034.2
164300 IF--TEST-GF-76.                                                  NC1034.2
164400     IF A02TWOS-DU-02V00 NOT EQUAL TO A02TWOS-DS-03V02            NC1034.2
164500         MOVE A02TWOS-DU-02V00 TO CORRECT-N                       NC1034.2
164600         MOVE A02TWOS-DS-03V02 TO COMPUTED-N                      NC1034.2
164700         PERFORM FAIL                                             NC1034.2
164800         GO TO IF-WRITE-GF-76  ELSE  NEXT SENTENCE.               NC1034.2
164900     PERFORM PASS                                                 NC1034.2
165000     GO TO IF-WRITE-GF-76.                                        NC1034.2
165100 IF-DELETE-GF-76.                                                 NC1034.2
165200     PERFORM DE-LETE.                                             NC1034.2
165300 IF-WRITE-GF-76.                                                  NC1034.2
165400     MOVE "IF--TEST-GF-76 " TO PAR-NAME.                          NC1034.2
165500     PERFORM PRINT-DETAIL.                                        NC1034.2
165600 IF--INIT-GF-77.                                                  NC1034.2
165700     MOVE     "COMPARE--NOT LESS   " TO FEATURE.                  NC1034.2
165800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
165900     MOVE   "22" TO TWOS-XN-00002.                                NC1034.2
166000     MOVE   "11" TO ONES-XN-00002.                                NC1034.2
166100 IF--TEST-GF-77.                                                  NC1034.2
166200     IF TWOS-XN-00002 NOT LESS THAN ONES-XN-00002                 NC1034.2
166300         PERFORM PASS                                             NC1034.2
166400         GO TO IF-WRITE-GF-77 ELSE   NEXT SENTENCE.               NC1034.2
166500     MOVE TWOS-XN-00002 TO CORRECT-A.                             NC1034.2
166600     MOVE ONES-XN-00002 TO COMPUTED-A.                            NC1034.2
166700     PERFORM FAIL.                                                NC1034.2
166800     GO TO IF-WRITE-GF-77.                                        NC1034.2
166900 IF-DELETE-GF-77.                                                 NC1034.2
167000     PERFORM DE-LETE.                                             NC1034.2
167100 IF-WRITE-GF-77.                                                  NC1034.2
167200     MOVE "IF--TEST-GF-77 " TO PAR-NAME.                          NC1034.2
167300     PERFORM PRINT-DETAIL.                                        NC1034.2
167400 IF--INIT-GF-78.                                                  NC1034.2
167500     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
167600     MOVE   .000000001 TO A01ONE-DS-P0801.                        NC1034.2
167700 IF--TEST-GF-78.                                                  NC1034.2
167800     IF 0.0000000001 IS NOT LESS THAN A01ONE-DS-P0801             NC1034.2
167900         MOVE "0.0000000001" TO CORRECT-A                         NC1034.2
168000         MOVE A01ONE-DS-P0801 TO COMPUTED-N                       NC1034.2
168100         PERFORM FAIL                                             NC1034.2
168200         GO TO IF-WRITE-GF-78  ELSE                               NC1034.2
168300     PERFORM PASS                                                 NC1034.2
168400     GO TO IF-WRITE-GF-78.                                        NC1034.2
168500 IF-DELETE-GF-78.                                                 NC1034.2
168600     PERFORM DE-LETE.                                             NC1034.2
168700 IF-WRITE-GF-78.                                                  NC1034.2
168800     MOVE "IF--TEST-GF-78 " TO PAR-NAME.                          NC1034.2
168900     PERFORM PRINT-DETAIL.                                        NC1034.2
169000 IF--INIT-GF-79.                                                  NC1034.2
169100     MOVE     "COMPARE--NOT GREATER" TO FEATURE.                  NC1034.2
169200     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
169300     MOVE  "11" TO ONES-XN-00002.                                 NC1034.2
169400     MOVE  "22" TO TWOS-XN-00002.                                 NC1034.2
169500 IF--TEST-GF-79.                                                  NC1034.2
169600     IF ONES-XN-00002 NOT GREATER THAN TWOS-XN-00002              NC1034.2
169700         PERFORM PASS                                             NC1034.2
169800         GO TO IF-WRITE-GF-79.                                    NC1034.2
169900     MOVE ONES-XN-00002 TO CORRECT-A.                             NC1034.2
170000     MOVE TWOS-XN-00002 TO COMPUTED-A.                            NC1034.2
170100     PERFORM FAIL.                                                NC1034.2
170200     GO TO IF-WRITE-GF-79.                                        NC1034.2
170300 IF-DELETE-GF-79.                                                 NC1034.2
170400     PERFORM DE-LETE.                                             NC1034.2
170500 IF-WRITE-GF-79.                                                  NC1034.2
170600     MOVE "IF--TEST-GF-79 " TO PAR-NAME.                          NC1034.2
170700     PERFORM PRINT-DETAIL.                                        NC1034.2
170800 IF--INIT-GF-80.                                                  NC1034.2
170900     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
171000     MOVE   +990 TO A990-DS-0201P.                                NC1034.2
171100     MOVE   99  TO A99-DS-02V00.                                  NC1034.2
171200 IF--TEST-GF-80.                                                  NC1034.2
171300     IF A990-DS-0201P NOT GREATER THAN A99-DS-02V00               NC1034.2
171400         MOVE A990-DS-0201P TO COMPUTED-N                         NC1034.2
171500         MOVE A99-DS-02V00 TO CORRECT-N                           NC1034.2
171600         PERFORM FAIL                                             NC1034.2
171700         GO TO IF-WRITE-GF-80.                                    NC1034.2
171800     PERFORM PASS.                                                NC1034.2
171900     GO TO IF-WRITE-GF-80.                                        NC1034.2
172000 IF-DELETE-GF-80.                                                 NC1034.2
172100     PERFORM DE-LETE.                                             NC1034.2
172200 IF-WRITE-GF-80.                                                  NC1034.2
172300     MOVE "IF--TEST-GF-80 " TO PAR-NAME.                          NC1034.2
172400     PERFORM PRINT-DETAIL.                                        NC1034.2
172500 IF--INIT-GF-81.                                                  NC1034.2
172600     MOVE     "COMPARE--GROUP VALUE" TO FEATURE.                  NC1034.2
172700     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
172800     MOVE  "*ASTERISK/SLASH" TO IF-D35.                           NC1034.2
172900 IF--TEST-GF-81.                                                  NC1034.2
173000     IF       IF-D36A EQUAL TO "*ASTER"                           NC1034.2
173100              PERFORM PASS GO TO IF--WRITE-GF-81.                 NC1034.2
173200     GO       TO IF--FAIL-GF-81.                                  NC1034.2
173300 IF--DELETE-GF-81.                                                NC1034.2
173400     PERFORM  DE-LETE.                                            NC1034.2
173500     GO       TO IF--WRITE-GF-81.                                 NC1034.2
173600 IF--FAIL-GF-81.                                                  NC1034.2
173700     PERFORM  FAIL.                                               NC1034.2
173800     MOVE     IF-D36A TO COMPUTED-A.                              NC1034.2
173900     MOVE     "*ASTER" TO CORRECT-A.                              NC1034.2
174000 IF--WRITE-GF-81.                                                 NC1034.2
174100     MOVE     "IF--TEST-GF-81" TO PAR-NAME.                       NC1034.2
174200     PERFORM  PRINT-DETAIL.                                       NC1034.2
174300 IF--INIT-GF-82.                                                  NC1034.2
174400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
174500     MOVE  "*ASTERISK/SLASH" TO IF-D35.                           NC1034.2
174600 IF--TEST-GF-82.                                                  NC1034.2
174700     IF       IF-D36B EQUAL TO "ISK"                              NC1034.2
174800              PERFORM PASS GO TO IF--WRITE-GF-82.                 NC1034.2
174900     GO       TO IF--FAIL-GF-82.                                  NC1034.2
175000 IF--DELETE-GF-82.                                                NC1034.2
175100     PERFORM  DE-LETE.                                            NC1034.2
175200     GO       TO IF--WRITE-GF-82.                                 NC1034.2
175300 IF--FAIL-GF-82.                                                  NC1034.2
175400     PERFORM  FAIL.                                               NC1034.2
175500     MOVE     IF-D36B TO COMPUTED-A.                              NC1034.2
175600     MOVE     "ISK" TO CORRECT-A.                                 NC1034.2
175700 IF--WRITE-GF-82.                                                 NC1034.2
175800     MOVE     "IF--TEST-GF-82" TO PAR-NAME.                       NC1034.2
175900     PERFORM  PRINT-DETAIL.                                       NC1034.2
176000 IF--INIT-GF-83.                                                  NC1034.2
176100     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
176200     MOVE  "*ASTERISK/SLASH" TO IF-D35.                           NC1034.2
176300 IF--TEST-GF-83.                                                  NC1034.2
176400     IF       IF-D36C EQUAL TO "/SLASH"                           NC1034.2
176500              PERFORM PASS GO TO IF--WRITE-GF-83.                 NC1034.2
176600     GO       TO IF--FAIL-GF-83.                                  NC1034.2
176700 IF--DELETE-GF-83.                                                NC1034.2
176800     PERFORM  DE-LETE.                                            NC1034.2
176900     GO       TO IF--WRITE-GF-83.                                 NC1034.2
177000 IF--FAIL-GF-83.                                                  NC1034.2
177100     PERFORM  FAIL.                                               NC1034.2
177200     MOVE     IF-D36C TO COMPUTED-A.                              NC1034.2
177300     MOVE     "/SLASH" TO CORRECT-A.                              NC1034.2
177400 IF--WRITE-GF-83.                                                 NC1034.2
177500     MOVE     "IF--TEST-GF-83" TO PAR-NAME.                       NC1034.2
177600     PERFORM  PRINT-DETAIL.                                       NC1034.2
177700 IF--INIT-GF-84.                                                  NC1034.2
177800     MOVE     "COMPARE--EQUAL" TO FEATURE.                        NC1034.2
177900     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
178000     MOVE   0001234 TO IF-D37.                                    NC1034.2
178100 IF--TEST-GF-84.                                                  NC1034.2
178200     IF       IF-D37  EQUAL TO 01234                              NC1034.2
178300              PERFORM PASS GO TO IF--WRITE-GF-84.                 NC1034.2
178400     GO       TO IF--FAIL-GF-84.                                  NC1034.2
178500 IF--DELETE-GF-84.                                                NC1034.2
178600     PERFORM  DE-LETE.                                            NC1034.2
178700     GO       TO IF--WRITE-GF-84.                                 NC1034.2
178800 IF--FAIL-GF-84.                                                  NC1034.2
178900     PERFORM  FAIL.                                               NC1034.2
179000     MOVE     IF-D37  TO COMPUTED-N.                              NC1034.2
179100     MOVE     01234 TO CORRECT-N.                                 NC1034.2
179200 IF--WRITE-GF-84.                                                 NC1034.2
179300     MOVE     "IF--TEST-GF-84" TO PAR-NAME.                       NC1034.2
179400     PERFORM  PRINT-DETAIL.                                       NC1034.2
179500 IF--INIT-GF-85.                                                  NC1034.2
179600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
179700     MOVE  " BABBAGE" TO IF-D38.                                  NC1034.2
179800 IF--TEST-GF-85.                                                  NC1034.2
179900     IF       IF-D38  EQUAL TO " BABBAGE            "             NC1034.2
180000              PERFORM PASS GO TO IF--WRITE-GF-85.                 NC1034.2
180100     GO       TO IF--FAIL-GF-85.                                  NC1034.2
180200 IF--DELETE-GF-85.                                                NC1034.2
180300     PERFORM  DE-LETE.                                            NC1034.2
180400     GO       TO IF--WRITE-GF-85.                                 NC1034.2
180500 IF--FAIL-GF-85.                                                  NC1034.2
180600     PERFORM  FAIL.                                               NC1034.2
180700     MOVE     IF-D38  TO COMPUTED-A.                              NC1034.2
180800     MOVE     " BABBAGE            " TO CORRECT-A.                NC1034.2
180900 IF--WRITE-GF-85.                                                 NC1034.2
181000     MOVE     "IF--TEST-GF-85" TO PAR-NAME.                       NC1034.2
181100     PERFORM  PRINT-DETAIL.                                       NC1034.2
181200 IF--INIT-GF-86.                                                  NC1034.2
181300     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
181400     MOVE                                                         NC1034.2
181500     XXXXX081                                                     NC1034.2
181600     TO NON-COBOL-CHARACTERS.                                     NC1034.2
181700 IF--TEST-GF-86.                                                  NC1034.2
181800     IF       NON-COBOL-CHARACTERS EQUAL TO                       NC1034.2
181900     XXXXX081                                                     NC1034.2
182000              PERFORM PASS GO TO IF--WRITE-GF-86.                 NC1034.2
182100     GO       TO IF--FAIL-GF-86.                                  NC1034.2
182200 IF--DELETE-GF-86.                                                NC1034.2
182300     PERFORM DE-LETE.                                             NC1034.2
182400     GO       TO IF--WRITE-GF-86.                                 NC1034.2
182500 IF--FAIL-GF-86.                                                  NC1034.2
182600     PERFORM  FAIL.                                               NC1034.2
182700     MOVE     NON-COBOL-CHARACTERS TO COMPUTED-A.                 NC1034.2
182800     MOVE                                                         NC1034.2
182900     XXXXX081                                                     NC1034.2
183000     TO CORRECT-A.                                                NC1034.2
183100 IF--WRITE-GF-86.                                                 NC1034.2
183200     MOVE     "IF--TEST-GF-86" TO PAR-NAME.                       NC1034.2
183300     MOVE     "NON COBOL CHARACTERS" TO RE-MARK.                  NC1034.2
183400     PERFORM  PRINT-DETAIL.                                       NC1034.2
183500 IF--INIT-GF-87.                                                  NC1034.2
183600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
183700     MOVE   100000 TO COMP-DATA2.                                 NC1034.2
183800     MOVE   100000 TO COMP-DATA9.                                 NC1034.2
183900 IF--TEST-GF-87.                                                  NC1034.2
184000     IF COMP-DATA2  EQUAL TO COMP-DATA9                           NC1034.2
184100         PERFORM PASS                                             NC1034.2
184200         GO TO IF--WRITE-GF-87.                                   NC1034.2
184300     MOVE COMP-DATA2 TO COMPUTED-18V0.                            NC1034.2
184400     MOVE COMP-DATA9 TO CORRECT-18V0.                             NC1034.2
184500     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
184600     PERFORM FAIL.                                                NC1034.2
184700     GO TO IF--WRITE-GF-87.                                       NC1034.2
184800 IF--DELETE-GF-87.                                                NC1034.2
184900     PERFORM DE-LETE.                                             NC1034.2
185000 IF--WRITE-GF-87.                                                 NC1034.2
185100     MOVE "IF--TEST-GF-87" TO PAR-NAME.                           NC1034.2
185200     PERFORM PRINT-DETAIL.                                        NC1034.2
185300 IF--INIT-GF-88.                                                  NC1034.2
185400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
185500     MOVE   300    TO COMP-DATA1.                                 NC1034.2
185600     MOVE   300.00 TO COMP-DATA7.                                 NC1034.2
185700 IF--TEST-GF-88.                                                  NC1034.2
185800     IF COMP-DATA1 EQUAL TO COMP-DATA7                            NC1034.2
185900         PERFORM PASS                                             NC1034.2
186000         GO TO IF--WRITE-GF-88.                                   NC1034.2
186100     MOVE COMP-DATA1 TO COMPUTED-18V0.                            NC1034.2
186200     MOVE COMP-DATA7 TO CORRECT-N.                                NC1034.2
186300     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
186400     PERFORM FAIL.                                                NC1034.2
186500     GO TO IF--WRITE-GF-88.                                       NC1034.2
186600 IF--DELETE-GF-88.                                                NC1034.2
186700     PERFORM DE-LETE.                                             NC1034.2
186800 IF--WRITE-GF-88.                                                 NC1034.2
186900     MOVE "IF--TEST-GF-88" TO PAR-NAME.                           NC1034.2
187000     PERFORM PRINT-DETAIL.                                        NC1034.2
187100 IF--INIT-GF-89.                                                  NC1034.2
187200     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
187300     MOVE   300 TO COMP-DATA1.                                    NC1034.2
187400     MOVE   300 TO DISP-DATA1.                                    NC1034.2
187500 IF--TEST-GF-89.                                                  NC1034.2
187600     IF COMP-DATA1 EQUAL TO DISP-DATA1                            NC1034.2
187700         PERFORM PASS                                             NC1034.2
187800         GO TO IF--WRITE-GF-89.                                   NC1034.2
187900     MOVE COMP-DATA1 TO COMPUTED-18V0.                            NC1034.2
188000     MOVE DISP-DATA1 TO CORRECT-18V0.                             NC1034.2
188100     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
188200     PERFORM FAIL.                                                NC1034.2
188300     GO TO IF--WRITE-GF-89.                                       NC1034.2
188400 IF--DELETE-GF-89.                                                NC1034.2
188500     PERFORM DE-LETE.                                             NC1034.2
188600 IF--WRITE-GF-89.                                                 NC1034.2
188700     MOVE "IF--TEST-GF-89" TO PAR-NAME.                           NC1034.2
188800     PERFORM PRINT-DETAIL.                                        NC1034.2
188900 IF--INIT-GF-90.                                                  NC1034.2
189000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
189100     MOVE   100000 TO COMP-DATA1.                                 NC1034.2
189200     MOVE   100000 TO DISP-DATA1.                                 NC1034.2
189300 IF--TEST-GF-90.                                                  NC1034.2
189400     IF COMP-DATA2 EQUAL TO DISP-DATA2                            NC1034.2
189500         PERFORM PASS                                             NC1034.2
189600         GO TO IF--WRITE-GF-90.                                   NC1034.2
189700     MOVE COMP-DATA2 TO COMPUTED-N.                               NC1034.2
189800     MOVE DISP-DATA2 TO CORRECT-N.                                NC1034.2
189900     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
190000     PERFORM FAIL.                                                NC1034.2
190100     GO TO IF--WRITE-GF-90.                                       NC1034.2
190200 IF--DELETE-GF-90.                                                NC1034.2
190300     PERFORM DE-LETE.                                             NC1034.2
190400 IF--WRITE-GF-90.                                                 NC1034.2
190500     MOVE "IF--TEST-GF-90" TO PAR-NAME.                           NC1034.2
190600     PERFORM PRINT-DETAIL.                                        NC1034.2
190700 IF--INIT-GF-91.                                                  NC1034.2
190800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
190900     MOVE   9 TO COMP-DATA3.                                      NC1034.2
191000     MOVE   9 TO DISP-DATA3.                                      NC1034.2
191100 IF--TEST-GF-91.                                                  NC1034.2
191200     IF COMP-DATA3  EQUAL TO DISP-DATA3                           NC1034.2
191300         PERFORM PASS                                             NC1034.2
191400         GO TO IF--WRITE-GF-91.                                   NC1034.2
191500     MOVE COMP-DATA3 TO COMPUTED-N.                               NC1034.2
191600     MOVE DISP-DATA3 TO CORRECT-N.                                NC1034.2
191700     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
191800     PERFORM FAIL.                                                NC1034.2
191900     GO TO IF--WRITE-GF-91.                                       NC1034.2
192000 IF--DELETE-GF-91.                                                NC1034.2
192100     PERFORM DE-LETE.                                             NC1034.2
192200 IF--WRITE-GF-91.                                                 NC1034.2
192300     MOVE "IF--TEST-GF-91" TO PAR-NAME.                           NC1034.2
192400     PERFORM PRINT-DETAIL.                                        NC1034.2
192500 IF--INIT-GF-92.                                                  NC1034.2
192600     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
192700     MOVE   300.00 TO COMP-DATA7.                                 NC1034.2
192800     MOVE   300    TO DISP-DATA1.                                 NC1034.2
192900 IF--TEST-GF-92.                                                  NC1034.2
193000     IF COMP-DATA7 EQUAL TO DISP-DATA1                            NC1034.2
193100         PERFORM PASS                                             NC1034.2
193200         GO TO IF--WRITE-GF-92.                                   NC1034.2
193300     MOVE COMP-DATA7 TO COMPUTED-N.                               NC1034.2
193400     MOVE DISP-DATA1 TO CORRECT-N.                                NC1034.2
193500     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
193600     PERFORM FAIL.                                                NC1034.2
193700     GO TO IF--WRITE-GF-92.                                       NC1034.2
193800 IF--DELETE-GF-92.                                                NC1034.2
193900     PERFORM DE-LETE.                                             NC1034.2
194000 IF--WRITE-GF-92.                                                 NC1034.2
194100     MOVE "IF--TEST-GF-92" TO PAR-NAME.                           NC1034.2
194200     PERFORM PRINT-DETAIL.                                        NC1034.2
194300 IF--INIT-GF-93.                                                  NC1034.2
194400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
194500     MOVE   3.3    TO COMP-DATA4.                                 NC1034.2
194600     MOVE   3.3000000 TO COMP-DATA8.                              NC1034.2
194700 IF--TEST-GF-93.                                                  NC1034.2
194800     IF COMP-DATA4 EQUAL TO COMP-DATA8                            NC1034.2
194900         PERFORM PASS                                             NC1034.2
195000         GO TO IF--WRITE-GF-93.                                   NC1034.2
195100     MOVE COMP-DATA4 TO COMPUTED-N.                               NC1034.2
195200     MOVE COMP-DATA8 TO CORRECT-N.                                NC1034.2
195300     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
195400     PERFORM FAIL.                                                NC1034.2
195500     GO TO IF--WRITE-GF-93.                                       NC1034.2
195600 IF--DELETE-GF-93.                                                NC1034.2
195700     PERFORM DE-LETE.                                             NC1034.2
195800 IF--WRITE-GF-93.                                                 NC1034.2
195900     MOVE "IF--TEST-GF-93" TO PAR-NAME.                           NC1034.2
196000     PERFORM PRINT-DETAIL.                                        NC1034.2
196100 IF--INIT-GF-94.                                                  NC1034.2
196200     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
196300     MOVE   300    TO COMP-DATA7.                                 NC1034.2
196400     MOVE   300    TO DISP-DATA1.                                 NC1034.2
196500 IF--TEST-GF-94.                                                  NC1034.2
196600     IF COMP-DATA7 EQUAL TO DISP-DATA1                            NC1034.2
196700         PERFORM PASS                                             NC1034.2
196800         GO TO IF--WRITE-GF-94.                                   NC1034.2
196900     MOVE COMP-DATA7 TO COMPUTED-N.                               NC1034.2
197000     MOVE DISP-DATA1 TO CORRECT-N.                                NC1034.2
197100     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
197200     PERFORM FAIL.                                                NC1034.2
197300     GO TO IF--WRITE-GF-94.                                       NC1034.2
197400 IF--DELETE-GF-94.                                                NC1034.2
197500     PERFORM DE-LETE.                                             NC1034.2
197600 IF--WRITE-GF-94.                                                 NC1034.2
197700     MOVE "IF--TEST-GF-94" TO PAR-NAME.                           NC1034.2
197800     PERFORM PRINT-DETAIL.                                        NC1034.2
197900 IF--INIT-GF-95.                                                  NC1034.2
198000     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
198100     MOVE   3.3000000 TO COMP-DATA8.                              NC1034.2
198200     MOVE   3.3       TO DISP-DATA4.                              NC1034.2
198300 IF--TEST-GF-95.                                                  NC1034.2
198400     IF DISP-DATA4 EQUAL TO COMP-DATA8                            NC1034.2
198500         PERFORM PASS                                             NC1034.2
198600         GO TO IF--WRITE-GF-95.                                   NC1034.2
198700     MOVE DISP-DATA4 TO COMPUTED-N.                               NC1034.2
198800     MOVE COMP-DATA8 TO CORRECT-N.                                NC1034.2
198900     MOVE "ENTRIES DIDNT COMPARE EQUAL" TO RE-MARK.               NC1034.2
199000     PERFORM FAIL.                                                NC1034.2
199100     GO TO IF--WRITE-GF-95.                                       NC1034.2
199200 IF--DELETE-GF-95.                                                NC1034.2
199300     PERFORM DE-LETE.                                             NC1034.2
199400 IF--WRITE-GF-95.                                                 NC1034.2
199500     MOVE "IF--TEST-GF-95" TO PAR-NAME.                           NC1034.2
199600     PERFORM PRINT-DETAIL.                                        NC1034.2
199700     MOVE "COMPARE GROUP-LEVEL " TO FEATURE.                      NC1034.2
199800 IF--INIT-GF-96.                                                  NC1034.2
199900     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
200000     MOVE   ZEROS     TO GROUP-1000-1.                            NC1034.2
200100     MOVE   QUOTES    TO XNAME.                                   NC1034.2
200200     MOVE   SPACES    TO GROUP-1000-2.                            NC1034.2
200300     MOVE   "."       TO GROUP-1000-3.                            NC1034.2
200400     MOVE   ZEROS     TO GROUP-X500-A.                            NC1034.2
200500     MOVE   QUOTES    TO GROUP-X500-1-1.                          NC1034.2
200600     MOVE   QUOTES    TO GROUP-X500-1-2.                          NC1034.2
200700     MOVE   SPACES    TO GROUP-X500-1-3.                          NC1034.2
200800     MOVE   " ."      TO GROUP-X500-1-4.                          NC1034.2
200900 IF--TEST-GF-96.                                                  NC1034.2
201000     IF GROUP-X1000 EQUAL TO GROUP-X500-2                         NC1034.2
201100         PERFORM PASS                                             NC1034.2
201200         GO TO IF--WRITE-GF-96.                                   NC1034.2
201300     MOVE "GROUP LEVEL X(1000) " TO COMPUTED-A.                   NC1034.2
201400     MOVE "GROUP LEVEL X(1000) " TO CORRECT-A.                    NC1034.2
201500     MOVE "FIELDS DIDNT COMPARE EQUAL" TO RE-MARK.                NC1034.2
201600     PERFORM FAIL.                                                NC1034.2
201700     GO TO IF--WRITE-GF-96.                                       NC1034.2
201800 IF--DELETE-GF-96.                                                NC1034.2
201900     PERFORM DE-LETE.                                             NC1034.2
202000 IF--WRITE-GF-96.                                                 NC1034.2
202100     MOVE "IF--TEST-GF-96" TO PAR-NAME.                           NC1034.2
202200     PERFORM PRINT-DETAIL.                                        NC1034.2
202300 IF--INIT-GF-97.                                                  NC1034.2
202400     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
202500     MOVE     22      TO A02TWOS-DU-02V00.                        NC1034.2
202600     MOVE     ZERO TO VAL.                                        NC1034.2
202700 IF--TEST-GF-97.                                                  NC1034.2
202800     IF       A02TWOS-DU-02V00 LESS THAN "AA"                     NC1034.2
202900              ADD 1 TO VAL.                                       NC1034.2
203000     IF       A02TWOS-DU-02V00 GREATER THAN "AA"                  NC1034.2
203100              ADD 1 TO VAL.                                       NC1034.2
203200     IF       VAL EQUAL TO 1                                      NC1034.2
203300              PERFORM PASS                                        NC1034.2
203400              GO TO IF--WRITE-GF-97.                              NC1034.2
203500     PERFORM  FAIL.                                               NC1034.2
203600     MOVE     VAL TO COMPUTED-N.                                  NC1034.2
203700     MOVE     1 TO CORRECT-N.                                     NC1034.2
203800     GO       TO IF--WRITE-GF-97.                                 NC1034.2
203900 IF--DELETE-GF-97.                                                NC1034.2
204000     PERFORM  DE-LETE.                                            NC1034.2
204100 IF--WRITE-GF-97.                                                 NC1034.2
204200     MOVE     "COMPARE NUM VS ALPH" TO FEATURE.                   NC1034.2
204300     MOVE     "IF--TEST-GF-97" TO PAR-NAME.                       NC1034.2
204400     PERFORM  PRINT-DETAIL.                                       NC1034.2
204500*                                                                 NC1034.2
204600*                                                                 NC1034.2
204700 IF--INIT-GF-98.                                                  NC1034.2
204800     MOVE  "V1-89 6.15.4 GR2" TO ANSI-REFERENCE.                  NC1034.2
204900     MOVE   -123456789012345678  TO WRK-DS-18V0-1.                NC1034.2
205000     MOVE   "123456789012345678" TO WRK-XN-18-2.                  NC1034.2
205100 IF-TEST-GF-98.                                                   NC1034.2
205200     IF WRK-DS-18V0-1 EQUAL WRK-XN-18-2 PERFORM PASS              NC1034.2
205300         ELSE PERFORM FAIL.                                       NC1034.2
205400     GO TO IF-WRITE-GF-98.                                        NC1034.2
205500 IF-DELETE-GF-98.                                                 NC1034.2
205600     PERFORM DE-LETE.                                             NC1034.2
205700 IF-WRITE-GF-98.                                                  NC1034.2
205800     MOVE "IF-TEST-GF-98" TO PAR-NAME.                            NC1034.2
205900     MOVE "EQUAL - NO TO" TO FEATURE.                             NC1034.2
206000     MOVE "PSEUDO-MOVE TO STRIP MINUS SIGN" TO RE-MARK.           NC1034.2
206100     PERFORM PRINT-DETAIL.                                        NC1034.2
206200*                                                                 NC1034.2
206300*                                                                 NC1034.2
206400 IF--INIT-GF-99.                                                  NC1034.2
206500*    ==--> OPTIONAL WORD "THEN" <--==                             NC1034.2
206600     MOVE  "COMPARE--EQUAL" TO FEATURE.                           NC1034.2
206700     MOVE  "V1-89 6.15.2  " TO ANSI-REFERENCE.                    NC1034.2
206800     MOVE   0 TO IF-D1.                                           NC1034.2
206900 IF--TEST-GF-99.                                                  NC1034.2
207000     IF      ZERO IS EQUAL TO IF-D1                               NC1034.2
207100        THEN PERFORM PASS                                         NC1034.2
207200     ELSE                                                         NC1034.2
207300             PERFORM FAIL.                                        NC1034.2
207400     GO TO   IF--WRITE-GF-99.                                     NC1034.2
207500 IF--DELETE-GF-99.                                                NC1034.2
207600     PERFORM DE-LETE.                                             NC1034.2
207700 IF--WRITE-GF-99.                                                 NC1034.2
207800     MOVE  "IF--TEST-GF-99" TO PAR-NAME.                          NC1034.2
207900     PERFORM PRINT-DETAIL.                                        NC1034.2
208000*                                                                 NC1034.2
208100*                                                                 NC1034.2
208200 IF--INIT-GF-100.                                                 NC1034.2
208300*    ==-->  EXPLICIT SCOPE TERMINATOR  <--==                      NC1034.2
208400     MOVE   "V1-89 6.4.3  " TO ANSI-REFERENCE.                    NC1034.2
208500     MOVE    ZERO TO WRK-DU-02V00.                                NC1034.2
208600     MOVE    ZERO TO IF-D2.                                       NC1034.2
208700 IF--TEST-GF-100-1.                                               NC1034.2
208800     IF      ZERO IS EQUAL TO IF-D2                               NC1034.2
208900             PERFORM PASS                                         NC1034.2
209000     ELSE                                                         NC1034.2
209100             PERFORM FAIL                                         NC1034.2
209200     END-IF                                                       NC1034.2
209300     MOVE    99 TO WRK-DU-02V00.                                  NC1034.2
209400     GO TO   IF--WRITE-GF-100-1.                                  NC1034.2
209500 IF--DELETE-GF-100-1.                                             NC1034.2
209600     PERFORM DE-LETE.                                             NC1034.2
209700 IF--WRITE-GF-100-1.                                              NC1034.2
209800     MOVE   "IF--TEST-GF-100-1" TO PAR-NAME.                      NC1034.2
209900     PERFORM PRINT-DETAIL.                                        NC1034.2
210000 IF--TEST-GF-100-2.                                               NC1034.2
210100     IF      WRK-DU-02V00 = 99                                    NC1034.2
210200             PERFORM PASS                                         NC1034.2
210300     ELSE                                                         NC1034.2
210400             MOVE    99           TO CORRECT-N                    NC1034.2
210500             MOVE    WRK-DU-02V00 TO COMPUTED-N                   NC1034.2
210600             PERFORM FAIL.                                        NC1034.2
210700     GO TO   IF--WRITE-GF-100-2.                                  NC1034.2
210800 IF--DELETE-GF-100-2.                                             NC1034.2
210900     PERFORM DE-LETE.                                             NC1034.2
211000 IF--WRITE-GF-100-2.                                              NC1034.2
211100     MOVE "IF--TEST-GF-100-2" TO PAR-NAME.                        NC1034.2
211200     PERFORM PRINT-DETAIL.                                        NC1034.2
211300*                                                                 NC1034.2
211400*                                                                 NC1034.2
211500 IF--INIT-GF-101.                                                 NC1034.2
211600     MOVE    " BABBAGE" TO IF-D38.                                NC1034.2
211700 IF--TEST-GF-101.                                                 NC1034.2
211800*    ==-->  EXPLICIT SCOPE TERMINATOR  <--==                      NC1034.2
211900     MOVE   "V1-89 6.4.3  " TO ANSI-REFERENCE.                    NC1034.2
212000     IF       IF-D38  EQUAL TO " BABBAGE            "             NC1034.2
212100              PERFORM PASS                                        NC1034.2
212200              GO TO   IF--WRITE-GF-101                            NC1034.2
212300     END-IF                                                       NC1034.2
212400     GO       TO IF--FAIL-GF-101.                                 NC1034.2
212500 IF--DELETE-GF-101.                                               NC1034.2
212600     PERFORM  DE-LETE.                                            NC1034.2
212700     GO       TO IF--WRITE-GF-101.                                NC1034.2
212800 IF--FAIL-GF-101.                                                 NC1034.2
212900     PERFORM  FAIL.                                               NC1034.2
213000     MOVE     IF-D38  TO COMPUTED-A.                              NC1034.2
213100     MOVE     " BABBAGE            " TO CORRECT-A.                NC1034.2
213200 IF--WRITE-GF-101.                                                NC1034.2
213300     MOVE     "IF--TEST-GF-101" TO PAR-NAME.                      NC1034.2
213400     PERFORM  PRINT-DETAIL.                                       NC1034.2
213500*                                                                 NC1034.2
213600*                                                                 NC1034.2
213700 CCVS-EXIT SECTION.                                               NC1034.2
213800 CCVS-999999.                                                     NC1034.2
213900     GO TO CLOSE-FILES.                                           NC1034.2
      *END-OF,NC103A                                                                  
