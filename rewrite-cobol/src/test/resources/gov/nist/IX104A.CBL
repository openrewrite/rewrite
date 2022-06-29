      *HEADER,COBOL,IX104A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1044.2
000200 PROGRAM-ID.                                                      IX1044.2
000300     IX104A.                                                      IX1044.2
000400****************************************************************  IX1044.2
000500*                                                              *  IX1044.2
000600*    VALIDATION FOR:-                                          *  IX1044.2
000700*                                                              *  IX1044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1044.2
000900*                                                              *  IX1044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1044.2
001100*                                                              *  IX1044.2
001200****************************************************************  IX1044.2
001300*    THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND SEMANTIC   IX1044.2
001400*    ACTIONS ASSOCIATED WITH THE FOLLOWING ELEMENTS:              IX1044.2
001500*                                                                 IX1044.2
001600*    (1) FILE STATUS                                              IX1044.2
001700*    (2) USE AFTER EXCEPTION USING FILE-NAME                      IX1044.2
001800*    (3) READ                                                     IX1044.2
001900*    (4) WRITE                                                    IX1044.2
002000*    (5) REWRITE                                                  IX1044.2
002100*    (6) RECORD KEY                                               IX1044.2
002200*    (7) ACCESS                                                   IX1044.2
002300*                                                                 IX1044.2
002400*    THIS PROGRAM CREATES AN INDEXED FILE SEQUENTIALLY (ACCESS    IX1044.2
002500*    MODE SEQUENTIAL) AND THEN UPDATES SELECTIVE RECORDS OF THE   IX1044.2
002600*    FILE.  THE FILE STATUS CONTENTS ARE CAPTURED AND TESTED FOR  IX1044.2
002700*    ACCURACY FOR EACH OPEN, CLOSE, READ AND REWRITE STATEMENT    IX1044.2
002800*    USED.  THE READ, WRITE AND REWRITE STATEMENTS ARE USED       IX1044.2
002900*    WITHOUT THE APPROPRIATE AT END OR INVALID KEY PHRASES. THE   IX1044.2
003000*    OMISSION OF THESE PHRASES ARE PERMITTED IF AN APPLICABLE USE IX1044.2
003100*    PROCEDURE HAS BEEN SPECIFIED.                                IX1044.2
003200*                                                                 IX1044.2
003300*                                                                 IX1044.2
003400*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX1044.2
003500*                                                                 IX1044.2
003600*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1044.2
003700*                    CLAUSE FOR DATA FILE IX-FD2                  IX1044.2
003800*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1044.2
003900*                    CLAUSE FOR INDEX FILE IX-FD2                 IX1044.2
004000*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX1044.2
004100*             X-62   FOR RAW-DATA                                 IX1044.2
004200*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX1044.2
004300*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX1044.2
004400*                                                                 IX1044.2
004500*         NOTE:  X-CARDS 45 AND 62       ARE OPTIONAL             IX1044.2
004600*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX1044.2
004700*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX1044.2
004800*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX1044.2
004900*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX1044.2
005000*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX1044.2
005100*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX1044.2
005200*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX1044.2
005300*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX1044.2
005400*               THEY ARE AS FOLLOWS                               IX1044.2
005500*                                                                 IX1044.2
005600*                  J  SELECTS X-CARD 45                           IX1044.2
005700*                                                                 IX1044.2
005800******************************************************            IX1044.2
005900 ENVIRONMENT DIVISION.                                            IX1044.2
006000 CONFIGURATION SECTION.                                           IX1044.2
006100 SOURCE-COMPUTER.                                                 IX1044.2
006200     XXXXX082.                                                    IX1044.2
006300 OBJECT-COMPUTER.                                                 IX1044.2
006400     XXXXX083.                                                    IX1044.2
006500 INPUT-OUTPUT SECTION.                                            IX1044.2
006600 FILE-CONTROL.                                                    IX1044.2
006700*    SELECT RAW-DATA   ASSIGN TO                                  IX1044.2
006800*    XXXXX062                                                     IX1044.2
006900*           ORGANIZATION IS INDEXED                               IX1044.2
007000*           ACCESS MODE IS RANDOM                                 IX1044.2
007100*           RECORD KEY IS RAW-DATA-KEY.                           IX1044.2
007200     SELECT PRINT-FILE ASSIGN TO                                  IX1044.2
007300     XXXXX055.                                                    IX1044.2
007400     SELECT   IX-FS2 ASSIGN                                       IX1044.2
007500     XXXXX025                                                     IX1044.2
007600*    XXXXX045                                                     IX1044.2
007700             ORGANIZATION IS INDEXED                              IX1044.2
007800        ACCESS SEQUENTIAL                                         IX1044.2
007900        FILE STATUS IS IX-FS2-STATUS                              IX1044.2
008000        RECORD  IX-FS2-KEY.                                       IX1044.2
008100 DATA DIVISION.                                                   IX1044.2
008200 FILE SECTION.                                                    IX1044.2
008300*                                                                 IX1044.2
008400*FD  RAW-DATA.                                                    IX1044.2
008500*                                                                 IX1044.2
008600*01  RAW-DATA-SATZ.                                               IX1044.2
008700*    05  RAW-DATA-KEY        PIC X(6).                            IX1044.2
008800*    05  C-DATE              PIC 9(6).                            IX1044.2
008900*    05  C-TIME              PIC 9(8).                            IX1044.2
009000*    05  C-NO-OF-TESTS       PIC 99.                              IX1044.2
009100*    05  C-OK                PIC 999.                             IX1044.2
009200*    05  C-ALL               PIC 999.                             IX1044.2
009300*    05  C-FAIL              PIC 999.                             IX1044.2
009400*    05  C-DELETED           PIC 999.                             IX1044.2
009500*    05  C-INSPECT           PIC 999.                             IX1044.2
009600*    05  C-NOTE              PIC X(13).                           IX1044.2
009700*    05  C-INDENT            PIC X.                               IX1044.2
009800*    05  C-ABORT             PIC X(8).                            IX1044.2
009900 FD  PRINT-FILE.                                                  IX1044.2
010000 01  PRINT-REC PICTURE X(120).                                    IX1044.2
010100 01  DUMMY-RECORD PICTURE X(120).                                 IX1044.2
010200 FD  IX-FS2                                                       IX1044.2
010300C    LABEL RECORDS ARE STANDARD                                   IX1044.2
010400C    DATA RECORDS IX-FS2R1-F-G-240                                IX1044.2
010500     BLOCK CONTAINS 480                                           IX1044.2
010600     RECORD CONTAINS  240  CHARACTERS.                            IX1044.2
010700 01  IX-FS2R1-F-G-240.                                            IX1044.2
010800     05 IX-FS2-REC-120    PIC X(120).                             IX1044.2
010900     05 IX-FS2-REC-120-240.                                       IX1044.2
011000     10  FILLER PICTURE X(8).                                     IX1044.2
011100     10  IX-FS2-KEY PIC X(29).                                    IX1044.2
011200     10  FILLER PIC X(83).                                        IX1044.2
011300 WORKING-STORAGE SECTION.                                         IX1044.2
011400 01  GRP-0101.                                                    IX1044.2
011500     02 FILLER PIC X(10) VALUE "ABCD921XYZ".                      IX1044.2
011600     02  WRK-DU-09V00-001 PIC 9(9) VALUE ZERO.                    IX1044.2
011700     02 FILLER PIC X(10) VALUE "Z2F()$+-AB".                      IX1044.2
011800 01  GRP-0001.                                                    IX1044.2
011900     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012000     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012100     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012200     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012300     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012400     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012500     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1044.2
012600     05 IX-FS2-STATUS     PIC  XX  VALUE  SPACE.                  IX1044.2
012700     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 IX1044.2
012800     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 IX1044.2
012900     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 IX1044.2
013000     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 IX1044.2
013100     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 IX1044.2
013200     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 IX1044.2
013300     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 IX1044.2
013400     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 IX1044.2
013500     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 IX1044.2
013600 01  DUMMY-WRK-REC.                                               IX1044.2
013700     02 DUMMY-WRK1       PIC X(120).                              IX1044.2
013800     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX1044.2
013900        03 FILLER   PIC X(5).                                     IX1044.2
014000        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX1044.2
014100 01  FILE-RECORD-INFORMATION-REC.                                 IX1044.2
014200     03 FILE-RECORD-INFO-SKELETON.                                IX1044.2
014300        05 FILLER                 PICTURE X(48)       VALUE       IX1044.2
014400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1044.2
014500        05 FILLER                 PICTURE X(46)       VALUE       IX1044.2
014600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1044.2
014700        05 FILLER                 PICTURE X(26)       VALUE       IX1044.2
014800             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1044.2
014900        05 FILLER                 PICTURE X(37)       VALUE       IX1044.2
015000             ",RECKEY=                             ".             IX1044.2
015100        05 FILLER                 PICTURE X(38)       VALUE       IX1044.2
015200             ",ALTKEY1=                             ".            IX1044.2
015300        05 FILLER                 PICTURE X(38)       VALUE       IX1044.2
015400             ",ALTKEY2=                             ".            IX1044.2
015500        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1044.2
015600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1044.2
015700        05 FILE-RECORD-INFO-P1-120.                               IX1044.2
015800           07 FILLER              PIC X(5).                       IX1044.2
015900           07 XFILE-NAME           PIC X(6).                      IX1044.2
016000           07 FILLER              PIC X(8).                       IX1044.2
016100           07 XRECORD-NAME         PIC X(6).                      IX1044.2
016200           07 FILLER              PIC X(1).                       IX1044.2
016300           07 REELUNIT-NUMBER     PIC 9(1).                       IX1044.2
016400           07 FILLER              PIC X(7).                       IX1044.2
016500           07 XRECORD-NUMBER       PIC 9(6).                      IX1044.2
016600           07 FILLER              PIC X(6).                       IX1044.2
016700           07 UPDATE-NUMBER       PIC 9(2).                       IX1044.2
016800           07 FILLER              PIC X(5).                       IX1044.2
016900           07 ODO-NUMBER          PIC 9(4).                       IX1044.2
017000           07 FILLER              PIC X(5).                       IX1044.2
017100           07 XPROGRAM-NAME        PIC X(5).                      IX1044.2
017200           07 FILLER              PIC X(7).                       IX1044.2
017300           07 XRECORD-LENGTH       PIC 9(6).                      IX1044.2
017400           07 FILLER              PIC X(7).                       IX1044.2
017500           07 CHARS-OR-RECORDS    PIC X(2).                       IX1044.2
017600           07 FILLER              PIC X(1).                       IX1044.2
017700           07 XBLOCK-SIZE          PIC 9(4).                      IX1044.2
017800           07 FILLER              PIC X(6).                       IX1044.2
017900           07 RECORDS-IN-FILE     PIC 9(6).                       IX1044.2
018000           07 FILLER              PIC X(5).                       IX1044.2
018100           07 XFILE-ORGANIZATION   PIC X(2).                      IX1044.2
018200           07 FILLER              PIC X(6).                       IX1044.2
018300           07 XLABEL-TYPE          PIC X(1).                      IX1044.2
018400        05 FILE-RECORD-INFO-P121-240.                             IX1044.2
018500           07 FILLER              PIC X(8).                       IX1044.2
018600           07 XRECORD-KEY          PIC X(29).                     IX1044.2
018700           07 FILLER              PIC X(9).                       IX1044.2
018800           07 ALTERNATE-KEY1      PIC X(29).                      IX1044.2
018900           07 FILLER              PIC X(9).                       IX1044.2
019000           07 ALTERNATE-KEY2      PIC X(29).                      IX1044.2
019100           07 FILLER              PIC X(7).                       IX1044.2
019200 01  TEST-RESULTS.                                                IX1044.2
019300     02 FILLER                   PIC X      VALUE SPACE.          IX1044.2
019400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1044.2
019500     02 FILLER                   PIC X      VALUE SPACE.          IX1044.2
019600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1044.2
019700     02 FILLER                   PIC X      VALUE SPACE.          IX1044.2
019800     02  PAR-NAME.                                                IX1044.2
019900       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1044.2
020000       03  PARDOT-X              PIC X      VALUE SPACE.          IX1044.2
020100       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1044.2
020200     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1044.2
020300     02 RE-MARK                  PIC X(61).                       IX1044.2
020400 01  TEST-COMPUTED.                                               IX1044.2
020500     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1044.2
020600     02 FILLER                   PIC X(17)  VALUE                 IX1044.2
020700            "       COMPUTED=".                                   IX1044.2
020800     02 COMPUTED-X.                                               IX1044.2
020900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1044.2
021000     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1044.2
021100                                 PIC -9(9).9(9).                  IX1044.2
021200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1044.2
021300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1044.2
021400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1044.2
021500     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1044.2
021600         04 COMPUTED-18V0                    PIC -9(18).          IX1044.2
021700         04 FILLER                           PIC X.               IX1044.2
021800     03 FILLER PIC X(50) VALUE SPACE.                             IX1044.2
021900 01  TEST-CORRECT.                                                IX1044.2
022000     02 FILLER PIC X(30) VALUE SPACE.                             IX1044.2
022100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1044.2
022200     02 CORRECT-X.                                                IX1044.2
022300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1044.2
022400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1044.2
022500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1044.2
022600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1044.2
022700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1044.2
022800     03      CR-18V0 REDEFINES CORRECT-A.                         IX1044.2
022900         04 CORRECT-18V0                     PIC -9(18).          IX1044.2
023000         04 FILLER                           PIC X.               IX1044.2
023100     03 FILLER PIC X(2) VALUE SPACE.                              IX1044.2
023200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1044.2
023300 01  CCVS-C-1.                                                    IX1044.2
023400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1044.2
023500-    "SS  PARAGRAPH-NAME                                          IX1044.2
023600-    "       REMARKS".                                            IX1044.2
023700     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1044.2
023800 01  CCVS-C-2.                                                    IX1044.2
023900     02 FILLER                     PIC X        VALUE SPACE.      IX1044.2
024000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1044.2
024100     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1044.2
024200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1044.2
024300     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1044.2
024400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1044.2
024500 01  REC-CT                        PIC 99       VALUE ZERO.       IX1044.2
024600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1044.2
024700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1044.2
024800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1044.2
024900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1044.2
025000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1044.2
025100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1044.2
025200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1044.2
025300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1044.2
025400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1044.2
025500 01  CCVS-H-1.                                                    IX1044.2
025600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1044.2
025700     02  FILLER                    PIC X(42)    VALUE             IX1044.2
025800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1044.2
025900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1044.2
026000 01  CCVS-H-2A.                                                   IX1044.2
026100   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1044.2
026200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1044.2
026300   02  FILLER                        PIC XXXX   VALUE             IX1044.2
026400     "4.2 ".                                                      IX1044.2
026500   02  FILLER                        PIC X(28)  VALUE             IX1044.2
026600            " COPY - NOT FOR DISTRIBUTION".                       IX1044.2
026700   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1044.2
026800                                                                  IX1044.2
026900 01  CCVS-H-2B.                                                   IX1044.2
027000   02  FILLER                        PIC X(15)  VALUE             IX1044.2
027100            "TEST RESULT OF ".                                    IX1044.2
027200   02  TEST-ID                       PIC X(9).                    IX1044.2
027300   02  FILLER                        PIC X(4)   VALUE             IX1044.2
027400            " IN ".                                               IX1044.2
027500   02  FILLER                        PIC X(12)  VALUE             IX1044.2
027600     " HIGH       ".                                              IX1044.2
027700   02  FILLER                        PIC X(22)  VALUE             IX1044.2
027800            " LEVEL VALIDATION FOR ".                             IX1044.2
027900   02  FILLER                        PIC X(58)  VALUE             IX1044.2
028000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1044.2
028100 01  CCVS-H-3.                                                    IX1044.2
028200     02  FILLER                      PIC X(34)  VALUE             IX1044.2
028300            " FOR OFFICIAL USE ONLY    ".                         IX1044.2
028400     02  FILLER                      PIC X(58)  VALUE             IX1044.2
028500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1044.2
028600     02  FILLER                      PIC X(28)  VALUE             IX1044.2
028700            "  COPYRIGHT   1985 ".                                IX1044.2
028800 01  CCVS-E-1.                                                    IX1044.2
028900     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1044.2
029000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1044.2
029100     02 ID-AGAIN                     PIC X(9).                    IX1044.2
029200     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1044.2
029300 01  CCVS-E-2.                                                    IX1044.2
029400     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1044.2
029500     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1044.2
029600     02 CCVS-E-2-2.                                               IX1044.2
029700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1044.2
029800         03 FILLER                   PIC X      VALUE SPACE.      IX1044.2
029900         03 ENDER-DESC               PIC X(44)  VALUE             IX1044.2
030000            "ERRORS ENCOUNTERED".                                 IX1044.2
030100 01  CCVS-E-3.                                                    IX1044.2
030200     02  FILLER                      PIC X(22)  VALUE             IX1044.2
030300            " FOR OFFICIAL USE ONLY".                             IX1044.2
030400     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1044.2
030500     02  FILLER                      PIC X(58)  VALUE             IX1044.2
030600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1044.2
030700     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1044.2
030800     02 FILLER                       PIC X(15)  VALUE             IX1044.2
030900             " COPYRIGHT 1985".                                   IX1044.2
031000 01  CCVS-E-4.                                                    IX1044.2
031100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1044.2
031200     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1044.2
031300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1044.2
031400     02 FILLER                       PIC X(40)  VALUE             IX1044.2
031500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1044.2
031600 01  XXINFO.                                                      IX1044.2
031700     02 FILLER                       PIC X(19)  VALUE             IX1044.2
031800            "*** INFORMATION ***".                                IX1044.2
031900     02 INFO-TEXT.                                                IX1044.2
032000       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1044.2
032100       04 XXCOMPUTED                 PIC X(20).                   IX1044.2
032200       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1044.2
032300       04 XXCORRECT                  PIC X(20).                   IX1044.2
032400     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1044.2
032500 01  HYPHEN-LINE.                                                 IX1044.2
032600     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1044.2
032700     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1044.2
032800-    "*****************************************".                 IX1044.2
032900     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1044.2
033000-    "******************************".                            IX1044.2
033100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1044.2
033200     "IX104A".                                                    IX1044.2
033300 PROCEDURE DIVISION.                                              IX1044.2
033400 DECLARATIVES.                                                    IX1044.2
033500 IX-FS2-01 SECTION.                                               IX1044.2
033600     USE    AFTER STANDARD ERROR PROCEDURE ON IX-FS2.             IX1044.2
033700 IX-FS2-01-01.                                                    IX1044.2
033800     ADD      1 TO  WRK-CS-09V00-013.                             IX1044.2
033900     GO TO    IX-FS2-01-03                                        IX1044.2
034000              IX-FS2-01-05                                        IX1044.2
034100              DEPENDING ON  WRK-CS-09V00-012.                     IX1044.2
034200     GO TO    IX-FS2-01-EXIT.                                     IX1044.2
034300 IX-FS2-01-03.                                                    IX1044.2
034400*ENTRY FROM SEGMENT INX-TEST-001.                                 IX1044.2
034500*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     IX1044.2
034600     ADD     1 TO  WRK-CS-09V00-014.                              IX1044.2
034700 IX-FS2-01-05.                                                    IX1044.2
034800     ADD      1 TO WRK-CS-09V00-017.                              IX1044.2
034900     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX1044.2
035000              MOVE   IX-FS2-STATUS TO WRK-XN-0002-002             IX1044.2
035100              MOVE  "10" TO WRK-XN-0002-003.                      IX1044.2
035200 IX-FS2-01-EXIT.                                                  IX1044.2
035300     EXIT.                                                        IX1044.2
035400 END DECLARATIVES.                                                IX1044.2
035500 CCVS1 SECTION.                                                   IX1044.2
035600 OPEN-FILES.                                                      IX1044.2
035700*    OPEN I-O RAW-DATA.                                           IX1044.2
035800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1044.2
035900*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1044.2
036000*    MOVE "ABORTED " TO C-ABORT.                                  IX1044.2
036100*    ADD 1 TO C-NO-OF-TESTS.                                      IX1044.2
036200*    ACCEPT C-DATE  FROM DATE.                                    IX1044.2
036300*    ACCEPT C-TIME  FROM TIME.                                    IX1044.2
036400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1044.2
036500*END-E-1.                                                         IX1044.2
036600*    CLOSE RAW-DATA.                                              IX1044.2
036700     OPEN    OUTPUT PRINT-FILE.                                   IX1044.2
036800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1044.2
036900     MOVE    SPACE TO TEST-RESULTS.                               IX1044.2
037000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1044.2
037100     MOVE    ZERO TO REC-SKL-SUB.                                 IX1044.2
037200     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1044.2
037300 CCVS-INIT-FILE.                                                  IX1044.2
037400     ADD     1 TO REC-SKL-SUB.                                    IX1044.2
037500     MOVE    FILE-RECORD-INFO-SKELETON                            IX1044.2
037600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1044.2
037700 CCVS-INIT-EXIT.                                                  IX1044.2
037800     GO TO CCVS1-EXIT.                                            IX1044.2
037900 CLOSE-FILES.                                                     IX1044.2
038000*    OPEN I-O RAW-DATA.                                           IX1044.2
038100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1044.2
038200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1044.2
038300*    MOVE "OK.     " TO C-ABORT.                                  IX1044.2
038400*    MOVE PASS-COUNTER TO C-OK.                                   IX1044.2
038500*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1044.2
038600*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1044.2
038700*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1044.2
038800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1044.2
038900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1044.2
039000*END-E-2.                                                         IX1044.2
039100*    CLOSE RAW-DATA.                                              IX1044.2
039200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1044.2
039300 TERMINATE-CCVS.                                                  IX1044.2
039400*    EXIT PROGRAM.                                                IX1044.2
039500*TERMINATE-CALL.                                                  IX1044.2
039600     STOP     RUN.                                                IX1044.2
039700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1044.2
039800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1044.2
039900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1044.2
040000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1044.2
040100     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1044.2
040200 PRINT-DETAIL.                                                    IX1044.2
040300     IF REC-CT NOT EQUAL TO ZERO                                  IX1044.2
040400             MOVE "." TO PARDOT-X                                 IX1044.2
040500             MOVE REC-CT TO DOTVALUE.                             IX1044.2
040600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1044.2
040700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1044.2
040800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1044.2
040900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1044.2
041000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1044.2
041100     MOVE SPACE TO CORRECT-X.                                     IX1044.2
041200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1044.2
041300     MOVE     SPACE TO RE-MARK.                                   IX1044.2
041400 HEAD-ROUTINE.                                                    IX1044.2
041500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1044.2
041600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1044.2
041700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1044.2
041800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1044.2
041900 COLUMN-NAMES-ROUTINE.                                            IX1044.2
042000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1044.2
042100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1044.2
042200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1044.2
042300 END-ROUTINE.                                                     IX1044.2
042400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1044.2
042500 END-RTN-EXIT.                                                    IX1044.2
042600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1044.2
042700 END-ROUTINE-1.                                                   IX1044.2
042800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1044.2
042900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1044.2
043000      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1044.2
043100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1044.2
043200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1044.2
043300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1044.2
043400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1044.2
043500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1044.2
043600  END-ROUTINE-12.                                                 IX1044.2
043700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1044.2
043800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1044.2
043900         MOVE "NO " TO ERROR-TOTAL                                IX1044.2
044000         ELSE                                                     IX1044.2
044100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1044.2
044200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1044.2
044300     PERFORM WRITE-LINE.                                          IX1044.2
044400 END-ROUTINE-13.                                                  IX1044.2
044500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1044.2
044600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1044.2
044700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1044.2
044800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1044.2
044900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1044.2
045000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1044.2
045100          MOVE "NO " TO ERROR-TOTAL                               IX1044.2
045200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1044.2
045300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1044.2
045400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1044.2
045500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1044.2
045600 WRITE-LINE.                                                      IX1044.2
045700     ADD 1 TO RECORD-COUNT.                                       IX1044.2
045800*    IF RECORD-COUNT GREATER 42                                   IX1044.2
045900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1044.2
046000*        MOVE SPACE TO DUMMY-RECORD                               IX1044.2
046100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1044.2
046200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1044.2
046300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1044.2
046400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1044.2
046500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1044.2
046600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1044.2
046700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1044.2
046800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1044.2
046900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1044.2
047000*        MOVE ZERO TO RECORD-COUNT.                               IX1044.2
047100     PERFORM WRT-LN.                                              IX1044.2
047200 WRT-LN.                                                          IX1044.2
047300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1044.2
047400     MOVE SPACE TO DUMMY-RECORD.                                  IX1044.2
047500 BLANK-LINE-PRINT.                                                IX1044.2
047600     PERFORM WRT-LN.                                              IX1044.2
047700 FAIL-ROUTINE.                                                    IX1044.2
047800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1044.2
047900            GO TO   FAIL-ROUTINE-WRITE.                           IX1044.2
048000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1044.2
048100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1044.2
048200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1044.2
048300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1044.2
048400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1044.2
048500     GO TO  FAIL-ROUTINE-EX.                                      IX1044.2
048600 FAIL-ROUTINE-WRITE.                                              IX1044.2
048700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1044.2
048800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1044.2
048900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1044.2
049000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1044.2
049100 FAIL-ROUTINE-EX. EXIT.                                           IX1044.2
049200 BAIL-OUT.                                                        IX1044.2
049300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1044.2
049400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1044.2
049500 BAIL-OUT-WRITE.                                                  IX1044.2
049600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1044.2
049700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1044.2
049800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1044.2
049900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1044.2
050000 BAIL-OUT-EX. EXIT.                                               IX1044.2
050100 CCVS1-EXIT.                                                      IX1044.2
050200     EXIT.                                                        IX1044.2
050300 SECT-IX-04-001 SECTION.                                          IX1044.2
050400 WRITE-INIT-GF-01.                                                IX1044.2
050500     MOVE     "CREATE IX-FS2" TO  FEATURE                         IX1044.2
050600     MOVE     "IX-FS2"        TO  XFILE-NAME (2).                 IX1044.2
050700     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               IX1044.2
050800     MOVE     CCVS-PGM-ID     TO  XPROGRAM-NAME (2).              IX1044.2
050900     MOVE     000240          TO  XRECORD-LENGTH (2).             IX1044.2
051000     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           IX1044.2
051100     MOVE     0001            TO  XBLOCK-SIZE       (2).          IX1044.2
051200     MOVE     000500          TO  RECORDS-IN-FILE  (2).           IX1044.2
051300     MOVE    "IX"                TO XFILE-ORGANIZATION (2).       IX1044.2
051400     MOVE    "WRITE-TEST-GF-01" TO PAR-NAME.                      IX1044.2
051500     MOVE     "S"             TO  XLABEL-TYPE        (2).         IX1044.2
051600     MOVE     000001          TO  XRECORD-NUMBER     (2).         IX1044.2
051700*INITIALIZE  RECORD WORK AREA NUMBER 2.                           IX1044.2
051800     MOVE     1         TO  WRK-CS-09V00-012.                     IX1044.2
051900     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     IX1044.2
052000                            WRK-CS-09V00-015 WRK-CS-09V00-016     IX1044.2
052100                            WRK-CS-09V00-017 WRK-CS-09V00-018.    IX1044.2
052200     MOVE     SPACE     TO  IX-FS2-STATUS.                        IX1044.2
052300     MOVE    ZERO TO WRK-DU-09V00-001.                            IX1044.2
052400     OPEN     OUTPUT    IX-FS2.                                   IX1044.2
052500     MOVE GRP-0101 TO IX-FS2-KEY.                                 IX1044.2
052600     MOVE     IX-FS2-STATUS TO WRK-XN-0002-001.                   IX1044.2
052700*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            IX1044.2
052800 WRITE-TEST-GF-01.                                                IX1044.2
052900     MOVE    "99"  TO IX-FS2-STATUS.                              IX1044.2
053000     MOVE     XRECORD-NUMBER (2) TO WRK-DU-09V00-001.             IX1044.2
053100     MOVE     GRP-0101 TO XRECORD-KEY (2).                        IX1044.2
053200     MOVE     FILE-RECORD-INFO (2) TO IX-FS2R1-F-G-240.           IX1044.2
053300     WRITE   IX-FS2R1-F-G-240.                                    IX1044.2
053400     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX1044.2
053500         ADD  1  TO WRK-CS-09V00-016.                             IX1044.2
053600     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX1044.2
053700              GO TO  WRITE-TEST-GF-01-2.                          IX1044.2
053800     ADD      01 TO  XRECORD-NUMBER (2).                          IX1044.2
053900     GO TO    WRITE-TEST-GF-01.                                   IX1044.2
054000 WRITE-TEST-GF-01-2.                                              IX1044.2
054100     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  IX1044.2
054200              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 IX1044.2
054300              MOVE  ZERO TO CORRECT-18V0                          IX1044.2
054400              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX1044.2
054500              MOVE "IX-41 4.9.2                       " TO RE-MARKIX1044.2
054600              PERFORM   FAIL                                      IX1044.2
054700              ELSE                                                IX1044.2
054800              PERFORM   PASS.                                     IX1044.2
054900     PERFORM  PRINT-DETAIL.                                       IX1044.2
055000 WRITE-TEST-GF-02.                                                IX1044.2
055100     MOVE     "CREATE IX-FS2" TO  FEATURE                         IX1044.2
055200     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX1044.2
055300     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 IX1044.2
055400              MOVE "INCORRECT COUNT" TO RE-MARK                   IX1044.2
055500              MOVE  500 TO CORRECT-18V0                           IX1044.2
055600             MOVE   XRECORD-NUMBER (2)  TO COMPUTED-18V0          IX1044.2
055700              MOVE "IX-41 4.9.2                       " TO RE-MARKIX1044.2
055800              PERFORM  FAIL                                       IX1044.2
055900              ELSE                                                IX1044.2
056000              PERFORM PASS.                                       IX1044.2
056100     PERFORM  PRINT-DETAIL.                                       IX1044.2
056200 WRITE-TEST-GF-03.                                                IX1044.2
056300     MOVE "OPEN: 00 EXP.    " TO  FEATURE.                        IX1044.2
056400     MOVE    "WRITE-TEST-GF-03" TO PAR-NAME.                      IX1044.2
056500     IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  IX1044.2
056600              MOVE WRK-XN-0002-001 TO COMPUTED-A                  IX1044.2
056700              MOVE "00" TO CORRECT-A                              IX1044.2
056800              MOVE "IX-41 4.9.2; IX-3 1.3.4  (1) A    " TO RE-MARKIX1044.2
056900              PERFORM  FAIL                                       IX1044.2
057000              ELSE                                                IX1044.2
057100              PERFORM  PASS.                                      IX1044.2
057200     PERFORM  PRINT-DETAIL.                                       IX1044.2
057300 WRITE-TEST-GF-04.                                                IX1044.2
057400     MOVE     "WRITE: 00 EXP." TO  FEATURE.                       IX1044.2
057500     MOVE    "WRITE-TEST-GF-04" TO PAR-NAME.                      IX1044.2
057600     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX1044.2
057700              MOVE IX-FS2-STATUS TO COMPUTED-A                    IX1044.2
057800              MOVE "00" TO CORRECT-A                              IX1044.2
057900              MOVE "IX-41 4.9.2; IX-3 1.3.4 (1) A     " TO RE-MARKIX1044.2
058000              PERFORM   FAIL                                      IX1044.2
058100              ELSE                                                IX1044.2
058200              PERFORM   PASS.                                     IX1044.2
058300     PERFORM  PRINT-DETAIL.                                       IX1044.2
058400 WRITE-TEST-GF-05.                                                IX1044.2
058500     MOVE "WRITE: 00 EXP.   " TO  FEATURE.                        IX1044.2
058600     MOVE    "WRITE-TEST-GF-05" TO PAR-NAME.                      IX1044.2
058700     IF      WRK-CS-09V00-016    NOT EQUAL TO ZERO                IX1044.2
058800              MOVE   ZERO TO CORRECT-18V0                         IX1044.2
058900              MOVE    WRK-CS-09V00-016 TO COMPUTED-18V0           IX1044.2
059000              MOVE "IX-41 4.9.2; IX-3 1.3.4 (1) A     " TO RE-MARKIX1044.2
059100              PERFORM   FAIL                                      IX1044.2
059200              ELSE                                                IX1044.2
059300              PERFORM  PASS.                                      IX1044.2
059400     PERFORM  PRINT-DETAIL.                                       IX1044.2
059500 WRITE-TEST-GF-06.                                                IX1044.2
059600     MOVE "CLOSE: 00 EXP.   " TO  FEATURE.                        IX1044.2
059700     MOVE    "WRITE-TEST-GF-06" TO PAR-NAME.                      IX1044.2
059800     MOVE     SPACE  TO  IX-FS2-STATUS.                           IX1044.2
059900     CLOSE    IX-FS2.                                             IX1044.2
060000     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX1044.2
060100              MOVE "CLOSE/STATUS; IX-3 1.3.4 (1) A" TO RE-MARK    IX1044.2
060200              MOVE   IX-FS2-STATUS TO COMPUTED-A                  IX1044.2
060300              MOVE "00" TO CORRECT-A                              IX1044.2
060400              PERFORM FAIL                                        IX1044.2
060500              ELSE                                                IX1044.2
060600              PERFORM PASS.                                       IX1044.2
060700     PERFORM  PRINT-DETAIL.                                       IX1044.2
060800 READ-INIT-F1-01.                                                 IX1044.2
060900     MOVE     2     TO  WRK-CS-09V00-012.                         IX1044.2
061000     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX1044.2
061100     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX1044.2
061200     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX1044.2
061300     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX1044.2
061400     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX1044.2
061500     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX1044.2
061600     MOVE    SPACE TO IX-FS2-STATUS.                              IX1044.2
061700     OPEN     I-O  IX-FS2.                                        IX1044.2
061800     MOVE     SPACE     TO  WRK-XN-0002-002                       IX1044.2
061900     MOVE     SPACE     TO  WRK-XN-0002-003                       IX1044.2
062000     MOVE     SPACE     TO  WRK-XN-0002-004                       IX1044.2
062100     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1044.2
062200     MOVE     SPACE TO IX-FS2-STATUS.                             IX1044.2
062300*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       IX1044.2
062400 READ-TEST-F1-01.                                                 IX1044.2
062500     ADD      1 TO WRK-CS-09V00-014.                              IX1044.2
062600     ADD      1 TO WRK-CS-09V00-015.                              IX1044.2
062700     READ  IX-FS2.                                                IX1044.2
062800     IF        IX-FS2-STATUS EQUAL TO "10"                        IX1044.2
062900              GO TO  READ-TEST-F1-01-3.                           IX1044.2
063000     MOVE     IX-FS2R1-F-G-240 TO FILE-RECORD-INFO (2).           IX1044.2
063100     IF       WRK-CS-09V00-015 EQUAL TO 5                         IX1044.2
063200              ADD 01 TO UPDATE-NUMBER (2)                         IX1044.2
063300     MOVE     FILE-RECORD-INFO (2) TO IX-FS2R1-F-G-240            IX1044.2
063400             PERFORM  READ-010-UPDATE                             IX1044.2
063500           MOVE ZERO TO WRK-CS-09V00-015                          IX1044.2
063600              GO TO  READ-TEST-F1-01-2.                           IX1044.2
063700     IF       WRK-CS-09V00-014 GREATER 500                        IX1044.2
063800              GO TO  READ-TEST-F1-01-3.                           IX1044.2
063900     GO TO    READ-TEST-F1-01.                                    IX1044.2
064000 READ-010-UPDATE.                                                 IX1044.2
064100     REWRITE   IX-FS2R1-F-G-240.                                  IX1044.2
064200 READ-TEST-F1-01-2.                                               IX1044.2
064300     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX1044.2
064400              ADD 1 TO WRK-CS-09V00-016.                          IX1044.2
064500     MOVE    SPACE  TO   IX-FS2-STATUS.                           IX1044.2
064600     GO TO    READ-TEST-F1-01.                                    IX1044.2
064700 READ-TEST-F1-01-3.                                               IX1044.2
064800     MOVE    "READ: 10 EXP. "  TO FEATURE.                        IX1044.2
064900     MOVE    "READ-TEST-F1-01-3" TO PAR-NAME.                     IX1044.2
065000     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     IX1044.2
065100              MOVE  "IX-4 1.3.4 (2) A                 " TO RE-MARKIX1044.2
065200              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              IX1044.2
065300              MOVE  1 TO CORRECT-18V0                             IX1044.2
065400              PERFORM FAIL                                        IX1044.2
065500              ELSE                                                IX1044.2
065600              PERFORM PASS.                                       IX1044.2
065700     PERFORM  PRINT-DETAIL.                                       IX1044.2
065800 READ-TEST-F1-02.                                                 IX1044.2
065900     MOVE    "READ                "  TO FEATURE.                  IX1044.2
066000     MOVE    "READ-TEST-F1-02  " TO PAR-NAME.                     IX1044.2
066100     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   IX1044.2
066200              MOVE "INCORRECT COUNT IX-28 4.5.2" TO RE-MARK       IX1044.2
066300              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX1044.2
066400              MOVE 501 TO CORRECT-18V0                            IX1044.2
066500              PERFORM FAIL                                        IX1044.2
066600              ELSE                                                IX1044.2
066700              PERFORM PASS.                                       IX1044.2
066800     PERFORM  PRINT-DETAIL.                                       IX1044.2
066900 READ-TEST-F1-03.                                                 IX1044.2
067000     MOVE    "OPEN:  00 EXP.      "  TO FEATURE.                  IX1044.2
067100     MOVE    "READ-TEST-F1-03  " TO PAR-NAME.                     IX1044.2
067200     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   IX1044.2
067300              MOVE  "IX-3 1.3.4 (1) A                 " TO RE-MARKIX1044.2
067400              MOVE WRK-XN-0002-001 TO COMPUTED-A                  IX1044.2
067500              MOVE  "00" TO CORRECT-A                             IX1044.2
067600              PERFORM FAIL                                        IX1044.2
067700              ELSE                                                IX1044.2
067800              PERFORM PASS.                                       IX1044.2
067900     PERFORM  PRINT-DETAIL.                                       IX1044.2
068000 READ-TEST-F1-04.                                                 IX1044.2
068100     MOVE    "READ AT END: 10 EXP."  TO FEATURE.                  IX1044.2
068200     MOVE    "READ-TEST-F1-04  " TO PAR-NAME.                     IX1044.2
068300     IF        IX-FS2-STATUS NOT EQUAL  TO "10"                   IX1044.2
068400              MOVE "ATEND/STATUS" TO RE-MARK                      IX1044.2
068500              MOVE  "IX-4 1.3.4 (2) A                 " TO RE-MARKIX1044.2
068600              MOVE  IX-FS2-STATUS TO COMPUTED-A                   IX1044.2
068700              MOVE "10" TO CORRECT-A                              IX1044.2
068800              PERFORM FAIL                                        IX1044.2
068900              ELSE                                                IX1044.2
069000              PERFORM PASS.                                       IX1044.2
069100     PERFORM  PRINT-DETAIL.                                       IX1044.2
069200 READ-TEST-F1-05.                                                 IX1044.2
069300     MOVE    "READ:   10 EXP.     "  TO FEATURE.                  IX1044.2
069400     MOVE    "READ-TEST-F1-05  " TO PAR-NAME.                     IX1044.2
069500     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   IX1044.2
069600              MOVE  "IX-4 1.3.4 (2) A                 " TO RE-MARKIX1044.2
069700              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 IX1044.2
069800              MOVE  "10" TO CORRECT-A                             IX1044.2
069900              PERFORM FAIL                                        IX1044.2
070000              ELSE                                                IX1044.2
070100              PERFORM PASS.                                       IX1044.2
070200     PERFORM  PRINT-DETAIL.                                       IX1044.2
070300 READ-TEST-F1-06.                                                 IX1044.2
070400     MOVE    "READ NO EXCEPTION 10"  TO FEATURE.                  IX1044.2
070500     MOVE    "READ-TEST-F1-06  " TO PAR-NAME.                     IX1044.2
070600     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   IX1044.2
070700              MOVE "NO/EXCEPTION IX-4 1.3.4 (2) A" TO  RE-MARK    IX1044.2
070800              MOVE WRK-XN-0002-003 TO COMPUTED-A                  IX1044.2
070900              MOVE "10" TO CORRECT-A                              IX1044.2
071000              PERFORM FAIL                                        IX1044.2
071100              ELSE                                                IX1044.2
071200              PERFORM PASS.                                       IX1044.2
071300     PERFORM  PRINT-DETAIL.                                       IX1044.2
071400 READ-TEST-F1-07.                                                 IX1044.2
071500     MOVE    "CLOSE:    00 EXP.   "  TO FEATURE.                  IX1044.2
071600     MOVE    "READ-TEST-F1-07  " TO PAR-NAME.                     IX1044.2
071700     MOVE     SPACE TO IX-FS2-STATUS.                             IX1044.2
071800     CLOSE    IX-FS2                                              IX1044.2
071900     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX1044.2
072000              MOVE "CLOSE/STATUS  IX-3  1.3.4 (1) A" TO RE-MARK   IX1044.2
072100              MOVE  IX-FS2-STATUS TO  COMPUTED-A                  IX1044.2
072200              MOVE "00" TO CORRECT-A                              IX1044.2
072300              PERFORM FAIL                                        IX1044.2
072400              ELSE                                                IX1044.2
072500              PERFORM PASS.                                       IX1044.2
072600     PERFORM  PRINT-DETAIL.                                       IX1044.2
072700 CCVS-EXIT SECTION.                                               IX1044.2
072800 CCVS-999999.                                                     IX1044.2
072900     GO TO CLOSE-FILES.                                           IX1044.2
      *END-OF,IX104A                                                                  
