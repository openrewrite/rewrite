      *HEADER,COBOL,NC204M                                                            
000100 IDENTIFICATION DIVISION.                                         NC2044.2
000200 PROGRAM-ID.                                                      NC2044.2
000300     NC204M.                                                      NC2044.2
000400****************************************************************  NC2044.2
000500*                                                              *  NC2044.2
000600*    VALIDATION FOR:-                                          *  NC2044.2
000700*                                                              *  NC2044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2044.2
000900*                                                              *  NC2044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2044.2
001100*                                                              *  NC2044.2
001200****************************************************************  NC2044.2
001300*                                                              *  NC2044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2044.2
001500*                                                              *  NC2044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2044.2
001900*                                                              *  NC2044.2
002000****************************************************************  NC2044.2
002100*                                                              *  NC2044.2
002200*    PROGRAM NC204M TESTS FORMAT 1 OF THE ACCEPT STATEMENT AND *  NC2044.2
002300*    THE GENERAL FORMAT OF THE DISPLAY STATEMENT.              *  NC2044.2
002400*                                                              *  NC2044.2
002500*     X CARDS USED ARE:-                                       *  NC2044.2
002600*                                                              *  NC2044.2
002700*      X-55  - SYSTEM PRINTER NAME.                             * NC2044.2
002800*      X-56  - DISPLAY MNEMONIC NAME.                           * NC2044.2
002900*      X-57  - ACCEPT MNEMONIC NAME.                           *  NC2044.2
003000*      X-82  - SOURCE COMPUTER NAME.                            * NC2044.2
003100*      X-83  - OBJECT COMPUTER NAME.                            * NC2044.2
003200*                                                              *  NC2044.2
003300****************************************************************  NC2044.2
003400 ENVIRONMENT DIVISION.                                            NC2044.2
003500 CONFIGURATION SECTION.                                           NC2044.2
003600 SOURCE-COMPUTER.                                                 NC2044.2
003700     XXXXX082.                                                    NC2044.2
003800 OBJECT-COMPUTER.                                                 NC2044.2
003900     XXXXX083.                                                    NC2044.2
004000 SPECIAL-NAMES.                                                   NC2044.2
004100     XXXXX057                                                     NC2044.2
004200     IS ACCEPT-INPUT-DEVICE                                       NC2044.2
004300     XXXXX056                                                     NC2044.2
004400     IS DISPLAY-OUTPUT-DEVICE.                                    NC2044.2
004500 INPUT-OUTPUT SECTION.                                            NC2044.2
004600 FILE-CONTROL.                                                    NC2044.2
004700     SELECT PRINT-FILE ASSIGN TO                                  NC2044.2
004800     XXXXX055.                                                    NC2044.2
004900 DATA DIVISION.                                                   NC2044.2
005000 FILE SECTION.                                                    NC2044.2
005100 FD  PRINT-FILE.                                                  NC2044.2
005200 01  PRINT-REC PICTURE X(120).                                    NC2044.2
005300 01  DUMMY-RECORD PICTURE X(120).                                 NC2044.2
005400 WORKING-STORAGE SECTION.                                         NC2044.2
005500 77  SUB      PICTURE 9 USAGE COMPUTATIONAL VALUE 5.              NC2044.2
005600 01  ACCEPT-DATA.                                                 NC2044.2
005700     02 ACCEPT-D1.                                                NC2044.2
005800       03 ACCEPT-D1-A PICTURE X(20).                              NC2044.2
005900       03 ACCEPT-D1-B PICTURE X(7).                               NC2044.2
006000     02 ACCEPT-D2 PICTURE X(27) VALUE "ABCDEFGHIJKLMNOPQRSTUVWXY ZNC2044.2
006100-    "".                                                          NC2044.2
006200     02 ACCEPT-D3 PICTURE 9(10) USAGE DISPLAY.                    NC2044.2
006300     02 ACCEPT-D4 PICTURE 9(10) USAGE DISPLAY VALUE 0123456789.   NC2044.2
006400     02 ACCEPT-D5 PICTURE X(11) .                                 NC2044.2
006500     02 ACCEPT-D6 PICTURE X(11) VALUE "().+-*/l, =".              NC2044.2
006600     02 ACCEPT-D7 PICTURE X.                                      NC2044.2
006700     02 ACCEPT-D8 PICTURE X VALUE "9".                            NC2044.2
006800     02 ACCEPT-D9 PICTURE X.                                      NC2044.2
006900     02 ACCEPT-D10 PICTURE X VALUE "0".                           NC2044.2
007000     02 ACCEPT-D11 PICTURE A(20).                                 NC2044.2
007100     02 ACCEPT-D12 PICTURE A(20)                                  NC2044.2
007200              VALUE " ABC            XYZ ".                       NC2044.2
007300     02 ACCEPT-D13 PICTURE X(200).                                NC2044.2
007400     02 ACCEPT-D15 PICTURE XX.                                    NC2044.2
007500     02 ACCEPT-D16 PICTURE XX VALUE " 9".                         NC2044.2
007600     02 ACCEPT-D17.                                               NC2044.2
007700       03 QUAL-ACCEPT PICTURE X.                                  NC2044.2
007800     02 ACCEPT-D18 PICTURE X VALUE QUOTE.                         NC2044.2
007900     02 ACCEPT-D19.                                               NC2044.2
008000       03 QUAL-ACCEPT PICTURE X.                                  NC2044.2
008100     02 ACCEPT-D20 PICTURE X VALUE "Q".                           NC2044.2
008200     02 ACCEPT-VALUE21 PICTURE X(12) VALUE "............".        NC2044.2
008300     02 ACCEPT-D21 REDEFINES ACCEPT-VALUE21.                      NC2044.2
008400       03 TAB-ACCEPT OCCURS 3 TIMES.                              NC2044.2
008500         04 TAB-A PICTURE XXXX.                                   NC2044.2
008600     02 ACCEPT-D22 PICTURE X(12) VALUE "....ABCD....".            NC2044.2
008700     02 ACCEPT-D23.                                               NC2044.2
008800       03 TAB-A PICTURE XXXX OCCURS 5 TIMES.                      NC2044.2
008900     02 ACCEPT-D24 PICTURE X(20) VALUE "----------------ABCD".    NC2044.2
009000     02  ACCEPT-TEST-14-DATA              PIC X(15).              NC2044.2
009100     02  FILLER     REDEFINES             ACCEPT-TEST-14-DATA.    NC2044.2
009200       03  ACC-14-CHARS-1-10              PIC X(10).              NC2044.2
009300     02  FILLER     REDEFINES             ACCEPT-TEST-14-DATA.    NC2044.2
009400       03  ACC-14-CHARS-11-15             PIC X(5).               NC2044.2
009500                                                                  NC2044.2
009600 01  GRP-CONSTANTS.                                               NC2044.2
009700         04 GRP-ALPHABETIC.                                       NC2044.2
009800              05 ALPHABET-AN-00026 PICTURE A(26)                  NC2044.2
009900                  VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ".             NC2044.2
010000         04 GRP-NUMERIC.                                          NC2044.2
010100              05 DIGITS-DV-10V00   PICTURE 9(10) VALUE 0123456789.NC2044.2
010200              05 DIGITS-DU-06V04-S REDEFINES DIGITS-DV-10V00      NC2044.2
010300                                   PICTURE 9(6)V9999.             NC2044.2
010400         04 GRP-ALPHANUMERIC.                                     NC2044.2
010500              05 ALPHANUMERIC-XN-00049 PICTURE X(50)              NC2044.2
010600     VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-<>=l,;.()/* 0123456789".  NC2044.2
010700              05 FILLER            PICTURE X VALUE QUOTE.         NC2044.2
010800 01  ACCEPT-RESULTS.                                              NC2044.2
010900     02  FILLER                   PICTURE X(80)    VALUE          NC2044.2
011000     "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z  0123456NC2044.2
011100-    "789                 ".                                      NC2044.2
011200 01  80X-CHARACTER-FIELD.                                         NC2044.2
011300     02 FILLER PICTURE X(80).                                     NC2044.2
011400 01  DISPLAY-DATA.                                                NC2044.2
011500     02 DISPLAY-A.                                                NC2044.2
011600     03 DISPLAY-A1 PICTURE A VALUE "A".                           NC2044.2
011700     03 DISPLAY-A2.                                               NC2044.2
011800     04 DISPLAY-A2A PICTURE A VALUE "L".                          NC2044.2
011900     04 DISPLAY-A3.                                               NC2044.2
012000     05 DISPLAY-A3A PICTURE A VALUE "P".                          NC2044.2
012100     05 DISPLAY-A4.                                               NC2044.2
012200     06 DISPLAY-A4A PICTURE A VALUE "H".                          NC2044.2
012300     06 DISPLAY-A5.                                               NC2044.2
012400     07 DISPLAY-A5A PICTURE A VALUE "A".                          NC2044.2
012500     07 DISPLAY-A6.                                               NC2044.2
012600     08 DISPLAY-A6A PICTURE A VALUE "B".                          NC2044.2
012700     08 DISPLAY-A7.                                               NC2044.2
012800     09 DISPLAY-A7A PICTURE A VALUE "E".                          NC2044.2
012900     09 DISPLAY-A8.                                               NC2044.2
013000     10 DISPLAY-A8A PICTURE AAA VALUE "TIC".                      NC2044.2
013100     02 DISPLAY-N PICTURE 9(10) VALUE 0123456789.                 NC2044.2
013200     02 DISPLAY-X PICTURE X(10) VALUE "A1B2C3D4E5".               NC2044.2
013300     02 DISPLAY-B PICTURE X(13).                                  NC2044.2
013400     02 DISPLAY-C REDEFINES DISPLAY-B.                            NC2044.2
013500     03 DISPLAY-D PICTURE X(8).                                   NC2044.2
013600     03 DISPLAY-E PICTURE X(5).                                   NC2044.2
013700     02 DISPLAY-F.                                                NC2044.2
013800     03 DISPLAY-G PICTURE X(100) VALUE IS "D001*002*003*004*005*00NC2044.2
013900-    "6*007*008*009*010*011*012*013*014*015*016*017*018*019*020D02NC2044.2
014000-    "1*022*023*024*025".                                         NC2044.2
014100     03 DISPLAY-H PICTURE IS X(100) VALUE IS "*026*027*028*029*030NC2044.2
014200-    "*031*032*033*034*035*036*037*038*039*040D041*042*043*044*045NC2044.2
014300-    "*046*047*048*049*050".                                      NC2044.2
014400     02 SEE-ABOVE       PICTURE X(9) VALUE  "SEE ABOVE".          NC2044.2
014500     02 SEE-BELOW       PICTURE X(9) VALUE  "SEE BELOW".          NC2044.2
014600     02 CORRECT-FOLLOWS           PICTURE X(20)                   NC2044.2
014700                                  VALUE "CORRECT DATA FOLLOWS".   NC2044.2
014800     02 END-CORRECT     PICTURE X(16) VALUE "END CORRECT DATA".   NC2044.2
014900     02 DISPLAY-WRITER.                                           NC2044.2
015000       03 DIS-PLAYER    PICTURE X(119).                           NC2044.2
015100     02 DISPLAY-SWITCH  PICTURE 9 VALUE ZERO.                     NC2044.2
015200     02 ZERO-SPACE-QUOTE.                                         NC2044.2
015300       03 FILLER PICTURE X VALUE "0".                             NC2044.2
015400       03 FILLER PICTURE X VALUE SPACE.                           NC2044.2
015500       03 FILLER PICTURE X VALUE QUOTE.                           NC2044.2
015600     02 QUAL-TAB-VALUE            PICTURE X(21)                   NC2044.2
015700                                  VALUE "ABCDEFGHIJKLMNOPQRSTU".  NC2044.2
015800     02 NO-QUAL-TAB-RECORD REDEFINES QUAL-TAB-VALUE.              NC2044.2
015900       03 X1  PICTURE X.                                          NC2044.2
016000       03 X2  PICTURE X.                                          NC2044.2
016100       03 X3  PICTURE X.                                          NC2044.2
016200       03 X4  PICTURE X.                                          NC2044.2
016300       03 X5  PICTURE X.                                          NC2044.2
016400       03 X6  PICTURE X.                                          NC2044.2
016500       03 X7  PICTURE X.                                          NC2044.2
016600       03 X8  PICTURE X.                                          NC2044.2
016700       03 X9  PICTURE X.                                          NC2044.2
016800       03 X10 PICTURE X.                                          NC2044.2
016900       03 X11 PICTURE X.                                          NC2044.2
017000       03 X12 PICTURE X.                                          NC2044.2
017100       03 X13 PICTURE X.                                          NC2044.2
017200       03 X14 PICTURE X.                                          NC2044.2
017300       03 X15 PICTURE X.                                          NC2044.2
017400       03 X16 PICTURE X.                                          NC2044.2
017500       03 X17 PICTURE X.                                          NC2044.2
017600       03 X18 PICTURE X.                                          NC2044.2
017700       03 X19 PICTURE X.                                          NC2044.2
017800       03 X20 PICTURE X.                                          NC2044.2
017900       03 X21 PICTURE X.                                          NC2044.2
018000     02 QUAL-TAB-RECORD REDEFINES QUAL-TAB-VALUE.                 NC2044.2
018100       03 XTAB                    PICTURE X OCCURS 9 TIMES.       NC2044.2
018200       03 GRP-1.                                                  NC2044.2
018300         04 ELEM-1                PICTURE X.                      NC2044.2
018400         04 ELEM-2                PICTURE X.                      NC2044.2
018500         04 ELEM-3                PICTURE X.                      NC2044.2
018600         04 SUB-TAB               PICTURE X OCCURS 3 TIMES.       NC2044.2
018700       03 GRP-2.                                                  NC2044.2
018800         04 ELEM-1                PICTURE X.                      NC2044.2
018900         04 ELEM-2                PICTURE X.                      NC2044.2
019000         04 ELEM-3                PICTURE X.                      NC2044.2
019100         04 SUB-TAB               PICTURE X OCCURS 3 TIMES.       NC2044.2
019200     02 DISPLAY-MIXTURE.                                          NC2044.2
019300       03 FILLER                  PICTURE X(6) VALUE "QUOTE ".    NC2044.2
019400       03 FILLER                  PICTURE X VALUE QUOTE.          NC2044.2
019500       03 FILLER                  PICTURE X(36) VALUE             NC2044.2
019600              " ASTERISK * NUMERIC LITERALS 21 1325".             NC2044.2
019700       03 I-DATA                  PICTURE X(17)                   NC2044.2
019800                                  VALUE " IDENTIFIER DATA ".      NC2044.2
019900       03 TA-VALUE                PICTURE X(20)                   NC2044.2
020000                                  VALUE "A B C D E 1 2 3 4 5 ".   NC2044.2
020100       03 TA-BLE REDEFINES TA-VALUE.                              NC2044.2
020200         04 ROW OCCURS 2 TIMES.                                   NC2044.2
020300           05 PIECE               PICTURE XX OCCURS 5 TIMES.      NC2044.2
020400       03 TRUE-PAIR.                                              NC2044.2
020500         04 A1                    PICTURE X(20)                   NC2044.2
020600                                  VALUE    "(TOTAL 21 OPERANDS) ".NC2044.2
020700         04 A2                    PICTURE X(11)                   NC2044.2
020800                                  VALUE    "END OF DATA".         NC2044.2
020900     02   FALSE-PAIR.                                             NC2044.2
021000         04 A1                    PICTURE X(20)                   NC2044.2
021100                                  VALUE    "(SOME BAD OPERANDS) ".NC2044.2
021200         04 A2                    PICTURE X(11)                   NC2044.2
021300                                  VALUE    "ERROR  DATA".         NC2044.2
021400 01  CHARACTER-BREAKDOWN-S.                                       NC2044.2
021500     02   FIRST-20S PICTURE X(20).                                NC2044.2
021600     02  SECOND-20S PICTURE X(20).                                NC2044.2
021700     02   THIRD-20S PICTURE X(20).                                NC2044.2
021800     02  FOURTH-20S PICTURE X(20).                                NC2044.2
021900     02   FIFTH-20S PICTURE X(20).                                NC2044.2
022000     02   SIXTH-20S PICTURE X(20).                                NC2044.2
022100     02 SEVENTH-20S PICTURE X(20).                                NC2044.2
022200     02  EIGHTH-20S PICTURE X(20).                                NC2044.2
022300     02   NINTH-20S PICTURE X(20).                                NC2044.2
022400     02   TENTH-20S PICTURE X(20).                                NC2044.2
022500 01  CHARACTER-BREAKDOWN-R.                                       NC2044.2
022600     02   FIRST-20R PICTURE X(20).                                NC2044.2
022700     02  SECOND-20R PICTURE X(20).                                NC2044.2
022800     02   THIRD-20R PICTURE X(20).                                NC2044.2
022900     02  FOURTH-20R PICTURE X(20).                                NC2044.2
023000     02   FIFTH-20R PICTURE X(20).                                NC2044.2
023100     02   SIXTH-20R PICTURE X(20).                                NC2044.2
023200     02 SEVENTH-20R PICTURE X(20).                                NC2044.2
023300     02  EIGHTH-20R PICTURE X(20).                                NC2044.2
023400     02   NINTH-20R PICTURE X(20).                                NC2044.2
023500     02   TENTH-20R PICTURE X(20).                                NC2044.2
023600 01  TEST-RESULTS.                                                NC2044.2
023700     02 FILLER                   PIC X      VALUE SPACE.          NC2044.2
023800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2044.2
023900     02 FILLER                   PIC X      VALUE SPACE.          NC2044.2
024000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2044.2
024100     02 FILLER                   PIC X      VALUE SPACE.          NC2044.2
024200     02  PAR-NAME.                                                NC2044.2
024300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2044.2
024400       03  PARDOT-X              PIC X      VALUE SPACE.          NC2044.2
024500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2044.2
024600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2044.2
024700     02 RE-MARK                  PIC X(61).                       NC2044.2
024800 01  TEST-COMPUTED.                                               NC2044.2
024900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2044.2
025000     02 FILLER                   PIC X(17)  VALUE                 NC2044.2
025100            "       COMPUTED=".                                   NC2044.2
025200     02 COMPUTED-X.                                               NC2044.2
025300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2044.2
025400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2044.2
025500                                 PIC -9(9).9(9).                  NC2044.2
025600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2044.2
025700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2044.2
025800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2044.2
025900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2044.2
026000         04 COMPUTED-18V0                    PIC -9(18).          NC2044.2
026100         04 FILLER                           PIC X.               NC2044.2
026200     03 FILLER PIC X(50) VALUE SPACE.                             NC2044.2
026300 01  TEST-CORRECT.                                                NC2044.2
026400     02 FILLER PIC X(30) VALUE SPACE.                             NC2044.2
026500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2044.2
026600     02 CORRECT-X.                                                NC2044.2
026700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2044.2
026800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2044.2
026900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2044.2
027000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2044.2
027100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2044.2
027200     03      CR-18V0 REDEFINES CORRECT-A.                         NC2044.2
027300         04 CORRECT-18V0                     PIC -9(18).          NC2044.2
027400         04 FILLER                           PIC X.               NC2044.2
027500     03 FILLER PIC X(2) VALUE SPACE.                              NC2044.2
027600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2044.2
027700 01  CCVS-C-1.                                                    NC2044.2
027800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2044.2
027900-    "SS  PARAGRAPH-NAME                                          NC2044.2
028000-    "       REMARKS".                                            NC2044.2
028100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2044.2
028200 01  CCVS-C-2.                                                    NC2044.2
028300     02 FILLER                     PIC X        VALUE SPACE.      NC2044.2
028400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2044.2
028500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2044.2
028600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2044.2
028700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2044.2
028800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2044.2
028900 01  REC-CT                        PIC 99       VALUE ZERO.       NC2044.2
029000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2044.2
029100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2044.2
029200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2044.2
029300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2044.2
029400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2044.2
029500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2044.2
029600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2044.2
029700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2044.2
029800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2044.2
029900 01  CCVS-H-1.                                                    NC2044.2
030000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2044.2
030100     02  FILLER                    PIC X(42)    VALUE             NC2044.2
030200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2044.2
030300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2044.2
030400 01  CCVS-H-2A.                                                   NC2044.2
030500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2044.2
030600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2044.2
030700   02  FILLER                        PIC XXXX   VALUE             NC2044.2
030800     "4.2 ".                                                      NC2044.2
030900   02  FILLER                        PIC X(28)  VALUE             NC2044.2
031000            " COPY - NOT FOR DISTRIBUTION".                       NC2044.2
031100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2044.2
031200                                                                  NC2044.2
031300 01  CCVS-H-2B.                                                   NC2044.2
031400   02  FILLER                        PIC X(15)  VALUE             NC2044.2
031500            "TEST RESULT OF ".                                    NC2044.2
031600   02  TEST-ID                       PIC X(9).                    NC2044.2
031700   02  FILLER                        PIC X(4)   VALUE             NC2044.2
031800            " IN ".                                               NC2044.2
031900   02  FILLER                        PIC X(12)  VALUE             NC2044.2
032000     " HIGH       ".                                              NC2044.2
032100   02  FILLER                        PIC X(22)  VALUE             NC2044.2
032200            " LEVEL VALIDATION FOR ".                             NC2044.2
032300   02  FILLER                        PIC X(58)  VALUE             NC2044.2
032400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2044.2
032500 01  CCVS-H-3.                                                    NC2044.2
032600     02  FILLER                      PIC X(34)  VALUE             NC2044.2
032700            " FOR OFFICIAL USE ONLY    ".                         NC2044.2
032800     02  FILLER                      PIC X(58)  VALUE             NC2044.2
032900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2044.2
033000     02  FILLER                      PIC X(28)  VALUE             NC2044.2
033100            "  COPYRIGHT   1985 ".                                NC2044.2
033200 01  CCVS-E-1.                                                    NC2044.2
033300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2044.2
033400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2044.2
033500     02 ID-AGAIN                     PIC X(9).                    NC2044.2
033600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2044.2
033700 01  CCVS-E-2.                                                    NC2044.2
033800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2044.2
033900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2044.2
034000     02 CCVS-E-2-2.                                               NC2044.2
034100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2044.2
034200         03 FILLER                   PIC X      VALUE SPACE.      NC2044.2
034300         03 ENDER-DESC               PIC X(44)  VALUE             NC2044.2
034400            "ERRORS ENCOUNTERED".                                 NC2044.2
034500 01  CCVS-E-3.                                                    NC2044.2
034600     02  FILLER                      PIC X(22)  VALUE             NC2044.2
034700            " FOR OFFICIAL USE ONLY".                             NC2044.2
034800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2044.2
034900     02  FILLER                      PIC X(58)  VALUE             NC2044.2
035000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2044.2
035100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2044.2
035200     02 FILLER                       PIC X(15)  VALUE             NC2044.2
035300             " COPYRIGHT 1985".                                   NC2044.2
035400 01  CCVS-E-4.                                                    NC2044.2
035500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2044.2
035600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2044.2
035700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2044.2
035800     02 FILLER                       PIC X(40)  VALUE             NC2044.2
035900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2044.2
036000 01  XXINFO.                                                      NC2044.2
036100     02 FILLER                       PIC X(19)  VALUE             NC2044.2
036200            "*** INFORMATION ***".                                NC2044.2
036300     02 INFO-TEXT.                                                NC2044.2
036400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2044.2
036500       04 XXCOMPUTED                 PIC X(20).                   NC2044.2
036600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2044.2
036700       04 XXCORRECT                  PIC X(20).                   NC2044.2
036800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2044.2
036900 01  HYPHEN-LINE.                                                 NC2044.2
037000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2044.2
037100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2044.2
037200-    "*****************************************".                 NC2044.2
037300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2044.2
037400-    "******************************".                            NC2044.2
037500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2044.2
037600     "NC204M".                                                    NC2044.2
037700 PROCEDURE DIVISION.                                              NC2044.2
037800 CCVS1 SECTION.                                                   NC2044.2
037900 OPEN-FILES.                                                      NC2044.2
038000     OPEN     OUTPUT PRINT-FILE.                                  NC2044.2
038100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2044.2
038200     MOVE    SPACE TO TEST-RESULTS.                               NC2044.2
038300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2044.2
038400     GO TO CCVS1-EXIT.                                            NC2044.2
038500 CLOSE-FILES.                                                     NC2044.2
038600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2044.2
038700 TERMINATE-CCVS.                                                  NC2044.2
038800*    EXIT PROGRAM.                                                NC2044.2
038900*TERMINATE-CALL.                                                  NC2044.2
039000     STOP     RUN.                                                NC2044.2
039100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2044.2
039200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2044.2
039300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2044.2
039400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2044.2
039500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2044.2
039600 PRINT-DETAIL.                                                    NC2044.2
039700     IF REC-CT NOT EQUAL TO ZERO                                  NC2044.2
039800             MOVE "." TO PARDOT-X                                 NC2044.2
039900             MOVE REC-CT TO DOTVALUE.                             NC2044.2
040000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2044.2
040100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2044.2
040200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2044.2
040300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2044.2
040400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2044.2
040500     MOVE SPACE TO CORRECT-X.                                     NC2044.2
040600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2044.2
040700     MOVE     SPACE TO RE-MARK.                                   NC2044.2
040800 HEAD-ROUTINE.                                                    NC2044.2
040900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2044.2
041000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2044.2
041100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2044.2
041200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2044.2
041300 COLUMN-NAMES-ROUTINE.                                            NC2044.2
041400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2044.2
041500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2044.2
041600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2044.2
041700 END-ROUTINE.                                                     NC2044.2
041800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2044.2
041900 END-RTN-EXIT.                                                    NC2044.2
042000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2044.2
042100 END-ROUTINE-1.                                                   NC2044.2
042200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2044.2
042300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2044.2
042400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2044.2
042500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2044.2
042600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2044.2
042700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2044.2
042800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2044.2
042900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2044.2
043000  END-ROUTINE-12.                                                 NC2044.2
043100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2044.2
043200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2044.2
043300         MOVE "NO " TO ERROR-TOTAL                                NC2044.2
043400         ELSE                                                     NC2044.2
043500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2044.2
043600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2044.2
043700     PERFORM WRITE-LINE.                                          NC2044.2
043800 END-ROUTINE-13.                                                  NC2044.2
043900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2044.2
044000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2044.2
044100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2044.2
044200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2044.2
044300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2044.2
044400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2044.2
044500          MOVE "NO " TO ERROR-TOTAL                               NC2044.2
044600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2044.2
044700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2044.2
044800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2044.2
044900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2044.2
045000 WRITE-LINE.                                                      NC2044.2
045100     ADD 1 TO RECORD-COUNT.                                       NC2044.2
045200*    IF RECORD-COUNT GREATER 50                                   NC2044.2
045300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2044.2
045400*        MOVE SPACE TO DUMMY-RECORD                               NC2044.2
045500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2044.2
045600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2044.2
045700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2044.2
045800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2044.2
045900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2044.2
046000*        MOVE ZERO TO RECORD-COUNT.                               NC2044.2
046100     PERFORM WRT-LN.                                              NC2044.2
046200 WRT-LN.                                                          NC2044.2
046300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2044.2
046400     MOVE SPACE TO DUMMY-RECORD.                                  NC2044.2
046500 BLANK-LINE-PRINT.                                                NC2044.2
046600     PERFORM WRT-LN.                                              NC2044.2
046700 FAIL-ROUTINE.                                                    NC2044.2
046800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2044.2
046900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2044.2
047000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2044.2
047100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2044.2
047200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2044.2
047300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2044.2
047400     GO TO  FAIL-ROUTINE-EX.                                      NC2044.2
047500 FAIL-ROUTINE-WRITE.                                              NC2044.2
047600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2044.2
047700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2044.2
047800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2044.2
047900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2044.2
048000 FAIL-ROUTINE-EX. EXIT.                                           NC2044.2
048100 BAIL-OUT.                                                        NC2044.2
048200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2044.2
048300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2044.2
048400 BAIL-OUT-WRITE.                                                  NC2044.2
048500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2044.2
048600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2044.2
048700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2044.2
048800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2044.2
048900 BAIL-OUT-EX. EXIT.                                               NC2044.2
049000 CCVS1-EXIT.                                                      NC2044.2
049100     EXIT.                                                        NC2044.2
049200 SECT-NC204M-001 SECTION.                                         NC2044.2
049300 DIS-INIT-GF.                                                     NC2044.2
049400     MOVE   "VI-78 6.10" TO ANSI-REFERENCE.                       NC2044.2
049500     MOVE    SPACE TO FEATURE.                                    NC2044.2
049600     PERFORM BLANK-LINE-PRINT.                                    NC2044.2
049700     MOVE   "SEE NOTE IN DIS-INIT-GF." TO RE-MARK.                NC2044.2
049800     PERFORM PRINT-DETAIL.                                        NC2044.2
049900     PERFORM BLANK-LINE-PRINT.                                    NC2044.2
050000     MOVE   "DISPLAY UPON" TO FEATURE.                            NC2044.2
050100*        NOTE FOR THE SAKE OF CONVENIENCE IN READING THE OUTPUT,  NC2044.2
050200*             THE DISPLAY TESTS ARE CONSTRUCTED ON THE ASSUMPTION NC2044.2
050300*             THAT THE DISPLAYED OUTPUT WILL BE PRINTED ALONG     NC2044.2
050400*             WITH THE OUTPUT FROM THE WRITE STATEMENTS. NOTE ,   NC2044.2
050500*             HOWEVER, IT IS NOT CONSIDERED NONSTANDARD IF THE    NC2044.2
050600*             DISPLAYED OUTPUT APPEARS ELSEWHERE IN THE LISTING.  NC2044.2
050700*                                                                 NC2044.2
050800 DIS-INIT-GF-1.                                                   NC2044.2
050900     MOVE     "DIS-TEST-GF-1 " TO PAR-NAME.                       NC2044.2
051000     MOVE     "ALPHABETIC"     TO DISPLAY-A.                      NC2044.2
051100 DIS-TEST-GF-1.                                                   NC2044.2
051200     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
051300     DISPLAY DISPLAY-A UPON DISPLAY-OUTPUT-DEVICE.                NC2044.2
051400     MOVE     DISPLAY-A TO DIS-PLAYER.                            NC2044.2
051500     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
051600     GO       TO DIS-WRITE-GF-1.                                  NC2044.2
051700 DIS-DELETE-GF-1.                                                 NC2044.2
051800     PERFORM  DE-LETE.                                            NC2044.2
051900 DIS-WRITE-GF-1.                                                  NC2044.2
052000     MOVE     "DIS-TEST-GF-1 " TO PAR-NAME.                       NC2044.2
052100     PERFORM  PRINT-DETAIL.                                       NC2044.2
052200*                                                                 NC2044.2
052300 DIS-INIT-GF-2.                                                   NC2044.2
052400     MOVE     "DIS-TEST-GF-2 " TO PAR-NAME.                       NC2044.2
052500 DIS-TEST-GF-2.                                                   NC2044.2
052600     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
052700     DISPLAY "ALPHABETIC LITERAL" UPON DISPLAY-OUTPUT-DEVICE.     NC2044.2
052800     MOVE     "ALPHABETIC LITERAL" TO DIS-PLAYER.                 NC2044.2
052900     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
053000     GO       TO DIS-WRITE-GF-2.                                  NC2044.2
053100 DIS-DELETE-GF-2.                                                 NC2044.2
053200     PERFORM  DE-LETE.                                            NC2044.2
053300 DIS-WRITE-GF-2.                                                  NC2044.2
053400     MOVE     "DIS-TEST-GF-2 " TO PAR-NAME.                       NC2044.2
053500     PERFORM  PRINT-DETAIL.                                       NC2044.2
053600*                                                                 NC2044.2
053700 DIS-INIT-GF-3.                                                   NC2044.2
053800     MOVE     "DIS-TEST-GF-3 " TO PAR-NAME.                       NC2044.2
053900     MOVE     0123456789       TO DISPLAY-N.                      NC2044.2
054000 DIS-TEST-GF-3.                                                   NC2044.2
054100     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
054200     DISPLAY  DISPLAY-N UPON DISPLAY-OUTPUT-DEVICE.               NC2044.2
054300     MOVE     DISPLAY-N TO DIS-PLAYER.                            NC2044.2
054400     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
054500     GO       TO DIS-WRITE-GF-3.                                  NC2044.2
054600 DIS-DELETE-GF-3.                                                 NC2044.2
054700     PERFORM  DE-LETE.                                            NC2044.2
054800 DIS-WRITE-GF-3.                                                  NC2044.2
054900     MOVE     "DIS-TEST-GF-3 " TO PAR-NAME.                       NC2044.2
055000     PERFORM  PRINT-DETAIL.                                       NC2044.2
055100*                                                                 NC2044.2
055200 DIS-INIT-GF-4.                                                   NC2044.2
055300     MOVE     "DIS-TEST-GF-4 " TO PAR-NAME.                       NC2044.2
055400 DIS-TEST-GF-4.                                                   NC2044.2
055500     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
055600     DISPLAY 9876543210 UPON DISPLAY-OUTPUT-DEVICE.               NC2044.2
055700     MOVE     "9876543210" TO DIS-PLAYER.                         NC2044.2
055800     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
055900     GO       TO DIS-WRITE-GF-4.                                  NC2044.2
056000 DIS-DELETE-GF-4.                                                 NC2044.2
056100     PERFORM  DE-LETE.                                            NC2044.2
056200 DIS-WRITE-GF-4.                                                  NC2044.2
056300     MOVE     "DIS-TEST-GF-4 " TO PAR-NAME.                       NC2044.2
056400     PERFORM  PRINT-DETAIL.                                       NC2044.2
056500*                                                                 NC2044.2
056600 DIS-INIT-GF-5.                                                   NC2044.2
056700     MOVE     "DIS-TEST-GF-5 " TO PAR-NAME.                       NC2044.2
056800     MOVE     "A1B2C3D4E5"     TO DISPLAY-X.                      NC2044.2
056900 DIS-TEST-GF-5.                                                   NC2044.2
057000     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
057100     DISPLAY DISPLAY-X UPON DISPLAY-OUTPUT-DEVICE.                NC2044.2
057200     MOVE     DISPLAY-X TO DIS-PLAYER.                            NC2044.2
057300     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
057400     GO       TO DIS-WRITE-GF-5.                                  NC2044.2
057500 DIS-DELETE-GF-5.                                                 NC2044.2
057600     PERFORM  DE-LETE.                                            NC2044.2
057700 DIS-WRITE-GF-5.                                                  NC2044.2
057800     MOVE     "DIS-TEST-GF-5 " TO PAR-NAME.                       NC2044.2
057900     PERFORM  PRINT-DETAIL.                                       NC2044.2
058000*                                                                 NC2044.2
058100 DIS-INIT-GF-6.                                                   NC2044.2
058200     MOVE     "DIS-TEST-GF-6 " TO PAR-NAME.                       NC2044.2
058300 DIS-TEST-GF-6.                                                   NC2044.2
058400     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
058500     DISPLAY "A1B2C3D4E5 ALPHANUMERIC LITERAL" UPON               NC2044.2
058600     DISPLAY-OUTPUT-DEVICE.                                       NC2044.2
058700     MOVE     "A1B2C3D4E5 ALPHANUMERIC LITERAL" TO DIS-PLAYER.    NC2044.2
058800     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
058900     GO       TO DIS-WRITE-GF-6.                                  NC2044.2
059000 DIS-DELETE-GF-6.                                                 NC2044.2
059100     PERFORM  DE-LETE.                                            NC2044.2
059200 DIS-WRITE-GF-6.                                                  NC2044.2
059300     MOVE     "DIS-TEST-GF-6 " TO PAR-NAME.                       NC2044.2
059400     PERFORM  PRINT-DETAIL.                                       NC2044.2
059500*                                                                 NC2044.2
059600 DIS-INIT-GF-7.                                                   NC2044.2
059700     MOVE     "DIS-TEST-GF-7 " TO PAR-NAME.                       NC2044.2
059800     MOVE     "ALPHABETIC"     TO DISPLAY-A.                      NC2044.2
059900     MOVE     0123456789       TO DISPLAY-N.                      NC2044.2
060000     MOVE     "A1B2C3D4E5"     TO DISPLAY-X.                      NC2044.2
060100 DIS-TEST-GF-7.                                                   NC2044.2
060200     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
060300     DISPLAY DISPLAY-A DISPLAY-N DISPLAY-X " SERIES" UPON         NC2044.2
060400     DISPLAY-OUTPUT-DEVICE.                                       NC2044.2
060500     MOVE     "ALPHABETIC0123456789A1B2C3D4E5 SERIES"             NC2044.2
060600              TO DIS-PLAYER.                                      NC2044.2
060700     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
060800     GO       TO DIS-WRITE-GF-7.                                  NC2044.2
060900 DIS-DELETE-GF-7.                                                 NC2044.2
061000     PERFORM  DE-LETE.                                            NC2044.2
061100 DIS-WRITE-GF-7.                                                  NC2044.2
061200     MOVE     "DIS-TEST-GF-7 " TO PAR-NAME.                       NC2044.2
061300     PERFORM  PRINT-DETAIL.                                       NC2044.2
061400*                                                                 NC2044.2
061500 DIS-INIT-GF-8.                                                   NC2044.2
061600     MOVE     "DIS-TEST-GF-8 " TO PAR-NAME.                       NC2044.2
061700 DIS-TEST-GF-8.                                                   NC2044.2
061800     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
061900     DISPLAY ZERO SPACE QUOTE UPON DISPLAY-OUTPUT-DEVICE.         NC2044.2
062000*    DISPLAY FIGURATIVE CONSTANT ONE ZERO EXPECTED.               NC2044.2
062100     MOVE      ZERO-SPACE-QUOTE TO DIS-PLAYER.                    NC2044.2
062200     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
062300     GO       TO DIS-WRITE-GF-8.                                  NC2044.2
062400 DIS-DELETE-GF-8.                                                 NC2044.2
062500     PERFORM  DE-LETE.                                            NC2044.2
062600 DIS-WRITE-GF-8.                                                  NC2044.2
062700     MOVE     "DIS-TEST-GF-8 " TO PAR-NAME.                       NC2044.2
062800     PERFORM  PRINT-DETAIL.                                       NC2044.2
062900*                                                                 NC2044.2
063000 DIS-INIT-GF-9.                                                   NC2044.2
063100     MOVE "DIS-TEST-GF-9 " TO PAR-NAME.                           NC2044.2
063200     MOVE "REDEFINE-INFO" TO DISPLAY-B.                           NC2044.2
063300 DIS-TEST-GF-9.                                                   NC2044.2
063400     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
063500     DISPLAY  DISPLAY-C UPON DISPLAY-OUTPUT-DEVICE.               NC2044.2
063600*    DISPLAY  REDEFINES FIELD.                                    NC2044.2
063700     MOVE     "REDEFINE-INFO" TO DIS-PLAYER.                      NC2044.2
063800     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
063900     GO       TO DIS-WRITE-GF-9.                                  NC2044.2
064000 DIS-DELETE-GF-9.                                                 NC2044.2
064100     PERFORM  DE-LETE.                                            NC2044.2
064200 DIS-WRITE-GF-9.                                                  NC2044.2
064300     MOVE     "DIS-TEST-GF-9 " TO PAR-NAME.                       NC2044.2
064400     PERFORM  PRINT-DETAIL.                                       NC2044.2
064500*                                                                 NC2044.2
064600 DIS-INIT-GF-10.                                                  NC2044.2
064700     MOVE     "DIS-TEST-GF-10" TO PAR-NAME.                       NC2044.2
064800     MOVE "D001*002*003*004*005*006*007*008*009*010*011*012*013*01NC2044.2
064900-    "4*015*016*017*018*019*020*021*022*023*024*025" TO DISPLAY-G.NC2044.2
065000     MOVE "*026*027*028*029*030*031*032*033*034*035*036*037*038*03NC2044.2
065100-    "9*040*041*042*043*044*045*046*047*048*049*050" TO DISPLAY-H.NC2044.2
065200 DIS-TEST-GF-10.                                                  NC2044.2
065300     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
065400     DISPLAY DISPLAY-F UPON DISPLAY-OUTPUT-DEVICE.                NC2044.2
065500     MOVE     DISPLAY-G TO DIS-PLAYER.                            NC2044.2
065600     MOVE     1 TO DISPLAY-SWITCH.                                NC2044.2
065700     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
065800*        NOTE THE "CORRECT" RESULT IS WRITTEN AS TWO              NC2044.2
065900*             100-CHARACTER LINES, BUT THE WAY THAT THE           NC2044.2
066000*             "COMPUTED" RESULT IS SPLIT UP IS NOT                NC2044.2
066100*             DEFINED BY THE STANDARD --- REGARDLESS OF           NC2044.2
066200*             THIS, ALL 200 CHARACTERS MUST BE DISPLAYED.         NC2044.2
066300     GO       TO DIS-WRITE-GF-10.                                 NC2044.2
066400 DIS-DELETE-GF-10.                                                NC2044.2
066500     PERFORM  DE-LETE.                                            NC2044.2
066600 DIS-WRITE-GF-10.                                                 NC2044.2
066700     MOVE     "DIS-TEST-GF-10" TO PAR-NAME.                       NC2044.2
066800     PERFORM  PRINT-DETAIL.                                       NC2044.2
066900*                                                                 NC2044.2
067000 DIS-INIT-GF-11.                                                  NC2044.2
067100     MOVE "DIS-TEST-GF-11"             TO PAR-NAME.               NC2044.2
067200     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO GRP-ALPHABETIC.         NC2044.2
067300     MOVE 0123456789                   TO DIGITS-DV-10V00.        NC2044.2
067400     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-<>=l,:.()/* 0123456789"    NC2044.2
067500     TO GRP-ALPHANUMERIC.                                         NC2044.2
067600 DIS-TEST-GF-11.                                                  NC2044.2
067700     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
067800     DISPLAY  GRP-ALPHABETIC UPON DISPLAY-OUTPUT-DEVICE.          NC2044.2
067900     DISPLAY  GRP-NUMERIC    UPON DISPLAY-OUTPUT-DEVICE.          NC2044.2
068000     DISPLAY  GRP-ALPHANUMERIC UPON DISPLAY-OUTPUT-DEVICE.        NC2044.2
068100     MOVE     GRP-ALPHABETIC TO DIS-PLAYER                        NC2044.2
068200     MOVE     2 TO DISPLAY-SWITCH.                                NC2044.2
068300     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
068400     GO       TO DIS-WRITE-GF-11.                                 NC2044.2
068500 DIS-DELETE-GF-11.                                                NC2044.2
068600     PERFORM  DE-LETE.                                            NC2044.2
068700 DIS-WRITE-GF-11.                                                 NC2044.2
068800     MOVE     "DIS-TEST-GF-11" TO PAR-NAME.                       NC2044.2
068900     PERFORM  PRINT-DETAIL.                                       NC2044.2
069000*                                                                 NC2044.2
069100 DIS-INIT-GF-12.                                                  NC2044.2
069200     MOVE     "DIS-TEST-GF-12" TO PAR-NAME.                       NC2044.2
069300 DIS-TEST-GF-12.                                                  NC2044.2
069400     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
069500     DISPLAY  X21 X20 X19 X18 X17 X16 X15 X14 X13 X12 X11 X10 X9  NC2044.2
069600              X8 X7 X6 X5 X4 X3 X2 X1 UPON DISPLAY-OUTPUT-DEVICE. NC2044.2
069700     MOVE     "UTSRQPONMLKJIHGFEDCBA" TO DIS-PLAYER.              NC2044.2
069800     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
069900     GO       TO DIS-WRITE-GF-12.                                 NC2044.2
070000 DIS-DELETE-GF-12.                                                NC2044.2
070100     PERFORM  DE-LETE.                                            NC2044.2
070200 DIS-WRITE-GF-12.                                                 NC2044.2
070300     MOVE     "DIS-TEST-GF-12" TO PAR-NAME.                       NC2044.2
070400     PERFORM  PRINT-DETAIL.                                       NC2044.2
070500*                                                                 NC2044.2
070600 DIS-INIT-GF-13.                                                  NC2044.2
070700     MOVE     "DIS-TEST-GF-13"        TO PAR-NAME.                NC2044.2
070800     MOVE     "ABCDEFGHIJKLMNOPQRSTU" TO QUAL-TAB-VALUE.          NC2044.2
070900 DIS-TEST-GF-13.                                                  NC2044.2
071000     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
071100     DISPLAY  XTAB (1), XTAB (2), XTAB (3), XTAB (4),             NC2044.2
071200              XTAB (5), XTAB (6), XTAB (7), XTAB (8),             NC2044.2
071300              XTAB (9),                                           NC2044.2
071400              ELEM-1 OF GRP-1,                                    NC2044.2
071500              ELEM-2 OF GRP-1,                                    NC2044.2
071600              ELEM-3 OF GRP-1,                                    NC2044.2
071700     SUB-TAB OF GRP-1 (1),                                        NC2044.2
071800     SUB-TAB OF GRP-1 (2),                                        NC2044.2
071900     SUB-TAB OF GRP-1 (3),                                        NC2044.2
072000              ELEM-1 IN GRP-2,                                    NC2044.2
072100              ELEM-2 IN GRP-2,                                    NC2044.2
072200              ELEM-3 IN GRP-2,                                    NC2044.2
072300     SUB-TAB OF GRP-2 (1),                                        NC2044.2
072400     SUB-TAB OF GRP-2 (2),                                        NC2044.2
072500     SUB-TAB OF GRP-2 (3)  UPON DISPLAY-OUTPUT-DEVICE.            NC2044.2
072600*        NOTE DISPLAY 21 VARIABLES, SUBSCRIPTED, QUALIFIED, BOTH. NC2044.2
072700     MOVE     QUAL-TAB-VALUE TO DIS-PLAYER.                       NC2044.2
072800     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
072900     GO       TO DIS-WRITE-GF-13.                                 NC2044.2
073000 DIS-DELETE-GF-13.                                                NC2044.2
073100     PERFORM  DE-LETE.                                            NC2044.2
073200 DIS-WRITE-GF-13.                                                 NC2044.2
073300     MOVE     "DIS-TEST-GF-13" TO PAR-NAME.                       NC2044.2
073400     PERFORM  PRINT-DETAIL.                                       NC2044.2
073500*                                                                 NC2044.2
073600 DIS-INIT-GF-14.                                                  NC2044.2
073700     MOVE     "DIS-TEST-GF-14" TO PAR-NAME.                       NC2044.2
073800     MOVE     "SEE NOTE IN DIS-TEST-GF-14" TO RE-MARK.            NC2044.2
073900 DIS-TEST-GF-14.                                                  NC2044.2
074000     PERFORM  DISPLAY-SUPPORT-1.                                  NC2044.2
074100     DISPLAY  "QUOTE "                                            NC2044.2
074200              QUOTES                                              NC2044.2
074300              " ASTERISK "                                        NC2044.2
074400              "*"                                                 NC2044.2
074500              " NUMERIC LITERALS "                                NC2044.2
074600              21                                                  NC2044.2
074700              SPACES                                              NC2044.2
074800              1325                                                NC2044.2
074900              I-DATA                                              NC2044.2
075000              PIECE (1, 1)                                        NC2044.2
075100              PIECE (1, 2)                                        NC2044.2
075200              PIECE (1, 3)                                        NC2044.2
075300              PIECE (1, 4)                                        NC2044.2
075400              PIECE (1, 5)                                        NC2044.2
075500              PIECE (2, 1)                                        NC2044.2
075600              PIECE (2, 2)                                        NC2044.2
075700              PIECE (2, 3)                                        NC2044.2
075800              PIECE (2, 4)                                        NC2044.2
075900              PIECE (2, 5)                                        NC2044.2
076000              A1 OF TRUE-PAIR                                     NC2044.2
076100              A2 IN TRUE-PAIR UPON DISPLAY-OUTPUT-DEVICE.         NC2044.2
076200*        NOTE 21 OPERANDS, 111 CHARACTERS.                        NC2044.2
076300     MOVE     DISPLAY-MIXTURE TO DIS-PLAYER.                      NC2044.2
076400     PERFORM  DISPLAY-SUPPORT-2.                                  NC2044.2
076500     GO       TO DIS-WRITE-GF-14.                                 NC2044.2
076600 DIS-DELETE-GF-14.                                                NC2044.2
076700     PERFORM  DE-LETE.                                            NC2044.2
076800 DIS-WRITE-GF-14.                                                 NC2044.2
076900     MOVE     "DIS-TEST-GF-14" TO PAR-NAME.                       NC2044.2
077000     PERFORM  PRINT-DETAIL.                                       NC2044.2
077100*                                                                 NC2044.2
077200 DISP-INIT-GF-15.                                                 NC2044.2
077300*    ==--> SINGLE IDENTIFIER WITH "WITH NO ADVANCING" PHRASE <--==NC2044.2
077400     MOVE   "VI-79 6.10.4 GR8" TO ANSI-REFERENCE.                 NC2044.2
077500     MOVE   "DIS-TEST-GF-15 " TO PAR-NAME.                        NC2044.2
077600     MOVE   "PLEASE PERFORM A VISUAL CHECK ON THE POSITIONING"    NC2044.2
077700          TO RE-MARK.                                             NC2044.2
077800     PERFORM PRINT-DETAIL.                                        NC2044.2
077900     MOVE   "OF THE HARDWARE DEVICE AFTER THIS TEST."             NC2044.2
078000          TO RE-MARK.                                             NC2044.2
078100     PERFORM PRINT-DETAIL.                                        NC2044.2
078200     PERFORM DISPLAY-SUPPORT-1.                                   NC2044.2
078300 DIS-TEST-GF-15.                                                  NC2044.2
078400     DISPLAY 9876543210 UPON DISPLAY-OUTPUT-DEVICE                NC2044.2
078500             WITH NO ADVANCING.                                   NC2044.2
078600     MOVE   "9876543210" TO DIS-PLAYER.                           NC2044.2
078700     PERFORM DISPLAY-SUPPORT-2.                                   NC2044.2
078800     GO      TO DIS-WRITE-GF-15.                                  NC2044.2
078900 DIS-DELETE-GF-15.                                                NC2044.2
079000     PERFORM DE-LETE.                                             NC2044.2
079100 DIS-WRITE-GF-15.                                                 NC2044.2
079200     PERFORM PRINT-DETAIL.                                        NC2044.2
079300*                                                                 NC2044.2
079400 DISP-INIT-GF-16.                                                 NC2044.2
079500*    ==--> MULTPL IDENTIFIERS WITH "WITH NO ADVANCING" PHRASE <--=NC2044.2
079600     MOVE   "VI-79 6.10.4 GR8" TO ANSI-REFERENCE.                 NC2044.2
079700     MOVE   "DIS-TEST-GF-16 " TO PAR-NAME.                        NC2044.2
079800     MOVE   "PLEASE PERFORM A VISUAL CHECK ON THE POSITIONING"    NC2044.2
079900          TO RE-MARK.                                             NC2044.2
080000     PERFORM PRINT-DETAIL.                                        NC2044.2
080100     MOVE   "OF THE HARDWARE DEVICE AFTER THIS TEST."             NC2044.2
080200          TO RE-MARK.                                             NC2044.2
080300     PERFORM PRINT-DETAIL.                                        NC2044.2
080400     PERFORM DISPLAY-SUPPORT-1.                                   NC2044.2
080500     MOVE "ALPHABETIC" TO DISPLAY-A.                              NC2044.2
080600     MOVE 0123456789   TO DISPLAY-N.                              NC2044.2
080700     MOVE "A1B2C3D4E5" TO DISPLAY-X.                              NC2044.2
080800 DIS-TEST-GF-16.                                                  NC2044.2
080900     DISPLAY DISPLAY-A DISPLAY-N DISPLAY-X " SERIES"              NC2044.2
081000        UPON DISPLAY-OUTPUT-DEVICE WITH NO ADVANCING.             NC2044.2
081100     MOVE   "ALPHABETIC0123456789A1B2C3D4E5 SERIES"               NC2044.2
081200          TO DIS-PLAYER.                                          NC2044.2
081300     PERFORM DISPLAY-SUPPORT-2.                                   NC2044.2
081400     GO      TO DIS-WRITE-GF-16.                                  NC2044.2
081500 DIS-DELETE-GF-16.                                                NC2044.2
081600     PERFORM DE-LETE.                                             NC2044.2
081700 DIS-WRITE-GF-16.                                                 NC2044.2
081800     MOVE   "DIS-TEST-GF-16 " TO PAR-NAME.                        NC2044.2
081900     PERFORM PRINT-DETAIL.                                        NC2044.2
082000*                                                                 NC2044.2
082100 AC-CEPT SECTION.                                                 NC2044.2
082200 ACC-INIT-F1.                                                     NC2044.2
082300     MOVE   "ACCEPT     " TO FEATURE.                             NC2044.2
082400     MOVE   "VI-71 6.5.2" TO ANSI-REFERENCE.                      NC2044.2
082500 ACC-INIT-F1-1.                                                   NC2044.2
082600     MOVE   "ACC-TEST-F1-1 " TO PAR-NAME.                         NC2044.2
082700     MOVE   "ABCDEFGHIJKLMNOPQRSTUVWXY Z" TO ACCEPT-D2.           NC2044.2
082800 ACC-TEST-F1-1.                                                   NC2044.2
082900     ACCEPT   ACCEPT-D1 FROM ACCEPT-INPUT-DEVICE.                 NC2044.2
083000     IF       ACCEPT-D1 EQUAL TO ACCEPT-D2                        NC2044.2
083100              PERFORM PASS GO TO ACC-WRITE-F1-1.                  NC2044.2
083200     GO       TO ACC-FAIL-F1-1.                                   NC2044.2
083300 ACC-DELETE-F1-1.                                                 NC2044.2
083400     PERFORM  DE-LETE.                                            NC2044.2
083500     GO       TO ACC-WRITE-F1-1.                                  NC2044.2
083600 ACC-FAIL-F1-1.                                                   NC2044.2
083700     MOVE     ACCEPT-D1-A TO COMPUTED-A.                          NC2044.2
083800     MOVE     "ABCDEFGHIJKLMNOPQRST" TO CORRECT-A.                NC2044.2
083900     PERFORM  PRINT-DETAIL.                                       NC2044.2
084000     MOVE     ACCEPT-D1-B TO COMPUTED-A.                          NC2044.2
084100     MOVE     "UVWXY Z" TO CORRECT-A.                             NC2044.2
084200     PERFORM  FAIL.                                               NC2044.2
084300     MOVE     "LAST 7 OF 27-CHAR FIELD" TO RE-MARK.               NC2044.2
084400 ACC-WRITE-F1-1.                                                  NC2044.2
084500     PERFORM  PRINT-DETAIL.                                       NC2044.2
084600*                                                                 NC2044.2
084700 ACC-INIT-F1-2.                                                   NC2044.2
084800     MOVE     "ACC-TEST-F1-2 " TO PAR-NAME.                       NC2044.2
084900     MOVE     0123456789       TO ACCEPT-D4.                      NC2044.2
085000 ACC-TEST-F1-2.                                                   NC2044.2
085100     ACCEPT   ACCEPT-D3 FROM ACCEPT-INPUT-DEVICE.                 NC2044.2
085200     IF       ACCEPT-D3  EQUAL TO ACCEPT-D4                       NC2044.2
085300              PERFORM PASS GO TO ACC-WRITE-F1-2.                  NC2044.2
085400     GO       TO ACC-FAIL-F1-2.                                   NC2044.2
085500 ACC-DELETE-F1-2.                                                 NC2044.2
085600     PERFORM  DE-LETE.                                            NC2044.2
085700     GO       TO ACC-WRITE-F1-2.                                  NC2044.2
085800 ACC-FAIL-F1-2.                                                   NC2044.2
085900     MOVE     ACCEPT-D3  TO COMPUTED-18V0.                        NC2044.2
086000     MOVE     ACCEPT-D4  TO CORRECT-18V0.                         NC2044.2
086100     PERFORM  FAIL.                                               NC2044.2
086200 ACC-WRITE-F1-2.                                                  NC2044.2
086300     PERFORM  PRINT-DETAIL.                                       NC2044.2
086400*                                                                 NC2044.2
086500 ACC-INIT-F1-3.                                                   NC2044.2
086600     MOVE     "ACC-TEST-F1-3 " TO PAR-NAME.                       NC2044.2
086700     MOVE     "().+-*/$, ="    TO ACCEPT-D6.                      NC2044.2
086800 ACC-TEST-F1-3.                                                   NC2044.2
086900     ACCEPT   ACCEPT-D5 FROM ACCEPT-INPUT-DEVICE.                 NC2044.2
087000     IF       ACCEPT-D5  EQUAL TO ACCEPT-D6                       NC2044.2
087100              PERFORM PASS GO TO ACC-WRITE-F1-3.                  NC2044.2
087200*        NOTE ACCEPT SPECIAL CHARACTERS.                          NC2044.2
087300     GO       TO ACC-FAIL-F1-3.                                   NC2044.2
087400 ACC-DELETE-F1-3.                                                 NC2044.2
087500     PERFORM  DE-LETE.                                            NC2044.2
087600     GO       TO ACC-WRITE-F1-3.                                  NC2044.2
087700 ACC-FAIL-F1-3.                                                   NC2044.2
087800     MOVE     ACCEPT-D5  TO COMPUTED-A.                           NC2044.2
087900     MOVE     ACCEPT-D6  TO CORRECT-A.                            NC2044.2
088000     PERFORM  FAIL.                                               NC2044.2
088100 ACC-WRITE-F1-3.                                                  NC2044.2
088200     PERFORM  PRINT-DETAIL.                                       NC2044.2
088300*                                                                 NC2044.2
088400 ACC-INIT-F1-4.                                                   NC2044.2
088500     MOVE     "ACC-TEST-F1-4 " TO PAR-NAME.                       NC2044.2
088600     MOVE     "9"              TO ACCEPT-D8.                      NC2044.2
088700 ACC-TEST-F1-4.                                                   NC2044.2
088800     ACCEPT   ACCEPT-D7 FROM ACCEPT-INPUT-DEVICE.                 NC2044.2
088900     IF       ACCEPT-D7  EQUAL TO ACCEPT-D8                       NC2044.2
089000              PERFORM PASS GO TO ACC-WRITE-F1-4.                  NC2044.2
089100     GO       TO ACC-FAIL-F1-4.                                   NC2044.2
089200 ACC-DELETE-F1-4.                                                 NC2044.2
089300     PERFORM  DE-LETE.                                            NC2044.2
089400     GO       TO ACC-WRITE-F1-4.                                  NC2044.2
089500 ACC-FAIL-F1-4.                                                   NC2044.2
089600     MOVE     ACCEPT-D7  TO COMPUTED-A.                           NC2044.2
089700     MOVE     ACCEPT-D8  TO CORRECT-A.                            NC2044.2
089800     MOVE     "9 EXPECTED" TO RE-MARK.                            NC2044.2
089900     PERFORM  FAIL.                                               NC2044.2
090000 ACC-WRITE-F1-4.                                                  NC2044.2
090100     PERFORM  PRINT-DETAIL.                                       NC2044.2
090200*                                                                 NC2044.2
090300 ACC-INIT-F1-5.                                                   NC2044.2
090400     MOVE     "ACC-TEST-F1-5 " TO PAR-NAME.                       NC2044.2
090500     MOVE     "0"              TO ACCEPT-D10.                     NC2044.2
090600 ACC-TEST-F1-5.                                                   NC2044.2
090700     ACCEPT   ACCEPT-D9 FROM ACCEPT-INPUT-DEVICE.                 NC2044.2
090800     IF       ACCEPT-D9  EQUAL TO ACCEPT-D10                      NC2044.2
090900              PERFORM PASS GO TO ACC-WRITE-F1-5.                  NC2044.2
091000     GO       TO ACC-FAIL-F1-5.                                   NC2044.2
091100 ACC-DELETE-F1-5.                                                 NC2044.2
091200     PERFORM  DE-LETE.                                            NC2044.2
091300     GO       TO ACC-WRITE-F1-5.                                  NC2044.2
091400 ACC-FAIL-F1-5.                                                   NC2044.2
091500     MOVE     ACCEPT-D9  TO COMPUTED-A.                           NC2044.2
091600     MOVE     ACCEPT-D10 TO CORRECT-A.                            NC2044.2
091700     MOVE     "0 EXPECTED" TO RE-MARK.                            NC2044.2
091800     PERFORM  FAIL.                                               NC2044.2
091900 ACC-WRITE-F1-5.                                                  NC2044.2
092000     PERFORM  PRINT-DETAIL.                                       NC2044.2
092100*                                                                 NC2044.2
092200 ACC-INIT-F1-6.                                                   NC2044.2
092300     MOVE     "ACC-TEST-F1-6 "       TO PAR-NAME.                 NC2044.2
092400     MOVE     " ABC            XYZ " TO ACCEPT-D12.               NC2044.2
092500 ACC-TEST-F1-6.                                                   NC2044.2
092600     ACCEPT   ACCEPT-D11 FROM ACCEPT-INPUT-DEVICE.                NC2044.2
092700     IF       ACCEPT-D11 EQUAL TO ACCEPT-D12                      NC2044.2
092800              PERFORM PASS GO TO ACC-WRITE-F1-6.                  NC2044.2
092900     GO       TO ACC-FAIL-F1-6.                                   NC2044.2
093000 ACC-DELETE-F1-6.                                                 NC2044.2
093100     PERFORM  DE-LETE.                                            NC2044.2
093200     GO       TO ACC-WRITE-F1-6.                                  NC2044.2
093300 ACC-FAIL-F1-6.                                                   NC2044.2
093400     MOVE     ACCEPT-D11 TO COMPUTED-A.                           NC2044.2
093500     MOVE     ACCEPT-D12 TO CORRECT-A.                            NC2044.2
093600     PERFORM  FAIL.                                               NC2044.2
093700 ACC-WRITE-F1-6.                                                  NC2044.2
093800     PERFORM  PRINT-DETAIL.                                       NC2044.2
093900*                                                                 NC2044.2
094000 ACC-INIT-F1-7.                                                   NC2044.2
094100     MOVE     "ACC-TEST-F1-7 " TO PAR-NAME.                       NC2044.2
094200     MOVE     " 9"             TO ACCEPT-D16.                     NC2044.2
094300 ACC-TEST-F1-7.                                                   NC2044.2
094400     ACCEPT   ACCEPT-D15 FROM ACCEPT-INPUT-DEVICE.                NC2044.2
094500     IF       ACCEPT-D15 EQUAL TO ACCEPT-D16                      NC2044.2
094600              PERFORM PASS GO TO ACC-WRITE-F1-7.                  NC2044.2
094700     GO       TO ACC-FAIL-F1-7.                                   NC2044.2
094800 ACC-DELETE-F1-7.                                                 NC2044.2
094900     PERFORM  DE-LETE.                                            NC2044.2
095000     GO       TO ACC-WRITE-F1-7.                                  NC2044.2
095100 ACC-FAIL-F1-7.                                                   NC2044.2
095200     PERFORM  FAIL.                                               NC2044.2
095300     MOVE     ACCEPT-D15 TO COMPUTED-A.                           NC2044.2
095400     MOVE    " 9    (SPACE 9)" TO CORRECT-A.                      NC2044.2
095500 ACC-WRITE-F1-7.                                                  NC2044.2
095600     PERFORM  PRINT-DETAIL.                                       NC2044.2
095700*                                                                 NC2044.2
095800 ACC-INIT-F1-8.                                                   NC2044.2
095900     MOVE     "ACC-TEST-F1-8 " TO PAR-NAME.                       NC2044.2
096000     MOVE     QUOTE            TO ACCEPT-D18.                     NC2044.2
096100 ACC-TEST-F1-8.                                                   NC2044.2
096200     ACCEPT   ACCEPT-D17 FROM ACCEPT-INPUT-DEVICE.                NC2044.2
096300     IF       ACCEPT-D17 EQUAL TO ACCEPT-D18                      NC2044.2
096400              PERFORM PASS GO TO ACC-WRITE-F1-8.                  NC2044.2
096500     GO       TO ACC-FAIL-F1-8.                                   NC2044.2
096600 ACC-DELETE-F1-8.                                                 NC2044.2
096700     PERFORM  DE-LETE.                                            NC2044.2
096800     GO       TO ACC-WRITE-F1-8.                                  NC2044.2
096900 ACC-FAIL-F1-8.                                                   NC2044.2
097000     PERFORM  FAIL.                                               NC2044.2
097100     MOVE     ACCEPT-D17 TO COMPUTED-A.                           NC2044.2
097200     MOVE     ACCEPT-D18 TO CORRECT-A.                            NC2044.2
097300 ACC-WRITE-F1-8.                                                  NC2044.2
097400     PERFORM  PRINT-DETAIL.                                       NC2044.2
097500*                                                                 NC2044.2
097600 ACC-INIT-F1-9.                                                   NC2044.2
097700     MOVE     "ACC-TEST-F1-9 " TO PAR-NAME.                       NC2044.2
097800     MOVE     "Q"              TO ACCEPT-D20.                     NC2044.2
097900 ACC-TEST-F1-9.                                                   NC2044.2
098000     ACCEPT   QUAL-ACCEPT OF ACCEPT-D19 FROM ACCEPT-INPUT-DEVICE. NC2044.2
098100     IF       ACCEPT-D19 EQUAL TO ACCEPT-D20                      NC2044.2
098200              PERFORM PASS GO TO ACC-WRITE-F1-9.                  NC2044.2
098300     GO       TO ACC-FAIL-F1-9.                                   NC2044.2
098400 ACC-DELETE-F1-9.                                                 NC2044.2
098500     PERFORM  DE-LETE.                                            NC2044.2
098600     GO       TO ACC-WRITE-F1-9.                                  NC2044.2
098700 ACC-FAIL-F1-9.                                                   NC2044.2
098800     PERFORM  FAIL.                                               NC2044.2
098900     MOVE     ACCEPT-D19 TO COMPUTED-A.                           NC2044.2
099000     MOVE     ACCEPT-D20 TO CORRECT-A.                            NC2044.2
099100 ACC-WRITE-F1-9.                                                  NC2044.2
099200     PERFORM  PRINT-DETAIL.                                       NC2044.2
099300*                                                                 NC2044.2
099400 ACC-INIT-F1-10.                                                  NC2044.2
099500     MOVE     "ACC-TEST-F1-10" TO PAR-NAME.                       NC2044.2
099600     MOVE     "....ABCD...."   TO ACCEPT-D22.                     NC2044.2
099700 ACC-TEST-F1-10.                                                  NC2044.2
099800     ACCEPT   TAB-ACCEPT (2) FROM ACCEPT-INPUT-DEVICE.            NC2044.2
099900     IF       ACCEPT-D21 EQUAL TO ACCEPT-D22                      NC2044.2
100000              PERFORM PASS GO TO ACC-WRITE-F1-10.                 NC2044.2
100100     GO       TO ACC-FAIL-F1-10.                                  NC2044.2
100200 ACC-DELETE-F1-10.                                                NC2044.2
100300     PERFORM  DE-LETE.                                            NC2044.2
100400     GO       TO ACC-WRITE-F1-10.                                 NC2044.2
100500 ACC-FAIL-F1-10.                                                  NC2044.2
100600     PERFORM  FAIL.                                               NC2044.2
100700     MOVE     ACCEPT-D21 TO COMPUTED-A.                           NC2044.2
100800     MOVE     ACCEPT-D22 TO CORRECT-A.                            NC2044.2
100900 ACC-WRITE-F1-10.                                                 NC2044.2
101000     PERFORM  PRINT-DETAIL.                                       NC2044.2
101100*                                                                 NC2044.2
101200 ACC-INIT-F1-11.                                                  NC2044.2
101300     MOVE     "ACC-TEST-F1-11"       TO PAR-NAME.                 NC2044.2
101400     MOVE     "--------------------" TO ACCEPT-D23.               NC2044.2
101500     MOVE     "----------------ABCD" TO ACCEPT-D24.               NC2044.2
101600 ACC-TEST-F1-11.                                                  NC2044.2
101700     ACCEPT   TAB-A IN ACCEPT-D23 (SUB) FROM ACCEPT-INPUT-DEVICE. NC2044.2
101800     IF       ACCEPT-D23 EQUAL TO ACCEPT-D24                      NC2044.2
101900              PERFORM PASS GO TO ACC-WRITE-F1-11.                 NC2044.2
102000     GO       TO ACC-FAIL-F1-11.                                  NC2044.2
102100 ACC-DELETE-F1-11.                                                NC2044.2
102200     PERFORM  DE-LETE.                                            NC2044.2
102300     GO       TO ACC-WRITE-F1-11.                                 NC2044.2
102400 ACC-FAIL-F1-11.                                                  NC2044.2
102500     PERFORM  FAIL.                                               NC2044.2
102600     MOVE     ACCEPT-D23 TO COMPUTED-A.                           NC2044.2
102700     MOVE     ACCEPT-D24 TO CORRECT-A.                            NC2044.2
102800 ACC-WRITE-F1-11.                                                 NC2044.2
102900     PERFORM  PRINT-DETAIL.                                       NC2044.2
103000*                                                                 NC2044.2
103100 ACC-INIT-F1-12.                                                  NC2044.2
103200     MOVE     "ACC-TEST-F1-12" TO PAR-NAME.                       NC2044.2
103300     MOVE                                                         NC2044.2
103400     "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z  0123456NC2044.2
103500-    "789                 "  TO ACCEPT-RESULTS.                   NC2044.2
103600 ACC-TEST-F1-12.                                                  NC2044.2
103700     ACCEPT   80X-CHARACTER-FIELD FROM ACCEPT-INPUT-DEVICE.       NC2044.2
103800     IF       80X-CHARACTER-FIELD EQUAL TO ACCEPT-RESULTS         NC2044.2
103900              PERFORM PASS GO TO ACC-WRITE-F1-12.                 NC2044.2
104000     GO       TO ACC-FAIL-F1-12.                                  NC2044.2
104100 ACC-DELETE-F1-12.                                                NC2044.2
104200     PERFORM  DE-LETE.                                            NC2044.2
104300     GO       TO ACC-WRITE-F1-12.                                 NC2044.2
104400 ACC-FAIL-F1-12.                                                  NC2044.2
104500     MOVE     80X-CHARACTER-FIELD TO CHARACTER-BREAKDOWN-R.       NC2044.2
104600     MOVE     ACCEPT-RESULTS TO CHARACTER-BREAKDOWN-S.            NC2044.2
104700     MOVE       FIRST-20R TO COMPUTED-A.                          NC2044.2
104800     MOVE       FIRST-20S TO CORRECT-A.                           NC2044.2
104900     PERFORM  PRINT-DETAIL.                                       NC2044.2
105000     MOVE      SECOND-20R TO COMPUTED-A.                          NC2044.2
105100     MOVE      SECOND-20S TO CORRECT-A.                           NC2044.2
105200     PERFORM  PRINT-DETAIL.                                       NC2044.2
105300     MOVE       THIRD-20R TO COMPUTED-A.                          NC2044.2
105400     MOVE       THIRD-20S TO CORRECT-A.                           NC2044.2
105500     PERFORM  PRINT-DETAIL.                                       NC2044.2
105600     MOVE      FOURTH-20R TO COMPUTED-A.                          NC2044.2
105700     MOVE      FOURTH-20S TO CORRECT-A.                           NC2044.2
105800     PERFORM  FAIL.                                               NC2044.2
105900     MOVE "LAST 20 OF  80 CHAR FIELD" TO RE-MARK.                 NC2044.2
106000 ACC-WRITE-F1-12.                                                 NC2044.2
106100     MOVE     "ACC-TEST-F1-12" TO PAR-NAME.                       NC2044.2
106200     PERFORM PRINT-DETAIL.                                        NC2044.2
106300*                                                                 NC2044.2
106400 ACC-INIT-F1-13.                                                  NC2044.2
106500     MOVE   "ACC-TEST-F1-13" TO PAR-NAME.                         NC2044.2
106600     MOVE "D001*002*003*004*005*006*007*008*009*010*011*012*013*01NC2044.2
106700-    "4*015*016*017*018*019*020D021*022*023*024*025" TO DISPLAY-G.NC2044.2
106800     MOVE "*026*027*028*029*030*031*032*033*034*035*036*037*038*03NC2044.2
106900-    "9*040D041*042*043*044*045*046*047*048*049*050" TO DISPLAY-H.NC2044.2
107000 ACC-TEST-F1-13.                                                  NC2044.2
107100     ACCEPT  ACCEPT-D13 FROM ACCEPT-INPUT-DEVICE.                 NC2044.2
107200     IF      ACCEPT-D13 EQUAL TO DISPLAY-F                        NC2044.2
107300             PERFORM PASS GO TO ACC-WRITE-F1-13.                  NC2044.2
107400     GO      TO ACC-FAIL-F1-13.                                   NC2044.2
107500 ACC-DELETE-F1-13.                                                NC2044.2
107600     PERFORM DE-LETE.                                             NC2044.2
107700     GO      TO ACC-WRITE-F1-13.                                  NC2044.2
107800 ACC-FAIL-F1-13.                                                  NC2044.2
107900     MOVE     ACCEPT-D13 TO CHARACTER-BREAKDOWN-R.                NC2044.2
108000     MOVE     DISPLAY-F TO CHARACTER-BREAKDOWN-S.                 NC2044.2
108100     MOVE      FIRST-20R TO COMPUTED-A.                           NC2044.2
108200     MOVE      FIRST-20S TO CORRECT-A.                            NC2044.2
108300     PERFORM  PRINT-DETAIL.                                       NC2044.2
108400     MOVE     SECOND-20R TO COMPUTED-A.                           NC2044.2
108500     MOVE     SECOND-20S TO CORRECT-A.                            NC2044.2
108600     PERFORM  PRINT-DETAIL.                                       NC2044.2
108700     MOVE      THIRD-20R TO COMPUTED-A.                           NC2044.2
108800     MOVE      THIRD-20S TO CORRECT-A.                            NC2044.2
108900     PERFORM  PRINT-DETAIL.                                       NC2044.2
109000     MOVE     FOURTH-20R TO COMPUTED-A.                           NC2044.2
109100     MOVE     FOURTH-20S TO CORRECT-A.                            NC2044.2
109200     PERFORM PRINT-DETAIL.                                        NC2044.2
109300     MOVE    FIFTH-20R TO COMPUTED-A.                             NC2044.2
109400     MOVE    FIFTH-20S    TO CORRECT-A.                           NC2044.2
109500     PERFORM PRINT-DETAIL.                                        NC2044.2
109600     MOVE    SIXTH-20R TO COMPUTED-A.                             NC2044.2
109700     MOVE    SIXTH-20S    TO CORRECT-A.                           NC2044.2
109800     PERFORM PRINT-DETAIL.                                        NC2044.2
109900     MOVE    SEVENTH-20R TO COMPUTED-A.                           NC2044.2
110000     MOVE    SEVENTH-20S  TO CORRECT-A.                           NC2044.2
110100     PERFORM PRINT-DETAIL.                                        NC2044.2
110200     MOVE    EIGHTH-20R TO COMPUTED-A.                            NC2044.2
110300     MOVE    EIGHTH-20S   TO CORRECT-A.                           NC2044.2
110400     PERFORM PRINT-DETAIL.                                        NC2044.2
110500     MOVE    NINTH-20R TO COMPUTED-A.                             NC2044.2
110600     MOVE    NINTH-20S TO CORRECT-A.                              NC2044.2
110700     PERFORM PRINT-DETAIL.                                        NC2044.2
110800     MOVE    TENTH-20R TO COMPUTED-A.                             NC2044.2
110900     MOVE    TENTH-20S TO CORRECT-A.                              NC2044.2
111000     PERFORM FAIL.                                                NC2044.2
111100     MOVE   "LAST 20 OF 200CHAR FIELD" TO RE-MARK.                NC2044.2
111200 ACC-WRITE-F1-13.                                                 NC2044.2
111300     MOVE   "ACC-TEST-F1-13" TO PAR-NAME.                         NC2044.2
111400     PERFORM PRINT-DETAIL.                                        NC2044.2
111500*                                                                 NC2044.2
111600 ACC-INIT-F1-14.                                                  NC2044.2
111700     MOVE   "VI-71 6.5.4 GR4(A)" TO ANSI-REFERENCE.               NC2044.2
111800     MOVE    SPACES TO ACCEPT-TEST-14-DATA.                       NC2044.2
111900     MOVE   "ACC-TEST-F1-14-1" TO PAR-NAME.                       NC2044.2
112000     MOVE   "PLEASE PERFORM A VISUAL CHECK TO ENSURE THAT"        NC2044.2
112100          TO RE-MARK.                                             NC2044.2
112200     PERFORM PRINT-DETAIL.                                        NC2044.2
112300     MOVE   "A REQUEST FOR FURTHER INPUT IS MADE BY THE"          NC2044.2
112400          TO RE-MARK.                                             NC2044.2
112500     PERFORM PRINT-DETAIL.                                        NC2044.2
112600     MOVE   "HARDWARE DEVICE" TO RE-MARK                          NC2044.2
112700     PERFORM PRINT-DETAIL.                                        NC2044.2
112800 ACC-INIT-F1-14-1.                                                NC2044.2
112900     MOVE   "ACC-TEST-F1-14-1" TO PAR-NAME.                       NC2044.2
113000 ACC-TEST-F1-14-1.                                                NC2044.2
113100     ACCEPT  ACCEPT-TEST-14-DATA FROM ACCEPT-INPUT-DEVICE.        NC2044.2
113200      IF     ACC-14-CHARS-1-10 = "ABCDEFGHIJ"                     NC2044.2
113300             PERFORM PASS                                         NC2044.2
113400             GO TO ACC-WRITE-F1-14-1.                             NC2044.2
113500     GO TO ACC-FAIL-F1-14-1.                                      NC2044.2
113600 ACC-DELETE-F1-14-1.                                              NC2044.2
113700     PERFORM DE-LETE.                                             NC2044.2
113800     GO TO ACC-WRITE-F1-14-1.                                     NC2044.2
113900 ACC-FAIL-F1-14-1.                                                NC2044.2
114000             MOVE   "ABCDEFGHIJ" TO CORRECT-A                     NC2044.2
114100                     MOVE   ACC-14-CHARS-1-10 TO COMPUTED-A       NC2044.2
114200             PERFORM FAIL.                                        NC2044.2
114300 ACC-WRITE-F1-14-1.                                               NC2044.2
114400             PERFORM PRINT-DETAIL.                                NC2044.2
114500*                                                                 NC2044.2
114600 ACC-INIT-F1-14-2.                                                NC2044.2
114700     MOVE   "ACC-TEST-F1-14-2" TO PAR-NAME.                       NC2044.2
114800 ACC-TEST-F1-14-2.                                                NC2044.2
114900     ACCEPT  ACCEPT-TEST-14-DATA FROM ACCEPT-INPUT-DEVICE.        NC2044.2
115000     IF      ACC-14-CHARS-11-15 = "KLMNO"                         NC2044.2
115100             PERFORM PASS                                         NC2044.2
115200             GO TO ACC-WRITE-F1-14-2.                             NC2044.2
115300     GO TO ACC-FAIL-F1-14-2.                                      NC2044.2
115400 ACC-DELETE-F1-14-2.                                              NC2044.2
115500     PERFORM DE-LETE.                                             NC2044.2
115600     GO TO ACC-WRITE-F1-14-2.                                     NC2044.2
115700 ACC-FAIL-F1-14-2.                                                NC2044.2
115800             MOVE   "KLMNO" TO CORRECT-A                          NC2044.2
115900             MOVE   ACC-14-CHARS-11-15 TO COMPUTED-A              NC2044.2
116000             PERFORM FAIL.                                        NC2044.2
116100 ACC-WRITE-F1-14-2.                                               NC2044.2
116200             PERFORM PRINT-DETAIL.                                NC2044.2
116300 ACCEPT-EXIT.                                                     NC2044.2
116400     GO TO    CCVS-EXIT.                                          NC2044.2
116500 DISPLAY-SUPPORT-1.                                               NC2044.2
116600     PERFORM  BLANK-LINE-PRINT.                                   NC2044.2
116700     MOVE     SPACE TO P-OR-F.                                    NC2044.2
116800     MOVE     SEE-BELOW TO COMPUTED-A.                            NC2044.2
116900     MOVE     SEE-BELOW TO CORRECT-A.                             NC2044.2
117000     PERFORM  PRINT-DETAIL.                                       NC2044.2
117100     MOVE     SPACE TO FEATURE.                                   NC2044.2
117200     DISPLAY  TEST-RESULTS UPON DISPLAY-OUTPUT-DEVICE.            NC2044.2
117300 DISPLAY-SUPPORT-2.                                               NC2044.2
117400     MOVE     SPACE TO TEST-RESULTS.                              NC2044.2
117500     DISPLAY  TEST-RESULTS UPON DISPLAY-OUTPUT-DEVICE.            NC2044.2
117600     MOVE     SPACE TO TEST-RESULTS.                              NC2044.2
117700     PERFORM  PRINT-DETAIL.                                       NC2044.2
117800     MOVE     CORRECT-FOLLOWS TO RE-MARK.                         NC2044.2
117900     PERFORM  PRINT-DETAIL.                                       NC2044.2
118000     PERFORM  BLANK-LINE-PRINT.                                   NC2044.2
118100     MOVE     DISPLAY-WRITER TO TEST-RESULTS.                     NC2044.2
118200     PERFORM  PRINT-DETAIL.                                       NC2044.2
118300     IF       DISPLAY-SWITCH EQUAL TO 1                           NC2044.2
118400              MOVE ZERO TO DISPLAY-SWITCH                         NC2044.2
118500              MOVE DISPLAY-H TO DIS-PLAYER                        NC2044.2
118600              MOVE DISPLAY-WRITER TO TEST-RESULTS                 NC2044.2
118700              PERFORM PRINT-DETAIL.                               NC2044.2
118800     IF       DISPLAY-SWITCH EQUAL TO 2                           NC2044.2
118900              MOVE ZERO TO DISPLAY-SWITCH                         NC2044.2
119000              MOVE GRP-NUMERIC TO DIS-PLAYER                      NC2044.2
119100              MOVE DISPLAY-WRITER TO TEST-RESULTS                 NC2044.2
119200              PERFORM PRINT-DETAIL                                NC2044.2
119300              MOVE GRP-ALPHANUMERIC TO DIS-PLAYER                 NC2044.2
119400              MOVE DISPLAY-WRITER TO TEST-RESULTS                 NC2044.2
119500              PERFORM PRINT-DETAIL.                               NC2044.2
119600     MOVE     SPACE TO TEST-RESULTS.                              NC2044.2
119700     PERFORM  BLANK-LINE-PRINT.                                   NC2044.2
119800     IF       DISPLAY-SWITCH EQUAL TO 1                           NC2044.2
119900              MOVE "SEE NOTE IN DIS-TEST-GF-10" TO RE-MARK        NC2044.2
120000              PERFORM PRINT-DETAIL.                               NC2044.2
120100     MOVE     "DISPLAY UPON" TO FEATURE.                          NC2044.2
120200     MOVE     SEE-ABOVE TO COMPUTED-A.                            NC2044.2
120300     MOVE     SEE-ABOVE TO CORRECT-A.                             NC2044.2
120400     MOVE     END-CORRECT TO RE-MARK.                             NC2044.2
120500 CCVS-EXIT SECTION.                                               NC2044.2
120600 CCVS-999999.                                                     NC2044.2
120700     GO TO CLOSE-FILES.                                           NC2044.2
      *END-OF,NC204M                                                                  
