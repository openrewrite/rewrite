      *HEADER,COBOL,NC105A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1054.2
000200 PROGRAM-ID.                                                      NC1054.2
000300     NC105A.                                                      NC1054.2
000400****************************************************************  NC1054.2
000500*                                                              *  NC1054.2
000600*    VALIDATION FOR:-                                          *  NC1054.2
000700*                                                              *  NC1054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1054.2
000900*                                                              *  NC1054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1054.2
001100*                                                              *  NC1054.2
001200****************************************************************  NC1054.2
001300*                                                              *  NC1054.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1054.2
001500*                                                              *  NC1054.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1054.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1054.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1054.2
001900*                                                              *  NC1054.2
002000****************************************************************  NC1054.2
002100*                                                                 NC1054.2
002200*    PROGRAM NC105A CONTAINS FURTHER TESTS OF FORMAT 1 OF         NC1054.2
002300*    THE MOVE STATEMENT.                                          NC1054.2
002400*                                                                 NC1054.2
002500*    (SEE ALSO NC104A).                                           NC1054.2
002600*                                                                 NC1054.2
002700 ENVIRONMENT DIVISION.                                            NC1054.2
002800 CONFIGURATION SECTION.                                           NC1054.2
002900 SOURCE-COMPUTER.                                                 NC1054.2
003000     XXXXX082.                                                    NC1054.2
003100 OBJECT-COMPUTER.                                                 NC1054.2
003200     XXXXX083.                                                    NC1054.2
003300 INPUT-OUTPUT SECTION.                                            NC1054.2
003400 FILE-CONTROL.                                                    NC1054.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1054.2
003600     XXXXX055.                                                    NC1054.2
003700 DATA DIVISION.                                                   NC1054.2
003800 FILE SECTION.                                                    NC1054.2
003900 FD  PRINT-FILE                                                   NC1054.2
004000     LABEL RECORDS                                                NC1054.2
004100     XXXXX084                                                     NC1054.2
004200     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       NC1054.2
004300 01  PRINT-REC PICTURE X(120).                                    NC1054.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 NC1054.2
004500 WORKING-STORAGE SECTION.                                         NC1054.2
004600 77  LENGTH-COUNTER              PICTURE 999  VALUE 000.          NC1054.2
004700 77  SPOS-LIT1                    PICTURE S9(5)  VALUE +60666.    NC1054.2
004800 77  SPOS-LIT2                    PICTURE S9(5)  VALUE +60667.    NC1054.2
004900 77  SNEG-LIT1                    PICTURE S9(5)  VALUE -70717.    NC1054.2
005000 77  SNEG-LIT2                    PICTURE S9(5)  VALUE -70718.    NC1054.2
005100 77  ALPHA-LIT                    PICTURE X(5)   VALUE SPACE.     NC1054.2
005200 77  TA--X PIC 9(5) COMPUTATIONAL.                                NC1054.2
005300 77  WRK-CS-18V00   VALUE ZERO   PICTURE 9(18) COMPUTATIONAL.     NC1054.2
005400 77  WRK-CS-01V00   VALUE ZERO   PICTURE 9  COMPUTATIONAL.        NC1054.2
005500 77  WRK-CS-10V00   VALUE ZERO   PICTURE 9(10) COMPUTATIONAL.     NC1054.2
005600 77  WRK-DS-18V00   VALUE ZERO   PICTURE 9(18).                   NC1054.2
005700 77  WRK-DS-01V00   VALUE ZERO   PICTURE 9.                       NC1054.2
005800 77  WRK-DS-10V00   VALUE ZERO   PICTURE 9(10).                   NC1054.2
005900 77  WRK-CS-08V08 PIC S9(8)V9(8) VALUE 832.553 COMPUTATIONAL.     NC1054.2
006000 77  WRK-CS-04V08 PIC S9(4)V9(8) VALUE 6382.47 COMPUTATIONAL.     NC1054.2
006100 77  WRK-DS-08V08 PIC S9(8)V9(8) VALUE ZERO.                      NC1054.2
006200 77  WRK-DS-04V08 PIC S9(4)V9(8) VALUE ZERO.                      NC1054.2
006300 77  WRK-EDIT-Z3VZ3 PIC ZZZ.ZZZ.                                  NC1054.2
006400 77  WRK-EDIT-05V00 PIC ****9.                                    NC1054.2
006500 77  WRK-EDIT-18V00 PIC ZZZZZZZZZZZZZZZZZ9.                       NC1054.2
006600 77  WRK-EDIT-05V02 PIC -99999.99.                                NC1054.2
006700 77  WRK-CS-03V00 PIC S999 COMPUTATIONAL.                         NC1054.2
006800 77  MOVE74   PICTURE 9(9)V9 VALUE 234565432.1                    NC1054.2
006900              SYNCHRONIZED RIGHT COMPUTATIONAL.                   NC1054.2
007000 77  MOVE75   PICTURE 9(10)                                       NC1054.2
007100              SYNCHRONIZED RIGHT COMPUTATIONAL.                   NC1054.2
007200 77  EDIT-PICTURE-01 PICTURE 9B(15)99.                            NC1054.2
007300 77  EDIT-PICTURE-02   PICTURE $0(10)999.                         NC1054.2
007400 77  EDIT-DATA-1 PICTURE 999 VALUE 333.                           NC1054.2
007500 77  EDIT-DATA-2 PICTURE 999 VALUE 916.                           NC1054.2
007600 01  GRP-EDIT-PIC-05.                                             NC1054.2
007700     02 EDIT-PIC-05  PICTURE   $$$,999.99.                        NC1054.2
007800 01  GRP-EDIT-PIC-06.                                             NC1054.2
007900     02  EDIT-PIC-06  PICTURE   $$$B999.99.                       NC1054.2
008000 01  GRP-EDIT-PIC-07.                                             NC1054.2
008100     02  EDIT-PIC-07  PICTURE +++,999.99.                         NC1054.2
008200 01  GRP-EDIT-PIC-08.                                             NC1054.2
008300     02  EDIT-PIC-08  PICTURE   ---,999.99.                       NC1054.2
008400 01  GRP-EDIT-PIC-09.                                             NC1054.2
008500     02  EDIT-PIC-09  PICTURE   ***,999.99.                       NC1054.2
008600 01  GRP-EDIT-PIC-10.                                             NC1054.2
008700     02  EDIT-PIC-10  PICTURE  ZZZ,999.99.                        NC1054.2
008800 01  GRP-MOVE-CONSTANTS.                                          NC1054.2
008900     03  GRP-GROUP-MOVE-FROM.                                     NC1054.2
009000     04  GRP-ALPHABETIC.                                          NC1054.2
009100         05  ALPHABET-AN-00026   PICTURE A(26)                    NC1054.2
009200              VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ".                 NC1054.2
009300     04  GRP-NUMERIC.                                             NC1054.2
009400         05  DIGITS-DU-10V00     PICTURE 9(10)                    NC1054.2
009500              VALUE 0123456789.                                   NC1054.2
009600         05  DIGITS-DU-06V04-S REDEFINES DIGITS-DU-10V00          NC1054.2
009700                                 PICTURE 9(6)V9999.               NC1054.2
009800     04  GRP-ALPHANUMERIC.                                        NC1054.2
009900         05  ALPHANUMERIC-XN-00049  PICTURE X(49)                 NC1054.2
010000     VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789".   NC1054.2
010100 01  GRP-ALPHANUMERIC-1001.                                       NC1054.2
010200     04  GRP-ALPHANUMERIC-1002.                                   NC1054.2
010300     05 ALPHANUMERIC-XN-00050 PICTURE X(50) VALUE                 NC1054.2
010400     "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789 ".        NC1054.2
010500 01  GRP-MOVE-RECEIVING-FIELDS.                                   NC1054.2
010600     03  GRP-GROUP-MOVE-TO.                                       NC1054.2
010700     04  GRP-WRK-AN-00026.                                        NC1054.2
010800         05  WRK-AN-00026        PICTURE A(26).                   NC1054.2
010900     04  GRP-WRK-DU-10V00.                                        NC1054.2
011000         05  WRK-DU-10V00        PICTURE 9(10).                   NC1054.2
011100     04  GRP-WRK-XN-00049.                                        NC1054.2
011200         05  WRK-XN-00049        PICTURE X(49).                   NC1054.2
011300     04  GRP-NE-0001.                                             NC1054.2
011400         05  NE-0001             PICTURE ZZZ,999.999,9.           NC1054.2
011500     04  GRP-NE-0002.                                             NC1054.2
011600         05  NE-0002             PICTURE Z(7),999.                NC1054.2
011700     04  GRP-AE-0001.                                             NC1054.2
011800         05  AE-0001             PICTURE X(26)BX(12)0X(10).       NC1054.2
011900     04  GRP-AE-0002.                                             NC1054.2
012000         05  AE-0002             PICTURE XX0XXBXXX.               NC1054.2
012100 01  GRP-NUMERIC-99               PICTURE 99  VALUE 99.           NC1054.2
012200 01  GRP-RECEIVING.                                               NC1054.2
012300     02  RECEIVE-1.                                               NC1054.2
012400         03  RECEIVE-2            PICTURE 99  VALUE 03.           NC1054.2
012500         03  RECEIVE-3            PICTURE 9A9  VALUE ZERO.        NC1054.2
012600     02  RECEIVE-4                PICTURE 9(5)V99 VALUE ZERO.     NC1054.2
012700     02  RECEIVE-5                PICTURE X(4) VALUE ZERO.        NC1054.2
012800     02  RECEIVE-6.                                               NC1054.2
012900         03  RECEIVE-7            PICTURE 999  VALUE ZERO.        NC1054.2
013000         03  RECEIVE-8            PICTURE AA  VALUE "AA".         NC1054.2
013100 01  SEND-BREAKDOWN.                                              NC1054.2
013200     02  FIRST-20S               PICTURE X(20).                   NC1054.2
013300     02  SECOND-20S              PICTURE X(20).                   NC1054.2
013400     02  THIRD-20S               PICTURE X(20).                   NC1054.2
013500     02  FOURTH-20S              PICTURE X(20).                   NC1054.2
013600     02  FIFTH-20S               PICTURE X(20).                   NC1054.2
013700     02  SIXTH-20S               PICTURE X(20).                   NC1054.2
013800 01  RECEIVE-BREAKDOWN.                                           NC1054.2
013900     02  FIRST-20R               PICTURE X(20).                   NC1054.2
014000     02  SECOND-20R              PICTURE X(20).                   NC1054.2
014100     02  THIRD-20R               PICTURE X(20).                   NC1054.2
014200     02  FOURTH-20R              PICTURE X(20).                   NC1054.2
014300     02  FIFTH-20R               PICTURE X(20).                   NC1054.2
014400     02  SIXTH-20R               PICTURE X(20).                   NC1054.2
014500 01  FORTY-NINE-COMPARE.                                          NC1054.2
014600     02  FIRST-26                PICTURE X(26).                   NC1054.2
014700     02  PADD-REST               PICTURE X(23).                   NC1054.2
014800 01  HIGH-VALUE-EDIT.                                             NC1054.2
014900     02  HIGH-1                  PICTURE XX  VALUE HIGH-VALUE.    NC1054.2
015000     02  FILLER                  PICTURE 9   VALUE 0.             NC1054.2
015100     02  HIGH-2                  PICTURE XX  VALUE HIGH-VALUE.    NC1054.2
015200     02  FILLER                  PICTURE X   VALUE SPACE.         NC1054.2
015300     02  HIGH-3                  PICTURE XXX VALUE HIGH-VALUE.    NC1054.2
015400 01  HIGH-VALU-10LONG            PICTURE X(10) VALUE HIGH-VALUE.  NC1054.2
015500 01  LOW-VALU-10LONG             PICTURE X(10) VALUE LOW-VALUE.   NC1054.2
015600 01  HIGH-VALU-49LONG            PICTURE X(49) VALUE HIGH-VALUE.  NC1054.2
015700 01  LOW-VALU-49LONG             PICTURE X(49) VALUE LOW-VALUE.   NC1054.2
015800 01  QUOTE-10LONG                PICTURE X(10) VALUE QUOTE.       NC1054.2
015900 01  QUOTE-49LONG                PICTURE X(49) VALUE QUOTE.       NC1054.2
016000 01  MOVE1                              PICTURE IS 9(5)           NC1054.2
016100     VALUE IS 12345.                                              NC1054.2
016200 01  MOVE2                              PICTURE IS 9(5).          NC1054.2
016300 01  MOVE3                              PICTURE IS 99.            NC1054.2
016400 01  MOVE5                              PICTURE IS 99V999.        NC1054.2
016500 01  MOVE6                              PICTURE IS V99999.        NC1054.2
016600 01  MOVE7                              PICTURE IS 9V99.          NC1054.2
016700 01  MOVE16                             PICTURE IS 9(5)CR.        NC1054.2
016800 01  MOVE20                             PICTURE IS X(4).          NC1054.2
016900 01  MOVE21                             PICTURE IS X(7).          NC1054.2
017000 01  MOVE23                             PICTURE IS 999V99         NC1054.2
017100     VALUE IS 123.45.                                             NC1054.2
017200 01  MOVE29                             PICTURE IS 9999V999.      NC1054.2
017300 01  MOVE29X REDEFINES MOVE29           PICTURE IS X(7).          NC1054.2
017400 01  MOVE29A VALUE IS "$123.45".                                  NC1054.2
017500     02 MOVE30                          PICTURE IS $999.99.       NC1054.2
017600 01  MOVE32                             PICTURE IS X(5)           NC1054.2
017700     VALUE IS "ABCDE".                                            NC1054.2
017800 01  MOVE35                             PICTURE IS A(3).          NC1054.2
017900 01  MOVE35A VALUE IS "1 A05".                                    NC1054.2
018000     02 MOVE36                          PICTURE IS XBA09.         NC1054.2
018100 01  MOVE37                             PICTURE IS AAAAA          NC1054.2
018200     VALUE IS "ABCDE".                                            NC1054.2
018300 01  MOVE39                             PICTURE IS 0XXXXX0.       NC1054.2
018400 01  MOVE40                             PICTURE IS 9999V9.        NC1054.2
018500 01  MOVE41                             PICTURE IS A(7)           NC1054.2
018600     JUSTIFIED RIGHT.                                             NC1054.2
018700 01  MOVE42.                                                      NC1054.2
018800     02 MOVE43.                                                   NC1054.2
018900     03 MOVE43A                         PICTURE IS 999            NC1054.2
019000     VALUE IS 123.                                                NC1054.2
019100     03 MOVE43B                         PICTURE IS AAA            NC1054.2
019200     VALUE IS "ABC".                                              NC1054.2
019300     02 MOVE43C.                                                  NC1054.2
019400     03 MOVE44                          PICTURE IS 999            NC1054.2
019500     VALUE IS 123.                                                NC1054.2
019600     03 MOVE45                          PICTURE IS AAA            NC1054.2
019700     VALUE IS "ABC".                                              NC1054.2
019800     02 MOVE46 REDEFINES MOVE43C.                                 NC1054.2
019900     03 MOVE47                          PICTURE IS X OCCURS       NC1054.2
020000     6 TIMES.                                                     NC1054.2
020100 01  MOVE47A.                                                     NC1054.2
020200     02 MOVE48                          PICTURE IS 9V9(17).       NC1054.2
020300     02 MOVE49                          PICTURE IS 9(5)           NC1054.2
020400     VALUE IS 00045.                                              NC1054.2
020500     02 MOVE51                          PICTURE IS S9(5)          NC1054.2
020600     VALUE IS -12345.                                             NC1054.2
020700     02 MOVE51A                         PICTURE IS S9(5)          NC1054.2
020800     VALUE IS -00045.                                             NC1054.2
020900     02 MOVE52                          PICTURE IS 9(5)-.         NC1054.2
021000 01  MOVE66                             PICTURE IS 9(5)DB.        NC1054.2
021100 01  MOVE67                             PICTURE IS 9(5)+.         NC1054.2
021200 01  MOVE68                             PICTURE IS ++++99.        NC1054.2
021300 01  MOVE69                             PICTURE IS ----99.        NC1054.2
021400 01  MOVE70                             PICTURE IS 9(5).          NC1054.2
021500 01  MOVE71                             PICTURE X(20).            NC1054.2
021600 01  MOVE72                             PICTURE 9(10)             NC1054.2
021700              VALUE 3344556677.                                   NC1054.2
021800 01  MOVE73                             PICTURE X(5)BA(10)0X.     NC1054.2
021900 01  GRP-LEV-NUMERIC.                                             NC1054.2
022000     02 NUMERIC-LIT PICTURE 9(5).                                 NC1054.2
022100     02 CU-05V00-001 PIC 9(5) USAGE COMP.                         NC1054.2
022200     02 CU-03V02-001 PIC 999V99 USAGE COMP.                       NC1054.2
022300     02 CS-05V00-001 PIC S9(5) USAGE IS COMP.                     NC1054.2
022400 01  TEST-RESULTS.                                                NC1054.2
022500     02 FILLER                   PIC X      VALUE SPACE.          NC1054.2
022600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1054.2
022700     02 FILLER                   PIC X      VALUE SPACE.          NC1054.2
022800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1054.2
022900     02 FILLER                   PIC X      VALUE SPACE.          NC1054.2
023000     02  PAR-NAME.                                                NC1054.2
023100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1054.2
023200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1054.2
023300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1054.2
023400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1054.2
023500     02 RE-MARK                  PIC X(61).                       NC1054.2
023600 01  TEST-COMPUTED.                                               NC1054.2
023700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1054.2
023800     02 FILLER                   PIC X(17)  VALUE                 NC1054.2
023900            "       COMPUTED=".                                   NC1054.2
024000     02 COMPUTED-X.                                               NC1054.2
024100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1054.2
024200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1054.2
024300                                 PIC -9(9).9(9).                  NC1054.2
024400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1054.2
024500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1054.2
024600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1054.2
024700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1054.2
024800         04 COMPUTED-18V0                    PIC -9(18).          NC1054.2
024900         04 FILLER                           PIC X.               NC1054.2
025000     03 FILLER PIC X(50) VALUE SPACE.                             NC1054.2
025100 01  TEST-CORRECT.                                                NC1054.2
025200     02 FILLER PIC X(30) VALUE SPACE.                             NC1054.2
025300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1054.2
025400     02 CORRECT-X.                                                NC1054.2
025500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1054.2
025600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1054.2
025700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1054.2
025800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1054.2
025900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1054.2
026000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1054.2
026100         04 CORRECT-18V0                     PIC -9(18).          NC1054.2
026200         04 FILLER                           PIC X.               NC1054.2
026300     03 FILLER PIC X(2) VALUE SPACE.                              NC1054.2
026400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1054.2
026500 01  CCVS-C-1.                                                    NC1054.2
026600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1054.2
026700-    "SS  PARAGRAPH-NAME                                          NC1054.2
026800-    "       REMARKS".                                            NC1054.2
026900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1054.2
027000 01  CCVS-C-2.                                                    NC1054.2
027100     02 FILLER                     PIC X        VALUE SPACE.      NC1054.2
027200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1054.2
027300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1054.2
027400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1054.2
027500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1054.2
027600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1054.2
027700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1054.2
027800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1054.2
027900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1054.2
028000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1054.2
028100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1054.2
028200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1054.2
028300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1054.2
028400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1054.2
028500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1054.2
028600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1054.2
028700 01  CCVS-H-1.                                                    NC1054.2
028800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1054.2
028900     02  FILLER                    PIC X(42)    VALUE             NC1054.2
029000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1054.2
029100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1054.2
029200 01  CCVS-H-2A.                                                   NC1054.2
029300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1054.2
029400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1054.2
029500   02  FILLER                        PIC XXXX   VALUE             NC1054.2
029600     "4.2 ".                                                      NC1054.2
029700   02  FILLER                        PIC X(28)  VALUE             NC1054.2
029800            " COPY - NOT FOR DISTRIBUTION".                       NC1054.2
029900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1054.2
030000                                                                  NC1054.2
030100 01  CCVS-H-2B.                                                   NC1054.2
030200   02  FILLER                        PIC X(15)  VALUE             NC1054.2
030300            "TEST RESULT OF ".                                    NC1054.2
030400   02  TEST-ID                       PIC X(9).                    NC1054.2
030500   02  FILLER                        PIC X(4)   VALUE             NC1054.2
030600            " IN ".                                               NC1054.2
030700   02  FILLER                        PIC X(12)  VALUE             NC1054.2
030800     " HIGH       ".                                              NC1054.2
030900   02  FILLER                        PIC X(22)  VALUE             NC1054.2
031000            " LEVEL VALIDATION FOR ".                             NC1054.2
031100   02  FILLER                        PIC X(58)  VALUE             NC1054.2
031200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1054.2
031300 01  CCVS-H-3.                                                    NC1054.2
031400     02  FILLER                      PIC X(34)  VALUE             NC1054.2
031500            " FOR OFFICIAL USE ONLY    ".                         NC1054.2
031600     02  FILLER                      PIC X(58)  VALUE             NC1054.2
031700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1054.2
031800     02  FILLER                      PIC X(28)  VALUE             NC1054.2
031900            "  COPYRIGHT   1985 ".                                NC1054.2
032000 01  CCVS-E-1.                                                    NC1054.2
032100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1054.2
032200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1054.2
032300     02 ID-AGAIN                     PIC X(9).                    NC1054.2
032400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1054.2
032500 01  CCVS-E-2.                                                    NC1054.2
032600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1054.2
032700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1054.2
032800     02 CCVS-E-2-2.                                               NC1054.2
032900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1054.2
033000         03 FILLER                   PIC X      VALUE SPACE.      NC1054.2
033100         03 ENDER-DESC               PIC X(44)  VALUE             NC1054.2
033200            "ERRORS ENCOUNTERED".                                 NC1054.2
033300 01  CCVS-E-3.                                                    NC1054.2
033400     02  FILLER                      PIC X(22)  VALUE             NC1054.2
033500            " FOR OFFICIAL USE ONLY".                             NC1054.2
033600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1054.2
033700     02  FILLER                      PIC X(58)  VALUE             NC1054.2
033800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1054.2
033900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1054.2
034000     02 FILLER                       PIC X(15)  VALUE             NC1054.2
034100             " COPYRIGHT 1985".                                   NC1054.2
034200 01  CCVS-E-4.                                                    NC1054.2
034300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1054.2
034400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1054.2
034500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1054.2
034600     02 FILLER                       PIC X(40)  VALUE             NC1054.2
034700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1054.2
034800 01  XXINFO.                                                      NC1054.2
034900     02 FILLER                       PIC X(19)  VALUE             NC1054.2
035000            "*** INFORMATION ***".                                NC1054.2
035100     02 INFO-TEXT.                                                NC1054.2
035200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1054.2
035300       04 XXCOMPUTED                 PIC X(20).                   NC1054.2
035400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1054.2
035500       04 XXCORRECT                  PIC X(20).                   NC1054.2
035600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1054.2
035700 01  HYPHEN-LINE.                                                 NC1054.2
035800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1054.2
035900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1054.2
036000-    "*****************************************".                 NC1054.2
036100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1054.2
036200-    "******************************".                            NC1054.2
036300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1054.2
036400     "NC105A".                                                    NC1054.2
036500 PROCEDURE DIVISION.                                              NC1054.2
036600 CCVS1 SECTION.                                                   NC1054.2
036700 OPEN-FILES.                                                      NC1054.2
036800     OPEN     OUTPUT PRINT-FILE.                                  NC1054.2
036900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1054.2
037000     MOVE    SPACE TO TEST-RESULTS.                               NC1054.2
037100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1054.2
037200     GO TO CCVS1-EXIT.                                            NC1054.2
037300 CLOSE-FILES.                                                     NC1054.2
037400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1054.2
037500 TERMINATE-CCVS.                                                  NC1054.2
037600*    EXIT PROGRAM.                                                NC1054.2
037700*TERMINATE-CALL.                                                  NC1054.2
037800     STOP     RUN.                                                NC1054.2
037900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1054.2
038000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1054.2
038100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1054.2
038200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1054.2
038300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1054.2
038400 PRINT-DETAIL.                                                    NC1054.2
038500     IF REC-CT NOT EQUAL TO ZERO                                  NC1054.2
038600             MOVE "." TO PARDOT-X                                 NC1054.2
038700             MOVE REC-CT TO DOTVALUE.                             NC1054.2
038800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1054.2
038900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1054.2
039000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1054.2
039100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1054.2
039200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1054.2
039300     MOVE SPACE TO CORRECT-X.                                     NC1054.2
039400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1054.2
039500     MOVE     SPACE TO RE-MARK.                                   NC1054.2
039600 HEAD-ROUTINE.                                                    NC1054.2
039700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1054.2
039800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1054.2
039900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1054.2
040000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1054.2
040100 COLUMN-NAMES-ROUTINE.                                            NC1054.2
040200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1054.2
040300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1054.2
040400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1054.2
040500 END-ROUTINE.                                                     NC1054.2
040600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1054.2
040700 END-RTN-EXIT.                                                    NC1054.2
040800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1054.2
040900 END-ROUTINE-1.                                                   NC1054.2
041000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1054.2
041100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1054.2
041200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1054.2
041300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1054.2
041400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1054.2
041500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1054.2
041600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1054.2
041700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1054.2
041800  END-ROUTINE-12.                                                 NC1054.2
041900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1054.2
042000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1054.2
042100         MOVE "NO " TO ERROR-TOTAL                                NC1054.2
042200         ELSE                                                     NC1054.2
042300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1054.2
042400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1054.2
042500     PERFORM WRITE-LINE.                                          NC1054.2
042600 END-ROUTINE-13.                                                  NC1054.2
042700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1054.2
042800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1054.2
042900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1054.2
043000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1054.2
043100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1054.2
043200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1054.2
043300          MOVE "NO " TO ERROR-TOTAL                               NC1054.2
043400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1054.2
043500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1054.2
043600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1054.2
043700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1054.2
043800 WRITE-LINE.                                                      NC1054.2
043900     ADD 1 TO RECORD-COUNT.                                       NC1054.2
044000*    IF RECORD-COUNT GREATER 42                                   NC1054.2
044100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1054.2
044200*        MOVE SPACE TO DUMMY-RECORD                               NC1054.2
044300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1054.2
044400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1054.2
044500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1054.2
044600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1054.2
044700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1054.2
044800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1054.2
044900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1054.2
045000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1054.2
045100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1054.2
045200*        MOVE ZERO TO RECORD-COUNT.                               NC1054.2
045300     PERFORM WRT-LN.                                              NC1054.2
045400 WRT-LN.                                                          NC1054.2
045500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1054.2
045600     MOVE SPACE TO DUMMY-RECORD.                                  NC1054.2
045700 BLANK-LINE-PRINT.                                                NC1054.2
045800     PERFORM WRT-LN.                                              NC1054.2
045900 FAIL-ROUTINE.                                                    NC1054.2
046000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1054.2
046100            GO TO FAIL-ROUTINE-WRITE.                             NC1054.2
046200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1054.2
046300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1054.2
046400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1054.2
046500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1054.2
046600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1054.2
046700     GO TO  FAIL-ROUTINE-EX.                                      NC1054.2
046800 FAIL-ROUTINE-WRITE.                                              NC1054.2
046900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1054.2
047000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1054.2
047100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1054.2
047200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1054.2
047300 FAIL-ROUTINE-EX. EXIT.                                           NC1054.2
047400 BAIL-OUT.                                                        NC1054.2
047500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1054.2
047600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1054.2
047700 BAIL-OUT-WRITE.                                                  NC1054.2
047800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1054.2
047900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1054.2
048000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1054.2
048100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1054.2
048200 BAIL-OUT-EX. EXIT.                                               NC1054.2
048300 CCVS1-EXIT.                                                      NC1054.2
048400     EXIT.                                                        NC1054.2
048500 SECT-NC105A-001 SECTION.                                         NC1054.2
048600 MOVE-INIT-F1-1.                                                  NC1054.2
048700     MOVE "VI-102 6.18.2"        TO ANSI-REFERENCE.               NC1054.2
048800     MOVE "MOVE LITERAL        " TO FEATURE.                      NC1054.2
048900 MOVE-TEST-F1-1-0.                                                NC1054.2
049000     MOVE     123.45 TO MOVE40.                                   NC1054.2
049100 MOVE-TEST-F1-1-1.                                                NC1054.2
049200     IF       MOVE40 EQUAL TO 123.4                               NC1054.2
049300              PERFORM PASS                                        NC1054.2
049400     ELSE                                                         NC1054.2
049500              GO TO MOVE-FAIL-F1-1.                               NC1054.2
049600*    NOTE NUMERIC LITERAL NON INTEGRAL TO NNI MOVE, TRUNCATION ON NC1054.2
049700*    RIGHT, ZERO PADDIND ON LEFT.                                 NC1054.2
049800     GO TO    MOVE-WRITE-F1-1.                                    NC1054.2
049900 MOVE-DELETE-F1-1.                                                NC1054.2
050000     PERFORM  DE-LETE.                                            NC1054.2
050100     GO TO    MOVE-WRITE-F1-1.                                    NC1054.2
050200 MOVE-FAIL-F1-1.                                                  NC1054.2
050300     MOVE     MOVE40 TO COMPUTED-N.                               NC1054.2
050400     MOVE     123.4 TO CORRECT-N.                                 NC1054.2
050500     PERFORM  FAIL.                                               NC1054.2
050600 MOVE-WRITE-F1-1.                                                 NC1054.2
050700     MOVE "MOVE-TEST-F1-1" TO PAR-NAME.                           NC1054.2
050800     PERFORM  PRINT-DETAIL.                                       NC1054.2
050900 MOVE-TEST-F1-2-0.                                                NC1054.2
051000     MOVE     123.45 TO MOVE5.                                    NC1054.2
051100 MOVE-TEST-F1-2-1.                                                NC1054.2
051200     IF       MOVE5 EQUAL TO 23.45                                NC1054.2
051300              PERFORM PASS                                        NC1054.2
051400              ELSE                                                NC1054.2
051500              GO TO MOVE-FAIL-F1-2.                               NC1054.2
051600*    NOTE NUMERIC LITERAL NON-INTEGRAL TO NNI MOVE, TRUNCATION ON NC1054.2
051700*    LEFT, ZERO PADDING ON RIGHT.                                 NC1054.2
051800     GO TO    MOVE-WRITE-F1-2.                                    NC1054.2
051900 MOVE-DELETE-F1-2.                                                NC1054.2
052000     PERFORM  DE-LETE.                                            NC1054.2
052100     GO TO    MOVE-WRITE-F1-2.                                    NC1054.2
052200 MOVE-FAIL-F1-2.                                                  NC1054.2
052300     MOVE     MOVE5 TO COMPUTED-N.                                NC1054.2
052400     MOVE     23.45 TO CORRECT-N.                                 NC1054.2
052500     PERFORM  FAIL.                                               NC1054.2
052600 MOVE-WRITE-F1-2.                                                 NC1054.2
052700     MOVE "MOVE-TEST-F1-2" TO PAR-NAME.                           NC1054.2
052800     PERFORM  PRINT-DETAIL.                                       NC1054.2
052900 MOVE-TEST-F1-3-0.                                                NC1054.2
053000     MOVE "ABCDE" TO MOVE21.                                      NC1054.2
053100 MOVE-TEST-F1-3-1.                                                NC1054.2
053200     IF       MOVE21 EQUAL TO "ABCDE  "                           NC1054.2
053300              PERFORM PASS                                        NC1054.2
053400              ELSE                                                NC1054.2
053500              GO TO MOVE-FAIL-F1-3.                               NC1054.2
053600*    NOTE NON-NUMERIC LITERAL TO AN MOVE, SPACE PADDING ON RIGHT. NC1054.2
053700     GO TO    MOVE-WRITE-F1-3.                                    NC1054.2
053800 MOVE-DELETE-F1-3.                                                NC1054.2
053900     PERFORM  DE-LETE.                                            NC1054.2
054000     GO TO    MOVE-WRITE-F1-3.                                    NC1054.2
054100 MOVE-FAIL-F1-3.                                                  NC1054.2
054200     MOVE     MOVE21 TO COMPUTED-A.                               NC1054.2
054300     MOVE "ABCDE  " TO CORRECT-A.                                 NC1054.2
054400     PERFORM  FAIL.                                               NC1054.2
054500 MOVE-WRITE-F1-3.                                                 NC1054.2
054600     MOVE "MOVE-TEST-F1-3" TO PAR-NAME.                           NC1054.2
054700     PERFORM  PRINT-DETAIL.                                       NC1054.2
054800 MOVE-TEST-F1-4-0.                                                NC1054.2
054900     MOVE "ABCDE" TO MOVE20.                                      NC1054.2
055000 MOVE-TEST-F1-4-1.                                                NC1054.2
055100     IF       MOVE20 EQUAL TO "ABCD"                              NC1054.2
055200              PERFORM PASS                                        NC1054.2
055300              ELSE                                                NC1054.2
055400              GO TO MOVE-FAIL-F1-4.                               NC1054.2
055500*    NOTE NON-NUMERIC LITERAL TO AN MOVE, TRUNCATION ON RIGHT.    NC1054.2
055600     GO TO    MOVE-WRITE-F1-4.                                    NC1054.2
055700 MOVE-DELETE-F1-4.                                                NC1054.2
055800     PERFORM  DE-LETE.                                            NC1054.2
055900     GO TO    MOVE-WRITE-F1-4.                                    NC1054.2
056000 MOVE-FAIL-F1-4.                                                  NC1054.2
056100     MOVE     MOVE20 TO COMPUTED-A.                               NC1054.2
056200     MOVE "ABCD" TO CORRECT-A.                                    NC1054.2
056300     PERFORM  FAIL.                                               NC1054.2
056400 MOVE-WRITE-F1-4.                                                 NC1054.2
056500     MOVE "MOVE-TEST-F1-4" TO PAR-NAME.                           NC1054.2
056600     PERFORM  PRINT-DETAIL.                                       NC1054.2
056700 MOVE-INIT-F1-5.                                                  NC1054.2
056800     MOVE "MISC MOVE           " TO FEATURE.                      NC1054.2
056900     MOVE 12345 TO MOVE1.                                         NC1054.2
057000 MOVE-TEST-F1-5-0.                                                NC1054.2
057100     MOVE     MOVE1 TO TA--X.                                     NC1054.2
057200 MOVE-TEST-F1-5-1.                                                NC1054.2
057300     IF       TA--X EQUAL TO 12345                                NC1054.2
057400              PERFORM PASS                                        NC1054.2
057500              ELSE                                                NC1054.2
057600              GO TO MOVE-FAIL-F1-5.                               NC1054.2
057700*    NOTE NUMERIC LITERAL TO COMP, ZERO FILL ON LEFT.             NC1054.2
057800     GO TO    MOVE-WRITE-F1-5.                                    NC1054.2
057900 MOVE-DELETE-F1-5.                                                NC1054.2
058000     PERFORM  DE-LETE.                                            NC1054.2
058100     GO TO    MOVE-WRITE-F1-5.                                    NC1054.2
058200 MOVE-FAIL-F1-5.                                                  NC1054.2
058300     MOVE     TA--X TO COMPUTED-N.                                NC1054.2
058400     MOVE     12345 TO CORRECT-N.                                 NC1054.2
058500     PERFORM  FAIL.                                               NC1054.2
058600 MOVE-WRITE-F1-5.                                                 NC1054.2
058700     MOVE "MOVE-TEST-F1-5" TO PAR-NAME.                           NC1054.2
058800     PERFORM  PRINT-DETAIL.                                       NC1054.2
058900 MOVE-TEST-F1-6-0.                                                NC1054.2
059000     MOVE     SPACE TO MOVE20.                                    NC1054.2
059100 MOVE-TEST-F1-6-1.                                                NC1054.2
059200     IF       MOVE20 EQUAL TO "    "                              NC1054.2
059300              PERFORM PASS                                        NC1054.2
059400              ELSE                                                NC1054.2
059500              GO TO MOVE-FAIL-F1-6.                               NC1054.2
059600*    NOTE FIGURATIVE CONSTANT SPACE TO AN MOVE.                   NC1054.2
059700     GO TO    MOVE-WRITE-F1-6.                                    NC1054.2
059800 MOVE-DELETE-F1-6.                                                NC1054.2
059900     PERFORM  DE-LETE.                                            NC1054.2
060000     GO TO    MOVE-WRITE-F1-6.                                    NC1054.2
060100 MOVE-FAIL-F1-6.                                                  NC1054.2
060200     MOVE     MOVE20 TO COMPUTED-A.                               NC1054.2
060300     MOVE "    " TO CORRECT-A.                                    NC1054.2
060400     PERFORM  FAIL.                                               NC1054.2
060500 MOVE-WRITE-F1-6.                                                 NC1054.2
060600     MOVE "MOVE-TEST-F1-6" TO PAR-NAME.                           NC1054.2
060700     PERFORM  PRINT-DETAIL.                                       NC1054.2
060800 MOVE-TEST-F1-7-0.                                                NC1054.2
060900     MOVE     ZERO TO MOVE2.                                      NC1054.2
061000 MOVE-TEST-F1-7-1.                                                NC1054.2
061100     IF       MOVE2 EQUAL TO 00000                                NC1054.2
061200              PERFORM PASS                                        NC1054.2
061300              ELSE                                                NC1054.2
061400              GO TO MOVE-FAIL-F1-7.                               NC1054.2
061500*    NOTE FIGURATIVE CONSTANT ZERO TO N MOVE.                     NC1054.2
061600     GO TO    MOVE-WRITE-F1-7.                                    NC1054.2
061700 MOVE-DELETE-F1-7.                                                NC1054.2
061800     PERFORM  DE-LETE.                                            NC1054.2
061900     GO TO    MOVE-WRITE-F1-7.                                    NC1054.2
062000 MOVE-FAIL-F1-7.                                                  NC1054.2
062100     MOVE     MOVE2 TO COMPUTED-N.                                NC1054.2
062200     MOVE     00000 TO CORRECT-N.                                 NC1054.2
062300     PERFORM  FAIL.                                               NC1054.2
062400 MOVE-WRITE-F1-7.                                                 NC1054.2
062500     MOVE "MOVE-TEST-F1-7" TO PAR-NAME.                           NC1054.2
062600     PERFORM  PRINT-DETAIL.                                       NC1054.2
062700 MOVE-INIT-F1-8.                                                  NC1054.2
062800     MOVE    "ABCDE" TO MOVE32.                                   NC1054.2
062900 MOVE-TEST-F1-8-0.                                                NC1054.2
063000     MOVE     MOVE32 TO MOVE41.                                   NC1054.2
063100 MOVE-TEST-F1-8-1.                                                NC1054.2
063200     IF       MOVE41 EQUAL TO "  ABCDE"                           NC1054.2
063300              PERFORM PASS                                        NC1054.2
063400              ELSE                                                NC1054.2
063500              GO TO MOVE-FAIL-F1-8.                               NC1054.2
063600*    NOTE AN TO A MOVE, JUSTIFIED RIGHT.                          NC1054.2
063700     GO TO    MOVE-WRITE-F1-8.                                    NC1054.2
063800 MOVE-DELETE-F1-8.                                                NC1054.2
063900     PERFORM  DE-LETE.                                            NC1054.2
064000     GO TO    MOVE-WRITE-F1-8.                                    NC1054.2
064100 MOVE-FAIL-F1-8.                                                  NC1054.2
064200     MOVE     MOVE41 TO COMPUTED-A.                               NC1054.2
064300     MOVE "  ABCDE" TO CORRECT-A.                                 NC1054.2
064400     PERFORM  FAIL.                                               NC1054.2
064500 MOVE-WRITE-F1-8.                                                 NC1054.2
064600     MOVE "MOVE-TEST-F1-8" TO PAR-NAME.                           NC1054.2
064700     PERFORM  PRINT-DETAIL.                                       NC1054.2
064800 MOVE-INIT-F1-9.                                                  NC1054.2
064900     MOVE "GROUP MOVE          " TO FEATURE.                      NC1054.2
065000     MOVE     12345 TO MOVE1.                                     NC1054.2
065100 MOVE-TEST-F1-9-0.                                                NC1054.2
065200     MOVE     MOVE1 TO MOVE46.                                    NC1054.2
065300 MOVE-TEST-F1-9-1.                                                NC1054.2
065400     IF       MOVE46 EQUAL TO "12345 "                            NC1054.2
065500              PERFORM PASS                                        NC1054.2
065600              ELSE                                                NC1054.2
065700              GO TO MOVE-FAIL-F1-9.                               NC1054.2
065800*    NOTE NI TO GROUP MOVE.                                       NC1054.2
065900     GO TO    MOVE-WRITE-F1-9.                                    NC1054.2
066000 MOVE-DELETE-F1-9.                                                NC1054.2
066100     PERFORM  DE-LETE.                                            NC1054.2
066200     GO TO    MOVE-WRITE-F1-9.                                    NC1054.2
066300 MOVE-FAIL-F1-9.                                                  NC1054.2
066400     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
066500     MOVE "12345 " TO CORRECT-A.                                  NC1054.2
066600     PERFORM  FAIL.                                               NC1054.2
066700 MOVE-WRITE-F1-9.                                                 NC1054.2
066800     MOVE "MOVE-TEST-F1-9" TO PAR-NAME.                           NC1054.2
066900     PERFORM  PRINT-DETAIL.                                       NC1054.2
067000 MOVE-INIT-F1-10.                                                 NC1054.2
067100     MOVE     123.45 TO MOVE23.                                   NC1054.2
067200 MOVE-TEST-F1-10-0.                                               NC1054.2
067300     MOVE     MOVE23 TO MOVE46.                                   NC1054.2
067400 MOVE-TEST-F1-10-1.                                               NC1054.2
067500     IF       MOVE46 EQUAL TO "12345 "                            NC1054.2
067600              PERFORM PASS                                        NC1054.2
067700              ELSE                                                NC1054.2
067800              GO TO MOVE-FAIL-F1-10.                              NC1054.2
067900*    NOTE NNI TO GROUP MOVE.                                      NC1054.2
068000     GO TO    MOVE-WRITE-F1-10.                                   NC1054.2
068100 MOVE-DELETE-F1-10.                                               NC1054.2
068200     PERFORM  DE-LETE.                                            NC1054.2
068300     GO TO    MOVE-WRITE-F1-10.                                   NC1054.2
068400 MOVE-FAIL-F1-10.                                                 NC1054.2
068500     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
068600     MOVE "12345 " TO CORRECT-A.                                  NC1054.2
068700     PERFORM  FAIL.                                               NC1054.2
068800 MOVE-WRITE-F1-10.                                                NC1054.2
068900     MOVE "MOVE-TEST-F1-10" TO PAR-NAME.                          NC1054.2
069000     PERFORM  PRINT-DETAIL.                                       NC1054.2
069100 MOVE-INIT-F1-11.                                                 NC1054.2
069200     MOVE    "$123.45" TO MOVE29A.                                NC1054.2
069300 MOVE-TEST-F1-11-0.                                               NC1054.2
069400     MOVE     MOVE30 TO MOVE46.                                   NC1054.2
069500 MOVE-TEST-F1-11-1.                                               NC1054.2
069600     IF       MOVE46 EQUAL TO "$123.4"                            NC1054.2
069700              PERFORM PASS                                        NC1054.2
069800              ELSE                                                NC1054.2
069900              GO TO MOVE-FAIL-F1-11.                              NC1054.2
070000*    NOTE NE TO GROUP MOVE.                                       NC1054.2
070100     GO TO    MOVE-WRITE-F1-11.                                   NC1054.2
070200 MOVE-DELETE-F1-11.                                               NC1054.2
070300     PERFORM  DE-LETE.                                            NC1054.2
070400     GO TO    MOVE-WRITE-F1-11.                                   NC1054.2
070500 MOVE-FAIL-F1-11.                                                 NC1054.2
070600     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
070700     MOVE "$123.4" TO CORRECT-A.                                  NC1054.2
070800     PERFORM  FAIL.                                               NC1054.2
070900 MOVE-WRITE-F1-11.                                                NC1054.2
071000     MOVE "MOVE-TEST-F1-11" TO PAR-NAME.                          NC1054.2
071100     PERFORM  PRINT-DETAIL.                                       NC1054.2
071200 MOVE-INIT-F1-12.                                                 NC1054.2
071300     MOVE    "ABCDE" TO MOVE32.                                   NC1054.2
071400 MOVE-TEST-F1-12-0.                                               NC1054.2
071500     MOVE     MOVE32 TO MOVE46.                                   NC1054.2
071600 MOVE-TEST-F1-12-1.                                               NC1054.2
071700     IF       MOVE46 EQUAL TO "ABCDE "                            NC1054.2
071800              PERFORM PASS                                        NC1054.2
071900              ELSE                                                NC1054.2
072000              GO TO MOVE-FAIL-F1-12.                              NC1054.2
072100*    NOTE AN TO GROUP MOVE.                                       NC1054.2
072200     GO TO    MOVE-WRITE-F1-12.                                   NC1054.2
072300 MOVE-DELETE-F1-12.                                               NC1054.2
072400     PERFORM  DE-LETE.                                            NC1054.2
072500     GO TO    MOVE-WRITE-F1-12.                                   NC1054.2
072600 MOVE-FAIL-F1-12.                                                 NC1054.2
072700     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
072800     MOVE "ABCDE" TO CORRECT-A.                                   NC1054.2
072900     PERFORM  FAIL.                                               NC1054.2
073000 MOVE-WRITE-F1-12.                                                NC1054.2
073100     MOVE "MOVE-TEST-F1-12" TO PAR-NAME.                          NC1054.2
073200     PERFORM  PRINT-DETAIL.                                       NC1054.2
073300 MOVE-INIT-F1-13.                                                 NC1054.2
073400     MOVE    "1 A05" TO MOVE35A.                                  NC1054.2
073500 MOVE-TEST-F1-13-0.                                               NC1054.2
073600     MOVE     MOVE36 TO MOVE46.                                   NC1054.2
073700 MOVE-TEST-F1-13-1.                                               NC1054.2
073800     IF       MOVE46 EQUAL TO "1 A05 "                            NC1054.2
073900              PERFORM PASS                                        NC1054.2
074000              ELSE                                                NC1054.2
074100              GO TO MOVE-FAIL-F1-13.                              NC1054.2
074200*    NOTE AE TO GROUP MOVE.                                       NC1054.2
074300     GO TO    MOVE-WRITE-F1-13.                                   NC1054.2
074400 MOVE-DELETE-F1-13.                                               NC1054.2
074500     PERFORM  DE-LETE.                                            NC1054.2
074600     GO TO    MOVE-WRITE-F1-13.                                   NC1054.2
074700 MOVE-FAIL-F1-13.                                                 NC1054.2
074800     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
074900     MOVE "1 A05 " TO CORRECT-A.                                  NC1054.2
075000     PERFORM  FAIL.                                               NC1054.2
075100 MOVE-WRITE-F1-13.                                                NC1054.2
075200     MOVE "MOVE-TEST-F1-13" TO PAR-NAME.                          NC1054.2
075300     PERFORM  PRINT-DETAIL.                                       NC1054.2
075400 MOVE-INIT-F1-14.                                                 NC1054.2
075500     MOVE    "ABCDE" TO MOVE37.                                   NC1054.2
075600 MOVE-TEST-F1-14-0.                                               NC1054.2
075700     MOVE     MOVE37 TO MOVE46.                                   NC1054.2
075800 MOVE-TEST-F1-14-1.                                               NC1054.2
075900     IF       MOVE46 EQUAL TO "ABCDE "                            NC1054.2
076000              PERFORM PASS                                        NC1054.2
076100              ELSE                                                NC1054.2
076200              GO TO MOVE-FAIL-F1-14.                              NC1054.2
076300*    NOTE A TO GROUP MOVE.                                        NC1054.2
076400     GO TO    MOVE-WRITE-F1-14.                                   NC1054.2
076500 MOVE-DELETE-F1-14.                                               NC1054.2
076600     PERFORM  DE-LETE.                                            NC1054.2
076700     GO TO    MOVE-WRITE-F1-14.                                   NC1054.2
076800 MOVE-FAIL-F1-14.                                                 NC1054.2
076900     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
077000     MOVE "ABCDE " TO CORRECT-A.                                  NC1054.2
077100     PERFORM  FAIL.                                               NC1054.2
077200 MOVE-WRITE-F1-14.                                                NC1054.2
077300     MOVE "MOVE-TEST-F1-14" TO PAR-NAME.                          NC1054.2
077400     PERFORM  PRINT-DETAIL.                                       NC1054.2
077500 MOVE-INIT-F1-15.                                                 NC1054.2
077600     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
077700 MOVE-TEST-F1-15-0.                                               NC1054.2
077800     MOVE     MOVE43 TO MOVE46.                                   NC1054.2
077900 MOVE-TEST-F1-15-1.                                               NC1054.2
078000     IF       MOVE46 EQUAL TO "123ABC"                            NC1054.2
078100              PERFORM PASS                                        NC1054.2
078200              ELSE                                                NC1054.2
078300              GO TO MOVE-FAIL-F1-15.                              NC1054.2
078400*    NOTE GROUP TO GROUP MOVE.                                    NC1054.2
078500     GO TO    MOVE-WRITE-F1-15.                                   NC1054.2
078600 MOVE-DELETE-F1-15.                                               NC1054.2
078700     PERFORM  DE-LETE.                                            NC1054.2
078800     GO TO    MOVE-WRITE-F1-15.                                   NC1054.2
078900 MOVE-FAIL-F1-15.                                                 NC1054.2
079000     MOVE     MOVE46 TO COMPUTED-A.                               NC1054.2
079100     MOVE "123ABC" TO CORRECT-A.                                  NC1054.2
079200     PERFORM  FAIL.                                               NC1054.2
079300 MOVE-WRITE-F1-15.                                                NC1054.2
079400     MOVE "MOVE-TEST-F1-15" TO PAR-NAME.                          NC1054.2
079500     PERFORM  PRINT-DETAIL.                                       NC1054.2
079600 MOVE-INIT-F1-16.                                                 NC1054.2
079700     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
079800 MOVE-TEST-F1-16-0.                                               NC1054.2
079900     MOVE     MOVE43 TO MOVE3.                                    NC1054.2
080000 MOVE-TEST-F1-16-1.                                               NC1054.2
080100     IF       MOVE3 EQUAL TO 12                                   NC1054.2
080200              PERFORM PASS                                        NC1054.2
080300              ELSE                                                NC1054.2
080400              GO TO MOVE-FAIL-F1-16.                              NC1054.2
080500*    NOTE GROUP TO NI MOVE.                                       NC1054.2
080600     GO TO    MOVE-WRITE-F1-16.                                   NC1054.2
080700 MOVE-DELETE-F1-16.                                               NC1054.2
080800     PERFORM  DE-LETE.                                            NC1054.2
080900     GO TO    MOVE-WRITE-F1-16.                                   NC1054.2
081000 MOVE-FAIL-F1-16.                                                 NC1054.2
081100     MOVE     MOVE3 TO COMPUTED-N.                                NC1054.2
081200     MOVE     12 TO CORRECT-N.                                    NC1054.2
081300     PERFORM  FAIL.                                               NC1054.2
081400 MOVE-WRITE-F1-16.                                                NC1054.2
081500     MOVE "MOVE-TEST-F1-16" TO PAR-NAME.                          NC1054.2
081600     PERFORM  PRINT-DETAIL.                                       NC1054.2
081700 MOVE-INIT-F1-17.                                                 NC1054.2
081800     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
081900 MOVE-TEST-F1-17-0.                                               NC1054.2
082000     MOVE     MOVE43 TO MOVE29.                                   NC1054.2
082100 MOVE-TEST-F1-17-1.                                               NC1054.2
082200     IF       MOVE29X EQUAL TO "123ABC "                          NC1054.2
082300              PERFORM PASS                                        NC1054.2
082400              ELSE                                                NC1054.2
082500              GO TO MOVE-FAIL-F1-17.                              NC1054.2
082600*    NOTE GROUP TO NNI MOVE.                                      NC1054.2
082700     GO TO    MOVE-WRITE-F1-17.                                   NC1054.2
082800 MOVE-DELETE-F1-17.                                               NC1054.2
082900     PERFORM  DE-LETE.                                            NC1054.2
083000     GO TO    MOVE-WRITE-F1-17.                                   NC1054.2
083100 MOVE-FAIL-F1-17.                                                 NC1054.2
083200     MOVE     MOVE29X TO COMPUTED-A.                              NC1054.2
083300     MOVE "123ABC" TO CORRECT-A.                                  NC1054.2
083400     PERFORM  FAIL.                                               NC1054.2
083500 MOVE-WRITE-F1-17.                                                NC1054.2
083600     MOVE "MOVE-TEST-F1-17" TO PAR-NAME.                          NC1054.2
083700     PERFORM  PRINT-DETAIL.                                       NC1054.2
083800 MOVE-INIT-F1-18.                                                 NC1054.2
083900     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
084000 MOVE-TEST-F1-18-0.                                               NC1054.2
084100     MOVE     MOVE43 TO MOVE21.                                   NC1054.2
084200 MOVE-TEST-F1-18-1.                                               NC1054.2
084300     IF       MOVE21 EQUAL TO "123ABC "                           NC1054.2
084400              PERFORM PASS                                        NC1054.2
084500              ELSE                                                NC1054.2
084600              GO TO MOVE-FAIL-F1-18.                              NC1054.2
084700*    NOTE GROUP TO AN MOVE SPACE PADDING ON RIGHT.                NC1054.2
084800     GO TO    MOVE-WRITE-F1-18.                                   NC1054.2
084900 MOVE-DELETE-F1-18.                                               NC1054.2
085000     PERFORM  DE-LETE.                                            NC1054.2
085100     GO TO    MOVE-WRITE-F1-18.                                   NC1054.2
085200 MOVE-FAIL-F1-18.                                                 NC1054.2
085300     MOVE     MOVE21 TO COMPUTED-A.                               NC1054.2
085400     MOVE "123ABC" TO CORRECT-A.                                  NC1054.2
085500     PERFORM  FAIL.                                               NC1054.2
085600 MOVE-WRITE-F1-18.                                                NC1054.2
085700     MOVE "MOVE-TEST-F1-18" TO PAR-NAME.                          NC1054.2
085800     PERFORM  PRINT-DETAIL.                                       NC1054.2
085900 MOVE-INIT-F1-19.                                                 NC1054.2
086000     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
086100 MOVE-TEST-F1-19-0.                                               NC1054.2
086200     MOVE     MOVE43 TO MOVE20.                                   NC1054.2
086300 MOVE-TEST-F1-19-1.                                               NC1054.2
086400     IF       MOVE20 EQUAL TO "123A"                              NC1054.2
086500              PERFORM PASS                                        NC1054.2
086600              ELSE                                                NC1054.2
086700              GO TO MOVE-FAIL-F1-19.                              NC1054.2
086800*    NOTE GROUP TO AN MOVE.                                       NC1054.2
086900     GO TO    MOVE-WRITE-F1-19.                                   NC1054.2
087000 MOVE-DELETE-F1-19.                                               NC1054.2
087100     PERFORM  DE-LETE.                                            NC1054.2
087200     GO TO    MOVE-WRITE-F1-19.                                   NC1054.2
087300 MOVE-FAIL-F1-19.                                                 NC1054.2
087400     MOVE     MOVE20 TO COMPUTED-A.                               NC1054.2
087500     MOVE "123A" TO CORRECT-A.                                    NC1054.2
087600     PERFORM  FAIL.                                               NC1054.2
087700 MOVE-WRITE-F1-19.                                                NC1054.2
087800     MOVE "MOVE-TEST-F1-19" TO PAR-NAME.                          NC1054.2
087900     PERFORM  PRINT-DETAIL.                                       NC1054.2
088000 MOVE-INIT-F1-20.                                                 NC1054.2
088100     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
088200 MOVE-TEST-F1-20-0.                                               NC1054.2
088300     MOVE     MOVE43 TO MOVE39.                                   NC1054.2
088400 MOVE-TEST-F1-20-1.                                               NC1054.2
088500     IF       MOVE39 NOT EQUAL TO "123ABC "                       NC1054.2
088600              GO TO MOVE-FAIL-F1-20.                              NC1054.2
088700*    NOTE GROUP TO AE MOVE.                                       NC1054.2
088800     PERFORM PASS.                                                NC1054.2
088900     GO TO    MOVE-WRITE-F1-20.                                   NC1054.2
089000 MOVE-DELETE-F1-20.                                               NC1054.2
089100     PERFORM  DE-LETE.                                            NC1054.2
089200     GO TO    MOVE-WRITE-F1-20.                                   NC1054.2
089300 MOVE-FAIL-F1-20.                                                 NC1054.2
089400     MOVE     MOVE39 TO COMPUTED-A.                               NC1054.2
089500     MOVE "123ABC" TO CORRECT-A.                                  NC1054.2
089600     PERFORM  FAIL.                                               NC1054.2
089700 MOVE-WRITE-F1-20.                                                NC1054.2
089800     MOVE "MOVE-TEST-F1-20" TO PAR-NAME.                          NC1054.2
089900     PERFORM  PRINT-DETAIL.                                       NC1054.2
090000 MOVE-INIT-F1-21.                                                 NC1054.2
090100     MOVE    "123ABC" TO MOVE43.                                  NC1054.2
090200 MOVE-TEST-F1-21-0.                                               NC1054.2
090300     MOVE     MOVE43 TO MOVE35.                                   NC1054.2
090400 MOVE-TEST-F1-21-1.                                               NC1054.2
090500     IF       MOVE35 EQUAL TO "123"                               NC1054.2
090600              PERFORM PASS                                        NC1054.2
090700              ELSE                                                NC1054.2
090800              GO TO MOVE-FAIL-F1-21.                              NC1054.2
090900*    NOTE GROUP TO A MOVE.                                        NC1054.2
091000     GO TO    MOVE-WRITE-F1-21.                                   NC1054.2
091100 MOVE-DELETE-F1-21.                                               NC1054.2
091200     PERFORM  DE-LETE.                                            NC1054.2
091300     GO TO    MOVE-WRITE-F1-21.                                   NC1054.2
091400 MOVE-FAIL-F1-21.                                                 NC1054.2
091500     MOVE     MOVE35 TO COMPUTED-A.                               NC1054.2
091600     MOVE "123" TO CORRECT-A.                                     NC1054.2
091700     PERFORM  FAIL.                                               NC1054.2
091800 MOVE-WRITE-F1-21.                                                NC1054.2
091900     MOVE "MOVE-TEST-F1-21" TO PAR-NAME.                          NC1054.2
092000     PERFORM  PRINT-DETAIL.                                       NC1054.2
092100 MOVE-INIT-F1-22.                                                 NC1054.2
092200     MOVE "EDITED MOVE         " TO FEATURE.                      NC1054.2
092300     MOVE "12345" TO MOVE1.                                       NC1054.2
092400 MOVE-TEST-F1-22-0.                                               NC1054.2
092500     MOVE     MOVE1 TO MOVE16.                                    NC1054.2
092600 MOVE-TEST-F1-22-1.                                               NC1054.2
092700     IF       MOVE16 EQUAL TO "12345  "                           NC1054.2
092800              PERFORM PASS                                        NC1054.2
092900              ELSE                                                NC1054.2
093000              GO TO MOVE-FAIL-F1-22.                              NC1054.2
093100*    NOTE NI TO NE MOVE, REPORT SYMBOL CR.                        NC1054.2
093200     GO TO    MOVE-WRITE-F1-22.                                   NC1054.2
093300 MOVE-DELETE-F1-22.                                               NC1054.2
093400     PERFORM  DE-LETE.                                            NC1054.2
093500     GO TO    MOVE-WRITE-F1-22.                                   NC1054.2
093600 MOVE-FAIL-F1-22.                                                 NC1054.2
093700     MOVE     MOVE16 TO COMPUTED-A.                               NC1054.2
093800     MOVE "12345  " TO CORRECT-A.                                 NC1054.2
093900     PERFORM  FAIL.                                               NC1054.2
094000 MOVE-WRITE-F1-22.                                                NC1054.2
094100     MOVE "MOVE-TEST-F1-22" TO PAR-NAME.                          NC1054.2
094200     PERFORM  PRINT-DETAIL.                                       NC1054.2
094300 MOVE-INIT-F1-23.                                                 NC1054.2
094400     MOVE "12345" TO MOVE1.                                       NC1054.2
094500 MOVE-TEST-F1-23-0.                                               NC1054.2
094600     MOVE     MOVE1 TO MOVE52.                                    NC1054.2
094700 MOVE-TEST-F1-23-1.                                               NC1054.2
094800     IF       MOVE52 EQUAL TO "12345 "                            NC1054.2
094900              PERFORM PASS                                        NC1054.2
095000              ELSE                                                NC1054.2
095100              GO TO MOVE-FAIL-F1-23.                              NC1054.2
095200*    NOTE NI TO NE MOVE, REPORT SIGN -.                           NC1054.2
095300     GO TO    MOVE-WRITE-F1-23.                                   NC1054.2
095400 MOVE-DELETE-F1-23.                                               NC1054.2
095500     PERFORM  DE-LETE.                                            NC1054.2
095600     GO TO    MOVE-WRITE-F1-23.                                   NC1054.2
095700 MOVE-FAIL-F1-23.                                                 NC1054.2
095800     MOVE     MOVE52 TO COMPUTED-A.                               NC1054.2
095900     MOVE "12345 " TO CORRECT-A.                                  NC1054.2
096000     PERFORM  FAIL.                                               NC1054.2
096100 MOVE-WRITE-F1-23.                                                NC1054.2
096200     MOVE "MOVE-TEST-F1-23" TO PAR-NAME.                          NC1054.2
096300     PERFORM  PRINT-DETAIL.                                       NC1054.2
096400 MOVE-INIT-F1-24.                                                 NC1054.2
096500     MOVE -12345 TO MOVE51.                                       NC1054.2
096600 MOVE-TEST-F1-24-0.                                               NC1054.2
096700     MOVE     MOVE51 TO MOVE66.                                   NC1054.2
096800 MOVE-TEST-F1-24-1.                                               NC1054.2
096900     IF       MOVE66 EQUAL TO "12345DB"                           NC1054.2
097000              PERFORM PASS                                        NC1054.2
097100              ELSE                                                NC1054.2
097200              GO TO MOVE-FAIL-F1-24.                              NC1054.2
097300*    NOTE NI TO NE MOVE, REPORT SYMBOL DB.                        NC1054.2
097400     GO TO    MOVE-WRITE-F1-24.                                   NC1054.2
097500 MOVE-DELETE-F1-24.                                               NC1054.2
097600     PERFORM  DE-LETE.                                            NC1054.2
097700     GO TO    MOVE-WRITE-F1-24.                                   NC1054.2
097800 MOVE-FAIL-F1-24.                                                 NC1054.2
097900     MOVE     MOVE66 TO COMPUTED-A.                               NC1054.2
098000     MOVE "12345DB" TO CORRECT-A.                                 NC1054.2
098100     PERFORM  FAIL.                                               NC1054.2
098200 MOVE-WRITE-F1-24.                                                NC1054.2
098300     MOVE "MOVE-TEST-F1-24" TO PAR-NAME.                          NC1054.2
098400     PERFORM  PRINT-DETAIL.                                       NC1054.2
098500 MOVE-INIT-F1-25.                                                 NC1054.2
098600     MOVE  12345 TO MOVE1.                                        NC1054.2
098700 MOVE-TEST-F1-25-0.                                               NC1054.2
098800     MOVE     MOVE1 TO MOVE66.                                    NC1054.2
098900 MOVE-TEST-F1-25-1.                                               NC1054.2
099000     IF       MOVE66 EQUAL TO "12345  "                           NC1054.2
099100              PERFORM PASS                                        NC1054.2
099200              ELSE                                                NC1054.2
099300              GO TO MOVE-FAIL-F1-25.                              NC1054.2
099400*    NOTE NI TO NE MOVE, REPORT SYMBOL DB.                        NC1054.2
099500     GO TO    MOVE-WRITE-F1-25.                                   NC1054.2
099600 MOVE-DELETE-F1-25.                                               NC1054.2
099700     PERFORM  DE-LETE.                                            NC1054.2
099800     GO TO    MOVE-WRITE-F1-25.                                   NC1054.2
099900 MOVE-FAIL-F1-25.                                                 NC1054.2
100000     MOVE     MOVE66 TO COMPUTED-A.                               NC1054.2
100100     MOVE "12345  " TO CORRECT-A.                                 NC1054.2
100200     PERFORM  FAIL.                                               NC1054.2
100300 MOVE-WRITE-F1-25.                                                NC1054.2
100400     MOVE "MOVE-TEST-F1-25" TO PAR-NAME.                          NC1054.2
100500     PERFORM  PRINT-DETAIL.                                       NC1054.2
100600 MOVE-INIT-F1-26.                                                 NC1054.2
100700     MOVE -12345 TO MOVE51.                                       NC1054.2
100800 MOVE-TEST-F1-26-0.                                               NC1054.2
100900     MOVE     MOVE51 TO MOVE67.                                   NC1054.2
101000 MOVE-TEST-F1-26-1.                                               NC1054.2
101100     IF       MOVE67 EQUAL TO "12345-"                            NC1054.2
101200              PERFORM PASS                                        NC1054.2
101300              ELSE                                                NC1054.2
101400              GO TO MOVE-FAIL-F1-26.                              NC1054.2
101500*    NOTE NI TO NE MOVE, REPORT SIGN +.                           NC1054.2
101600     GO TO    MOVE-WRITE-F1-26.                                   NC1054.2
101700 MOVE-DELETE-F1-26.                                               NC1054.2
101800     PERFORM  DE-LETE.                                            NC1054.2
101900     GO TO    MOVE-WRITE-F1-26.                                   NC1054.2
102000 MOVE-FAIL-F1-26.                                                 NC1054.2
102100     MOVE     MOVE67 TO COMPUTED-A.                               NC1054.2
102200     MOVE "12345-" TO CORRECT-A.                                  NC1054.2
102300     PERFORM  FAIL.                                               NC1054.2
102400 MOVE-WRITE-F1-26.                                                NC1054.2
102500     MOVE "MOVE-TEST-F1-26" TO PAR-NAME.                          NC1054.2
102600     PERFORM  PRINT-DETAIL.                                       NC1054.2
102700 MOVE-INIT-F1-27.                                                 NC1054.2
102800     MOVE  12345 TO MOVE1.                                        NC1054.2
102900 MOVE-TEST-F1-27-0.                                               NC1054.2
103000     MOVE     MOVE1 TO MOVE67.                                    NC1054.2
103100 MOVE-TEST-F1-27-1.                                               NC1054.2
103200     IF       MOVE67 EQUAL TO "12345+"                            NC1054.2
103300              PERFORM PASS                                        NC1054.2
103400              ELSE                                                NC1054.2
103500              GO TO MOVE-FAIL-F1-27.                              NC1054.2
103600*    NOTE NI TO NE MOVE, REPORT SIGN +.                           NC1054.2
103700     GO TO    MOVE-WRITE-F1-27.                                   NC1054.2
103800 MOVE-DELETE-F1-27.                                               NC1054.2
103900     PERFORM  DE-LETE.                                            NC1054.2
104000     GO TO    MOVE-WRITE-F1-27.                                   NC1054.2
104100 MOVE-FAIL-F1-27.                                                 NC1054.2
104200     MOVE     MOVE67 TO COMPUTED-A.                               NC1054.2
104300     MOVE "12345+" TO CORRECT-A.                                  NC1054.2
104400     PERFORM  FAIL.                                               NC1054.2
104500 MOVE-WRITE-F1-27.                                                NC1054.2
104600     MOVE "MOVE-TEST-F1-27" TO PAR-NAME.                          NC1054.2
104700     PERFORM  PRINT-DETAIL.                                       NC1054.2
104800 MOVE-INIT-F1-28.                                                 NC1054.2
104900     MOVE     45 TO MOVE49.                                       NC1054.2
105000 MOVE-TEST-F1-28-0.                                               NC1054.2
105100     MOVE     MOVE49 TO MOVE68.                                   NC1054.2
105200 MOVE-TEST-F1-28-1.                                               NC1054.2
105300     IF       MOVE68 EQUAL TO "   +45"                            NC1054.2
105400              PERFORM PASS                                        NC1054.2
105500              ELSE                                                NC1054.2
105600              GO TO MOVE-FAIL-F1-28.                              NC1054.2
105700*    NOTE NI TO NE MOVE, FLOATING REPORT SIGN.                    NC1054.2
105800     GO TO    MOVE-WRITE-F1-28.                                   NC1054.2
105900 MOVE-DELETE-F1-28.                                               NC1054.2
106000     PERFORM  DE-LETE.                                            NC1054.2
106100     GO TO    MOVE-WRITE-F1-28.                                   NC1054.2
106200 MOVE-FAIL-F1-28.                                                 NC1054.2
106300     MOVE     MOVE68 TO COMPUTED-A.                               NC1054.2
106400     MOVE "   +45" TO CORRECT-A.                                  NC1054.2
106500     PERFORM  FAIL.                                               NC1054.2
106600 MOVE-WRITE-F1-28.                                                NC1054.2
106700     MOVE "MOVE-TEST-F1-28" TO PAR-NAME.                          NC1054.2
106800     PERFORM  PRINT-DETAIL.                                       NC1054.2
106900 MOVE-INIT-F1-29.                                                 NC1054.2
107000     MOVE    -45 TO MOVE51A.                                      NC1054.2
107100 MOVE-TEST-F1-29-0.                                               NC1054.2
107200     MOVE     MOVE51A TO MOVE69.                                  NC1054.2
107300 MOVE-TEST-F1-29-1.                                               NC1054.2
107400     IF       MOVE69 EQUAL TO "   -45"                            NC1054.2
107500              PERFORM PASS                                        NC1054.2
107600              ELSE                                                NC1054.2
107700              GO TO MOVE-FAIL-F1-29.                              NC1054.2
107800*    NOTE NI TO NE MOVE, FLOATING REPORT SIGN.                    NC1054.2
107900     GO TO    MOVE-WRITE-F1-29.                                   NC1054.2
108000 MOVE-DELETE-F1-29.                                               NC1054.2
108100     PERFORM  DE-LETE.                                            NC1054.2
108200     GO TO    MOVE-WRITE-F1-29.                                   NC1054.2
108300 MOVE-FAIL-F1-29.                                                 NC1054.2
108400     MOVE     MOVE69 TO COMPUTED-A.                               NC1054.2
108500     MOVE "   -45" TO CORRECT-A.                                  NC1054.2
108600     PERFORM  FAIL.                                               NC1054.2
108700 MOVE-WRITE-F1-29.                                                NC1054.2
108800     MOVE "MOVE-TEST-F1-29" TO PAR-NAME.                          NC1054.2
108900     PERFORM  PRINT-DETAIL.                                       NC1054.2
109000 MOVE-INIT-F1-30.                                                 NC1054.2
109100     MOVE  12345 TO MOVE1.                                        NC1054.2
109200 MOVE-TEST-F1-30-0.                                               NC1054.2
109300     MOVE     MOVE1 TO MOVE70.                                    NC1054.2
109400 MOVE-TEST-F1-30-1.                                               NC1054.2
109500     IF       MOVE70 EQUAL TO 12345                               NC1054.2
109600              PERFORM PASS                                        NC1054.2
109700              ELSE                                                NC1054.2
109800              GO TO MOVE-FAIL-F1-30.                              NC1054.2
109900*    NOTE, TO AUDIT SYNC OPTION.                                  NC1054.2
110000     GO TO    MOVE-WRITE-F1-30.                                   NC1054.2
110100 MOVE-DELETE-F1-30.                                               NC1054.2
110200     PERFORM  DE-LETE.                                            NC1054.2
110300     GO TO    MOVE-WRITE-F1-30.                                   NC1054.2
110400 MOVE-FAIL-F1-30.                                                 NC1054.2
110500     MOVE     MOVE70 TO COMPUTED-N.                               NC1054.2
110600     MOVE     12345 TO CORRECT-N.                                 NC1054.2
110700     PERFORM  FAIL.                                               NC1054.2
110800 MOVE-WRITE-F1-30.                                                NC1054.2
110900     MOVE "MISC MOVE           " TO FEATURE.                      NC1054.2
111000     MOVE "MOVE-TEST-F1-30" TO PAR-NAME.                          NC1054.2
111100     PERFORM  PRINT-DETAIL.                                       NC1054.2
111200 MOVE-INIT-F1-31.                                                 NC1054.2
111300                                                                  NC1054.2
111400 MOVE-TEST-F1-31-0.                                               NC1054.2
111500     MOVE     1.11115111115111115 TO MOVE48.                      NC1054.2
111600 MOVE-TEST-F1-31-1.                                               NC1054.2
111700     IF       MOVE48 EQUAL TO 1.11115111115111115                 NC1054.2
111800              PERFORM PASS                                        NC1054.2
111900              ELSE                                                NC1054.2
112000              GO TO MOVE-FAIL-F1-31.                              NC1054.2
112100*    NOTE MAXIMUM LENGTH NUMERIC LITERAL.                         NC1054.2
112200     GO TO    MOVE-WRITE-F1-31.                                   NC1054.2
112300 MOVE-DELETE-F1-31.                                               NC1054.2
112400     PERFORM  DE-LETE.                                            NC1054.2
112500     GO TO    MOVE-WRITE-F1-31.                                   NC1054.2
112600 MOVE-FAIL-F1-31.                                                 NC1054.2
112700     MOVE     MOVE48 TO COMPUTED-N.                               NC1054.2
112800     MOVE "1.11115111115111115" TO CORRECT-A.                     NC1054.2
112900     PERFORM  FAIL.                                               NC1054.2
113000 MOVE-WRITE-F1-31.                                                NC1054.2
113100     MOVE "MAXIMUM LENGTH MOVE " TO FEATURE.                      NC1054.2
113200     MOVE "MOVE-TEST-F1-31" TO PAR-NAME.                          NC1054.2
113300     PERFORM  PRINT-DETAIL.                                       NC1054.2
113400 MOVE-INIT-F1-32.                                                 NC1054.2
113500              MOVE 0 TO TA--X.                                    NC1054.2
113600 MOVE-TEST-F1-32-0.                                               NC1054.2
113700     MOVE     MOVE23 TO MOVE5 MOVE6 MOVE7.                        NC1054.2
113800 MOVE-TEST-F1-32-1.                                               NC1054.2
113900     IF       MOVE5 NOT EQUAL TO 23.45                            NC1054.2
114000              MOVE MOVE5 TO COMPUTED-N                            NC1054.2
114100              MOVE 23.45 TO CORRECT-N                             NC1054.2
114200              PERFORM FAIL PERFORM MOVE-WRITE-F1-32               NC1054.2
114300              MOVE 1 TO TA--X.                                    NC1054.2
114400     IF       MOVE6 NOT EQUAL TO .45                              NC1054.2
114500              MOVE MOVE6 TO COMPUTED-N                            NC1054.2
114600              MOVE .45 TO CORRECT-N                               NC1054.2
114700              PERFORM FAIL PERFORM MOVE-WRITE-F1-32               NC1054.2
114800              MOVE 1 TO TA--X.                                    NC1054.2
114900     IF       MOVE7 NOT EQUAL TO 3.45                             NC1054.2
115000              MOVE MOVE7 TO COMPUTED-N                            NC1054.2
115100              MOVE 3.45 TO CORRECT-N                              NC1054.2
115200              GO TO MOVE-FAIL-F1-32.                              NC1054.2
115300     IF TA--X IS NOT EQUAL TO 0 GO TO MOVE-INIT-F1-33.            NC1054.2
115400     PERFORM  PASS.                                               NC1054.2
115500     GO TO    MOVE-WRITE-F1-32.                                   NC1054.2
115600 MOVE-DELETE-F1-32.                                               NC1054.2
115700     PERFORM  DE-LETE.                                            NC1054.2
115800     GO TO    MOVE-WRITE-F1-32.                                   NC1054.2
115900 MOVE-FAIL-F1-32.                                                 NC1054.2
116000     PERFORM  FAIL.                                               NC1054.2
116100 MOVE-WRITE-F1-32.                                                NC1054.2
116200     MOVE "MOVE-TEST-F1-32" TO PAR-NAME.                          NC1054.2
116300     PERFORM  PRINT-DETAIL.                                       NC1054.2
116400 MOVE-INIT-F1-33.                                                 NC1054.2
116500     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO GRP-ALPHABETIC.      NC1054.2
116600 MOVE-TEST-F1-33-0.                                               NC1054.2
116700     MOVE GRP-GROUP-MOVE-FROM TO GRP-GROUP-MOVE-TO.               NC1054.2
116800 MOVE-TEST-F1-33-1.                                               NC1054.2
116900     IF ALPHABET-AN-00026 NOT EQUAL TO WRK-AN-00026               NC1054.2
117000              GO TO MOVE-FAIL-F1-33.                              NC1054.2
117100     IF DIGITS-DU-10V00 NOT EQUAL TO WRK-DU-10V00                 NC1054.2
117200              GO TO MOVE-FAIL-F1-33.                              NC1054.2
117300     IF ALPHANUMERIC-XN-00049 NOT EQUAL TO WRK-XN-00049           NC1054.2
117400              GO TO MOVE-FAIL-F1-33.                              NC1054.2
117500     IF NE-0001 NOT EQUAL TO SPACE GO TO MOVE-FAIL-F1-33.         NC1054.2
117600     IF NE-0002 NOT EQUAL TO SPACE GO TO MOVE-FAIL-F1-33.         NC1054.2
117700     IF AE-0001 NOT EQUAL TO SPACE GO TO MOVE-FAIL-F1-33.         NC1054.2
117800     IF AE-0002 EQUAL TO SPACE                                    NC1054.2
117900         PERFORM PASS                                             NC1054.2
118000         GO TO MOVE-WRITE-F1-33.                                  NC1054.2
118100     GO TO MOVE-FAIL-F1-33.                                       NC1054.2
118200 MOVE-DELETE-F1-33.                                               NC1054.2
118300     PERFORM DE-LETE.                                             NC1054.2
118400     GO TO MOVE-WRITE-F1-33.                                      NC1054.2
118500 MOVE-FAIL-F1-33.                                                 NC1054.2
118600     MOVE GRP-MOVE-CONSTANTS TO SEND-BREAKDOWN.                   NC1054.2
118700     MOVE GRP-MOVE-RECEIVING-FIELDS TO RECEIVE-BREAKDOWN.         NC1054.2
118800     MOVE 119 TO LENGTH-COUNTER.                                  NC1054.2
118900     PERFORM FAIL.                                                NC1054.2
119000     PERFORM A20 THRU A120.                                       NC1054.2
119100 MOVE-WRITE-F1-33.                                                NC1054.2
119200     MOVE "MOVE ALPHA GROUP    " TO FEATURE.                      NC1054.2
119300     MOVE "MOVE-TEST-F1-33 " TO PAR-NAME.                         NC1054.2
119400     PERFORM PRINT-DETAIL.                                        NC1054.2
119500 MOVE-INIT-F1-34.                                                 NC1054.2
119600     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO GRP-ALPHABETIC.      NC1054.2
119700 MOVE-TEST-F1-34-0.                                               NC1054.2
119800     MOVE GRP-ALPHABETIC TO WRK-AN-00026.                         NC1054.2
119900 MOVE-TEST-F1-34-1.                                               NC1054.2
120000     IF GRP-ALPHABETIC EQUAL TO GRP-WRK-AN-00026                  NC1054.2
120100         PERFORM PASS                                             NC1054.2
120200         GO TO MOVE-WRITE-F1-34.                                  NC1054.2
120300     GO TO MOVE-FAIL-F1-34.                                       NC1054.2
120400 MOVE-DELETE-F1-34.                                               NC1054.2
120500     PERFORM DE-LETE.                                             NC1054.2
120600     GO TO MOVE-WRITE-F1-34.                                      NC1054.2
120700 MOVE-FAIL-F1-34.                                                 NC1054.2
120800     MOVE GRP-ALPHABETIC TO SEND-BREAKDOWN.                       NC1054.2
120900     MOVE GRP-WRK-AN-00026 TO RECEIVE-BREAKDOWN.                  NC1054.2
121000     MOVE 026 TO LENGTH-COUNTER.                                  NC1054.2
121100     PERFORM FAIL.                                                NC1054.2
121200     PERFORM A20 THRU A40.                                        NC1054.2
121300 MOVE-WRITE-F1-34.                                                NC1054.2
121400     MOVE "MOVE-TEST-F1-34 " TO PAR-NAME.                         NC1054.2
121500     PERFORM PRINT-DETAIL.                                        NC1054.2
121600 MOVE-INIT-F1-35.                                                 NC1054.2
121700     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
121800           TO GRP-ALPHANUMERIC.                                   NC1054.2
121900     MOVE "MOVE ALPHA-NUM GROUP" TO FEATURE.                      NC1054.2
122000 MOVE-TEST-F1-35-0.                                               NC1054.2
122100     MOVE GRP-ALPHANUMERIC TO WRK-XN-00049.                       NC1054.2
122200 MOVE-TEST-F1-35-1.                                               NC1054.2
122300     IF GRP-ALPHANUMERIC EQUAL TO GRP-WRK-XN-00049                NC1054.2
122400         PERFORM PASS                                             NC1054.2
122500         GO TO MOVE-WRITE-F1-35.                                  NC1054.2
122600     GO TO MOVE-FAIL-F1-35.                                       NC1054.2
122700 MOVE-DELETE-F1-35.                                               NC1054.2
122800     PERFORM DE-LETE.                                             NC1054.2
122900     GO TO MOVE-WRITE-F1-35.                                      NC1054.2
123000 MOVE-FAIL-F1-35.                                                 NC1054.2
123100     MOVE GRP-ALPHANUMERIC TO SEND-BREAKDOWN.                     NC1054.2
123200     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
123300     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
123400     PERFORM FAIL.                                                NC1054.2
123500     PERFORM A20 THRU A60.                                        NC1054.2
123600 MOVE-WRITE-F1-35.                                                NC1054.2
123700     MOVE "MOVE-TEST-F1-35 " TO PAR-NAME.                         NC1054.2
123800     PERFORM PRINT-DETAIL.                                        NC1054.2
123900 MOVE-INIT-F1-36.                                                 NC1054.2
124000     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
124100           TO GRP-ALPHANUMERIC.                                   NC1054.2
124200     MOVE "MOVE ALPHA-NUM GROUP" TO FEATURE.                      NC1054.2
124300 MOVE-TEST-F1-36-0.                                               NC1054.2
124400     MOVE GRP-ALPHANUMERIC TO AE-0001.                            NC1054.2
124500 MOVE-TEST-F1-36-1.                                               NC1054.2
124600     IF       GRP-ALPHANUMERIC-1002 EQUAL TO GRP-AE-0001          NC1054.2
124700         PERFORM PASS                                             NC1054.2
124800         GO TO MOVE-WRITE-F1-36.                                  NC1054.2
124900     GO TO MOVE-FAIL-F1-36.                                       NC1054.2
125000 MOVE-DELETE-F1-36.                                               NC1054.2
125100     PERFORM DE-LETE.                                             NC1054.2
125200     GO TO MOVE-WRITE-F1-36.                                      NC1054.2
125300 MOVE-FAIL-F1-36.                                                 NC1054.2
125400     MOVE     GRP-ALPHANUMERIC-1002 TO SEND-BREAKDOWN.            NC1054.2
125500     MOVE     GRP-AE-0001 TO RECEIVE-BREAKDOWN.                   NC1054.2
125600     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
125700     PERFORM FAIL.                                                NC1054.2
125800     PERFORM A20 THRU A60.                                        NC1054.2
125900 MOVE-WRITE-F1-36.                                                NC1054.2
126000     MOVE "MOVE-TEST-F1-36 " TO PAR-NAME.                         NC1054.2
126100     PERFORM PRINT-DETAIL.                                        NC1054.2
126200 MOVE-INIT-F1-37.                                                 NC1054.2
126300     MOVE "MOVE NUMERIC GROUP  " TO FEATURE.                      NC1054.2
126400     MOVE  0123456789 TO GRP-NUMERIC.                             NC1054.2
126500 MOVE-TEST-F1-37-0.                                               NC1054.2
126600     MOVE GRP-NUMERIC TO WRK-DU-10V00.                            NC1054.2
126700 MOVE-TEST-F1-37-1.                                               NC1054.2
126800     IF GRP-NUMERIC EQUAL TO GRP-WRK-DU-10V00                     NC1054.2
126900         PERFORM PASS                                             NC1054.2
127000         GO TO MOVE-WRITE-F1-37.                                  NC1054.2
127100     GO TO MOVE-FAIL-F1-37.                                       NC1054.2
127200 MOVE-DELETE-F1-37.                                               NC1054.2
127300     PERFORM DE-LETE.                                             NC1054.2
127400     GO TO MOVE-WRITE-F1-37.                                      NC1054.2
127500 MOVE-FAIL-F1-37.                                                 NC1054.2
127600     MOVE GRP-NUMERIC TO CORRECT-A.                               NC1054.2
127700     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
127800     PERFORM FAIL.                                                NC1054.2
127900 MOVE-WRITE-F1-37.                                                NC1054.2
128000     MOVE "MOVE-TEST-F1-37 " TO PAR-NAME.                         NC1054.2
128100     PERFORM PRINT-DETAIL.                                        NC1054.2
128200 MOVE-INIT-F1-38.                                                 NC1054.2
128300     MOVE  0123456789 TO GRP-NUMERIC.                             NC1054.2
128400 MOVE-TEST-F1-38-0.                                               NC1054.2
128500     MOVE GRP-NUMERIC TO NE-0001.                                 NC1054.2
128600 MOVE-TEST-F1-38-1.                                               NC1054.2
128700     IF "0123456789   " EQUAL TO GRP-NE-0001                      NC1054.2
128800         PERFORM PASS                                             NC1054.2
128900         GO TO MOVE-WRITE-F1-38.                                  NC1054.2
129000     GO TO MOVE-FAIL-F1-38.                                       NC1054.2
129100 MOVE-DELETE-F1-38.                                               NC1054.2
129200     PERFORM DE-LETE.                                             NC1054.2
129300     GO TO MOVE-WRITE-F1-38.                                      NC1054.2
129400 MOVE-FAIL-F1-38.                                                 NC1054.2
129500     MOVE GRP-NUMERIC TO CORRECT-A.                               NC1054.2
129600     MOVE GRP-NE-0001 TO COMPUTED-A.                              NC1054.2
129700     PERFORM FAIL.                                                NC1054.2
129800 MOVE-WRITE-F1-38.                                                NC1054.2
129900     MOVE "MOVE-TEST-F1-38 " TO PAR-NAME.                         NC1054.2
130000     PERFORM PRINT-DETAIL.                                        NC1054.2
130100 MOVE-INIT-F1-39.                                                 NC1054.2
130200     MOVE "MOVE ALPHA ITEM     " TO FEATURE.                      NC1054.2
130300     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO ALPHABET-AN-00026.      NC1054.2
130400 MOVE-TEST-F1-39-0.                                               NC1054.2
130500     MOVE ALPHABET-AN-00026 TO GRP-WRK-AN-00026.                  NC1054.2
130600 MOVE-TEST-F1-39-1.                                               NC1054.2
130700     IF ALPHABET-AN-00026 EQUAL TO WRK-AN-00026                   NC1054.2
130800         PERFORM PASS                                             NC1054.2
130900         GO TO MOVE-WRITE-F1-39.                                  NC1054.2
131000     GO TO MOVE-FAIL-F1-39.                                       NC1054.2
131100 MOVE-DELETE-F1-39.                                               NC1054.2
131200     PERFORM DE-LETE.                                             NC1054.2
131300     GO TO MOVE-WRITE-F1-39.                                      NC1054.2
131400 MOVE-FAIL-F1-39.                                                 NC1054.2
131500     MOVE ALPHABET-AN-00026 TO SEND-BREAKDOWN.                    NC1054.2
131600     MOVE GRP-WRK-AN-00026 TO RECEIVE-BREAKDOWN.                  NC1054.2
131700     MOVE 026 TO LENGTH-COUNTER.                                  NC1054.2
131800     PERFORM FAIL.                                                NC1054.2
131900     PERFORM A20 THRU A40.                                        NC1054.2
132000 MOVE-WRITE-F1-39.                                                NC1054.2
132100     MOVE "MOVE-TEST-F1-39 " TO PAR-NAME.                         NC1054.2
132200     PERFORM PRINT-DETAIL.                                        NC1054.2
132300 MOVE-INIT-F1-40.                                                 NC1054.2
132400     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO ALPHABET-AN-00026.      NC1054.2
132500 MOVE-TEST-F1-40-0.                                               NC1054.2
132600     MOVE ALPHABET-AN-00026 TO WRK-AN-00026.                      NC1054.2
132700 MOVE-TEST-F1-40-1.                                               NC1054.2
132800     IF ALPHABET-AN-00026 EQUAL TO GRP-WRK-AN-00026               NC1054.2
132900         PERFORM PASS                                             NC1054.2
133000         GO TO MOVE-WRITE-F1-40.                                  NC1054.2
133100     GO TO MOVE-FAIL-F1-40.                                       NC1054.2
133200 MOVE-DELETE-F1-40.                                               NC1054.2
133300     PERFORM DE-LETE.                                             NC1054.2
133400     GO TO MOVE-WRITE-F1-40.                                      NC1054.2
133500 MOVE-FAIL-F1-40.                                                 NC1054.2
133600     MOVE ALPHABET-AN-00026 TO SEND-BREAKDOWN.                    NC1054.2
133700     MOVE WRK-AN-00026 TO RECEIVE-BREAKDOWN.                      NC1054.2
133800     MOVE 026 TO LENGTH-COUNTER.                                  NC1054.2
133900     PERFORM FAIL.                                                NC1054.2
134000     PERFORM A20 THRU A40.                                        NC1054.2
134100 MOVE-WRITE-F1-40.                                                NC1054.2
134200     MOVE "MOVE-TEST-F1-40 " TO PAR-NAME.                         NC1054.2
134300     PERFORM PRINT-DETAIL.                                        NC1054.2
134400 MOVE-INIT-F1-41.                                                 NC1054.2
134500     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO ALPHABET-AN-00026.      NC1054.2
134600 MOVE-TEST-F1-41-0.                                               NC1054.2
134700     MOVE ALPHABET-AN-00026 TO WRK-XN-00049  FIRST-26.            NC1054.2
134800 MOVE-TEST-F1-41-1.                                               NC1054.2
134900     MOVE SPACE TO PADD-REST.                                     NC1054.2
135000     IF FORTY-NINE-COMPARE EQUAL TO GRP-WRK-XN-00049              NC1054.2
135100         PERFORM PASS                                             NC1054.2
135200         GO TO MOVE-WRITE-F1-41.                                  NC1054.2
135300     GO TO MOVE-FAIL-F1-41.                                       NC1054.2
135400 MOVE-DELETE-F1-41.                                               NC1054.2
135500     PERFORM DE-LETE.                                             NC1054.2
135600     GO TO MOVE-WRITE-F1-41.                                      NC1054.2
135700 MOVE-FAIL-F1-41.                                                 NC1054.2
135800     MOVE FORTY-NINE-COMPARE TO SEND-BREAKDOWN.                   NC1054.2
135900     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
136000     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
136100     PERFORM FAIL.                                                NC1054.2
136200     PERFORM A20 THRU A60.                                        NC1054.2
136300 MOVE-WRITE-F1-41.                                                NC1054.2
136400     MOVE "MOVE-TEST-F1-41 " TO PAR-NAME.                         NC1054.2
136500     PERFORM PRINT-DETAIL.                                        NC1054.2
136600 MOVE-INIT-F1-42.                                                 NC1054.2
136700     MOVE "MOVE ALPHA-NUM ITEM " TO FEATURE.                      NC1054.2
136800     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
136900           TO ALPHANUMERIC-XN-00049.                              NC1054.2
137000 MOVE-TEST-F1-42-0.                                               NC1054.2
137100     MOVE ALPHANUMERIC-XN-00049 TO GRP-WRK-XN-00049.              NC1054.2
137200 MOVE-TEST-F1-42-1.                                               NC1054.2
137300     IF ALPHANUMERIC-XN-00049 EQUAL TO WRK-XN-00049               NC1054.2
137400         PERFORM PASS                                             NC1054.2
137500         GO TO MOVE-WRITE-F1-42.                                  NC1054.2
137600     GO TO MOVE-FAIL-F1-42.                                       NC1054.2
137700 MOVE-DELETE-F1-42.                                               NC1054.2
137800     PERFORM DE-LETE.                                             NC1054.2
137900     GO TO MOVE-WRITE-F1-42.                                      NC1054.2
138000 MOVE-FAIL-F1-42.                                                 NC1054.2
138100     MOVE ALPHANUMERIC-XN-00049 TO SEND-BREAKDOWN.                NC1054.2
138200     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
138300     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
138400     PERFORM FAIL.                                                NC1054.2
138500     PERFORM A20 THRU A60.                                        NC1054.2
138600 MOVE-WRITE-F1-42.                                                NC1054.2
138700     MOVE "MOVE-TEST-F1-42 " TO PAR-NAME.                         NC1054.2
138800     PERFORM PRINT-DETAIL.                                        NC1054.2
138900 MOVE-INIT-F1-43.                                                 NC1054.2
139000     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
139100           TO ALPHANUMERIC-XN-00049.                              NC1054.2
139200 MOVE-TEST-F1-43-0.                                               NC1054.2
139300     MOVE ALPHANUMERIC-XN-00049 TO WRK-AN-00026                   NC1054.2
139400                                   FORTY-NINE-COMPARE.            NC1054.2
139500     MOVE SPACE TO PADD-REST.                                     NC1054.2
139600 MOVE-TEST-F1-43-1.                                               NC1054.2
139700     IF FIRST-26 EQUAL TO GRP-WRK-AN-00026                        NC1054.2
139800         PERFORM PASS                                             NC1054.2
139900         GO TO MOVE-WRITE-F1-43.                                  NC1054.2
140000     GO TO MOVE-FAIL-F1-43.                                       NC1054.2
140100 MOVE-DELETE-F1-43.                                               NC1054.2
140200     PERFORM DE-LETE.                                             NC1054.2
140300     GO TO MOVE-WRITE-F1-43.                                      NC1054.2
140400 MOVE-FAIL-F1-43.                                                 NC1054.2
140500     MOVE GRP-WRK-AN-00026 TO RECEIVE-BREAKDOWN.                  NC1054.2
140600     MOVE FIRST-26 TO SEND-BREAKDOWN.                             NC1054.2
140700     MOVE 026 TO LENGTH-COUNTER.                                  NC1054.2
140800     PERFORM FAIL.                                                NC1054.2
140900     PERFORM A20 THRU A40.                                        NC1054.2
141000 MOVE-WRITE-F1-43.                                                NC1054.2
141100     MOVE "MOVE-TEST-F1-43 " TO PAR-NAME.                         NC1054.2
141200     PERFORM PRINT-DETAIL.                                        NC1054.2
141300 MOVE-INIT-F1-44.                                                 NC1054.2
141400     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
141500           TO ALPHANUMERIC-XN-00049.                              NC1054.2
141600 MOVE-TEST-F1-44-0.                                               NC1054.2
141700     MOVE ALPHANUMERIC-XN-00049 TO WRK-XN-00049.                  NC1054.2
141800 MOVE-TEST-F1-44-1.                                               NC1054.2
141900     IF ALPHANUMERIC-XN-00049 EQUAL TO GRP-WRK-XN-00049           NC1054.2
142000         PERFORM PASS                                             NC1054.2
142100         GO TO MOVE-WRITE-F1-44.                                  NC1054.2
142200     GO TO MOVE-FAIL-F1-44.                                       NC1054.2
142300 MOVE-DELETE-F1-44.                                               NC1054.2
142400     PERFORM DE-LETE.                                             NC1054.2
142500     GO TO MOVE-WRITE-F1-44.                                      NC1054.2
142600 MOVE-FAIL-F1-44.                                                 NC1054.2
142700     MOVE ALPHANUMERIC-XN-00049 TO SEND-BREAKDOWN.                NC1054.2
142800     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
142900     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
143000     PERFORM FAIL.                                                NC1054.2
143100     PERFORM A20 THRU A60.                                        NC1054.2
143200 MOVE-WRITE-F1-44.                                                NC1054.2
143300     MOVE "MOVE-TEST-F1-44" TO PAR-NAME.                          NC1054.2
143400     PERFORM PRINT-DETAIL.                                        NC1054.2
143500 MOVE-INIT-F1-45.                                                 NC1054.2
143600     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
143700           TO ALPHANUMERIC-XN-00049.                              NC1054.2
143800 MOVE-TEST-F1-45-0.                                               NC1054.2
143900     MOVE ALPHANUMERIC-XN-00049 TO AE-0001.                       NC1054.2
144000 MOVE-TEST-F1-45-1.                                               NC1054.2
144100     IF GRP-AE-0001 EQUAL TO "ABCDEFGHIJKLMNOPQRSTUVWXYZ +-  =$, .NC1054.2
144200-    "()/0 012345678"                                             NC1054.2
144300         PERFORM PASS                                             NC1054.2
144400         GO TO MOVE-WRITE-F1-45.                                  NC1054.2
144500     GO TO MOVE-FAIL-F1-45.                                       NC1054.2
144600 MOVE-DELETE-F1-45.                                               NC1054.2
144700     PERFORM DE-LETE.                                             NC1054.2
144800     GO TO MOVE-WRITE-F1-45.                                      NC1054.2
144900 MOVE-FAIL-F1-45.                                                 NC1054.2
145000     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ +-  =$, .()/0 012345678"    NC1054.2
145100         TO SEND-BREAKDOWN.                                       NC1054.2
145200     MOVE AE-0001 TO RECEIVE-BREAKDOWN.                           NC1054.2
145300     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
145400     PERFORM FAIL.                                                NC1054.2
145500     PERFORM A20 THRU A60.                                        NC1054.2
145600 MOVE-WRITE-F1-45.                                                NC1054.2
145700     MOVE "MOVE-TEST-F1-45" TO PAR-NAME.                          NC1054.2
145800     PERFORM PRINT-DETAIL.                                        NC1054.2
145900 MOVE-INIT-F1-46.                                                 NC1054.2
146000                                                                  NC1054.2
146100 MOVE-TEST-F1-46-0.                                               NC1054.2
146200     MOVE     "4444444444444444440123456789" TO WRK-DU-10V00.     NC1054.2
146300 MOVE-TEST-F1-46-1.                                               NC1054.2
146400     IF GRP-WRK-DU-10V00 EQUAL TO "0123456789"                    NC1054.2
146500         PERFORM PASS                                             NC1054.2
146600         GO TO MOVE-WRITE-F1-46.                                  NC1054.2
146700     GO TO MOVE-FAIL-F1-46.                                       NC1054.2
146800 MOVE-DELETE-F1-46.                                               NC1054.2
146900     PERFORM DE-LETE.                                             NC1054.2
147000     GO TO MOVE-WRITE-F1-46.                                      NC1054.2
147100 MOVE-FAIL-F1-46.                                                 NC1054.2
147200     MOVE "0123456789" TO CORRECT-A.                              NC1054.2
147300     MOVE WRK-DU-10V00 TO COMPUTED-A.                             NC1054.2
147400     PERFORM FAIL.                                                NC1054.2
147500 MOVE-WRITE-F1-46.                                                NC1054.2
147600     MOVE "MOVE-TEST-F1-46" TO PAR-NAME.                          NC1054.2
147700     PERFORM PRINT-DETAIL.                                        NC1054.2
147800 MOVE-INIT-F1-47.                                                 NC1054.2
147900     MOVE    3344556677 TO MOVE72.                                NC1054.2
148000 MOVE-TEST-F1-47-0.                                               NC1054.2
148100     MOVE    MOVE72 TO MOVE73.                                    NC1054.2
148200 MOVE-TEST-F1-47-1.                                               NC1054.2
148300     IF      MOVE73 EQUAL TO "33445 56677     0 "                 NC1054.2
148400             PERFORM PASS                                         NC1054.2
148500             GO TO MOVE-WRITE-F1-47.                              NC1054.2
148600     GO TO   MOVE-FAIL-F1-47.                                     NC1054.2
148700 MOVE-DELETE-F1-47.                                               NC1054.2
148800     PERFORM DE-LETE.                                             NC1054.2
148900     GO TO   MOVE-WRITE-F1-47.                                    NC1054.2
149000 MOVE-FAIL-F1-47.                                                 NC1054.2
149100     MOVE     MOVE73 TO COMPUTED-A.                               NC1054.2
149200     MOVE     "33445 56677     0 " TO CORRECT-A.                  NC1054.2
149300     PERFORM FAIL.                                                NC1054.2
149400 MOVE-WRITE-F1-47.                                                NC1054.2
149500     MOVE "MOVE-TEST-F1-47" TO PAR-NAME.                          NC1054.2
149600     PERFORM PRINT-DETAIL.                                        NC1054.2
149700 MOVE-INIT-F1-48.                                                 NC1054.2
149800                                                                  NC1054.2
149900 MOVE-TEST-F1-48-0.                                               NC1054.2
150000     MOVE "*" TO AE-0002.                                         NC1054.2
150100 MOVE-TEST-F1-48-1.                                               NC1054.2
150200     IF GRP-AE-0002 EQUAL TO "* 0      "                          NC1054.2
150300         PERFORM PASS                                             NC1054.2
150400         GO TO MOVE-WRITE-F1-48.                                  NC1054.2
150500     GO TO MOVE-FAIL-F1-48.                                       NC1054.2
150600 MOVE-DELETE-F1-48.                                               NC1054.2
150700     PERFORM DE-LETE.                                             NC1054.2
150800     GO TO MOVE-WRITE-F1-48.                                      NC1054.2
150900 MOVE-FAIL-F1-48.                                                 NC1054.2
151000     MOVE AE-0002 TO COMPUTED-A.                                  NC1054.2
151100     MOVE "* 0      " TO CORRECT-A.                               NC1054.2
151200     PERFORM FAIL.                                                NC1054.2
151300     PERFORM A20 THRU A60.                                        NC1054.2
151400 MOVE-WRITE-F1-48.                                                NC1054.2
151500     MOVE "MOVE-TEST-F1-48" TO PAR-NAME.                          NC1054.2
151600     PERFORM PRINT-DETAIL.                                        NC1054.2
151700 MOVE-INIT-F1-49.                                                 NC1054.2
151800     MOVE    "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-  =$, .()/ 0123456789"  NC1054.2
151900           TO ALPHANUMERIC-XN-00049.                              NC1054.2
152000 MOVE-TEST-F1-49-0.                                               NC1054.2
152100     MOVE ALPHANUMERIC-XN-00049 TO AE-0001.                       NC1054.2
152200 MOVE-TEST-F1-49-1.                                               NC1054.2
152300     MOVE AE-0001 TO AE-0002.                                     NC1054.2
152400     IF AE-0002 EQUAL TO "AB0CD EFG"                              NC1054.2
152500         PERFORM PASS                                             NC1054.2
152600         GO TO MOVE-WRITE-F1-49.                                  NC1054.2
152700     GO TO MOVE-FAIL-F1-49.                                       NC1054.2
152800 MOVE-DELETE-F1-49.                                               NC1054.2
152900     PERFORM DE-LETE.                                             NC1054.2
153000     GO TO MOVE-WRITE-F1-49.                                      NC1054.2
153100 MOVE-FAIL-F1-49.                                                 NC1054.2
153200     MOVE AE-0002 TO COMPUTED-A.                                  NC1054.2
153300     MOVE "AB0CD EFG" TO CORRECT-A.                               NC1054.2
153400     PERFORM FAIL.                                                NC1054.2
153500 MOVE-WRITE-F1-49.                                                NC1054.2
153600     MOVE "MOVE-TEST-F1-49" TO PAR-NAME.                          NC1054.2
153700     PERFORM PRINT-DETAIL.                                        NC1054.2
153800 MOVE-INIT-F1-50.                                                 NC1054.2
153900     MOVE "MOVE NUMERIC ITEM   " TO FEATURE.                      NC1054.2
154000     MOVE  0123456789 TO DIGITS-DU-10V00.                         NC1054.2
154100 MOVE-TEST-F1-50-0.                                               NC1054.2
154200     MOVE DIGITS-DU-10V00 TO GRP-WRK-DU-10V00.                    NC1054.2
154300 MOVE-TEST-F1-50-1.                                               NC1054.2
154400     IF WRK-DU-10V00 EQUAL TO DIGITS-DU-10V00                     NC1054.2
154500         PERFORM PASS                                             NC1054.2
154600         GO TO MOVE-WRITE-F1-50.                                  NC1054.2
154700     GO TO MOVE-FAIL-F1-50.                                       NC1054.2
154800 MOVE-DELETE-F1-50.                                               NC1054.2
154900     PERFORM DE-LETE.                                             NC1054.2
155000     GO TO MOVE-WRITE-F1-50.                                      NC1054.2
155100 MOVE-FAIL-F1-50.                                                 NC1054.2
155200     MOVE DIGITS-DU-10V00 TO CORRECT-A.                           NC1054.2
155300     MOVE WRK-DU-10V00 TO COMPUTED-A.                             NC1054.2
155400     PERFORM FAIL.                                                NC1054.2
155500 MOVE-WRITE-F1-50.                                                NC1054.2
155600     MOVE "MOVE-TEST-F1-50" TO PAR-NAME.                          NC1054.2
155700     PERFORM PRINT-DETAIL.                                        NC1054.2
155800 MOVE-INIT-F1-51.                                                 NC1054.2
155900     MOVE  0123456789 TO DIGITS-DU-10V00.                         NC1054.2
156000 MOVE-TEST-F1-51-0.                                               NC1054.2
156100     MOVE DIGITS-DU-10V00 TO WRK-XN-00049.                        NC1054.2
156200 MOVE-TEST-F1-51-1.                                               NC1054.2
156300     IF GRP-WRK-XN-00049 EQUAL TO "0123456789                     NC1054.2
156400-    "                  "                                         NC1054.2
156500         PERFORM PASS                                             NC1054.2
156600         GO TO MOVE-WRITE-F1-51.                                  NC1054.2
156700     GO TO MOVE-FAIL-F1-51.                                       NC1054.2
156800 MOVE-DELETE-F1-51.                                               NC1054.2
156900     PERFORM DE-LETE.                                             NC1054.2
157000     GO TO MOVE-WRITE-F1-51.                                      NC1054.2
157100 MOVE-FAIL-F1-51.                                                 NC1054.2
157200     MOVE "0123456789                                       "     NC1054.2
157300          TO SEND-BREAKDOWN                                       NC1054.2
157400     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
157500     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
157600     PERFORM FAIL.                                                NC1054.2
157700     PERFORM A20 THRU A60.                                        NC1054.2
157800 MOVE-WRITE-F1-51.                                                NC1054.2
157900     MOVE "MOVE-TEST-F1-51" TO PAR-NAME.                          NC1054.2
158000     PERFORM PRINT-DETAIL.                                        NC1054.2
158100 MOVE-INIT-F1-52.                                                 NC1054.2
158200     MOVE  0123456789 TO DIGITS-DU-10V00.                         NC1054.2
158300 MOVE-TEST-F1-52-0.                                               NC1054.2
158400     MOVE DIGITS-DU-10V00 TO AE-0002.                             NC1054.2
158500 MOVE-TEST-F1-52-1.                                               NC1054.2
158600     IF GRP-AE-0002 EQUAL TO "01023 456"                          NC1054.2
158700         PERFORM PASS                                             NC1054.2
158800         GO TO MOVE-WRITE-F1-52.                                  NC1054.2
158900     GO TO MOVE-FAIL-F1-52.                                       NC1054.2
159000 MOVE-DELETE-F1-52.                                               NC1054.2
159100     PERFORM DE-LETE.                                             NC1054.2
159200     GO TO MOVE-WRITE-F1-52.                                      NC1054.2
159300 MOVE-FAIL-F1-52.                                                 NC1054.2
159400     MOVE "01023 456" TO CORRECT-A.                               NC1054.2
159500     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
159600     PERFORM FAIL.                                                NC1054.2
159700 MOVE-WRITE-F1-52.                                                NC1054.2
159800     MOVE "MOVE-TEST-F1-52" TO PAR-NAME.                          NC1054.2
159900     PERFORM PRINT-DETAIL.                                        NC1054.2
160000 MOVE-INIT-F1-53.                                                 NC1054.2
160100     MOVE  0123456789 TO DIGITS-DU-10V00.                         NC1054.2
160200 MOVE-TEST-F1-53-0.                                               NC1054.2
160300     MOVE DIGITS-DU-10V00 TO WRK-DU-10V00.                        NC1054.2
160400 MOVE-TEST-F1-53-1.                                               NC1054.2
160500     IF GRP-WRK-DU-10V00 EQUAL TO DIGITS-DU-10V00                 NC1054.2
160600         PERFORM PASS                                             NC1054.2
160700         GO TO MOVE-WRITE-F1-53.                                  NC1054.2
160800     GO TO MOVE-FAIL-F1-53.                                       NC1054.2
160900 MOVE-DELETE-F1-53.                                               NC1054.2
161000     PERFORM DE-LETE.                                             NC1054.2
161100     GO TO MOVE-WRITE-F1-53.                                      NC1054.2
161200 MOVE-FAIL-F1-53.                                                 NC1054.2
161300     MOVE DIGITS-DU-10V00 TO CORRECT-A.                           NC1054.2
161400     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
161500     PERFORM FAIL.                                                NC1054.2
161600 MOVE-WRITE-F1-53.                                                NC1054.2
161700     MOVE "MOVE-TEST-F1-53" TO PAR-NAME.                          NC1054.2
161800     PERFORM PRINT-DETAIL.                                        NC1054.2
161900 MOVE-INIT-F1-54.                                                 NC1054.2
162000     MOVE  0123456789 TO DIGITS-DU-10V00.                         NC1054.2
162100 MOVE-TEST-F1-54-0.                                               NC1054.2
162200     MOVE DIGITS-DU-06V04-S TO NE-0001.                           NC1054.2
162300 MOVE-TEST-F1-54-1.                                               NC1054.2
162400     IF GRP-NE-0001 EQUAL TO " 12,345.678,9"                      NC1054.2
162500         PERFORM PASS                                             NC1054.2
162600         GO TO MOVE-WRITE-F1-54.                                  NC1054.2
162700     GO TO MOVE-FAIL-F1-54.                                       NC1054.2
162800 MOVE-DELETE-F1-54.                                               NC1054.2
162900     PERFORM DE-LETE.                                             NC1054.2
163000     GO TO MOVE-WRITE-F1-54.                                      NC1054.2
163100 MOVE-FAIL-F1-54.                                                 NC1054.2
163200     MOVE " 12,345.678,9" TO CORRECT-A.                           NC1054.2
163300     MOVE GRP-NE-0001 TO COMPUTED-A.                              NC1054.2
163400     PERFORM FAIL.                                                NC1054.2
163500 MOVE-WRITE-F1-54.                                                NC1054.2
163600     MOVE "MOVE-TEST-F1-54" TO PAR-NAME.                          NC1054.2
163700     PERFORM PRINT-DETAIL.                                        NC1054.2
163800 MOVE-INIT-F1-55.                                                 NC1054.2
163900     MOVE "MOVE NUMERIC EDITED" TO FEATURE.                       NC1054.2
164000 MOVE-TEST-F1-55-0.                                               NC1054.2
164100     MOVE " 12,345.678,9" TO GRP-NE-0001.                         NC1054.2
164200     MOVE NE-0001 TO GRP-WRK-XN-00049.                            NC1054.2
164300 MOVE-TEST-F1-55-1.                                               NC1054.2
164400     IF GRP-WRK-XN-00049 EQUAL TO                                 NC1054.2
164500        " 12,345.678,9                                    "       NC1054.2
164600         PERFORM PASS                                             NC1054.2
164700         GO TO MOVE-WRITE-F1-55.                                  NC1054.2
164800     GO TO MOVE-FAIL-F1-55.                                       NC1054.2
164900 MOVE-DELETE-F1-55.                                               NC1054.2
165000     PERFORM DE-LETE.                                             NC1054.2
165100     GO TO MOVE-WRITE-F1-55.                                      NC1054.2
165200 MOVE-FAIL-F1-55.                                                 NC1054.2
165300     MOVE " 12,345.678,9                                    "     NC1054.2
165400          TO SEND-BREAKDOWN.                                      NC1054.2
165500     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
165600     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
165700     PERFORM FAIL.                                                NC1054.2
165800     PERFORM A20 THRU A60.                                        NC1054.2
165900 MOVE-WRITE-F1-55.                                                NC1054.2
166000     MOVE "MOVE-TEST-F1-55" TO PAR-NAME.                          NC1054.2
166100     PERFORM PRINT-DETAIL.                                        NC1054.2
166200 MOVE-INIT-F1-56.                                                 NC1054.2
166300                                                                  NC1054.2
166400 MOVE-TEST-F1-56-0.                                               NC1054.2
166500     MOVE " 12,345.678,9" TO GRP-NE-0001.                         NC1054.2
166600     MOVE NE-0001 TO WRK-XN-00049.                                NC1054.2
166700 MOVE-TEST-F1-56-1.                                               NC1054.2
166800     IF GRP-WRK-XN-00049 EQUAL TO                                 NC1054.2
166900        " 12,345.678,9                                    "       NC1054.2
167000         PERFORM PASS                                             NC1054.2
167100         GO TO MOVE-WRITE-F1-56.                                  NC1054.2
167200     GO TO MOVE-FAIL-F1-56.                                       NC1054.2
167300 MOVE-DELETE-F1-56.                                               NC1054.2
167400     PERFORM DE-LETE.                                             NC1054.2
167500     GO TO MOVE-WRITE-F1-56.                                      NC1054.2
167600 MOVE-FAIL-F1-56.                                                 NC1054.2
167700     MOVE " 12,345.678,9                                    "     NC1054.2
167800          TO SEND-BREAKDOWN.                                      NC1054.2
167900     MOVE WRK-XN-00049 TO RECEIVE-BREAKDOWN.                      NC1054.2
168000     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
168100     PERFORM FAIL.                                                NC1054.2
168200     PERFORM A20 THRU A60.                                        NC1054.2
168300 MOVE-WRITE-F1-56.                                                NC1054.2
168400     MOVE "MOVE-TEST-F1-56" TO PAR-NAME.                          NC1054.2
168500     PERFORM PRINT-DETAIL.                                        NC1054.2
168600 MOVE-INIT-F1-57.                                                 NC1054.2
168700                                                                  NC1054.2
168800 MOVE-TEST-F1-57-0.                                               NC1054.2
168900     MOVE " 12,345.678,9" TO GRP-NE-0001.                         NC1054.2
169000     MOVE NE-0001 TO AE-0002.                                     NC1054.2
169100 MOVE-TEST-F1-57-1.                                               NC1054.2
169200     IF GRP-AE-0002 EQUAL TO " 102, 345"                          NC1054.2
169300         PERFORM PASS                                             NC1054.2
169400         GO TO MOVE-WRITE-F1-57.                                  NC1054.2
169500     GO TO MOVE-FAIL-F1-57.                                       NC1054.2
169600 MOVE-DELETE-F1-57.                                               NC1054.2
169700     PERFORM DE-LETE.                                             NC1054.2
169800     GO TO MOVE-WRITE-F1-57.                                      NC1054.2
169900 MOVE-FAIL-F1-57.                                                 NC1054.2
170000     MOVE " 102, 345" TO CORRECT-A.                               NC1054.2
170100     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
170200     PERFORM FAIL.                                                NC1054.2
170300 MOVE-WRITE-F1-57.                                                NC1054.2
170400     MOVE "MOVE-TEST-F1-57" TO PAR-NAME.                          NC1054.2
170500     PERFORM PRINT-DETAIL.                                        NC1054.2
170600 MOVE-INIT-F1-58.                                                 NC1054.2
170700     MOVE "MOVE ZERO LITERAL   " TO FEATURE.                      NC1054.2
170800 MOVE-TEST-F1-58-0.                                               NC1054.2
170900     MOVE ZERO TO GRP-WRK-DU-10V00.                               NC1054.2
171000 MOVE-TEST-F1-58-1.                                               NC1054.2
171100     IF WRK-DU-10V00 EQUAL TO "0000000000"                        NC1054.2
171200         PERFORM PASS                                             NC1054.2
171300         GO TO MOVE-WRITE-F1-58.                                  NC1054.2
171400     GO TO MOVE-FAIL-F1-58.                                       NC1054.2
171500 MOVE-DELETE-F1-58.                                               NC1054.2
171600     PERFORM DE-LETE.                                             NC1054.2
171700     GO TO MOVE-WRITE-F1-58.                                      NC1054.2
171800 MOVE-FAIL-F1-58.                                                 NC1054.2
171900     MOVE "0000000000" TO CORRECT-A.                              NC1054.2
172000     MOVE WRK-DU-10V00 TO COMPUTED-A.                             NC1054.2
172100     PERFORM FAIL.                                                NC1054.2
172200 MOVE-WRITE-F1-58.                                                NC1054.2
172300     MOVE "MOVE-TEST-F1-58" TO PAR-NAME.                          NC1054.2
172400     PERFORM PRINT-DETAIL.                                        NC1054.2
172500 MOVE-INIT-F1-59.                                                 NC1054.2
172600                                                                  NC1054.2
172700 MOVE-TEST-F1-59-0.                                               NC1054.2
172800     MOVE "0000000000000000000000000000000000000000000000000"     NC1054.2
172900          TO WRK-XN-00049.                                        NC1054.2
173000 MOVE-TEST-F1-59-1.                                               NC1054.2
173100     IF GRP-WRK-XN-00049 EQUAL TO ZERO                            NC1054.2
173200         PERFORM PASS                                             NC1054.2
173300         GO TO MOVE-WRITE-F1-59.                                  NC1054.2
173400     GO TO MOVE-FAIL-F1-59.                                       NC1054.2
173500 MOVE-DELETE-F1-59.                                               NC1054.2
173600     PERFORM DE-LETE.                                             NC1054.2
173700     GO TO MOVE-WRITE-F1-59.                                      NC1054.2
173800 MOVE-FAIL-F1-59.                                                 NC1054.2
173900     MOVE "0000000000000000000000000000000000000000000000000"     NC1054.2
174000         TO SEND-BREAKDOWN.                                       NC1054.2
174100     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
174200     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
174300     PERFORM FAIL.                                                NC1054.2
174400     PERFORM A20 THRU A60.                                        NC1054.2
174500 MOVE-WRITE-F1-59.                                                NC1054.2
174600     MOVE "MOVE-TEST-F1-59" TO PAR-NAME.                          NC1054.2
174700     PERFORM PRINT-DETAIL.                                        NC1054.2
174800 MOVE-INIT-F1-60.                                                 NC1054.2
174900                                                                  NC1054.2
175000 MOVE-TEST-F1-60-0.                                               NC1054.2
175100     MOVE ZERO TO AE-0002.                                        NC1054.2
175200 MOVE-TEST-F1-60-1.                                               NC1054.2
175300     IF GRP-AE-0002 EQUAL TO "00000 000"                          NC1054.2
175400         PERFORM PASS                                             NC1054.2
175500         GO TO MOVE-WRITE-F1-60.                                  NC1054.2
175600     GO TO MOVE-FAIL-F1-60.                                       NC1054.2
175700 MOVE-DELETE-F1-60.                                               NC1054.2
175800     PERFORM DE-LETE.                                             NC1054.2
175900     GO TO MOVE-WRITE-F1-60.                                      NC1054.2
176000 MOVE-FAIL-F1-60.                                                 NC1054.2
176100     MOVE "00000 000" TO CORRECT-A.                               NC1054.2
176200     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
176300     PERFORM FAIL.                                                NC1054.2
176400 MOVE-WRITE-F1-60.                                                NC1054.2
176500     MOVE "MOVE-TEST-F1-60" TO PAR-NAME.                          NC1054.2
176600     PERFORM PRINT-DETAIL.                                        NC1054.2
176700 MOVE-INIT-F1-61.                                                 NC1054.2
176800 MOVE-TEST-F1-61-0.                                               NC1054.2
176900     MOVE ZERO TO WRK-DU-10V00.                                   NC1054.2
177000 MOVE-TEST-F1-61-1.                                               NC1054.2
177100     IF GRP-WRK-DU-10V00 EQUAL TO "0000000000"                    NC1054.2
177200         PERFORM PASS                                             NC1054.2
177300         GO TO MOVE-WRITE-F1-61.                                  NC1054.2
177400     GO TO MOVE-FAIL-F1-61.                                       NC1054.2
177500 MOVE-DELETE-117.                                                 NC1054.2
177600     PERFORM DE-LETE.                                             NC1054.2
177700     GO TO MOVE-WRITE-F1-61.                                      NC1054.2
177800 MOVE-FAIL-F1-61.                                                 NC1054.2
177900     MOVE "0000000000" TO CORRECT-A.                              NC1054.2
178000     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
178100     PERFORM FAIL.                                                NC1054.2
178200 MOVE-WRITE-F1-61.                                                NC1054.2
178300     MOVE "MOVE-TEST-F1-61" TO PAR-NAME.                          NC1054.2
178400     PERFORM PRINT-DETAIL.                                        NC1054.2
178500 MOVE-INIT-F1-62.                                                 NC1054.2
178600 MOVE-TEST-F1-62-0.                                               NC1054.2
178700     MOVE ZERO TO NE-0001.                                        NC1054.2
178800 MOVE-TEST-F1-62-1.                                               NC1054.2
178900     IF GRP-NE-0001 EQUAL TO "    000.000,0"                      NC1054.2
179000         PERFORM PASS                                             NC1054.2
179100         GO TO MOVE-WRITE-F1-62.                                  NC1054.2
179200     GO TO MOVE-FAIL-F1-62.                                       NC1054.2
179300 MOVE-DELETE-F1-62.                                               NC1054.2
179400     PERFORM DE-LETE.                                             NC1054.2
179500     GO TO MOVE-WRITE-F1-62.                                      NC1054.2
179600 MOVE-FAIL-F1-62.                                                 NC1054.2
179700     MOVE "    000.000,0" TO CORRECT-A.                           NC1054.2
179800     MOVE GRP-NE-0001 TO COMPUTED-A.                              NC1054.2
179900     PERFORM FAIL.                                                NC1054.2
180000 MOVE-WRITE-F1-62.                                                NC1054.2
180100     MOVE "MOVE-TEST-F1-62" TO PAR-NAME.                          NC1054.2
180200     PERFORM PRINT-DETAIL.                                        NC1054.2
180300 MOVE-INIT-F1-63.                                                 NC1054.2
180400     MOVE "MOVE SPACE LITERAL  " TO FEATURE.                      NC1054.2
180500 MOVE-TEST-F1-63-0.                                               NC1054.2
180600     MOVE SPACE TO GRP-WRK-DU-10V00.                              NC1054.2
180700 MOVE-TEST-F1-63-1.                                               NC1054.2
180800     IF GRP-WRK-DU-10V00 EQUAL TO SPACE                           NC1054.2
180900         PERFORM PASS                                             NC1054.2
181000         GO TO MOVE-WRITE-F1-63.                                  NC1054.2
181100     GO TO MOVE-FAIL-F1-63.                                       NC1054.2
181200 MOVE-DELETE-F1-63.                                               NC1054.2
181300     PERFORM DE-LETE.                                             NC1054.2
181400     GO TO MOVE-WRITE-F1-63.                                      NC1054.2
181500 MOVE-FAIL-F1-63.                                                 NC1054.2
181600     MOVE SPACE TO CORRECT-A.                                     NC1054.2
181700     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
181800     PERFORM FAIL.                                                NC1054.2
181900 MOVE-WRITE-F1-63.                                                NC1054.2
182000     MOVE "MOVE-TEST-F1-63" TO PAR-NAME.                          NC1054.2
182100     PERFORM PRINT-DETAIL.                                        NC1054.2
182200 MOVE-INIT-F1-64.                                                 NC1054.2
182300 MOVE-TEST-F1-64-0.                                               NC1054.2
182400     MOVE SPACE TO WRK-AN-00026.                                  NC1054.2
182500 MOVE-TEST-F1-64-1.                                               NC1054.2
182600     IF GRP-WRK-AN-00026 EQUAL TO "                          "    NC1054.2
182700         PERFORM PASS                                             NC1054.2
182800         GO TO MOVE-WRITE-F1-64.                                  NC1054.2
182900     GO TO MOVE-FAIL-F1-64.                                       NC1054.2
183000 MOVE-DELETE-F1-64.                                               NC1054.2
183100     PERFORM DE-LETE.                                             NC1054.2
183200     GO TO MOVE-WRITE-F1-64.                                      NC1054.2
183300 MOVE-FAIL-F1-64.                                                 NC1054.2
183400     MOVE SPACE TO SEND-BREAKDOWN.                                NC1054.2
183500     MOVE GRP-WRK-AN-00026 TO RECEIVE-BREAKDOWN.                  NC1054.2
183600     MOVE 026 TO LENGTH-COUNTER.                                  NC1054.2
183700     PERFORM FAIL.                                                NC1054.2
183800     PERFORM A20 THRU A40.                                        NC1054.2
183900 MOVE-WRITE-F1-64.                                                NC1054.2
184000     MOVE "MOVE-TEST-F1-64" TO PAR-NAME.                          NC1054.2
184100     PERFORM PRINT-DETAIL.                                        NC1054.2
184200 MOVE-INIT-F1-65.                                                 NC1054.2
184300 MOVE-TEST-F1-65-0.                                               NC1054.2
184400     MOVE SPACE TO WRK-XN-00049.                                  NC1054.2
184500 MOVE-TEST-F1-65-1.                                               NC1054.2
184600     IF GRP-WRK-XN-00049 EQUAL TO SPACE                           NC1054.2
184700         PERFORM PASS                                             NC1054.2
184800         GO TO MOVE-WRITE-F1-65.                                  NC1054.2
184900     GO TO MOVE-FAIL-F1-65.                                       NC1054.2
185000 MOVE-DELETE-F1-65.                                               NC1054.2
185100     PERFORM DE-LETE.                                             NC1054.2
185200     GO TO MOVE-WRITE-F1-65.                                      NC1054.2
185300 MOVE-FAIL-F1-65.                                                 NC1054.2
185400     MOVE SPACE TO SEND-BREAKDOWN.                                NC1054.2
185500     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
185600     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
185700     PERFORM FAIL.                                                NC1054.2
185800     PERFORM A20 THRU A60.                                        NC1054.2
185900 MOVE-WRITE-F1-65.                                                NC1054.2
186000     MOVE "MOVE-TEST-F1-65" TO PAR-NAME.                          NC1054.2
186100     PERFORM PRINT-DETAIL.                                        NC1054.2
186200 MOVE-INIT-F1-66.                                                 NC1054.2
186300 MOVE-TEST-F1-66-0.                                               NC1054.2
186400     MOVE SPACE TO AE-0002.                                       NC1054.2
186500 MOVE-TEST-F1-66-1.                                               NC1054.2
186600     IF GRP-AE-0002 EQUAL TO "  0      "                          NC1054.2
186700         PERFORM PASS                                             NC1054.2
186800         GO TO MOVE-WRITE-F1-66.                                  NC1054.2
186900     GO TO MOVE-FAIL-F1-66.                                       NC1054.2
187000 MOVE-DELETE-F1-66.                                               NC1054.2
187100     PERFORM DE-LETE.                                             NC1054.2
187200     GO TO MOVE-WRITE-F1-66.                                      NC1054.2
187300 MOVE-FAIL-F1-66.                                                 NC1054.2
187400     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
187500     MOVE "  0      " TO CORRECT-A.                               NC1054.2
187600     PERFORM FAIL.                                                NC1054.2
187700 MOVE-WRITE-F1-66.                                                NC1054.2
187800     MOVE "MOVE-TEST-F1-66" TO PAR-NAME.                          NC1054.2
187900     PERFORM PRINT-DETAIL.                                        NC1054.2
188000 MOVE-INIT-F1-67.                                                 NC1054.2
188100     MOVE "MOVE HIGH-VALUE     " TO FEATURE.                      NC1054.2
188200 MOVE-TEST-F1-67-0.                                               NC1054.2
188300     MOVE HIGH-VALUE TO GRP-WRK-DU-10V00.                         NC1054.2
188400 MOVE-TEST-F1-67-1.                                               NC1054.2
188500     IF GRP-WRK-DU-10V00 EQUAL TO HIGH-VALUE                      NC1054.2
188600         PERFORM PASS                                             NC1054.2
188700         GO TO MOVE-WRITE-F1-67.                                  NC1054.2
188800     GO TO MOVE-FAIL-F1-67.                                       NC1054.2
188900 MOVE-DELETE-F1-67.                                               NC1054.2
189000     PERFORM DE-LETE.                                             NC1054.2
189100     GO TO MOVE-WRITE-F1-67.                                      NC1054.2
189200 MOVE-FAIL-F1-67.                                                 NC1054.2
189300     MOVE HIGH-VALU-10LONG TO CORRECT-A.                          NC1054.2
189400     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
189500     PERFORM FAIL.                                                NC1054.2
189600 MOVE-WRITE-F1-67.                                                NC1054.2
189700     MOVE "MOVE-TEST-F1-67" TO PAR-NAME.                          NC1054.2
189800     PERFORM PRINT-DETAIL.                                        NC1054.2
189900 MOVE-INIT-F1-68.                                                 NC1054.2
190000 MOVE-TEST-F1-68-0.                                               NC1054.2
190100     MOVE HIGH-VALUE TO WRK-XN-00049.                             NC1054.2
190200 MOVE-TEST-F1-68-1.                                               NC1054.2
190300     IF GRP-WRK-XN-00049 EQUAL TO HIGH-VALUE                      NC1054.2
190400         PERFORM PASS                                             NC1054.2
190500         GO TO MOVE-WRITE-F1-68.                                  NC1054.2
190600 MOVE-DELETE-F1-68.                                               NC1054.2
190700     PERFORM DE-LETE.                                             NC1054.2
190800     GO TO MOVE-WRITE-F1-68.                                      NC1054.2
190900 MOVE-FAIL-F1-68.                                                 NC1054.2
191000     MOVE HIGH-VALU-49LONG TO SEND-BREAKDOWN.                     NC1054.2
191100     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
191200     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
191300     PERFORM FAIL.                                                NC1054.2
191400     PERFORM A20 THRU A60.                                        NC1054.2
191500 MOVE-WRITE-F1-68.                                                NC1054.2
191600     MOVE "MOVE-TEST-F1-68" TO PAR-NAME.                          NC1054.2
191700     PERFORM PRINT-DETAIL.                                        NC1054.2
191800 MOVE-INIT-F1-69.                                                 NC1054.2
191900 MOVE-TEST-F1-69-0.                                               NC1054.2
192000     MOVE HIGH-VALUE TO AE-0002.                                  NC1054.2
192100 MOVE-TEST-F1-69-1.                                               NC1054.2
192200     IF GRP-AE-0002 EQUAL TO HIGH-VALUE-EDIT                      NC1054.2
192300         PERFORM PASS                                             NC1054.2
192400         GO TO MOVE-WRITE-F1-69.                                  NC1054.2
192500     GO TO MOVE-FAIL-F1-69.                                       NC1054.2
192600 MOVE-DELETE-F1-69.                                               NC1054.2
192700     PERFORM DE-LETE.                                             NC1054.2
192800     GO TO MOVE-WRITE-F1-69.                                      NC1054.2
192900 MOVE-FAIL-F1-69.                                                 NC1054.2
193000     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
193100     MOVE HIGH-VALUE-EDIT TO CORRECT-A.                           NC1054.2
193200     PERFORM FAIL.                                                NC1054.2
193300 MOVE-WRITE-F1-69.                                                NC1054.2
193400     MOVE "MOVE-TEST-F1-69" TO PAR-NAME.                          NC1054.2
193500     PERFORM PRINT-DETAIL.                                        NC1054.2
193600 MOVE-INIT-F1-70.                                                 NC1054.2
193700     MOVE "MOVE LOW-VALUE      " TO FEATURE.                      NC1054.2
193800 MOVE-TEST-F1-70-0.                                               NC1054.2
193900     MOVE LOW-VALUE TO GRP-WRK-DU-10V00.                          NC1054.2
194000 MOVE-TEST-F1-70-1.                                               NC1054.2
194100     IF GRP-WRK-DU-10V00 EQUAL TO LOW-VALUE                       NC1054.2
194200         PERFORM PASS                                             NC1054.2
194300         GO TO MOVE-WRITE-F1-70.                                  NC1054.2
194400     GO TO MOVE-FAIL-F1-70.                                       NC1054.2
194500 MOVE-DELETE-F1-70.                                               NC1054.2
194600     PERFORM DE-LETE.                                             NC1054.2
194700     GO TO MOVE-WRITE-F1-70.                                      NC1054.2
194800 MOVE-FAIL-F1-70.                                                 NC1054.2
194900     MOVE LOW-VALU-10LONG TO CORRECT-A.                           NC1054.2
195000     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
195100     PERFORM FAIL.                                                NC1054.2
195200 MOVE-WRITE-F1-70.                                                NC1054.2
195300     MOVE "MOVE-TEST-F1-70" TO PAR-NAME.                          NC1054.2
195400     PERFORM PRINT-DETAIL.                                        NC1054.2
195500 MOVE-INIT-F1-71.                                                 NC1054.2
195600 MOVE-TEST-F1-71-0.                                               NC1054.2
195700     MOVE LOW-VALUE TO WRK-XN-00049.                              NC1054.2
195800 MOVE-TEST-F1-71-1.                                               NC1054.2
195900     IF GRP-WRK-XN-00049 EQUAL TO LOW-VALUE                       NC1054.2
196000         PERFORM PASS                                             NC1054.2
196100         GO TO MOVE-WRITE-F1-71.                                  NC1054.2
196200     GO TO MOVE-FAIL-F1-71.                                       NC1054.2
196300 MOVE-DELETE-F1-71.                                               NC1054.2
196400     PERFORM DE-LETE.                                             NC1054.2
196500     GO TO MOVE-WRITE-F1-71.                                      NC1054.2
196600 MOVE-FAIL-F1-71.                                                 NC1054.2
196700     MOVE LOW-VALU-49LONG TO SEND-BREAKDOWN.                      NC1054.2
196800     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
196900     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
197000     PERFORM FAIL.                                                NC1054.2
197100     PERFORM A20 THRU A60.                                        NC1054.2
197200 MOVE-WRITE-F1-71.                                                NC1054.2
197300     MOVE "MOVE-TEST-F1-71" TO PAR-NAME.                          NC1054.2
197400     PERFORM PRINT-DETAIL.                                        NC1054.2
197500 MOVE-INIT-F1-72.                                                 NC1054.2
197600     MOVE LOW-VALUE TO HIGH-1 HIGH-2 HIGH-3.                      NC1054.2
197700 MOVE-TEST-F1-72-0.                                               NC1054.2
197800     MOVE LOW-VALUE TO AE-0002.                                   NC1054.2
197900 MOVE-TEST-F1-72-1.                                               NC1054.2
198000     IF GRP-AE-0002 EQUAL TO HIGH-VALUE-EDIT                      NC1054.2
198100         PERFORM PASS                                             NC1054.2
198200         GO TO MOVE-WRITE-F1-72.                                  NC1054.2
198300     GO TO MOVE-FAIL-F1-72.                                       NC1054.2
198400 MOVE-DELETE-F1-72.                                               NC1054.2
198500     PERFORM DE-LETE.                                             NC1054.2
198600     GO TO MOVE-WRITE-F1-72.                                      NC1054.2
198700 MOVE-FAIL-F1-72.                                                 NC1054.2
198800     MOVE HIGH-VALUE-EDIT TO CORRECT-A.                           NC1054.2
198900     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
199000     PERFORM FAIL.                                                NC1054.2
199100 MOVE-WRITE-F1-72.                                                NC1054.2
199200     MOVE "MOVE-TEST-F1-72" TO PAR-NAME.                          NC1054.2
199300     PERFORM PRINT-DETAIL.                                        NC1054.2
199400 MOVE-INIT-F1-73.                                                 NC1054.2
199500     MOVE "MOVE QUOTE          " TO FEATURE.                      NC1054.2
199600 MOVE-TEST-F1-73-0.                                               NC1054.2
199700     MOVE QUOTE TO GRP-WRK-DU-10V00.                              NC1054.2
199800 MOVE-TEST-F1-73-1.                                               NC1054.2
199900     IF GRP-WRK-DU-10V00 EQUAL TO QUOTE                           NC1054.2
200000         PERFORM PASS                                             NC1054.2
200100         GO TO MOVE-WRITE-F1-73.                                  NC1054.2
200200     GO TO MOVE-FAIL-F1-73.                                       NC1054.2
200300 MOVE-DELETE-F1-73.                                               NC1054.2
200400     PERFORM DE-LETE.                                             NC1054.2
200500     GO TO MOVE-WRITE-F1-73.                                      NC1054.2
200600 MOVE-FAIL-F1-73.                                                 NC1054.2
200700     MOVE QUOTE-10LONG TO CORRECT-A.                              NC1054.2
200800     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
200900     PERFORM FAIL.                                                NC1054.2
201000 MOVE-WRITE-F1-73.                                                NC1054.2
201100     MOVE "MOVE-TEST-F1-73" TO PAR-NAME.                          NC1054.2
201200     PERFORM PRINT-DETAIL.                                        NC1054.2
201300 MOVE-INIT-F1-74.                                                 NC1054.2
201400 MOVE-TEST-F1-74-0.                                               NC1054.2
201500     MOVE QUOTE TO WRK-XN-00049.                                  NC1054.2
201600 MOVE-TEST-F1-74-1.                                               NC1054.2
201700     IF GRP-WRK-XN-00049 EQUAL TO QUOTE                           NC1054.2
201800         PERFORM PASS                                             NC1054.2
201900         GO TO MOVE-WRITE-F1-74.                                  NC1054.2
202000     GO TO MOVE-FAIL-F1-74.                                       NC1054.2
202100 MOVE-DELETE-F1-74.                                               NC1054.2
202200     PERFORM DE-LETE.                                             NC1054.2
202300     GO TO MOVE-WRITE-F1-74.                                      NC1054.2
202400 MOVE-FAIL-F1-74.                                                 NC1054.2
202500     MOVE QUOTE-49LONG TO SEND-BREAKDOWN.                         NC1054.2
202600     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
202700     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
202800     PERFORM FAIL.                                                NC1054.2
202900     PERFORM A20 THRU A60.                                        NC1054.2
203000 MOVE-WRITE-F1-74.                                                NC1054.2
203100     MOVE "MOVE-TEST-F1-74" TO PAR-NAME.                          NC1054.2
203200     PERFORM PRINT-DETAIL.                                        NC1054.2
203300 MOVE-INIT-F1-75.                                                 NC1054.2
203400     MOVE QUOTE TO HIGH-1 HIGH-2 HIGH-3.                          NC1054.2
203500 MOVE-TEST-F1-75-0.                                               NC1054.2
203600     MOVE QUOTE TO AE-0002.                                       NC1054.2
203700 MOVE-TEST-F1-75-1.                                               NC1054.2
203800     IF GRP-AE-0002 EQUAL TO HIGH-VALUE-EDIT                      NC1054.2
203900         PERFORM PASS                                             NC1054.2
204000         GO TO MOVE-WRITE-F1-75.                                  NC1054.2
204100     GO TO MOVE-FAIL-F1-75.                                       NC1054.2
204200 MOVE-DELETE-F1-75.                                               NC1054.2
204300     PERFORM DE-LETE.                                             NC1054.2
204400     GO TO MOVE-WRITE-F1-75.                                      NC1054.2
204500 MOVE-FAIL-F1-75.                                                 NC1054.2
204600     MOVE HIGH-VALUE-EDIT TO CORRECT-A.                           NC1054.2
204700     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
204800     PERFORM FAIL.                                                NC1054.2
204900 MOVE-WRITE-F1-75.                                                NC1054.2
205000     MOVE "MOVE-TEST-F1-75" TO PAR-NAME.                          NC1054.2
205100     PERFORM PRINT-DETAIL.                                        NC1054.2
205200 MOVE-INIT-F1-76.                                                 NC1054.2
205300 MOVE-TEST-F1-76-0.                                               NC1054.2
205400     MOVE "A1B2C3D4E5" TO GRP-WRK-DU-10V00.                       NC1054.2
205500 MOVE-TEST-F1-76-1.                                               NC1054.2
205600     IF GRP-WRK-DU-10V00 EQUAL TO "A1B2C3D4E5"                    NC1054.2
205700         PERFORM PASS                                             NC1054.2
205800         GO TO MOVE-WRITE-F1-76.                                  NC1054.2
205900     GO TO MOVE-FAIL-F1-76.                                       NC1054.2
206000 MOVE-DELETE-F1-76.                                               NC1054.2
206100     PERFORM DE-LETE.                                             NC1054.2
206200     GO TO MOVE-WRITE-F1-76.                                      NC1054.2
206300 MOVE-FAIL-F1-76.                                                 NC1054.2
206400     MOVE "A1B2C3D4E5" TO CORRECT-A.                              NC1054.2
206500     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
206600     PERFORM FAIL.                                                NC1054.2
206700 MOVE-WRITE-F1-76.                                                NC1054.2
206800     MOVE "MOVE ALPHNUM LITERAL" TO FEATURE.                      NC1054.2
206900     MOVE "MOVE-TEST-F1-76" TO PAR-NAME.                          NC1054.2
207000     PERFORM PRINT-DETAIL.                                        NC1054.2
207100 MOVE-INIT-F1-77.                                                 NC1054.2
207200 MOVE-TEST-F1-77-0.                                               NC1054.2
207300     MOVE "ABCDEFGHIJK" TO WRK-AN-00026.                          NC1054.2
207400 MOVE-TEST-F1-77-1.                                               NC1054.2
207500     IF     GRP-WRK-AN-00026 EQUAL TO "ABCDEFGHIJK               "NC1054.2
207600         PERFORM PASS                                             NC1054.2
207700         GO TO MOVE-WRITE-F1-77.                                  NC1054.2
207800     GO TO MOVE-FAIL-F1-77.                                       NC1054.2
207900 MOVE-DELETE-F1-77.                                               NC1054.2
208000     PERFORM DE-LETE.                                             NC1054.2
208100     GO TO MOVE-WRITE-F1-77.                                      NC1054.2
208200 MOVE-FAIL-F1-77.                                                 NC1054.2
208300     MOVE "ABCDEFGHIJK               " TO SEND-BREAKDOWN.         NC1054.2
208400     MOVE GRP-WRK-AN-00026 TO RECEIVE-BREAKDOWN.                  NC1054.2
208500     MOVE 026 TO LENGTH-COUNTER.                                  NC1054.2
208600     PERFORM FAIL.                                                NC1054.2
208700     PERFORM A20 THRU A40.                                        NC1054.2
208800 MOVE-WRITE-F1-77.                                                NC1054.2
208900     MOVE "MOVE ALPHA LITERAL  " TO FEATURE.                      NC1054.2
209000     MOVE "MOVE-TEST-F1-77" TO PAR-NAME.                          NC1054.2
209100     PERFORM PRINT-DETAIL.                                        NC1054.2
209200 MOVE-INIT-F1-78.                                                 NC1054.2
209300     MOVE "MOVE ALPHNUM LITERAL" TO FEATURE.                      NC1054.2
209400 MOVE-TEST-F1-78-0.                                               NC1054.2
209500     MOVE "1A2B3C4D5E6F" TO WRK-XN-00049.                         NC1054.2
209600 MOVE-TEST-F1-78-1.                                               NC1054.2
209700     IF GRP-WRK-XN-00049 EQUAL TO                                 NC1054.2
209800        "1A2B3C4D5E6F                                     "       NC1054.2
209900         PERFORM PASS                                             NC1054.2
210000         GO TO MOVE-WRITE-F1-78.                                  NC1054.2
210100     GO TO MOVE-FAIL-F1-78.                                       NC1054.2
210200 MOVE-DELETE-F1-78.                                               NC1054.2
210300     PERFORM DE-LETE.                                             NC1054.2
210400     GO TO MOVE-WRITE-F1-78.                                      NC1054.2
210500 MOVE-FAIL-F1-78.                                                 NC1054.2
210600     MOVE "1A2B3C4D5E6F                                     "     NC1054.2
210700          TO SEND-BREAKDOWN.                                      NC1054.2
210800     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
210900     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
211000     PERFORM FAIL.                                                NC1054.2
211100     PERFORM A20 THRU A60.                                        NC1054.2
211200 MOVE-WRITE-F1-78.                                                NC1054.2
211300     MOVE "MOVE-TEST-F1-78" TO PAR-NAME.                          NC1054.2
211400     PERFORM PRINT-DETAIL.                                        NC1054.2
211500 MOVE-INIT-F1-79.                                                 NC1054.2
211600 MOVE-TEST-F1-79-0.                                               NC1054.2
211700     MOVE "1Z2Y3X4W5V" TO AE-0002.                                NC1054.2
211800 MOVE-TEST-F1-79-1.                                               NC1054.2
211900     IF GRP-AE-0002 EQUAL TO "1Z02Y 3X4"                          NC1054.2
212000         PERFORM PASS                                             NC1054.2
212100         GO TO MOVE-WRITE-F1-79.                                  NC1054.2
212200     GO TO MOVE-FAIL-F1-79.                                       NC1054.2
212300 MOVE-DELETE-F1-79.                                               NC1054.2
212400     PERFORM DE-LETE.                                             NC1054.2
212500     GO TO MOVE-WRITE-F1-79.                                      NC1054.2
212600 MOVE-FAIL-F1-79.                                                 NC1054.2
212700     MOVE "1Z02Y 3X4" TO CORRECT-A.                               NC1054.2
212800     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
212900     PERFORM FAIL.                                                NC1054.2
213000 MOVE-WRITE-F1-79.                                                NC1054.2
213100     MOVE "MOVE-TEST-F1-79" TO PAR-NAME.                          NC1054.2
213200     PERFORM PRINT-DETAIL.                                        NC1054.2
213300 MOVE-INIT-F1-80.                                                 NC1054.2
213400 MOVE-TEST-F1-80-0.                                               NC1054.2
213500     MOVE "9876543210" TO WRK-DU-10V00.                           NC1054.2
213600 MOVE-TEST-F1-80-1.                                               NC1054.2
213700     IF GRP-WRK-DU-10V00 EQUAL TO "9876543210"                    NC1054.2
213800         PERFORM PASS                                             NC1054.2
213900         GO TO MOVE-WRITE-F1-80.                                  NC1054.2
214000     GO TO MOVE-FAIL-F1-80.                                       NC1054.2
214100 MOVE-DELETE-F1-80.                                               NC1054.2
214200     PERFORM DE-LETE.                                             NC1054.2
214300     GO TO MOVE-WRITE-F1-80.                                      NC1054.2
214400 MOVE-FAIL-F1-80.                                                 NC1054.2
214500     MOVE "9876543210" TO CORRECT-A.                              NC1054.2
214600     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
214700     PERFORM FAIL.                                                NC1054.2
214800 MOVE-WRITE-F1-80.                                                NC1054.2
214900     MOVE "MOVE-TEST-F1-80" TO PAR-NAME.                          NC1054.2
215000     PERFORM PRINT-DETAIL.                                        NC1054.2
215100 MOVE-INIT-F1-81.                                                 NC1054.2
215200 MOVE-TEST-F1-81-0.                                               NC1054.2
215300     MOVE "9876543210" TO NE-0002.                                NC1054.2
215400 MOVE-TEST-F1-81-1.                                               NC1054.2
215500     IF GRP-NE-0002 EQUAL TO "9876543,210"                        NC1054.2
215600         PERFORM PASS                                             NC1054.2
215700         GO TO MOVE-WRITE-F1-81.                                  NC1054.2
215800     GO TO MOVE-FAIL-F1-81.                                       NC1054.2
215900 MOVE-DELETE-F1-81.                                               NC1054.2
216000     PERFORM DE-LETE.                                             NC1054.2
216100     GO TO MOVE-WRITE-F1-81.                                      NC1054.2
216200 MOVE-FAIL-F1-81.                                                 NC1054.2
216300     MOVE "9876543,210" TO CORRECT-A.                             NC1054.2
216400     MOVE GRP-NE-0002 TO COMPUTED-A.                              NC1054.2
216500     PERFORM FAIL.                                                NC1054.2
216600 MOVE-WRITE-F1-81.                                                NC1054.2
216700     MOVE "MOVE-TEST-F1-81" TO PAR-NAME.                          NC1054.2
216800     PERFORM PRINT-DETAIL.                                        NC1054.2
216900 MOVE-INIT-F1-82.                                                 NC1054.2
217000     MOVE "MOVE NUMERIC LITERAL" TO FEATURE.                      NC1054.2
217100 MOVE-TEST-F1-82-0.                                               NC1054.2
217200     MOVE 0123456789 TO GRP-WRK-DU-10V00.                         NC1054.2
217300 MOVE-TEST-F1-82-1.                                               NC1054.2
217400     IF GRP-WRK-DU-10V00 EQUAL TO "0123456789"                    NC1054.2
217500         PERFORM PASS                                             NC1054.2
217600         GO TO MOVE-WRITE-F1-82.                                  NC1054.2
217700     GO TO MOVE-FAIL-F1-82.                                       NC1054.2
217800 MOVE-DELETE-F1-82.                                               NC1054.2
217900     PERFORM DE-LETE.                                             NC1054.2
218000     GO TO MOVE-WRITE-F1-82.                                      NC1054.2
218100 MOVE-FAIL-F1-82.                                                 NC1054.2
218200     MOVE "0123456789" TO CORRECT-A.                              NC1054.2
218300     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
218400     PERFORM FAIL.                                                NC1054.2
218500 MOVE-WRITE-F1-82.                                                NC1054.2
218600     MOVE "MOVE-TEST-F1-82" TO PAR-NAME.                          NC1054.2
218700     PERFORM PRINT-DETAIL.                                        NC1054.2
218800 MOVE-INIT-F1-83.                                                 NC1054.2
218900 MOVE-TEST-F1-83-0.                                               NC1054.2
219000     MOVE 0918273645 TO WRK-XN-00049.                             NC1054.2
219100 MOVE-TEST-F1-83-1.                                               NC1054.2
219200     IF GRP-WRK-XN-00049 EQUAL TO                                 NC1054.2
219300        "0918273645                                       "       NC1054.2
219400         PERFORM PASS                                             NC1054.2
219500         GO TO MOVE-WRITE-F1-83.                                  NC1054.2
219600     GO TO MOVE-FAIL-F1-83.                                       NC1054.2
219700 MOVE-DELETE-F1-83.                                               NC1054.2
219800     PERFORM DE-LETE.                                             NC1054.2
219900     GO TO MOVE-WRITE-F1-83.                                      NC1054.2
220000 MOVE-FAIL-F1-83.                                                 NC1054.2
220100     MOVE "0918273645                                       "     NC1054.2
220200          TO SEND-BREAKDOWN.                                      NC1054.2
220300     MOVE GRP-WRK-XN-00049 TO RECEIVE-BREAKDOWN.                  NC1054.2
220400     MOVE 049 TO LENGTH-COUNTER.                                  NC1054.2
220500     PERFORM FAIL.                                                NC1054.2
220600     PERFORM A20 THRU A60.                                        NC1054.2
220700 MOVE-WRITE-F1-83.                                                NC1054.2
220800     MOVE "MOVE-TEST-F1-82" TO PAR-NAME.                          NC1054.2
220900     PERFORM PRINT-DETAIL.                                        NC1054.2
221000 MOVE-INIT-F1-84.                                                 NC1054.2
221100 MOVE-TEST-F1-84-0.                                               NC1054.2
221200     MOVE 019823 TO AE-0002.                                      NC1054.2
221300 MOVE-TEST-F1-84-1.                                               NC1054.2
221400     IF GRP-AE-0002 EQUAL TO "01098 23 "                          NC1054.2
221500         PERFORM PASS                                             NC1054.2
221600         GO TO MOVE-WRITE-F1-84.                                  NC1054.2
221700     GO TO MOVE-FAIL-F1-84.                                       NC1054.2
221800 MOVE-DELETE-F1-84.                                               NC1054.2
221900     PERFORM DE-LETE.                                             NC1054.2
222000     GO TO MOVE-WRITE-F1-84.                                      NC1054.2
222100 MOVE-FAIL-F1-84.                                                 NC1054.2
222200     MOVE "01098 23 " TO CORRECT-A.                               NC1054.2
222300     MOVE GRP-AE-0002 TO COMPUTED-A.                              NC1054.2
222400     PERFORM FAIL.                                                NC1054.2
222500 MOVE-WRITE-F1-84.                                                NC1054.2
222600     MOVE "MOVE-TEST-F1-84" TO PAR-NAME.                          NC1054.2
222700     PERFORM PRINT-DETAIL.                                        NC1054.2
222800 MOVE-INIT-F1-85.                                                 NC1054.2
222900 MOVE-TEST-F1-85-0.                                               NC1054.2
223000     MOVE 9876543210 TO WRK-DU-10V00.                             NC1054.2
223100 MOVE-TEST-F1-85-1.                                               NC1054.2
223200     IF GRP-WRK-DU-10V00 EQUAL TO "9876543210"                    NC1054.2
223300         PERFORM PASS                                             NC1054.2
223400         GO TO MOVE-WRITE-F1-85.                                  NC1054.2
223500     GO TO MOVE-FAIL-F1-85.                                       NC1054.2
223600 MOVE-DELETE-F1-85.                                               NC1054.2
223700     PERFORM DE-LETE.                                             NC1054.2
223800     GO TO MOVE-WRITE-F1-85.                                      NC1054.2
223900 MOVE-FAIL-F1-85.                                                 NC1054.2
224000     MOVE "9876543210" TO CORRECT-A.                              NC1054.2
224100     MOVE GRP-WRK-DU-10V00 TO COMPUTED-A.                         NC1054.2
224200     PERFORM FAIL.                                                NC1054.2
224300 MOVE-WRITE-F1-85.                                                NC1054.2
224400     MOVE "MOVE-TEST-F1-85" TO PAR-NAME.                          NC1054.2
224500     PERFORM PRINT-DETAIL.                                        NC1054.2
224600 MOVE-INIT-F1-86.                                                 NC1054.2
224700 MOVE-TEST-F1-86-0.                                               NC1054.2
224800     MOVE 00012345 TO NE-0002.                                    NC1054.2
224900 MOVE-TEST-F1-86-1.                                               NC1054.2
225000     IF GRP-NE-0002 EQUAL TO "     12,345"                        NC1054.2
225100         PERFORM PASS                                             NC1054.2
225200         GO TO MOVE-WRITE-F1-86.                                  NC1054.2
225300     GO TO MOVE-FAIL-F1-86.                                       NC1054.2
225400 MOVE-DELETE-F1-86.                                               NC1054.2
225500     PERFORM DE-LETE.                                             NC1054.2
225600     GO TO MOVE-WRITE-F1-86.                                      NC1054.2
225700 MOVE-FAIL-F1-86.                                                 NC1054.2
225800     MOVE "     12,345" TO CORRECT-A.                             NC1054.2
225900     MOVE GRP-NE-0002 TO COMPUTED-A.                              NC1054.2
226000     PERFORM FAIL.                                                NC1054.2
226100 MOVE-WRITE-F1-86.                                                NC1054.2
226200     MOVE "MOVE-TEST-F1-86" TO PAR-NAME.                          NC1054.2
226300     PERFORM PRINT-DETAIL.                                        NC1054.2
226400 MOVE-INIT-F1-87.                                                 NC1054.2
226500 MOVE-TEST-F1-87-0.                                               NC1054.2
226600     MOVE 000011.1223 TO NE-0001.                                 NC1054.2
226700 MOVE-TEST-F1-87-1.                                               NC1054.2
226800     IF GRP-NE-0001 EQUAL TO "    011.122,3"                      NC1054.2
226900         PERFORM PASS                                             NC1054.2
227000         GO TO MOVE-WRITE-F1-87.                                  NC1054.2
227100     GO TO MOVE-FAIL-F1-87.                                       NC1054.2
227200 MOVE-DELETE-F1-87.                                               NC1054.2
227300     PERFORM DE-LETE.                                             NC1054.2
227400     GO TO MOVE-WRITE-F1-87.                                      NC1054.2
227500 MOVE-FAIL-F1-87.                                                 NC1054.2
227600     MOVE "    011.122,3" TO CORRECT-A.                           NC1054.2
227700     MOVE GRP-NE-0001 TO COMPUTED-A.                              NC1054.2
227800     PERFORM FAIL.                                                NC1054.2
227900 MOVE-WRITE-F1-87.                                                NC1054.2
228000     MOVE "MOVE-TEST-F1-87" TO PAR-NAME.                          NC1054.2
228100     PERFORM PRINT-DETAIL.                                        NC1054.2
228200 MOVE-INIT-F1-88.                                                 NC1054.2
228300     MOVE   +60666 TO SPOS-LIT1.                                  NC1054.2
228400 MOVE-TEST-F1-88-0.                                               NC1054.2
228500     MOVE SPOS-LIT1 TO NUMERIC-LIT.                               NC1054.2
228600 MOVE-TEST-F1-88-1.                                               NC1054.2
228700     IF NUMERIC-LIT EQUAL TO "60666"                              NC1054.2
228800         PERFORM PASS                                             NC1054.2
228900         GO TO MOVE-WRITE-F1-88.                                  NC1054.2
229000     MOVE GRP-LEV-NUMERIC TO COMPUTED-A.                          NC1054.2
229100     MOVE 60666 TO CORRECT-A.                                     NC1054.2
229200     PERFORM FAIL.                                                NC1054.2
229300     GO TO MOVE-WRITE-F1-88.                                      NC1054.2
229400 MOVE-DELETE-F1-88.                                               NC1054.2
229500     PERFORM DE-LETE.                                             NC1054.2
229600 MOVE-WRITE-F1-88.                                                NC1054.2
229700     MOVE "MOVE-TEST-F1-88" TO PAR-NAME.                          NC1054.2
229800     PERFORM PRINT-DETAIL.                                        NC1054.2
229900 MOVE-INIT-F1-89.                                                 NC1054.2
230000     MOVE   -70717 TO SPOS-LIT1.                                  NC1054.2
230100 MOVE-TEST-F1-89-0.                                               NC1054.2
230200     MOVE SNEG-LIT1 TO NUMERIC-LIT.                               NC1054.2
230300 MOVE-TEST-F1-89-1.                                               NC1054.2
230400     IF NUMERIC-LIT EQUAL TO "70717"                              NC1054.2
230500         PERFORM PASS                                             NC1054.2
230600         GO TO MOVE-WRITE-F1-89.                                  NC1054.2
230700     MOVE GRP-LEV-NUMERIC TO COMPUTED-A.                          NC1054.2
230800     MOVE 70717 TO CORRECT-A.                                     NC1054.2
230900     PERFORM FAIL.                                                NC1054.2
231000     GO TO MOVE-WRITE-F1-89.                                      NC1054.2
231100 MOVE-DELETE-F1-89.                                               NC1054.2
231200     PERFORM DE-LETE.                                             NC1054.2
231300 MOVE-WRITE-F1-89.                                                NC1054.2
231400     MOVE "MOVE-TEST-F1-89" TO PAR-NAME.                          NC1054.2
231500     PERFORM PRINT-DETAIL.                                        NC1054.2
231600 MOVE-INIT-F1-90.                                                 NC1054.2
231700     MOVE   +60667 TO SPOS-LIT2.                                  NC1054.2
231800 MOVE-TEST-F1-90-0.                                               NC1054.2
231900     MOVE SPOS-LIT2 TO NUMERIC-LIT.                               NC1054.2
232000 MOVE-TEST-F1-90-1.                                               NC1054.2
232100     IF NUMERIC-LIT EQUAL TO 60667                                NC1054.2
232200         PERFORM PASS                                             NC1054.2
232300         GO TO MOVE-WRITE-F1-90.                                  NC1054.2
232400     MOVE GRP-LEV-NUMERIC TO COMPUTED-A.                          NC1054.2
232500     MOVE 60667 TO CORRECT-A.                                     NC1054.2
232600     PERFORM FAIL.                                                NC1054.2
232700     GO TO MOVE-WRITE-F1-90.                                      NC1054.2
232800 MOVE-DELETE-F1-90.                                               NC1054.2
232900     PERFORM DE-LETE.                                             NC1054.2
233000 MOVE-WRITE-F1-90.                                                NC1054.2
233100     MOVE "MOVE-TEST-F1-90" TO PAR-NAME.                          NC1054.2
233200     PERFORM PRINT-DETAIL.                                        NC1054.2
233300 MOVE-INIT-F1-91.                                                 NC1054.2
233400     MOVE   -70718 TO SNEG-LIT2.                                  NC1054.2
233500 MOVE-TEST-F1-91-0.                                               NC1054.2
233600     MOVE    SNEG-LIT2 TO NUMERIC-LIT.                            NC1054.2
233700 MOVE-TEST-F1-91-1.                                               NC1054.2
233800     IF      NUMERIC-LIT EQUAL TO 70718                           NC1054.2
233900         PERFORM PASS                                             NC1054.2
234000         GO TO MOVE-WRITE-F1-91.                                  NC1054.2
234100     MOVE "+S9 MOVED TO PICTURE X " TO RE-MARK.                   NC1054.2
234200     MOVE NUMERIC-LIT TO COMPUTED-A.                              NC1054.2
234300     MOVE "70718" TO CORRECT-A.                                   NC1054.2
234400     PERFORM FAIL.                                                NC1054.2
234500     GO TO MOVE-WRITE-F1-91.                                      NC1054.2
234600 MOVE-DELETE-F1-91.                                               NC1054.2
234700     PERFORM DE-LETE.                                             NC1054.2
234800 MOVE-WRITE-F1-91.                                                NC1054.2
234900     MOVE "MOVE-TEST-F1-91" TO PAR-NAME.                          NC1054.2
235000     PERFORM PRINT-DETAIL.                                        NC1054.2
235100 MOVE-INIT-F1-92.                                                 NC1054.2
235200     MOVE   +60666     TO SPOS-LIT1.                              NC1054.2
235300 MOVE-TEST-F1-92-0.                                               NC1054.2
235400     MOVE    SPOS-LIT1 TO ALPHA-LIT.                              NC1054.2
235500 MOVE-TEST-F1-92-1.                                               NC1054.2
235600     IF      ALPHA-LIT EQUAL TO "60666"                           NC1054.2
235700              PERFORM PASS                                        NC1054.2
235800              GO TO MOVE-WRITE-F1-92.                             NC1054.2
235900     MOVE ALPHA-LIT TO COMPUTED-A.                                NC1054.2
236000     MOVE "60666" TO CORRECT-A.                                   NC1054.2
236100     MOVE "SIGN SHOULD NOT BE MOVED" TO RE-MARK.                  NC1054.2
236200     PERFORM FAIL.                                                NC1054.2
236300     GO TO MOVE-WRITE-F1-92.                                      NC1054.2
236400 MOVE-DELETE-F1-92.                                               NC1054.2
236500     PERFORM DE-LETE.                                             NC1054.2
236600 MOVE-WRITE-F1-92.                                                NC1054.2
236700     MOVE "MOVE-TEST-F1-92" TO PAR-NAME.                          NC1054.2
236800     PERFORM PRINT-DETAIL.                                        NC1054.2
236900 MOVE-INIT-F1-93.                                                 NC1054.2
237000     MOVE  -70717   TO SNEG-LIT1.                                 NC1054.2
237100 MOVE-TEST-F1-93-0.                                               NC1054.2
237200     MOVE SNEG-LIT1 TO ALPHA-LIT.                                 NC1054.2
237300 MOVE-TEST-F1-93-1.                                               NC1054.2
237400     IF ALPHA-LIT EQUAL TO "70717"                                NC1054.2
237500         PERFORM PASS                                             NC1054.2
237600         GO TO MOVE-WRITE-F1-93.                                  NC1054.2
237700     MOVE ALPHA-LIT TO COMPUTED-A.                                NC1054.2
237800     MOVE "70717" TO CORRECT-A.                                   NC1054.2
237900     MOVE "SIGN SHOULD NOT BE MOVED" TO RE-MARK.                  NC1054.2
238000     PERFORM FAIL.                                                NC1054.2
238100     GO TO MOVE-WRITE-F1-93.                                      NC1054.2
238200 MOVE-DELETE-F1-93.                                               NC1054.2
238300     PERFORM DE-LETE.                                             NC1054.2
238400 MOVE-WRITE-F1-93.                                                NC1054.2
238500     MOVE "MOVE-TEST-F1-93" TO PAR-NAME.                          NC1054.2
238600     PERFORM PRINT-DETAIL.                                        NC1054.2
238700 MOVE-INIT-F1-94.                                                 NC1054.2
238800     MOVE "JUSTIFIED MOVES    " TO FEATURE.                       NC1054.2
238900     MOVE  99 TO GRP-NUMERIC-99.                                  NC1054.2
239000 MOVE-TEST-F1-94-0.                                               NC1054.2
239100     MOVE GRP-NUMERIC-99 TO RECEIVE-1  RECEIVE-4  RECEIVE-5       NC1054.2
239200         RECEIVE-6.                                               NC1054.2
239300 MOVE-TEST-F1-94-1.                                               NC1054.2
239400     IF RECEIVE-1 EQUAL TO "99   "                                NC1054.2
239500         PERFORM PASS                                             NC1054.2
239600         GO TO MOVE-WRITE-F1-94.                                  NC1054.2
239700     MOVE RECEIVE-1 TO COMPUTED-A.                                NC1054.2
239800     MOVE "99   " TO CORRECT-A.                                   NC1054.2
239900     PERFORM FAIL.                                                NC1054.2
240000     GO TO MOVE-WRITE-F1-94.                                      NC1054.2
240100 MOVE-DELETE-F1-94.                                               NC1054.2
240200     PERFORM DE-LETE.                                             NC1054.2
240300 MOVE-WRITE-F1-94.                                                NC1054.2
240400     MOVE "MOVE-TEST-F1-94" TO PAR-NAME.                          NC1054.2
240500     PERFORM PRINT-DETAIL.                                        NC1054.2
240600 MOVE-TEST-F1-95.                                                 NC1054.2
240700     IF RECEIVE-4 EQUAL TO 99.00                                  NC1054.2
240800         PERFORM PASS                                             NC1054.2
240900         GO TO MOVE-WRITE-F1-95.                                  NC1054.2
241000     MOVE 99.00 TO CORRECT-N.                                     NC1054.2
241100     MOVE RECEIVE-4 TO COMPUTED-N.                                NC1054.2
241200     PERFORM FAIL.                                                NC1054.2
241300     GO TO MOVE-WRITE-F1-95.                                      NC1054.2
241400 MOVE-DELETE-F1-95.                                               NC1054.2
241500     PERFORM DE-LETE.                                             NC1054.2
241600 MOVE-WRITE-F1-95.                                                NC1054.2
241700     MOVE "MOVE-TEST-F1-95" TO PAR-NAME.                          NC1054.2
241800     PERFORM PRINT-DETAIL.                                        NC1054.2
241900 MOVE-TEST-F1-96.                                                 NC1054.2
242000     IF RECEIVE-5 EQUAL TO "99  "                                 NC1054.2
242100         PERFORM PASS                                             NC1054.2
242200         GO TO MOVE-WRITE-F1-96.                                  NC1054.2
242300     MOVE RECEIVE-5 TO COMPUTED-A.                                NC1054.2
242400     MOVE "99  " TO CORRECT-A.                                    NC1054.2
242500     PERFORM FAIL.                                                NC1054.2
242600     GO TO MOVE-WRITE-F1-96.                                      NC1054.2
242700 MOVE-DELETE-F1-96.                                               NC1054.2
242800     PERFORM DE-LETE.                                             NC1054.2
242900 MOVE-WRITE-F1-96.                                                NC1054.2
243000     MOVE "MOVE-TEST-F1-96" TO PAR-NAME.                          NC1054.2
243100     PERFORM PRINT-DETAIL.                                        NC1054.2
243200 MOVE-TEST-F1-97.                                                 NC1054.2
243300     IF RECEIVE-6 EQUAL TO "99   "                                NC1054.2
243400         PERFORM PASS                                             NC1054.2
243500         GO TO MOVE-WRITE-F1-97.                                  NC1054.2
243600     MOVE RECEIVE-6 TO COMPUTED-A.                                NC1054.2
243700     MOVE "99   " TO CORRECT-A.                                   NC1054.2
243800     PERFORM FAIL.                                                NC1054.2
243900     GO TO MOVE-WRITE-F1-97.                                      NC1054.2
244000 MOVE-DELETE-F1-97.                                               NC1054.2
244100     PERFORM DE-LETE.                                             NC1054.2
244200 MOVE-WRITE-F1-97.                                                NC1054.2
244300     MOVE "MOVE-TEST-F1-97" TO PAR-NAME.                          NC1054.2
244400     PERFORM PRINT-DETAIL.                                        NC1054.2
244500 MOVE-INIT-F1-98.                                                 NC1054.2
244600     MOVE "MOVE (COMP/DISPLAY)" TO FEATURE.                       NC1054.2
244700     MOVE 798 TO WRK-CS-18V00.                                    NC1054.2
244800 MOVE-TEST-F1-98-0.                                               NC1054.2
244900     MOVE WRK-CS-18V00 TO WRK-DS-18V00.                           NC1054.2
245000 MOVE-TEST-F1-98-1.                                               NC1054.2
245100     IF WRK-DS-18V00 EQUAL TO WRK-CS-18V00                        NC1054.2
245200         PERFORM PASS                                             NC1054.2
245300         GO TO MOVE-WRITE-F1-98.                                  NC1054.2
245400     MOVE WRK-CS-18V00 TO CORRECT-18V0.                           NC1054.2
245500     MOVE WRK-DS-18V00 TO COMPUTED-18V0.                          NC1054.2
245600     PERFORM FAIL.                                                NC1054.2
245700     MOVE "FIELDS COMPARED UNEQUAL" TO RE-MARK.                   NC1054.2
245800     GO TO MOVE-WRITE-F1-98.                                      NC1054.2
245900 MOVE-DELETE-F1-98.                                               NC1054.2
246000     PERFORM DE-LETE.                                             NC1054.2
246100 MOVE-WRITE-F1-98.                                                NC1054.2
246200     MOVE "MOVE-TEST-F1-98" TO PAR-NAME.                          NC1054.2
246300     PERFORM PRINT-DETAIL.                                        NC1054.2
246400 MOVE-INIT-F1-99.                                                 NC1054.2
246500     MOVE 798 TO WRK-CS-18V00.                                    NC1054.2
246600 MOVE-TEST-F1-99-0.                                               NC1054.2
246700     MOVE     WRK-CS-18V00 TO WRK-DS-10V00.                       NC1054.2
246800 MOVE-TEST-F1-99-1.                                               NC1054.2
246900     IF WRK-DS-10V00 EQUAL TO WRK-CS-18V00                        NC1054.2
247000         PERFORM PASS                                             NC1054.2
247100         GO TO MOVE-WRITE-F1-99.                                  NC1054.2
247200     MOVE WRK-DS-10V00 TO CORRECT-18V0.                           NC1054.2
247300     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1054.2
247400     PERFORM FAIL.                                                NC1054.2
247500     GO TO MOVE-WRITE-F1-99.                                      NC1054.2
247600 MOVE-DELETE-F1-99.                                               NC1054.2
247700     PERFORM DE-LETE.                                             NC1054.2
247800 MOVE-WRITE-F1-99.                                                NC1054.2
247900     MOVE "MOVE-TEST-F1-99" TO PAR-NAME.                          NC1054.2
248000     PERFORM PRINT-DETAIL.                                        NC1054.2
248100 MOVE-INIT-F1-100.                                                NC1054.2
248200     MOVE 7 TO WRK-DS-18V00.                                      NC1054.2
248300 MOVE-TEST-F1-100-0.                                              NC1054.2
248400     MOVE WRK-DS-18V00 TO WRK-CS-01V00.                           NC1054.2
248500 MOVE-TEST-F1-100-1.                                              NC1054.2
248600     IF WRK-CS-01V00 EQUAL TO WRK-DS-18V00                        NC1054.2
248700         PERFORM PASS                                             NC1054.2
248800         GO TO MOVE-WRITE-F1-100.                                 NC1054.2
248900     MOVE WRK-DS-18V00 TO COMPUTED-18V0                           NC1054.2
249000     MOVE WRK-CS-01V00 TO CORRECT-18V0.                           NC1054.2
249100     PERFORM FAIL.                                                NC1054.2
249200     GO TO MOVE-WRITE-F1-100.                                     NC1054.2
249300 MOVE-DELETE-F1-100.                                              NC1054.2
249400     PERFORM DE-LETE.                                             NC1054.2
249500 MOVE-WRITE-F1-100.                                               NC1054.2
249600     MOVE "MOVE-TEST-F1-100" TO PAR-NAME.                         NC1054.2
249700     PERFORM PRINT-DETAIL.                                        NC1054.2
249800 MOVE-INIT-F1-101.                                                NC1054.2
249900     MOVE 0123456789 TO WRK-DS-10V00.                             NC1054.2
250000 MOVE-TEST-F1-101-0.                                              NC1054.2
250100     MOVE WRK-DS-10V00 TO WRK-CS-18V00.                           NC1054.2
250200 MOVE-TEST-F1-101-1.                                              NC1054.2
250300     IF WRK-DS-10V00 EQUAL TO WRK-CS-18V00                        NC1054.2
250400         PERFORM PASS                                             NC1054.2
250500         GO TO MOVE-WRITE-F1-101.                                 NC1054.2
250600     MOVE WRK-DS-10V00 TO COMPUTED-18V0.                          NC1054.2
250700     MOVE WRK-CS-18V00 TO CORRECT-18V0.                           NC1054.2
250800     MOVE "FIELDS COMPARED UNEQUAL" TO RE-MARK.                   NC1054.2
250900     PERFORM FAIL.                                                NC1054.2
251000     GO TO MOVE-WRITE-F1-101.                                     NC1054.2
251100 MOVE-DELETE-F1-101.                                              NC1054.2
251200     PERFORM DE-LETE.                                             NC1054.2
251300 MOVE-WRITE-F1-101.                                               NC1054.2
251400     MOVE "MOVE-TEST-F1-101" TO PAR-NAME.                         NC1054.2
251500     PERFORM PRINT-DETAIL.                                        NC1054.2
251600 MOVE-INIT-F1-102.                                                NC1054.2
251700     MOVE 3 TO WRK-CS-18V00.                                      NC1054.2
251800 MOVE-TEST-F1-102-0.                                              NC1054.2
251900     MOVE WRK-CS-18V00 TO WRK-DS-01V00.                           NC1054.2
252000 MOVE-TEST-F1-102-1.                                              NC1054.2
252100     IF WRK-CS-18V00 EQUAL TO WRK-DS-01V00                        NC1054.2
252200         PERFORM PASS                                             NC1054.2
252300         GO TO MOVE-WRITE-F1-102.                                 NC1054.2
252400     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1054.2
252500     MOVE WRK-DS-01V00 TO CORRECT-18V0.                           NC1054.2
252600     MOVE "FIELDS COMPARED UNEQUAL" TO RE-MARK.                   NC1054.2
252700     PERFORM FAIL.                                                NC1054.2
252800     GO TO MOVE-WRITE-F1-102.                                     NC1054.2
252900 MOVE-DELETE-F1-102.                                              NC1054.2
253000     PERFORM DE-LETE.                                             NC1054.2
253100 MOVE-WRITE-F1-102.                                               NC1054.2
253200     MOVE "MOVE-TEST-F1-102" TO PAR-NAME.                         NC1054.2
253300     PERFORM PRINT-DETAIL.                                        NC1054.2
253400 MOVE-INIT-F1-103.                                                NC1054.2
253500     MOVE    832.553 TO WRK-CS-08V08.                             NC1054.2
253600 MOVE-TEST-F1-103-0.                                              NC1054.2
253700     MOVE WRK-CS-08V08 TO WRK-EDIT-Z3VZ3.                         NC1054.2
253800 MOVE-TEST-F1-103-1.                                              NC1054.2
253900     IF WRK-EDIT-Z3VZ3 EQUAL TO "832.553"                         NC1054.2
254000         PERFORM PASS                                             NC1054.2
254100         GO TO MOVE-WRITE-F1-103.                                 NC1054.2
254200     MOVE "832.553" TO CORRECT-A.                                 NC1054.2
254300     MOVE WRK-EDIT-Z3VZ3 TO COMPUTED-A.                           NC1054.2
254400     PERFORM FAIL.                                                NC1054.2
254500     GO TO MOVE-WRITE-F1-103.                                     NC1054.2
254600 MOVE-DELETE-F1-103.                                              NC1054.2
254700     PERFORM DE-LETE.                                             NC1054.2
254800 MOVE-WRITE-F1-103.                                               NC1054.2
254900     MOVE "MOVE-TEST-F1-103" TO PAR-NAME.                         NC1054.2
255000     PERFORM PRINT-DETAIL.                                        NC1054.2
255100 MOVE-INIT-F1-104.                                                NC1054.2
255200     MOVE    6382.47 TO WRK-CS-08V08.                             NC1054.2
255300 MOVE-TEST-F1-104-0.                                              NC1054.2
255400     MOVE WRK-CS-04V08 TO WRK-EDIT-05V02.                         NC1054.2
255500 MOVE-TEST-F1-104-1.                                              NC1054.2
255600     IF       WRK-EDIT-05V02 EQUAL TO " 06382.47"                 NC1054.2
255700         PERFORM PASS                                             NC1054.2
255800         GO TO MOVE-WRITE-F1-104.                                 NC1054.2
255900     MOVE     " 06382.47" TO CORRECT-A.                           NC1054.2
256000     MOVE WRK-EDIT-05V02 TO COMPUTED-A.                           NC1054.2
256100     PERFORM FAIL.                                                NC1054.2
256200     GO TO MOVE-WRITE-F1-104.                                     NC1054.2
256300 MOVE-DELETE-F1-104.                                              NC1054.2
256400     PERFORM DE-LETE.                                             NC1054.2
256500 MOVE-WRITE-F1-104.                                               NC1054.2
256600     MOVE "MOVE-TEST-F1-104" TO PAR-NAME.                         NC1054.2
256700     PERFORM PRINT-DETAIL.                                        NC1054.2
256800 MOVE-INIT-F1-105.                                                NC1054.2
256900     MOVE    832.553 TO WRK-CS-08V08.                             NC1054.2
257000 MOVE-TEST-F1-105-0.                                              NC1054.2
257100     MOVE WRK-CS-08V08 TO WRK-EDIT-05V00.                         NC1054.2
257200 MOVE-TEST-F1-105-1.                                              NC1054.2
257300     IF WRK-EDIT-05V00 EQUAL TO "**832"                           NC1054.2
257400         PERFORM PASS                                             NC1054.2
257500         GO TO MOVE-WRITE-F1-105.                                 NC1054.2
257600     MOVE "**832" TO CORRECT-A.                                   NC1054.2
257700     MOVE WRK-EDIT-05V00 TO COMPUTED-A.                           NC1054.2
257800     PERFORM FAIL.                                                NC1054.2
257900     GO TO MOVE-WRITE-F1-105.                                     NC1054.2
258000 MOVE-DELETE-F1-105.                                              NC1054.2
258100     PERFORM DE-LETE.                                             NC1054.2
258200 MOVE-WRITE-F1-105.                                               NC1054.2
258300     MOVE "MOVE-TEST-F1-105" TO PAR-NAME.                         NC1054.2
258400     PERFORM PRINT-DETAIL.                                        NC1054.2
258500 MOVE-INIT-F1-106.                                                NC1054.2
258600     MOVE 6382.47 TO WRK-CS-04V08.                                NC1054.2
258700 MOVE-TEST-F1-106-0.                                              NC1054.2
258800     MOVE WRK-CS-04V08 TO WRK-EDIT-05V02.                         NC1054.2
258900 MOVE-TEST-F1-106-1.                                              NC1054.2
259000     IF WRK-EDIT-05V02 EQUAL TO " 06382.47"                       NC1054.2
259100         PERFORM PASS                                             NC1054.2
259200         GO TO MOVE-WRITE-F1-106.                                 NC1054.2
259300     MOVE WRK-EDIT-05V02 TO COMPUTED-A.                           NC1054.2
259400     MOVE " 06382.47" TO CORRECT-A.                               NC1054.2
259500     PERFORM FAIL.                                                NC1054.2
259600     GO TO MOVE-WRITE-F1-106.                                     NC1054.2
259700 MOVE-DELETE-F1-106.                                              NC1054.2
259800     PERFORM DE-LETE.                                             NC1054.2
259900 MOVE-WRITE-F1-106.                                               NC1054.2
260000     MOVE "MOVE-TEST-F1-106" TO PAR-NAME.                         NC1054.2
260100     PERFORM PRINT-DETAIL.                                        NC1054.2
260200 MOVE-INIT-F1-107.                                                NC1054.2
260300     MOVE ZERO TO WRK-CS-18V00.                                   NC1054.2
260400 MOVE-TEST-F1-107-0.                                              NC1054.2
260500     MOVE WRK-CS-18V00 TO WRK-EDIT-18V00.                         NC1054.2
260600 MOVE-TEST-F1-107-1.                                              NC1054.2
260700     IF WRK-EDIT-18V00 EQUAL TO "                 0"              NC1054.2
260800         PERFORM PASS                                             NC1054.2
260900         GO TO MOVE-WRITE-F1-107.                                 NC1054.2
261000     MOVE "                 0" TO CORRECT-A.                      NC1054.2
261100     MOVE WRK-EDIT-18V00 TO COMPUTED-A.                           NC1054.2
261200     PERFORM FAIL.                                                NC1054.2
261300     GO TO MOVE-WRITE-F1-107.                                     NC1054.2
261400 MOVE-DELETE-F1-107.                                              NC1054.2
261500     PERFORM DE-LETE.                                             NC1054.2
261600 MOVE-WRITE-F1-107.                                               NC1054.2
261700     MOVE "MOVE-TEST-F1-107" TO PAR-NAME.                         NC1054.2
261800     PERFORM PRINT-DETAIL.                                        NC1054.2
261900 MOVE-INIT-F1-108.                                                NC1054.2
262000     MOVE "MOVE (DISPLAY/COMP)" TO FEATURE.                       NC1054.2
262100     MOVE 15 TO WRK-DS-10V00.                                     NC1054.2
262200 MOVE-TEST-F1-108-0.                                              NC1054.2
262300     MOVE WRK-DS-10V00 TO WRK-CS-01V00.                           NC1054.2
262400 MOVE-TEST-F1-108-1.                                              NC1054.2
262500     IF WRK-CS-01V00 EQUAL TO 5                                   NC1054.2
262600         PERFORM PASS                                             NC1054.2
262700         GO TO MOVE-WRITE-F1-108.                                 NC1054.2
262800     MOVE 5 TO CORRECT-N.                                         NC1054.2
262900     MOVE WRK-CS-01V00 TO COMPUTED-N.                             NC1054.2
263000     PERFORM FAIL.                                                NC1054.2
263100     GO TO MOVE-WRITE-F1-108.                                     NC1054.2
263200 MOVE-DELETE-F1-108.                                              NC1054.2
263300     PERFORM DE-LETE.                                             NC1054.2
263400 MOVE-WRITE-F1-108.                                               NC1054.2
263500     MOVE "MOVE-TEST-F1-108" TO PAR-NAME.                         NC1054.2
263600     PERFORM PRINT-DETAIL.                                        NC1054.2
263700 MOVE-INIT-F1-109.                                                NC1054.2
263800     MOVE 1023 TO WRK-DS-10V00.                                   NC1054.2
263900 MOVE-TEST-F1-109-0.                                              NC1054.2
264000     MOVE WRK-DS-10V00 TO WRK-CS-03V00.                           NC1054.2
264100 MOVE-TEST-F1-109-1.                                              NC1054.2
264200     IF WRK-CS-03V00 EQUAL TO 023                                 NC1054.2
264300         PERFORM PASS                                             NC1054.2
264400         GO TO MOVE-WRITE-F1-109.                                 NC1054.2
264500     MOVE WRK-CS-03V00 TO COMPUTED-N.                             NC1054.2
264600     MOVE 023 TO CORRECT-N.                                       NC1054.2
264700     PERFORM FAIL.                                                NC1054.2
264800     GO TO MOVE-WRITE-F1-109.                                     NC1054.2
264900 MOVE-DELETE-F1-109.                                              NC1054.2
265000     PERFORM DE-LETE.                                             NC1054.2
265100 MOVE-WRITE-F1-109.                                               NC1054.2
265200     MOVE "MOVE-TEST-F1-109" TO PAR-NAME.                         NC1054.2
265300     PERFORM PRINT-DETAIL.                                        NC1054.2
265400 MOVE-INIT-F1-110.                                                NC1054.2
265500     MOVE     SPACE TO MOVE71.                                    NC1054.2
265600 MOVE-TEST-F1-110-0.                                              NC1054.2
265700     MOVE     00000 TO MOVE71.                                    NC1054.2
265800 MOVE-TEST-F1-110-1.                                              NC1054.2
265900     IF       MOVE71 EQUAL TO "00000               "              NC1054.2
266000              PERFORM PASS GO TO MOVE-WRITE-F1-110.               NC1054.2
266100     GO       TO MOVE-FAIL-F1-110.                                NC1054.2
266200 MOVE-DELETE-F1-110.                                              NC1054.2
266300     PERFORM  DE-LETE.                                            NC1054.2
266400     GO       TO MOVE-WRITE-F1-110.                               NC1054.2
266500 MOVE-FAIL-F1-110.                                                NC1054.2
266600     PERFORM  FAIL.                                               NC1054.2
266700     MOVE     MOVE71 TO COMPUTED-A.                               NC1054.2
266800     MOVE     "00000               " TO CORRECT-A.                NC1054.2
266900 MOVE-WRITE-F1-110.                                               NC1054.2
267000     MOVE     "MOVE NUMERIC" TO FEATURE.                          NC1054.2
267100     MOVE     "MOVE-TEST-F1-110" TO PAR-NAME.                     NC1054.2
267200     PERFORM  PRINT-DETAIL.                                       NC1054.2
267300 MOVE-INIT-F1-111.                                                NC1054.2
267400     MOVE     234565432.1 TO MOVE74.                              NC1054.2
267500 MOVE-TEST-F1-111-0.                                              NC1054.2
267600     MOVE     MOVE74 TO MOVE75.                                   NC1054.2
267700 MOVE-TEST-F1-111.                                                NC1054.2
267800     IF       MOVE75 EQUAL TO 234565432                           NC1054.2
267900              PERFORM PASS GO TO MOVE-WRITE-F1-111.               NC1054.2
268000     GO       TO MOVE-FAIL-F1-111.                                NC1054.2
268100 MOVE-DELETE-F1-111.                                              NC1054.2
268200     PERFORM  DE-LETE.                                            NC1054.2
268300     GO       TO MOVE-WRITE-F1-111.                               NC1054.2
268400 MOVE-FAIL-F1-111.                                                NC1054.2
268500     MOVE     MOVE75 TO COMPUTED-N.                               NC1054.2
268600     MOVE     234565432 TO CORRECT-N.                             NC1054.2
268700     PERFORM  FAIL.                                               NC1054.2
268800 MOVE-WRITE-F1-111.                                               NC1054.2
268900     MOVE     "MOVE -- COMP, SYNC" TO FEATURE.                    NC1054.2
269000     MOVE     "MOVE-TEST-F1-111" TO PAR-NAME.                     NC1054.2
269100     PERFORM  PRINT-DETAIL.                                       NC1054.2
269200 MOVE-INIT-F1-112.                                                NC1054.2
269300     MOVE "MOVE TO COMP (ABS)" TO FEATURE.                        NC1054.2
269400     MOVE  +60666 TO SPOS-LIT1.                                   NC1054.2
269500 MOVE-TEST-F1-112-0.                                              NC1054.2
269600     MOVE SPOS-LIT1 TO CU-05V00-001.                              NC1054.2
269700 MOVE-TEST-F1-112-1.                                              NC1054.2
269800     IF       CU-05V00-001 EQUAL TO 60666                         NC1054.2
269900             PERFORM PASS                                         NC1054.2
270000             GO TO MOVE-WRITE-F1-112.                             NC1054.2
270100     MOVE    CU-05V00-001 TO COMPUTED-18V0.                       NC1054.2
270200     MOVE     60666 TO CORRECT-18V0.                              NC1054.2
270300     PERFORM FAIL.                                                NC1054.2
270400     GO TO   MOVE-WRITE-F1-112.                                   NC1054.2
270500 MOVE-DELETE-F1-112.                                              NC1054.2
270600     PERFORM DE-LETE.                                             NC1054.2
270700 MOVE-WRITE-F1-112.                                               NC1054.2
270800     MOVE "MOVE-TEST-F1-112" TO PAR-NAME.                         NC1054.2
270900     PERFORM PRINT-DETAIL.                                        NC1054.2
271000 MOVE-INIT-F1-113.                                                NC1054.2
271100     MOVE  +60667 TO SPOS-LIT2.                                   NC1054.2
271200 MOVE-TEST-F1-113-0.                                              NC1054.2
271300     MOVE SPOS-LIT2 TO CU-05V00-001.                              NC1054.2
271400 MOVE-TEST-F1-113-1.                                              NC1054.2
271500     IF       CU-05V00-001 EQUAL TO 60667                         NC1054.2
271600             PERFORM PASS                                         NC1054.2
271700             GO TO MOVE-WRITE-F1-113.                             NC1054.2
271800     MOVE    CU-05V00-001 TO COMPUTED-18V0.                       NC1054.2
271900     MOVE     60667 TO CORRECT-18V0.                              NC1054.2
272000     PERFORM FAIL.                                                NC1054.2
272100     GO TO   MOVE-WRITE-F1-113.                                   NC1054.2
272200 MOVE-DELETE-F1-113.                                              NC1054.2
272300     PERFORM DE-LETE.                                             NC1054.2
272400 MOVE-WRITE-F1-113.                                               NC1054.2
272500     MOVE "MOVE-TEST-F1-113" TO PAR-NAME.                         NC1054.2
272600     PERFORM PRINT-DETAIL.                                        NC1054.2
272700 MOVE-TEST-F1-114.                                                NC1054.2
272800     MOVE SNEG-LIT1 TO CU-05V00-001.                              NC1054.2
272900     IF       CU-05V00-001 EQUAL TO 70717                         NC1054.2
273000             PERFORM PASS                                         NC1054.2
273100             GO TO MOVE-WRITE-F1-114.                             NC1054.2
273200     MOVE    CU-05V00-001 TO COMPUTED-18V0.                       NC1054.2
273300     MOVE     70717 TO CORRECT-18V0.                              NC1054.2
273400     PERFORM FAIL.                                                NC1054.2
273500     GO TO   MOVE-WRITE-F1-114.                                   NC1054.2
273600 MOVE-DELETE-F1-114.                                              NC1054.2
273700     PERFORM DE-LETE.                                             NC1054.2
273800 MOVE-WRITE-F1-114.                                               NC1054.2
273900     MOVE "MOVE-TEST-F1-114" TO PAR-NAME.                         NC1054.2
274000     PERFORM PRINT-DETAIL.                                        NC1054.2
274100 MOVE-INIT-F1-115.                                                NC1054.2
274200     MOVE  -70718 TO SNEG-LIT2.                                   NC1054.2
274300 MOVE-TEST-F1-115-0.                                              NC1054.2
274400     MOVE SNEG-LIT2 TO CU-05V00-001.                              NC1054.2
274500 MOVE-TEST-F1-115.                                                NC1054.2
274600     IF       CU-05V00-001 EQUAL TO 70718                         NC1054.2
274700             PERFORM PASS                                         NC1054.2
274800             GO TO MOVE-WRITE-F1-115.                             NC1054.2
274900     MOVE    CU-05V00-001 TO COMPUTED-18V0.                       NC1054.2
275000     MOVE     70718 TO CORRECT-18V0.                              NC1054.2
275100     PERFORM FAIL.                                                NC1054.2
275200     GO TO   MOVE-WRITE-F1-115.                                   NC1054.2
275300 MOVE-DELETE-F1-115.                                              NC1054.2
275400     PERFORM DE-LETE.                                             NC1054.2
275500 MOVE-WRITE-F1-115.                                               NC1054.2
275600     MOVE "MOVE-TEST-F1-115" TO PAR-NAME.                         NC1054.2
275700     PERFORM PRINT-DETAIL.                                        NC1054.2
275800 MOVE-INIT-F1-116.                                                NC1054.2
275900     MOVE  +60666 TO SPOS-LIT1.                                   NC1054.2
276000 MOVE-TEST-F1-116-0.                                              NC1054.2
276100     MOVE     SPOS-LIT1 TO CS-05V00-001.                          NC1054.2
276200 MOVE-TEST-F1-116-1.                                              NC1054.2
276300     MOVE     CS-05V00-001 TO CU-05V00-001.                       NC1054.2
276400     IF      CU-05V00-001 EQUAL TO 60666                          NC1054.2
276500             PERFORM PASS                                         NC1054.2
276600             GO TO MOVE-WRITE-F1-116.                             NC1054.2
276700     MOVE CU-05V00-001 TO COMPUTED-18V0.                          NC1054.2
276800     MOVE     60666 TO CORRECT-18V0.                              NC1054.2
276900     PERFORM FAIL.                                                NC1054.2
277000     GO TO MOVE-WRITE-F1-116.                                     NC1054.2
277100 MOVE-DELETE-F1-116.                                              NC1054.2
277200     PERFORM  DE-LETE.                                            NC1054.2
277300 MOVE-WRITE-F1-116.                                               NC1054.2
277400     MOVE    "MOVE-TEST-F1-116" TO PAR-NAME.                      NC1054.2
277500     PERFORM  PRINT-DETAIL.                                       NC1054.2
277600 MOVE-INIT-F1-117.                                                NC1054.2
277700     MOVE  +60667 TO SPOS-LIT2.                                   NC1054.2
277800 MOVE-TEST-F1-117-0.                                              NC1054.2
277900     MOVE     SPOS-LIT2 TO CS-05V00-001.                          NC1054.2
278000     MOVE     CS-05V00-001 TO CU-05V00-001.                       NC1054.2
278100 MOVE-TEST-F1-117-1.                                              NC1054.2
278200     IF      CU-05V00-001 EQUAL TO 60667                          NC1054.2
278300             PERFORM PASS                                         NC1054.2
278400             GO TO MOVE-WRITE-F1-117.                             NC1054.2
278500     MOVE CU-05V00-001 TO COMPUTED-18V0.                          NC1054.2
278600     MOVE     60667 TO CORRECT-18V0.                              NC1054.2
278700     PERFORM FAIL.                                                NC1054.2
278800     GO TO MOVE-WRITE-F1-117.                                     NC1054.2
278900 MOVE-DELETE-F1-117.                                              NC1054.2
279000     PERFORM  DE-LETE.                                            NC1054.2
279100 MOVE-WRITE-F1-117.                                               NC1054.2
279200     MOVE    "MOVE-TEST-F1-117" TO PAR-NAME.                      NC1054.2
279300     PERFORM  PRINT-DETAIL.                                       NC1054.2
279400 MOVE-INIT-F1-118.                                                NC1054.2
279500     MOVE  -70717 TO SNEG-LIT1.                                   NC1054.2
279600 MOVE-TEST-F1-118-0.                                              NC1054.2
279700     MOVE     SNEG-LIT1 TO CS-05V00-001.                          NC1054.2
279800     MOVE     CS-05V00-001 TO CU-05V00-001.                       NC1054.2
279900 MOVE-TEST-F1-118-1.                                              NC1054.2
280000     IF      CU-05V00-001 EQUAL TO 70717                          NC1054.2
280100             PERFORM PASS                                         NC1054.2
280200             GO TO MOVE-WRITE-F1-118.                             NC1054.2
280300     MOVE CU-05V00-001 TO COMPUTED-18V0.                          NC1054.2
280400     MOVE     70717 TO CORRECT-18V0.                              NC1054.2
280500     PERFORM FAIL.                                                NC1054.2
280600     GO TO MOVE-WRITE-F1-118.                                     NC1054.2
280700 MOVE-DELETE-F1-118.                                              NC1054.2
280800     PERFORM  DE-LETE.                                            NC1054.2
280900 MOVE-WRITE-F1-118.                                               NC1054.2
281000     MOVE    "MOVE-TEST-F1-118" TO PAR-NAME.                      NC1054.2
281100     PERFORM  PRINT-DETAIL.                                       NC1054.2
281200 MOVE-INIT-F1-119.                                                NC1054.2
281300     MOVE  -70718 TO SNEG-LIT2.                                   NC1054.2
281400 MOVE-TEST-F1-119-0.                                              NC1054.2
281500     MOVE     SNEG-LIT2 TO CS-05V00-001.                          NC1054.2
281600     MOVE     CS-05V00-001 TO CU-05V00-001.                       NC1054.2
281700 MOVE-TEST-F1-119-1.                                              NC1054.2
281800     IF      CU-05V00-001 EQUAL TO 70718                          NC1054.2
281900             PERFORM PASS                                         NC1054.2
282000             GO TO MOVE-WRITE-F1-119.                             NC1054.2
282100     MOVE CU-05V00-001 TO COMPUTED-18V0.                          NC1054.2
282200     MOVE     70718 TO CORRECT-18V0.                              NC1054.2
282300     PERFORM FAIL.                                                NC1054.2
282400     GO TO MOVE-WRITE-F1-119.                                     NC1054.2
282500 MOVE-DELETE-F1-119.                                              NC1054.2
282600     PERFORM  DE-LETE.                                            NC1054.2
282700 MOVE-WRITE-F1-119.                                               NC1054.2
282800     MOVE    "MOVE-TEST-F1-119" TO PAR-NAME.                      NC1054.2
282900     PERFORM  PRINT-DETAIL.                                       NC1054.2
283000*                                                                 NC1054.2
283100*        MOVE-TEST-176 THROUGH MOVE-TEST-178 CONTAIN MOVE         NC1054.2
283200*    STATEMENTS OF THE FORM                                       NC1054.2
283300*            MOVE ALL LITERAL TO NUMERIC DATA ITEM.               NC1054.2
283400*                                                                 NC1054.2
283500*    REFERENCES IN X3.23-1974                                     NC1054.2
283600*            PAGE I-85, 5.3.2.2.2.3(1)                            NC1054.2
283700*            PAGE II-76, 5.15.4(4)B.3                             NC1054.2
283800*                                                                 NC1054.2
283900*MOVE-TEST-176.                                                   NC1054.2
284000*    MOVE ZERO TO MOVE5.                                          NC1054.2
284100*    MOVE ALL "123" TO MOVE5.                                     NC1054.2
284200*    IF MOVE5 EQUAL TO 12                                         NC1054.2
284300*            PERFORM PASS                                         NC1054.2
284400*            GO TO MOVE-WRITE-176                                 NC1054.2
284500*        ELSE GO TO MOVE-FAIL-176.                                NC1054.2
284600 MOVE-DELETE-176.                                                 NC1054.2
284700     PERFORM DE-LETE.                                             NC1054.2
284800     GO TO MOVE-WRITE-176.                                        NC1054.2
284900 MOVE-FAIL-176.                                                   NC1054.2
285000     PERFORM FAIL.                                                NC1054.2
285100     MOVE 12 TO CORRECT-N.                                        NC1054.2
285200     MOVE MOVE5 TO COMPUTED-N.                                    NC1054.2
285300 MOVE-WRITE-176.                                                  NC1054.2
285400     MOVE "*DELETED BY FCCTS*" TO FEATURE.                        NC1054.2
285500     MOVE "MOVE-TEST-176" TO PAR-NAME.                            NC1054.2
285600     PERFORM PRINT-DETAIL.                                        NC1054.2
285700*MOVE-TEST-177.                                                   NC1054.2
285800*    MOVE ZERO TO MOVE5.                                          NC1054.2
285900*    MOVE ALL "ABC123" TO MOVE5.                                  NC1054.2
286000*    IF MOVE5 EQUAL TO 23                                         NC1054.2
286100*            PERFORM PASS                                         NC1054.2
286200*            GO TO MOVE-WRITE-177                                 NC1054.2
286300*        ELSE GO TO MOVE-FAIL-177.                                NC1054.2
286400 MOVE-DELETE-177.                                                 NC1054.2
286500     PERFORM DE-LETE.                                             NC1054.2
286600     GO TO MOVE-WRITE-177.                                        NC1054.2
286700 MOVE-FAIL-177.                                                   NC1054.2
286800     PERFORM FAIL.                                                NC1054.2
286900     MOVE 23 TO CORRECT-N.                                        NC1054.2
287000     MOVE MOVE5 TO COMPUTED-N.                                    NC1054.2
287100 MOVE-WRITE-177.                                                  NC1054.2
287200     MOVE "*DELETED BY FCCTS*" TO FEATURE.                        NC1054.2
287300     MOVE "MOVE-TEST-177" TO PAR-NAME.                            NC1054.2
287400     PERFORM PRINT-DETAIL.                                        NC1054.2
287500*MOVE-TEST-178.                                                   NC1054.2
287600*    MOVE ZERO TO MOVE7.                                          NC1054.2
287700*    MOVE ALL "2A" TO MOVE7.                                      NC1054.2
287800*    IF MOVE7 EQUAL TO 2                                          NC1054.2
287900*            PERFORM PASS                                         NC1054.2
288000*            GO TO MOVE-WRITE-178                                 NC1054.2
288100*        ELSE GO TO MOVE-FAIL-178.                                NC1054.2
288200 MOVE-DELETE-178.                                                 NC1054.2
288300     PERFORM DE-LETE.                                             NC1054.2
288400     GO TO MOVE-WRITE-178.                                        NC1054.2
288500 MOVE-FAIL-178.                                                   NC1054.2
288600     PERFORM FAIL.                                                NC1054.2
288700     MOVE 2 TO CORRECT-N.                                         NC1054.2
288800     MOVE MOVE7 TO COMPUTED-N.                                    NC1054.2
288900 MOVE-WRITE-178.                                                  NC1054.2
289000     MOVE "*DELETED BY FCCTS*" TO FEATURE.                        NC1054.2
289100     MOVE "MOVE-TEST-178" TO PAR-NAME.                            NC1054.2
289200     PERFORM PRINT-DETAIL.                                        NC1054.2
289300     MOVE "EDIT--B(N), 0(N)"  TO FEATURE.                         NC1054.2
289400 EDIT-INIT-F1-120.                                                NC1054.2
289500                                                                  NC1054.2
289600 EDIT-TEST-F1-120-0.                                              NC1054.2
289700     MOVE "926"    TO EDIT-PICTURE-01.                            NC1054.2
289800 EDIT-TEST-F1-120-1.                                              NC1054.2
289900     IF EDIT-PICTURE-01 EQUAL TO  "9               26"            NC1054.2
290000              PERFORM PASS                                        NC1054.2
290100              GO TO EDIT-WRITE-F1-120.                            NC1054.2
290200     PERFORM FAIL.                                                NC1054.2
290300     MOVE EDIT-PICTURE-01 TO COMPUTED-A.                          NC1054.2
290400     MOVE "9               26" TO CORRECT-A.                      NC1054.2
290500     GO TO EDIT-WRITE-F1-120.                                     NC1054.2
290600 EDIT-DELETE-F1-120.                                              NC1054.2
290700     PERFORM DE-LETE.                                             NC1054.2
290800 EDIT-WRITE-F1-120.                                               NC1054.2
290900     MOVE "EDIT-TEST-F1-120" TO PAR-NAME.                         NC1054.2
291000     PERFORM PRINT-DETAIL.                                        NC1054.2
291100 EDIT-INIT-F1-121.                                                NC1054.2
291200                                                                  NC1054.2
291300 EDIT-TEST-F1-121-0.                                              NC1054.2
291400     MOVE "1492"   TO EDIT-PICTURE-02.                            NC1054.2
291500 EDIT-TEST-F1-121-1.                                              NC1054.2
291600     IF EDIT-PICTURE-02 EQUAL TO "$0000000000492"                 NC1054.2
291700              PERFORM PASS                                        NC1054.2
291800              GO TO EDIT-WRITE-F1-121.                            NC1054.2
291900     PERFORM FAIL.                                                NC1054.2
292000     MOVE EDIT-PICTURE-02 TO COMPUTED-A.                          NC1054.2
292100     MOVE "$0000000000492" TO CORRECT-A.                          NC1054.2
292200     GO TO EDIT-WRITE-F1-121.                                     NC1054.2
292300 EDIT-DELETE-F1-121.                                              NC1054.2
292400     PERFORM DE-LETE.                                             NC1054.2
292500 EDIT-WRITE-F1-121.                                               NC1054.2
292600     MOVE "EDIT-TEST-F1-121" TO PAR-NAME.                         NC1054.2
292700     PERFORM PRINT-DETAIL.                                        NC1054.2
292800 EDIT-INIT-F1-122.                                                NC1054.2
292900     MOVE   333 TO EDIT-DATA-1.                                   NC1054.2
293000 EDIT-TEST-F1-122-0.                                              NC1054.2
293100     MOVE EDIT-DATA-1 TO EDIT-PICTURE-01.                         NC1054.2
293200 EDIT-TEST-F1-122-1.                                              NC1054.2
293300     IF EDIT-PICTURE-01 EQUAL TO  "3               33"            NC1054.2
293400              PERFORM PASS                                        NC1054.2
293500              GO TO EDIT-WRITE-F1-122.                            NC1054.2
293600     PERFORM FAIL.                                                NC1054.2
293700     MOVE EDIT-PICTURE-01 TO COMPUTED-A.                          NC1054.2
293800     MOVE "3               33" TO CORRECT-A.                      NC1054.2
293900     GO TO EDIT-WRITE-F1-122.                                     NC1054.2
294000 EDIT-DELETE-F1-122.                                              NC1054.2
294100     PERFORM DE-LETE.                                             NC1054.2
294200 EDIT-WRITE-F1-122.                                               NC1054.2
294300     MOVE "EDIT-TEST-F1-122" TO PAR-NAME.                         NC1054.2
294400     PERFORM PRINT-DETAIL.                                        NC1054.2
294500 EDIT-INIT-F1-123.                                                NC1054.2
294600     MOVE   916 TO EDIT-DATA-2.                                   NC1054.2
294700 EDIT-TEST-F1-123-0.                                              NC1054.2
294800     MOVE EDIT-DATA-2 TO EDIT-PICTURE-02.                         NC1054.2
294900 EDIT-TEST-F1-123-1.                                              NC1054.2
295000     IF EDIT-PICTURE-02 EQUAL TO "$0000000000916"                 NC1054.2
295100              PERFORM PASS                                        NC1054.2
295200              GO TO EDIT-WRITE-F1-123.                            NC1054.2
295300     PERFORM FAIL.                                                NC1054.2
295400     MOVE EDIT-PICTURE-02 TO COMPUTED-A.                          NC1054.2
295500     MOVE "$0000000000916 " TO CORRECT-A.                         NC1054.2
295600     GO TO EDIT-WRITE-F1-123.                                     NC1054.2
295700 EDIT-DELETE-F1-123.                                              NC1054.2
295800     PERFORM DE-LETE.                                             NC1054.2
295900 EDIT-WRITE-F1-123.                                               NC1054.2
296000     MOVE "EDIT-TEST-F1-123" TO PAR-NAME.                         NC1054.2
296100     PERFORM PRINT-DETAIL.                                        NC1054.2
296200 EDIT-INIT-F1-124.                                                NC1054.2
296300     MOVE "EDIT -- MASKED EDIT"      TO FEATURE.                  NC1054.2
296400 EDIT-TEST-F1-124-0.                                              NC1054.2
296500     MOVE  000987.65    TO EDIT-PIC-05.                           NC1054.2
296600 EDIT-TEST-F1-124-1.                                              NC1054.2
296700     IF GRP-EDIT-PIC-05 EQUAL TO "   $987.65"                     NC1054.2
296800              PERFORM PASS                                        NC1054.2
296900              GO TO EDIT-WRITE-F1-124.                            NC1054.2
297000     PERFORM FAIL.                                                NC1054.2
297100     MOVE EDIT-PIC-05    TO COMPUTED-A.                           NC1054.2
297200     MOVE "   $987.65"  TO CORRECT-A.                             NC1054.2
297300     GO TO EDIT-WRITE-F1-124.                                     NC1054.2
297400 EDIT-DELETE-F1-124.                                              NC1054.2
297500     PERFORM DE-LETE.                                             NC1054.2
297600 EDIT-WRITE-F1-124.                                               NC1054.2
297700     MOVE "EDIT-TEST-F1-124"   TO PAR-NAME.                       NC1054.2
297800     PERFORM PRINT-DETAIL.                                        NC1054.2
297900 EDIT-INIT-F1-125.                                                NC1054.2
298000*                                                                 NC1054.2
298100 EDIT-TEST-F1-125-0.                                              NC1054.2
298200     MOVE  000123.45    TO EDIT-PIC-06.                           NC1054.2
298300 EDIT-TEST-F1-125-1.                                              NC1054.2
298400     IF GRP-EDIT-PIC-06 EQUAL TO  "   $123.45"                    NC1054.2
298500              PERFORM PASS                                        NC1054.2
298600              GO TO EDIT-WRITE-F1-125.                            NC1054.2
298700     PERFORM FAIL.                                                NC1054.2
298800     MOVE EDIT-PIC-06    TO COMPUTED-A.                           NC1054.2
298900     MOVE "   $123.45" TO CORRECT-A.                              NC1054.2
299000     GO TO EDIT-WRITE-F1-125.                                     NC1054.2
299100 EDIT-DELETE-F1-125.                                              NC1054.2
299200     PERFORM DE-LETE.                                             NC1054.2
299300 EDIT-WRITE-F1-125.                                               NC1054.2
299400     MOVE "EDIT-TEST-F1-125"   TO PAR-NAME.                       NC1054.2
299500     PERFORM PRINT-DETAIL.                                        NC1054.2
299600 EDIT-INIT-F1-126.                                                NC1054.2
299700*                                                                 NC1054.2
299800 EDIT-TEST-F1-126-0.                                              NC1054.2
299900     MOVE  000321.01    TO EDIT-PIC-07.                           NC1054.2
300000 EDIT-TEST-F1-126-1.                                              NC1054.2
300100     IF GRP-EDIT-PIC-07 EQUAL TO  "   +321.01"                    NC1054.2
300200              PERFORM PASS                                        NC1054.2
300300              GO TO EDIT-WRITE-F1-126.                            NC1054.2
300400     PERFORM FAIL.                                                NC1054.2
300500     MOVE EDIT-PIC-07    TO COMPUTED-A.                           NC1054.2
300600     MOVE "   +321.01"  TO CORRECT-A.                             NC1054.2
300700     GO TO EDIT-WRITE-F1-126.                                     NC1054.2
300800 EDIT-DELETE-F1-126.                                              NC1054.2
300900     PERFORM DE-LETE.                                             NC1054.2
301000 EDIT-WRITE-F1-126.                                               NC1054.2
301100     MOVE "EDIT-TEST-F1-126"   TO PAR-NAME.                       NC1054.2
301200     PERFORM PRINT-DETAIL.                                        NC1054.2
301300 EDIT-INIT-F1-127.                                                NC1054.2
301400*                                                                 NC1054.2
301500 EDIT-TEST-F1-127-0.                                              NC1054.2
301600     MOVE -0012.98      TO EDIT-PIC-08.                           NC1054.2
301700 EDIT-TEST-F1-127-1.                                              NC1054.2
301800     IF GRP-EDIT-PIC-08 EQUAL TO "   -012.98"                     NC1054.2
301900              PERFORM PASS                                        NC1054.2
302000              GO TO EDIT-WRITE-F1-127.                            NC1054.2
302100     PERFORM FAIL.                                                NC1054.2
302200     MOVE EDIT-PIC-08    TO COMPUTED-A.                           NC1054.2
302300     MOVE "   -012.98"  TO CORRECT-A.                             NC1054.2
302400     GO TO EDIT-WRITE-F1-127.                                     NC1054.2
302500 EDIT-DELETE-F1-127.                                              NC1054.2
302600     PERFORM DE-LETE.                                             NC1054.2
302700 EDIT-WRITE-F1-127.                                               NC1054.2
302800     MOVE "EDIT-TEST-F1-127"   TO PAR-NAME.                       NC1054.2
302900     PERFORM PRINT-DETAIL.                                        NC1054.2
303000 EDIT-INIT-F1-128.                                                NC1054.2
303100*                                                                 NC1054.2
303200 EDIT-TEST-F1-128-0.                                              NC1054.2
303300     MOVE  0000567.43   TO EDIT-PIC-09.                           NC1054.2
303400 EDIT-TEST-F1-128-1.                                              NC1054.2
303500     IF GRP-EDIT-PIC-09 EQUAL TO "****567.43"                     NC1054.2
303600              PERFORM PASS                                        NC1054.2
303700              GO TO EDIT-WRITE-F1-128.                            NC1054.2
303800     PERFORM FAIL.                                                NC1054.2
303900     MOVE EDIT-PIC-09    TO COMPUTED-A.                           NC1054.2
304000     MOVE "****567.43"  TO CORRECT-A.                             NC1054.2
304100     GO TO EDIT-WRITE-F1-128.                                     NC1054.2
304200 EDIT-DELETE-F1-128.                                              NC1054.2
304300     PERFORM DE-LETE.                                             NC1054.2
304400 EDIT-WRITE-F1-128.                                               NC1054.2
304500     MOVE "EDIT-TEST-F1-128"   TO PAR-NAME.                       NC1054.2
304600     PERFORM PRINT-DETAIL.                                        NC1054.2
304700 EDIT-INIT-F1-129.                                                NC1054.2
304800*                                                                 NC1054.2
304900 EDIT-TEST-F1-129-0.                                              NC1054.2
305000     MOVE ZERO          TO EDIT-PIC-10.                           NC1054.2
305100 EDIT-TEST-F1-129-1.                                              NC1054.2
305200     IF GRP-EDIT-PIC-10 EQUAL TO  "    000.00"                    NC1054.2
305300              PERFORM PASS                                        NC1054.2
305400              GO TO EDIT-WRITE-F1-129.                            NC1054.2
305500     PERFORM FAIL.                                                NC1054.2
305600     MOVE EDIT-PIC-10    TO COMPUTED-A.                           NC1054.2
305700     MOVE "    000.00"   TO CORRECT-A.                            NC1054.2
305800     GO TO EDIT-WRITE-F1-129.                                     NC1054.2
305900 EDIT-DELETE-F1-129.                                              NC1054.2
306000     PERFORM DE-LETE.                                             NC1054.2
306100 EDIT-WRITE-F1-129.                                               NC1054.2
306200     MOVE "EDIT-TEST-F1-129"  TO PAR-NAME.                        NC1054.2
306300     PERFORM PRINT-DETAIL.                                        NC1054.2
306400     GO TO CCVS-EXIT.                                             NC1054.2
306500 A20.                                                             NC1054.2
306600     MOVE FIRST-20S TO CORRECT-A.                                 NC1054.2
306700     MOVE FIRST-20R TO COMPUTED-A.                                NC1054.2
306800     MOVE "1ST 20 POSITIONS OF RESULTS" TO RE-MARK.               NC1054.2
306900     MOVE TEST-RESULTS TO PRINT-REC.                              NC1054.2
307000     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     NC1054.2
307100     SUBTRACT 20 FROM LENGTH-COUNTER.                             NC1054.2
307200 A40.                                                             NC1054.2
307300     MOVE SECOND-20S TO CORRECT-A.                                NC1054.2
307400     MOVE SECOND-20R TO COMPUTED-A.                               NC1054.2
307500     MOVE "2ND 20 POSITIONS OF RESULTS" TO RE-MARK.               NC1054.2
307600     IF LENGTH-COUNTER GREATER THAN 20                            NC1054.2
307700         MOVE SPACE TO P-OR-F                                     NC1054.2
307800         MOVE TEST-RESULTS TO PRINT-REC                           NC1054.2
307900         WRITE PRINT-REC AFTER ADVANCING 1 LINES                  NC1054.2
308000         SUBTRACT 20 FROM LENGTH-COUNTER ELSE                     NC1054.2
308100     MOVE 000 TO LENGTH-COUNTER.                                  NC1054.2
308200 A60.                                                             NC1054.2
308300     MOVE THIRD-20S TO CORRECT-A.                                 NC1054.2
308400     MOVE THIRD-20R TO COMPUTED-A.                                NC1054.2
308500     MOVE "3RD 20 POSITIONS OF RESULTS" TO RE-MARK.               NC1054.2
308600     IF LENGTH-COUNTER GREATER THAN 20                            NC1054.2
308700         MOVE SPACE TO P-OR-F                                     NC1054.2
308800         MOVE TEST-RESULTS TO PRINT-REC                           NC1054.2
308900         WRITE PRINT-REC AFTER ADVANCING 1 LINES                  NC1054.2
309000         SUBTRACT 20 FROM LENGTH-COUNTER ELSE                     NC1054.2
309100     MOVE 000 TO LENGTH-COUNTER.                                  NC1054.2
309200 A80.                                                             NC1054.2
309300     MOVE FOURTH-20S TO CORRECT-A.                                NC1054.2
309400     MOVE FOURTH-20R TO COMPUTED-A.                               NC1054.2
309500     MOVE "4TH 20 POSITIONS OF RESULTS" TO RE-MARK.               NC1054.2
309600     IF LENGTH-COUNTER GREATER THAN 20                            NC1054.2
309700         MOVE SPACE TO P-OR-F                                     NC1054.2
309800         MOVE TEST-RESULTS TO PRINT-REC                           NC1054.2
309900         WRITE PRINT-REC AFTER ADVANCING 1 LINES                  NC1054.2
310000         SUBTRACT 20 FROM LENGTH-COUNTER ELSE                     NC1054.2
310100     MOVE 000 TO LENGTH-COUNTER.                                  NC1054.2
310200 A100.                                                            NC1054.2
310300     MOVE FIFTH-20S TO CORRECT-A.                                 NC1054.2
310400     MOVE FIFTH-20R TO COMPUTED-A.                                NC1054.2
310500     MOVE "5TH 20 POSITIONS OF RESULTS" TO RE-MARK.               NC1054.2
310600     IF LENGTH-COUNTER GREATER THAN 20                            NC1054.2
310700         MOVE SPACE TO P-OR-F                                     NC1054.2
310800         MOVE TEST-RESULTS TO PRINT-REC                           NC1054.2
310900         WRITE PRINT-REC AFTER ADVANCING 1 LINES.                 NC1054.2
311000     MOVE 000 TO LENGTH-COUNTER.                                  NC1054.2
311100 A120.                                                            NC1054.2
311200     MOVE SIXTH-20S TO CORRECT-A.                                 NC1054.2
311300     MOVE SIXTH-20R TO COMPUTED-A.                                NC1054.2
311400     MOVE "6TH 20 POSITIONS OF RESULTS" TO RE-MARK.               NC1054.2
311500 CCVS-EXIT SECTION.                                               NC1054.2
311600 CCVS-999999.                                                     NC1054.2
311700     GO TO CLOSE-FILES.                                           NC1054.2
      *END-OF,NC105A                                                                  
