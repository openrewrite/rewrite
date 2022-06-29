      *HEADER,COBOL,RL119A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1194.2
000200 PROGRAM-ID.                                                      RL1194.2
000300     RL119A.                                                      RL1194.2
000400****************************************************************  RL1194.2
000500*                                                              *  RL1194.2
000600*    VALIDATION FOR:-                                          *  RL1194.2
000700*                                                              *  RL1194.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1194.2
000900*                                                              *  RL1194.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1194.2
001100*                                                              *  RL1194.2
001200****************************************************************  RL1194.2
001300*                                                              *  RL1194.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1194.2
001500*                                                              *  RL1194.2
001600*            X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1194.2
001700*                    RELATIVE  I-O DATA FILE                   *  RL1194.2
001800*            X-55   SYSTEM PRINTER                             *  RL1194.2
001900*            X-69   ADDITIONAL VALUE OF CLAUSES                *  RL1194.2
002000*            X-74   VALUE OF IMPLEMENTOR-NAME                  *  RL1194.2
002100*            X-75   OBJECT OF VALUE OF CLAUSE                  *  RL1194.2
002200*            X-82   SOURCE-COMPUTER                            *  RL1194.2
002300*            X-83   OBJECT-COMPUTER.                           *  RL1194.2
002400*            X-92   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   *  RL1194.2
002500*                    RELATIVE  I-O DATA FILE                   *  RL1194.2
002600*                                                              *  RL1194.2
002700****************************************************************  RL1194.2
002800*    RL119A                                                    *  RL1194.2
002900****************************************************************  RL1194.2
003000*            THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND *  RL1194.2
003100*            SEMANTIC ACTIONS ASSOCIATED WITH THE "STATUS"     *  RL1194.2
003200*            CLAUSE FOR THE VALUES "35".                       *  RL1194.2
003300*                                                              *  RL1194.2
003400*            THE CODE FOR THE VALUE "39" HAS BEEN ASTERISKED   *  RL1194.2
003500*            OUT AS NO SUITABLE METHOD OF TESTING THIS         *  RL1194.2
003600*            CONDITION IS AVAILABLE AT THE TIME OF WRITING.    *  RL1194.2
003700****************************************************************  RL1194.2
003800 ENVIRONMENT DIVISION.                                            RL1194.2
003900 CONFIGURATION SECTION.                                           RL1194.2
004000 SOURCE-COMPUTER.                                                 RL1194.2
004100     XXXXX082.                                                    RL1194.2
004200 OBJECT-COMPUTER.                                                 RL1194.2
004300     XXXXX083.                                                    RL1194.2
004400 INPUT-OUTPUT SECTION.                                            RL1194.2
004500 FILE-CONTROL.                                                    RL1194.2
004600     SELECT PRINT-FILE ASSIGN TO                                  RL1194.2
004700     XXXXX055.                                                    RL1194.2
004800     SELECT   RL-FD3 ASSIGN                                       RL1194.2
004900     XXXXX092                                                     RL1194.2
005000              ORGANIZATION RELATIVE                               RL1194.2
005100              ACCESS  RANDOM                                      RL1194.2
005200              RELATIVE  RL-FD3-KEY                                RL1194.2
005300         FILE STATUS IS RL-FD3-STATUS.                            RL1194.2
005400 DATA DIVISION.                                                   RL1194.2
005500 FILE SECTION.                                                    RL1194.2
005600 FD  PRINT-FILE.                                                  RL1194.2
005700 01  PRINT-REC PICTURE X(120).                                    RL1194.2
005800 01  DUMMY-RECORD PICTURE X(120).                                 RL1194.2
005900 FD  RL-FD3                                                       RL1194.2
006000C    VALUE OF                                                     RL1194.2
006100C    XXXXX074                                                     RL1194.2
006200C    IS                                                           RL1194.2
006300C    XXXXX076                                                     RL1194.2
006400*    XXXXX069                                                     RL1194.2
006500     LABEL RECORDS ARE STANDARD                                   RL1194.2
006600     BLOCK CONTAINS 1 RECORDS                                     RL1194.2
006700     DATA RECORD RL-FD3R1-F-G-240.                                RL1194.2
006800 01  RL-FD3R1-F-G-240.                                            RL1194.2
006900     05 RL-FD3-WRK-120 PIC X(120).                                RL1194.2
007000     05 RL-FD3-GRP-120.                                           RL1194.2
007100        10 RL-FD3-WRK-XN-0001-O120F                               RL1194.2
007200                        PICTURE X OCCURS 120 TIMES.               RL1194.2
007300 WORKING-STORAGE SECTION.                                         RL1194.2
007400 01  GRP-0001.                                                    RL1194.2
007500     05 RL-FD3-KEY   PIC 9(8)  VALUE ZERO.                        RL1194.2
007600     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
007700     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
007800     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
007900     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
008000     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
008100     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
008200     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       RL1194.2
008300     05 RL-FD3-STATUS     PIC  XX  VALUE  SPACE.                  RL1194.2
008400     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 RL1194.2
008500     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 RL1194.2
008600     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 RL1194.2
008700     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 RL1194.2
008800     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 RL1194.2
008900     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 RL1194.2
009000     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 RL1194.2
009100     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 RL1194.2
009200     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 RL1194.2
009300 01  FILE-RECORD-INFORMATION-REC.                                 RL1194.2
009400     03 FILE-RECORD-INFO-SKELETON.                                RL1194.2
009500        05 FILLER                 PICTURE X(48)       VALUE       RL1194.2
009600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1194.2
009700        05 FILLER                 PICTURE X(46)       VALUE       RL1194.2
009800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1194.2
009900        05 FILLER                 PICTURE X(26)       VALUE       RL1194.2
010000             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1194.2
010100        05 FILLER                 PICTURE X(37)       VALUE       RL1194.2
010200             ",RECKEY=                             ".             RL1194.2
010300        05 FILLER                 PICTURE X(38)       VALUE       RL1194.2
010400             ",ALTKEY1=                             ".            RL1194.2
010500        05 FILLER                 PICTURE X(38)       VALUE       RL1194.2
010600             ",ALTKEY2=                             ".            RL1194.2
010700        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1194.2
010800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1194.2
010900        05 FILE-RECORD-INFO-P1-120.                               RL1194.2
011000           07 FILLER              PIC X(5).                       RL1194.2
011100           07 XFILE-NAME           PIC X(6).                      RL1194.2
011200           07 FILLER              PIC X(8).                       RL1194.2
011300           07 XRECORD-NAME         PIC X(6).                      RL1194.2
011400           07 FILLER              PIC X(1).                       RL1194.2
011500           07 REELUNIT-NUMBER     PIC 9(1).                       RL1194.2
011600           07 FILLER              PIC X(7).                       RL1194.2
011700           07 XRECORD-NUMBER       PIC 9(6).                      RL1194.2
011800           07 FILLER              PIC X(6).                       RL1194.2
011900           07 UPDATE-NUMBER       PIC 9(2).                       RL1194.2
012000           07 FILLER              PIC X(5).                       RL1194.2
012100           07 ODO-NUMBER          PIC 9(4).                       RL1194.2
012200           07 FILLER              PIC X(5).                       RL1194.2
012300           07 XPROGRAM-NAME        PIC X(5).                      RL1194.2
012400           07 FILLER              PIC X(7).                       RL1194.2
012500           07 XRECORD-LENGTH       PIC 9(6).                      RL1194.2
012600           07 FILLER              PIC X(7).                       RL1194.2
012700           07 CHARS-OR-RECORDS    PIC X(2).                       RL1194.2
012800           07 FILLER              PIC X(1).                       RL1194.2
012900           07 XBLOCK-SIZE          PIC 9(4).                      RL1194.2
013000           07 FILLER              PIC X(6).                       RL1194.2
013100           07 RECORDS-IN-FILE     PIC 9(6).                       RL1194.2
013200           07 FILLER              PIC X(5).                       RL1194.2
013300           07 XFILE-ORGANIZATION   PIC X(2).                      RL1194.2
013400           07 FILLER              PIC X(6).                       RL1194.2
013500           07 XLABEL-TYPE          PIC X(1).                      RL1194.2
013600        05 FILE-RECORD-INFO-P121-240.                             RL1194.2
013700           07 FILLER              PIC X(8).                       RL1194.2
013800           07 XRECORD-KEY          PIC X(29).                     RL1194.2
013900           07 FILLER              PIC X(9).                       RL1194.2
014000           07 ALTERNATE-KEY1      PIC X(29).                      RL1194.2
014100           07 FILLER              PIC X(9).                       RL1194.2
014200           07 ALTERNATE-KEY2      PIC X(29).                      RL1194.2
014300           07 FILLER              PIC X(7).                       RL1194.2
014400 01  TEST-RESULTS.                                                RL1194.2
014500     02 FILLER                   PIC X      VALUE SPACE.          RL1194.2
014600     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1194.2
014700     02 FILLER                   PIC X      VALUE SPACE.          RL1194.2
014800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1194.2
014900     02 FILLER                   PIC X      VALUE SPACE.          RL1194.2
015000     02  PAR-NAME.                                                RL1194.2
015100       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1194.2
015200       03  PARDOT-X              PIC X      VALUE SPACE.          RL1194.2
015300       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1194.2
015400     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1194.2
015500     02 RE-MARK                  PIC X(61).                       RL1194.2
015600 01  TEST-COMPUTED.                                               RL1194.2
015700     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1194.2
015800     02 FILLER                   PIC X(17)  VALUE                 RL1194.2
015900            "       COMPUTED=".                                   RL1194.2
016000     02 COMPUTED-X.                                               RL1194.2
016100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1194.2
016200     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1194.2
016300                                 PIC -9(9).9(9).                  RL1194.2
016400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1194.2
016500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1194.2
016600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1194.2
016700     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1194.2
016800         04 COMPUTED-18V0                    PIC -9(18).          RL1194.2
016900         04 FILLER                           PIC X.               RL1194.2
017000     03 FILLER PIC X(50) VALUE SPACE.                             RL1194.2
017100 01  TEST-CORRECT.                                                RL1194.2
017200     02 FILLER PIC X(30) VALUE SPACE.                             RL1194.2
017300     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1194.2
017400     02 CORRECT-X.                                                RL1194.2
017500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1194.2
017600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1194.2
017700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1194.2
017800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1194.2
017900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1194.2
018000     03      CR-18V0 REDEFINES CORRECT-A.                         RL1194.2
018100         04 CORRECT-18V0                     PIC -9(18).          RL1194.2
018200         04 FILLER                           PIC X.               RL1194.2
018300     03 FILLER PIC X(2) VALUE SPACE.                              RL1194.2
018400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1194.2
018500 01  CCVS-C-1.                                                    RL1194.2
018600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1194.2
018700-    "SS  PARAGRAPH-NAME                                          RL1194.2
018800-    "       REMARKS".                                            RL1194.2
018900     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1194.2
019000 01  CCVS-C-2.                                                    RL1194.2
019100     02 FILLER                     PIC X        VALUE SPACE.      RL1194.2
019200     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1194.2
019300     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1194.2
019400     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1194.2
019500     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1194.2
019600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1194.2
019700 01  REC-CT                        PIC 99       VALUE ZERO.       RL1194.2
019800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1194.2
019900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1194.2
020000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1194.2
020100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1194.2
020200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1194.2
020300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1194.2
020400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1194.2
020500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1194.2
020600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1194.2
020700 01  CCVS-H-1.                                                    RL1194.2
020800     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1194.2
020900     02  FILLER                    PIC X(42)    VALUE             RL1194.2
021000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1194.2
021100     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1194.2
021200 01  CCVS-H-2A.                                                   RL1194.2
021300   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1194.2
021400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1194.2
021500   02  FILLER                        PIC XXXX   VALUE             RL1194.2
021600     "4.2 ".                                                      RL1194.2
021700   02  FILLER                        PIC X(28)  VALUE             RL1194.2
021800            " COPY - NOT FOR DISTRIBUTION".                       RL1194.2
021900   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1194.2
022000                                                                  RL1194.2
022100 01  CCVS-H-2B.                                                   RL1194.2
022200   02  FILLER                        PIC X(15)  VALUE             RL1194.2
022300            "TEST RESULT OF ".                                    RL1194.2
022400   02  TEST-ID                       PIC X(9).                    RL1194.2
022500   02  FILLER                        PIC X(4)   VALUE             RL1194.2
022600            " IN ".                                               RL1194.2
022700   02  FILLER                        PIC X(12)  VALUE             RL1194.2
022800     " HIGH       ".                                              RL1194.2
022900   02  FILLER                        PIC X(22)  VALUE             RL1194.2
023000            " LEVEL VALIDATION FOR ".                             RL1194.2
023100   02  FILLER                        PIC X(58)  VALUE             RL1194.2
023200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1194.2
023300 01  CCVS-H-3.                                                    RL1194.2
023400     02  FILLER                      PIC X(34)  VALUE             RL1194.2
023500            " FOR OFFICIAL USE ONLY    ".                         RL1194.2
023600     02  FILLER                      PIC X(58)  VALUE             RL1194.2
023700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1194.2
023800     02  FILLER                      PIC X(28)  VALUE             RL1194.2
023900            "  COPYRIGHT   1985 ".                                RL1194.2
024000 01  CCVS-E-1.                                                    RL1194.2
024100     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1194.2
024200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1194.2
024300     02 ID-AGAIN                     PIC X(9).                    RL1194.2
024400     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1194.2
024500 01  CCVS-E-2.                                                    RL1194.2
024600     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1194.2
024700     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1194.2
024800     02 CCVS-E-2-2.                                               RL1194.2
024900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1194.2
025000         03 FILLER                   PIC X      VALUE SPACE.      RL1194.2
025100         03 ENDER-DESC               PIC X(44)  VALUE             RL1194.2
025200            "ERRORS ENCOUNTERED".                                 RL1194.2
025300 01  CCVS-E-3.                                                    RL1194.2
025400     02  FILLER                      PIC X(22)  VALUE             RL1194.2
025500            " FOR OFFICIAL USE ONLY".                             RL1194.2
025600     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1194.2
025700     02  FILLER                      PIC X(58)  VALUE             RL1194.2
025800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1194.2
025900     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1194.2
026000     02 FILLER                       PIC X(15)  VALUE             RL1194.2
026100             " COPYRIGHT 1985".                                   RL1194.2
026200 01  CCVS-E-4.                                                    RL1194.2
026300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1194.2
026400     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1194.2
026500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1194.2
026600     02 FILLER                       PIC X(40)  VALUE             RL1194.2
026700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1194.2
026800 01  XXINFO.                                                      RL1194.2
026900     02 FILLER                       PIC X(19)  VALUE             RL1194.2
027000            "*** INFORMATION ***".                                RL1194.2
027100     02 INFO-TEXT.                                                RL1194.2
027200       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1194.2
027300       04 XXCOMPUTED                 PIC X(20).                   RL1194.2
027400       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1194.2
027500       04 XXCORRECT                  PIC X(20).                   RL1194.2
027600     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1194.2
027700 01  HYPHEN-LINE.                                                 RL1194.2
027800     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1194.2
027900     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1194.2
028000-    "*****************************************".                 RL1194.2
028100     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1194.2
028200-    "******************************".                            RL1194.2
028300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1194.2
028400     "RL119A".                                                    RL1194.2
028500 PROCEDURE DIVISION.                                              RL1194.2
028600 DECLARATIVES.                                                    RL1194.2
028700 RL-FD3-01 SECTION.                                               RL1194.2
028800     USE AFTER ERROR PROCEDURE RL-FD3.                            RL1194.2
028900 RL-FD3-01-01.                                                    RL1194.2
029000     IF RL-FD3-STATUS EQUAL TO "35"                               RL1194.2
029100         PERFORM D-PASS                                           RL1194.2
029200     ELSE                                                         RL1194.2
029300         MOVE RL-FD3-STATUS TO COMPUTED-A                         RL1194.2
029400         MOVE "35" TO CORRECT-A                                   RL1194.2
029500         PERFORM D-FAIL.                                          RL1194.2
029600     PERFORM D-PRINT-DETAIL.                                      RL1194.2
029700     GO TO D-CLOSE-FILES.                                         RL1194.2
029800 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         RL1194.2
029900 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        RL1194.2
030000 D-CLOSE-FILES.                                                   RL1194.2
030100     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 RL1194.2
030200     CLOSE PRINT-FILE.                                            RL1194.2
030300     STOP RUN.                                                    RL1194.2
030400 D-PRINT-DETAIL.                                                  RL1194.2
030500     IF REC-CT NOT EQUAL TO ZERO                                  RL1194.2
030600             MOVE "." TO PARDOT-X                                 RL1194.2
030700             MOVE REC-CT TO DOTVALUE.                             RL1194.2
030800     MOVE     TEST-RESULTS TO PRINT-REC.                          RL1194.2
030900             PERFORM D-WRITE-LINE.                                RL1194.2
031000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM D-WRITE-LINE             RL1194.2
031100        PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX             RL1194.2
031200          ELSE PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.             RL1194.2
031300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1194.2
031400     MOVE SPACE TO CORRECT-X.                                     RL1194.2
031500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1194.2
031600     MOVE     SPACE TO RE-MARK.                                   RL1194.2
031700 D-END-ROUTINE.                                                   RL1194.2
031800     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            RL1194.2
031900     PERFORM D-WRITE-LINE 5 TIMES.                                RL1194.2
032000 D-END-RTN-EXIT.                                                  RL1194.2
032100     MOVE CCVS-E-1 TO DUMMY-RECORD.                               RL1194.2
032200     PERFORM D-WRITE-LINE 2 TIMES.                                RL1194.2
032300 D-END-ROUTINE-1.                                                 RL1194.2
032400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1194.2
032500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1194.2
032600      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1194.2
032700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO D-END-ROUTINE-12. RL1194.2
032800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1194.2
032900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1194.2
033000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1194.2
033100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.         RL1194.2
033200  D-END-ROUTINE-12.                                               RL1194.2
033300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1194.2
033400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1194.2
033500         MOVE "NO " TO ERROR-TOTAL                                RL1194.2
033600         ELSE                                                     RL1194.2
033700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1194.2
033800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1194.2
033900     PERFORM D-WRITE-LINE.                                        RL1194.2
034000 D-END-ROUTINE-13.                                                RL1194.2
034100     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1194.2
034200         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1194.2
034300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1194.2
034400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1194.2
034500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         RL1194.2
034600      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1194.2
034700          MOVE "NO " TO ERROR-TOTAL                               RL1194.2
034800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1194.2
034900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1194.2
035000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.        RL1194.2
035100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         RL1194.2
035200 D-WRITE-LINE.                                                    RL1194.2
035300     ADD 1 TO RECORD-COUNT.                                       RL1194.2
035400*    IF RECORD-COUNT GREATER 50                                   RL1194.2
035500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1194.2
035600*        MOVE SPACE TO DUMMY-RECORD                               RL1194.2
035700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1194.2
035800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM D-WRT-LN           RL1194.2
035900*        MOVE CCVS-C-2 TO DUMMY-RECORD                            RL1194.2
036000*        PERFORM D-WRT-LN 2 TIMES                                 RL1194.2
036100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN        RL1194.2
036200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1194.2
036300*        MOVE ZERO TO RECORD-COUNT.                               RL1194.2
036400     PERFORM D-WRT-LN.                                            RL1194.2
036500 D-WRT-LN.                                                        RL1194.2
036600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1194.2
036700     MOVE SPACE TO DUMMY-RECORD.                                  RL1194.2
036800 D-FAIL-ROUTINE.                                                  RL1194.2
036900     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1194.2
037000            GO TO   D-FAIL-ROUTINE-WRITE.                         RL1194.2
037100     IF     CORRECT-X NOT EQUAL TO SPACE                          RL1194.2
037200            GO TO D-FAIL-ROUTINE-WRITE.                           RL1194.2
037300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1194.2
037400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1194.2
037500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES. RL1194.2
037600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1194.2
037700     GO TO  D-FAIL-ROUTINE-EX.                                    RL1194.2
037800 D-FAIL-ROUTINE-WRITE.                                            RL1194.2
037900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE       RL1194.2
038000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1194.2
038100     MOVE   TEST-CORRECT TO PRINT-REC                             RL1194.2
038200     PERFORM D-WRITE-LINE 2 TIMES.                                RL1194.2
038300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1194.2
038400 D-FAIL-ROUTINE-EX. EXIT.                                         RL1194.2
038500 D-BAIL-OUT.                                                      RL1194.2
038600     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    RL1194.2
038700     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            RL1194.2
038800 D-BAIL-OUT-WRITE.                                                RL1194.2
038900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1194.2
039000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1194.2
039100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES. RL1194.2
039200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1194.2
039300 D-BAIL-OUT-EX. EXIT.                                             RL1194.2
039400 END DECLARATIVES.                                                RL1194.2
039500 CCVS1 SECTION.                                                   RL1194.2
039600 OPEN-FILES.                                                      RL1194.2
039700     OPEN    OUTPUT PRINT-FILE.                                   RL1194.2
039800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1194.2
039900     MOVE    SPACE TO TEST-RESULTS.                               RL1194.2
040000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1194.2
040100     MOVE    ZERO TO REC-SKL-SUB.                                 RL1194.2
040200     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1194.2
040300 CCVS-INIT-FILE.                                                  RL1194.2
040400     ADD     1 TO REC-SKL-SUB.                                    RL1194.2
040500     MOVE    FILE-RECORD-INFO-SKELETON                            RL1194.2
040600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1194.2
040700 CCVS-INIT-EXIT.                                                  RL1194.2
040800     GO TO CCVS1-EXIT.                                            RL1194.2
040900 CLOSE-FILES.                                                     RL1194.2
041000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1194.2
041100 TERMINATE-CCVS.                                                  RL1194.2
041200*    EXIT PROGRAM.                                                RL1194.2
041300*TERMINATE-CALL.                                                  RL1194.2
041400     STOP     RUN.                                                RL1194.2
041500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1194.2
041600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1194.2
041700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1194.2
041800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1194.2
041900     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1194.2
042000 PRINT-DETAIL.                                                    RL1194.2
042100     IF REC-CT NOT EQUAL TO ZERO                                  RL1194.2
042200             MOVE "." TO PARDOT-X                                 RL1194.2
042300             MOVE REC-CT TO DOTVALUE.                             RL1194.2
042400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1194.2
042500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1194.2
042600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1194.2
042700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1194.2
042800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1194.2
042900     MOVE SPACE TO CORRECT-X.                                     RL1194.2
043000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1194.2
043100     MOVE     SPACE TO RE-MARK.                                   RL1194.2
043200 HEAD-ROUTINE.                                                    RL1194.2
043300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1194.2
043400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1194.2
043500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1194.2
043600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1194.2
043700 COLUMN-NAMES-ROUTINE.                                            RL1194.2
043800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1194.2
043900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1194.2
044000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1194.2
044100 END-ROUTINE.                                                     RL1194.2
044200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1194.2
044300 END-RTN-EXIT.                                                    RL1194.2
044400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1194.2
044500 END-ROUTINE-1.                                                   RL1194.2
044600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1194.2
044700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1194.2
044800      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1194.2
044900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1194.2
045000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1194.2
045100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1194.2
045200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1194.2
045300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1194.2
045400  END-ROUTINE-12.                                                 RL1194.2
045500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1194.2
045600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1194.2
045700         MOVE "NO " TO ERROR-TOTAL                                RL1194.2
045800         ELSE                                                     RL1194.2
045900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1194.2
046000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1194.2
046100     PERFORM WRITE-LINE.                                          RL1194.2
046200 END-ROUTINE-13.                                                  RL1194.2
046300     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1194.2
046400         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1194.2
046500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1194.2
046600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1194.2
046700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1194.2
046800      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1194.2
046900          MOVE "NO " TO ERROR-TOTAL                               RL1194.2
047000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1194.2
047100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1194.2
047200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1194.2
047300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1194.2
047400 WRITE-LINE.                                                      RL1194.2
047500     ADD 1 TO RECORD-COUNT.                                       RL1194.2
047600*    IF RECORD-COUNT GREATER 50                                   RL1194.2
047700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1194.2
047800*        MOVE SPACE TO DUMMY-RECORD                               RL1194.2
047900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1194.2
048000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1194.2
048100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1194.2
048200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1194.2
048300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1194.2
048400*        MOVE ZERO TO RECORD-COUNT.                               RL1194.2
048500     PERFORM WRT-LN.                                              RL1194.2
048600 WRT-LN.                                                          RL1194.2
048700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1194.2
048800     MOVE SPACE TO DUMMY-RECORD.                                  RL1194.2
048900 BLANK-LINE-PRINT.                                                RL1194.2
049000     PERFORM WRT-LN.                                              RL1194.2
049100 FAIL-ROUTINE.                                                    RL1194.2
049200     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1194.2
049300            GO TO   FAIL-ROUTINE-WRITE.                           RL1194.2
049400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1194.2
049500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1194.2
049600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1194.2
049700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1194.2
049800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1194.2
049900     GO TO  FAIL-ROUTINE-EX.                                      RL1194.2
050000 FAIL-ROUTINE-WRITE.                                              RL1194.2
050100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1194.2
050200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1194.2
050300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1194.2
050400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1194.2
050500 FAIL-ROUTINE-EX. EXIT.                                           RL1194.2
050600 BAIL-OUT.                                                        RL1194.2
050700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1194.2
050800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1194.2
050900 BAIL-OUT-WRITE.                                                  RL1194.2
051000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1194.2
051100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1194.2
051200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1194.2
051300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1194.2
051400 BAIL-OUT-EX. EXIT.                                               RL1194.2
051500 CCVS1-EXIT.                                                      RL1194.2
051600     EXIT.                                                        RL1194.2
051700 SECT-RL119A-001 SECTION.                                         RL1194.2
051800*                                                                 RL1194.2
051900 REL-INIT-1.                                                      RL1194.2
052000     MOVE   "REL-TEST-1" TO PAR-NAME.                             RL1194.2
052100     MOVE   "VIII-4 1.3.4 4B" TO ANSI-REFERENCE.                  RL1194.2
052200     MOVE    SPACE TO RL-FD3-STATUS.                              RL1194.2
052300     OPEN    I-O RL-FD3.                                          RL1194.2
052400     GO TO   REL-TEST-1.                                          RL1194.2
052500 REL-DELETE-1.                                                    RL1194.2
052600     PERFORM DE-LETE.                                             RL1194.2
052700     PERFORM PRINT-DETAIL.                                        RL1194.2
052800     GO TO   REL-TEST-1-EXIT.                                     RL1194.2
052900 REL-TEST-1.                                                      RL1194.2
053000     IF      RL-FD3-STATUS NOT EQUAL  TO "35"                     RL1194.2
053100             MOVE   "NON-EXISTING FILE HAS BEEN OPENED"           RL1194.2
053200                  TO RE-MARK                                      RL1194.2
053300             MOVE    RL-FD3-STATUS TO COMPUTED-A                  RL1194.2
053400             MOVE   "35" TO CORRECT-A                             RL1194.2
053500             PERFORM FAIL                                         RL1194.2
053600             PERFORM PRINT-DETAIL                                 RL1194.2
053700     ELSE                                                         RL1194.2
053800             PERFORM PASS                                         RL1194.2
053900             PERFORM PRINT-DETAIL.                                RL1194.2
054000 REL-TEST-1-EXIT.                                                 RL1194.2
054100*    EXIT.                                                        RL1194.2
054200*                                                                 RL1194.2
054300 CCVS-EXIT SECTION.                                               RL1194.2
054400 CCVS-999999.                                                     RL1194.2
054500     GO TO CLOSE-FILES.                                           RL1194.2
      *END-OF,RL119A                                                                  
