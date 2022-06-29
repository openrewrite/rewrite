      *HEADER,COBOL,IX205A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2054.2
000200 PROGRAM-ID.                                                      IX2054.2
000300     IX205A.                                                      IX2054.2
000400****************************************************************  IX2054.2
000500*                                                              *  IX2054.2
000600*    VALIDATION FOR:-                                          *  IX2054.2
000700*                                                              *  IX2054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2054.2
000900*                                                              *  IX2054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2054.2
001100*                                                              *  IX2054.2
001200****************************************************************  IX2054.2
001300*    THE FUNCTION OF THIS PROGRAM IS TO TEST THE PERMISSIBLE      IX2054.2
001400*    SYNTACTICAL CONSTRUCTS OF COBOL ELEMENTS ASSOCIATED WITH     IX2054.2
001500*    LEVEL 2 OF INDEXED I-O.  THE ELEMENTS TESTED IN THIS ROU-    IX2054.2
001600*    TINE ARE:                                                    IX2054.2
001700*                                                                 IX2054.2
001800*    (1) ACCESS MODE DYNAMIC;                                     IX2054.2
001900*    (2) ALTERNATE RECORD KEY WITHOUT THE DUPLICATES OPTION;      IX2054.2
002000*    (3) RESERVE CLAUSE;                                          IX2054.2
002100*    (4) SAME CLAUSE;                                             IX2054.2
002200*    (5) BLOCK CONTAINS INTEGER-1 TO INTEGER-2 CLAUSE;            IX2054.2
002300*    (6) VALUE OF IMPLEMENTOR-NAME.                               IX2054.2
002400*                                                                 IX2054.2
002500*   NEW TEST:   START ...                                         IX2054.2
002600*              KEY IS GREATER THAN OR EQUAL TO  ...               IX2054.2
002700*                                                                 IX2054.2
002800*    EACH ELEMENT TESTED WILL BE EXERCISED SEMANTICALLY BY THIS   IX2054.2
002900*    ROUTINE.  FILES ARE CREATED AND ACCESSED USING THE ACCESS    IX2054.2
003000*    MODE IS DYNAMIC.                                             IX2054.2
003100*                                                                 IX2054.2
003200*                                                                 IX2054.2
003300*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2054.2
003400*                                                                 IX2054.2
003500*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2054.2
003600*                    CLAUSE FOR DATA FILE IX-FS1                  IX2054.2
003700*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2054.2
003800*                    CLAUSE FOR DATA FILE IX-FD2                  IX2054.2
003900*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2054.2
004000*                    CLAUSE FOR INDEX FILE IX-FS1                 IX2054.2
004100*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2054.2
004200*                    CLAUSE FOR INDEX FILE IX-FD2                 IX2054.2
004300*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2054.2
004400*             X-62   FOR RAW-DATA                                 IX2054.2
004500*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2054.2
004600*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2054.2
004700*             X-84   LABEL RECORDS  FOR PRINT-FILE                IX2054.2
004800*                                                                 IX2054.2
004900*        NOTE:  X-CARDS 44, 45, 62 AND 84     ARE OPTIONAL        IX2054.2
005000*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2054.2
005100*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2054.2
005200*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2054.2
005300*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2054.2
005400*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2054.2
005500*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2054.2
005600*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2054.2
005700*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2054.2
005800*               THEY ARE AS FOLLOWS                               IX2054.2
005900*                                                                 IX2054.2
006000*                  P  SELECTS X-CARDS 62                          IX2054.2
006100*                  J  SELECTS X-CARDS 44 AND 45                   IX2054.2
006200*                  C  SELECTS X-CARDS 84                          IX2054.2
006300*                                                                 IX2054.2
006400******************************************************            IX2054.2
006500 ENVIRONMENT DIVISION.                                            IX2054.2
006600 CONFIGURATION SECTION.                                           IX2054.2
006700 SOURCE-COMPUTER.                                                 IX2054.2
006800     XXXXX082.                                                    IX2054.2
006900 OBJECT-COMPUTER.                                                 IX2054.2
007000     XXXXX083.                                                    IX2054.2
007100 INPUT-OUTPUT SECTION.                                            IX2054.2
007200 FILE-CONTROL.                                                    IX2054.2
007300*    SELECT RAW-DATA   ASSIGN TO                                  IX2054.2
007400*    XXXXX062                                                     IX2054.2
007500*           ORGANIZATION IS INDEXED                               IX2054.2
007600*           ACCESS MODE IS RANDOM                                 IX2054.2
007700*           RECORD KEY IS RAW-DATA-KEY.                           IX2054.2
007800     SELECT PRINT-FILE ASSIGN TO                                  IX2054.2
007900     XXXXX055.                                                    IX2054.2
008000     SELECT   IX-FD1  ASSIGN TO                                   IX2054.2
008100     XXXXX024                                                     IX2054.2
008200*    XXXXX044                                                     IX2054.2
008300              RESERVE  3  AREA                                    IX2054.2
008400              ORGANIZATION IS INDEXED                             IX2054.2
008500              ACCESS MODE IS DYNAMIC                              IX2054.2
008600              RECORD  KEY IS IX-FD1-KEY                           IX2054.2
008700              ALTERNATE RECORD KEY IS IX-FD1-ALTKEY1.             IX2054.2
008800     SELECT   IX-FD2    ASSIGN TO                                 IX2054.2
008900     XXXXX025                                                     IX2054.2
009000*    XXXXX045                                                     IX2054.2
009100              RESERVE  2  AREAS                                   IX2054.2
009200              ACCESS MODE  DYNAMIC                                IX2054.2
009300              ORGANIZATION INDEXED                                IX2054.2
009400              RECORD KEY  IX-FD2-KEY                              IX2054.2
009500              ALTERNATE RECORD        IX-FD2-ALTKEY1.             IX2054.2
009600 I-O-CONTROL.                                                     IX2054.2
009700     SAME   RECORD  IX-FD1  IX-FD2.                               IX2054.2
009800 DATA DIVISION.                                                   IX2054.2
009900 FILE SECTION.                                                    IX2054.2
010000*                                                                 IX2054.2
010100*FD  RAW-DATA.                                                    IX2054.2
010200*                                                                 IX2054.2
010300*01  RAW-DATA-SATZ.                                               IX2054.2
010400*    05  RAW-DATA-KEY        PIC X(6).                            IX2054.2
010500*    05  C-DATE              PIC 9(6).                            IX2054.2
010600*    05  C-TIME              PIC 9(8).                            IX2054.2
010700*    05  C-NO-OF-TESTS       PIC 99.                              IX2054.2
010800*    05  C-OK                PIC 999.                             IX2054.2
010900*    05  C-ALL               PIC 999.                             IX2054.2
011000*    05  C-FAIL              PIC 999.                             IX2054.2
011100*    05  C-DELETED           PIC 999.                             IX2054.2
011200*    05  C-INSPECT           PIC 999.                             IX2054.2
011300*    05  C-NOTE              PIC X(13).                           IX2054.2
011400*    05  C-INDENT            PIC X.                               IX2054.2
011500*    05  C-ABORT             PIC X(8).                            IX2054.2
011600 FD  PRINT-FILE.                                                  IX2054.2
011700 01  PRINT-REC PICTURE X(120).                                    IX2054.2
011800 01  DUMMY-RECORD PICTURE X(120).                                 IX2054.2
011900 FD  IX-FD1                                                       IX2054.2
012000C    LABEL  RECORDS ARE STANDARD                                  IX2054.2
012100     RECORD CONTAINS  240  CHARACTERS.                            IX2054.2
012200 01  IX-FD1R1-F-G-240.                                            IX2054.2
012300     05 IX-FD1-REC-120           PIC X(120).                      IX2054.2
012400     05 IX-FD1-REC-120-240.                                       IX2054.2
012500              10 FILLER          PIC X(8).                        IX2054.2
012600              10 IX-FD1-REC-KEY.                                  IX2054.2
012700                 15 FILLER       PIC X(19).                       IX2054.2
012800                 15 IX-FD1-KEY   PIC X(10).                       IX2054.2
012900              10 FILLER          PIC X(9).                        IX2054.2
013000              10 IX-FD1-ALT1-KEY.                                 IX2054.2
013100                 15 FILLER       PIC X(19).                       IX2054.2
013200                 15 IX-FD1-ALTKEY1  PIC X(10).                    IX2054.2
013300              10 FILLER          PIC X(45).                       IX2054.2
013400 FD  IX-FD2                                                       IX2054.2
013500C    LABEL RECORDS ARE STANDARD                                   IX2054.2
013600     BLOCK CONTAINS  5  TO  25  RECORDS                           IX2054.2
013700     RECORD CONTAINS  240  CHARACTERS.                            IX2054.2
013800 01  IX-FD2R1-F-G-240.                                            IX2054.2
013900     05 IX-FD2-REC-120           PIC X(120).                      IX2054.2
014000     05 IX-FD2-REC-120-240.                                       IX2054.2
014100              10 FILLER          PIC X(8).                        IX2054.2
014200              10 IX-FD2-REC-KEY.                                  IX2054.2
014300                 15 FILLER       PIC X(19).                       IX2054.2
014400                 15 IX-FD2-KEY   PIC X(10).                       IX2054.2
014500              10 FILLER          PIC X(9).                        IX2054.2
014600              10 IX-FD2-ALT1-KEY.                                 IX2054.2
014700                 15 FILLER       PIC X(19).                       IX2054.2
014800                 15 IX-FD2-ALTKEY1 PIC X(10).                     IX2054.2
014900              10 FILLER          PIC X(45).                       IX2054.2
015000 WORKING-STORAGE SECTION.                                         IX2054.2
015100*01  IX-FD2-ID                                                    IX2054.2
015200*    XXXXX086.                                                    IX2054.2
015300 01  WRK-CS-09V00-001            PIC S9(9)    COMPUTATIONAL.      IX2054.2
015400 01  WRK-REC-KEY-FD1.                                             IX2054.2
015500     03 FILLER                   PIC X(19)         VALUE SPACE.   IX2054.2
015600     03 WRK-DU-10V00-001         PIC 9(10)      VALUE ZERO.       IX2054.2
015700 01  WRK-ALT1-KEY-FD1.                                            IX2054.2
015800     03 FILLER                   PIC X(19)        VALUE SPACE.    IX2054.2
015900     03 WRK-DU-10V00-002         PIC 9(10)     VALUE ZERO.        IX2054.2
016000 01  FD1-FILE-SIZE               PIC 9(10)  VALUE 200.            IX2054.2
016100 01  WRK-REC-KEY-FD2.                                             IX2054.2
016200     03 FILLER                   PIC X(19)      VALUE SPACE.      IX2054.2
016300     03 WRK-DU-10V00-003         PIC 9(10)    VALUE ZERO.         IX2054.2
016400 01  WRK-ALT1-KEY-FD2.                                            IX2054.2
016500     03 FILLER                   PIC X(19)      VALUE SPACE.      IX2054.2
016600     03 WRK-DU-10V00-004         PIC 9(10)   VALUE ZERO.          IX2054.2
016700 01  FD2-FILE-SIZE               PIC 9(10)  VALUE 200.            IX2054.2
016800 01  FILE-RECORD-INFORMATION-REC.                                 IX2054.2
016900     03 FILE-RECORD-INFO-SKELETON.                                IX2054.2
017000        05 FILLER                 PICTURE X(48)       VALUE       IX2054.2
017100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2054.2
017200        05 FILLER                 PICTURE X(46)       VALUE       IX2054.2
017300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2054.2
017400        05 FILLER                 PICTURE X(26)       VALUE       IX2054.2
017500             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2054.2
017600        05 FILLER                 PICTURE X(37)       VALUE       IX2054.2
017700             ",RECKEY=                             ".             IX2054.2
017800        05 FILLER                 PICTURE X(38)       VALUE       IX2054.2
017900             ",ALTKEY1=                             ".            IX2054.2
018000        05 FILLER                 PICTURE X(38)       VALUE       IX2054.2
018100             ",ALTKEY2=                             ".            IX2054.2
018200        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2054.2
018300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2054.2
018400        05 FILE-RECORD-INFO-P1-120.                               IX2054.2
018500           07 FILLER              PIC X(5).                       IX2054.2
018600           07 XFILE-NAME           PIC X(6).                      IX2054.2
018700           07 FILLER              PIC X(8).                       IX2054.2
018800           07 XRECORD-NAME         PIC X(6).                      IX2054.2
018900           07 FILLER              PIC X(1).                       IX2054.2
019000           07 REELUNIT-NUMBER     PIC 9(1).                       IX2054.2
019100           07 FILLER              PIC X(7).                       IX2054.2
019200           07 XRECORD-NUMBER       PIC 9(6).                      IX2054.2
019300           07 FILLER              PIC X(6).                       IX2054.2
019400           07 UPDATE-NUMBER       PIC 9(2).                       IX2054.2
019500           07 FILLER              PIC X(5).                       IX2054.2
019600           07 ODO-NUMBER          PIC 9(4).                       IX2054.2
019700           07 FILLER              PIC X(5).                       IX2054.2
019800           07 XPROGRAM-NAME        PIC X(5).                      IX2054.2
019900           07 FILLER              PIC X(7).                       IX2054.2
020000           07 XRECORD-LENGTH       PIC 9(6).                      IX2054.2
020100           07 FILLER              PIC X(7).                       IX2054.2
020200           07 CHARS-OR-RECORDS    PIC X(2).                       IX2054.2
020300           07 FILLER              PIC X(1).                       IX2054.2
020400           07 XBLOCK-SIZE          PIC 9(4).                      IX2054.2
020500           07 FILLER              PIC X(6).                       IX2054.2
020600           07 RECORDS-IN-FILE     PIC 9(6).                       IX2054.2
020700           07 FILLER              PIC X(5).                       IX2054.2
020800           07 XFILE-ORGANIZATION   PIC X(2).                      IX2054.2
020900           07 FILLER              PIC X(6).                       IX2054.2
021000           07 XLABEL-TYPE          PIC X(1).                      IX2054.2
021100        05 FILE-RECORD-INFO-P121-240.                             IX2054.2
021200           07 FILLER              PIC X(8).                       IX2054.2
021300           07 XRECORD-KEY          PIC X(29).                     IX2054.2
021400           07 FILLER              PIC X(9).                       IX2054.2
021500           07 ALTERNATE-KEY1      PIC X(29).                      IX2054.2
021600           07 FILLER              PIC X(9).                       IX2054.2
021700           07 ALTERNATE-KEY2      PIC X(29).                      IX2054.2
021800           07 FILLER              PIC X(7).                       IX2054.2
021900 01  TEST-RESULTS.                                                IX2054.2
022000     02 FILLER                   PIC X      VALUE SPACE.          IX2054.2
022100     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2054.2
022200     02 FILLER                   PIC X      VALUE SPACE.          IX2054.2
022300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2054.2
022400     02 FILLER                   PIC X      VALUE SPACE.          IX2054.2
022500     02  PAR-NAME.                                                IX2054.2
022600       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2054.2
022700       03  PARDOT-X              PIC X      VALUE SPACE.          IX2054.2
022800       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2054.2
022900     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2054.2
023000     02 RE-MARK                  PIC X(61).                       IX2054.2
023100 01  TEST-COMPUTED.                                               IX2054.2
023200     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2054.2
023300     02 FILLER                   PIC X(17)  VALUE                 IX2054.2
023400            "       COMPUTED=".                                   IX2054.2
023500     02 COMPUTED-X.                                               IX2054.2
023600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2054.2
023700     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2054.2
023800                                 PIC -9(9).9(9).                  IX2054.2
023900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2054.2
024000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2054.2
024100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2054.2
024200     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2054.2
024300         04 COMPUTED-18V0                    PIC -9(18).          IX2054.2
024400         04 FILLER                           PIC X.               IX2054.2
024500     03 FILLER PIC X(50) VALUE SPACE.                             IX2054.2
024600 01  TEST-CORRECT.                                                IX2054.2
024700     02 FILLER PIC X(30) VALUE SPACE.                             IX2054.2
024800     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2054.2
024900     02 CORRECT-X.                                                IX2054.2
025000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2054.2
025100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2054.2
025200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2054.2
025300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2054.2
025400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2054.2
025500     03      CR-18V0 REDEFINES CORRECT-A.                         IX2054.2
025600         04 CORRECT-18V0                     PIC -9(18).          IX2054.2
025700         04 FILLER                           PIC X.               IX2054.2
025800     03 FILLER PIC X(2) VALUE SPACE.                              IX2054.2
025900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2054.2
026000 01  CCVS-C-1.                                                    IX2054.2
026100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2054.2
026200-    "SS  PARAGRAPH-NAME                                          IX2054.2
026300-    "       REMARKS".                                            IX2054.2
026400     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2054.2
026500 01  CCVS-C-2.                                                    IX2054.2
026600     02 FILLER                     PIC X        VALUE SPACE.      IX2054.2
026700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2054.2
026800     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2054.2
026900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2054.2
027000     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2054.2
027100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2054.2
027200 01  REC-CT                        PIC 99       VALUE ZERO.       IX2054.2
027300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2054.2
027400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2054.2
027500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2054.2
027600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2054.2
027700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2054.2
027800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2054.2
027900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2054.2
028000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2054.2
028100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2054.2
028200 01  CCVS-H-1.                                                    IX2054.2
028300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2054.2
028400     02  FILLER                    PIC X(42)    VALUE             IX2054.2
028500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2054.2
028600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2054.2
028700 01  CCVS-H-2A.                                                   IX2054.2
028800   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2054.2
028900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2054.2
029000   02  FILLER                        PIC XXXX   VALUE             IX2054.2
029100     "4.2 ".                                                      IX2054.2
029200   02  FILLER                        PIC X(28)  VALUE             IX2054.2
029300            " COPY - NOT FOR DISTRIBUTION".                       IX2054.2
029400   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2054.2
029500                                                                  IX2054.2
029600 01  CCVS-H-2B.                                                   IX2054.2
029700   02  FILLER                        PIC X(15)  VALUE             IX2054.2
029800            "TEST RESULT OF ".                                    IX2054.2
029900   02  TEST-ID                       PIC X(9).                    IX2054.2
030000   02  FILLER                        PIC X(4)   VALUE             IX2054.2
030100            " IN ".                                               IX2054.2
030200   02  FILLER                        PIC X(12)  VALUE             IX2054.2
030300     " HIGH       ".                                              IX2054.2
030400   02  FILLER                        PIC X(22)  VALUE             IX2054.2
030500            " LEVEL VALIDATION FOR ".                             IX2054.2
030600   02  FILLER                        PIC X(58)  VALUE             IX2054.2
030700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2054.2
030800 01  CCVS-H-3.                                                    IX2054.2
030900     02  FILLER                      PIC X(34)  VALUE             IX2054.2
031000            " FOR OFFICIAL USE ONLY    ".                         IX2054.2
031100     02  FILLER                      PIC X(58)  VALUE             IX2054.2
031200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2054.2
031300     02  FILLER                      PIC X(28)  VALUE             IX2054.2
031400            "  COPYRIGHT   1985 ".                                IX2054.2
031500 01  CCVS-E-1.                                                    IX2054.2
031600     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2054.2
031700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2054.2
031800     02 ID-AGAIN                     PIC X(9).                    IX2054.2
031900     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2054.2
032000 01  CCVS-E-2.                                                    IX2054.2
032100     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2054.2
032200     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2054.2
032300     02 CCVS-E-2-2.                                               IX2054.2
032400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2054.2
032500         03 FILLER                   PIC X      VALUE SPACE.      IX2054.2
032600         03 ENDER-DESC               PIC X(44)  VALUE             IX2054.2
032700            "ERRORS ENCOUNTERED".                                 IX2054.2
032800 01  CCVS-E-3.                                                    IX2054.2
032900     02  FILLER                      PIC X(22)  VALUE             IX2054.2
033000            " FOR OFFICIAL USE ONLY".                             IX2054.2
033100     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2054.2
033200     02  FILLER                      PIC X(58)  VALUE             IX2054.2
033300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2054.2
033400     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2054.2
033500     02 FILLER                       PIC X(15)  VALUE             IX2054.2
033600             " COPYRIGHT 1985".                                   IX2054.2
033700 01  CCVS-E-4.                                                    IX2054.2
033800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2054.2
033900     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2054.2
034000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2054.2
034100     02 FILLER                       PIC X(40)  VALUE             IX2054.2
034200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2054.2
034300 01  XXINFO.                                                      IX2054.2
034400     02 FILLER                       PIC X(19)  VALUE             IX2054.2
034500            "*** INFORMATION ***".                                IX2054.2
034600     02 INFO-TEXT.                                                IX2054.2
034700       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2054.2
034800       04 XXCOMPUTED                 PIC X(20).                   IX2054.2
034900       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2054.2
035000       04 XXCORRECT                  PIC X(20).                   IX2054.2
035100     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2054.2
035200 01  HYPHEN-LINE.                                                 IX2054.2
035300     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2054.2
035400     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2054.2
035500-    "*****************************************".                 IX2054.2
035600     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2054.2
035700-    "******************************".                            IX2054.2
035800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2054.2
035900     "IX205A".                                                    IX2054.2
036000 PROCEDURE DIVISION.                                              IX2054.2
036100 CCVS1 SECTION.                                                   IX2054.2
036200 OPEN-FILES.                                                      IX2054.2
036300*    OPEN I-O RAW-DATA.                                           IX2054.2
036400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2054.2
036500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2054.2
036600*    MOVE "ABORTED " TO C-ABORT.                                  IX2054.2
036700*    ADD 1 TO C-NO-OF-TESTS.                                      IX2054.2
036800*    ACCEPT C-DATE  FROM DATE.                                    IX2054.2
036900*    ACCEPT C-TIME  FROM TIME.                                    IX2054.2
037000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2054.2
037100*END-E-1.                                                         IX2054.2
037200*    CLOSE RAW-DATA.                                              IX2054.2
037300     OPEN    OUTPUT PRINT-FILE.                                   IX2054.2
037400     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2054.2
037500     MOVE    SPACE TO TEST-RESULTS.                               IX2054.2
037600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2054.2
037700     MOVE    ZERO TO REC-SKL-SUB.                                 IX2054.2
037800     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2054.2
037900 CCVS-INIT-FILE.                                                  IX2054.2
038000     ADD     1 TO REC-SKL-SUB.                                    IX2054.2
038100     MOVE    FILE-RECORD-INFO-SKELETON                            IX2054.2
038200          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2054.2
038300 CCVS-INIT-EXIT.                                                  IX2054.2
038400     GO TO CCVS1-EXIT.                                            IX2054.2
038500 CLOSE-FILES.                                                     IX2054.2
038600*    OPEN I-O RAW-DATA.                                           IX2054.2
038700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2054.2
038800*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2054.2
038900*    MOVE "OK.     " TO C-ABORT.                                  IX2054.2
039000*    MOVE PASS-COUNTER TO C-OK.                                   IX2054.2
039100*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2054.2
039200*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2054.2
039300*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2054.2
039400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2054.2
039500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2054.2
039600*END-E-2.                                                         IX2054.2
039700*    CLOSE RAW-DATA.                                              IX2054.2
039800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2054.2
039900 TERMINATE-CCVS.                                                  IX2054.2
040000*    EXIT PROGRAM.                                                IX2054.2
040100*TERMINATE-CALL.                                                  IX2054.2
040200     STOP     RUN.                                                IX2054.2
040300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2054.2
040400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2054.2
040500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2054.2
040600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2054.2
040700     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2054.2
040800 PRINT-DETAIL.                                                    IX2054.2
040900     IF REC-CT NOT EQUAL TO ZERO                                  IX2054.2
041000             MOVE "." TO PARDOT-X                                 IX2054.2
041100             MOVE REC-CT TO DOTVALUE.                             IX2054.2
041200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2054.2
041300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2054.2
041400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2054.2
041500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2054.2
041600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2054.2
041700     MOVE SPACE TO CORRECT-X.                                     IX2054.2
041800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2054.2
041900     MOVE     SPACE TO RE-MARK.                                   IX2054.2
042000 HEAD-ROUTINE.                                                    IX2054.2
042100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2054.2
042200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2054.2
042300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2054.2
042400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2054.2
042500 COLUMN-NAMES-ROUTINE.                                            IX2054.2
042600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2054.2
042700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2054.2
042800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2054.2
042900 END-ROUTINE.                                                     IX2054.2
043000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2054.2
043100 END-RTN-EXIT.                                                    IX2054.2
043200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2054.2
043300 END-ROUTINE-1.                                                   IX2054.2
043400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2054.2
043500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2054.2
043600      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2054.2
043700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2054.2
043800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2054.2
043900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2054.2
044000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2054.2
044100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2054.2
044200  END-ROUTINE-12.                                                 IX2054.2
044300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2054.2
044400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2054.2
044500         MOVE "NO " TO ERROR-TOTAL                                IX2054.2
044600         ELSE                                                     IX2054.2
044700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2054.2
044800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2054.2
044900     PERFORM WRITE-LINE.                                          IX2054.2
045000 END-ROUTINE-13.                                                  IX2054.2
045100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2054.2
045200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2054.2
045300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2054.2
045400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2054.2
045500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2054.2
045600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2054.2
045700          MOVE "NO " TO ERROR-TOTAL                               IX2054.2
045800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2054.2
045900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2054.2
046000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2054.2
046100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2054.2
046200 WRITE-LINE.                                                      IX2054.2
046300     ADD 1 TO RECORD-COUNT.                                       IX2054.2
046400*    IF RECORD-COUNT GREATER 42                                   IX2054.2
046500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2054.2
046600*        MOVE SPACE TO DUMMY-RECORD                               IX2054.2
046700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2054.2
046800*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2054.2
046900*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2054.2
047000*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2054.2
047100*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2054.2
047200*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2054.2
047300*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2054.2
047400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2054.2
047500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2054.2
047600*        MOVE ZERO TO RECORD-COUNT.                               IX2054.2
047700     PERFORM WRT-LN.                                              IX2054.2
047800 WRT-LN.                                                          IX2054.2
047900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2054.2
048000     MOVE SPACE TO DUMMY-RECORD.                                  IX2054.2
048100 BLANK-LINE-PRINT.                                                IX2054.2
048200     PERFORM WRT-LN.                                              IX2054.2
048300 FAIL-ROUTINE.                                                    IX2054.2
048400     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2054.2
048500            GO TO   FAIL-ROUTINE-WRITE.                           IX2054.2
048600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2054.2
048700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2054.2
048800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2054.2
048900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2054.2
049000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2054.2
049100     GO TO  FAIL-ROUTINE-EX.                                      IX2054.2
049200 FAIL-ROUTINE-WRITE.                                              IX2054.2
049300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2054.2
049400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2054.2
049500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2054.2
049600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2054.2
049700 FAIL-ROUTINE-EX. EXIT.                                           IX2054.2
049800 BAIL-OUT.                                                        IX2054.2
049900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2054.2
050000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2054.2
050100 BAIL-OUT-WRITE.                                                  IX2054.2
050200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2054.2
050300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2054.2
050400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2054.2
050500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2054.2
050600 BAIL-OUT-EX. EXIT.                                               IX2054.2
050700 CCVS1-EXIT.                                                      IX2054.2
050800     EXIT.                                                        IX2054.2
050900 SECT-IX-01-001  SECTION.                                         IX2054.2
051000 WRITE-INIT-GF-01.                                                IX2054.2
051100     OPEN     OUTPUT   IX-FD1.                                    IX2054.2
051200     MOVE     ZERO TO WRK-CS-09V00-001.                           IX2054.2
051300     MOVE     ZERO TO WRK-DU-10V00-001.                           IX2054.2
051400     MOVE     "IX-FD1"  TO XFILE-NAME (1).                        IX2054.2
051500     MOVE     "R1-F-G"  TO XRECORD-NAME (1).                      IX2054.2
051600     MOVE     000001    TO XRECORD-NUMBER (1).                    IX2054.2
051700     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   IX2054.2
051800     MOVE     200  TO RECORDS-IN-FILE (1).                        IX2054.2
051900     MOVE     240  TO XRECORD-LENGTH (1).                         IX2054.2
052000     MOVE     0001      TO XBLOCK-SIZE (1).                       IX2054.2
052100     MOVE     "RC"      TO CHARS-OR-RECORDS (1).                  IX2054.2
052200     MOVE     "IX"      TO XFILE-ORGANIZATION (1).                IX2054.2
052300     MOVE     "S"       TO XLABEL-TYPE (1).                       IX2054.2
052400     MOVE     000200    TO WRK-DU-10V00-002                       IX2054.2
052500     MOVE     "FILE CREATED"  TO RE-MARK.                         IX2054.2
052600     MOVE     "WRITE-TEST-GF-01" TO PAR-NAME.                     IX2054.2
052700     MOVE     "WRITE DYNAMIC MODE " TO FEATURE.                   IX2054.2
052800 WRITE-TEST-GF-01-R.                                              IX2054.2
052900     MOVE     XRECORD-NUMBER (1) TO WRK-DU-10V00-001.             IX2054.2
053000     MOVE     WRK-REC-KEY-FD1  TO XRECORD-KEY (1).                IX2054.2
053100     MOVE     WRK-ALT1-KEY-FD1  TO ALTERNATE-KEY1 (1).            IX2054.2
053200     MOVE     FILE-RECORD-INFO (1)  TO IX-FD1R1-F-G-240.          IX2054.2
053300     WRITE    IX-FD1R1-F-G-240                                    IX2054.2
053400                        INVALID KEY GO TO WRITE-TEST-GF-01.       IX2054.2
053500     IF     XRECORD-NUMBER (1)  NOT LESS THAN FD1-FILE-SIZE       IX2054.2
053600              GO TO    WRITE-TEST-GF-01.                          IX2054.2
053700     ADD      000001   TO XRECORD-NUMBER (1).                     IX2054.2
053800     SUBTRACT  000001   FROM WRK-DU-10V00-002.                    IX2054.2
053900     GO TO    WRITE-TEST-GF-01-R.                                 IX2054.2
054000 WRITE-TEST-GF-01.                                                IX2054.2
054100     MOVE     XRECORD-NUMBER  (1)  TO COMPUTED-18V0.              IX2054.2
054200     MOVE     FD1-FILE-SIZE   TO CORRECT-18V0.                    IX2054.2
054300     IF       XRECORD-NUMBER (1)  EQUAL TO FD1-FILE-SIZE          IX2054.2
054400              PERFORM  PASS                                       IX2054.2
054500          ELSE                                                    IX2054.2
054600             MOVE     "FILE CREATION PREMATURE" TO RE-MARK        IX2054.2
054700             PERFORM  FAIL.                                       IX2054.2
054800     PERFORM   PRINT-DETAIL.                                      IX2054.2
054900*                                                                 IX2054.2
055000*    01                                                           IX2054.2
055100*                                                                 IX2054.2
055200     CLOSE  IX-FD1.                                               IX2054.2
055300 READ-INIT-F1-01.                                                 IX2054.2
055400     OPEN     INPUT  IX-FD1.                                      IX2054.2
055500     MOVE     ZERO TO WRK-DU-10V00-001.                           IX2054.2
055600     MOVE     ZERO TO WRK-DU-10V00-002.                           IX2054.2
055700     MOVE    ZERO TO WRK-CS-09V00-001.                            IX2054.2
055800     MOVE     "READ-TEST-F1-01  "  TO PAR-NAME.                   IX2054.2
055900     MOVE     "READ NEXT RECORD "  TO FEATURE.                    IX2054.2
056000 READ-TEST-F1-01-3.                                               IX2054.2
056100     ADD      1   TO WRK-DU-10V00-001.                            IX2054.2
056200     READ    IX-FD1                                               IX2054.2
056300        NEXT RECORD                                               IX2054.2
056400         AT END                                                   IX2054.2
056500              ADD    1000  TO WRK-DU-10V00-002.                   IX2054.2
056600*                                                                 IX2054.2
056700*    COMPUTED RESULTS IN INCREMENTS OF 1000  INDICATE AT END PATH IX2054.2
056800*         HAS BEEN TAKEN.                                         IX2054.2
056900*                                                                 IX2054.2
057000     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2054.2
057100     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2054.2
057200              ADD     1  TO WRK-DU-10V00-002.                     IX2054.2
057300*                                                                 IX2054.2
057400*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICATE THAT THE       IX2054.2
057500*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2054.2
057600*                                                                 IX2054.2
057700     ADD     1  TO WRK-CS-09V00-001.                              IX2054.2
057800     IF      WRK-CS-09V00-001  GREATER THAN 24                    IX2054.2
057900              NEXT  SENTENCE  ELSE                                IX2054.2
058000              GO TO   READ-TEST-F1-01-3.                          IX2054.2
058100     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2054.2
058200              PERFORM  FAIL                                       IX2054.2
058300              MOVE    WRK-DU-10V00-002 TO COMPUTED-18V0           IX2054.2
058400              MOVE    ZERO TO CORRECT-18V0                        IX2054.2
058500              MOVE    "SEE  PROGRAM"  TO RE-MARK                  IX2054.2
058600              ELSE  PERFORM PASS.                                 IX2054.2
058700     PERFORM   PRINT-DETAIL.                                      IX2054.2
058800*                                                                 IX2054.2
058900*    02                                                           IX2054.2
059000*                                                                 IX2054.2
059100 READ-INIT-F2-02.                                                 IX2054.2
059200     MOVE  "READ-TEST-F1-02  "  TO PAR-NAME.                      IX2054.2
059300     MOVE  "READ . RECORD INVALID"  TO FEATURE.                   IX2054.2
059400     MOVE     ZERO TO WRK-DU-10V00-001.                           IX2054.2
059500     MOVE     ZERO TO WRK-CS-09V00-001.                           IX2054.2
059600     MOVE     ZERO TO WRK-DU-10V00-002.                           IX2054.2
059700 READ-TEST-F1-02-5.                                               IX2054.2
059800     ADD      10  TO WRK-DU-10V00-001.                            IX2054.2
059900     MOVE     WRK-DU-10V00-001  TO IX-FD1-KEY                     IX2054.2
060000     READ     IX-FD1  RECORD                                      IX2054.2
060100          INVALID KEY                                             IX2054.2
060200              ADD   1000 TO WRK-DU-10V00-002.                     IX2054.2
060300*                                                                 IX2054.2
060400*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE INVALID KEY  IX2054.2
060500*       PATH HAS BEEN TAKEN.                                      IX2054.2
060600*                                                                 IX2054.2
060700     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2054.2
060800     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2054.2
060900              ADD    1    TO WRK-DU-10V00-002.                    IX2054.2
061000*                                                                 IX2054.2
061100*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICAT THAT THE        IX2054.2
061200*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2054.2
061300*                                                                 IX2054.2
061400     ADD     1  TO WRK-CS-09V00-001.                              IX2054.2
061500     IF      WRK-CS-09V00-001  GREATER THAN   10                  IX2054.2
061600              NEXT SENTENCE  ELSE                                 IX2054.2
061700              GO TO    READ-TEST-F1-02-5.                         IX2054.2
061800 READ-TEST-F1-02.                                                 IX2054.2
061900     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2054.2
062000              PERFORM  FAIL                                       IX2054.2
062100              MOVE    WRK-DU-10V00-002  TO COMPUTED-18V0          IX2054.2
062200              MOVE    ZERO  TO CORRECT-18V0                       IX2054.2
062300              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2054.2
062400              ELSE  PERFORM  PASS.                                IX2054.2
062500     PERFORM   PRINT-DETAIL.                                      IX2054.2
062600*                                                                 IX2054.2
062700*    03                                                           IX2054.2
062800*                                                                 IX2054.2
062900 READ-INIT-GF-03.                                                 IX2054.2
063000     MOVE  "READ-TEST-F1-03  "  TO PAR-NAME.                      IX2054.2
063100     MOVE  "START = READ  ALTERN."  TO FEATURE.                   IX2054.2
063200     MOVE    0000000200  TO WRK-DU-10V00-001.                     IX2054.2
063300     MOVE    WRK-REC-KEY-FD1  TO IX-FD1-REC-KEY.                  IX2054.2
063400     MOVE     FD1-FILE-SIZE TO WRK-DU-10V00-001.                  IX2054.2
063500     MOVE     0000000001 TO WRK-DU-10V00-002.                     IX2054.2
063600     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2054.2
063700     MOVE    WRK-ALT1-KEY-FD1  TO IX-FD1-ALT1-KEY.                IX2054.2
063800     MOVE    ZERO  TO WRK-DU-10V00-002.                           IX2054.2
063900     START    IX-FD1                                              IX2054.2
064000                     KEY IS EQUAL TO IX-FD1-ALTKEY1               IX2054.2
064100          INVALID KEY                                             IX2054.2
064200                          ADD 1000000  TO WRK-DU-10V00-002.       IX2054.2
064300*                                                                 IX2054.2
064400*    COMPUTED RESULTS VALUE IN INCREMENTS OF 1000000 INDICATE     IX2054.2
064500*        INVALID KEY PATH HAS BEEN TAKEN ON START STATEMENT.      IX2054.2
064600*                                                                 IX2054.2
064700 READ-TEST-F1-03.                                                 IX2054.2
064800     READ    IX-FD1                                               IX2054.2
064900        NEXT RECORD                                               IX2054.2
065000         AT END                                                   IX2054.2
065100              ADD   1000  TO WRK-DU-10V00-002.                    IX2054.2
065200*                                                                 IX2054.2
065300*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE AT END PATH  IX2054.2
065400*       HAS BEEN TAKEN ON THE READ STATEMENT.                     IX2054.2
065500*                                                                 IX2054.2
065600     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2054.2
065700     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2054.2
065800              ADD    1   TO WRK-DU-10V00-002.                     IX2054.2
065900*                                                                 IX2054.2
066000*     COMPUTED RESULTS IN INCREMENTS OF 1 INDICATE THAT           IX2054.2
066100*       RECORD  RETRIEVED WAS NOT THE ONE EXPECTED.               IX2054.2
066200*                                                                 IX2054.2
066300     SUBTRACT  1  FROM WRK-DU-10V00-001.                          IX2054.2
066400     ADD     1  TO WRK-CS-09V00-001.                              IX2054.2
066500     IF      WRK-CS-09V00-001  GREATER THAN 25                    IX2054.2
066600              NEXT  SENTENCE  ELSE                                IX2054.2
066700              GO TO READ-TEST-F1-03.                              IX2054.2
066800     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2054.2
066900              PERFORM  FAIL                                       IX2054.2
067000              MOVE    WRK-DU-10V00-002 TO COMPUTED-18V0           IX2054.2
067100              MOVE     ZERO TO CORRECT-18V0                       IX2054.2
067200              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2054.2
067300              ELSE    PERFORM  PASS.                              IX2054.2
067400     PERFORM   PRINT-DETAIL.                                      IX2054.2
067500*                                                                 IX2054.2
067600*    04                                                           IX2054.2
067700*                                                                 IX2054.2
067800 READ-INIT-GF-04.                                                 IX2054.2
067900     MOVE  "READ-TEST-F1-04  "  TO PAR-NAME.                      IX2054.2
068000     MOVE  "START >= READ  ALTERN."  TO FEATURE.                  IX2054.2
068100     MOVE    0000000200  TO WRK-DU-10V00-001.                     IX2054.2
068200     MOVE    WRK-REC-KEY-FD1  TO IX-FD1-REC-KEY.                  IX2054.2
068300     MOVE     FD1-FILE-SIZE TO WRK-DU-10V00-001.                  IX2054.2
068400     MOVE     0000000001 TO WRK-DU-10V00-002.                     IX2054.2
068500     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2054.2
068600     MOVE    WRK-ALT1-KEY-FD1  TO IX-FD1-ALT1-KEY.                IX2054.2
068700     MOVE    ZERO  TO WRK-DU-10V00-002.                           IX2054.2
068800     START    IX-FD1                                              IX2054.2
068900        KEY IS GREATER THAN OR EQUAL TO IX-FD1-ALTKEY1            IX2054.2
069000          INVALID KEY                                             IX2054.2
069100                          ADD 1000000  TO WRK-DU-10V00-002.       IX2054.2
069200*                                                                 IX2054.2
069300*    COMPUTED RESULTS VALUE IN INCREMENTS OF 1000000 INDICATE     IX2054.2
069400*        INVALID KEY PATH HAS BEEN TAKEN ON START STATEMENT.      IX2054.2
069500*                                                                 IX2054.2
069600 READ-TEST-F1-04.                                                 IX2054.2
069700     READ    IX-FD1                                               IX2054.2
069800        NEXT RECORD                                               IX2054.2
069900         AT END                                                   IX2054.2
070000              ADD   1000  TO WRK-DU-10V00-002.                    IX2054.2
070100*                                                                 IX2054.2
070200*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE AT END PATH  IX2054.2
070300*       HAS BEEN TAKEN ON THE READ STATEMENT.                     IX2054.2
070400*                                                                 IX2054.2
070500     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2054.2
070600     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2054.2
070700              ADD    1   TO WRK-DU-10V00-002.                     IX2054.2
070800*                                                                 IX2054.2
070900*     COMPUTED RESULTS IN INCREMENTS OF 1 INDICATE THAT           IX2054.2
071000*       RECORD  RETRIEVED WAS NOT THE ONE EXPECTED.               IX2054.2
071100*                                                                 IX2054.2
071200     SUBTRACT  1  FROM WRK-DU-10V00-001.                          IX2054.2
071300     ADD     1  TO WRK-CS-09V00-001.                              IX2054.2
071400     IF      WRK-CS-09V00-001  GREATER THAN 25                    IX2054.2
071500              NEXT  SENTENCE  ELSE                                IX2054.2
071600              GO TO READ-TEST-F1-04.                              IX2054.2
071700     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2054.2
071800              PERFORM  FAIL                                       IX2054.2
071900              MOVE    WRK-DU-10V00-002 TO COMPUTED-18V0           IX2054.2
072000              MOVE     ZERO TO CORRECT-18V0                       IX2054.2
072100              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2054.2
072200              ELSE    PERFORM  PASS.                              IX2054.2
072300     PERFORM   PRINT-DETAIL.                                      IX2054.2
072400     CLOSE    IX-FD1.                                             IX2054.2
072500 READ-EXIT-F1.                                                    IX2054.2
072600     EXIT.                                                        IX2054.2
072700 SECT-IX-01-002 SECTION.                                          IX2054.2
072800 WRITE-INIT-GF-02.                                                IX2054.2
072900     OPEN     OUTPUT   IX-FD2.                                    IX2054.2
073000     MOVE     ZERO TO  WRK-CS-09V00-001.                          IX2054.2
073100     MOVE     ZERO TO  WRK-DU-10V00-003.                          IX2054.2
073200     MOVE     "IX-FD2"  TO XFILE-NAME (2).                        IX2054.2
073300     MOVE     "R1-F-G"  TO XRECORD-NAME (2).                      IX2054.2
073400     MOVE     000001   TO XRECORD-NUMBER (2).                     IX2054.2
073500     MOVE     CCVS-PGM-ID  TO XPROGRAM-NAME (2).                  IX2054.2
073600     MOVE     000240     TO XRECORD-LENGTH (2).                   IX2054.2
073700     MOVE     "RC"      TO CHARS-OR-RECORDS (2).                  IX2054.2
073800     MOVE     0025    TO XBLOCK-SIZE (2).                         IX2054.2
073900     MOVE     0000200   TO RECORDS-IN-FILE (2).                   IX2054.2
074000     MOVE     "IX"     TO XFILE-ORGANIZATION (2).                 IX2054.2
074100     MOVE     "S"      TO XLABEL-TYPE (2).                        IX2054.2
074200     MOVE     000200    TO WRK-DU-10V00-004.                      IX2054.2
074300     MOVE     "FILE CREATED"  TO RE-MARK.                         IX2054.2
074400     MOVE     "WRITE-TEST-GF-02" TO PAR-NAME.                     IX2054.2
074500     MOVE     "DYNAMIC MODE"  TO FEATURE.                         IX2054.2
074600 WRITE-TEST-GF-02-R.                                              IX2054.2
074700     MOVE     XRECORD-NUMBER (2) TO WRK-DU-10V00-003.             IX2054.2
074800     MOVE     WRK-REC-KEY-FD2  TO XRECORD-KEY (2).                IX2054.2
074900     MOVE     WRK-ALT1-KEY-FD2  TO ALTERNATE-KEY1 (2).            IX2054.2
075000     MOVE     FILE-RECORD-INFO (2)  TO IX-FD2R1-F-G-240.          IX2054.2
075100     WRITE    IX-FD2R1-F-G-240                                    IX2054.2
075200              INVALID KEY GO TO WRITE-TEST-GF-02.                 IX2054.2
075300     IF       XRECORD-NUMBER (2)  NOT LESS THAN FD2-FILE-SIZE     IX2054.2
075400              GO TO    WRITE-TEST-GF-02.                          IX2054.2
075500     ADD      000001  TO XRECORD-NUMBER (2).                      IX2054.2
075600     SUBTRACT 000001  FROM  WRK-DU-10V00-004.                     IX2054.2
075700     GO TO    WRITE-TEST-GF-02-R.                                 IX2054.2
075800 WRITE-TEST-GF-02.                                                IX2054.2
075900     MOVE     XRECORD-NUMBER  (2)  TO COMPUTED-18V0.              IX2054.2
076000     MOVE     FD2-FILE-SIZE   TO CORRECT-18V0.                    IX2054.2
076100     IF       XRECORD-NUMBER (2)  EQUAL TO FD2-FILE-SIZE          IX2054.2
076200              PERFORM  PASS                                       IX2054.2
076300     ELSE                                                         IX2054.2
076400         MOVE     "FILE CREATION PREMATURE; IX-41" TO RE-MARK     IX2054.2
076500         PERFORM  FAIL.                                           IX2054.2
076600     PERFORM   PRINT-DETAIL.                                      IX2054.2
076700*                                                                 IX2054.2
076800*    02                                                           IX2054.2
076900*                                                                 IX2054.2
077000     CLOSE    IX-FD2.                                             IX2054.2
077100 READ-INIT-F1-05.                                                 IX2054.2
077200     OPEN     INPUT  IX-FD2.                                      IX2054.2
077300     MOVE     ZERO TO WRK-DU-10V00-003.                           IX2054.2
077400     MOVE     ZERO TO WRK-DU-10V00-004.                           IX2054.2
077500     MOVE     "  READ SEQUENTIAL"  TO FEATURE.                    IX2054.2
077600     MOVE     ZERO TO WRK-CS-09V00-001.                           IX2054.2
077700 READ-TEST-F1-05-3.                                               IX2054.2
077800     ADD      1   TO WRK-DU-10V00-003.                            IX2054.2
077900     READ     IX-FD2                                              IX2054.2
078000              NEXT RECORD                                         IX2054.2
078100         AT END                                                   IX2054.2
078200              ADD    1000  TO WRK-DU-10V00-004.                   IX2054.2
078300*                                                                 IX2054.2
078400*    COMPUTED RESULTS IN INCREMENTS OF 1000  INDICATE AT END PATH IX2054.2
078500*         HAS BEEN TAKEN.                                         IX2054.2
078600*                                                                 IX2054.2
078700     MOVE     IX-FD2R1-F-G-240  TO FILE-RECORD-INFO (2).          IX2054.2
078800     IF       XRECORD-NUMBER (2)  NOT EQUAL TO WRK-DU-10V00-003   IX2054.2
078900              ADD     1  TO WRK-DU-10V00-004.                     IX2054.2
079000*                                                                 IX2054.2
079100*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICATE THAT THE       IX2054.2
079200*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2054.2
079300*                                                                 IX2054.2
079400     ADD     000000001  TO WRK-CS-09V00-001.                      IX2054.2
079500     IF      WRK-CS-09V00-001  GREATER THAN  24                   IX2054.2
079600              NEXT  SENTENCE  ELSE                                IX2054.2
079700              GO TO   READ-TEST-F1-05-3.                          IX2054.2
079800 READ-TEST-F1-05.                                                 IX2054.2
079900     MOVE "READ-TEST-F1-05" TO PAR-NAME.                          IX2054.2
080000     IF       WRK-DU-10V00-004  GREATER THAN ZERO                 IX2054.2
080100              MOVE "RETRIEVED A NOT EXPECTED RECORD " TO RE-MARK  IX2054.2
080200              PERFORM  FAIL                                       IX2054.2
080300              MOVE    WRK-DU-10V00-004 TO COMPUTED-18V0           IX2054.2
080400              MOVE    ZERO TO CORRECT-18V0                        IX2054.2
080500              MOVE    "SEE  PROGRAM"  TO RE-MARK                  IX2054.2
080600              ELSE  PERFORM PASS.                                 IX2054.2
080700     PERFORM   PRINT-DETAIL.                                      IX2054.2
080800*                                                                 IX2054.2
080900*    06                                                           IX2054.2
081000*                                                                 IX2054.2
081100 READ-TEST-F1-06-4.                                               IX2054.2
081200     MOVE     ZERO TO WRK-DU-10V00-003.                           IX2054.2
081300     MOVE     ZERO TO WRK-DU-10V00-004.                           IX2054.2
081400     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2054.2
081500     MOVE     "  READ RANDOM"  TO FEATURE.                        IX2054.2
081600 READ-TEST-F1-06-5.                                               IX2054.2
081700     ADD      10  TO WRK-DU-10V00-003.                            IX2054.2
081800     MOVE     WRK-DU-10V00-003  TO IX-FD2-KEY                     IX2054.2
081900     READ     IX-FD2  RECORD                                      IX2054.2
082000          INVALID KEY                                             IX2054.2
082100              ADD   1000 TO WRK-DU-10V00-004.                     IX2054.2
082200*                                                                 IX2054.2
082300*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE INVALID KEY  IX2054.2
082400*       PATH HAS BEEN TAKEN.                                      IX2054.2
082500*                                                                 IX2054.2
082600     MOVE     IX-FD2R1-F-G-240  TO FILE-RECORD-INFO (2).          IX2054.2
082700     IF       XRECORD-NUMBER (2)  NOT EQUAL TO WRK-DU-10V00-003   IX2054.2
082800              ADD    1    TO WRK-DU-10V00-004.                    IX2054.2
082900*                                                                 IX2054.2
083000*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICAT THAT THE        IX2054.2
083100*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2054.2
083200*                                                                 IX2054.2
083300     ADD     1  TO WRK-CS-09V00-001.                              IX2054.2
083400     IF      WRK-CS-09V00-001  GREATER THAN   10                  IX2054.2
083500              NEXT SENTENCE  ELSE                                 IX2054.2
083600              GO TO    READ-TEST-F1-06-5.                         IX2054.2
083700 READ-TEST-F1-06.                                                 IX2054.2
083800     MOVE "READ-TEST-F1-06" TO PAR-NAME.                          IX2054.2
083900     IF       WRK-DU-10V00-004  GREATER THAN ZERO                 IX2054.2
084000              MOVE "RETRIEVED A NOT EXPECTED RECORD " TO RE-MARK  IX2054.2
084100              PERFORM  FAIL                                       IX2054.2
084200              MOVE    WRK-DU-10V00-004  TO COMPUTED-18V0          IX2054.2
084300              MOVE    ZERO  TO CORRECT-18V0                       IX2054.2
084400              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2054.2
084500              ELSE  PERFORM  PASS.                                IX2054.2
084600     PERFORM   PRINT-DETAIL.                                      IX2054.2
084700*                                                                 IX2054.2
084800*    07                                                           IX2054.2
084900*                                                                 IX2054.2
085000 READ-TEST-F1-07-6.                                               IX2054.2
085100     MOVE    00000200  TO WRK-DU-10V00-003.                       IX2054.2
085200     MOVE    WRK-REC-KEY-FD2  TO IX-FD2-REC-KEY.                  IX2054.2
085300     MOVE     FD2-FILE-SIZE TO WRK-DU-10V00-003.                  IX2054.2
085400     MOVE     000000001 TO WRK-DU-10V00-004.                      IX2054.2
085500     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2054.2
085600     MOVE    WRK-ALT1-KEY-FD2  TO IX-FD2-ALT1-KEY.                IX2054.2
085700     MOVE     "  READ ALTERNATE KEY"  TO FEATURE.                 IX2054.2
085800     MOVE    ZERO TO WRK-DU-10V00-004.                            IX2054.2
085900     START   IX-FD2                                               IX2054.2
086000                   KEY IS EQUAL TO IX-FD2-ALTKEY1                 IX2054.2
086100              INVALID KEY ADD 1000000  TO WRK-DU-10V00-004.       IX2054.2
086200*                                                                 IX2054.2
086300*    COMPUTED RESULTS VALUE IN INCREMENTS OF 1000000 INDICATE     IX2054.2
086400*        INVALID KEY PATH HAS BEEN TAKEN ON START STATEMENT.      IX2054.2
086500*                                                                 IX2054.2
086600 READ-TEST-F1-07-7.                                               IX2054.2
086700     READ    IX-FD2                                               IX2054.2
086800        NEXT RECORD                                               IX2054.2
086900         AT END                                                   IX2054.2
087000              ADD   1000  TO WRK-DU-10V00-004.                    IX2054.2
087100*                                                                 IX2054.2
087200*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE AT END PATH  IX2054.2
087300*       HAS BEEN TAKEN.                                           IX2054.2
087400*                                                                 IX2054.2
087500     MOVE     IX-FD2R1-F-G-240  TO FILE-RECORD-INFO (2).          IX2054.2
087600     IF       XRECORD-NUMBER (2)  NOT EQUAL TO WRK-DU-10V00-003   IX2054.2
087700              ADD    1   TO WRK-DU-10V00-004.                     IX2054.2
087800*                                                                 IX2054.2
087900*     COMPUTED RESULTS IN INCREMENTS OF 1 INDICATE THAT           IX2054.2
088000*       RECORD  RETRIEVED WAS NOT THE ONE EXPECTED.               IX2054.2
088100*                                                                 IX2054.2
088200     SUBTRACT  1  FROM WRK-DU-10V00-003.                          IX2054.2
088300     ADD     1  TO WRK-CS-09V00-001.                              IX2054.2
088400     IF      WRK-CS-09V00-001  GREATER THAN 25                    IX2054.2
088500              NEXT  SENTENCE  ELSE                                IX2054.2
088600              GO TO    READ-TEST-F1-07-7.                         IX2054.2
088700 READ-TEST-F1-07.                                                 IX2054.2
088800     MOVE "READ-TEST-F1-07" TO PAR-NAME.                          IX2054.2
088900     IF       WRK-DU-10V00-004  GREATER THAN ZERO                 IX2054.2
089000              PERFORM  FAIL                                       IX2054.2
089100              MOVE    WRK-DU-10V00-004 TO COMPUTED-18V0           IX2054.2
089200              MOVE     ZERO TO CORRECT-18V0                       IX2054.2
089300              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2054.2
089400              ELSE    PERFORM  PASS.                              IX2054.2
089500     PERFORM   PRINT-DETAIL.                                      IX2054.2
089600     CLOSE    IX-FD2.                                             IX2054.2
089700 INX-EXIT-002.                                                    IX2054.2
089800     EXIT.                                                        IX2054.2
089900 READ-INIT-F1-08.                                                 IX2054.2
090000     OPEN     INPUT    IX-FD1.                                    IX2054.2
090100     OPEN     INPUT   IX-FD2.                                     IX2054.2
090200     MOVE     SPACE TO FILE-RECORD-INFO (9).                      IX2054.2
090300     MOVE    SPACE TO FILE-RECORD-INFO (1).                       IX2054.2
090400     MOVE     ZERO TO WRK-DU-10V00-004.                           IX2054.2
090500     MOVE     SPACES   TO IX-FD1R1-F-G-240.                       IX2054.2
090600     MOVE     SPACES TO IX-FD2R1-F-G-240.                         IX2054.2
090700     MOVE    "SAME AREA" TO FEATURE.                              IX2054.2
090800 READ-TEST-F1-08-1.                                               IX2054.2
090900     READ    IX-FD1                                               IX2054.2
091000                NEXT RECORD                                       IX2054.2
091100                AT END                                            IX2054.2
091200              ADD     1000  TO WRK-DU-10V00-004.                  IX2054.2
091300     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2054.2
091400 READ-TEST-F1-08.                                                 IX2054.2
091500     MOVE "READ-TEST-F1-08" TO PAR-NAME.                          IX2054.2
091600     IF       XFILE-NAME (1)  EQUAL TO "IX-FD1"                   IX2054.2
091700              PERFORM  PASS                                       IX2054.2
091800              ELSE                                                IX2054.2
091900              MOVE "RETRIEVED A RECORD NOT EXPECTED " TO RE-MARK  IX2054.2
092000              PERFORM FAIL                                        IX2054.2
092100              MOVE    XFILE-NAME (1)  TO COMPUTED-A               IX2054.2
092200              MOVE     "IX-FD1"  TO CORRECT-A.                    IX2054.2
092300     PERFORM   PRINT-DETAIL.                                      IX2054.2
092400*                                                                 IX2054.2
092500*    09                                                           IX2054.2
092600*                                                                 IX2054.2
092700     READ    IX-FD2                                               IX2054.2
092800               NEXT RECORD                                        IX2054.2
092900                       AT END                                     IX2054.2
093000              ADD    1000 TO WRK-DU-10V00-004.                    IX2054.2
093100*                                                                 IX2054.2
093200*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE INVALID KEY  IX2054.2
093300*       PATH HAS BEEN TAKEN.                                      IX2054.2
093400*                                                                 IX2054.2
093500 READ-TEST-F1-09.                                                 IX2054.2
093600     MOVE "READ-TEST-F1-09" TO PAR-NAME.                          IX2054.2
093700     IF       WRK-DU-10V00-004    EQUAL TO ZERO                   IX2054.2
093800              PERFORM PASS                                        IX2054.2
093900            ELSE                                                  IX2054.2
094000              MOVE "RETRIEVED A RECORD NOT EXPECTED " TO RE-MARK  IX2054.2
094100              PERFORM FAIL                                        IX2054.2
094200              MOVE     WRK-DU-10V00-004  TO COMPUTED-A            IX2054.2
094300              MOVE      ZERO TO CORRECT-A                         IX2054.2
094400              MOVE      "SEE PROGRAM"  TO RE-MARK.                IX2054.2
094500     PERFORM   PRINT-DETAIL.                                      IX2054.2
094600*                                                                 IX2054.2
094700*    10                                                           IX2054.2
094800*                                                                 IX2054.2
094900 READ-TEST-F1-10.                                                 IX2054.2
095000     MOVE "READ-TEST-F1-10" TO PAR-NAME.                          IX2054.2
095100     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO  (9).          IX2054.2
095200*                                                                 IX2054.2
095300*    NOTE    IN TESTING THE SAME AREA CLAUSE THE RECORD AREA      IX2054.2
095400*       SHOULD BE SHARED BY BOTH FILES IX-FD1 AND IX-FD2,         IX2054.2
095500*       THEREFORE FILE IX-FD2 IS READ AND THE RECORD              IX2054.2
095600*       IDENTIFIED FOR IX-FD1 IS ACCESSED AND TESTED FOR          IX2054.2
095700*       EXPECTED PRESENCE OF IX-FD2 FILE RECORD CONTENTS.         IX2054.2
095800*                                                                 IX2054.2
095900     IF       XFILE-NAME (9)  EQUAL TO "IX-FD2"                   IX2054.2
096000              PERFORM   PASS                                      IX2054.2
096100              ELSE                                                IX2054.2
096200              PERFORM   FAIL                                      IX2054.2
096300              MOVE     XFILE-NAME (9)  TO COMPUTED-A              IX2054.2
096400              MOVE "IX-FD2" TO CORRECT-A                          IX2054.2
096500              MOVE    "SEE PROGRAM"  TO RE-MARK.                  IX2054.2
096600     PERFORM   PRINT-DETAIL.                                      IX2054.2
096700     CLOSE    IX-FD2.                                             IX2054.2
096800     CLOSE    IX-FD1.                                             IX2054.2
096900 INX-EXIT-003.                                                    IX2054.2
097000     EXIT.                                                        IX2054.2
097100 CCVS-EXIT SECTION.                                               IX2054.2
097200 CCVS-999999.                                                     IX2054.2
097300     GO TO CLOSE-FILES.                                           IX2054.2
      *END-OF,IX205A                                                                  
