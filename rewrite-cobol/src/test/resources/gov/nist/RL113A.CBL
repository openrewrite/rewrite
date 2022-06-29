      *HEADER,COBOL,RL113A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1134.2
000200 PROGRAM-ID.                                                      RL1134.2
000300     RL113A.                                                      RL1134.2
000400****************************************************************  RL1134.2
000500*                                                              *  RL1134.2
000600*    VALIDATION FOR:-                                          *  RL1134.2
000700*                                                              *  RL1134.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1134.2
000900*                                                              *  RL1134.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1134.2
001100*                                                              *  RL1134.2
001200****************************************************************  RL1134.2
001300*                                                              *  RL1134.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1134.2
001500*                                                              *  RL1134.2
001600*            X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1134.2
001700*                    RELATIVE  I-O DATA FILE                   *  RL1134.2
001800*            X-55   SYSTEM PRINTER                             *  RL1134.2
001900*            X-69   ADDITIONAL VALUE OF CLAUSES                *  RL1134.2
002000*            X-74   VALUE OF IMPLEMENTOR-NAME                  *  RL1134.2
002100*            X-75   OBJECT OF VALUE OF CLAUSE                  *  RL1134.2
002200*            X-82   SOURCE-COMPUTER                            *  RL1134.2
002300*            X-83   OBJECT-COMPUTER.                           *  RL1134.2
002400*                                                              *  RL1134.2
002500****************************************************************  RL1134.2
002600*    RL113A                                                    *  RL1134.2
002700****************************************************************  RL1134.2
002800*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND    RL1134.2
002900*            SEMANTIC ACTIONS ASSOCIATED WITH THE "USE"           RL1134.2
003000*            STATEMENT.                                           RL1134.2
003100*                                                                 RL1134.2
003200*                                                                 RL1134.2
003300*            THIS PROGRAM CREATES A RELATIVE I-O FILE SEQUENTIALLYRL1134.2
003400*            (ACCESS MODE RANDOM)    AND THEN UPDATES SELECTIVE   RL1134.2
003500*            RECORDS OF THE FILE.  THE FILE STATUS CONTENTS ARE   RL1134.2
003600*            CAPTURED AND TESTED FOR ACCURACY FOR EACH OPEN, CLOSERL1134.2
003700*            READ AND REWRITE STATEMENT USED.  THE READ, WRITE ANDRL1134.2
003800*            REWRITE STATEMENTS ARE USED WITHOUT THE APPROPRIATE  RL1134.2
003900*            AT END ON INVALID KEY PHRASES.  THE OMISSION OF THESERL1134.2
004000*            PHRASES ARE PERMITTED IF AN APPLICABLE USE PROCEDURE RL1134.2
004100*            HAS BEEN SPECIFIED.                                  RL1134.2
004200*                                                                 RL1134.2
004300***************************************************               RL1134.2
004400 ENVIRONMENT DIVISION.                                            RL1134.2
004500 CONFIGURATION SECTION.                                           RL1134.2
004600 SOURCE-COMPUTER.                                                 RL1134.2
004700     XXXXX082.                                                    RL1134.2
004800 OBJECT-COMPUTER.                                                 RL1134.2
004900     XXXXX083.                                                    RL1134.2
005000 INPUT-OUTPUT SECTION.                                            RL1134.2
005100 FILE-CONTROL.                                                    RL1134.2
005200     SELECT PRINT-FILE ASSIGN TO                                  RL1134.2
005300     XXXXX055.                                                    RL1134.2
005400     SELECT   RL-FD2 ASSIGN                                       RL1134.2
005500     XXXXX022                                                     RL1134.2
005600              ORGANIZATION RELATIVE                               RL1134.2
005700              ACCESS  RANDOM                                      RL1134.2
005800              RELATIVE  RL-FD2-KEY                                RL1134.2
005900         FILE STATUS IS RL-FD2-STATUS.                            RL1134.2
006000 DATA DIVISION.                                                   RL1134.2
006100 FILE SECTION.                                                    RL1134.2
006200 FD  PRINT-FILE.                                                  RL1134.2
006300 01  PRINT-REC PICTURE X(120).                                    RL1134.2
006400 01  DUMMY-RECORD PICTURE X(120).                                 RL1134.2
006500 FD  RL-FD2                                                       RL1134.2
006600C    VALUE OF                                                     RL1134.2
006700C    XXXXX074                                                     RL1134.2
006800C    IS                                                           RL1134.2
006900C    XXXXX076                                                     RL1134.2
007000*    XXXXX069                                                     RL1134.2
007100     LABEL RECORDS ARE STANDARD                                   RL1134.2
007200     BLOCK CONTAINS 1 RECORDS                                     RL1134.2
007300     DATA RECORD RL-FD2R1-F-G-240.                                RL1134.2
007400 01  RL-FD2R1-F-G-240.                                            RL1134.2
007500     05 RL-FD2-WRK-120 PIC X(120).                                RL1134.2
007600     05 RL-FD2-GRP-120.                                           RL1134.2
007700        10 RL-FD2-WRK-XN-0001-O120F                               RL1134.2
007800                        PICTURE X OCCURS 120 TIMES.               RL1134.2
007900 WORKING-STORAGE SECTION.                                         RL1134.2
008000 01  GRP-0001.                                                    RL1134.2
008100     05 RL-FD2-KEY   PIC 9(8)  VALUE ZERO.                        RL1134.2
008200     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008300     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008400     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008500     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008600     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008700     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008800     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1134.2
008900     05 RL-FD2-STATUS     PIC  XX  VALUE  SPACE.                  RL1134.2
009000     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1134.2
009100     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1134.2
009200     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1134.2
009300     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1134.2
009400     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1134.2
009500     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1134.2
009600     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1134.2
009700     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1134.2
009800     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1134.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 RL1134.2
010000     03 FILE-RECORD-INFO-SKELETON.                                RL1134.2
010100        05 FILLER                 PICTURE X(48)       VALUE       RL1134.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1134.2
010300        05 FILLER                 PICTURE X(46)       VALUE       RL1134.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1134.2
010500        05 FILLER                 PICTURE X(26)       VALUE       RL1134.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1134.2
010700        05 FILLER                 PICTURE X(37)       VALUE       RL1134.2
010800             ",RECKEY=                             ".             RL1134.2
010900        05 FILLER                 PICTURE X(38)       VALUE       RL1134.2
011000             ",ALTKEY1=                             ".            RL1134.2
011100        05 FILLER                 PICTURE X(38)       VALUE       RL1134.2
011200             ",ALTKEY2=                             ".            RL1134.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1134.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1134.2
011500        05 FILE-RECORD-INFO-P1-120.                               RL1134.2
011600           07 FILLER              PIC X(5).                       RL1134.2
011700           07 XFILE-NAME           PIC X(6).                      RL1134.2
011800           07 FILLER              PIC X(8).                       RL1134.2
011900           07 XRECORD-NAME         PIC X(6).                      RL1134.2
012000           07 FILLER              PIC X(1).                       RL1134.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       RL1134.2
012200           07 FILLER              PIC X(7).                       RL1134.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      RL1134.2
012400           07 FILLER              PIC X(6).                       RL1134.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       RL1134.2
012600           07 FILLER              PIC X(5).                       RL1134.2
012700           07 ODO-NUMBER          PIC 9(4).                       RL1134.2
012800           07 FILLER              PIC X(5).                       RL1134.2
012900           07 XPROGRAM-NAME        PIC X(5).                      RL1134.2
013000           07 FILLER              PIC X(7).                       RL1134.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      RL1134.2
013200           07 FILLER              PIC X(7).                       RL1134.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       RL1134.2
013400           07 FILLER              PIC X(1).                       RL1134.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      RL1134.2
013600           07 FILLER              PIC X(6).                       RL1134.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       RL1134.2
013800           07 FILLER              PIC X(5).                       RL1134.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      RL1134.2
014000           07 FILLER              PIC X(6).                       RL1134.2
014100           07 XLABEL-TYPE          PIC X(1).                      RL1134.2
014200        05 FILE-RECORD-INFO-P121-240.                             RL1134.2
014300           07 FILLER              PIC X(8).                       RL1134.2
014400           07 XRECORD-KEY          PIC X(29).                     RL1134.2
014500           07 FILLER              PIC X(9).                       RL1134.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      RL1134.2
014700           07 FILLER              PIC X(9).                       RL1134.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      RL1134.2
014900           07 FILLER              PIC X(7).                       RL1134.2
015000 01  TEST-RESULTS.                                                RL1134.2
015100     02 FILLER                   PIC X      VALUE SPACE.          RL1134.2
015200     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1134.2
015300     02 FILLER                   PIC X      VALUE SPACE.          RL1134.2
015400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1134.2
015500     02 FILLER                   PIC X      VALUE SPACE.          RL1134.2
015600     02  PAR-NAME.                                                RL1134.2
015700       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1134.2
015800       03  PARDOT-X              PIC X      VALUE SPACE.          RL1134.2
015900       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1134.2
016000     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1134.2
016100     02 RE-MARK                  PIC X(61).                       RL1134.2
016200 01  TEST-COMPUTED.                                               RL1134.2
016300     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1134.2
016400     02 FILLER                   PIC X(17)  VALUE                 RL1134.2
016500            "       COMPUTED=".                                   RL1134.2
016600     02 COMPUTED-X.                                               RL1134.2
016700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1134.2
016800     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1134.2
016900                                 PIC -9(9).9(9).                  RL1134.2
017000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1134.2
017100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1134.2
017200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1134.2
017300     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1134.2
017400         04 COMPUTED-18V0                    PIC -9(18).          RL1134.2
017500         04 FILLER                           PIC X.               RL1134.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             RL1134.2
017700 01  TEST-CORRECT.                                                RL1134.2
017800     02 FILLER PIC X(30) VALUE SPACE.                             RL1134.2
017900     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1134.2
018000     02 CORRECT-X.                                                RL1134.2
018100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1134.2
018200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1134.2
018300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1134.2
018400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1134.2
018500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1134.2
018600     03      CR-18V0 REDEFINES CORRECT-A.                         RL1134.2
018700         04 CORRECT-18V0                     PIC -9(18).          RL1134.2
018800         04 FILLER                           PIC X.               RL1134.2
018900     03 FILLER PIC X(2) VALUE SPACE.                              RL1134.2
019000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1134.2
019100 01  CCVS-C-1.                                                    RL1134.2
019200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1134.2
019300-    "SS  PARAGRAPH-NAME                                          RL1134.2
019400-    "       REMARKS".                                            RL1134.2
019500     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1134.2
019600 01  CCVS-C-2.                                                    RL1134.2
019700     02 FILLER                     PIC X        VALUE SPACE.      RL1134.2
019800     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1134.2
019900     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1134.2
020000     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1134.2
020100     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1134.2
020200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1134.2
020300 01  REC-CT                        PIC 99       VALUE ZERO.       RL1134.2
020400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1134.2
020500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1134.2
020600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1134.2
020700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1134.2
020800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1134.2
020900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1134.2
021000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1134.2
021100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1134.2
021200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1134.2
021300 01  CCVS-H-1.                                                    RL1134.2
021400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1134.2
021500     02  FILLER                    PIC X(42)    VALUE             RL1134.2
021600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1134.2
021700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1134.2
021800 01  CCVS-H-2A.                                                   RL1134.2
021900   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1134.2
022000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1134.2
022100   02  FILLER                        PIC XXXX   VALUE             RL1134.2
022200     "4.2 ".                                                      RL1134.2
022300   02  FILLER                        PIC X(28)  VALUE             RL1134.2
022400            " COPY - NOT FOR DISTRIBUTION".                       RL1134.2
022500   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1134.2
022600                                                                  RL1134.2
022700 01  CCVS-H-2B.                                                   RL1134.2
022800   02  FILLER                        PIC X(15)  VALUE             RL1134.2
022900            "TEST RESULT OF ".                                    RL1134.2
023000   02  TEST-ID                       PIC X(9).                    RL1134.2
023100   02  FILLER                        PIC X(4)   VALUE             RL1134.2
023200            " IN ".                                               RL1134.2
023300   02  FILLER                        PIC X(12)  VALUE             RL1134.2
023400     " HIGH       ".                                              RL1134.2
023500   02  FILLER                        PIC X(22)  VALUE             RL1134.2
023600            " LEVEL VALIDATION FOR ".                             RL1134.2
023700   02  FILLER                        PIC X(58)  VALUE             RL1134.2
023800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1134.2
023900 01  CCVS-H-3.                                                    RL1134.2
024000     02  FILLER                      PIC X(34)  VALUE             RL1134.2
024100            " FOR OFFICIAL USE ONLY    ".                         RL1134.2
024200     02  FILLER                      PIC X(58)  VALUE             RL1134.2
024300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1134.2
024400     02  FILLER                      PIC X(28)  VALUE             RL1134.2
024500            "  COPYRIGHT   1985 ".                                RL1134.2
024600 01  CCVS-E-1.                                                    RL1134.2
024700     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1134.2
024800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1134.2
024900     02 ID-AGAIN                     PIC X(9).                    RL1134.2
025000     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1134.2
025100 01  CCVS-E-2.                                                    RL1134.2
025200     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1134.2
025300     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1134.2
025400     02 CCVS-E-2-2.                                               RL1134.2
025500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1134.2
025600         03 FILLER                   PIC X      VALUE SPACE.      RL1134.2
025700         03 ENDER-DESC               PIC X(44)  VALUE             RL1134.2
025800            "ERRORS ENCOUNTERED".                                 RL1134.2
025900 01  CCVS-E-3.                                                    RL1134.2
026000     02  FILLER                      PIC X(22)  VALUE             RL1134.2
026100            " FOR OFFICIAL USE ONLY".                             RL1134.2
026200     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1134.2
026300     02  FILLER                      PIC X(58)  VALUE             RL1134.2
026400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1134.2
026500     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1134.2
026600     02 FILLER                       PIC X(15)  VALUE             RL1134.2
026700             " COPYRIGHT 1985".                                   RL1134.2
026800 01  CCVS-E-4.                                                    RL1134.2
026900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1134.2
027000     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1134.2
027100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1134.2
027200     02 FILLER                       PIC X(40)  VALUE             RL1134.2
027300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1134.2
027400 01  XXINFO.                                                      RL1134.2
027500     02 FILLER                       PIC X(19)  VALUE             RL1134.2
027600            "*** INFORMATION ***".                                RL1134.2
027700     02 INFO-TEXT.                                                RL1134.2
027800       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1134.2
027900       04 XXCOMPUTED                 PIC X(20).                   RL1134.2
028000       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1134.2
028100       04 XXCORRECT                  PIC X(20).                   RL1134.2
028200     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1134.2
028300 01  HYPHEN-LINE.                                                 RL1134.2
028400     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1134.2
028500     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1134.2
028600-    "*****************************************".                 RL1134.2
028700     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1134.2
028800-    "******************************".                            RL1134.2
028900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1134.2
029000     "RL113A".                                                    RL1134.2
029100 PROCEDURE DIVISION.                                              RL1134.2
029200 DECLARATIVES.                                                    RL1134.2
029300 RL-FD2-01 SECTION.                                               RL1134.2
029400     USE     AFTER ERROR PROCEDURE INPUT.                         RL1134.2
029500 RL-FD2-01-01.                                                    RL1134.2
029600     MOVE   "USE AFTER ERROR INPUT PROCEDURE SHOULD NOT BE OBEYED"RL1134.2
029700          TO RE-MARK.                                             RL1134.2
029800     MOVE   "RL-FD2-01-01" TO PAR-NAME.                           RL1134.2
029900     MOVE   "FAIL*" TO P-OR-F.                                    RL1134.2
030000     ADD     1 TO ERROR-COUNTER.                                  RL1134.2
030100*                                                                 RL1134.2
030200     IF      REC-CT NOT EQUAL TO ZERO                             RL1134.2
030300             MOVE "." TO PARDOT-X                                 RL1134.2
030400             MOVE REC-CT TO DOTVALUE.                             RL1134.2
030500     MOVE    TEST-RESULTS TO PRINT-REC.                           RL1134.2
030600     PERFORM D1-WRITE-LINE THRU D1-WRITE-LINE-EXIT.               RL1134.2
030700     IF      P-OR-F EQUAL TO "FAIL*"                              RL1134.2
030800             PERFORM D1-WRITE-LINE THRU D1-WRITE-LINE-EXIT        RL1134.2
030900             PERFORM D1-FAIL-ROUTINE THRU D1-FAIL-ROUTINE-EX      RL1134.2
031000      ELSE                                                        RL1134.2
031100             PERFORM D1-BAIL-OUT THRU D1-BAIL-OUT-EX.             RL1134.2
031200     MOVE    SPACE TO P-OR-F.                                     RL1134.2
031300     MOVE    SPACE TO COMPUTED-X.                                 RL1134.2
031400     MOVE    SPACE TO CORRECT-X.                                  RL1134.2
031500     IF      REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.        RL1134.2
031600     MOVE    SPACE TO RE-MARK.                                    RL1134.2
031700     GO TO   RL-FD2-01-EXIT.                                      RL1134.2
031800 D1-FAIL-ROUTINE.                                                 RL1134.2
031900     IF      COMPUTED-X NOT EQUAL TO SPACE                        RL1134.2
032000             GO TO   D1-FAIL-ROUTINE-WRITE.                       RL1134.2
032100     IF      CORRECT-X NOT EQUAL TO SPACE                         RL1134.2
032200             GO TO   D1-FAIL-ROUTINE-WRITE.                       RL1134.2
032300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1134.2
032400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  RL1134.2
032500     MOVE    XXINFO TO DUMMY-RECORD.                              RL1134.2
032600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
032700     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
032800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
032900     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
033000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1134.2
033100     GO TO   D1-FAIL-ROUTINE-EX.                                  RL1134.2
033200 D1-FAIL-ROUTINE-WRITE.                                           RL1134.2
033300     MOVE    TEST-COMPUTED TO PRINT-REC.                          RL1134.2
033400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
033500     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
033600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                RL1134.2
033700     MOVE    TEST-CORRECT TO PRINT-REC.                           RL1134.2
033800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
033900     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
034000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
034100     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
034200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        RL1134.2
034300 D1-FAIL-ROUTINE-EX.                                              RL1134.2
034400     EXIT.                                                        RL1134.2
034500 D1-BAIL-OUT.                                                     RL1134.2
034600     IF      COMPUTED-A NOT EQUAL TO SPACE                        RL1134.2
034700             GO TO    D1-BAIL-OUT-WRITE.                          RL1134.2
034800     IF      CORRECT-A EQUAL TO SPACE                             RL1134.2
034900             GO TO    D1-BAIL-OUT-EX.                             RL1134.2
035000 D1-BAIL-OUT-WRITE.                                               RL1134.2
035100     MOVE    CORRECT-A  TO XXCORRECT.                             RL1134.2
035200     MOVE    COMPUTED-A TO XXCOMPUTED.                            RL1134.2
035300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1134.2
035400     MOVE    XXINFO TO DUMMY-RECORD.                              RL1134.2
035500     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
035600     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
035700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
035800     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
035900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1134.2
036000 D1-BAIL-OUT-EX.                                                  RL1134.2
036100     EXIT.                                                        RL1134.2
036200 D1-WRITE-LINE.                                                   RL1134.2
036300     ADD     1 TO RECORD-COUNT.                                   RL1134.2
036400*    IF      RECORD-COUNT GREATER 50                              RL1134.2
036500*            MOVE    DUMMY-RECORD TO DUMMY-HOLD                   RL1134.2
036600*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
036700*            WRITE   DUMMY-RECORD AFTER ADVANCING PAGE            RL1134.2
036800*            MOVE    CCVS-C-1 TO DUMMY-RECORD                     RL1134.2
036900*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
037000*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
037100*            MOVE    CCVS-C-2 TO DUMMY-RECORD                     RL1134.2
037200*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
037300*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
037400*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
037500*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
037600*            MOVE    HYPHEN-LINE TO DUMMY-RECORD                  RL1134.2
037700*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
037800*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
037900*            MOVE    DUMMY-HOLD TO DUMMY-RECORD                   RL1134.2
038000*            MOVE    ZERO TO RECORD-COUNT.                        RL1134.2
038100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
038200     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
038300 D1-WRITE-LINE-EXIT.                                              RL1134.2
038400     EXIT.                                                        RL1134.2
038500 RL-FD2-01-EXIT.                                                  RL1134.2
038600     EXIT.                                                        RL1134.2
038700*                                                                 RL1134.2
038800 RL-FD2-02 SECTION.                                               RL1134.2
038900     USE      AFTER STANDARD EXCEPTION PROCEDURE ON RL-FD2.       RL1134.2
039000 RL-FD2-01-03.                                                    RL1134.2
039100     MOVE   "PASS " TO P-OR-F.                                    RL1134.2
039200     ADD     1 TO PASS-COUNTER.                                   RL1134.2
039300*                                                                 RL1134.2
039400     IF      REC-CT NOT EQUAL TO ZERO                             RL1134.2
039500             MOVE "." TO PARDOT-X                                 RL1134.2
039600             MOVE REC-CT TO DOTVALUE.                             RL1134.2
039700     MOVE    TEST-RESULTS TO PRINT-REC.                           RL1134.2
039800     PERFORM D2-WRITE-LINE THRU D2-WRITE-LINE-EXIT.               RL1134.2
039900*                                                                 RL1134.2
040000     IF      P-OR-F EQUAL TO "FAIL*"                              RL1134.2
040100             PERFORM D2-WRITE-LINE THRU D2-WRITE-LINE-EXIT        RL1134.2
040200             PERFORM D2-FAIL-ROUTINE THRU D2-FAIL-ROUTINE-EX      RL1134.2
040300      ELSE                                                        RL1134.2
040400             PERFORM D2-BAIL-OUT THRU D2-BAIL-OUT-EX.             RL1134.2
040500     MOVE    SPACE TO P-OR-F.                                     RL1134.2
040600     MOVE    SPACE TO COMPUTED-X.                                 RL1134.2
040700     MOVE    SPACE TO CORRECT-X.                                  RL1134.2
040800     IF      REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.        RL1134.2
040900     MOVE    SPACE TO RE-MARK.                                    RL1134.2
041000     GO TO   RL-FD2-02-EXIT.                                      RL1134.2
041100 D2-FAIL-ROUTINE.                                                 RL1134.2
041200     IF      COMPUTED-X NOT EQUAL TO SPACE                        RL1134.2
041300             GO TO   D2-FAIL-ROUTINE-WRITE.                       RL1134.2
041400     IF      CORRECT-X NOT EQUAL TO SPACE                         RL1134.2
041500             GO TO   D2-FAIL-ROUTINE-WRITE.                       RL1134.2
041600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1134.2
041700     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  RL1134.2
041800     MOVE    XXINFO TO DUMMY-RECORD.                              RL1134.2
041900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
042000     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
042100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
042200     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
042300     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1134.2
042400     GO TO   D2-FAIL-ROUTINE-EX.                                  RL1134.2
042500 D2-FAIL-ROUTINE-WRITE.                                           RL1134.2
042600     MOVE    TEST-COMPUTED TO PRINT-REC.                          RL1134.2
042700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
042800     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
042900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                RL1134.2
043000     MOVE    TEST-CORRECT TO PRINT-REC.                           RL1134.2
043100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
043200     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
043300     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
043400     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
043500     MOVE    SPACES TO COR-ANSI-REFERENCE.                        RL1134.2
043600 D2-FAIL-ROUTINE-EX.                                              RL1134.2
043700     EXIT.                                                        RL1134.2
043800 D2-BAIL-OUT.                                                     RL1134.2
043900     IF      COMPUTED-A NOT EQUAL TO SPACE                        RL1134.2
044000             GO TO    D2-BAIL-OUT-WRITE.                          RL1134.2
044100     IF      CORRECT-A EQUAL TO SPACE                             RL1134.2
044200             GO TO    D2-BAIL-OUT-EX.                             RL1134.2
044300 D2-BAIL-OUT-WRITE.                                               RL1134.2
044400     MOVE    CORRECT-A  TO XXCORRECT.                             RL1134.2
044500     MOVE    COMPUTED-A TO XXCOMPUTED.                            RL1134.2
044600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1134.2
044700     MOVE    XXINFO TO DUMMY-RECORD.                              RL1134.2
044800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
044900     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
045000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
045100     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
045200     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1134.2
045300 D2-BAIL-OUT-EX.                                                  RL1134.2
045400     EXIT.                                                        RL1134.2
045500 RL-FD2-02-EXIT.                                                  RL1134.2
045600     EXIT.                                                        RL1134.2
045700 D2-WRITE-LINE.                                                   RL1134.2
045800     ADD     1 TO RECORD-COUNT.                                   RL1134.2
045900*    IF      RECORD-COUNT GREATER 50                              RL1134.2
046000*            MOVE    DUMMY-RECORD TO DUMMY-HOLD                   RL1134.2
046100*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
046200*            WRITE   DUMMY-RECORD AFTER ADVANCING PAGE            RL1134.2
046300*            MOVE    CCVS-C-1 TO DUMMY-RECORD                     RL1134.2
046400*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
046500*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
046600*            MOVE    CCVS-C-2 TO DUMMY-RECORD                     RL1134.2
046700*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
046800*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
046900*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
047000*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
047100*            MOVE    HYPHEN-LINE TO DUMMY-RECORD                  RL1134.2
047200*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1134.2
047300*            MOVE    SPACE TO DUMMY-RECORD                        RL1134.2
047400*            MOVE    DUMMY-HOLD TO DUMMY-RECORD                   RL1134.2
047500*            MOVE    ZERO TO RECORD-COUNT.                        RL1134.2
047600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1134.2
047700     MOVE    SPACE TO DUMMY-RECORD.                               RL1134.2
047800 D2-WRITE-LINE-EXIT.                                              RL1134.2
047900     EXIT.                                                        RL1134.2
048000 END DECLARATIVES.                                                RL1134.2
048100 CCVS1 SECTION.                                                   RL1134.2
048200 OPEN-FILES.                                                      RL1134.2
048300     OPEN    OUTPUT PRINT-FILE.                                   RL1134.2
048400     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1134.2
048500     MOVE    SPACE TO TEST-RESULTS.                               RL1134.2
048600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1134.2
048700     MOVE    ZERO TO REC-SKL-SUB.                                 RL1134.2
048800     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1134.2
048900 CCVS-INIT-FILE.                                                  RL1134.2
049000     ADD     1 TO REC-SKL-SUB.                                    RL1134.2
049100     MOVE    FILE-RECORD-INFO-SKELETON                            RL1134.2
049200          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1134.2
049300 CCVS-INIT-EXIT.                                                  RL1134.2
049400     GO TO CCVS1-EXIT.                                            RL1134.2
049500 CLOSE-FILES.                                                     RL1134.2
049600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1134.2
049700 TERMINATE-CCVS.                                                  RL1134.2
049800*    EXIT PROGRAM.                                                RL1134.2
049900*TERMINATE-CALL.                                                  RL1134.2
050000     STOP     RUN.                                                RL1134.2
050100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1134.2
050200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1134.2
050300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1134.2
050400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1134.2
050500     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1134.2
050600 PRINT-DETAIL.                                                    RL1134.2
050700     IF REC-CT NOT EQUAL TO ZERO                                  RL1134.2
050800             MOVE "." TO PARDOT-X                                 RL1134.2
050900             MOVE REC-CT TO DOTVALUE.                             RL1134.2
051000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1134.2
051100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1134.2
051200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1134.2
051300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1134.2
051400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1134.2
051500     MOVE SPACE TO CORRECT-X.                                     RL1134.2
051600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1134.2
051700     MOVE     SPACE TO RE-MARK.                                   RL1134.2
051800 HEAD-ROUTINE.                                                    RL1134.2
051900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1134.2
052000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1134.2
052100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1134.2
052200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1134.2
052300 COLUMN-NAMES-ROUTINE.                                            RL1134.2
052400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1134.2
052500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1134.2
052600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1134.2
052700 END-ROUTINE.                                                     RL1134.2
052800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1134.2
052900 END-RTN-EXIT.                                                    RL1134.2
053000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1134.2
053100 END-ROUTINE-1.                                                   RL1134.2
053200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1134.2
053300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1134.2
053400      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1134.2
053500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1134.2
053600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1134.2
053700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1134.2
053800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1134.2
053900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1134.2
054000  END-ROUTINE-12.                                                 RL1134.2
054100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1134.2
054200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1134.2
054300         MOVE "NO " TO ERROR-TOTAL                                RL1134.2
054400         ELSE                                                     RL1134.2
054500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1134.2
054600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1134.2
054700     PERFORM WRITE-LINE.                                          RL1134.2
054800 END-ROUTINE-13.                                                  RL1134.2
054900     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1134.2
055000         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1134.2
055100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1134.2
055200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1134.2
055300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1134.2
055400      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1134.2
055500          MOVE "NO " TO ERROR-TOTAL                               RL1134.2
055600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1134.2
055700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1134.2
055800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1134.2
055900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1134.2
056000 WRITE-LINE.                                                      RL1134.2
056100     ADD 1 TO RECORD-COUNT.                                       RL1134.2
056200*    IF RECORD-COUNT GREATER 50                                   RL1134.2
056300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1134.2
056400*        MOVE SPACE TO DUMMY-RECORD                               RL1134.2
056500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1134.2
056600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1134.2
056700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1134.2
056800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1134.2
056900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1134.2
057000*        MOVE ZERO TO RECORD-COUNT.                               RL1134.2
057100     PERFORM WRT-LN.                                              RL1134.2
057200 WRT-LN.                                                          RL1134.2
057300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1134.2
057400     MOVE SPACE TO DUMMY-RECORD.                                  RL1134.2
057500 BLANK-LINE-PRINT.                                                RL1134.2
057600     PERFORM WRT-LN.                                              RL1134.2
057700 FAIL-ROUTINE.                                                    RL1134.2
057800     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1134.2
057900            GO TO   FAIL-ROUTINE-WRITE.                           RL1134.2
058000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1134.2
058100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1134.2
058200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1134.2
058300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1134.2
058400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1134.2
058500     GO TO  FAIL-ROUTINE-EX.                                      RL1134.2
058600 FAIL-ROUTINE-WRITE.                                              RL1134.2
058700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1134.2
058800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1134.2
058900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1134.2
059000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1134.2
059100 FAIL-ROUTINE-EX. EXIT.                                           RL1134.2
059200 BAIL-OUT.                                                        RL1134.2
059300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1134.2
059400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1134.2
059500 BAIL-OUT-WRITE.                                                  RL1134.2
059600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1134.2
059700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1134.2
059800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1134.2
059900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1134.2
060000 BAIL-OUT-EX. EXIT.                                               RL1134.2
060100 CCVS1-EXIT.                                                      RL1134.2
060200     EXIT.                                                        RL1134.2
060300 SECT-RL113A-001 SECTION.                                         RL1134.2
060400 REL-INIT-009.                                                    RL1134.2
060500     MOVE    "VIII-36 4.8.4 GR4" TO ANSI-REFERENCE.               RL1134.2
060600     MOVE    "REL-TEST-009" TO PAR-NAME.                          RL1134.2
060700     MOVE     "CREATE RL-FD2" TO  FEATURE                         RL1134.2
060800     MOVE     "RL-FD2"        TO  XFILE-NAME (2).                 RL1134.2
060900     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL1134.2
061000     MOVE CCVS-PGM-ID             TO  XPROGRAM-NAME (2).          RL1134.2
061100     MOVE     000240          TO  XRECORD-LENGTH (2).             RL1134.2
061200     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL1134.2
061300     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL1134.2
061400     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL1134.2
061500     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL1134.2
061600     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL1134.2
061700     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL1134.2
061800*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1134.2
061900     MOVE     1         TO  WRK-CS-09V00-012.                     RL1134.2
062000     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1134.2
062100                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1134.2
062200                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1134.2
062300     MOVE     SPACE     TO  RL-FD2-STATUS.                        RL1134.2
062400     MOVE     90000002  TO  RL-FD2-KEY.                           RL1134.2
062500     MOVE     01 TO REC-CT.                                       RL1134.2
062600     OPEN     OUTPUT    RL-FD2.                                   RL1134.2
062700     MOVE     RL-FD2-STATUS TO WRK-XN-0002-001.                   RL1134.2
062800*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL1134.2
062900 REL-TEST-009-R.                                                  RL1134.2
063000     MOVE XRECORD-NUMBER (2) TO RL-FD2-KEY.                       RL1134.2
063100     MOVE    "99"  TO RL-FD2-STATUS.                              RL1134.2
063200     MOVE     FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.      RL1134.2
063300     MOVE     FILE-RECORD-INFO-P1-120 (2) TO                      RL1134.2
063400               RL-FD2-GRP-120.                                    RL1134.2
063500     WRITE    RL-FD2R1-F-G-240.                                   RL1134.2
063600     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1134.2
063700              GO TO  REL-TEST-009-2.                              RL1134.2
063800     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL1134.2
063900             GO TO REL-TEST-009-2.                                RL1134.2
064000     ADD      01 TO  XRECORD-NUMBER (2).                          RL1134.2
064100     GO TO    REL-TEST-009-R.                                     RL1134.2
064200 REL-TEST-009-2.                                                  RL1134.2
064300     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  RL1134.2
064400              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 RL1134.2
064500              MOVE ZERO TO CORRECT-18V0                           RL1134.2
064600              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1134.2
064700              PERFORM   FAIL                                      RL1134.2
064800              ELSE                                                RL1134.2
064900              PERFORM   PASS.                                     RL1134.2
065000     PERFORM  PRINT-DETAIL.                                       RL1134.2
065100     ADD      01 TO REC-CT.                                       RL1134.2
065200*    .01                                                          RL1134.2
065300     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 RL1134.2
065400              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1134.2
065500              MOVE  500 TO CORRECT-18V0                           RL1134.2
065600             MOVE    XRECORD-NUMBER (2)  TO COMPUTED-18V0         RL1134.2
065700              PERFORM  FAIL                                       RL1134.2
065800              ELSE                                                RL1134.2
065900              PERFORM PASS.                                       RL1134.2
066000     PERFORM  PRINT-DETAIL.                                       RL1134.2
066100     ADD      01 TO REC-CT.                                       RL1134.2
066200*    .02                                                          RL1134.2
066300     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  RL1134.2
066400              MOVE "STATUS/OPEN" TO RE-MARK                       RL1134.2
066500              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1134.2
066600              MOVE "00" TO CORRECT-A                              RL1134.2
066700              PERFORM  FAIL                                       RL1134.2
066800              ELSE                                                RL1134.2
066900              PERFORM  PASS.                                      RL1134.2
067000     PERFORM  PRINT-DETAIL.                                       RL1134.2
067100     ADD      01 TO REC-CT.                                       RL1134.2
067200*    .03                                                          RL1134.2
067300     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1134.2
067400              MOVE "STATUS/WRITE" TO RE-MARK                      RL1134.2
067500              MOVE RL-FD2-STATUS TO COMPUTED-A                    RL1134.2
067600              MOVE "00" TO CORRECT-A                              RL1134.2
067700              PERFORM   FAIL                                      RL1134.2
067800              ELSE                                                RL1134.2
067900              PERFORM   PASS.                                     RL1134.2
068000     PERFORM  PRINT-DETAIL.                                       RL1134.2
068100     ADD      01 TO REC-CT.                                       RL1134.2
068200*    .04                                                          RL1134.2
068300     MOVE     SPACE  TO  RL-FD2-STATUS.                           RL1134.2
068400     CLOSE    RL-FD2.                                             RL1134.2
068500     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1134.2
068600              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1134.2
068700              MOVE   RL-FD2-STATUS TO COMPUTED-A                  RL1134.2
068800              MOVE "00" TO CORRECT-A                              RL1134.2
068900              PERFORM FAIL                                        RL1134.2
069000              ELSE                                                RL1134.2
069100              PERFORM PASS.                                       RL1134.2
069200     PERFORM  PRINT-DETAIL.                                       RL1134.2
069300     ADD      01 TO REC-CT.                                       RL1134.2
069400*    .05                                                          RL1134.2
069500 REL-INIT-010.                                                    RL1134.2
069600     MOVE    "REL-TEST-010" TO PAR-NAME.                          RL1134.2
069700     MOVE     2     TO  WRK-CS-09V00-012.                         RL1134.2
069800     MOVE     ZERO  TO  RL-FD2-KEY.                               RL1134.2
069900     MOVE     ZERO  TO  WRK-CS-09V00-013.                         RL1134.2
070000     MOVE     ZERO  TO  WRK-CS-09V00-014.                         RL1134.2
070100     MOVE     ZERO  TO  WRK-CS-09V00-015.                         RL1134.2
070200     MOVE     ZERO  TO  WRK-CS-09V00-016.                         RL1134.2
070300     MOVE     ZERO  TO  WRK-CS-09V00-017.                         RL1134.2
070400     MOVE     ZERO  TO  WRK-CS-09V00-018.                         RL1134.2
070500     MOVE     01 TO REC-CT.                                       RL1134.2
070600     OPEN     I-O   RL-FD2.                                       RL1134.2
070700     MOVE     SPACE     TO  WRK-XN-0002-002                       RL1134.2
070800     MOVE     SPACE     TO  WRK-XN-0002-003                       RL1134.2
070900     MOVE     SPACE     TO  WRK-XN-0002-004                       RL1134.2
071000     MOVE     RL-FD2-STATUS TO WRK-XN-0002-001                    RL1134.2
071100     MOVE     SPACE TO RL-FD2-STATUS.                             RL1134.2
071200*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       RL1134.2
071300     MOVE    "USE/FILE STATUS"  TO FEATURE.                       RL1134.2
071400 REL-TEST-010-R.                                                  RL1134.2
071500     ADD      1 TO RL-FD2-KEY.                                    RL1134.2
071600     ADD      1 TO WRK-CS-09V00-014.                              RL1134.2
071700     ADD      1 TO WRK-CS-09V00-015.                              RL1134.2
071800     READ     RL-FD2 RECORD.                                      RL1134.2
071900     IF       RL-FD2-STATUS EQUAL TO "23"                         RL1134.2
072000              GO TO  REL-TEST-010-3.                              RL1134.2
072100     MOVE     RL-FD2-WRK-120 TO FILE-RECORD-INFO-P1-120 (2).      RL1134.2
072200     IF       WRK-CS-09V00-015 EQUAL TO 5                         RL1134.2
072300              ADD 01 TO UPDATE-NUMBER (2)                         RL1134.2
072400              MOVE FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120  RL1134.2
072500              REWRITE RL-FD2R1-F-G-240                            RL1134.2
072600           MOVE ZERO TO WRK-CS-09V00-015                          RL1134.2
072700              GO TO  REL-TEST-010-2.                              RL1134.2
072800     IF       WRK-CS-09V00-014 GREATER 500                        RL1134.2
072900              MOVE 1 TO WRK-CS-09V00-013                          RL1134.2
073000              GO TO  REL-TEST-010-3.                              RL1134.2
073100     GO TO    REL-TEST-010-R.                                     RL1134.2
073200 REL-TEST-010-2.                                                  RL1134.2
073300     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1134.2
073400              ADD 1 TO WRK-CS-09V00-016.                          RL1134.2
073500     MOVE    SPACE TO RL-FD2-STATUS.                              RL1134.2
073600     GO TO    REL-TEST-010-R.                                     RL1134.2
073700 REL-TEST-010-3.                                                  RL1134.2
073800     IF       WRK-CS-09V00-013 NOT EQUAL TO 0                     RL1134.2
073900              MOVE  "EXCEPTIONS/ERRORS" TO RE-MARK                RL1134.2
074000              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              RL1134.2
074100              MOVE  0 TO CORRECT-18V0                             RL1134.2
074200              PERFORM FAIL                                        RL1134.2
074300              ELSE                                                RL1134.2
074400              PERFORM PASS.                                       RL1134.2
074500     PERFORM  PRINT-DETAIL.                                       RL1134.2
074600     ADD      01 TO REC-CT.                                       RL1134.2
074700*    .01                                                          RL1134.2
074800     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   RL1134.2
074900              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1134.2
075000              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1134.2
075100              MOVE 501 TO CORRECT-18V0                            RL1134.2
075200              PERFORM FAIL                                        RL1134.2
075300              ELSE                                                RL1134.2
075400             PERFORM PASS.                                        RL1134.2
075500     PERFORM  PRINT-DETAIL.                                       RL1134.2
075600     ADD      01 TO REC-CT.                                       RL1134.2
075700*    .02                                                          RL1134.2
075800     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   RL1134.2
075900              MOVE "OPEN/STATUS"  TO  RE-MARK                     RL1134.2
076000              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1134.2
076100              MOVE  "00" TO CORRECT-A                             RL1134.2
076200              PERFORM FAIL                                        RL1134.2
076300              ELSE                                                RL1134.2
076400              PERFORM PASS.                                       RL1134.2
076500     PERFORM  PRINT-DETAIL.                                       RL1134.2
076600     ADD      01 TO REC-CT.                                       RL1134.2
076700*    .03                                                          RL1134.2
076800     IF        RL-FD2-STATUS NOT EQUAL  TO "23"                   RL1134.2
076900              MOVE "AT END/STATUS" TO RE-MARK                     RL1134.2
077000              MOVE  RL-FD2-STATUS TO COMPUTED-A                   RL1134.2
077100              MOVE "23" TO CORRECT-A                              RL1134.2
077200              PERFORM FAIL                                        RL1134.2
077300              ELSE                                                RL1134.2
077400              PERFORM PASS.                                       RL1134.2
077500     PERFORM  PRINT-DETAIL.                                       RL1134.2
077600     ADD      01 TO REC-CT.                                       RL1134.2
077700*    .04                                                          RL1134.2
077800*    IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   RL1134.2
077900*             MOVE  "EXCEPTION/STATUS" TO RE-MARK                 RL1134.2
078000*             MOVE  WRK-XN-0002-002 TO COMPUTED-A                 RL1134.2
078100*             MOVE  "10" TO CORRECT-A                             RL1134.2
078200*             PERFORM FAIL                                        RL1134.2
078300*             ELSE                                                RL1134.2
078400*             PERFORM PASS.                                       RL1134.2
078500*    PERFORM  PRINT-DETAIL.                                       RL1134.2
078600*    ADD      01 TO REC-CT.                                       RL1134.2
078700*    .05                                                          RL1134.2
078800*    IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   RL1134.2
078900*             MOVE "NO EXCEPTION" TO  RE-MARK                     RL1134.2
079000*             MOVE WRK-XN-0002-003 TO COMPUTED-A                  RL1134.2
079100*             MOVE "10" TO CORRECT-A                              RL1134.2
079200*             PERFORM FAIL                                        RL1134.2
079300*             ELSE                                                RL1134.2
079400*             PERFORM PASS.                                       RL1134.2
079500*    PERFORM  PRINT-DETAIL                                        RL1134.2
079600*    ADD      01 TO REC-CT.                                       RL1134.2
079700*    .06                                                          RL1134.2
079800     MOVE     SPACE TO RL-FD2-STATUS.                             RL1134.2
079900     CLOSE    RL-FD2                                              RL1134.2
080000     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1134.2
080100              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1134.2
080200              MOVE  RL-FD2-STATUS TO  COMPUTED-A                  RL1134.2
080300              MOVE "00" TO CORRECT-A                              RL1134.2
080400              PERFORM FAIL                                        RL1134.2
080500              ELSE                                                RL1134.2
080600              PERFORM PASS.                                       RL1134.2
080700     PERFORM  PRINT-DETAIL.                                       RL1134.2
080800     ADD      01 TO REC-CT.                                       RL1134.2
080900*    .07                                                          RL1134.2
081000 CCVS-EXIT SECTION.                                               RL1134.2
081100 CCVS-999999.                                                     RL1134.2
081200     GO TO CLOSE-FILES.                                           RL1134.2
      *END-OF,RL113A                                                                  
