      *HEADER,COBOL,IX204A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2044.2
000200 PROGRAM-ID.                                                      IX2044.2
000300     IX204A.                                                      IX2044.2
000400****************************************************************  IX2044.2
000500*                                                              *  IX2044.2
000600*    VALIDATION FOR:-                                          *  IX2044.2
000700*                                                              *  IX2044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2044.2
000900*                                                              *  IX2044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2044.2
001100*                                                              *  IX2044.2
001200****************************************************************  IX2044.2
001300*IX204A                                                           IX2044.2
001400******************************************************************IX2044.2
001500*                                                                 IX2044.2
001600*    NEW TESTS:                                                   IX2044.2
001700*                         CLOSE  ... <WITH LOCK>                  IX2044.2
001800*                                                                 IX2044.2
001900*                                                                 IX2044.2
002000*    THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND SEMANTIC   IX2044.2
002100*    ACTIONS ASSOCIATED WITH THE FOLLOWING ELEMENTS:              IX2044.2
002200*                                                                 IX2044.2
002300*    (1) FILE STATUS   (ONLY 00 & 10)                             IX2044.2
002400*    (2) USE AFTER ERROR PROCEDURE ON FILE-NAME                   IX2044.2
002500*    (3) READ                                                     IX2044.2
002600*    (4) WRITE                                                    IX2044.2
002700*    (5) REWRITE                                                  IX2044.2
002800*    (6) RECORD KEY                                               IX2044.2
002900*    (7) ACCESS                                                   IX2044.2
003000*                                                                 IX2044.2
003100*    THIS PROGRAM CREATES AN INDEXED FILE SEQUENTIALLY (ACCESS    IX2044.2
003200*    MODE DYMANIC) AND THEN UPDATES SELECTIVE RECORDS OF THE FILE.IX2044.2
003300*    THE FILE STATUS CONTENTS ARE CAPTURED AND TESTED FOR ACCURACYIX2044.2
003400*    FOR EACH OPEN, CLOSE, READ AND REWRITE STATEMENT USED.  THE  IX2044.2
003500*    READ, WRITE AND REWRITE STATEMENTS ARE USED WITHOUT THE      IX2044.2
003600*    APPROPRIATE AT END OR INVALID KEY PHRASES.  THE OMISSION OF  IX2044.2
003700*    THESE PHRASES ARE PERMITTED IF AN APPLICABLE USE PROCEDURE   IX2044.2
003800*    HAS BEEN SPECIFIED.                                          IX2044.2
003900*                                                                 IX2044.2
004000*                                                                 IX2044.2
004100*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2044.2
004200*                                                                 IX2044.2
004300*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2044.2
004400*                    CLAUSE FOR DATA FILE IX-FD2                  IX2044.2
004500*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2044.2
004600*                    CLAUSE FOR INDEX FILE IX-FD2                 IX2044.2
004700*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2044.2
004800*             X-62   IMPLEMENTOR-NAME FOR RAW-DATA                IX2044.2
004900*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2044.2
005000*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2044.2
005100*             X-84   LABEL RECORDS    FOR PRINT-FILEPUTER         IX2044.2
005200*                                                                 IX2044.2
005300*         NOTE:  X-CARDS 45, 62 AND 84      ARE OPTIONAL          IX2044.2
005400*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2044.2
005500*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2044.2
005600*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2044.2
005700*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2044.2
005800*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2044.2
005900*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2044.2
006000*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2044.2
006100*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2044.2
006200*               THEY ARE AS FOLLOWS                               IX2044.2
006300*                                                                 IX2044.2
006400*                  P  SELECTS X-CARDS 62                          IX2044.2
006500*                  J  SELECTS X-CARD 45                           IX2044.2
006600*                  C  SELECTS X-CARD 84                           IX2044.2
006700*                                                                 IX2044.2
006800******************************************************            IX2044.2
006900 ENVIRONMENT DIVISION.                                            IX2044.2
007000 CONFIGURATION SECTION.                                           IX2044.2
007100 SOURCE-COMPUTER.                                                 IX2044.2
007200     XXXXX082.                                                    IX2044.2
007300 OBJECT-COMPUTER.                                                 IX2044.2
007400     XXXXX083.                                                    IX2044.2
007500 INPUT-OUTPUT SECTION.                                            IX2044.2
007600 FILE-CONTROL.                                                    IX2044.2
007700*    SELECT RAW-DATA   ASSIGN TO                                  IX2044.2
007800*    XXXXX062                                                     IX2044.2
007900*           ORGANIZATION IS INDEXED                               IX2044.2
008000*           ACCESS MODE IS RANDOM                                 IX2044.2
008100*           RECORD KEY IS RAW-DATA-KEY.                           IX2044.2
008200     SELECT PRINT-FILE ASSIGN TO                                  IX2044.2
008300     XXXXX055.                                                    IX2044.2
008400     SELECT IX-FD2 ASSIGN                                         IX2044.2
008500     XXXXX025                                                     IX2044.2
008600*    XXXXX045                                                     IX2044.2
008700             ORGANIZATION IS INDEXED                              IX2044.2
008800        ACCESS  DYNAMIC                                           IX2044.2
008900        FILE STATUS IS IX-FD2-STATUS                              IX2044.2
009000        RECORD  IX-FD2-KEY.                                       IX2044.2
009100 DATA DIVISION.                                                   IX2044.2
009200 FILE SECTION.                                                    IX2044.2
009300*                                                                 IX2044.2
009400*FD  RAW-DATA.                                                    IX2044.2
009500*                                                                 IX2044.2
009600*01  RAW-DATA-SATZ.                                               IX2044.2
009700*    05  RAW-DATA-KEY        PIC X(6).                            IX2044.2
009800*    05  C-DATE              PIC 9(6).                            IX2044.2
009900*    05  C-TIME              PIC 9(8).                            IX2044.2
010000*    05  C-NO-OF-TESTS       PIC 99.                              IX2044.2
010100*    05  C-OK                PIC 999.                             IX2044.2
010200*    05  C-ALL               PIC 999.                             IX2044.2
010300*    05  C-FAIL              PIC 999.                             IX2044.2
010400*    05  C-DELETED           PIC 999.                             IX2044.2
010500*    05  C-INSPECT           PIC 999.                             IX2044.2
010600*    05  C-NOTE              PIC X(13).                           IX2044.2
010700*    05  C-INDENT            PIC X.                               IX2044.2
010800*    05  C-ABORT             PIC X(8).                            IX2044.2
010900 FD  PRINT-FILE.                                                  IX2044.2
011000 01  PRINT-REC PICTURE X(120).                                    IX2044.2
011100 01  DUMMY-RECORD PICTURE X(120).                                 IX2044.2
011200 FD  IX-FD2                                                       IX2044.2
011300C    LABEL RECORDS ARE STANDARD                                   IX2044.2
011400C    DATA RECORDS IX-FD2R1-F-G-240                                IX2044.2
011500     BLOCK CONTAINS 480                                           IX2044.2
011600     RECORD CONTAINS  240  CHARACTERS.                            IX2044.2
011700 01  IX-FD2R1-F-G-240.                                            IX2044.2
011800     05 IX-FD2-REC-120    PIC X(120).                             IX2044.2
011900     05 IX-FD2-REC-120-240.                                       IX2044.2
012000     10  FILLER PICTURE X(8).                                     IX2044.2
012100     10  IX-FD2-KEY PIC X(29).                                    IX2044.2
012200     10  FILLER PIC X(83).                                        IX2044.2
012300 WORKING-STORAGE SECTION.                                         IX2044.2
012400 01  GRP-0101.                                                    IX2044.2
012500     02 FILLER PIC X(10) VALUE "ABCD921XYZ".                      IX2044.2
012600     02  WRK-DU-09V00-001 PIC 9(9) VALUE ZERO.                    IX2044.2
012700     02 FILLER PIC X(10) VALUE "Z2F()$+-AB".                      IX2044.2
012800 01  GRP-0001.                                                    IX2044.2
012900     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013000     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013100     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013200     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013300     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013400     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013500     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2044.2
013600     05 IX-FD2-STATUS     PIC  XX  VALUE  SPACE.                  IX2044.2
013700     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 IX2044.2
013800     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 IX2044.2
013900     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 IX2044.2
014000     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 IX2044.2
014100     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 IX2044.2
014200     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 IX2044.2
014300     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 IX2044.2
014400     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 IX2044.2
014500     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 IX2044.2
014600 01  DUMMY-WRK-REC.                                               IX2044.2
014700     02 DUMMY-WRK1       PIC X(120).                              IX2044.2
014800     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX2044.2
014900        03 FILLER   PIC X(5).                                     IX2044.2
015000        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX2044.2
015100 01  FILE-RECORD-INFORMATION-REC.                                 IX2044.2
015200     03 FILE-RECORD-INFO-SKELETON.                                IX2044.2
015300        05 FILLER                 PICTURE X(48)       VALUE       IX2044.2
015400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2044.2
015500        05 FILLER                 PICTURE X(46)       VALUE       IX2044.2
015600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2044.2
015700        05 FILLER                 PICTURE X(26)       VALUE       IX2044.2
015800             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2044.2
015900        05 FILLER                 PICTURE X(37)       VALUE       IX2044.2
016000             ",RECKEY=                             ".             IX2044.2
016100        05 FILLER                 PICTURE X(38)       VALUE       IX2044.2
016200             ",ALTKEY1=                             ".            IX2044.2
016300        05 FILLER                 PICTURE X(38)       VALUE       IX2044.2
016400             ",ALTKEY2=                             ".            IX2044.2
016500        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2044.2
016600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2044.2
016700        05 FILE-RECORD-INFO-P1-120.                               IX2044.2
016800           07 FILLER              PIC X(5).                       IX2044.2
016900           07 XFILE-NAME           PIC X(6).                      IX2044.2
017000           07 FILLER              PIC X(8).                       IX2044.2
017100           07 XRECORD-NAME         PIC X(6).                      IX2044.2
017200           07 FILLER              PIC X(1).                       IX2044.2
017300           07 REELUNIT-NUMBER     PIC 9(1).                       IX2044.2
017400           07 FILLER              PIC X(7).                       IX2044.2
017500           07 XRECORD-NUMBER       PIC 9(6).                      IX2044.2
017600           07 FILLER              PIC X(6).                       IX2044.2
017700           07 UPDATE-NUMBER       PIC 9(2).                       IX2044.2
017800           07 FILLER              PIC X(5).                       IX2044.2
017900           07 ODO-NUMBER          PIC 9(4).                       IX2044.2
018000           07 FILLER              PIC X(5).                       IX2044.2
018100           07 XPROGRAM-NAME        PIC X(5).                      IX2044.2
018200           07 FILLER              PIC X(7).                       IX2044.2
018300           07 XRECORD-LENGTH       PIC 9(6).                      IX2044.2
018400           07 FILLER              PIC X(7).                       IX2044.2
018500           07 CHARS-OR-RECORDS    PIC X(2).                       IX2044.2
018600           07 FILLER              PIC X(1).                       IX2044.2
018700           07 XBLOCK-SIZE          PIC 9(4).                      IX2044.2
018800           07 FILLER              PIC X(6).                       IX2044.2
018900           07 RECORDS-IN-FILE     PIC 9(6).                       IX2044.2
019000           07 FILLER              PIC X(5).                       IX2044.2
019100           07 XFILE-ORGANIZATION   PIC X(2).                      IX2044.2
019200           07 FILLER              PIC X(6).                       IX2044.2
019300           07 XLABEL-TYPE          PIC X(1).                      IX2044.2
019400        05 FILE-RECORD-INFO-P121-240.                             IX2044.2
019500           07 FILLER              PIC X(8).                       IX2044.2
019600           07 XRECORD-KEY          PIC X(29).                     IX2044.2
019700           07 FILLER              PIC X(9).                       IX2044.2
019800           07 ALTERNATE-KEY1      PIC X(29).                      IX2044.2
019900           07 FILLER              PIC X(9).                       IX2044.2
020000           07 ALTERNATE-KEY2      PIC X(29).                      IX2044.2
020100           07 FILLER              PIC X(7).                       IX2044.2
020200 01  TEST-RESULTS.                                                IX2044.2
020300     02 FILLER                   PIC X      VALUE SPACE.          IX2044.2
020400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2044.2
020500     02 FILLER                   PIC X      VALUE SPACE.          IX2044.2
020600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2044.2
020700     02 FILLER                   PIC X      VALUE SPACE.          IX2044.2
020800     02  PAR-NAME.                                                IX2044.2
020900       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2044.2
021000       03  PARDOT-X              PIC X      VALUE SPACE.          IX2044.2
021100       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2044.2
021200     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2044.2
021300     02 RE-MARK                  PIC X(61).                       IX2044.2
021400 01  TEST-COMPUTED.                                               IX2044.2
021500     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2044.2
021600     02 FILLER                   PIC X(17)  VALUE                 IX2044.2
021700            "       COMPUTED=".                                   IX2044.2
021800     02 COMPUTED-X.                                               IX2044.2
021900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2044.2
022000     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2044.2
022100                                 PIC -9(9).9(9).                  IX2044.2
022200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2044.2
022300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2044.2
022400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2044.2
022500     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2044.2
022600         04 COMPUTED-18V0                    PIC -9(18).          IX2044.2
022700         04 FILLER                           PIC X.               IX2044.2
022800     03 FILLER PIC X(50) VALUE SPACE.                             IX2044.2
022900 01  TEST-CORRECT.                                                IX2044.2
023000     02 FILLER PIC X(30) VALUE SPACE.                             IX2044.2
023100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2044.2
023200     02 CORRECT-X.                                                IX2044.2
023300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2044.2
023400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2044.2
023500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2044.2
023600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2044.2
023700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2044.2
023800     03      CR-18V0 REDEFINES CORRECT-A.                         IX2044.2
023900         04 CORRECT-18V0                     PIC -9(18).          IX2044.2
024000         04 FILLER                           PIC X.               IX2044.2
024100     03 FILLER PIC X(2) VALUE SPACE.                              IX2044.2
024200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2044.2
024300 01  CCVS-C-1.                                                    IX2044.2
024400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2044.2
024500-    "SS  PARAGRAPH-NAME                                          IX2044.2
024600-    "       REMARKS".                                            IX2044.2
024700     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2044.2
024800 01  CCVS-C-2.                                                    IX2044.2
024900     02 FILLER                     PIC X        VALUE SPACE.      IX2044.2
025000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2044.2
025100     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2044.2
025200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2044.2
025300     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2044.2
025400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2044.2
025500 01  REC-CT                        PIC 99       VALUE ZERO.       IX2044.2
025600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2044.2
025700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2044.2
025800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2044.2
025900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2044.2
026000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2044.2
026100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2044.2
026200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2044.2
026300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2044.2
026400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2044.2
026500 01  CCVS-H-1.                                                    IX2044.2
026600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2044.2
026700     02  FILLER                    PIC X(42)    VALUE             IX2044.2
026800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2044.2
026900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2044.2
027000 01  CCVS-H-2A.                                                   IX2044.2
027100   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2044.2
027200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2044.2
027300   02  FILLER                        PIC XXXX   VALUE             IX2044.2
027400     "4.2 ".                                                      IX2044.2
027500   02  FILLER                        PIC X(28)  VALUE             IX2044.2
027600            " COPY - NOT FOR DISTRIBUTION".                       IX2044.2
027700   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2044.2
027800                                                                  IX2044.2
027900 01  CCVS-H-2B.                                                   IX2044.2
028000   02  FILLER                        PIC X(15)  VALUE             IX2044.2
028100            "TEST RESULT OF ".                                    IX2044.2
028200   02  TEST-ID                       PIC X(9).                    IX2044.2
028300   02  FILLER                        PIC X(4)   VALUE             IX2044.2
028400            " IN ".                                               IX2044.2
028500   02  FILLER                        PIC X(12)  VALUE             IX2044.2
028600     " HIGH       ".                                              IX2044.2
028700   02  FILLER                        PIC X(22)  VALUE             IX2044.2
028800            " LEVEL VALIDATION FOR ".                             IX2044.2
028900   02  FILLER                        PIC X(58)  VALUE             IX2044.2
029000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2044.2
029100 01  CCVS-H-3.                                                    IX2044.2
029200     02  FILLER                      PIC X(34)  VALUE             IX2044.2
029300            " FOR OFFICIAL USE ONLY    ".                         IX2044.2
029400     02  FILLER                      PIC X(58)  VALUE             IX2044.2
029500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2044.2
029600     02  FILLER                      PIC X(28)  VALUE             IX2044.2
029700            "  COPYRIGHT   1985 ".                                IX2044.2
029800 01  CCVS-E-1.                                                    IX2044.2
029900     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2044.2
030000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2044.2
030100     02 ID-AGAIN                     PIC X(9).                    IX2044.2
030200     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2044.2
030300 01  CCVS-E-2.                                                    IX2044.2
030400     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2044.2
030500     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2044.2
030600     02 CCVS-E-2-2.                                               IX2044.2
030700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2044.2
030800         03 FILLER                   PIC X      VALUE SPACE.      IX2044.2
030900         03 ENDER-DESC               PIC X(44)  VALUE             IX2044.2
031000            "ERRORS ENCOUNTERED".                                 IX2044.2
031100 01  CCVS-E-3.                                                    IX2044.2
031200     02  FILLER                      PIC X(22)  VALUE             IX2044.2
031300            " FOR OFFICIAL USE ONLY".                             IX2044.2
031400     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2044.2
031500     02  FILLER                      PIC X(58)  VALUE             IX2044.2
031600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2044.2
031700     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2044.2
031800     02 FILLER                       PIC X(15)  VALUE             IX2044.2
031900             " COPYRIGHT 1985".                                   IX2044.2
032000 01  CCVS-E-4.                                                    IX2044.2
032100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2044.2
032200     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2044.2
032300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2044.2
032400     02 FILLER                       PIC X(40)  VALUE             IX2044.2
032500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2044.2
032600 01  XXINFO.                                                      IX2044.2
032700     02 FILLER                       PIC X(19)  VALUE             IX2044.2
032800            "*** INFORMATION ***".                                IX2044.2
032900     02 INFO-TEXT.                                                IX2044.2
033000       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2044.2
033100       04 XXCOMPUTED                 PIC X(20).                   IX2044.2
033200       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2044.2
033300       04 XXCORRECT                  PIC X(20).                   IX2044.2
033400     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2044.2
033500 01  HYPHEN-LINE.                                                 IX2044.2
033600     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2044.2
033700     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2044.2
033800-    "*****************************************".                 IX2044.2
033900     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2044.2
034000-    "******************************".                            IX2044.2
034100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2044.2
034200     "IX204A".                                                    IX2044.2
034300 PROCEDURE DIVISION.                                              IX2044.2
034400 DECLARATIVES.                                                    IX2044.2
034500 IX-FD2-01 SECTION.                                               IX2044.2
034600     USE    AFTER STANDARD ERROR PROCEDURE ON IX-FD2.             IX2044.2
034700 IX-FD2-01-01.                                                    IX2044.2
034800     ADD      1 TO  WRK-CS-09V00-013.                             IX2044.2
034900     GO TO    IX-FD2-01-03                                        IX2044.2
035000              IX-FD2-01-05                                        IX2044.2
035100              DEPENDING ON  WRK-CS-09V00-012.                     IX2044.2
035200     GO TO    IX-FD2-01-EXIT.                                     IX2044.2
035300 IX-FD2-01-03.                                                    IX2044.2
035400*ENTRY FROM SEGMENT INX-TEST-001.                                 IX2044.2
035500*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     IX2044.2
035600     ADD     1 TO  WRK-CS-09V00-014.                              IX2044.2
035700 IX-FD2-01-05.                                                    IX2044.2
035800     ADD      1 TO WRK-CS-09V00-017.                              IX2044.2
035900     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX2044.2
036000              MOVE   IX-FD2-STATUS TO WRK-XN-0002-002             IX2044.2
036100              MOVE  "10" TO WRK-XN-0002-003.                      IX2044.2
036200 IX-FD2-01-EXIT.                                                  IX2044.2
036300     EXIT.                                                        IX2044.2
036400 END DECLARATIVES.                                                IX2044.2
036500 CCVS1 SECTION.                                                   IX2044.2
036600 OPEN-FILES.                                                      IX2044.2
036700*    OPEN I-O RAW-DATA.                                           IX2044.2
036800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2044.2
036900*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2044.2
037000*    MOVE "ABORTED " TO C-ABORT.                                  IX2044.2
037100*    ADD 1 TO C-NO-OF-TESTS.                                      IX2044.2
037200*    ACCEPT C-DATE  FROM DATE.                                    IX2044.2
037300*    ACCEPT C-TIME  FROM TIME.                                    IX2044.2
037400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2044.2
037500*END-E-1.                                                         IX2044.2
037600*    CLOSE RAW-DATA.                                              IX2044.2
037700     OPEN    OUTPUT PRINT-FILE.                                   IX2044.2
037800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2044.2
037900     MOVE    SPACE TO TEST-RESULTS.                               IX2044.2
038000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2044.2
038100     MOVE    ZERO TO REC-SKL-SUB.                                 IX2044.2
038200     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2044.2
038300 CCVS-INIT-FILE.                                                  IX2044.2
038400     ADD     1 TO REC-SKL-SUB.                                    IX2044.2
038500     MOVE    FILE-RECORD-INFO-SKELETON                            IX2044.2
038600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2044.2
038700 CCVS-INIT-EXIT.                                                  IX2044.2
038800     GO TO CCVS1-EXIT.                                            IX2044.2
038900 CLOSE-FILES.                                                     IX2044.2
039000*    OPEN I-O RAW-DATA.                                           IX2044.2
039100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2044.2
039200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2044.2
039300*    MOVE "OK.     " TO C-ABORT.                                  IX2044.2
039400*    MOVE PASS-COUNTER TO C-OK.                                   IX2044.2
039500*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2044.2
039600*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2044.2
039700*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2044.2
039800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2044.2
039900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2044.2
040000*END-E-2.                                                         IX2044.2
040100*    CLOSE RAW-DATA.                                              IX2044.2
040200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2044.2
040300 TERMINATE-CCVS.                                                  IX2044.2
040400*    EXIT PROGRAM.                                                IX2044.2
040500*TERMINATE-CALL.                                                  IX2044.2
040600     STOP     RUN.                                                IX2044.2
040700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2044.2
040800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2044.2
040900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2044.2
041000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2044.2
041100     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2044.2
041200 PRINT-DETAIL.                                                    IX2044.2
041300     IF REC-CT NOT EQUAL TO ZERO                                  IX2044.2
041400             MOVE "." TO PARDOT-X                                 IX2044.2
041500             MOVE REC-CT TO DOTVALUE.                             IX2044.2
041600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2044.2
041700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2044.2
041800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2044.2
041900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2044.2
042000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2044.2
042100     MOVE SPACE TO CORRECT-X.                                     IX2044.2
042200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2044.2
042300     MOVE     SPACE TO RE-MARK.                                   IX2044.2
042400 HEAD-ROUTINE.                                                    IX2044.2
042500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2044.2
042600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2044.2
042700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2044.2
042800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2044.2
042900 COLUMN-NAMES-ROUTINE.                                            IX2044.2
043000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2044.2
043100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2044.2
043200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2044.2
043300 END-ROUTINE.                                                     IX2044.2
043400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2044.2
043500 END-RTN-EXIT.                                                    IX2044.2
043600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2044.2
043700 END-ROUTINE-1.                                                   IX2044.2
043800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2044.2
043900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2044.2
044000      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2044.2
044100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2044.2
044200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2044.2
044300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2044.2
044400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2044.2
044500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2044.2
044600  END-ROUTINE-12.                                                 IX2044.2
044700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2044.2
044800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2044.2
044900         MOVE "NO " TO ERROR-TOTAL                                IX2044.2
045000         ELSE                                                     IX2044.2
045100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2044.2
045200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2044.2
045300     PERFORM WRITE-LINE.                                          IX2044.2
045400 END-ROUTINE-13.                                                  IX2044.2
045500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2044.2
045600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2044.2
045700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2044.2
045800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2044.2
045900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2044.2
046000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2044.2
046100          MOVE "NO " TO ERROR-TOTAL                               IX2044.2
046200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2044.2
046300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2044.2
046400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2044.2
046500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2044.2
046600 WRITE-LINE.                                                      IX2044.2
046700     ADD 1 TO RECORD-COUNT.                                       IX2044.2
046800*    IF RECORD-COUNT GREATER 42                                   IX2044.2
046900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2044.2
047000*        MOVE SPACE TO DUMMY-RECORD                               IX2044.2
047100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2044.2
047200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2044.2
047300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2044.2
047400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2044.2
047500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2044.2
047600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2044.2
047700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2044.2
047800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2044.2
047900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2044.2
048000*        MOVE ZERO TO RECORD-COUNT.                               IX2044.2
048100     PERFORM WRT-LN.                                              IX2044.2
048200 WRT-LN.                                                          IX2044.2
048300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2044.2
048400     MOVE SPACE TO DUMMY-RECORD.                                  IX2044.2
048500 BLANK-LINE-PRINT.                                                IX2044.2
048600     PERFORM WRT-LN.                                              IX2044.2
048700 FAIL-ROUTINE.                                                    IX2044.2
048800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2044.2
048900            GO TO   FAIL-ROUTINE-WRITE.                           IX2044.2
049000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2044.2
049100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2044.2
049200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2044.2
049300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2044.2
049400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2044.2
049500     GO TO  FAIL-ROUTINE-EX.                                      IX2044.2
049600 FAIL-ROUTINE-WRITE.                                              IX2044.2
049700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2044.2
049800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2044.2
049900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2044.2
050000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2044.2
050100 FAIL-ROUTINE-EX. EXIT.                                           IX2044.2
050200 BAIL-OUT.                                                        IX2044.2
050300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2044.2
050400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2044.2
050500 BAIL-OUT-WRITE.                                                  IX2044.2
050600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2044.2
050700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2044.2
050800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2044.2
050900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2044.2
051000 BAIL-OUT-EX. EXIT.                                               IX2044.2
051100 CCVS1-EXIT.                                                      IX2044.2
051200     EXIT.                                                        IX2044.2
051300 SECT-IX-04-001 SECTION.                                          IX2044.2
051400 WRITE-INIT-GF-01.                                                IX2044.2
051500     MOVE     "CREATE IX-FD2" TO  FEATURE                         IX2044.2
051600     MOVE    "WRITE-TEST-001" TO PAR-NAME.                        IX2044.2
051700     MOVE     "IX-FD2"        TO  XFILE-NAME (2).                 IX2044.2
051800     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               IX2044.2
051900     MOVE     CCVS-PGM-ID     TO  XPROGRAM-NAME (2).              IX2044.2
052000     MOVE     000240          TO  XRECORD-LENGTH (2).             IX2044.2
052100     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           IX2044.2
052200     MOVE     0001            TO  XBLOCK-SIZE       (2).          IX2044.2
052300     MOVE     000500          TO  RECORDS-IN-FILE  (2).           IX2044.2
052400     MOVE    "IX"                TO XFILE-ORGANIZATION (2).       IX2044.2
052500     MOVE     "S"             TO  XLABEL-TYPE        (2).         IX2044.2
052600     MOVE     000001          TO  XRECORD-NUMBER     (2).         IX2044.2
052700*INITIALIZE  RECORD WORK AREA NUMBER 2.                           IX2044.2
052800     MOVE     1         TO  WRK-CS-09V00-012.                     IX2044.2
052900     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     IX2044.2
053000                            WRK-CS-09V00-015 WRK-CS-09V00-016     IX2044.2
053100                            WRK-CS-09V00-017 WRK-CS-09V00-018.    IX2044.2
053200     MOVE     SPACE     TO  IX-FD2-STATUS.                        IX2044.2
053300     MOVE    ZERO TO WRK-DU-09V00-001.                            IX2044.2
053400     OPEN    OUTPUT  IX-FD2.                                      IX2044.2
053500     MOVE    GRP-0101  TO IX-FD2-KEY.                             IX2044.2
053600     MOVE     IX-FD2-STATUS TO WRK-XN-0002-001.                   IX2044.2
053700*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            IX2044.2
053800 WRITE-TEST-GF-01-R.                                              IX2044.2
053900     MOVE    "99"  TO IX-FD2-STATUS.                              IX2044.2
054000     MOVE     XRECORD-NUMBER (2) TO WRK-DU-09V00-001.             IX2044.2
054100     MOVE     GRP-0101 TO XRECORD-KEY (2).                        IX2044.2
054200     MOVE     FILE-RECORD-INFO (2) TO IX-FD2R1-F-G-240.           IX2044.2
054300     WRITE   IX-FD2R1-F-G-240.                                    IX2044.2
054400     IF        IX-FD2-STATUS NOT EQUAL TO "00"                    IX2044.2
054500         ADD  1  TO WRK-CS-09V00-016.                             IX2044.2
054600     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX2044.2
054700              GO TO  WRITE-TEST-GF-01.                            IX2044.2
054800     ADD      01 TO  XRECORD-NUMBER (2).                          IX2044.2
054900     GO TO    WRITE-TEST-GF-01-R.                                 IX2044.2
055000 WRITE-TEST-GF-01.                                                IX2044.2
055100     MOVE     "WRITE  IX-FD2." TO  FEATURE.                       IX2044.2
055200     MOVE    "WRITE-TEST-GF-01" TO PAR-NAME.                      IX2044.2
055300     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  IX2044.2
055400              MOVE "EXCEPTIONS/ERRORS; IX-41 4.9.2" TO RE-MARK    IX2044.2
055500              MOVE  ZERO TO CORRECT-18V0                          IX2044.2
055600              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX2044.2
055700              PERFORM   FAIL                                      IX2044.2
055800              ELSE                                                IX2044.2
055900              PERFORM   PASS.                                     IX2044.2
056000     PERFORM  PRINT-DETAIL.                                       IX2044.2
056100 WRITE-TEST-GF-02.                                                IX2044.2
056200     MOVE    "WRITE  IX-FD2." TO  FEATURE.                        IX2044.2
056300     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX2044.2
056400     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 IX2044.2
056500              MOVE "INCORRECT COUNT; IX-41 4.9.2" TO RE-MARK      IX2044.2
056600              MOVE  500 TO CORRECT-18V0                           IX2044.2
056700             MOVE   XRECORD-NUMBER (2)  TO COMPUTED-18V0          IX2044.2
056800              PERFORM  FAIL                                       IX2044.2
056900              ELSE                                                IX2044.2
057000              PERFORM PASS.                                       IX2044.2
057100     PERFORM  PRINT-DETAIL.                                       IX2044.2
057200 WRITE-TEST-GF-03.                                                IX2044.2
057300     MOVE    "OPEN OUTPUT 00" TO  FEATURE.                        IX2044.2
057400     MOVE    "WRITE-TEST-GF-03" TO PAR-NAME.                      IX2044.2
057500     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  IX2044.2
057600              MOVE "STATUS/OPEN; IX-3 1.3.4 (1) A" TO RE-MARK     IX2044.2
057700              MOVE WRK-XN-0002-001 TO COMPUTED-A                  IX2044.2
057800              MOVE "00" TO CORRECT-A                              IX2044.2
057900              PERFORM  FAIL                                       IX2044.2
058000              ELSE                                                IX2044.2
058100              PERFORM  PASS.                                      IX2044.2
058200     PERFORM  PRINT-DETAIL.                                       IX2044.2
058300 WRITE-TEST-GF-04.                                                IX2044.2
058400     MOVE    "WRITE STATUS 00" TO  FEATURE.                       IX2044.2
058500     MOVE    "WRITE-TEST-GF-04" TO PAR-NAME.                      IX2044.2
058600     IF        IX-FD2-STATUS NOT EQUAL TO "00"                    IX2044.2
058700              MOVE "STATUS/WRITE; IX-3 1.3.4 (1) A" TO RE-MARK    IX2044.2
058800              MOVE IX-FD2-STATUS TO COMPUTED-A                    IX2044.2
058900              MOVE "00" TO CORRECT-A                              IX2044.2
059000              PERFORM   FAIL                                      IX2044.2
059100              ELSE                                                IX2044.2
059200              PERFORM   PASS.                                     IX2044.2
059300     PERFORM  PRINT-DETAIL.                                       IX2044.2
059400 WRITE-TEST-GF-05.                                                IX2044.2
059500     MOVE    "WRITE STATUS 00" TO  FEATURE.                       IX2044.2
059600     MOVE    "WRITE-TEST-GF-05" TO PAR-NAME.                      IX2044.2
059700     IF      WRK-CS-09V00-016    NOT EQUAL TO ZERO                IX2044.2
059800              MOVE  "STATUS/WRITE; IX-3 1.3.4 (1) A"  TO RE-MARK  IX2044.2
059900              MOVE    ZERO TO CORRECT-18V0                        IX2044.2
060000              MOVE    WRK-CS-09V00-016  TO COMPUTED-18V0          IX2044.2
060100              PERFORM   FAIL                                      IX2044.2
060200              ELSE                                                IX2044.2
060300              PERFORM  PASS.                                      IX2044.2
060400     PERFORM  PRINT-DETAIL.                                       IX2044.2
060500 WRITE-TEST-GF-06.                                                IX2044.2
060600     MOVE    "CLOSE: STATUS: 00" TO  FEATURE.                     IX2044.2
060700     MOVE    "WRITE-TEST-GF-06" TO PAR-NAME.                      IX2044.2
060800     MOVE     SPACE  TO  IX-FD2-STATUS.                           IX2044.2
060900     CLOSE    IX-FD2.                                             IX2044.2
061000     IF        IX-FD2-STATUS NOT EQUAL TO "00"                    IX2044.2
061100              MOVE "CLOSE/STATUS; IX-3 1.3.4 (1) A" TO RE-MARK    IX2044.2
061200              MOVE IX-FD2-STATUS TO COMPUTED-18V0                 IX2044.2
061300              MOVE "00" TO CORRECT-A                              IX2044.2
061400              PERFORM FAIL                                        IX2044.2
061500              ELSE                                                IX2044.2
061600              PERFORM PASS.                                       IX2044.2
061700     PERFORM  PRINT-DETAIL.                                       IX2044.2
061800 RWRT-INIT-GF-01.                                                 IX2044.2
061900     MOVE     2     TO  WRK-CS-09V00-012.                         IX2044.2
062000     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX2044.2
062100     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX2044.2
062200     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX2044.2
062300     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX2044.2
062400     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX2044.2
062500     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX2044.2
062600     MOVE    SPACE TO IX-FD2-STATUS.                              IX2044.2
062700     OPEN     I-O  IX-FD2.                                        IX2044.2
062800     MOVE     SPACE     TO  WRK-XN-0002-002                       IX2044.2
062900     MOVE     SPACE     TO  WRK-XN-0002-003                       IX2044.2
063000     MOVE     SPACE     TO  WRK-XN-0002-004                       IX2044.2
063100     MOVE      IX-FD2-STATUS TO WRK-XN-0002-001                   IX2044.2
063200     MOVE     SPACE TO IX-FD2-STATUS.                             IX2044.2
063300*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       IX2044.2
063400 RWRT-TEST-GF-01-R.                                               IX2044.2
063500     ADD      1 TO WRK-CS-09V00-014.                              IX2044.2
063600     ADD      1 TO WRK-CS-09V00-015.                              IX2044.2
063700     READ  IX-FD2 NEXT RECORD.                                    IX2044.2
063800     IF        IX-FD2-STATUS EQUAL TO "10"                        IX2044.2
063900              GO TO  RWRT-TEST-GF-01.                             IX2044.2
064000     MOVE     IX-FD2R1-F-G-240 TO FILE-RECORD-INFO (2).           IX2044.2
064100     IF       WRK-CS-09V00-015 EQUAL TO 5                         IX2044.2
064200              ADD 01 TO UPDATE-NUMBER (2)                         IX2044.2
064300              MOVE FILE-RECORD-INFO (2) TO IX-FD2R1-F-G-240       IX2044.2
064400             PERFORM  RWRT-010-UPDATE                             IX2044.2
064500             MOVE ZERO TO WRK-CS-09V00-015                        IX2044.2
064600              GO TO  RWRT-TEST-GF-01-2.                           IX2044.2
064700     IF       WRK-CS-09V00-014 GREATER 500                        IX2044.2
064800              GO TO  RWRT-TEST-GF-01.                             IX2044.2
064900     GO TO    RWRT-TEST-GF-01-R.                                  IX2044.2
065000 RWRT-010-UPDATE.                                                 IX2044.2
065100     REWRITE   IX-FD2R1-F-G-240.                                  IX2044.2
065200 RWRT-TEST-GF-01-2.                                               IX2044.2
065300     IF        IX-FD2-STATUS NOT EQUAL TO "00"                    IX2044.2
065400              ADD 1 TO WRK-CS-09V00-016.                          IX2044.2
065500     MOVE    SPACE  TO   IX-FD2-STATUS.                           IX2044.2
065600     GO TO    RWRT-TEST-GF-01-R.                                  IX2044.2
065700 RWRT-TEST-GF-01.                                                 IX2044.2
065800     MOVE    "RWRT-TEST-GF-01" TO PAR-NAME.                       IX2044.2
065900     MOVE    "REWRITE IX-FD2"  TO FEATURE.                        IX2044.2
066000     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     IX2044.2
066100              MOVE  "EXCEPTIONS/ERRORS; IX-33 4.6.2" TO RE-MARK   IX2044.2
066200              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              IX2044.2
066300              MOVE  1 TO CORRECT-18V0                             IX2044.2
066400              PERFORM FAIL                                        IX2044.2
066500              ELSE                                                IX2044.2
066600              PERFORM PASS.                                       IX2044.2
066700     PERFORM  PRINT-DETAIL.                                       IX2044.2
066800 RWRT-TEST-GF-02.                                                 IX2044.2
066900     MOVE    "RWRT-TEST-GF-02" TO PAR-NAME.                       IX2044.2
067000     MOVE    "UPDATE  IX-FD2"  TO FEATURE.                        IX2044.2
067100     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   IX2044.2
067200              MOVE "INCORRECT COUNT; IX-33 4.6.2" TO RE-MARK      IX2044.2
067300              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX2044.2
067400              MOVE 501 TO CORRECT-18V0                            IX2044.2
067500              PERFORM FAIL                                        IX2044.2
067600              ELSE                                                IX2044.2
067700              PERFORM PASS.                                       IX2044.2
067800     PERFORM  PRINT-DETAIL.                                       IX2044.2
067900 RWRT-TEST-GF-03.                                                 IX2044.2
068000     MOVE    "RWRT-TEST-GF-03" TO PAR-NAME.                       IX2044.2
068100     MOVE    "OPEN I-O STATUS: 00"  TO FEATURE.                   IX2044.2
068200     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   IX2044.2
068300              MOVE "OPEN/STATUS; IX-3 1.3.4 (1) A"  TO  RE-MARK   IX2044.2
068400              MOVE WRK-XN-0002-001 TO COMPUTED-A                  IX2044.2
068500              MOVE  "00" TO CORRECT-A                             IX2044.2
068600              PERFORM FAIL                                        IX2044.2
068700              ELSE                                                IX2044.2
068800              PERFORM PASS.                                       IX2044.2
068900     PERFORM  PRINT-DETAIL.                                       IX2044.2
069000 RWRT-TEST-GF-04.                                                 IX2044.2
069100     MOVE    "RWRT-TEST-GF-04" TO PAR-NAME.                       IX2044.2
069200     MOVE    "READ I-O STATUS 10"  TO FEATURE.                    IX2044.2
069300     IF        IX-FD2-STATUS NOT EQUAL  TO "10"                   IX2044.2
069400              MOVE "AT END/STATUS; IX-4 1.3.4 (2) A" TO RE-MARK   IX2044.2
069500              MOVE  IX-FD2-STATUS TO COMPUTED-A                   IX2044.2
069600              MOVE "10" TO CORRECT-A                              IX2044.2
069700              PERFORM FAIL                                        IX2044.2
069800              ELSE                                                IX2044.2
069900              PERFORM PASS.                                       IX2044.2
070000     PERFORM  PRINT-DETAIL.                                       IX2044.2
070100 RWRT-TEST-GF-05.                                                 IX2044.2
070200     MOVE    "RWRT-TEST-GF-05" TO PAR-NAME.                       IX2044.2
070300     MOVE    "UPDATE  IX-FD2"  TO FEATURE.                        IX2044.2
070400     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   IX2044.2
070500              MOVE  "EXCEPTIN/STATUS; IX-4 1.3.4 (2) A" TO RE-MARKIX2044.2
070600              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 IX2044.2
070700              MOVE  "10" TO CORRECT-A                             IX2044.2
070800              PERFORM FAIL                                        IX2044.2
070900              ELSE                                                IX2044.2
071000              PERFORM PASS.                                       IX2044.2
071100     PERFORM  PRINT-DETAIL.                                       IX2044.2
071200 RWRT-TEST-GF-06.                                                 IX2044.2
071300     MOVE    "RWRT-TEST-GF-06" TO PAR-NAME.                       IX2044.2
071400     MOVE    "STATUS: 10    "  TO FEATURE.                        IX2044.2
071500     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   IX2044.2
071600              MOVE "NO/EXCEPTION; IX-4 1.3.4 (2) A" TO  RE-MARK   IX2044.2
071700              MOVE WRK-XN-0002-003 TO COMPUTED-A                  IX2044.2
071800              MOVE "10" TO CORRECT-A                              IX2044.2
071900              PERFORM FAIL                                        IX2044.2
072000              ELSE                                                IX2044.2
072100              PERFORM PASS.                                       IX2044.2
072200     PERFORM  PRINT-DETAIL.                                       IX2044.2
072300 RWRT-TEST-GF-07.                                                 IX2044.2
072400     MOVE    "RWRT-TEST-GF-07" TO PAR-NAME.                       IX2044.2
072500     MOVE    "CLOSE . LOCK: 00"  TO FEATURE.                      IX2044.2
072600     MOVE     SPACE TO IX-FD2-STATUS.                             IX2044.2
072700     CLOSE    IX-FD2      WITH LOCK.                              IX2044.2
072800     IF        IX-FD2-STATUS NOT EQUAL TO "00"                    IX2044.2
072900              MOVE "CLOSE/STATUS; IX-3 1.3.4 (1) A" TO RE-MARK    IX2044.2
073000              MOVE  IX-FD2-STATUS TO  COMPUTED-A                  IX2044.2
073100              MOVE "00" TO CORRECT-A                              IX2044.2
073200              PERFORM FAIL                                        IX2044.2
073300              ELSE                                                IX2044.2
073400              PERFORM PASS.                                       IX2044.2
073500     PERFORM  PRINT-DETAIL.                                       IX2044.2
073600                                                                  IX2044.2
073700 CCVS-EXIT SECTION.                                               IX2044.2
073800 CCVS-999999.                                                     IX2044.2
073900     GO TO CLOSE-FILES.                                           IX2044.2
      *END-OF,IX204A                                                                  
