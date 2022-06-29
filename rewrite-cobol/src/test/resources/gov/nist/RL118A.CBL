      *HEADER,COBOL,RL118A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1184.2
000200 PROGRAM-ID.                                                      RL1184.2
000300     RL118A.                                                      RL1184.2
000400****************************************************************  RL1184.2
000500*                                                              *  RL1184.2
000600*    VALIDATION FOR:-                                          *  RL1184.2
000700*                                                              *  RL1184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1184.2
000900*                                                              *  RL1184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1184.2
001100*                                                              *  RL1184.2
001200****************************************************************  RL1184.2
001300*                                                              *  RL1184.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1184.2
001500*                                                              *  RL1184.2
001600*            X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1184.2
001700*                    RELATIVE  I-O DATA FILE                   *  RL1184.2
001800*            X-55   SYSTEM PRINTER                             *  RL1184.2
001900*            X-69   ADDITIONAL VALUE OF CLAUSES                *  RL1184.2
002000*            X-74   VALUE OF IMPLEMENTOR-NAME                  *  RL1184.2
002100*            X-75   OBJECT OF VALUE OF CLAUSE                  *  RL1184.2
002200*            X-82   SOURCE-COMPUTER                            *  RL1184.2
002300*            X-83   OBJECT-COMPUTER.                           *  RL1184.2
002400*                                                              *  RL1184.2
002500****************************************************************  RL1184.2
002600*    RL118A                                                    *  RL1184.2
002700****************************************************************  RL1184.2
002800*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND *  RL1184.2
002900*            SEMANTIC ACTIONS ASSOCIATED WITH THE "STATUS"     *  RL1184.2
003000*            CLAUSE FOR THE VALUES "22", "23" AND "24".        *  RL1184.2
003100****************************************************************  RL1184.2
003200 ENVIRONMENT DIVISION.                                            RL1184.2
003300 CONFIGURATION SECTION.                                           RL1184.2
003400 SOURCE-COMPUTER.                                                 RL1184.2
003500     XXXXX082.                                                    RL1184.2
003600 OBJECT-COMPUTER.                                                 RL1184.2
003700     XXXXX083.                                                    RL1184.2
003800 INPUT-OUTPUT SECTION.                                            RL1184.2
003900 FILE-CONTROL.                                                    RL1184.2
004000     SELECT PRINT-FILE ASSIGN TO                                  RL1184.2
004100     XXXXX055.                                                    RL1184.2
004200     SELECT   RL-FD2 ASSIGN                                       RL1184.2
004300     XXXXX022                                                     RL1184.2
004400              ORGANIZATION RELATIVE                               RL1184.2
004500              ACCESS  RANDOM                                      RL1184.2
004600              RELATIVE  RL-FD2-KEY                                RL1184.2
004700         FILE STATUS IS RL-FD2-STATUS.                            RL1184.2
004800     SELECT   RL-FD3 ASSIGN                                       RL1184.2
004900     XXXXX022                                                     RL1184.2
005000              ORGANIZATION RELATIVE                               RL1184.2
005100              ACCESS  RANDOM                                      RL1184.2
005200              RELATIVE  RL-FD3-KEY                                RL1184.2
005300         FILE STATUS IS RL-FD3-STATUS.                            RL1184.2
005400 DATA DIVISION.                                                   RL1184.2
005500 FILE SECTION.                                                    RL1184.2
005600 FD  PRINT-FILE.                                                  RL1184.2
005700 01  PRINT-REC PICTURE X(120).                                    RL1184.2
005800 01  DUMMY-RECORD PICTURE X(120).                                 RL1184.2
005900 FD  RL-FD2                                                       RL1184.2
006000C    VALUE OF                                                     RL1184.2
006100C    XXXXX074                                                     RL1184.2
006200C    IS                                                           RL1184.2
006300C    XXXXX076                                                     RL1184.2
006400*    XXXXX069                                                     RL1184.2
006500     LABEL RECORDS ARE STANDARD                                   RL1184.2
006600     BLOCK CONTAINS 1 RECORDS                                     RL1184.2
006700     DATA RECORD RL-FD2R1-F-G-240.                                RL1184.2
006800 01  RL-FD2R1-F-G-240.                                            RL1184.2
006900     05 RL-FD2-WRK-120 PIC X(120).                                RL1184.2
007000     05 RL-FD2-GRP-120.                                           RL1184.2
007100        10 RL-FD2-WRK-XN-0001-O120F                               RL1184.2
007200                        PICTURE X OCCURS 120 TIMES.               RL1184.2
007300 FD  RL-FD3                                                       RL1184.2
007400C    VALUE OF                                                     RL1184.2
007500C    XXXXX074                                                     RL1184.2
007600C    IS                                                           RL1184.2
007700C    XXXXX076                                                     RL1184.2
007800*    XXXXX069                                                     RL1184.2
007900     LABEL RECORDS ARE STANDARD                                   RL1184.2
008000     BLOCK CONTAINS 1 RECORDS                                     RL1184.2
008100     DATA RECORD RL-FD3R1-F-G-240.                                RL1184.2
008200 01  RL-FD3R1-F-G-240.                                            RL1184.2
008300     05 RL-FD3-WRK-120 PIC X(120).                                RL1184.2
008400     05 RL-FD3-GRP-120.                                           RL1184.2
008500        10 RL-FD3-WRK-XN-0001-O120F                               RL1184.2
008600                        PICTURE X OCCURS 120 TIMES.               RL1184.2
008700 WORKING-STORAGE SECTION.                                         RL1184.2
008800 01  GRP-0001.                                                    RL1184.2
008900     05 RL-FD2-KEY   PIC  99   VALUE ZERO.                        RL1184.2
009000     05 RL-FD3-KEY   PIC  999  VALUE ZERO.                        RL1184.2
009100     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009200     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009300     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009400     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009500     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009600     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009700     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1184.2
009800     05 RL-FD2-STATUS     PIC  XX  VALUE  SPACE.                  RL1184.2
009900     05 RL-FD3-STATUS     PIC  XX  VALUE  SPACE.                  RL1184.2
010000     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1184.2
010100     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1184.2
010200     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1184.2
010300     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1184.2
010400     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1184.2
010500     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1184.2
010600     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1184.2
010700     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1184.2
010800     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1184.2
010900 01  FILE-RECORD-INFORMATION-REC.                                 RL1184.2
011000     03 FILE-RECORD-INFO-SKELETON.                                RL1184.2
011100        05 FILLER                 PICTURE X(48)       VALUE       RL1184.2
011200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1184.2
011300        05 FILLER                 PICTURE X(46)       VALUE       RL1184.2
011400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1184.2
011500        05 FILLER                 PICTURE X(26)       VALUE       RL1184.2
011600             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1184.2
011700        05 FILLER                 PICTURE X(37)       VALUE       RL1184.2
011800             ",RECKEY=                             ".             RL1184.2
011900        05 FILLER                 PICTURE X(38)       VALUE       RL1184.2
012000             ",ALTKEY1=                             ".            RL1184.2
012100        05 FILLER                 PICTURE X(38)       VALUE       RL1184.2
012200             ",ALTKEY2=                             ".            RL1184.2
012300        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1184.2
012400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1184.2
012500        05 FILE-RECORD-INFO-P1-120.                               RL1184.2
012600           07 FILLER              PIC X(5).                       RL1184.2
012700           07 XFILE-NAME           PIC X(6).                      RL1184.2
012800           07 FILLER              PIC X(8).                       RL1184.2
012900           07 XRECORD-NAME         PIC X(6).                      RL1184.2
013000           07 FILLER              PIC X(1).                       RL1184.2
013100           07 REELUNIT-NUMBER     PIC 9(1).                       RL1184.2
013200           07 FILLER              PIC X(7).                       RL1184.2
013300           07 XRECORD-NUMBER       PIC 9(6).                      RL1184.2
013400           07 FILLER              PIC X(6).                       RL1184.2
013500           07 UPDATE-NUMBER       PIC 9(2).                       RL1184.2
013600           07 FILLER              PIC X(5).                       RL1184.2
013700           07 ODO-NUMBER          PIC 9(4).                       RL1184.2
013800           07 FILLER              PIC X(5).                       RL1184.2
013900           07 XPROGRAM-NAME        PIC X(5).                      RL1184.2
014000           07 FILLER              PIC X(7).                       RL1184.2
014100           07 XRECORD-LENGTH       PIC 9(6).                      RL1184.2
014200           07 FILLER              PIC X(7).                       RL1184.2
014300           07 CHARS-OR-RECORDS    PIC X(2).                       RL1184.2
014400           07 FILLER              PIC X(1).                       RL1184.2
014500           07 XBLOCK-SIZE          PIC 9(4).                      RL1184.2
014600           07 FILLER              PIC X(6).                       RL1184.2
014700           07 RECORDS-IN-FILE     PIC 9(6).                       RL1184.2
014800           07 FILLER              PIC X(5).                       RL1184.2
014900           07 XFILE-ORGANIZATION   PIC X(2).                      RL1184.2
015000           07 FILLER              PIC X(6).                       RL1184.2
015100           07 XLABEL-TYPE          PIC X(1).                      RL1184.2
015200        05 FILE-RECORD-INFO-P121-240.                             RL1184.2
015300           07 FILLER              PIC X(8).                       RL1184.2
015400           07 XRECORD-KEY          PIC X(29).                     RL1184.2
015500           07 FILLER              PIC X(9).                       RL1184.2
015600           07 ALTERNATE-KEY1      PIC X(29).                      RL1184.2
015700           07 FILLER              PIC X(9).                       RL1184.2
015800           07 ALTERNATE-KEY2      PIC X(29).                      RL1184.2
015900           07 FILLER              PIC X(7).                       RL1184.2
016000 01  TEST-RESULTS.                                                RL1184.2
016100     02 FILLER                   PIC X      VALUE SPACE.          RL1184.2
016200     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1184.2
016300     02 FILLER                   PIC X      VALUE SPACE.          RL1184.2
016400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1184.2
016500     02 FILLER                   PIC X      VALUE SPACE.          RL1184.2
016600     02  PAR-NAME.                                                RL1184.2
016700       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1184.2
016800       03  PARDOT-X              PIC X      VALUE SPACE.          RL1184.2
016900       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1184.2
017000     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1184.2
017100     02 RE-MARK                  PIC X(61).                       RL1184.2
017200 01  TEST-COMPUTED.                                               RL1184.2
017300     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1184.2
017400     02 FILLER                   PIC X(17)  VALUE                 RL1184.2
017500            "       COMPUTED=".                                   RL1184.2
017600     02 COMPUTED-X.                                               RL1184.2
017700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1184.2
017800     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1184.2
017900                                 PIC -9(9).9(9).                  RL1184.2
018000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1184.2
018100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1184.2
018200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1184.2
018300     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1184.2
018400         04 COMPUTED-18V0                    PIC -9(18).          RL1184.2
018500         04 FILLER                           PIC X.               RL1184.2
018600     03 FILLER PIC X(50) VALUE SPACE.                             RL1184.2
018700 01  TEST-CORRECT.                                                RL1184.2
018800     02 FILLER PIC X(30) VALUE SPACE.                             RL1184.2
018900     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1184.2
019000     02 CORRECT-X.                                                RL1184.2
019100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1184.2
019200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1184.2
019300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1184.2
019400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1184.2
019500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1184.2
019600     03      CR-18V0 REDEFINES CORRECT-A.                         RL1184.2
019700         04 CORRECT-18V0                     PIC -9(18).          RL1184.2
019800         04 FILLER                           PIC X.               RL1184.2
019900     03 FILLER PIC X(2) VALUE SPACE.                              RL1184.2
020000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1184.2
020100 01  CCVS-C-1.                                                    RL1184.2
020200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1184.2
020300-    "SS  PARAGRAPH-NAME                                          RL1184.2
020400-    "       REMARKS".                                            RL1184.2
020500     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1184.2
020600 01  CCVS-C-2.                                                    RL1184.2
020700     02 FILLER                     PIC X        VALUE SPACE.      RL1184.2
020800     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1184.2
020900     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1184.2
021000     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1184.2
021100     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1184.2
021200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1184.2
021300 01  REC-CT                        PIC 99       VALUE ZERO.       RL1184.2
021400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1184.2
021500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1184.2
021600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1184.2
021700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1184.2
021800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1184.2
021900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1184.2
022000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1184.2
022100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1184.2
022200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1184.2
022300 01  CCVS-H-1.                                                    RL1184.2
022400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1184.2
022500     02  FILLER                    PIC X(42)    VALUE             RL1184.2
022600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1184.2
022700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1184.2
022800 01  CCVS-H-2A.                                                   RL1184.2
022900   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1184.2
023000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1184.2
023100   02  FILLER                        PIC XXXX   VALUE             RL1184.2
023200     "4.2 ".                                                      RL1184.2
023300   02  FILLER                        PIC X(28)  VALUE             RL1184.2
023400            " COPY - NOT FOR DISTRIBUTION".                       RL1184.2
023500   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1184.2
023600                                                                  RL1184.2
023700 01  CCVS-H-2B.                                                   RL1184.2
023800   02  FILLER                        PIC X(15)  VALUE             RL1184.2
023900            "TEST RESULT OF ".                                    RL1184.2
024000   02  TEST-ID                       PIC X(9).                    RL1184.2
024100   02  FILLER                        PIC X(4)   VALUE             RL1184.2
024200            " IN ".                                               RL1184.2
024300   02  FILLER                        PIC X(12)  VALUE             RL1184.2
024400     " HIGH       ".                                              RL1184.2
024500   02  FILLER                        PIC X(22)  VALUE             RL1184.2
024600            " LEVEL VALIDATION FOR ".                             RL1184.2
024700   02  FILLER                        PIC X(58)  VALUE             RL1184.2
024800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1184.2
024900 01  CCVS-H-3.                                                    RL1184.2
025000     02  FILLER                      PIC X(34)  VALUE             RL1184.2
025100            " FOR OFFICIAL USE ONLY    ".                         RL1184.2
025200     02  FILLER                      PIC X(58)  VALUE             RL1184.2
025300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1184.2
025400     02  FILLER                      PIC X(28)  VALUE             RL1184.2
025500            "  COPYRIGHT   1985 ".                                RL1184.2
025600 01  CCVS-E-1.                                                    RL1184.2
025700     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1184.2
025800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1184.2
025900     02 ID-AGAIN                     PIC X(9).                    RL1184.2
026000     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1184.2
026100 01  CCVS-E-2.                                                    RL1184.2
026200     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1184.2
026300     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1184.2
026400     02 CCVS-E-2-2.                                               RL1184.2
026500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1184.2
026600         03 FILLER                   PIC X      VALUE SPACE.      RL1184.2
026700         03 ENDER-DESC               PIC X(44)  VALUE             RL1184.2
026800            "ERRORS ENCOUNTERED".                                 RL1184.2
026900 01  CCVS-E-3.                                                    RL1184.2
027000     02  FILLER                      PIC X(22)  VALUE             RL1184.2
027100            " FOR OFFICIAL USE ONLY".                             RL1184.2
027200     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1184.2
027300     02  FILLER                      PIC X(58)  VALUE             RL1184.2
027400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1184.2
027500     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1184.2
027600     02 FILLER                       PIC X(15)  VALUE             RL1184.2
027700             " COPYRIGHT 1985".                                   RL1184.2
027800 01  CCVS-E-4.                                                    RL1184.2
027900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1184.2
028000     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1184.2
028100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1184.2
028200     02 FILLER                       PIC X(40)  VALUE             RL1184.2
028300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1184.2
028400 01  XXINFO.                                                      RL1184.2
028500     02 FILLER                       PIC X(19)  VALUE             RL1184.2
028600            "*** INFORMATION ***".                                RL1184.2
028700     02 INFO-TEXT.                                                RL1184.2
028800       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1184.2
028900       04 XXCOMPUTED                 PIC X(20).                   RL1184.2
029000       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1184.2
029100       04 XXCORRECT                  PIC X(20).                   RL1184.2
029200     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1184.2
029300 01  HYPHEN-LINE.                                                 RL1184.2
029400     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1184.2
029500     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1184.2
029600-    "*****************************************".                 RL1184.2
029700     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1184.2
029800-    "******************************".                            RL1184.2
029900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1184.2
030000     "RL118A".                                                    RL1184.2
030100 PROCEDURE DIVISION.                                              RL1184.2
030200 CCVS1 SECTION.                                                   RL1184.2
030300 OPEN-FILES.                                                      RL1184.2
030400     OPEN    OUTPUT PRINT-FILE.                                   RL1184.2
030500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1184.2
030600     MOVE    SPACE TO TEST-RESULTS.                               RL1184.2
030700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1184.2
030800     MOVE    ZERO TO REC-SKL-SUB.                                 RL1184.2
030900     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1184.2
031000 CCVS-INIT-FILE.                                                  RL1184.2
031100     ADD     1 TO REC-SKL-SUB.                                    RL1184.2
031200     MOVE    FILE-RECORD-INFO-SKELETON                            RL1184.2
031300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1184.2
031400 CCVS-INIT-EXIT.                                                  RL1184.2
031500     GO TO CCVS1-EXIT.                                            RL1184.2
031600 CLOSE-FILES.                                                     RL1184.2
031700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1184.2
031800 TERMINATE-CCVS.                                                  RL1184.2
031900*    EXIT PROGRAM.                                                RL1184.2
032000*TERMINATE-CALL.                                                  RL1184.2
032100     STOP     RUN.                                                RL1184.2
032200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1184.2
032300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1184.2
032400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1184.2
032500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1184.2
032600     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1184.2
032700 PRINT-DETAIL.                                                    RL1184.2
032800     IF REC-CT NOT EQUAL TO ZERO                                  RL1184.2
032900             MOVE "." TO PARDOT-X                                 RL1184.2
033000             MOVE REC-CT TO DOTVALUE.                             RL1184.2
033100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1184.2
033200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1184.2
033300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1184.2
033400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1184.2
033500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1184.2
033600     MOVE SPACE TO CORRECT-X.                                     RL1184.2
033700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1184.2
033800     MOVE     SPACE TO RE-MARK.                                   RL1184.2
033900 HEAD-ROUTINE.                                                    RL1184.2
034000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1184.2
034100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1184.2
034200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1184.2
034300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1184.2
034400 COLUMN-NAMES-ROUTINE.                                            RL1184.2
034500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1184.2
034600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1184.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1184.2
034800 END-ROUTINE.                                                     RL1184.2
034900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1184.2
035000 END-RTN-EXIT.                                                    RL1184.2
035100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1184.2
035200 END-ROUTINE-1.                                                   RL1184.2
035300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1184.2
035400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1184.2
035500      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1184.2
035600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1184.2
035700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1184.2
035800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1184.2
035900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1184.2
036000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1184.2
036100  END-ROUTINE-12.                                                 RL1184.2
036200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1184.2
036300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1184.2
036400         MOVE "NO " TO ERROR-TOTAL                                RL1184.2
036500         ELSE                                                     RL1184.2
036600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1184.2
036700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1184.2
036800     PERFORM WRITE-LINE.                                          RL1184.2
036900 END-ROUTINE-13.                                                  RL1184.2
037000     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1184.2
037100         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1184.2
037200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1184.2
037300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1184.2
037400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1184.2
037500      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1184.2
037600          MOVE "NO " TO ERROR-TOTAL                               RL1184.2
037700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1184.2
037800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1184.2
037900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1184.2
038000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1184.2
038100 WRITE-LINE.                                                      RL1184.2
038200     ADD 1 TO RECORD-COUNT.                                       RL1184.2
038300*    IF RECORD-COUNT GREATER 50                                   RL1184.2
038400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1184.2
038500*        MOVE SPACE TO DUMMY-RECORD                               RL1184.2
038600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1184.2
038700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1184.2
038800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1184.2
038900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1184.2
039000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1184.2
039100*        MOVE ZERO TO RECORD-COUNT.                               RL1184.2
039200     PERFORM WRT-LN.                                              RL1184.2
039300 WRT-LN.                                                          RL1184.2
039400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1184.2
039500     MOVE SPACE TO DUMMY-RECORD.                                  RL1184.2
039600 BLANK-LINE-PRINT.                                                RL1184.2
039700     PERFORM WRT-LN.                                              RL1184.2
039800 FAIL-ROUTINE.                                                    RL1184.2
039900     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1184.2
040000            GO TO   FAIL-ROUTINE-WRITE.                           RL1184.2
040100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1184.2
040200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1184.2
040300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1184.2
040400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1184.2
040500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1184.2
040600     GO TO  FAIL-ROUTINE-EX.                                      RL1184.2
040700 FAIL-ROUTINE-WRITE.                                              RL1184.2
040800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1184.2
040900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1184.2
041000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1184.2
041100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1184.2
041200 FAIL-ROUTINE-EX. EXIT.                                           RL1184.2
041300 BAIL-OUT.                                                        RL1184.2
041400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1184.2
041500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1184.2
041600 BAIL-OUT-WRITE.                                                  RL1184.2
041700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1184.2
041800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1184.2
041900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1184.2
042000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1184.2
042100 BAIL-OUT-EX. EXIT.                                               RL1184.2
042200 CCVS1-EXIT.                                                      RL1184.2
042300     EXIT.                                                        RL1184.2
042400 SECT-RL118A-001 SECTION.                                         RL1184.2
042500 REL-INIT-009.                                                    RL1184.2
042600     MOVE   "REL-TEST-009"  TO PAR-NAME.                          RL1184.2
042700     MOVE   "CREATE RL-FD2" TO  FEATURE                           RL1184.2
042800     MOVE   "RL-FD2"        TO  XFILE-NAME (2).                   RL1184.2
042900     MOVE   "R1-F-G"        TO  XRECORD-NAME (2).                 RL1184.2
043000     MOVE    CCVS-PGM-ID    TO  XPROGRAM-NAME (2).                RL1184.2
043100     MOVE    000240         TO  XRECORD-LENGTH (2).               RL1184.2
043200     MOVE   "RC"            TO  CHARS-OR-RECORDS (2).             RL1184.2
043300     MOVE    0001           TO  XBLOCK-SIZE       (2).            RL1184.2
043400     MOVE    000500         TO  RECORDS-IN-FILE  (2).             RL1184.2
043500     MOVE     "RL"          TO  XFILE-ORGANIZATION (2).           RL1184.2
043600     MOVE     "S"           TO  XLABEL-TYPE        (2).           RL1184.2
043700     MOVE     000001        TO  XRECORD-NUMBER     (2).           RL1184.2
043800*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1184.2
043900     MOVE     1         TO  WRK-CS-09V00-012.                     RL1184.2
044000     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1184.2
044100                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1184.2
044200                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1184.2
044300*                                                                 RL1184.2
044400 REL-INIT-1.                                                      RL1184.2
044500     MOVE   "REL-TEST-1" TO PAR-NAME.                             RL1184.2
044600     MOVE   "VII-3 1.3.4 3C" TO ANSI-REFERENCE.                   RL1184.2
044700     MOVE    0 TO XRECORD-NUMBER (2).                             RL1184.2
044800     OPEN    OUTPUT RL-FD2.                                       RL1184.2
044900     MOVE    SPACE TO RL-FD2-STATUS.                              RL1184.2
045000     PERFORM REL-INIT-1-A 100 TIMES.                              RL1184.2
045100     GO TO   REL-DELETE-1.                                        RL1184.2
045200 REL-INIT-1-A.                                                    RL1184.2
045300     ADD     1 TO XRECORD-NUMBER (2).                             RL1184.2
045400     MOVE    XRECORD-NUMBER (2) TO RL-FD2-KEY.                    RL1184.2
045500     MOVE    FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.       RL1184.2
045600     MOVE    FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-GRP-120.       RL1184.2
045700     WRITE   RL-FD2R1-F-G-240                                     RL1184.2
045800         INVALID GO TO REL-DELETE-1.                              RL1184.2
045900 REL-DELETE-1.                                                    RL1184.2
046000     PERFORM DE-LETE.                                             RL1184.2
046100     PERFORM PRINT-DETAIL.                                        RL1184.2
046200     GO TO   REL-INIT-2.                                          RL1184.2
046300 REL-TEST-1.                                                      RL1184.2
046400     IF      RL-FD2-STATUS NOT EQUAL  TO "24"                     RL1184.2
046500             MOVE   "100TH RECORD SHOULD NOT BE WRITTEN"          RL1184.2
046600                  TO RE-MARK                                      RL1184.2
046700             MOVE    RL-FD2-STATUS TO COMPUTED-A                  RL1184.2
046800             MOVE   "24" TO CORRECT-A                             RL1184.2
046900             PERFORM FAIL                                         RL1184.2
047000             PERFORM PRINT-DETAIL                                 RL1184.2
047100     ELSE                                                         RL1184.2
047200             PERFORM PASS                                         RL1184.2
047300             PERFORM PRINT-DETAIL.                                RL1184.2
047400*                                                                 RL1184.2
047500 REL-INIT-2.                                                      RL1184.2
047600     MOVE   "REL-TEST-2" TO PAR-NAME.                             RL1184.2
047700     MOVE   "VII-3 1.3.4 3A" TO ANSI-REFERENCE.                   RL1184.2
047800     MOVE    SPACE TO RL-FD2-STATUS.                              RL1184.2
047900     MOVE    27 TO RL-FD2-KEY.                                    RL1184.2
048000     MOVE    FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.       RL1184.2
048100     MOVE    FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-GRP-120.       RL1184.2
048200     WRITE   RL-FD2R1-F-G-240                                     RL1184.2
048300         INVALID GO TO REL-TEST-2.                                RL1184.2
048400     GO TO   REL-TEST-2.                                          RL1184.2
048500 REL-DELETE-2.                                                    RL1184.2
048600     PERFORM DE-LETE.                                             RL1184.2
048700     PERFORM PRINT-DETAIL.                                        RL1184.2
048800     GO TO   REL-INIT-3.                                          RL1184.2
048900 REL-TEST-2.                                                      RL1184.2
049000     IF      RL-FD2-STATUS NOT EQUAL  TO "22"                     RL1184.2
049100             MOVE   "DUPLICATE KEY SHOULD HAVE OCCURRED"          RL1184.2
049200                  TO RE-MARK                                      RL1184.2
049300             MOVE    RL-FD2-STATUS TO COMPUTED-A                  RL1184.2
049400             MOVE   "22" TO CORRECT-A                             RL1184.2
049500             PERFORM FAIL                                         RL1184.2
049600             PERFORM PRINT-DETAIL                                 RL1184.2
049700     ELSE                                                         RL1184.2
049800             PERFORM PASS                                         RL1184.2
049900             PERFORM PRINT-DETAIL.                                RL1184.2
050000*                                                                 RL1184.2
050100*                                                                 RL1184.2
050200 REL-INIT-3.                                                      RL1184.2
050300     MOVE   "REL-TEST-3" TO PAR-NAME.                             RL1184.2
050400     MOVE   "VII-3 1.3.4 3B" TO ANSI-REFERENCE.                   RL1184.2
050500     CLOSE   RL-FD2.                                              RL1184.2
050600     OPEN    I-O RL-FD3.                                          RL1184.2
050700     MOVE    999 TO RL-FD3-KEY.                                   RL1184.2
050800     READ RL-FD3 INVALID GO TO REL-TEST-3.                        RL1184.2
050900     GO TO   REL-TEST-3.                                          RL1184.2
051000 REL-DELETE-3.                                                    RL1184.2
051100     PERFORM DE-LETE.                                             RL1184.2
051200     PERFORM PRINT-DETAIL.                                        RL1184.2
051300     GO TO   REL-DELETE-4.                                        RL1184.2
051400 REL-TEST-3.                                                      RL1184.2
051500     IF      RL-FD3-STATUS NOT EQUAL  TO "23"                     RL1184.2
051600             MOVE   "RECORD READ SHOULD NOT EXIST" TO RE-MARK     RL1184.2
051700             MOVE    RL-FD3-STATUS TO COMPUTED-A                  RL1184.2
051800             MOVE   "23" TO CORRECT-A                             RL1184.2
051900             PERFORM FAIL                                         RL1184.2
052000             PERFORM PRINT-DETAIL                                 RL1184.2
052100     ELSE                                                         RL1184.2
052200             PERFORM PASS                                         RL1184.2
052300             PERFORM PRINT-DETAIL.                                RL1184.2
052400*                                                                 RL1184.2
052500 REL-INIT-4.                                                      RL1184.2
052600     MOVE   "REL-TEST-4" TO PAR-NAME.                             RL1184.2
052700     MOVE   "VII-3 1.3.4 3C" TO ANSI-REFERENCE.                   RL1184.2
052800     MOVE    SPACE TO RL-FD2-STATUS.                              RL1184.2
052900     MOVE    100 TO RL-FD3-KEY.                                   RL1184.2
053000     GO TO REL-DELETE-4.                                          RL1184.2
053100*    WRITE   RL-FD3R1-F-G-240                                     RL1184.2
053200*        INVALID GO TO REL-DELETE-4.                              RL1184.2
053300*    GO TO   REL-TEST-4.                                          RL1184.2
053400 REL-DELETE-4.                                                    RL1184.2
053500     PERFORM DE-LETE.                                             RL1184.2
053600     PERFORM PRINT-DETAIL.                                        RL1184.2
053700     GO TO   REL-TEST-4-EXIT.                                     RL1184.2
053800 REL-TEST-4.                                                      RL1184.2
053900     IF      RL-FD3-STATUS NOT EQUAL  TO "24"                     RL1184.2
054000             MOVE   "BOUNDARY VIOLATION SHOULD HAVE OCCURRED"     RL1184.2
054100                  TO RE-MARK                                      RL1184.2
054200             MOVE    RL-FD3-STATUS TO COMPUTED-A                  RL1184.2
054300             MOVE   "24" TO CORRECT-A                             RL1184.2
054400             PERFORM FAIL                                         RL1184.2
054500             PERFORM PRINT-DETAIL                                 RL1184.2
054600     ELSE                                                         RL1184.2
054700             PERFORM PASS                                         RL1184.2
054800             PERFORM PRINT-DETAIL.                                RL1184.2
054900 REL-TEST-4-EXIT.                                                 RL1184.2
055000     CLOSE RL-FD3.                                                RL1184.2
055100*                                                                 RL1184.2
055200 CCVS-EXIT SECTION.                                               RL1184.2
055300 CCVS-999999.                                                     RL1184.2
055400     GO TO CLOSE-FILES.                                           RL1184.2
      *END-OF,RL118A                                                                  
