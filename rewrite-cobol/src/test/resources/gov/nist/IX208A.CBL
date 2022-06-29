      *HEADER,COBOL,IX208A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2084.2
000200 PROGRAM-ID.                                                      IX2084.2
000300     IX208A.                                                      IX2084.2
000400****************************************************************  IX2084.2
000500*                                                              *  IX2084.2
000600*    VALIDATION FOR:-                                          *  IX2084.2
000700*                                                              *  IX2084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2084.2
000900*                                                              *  IX2084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2084.2
001100*                                                              *  IX2084.2
001200****************************************************************  IX2084.2
001300*    THE FUNCTION OF THIS PROGRAM IS TO TEST THE PERMISSIBLE SYN- IX2084.2
001400*    TACTICAL CONSTRUCTS OF COBOL ELEMENTS ASSOCIATED WITH LEVEL 2IX2084.2
001500*    OF THE INDEXED I-O MODULE.  THE ELEMENTS TESTED IN THIS      IX2084.2
001600*    ROUTINE ARE:                                                 IX2084.2
001700*                                                                 IX2084.2
001800*    (1) READ STATEMENT;                                          IX2084.2
001900*    (2) START STATEMENT;                                         IX2084.2
002000*    (3) USE STATEMENT.                                           IX2084.2
002100*                                                                 IX2084.2
002200*    EACH ELEMENT TESTED WILL BE EXERCISED SEMANTICALLY BY THIS   IX2084.2
002300*    ROUTINE.                                                     IX2084.2
002400*                                                                 IX2084.2
002500*                                                                 IX2084.2
002600*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2084.2
002700*                                                                 IX2084.2
002800*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2084.2
002900*                    CLAUSE FOR DATA FILE IX-FS1                  IX2084.2
003000*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2084.2
003100*                    CLAUSE FOR DATA FILE IX-FD2                  IX2084.2
003200*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2084.2
003300*                    CLAUSE FOR INDEX FILE IX-FS1                 IX2084.2
003400*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX2084.2
003500*                    CLAUSE FOR INDEX FILE IX-FD2                 IX2084.2
003600*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2084.2
003700*             X-69   ADDITIONAL VALUE OF PHRASES                  IX2084.2
003800*             X-74   VALUE OF IMPLEMENTOR-NAME                    IX2084.2
003900*             X-75   OBJECT OF VALUE OF CLAUSE FOR FILE IX-FS1    IX2084.2
004000*             X-76   OBJECT OF VALUE OF CLAUSE FOR FILE IX-FD2    IX2084.2
004100*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2084.2
004200*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2084.2
004300*                                                                 IX2084.2
004400*        NOTE:  X-CARDS 44,45,69,74,75 AND 76 ARE OPTIONAL        IX2084.2
004500*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2084.2
004600*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2084.2
004700*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2084.2
004800*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2084.2
004900*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2084.2
005000*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2084.2
005100*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2084.2
005200*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2084.2
005300*               THEY ARE AS FOLLOWS                               IX2084.2
005400*                                                                 IX2084.2
005500*                  C  SELECTS X-CARDS 74,75 AND 76                IX2084.2
005600*                  G  SELECTS X-CARDS 69                          IX2084.2
005700*                  J  SELECTS X-CARDS 44 AND 45                   IX2084.2
005800*                                                                 IX2084.2
005900*        NOTE:  THERE IS OPTIONAL SOURCE CODE IN THIS PROGRAM     IX2084.2
006000*               FOR THE CONVENIENCE OF THE USER.  THIS OPTIONAL   IX2084.2
006100*               CODE IS IDENTIFIED BY THE LETTER T,U OR X IN      IX2084.2
006200*               POSITION 7  OF THE SOURCE LINE.  USE OF           IX2084.2
006300*               SOURCE CODE WITH LETTER X WILL PRINT THE CONTENTS IX2084.2
006400*               OF THE FILES AFTER THE TEST REPORT.  FOR CODE     IX2084.2
006500*               WITH LETTERS T OR U ONLY ONE SHOULD BE SELECTED.  IX2084.2
006600*               EITHER THE T"S OR THE U"S SHOULD BE USED EXCLU-   IX2084.2
006700*               SIVELY, NOT BOTH.  THE T"S PROVIDE A 29 CHARACTER IX2084.2
006800*               INDEXED KEY SIZE FOR THE FILE AND THE U"S PROVIDE IX2084.2
006900*               AN INDEXED KEY NO GREATER THAN 8 CHARACTERS.      IX2084.2
007000*               IF THE VP-ROUTINE IS USED THE APPROPRIATE         IX2084.2
007100*               SOURCE CODE MAY BE SELECTED BY SPECIFYING THE     IX2084.2
007200*               RESPECTIVE LETTER IN THE "*OPT" VP-ROUTINE CONTROLIX2084.2
007300*               CARD.                                             IX2084.2
007400*                                                                 IX2084.2
007500******************************************************            IX2084.2
007600 ENVIRONMENT DIVISION.                                            IX2084.2
007700 CONFIGURATION SECTION.                                           IX2084.2
007800 SOURCE-COMPUTER.                                                 IX2084.2
007900     XXXXX082.                                                    IX2084.2
008000 OBJECT-COMPUTER.                                                 IX2084.2
008100     XXXXX083.                                                    IX2084.2
008200 INPUT-OUTPUT SECTION.                                            IX2084.2
008300 FILE-CONTROL.                                                    IX2084.2
008400*    SELECT RAW-DATA   ASSIGN TO                                  IX2084.2
008500*    XXXXX062                                                     IX2084.2
008600*           ORGANIZATION IS INDEXED                               IX2084.2
008700*           ACCESS MODE IS RANDOM                                 IX2084.2
008800*           RECORD KEY IS RAW-DATA-KEY.                           IX2084.2
008900     SELECT PRINT-FILE ASSIGN TO                                  IX2084.2
009000     XXXXX055.                                                    IX2084.2
009100     SELECT   IX-FD1                                              IX2084.2
009200        ASSIGN TO                                                 IX2084.2
009300     XXXXX024                                                     IX2084.2
009400*    XXXXX044                                                     IX2084.2
009500         ORGANIZATION IS INDEXED                                  IX2084.2
009600        ALTERNATE RECORD KEY IS IX-FD1-ALTKEY1                    IX2084.2
009700        RECORD KEY IS   IX-FD1-KEY                                IX2084.2
009800        ACCESS MODE IS DYNAMIC.                                   IX2084.2
009900     SELECT   IX-FS2                                              IX2084.2
010000         ASSIGN TO                                                IX2084.2
010100     XXXXX025                                                     IX2084.2
010200*    XXXXX045                                                     IX2084.2
010300     RECORD KEY IS IX-FS2-KEY                                     IX2084.2
010400        ALTERNATE RECORD KEY IS IX-FS2-ALTKEY1                    IX2084.2
010500        ACCESS MODE IS SEQUENTIAL                                 IX2084.2
010600         ORGANIZATION IS INDEXED.                                 IX2084.2
010700 DATA DIVISION.                                                   IX2084.2
010800 FILE SECTION.                                                    IX2084.2
010900*                                                                 IX2084.2
011000*FD  RAW-DATA.                                                    IX2084.2
011100*                                                                 IX2084.2
011200*01  RAW-DATA-SATZ.                                               IX2084.2
011300*    05  RAW-DATA-KEY        PIC X(6).                            IX2084.2
011400*    05  C-DATE              PIC 9(6).                            IX2084.2
011500*    05  C-TIME              PIC 9(8).                            IX2084.2
011600*    05  C-NO-OF-TESTS       PIC 99.                              IX2084.2
011700*    05  C-OK                PIC 999.                             IX2084.2
011800*    05  C-ALL               PIC 999.                             IX2084.2
011900*    05  C-FAIL              PIC 999.                             IX2084.2
012000*    05  C-DELETED           PIC 999.                             IX2084.2
012100*    05  C-INSPECT           PIC 999.                             IX2084.2
012200*    05  C-NOTE              PIC X(13).                           IX2084.2
012300*    05  C-INDENT            PIC X.                               IX2084.2
012400*    05  C-ABORT             PIC X(8).                            IX2084.2
012500 FD  PRINT-FILE.                                                  IX2084.2
012600 01  PRINT-REC PICTURE X(120).                                    IX2084.2
012700 01  DUMMY-RECORD PICTURE X(120).                                 IX2084.2
012800 FD  IX-FD1                                                       IX2084.2
012900C    LABEL RECORD IS STANDARD                                     IX2084.2
013000C    DATA RECORD IS IX-FD1R1-F-G-240                              IX2084.2
013100     RECORD CONTAINS 240 CHARACTERS.                              IX2084.2
013200 01  IX-FD1R1-F-G-240.                                            IX2084.2
013300     05 IX-FD1-REC-001-120        PICTURE X(120).                 IX2084.2
013400     05 IX-FD1-REC-121-240.                                       IX2084.2
013500     10 FILLER          PICTURE X(8).                             IX2084.2
013600     10 IX-FD1-KEY.                                               IX2084.2
013700        15 IX-FS1-KEYNUM PICTURE 9(5).                            IX2084.2
013800*       15 FILLER        PICTURE 9(5).                            IX2084.2
013900*    10 FILLER           PICTURE X(5).                            IX2084.2
014000     10 FILLER           PICTURE X(19).                           IX2084.2
014100     10 FILLER           PICTURE X(9).                            IX2084.2
014200     10 IX-FD1-ALTKEY1.                                           IX2084.2
014300*       15 FILLER        PICTURE 9(5).                            IX2084.2
014400        15 IX-FD1-ALTKEY1NUM      PICTURE 9(5).                   IX2084.2
014500*    10 FILLER                    PICTURE 9(5).                   IX2084.2
014600     10 FILLER                    PICTURE X(19).                  IX2084.2
014700     10 FILLER                    PICTURE X(45).                  IX2084.2
014800 FD  IX-FS2                                                       IX2084.2
014900C    LABEL RECORDS ARE STANDARD                                   IX2084.2
015000C    DATA RECORD IS IX-FS2R1-F-G-240                              IX2084.2
015100          .                                                       IX2084.2
015200 01  IX-FS2R1-F-G-240.                                            IX2084.2
015300     05 IX-FS2-REC-001-120        PICTURE X(120).                 IX2084.2
015400     05 IX-FS2-REC-121-240.                                       IX2084.2
015500        10 FILLER                 PICTURE X(8).                   IX2084.2
015600        10 IX-FS2-KEY.                                            IX2084.2
015700           15 IX-FS2-KEYNUM       PICTURE 9(5).                   IX2084.2
015800*          15 FILLER              PICTURE 9(5).                   IX2084.2
015900*       10 FILLER                 PICTURE 9(5).                   IX2084.2
016000        10 FILLER                 PICTURE X(19).                  IX2084.2
016100        10 FILLER                 PICTURE X(9).                   IX2084.2
016200        10 IX-FS2-ALTKEY1.                                        IX2084.2
016300*          15 FILLER              PICTURE 9(5).                   IX2084.2
016400           15 IX-FS2-ALTKEY1NUM   PICTURE 9(5).                   IX2084.2
016500*       10 FILLER                 PICTURE 9(5).                   IX2084.2
016600        10 FILLER                 PICTURE X(19).                  IX2084.2
016700        10 FILLER                 PICTURE X(45).                  IX2084.2
016800 WORKING-STORAGE SECTION.                                         IX2084.2
016900 01  IX-FD1-FILESIZE              PICTURE 9(6) VALUE 300.         IX2084.2
017000 01  IX-FS2-FILESIZE              PICTURE 9(6) VALUE 300.         IX2084.2
017100 01  WRK-IX-FD1-RECKEY.                                           IX2084.2
017200     03 WRK-DU-05V00-001          PICTURE 9(5) VALUE ZERO.        IX2084.2
017300*    03 FILLER                    PICTURE 9(5) VALUE ZERO.        IX2084.2
017400 01  WRK-IX-FS2-RECKEY.                                           IX2084.2
017500     03 WRK-DU-05V00-003          PICTURE 9(5)  VALUE  ZERO.      IX2084.2
017600*    03 FILLER                    PICTURE 9(5)  VALUE  ZERO.      IX2084.2
017700 01  WRK-IX-FD1-ALTKEY.                                           IX2084.2
017800*    03 FILLER                    PICTURE 9(5)  VALUE  ZERO.      IX2084.2
017900     03 WRK-DU-05V00-002          PICTURE 9(5)  VALUE  ZERO.      IX2084.2
018000 01  WRK-IX-FS2-ALTKEY.                                           IX2084.2
018100*    03 FILLER                    PICTURE 9(5)  VALUE  ZERO.      IX2084.2
018200     03 WRK-DU-05V00-004          PICTURE 9(5)  VALUE  ZERO.      IX2084.2
018300 01  EXCUT-COUNTER-06V00          PICTURE S9(6) VALUE  ZERO.      IX2084.2
018400 01  INV-KEY-COUNTER              PICTURE S9(6) VALUE  ZERO.      IX2084.2
018500 01  LOGICAL-FILE-REC             PICTURE S9(6) VALUE  ZERO.      IX2084.2
018600 01  ERROR-COUNTER-06V00          PICTURE S9(6) VALUE  ZERO.      IX2084.2
018700 01  ASCEND-DESEND-SWITCH        PICTURE XX   VALUE "UP".         IX2084.2
018800             88 ASCEND VALUE "UP".                                IX2084.2
018900             88 DSCEND VALUE "DN".                                IX2084.2
019000 01  FILE-RECORD-INFORMATION-REC.                                 IX2084.2
019100     03 FILE-RECORD-INFO-SKELETON.                                IX2084.2
019200        05 FILLER                 PICTURE X(48)       VALUE       IX2084.2
019300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2084.2
019400        05 FILLER                 PICTURE X(46)       VALUE       IX2084.2
019500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2084.2
019600        05 FILLER                 PICTURE X(26)       VALUE       IX2084.2
019700             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2084.2
019800        05 FILLER                 PICTURE X(37)       VALUE       IX2084.2
019900             ",RECKEY=                             ".             IX2084.2
020000        05 FILLER                 PICTURE X(38)       VALUE       IX2084.2
020100             ",ALTKEY1=                             ".            IX2084.2
020200        05 FILLER                 PICTURE X(38)       VALUE       IX2084.2
020300             ",ALTKEY2=                             ".            IX2084.2
020400        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2084.2
020500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2084.2
020600        05 FILE-RECORD-INFO-P1-120.                               IX2084.2
020700           07 FILLER              PIC X(5).                       IX2084.2
020800           07 XFILE-NAME           PIC X(6).                      IX2084.2
020900           07 FILLER              PIC X(8).                       IX2084.2
021000           07 XRECORD-NAME         PIC X(6).                      IX2084.2
021100           07 FILLER              PIC X(1).                       IX2084.2
021200           07 REELUNIT-NUMBER     PIC 9(1).                       IX2084.2
021300           07 FILLER              PIC X(7).                       IX2084.2
021400           07 XRECORD-NUMBER       PIC 9(6).                      IX2084.2
021500           07 FILLER              PIC X(6).                       IX2084.2
021600           07 UPDATE-NUMBER       PIC 9(2).                       IX2084.2
021700           07 FILLER              PIC X(5).                       IX2084.2
021800           07 ODO-NUMBER          PIC 9(4).                       IX2084.2
021900           07 FILLER              PIC X(5).                       IX2084.2
022000           07 XPROGRAM-NAME        PIC X(5).                      IX2084.2
022100           07 FILLER              PIC X(7).                       IX2084.2
022200           07 XRECORD-LENGTH       PIC 9(6).                      IX2084.2
022300           07 FILLER              PIC X(7).                       IX2084.2
022400           07 CHARS-OR-RECORDS    PIC X(2).                       IX2084.2
022500           07 FILLER              PIC X(1).                       IX2084.2
022600           07 XBLOCK-SIZE          PIC 9(4).                      IX2084.2
022700           07 FILLER              PIC X(6).                       IX2084.2
022800           07 RECORDS-IN-FILE     PIC 9(6).                       IX2084.2
022900           07 FILLER              PIC X(5).                       IX2084.2
023000           07 XFILE-ORGANIZATION   PIC X(2).                      IX2084.2
023100           07 FILLER              PIC X(6).                       IX2084.2
023200           07 XLABEL-TYPE          PIC X(1).                      IX2084.2
023300        05 FILE-RECORD-INFO-P121-240.                             IX2084.2
023400           07 FILLER              PIC X(8).                       IX2084.2
023500           07 XRECORD-KEY          PIC X(29).                     IX2084.2
023600           07 FILLER              PIC X(9).                       IX2084.2
023700           07 ALTERNATE-KEY1      PIC X(29).                      IX2084.2
023800           07 FILLER              PIC X(9).                       IX2084.2
023900           07 ALTERNATE-KEY2      PIC X(29).                      IX2084.2
024000           07 FILLER              PIC X(7).                       IX2084.2
024100 01  TEST-RESULTS.                                                IX2084.2
024200     02 FILLER                   PIC X      VALUE SPACE.          IX2084.2
024300     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2084.2
024400     02 FILLER                   PIC X      VALUE SPACE.          IX2084.2
024500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2084.2
024600     02 FILLER                   PIC X      VALUE SPACE.          IX2084.2
024700     02  PAR-NAME.                                                IX2084.2
024800       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2084.2
024900       03  PARDOT-X              PIC X      VALUE SPACE.          IX2084.2
025000       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2084.2
025100     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2084.2
025200     02 RE-MARK                  PIC X(61).                       IX2084.2
025300 01  TEST-COMPUTED.                                               IX2084.2
025400     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2084.2
025500     02 FILLER                   PIC X(17)  VALUE                 IX2084.2
025600            "       COMPUTED=".                                   IX2084.2
025700     02 COMPUTED-X.                                               IX2084.2
025800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2084.2
025900     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2084.2
026000                                 PIC -9(9).9(9).                  IX2084.2
026100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2084.2
026200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2084.2
026300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2084.2
026400     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2084.2
026500         04 COMPUTED-18V0                    PIC -9(18).          IX2084.2
026600         04 FILLER                           PIC X.               IX2084.2
026700     03 FILLER PIC X(50) VALUE SPACE.                             IX2084.2
026800 01  TEST-CORRECT.                                                IX2084.2
026900     02 FILLER PIC X(30) VALUE SPACE.                             IX2084.2
027000     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2084.2
027100     02 CORRECT-X.                                                IX2084.2
027200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2084.2
027300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2084.2
027400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2084.2
027500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2084.2
027600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2084.2
027700     03      CR-18V0 REDEFINES CORRECT-A.                         IX2084.2
027800         04 CORRECT-18V0                     PIC -9(18).          IX2084.2
027900         04 FILLER                           PIC X.               IX2084.2
028000     03 FILLER PIC X(2) VALUE SPACE.                              IX2084.2
028100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2084.2
028200 01  CCVS-C-1.                                                    IX2084.2
028300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2084.2
028400-    "SS  PARAGRAPH-NAME                                          IX2084.2
028500-    "       REMARKS".                                            IX2084.2
028600     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2084.2
028700 01  CCVS-C-2.                                                    IX2084.2
028800     02 FILLER                     PIC X        VALUE SPACE.      IX2084.2
028900     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2084.2
029000     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2084.2
029100     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2084.2
029200     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2084.2
029300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2084.2
029400 01  REC-CT                        PIC 99       VALUE ZERO.       IX2084.2
029500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2084.2
029600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2084.2
029700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2084.2
029800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2084.2
029900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2084.2
030000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2084.2
030100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2084.2
030200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2084.2
030300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2084.2
030400 01  CCVS-H-1.                                                    IX2084.2
030500     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2084.2
030600     02  FILLER                    PIC X(42)    VALUE             IX2084.2
030700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2084.2
030800     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2084.2
030900 01  CCVS-H-2A.                                                   IX2084.2
031000   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2084.2
031100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2084.2
031200   02  FILLER                        PIC XXXX   VALUE             IX2084.2
031300     "4.2 ".                                                      IX2084.2
031400   02  FILLER                        PIC X(28)  VALUE             IX2084.2
031500            " COPY - NOT FOR DISTRIBUTION".                       IX2084.2
031600   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2084.2
031700                                                                  IX2084.2
031800 01  CCVS-H-2B.                                                   IX2084.2
031900   02  FILLER                        PIC X(15)  VALUE             IX2084.2
032000            "TEST RESULT OF ".                                    IX2084.2
032100   02  TEST-ID                       PIC X(9).                    IX2084.2
032200   02  FILLER                        PIC X(4)   VALUE             IX2084.2
032300            " IN ".                                               IX2084.2
032400   02  FILLER                        PIC X(12)  VALUE             IX2084.2
032500     " HIGH       ".                                              IX2084.2
032600   02  FILLER                        PIC X(22)  VALUE             IX2084.2
032700            " LEVEL VALIDATION FOR ".                             IX2084.2
032800   02  FILLER                        PIC X(58)  VALUE             IX2084.2
032900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2084.2
033000 01  CCVS-H-3.                                                    IX2084.2
033100     02  FILLER                      PIC X(34)  VALUE             IX2084.2
033200            " FOR OFFICIAL USE ONLY    ".                         IX2084.2
033300     02  FILLER                      PIC X(58)  VALUE             IX2084.2
033400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2084.2
033500     02  FILLER                      PIC X(28)  VALUE             IX2084.2
033600            "  COPYRIGHT   1985 ".                                IX2084.2
033700 01  CCVS-E-1.                                                    IX2084.2
033800     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2084.2
033900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2084.2
034000     02 ID-AGAIN                     PIC X(9).                    IX2084.2
034100     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2084.2
034200 01  CCVS-E-2.                                                    IX2084.2
034300     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2084.2
034400     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2084.2
034500     02 CCVS-E-2-2.                                               IX2084.2
034600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2084.2
034700         03 FILLER                   PIC X      VALUE SPACE.      IX2084.2
034800         03 ENDER-DESC               PIC X(44)  VALUE             IX2084.2
034900            "ERRORS ENCOUNTERED".                                 IX2084.2
035000 01  CCVS-E-3.                                                    IX2084.2
035100     02  FILLER                      PIC X(22)  VALUE             IX2084.2
035200            " FOR OFFICIAL USE ONLY".                             IX2084.2
035300     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2084.2
035400     02  FILLER                      PIC X(58)  VALUE             IX2084.2
035500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2084.2
035600     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2084.2
035700     02 FILLER                       PIC X(15)  VALUE             IX2084.2
035800             " COPYRIGHT 1985".                                   IX2084.2
035900 01  CCVS-E-4.                                                    IX2084.2
036000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2084.2
036100     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2084.2
036200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2084.2
036300     02 FILLER                       PIC X(40)  VALUE             IX2084.2
036400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2084.2
036500 01  XXINFO.                                                      IX2084.2
036600     02 FILLER                       PIC X(19)  VALUE             IX2084.2
036700            "*** INFORMATION ***".                                IX2084.2
036800     02 INFO-TEXT.                                                IX2084.2
036900       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2084.2
037000       04 XXCOMPUTED                 PIC X(20).                   IX2084.2
037100       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2084.2
037200       04 XXCORRECT                  PIC X(20).                   IX2084.2
037300     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2084.2
037400 01  HYPHEN-LINE.                                                 IX2084.2
037500     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2084.2
037600     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2084.2
037700-    "*****************************************".                 IX2084.2
037800     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2084.2
037900-    "******************************".                            IX2084.2
038000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2084.2
038100     "IX208A".                                                    IX2084.2
038200 PROCEDURE DIVISION.                                              IX2084.2
038300 DECLARATIVES.                                                    IX2084.2
038400 USE-IX208A-TEST SECTION.                                         IX2084.2
038500     USE      AFTER ERROR PROCEDURE IX-FD1  IX-FS2.               IX2084.2
038600 USE-PAR-001.                                                     IX2084.2
038700     ADD      010000  TO ERROR-COUNTER-06V00.                     IX2084.2
038800 USE-PAR-EXIT.                                                    IX2084.2
038900     EXIT.                                                        IX2084.2
039000 END DECLARATIVES.                                                IX2084.2
039100 CCVS1 SECTION.                                                   IX2084.2
039200 OPEN-FILES.                                                      IX2084.2
039300*    OPEN I-O RAW-DATA.                                           IX2084.2
039400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2084.2
039500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2084.2
039600*    MOVE "ABORTED " TO C-ABORT.                                  IX2084.2
039700*    ADD 1 TO C-NO-OF-TESTS.                                      IX2084.2
039800*    ACCEPT C-DATE  FROM DATE.                                    IX2084.2
039900*    ACCEPT C-TIME  FROM TIME.                                    IX2084.2
040000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2084.2
040100*END-E-1.                                                         IX2084.2
040200*    CLOSE RAW-DATA.                                              IX2084.2
040300     OPEN    OUTPUT PRINT-FILE.                                   IX2084.2
040400     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2084.2
040500     MOVE    SPACE TO TEST-RESULTS.                               IX2084.2
040600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2084.2
040700     MOVE    ZERO TO REC-SKL-SUB.                                 IX2084.2
040800     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2084.2
040900 CCVS-INIT-FILE.                                                  IX2084.2
041000     ADD     1 TO REC-SKL-SUB.                                    IX2084.2
041100     MOVE    FILE-RECORD-INFO-SKELETON                            IX2084.2
041200          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2084.2
041300 CCVS-INIT-EXIT.                                                  IX2084.2
041400     GO TO CCVS1-EXIT.                                            IX2084.2
041500 CLOSE-FILES.                                                     IX2084.2
041600*    OPEN I-O RAW-DATA.                                           IX2084.2
041700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2084.2
041800*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2084.2
041900*    MOVE "OK.     " TO C-ABORT.                                  IX2084.2
042000*    MOVE PASS-COUNTER TO C-OK.                                   IX2084.2
042100*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2084.2
042200*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2084.2
042300*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2084.2
042400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2084.2
042500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2084.2
042600*END-E-2.                                                         IX2084.2
042700*    CLOSE RAW-DATA.                                              IX2084.2
042800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2084.2
042900 TERMINATE-CCVS.                                                  IX2084.2
043000*    EXIT PROGRAM.                                                IX2084.2
043100*TERMINATE-CALL.                                                  IX2084.2
043200     STOP     RUN.                                                IX2084.2
043300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2084.2
043400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2084.2
043500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2084.2
043600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2084.2
043700     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2084.2
043800 PRINT-DETAIL.                                                    IX2084.2
043900     IF REC-CT NOT EQUAL TO ZERO                                  IX2084.2
044000             MOVE "." TO PARDOT-X                                 IX2084.2
044100             MOVE REC-CT TO DOTVALUE.                             IX2084.2
044200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2084.2
044300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2084.2
044400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2084.2
044500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2084.2
044600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2084.2
044700     MOVE SPACE TO CORRECT-X.                                     IX2084.2
044800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2084.2
044900     MOVE     SPACE TO RE-MARK.                                   IX2084.2
045000 HEAD-ROUTINE.                                                    IX2084.2
045100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2084.2
045200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2084.2
045300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2084.2
045400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2084.2
045500 COLUMN-NAMES-ROUTINE.                                            IX2084.2
045600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2084.2
045700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2084.2
045800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2084.2
045900 END-ROUTINE.                                                     IX2084.2
046000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2084.2
046100 END-RTN-EXIT.                                                    IX2084.2
046200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2084.2
046300 END-ROUTINE-1.                                                   IX2084.2
046400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2084.2
046500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2084.2
046600      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2084.2
046700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2084.2
046800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2084.2
046900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2084.2
047000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2084.2
047100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2084.2
047200  END-ROUTINE-12.                                                 IX2084.2
047300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2084.2
047400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2084.2
047500         MOVE "NO " TO ERROR-TOTAL                                IX2084.2
047600         ELSE                                                     IX2084.2
047700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2084.2
047800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2084.2
047900     PERFORM WRITE-LINE.                                          IX2084.2
048000 END-ROUTINE-13.                                                  IX2084.2
048100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2084.2
048200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2084.2
048300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2084.2
048400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2084.2
048500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2084.2
048600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2084.2
048700          MOVE "NO " TO ERROR-TOTAL                               IX2084.2
048800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2084.2
048900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2084.2
049000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2084.2
049100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2084.2
049200 WRITE-LINE.                                                      IX2084.2
049300     ADD 1 TO RECORD-COUNT.                                       IX2084.2
049400*    IF RECORD-COUNT GREATER 42                                   IX2084.2
049500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2084.2
049600*        MOVE SPACE TO DUMMY-RECORD                               IX2084.2
049700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2084.2
049800*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2084.2
049900*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2084.2
050000*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2084.2
050100*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2084.2
050200*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2084.2
050300*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2084.2
050400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2084.2
050500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2084.2
050600*        MOVE ZERO TO RECORD-COUNT.                               IX2084.2
050700     PERFORM WRT-LN.                                              IX2084.2
050800 WRT-LN.                                                          IX2084.2
050900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2084.2
051000     MOVE SPACE TO DUMMY-RECORD.                                  IX2084.2
051100 BLANK-LINE-PRINT.                                                IX2084.2
051200     PERFORM WRT-LN.                                              IX2084.2
051300 FAIL-ROUTINE.                                                    IX2084.2
051400     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2084.2
051500            GO TO   FAIL-ROUTINE-WRITE.                           IX2084.2
051600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2084.2
051700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2084.2
051800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2084.2
051900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2084.2
052000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2084.2
052100     GO TO  FAIL-ROUTINE-EX.                                      IX2084.2
052200 FAIL-ROUTINE-WRITE.                                              IX2084.2
052300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2084.2
052400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2084.2
052500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2084.2
052600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2084.2
052700 FAIL-ROUTINE-EX. EXIT.                                           IX2084.2
052800 BAIL-OUT.                                                        IX2084.2
052900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2084.2
053000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2084.2
053100 BAIL-OUT-WRITE.                                                  IX2084.2
053200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2084.2
053300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2084.2
053400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2084.2
053500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2084.2
053600 BAIL-OUT-EX. EXIT.                                               IX2084.2
053700 CCVS1-EXIT.                                                      IX2084.2
053800     EXIT.                                                        IX2084.2
053900 SECT-IX208A-0001 SECTION.                                        IX2084.2
054000 WRITE-INIT-GF-01.                                                IX2084.2
054100     OPEN     OUTPUT  IX-FD1.                                     IX2084.2
054200     OPEN     OUTPUT  IX-FS2.                                     IX2084.2
054300     MOVE     "IX-FD1"  TO XFILE-NAME (1).                        IX2084.2
054400     MOVE     "R1-F-G"  TO XRECORD-NAME (1).                      IX2084.2
054500     MOVE     ZERO      TO XRECORD-NUMBER (1).                    IX2084.2
054600     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   IX2084.2
054700     MOVE     000240    TO XRECORD-LENGTH (1).                    IX2084.2
054800     MOVE     0001      TO XBLOCK-SIZE (1).                       IX2084.2
054900     MOVE     "RC"      TO CHARS-OR-RECORDS (1).                  IX2084.2
055000     MOVE     "IX"      TO XFILE-ORGANIZATION (1).                IX2084.2
055100     MOVE     "S"       TO XLABEL-TYPE (1).                       IX2084.2
055200     MOVE     000300    TO IX-FD1-FILESIZE.                       IX2084.2
055300     MOVE     000300    TO RECORDS-IN-FILE (1).                   IX2084.2
055400     MOVE     00001     TO WRK-DU-05V00-001.                      IX2084.2
055500     MOVE     00300     TO WRK-DU-05V00-002.                      IX2084.2
055600     MOVE     ZERO      TO EXCUT-COUNTER-06V00.                   IX2084.2
055700     MOVE     ZERO      TO INV-KEY-COUNTER.                       IX2084.2
055800     MOVE     "WRITE-INIT-GF-01" TO PAR-NAME.                     IX2084.2
055900     MOVE     "IX-FS2"  TO XFILE-NAME (2).                        IX2084.2
056000     MOVE     "R1-F-G"  TO XRECORD-NAME (2).                      IX2084.2
056100     MOVE     ZERO      TO XRECORD-NUMBER (2).                    IX2084.2
056200     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (2).                   IX2084.2
056300     MOVE     000240    TO XRECORD-LENGTH (2).                    IX2084.2
056400     MOVE     0001      TO XBLOCK-SIZE (2).                       IX2084.2
056500     MOVE     "RC"      TO CHARS-OR-RECORDS (2).                  IX2084.2
056600     MOVE     "IX"      TO XFILE-ORGANIZATION (2).                IX2084.2
056700     MOVE     "S"       TO XLABEL-TYPE (2).                       IX2084.2
056800     MOVE     00300     TO IX-FS2-FILESIZE.                       IX2084.2
056900     MOVE     00300     TO RECORDS-IN-FILE (2).                   IX2084.2
057000     MOVE     00001     TO WRK-DU-05V00-003.                      IX2084.2
057100     MOVE     00300     TO WRK-DU-05V00-004.                      IX2084.2
057200 WRITE-TEST-GF-00.                                                IX2084.2
057300     ADD      0001 TO   XRECORD-NUMBER (1).                       IX2084.2
057400     MOVE     WRK-IX-FD1-RECKEY  TO XRECORD-KEY (1).              IX2084.2
057500     MOVE     WRK-IX-FD1-ALTKEY  TO ALTERNATE-KEY1 (1).           IX2084.2
057600     MOVE     FILE-RECORD-INFO (1) TO IX-FD1R1-F-G-240.           IX2084.2
057700     WRITE    IX-FD1R1-F-G-240                                    IX2084.2
057800              INVALID KEY                                         IX2084.2
057900              ADD       000001 TO INV-KEY-COUNTER.                IX2084.2
058000     ADD      000001  TO EXCUT-COUNTER-06V00.                     IX2084.2
058100     ADD      00001   TO WRK-DU-05V00-001.                        IX2084.2
058200     SUBTRACT 00001   FROM WRK-DU-05V00-002.                      IX2084.2
058300     IF       XRECORD-NUMBER (1) LESS THAN IX-FD1-FILESIZE        IX2084.2
058400              GO TO WRITE-TEST-GF-00.                             IX2084.2
058500     CLOSE    IX-FD1.                                             IX2084.2
058600 WRITE-TEST-GF-01.                                                IX2084.2
058700     MOVE     "CREATE FILE IX-FD1"  TO FEATURE.                   IX2084.2
058800     IF       EXCUT-COUNTER-06V00  NOT EQUAL TO IX-FD1-FILESIZE   IX2084.2
058900              PERFORM   FAIL                                      IX2084.2
059000              MOVE      IX-FD1-FILESIZE  TO  CORRECT-N            IX2084.2
059100              MOVE      EXCUT-COUNTER-06V00 TO COMPUTED-N         IX2084.2
059200     MOVE "INCORRECT NUMBER OF WRITES; IX-41"        TO RE-MARK   IX2084.2
059300              PERFORM   PRINT-DETAIL                              IX2084.2
059400              GO TO    WRITE-INIT-GF-02.                          IX2084.2
059500     IF       INV-KEY-COUNTER NOT EQUAL TO ZERO                   IX2084.2
059600              PERFORM   FAIL                                      IX2084.2
059700              MOVE      INV-KEY-COUNTER TO COMPUTED-N             IX2084.2
059800              MOVE      ZERO TO  CORRECT-N                        IX2084.2
059900       MOVE "INVALID KEY ON WRITE; IX-41" TO RE-MARK              IX2084.2
060000              PERFORM   PRINT-DETAIL                              IX2084.2
060100              GO TO    WRITE-INIT-GF-02.                          IX2084.2
060200*                                                                 IX2084.2
060300*    01                                                           IX2084.2
060400*                                                                 IX2084.2
060500     PERFORM  PASS.                                               IX2084.2
060600     PERFORM  PRINT-DETAIL.                                       IX2084.2
060700 WRITE-INIT-GF-02.                                                IX2084.2
060800     MOVE     ZERO   TO INV-KEY-COUNTER.                          IX2084.2
060900     MOVE     ZERO   TO EXCUT-COUNTER-06V00.                      IX2084.2
061000 WRITE-TEST-GF-02-1.                                              IX2084.2
061100     ADD      0001  TO  XRECORD-NUMBER (2).                       IX2084.2
061200     MOVE     WRK-IX-FS2-RECKEY  TO  XRECORD-KEY (2).             IX2084.2
061300     MOVE     WRK-IX-FS2-ALTKEY  TO  ALTERNATE-KEY1 (2).          IX2084.2
061400     MOVE     FILE-RECORD-INFO (2) TO IX-FS2R1-F-G-240.           IX2084.2
061500     WRITE    IX-FS2R1-F-G-240                                    IX2084.2
061600              INVALID KEY                                         IX2084.2
061700              ADD   000001 TO INV-KEY-COUNTER.                    IX2084.2
061800     ADD      000001 TO EXCUT-COUNTER-06V00.                      IX2084.2
061900     ADD      00001  TO WRK-DU-05V00-003.                         IX2084.2
062000     SUBTRACT 00001  FROM WRK-DU-05V00-004.                       IX2084.2
062100     IF       XRECORD-NUMBER (2) LESS THAN IX-FS2-FILESIZE        IX2084.2
062200              GO TO  WRITE-TEST-GF-02-1.                          IX2084.2
062300     CLOSE    IX-FS2.                                             IX2084.2
062400 WRITE-TEST-GF-02.                                                IX2084.2
062500     MOVE     "CREATE FILE IX-FS2" TO  FEATURE.                   IX2084.2
062600     MOVE     "WRITE-TEST-GF-02  " TO PAR-NAME.                   IX2084.2
062700     IF       EXCUT-COUNTER-06V00 NOT EQUAL TO IX-FS2-FILESIZE    IX2084.2
062800              PERFORM   FAIL                                      IX2084.2
062900              MOVE      IX-FS2-FILESIZE  TO  CORRECT-N            IX2084.2
063000              MOVE      EXCUT-COUNTER-06V00 TO COMPUTED-N         IX2084.2
063100       MOVE      "INCORRECT NUMBER OF WRITES; IX-41" TO RE-MARK   IX2084.2
063200              PERFORM   PRINT-DETAIL                              IX2084.2
063300              GO  TO    READ-INIT-F1-01.                          IX2084.2
063400*                                                                 IX2084.2
063500*    02                                                           IX2084.2
063600*                                                                 IX2084.2
063700     IF       INV-KEY-COUNTER  NOT EQUAL TO ZERO                  IX2084.2
063800              PERFORM   FAIL                                      IX2084.2
063900              MOVE      INV-KEY-COUNTER  TO COMPUTED-N            IX2084.2
064000              MOVE      ZERO TO CORRECT-N                         IX2084.2
064100       MOVE      "INVALID KEY ON WRITE; IX-41" TO RE-MARK         IX2084.2
064200              PERFORM   PRINT-DETAIL                              IX2084.2
064300              GO TO     READ-INIT-F1-01.                          IX2084.2
064400     PERFORM  PASS.                                               IX2084.2
064500     PERFORM  PRINT-DETAIL.                                       IX2084.2
064600 READ-INIT-F1-01.                                                 IX2084.2
064700     PERFORM  BLANK-LINE-PRINT.                                   IX2084.2
064800     MOVE     "THE FOLLOWING  TESTS ACCESS A FILE DEFINED AS      IX2084.2
064900-             "ACCESS MODE IS DYNAMIC."  TO  PRINT-REC.           IX2084.2
065000     PERFORM  WRITE-LINE.                                         IX2084.2
065100     PERFORM  BLANK-LINE-PRINT.                                   IX2084.2
065200     MOVE     "READ NEXT"  TO FEATURE.                            IX2084.2
065300     MOVE     "READ-TEST-F1-01" TO PAR-NAME.                      IX2084.2
065400 READ-INIT-F1-01-R1.                                              IX2084.2
065500     OPEN  INPUT  IX-FD1.                                         IX2084.2
065600     PERFORM  INX-INIT-002-R.                                     IX2084.2
065700 READ-TEST-F1-01-1.                                               IX2084.2
065800     READ     IX-FD1 NEXT.                                        IX2084.2
065900     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
066000     PERFORM  INX-VERIFY-002.                                     IX2084.2
066100     IF       EXCUT-COUNTER-06V00 LESS THAN  10                   IX2084.2
066200              GO TO READ-TEST-F1-01-1.                            IX2084.2
066300     CLOSE    IX-FD1.                                             IX2084.2
066400 READ-TEST-F1-01.                                                 IX2084.2
066500     PERFORM  INX-TEST-002.                                       IX2084.2
066600     GO TO  READ-INIT-F1-02.                                      IX2084.2
066700*                                                                 IX2084.2
066800*    01                                                           IX2084.2
066900*                                                                 IX2084.2
067000 READ-DELETE-F1-01.                                               IX2084.2
067100     PERFORM  DE-LETE.                                            IX2084.2
067200     PERFORM  PRINT-DETAIL.                                       IX2084.2
067300 READ-INIT-F1-02.                                                 IX2084.2
067400     PERFORM  INX-INIT-002-R.                                     IX2084.2
067500     OPEN     INPUT     IX-FD1.                                   IX2084.2
067600 READ-TEST-F1-02.                                                 IX2084.2
067700     MOVE     SPACE TO FILE-RECORD-INFO (9).                      IX2084.2
067800     MOVE     SPACE TO FILE-RECORD-INFO (1).                      IX2084.2
067900     READ     IX-FD1   NEXT RECORD                                IX2084.2
068000              INTO   FILE-RECORD-INFO (9).                        IX2084.2
068100     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
068200     PERFORM  INX-VERIFY-002.                                     IX2084.2
068300     IF       XRECORD-NUMBER (9) NOT EQUAL TO LOGICAL-FILE-REC    IX2084.2
068400              ADD   000100  TO ERROR-COUNTER-06V00.               IX2084.2
068500     IF       EXCUT-COUNTER-06V00 LESS THAN 10                    IX2084.2
068600              GO TO  READ-TEST-F1-02.                             IX2084.2
068700     CLOSE    IX-FD1.                                             IX2084.2
068800     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          IX2084.2
068900     MOVE "READ . NEXT INTO" TO FEATURE.                          IX2084.2
069000     PERFORM  INX-TEST-002.                                       IX2084.2
069100*                                                                 IX2084.2
069200*    02                                                           IX2084.2
069300*                                                                 IX2084.2
069400     GO TO  READ-INIT-F1-03.                                      IX2084.2
069500 READ-DELETE-F1-02.                                               IX2084.2
069600     MOVE "READ-TEST-F1-02     " TO PAR-NAME.                     IX2084.2
069700     MOVE "READ ... INTO       " TO FEATURE.                      IX2084.2
069800     PERFORM  DE-LETE.                                            IX2084.2
069900     PERFORM  PRINT-DETAIL.                                       IX2084.2
070000 READ-INIT-F1-03.                                                 IX2084.2
070100     OPEN     INPUT  IX-FD1.                                      IX2084.2
070200     PERFORM  INX-INIT-002-R.                                     IX2084.2
070300 READ-TEST-F1-03.                                                 IX2084.2
070400     MOVE     SPACE TO FILE-RECORD-INFO (9).                      IX2084.2
070500     MOVE     SPACE TO FILE-RECORD-INFO (1).                      IX2084.2
070600     READ     IX-FD1  NEXT                                        IX2084.2
070700              INTO   FILE-RECORD-INFO (9).                        IX2084.2
070800     MOVE     IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX2084.2
070900     PERFORM  INX-VERIFY-002.                                     IX2084.2
071000     IF       XRECORD-NUMBER (9) NOT EQUAL TO LOGICAL-FILE-REC    IX2084.2
071100              ADD   000100  TO  ERROR-COUNTER-06V00.              IX2084.2
071200     IF       EXCUT-COUNTER-06V00 LESS THAN 10                    IX2084.2
071300              GO TO READ-TEST-F1-03.                              IX2084.2
071400     CLOSE    IX-FD1.                                             IX2084.2
071500 READ-TEST-F1-03-1.                                               IX2084.2
071600     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          IX2084.2
071700     MOVE "READ . NEXT INTO" TO FEATURE.                          IX2084.2
071800     PERFORM  INX-TEST-002.                                       IX2084.2
071900*                                                                 IX2084.2
072000*    03                                                           IX2084.2
072100*                                                                 IX2084.2
072200     GO TO  READ-INIT-F1-04.                                      IX2084.2
072300 READ-DELETE-TEST-F1-03.                                          IX2084.2
072400     MOVE "READ-TEST-F1-03     " TO PAR-NAME.                     IX2084.2
072500     PERFORM  DE-LETE.                                            IX2084.2
072600     PERFORM  PRINT-DETAIL.                                       IX2084.2
072700 READ-INIT-F1-04.                                                 IX2084.2
072800     OPEN     INPUT  IX-FD1.                                      IX2084.2
072900     PERFORM  INX-INIT-002-R.                                     IX2084.2
073000     MOVE     IX-FD1-FILESIZE TO ERROR-COUNTER-06V00.             IX2084.2
073100     ADD      000001  TO ERROR-COUNTER-06V00.                     IX2084.2
073200     MOVE "READ-TEST-F1-04" TO PAR-NAME.                          IX2084.2
073300     MOVE "READ . NEXT INTO" TO FEATURE.                          IX2084.2
073400 READ-TEST-F1-04.                                                 IX2084.2
073500     MOVE     SPACE TO FILE-RECORD-INFO (9).                      IX2084.2
073600     MOVE     SPACE TO FILE-RECORD-INFO (1).                      IX2084.2
073700     READ     IX-FD1  NEXT INTO  FILE-RECORD-INFO (9) AT END      IX2084.2
073800              SUBTRACT   000001  FROM  ERROR-COUNTER-06V00        IX2084.2
073900             GO TO  READ-TEST-F1-04-1.                            IX2084.2
074000     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
074100     PERFORM  INX-VERIFY-002.                                     IX2084.2
074200     IF       EXCUT-COUNTER-06V00  GREATER THAN IX-FD1-FILESIZE   IX2084.2
074300              NEXT SENTENCE                                       IX2084.2
074400              ELSE                                                IX2084.2
074500              GO TO   READ-TEST-F1-04.                            IX2084.2
074600*                                                                 IX2084.2
074700*    TEST READ-TEST-F1-04 TESTS THE COBOL CONSTRUCT  "READ FILE-  IX2084.2
074800*    NAME NEXT INTO IDENTIFIER AT END".  THE TEST READS THE FILE  IX2084.2
074900*    SEQUENTIALY VIA THE RECORD KEY  (RECORD KEY IS THE KEY OF    IX2084.2
075000*    REFERENCE) UNTIL AN END-OF-FILE CONDITION OCCURS.  A CHECK   IX2084.2
075100*    IS MADE TO VERIFY THAT THE PROPER RECORDS WERE RETRIVED AND  IX2084.2
075200*    THE AT END PATH WAS TAKEN ON THE 301 ST READ.                IX2084.2
075300*                                                                 IX2084.2
075400 READ-TEST-F1-04-1.                                               IX2084.2
075500     CLOSE   IX-FD1.                                              IX2084.2
075600     PERFORM  INX-TEST-002.                                       IX2084.2
075700*   .04                                                           IX2084.2
075800     GO TO  READ-INIT-F2-01.                                      IX2084.2
075900 READ-DELETE-F1-04.                                               IX2084.2
076000     MOVE "READ-TEST-F1-04     " TO PAR-NAME.                     IX2084.2
076100     PERFORM  DE-LETE.                                            IX2084.2
076200     PERFORM  PRINT-DETAIL.                                       IX2084.2
076300 READ-INIT-F2-01.                                                 IX2084.2
076400     OPEN     INPUT  IX-FD1.                                      IX2084.2
076500     MOVE "READ-TEST-F2-01" TO PAR-NAME.                          IX2084.2
076600     MOVE "READ . KEY IS .." TO FEATURE.                          IX2084.2
076700     PERFORM  INX-INIT-002-R.                                     IX2084.2
076800     MOVE     ZERO TO WRK-DU-05V00-001.                           IX2084.2
076900 READ-TEST-F2-01.                                                 IX2084.2
077000     ADD      00005   TO WRK-DU-05V00-001.                        IX2084.2
077100     ADD      000004  TO LOGICAL-FILE-REC.                        IX2084.2
077200     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
077300     READ     IX-FD1                                              IX2084.2
077400                      KEY IS  IX-FD1-KEY.                         IX2084.2
077500     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
077600     PERFORM  INX-VERIFY-002.                                     IX2084.2
077700     MOVE     SPACE TO FILE-RECORD-INFO (1).                      IX2084.2
077800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
077900              GO TO   READ-TEST-F2-01.                            IX2084.2
078000     CLOSE    IX-FD1.                                             IX2084.2
078100     PERFORM  INX-TEST-002.                                       IX2084.2
078200*   .05                                                           IX2084.2
078300     GO TO  READ-INIT-F2-02.                                      IX2084.2
078400 READ-DELETE-F2-01.                                               IX2084.2
078500     MOVE "READ-TEST-F2-01     " TO PAR-NAME.                     IX2084.2
078600     PERFORM  DE-LETE.                                            IX2084.2
078700     PERFORM  PRINT-DETAIL.                                       IX2084.2
078800 READ-INIT-F2-02.                                                 IX2084.2
078900     MOVE "READ-TEST-F2-02     " TO PAR-NAME.                     IX2084.2
079000     MOVE "READ ... INTO       " TO FEATURE.                      IX2084.2
079100     OPEN     INPUT  IX-FD1.                                      IX2084.2
079200     PERFORM  INX-INIT-002-R.                                     IX2084.2
079300     MOVE     ZERO TO WRK-DU-05V00-001.                           IX2084.2
079400 READ-TEST-F2-02.                                                 IX2084.2
079500     MOVE     SPACE TO FILE-RECORD-INFO (9).                      IX2084.2
079600     MOVE     SPACE TO FILE-RECORD-INFO (1).                      IX2084.2
079700     ADD      00005   TO WRK-DU-05V00-001.                        IX2084.2
079800     ADD      000004  TO LOGICAL-FILE-REC.                        IX2084.2
079900     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
080000     READ     IX-FD1  INTO   FILE-RECORD-INFO (9)                 IX2084.2
080100              KEY IS IX-FD1-KEY.                                  IX2084.2
080200     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
080300     PERFORM  INX-VERIFY-002.                                     IX2084.2
080400     IF       XRECORD-NUMBER (9)   NOT EQUAL   TO LOGICAL-FILE-RECIX2084.2
080500              ADD   000100 TO ERROR-COUNTER-06V00.                IX2084.2
080600     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
080700              GO TO   READ-TEST-F2-02.                            IX2084.2
080800     CLOSE    IX-FD1.                                             IX2084.2
080900     PERFORM  INX-TEST-002.                                       IX2084.2
081000*   .06                                                           IX2084.2
081100     GO TO  READ-INIT-F2-03.                                      IX2084.2
081200 READ-DELETE-F2-02.                                               IX2084.2
081300     MOVE "READ-TEST-F2-02     " TO PAR-NAME.                     IX2084.2
081400     PERFORM  DE-LETE.                                            IX2084.2
081500     PERFORM  PRINT-DETAIL.                                       IX2084.2
081600 READ-INIT-F2-03.                                                 IX2084.2
081700     MOVE "READ-TEST-F2-03     " TO PAR-NAME.                     IX2084.2
081800     MOVE "READ . KEY ALTERNATE" TO FEATURE.                      IX2084.2
081900     OPEN     INPUT  IX-FD1.                                      IX2084.2
082000     PERFORM  INX-INIT-002-R.                                     IX2084.2
082100     MOVE     ZERO  TO WRK-DU-05V00-002.                          IX2084.2
082200     MOVE     301  TO LOGICAL-FILE-REC.                           IX2084.2
082300 READ-TEST-F2-03.                                                 IX2084.2
082400     MOVE      SPACE TO FILE-RECORD-INFO (1).                     IX2084.2
082500     ADD      00005   TO WRK-DU-05V00-002.                        IX2084.2
082600     SUBTRACT  00006  FROM LOGICAL-FILE-REC.                      IX2084.2
082700     MOVE     WRK-IX-FD1-ALTKEY  TO IX-FD1-ALTKEY1.               IX2084.2
082800     READ     IX-FD1  RECORD                                      IX2084.2
082900                             KEY IX-FD1-ALTKEY1.                  IX2084.2
083000     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
083100     PERFORM  INX-VERIFY-002.                                     IX2084.2
083200     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
083300              GO TO   READ-TEST-F2-03.                            IX2084.2
083400     CLOSE    IX-FD1.                                             IX2084.2
083500     PERFORM  INX-TEST-002.                                       IX2084.2
083600*   .07                                                           IX2084.2
083700     GO TO  READ-INIT-F2-04.                                      IX2084.2
083800 READ-DELETE-F2-03.                                               IX2084.2
083900     MOVE "READ-TEST-F2-03     " TO PAR-NAME.                     IX2084.2
084000     PERFORM  DE-LETE.                                            IX2084.2
084100     PERFORM  PRINT-DETAIL.                                       IX2084.2
084200 READ-INIT-F2-04.                                                 IX2084.2
084300     MOVE "READ-TEST-F2-04     " TO PAR-NAME.                     IX2084.2
084400     MOVE "READ .RECORD KEY ..." TO FEATURE.                      IX2084.2
084500     OPEN     INPUT  IX-FD1.                                      IX2084.2
084600     PERFORM  INX-INIT-002-R.                                     IX2084.2
084700     MOVE     00301  TO WRK-DU-05V00-001.                         IX2084.2
084800     MOVE     SPACE TO IX-FD1R1-F-G-240.                          IX2084.2
084900 READ-TEST-F2-04.                                                 IX2084.2
085000     ADD      00005   TO WRK-DU-05V00-001.                        IX2084.2
085100     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
085200     READ     IX-FD1  RECORD                                      IX2084.2
085300                             KEY IX-FD1-KEY                       IX2084.2
085400     INVALID  SUBTRACT  000001  FROM ERROR-COUNTER-06V00.         IX2084.2
085500     ADD     000001  TO EXCUT-COUNTER-06V00.                      IX2084.2
085600     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
085700              GO TO   READ-TEST-F2-04.                            IX2084.2
085800     CLOSE    IX-FD1.                                             IX2084.2
085900     PERFORM  INX-TEST-002.                                       IX2084.2
086000*   .08                                                           IX2084.2
086100     GO TO  READ-INIT-F2-05.                                      IX2084.2
086200 READ-DELETE-F2-04.                                               IX2084.2
086300     MOVE "READ-TEST-F2-04     " TO PAR-NAME.                     IX2084.2
086400     PERFORM  DE-LETE.                                            IX2084.2
086500     PERFORM  PRINT-DETAIL.                                       IX2084.2
086600 READ-INIT-F2-05.                                                 IX2084.2
086700     MOVE "READ-TEST-F2-05     " TO PAR-NAME.                     IX2084.2
086800     MOVE "READ RECORD KEY IS A" TO FEATURE.                      IX2084.2
086900     OPEN     INPUT  IX-FD1.                                      IX2084.2
087000     PERFORM  INX-INIT-002-R.                                     IX2084.2
087100     MOVE     00010  TO WRK-DU-05V00-001.                         IX2084.2
087200     MOVE     00301  TO WRK-DU-05V00-002.                         IX2084.2
087300     MOVE     SPACE TO IX-FD1R1-F-G-240.                          IX2084.2
087400 READ-TEST-F2-05.                                                 IX2084.2
087500     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
087600     MOVE     SPACE TO FILE-RECORD-INFO (1).                      IX2084.2
087700     ADD      00005   TO WRK-DU-05V00-002.                        IX2084.2
087800     MOVE     WRK-IX-FD1-ALTKEY  TO IX-FD1-ALTKEY1.               IX2084.2
087900     READ     IX-FD1  RECORD                                      IX2084.2
088000                             KEY IS IX-FD1-ALTKEY1                IX2084.2
088100     INVALID KEY SUBTRACT  000001  FROM ERROR-COUNTER-06V00.      IX2084.2
088200     ADD    00001  TO EXCUT-COUNTER-06V00.                        IX2084.2
088300     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
088400              GO TO   READ-TEST-F2-05.                            IX2084.2
088500     CLOSE    IX-FD1.                                             IX2084.2
088600     PERFORM  INX-TEST-002.                                       IX2084.2
088700*   .09                                                           IX2084.2
088800     GO TO START-INIT-GF-01.                                      IX2084.2
088900 READ-DELETE-F2-05.                                               IX2084.2
089000     MOVE "READ-TEST-F2-05     " TO PAR-NAME.                     IX2084.2
089100     PERFORM  DE-LETE.                                            IX2084.2
089200     PERFORM  PRINT-DETAIL.                                       IX2084.2
089300 INX-INIT-002-R.                                                  IX2084.2
089400     MOVE     00010  TO ERROR-COUNTER-06V00.                      IX2084.2
089500     MOVE     ZERO TO EXCUT-COUNTER-06V00.                        IX2084.2
089600     MOVE     ZERO TO INV-KEY-COUNTER.                            IX2084.2
089700     MOVE     ZERO TO LOGICAL-FILE-REC.                           IX2084.2
089800 INX-VERIFY-002.                                                  IX2084.2
089900     ADD      000001    TO  EXCUT-COUNTER-06V00.                  IX2084.2
090000     ADD      000001    TO  LOGICAL-FILE-REC.                     IX2084.2
090100     IF       XRECORD-NUMBER (1) EQUAL TO  LOGICAL-FILE-REC       IX2084.2
090200              SUBTRACT   000001 FROM  ERROR-COUNTER-06V00.        IX2084.2
090300 INX-TEST-002.                                                    IX2084.2
090400     IF       ERROR-COUNTER-06V00 EQUAL TO ZERO                   IX2084.2
090500              PERFORM   PASS                                      IX2084.2
090600              ELSE                                                IX2084.2
090700              PERFORM   FAIL                                      IX2084.2
090800              MOVE      ZERO TO   CORRECT-N                       IX2084.2
090900              MOVE      ERROR-COUNTER-06V00 TO COMPUTED-N         IX2084.2
091000              MOVE "SEE PROGRAM (READ-TEST- ; IX-28)" TO RE-MARK. IX2084.2
091100     PERFORM  PRINT-DETAIL.                                       IX2084.2
091200*                                                                 IX2084.2
091300*       EACH TEST IS EXECUTED 10 TIMES EXCEPT FOR  INX-TEST-002-04IX2084.2
091400*    WHICH IS EXECUTED 300 TIMES.  FOLLOWING THE LAST             IX2084.2
091500*    EXECUTION A TEST IS MADE ON ERROR-COUNTER-06V00 WHICH IS     IX2084.2
091600*    EXPECTED TO BE ZERO.  IF ERROR-COUNTER-06V00 IS NOT ZERO     IX2084.2
091700*    THE VALUE IN THE COUNTER INDICATES HOW THE EXECUTION FAILED  IX2084.2
091800*    AND THE NUMBER OF TIMES THE UNEXPECTED ACTION OCCURRED       IX2084.2
091900*    DURING THE TEST.  BEFORE THE TEST BEGINS ERROR-COUNTER-06V00 IX2084.2
092000*    IS INITIALIZED WITH A VALUE.  EACH TIME THE CORRECT RECORD   IX2084.2
092100*    WAS MADE AVAILABLE FOLLOWING THE READ, OR AN INVALID KEY     IX2084.2
092200*    CONDITION OCCURRED THAT WAS EXPECTED FOLLOWING A READ OR     IX2084.2
092300*    START, ERROR-COUNTER-06V00 IS DECREMENTED BY 1.              IX2084.2
092400*    FOR EACH EXECUTION THAT DID NOT PRODUCE THE EXPECTED         IX2084.2
092500*    RESULTS  THE ERROR-COUNTER-06V00 IS INCREMENTED BY THE VALUE IX2084.2
092600*    FOR THE ACTION LISTED BELOW, E.G., VALUE 20003 WOULD INDICATEIX2084.2
092700*    THAT OF THE 10 EXECUTIONS DURING THE TEST (READING LEFT TO   IX2084.2
092800*    RIGHT)  2 INVALID KEY CONDITIONS AND 3 RECORDS RETRIEVED     IX2084.2
092900*    AS A RESULT OF THE READ OR START WAS NOT-AS EXPECTED.        IX2084.2
093000*                                                                 IX2084.2
093100*                                                                 IX2084.2
093200*                                                                 IX2084.2
093300*    COMPUTED RESULT                 INDICATED                    IX2084.2
093400*       INCREMENTS                     ACTION                     IX2084.2
093500*                                                                 IX2084.2
093600*       000100          THE  RECORD  FOUND IN THE IDENTIFIER      IX2084.2
093700*                       SPECIFIED IN THE INTO PHRASE OF THE       IX2084.2
093800*                       READ STATEMENT WAS NOT THE RECORD         IX2084.2
093900*                       EXPECTED FOLLOWING EXECUTION OF THE       IX2084.2
094000*                       READ.                                     IX2084.2
094100*                                                                 IX2084.2
094200*       000001          THE RECORD RETREIVED FROM THE FILE        IX2084.2
094300*                       FOLLOWING THE READ WAS NOT THE ONE        IX2084.2
094400*                       EXPECTED.                                 IX2084.2
094500*                                                                 IX2084.2
094600*       010000          AN UNEXPECTED INVALID KEY OR AT END       IX2084.2
094700*                       CONDITION OCCURRED.  NOTE - ASSUMPTION    IX2084.2
094800*                       IS THAT THE "USE" STATEMENT IS ONLY       IX2084.2
094900*                       EXECUTED WHEN AN INVALID KEY OR AT END    IX2084.2
095000*                       CONDITION OCCURS AND THE INVALID KEY OR   IX2084.2
095100*                       AT END PHRASE HAS NOT BEEN SPECIFIED.     IX2084.2
095200*                                                                 IX2084.2
095300 START-INIT-GF-01.                                                IX2084.2
095400     OPEN     INPUT  IX-FD1.                                      IX2084.2
095500     OPEN     INPUT  IX-FS2.                                      IX2084.2
095600     PERFORM  BLANK-LINE-PRINT.                                   IX2084.2
095700     MOVE     "THE FOLLOWING TESTS ACCESS A FILE DEFINE AS        IX2084.2
095800-             "ACCESS MODE IS SEQUENTIAL"  TO PRINT-REC.          IX2084.2
095900     PERFORM  WRITE-LINE.                                         IX2084.2
096000     PERFORM  BLANK-LINE-PRINT.                                   IX2084.2
096100     MOVE "START-TEST-GF-01    " TO PAR-NAME.                     IX2084.2
096200     MOVE     "START  EQUAL TO"  TO FEATURE.                      IX2084.2
096300     PERFORM  INX-INIT-003-R.                                     IX2084.2
096400 START-TEST-GF-01.                                                IX2084.2
096500     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
096600     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
096700     START    IX-FS2.                                             IX2084.2
096800     READ     IX-FS2       RECORD  AT END                         IX2084.2
096900             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
097000              GO TO      START-TEST-GF-01-1.                      IX2084.2
097100     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
097200     PERFORM  INX-VERIFY-003A.                                    IX2084.2
097300     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
097400              GO TO   START-TEST-GF-01.                           IX2084.2
097500 START-TEST-GF-01-1.                                              IX2084.2
097600     PERFORM  INX-TEST-003.                                       IX2084.2
097700*   .01                                                           IX2084.2
097800     GO TO  START-INIT-GF-02.                                     IX2084.2
097900 INX-DELETE-003-01.                                               IX2084.2
098000     MOVE "START-TEST-GF-01    " TO PAR-NAME.                     IX2084.2
098100     PERFORM  DE-LETE.                                            IX2084.2
098200     PERFORM  PRINT-DETAIL.                                       IX2084.2
098300 START-INIT-GF-02.                                                IX2084.2
098400     PERFORM  INX-INIT-003-R.                                     IX2084.2
098500 START-TEST-GF-02.                                                IX2084.2
098600     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
098700     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
098800     START    IX-FS2                                              IX2084.2
098900                      KEY EQUAL TO IX-FS2-KEY.                    IX2084.2
099000     READ     IX-FS2       RECORD  AT END                         IX2084.2
099100             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
099200              GO TO     START-TEST-GF-02-1.                       IX2084.2
099300     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
099400     PERFORM  INX-VERIFY-003A.                                    IX2084.2
099500     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
099600              GO TO    START-TEST-GF-02.                          IX2084.2
099700 START-TEST-GF-02-1.                                              IX2084.2
099800     MOVE "START-TEST-GF-02    " TO PAR-NAME.                     IX2084.2
099900     MOVE "START KEY EQUAL TO  " TO FEATURE.                      IX2084.2
100000     PERFORM  INX-TEST-003.                                       IX2084.2
100100*   .02                                                           IX2084.2
100200     GO TO   START-INIT-GF-03.                                    IX2084.2
100300 START-DELETE-GF-02.                                              IX2084.2
100400     MOVE "START-TEST-GF-02    " TO PAR-NAME.                     IX2084.2
100500     PERFORM  DE-LETE.                                            IX2084.2
100600     PERFORM  PRINT-DETAIL.                                       IX2084.2
100700 START-INIT-GF-03.                                                IX2084.2
100800     PERFORM  INX-INIT-003-R.                                     IX2084.2
100900 START-TEST-GF-03.                                                IX2084.2
101000     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
101100     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
101200     START    IX-FS2                                              IX2084.2
101300                      KEY IS EQUAL TO  IX-FS2-KEY.                IX2084.2
101400     READ     IX-FS2       RECORD  AT END                         IX2084.2
101500             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
101600              GO TO    START-TEST-GF-03-1.                        IX2084.2
101700     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
101800     PERFORM  INX-VERIFY-003A.                                    IX2084.2
101900     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
102000              GO TO    START-TEST-GF-03.                          IX2084.2
102100 START-TEST-GF-03-1.                                              IX2084.2
102200     MOVE "START-TEST-GF-03    " TO PAR-NAME.                     IX2084.2
102300     MOVE "START KEY IS EQUAL  " TO FEATURE.                      IX2084.2
102400     PERFORM  INX-TEST-003.                                       IX2084.2
102500*   .03                                                           IX2084.2
102600     GO TO  START-INIT-GF-04.                                     IX2084.2
102700 START-DELETE-GF-03.                                              IX2084.2
102800     MOVE "START-TEST-GF-03    " TO PAR-NAME.                     IX2084.2
102900     MOVE "START KEY IS EQUAL  " TO FEATURE.                      IX2084.2
103000     PERFORM  DE-LETE.                                            IX2084.2
103100     PERFORM  PRINT-DETAIL.                                       IX2084.2
103200 START-INIT-GF-04.                                                IX2084.2
103300     PERFORM  INX-INIT-003-R.                                     IX2084.2
103400 START-TEST-GF-04.                                                IX2084.2
103500     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
103600     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
103700     START    IX-FS2                                              IX2084.2
103800                      KEY IS EQUAL  IX-FS2-KEY.                   IX2084.2
103900     READ     IX-FS2       RECORD  AT END                         IX2084.2
104000             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
104100              GO TO       START-TEST-GF-04-1.                     IX2084.2
104200     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
104300     PERFORM  INX-VERIFY-003A.                                    IX2084.2
104400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
104500              GO TO   START-TEST-GF-04.                           IX2084.2
104600 START-TEST-GF-04-1.                                              IX2084.2
104700     MOVE "START-TEST-GF-04    " TO PAR-NAME.                     IX2084.2
104800     MOVE "START KEY IS EQUAL  " TO FEATURE.                      IX2084.2
104900     PERFORM  INX-TEST-003.                                       IX2084.2
105000*   .04                                                           IX2084.2
105100     GO TO START-INIT-GF-05.                                      IX2084.2
105200 INX-DELETE-003-04.                                               IX2084.2
105300     MOVE "START-TEST-GF-04    " TO PAR-NAME.                     IX2084.2
105400     PERFORM  DE-LETE.                                            IX2084.2
105500     PERFORM  PRINT-DETAIL.                                       IX2084.2
105600 START-INIT-GF-05.                                                IX2084.2
105700     PERFORM  INX-INIT-003-R.                                     IX2084.2
105800 START-TEST-GF-05.                                                IX2084.2
105900     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
106000     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
106100     START    IX-FS2                                              IX2084.2
106200                      KEY IS  =  IX-FS2-KEY.                      IX2084.2
106300     READ     IX-FS2       RECORD  AT END                         IX2084.2
106400             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
106500              GO TO       START-TEST-GF-05-1.                     IX2084.2
106600     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
106700     PERFORM  INX-VERIFY-003A.                                    IX2084.2
106800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
106900              GO TO    START-TEST-GF-05.                          IX2084.2
107000 START-TEST-GF-05-1.                                              IX2084.2
107100     MOVE "START-TEST-GF-05    " TO PAR-NAME.                     IX2084.2
107200     MOVE "START KEY IS = ...  " TO FEATURE.                      IX2084.2
107300     PERFORM  INX-TEST-003.                                       IX2084.2
107400*   .05                                                           IX2084.2
107500     GO TO  START-INIT-GF-06.                                     IX2084.2
107600 START-DELETE-GF-05.                                              IX2084.2
107700     MOVE "START-TEST-GF-05    " TO PAR-NAME.                     IX2084.2
107800     PERFORM  DE-LETE.                                            IX2084.2
107900     PERFORM  PRINT-DETAIL.                                       IX2084.2
108000 START-INIT-GF-06.                                                IX2084.2
108100     PERFORM  INX-INIT-003-R.                                     IX2084.2
108200     ADD      000001 TO LOGICAL-FILE-REC.                         IX2084.2
108300 START-TEST-GF-06.                                                IX2084.2
108400     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
108500     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
108600     START    IX-FS2                                              IX2084.2
108700                      KEY IS GREATER THAN  IX-FS2-KEY.            IX2084.2
108800     READ     IX-FS2       RECORD  AT END                         IX2084.2
108900             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
109000              GO TO   START-TEST-GF-06-1.                         IX2084.2
109100     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
109200     PERFORM  INX-VERIFY-003A.                                    IX2084.2
109300     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
109400              GO TO   START-TEST-GF-06.                           IX2084.2
109500 START-TEST-GF-06-1.                                              IX2084.2
109600     MOVE "START-TEST-GF-06    " TO PAR-NAME.                     IX2084.2
109700     MOVE     "START  GREATER THAN"  TO FEATURE.                  IX2084.2
109800     PERFORM  INX-TEST-003.                                       IX2084.2
109900*   .06                                                           IX2084.2
110000     GO TO START-INIT-GF-07.                                      IX2084.2
110100 START-DELETE-GF-06.                                              IX2084.2
110200     MOVE "START-TEST-GF-06    " TO PAR-NAME.                     IX2084.2
110300     PERFORM  DE-LETE.                                            IX2084.2
110400     PERFORM  PRINT-DETAIL.                                       IX2084.2
110500 START-INIT-GF-07.                                                IX2084.2
110600     PERFORM  INX-INIT-003-R.                                     IX2084.2
110700     ADD      000001 TO LOGICAL-FILE-REC.                         IX2084.2
110800 START-TEST-GF-07.                                                IX2084.2
110900     ADD      00003   TO WRK-DU-05V00-003.                        IX2084.2
111000     MOVE     WRK-IX-FS2-RECKEY TO IX-FS2-KEY.                    IX2084.2
111100     START    IX-FS2                                              IX2084.2
111200                      KEY GREATER THAN IX-FS2-KEY.                IX2084.2
111300     READ     IX-FS2       RECORD  AT END                         IX2084.2
111400             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
111500              GO TO   START-TEST-GF-07-1.                         IX2084.2
111600     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
111700     PERFORM  INX-VERIFY-003A.                                    IX2084.2
111800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
111900              GO TO   START-TEST-GF-07.                           IX2084.2
112000 START-TEST-GF-07-1.                                              IX2084.2
112100     MOVE "START-TEST-GF-07    " TO PAR-NAME.                     IX2084.2
112200     MOVE "START KEY GREATER THAN" TO FEATURE.                    IX2084.2
112300*   .07                                                           IX2084.2
112400     GO TO  START-INIT-GF-08.                                     IX2084.2
112500 START-DELETE-GF-07.                                              IX2084.2
112600     MOVE "START-TEST-GF-07    " TO PAR-NAME.                     IX2084.2
112700     PERFORM  DE-LETE.                                            IX2084.2
112800     PERFORM  PRINT-DETAIL.                                       IX2084.2
112900 START-INIT-GF-08.                                                IX2084.2
113000     PERFORM  INX-INIT-003-R.                                     IX2084.2
113100     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
113200              GIVING     LOGICAL-FILE-REC.                        IX2084.2
113300     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
113400 START-TEST-GF-08.                                                IX2084.2
113500     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
113600     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
113700     START    IX-FS2                                              IX2084.2
113800                      KEY IS GREATER IX-FS2-ALTKEY1.              IX2084.2
113900     READ     IX-FS2       RECORD  AT END                         IX2084.2
114000             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
114100              GO TO       START-TEST-GF-08-1.                     IX2084.2
114200     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
114300     PERFORM  INX-VERIFY-003A.                                    IX2084.2
114400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
114500              GO TO    START-TEST-GF-08.                          IX2084.2
114600 START-TEST-GF-08-1.                                              IX2084.2
114700     MOVE "START-TEST-GF-08    " TO PAR-NAME.                     IX2084.2
114800     MOVE "START KEY IS GREATER" TO FEATURE.                      IX2084.2
114900     PERFORM  INX-TEST-003.                                       IX2084.2
115000*   .08                                                           IX2084.2
115100     GO TO   START-INIT-GF-09.                                    IX2084.2
115200 START-DELETE-GF-08.                                              IX2084.2
115300     MOVE "START-TEST-GF-08    " TO PAR-NAME.                     IX2084.2
115400     PERFORM  DE-LETE.                                            IX2084.2
115500     PERFORM  PRINT-DETAIL.                                       IX2084.2
115600 START-INIT-GF-09.                                                IX2084.2
115700     PERFORM  INX-INIT-003-R.                                     IX2084.2
115800     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
115900              GIVING     LOGICAL-FILE-REC.                        IX2084.2
116000     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
116100 START-TEST-GF-09.                                                IX2084.2
116200     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
116300     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
116400     START    IX-FS2                                              IX2084.2
116500                      KEY IS >  IX-FS2-ALTKEY1.                   IX2084.2
116600     READ     IX-FS2       RECORD  AT END                         IX2084.2
116700             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
116800              GO TO       START-TEST-GF-09-1.                     IX2084.2
116900     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
117000     PERFORM   INX-VERIFY-003A.                                   IX2084.2
117100     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
117200              GO TO START-TEST-GF-09.                             IX2084.2
117300 START-TEST-GF-09-1.                                              IX2084.2
117400     MOVE "START-TEST-GF-09    " TO PAR-NAME.                     IX2084.2
117500     MOVE "START KEY IS > ...  " TO FEATURE.                      IX2084.2
117600     PERFORM  INX-TEST-003.                                       IX2084.2
117700*   .09                                                           IX2084.2
117800     GO TO START-INIT-GF-10.                                      IX2084.2
117900 START-DELETE-GF-09.                                              IX2084.2
118000     MOVE "START-TEST-GF-09    " TO PAR-NAME.                     IX2084.2
118100     PERFORM  DE-LETE.                                            IX2084.2
118200     PERFORM  PRINT-DETAIL.                                       IX2084.2
118300 START-INIT-GF-10.                                                IX2084.2
118400     PERFORM  INX-INIT-003-R.                                     IX2084.2
118500     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
118600              GIVING     LOGICAL-FILE-REC.                        IX2084.2
118700     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
118800 START-TEST-GF-10.                                                IX2084.2
118900     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
119000     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
119100     START    IX-FS2                                              IX2084.2
119200                      KEY  > IX-FS2-ALTKEY1.                      IX2084.2
119300     READ     IX-FS2       RECORD  AT END                         IX2084.2
119400             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
119500              GO TO       START-TEST-GF-10-1.                     IX2084.2
119600     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
119700     PERFORM  INX-VERIFY-003A.                                    IX2084.2
119800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
119900              GO TO   START-TEST-GF-10.                           IX2084.2
120000 START-TEST-GF-10-1.                                              IX2084.2
120100     MOVE "START-TEST-GF-10    " TO PAR-NAME.                     IX2084.2
120200     MOVE "START ... KEY > ... " TO FEATURE.                      IX2084.2
120300     PERFORM  INX-TEST-003.                                       IX2084.2
120400*   .10                                                           IX2084.2
120500     GO TO  START-INIT-GF-11.                                     IX2084.2
120600 START-DELETE-GF-10.                                              IX2084.2
120700     MOVE "START-TEST-GF-10    " TO PAR-NAME.                     IX2084.2
120800     PERFORM  DE-LETE.                                            IX2084.2
120900     PERFORM  PRINT-DETAIL.                                       IX2084.2
121000 START-INIT-GF-11.                                                IX2084.2
121100     MOVE     "START  NOT LESS THAN" TO FEATURE.                  IX2084.2
121200     PERFORM  INX-INIT-003-R.                                     IX2084.2
121300     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
121400              GIVING     LOGICAL-FILE-REC.                        IX2084.2
121500     ADD      000001  TO LOGICAL-FILE-REC.                        IX2084.2
121600     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
121700 START-TEST-GF-11.                                                IX2084.2
121800     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
121900     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
122000     START    IX-FS2                                              IX2084.2
122100                      KEY IS NOT LESS THAN IX-FS2-ALTKEY1.        IX2084.2
122200     READ     IX-FS2       RECORD  AT END                         IX2084.2
122300             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
122400              GO TO       START-TEST-GF-11-1.                     IX2084.2
122500     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
122600     PERFORM  INX-VERIFY-003A.                                    IX2084.2
122700     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
122800              GO TO    START-TEST-GF-11.                          IX2084.2
122900 START-TEST-GF-11-1.                                              IX2084.2
123000     MOVE "START-TEST-GF-11    " TO PAR-NAME.                     IX2084.2
123100     MOVE "START KEY IS NOT LESS THAN" TO FEATURE.                IX2084.2
123200     PERFORM  INX-TEST-003.                                       IX2084.2
123300*   .11                                                           IX2084.2
123400     GO TO START-INIT-GF-12.                                      IX2084.2
123500 START-DELETE-GF-22.                                              IX2084.2
123600     MOVE "START-TEST-GF-11    " TO PAR-NAME.                     IX2084.2
123700     PERFORM  DE-LETE.                                            IX2084.2
123800     PERFORM PRINT-DETAIL.                                        IX2084.2
123900 START-INIT-GF-12.                                                IX2084.2
124000     PERFORM  INX-INIT-003-R.                                     IX2084.2
124100     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
124200              GIVING     LOGICAL-FILE-REC.                        IX2084.2
124300     ADD      000001  TO LOGICAL-FILE-REC.                        IX2084.2
124400     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
124500 START-TEST-GF-12.                                                IX2084.2
124600     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
124700     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
124800     START    IX-FS2                                              IX2084.2
124900                      KEY IS NOT LESS  IX-FS2-ALTKEY1.            IX2084.2
125000     READ     IX-FS2       RECORD  AT END                         IX2084.2
125100             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
125200              GO TO       START-TEST-GF-12-1.                     IX2084.2
125300     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
125400     PERFORM  INX-VERIFY-003A.                                    IX2084.2
125500     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
125600              GO TO   START-TEST-GF-12.                           IX2084.2
125700 START-TEST-GF-12-1.                                              IX2084.2
125800     MOVE "START-TEST-GF-12    " TO PAR-NAME.                     IX2084.2
125900     MOVE "START KEY IS NOT LESS" TO FEATURE.                     IX2084.2
126000     PERFORM  INX-TEST-003.                                       IX2084.2
126100*   .12                                                           IX2084.2
126200     GO TO START-INIT-GF-13.                                      IX2084.2
126300 START-DELETE-GF-12.                                              IX2084.2
126400     MOVE "START-TEST-GF-12    " TO PAR-NAME.                     IX2084.2
126500     PERFORM  DE-LETE.                                            IX2084.2
126600     PERFORM  PRINT-DETAIL.                                       IX2084.2
126700 START-INIT-GF-13.                                                IX2084.2
126800     PERFORM  INX-INIT-003-R.                                     IX2084.2
126900     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
127000              GIVING     LOGICAL-FILE-REC.                        IX2084.2
127100     ADD      000001  TO LOGICAL-FILE-REC.                        IX2084.2
127200     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
127300 START-TEST-GF-13.                                                IX2084.2
127400     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
127500     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
127600     START    IX-FS2                                              IX2084.2
127700                      KEY NOT LESS THAN  IX-FS2-ALTKEY1.          IX2084.2
127800     READ     IX-FS2       RECORD  AT END                         IX2084.2
127900             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
128000              GO TO       START-TEST-GF-13-1.                     IX2084.2
128100     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
128200     PERFORM  INX-VERIFY-003A.                                    IX2084.2
128300     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
128400              GO TO    START-TEST-GF-13.                          IX2084.2
128500 START-TEST-GF-13-1.                                              IX2084.2
128600     MOVE "START-TEST-GF-13    " TO PAR-NAME.                     IX2084.2
128700     MOVE "START KEY NOT LESS THAN " TO FEATURE.                  IX2084.2
128800     PERFORM  INX-TEST-003.                                       IX2084.2
128900*   .13                                                           IX2084.2
129000     GO TO START-INIT-GF-14.                                      IX2084.2
129100 START-DELETE-GF-13.                                              IX2084.2
129200     MOVE "START-TEST-GF-13    " TO PAR-NAME.                     IX2084.2
129300     PERFORM  DE-LETE.                                            IX2084.2
129400     PERFORM  PRINT-DETAIL.                                       IX2084.2
129500 START-INIT-GF-14.                                                IX2084.2
129600     PERFORM  INX-INIT-003-R.                                     IX2084.2
129700     SUBTRACT   WRK-DU-05V00-004  FROM  IX-FS2-FILESIZE           IX2084.2
129800              GIVING     LOGICAL-FILE-REC.                        IX2084.2
129900     ADD      000001  TO LOGICAL-FILE-REC.                        IX2084.2
130000     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
130100 START-TEST-GF-14.                                                IX2084.2
130200     ADD      00003   TO WRK-DU-05V00-004.                        IX2084.2
130300     MOVE     WRK-IX-FS2-ALTKEY  TO IX-FS2-ALTKEY1.               IX2084.2
130400     START    IX-FS2                                              IX2084.2
130500                      KEY IS NOT  <   IX-FS2-ALTKEY1.             IX2084.2
130600     READ     IX-FS2       RECORD  AT END                         IX2084.2
130700             ADD   010000  TO ERROR-COUNTER-06V00                 IX2084.2
130800              GO TO       START-TEST-GF-14-1.                     IX2084.2
130900     MOVE     IX-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         IX2084.2
131000     PERFORM  INX-VERIFY-003A.                                    IX2084.2
131100     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
131200              GO TO   START-TEST-GF-14.                           IX2084.2
131300 START-TEST-GF-14-1.                                              IX2084.2
131400     MOVE "START-TEST-GF-14    " TO PAR-NAME.                     IX2084.2
131500     MOVE "START KEY IS NOT <  " TO FEATURE.                      IX2084.2
131600     PERFORM  INX-TEST-003.                                       IX2084.2
131700*   .14                                                           IX2084.2
131800     GO TO  START-INIT-GF-15.                                     IX2084.2
131900 START-DELETE-GF-14.                                              IX2084.2
132000     MOVE "START-TEST-GF-14    " TO PAR-NAME.                     IX2084.2
132100     PERFORM  DE-LETE.                                            IX2084.2
132200     PERFORM  PRINT-DETAIL.                                       IX2084.2
132300 START-INIT-GF-15.                                                IX2084.2
132400     PERFORM  BLANK-LINE-PRINT.                                   IX2084.2
132500     MOVE     "THE FOLLOWING TESTS ACCESS A FILE DEFINED AS       IX2084.2
132600-             "ACCESS MODE IS DYNAMIC"  TO PRINT-REC.             IX2084.2
132700     PERFORM  WRITE-LINE.                                         IX2084.2
132800     PERFORM  BLANK-LINE-PRINT.                                   IX2084.2
132900     MOVE     "START  EQUAL TO " TO FEATURE.                      IX2084.2
133000     PERFORM  INX-INIT-003-R.                                     IX2084.2
133100     SUBTRACT   WRK-DU-05V00-002  FROM  IX-FD1-FILESIZE           IX2084.2
133200              GIVING     LOGICAL-FILE-REC.                        IX2084.2
133300     ADD      000001  TO LOGICAL-FILE-REC.                        IX2084.2
133400     MOVE     "DN"  TO ASCEND-DESEND-SWITCH.                      IX2084.2
133500 START-TEST-GF-15.                                                IX2084.2
133600     ADD      000002  TO WRK-DU-05V00-002.                        IX2084.2
133700     MOVE     WRK-IX-FD1-ALTKEY  TO IX-FD1-ALTKEY1.               IX2084.2
133800     START    IX-FD1                                              IX2084.2
133900                      KEY IS EQUAL TO  IX-FD1-ALTKEY1             IX2084.2
134000     INVALID KEY  ADD 010000  TO ERROR-COUNTER-06V00.             IX2084.2
134100     READ     IX-FD1  NEXT RECORD  AT END                         IX2084.2
134200              ADD   010000  TO ERROR-COUNTER-06V00                IX2084.2
134300              GO TO       START-TEST-GF-15-1.                     IX2084.2
134400     MOVE     IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         IX2084.2
134500     PERFORM  INX-VERIFY-003B.                                    IX2084.2
134600     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
134700              GO TO    START-TEST-GF-15.                          IX2084.2
134800 START-TEST-GF-15-1.                                              IX2084.2
134900     MOVE "START-TEST-GF-15    " TO PAR-NAME.                     IX2084.2
135000     MOVE "START KEY IS EQUAL TO" TO FEATURE.                     IX2084.2
135100     PERFORM  INX-TEST-003.                                       IX2084.2
135200*   .15                                                           IX2084.2
135300     GO TO START-INIT-GF-16.                                      IX2084.2
135400 START-DELETE-GF-15.                                              IX2084.2
135500     MOVE "START-TEST-GF-15    " TO PAR-NAME.                     IX2084.2
135600     PERFORM  DE-LETE.                                            IX2084.2
135700     PERFORM  PRINT-DETAIL.                                       IX2084.2
135800 START-INIT-GF-16.                                                IX2084.2
135900     MOVE      00055  TO WRK-DU-05V00-001.                        IX2084.2
136000     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
136100     MOVE     "START  INVALID KEY"  TO FEATURE.                   IX2084.2
136200     PERFORM  INX-INIT-003-R.                                     IX2084.2
136300     MOVE     IX-FD1-FILESIZE  TO  LOGICAL-FILE-REC.              IX2084.2
136400     MOVE     IX-FD1-FILESIZE  TO  WRK-DU-05V00-002.              IX2084.2
136500 START-TEST-GF-16.                                                IX2084.2
136600     ADD      000002  TO WRK-DU-05V00-002.                        IX2084.2
136700     MOVE     WRK-IX-FD1-ALTKEY  TO IX-FD1-ALTKEY1.               IX2084.2
136800     START    IX-FD1                                              IX2084.2
136900                      KEY IS EQUAL TO IX-FD1-ALTKEY1              IX2084.2
137000      INVALID SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        IX2084.2
137100     ADD      000001 TO EXCUT-COUNTER-06V00.                      IX2084.2
137200     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
137300              GO TO    START-TEST-GF-16.                          IX2084.2
137400     MOVE "START-TEST-GF-16    " TO PAR-NAME.                     IX2084.2
137500     PERFORM  INX-TEST-003.                                       IX2084.2
137600*   .16                                                           IX2084.2
137700     GO TO    START-INIT-GF-17.                                   IX2084.2
137800 START-DELETE-GF-16.                                              IX2084.2
137900     MOVE "START-TEST-GF-16    " TO PAR-NAME.                     IX2084.2
138000     PERFORM  DE-LETE.                                            IX2084.2
138100     PERFORM PRINT-DETAIL.                                        IX2084.2
138200 START-INIT-GF-17.                                                IX2084.2
138300     MOVE     00055  TO WRK-DU-05V00-002.                         IX2084.2
138400     MOVE     WRK-IX-FD1-ALTKEY TO IX-FD1-ALTKEY1.                IX2084.2
138500     PERFORM  INX-INIT-003-R.                                     IX2084.2
138600     MOVE     IX-FD1-FILESIZE  TO  LOGICAL-FILE-REC.              IX2084.2
138700     MOVE     IX-FD1-FILESIZE  TO  WRK-DU-05V00-001.              IX2084.2
138800 START-TEST-GF-17.                                                IX2084.2
138900     ADD      00003   TO WRK-DU-05V00-001.                        IX2084.2
139000     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
139100     START    IX-FD1   INVALID KEY                                IX2084.2
139200              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        IX2084.2
139300     ADD      000001 TO EXCUT-COUNTER-06V00.                      IX2084.2
139400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
139500              GO TO    START-TEST-GF-17.                          IX2084.2
139600     MOVE "START-TEST-GF-17    " TO PAR-NAME.                     IX2084.2
139700     PERFORM  INX-TEST-003.                                       IX2084.2
139800*   .17                                                           IX2084.2
139900     GO TO   START-INIT-GF-18.                                    IX2084.2
140000 START-DELETE-GF-17.                                              IX2084.2
140100     MOVE "START-TEST-GF-17    " TO PAR-NAME.                     IX2084.2
140200     PERFORM  DE-LETE.                                            IX2084.2
140300     PERFORM  PRINT-DETAIL.                                       IX2084.2
140400 START-INIT-GF-18.                                                IX2084.2
140500     MOVE     00055  TO WRK-DU-05V00-002.                         IX2084.2
140600     MOVE     WRK-IX-FD1-ALTKEY TO IX-FD1-ALTKEY1.                IX2084.2
140700     PERFORM  INX-INIT-003-R.                                     IX2084.2
140800     MOVE     IX-FD1-FILESIZE  TO  LOGICAL-FILE-REC.              IX2084.2
140900     MOVE     IX-FD1-FILESIZE  TO  WRK-DU-05V00-001.              IX2084.2
141000 START-TEST-GF-18.                                                IX2084.2
141100     ADD      00003   TO WRK-DU-05V00-001.                        IX2084.2
141200     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
141300     START    IX-FD1  ; INVALID KEY                               IX2084.2
141400              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        IX2084.2
141500     ADD      000001 TO EXCUT-COUNTER-06V00.                      IX2084.2
141600     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
141700              GO TO    START-TEST-GF-18.                          IX2084.2
141800     MOVE "START-TEST-GF-18    " TO PAR-NAME.                     IX2084.2
141900     PERFORM  INX-TEST-003.                                       IX2084.2
142000*   .18                                                           IX2084.2
142100     GO TO START-INIT-GF-19.                                      IX2084.2
142200 START-DELETE-GF-18.                                              IX2084.2
142300     MOVE "START-TEST-GF-18    " TO PAR-NAME.                     IX2084.2
142400     PERFORM  DE-LETE.                                            IX2084.2
142500     PERFORM  PRINT-DETAIL.                                       IX2084.2
142600 START-INIT-GF-19.                                                IX2084.2
142700     PERFORM  INX-INIT-003-R.                                     IX2084.2
142800     MOVE     IX-FD1-FILESIZE  TO  WRK-DU-05V00-001.              IX2084.2
142900     MOVE     WRK-IX-FD1-RECKEY TO IX-FD1-KEY.                    IX2084.2
143000     MOVE     IX-FD1-FILESIZE  TO  WRK-DU-05V00-002.              IX2084.2
143100 START-TEST-GF-19.                                                IX2084.2
143200     ADD      000002  TO WRK-DU-05V00-002.                        IX2084.2
143300     MOVE     WRK-IX-FD1-ALTKEY TO IX-FD1-ALTKEY1.                IX2084.2
143400     START    IX-FD1                                              IX2084.2
143500                     KEY IS EQUAL TO  IX-FD1-ALTKEY1              IX2084.2
143600     ; INVALID KEY SUBTRACT 000001  FROM ERROR-COUNTER-06V00.     IX2084.2
143700     ADD      000001  TO EXCUT-COUNTER-06V00.                     IX2084.2
143800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   IX2084.2
143900              GO TO   START-TEST-GF-19.                           IX2084.2
144000     MOVE "START-TEST-GF-19    " TO PAR-NAME.                     IX2084.2
144100     PERFORM  INX-TEST-003.                                       IX2084.2
144200*   .19                                                           IX2084.2
144300     GO TO START-END.                                             IX2084.2
144400 START-DELETE-GF-19.                                              IX2084.2
144500     MOVE "START-TEST-GF-19    " TO PAR-NAME.                     IX2084.2
144600     PERFORM  DE-LETE.                                            IX2084.2
144700     PERFORM  PRINT-DETAIL.                                       IX2084.2
144800 INX-INIT-003-R.                                                  IX2084.2
144900     MOVE     ZERO TO LOGICAL-FILE-REC.                           IX2084.2
145000     MOVE     ZERO TO EXCUT-COUNTER-06V00.                        IX2084.2
145100     MOVE     00055  TO WRK-DU-05V00-002.                         IX2084.2
145200     MOVE     00050  TO WRK-DU-05V00-004.                         IX2084.2
145300     MOVE     ZERO TO WRK-DU-05V00-003.                           IX2084.2
145400     MOVE     10  TO ERROR-COUNTER-06V00.                         IX2084.2
145500 INX-VERIFY-003A.                                                 IX2084.2
145600     IF       ASCEND                                              IX2084.2
145700              ADD    000003  TO LOGICAL-FILE-REC                  IX2084.2
145800              ELSE                                                IX2084.2
145900              SUBTRACT   000003  FROM  LOGICAL-FILE-REC.          IX2084.2
146000     IF       LOGICAL-FILE-REC  EQUAL  TO XRECORD-NUMBER (2)      IX2084.2
146100              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        IX2084.2
146200     ADD      000001 TO EXCUT-COUNTER-06V00.                      IX2084.2
146300 INX-VERIFY-003B.                                                 IX2084.2
146400     IF       ASCEND                                              IX2084.2
146500              ADD    000002  TO LOGICAL-FILE-REC                  IX2084.2
146600              ELSE                                                IX2084.2
146700              SUBTRACT   000002  FROM  LOGICAL-FILE-REC.          IX2084.2
146800     IF       LOGICAL-FILE-REC  EQUAL  TO XRECORD-NUMBER (1)      IX2084.2
146900              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        IX2084.2
147000     ADD      000001 TO EXCUT-COUNTER-06V00.                      IX2084.2
147100 INX-TEST-003.                                                    IX2084.2
147200     IF       EXCUT-COUNTER-06V00  NOT EQUAL  TO 000010           IX2084.2
147300              MULTIPLY  100  BY   EXCUT-COUNTER-06V00             IX2084.2
147400              ADD  EXCUT-COUNTER-06V00 TO ERROR-COUNTER-06V00.    IX2084.2
147500     IF       ERROR-COUNTER-06V00 EQUAL TO ZERO                   IX2084.2
147600              PERFORM PASS                                        IX2084.2
147700              ELSE                                                IX2084.2
147800              PERFORM   FAIL                                      IX2084.2
147900              MOVE   ZERO  TO CORRECT-N                           IX2084.2
148000              MOVE     ERROR-COUNTER-06V00  TO COMPUTED-N         IX2084.2
148100              MOVE "SEE PROGRAM (START-TEST- ); IX-36" TO RE-MARK.IX2084.2
148200     PERFORM  PRINT-DETAIL.                                       IX2084.2
148300*                                                                 IX2084.2
148400*       EACH TEST IS EXECUTED 10 TIMES.  FOLLOWING THE 10TH       IX2084.2
148500*    EXECUTION A TEST IS MADE ON ERROR-COUNTER-06V00 WHICH IS     IX2084.2
148600*    EXPECTED TO BE ZERO.  IF ERROR-COUNTER-06V00 IS NOT ZERO     IX2084.2
148700*    THE VALUE IN THE COUNTER INDICATES HOW THE EXECUTION FAILED  IX2084.2
148800*    AND THE NUMBER OF TIMES THE UNEXPECTED ACTION OCCURRED       IX2084.2
148900*    DURING THE TEST.  BEFORE THE TEST BEGINS ERROR-COUNTER-06V00 IX2084.2
149000*    IS LOADED WITH THE VALUE 10.  EACH TIME THE CORRECT RECORD   IX2084.2
149100*    WAS MADE AVAILABLE FOLLOWING THE READ, OR AN INVALID KEY     IX2084.2
149200*    CONDITION OCCURRED THAT WAS EXPECTED FOLLOWING A READ OR     IX2084.2
149300*    START, ERROR-COUNTER-06V00 IS DECREMENTED BY 1.              IX2084.2
149400*    FOR EACH ACTION THAT DID NOT OCCUR AS                        IX2084.2
149500*    EXPECTED THE ERROR-COUNTER-06V00 IS INCREMENTED BY THE VALUE IX2084.2
149600*    FOR THE ACTION LISTED BELOW, E.G., VALUE 20003 WOULD INDICATEIX2084.2
149700*    THAT OF THE 10 EXECUTIONS DURING THE TEST (READING LEFT TO   IX2084.2
149800*    RIGHT)  2 INVALID KEY CONDITIONS AND 3 RECORDS RETRIEVED     IX2084.2
149900*    AS A RESULT OF THE READ OR START WAS NOT AS EXPECTED.        IX2084.2
150000*                                                                 IX2084.2
150100*    COMPUTED RESULT                 INDICATED                    IX2084.2
150200*       INCREMENTS                     ACTION                     IX2084.2
150300*                                                                 IX2084.2
150400*       000001          THE RECORD RETREIVED FROM THE FILE        IX2084.2
150500*                       FOLLOWING THE READ WAS NOT THE ONE        IX2084.2
150600*                       EXPECTED.                                 IX2084.2
150700*                                                                 IX2084.2
150800*       000100          INDICATES,BY 10"S THE NUMBER OF TIMES THE IX2084.2
150900*                       TEST WAS EXECUTED.                        IX2084.2
151000*                                                                 IX2084.2
151100*       010000          AN UNEXPECTED INVALID KEY OR AT END       IX2084.2
151200*                       CONDITION OCCURRED.  NOTE - ASSUMPTION    IX2084.2
151300*                       IS THAT THE "USE" STATEMENT IS ONLY       IX2084.2
151400*                       EXECUTED WHEN AN INVALID KEY OR AT END    IX2084.2
151500*                       CONDITION OCCURS AND THE INVALID KEY OR   IX2084.2
151600*                       AT END PHRASE HAS NOT BEEN SPECIFIED.     IX2084.2
151700*                                                                 IX2084.2
151800 START-END.                                                       IX2084.2
151900     CLOSE    IX-FD1.                                             IX2084.2
152000      CLOSE   IX-FS2.                                             IX2084.2
152100 INX-EXIT-003.                                                    IX2084.2
152200     EXIT.                                                        IX2084.2
152300 CCVS-EXIT SECTION.                                               IX2084.2
152400 CCVS-999999.                                                     IX2084.2
152500     GO TO CLOSE-FILES.                                           IX2084.2
      *END-OF,IX208A                                                                  
