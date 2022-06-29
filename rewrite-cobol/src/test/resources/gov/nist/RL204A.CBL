      *HEADER,COBOL,RL204A                                                            
000100 IDENTIFICATION DIVISION.                                         RL2044.2
000200 PROGRAM-ID.                                                      RL2044.2
000300     RL204A.                                                      RL2044.2
000400****************************************************************  RL2044.2
000500*                                                              *  RL2044.2
000600*    VALIDATION FOR:-                                          *  RL2044.2
000700*                                                              *  RL2044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2044.2
000900*                                                              *  RL2044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2044.2
001100*                                                              *  RL2044.2
001200****************************************************************  RL2044.2
001300*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND    RL2044.2
001400*            SEMANTIC ACTIONS ASSOCIATED WITH THE FOLLOWING       RL2044.2
001500*            ELEMENTS:                                            RL2044.2
001600*                                                                 RL2044.2
001700*            (1)  FILE STATUS                                     RL2044.2
001800*            (2)  USE AFTER EXCEPTION PROCEDURE ON FILE-NAME      RL2044.2
001900*            (3)  READ                                            RL2044.2
002000*            (4)  WRITE                                           RL2044.2
002100*            (5)  REWRITE                                         RL2044.2
002200*            (6)  RELATIVE KEY                                    RL2044.2
002300*            (7)  ACCESS MODE                                     RL2044.2
002400*                                                                 RL2044.2
002500*            THIS PROGRAM CREATES A RELATIVE I-O FILE SEQUENTIALLYRL2044.2
002600*            (ACCESS MODE DYNAMIC)    AND THEN UPDATES SELECTIVE  RL2044.2
002700*            RECORDS OF THE FILE.  THE FILE STATUS CONTENTS ARE   RL2044.2
002800*            CAPTURED AND TESTED FOR ACCURACY FOR EACH OPEN, CLOSERL2044.2
002900*            READ AND REWRITE STATEMENT USED.  THE READ, WRITE ANDRL2044.2
003000*            REWRITE STATEMENTS ARE USED WITHOUT THE APPROPRIATE  RL2044.2
003100*            AT END ON INVALID KEY PHRASES.  THE OMISSION OF THESERL2044.2
003200*            PHRASES ARE PERMITTED IF AN APPLICABLE USE PROCEDURE RL2044.2
003300*            HAS BEEN SPECIFIED.                                  RL2044.2
003400*                                                                 RL2044.2
003500*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL2044.2
003600*            PROGRAM ARE:                                         RL2044.2
003700*                                                                 RL2044.2
003800*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL2044.2
003900*                         RELATIVE  I-O DATA FILE                 RL2044.2
004000*                 X-55   SYSTEM PRINTER                           RL2044.2
004100*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL2044.2
004200*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL2044.2
004300*                 X-75   OBJECT OF VALUE OF CLAUSE                RL2044.2
004400*                 X-82   SOURCE-COMPUTER                          RL2044.2
004500*                 X-83   OBJECT-COMPUTER.                         RL2044.2
004600*                                                                 RL2044.2
004700***************************************************               RL2044.2
004800 ENVIRONMENT DIVISION.                                            RL2044.2
004900 CONFIGURATION SECTION.                                           RL2044.2
005000 SOURCE-COMPUTER.                                                 RL2044.2
005100     XXXXX082.                                                    RL2044.2
005200 OBJECT-COMPUTER.                                                 RL2044.2
005300     XXXXX083.                                                    RL2044.2
005400 INPUT-OUTPUT SECTION.                                            RL2044.2
005500 FILE-CONTROL.                                                    RL2044.2
005600     SELECT PRINT-FILE ASSIGN TO                                  RL2044.2
005700     XXXXX055.                                                    RL2044.2
005800     SELECT   RL-FD2 ASSIGN                                       RL2044.2
005900     XXXXX022                                                     RL2044.2
006000              ORGANIZATION RELATIVE                               RL2044.2
006100              ACCESS  DYNAMIC                                     RL2044.2
006200              RELATIVE  RL-FD2-KEY                                RL2044.2
006300         FILE STATUS IS RL-FD2-STATUS.                            RL2044.2
006400 DATA DIVISION.                                                   RL2044.2
006500 FILE SECTION.                                                    RL2044.2
006600 FD  PRINT-FILE.                                                  RL2044.2
006700 01  PRINT-REC PICTURE X(120).                                    RL2044.2
006800 01  DUMMY-RECORD PICTURE X(120).                                 RL2044.2
006900 FD  RL-FD2                                                       RL2044.2
007000C    VALUE OF                                                     RL2044.2
007100C    XXXXX074                                                     RL2044.2
007200C    IS                                                           RL2044.2
007300C    XXXXX076                                                     RL2044.2
007400*    XXXXX069                                                     RL2044.2
007500     LABEL RECORDS ARE STANDARD                                   RL2044.2
007600     BLOCK CONTAINS 1 RECORDS                                     RL2044.2
007700     DATA RECORD RL-FD2R1-F-G-240.                                RL2044.2
007800 01  RL-FD2R1-F-G-240.                                            RL2044.2
007900     05 RL-FD2-WRK-120 PIC X(120).                                RL2044.2
008000     05 RL-FD2-GRP-120.                                           RL2044.2
008100        10 RL-FD2-WRK-XN-0001-O120F                               RL2044.2
008200                        PICTURE X OCCURS 120 TIMES.               RL2044.2
008300 WORKING-STORAGE SECTION.                                         RL2044.2
008400 01  GRP-0001.                                                    RL2044.2
008500     05 RL-FD2-KEY   PIC 9(8)  VALUE ZERO.                        RL2044.2
008600     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
008700     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
008800     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
008900     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
009000     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
009100     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
009200     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL2044.2
009300     05 RL-FD2-STATUS     PIC  XX  VALUE  SPACE.                  RL2044.2
009400     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL2044.2
009500     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL2044.2
009600     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL2044.2
009700     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL2044.2
009800     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL2044.2
009900     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL2044.2
010000     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL2044.2
010100     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL2044.2
010200     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL2044.2
010300 01  FILE-RECORD-INFORMATION-REC.                                 RL2044.2
010400     03 FILE-RECORD-INFO-SKELETON.                                RL2044.2
010500        05 FILLER                 PICTURE X(48)       VALUE       RL2044.2
010600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2044.2
010700        05 FILLER                 PICTURE X(46)       VALUE       RL2044.2
010800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2044.2
010900        05 FILLER                 PICTURE X(26)       VALUE       RL2044.2
011000             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2044.2
011100        05 FILLER                 PICTURE X(37)       VALUE       RL2044.2
011200             ",RECKEY=                             ".             RL2044.2
011300        05 FILLER                 PICTURE X(38)       VALUE       RL2044.2
011400             ",ALTKEY1=                             ".            RL2044.2
011500        05 FILLER                 PICTURE X(38)       VALUE       RL2044.2
011600             ",ALTKEY2=                             ".            RL2044.2
011700        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2044.2
011800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2044.2
011900        05 FILE-RECORD-INFO-P1-120.                               RL2044.2
012000           07 FILLER              PIC X(5).                       RL2044.2
012100           07 XFILE-NAME           PIC X(6).                      RL2044.2
012200           07 FILLER              PIC X(8).                       RL2044.2
012300           07 XRECORD-NAME         PIC X(6).                      RL2044.2
012400           07 FILLER              PIC X(1).                       RL2044.2
012500           07 REELUNIT-NUMBER     PIC 9(1).                       RL2044.2
012600           07 FILLER              PIC X(7).                       RL2044.2
012700           07 XRECORD-NUMBER       PIC 9(6).                      RL2044.2
012800           07 FILLER              PIC X(6).                       RL2044.2
012900           07 UPDATE-NUMBER       PIC 9(2).                       RL2044.2
013000           07 FILLER              PIC X(5).                       RL2044.2
013100           07 ODO-NUMBER          PIC 9(4).                       RL2044.2
013200           07 FILLER              PIC X(5).                       RL2044.2
013300           07 XPROGRAM-NAME        PIC X(5).                      RL2044.2
013400           07 FILLER              PIC X(7).                       RL2044.2
013500           07 XRECORD-LENGTH       PIC 9(6).                      RL2044.2
013600           07 FILLER              PIC X(7).                       RL2044.2
013700           07 CHARS-OR-RECORDS    PIC X(2).                       RL2044.2
013800           07 FILLER              PIC X(1).                       RL2044.2
013900           07 XBLOCK-SIZE          PIC 9(4).                      RL2044.2
014000           07 FILLER              PIC X(6).                       RL2044.2
014100           07 RECORDS-IN-FILE     PIC 9(6).                       RL2044.2
014200           07 FILLER              PIC X(5).                       RL2044.2
014300           07 XFILE-ORGANIZATION   PIC X(2).                      RL2044.2
014400           07 FILLER              PIC X(6).                       RL2044.2
014500           07 XLABEL-TYPE          PIC X(1).                      RL2044.2
014600        05 FILE-RECORD-INFO-P121-240.                             RL2044.2
014700           07 FILLER              PIC X(8).                       RL2044.2
014800           07 XRECORD-KEY          PIC X(29).                     RL2044.2
014900           07 FILLER              PIC X(9).                       RL2044.2
015000           07 ALTERNATE-KEY1      PIC X(29).                      RL2044.2
015100           07 FILLER              PIC X(9).                       RL2044.2
015200           07 ALTERNATE-KEY2      PIC X(29).                      RL2044.2
015300           07 FILLER              PIC X(7).                       RL2044.2
015400 01  TEST-RESULTS.                                                RL2044.2
015500     02 FILLER                   PIC X      VALUE SPACE.          RL2044.2
015600     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2044.2
015700     02 FILLER                   PIC X      VALUE SPACE.          RL2044.2
015800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2044.2
015900     02 FILLER                   PIC X      VALUE SPACE.          RL2044.2
016000     02  PAR-NAME.                                                RL2044.2
016100       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2044.2
016200       03  PARDOT-X              PIC X      VALUE SPACE.          RL2044.2
016300       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2044.2
016400     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2044.2
016500     02 RE-MARK                  PIC X(61).                       RL2044.2
016600 01  TEST-COMPUTED.                                               RL2044.2
016700     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2044.2
016800     02 FILLER                   PIC X(17)  VALUE                 RL2044.2
016900            "       COMPUTED=".                                   RL2044.2
017000     02 COMPUTED-X.                                               RL2044.2
017100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2044.2
017200     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2044.2
017300                                 PIC -9(9).9(9).                  RL2044.2
017400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2044.2
017500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2044.2
017600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2044.2
017700     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2044.2
017800         04 COMPUTED-18V0                    PIC -9(18).          RL2044.2
017900         04 FILLER                           PIC X.               RL2044.2
018000     03 FILLER PIC X(50) VALUE SPACE.                             RL2044.2
018100 01  TEST-CORRECT.                                                RL2044.2
018200     02 FILLER PIC X(30) VALUE SPACE.                             RL2044.2
018300     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2044.2
018400     02 CORRECT-X.                                                RL2044.2
018500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2044.2
018600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2044.2
018700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2044.2
018800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2044.2
018900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2044.2
019000     03      CR-18V0 REDEFINES CORRECT-A.                         RL2044.2
019100         04 CORRECT-18V0                     PIC -9(18).          RL2044.2
019200         04 FILLER                           PIC X.               RL2044.2
019300     03 FILLER PIC X(2) VALUE SPACE.                              RL2044.2
019400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2044.2
019500 01  CCVS-C-1.                                                    RL2044.2
019600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2044.2
019700-    "SS  PARAGRAPH-NAME                                          RL2044.2
019800-    "       REMARKS".                                            RL2044.2
019900     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2044.2
020000 01  CCVS-C-2.                                                    RL2044.2
020100     02 FILLER                     PIC X        VALUE SPACE.      RL2044.2
020200     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2044.2
020300     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2044.2
020400     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2044.2
020500     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2044.2
020600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2044.2
020700 01  REC-CT                        PIC 99       VALUE ZERO.       RL2044.2
020800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2044.2
020900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2044.2
021000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2044.2
021100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2044.2
021200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2044.2
021300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2044.2
021400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2044.2
021500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2044.2
021600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2044.2
021700 01  CCVS-H-1.                                                    RL2044.2
021800     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2044.2
021900     02  FILLER                    PIC X(42)    VALUE             RL2044.2
022000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2044.2
022100     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2044.2
022200 01  CCVS-H-2A.                                                   RL2044.2
022300   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2044.2
022400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2044.2
022500   02  FILLER                        PIC XXXX   VALUE             RL2044.2
022600     "4.2 ".                                                      RL2044.2
022700   02  FILLER                        PIC X(28)  VALUE             RL2044.2
022800            " COPY - NOT FOR DISTRIBUTION".                       RL2044.2
022900   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2044.2
023000                                                                  RL2044.2
023100 01  CCVS-H-2B.                                                   RL2044.2
023200   02  FILLER                        PIC X(15)  VALUE             RL2044.2
023300            "TEST RESULT OF ".                                    RL2044.2
023400   02  TEST-ID                       PIC X(9).                    RL2044.2
023500   02  FILLER                        PIC X(4)   VALUE             RL2044.2
023600            " IN ".                                               RL2044.2
023700   02  FILLER                        PIC X(12)  VALUE             RL2044.2
023800     " HIGH       ".                                              RL2044.2
023900   02  FILLER                        PIC X(22)  VALUE             RL2044.2
024000            " LEVEL VALIDATION FOR ".                             RL2044.2
024100   02  FILLER                        PIC X(58)  VALUE             RL2044.2
024200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2044.2
024300 01  CCVS-H-3.                                                    RL2044.2
024400     02  FILLER                      PIC X(34)  VALUE             RL2044.2
024500            " FOR OFFICIAL USE ONLY    ".                         RL2044.2
024600     02  FILLER                      PIC X(58)  VALUE             RL2044.2
024700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2044.2
024800     02  FILLER                      PIC X(28)  VALUE             RL2044.2
024900            "  COPYRIGHT   1985 ".                                RL2044.2
025000 01  CCVS-E-1.                                                    RL2044.2
025100     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2044.2
025200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2044.2
025300     02 ID-AGAIN                     PIC X(9).                    RL2044.2
025400     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2044.2
025500 01  CCVS-E-2.                                                    RL2044.2
025600     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2044.2
025700     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2044.2
025800     02 CCVS-E-2-2.                                               RL2044.2
025900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2044.2
026000         03 FILLER                   PIC X      VALUE SPACE.      RL2044.2
026100         03 ENDER-DESC               PIC X(44)  VALUE             RL2044.2
026200            "ERRORS ENCOUNTERED".                                 RL2044.2
026300 01  CCVS-E-3.                                                    RL2044.2
026400     02  FILLER                      PIC X(22)  VALUE             RL2044.2
026500            " FOR OFFICIAL USE ONLY".                             RL2044.2
026600     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2044.2
026700     02  FILLER                      PIC X(58)  VALUE             RL2044.2
026800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2044.2
026900     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2044.2
027000     02 FILLER                       PIC X(15)  VALUE             RL2044.2
027100             " COPYRIGHT 1985".                                   RL2044.2
027200 01  CCVS-E-4.                                                    RL2044.2
027300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2044.2
027400     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2044.2
027500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2044.2
027600     02 FILLER                       PIC X(40)  VALUE             RL2044.2
027700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2044.2
027800 01  XXINFO.                                                      RL2044.2
027900     02 FILLER                       PIC X(19)  VALUE             RL2044.2
028000            "*** INFORMATION ***".                                RL2044.2
028100     02 INFO-TEXT.                                                RL2044.2
028200       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2044.2
028300       04 XXCOMPUTED                 PIC X(20).                   RL2044.2
028400       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2044.2
028500       04 XXCORRECT                  PIC X(20).                   RL2044.2
028600     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2044.2
028700 01  HYPHEN-LINE.                                                 RL2044.2
028800     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2044.2
028900     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2044.2
029000-    "*****************************************".                 RL2044.2
029100     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2044.2
029200-    "******************************".                            RL2044.2
029300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2044.2
029400     "RL204A".                                                    RL2044.2
029500 PROCEDURE DIVISION.                                              RL2044.2
029600 DECLARATIVES.                                                    RL2044.2
029700 RL-FD2-01 SECTION.                                               RL2044.2
029800     USE      AFTER STANDARD EXCEPTION PROCEDURE ON RL-FD2.       RL2044.2
029900 RL-FD2-01-01.                                                    RL2044.2
030000     ADD      1 TO  WRK-CS-09V00-013.                             RL2044.2
030100     GO TO    RL-FD2-01-03                                        RL2044.2
030200              RL-FD2-01-05                                        RL2044.2
030300              DEPENDING ON  WRK-CS-09V00-012.                     RL2044.2
030400     GO TO    RL-FD2-01-EXIT.                                     RL2044.2
030500 RL-FD2-01-03.                                                    RL2044.2
030600*ENTRY FROM SEGMENT REL-TEST-009.                                 RL2044.2
030700*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     RL2044.2
030800     ADD     1   TO WRK-CS-09V00-014.                             RL2044.2
030900 RL-FD2-01-05.                                                    RL2044.2
031000     ADD      1 TO WRK-CS-09V00-017.                              RL2044.2
031100     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL2044.2
031200              MOVE   RL-FD2-STATUS TO WRK-XN-0002-002             RL2044.2
031300              MOVE  "10" TO WRK-XN-0002-003.                      RL2044.2
031400 RL-FD2-01-EXIT.                                                  RL2044.2
031500     EXIT.                                                        RL2044.2
031600 END DECLARATIVES.                                                RL2044.2
031700 CCVS1 SECTION.                                                   RL2044.2
031800 OPEN-FILES.                                                      RL2044.2
031900     OPEN    OUTPUT PRINT-FILE.                                   RL2044.2
032000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2044.2
032100     MOVE    SPACE TO TEST-RESULTS.                               RL2044.2
032200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2044.2
032300     MOVE    ZERO TO REC-SKL-SUB.                                 RL2044.2
032400     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2044.2
032500 CCVS-INIT-FILE.                                                  RL2044.2
032600     ADD     1 TO REC-SKL-SUB.                                    RL2044.2
032700     MOVE    FILE-RECORD-INFO-SKELETON                            RL2044.2
032800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2044.2
032900 CCVS-INIT-EXIT.                                                  RL2044.2
033000     GO TO CCVS1-EXIT.                                            RL2044.2
033100 CLOSE-FILES.                                                     RL2044.2
033200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2044.2
033300 TERMINATE-CCVS.                                                  RL2044.2
033400*    EXIT PROGRAM.                                                RL2044.2
033500*TERMINATE-CALL.                                                  RL2044.2
033600     STOP     RUN.                                                RL2044.2
033700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2044.2
033800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2044.2
033900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2044.2
034000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2044.2
034100     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2044.2
034200 PRINT-DETAIL.                                                    RL2044.2
034300     IF REC-CT NOT EQUAL TO ZERO                                  RL2044.2
034400             MOVE "." TO PARDOT-X                                 RL2044.2
034500             MOVE REC-CT TO DOTVALUE.                             RL2044.2
034600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2044.2
034700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2044.2
034800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2044.2
034900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2044.2
035000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2044.2
035100     MOVE SPACE TO CORRECT-X.                                     RL2044.2
035200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2044.2
035300     MOVE     SPACE TO RE-MARK.                                   RL2044.2
035400 HEAD-ROUTINE.                                                    RL2044.2
035500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2044.2
035600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2044.2
035700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2044.2
035800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2044.2
035900 COLUMN-NAMES-ROUTINE.                                            RL2044.2
036000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2044.2
036100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2044.2
036200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2044.2
036300 END-ROUTINE.                                                     RL2044.2
036400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2044.2
036500 END-RTN-EXIT.                                                    RL2044.2
036600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2044.2
036700 END-ROUTINE-1.                                                   RL2044.2
036800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2044.2
036900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2044.2
037000      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2044.2
037100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2044.2
037200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2044.2
037300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2044.2
037400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2044.2
037500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2044.2
037600  END-ROUTINE-12.                                                 RL2044.2
037700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2044.2
037800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2044.2
037900         MOVE "NO " TO ERROR-TOTAL                                RL2044.2
038000         ELSE                                                     RL2044.2
038100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2044.2
038200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2044.2
038300     PERFORM WRITE-LINE.                                          RL2044.2
038400 END-ROUTINE-13.                                                  RL2044.2
038500     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2044.2
038600         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2044.2
038700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2044.2
038800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2044.2
038900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2044.2
039000      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2044.2
039100          MOVE "NO " TO ERROR-TOTAL                               RL2044.2
039200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2044.2
039300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2044.2
039400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2044.2
039500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2044.2
039600 WRITE-LINE.                                                      RL2044.2
039700     ADD 1 TO RECORD-COUNT.                                       RL2044.2
039800*    IF RECORD-COUNT GREATER 50                                   RL2044.2
039900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2044.2
040000*        MOVE SPACE TO DUMMY-RECORD                               RL2044.2
040100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2044.2
040200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2044.2
040300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2044.2
040400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2044.2
040500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2044.2
040600*        MOVE ZERO TO RECORD-COUNT.                               RL2044.2
040700     PERFORM WRT-LN.                                              RL2044.2
040800 WRT-LN.                                                          RL2044.2
040900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2044.2
041000     MOVE SPACE TO DUMMY-RECORD.                                  RL2044.2
041100 BLANK-LINE-PRINT.                                                RL2044.2
041200     PERFORM WRT-LN.                                              RL2044.2
041300 FAIL-ROUTINE.                                                    RL2044.2
041400     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2044.2
041500            GO TO   FAIL-ROUTINE-WRITE.                           RL2044.2
041600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2044.2
041700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2044.2
041800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2044.2
041900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2044.2
042000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2044.2
042100     GO TO  FAIL-ROUTINE-EX.                                      RL2044.2
042200 FAIL-ROUTINE-WRITE.                                              RL2044.2
042300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2044.2
042400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2044.2
042500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2044.2
042600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2044.2
042700 FAIL-ROUTINE-EX. EXIT.                                           RL2044.2
042800 BAIL-OUT.                                                        RL2044.2
042900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2044.2
043000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2044.2
043100 BAIL-OUT-WRITE.                                                  RL2044.2
043200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2044.2
043300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2044.2
043400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2044.2
043500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2044.2
043600 BAIL-OUT-EX. EXIT.                                               RL2044.2
043700 CCVS1-EXIT.                                                      RL2044.2
043800     EXIT.                                                        RL2044.2
043900 SECT-RL204-001 SECTION.                                          RL2044.2
044000 REL-INIT-009.                                                    RL2044.2
044100     MOVE    "REL-TEST-009" TO PAR-NAME.                          RL2044.2
044200     MOVE     "CREATE RL-FD2" TO  FEATURE                         RL2044.2
044300     MOVE     "RL-FD2"        TO  XFILE-NAME (2).                 RL2044.2
044400     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL2044.2
044500     MOVE CCVS-PGM-ID             TO  XPROGRAM-NAME (2).          RL2044.2
044600     MOVE     000240          TO  XRECORD-LENGTH (2).             RL2044.2
044700     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL2044.2
044800     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL2044.2
044900     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL2044.2
045000     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL2044.2
045100     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL2044.2
045200     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL2044.2
045300*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL2044.2
045400     MOVE     1         TO  WRK-CS-09V00-012.                     RL2044.2
045500     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL2044.2
045600                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL2044.2
045700                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL2044.2
045800     MOVE     SPACE     TO  RL-FD2-STATUS.                        RL2044.2
045900     MOVE     90000002  TO  RL-FD2-KEY.                           RL2044.2
046000     MOVE     01 TO REC-CT.                                       RL2044.2
046100     OPEN     OUTPUT    RL-FD2.                                   RL2044.2
046200     MOVE     RL-FD2-STATUS TO WRK-XN-0002-001.                   RL2044.2
046300*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL2044.2
046400 REL-TEST-009-R.                                                  RL2044.2
046500     MOVE XRECORD-NUMBER (2) TO RL-FD2-KEY.                       RL2044.2
046600     MOVE    "99"  TO RL-FD2-STATUS.                              RL2044.2
046700     MOVE     FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.      RL2044.2
046800     MOVE     FILE-RECORD-INFO-P1-120 (2) TO                      RL2044.2
046900               RL-FD2-GRP-120.                                    RL2044.2
047000     WRITE    RL-FD2R1-F-G-240.                                   RL2044.2
047100     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL2044.2
047200              GO TO  REL-TEST-009-2.                              RL2044.2
047300     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL2044.2
047400             GO TO REL-TEST-009-2.                                RL2044.2
047500     ADD      01 TO  XRECORD-NUMBER (2).                          RL2044.2
047600     GO TO    REL-TEST-009-R.                                     RL2044.2
047700 REL-TEST-009-2.                                                  RL2044.2
047800     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  RL2044.2
047900              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 RL2044.2
048000              MOVE ZERO TO CORRECT-18V0                           RL2044.2
048100              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL2044.2
048200              PERFORM   FAIL                                      RL2044.2
048300              ELSE                                                RL2044.2
048400              PERFORM   PASS.                                     RL2044.2
048500     PERFORM  PRINT-DETAIL.                                       RL2044.2
048600     ADD      01 TO REC-CT.                                       RL2044.2
048700*    .01                                                          RL2044.2
048800     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 RL2044.2
048900              MOVE "INCORRECT COUNT" TO RE-MARK                   RL2044.2
049000              MOVE  500 TO CORRECT-18V0                           RL2044.2
049100             MOVE    XRECORD-NUMBER (2)  TO COMPUTED-18V0         RL2044.2
049200              PERFORM  FAIL                                       RL2044.2
049300              ELSE                                                RL2044.2
049400              PERFORM PASS.                                       RL2044.2
049500     PERFORM  PRINT-DETAIL.                                       RL2044.2
049600     ADD      01 TO REC-CT.                                       RL2044.2
049700*    .02                                                          RL2044.2
049800     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  RL2044.2
049900              MOVE "STATUS/OPEN" TO RE-MARK                       RL2044.2
050000              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL2044.2
050100              MOVE "00" TO CORRECT-A                              RL2044.2
050200              PERFORM  FAIL                                       RL2044.2
050300              ELSE                                                RL2044.2
050400              PERFORM  PASS.                                      RL2044.2
050500     PERFORM  PRINT-DETAIL.                                       RL2044.2
050600     ADD      01 TO REC-CT.                                       RL2044.2
050700*    .03                                                          RL2044.2
050800     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL2044.2
050900              MOVE "STATUS/WRITE" TO RE-MARK                      RL2044.2
051000              MOVE RL-FD2-STATUS TO COMPUTED-A                    RL2044.2
051100              MOVE "00" TO CORRECT-A                              RL2044.2
051200              PERFORM   FAIL                                      RL2044.2
051300              ELSE                                                RL2044.2
051400              PERFORM   PASS.                                     RL2044.2
051500     PERFORM  PRINT-DETAIL.                                       RL2044.2
051600     ADD      01 TO REC-CT.                                       RL2044.2
051700*    .04                                                          RL2044.2
051800     MOVE     SPACE  TO  RL-FD2-STATUS.                           RL2044.2
051900     CLOSE    RL-FD2.                                             RL2044.2
052000     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL2044.2
052100              MOVE "CLOSE/STATUS" TO RE-MARK                      RL2044.2
052200              MOVE   RL-FD2-STATUS TO COMPUTED-A                  RL2044.2
052300              MOVE "00" TO CORRECT-A                              RL2044.2
052400              PERFORM FAIL                                        RL2044.2
052500              ELSE                                                RL2044.2
052600              PERFORM PASS.                                       RL2044.2
052700     PERFORM  PRINT-DETAIL.                                       RL2044.2
052800     ADD      01 TO REC-CT.                                       RL2044.2
052900*    .05                                                          RL2044.2
053000 REL-INIT-010.                                                    RL2044.2
053100     MOVE    "REL-TEST-010" TO PAR-NAME.                          RL2044.2
053200     MOVE     2     TO  WRK-CS-09V00-012.                         RL2044.2
053300     MOVE     ZERO  TO  WRK-CS-09V00-013.                         RL2044.2
053400     MOVE     ZERO  TO  WRK-CS-09V00-014.                         RL2044.2
053500     MOVE     ZERO  TO  WRK-CS-09V00-015.                         RL2044.2
053600     MOVE     ZERO  TO  WRK-CS-09V00-016.                         RL2044.2
053700     MOVE     ZERO  TO  WRK-CS-09V00-017.                         RL2044.2
053800     MOVE     ZERO  TO  WRK-CS-09V00-018.                         RL2044.2
053900     MOVE     01 TO REC-CT.                                       RL2044.2
054000     OPEN     I-O  RL-FD2.                                        RL2044.2
054100     MOVE     SPACE     TO  WRK-XN-0002-002                       RL2044.2
054200     MOVE     SPACE     TO  WRK-XN-0002-003                       RL2044.2
054300     MOVE     SPACE     TO  WRK-XN-0002-004                       RL2044.2
054400     MOVE      RL-FD2-STATUS TO WRK-XN-0002-001                   RL2044.2
054500     MOVE     SPACE TO RL-FD2-STATUS.                             RL2044.2
054600*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       RL2044.2
054700     MOVE    "USE/FILE STATUS"  TO FEATURE.                       RL2044.2
054800 REL-TEST-010-R.                                                  RL2044.2
054900     ADD      1 TO WRK-CS-09V00-014.                              RL2044.2
055000     ADD      1 TO WRK-CS-09V00-015.                              RL2044.2
055100     READ     RL-FD2 NEXT RECORD.                                 RL2044.2
055200     IF        RL-FD2-STATUS EQUAL TO "10"                        RL2044.2
055300              GO TO  REL-TEST-010-3.                              RL2044.2
055400     MOVE     RL-FD2-WRK-120 TO FILE-RECORD-INFO-P1-120 (2).      RL2044.2
055500     IF       WRK-CS-09V00-015 EQUAL TO 5                         RL2044.2
055600              ADD 01 TO UPDATE-NUMBER (2)                         RL2044.2
055700              MOVE FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120  RL2044.2
055800              REWRITE RL-FD2R1-F-G-240                            RL2044.2
055900           MOVE ZERO TO WRK-CS-09V00-015                          RL2044.2
056000              GO TO  REL-TEST-010-2.                              RL2044.2
056100     IF       WRK-CS-09V00-014 GREATER 500                        RL2044.2
056200              GO TO  REL-TEST-010-3.                              RL2044.2
056300     GO TO    REL-TEST-010-R.                                     RL2044.2
056400 REL-TEST-010-2.                                                  RL2044.2
056500     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL2044.2
056600              ADD 1 TO WRK-CS-09V00-016.                          RL2044.2
056700     MOVE    SPACE TO RL-FD2-STATUS.                              RL2044.2
056800     GO TO    REL-TEST-010-R.                                     RL2044.2
056900 REL-TEST-010-3.                                                  RL2044.2
057000     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     RL2044.2
057100              MOVE  "EXCEPTIONS/ERRORS" TO RE-MARK                RL2044.2
057200              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              RL2044.2
057300              MOVE  1 TO CORRECT-18V0                             RL2044.2
057400              PERFORM FAIL                                        RL2044.2
057500              ELSE                                                RL2044.2
057600              PERFORM PASS.                                       RL2044.2
057700     PERFORM  PRINT-DETAIL.                                       RL2044.2
057800     ADD      01 TO REC-CT.                                       RL2044.2
057900*    .01                                                          RL2044.2
058000     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   RL2044.2
058100              MOVE "INCORRECT COUNT" TO RE-MARK                   RL2044.2
058200              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL2044.2
058300              MOVE 501 TO CORRECT-18V0                            RL2044.2
058400              PERFORM FAIL                                        RL2044.2
058500              ELSE                                                RL2044.2
058600             PERFORM PASS.                                        RL2044.2
058700     PERFORM  PRINT-DETAIL.                                       RL2044.2
058800     ADD      01 TO REC-CT.                                       RL2044.2
058900*    .02                                                          RL2044.2
059000     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   RL2044.2
059100              MOVE "OPEN/STATUS"  TO  RE-MARK                     RL2044.2
059200              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL2044.2
059300              MOVE  "00" TO CORRECT-A                             RL2044.2
059400              PERFORM FAIL                                        RL2044.2
059500              ELSE                                                RL2044.2
059600              PERFORM PASS.                                       RL2044.2
059700     PERFORM  PRINT-DETAIL.                                       RL2044.2
059800     ADD      01 TO REC-CT.                                       RL2044.2
059900*    .03                                                          RL2044.2
060000     IF        RL-FD2-STATUS NOT EQUAL  TO "10"                   RL2044.2
060100              MOVE "ATEND/STATUS" TO RE-MARK                      RL2044.2
060200              MOVE  RL-FD2-STATUS TO COMPUTED-A                   RL2044.2
060300              MOVE "10" TO CORRECT-A                              RL2044.2
060400              PERFORM FAIL                                        RL2044.2
060500              ELSE                                                RL2044.2
060600              PERFORM PASS.                                       RL2044.2
060700     PERFORM  PRINT-DETAIL.                                       RL2044.2
060800     ADD      01 TO REC-CT.                                       RL2044.2
060900*    .04                                                          RL2044.2
061000     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   RL2044.2
061100              MOVE  "EXCEPTIN/STATUS" TO RE-MARK                  RL2044.2
061200              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 RL2044.2
061300              MOVE  "10" TO CORRECT-A                             RL2044.2
061400              PERFORM FAIL                                        RL2044.2
061500              ELSE                                                RL2044.2
061600              PERFORM PASS.                                       RL2044.2
061700     PERFORM  PRINT-DETAIL.                                       RL2044.2
061800     ADD      01 TO REC-CT.                                       RL2044.2
061900*    .05                                                          RL2044.2
062000     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   RL2044.2
062100              MOVE "NO/EXCEPTION" TO  RE-MARK                     RL2044.2
062200              MOVE WRK-XN-0002-003 TO COMPUTED-A                  RL2044.2
062300              MOVE "10" TO CORRECT-A                              RL2044.2
062400              PERFORM FAIL                                        RL2044.2
062500              ELSE                                                RL2044.2
062600              PERFORM PASS.                                       RL2044.2
062700     PERFORM  PRINT-DETAIL                                        RL2044.2
062800     ADD      01 TO REC-CT.                                       RL2044.2
062900*    .06                                                          RL2044.2
063000     MOVE     SPACE TO RL-FD2-STATUS.                             RL2044.2
063100     CLOSE    RL-FD2                                              RL2044.2
063200     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL2044.2
063300              MOVE "CLOSE/STATUS" TO RE-MARK                      RL2044.2
063400              MOVE  RL-FD2-STATUS TO  COMPUTED-A                  RL2044.2
063500              MOVE "00" TO CORRECT-A                              RL2044.2
063600              PERFORM FAIL                                        RL2044.2
063700              ELSE                                                RL2044.2
063800              PERFORM PASS.                                       RL2044.2
063900     PERFORM  PRINT-DETAIL.                                       RL2044.2
064000     ADD      01 TO REC-CT.                                       RL2044.2
064100*    .07                                                          RL2044.2
064200 CCVS-EXIT SECTION.                                               RL2044.2
064300 CCVS-999999.                                                     RL2044.2
064400     GO TO CLOSE-FILES.                                           RL2044.2
      *END-OF,RL204A                                                                  
