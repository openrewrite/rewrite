      *HEADER,COBOL,RL116A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1164.2
000200 PROGRAM-ID.                                                      RL1164.2
000300     RL116A.                                                      RL1164.2
000400****************************************************************  RL1164.2
000500*                                                              *  RL1164.2
000600*    VALIDATION FOR:-                                          *  RL1164.2
000700*                                                              *  RL1164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1164.2
000900*                                                              *  RL1164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1164.2
001100*                                                              *  RL1164.2
001200****************************************************************  RL1164.2
001300*                                                              *  RL1164.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1164.2
001500*                                                              *  RL1164.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  RL1164.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  RL1164.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  RL1164.2
001900*                                                              *  RL1164.2
002000****************************************************************  RL1164.2
002100*                                                              *  RL1164.2
002200*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1164.2
002300*                                                              *  RL1164.2
002400*            X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1164.2
002500*                    RELATIVE  I-O DATA FILE                   *  RL1164.2
002600*            X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1164.2
002700*                    RELATIVE  I-O DATA FILE                   *  RL1164.2
002800*            X-55   SYSTEM PRINTER                             *  RL1164.2
002900*            X-69   ADDITIONAL VALUE OF CLAUSES                *  RL1164.2
003000*            X-74   VALUE OF IMPLEMENTOR-NAME                  *  RL1164.2
003100*            X-75   OBJECT OF VALUE OF CLAUSE                  *  RL1164.2
003200*            X-82   SOURCE-COMPUTER                            *  RL1164.2
003300*            X-83   OBJECT-COMPUTER.                           *  RL1164.2
003400*                                                              *  RL1164.2
003500****************************************************************  RL1164.2
003600*    RL116A                                                    *  RL1164.2
003700****************************************************************  RL1164.2
003800*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND    RL1164.2
003900*            SEMANTIC ACTIONS ASSOCIATED WITH THE "STATUS"        RL1164.2
004000*            CLAUSE FOR THE VALUES "00" AND "10".                 RL1164.2
004100*                                                                 RL1164.2
004200*                                                                 RL1164.2
004300*                                                                 RL1164.2
004400***************************************************               RL1164.2
004500 ENVIRONMENT DIVISION.                                            RL1164.2
004600 CONFIGURATION SECTION.                                           RL1164.2
004700 SOURCE-COMPUTER.                                                 RL1164.2
004800     XXXXX082.                                                    RL1164.2
004900 OBJECT-COMPUTER.                                                 RL1164.2
005000     XXXXX083.                                                    RL1164.2
005100 INPUT-OUTPUT SECTION.                                            RL1164.2
005200 FILE-CONTROL.                                                    RL1164.2
005300     SELECT PRINT-FILE ASSIGN TO                                  RL1164.2
005400     XXXXX055.                                                    RL1164.2
005500     SELECT   RL-FD2 ASSIGN                                       RL1164.2
005600     XXXXX022                                                     RL1164.2
005700              ORGANIZATION RELATIVE                               RL1164.2
005800              ACCESS  RANDOM                                      RL1164.2
005900              RELATIVE  RL-FD2-KEY                                RL1164.2
006000         FILE STATUS IS RL-FD2-STATUS.                            RL1164.2
006100     SELECT   RL-FD3 ASSIGN                                       RL1164.2
006200     XXXXX022                                                     RL1164.2
006300              ORGANIZATION RELATIVE                               RL1164.2
006400              ACCESS  SEQUENTIAL                                  RL1164.2
006500              RELATIVE  RL-FD3-KEY                                RL1164.2
006600         FILE STATUS IS RL-FD3-STATUS.                            RL1164.2
006700 DATA DIVISION.                                                   RL1164.2
006800 FILE SECTION.                                                    RL1164.2
006900 FD  PRINT-FILE.                                                  RL1164.2
007000 01  PRINT-REC PICTURE X(120).                                    RL1164.2
007100 01  DUMMY-RECORD PICTURE X(120).                                 RL1164.2
007200 FD  RL-FD2                                                       RL1164.2
007300C    VALUE OF                                                     RL1164.2
007400C    XXXXX074                                                     RL1164.2
007500C    IS                                                           RL1164.2
007600C    XXXXX076                                                     RL1164.2
007700*    XXXXX069                                                     RL1164.2
007800     LABEL RECORDS ARE STANDARD                                   RL1164.2
007900     BLOCK CONTAINS 1 RECORDS                                     RL1164.2
008000     DATA RECORD RL-FD2R1-F-G-240.                                RL1164.2
008100 01  RL-FD2R1-F-G-240.                                            RL1164.2
008200     05 RL-FD2-WRK-120 PIC X(120).                                RL1164.2
008300     05 RL-FD2-GRP-120.                                           RL1164.2
008400        10 RL-FD2-WRK-XN-0001-O120F                               RL1164.2
008500                        PICTURE X OCCURS 120 TIMES.               RL1164.2
008600 FD  RL-FD3                                                       RL1164.2
008700C    VALUE OF                                                     RL1164.2
008800C    XXXXX074                                                     RL1164.2
008900C    IS                                                           RL1164.2
009000C    XXXXX076                                                     RL1164.2
009100*    XXXXX069                                                     RL1164.2
009200     LABEL RECORDS ARE STANDARD                                   RL1164.2
009300     BLOCK CONTAINS 1 RECORDS                                     RL1164.2
009400     DATA RECORD RL-FD3R1-F-G-240.                                RL1164.2
009500 01  RL-FD3R1-F-G-240.                                            RL1164.2
009600     05 RL-FD3-WRK-120 PIC X(120).                                RL1164.2
009700     05 RL-FD3-GRP-130.                                           RL1164.2
009800        10 RL-FD3-WRK-XN-0001-O120F                               RL1164.2
009900                        PICTURE X OCCURS 120 TIMES.               RL1164.2
010000 WORKING-STORAGE SECTION.                                         RL1164.2
010100 01  GRP-0001.                                                    RL1164.2
010200     05 RL-FD2-KEY   PIC 9(8)  VALUE ZERO.                        RL1164.2
010300     05 RL-FD3-KEY   PIC 9(8)  VALUE ZERO.                        RL1164.2
010400     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
010500     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
010600     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
010700     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
010800     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
010900     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
011000     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1164.2
011100     05 RL-FD2-STATUS     PIC  XX  VALUE  SPACE.                  RL1164.2
011200     05 RL-FD3-STATUS     PIC  XX  VALUE  SPACE.                  RL1164.2
011300     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1164.2
011400     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1164.2
011500     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1164.2
011600     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1164.2
011700     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1164.2
011800     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1164.2
011900     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1164.2
012000     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1164.2
012100     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1164.2
012200 01  FILE-RECORD-INFORMATION-REC.                                 RL1164.2
012300     03 FILE-RECORD-INFO-SKELETON.                                RL1164.2
012400        05 FILLER                 PICTURE X(48)       VALUE       RL1164.2
012500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1164.2
012600        05 FILLER                 PICTURE X(46)       VALUE       RL1164.2
012700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1164.2
012800        05 FILLER                 PICTURE X(26)       VALUE       RL1164.2
012900             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1164.2
013000        05 FILLER                 PICTURE X(37)       VALUE       RL1164.2
013100             ",RECKEY=                             ".             RL1164.2
013200        05 FILLER                 PICTURE X(38)       VALUE       RL1164.2
013300             ",ALTKEY1=                             ".            RL1164.2
013400        05 FILLER                 PICTURE X(38)       VALUE       RL1164.2
013500             ",ALTKEY2=                             ".            RL1164.2
013600        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1164.2
013700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1164.2
013800        05 FILE-RECORD-INFO-P1-120.                               RL1164.2
013900           07 FILLER              PIC X(5).                       RL1164.2
014000           07 XFILE-NAME           PIC X(6).                      RL1164.2
014100           07 FILLER              PIC X(8).                       RL1164.2
014200           07 XRECORD-NAME         PIC X(6).                      RL1164.2
014300           07 FILLER              PIC X(1).                       RL1164.2
014400           07 REELUNIT-NUMBER     PIC 9(1).                       RL1164.2
014500           07 FILLER              PIC X(7).                       RL1164.2
014600           07 XRECORD-NUMBER       PIC 9(6).                      RL1164.2
014700           07 FILLER              PIC X(6).                       RL1164.2
014800           07 UPDATE-NUMBER       PIC 9(2).                       RL1164.2
014900           07 FILLER              PIC X(5).                       RL1164.2
015000           07 ODO-NUMBER          PIC 9(4).                       RL1164.2
015100           07 FILLER              PIC X(5).                       RL1164.2
015200           07 XPROGRAM-NAME        PIC X(5).                      RL1164.2
015300           07 FILLER              PIC X(7).                       RL1164.2
015400           07 XRECORD-LENGTH       PIC 9(6).                      RL1164.2
015500           07 FILLER              PIC X(7).                       RL1164.2
015600           07 CHARS-OR-RECORDS    PIC X(2).                       RL1164.2
015700           07 FILLER              PIC X(1).                       RL1164.2
015800           07 XBLOCK-SIZE          PIC 9(4).                      RL1164.2
015900           07 FILLER              PIC X(6).                       RL1164.2
016000           07 RECORDS-IN-FILE     PIC 9(6).                       RL1164.2
016100           07 FILLER              PIC X(5).                       RL1164.2
016200           07 XFILE-ORGANIZATION   PIC X(2).                      RL1164.2
016300           07 FILLER              PIC X(6).                       RL1164.2
016400           07 XLABEL-TYPE          PIC X(1).                      RL1164.2
016500        05 FILE-RECORD-INFO-P121-240.                             RL1164.2
016600           07 FILLER              PIC X(8).                       RL1164.2
016700           07 XRECORD-KEY          PIC X(29).                     RL1164.2
016800           07 FILLER              PIC X(9).                       RL1164.2
016900           07 ALTERNATE-KEY1      PIC X(29).                      RL1164.2
017000           07 FILLER              PIC X(9).                       RL1164.2
017100           07 ALTERNATE-KEY2      PIC X(29).                      RL1164.2
017200           07 FILLER              PIC X(7).                       RL1164.2
017300 01  TEST-RESULTS.                                                RL1164.2
017400     02 FILLER                   PIC X      VALUE SPACE.          RL1164.2
017500     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1164.2
017600     02 FILLER                   PIC X      VALUE SPACE.          RL1164.2
017700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1164.2
017800     02 FILLER                   PIC X      VALUE SPACE.          RL1164.2
017900     02  PAR-NAME.                                                RL1164.2
018000       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1164.2
018100       03  PARDOT-X              PIC X      VALUE SPACE.          RL1164.2
018200       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1164.2
018300     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1164.2
018400     02 RE-MARK                  PIC X(61).                       RL1164.2
018500 01  TEST-COMPUTED.                                               RL1164.2
018600     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1164.2
018700     02 FILLER                   PIC X(17)  VALUE                 RL1164.2
018800            "       COMPUTED=".                                   RL1164.2
018900     02 COMPUTED-X.                                               RL1164.2
019000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1164.2
019100     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1164.2
019200                                 PIC -9(9).9(9).                  RL1164.2
019300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1164.2
019400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1164.2
019500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1164.2
019600     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1164.2
019700         04 COMPUTED-18V0                    PIC -9(18).          RL1164.2
019800         04 FILLER                           PIC X.               RL1164.2
019900     03 FILLER PIC X(50) VALUE SPACE.                             RL1164.2
020000 01  TEST-CORRECT.                                                RL1164.2
020100     02 FILLER PIC X(30) VALUE SPACE.                             RL1164.2
020200     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1164.2
020300     02 CORRECT-X.                                                RL1164.2
020400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1164.2
020500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1164.2
020600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1164.2
020700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1164.2
020800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1164.2
020900     03      CR-18V0 REDEFINES CORRECT-A.                         RL1164.2
021000         04 CORRECT-18V0                     PIC -9(18).          RL1164.2
021100         04 FILLER                           PIC X.               RL1164.2
021200     03 FILLER PIC X(2) VALUE SPACE.                              RL1164.2
021300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1164.2
021400 01  CCVS-C-1.                                                    RL1164.2
021500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1164.2
021600-    "SS  PARAGRAPH-NAME                                          RL1164.2
021700-    "       REMARKS".                                            RL1164.2
021800     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1164.2
021900 01  CCVS-C-2.                                                    RL1164.2
022000     02 FILLER                     PIC X        VALUE SPACE.      RL1164.2
022100     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1164.2
022200     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1164.2
022300     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1164.2
022400     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1164.2
022500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1164.2
022600 01  REC-CT                        PIC 99       VALUE ZERO.       RL1164.2
022700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1164.2
022800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1164.2
022900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1164.2
023000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1164.2
023100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1164.2
023200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1164.2
023300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1164.2
023400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1164.2
023500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1164.2
023600 01  CCVS-H-1.                                                    RL1164.2
023700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1164.2
023800     02  FILLER                    PIC X(42)    VALUE             RL1164.2
023900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1164.2
024000     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1164.2
024100 01  CCVS-H-2A.                                                   RL1164.2
024200   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1164.2
024300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1164.2
024400   02  FILLER                        PIC XXXX   VALUE             RL1164.2
024500     "4.2 ".                                                      RL1164.2
024600   02  FILLER                        PIC X(28)  VALUE             RL1164.2
024700            " COPY - NOT FOR DISTRIBUTION".                       RL1164.2
024800   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1164.2
024900                                                                  RL1164.2
025000 01  CCVS-H-2B.                                                   RL1164.2
025100   02  FILLER                        PIC X(15)  VALUE             RL1164.2
025200            "TEST RESULT OF ".                                    RL1164.2
025300   02  TEST-ID                       PIC X(9).                    RL1164.2
025400   02  FILLER                        PIC X(4)   VALUE             RL1164.2
025500            " IN ".                                               RL1164.2
025600   02  FILLER                        PIC X(12)  VALUE             RL1164.2
025700     " HIGH       ".                                              RL1164.2
025800   02  FILLER                        PIC X(22)  VALUE             RL1164.2
025900            " LEVEL VALIDATION FOR ".                             RL1164.2
026000   02  FILLER                        PIC X(58)  VALUE             RL1164.2
026100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1164.2
026200 01  CCVS-H-3.                                                    RL1164.2
026300     02  FILLER                      PIC X(34)  VALUE             RL1164.2
026400            " FOR OFFICIAL USE ONLY    ".                         RL1164.2
026500     02  FILLER                      PIC X(58)  VALUE             RL1164.2
026600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1164.2
026700     02  FILLER                      PIC X(28)  VALUE             RL1164.2
026800            "  COPYRIGHT   1985 ".                                RL1164.2
026900 01  CCVS-E-1.                                                    RL1164.2
027000     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1164.2
027100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1164.2
027200     02 ID-AGAIN                     PIC X(9).                    RL1164.2
027300     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1164.2
027400 01  CCVS-E-2.                                                    RL1164.2
027500     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1164.2
027600     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1164.2
027700     02 CCVS-E-2-2.                                               RL1164.2
027800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1164.2
027900         03 FILLER                   PIC X      VALUE SPACE.      RL1164.2
028000         03 ENDER-DESC               PIC X(44)  VALUE             RL1164.2
028100            "ERRORS ENCOUNTERED".                                 RL1164.2
028200 01  CCVS-E-3.                                                    RL1164.2
028300     02  FILLER                      PIC X(22)  VALUE             RL1164.2
028400            " FOR OFFICIAL USE ONLY".                             RL1164.2
028500     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1164.2
028600     02  FILLER                      PIC X(58)  VALUE             RL1164.2
028700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1164.2
028800     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1164.2
028900     02 FILLER                       PIC X(15)  VALUE             RL1164.2
029000             " COPYRIGHT 1985".                                   RL1164.2
029100 01  CCVS-E-4.                                                    RL1164.2
029200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1164.2
029300     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1164.2
029400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1164.2
029500     02 FILLER                       PIC X(40)  VALUE             RL1164.2
029600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1164.2
029700 01  XXINFO.                                                      RL1164.2
029800     02 FILLER                       PIC X(19)  VALUE             RL1164.2
029900            "*** INFORMATION ***".                                RL1164.2
030000     02 INFO-TEXT.                                                RL1164.2
030100       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1164.2
030200       04 XXCOMPUTED                 PIC X(20).                   RL1164.2
030300       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1164.2
030400       04 XXCORRECT                  PIC X(20).                   RL1164.2
030500     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1164.2
030600 01  HYPHEN-LINE.                                                 RL1164.2
030700     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1164.2
030800     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1164.2
030900-    "*****************************************".                 RL1164.2
031000     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1164.2
031100-    "******************************".                            RL1164.2
031200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1164.2
031300     "RL116A".                                                    RL1164.2
031400 PROCEDURE DIVISION.                                              RL1164.2
031500 DECLARATIVES.                                                    RL1164.2
031600 RL-FD2-01 SECTION.                                               RL1164.2
031700     USE     AFTER ERROR PROCEDURE I-O.                           RL1164.2
031800 RL-FD2-01-01.                                                    RL1164.2
031900     MOVE   "PASS " TO P-OR-F.                                    RL1164.2
032000     ADD     1 TO PASS-COUNTER.                                   RL1164.2
032100*                                                                 RL1164.2
032200     IF      REC-CT NOT EQUAL TO ZERO                             RL1164.2
032300             MOVE "." TO PARDOT-X                                 RL1164.2
032400             MOVE REC-CT TO DOTVALUE.                             RL1164.2
032500     MOVE    TEST-RESULTS TO PRINT-REC.                           RL1164.2
032600     PERFORM D1-WRITE-LINE THRU D1-WRITE-LINE-EXIT.               RL1164.2
032700     IF      P-OR-F EQUAL TO "FAIL*"                              RL1164.2
032800             PERFORM D1-WRITE-LINE THRU D1-WRITE-LINE-EXIT        RL1164.2
032900             PERFORM D1-FAIL-ROUTINE THRU D1-FAIL-ROUTINE-EX      RL1164.2
033000      ELSE                                                        RL1164.2
033100             PERFORM D1-BAIL-OUT THRU D1-BAIL-OUT-EX.             RL1164.2
033200     MOVE    SPACE TO P-OR-F.                                     RL1164.2
033300     MOVE    SPACE TO COMPUTED-X.                                 RL1164.2
033400     MOVE    SPACE TO CORRECT-X.                                  RL1164.2
033500     IF      REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.        RL1164.2
033600     MOVE    SPACE TO RE-MARK.                                    RL1164.2
033700     GO TO   RL-FD2-01-EXIT.                                      RL1164.2
033800 D1-FAIL-ROUTINE.                                                 RL1164.2
033900     IF      COMPUTED-X NOT EQUAL TO SPACE                        RL1164.2
034000             GO TO   D1-FAIL-ROUTINE-WRITE.                       RL1164.2
034100     IF      CORRECT-X NOT EQUAL TO SPACE                         RL1164.2
034200             GO TO   D1-FAIL-ROUTINE-WRITE.                       RL1164.2
034300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1164.2
034400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  RL1164.2
034500     MOVE    XXINFO TO DUMMY-RECORD.                              RL1164.2
034600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
034700     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
034800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
034900     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
035000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1164.2
035100     GO TO   D1-FAIL-ROUTINE-EX.                                  RL1164.2
035200 D1-FAIL-ROUTINE-WRITE.                                           RL1164.2
035300     MOVE    TEST-COMPUTED TO PRINT-REC.                          RL1164.2
035400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
035500     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
035600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                RL1164.2
035700     MOVE    TEST-CORRECT TO PRINT-REC.                           RL1164.2
035800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
035900     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
036000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
036100     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
036200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        RL1164.2
036300 D1-FAIL-ROUTINE-EX.                                              RL1164.2
036400     EXIT.                                                        RL1164.2
036500 D1-BAIL-OUT.                                                     RL1164.2
036600     IF      COMPUTED-A NOT EQUAL TO SPACE                        RL1164.2
036700             GO TO    D1-BAIL-OUT-WRITE.                          RL1164.2
036800     IF      CORRECT-A EQUAL TO SPACE                             RL1164.2
036900             GO TO    D1-BAIL-OUT-EX.                             RL1164.2
037000 D1-BAIL-OUT-WRITE.                                               RL1164.2
037100     MOVE    CORRECT-A  TO XXCORRECT.                             RL1164.2
037200     MOVE    COMPUTED-A TO XXCOMPUTED.                            RL1164.2
037300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1164.2
037400     MOVE    XXINFO TO DUMMY-RECORD.                              RL1164.2
037500     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
037600     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
037700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
037800     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
037900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1164.2
038000 D1-BAIL-OUT-EX.                                                  RL1164.2
038100     EXIT.                                                        RL1164.2
038200 D1-WRITE-LINE.                                                   RL1164.2
038300     ADD     1 TO RECORD-COUNT.                                   RL1164.2
038400*    IF      RECORD-COUNT GREATER 50                              RL1164.2
038500*            MOVE    DUMMY-RECORD TO DUMMY-HOLD                   RL1164.2
038600*            MOVE    SPACE TO DUMMY-RECORD                        RL1164.2
038700*            WRITE   DUMMY-RECORD AFTER ADVANCING PAGE            RL1164.2
038800*            MOVE    CCVS-C-1 TO DUMMY-RECORD                     RL1164.2
038900*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1164.2
039000*            MOVE    SPACE TO DUMMY-RECORD                        RL1164.2
039100*            MOVE    CCVS-C-2 TO DUMMY-RECORD                     RL1164.2
039200*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1164.2
039300*            MOVE    SPACE TO DUMMY-RECORD                        RL1164.2
039400*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1164.2
039500*            MOVE    SPACE TO DUMMY-RECORD                        RL1164.2
039600*            MOVE    HYPHEN-LINE TO DUMMY-RECORD                  RL1164.2
039700*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1164.2
039800*            MOVE    SPACE TO DUMMY-RECORD                        RL1164.2
039900*            MOVE    DUMMY-HOLD TO DUMMY-RECORD                   RL1164.2
040000*            MOVE    ZERO TO RECORD-COUNT.                        RL1164.2
040100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1164.2
040200     MOVE    SPACE TO DUMMY-RECORD.                               RL1164.2
040300 D1-WRITE-LINE-EXIT.                                              RL1164.2
040400     EXIT.                                                        RL1164.2
040500 RL-FD2-01-EXIT.                                                  RL1164.2
040600     EXIT.                                                        RL1164.2
040700 END DECLARATIVES.                                                RL1164.2
040800 CCVS1 SECTION.                                                   RL1164.2
040900 OPEN-FILES.                                                      RL1164.2
041000     OPEN    OUTPUT PRINT-FILE.                                   RL1164.2
041100     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1164.2
041200     MOVE    SPACE TO TEST-RESULTS.                               RL1164.2
041300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1164.2
041400     MOVE    ZERO TO REC-SKL-SUB.                                 RL1164.2
041500     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1164.2
041600 CCVS-INIT-FILE.                                                  RL1164.2
041700     ADD     1 TO REC-SKL-SUB.                                    RL1164.2
041800     MOVE    FILE-RECORD-INFO-SKELETON                            RL1164.2
041900          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1164.2
042000 CCVS-INIT-EXIT.                                                  RL1164.2
042100     GO TO CCVS1-EXIT.                                            RL1164.2
042200 CLOSE-FILES.                                                     RL1164.2
042300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1164.2
042400 TERMINATE-CCVS.                                                  RL1164.2
042500*    EXIT PROGRAM.                                                RL1164.2
042600*TERMINATE-CALL.                                                  RL1164.2
042700     STOP     RUN.                                                RL1164.2
042800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1164.2
042900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1164.2
043000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1164.2
043100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1164.2
043200     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1164.2
043300 PRINT-DETAIL.                                                    RL1164.2
043400     IF REC-CT NOT EQUAL TO ZERO                                  RL1164.2
043500             MOVE "." TO PARDOT-X                                 RL1164.2
043600             MOVE REC-CT TO DOTVALUE.                             RL1164.2
043700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1164.2
043800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1164.2
043900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1164.2
044000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1164.2
044100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1164.2
044200     MOVE SPACE TO CORRECT-X.                                     RL1164.2
044300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1164.2
044400     MOVE     SPACE TO RE-MARK.                                   RL1164.2
044500 HEAD-ROUTINE.                                                    RL1164.2
044600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1164.2
044700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1164.2
044800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1164.2
044900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1164.2
045000 COLUMN-NAMES-ROUTINE.                                            RL1164.2
045100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1164.2
045200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1164.2
045300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1164.2
045400 END-ROUTINE.                                                     RL1164.2
045500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1164.2
045600 END-RTN-EXIT.                                                    RL1164.2
045700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1164.2
045800 END-ROUTINE-1.                                                   RL1164.2
045900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1164.2
046000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1164.2
046100      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1164.2
046200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1164.2
046300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1164.2
046400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1164.2
046500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1164.2
046600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1164.2
046700  END-ROUTINE-12.                                                 RL1164.2
046800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1164.2
046900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1164.2
047000         MOVE "NO " TO ERROR-TOTAL                                RL1164.2
047100         ELSE                                                     RL1164.2
047200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1164.2
047300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1164.2
047400     PERFORM WRITE-LINE.                                          RL1164.2
047500 END-ROUTINE-13.                                                  RL1164.2
047600     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1164.2
047700         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1164.2
047800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1164.2
047900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1164.2
048000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1164.2
048100      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1164.2
048200          MOVE "NO " TO ERROR-TOTAL                               RL1164.2
048300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1164.2
048400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1164.2
048500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1164.2
048600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1164.2
048700 WRITE-LINE.                                                      RL1164.2
048800     ADD 1 TO RECORD-COUNT.                                       RL1164.2
048900*    IF RECORD-COUNT GREATER 50                                   RL1164.2
049000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1164.2
049100*        MOVE SPACE TO DUMMY-RECORD                               RL1164.2
049200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1164.2
049300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1164.2
049400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1164.2
049500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1164.2
049600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1164.2
049700*        MOVE ZERO TO RECORD-COUNT.                               RL1164.2
049800     PERFORM WRT-LN.                                              RL1164.2
049900 WRT-LN.                                                          RL1164.2
050000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1164.2
050100     MOVE SPACE TO DUMMY-RECORD.                                  RL1164.2
050200 BLANK-LINE-PRINT.                                                RL1164.2
050300     PERFORM WRT-LN.                                              RL1164.2
050400 FAIL-ROUTINE.                                                    RL1164.2
050500     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1164.2
050600            GO TO   FAIL-ROUTINE-WRITE.                           RL1164.2
050700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1164.2
050800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1164.2
050900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1164.2
051000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1164.2
051100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1164.2
051200     GO TO  FAIL-ROUTINE-EX.                                      RL1164.2
051300 FAIL-ROUTINE-WRITE.                                              RL1164.2
051400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1164.2
051500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1164.2
051600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1164.2
051700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1164.2
051800 FAIL-ROUTINE-EX. EXIT.                                           RL1164.2
051900 BAIL-OUT.                                                        RL1164.2
052000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1164.2
052100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1164.2
052200 BAIL-OUT-WRITE.                                                  RL1164.2
052300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1164.2
052400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1164.2
052500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1164.2
052600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1164.2
052700 BAIL-OUT-EX. EXIT.                                               RL1164.2
052800 CCVS1-EXIT.                                                      RL1164.2
052900     EXIT.                                                        RL1164.2
053000 SECT-RL116A-001 SECTION.                                         RL1164.2
053100 REL-INIT-009.                                                    RL1164.2
053200     MOVE    "VIII-36 4.8.4 GR4" TO ANSI-REFERENCE.               RL1164.2
053300     MOVE    "REL-TEST-009" TO PAR-NAME.                          RL1164.2
053400     MOVE     "CREATE RL-FD2" TO  FEATURE                         RL1164.2
053500     MOVE     "RL-FD2"        TO  XFILE-NAME (2).                 RL1164.2
053600     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL1164.2
053700     MOVE CCVS-PGM-ID             TO  XPROGRAM-NAME (2).          RL1164.2
053800     MOVE     000240          TO  XRECORD-LENGTH (2).             RL1164.2
053900     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL1164.2
054000     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL1164.2
054100     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL1164.2
054200     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL1164.2
054300     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL1164.2
054400     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL1164.2
054500*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1164.2
054600     MOVE     1         TO  WRK-CS-09V00-012.                     RL1164.2
054700     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1164.2
054800                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1164.2
054900                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1164.2
055000     MOVE     SPACE     TO  RL-FD2-STATUS.                        RL1164.2
055100     MOVE     90000002  TO  RL-FD2-KEY.                           RL1164.2
055200     MOVE     01 TO REC-CT.                                       RL1164.2
055300     OPEN     OUTPUT    RL-FD2.                                   RL1164.2
055400     MOVE     RL-FD2-STATUS TO WRK-XN-0002-001.                   RL1164.2
055500*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL1164.2
055600 REL-INIT-1.                                                      RL1164.2
055700     MOVE   "REL-TEST-1" TO PAR-NAME.                             RL1164.2
055800     MOVE   "VII-3 1.3.4 1A" TO ANSI-REFERENCE.                   RL1164.2
055900     MOVE    XRECORD-NUMBER (2) TO RL-FD2-KEY.                    RL1164.2
056000     MOVE   "99"  TO RL-FD2-STATUS.                               RL1164.2
056100     MOVE    FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.       RL1164.2
056200     MOVE    FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-GRP-120.       RL1164.2
056300     WRITE   RL-FD2R1-F-G-240                                     RL1164.2
056400            INVALID KEY CONTINUE.                                 RL1164.2
056500 REL-TEST-1.                                                      RL1164.2
056600     IF      RL-FD2-STATUS NOT EQUAL TO "00"                      RL1164.2
056700             MOVE "INVALID WRITE" TO RE-MARK                      RL1164.2
056800             MOVE RL-FD2-STATUS TO COMPUTED-X                     RL1164.2
056900             MOVE "00" TO CORRECT-X                               RL1164.2
057000             PERFORM FAIL                                         RL1164.2
057100             PERFORM PRINT-DETAIL                                 RL1164.2
057200     ELSE                                                         RL1164.2
057300             PERFORM PASS                                         RL1164.2
057400             PERFORM PRINT-DETAIL.                                RL1164.2
057500*                                                                 RL1164.2
057600 REL-INIT-2.                                                      RL1164.2
057700     MOVE   "REL-TEST-2" TO PAR-NAME.                             RL1164.2
057800     MOVE   "VII-3 1.3.4 2A" TO ANSI-REFERENCE.                   RL1164.2
057900     IF      RL-FD2-STATUS NOT = "00"                             RL1164.2
058000             MOVE  "TEST-2 NOT PERFORMED DUE TO FAILURE OF TEST-1"RL1164.2
058100                  TO RE-MARK                                      RL1164.2
058200             PERFORM FAIL                                         RL1164.2
058300             PERFORM PRINT-DETAIL                                 RL1164.2
058400             GO TO   REL-TEST-2-EXIT.                             RL1164.2
058500     CLOSE   RL-FD2.                                              RL1164.2
058600     OPEN    INPUT RL-FD3.                                        RL1164.2
058700     MOVE 1 TO RL-FD3-KEY.                                        RL1164.2
058800 REL-TEST-2-0.                                                    RL1164.2
058900     READ    RL-FD3                                               RL1164.2
059000             AT END GO TO REL-TEST-2-1.                           RL1164.2
059100     GO TO   REL-TEST-2-0.                                        RL1164.2
059200 REL-TEST-2-1.                                                    RL1164.2
059300     IF      RL-FD3-STATUS NOT = "10"                             RL1164.2
059400             MOVE    RL-FD3-STATUS TO COMPUTED-X                  RL1164.2
059500             MOVE   "10" TO CORRECT-X                             RL1164.2
059600             PERFORM FAIL                                         RL1164.2
059700             PERFORM PRINT-DETAIL                                 RL1164.2
059800     ELSE                                                         RL1164.2
059900             PERFORM PASS                                         RL1164.2
060000             PERFORM PRINT-DETAIL.                                RL1164.2
060100     CLOSE   RL-FD3                                               RL1164.2
060200     IF      RL-FD3-STATUS NOT EQUAL TO "00"                      RL1164.2
060300             MOVE "CLOSE/STATUS" TO RE-MARK                       RL1164.2
060400             MOVE  RL-FD3-STATUS TO  COMPUTED-A                   RL1164.2
060500             MOVE "00" TO CORRECT-A                               RL1164.2
060600             PERFORM FAIL                                         RL1164.2
060700     ELSE                                                         RL1164.2
060800             PERFORM PASS.                                        RL1164.2
060900     PERFORM PRINT-DETAIL.                                        RL1164.2
061000 REL-TEST-2-EXIT.                                                 RL1164.2
061100*                                                                 RL1164.2
061200 CCVS-EXIT SECTION.                                               RL1164.2
061300 CCVS-999999.                                                     RL1164.2
061400     GO TO CLOSE-FILES.                                           RL1164.2
      *END-OF,RL116A                                                                  
