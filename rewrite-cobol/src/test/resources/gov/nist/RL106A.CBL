      *HEADER,COBOL,RL106A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1064.2
000200 PROGRAM-ID.                                                      RL1064.2
000300     RL106A.                                                      RL1064.2
000400****************************************************************  RL1064.2
000500*                                                              *  RL1064.2
000600*    VALIDATION FOR:-                                          *  RL1064.2
000700*                                                              *  RL1064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1064.2
000900*                                                              *  RL1064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1064.2
001100*                                                              *  RL1064.2
001200****************************************************************  RL1064.2
001300*GENERAL:    THIS PROGRAM PROCESSES THREE RLEATIVE I-O FILES      RL1064.2
001400*            IDENTIFIED AS RL-FR4,RL-FR5 AND RL-FR6.  THE FUNCTIONRL1064.2
001500*            OF THIS PROGRAM IS TO CREATE THREE RELATIVE FILES    RL1064.2
001600*            RANDOMLLY (ACCESS MODE RANDOM) AND VERIFY THAT THEY  RL1064.2
001700*            WERE CREATED CORRECTLY.  THE FILES PROCESSED         RL1064.2
001800*            CONTAIN VARIABLE LENGTH RECORDS.                     RL1064.2
001900*                                                                 RL1064.2
002000*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL1064.2
002100*            PROGRAM ARE:                                         RL1064.2
002200*                                                                 RL1064.2
002300*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1064.2
002400*                         RELATIVE  I-O DATA FILE-1               RL1064.2
002500*                 X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1064.2
002600*                         RELATIVE  I-O DATA FILE-2               RL1064.2
002700*                 X-23   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1064.2
002800*                         RELATIVE  I-O DATA FILE-3               RL1064.2
002900*                 X-55   SYSTEM PRINTER                           RL1064.2
003000*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL1064.2
003100*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL1064.2
003200*                 X-75   OBJECT OF VALUE OF CLAUSE FOR FILE-1     RL1064.2
003300*                 X-76   OBJECT OF VALUE OF CLAUSE FOR FILE-2     RL1064.2
003400*                 X-77   OBJECT OF VALUE OF CLAUSE FOR FILE-3     RL1064.2
003500*                 X-82   SOURCE-COMPUTER                          RL1064.2
003600*                 X-83   OBJECT-COMPUTER.                         RL1064.2
003700*                                                                 RL1064.2
003800****************************************************************  RL1064.2
003900 ENVIRONMENT DIVISION.                                            RL1064.2
004000 CONFIGURATION SECTION.                                           RL1064.2
004100 SOURCE-COMPUTER.                                                 RL1064.2
004200     XXXXX082.                                                    RL1064.2
004300 OBJECT-COMPUTER.                                                 RL1064.2
004400     XXXXX083.                                                    RL1064.2
004500 INPUT-OUTPUT SECTION.                                            RL1064.2
004600 FILE-CONTROL.                                                    RL1064.2
004700     SELECT PRINT-FILE ASSIGN TO                                  RL1064.2
004800     XXXXX055.                                                    RL1064.2
004900     SELECT RL-FR4 ASSIGN TO                                      RL1064.2
005000     XXXXX021                                                     RL1064.2
005100             ORGANIZATION IS RELATIVE                             RL1064.2
005200             ACCESS  MODE IS RANDOM                               RL1064.2
005300              RELATIVE KEY IS KEY-1.                              RL1064.2
005400     SELECT RL-FR5 ASSIGN TO                                      RL1064.2
005500     XXXXX022                                                     RL1064.2
005600             ORGANIZATION IS RELATIVE                             RL1064.2
005700             ACCESS  MODE IS RANDOM                               RL1064.2
005800              RELATIVE KEY IS KEY-2.                              RL1064.2
005900     SELECT RL-FR6 ASSIGN TO                                      RL1064.2
006000     XXXXX023                                                     RL1064.2
006100             ORGANIZATION IS RELATIVE                             RL1064.2
006200             ACCESS  MODE IS RANDOM                               RL1064.2
006300              RELATIVE KEY IS KEY-3.                              RL1064.2
006400 I-O-CONTROL.                                                     RL1064.2
006500     SAME RL-FR5 RL-FR6.                                          RL1064.2
006600 DATA DIVISION.                                                   RL1064.2
006700 FILE SECTION.                                                    RL1064.2
006800 FD  PRINT-FILE.                                                  RL1064.2
006900 01  PRINT-REC PICTURE X(120).                                    RL1064.2
007000 01  DUMMY-RECORD PICTURE X(120).                                 RL1064.2
007100 FD  RL-FR4                                                       RL1064.2
007200C    VALUE OF                                                     RL1064.2
007300C    XXXXX074                                                     RL1064.2
007400C    IS                                                           RL1064.2
007500C    XXXXX075                                                     RL1064.2
007600*    XXXXX069                                                     RL1064.2
007700     LABEL RECORDS ARE STANDARD                                   RL1064.2
007800     DATA RECORDS ARE GRP-1SEQ-RECORD-4A  GRP-1SEQ-RECORD-4B.     RL1064.2
007900 01  GRP-1SEQ-RECORD-4A.                                          RL1064.2
008000     02  FILLER-4A               PICTURE X(56).                   RL1064.2
008100 01  GRP-1SEQ-RECORD-4B.                                          RL1064.2
008200     02  FILLER-4B               PICTURE X(56).                   RL1064.2
008300     02  LONG-REC-4B.                                             RL1064.2
008400         03  FILLER              PICTURE X(15).                   RL1064.2
008500         03  REC-NUMBER-4B       PIC XX.                          RL1064.2
008600         03  FILLER              PICTURE X(27).                   RL1064.2
008700 FD  RL-FR5                                                       RL1064.2
008800     RECORD CONTAINS 56 TO 101 CHARACTERS                         RL1064.2
008900C    VALUE OF                                                     RL1064.2
009000C    XXXXX074                                                     RL1064.2
009100C    IS                                                           RL1064.2
009200C    XXXXX076                                                     RL1064.2
009300*    XXXXX069                                                     RL1064.2
009400     LABEL RECORDS ARE STANDARD                                   RL1064.2
009500     DATA RECORDS  GRP-1SEQ-RECORD-5A  GRP-1SEQ-RECORD-5B.        RL1064.2
009600 01  GRP-1SEQ-RECORD-5A.                                          RL1064.2
009700     02  FILLER-5A               PICTURE X(56).                   RL1064.2
009800 01  GRP-1SEQ-RECORD-5B.                                          RL1064.2
009900     02  FILLER-5B               PICTURE X(56).                   RL1064.2
010000     02  LONG-REC-5B.                                             RL1064.2
010100         03  FILLER              PICTURE X(15).                   RL1064.2
010200         03  REC-NUMBER-5B       PIC XX.                          RL1064.2
010300         03  FILLER              PICTURE X(28).                   RL1064.2
010400 FD  RL-FR6                                                       RL1064.2
010500     BLOCK 3 RECORDS                                              RL1064.2
010600     RECORD CONTAINS 56 TO 102 CHARACTERS                         RL1064.2
010700     LABEL RECORD STANDARD                                        RL1064.2
010800C    VALUE OF                                                     RL1064.2
010900C    XXXXX074                                                     RL1064.2
011000C    IS                                                           RL1064.2
011100C    XXXXX077                                                     RL1064.2
011200     DATA RECORD  GRP-1SEQ-RECORD-6A  GRP-1SEQ-RECORD-6B.         RL1064.2
011300 01  GRP-1SEQ-RECORD-6A.                                          RL1064.2
011400     02  FILLER-6A               PICTURE X(56).                   RL1064.2
011500 01  GRP-1SEQ-RECORD-6B.                                          RL1064.2
011600     02  FILLER-6B               PICTURE X(56).                   RL1064.2
011700     02  LONG-REC-6B.                                             RL1064.2
011800         03  FILLER              PICTURE X(15).                   RL1064.2
011900         03  REC-NUMBER-6B       PIC XX.                          RL1064.2
012000         03  FILLER              PICTURE X(29).                   RL1064.2
012100 WORKING-STORAGE SECTION.                                         RL1064.2
012200 77  SUB-1                        PICTURE 99.                     RL1064.2
012300 77  KEY-1                                                        RL1064.2
012400         PICTURE   9(5).                                          RL1064.2
012500 77  KEY-2                                                        RL1064.2
012600         PICTURE   9(5).                                          RL1064.2
012700 77  KEY-3                                                        RL1064.2
012800         PICTURE   9(5).                                          RL1064.2
012900 01  READ-WRITE-COUNTER.                                          RL1064.2
013000     02  ENTRY-RW OCCURS 20 TIMES  PICTURE X.                     RL1064.2
013100 01  RECORD-BUILD.                                                RL1064.2
013200     02  FILLER                  PICTURE X(27)  VALUE             RL1064.2
013300     " TYPE OF RECORD WRITTEN IS ".                               RL1064.2
013400     02  RECORD-LONG-OR-SHORT    PICTURE X(5) VALUE "SHORT".      RL1064.2
013500     02  FILLER                  PICTURE X(24) VALUE SPACE.       RL1064.2
013600     02  RECORD-LONG-ONLY.                                        RL1064.2
013700         03  FILLER              PICTURE X(15)  VALUE             RL1064.2
013800         " RECORD NUMBER ".                                       RL1064.2
013900         03  POS-NUM2            PICTURE 99.                      RL1064.2
014000         03  FILLER-LONG         PICTURE X(29)  VALUE             RL1064.2
014100         " AREA USED FOR LONG RECORD ".                           RL1064.2
014200 01  FILE-RECORD-INFORMATION-REC.                                 RL1064.2
014300     03 FILE-RECORD-INFO-SKELETON.                                RL1064.2
014400        05 FILLER                 PICTURE X(48)       VALUE       RL1064.2
014500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1064.2
014600        05 FILLER                 PICTURE X(46)       VALUE       RL1064.2
014700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1064.2
014800        05 FILLER                 PICTURE X(26)       VALUE       RL1064.2
014900             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1064.2
015000        05 FILLER                 PICTURE X(37)       VALUE       RL1064.2
015100             ",RECKEY=                             ".             RL1064.2
015200        05 FILLER                 PICTURE X(38)       VALUE       RL1064.2
015300             ",ALTKEY1=                             ".            RL1064.2
015400        05 FILLER                 PICTURE X(38)       VALUE       RL1064.2
015500             ",ALTKEY2=                             ".            RL1064.2
015600        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1064.2
015700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1064.2
015800        05 FILE-RECORD-INFO-P1-120.                               RL1064.2
015900           07 FILLER              PIC X(5).                       RL1064.2
016000           07 XFILE-NAME           PIC X(6).                      RL1064.2
016100           07 FILLER              PIC X(8).                       RL1064.2
016200           07 XRECORD-NAME         PIC X(6).                      RL1064.2
016300           07 FILLER              PIC X(1).                       RL1064.2
016400           07 REELUNIT-NUMBER     PIC 9(1).                       RL1064.2
016500           07 FILLER              PIC X(7).                       RL1064.2
016600           07 XRECORD-NUMBER       PIC 9(6).                      RL1064.2
016700           07 FILLER              PIC X(6).                       RL1064.2
016800           07 UPDATE-NUMBER       PIC 9(2).                       RL1064.2
016900           07 FILLER              PIC X(5).                       RL1064.2
017000           07 ODO-NUMBER          PIC 9(4).                       RL1064.2
017100           07 FILLER              PIC X(5).                       RL1064.2
017200           07 XPROGRAM-NAME        PIC X(5).                      RL1064.2
017300           07 FILLER              PIC X(7).                       RL1064.2
017400           07 XRECORD-LENGTH       PIC 9(6).                      RL1064.2
017500           07 FILLER              PIC X(7).                       RL1064.2
017600           07 CHARS-OR-RECORDS    PIC X(2).                       RL1064.2
017700           07 FILLER              PIC X(1).                       RL1064.2
017800           07 XBLOCK-SIZE          PIC 9(4).                      RL1064.2
017900           07 FILLER              PIC X(6).                       RL1064.2
018000           07 RECORDS-IN-FILE     PIC 9(6).                       RL1064.2
018100           07 FILLER              PIC X(5).                       RL1064.2
018200           07 XFILE-ORGANIZATION   PIC X(2).                      RL1064.2
018300           07 FILLER              PIC X(6).                       RL1064.2
018400           07 XLABEL-TYPE          PIC X(1).                      RL1064.2
018500        05 FILE-RECORD-INFO-P121-240.                             RL1064.2
018600           07 FILLER              PIC X(8).                       RL1064.2
018700           07 XRECORD-KEY          PIC X(29).                     RL1064.2
018800           07 FILLER              PIC X(9).                       RL1064.2
018900           07 ALTERNATE-KEY1      PIC X(29).                      RL1064.2
019000           07 FILLER              PIC X(9).                       RL1064.2
019100           07 ALTERNATE-KEY2      PIC X(29).                      RL1064.2
019200           07 FILLER              PIC X(7).                       RL1064.2
019300 01  TEST-RESULTS.                                                RL1064.2
019400     02 FILLER                   PIC X      VALUE SPACE.          RL1064.2
019500     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1064.2
019600     02 FILLER                   PIC X      VALUE SPACE.          RL1064.2
019700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1064.2
019800     02 FILLER                   PIC X      VALUE SPACE.          RL1064.2
019900     02  PAR-NAME.                                                RL1064.2
020000       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1064.2
020100       03  PARDOT-X              PIC X      VALUE SPACE.          RL1064.2
020200       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1064.2
020300     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1064.2
020400     02 RE-MARK                  PIC X(61).                       RL1064.2
020500 01  TEST-COMPUTED.                                               RL1064.2
020600     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1064.2
020700     02 FILLER                   PIC X(17)  VALUE                 RL1064.2
020800            "       COMPUTED=".                                   RL1064.2
020900     02 COMPUTED-X.                                               RL1064.2
021000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1064.2
021100     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1064.2
021200                                 PIC -9(9).9(9).                  RL1064.2
021300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1064.2
021400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1064.2
021500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1064.2
021600     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1064.2
021700         04 COMPUTED-18V0                    PIC -9(18).          RL1064.2
021800         04 FILLER                           PIC X.               RL1064.2
021900     03 FILLER PIC X(50) VALUE SPACE.                             RL1064.2
022000 01  TEST-CORRECT.                                                RL1064.2
022100     02 FILLER PIC X(30) VALUE SPACE.                             RL1064.2
022200     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1064.2
022300     02 CORRECT-X.                                                RL1064.2
022400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1064.2
022500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1064.2
022600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1064.2
022700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1064.2
022800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1064.2
022900     03      CR-18V0 REDEFINES CORRECT-A.                         RL1064.2
023000         04 CORRECT-18V0                     PIC -9(18).          RL1064.2
023100         04 FILLER                           PIC X.               RL1064.2
023200     03 FILLER PIC X(2) VALUE SPACE.                              RL1064.2
023300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1064.2
023400 01  CCVS-C-1.                                                    RL1064.2
023500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1064.2
023600-    "SS  PARAGRAPH-NAME                                          RL1064.2
023700-    "       REMARKS".                                            RL1064.2
023800     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1064.2
023900 01  CCVS-C-2.                                                    RL1064.2
024000     02 FILLER                     PIC X        VALUE SPACE.      RL1064.2
024100     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1064.2
024200     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1064.2
024300     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1064.2
024400     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1064.2
024500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1064.2
024600 01  REC-CT                        PIC 99       VALUE ZERO.       RL1064.2
024700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1064.2
024800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1064.2
024900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1064.2
025000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1064.2
025100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1064.2
025200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1064.2
025300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1064.2
025400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1064.2
025500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1064.2
025600 01  CCVS-H-1.                                                    RL1064.2
025700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1064.2
025800     02  FILLER                    PIC X(42)    VALUE             RL1064.2
025900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1064.2
026000     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1064.2
026100 01  CCVS-H-2A.                                                   RL1064.2
026200   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1064.2
026300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1064.2
026400   02  FILLER                        PIC XXXX   VALUE             RL1064.2
026500     "4.2 ".                                                      RL1064.2
026600   02  FILLER                        PIC X(28)  VALUE             RL1064.2
026700            " COPY - NOT FOR DISTRIBUTION".                       RL1064.2
026800   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1064.2
026900                                                                  RL1064.2
027000 01  CCVS-H-2B.                                                   RL1064.2
027100   02  FILLER                        PIC X(15)  VALUE             RL1064.2
027200            "TEST RESULT OF ".                                    RL1064.2
027300   02  TEST-ID                       PIC X(9).                    RL1064.2
027400   02  FILLER                        PIC X(4)   VALUE             RL1064.2
027500            " IN ".                                               RL1064.2
027600   02  FILLER                        PIC X(12)  VALUE             RL1064.2
027700     " HIGH       ".                                              RL1064.2
027800   02  FILLER                        PIC X(22)  VALUE             RL1064.2
027900            " LEVEL VALIDATION FOR ".                             RL1064.2
028000   02  FILLER                        PIC X(58)  VALUE             RL1064.2
028100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1064.2
028200 01  CCVS-H-3.                                                    RL1064.2
028300     02  FILLER                      PIC X(34)  VALUE             RL1064.2
028400            " FOR OFFICIAL USE ONLY    ".                         RL1064.2
028500     02  FILLER                      PIC X(58)  VALUE             RL1064.2
028600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1064.2
028700     02  FILLER                      PIC X(28)  VALUE             RL1064.2
028800            "  COPYRIGHT   1985 ".                                RL1064.2
028900 01  CCVS-E-1.                                                    RL1064.2
029000     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1064.2
029100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1064.2
029200     02 ID-AGAIN                     PIC X(9).                    RL1064.2
029300     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1064.2
029400 01  CCVS-E-2.                                                    RL1064.2
029500     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1064.2
029600     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1064.2
029700     02 CCVS-E-2-2.                                               RL1064.2
029800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1064.2
029900         03 FILLER                   PIC X      VALUE SPACE.      RL1064.2
030000         03 ENDER-DESC               PIC X(44)  VALUE             RL1064.2
030100            "ERRORS ENCOUNTERED".                                 RL1064.2
030200 01  CCVS-E-3.                                                    RL1064.2
030300     02  FILLER                      PIC X(22)  VALUE             RL1064.2
030400            " FOR OFFICIAL USE ONLY".                             RL1064.2
030500     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1064.2
030600     02  FILLER                      PIC X(58)  VALUE             RL1064.2
030700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1064.2
030800     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1064.2
030900     02 FILLER                       PIC X(15)  VALUE             RL1064.2
031000             " COPYRIGHT 1985".                                   RL1064.2
031100 01  CCVS-E-4.                                                    RL1064.2
031200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1064.2
031300     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1064.2
031400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1064.2
031500     02 FILLER                       PIC X(40)  VALUE             RL1064.2
031600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1064.2
031700 01  XXINFO.                                                      RL1064.2
031800     02 FILLER                       PIC X(19)  VALUE             RL1064.2
031900            "*** INFORMATION ***".                                RL1064.2
032000     02 INFO-TEXT.                                                RL1064.2
032100       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1064.2
032200       04 XXCOMPUTED                 PIC X(20).                   RL1064.2
032300       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1064.2
032400       04 XXCORRECT                  PIC X(20).                   RL1064.2
032500     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1064.2
032600 01  HYPHEN-LINE.                                                 RL1064.2
032700     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1064.2
032800     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1064.2
032900-    "*****************************************".                 RL1064.2
033000     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1064.2
033100-    "******************************".                            RL1064.2
033200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1064.2
033300     "RL106A".                                                    RL1064.2
033400 PROCEDURE DIVISION.                                              RL1064.2
033500 CCVS1 SECTION.                                                   RL1064.2
033600 OPEN-FILES.                                                      RL1064.2
033700     OPEN    OUTPUT PRINT-FILE.                                   RL1064.2
033800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1064.2
033900     MOVE    SPACE TO TEST-RESULTS.                               RL1064.2
034000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1064.2
034100     MOVE    ZERO TO REC-SKL-SUB.                                 RL1064.2
034200     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1064.2
034300 CCVS-INIT-FILE.                                                  RL1064.2
034400     ADD     1 TO REC-SKL-SUB.                                    RL1064.2
034500     MOVE    FILE-RECORD-INFO-SKELETON                            RL1064.2
034600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1064.2
034700 CCVS-INIT-EXIT.                                                  RL1064.2
034800     GO TO CCVS1-EXIT.                                            RL1064.2
034900 CLOSE-FILES.                                                     RL1064.2
035000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1064.2
035100 TERMINATE-CCVS.                                                  RL1064.2
035200*    EXIT PROGRAM.                                                RL1064.2
035300*TERMINATE-CALL.                                                  RL1064.2
035400     STOP     RUN.                                                RL1064.2
035500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1064.2
035600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1064.2
035700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1064.2
035800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1064.2
035900     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1064.2
036000 PRINT-DETAIL.                                                    RL1064.2
036100     IF REC-CT NOT EQUAL TO ZERO                                  RL1064.2
036200             MOVE "." TO PARDOT-X                                 RL1064.2
036300             MOVE REC-CT TO DOTVALUE.                             RL1064.2
036400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1064.2
036500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1064.2
036600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1064.2
036700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1064.2
036800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1064.2
036900     MOVE SPACE TO CORRECT-X.                                     RL1064.2
037000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1064.2
037100     MOVE     SPACE TO RE-MARK.                                   RL1064.2
037200 HEAD-ROUTINE.                                                    RL1064.2
037300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1064.2
037400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1064.2
037500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1064.2
037600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1064.2
037700 COLUMN-NAMES-ROUTINE.                                            RL1064.2
037800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1064.2
037900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1064.2
038000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1064.2
038100 END-ROUTINE.                                                     RL1064.2
038200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1064.2
038300 END-RTN-EXIT.                                                    RL1064.2
038400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1064.2
038500 END-ROUTINE-1.                                                   RL1064.2
038600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1064.2
038700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1064.2
038800      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1064.2
038900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1064.2
039000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1064.2
039100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1064.2
039200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1064.2
039300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1064.2
039400  END-ROUTINE-12.                                                 RL1064.2
039500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1064.2
039600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1064.2
039700         MOVE "NO " TO ERROR-TOTAL                                RL1064.2
039800         ELSE                                                     RL1064.2
039900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1064.2
040000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1064.2
040100     PERFORM WRITE-LINE.                                          RL1064.2
040200 END-ROUTINE-13.                                                  RL1064.2
040300     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1064.2
040400         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1064.2
040500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1064.2
040600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1064.2
040700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1064.2
040800      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1064.2
040900          MOVE "NO " TO ERROR-TOTAL                               RL1064.2
041000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1064.2
041100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1064.2
041200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1064.2
041300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1064.2
041400 WRITE-LINE.                                                      RL1064.2
041500     ADD 1 TO RECORD-COUNT.                                       RL1064.2
041600*    IF RECORD-COUNT GREATER 50                                   RL1064.2
041700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1064.2
041800*        MOVE SPACE TO DUMMY-RECORD                               RL1064.2
041900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1064.2
042000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1064.2
042100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1064.2
042200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1064.2
042300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1064.2
042400*        MOVE ZERO TO RECORD-COUNT.                               RL1064.2
042500     PERFORM WRT-LN.                                              RL1064.2
042600 WRT-LN.                                                          RL1064.2
042700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1064.2
042800     MOVE SPACE TO DUMMY-RECORD.                                  RL1064.2
042900 BLANK-LINE-PRINT.                                                RL1064.2
043000     PERFORM WRT-LN.                                              RL1064.2
043100 FAIL-ROUTINE.                                                    RL1064.2
043200     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1064.2
043300            GO TO   FAIL-ROUTINE-WRITE.                           RL1064.2
043400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1064.2
043500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1064.2
043600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1064.2
043700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1064.2
043800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1064.2
043900     GO TO  FAIL-ROUTINE-EX.                                      RL1064.2
044000 FAIL-ROUTINE-WRITE.                                              RL1064.2
044100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1064.2
044200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1064.2
044300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1064.2
044400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1064.2
044500 FAIL-ROUTINE-EX. EXIT.                                           RL1064.2
044600 BAIL-OUT.                                                        RL1064.2
044700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1064.2
044800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1064.2
044900 BAIL-OUT-WRITE.                                                  RL1064.2
045000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1064.2
045100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1064.2
045200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1064.2
045300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1064.2
045400 BAIL-OUT-EX. EXIT.                                               RL1064.2
045500 CCVS1-EXIT.                                                      RL1064.2
045600     EXIT.                                                        RL1064.2
045700 SECT-RC106-001 SECTION.                                          RL1064.2
045800 SECT-RC-02-001-INIT.                                             RL1064.2
045900     MOVE 1 TO KEY-1  POS-NUM2.                                   RL1064.2
046000     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1064.2
046100     MOVE "R/W REL LENGTH RECS" TO FEATURE.                       RL1064.2
046200 REL-TEST-8.                                                      RL1064.2
046300     OPEN OUTPUT RL-FR4.                                          RL1064.2
046400     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
046500     WRITE GRP-1SEQ-RECORD-4A INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
046600     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
046700     ADD 1 TO POS-NUM2.                                           RL1064.2
046800     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
046900     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
047000     WRITE GRP-1SEQ-RECORD-4A INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
047100     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
047200     ADD 1 TO POS-NUM2.                                           RL1064.2
047300     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
047400     MOVE "LONG "  TO RECORD-LONG-OR-SHORT.                       RL1064.2
047500     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
047600     WRITE GRP-1SEQ-RECORD-4B INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
047700     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
047800     ADD 1 TO POS-NUM2.                                           RL1064.2
047900     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
048000     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
048100     WRITE GRP-1SEQ-RECORD-4B INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
048200     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
048300     ADD 1 TO POS-NUM2.                                           RL1064.2
048400     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
048500     MOVE "SHORT" TO RECORD-LONG-OR-SHORT.                        RL1064.2
048600     PERFORM WRITE-FOR-TEST-8 THRU 8-EXIT 11 TIMES.               RL1064.2
048700     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
048800     WRITE GRP-1SEQ-RECORD-4A INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
048900     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
049000     ADD 1 TO POS-NUM2.                                           RL1064.2
049100     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
049200     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
049300     WRITE GRP-1SEQ-RECORD-4A INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
049400     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
049500     ADD 1 TO POS-NUM2.                                           RL1064.2
049600     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
049700     MOVE "LONG " TO RECORD-LONG-OR-SHORT.                        RL1064.2
049800     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
049900     WRITE GRP-1SEQ-RECORD-4B INVALID KEY GO TO INVALID-TEST-8.   RL1064.2
050000     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
050100     MOVE SPACE  TO CORRECT-A  COMPUTED-A.                        RL1064.2
050200     MOVE SPACE TO P-OR-F.                                        RL1064.2
050300     IF READ-WRITE-COUNTER NOT EQUAL TO "WWWWWWWWWWWWWWWWWW  "    RL1064.2
050400         MOVE READ-WRITE-COUNTER TO COMPUTED-A ELSE               RL1064.2
050500     MOVE "18 RECORDS PASSED TO TEST-9" TO RE-MARK.               RL1064.2
050600     GO TO REL-TEST-8-WRITE.                                      RL1064.2
050700 REL-DELETE-8.                                                    RL1064.2
050800     PERFORM DE-LETE.                                             RL1064.2
050900*NOTE  RL-FR4  IS NOT CREATED  -  SO SKIP TO REL-TEST-11.         RL1064.2
051000     MOVE "REL-TEST-8         " TO PAR-NAME.                      RL1064.2
051100     PERFORM PRINT-DETAIL.                                        RL1064.2
051200     GO TO REL-TEST-11.                                           RL1064.2
051300 INVALID-TEST-8.                                                  RL1064.2
051400     MOVE "I" TO ENTRY-RW (POS-NUM2).                             RL1064.2
051500     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1064.2
051600     PERFORM FAIL.                                                RL1064.2
051700     MOVE "INVALID KEY ON WRITE " TO RE-MARK.                     RL1064.2
051800 REL-TEST-8-WRITE.                                                RL1064.2
051900     MOVE "REL-TEST-8         " TO PAR-NAME.                      RL1064.2
052000     PERFORM PRINT-DETAIL.                                        RL1064.2
052100     CLOSE RL-FR4.                                                RL1064.2
052200*                                                                 RL1064.2
052300     GO TO INIT-TEST-9.                                           RL1064.2
052400 WRITE-FOR-TEST-8.                                                RL1064.2
052500     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-4B.                     RL1064.2
052600     WRITE GRP-1SEQ-RECORD-4A INVALID KEY GO TO I-KEY-8.          RL1064.2
052700     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
052800     ADD 1 TO POS-NUM2.                                           RL1064.2
052900     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
053000     GO TO 8-EXIT.                                                RL1064.2
053100 I-KEY-8.                                                         RL1064.2
053200     MOVE "INVALID KEY ON WRITE " TO RE-MARK.                     RL1064.2
053300     MOVE "I" TO ENTRY-RW (POS-NUM2).                             RL1064.2
053400     ADD 1 TO POS-NUM2.                                           RL1064.2
053500     MOVE POS-NUM2 TO KEY-1.                                      RL1064.2
053600     PERFORM FAIL.                                                RL1064.2
053700 8-EXIT.                                                          RL1064.2
053800     EXIT.                                                        RL1064.2
053900 INIT-TEST-9.                                                     RL1064.2
054000     OPEN INPUT RL-FR4.                                           RL1064.2
054100     MOVE 01 TO SUB-1  KEY-1.                                     RL1064.2
054200     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1064.2
054300 REL-TEST-9.                                                      RL1064.2
054400     READ RL-FR4 INVALID KEY                                      RL1064.2
054500         MOVE "E" TO ENTRY-RW (SUB-1)                             RL1064.2
054600         GO TO COMPARE-FOR-TEST-9.                                RL1064.2
054700     IF SUB-1 EQUAL TO 19                                         RL1064.2
054800         GO TO COMPARE-FOR-TEST-9.                                RL1064.2
054900     MOVE "R" TO ENTRY-RW (SUB-1).                                RL1064.2
055000     ADD 1 TO SUB-1.                                              RL1064.2
055100     MOVE SUB-1 TO KEY-1.                                         RL1064.2
055200     GO TO REL-TEST-9.                                            RL1064.2
055300 COMPARE-FOR-TEST-9.                                              RL1064.2
055400     IF READ-WRITE-COUNTER EQUAL TO "RRRRRRRRRRRRRRRRRRE "        RL1064.2
055500         PERFORM PASS                                             RL1064.2
055600         GO TO REL-TEST-9-WRITE.                                  RL1064.2
055700     MOVE "RRRRRRRRRRRRRRRRRRE " TO CORRECT-A.                    RL1064.2
055800     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1064.2
055900     PERFORM FAIL.                                                RL1064.2
056000     MOVE "INCORRECT NUMBER OF READS  " TO RE-MARK.               RL1064.2
056100 REL-TEST-9-WRITE.                                                RL1064.2
056200     MOVE "REL-TEST-9         " TO PAR-NAME.                      RL1064.2
056300     PERFORM PRINT-DETAIL.                                        RL1064.2
056400     CLOSE RL-FR4.                                                RL1064.2
056500 INIT-TEST-10.                                                    RL1064.2
056600     OPEN INPUT RL-FR4.                                           RL1064.2
056700     MOVE 10 TO SUB-1  KEY-1.                                     RL1064.2
056800 REL-TEST-10.                                                     RL1064.2
056900     READ RL-FR4 INVALID KEY                                      RL1064.2
057000         MOVE "***INVALID KEY***" TO COMPUTED-A                   RL1064.2
057100         MOVE SPACE TO CORRECT-A                                  RL1064.2
057200         MOVE "INVAILD KEY RL-FR4  " TO RE-MARK                   RL1064.2
057300         PERFORM FAIL                                             RL1064.2
057400         GO TO REL-TEST-10-WRITE.                                 RL1064.2
057500*    NOTE  *** IF REC-NUMBER-4B CONTAINS THE RECORD NUMBER        RL1064.2
057600*          THEN FIXED LENGTH RECORDS WERE WRITTEN INSTEAD         RL1064.2
057700*          OF VARIABLE LENGTH RECORDS.                            RL1064.2
057800*    NOTE  CHECK LENGTH OF RECORD 10.                             RL1064.2
057900 COMPARE-FOR-TEST-10.                                             RL1064.2
058000     IF REC-NUMBER-4B EQUAL TO "10"                               RL1064.2
058100          MOVE "FIXED LENGTH RECORDS" TO COMPUTED-A.              RL1064.2
058200 REL-TEST-10-WRITE.                                               RL1064.2
058300     CLOSE RL-FR4.                                                RL1064.2
058400     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     RL1064.2
058500     MOVE "REL-TEST-10        " TO PAR-NAME.                      RL1064.2
058600     PERFORM PRINT-DETAIL.                                        RL1064.2
058700     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1064.2
058800     MOVE 1 TO KEY-2  POS-NUM2.                                   RL1064.2
058900 REL-TEST-11.                                                     RL1064.2
059000     OPEN OUTPUT RL-FR5.                                          RL1064.2
059100     PERFORM REL-TEST-11-SHORT-REC  2 TIMES.                      RL1064.2
059200     PERFORM REL-TEST-11-LONG-REC 2 TIMES.                        RL1064.2
059300     PERFORM REL-TEST-11-SHORT-REC 4 TIMES.                       RL1064.2
059400     PERFORM REL-TEST-11-LONG-REC 2 TIMES.                        RL1064.2
059500     MOVE SPACE TO COMPUTED-A  CORRECT-A.                         RL1064.2
059600     MOVE "10 RECORDS PASSED  TEST-12" TO RE-MARK.                RL1064.2
059700     GO TO REL-TEST-11-WRITE.                                     RL1064.2
059800 REL-DELETE-11.                                                   RL1064.2
059900     PERFORM DE-LETE.                                             RL1064.2
060000*    NOTE RL-FR5 IS NOT CREATED  SO SKIP TO REL-TEST-15.          RL1064.2
060100     MOVE "REL-TEST-11        " TO PAR-NAME.                      RL1064.2
060200     PERFORM PRINT-DETAIL.                                        RL1064.2
060300     GO TO REL-TEST-15.                                           RL1064.2
060400 REL-TEST-11-LONG-REC.                                            RL1064.2
060500     MOVE "LONG " TO RECORD-LONG-OR-SHORT.                        RL1064.2
060600     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-5B.                     RL1064.2
060700     WRITE GRP-1SEQ-RECORD-5B INVALID KEY GO TO I-KEY-11.         RL1064.2
060800     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
060900     ADD 1 TO POS-NUM2.                                           RL1064.2
061000     MOVE POS-NUM2 TO KEY-2.                                      RL1064.2
061100 REL-TEST-11-SHORT-REC.                                           RL1064.2
061200     MOVE "SHORT" TO RECORD-LONG-OR-SHORT.                        RL1064.2
061300     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-5B.                     RL1064.2
061400     WRITE GRP-1SEQ-RECORD-5A INVALID KEY GO TO I-KEY-11.         RL1064.2
061500     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
061600     ADD 1 TO POS-NUM2.                                           RL1064.2
061700     MOVE POS-NUM2 TO KEY-2.                                      RL1064.2
061800 I-KEY-11.                                                        RL1064.2
061900     MOVE "INVALID KEY ON WRITE " TO RE-MARK.                     RL1064.2
062000     PERFORM FAIL.                                                RL1064.2
062100     MOVE "I" TO ENTRY-RW (POS-NUM2).                             RL1064.2
062200     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1064.2
062300 REL-TEST-11-WRITE.                                               RL1064.2
062400     CLOSE RL-FR5.                                                RL1064.2
062500     MOVE "REL-TEST-11        " TO PAR-NAME.                      RL1064.2
062600     PERFORM PRINT-DETAIL.                                        RL1064.2
062700*                                                                 RL1064.2
062800 INIT-TEST-12.                                                    RL1064.2
062900     OPEN INPUT RL-FR5.                                           RL1064.2
063000     MOVE 01 TO SUB-1  KEY-2.                                     RL1064.2
063100     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1064.2
063200 REL-TEST-12.                                                     RL1064.2
063300     READ RL-FR5 INVALID KEY                                      RL1064.2
063400         MOVE "E" TO ENTRY-RW (SUB-1)                             RL1064.2
063500         GO TO COMPARE-FOR-TEST-12.                               RL1064.2
063600     MOVE "R" TO ENTRY-RW (SUB-1).                                RL1064.2
063700     IF SUB-1 EQUAL TO 11                                         RL1064.2
063800         GO TO COMPARE-FOR-TEST-12.                               RL1064.2
063900     ADD 1 TO SUB-1.                                              RL1064.2
064000*    NOTE  BLANK OUT GARBAGE IN INPUT AREA.                       RL1064.2
064100*    MOVE SPACE TO GRP-1SEQ-RECORD-5B.                            RL1064.2
064200     MOVE SUB-1 TO KEY-2.                                         RL1064.2
064300     GO TO REL-TEST-12.                                           RL1064.2
064400 COMPARE-FOR-TEST-12.                                             RL1064.2
064500     IF READ-WRITE-COUNTER EQUAL TO "RRRRRRRRRRE"                 RL1064.2
064600         PERFORM PASS                                             RL1064.2
064700         GO TO REL-TEST-12-WRITE.                                 RL1064.2
064800     MOVE "RRRRRRRRRRE" TO CORRECT-A.                             RL1064.2
064900     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1064.2
065000     PERFORM FAIL.                                                RL1064.2
065100 REL-TEST-12-WRITE.                                               RL1064.2
065200     MOVE "REL-TEST-12        " TO PAR-NAME.                      RL1064.2
065300     PERFORM PRINT-DETAIL.                                        RL1064.2
065400     CLOSE RL-FR5.                                                RL1064.2
065500 INIT-TEST-13.                                                    RL1064.2
065600     OPEN INPUT RL-FR5.                                           RL1064.2
065700     MOVE 05 TO SUB-1  KEY-2.                                     RL1064.2
065800 READ-FOR-TEST-13.                                                RL1064.2
065900     READ RL-FR5 INVALID KEY                                      RL1064.2
066000         MOVE "***INVALID KEY***" TO COMPUTED-A                   RL1064.2
066100         MOVE SPACE TO CORRECT-A                                  RL1064.2
066200         MOVE "INVALID KEY RL-FR5 " TO RE-MARK                    RL1064.2
066300         PERFORM FAIL                                             RL1064.2
066400         GO TO REL-TEST-13-WRITE.                                 RL1064.2
066500*    NOTE  *** IF REC-NUMBER-5B CONTAINS THE RECORD NUMBER        RL1064.2
066600*          THEN FIXED LENGTH RECORDS WERE WRITTEN INSTEAD         RL1064.2
066700*          OF VARIABLE LENGTH RECORDS.                            RL1064.2
066800*    NOTE CHECK LENGTH OF RECORD 5.                               RL1064.2
066900 REL-TEST-13.                                                     RL1064.2
067000     IF REC-NUMBER-5B EQUAL TO "05"                               RL1064.2
067100         MOVE "FIXED LENGTH RECORDS" TO COMPUTED-A.               RL1064.2
067200 REL-TEST-13-WRITE.                                               RL1064.2
067300     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     RL1064.2
067400     MOVE "REL-TEST-13        " TO PAR-NAME.                      RL1064.2
067500     PERFORM PRINT-DETAIL.                                        RL1064.2
067600     MOVE 6 TO KEY-2.                                             RL1064.2
067700 REL-TEST-14.                                                     RL1064.2
067800     READ RL-FR5 INVALID KEY                                      RL1064.2
067900         MOVE "***INVALID KEY***" TO COMPUTED-A                   RL1064.2
068000         MOVE SPACE TO CORRECT-A                                  RL1064.2
068100         MOVE "INVALID KEY ON RECORD 6 " TO RE-MARK               RL1064.2
068200         PERFORM FAIL                                             RL1064.2
068300         GO TO REL-TEST-14-WRITE.                                 RL1064.2
068400*    NOTE  CHECK LENGTH OF RECORD 6.                              RL1064.2
068500     IF REC-NUMBER-5B EQUAL TO "06"                               RL1064.2
068600         MOVE "FIXED LENGTH RECORDS" TO COMPUTED-A.               RL1064.2
068700 REL-TEST-14-WRITE.                                               RL1064.2
068800     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     RL1064.2
068900     MOVE "REL-TEST-14        " TO PAR-NAME.                      RL1064.2
069000     PERFORM PRINT-DETAIL.                                        RL1064.2
069100     MOVE SPACE TO READ-WRITE-COUNTER.                            RL1064.2
069200 INIT-TEST-15.                                                    RL1064.2
069300     CLOSE RL-FR5.                                                RL1064.2
069400     MOVE 1 TO KEY-1  KEY-2  KEY-3  POS-NUM2.                     RL1064.2
069500 REL-TEST-15.                                                     RL1064.2
069600     OPEN OUTPUT RL-FR6.                                          RL1064.2
069700     PERFORM REL-TEST-12-SHORT-REC 3 TIMES.                       RL1064.2
069800     PERFORM REL-TEST-12-LONG-REC 2 TIMES.                        RL1064.2
069900     PERFORM REL-TEST-12-SHORT-REC.                               RL1064.2
070000     PERFORM REL-TEST-12-LONG-REC 2 TIMES.                        RL1064.2
070100     PERFORM REL-TEST-12-SHORT-REC 3 TIMES.                       RL1064.2
070200     PERFORM REL-TEST-12-LONG-REC.                                RL1064.2
070300     MOVE SPACE TO COMPUTED-A  CORRECT-A.                         RL1064.2
070400     MOVE "12 RECORDS PASSED  TEST-16" TO RE-MARK.                RL1064.2
070500     GO TO REL-TEST-15-WRITE.                                     RL1064.2
070600 REL-DELETE-15.                                                   RL1064.2
070700     PERFORM DE-LETE.                                             RL1064.2
070800     MOVE "REL-TEST-15        " TO PAR-NAME.                      RL1064.2
070900*    NOTE  THIS IS THE FINAL SERIES OF TESTS,  IF THESE ARE       RL1064.2
071000*          DELETED, THE PROGRAM IS AT AN END  SO,                 RL1064.2
071100*          SKIP TO      END-PARAGRAPH.                            RL1064.2
071200     PERFORM  PRINT-DETAIL.                                       RL1064.2
071300     GO TO    CCVS-EXIT.                                          RL1064.2
071400 REL-TEST-12-LONG-REC.                                            RL1064.2
071500     MOVE "LONG " TO RECORD-LONG-OR-SHORT.                        RL1064.2
071600     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-6B.                     RL1064.2
071700     WRITE GRP-1SEQ-RECORD-6B INVALID KEY GO TO I-KEY-15.         RL1064.2
071800     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
071900     ADD 1 TO POS-NUM2.                                           RL1064.2
072000     MOVE POS-NUM2 TO KEY-3.                                      RL1064.2
072100 REL-TEST-12-SHORT-REC.                                           RL1064.2
072200     MOVE "SHORT" TO RECORD-LONG-OR-SHORT.                        RL1064.2
072300     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-6B.                     RL1064.2
072400     WRITE GRP-1SEQ-RECORD-6A INVALID KEY GO TO I-KEY-15.         RL1064.2
072500     MOVE "W" TO ENTRY-RW (POS-NUM2).                             RL1064.2
072600     ADD 1 TO POS-NUM2.                                           RL1064.2
072700     MOVE POS-NUM2 TO KEY-3.                                      RL1064.2
072800 I-KEY-15.                                                        RL1064.2
072900     MOVE "INVALID KEY ON WRITE "   TO RE-MARK                    RL1064.2
073000     PERFORM FAIL.                                                RL1064.2
073100     MOVE "I" TO ENTRY-RW (POS-NUM2).                             RL1064.2
073200     MOVE READ-WRITE-COUNTER TO COMPUTED-A.                       RL1064.2
073300 REL-TEST-15-WRITE.                                               RL1064.2
073400     MOVE "REL-TEST-15        " TO PAR-NAME.                      RL1064.2
073500     PERFORM PRINT-DETAIL.                                        RL1064.2
073600     CLOSE RL-FR6.                                                RL1064.2
073700*                                                                 RL1064.2
073800 INIT-TEST-16.                                                    RL1064.2
073900     OPEN INPUT RL-FR6.                                           RL1064.2
074000     MOVE 01 TO SUB-1  KEY-3.                                     RL1064.2
074100 REL-TEST-16.                                                     RL1064.2
074200     READ RL-FR6 INVALID KEY                                      RL1064.2
074300         MOVE "***INVALID KEY***" TO COMPUTED-A                   RL1064.2
074400         MOVE SPACE TO CORRECT-A                                  RL1064.2
074500         MOVE "INVALID KEY ON RANDOM-FILE3" TO RE-MARK            RL1064.2
074600         PERFORM FAIL                                             RL1064.2
074700         GO TO REL-TEST-16-WRITE.                                 RL1064.2
074800     IF SUB-1 EQUAL TO 7                                          RL1064.2
074900         GO TO COMPARE-FOR-TEST-16.                               RL1064.2
075000     ADD 1 TO SUB-1.                                              RL1064.2
075100     MOVE SUB-1 TO KEY-3.                                         RL1064.2
075200     GO TO REL-TEST-16.                                           RL1064.2
075300 COMPARE-FOR-TEST-16.                                             RL1064.2
075400     IF REC-NUMBER-6B EQUAL TO "07"                               RL1064.2
075500         PERFORM PASS                                             RL1064.2
075600         GO TO REL-TEST-16-WRITE.                                 RL1064.2
075700     MOVE "RECORD 07 EXPECTED" TO CORRECT-A.                      RL1064.2
075800     MOVE SPACE TO FILLER-LONG.                                   RL1064.2
075900     MOVE RECORD-LONG-ONLY TO COMPUTED-A.                         RL1064.2
076000     MOVE "COMPUTED-A SHOWS REC READ" TO RE-MARK.                 RL1064.2
076100     PERFORM FAIL.                                                RL1064.2
076200 REL-TEST-16-WRITE.                                               RL1064.2
076300     MOVE "REL-TEST-16        " TO PAR-NAME.                      RL1064.2
076400     PERFORM PRINT-DETAIL.                                        RL1064.2
076500 INIT-TEST-17.                                                    RL1064.2
076600     MOVE 01 TO SUB-1  KEY-3.                                     RL1064.2
076700 READ-FOR-TEST-17.                                                RL1064.2
076800     READ RL-FR6 INVALID KEY                                      RL1064.2
076900         MOVE "***INVALID KEY***" TO COMPUTED-A                   RL1064.2
077000         MOVE SPACE TO CORRECT-A                                  RL1064.2
077100         MOVE "INVALID KEY RL-FR6 " TO RE-MARK                    RL1064.2
077200         PERFORM FAIL                                             RL1064.2
077300         GO TO REL-TEST-17-WRITE.                                 RL1064.2
077400     IF SUB-1 EQUAL TO 02                                         RL1064.2
077500         GO TO REL-TEST-17.                                       RL1064.2
077600     ADD 1 TO SUB-1.                                              RL1064.2
077700     MOVE SUB-1 TO KEY-3.                                         RL1064.2
077800     GO TO READ-FOR-TEST-17.                                      RL1064.2
077900 REL-TEST-17.                                                     RL1064.2
078000     IF REC-NUMBER-6B EQUAL TO "02"                               RL1064.2
078100        MOVE "FIXED LENGTH RECORDS" TO COMPUTED-A.                RL1064.2
078200         GO TO REL-TEST-17-WRITE.                                 RL1064.2
078300*    NOTE CHECK LENGTH OF RECORD 2.                               RL1064.2
078400*    NOTE  *** IF REC-NUMBER-6B CONTAINS THE RECORD NUMBER        RL1064.2
078500*          THEN FIXED LENGTH RECORDS WERE WRITTEN INSTEAD         RL1064.2
078600*          OF VARIABLE LENGTH RECORDS.                            RL1064.2
078700 REL-TEST-17-WRITE.                                               RL1064.2
078800     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     RL1064.2
078900     MOVE "REL-TEST-17        " TO PAR-NAME.                      RL1064.2
079000     PERFORM PRINT-DETAIL.                                        RL1064.2
079100 INIT-TEST-18.                                                    RL1064.2
079200     MOVE 12 TO SUB-1  KEY-3.                                     RL1064.2
079300 READ-FOR-TEST-18.                                                RL1064.2
079400     READ RL-FR6 INVALID KEY                                      RL1064.2
079500         MOVE "***INVALID KEY***" TO COMPUTED-A                   RL1064.2
079600         MOVE "RECORD 12 IS MISSING" TO CORRECT-A                 RL1064.2
079700         MOVE "ATTEMPT TO READ LAST RECORD" TO RE-MARK            RL1064.2
079800         PERFORM FAIL                                             RL1064.2
079900         GO TO REL-TEST-18-WRITE.                                 RL1064.2
080000*    NOTE  *** RECORD 12 WAS A LONG RECORD AND                    RL1064.2
080100*          REC-NUMBER-6B SHOULD CONTAIN 12.                       RL1064.2
080200 REL-TEST-18.                                                     RL1064.2
080300     IF REC-NUMBER-6B EQUAL TO "12"                               RL1064.2
080400          PERFORM PASS                                            RL1064.2
080500         GO TO REL-TEST-18-WRITE.                                 RL1064.2
080600     MOVE "WRONG LENGTH RECORD" TO COMPUTED-A.                    RL1064.2
080700             PERFORM FAIL.                                        RL1064.2
080800 REL-TEST-18-WRITE.                                               RL1064.2
080900     MOVE "REL-TEST-18        " TO PAR-NAME.                      RL1064.2
081000     PERFORM PRINT-DETAIL.                                        RL1064.2
081100     CLOSE RL-FR6.                                                RL1064.2
081200 CCVS-EXIT SECTION.                                               RL1064.2
081300 CCVS-999999.                                                     RL1064.2
081400     GO TO CLOSE-FILES.                                           RL1064.2
      *END-OF,RL106A                                                                  
