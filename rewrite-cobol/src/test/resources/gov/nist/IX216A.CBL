      *HEADER,COBOL,IX216A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2164.2
000200 PROGRAM-ID.                                                      IX2164.2
000300     IX216A.                                                      IX2164.2
000400****************************************************************  IX2164.2
000500*                                                              *  IX2164.2
000600*    VALIDATION FOR:-                                          *  IX2164.2
000700*                                                              *  IX2164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2164.2
000900*                                                              *  IX2164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2164.2
001100*                                                              *  IX2164.2
001200****************************************************************  IX2164.2
001300*                                                                 IX2164.2
001400*         NEW TESTS:                                              IX2164.2
001500*                                                                 IX2164.2
001600*             SELECT OPTIONAL ...   WITH                          IX2164.2
001700*                    --------                                     IX2164.2
001800*             OPEN EXTEND ...   (FOR A NON-EXISTING FILE)         IX2164.2
001900*                  ------              ------------               IX2164.2
002000*         THEN THE FILE IS CLOSED AFTER WRITING 300 RECORDS       IX2164.2
002100*         AND OPENED WITH:                                        IX2164.2
002200*                                                                 IX2164.2
002300*             OPEN EXTEND ...   (FOR AN EXISTING FILE)            IX2164.2
002400*                  ------               --------                  IX2164.2
002500*         AND     CLOSE IX-FS2 LOCK.                              IX2164.2
002600*                              ----                               IX2164.2
002700*                                                                 IX2164.2
002800*         ALL OTHER TESTS ARE IDENTICAL WITH THE TESTS IN IX104.  IX2164.2
002900*                                                                 IX2164.2
003000*                                                                 IX2164.2
003100*    THIS PROGRAM TESTS THE SYNTACTICAL CONSTRUCTS AND SEMANTIC   IX2164.2
003200*    ACTIONS ASSOCIATED WITH THE FOLLOWING ELEMENTS:              IX2164.2
003300*                                                                 IX2164.2
003400*    (1) FILE STATUS                                              IX2164.2
003500*    (2) USE AFTER EXCEPTION USING FILE-NAME                      IX2164.2
003600*    (3) READ                                                     IX2164.2
003700*    (4) WRITE                                                    IX2164.2
003800*    (5) REWRITE                                                  IX2164.2
003900*    (6) RECORD KEY                                               IX2164.2
004000*    (7) ACCESS                                                   IX2164.2
004100*                                                                 IX2164.2
004200*    THIS PROGRAM CREATES AN INDEXED FILE SEQUENTIALLY (ACCESS    IX2164.2
004300*    MODE SEQUENTIAL) AND THEN UPDATES SELECTIVE RECORDS OF THE   IX2164.2
004400*    FILE.  THE FILE STATUS CONTENTS ARE CAPTURED AND TESTED FOR  IX2164.2
004500*    ACCURACY FOR EACH OPEN, CLOSE, READ AND REWRITE STATEMENT    IX2164.2
004600*    USED.  THE READ, WRITE AND REWRITE STATEMENTS ARE USED       IX2164.2
004700*    WITHOUT THE APPROPRIATE AT END OR INVALID KEY PHRASES. THE   IX2164.2
004800*    OMISSION OF THESE PHRASES ARE PERMITTED IF AN APPLICABLE USE IX2164.2
004900*    PROCEDURE HAS BEEN SPECIFIED.                                IX2164.2
005000*                                                                 IX2164.2
005100*                                                                 IX2164.2
005200*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2164.2
005300*                                                                 IX2164.2
005400*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2164.2
005500*                    CLAUSE FOR DATA FILE IX-FD2                  IX2164.2
005600*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2164.2
005700*                    CLAUSE FOR INDEX FILE IX-FD2                 IX2164.2
005800*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2164.2
005900*             X-62   IMPLEMENTOR-NAME FOR RAW-DATA (OPTIONAL)     IX2164.2
006000*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2164.2
006100*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2164.2
006200*                                                                 IX2164.2
006300*         NOTE:  X-CARDS 45 AND 62          ARE OPTIONAL          IX2164.2
006400*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2164.2
006500*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2164.2
006600*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2164.2
006700*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2164.2
006800*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2164.2
006900*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2164.2
007000*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2164.2
007100*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2164.2
007200*               THEY ARE AS FOLLOWS                               IX2164.2
007300*                                                                 IX2164.2
007400*                  J  SELECTS X-CARD 45                           IX2164.2
007500*                  P  SELECTS X-CARD 62                           IX2164.2
007600*                                                                 IX2164.2
007700******************************************************            IX2164.2
007800 ENVIRONMENT DIVISION.                                            IX2164.2
007900 CONFIGURATION SECTION.                                           IX2164.2
008000 SOURCE-COMPUTER.                                                 IX2164.2
008100     XXXXX082.                                                    IX2164.2
008200 OBJECT-COMPUTER.                                                 IX2164.2
008300     XXXXX083.                                                    IX2164.2
008400 INPUT-OUTPUT SECTION.                                            IX2164.2
008500 FILE-CONTROL.                                                    IX2164.2
008600*    SELECT RAW-DATA   ASSIGN TO                                  IX2164.2
008700*    XXXXX062                                                     IX2164.2
008800*           ORGANIZATION IS INDEXED                               IX2164.2
008900*           ACCESS MODE IS RANDOM                                 IX2164.2
009000*           RECORD KEY IS RAW-DATA-KEY.                           IX2164.2
009100     SELECT PRINT-FILE ASSIGN TO                                  IX2164.2
009200     XXXXX055.                                                    IX2164.2
009300     SELECT OPTIONAL  IX-FS2 ASSIGN                               IX2164.2
009400     XXXXX025                                                     IX2164.2
009500*    XXXXX045                                                     IX2164.2
009600             ORGANIZATION IS INDEXED                              IX2164.2
009700        ACCESS SEQUENTIAL                                         IX2164.2
009800        FILE STATUS IS IX-FS2-STATUS                              IX2164.2
009900        RECORD  IX-FS2-KEY.                                       IX2164.2
010000 DATA DIVISION.                                                   IX2164.2
010100 FILE SECTION.                                                    IX2164.2
010200*                                                                 IX2164.2
010300*FD  RAW-DATA.                                                    IX2164.2
010400*                                                                 IX2164.2
010500*01  RAW-DATA-SATZ.                                               IX2164.2
010600*    05  RAW-DATA-KEY        PIC X(6).                            IX2164.2
010700*    05  C-DATE              PIC 9(6).                            IX2164.2
010800*    05  C-TIME              PIC 9(8).                            IX2164.2
010900*    05  C-NO-OF-TESTS       PIC 99.                              IX2164.2
011000*    05  C-OK                PIC 999.                             IX2164.2
011100*    05  C-ALL               PIC 999.                             IX2164.2
011200*    05  C-FAIL              PIC 999.                             IX2164.2
011300*    05  C-DELETED           PIC 999.                             IX2164.2
011400*    05  C-INSPECT           PIC 999.                             IX2164.2
011500*    05  C-NOTE              PIC X(13).                           IX2164.2
011600*    05  C-INDENT            PIC X.                               IX2164.2
011700*    05  C-ABORT             PIC X(8).                            IX2164.2
011800 FD  PRINT-FILE.                                                  IX2164.2
011900 01  PRINT-REC PICTURE X(120).                                    IX2164.2
012000 01  DUMMY-RECORD PICTURE X(120).                                 IX2164.2
012100 FD  IX-FS2                                                       IX2164.2
012200C    LABEL RECORDS ARE STANDARD                                   IX2164.2
012300C    DATA RECORDS IX-FS2R1-F-G-240                                IX2164.2
012400     BLOCK CONTAINS 480                                           IX2164.2
012500     RECORD CONTAINS  240  CHARACTERS.                            IX2164.2
012600 01  IX-FS2R1-F-G-240.                                            IX2164.2
012700     05 IX-FS2-REC-120    PIC X(120).                             IX2164.2
012800     05 IX-FS2-REC-120-240.                                       IX2164.2
012900     10  FILLER PICTURE X(8).                                     IX2164.2
013000     10  IX-FS2-KEY PIC X(29).                                    IX2164.2
013100     10  FILLER PIC X(83).                                        IX2164.2
013200 WORKING-STORAGE SECTION.                                         IX2164.2
013300 01  GRP-0101.                                                    IX2164.2
013400     02 FILLER PIC X(10) VALUE "ABCD921XYZ".                      IX2164.2
013500     02  WRK-DU-09V00-001 PIC 9(9) VALUE ZERO.                    IX2164.2
013600     02 FILLER PIC X(10) VALUE "Z2F()$+-AB".                      IX2164.2
013700 01  GRP-0001.                                                    IX2164.2
013800     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
013900     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
014000     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
014100     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
014200     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
014300     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
014400     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       IX2164.2
014500     05 IX-FS2-STATUS     PIC  XX  VALUE  SPACE.                  IX2164.2
014600     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 IX2164.2
014700     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 IX2164.2
014800     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 IX2164.2
014900     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 IX2164.2
015000     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 IX2164.2
015100     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 IX2164.2
015200     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 IX2164.2
015300     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 IX2164.2
015400     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 IX2164.2
015500 01  DUMMY-WRK-REC.                                               IX2164.2
015600     02 DUMMY-WRK1       PIC X(120).                              IX2164.2
015700     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX2164.2
015800        03 FILLER   PIC X(5).                                     IX2164.2
015900        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX2164.2
016000 01  FILE-RECORD-INFORMATION-REC.                                 IX2164.2
016100     03 FILE-RECORD-INFO-SKELETON.                                IX2164.2
016200        05 FILLER                 PICTURE X(48)       VALUE       IX2164.2
016300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2164.2
016400        05 FILLER                 PICTURE X(46)       VALUE       IX2164.2
016500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2164.2
016600        05 FILLER                 PICTURE X(26)       VALUE       IX2164.2
016700             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2164.2
016800        05 FILLER                 PICTURE X(37)       VALUE       IX2164.2
016900             ",RECKEY=                             ".             IX2164.2
017000        05 FILLER                 PICTURE X(38)       VALUE       IX2164.2
017100             ",ALTKEY1=                             ".            IX2164.2
017200        05 FILLER                 PICTURE X(38)       VALUE       IX2164.2
017300             ",ALTKEY2=                             ".            IX2164.2
017400        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2164.2
017500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2164.2
017600        05 FILE-RECORD-INFO-P1-120.                               IX2164.2
017700           07 FILLER              PIC X(5).                       IX2164.2
017800           07 XFILE-NAME           PIC X(6).                      IX2164.2
017900           07 FILLER              PIC X(8).                       IX2164.2
018000           07 XRECORD-NAME         PIC X(6).                      IX2164.2
018100           07 FILLER              PIC X(1).                       IX2164.2
018200           07 REELUNIT-NUMBER     PIC 9(1).                       IX2164.2
018300           07 FILLER              PIC X(7).                       IX2164.2
018400           07 XRECORD-NUMBER       PIC 9(6).                      IX2164.2
018500           07 FILLER              PIC X(6).                       IX2164.2
018600           07 UPDATE-NUMBER       PIC 9(2).                       IX2164.2
018700           07 FILLER              PIC X(5).                       IX2164.2
018800           07 ODO-NUMBER          PIC 9(4).                       IX2164.2
018900           07 FILLER              PIC X(5).                       IX2164.2
019000           07 XPROGRAM-NAME        PIC X(5).                      IX2164.2
019100           07 FILLER              PIC X(7).                       IX2164.2
019200           07 XRECORD-LENGTH       PIC 9(6).                      IX2164.2
019300           07 FILLER              PIC X(7).                       IX2164.2
019400           07 CHARS-OR-RECORDS    PIC X(2).                       IX2164.2
019500           07 FILLER              PIC X(1).                       IX2164.2
019600           07 XBLOCK-SIZE          PIC 9(4).                      IX2164.2
019700           07 FILLER              PIC X(6).                       IX2164.2
019800           07 RECORDS-IN-FILE     PIC 9(6).                       IX2164.2
019900           07 FILLER              PIC X(5).                       IX2164.2
020000           07 XFILE-ORGANIZATION   PIC X(2).                      IX2164.2
020100           07 FILLER              PIC X(6).                       IX2164.2
020200           07 XLABEL-TYPE          PIC X(1).                      IX2164.2
020300        05 FILE-RECORD-INFO-P121-240.                             IX2164.2
020400           07 FILLER              PIC X(8).                       IX2164.2
020500           07 XRECORD-KEY          PIC X(29).                     IX2164.2
020600           07 FILLER              PIC X(9).                       IX2164.2
020700           07 ALTERNATE-KEY1      PIC X(29).                      IX2164.2
020800           07 FILLER              PIC X(9).                       IX2164.2
020900           07 ALTERNATE-KEY2      PIC X(29).                      IX2164.2
021000           07 FILLER              PIC X(7).                       IX2164.2
021100 01  TEST-RESULTS.                                                IX2164.2
021200     02 FILLER                   PIC X      VALUE SPACE.          IX2164.2
021300     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2164.2
021400     02 FILLER                   PIC X      VALUE SPACE.          IX2164.2
021500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2164.2
021600     02 FILLER                   PIC X      VALUE SPACE.          IX2164.2
021700     02  PAR-NAME.                                                IX2164.2
021800       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2164.2
021900       03  PARDOT-X              PIC X      VALUE SPACE.          IX2164.2
022000       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2164.2
022100     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2164.2
022200     02 RE-MARK                  PIC X(61).                       IX2164.2
022300 01  TEST-COMPUTED.                                               IX2164.2
022400     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2164.2
022500     02 FILLER                   PIC X(17)  VALUE                 IX2164.2
022600            "       COMPUTED=".                                   IX2164.2
022700     02 COMPUTED-X.                                               IX2164.2
022800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2164.2
022900     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2164.2
023000                                 PIC -9(9).9(9).                  IX2164.2
023100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2164.2
023200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2164.2
023300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2164.2
023400     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2164.2
023500         04 COMPUTED-18V0                    PIC -9(18).          IX2164.2
023600         04 FILLER                           PIC X.               IX2164.2
023700     03 FILLER PIC X(50) VALUE SPACE.                             IX2164.2
023800 01  TEST-CORRECT.                                                IX2164.2
023900     02 FILLER PIC X(30) VALUE SPACE.                             IX2164.2
024000     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2164.2
024100     02 CORRECT-X.                                                IX2164.2
024200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2164.2
024300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2164.2
024400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2164.2
024500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2164.2
024600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2164.2
024700     03      CR-18V0 REDEFINES CORRECT-A.                         IX2164.2
024800         04 CORRECT-18V0                     PIC -9(18).          IX2164.2
024900         04 FILLER                           PIC X.               IX2164.2
025000     03 FILLER PIC X(2) VALUE SPACE.                              IX2164.2
025100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2164.2
025200 01  CCVS-C-1.                                                    IX2164.2
025300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2164.2
025400-    "SS  PARAGRAPH-NAME                                          IX2164.2
025500-    "       REMARKS".                                            IX2164.2
025600     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2164.2
025700 01  CCVS-C-2.                                                    IX2164.2
025800     02 FILLER                     PIC X        VALUE SPACE.      IX2164.2
025900     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2164.2
026000     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2164.2
026100     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2164.2
026200     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2164.2
026300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2164.2
026400 01  REC-CT                        PIC 99       VALUE ZERO.       IX2164.2
026500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2164.2
026600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2164.2
026700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2164.2
026800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2164.2
026900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2164.2
027000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2164.2
027100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2164.2
027200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2164.2
027300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2164.2
027400 01  CCVS-H-1.                                                    IX2164.2
027500     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2164.2
027600     02  FILLER                    PIC X(42)    VALUE             IX2164.2
027700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2164.2
027800     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2164.2
027900 01  CCVS-H-2A.                                                   IX2164.2
028000   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2164.2
028100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2164.2
028200   02  FILLER                        PIC XXXX   VALUE             IX2164.2
028300     "4.2 ".                                                      IX2164.2
028400   02  FILLER                        PIC X(28)  VALUE             IX2164.2
028500            " COPY - NOT FOR DISTRIBUTION".                       IX2164.2
028600   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2164.2
028700                                                                  IX2164.2
028800 01  CCVS-H-2B.                                                   IX2164.2
028900   02  FILLER                        PIC X(15)  VALUE             IX2164.2
029000            "TEST RESULT OF ".                                    IX2164.2
029100   02  TEST-ID                       PIC X(9).                    IX2164.2
029200   02  FILLER                        PIC X(4)   VALUE             IX2164.2
029300            " IN ".                                               IX2164.2
029400   02  FILLER                        PIC X(12)  VALUE             IX2164.2
029500     " HIGH       ".                                              IX2164.2
029600   02  FILLER                        PIC X(22)  VALUE             IX2164.2
029700            " LEVEL VALIDATION FOR ".                             IX2164.2
029800   02  FILLER                        PIC X(58)  VALUE             IX2164.2
029900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2164.2
030000 01  CCVS-H-3.                                                    IX2164.2
030100     02  FILLER                      PIC X(34)  VALUE             IX2164.2
030200            " FOR OFFICIAL USE ONLY    ".                         IX2164.2
030300     02  FILLER                      PIC X(58)  VALUE             IX2164.2
030400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2164.2
030500     02  FILLER                      PIC X(28)  VALUE             IX2164.2
030600            "  COPYRIGHT   1985 ".                                IX2164.2
030700 01  CCVS-E-1.                                                    IX2164.2
030800     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2164.2
030900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2164.2
031000     02 ID-AGAIN                     PIC X(9).                    IX2164.2
031100     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2164.2
031200 01  CCVS-E-2.                                                    IX2164.2
031300     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2164.2
031400     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2164.2
031500     02 CCVS-E-2-2.                                               IX2164.2
031600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2164.2
031700         03 FILLER                   PIC X      VALUE SPACE.      IX2164.2
031800         03 ENDER-DESC               PIC X(44)  VALUE             IX2164.2
031900            "ERRORS ENCOUNTERED".                                 IX2164.2
032000 01  CCVS-E-3.                                                    IX2164.2
032100     02  FILLER                      PIC X(22)  VALUE             IX2164.2
032200            " FOR OFFICIAL USE ONLY".                             IX2164.2
032300     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2164.2
032400     02  FILLER                      PIC X(58)  VALUE             IX2164.2
032500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2164.2
032600     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2164.2
032700     02 FILLER                       PIC X(15)  VALUE             IX2164.2
032800             " COPYRIGHT 1985".                                   IX2164.2
032900 01  CCVS-E-4.                                                    IX2164.2
033000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2164.2
033100     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2164.2
033200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2164.2
033300     02 FILLER                       PIC X(40)  VALUE             IX2164.2
033400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2164.2
033500 01  XXINFO.                                                      IX2164.2
033600     02 FILLER                       PIC X(19)  VALUE             IX2164.2
033700            "*** INFORMATION ***".                                IX2164.2
033800     02 INFO-TEXT.                                                IX2164.2
033900       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2164.2
034000       04 XXCOMPUTED                 PIC X(20).                   IX2164.2
034100       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2164.2
034200       04 XXCORRECT                  PIC X(20).                   IX2164.2
034300     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2164.2
034400 01  HYPHEN-LINE.                                                 IX2164.2
034500     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2164.2
034600     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2164.2
034700-    "*****************************************".                 IX2164.2
034800     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2164.2
034900-    "******************************".                            IX2164.2
035000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2164.2
035100     "IX216A".                                                    IX2164.2
035200 PROCEDURE DIVISION.                                              IX2164.2
035300 DECLARATIVES.                                                    IX2164.2
035400 IX-FS2-01 SECTION.                                               IX2164.2
035500     USE    AFTER STANDARD ERROR PROCEDURE ON IX-FS2.             IX2164.2
035600 IX-FS2-01-01.                                                    IX2164.2
035700     ADD      1 TO  WRK-CS-09V00-013.                             IX2164.2
035800     GO TO    IX-FS2-01-03                                        IX2164.2
035900              IX-FS2-01-05                                        IX2164.2
036000              DEPENDING ON  WRK-CS-09V00-012.                     IX2164.2
036100     GO TO    IX-FS2-01-EXIT.                                     IX2164.2
036200 IX-FS2-01-03.                                                    IX2164.2
036300*ENTRY FROM SEGMENT INX-TEST-001.                                 IX2164.2
036400*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     IX2164.2
036500     ADD     1 TO  WRK-CS-09V00-014.                              IX2164.2
036600 IX-FS2-01-05.                                                    IX2164.2
036700     ADD      1 TO WRK-CS-09V00-017.                              IX2164.2
036800     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX2164.2
036900              MOVE   IX-FS2-STATUS TO WRK-XN-0002-002             IX2164.2
037000              MOVE  "10" TO WRK-XN-0002-003.                      IX2164.2
037100 IX-FS2-01-EXIT.                                                  IX2164.2
037200     EXIT.                                                        IX2164.2
037300 END DECLARATIVES.                                                IX2164.2
037400 CCVS1 SECTION.                                                   IX2164.2
037500 OPEN-FILES.                                                      IX2164.2
037600*    OPEN I-O RAW-DATA.                                           IX2164.2
037700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2164.2
037800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2164.2
037900*    MOVE "ABORTED " TO C-ABORT.                                  IX2164.2
038000*    ADD 1 TO C-NO-OF-TESTS.                                      IX2164.2
038100*    ACCEPT C-DATE  FROM DATE.                                    IX2164.2
038200*    ACCEPT C-TIME  FROM TIME.                                    IX2164.2
038300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2164.2
038400*END-E-1.                                                         IX2164.2
038500*    CLOSE RAW-DATA.                                              IX2164.2
038600     OPEN    OUTPUT PRINT-FILE.                                   IX2164.2
038700     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2164.2
038800     MOVE    SPACE TO TEST-RESULTS.                               IX2164.2
038900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2164.2
039000     MOVE    ZERO TO REC-SKL-SUB.                                 IX2164.2
039100     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2164.2
039200 CCVS-INIT-FILE.                                                  IX2164.2
039300     ADD     1 TO REC-SKL-SUB.                                    IX2164.2
039400     MOVE    FILE-RECORD-INFO-SKELETON                            IX2164.2
039500          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2164.2
039600 CCVS-INIT-EXIT.                                                  IX2164.2
039700     GO TO CCVS1-EXIT.                                            IX2164.2
039800 CLOSE-FILES.                                                     IX2164.2
039900*    OPEN I-O RAW-DATA.                                           IX2164.2
040000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2164.2
040100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2164.2
040200*    MOVE "OK.     " TO C-ABORT.                                  IX2164.2
040300*    MOVE PASS-COUNTER TO C-OK.                                   IX2164.2
040400*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2164.2
040500*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2164.2
040600*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2164.2
040700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2164.2
040800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2164.2
040900*END-E-2.                                                         IX2164.2
041000*    CLOSE RAW-DATA.                                              IX2164.2
041100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2164.2
041200 TERMINATE-CCVS.                                                  IX2164.2
041300*    EXIT PROGRAM.                                                IX2164.2
041400*TERMINATE-CALL.                                                  IX2164.2
041500     STOP     RUN.                                                IX2164.2
041600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2164.2
041700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2164.2
041800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2164.2
041900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2164.2
042000     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2164.2
042100 PRINT-DETAIL.                                                    IX2164.2
042200     IF REC-CT NOT EQUAL TO ZERO                                  IX2164.2
042300             MOVE "." TO PARDOT-X                                 IX2164.2
042400             MOVE REC-CT TO DOTVALUE.                             IX2164.2
042500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2164.2
042600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2164.2
042700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2164.2
042800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2164.2
042900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2164.2
043000     MOVE SPACE TO CORRECT-X.                                     IX2164.2
043100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2164.2
043200     MOVE     SPACE TO RE-MARK.                                   IX2164.2
043300 HEAD-ROUTINE.                                                    IX2164.2
043400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2164.2
043500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2164.2
043600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2164.2
043700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2164.2
043800 COLUMN-NAMES-ROUTINE.                                            IX2164.2
043900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2164.2
044000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2164.2
044100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2164.2
044200 END-ROUTINE.                                                     IX2164.2
044300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2164.2
044400 END-RTN-EXIT.                                                    IX2164.2
044500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2164.2
044600 END-ROUTINE-1.                                                   IX2164.2
044700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2164.2
044800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2164.2
044900      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2164.2
045000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2164.2
045100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2164.2
045200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2164.2
045300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2164.2
045400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2164.2
045500  END-ROUTINE-12.                                                 IX2164.2
045600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2164.2
045700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2164.2
045800         MOVE "NO " TO ERROR-TOTAL                                IX2164.2
045900         ELSE                                                     IX2164.2
046000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2164.2
046100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2164.2
046200     PERFORM WRITE-LINE.                                          IX2164.2
046300 END-ROUTINE-13.                                                  IX2164.2
046400     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2164.2
046500         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2164.2
046600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2164.2
046700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2164.2
046800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2164.2
046900      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2164.2
047000          MOVE "NO " TO ERROR-TOTAL                               IX2164.2
047100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2164.2
047200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2164.2
047300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2164.2
047400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2164.2
047500 WRITE-LINE.                                                      IX2164.2
047600     ADD 1 TO RECORD-COUNT.                                       IX2164.2
047700*    IF RECORD-COUNT GREATER 42                                   IX2164.2
047800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2164.2
047900*        MOVE SPACE TO DUMMY-RECORD                               IX2164.2
048000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2164.2
048100*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2164.2
048200*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2164.2
048300*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2164.2
048400*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2164.2
048500*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2164.2
048600*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2164.2
048700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2164.2
048800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2164.2
048900*        MOVE ZERO TO RECORD-COUNT.                               IX2164.2
049000     PERFORM WRT-LN.                                              IX2164.2
049100 WRT-LN.                                                          IX2164.2
049200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2164.2
049300     MOVE SPACE TO DUMMY-RECORD.                                  IX2164.2
049400 BLANK-LINE-PRINT.                                                IX2164.2
049500     PERFORM WRT-LN.                                              IX2164.2
049600 FAIL-ROUTINE.                                                    IX2164.2
049700     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2164.2
049800            GO TO   FAIL-ROUTINE-WRITE.                           IX2164.2
049900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2164.2
050000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2164.2
050100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2164.2
050200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2164.2
050300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2164.2
050400     GO TO  FAIL-ROUTINE-EX.                                      IX2164.2
050500 FAIL-ROUTINE-WRITE.                                              IX2164.2
050600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2164.2
050700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2164.2
050800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2164.2
050900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2164.2
051000 FAIL-ROUTINE-EX. EXIT.                                           IX2164.2
051100 BAIL-OUT.                                                        IX2164.2
051200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2164.2
051300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2164.2
051400 BAIL-OUT-WRITE.                                                  IX2164.2
051500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2164.2
051600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2164.2
051700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2164.2
051800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2164.2
051900 BAIL-OUT-EX. EXIT.                                               IX2164.2
052000 CCVS1-EXIT.                                                      IX2164.2
052100     EXIT.                                                        IX2164.2
052200 SECT-IX-04-001 SECTION.                                          IX2164.2
052300 INX-INIT-001.                                                    IX2164.2
052400     MOVE     "CREATE IX-FS2" TO  FEATURE                         IX2164.2
052500     MOVE     "IX-FS2"        TO  XFILE-NAME (2).                 IX2164.2
052600     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               IX2164.2
052700     MOVE     CCVS-PGM-ID     TO  XPROGRAM-NAME (2).              IX2164.2
052800     MOVE     000240          TO  XRECORD-LENGTH (2).             IX2164.2
052900     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           IX2164.2
053000     MOVE     0001            TO  XBLOCK-SIZE       (2).          IX2164.2
053100     MOVE     000500          TO  RECORDS-IN-FILE  (2).           IX2164.2
053200     MOVE    "IX"                TO XFILE-ORGANIZATION (2).       IX2164.2
053300     MOVE     "S"             TO  XLABEL-TYPE        (2).         IX2164.2
053400     MOVE     000001          TO  XRECORD-NUMBER     (2).         IX2164.2
053500*INITIALIZE  RECORD WORK AREA NUMBER 2.                           IX2164.2
053600     MOVE     1         TO  WRK-CS-09V00-012.                     IX2164.2
053700     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     IX2164.2
053800                            WRK-CS-09V00-015 WRK-CS-09V00-016     IX2164.2
053900                            WRK-CS-09V00-017 WRK-CS-09V00-018.    IX2164.2
054000 OPEN-INIT-GF-01.                                                 IX2164.2
054100     MOVE     SPACE     TO  IX-FS2-STATUS.                        IX2164.2
054200     MOVE    ZERO TO WRK-DU-09V00-001.                            IX2164.2
054300 OPEN-TEST-GF-01.                                                 IX2164.2
054400*   FILE IX-FS2   DOES NOT EXIST **********************           IX2164.2
054500     OPEN    EXTEND     IX-FS2.                                   IX2164.2
054600     IF IX-FS2-STATUS = "05"                                      IX2164.2
054700         GO TO OPEN-PASS-GF-01.                                   IX2164.2
054800 OPEN-FAIL-GF-01.                                                 IX2164.2
054900     PERFORM FAIL.                                                IX2164.2
055000     MOVE IX-FS2-STATUS TO COMPUTED-A.                            IX2164.2
055100     MOVE "05" TO CORRECT-A.                                      IX2164.2
055200     MOVE "IX-3; 1.3.4 (1) D; STATUS AFTER OPEN EXTEND" TO RE-MARKIX2164.2
055300     GO TO OPEN-WRITE-GF-01.                                      IX2164.2
055400 OPEN-PASS-GF-01.                                                 IX2164.2
055500     PERFORM PASS.                                                IX2164.2
055600 OPEN-WRITE-GF-01.                                                IX2164.2
055700     MOVE "OPEN-TEST-GF-01" TO PAR-NAME.                          IX2164.2
055800     MOVE "OPEN EXTEND: EXP: 05"       TO FEATURE.                IX2164.2
055900     PERFORM PRINT-DETAIL.                                        IX2164.2
056000     MOVE GRP-0101 TO IX-FS2-KEY.                                 IX2164.2
056100     MOVE     IX-FS2-STATUS TO WRK-XN-0002-001.                   IX2164.2
056200*CAPTURE STATUS KEY AFTER  OPEN STATEMENT IS EXECUTED.            IX2164.2
056300 WRITE-INIT-GF-01.                                                IX2164.2
056400     MOVE    "99"  TO IX-FS2-STATUS.                              IX2164.2
056500     MOVE     XRECORD-NUMBER (2) TO WRK-DU-09V00-001.             IX2164.2
056600     MOVE     GRP-0101 TO XRECORD-KEY (2).                        IX2164.2
056700     MOVE     FILE-RECORD-INFO (2) TO IX-FS2R1-F-G-240.           IX2164.2
056800     WRITE   IX-FS2R1-F-G-240.                                    IX2164.2
056900     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX2164.2
057000         ADD  1  TO WRK-CS-09V00-016.                             IX2164.2
057100     IF XRECORD-NUMBER (2) EQUAL TO 300                           IX2164.2
057200         PERFORM WRITE-TEST-GF-01 THRU WRITE-TEST-GF-01-END.      IX2164.2
057300     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX2164.2
057400              GO TO  WRITE-TEST-GF-02.                            IX2164.2
057500     ADD      01 TO  XRECORD-NUMBER (2).                          IX2164.2
057600     GO TO    WRITE-INIT-GF-01.                                   IX2164.2
057700 WRITE-TEST-GF-01.                                                IX2164.2
057800     CLOSE IX-FS2.                                                IX2164.2
057900     MOVE     SPACE     TO  IX-FS2-STATUS.                        IX2164.2
058000     OPEN     EXTEND    IX-FS2.                                   IX2164.2
058100 WRITE-TEST-GF-01-02.                                             IX2164.2
058200     IF IX-FS2-STATUS = "00"                                      IX2164.2
058300         GO TO WRITE-TEST-GF-01-02-PASS.                          IX2164.2
058400 WRITE-TEST-GF-01-02-FAIL.                                        IX2164.2
058500     PERFORM FAIL.                                                IX2164.2
058600     MOVE IX-FS2-STATUS TO COMPUTED-A.                            IX2164.2
058700     MOVE "00" TO CORRECT-A.                                      IX2164.2
058800     MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER OPEN EXTEND" TO RE-MARKIX2164.2
058900     GO TO WRITE-TEST-GF-01-02-WRITE.                             IX2164.2
059000 WRITE-TEST-GF-01-02-PASS.                                        IX2164.2
059100     PERFORM PASS.                                                IX2164.2
059200 WRITE-TEST-GF-01-02-WRITE.                                       IX2164.2
059300     MOVE "WRITE-TEST-GF-01   " TO PAR-NAME.                      IX2164.2
059400     MOVE "OPEN EXTEND EXISTING" TO FEATURE.                      IX2164.2
059500     PERFORM PRINT-DETAIL.                                        IX2164.2
059600 WRITE-TEST-GF-01-END.                                            IX2164.2
059700     EXIT.                                                        IX2164.2
059800                                                                  IX2164.2
059900 WRITE-TEST-GF-02.                                                IX2164.2
060000     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  IX2164.2
060100              MOVE "IX-41; EXCEPTIONS/ERRORS" TO RE-MARK          IX2164.2
060200              MOVE  ZERO TO CORRECT-18V0                          IX2164.2
060300              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX2164.2
060400              PERFORM   FAIL                                      IX2164.2
060500              ELSE                                                IX2164.2
060600              PERFORM   PASS.                                     IX2164.2
060700     MOVE "OP EXT: ERROR/EXCEPT" TO FEATURE.                      IX2164.2
060800     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         IX2164.2
060900     PERFORM  PRINT-DETAIL.                                       IX2164.2
061000 WRITE-TEST-GF-03.                                                IX2164.2
061100     MOVE "OP EXT: INCORR COUNT" TO FEATURE.                      IX2164.2
061200     MOVE "WRITE-TEST-GF-03" TO PAR-NAME.                         IX2164.2
061300     IF       XRECORD-NUMBER (2) NOT EQUAL TO 500                 IX2164.2
061400              MOVE "IX-41; INCORRECT COUNT" TO RE-MARK            IX2164.2
061500              MOVE  500 TO CORRECT-18V0                           IX2164.2
061600             MOVE   XRECORD-NUMBER (2)  TO COMPUTED-18V0          IX2164.2
061700              PERFORM  FAIL                                       IX2164.2
061800              ELSE                                                IX2164.2
061900              PERFORM PASS.                                       IX2164.2
062000     PERFORM  PRINT-DETAIL.                                       IX2164.2
062100 WRITE-TEST-GF-04.                                                IX2164.2
062200     MOVE "OP EXT STATUS EXP:00" TO FEATURE.                      IX2164.2
062300     MOVE "WRITE-TEST-GF-04" TO PAR-NAME.                         IX2164.2
062400*    IF       WRK-XN-0002-001 NOT EQUAL TO  "00"                  IX2164.2
062500*           MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER OPEN" TO RE-MARKIX2164.2
062600*             MOVE WRK-XN-0002-001 TO COMPUTED-A                  IX2164.2
062700*             MOVE "00" TO CORRECT-A                              IX2164.2
062800*             PERFORM  FAIL                                       IX2164.2
062900*             ELSE                                                IX2164.2
063000*             PERFORM  PASS.                                      IX2164.2
063100     PERFORM DE-LETE.                                             IX2164.2
063200     PERFORM  PRINT-DETAIL.                                       IX2164.2
063300 WRITE-TEST-GF-05.                                                IX2164.2
063400     MOVE "WRITE STATUS EXP: 00" TO FEATURE.                      IX2164.2
063500     MOVE "WRITE-TEST-GF-05" TO PAR-NAME.                         IX2164.2
063600     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX2164.2
063700           MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER WRITE" TO RE-MARKIX2164.2
063800              MOVE IX-FS2-STATUS TO COMPUTED-A                    IX2164.2
063900              MOVE "00" TO CORRECT-A                              IX2164.2
064000              PERFORM   FAIL                                      IX2164.2
064100              ELSE                                                IX2164.2
064200              PERFORM   PASS.                                     IX2164.2
064300     PERFORM  PRINT-DETAIL.                                       IX2164.2
064400 WRITE-TEST-GF-06.                                                IX2164.2
064500     MOVE "WRITE STATUS EXP: 00" TO FEATURE.                      IX2164.2
064600     MOVE "WRITE-TEST-GF-06" TO PAR-NAME.                         IX2164.2
064700     IF      WRK-CS-09V00-016    NOT EQUAL TO ZERO                IX2164.2
064800           MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER WRITE" TO RE-MARKIX2164.2
064900              MOVE   ZERO TO CORRECT-18V0                         IX2164.2
065000              MOVE    WRK-CS-09V00-016 TO COMPUTED-18V0           IX2164.2
065100              PERFORM   FAIL                                      IX2164.2
065200              ELSE                                                IX2164.2
065300              PERFORM  PASS.                                      IX2164.2
065400     PERFORM  PRINT-DETAIL.                                       IX2164.2
065500 WRITE-TEST-GF-07.                                                IX2164.2
065600     MOVE "CLOSE STATUS EXP: 00" TO FEATURE.                      IX2164.2
065700     MOVE "WRITE-TEST-GF-07" TO PAR-NAME.                         IX2164.2
065800     MOVE   99       TO  IX-FS2-STATUS.                           IX2164.2
065900     CLOSE    IX-FS2.                                             IX2164.2
066000     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX2164.2
066100           MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER CLOSE" TO RE-MARKIX2164.2
066200              MOVE   IX-FS2-STATUS TO COMPUTED-A                  IX2164.2
066300              MOVE "00" TO CORRECT-A                              IX2164.2
066400              PERFORM FAIL                                        IX2164.2
066500              ELSE                                                IX2164.2
066600              PERFORM PASS.                                       IX2164.2
066700     PERFORM  PRINT-DETAIL.                                       IX2164.2
066800                                                                  IX2164.2
066900 RWRT-INIT-GF-01.                                                 IX2164.2
067000     MOVE     2     TO  WRK-CS-09V00-012.                         IX2164.2
067100     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX2164.2
067200     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX2164.2
067300     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX2164.2
067400     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX2164.2
067500     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX2164.2
067600     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX2164.2
067700     MOVE    SPACE TO IX-FS2-STATUS.                              IX2164.2
067800     MOVE "RWRT-TEST-GF-01" TO PAR-NAME.                          IX2164.2
067900     OPEN     I-O  IX-FS2.                                        IX2164.2
068000     MOVE     SPACE     TO  WRK-XN-0002-002                       IX2164.2
068100     MOVE     SPACE     TO  WRK-XN-0002-003                       IX2164.2
068200     MOVE     SPACE     TO  WRK-XN-0002-004                       IX2164.2
068300     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX2164.2
068400     MOVE     SPACE TO IX-FS2-STATUS.                             IX2164.2
068500     MOVE    "UPDATE  IX-FS2"  TO FEATURE.                        IX2164.2
068600*CAPTURE THE CONTENTS OF STATUS KEY AFTER OPEN IS EXECUTED.       IX2164.2
068700 RWRT-TEST-GF-01.                                                 IX2164.2
068800     ADD      1 TO WRK-CS-09V00-014.                              IX2164.2
068900     ADD      1 TO WRK-CS-09V00-015.                              IX2164.2
069000     READ  IX-FS2.                                                IX2164.2
069100     IF        IX-FS2-STATUS EQUAL TO "10"                        IX2164.2
069200              GO TO  RWRT-TEST-GF-01-1.                           IX2164.2
069300     MOVE     IX-FS2R1-F-G-240 TO FILE-RECORD-INFO (2).           IX2164.2
069400     IF       WRK-CS-09V00-015 EQUAL TO 5                         IX2164.2
069500              ADD 01 TO UPDATE-NUMBER (2)                         IX2164.2
069600     MOVE     FILE-RECORD-INFO (2) TO IX-FS2R1-F-G-240            IX2164.2
069700             PERFORM  RWRT-010-UPDATE                             IX2164.2
069800           MOVE ZERO TO WRK-CS-09V00-015                          IX2164.2
069900              GO TO  RWRT-TEST-GF-01-2.                           IX2164.2
070000     IF       WRK-CS-09V00-014 GREATER 500                        IX2164.2
070100              GO TO  RWRT-TEST-GF-01-1.                           IX2164.2
070200     GO TO    RWRT-TEST-GF-01.                                    IX2164.2
070300 RWRT-010-UPDATE.                                                 IX2164.2
070400     REWRITE   IX-FS2R1-F-G-240.                                  IX2164.2
070500 RWRT-TEST-GF-01-2.                                               IX2164.2
070600     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX2164.2
070700              ADD 1 TO WRK-CS-09V00-016.                          IX2164.2
070800     MOVE    SPACE  TO   IX-FS2-STATUS.                           IX2164.2
070900     GO TO    RWRT-TEST-GF-01.                                    IX2164.2
071000 RWRT-TEST-GF-01-1.                                               IX2164.2
071100     MOVE "REWRITE ERR/EXCEPTIO" TO FEATURE.                      IX2164.2
071200     MOVE "RWRT-TEST-GF-01" TO PAR-NAME.                          IX2164.2
071300     IF       WRK-CS-09V00-013 NOT EQUAL TO 1                     IX2164.2
071400              MOVE "IX-33; EXCEPTIONS/ERRORS" TO RE-MARK          IX2164.2
071500              MOVE WRK-CS-09V00-013 TO COMPUTED-18V0              IX2164.2
071600              MOVE  1 TO CORRECT-18V0                             IX2164.2
071700              PERFORM FAIL                                        IX2164.2
071800              ELSE                                                IX2164.2
071900              PERFORM PASS.                                       IX2164.2
072000     PERFORM  PRINT-DETAIL.                                       IX2164.2
072100 RWRT-TEST-GF-02.                                                 IX2164.2
072200     MOVE "REWRITE ERR/EXCEPTIO" TO FEATURE.                      IX2164.2
072300     MOVE "RWRT-TEST-GF-02" TO PAR-NAME.                          IX2164.2
072400     IF       WRK-CS-09V00-014 NOT EQUAL TO 501                   IX2164.2
072500              MOVE "IX-33;INCORRECT COUNT" TO RE-MARK             IX2164.2
072600              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX2164.2
072700              MOVE 501 TO CORRECT-18V0                            IX2164.2
072800              PERFORM FAIL                                        IX2164.2
072900              ELSE                                                IX2164.2
073000              PERFORM PASS.                                       IX2164.2
073100     PERFORM  PRINT-DETAIL.                                       IX2164.2
073200 RWRT-TEST-GF-03.                                                 IX2164.2
073300     MOVE "OPEN STATUS EXP:  00" TO FEATURE.                      IX2164.2
073400     MOVE "RWRT-TEST-GF-03" TO PAR-NAME.                          IX2164.2
073500     IF       WRK-XN-0002-001 NOT EQUAL TO "00"                   IX2164.2
073600            MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER OPEN" TO RE-MARKIX2164.2
073700              MOVE WRK-XN-0002-001 TO COMPUTED-A                  IX2164.2
073800              MOVE  "00" TO CORRECT-A                             IX2164.2
073900              PERFORM FAIL                                        IX2164.2
074000              ELSE                                                IX2164.2
074100              PERFORM PASS.                                       IX2164.2
074200     PERFORM  PRINT-DETAIL.                                       IX2164.2
074300 RWRT-TEST-GF-04.                                                 IX2164.2
074400     MOVE "AT END STATUS EXP:10" TO FEATURE.                      IX2164.2
074500     MOVE "RWRT-TEST-GF-04" TO PAR-NAME.                          IX2164.2
074600     IF        IX-FS2-STATUS NOT EQUAL  TO "10"                   IX2164.2
074700            MOVE "IX-4; 1.3.4 (2) A; STATUS AT END    " TO RE-MARKIX2164.2
074800              MOVE  IX-FS2-STATUS TO COMPUTED-A                   IX2164.2
074900              MOVE "10" TO CORRECT-A                              IX2164.2
075000              PERFORM FAIL                                        IX2164.2
075100              ELSE                                                IX2164.2
075200              PERFORM PASS.                                       IX2164.2
075300     PERFORM  PRINT-DETAIL.                                       IX2164.2
075400 RWRT-TEST-GF-05.                                                 IX2164.2
075500     MOVE "REWRITE ERR/EXCEPTIO" TO FEATURE.                      IX2164.2
075600     MOVE "RWRT-TEST-GF-05" TO PAR-NAME.                          IX2164.2
075700     IF       WRK-XN-0002-002 NOT EQUAL TO "10"                   IX2164.2
075800            MOVE "IX-4; 1.3.4 (2) A; STATUS AFTER  END" TO RE-MARKIX2164.2
075900              MOVE  "EXCEPTIN/STATUS" TO RE-MARK                  IX2164.2
076000              MOVE  WRK-XN-0002-002 TO COMPUTED-A                 IX2164.2
076100              MOVE  "10" TO CORRECT-A                             IX2164.2
076200              PERFORM FAIL                                        IX2164.2
076300              ELSE                                                IX2164.2
076400              PERFORM PASS.                                       IX2164.2
076500     PERFORM  PRINT-DETAIL.                                       IX2164.2
076600 RWRT-TEST-GF-06.                                                 IX2164.2
076700     MOVE "REWRITE ERR/EXCEPTIO" TO FEATURE.                      IX2164.2
076800     MOVE "RWRT-TEST-GF-06" TO PAR-NAME.                          IX2164.2
076900     IF       WRK-XN-0002-003 NOT EQUAL TO "10"                   IX2164.2
077000            MOVE "IX-4; 1.3.4 (2) A; NO/ EXCEPTION    " TO RE-MARKIX2164.2
077100              MOVE WRK-XN-0002-003 TO COMPUTED-A                  IX2164.2
077200              MOVE "10" TO CORRECT-A                              IX2164.2
077300              PERFORM FAIL                                        IX2164.2
077400              ELSE                                                IX2164.2
077500              PERFORM PASS.                                       IX2164.2
077600     PERFORM  PRINT-DETAIL.                                       IX2164.2
077700 RWRT-TEST-GF-07.                                                 IX2164.2
077800     MOVE "CLOSE LOCK STAT:  00" TO FEATURE.                      IX2164.2
077900     MOVE "RWRT-TEST-GF-07" TO PAR-NAME.                          IX2164.2
078000     MOVE     SPACE TO IX-FS2-STATUS.                             IX2164.2
078100     CLOSE    IX-FS2      LOCK.                                   IX2164.2
078200     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX2164.2
078300           MOVE "IX-3; 1.3.4 (1) A; STATUS AFTER CLOSE" TO RE-MARKIX2164.2
078400              MOVE  IX-FS2-STATUS TO  COMPUTED-A                  IX2164.2
078500              MOVE "00" TO CORRECT-A                              IX2164.2
078600              PERFORM FAIL                                        IX2164.2
078700              ELSE                                                IX2164.2
078800              PERFORM PASS.                                       IX2164.2
078900     PERFORM  PRINT-DETAIL.                                       IX2164.2
079000 CCVS-EXIT SECTION.                                               IX2164.2
079100 CCVS-999999.                                                     IX2164.2
079200     GO TO CLOSE-FILES.                                           IX2164.2
      *END-OF,IX216A                                                                  
