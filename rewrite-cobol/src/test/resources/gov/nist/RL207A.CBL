      *HEADER,COBOL,RL206A,SUBPRG,RL207A                                              
000100 IDENTIFICATION DIVISION.                                         RL2074.2
000200 PROGRAM-ID.                                                      RL2074.2
000300     RL207A.                                                      RL2074.2
000400****************************************************************  RL2074.2
000500*                                                              *  RL2074.2
000600*    VALIDATION FOR:-                                          *  RL2074.2
000700*                                                              *  RL2074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2074.2
000900*                                                              *  RL2074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2074.2
001100*                                                              *  RL2074.2
001200****************************************************************  RL2074.2
001300*GENERAL:    THE FUNCTION OF THIS PROGRAM IS TO PROCESS A RELATIVERL2074.2
001400*            I-O FILE RANDOMLY (ACCESS MODE IS DYNAMIC). THE FILE RL2074.2
001500*            USED AS INPUT IS THAT FILE CREATED BY RL206A.        RL2074.2
001600*                                                                 RL2074.2
001700*            FIRST THE FILE IS VERIFED AS TO THE EXISTANCE AND    RL2074.2
001800*            ACCURACY OF THE 500  RECORDS CREATED IN THE FIRST    RL2074.2
001900*            PROGRAM.  SECONDLY, RECORDS OF THE FILE ARE SEL-     RL2074.2
002000*            ECTIVELY UPDATED; AND THIRDLY, THE ACCURACY OF EACH  RL2074.2
002100*            RECORD IN THE FILE IS AGAIN VERIFIED.                RL2074.2
002200*                                                                 RL2074.2
002300*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL2074.2
002400*            PROGRAM ARE:                                         RL2074.2
002500*                                                                 RL2074.2
002600*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL2074.2
002700*                         RELATIVE  I-O DATA FILE                 RL2074.2
002800*                 X-55   SYSTEM PRINTER                           RL2074.2
002900*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL2074.2
003000*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL2074.2
003100*                 X-75   OBJECT OF VALUE OF CLAUSE                RL2074.2
003200*                 X-82   SOURCE-COMPUTER                          RL2074.2
003300*                 X-83   OBJECT-COMPUTER.                         RL2074.2
003400*                                                                 RL2074.2
003500****************************************************************  RL2074.2
003600 ENVIRONMENT DIVISION.                                            RL2074.2
003700 CONFIGURATION SECTION.                                           RL2074.2
003800 SOURCE-COMPUTER.                                                 RL2074.2
003900     XXXXX082.                                                    RL2074.2
004000 OBJECT-COMPUTER.                                                 RL2074.2
004100     XXXXX083.                                                    RL2074.2
004200 INPUT-OUTPUT SECTION.                                            RL2074.2
004300 FILE-CONTROL.                                                    RL2074.2
004400     SELECT PRINT-FILE ASSIGN TO                                  RL2074.2
004500     XXXXX055.                                                    RL2074.2
004600     SELECT  RL-FD1 ASSIGN TO                                     RL2074.2
004700     XXXXP021                                                     RL2074.2
004800             ORGANIZATION IS RELATIVE                             RL2074.2
004900             ACCESS  MODE IS DYNAMIC                              RL2074.2
005000             RELATIVE KEY RL-FD1-KEY                              RL2074.2
005100             STATUS WS-STATUS.                                    RL2074.2
005200 DATA DIVISION.                                                   RL2074.2
005300 FILE SECTION.                                                    RL2074.2
005400 FD  PRINT-FILE.                                                  RL2074.2
005500 01  PRINT-REC PICTURE X(120).                                    RL2074.2
005600 01  DUMMY-RECORD PICTURE X(120).                                 RL2074.2
005700 FD  RL-FD1                                                       RL2074.2
005800     LABEL RECORDS STANDARD                                       RL2074.2
005900C    VALUE OF                                                     RL2074.2
006000C    XXXXX074                                                     RL2074.2
006100C    IS                                                           RL2074.2
006200C    XXXXX075                                                     RL2074.2
006300*    XXXXX069                                                     RL2074.2
006400     BLOCK  CONTAINS 1 RECORDS                                    RL2074.2
006500     RECORD VARYING 120 TO 140 DEPENDING WRK-SIZE.                RL2074.2
006600 01  RL-FD1R1-F-G-140.                                            RL2074.2
006700     02 FILLER PICTURE X(140).                                    RL2074.2
006800 WORKING-STORAGE SECTION.                                         RL2074.2
006900 01  WS-STATUS    PIC XX.                                         RL2074.2
007000 01  WRK-SIZE     PIC 999.                                        RL2074.2
007100 01  WRK-CS-09V00 PIC S9(09)      USAGE COMP VALUE ZERO.          RL2074.2
007200 01  RL-FD1-KEY        PIC 9(09)  USAGE COMP VALUE ZERO.          RL2074.2
007300 01  WRK-DS-09V00-002 PIC S9(9) VALUE ZERO.                       RL2074.2
007400 01  WRK-CS-09V00-002 PIC S9(09)       USAGE COMP VALUE ZERO.     RL2074.2
007500 01  WRK-CS-09V00-003 PIC S9(09)       USAGE COMP VALUE ZERO.     RL2074.2
007600 01  I-O-ERROR-RL-FD1 PIC X(3) VALUE "NO ".                       RL2074.2
007700 01  WRK-CS-09V00-001 PIC S9(09)       USAGE COMP VALUE ZERO.     RL2074.2
007800 01  WRK-CS-09V00-004 PIC S9(09)       USAGE COMP VALUE ZERO.     RL2074.2
007900 01  WRK-CS-09V00-005 PIC S9(09)       USAGE COMP VALUE ZERO.     RL2074.2
008000 01  WRK-DS-09V00-001 PIC S9(09)      VALUE ZERO.                 RL2074.2
008100 01  FILE-RECORD-INFORMATION-REC.                                 RL2074.2
008200     03 FILE-RECORD-INFO-SKELETON.                                RL2074.2
008300        05 FILLER                 PICTURE X(48)       VALUE       RL2074.2
008400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2074.2
008500        05 FILLER                 PICTURE X(46)       VALUE       RL2074.2
008600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2074.2
008700        05 FILLER                 PICTURE X(26)       VALUE       RL2074.2
008800             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2074.2
008900        05 FILLER                 PICTURE X(37)       VALUE       RL2074.2
009000             ",RECKEY=                             ".             RL2074.2
009100        05 FILLER                 PICTURE X(38)       VALUE       RL2074.2
009200             ",ALTKEY1=                             ".            RL2074.2
009300        05 FILLER                 PICTURE X(38)       VALUE       RL2074.2
009400             ",ALTKEY2=                             ".            RL2074.2
009500        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2074.2
009600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2074.2
009700        05 FILE-RECORD-INFO-P1-120.                               RL2074.2
009800           07 FILLER              PIC X(5).                       RL2074.2
009900           07 XFILE-NAME           PIC X(6).                      RL2074.2
010000           07 FILLER              PIC X(8).                       RL2074.2
010100           07 XRECORD-NAME         PIC X(6).                      RL2074.2
010200           07 FILLER              PIC X(1).                       RL2074.2
010300           07 REELUNIT-NUMBER     PIC 9(1).                       RL2074.2
010400           07 FILLER              PIC X(7).                       RL2074.2
010500           07 XRECORD-NUMBER       PIC 9(6).                      RL2074.2
010600           07 FILLER              PIC X(6).                       RL2074.2
010700           07 UPDATE-NUMBER       PIC 9(2).                       RL2074.2
010800           07 FILLER              PIC X(5).                       RL2074.2
010900           07 ODO-NUMBER          PIC 9(4).                       RL2074.2
011000           07 FILLER              PIC X(5).                       RL2074.2
011100           07 XPROGRAM-NAME        PIC X(5).                      RL2074.2
011200           07 FILLER              PIC X(7).                       RL2074.2
011300           07 XRECORD-LENGTH       PIC 9(6).                      RL2074.2
011400           07 FILLER              PIC X(7).                       RL2074.2
011500           07 CHARS-OR-RECORDS    PIC X(2).                       RL2074.2
011600           07 FILLER              PIC X(1).                       RL2074.2
011700           07 XBLOCK-SIZE          PIC 9(4).                      RL2074.2
011800           07 FILLER              PIC X(6).                       RL2074.2
011900           07 RECORDS-IN-FILE     PIC 9(6).                       RL2074.2
012000           07 FILLER              PIC X(5).                       RL2074.2
012100           07 XFILE-ORGANIZATION   PIC X(2).                      RL2074.2
012200           07 FILLER              PIC X(6).                       RL2074.2
012300           07 XLABEL-TYPE          PIC X(1).                      RL2074.2
012400        05 FILE-RECORD-INFO-P121-240.                             RL2074.2
012500           07 FILLER              PIC X(8).                       RL2074.2
012600           07 XRECORD-KEY          PIC X(29).                     RL2074.2
012700           07 FILLER              PIC X(9).                       RL2074.2
012800           07 ALTERNATE-KEY1      PIC X(29).                      RL2074.2
012900           07 FILLER              PIC X(9).                       RL2074.2
013000           07 ALTERNATE-KEY2      PIC X(29).                      RL2074.2
013100           07 FILLER              PIC X(7).                       RL2074.2
013200 01  NEW-115-CHAR-AREA.                                           RL2074.2
013300   03         NEW-115-120         PIC X(115).                     RL2074.2
013400 01  NEW-125-CHAR-AREA.                                           RL2074.2
013500   03         NEW-125-120         PIC X(120).                     RL2074.2
013600   03         EXTRA-5-CHARS       PIC X(5).                       RL2074.2
013700 01  NEW-128-CHAR-AREA.                                           RL2074.2
013800   03         NEW-128-120         PIC X(120).                     RL2074.2
013900   03         EXTRA-8-CHARS       PIC X(8).                       RL2074.2
014000 01  NEW-130-CHAR-AREA.                                           RL2074.2
014100   03         NEW-130-120         PIC X(120).                     RL2074.2
014200   03         EXTRA-10-CHARS      PIC X(10).                      RL2074.2
014300 01  NEW-132-CHAR-AREA.                                           RL2074.2
014400   03         NEW-132-120         PIC X(120).                     RL2074.2
014500   03         EXTRA-12-CHARS      PIC X(12).                      RL2074.2
014600 01  NEW-135-CHAR-AREA.                                           RL2074.2
014700   03         NEW-135-120         PIC X(120).                     RL2074.2
014800   03         EXTRA-15-CHARS      PIC X(15).                      RL2074.2
014900 01  NEW-140-CHAR-AREA.                                           RL2074.2
015000   03         NEW-140-120         PIC X(120).                     RL2074.2
015100   03         EXTRA-20-CHARS      PIC X(20).                      RL2074.2
015200 01  NEW-145-CHAR-AREA.                                           RL2074.2
015300   03         NEW-145-120         PIC X(120).                     RL2074.2
015400   03         EXTRA-25-CHARS      PIC X(25).                      RL2074.2
015500                                                                  RL2074.2
015600 01  TEST-RESULTS.                                                RL2074.2
015700     02 FILLER                   PIC X      VALUE SPACE.          RL2074.2
015800     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2074.2
015900     02 FILLER                   PIC X      VALUE SPACE.          RL2074.2
016000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2074.2
016100     02 FILLER                   PIC X      VALUE SPACE.          RL2074.2
016200     02  PAR-NAME.                                                RL2074.2
016300       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2074.2
016400       03  PARDOT-X              PIC X      VALUE SPACE.          RL2074.2
016500       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2074.2
016600     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2074.2
016700     02 RE-MARK                  PIC X(61).                       RL2074.2
016800 01  TEST-COMPUTED.                                               RL2074.2
016900     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2074.2
017000     02 FILLER                   PIC X(17)  VALUE                 RL2074.2
017100            "       COMPUTED=".                                   RL2074.2
017200     02 COMPUTED-X.                                               RL2074.2
017300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2074.2
017400     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2074.2
017500                                 PIC -9(9).9(9).                  RL2074.2
017600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2074.2
017700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2074.2
017800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2074.2
017900     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2074.2
018000         04 COMPUTED-18V0                    PIC -9(18).          RL2074.2
018100         04 FILLER                           PIC X.               RL2074.2
018200     03 FILLER PIC X(50) VALUE SPACE.                             RL2074.2
018300 01  TEST-CORRECT.                                                RL2074.2
018400     02 FILLER PIC X(30) VALUE SPACE.                             RL2074.2
018500     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2074.2
018600     02 CORRECT-X.                                                RL2074.2
018700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2074.2
018800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2074.2
018900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2074.2
019000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2074.2
019100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2074.2
019200     03      CR-18V0 REDEFINES CORRECT-A.                         RL2074.2
019300         04 CORRECT-18V0                     PIC -9(18).          RL2074.2
019400         04 FILLER                           PIC X.               RL2074.2
019500     03 FILLER PIC X(2) VALUE SPACE.                              RL2074.2
019600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2074.2
019700 01  CCVS-C-1.                                                    RL2074.2
019800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2074.2
019900-    "SS  PARAGRAPH-NAME                                          RL2074.2
020000-    "       REMARKS".                                            RL2074.2
020100     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2074.2
020200 01  CCVS-C-2.                                                    RL2074.2
020300     02 FILLER                     PIC X        VALUE SPACE.      RL2074.2
020400     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2074.2
020500     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2074.2
020600     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2074.2
020700     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2074.2
020800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2074.2
020900 01  REC-CT                        PIC 99       VALUE ZERO.       RL2074.2
021000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2074.2
021100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2074.2
021200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2074.2
021300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2074.2
021400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2074.2
021500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2074.2
021600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2074.2
021700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2074.2
021800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2074.2
021900 01  CCVS-H-1.                                                    RL2074.2
022000     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2074.2
022100     02  FILLER                    PIC X(42)    VALUE             RL2074.2
022200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2074.2
022300     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2074.2
022400 01  CCVS-H-2A.                                                   RL2074.2
022500   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2074.2
022600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2074.2
022700   02  FILLER                        PIC XXXX   VALUE             RL2074.2
022800     "4.2 ".                                                      RL2074.2
022900   02  FILLER                        PIC X(28)  VALUE             RL2074.2
023000            " COPY - NOT FOR DISTRIBUTION".                       RL2074.2
023100   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2074.2
023200                                                                  RL2074.2
023300 01  CCVS-H-2B.                                                   RL2074.2
023400   02  FILLER                        PIC X(15)  VALUE             RL2074.2
023500            "TEST RESULT OF ".                                    RL2074.2
023600   02  TEST-ID                       PIC X(9).                    RL2074.2
023700   02  FILLER                        PIC X(4)   VALUE             RL2074.2
023800            " IN ".                                               RL2074.2
023900   02  FILLER                        PIC X(12)  VALUE             RL2074.2
024000     " HIGH       ".                                              RL2074.2
024100   02  FILLER                        PIC X(22)  VALUE             RL2074.2
024200            " LEVEL VALIDATION FOR ".                             RL2074.2
024300   02  FILLER                        PIC X(58)  VALUE             RL2074.2
024400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2074.2
024500 01  CCVS-H-3.                                                    RL2074.2
024600     02  FILLER                      PIC X(34)  VALUE             RL2074.2
024700            " FOR OFFICIAL USE ONLY    ".                         RL2074.2
024800     02  FILLER                      PIC X(58)  VALUE             RL2074.2
024900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2074.2
025000     02  FILLER                      PIC X(28)  VALUE             RL2074.2
025100            "  COPYRIGHT   1985 ".                                RL2074.2
025200 01  CCVS-E-1.                                                    RL2074.2
025300     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2074.2
025400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2074.2
025500     02 ID-AGAIN                     PIC X(9).                    RL2074.2
025600     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2074.2
025700 01  CCVS-E-2.                                                    RL2074.2
025800     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2074.2
025900     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2074.2
026000     02 CCVS-E-2-2.                                               RL2074.2
026100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2074.2
026200         03 FILLER                   PIC X      VALUE SPACE.      RL2074.2
026300         03 ENDER-DESC               PIC X(44)  VALUE             RL2074.2
026400            "ERRORS ENCOUNTERED".                                 RL2074.2
026500 01  CCVS-E-3.                                                    RL2074.2
026600     02  FILLER                      PIC X(22)  VALUE             RL2074.2
026700            " FOR OFFICIAL USE ONLY".                             RL2074.2
026800     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2074.2
026900     02  FILLER                      PIC X(58)  VALUE             RL2074.2
027000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2074.2
027100     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2074.2
027200     02 FILLER                       PIC X(15)  VALUE             RL2074.2
027300             " COPYRIGHT 1985".                                   RL2074.2
027400 01  CCVS-E-4.                                                    RL2074.2
027500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2074.2
027600     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2074.2
027700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2074.2
027800     02 FILLER                       PIC X(40)  VALUE             RL2074.2
027900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2074.2
028000 01  XXINFO.                                                      RL2074.2
028100     02 FILLER                       PIC X(19)  VALUE             RL2074.2
028200            "*** INFORMATION ***".                                RL2074.2
028300     02 INFO-TEXT.                                                RL2074.2
028400       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2074.2
028500       04 XXCOMPUTED                 PIC X(20).                   RL2074.2
028600       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2074.2
028700       04 XXCORRECT                  PIC X(20).                   RL2074.2
028800     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2074.2
028900 01  HYPHEN-LINE.                                                 RL2074.2
029000     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2074.2
029100     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2074.2
029200-    "*****************************************".                 RL2074.2
029300     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2074.2
029400-    "******************************".                            RL2074.2
029500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2074.2
029600     "RL207A".                                                    RL2074.2
029700 PROCEDURE DIVISION.                                              RL2074.2
029800 CCVS1 SECTION.                                                   RL2074.2
029900 OPEN-FILES.                                                      RL2074.2
030000     OPEN    OUTPUT PRINT-FILE.                                   RL2074.2
030100     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2074.2
030200     MOVE    SPACE TO TEST-RESULTS.                               RL2074.2
030300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2074.2
030400     MOVE    ZERO TO REC-SKL-SUB.                                 RL2074.2
030500     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2074.2
030600 CCVS-INIT-FILE.                                                  RL2074.2
030700     ADD     1 TO REC-SKL-SUB.                                    RL2074.2
030800     MOVE    FILE-RECORD-INFO-SKELETON                            RL2074.2
030900          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2074.2
031000 CCVS-INIT-EXIT.                                                  RL2074.2
031100     GO TO CCVS1-EXIT.                                            RL2074.2
031200 CLOSE-FILES.                                                     RL2074.2
031300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2074.2
031400 TERMINATE-CCVS.                                                  RL2074.2
031500*    EXIT PROGRAM.                                                RL2074.2
031600*TERMINATE-CALL.                                                  RL2074.2
031700     STOP     RUN.                                                RL2074.2
031800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2074.2
031900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2074.2
032000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2074.2
032100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2074.2
032200     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2074.2
032300 PRINT-DETAIL.                                                    RL2074.2
032400     IF REC-CT NOT EQUAL TO ZERO                                  RL2074.2
032500             MOVE "." TO PARDOT-X                                 RL2074.2
032600             MOVE REC-CT TO DOTVALUE.                             RL2074.2
032700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2074.2
032800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2074.2
032900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2074.2
033000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2074.2
033100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2074.2
033200     MOVE SPACE TO CORRECT-X.                                     RL2074.2
033300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2074.2
033400     MOVE     SPACE TO RE-MARK.                                   RL2074.2
033500 HEAD-ROUTINE.                                                    RL2074.2
033600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2074.2
033700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2074.2
033800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2074.2
033900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2074.2
034000 COLUMN-NAMES-ROUTINE.                                            RL2074.2
034100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2074.2
034200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2074.2
034300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2074.2
034400 END-ROUTINE.                                                     RL2074.2
034500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2074.2
034600 END-RTN-EXIT.                                                    RL2074.2
034700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2074.2
034800 END-ROUTINE-1.                                                   RL2074.2
034900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2074.2
035000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2074.2
035100      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2074.2
035200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2074.2
035300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2074.2
035400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2074.2
035500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2074.2
035600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2074.2
035700  END-ROUTINE-12.                                                 RL2074.2
035800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2074.2
035900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2074.2
036000         MOVE "NO " TO ERROR-TOTAL                                RL2074.2
036100         ELSE                                                     RL2074.2
036200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2074.2
036300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2074.2
036400     PERFORM WRITE-LINE.                                          RL2074.2
036500 END-ROUTINE-13.                                                  RL2074.2
036600     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2074.2
036700         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2074.2
036800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2074.2
036900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2074.2
037000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2074.2
037100      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2074.2
037200          MOVE "NO " TO ERROR-TOTAL                               RL2074.2
037300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2074.2
037400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2074.2
037500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2074.2
037600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2074.2
037700 WRITE-LINE.                                                      RL2074.2
037800     ADD 1 TO RECORD-COUNT.                                       RL2074.2
037900*    IF RECORD-COUNT GREATER 50                                   RL2074.2
038000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2074.2
038100*        MOVE SPACE TO DUMMY-RECORD                               RL2074.2
038200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2074.2
038300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2074.2
038400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2074.2
038500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2074.2
038600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2074.2
038700*        MOVE ZERO TO RECORD-COUNT.                               RL2074.2
038800     PERFORM WRT-LN.                                              RL2074.2
038900 WRT-LN.                                                          RL2074.2
039000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2074.2
039100     MOVE SPACE TO DUMMY-RECORD.                                  RL2074.2
039200 BLANK-LINE-PRINT.                                                RL2074.2
039300     PERFORM WRT-LN.                                              RL2074.2
039400 FAIL-ROUTINE.                                                    RL2074.2
039500     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2074.2
039600            GO TO FAIL-ROUTINE-WRITE.                             RL2074.2
039700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2074.2
039800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2074.2
039900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2074.2
040000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2074.2
040100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2074.2
040200     GO TO  FAIL-ROUTINE-EX.                                      RL2074.2
040300 FAIL-ROUTINE-WRITE.                                              RL2074.2
040400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2074.2
040500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2074.2
040600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2074.2
040700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2074.2
040800 FAIL-ROUTINE-EX. EXIT.                                           RL2074.2
040900 BAIL-OUT.                                                        RL2074.2
041000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2074.2
041100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2074.2
041200 BAIL-OUT-WRITE.                                                  RL2074.2
041300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2074.2
041400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2074.2
041500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2074.2
041600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2074.2
041700 BAIL-OUT-EX. EXIT.                                               RL2074.2
041800 CCVS1-EXIT.                                                      RL2074.2
041900     EXIT.                                                        RL2074.2
042000 SECT-RL207A-001 SECTION.                                         RL2074.2
042100 REL-INIT-003.                                                    RL2074.2
042200     MOVE   "VIII-26 4.5.4" TO ANSI-REFERENCE.                    RL2074.2
042300     OPEN    INPUT  RL-FD1.                                       RL2074.2
042400     MOVE   "REL-TEST-003"   TO PAR-NAME.                         RL2074.2
042500     MOVE    ZERO TO   RL-FD1-KEY.                                RL2074.2
042600     MOVE    ZERO TO   WRK-CS-09V00-002.                          RL2074.2
042700     MOVE    ZERO  TO  WRK-CS-09V00-003.                          RL2074.2
042800*                                                                 RL2074.2
042900     MOVE    01 TO REC-CT.                                        RL2074.2
043000     MOVE   "READ RANDOM"  TO FEATURE.                            RL2074.2
043100 REL-TEST-003-R.                                                  RL2074.2
043200     ADD     1 TO WRK-CS-09V00-003.                               RL2074.2
043300     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
043400     IF      RL-FD1-KEY GREATER +501                              RL2074.2
043500             MOVE "INVALID KEY NOT TAKEN" TO COMPUTED-A           RL2074.2
043600             MOVE RL-FD1-KEY TO CORRECT-18V0                      RL2074.2
043700             PERFORM FAIL                                         RL2074.2
043800             PERFORM PRINT-DETAIL                                 RL2074.2
043900             ADD 1 TO REC-CT                                      RL2074.2
044000             GO TO REL-WRITE-003.                                 RL2074.2
044100     READ    RL-FD1                                               RL2074.2
044200             INVALID KEY GO TO REL-WRITE-003.                     RL2074.2
044300     MOVE    RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
044400     IF      XRECORD-NUMBER (1) EQUAL TO RL-FD1-KEY               RL2074.2
044500             GO TO REL-TEST-003-R.                                RL2074.2
044600     MOVE    "YES" TO I-O-ERROR-RL-FD1.                           RL2074.2
044700     ADD      1 TO WRK-CS-09V00-002.                              RL2074.2
044800     GO TO    REL-TEST-003-R.                                     RL2074.2
044900 REL-WRITE-003.                                                   RL2074.2
045000     IF      RL-FD1-KEY NOT EQUAL TO 501                          RL2074.2
045100              MOVE "WRONG KEY/NOT 500" TO CORRECT-A               RL2074.2
045200              MOVE  RL-FD1-KEY TO COMPUTED-18V0                   RL2074.2
045300              PERFORM FAIL                                        RL2074.2
045400              ELSE                                                RL2074.2
045500              PERFORM PASS.                                       RL2074.2
045600     PERFORM PRINT-DETAIL.                                        RL2074.2
045700*                                                                 RL2074.2
045800*01                                                               RL2074.2
045900*                                                                 RL2074.2
046000     ADD      1 TO REC-CT.                                        RL2074.2
046100     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 RL2074.2
046200              MOVE "WRONG RECORD/NOT 500" TO CORRECT-A            RL2074.2
046300              MOVE  XRECORD-NUMBER (1) TO COMPUTED-18V0           RL2074.2
046400              PERFORM FAIL                                        RL2074.2
046500              ELSE                                                RL2074.2
046600              PERFORM PASS.                                       RL2074.2
046700     PERFORM PRINT-DETAIL.                                        RL2074.2
046800*                                                                 RL2074.2
046900*02                                                               RL2074.2
047000*                                                                 RL2074.2
047100     ADD      1 TO REC-CT.                                        RL2074.2
047200     IF      WRK-CS-09V00-003 NOT EQUAL TO 501                    RL2074.2
047300              MOVE "INCORRECT RECORD COUNT" TO RE-MARK            RL2074.2
047400              MOVE  WRK-CS-09V00-003 TO COMPUTED-18V0             RL2074.2
047500             MOVE 501  TO CORRECT-18V0                            RL2074.2
047600              PERFORM FAIL                                        RL2074.2
047700              ELSE                                                RL2074.2
047800              PERFORM PASS.                                       RL2074.2
047900     PERFORM PRINT-DETAIL.                                        RL2074.2
048000*                                                                 RL2074.2
048100*03                                                               RL2074.2
048200*                                                                 RL2074.2
048300     ADD      1 TO REC-CT.                                        RL2074.2
048400     IF       I-O-ERROR-RL-FD1 EQUAL TO "YES"                     RL2074.2
048500              MOVE WRK-CS-09V00-002 TO COMPUTED-18V0              RL2074.2
048600              MOVE "RECORDS DID NOT COMPARE" TO RE-MARK           RL2074.2
048700              PERFORM FAIL                                        RL2074.2
048800              ELSE                                                RL2074.2
048900              PERFORM PASS.                                       RL2074.2
049000     PERFORM PRINT-DETAIL.                                        RL2074.2
049100*                                                                 RL2074.2
049200*04                                                               RL2074.2
049300*                                                                 RL2074.2
049400     ADD      1 TO REC-CT.                                        RL2074.2
049500     CLOSE    RL-FD1.                                             RL2074.2
049600 REL-INIT-004-R .                                                 RL2074.2
049700     MOVE     "REL-TEST-004" TO PAR-NAME.                         RL2074.2
049800     OPEN I-O RL-FD1.                                             RL2074.2
049900     MOVE     ZERO TO RL-FD1-KEY.                                 RL2074.2
050000     MOVE     ZERO TO WRK-CS-09V00-002.                           RL2074.2
050100     MOVE     ZERO TO WRK-CS-09V00-003.                           RL2074.2
050200      MOVE    ZERO TO WRK-CS-09V00-004.                           RL2074.2
050300      MOVE    ZERO TO WRK-CS-09V00-005.                           RL2074.2
050400*                                                                 RL2074.2
050500     MOVE     01 TO REC-CT.                                       RL2074.2
050600     MOVE     SPACE TO  FILE-RECORD-INFO-P1-120 (1).              RL2074.2
050700     MOVE    "REWRITE"  TO FEATURE.                               RL2074.2
050800 REL-TEST-004-R.                                                  RL2074.2
050900     ADD      5 TO  WRK-CS-09V00-003.                             RL2074.2
051000     MOVE     WRK-CS-09V00-003 TO RL-FD1-KEY.                     RL2074.2
051100      IF     RL-FD1-KEY GREATER 505                               RL2074.2
051200              MOVE "INVALID KEY/NOT TAKEN" TO COMPUTED-A          RL2074.2
051300              MOVE  RL-FD1-KEY TO CORRECT-18V0                    RL2074.2
051400              PERFORM FAIL                                        RL2074.2
051500              PERFORM PRINT-DETAIL                                RL2074.2
051600              ADD 1 TO REC-CT                                     RL2074.2
051700              GO TO REL-TEST-004-3.                               RL2074.2
051800     READ     RL-FD1                                              RL2074.2
051900              INVALID KEY GO TO REL-TEST-004-1.                   RL2074.2
052000     MOVE    RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1)      RL2074.2
052100     ADD      01 TO UPDATE-NUMBER (1).                            RL2074.2
052200     MOVE CCVS-PGM-ID       TO XPROGRAM-NAME (1).                 RL2074.2
052300     MOVE     FILE-RECORD-INFO-P1-120 (1) TO RL-FD1R1-F-G-140.    RL2074.2
052400     REWRITE    RL-FD1R1-F-G-140                                  RL2074.2
052500              INVALID KEY GO TO REL-TEST-004-2.                   RL2074.2
052600     GO TO    REL-TEST-004-R.                                     RL2074.2
052700 REL-TEST-004-1.                                                  RL2074.2
052800     IF       RL-FD1-KEY LESS THAN 501                            RL2074.2
052900              ADD 1 TO  WRK-CS-09V00-004                          RL2074.2
053000              GO TO REL-TEST-004-R.                               RL2074.2
053100     PERFORM   PASS.                                              RL2074.2
053200     PERFORM   PRINT-DETAIL.                                      RL2074.2
053300*                                                                 RL2074.2
053400*01                                                               RL2074.2
053500*                                                                 RL2074.2
053600     ADD     1  TO REC-CT.                                        RL2074.2
053700     GO TO    REL-TEST-004-3.                                     RL2074.2
053800 REL-TEST-004-2.                                                  RL2074.2
053900     ADD      1 TO WRK-CS-09V00-005.                              RL2074.2
054000     IF       RL-FD1-KEY LESS 501                                 RL2074.2
054100              GO TO REL-TEST-004-R.                               RL2074.2
054200 REL-TEST-004-3.                                                  RL2074.2
054300     IF       WRK-CS-09V00-004 NOT EQUAL TO ZERO                  RL2074.2
054400              MOVE "INVALID KEY ON READ" TO COMPUTED-A            RL2074.2
054500              MOVE WRK-CS-09V00-004 TO CORRECT-18V0               RL2074.2
054600              PERFORM FAIL                                        RL2074.2
054700              ELSE                                                RL2074.2
054800              PERFORM PASS.                                       RL2074.2
054900     PERFORM PRINT-DETAIL.                                        RL2074.2
055000*                                                                 RL2074.2
055100*02                                                               RL2074.2
055200*                                                                 RL2074.2
055300     ADD      1 TO REC-CT.                                        RL2074.2
055400     IF      WRK-CS-09V00-005 NOT EQUAL TO ZERO                   RL2074.2
055500             MOVE "INVALID KEY ON REWRITE" TO COMPUTED-A          RL2074.2
055600             MOVE  WRK-CS-09V00-005 TO CORRECT-18V0               RL2074.2
055700             PERFORM FAIL                                         RL2074.2
055800     ELSE                                                         RL2074.2
055900             PERFORM PASS.                                        RL2074.2
056000     PERFORM PRINT-DETAIL.                                        RL2074.2
056100*                                                                 RL2074.2
056200*03                                                               RL2074.2
056300*                                                                 RL2074.2
056400     ADD     1 TO REC-CT.                                         RL2074.2
056500     CLOSE   RL-FD1.                                              RL2074.2
056600 REL-INIT-005.                                                    RL2074.2
056700     MOVE   "REL-TEST-005" TO PAR-NAME.                           RL2074.2
056800     OPEN    INPUT  RL-FD1.                                       RL2074.2
056900     MOVE    501  TO WRK-CS-09V00-003.                            RL2074.2
057000     MOVE    ZERO TO WRK-CS-09V00-004.                            RL2074.2
057100     MOVE    ZERO TO WRK-CS-09V00-005.                            RL2074.2
057200     MOVE    ZERO TO WRK-CS-09V00-002.                            RL2074.2
057300     MOVE     SPACE TO  FILE-RECORD-INFO-P1-120 (1).              RL2074.2
057400     MOVE     01 TO REC-CT.                                       RL2074.2
057500*                                                                 RL2074.2
057600     MOVE    "READ RANDOM"  TO FEATURE.                           RL2074.2
057700 REL-TEST-005-R.                                                  RL2074.2
057800     SUBTRACT 1 FROM    WRK-CS-09V00-003.                         RL2074.2
057900     MOVE     WRK-CS-09V00-003 TO RL-FD1-KEY.                     RL2074.2
058000     IF      WRK-CS-09V00-003 LESS THAN ZERO                      RL2074.2
058100             MOVE    "INVALID KEY/NOT TAKEN"  TO RE-MARK          RL2074.2
058200             MOVE   WRK-CS-09V00-003  TO COMPUTED-18V0            RL2074.2
058300             MOVE   ZERO TO CORRECT-18V0                          RL2074.2
058400              PERFORM FAIL                                        RL2074.2
058500              PERFORM PRINT-DETAIL                                RL2074.2
058600              ADD 1 TO REC-CT                                     RL2074.2
058700              GO TO REL-TEST-005-3.                               RL2074.2
058800     READ     RL-FD1                                              RL2074.2
058900              INVALID KEY  GO TO REL-TEST-005-1.                  RL2074.2
059000     MOVE     RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1).    RL2074.2
059100     IF     UPDATE-NUMBER (1) EQUAL TO 00                         RL2074.2
059200              ADD 1 TO WRK-CS-09V00-004.                          RL2074.2
059300     IF     UPDATE-NUMBER (1) EQUAL TO 01                         RL2074.2
059400              ADD 1 TO WRK-CS-09V00-005.                          RL2074.2
059500     GO TO    REL-TEST-005-R.                                     RL2074.2
059600 REL-TEST-005-1.                                                  RL2074.2
059700     IF       RL-FD1-KEY GREATER ZERO                             RL2074.2
059800             ADD 1 TO WRK-CS-09V00-002                            RL2074.2
059900              GO TO REL-TEST-005-R.                               RL2074.2
060000     PERFORM   PASS.                                              RL2074.2
060100     PERFORM   PRINT-DETAIL.                                      RL2074.2
060200     ADD      1  TO REC-CT.                                       RL2074.2
060300*01                                                               RL2074.2
060400     GO TO    REL-TEST-005-3.                                     RL2074.2
060500 REL-TEST-005-3.                                                  RL2074.2
060600     IF       WRK-CS-09V00-004 NOT EQUAL TO 400                   RL2074.2
060700              MOVE "NON-UPDATED RECORDS" TO COMPUTED-A            RL2074.2
060800             MOVE WRK-CS-09V00-004 TO CORRECT-18V0                RL2074.2
060900             MOVE "SHOULD BE 400" TO RE-MARK                      RL2074.2
061000             PERFORM FAIL                                         RL2074.2
061100     ELSE                                                         RL2074.2
061200             PERFORM PASS.                                        RL2074.2
061300     PERFORM PRINT-DETAIL.                                        RL2074.2
061400*                                                                 RL2074.2
061500*                                                                 RL2074.2
061600*02                                                               RL2074.2
061700*                                                                 RL2074.2
061800     ADD     1 TO REC-CT.                                         RL2074.2
061900     IF      WRK-CS-09V00-005 NOT EQUAL TO 100                    RL2074.2
062000             MOVE "UPDATED RECORDS" TO COMPUTED-A                 RL2074.2
062100             MOVE WRK-CS-09V00-005 TO CORRECT-18V0                RL2074.2
062200             MOVE "SHOULD BE 100" TO RE-MARK                      RL2074.2
062300             PERFORM FAIL                                         RL2074.2
062400     ELSE                                                         RL2074.2
062500             PERFORM PASS.                                        RL2074.2
062600     PERFORM PRINT-DETAIL.                                        RL2074.2
062700*                                                                 RL2074.2
062800*03                                                               RL2074.2
062900*                                                                 RL2074.2
063000     ADD     1 TO REC-CT.                                         RL2074.2
063100     IF      WRK-CS-09V00-002 GREATER 1                           RL2074.2
063200             MOVE WRK-CS-09V00-002 TO COMPUTED-N                  RL2074.2
063300             MOVE  "INVALID KEY/READS" TO CORRECT-A               RL2074.2
063400             PERFORM FAIL                                         RL2074.2
063500     ELSE                                                         RL2074.2
063600             PERFORM PASS.                                        RL2074.2
063700     PERFORM PRINT-DETAIL.                                        RL2074.2
063800*                                                                 RL2074.2
063900*04                                                               RL2074.2
064000*                                                                 RL2074.2
064100     ADD     1 TO REC-CT.                                         RL2074.2
064200     CLOSE   RL-FD1.                                              RL2074.2
064300 REL-INIT-006.                                                    RL2074.2
064400     MOVE   "REL-TEST-006" TO PAR-NAME.                           RL2074.2
064500     OPEN    I-O  RL-FD1.                                         RL2074.2
064600     MOVE   "VAR. SIZE REC UPDATE" TO FEATURE.                    RL2074.2
064700     MOVE    1    TO WRK-CS-09V00-003.                            RL2074.2
064800     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
064900     READ    RL-FD1                                               RL2074.2
065000             INVALID KEY  MOVE "REL-INIT-006 READ1" TO PAR-NAME   RL2074.2
065100                          MOVE "INVALID KEY ON 1ST RECORD READ"   RL2074.2
065200                             TO RE-MARK                           RL2074.2
065300                          PERFORM FAIL                            RL2074.2
065400                          PERFORM PRINT-DETAIL                    RL2074.2
065500                          GO TO   REL-INIT-007.                   RL2074.2
065600     MOVE    RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
065700     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
065800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO RL-FD1R1-F-G-140.     RL2074.2
065900     MOVE    120 TO WRK-SIZE.                                     RL2074.2
066000     REWRITE RL-FD1R1-F-G-140                                     RL2074.2
066100             INVALID KEY MOVE "REL-INIT-006 REWRITE" TO PAR-NAME  RL2074.2
066200                         MOVE "INVALID KEY ON 1ST RECORD REWRITE" RL2074.2
066300                            TO RE-MARK                            RL2074.2
066400                         PERFORM FAIL                             RL2074.2
066500                         PERFORM PRINT-DETAIL                     RL2074.2
066600                         GO TO   REL-INIT-007.                    RL2074.2
066700     MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
066800     READ    RL-FD1                                               RL2074.2
066900             INVALID KEY  MOVE "REL-INIT-006 READ2" TO PAR-NAME   RL2074.2
067000                          MOVE "INVALID KEY ON 1ST RECORD READ"   RL2074.2
067100                             TO RE-MARK                           RL2074.2
067200                          PERFORM FAIL                            RL2074.2
067300                          PERFORM PRINT-DETAIL                    RL2074.2
067400                          GO TO   REL-INIT-007.                   RL2074.2
067500 REL-TEST-006.                                                    RL2074.2
067600     MOVE   "REL-TEST-006" TO PAR-NAME.                           RL2074.2
067700     MOVE    RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
067800     IF      UPDATE-NUMBER (1) = 98                               RL2074.2
067900             PERFORM PASS                                         RL2074.2
068000             PERFORM PRINT-DETAIL                                 RL2074.2
068100     ELSE                                                         RL2074.2
068200             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
068300                  TO RE-MARK                                      RL2074.2
068400             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
068500             MOVE    98 TO CORRECT-N                              RL2074.2
068600             PERFORM FAIL                                         RL2074.2
068700             PERFORM PRINT-DETAIL.                                RL2074.2
068800*                                                                 RL2074.2
068900 REL-INIT-007.                                                    RL2074.2
069000     MOVE   "REL-TEST-007" TO PAR-NAME.                           RL2074.2
069100     MOVE    11   TO WRK-CS-09V00-003.                            RL2074.2
069200     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
069300     READ    RL-FD1                                               RL2074.2
069400             INVALID KEY  MOVE "REL-INIT-007 READ1" TO PAR-NAME   RL2074.2
069500                          MOVE "INVALID KEY ON 11TH RECORD READ"  RL2074.2
069600                             TO RE-MARK                           RL2074.2
069700                          PERFORM FAIL                            RL2074.2
069800                          PERFORM PRINT-DETAIL                    RL2074.2
069900                          GO TO   REL-INIT-008.                   RL2074.2
070000     MOVE    RL-FD1R1-F-G-140 TO NEW-130-CHAR-AREA.               RL2074.2
070100     MOVE    NEW-130-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
070200     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
070300     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-130-120.          RL2074.2
070400     MOVE    NEW-130-CHAR-AREA           TO RL-FD1R1-F-G-140.     RL2074.2
070500     MOVE    130 TO WRK-SIZE.                                     RL2074.2
070600     REWRITE RL-FD1R1-F-G-140                                     RL2074.2
070700             INVALID KEY MOVE "REL-INIT-007 REWRITE" TO PAR-NAME  RL2074.2
070800                         MOVE "INVALID KEY ON 11TH RECORD REWRITE"RL2074.2
070900                            TO RE-MARK                            RL2074.2
071000                         PERFORM FAIL                             RL2074.2
071100                         PERFORM PRINT-DETAIL                     RL2074.2
071200                         GO TO   REL-INIT-008.                    RL2074.2
071300     MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
071400     READ    RL-FD1                                               RL2074.2
071500             INVALID KEY  MOVE "REL-INIT-007 READ2" TO PAR-NAME   RL2074.2
071600                          MOVE "INVALID KEY ON 11TH RECORD READ"  RL2074.2
071700                             TO RE-MARK                           RL2074.2
071800                          PERFORM FAIL                            RL2074.2
071900                          PERFORM PRINT-DETAIL                    RL2074.2
072000                          GO TO   REL-INIT-008.                   RL2074.2
072100 REL-TEST-007.                                                    RL2074.2
072200     MOVE   "REL-TEST-007" TO PAR-NAME.                           RL2074.2
072300     MOVE    RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
072400     IF      UPDATE-NUMBER (1) = 98                               RL2074.2
072500             PERFORM PASS                                         RL2074.2
072600             PERFORM PRINT-DETAIL                                 RL2074.2
072700     ELSE                                                         RL2074.2
072800             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
072900                  TO RE-MARK                                      RL2074.2
073000             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
073100             MOVE    98 TO CORRECT-N                              RL2074.2
073200             PERFORM FAIL                                         RL2074.2
073300             PERFORM PRINT-DETAIL.                                RL2074.2
073400*                                                                 RL2074.2
073500 REL-INIT-008.                                                    RL2074.2
073600     MOVE   "REL-TEST-008" TO PAR-NAME.                           RL2074.2
073700     MOVE    21   TO WRK-CS-09V00-003.                            RL2074.2
073800     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
073900     READ    RL-FD1                                               RL2074.2
074000             INVALID KEY  MOVE "REL-INIT-008 READ1" TO PAR-NAME   RL2074.2
074100                          MOVE "INVALID KEY ON 21ST RECORD READ"  RL2074.2
074200                             TO RE-MARK                           RL2074.2
074300                          PERFORM FAIL                            RL2074.2
074400                          PERFORM PRINT-DETAIL                    RL2074.2
074500                          GO TO   REL-INIT-009.                   RL2074.2
074600     MOVE    RL-FD1R1-F-G-140 TO NEW-140-CHAR-AREA.               RL2074.2
074700     MOVE    NEW-140-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
074800     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
074900     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-140-120.          RL2074.2
075000     MOVE    NEW-140-CHAR-AREA           TO RL-FD1R1-F-G-140.     RL2074.2
075100     MOVE    140 TO WRK-SIZE.                                     RL2074.2
075200     REWRITE RL-FD1R1-F-G-140                                     RL2074.2
075300             INVALID KEY MOVE "REL-INIT-008 REWRITE" TO PAR-NAME  RL2074.2
075400                         MOVE "INVALID KEY ON 21ST RECORD REWRITE"RL2074.2
075500                            TO RE-MARK                            RL2074.2
075600                         PERFORM FAIL                             RL2074.2
075700                         PERFORM PRINT-DETAIL                     RL2074.2
075800                         GO TO   REL-INIT-009.                    RL2074.2
075900     MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
076000     READ    RL-FD1                                               RL2074.2
076100             INVALID KEY  MOVE "REL-INIT-008 READ2" TO PAR-NAME   RL2074.2
076200                          MOVE "INVALID KEY ON 21ST RECORD READ"  RL2074.2
076300                             TO RE-MARK                           RL2074.2
076400                          PERFORM FAIL                            RL2074.2
076500                          PERFORM PRINT-DETAIL                    RL2074.2
076600                          GO TO   REL-INIT-009.                   RL2074.2
076700 REL-TEST-008.                                                    RL2074.2
076800     MOVE   "REL-TEST-008" TO PAR-NAME.                           RL2074.2
076900     MOVE    RL-FD1R1-F-G-140 TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
077000     IF      UPDATE-NUMBER (1) = 98                               RL2074.2
077100             PERFORM PASS                                         RL2074.2
077200             PERFORM PRINT-DETAIL                                 RL2074.2
077300     ELSE                                                         RL2074.2
077400             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
077500                  TO RE-MARK                                      RL2074.2
077600             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
077700             MOVE    98 TO CORRECT-N                              RL2074.2
077800             PERFORM FAIL                                         RL2074.2
077900             PERFORM PRINT-DETAIL.                                RL2074.2
078000*                                                                 RL2074.2
078100 REL-INIT-009.                                                    RL2074.2
078200     MOVE   "REL-TEST-009" TO PAR-NAME.                           RL2074.2
078300     MOVE    31   TO WRK-CS-09V00-003.                            RL2074.2
078400     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
078500     READ    RL-FD1                                               RL2074.2
078600             INVALID KEY  MOVE "REL-INIT-009 READ1" TO PAR-NAME   RL2074.2
078700                          MOVE "INVALID KEY ON 31ST RECORD READ"  RL2074.2
078800                             TO RE-MARK                           RL2074.2
078900                          PERFORM FAIL                            RL2074.2
079000                          PERFORM PRINT-DETAIL                    RL2074.2
079100                          GO TO   REL-INIT-010.                   RL2074.2
079200     MOVE    RL-FD1R1-F-G-140 TO NEW-125-CHAR-AREA.               RL2074.2
079300     MOVE    NEW-125-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
079400     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
079500     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-128-120.          RL2074.2
079600     MOVE   "ABCDEFGH" TO EXTRA-8-CHARS.                          RL2074.2
079700     MOVE    NEW-128-CHAR-AREA           TO RL-FD1R1-F-G-140.     RL2074.2
079800     MOVE    128 TO WRK-SIZE.                                     RL2074.2
079900 REL-TEST-009-1.                                                  RL2074.2
080000     MOVE   "REL-TEST-009-1" TO PAR-NAME.                         RL2074.2
080100     REWRITE RL-FD1R1-F-G-140                                     RL2074.2
080200             INVALID KEY MOVE "INVALID KEY - 31ST RECORD REWRITE" RL2074.2
080300                            TO RE-MARK                            RL2074.2
080400                         PERFORM FAIL                             RL2074.2
080500                         PERFORM PRINT-DETAIL                     RL2074.2
080600                         GO TO   REL-INIT-010.                    RL2074.2
080700     PERFORM PASS.                                                RL2074.2
080800     PERFORM PRINT-DETAIL.                                        RL2074.2
080900     MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
081000     READ    RL-FD1                                               RL2074.2
081100             INVALID KEY  MOVE "REL-INIT-009 READ2" TO PAR-NAME   RL2074.2
081200                          MOVE "INVALID KEY ON 31ST RECORD READ"  RL2074.2
081300                             TO RE-MARK                           RL2074.2
081400                          PERFORM FAIL                            RL2074.2
081500                          PERFORM PRINT-DETAIL                    RL2074.2
081600                          GO TO   REL-INIT-010.                   RL2074.2
081700 REL-TEST-009-2.                                                  RL2074.2
081800     MOVE   "REL-TEST-009-2" TO PAR-NAME.                         RL2074.2
081900     MOVE    RL-FD1R1-F-G-140 TO NEW-128-CHAR-AREA.               RL2074.2
082000     MOVE    NEW-128-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
082100     IF      UPDATE-NUMBER (1) = 98                               RL2074.2
082200             PERFORM PASS                                         RL2074.2
082300             PERFORM PRINT-DETAIL                                 RL2074.2
082400     ELSE                                                         RL2074.2
082500             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
082600                  TO RE-MARK                                      RL2074.2
082700             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
082800             MOVE    98 TO CORRECT-N                              RL2074.2
082900             PERFORM FAIL                                         RL2074.2
083000             PERFORM PRINT-DETAIL.                                RL2074.2
083100 REL-TEST-009-3.                                                  RL2074.2
083200     MOVE   "REL-TEST-009-3" TO PAR-NAME.                         RL2074.2
083300     IF      EXTRA-8-CHARS = "ABCDEFGH"                           RL2074.2
083400             PERFORM PASS                                         RL2074.2
083500             PERFORM PRINT-DETAIL                                 RL2074.2
083600     ELSE                                                         RL2074.2
083700             MOVE   "INVALID UPDATE OF DIFFERENT LENGTH. RECS"    RL2074.2
083800                  TO RE-MARK                                      RL2074.2
083900             MOVE   "ABCDEFGH" TO CORRECT-X                       RL2074.2
084000             MOVE    EXTRA-8-CHARS TO COMPUTED-X                  RL2074.2
084100             PERFORM FAIL                                         RL2074.2
084200             PERFORM PRINT-DETAIL.                                RL2074.2
084300*                                                                 RL2074.2
084400 REL-INIT-010.                                                    RL2074.2
084500     MOVE   "REL-TEST-010" TO PAR-NAME.                           RL2074.2
084600     MOVE    32   TO WRK-CS-09V00-003.                            RL2074.2
084700     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
084800     READ    RL-FD1                                               RL2074.2
084900             INVALID KEY  MOVE "REL-INIT-010 READ1" TO PAR-NAME   RL2074.2
085000                          MOVE "INVALID KEY ON 32ND RECORD READ"  RL2074.2
085100                             TO RE-MARK                           RL2074.2
085200                          PERFORM FAIL                            RL2074.2
085300                          PERFORM PRINT-DETAIL                    RL2074.2
085400                          GO TO   REL-INIT-011.                   RL2074.2
085500     MOVE    RL-FD1R1-F-G-140 TO NEW-135-CHAR-AREA.               RL2074.2
085600     MOVE    NEW-135-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
085700     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
085800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-132-120.          RL2074.2
085900     MOVE   "ABCDEFGHIJKL" TO EXTRA-12-CHARS.                     RL2074.2
086000     MOVE    NEW-132-CHAR-AREA           TO RL-FD1R1-F-G-140.     RL2074.2
086100     MOVE    132 TO WRK-SIZE.                                     RL2074.2
086200 REL-TEST-010-1.                                                  RL2074.2
086300     MOVE   "REL-TEST-010-1" TO PAR-NAME.                         RL2074.2
086400     REWRITE RL-FD1R1-F-G-140                                     RL2074.2
086500             INVALID KEY MOVE "INVALID KEY - 32ND RECORD REWRITE" RL2074.2
086600                            TO RE-MARK                            RL2074.2
086700                         PERFORM FAIL                             RL2074.2
086800                         PERFORM PRINT-DETAIL                     RL2074.2
086900                         GO TO   REL-INIT-011.                    RL2074.2
087000     PERFORM PASS.                                                RL2074.2
087100     PERFORM PRINT-DETAIL.                                        RL2074.2
087200     MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
087300     READ    RL-FD1                                               RL2074.2
087400             INVALID KEY                                          RL2074.2
087500                          MOVE "INVALID KEY ON 32ND RECORD READ"  RL2074.2
087600                             TO RE-MARK                           RL2074.2
087700                          PERFORM FAIL                            RL2074.2
087800                          PERFORM PRINT-DETAIL                    RL2074.2
087900                          GO TO   REL-INIT-011.                   RL2074.2
088000 REL-TEST-010-2.                                                  RL2074.2
088100     MOVE   "REL-TEST-010-2" TO PAR-NAME.                         RL2074.2
088200     MOVE    RL-FD1R1-F-G-140 TO NEW-132-CHAR-AREA.               RL2074.2
088300     MOVE    NEW-132-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
088400     IF      UPDATE-NUMBER (1) = 98                               RL2074.2
088500             PERFORM PASS                                         RL2074.2
088600             PERFORM PRINT-DETAIL                                 RL2074.2
088700     ELSE                                                         RL2074.2
088800             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
088900                  TO RE-MARK                                      RL2074.2
089000             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
089100             MOVE    98 TO CORRECT-N                              RL2074.2
089200             PERFORM FAIL                                         RL2074.2
089300             PERFORM PRINT-DETAIL.                                RL2074.2
089400 REL-TEST-010-3.                                                  RL2074.2
089500     MOVE   "REL-TEST-010-3" TO PAR-NAME.                         RL2074.2
089600     IF      EXTRA-12-CHARS = "ABCDEFGHIJKL"                      RL2074.2
089700             PERFORM PASS                                         RL2074.2
089800             PERFORM PRINT-DETAIL                                 RL2074.2
089900     ELSE                                                         RL2074.2
090000             MOVE   "INVALID UPDATE OF DIFFERENT LENGTH. RECS"    RL2074.2
090100                  TO RE-MARK                                      RL2074.2
090200             MOVE   "ABCDEFGHIJKL" TO CORRECT-X                   RL2074.2
090300             MOVE    EXTRA-12-CHARS TO COMPUTED-X                 RL2074.2
090400             PERFORM FAIL                                         RL2074.2
090500             PERFORM PRINT-DETAIL.                                RL2074.2
090600*                                                                 RL2074.2
090700 REL-INIT-011.                                                    RL2074.2
090800     MOVE   "REL-TEST-011" TO PAR-NAME.                           RL2074.2
090900     MOVE    33   TO WRK-CS-09V00-003.                            RL2074.2
091000     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
091100     READ    RL-FD1                                               RL2074.2
091200             INVALID KEY  MOVE "REL-INIT-011 READ1" TO PAR-NAME   RL2074.2
091300                          MOVE "INVALID KEY ON 33RD RECORD READ"  RL2074.2
091400                             TO RE-MARK                           RL2074.2
091500                          PERFORM FAIL                            RL2074.2
091600                          PERFORM PRINT-DETAIL                    RL2074.2
091700                          GO TO   REL-INIT-012.                   RL2074.2
091800     MOVE    RL-FD1R1-F-G-140 TO NEW-145-CHAR-AREA.               RL2074.2
091900     MOVE    NEW-145-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
092000     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
092100     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-145-120.          RL2074.2
092200     MOVE   "ABCDEFGHIJKLMNOPQRSTUVWXY"  TO EXTRA-25-CHARS.       RL2074.2
092300     MOVE    145 TO WRK-SIZE.                                     RL2074.2
092400 REL-TEST-011-1.                                                  RL2074.2
092500     MOVE   "REL-TEST-011-1" TO PAR-NAME.                         RL2074.2
092600*    REWRITE RL-FD1R1-F-G-140 FROM NEW-145-CHAR-AREA              RL2074.2
092700*            INVALID GO TO REL-TEST-011-1-A.                      RL2074.2
092800*REL-TEST-011-1-A.                                                RL2074.2
092900*    IF      WS-STATUS = "44"                                     RL2074.2
093000*            PERFORM PASS                                         RL2074.2
093100*            PERFORM PRINT-DETAIL                                 RL2074.2
093200*    ELSE                                                         RL2074.2
093300*            MOVE  "INCORRECT FILE STATUS HAS OCCURED"            RL2074.2
093400*                TO RE-MARK                                       RL2074.2
093500*            MOVE   "44"       TO CORRECT-X                       RL2074.2
093600*            MOVE    WS-STATUS TO COMPUTED-X                      RL2074.2
093700*            PERFORM FAIL                                         RL2074.2
093800*            PERFORM PRINT-DETAIL.                                RL2074.2
093900*    MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
094000*    READ    RL-FD1  INTO NEW-140-CHAR-AREA                       RL2074.2
094100*            INVALID KEY                                          RL2074.2
094200*                         MOVE "INVALID KEY ON 33RD RECORD READ"  RL2074.2
094300*                            TO RE-MARK                           RL2074.2
094400*                         PERFORM FAIL                            RL2074.2
094500*                         PERFORM PRINT-DETAIL                    RL2074.2
094600                          GO TO   CCVS-EXIT.                      RL2074.2
094700 REL-TEST-011-2.                                                  RL2074.2
094800     MOVE   "REL-TEST-011-2" TO PAR-NAME.                         RL2074.2
094900     MOVE    NEW-140-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
095000     IF      UPDATE-NUMBER (1) = ZERO                             RL2074.2
095100             PERFORM PASS                                         RL2074.2
095200             PERFORM PRINT-DETAIL                                 RL2074.2
095300     ELSE                                                         RL2074.2
095400             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
095500                  TO RE-MARK                                      RL2074.2
095600             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
095700             MOVE    00 TO CORRECT-N                              RL2074.2
095800             PERFORM FAIL                                         RL2074.2
095900             PERFORM PRINT-DETAIL.                                RL2074.2
096000 REL-TEST-011-3.                                                  RL2074.2
096100     MOVE   "REL-TEST-011-3" TO PAR-NAME.                         RL2074.2
096200     IF      EXTRA-20-CHARS = "ABCDEFGHIJKLMNOPQRST"              RL2074.2
096300             PERFORM PASS                                         RL2074.2
096400             PERFORM PRINT-DETAIL                                 RL2074.2
096500     ELSE                                                         RL2074.2
096600             MOVE   "INVALID UPDATE OF DIFFERENT LENGTH. RECS"    RL2074.2
096700                  TO RE-MARK                                      RL2074.2
096800             MOVE "ABCDEFGHIJKLMNOPQRST" TO CORRECT-X             RL2074.2
096900             MOVE    EXTRA-20-CHARS TO COMPUTED-X                 RL2074.2
097000             PERFORM FAIL                                         RL2074.2
097100             PERFORM PRINT-DETAIL.                                RL2074.2
097200 REL-TEST-011-4.                                                  RL2074.2
097300     MOVE   "REL-TEST-011-4" TO PAR-NAME.                         RL2074.2
097400     IF      WRK-SIZE = 140                                       RL2074.2
097500             PERFORM PASS                                         RL2074.2
097600             PERFORM PRINT-DETAIL                                 RL2074.2
097700     ELSE                                                         RL2074.2
097800             MOVE   "INCORRECT RECORD LENGTH READ"                RL2074.2
097900                  TO RE-MARK                                      RL2074.2
098000             MOVE    140      TO CORRECT-18V0                     RL2074.2
098100             MOVE    WRK-SIZE TO COMPUTED-18V0                    RL2074.2
098200             PERFORM FAIL                                         RL2074.2
098300             PERFORM PRINT-DETAIL.                                RL2074.2
098400*                                                                 RL2074.2
098500 REL-INIT-012.                                                    RL2074.2
098600     MOVE   "REL-TEST-012" TO PAR-NAME.                           RL2074.2
098700     MOVE    34   TO WRK-CS-09V00-003.                            RL2074.2
098800     MOVE    WRK-CS-09V00-003 TO RL-FD1-KEY.                      RL2074.2
098900     READ    RL-FD1                                               RL2074.2
099000             INVALID KEY  MOVE "REL-INIT-012 READ1" TO PAR-NAME   RL2074.2
099100                          MOVE "INVALID KEY ON 34TH RECORD READ"  RL2074.2
099200                             TO RE-MARK                           RL2074.2
099300                          PERFORM FAIL                            RL2074.2
099400                          PERFORM PRINT-DETAIL                    RL2074.2
099500                          GO TO   CCVS-EXIT.                      RL2074.2
099600     MOVE    LOW-VALUES TO FILE-RECORD-INFO-P1-120 (1).           RL2074.2
099700     MOVE    98 TO UPDATE-NUMBER (1).                             RL2074.2
099800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-115-120.          RL2074.2
099900     MOVE    115 TO WRK-SIZE.                                     RL2074.2
100000 REL-TEST-012-1.                                                  RL2074.2
100100     MOVE   "REL-TEST-012-1" TO PAR-NAME.                         RL2074.2
100200                                                                  RL2074.2
100300     REWRITE RL-FD1R1-F-G-140 FROM NEW-115-CHAR-AREA              RL2074.2
100400             INVALID GO TO REL-TEST-012-2.                        RL2074.2
100500*REL-TEST-012-1-A.                                                RL2074.2
100600*    IF      WS-STATUS = "44"                                     RL2074.2
100700*            PERFORM PASS                                         RL2074.2
100800*            PERFORM PRINT-DETAIL                                 RL2074.2
100900*    ELSE                                                         RL2074.2
101000*            MOVE  "INCORRECT FILE STATUS HAS OCCURED"            RL2074.2
101100*                TO RE-MARK                                       RL2074.2
101200*            MOVE   "44"       TO CORRECT-X                       RL2074.2
101300*            MOVE    WS-STATUS TO COMPUTED-X                      RL2074.2
101400*            PERFORM FAIL                                         RL2074.2
101500*            PERFORM PRINT-DETAIL.                                RL2074.2
101600*    MOVE    SPACES   TO RL-FD1R1-F-G-140.                        RL2074.2
101700*    READ    RL-FD1  INTO NEW-140-CHAR-AREA                       RL2074.2
101800*            INVALID KEY  MOVE "REL-INIT-012 READ2" TO PAR-NAME   RL2074.2
101900*                         MOVE "INVALID KEY ON 34TH RECORD READ"  RL2074.2
102000*                            TO RE-MARK                           RL2074.2
102100*                         PERFORM FAIL                            RL2074.2
102200*                         PERFORM PRINT-DETAIL                    RL2074.2
102300*                         GO TO   REL-INIT-012.                   RL2074.2
102400 REL-TEST-012-2.                                                  RL2074.2
102500     MOVE   "REL-TEST-012-2" TO PAR-NAME.                         RL2074.2
102600     MOVE    NEW-140-120      TO FILE-RECORD-INFO-P1-120 (1).     RL2074.2
102700     IF      UPDATE-NUMBER (1) = ZERO                             RL2074.2
102800             PERFORM PASS                                         RL2074.2
102900             PERFORM PRINT-DETAIL                                 RL2074.2
103000     ELSE                                                         RL2074.2
103100             MOVE   "INVALID UPDATE OF UPDATE NUMBER FIELD"       RL2074.2
103200                  TO RE-MARK                                      RL2074.2
103300             MOVE    UPDATE-NUMBER (1) TO COMPUTED-X              RL2074.2
103400             MOVE    00 TO CORRECT-N                              RL2074.2
103500             PERFORM FAIL                                         RL2074.2
103600             PERFORM PRINT-DETAIL.                                RL2074.2
103700 REL-TEST-012-3.                                                  RL2074.2
103800     MOVE   "REL-TEST-012-3" TO PAR-NAME.                         RL2074.2
103900     IF      EXTRA-20-CHARS = "ABCDEFGHIJKLMNOPQRST"              RL2074.2
104000             PERFORM PASS                                         RL2074.2
104100             PERFORM PRINT-DETAIL                                 RL2074.2
104200     ELSE                                                         RL2074.2
104300             MOVE   "INVALID UPDATE OF DIFFERENT LENGTH. RECS"    RL2074.2
104400                  TO RE-MARK                                      RL2074.2
104500             MOVE "ABCDEFGHIJKLMNOPQRST" TO CORRECT-X             RL2074.2
104600             MOVE    EXTRA-20-CHARS TO COMPUTED-X                 RL2074.2
104700             PERFORM FAIL                                         RL2074.2
104800             PERFORM PRINT-DETAIL.                                RL2074.2
104900 REL-TEST-012-4.                                                  RL2074.2
105000     MOVE   "REL-TEST-012-4" TO PAR-NAME.                         RL2074.2
105100     IF      WRK-SIZE = 140                                       RL2074.2
105200             PERFORM PASS                                         RL2074.2
105300             PERFORM PRINT-DETAIL                                 RL2074.2
105400     ELSE                                                         RL2074.2
105500             MOVE   "INCORRECT RECORD LENGTH READ"                RL2074.2
105600                  TO RE-MARK                                      RL2074.2
105700             MOVE    140      TO CORRECT-18V0                     RL2074.2
105800             MOVE    WRK-SIZE TO COMPUTED-18V0                    RL2074.2
105900             PERFORM FAIL                                         RL2074.2
106000             PERFORM PRINT-DETAIL.                                RL2074.2
106100*                                                                 RL2074.2
106200 CCVS-EXIT SECTION.                                               RL2074.2
106300 CCVS-999999.                                                     RL2074.2
106400     GO TO CLOSE-FILES.                                           RL2074.2
      *END-OF,RL207A                                                                  
