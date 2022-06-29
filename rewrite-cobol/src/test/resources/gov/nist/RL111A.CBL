      *HEADER,COBOL,RL111A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1114.2
000200 PROGRAM-ID.                                                      RL1114.2
000300     RL111A.                                                      RL1114.2
000400****************************************************************  RL1114.2
000500*                                                              *  RL1114.2
000600*    VALIDATION FOR:-                                          *  RL1114.2
000700*                                                              *  RL1114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1114.2
000900*                                                              *  RL1114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1114.2
001100*                                                              *  RL1114.2
001200****************************************************************  RL1114.2
001300*                                                              *  RL1114.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1114.2
001500*                                                              *  RL1114.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  RL1114.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  RL1114.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  RL1114.2
001900*                                                              *  RL1114.2
002000****************************************************************  RL1114.2
002100*                                                              *  RL1114.2
002200*     THIS PROGRAM WILL TEST THE SYNTACTICAL CONSTRUCTS AND    *  RL1114.2
002300*     SEMANTIC ACTIONS ASSOCIATED WITH THE FOLLOWING CLAUSES:  *  RL1114.2
002400*                                                              *  RL1114.2
002500*            - ACCESS                                          *  RL1114.2
002600*            - READ                                            *  RL1114.2
002700*            - WRITE                                           *  RL1114.2
002800*            - REWRITE                                         *  RL1114.2
002900*                                                              *  RL1114.2
003000*     1) THE PROGRAM WILL CREATE A RELATIVE I-O FILE           *  RL1114.2
003100*     2) THEN IT WILL UPDATE SELECTIVE RECORDS OF THE FILE     *  RL1114.2
003200*     3) THE FILE STATUS CONTENTS ARE CAPTURED AND TESTED FOR  *  RL1114.2
003300*        ACCURACY FOR EACH "OPEN", "CLOSE", "READ" AND         *  RL1114.2
003400*        "REWRITE" STATEMENT USED.                             *  RL1114.2
003500*     4) THE "READ", "WRITE" AND "REWRITE" STATEMENT WILL BE   *  RL1114.2
003600*        USED WITH THE APPROPRIATE "AT END", "NOT AT END"      *  RL1114.2
003700*        "INVALID KEY" AND "NOT INVALID KEY" PHRASES.          *  RL1114.2
003800*                                                              *  RL1114.2
003900****************************************************************  RL1114.2
004000 ENVIRONMENT DIVISION.                                            RL1114.2
004100 CONFIGURATION SECTION.                                           RL1114.2
004200 SOURCE-COMPUTER.                                                 RL1114.2
004300     XXXXX082.                                                    RL1114.2
004400 OBJECT-COMPUTER.                                                 RL1114.2
004500     XXXXX083.                                                    RL1114.2
004600 INPUT-OUTPUT SECTION.                                            RL1114.2
004700 FILE-CONTROL.                                                    RL1114.2
004800     SELECT PRINT-FILE ASSIGN TO                                  RL1114.2
004900     XXXXX055.                                                    RL1114.2
005000     SELECT   RL-FS2 ASSIGN                                       RL1114.2
005100     XXXXX022                                                     RL1114.2
005200              ORGANIZATION RELATIVE                               RL1114.2
005300              ACCESS IS SEQUENTIAL                                RL1114.2
005400              RELATIVE KEY IS RL-FS2-KEY                          RL1114.2
005500              STATUS RL-FS2-STATUS.                               RL1114.2
005600     SELECT   RL-FS3 ASSIGN                                       RL1114.2
005700     XXXXX022                                                     RL1114.2
005800              ORGANIZATION RELATIVE                               RL1114.2
005900              ACCESS IS RANDOM                                    RL1114.2
006000              RELATIVE KEY IS RL-FS3-KEY                          RL1114.2
006100              STATUS RL-FS3-STATUS.                               RL1114.2
006200 DATA DIVISION.                                                   RL1114.2
006300 FILE SECTION.                                                    RL1114.2
006400 FD  PRINT-FILE.                                                  RL1114.2
006500 01  PRINT-REC PICTURE X(120).                                    RL1114.2
006600 01  DUMMY-RECORD PICTURE X(120).                                 RL1114.2
006700 FD  RL-FS2                                                       RL1114.2
006800C    VALUE OF                                                     RL1114.2
006900C    XXXXX074                                                     RL1114.2
007000C    IS                                                           RL1114.2
007100C    XXXXX076                                                     RL1114.2
007200*    XXXXX069                                                     RL1114.2
007300     LABEL RECORDS ARE STANDARD                                   RL1114.2
007400     BLOCK CONTAINS 1 RECORDS                                     RL1114.2
007500     DATA RECORD RL-FS2R1-F-G-240.                                RL1114.2
007600 01  RL-FS2R1-F-G-240.                                            RL1114.2
007700     05 RL-FS2-WRK-120 PIC X(120).                                RL1114.2
007800     05 RL-FS2-GRP-120.                                           RL1114.2
007900        10 RL-FS2-WRK-XN-0001-O120F                               RL1114.2
008000                        PICTURE X OCCURS 120 TIMES.               RL1114.2
008100 FD  RL-FS3                                                       RL1114.2
008200C    VALUE OF                                                     RL1114.2
008300C    XXXXX074                                                     RL1114.2
008400C    IS                                                           RL1114.2
008500C    XXXXX076                                                     RL1114.2
008600*    XXXXX069                                                     RL1114.2
008700     LABEL RECORDS ARE STANDARD                                   RL1114.2
008800     BLOCK CONTAINS 1 RECORDS                                     RL1114.2
008900     DATA RECORD RL-FS3R1-F-G-240.                                RL1114.2
009000 01  RL-FS3R1-F-G-240.                                            RL1114.2
009100     05 RL-FS3-WRK-120 PIC X(120).                                RL1114.2
009200     05 RL-FS3-GRP-120.                                           RL1114.2
009300        10 RL-FS3-WRK-XN-0001-O120F                               RL1114.2
009400                        PICTURE X OCCURS 120 TIMES.               RL1114.2
009500 WORKING-STORAGE SECTION.                                         RL1114.2
009600 01  GRP-0001.                                                    RL1114.2
009700     05 RL-FS2-KEY   PIC 9(8)  VALUE ZERO.                        RL1114.2
009800     05 RL-FS3-KEY   PIC 9(8)  VALUE ZERO.                        RL1114.2
009900     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010000     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010100     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010200     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010300     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010400     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010500     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1114.2
010600     05 RL-FS2-STATUS     PIC  XX  VALUE  SPACE.                  RL1114.2
010700     05 RL-FS3-STATUS     PIC  XX  VALUE SPACE.                   RL1114.2
010800     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1114.2
010900     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1114.2
011000     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1114.2
011100     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1114.2
011200     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1114.2
011300     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1114.2
011400     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1114.2
011500     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1114.2
011600     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1114.2
011700 01  FILE-RECORD-INFORMATION-REC.                                 RL1114.2
011800     03 FILE-RECORD-INFO-SKELETON.                                RL1114.2
011900        05 FILLER                 PICTURE X(48)       VALUE       RL1114.2
012000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1114.2
012100        05 FILLER                 PICTURE X(46)       VALUE       RL1114.2
012200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1114.2
012300        05 FILLER                 PICTURE X(26)       VALUE       RL1114.2
012400             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1114.2
012500        05 FILLER                 PICTURE X(37)       VALUE       RL1114.2
012600             ",RECKEY=                             ".             RL1114.2
012700        05 FILLER                 PICTURE X(38)       VALUE       RL1114.2
012800             ",ALTKEY1=                             ".            RL1114.2
012900        05 FILLER                 PICTURE X(38)       VALUE       RL1114.2
013000             ",ALTKEY2=                             ".            RL1114.2
013100        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1114.2
013200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1114.2
013300        05 FILE-RECORD-INFO-P1-120.                               RL1114.2
013400           07 FILLER              PIC X(5).                       RL1114.2
013500           07 XFILE-NAME           PIC X(6).                      RL1114.2
013600           07 FILLER              PIC X(8).                       RL1114.2
013700           07 XRECORD-NAME         PIC X(6).                      RL1114.2
013800           07 FILLER              PIC X(1).                       RL1114.2
013900           07 REELUNIT-NUMBER     PIC 9(1).                       RL1114.2
014000           07 FILLER              PIC X(7).                       RL1114.2
014100           07 XRECORD-NUMBER       PIC 9(6).                      RL1114.2
014200           07 FILLER              PIC X(6).                       RL1114.2
014300           07 UPDATE-NUMBER       PIC 9(2).                       RL1114.2
014400           07 FILLER              PIC X(5).                       RL1114.2
014500           07 ODO-NUMBER          PIC 9(4).                       RL1114.2
014600           07 FILLER              PIC X(5).                       RL1114.2
014700           07 XPROGRAM-NAME        PIC X(5).                      RL1114.2
014800           07 FILLER              PIC X(7).                       RL1114.2
014900           07 XRECORD-LENGTH       PIC 9(6).                      RL1114.2
015000           07 FILLER              PIC X(7).                       RL1114.2
015100           07 CHARS-OR-RECORDS    PIC X(2).                       RL1114.2
015200           07 FILLER              PIC X(1).                       RL1114.2
015300           07 XBLOCK-SIZE          PIC 9(4).                      RL1114.2
015400           07 FILLER              PIC X(6).                       RL1114.2
015500           07 RECORDS-IN-FILE     PIC 9(6).                       RL1114.2
015600           07 FILLER              PIC X(5).                       RL1114.2
015700           07 XFILE-ORGANIZATION   PIC X(2).                      RL1114.2
015800           07 FILLER              PIC X(6).                       RL1114.2
015900           07 XLABEL-TYPE          PIC X(1).                      RL1114.2
016000        05 FILE-RECORD-INFO-P121-240.                             RL1114.2
016100           07 FILLER              PIC X(8).                       RL1114.2
016200           07 XRECORD-KEY          PIC X(29).                     RL1114.2
016300           07 FILLER              PIC X(9).                       RL1114.2
016400           07 ALTERNATE-KEY1      PIC X(29).                      RL1114.2
016500           07 FILLER              PIC X(9).                       RL1114.2
016600           07 ALTERNATE-KEY2      PIC X(29).                      RL1114.2
016700           07 FILLER              PIC X(7).                       RL1114.2
016800 01  WRK-XN-00001-1              PIC X.                           RL1114.2
016900 01  WRK-XN-00001-2              PIC X.                           RL1114.2
017000 01  TEST-RESULTS.                                                RL1114.2
017100     02 FILLER                   PIC X      VALUE SPACE.          RL1114.2
017200     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1114.2
017300     02 FILLER                   PIC X      VALUE SPACE.          RL1114.2
017400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1114.2
017500     02 FILLER                   PIC X      VALUE SPACE.          RL1114.2
017600     02  PAR-NAME.                                                RL1114.2
017700       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1114.2
017800       03  PARDOT-X              PIC X      VALUE SPACE.          RL1114.2
017900       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1114.2
018000     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1114.2
018100     02 RE-MARK                  PIC X(61).                       RL1114.2
018200 01  TEST-COMPUTED.                                               RL1114.2
018300     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1114.2
018400     02 FILLER                   PIC X(17)  VALUE                 RL1114.2
018500            "       COMPUTED=".                                   RL1114.2
018600     02 COMPUTED-X.                                               RL1114.2
018700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1114.2
018800     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1114.2
018900                                 PIC -9(9).9(9).                  RL1114.2
019000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1114.2
019100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1114.2
019200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1114.2
019300     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1114.2
019400         04 COMPUTED-18V0                    PIC -9(18).          RL1114.2
019500         04 FILLER                           PIC X.               RL1114.2
019600     03 FILLER PIC X(50) VALUE SPACE.                             RL1114.2
019700 01  TEST-CORRECT.                                                RL1114.2
019800     02 FILLER PIC X(30) VALUE SPACE.                             RL1114.2
019900     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1114.2
020000     02 CORRECT-X.                                                RL1114.2
020100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1114.2
020200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1114.2
020300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1114.2
020400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1114.2
020500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1114.2
020600     03      CR-18V0 REDEFINES CORRECT-A.                         RL1114.2
020700         04 CORRECT-18V0                     PIC -9(18).          RL1114.2
020800         04 FILLER                           PIC X.               RL1114.2
020900     03 FILLER PIC X(2) VALUE SPACE.                              RL1114.2
021000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1114.2
021100 01  CCVS-C-1.                                                    RL1114.2
021200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1114.2
021300-    "SS  PARAGRAPH-NAME                                          RL1114.2
021400-    "       REMARKS".                                            RL1114.2
021500     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1114.2
021600 01  CCVS-C-2.                                                    RL1114.2
021700     02 FILLER                     PIC X        VALUE SPACE.      RL1114.2
021800     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1114.2
021900     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1114.2
022000     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1114.2
022100     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1114.2
022200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1114.2
022300 01  REC-CT                        PIC 99       VALUE ZERO.       RL1114.2
022400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1114.2
022500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1114.2
022600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1114.2
022700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1114.2
022800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1114.2
022900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1114.2
023000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1114.2
023100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1114.2
023200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1114.2
023300 01  CCVS-H-1.                                                    RL1114.2
023400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1114.2
023500     02  FILLER                    PIC X(42)    VALUE             RL1114.2
023600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1114.2
023700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1114.2
023800 01  CCVS-H-2A.                                                   RL1114.2
023900   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1114.2
024000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1114.2
024100   02  FILLER                        PIC XXXX   VALUE             RL1114.2
024200     "4.2 ".                                                      RL1114.2
024300   02  FILLER                        PIC X(28)  VALUE             RL1114.2
024400            " COPY - NOT FOR DISTRIBUTION".                       RL1114.2
024500   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1114.2
024600                                                                  RL1114.2
024700 01  CCVS-H-2B.                                                   RL1114.2
024800   02  FILLER                        PIC X(15)  VALUE             RL1114.2
024900            "TEST RESULT OF ".                                    RL1114.2
025000   02  TEST-ID                       PIC X(9).                    RL1114.2
025100   02  FILLER                        PIC X(4)   VALUE             RL1114.2
025200            " IN ".                                               RL1114.2
025300   02  FILLER                        PIC X(12)  VALUE             RL1114.2
025400     " HIGH       ".                                              RL1114.2
025500   02  FILLER                        PIC X(22)  VALUE             RL1114.2
025600            " LEVEL VALIDATION FOR ".                             RL1114.2
025700   02  FILLER                        PIC X(58)  VALUE             RL1114.2
025800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1114.2
025900 01  CCVS-H-3.                                                    RL1114.2
026000     02  FILLER                      PIC X(34)  VALUE             RL1114.2
026100            " FOR OFFICIAL USE ONLY    ".                         RL1114.2
026200     02  FILLER                      PIC X(58)  VALUE             RL1114.2
026300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1114.2
026400     02  FILLER                      PIC X(28)  VALUE             RL1114.2
026500            "  COPYRIGHT   1985 ".                                RL1114.2
026600 01  CCVS-E-1.                                                    RL1114.2
026700     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1114.2
026800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1114.2
026900     02 ID-AGAIN                     PIC X(9).                    RL1114.2
027000     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1114.2
027100 01  CCVS-E-2.                                                    RL1114.2
027200     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1114.2
027300     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1114.2
027400     02 CCVS-E-2-2.                                               RL1114.2
027500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1114.2
027600         03 FILLER                   PIC X      VALUE SPACE.      RL1114.2
027700         03 ENDER-DESC               PIC X(44)  VALUE             RL1114.2
027800            "ERRORS ENCOUNTERED".                                 RL1114.2
027900 01  CCVS-E-3.                                                    RL1114.2
028000     02  FILLER                      PIC X(22)  VALUE             RL1114.2
028100            " FOR OFFICIAL USE ONLY".                             RL1114.2
028200     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1114.2
028300     02  FILLER                      PIC X(58)  VALUE             RL1114.2
028400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1114.2
028500     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1114.2
028600     02 FILLER                       PIC X(15)  VALUE             RL1114.2
028700             " COPYRIGHT 1985".                                   RL1114.2
028800 01  CCVS-E-4.                                                    RL1114.2
028900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1114.2
029000     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1114.2
029100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1114.2
029200     02 FILLER                       PIC X(40)  VALUE             RL1114.2
029300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1114.2
029400 01  XXINFO.                                                      RL1114.2
029500     02 FILLER                       PIC X(19)  VALUE             RL1114.2
029600            "*** INFORMATION ***".                                RL1114.2
029700     02 INFO-TEXT.                                                RL1114.2
029800       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1114.2
029900       04 XXCOMPUTED                 PIC X(20).                   RL1114.2
030000       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1114.2
030100       04 XXCORRECT                  PIC X(20).                   RL1114.2
030200     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1114.2
030300 01  HYPHEN-LINE.                                                 RL1114.2
030400     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1114.2
030500     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1114.2
030600-    "*****************************************".                 RL1114.2
030700     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1114.2
030800-    "******************************".                            RL1114.2
030900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1114.2
031000     "RL111A".                                                    RL1114.2
031100 PROCEDURE DIVISION.                                              RL1114.2
031200 DECLARATIVES.                                                    RL1114.2
031300 RL-FS2-01 SECTION.                                               RL1114.2
031400     USE AFTER STANDARD EXCEPTION PROCEDURE ON RL-FS2.            RL1114.2
031500 RL-FS2-01-01.                                                    RL1114.2
031600     ADD     1 TO  WRK-CS-09V00-013.                              RL1114.2
031700     GO TO   RL-FS2-01-03                                         RL1114.2
031800             RL-FS2-01-05                                         RL1114.2
031900             DEPENDING ON  WRK-CS-09V00-012.                      RL1114.2
032000     GO TO   RL-FS2-01-EXIT.                                      RL1114.2
032100 RL-FS2-01-03.                                                    RL1114.2
032200*ENTRY FROM SEGMENT REL-TEST-009.                                 RL1114.2
032300*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     RL1114.2
032400     ADD    1   TO WRK-CS-09V00-014.                              RL1114.2
032500 RL-FS2-01-05.                                                    RL1114.2
032600     ADD     1 TO WRK-CS-09V00-017.                               RL1114.2
032700     IF      XRECORD-NUMBER (2) EQUAL TO 500                      RL1114.2
032800              MOVE   RL-FS2-STATUS TO WRK-XN-0002-002             RL1114.2
032900              MOVE  "10" TO WRK-XN-0002-003.                      RL1114.2
033000 RL-FS2-01-EXIT.                                                  RL1114.2
033100     EXIT.                                                        RL1114.2
033200 RL-FS2-02 SECTION.                                               RL1114.2
033300     USE AFTER STANDARD EXCEPTION PROCEDURE ON RL-FS3.            RL1114.2
033400 RL-FS2-02-01.                                                    RL1114.2
033500     IF      PAR-NAME = "REL-TEST-060-1"                          RL1114.2
033600             GO TO RL-FS2-02-STAT.                                RL1114.2
033700     ADD     1 TO  WRK-CS-09V00-013.                              RL1114.2
033800*    GO TO   RL-FS2-02-03                                         RL1114.2
033900*            RL-FS2-02-05                                         RL1114.2
034000*            DEPENDING ON  WRK-CS-09V00-012.                      RL1114.2
034100     GO TO D-CLOSE-FILES.                                         RL1114.2
034200 RL-FS2-02-STAT.                                                  RL1114.2
034300     IF RL-FS3-STATUS = "48"                                      RL1114.2
034400          PERFORM D-PASS                                          RL1114.2
034500     ELSE                                                         RL1114.2
034600          MOVE   "WRITE TO FILE OPENED INPUT NOT ALLOWED"         RL1114.2
034700               TO RE-MARK                                         RL1114.2
034800         PERFORM D-FAIL.                                          RL1114.2
034900     PERFORM D-PRINT-DETAIL.                                      RL1114.2
035000 D-CLOSE-FILES.                                                   RL1114.2
035100     CLOSE RL-FS3.                                                RL1114.2
035200     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 RL1114.2
035300     CLOSE PRINT-FILE.                                            RL1114.2
035400 D-TERMINATE-CCVS.                                                RL1114.2
035500*    EXIT PROGRAM.                                                RL1114.2
035600*D-TERMINATE-CALL.                                                RL1114.2
035700     STOP     RUN.                                                RL1114.2
035800 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         RL1114.2
035900 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        RL1114.2
036000 D-PRINT-DETAIL.                                                  RL1114.2
036100     IF REC-CT NOT EQUAL TO ZERO                                  RL1114.2
036200             MOVE "." TO PARDOT-X                                 RL1114.2
036300             MOVE REC-CT TO DOTVALUE.                             RL1114.2
036400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM D-WRITE-LINE.    RL1114.2
036500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM D-WRITE-LINE             RL1114.2
036600        PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX             RL1114.2
036700          ELSE PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.             RL1114.2
036800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1114.2
036900     MOVE SPACE TO CORRECT-X.                                     RL1114.2
037000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1114.2
037100     MOVE     SPACE TO RE-MARK.                                   RL1114.2
037200 D-END-ROUTINE.                                                   RL1114.2
037300     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            RL1114.2
037400     PERFORM D-WRITE-LINE 5 TIMES.                                RL1114.2
037500 D-END-RTN-EXIT.                                                  RL1114.2
037600     MOVE CCVS-E-1 TO DUMMY-RECORD.                               RL1114.2
037700     PERFORM D-WRITE-LINE 2 TIMES.                                RL1114.2
037800 D-END-ROUTINE-1.                                                 RL1114.2
037900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1114.2
038000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1114.2
038100      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1114.2
038200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1114.2
038300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1114.2
038400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1114.2
038500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.         RL1114.2
038600 D-END-ROUTINE-12.                                                RL1114.2
038700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1114.2
038800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1114.2
038900         MOVE "NO " TO ERROR-TOTAL                                RL1114.2
039000         ELSE                                                     RL1114.2
039100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1114.2
039200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1114.2
039300     PERFORM D-WRITE-LINE.                                        RL1114.2
039400 D-END-ROUTINE-13.                                                RL1114.2
039500     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1114.2
039600         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1114.2
039700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1114.2
039800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1114.2
039900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         RL1114.2
040000      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1114.2
040100          MOVE "NO " TO ERROR-TOTAL                               RL1114.2
040200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1114.2
040300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1114.2
040400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.        RL1114.2
040500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         RL1114.2
040600 D-WRITE-LINE.                                                    RL1114.2
040700     ADD 1 TO RECORD-COUNT.                                       RL1114.2
040800*    IF RECORD-COUNT GREATER 50                                   RL1114.2
040900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1114.2
041000*        MOVE SPACE TO DUMMY-RECORD                               RL1114.2
041100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1114.2
041200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM D-WRT-LN           RL1114.2
041300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   RL1114.2
041400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN        RL1114.2
041500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1114.2
041600*        MOVE ZERO TO RECORD-COUNT.                               RL1114.2
041700     PERFORM D-WRT-LN.                                            RL1114.2
041800 D-WRT-LN.                                                        RL1114.2
041900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1114.2
042000     MOVE SPACE TO DUMMY-RECORD.                                  RL1114.2
042100 D-FAIL-ROUTINE.                                                  RL1114.2
042200     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1114.2
042300            GO TO  D-FAIL-ROUTINE-WRITE.                          RL1114.2
042400     IF     CORRECT-X NOT EQUAL TO SPACE                          RL1114.2
042500            GO TO D-FAIL-ROUTINE-WRITE.                           RL1114.2
042600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1114.2
042700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1114.2
042800     MOVE   XXINFO TO DUMMY-RECORD.                               RL1114.2
042900     PERFORM D-WRITE-LINE 2 TIMES.                                RL1114.2
043000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1114.2
043100     GO TO  D-FAIL-ROUTINE-EX.                                    RL1114.2
043200 D-FAIL-ROUTINE-WRITE.                                            RL1114.2
043300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE       RL1114.2
043400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1114.2
043500     MOVE   TEST-CORRECT TO PRINT-REC                             RL1114.2
043600     PERFORM D-WRITE-LINE 2 TIMES.                                RL1114.2
043700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1114.2
043800 D-FAIL-ROUTINE-EX. EXIT.                                         RL1114.2
043900 D-BAIL-OUT.                                                      RL1114.2
044000     IF   COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.   RL1114.2
044100     IF     CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.         RL1114.2
044200 D-BAIL-OUT-WRITE.                                                RL1114.2
044300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1114.2
044400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1114.2
044500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES. RL1114.2
044600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1114.2
044700 D-BAIL-OUT-EX. EXIT.                                             RL1114.2
044800 RL-FS2-02-03.                                                    RL1114.2
044900*ENTRY FROM SEGMENT REL-TEST-009.                                 RL1114.2
045000*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     RL1114.2
045100     ADD    1   TO WRK-CS-09V00-014.                              RL1114.2
045200 RL-FS2-02-05.                                                    RL1114.2
045300     ADD     1 TO WRK-CS-09V00-017.                               RL1114.2
045400     IF      XRECORD-NUMBER (2) EQUAL TO 500                      RL1114.2
045500              MOVE   RL-FS2-STATUS TO WRK-XN-0002-002             RL1114.2
045600              MOVE  "10" TO WRK-XN-0002-003.                      RL1114.2
045700 RL-FS2-02-EXIT.                                                  RL1114.2
045800     EXIT.                                                        RL1114.2
045900 END DECLARATIVES.                                                RL1114.2
046000 CCVS1 SECTION.                                                   RL1114.2
046100 OPEN-FILES.                                                      RL1114.2
046200     OPEN    OUTPUT PRINT-FILE.                                   RL1114.2
046300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1114.2
046400     MOVE    SPACE TO TEST-RESULTS.                               RL1114.2
046500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1114.2
046600     MOVE    ZERO TO REC-SKL-SUB.                                 RL1114.2
046700     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1114.2
046800 CCVS-INIT-FILE.                                                  RL1114.2
046900     ADD     1 TO REC-SKL-SUB.                                    RL1114.2
047000     MOVE    FILE-RECORD-INFO-SKELETON                            RL1114.2
047100          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1114.2
047200 CCVS-INIT-EXIT.                                                  RL1114.2
047300     GO TO CCVS1-EXIT.                                            RL1114.2
047400 CLOSE-FILES.                                                     RL1114.2
047500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1114.2
047600 TERMINATE-CCVS.                                                  RL1114.2
047700*    EXIT PROGRAM.                                                RL1114.2
047800*TERMINATE-CALL.                                                  RL1114.2
047900     STOP     RUN.                                                RL1114.2
048000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1114.2
048100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1114.2
048200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1114.2
048300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1114.2
048400     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1114.2
048500 PRINT-DETAIL.                                                    RL1114.2
048600     IF REC-CT NOT EQUAL TO ZERO                                  RL1114.2
048700             MOVE "." TO PARDOT-X                                 RL1114.2
048800             MOVE REC-CT TO DOTVALUE.                             RL1114.2
048900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1114.2
049000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1114.2
049100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1114.2
049200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1114.2
049300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1114.2
049400     MOVE SPACE TO CORRECT-X.                                     RL1114.2
049500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1114.2
049600     MOVE     SPACE TO RE-MARK.                                   RL1114.2
049700 HEAD-ROUTINE.                                                    RL1114.2
049800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1114.2
049900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1114.2
050000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1114.2
050100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1114.2
050200 COLUMN-NAMES-ROUTINE.                                            RL1114.2
050300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1114.2
050400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1114.2
050500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1114.2
050600 END-ROUTINE.                                                     RL1114.2
050700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1114.2
050800 END-RTN-EXIT.                                                    RL1114.2
050900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1114.2
051000 END-ROUTINE-1.                                                   RL1114.2
051100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1114.2
051200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1114.2
051300      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1114.2
051400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1114.2
051500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1114.2
051600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1114.2
051700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1114.2
051800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1114.2
051900  END-ROUTINE-12.                                                 RL1114.2
052000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1114.2
052100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1114.2
052200         MOVE "NO " TO ERROR-TOTAL                                RL1114.2
052300         ELSE                                                     RL1114.2
052400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1114.2
052500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1114.2
052600     PERFORM WRITE-LINE.                                          RL1114.2
052700 END-ROUTINE-13.                                                  RL1114.2
052800     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1114.2
052900         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1114.2
053000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1114.2
053100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1114.2
053200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1114.2
053300      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1114.2
053400          MOVE "NO " TO ERROR-TOTAL                               RL1114.2
053500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1114.2
053600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1114.2
053700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1114.2
053800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1114.2
053900 WRITE-LINE.                                                      RL1114.2
054000     ADD 1 TO RECORD-COUNT.                                       RL1114.2
054100*    IF RECORD-COUNT GREATER 50                                   RL1114.2
054200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1114.2
054300*        MOVE SPACE TO DUMMY-RECORD                               RL1114.2
054400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1114.2
054500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1114.2
054600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1114.2
054700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1114.2
054800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1114.2
054900*        MOVE ZERO TO RECORD-COUNT.                               RL1114.2
055000     PERFORM WRT-LN.                                              RL1114.2
055100 WRT-LN.                                                          RL1114.2
055200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1114.2
055300     MOVE SPACE TO DUMMY-RECORD.                                  RL1114.2
055400 BLANK-LINE-PRINT.                                                RL1114.2
055500     PERFORM WRT-LN.                                              RL1114.2
055600 FAIL-ROUTINE.                                                    RL1114.2
055700     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1114.2
055800            GO TO   FAIL-ROUTINE-WRITE.                           RL1114.2
055900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1114.2
056000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1114.2
056100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1114.2
056200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1114.2
056300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1114.2
056400     GO TO  FAIL-ROUTINE-EX.                                      RL1114.2
056500 FAIL-ROUTINE-WRITE.                                              RL1114.2
056600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1114.2
056700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1114.2
056800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1114.2
056900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1114.2
057000 FAIL-ROUTINE-EX. EXIT.                                           RL1114.2
057100 BAIL-OUT.                                                        RL1114.2
057200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1114.2
057300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1114.2
057400 BAIL-OUT-WRITE.                                                  RL1114.2
057500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1114.2
057600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1114.2
057700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1114.2
057800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1114.2
057900 BAIL-OUT-EX. EXIT.                                               RL1114.2
058000 CCVS1-EXIT.                                                      RL1114.2
058100     EXIT.                                                        RL1114.2
058200 SECT-RL-04-001 SECTION.                                          RL1114.2
058300 REL-INIT-009.                                                    RL1114.2
058400     MOVE    "REL-TEST-009"   TO  PAR-NAME.                       RL1114.2
058500     MOVE     "CREATE RL-FS2" TO  FEATURE                         RL1114.2
058600     MOVE     "RL-FS2"        TO  XFILE-NAME (2).                 RL1114.2
058700     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL1114.2
058800     MOVE CCVS-PGM-ID         TO  XPROGRAM-NAME (2).              RL1114.2
058900     MOVE     000240          TO  XRECORD-LENGTH (2).             RL1114.2
059000     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL1114.2
059100     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL1114.2
059200     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL1114.2
059300     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL1114.2
059400     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL1114.2
059500     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL1114.2
059600*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1114.2
059700     MOVE     1         TO  WRK-CS-09V00-012.                     RL1114.2
059800     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1114.2
059900                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1114.2
060000                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1114.2
060100     MOVE     SPACE     TO  RL-FS2-STATUS.                        RL1114.2
060200     MOVE     01 TO REC-CT.                                       RL1114.2
060300     OPEN     OUTPUT    RL-FS2.                                   RL1114.2
060400     MOVE     RL-FS2-STATUS TO WRK-XN-0002-001.                   RL1114.2
060500*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL1114.2
060600 REL-TEST-009-R.                                                  RL1114.2
060700     MOVE    "99"  TO RL-FS2-STATUS.                              RL1114.2
060800     MOVE     FILE-RECORD-INFO-P1-120 (2) TO RL-FS2-WRK-120.      RL1114.2
060900     MOVE     FILE-RECORD-INFO-P1-120 (2) TO                      RL1114.2
061000               RL-FS2-GRP-120.                                    RL1114.2
061100     WRITE    RL-FS2R1-F-G-240.                                   RL1114.2
061200     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1114.2
061300              GO TO  REL-TEST-009-2.                              RL1114.2
061400     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL1114.2
061500             GO TO REL-TEST-009-2.                                RL1114.2
061600     ADD      01 TO  XRECORD-NUMBER (2).                          RL1114.2
061700     GO TO    REL-TEST-009-R.                                     RL1114.2
061800 REL-TEST-009-2.                                                  RL1114.2
061900     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  RL1114.2
062000              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 RL1114.2
062100              MOVE ZERO TO CORRECT-18V0                           RL1114.2
062200              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1114.2
062300              PERFORM   FAIL                                      RL1114.2
062400     ELSE                                                         RL1114.2
062500              PERFORM   PASS.                                     RL1114.2
062600     PERFORM  PRINT-DETAIL.                                       RL1114.2
062700     ADD      01 TO REC-CT.                                       RL1114.2
062800*    .01                                                          RL1114.2
062900     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 RL1114.2
063000              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1114.2
063100              MOVE  500 TO CORRECT-18V0                           RL1114.2
063200             MOVE    XRECORD-NUMBER (2)  TO COMPUTED-18V0         RL1114.2
063300              PERFORM  FAIL                                       RL1114.2
063400              ELSE                                                RL1114.2
063500              PERFORM PASS.                                       RL1114.2
063600     PERFORM  PRINT-DETAIL.                                       RL1114.2
063700     ADD      01 TO REC-CT.                                       RL1114.2
063800*    .02                                                          RL1114.2
063900     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  RL1114.2
064000              MOVE "STATUS/OPEN" TO RE-MARK                       RL1114.2
064100              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1114.2
064200              MOVE "00" TO CORRECT-A                              RL1114.2
064300              PERFORM  FAIL                                       RL1114.2
064400              ELSE                                                RL1114.2
064500              PERFORM  PASS.                                      RL1114.2
064600     PERFORM  PRINT-DETAIL.                                       RL1114.2
064700     ADD      01 TO REC-CT.                                       RL1114.2
064800*    .03                                                          RL1114.2
064900     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1114.2
065000              MOVE "STATUS/WRITE" TO RE-MARK                      RL1114.2
065100              MOVE RL-FS2-STATUS TO COMPUTED-A                    RL1114.2
065200              MOVE "00" TO CORRECT-A                              RL1114.2
065300              PERFORM   FAIL                                      RL1114.2
065400              ELSE                                                RL1114.2
065500              PERFORM   PASS.                                     RL1114.2
065600     PERFORM  PRINT-DETAIL.                                       RL1114.2
065700     ADD      01 TO REC-CT.                                       RL1114.2
065800*    .04                                                          RL1114.2
065900     MOVE     SPACE  TO  RL-FS2-STATUS.                           RL1114.2
066000     CLOSE    RL-FS2.                                             RL1114.2
066100     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1114.2
066200              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1114.2
066300              MOVE   RL-FS2-STATUS TO COMPUTED-A                  RL1114.2
066400              MOVE "00" TO CORRECT-A                              RL1114.2
066500              PERFORM FAIL                                        RL1114.2
066600              ELSE                                                RL1114.2
066700              PERFORM PASS.                                       RL1114.2
066800     PERFORM  PRINT-DETAIL.                                       RL1114.2
066900     ADD      01 TO REC-CT.                                       RL1114.2
067000*    .05                                                          RL1114.2
067100 REL-INIT-010.                                                    RL1114.2
067200     MOVE    "REL-TEST-010" TO PAR-NAME.                          RL1114.2
067300     MOVE     2     TO  WRK-CS-09V00-012.                         RL1114.2
067400     MOVE     ZERO  TO  WRK-CS-09V00-013.                         RL1114.2
067500     MOVE     ZERO  TO  WRK-CS-09V00-014.                         RL1114.2
067600     MOVE     ZERO  TO  WRK-CS-09V00-015.                         RL1114.2
067700     MOVE     ZERO  TO  WRK-CS-09V00-016.                         RL1114.2
067800     MOVE     ZERO  TO  WRK-CS-09V00-017.                         RL1114.2
067900     MOVE     ZERO  TO  WRK-CS-09V00-018.                         RL1114.2
068000     MOVE     01 TO REC-CT.                                       RL1114.2
068100     OPEN     I-O  RL-FS2.                                        RL1114.2
068200     MOVE     SPACE     TO  WRK-XN-0002-002                       RL1114.2
068300     MOVE     SPACE     TO  WRK-XN-0002-003                       RL1114.2
068400     MOVE     SPACE     TO  WRK-XN-0002-004                       RL1114.2
068500     MOVE      RL-FS2-STATUS TO WRK-XN-0002-001                   RL1114.2
068600     MOVE     SPACE TO RL-FS2-STATUS.                             RL1114.2
068700*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       RL1114.2
068800     MOVE    "USE/FILE STATUS"  TO FEATURE.                       RL1114.2
068900 REL-TEST-010-R.                                                  RL1114.2
069000     MOVE    "REL-TEST-010-R" TO PAR-NAME.                        RL1114.2
069100     MOVE    "VIII-26 4.5.2" TO ANSI-REFERENCE.                   RL1114.2
069200     ADD      1 TO WRK-CS-09V00-014.                              RL1114.2
069300     ADD      1 TO WRK-CS-09V00-015.                              RL1114.2
069400     READ     RL-FS2.                                             RL1114.2
069500     IF        RL-FS2-STATUS EQUAL TO "10"                        RL1114.2
069600              GO TO  REL-TEST-010-3.                              RL1114.2
069700     MOVE     RL-FS2-WRK-120 TO FILE-RECORD-INFO-P1-120 (2).      RL1114.2
069800     IF       WRK-CS-09V00-015 EQUAL TO 5                         RL1114.2
069900              ADD 01 TO UPDATE-NUMBER (2)                         RL1114.2
070000              MOVE FILE-RECORD-INFO-P1-120 (2) TO RL-FS2-WRK-120  RL1114.2
070100              REWRITE RL-FS2R1-F-G-240                            RL1114.2
070200           MOVE ZERO TO WRK-CS-09V00-015                          RL1114.2
070300              GO TO  REL-TEST-010-2.                              RL1114.2
070400     IF       WRK-CS-09V00-014 GREATER 500                        RL1114.2
070500              GO TO  REL-TEST-010-3.                              RL1114.2
070600     GO TO    REL-TEST-010-R.                                     RL1114.2
070700 REL-TEST-010-2.                                                  RL1114.2
070800     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1114.2
070900              ADD 1 TO WRK-CS-09V00-016.                          RL1114.2
071000     MOVE    SPACE TO RL-FS2-STATUS.                              RL1114.2
071100     GO TO    REL-TEST-010-R.                                     RL1114.2
071200 REL-TEST-010-3.                                                  RL1114.2
071300     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     RL1114.2
071400              MOVE  "EXCEPTIONS/ERRORS" TO RE-MARK                RL1114.2
071500              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              RL1114.2
071600              MOVE  1 TO CORRECT-18V0                             RL1114.2
071700              PERFORM FAIL                                        RL1114.2
071800              ELSE                                                RL1114.2
071900              PERFORM PASS.                                       RL1114.2
072000     PERFORM  PRINT-DETAIL.                                       RL1114.2
072100     ADD      01 TO REC-CT.                                       RL1114.2
072200*    .01                                                          RL1114.2
072300     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   RL1114.2
072400              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1114.2
072500              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1114.2
072600              MOVE 501 TO CORRECT-18V0                            RL1114.2
072700              PERFORM FAIL                                        RL1114.2
072800              ELSE                                                RL1114.2
072900             PERFORM PASS.                                        RL1114.2
073000     PERFORM  PRINT-DETAIL.                                       RL1114.2
073100     ADD      01 TO REC-CT.                                       RL1114.2
073200*    .02                                                          RL1114.2
073300     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   RL1114.2
073400              MOVE "OPEN/STATUS"  TO  RE-MARK                     RL1114.2
073500              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1114.2
073600              MOVE  "00" TO CORRECT-A                             RL1114.2
073700              PERFORM FAIL                                        RL1114.2
073800              ELSE                                                RL1114.2
073900              PERFORM PASS.                                       RL1114.2
074000     PERFORM  PRINT-DETAIL.                                       RL1114.2
074100     ADD      01 TO REC-CT.                                       RL1114.2
074200*    .03                                                          RL1114.2
074300     IF        RL-FS2-STATUS NOT EQUAL  TO "10"                   RL1114.2
074400              MOVE "AT END/STATUS" TO RE-MARK                     RL1114.2
074500              MOVE  RL-FS2-STATUS TO COMPUTED-A                   RL1114.2
074600              MOVE "10" TO CORRECT-A                              RL1114.2
074700              PERFORM FAIL                                        RL1114.2
074800              ELSE                                                RL1114.2
074900              PERFORM PASS.                                       RL1114.2
075000     PERFORM  PRINT-DETAIL.                                       RL1114.2
075100     ADD      01 TO REC-CT.                                       RL1114.2
075200*    .04                                                          RL1114.2
075300     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   RL1114.2
075400              MOVE  "EXCEPTION/STATUS" TO RE-MARK                 RL1114.2
075500              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 RL1114.2
075600              MOVE  "10" TO CORRECT-A                             RL1114.2
075700              PERFORM FAIL                                        RL1114.2
075800              ELSE                                                RL1114.2
075900              PERFORM PASS.                                       RL1114.2
076000     PERFORM  PRINT-DETAIL.                                       RL1114.2
076100     ADD      01 TO REC-CT.                                       RL1114.2
076200*    .05                                                          RL1114.2
076300     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   RL1114.2
076400              MOVE "NO/EXCEPTION" TO  RE-MARK                     RL1114.2
076500              MOVE WRK-XN-0002-003 TO COMPUTED-A                  RL1114.2
076600              MOVE "10" TO CORRECT-A                              RL1114.2
076700              PERFORM FAIL                                        RL1114.2
076800              ELSE                                                RL1114.2
076900              PERFORM PASS.                                       RL1114.2
077000     PERFORM  PRINT-DETAIL                                        RL1114.2
077100     ADD      01 TO REC-CT.                                       RL1114.2
077200*    .06                                                          RL1114.2
077300     MOVE     SPACE TO RL-FS2-STATUS.                             RL1114.2
077400     CLOSE    RL-FS2                                              RL1114.2
077500     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1114.2
077600              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1114.2
077700              MOVE  RL-FS2-STATUS TO  COMPUTED-A                  RL1114.2
077800              MOVE "00" TO CORRECT-A                              RL1114.2
077900              PERFORM FAIL                                        RL1114.2
078000              ELSE                                                RL1114.2
078100              PERFORM PASS.                                       RL1114.2
078200     PERFORM  PRINT-DETAIL.                                       RL1114.2
078300     ADD      01 TO REC-CT.                                       RL1114.2
078400*    .07                                                          RL1114.2
078500*                                                                 RL1114.2
078600*                                                                 RL1114.2
078700 SECT-RL111-003-COBOL8X SECTION.                                  RL1114.2
078800*==============================                                   RL1114.2
078900*                                                                 RL1114.2
079000****************************************************************  RL1114.2
079100*                                                              *  RL1114.2
079200*    THIS SECTION CONTAINS THE ADDITIONAL CODING/TESTS FOR     *  RL1114.2
079300*    THE NEW COBOL '85 FEATURES.                               *  RL1114.2
079400*                                                              *  RL1114.2
079500****************************************************************  RL1114.2
079600 REL-INIT-020.                                                    RL1114.2
079700     MOVE   "VIII-26 4.5.2"  TO ANSI-REFERENCE.                   RL1114.2
079800     MOVE   "NEW COBOL85 TESTS" TO FEATURE.                       RL1114.2
079900     MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
080000     MOVE    0 TO REC-CT.                                         RL1114.2
080100     MOVE    SPACES TO RL-FS2-STATUS.                             RL1114.2
080200     MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
080300     OPEN    I-O RL-FS2.                                          RL1114.2
080400 REL-TEST-020-1.                                                  RL1114.2
080500     MOVE   "REL-TEST-020-1" TO PAR-NAME.                         RL1114.2
080600     IF      RL-FS2-STATUS = "00"                                 RL1114.2
080700             PERFORM PASS                                         RL1114.2
080800             PERFORM PRINT-DETAIL                                 RL1114.2
080900     ELSE                                                         RL1114.2
081000             MOVE   "INVALID OPEN" TO RE-MARK                     RL1114.2
081100             MOVE   "00"           TO CORRECT-X                   RL1114.2
081200             MOVE    RL-FS2-STATUS TO COMPUTED-X                  RL1114.2
081300             PERFORM FAIL                                         RL1114.2
081400             PERFORM PRINT-DETAIL.                                RL1114.2
081500 REL-TEST-020-2.                                                  RL1114.2
081600     MOVE   "REL-TEST-020-2" TO PAR-NAME.                         RL1114.2
081700     READ    RL-FS2  INTO FILE-RECORD-INFO-P1-120 (2)             RL1114.2
081800             END                                                  RL1114.2
081900             MOVE   "END ENCOUNTERED ON FIRST READ" TO RE-MARK    RL1114.2
082000             PERFORM FAIL                                         RL1114.2
082100             PERFORM PRINT-DETAIL                                 RL1114.2
082200             NOT AT END                                           RL1114.2
082300             PERFORM PASS                                         RL1114.2
082400             PERFORM PRINT-DETAIL                                 RL1114.2
082500     END-READ                                                     RL1114.2
082600     MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
082700 REL-TEST-020-3.                                                  RL1114.2
082800     MOVE   "REL-TEST-020-3" TO PAR-NAME.                         RL1114.2
082900     IF      XRECORD-NUMBER (2) = 1                               RL1114.2
083000             PERFORM PASS                                         RL1114.2
083100             PERFORM PRINT-DETAIL                                 RL1114.2
083200     ELSE                                                         RL1114.2
083300             MOVE   "FIRST RECORD NOT READ ON FIRST READ"         RL1114.2
083400                  TO RE-MARK                                      RL1114.2
083500             MOVE    1             TO CORRECT-N                   RL1114.2
083600             MOVE    XRECORD-NUMBER (2) TO COMPUTED-N             RL1114.2
083700             PERFORM FAIL                                         RL1114.2
083800             PERFORM PRINT-DETAIL.                                RL1114.2
083900 REL-TEST-020-4.                                                  RL1114.2
084000     MOVE   "REL-TEST-020-4" TO PAR-NAME.                         RL1114.2
084100     IF      WRK-XN-00001-1 = "X"                                 RL1114.2
084200             PERFORM PASS                                         RL1114.2
084300             PERFORM PRINT-DETAIL                                 RL1114.2
084400     ELSE                                                         RL1114.2
084500             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
084600             MOVE   "X"             TO CORRECT-X                  RL1114.2
084700             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
084800             PERFORM FAIL                                         RL1114.2
084900             PERFORM PRINT-DETAIL.                                RL1114.2
085000*                                                                 RL1114.2
085100 REL-INIT-030.                                                    RL1114.2
085200     MOVE   "VIII-29 4.5.4"  TO ANSI-REFERENCE.                   RL1114.2
085300     MOVE    0 TO REC-CT.                                         RL1114.2
085400     MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
085500     MOVE    SPACES TO RL-FS2-STATUS.                             RL1114.2
085600     MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
085700 REL-TEST-030-1.                                                  RL1114.2
085800     MOVE   "REL-TEST-030-1" TO PAR-NAME.                         RL1114.2
085900     READ    RL-FS2                                               RL1114.2
086000             END                                                  RL1114.2
086100             MOVE   "END ENCOUNTERED ON SECOND READ" TO RE-MARK   RL1114.2
086200             PERFORM FAIL                                         RL1114.2
086300             PERFORM PRINT-DETAIL                                 RL1114.2
086400             NOT AT END                                           RL1114.2
086500             PERFORM PASS                                         RL1114.2
086600             PERFORM PRINT-DETAIL                                 RL1114.2
086700     END-READ                                                     RL1114.2
086800     MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
086900 REL-TEST-030-2.                                                  RL1114.2
087000     MOVE   "REL-TEST-030-2" TO PAR-NAME.                         RL1114.2
087100     MOVE    RL-FS2-WRK-120  TO FILE-RECORD-INFO-P1-120 (2).      RL1114.2
087200     IF      XRECORD-NUMBER (2) = 2                               RL1114.2
087300             PERFORM PASS                                         RL1114.2
087400             PERFORM PRINT-DETAIL                                 RL1114.2
087500     ELSE                                                         RL1114.2
087600             MOVE   "SECOND RECORD NOT READ ON SECOND READ"       RL1114.2
087700                  TO RE-MARK                                      RL1114.2
087800             MOVE    2             TO CORRECT-N                   RL1114.2
087900             MOVE    XRECORD-NUMBER (2) TO COMPUTED-N             RL1114.2
088000             PERFORM FAIL                                         RL1114.2
088100             PERFORM PRINT-DETAIL.                                RL1114.2
088200 REL-TEST-030-3.                                                  RL1114.2
088300     MOVE   "REL-TEST-030-3" TO PAR-NAME.                         RL1114.2
088400     IF      WRK-XN-00001-1 = "X"                                 RL1114.2
088500             PERFORM PASS                                         RL1114.2
088600             PERFORM PRINT-DETAIL                                 RL1114.2
088700     ELSE                                                         RL1114.2
088800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
088900             MOVE   "X"             TO CORRECT-X                  RL1114.2
089000             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
089100             PERFORM FAIL                                         RL1114.2
089200             PERFORM PRINT-DETAIL.                                RL1114.2
089300*                                                                 RL1114.2
089400 REL-INIT-040.                                                    RL1114.2
089500     CLOSE   RL-FS2.                                              RL1114.2
089600     OPEN    I-O RL-FS3.                                          RL1114.2
089700     MOVE   "VIII-29 4.5.4"  TO ANSI-REFERENCE.                   RL1114.2
089800     MOVE    0 TO REC-CT.                                         RL1114.2
089900     MOVE    2 TO RL-FS3-KEY.                                     RL1114.2
090000     MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
090100     MOVE    SPACES TO RL-FS3-STATUS.                             RL1114.2
090200     MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
090300 REL-TEST-040-1.                                                  RL1114.2
090400     MOVE   "REL-TEST-040-1" TO PAR-NAME.                         RL1114.2
090500     WRITE   RL-FS3R1-F-G-240                                     RL1114.2
090600             INVALID                                              RL1114.2
090700             PERFORM PASS                                         RL1114.2
090800             PERFORM PRINT-DETAIL                                 RL1114.2
090900             NOT INVALID                                          RL1114.2
091000             MOVE   "DUPLICATE KEY SHOULD NOT HAVE BEEN WRITTEN"  RL1114.2
091100                  TO RE-MARK                                      RL1114.2
091200             PERFORM FAIL                                         RL1114.2
091300             PERFORM PRINT-DETAIL                                 RL1114.2
091400     END-WRITE                                                    RL1114.2
091500     MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
091600 REL-TEST-040-2.                                                  RL1114.2
091700     MOVE   "REL-TEST-040-2" TO PAR-NAME.                         RL1114.2
091800     IF      WRK-XN-00001-1 = "X"                                 RL1114.2
091900             PERFORM PASS                                         RL1114.2
092000             PERFORM PRINT-DETAIL                                 RL1114.2
092100     ELSE                                                         RL1114.2
092200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
092300             MOVE   "X"             TO CORRECT-X                  RL1114.2
092400             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
092500             PERFORM FAIL                                         RL1114.2
092600             PERFORM PRINT-DETAIL.                                RL1114.2
092700*                                                                 RL1114.2
092800 REL-INIT-050.                                                    RL1114.2
092900     MOVE   "VIII-38 4.9.4 GR9(A)"  TO ANSI-REFERENCE.            RL1114.2
093000     MOVE    0 TO REC-CT.                                         RL1114.2
093100     MOVE    600  TO RL-FS3-KEY.                                  RL1114.2
093200     MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
093300     MOVE    SPACES TO RL-FS3-STATUS.                             RL1114.2
093400     MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
093500 REL-TEST-050-1.                                                  RL1114.2
093600     MOVE   "REL-TEST-050-1" TO PAR-NAME.                         RL1114.2
093700     WRITE   RL-FS3R1-F-G-240                                     RL1114.2
093800             INVALID                                              RL1114.2
093900             MOVE   "NEW KEY, RECORD SHOULD HAVE BEEN WRITTEN OK" RL1114.2
094000                  TO RE-MARK                                      RL1114.2
094100             PERFORM FAIL                                         RL1114.2
094200             PERFORM PRINT-DETAIL                                 RL1114.2
094300             NOT INVALID                                          RL1114.2
094400             PERFORM PASS                                         RL1114.2
094500             PERFORM PRINT-DETAIL                                 RL1114.2
094600     END-WRITE                                                    RL1114.2
094700     MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
094800 REL-TEST-050-2.                                                  RL1114.2
094900     MOVE   "REL-TEST-050-2" TO PAR-NAME.                         RL1114.2
095000     IF      WRK-XN-00001-1 = "X"                                 RL1114.2
095100             PERFORM PASS                                         RL1114.2
095200             PERFORM PRINT-DETAIL                                 RL1114.2
095300     ELSE                                                         RL1114.2
095400             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
095500             MOVE   "X"             TO CORRECT-X                  RL1114.2
095600             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
095700             PERFORM FAIL                                         RL1114.2
095800             PERFORM PRINT-DETAIL.                                RL1114.2
095900*                                                                 RL1114.2
096000 REL-INIT-060.                                                    RL1114.2
096100     MOVE   "VIII-38 4.5.4 GR9(B)"  TO ANSI-REFERENCE.            RL1114.2
096200     CLOSE   RL-FS3.                                              RL1114.2
096300     OPEN    INPUT RL-FS3.                                        RL1114.2
096400     MOVE    0 TO REC-CT.                                         RL1114.2
096500     MOVE    99999998 TO RL-FS3-KEY.                              RL1114.2
096600     MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
096700     MOVE    SPACES TO RL-FS3-STATUS.                             RL1114.2
096800     MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
096900 REL-TEST-060-1.                                                  RL1114.2
097000     MOVE   "REL-TEST-060-1" TO PAR-NAME.                         RL1114.2
097100     WRITE   RL-FS3R1-F-G-240.                                    RL1114.2
097200     IF RL-FS3-STATUS NOT = "48"                                  RL1114.2
097300          MOVE   "WRITE TO FILE OPENED INPUT NOT ALLOWED"         RL1114.2
097400               TO RE-MARK                                         RL1114.2
097500     ELSE                                                         RL1114.2
097600          MOVE "SHOULD HAVE ACTIONED DECLARATIVES" TO RE-MARK.    RL1114.2
097700     PERFORM FAIL                                                 RL1114.2
097800     PERFORM PRINT-DETAIL.                                        RL1114.2
097900* ENTRY TO THE DECLARATIVES CLOSES ALL FILES AND                  RL1114.2
098000* TERMINATES THE PROGRAM.                                         RL1114.2
098100* EXECUTION SHOULD REACH THIS POINT ONLY AS RESULT OF AN ERROR    RL1114.2
098200     CLOSE RL-FS3.                                                RL1114.2
098300*                                                                 RL1114.2
098400 REL-INIT-070.                                                    RL1114.2
098500*    MOVE   "VIII-30 4.6.2"  TO ANSI-REFERENCE.                   RL1114.2
098600*    OPEN    I-O RL-FS3.                                          RL1114.2
098700*    MOVE    0 TO REC-CT.                                         RL1114.2
098800*    MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
098900*    MOVE    SPACES TO RL-FS3-STATUS.                             RL1114.2
099000*    MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
099100*REL-TEST-070-1.                                                  RL1114.2
099200*    MOVE   "REL-TEST-070-1" TO PAR-NAME.                         RL1114.2
099300*    READ    RL-FS3                                               RL1114.2
099400*            INVALID                                              RL1114.2
099500*            MOVE   "INVALID KEY ON FIRST READ" TO RE-MARK        RL1114.2
099600*            PERFORM FAIL                                         RL1114.2
099700*            PERFORM PRINT-DETAIL                                 RL1114.2
099800*            NOT INVALID KEY                                      RL1114.2
099900*            PERFORM PASS                                         RL1114.2
100000*            PERFORM PRINT-DETAIL                                 RL1114.2
100100*    END-READ                                                     RL1114.2
100200*    MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
100300*REL-TEST-070-2.                                                  RL1114.2
100400*    MOVE   "REL-TEST-070-2" TO PAR-NAME.                         RL1114.2
100500*    IF      WRK-XN-00001-1 = "X"                                 RL1114.2
100600*            PERFORM PASS                                         RL1114.2
100700*            PERFORM PRINT-DETAIL                                 RL1114.2
100800*    ELSE                                                         RL1114.2
100900*            MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
101000*            MOVE   "X"             TO CORRECT-X                  RL1114.2
101100*            MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
101200*            PERFORM FAIL                                         RL1114.2
101300*            PERFORM PRINT-DETAIL.                                RL1114.2
101400*REL-TEST-070-3.                                                  RL1114.2
101500*    MOVE   "REL-TEST-070-3" TO PAR-NAME.                         RL1114.2
101600*                                                                 RL1114.2
101700*    IF      WRK-XN-00001-2 = "@"                                 RL1114.2
101800*            PERFORM PASS                                         RL1114.2
101900*            PERFORM PRINT-DETAIL                                 RL1114.2
102000*    ELSE                                                         RL1114.2
102100*            MOVE   "'USE AFTER' PROCEDURE NOT ACTIONED"          RL1114.2
102200*                 TO RE-MARK                                      RL1114.2
102300*            MOVE   "@"             TO CORRECT-X                  RL1114.2
102400*            MOVE    WRK-XN-00001-2 TO COMPUTED-X                 RL1114.2
102500*            PERFORM FAIL                                         RL1114.2
102600*            PERFORM PRINT-DETAIL.                                RL1114.2
102700*                                                                 RL1114.2
102800*                                                                 RL1114.2
102900*REL-INIT-080.                                                    RL1114.2
103000*    MOVE   "VIII-30 4.6.2"  TO ANSI-REFERENCE.                   RL1114.2
103100*    CLOSE   RL-FS3.                                              RL1114.2
103200*    OPEN    I-O RL-FS3.                                          RL1114.2
103300*    MOVE    0 TO REC-CT.                                         RL1114.2
103400*    MOVE    900000002 TO RL-FS3-KEY.                             RL1114.2
103500*    MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
103600*    MOVE    SPACES TO RL-FS3-STATUS.                             RL1114.2
103700*    MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
103800*REL-TEST-080-1.                                                  RL1114.2
103900*    MOVE   "REL-TEST-080-1" TO PAR-NAME.                         RL1114.2
104000*    READ    RL-FS3                                               RL1114.2
104100*            INVALID                                              RL1114.2
104200*            MOVE   "INVALID KEY ON FIRST READ" TO RE-MARK        RL1114.2
104300*            PERFORM FAIL                                         RL1114.2
104400*            PERFORM PRINT-DETAIL                                 RL1114.2
104500*            NOT INVALID KEY                                      RL1114.2
104600*            PERFORM PASS                                         RL1114.2
104700*            PERFORM PRINT-DETAIL                                 RL1114.2
104800*    END-READ                                                     RL1114.2
104900*    MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
105000*REL-TEST-080-2.                                                  RL1114.2
105100*    MOVE   "REL-TEST-080-2" TO PAR-NAME.                         RL1114.2
105200*    IF      WRK-XN-00001-1 = "X"                                 RL1114.2
105300*            PERFORM PASS                                         RL1114.2
105400*            PERFORM PRINT-DETAIL                                 RL1114.2
105500*    ELSE                                                         RL1114.2
105600*            MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
105700*            MOVE   "X"             TO CORRECT-X                  RL1114.2
105800*            MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
105900*            PERFORM FAIL                                         RL1114.2
106000*            PERFORM PRINT-DETAIL.                                RL1114.2
106100*                                                                 RL1114.2
106200*                                                                 RL1114.2
106300*REL-INIT-090.                                                    RL1114.2
106400*    MOVE   "VIII-30 4.6.2"  TO ANSI-REFERENCE.                   RL1114.2
106500*    MOVE    SPACES TO WRK-XN-00001-1.                            RL1114.2
106600*    MOVE    SPACES TO RL-FS2-STATUS.                             RL1114.2
106700*    MOVE    SPACES TO FILE-RECORD-INFO-P1-120 (2).               RL1114.2
106800*REL-TEST-090-1.                                                  RL1114.2
106900*    MOVE   "REL-TEST-090-1" TO PAR-NAME.                         RL1114.2
107000*    REWRITE RL-FS2R1-F-G-240                                     RL1114.2
107100*            INVALID KEY                                          RL1114.2
107200*            MOVE   "INVALID KEY ON REWRITE" TO RE-MARK           RL1114.2
107300*            PERFORM FAIL                                         RL1114.2
107400*            PERFORM PRINT-DETAIL                                 RL1114.2
107500*            NOT INVALID KEY                                      RL1114.2
107600*            PERFORM PASS                                         RL1114.2
107700*            PERFORM PRINT-DETAIL                                 RL1114.2
107800*    END-REWRITE                                                  RL1114.2
107900*    MOVE   "X" TO WRK-XN-00001-1.                                RL1114.2
108000*REL-TEST-090-2.                                                  RL1114.2
108100*    MOVE   "REL-TEST-090-2" TO PAR-NAME.                         RL1114.2
108200*    IF      WRK-XN-00001-1 = "X"                                 RL1114.2
108300*            PERFORM PASS                                         RL1114.2
108400*            PERFORM PRINT-DETAIL                                 RL1114.2
108500*    ELSE                                                         RL1114.2
108600*            MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL1114.2
108700*            MOVE   "X"             TO CORRECT-X                  RL1114.2
108800*            MOVE    WRK-XN-00001-1 TO COMPUTED-X                 RL1114.2
108900*            PERFORM FAIL                                         RL1114.2
109000*            PERFORM PRINT-DETAIL.                                RL1114.2
109100*                                                                 RL1114.2
109200 CCVS-EXIT SECTION.                                               RL1114.2
109300 CCVS-999999.                                                     RL1114.2
109400     GO TO CLOSE-FILES.                                           RL1114.2
      *END-OF,RL111A                                                                  
