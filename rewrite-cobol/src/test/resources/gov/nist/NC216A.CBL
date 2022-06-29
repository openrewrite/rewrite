      *HEADER,COBOL,NC216A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2164.2
000200 PROGRAM-ID.                                                      NC2164.2
000300     NC216A.                                                      NC2164.2
000400****************************************************************  NC2164.2
000500*                                                              *  NC2164.2
000600*    VALIDATION FOR:-                                          *  NC2164.2
000700*                                                              *  NC2164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2164.2
000900*                                                              *  NC2164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2164.2
001100*                                                              *  NC2164.2
001200****************************************************************  NC2164.2
001300*                                                              *  NC2164.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2164.2
001500*                                                              *  NC2164.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2164.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2164.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2164.2
001900*                                                              *  NC2164.2
002000****************************************************************  NC2164.2
002100*                                                              *  NC2164.2
002200*    PROGRAM NC216A TESTS ALL FOUR FORMATS OF THE "INSPECT"    *  NC2164.2
002300*    STATEMENT USING VARIOUS COMBINATIONS OF THE OPTIONAL      *  NC2164.2
002400*    PHRASES: CHARACTERS, ALL, LEADING, FIRST, BEFORE, AFTER.  *  NC2164.2
002500*                                                              *  NC2164.2
002600****************************************************************  NC2164.2
002700 ENVIRONMENT DIVISION.                                            NC2164.2
002800 CONFIGURATION SECTION.                                           NC2164.2
002900 SOURCE-COMPUTER.                                                 NC2164.2
003000     XXXXX082.                                                    NC2164.2
003100 OBJECT-COMPUTER.                                                 NC2164.2
003200     XXXXX083.                                                    NC2164.2
003300 INPUT-OUTPUT SECTION.                                            NC2164.2
003400 FILE-CONTROL.                                                    NC2164.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC2164.2
003600     XXXXX055.                                                    NC2164.2
003700 DATA DIVISION.                                                   NC2164.2
003800 FILE SECTION.                                                    NC2164.2
003900 FD  PRINT-FILE.                                                  NC2164.2
004000 01  PRINT-REC PICTURE X(120).                                    NC2164.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC2164.2
004200 WORKING-STORAGE SECTION.                                         NC2164.2
004300 01  WRK-DU-999-1 PIC 999.                                        NC2164.2
004400 01  WRK-DU-999-2 PIC 999.                                        NC2164.2
004500 01  WRK-DU-999-3 PIC 999.                                        NC2164.2
004600 01  WRK-DU-999-4 PIC 999.                                        NC2164.2
004700 01  JUST-XN-20-1 PIC X(20) JUSTIFIED.                            NC2164.2
004800 01  SPACE-XN-1-1 PIC X VALUE SPACE.                              NC2164.2
004900 01  COMMA-XN-1-1 PIC X VALUE ",".                                NC2164.2
005000 01  HYPEN-XN-1-1 PIC X VALUE "-".                                NC2164.2
005100 01  A-XN-1-1 PIC X VALUE "A".                                    NC2164.2
005200 01  D-XN-1-1 PIC X VALUE "D".                                    NC2164.2
005300 01  G-XN-1-1 PIC X VALUE "G".                                    NC2164.2
005400 01  H-XN-1-1 PIC X VALUE "H".                                    NC2164.2
005500 01  L-XN-1-1 PIC X VALUE "L".                                    NC2164.2
005600 01  O-XN-1-1 PIC X VALUE "O".                                    NC2164.2
005700 01  P-XN-1-1 PIC X VALUE "P".                                    NC2164.2
005800 01  S-XN-1-1 PIC X VALUE "S".                                    NC2164.2
005900 01  Z-XN-1-1 PIC X VALUE "Z".                                    NC2164.2
006000 01  AH-XN-2 PIC X(2) VALUE "AH".                                 NC2164.2
006100 01  HSPACE-XN-2 PIC X(2) VALUE "H ".                             NC2164.2
006200 01  OH-XN-2 PIC X(2) VALUE "OH".                                 NC2164.2
006300 01  ALL-XN-3 PIC X(3) VALUE "ALL".                               NC2164.2
006400 01  YES-XN-3 PIC X(3) VALUE "YES".                               NC2164.2
006500 01  X-SPACE-X-XN-3                  PICTURE X(3) VALUE "X X".    NC2164.2
006600 01  AABA-XN-4                       PICTURE X(4) VALUE "AABA".   NC2164.2
006700 01  WRK-XN-83-1 PIC X(83).                                       NC2164.2
006800 01  WRK-XN-83-2  PIC X(83).                                      NC2164.2
006900 01  WRK-DS-5V0-1  PIC S9(5) VALUE -12345.                        NC2164.2
007000 01  WRK-NE-1  PIC -999,999.99/9 VALUE "-123,456.78/9".           NC2164.2
007100 01  KIDS-CAN-NOT-BE  PIC  X(15)  VALUE "KIDS CAN NOT BE".        NC2164.2
007200 01  BLANK-PERIOD PIC X(2) VALUE " ." .                           NC2164.2
007300 01  WC-XN-83  PIC X(83) VALUE                                    NC2164.2
007400     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
007500-    "IDS CAN NOT BE ALL BAD.".                                   NC2164.2
007600 01  ANS-XN-83-1 PIC X(83) VALUE                                  NC2164.2
007700     "OH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
007800-    "IDS CAN NOT BE ALL BAD.".                                   NC2164.2
007900 01  ANS-XN-83-2 PIC X(83) VALUE                                  NC2164.2
008000     "AH YES,AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
008100-    "IDS CAN NOT BE ALL BAD.".                                   NC2164.2
008200 01  ANS-XN-83-3 PIC X(83) VALUE                                  NC2164.2
008300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
008400-    "IDS CAN NOT BE ALL-BAD.".                                   NC2164.2
008500 01  ANS-XN-83-4 PIC X(83) VALUE                                  NC2164.2
008600     "EH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
008700-    "IDS CAN NOT BE ALL BAD.".                                   NC2164.2
008800 01  ANS-XN-83-5 PIC X(83) VALUE                                  NC2164.2
008900     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
009000-    "IDS CAN NOT BE ALL BAD.".                                   NC2164.2
009100 01  ANS-XN-83-6 PIC X(83) VALUE                                  NC2164.2
009200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
009300-    "IDS CAN NOT BE ALZZZZZZ".                                   NC2164.2
009400 01  ANS-XN-83-7 PIC X(83) VALUE                                  NC2164.2
009500     "OH-YES-AH-YES-W.P.-ZRITOES-HERE.-ANYONE-WHO-HATES-DOGS-AND-KNC2164.2
009600-    "IDS-CAN-NOT-BE-ALZZZZZZ".                                   NC2164.2
009700 01  ANS-XN-83-8 PIC X(83) VALUE                                  NC2164.2
009800     "AH-YES-AH-YES-W.C.-FRITOES-HERE.-ANYONE-WHO-HATES-DOGS-AND-KNC2164.2
009900-    "IDS-CAN-NOT-BE-ALL-BAD.".                                   NC2164.2
010000 01  ANS-XN-83-9  PIC X(83) VALUE                                 NC2164.2
010100     "OH YES AH YES W.C. FROTOES HERE, ANYONE WHO HATES DOGS AND KNC2164.2
010200-    "IDS CAN NOT BE ALL BAD.".                                   NC2164.2
010300 01  ANS-XN-83-10 PIC X(83) VALUE                                 NC2164.2
010400     "OH YES AH YES W.C. FRITOES HE                               NC2164.2
010500-    "            BE ALL BAD.".                                   NC2164.2
010600 01  ANS-XN-83-11 PIC X(83) VALUE                                 NC2164.2
010700     "OH YES AH                                                   NC2164.2
010800-    "                     D.".                                   NC2164.2
010900                                                                  NC2164.2
011000                                                                  NC2164.2
011100 01  WS-RIGHT-1-83.                                               NC2164.2
011200   03  WS-RIGHT-1-20                PIC X(20).                    NC2164.2
011300   03  WS-RIGHT-21-40               PIC X(20).                    NC2164.2
011400   03  WS-RIGHT-41-60               PIC X(20).                    NC2164.2
011500   03  WS-RIGHT-61-80               PIC X(20).                    NC2164.2
011600   03  WS-RIGHT-81-83               PIC X(3).                     NC2164.2
011700 01  WS-WRONG-1-83.                                               NC2164.2
011800   03  WS-WRONG-1-20                PIC X(20).                    NC2164.2
011900   03  WS-WRONG-21-40               PIC X(20).                    NC2164.2
012000   03  WS-WRONG-41-60               PIC X(20).                    NC2164.2
012100   03  WS-WRONG-61-80               PIC X(20).                    NC2164.2
012200   03  WS-WRONG-81-83               PIC X(3).                     NC2164.2
012300                                                                  NC2164.2
012400 01  INSPECT-FIELDS.                                              NC2164.2
012500   03  GRP-A.                                                     NC2164.2
012600     05  PIC X(7)  VALUE "XXXXXXX".                               NC2164.2
012700     05  PIC X(7)  VALUE "YYYYYYY".                               NC2164.2
012800     05  PIC X(7)  VALUE "AAABAAA".                               NC2164.2
012900     05  PIC X(7)  VALUE "SSSSSSS".                               NC2164.2
013000     05  PIC X(7)  VALUE "TTTTTTT".                               NC2164.2
013100   03  GRP-B       REDEFINES GRP-A.                               NC2164.2
013200     05  DATA-FIELD  PIC X(7) OCCURS 5.                           NC2164.2
013300 01  LOCATE-CHARS.                                                NC2164.2
013400   03  GRP-C.                                                     NC2164.2
013500     05  PIC X  VALUE "G".                                        NC2164.2
013600     05  PIC X  VALUE "H".                                        NC2164.2
013700     05  PIC X  VALUE "B".                                        NC2164.2
013800     05  PIC X  VALUE "D".                                        NC2164.2
013900     05  PIC X  VALUE "C".                                        NC2164.2
014000   03  GRP-D       REDEFINES GRP-C.                               NC2164.2
014100     05  END-CHAR  PIC X OCCURS 5.                                NC2164.2
014200 01  SUB PIC 9 COMP.                                              NC2164.2
014300 01  WS-BB         PIC XX VALUE "BB".                             NC2164.2
014400 01  WS-Y          PIC X  VALUE "Y".                              NC2164.2
014500 01  WS-3          PIC X  VALUE "3".                              NC2164.2
014600 01  WS-E          PIC X  VALUE "E".                              NC2164.2
014700 01  XN-DF         PIC XX VALUE "DF".                             NC2164.2
014800 01  XN-67         PIC XX VALUE "67".                             NC2164.2
014900 01  XN-B          PIC X  VALUE "B".                              NC2164.2
015000 01  TEST-31-DATA.                                                NC2164.2
015100   03  FILLER PIC X(48)  VALUE                                    NC2164.2
015200            "AABBCCDDEBBBBGHDDIJJXXAABBCCDDEEEFFGGHHIIJJKKLLM".   NC2164.2
015300 01  TEST-32-DATA.                                                NC2164.2
015400   03  FILLER PIC X(48)  VALUE                                    NC2164.2
015500            "AABSSSSSEBBTTTT1URSTSTSTVVDYYDEEEFFGSSSSTZSTZSTM".   NC2164.2
015600 01  TEST-34-DATA.                                                NC2164.2
015700   03  FILLER PIC X(20)  VALUE                                    NC2164.2
015800            "AAFSSA  ET U V W H S".                               NC2164.2
015900 01  TEST-34-ANSWER.                                              NC2164.2
016000   03  FILLER PIC X(20)  VALUE                                    NC2164.2
016100            "AAFXXA  ET Y Y Y H S".                               NC2164.2
016200 01  TEST-35-DATA.                                                NC2164.2
016300   03  FILLER PIC X(20)  VALUE                                    NC2164.2
016400            "AX SSA YEG U V W H S".                               NC2164.2
016500 01  TEST-35-ANSWER.                                              NC2164.2
016600   03  FILLER PIC X(20)  VALUE                                    NC2164.2
016700            "AX AAA YEG       H S".                               NC2164.2
016800 01  TEST-38-DATA.                                                NC2164.2
016900   03  FILLER PIC X(20)  VALUE                                    NC2164.2
017000            "AXESSA YEGTUASSW H S".                               NC2164.2
017100 01  TEST-39-DATA.                                                NC2164.2
017200   03  FILLER PIC X(20)  VALUE                                    NC2164.2
017300            "ABESSA YE TUTCGW H S".                               NC2164.2
017400 01  TEST-40-DATA.                                                NC2164.2
017500   03  FILLER PIC X(13)  VALUE                                    NC2164.2
017600            "GADQAUZTABAGA".                                      NC2164.2
017700 01  TEST-RESULTS.                                                NC2164.2
017800     02 FILLER                   PIC X      VALUE SPACE.          NC2164.2
017900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2164.2
018000     02 FILLER                   PIC X      VALUE SPACE.          NC2164.2
018100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2164.2
018200     02 FILLER                   PIC X      VALUE SPACE.          NC2164.2
018300     02  PAR-NAME.                                                NC2164.2
018400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2164.2
018500       03  PARDOT-X              PIC X      VALUE SPACE.          NC2164.2
018600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2164.2
018700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2164.2
018800     02 RE-MARK                  PIC X(61).                       NC2164.2
018900 01  TEST-COMPUTED.                                               NC2164.2
019000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2164.2
019100     02 FILLER                   PIC X(17)  VALUE                 NC2164.2
019200            "       COMPUTED=".                                   NC2164.2
019300     02 COMPUTED-X.                                               NC2164.2
019400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2164.2
019500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2164.2
019600                                 PIC -9(9).9(9).                  NC2164.2
019700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2164.2
019800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2164.2
019900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2164.2
020000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2164.2
020100         04 COMPUTED-18V0                    PIC -9(18).          NC2164.2
020200         04 FILLER                           PIC X.               NC2164.2
020300     03 FILLER PIC X(50) VALUE SPACE.                             NC2164.2
020400 01  TEST-CORRECT.                                                NC2164.2
020500     02 FILLER PIC X(30) VALUE SPACE.                             NC2164.2
020600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2164.2
020700     02 CORRECT-X.                                                NC2164.2
020800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2164.2
020900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2164.2
021000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2164.2
021100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2164.2
021200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2164.2
021300     03      CR-18V0 REDEFINES CORRECT-A.                         NC2164.2
021400         04 CORRECT-18V0                     PIC -9(18).          NC2164.2
021500         04 FILLER                           PIC X.               NC2164.2
021600     03 FILLER PIC X(2) VALUE SPACE.                              NC2164.2
021700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2164.2
021800 01  CCVS-C-1.                                                    NC2164.2
021900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2164.2
022000-    "SS  PARAGRAPH-NAME                                          NC2164.2
022100-    "       REMARKS".                                            NC2164.2
022200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2164.2
022300 01  CCVS-C-2.                                                    NC2164.2
022400     02 FILLER                     PIC X        VALUE SPACE.      NC2164.2
022500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2164.2
022600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2164.2
022700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2164.2
022800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2164.2
022900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2164.2
023000 01  REC-CT                        PIC 99       VALUE ZERO.       NC2164.2
023100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2164.2
023200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2164.2
023300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2164.2
023400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2164.2
023500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2164.2
023600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2164.2
023700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2164.2
023800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2164.2
023900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2164.2
024000 01  CCVS-H-1.                                                    NC2164.2
024100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2164.2
024200     02  FILLER                    PIC X(42)    VALUE             NC2164.2
024300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2164.2
024400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2164.2
024500 01  CCVS-H-2A.                                                   NC2164.2
024600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2164.2
024700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2164.2
024800   02  FILLER                        PIC XXXX   VALUE             NC2164.2
024900     "4.2 ".                                                      NC2164.2
025000   02  FILLER                        PIC X(28)  VALUE             NC2164.2
025100            " COPY - NOT FOR DISTRIBUTION".                       NC2164.2
025200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2164.2
025300                                                                  NC2164.2
025400 01  CCVS-H-2B.                                                   NC2164.2
025500   02  FILLER                        PIC X(15)  VALUE             NC2164.2
025600            "TEST RESULT OF ".                                    NC2164.2
025700   02  TEST-ID                       PIC X(9).                    NC2164.2
025800   02  FILLER                        PIC X(4)   VALUE             NC2164.2
025900            " IN ".                                               NC2164.2
026000   02  FILLER                        PIC X(12)  VALUE             NC2164.2
026100     " HIGH       ".                                              NC2164.2
026200   02  FILLER                        PIC X(22)  VALUE             NC2164.2
026300            " LEVEL VALIDATION FOR ".                             NC2164.2
026400   02  FILLER                        PIC X(58)  VALUE             NC2164.2
026500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2164.2
026600 01  CCVS-H-3.                                                    NC2164.2
026700     02  FILLER                      PIC X(34)  VALUE             NC2164.2
026800            " FOR OFFICIAL USE ONLY    ".                         NC2164.2
026900     02  FILLER                      PIC X(58)  VALUE             NC2164.2
027000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2164.2
027100     02  FILLER                      PIC X(28)  VALUE             NC2164.2
027200            "  COPYRIGHT   1985 ".                                NC2164.2
027300 01  CCVS-E-1.                                                    NC2164.2
027400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2164.2
027500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2164.2
027600     02 ID-AGAIN                     PIC X(9).                    NC2164.2
027700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2164.2
027800 01  CCVS-E-2.                                                    NC2164.2
027900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2164.2
028000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2164.2
028100     02 CCVS-E-2-2.                                               NC2164.2
028200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2164.2
028300         03 FILLER                   PIC X      VALUE SPACE.      NC2164.2
028400         03 ENDER-DESC               PIC X(44)  VALUE             NC2164.2
028500            "ERRORS ENCOUNTERED".                                 NC2164.2
028600 01  CCVS-E-3.                                                    NC2164.2
028700     02  FILLER                      PIC X(22)  VALUE             NC2164.2
028800            " FOR OFFICIAL USE ONLY".                             NC2164.2
028900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2164.2
029000     02  FILLER                      PIC X(58)  VALUE             NC2164.2
029100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2164.2
029200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2164.2
029300     02 FILLER                       PIC X(15)  VALUE             NC2164.2
029400             " COPYRIGHT 1985".                                   NC2164.2
029500 01  CCVS-E-4.                                                    NC2164.2
029600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2164.2
029700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2164.2
029800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2164.2
029900     02 FILLER                       PIC X(40)  VALUE             NC2164.2
030000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2164.2
030100 01  XXINFO.                                                      NC2164.2
030200     02 FILLER                       PIC X(19)  VALUE             NC2164.2
030300            "*** INFORMATION ***".                                NC2164.2
030400     02 INFO-TEXT.                                                NC2164.2
030500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2164.2
030600       04 XXCOMPUTED                 PIC X(20).                   NC2164.2
030700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2164.2
030800       04 XXCORRECT                  PIC X(20).                   NC2164.2
030900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2164.2
031000 01  HYPHEN-LINE.                                                 NC2164.2
031100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2164.2
031200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2164.2
031300-    "*****************************************".                 NC2164.2
031400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2164.2
031500-    "******************************".                            NC2164.2
031600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2164.2
031700     "NC216A".                                                    NC2164.2
031800 PROCEDURE DIVISION.                                              NC2164.2
031900 CCVS1 SECTION.                                                   NC2164.2
032000 OPEN-FILES.                                                      NC2164.2
032100     OPEN     OUTPUT PRINT-FILE.                                  NC2164.2
032200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2164.2
032300     MOVE    SPACE TO TEST-RESULTS.                               NC2164.2
032400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2164.2
032500     GO TO CCVS1-EXIT.                                            NC2164.2
032600 CLOSE-FILES.                                                     NC2164.2
032700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2164.2
032800 TERMINATE-CCVS.                                                  NC2164.2
032900*    EXIT PROGRAM.                                                NC2164.2
033000*TERMINATE-CALL.                                                  NC2164.2
033100     STOP     RUN.                                                NC2164.2
033200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2164.2
033300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2164.2
033400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2164.2
033500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2164.2
033600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2164.2
033700 PRINT-DETAIL.                                                    NC2164.2
033800     IF REC-CT NOT EQUAL TO ZERO                                  NC2164.2
033900             MOVE "." TO PARDOT-X                                 NC2164.2
034000             MOVE REC-CT TO DOTVALUE.                             NC2164.2
034100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2164.2
034200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2164.2
034300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2164.2
034400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2164.2
034500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2164.2
034600     MOVE SPACE TO CORRECT-X.                                     NC2164.2
034700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2164.2
034800     MOVE     SPACE TO RE-MARK.                                   NC2164.2
034900 HEAD-ROUTINE.                                                    NC2164.2
035000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2164.2
035100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2164.2
035200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2164.2
035300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2164.2
035400 COLUMN-NAMES-ROUTINE.                                            NC2164.2
035500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2164.2
035600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2164.2
035700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2164.2
035800 END-ROUTINE.                                                     NC2164.2
035900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2164.2
036000 END-RTN-EXIT.                                                    NC2164.2
036100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2164.2
036200 END-ROUTINE-1.                                                   NC2164.2
036300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2164.2
036400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2164.2
036500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2164.2
036600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2164.2
036700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2164.2
036800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2164.2
036900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2164.2
037000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2164.2
037100  END-ROUTINE-12.                                                 NC2164.2
037200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2164.2
037300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2164.2
037400         MOVE "NO " TO ERROR-TOTAL                                NC2164.2
037500         ELSE                                                     NC2164.2
037600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2164.2
037700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2164.2
037800     PERFORM WRITE-LINE.                                          NC2164.2
037900 END-ROUTINE-13.                                                  NC2164.2
038000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2164.2
038100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2164.2
038200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2164.2
038300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2164.2
038400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2164.2
038500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2164.2
038600          MOVE "NO " TO ERROR-TOTAL                               NC2164.2
038700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2164.2
038800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2164.2
038900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2164.2
039000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2164.2
039100 WRITE-LINE.                                                      NC2164.2
039200     ADD 1 TO RECORD-COUNT.                                       NC2164.2
039300*    IF RECORD-COUNT GREATER 50                                   NC2164.2
039400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2164.2
039500*        MOVE SPACE TO DUMMY-RECORD                               NC2164.2
039600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2164.2
039700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2164.2
039800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2164.2
039900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2164.2
040000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2164.2
040100*        MOVE ZERO TO RECORD-COUNT.                               NC2164.2
040200     PERFORM WRT-LN.                                              NC2164.2
040300 WRT-LN.                                                          NC2164.2
040400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2164.2
040500     MOVE SPACE TO DUMMY-RECORD.                                  NC2164.2
040600 BLANK-LINE-PRINT.                                                NC2164.2
040700     PERFORM WRT-LN.                                              NC2164.2
040800 FAIL-ROUTINE.                                                    NC2164.2
040900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2164.2
041000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2164.2
041100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2164.2
041200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2164.2
041300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2164.2
041400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2164.2
041500     GO TO  FAIL-ROUTINE-EX.                                      NC2164.2
041600 FAIL-ROUTINE-WRITE.                                              NC2164.2
041700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2164.2
041800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2164.2
041900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2164.2
042000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2164.2
042100 FAIL-ROUTINE-EX. EXIT.                                           NC2164.2
042200 BAIL-OUT.                                                        NC2164.2
042300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2164.2
042400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2164.2
042500 BAIL-OUT-WRITE.                                                  NC2164.2
042600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2164.2
042700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2164.2
042800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2164.2
042900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2164.2
043000 BAIL-OUT-EX. EXIT.                                               NC2164.2
043100 CCVS1-EXIT.                                                      NC2164.2
043200     EXIT.                                                        NC2164.2
043300 SECT-NC216A-001 SECTION.                                         NC2164.2
043400*                                                                 NC2164.2
043500 INS-INIT-F1-1.                                                   NC2164.2
043600     MOVE "INS-TEST-F1-1" TO PAR-NAME.                            NC2164.2
043700     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
043800     MOVE "TALLY FOR CHARACTERS" TO FEATURE.                      NC2164.2
043900     MOVE                                                         NC2164.2
044000     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
044100-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
044200         TO WC-XN-83.                                             NC2164.2
044300     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
044400 INS-TEST-F1-1.                                                   NC2164.2
044500     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR CHARACTERS.       NC2164.2
044600     IF      WRK-DU-999-1 EQUAL TO 83                             NC2164.2
044700             PERFORM PASS                                         NC2164.2
044800             GO TO   INS-WRITE-F1-1.                              NC2164.2
044900     GO TO INS-FAIL-F1-1.                                         NC2164.2
045000 INS-DELETE-F1-1.                                                 NC2164.2
045100     PERFORM DE-LETE.                                             NC2164.2
045200     GO TO INS-WRITE-F1-1.                                        NC2164.2
045300 INS-FAIL-F1-1.                                                   NC2164.2
045400     PERFORM FAIL.                                                NC2164.2
045500     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
045600     MOVE 83 TO CORRECT-N.                                        NC2164.2
045700 INS-WRITE-F1-1.                                                  NC2164.2
045800     PERFORM PRINT-DETAIL.                                        NC2164.2
045900*                                                                 NC2164.2
046000 INS-INIT-F1-2.                                                   NC2164.2
046100     MOVE "INS-TEST-F1-2" TO PAR-NAME.                            NC2164.2
046200     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
046300     MOVE "TALLY ALL LITERAL" TO FEATURE.                         NC2164.2
046400     MOVE                                                         NC2164.2
046500     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
046600-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
046700         TO WC-XN-83.                                             NC2164.2
046800     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
046900 INS-TEST-F1-2.                                                   NC2164.2
047000     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL "A".          NC2164.2
047100     IF      WRK-DU-999-1 EQUAL TO 8                              NC2164.2
047200            PERFORM PASS                                          NC2164.2
047300            GO      TO INS-WRITE-F1-2.                            NC2164.2
047400     GO TO INS-FAIL-F1-2.                                         NC2164.2
047500 INS-DELETE-F1-2.                                                 NC2164.2
047600     PERFORM DE-LETE.                                             NC2164.2
047700     GO TO INS-WRITE-F1-2.                                        NC2164.2
047800 INS-FAIL-F1-2.                                                   NC2164.2
047900     PERFORM FAIL.                                                NC2164.2
048000     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
048100     MOVE 8 TO CORRECT-N.                                         NC2164.2
048200 INS-WRITE-F1-2.                                                  NC2164.2
048300     PERFORM PRINT-DETAIL.                                        NC2164.2
048400*                                                                 NC2164.2
048500 INS-INIT-F1-3.                                                   NC2164.2
048600     MOVE "INS-TEST-F1-3" TO PAR-NAME.                            NC2164.2
048700     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
048800     MOVE "TALLY FOR ALL SPACES" TO FEATURE.                      NC2164.2
048900     MOVE                                                         NC2164.2
049000     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
049100-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
049200         TO WC-XN-83.                                             NC2164.2
049300     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
049400 INS-TEST-F1-3.                                                   NC2164.2
049500     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL SPACES.       NC2164.2
049600     IF      WRK-DU-999-1 EQUAL TO 17                             NC2164.2
049700     PERFORM PASS                                                 NC2164.2
049800     GO      TO INS-WRITE-F1-3.                                   NC2164.2
049900     GO TO INS-FAIL-F1-3.                                         NC2164.2
050000 INS-DELETE-F1-3.                                                 NC2164.2
050100     PERFORM DE-LETE.                                             NC2164.2
050200     GO TO INS-WRITE-F1-3.                                        NC2164.2
050300 INS-FAIL-F1-3.                                                   NC2164.2
050400     PERFORM FAIL.                                                NC2164.2
050500     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
050600     MOVE 17 TO CORRECT-N.                                        NC2164.2
050700 INS-WRITE-F1-3.                                                  NC2164.2
050800     PERFORM PRINT-DETAIL.                                        NC2164.2
050900*                                                                 NC2164.2
051000 INS-INIT-F1-4.                                                   NC2164.2
051100     MOVE "INS-TEST-F1-4" TO PAR-NAME.                            NC2164.2
051200     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
051300     MOVE "TALLY LEADING LIT" TO FEATURE.                         NC2164.2
051400     MOVE                                                         NC2164.2
051500     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
051600-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
051700         TO WC-XN-83.                                             NC2164.2
051800     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
051900 INS-TEST-F1-4.                                                   NC2164.2
052000     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR LEADING "AH".     NC2164.2
052100     IF      WRK-DU-999-1 EQUAL TO 1                              NC2164.2
052200     PERFORM PASS                                                 NC2164.2
052300     GO      TO INS-WRITE-F1-4.                                   NC2164.2
052400     GO TO INS-FAIL-F1-4.                                         NC2164.2
052500 INS-DELETE-F1-4.                                                 NC2164.2
052600     PERFORM DE-LETE.                                             NC2164.2
052700     GO TO INS-WRITE-F1-4.                                        NC2164.2
052800 INS-FAIL-F1-4.                                                   NC2164.2
052900     PERFORM FAIL.                                                NC2164.2
053000     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
053100     MOVE 1 TO CORRECT-N.                                         NC2164.2
053200 INS-WRITE-F1-4.                                                  NC2164.2
053300     PERFORM PRINT-DETAIL.                                        NC2164.2
053400*                                                                 NC2164.2
053500 INS-INIT-F1-5.                                                   NC2164.2
053600     MOVE "INS-TEST-F1-5" TO PAR-NAME.                            NC2164.2
053700     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
053800     MOVE "FOR CHARS AFTER LIT" TO FEATURE.                       NC2164.2
053900     MOVE                                                         NC2164.2
054000     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
054100-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
054200         TO WC-XN-83.                                             NC2164.2
054300     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
054400 INS-TEST-F1-5.                                                   NC2164.2
054500     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR CHARACTERS        NC2164.2
054600         AFTER " W".                                              NC2164.2
054700     IF      WRK-DU-999-1 EQUAL TO 68                             NC2164.2
054800     PERFORM PASS                                                 NC2164.2
054900     GO      TO INS-WRITE-F1-5.                                   NC2164.2
055000     GO TO INS-FAIL-F1-5.                                         NC2164.2
055100 INS-DELETE-F1-5.                                                 NC2164.2
055200     PERFORM DE-LETE.                                             NC2164.2
055300     GO TO INS-WRITE-F1-5.                                        NC2164.2
055400 INS-FAIL-F1-5.                                                   NC2164.2
055500     PERFORM FAIL.                                                NC2164.2
055600     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
055700     MOVE 68 TO CORRECT-N.                                        NC2164.2
055800 INS-WRITE-F1-5.                                                  NC2164.2
055900     PERFORM PRINT-DETAIL.                                        NC2164.2
056000*                                                                 NC2164.2
056100 INS-INIT-F1-6.                                                   NC2164.2
056200     MOVE "INS-TEST-F1-6" TO PAR-NAME.                            NC2164.2
056300     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
056400     MOVE "ALL BEFORE INITIAL" TO FEATURE.                        NC2164.2
056500     MOVE                                                         NC2164.2
056600     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
056700-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
056800         TO WC-XN-83.                                             NC2164.2
056900     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
057000 INS-TEST-F1-6.                                                   NC2164.2
057100     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL " "           NC2164.2
057200         BEFORE INITIAL "W.C.".                                   NC2164.2
057300     IF      WRK-DU-999-1 EQUAL TO 4                              NC2164.2
057400     PERFORM PASS                                                 NC2164.2
057500     GO      TO INS-WRITE-F1-6.                                   NC2164.2
057600     GO TO INS-FAIL-F1-6.                                         NC2164.2
057700 INS-DELETE-F1-6.                                                 NC2164.2
057800     PERFORM DE-LETE.                                             NC2164.2
057900     GO TO INS-WRITE-F1-6.                                        NC2164.2
058000 INS-FAIL-F1-6.                                                   NC2164.2
058100     PERFORM FAIL.                                                NC2164.2
058200     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
058300     MOVE 4 TO CORRECT-N.                                         NC2164.2
058400 INS-WRITE-F1-6.                                                  NC2164.2
058500     PERFORM PRINT-DETAIL.                                        NC2164.2
058600*                                                                 NC2164.2
058700 INS-INIT-F1-7.                                                   NC2164.2
058800     MOVE "INS-TEST-F1-7" TO PAR-NAME.                            NC2164.2
058900     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
059000     MOVE "LEAD LIT INITIAL FIG" TO FEATURE.                      NC2164.2
059100     MOVE                                                         NC2164.2
059200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
059300-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
059400         TO WC-XN-83.                                             NC2164.2
059500     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
059600 INS-TEST-F1-7.                                                   NC2164.2
059700     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR LEADING "Y"       NC2164.2
059800         AFTER INITIAL SPACES.                                    NC2164.2
059900     IF      WRK-DU-999-1 EQUAL TO 1                              NC2164.2
060000     PERFORM PASS                                                 NC2164.2
060100     GO      TO INS-WRITE-F1-7.                                   NC2164.2
060200     GO TO INS-FAIL-F1-7.                                         NC2164.2
060300 INS-DELETE-F1-7.                                                 NC2164.2
060400     PERFORM DE-LETE.                                             NC2164.2
060500     GO TO INS-WRITE-F1-7.                                        NC2164.2
060600 INS-FAIL-F1-7.                                                   NC2164.2
060700     PERFORM FAIL.                                                NC2164.2
060800     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
060900     MOVE 1 TO CORRECT-N.                                         NC2164.2
061000 INS-WRITE-F1-7.                                                  NC2164.2
061100     PERFORM PRINT-DETAIL.                                        NC2164.2
061200*                                                                 NC2164.2
061300 INS-INIT-F2-8.                                                   NC2164.2
061400     MOVE "INS-TEST-F2-8" TO PAR-NAME.                            NC2164.2
061500     MOVE   "VI-94 6.17.3" TO ANSI-REFERENCE.                     NC2164.2
061600     MOVE "REP CHARS BY SPACES" TO FEATURE.                       NC2164.2
061700     MOVE                                                         NC2164.2
061800     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
061900-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
062000         TO WC-XN-83.                                             NC2164.2
062100     MOVE    WC-XN-83 TO WRK-XN-83-1.                             NC2164.2
062200     MOVE                                                         NC2164.2
062300     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
062400-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
062500         TO ANS-XN-83-5.                                          NC2164.2
062600 INS-TEST-F2-8.                                                   NC2164.2
062700     INSPECT WRK-XN-83-1 REPLACING CHARACTERS BY SPACES.          NC2164.2
062800     IF      WRK-XN-83-1 EQUAL TO SPACES                          NC2164.2
062900             PERFORM PASS                                         NC2164.2
063000             GO      TO INS-WRITE-F2-8.                           NC2164.2
063100     GO TO INS-FAIL-F2-8.                                         NC2164.2
063200 INS-DELETE-F2-8.                                                 NC2164.2
063300     PERFORM DE-LETE.                                             NC2164.2
063400     GO TO INS-WRITE-F2-8.                                        NC2164.2
063500 INS-FAIL-F2-8.                                                   NC2164.2
063600     PERFORM FAIL.                                                NC2164.2
063700     MOVE    "83 SPACES" TO RE-MARK.                              NC2164.2
063800     MOVE    WRK-XN-83-1 TO WS-WRONG-1-83.                        NC2164.2
063900     MOVE    SPACES      TO WS-RIGHT-1-83.                        NC2164.2
064000     PERFORM FAIL.                                                NC2164.2
064100     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83.                    NC2164.2
064200     MOVE    ANS-XN-83-5     TO WS-RIGHT-1-83.                    NC2164.2
064300     MOVE    WS-RIGHT-1-20   TO CORRECT-A.                        NC2164.2
064400     MOVE    WS-WRONG-1-20   TO COMPUTED-A.                       NC2164.2
064500     PERFORM PRINT-DETAIL.                                        NC2164.2
064600     MOVE    WS-RIGHT-21-40  TO CORRECT-A.                        NC2164.2
064700     MOVE    WS-WRONG-21-40  TO COMPUTED-A.                       NC2164.2
064800     PERFORM PRINT-DETAIL.                                        NC2164.2
064900     MOVE    WS-RIGHT-41-60  TO CORRECT-A.                        NC2164.2
065000     MOVE    WS-WRONG-41-60  TO COMPUTED-A.                       NC2164.2
065100     PERFORM PRINT-DETAIL.                                        NC2164.2
065200     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
065300     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
065400     PERFORM PRINT-DETAIL.                                        NC2164.2
065500     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
065600     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
065700 INS-WRITE-F2-8.                                                  NC2164.2
065800     PERFORM PRINT-DETAIL.                                        NC2164.2
065900*                                                                 NC2164.2
066000 INS-INIT-F2-9.                                                   NC2164.2
066100     MOVE "INS-TEST-F2-9" TO PAR-NAME.                            NC2164.2
066200     MOVE   "VI-94 6.17.3" TO ANSI-REFERENCE.                     NC2164.2
066300     MOVE "CHARS BEFORE INITIAL" TO FEATURE.                      NC2164.2
066400     MOVE                                                         NC2164.2
066500     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
066600-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
066700         TO WC-XN-83.                                             NC2164.2
066800     MOVE                                                         NC2164.2
066900     "OH YES AH YES W.C. FROTOES HERE, ANYONE WHO HATES DOGS AND KNC2164.2
067000-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
067100         TO ANS-XN-83-9.                                          NC2164.2
067200     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
067300     MOVE    1 TO REC-CT.                                         NC2164.2
067400 INS-TEST-F2-9.                                                   NC2164.2
067500     INSPECT WRK-XN-83-1                                          NC2164.2
067600         REPLACING LEADING "AH" BY "OH" BEFORE INITIAL " AH YES"  NC2164.2
067700                   FIRST "I" BY "O" AFTER INITIAL "."             NC2164.2
067800                   ALL ". " BY ", " AFTER INITIAL "HE".           NC2164.2
067900     MOVE    WRK-XN-83-1 TO WRK-XN-83-2.                          NC2164.2
068000     INSPECT WRK-XN-83-1                                          NC2164.2
068100         REPLACING ALL "OT" BY "IT" BEFORE "HE"                   NC2164.2
068200                   LEADING ", " BY ". " AFTER "RE"                NC2164.2
068300                   FIRST "KIDS CAN NOT BE" BY  KIDS-CAN-NOT-BE    NC2164.2
068400                   ALL BLANK-PERIOD BY " ." AFTER "BAD".          NC2164.2
068500     GO      TO INS-TEST-F2-9-1.                                  NC2164.2
068600 INS-DELETE-F2-9.                                                 NC2164.2
068700     PERFORM DE-LETE.                                             NC2164.2
068800     PERFORM PRINT-DETAIL.                                        NC2164.2
068900     GO      TO INS-TEST-F2-10.                                   NC2164.2
069000*                                                                 NC2164.2
069100 INS-TEST-F2-9-1.                                                 NC2164.2
069200     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC2164.2
069300             PERFORM PASS                                         NC2164.2
069400             GO TO INS-WRITE-F2-9-1                               NC2164.2
069500     ELSE                                                         NC2164.2
069600             GO TO INS-FAIL-F2-9-1.                               NC2164.2
069700 INS-DELETE-F2-9-1.                                               NC2164.2
069800     PERFORM DE-LETE.                                             NC2164.2
069900     GO TO INS-WRITE-F2-9-1.                                      NC2164.2
070000 INS-FAIL-F2-9-1.                                                 NC2164.2
070100     PERFORM FAIL                                                 NC2164.2
070200     MOVE    WRK-XN-83-1 TO COMPUTED-A                            NC2164.2
070300     MOVE    ANS-XN-83-1 TO CORRECT-A.                            NC2164.2
070400 INS-WRITE-F2-9-1.                                                NC2164.2
070500     PERFORM PRINT-DETAIL.                                        NC2164.2
070600*                                                                 NC2164.2
070700 INS-TEST-F2-9-2.                                                 NC2164.2
070800     ADD     1 TO REC-CT.                                         NC2164.2
070900     IF      WRK-XN-83-2 EQUAL TO ANS-XN-83-9                     NC2164.2
071000             PERFORM PASS                                         NC2164.2
071100             GO TO INS-WRITE-F2-9-2                               NC2164.2
071200     ELSE                                                         NC2164.2
071300             GO TO INS-FAIL-F2-9-2.                               NC2164.2
071400 INS-FAIL-F2-9-2.                                                 NC2164.2
071500     PERFORM FAIL                                                 NC2164.2
071600     MOVE    WRK-XN-83-2     TO WS-WRONG-1-83                     NC2164.2
071700     MOVE    ANS-XN-83-9     TO WS-RIGHT-1-83                     NC2164.2
071800     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
071900     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
072000     PERFORM PRINT-DETAIL                                         NC2164.2
072100     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
072200     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
072300     PERFORM PRINT-DETAIL                                         NC2164.2
072400     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
072500     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
072600     PERFORM PRINT-DETAIL                                         NC2164.2
072700     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
072800     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
072900     PERFORM PRINT-DETAIL                                         NC2164.2
073000     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
073100     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
073200 INS-WRITE-F2-9-2.                                                NC2164.2
073300     PERFORM PRINT-DETAIL.                                        NC2164.2
073400*                                                                 NC2164.2
073500 INS-INIT-F2-10.                                                  NC2164.2
073600     MOVE "INS-TEST-F2-10" TO PAR-NAME.                           NC2164.2
073700     MOVE   "VI-94 6.17.3" TO ANSI-REFERENCE.                     NC2164.2
073800     MOVE "LEAD AFTER INIT ID" TO FEATURE.                        NC2164.2
073900     MOVE    ZERO TO REC-CT.                                      NC2164.2
074000     MOVE                                                         NC2164.2
074100     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
074200-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
074300         TO WC-XN-83.                                             NC2164.2
074400     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
074500 INS-TEST-F2-10.                                                  NC2164.2
074600     INSPECT WRK-XN-83-1 REPLACING LEADING SPACE-XN-1-1           NC2164.2
074700         BY COMMA-XN-1-1 AFTER INITIAL YES-XN-3.                  NC2164.2
074800     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-2                     NC2164.2
074900     PERFORM PASS                                                 NC2164.2
075000     GO      TO INS-WRITE-F2-10.                                  NC2164.2
075100     GO TO INS-FAIL-F2-10.                                        NC2164.2
075200 INS-DELETE-F2-10.                                                NC2164.2
075300     PERFORM DE-LETE.                                             NC2164.2
075400     GO TO INS-WRITE-F2-10.                                       NC2164.2
075500 INS-FAIL-F2-10.                                                  NC2164.2
075600     PERFORM FAIL.                                                NC2164.2
075700     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
075800     MOVE    ANS-XN-83-2     TO WS-RIGHT-1-83                     NC2164.2
075900     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
076000     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
076100     PERFORM PRINT-DETAIL                                         NC2164.2
076200     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
076300     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
076400     PERFORM PRINT-DETAIL                                         NC2164.2
076500     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
076600     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
076700     PERFORM PRINT-DETAIL                                         NC2164.2
076800     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
076900     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
077000     PERFORM PRINT-DETAIL                                         NC2164.2
077100     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
077200     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
077300 INS-WRITE-F2-10.                                                 NC2164.2
077400     PERFORM PRINT-DETAIL.                                        NC2164.2
077500*                                                                 NC2164.2
077600 INS-INIT-F2-11.                                                  NC2164.2
077700     MOVE "INS-TEST-F2-11" TO PAR-NAME.                           NC2164.2
077800     MOVE   "VI-94 6.17.3" TO ANSI-REFERENCE.                     NC2164.2
077900     MOVE "FIRST BY ID BEFORE" TO FEATURE.                        NC2164.2
078000     MOVE                                                         NC2164.2
078100     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
078200-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
078300         TO WC-XN-83.                                             NC2164.2
078400     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
078500     MOVE "O" TO O-XN-1-1.                                        NC2164.2
078600 INS-TEST-F2-11.                                                  NC2164.2
078700     INSPECT WRK-XN-83-1 REPLACING FIRST "A" BY O-XN-1-1          NC2164.2
078800         BEFORE INITIAL "H YES".                                  NC2164.2
078900     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC2164.2
079000     PERFORM PASS                                                 NC2164.2
079100     GO      TO INS-WRITE-F2-11.                                  NC2164.2
079200     GO TO INS-FAIL-F2-11.                                        NC2164.2
079300 INS-DELETE-F2-11.                                                NC2164.2
079400     PERFORM DE-LETE.                                             NC2164.2
079500     GO TO INS-WRITE-F2-11.                                       NC2164.2
079600 INS-FAIL-F2-11.                                                  NC2164.2
079700     PERFORM FAIL.                                                NC2164.2
079800     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
079900     MOVE    ANS-XN-83-1     TO WS-RIGHT-1-83                     NC2164.2
080000     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
080100     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
080200     PERFORM PRINT-DETAIL                                         NC2164.2
080300     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
080400     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
080500     PERFORM PRINT-DETAIL                                         NC2164.2
080600     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
080700     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
080800     PERFORM PRINT-DETAIL                                         NC2164.2
080900     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
081000     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
081100     PERFORM PRINT-DETAIL                                         NC2164.2
081200     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
081300     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
081400 INS-WRITE-F2-11.                                                 NC2164.2
081500     PERFORM PRINT-DETAIL.                                        NC2164.2
081600*                                                                 NC2164.2
081700 INS-INIT-F2-12.                                                  NC2164.2
081800     MOVE "INS-TEST-F2-12" TO PAR-NAME.                           NC2164.2
081900     MOVE   "VI-94 6.17.3" TO ANSI-REFERENCE.                     NC2164.2
082000     MOVE "ALL ID BY LIT AFTER" TO FEATURE.                       NC2164.2
082100     MOVE                                                         NC2164.2
082200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
082300-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
082400         TO WC-XN-83.                                             NC2164.2
082500     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
082600 INS-TEST-F2-12.                                                  NC2164.2
082700     INSPECT WRK-XN-83-1 REPLACING ALL SPACE-XN-1-1 BY "-"        NC2164.2
082800         AFTER ALL-XN-3.                                          NC2164.2
082900     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-3                     NC2164.2
083000     PERFORM PASS                                                 NC2164.2
083100     GO      TO INS-WRITE-F2-12.                                  NC2164.2
083200     GO TO INS-FAIL-F2-12.                                        NC2164.2
083300 INS-DELETE-F2-12.                                                NC2164.2
083400     PERFORM DE-LETE.                                             NC2164.2
083500     GO TO INS-WRITE-F2-12.                                       NC2164.2
083600 INS-FAIL-F2-12.                                                  NC2164.2
083700     PERFORM FAIL.                                                NC2164.2
083800     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
083900     MOVE    ANS-XN-83-3     TO WS-RIGHT-1-83                     NC2164.2
084000     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
084100     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
084200     PERFORM PRINT-DETAIL                                         NC2164.2
084300     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
084400     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
084500     PERFORM PRINT-DETAIL                                         NC2164.2
084600     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
084700     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
084800     PERFORM PRINT-DETAIL                                         NC2164.2
084900     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
085000     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
085100     PERFORM PRINT-DETAIL                                         NC2164.2
085200     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
085300     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
085400 INS-WRITE-F2-12.                                                 NC2164.2
085500     PERFORM PRINT-DETAIL.                                        NC2164.2
085600*                                                                 NC2164.2
085700 INS-INIT-F3-13.                                                  NC2164.2
085800     MOVE "INS-TEST-F3-13" TO PAR-NAME.                           NC2164.2
085900     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
086000     MOVE "TALLY-REPLACE CHARS" TO FEATURE.                       NC2164.2
086100     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
086200     MOVE                                                         NC2164.2
086300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
086400-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
086500         TO WC-XN-83.                                             NC2164.2
086600     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
086700     MOVE    1 TO REC-CT.                                         NC2164.2
086800 INS-TEST-F3-13-0.                                                NC2164.2
086900     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC2164.2
087000         REPLACING CHARACTERS BY SPACES.                          NC2164.2
087100     GO      TO INS-TEST-F3-13-1.                                 NC2164.2
087200 INS-DELETE-F3-13.                                                NC2164.2
087300     PERFORM DE-LETE.                                             NC2164.2
087400     PERFORM PRINT-DETAIL.                                        NC2164.2
087500     GO TO INS-INIT-F3-14.                                        NC2164.2
087600*                                                                 NC2164.2
087700 INS-TEST-F3-13-1.                                                NC2164.2
087800     IF      WRK-DU-999-1 EQUAL TO 83                             NC2164.2
087900             PERFORM PASS                                         NC2164.2
088000             GO TO INS-WRITE-F3-13-1                              NC2164.2
088100     ELSE                                                         NC2164.2
088200             GO TO INS-FAIL-F3-13-1.                              NC2164.2
088300 INS-DELETE-F3-13-1.                                              NC2164.2
088400     PERFORM DE-LETE.                                             NC2164.2
088500     GO TO INS-WRITE-F3-13-1.                                     NC2164.2
088600 INS-FAIL-F3-13-1.                                                NC2164.2
088700     PERFORM FAIL                                                 NC2164.2
088800     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
088900     MOVE    83 TO CORRECT-N.                                     NC2164.2
089000 INS-WRITE-F3-13-1.                                               NC2164.2
089100     PERFORM PRINT-DETAIL.                                        NC2164.2
089200*                                                                 NC2164.2
089300 TEST-13-2.                                                       NC2164.2
089400     ADD     1 TO REC-CT.                                         NC2164.2
089500     IF      WRK-XN-83-1 EQUAL TO SPACES                          NC2164.2
089600             PERFORM PASS                                         NC2164.2
089700             GO TO INS-WRITE-F3-13-2                              NC2164.2
089800     ELSE                                                         NC2164.2
089900             GO TO INS-FAIL-F3-13-2.                              NC2164.2
090000 INS-DELETE-F3-13-2.                                              NC2164.2
090100     PERFORM DE-LETE.                                             NC2164.2
090200     GO TO INS-WRITE-F3-13-2.                                     NC2164.2
090300 INS-FAIL-F3-13-2.                                                NC2164.2
090400     PERFORM FAIL                                                 NC2164.2
090500     MOVE    WRK-XN-83-1 TO COMPUTED-A                            NC2164.2
090600     MOVE    "83 SPACES" TO CORRECT-A.                            NC2164.2
090700 INS-WRITE-F3-13-2.                                               NC2164.2
090800     PERFORM PRINT-DETAIL.                                        NC2164.2
090900*                                                                 NC2164.2
091000 INS-INIT-F3-14.                                                  NC2164.2
091100     MOVE "INS-TEST-F3-14" TO PAR-NAME.                           NC2164.2
091200     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
091300     MOVE "LIT BY BEFORE INIT" TO FEATURE.                        NC2164.2
091400     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
091500     MOVE                                                         NC2164.2
091600     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
091700-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
091800         TO WC-XN-83.                                             NC2164.2
091900     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
092000     MOVE    1 TO REC-CT.                                         NC2164.2
092100 INS-TEST-F3-14-0.                                                NC2164.2
092200     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC2164.2
092300         AFTER L-XN-1-1 REPLACING ALL "A" BY "E" BEFORE INITIAL   NC2164.2
092400     HSPACE-XN-2.                                                 NC2164.2
092500     GO TO INS-TEST-F3-14-1.                                      NC2164.2
092600 INS-DELETE-F3-14.                                                NC2164.2
092700     PERFORM DE-LETE.                                             NC2164.2
092800     PERFORM PRINT-DETAIL.                                        NC2164.2
092900     GO TO INS-INIT-F3-15.                                        NC2164.2
093000*                                                                 NC2164.2
093100 INS-TEST-F3-14-1.                                                NC2164.2
093200     IF      WRK-DU-999-1 EQUAL TO 6                              NC2164.2
093300     PERFORM PASS                                                 NC2164.2
093400             GO TO INS-WRITE-F3-14-1                              NC2164.2
093500     ELSE                                                         NC2164.2
093600             PERFORM FAIL                                         NC2164.2
093700             GO TO INS-FAIL-F3-14-1.                              NC2164.2
093800 INS-DELETE-F3-14-1.                                              NC2164.2
093900     PERFORM DE-LETE.                                             NC2164.2
094000     GO TO INS-WRITE-F3-14-1.                                     NC2164.2
094100 INS-FAIL-F3-14-1.                                                NC2164.2
094200     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
094300     MOVE    6 TO CORRECT-N.                                      NC2164.2
094400 INS-WRITE-F3-14-1.                                               NC2164.2
094500     PERFORM PRINT-DETAIL.                                        NC2164.2
094600*                                                                 NC2164.2
094700 INS-TEST-F3-14-2.                                                NC2164.2
094800     ADD     1 TO REC-CT.                                         NC2164.2
094900     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-4                     NC2164.2
095000             PERFORM PASS                                         NC2164.2
095100             GO TO INS-WRITE-F3-14-2                              NC2164.2
095200     ELSE                                                         NC2164.2
095300             PERFORM FAIL                                         NC2164.2
095400             GO TO INS-FAIL-F3-14-2.                              NC2164.2
095500 INS-DELETE-F3-14-2.                                              NC2164.2
095600     PERFORM DE-LETE.                                             NC2164.2
095700     GO TO INS-WRITE-F3-14-2.                                     NC2164.2
095800 INS-FAIL-F3-14-2.                                                NC2164.2
095900     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
096000     MOVE    ANS-XN-83-4     TO WS-RIGHT-1-83                     NC2164.2
096100     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
096200     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
096300     PERFORM PRINT-DETAIL                                         NC2164.2
096400     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
096500     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
096600     PERFORM PRINT-DETAIL                                         NC2164.2
096700     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
096800     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
096900     PERFORM PRINT-DETAIL                                         NC2164.2
097000     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
097100     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
097200     PERFORM PRINT-DETAIL                                         NC2164.2
097300     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
097400     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
097500 INS-WRITE-F3-14-2.                                               NC2164.2
097600     PERFORM PRINT-DETAIL.                                        NC2164.2
097700*                                                                 NC2164.2
097800 INS-INIT-F3-15.                                                  NC2164.2
097900     MOVE "INS-TEST-F3-15" TO PAR-NAME.                           NC2164.2
098000     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
098100     MOVE "REPL FIRST AFTER" TO FEATURE.                          NC2164.2
098200     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
098300     MOVE                                                         NC2164.2
098400     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
098500-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
098600         TO WC-XN-83.                                             NC2164.2
098700     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
098800     MOVE                                                         NC2164.2
098900     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
099000-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
099100         TO ANS-XN-83-5.                                          NC2164.2
099200     MOVE    1 TO REC-CT.                                         NC2164.2
099300 INS-TEST-F3-15-0.                                                NC2164.2
099400     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR ALL "A" BEFORE NC2164.2
099500         L-XN-1-1 REPLACING FIRST  AH-XN-2 BY "OH" AFTER          NC2164.2
099600         INITIAL HSPACE-XN-2.                                     NC2164.2
099700     GO      TO INS-TEST-F3-15-1.                                 NC2164.2
099800 INS-DELETE-F3-15.                                                NC2164.2
099900     PERFORM DE-LETE.                                             NC2164.2
100000     PERFORM PRINT-DETAIL.                                        NC2164.2
100100     GO TO INS-INIT-F3-16.                                        NC2164.2
100200*                                                                 NC2164.2
100300 INS-TEST-F3-15-1.                                                NC2164.2
100400     IF      WRK-DU-999-1 EQUAL TO 7                              NC2164.2
100500             PERFORM PASS                                         NC2164.2
100600             GO TO INS-WRITE-F3-15-1                              NC2164.2
100700     ELSE                                                         NC2164.2
100800             PERFORM FAIL                                         NC2164.2
100900             GO TO INS-FAIL-F3-15-1.                              NC2164.2
101000 INS-DELETE-F3-15-1.                                              NC2164.2
101100     PERFORM DE-LETE.                                             NC2164.2
101200     GO TO INS-WRITE-F3-15-1.                                     NC2164.2
101300 INS-FAIL-F3-15-1.                                                NC2164.2
101400     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
101500     MOVE    7 TO CORRECT-N.                                      NC2164.2
101600 INS-WRITE-F3-15-1.                                               NC2164.2
101700     PERFORM PRINT-DETAIL.                                        NC2164.2
101800*                                                                 NC2164.2
101900 INS-TEST-F3-15-2.                                                NC2164.2
102000     ADD     1 TO REC-CT.                                         NC2164.2
102100     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-5                     NC2164.2
102200             PERFORM PASS                                         NC2164.2
102300             GO TO INS-WRITE-F3-15-2                              NC2164.2
102400     ELSE                                                         NC2164.2
102500             PERFORM FAIL                                         NC2164.2
102600             GO TO INS-FAIL-F3-15-2.                              NC2164.2
102700 INS-DELETE-F3-15-2.                                              NC2164.2
102800     PERFORM DE-LETE.                                             NC2164.2
102900     GO TO INS-WRITE-F3-15-2.                                     NC2164.2
103000 INS-FAIL-F3-15-2.                                                NC2164.2
103100     MOVE    WRK-XN-83-1 TO COMPUTED-A                            NC2164.2
103200     MOVE    ANS-XN-83-5 TO CORRECT-A.                            NC2164.2
103300 INS-WRITE-F3-15-2.                                               NC2164.2
103400     PERFORM PRINT-DETAIL.                                        NC2164.2
103500*                                                                 NC2164.2
103600 INS-INIT-F3-16.                                                  NC2164.2
103700     MOVE "INS-TEST-F3-16" TO PAR-NAME.                           NC2164.2
103800     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
103900     MOVE "FOR LEADING" TO FEATURE.                               NC2164.2
104000     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
104100     MOVE                                                         NC2164.2
104200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
104300-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
104400         TO WC-XN-83.                                             NC2164.2
104500     MOVE                                                         NC2164.2
104600     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
104700-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
104800         TO ANS-XN-83-5.                                          NC2164.2
104900     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
105000     MOVE    1 TO REC-CT.                                         NC2164.2
105100 INS-TEST-F3-16-0.                                                NC2164.2
105200     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR LEADING        NC2164.2
105300         AH-XN-2 REPLACING LEADING AH-XN-2 BY "OH".               NC2164.2
105400     GO      TO INS-TEST-F3-16-1.                                 NC2164.2
105500 INS-DELETE-F3-16.                                                NC2164.2
105600     PERFORM DE-LETE.                                             NC2164.2
105700     PERFORM PRINT-DETAIL.                                        NC2164.2
105800     GO TO INS-INIT-F3-17.                                        NC2164.2
105900*                                                                 NC2164.2
106000 INS-TEST-F3-16-1.                                                NC2164.2
106100     IF      WRK-DU-999-1 EQUAL TO 1                              NC2164.2
106200             PERFORM PASS                                         NC2164.2
106300             GO TO INS-WRITE-F3-16-1                              NC2164.2
106400     ELSE                                                         NC2164.2
106500             PERFORM FAIL                                         NC2164.2
106600             GO TO INS-FAIL-F3-16-1.                              NC2164.2
106700 INS-DELETE-F3-16-1.                                              NC2164.2
106800     PERFORM DE-LETE.                                             NC2164.2
106900     GO TO INS-WRITE-F3-16-1.                                     NC2164.2
107000 INS-FAIL-F3-16-1.                                                NC2164.2
107100     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
107200     MOVE    1 TO CORRECT-N.                                      NC2164.2
107300 INS-WRITE-F3-16-1.                                               NC2164.2
107400     PERFORM PRINT-DETAIL.                                        NC2164.2
107500*                                                                 NC2164.2
107600 INS-TEST-F3-16-2.                                                NC2164.2
107700     ADD     1 TO REC-CT.                                         NC2164.2
107800     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC2164.2
107900             PERFORM PASS                                         NC2164.2
108000             GO TO INS-WRITE-F3-16-2                              NC2164.2
108100     ELSE                                                         NC2164.2
108200             PERFORM FAIL                                         NC2164.2
108300             GO TO INS-FAIL-F3-16-2.                              NC2164.2
108400 INS-DELETE-F3-16-2.                                              NC2164.2
108500     PERFORM DE-LETE.                                             NC2164.2
108600     GO TO   INS-WRITE-F3-16-2.                                   NC2164.2
108700 INS-FAIL-F3-16-2.                                                NC2164.2
108800     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
108900     MOVE    ANS-XN-83-1     TO WS-RIGHT-1-83                     NC2164.2
109000     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
109100     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
109200     PERFORM PRINT-DETAIL                                         NC2164.2
109300     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
109400     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
109500     PERFORM PRINT-DETAIL                                         NC2164.2
109600     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
109700     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
109800     PERFORM PRINT-DETAIL                                         NC2164.2
109900     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
110000     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
110100     PERFORM PRINT-DETAIL                                         NC2164.2
110200     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
110300     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
110400 INS-WRITE-F3-16-2.                                               NC2164.2
110500     PERFORM PRINT-DETAIL.                                        NC2164.2
110600*                                                                 NC2164.2
110700 INS-INIT-F3-17.                                                  NC2164.2
110800     MOVE "INS-TEST-F3-17" TO PAR-NAME.                           NC2164.2
110900     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
111000     MOVE "LIT BY AFTER INIT" TO FEATURE.                         NC2164.2
111100     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
111200     MOVE                                                         NC2164.2
111300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
111400-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
111500         TO WC-XN-83.                                             NC2164.2
111600     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
111700     MOVE                                                         NC2164.2
111800     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
111900-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
112000         TO ANS-XN-83-5.                                          NC2164.2
112100     MOVE    1 TO REC-CT.                                         NC2164.2
112200 INS-TEST-F3-17-0.                                                NC2164.2
112300     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR ALL "A"        NC2164.2
112400         REPLACING FIRST "AH" BY "OH" AFTER INITIAL "YES".        NC2164.2
112500     GO      TO INS-TEST-F3-17-1.                                 NC2164.2
112600 INS-DELETE-F3-17.                                                NC2164.2
112700     PERFORM DE-LETE.                                             NC2164.2
112800     PERFORM PRINT-DETAIL.                                        NC2164.2
112900     GO TO INS-INIT-F3-18.                                        NC2164.2
113000*                                                                 NC2164.2
113100 INS-TEST-F3-17-1.                                                NC2164.2
113200     IF      WRK-DU-999-1 EQUAL TO 8                              NC2164.2
113300             PERFORM PASS                                         NC2164.2
113400             GO TO INS-WRITE-F3-17-1                              NC2164.2
113500     ELSE                                                         NC2164.2
113600             PERFORM FAIL                                         NC2164.2
113700             GO TO INS-FAIL-F3-17-1.                              NC2164.2
113800 INS-DELETE-F3-17-1.                                              NC2164.2
113900     PERFORM DE-LETE.                                             NC2164.2
114000     GO TO INS-WRITE-F3-17-1.                                     NC2164.2
114100 INS-FAIL-F3-17-1.                                                NC2164.2
114200     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
114300     MOVE    8 TO CORRECT-N.                                      NC2164.2
114400 INS-WRITE-F3-17-1.                                               NC2164.2
114500     PERFORM PRINT-DETAIL.                                        NC2164.2
114600*                                                                 NC2164.2
114700 INS-TEST-F3-17-2.                                                NC2164.2
114800     ADD     1 TO REC-CT.                                         NC2164.2
114900     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-5                     NC2164.2
115000             PERFORM PASS                                         NC2164.2
115100             GO TO INS-WRITE-F3-17-2                              NC2164.2
115200     ELSE                                                         NC2164.2
115300             PERFORM FAIL                                         NC2164.2
115400             GO TO INS-FAIL-F3-17-2.                              NC2164.2
115500 INS-DELETE-F3-17-2.                                              NC2164.2
115600     PERFORM DE-LETE.                                             NC2164.2
115700     GO TO INS-WRITE-F3-17-2.                                     NC2164.2
115800 INS-FAIL-F3-17-2.                                                NC2164.2
115900     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
116000     MOVE    ANS-XN-83-5     TO WS-RIGHT-1-83                     NC2164.2
116100     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
116200     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
116300     PERFORM PRINT-DETAIL                                         NC2164.2
116400     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
116500     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
116600     PERFORM PRINT-DETAIL                                         NC2164.2
116700     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
116800     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
116900     PERFORM PRINT-DETAIL                                         NC2164.2
117000     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
117100     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
117200     PERFORM PRINT-DETAIL                                         NC2164.2
117300     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
117400     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
117500 INS-WRITE-F3-17-2.                                               NC2164.2
117600     PERFORM PRINT-DETAIL.                                        NC2164.2
117700*                                                                 NC2164.2
117800 INS-INIT-F3-18.                                                  NC2164.2
117900     MOVE "INS-TEST-F3-18" TO PAR-NAME.                           NC2164.2
118000     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
118100     MOVE "CHAR AFTER ALL BEF" TO FEATURE.                        NC2164.2
118200     MOVE ZERO TO WRK-DU-999-1.                                   NC2164.2
118300     MOVE                                                         NC2164.2
118400     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
118500-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
118600         TO WC-XN-83.                                             NC2164.2
118700     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
118800     MOVE 1 TO REC-CT.                                            NC2164.2
118900 INS-TEST-F3-18-0.                                                NC2164.2
119000     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC2164.2
119100         AFTER AH-XN-2 REPLACING ALL "AH" BY "OH" BEFORE YES-XN-3.NC2164.2
119200     GO      TO INS-TEST-F3-18-1.                                 NC2164.2
119300 INS-DELETE-F3-18.                                                NC2164.2
119400     PERFORM DE-LETE.                                             NC2164.2
119500     PERFORM PRINT-DETAIL.                                        NC2164.2
119600     GO      TO INS-INIT-F3-19.                                   NC2164.2
119700*                                                                 NC2164.2
119800 INS-TEST-F3-18-1.                                                NC2164.2
119900     IF      WRK-DU-999-1 EQUAL TO 81                             NC2164.2
120000             PERFORM PASS                                         NC2164.2
120100             GO TO INS-WRITE-F3-18-1                              NC2164.2
120200     ELSE                                                         NC2164.2
120300             PERFORM FAIL                                         NC2164.2
120400             GO TO INS-FAIL-F3-18-1.                              NC2164.2
120500 INS-DELETE-F3-18-1.                                              NC2164.2
120600     PERFORM DE-LETE.                                             NC2164.2
120700     GO TO INS-WRITE-F3-18-1.                                     NC2164.2
120800 INS-FAIL-F3-18-1.                                                NC2164.2
120900     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
121000     MOVE    81 TO CORRECT-N.                                     NC2164.2
121100 INS-WRITE-F3-18-1.                                               NC2164.2
121200     PERFORM PRINT-DETAIL.                                        NC2164.2
121300*                                                                 NC2164.2
121400 INS-TEST-F3-18-2.                                                NC2164.2
121500     ADD     1 TO REC-CT.                                         NC2164.2
121600     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC2164.2
121700             PERFORM PASS                                         NC2164.2
121800             GO TO INS-WRITE-F3-18-2                              NC2164.2
121900     ELSE                                                         NC2164.2
122000             PERFORM FAIL                                         NC2164.2
122100             GO TO INS-FAIL-F3-18-2.                              NC2164.2
122200 INS-DELETE-F3-18-2.                                              NC2164.2
122300     PERFORM DE-LETE.                                             NC2164.2
122400     GO TO INS-WRITE-F3-18-2.                                     NC2164.2
122500 INS-FAIL-F3-18-2.                                                NC2164.2
122600     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
122700     MOVE    ANS-XN-83-1     TO WS-RIGHT-1-83                     NC2164.2
122800     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
122900     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
123000     PERFORM PRINT-DETAIL                                         NC2164.2
123100     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
123200     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
123300     PERFORM PRINT-DETAIL                                         NC2164.2
123400     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
123500     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
123600     PERFORM PRINT-DETAIL                                         NC2164.2
123700     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
123800     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
123900     PERFORM PRINT-DETAIL                                         NC2164.2
124000     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
124100     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
124200 INS-WRITE-F3-18-2.                                               NC2164.2
124300     PERFORM PRINT-DETAIL.                                        NC2164.2
124400*                                                                 NC2164.2
124500 INS-INIT-F3-19.                                                  NC2164.2
124600     MOVE "INS-TEST-F3-19" TO PAR-NAME.                           NC2164.2
124700     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
124800     MOVE "TALLY SERIES" TO FEATURE.                              NC2164.2
124900     MOVE ZERO TO WRK-DU-999-1 WRK-DU-999-2 WRK-DU-999-3          NC2164.2
125000         WRK-DU-999-4.                                            NC2164.2
125100     MOVE                                                         NC2164.2
125200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
125300-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
125400         TO WC-XN-83.                                             NC2164.2
125500     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
125600     MOVE 1 TO REC-CT.                                            NC2164.2
125700     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR ALL "A"        NC2164.2
125800             WRK-DU-999-2 FOR LEADING "AH"                        NC2164.2
125900             WRK-DU-999-3 FOR CHARACTERS BEFORE "."               NC2164.2
126000             WRK-DU-999-4 FOR CHARACTERS AFTER "AL"               NC2164.2
126100             REPLACING                                            NC2164.2
126200                 FIRST "L " BY "ZZ" AFTER INITIAL "AL"            NC2164.2
126300                 FIRST "BAD" BY "ZZZ" AFTER "L "                  NC2164.2
126400                 LEADING "BAD" BY "ZZZ" BEFORE INITIAL "Q"        NC2164.2
126500                 FIRST "BAD" BY "ZZZ" BEFORE INITIAL "Z"          NC2164.2
126600                 FIRST "BAD" BY "ZZZ" AFTER "ALL "                NC2164.2
126700                 ALL "." BY "Z" AFTER "AL".                       NC2164.2
126800     GO TO INS-TEST-F3-19-1.                                      NC2164.2
126900 INS-DELETE-F3-19.                                                NC2164.2
127000     PERFORM DE-LETE.                                             NC2164.2
127100     PERFORM PRINT-DETAIL.                                        NC2164.2
127200     GO TO INS-INIT-F3-20.                                        NC2164.2
127300*                                                                 NC2164.2
127400 INS-TEST-F3-19-1.                                                NC2164.2
127500     IF WRK-DU-999-1 = 8                                          NC2164.2
127600             PERFORM PASS                                         NC2164.2
127700             GO TO INS-WRITE-F3-19-1                              NC2164.2
127800     ELSE                                                         NC2164.2
127900             GO TO INS-FAIL-F3-19-1.                              NC2164.2
128000 INS-DELETE-F3-19-1.                                              NC2164.2
128100     PERFORM DE-LETE.                                             NC2164.2
128200     GO TO INS-WRITE-F3-19-1.                                     NC2164.2
128300 INS-FAIL-F3-19-1.                                                NC2164.2
128400     PERFORM FAIL                                                 NC2164.2
128500     MOVE WRK-DU-999-1 TO COMPUTED-N                              NC2164.2
128600     MOVE 8 TO CORRECT-N.                                         NC2164.2
128700 INS-WRITE-F3-19-1.                                               NC2164.2
128800     PERFORM PRINT-DETAIL.                                        NC2164.2
128900*                                                                 NC2164.2
129000 INS-TEST-F3-19-2.                                                NC2164.2
129100     ADD 1 TO REC-CT.                                             NC2164.2
129200     IF WRK-DU-999-2 = 0                                          NC2164.2
129300             PERFORM PASS                                         NC2164.2
129400             GO TO INS-WRITE-F3-19-2                              NC2164.2
129500     ELSE                                                         NC2164.2
129600             GO TO INS-FAIL-F3-19-2.                              NC2164.2
129700 INS-DELETE-F3-19-2.                                              NC2164.2
129800     PERFORM DE-LETE.                                             NC2164.2
129900     GO TO INS-WRITE-F3-19-2.                                     NC2164.2
130000 INS-FAIL-F3-19-2.                                                NC2164.2
130100     PERFORM FAIL                                                 NC2164.2
130200     MOVE WRK-DU-999-2 TO COMPUTED-N                              NC2164.2
130300     MOVE 0 TO CORRECT-N.                                         NC2164.2
130400 INS-WRITE-F3-19-2.                                               NC2164.2
130500     PERFORM PRINT-DETAIL.                                        NC2164.2
130600*                                                                 NC2164.2
130700 INS-TEST-F3-19-3.                                                NC2164.2
130800     ADD 1 TO REC-CT.                                             NC2164.2
130900     IF WRK-DU-999-3 = 13                                         NC2164.2
131000             PERFORM PASS                                         NC2164.2
131100             GO TO INS-WRITE-F3-19-3                              NC2164.2
131200     ELSE                                                         NC2164.2
131300             PERFORM FAIL                                         NC2164.2
131400             GO TO INS-FAIL-F3-19-3.                              NC2164.2
131500 INS-DELETE-F3-19-3.                                              NC2164.2
131600     PERFORM DE-LETE.                                             NC2164.2
131700     GO TO INS-WRITE-F3-19-3.                                     NC2164.2
131800 INS-FAIL-F3-19-3.                                                NC2164.2
131900     MOVE WRK-DU-999-3 TO COMPUTED-N                              NC2164.2
132000     MOVE 13 TO CORRECT-N.                                        NC2164.2
132100 INS-WRITE-F3-19-3.                                               NC2164.2
132200     PERFORM PRINT-DETAIL.                                        NC2164.2
132300*                                                                 NC2164.2
132400 INS-TEST-F3-19-4.                                                NC2164.2
132500     ADD 1 TO REC-CT.                                             NC2164.2
132600     IF WRK-DU-999-4 = 5                                          NC2164.2
132700             PERFORM PASS                                         NC2164.2
132800             GO TO INS-WRITE-F3-19-4                              NC2164.2
132900     ELSE                                                         NC2164.2
133000             GO TO INS-FAIL-F3-19-4.                              NC2164.2
133100 INS-DELETE-F3-19-4.                                              NC2164.2
133200     PERFORM DE-LETE.                                             NC2164.2
133300     GO TO INS-WRITE-F3-19-4.                                     NC2164.2
133400 INS-FAIL-F3-19-4.                                                NC2164.2
133500     PERFORM FAIL                                                 NC2164.2
133600     MOVE WRK-DU-999-4 TO COMPUTED-N                              NC2164.2
133700     MOVE 5 TO CORRECT-N.                                         NC2164.2
133800 INS-WRITE-F3-19-4.                                               NC2164.2
133900     PERFORM PRINT-DETAIL.                                        NC2164.2
134000*                                                                 NC2164.2
134100 INS-TEST-F3-19-5.                                                NC2164.2
134200     ADD 1 TO REC-CT.                                             NC2164.2
134300     IF WRK-XN-83-1 = ANS-XN-83-6                                 NC2164.2
134400             PERFORM PASS                                         NC2164.2
134500             GO TO INS-WRITE-F3-19-5                              NC2164.2
134600     ELSE                                                         NC2164.2
134700             GO TO INS-FAIL-F3-19-5.                              NC2164.2
134800 INS-DELETE-F3-19-5.                                              NC2164.2
134900     PERFORM DE-LETE.                                             NC2164.2
135000     GO TO INS-WRITE-F3-19-5.                                     NC2164.2
135100 INS-FAIL-F3-19-5.                                                NC2164.2
135200     PERFORM FAIL                                                 NC2164.2
135300     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
135400     MOVE    ANS-XN-83-6     TO WS-RIGHT-1-83                     NC2164.2
135500     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
135600     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
135700     PERFORM PRINT-DETAIL                                         NC2164.2
135800     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
135900     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
136000     PERFORM PRINT-DETAIL                                         NC2164.2
136100     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
136200     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
136300     PERFORM PRINT-DETAIL                                         NC2164.2
136400     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
136500     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
136600     PERFORM PRINT-DETAIL                                         NC2164.2
136700     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
136800     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
136900 INS-WRITE-F3-19-5.                                               NC2164.2
137000     PERFORM PRINT-DETAIL.                                        NC2164.2
137100*                                                                 NC2164.2
137200 INS-INIT-F3-20.                                                  NC2164.2
137300     MOVE "INS-TEST-F3-20" TO PAR-NAME.                           NC2164.2
137400     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
137500     MOVE "REPLACE SERIES" TO FEATURE.                            NC2164.2
137600     MOVE ZERO TO REC-CT WRK-DU-999-1.                            NC2164.2
137700     MOVE                                                         NC2164.2
137800     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
137900-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
138000         TO WC-XN-83.                                             NC2164.2
138100     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC2164.2
138200     MOVE    1 TO REC-CT.                                         NC2164.2
138300 INS-TEST-F3-20-0.                                                NC2164.2
138400     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1  FOR CHARACTERS    NC2164.2
138500             BEFORE "."                                           NC2164.2
138600             REPLACING                                            NC2164.2
138700             ALL "L BAD." BY "ZZZZZZ" AFTER L-XN-1-1              NC2164.2
138800             ALL " " BY HYPEN-XN-1-1                              NC2164.2
138900             FIRST "C" BY P-XN-1-1                                NC2164.2
139000             LEADING AH-XN-2 BY OH-XN-2                           NC2164.2
139100             ALL "F" BY "Z" BEFORE G-XN-1-1.                      NC2164.2
139200     GO      TO INS-TEST-F3-20-1.                                 NC2164.2
139300 INS-DELETE-F3-20.                                                NC2164.2
139400     PERFORM DE-LETE.                                             NC2164.2
139500     PERFORM PRINT-DETAIL.                                        NC2164.2
139600     GO      TO CCVS-999999.                                      NC2164.2
139700*                                                                 NC2164.2
139800 INS-TEST-F3-20-1.                                                NC2164.2
139900     IF      WRK-DU-999-1 EQUAL TO 15                             NC2164.2
140000             PERFORM PASS                                         NC2164.2
140100             GO TO INS-WRITE-F3-20-1                              NC2164.2
140200     ELSE                                                         NC2164.2
140300             GO TO INS-FAIL-F3-20-1.                              NC2164.2
140400 INS-DELETE-F3-20-1.                                              NC2164.2
140500     PERFORM DE-LETE.                                             NC2164.2
140600     GO TO INS-WRITE-F3-20-1.                                     NC2164.2
140700 INS-FAIL-F3-20-1.                                                NC2164.2
140800     PERFORM FAIL                                                 NC2164.2
140900     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
141000     MOVE    15 TO CORRECT-N.                                     NC2164.2
141100 INS-WRITE-F3-20-1.                                               NC2164.2
141200     PERFORM PRINT-DETAIL.                                        NC2164.2
141300*                                                                 NC2164.2
141400 INS-TEST-F3-20-2.                                                NC2164.2
141500     ADD     1 TO REC-CT.                                         NC2164.2
141600     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-7                     NC2164.2
141700             PERFORM PASS                                         NC2164.2
141800             GO TO INS-WRITE-F3-20-2                              NC2164.2
141900     ELSE                                                         NC2164.2
142000             GO TO INS-FAIL-F3-20-2.                              NC2164.2
142100 INS-DELETE-F3-20-2.                                              NC2164.2
142200     PERFORM DE-LETE.                                             NC2164.2
142300     GO TO INS-WRITE-F3-20-2.                                     NC2164.2
142400 INS-FAIL-F3-20-2.                                                NC2164.2
142500     PERFORM FAIL                                                 NC2164.2
142600     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
142700     MOVE    ANS-XN-83-7     TO WS-RIGHT-1-83                     NC2164.2
142800     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
142900     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
143000     PERFORM PRINT-DETAIL                                         NC2164.2
143100     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
143200     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
143300     PERFORM PRINT-DETAIL                                         NC2164.2
143400     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
143500     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
143600     PERFORM PRINT-DETAIL                                         NC2164.2
143700     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
143800     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
143900     PERFORM PRINT-DETAIL                                         NC2164.2
144000     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
144100     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
144200 INS-WRITE-F3-20-2.                                               NC2164.2
144300     PERFORM PRINT-DETAIL.                                        NC2164.2
144400*                                                                 NC2164.2
144500 INS-INIT-F2-21.                                                  NC2164.2
144600     MOVE    ZERO TO REC-CT.                                      NC2164.2
144700     MOVE    SPACES TO PAR-NAME.                                  NC2164.2
144800     MOVE "INS-TEST-F2-21" TO PAR-NAME.                           NC2164.2
144900     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
145000     MOVE "REPLACE BEFORE" TO FEATURE.                            NC2164.2
145100     MOVE                                                         NC2164.2
145200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
145300-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
145400         TO WC-XN-83.                                             NC2164.2
145500     MOVE    WC-XN-83 TO WRK-XN-83-1.                             NC2164.2
145600 INS-TEST-F2-21.                                                  NC2164.2
145700     INSPECT WRK-XN-83-1                                          NC2164.2
145800         REPLACING ALL SPACE-XN-1-1 BY "-" BEFORE INITIAL "Z".    NC2164.2
145900     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-8                     NC2164.2
146000             PERFORM PASS                                         NC2164.2
146100             GO TO INS-WRITE-F2-21-1                              NC2164.2
146200     ELSE                                                         NC2164.2
146300             GO TO INS-FAIL-F2-21-1.                              NC2164.2
146400 INS-DELETE-F2-21-1.                                              NC2164.2
146500     PERFORM DE-LETE.                                             NC2164.2
146600     GO TO INS-WRITE-F2-21-1.                                     NC2164.2
146700 INS-FAIL-F2-21-1.                                                NC2164.2
146800     PERFORM FAIL                                                 NC2164.2
146900     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
147000     MOVE    ANS-XN-83-8     TO WS-RIGHT-1-83                     NC2164.2
147100     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
147200     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
147300     PERFORM PRINT-DETAIL                                         NC2164.2
147400     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
147500     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
147600     PERFORM PRINT-DETAIL                                         NC2164.2
147700     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
147800     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
147900     PERFORM PRINT-DETAIL                                         NC2164.2
148000     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
148100     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
148200     PERFORM PRINT-DETAIL                                         NC2164.2
148300     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
148400     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
148500 INS-WRITE-F2-21-1.                                               NC2164.2
148600     PERFORM PRINT-DETAIL.                                        NC2164.2
148700*                                                                 NC2164.2
148800 INS-INIT-F2-22.                                                  NC2164.2
148900     MOVE "INS-TEST-F2-22" TO PAR-NAME.                           NC2164.2
149000     MOVE "VI-94 6.17.3"   TO ANSI-REFERENCE.                     NC2164.2
149100     MOVE  "REPLACE AFTER" TO FEATURE.                            NC2164.2
149200     MOVE                                                         NC2164.2
149300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
149400-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
149500         TO WC-XN-83.                                             NC2164.2
149600     MOVE    WC-XN-83 TO WRK-XN-83-1.                             NC2164.2
149700 INS-TEST-F2-22.                                                  NC2164.2
149800     INSPECT WRK-XN-83-1                                          NC2164.2
149900         REPLACING ALL SPACE-XN-1-1 BY "-" AFTER INITIAL "Z".     NC2164.2
150000     IF      WRK-XN-83-1 EQUAL TO WC-XN-83                        NC2164.2
150100             PERFORM PASS                                         NC2164.2
150200             GO TO INS-WRITE-F2-22                                NC2164.2
150300     ELSE                                                         NC2164.2
150400             GO TO INS-FAIL-F2-22.                                NC2164.2
150500 INS-DELETE-F2-22.                                                NC2164.2
150600     PERFORM DE-LETE.                                             NC2164.2
150700     GO TO INS-WRITE-F2-22.                                       NC2164.2
150800 INS-FAIL-F2-22.                                                  NC2164.2
150900     PERFORM FAIL                                                 NC2164.2
151000     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
151100     MOVE    WC-XN-83        TO WS-RIGHT-1-83                     NC2164.2
151200     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
151300     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
151400     PERFORM PRINT-DETAIL                                         NC2164.2
151500     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
151600     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
151700     PERFORM PRINT-DETAIL                                         NC2164.2
151800     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
151900     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
152000     PERFORM PRINT-DETAIL                                         NC2164.2
152100     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
152200     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
152300     PERFORM PRINT-DETAIL                                         NC2164.2
152400     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
152500     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
152600 INS-WRITE-F2-22.                                                 NC2164.2
152700     PERFORM PRINT-DETAIL.                                        NC2164.2
152800*                                                                 NC2164.2
152900 INS-INIT-F1-23.                                                  NC2164.2
153000     MOVE "INS-TEST-F1-23" TO PAR-NAME.                           NC2164.2
153100     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
153200     MOVE    "TALLY SIGNED NUM." TO FEATURE.                      NC2164.2
153300     MOVE    -12345 TO WRK-DS-5V0-1.                              NC2164.2
153400     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
153500     MOVE    ZERO TO WRK-DU-999-2.                                NC2164.2
153600     MOVE    1 TO REC-CT.                                         NC2164.2
153700 INS-TEST-F1-23-0.                                                NC2164.2
153800     INSPECT WRK-DS-5V0-1                                         NC2164.2
153900         TALLYING WRK-DU-999-1 FOR ALL "-"                        NC2164.2
154000                  WRK-DU-999-2 FOR ALL "5".                       NC2164.2
154100     GO      TO INS-TEST-F1-23-1.                                 NC2164.2
154200 INS-DELETE-F1-23.                                                NC2164.2
154300     PERFORM DE-LETE.                                             NC2164.2
154400     PERFORM PRINT-DETAIL.                                        NC2164.2
154500     GO TO INS-INIT-F1-24.                                        NC2164.2
154600*                                                                 NC2164.2
154700 INS-TEST-F1-23-1.                                                NC2164.2
154800     IF      WRK-DU-999-1 EQUAL 0                                 NC2164.2
154900             PERFORM PASS                                         NC2164.2
155000             GO TO INS-WRITE-F1-23-1                              NC2164.2
155100     ELSE                                                         NC2164.2
155200             PERFORM FAIL                                         NC2164.2
155300             GO TO INS-FAIL-F1-23-1.                              NC2164.2
155400 INS-DELETE-F1-23-1.                                              NC2164.2
155500     PERFORM DE-LETE.                                             NC2164.2
155600     GO TO INS-WRITE-F1-23-1.                                     NC2164.2
155700 INS-FAIL-F1-23-1.                                                NC2164.2
155800     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
155900     MOVE    ZERO TO CORRECT-N.                                   NC2164.2
156000 INS-WRITE-F1-23-1.                                               NC2164.2
156100     PERFORM PRINT-DETAIL.                                        NC2164.2
156200*                                                                 NC2164.2
156300 INS-TEST-F1-23-2.                                                NC2164.2
156400     ADD     1 TO REC-CT.                                         NC2164.2
156500     IF      WRK-DU-999-2 EQUAL TO 1                              NC2164.2
156600     PERFORM PASS                                                 NC2164.2
156700             GO TO INS-WRITE-F1-23-2                              NC2164.2
156800     ELSE                                                         NC2164.2
156900             PERFORM FAIL                                         NC2164.2
157000             GO TO INS-FAIL-F1-23-2.                              NC2164.2
157100 INS-DELETE-F1-23-2.                                              NC2164.2
157200     PERFORM DE-LETE.                                             NC2164.2
157300     GO TO INS-WRITE-F1-23-2.                                     NC2164.2
157400 INS-FAIL-F1-23-2.                                                NC2164.2
157500     MOVE    WRK-DU-999-2 TO COMPUTED-N                           NC2164.2
157600     MOVE    1 TO CORRECT-N.                                      NC2164.2
157700 INS-WRITE-F1-23-2.                                               NC2164.2
157800     PERFORM PRINT-DETAIL.                                        NC2164.2
157900*                                                                 NC2164.2
158000 INS-INIT-F1-24.                                                  NC2164.2
158100     MOVE "INS-TEST-F1-24" TO PAR-NAME.                           NC2164.2
158200     MOVE "NUMERIC EDITED FIELD" TO FEATURE.                      NC2164.2
158300     MOVE "VI-94" TO ANSI-REFERENCE.                              NC2164.2
158400     MOVE  123456.789  TO WRK-NE-1.                               NC2164.2
158500     MOVE    ZERO TO REC-CT.                                      NC2164.2
158600     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
158700 INS-TEST-F1-24.                                                  NC2164.2
158800     INSPECT WRK-NE-1  TALLYING WRK-DU-999-1  FOR ALL ",".        NC2164.2
158900     IF WRK-DU-999-1 EQUAL TO 1                                   NC2164.2
159000             PERFORM PASS                                         NC2164.2
159100             GO TO INS-WRITE-F1-24                                NC2164.2
159200     ELSE                                                         NC2164.2
159300             GO TO INS-FAIL-F1-24.                                NC2164.2
159400 INS-DELETE-F1-24.                                                NC2164.2
159500     PERFORM DE-LETE.                                             NC2164.2
159600     GO TO INS-WRITE-F1-24.                                       NC2164.2
159700 INS-FAIL-F1-24.                                                  NC2164.2
159800     PERFORM FAIL                                                 NC2164.2
159900     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
160000     MOVE    1 TO CORRECT-N.                                      NC2164.2
160100 INS-WRITE-F1-24.                                                 NC2164.2
160200     PERFORM PRINT-DETAIL.                                        NC2164.2
160300*                                                                 NC2164.2
160400 INS-INIT-F1-25.                                                  NC2164.2
160500     MOVE "INS-TEST-F1-25" TO PAR-NAME.                           NC2164.2
160600     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
160700     MOVE    "NUMERIC EDITED FIELD" TO FEATURE.                   NC2164.2
160800     MOVE    123456.789 TO WRK-NE-1.                              NC2164.2
160900     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
161000 INS-TEST-F1-25.                                                  NC2164.2
161100     INSPECT WRK-NE-1  TALLYING WRK-DU-999-1  FOR ALL "-".        NC2164.2
161200     IF      WRK-DU-999-1 EQUAL TO ZERO                           NC2164.2
161300             PERFORM PASS                                         NC2164.2
161400             GO TO INS-WRITE-F1-25                                NC2164.2
161500     ELSE                                                         NC2164.2
161600             GO TO INS-FAIL-F1-25.                                NC2164.2
161700 INS-DELETE-F1-25.                                                NC2164.2
161800     PERFORM DE-LETE.                                             NC2164.2
161900     GO TO INS-WRITE-F1-25.                                       NC2164.2
162000 INS-FAIL-F1-25.                                                  NC2164.2
162100     PERFORM FAIL                                                 NC2164.2
162200     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
162300     MOVE    ZERO TO CORRECT-N.                                   NC2164.2
162400 INS-WRITE-F1-25.                                                 NC2164.2
162500     PERFORM PRINT-DETAIL.                                        NC2164.2
162600*                                                                 NC2164.2
162700 INS-INIT-F1-26.                                                  NC2164.2
162800     MOVE    "INS-TEST-F1-26" TO PAR-NAME.                        NC2164.2
162900     MOVE    "VI-94" TO ANSI-REFERENCE.                           NC2164.2
163000     MOVE    "2 CHARACTER MASK" TO FEATURE.                       NC2164.2
163100     MOVE    "X X" TO X-SPACE-X-XN-3.                             NC2164.2
163200     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
163300 INS-TEST-F1-26.                                                  NC2164.2
163400     INSPECT X-SPACE-X-XN-3 TALLYING WRK-DU-999-1 FOR ALL "X ".   NC2164.2
163500     IF      WRK-DU-999-1 EQUAL TO 1                              NC2164.2
163600             PERFORM PASS                                         NC2164.2
163700             GO TO INS-WRITE-F1-26                                NC2164.2
163800     ELSE                                                         NC2164.2
163900             GO TO INS-FAIL-F1-26.                                NC2164.2
164000 INS-DELETE-F1-26.                                                NC2164.2
164100     PERFORM DE-LETE.                                             NC2164.2
164200     GO TO INS-WRITE-F1-26.                                       NC2164.2
164300 INS-FAIL-F1-26.                                                  NC2164.2
164400     PERFORM FAIL                                                 NC2164.2
164500     MOVE WRK-DU-999-1 TO COMPUTED-N                              NC2164.2
164600     MOVE 1 TO CORRECT-N.                                         NC2164.2
164700 INS-WRITE-F1-26.                                                 NC2164.2
164800     PERFORM PRINT-DETAIL.                                        NC2164.2
164900*                                                                 NC2164.2
165000 INS-INIT-F1-27.                                                  NC2164.2
165100     MOVE   "INS-TEST-F1-27" TO PAR-NAME.                         NC2164.2
165200     MOVE   "VI-94" TO ANSI-REFERENCE.                            NC2164.2
165300     MOVE   "ORDER OF COMPARE" TO FEATURE.                        NC2164.2
165400     MOVE   "AABA" TO AABA-XN-4.                                  NC2164.2
165500     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
165600     MOVE    ZERO TO WRK-DU-999-2.                                NC2164.2
165700 INS-TEST-F1-27.                                                  NC2164.2
165800     INSPECT AABA-XN-4 TALLYING WRK-DU-999-1 FOR ALL "AA"         NC2164.2
165900                                WRK-DU-999-2 FOR ALL "A".         NC2164.2
166000     IF      WRK-DU-999-1 EQUAL TO 1                              NC2164.2
166100         AND                                                      NC2164.2
166200             WRK-DU-999-2 EQUAL TO 1                              NC2164.2
166300             PERFORM PASS                                         NC2164.2
166400             GO TO INS-WRITE-F1-27                                NC2164.2
166500     ELSE                                                         NC2164.2
166600             GO TO INS-FAIL-F1-27.                                NC2164.2
166700 INS-DELETE-F1-27.                                                NC2164.2
166800     PERFORM DE-LETE.                                             NC2164.2
166900     GO TO INS-WRITE-F1-27.                                       NC2164.2
167000 INS-FAIL-F1-27.                                                  NC2164.2
167100     PERFORM FAIL                                                 NC2164.2
167200     MOVE WRK-DU-999-1 TO COMPUTED-N                              NC2164.2
167300     MOVE WRK-DU-999-2 TO CORRECT-N                               NC2164.2
167400     MOVE "BOTH SHOULD BE 1" TO RE-MARK.                          NC2164.2
167500 INS-WRITE-F1-27.                                                 NC2164.2
167600     PERFORM PRINT-DETAIL.                                        NC2164.2
167700*                                                                 NC2164.2
167800 INS-INIT-F1-28.                                                  NC2164.2
167900*    ===-->  BEFORE AND AFTER PHRASES  <--===                     NC2164.2
168000     MOVE   "INS-TEST-F1-28" TO PAR-NAME.                         NC2164.2
168100     MOVE   "VI-94 6.17.3 SR4" TO ANSI-REFERENCE.                 NC2164.2
168200     MOVE   "TALLY FOR ALL SPACES" TO FEATURE.                    NC2164.2
168300     MOVE                                                         NC2164.2
168400     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
168500-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
168600         TO WC-XN-83.                                             NC2164.2
168700     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
168800 INS-TEST-F1-28.                                                  NC2164.2
168900     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL SPACES        NC2164.2
169000             AFTER  "C."                                          NC2164.2
169100             BEFORE "DO".                                         NC2164.2
169200     IF      WRK-DU-999-1 EQUAL TO 6                              NC2164.2
169300             PERFORM PASS                                         NC2164.2
169400             GO TO   INS-WRITE-F1-28.                             NC2164.2
169500     GO TO   INS-FAIL-F1-28.                                      NC2164.2
169600 INS-DELETE-F1-28.                                                NC2164.2
169700     PERFORM DE-LETE.                                             NC2164.2
169800     GO TO   INS-WRITE-F1-28.                                     NC2164.2
169900 INS-FAIL-F1-28.                                                  NC2164.2
170000     PERFORM FAIL.                                                NC2164.2
170100     MOVE    WRK-DU-999-1 TO COMPUTED-N.                          NC2164.2
170200     MOVE    6 TO CORRECT-N.                                      NC2164.2
170300 INS-WRITE-F1-28.                                                 NC2164.2
170400     PERFORM PRINT-DETAIL.                                        NC2164.2
170500*                                                                 NC2164.2
170600 INS-INIT-F1-29.                                                  NC2164.2
170700*    ===-->  BEFORE AND AFTER PHRASES  <--===                     NC2164.2
170800     MOVE "INS-TEST-F1-29" TO PAR-NAME.                           NC2164.2
170900     MOVE "VI-94 6.17.3 SR4" TO ANSI-REFERENCE.                   NC2164.2
171000     MOVE "FOR CHARS AFTER LIT" TO FEATURE.                       NC2164.2
171100     MOVE                                                         NC2164.2
171200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
171300-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
171400         TO WC-XN-83.                                             NC2164.2
171500     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
171600 INS-TEST-F1-29.                                                  NC2164.2
171700     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR CHARACTERS        NC2164.2
171800             BEFORE "KI"                                          NC2164.2
171900             AFTER  " W".                                         NC2164.2
172000     IF      WRK-DU-999-1 EQUAL TO 44                             NC2164.2
172100             PERFORM PASS                                         NC2164.2
172200             GO TO   INS-WRITE-F1-29.                             NC2164.2
172300     GO TO   INS-FAIL-F1-29.                                      NC2164.2
172400 INS-DELETE-F1-29.                                                NC2164.2
172500     PERFORM DE-LETE.                                             NC2164.2
172600     GO TO   INS-WRITE-F1-29.                                     NC2164.2
172700 INS-FAIL-F1-29.                                                  NC2164.2
172800     PERFORM FAIL.                                                NC2164.2
172900     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC2164.2
173000     MOVE 44 TO CORRECT-N.                                        NC2164.2
173100 INS-WRITE-F1-29.                                                 NC2164.2
173200     PERFORM PRINT-DETAIL.                                        NC2164.2
173300*                                                                 NC2164.2
173400 INS-INIT-F1-30.                                                  NC2164.2
173500*    ===-->  EVALUATION OF SUBSCRIPTED IDENTIFIERS  <--===        NC2164.2
173600     MOVE   "INS-TEST-F1-30" TO PAR-NAME.                         NC2164.2
173700     MOVE   "VI-95 6.17.4 GR4 & VI-97 6.17.4 GR8"                 NC2164.2
173800          TO ANSI-REFERENCE.                                      NC2164.2
173900     MOVE   "FOR CHARS AFTER LIT" TO FEATURE.                     NC2164.2
174000     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
174100     MOVE "XXXXXXXYYYYYYYAAABAAASSSSSSSTTTTTTT"                   NC2164.2
174200          TO INSPECT-FIELDS.                                      NC2164.2
174300     MOVE "GHBDC" TO LOCATE-CHARS.                                NC2164.2
174400     MOVE    3    TO SUB.                                         NC2164.2
174500 INS-TEST-F1-30-0.                                                NC2164.2
174600     INSPECT DATA-FIELD (SUB)                                     NC2164.2
174700             TALLYING WRK-DU-999-1                                NC2164.2
174800             FOR ALL "A" BEFORE END-CHAR (SUB)                    NC2164.2
174900                 ALL END-CHAR (SUB).                              NC2164.2
175000 INS-TEST-F1-30-1.                                                NC2164.2
175100     IF      WRK-DU-999-1 EQUAL TO 4                              NC2164.2
175200             PERFORM PASS                                         NC2164.2
175300             GO TO   INS-WRITE-F1-30-1.                           NC2164.2
175400     GO TO   INS-FAIL-F1-30-1.                                    NC2164.2
175500 INS-DELETE-F1-30-1.                                              NC2164.2
175600     PERFORM DE-LETE.                                             NC2164.2
175700     GO TO   INS-WRITE-F1-30-1.                                   NC2164.2
175800 INS-FAIL-F1-30-1.                                                NC2164.2
175900     PERFORM FAIL.                                                NC2164.2
176000     MOVE    WRK-DU-999-1 TO COMPUTED-N.                          NC2164.2
176100     MOVE    4 TO CORRECT-N.                                      NC2164.2
176200 INS-WRITE-F1-30-1.                                               NC2164.2
176300     PERFORM PRINT-DETAIL.                                        NC2164.2
176400*                                                                 NC2164.2
176500 INS-INIT-F1-31.                                                  NC2164.2
176600     MOVE   "INS-TEST-F1-31" TO PAR-NAME.                         NC2164.2
176700     MOVE   "VI-93" TO ANSI-REFERENCE.                            NC2164.2
176800     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
176900     MOVE   "AABBCCDDEBBBBGHDDIJJXXAABBCCDDEEEFFGGHHIIJJKKLLM"    NC2164.2
177000         TO TEST-31-DATA.                                         NC2164.2
177100     MOVE "BB" TO WS-BB.                                          NC2164.2
177200 INS-TEST-F1-31-0.                                                NC2164.2
177300     INSPECT TEST-31-DATA TALLYING WRK-DU-999-1                   NC2164.2
177400             FOR ALL "A"    BEFORE "X"                            NC2164.2
177500                 ALL  WS-BB BEFORE "X"                            NC2164.2
177600                 ALL "D"    BEFORE "X".                           NC2164.2
177700 INS-TEST-F1-31-1.                                                NC2164.2
177800     IF      WRK-DU-999-1 EQUAL TO 9                              NC2164.2
177900             PERFORM PASS                                         NC2164.2
178000             GO TO   INS-WRITE-F1-31-1.                           NC2164.2
178100     GO TO   INS-FAIL-F1-31-1.                                    NC2164.2
178200 INS-DELETE-F1-31-1.                                              NC2164.2
178300     PERFORM DE-LETE.                                             NC2164.2
178400     GO TO   INS-WRITE-F1-31-1.                                   NC2164.2
178500 INS-FAIL-F1-31-1.                                                NC2164.2
178600     PERFORM FAIL.                                                NC2164.2
178700     MOVE    WRK-DU-999-1 TO COMPUTED-N.                          NC2164.2
178800     MOVE    10 TO CORRECT-N.                                     NC2164.2
178900 INS-WRITE-F1-31-1.                                               NC2164.2
179000     PERFORM PRINT-DETAIL.                                        NC2164.2
179100*                                                                 NC2164.2
179200 INS-INIT-F1-32.                                                  NC2164.2
179300     MOVE   "INS-TEST-F1-32" TO PAR-NAME.                         NC2164.2
179400     MOVE   "VI-93" TO ANSI-REFERENCE.                            NC2164.2
179500     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
179600     MOVE "AABSSSSSEBBTTTT1URSTSTSTVVDYYDEEEFFGSSSSTZSTZSTM"      NC2164.2
179700         TO TEST-32-DATA.                                         NC2164.2
179800     MOVE "Y" TO WS-Y.                                            NC2164.2
179900 INS-TEST-F1-32-0.                                                NC2164.2
180000     INSPECT TEST-32-DATA TALLYING WRK-DU-999-1                   NC2164.2
180100             FOR LEADING "S" AFTER WS-Y                           NC2164.2
180200                         "S" AFTER "U"                            NC2164.2
180300                         "T" AFTER WS-Y                           NC2164.2
180400                         "T" AFTER "U".                           NC2164.2
180500 INS-TEST-F1-32-1.                                                NC2164.2
180600     IF      WRK-DU-999-1 EQUAL TO 0                              NC2164.2
180700             PERFORM PASS                                         NC2164.2
180800             GO TO   INS-WRITE-F1-32-1.                           NC2164.2
180900     GO TO   INS-FAIL-F1-32-1.                                    NC2164.2
181000 INS-DELETE-F1-32-1.                                              NC2164.2
181100     PERFORM DE-LETE.                                             NC2164.2
181200     GO TO   INS-WRITE-F1-32-1.                                   NC2164.2
181300 INS-FAIL-F1-32-1.                                                NC2164.2
181400     PERFORM FAIL.                                                NC2164.2
181500     MOVE    WRK-DU-999-1 TO COMPUTED-N.                          NC2164.2
181600     MOVE    0 TO CORRECT-N.                                      NC2164.2
181700 INS-WRITE-F1-32-1.                                               NC2164.2
181800     PERFORM PRINT-DETAIL.                                        NC2164.2
181900*                                                                 NC2164.2
182000 INS-INIT-F2-33.                                                  NC2164.2
182100*    ===--> "BEFORE" AND "AFTER" PHRASES  <--===                  NC2164.2
182200     MOVE "INS-TEST-F2-33" TO PAR-NAME.                           NC2164.2
182300     MOVE "VI-94 6.17.3 SR4" TO ANSI-REFERENCE.                   NC2164.2
182400     MOVE "REP CHARS BY SPACES" TO FEATURE.                       NC2164.2
182500     MOVE                                                         NC2164.2
182600     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
182700-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
182800         TO WC-XN-83.                                             NC2164.2
182900     MOVE    WC-XN-83 TO WRK-XN-83-1.                             NC2164.2
183000     MOVE                                                         NC2164.2
183100     "AH YES AH YES W.C                                           NC2164.2
183200-    "            BE ALL BAD."                                    NC2164.2
183300         TO ANS-XN-83-10.                                         NC2164.2
183400 INS-TEST-F2-33-0.                                                NC2164.2
183500     INSPECT WRK-XN-83-1 REPLACING CHARACTERS BY SPACES           NC2164.2
183600             BEFORE "B" AFTER "C".                                NC2164.2
183700 INS-TEST-F2-33-1.                                                NC2164.2
183800     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-10                    NC2164.2
183900             PERFORM PASS                                         NC2164.2
184000             GO TO   INS-WRITE-F2-33-1.                           NC2164.2
184100     GO TO   INS-FAIL-F2-33-1.                                    NC2164.2
184200 INS-DELETE-F2-33-1.                                              NC2164.2
184300     PERFORM DE-LETE.                                             NC2164.2
184400     GO TO   INS-WRITE-F2-33-1.                                   NC2164.2
184500 INS-FAIL-F2-33-1.                                                NC2164.2
184600     PERFORM FAIL.                                                NC2164.2
184700     MOVE    WRK-XN-83-1  TO WS-WRONG-1-83.                       NC2164.2
184800     MOVE    ANS-XN-83-10 TO WS-RIGHT-1-83.                       NC2164.2
184900     MOVE "THERE SHOUD BE 55 SPACES BETWEEN ""B"" AND ""C""."     NC2164.2
185000          TO RE-MARK.                                             NC2164.2
185100     MOVE    WS-RIGHT-1-20   TO CORRECT-A.                        NC2164.2
185200     MOVE    WS-WRONG-1-20   TO COMPUTED-A.                       NC2164.2
185300     PERFORM PRINT-DETAIL.                                        NC2164.2
185400     MOVE    WS-RIGHT-21-40  TO CORRECT-A.                        NC2164.2
185500     MOVE    WS-WRONG-21-40  TO COMPUTED-A.                       NC2164.2
185600     PERFORM PRINT-DETAIL.                                        NC2164.2
185700     MOVE    WS-RIGHT-41-60  TO CORRECT-A.                        NC2164.2
185800     MOVE    WS-WRONG-41-60  TO COMPUTED-A.                       NC2164.2
185900     PERFORM PRINT-DETAIL.                                        NC2164.2
186000     MOVE    WS-RIGHT-61-80  TO CORRECT-A.                        NC2164.2
186100     MOVE    WS-WRONG-61-80  TO COMPUTED-A.                       NC2164.2
186200     PERFORM PRINT-DETAIL.                                        NC2164.2
186300     MOVE    WS-RIGHT-81-83  TO CORRECT-A.                        NC2164.2
186400     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
186500 INS-WRITE-F2-33-1.                                               NC2164.2
186600     PERFORM PRINT-DETAIL.                                        NC2164.2
186700*                                                                 NC2164.2
186800 INS-INIT-F2-34.                                                  NC2164.2
186900*    ===-->  MULTIPLE "CHARACTERS" PHRASES WITH  <--===           NC2164.2
187000*    ===-->         "BEFORE" AND "AFTER"         <--===           NC2164.2
187100     MOVE   "INS-TEST-F2-34" TO PAR-NAME.                         NC2164.2
187200     MOVE   "VI-93 6.17.2" TO ANSI-REFERENCE.                     NC2164.2
187300     MOVE   "AAFSSQ  ET U V W H S" TO TEST-34-DATA.               NC2164.2
187400     MOVE   "AAFXXQ  ETYYYYYYYH S" TO TEST-34-ANSWER.             NC2164.2
187500 INS-TEST-F2-34-0.                                                NC2164.2
187600     INSPECT TEST-34-DATA                                         NC2164.2
187700             REPLACING                                            NC2164.2
187800             CHARACTERS BY "X" AFTER "F" BEFORE "Q"               NC2164.2
187900             CHARACTERS BY "Y" AFTER "T" BEFORE "H".              NC2164.2
188000 INS-TEST-F2-34-1.                                                NC2164.2
188100     IF      TEST-34-DATA = TEST-34-ANSWER                        NC2164.2
188200             PERFORM PASS                                         NC2164.2
188300             GO TO   INS-WRITE-F2-34-1.                           NC2164.2
188400     GO TO   INS-FAIL-F2-34-1.                                    NC2164.2
188500 INS-DELETE-F2-34-1.                                              NC2164.2
188600     PERFORM DE-LETE.                                             NC2164.2
188700     GO TO   INS-WRITE-F2-34-1.                                   NC2164.2
188800 INS-FAIL-F2-34-1.                                                NC2164.2
188900     PERFORM FAIL.                                                NC2164.2
189000     MOVE    TEST-34-DATA TO COMPUTED-A.                          NC2164.2
189100     MOVE    TEST-34-ANSWER TO CORRECT-A.                         NC2164.2
189200 INS-WRITE-F2-34-1.                                               NC2164.2
189300     PERFORM PRINT-DETAIL.                                        NC2164.2
189400*                                                                 NC2164.2
189500 INS-INIT-F3-35.                                                  NC2164.2
189600*    ===-->  MULTIPLE "CHARACTERS" PHRASES WITH  <--===           NC2164.2
189700*    ===-->         "BEFORE" AND "AFTER"         <--===           NC2164.2
189800     MOVE   "INS-TEST-F3-35-1" TO PAR-NAME.                       NC2164.2
189900     MOVE   "VI-93 6.17.2" TO ANSI-REFERENCE.                     NC2164.2
190000     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
190100     MOVE "AX SSA YEG U V W H S"  TO TEST-35-DATA.                NC2164.2
190200     MOVE "AXAAAAAYEG       H S"  TO TEST-35-ANSWER.              NC2164.2
190300 INS-TEST-F3-35-0.                                                NC2164.2
190400     INSPECT TEST-35-DATA TALLYING WRK-DU-999-1                   NC2164.2
190500             FOR CHARACTERS                                       NC2164.2
190600             REPLACING                                            NC2164.2
190700             CHARACTERS BY   "A" AFTER "X" BEFORE "Y"             NC2164.2
190800             CHARACTERS BY SPACE AFTER "G" BEFORE "H".            NC2164.2
190900 INS-TEST-F3-35-1.                                                NC2164.2
191000     IF      TEST-35-DATA = TEST-35-ANSWER                        NC2164.2
191100             PERFORM PASS                                         NC2164.2
191200             GO TO   INS-WRITE-F3-35-1.                           NC2164.2
191300     GO TO   INS-FAIL-F3-35-1.                                    NC2164.2
191400 INS-DELETE-F3-35-1.                                              NC2164.2
191500     PERFORM DE-LETE.                                             NC2164.2
191600     GO TO   INS-WRITE-F3-35-1.                                   NC2164.2
191700 INS-FAIL-F3-35-1.                                                NC2164.2
191800     PERFORM FAIL.                                                NC2164.2
191900     MOVE    TEST-35-DATA TO COMPUTED-A.                          NC2164.2
192000     MOVE    TEST-35-ANSWER TO CORRECT-A.                         NC2164.2
192100 INS-WRITE-F3-35-1.                                               NC2164.2
192200     PERFORM PRINT-DETAIL.                                        NC2164.2
192300*                                                                 NC2164.2
192400 INS-INIT-F3-36.                                                  NC2164.2
192500*    ===-->    "BEFORE" AND "AFTER" PHRASES     <--===            NC2164.2
192600     MOVE   "INS-TEST-F3-36" TO PAR-NAME.                         NC2164.2
192700     MOVE   "TALLY-REPLACE CHARS" TO FEATURE.                     NC2164.2
192800     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
192900     MOVE                                                         NC2164.2
193000     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
193100-    "IDS CAN NOT BE ALL BAZ."                                    NC2164.2
193200         TO WC-XN-83.                                             NC2164.2
193300     MOVE    WC-XN-83 TO WRK-XN-83-1.                             NC2164.2
193400     MOVE                                                         NC2164.2
193500     "AH                                                          NC2164.2
193600-    "                     Z."                                    NC2164.2
193700          TO ANS-XN-83-11.                                        NC2164.2
193800     MOVE    1 TO REC-CT.                                         NC2164.2
193900 INS-TEST-F3-36-0.                                                NC2164.2
194000     INSPECT WRK-XN-83-1                                          NC2164.2
194100             TALLYING  WRK-DU-999-1 FOR CHARACTERS                NC2164.2
194200                       BEFORE "LL" AFTER "ES"                     NC2164.2
194300             REPLACING CHARACTERS BY SPACES                       NC2164.2
194400                       AFTER "H" BEFORE "Z".                      NC2164.2
194500     GO TO   INS-TEST-F3-36-1.                                    NC2164.2
194600 INS-DELETE-F3-36.                                                NC2164.2
194700     PERFORM DE-LETE.                                             NC2164.2
194800     PERFORM PRINT-DETAIL.                                        NC2164.2
194900     GO      TO INS-INIT-F3-37.                                   NC2164.2
195000 INS-TEST-F3-36-1.                                                NC2164.2
195100     IF      WRK-DU-999-1 EQUAL TO 70                             NC2164.2
195200             PERFORM PASS                                         NC2164.2
195300             GO TO INS-WRITE-F3-36-1                              NC2164.2
195400     ELSE                                                         NC2164.2
195500             GO TO INS-FAIL-F3-36-1.                              NC2164.2
195600 INS-DELETE-F3-36-1.                                              NC2164.2
195700     PERFORM DE-LETE.                                             NC2164.2
195800     GO TO INS-WRITE-F3-36-1.                                     NC2164.2
195900 INS-FAIL-F3-36-1.                                                NC2164.2
196000     PERFORM FAIL                                                 NC2164.2
196100     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
196200     MOVE    70 TO CORRECT-N.                                     NC2164.2
196300 INS-WRITE-F3-36-1.                                               NC2164.2
196400     PERFORM PRINT-DETAIL.                                        NC2164.2
196500*                                                                 NC2164.2
196600 INS-TEST-F3-36-2.                                                NC2164.2
196700     ADD     1 TO REC-CT.                                         NC2164.2
196800     IF      WRK-XN-83-1 = ANS-XN-83-11                           NC2164.2
196900             PERFORM PASS                                         NC2164.2
197000             GO TO INS-WRITE-F3-36-2                              NC2164.2
197100     ELSE                                                         NC2164.2
197200             GO TO INS-FAIL-F3-36-2.                              NC2164.2
197300 INS-DELETE-F3-36-2.                                              NC2164.2
197400     PERFORM DE-LETE.                                             NC2164.2
197500     GO TO INS-WRITE-F3-36-2.                                     NC2164.2
197600 INS-FAIL-F3-36-2.                                                NC2164.2
197700     PERFORM FAIL                                                 NC2164.2
197800     MOVE    WRK-XN-83-1  TO WS-WRONG-1-83                        NC2164.2
197900     MOVE    ANS-XN-83-11 TO WS-RIGHT-1-83                        NC2164.2
198000     MOVE "THERE SHOULD BE 81 SPACES BETWEEN ""H"" AND ""Z""."    NC2164.2
198100             TO RE-MARK                                           NC2164.2
198200     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
198300     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
198400     PERFORM PRINT-DETAIL                                         NC2164.2
198500     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
198600     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
198700     PERFORM PRINT-DETAIL                                         NC2164.2
198800     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
198900     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
199000     PERFORM PRINT-DETAIL                                         NC2164.2
199100     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
199200     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
199300     PERFORM PRINT-DETAIL                                         NC2164.2
199400     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
199500     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
199600 INS-WRITE-F3-36-2.                                               NC2164.2
199700     PERFORM PRINT-DETAIL.                                        NC2164.2
199800*                                                                 NC2164.2
199900 INS-INIT-F3-37.                                                  NC2164.2
200000*    ===-->    "BEFORE" AND "AFTER" PHRASES     <--===            NC2164.2
200100     MOVE   "INS-TEST-F3-37" TO PAR-NAME.                         NC2164.2
200200     MOVE   "REPL FIRST AFTER" TO FEATURE.                        NC2164.2
200300     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
200400     MOVE                                                         NC2164.2
200500     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
200600-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
200700         TO WC-XN-83.                                             NC2164.2
200800     MOVE    WC-XN-83 TO WRK-XN-83-1.                             NC2164.2
200900     MOVE                                                         NC2164.2
201000     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2164.2
201100-    "IDS CAN NOT BE ALL BAD."                                    NC2164.2
201200         TO ANS-XN-83-5.                                          NC2164.2
201300     MOVE    1 TO REC-CT.                                         NC2164.2
201400 INS-TEST-F3-37-0.                                                NC2164.2
201500     INSPECT WRK-XN-83-1                                          NC2164.2
201600             TALLYING  WRK-DU-999-1 FOR ALL "A"                   NC2164.2
201700                       BEFORE L-XN-1-1                            NC2164.2
201800                       AFTER  "YE"                                NC2164.2
201900             REPLACING FIRST  AH-XN-2 BY "OH"                     NC2164.2
202000                       AFTER  INITIAL HSPACE-XN-2                 NC2164.2
202100                       BEFORE "F".                                NC2164.2
202200     GO TO INS-TEST-F3-37-1.                                      NC2164.2
202300 INS-DELETE-F3-37.                                                NC2164.2
202400     PERFORM DE-LETE.                                             NC2164.2
202500     PERFORM PRINT-DETAIL.                                        NC2164.2
202600     GO      TO INS-INIT-F3-38.                                   NC2164.2
202700 INS-TEST-F3-37-1.                                                NC2164.2
202800     IF      WRK-DU-999-1 EQUAL TO 6                              NC2164.2
202900             PERFORM PASS                                         NC2164.2
203000             GO TO INS-WRITE-F3-37-1                              NC2164.2
203100     ELSE                                                         NC2164.2
203200             GO TO INS-FAIL-F3-37-1.                              NC2164.2
203300 INS-DELETE-F3-37-1.                                              NC2164.2
203400     PERFORM DE-LETE.                                             NC2164.2
203500     GO TO INS-WRITE-F3-37-1.                                     NC2164.2
203600 INS-FAIL-F3-37-1.                                                NC2164.2
203700     PERFORM FAIL                                                 NC2164.2
203800     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC2164.2
203900     MOVE    6 TO CORRECT-N.                                      NC2164.2
204000 INS-WRITE-F3-37-1.                                               NC2164.2
204100     PERFORM PRINT-DETAIL.                                        NC2164.2
204200*                                                                 NC2164.2
204300 INS-TEST-F3-37-2.                                                NC2164.2
204400     ADD     1 TO REC-CT.                                         NC2164.2
204500     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-5                     NC2164.2
204600             PERFORM PASS                                         NC2164.2
204700             GO TO INS-WRITE-F3-37-2                              NC2164.2
204800     ELSE                                                         NC2164.2
204900             GO TO INS-FAIL-F3-37-2.                              NC2164.2
205000 INS-DELETE-F3-37-2.                                              NC2164.2
205100     PERFORM DE-LETE.                                             NC2164.2
205200     GO TO INS-WRITE-F3-37-2.                                     NC2164.2
205300 INS-FAIL-F3-37-2.                                                NC2164.2
205400     PERFORM FAIL                                                 NC2164.2
205500     MOVE    WRK-XN-83-1     TO WS-WRONG-1-83                     NC2164.2
205600     MOVE    ANS-XN-83-5     TO WS-RIGHT-1-83                     NC2164.2
205700     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2164.2
205800     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2164.2
205900     PERFORM PRINT-DETAIL                                         NC2164.2
206000     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2164.2
206100     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2164.2
206200     PERFORM PRINT-DETAIL                                         NC2164.2
206300     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2164.2
206400     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2164.2
206500     PERFORM PRINT-DETAIL                                         NC2164.2
206600     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2164.2
206700     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2164.2
206800     PERFORM PRINT-DETAIL                                         NC2164.2
206900     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2164.2
207000     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2164.2
207100 INS-WRITE-F3-37-2.                                               NC2164.2
207200     PERFORM PRINT-DETAIL.                                        NC2164.2
207300*                                                                 NC2164.2
207400 INS-INIT-F3-38.                                                  NC2164.2
207500*    ===-->  MULTIPLE OPERANDS FOR "ALL"         <--===           NC2164.2
207600     MOVE   "INS-TEST-F3-38" TO PAR-NAME.                         NC2164.2
207700     MOVE   "VI-93 6.17.2" TO ANSI-REFERENCE.                     NC2164.2
207800     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
207900     MOVE "E" TO WS-E.                                            NC2164.2
208000     MOVE "AXESSA YEGTUASSW H S"  TO TEST-38-DATA.                NC2164.2
208100     MOVE    ZERO TO REC-CT.                                      NC2164.2
208200 INS-TEST-F3-38-0.                                                NC2164.2
208300     INSPECT TEST-38-DATA TALLYING WRK-DU-999-1                   NC2164.2
208400             FOR                                                  NC2164.2
208500             ALL "A" AFTER WS-E                                   NC2164.2
208600                 "A" AFTER "T"                                    NC2164.2
208700                 "S" AFTER WS-E                                   NC2164.2
208800                 "S" AFTER "T".                                   NC2164.2
208900 INS-TEST-F3-38-1.                                                NC2164.2
209000     IF      WRK-DU-999-1 = 7                                     NC2164.2
209100             PERFORM PASS                                         NC2164.2
209200             GO TO   INS-WRITE-F3-38.                             NC2164.2
209300     GO TO   INS-FAIL-F3-38.                                      NC2164.2
209400 INS-DELETE-F3-38.                                                NC2164.2
209500     PERFORM DE-LETE.                                             NC2164.2
209600     GO TO   INS-WRITE-F3-38.                                     NC2164.2
209700 INS-FAIL-F3-38.                                                  NC2164.2
209800     PERFORM FAIL.                                                NC2164.2
209900     MOVE    WRK-DU-999-1 TO COMPUTED-N.                          NC2164.2
210000     MOVE    7 TO CORRECT-N.                                      NC2164.2
210100 INS-WRITE-F3-38.                                                 NC2164.2
210200     PERFORM PRINT-DETAIL.                                        NC2164.2
210300*                                                                 NC2164.2
210400 INS-INIT-F3-39.                                                  NC2164.2
210500*    ===-->  MULTIPLE OPERANDS FOR "LEADING"     <--===           NC2164.2
210600     MOVE   "INS-TEST-F3-39" TO PAR-NAME.                         NC2164.2
210700     MOVE   "VI-93 6.17.2" TO ANSI-REFERENCE.                     NC2164.2
210800     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
210900     MOVE "ABESSA YE TUTCGW H S" TO TEST-39-DATA.                 NC2164.2
211000 INS-TEST-F3-39-0.                                                NC2164.2
211100     INSPECT TEST-39-DATA                                         NC2164.2
211200             TALLYING   WRK-DU-999-1                              NC2164.2
211300             FOR        LEADING "B"                               NC2164.2
211400                        LEADING WS-E                              NC2164.2
211500                        BEFORE  "C"                               NC2164.2
211600             REPLACING                                            NC2164.2
211700             CHARACTERS BY   "A" AFTER "X" BEFORE "Y"             NC2164.2
211800             CHARACTERS BY SPACE AFTER "G" BEFORE "H".            NC2164.2
211900 INS-TEST-F3-39-1.                                                NC2164.2
212000     IF      WRK-DU-999-1 = 0                                     NC2164.2
212100             PERFORM PASS                                         NC2164.2
212200             GO TO   INS-WRITE-F3-39-1.                           NC2164.2
212300     GO TO   INS-FAIL-F3-39-1.                                    NC2164.2
212400 INS-DELETE-F3-39-1.                                              NC2164.2
212500     PERFORM DE-LETE.                                             NC2164.2
212600     GO TO   INS-WRITE-F3-39-1.                                   NC2164.2
212700 INS-FAIL-F3-39-1.                                                NC2164.2
212800     PERFORM FAIL.                                                NC2164.2
212900     MOVE    WRK-DU-999-1 TO COMPUTED-N.                          NC2164.2
213000     MOVE    0            TO CORRECT-N.                           NC2164.2
213100 INS-WRITE-F3-39-1.                                               NC2164.2
213200     PERFORM PRINT-DETAIL.                                        NC2164.2
213300*                                                                 NC2164.2
213400 INS-INIT-F4-40.                                                  NC2164.2
213500*    ===-->        INSPECT CONVERTING            <--===           NC2164.2
213600     MOVE   "INS-TEST-F4-40" TO PAR-NAME.                         NC2164.2
213700     MOVE   "VI-943" TO ANSI-REFERENCE.                           NC2164.2
213800     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
213900     MOVE "GADQAUZTABAGA" TO TEST-40-DATA.                        NC2164.2
214000 INS-TEST-F4-40-0.                                                NC2164.2
214100     INSPECT TEST-40-DATA                                         NC2164.2
214200             CONVERTING "AU" TO "23" BEFORE "B" AFTER "Q".        NC2164.2
214300     GO TO   INS-TEST-F4-40-1.                                    NC2164.2
214400 INS-DELETE-F4-40.                                                NC2164.2
214500     PERFORM DE-LETE.                                             NC2164.2
214600     PERFORM PRINT-DETAIL.                                        NC2164.2
214700     GO TO   INS-INIT-F4-41.                                      NC2164.2
214800 INS-TEST-F4-40-1.                                                NC2164.2
214900     IF      TEST-40-DATA = "GADQ23ZT2BAGA"                       NC2164.2
215000             PERFORM PASS                                         NC2164.2
215100             GO TO INS-WRITE-F4-40-1                              NC2164.2
215200     ELSE                                                         NC2164.2
215300             GO TO INS-FAIL-F4-40-1.                              NC2164.2
215400 INS-DELETE-F4-40-1.                                              NC2164.2
215500     PERFORM DE-LETE.                                             NC2164.2
215600     GO TO INS-WRITE-F4-40-1.                                     NC2164.2
215700 INS-FAIL-F4-40-1.                                                NC2164.2
215800     MOVE   "GADQ23ZT2BAGA" TO CORRECT-A                          NC2164.2
215900     MOVE    TEST-40-DATA TO COMPUTED-A                           NC2164.2
216000     PERFORM FAIL.                                                NC2164.2
216100 INS-WRITE-F4-40-1.                                               NC2164.2
216200     PERFORM PRINT-DETAIL.                                        NC2164.2
216300 INS-INIT-F4-41.                                                  NC2164.2
216400*    ===-->        INSPECT CONVERTING            <--===           NC2164.2
216500     MOVE   "INS-TEST-F4-41" TO PAR-NAME.                         NC2164.2
216600     MOVE   "VI-943" TO ANSI-REFERENCE.                           NC2164.2
216700     MOVE   "GADQ23ZT2BAGA"  TO TEST-40-DATA.                     NC2164.2
216800     MOVE   "DF" TO XN-DF.                                        NC2164.2
216900     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
217000 INS-TEST-F4-41-0.                                                NC2164.2
217100     INSPECT TEST-40-DATA                                         NC2164.2
217200             CONVERTING XN-DF TO "45".                            NC2164.2
217300     GO TO   INS-TEST-F4-41-1.                                    NC2164.2
217400 INS-DELETE-F4-41.                                                NC2164.2
217500     PERFORM DE-LETE.                                             NC2164.2
217600     PERFORM PRINT-DETAIL.                                        NC2164.2
217700     GO TO   INS-INIT-F4-42.                                      NC2164.2
217800 INS-TEST-F4-41-1.                                                NC2164.2
217900     IF      TEST-40-DATA = "GA4Q23ZT2BAGA"                       NC2164.2
218000             PERFORM PASS                                         NC2164.2
218100             GO TO INS-WRITE-F4-41-1                              NC2164.2
218200     ELSE                                                         NC2164.2
218300             GO TO INS-FAIL-F4-41-1.                              NC2164.2
218400 INS-DELETE-F4-41-1.                                              NC2164.2
218500     PERFORM DE-LETE.                                             NC2164.2
218600     GO TO INS-WRITE-F4-41-1.                                     NC2164.2
218700 INS-FAIL-F4-41-1.                                                NC2164.2
218800     MOVE   "GA4Q23ZT2BAGA" TO CORRECT-A                          NC2164.2
218900     MOVE    TEST-40-DATA TO COMPUTED-A                           NC2164.2
219000     PERFORM FAIL.                                                NC2164.2
219100 INS-WRITE-F4-41-1.                                               NC2164.2
219200     PERFORM PRINT-DETAIL.                                        NC2164.2
219300*                                                                 NC2164.2
219400 INS-INIT-F4-42.                                                  NC2164.2
219500*    ===-->        INSPECT CONVERTING            <--===           NC2164.2
219600     MOVE   "INS-TEST-F4-42" TO PAR-NAME.                         NC2164.2
219700     MOVE   "VI-943" TO ANSI-REFERENCE.                           NC2164.2
219800     MOVE    ZERO TO WRK-DU-999-1.                                NC2164.2
219900     MOVE   "GA4Q23ZT2BAGA" TO TEST-40-DATA.                      NC2164.2
220000     MOVE   "67" TO XN-67.                                        NC2164.2
220100 INS-TEST-F4-42-0.                                                NC2164.2
220200     INSPECT TEST-40-DATA                                         NC2164.2
220300             CONVERTING  "GA" TO XN-67 BEFORE XN-B.               NC2164.2
220400     GO TO   INS-TEST-F4-42-1.                                    NC2164.2
220500 INS-DELETE-F4-42.                                                NC2164.2
220600     PERFORM DE-LETE.                                             NC2164.2
220700     PERFORM PRINT-DETAIL.                                        NC2164.2
220800     GO TO   CCVS-EXIT.                                           NC2164.2
220900 INS-TEST-F4-42-1.                                                NC2164.2
221000     IF      TEST-40-DATA = "674Q23ZT2BAGA"                       NC2164.2
221100             PERFORM PASS                                         NC2164.2
221200             GO TO INS-WRITE-F4-42-1                              NC2164.2
221300     ELSE                                                         NC2164.2
221400             GO TO INS-FAIL-F4-42-1.                              NC2164.2
221500 INS-DELETE-F4-42-1.                                              NC2164.2
221600     PERFORM DE-LETE.                                             NC2164.2
221700     GO TO INS-WRITE-F4-42-1.                                     NC2164.2
221800 INS-FAIL-F4-42-1.                                                NC2164.2
221900     MOVE   "674Q23ZT2BAGA" TO CORRECT-A                          NC2164.2
222000     MOVE    TEST-40-DATA TO COMPUTED-A                           NC2164.2
222100     PERFORM FAIL.                                                NC2164.2
222200 INS-WRITE-F4-42-1.                                               NC2164.2
222300     PERFORM PRINT-DETAIL.                                        NC2164.2
222400*                                                                 NC2164.2
222500 CCVS-EXIT SECTION.                                               NC2164.2
222600 CCVS-999999.                                                     NC2164.2
222700     GO TO CLOSE-FILES.                                           NC2164.2
      *END-OF,NC216A                                                                  
