      *HEADER,COBOL,RL105A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1054.2
000200 PROGRAM-ID.                                                      RL1054.2
000300     RL105A.                                                      RL1054.2
000400****************************************************************  RL1054.2
000500*                                                              *  RL1054.2
000600*    VALIDATION FOR:-                                          *  RL1054.2
000700*                                                              *  RL1054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1054.2
000900*                                                              *  RL1054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1054.2
001100*                                                              *  RL1054.2
001200****************************************************************  RL1054.2
001300*GENERAL:    THIS PROGRAM PROCESSED THREE RELATIVE FILES          RL1054.2
001400*            IDENTIFIED AS RL-FR1, RL-FR2 AND RL-FR3.  THE        RL1054.2
001500*            FUNCTION OF THIS PROGRAM IS TO CREATE THREE RELATIVE RL1054.2
001600*            I-O FILES RANDOMLLY (ACCESS MODE RANDOM)  AND VERIFY RL1054.2
001700*            THAT THEY WERE CREATED CORRECTLY.  THE FILES         RL1054.2
001800*            PROCESSED CONTAIN FIXED LENGTH RECORDS.              RL1054.2
001900*                                                                 RL1054.2
002000*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL1054.2
002100*            PROGRAM ARE:                                         RL1054.2
002200*                                                                 RL1054.2
002300*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1054.2
002400*                         RELATIVE  I-O DATA FILE-1               RL1054.2
002500*                 X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1054.2
002600*                         RELATIVE  I-O DATA FILE-2               RL1054.2
002700*                 X-23   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1054.2
002800*                         RELATIVE  I-O DATA FILE-3               RL1054.2
002900*                 X-55   SYSTEM PRINTER                           RL1054.2
003000*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL1054.2
003100*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL1054.2
003200*                 X-75   OBJECT OF VALUE OF CLAUSE FOR FILE-1     RL1054.2
003300*                 X-76   OBJECT OF VALUE OF CLAUSE FOR FILE-2     RL1054.2
003400*                 X-77   OBJECT OF VALUE OF CLAUSE FOR FILE-3     RL1054.2
003500*                 X-82   SOURCE-COMPUTER                          RL1054.2
003600*                 X-83   OBJECT-COMPUTER.                         RL1054.2
003700*                                                                 RL1054.2
003800****************************************************************  RL1054.2
003900 ENVIRONMENT DIVISION.                                            RL1054.2
004000 CONFIGURATION SECTION.                                           RL1054.2
004100 SOURCE-COMPUTER.                                                 RL1054.2
004200     XXXXX082.                                                    RL1054.2
004300 OBJECT-COMPUTER.                                                 RL1054.2
004400     XXXXX083.                                                    RL1054.2
004500 INPUT-OUTPUT SECTION.                                            RL1054.2
004600 FILE-CONTROL.                                                    RL1054.2
004700     SELECT PRINT-FILE ASSIGN TO                                  RL1054.2
004800     XXXXX055.                                                    RL1054.2
004900     SELECT   RL-FR1 ASSIGN TO                                    RL1054.2
005000     XXXXX021                                                     RL1054.2
005100             ORGANIZATION IS RELATIVE                             RL1054.2
005200             ACCESS  MODE IS RANDOM                               RL1054.2
005300              RELATIVE KEY IS KEY-1.                              RL1054.2
005400     SELECT RL-FR2 ASSIGN                                         RL1054.2
005500     XXXXX022                                                     RL1054.2
005600             ORGANIZATION IS RELATIVE                             RL1054.2
005700             ACCESS  MODE IS RANDOM                               RL1054.2
005800              RELATIVE KEY IS KEY-2.                              RL1054.2
005900     SELECT RL-FR3 ASSIGN TO                                      RL1054.2
006000     XXXXX023                                                     RL1054.2
006100             ORGANIZATION IS RELATIVE                             RL1054.2
006200             ACCESS  MODE IS RANDOM                               RL1054.2
006300              RELATIVE KEY IS KEY-3.                              RL1054.2
006400 DATA DIVISION.                                                   RL1054.2
006500 FILE SECTION.                                                    RL1054.2
006600 FD  PRINT-FILE.                                                  RL1054.2
006700 01  PRINT-REC PICTURE X(120).                                    RL1054.2
006800 01  DUMMY-RECORD PICTURE X(120).                                 RL1054.2
006900 FD  RL-FR1                                                       RL1054.2
007000C    VALUE OF                                                     RL1054.2
007100C    XXXXX074                                                     RL1054.2
007200C    IS                                                           RL1054.2
007300C    XXXXX075                                                     RL1054.2
007400*    XXXXX069                                                     RL1054.2
007500     LABEL RECORDS ARE STANDARD                                   RL1054.2
007600     DATA RECORD IS GRP-1SEQ-RECORD-1.                            RL1054.2
007700 01  GRP-1SEQ-RECORD-1.                                           RL1054.2
007800     02  EXPRESSION              PICTURE X(51).                   RL1054.2
007900     02  FILLER                  PICTURE X(49).                   RL1054.2
008000 FD  RL-FR2                                                       RL1054.2
008100C    VALUE OF                                                     RL1054.2
008200C    XXXXX074                                                     RL1054.2
008300C    IS                                                           RL1054.2
008400C    XXXXX076                                                     RL1054.2
008500*    XXXXX069                                                     RL1054.2
008600     LABEL RECORDS ARE STANDARD                                   RL1054.2
008700     DATA RECORD IS GRP-1SEQ-RECORD-2.                            RL1054.2
008800 01  GRP-1SEQ-RECORD-2.                                           RL1054.2
008900     02  FILLER                  PICTURE X(100).                  RL1054.2
009000 FD  RL-FR3                                                       RL1054.2
009100C    VALUE OF                                                     RL1054.2
009200C    XXXXX074                                                     RL1054.2
009300C    IS                                                           RL1054.2
009400C    XXXXX077                                                     RL1054.2
009500*    XXXXX069                                                     RL1054.2
009600     LABEL RECORDS ARE STANDARD                                   RL1054.2
009700     DATA RECORD IS GRP-1SEQ-RECORD-3.                            RL1054.2
009800 01  GRP-1SEQ-RECORD-3.                                           RL1054.2
009900     02  FILLER                  PICTURE X(100).                  RL1054.2
010000 WORKING-STORAGE SECTION.                                         RL1054.2
010100 77  SUB-1             PICTURE 99  VALUE ZERO.                    RL1054.2
010200 77  KEY-1                                                        RL1054.2
010300         PICTURE   9(5).                                          RL1054.2
010400 77  KEY-2                                                        RL1054.2
010500         PICTURE   9(5).                                          RL1054.2
010600 77  KEY-3                                                        RL1054.2
010700         PICTURE   9(5).                                          RL1054.2
010800 01  RECORD-MESSAGE.                                              RL1054.2
010900     02  FILLER                   PICTURE X(8)  VALUE "RECORD  ". RL1054.2
011000     02  2POS-NUM                 PICTURE 99.                     RL1054.2
011100     02  FILLER                   PICTURE X(40)  VALUE            RL1054.2
011200     "  OF THIS FILE CONTAINS THIS INFORMATION".                  RL1054.2
011300     02  FILLER                   PICTURE X(50)  VALUE SPACE.     RL1054.2
011400 01  GRP-SEQ-TEST-RECORD         PICTURE X(100) VALUE SPACE.      RL1054.2
011500 01  GRP-HOLD-RECORD             PICTURE X(100) VALUE SPACE.      RL1054.2
011600 01  READ-WRITE-COUNTER.                                          RL1054.2
011700     02  ENTRY-RW OCCURS 20 TIMES PICTURE X.                      RL1054.2
011800 01  FILE-RECORD-INFORMATION-REC.                                 RL1054.2
011900     03 FILE-RECORD-INFO-SKELETON.                                RL1054.2
012000        05 FILLER                 PICTURE X(48)       VALUE       RL1054.2
012100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1054.2
012200        05 FILLER                 PICTURE X(46)       VALUE       RL1054.2
012300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1054.2
012400        05 FILLER                 PICTURE X(26)       VALUE       RL1054.2
012500             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1054.2
012600        05 FILLER                 PICTURE X(37)       VALUE       RL1054.2
012700             ",RECKEY=                             ".             RL1054.2
012800        05 FILLER                 PICTURE X(38)       VALUE       RL1054.2
012900             ",ALTKEY1=                             ".            RL1054.2
013000        05 FILLER                 PICTURE X(38)       VALUE       RL1054.2
013100             ",ALTKEY2=                             ".            RL1054.2
013200        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1054.2
013300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1054.2
013400        05 FILE-RECORD-INFO-P1-120.                               RL1054.2
013500           07 FILLER              PIC X(5).                       RL1054.2
013600           07 XFILE-NAME           PIC X(6).                      RL1054.2
013700           07 FILLER              PIC X(8).                       RL1054.2
013800           07 XRECORD-NAME         PIC X(6).                      RL1054.2
013900           07 FILLER              PIC X(1).                       RL1054.2
014000           07 REELUNIT-NUMBER     PIC 9(1).                       RL1054.2
014100           07 FILLER              PIC X(7).                       RL1054.2
014200           07 XRECORD-NUMBER       PIC 9(6).                      RL1054.2
014300           07 FILLER              PIC X(6).                       RL1054.2
014400           07 UPDATE-NUMBER       PIC 9(2).                       RL1054.2
014500           07 FILLER              PIC X(5).                       RL1054.2
014600           07 ODO-NUMBER          PIC 9(4).                       RL1054.2
014700           07 FILLER              PIC X(5).                       RL1054.2
014800           07 XPROGRAM-NAME        PIC X(5).                      RL1054.2
014900           07 FILLER              PIC X(7).                       RL1054.2
015000           07 XRECORD-LENGTH       PIC 9(6).                      RL1054.2
015100           07 FILLER              PIC X(7).                       RL1054.2
015200           07 CHARS-OR-RECORDS    PIC X(2).                       RL1054.2
015300           07 FILLER              PIC X(1).                       RL1054.2
015400           07 XBLOCK-SIZE          PIC 9(4).                      RL1054.2
015500           07 FILLER              PIC X(6).                       RL1054.2
015600           07 RECORDS-IN-FILE     PIC 9(6).                       RL1054.2
015700           07 FILLER              PIC X(5).                       RL1054.2
015800           07 XFILE-ORGANIZATION   PIC X(2).                      RL1054.2
015900           07 FILLER              PIC X(6).                       RL1054.2
016000           07 XLABEL-TYPE          PIC X(1).                      RL1054.2
016100        05 FILE-RECORD-INFO-P121-240.                             RL1054.2
016200           07 FILLER              PIC X(8).                       RL1054.2
016300           07 XRECORD-KEY          PIC X(29).                     RL1054.2
016400           07 FILLER              PIC X(9).                       RL1054.2
016500           07 ALTERNATE-KEY1      PIC X(29).                      RL1054.2
016600           07 FILLER              PIC X(9).                       RL1054.2
016700           07 ALTERNATE-KEY2      PIC X(29).                      RL1054.2
016800           07 FILLER              PIC X(7).                       RL1054.2
016900 01  TEST-RESULTS.                                                RL1054.2
017000     02 FILLER                   PIC X      VALUE SPACE.          RL1054.2
017100     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1054.2
017200     02 FILLER                   PIC X      VALUE SPACE.          RL1054.2
017300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1054.2
017400     02 FILLER                   PIC X      VALUE SPACE.          RL1054.2
017500     02  PAR-NAME.                                                RL1054.2
017600       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1054.2
017700       03  PARDOT-X              PIC X      VALUE SPACE.          RL1054.2
017800       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1054.2
017900     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1054.2
018000     02 RE-MARK                  PIC X(61).                       RL1054.2
018100 01  TEST-COMPUTED.                                               RL1054.2
018200     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1054.2
018300     02 FILLER                   PIC X(17)  VALUE                 RL1054.2
018400            "       COMPUTED=".                                   RL1054.2
018500     02 COMPUTED-X.                                               RL1054.2
018600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1054.2
018700     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1054.2
018800                                 PIC -9(9).9(9).                  RL1054.2
018900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1054.2
019000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1054.2
019100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1054.2
019200     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1054.2
019300         04 COMPUTED-18V0                    PIC -9(18).          RL1054.2
019400         04 FILLER                           PIC X.               RL1054.2
019500     03 FILLER PIC X(50) VALUE SPACE.                             RL1054.2
019600 01  TEST-CORRECT.                                                RL1054.2
019700     02 FILLER PIC X(30) VALUE SPACE.                             RL1054.2
019800     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1054.2
019900     02 CORRECT-X.                                                RL1054.2
020000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1054.2
020100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1054.2
020200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1054.2
020300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1054.2
020400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1054.2
020500     03      CR-18V0 REDEFINES CORRECT-A.                         RL1054.2
020600         04 CORRECT-18V0                     PIC -9(18).          RL1054.2
020700         04 FILLER                           PIC X.               RL1054.2
020800     03 FILLER PIC X(2) VALUE SPACE.                              RL1054.2
020900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1054.2
021000 01  CCVS-C-1.                                                    RL1054.2
021100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1054.2
021200-    "SS  PARAGRAPH-NAME                                          RL1054.2
021300-    "       REMARKS".                                            RL1054.2
021400     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1054.2
021500 01  CCVS-C-2.                                                    RL1054.2
021600     02 FILLER                     PIC X        VALUE SPACE.      RL1054.2
021700     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1054.2
021800     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1054.2
021900     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1054.2
022000     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1054.2
022100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1054.2
022200 01  REC-CT                        PIC 99       VALUE ZERO.       RL1054.2
022300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1054.2
022400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1054.2
022500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1054.2
022600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1054.2
022700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1054.2
022800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1054.2
022900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1054.2
023000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1054.2
023100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1054.2
023200 01  CCVS-H-1.                                                    RL1054.2
023300     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1054.2
023400     02  FILLER                    PIC X(42)    VALUE             RL1054.2
023500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1054.2
023600     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1054.2
023700 01  CCVS-H-2A.                                                   RL1054.2
023800   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1054.2
023900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1054.2
024000   02  FILLER                        PIC XXXX   VALUE             RL1054.2
024100     "4.2 ".                                                      RL1054.2
024200   02  FILLER                        PIC X(28)  VALUE             RL1054.2
024300            " COPY - NOT FOR DISTRIBUTION".                       RL1054.2
024400   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1054.2
024500                                                                  RL1054.2
024600 01  CCVS-H-2B.                                                   RL1054.2
024700   02  FILLER                        PIC X(15)  VALUE             RL1054.2
024800            "TEST RESULT OF ".                                    RL1054.2
024900   02  TEST-ID                       PIC X(9).                    RL1054.2
025000   02  FILLER                        PIC X(4)   VALUE             RL1054.2
025100            " IN ".                                               RL1054.2
025200   02  FILLER                        PIC X(12)  VALUE             RL1054.2
025300     " HIGH       ".                                              RL1054.2
025400   02  FILLER                        PIC X(22)  VALUE             RL1054.2
025500            " LEVEL VALIDATION FOR ".                             RL1054.2
025600   02  FILLER                        PIC X(58)  VALUE             RL1054.2
025700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1054.2
025800 01  CCVS-H-3.                                                    RL1054.2
025900     02  FILLER                      PIC X(34)  VALUE             RL1054.2
026000            " FOR OFFICIAL USE ONLY    ".                         RL1054.2
026100     02  FILLER                      PIC X(58)  VALUE             RL1054.2
026200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1054.2
026300     02  FILLER                      PIC X(28)  VALUE             RL1054.2
026400            "  COPYRIGHT   1985 ".                                RL1054.2
026500 01  CCVS-E-1.                                                    RL1054.2
026600     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1054.2
026700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1054.2
026800     02 ID-AGAIN                     PIC X(9).                    RL1054.2
026900     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1054.2
027000 01  CCVS-E-2.                                                    RL1054.2
027100     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1054.2
027200     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1054.2
027300     02 CCVS-E-2-2.                                               RL1054.2
027400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1054.2
027500         03 FILLER                   PIC X      VALUE SPACE.      RL1054.2
027600         03 ENDER-DESC               PIC X(44)  VALUE             RL1054.2
027700            "ERRORS ENCOUNTERED".                                 RL1054.2
027800 01  CCVS-E-3.                                                    RL1054.2
027900     02  FILLER                      PIC X(22)  VALUE             RL1054.2
028000            " FOR OFFICIAL USE ONLY".                             RL1054.2
028100     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1054.2
028200     02  FILLER                      PIC X(58)  VALUE             RL1054.2
028300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1054.2
028400     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1054.2
028500     02 FILLER                       PIC X(15)  VALUE             RL1054.2
028600             " COPYRIGHT 1985".                                   RL1054.2
028700 01  CCVS-E-4.                                                    RL1054.2
028800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1054.2
028900     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1054.2
029000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1054.2
029100     02 FILLER                       PIC X(40)  VALUE             RL1054.2
029200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1054.2
029300 01  XXINFO.                                                      RL1054.2
029400     02 FILLER                       PIC X(19)  VALUE             RL1054.2
029500            "*** INFORMATION ***".                                RL1054.2
029600     02 INFO-TEXT.                                                RL1054.2
029700       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1054.2
029800       04 XXCOMPUTED                 PIC X(20).                   RL1054.2
029900       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1054.2
030000       04 XXCORRECT                  PIC X(20).                   RL1054.2
030100     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1054.2
030200 01  HYPHEN-LINE.                                                 RL1054.2
030300     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1054.2
030400     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1054.2
030500-    "*****************************************".                 RL1054.2
030600     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1054.2
030700-    "******************************".                            RL1054.2
030800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1054.2
030900     "RL105A".                                                    RL1054.2
031000 PROCEDURE DIVISION.                                              RL1054.2
031100 CCVS1 SECTION.                                                   RL1054.2
031200 OPEN-FILES.                                                      RL1054.2
031300     OPEN    OUTPUT PRINT-FILE.                                   RL1054.2
031400     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1054.2
031500     MOVE    SPACE TO TEST-RESULTS.                               RL1054.2
031600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1054.2
031700     MOVE    ZERO TO REC-SKL-SUB.                                 RL1054.2
031800     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1054.2
031900 CCVS-INIT-FILE.                                                  RL1054.2
032000     ADD     1 TO REC-SKL-SUB.                                    RL1054.2
032100     MOVE    FILE-RECORD-INFO-SKELETON                            RL1054.2
032200          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1054.2
032300 CCVS-INIT-EXIT.                                                  RL1054.2
032400     GO TO CCVS1-EXIT.                                            RL1054.2
032500 CLOSE-FILES.                                                     RL1054.2
032600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1054.2
032700 TERMINATE-CCVS.                                                  RL1054.2
032800*    EXIT PROGRAM.                                                RL1054.2
032900*TERMINATE-CALL.                                                  RL1054.2
033000     STOP     RUN.                                                RL1054.2
033100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1054.2
033200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1054.2
033300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1054.2
033400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1054.2
033500     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1054.2
033600 PRINT-DETAIL.                                                    RL1054.2
033700     IF REC-CT NOT EQUAL TO ZERO                                  RL1054.2
033800             MOVE "." TO PARDOT-X                                 RL1054.2
033900             MOVE REC-CT TO DOTVALUE.                             RL1054.2
034000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1054.2
034100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1054.2
034200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1054.2
034300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1054.2
034400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1054.2
034500     MOVE SPACE TO CORRECT-X.                                     RL1054.2
034600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1054.2
034700     MOVE     SPACE TO RE-MARK.                                   RL1054.2
034800 HEAD-ROUTINE.                                                    RL1054.2
034900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1054.2
035000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1054.2
035100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1054.2
035200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1054.2
035300 COLUMN-NAMES-ROUTINE.                                            RL1054.2
035400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1054.2
035500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1054.2
035600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1054.2
035700 END-ROUTINE.                                                     RL1054.2
035800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1054.2
035900 END-RTN-EXIT.                                                    RL1054.2
036000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1054.2
036100 END-ROUTINE-1.                                                   RL1054.2
036200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1054.2
036300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1054.2
036400      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1054.2
036500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1054.2
036600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1054.2
036700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1054.2
036800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1054.2
036900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1054.2
037000  END-ROUTINE-12.                                                 RL1054.2
037100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1054.2
037200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1054.2
037300         MOVE "NO " TO ERROR-TOTAL                                RL1054.2
037400         ELSE                                                     RL1054.2
037500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1054.2
037600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1054.2
037700     PERFORM WRITE-LINE.                                          RL1054.2
037800 END-ROUTINE-13.                                                  RL1054.2
037900     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1054.2
038000         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1054.2
038100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1054.2
038200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1054.2
038300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1054.2
038400      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1054.2
038500          MOVE "NO " TO ERROR-TOTAL                               RL1054.2
038600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1054.2
038700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1054.2
038800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1054.2
038900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1054.2
039000 WRITE-LINE.                                                      RL1054.2
039100     ADD 1 TO RECORD-COUNT.                                       RL1054.2
039200*    IF RECORD-COUNT GREATER 50                                   RL1054.2
039300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1054.2
039400*        MOVE SPACE TO DUMMY-RECORD                               RL1054.2
039500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1054.2
039600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1054.2
039700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1054.2
039800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1054.2
039900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1054.2
040000*        MOVE ZERO TO RECORD-COUNT.                               RL1054.2
040100     PERFORM WRT-LN.                                              RL1054.2
040200 WRT-LN.                                                          RL1054.2
040300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1054.2
040400     MOVE SPACE TO DUMMY-RECORD.                                  RL1054.2
040500 BLANK-LINE-PRINT.                                                RL1054.2
040600     PERFORM WRT-LN.                                              RL1054.2
040700 FAIL-ROUTINE.                                                    RL1054.2
040800     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1054.2
040900            GO TO   FAIL-ROUTINE-WRITE.                           RL1054.2
041000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1054.2
041100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1054.2
041200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1054.2
041300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1054.2
041400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1054.2
041500     GO TO  FAIL-ROUTINE-EX.                                      RL1054.2
041600 FAIL-ROUTINE-WRITE.                                              RL1054.2
041700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1054.2
041800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1054.2
041900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1054.2
042000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1054.2
042100 FAIL-ROUTINE-EX. EXIT.                                           RL1054.2
042200 BAIL-OUT.                                                        RL1054.2
042300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1054.2
042400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1054.2
042500 BAIL-OUT-WRITE.                                                  RL1054.2
042600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1054.2
042700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1054.2
042800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1054.2
042900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1054.2
043000 BAIL-OUT-EX. EXIT.                                               RL1054.2
043100 CCVS1-EXIT.                                                      RL1054.2
043200     EXIT.                                                        RL1054.2
043300 SECT-RL105-001 SECTION.                                          RL1054.2
043400 SECT-RC-01-001-INIT.                                             RL1054.2
043500     MOVE 1 TO KEY-1  KEY-2  KEY-3  2POS-NUM.                     RL1054.2
043600     MOVE "READ/WRITE INVAL KEY" TO FEATURE.                      RL1054.2
043700 WRITE-REL-RECORDS SECTION.                                       RL1054.2
043800 REL-INIT-1.                                                      RL1054.2
043900     MOVE ZERO TO 2POS-NUM.                                       RL1054.2
044000     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1054.2
044100     OPEN OUTPUT RL-FR1.                                          RL1054.2
044200 REL-TEST-1.                                                      RL1054.2
044300     PERFORM REL-WRITE-FOR-TEST-1 THRU 1-EXIT 19 TIMES.           RL1054.2
044400     IF READ-WRITE-COUNTER EQUAL TO "WWWWWWWWWWWWWWWWWWW "        RL1054.2
044500         MOVE "19 RECORDS PASSED TO TEST-2" TO RE-MARK            RL1054.2
044600         GO TO REL-TEST-1-WRITE.                                  RL1054.2
044700     MOVE "WWWWWWWWWWWWWWWWWWW " TO CORRECT-A.                    RL1054.2
044800     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1054.2
044900     PERFORM FAIL.                                                RL1054.2
045000     GO TO REL-TEST-1-WRITE.                                      RL1054.2
045100 REL-DELETE-TEST-1.                                               RL1054.2
045200     MOVE "***TESTS-DELETED ***" TO COMPUTED-A  CORRECT-A.        RL1054.2
045300     MOVE "WRITE-REL-RECORDS SECTION " TO RE-MARK.                RL1054.2
045400     PERFORM REL-TEST-1-WRITE.                                    RL1054.2
045500     GO TO EXIT-SECTION-1.                                        RL1054.2
045600 REL-WRITE-FOR-TEST-1.                                            RL1054.2
045700     ADD 1 TO SUB-1.                                              RL1054.2
045800     MOVE "W" TO ENTRY-RW (SUB-1).                                RL1054.2
045900     MOVE SUB-1 TO KEY-1.                                         RL1054.2
046000     ADD 1 TO 2POS-NUM.                                           RL1054.2
046100     MOVE RECORD-MESSAGE TO GRP-1SEQ-RECORD-1.                    RL1054.2
046200     WRITE GRP-1SEQ-RECORD-1 INVALID KEY GO TO I-KEY-1.           RL1054.2
046300     GO TO 1-EXIT.                                                RL1054.2
046400 I-KEY-1.                                                         RL1054.2
046500     MOVE "I" TO ENTRY-RW (SUB-1).                                RL1054.2
046600 1-EXIT.                                                          RL1054.2
046700     EXIT.                                                        RL1054.2
046800 REL-TEST-1-WRITE.                                                RL1054.2
046900     MOVE "REL-TEST-1         " TO PAR-NAME.                      RL1054.2
047000     PERFORM PRINT-DETAIL.                                        RL1054.2
047100     CLOSE RL-FR1.                                                RL1054.2
047200 REL-INIT-2.                                                      RL1054.2
047300     MOVE 01 TO SUB-1  KEY-1.                                     RL1054.2
047400     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1054.2
047500     OPEN INPUT RL-FR1.                                           RL1054.2
047600 REL-TEST-2.                                                      RL1054.2
047700     READ RL-FR1 RECORD INVALID                                   RL1054.2
047800         MOVE "E" TO ENTRY-RW (SUB-1)                             RL1054.2
047900         GO TO COMPARE-FOR-TEST-2.                                RL1054.2
048000     IF SUB-1 EQUAL TO 20                                         RL1054.2
048100         GO TO COMPARE-FOR-TEST-2.                                RL1054.2
048200     MOVE "R" TO ENTRY-RW (SUB-1).                                RL1054.2
048300     ADD 1 TO SUB-1.                                              RL1054.2
048400     MOVE SUB-1 TO KEY-1.                                         RL1054.2
048500     GO TO REL-TEST-2.                                            RL1054.2
048600 COMPARE-FOR-TEST-2.                                              RL1054.2
048700     IF READ-WRITE-COUNTER EQUAL TO "RRRRRRRRRRRRRRRRRRRE"        RL1054.2
048800         PERFORM PASS                                             RL1054.2
048900         GO TO REL-TEST-2-WRITE.                                  RL1054.2
049000     MOVE "RRRRRRRRRRRRRRRRRRRE" TO CORRECT-A.                    RL1054.2
049100     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1054.2
049200     MOVE "INCORRECT RECORD COUNT     " TO RE-MARK.               RL1054.2
049300     PERFORM FAIL.                                                RL1054.2
049400 REL-TEST-2-WRITE.                                                RL1054.2
049500     MOVE "REL-TEST-2         " TO PAR-NAME.                      RL1054.2
049600     PERFORM PRINT-DETAIL.                                        RL1054.2
049700 EXIT-SECTION-1.                                                  RL1054.2
049800     CLOSE RL-FR1.                                                RL1054.2
049900 BLOCKED-UNBLOCKED SECTION.                                       RL1054.2
050000 REL-INIT-3.                                                      RL1054.2
050100     MOVE 0 TO SUB-1.                                             RL1054.2
050200     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1054.2
050300     OPEN OUTPUT RL-FR2.                                          RL1054.2
050400     MOVE 1 TO KEY-2  2POS-NUM.                                   RL1054.2
050500 REL-TEST-3.                                                      RL1054.2
050600     PERFORM REL-WRITE-FOR-TEST-3 THRU 3-EXIT 8 TIMES.            RL1054.2
050700     PERFORM REL-WRITE-FOR-TEST-3 THRU 3-EXIT 8 TIMES.            RL1054.2
050800     CLOSE RL-FR2.                                                RL1054.2
050900     MOVE "REL-TEST-3         " TO PAR-NAME.                      RL1054.2
051000     IF READ-WRITE-COUNTER NOT EQUAL TO "WWWWWWWWWWWWWWWW    "    RL1054.2
051100         MOVE "INVALID KEYS ON WRITE " TO RE-MARK ELSE            RL1054.2
051200     MOVE "16 RECORDS PASSED TO TEST-4" TO RE-MARK.               RL1054.2
051300     MOVE SPACE TO CORRECT-A  COMPUTED-A.                         RL1054.2
051400     PERFORM PRINT-DETAIL.                                        RL1054.2
051500     GO TO REL-TEST-4.                                            RL1054.2
051600 REL-DELETE-TEST-3.                                               RL1054.2
051700     MOVE "***TESTS-DELETED ***" TO CORRECT-A COMPUTED-A.         RL1054.2
051800     MOVE "BLOCKED-UNBLOCKED SECTION  " TO RE-MARK.               RL1054.2
051900     MOVE "REL-TEST-3         " TO PAR-NAME.                      RL1054.2
052000     PERFORM PRINT-DETAIL.                                        RL1054.2
052100     GO TO REL-TEST-4.                                            RL1054.2
052200 REL-WRITE-FOR-TEST-3.                                            RL1054.2
052300     MOVE RECORD-MESSAGE TO GRP-1SEQ-RECORD-2.                    RL1054.2
052400     WRITE GRP-1SEQ-RECORD-2 INVALID KEY GO TO I-KEY-3.           RL1054.2
052500     MOVE "W" TO ENTRY-RW (2POS-NUM).                             RL1054.2
052600     ADD 1 TO 2POS-NUM.                                           RL1054.2
052700     MOVE 2POS-NUM TO KEY-2.                                      RL1054.2
052800     GO TO 3-EXIT.                                                RL1054.2
052900 I-KEY-3.                                                         RL1054.2
053000     MOVE "I" TO ENTRY-RW (2POS-NUM).                             RL1054.2
053100     ADD 1 TO 2POS-NUM.                                           RL1054.2
053200     MOVE 2POS-NUM TO KEY-2.                                      RL1054.2
053300 3-EXIT.                                                          RL1054.2
053400     EXIT.                                                        RL1054.2
053500 REL-TEST-4.                                                      RL1054.2
053600     OPEN INPUT RL-FR2.                                           RL1054.2
053700     MOVE 01 TO 2POS-NUM.                                         RL1054.2
053800     MOVE 01 TO SUB-1  KEY-2.                                     RL1054.2
053900     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1054.2
054000 READ-FOR-TEST-4.                                                 RL1054.2
054100     READ RL-FR2 INVALID                                          RL1054.2
054200         MOVE "INVALID KEY FILE-2  " TO COMPUTED-A                RL1054.2
054300         MOVE SPACE TO CORRECT-A                                  RL1054.2
054400         MOVE "INVALID KEY ON FIRST READ  " TO RE-MARK            RL1054.2
054500         PERFORM FAIL                                             RL1054.2
054600         MOVE "I" TO ENTRY-RW (SUB-1)                             RL1054.2
054700         GO TO REL-TEST-4-WRITE.                                  RL1054.2
054800     IF GRP-1SEQ-RECORD-2 NOT EQUAL TO RECORD-MESSAGE             RL1054.2
054900         MOVE "INCORRECT 1ST RECORD" TO COMPUTED-A                RL1054.2
055000         MOVE SPACE TO CORRECT-A                                  RL1054.2
055100         MOVE "RECORD NO. 1 INVALID" TO RE-MARK                   RL1054.2
055200         PERFORM FAIL                                             RL1054.2
055300         MOVE "R" TO ENTRY-RW (SUB-1)                             RL1054.2
055400         GO TO REL-TEST-4-WRITE.                                  RL1054.2
055500     MOVE "R" TO ENTRY-RW (SUB-1).                                RL1054.2
055600     PERFORM PASS.                                                RL1054.2
055700 REL-TEST-4-WRITE.                                                RL1054.2
055800     MOVE "REL-TEST-4         " TO PAR-NAME.                      RL1054.2
055900     PERFORM PRINT-DETAIL.                                        RL1054.2
056000 REL-TEST-5.                                                      RL1054.2
056100     ADD 1 TO SUB-1.                                              RL1054.2
056200*    NOTE  THIS TEST DEPENDS ON TEST-4.                           RL1054.2
056300     MOVE SUB-1 TO KEY-2.                                         RL1054.2
056400     READ RL-FR2 INVALID KEY                                      RL1054.2
056500         MOVE "E" TO ENTRY-RW (SUB-1)                             RL1054.2
056600         GO TO COMPARE-FOR-TEST-5.                                RL1054.2
056700     IF SUB-1 EQUAL TO 17                                         RL1054.2
056800         GO TO COMPARE-FOR-TEST-5.                                RL1054.2
056900     MOVE "R" TO ENTRY-RW (SUB-1).                                RL1054.2
057000     GO TO REL-TEST-5.                                            RL1054.2
057100 COMPARE-FOR-TEST-5.                                              RL1054.2
057200     IF READ-WRITE-COUNTER EQUAL TO "RRRRRRRRRRRRRRRRE   "        RL1054.2
057300         PERFORM PASS                                             RL1054.2
057400         GO TO REL-TEST-5-WRITE.                                  RL1054.2
057500     MOVE "RRRRRRRRRRRRRRRRE   " TO CORRECT-A.                    RL1054.2
057600     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1054.2
057700     MOVE "INCORRECT RECORD COUNT     " TO RE-MARK.               RL1054.2
057800     PERFORM FAIL.                                                RL1054.2
057900 REL-TEST-5-WRITE.                                                RL1054.2
058000     MOVE "REL-TEST-5         " TO PAR-NAME.                      RL1054.2
058100     PERFORM PRINT-DETAIL.                                        RL1054.2
058200     CLOSE RL-FR2.                                                RL1054.2
058300     OPEN OUTPUT RL-FR3.                                          RL1054.2
058400     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1054.2
058500     MOVE 1 TO KEY-3  2POS-NUM.                                   RL1054.2
058600 REL-TEST-6.                                                      RL1054.2
058700     PERFORM REL-WRITE-FOR-TEST-6 THRU 6-EXIT 7 TIMES.            RL1054.2
058800     PERFORM REL-WRITE-FOR-TEST-6 THRU 6-EXIT 9 TIMES.            RL1054.2
058900     IF READ-WRITE-COUNTER NOT EQUAL TO "WWWWWWWWWWWWWWWW    "    RL1054.2
059000         MOVE "INVALID KEYS ON WRITE " TO RE-MARK ELSE            RL1054.2
059100     MOVE "16 RECORDS PASSED TO TEST-7" TO RE-MARK.               RL1054.2
059200     MOVE SPACE TO CORRECT-A  COMPUTED-A.                         RL1054.2
059300     MOVE "REL-TEST-6         " TO PAR-NAME.                      RL1054.2
059400     PERFORM PRINT-DETAIL.                                        RL1054.2
059500     CLOSE RL-FR3.                                                RL1054.2
059600     GO TO REL-TEST-7.                                            RL1054.2
059700 REL-WRITE-FOR-TEST-6.                                            RL1054.2
059800     MOVE RECORD-MESSAGE TO GRP-1SEQ-RECORD-3.                    RL1054.2
059900     WRITE GRP-1SEQ-RECORD-3 INVALID KEY GO TO I-KEY-6.           RL1054.2
060000     MOVE "W" TO ENTRY-RW (2POS-NUM).                             RL1054.2
060100     ADD 1 TO 2POS-NUM.                                           RL1054.2
060200     MOVE 2POS-NUM TO KEY-3.                                      RL1054.2
060300     GO TO 6-EXIT.                                                RL1054.2
060400 I-KEY-6.                                                         RL1054.2
060500     MOVE "I" TO ENTRY-RW (2POS-NUM).                             RL1054.2
060600     ADD 1 TO 2POS-NUM.                                           RL1054.2
060700     MOVE 2POS-NUM TO KEY-3.                                      RL1054.2
060800 6-EXIT.                                                          RL1054.2
060900     EXIT.                                                        RL1054.2
061000 REL-TEST-7.                                                      RL1054.2
061100     OPEN INPUT RL-FR3.                                           RL1054.2
061200     MOVE 08 TO SUB-1  KEY-3  2POS-NUM.                           RL1054.2
061300     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1054.2
061400 READ-FOR-TEST-7.                                                 RL1054.2
061500     READ RL-FR3 INVALID KEY                                      RL1054.2
061600         MOVE "INVALID KEY ON FILE3" TO COMPUTED-A                RL1054.2
061700         MOVE SPACE TO CORRECT-A                                  RL1054.2
061800         PERFORM FAIL                                             RL1054.2
061900         GO TO REL-TEST-7-WRITE.                                  RL1054.2
062000     IF GRP-1SEQ-RECORD-3 NOT EQUAL TO RECORD-MESSAGE             RL1054.2
062100         MOVE GRP-1SEQ-RECORD-3 TO COMPUTED-A                     RL1054.2
062200         MOVE "8TH RECORD MESSAGE" TO CORRECT-A                   RL1054.2
062300         MOVE "COMPUTED-A SHOWS 1ST 20 POS" TO RE-MARK            RL1054.2
062400         PERFORM FAIL                                             RL1054.2
062500         GO TO REL-TEST-7-WRITE.                                  RL1054.2
062600     PERFORM PASS.                                                RL1054.2
062700 REL-TEST-7-WRITE.                                                RL1054.2
062800     MOVE "REL-TEST-7         " TO PAR-NAME.                      RL1054.2
062900     PERFORM PRINT-DETAIL.                                        RL1054.2
063000     CLOSE RL-FR3.                                                RL1054.2
063100 CCVS-EXIT SECTION.                                               RL1054.2
063200 CCVS-999999.                                                     RL1054.2
063300     GO TO CLOSE-FILES.                                           RL1054.2
      *END-OF,RL105A                                                                  
