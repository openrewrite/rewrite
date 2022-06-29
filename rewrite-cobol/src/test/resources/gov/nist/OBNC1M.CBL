      *HEADER,COBOL,OBNC1M                                                            
000100 IDENTIFICATION DIVISION.                                         OBNC14.2
000200 PROGRAM-ID.                                                      OBNC14.2
000300     OBNC1M.                                                      OBNC14.2
000400                                                                  OBNC14.2
000500 AUTHOR.                                                          OBNC14.2
000600     FEDERAL COMPILER TESTING CENTRE.                             OBNC14.2
000700 INSTALLATION.                                                    OBNC14.2
000800     GENERAL SERVICES ADMINISTRATION                              OBNC14.2
000900     AUTOMATIC DATA AND TELECOMMUNICATION SERVICE                 OBNC14.2
001000     SOFTWARE DEVELOPMENT OFFICE                                  OBNC14.2
001100     5203 LEESBURG PIKE. SUITE 1100                               OBNC14.2
001200     FALLS CHURCH VIRGINIA 22041                                  OBNC14.2
001300                                                                  OBNC14.2
001400     PHONE   (703) 756-6153                                       OBNC14.2
001500 DATE-WRITTEN.                                                    OBNC14.2
001600     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           OBNC14.2
001700     CREATION DATE     /    VALIDATION DATE                       OBNC14.2
001800                                                                  OBNC14.2
001900 SECURITY.                                                        OBNC14.2
002000     NONE                                                         OBNC14.2
002100                                                                  OBNC14.2
002200                                                                  OBNC14.2
002300****************************************************************  OBNC14.2
002400*                                                              *  OBNC14.2
002500*    VALIDATION FOR:-                                          *  OBNC14.2
002600*                                                              *  OBNC14.2
002700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBNC14.2
002800*                                                              *  OBNC14.2
002900*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBNC14.2
003000*                                                              *  OBNC14.2
003100****************************************************************  OBNC14.2
003200*                                                              *  OBNC14.2
003300*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  OBNC14.2
003400*                                                              *  OBNC14.2
003500*        X-55  - SYSTEM PRINTER NAME.                          *  OBNC14.2
003600*        X-82  - SOURCE COMPUTER NAME.                         *  OBNC14.2
003700*        X-83  - OBJECT COMPUTER NAME.                         *  OBNC14.2
003800*                                                              *  OBNC14.2
003900****************************************************************  OBNC14.2
004000*                                                                 OBNC14.2
004100*     PROGRAM OBNC1M CONTAINS CCVS74 TESTS OF LEVEL 1 LANGUAGE    OBNC14.2
004200*      ELEMENTS DEFINED AS OBSOLETE IN THE 1985 STANDARDS.        OBNC14.2
004300*                                                                 OBNC14.2
004400*                                                                 OBNC14.2
004500****************************************************************  OBNC14.2
004600                                                                  OBNC14.2
004700                                                                  OBNC14.2
004800     A COMMENT ENTRY PARAGRAPH IS TO BE TREATED AS                OBNC14.2
004900     DOCUMENTATION. ANY ATTEMPT TO COMPILE ANYTHING CONTAINED     OBNC14.2
005000     HERE IS ILLEGAL. THE LINES WHICH FOLLOW CONSTITUTE A TEST    OBNC14.2
005100     OF THIS REQUIREMENT. ALL LINES BEGIN IN AREA B ---           OBNC14.2
005200                                                                  OBNC14.2
005300     ENVIRONMENT DIVISION.                                        OBNC14.2
005400     CONFIGURATION SECTION.                                       OBNC14.2
005500     SOURCE-COMPUTER.                                             OBNC14.2
005600     XXXXX082.                                                    OBNC14.2
005700     OBJECT-COMPUTER.                                             OBNC14.2
005800     XXXXX083.                                                    OBNC14.2
005900     INPUT-OUTPUT SECTION.                                        OBNC14.2
006000     FILE-CONTROL.                                                OBNC14.2
006100     SELECT PHONY-PRINT-FILE ASSIGN TO                            OBNC14.2
006200     XXXXX055.                                                    OBNC14.2
006300     DATA DIVISION.                                               OBNC14.2
006400     FILE SECTION.                                                OBNC14.2
006500     FD  PHONY-PRINT-FILE                                         OBNC14.2
006600         LABEL RECORDS OMITTED                                    OBNC14.2
006700         DATA RECORD IS PHONY-PRINT-REC.                          OBNC14.2
006800     01 PHONY-PRINT-REC PICTURE X(120).                           OBNC14.2
006900     WORKING-STORAGE SECTION.                                     OBNC14.2
007000     01  COM-MENT.                                                OBNC14.2
007100         02 FILLER      PICTURE X(56) VALUE                       OBNC14.2
007200     " CONGRATULATIONS --- YOUR COMPILER HAS JUST SUCCESSFULLY".  OBNC14.2
007300         02 FILLER      PICTURE X(51) VALUE                       OBNC14.2
007400     " COMPILED AND EXECUTED THE COBOL REMARKS PARAGRAPH.".       OBNC14.2
007500     PROCEDURE DIVISION.                                          OBNC14.2
007600     PHONY-OPEN.                                                  OBNC14.2
007700         OPEN OUTPUT PHONY-PRINT-FILE.                            OBNC14.2
007800     PHONY-WRITE.                                                 OBNC14.2
007900         MOVE COM-MENT TO PHONY-PRINT-REC.                        OBNC14.2
008000         WRITE PHONY-PRINT-REC.                                   OBNC14.2
008100     PHONY-CLOSE.                                                 OBNC14.2
008200         CLOSE PHONY-PRINT-FILE.                                  OBNC14.2
008300         STOP RUN.                                                OBNC14.2
008400     IDENTIFICATION DIVISION.                                     OBNC14.2
008500                                                                  OBNC14.2
008600                                                                  OBNC14.2
008700 ENVIRONMENT DIVISION.                                            OBNC14.2
008800 CONFIGURATION SECTION.                                           OBNC14.2
008900 SOURCE-COMPUTER.                                                 OBNC14.2
009000     XXXXX082.                                                    OBNC14.2
009100 OBJECT-COMPUTER.                                                 OBNC14.2
009200     XXXXX083                                                     OBNC14.2
009300      MEMORY SIZE                                                 OBNC14.2
009400     XXXXX067                                                     OBNC14.2
009500     WORDS.                                                       OBNC14.2
009600 INPUT-OUTPUT SECTION.                                            OBNC14.2
009700 FILE-CONTROL.                                                    OBNC14.2
009800     SELECT PRINT-FILE ASSIGN TO                                  OBNC14.2
009900     XXXXX055.                                                    OBNC14.2
010000 DATA DIVISION.                                                   OBNC14.2
010100 FILE SECTION.                                                    OBNC14.2
010200 FD  PRINT-FILE.                                                  OBNC14.2
010300 01  PRINT-REC PICTURE X(132).                                    OBNC14.2
010400 01  DUMMY-RECORD PICTURE X(132).                                 OBNC14.2
010500 WORKING-STORAGE SECTION.                                         OBNC14.2
010600 01  CHARACTER-BREAKDOWN-R.                                       OBNC14.2
010700     02  FIRST-20R               PICTURE X(20).                   OBNC14.2
010800     02  SECOND-20R              PICTURE X(20).                   OBNC14.2
010900     02  THIRD-20R               PICTURE X(20).                   OBNC14.2
011000     02  FOURTH-20R              PICTURE X(20).                   OBNC14.2
011100 01  CHARACTER-BREAKDOWN-S.                                       OBNC14.2
011200     02  FIRST-20S               PICTURE X(20).                   OBNC14.2
011300     02  SECOND-20S              PICTURE X(20).                   OBNC14.2
011400     02  THIRD-20S               PICTURE X(20).                   OBNC14.2
011500     02  FOURTH-20S              PICTURE X(20).                   OBNC14.2
011600 01  X80-CHARACTER-FIELD.                                         OBNC14.2
011700     02  FILLER                  PICTURE X(80).                   OBNC14.2
011800 01  ACCEPT-RESULTS.                                              OBNC14.2
011900     02  FILLER                  PICTURE X(80)  VALUE             OBNC14.2
012000     "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z  0123456OBNC14.2
012100-    "789                 ".                                      OBNC14.2
012200 01  ALTERLOOP                   PIC 9 VALUE ZERO.                OBNC14.2
012300 01  DISPLAY-DATA.                                                OBNC14.2
012400     02 DISPLAY-A.                                                OBNC14.2
012500       03 DISPLAY-03               PICTURE A     VALUE "A".       OBNC14.2
012600       03 DISPLAY-03A.                                            OBNC14.2
012700         04 DISPLAY-04             PICTURE A     VALUE "L".       OBNC14.2
012800         04 DISPLAY-04A.                                          OBNC14.2
012900           05 DISPLAY-05           PICTURE A     VALUE "P".       OBNC14.2
013000           05 DISPLAY-05A.                                        OBNC14.2
013100             06 DISPLAY-06         PICTURE A     VALUE "H".       OBNC14.2
013200             06 DISPLAY-06A.                                      OBNC14.2
013300               07 DISPLAY-07       PICTURE A     VALUE "A".       OBNC14.2
013400               07 DISPLAY-07A.                                    OBNC14.2
013500                 08 DISPLAY-08     PICTURE A     VALUE "B".       OBNC14.2
013600                 08 DISPLAY-08A.                                  OBNC14.2
013700                   09 DISPLAY-09   PICTURE A     VALUE "E".       OBNC14.2
013800                   09 DISPLAY-09A.                                OBNC14.2
013900                     10 DISPLAY-10 PICTURE AAA   VALUE "TIC".     OBNC14.2
014000     02 DISPLAY-N       PICTURE 9(10)       VALUE 0123456789.     OBNC14.2
014100     02 DISPLAY-X       PICTURE X(10)       VALUE "A1B2C3D4E5".   OBNC14.2
014200     02 DISPLAY-B       PICTURE X(13).                            OBNC14.2
014300     02 DISPLAY-C REDEFINES DISPLAY-B.                            OBNC14.2
014400       03 DISPLAY-D     PICTURE X(8).                             OBNC14.2
014500       03 DISPLAY-E     PICTURE X(5).                             OBNC14.2
014600     02 DISPLAY-F.                                                OBNC14.2
014700       03 DISPLAY-G     PICTURE X(100)      VALUE "*001*002*003*00OBNC14.2
014800-    "4*005*006*007*008*009*010*011*012*013*014*015*016*017*018*01OBNC14.2
014900-    "9*020*021*022*023*024*025".                                 OBNC14.2
015000       03 DISPLAY-H     PICTURE X(100)      VALUE "*026*027*028*02OBNC14.2
015100-    "9*030*031*032*033*034*035*036*037*038*039*040*041*042*043*04OBNC14.2
015200-    "4*045*046*047*048*049*050".                                 OBNC14.2
015300     02 SEE-ABOVE       PICTURE X(9) VALUE "SEE ABOVE".           OBNC14.2
015400     02 SEE-BELOW       PICTURE X(9) VALUE "SEE BELOW".           OBNC14.2
015500     02 CORRECT-FOLLOWS PICTURE X(20) VALUE                       OBNC14.2
015600                                      "CORRECT DATA FOLLOWS".     OBNC14.2
015700     02 END-CORRECT     PICTURE X(16) VALUE                       OBNC14.2
015800                                      "END CORRECT DATA".         OBNC14.2
015900     02 DISPLAY-WRITER.                                           OBNC14.2
016000       03 DIS-PLAYER.                                             OBNC14.2
016100         04 FILLER      PICTURE X(6).                             OBNC14.2
016200         04 QUOTE-SLOT  PICTURE X.                                OBNC14.2
016300         04 FILLER      PICTURE X(112).                           OBNC14.2
016400     02 DISPLAY-SWITCH  PICTURE 9 VALUE ZERO.                     OBNC14.2
016500     02 ZERO-SPACE-QUOTE.                                         OBNC14.2
016600       03 FILLER        PICTURE X VALUE ZERO.                     OBNC14.2
016700       03 FILLER        PICTURE X VALUE SPACE.                    OBNC14.2
016800       03 FILLER        PICTURE X VALUE QUOTE.                    OBNC14.2
016900 01  LONG-LITERAL.                                                OBNC14.2
017000     02 LONG20                          PICTURE IS X(20)          OBNC14.2
017100     VALUE IS "STANDARD COMPILERS M".                             OBNC14.2
017200     02 LONG40                          PICTURE IS X(20)          OBNC14.2
017300     VALUE IS "UST ALLOW NON-NUMERI".                             OBNC14.2
017400     02 LONG60                          PICTURE IS X(20)          OBNC14.2
017500     VALUE IS "C LITERALS OF AT LEA".                             OBNC14.2
017600     02 LONG80                          PICTURE IS X(20)          OBNC14.2
017700     VALUE IS "ST 120 CHARACTERS AN".                             OBNC14.2
017800     02 LONG100                         PICTURE IS X(20)          OBNC14.2
017900     VALUE IS "D NUMERIC LITERALS O".                             OBNC14.2
018000     02 LONG120                         PICTURE IS X(20)          OBNC14.2
018100     VALUE IS "F AT LEAST 18 DIGITS".                             OBNC14.2
018200 01  ACCEPT-DATA.                                                 OBNC14.2
018300     02 ACCEPT-D1.                                                OBNC14.2
018400       03 ACCEPT-D1-A PICTURE X(20).                              OBNC14.2
018500       03 ACCEPT-D1-B PICTURE X(7).                               OBNC14.2
018600     02 ACCEPT-D2       PICTURE X(27)                             OBNC14.2
018700                        VALUE "ABCDEFGHIJKLMNOPQRSTUVWXY Z".      OBNC14.2
018800     02 ACCEPT-D3  PICTURE 9(10) USAGE DISPLAY.                   OBNC14.2
018900     02 ACCEPT-D4  PICTURE 9(10) USAGE DISPLAY VALUE 0123456789.  OBNC14.2
019000     02 ACCEPT-D5  PICTURE X(11).                                 OBNC14.2
019100     02  ACCEPT-D6 PICTURE X(11) VALUE "().+-*/l, =".             OBNC14.2
019200     02 ACCEPT-D7  PICTURE X.                                     OBNC14.2
019300     02 ACCEPT-D8 PICTURE X VALUE "9".                            OBNC14.2
019400     02 ACCEPT-D9  PICTURE X.                                     OBNC14.2
019500     02 ACCEPT-D10 PICTURE X VALUE "0".                           OBNC14.2
019600     02 ACCEPT-D11 PICTURE A(20).                                 OBNC14.2
019700     02 ACCEPT-D12 PICTURE A(20)                                  OBNC14.2
019800              VALUE " ABC            XYZ ".                       OBNC14.2
019900     02 ACCEPT-D13 PICTURE 9(9).                                  OBNC14.2
020000     02 ACCEPT-D14 PICTURE 9(9) VALUE 012345678.                  OBNC14.2
020100     02 ACCEPT-D15 PICTURE X.                                     OBNC14.2
020200     02 ACCEPT-D16 PICTURE X VALUE SPACE.                         OBNC14.2
020300     02 ACCEPT-D17 PICTURE X.                                     OBNC14.2
020400     02 ACCEPT-D18 PICTURE X VALUE QUOTE.                         OBNC14.2
020500     02 ACCEPT-D21.                                               OBNC14.2
020600         03 TAB-ACCEPT PICTURE XXXX OCCURS 3 TIMES.               OBNC14.2
020700     02 ACCEPT-D22 PICTURE X(12) VALUE "....ABCD....".            OBNC14.2
020800 01  TAB-VALUE                    PICTURE X(21)                   OBNC14.2
020900                                  VALUE "ABCDEFGHIJKLMNOPQRSTU".  OBNC14.2
021000 01  NO-TAB-RECORD REDEFINES TAB-VALUE.                           OBNC14.2
021100     02 X1    PICTURE X.                                          OBNC14.2
021200     02 X2    PICTURE X.                                          OBNC14.2
021300     02 X3    PICTURE X.                                          OBNC14.2
021400     02 X4    PICTURE X.                                          OBNC14.2
021500     02 X5    PICTURE X.                                          OBNC14.2
021600     02 X6    PICTURE X.                                          OBNC14.2
021700     02 X7    PICTURE X.                                          OBNC14.2
021800     02 X8    PICTURE X.                                          OBNC14.2
021900     02 X9    PICTURE X.                                          OBNC14.2
022000     02 X10   PICTURE X.                                          OBNC14.2
022100     02 X11   PICTURE X.                                          OBNC14.2
022200     02 X12   PICTURE X.                                          OBNC14.2
022300     02 X13   PICTURE X.                                          OBNC14.2
022400     02 X14   PICTURE X.                                          OBNC14.2
022500     02 X15   PICTURE X.                                          OBNC14.2
022600     02 X16   PICTURE X.                                          OBNC14.2
022700     02 X17   PICTURE X.                                          OBNC14.2
022800     02 X18   PICTURE X.                                          OBNC14.2
022900     02 X19   PICTURE X.                                          OBNC14.2
023000     02 X20   PICTURE X.                                          OBNC14.2
023100     02 X21   PICTURE X.                                          OBNC14.2
023200 01  TAB-RECORD REDEFINES TAB-VALUE.                              OBNC14.2
023300     02 XTAB                       PICTURE X OCCURS 21 TIMES.     OBNC14.2
023400 01  DISPLAY-MIXTURE.                                             OBNC14.2
023500     02 I-DATA                    PICTURE X(17)                   OBNC14.2
023600                                  VALUE " IDENTIFIER DATA ".      OBNC14.2
023700     02 TA-VALUE                  PICTURE X(20)                   OBNC14.2
023800              VALUE "A B C D E 0102030405".                       OBNC14.2
023900     02 TA-BLE REDEFINES TA-VALUE.                                OBNC14.2
024000             04 PIECE-A           PICTURE XX OCCURS 5 TIMES.      OBNC14.2
024100             04 PIECE-N           PICTURE 99 OCCURS 5 TIMES.      OBNC14.2
024200     02 TRUE-PAIR.                                                OBNC14.2
024300         03 A1                    PICTURE X(21)                   OBNC14.2
024400                                  VALUE " (TOTAL 21 OPERANDS) ".  OBNC14.2
024500         03 A2                    PICTURE X(11)                   OBNC14.2
024600                                  VALUE "END OF DATA".            OBNC14.2
024700 01  TEST-RESULTS.                                                OBNC14.2
024800     02 FILLER                   PIC X      VALUE SPACE.          OBNC14.2
024900     02 FEATURE                  PIC X(20)  VALUE SPACE.          OBNC14.2
025000     02 FILLER                   PIC X      VALUE SPACE.          OBNC14.2
025100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          OBNC14.2
025200     02 FILLER                   PIC X      VALUE SPACE.          OBNC14.2
025300     02  PAR-NAME.                                                OBNC14.2
025400       03 FILLER                 PIC X(19)  VALUE SPACE.          OBNC14.2
025500       03  PARDOT-X              PIC X      VALUE SPACE.          OBNC14.2
025600       03 DOTVALUE               PIC 99     VALUE ZERO.           OBNC14.2
025700     02 FILLER                   PIC X(8)   VALUE SPACE.          OBNC14.2
025800     02 RE-MARK                  PIC X(61).                       OBNC14.2
025900 01  TEST-COMPUTED.                                               OBNC14.2
026000     02 FILLER                   PIC X(30)  VALUE SPACE.          OBNC14.2
026100     02 FILLER                   PIC X(17)  VALUE                 OBNC14.2
026200            "       COMPUTED=".                                   OBNC14.2
026300     02 COMPUTED-X.                                               OBNC14.2
026400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          OBNC14.2
026500     03 COMPUTED-N               REDEFINES COMPUTED-A             OBNC14.2
026600                                 PIC -9(9).9(9).                  OBNC14.2
026700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         OBNC14.2
026800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     OBNC14.2
026900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     OBNC14.2
027000     03       CM-18V0 REDEFINES COMPUTED-A.                       OBNC14.2
027100         04 COMPUTED-18V0                    PIC -9(18).          OBNC14.2
027200         04 FILLER                           PIC X.               OBNC14.2
027300     03 FILLER PIC X(50) VALUE SPACE.                             OBNC14.2
027400 01  TEST-CORRECT.                                                OBNC14.2
027500     02 FILLER PIC X(30) VALUE SPACE.                             OBNC14.2
027600     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBNC14.2
027700     02 CORRECT-X.                                                OBNC14.2
027800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         OBNC14.2
027900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      OBNC14.2
028000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         OBNC14.2
028100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     OBNC14.2
028200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     OBNC14.2
028300     03      CR-18V0 REDEFINES CORRECT-A.                         OBNC14.2
028400         04 CORRECT-18V0                     PIC -9(18).          OBNC14.2
028500         04 FILLER                           PIC X.               OBNC14.2
028600     03 FILLER PIC X(2) VALUE SPACE.                              OBNC14.2
028700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     OBNC14.2
028800 01  CCVS-C-1.                                                    OBNC14.2
028900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAOBNC14.2
029000-    "SS  PARAGRAPH-NAME                                          OBNC14.2
029100-    "       REMARKS".                                            OBNC14.2
029200     02 FILLER                     PIC X(20)    VALUE SPACE.      OBNC14.2
029300 01  CCVS-C-2.                                                    OBNC14.2
029400     02 FILLER                     PIC X        VALUE SPACE.      OBNC14.2
029500     02 FILLER                     PIC X(6)     VALUE "TESTED".   OBNC14.2
029600     02 FILLER                     PIC X(15)    VALUE SPACE.      OBNC14.2
029700     02 FILLER                     PIC X(4)     VALUE "FAIL".     OBNC14.2
029800     02 FILLER                     PIC X(94)    VALUE SPACE.      OBNC14.2
029900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       OBNC14.2
030000 01  REC-CT                        PIC 99       VALUE ZERO.       OBNC14.2
030100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       OBNC14.2
030200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       OBNC14.2
030300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       OBNC14.2
030400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       OBNC14.2
030500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       OBNC14.2
030600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       OBNC14.2
030700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      OBNC14.2
030800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       OBNC14.2
030900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     OBNC14.2
031000 01  CCVS-H-1.                                                    OBNC14.2
031100     02  FILLER                    PIC X(39)    VALUE SPACES.     OBNC14.2
031200     02  FILLER                    PIC X(42)    VALUE             OBNC14.2
031300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 OBNC14.2
031400     02  FILLER                    PIC X(39)    VALUE SPACES.     OBNC14.2
031500 01  CCVS-H-2A.                                                   OBNC14.2
031600   02  FILLER                        PIC X(40)  VALUE SPACE.      OBNC14.2
031700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  OBNC14.2
031800   02  FILLER                        PIC XXXX   VALUE             OBNC14.2
031900     "4.2 ".                                                      OBNC14.2
032000   02  FILLER                        PIC X(28)  VALUE             OBNC14.2
032100            " COPY - NOT FOR DISTRIBUTION".                       OBNC14.2
032200   02  FILLER                        PIC X(41)  VALUE SPACE.      OBNC14.2
032300                                                                  OBNC14.2
032400 01  CCVS-H-2B.                                                   OBNC14.2
032500   02  FILLER                        PIC X(15)  VALUE             OBNC14.2
032600            "TEST RESULT OF ".                                    OBNC14.2
032700   02  TEST-ID                       PIC X(9).                    OBNC14.2
032800   02  FILLER                        PIC X(4)   VALUE             OBNC14.2
032900            " IN ".                                               OBNC14.2
033000   02  FILLER                        PIC X(12)  VALUE             OBNC14.2
033100     " HIGH       ".                                              OBNC14.2
033200   02  FILLER                        PIC X(22)  VALUE             OBNC14.2
033300            " LEVEL VALIDATION FOR ".                             OBNC14.2
033400   02  FILLER                        PIC X(58)  VALUE             OBNC14.2
033500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBNC14.2
033600 01  CCVS-H-3.                                                    OBNC14.2
033700     02  FILLER                      PIC X(34)  VALUE             OBNC14.2
033800            " FOR OFFICIAL USE ONLY    ".                         OBNC14.2
033900     02  FILLER                      PIC X(58)  VALUE             OBNC14.2
034000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBNC14.2
034100     02  FILLER                      PIC X(28)  VALUE             OBNC14.2
034200            "  COPYRIGHT   1985 ".                                OBNC14.2
034300 01  CCVS-E-1.                                                    OBNC14.2
034400     02 FILLER                       PIC X(52)  VALUE SPACE.      OBNC14.2
034500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              OBNC14.2
034600     02 ID-AGAIN                     PIC X(9).                    OBNC14.2
034700     02 FILLER                       PIC X(45)  VALUE SPACES.     OBNC14.2
034800 01  CCVS-E-2.                                                    OBNC14.2
034900     02  FILLER                      PIC X(31)  VALUE SPACE.      OBNC14.2
035000     02  FILLER                      PIC X(21)  VALUE SPACE.      OBNC14.2
035100     02 CCVS-E-2-2.                                               OBNC14.2
035200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      OBNC14.2
035300         03 FILLER                   PIC X      VALUE SPACE.      OBNC14.2
035400         03 ENDER-DESC               PIC X(44)  VALUE             OBNC14.2
035500            "ERRORS ENCOUNTERED".                                 OBNC14.2
035600 01  CCVS-E-3.                                                    OBNC14.2
035700     02  FILLER                      PIC X(22)  VALUE             OBNC14.2
035800            " FOR OFFICIAL USE ONLY".                             OBNC14.2
035900     02  FILLER                      PIC X(12)  VALUE SPACE.      OBNC14.2
036000     02  FILLER                      PIC X(58)  VALUE             OBNC14.2
036100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBNC14.2
036200     02  FILLER                      PIC X(13)  VALUE SPACE.      OBNC14.2
036300     02 FILLER                       PIC X(15)  VALUE             OBNC14.2
036400             " COPYRIGHT 1985".                                   OBNC14.2
036500 01  CCVS-E-4.                                                    OBNC14.2
036600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      OBNC14.2
036700     02 FILLER                       PIC X(4)   VALUE " OF ".     OBNC14.2
036800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      OBNC14.2
036900     02 FILLER                       PIC X(40)  VALUE             OBNC14.2
037000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBNC14.2
037100 01  XXINFO.                                                      OBNC14.2
037200     02 FILLER                       PIC X(19)  VALUE             OBNC14.2
037300            "*** INFORMATION ***".                                OBNC14.2
037400     02 INFO-TEXT.                                                OBNC14.2
037500       04 FILLER                     PIC X(8)   VALUE SPACE.      OBNC14.2
037600       04 XXCOMPUTED                 PIC X(20).                   OBNC14.2
037700       04 FILLER                     PIC X(5)   VALUE SPACE.      OBNC14.2
037800       04 XXCORRECT                  PIC X(20).                   OBNC14.2
037900     02 INF-ANSI-REFERENCE           PIC X(48).                   OBNC14.2
038000 01  HYPHEN-LINE.                                                 OBNC14.2
038100     02 FILLER  PIC IS X VALUE IS SPACE.                          OBNC14.2
038200     02 FILLER  PIC IS X(65)    VALUE IS "************************OBNC14.2
038300-    "*****************************************".                 OBNC14.2
038400     02 FILLER  PIC IS X(54)    VALUE IS "************************OBNC14.2
038500-    "******************************".                            OBNC14.2
038600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             OBNC14.2
038700     "OBNC1M".                                                    OBNC14.2
038800 PROCEDURE DIVISION.                                              OBNC14.2
038900 CCVS1 SECTION.                                                   OBNC14.2
039000 OPEN-FILES.                                                      OBNC14.2
039100     OPEN     OUTPUT PRINT-FILE.                                  OBNC14.2
039200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBNC14.2
039300     MOVE    SPACE TO TEST-RESULTS.                               OBNC14.2
039400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBNC14.2
039500     GO TO CCVS1-EXIT.                                            OBNC14.2
039600 CLOSE-FILES.                                                     OBNC14.2
039700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBNC14.2
039800 TERMINATE-CCVS.                                                  OBNC14.2
039900*    EXIT PROGRAM.                                                OBNC14.2
040000*TERMINATE-CALL.                                                  OBNC14.2
040100     STOP     RUN.                                                OBNC14.2
040200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBNC14.2
040300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBNC14.2
040400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBNC14.2
040500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      OBNC14.2
040600     MOVE "****TEST DELETED****" TO RE-MARK.                      OBNC14.2
040700 PRINT-DETAIL.                                                    OBNC14.2
040800     IF REC-CT NOT EQUAL TO ZERO                                  OBNC14.2
040900             MOVE "." TO PARDOT-X                                 OBNC14.2
041000             MOVE REC-CT TO DOTVALUE.                             OBNC14.2
041100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBNC14.2
041200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBNC14.2
041300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBNC14.2
041400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBNC14.2
041500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBNC14.2
041600     MOVE SPACE TO CORRECT-X.                                     OBNC14.2
041700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBNC14.2
041800     MOVE     SPACE TO RE-MARK.                                   OBNC14.2
041900 HEAD-ROUTINE.                                                    OBNC14.2
042000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  OBNC14.2
042100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  OBNC14.2
042200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  OBNC14.2
042300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  OBNC14.2
042400 COLUMN-NAMES-ROUTINE.                                            OBNC14.2
042500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBNC14.2
042600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC14.2
042700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBNC14.2
042800 END-ROUTINE.                                                     OBNC14.2
042900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBNC14.2
043000 END-RTN-EXIT.                                                    OBNC14.2
043100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC14.2
043200 END-ROUTINE-1.                                                   OBNC14.2
043300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBNC14.2
043400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               OBNC14.2
043500      ADD PASS-COUNTER TO ERROR-HOLD.                             OBNC14.2
043600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBNC14.2
043700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBNC14.2
043800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBNC14.2
043900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBNC14.2
044000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBNC14.2
044100  END-ROUTINE-12.                                                 OBNC14.2
044200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBNC14.2
044300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBNC14.2
044400         MOVE "NO " TO ERROR-TOTAL                                OBNC14.2
044500         ELSE                                                     OBNC14.2
044600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBNC14.2
044700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBNC14.2
044800     PERFORM WRITE-LINE.                                          OBNC14.2
044900 END-ROUTINE-13.                                                  OBNC14.2
045000     IF DELETE-COUNTER IS EQUAL TO ZERO                           OBNC14.2
045100         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBNC14.2
045200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      OBNC14.2
045300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBNC14.2
045400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBNC14.2
045500      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBNC14.2
045600          MOVE "NO " TO ERROR-TOTAL                               OBNC14.2
045700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBNC14.2
045800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBNC14.2
045900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBNC14.2
046000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBNC14.2
046100 WRITE-LINE.                                                      OBNC14.2
046200     ADD 1 TO RECORD-COUNT.                                       OBNC14.2
046300*    IF RECORD-COUNT GREATER 42                                   OBNC14.2
046400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBNC14.2
046500*        MOVE SPACE TO DUMMY-RECORD                               OBNC14.2
046600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBNC14.2
046700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   OBNC14.2
046800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   OBNC14.2
046900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   OBNC14.2
047000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   OBNC14.2
047100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           OBNC14.2
047200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           OBNC14.2
047300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBNC14.2
047400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBNC14.2
047500*        MOVE ZERO TO RECORD-COUNT.                               OBNC14.2
047600     PERFORM WRT-LN.                                              OBNC14.2
047700 WRT-LN.                                                          OBNC14.2
047800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBNC14.2
047900     MOVE SPACE TO DUMMY-RECORD.                                  OBNC14.2
048000 BLANK-LINE-PRINT.                                                OBNC14.2
048100     PERFORM WRT-LN.                                              OBNC14.2
048200 FAIL-ROUTINE.                                                    OBNC14.2
048300     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. OBNC14.2
048400     IF   CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.  OBNC14.2
048500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 OBNC14.2
048600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   OBNC14.2
048700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC14.2
048800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         OBNC14.2
048900     GO TO  FAIL-ROUTINE-EX.                                      OBNC14.2
049000 FAIL-ROUTINE-WRITE.                                              OBNC14.2
049100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         OBNC14.2
049200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 OBNC14.2
049300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. OBNC14.2
049400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         OBNC14.2
049500 FAIL-ROUTINE-EX. EXIT.                                           OBNC14.2
049600 BAIL-OUT.                                                        OBNC14.2
049700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   OBNC14.2
049800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           OBNC14.2
049900 BAIL-OUT-WRITE.                                                  OBNC14.2
050000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBNC14.2
050100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 OBNC14.2
050200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBNC14.2
050300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         OBNC14.2
050400 BAIL-OUT-EX. EXIT.                                               OBNC14.2
050500 CCVS1-EXIT.                                                      OBNC14.2
050600     EXIT.                                                        OBNC14.2
050700 SECT-NC107A-001 SECTION.                                         OBNC14.2
050800 REMARKS-TEST.                                                    OBNC14.2
050900     MOVE   "IV-11 7.2.4" TO ANSI-REFERENCE.                      OBNC14.2
051000     MOVE   "COBOL REMARKS PARA" TO FEATURE.                      OBNC14.2
051100     MOVE   "REMARKS" TO PAR-NAME.                                OBNC14.2
051200     MOVE   "PHONY LINES SHOULDNT EXECUT" TO RE-MARK.             OBNC14.2
051300     PERFORM PRINT-DETAIL.                                        OBNC14.2
051400*    THE FOLLOWING HAS BEEN MOVED FROM THE END TO ENSURE          OBNC14.2
051500*    EXECUTION                                                    OBNC14.2
051600 NOTE-TEST-6.                                                     OBNC14.2
051700*    PERFORM  FAIL.                                               OBNC14.2
051800*        NOTE ENTER                         GO TO NOTE-WRITE-6    OBNC14.2
051900*             USE                           GO TO NOTE-WRITE-6    OBNC14.2
052000*             DECLARATIVES                  GO TO NOTE-WRITE-6    OBNC14.2
052100*             DATA DIVISION                 GO TO NOTE-WRITE-6    OBNC14.2
052200*             COPY (SEE ALSO PROGRAM LB104) GO TO NOTE-WRITE-6    OBNC14.2
052300*             THE COMPILER SHOULD "IGNORE" THE ABOVE WORDS.       OBNC14.2
052400*    PERFORM PASS.                                                OBNC14.2
052500     GO TO   NOTE-WRITE-6.                                        OBNC14.2
052600 NOTE-DELETE-6.                                                   OBNC14.2
052700     PERFORM DE-LETE.                                             OBNC14.2
052800 NOTE-WRITE-6.                                                    OBNC14.2
052900     MOVE   "NOTE RESERVED WORDS" TO FEATURE.                     OBNC14.2
053000     MOVE   "NOTE-TEST-6" TO PAR-NAME.                            OBNC14.2
053100     PERFORM PRINT-DETAIL.                                        OBNC14.2
053200 NUM-INIT-1.                                                      OBNC14.2
053300     MOVE   "NUMERIC PARA-NAMES" TO FEATURE.                      OBNC14.2
053400     MOVE   "VI-75 6.7.2" TO ANSI-REFERENCE.                      OBNC14.2
053500     PERFORM  PRINT-DETAIL.                                       OBNC14.2
053600 NUM-TEST-1.                                                      OBNC14.2
053700     ALTER    02 TO PROCEED TO 77.                                OBNC14.2
053800     GO       TO 02.                                              OBNC14.2
053900 NUM-DELETE-1.                                                    OBNC14.2
054000     PERFORM  DE-LETE.                                            OBNC14.2
054100     GO       TO NUM-WRITE-1.                                     OBNC14.2
054200 02.                                                              OBNC14.2
054300     GO       TO 50.                                              OBNC14.2
054400 50. PERFORM  FAIL.                                               OBNC14.2
054500     GO       TO NUM-WRITE-1.                                     OBNC14.2
054600 77.                                                              OBNC14.2
054700     PERFORM  PASS.                                               OBNC14.2
054800 NUM-WRITE-1.                                                     OBNC14.2
054900     MOVE     "NUM-TEST-1" TO PAR-NAME.                           OBNC14.2
055000     PERFORM  PRINT-DETAIL.                                       OBNC14.2
055100 ALTER-INIT.                                                      OBNC14.2
055200     MOVE   "ALTER" TO FEATURE.                                   OBNC14.2
055300     MOVE   "VI-75 6.7.2" TO ANSI-REFERENCE.                      OBNC14.2
055400 ALTER-TEST-1.                                                    OBNC14.2
055500     ALTER   ALTER-A TO PROCEED TO ALTER-C.                       OBNC14.2
055600     GO TO   ALTER-A.                                             OBNC14.2
055700 ALTER-DELETE-1.                                                  OBNC14.2
055800     PERFORM DE-LETE.                                             OBNC14.2
055900     GO TO   ALTER-WRITE-1.                                       OBNC14.2
056000 ALTER-A.                                                         OBNC14.2
056100     GO TO   ALTER-B.                                             OBNC14.2
056200 ALTER-B.                                                         OBNC14.2
056300     PERFORM FAIL.                                                OBNC14.2
056400     GO TO   ALTER-WRITE-1.                                       OBNC14.2
056500 ALTER-C.                                                         OBNC14.2
056600     PERFORM PASS.                                                OBNC14.2
056700 ALTER-WRITE-1.                                                   OBNC14.2
056800     MOVE   "ALTER-TEST-1" TO PAR-NAME.                           OBNC14.2
056900     PERFORM PRINT-DETAIL.                                        OBNC14.2
057000 ALTER-TEST-2.                                                    OBNC14.2
057100     ALTER   ALTER-D TO ALTER-F.                                  OBNC14.2
057200*    NOTE THE WORDS "PROCEED TO" ARE OPTIONAL.                    OBNC14.2
057300     GO TO   ALTER-D.                                             OBNC14.2
057400 ALTER-DELETE-2.                                                  OBNC14.2
057500     PERFORM DE-LETE.                                             OBNC14.2
057600     GO TO   ALTER-WRITE-2.                                       OBNC14.2
057700 ALTER-D.                                                         OBNC14.2
057800     GO TO   ALTER-E.                                             OBNC14.2
057900 ALTER-E.                                                         OBNC14.2
058000     PERFORM FAIL.                                                OBNC14.2
058100     GO TO   ALTER-WRITE-2.                                       OBNC14.2
058200 ALTER-F.                                                         OBNC14.2
058300     PERFORM PASS.                                                OBNC14.2
058400 ALTER-WRITE-2.                                                   OBNC14.2
058500     MOVE   "ALTER-TEST-2" TO PAR-NAME.                           OBNC14.2
058600     PERFORM PRINT-DETAIL.                                        OBNC14.2
058700 ALTER-TEST-3.                                                    OBNC14.2
058800     ALTER   ALTER-G TO PROCEED TO ALTER-I.                       OBNC14.2
058900*    NOTE COMPOUND ALTERS, MULTIPLE ALTERS OF THE SAME SEQUENCE.  OBNC14.2
059000     GO TO   ALTER-G.                                             OBNC14.2
059100 ALTER-DELETE-3.                                                  OBNC14.2
059200     PERFORM DE-LETE.                                             OBNC14.2
059300     GO TO   ALTER-WRITE-3.                                       OBNC14.2
059400 ALTER-G.                                                         OBNC14.2
059500     GO TO   ALTER-H.                                             OBNC14.2
059600 ALTER-H.                                                         OBNC14.2
059700     PERFORM FAIL.                                                OBNC14.2
059800     GO TO   ALTER-WRITE-3.                                       OBNC14.2
059900 ALTER-I.                                                         OBNC14.2
060000     ADD     1 TO ALTERLOOP.                                      OBNC14.2
060100     IF      ALTERLOOP GREATER THAN 1                             OBNC14.2
060200              PERFORM FAIL                                        OBNC14.2
060300             GO TO ALTER-WRITE-3.                                 OBNC14.2
060400     ALTER   ALTER-G TO PROCEED TO ALTER-J.                       OBNC14.2
060500     GO TO   ALTER-G.                                             OBNC14.2
060600 ALTER-J.                                                         OBNC14.2
060700     PERFORM PASS.                                                OBNC14.2
060800 ALTER-WRITE-3.                                                   OBNC14.2
060900     MOVE   "ALTER-TEST-3" TO PAR-NAME.                           OBNC14.2
061000     PERFORM PRINT-DETAIL.                                        OBNC14.2
061100*                                                                 OBNC14.2
061200 GO--TEST-1.                                                      OBNC14.2
061300     ALTER GO--A TO PROCEED TO GO--C.                             OBNC14.2
061400*    NOTE THE GO STATEMENT IN GO--A IS NOT LEGAL UNLESS IT IS     OBNC14.2
061500*        ALTERED AS SHOWN ABOVE BEFORE CONTROL PASSES TO IT.      OBNC14.2
061600     GO TO GO--A.                                                 OBNC14.2
061700 GO--DELETE-1.                                                    OBNC14.2
061800     PERFORM DE-LETE.                                             OBNC14.2
061900     GO TO GO--WRITE-1.                                           OBNC14.2
062000 GO--A.                                                           OBNC14.2
062100     GO TO.                                                       OBNC14.2
062200 GO--B.                                                           OBNC14.2
062300     PERFORM FAIL.                                                OBNC14.2
062400     GO TO GO--WRITE-1.                                           OBNC14.2
062500 GO--C.                                                           OBNC14.2
062600     PERFORM PASS.                                                OBNC14.2
062700 GO--WRITE-1.                                                     OBNC14.2
062800     PERFORM END-ROUTINE.                                         OBNC14.2
062900     MOVE "UNFINISHED GO TO" TO FEATURE.                          OBNC14.2
063000     MOVE "GO--TEST-1" TO PAR-NAME.                               OBNC14.2
063100     PERFORM PRINT-DETAIL.                                        OBNC14.2
063200 COMMENT-ENTRIES-INIT.                                            OBNC14.2
063300     MOVE "VI-6 3.2.1.1" TO ANSI-REFERENCE.                       OBNC14.2
063400 COMMENT-ENTRIES-TEST.                                            OBNC14.2
063500     MOVE "PLEASE VISUALLY VERIFY THE FOLLOWING PARAGRAPHS: "     OBNC14.2
063600        TO RE-MARK.                                               OBNC14.2
063700     PERFORM PRINT-DETAIL.                                        OBNC14.2
063800     MOVE "  AUTHOR, INSTALLATION, DATE-WRITTEN, SECURITY"        OBNC14.2
063900        TO RE-MARK.                                               OBNC14.2
064000     PERFORM PRINT-DETAIL.                                        OBNC14.2
064100 SECT-NC180M-001 SECTION.                                         OBNC14.2
064200 STOP-INIT-GF-1.                                                  OBNC14.2
064300     MOVE     "STOP LITERAL" TO FEATURE.                          OBNC14.2
064400     MOVE  "VI-88 6.14.3 SR3, 6.14.4 GR2" TO ANSI-REFERENCE.      OBNC14.2
064500 STOP-TEST-GF-1.                                                  OBNC14.2
064600     STOP "OPERATOR PLEASE EXECUTE RUN CONTINUATION".             OBNC14.2
064700     PERFORM  PASS.                                               OBNC14.2
064800     GO TO    STOP-WRITE-GF-1.                                    OBNC14.2
064900 STOP-DELETE-GF-1.                                                OBNC14.2
065000     PERFORM  DE-LETE.                                            OBNC14.2
065100 STOP-WRITE-GF-1.                                                 OBNC14.2
065200     MOVE "STOP-TEST-GF-1" TO PAR-NAME.                           OBNC14.2
065300     PERFORM  PRINT-DETAIL.                                       OBNC14.2
065400*STOP-NOTE.                                                       OBNC14.2
065500*        NOTE THE ABOVE TEST TESTS THE BASIC FUNCTIONING OF THE   OBNC14.2
065600*             STOP VERB WITH LITERAL - A MESSAGE TO THE OPERATOR  OBNC14.2
065700*             AND RESTART ABILITY. THE FOLLOWING TESTS ASCERTAIN  OBNC14.2
065800*             THAT THE "LITERAL" MAY BE ANY LEGAL COBOL LITERAL.  OBNC14.2
065900*             THE USER MUST VISUALLY CHECK THE MESSAGES TO THE    OBNC14.2
066000*             OPERATOR AND SEE THAT THEY ARE IDENTICAL TO THE     OBNC14.2
066100*             DATA SHOWN IN THE OUTPUT LISTING.                   OBNC14.2
066200 STOP-INIT-GF-2.                                                  OBNC14.2
066300     MOVE     "SEE STOP-NOTE PARAGRAPH" TO RE-MARK.               OBNC14.2
066400     PERFORM  PRINT-DETAIL.                                       OBNC14.2
066500 STOP-TEST-GF-2.                                                  OBNC14.2
066600     STOP     "A".                                                OBNC14.2
066700     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
066800     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
066900     GO       TO STOP-WRITE-GF-2.                                 OBNC14.2
067000 STOP-DELETE-GF-2.                                                OBNC14.2
067100     PERFORM  DE-LETE.                                            OBNC14.2
067200 STOP-WRITE-GF-2.                                                 OBNC14.2
067300     MOVE     "STOP-TEST-GF-2 " TO PAR-NAME.                      OBNC14.2
067400     PERFORM  PRINT-DETAIL.                                       OBNC14.2
067500     MOVE     " A" TO PRINT-REC.                                  OBNC14.2
067600     WRITE    PRINT-REC.                                          OBNC14.2
067700     MOVE    SPACE TO TEST-RESULTS.                               OBNC14.2
067800 STOP-TEST-GF-3.                                                  OBNC14.2
067900     STOP     "*".                                                OBNC14.2
068000     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
068100     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
068200     GO       TO STOP-WRITE-GF-3.                                 OBNC14.2
068300 STOP-DELETE-GF-3.                                                OBNC14.2
068400     PERFORM  DE-LETE.                                            OBNC14.2
068500 STOP-WRITE-GF-3.                                                 OBNC14.2
068600     MOVE     "STOP-TEST-GF-3 " TO PAR-NAME.                      OBNC14.2
068700     PERFORM  PRINT-DETAIL.                                       OBNC14.2
068800     MOVE     " *" TO PRINT-REC.                                  OBNC14.2
068900     WRITE    PRINT-REC.                                          OBNC14.2
069000 STOP-TEST-GF-4.                                                  OBNC14.2
069100     STOP     QUOTE.                                              OBNC14.2
069200     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
069300     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
069400     GO       TO STOP-WRITE-GF-4.                                 OBNC14.2
069500 STOP-DELETE-GF-4.                                                OBNC14.2
069600     PERFORM  DE-LETE.                                            OBNC14.2
069700 STOP-WRITE-GF-4.                                                 OBNC14.2
069800     MOVE     "STOP-TEST-GF-4 " TO PAR-NAME.                      OBNC14.2
069900     PERFORM PRINT-DETAIL.                                        OBNC14.2
070000     MOVE   " (A SINGLE QUOTE)" TO PRINT-REC.                     OBNC14.2
070100     WRITE   PRINT-REC.                                           OBNC14.2
070200 STOP-TEST-GF-5.                                                  OBNC14.2
070300     MOVE   "IV-9 4.3.3.3.1" TO ANSI-REFERENCE.                   OBNC14.2
070400     STOP     " *  5 * 10 * 15 * 20 * 25 * 30 * 35 * 40 * 45 * 50 OBNC14.2
070500-    "* 55 * 60 * 65 * 70 * 75 * 80 * 85 * 90 * 95 *100 *105 *110 OBNC14.2
070600-    "*115 *120 *125 *130 *135 *140 *145 *150 *155 *160".         OBNC14.2
070700     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
070800     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
070900     GO       TO STOP-WRITE-GF-5.                                 OBNC14.2
071000 STOP-DELETE-GF-5.                                                OBNC14.2
071100     PERFORM  DE-LETE.                                            OBNC14.2
071200 STOP-WRITE-GF-5.                                                 OBNC14.2
071300     MOVE     "STOP-TEST-GF-5 " TO PAR-NAME.                      OBNC14.2
071400     PERFORM  PRINT-DETAIL.                                       OBNC14.2
071500     MOVE     " *  5 * 10 * 15 * 20 * 25 * 30 * 35 * 40 * 45 * 50 OBNC14.2
071600-    "* 55 * 60 * 65 * 70 * 75 * 80 * 85 * 90 * 95 *100 *105 *110 OBNC14.2
071700-    "*115 *120 *125 *130 *135 *140 *145 *150 *155 *160"          OBNC14.2
071800           TO PRINT-REC.                                          OBNC14.2
071900     WRITE    PRINT-REC.                                          OBNC14.2
072000 STOP-TEST-GF-6.                                                  OBNC14.2
072100     MOVE   "VI-129 6.23.4" TO ANSI-REFERENCE.                    OBNC14.2
072200     STOP     7.                                                  OBNC14.2
072300     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
072400     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
072500     GO       TO STOP-WRITE-GF-6.                                 OBNC14.2
072600 STOP-DELETE-GF-6.                                                OBNC14.2
072700     PERFORM  DE-LETE.                                            OBNC14.2
072800 STOP-WRITE-GF-6.                                                 OBNC14.2
072900     MOVE     "STOP-TEST-GF-6 " TO PAR-NAME.                      OBNC14.2
073000     PERFORM  PRINT-DETAIL.                                       OBNC14.2
073100     MOVE     " 7" TO PRINT-REC.                                  OBNC14.2
073200     WRITE    PRINT-REC.                                          OBNC14.2
073300 STOP-TEST-GF-7.                                                  OBNC14.2
073400     STOP     123456789987654321.                                 OBNC14.2
073500     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
073600     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
073700     GO       TO STOP-WRITE-GF-7.                                 OBNC14.2
073800 STOP-DELETE-GF-7.                                                OBNC14.2
073900     PERFORM  DE-LETE.                                            OBNC14.2
074000 STOP-WRITE-GF-7.                                                 OBNC14.2
074100     MOVE     "STOP-TEST-GF-7 " TO PAR-NAME.                      OBNC14.2
074200     PERFORM  PRINT-DETAIL.                                       OBNC14.2
074300     MOVE     " 123456789987654321" TO PRINT-REC.                 OBNC14.2
074400     WRITE    PRINT-REC.                                          OBNC14.2
074500 STOP-TEST-GF-8.                                                  OBNC14.2
074600     STOP     ZERO.                                               OBNC14.2
074700     MOVE     "SEE OPERATOR MESSAGE" TO COMPUTED-A.               OBNC14.2
074800     MOVE     "SEE BELOW" TO CORRECT-A.                           OBNC14.2
074900     GO       TO STOP-WRITE-GF-8.                                 OBNC14.2
075000 STOP-DELETE-GF-8.                                                OBNC14.2
075100     PERFORM  DE-LETE.                                            OBNC14.2
075200 STOP-WRITE-GF-8.                                                 OBNC14.2
075300     MOVE "STOP-TEST-GF-8" TO PAR-NAME.                           OBNC14.2
075400     PERFORM  PRINT-DETAIL.                                       OBNC14.2
075500     MOVE     " 0" TO PRINT-REC.                                  OBNC14.2
075600     WRITE    PRINT-REC.                                          OBNC14.2
075700 STOP-TEST-GF-9.                                                  OBNC14.2
075800     MOVE SPACE TO DUMMY-RECORD.                                  OBNC14.2
075900     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           OBNC14.2
076000     MOVE     " STOP-TEST-GF-9 PASSES UNLESS A SECOND REPORT FOR  OBNC14.2
076100-    "OBNC1 IS GENERATED AFTER THIS ONE." TO TEST-RESULTS.        OBNC14.2
076200     PERFORM  PRINT-DETAIL.                                       OBNC14.2
076300     MOVE    SPACE TO TEST-RESULTS.                               OBNC14.2
076400     PERFORM    END-ROUTINE THRU END-ROUTINE-13.                  OBNC14.2
076500     CLOSE    PRINT-FILE.                                         OBNC14.2
076600     STOP     "OPERATOR KILL OBNC1".                              OBNC14.2
076700     MOVE ZEROES TO ERROR-HOLD.                                   OBNC14.2
076800     OPEN     OUTPUT PRINT-FILE.                                  OBNC14.2
076900     PERFORM  HEAD-ROUTINE THROUGH COLUMN-NAMES-ROUTINE.          OBNC14.2
077000     PERFORM  FAIL.                                               OBNC14.2
077100     MOVE     "EXECUTION DID NOT HALT" TO RE-MARK.                OBNC14.2
077200     GO       TO STOP-WRITE-GF-9.                                 OBNC14.2
077300 STOP-DELETE-GF-9.                                                OBNC14.2
077400     PERFORM  DE-LETE.                                            OBNC14.2
077500 STOP-WRITE-GF-9.                                                 OBNC14.2
077600     MOVE     "STOP LITERAL" TO FEATURE.                          OBNC14.2
077700     MOVE     "STOP-GF-9-TEST" TO PAR-NAME.                       OBNC14.2
077800     PERFORM  PRINT-DETAIL.                                       OBNC14.2
077900                                                                  OBNC14.2
078000 CCVS-EXIT SECTION.                                               OBNC14.2
078100 CCVS-999999.                                                     OBNC14.2
078200     GO TO CLOSE-FILES.                                           OBNC14.2
      *END-OF,OBNC1M                                                                  
