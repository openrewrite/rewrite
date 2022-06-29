      *HEADER,COBOL,NC109M                                                            
000100 IDENTIFICATION DIVISION.                                         NC1094.2
000200 PROGRAM-ID.                                                      NC1094.2
000300     NC109M.                                                      NC1094.2
000400****************************************************************  NC1094.2
000500*                                                              *  NC1094.2
000600*    VALIDATION FOR:-                                          *  NC1094.2
000700*                                                              *  NC1094.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1094.2
000900*                                                              *  NC1094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1094.2
001100*                                                              *  NC1094.2
001200****************************************************************  NC1094.2
001300*                                                              *  NC1094.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1094.2
001500*                                                              *  NC1094.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1094.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1094.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1094.2
001900*                                                              *  NC1094.2
002000****************************************************************  NC1094.2
002100*                                                                 NC1094.2
002200*      PROGRAM NC109M TESTS FORMAT 1 OF THE ACCEPT STATEMENT      NC1094.2
002300*      AND THE GENERAL FORMAT OF THE DISPLAY STATEMENT.           NC1094.2
002400*                                                                 NC1094.2
002500*                                                                 NC1094.2
002600                                                                  NC1094.2
002700 ENVIRONMENT DIVISION.                                            NC1094.2
002800 CONFIGURATION SECTION.                                           NC1094.2
002900 SOURCE-COMPUTER.                                                 NC1094.2
003000     XXXXX082.                                                    NC1094.2
003100 OBJECT-COMPUTER.                                                 NC1094.2
003200     XXXXX083.                                                    NC1094.2
003300 INPUT-OUTPUT SECTION.                                            NC1094.2
003400 FILE-CONTROL.                                                    NC1094.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1094.2
003600     XXXXX055.                                                    NC1094.2
003700 DATA DIVISION.                                                   NC1094.2
003800 FILE SECTION.                                                    NC1094.2
003900 FD  PRINT-FILE.                                                  NC1094.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1094.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1094.2
004200 WORKING-STORAGE SECTION.                                         NC1094.2
004300 01  CHARACTER-BREAKDOWN-R.                                       NC1094.2
004400     02  FIRST-20R               PICTURE X(20).                   NC1094.2
004500     02  SECOND-20R              PICTURE X(20).                   NC1094.2
004600     02  THIRD-20R               PICTURE X(20).                   NC1094.2
004700     02  FOURTH-20R              PICTURE X(20).                   NC1094.2
004800 01  CHARACTER-BREAKDOWN-S.                                       NC1094.2
004900     02  FIRST-20S               PICTURE X(20).                   NC1094.2
005000     02  SECOND-20S              PICTURE X(20).                   NC1094.2
005100     02  THIRD-20S               PICTURE X(20).                   NC1094.2
005200     02  FOURTH-20S              PICTURE X(20).                   NC1094.2
005300 01  X80-CHARACTER-FIELD.                                         NC1094.2
005400     02  FILLER                  PICTURE X(80).                   NC1094.2
005500 01  ACCEPT-RESULTS.                                              NC1094.2
005600     02  FILLER                  PICTURE X(80)  VALUE             NC1094.2
005700     "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z  0123456NC1094.2
005800-    "789                 ".                                      NC1094.2
005900 01  DISPLAY-DATA.                                                NC1094.2
006000     02 DISPLAY-A.                                                NC1094.2
006100       03 DISPLAY-03               PICTURE A     VALUE "A".       NC1094.2
006200       03 DISPLAY-03A.                                            NC1094.2
006300         04 DISPLAY-04             PICTURE A     VALUE "L".       NC1094.2
006400         04 DISPLAY-04A.                                          NC1094.2
006500           05 DISPLAY-05           PICTURE A     VALUE "P".       NC1094.2
006600           05 DISPLAY-05A.                                        NC1094.2
006700             06 DISPLAY-06         PICTURE A     VALUE "H".       NC1094.2
006800             06 DISPLAY-06A.                                      NC1094.2
006900               07 DISPLAY-07       PICTURE A     VALUE "A".       NC1094.2
007000               07 DISPLAY-07A.                                    NC1094.2
007100                 08 DISPLAY-08     PICTURE A     VALUE "B".       NC1094.2
007200                 08 DISPLAY-08A.                                  NC1094.2
007300                   09 DISPLAY-09   PICTURE A     VALUE "E".       NC1094.2
007400                   09 DISPLAY-09A.                                NC1094.2
007500                     10 DISPLAY-10 PICTURE AAA   VALUE "TIC".     NC1094.2
007600     02 DISPLAY-N       PICTURE 9(10)       VALUE 0123456789.     NC1094.2
007700     02 DISPLAY-X       PICTURE X(10)       VALUE "A1B2C3D4E5".   NC1094.2
007800     02 DISPLAY-B       PICTURE X(13).                            NC1094.2
007900     02 DISPLAY-C REDEFINES DISPLAY-B.                            NC1094.2
008000       03 DISPLAY-D     PICTURE X(8).                             NC1094.2
008100       03 DISPLAY-E     PICTURE X(5).                             NC1094.2
008200     02 DISPLAY-F.                                                NC1094.2
008300       03 DISPLAY-G     PICTURE X(100)      VALUE "*001*002*003*00NC1094.2
008400-    "4*005*006*007*008*009*010*011*012*013*014*015*016*017*018*01NC1094.2
008500-    "9*020*021*022*023*024*025".                                 NC1094.2
008600       03 DISPLAY-H     PICTURE X(100)      VALUE "*026*027*028*02NC1094.2
008700-    "9*030*031*032*033*034*035*036*037*038*039*040*041*042*043*04NC1094.2
008800-    "4*045*046*047*048*049*050".                                 NC1094.2
008900     02 SEE-ABOVE       PICTURE X(9) VALUE "SEE ABOVE".           NC1094.2
009000     02 SEE-BELOW       PICTURE X(9) VALUE "SEE BELOW".           NC1094.2
009100     02 CORRECT-FOLLOWS PICTURE X(20) VALUE                       NC1094.2
009200                                      "CORRECT DATA FOLLOWS".     NC1094.2
009300     02 END-CORRECT     PICTURE X(16) VALUE                       NC1094.2
009400                                      "END CORRECT DATA".         NC1094.2
009500     02 DISPLAY-WRITER.                                           NC1094.2
009600       03 DIS-PLAYER.                                             NC1094.2
009700         04 FILLER      PICTURE X(6).                             NC1094.2
009800         04 QUOTE-SLOT  PICTURE X.                                NC1094.2
009900         04 FILLER      PICTURE X(112).                           NC1094.2
010000     02 DISPLAY-SWITCH  PICTURE 9 VALUE ZERO.                     NC1094.2
010100     02 ZERO-SPACE-QUOTE.                                         NC1094.2
010200       03 FILLER        PICTURE X VALUE ZERO.                     NC1094.2
010300       03 FILLER        PICTURE X VALUE SPACE.                    NC1094.2
010400       03 FILLER        PICTURE X VALUE QUOTE.                    NC1094.2
010500 01  LONG-LITERAL.                                                NC1094.2
010600     02 LONG20                          PICTURE IS X(20)          NC1094.2
010700     VALUE IS "STANDARD COMPILERS M".                             NC1094.2
010800     02 LONG40                          PICTURE IS X(20)          NC1094.2
010900     VALUE IS "UST ALLOW NON-NUMERI".                             NC1094.2
011000     02 LONG60                          PICTURE IS X(20)          NC1094.2
011100     VALUE IS "C LITERALS OF AT LEA".                             NC1094.2
011200     02 LONG80                          PICTURE IS X(20)          NC1094.2
011300     VALUE IS "ST 120 CHARACTERS AN".                             NC1094.2
011400     02 LONG100                         PICTURE IS X(20)          NC1094.2
011500     VALUE IS "D NUMERIC LITERALS O".                             NC1094.2
011600     02 LONG120                         PICTURE IS X(20)          NC1094.2
011700     VALUE IS "F AT LEAST 18 DIGITS".                             NC1094.2
011800 01  ACCEPT-DATA.                                                 NC1094.2
011900     02 ACCEPT-D1.                                                NC1094.2
012000       03 ACCEPT-D1-A PICTURE X(20).                              NC1094.2
012100       03 ACCEPT-D1-B PICTURE X(7).                               NC1094.2
012200     02 ACCEPT-D2       PICTURE X(27)                             NC1094.2
012300                        VALUE "ABCDEFGHIJKLMNOPQRSTUVWXY Z".      NC1094.2
012400     02 ACCEPT-D3  PICTURE 9(10) USAGE DISPLAY.                   NC1094.2
012500     02 ACCEPT-D4  PICTURE 9(10) USAGE DISPLAY VALUE 0123456789.  NC1094.2
012600     02 ACCEPT-D5  PICTURE X(11).                                 NC1094.2
012700     02  ACCEPT-D6 PICTURE X(11) VALUE "().+-*/$, =".             NC1094.2
012800     02 ACCEPT-D7  PICTURE X.                                     NC1094.2
012900     02 ACCEPT-D8 PICTURE X VALUE "9".                            NC1094.2
013000     02 ACCEPT-D9  PICTURE X.                                     NC1094.2
013100     02 ACCEPT-D10 PICTURE X VALUE "0".                           NC1094.2
013200     02 ACCEPT-D11 PICTURE A(20).                                 NC1094.2
013300     02 ACCEPT-D12 PICTURE A(20)                                  NC1094.2
013400              VALUE " ABC            XYZ ".                       NC1094.2
013500     02 ACCEPT-D13 PICTURE 9(9).                                  NC1094.2
013600     02 ACCEPT-D14 PICTURE 9(9) VALUE 012345678.                  NC1094.2
013700     02 ACCEPT-D15 PICTURE X.                                     NC1094.2
013800     02 ACCEPT-D16 PICTURE X VALUE SPACE.                         NC1094.2
013900     02 ACCEPT-D17 PICTURE X.                                     NC1094.2
014000     02 ACCEPT-D18 PICTURE X VALUE QUOTE.                         NC1094.2
014100     02 ACCEPT-D21.                                               NC1094.2
014200         03 TAB-ACCEPT PICTURE XXXX OCCURS 3 TIMES.               NC1094.2
014300     02 ACCEPT-D22 PICTURE X(12) VALUE "....ABCD....".            NC1094.2
014400 01  TAB-VALUE                    PICTURE X(21)                   NC1094.2
014500                                  VALUE "ABCDEFGHIJKLMNOPQRSTU".  NC1094.2
014600 01  NO-TAB-RECORD REDEFINES TAB-VALUE.                           NC1094.2
014700     02 X1    PICTURE X.                                          NC1094.2
014800     02 X2    PICTURE X.                                          NC1094.2
014900     02 X3    PICTURE X.                                          NC1094.2
015000     02 X4    PICTURE X.                                          NC1094.2
015100     02 X5    PICTURE X.                                          NC1094.2
015200     02 X6    PICTURE X.                                          NC1094.2
015300     02 X7    PICTURE X.                                          NC1094.2
015400     02 X8    PICTURE X.                                          NC1094.2
015500     02 X9    PICTURE X.                                          NC1094.2
015600     02 X10   PICTURE X.                                          NC1094.2
015700     02 X11   PICTURE X.                                          NC1094.2
015800     02 X12   PICTURE X.                                          NC1094.2
015900     02 X13   PICTURE X.                                          NC1094.2
016000     02 X14   PICTURE X.                                          NC1094.2
016100     02 X15   PICTURE X.                                          NC1094.2
016200     02 X16   PICTURE X.                                          NC1094.2
016300     02 X17   PICTURE X.                                          NC1094.2
016400     02 X18   PICTURE X.                                          NC1094.2
016500     02 X19   PICTURE X.                                          NC1094.2
016600     02 X20   PICTURE X.                                          NC1094.2
016700     02 X21   PICTURE X.                                          NC1094.2
016800 01  TAB-RECORD REDEFINES TAB-VALUE.                              NC1094.2
016900     02 XTAB                      PICTURE X OCCURS 21 TIMES.      NC1094.2
017000 01  DISPLAY-MIXTURE.                                             NC1094.2
017100     02 I-DATA                    PICTURE X(17)                   NC1094.2
017200                                  VALUE " IDENTIFIER DATA ".      NC1094.2
017300     02 TA-VALUE                  PICTURE X(20)                   NC1094.2
017400              VALUE "A B C D E 0102030405".                       NC1094.2
017500     02 TA-BLE REDEFINES TA-VALUE.                                NC1094.2
017600             04 PIECE-A           PICTURE XX OCCURS 5 TIMES.      NC1094.2
017700             04 PIECE-N           PICTURE 99 OCCURS 5 TIMES.      NC1094.2
017800     02 TRUE-PAIR.                                                NC1094.2
017900         03 A1                    PICTURE X(21)                   NC1094.2
018000                                  VALUE " (TOTAL 21 OPERANDS) ".  NC1094.2
018100         03 A2                    PICTURE X(11)                   NC1094.2
018200                                  VALUE "END OF DATA".            NC1094.2
018300 01  TEST-RESULTS.                                                NC1094.2
018400     02 FILLER                   PIC X      VALUE SPACE.          NC1094.2
018500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1094.2
018600     02 FILLER                   PIC X      VALUE SPACE.          NC1094.2
018700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1094.2
018800     02 FILLER                   PIC X      VALUE SPACE.          NC1094.2
018900     02  PAR-NAME.                                                NC1094.2
019000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1094.2
019100       03  PARDOT-X              PIC X      VALUE SPACE.          NC1094.2
019200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1094.2
019300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1094.2
019400     02 RE-MARK                  PIC X(61).                       NC1094.2
019500 01  TEST-COMPUTED.                                               NC1094.2
019600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1094.2
019700     02 FILLER                   PIC X(17)  VALUE                 NC1094.2
019800            "       COMPUTED=".                                   NC1094.2
019900     02 COMPUTED-X.                                               NC1094.2
020000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1094.2
020100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1094.2
020200                                 PIC -9(9).9(9).                  NC1094.2
020300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1094.2
020400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1094.2
020500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1094.2
020600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1094.2
020700         04 COMPUTED-18V0                    PIC -9(18).          NC1094.2
020800         04 FILLER                           PIC X.               NC1094.2
020900     03 FILLER PIC X(50) VALUE SPACE.                             NC1094.2
021000 01  TEST-CORRECT.                                                NC1094.2
021100     02 FILLER PIC X(30) VALUE SPACE.                             NC1094.2
021200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1094.2
021300     02 CORRECT-X.                                                NC1094.2
021400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1094.2
021500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1094.2
021600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1094.2
021700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1094.2
021800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1094.2
021900     03      CR-18V0 REDEFINES CORRECT-A.                         NC1094.2
022000         04 CORRECT-18V0                     PIC -9(18).          NC1094.2
022100         04 FILLER                           PIC X.               NC1094.2
022200     03 FILLER PIC X(2) VALUE SPACE.                              NC1094.2
022300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1094.2
022400 01  CCVS-C-1.                                                    NC1094.2
022500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1094.2
022600-    "SS  PARAGRAPH-NAME                                          NC1094.2
022700-    "       REMARKS".                                            NC1094.2
022800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1094.2
022900 01  CCVS-C-2.                                                    NC1094.2
023000     02 FILLER                     PIC X        VALUE SPACE.      NC1094.2
023100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1094.2
023200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1094.2
023300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1094.2
023400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1094.2
023500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1094.2
023600 01  REC-CT                        PIC 99       VALUE ZERO.       NC1094.2
023700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1094.2
023800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1094.2
023900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1094.2
024000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1094.2
024100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1094.2
024200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1094.2
024300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1094.2
024400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1094.2
024500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1094.2
024600 01  CCVS-H-1.                                                    NC1094.2
024700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1094.2
024800     02  FILLER                    PIC X(42)    VALUE             NC1094.2
024900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1094.2
025000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1094.2
025100 01  CCVS-H-2A.                                                   NC1094.2
025200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1094.2
025300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1094.2
025400   02  FILLER                        PIC XXXX   VALUE             NC1094.2
025500     "4.2 ".                                                      NC1094.2
025600   02  FILLER                        PIC X(28)  VALUE             NC1094.2
025700            " COPY - NOT FOR DISTRIBUTION".                       NC1094.2
025800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1094.2
025900                                                                  NC1094.2
026000 01  CCVS-H-2B.                                                   NC1094.2
026100   02  FILLER                        PIC X(15)  VALUE             NC1094.2
026200            "TEST RESULT OF ".                                    NC1094.2
026300   02  TEST-ID                       PIC X(9).                    NC1094.2
026400   02  FILLER                        PIC X(4)   VALUE             NC1094.2
026500            " IN ".                                               NC1094.2
026600   02  FILLER                        PIC X(12)  VALUE             NC1094.2
026700     " HIGH       ".                                              NC1094.2
026800   02  FILLER                        PIC X(22)  VALUE             NC1094.2
026900            " LEVEL VALIDATION FOR ".                             NC1094.2
027000   02  FILLER                        PIC X(58)  VALUE             NC1094.2
027100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1094.2
027200 01  CCVS-H-3.                                                    NC1094.2
027300     02  FILLER                      PIC X(34)  VALUE             NC1094.2
027400            " FOR OFFICIAL USE ONLY    ".                         NC1094.2
027500     02  FILLER                      PIC X(58)  VALUE             NC1094.2
027600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1094.2
027700     02  FILLER                      PIC X(28)  VALUE             NC1094.2
027800            "  COPYRIGHT   1985 ".                                NC1094.2
027900 01  CCVS-E-1.                                                    NC1094.2
028000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1094.2
028100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1094.2
028200     02 ID-AGAIN                     PIC X(9).                    NC1094.2
028300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1094.2
028400 01  CCVS-E-2.                                                    NC1094.2
028500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1094.2
028600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1094.2
028700     02 CCVS-E-2-2.                                               NC1094.2
028800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1094.2
028900         03 FILLER                   PIC X      VALUE SPACE.      NC1094.2
029000         03 ENDER-DESC               PIC X(44)  VALUE             NC1094.2
029100            "ERRORS ENCOUNTERED".                                 NC1094.2
029200 01  CCVS-E-3.                                                    NC1094.2
029300     02  FILLER                      PIC X(22)  VALUE             NC1094.2
029400            " FOR OFFICIAL USE ONLY".                             NC1094.2
029500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1094.2
029600     02  FILLER                      PIC X(58)  VALUE             NC1094.2
029700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1094.2
029800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1094.2
029900     02 FILLER                       PIC X(15)  VALUE             NC1094.2
030000             " COPYRIGHT 1985".                                   NC1094.2
030100 01  CCVS-E-4.                                                    NC1094.2
030200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1094.2
030300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1094.2
030400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1094.2
030500     02 FILLER                       PIC X(40)  VALUE             NC1094.2
030600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1094.2
030700 01  XXINFO.                                                      NC1094.2
030800     02 FILLER                       PIC X(19)  VALUE             NC1094.2
030900            "*** INFORMATION ***".                                NC1094.2
031000     02 INFO-TEXT.                                                NC1094.2
031100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1094.2
031200       04 XXCOMPUTED                 PIC X(20).                   NC1094.2
031300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1094.2
031400       04 XXCORRECT                  PIC X(20).                   NC1094.2
031500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1094.2
031600 01  HYPHEN-LINE.                                                 NC1094.2
031700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1094.2
031800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1094.2
031900-    "*****************************************".                 NC1094.2
032000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1094.2
032100-    "******************************".                            NC1094.2
032200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1094.2
032300     "NC109M".                                                    NC1094.2
032400 PROCEDURE DIVISION.                                              NC1094.2
032500 CCVS1 SECTION.                                                   NC1094.2
032600 OPEN-FILES.                                                      NC1094.2
032700     OPEN     OUTPUT PRINT-FILE.                                  NC1094.2
032800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1094.2
032900     MOVE    SPACE TO TEST-RESULTS.                               NC1094.2
033000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1094.2
033100     GO TO CCVS1-EXIT.                                            NC1094.2
033200 CLOSE-FILES.                                                     NC1094.2
033300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1094.2
033400 TERMINATE-CCVS.                                                  NC1094.2
033500*    EXIT PROGRAM.                                                NC1094.2
033600*TERMINATE-CALL.                                                  NC1094.2
033700     STOP     RUN.                                                NC1094.2
033800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1094.2
033900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1094.2
034000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1094.2
034100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1094.2
034200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1094.2
034300 PRINT-DETAIL.                                                    NC1094.2
034400     IF REC-CT NOT EQUAL TO ZERO                                  NC1094.2
034500             MOVE "." TO PARDOT-X                                 NC1094.2
034600             MOVE REC-CT TO DOTVALUE.                             NC1094.2
034700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1094.2
034800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1094.2
034900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1094.2
035000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1094.2
035100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1094.2
035200     MOVE SPACE TO CORRECT-X.                                     NC1094.2
035300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1094.2
035400     MOVE     SPACE TO RE-MARK.                                   NC1094.2
035500 HEAD-ROUTINE.                                                    NC1094.2
035600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1094.2
035700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1094.2
035800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1094.2
035900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1094.2
036000 COLUMN-NAMES-ROUTINE.                                            NC1094.2
036100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1094.2
036200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1094.2
036300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1094.2
036400 END-ROUTINE.                                                     NC1094.2
036500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1094.2
036600 END-RTN-EXIT.                                                    NC1094.2
036700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1094.2
036800 END-ROUTINE-1.                                                   NC1094.2
036900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1094.2
037000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1094.2
037100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1094.2
037200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1094.2
037300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1094.2
037400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1094.2
037500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1094.2
037600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1094.2
037700  END-ROUTINE-12.                                                 NC1094.2
037800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1094.2
037900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1094.2
038000         MOVE "NO " TO ERROR-TOTAL                                NC1094.2
038100         ELSE                                                     NC1094.2
038200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1094.2
038300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1094.2
038400     PERFORM WRITE-LINE.                                          NC1094.2
038500 END-ROUTINE-13.                                                  NC1094.2
038600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1094.2
038700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1094.2
038800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1094.2
038900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1094.2
039000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1094.2
039100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1094.2
039200          MOVE "NO " TO ERROR-TOTAL                               NC1094.2
039300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1094.2
039400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1094.2
039500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1094.2
039600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1094.2
039700 WRITE-LINE.                                                      NC1094.2
039800     ADD 1 TO RECORD-COUNT.                                       NC1094.2
039900*    IF RECORD-COUNT GREATER 42                                   NC1094.2
040000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1094.2
040100*        MOVE SPACE TO DUMMY-RECORD                               NC1094.2
040200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1094.2
040300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1094.2
040400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1094.2
040500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1094.2
040600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1094.2
040700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1094.2
040800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1094.2
040900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1094.2
041000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1094.2
041100*        MOVE ZERO TO RECORD-COUNT.                               NC1094.2
041200     PERFORM WRT-LN.                                              NC1094.2
041300 WRT-LN.                                                          NC1094.2
041400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1094.2
041500     MOVE SPACE TO DUMMY-RECORD.                                  NC1094.2
041600 BLANK-LINE-PRINT.                                                NC1094.2
041700     PERFORM WRT-LN.                                              NC1094.2
041800 FAIL-ROUTINE.                                                    NC1094.2
041900     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1094.2
042000            GO TO FAIL-ROUTINE-WRITE.                             NC1094.2
042100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1094.2
042200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1094.2
042300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1094.2
042400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1094.2
042500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1094.2
042600     GO TO  FAIL-ROUTINE-EX.                                      NC1094.2
042700 FAIL-ROUTINE-WRITE.                                              NC1094.2
042800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1094.2
042900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1094.2
043000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1094.2
043100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1094.2
043200 FAIL-ROUTINE-EX. EXIT.                                           NC1094.2
043300 BAIL-OUT.                                                        NC1094.2
043400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1094.2
043500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1094.2
043600 BAIL-OUT-WRITE.                                                  NC1094.2
043700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1094.2
043800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1094.2
043900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1094.2
044000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1094.2
044100 BAIL-OUT-EX. EXIT.                                               NC1094.2
044200 CCVS1-EXIT.                                                      NC1094.2
044300     EXIT.                                                        NC1094.2
044400 SECT-NC109M-001 SECTION.                                         NC1094.2
044500 ACC-INIT-GF-1.                                                   NC1094.2
044600     MOVE   "ACCEPT" TO FEATURE.                                  NC1094.2
044700     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
044800     MOVE    SPACES TO ACCEPT-D1.                                 NC1094.2
044900 ACC-TEST-GF-1.                                                   NC1094.2
045000     MOVE    "ACC-TEST-GF-1" TO PAR-NAME.                         NC1094.2
045100     ACCEPT   ACCEPT-D1.                                          NC1094.2
045200     IF       ACCEPT-D1 EQUAL TO ACCEPT-D2                        NC1094.2
045300              PERFORM PASS GO TO ACC-WRITE-GF-1.                  NC1094.2
045400*        NOTE ACCEPT ALPHABETIC LITERAL TO ALPHANUMERIC FIELD.    NC1094.2
045500     GO TO   ACC-FAIL-GF-1.                                       NC1094.2
045600 ACC-DELETE-GF-1.                                                 NC1094.2
045700     MOVE     "ACC-TEST-GF-1" TO PAR-NAME.                        NC1094.2
045800     PERFORM  DE-LETE.                                            NC1094.2
045900     GO       TO ACC-WRITE-GF-1.                                  NC1094.2
046000 ACC-FAIL-GF-1.                                                   NC1094.2
046100     PERFORM  FAIL.                                               NC1094.2
046200     MOVE     ACCEPT-D1-A TO COMPUTED-A.                          NC1094.2
046300     MOVE     "ABCDEFGHIJKLMNOPQRST" TO CORRECT-A.                NC1094.2
046400     PERFORM  PRINT-DETAIL.                                       NC1094.2
046500     MOVE     ACCEPT-D1-B TO COMPUTED-A.                          NC1094.2
046600     MOVE     "UVWXY Z" TO CORRECT-A.                             NC1094.2
046700     MOVE     "LAST 7 OF 27-CHAR FIELD" TO RE-MARK.               NC1094.2
046800 ACC-WRITE-GF-1.                                                  NC1094.2
046900     PERFORM  PRINT-DETAIL.                                       NC1094.2
047000 ACC-INIT-GF-2.                                                   NC1094.2
047100     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
047200     MOVE     ZEROES TO ACCEPT-D3.                                NC1094.2
047300 ACC-TEST-GF-2.                                                   NC1094.2
047400     ACCEPT   ACCEPT-D3.                                          NC1094.2
047500     IF       ACCEPT-D3  EQUAL TO ACCEPT-D4                       NC1094.2
047600              PERFORM PASS GO TO ACC-WRITE-GF-2.                  NC1094.2
047700*        NOTE ACCEPT NUMERIC LITERAL TO NUMERIC FIELD SAME LENGTH.NC1094.2
047800     GO       TO ACC-FAIL-GF-2.                                   NC1094.2
047900 ACC-DELETE-GF-2.                                                 NC1094.2
048000     PERFORM  DE-LETE.                                            NC1094.2
048100     GO       TO ACC-WRITE-GF-2.                                  NC1094.2
048200 ACC-FAIL-GF-2.                                                   NC1094.2
048300     MOVE     ACCEPT-D3  TO COMPUTED-18V0.                        NC1094.2
048400     MOVE     ACCEPT-D4  TO CORRECT-18V0.                         NC1094.2
048500     PERFORM  FAIL.                                               NC1094.2
048600 ACC-WRITE-GF-2.                                                  NC1094.2
048700     MOVE     "ACC-TEST-GF-2 " TO PAR-NAME.                       NC1094.2
048800     PERFORM  PRINT-DETAIL.                                       NC1094.2
048900 ACC-INIT-GF-3.                                                   NC1094.2
049000     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
049100     MOVE     SPACES TO ACCEPT-D5.                                NC1094.2
049200 ACC-TEST-GF-3.                                                   NC1094.2
049300     ACCEPT   ACCEPT-D5.                                          NC1094.2
049400     IF       ACCEPT-D5  EQUAL TO ACCEPT-D6                       NC1094.2
049500              PERFORM PASS GO TO ACC-WRITE-GF-3.                  NC1094.2
049600*        NOTE ACCEPT SPECIAL CHARACTERS.                          NC1094.2
049700     GO       TO ACC-FAIL-GF-3.                                   NC1094.2
049800 ACC-DELETE-GF-3.                                                 NC1094.2
049900     PERFORM  DE-LETE.                                            NC1094.2
050000     GO       TO ACC-WRITE-GF-3.                                  NC1094.2
050100 ACC-FAIL-GF-3.                                                   NC1094.2
050200     MOVE     ACCEPT-D5  TO COMPUTED-A.                           NC1094.2
050300     MOVE     ACCEPT-D6  TO CORRECT-A.                            NC1094.2
050400     PERFORM  FAIL.                                               NC1094.2
050500 ACC-WRITE-GF-3.                                                  NC1094.2
050600     MOVE     "ACC-TEST-GF-3 " TO PAR-NAME.                       NC1094.2
050700     PERFORM  PRINT-DETAIL.                                       NC1094.2
050800 ACC-INIT-GF-4.                                                   NC1094.2
050900     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
051000     MOVE     SPACES TO ACCEPT-D7.                                NC1094.2
051100 ACC-TEST-GF-4.                                                   NC1094.2
051200     ACCEPT   ACCEPT-D7.                                          NC1094.2
051300     IF       ACCEPT-D7  EQUAL TO ACCEPT-D8                       NC1094.2
051400              PERFORM PASS GO TO ACC-WRITE-GF-4.                  NC1094.2
051500*        NOTE ACCEPT HIGH-VALUE.                                  NC1094.2
051600*        NOTE CHANGED TO ACCEPT AN ALPHANUMERIC 9.                NC1094.2
051700     GO       TO ACC-FAIL-GF-4.                                   NC1094.2
051800 ACC-DELETE-GF-4.                                                 NC1094.2
051900     PERFORM  DE-LETE.                                            NC1094.2
052000     GO       TO ACC-WRITE-GF-4.                                  NC1094.2
052100 ACC-FAIL-GF-4.                                                   NC1094.2
052200     MOVE     ACCEPT-D7  TO COMPUTED-A.                           NC1094.2
052300     MOVE     ACCEPT-D8  TO CORRECT-A.                            NC1094.2
052400     PERFORM  FAIL.                                               NC1094.2
052500 ACC-WRITE-GF-4.                                                  NC1094.2
052600     MOVE     "ACC-TEST-GF-4 " TO PAR-NAME.                       NC1094.2
052700     PERFORM  PRINT-DETAIL.                                       NC1094.2
052800 ACC-INIT-GF-5.                                                   NC1094.2
052900     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
053000     MOVE     SPACES TO ACCEPT-D9.                                NC1094.2
053100 ACC-TEST-GF-5.                                                   NC1094.2
053200     ACCEPT   ACCEPT-D9.                                          NC1094.2
053300     IF       ACCEPT-D9  EQUAL TO ACCEPT-D10                      NC1094.2
053400              PERFORM PASS GO TO ACC-WRITE-GF-5.                  NC1094.2
053500*        NOTE CHANGED TO ACCEPT AN ALPHANUMERIC 0.                NC1094.2
053600*        NOTE ACCEPT LOW-VALUE.                                   NC1094.2
053700     GO       TO ACC-FAIL-GF-5.                                   NC1094.2
053800 ACC-DELETE-GF-5.                                                 NC1094.2
053900     PERFORM  DE-LETE.                                            NC1094.2
054000     GO       TO ACC-WRITE-GF-5.                                  NC1094.2
054100 ACC-FAIL-GF-5.                                                   NC1094.2
054200     MOVE     ACCEPT-D9  TO COMPUTED-A.                           NC1094.2
054300     MOVE     ACCEPT-D10 TO CORRECT-A.                            NC1094.2
054400     PERFORM  FAIL.                                               NC1094.2
054500 ACC-WRITE-GF-5.                                                  NC1094.2
054600     MOVE     "ACC-TEST-GF-5 " TO PAR-NAME.                       NC1094.2
054700     PERFORM  PRINT-DETAIL.                                       NC1094.2
054800 ACC-INIT-GF-6.                                                   NC1094.2
054900     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
055000     MOVE     SPACES TO ACCEPT-D11.                               NC1094.2
055100 ACC-TEST-GF-6.                                                   NC1094.2
055200     ACCEPT   ACCEPT-D11.                                         NC1094.2
055300     IF       ACCEPT-D11 EQUAL TO ACCEPT-D12                      NC1094.2
055400              PERFORM PASS GO TO ACC-WRITE-GF-6.                  NC1094.2
055500*        NOTE ACCEPT ALPHABETIC LITERAL TO ALPHABETIC FIELD.      NC1094.2
055600     GO       TO ACC-FAIL-GF-6.                                   NC1094.2
055700 ACC-DELETE-GF-6.                                                 NC1094.2
055800     PERFORM  DE-LETE.                                            NC1094.2
055900     GO       TO ACC-WRITE-GF-6.                                  NC1094.2
056000 ACC-FAIL-GF-6.                                                   NC1094.2
056100     MOVE     ACCEPT-D11 TO COMPUTED-A.                           NC1094.2
056200     MOVE     ACCEPT-D12 TO CORRECT-A.                            NC1094.2
056300     PERFORM  FAIL.                                               NC1094.2
056400 ACC-WRITE-GF-6.                                                  NC1094.2
056500     MOVE     "ACC-TEST-GF-6 " TO PAR-NAME.                       NC1094.2
056600     PERFORM  PRINT-DETAIL.                                       NC1094.2
056700 ACC-INIT-GF-7.                                                   NC1094.2
056800     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
056900     MOVE     ZEROES TO ACCEPT-D13.                               NC1094.2
057000 ACC-TEST-GF-7.                                                   NC1094.2
057100     ACCEPT   ACCEPT-D13.                                         NC1094.2
057200     IF       ACCEPT-D13 EQUAL TO ACCEPT-D14                      NC1094.2
057300              PERFORM PASS GO TO ACC-WRITE-GF-7.                  NC1094.2
057400*        NOTE ACCEPT NUMERIC LITERAL TO NUMERIC FIELD OF DIFFERENTNC1094.2
057500*             LENGTH.                                             NC1094.2
057600     GO       TO ACC-FAIL-GF-7.                                   NC1094.2
057700 ACC-DELETE-GF-7.                                                 NC1094.2
057800     PERFORM  DE-LETE.                                            NC1094.2
057900     GO       TO ACC-WRITE-GF-7.                                  NC1094.2
058000 ACC-FAIL-GF-7.                                                   NC1094.2
058100     MOVE     ACCEPT-D13 TO COMPUTED-A.                           NC1094.2
058200     MOVE     ACCEPT-D14 TO CORRECT-A.                            NC1094.2
058300     PERFORM  FAIL.                                               NC1094.2
058400 ACC-WRITE-GF-7.                                                  NC1094.2
058500     MOVE     "ACC-TEST-GF-7 " TO PAR-NAME.                       NC1094.2
058600     PERFORM  PRINT-DETAIL.                                       NC1094.2
058700 ACC-INIT-GF-8.                                                   NC1094.2
058800     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
058900     MOVE     ZEROES TO ACCEPT-D15.                               NC1094.2
059000 ACC-TEST-GF-8.                                                   NC1094.2
059100     ACCEPT   ACCEPT-D15.                                         NC1094.2
059200     IF       ACCEPT-D15 EQUAL TO ACCEPT-D16                      NC1094.2
059300              PERFORM PASS GO TO ACC-WRITE-GF-8.                  NC1094.2
059400*        NOTE ACCEPT SINGLE SPACE.                                NC1094.2
059500     GO       TO ACC-FAIL-GF-8.                                   NC1094.2
059600 ACC-DELETE-GF-8.                                                 NC1094.2
059700     PERFORM  DE-LETE.                                            NC1094.2
059800     GO       TO ACC-WRITE-GF-8.                                  NC1094.2
059900 ACC-FAIL-GF-8.                                                   NC1094.2
060000     PERFORM  FAIL.                                               NC1094.2
060100     MOVE     ACCEPT-D15 TO COMPUTED-A.                           NC1094.2
060200     MOVE     "      (SPACES)" TO CORRECT-A.                      NC1094.2
060300 ACC-WRITE-GF-8.                                                  NC1094.2
060400     MOVE     "ACC-TEST-GF-8" TO PAR-NAME.                        NC1094.2
060500     PERFORM  PRINT-DETAIL.                                       NC1094.2
060600 ACC-INIT-GF-9.                                                   NC1094.2
060700     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
060800     MOVE     ZEROES TO ACCEPT-D17.                               NC1094.2
060900 ACC-TEST-GF-9.                                                   NC1094.2
061000     ACCEPT   ACCEPT-D17.                                         NC1094.2
061100     IF       ACCEPT-D17 EQUAL TO ACCEPT-D18                      NC1094.2
061200              PERFORM PASS GO TO ACC-WRITE-GF-9.                  NC1094.2
061300*        NOTE ACCEPT A QUOTE.                                     NC1094.2
061400     GO       TO ACC-FAIL-GF-9.                                   NC1094.2
061500 ACC-DELETE-GF-9.                                                 NC1094.2
061600     PERFORM  DE-LETE.                                            NC1094.2
061700     GO       TO ACC-WRITE-GF-9.                                  NC1094.2
061800 ACC-FAIL-GF-9.                                                   NC1094.2
061900     PERFORM  FAIL.                                               NC1094.2
062000     MOVE     ACCEPT-D17 TO COMPUTED-A.                           NC1094.2
062100     MOVE     ACCEPT-D18 TO CORRECT-A.                            NC1094.2
062200 ACC-WRITE-GF-9.                                                  NC1094.2
062300     MOVE     "ACC-TEST-GF-9" TO PAR-NAME.                        NC1094.2
062400     PERFORM  PRINT-DETAIL.                                       NC1094.2
062500 ACC-INIT-GF-10.                                                  NC1094.2
062600     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
062700     MOVE     "............" TO ACCEPT-D21.                       NC1094.2
062800 ACC-TEST-GF-10.                                                  NC1094.2
062900     ACCEPT   TAB-ACCEPT (2).                                     NC1094.2
063000     IF       ACCEPT-D21 EQUAL TO ACCEPT-D22                      NC1094.2
063100              PERFORM PASS GO TO ACC-WRITE-GF-10.                 NC1094.2
063200*        NOTE ACCEPT TO SUBSCRIPTED AREA.                         NC1094.2
063300     GO       TO ACC-FAIL-GF-10.                                  NC1094.2
063400 ACC-DELETE-GF-10.                                                NC1094.2
063500     PERFORM  DE-LETE.                                            NC1094.2
063600     GO       TO ACC-WRITE-GF-10.                                 NC1094.2
063700 ACC-FAIL-GF-10.                                                  NC1094.2
063800     PERFORM  FAIL.                                               NC1094.2
063900     MOVE     ACCEPT-D21 TO COMPUTED-A.                           NC1094.2
064000     MOVE     ACCEPT-D22 TO CORRECT-A.                            NC1094.2
064100 ACC-WRITE-GF-10.                                                 NC1094.2
064200     MOVE     "ACC-TEST-GF-10" TO PAR-NAME.                       NC1094.2
064300     PERFORM  PRINT-DETAIL.                                       NC1094.2
064400 ACC-INIT-GF-11.                                                  NC1094.2
064500     MOVE   "V1-71 6.5.4 GR1-5" TO ANSI-REFERENCE.                NC1094.2
064600     MOVE    SPACES TO X80-CHARACTER-FIELD.                       NC1094.2
064700 ACC-TEST-GF-11.                                                  NC1094.2
064800     ACCEPT X80-CHARACTER-FIELD.                                  NC1094.2
064900     MOVE     "ACC-TEST-GF-11" TO PAR-NAME.                       NC1094.2
065000     IF       X80-CHARACTER-FIELD EQUAL TO ACCEPT-RESULTS         NC1094.2
065100              PERFORM PASS GO TO ACC-WRITE-GF-11.                 NC1094.2
065200*        NOTE ACCEPT 80-CHARACTER LITERAL.                        NC1094.2
065300     GO       TO ACC-FAIL-GF-11.                                  NC1094.2
065400 ACC-DELETE-GF-11.                                                NC1094.2
065500     PERFORM  DE-LETE.                                            NC1094.2
065600     MOVE     "ACC-TEST-GF-11" TO PAR-NAME.                       NC1094.2
065700     GO       TO ACC-WRITE-GF-11.                                 NC1094.2
065800 ACC-FAIL-GF-11.                                                  NC1094.2
065900     MOVE     X80-CHARACTER-FIELD TO CHARACTER-BREAKDOWN-R.       NC1094.2
066000     PERFORM  FAIL.                                               NC1094.2
066100     MOVE     ACCEPT-RESULTS TO CHARACTER-BREAKDOWN-S.            NC1094.2
066200     MOVE      FIRST-20R TO COMPUTED-A.                           NC1094.2
066300     MOVE      FIRST-20S TO CORRECT-A.                            NC1094.2
066400     PERFORM  PRINT-DETAIL.                                       NC1094.2
066500     MOVE     SECOND-20R TO COMPUTED-A.                           NC1094.2
066600     MOVE     SECOND-20S TO CORRECT-A.                            NC1094.2
066700     PERFORM  PRINT-DETAIL.                                       NC1094.2
066800     MOVE      THIRD-20R TO COMPUTED-A.                           NC1094.2
066900     MOVE      THIRD-20S TO CORRECT-A.                            NC1094.2
067000     PERFORM  PRINT-DETAIL.                                       NC1094.2
067100     MOVE     FOURTH-20R TO COMPUTED-A.                           NC1094.2
067200     MOVE     FOURTH-20S TO CORRECT-A.                            NC1094.2
067300     MOVE     "LAST 20 OF 80-CHAR FIELD" TO RE-MARK.              NC1094.2
067400 ACC-WRITE-GF-11.                                                 NC1094.2
067500     PERFORM PRINT-DETAIL.                                        NC1094.2
067600 DISP-INIT-GF-1.                                                  NC1094.2
067700     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
067800     PERFORM BLANK-LINE-PRINT.                                    NC1094.2
067900     MOVE   "DISPLAY TESTS" TO FEATURE.                           NC1094.2
068000     MOVE   "SEE NOTE IN DISP-INIT-GF-1" TO RE-MARK.              NC1094.2
068100     PERFORM PRINT-DETAIL.                                        NC1094.2
068200     PERFORM BLANK-LINE-PRINT 4 TIMES.                            NC1094.2
068300     MOVE     "DISPLAY" TO FEATURE.                               NC1094.2
068400*        NOTE FOR THE SAKE OF CONVENIENCE IN READING THE OUTPUT,  NC1094.2
068500*             THE DISPLAY TESTS ARE CONSTRUCTED ON THE ASSUMPTION NC1094.2
068600*             THAT THE DISPLAYED OUTPUT WILL BE PRINTED ALONG     NC1094.2
068700*             WITH THE OUTPUT FROM THE WRITE STATEMENTS ---       NC1094.2
068800*             HOWEVER IT IS NOT CONSIDERED NONSTANDARD IF THE     NC1094.2
068900*             DISPLAYED DATA APPEARS ELSEWHERE IN THE LISTING, OR NC1094.2
069000*             FOR THAT MATTER, ON SOME OTHER DEVICE.              NC1094.2
069100 DISP-TEST-GF-1.                                                  NC1094.2
069200     MOVE     "DISP-TEST-GF-1 " TO PAR-NAME.                      NC1094.2
069300     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
069400     DISPLAY  DISPLAY-A.                                          NC1094.2
069500*        NOTE GROUP OF ALPHABETIC DATA ITEMS.                     NC1094.2
069600     MOVE     DISPLAY-A TO DIS-PLAYER.                            NC1094.2
069700     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
069800     GO       TO DISP-WRITE-GF-1.                                 NC1094.2
069900 DISP-DELETE-GF-1.                                                NC1094.2
070000     PERFORM  DE-LETE.                                            NC1094.2
070100 DISP-WRITE-GF-1.                                                 NC1094.2
070200     MOVE     "DISP-TEST-GF-1 " TO PAR-NAME.                      NC1094.2
070300     PERFORM  PRINT-DETAIL.                                       NC1094.2
070400 DISP-INIT-GF-2.                                                  NC1094.2
070500     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
070600     MOVE     "DISP-TEST-GF-2 " TO PAR-NAME.                      NC1094.2
070700 DISP-TEST-GF-2.                                                  NC1094.2
070800     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
070900     DISPLAY  "ALPHABETIC LITERAL".                               NC1094.2
071000*        NOTE ALPHABETIC LITERAL.                                 NC1094.2
071100     MOVE     "ALPHABETIC LITERAL" TO DIS-PLAYER.                 NC1094.2
071200     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
071300     GO       TO DISP-WRITE-GF-2.                                 NC1094.2
071400 DISP-DELETE-GF-2.                                                NC1094.2
071500     PERFORM  DE-LETE.                                            NC1094.2
071600 DISP-WRITE-GF-2.                                                 NC1094.2
071700     MOVE     "DISP-TEST-GF-2 " TO PAR-NAME.                      NC1094.2
071800     PERFORM  PRINT-DETAIL.                                       NC1094.2
071900 DISP-INIT-GF-3.                                                  NC1094.2
072000     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
072100     MOVE     "DISP-TEST-GF-3" TO PAR-NAME.                       NC1094.2
072200     MOVE     0123456789 TO DISPLAY-N.                            NC1094.2
072300 DISP-TEST-GF-3.                                                  NC1094.2
072400     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
072500     DISPLAY  DISPLAY-N.                                          NC1094.2
072600*        NOTE NUMERIC DATA ITEM.                                  NC1094.2
072700     MOVE     DISPLAY-N TO DIS-PLAYER.                            NC1094.2
072800     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
072900     GO       TO DISP-WRITE-GF-3.                                 NC1094.2
073000 DISP-DELETE-GF-3.                                                NC1094.2
073100     PERFORM  DE-LETE.                                            NC1094.2
073200 DISP-WRITE-GF-3.                                                 NC1094.2
073300     MOVE     "DISP-TEST-GF-3 " TO PAR-NAME.                      NC1094.2
073400     PERFORM  PRINT-DETAIL.                                       NC1094.2
073500 DISP-INIT-GF-4.                                                  NC1094.2
073600     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
073700     MOVE     "DISP-TEST-GF-4" TO PAR-NAME.                       NC1094.2
073800 DISP-TEST-GF-4.                                                  NC1094.2
073900     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
074000     DISPLAY  9876543210.                                         NC1094.2
074100*        NOTE NUMERIC LITERAL.                                    NC1094.2
074200     MOVE     9876543210 TO DIS-PLAYER.                           NC1094.2
074300     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
074400     GO       TO DISP-WRITE-GF-4.                                 NC1094.2
074500 DISP-DELETE-GF-4.                                                NC1094.2
074600     PERFORM  DE-LETE.                                            NC1094.2
074700 DISP-WRITE-GF-4.                                                 NC1094.2
074800     MOVE     "DISP-TEST-GF-4 " TO PAR-NAME.                      NC1094.2
074900     PERFORM  PRINT-DETAIL.                                       NC1094.2
075000 DISP-INIT-GF-5.                                                  NC1094.2
075100     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
075200     MOVE   "DISP-TEST-GF-5" TO PAR-NAME.                         NC1094.2
075300     MOVE   "A1B2C3D4E5" TO DISPLAY-X.                            NC1094.2
075400 DISP-TEST-GF-5.                                                  NC1094.2
075500     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
075600     DISPLAY  DISPLAY-X.                                          NC1094.2
075700*        NOTE ALPHANUMERIC DATA ITEM.                             NC1094.2
075800     MOVE     DISPLAY-X TO DIS-PLAYER.                            NC1094.2
075900     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
076000     GO       TO DISP-WRITE-GF-5.                                 NC1094.2
076100 DISP-DELETE-GF-5.                                                NC1094.2
076200     PERFORM  DE-LETE.                                            NC1094.2
076300 DISP-WRITE-GF-5.                                                 NC1094.2
076400     MOVE     "DISP-TEST-GF-5 " TO PAR-NAME.                      NC1094.2
076500     PERFORM  PRINT-DETAIL.                                       NC1094.2
076600 DISP-INIT-GF-6.                                                  NC1094.2
076700     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
076800     MOVE   "DISP-TEST-GF-6" TO PAR-NAME.                         NC1094.2
076900 DISP-TEST-GF-6.                                                  NC1094.2
077000     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
077100     DISPLAY  "12345 ///// ALPHANUMERIC LITERAL".                 NC1094.2
077200*        NOTE ALPHANUMERIC LITERAL.                               NC1094.2
077300     MOVE     "12345 ///// ALPHANUMERIC LITERAL" TO DIS-PLAYER.   NC1094.2
077400     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
077500     GO       TO DISP-WRITE-GF-6.                                 NC1094.2
077600 DISP-DELETE-GF-6.                                                NC1094.2
077700     PERFORM  DE-LETE.                                            NC1094.2
077800 DISP-WRITE-GF-6.                                                 NC1094.2
077900     MOVE     "DISP-TEST-GF-6 " TO PAR-NAME.                      NC1094.2
078000     PERFORM  PRINT-DETAIL.                                       NC1094.2
078100 DISP-INIT-GF-7.                                                  NC1094.2
078200     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
078300     MOVE   "DISP-TEST-GF-7" TO PAR-NAME.                         NC1094.2
078400     MOVE   "ALPHABETIC" TO DISPLAY-A.                            NC1094.2
078500     MOVE    0123456789  TO DISPLAY-N.                            NC1094.2
078600     MOVE   "A1B2C3D4E5" TO DISPLAY-X.                            NC1094.2
078700 DISP-TEST-GF-7.                                                  NC1094.2
078800     MOVE     "DISP-TEST-GF-7 " TO PAR-NAME.                      NC1094.2
078900     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
079000     DISPLAY  DISPLAY-A DISPLAY-N DISPLAY-X " SERIES".            NC1094.2
079100*        NOTE SERIES OF THREE DATA ITEMS AND A LITERAL.           NC1094.2
079200     MOVE     "ALPHABETIC0123456789A1B2C3D4E5 SERIES"             NC1094.2
079300              TO DIS-PLAYER.                                      NC1094.2
079400     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
079500     GO       TO DISP-WRITE-GF-7.                                 NC1094.2
079600 DISP-DELETE-GF-7.                                                NC1094.2
079700     PERFORM  DE-LETE.                                            NC1094.2
079800 DISP-WRITE-GF-7.                                                 NC1094.2
079900     MOVE     "DISP-TEST-GF-7 " TO PAR-NAME.                      NC1094.2
080000     PERFORM  PRINT-DETAIL.                                       NC1094.2
080100 DISP-INIT-GF-8.                                                  NC1094.2
080200     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
080300     MOVE     "DISP-TEST-GF-8 " TO PAR-NAME.                      NC1094.2
080400 DISP-TEST-GF-8.                                                  NC1094.2
080500     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
080600     DISPLAY  ZERO SPACE QUOTE.                                   NC1094.2
080700*        NOTE SERIES OF FIGURATIVE CONSTANTS --- ONLY ONE OCCUR-  NC1094.2
080800*             RANCE OF EACH CHARACTER SHOULD APPEAR.              NC1094.2
080900     MOVE     ZERO-SPACE-QUOTE TO DIS-PLAYER.                     NC1094.2
081000     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
081100     GO       TO DISP-WRITE-GF-8.                                 NC1094.2
081200 DISP-DELETE-GF-8.                                                NC1094.2
081300     PERFORM  DE-LETE.                                            NC1094.2
081400 DISP-WRITE-GF-8.                                                 NC1094.2
081500     MOVE     "DISP-TEST-GF-8 " TO PAR-NAME.                      NC1094.2
081600     PERFORM  PRINT-DETAIL.                                       NC1094.2
081700 DISP-INIT-GF-9.                                                  NC1094.2
081800     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
081900     MOVE     "DISP-TEST-GF-9 " TO PAR-NAME.                      NC1094.2
082000     MOVE     "REDEFINE-INFO" TO DISPLAY-B.                       NC1094.2
082100 DISP-TEST-GF-9.                                                  NC1094.2
082200     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
082300     DISPLAY  DISPLAY-C.                                          NC1094.2
082400*        NOTE DISPLAY DATA ITEM WHICH CONTAINS A REDEFINES CLAUSE.NC1094.2
082500     MOVE     "REDEFINE-INFO" TO DIS-PLAYER.                      NC1094.2
082600     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
082700     GO       TO DISP-WRITE-GF-9.                                 NC1094.2
082800 DISP-DELETE-GF-9.                                                NC1094.2
082900     PERFORM  DE-LETE.                                            NC1094.2
083000 DISP-WRITE-GF-9.                                                 NC1094.2
083100     MOVE     "DISP-TEST-GF-9 " TO PAR-NAME.                      NC1094.2
083200     PERFORM  PRINT-DETAIL.                                       NC1094.2
083300 DISP-INIT-GF-10.                                                 NC1094.2
083400     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
083500     MOVE     "DISP-TEST-GF-10 " TO PAR-NAME.                     NC1094.2
083600 DISP-TEST-GF-10.                                                 NC1094.2
083700     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
083800     DISPLAY  DISPLAY-F.                                          NC1094.2
083900*        NOTE 200-CHARACTER GROUP ITEM --- ACTUAL NUMBER OF       NC1094.2
084000*             CHARACTERS DISPLAYED DEPENDS UPON THE SYSTEM.       NC1094.2
084100     MOVE     DISPLAY-G TO DIS-PLAYER.                            NC1094.2
084200     MOVE     1 TO DISPLAY-SWITCH.                                NC1094.2
084300*        NOTE THE "CORRECT" RESULT IS WRITTEN AS TWO 100-CHARACTERNC1094.2
084400*             LINES, BUT THE DIVISION OF THE DISPLAYED "COMPUTED" NC1094.2
084500*             DATA DEPENDS UPON THE SYSTEM.                       NC1094.2
084600     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
084700     GO       TO DISP-WRITE-GF-10.                                NC1094.2
084800 DISP-DELETE-GF-10.                                               NC1094.2
084900     PERFORM  DE-LETE.                                            NC1094.2
085000 DISP-WRITE-GF-10.                                                NC1094.2
085100     MOVE     "DISP-TEST-GF-10 " TO PAR-NAME.                     NC1094.2
085200     PERFORM  PRINT-DETAIL.                                       NC1094.2
085300 DISP-INIT-GF-11.                                                 NC1094.2
085400     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
085500     MOVE     "DISP-TEST-GF-11 " TO PAR-NAME.                     NC1094.2
085600     MOVE   "ABCDEFGHIJKLMNOPQRSTU" TO TAB-VALUE.                 NC1094.2
085700 DISP-TEST-GF-11.                                                 NC1094.2
085800     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
085900     DISPLAY  X21 X20 X19 X18 X17 X16 X15 X14 X13 X12 X11 X10 X9  NC1094.2
086000              X8  X7  X6  X5  X4  X3  X2  X1.                     NC1094.2
086100*        NOTE 21 ELEMENTARY ALPHABETIC DATA ITEMS.                NC1094.2
086200     MOVE     "UTSRQPONMLKJIHGFEDCBA" TO DIS-PLAYER.              NC1094.2
086300     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
086400     GO       TO DISP-WRITE-GF-11.                                NC1094.2
086500 DISP-DELETE-GF-11.                                               NC1094.2
086600     PERFORM  DE-LETE.                                            NC1094.2
086700 DISP-WRITE-GF-11.                                                NC1094.2
086800     MOVE     "DISP-TEST-GF-11 " TO PAR-NAME.                     NC1094.2
086900     PERFORM  PRINT-DETAIL.                                       NC1094.2
087000 DISP-INIT-GF-12.                                                 NC1094.2
087100     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
087200     MOVE     "DISP-TEST-GF-12 " TO PAR-NAME.                     NC1094.2
087300     MOVE   "ABCDEFGHIJKLMNOPQRSTU" TO TAB-VALUE.                 NC1094.2
087400 DISP-TEST-GF-12.                                                 NC1094.2
087500     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
087600     DISPLAY  XTAB (1)  XTAB (2)  XTAB (3)  XTAB (4)              NC1094.2
087700              XTAB (5)  XTAB (6)  XTAB (7)  XTAB (8)              NC1094.2
087800              XTAB (9)  XTAB (10)  XTAB (11)  XTAB (12)           NC1094.2
087900              XTAB (13)  XTAB (14)  XTAB (15)  XTAB (16)          NC1094.2
088000              XTAB (17)  XTAB (18)  XTAB (19)  XTAB (20)          NC1094.2
088100              XTAB (21).                                          NC1094.2
088200*        NOTE 21 SUBSCRIPTED DATA ITEMS.                          NC1094.2
088300     MOVE     "ABCDEFGHIJKLMNOPQRSTU" TO DIS-PLAYER.              NC1094.2
088400     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
088500     GO       TO DISP-WRITE-GF-12.                                NC1094.2
088600 DISP-DELETE-GF-12.                                               NC1094.2
088700     PERFORM  DE-LETE.                                            NC1094.2
088800 DISP-WRITE-GF-12.                                                NC1094.2
088900     MOVE     "DISP-TEST-GF-12 " TO PAR-NAME.                     NC1094.2
089000     PERFORM  PRINT-DETAIL.                                       NC1094.2
089100 DISP-INIT-GF-13.                                                 NC1094.2
089200     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
089300     MOVE     "DISP-TEST-GF-13 " TO PAR-NAME.                     NC1094.2
089400 DISP-TEST-GF-13.                                                 NC1094.2
089500     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
089600     DISPLAY  "QUOTE " QUOTE " ASTERISK " "*" " NUMERIC LITERALS "NC1094.2
089700              21 SPACE  35  I-DATA PIECE-A (1) PIECE-A (2) PIECE-ANC1094.2
089800              (3) PIECE-A (4) PIECE-A (5) PIECE-N (1) PIECE-N (2) NC1094.2
089900              PIECE-N (3) PIECE-N (4) PIECE-N (5) A1 A2.          NC1094.2
090000     MOVE       "QUOTE   ASTERISK * NUMERIC LITERALS 21 35 IDENTIFNC1094.2
090100-    "IER DATA A B C D E 0102030405 (TOTAL 21 OPERANDS) END OF DATNC1094.2
090200-    "A" TO DIS-PLAYER.                                           NC1094.2
090300*        NOTE 21 MIXED IDENTIFIERS AND LITERALS.                  NC1094.2
090400     MOVE    QUOTE TO QUOTE-SLOT.                                 NC1094.2
090500     PERFORM DISPLAY-SUPPORT-2.                                   NC1094.2
090600     GO      TO DISP-WRITE-GF-13.                                 NC1094.2
090700 DISP-DELETE-GF-13.                                               NC1094.2
090800     PERFORM DE-LETE.                                             NC1094.2
090900 DISP-WRITE-GF-13.                                                NC1094.2
091000     MOVE   "DISP-TEST-GF-13 " TO PAR-NAME.                       NC1094.2
091100     PERFORM PRINT-DETAIL.                                        NC1094.2
091200 DISP-INIT-GF-14.                                                 NC1094.2
091300*    ===---> "ALL" LITERAL  <--===                                NC1094.2
091400     MOVE   "V1-78 6.10.4" TO ANSI-REFERENCE.                     NC1094.2
091500     MOVE     "DISP-TEST-GF-14 GR3 " TO PAR-NAME.                 NC1094.2
091600 DISP-TEST-GF-14.                                                 NC1094.2
091700     PERFORM  DISPLAY-SUPPORT-1.                                  NC1094.2
091800     DISPLAY  ALL "ABCD"                                          NC1094.2
091900*        NOTE "ALL" LITERAL.                                      NC1094.2
092000     MOVE     "ABCD" TO DIS-PLAYER.                               NC1094.2
092100     PERFORM  DISPLAY-SUPPORT-2.                                  NC1094.2
092200     GO       TO DISP-WRITE-GF-14.                                NC1094.2
092300 DISP-DELETE-GF-14.                                               NC1094.2
092400     PERFORM  DE-LETE.                                            NC1094.2
092500 DISP-WRITE-GF-14.                                                NC1094.2
092600     MOVE     "DISP-TEST-GF-14 " TO PAR-NAME.                     NC1094.2
092700     PERFORM  PRINT-DETAIL.                                       NC1094.2
092800     GO TO   CCVS-EXIT.                                           NC1094.2
092900 DISPLAY-SUPPORT-1.                                               NC1094.2
093000     PERFORM  BLANK-LINE-PRINT.                                   NC1094.2
093100     MOVE     SPACE TO P-OR-F.                                    NC1094.2
093200     MOVE     SEE-BELOW TO COMPUTED-A.                            NC1094.2
093300     MOVE     SEE-BELOW TO CORRECT-A.                             NC1094.2
093400     PERFORM  PRINT-DETAIL.                                       NC1094.2
093500     MOVE     SPACE TO FEATURE.                                   NC1094.2
093600     DISPLAY  TEST-RESULTS.                                       NC1094.2
093700 DISPLAY-SUPPORT-2.                                               NC1094.2
093800     MOVE     SPACE TO TEST-RESULTS.                              NC1094.2
093900     DISPLAY  TEST-RESULTS.                                       NC1094.2
094000     MOVE     SPACE TO TEST-RESULTS.                              NC1094.2
094100     PERFORM  PRINT-DETAIL.                                       NC1094.2
094200     MOVE     CORRECT-FOLLOWS TO RE-MARK.                         NC1094.2
094300     PERFORM  PRINT-DETAIL.                                       NC1094.2
094400     PERFORM  BLANK-LINE-PRINT.                                   NC1094.2
094500     MOVE     DISPLAY-WRITER TO TEST-RESULTS.                     NC1094.2
094600     PERFORM  PRINT-DETAIL.                                       NC1094.2
094700     IF       DISPLAY-SWITCH EQUAL TO 1                           NC1094.2
094800              MOVE ZERO TO DISPLAY-SWITCH                         NC1094.2
094900              MOVE DISPLAY-H TO DIS-PLAYER                        NC1094.2
095000              MOVE DISPLAY-WRITER TO TEST-RESULTS                 NC1094.2
095100              PERFORM PRINT-DETAIL.                               NC1094.2
095200     MOVE     SPACE TO TEST-RESULTS.                              NC1094.2
095300     PERFORM  BLANK-LINE-PRINT.                                   NC1094.2
095400     IF       DISPLAY-SWITCH EQUAL TO 1                           NC1094.2
095500              MOVE "SEE NOTE IN DISP-TEST-GF-10" TO RE-MARK.      NC1094.2
095600              PERFORM PRINT-DETAIL.                               NC1094.2
095700     MOVE     "DISPLAY" TO FEATURE.                               NC1094.2
095800     MOVE     SEE-ABOVE TO COMPUTED-A.                            NC1094.2
095900     MOVE     SEE-ABOVE TO CORRECT-A.                             NC1094.2
096000     MOVE     END-CORRECT TO RE-MARK.                             NC1094.2
096100     MOVE    "ERRORS ENCOUNTERED" TO ENDER-DESC.                  NC1094.2
096200 CCVS-EXIT SECTION.                                               NC1094.2
096300 CCVS-999999.                                                     NC1094.2
096400     GO TO CLOSE-FILES.                                           NC1094.2
      *END-OF,NC109M                                                                  
