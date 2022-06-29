      *HEADER,COBOL,IX206A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2064.2
000200 PROGRAM-ID.                                                      IX2064.2
000300     IX206A.                                                      IX2064.2
000400****************************************************************  IX2064.2
000500*                                                              *  IX2064.2
000600*    VALIDATION FOR:-                                          *  IX2064.2
000700*                                                              *  IX2064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2064.2
000900*                                                              *  IX2064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2064.2
001100*                                                              *  IX2064.2
001200*IX206A                                                           IX2064.2
001300******************************************************************IX2064.2
001400*    THE FUNCTION OF THIS PROGRAM IS TO TEST THE PERMISSIBLE      IX2064.2
001500*    SYNTACTICAL CONSTRUCTS OF COBOL ELEMENTS ASSOCIATED WITH     IX2064.2
001600*    LEVEL 2 OF INDEXED I-O.                                      IX2064.2
001700*    THE ELEMENTS TESTED IN THIS PROGRAM ARE:                     IX2064.2
001800*                                                                 IX2064.2
001900*    (1) ACCESS MODE DYNAMIC                                      IX2064.2
002000*    (2) ALTERNATE RECORD KEY WITHOUT THE DUPLICATES OPTION       IX2064.2
002100*    (3) RESERVE CLAUSE                                           IX2064.2
002200*    (4) SAME CLAUSE                                              IX2064.2
002300*    (5) BLOCK CONTAINS INTEGER-1 TO INTEGER-2 CLAUSE             IX2064.2
002400*    (6) VALUE OF IMPLEMENTOR-NAME SERIES.                        IX2064.2
002500*                                                                 IX2064.2
002600*    EACH ELEMENT TESTED WILL BE EXERCISED SEMANTICALLY BY THIS   IX2064.2
002700*    ROUTINE.  ONE FILE IS CREATED AND ACCESSED IN THE DYNAMIC    IX2064.2
002800*    ACCESS MODE AND THE 2ND FILE IS CREATED 2ND ACCESSED IN THE  IX2064.2
002900*    SEQUENTIAL ACCESS MODE.                                      IX2064.2
003000*                                                                 IX2064.2
003100*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2064.2
003200*                                                                 IX2064.2
003300*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2064.2
003400*                    CLAUSE FOR DATA FILE IX-FS1                  IX2064.2
003500*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2064.2
003600*                    CLAUSE FOR DATA FILE IX-FD2                  IX2064.2
003700*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2064.2
003800*                    CLAUSE FOR INDEX FILE IX-FS1                 IX2064.2
003900*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2064.2
004000*                    CLAUSE FOR INDEX FILE IX-FD2                 IX2064.2
004100*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2064.2
004200*             X-62   FOR RAW-DATA                                 IX2064.2
004300*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2064.2
004400*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2064.2
004500*             X-84   LABEL RECORDS  FOR PRINT-FILE                IX2064.2
004600*                                                                 IX2064.2
004700*        NOTE:  X-CARDS 44, 45, 62 AND 84     ARE OPTIONAL        IX2064.2
004800*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2064.2
004900*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2064.2
005000*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2064.2
005100*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2064.2
005200*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2064.2
005300*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2064.2
005400*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2064.2
005500*               THEY ARE AS FOLLOWS                               IX2064.2
005600*                                                                 IX2064.2
005700*                  P  SELECTS X-CARDS 62                          IX2064.2
005800*                  J  SELECTS X-CARDS 44 & 45                     IX2064.2
005900*                  C  SELECTS X-CARDS 84                          IX2064.2
006000*                                                                 IX2064.2
006100******************************************************            IX2064.2
006200 ENVIRONMENT DIVISION.                                            IX2064.2
006300 CONFIGURATION SECTION.                                           IX2064.2
006400 SOURCE-COMPUTER.                                                 IX2064.2
006500     XXXXX082.                                                    IX2064.2
006600 OBJECT-COMPUTER.                                                 IX2064.2
006700     XXXXX083.                                                    IX2064.2
006800 INPUT-OUTPUT SECTION.                                            IX2064.2
006900 FILE-CONTROL.                                                    IX2064.2
007000*    SELECT RAW-DATA   ASSIGN TO                                  IX2064.2
007100*    XXXXX062                                                     IX2064.2
007200*           ORGANIZATION IS INDEXED                               IX2064.2
007300*           ACCESS MODE IS RANDOM                                 IX2064.2
007400*           RECORD KEY IS RAW-DATA-KEY.                           IX2064.2
007500     SELECT PRINT-FILE ASSIGN TO                                  IX2064.2
007600     XXXXX055.                                                    IX2064.2
007700     SELECT   IX-FD1  ASSIGN TO                                   IX2064.2
007800     XXXXX024                                                     IX2064.2
007900*    XXXXX044                                                     IX2064.2
008000              RESERVE  3                                          IX2064.2
008100              ORGANIZATION IS INDEXED                             IX2064.2
008200              ACCESS DYNAMIC                                      IX2064.2
008300              RECORD  KEY IS IX-FD1-KEY                           IX2064.2
008400              ALTERNATE RECORD  IS IX-FD1-ALTKEY1.                IX2064.2
008500     SELECT   IX-FS1    ASSIGN TO                                 IX2064.2
008600     XXXXX025                                                     IX2064.2
008700*    XXXXX045                                                     IX2064.2
008800              ; RESERVE  4  AREAS                                 IX2064.2
008900              ; ACCESS MODE  IS SEQUENTIAL                        IX2064.2
009000              ORGANIZATION INDEXED                                IX2064.2
009100              RECORD KEY  IX-FS1-KEY                              IX2064.2
009200              ; ALTERNATE RECORD KEY         IX-FS1-ALTKEY1.      IX2064.2
009300 I-O-CONTROL.                                                     IX2064.2
009400     SAME   RECORD FOR   IX-FD1,  IX-FS1.                         IX2064.2
009500 DATA DIVISION.                                                   IX2064.2
009600 FILE SECTION.                                                    IX2064.2
009700*                                                                 IX2064.2
009800*FD  RAW-DATA.                                                    IX2064.2
009900*                                                                 IX2064.2
010000*01  RAW-DATA-SATZ.                                               IX2064.2
010100*    05  RAW-DATA-KEY        PIC X(6).                            IX2064.2
010200*    05  C-DATE              PIC 9(6).                            IX2064.2
010300*    05  C-TIME              PIC 9(8).                            IX2064.2
010400*    05  C-NO-OF-TESTS       PIC 99.                              IX2064.2
010500*    05  C-OK                PIC 999.                             IX2064.2
010600*    05  C-ALL               PIC 999.                             IX2064.2
010700*    05  C-FAIL              PIC 999.                             IX2064.2
010800*    05  C-DELETED           PIC 999.                             IX2064.2
010900*    05  C-INSPECT           PIC 999.                             IX2064.2
011000*    05  C-NOTE              PIC X(13).                           IX2064.2
011100*    05  C-INDENT            PIC X.                               IX2064.2
011200*    05  C-ABORT             PIC X(8).                            IX2064.2
011300 FD  PRINT-FILE.                                                  IX2064.2
011400 01  PRINT-REC PICTURE X(120).                                    IX2064.2
011500 01  DUMMY-RECORD PICTURE X(120).                                 IX2064.2
011600 FD  IX-FD1                                                       IX2064.2
011700C    LABEL  RECORDS ARE STANDARD                                  IX2064.2
011800     BLOCK    10  TO  20  RECORDS                                 IX2064.2
011900     RECORD CONTAINS  240  CHARACTERS.                            IX2064.2
012000 01  IX-FD1R1-F-G-240.                                            IX2064.2
012100     05 IX-FD1-REC-120           PIC X(120).                      IX2064.2
012200     05 IX-FD1-REC-120-240.                                       IX2064.2
012300              10 FILLER          PIC X(8).                        IX2064.2
012400              10 IX-FD1-REC-KEY.                                  IX2064.2
012500                 15 FILLER       PIC X(19).                       IX2064.2
012600                 15 IX-FD1-KEY   PIC X(10).                       IX2064.2
012700              10 FILLER          PIC X(9).                        IX2064.2
012800              10 IX-FD1-ALT1-KEY.                                 IX2064.2
012900                 15 FILLER       PIC X(19).                       IX2064.2
013000                 15 IX-FD1-ALTKEY1  PIC X(10).                    IX2064.2
013100              10 FILLER          PIC X(45).                       IX2064.2
013200 FD  IX-FS1                                                       IX2064.2
013300C    LABEL RECORDS ARE STANDARD                                   IX2064.2
013400     RECORD CONTAINS  240  CHARACTERS.                            IX2064.2
013500 01  IX-FS1R1-F-G-240.                                            IX2064.2
013600     05 IX-FS1-REC-120           PIC X(120).                      IX2064.2
013700     05 IX-FS1-REC-120-240.                                       IX2064.2
013800              10 FILLER          PIC X(8).                        IX2064.2
013900              10 IX-FS1-REC-KEY.                                  IX2064.2
014000                 15 FILLER       PIC X(19).                       IX2064.2
014100                 15 IX-FS1-KEY   PIC X(10).                       IX2064.2
014200              10 FILLER          PIC X(9).                        IX2064.2
014300              10 IX-FS1-ALT1-KEY.                                 IX2064.2
014400                 15 FILLER       PIC X(19).                       IX2064.2
014500                 15 IX-FS1-ALTKEY1 PIC X(10).                     IX2064.2
014600              10 FILLER          PIC X(45).                       IX2064.2
014700 WORKING-STORAGE SECTION.                                         IX2064.2
014800 01  WRK-CS-09V00-001            PIC S9(9)    COMPUTATIONAL.      IX2064.2
014900 01  WRK-REC-KEY-FD1.                                             IX2064.2
015000     03 FILLER                   PIC X(19)         VALUE SPACE.   IX2064.2
015100     03 WRK-DU-10V00-001         PIC 9(10)      VALUE ZERO.       IX2064.2
015200 01  WRK-ALT1-KEY-FD1.                                            IX2064.2
015300     03 FILLER                   PIC X(19)        VALUE SPACE.    IX2064.2
015400     03 WRK-DU-10V00-002         PIC 9(10)     VALUE ZERO.        IX2064.2
015500 01  FD1-FILE-SIZE               PIC 9(10)  VALUE 200.            IX2064.2
015600 01  WRK-REC-KEY-FS1.                                             IX2064.2
015700     03 FILLER                   PIC X(19)      VALUE SPACE.      IX2064.2
015800     03 WRK-DU-10V00-003         PIC 9(10)    VALUE ZERO.         IX2064.2
015900 01  WRK-ALT1-KEY-FS1.                                            IX2064.2
016000     03 FILLER                   PIC X(19)      VALUE SPACE.      IX2064.2
016100     03 WRK-DU-10V00-004         PIC 9(10)   VALUE ZERO.          IX2064.2
016200 01  FS1-FILE-SIZE               PIC 9(10)  VALUE 200.            IX2064.2
016300C01  IX-FD1-ID1                                                   IX2064.2
016400C    XXXXX086.                                                    IX2064.2
016500C01  IX-FD1-ID2                                                   IX2064.2
016600C    XXXXX087.                                                    IX2064.2
016700C01  IX-FS1-ID2                                                   IX2064.2
016800C    XXXXX088.                                                    IX2064.2
016900 01  FILE-RECORD-INFORMATION-REC.                                 IX2064.2
017000     03 FILE-RECORD-INFO-SKELETON.                                IX2064.2
017100        05 FILLER                 PICTURE X(48)       VALUE       IX2064.2
017200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2064.2
017300        05 FILLER                 PICTURE X(46)       VALUE       IX2064.2
017400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2064.2
017500        05 FILLER                 PICTURE X(26)       VALUE       IX2064.2
017600             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2064.2
017700        05 FILLER                 PICTURE X(37)       VALUE       IX2064.2
017800             ",RECKEY=                             ".             IX2064.2
017900        05 FILLER                 PICTURE X(38)       VALUE       IX2064.2
018000             ",ALTKEY1=                             ".            IX2064.2
018100        05 FILLER                 PICTURE X(38)       VALUE       IX2064.2
018200             ",ALTKEY2=                             ".            IX2064.2
018300        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2064.2
018400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2064.2
018500        05 FILE-RECORD-INFO-P1-120.                               IX2064.2
018600           07 FILLER              PIC X(5).                       IX2064.2
018700           07 XFILE-NAME           PIC X(6).                      IX2064.2
018800           07 FILLER              PIC X(8).                       IX2064.2
018900           07 XRECORD-NAME         PIC X(6).                      IX2064.2
019000           07 FILLER              PIC X(1).                       IX2064.2
019100           07 REELUNIT-NUMBER     PIC 9(1).                       IX2064.2
019200           07 FILLER              PIC X(7).                       IX2064.2
019300           07 XRECORD-NUMBER       PIC 9(6).                      IX2064.2
019400           07 FILLER              PIC X(6).                       IX2064.2
019500           07 UPDATE-NUMBER       PIC 9(2).                       IX2064.2
019600           07 FILLER              PIC X(5).                       IX2064.2
019700           07 ODO-NUMBER          PIC 9(4).                       IX2064.2
019800           07 FILLER              PIC X(5).                       IX2064.2
019900           07 XPROGRAM-NAME        PIC X(5).                      IX2064.2
020000           07 FILLER              PIC X(7).                       IX2064.2
020100           07 XRECORD-LENGTH       PIC 9(6).                      IX2064.2
020200           07 FILLER              PIC X(7).                       IX2064.2
020300           07 CHARS-OR-RECORDS    PIC X(2).                       IX2064.2
020400           07 FILLER              PIC X(1).                       IX2064.2
020500           07 XBLOCK-SIZE          PIC 9(4).                      IX2064.2
020600           07 FILLER              PIC X(6).                       IX2064.2
020700           07 RECORDS-IN-FILE     PIC 9(6).                       IX2064.2
020800           07 FILLER              PIC X(5).                       IX2064.2
020900           07 XFILE-ORGANIZATION   PIC X(2).                      IX2064.2
021000           07 FILLER              PIC X(6).                       IX2064.2
021100           07 XLABEL-TYPE          PIC X(1).                      IX2064.2
021200        05 FILE-RECORD-INFO-P121-240.                             IX2064.2
021300           07 FILLER              PIC X(8).                       IX2064.2
021400           07 XRECORD-KEY          PIC X(29).                     IX2064.2
021500           07 FILLER              PIC X(9).                       IX2064.2
021600           07 ALTERNATE-KEY1      PIC X(29).                      IX2064.2
021700           07 FILLER              PIC X(9).                       IX2064.2
021800           07 ALTERNATE-KEY2      PIC X(29).                      IX2064.2
021900           07 FILLER              PIC X(7).                       IX2064.2
022000 01  TEST-RESULTS.                                                IX2064.2
022100     02 FILLER                   PIC X      VALUE SPACE.          IX2064.2
022200     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2064.2
022300     02 FILLER                   PIC X      VALUE SPACE.          IX2064.2
022400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2064.2
022500     02 FILLER                   PIC X      VALUE SPACE.          IX2064.2
022600     02  PAR-NAME.                                                IX2064.2
022700       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2064.2
022800       03  PARDOT-X              PIC X      VALUE SPACE.          IX2064.2
022900       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2064.2
023000     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2064.2
023100     02 RE-MARK                  PIC X(61).                       IX2064.2
023200 01  TEST-COMPUTED.                                               IX2064.2
023300     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2064.2
023400     02 FILLER                   PIC X(17)  VALUE                 IX2064.2
023500            "       COMPUTED=".                                   IX2064.2
023600     02 COMPUTED-X.                                               IX2064.2
023700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2064.2
023800     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2064.2
023900                                 PIC -9(9).9(9).                  IX2064.2
024000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2064.2
024100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2064.2
024200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2064.2
024300     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2064.2
024400         04 COMPUTED-18V0                    PIC -9(18).          IX2064.2
024500         04 FILLER                           PIC X.               IX2064.2
024600     03 FILLER PIC X(50) VALUE SPACE.                             IX2064.2
024700 01  TEST-CORRECT.                                                IX2064.2
024800     02 FILLER PIC X(30) VALUE SPACE.                             IX2064.2
024900     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2064.2
025000     02 CORRECT-X.                                                IX2064.2
025100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2064.2
025200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2064.2
025300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2064.2
025400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2064.2
025500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2064.2
025600     03      CR-18V0 REDEFINES CORRECT-A.                         IX2064.2
025700         04 CORRECT-18V0                     PIC -9(18).          IX2064.2
025800         04 FILLER                           PIC X.               IX2064.2
025900     03 FILLER PIC X(2) VALUE SPACE.                              IX2064.2
026000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2064.2
026100 01  CCVS-C-1.                                                    IX2064.2
026200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2064.2
026300-    "SS  PARAGRAPH-NAME                                          IX2064.2
026400-    "       REMARKS".                                            IX2064.2
026500     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2064.2
026600 01  CCVS-C-2.                                                    IX2064.2
026700     02 FILLER                     PIC X        VALUE SPACE.      IX2064.2
026800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2064.2
026900     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2064.2
027000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2064.2
027100     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2064.2
027200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2064.2
027300 01  REC-CT                        PIC 99       VALUE ZERO.       IX2064.2
027400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2064.2
027500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2064.2
027600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2064.2
027700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2064.2
027800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2064.2
027900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2064.2
028000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2064.2
028100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2064.2
028200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2064.2
028300 01  CCVS-H-1.                                                    IX2064.2
028400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2064.2
028500     02  FILLER                    PIC X(42)    VALUE             IX2064.2
028600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2064.2
028700     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2064.2
028800 01  CCVS-H-2A.                                                   IX2064.2
028900   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2064.2
029000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2064.2
029100   02  FILLER                        PIC XXXX   VALUE             IX2064.2
029200     "4.2 ".                                                      IX2064.2
029300   02  FILLER                        PIC X(28)  VALUE             IX2064.2
029400            " COPY - NOT FOR DISTRIBUTION".                       IX2064.2
029500   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2064.2
029600                                                                  IX2064.2
029700 01  CCVS-H-2B.                                                   IX2064.2
029800   02  FILLER                        PIC X(15)  VALUE             IX2064.2
029900            "TEST RESULT OF ".                                    IX2064.2
030000   02  TEST-ID                       PIC X(9).                    IX2064.2
030100   02  FILLER                        PIC X(4)   VALUE             IX2064.2
030200            " IN ".                                               IX2064.2
030300   02  FILLER                        PIC X(12)  VALUE             IX2064.2
030400     " HIGH       ".                                              IX2064.2
030500   02  FILLER                        PIC X(22)  VALUE             IX2064.2
030600            " LEVEL VALIDATION FOR ".                             IX2064.2
030700   02  FILLER                        PIC X(58)  VALUE             IX2064.2
030800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2064.2
030900 01  CCVS-H-3.                                                    IX2064.2
031000     02  FILLER                      PIC X(34)  VALUE             IX2064.2
031100            " FOR OFFICIAL USE ONLY    ".                         IX2064.2
031200     02  FILLER                      PIC X(58)  VALUE             IX2064.2
031300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2064.2
031400     02  FILLER                      PIC X(28)  VALUE             IX2064.2
031500            "  COPYRIGHT   1985 ".                                IX2064.2
031600 01  CCVS-E-1.                                                    IX2064.2
031700     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2064.2
031800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2064.2
031900     02 ID-AGAIN                     PIC X(9).                    IX2064.2
032000     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2064.2
032100 01  CCVS-E-2.                                                    IX2064.2
032200     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2064.2
032300     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2064.2
032400     02 CCVS-E-2-2.                                               IX2064.2
032500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2064.2
032600         03 FILLER                   PIC X      VALUE SPACE.      IX2064.2
032700         03 ENDER-DESC               PIC X(44)  VALUE             IX2064.2
032800            "ERRORS ENCOUNTERED".                                 IX2064.2
032900 01  CCVS-E-3.                                                    IX2064.2
033000     02  FILLER                      PIC X(22)  VALUE             IX2064.2
033100            " FOR OFFICIAL USE ONLY".                             IX2064.2
033200     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2064.2
033300     02  FILLER                      PIC X(58)  VALUE             IX2064.2
033400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2064.2
033500     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2064.2
033600     02 FILLER                       PIC X(15)  VALUE             IX2064.2
033700             " COPYRIGHT 1985".                                   IX2064.2
033800 01  CCVS-E-4.                                                    IX2064.2
033900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2064.2
034000     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2064.2
034100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2064.2
034200     02 FILLER                       PIC X(40)  VALUE             IX2064.2
034300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2064.2
034400 01  XXINFO.                                                      IX2064.2
034500     02 FILLER                       PIC X(19)  VALUE             IX2064.2
034600            "*** INFORMATION ***".                                IX2064.2
034700     02 INFO-TEXT.                                                IX2064.2
034800       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2064.2
034900       04 XXCOMPUTED                 PIC X(20).                   IX2064.2
035000       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2064.2
035100       04 XXCORRECT                  PIC X(20).                   IX2064.2
035200     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2064.2
035300 01  HYPHEN-LINE.                                                 IX2064.2
035400     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2064.2
035500     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2064.2
035600-    "*****************************************".                 IX2064.2
035700     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2064.2
035800-    "******************************".                            IX2064.2
035900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2064.2
036000     "IX206A".                                                    IX2064.2
036100 PROCEDURE DIVISION.                                              IX2064.2
036200 CCVS1 SECTION.                                                   IX2064.2
036300 OPEN-FILES.                                                      IX2064.2
036400*    OPEN I-O RAW-DATA.                                           IX2064.2
036500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2064.2
036600*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2064.2
036700*    MOVE "ABORTED " TO C-ABORT.                                  IX2064.2
036800*    ADD 1 TO C-NO-OF-TESTS.                                      IX2064.2
036900*    ACCEPT C-DATE  FROM DATE.                                    IX2064.2
037000*    ACCEPT C-TIME  FROM TIME.                                    IX2064.2
037100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2064.2
037200*END-E-1.                                                         IX2064.2
037300*    CLOSE RAW-DATA.                                              IX2064.2
037400     OPEN    OUTPUT PRINT-FILE.                                   IX2064.2
037500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2064.2
037600     MOVE    SPACE TO TEST-RESULTS.                               IX2064.2
037700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2064.2
037800     MOVE    ZERO TO REC-SKL-SUB.                                 IX2064.2
037900     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2064.2
038000 CCVS-INIT-FILE.                                                  IX2064.2
038100     ADD     1 TO REC-SKL-SUB.                                    IX2064.2
038200     MOVE    FILE-RECORD-INFO-SKELETON                            IX2064.2
038300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2064.2
038400 CCVS-INIT-EXIT.                                                  IX2064.2
038500     GO TO CCVS1-EXIT.                                            IX2064.2
038600 CLOSE-FILES.                                                     IX2064.2
038700*    OPEN I-O RAW-DATA.                                           IX2064.2
038800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2064.2
038900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2064.2
039000*    MOVE "OK.     " TO C-ABORT.                                  IX2064.2
039100*    MOVE PASS-COUNTER TO C-OK.                                   IX2064.2
039200*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2064.2
039300*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2064.2
039400*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2064.2
039500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2064.2
039600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2064.2
039700*END-E-2.                                                         IX2064.2
039800*    CLOSE RAW-DATA.                                              IX2064.2
039900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2064.2
040000 TERMINATE-CCVS.                                                  IX2064.2
040100*    EXIT PROGRAM.                                                IX2064.2
040200*TERMINATE-CALL.                                                  IX2064.2
040300     STOP     RUN.                                                IX2064.2
040400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2064.2
040500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2064.2
040600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2064.2
040700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2064.2
040800     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2064.2
040900 PRINT-DETAIL.                                                    IX2064.2
041000     IF REC-CT NOT EQUAL TO ZERO                                  IX2064.2
041100             MOVE "." TO PARDOT-X                                 IX2064.2
041200             MOVE REC-CT TO DOTVALUE.                             IX2064.2
041300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2064.2
041400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2064.2
041500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2064.2
041600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2064.2
041700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2064.2
041800     MOVE SPACE TO CORRECT-X.                                     IX2064.2
041900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2064.2
042000     MOVE     SPACE TO RE-MARK.                                   IX2064.2
042100 HEAD-ROUTINE.                                                    IX2064.2
042200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2064.2
042300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2064.2
042400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2064.2
042500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2064.2
042600 COLUMN-NAMES-ROUTINE.                                            IX2064.2
042700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2064.2
042800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2064.2
042900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2064.2
043000 END-ROUTINE.                                                     IX2064.2
043100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2064.2
043200 END-RTN-EXIT.                                                    IX2064.2
043300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2064.2
043400 END-ROUTINE-1.                                                   IX2064.2
043500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2064.2
043600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2064.2
043700      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2064.2
043800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2064.2
043900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2064.2
044000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2064.2
044100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2064.2
044200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2064.2
044300  END-ROUTINE-12.                                                 IX2064.2
044400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2064.2
044500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2064.2
044600         MOVE "NO " TO ERROR-TOTAL                                IX2064.2
044700         ELSE                                                     IX2064.2
044800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2064.2
044900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2064.2
045000     PERFORM WRITE-LINE.                                          IX2064.2
045100 END-ROUTINE-13.                                                  IX2064.2
045200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2064.2
045300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2064.2
045400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2064.2
045500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2064.2
045600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2064.2
045700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2064.2
045800          MOVE "NO " TO ERROR-TOTAL                               IX2064.2
045900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2064.2
046000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2064.2
046100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2064.2
046200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2064.2
046300 WRITE-LINE.                                                      IX2064.2
046400     ADD 1 TO RECORD-COUNT.                                       IX2064.2
046500*    IF RECORD-COUNT GREATER 42                                   IX2064.2
046600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2064.2
046700*        MOVE SPACE TO DUMMY-RECORD                               IX2064.2
046800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2064.2
046900*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2064.2
047000*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2064.2
047100*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2064.2
047200*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2064.2
047300*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2064.2
047400*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2064.2
047500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2064.2
047600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2064.2
047700*        MOVE ZERO TO RECORD-COUNT.                               IX2064.2
047800     PERFORM WRT-LN.                                              IX2064.2
047900 WRT-LN.                                                          IX2064.2
048000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2064.2
048100     MOVE SPACE TO DUMMY-RECORD.                                  IX2064.2
048200 BLANK-LINE-PRINT.                                                IX2064.2
048300     PERFORM WRT-LN.                                              IX2064.2
048400 FAIL-ROUTINE.                                                    IX2064.2
048500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2064.2
048600            GO TO   FAIL-ROUTINE-WRITE.                           IX2064.2
048700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2064.2
048800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2064.2
048900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2064.2
049000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2064.2
049100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2064.2
049200     GO TO  FAIL-ROUTINE-EX.                                      IX2064.2
049300 FAIL-ROUTINE-WRITE.                                              IX2064.2
049400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2064.2
049500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2064.2
049600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2064.2
049700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2064.2
049800 FAIL-ROUTINE-EX. EXIT.                                           IX2064.2
049900 BAIL-OUT.                                                        IX2064.2
050000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2064.2
050100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2064.2
050200 BAIL-OUT-WRITE.                                                  IX2064.2
050300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2064.2
050400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2064.2
050500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2064.2
050600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2064.2
050700 BAIL-OUT-EX. EXIT.                                               IX2064.2
050800 CCVS1-EXIT.                                                      IX2064.2
050900     EXIT.                                                        IX2064.2
051000 SECT-IX-01-001  SECTION.                                         IX2064.2
051100 WRITE-INIT-GF-01.                                                IX2064.2
051200     OPEN     OUTPUT   IX-FD1.                                    IX2064.2
051300     MOVE     ZERO TO WRK-CS-09V00-001.                           IX2064.2
051400     MOVE     ZERO TO WRK-DU-10V00-001.                           IX2064.2
051500     MOVE     "IX-FD1"  TO XFILE-NAME (1).                        IX2064.2
051600     MOVE     "R1-F-G"  TO XRECORD-NAME (1).                      IX2064.2
051700     MOVE     000001    TO XRECORD-NUMBER (1).                    IX2064.2
051800     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   IX2064.2
051900     MOVE     200  TO RECORDS-IN-FILE (1).                        IX2064.2
052000     MOVE     240  TO XRECORD-LENGTH (1).                         IX2064.2
052100     MOVE     0020      TO XBLOCK-SIZE (1).                       IX2064.2
052200     MOVE     "RC"      TO CHARS-OR-RECORDS (1).                  IX2064.2
052300     MOVE     "IX"      TO XFILE-ORGANIZATION (1).                IX2064.2
052400     MOVE     "S"       TO XLABEL-TYPE (1).                       IX2064.2
052500     MOVE     000200    TO WRK-DU-10V00-002                       IX2064.2
052600     MOVE     "FILE CREATED"  TO RE-MARK.                         IX2064.2
052700 WRITE-TEST-GF-01-R.                                              IX2064.2
052800     MOVE    XRECORD-NUMBER (1) TO WRK-DU-10V00-001.              IX2064.2
052900     MOVE    WRK-REC-KEY-FD1  TO XRECORD-KEY (1).                 IX2064.2
053000     MOVE    WRK-ALT1-KEY-FD1  TO ALTERNATE-KEY1 (1).             IX2064.2
053100     MOVE     FILE-RECORD-INFO (1)  TO IX-FD1R1-F-G-240.          IX2064.2
053200     WRITE    IX-FD1R1-F-G-240                                    IX2064.2
053300                        INVALID KEY GO TO WRITE-TEST-GF-01.       IX2064.2
053400     IF     XRECORD-NUMBER (1)  NOT LESS THAN FD1-FILE-SIZE       IX2064.2
053500              GO TO    WRITE-TEST-GF-01.                          IX2064.2
053600     ADD      000001   TO XRECORD-NUMBER (1).                     IX2064.2
053700     SUBTRACT  000001   FROM WRK-DU-10V00-002.                    IX2064.2
053800     GO TO    WRITE-TEST-GF-01-R.                                 IX2064.2
053900 WRITE-TEST-GF-01.                                                IX2064.2
054000     MOVE     "WRITE-TEST-GF-01" TO PAR-NAME.                     IX2064.2
054100     MOVE     "WRITE  IX-FD1" TO FEATURE.                         IX2064.2
054200     MOVE     XRECORD-NUMBER  (1)  TO COMPUTED-18V0.              IX2064.2
054300     MOVE     FD1-FILE-SIZE   TO CORRECT-18V0.                    IX2064.2
054400     IF       XRECORD-NUMBER (1)  EQUAL TO FD1-FILE-SIZE          IX2064.2
054500              PERFORM  PASS                                       IX2064.2
054600        ELSE                                                      IX2064.2
054700            MOVE     "FILE CREATION PREMATURE" TO RE-MARK         IX2064.2
054800            PERFORM  FAIL.                                        IX2064.2
054900     PERFORM   PRINT-DETAIL.                                      IX2064.2
055000*                                                                 IX2064.2
055100*                                                                 IX2064.2
055200     CLOSE  IX-FD1.                                               IX2064.2
055300 READ-INIT-F1-01.                                                 IX2064.2
055400     OPEN     INPUT  IX-FD1.                                      IX2064.2
055500     MOVE     ZERO TO WRK-DU-10V00-001.                           IX2064.2
055600     MOVE     ZERO TO WRK-DU-10V00-002.                           IX2064.2
055700     MOVE    ZERO TO WRK-CS-09V00-001.                            IX2064.2
055800 READ-TEST-F1-01-3.                                               IX2064.2
055900     ADD      1   TO WRK-DU-10V00-001.                            IX2064.2
056000     READ    IX-FD1                                               IX2064.2
056100        NEXT RECORD                                               IX2064.2
056200         AT END                                                   IX2064.2
056300              ADD    1000  TO WRK-DU-10V00-002.                   IX2064.2
056400*                                                                 IX2064.2
056500*    COMPUTED RESULTS IN INCREMENTS OF 1000  INDICATE AT END PATH IX2064.2
056600*         HAS BEEN TAKEN.                                         IX2064.2
056700*                                                                 IX2064.2
056800     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2064.2
056900     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2064.2
057000              ADD     1  TO WRK-DU-10V00-002.                     IX2064.2
057100*                                                                 IX2064.2
057200*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICATE THAT THE       IX2064.2
057300*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2064.2
057400*                                                                 IX2064.2
057500     ADD     1  TO WRK-CS-09V00-001.                              IX2064.2
057600     IF      WRK-CS-09V00-001  GREATER THAN 24                    IX2064.2
057700           GO TO READ-TEST-F1-01.                                 IX2064.2
057800     GO TO   READ-TEST-F1-01-3.                                   IX2064.2
057900 READ-TEST-F1-01.                                                 IX2064.2
058000     MOVE     "READ-TEST-F1-01 " TO PAR-NAME.                     IX2064.2
058100     MOVE     "READ SEQUENTIAL" TO FEATURE.                       IX2064.2
058200     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2064.2
058300              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
058400              PERFORM  FAIL                                       IX2064.2
058500              MOVE    WRK-DU-10V00-002 TO COMPUTED-18V0           IX2064.2
058600              MOVE    ZERO TO CORRECT-18V0                        IX2064.2
058700              MOVE    "SEE  PROGRAM"  TO RE-MARK                  IX2064.2
058800        ELSE                                                      IX2064.2
058900              PERFORM  PASS.                                      IX2064.2
059000     PERFORM   PRINT-DETAIL.                                      IX2064.2
059100*                                                                 IX2064.2
059200*                                                                 IX2064.2
059300 READ-INIT-F2-02.                                                 IX2064.2
059400     MOVE     ZERO TO WRK-DU-10V00-001.                           IX2064.2
059500     MOVE    ZERO TO WRK-CS-09V00-001.                            IX2064.2
059600     MOVE     ZERO TO WRK-DU-10V00-002.                           IX2064.2
059700 READ-TEST-F2-02-5.                                               IX2064.2
059800     ADD      10  TO WRK-DU-10V00-001.                            IX2064.2
059900     MOVE     WRK-DU-10V00-001  TO IX-FD1-KEY                     IX2064.2
060000     READ     IX-FD1  RECORD                                      IX2064.2
060100          INVALID KEY                                             IX2064.2
060200              ADD   1000 TO WRK-DU-10V00-002.                     IX2064.2
060300*                                                                 IX2064.2
060400*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE INVALID KEY  IX2064.2
060500*       PATH HAS BEEN TAKEN.                                      IX2064.2
060600*                                                                 IX2064.2
060700     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2064.2
060800     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2064.2
060900              ADD    1    TO WRK-DU-10V00-002.                    IX2064.2
061000*                                                                 IX2064.2
061100*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICAT THAT THE        IX2064.2
061200*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2064.2
061300*                                                                 IX2064.2
061400     ADD     1  TO WRK-CS-09V00-001.                              IX2064.2
061500     IF      WRK-CS-09V00-001  GREATER THAN   10                  IX2064.2
061600              NEXT SENTENCE  ELSE                                 IX2064.2
061700              GO TO    READ-TEST-F2-02-5.                         IX2064.2
061800 READ-TEST-F2-02.                                                 IX2064.2
061900     MOVE      "READ-TEST-F2-02" TO PAR-NAME.                     IX2064.2
062000     MOVE "READ RANDOM    " TO FEATURE.                           IX2064.2
062100     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2064.2
062200              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
062300              PERFORM  FAIL                                       IX2064.2
062400              MOVE    WRK-DU-10V00-002  TO COMPUTED-18V0          IX2064.2
062500              MOVE    ZERO  TO CORRECT-18V0                       IX2064.2
062600              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2064.2
062700        ELSE                                                      IX2064.2
062800              PERFORM  PASS.                                      IX2064.2
062900     PERFORM   PRINT-DETAIL.                                      IX2064.2
063000*                                                                 IX2064.2
063100*                                                                 IX2064.2
063200 READ-INIT-F2-03.                                                 IX2064.2
063300     MOVE    0000000200  TO WRK-DU-10V00-001.                     IX2064.2
063400     MOVE   WRK-REC-KEY-FD1 TO IX-FD1-REC-KEY.                    IX2064.2
063500     MOVE     FD1-FILE-SIZE TO WRK-DU-10V00-001.                  IX2064.2
063600     MOVE     0000000001 TO WRK-DU-10V00-002.                     IX2064.2
063700     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2064.2
063800     MOVE    WRK-ALT1-KEY-FD1  TO IX-FD1-ALT1-KEY.                IX2064.2
063900     MOVE    ZERO  TO WRK-DU-10V00-002.                           IX2064.2
064000     START    IX-FD1                                              IX2064.2
064100                     KEY IS EQUAL TO IX-FD1-ALTKEY1               IX2064.2
064200          INVALID KEY                                             IX2064.2
064300                          ADD 1000000  TO WRK-DU-10V00-002.       IX2064.2
064400*                                                                 IX2064.2
064500*    COMPUTED RESULTS VALUE IN INCREMENTS OF 1000000 INDICATE     IX2064.2
064600*        INVALID KEY PATH HAS BEEN TAKEN ON START STATEMENT.      IX2064.2
064700*                                                                 IX2064.2
064800 READ-TEST-F2-03-7.                                               IX2064.2
064900     READ    IX-FD1                                               IX2064.2
065000        NEXT RECORD                                               IX2064.2
065100         AT END                                                   IX2064.2
065200              ADD   1000  TO WRK-DU-10V00-002.                    IX2064.2
065300*                                                                 IX2064.2
065400*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE AT END PATH  IX2064.2
065500*       HAS BEEN TAKEN ON THE READ STATEMENT.                     IX2064.2
065600*                                                                 IX2064.2
065700     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2064.2
065800     IF       XRECORD-NUMBER (1)  NOT EQUAL TO WRK-DU-10V00-001   IX2064.2
065900              ADD    1   TO WRK-DU-10V00-002.                     IX2064.2
066000*                                                                 IX2064.2
066100*     COMPUTED RESULTS IN INCREMENTS OF 1 INDICATE THAT           IX2064.2
066200*       RECORD  RETRIEVED WAS NOT THE ONE EXPECTED.               IX2064.2
066300*                                                                 IX2064.2
066400     SUBTRACT  1  FROM WRK-DU-10V00-001.                          IX2064.2
066500     ADD     1  TO WRK-CS-09V00-001.                              IX2064.2
066600     IF      WRK-CS-09V00-001  GREATER THAN 25                    IX2064.2
066700              NEXT  SENTENCE  ELSE                                IX2064.2
066800              GO TO    READ-TEST-F2-03-7.                         IX2064.2
066900 READ-TEST-F2-03.                                                 IX2064.2
067000     MOVE      "READ-TEST-F2-03" TO PAR-NAME.                     IX2064.2
067100     MOVE "READ ALTERNATE KEY " TO FEATURE.                       IX2064.2
067200     IF       WRK-DU-10V00-002  GREATER THAN ZERO                 IX2064.2
067300              PERFORM  FAIL                                       IX2064.2
067400              MOVE    WRK-DU-10V00-002 TO COMPUTED-18V0           IX2064.2
067500              MOVE     ZERO TO CORRECT-18V0                       IX2064.2
067600              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2064.2
067700        ELSE                                                      IX2064.2
067800              PERFORM  PASS.                                      IX2064.2
067900     PERFORM   PRINT-DETAIL.                                      IX2064.2
068000     CLOSE    IX-FD1.                                             IX2064.2
068100 INX-EXIT-001.                                                    IX2064.2
068200     EXIT.                                                        IX2064.2
068300 SECT-IX-01-002 SECTION.                                          IX2064.2
068400 WRITE-INIT-GF-02.                                                IX2064.2
068500     OPEN     OUTPUT   IX-FS1.                                    IX2064.2
068600     MOVE     ZERO TO  WRK-CS-09V00-001.                          IX2064.2
068700     MOVE     ZERO TO  WRK-DU-10V00-003.                          IX2064.2
068800     MOVE     "IX-FS1"  TO XFILE-NAME (2).                        IX2064.2
068900     MOVE     "R1-F-G"  TO XRECORD-NAME (2).                      IX2064.2
069000     MOVE     000001   TO XRECORD-NUMBER (2).                     IX2064.2
069100     MOVE     CCVS-PGM-ID   TO XPROGRAM-NAME (2).                 IX2064.2
069200     MOVE     000240     TO XRECORD-LENGTH (2).                   IX2064.2
069300     MOVE     "RC"      TO CHARS-OR-RECORDS (2).                  IX2064.2
069400     MOVE     0001    TO XBLOCK-SIZE (2).                         IX2064.2
069500     MOVE     0000200   TO RECORDS-IN-FILE (2).                   IX2064.2
069600     MOVE     "IX"     TO XFILE-ORGANIZATION (2).                 IX2064.2
069700     MOVE     "S"      TO XLABEL-TYPE (2).                        IX2064.2
069800     MOVE     000200    TO WRK-DU-10V00-004.                      IX2064.2
069900     MOVE     "FILE CREATED"  TO RE-MARK.                         IX2064.2
070000     MOVE    "SEQUENTIAL MODE"  TO FEATURE.                       IX2064.2
070100     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX2064.2
070200 WRITE-TEST-GF-02-R.                                              IX2064.2
070300     MOVE    XRECORD-NUMBER (2) TO WRK-DU-10V00-003.              IX2064.2
070400     MOVE    WRK-REC-KEY-FS1  TO XRECORD-KEY (2).                 IX2064.2
070500     MOVE    WRK-ALT1-KEY-FS1  TO ALTERNATE-KEY1 (2).             IX2064.2
070600     MOVE     FILE-RECORD-INFO (2)  TO IX-FS1R1-F-G-240.          IX2064.2
070700     WRITE    IX-FS1R1-F-G-240                                    IX2064.2
070800              INVALID KEY GO TO WRITE-TEST-GF-02.                 IX2064.2
070900     IF     XRECORD-NUMBER (2)  NOT LESS THAN FS1-FILE-SIZE       IX2064.2
071000              GO TO    WRITE-TEST-GF-02.                          IX2064.2
071100     ADD      000001  TO XRECORD-NUMBER (2).                      IX2064.2
071200     SUBTRACT   000001  FROM  WRK-DU-10V00-004.                   IX2064.2
071300     GO TO    WRITE-TEST-GF-02-R.                                 IX2064.2
071400 WRITE-TEST-GF-02.                                                IX2064.2
071500     MOVE     XRECORD-NUMBER  (2)  TO COMPUTED-18V0.              IX2064.2
071600     MOVE     FS1-FILE-SIZE   TO CORRECT-18V0.                    IX2064.2
071700     IF       XRECORD-NUMBER (2)  EQUAL TO FS1-FILE-SIZE          IX2064.2
071800              PERFORM  PASS                                       IX2064.2
071900        ELSE                                                      IX2064.2
072000              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
072100              PERFORM  FAIL.                                      IX2064.2
072200     PERFORM   PRINT-DETAIL.                                      IX2064.2
072300*                                                                 IX2064.2
072400*                                                                 IX2064.2
072500     CLOSE    IX-FS1.                                             IX2064.2
072600 READ-INIT-F1-04.                                                 IX2064.2
072700     OPEN     INPUT  IX-FS1.                                      IX2064.2
072800     MOVE     ZERO TO WRK-DU-10V00-003.                           IX2064.2
072900     MOVE     ZERO TO WRK-DU-10V00-004.                           IX2064.2
073000     MOVE    ZERO TO WRK-CS-09V00-001.                            IX2064.2
073100 READ-TEST-F1-04-3.                                               IX2064.2
073200     ADD      1   TO WRK-DU-10V00-003.                            IX2064.2
073300     READ    IX-FS1                                               IX2064.2
073400         AT END                                                   IX2064.2
073500              ADD    1000  TO WRK-DU-10V00-004.                   IX2064.2
073600*                                                                 IX2064.2
073700*    COMPUTED RESULTS IN INCREMENTS OF 1000  INDICATE AT END PATH IX2064.2
073800*         HAS BEEN TAKEN.                                         IX2064.2
073900*                                                                 IX2064.2
074000     MOVE     IX-FS1R1-F-G-240  TO FILE-RECORD-INFO (2).          IX2064.2
074100     IF       XRECORD-NUMBER (2)  NOT EQUAL TO WRK-DU-10V00-003   IX2064.2
074200              ADD     1  TO WRK-DU-10V00-004.                     IX2064.2
074300*                                                                 IX2064.2
074400*    COMPUTED RESULTS IN INCREMENTS OF 1  INDICATE THAT THE       IX2064.2
074500*       RECORD RETRIEVED WAS NOT THE ONE EXPECTED.                IX2064.2
074600*                                                                 IX2064.2
074700     ADD     000000001  TO WRK-CS-09V00-001.                      IX2064.2
074800     IF      WRK-CS-09V00-001  GREATER THAN  24                   IX2064.2
074900              NEXT  SENTENCE  ELSE                                IX2064.2
075000              GO TO   READ-TEST-F1-04-3.                          IX2064.2
075100 READ-TEST-F1-04.                                                 IX2064.2
075200     MOVE     "READE-TEST-F1-04" TO PAR-NAME.                     IX2064.2
075300     MOVE "READ SEQUENTIAL    " TO FEATURE.                       IX2064.2
075400     IF       WRK-DU-10V00-004  GREATER THAN ZERO                 IX2064.2
075500              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
075600              PERFORM  FAIL                                       IX2064.2
075700              MOVE    WRK-DU-10V00-004 TO COMPUTED-18V0           IX2064.2
075800              MOVE    ZERO TO CORRECT-18V0                        IX2064.2
075900              MOVE    "SEE  PROGRAM"  TO RE-MARK                  IX2064.2
076000        ELSE                                                      IX2064.2
076100              PERFORM  PASS.                                      IX2064.2
076200     PERFORM   PRINT-DETAIL.                                      IX2064.2
076300*                                                                 IX2064.2
076400*                                                                 IX2064.2
076500 READ-TEST-F2-05-4.                                               IX2064.2
076600     MOVE     ZERO TO WRK-DU-10V00-003.                           IX2064.2
076700     MOVE     ZERO TO WRK-DU-10V00-004.                           IX2064.2
076800     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2064.2
076900 READ-TEST-F2-05-5.                                               IX2064.2
077000     MOVE    00000200  TO WRK-DU-10V00-003.                       IX2064.2
077100     MOVE    WRK-REC-KEY-FS1  TO IX-FS1-REC-KEY.                  IX2064.2
077200     MOVE     FS1-FILE-SIZE TO WRK-DU-10V00-003.                  IX2064.2
077300     MOVE     000000001 TO WRK-DU-10V00-004.                      IX2064.2
077400     MOVE    ZERO  TO  WRK-CS-09V00-001.                          IX2064.2
077500     MOVE    WRK-ALT1-KEY-FS1  TO IX-FS1-ALT1-KEY.                IX2064.2
077600     MOVE    ZERO TO WRK-DU-10V00-004.                            IX2064.2
077700     START   IX-FS1                                               IX2064.2
077800                   KEY IS EQUAL TO IX-FS1-ALTKEY1                 IX2064.2
077900              INVALID KEY ADD 1000000  TO WRK-DU-10V00-004.       IX2064.2
078000*                                                                 IX2064.2
078100*    COMPUTED RESULTS VALUE IN INCREMENTS OF 1000000 INDICATE     IX2064.2
078200*        INVALID KEY PATH HAS BEEN TAKEN ON START STATEMENT.      IX2064.2
078300*                                                                 IX2064.2
078400 READ-TEST-F2-05-6.                                               IX2064.2
078500     READ    IX-FS1                                               IX2064.2
078600         AT END                                                   IX2064.2
078700              ADD   1000  TO WRK-DU-10V00-004.                    IX2064.2
078800*                                                                 IX2064.2
078900*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE AT END PATH  IX2064.2
079000*       HAS BEEN TAKEN.                                           IX2064.2
079100*                                                                 IX2064.2
079200     MOVE     IX-FS1R1-F-G-240  TO FILE-RECORD-INFO (2).          IX2064.2
079300     IF       XRECORD-NUMBER (2)  NOT EQUAL TO WRK-DU-10V00-003   IX2064.2
079400              ADD    1   TO WRK-DU-10V00-004.                     IX2064.2
079500*                                                                 IX2064.2
079600*     COMPUTED RESULTS IN INCREMENTS OF 1 INDICATE THAT           IX2064.2
079700*       RECORD  RETRIEVED WAS NOT THE ONE EXPECTED.               IX2064.2
079800*                                                                 IX2064.2
079900     SUBTRACT  1  FROM WRK-DU-10V00-003.                          IX2064.2
080000     ADD     1  TO WRK-CS-09V00-001.                              IX2064.2
080100     IF      WRK-CS-09V00-001  GREATER THAN 25                    IX2064.2
080200              NEXT  SENTENCE  ELSE                                IX2064.2
080300              GO TO    READ-TEST-F2-05-6.                         IX2064.2
080400 READ-TEST-F2-05.                                                 IX2064.2
080500     MOVE      "READ-TEST-F2-05" TO PAR-NAME.                     IX2064.2
080600     MOVE "READ ALTERNATE KEY " TO FEATURE.                       IX2064.2
080700     IF       WRK-DU-10V00-004  GREATER THAN ZERO                 IX2064.2
080800              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
080900              PERFORM  FAIL                                       IX2064.2
081000              MOVE    WRK-DU-10V00-004 TO COMPUTED-18V0           IX2064.2
081100              MOVE     ZERO TO CORRECT-18V0                       IX2064.2
081200              MOVE    "SEE PROGRAM"  TO RE-MARK                   IX2064.2
081300        ELSE                                                      IX2064.2
081400              PERFORM  PASS.                                      IX2064.2
081500     PERFORM   PRINT-DETAIL.                                      IX2064.2
081600     CLOSE    IX-FS1.                                             IX2064.2
081700 INX-EXIT-002.                                                    IX2064.2
081800     EXIT.                                                        IX2064.2
081900 READ-INIT-F1-06.                                                 IX2064.2
082000     OPEN     INPUT    IX-FD1.                                    IX2064.2
082100     OPEN     INPUT   IX-FS1.                                     IX2064.2
082200     MOVE     SPACE TO FILE-RECORD-INFO (9).                      IX2064.2
082300     MOVE    SPACE TO FILE-RECORD-INFO (1).                       IX2064.2
082400     MOVE     ZERO TO WRK-DU-10V00-004.                           IX2064.2
082500     MOVE     SPACES   TO IX-FD1R1-F-G-240.                       IX2064.2
082600     MOVE     SPACES TO IX-FS1R1-F-G-240.                         IX2064.2
082700     MOVE    "READ-TEST-F1-06" TO PAR-NAME.                       IX2064.2
082800     MOVE    "SAME AREA" TO FEATURE.                              IX2064.2
082900 READ-TEST-F1-06.                                                 IX2064.2
083000     READ    IX-FD1                                               IX2064.2
083100                NEXT RECORD                                       IX2064.2
083200                AT END                                            IX2064.2
083300              ADD     1000  TO WRK-DU-10V00-004.                  IX2064.2
083400     MOVE     IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).          IX2064.2
083500     IF       XFILE-NAME (1)  EQUAL TO "IX-FD1"                   IX2064.2
083600              PERFORM  PASS                                       IX2064.2
083700           ELSE                                                   IX2064.2
083800              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
083900              PERFORM FAIL                                        IX2064.2
084000              MOVE    XFILE-NAME (1)  TO COMPUTED-A               IX2064.2
084100              MOVE     "IX-FD1"  TO CORRECT-A.                    IX2064.2
084200     PERFORM  PRINT-DETAIL.                                       IX2064.2
084300*                                                                 IX2064.2
084400*                                                                 IX2064.2
084500 READ-TEST-F1-07.                                                 IX2064.2
084600     MOVE     "READ-TEST-F1-07 " TO PAR-NAME.                     IX2064.2
084700     MOVE "SAME AREA          " TO FEATURE.                       IX2064.2
084800     READ    IX-FS1                                               IX2064.2
084900                       AT END                                     IX2064.2
085000              ADD    1000 TO WRK-DU-10V00-004.                    IX2064.2
085100*                                                                 IX2064.2
085200*    COMPUTED RESULTS IN INCREMENTS OF 1000 INDICATE INVALID KEY  IX2064.2
085300*       PATH HAS BEEN TAKEN.                                      IX2064.2
085400*                                                                 IX2064.2
085500     IF       WRK-DU-10V00-004    EQUAL TO ZERO                   IX2064.2
085600              PERFORM PASS                                        IX2064.2
085700            ELSE                                                  IX2064.2
085800              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
085900              PERFORM FAIL                                        IX2064.2
086000              MOVE     WRK-DU-10V00-004  TO COMPUTED-A            IX2064.2
086100              MOVE      ZERO TO CORRECT-A                         IX2064.2
086200              MOVE      "SEE PROGRAM"  TO RE-MARK.                IX2064.2
086300     PERFORM PRINT-DETAIL.                                        IX2064.2
086400*                                                                 IX2064.2
086500*                                                                 IX2064.2
086600 READ-TEST-F1-08.                                                 IX2064.2
086700     MOVE "READ-TEST-F1-08    "  TO PAR-NAME.                     IX2064.2
086800     MOVE "SAME AREA          " TO FEATURE.                       IX2064.2
086900     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO  (9).          IX2064.2
087000*                                                                 IX2064.2
087100*    NOTE    IN TESTING THE SAME AREA CLAUSE THE RECORD AREA      IX2064.2
087200*       SHOULD BE SHARED BY BOTH FILES IX-FD1 AND IX-FS1,         IX2064.2
087300*       THEREFORE FILE IX-FS1 IS READ AND THE RECORD              IX2064.2
087400*       IDENTIFIED FOR IX-FD1 IS ACCESSED AND TESTED FOR          IX2064.2
087500*       EXPECTED PRESENCE OF IX-FS1 FILE RECORD CONTENTS.         IX2064.2
087600*                                                                 IX2064.2
087700     IF       XFILE-NAME (9)  EQUAL TO "IX-FS1"                   IX2064.2
087800              PERFORM   PASS                                      IX2064.2
087900              ELSE                                                IX2064.2
088000              MOVE     "FILE CREATION PREMATURE" TO RE-MARK       IX2064.2
088100              PERFORM   FAIL                                      IX2064.2
088200              MOVE     XFILE-NAME (9)  TO COMPUTED-A              IX2064.2
088300              MOVE "IX-FS1" TO CORRECT-A                          IX2064.2
088400              MOVE    "SEE PROGRAM"  TO RE-MARK.                  IX2064.2
088500     PERFORM PRINT-DETAIL.                                        IX2064.2
088600     CLOSE    IX-FS1.                                             IX2064.2
088700     CLOSE    IX-FD1.                                             IX2064.2
088800 INX-EXIT-003.                                                    IX2064.2
088900     EXIT.                                                        IX2064.2
089000 CCVS-EXIT SECTION.                                               IX2064.2
089100 CCVS-999999.                                                     IX2064.2
089200     GO TO CLOSE-FILES.                                           IX2064.2
      *END-OF,IX206A                                                                  
