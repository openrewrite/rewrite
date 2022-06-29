      *HEADER,COBOL,RL117A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1174.2
000200 PROGRAM-ID.                                                      RL1174.2
000300     RL117A.                                                      RL1174.2
000400****************************************************************  RL1174.2
000500*                                                              *  RL1174.2
000600*    VALIDATION FOR:-                                          *  RL1174.2
000700*                                                              *  RL1174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1174.2
000900*                                                              *  RL1174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1174.2
001100*                                                              *  RL1174.2
001200****************************************************************  RL1174.2
001300*                                                              *  RL1174.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1174.2
001500*                                                              *  RL1174.2
001600*            X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1174.2
001700*                    RELATIVE  I-O DATA FILE                   *  RL1174.2
001800*            X-55   SYSTEM PRINTER                             *  RL1174.2
001900*            X-69   ADDITIONAL VALUE OF CLAUSES                *  RL1174.2
002000*            X-74   VALUE OF IMPLEMENTOR-NAME                  *  RL1174.2
002100*            X-75   OBJECT OF VALUE OF CLAUSE                  *  RL1174.2
002200*            X-82   SOURCE-COMPUTER                            *  RL1174.2
002300*            X-83   OBJECT-COMPUTER.                           *  RL1174.2
002400*                                                              *  RL1174.2
002500****************************************************************  RL1174.2
002600*    RL117A                                                    *  RL1174.2
002700****************************************************************  RL1174.2
002800*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND *  RL1174.2
002900*            SEMANTIC ACTIONS ASSOCIATED WITH THE "STATUS"     *  RL1174.2
003000*            CLAUSE FOR THE VALUES "10" AND "14".              *  RL1174.2
003100*                                                              *  RL1174.2
003200*                                                              *  RL1174.2
003300*                                                              *  RL1174.2
003400****************************************************************  RL1174.2
003500 ENVIRONMENT DIVISION.                                            RL1174.2
003600 CONFIGURATION SECTION.                                           RL1174.2
003700 SOURCE-COMPUTER.                                                 RL1174.2
003800     XXXXX082.                                                    RL1174.2
003900 OBJECT-COMPUTER.                                                 RL1174.2
004000     XXXXX083.                                                    RL1174.2
004100 INPUT-OUTPUT SECTION.                                            RL1174.2
004200 FILE-CONTROL.                                                    RL1174.2
004300     SELECT PRINT-FILE ASSIGN TO                                  RL1174.2
004400     XXXXX055.                                                    RL1174.2
004500     SELECT   RL-FD2 ASSIGN                                       RL1174.2
004600     XXXXX022                                                     RL1174.2
004700              ORGANIZATION RELATIVE                               RL1174.2
004800              ACCESS  RANDOM                                      RL1174.2
004900              RELATIVE  RL-FD2-KEY                                RL1174.2
005000         FILE STATUS IS RL-FD2-STATUS.                            RL1174.2
005100     SELECT   RL-FD3 ASSIGN                                       RL1174.2
005200     XXXXX022                                                     RL1174.2
005300              ORGANIZATION RELATIVE                               RL1174.2
005400              ACCESS  SEQUENTIAL                                  RL1174.2
005500              RELATIVE  RL-FD3-KEY                                RL1174.2
005600         FILE STATUS IS RL-FD3-STATUS.                            RL1174.2
005700 DATA DIVISION.                                                   RL1174.2
005800 FILE SECTION.                                                    RL1174.2
005900 FD  PRINT-FILE.                                                  RL1174.2
006000 01  PRINT-REC PICTURE X(120).                                    RL1174.2
006100 01  DUMMY-RECORD PICTURE X(120).                                 RL1174.2
006200 FD  RL-FD2                                                       RL1174.2
006300C    VALUE OF                                                     RL1174.2
006400C    XXXXX074                                                     RL1174.2
006500C    IS                                                           RL1174.2
006600C    XXXXX076                                                     RL1174.2
006700*    XXXXX069                                                     RL1174.2
006800     LABEL RECORDS ARE STANDARD                                   RL1174.2
006900     BLOCK CONTAINS 1 RECORDS                                     RL1174.2
007000     DATA RECORD RL-FD2R1-F-G-240.                                RL1174.2
007100 01  RL-FD2R1-F-G-240.                                            RL1174.2
007200     05 RL-FD2-WRK-120 PIC X(120).                                RL1174.2
007300     05 RL-FD2-GRP-120.                                           RL1174.2
007400        10 RL-FD2-WRK-XN-0001-O120F                               RL1174.2
007500                        PICTURE X OCCURS 120 TIMES.               RL1174.2
007600 FD  RL-FD3                                                       RL1174.2
007700C    VALUE OF                                                     RL1174.2
007800C    XXXXX074                                                     RL1174.2
007900C    IS                                                           RL1174.2
008000C    XXXXX076                                                     RL1174.2
008100*    XXXXX069                                                     RL1174.2
008200     LABEL RECORDS ARE STANDARD                                   RL1174.2
008300     BLOCK CONTAINS 1 RECORDS                                     RL1174.2
008400     DATA RECORD RL-FD3R1-F-G-240.                                RL1174.2
008500 01  RL-FD3R1-F-G-240.                                            RL1174.2
008600     05 RL-FD3-WRK-120 PIC X(120).                                RL1174.2
008700     05 RL-FD3-GRP-120.                                           RL1174.2
008800        10 RL-FD3-WRK-XN-0001-O120F                               RL1174.2
008900                        PICTURE X OCCURS 120 TIMES.               RL1174.2
009000 WORKING-STORAGE SECTION.                                         RL1174.2
009100 01  GRP-0001.                                                    RL1174.2
009200     05 RL-FD2-KEY   PIC 9(8)  VALUE ZERO.                        RL1174.2
009300     05 RL-FD3-KEY   PIC  99   VALUE ZERO.                        RL1174.2
009400     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
009500     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
009600     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
009700     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
009800     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
009900     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
010000     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1174.2
010100     05 RL-FD2-STATUS     PIC  XX  VALUE  SPACE.                  RL1174.2
010200     05 RL-FD3-STATUS     PIC  XX  VALUE  SPACE.                  RL1174.2
010300     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1174.2
010400     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1174.2
010500     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1174.2
010600     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1174.2
010700     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1174.2
010800     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1174.2
010900     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1174.2
011000     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1174.2
011100     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1174.2
011200 01  FILE-RECORD-INFORMATION-REC.                                 RL1174.2
011300     03 FILE-RECORD-INFO-SKELETON.                                RL1174.2
011400        05 FILLER                 PICTURE X(48)       VALUE       RL1174.2
011500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1174.2
011600        05 FILLER                 PICTURE X(46)       VALUE       RL1174.2
011700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1174.2
011800        05 FILLER                 PICTURE X(26)       VALUE       RL1174.2
011900             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1174.2
012000        05 FILLER                 PICTURE X(37)       VALUE       RL1174.2
012100             ",RECKEY=                             ".             RL1174.2
012200        05 FILLER                 PICTURE X(38)       VALUE       RL1174.2
012300             ",ALTKEY1=                             ".            RL1174.2
012400        05 FILLER                 PICTURE X(38)       VALUE       RL1174.2
012500             ",ALTKEY2=                             ".            RL1174.2
012600        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1174.2
012700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1174.2
012800        05 FILE-RECORD-INFO-P1-120.                               RL1174.2
012900           07 FILLER              PIC X(5).                       RL1174.2
013000           07 XFILE-NAME           PIC X(6).                      RL1174.2
013100           07 FILLER              PIC X(8).                       RL1174.2
013200           07 XRECORD-NAME         PIC X(6).                      RL1174.2
013300           07 FILLER              PIC X(1).                       RL1174.2
013400           07 REELUNIT-NUMBER     PIC 9(1).                       RL1174.2
013500           07 FILLER              PIC X(7).                       RL1174.2
013600           07 XRECORD-NUMBER       PIC 9(6).                      RL1174.2
013700           07 FILLER              PIC X(6).                       RL1174.2
013800           07 UPDATE-NUMBER       PIC 9(2).                       RL1174.2
013900           07 FILLER              PIC X(5).                       RL1174.2
014000           07 ODO-NUMBER          PIC 9(4).                       RL1174.2
014100           07 FILLER              PIC X(5).                       RL1174.2
014200           07 XPROGRAM-NAME        PIC X(5).                      RL1174.2
014300           07 FILLER              PIC X(7).                       RL1174.2
014400           07 XRECORD-LENGTH       PIC 9(6).                      RL1174.2
014500           07 FILLER              PIC X(7).                       RL1174.2
014600           07 CHARS-OR-RECORDS    PIC X(2).                       RL1174.2
014700           07 FILLER              PIC X(1).                       RL1174.2
014800           07 XBLOCK-SIZE          PIC 9(4).                      RL1174.2
014900           07 FILLER              PIC X(6).                       RL1174.2
015000           07 RECORDS-IN-FILE     PIC 9(6).                       RL1174.2
015100           07 FILLER              PIC X(5).                       RL1174.2
015200           07 XFILE-ORGANIZATION   PIC X(2).                      RL1174.2
015300           07 FILLER              PIC X(6).                       RL1174.2
015400           07 XLABEL-TYPE          PIC X(1).                      RL1174.2
015500        05 FILE-RECORD-INFO-P121-240.                             RL1174.2
015600           07 FILLER              PIC X(8).                       RL1174.2
015700           07 XRECORD-KEY          PIC X(29).                     RL1174.2
015800           07 FILLER              PIC X(9).                       RL1174.2
015900           07 ALTERNATE-KEY1      PIC X(29).                      RL1174.2
016000           07 FILLER              PIC X(9).                       RL1174.2
016100           07 ALTERNATE-KEY2      PIC X(29).                      RL1174.2
016200           07 FILLER              PIC X(7).                       RL1174.2
016300 01  TEST-RESULTS.                                                RL1174.2
016400     02 FILLER                   PIC X      VALUE SPACE.          RL1174.2
016500     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1174.2
016600     02 FILLER                   PIC X      VALUE SPACE.          RL1174.2
016700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1174.2
016800     02 FILLER                   PIC X      VALUE SPACE.          RL1174.2
016900     02  PAR-NAME.                                                RL1174.2
017000       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1174.2
017100       03  PARDOT-X              PIC X      VALUE SPACE.          RL1174.2
017200       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1174.2
017300     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1174.2
017400     02 RE-MARK                  PIC X(61).                       RL1174.2
017500 01  TEST-COMPUTED.                                               RL1174.2
017600     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1174.2
017700     02 FILLER                   PIC X(17)  VALUE                 RL1174.2
017800            "       COMPUTED=".                                   RL1174.2
017900     02 COMPUTED-X.                                               RL1174.2
018000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1174.2
018100     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1174.2
018200                                 PIC -9(9).9(9).                  RL1174.2
018300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1174.2
018400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1174.2
018500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1174.2
018600     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1174.2
018700         04 COMPUTED-18V0                    PIC -9(18).          RL1174.2
018800         04 FILLER                           PIC X.               RL1174.2
018900     03 FILLER PIC X(50) VALUE SPACE.                             RL1174.2
019000 01  TEST-CORRECT.                                                RL1174.2
019100     02 FILLER PIC X(30) VALUE SPACE.                             RL1174.2
019200     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1174.2
019300     02 CORRECT-X.                                                RL1174.2
019400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1174.2
019500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1174.2
019600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1174.2
019700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1174.2
019800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1174.2
019900     03      CR-18V0 REDEFINES CORRECT-A.                         RL1174.2
020000         04 CORRECT-18V0                     PIC -9(18).          RL1174.2
020100         04 FILLER                           PIC X.               RL1174.2
020200     03 FILLER PIC X(2) VALUE SPACE.                              RL1174.2
020300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1174.2
020400 01  CCVS-C-1.                                                    RL1174.2
020500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1174.2
020600-    "SS  PARAGRAPH-NAME                                          RL1174.2
020700-    "       REMARKS".                                            RL1174.2
020800     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1174.2
020900 01  CCVS-C-2.                                                    RL1174.2
021000     02 FILLER                     PIC X        VALUE SPACE.      RL1174.2
021100     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1174.2
021200     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1174.2
021300     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1174.2
021400     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1174.2
021500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1174.2
021600 01  REC-CT                        PIC 99       VALUE ZERO.       RL1174.2
021700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1174.2
021800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1174.2
021900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1174.2
022000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1174.2
022100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1174.2
022200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1174.2
022300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1174.2
022400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1174.2
022500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1174.2
022600 01  CCVS-H-1.                                                    RL1174.2
022700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1174.2
022800     02  FILLER                    PIC X(42)    VALUE             RL1174.2
022900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1174.2
023000     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1174.2
023100 01  CCVS-H-2A.                                                   RL1174.2
023200   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1174.2
023300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1174.2
023400   02  FILLER                        PIC XXXX   VALUE             RL1174.2
023500     "4.2 ".                                                      RL1174.2
023600   02  FILLER                        PIC X(28)  VALUE             RL1174.2
023700            " COPY - NOT FOR DISTRIBUTION".                       RL1174.2
023800   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1174.2
023900                                                                  RL1174.2
024000 01  CCVS-H-2B.                                                   RL1174.2
024100   02  FILLER                        PIC X(15)  VALUE             RL1174.2
024200            "TEST RESULT OF ".                                    RL1174.2
024300   02  TEST-ID                       PIC X(9).                    RL1174.2
024400   02  FILLER                        PIC X(4)   VALUE             RL1174.2
024500            " IN ".                                               RL1174.2
024600   02  FILLER                        PIC X(12)  VALUE             RL1174.2
024700     " HIGH       ".                                              RL1174.2
024800   02  FILLER                        PIC X(22)  VALUE             RL1174.2
024900            " LEVEL VALIDATION FOR ".                             RL1174.2
025000   02  FILLER                        PIC X(58)  VALUE             RL1174.2
025100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1174.2
025200 01  CCVS-H-3.                                                    RL1174.2
025300     02  FILLER                      PIC X(34)  VALUE             RL1174.2
025400            " FOR OFFICIAL USE ONLY    ".                         RL1174.2
025500     02  FILLER                      PIC X(58)  VALUE             RL1174.2
025600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1174.2
025700     02  FILLER                      PIC X(28)  VALUE             RL1174.2
025800            "  COPYRIGHT   1985 ".                                RL1174.2
025900 01  CCVS-E-1.                                                    RL1174.2
026000     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1174.2
026100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1174.2
026200     02 ID-AGAIN                     PIC X(9).                    RL1174.2
026300     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1174.2
026400 01  CCVS-E-2.                                                    RL1174.2
026500     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1174.2
026600     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1174.2
026700     02 CCVS-E-2-2.                                               RL1174.2
026800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1174.2
026900         03 FILLER                   PIC X      VALUE SPACE.      RL1174.2
027000         03 ENDER-DESC               PIC X(44)  VALUE             RL1174.2
027100            "ERRORS ENCOUNTERED".                                 RL1174.2
027200 01  CCVS-E-3.                                                    RL1174.2
027300     02  FILLER                      PIC X(22)  VALUE             RL1174.2
027400            " FOR OFFICIAL USE ONLY".                             RL1174.2
027500     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1174.2
027600     02  FILLER                      PIC X(58)  VALUE             RL1174.2
027700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1174.2
027800     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1174.2
027900     02 FILLER                       PIC X(15)  VALUE             RL1174.2
028000             " COPYRIGHT 1985".                                   RL1174.2
028100 01  CCVS-E-4.                                                    RL1174.2
028200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1174.2
028300     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1174.2
028400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1174.2
028500     02 FILLER                       PIC X(40)  VALUE             RL1174.2
028600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1174.2
028700 01  XXINFO.                                                      RL1174.2
028800     02 FILLER                       PIC X(19)  VALUE             RL1174.2
028900            "*** INFORMATION ***".                                RL1174.2
029000     02 INFO-TEXT.                                                RL1174.2
029100       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1174.2
029200       04 XXCOMPUTED                 PIC X(20).                   RL1174.2
029300       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1174.2
029400       04 XXCORRECT                  PIC X(20).                   RL1174.2
029500     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1174.2
029600 01  HYPHEN-LINE.                                                 RL1174.2
029700     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1174.2
029800     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1174.2
029900-    "*****************************************".                 RL1174.2
030000     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1174.2
030100-    "******************************".                            RL1174.2
030200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1174.2
030300     "RL117A".                                                    RL1174.2
030400 PROCEDURE DIVISION.                                              RL1174.2
030500 CCVS1 SECTION.                                                   RL1174.2
030600 OPEN-FILES.                                                      RL1174.2
030700     OPEN    OUTPUT PRINT-FILE.                                   RL1174.2
030800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1174.2
030900     MOVE    SPACE TO TEST-RESULTS.                               RL1174.2
031000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1174.2
031100     MOVE    ZERO TO REC-SKL-SUB.                                 RL1174.2
031200     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1174.2
031300 CCVS-INIT-FILE.                                                  RL1174.2
031400     ADD     1 TO REC-SKL-SUB.                                    RL1174.2
031500     MOVE    FILE-RECORD-INFO-SKELETON                            RL1174.2
031600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1174.2
031700 CCVS-INIT-EXIT.                                                  RL1174.2
031800     GO TO CCVS1-EXIT.                                            RL1174.2
031900 CLOSE-FILES.                                                     RL1174.2
032000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1174.2
032100 TERMINATE-CCVS.                                                  RL1174.2
032200*    EXIT PROGRAM.                                                RL1174.2
032300*TERMINATE-CALL.                                                  RL1174.2
032400     STOP     RUN.                                                RL1174.2
032500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1174.2
032600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1174.2
032700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1174.2
032800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1174.2
032900     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1174.2
033000 PRINT-DETAIL.                                                    RL1174.2
033100     IF REC-CT NOT EQUAL TO ZERO                                  RL1174.2
033200             MOVE "." TO PARDOT-X                                 RL1174.2
033300             MOVE REC-CT TO DOTVALUE.                             RL1174.2
033400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1174.2
033500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1174.2
033600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1174.2
033700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1174.2
033800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1174.2
033900     MOVE SPACE TO CORRECT-X.                                     RL1174.2
034000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1174.2
034100     MOVE     SPACE TO RE-MARK.                                   RL1174.2
034200 HEAD-ROUTINE.                                                    RL1174.2
034300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1174.2
034400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1174.2
034500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1174.2
034600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1174.2
034700 COLUMN-NAMES-ROUTINE.                                            RL1174.2
034800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1174.2
034900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1174.2
035000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1174.2
035100 END-ROUTINE.                                                     RL1174.2
035200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1174.2
035300 END-RTN-EXIT.                                                    RL1174.2
035400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1174.2
035500 END-ROUTINE-1.                                                   RL1174.2
035600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1174.2
035700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1174.2
035800      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1174.2
035900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1174.2
036000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1174.2
036100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1174.2
036200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1174.2
036300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1174.2
036400  END-ROUTINE-12.                                                 RL1174.2
036500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1174.2
036600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1174.2
036700         MOVE "NO " TO ERROR-TOTAL                                RL1174.2
036800         ELSE                                                     RL1174.2
036900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1174.2
037000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1174.2
037100     PERFORM WRITE-LINE.                                          RL1174.2
037200 END-ROUTINE-13.                                                  RL1174.2
037300     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1174.2
037400         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1174.2
037500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1174.2
037600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1174.2
037700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1174.2
037800      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1174.2
037900          MOVE "NO " TO ERROR-TOTAL                               RL1174.2
038000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1174.2
038100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1174.2
038200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1174.2
038300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1174.2
038400 WRITE-LINE.                                                      RL1174.2
038500     ADD 1 TO RECORD-COUNT.                                       RL1174.2
038600*    IF RECORD-COUNT GREATER 50                                   RL1174.2
038700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1174.2
038800*        MOVE SPACE TO DUMMY-RECORD                               RL1174.2
038900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1174.2
039000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1174.2
039100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1174.2
039200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1174.2
039300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1174.2
039400*        MOVE ZERO TO RECORD-COUNT.                               RL1174.2
039500     PERFORM WRT-LN.                                              RL1174.2
039600 WRT-LN.                                                          RL1174.2
039700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1174.2
039800     MOVE SPACE TO DUMMY-RECORD.                                  RL1174.2
039900 BLANK-LINE-PRINT.                                                RL1174.2
040000     PERFORM WRT-LN.                                              RL1174.2
040100 FAIL-ROUTINE.                                                    RL1174.2
040200     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1174.2
040300            GO TO   FAIL-ROUTINE-WRITE.                           RL1174.2
040400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1174.2
040500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1174.2
040600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1174.2
040700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1174.2
040800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1174.2
040900     GO TO  FAIL-ROUTINE-EX.                                      RL1174.2
041000 FAIL-ROUTINE-WRITE.                                              RL1174.2
041100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1174.2
041200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1174.2
041300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1174.2
041400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1174.2
041500 FAIL-ROUTINE-EX. EXIT.                                           RL1174.2
041600 BAIL-OUT.                                                        RL1174.2
041700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1174.2
041800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1174.2
041900 BAIL-OUT-WRITE.                                                  RL1174.2
042000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1174.2
042100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1174.2
042200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1174.2
042300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1174.2
042400 BAIL-OUT-EX. EXIT.                                               RL1174.2
042500 CCVS1-EXIT.                                                      RL1174.2
042600     EXIT.                                                        RL1174.2
042700 SECT-RL117A-001 SECTION.                                         RL1174.2
042800 REL-INIT-009.                                                    RL1174.2
042900     MOVE    "VIII-36 4.8.4 GR4" TO ANSI-REFERENCE.               RL1174.2
043000     MOVE    "REL-TEST-009" TO PAR-NAME.                          RL1174.2
043100     MOVE     "CREATE RL-FD2" TO  FEATURE                         RL1174.2
043200     MOVE     "RL-FD2"        TO  XFILE-NAME (2).                 RL1174.2
043300     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL1174.2
043400     MOVE     CCVS-PGM-ID     TO  XPROGRAM-NAME (2).              RL1174.2
043500     MOVE     000240          TO  XRECORD-LENGTH (2).             RL1174.2
043600     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL1174.2
043700     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL1174.2
043800     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL1174.2
043900     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL1174.2
044000     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL1174.2
044100     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL1174.2
044200*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1174.2
044300     MOVE     1         TO  WRK-CS-09V00-012.                     RL1174.2
044400     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1174.2
044500                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1174.2
044600                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1174.2
044700     MOVE     SPACE     TO  RL-FD2-STATUS.                        RL1174.2
044800     MOVE     90000002  TO  RL-FD2-KEY.                           RL1174.2
044900     MOVE     01 TO REC-CT.                                       RL1174.2
045000     OPEN     OUTPUT    RL-FD2.                                   RL1174.2
045100     MOVE     RL-FD2-STATUS TO WRK-XN-0002-001.                   RL1174.2
045200*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL1174.2
045300 REL-TEST-009-R.                                                  RL1174.2
045400     MOVE XRECORD-NUMBER (2) TO RL-FD2-KEY.                       RL1174.2
045500     MOVE    "99"  TO RL-FD2-STATUS.                              RL1174.2
045600     MOVE     FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.      RL1174.2
045700     MOVE     FILE-RECORD-INFO-P1-120 (2) TO                      RL1174.2
045800               RL-FD2-GRP-120.                                    RL1174.2
045900     WRITE    RL-FD2R1-F-G-240 INVALID KEY                        RL1174.2
046000              GO TO REL-TEST-009-2.                               RL1174.2
046100     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1174.2
046200              GO TO  REL-TEST-009-2.                              RL1174.2
046300     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL1174.2
046400             GO TO REL-TEST-009-2.                                RL1174.2
046500     ADD      01 TO  XRECORD-NUMBER (2).                          RL1174.2
046600     GO TO    REL-TEST-009-R.                                     RL1174.2
046700 REL-TEST-009-2.                                                  RL1174.2
046800     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  RL1174.2
046900              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 RL1174.2
047000              MOVE ZERO TO CORRECT-18V0                           RL1174.2
047100              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1174.2
047200              PERFORM   FAIL                                      RL1174.2
047300              ELSE                                                RL1174.2
047400              PERFORM   PASS.                                     RL1174.2
047500     PERFORM  PRINT-DETAIL.                                       RL1174.2
047600     ADD      01 TO REC-CT.                                       RL1174.2
047700*    .01                                                          RL1174.2
047800     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 RL1174.2
047900              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1174.2
048000              MOVE  500 TO CORRECT-18V0                           RL1174.2
048100             MOVE    XRECORD-NUMBER (2)  TO COMPUTED-18V0         RL1174.2
048200              PERFORM  FAIL                                       RL1174.2
048300              ELSE                                                RL1174.2
048400              PERFORM PASS.                                       RL1174.2
048500     PERFORM  PRINT-DETAIL.                                       RL1174.2
048600     ADD      01 TO REC-CT.                                       RL1174.2
048700*    .02                                                          RL1174.2
048800     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  RL1174.2
048900              MOVE "STATUS/OPEN" TO RE-MARK                       RL1174.2
049000              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1174.2
049100              MOVE "00" TO CORRECT-A                              RL1174.2
049200              PERFORM  FAIL                                       RL1174.2
049300              ELSE                                                RL1174.2
049400              PERFORM  PASS.                                      RL1174.2
049500     PERFORM  PRINT-DETAIL.                                       RL1174.2
049600     ADD      01 TO REC-CT.                                       RL1174.2
049700*    .03                                                          RL1174.2
049800     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1174.2
049900              MOVE "STATUS/WRITE" TO RE-MARK                      RL1174.2
050000              MOVE RL-FD2-STATUS TO COMPUTED-A                    RL1174.2
050100              MOVE "00" TO CORRECT-A                              RL1174.2
050200              PERFORM   FAIL                                      RL1174.2
050300              ELSE                                                RL1174.2
050400              PERFORM   PASS.                                     RL1174.2
050500     PERFORM  PRINT-DETAIL.                                       RL1174.2
050600     ADD      01 TO REC-CT.                                       RL1174.2
050700*    .04                                                          RL1174.2
050800     MOVE     SPACE  TO  RL-FD2-STATUS.                           RL1174.2
050900     CLOSE    RL-FD2.                                             RL1174.2
051000     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1174.2
051100              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1174.2
051200              MOVE   RL-FD2-STATUS TO COMPUTED-A                  RL1174.2
051300              MOVE "00" TO CORRECT-A                              RL1174.2
051400              PERFORM FAIL                                        RL1174.2
051500              ELSE                                                RL1174.2
051600              PERFORM PASS.                                       RL1174.2
051700     PERFORM  PRINT-DETAIL.                                       RL1174.2
051800*                                                                 RL1174.2
051900 REL-INIT-1.                                                      RL1174.2
052000     MOVE   "REL-TEST-1" TO PAR-NAME.                             RL1174.2
052100     MOVE   "VII-3 1.3.4 2A" TO ANSI-REFERENCE.                   RL1174.2
052200     MOVE    0 TO REC-CT.                                         RL1174.2
052300     OPEN    I-O  RL-FD3.                                         RL1174.2
052400* DELETE THE NEXT LINE TO DELETE THE TEST                         RL1174.2
052500*    GO TO REL-INIT-1-BETA.                                       RL1174.2
052600 REL-INIT-1-ALPHA.                                                RL1174.2
052700     GO TO REL-DELETE-1.                                          RL1174.2
052800 REL-INIT-1-BETA.                                                 RL1174.2
052900     MOVE    SPACE TO RL-FD3-STATUS.                              RL1174.2
053000     PERFORM REL-INIT-1-A 501 TIMES.                              RL1174.2
053100     GO TO   REL-TEST-1.                                          RL1174.2
053200 REL-INIT-1-A.                                                    RL1174.2
053300     READ    RL-FD3 AT END GO TO REL-TEST-1.                      RL1174.2
053400 REL-DELETE-1.                                                    RL1174.2
053500     PERFORM DE-LETE.                                             RL1174.2
053600     PERFORM PRINT-DETAIL.                                        RL1174.2
053700     GO TO   REL-INIT-2.                                          RL1174.2
053800 REL-TEST-1.                                                      RL1174.2
053900     IF      RL-FD3-STATUS NOT EQUAL  TO "10"                     RL1174.2
054000             MOVE   "AT END STATUS" TO RE-MARK                    RL1174.2
054100             MOVE    RL-FD3-STATUS TO COMPUTED-A                  RL1174.2
054200             MOVE   "10" TO CORRECT-A                             RL1174.2
054300             PERFORM FAIL                                         RL1174.2
054400             PERFORM PRINT-DETAIL                                 RL1174.2
054500     ELSE                                                         RL1174.2
054600             PERFORM PASS                                         RL1174.2
054700             PERFORM PRINT-DETAIL.                                RL1174.2
054800*                                                                 RL1174.2
054900 REL-INIT-2.                                                      RL1174.2
055000     MOVE   "REL-TEST-2" TO PAR-NAME.                             RL1174.2
055100     MOVE   "VII-3 1.3.4 2D" TO ANSI-REFERENCE.                   RL1174.2
055200     MOVE    SPACE TO RL-FD3-STATUS.                              RL1174.2
055300* DELETE THE NEXT LINE TO DELETE THE TEST                         RL1174.2
055400*    GO TO REL-INIT-2-BETA.                                       RL1174.2
055500 REL-INIT-2-ALPHA.                                                RL1174.2
055600     GO TO REL-DELETE-2.                                          RL1174.2
055700 REL-INIT-2-BETA.                                                 RL1174.2
055800     READ    RL-FD3   AT END GO TO REL-TEST-2.                    RL1174.2
055900     GO TO   REL-TEST-2.                                          RL1174.2
056000 REL-DELETE-2.                                                    RL1174.2
056100     PERFORM DE-LETE.                                             RL1174.2
056200     PERFORM PRINT-DETAIL.                                        RL1174.2
056300     GO TO   REL-INIT-3.                                          RL1174.2
056400 REL-TEST-2.                                                      RL1174.2
056500     IF      RL-FD3-STATUS NOT EQUAL  TO "46"                     RL1174.2
056600             MOVE   "SEQUENTIAL READ FOLLOWING 'AT END' CONDITION"RL1174.2
056700                  TO RE-MARK                                      RL1174.2
056800             MOVE    RL-FD3-STATUS TO COMPUTED-A                  RL1174.2
056900             MOVE   "46" TO CORRECT-A                             RL1174.2
057000             PERFORM FAIL                                         RL1174.2
057100             PERFORM PRINT-DETAIL                                 RL1174.2
057200     ELSE                                                         RL1174.2
057300             PERFORM PASS                                         RL1174.2
057400             PERFORM PRINT-DETAIL.                                RL1174.2
057500*                                                                 RL1174.2
057600 REL-INIT-3.                                                      RL1174.2
057700     MOVE   "REL-TEST-3" TO PAR-NAME.                             RL1174.2
057800     MOVE   "VII-3 1.3.4 2B" TO ANSI-REFERENCE.                   RL1174.2
057900     CLOSE   RL-FD3.                                              RL1174.2
058000     OPEN    INPUT RL-FD3.                                        RL1174.2
058100     PERFORM REL-INIT-3-A 100 TIMES.                              RL1174.2
058200     GO TO   REL-TEST-3.                                          RL1174.2
058300 REL-INIT-3-A.                                                    RL1174.2
058400     READ    RL-FD3 RECORD AT END GO TO REL-TEST-3.               RL1174.2
058500 REL-DELETE-3.                                                    RL1174.2
058600     PERFORM DE-LETE.                                             RL1174.2
058700     PERFORM PRINT-DETAIL.                                        RL1174.2
058800     GO TO   RL-TEST-3-EXIT.                                      RL1174.2
058900 REL-TEST-3.                                                      RL1174.2
059000     IF      RL-FD3-STATUS NOT EQUAL  TO "14"                     RL1174.2
059100             MOVE    RL-FD3-STATUS TO COMPUTED-A                  RL1174.2
059200             MOVE   "14" TO CORRECT-A                             RL1174.2
059300             PERFORM FAIL                                         RL1174.2
059400             PERFORM PRINT-DETAIL                                 RL1174.2
059500     ELSE                                                         RL1174.2
059600             PERFORM PASS                                         RL1174.2
059700             PERFORM PRINT-DETAIL                                 RL1174.2
059800     CLOSE   RL-FD3.                                              RL1174.2
059900 RL-TEST-3-EXIT.                                                  RL1174.2
060000     EXIT.                                                        RL1174.2
060100*                                                                 RL1174.2
060200 CCVS-EXIT SECTION.                                               RL1174.2
060300 CCVS-999999.                                                     RL1174.2
060400     GO TO CLOSE-FILES.                                           RL1174.2
      *END-OF,RL117A                                                                  
