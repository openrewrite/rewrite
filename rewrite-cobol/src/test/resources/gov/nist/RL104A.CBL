      *HEADER,COBOL,RL104A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1044.2
000200 PROGRAM-ID.                                                      RL1044.2
000300     RL104A.                                                      RL1044.2
000400****************************************************************  RL1044.2
000500*                                                              *  RL1044.2
000600*    VALIDATION FOR:-                                          *  RL1044.2
000700*                                                              *  RL1044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1044.2
000900*                                                              *  RL1044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1044.2
001100*                                                              *  RL1044.2
001200****************************************************************  RL1044.2
001300*                                                              *  RL1044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1044.2
001500*                                                              *  RL1044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  RL1044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  RL1044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  RL1044.2
001900*                                                              *  RL1044.2
002000****************************************************************  RL1044.2
002100*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND    RL1044.2
002200*            SEMANTIC ACTIONS ASSOCIATED WITH THE FOLLOWING       RL1044.2
002300*            ELEMENTS:                                            RL1044.2
002400*                                                                 RL1044.2
002500*            (1)  FILE STATUS                                     RL1044.2
002600*            (2)  USE AFTER EXCEPTION PROCEDURE ON FILE-NAME      RL1044.2
002700*            (3)  READ                                            RL1044.2
002800*            (4)  WRITE                                           RL1044.2
002900*            (5)  REWRITE                                         RL1044.2
003000*            (6)  RELATIVE KEY                                    RL1044.2
003100*            (7)  ACCESS MODE                                     RL1044.2
003200*                                                                 RL1044.2
003300*            THIS PROGRAM CREATES A RELATIVE I-O FILE SEQUENTIALLYRL1044.2
003400*            (ACCESS MODE SEQUENTIAL) AND THEN UPDATES SELECTIVE  RL1044.2
003500*            RECORDS OF THE FILE.  THE FILE STATUS CONTENTS ARE   RL1044.2
003600*            CAPTURED AND TESTED FOR ACCURACY FOR EACH OPEN, CLOSERL1044.2
003700*            READ AND REWRITE STATEMENT USED.  THE READ, WRITE ANDRL1044.2
003800*            REWRITE STATEMENTS ARE USED WITHOUT THE APPROPRIATE  RL1044.2
003900*            AT END ON INVALID KEY PHRASES.  THE OMISSION OF THESERL1044.2
004000*            PHRASES ARE PERMITTED IF AN APPLICABLE USE PROCEDURE RL1044.2
004100*            HAS BEEN SPECIFIED.                                  RL1044.2
004200*                                                                 RL1044.2
004300****************************************************************  RL1044.2
004400 ENVIRONMENT DIVISION.                                            RL1044.2
004500 CONFIGURATION SECTION.                                           RL1044.2
004600 SOURCE-COMPUTER.                                                 RL1044.2
004700     XXXXX082.                                                    RL1044.2
004800 OBJECT-COMPUTER.                                                 RL1044.2
004900     XXXXX083.                                                    RL1044.2
005000 INPUT-OUTPUT SECTION.                                            RL1044.2
005100 FILE-CONTROL.                                                    RL1044.2
005200     SELECT PRINT-FILE ASSIGN TO                                  RL1044.2
005300     XXXXX055.                                                    RL1044.2
005400     SELECT   RL-FS2 ASSIGN                                       RL1044.2
005500     XXXXX022                                                     RL1044.2
005600              ORGANIZATION RELATIVE                               RL1044.2
005700              ACCESS SEQUENTIAL                                   RL1044.2
005800              RELATIVE  RL-FS2-KEY                                RL1044.2
005900         FILE STATUS IS RL-FS2-STATUS.                            RL1044.2
006000 DATA DIVISION.                                                   RL1044.2
006100 FILE SECTION.                                                    RL1044.2
006200 FD  PRINT-FILE.                                                  RL1044.2
006300 01  PRINT-REC PICTURE X(120).                                    RL1044.2
006400 01  DUMMY-RECORD PICTURE X(120).                                 RL1044.2
006500 FD  RL-FS2                                                       RL1044.2
006600C    VALUE OF                                                     RL1044.2
006700C    XXXXX074                                                     RL1044.2
006800C    IS                                                           RL1044.2
006900C    XXXXX076                                                     RL1044.2
007000*    XXXXX069                                                     RL1044.2
007100     LABEL RECORDS ARE STANDARD                                   RL1044.2
007200     BLOCK CONTAINS 1 RECORDS                                     RL1044.2
007300     DATA RECORD RL-FS2R1-F-G-240.                                RL1044.2
007400 01  RL-FS2R1-F-G-240.                                            RL1044.2
007500     05 RL-FS2-WRK-120 PIC X(120).                                RL1044.2
007600     05 RL-FS2-GRP-120.                                           RL1044.2
007700        10 RL-FS2-WRK-XN-0001-O120F                               RL1044.2
007800                        PICTURE X OCCURS 120 TIMES.               RL1044.2
007900 WORKING-STORAGE SECTION.                                         RL1044.2
008000 01  GRP-0001.                                                    RL1044.2
008100     05 RL-FS2-KEY   PIC 9(8)  VALUE ZERO.                        RL1044.2
008200     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008300     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008400     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008500     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008600     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008700     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008800     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1044.2
008900     05 RL-FS2-STATUS     PIC  XX  VALUE  SPACE.                  RL1044.2
009000     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1044.2
009100     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1044.2
009200     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1044.2
009300     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1044.2
009400     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1044.2
009500     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1044.2
009600     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1044.2
009700     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1044.2
009800     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1044.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 RL1044.2
010000     03 FILE-RECORD-INFO-SKELETON.                                RL1044.2
010100        05 FILLER                 PICTURE X(48)       VALUE       RL1044.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1044.2
010300        05 FILLER                 PICTURE X(46)       VALUE       RL1044.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1044.2
010500        05 FILLER                 PICTURE X(26)       VALUE       RL1044.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1044.2
010700        05 FILLER                 PICTURE X(37)       VALUE       RL1044.2
010800             ",RECKEY=                             ".             RL1044.2
010900        05 FILLER                 PICTURE X(38)       VALUE       RL1044.2
011000             ",ALTKEY1=                             ".            RL1044.2
011100        05 FILLER                 PICTURE X(38)       VALUE       RL1044.2
011200             ",ALTKEY2=                             ".            RL1044.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1044.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1044.2
011500        05 FILE-RECORD-INFO-P1-120.                               RL1044.2
011600           07 FILLER              PIC X(5).                       RL1044.2
011700           07 XFILE-NAME           PIC X(6).                      RL1044.2
011800           07 FILLER              PIC X(8).                       RL1044.2
011900           07 XRECORD-NAME         PIC X(6).                      RL1044.2
012000           07 FILLER              PIC X(1).                       RL1044.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       RL1044.2
012200           07 FILLER              PIC X(7).                       RL1044.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      RL1044.2
012400           07 FILLER              PIC X(6).                       RL1044.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       RL1044.2
012600           07 FILLER              PIC X(5).                       RL1044.2
012700           07 ODO-NUMBER          PIC 9(4).                       RL1044.2
012800           07 FILLER              PIC X(5).                       RL1044.2
012900           07 XPROGRAM-NAME        PIC X(5).                      RL1044.2
013000           07 FILLER              PIC X(7).                       RL1044.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      RL1044.2
013200           07 FILLER              PIC X(7).                       RL1044.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       RL1044.2
013400           07 FILLER              PIC X(1).                       RL1044.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      RL1044.2
013600           07 FILLER              PIC X(6).                       RL1044.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       RL1044.2
013800           07 FILLER              PIC X(5).                       RL1044.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      RL1044.2
014000           07 FILLER              PIC X(6).                       RL1044.2
014100           07 XLABEL-TYPE          PIC X(1).                      RL1044.2
014200        05 FILE-RECORD-INFO-P121-240.                             RL1044.2
014300           07 FILLER              PIC X(8).                       RL1044.2
014400           07 XRECORD-KEY          PIC X(29).                     RL1044.2
014500           07 FILLER              PIC X(9).                       RL1044.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      RL1044.2
014700           07 FILLER              PIC X(9).                       RL1044.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      RL1044.2
014900           07 FILLER              PIC X(7).                       RL1044.2
015000 01  TEST-RESULTS.                                                RL1044.2
015100     02 FILLER                   PIC X      VALUE SPACE.          RL1044.2
015200     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1044.2
015300     02 FILLER                   PIC X      VALUE SPACE.          RL1044.2
015400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1044.2
015500     02 FILLER                   PIC X      VALUE SPACE.          RL1044.2
015600     02  PAR-NAME.                                                RL1044.2
015700       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1044.2
015800       03  PARDOT-X              PIC X      VALUE SPACE.          RL1044.2
015900       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1044.2
016000     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1044.2
016100     02 RE-MARK                  PIC X(61).                       RL1044.2
016200 01  TEST-COMPUTED.                                               RL1044.2
016300     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1044.2
016400     02 FILLER                   PIC X(17)  VALUE                 RL1044.2
016500            "       COMPUTED=".                                   RL1044.2
016600     02 COMPUTED-X.                                               RL1044.2
016700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1044.2
016800     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1044.2
016900                                 PIC -9(9).9(9).                  RL1044.2
017000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1044.2
017100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1044.2
017200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1044.2
017300     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1044.2
017400         04 COMPUTED-18V0                    PIC -9(18).          RL1044.2
017500         04 FILLER                           PIC X.               RL1044.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             RL1044.2
017700 01  TEST-CORRECT.                                                RL1044.2
017800     02 FILLER PIC X(30) VALUE SPACE.                             RL1044.2
017900     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1044.2
018000     02 CORRECT-X.                                                RL1044.2
018100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1044.2
018200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1044.2
018300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1044.2
018400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1044.2
018500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1044.2
018600     03      CR-18V0 REDEFINES CORRECT-A.                         RL1044.2
018700         04 CORRECT-18V0                     PIC -9(18).          RL1044.2
018800         04 FILLER                           PIC X.               RL1044.2
018900     03 FILLER PIC X(2) VALUE SPACE.                              RL1044.2
019000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1044.2
019100 01  CCVS-C-1.                                                    RL1044.2
019200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1044.2
019300-    "SS  PARAGRAPH-NAME                                          RL1044.2
019400-    "       REMARKS".                                            RL1044.2
019500     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1044.2
019600 01  CCVS-C-2.                                                    RL1044.2
019700     02 FILLER                     PIC X        VALUE SPACE.      RL1044.2
019800     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1044.2
019900     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1044.2
020000     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1044.2
020100     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1044.2
020200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1044.2
020300 01  REC-CT                        PIC 99       VALUE ZERO.       RL1044.2
020400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1044.2
020500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1044.2
020600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1044.2
020700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1044.2
020800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1044.2
020900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1044.2
021000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1044.2
021100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1044.2
021200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1044.2
021300 01  CCVS-H-1.                                                    RL1044.2
021400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1044.2
021500     02  FILLER                    PIC X(42)    VALUE             RL1044.2
021600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1044.2
021700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1044.2
021800 01  CCVS-H-2A.                                                   RL1044.2
021900   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1044.2
022000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1044.2
022100   02  FILLER                        PIC XXXX   VALUE             RL1044.2
022200     "4.2 ".                                                      RL1044.2
022300   02  FILLER                        PIC X(28)  VALUE             RL1044.2
022400            " COPY - NOT FOR DISTRIBUTION".                       RL1044.2
022500   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1044.2
022600                                                                  RL1044.2
022700 01  CCVS-H-2B.                                                   RL1044.2
022800   02  FILLER                        PIC X(15)  VALUE             RL1044.2
022900            "TEST RESULT OF ".                                    RL1044.2
023000   02  TEST-ID                       PIC X(9).                    RL1044.2
023100   02  FILLER                        PIC X(4)   VALUE             RL1044.2
023200            " IN ".                                               RL1044.2
023300   02  FILLER                        PIC X(12)  VALUE             RL1044.2
023400     " HIGH       ".                                              RL1044.2
023500   02  FILLER                        PIC X(22)  VALUE             RL1044.2
023600            " LEVEL VALIDATION FOR ".                             RL1044.2
023700   02  FILLER                        PIC X(58)  VALUE             RL1044.2
023800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1044.2
023900 01  CCVS-H-3.                                                    RL1044.2
024000     02  FILLER                      PIC X(34)  VALUE             RL1044.2
024100            " FOR OFFICIAL USE ONLY    ".                         RL1044.2
024200     02  FILLER                      PIC X(58)  VALUE             RL1044.2
024300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1044.2
024400     02  FILLER                      PIC X(28)  VALUE             RL1044.2
024500            "  COPYRIGHT   1985 ".                                RL1044.2
024600 01  CCVS-E-1.                                                    RL1044.2
024700     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1044.2
024800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1044.2
024900     02 ID-AGAIN                     PIC X(9).                    RL1044.2
025000     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1044.2
025100 01  CCVS-E-2.                                                    RL1044.2
025200     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1044.2
025300     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1044.2
025400     02 CCVS-E-2-2.                                               RL1044.2
025500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1044.2
025600         03 FILLER                   PIC X      VALUE SPACE.      RL1044.2
025700         03 ENDER-DESC               PIC X(44)  VALUE             RL1044.2
025800            "ERRORS ENCOUNTERED".                                 RL1044.2
025900 01  CCVS-E-3.                                                    RL1044.2
026000     02  FILLER                      PIC X(22)  VALUE             RL1044.2
026100            " FOR OFFICIAL USE ONLY".                             RL1044.2
026200     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1044.2
026300     02  FILLER                      PIC X(58)  VALUE             RL1044.2
026400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1044.2
026500     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1044.2
026600     02 FILLER                       PIC X(15)  VALUE             RL1044.2
026700             " COPYRIGHT 1985".                                   RL1044.2
026800 01  CCVS-E-4.                                                    RL1044.2
026900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1044.2
027000     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1044.2
027100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1044.2
027200     02 FILLER                       PIC X(40)  VALUE             RL1044.2
027300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1044.2
027400 01  XXINFO.                                                      RL1044.2
027500     02 FILLER                       PIC X(19)  VALUE             RL1044.2
027600            "*** INFORMATION ***".                                RL1044.2
027700     02 INFO-TEXT.                                                RL1044.2
027800       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1044.2
027900       04 XXCOMPUTED                 PIC X(20).                   RL1044.2
028000       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1044.2
028100       04 XXCORRECT                  PIC X(20).                   RL1044.2
028200     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1044.2
028300 01  HYPHEN-LINE.                                                 RL1044.2
028400     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1044.2
028500     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1044.2
028600-    "*****************************************".                 RL1044.2
028700     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1044.2
028800-    "******************************".                            RL1044.2
028900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1044.2
029000     "RL104A".                                                    RL1044.2
029100 PROCEDURE DIVISION.                                              RL1044.2
029200 DECLARATIVES.                                                    RL1044.2
029300 RL-FS2-01 SECTION.                                               RL1044.2
029400     USE      AFTER STANDARD EXCEPTION PROCEDURE ON RL-FS2.       RL1044.2
029500 RL-FS2-01-01.                                                    RL1044.2
029600     ADD      1 TO  WRK-CS-09V00-013.                             RL1044.2
029700     GO TO    RL-FS2-01-03                                        RL1044.2
029800              RL-FS2-01-05                                        RL1044.2
029900              DEPENDING ON  WRK-CS-09V00-012.                     RL1044.2
030000     GO TO    RL-FS2-01-EXIT.                                     RL1044.2
030100 RL-FS2-01-03.                                                    RL1044.2
030200*ENTRY FROM SEGMENT REL-TEST-009.                                 RL1044.2
030300*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     RL1044.2
030400     ADD     1   TO WRK-CS-09V00-014.                             RL1044.2
030500 RL-FS2-01-05.                                                    RL1044.2
030600     ADD      1 TO WRK-CS-09V00-017.                              RL1044.2
030700     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL1044.2
030800              MOVE   RL-FS2-STATUS TO WRK-XN-0002-002             RL1044.2
030900              MOVE  "10" TO WRK-XN-0002-003.                      RL1044.2
031000 RL-FS2-01-EXIT.                                                  RL1044.2
031100     EXIT.                                                        RL1044.2
031200 END DECLARATIVES.                                                RL1044.2
031300 CCVS1 SECTION.                                                   RL1044.2
031400 OPEN-FILES.                                                      RL1044.2
031500     OPEN    OUTPUT PRINT-FILE.                                   RL1044.2
031600     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1044.2
031700     MOVE    SPACE TO TEST-RESULTS.                               RL1044.2
031800     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1044.2
031900     MOVE    ZERO TO REC-SKL-SUB.                                 RL1044.2
032000     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1044.2
032100 CCVS-INIT-FILE.                                                  RL1044.2
032200     ADD     1 TO REC-SKL-SUB.                                    RL1044.2
032300     MOVE    FILE-RECORD-INFO-SKELETON                            RL1044.2
032400          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1044.2
032500 CCVS-INIT-EXIT.                                                  RL1044.2
032600     GO TO CCVS1-EXIT.                                            RL1044.2
032700 CLOSE-FILES.                                                     RL1044.2
032800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1044.2
032900 TERMINATE-CCVS.                                                  RL1044.2
033000*    EXIT PROGRAM.                                                RL1044.2
033100*TERMINATE-CALL.                                                  RL1044.2
033200     STOP     RUN.                                                RL1044.2
033300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1044.2
033400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1044.2
033500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1044.2
033600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1044.2
033700     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1044.2
033800 PRINT-DETAIL.                                                    RL1044.2
033900     IF REC-CT NOT EQUAL TO ZERO                                  RL1044.2
034000             MOVE "." TO PARDOT-X                                 RL1044.2
034100             MOVE REC-CT TO DOTVALUE.                             RL1044.2
034200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1044.2
034300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1044.2
034400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1044.2
034500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1044.2
034600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1044.2
034700     MOVE SPACE TO CORRECT-X.                                     RL1044.2
034800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1044.2
034900     MOVE     SPACE TO RE-MARK.                                   RL1044.2
035000 HEAD-ROUTINE.                                                    RL1044.2
035100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1044.2
035200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1044.2
035300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1044.2
035400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1044.2
035500 COLUMN-NAMES-ROUTINE.                                            RL1044.2
035600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1044.2
035700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1044.2
035800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1044.2
035900 END-ROUTINE.                                                     RL1044.2
036000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1044.2
036100 END-RTN-EXIT.                                                    RL1044.2
036200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1044.2
036300 END-ROUTINE-1.                                                   RL1044.2
036400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1044.2
036500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1044.2
036600      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1044.2
036700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1044.2
036800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1044.2
036900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1044.2
037000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1044.2
037100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1044.2
037200  END-ROUTINE-12.                                                 RL1044.2
037300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1044.2
037400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1044.2
037500         MOVE "NO " TO ERROR-TOTAL                                RL1044.2
037600         ELSE                                                     RL1044.2
037700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1044.2
037800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1044.2
037900     PERFORM WRITE-LINE.                                          RL1044.2
038000 END-ROUTINE-13.                                                  RL1044.2
038100     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1044.2
038200         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1044.2
038300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1044.2
038400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1044.2
038500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1044.2
038600      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1044.2
038700          MOVE "NO " TO ERROR-TOTAL                               RL1044.2
038800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1044.2
038900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1044.2
039000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1044.2
039100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1044.2
039200 WRITE-LINE.                                                      RL1044.2
039300     ADD 1 TO RECORD-COUNT.                                       RL1044.2
039400*    IF RECORD-COUNT GREATER 50                                   RL1044.2
039500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1044.2
039600*        MOVE SPACE TO DUMMY-RECORD                               RL1044.2
039700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1044.2
039800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1044.2
039900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1044.2
040000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1044.2
040100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1044.2
040200*        MOVE ZERO TO RECORD-COUNT.                               RL1044.2
040300     PERFORM WRT-LN.                                              RL1044.2
040400 WRT-LN.                                                          RL1044.2
040500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1044.2
040600     MOVE SPACE TO DUMMY-RECORD.                                  RL1044.2
040700 BLANK-LINE-PRINT.                                                RL1044.2
040800     PERFORM WRT-LN.                                              RL1044.2
040900 FAIL-ROUTINE.                                                    RL1044.2
041000     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1044.2
041100            GO TO   FAIL-ROUTINE-WRITE.                           RL1044.2
041200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1044.2
041300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1044.2
041400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1044.2
041500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1044.2
041600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1044.2
041700     GO TO  FAIL-ROUTINE-EX.                                      RL1044.2
041800 FAIL-ROUTINE-WRITE.                                              RL1044.2
041900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1044.2
042000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1044.2
042100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1044.2
042200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1044.2
042300 FAIL-ROUTINE-EX. EXIT.                                           RL1044.2
042400 BAIL-OUT.                                                        RL1044.2
042500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1044.2
042600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1044.2
042700 BAIL-OUT-WRITE.                                                  RL1044.2
042800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1044.2
042900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1044.2
043000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1044.2
043100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1044.2
043200 BAIL-OUT-EX. EXIT.                                               RL1044.2
043300 CCVS1-EXIT.                                                      RL1044.2
043400     EXIT.                                                        RL1044.2
043500 SECT-RL-04-001 SECTION.                                          RL1044.2
043600 REL-INIT-009.                                                    RL1044.2
043700     MOVE    "REL-TEST-009" TO PAR-NAME.                          RL1044.2
043800     MOVE     "CREATE RL-FS2" TO  FEATURE                         RL1044.2
043900     MOVE     "RL-FS2"        TO  XFILE-NAME (2).                 RL1044.2
044000     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL1044.2
044100     MOVE CCVS-PGM-ID         TO  XPROGRAM-NAME (2).              RL1044.2
044200     MOVE     000240          TO  XRECORD-LENGTH (2).             RL1044.2
044300     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL1044.2
044400     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL1044.2
044500     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL1044.2
044600     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL1044.2
044700     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL1044.2
044800     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL1044.2
044900*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1044.2
045000     MOVE     1         TO  WRK-CS-09V00-012.                     RL1044.2
045100     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1044.2
045200                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1044.2
045300                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1044.2
045400     MOVE     SPACE     TO  RL-FS2-STATUS.                        RL1044.2
045500     MOVE     90000002  TO  RL-FS2-KEY.                           RL1044.2
045600     MOVE     01 TO REC-CT.                                       RL1044.2
045700     OPEN     OUTPUT    RL-FS2.                                   RL1044.2
045800     MOVE     RL-FS2-STATUS TO WRK-XN-0002-001.                   RL1044.2
045900*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL1044.2
046000 REL-TEST-009-R.                                                  RL1044.2
046100     MOVE    "99"  TO RL-FS2-STATUS.                              RL1044.2
046200     MOVE     FILE-RECORD-INFO-P1-120 (2) TO RL-FS2-WRK-120.      RL1044.2
046300     MOVE     FILE-RECORD-INFO-P1-120 (2) TO                      RL1044.2
046400               RL-FS2-GRP-120.                                    RL1044.2
046500     WRITE    RL-FS2R1-F-G-240.                                   RL1044.2
046600     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1044.2
046700              GO TO  REL-TEST-009-2.                              RL1044.2
046800     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL1044.2
046900             GO TO REL-TEST-009-2.                                RL1044.2
047000     ADD      01 TO  XRECORD-NUMBER (2).                          RL1044.2
047100     GO TO    REL-TEST-009-R.                                     RL1044.2
047200 REL-TEST-009-2.                                                  RL1044.2
047300     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  RL1044.2
047400              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 RL1044.2
047500              MOVE ZERO TO CORRECT-18V0                           RL1044.2
047600              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1044.2
047700              PERFORM   FAIL                                      RL1044.2
047800              ELSE                                                RL1044.2
047900              PERFORM   PASS.                                     RL1044.2
048000     PERFORM  PRINT-DETAIL.                                       RL1044.2
048100     ADD      01 TO REC-CT.                                       RL1044.2
048200*    .01                                                          RL1044.2
048300     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 RL1044.2
048400              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1044.2
048500              MOVE  500 TO CORRECT-18V0                           RL1044.2
048600             MOVE    XRECORD-NUMBER (2)  TO COMPUTED-18V0         RL1044.2
048700              PERFORM  FAIL                                       RL1044.2
048800              ELSE                                                RL1044.2
048900              PERFORM PASS.                                       RL1044.2
049000     PERFORM  PRINT-DETAIL.                                       RL1044.2
049100     ADD      01 TO REC-CT.                                       RL1044.2
049200*    .02                                                          RL1044.2
049300     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  RL1044.2
049400              MOVE "STATUS/OPEN" TO RE-MARK                       RL1044.2
049500              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1044.2
049600              MOVE "00" TO CORRECT-A                              RL1044.2
049700              PERFORM  FAIL                                       RL1044.2
049800              ELSE                                                RL1044.2
049900              PERFORM  PASS.                                      RL1044.2
050000     PERFORM  PRINT-DETAIL.                                       RL1044.2
050100     ADD      01 TO REC-CT.                                       RL1044.2
050200*    .03                                                          RL1044.2
050300     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1044.2
050400              MOVE "STATUS/WRITE" TO RE-MARK                      RL1044.2
050500              MOVE RL-FS2-STATUS TO COMPUTED-A                    RL1044.2
050600              MOVE "00" TO CORRECT-A                              RL1044.2
050700              PERFORM   FAIL                                      RL1044.2
050800              ELSE                                                RL1044.2
050900              PERFORM   PASS.                                     RL1044.2
051000     PERFORM  PRINT-DETAIL.                                       RL1044.2
051100     ADD      01 TO REC-CT.                                       RL1044.2
051200*    .04                                                          RL1044.2
051300     MOVE     SPACE  TO  RL-FS2-STATUS.                           RL1044.2
051400     CLOSE    RL-FS2.                                             RL1044.2
051500     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1044.2
051600              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1044.2
051700              MOVE   RL-FS2-STATUS TO COMPUTED-A                  RL1044.2
051800              MOVE "00" TO CORRECT-A                              RL1044.2
051900              PERFORM FAIL                                        RL1044.2
052000              ELSE                                                RL1044.2
052100              PERFORM PASS.                                       RL1044.2
052200     PERFORM  PRINT-DETAIL.                                       RL1044.2
052300     ADD      01 TO REC-CT.                                       RL1044.2
052400*    .05                                                          RL1044.2
052500 REL-INIT-010.                                                    RL1044.2
052600     MOVE    "REL-TEST-010" TO PAR-NAME.                          RL1044.2
052700     MOVE     2     TO  WRK-CS-09V00-012.                         RL1044.2
052800     MOVE     ZERO  TO  WRK-CS-09V00-013.                         RL1044.2
052900     MOVE     ZERO  TO  WRK-CS-09V00-014.                         RL1044.2
053000     MOVE     ZERO  TO  WRK-CS-09V00-015.                         RL1044.2
053100     MOVE     ZERO  TO  WRK-CS-09V00-016.                         RL1044.2
053200     MOVE     ZERO  TO  WRK-CS-09V00-017.                         RL1044.2
053300     MOVE     ZERO  TO  WRK-CS-09V00-018.                         RL1044.2
053400     MOVE     01 TO REC-CT.                                       RL1044.2
053500     OPEN     I-O  RL-FS2.                                        RL1044.2
053600     MOVE     SPACE     TO  WRK-XN-0002-002                       RL1044.2
053700     MOVE     SPACE     TO  WRK-XN-0002-003                       RL1044.2
053800     MOVE     SPACE     TO  WRK-XN-0002-004                       RL1044.2
053900     MOVE      RL-FS2-STATUS TO WRK-XN-0002-001                   RL1044.2
054000     MOVE     SPACE TO RL-FS2-STATUS.                             RL1044.2
054100*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       RL1044.2
054200     MOVE    "USE/FILE STATUE"  TO FEATURE.                       RL1044.2
054300 REL-TEST-010-R.                                                  RL1044.2
054400     ADD      1 TO WRK-CS-09V00-014.                              RL1044.2
054500     ADD      1 TO WRK-CS-09V00-015.                              RL1044.2
054600     READ     RL-FS2.                                             RL1044.2
054700     IF        RL-FS2-STATUS EQUAL TO "10"                        RL1044.2
054800              GO TO  REL-TEST-010-3.                              RL1044.2
054900     MOVE     RL-FS2-WRK-120 TO FILE-RECORD-INFO-P1-120 (2).      RL1044.2
055000     IF       WRK-CS-09V00-015 EQUAL TO 5                         RL1044.2
055100              ADD 01 TO UPDATE-NUMBER (2)                         RL1044.2
055200              MOVE FILE-RECORD-INFO-P1-120 (2) TO RL-FS2-WRK-120  RL1044.2
055300              REWRITE RL-FS2R1-F-G-240                            RL1044.2
055400           MOVE ZERO TO WRK-CS-09V00-015                          RL1044.2
055500              GO TO  REL-TEST-010-2.                              RL1044.2
055600     IF       WRK-CS-09V00-014 GREATER 500                        RL1044.2
055700              GO TO  REL-TEST-010-3.                              RL1044.2
055800     GO TO    REL-TEST-010-R.                                     RL1044.2
055900 REL-TEST-010-2.                                                  RL1044.2
056000     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1044.2
056100              ADD 1 TO WRK-CS-09V00-016.                          RL1044.2
056200     MOVE    SPACE TO RL-FS2-STATUS.                              RL1044.2
056300     GO TO    REL-TEST-010-R.                                     RL1044.2
056400 REL-TEST-010-3.                                                  RL1044.2
056500     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     RL1044.2
056600              MOVE  "EXCEPTIONS/ERRORS" TO RE-MARK                RL1044.2
056700              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              RL1044.2
056800              MOVE  1 TO CORRECT-18V0                             RL1044.2
056900              PERFORM FAIL                                        RL1044.2
057000              ELSE                                                RL1044.2
057100              PERFORM PASS.                                       RL1044.2
057200     PERFORM  PRINT-DETAIL.                                       RL1044.2
057300     ADD      01 TO REC-CT.                                       RL1044.2
057400*    .01                                                          RL1044.2
057500     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   RL1044.2
057600              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1044.2
057700              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1044.2
057800              MOVE 501 TO CORRECT-18V0                            RL1044.2
057900              PERFORM FAIL                                        RL1044.2
058000              ELSE                                                RL1044.2
058100             PERFORM PASS.                                        RL1044.2
058200     PERFORM  PRINT-DETAIL.                                       RL1044.2
058300     ADD      01 TO REC-CT.                                       RL1044.2
058400*    .02                                                          RL1044.2
058500     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   RL1044.2
058600              MOVE "OPEN/STATUS"  TO  RE-MARK                     RL1044.2
058700              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1044.2
058800              MOVE  "00" TO CORRECT-A                             RL1044.2
058900              PERFORM FAIL                                        RL1044.2
059000              ELSE                                                RL1044.2
059100              PERFORM PASS.                                       RL1044.2
059200     PERFORM  PRINT-DETAIL.                                       RL1044.2
059300     ADD      01 TO REC-CT.                                       RL1044.2
059400*    .03                                                          RL1044.2
059500     IF        RL-FS2-STATUS NOT EQUAL  TO "10"                   RL1044.2
059600              MOVE "ATEND/STATUS" TO RE-MARK                      RL1044.2
059700              MOVE  RL-FS2-STATUS TO COMPUTED-A                   RL1044.2
059800              MOVE "10" TO CORRECT-A                              RL1044.2
059900              PERFORM FAIL                                        RL1044.2
060000              ELSE                                                RL1044.2
060100              PERFORM PASS.                                       RL1044.2
060200     PERFORM  PRINT-DETAIL.                                       RL1044.2
060300     ADD      01 TO REC-CT.                                       RL1044.2
060400*    .04                                                          RL1044.2
060500     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   RL1044.2
060600              MOVE  "EXCEPTIN/STATUS" TO RE-MARK                  RL1044.2
060700              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 RL1044.2
060800              MOVE  "10" TO CORRECT-A                             RL1044.2
060900              PERFORM FAIL                                        RL1044.2
061000              ELSE                                                RL1044.2
061100              PERFORM PASS.                                       RL1044.2
061200     PERFORM  PRINT-DETAIL.                                       RL1044.2
061300     ADD      01 TO REC-CT.                                       RL1044.2
061400*    .05                                                          RL1044.2
061500     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   RL1044.2
061600              MOVE "NO/EXCEPTION" TO  RE-MARK                     RL1044.2
061700              MOVE WRK-XN-0002-003 TO COMPUTED-A                  RL1044.2
061800              MOVE "10" TO CORRECT-A                              RL1044.2
061900              PERFORM FAIL                                        RL1044.2
062000              ELSE                                                RL1044.2
062100              PERFORM PASS.                                       RL1044.2
062200     PERFORM  PRINT-DETAIL                                        RL1044.2
062300     ADD      01 TO REC-CT.                                       RL1044.2
062400*    .06                                                          RL1044.2
062500     MOVE     SPACE TO RL-FS2-STATUS.                             RL1044.2
062600     CLOSE    RL-FS2                                              RL1044.2
062700     IF        RL-FS2-STATUS NOT EQUAL TO "00"                    RL1044.2
062800              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1044.2
062900              MOVE  RL-FS2-STATUS TO  COMPUTED-A                  RL1044.2
063000              MOVE "00" TO CORRECT-A                              RL1044.2
063100              PERFORM FAIL                                        RL1044.2
063200              ELSE                                                RL1044.2
063300              PERFORM PASS.                                       RL1044.2
063400     PERFORM  PRINT-DETAIL.                                       RL1044.2
063500     ADD      01 TO REC-CT.                                       RL1044.2
063600*    .07                                                          RL1044.2
063700 CCVS-EXIT SECTION.                                               RL1044.2
063800 CCVS-999999.                                                     RL1044.2
063900     GO TO CLOSE-FILES.                                           RL1044.2
      *END-OF,RL104A                                                                  
