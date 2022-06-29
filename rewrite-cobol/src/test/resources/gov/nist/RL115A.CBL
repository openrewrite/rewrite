      *HEADER,COBOL,RL115A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1154.2
000200 PROGRAM-ID.                                                      RL1154.2
000300     RL115A.                                                      RL1154.2
000400****************************************************************  RL1154.2
000500*                                                              *  RL1154.2
000600*    VALIDATION FOR:-                                          *  RL1154.2
000700*                                                              *  RL1154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1154.2
000900*                                                              *  RL1154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1154.2
001100*                                                              *  RL1154.2
001200****************************************************************  RL1154.2
001300*                                                              *  RL1154.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1154.2
001500*                                                              *  RL1154.2
001600*            X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1154.2
001700*                    RELATIVE  I-O DATA FILE                   *  RL1154.2
001800*            X-55   SYSTEM PRINTER                             *  RL1154.2
001900*            X-69   ADDITIONAL VALUE OF CLAUSES                *  RL1154.2
002000*            X-74   VALUE OF IMPLEMENTOR-NAME                  *  RL1154.2
002100*            X-75   OBJECT OF VALUE OF CLAUSE                  *  RL1154.2
002200*            X-82   SOURCE-COMPUTER                            *  RL1154.2
002300*            X-83   OBJECT-COMPUTER.                           *  RL1154.2
002400*                                                              *  RL1154.2
002500****************************************************************  RL1154.2
002600*    RL115A                                                    *  RL1154.2
002700****************************************************************  RL1154.2
002800*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND    RL1154.2
002900*            SEMANTIC ACTIONS ASSOCIATED WITH THE "USE"           RL1154.2
003000*            STATEMENT.                                           RL1154.2
003100*                                                                 RL1154.2
003200*                                                                 RL1154.2
003300*            THIS PROGRAM CREATES A RELATIVE I-O FILE SEQUENTIALLYRL1154.2
003400*            (ACCESS MODE SEQUENTIAL) AND THEN UPDATES SELECTIVE  RL1154.2
003500*            RECORDS OF THE FILE.  THE FILE STATUS CONTENTS ARE   RL1154.2
003600*            CAPTURED AND TESTED FOR ACCURACY FOR EACH OPEN, CLOSERL1154.2
003700*            READ AND REWRITE STATEMENT USED.  THE READ, WRITE ANDRL1154.2
003800*            REWRITE STATEMENTS ARE USED WITHOUT THE APPROPRIATE  RL1154.2
003900*            AT END ON INVALID KEY PHRASES.  THE OMISSION OF THESERL1154.2
004000*            PHRASES ARE PERMITTED IF AN APPLICABLE USE PROCEDURE RL1154.2
004100*            HAS BEEN SPECIFIED.                                  RL1154.2
004200*                                                                 RL1154.2
004300***************************************************               RL1154.2
004400 ENVIRONMENT DIVISION.                                            RL1154.2
004500 CONFIGURATION SECTION.                                           RL1154.2
004600 SOURCE-COMPUTER.                                                 RL1154.2
004700     XXXXX082.                                                    RL1154.2
004800 OBJECT-COMPUTER.                                                 RL1154.2
004900     XXXXX083.                                                    RL1154.2
005000 INPUT-OUTPUT SECTION.                                            RL1154.2
005100 FILE-CONTROL.                                                    RL1154.2
005200     SELECT PRINT-FILE ASSIGN TO                                  RL1154.2
005300     XXXXX055.                                                    RL1154.2
005400     SELECT   RL-FD2 ASSIGN                                       RL1154.2
005500     XXXXX022                                                     RL1154.2
005600              ORGANIZATION RELATIVE                               RL1154.2
005700              ACCESS  SEQUENTIAL                                  RL1154.2
005800              RELATIVE  RL-FD2-KEY                                RL1154.2
005900         FILE STATUS IS RL-FD2-STATUS.                            RL1154.2
006000 DATA DIVISION.                                                   RL1154.2
006100 FILE SECTION.                                                    RL1154.2
006200 FD  PRINT-FILE.                                                  RL1154.2
006300 01  PRINT-REC PICTURE X(120).                                    RL1154.2
006400 01  DUMMY-RECORD PICTURE X(120).                                 RL1154.2
006500 FD  RL-FD2                                                       RL1154.2
006600C    VALUE OF                                                     RL1154.2
006700C    XXXXX074                                                     RL1154.2
006800C    IS                                                           RL1154.2
006900C    XXXXX076                                                     RL1154.2
007000*    XXXXX069                                                     RL1154.2
007100     LABEL RECORDS ARE STANDARD                                   RL1154.2
007200     BLOCK CONTAINS 1 RECORDS                                     RL1154.2
007300     DATA RECORD RL-FD2R1-F-G-240.                                RL1154.2
007400 01  RL-FD2R1-F-G-240.                                            RL1154.2
007500     05 RL-FD2-WRK-120 PIC X(120).                                RL1154.2
007600     05 RL-FD2-GRP-120.                                           RL1154.2
007700        10 RL-FD2-WRK-XN-0001-O120F                               RL1154.2
007800                        PICTURE X OCCURS 120 TIMES.               RL1154.2
007900 WORKING-STORAGE SECTION.                                         RL1154.2
008000 01  GRP-0001.                                                    RL1154.2
008100     05 RL-FD2-KEY   PIC 9(8)  VALUE ZERO.                        RL1154.2
008200     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008300     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008400     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008500     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008600     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008700     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008800     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1154.2
008900     05 RL-FD2-STATUS     PIC  XX  VALUE  SPACE.                  RL1154.2
009000     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1154.2
009100     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1154.2
009200     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1154.2
009300     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1154.2
009400     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1154.2
009500     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1154.2
009600     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1154.2
009700     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1154.2
009800     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1154.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 RL1154.2
010000     03 FILE-RECORD-INFO-SKELETON.                                RL1154.2
010100        05 FILLER                 PICTURE X(48)       VALUE       RL1154.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1154.2
010300        05 FILLER                 PICTURE X(46)       VALUE       RL1154.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1154.2
010500        05 FILLER                 PICTURE X(26)       VALUE       RL1154.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1154.2
010700        05 FILLER                 PICTURE X(37)       VALUE       RL1154.2
010800             ",RECKEY=                             ".             RL1154.2
010900        05 FILLER                 PICTURE X(38)       VALUE       RL1154.2
011000             ",ALTKEY1=                             ".            RL1154.2
011100        05 FILLER                 PICTURE X(38)       VALUE       RL1154.2
011200             ",ALTKEY2=                             ".            RL1154.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1154.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1154.2
011500        05 FILE-RECORD-INFO-P1-120.                               RL1154.2
011600           07 FILLER              PIC X(5).                       RL1154.2
011700           07 XFILE-NAME           PIC X(6).                      RL1154.2
011800           07 FILLER              PIC X(8).                       RL1154.2
011900           07 XRECORD-NAME         PIC X(6).                      RL1154.2
012000           07 FILLER              PIC X(1).                       RL1154.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       RL1154.2
012200           07 FILLER              PIC X(7).                       RL1154.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      RL1154.2
012400           07 FILLER              PIC X(6).                       RL1154.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       RL1154.2
012600           07 FILLER              PIC X(5).                       RL1154.2
012700           07 ODO-NUMBER          PIC 9(4).                       RL1154.2
012800           07 FILLER              PIC X(5).                       RL1154.2
012900           07 XPROGRAM-NAME        PIC X(5).                      RL1154.2
013000           07 FILLER              PIC X(7).                       RL1154.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      RL1154.2
013200           07 FILLER              PIC X(7).                       RL1154.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       RL1154.2
013400           07 FILLER              PIC X(1).                       RL1154.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      RL1154.2
013600           07 FILLER              PIC X(6).                       RL1154.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       RL1154.2
013800           07 FILLER              PIC X(5).                       RL1154.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      RL1154.2
014000           07 FILLER              PIC X(6).                       RL1154.2
014100           07 XLABEL-TYPE          PIC X(1).                      RL1154.2
014200        05 FILE-RECORD-INFO-P121-240.                             RL1154.2
014300           07 FILLER              PIC X(8).                       RL1154.2
014400           07 XRECORD-KEY          PIC X(29).                     RL1154.2
014500           07 FILLER              PIC X(9).                       RL1154.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      RL1154.2
014700           07 FILLER              PIC X(9).                       RL1154.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      RL1154.2
014900           07 FILLER              PIC X(7).                       RL1154.2
015000 01  TEST-RESULTS.                                                RL1154.2
015100     02 FILLER                   PIC X      VALUE SPACE.          RL1154.2
015200     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1154.2
015300     02 FILLER                   PIC X      VALUE SPACE.          RL1154.2
015400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1154.2
015500     02 FILLER                   PIC X      VALUE SPACE.          RL1154.2
015600     02  PAR-NAME.                                                RL1154.2
015700       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1154.2
015800       03  PARDOT-X              PIC X      VALUE SPACE.          RL1154.2
015900       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1154.2
016000     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1154.2
016100     02 RE-MARK                  PIC X(61).                       RL1154.2
016200 01  TEST-COMPUTED.                                               RL1154.2
016300     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1154.2
016400     02 FILLER                   PIC X(17)  VALUE                 RL1154.2
016500            "       COMPUTED=".                                   RL1154.2
016600     02 COMPUTED-X.                                               RL1154.2
016700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1154.2
016800     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1154.2
016900                                 PIC -9(9).9(9).                  RL1154.2
017000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1154.2
017100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1154.2
017200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1154.2
017300     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1154.2
017400         04 COMPUTED-18V0                    PIC -9(18).          RL1154.2
017500         04 FILLER                           PIC X.               RL1154.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             RL1154.2
017700 01  TEST-CORRECT.                                                RL1154.2
017800     02 FILLER PIC X(30) VALUE SPACE.                             RL1154.2
017900     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1154.2
018000     02 CORRECT-X.                                                RL1154.2
018100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1154.2
018200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1154.2
018300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1154.2
018400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1154.2
018500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1154.2
018600     03      CR-18V0 REDEFINES CORRECT-A.                         RL1154.2
018700         04 CORRECT-18V0                     PIC -9(18).          RL1154.2
018800         04 FILLER                           PIC X.               RL1154.2
018900     03 FILLER PIC X(2) VALUE SPACE.                              RL1154.2
019000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1154.2
019100 01  CCVS-C-1.                                                    RL1154.2
019200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1154.2
019300-    "SS  PARAGRAPH-NAME                                          RL1154.2
019400-    "       REMARKS".                                            RL1154.2
019500     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1154.2
019600 01  CCVS-C-2.                                                    RL1154.2
019700     02 FILLER                     PIC X        VALUE SPACE.      RL1154.2
019800     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1154.2
019900     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1154.2
020000     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1154.2
020100     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1154.2
020200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1154.2
020300 01  REC-CT                        PIC 99       VALUE ZERO.       RL1154.2
020400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1154.2
020500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1154.2
020600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1154.2
020700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1154.2
020800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1154.2
020900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1154.2
021000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1154.2
021100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1154.2
021200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1154.2
021300 01  CCVS-H-1.                                                    RL1154.2
021400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1154.2
021500     02  FILLER                    PIC X(42)    VALUE             RL1154.2
021600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1154.2
021700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1154.2
021800 01  CCVS-H-2A.                                                   RL1154.2
021900   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1154.2
022000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1154.2
022100   02  FILLER                        PIC XXXX   VALUE             RL1154.2
022200     "4.2 ".                                                      RL1154.2
022300   02  FILLER                        PIC X(28)  VALUE             RL1154.2
022400            " COPY - NOT FOR DISTRIBUTION".                       RL1154.2
022500   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1154.2
022600                                                                  RL1154.2
022700 01  CCVS-H-2B.                                                   RL1154.2
022800   02  FILLER                        PIC X(15)  VALUE             RL1154.2
022900            "TEST RESULT OF ".                                    RL1154.2
023000   02  TEST-ID                       PIC X(9).                    RL1154.2
023100   02  FILLER                        PIC X(4)   VALUE             RL1154.2
023200            " IN ".                                               RL1154.2
023300   02  FILLER                        PIC X(12)  VALUE             RL1154.2
023400     " HIGH       ".                                              RL1154.2
023500   02  FILLER                        PIC X(22)  VALUE             RL1154.2
023600            " LEVEL VALIDATION FOR ".                             RL1154.2
023700   02  FILLER                        PIC X(58)  VALUE             RL1154.2
023800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1154.2
023900 01  CCVS-H-3.                                                    RL1154.2
024000     02  FILLER                      PIC X(34)  VALUE             RL1154.2
024100            " FOR OFFICIAL USE ONLY    ".                         RL1154.2
024200     02  FILLER                      PIC X(58)  VALUE             RL1154.2
024300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1154.2
024400     02  FILLER                      PIC X(28)  VALUE             RL1154.2
024500            "  COPYRIGHT   1985 ".                                RL1154.2
024600 01  CCVS-E-1.                                                    RL1154.2
024700     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1154.2
024800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1154.2
024900     02 ID-AGAIN                     PIC X(9).                    RL1154.2
025000     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1154.2
025100 01  CCVS-E-2.                                                    RL1154.2
025200     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1154.2
025300     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1154.2
025400     02 CCVS-E-2-2.                                               RL1154.2
025500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1154.2
025600         03 FILLER                   PIC X      VALUE SPACE.      RL1154.2
025700         03 ENDER-DESC               PIC X(44)  VALUE             RL1154.2
025800            "ERRORS ENCOUNTERED".                                 RL1154.2
025900 01  CCVS-E-3.                                                    RL1154.2
026000     02  FILLER                      PIC X(22)  VALUE             RL1154.2
026100            " FOR OFFICIAL USE ONLY".                             RL1154.2
026200     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1154.2
026300     02  FILLER                      PIC X(58)  VALUE             RL1154.2
026400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1154.2
026500     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1154.2
026600     02 FILLER                       PIC X(15)  VALUE             RL1154.2
026700             " COPYRIGHT 1985".                                   RL1154.2
026800 01  CCVS-E-4.                                                    RL1154.2
026900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1154.2
027000     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1154.2
027100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1154.2
027200     02 FILLER                       PIC X(40)  VALUE             RL1154.2
027300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1154.2
027400 01  XXINFO.                                                      RL1154.2
027500     02 FILLER                       PIC X(19)  VALUE             RL1154.2
027600            "*** INFORMATION ***".                                RL1154.2
027700     02 INFO-TEXT.                                                RL1154.2
027800       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1154.2
027900       04 XXCOMPUTED                 PIC X(20).                   RL1154.2
028000       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1154.2
028100       04 XXCORRECT                  PIC X(20).                   RL1154.2
028200     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1154.2
028300 01  HYPHEN-LINE.                                                 RL1154.2
028400     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1154.2
028500     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1154.2
028600-    "*****************************************".                 RL1154.2
028700     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1154.2
028800-    "******************************".                            RL1154.2
028900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1154.2
029000     "RL115A".                                                    RL1154.2
029100 PROCEDURE DIVISION.                                              RL1154.2
029200 DECLARATIVES.                                                    RL1154.2
029300 RL-FD2-01 SECTION.                                               RL1154.2
029400     USE     AFTER ERROR PROCEDURE I-O.                           RL1154.2
029500 RL-FD2-01-01.                                                    RL1154.2
029600     MOVE   "PASS " TO P-OR-F.                                    RL1154.2
029700     ADD     1 TO PASS-COUNTER.                                   RL1154.2
029800*                                                                 RL1154.2
029900     IF      REC-CT NOT EQUAL TO ZERO                             RL1154.2
030000             MOVE "." TO PARDOT-X                                 RL1154.2
030100             MOVE REC-CT TO DOTVALUE.                             RL1154.2
030200     MOVE    TEST-RESULTS TO PRINT-REC.                           RL1154.2
030300     PERFORM D1-WRITE-LINE THRU D1-WRITE-LINE-EXIT.               RL1154.2
030400     IF      P-OR-F EQUAL TO "FAIL*"                              RL1154.2
030500             PERFORM D1-WRITE-LINE THRU D1-WRITE-LINE-EXIT        RL1154.2
030600             PERFORM D1-FAIL-ROUTINE THRU D1-FAIL-ROUTINE-EX      RL1154.2
030700      ELSE                                                        RL1154.2
030800             PERFORM D1-BAIL-OUT THRU D1-BAIL-OUT-EX.             RL1154.2
030900     MOVE    SPACE TO P-OR-F.                                     RL1154.2
031000     MOVE    SPACE TO COMPUTED-X.                                 RL1154.2
031100     MOVE    SPACE TO CORRECT-X.                                  RL1154.2
031200     IF      REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.        RL1154.2
031300     MOVE    SPACE TO RE-MARK.                                    RL1154.2
031400     ADD     1 TO WRK-CS-09V00-013.                               RL1154.2
031500     MOVE    RL-FD2-STATUS TO WRK-XN-0002-002.                    RL1154.2
031600     MOVE    "10" TO WRK-XN-0002-003.                             RL1154.2
031700     GO TO   RL-FD2-01-EXIT.                                      RL1154.2
031800 D1-FAIL-ROUTINE.                                                 RL1154.2
031900     IF      COMPUTED-X NOT EQUAL TO SPACE                        RL1154.2
032000             GO TO   D1-FAIL-ROUTINE-WRITE.                       RL1154.2
032100     IF      CORRECT-X NOT EQUAL TO SPACE                         RL1154.2
032200             GO TO   D1-FAIL-ROUTINE-WRITE.                       RL1154.2
032300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1154.2
032400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  RL1154.2
032500     MOVE    XXINFO TO DUMMY-RECORD.                              RL1154.2
032600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
032700     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
032800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
032900     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
033000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1154.2
033100     GO TO   D1-FAIL-ROUTINE-EX.                                  RL1154.2
033200 D1-FAIL-ROUTINE-WRITE.                                           RL1154.2
033300     MOVE    TEST-COMPUTED TO PRINT-REC.                          RL1154.2
033400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
033500     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
033600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                RL1154.2
033700     MOVE    TEST-CORRECT TO PRINT-REC.                           RL1154.2
033800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
033900     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
034000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
034100     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
034200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        RL1154.2
034300 D1-FAIL-ROUTINE-EX.                                              RL1154.2
034400     EXIT.                                                        RL1154.2
034500 D1-BAIL-OUT.                                                     RL1154.2
034600     IF      COMPUTED-A NOT EQUAL TO SPACE                        RL1154.2
034700             GO TO    D1-BAIL-OUT-WRITE.                          RL1154.2
034800     IF      CORRECT-A EQUAL TO SPACE                             RL1154.2
034900             GO TO    D1-BAIL-OUT-EX.                             RL1154.2
035000 D1-BAIL-OUT-WRITE.                                               RL1154.2
035100     MOVE    CORRECT-A  TO XXCORRECT.                             RL1154.2
035200     MOVE    COMPUTED-A TO XXCOMPUTED.                            RL1154.2
035300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                RL1154.2
035400     MOVE    XXINFO TO DUMMY-RECORD.                              RL1154.2
035500     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
035600     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
035700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
035800     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
035900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        RL1154.2
036000 D1-BAIL-OUT-EX.                                                  RL1154.2
036100     EXIT.                                                        RL1154.2
036200 D1-WRITE-LINE.                                                   RL1154.2
036300     ADD     1 TO RECORD-COUNT.                                   RL1154.2
036400*    IF      RECORD-COUNT GREATER 50                              RL1154.2
036500*            MOVE    DUMMY-RECORD TO DUMMY-HOLD                   RL1154.2
036600*            MOVE    SPACE TO DUMMY-RECORD                        RL1154.2
036700*            WRITE   DUMMY-RECORD AFTER ADVANCING PAGE            RL1154.2
036800*            MOVE    CCVS-C-1 TO DUMMY-RECORD                     RL1154.2
036900*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1154.2
037000*            MOVE    SPACE TO DUMMY-RECORD                        RL1154.2
037100*            MOVE    CCVS-C-2 TO DUMMY-RECORD                     RL1154.2
037200*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1154.2
037300*            MOVE    SPACE TO DUMMY-RECORD                        RL1154.2
037400*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1154.2
037500*            MOVE    SPACE TO DUMMY-RECORD                        RL1154.2
037600*            MOVE    HYPHEN-LINE TO DUMMY-RECORD                  RL1154.2
037700*            WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES         RL1154.2
037800*            MOVE    SPACE TO DUMMY-RECORD                        RL1154.2
037900*            MOVE    DUMMY-HOLD TO DUMMY-RECORD                   RL1154.2
038000*            MOVE    ZERO TO RECORD-COUNT.                        RL1154.2
038100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                RL1154.2
038200     MOVE    SPACE TO DUMMY-RECORD.                               RL1154.2
038300 D1-WRITE-LINE-EXIT.                                              RL1154.2
038400     EXIT.                                                        RL1154.2
038500 RL-FD2-01-EXIT.                                                  RL1154.2
038600     EXIT.                                                        RL1154.2
038700 END DECLARATIVES.                                                RL1154.2
038800 CCVS1 SECTION.                                                   RL1154.2
038900 OPEN-FILES.                                                      RL1154.2
039000     OPEN    OUTPUT PRINT-FILE.                                   RL1154.2
039100     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1154.2
039200     MOVE    SPACE TO TEST-RESULTS.                               RL1154.2
039300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1154.2
039400     MOVE    ZERO TO REC-SKL-SUB.                                 RL1154.2
039500     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1154.2
039600 CCVS-INIT-FILE.                                                  RL1154.2
039700     ADD     1 TO REC-SKL-SUB.                                    RL1154.2
039800     MOVE    FILE-RECORD-INFO-SKELETON                            RL1154.2
039900          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1154.2
040000 CCVS-INIT-EXIT.                                                  RL1154.2
040100     GO TO CCVS1-EXIT.                                            RL1154.2
040200 CLOSE-FILES.                                                     RL1154.2
040300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1154.2
040400 TERMINATE-CCVS.                                                  RL1154.2
040500*    EXIT PROGRAM.                                                RL1154.2
040600*TERMINATE-CALL.                                                  RL1154.2
040700     STOP     RUN.                                                RL1154.2
040800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1154.2
040900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1154.2
041000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1154.2
041100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1154.2
041200     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1154.2
041300 PRINT-DETAIL.                                                    RL1154.2
041400     IF REC-CT NOT EQUAL TO ZERO                                  RL1154.2
041500             MOVE "." TO PARDOT-X                                 RL1154.2
041600             MOVE REC-CT TO DOTVALUE.                             RL1154.2
041700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1154.2
041800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1154.2
041900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1154.2
042000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1154.2
042100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1154.2
042200     MOVE SPACE TO CORRECT-X.                                     RL1154.2
042300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1154.2
042400     MOVE     SPACE TO RE-MARK.                                   RL1154.2
042500 HEAD-ROUTINE.                                                    RL1154.2
042600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1154.2
042700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1154.2
042800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1154.2
042900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1154.2
043000 COLUMN-NAMES-ROUTINE.                                            RL1154.2
043100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1154.2
043200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1154.2
043300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1154.2
043400 END-ROUTINE.                                                     RL1154.2
043500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1154.2
043600 END-RTN-EXIT.                                                    RL1154.2
043700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1154.2
043800 END-ROUTINE-1.                                                   RL1154.2
043900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1154.2
044000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1154.2
044100      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1154.2
044200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1154.2
044300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1154.2
044400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1154.2
044500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1154.2
044600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1154.2
044700  END-ROUTINE-12.                                                 RL1154.2
044800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1154.2
044900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1154.2
045000         MOVE "NO " TO ERROR-TOTAL                                RL1154.2
045100         ELSE                                                     RL1154.2
045200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1154.2
045300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1154.2
045400     PERFORM WRITE-LINE.                                          RL1154.2
045500 END-ROUTINE-13.                                                  RL1154.2
045600     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1154.2
045700         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1154.2
045800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1154.2
045900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1154.2
046000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1154.2
046100      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1154.2
046200          MOVE "NO " TO ERROR-TOTAL                               RL1154.2
046300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1154.2
046400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1154.2
046500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1154.2
046600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1154.2
046700 WRITE-LINE.                                                      RL1154.2
046800     ADD 1 TO RECORD-COUNT.                                       RL1154.2
046900*    IF RECORD-COUNT GREATER 50                                   RL1154.2
047000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1154.2
047100*        MOVE SPACE TO DUMMY-RECORD                               RL1154.2
047200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1154.2
047300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1154.2
047400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1154.2
047500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1154.2
047600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1154.2
047700*        MOVE ZERO TO RECORD-COUNT.                               RL1154.2
047800     PERFORM WRT-LN.                                              RL1154.2
047900 WRT-LN.                                                          RL1154.2
048000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1154.2
048100     MOVE SPACE TO DUMMY-RECORD.                                  RL1154.2
048200 BLANK-LINE-PRINT.                                                RL1154.2
048300     PERFORM WRT-LN.                                              RL1154.2
048400 FAIL-ROUTINE.                                                    RL1154.2
048500     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1154.2
048600            GO TO   FAIL-ROUTINE-WRITE.                           RL1154.2
048700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1154.2
048800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1154.2
048900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1154.2
049000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1154.2
049100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1154.2
049200     GO TO  FAIL-ROUTINE-EX.                                      RL1154.2
049300 FAIL-ROUTINE-WRITE.                                              RL1154.2
049400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1154.2
049500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1154.2
049600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1154.2
049700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1154.2
049800 FAIL-ROUTINE-EX. EXIT.                                           RL1154.2
049900 BAIL-OUT.                                                        RL1154.2
050000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1154.2
050100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1154.2
050200 BAIL-OUT-WRITE.                                                  RL1154.2
050300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1154.2
050400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1154.2
050500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1154.2
050600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1154.2
050700 BAIL-OUT-EX. EXIT.                                               RL1154.2
050800 CCVS1-EXIT.                                                      RL1154.2
050900     EXIT.                                                        RL1154.2
051000 SECT-RL115A-001 SECTION.                                         RL1154.2
051100 REL-INIT-009.                                                    RL1154.2
051200     MOVE    "VIII-36 4.8.4 GR4" TO ANSI-REFERENCE.               RL1154.2
051300     MOVE    "REL-TEST-009" TO PAR-NAME.                          RL1154.2
051400     MOVE     "CREATE RL-FD2" TO  FEATURE                         RL1154.2
051500     MOVE     "RL-FD2"        TO  XFILE-NAME (2).                 RL1154.2
051600     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               RL1154.2
051700     MOVE CCVS-PGM-ID             TO  XPROGRAM-NAME (2).          RL1154.2
051800     MOVE     000240          TO  XRECORD-LENGTH (2).             RL1154.2
051900     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           RL1154.2
052000     MOVE     0001            TO  XBLOCK-SIZE       (2).          RL1154.2
052100     MOVE     000500          TO  RECORDS-IN-FILE  (2).           RL1154.2
052200     MOVE     "RL"            TO  XFILE-ORGANIZATION (2).         RL1154.2
052300     MOVE     "S"             TO  XLABEL-TYPE        (2).         RL1154.2
052400     MOVE     000001          TO  XRECORD-NUMBER     (2).         RL1154.2
052500*INITIALIZE  RECORD WORK AREA NUMBER 2.                           RL1154.2
052600     MOVE     1         TO  WRK-CS-09V00-012.                     RL1154.2
052700     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     RL1154.2
052800                            WRK-CS-09V00-015 WRK-CS-09V00-016     RL1154.2
052900                            WRK-CS-09V00-017 WRK-CS-09V00-018.    RL1154.2
053000     MOVE     SPACE     TO  RL-FD2-STATUS.                        RL1154.2
053100     MOVE     90000002  TO  RL-FD2-KEY.                           RL1154.2
053200     MOVE     01 TO REC-CT.                                       RL1154.2
053300     OPEN     OUTPUT    RL-FD2.                                   RL1154.2
053400     MOVE     RL-FD2-STATUS TO WRK-XN-0002-001.                   RL1154.2
053500*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            RL1154.2
053600 REL-TEST-009-R.                                                  RL1154.2
053700     MOVE XRECORD-NUMBER (2) TO RL-FD2-KEY.                       RL1154.2
053800     MOVE    "99"  TO RL-FD2-STATUS.                              RL1154.2
053900     MOVE     FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120.      RL1154.2
054000     MOVE     FILE-RECORD-INFO-P1-120 (2) TO                      RL1154.2
054100               RL-FD2-GRP-120.                                    RL1154.2
054200     WRITE    RL-FD2R1-F-G-240 INVALID KEY                        RL1154.2
054300              GO TO REL-TEST-009-2.                               RL1154.2
054400     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1154.2
054500              GO TO  REL-TEST-009-2.                              RL1154.2
054600     IF       XRECORD-NUMBER (2) EQUAL TO 500                     RL1154.2
054700             GO TO REL-TEST-009-2.                                RL1154.2
054800     ADD      01 TO  XRECORD-NUMBER (2).                          RL1154.2
054900     GO TO    REL-TEST-009-R.                                     RL1154.2
055000 REL-TEST-009-2.                                                  RL1154.2
055100     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  RL1154.2
055200              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 RL1154.2
055300              MOVE ZERO TO CORRECT-18V0                           RL1154.2
055400              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1154.2
055500              PERFORM   FAIL                                      RL1154.2
055600              ELSE                                                RL1154.2
055700              PERFORM   PASS.                                     RL1154.2
055800     PERFORM  PRINT-DETAIL.                                       RL1154.2
055900     ADD      01 TO REC-CT.                                       RL1154.2
056000*    .01                                                          RL1154.2
056100     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 RL1154.2
056200              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1154.2
056300              MOVE  500 TO CORRECT-18V0                           RL1154.2
056400             MOVE    XRECORD-NUMBER (2)  TO COMPUTED-18V0         RL1154.2
056500              PERFORM  FAIL                                       RL1154.2
056600              ELSE                                                RL1154.2
056700              PERFORM PASS.                                       RL1154.2
056800     PERFORM  PRINT-DETAIL.                                       RL1154.2
056900     ADD      01 TO REC-CT.                                       RL1154.2
057000*    .02                                                          RL1154.2
057100     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  RL1154.2
057200              MOVE "STATUS/OPEN" TO RE-MARK                       RL1154.2
057300              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1154.2
057400              MOVE "00" TO CORRECT-A                              RL1154.2
057500              PERFORM  FAIL                                       RL1154.2
057600              ELSE                                                RL1154.2
057700              PERFORM  PASS.                                      RL1154.2
057800     PERFORM  PRINT-DETAIL.                                       RL1154.2
057900     ADD      01 TO REC-CT.                                       RL1154.2
058000*    .03                                                          RL1154.2
058100     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1154.2
058200              MOVE "STATUS/WRITE" TO RE-MARK                      RL1154.2
058300              MOVE RL-FD2-STATUS TO COMPUTED-A                    RL1154.2
058400              MOVE "00" TO CORRECT-A                              RL1154.2
058500              PERFORM   FAIL                                      RL1154.2
058600              ELSE                                                RL1154.2
058700              PERFORM   PASS.                                     RL1154.2
058800     PERFORM  PRINT-DETAIL.                                       RL1154.2
058900     ADD      01 TO REC-CT.                                       RL1154.2
059000*    .04                                                          RL1154.2
059100     MOVE     SPACE  TO  RL-FD2-STATUS.                           RL1154.2
059200     CLOSE    RL-FD2.                                             RL1154.2
059300     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1154.2
059400              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1154.2
059500              MOVE   RL-FD2-STATUS TO COMPUTED-A                  RL1154.2
059600              MOVE "00" TO CORRECT-A                              RL1154.2
059700              PERFORM FAIL                                        RL1154.2
059800              ELSE                                                RL1154.2
059900              PERFORM PASS.                                       RL1154.2
060000     PERFORM  PRINT-DETAIL.                                       RL1154.2
060100     ADD      01 TO REC-CT.                                       RL1154.2
060200*    .05                                                          RL1154.2
060300 REL-INIT-010.                                                    RL1154.2
060400     MOVE    "REL-TEST-010" TO PAR-NAME.                          RL1154.2
060500     MOVE     2     TO  WRK-CS-09V00-012.                         RL1154.2
060600     MOVE     ZERO  TO  RL-FD2-KEY.                               RL1154.2
060700     MOVE     ZERO  TO  WRK-CS-09V00-013.                         RL1154.2
060800     MOVE     ZERO  TO  WRK-CS-09V00-014.                         RL1154.2
060900     MOVE     ZERO  TO  WRK-CS-09V00-015.                         RL1154.2
061000     MOVE     ZERO  TO  WRK-CS-09V00-016.                         RL1154.2
061100     MOVE     ZERO  TO  WRK-CS-09V00-017.                         RL1154.2
061200     MOVE     ZERO  TO  WRK-CS-09V00-018.                         RL1154.2
061300     MOVE     01 TO REC-CT.                                       RL1154.2
061400     OPEN     I-O  RL-FD2.                                        RL1154.2
061500     MOVE     SPACE     TO  WRK-XN-0002-002                       RL1154.2
061600     MOVE     SPACE     TO  WRK-XN-0002-003                       RL1154.2
061700     MOVE     SPACE     TO  WRK-XN-0002-004                       RL1154.2
061800     MOVE      RL-FD2-STATUS TO WRK-XN-0002-001                   RL1154.2
061900     MOVE     SPACE TO RL-FD2-STATUS.                             RL1154.2
062000*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       RL1154.2
062100     MOVE    "USE/FILE STATUS"  TO FEATURE.                       RL1154.2
062200 REL-TEST-010-R.                                                  RL1154.2
062300     ADD      1 TO RL-FD2-KEY.                                    RL1154.2
062400     ADD      1 TO WRK-CS-09V00-014.                              RL1154.2
062500     ADD      1 TO WRK-CS-09V00-015.                              RL1154.2
062600     READ     RL-FD2.                                             RL1154.2
062700     IF       RL-FD2-STATUS EQUAL TO "10"                         RL1154.2
062800              GO TO  REL-TEST-010-3.                              RL1154.2
062900     MOVE     RL-FD2-WRK-120 TO FILE-RECORD-INFO-P1-120 (2).      RL1154.2
063000     IF       WRK-CS-09V00-015 EQUAL TO 5                         RL1154.2
063100              ADD 01 TO UPDATE-NUMBER (2)                         RL1154.2
063200              MOVE FILE-RECORD-INFO-P1-120 (2) TO RL-FD2-WRK-120  RL1154.2
063300              REWRITE RL-FD2R1-F-G-240                            RL1154.2
063400           MOVE ZERO TO WRK-CS-09V00-015                          RL1154.2
063500              GO TO  REL-TEST-010-2.                              RL1154.2
063600     IF       WRK-CS-09V00-014 GREATER 500                        RL1154.2
063700              GO TO  REL-TEST-010-3.                              RL1154.2
063800     GO TO    REL-TEST-010-R.                                     RL1154.2
063900 REL-TEST-010-2.                                                  RL1154.2
064000     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1154.2
064100              ADD 1 TO WRK-CS-09V00-016.                          RL1154.2
064200     MOVE    SPACE TO RL-FD2-STATUS.                              RL1154.2
064300     GO TO    REL-TEST-010-R.                                     RL1154.2
064400 REL-TEST-010-3.                                                  RL1154.2
064500     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     RL1154.2
064600              MOVE  "EXCEPTIONS/ERRORS" TO RE-MARK                RL1154.2
064700              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              RL1154.2
064800              MOVE  1 TO CORRECT-18V0                             RL1154.2
064900              PERFORM FAIL                                        RL1154.2
065000              ELSE                                                RL1154.2
065100              PERFORM PASS.                                       RL1154.2
065200     PERFORM  PRINT-DETAIL.                                       RL1154.2
065300     ADD      01 TO REC-CT.                                       RL1154.2
065400*    .01                                                          RL1154.2
065500     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   RL1154.2
065600              MOVE "INCORRECT COUNT" TO RE-MARK                   RL1154.2
065700              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              RL1154.2
065800              MOVE 501 TO CORRECT-18V0                            RL1154.2
065900              PERFORM FAIL                                        RL1154.2
066000              ELSE                                                RL1154.2
066100             PERFORM PASS.                                        RL1154.2
066200     PERFORM  PRINT-DETAIL.                                       RL1154.2
066300     ADD      01 TO REC-CT.                                       RL1154.2
066400*    .02                                                          RL1154.2
066500     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   RL1154.2
066600              MOVE "OPEN/STATUS"  TO  RE-MARK                     RL1154.2
066700              MOVE WRK-XN-0002-001 TO COMPUTED-A                  RL1154.2
066800              MOVE  "00" TO CORRECT-A                             RL1154.2
066900              PERFORM FAIL                                        RL1154.2
067000              ELSE                                                RL1154.2
067100              PERFORM PASS.                                       RL1154.2
067200     PERFORM  PRINT-DETAIL.                                       RL1154.2
067300     ADD      01 TO REC-CT.                                       RL1154.2
067400*    .03                                                          RL1154.2
067500     IF        RL-FD2-STATUS NOT EQUAL  TO "10"                   RL1154.2
067600              MOVE "ATEND/STATUS" TO RE-MARK                      RL1154.2
067700              MOVE  RL-FD2-STATUS TO COMPUTED-A                   RL1154.2
067800              MOVE "10" TO CORRECT-A                              RL1154.2
067900              PERFORM FAIL                                        RL1154.2
068000              ELSE                                                RL1154.2
068100              PERFORM PASS.                                       RL1154.2
068200     PERFORM  PRINT-DETAIL.                                       RL1154.2
068300     ADD      01 TO REC-CT.                                       RL1154.2
068400*    .04                                                          RL1154.2
068500     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   RL1154.2
068600              MOVE  "EXCEPTIN/STATUS" TO RE-MARK                  RL1154.2
068700              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 RL1154.2
068800              MOVE  "10" TO CORRECT-A                             RL1154.2
068900              PERFORM FAIL                                        RL1154.2
069000              ELSE                                                RL1154.2
069100              PERFORM PASS.                                       RL1154.2
069200     PERFORM  PRINT-DETAIL.                                       RL1154.2
069300     ADD      01 TO REC-CT.                                       RL1154.2
069400*    .05                                                          RL1154.2
069500     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   RL1154.2
069600              MOVE "NO/EXCEPTION" TO  RE-MARK                     RL1154.2
069700              MOVE WRK-XN-0002-003 TO COMPUTED-A                  RL1154.2
069800              MOVE "10" TO CORRECT-A                              RL1154.2
069900              PERFORM FAIL                                        RL1154.2
070000              ELSE                                                RL1154.2
070100              PERFORM PASS.                                       RL1154.2
070200     PERFORM  PRINT-DETAIL                                        RL1154.2
070300     ADD      01 TO REC-CT.                                       RL1154.2
070400*    .06                                                          RL1154.2
070500     MOVE     SPACE TO RL-FD2-STATUS.                             RL1154.2
070600     CLOSE    RL-FD2                                              RL1154.2
070700     IF        RL-FD2-STATUS NOT EQUAL TO "00"                    RL1154.2
070800              MOVE "CLOSE/STATUS" TO RE-MARK                      RL1154.2
070900              MOVE  RL-FD2-STATUS TO  COMPUTED-A                  RL1154.2
071000              MOVE "00" TO CORRECT-A                              RL1154.2
071100              PERFORM FAIL                                        RL1154.2
071200              ELSE                                                RL1154.2
071300              PERFORM PASS.                                       RL1154.2
071400     PERFORM  PRINT-DETAIL.                                       RL1154.2
071500     ADD      01 TO REC-CT.                                       RL1154.2
071600*    .07                                                          RL1154.2
071700 CCVS-EXIT SECTION.                                               RL1154.2
071800 CCVS-999999.                                                     RL1154.2
071900     GO TO CLOSE-FILES.                                           RL1154.2
      *END-OF,RL115A                                                                  
