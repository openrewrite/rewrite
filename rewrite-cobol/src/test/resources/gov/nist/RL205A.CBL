      *HEADER,COBOL,RL205A                                                            
000100 IDENTIFICATION DIVISION.                                         RL2054.2
000200 PROGRAM-ID.                                                      RL2054.2
000300     RL205A.                                                      RL2054.2
000400****************************************************************  RL2054.2
000500*                                                              *  RL2054.2
000600*    VALIDATION FOR:-                                          *  RL2054.2
000700*                                                              *  RL2054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2054.2
000900*                                                              *  RL2054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2054.2
001100*                                                              *  RL2054.2
001200****************************************************************  RL2054.2
001300*    THE FUNCTION OF THIS PROGRAM IS TO TEST THE PERMISSIBLE SYN- RL2054.2
001400*    TACTICAL CONSTRUCTS OF COBOL ELEMENTS ASSOCIATED WITH LEVEL 2RL2054.2
001500*    OF THE RELATIVE I-O MODULE.  THE ELEMENTS TESTED IN THIS     RL2054.2
001600*    ROUTINE ARE:                                                 RL2054.2
001700*                                                                 RL2054.2
001800*            USE AFTER ERROR PROCEDURE FILE-NAME-1 FILE-NAME-2    RL2054.2
001900*            READ .....  NEXT                                     RL2054.2
002000*            READ .....  NEXT RECORD INTO ....                    RL2054.2
002100*            READ .....  NEXT INTO .....                          RL2054.2
002200*            READ .....  NEXT INTO ..... AT END ....              RL2054.2
002300*            READ .....                                           RL2054.2
002400*            READ .....  INTO .....                               RL2054.2
002500*            READ .....  RECORD                                   RL2054.2
002600*            READ .....  RECORD  INVALID .....                    RL2054.2
002700*            READ .....  RECORD  INVALID KEY ....                 RL2054.2
002800*            START FILE-NAME-2                                    RL2054.2
002900*            START FILE-NAME-2 KEY EQUAL TO ....                  RL2054.2
003000*            START FILE-NAME-2 KEY IS EQUAL TO  ....              RL2054.2
003100*            START FILE-NAME-2 KEY IS EQUAL .....                 RL2054.2
003200*            START FILE-NAME-2 KEY IS =  ......                   RL2054.2
003300*            START FILE-NAME-2 KEY IS GREATER  .....              RL2054.2
003400*            START FILE-NAME-2 KEY  GREATER THAN ....             RL2054.2
003500*            START FILE-NAME-2 KEY IS GREATER  .....              RL2054.2
003600*            START FILE-NAME-2 KEY IS  >   ....                   RL2054.2
003700*            START FILE-NAME-2 KEY     >   ....                   RL2054.2
003800*            START FILE-NAME-2 KEY IS NOT LESS THAN ....          RL2054.2
003900*            START FILE-NAME-2 KEY IS NOT LESS  ....              RL2054.2
004000*            START FILE-NAME-2 KEY    NOT LESS  ....              RL2054.2
004100*            START FILE-NAME-2 KEY IS NOT  <  .....               RL2054.2
004200*            START FILE-NAME-1 KEY IS EQUAL TO INVALID KEY .....  RL2054.2
004300*            START FILE-NAME-1 KEY IS EQUAL TO INVALID ....       RL2054.2
004400*            START FILE-NAME-1 INVALID KEY ....                   RL2054.2
004500*            START FILE-NAME-1 ; INVALID KEY ....                 RL2054.2
004600*            START FILE-NAME-1 KEY EQUAL TO  .....                RL2054.2
004700*            FILE POSITION INDICATOR                              RL2054.2
004800*                                                                 RL2054.2
004900*    EACH ELEMENT TESTED WILL BE EXERCISED SEMANTICALLY BY THIS   RL2054.2
005000*    ROUTINE.                                                     RL2054.2
005100*    FOR A RELATIVE OR INDEXED FILE IN THE DYNAMIC ACCESS MODE,   RL2054.2
005200*    EXECUTION OF AN "OPEN I-O" STATEMENT FOLLOWED BYONE OR       RL2054.2
005300*    MORE "WRITE" STATEMENTS AND THEN A "READ NEXT" STATEMENT     RL2054.2
005400*    WILL CAUSE THE "READ" STATEMENT TO ACCESS THE FIRST RECORD   RL2054.2
005500*    IN THE FILE AT THE TIME OF EXECUTION OF THE "READ"           RL2054.2
005600*    STATEMENT.  SYNTAX TESTS FOR THE "START" STATEMENT ARE       RL2054.2
005700*    ALSO INCLUDED.                                               RL2054.2
005800*                                                                 RL2054.2
005900*                                                                 RL2054.2
006000*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      RL2054.2
006100*                                                                 RL2054.2
006200*             X-21   RELATIVE FILE IMPLEMENTOR-NAME IN ASSGN TO   RL2054.2
006300*                    CLAUSE FOR DATA FILE RL-FS1                  RL2054.2
006400*             X-22   RELATIVE FILE IMPLEMENTOR-NAME IN ASSIGN TO  RL2054.2
006500*                    CLAUSE FOR DATA FILE RL-FD2                  RL2054.2
006600*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          RL2054.2
006700*             X-69   ADDITIONAL VALUE OF PHRASES                  RL2054.2
006800*             X-74   VALUE OF IMPLEMENTOR-NAME                    RL2054.2
006900*             X-75   OBJECT OF VALUE OF CLAUSE FOR FILE RL-FS1    RL2054.2
007000*             X-76   OBJECT OF VALUE OF CLAUSE FOR FILE RL-FD2    RL2054.2
007100*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         RL2054.2
007200*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         RL2054.2
007300*                                                                 RL2054.2
007400*        NOTE:  X-CARDS 69,74,75 AND 76 ARE OPTIONAL              RL2054.2
007500*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   RL2054.2
007600*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  RL2054.2
007700*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   RL2054.2
007800*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   RL2054.2
007900*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    RL2054.2
008000*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       RL2054.2
008100*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        RL2054.2
008200*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    RL2054.2
008300*               THEY ARE AS FOLLOWS                               RL2054.2
008400*                                                                 RL2054.2
008500*                  C  SELECTS X-CARDS 74,75 AND 76                RL2054.2
008600*                  G  SELECTS X-CARDS 69                          RL2054.2
008700*                                                                 RL2054.2
008800*        NOTE:  THERE IS OPTIONAL SOURCE CODE IN THIS PROGRAM     RL2054.2
008900*               FOR THE CONVENIENCE OF THE USER.  THIS OPTIONAL   RL2054.2
009000*               CODE IS IDENTIFIED BY THE LETTER  X IN            RL2054.2
009100*               POSITION 7  OF THE SOURCE LINE.  USE OF           RL2054.2
009200*               SOURCE CODE WITH LETTER X WILL PRINT THE CONTENTS RL2054.2
009300*               OF THE FILES AFTER THE TEST REPORT.               RL2054.2
009400*               IF THE VP-ROUTINE IS USED THE APPROPRIATE         RL2054.2
009500*               SOURCE CODE MAY BE SELECTED BY SPECIFYING THE     RL2054.2
009600*               RESPECTIVE LETTER IN THE "*OPT" VP-ROUTINE CONTROLRL2054.2
009700*               CARD.                                             RL2054.2
009800*                                                                 RL2054.2
009900******************************************************            RL2054.2
010000 ENVIRONMENT DIVISION.                                            RL2054.2
010100 CONFIGURATION SECTION.                                           RL2054.2
010200 SOURCE-COMPUTER.                                                 RL2054.2
010300     XXXXX082.                                                    RL2054.2
010400 OBJECT-COMPUTER.                                                 RL2054.2
010500     XXXXX083.                                                    RL2054.2
010600 INPUT-OUTPUT SECTION.                                            RL2054.2
010700 FILE-CONTROL.                                                    RL2054.2
010800     SELECT PRINT-FILE ASSIGN TO                                  RL2054.2
010900     XXXXX055.                                                    RL2054.2
011000     SELECT   RL-FD1                                              RL2054.2
011100        ASSIGN TO                                                 RL2054.2
011200     XXXXX021                                                     RL2054.2
011300         ORGANIZATION IS RELATIVE                                 RL2054.2
011400       ACCESS MODE IS DYNAMIC                                     RL2054.2
011500       RELATIVE KEY IS RL-FD1-KEY.                                RL2054.2
011600     SELECT   RL-FS2                                              RL2054.2
011700         ASSIGN TO                                                RL2054.2
011800     XXXXX022                                                     RL2054.2
011900        ACCESS MODE IS SEQUENTIAL                                 RL2054.2
012000       RELATIVE KEY IS RL-FS2-KEY                                 RL2054.2
012100         ORGANIZATION IS RELATIVE.                                RL2054.2
012200 DATA DIVISION.                                                   RL2054.2
012300 FILE SECTION.                                                    RL2054.2
012400 FD  PRINT-FILE.                                                  RL2054.2
012500 01  PRINT-REC PICTURE X(120).                                    RL2054.2
012600 01  DUMMY-RECORD PICTURE X(120).                                 RL2054.2
012700 FD  RL-FD1                                                       RL2054.2
012800     RECORD CONTAINS 240 CHARACTERS                               RL2054.2
012900C    VALUE OF                                                     RL2054.2
013000C    XXXXX074                                                     RL2054.2
013100C    IS                                                           RL2054.2
013200C    XXXXX075                                                     RL2054.2
013300*    XXXXX069                                                     RL2054.2
013400     .                                                            RL2054.2
013500 01  RL-FD1R1-F-G-240.                                            RL2054.2
013600     05 RL-FD1-REC-001-120        PICTURE X(120).                 RL2054.2
013700     05 RL-FD1-REC-121-240.                                       RL2054.2
013800     10 FILLER          PICTURE X(8).                             RL2054.2
013900     10 RL-FD1-FILLER.                                            RL2054.2
014000        15 RL-FS1-KEYNUM PICTURE 9(5).                            RL2054.2
014100    10 FILLER           PICTURE X(5).                             RL2054.2
014200     10 FILLER           PICTURE X(19).                           RL2054.2
014300     10 FILLER           PICTURE X(9).                            RL2054.2
014400     10 RL-FD1-FILLER1.                                           RL2054.2
014500        15 RL-FD1-FILLER1NUM      PICTURE 9(5).                   RL2054.2
014600    10 FILLER                    PICTURE 9(5).                    RL2054.2
014700     10 FILLER                    PICTURE X(19).                  RL2054.2
014800     10 FILLER                    PICTURE X(45).                  RL2054.2
014900 FD  RL-FS2                                                       RL2054.2
015000C    VALUE OF                                                     RL2054.2
015100C    XXXXX074                                                     RL2054.2
015200C    IS                                                           RL2054.2
015300C    XXXXX076                                                     RL2054.2
015400*    XXXXX069                                                     RL2054.2
015500     .                                                            RL2054.2
015600 01  RL-FS2R1-F-G-240.                                            RL2054.2
015700     05 RL-FS2-REC-001-120        PICTURE X(120).                 RL2054.2
015800     05 RL-FS2-REC-121-240.                                       RL2054.2
015900        10 FILLER                 PICTURE X(8).                   RL2054.2
016000        10 RL-FS2-FILLER.                                         RL2054.2
016100           15 RL-FS2-KEYNUM       PICTURE 9(5).                   RL2054.2
016200       10 FILLER                 PICTURE 9(5).                    RL2054.2
016300        10 FILLER                 PICTURE X(19).                  RL2054.2
016400        10 FILLER                 PICTURE X(9).                   RL2054.2
016500        10 RL-FS2-FILLER1.                                        RL2054.2
016600           15 RL-FS2-FILLER1NUM   PICTURE 9(5).                   RL2054.2
016700       10 FILLER                 PICTURE 9(5).                    RL2054.2
016800        10 FILLER                 PICTURE X(19).                  RL2054.2
016900        10 FILLER                 PICTURE X(45).                  RL2054.2
017000 WORKING-STORAGE SECTION.                                         RL2054.2
017100 01  WRK-XN-00001    PIC X.                                       RL2054.2
017200 01  WRK-XN-00002    PIC X.                                       RL2054.2
017300 01  RL-FD1-KEY           PICTURE 9(5)  VALUE ZERO.               RL2054.2
017400 01  RL-FS2-KEY           PICTURE 9(5)  VALUE ZERO.               RL2054.2
017500 01  RL-FD1-FILESIZE              PICTURE 9(6) VALUE 300.         RL2054.2
017600 01  RL-FS2-FILESIZE              PICTURE 9(6) VALUE 300.         RL2054.2
017700 01  WRK-RL-FD1-RECKEY-CHAR.                                      RL2054.2
017800  03 WRK-RL-FD1-RECKEY   PIC 9(5)  VALUE ZERO.                    RL2054.2
017900 01  WRK-RL-FS2-RECKEY    PIC 9(5)  VALUE ZERO.                   RL2054.2
018000 01  WRK-RL-FD1-FILLER.                                           RL2054.2
018100     03 WRK-DU-05V00-002          PICTURE 9(5)  VALUE  ZERO.      RL2054.2
018200 01  WRK-RL-FS2-FILLER.                                           RL2054.2
018300     03 WRK-DU-05V00-004          PICTURE 9(5)  VALUE  ZERO.      RL2054.2
018400 01  EXCUT-COUNTER-06V00          PICTURE S9(6) VALUE  ZERO.      RL2054.2
018500 01  INV-KEY-COUNTER              PICTURE S9(6) VALUE  ZERO.      RL2054.2
018600 01  LOGICAL-FILE-REC             PICTURE S9(6) VALUE  ZERO.      RL2054.2
018700 01  ERROR-COUNTER-06V00          PICTURE S9(6) VALUE  ZERO.      RL2054.2
018800 01  ASCEND-DESEND-SWITCH        PICTURE XX   VALUE "UP".         RL2054.2
018900             88 ASCEND VALUE "UP".                                RL2054.2
019000             88 DSCEND VALUE "DN".                                RL2054.2
019100 01  FILE-RECORD-INFORMATION-REC.                                 RL2054.2
019200     03 FILE-RECORD-INFO-SKELETON.                                RL2054.2
019300        05 FILLER                 PICTURE X(48)       VALUE       RL2054.2
019400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2054.2
019500        05 FILLER                 PICTURE X(46)       VALUE       RL2054.2
019600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2054.2
019700        05 FILLER                 PICTURE X(26)       VALUE       RL2054.2
019800             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2054.2
019900        05 FILLER                 PICTURE X(37)       VALUE       RL2054.2
020000             ",RECKEY=                             ".             RL2054.2
020100        05 FILLER                 PICTURE X(38)       VALUE       RL2054.2
020200             ",ALTKEY1=                             ".            RL2054.2
020300        05 FILLER                 PICTURE X(38)       VALUE       RL2054.2
020400             ",ALTKEY2=                             ".            RL2054.2
020500        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2054.2
020600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2054.2
020700        05 FILE-RECORD-INFO-P1-120.                               RL2054.2
020800           07 FILLER              PIC X(5).                       RL2054.2
020900           07 XFILE-NAME           PIC X(6).                      RL2054.2
021000           07 FILLER              PIC X(8).                       RL2054.2
021100           07 XRECORD-NAME         PIC X(6).                      RL2054.2
021200           07 FILLER              PIC X(1).                       RL2054.2
021300           07 REELUNIT-NUMBER     PIC 9(1).                       RL2054.2
021400           07 FILLER              PIC X(7).                       RL2054.2
021500           07 XRECORD-NUMBER       PIC 9(6).                      RL2054.2
021600           07 FILLER              PIC X(6).                       RL2054.2
021700           07 UPDATE-NUMBER       PIC 9(2).                       RL2054.2
021800           07 FILLER              PIC X(5).                       RL2054.2
021900           07 ODO-NUMBER          PIC 9(4).                       RL2054.2
022000           07 FILLER              PIC X(5).                       RL2054.2
022100           07 XPROGRAM-NAME        PIC X(5).                      RL2054.2
022200           07 FILLER              PIC X(7).                       RL2054.2
022300           07 XRECORD-LENGTH       PIC 9(6).                      RL2054.2
022400           07 FILLER              PIC X(7).                       RL2054.2
022500           07 CHARS-OR-RECORDS    PIC X(2).                       RL2054.2
022600           07 FILLER              PIC X(1).                       RL2054.2
022700           07 XBLOCK-SIZE          PIC 9(4).                      RL2054.2
022800           07 FILLER              PIC X(6).                       RL2054.2
022900           07 RECORDS-IN-FILE     PIC 9(6).                       RL2054.2
023000           07 FILLER              PIC X(5).                       RL2054.2
023100           07 XFILE-ORGANIZATION   PIC X(2).                      RL2054.2
023200           07 FILLER              PIC X(6).                       RL2054.2
023300           07 XLABEL-TYPE          PIC X(1).                      RL2054.2
023400        05 FILE-RECORD-INFO-P121-240.                             RL2054.2
023500           07 FILLER              PIC X(8).                       RL2054.2
023600           07 XRECORD-KEY          PIC X(29).                     RL2054.2
023700           07 FILLER              PIC X(9).                       RL2054.2
023800           07 ALTERNATE-KEY1      PIC X(29).                      RL2054.2
023900           07 FILLER              PIC X(9).                       RL2054.2
024000           07 ALTERNATE-KEY2      PIC X(29).                      RL2054.2
024100           07 FILLER              PIC X(7).                       RL2054.2
024200 01  TEST-RESULTS.                                                RL2054.2
024300     02 FILLER                   PIC X      VALUE SPACE.          RL2054.2
024400     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2054.2
024500     02 FILLER                   PIC X      VALUE SPACE.          RL2054.2
024600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2054.2
024700     02 FILLER                   PIC X      VALUE SPACE.          RL2054.2
024800     02  PAR-NAME.                                                RL2054.2
024900       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2054.2
025000       03  PARDOT-X              PIC X      VALUE SPACE.          RL2054.2
025100       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2054.2
025200     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2054.2
025300     02 RE-MARK                  PIC X(61).                       RL2054.2
025400 01  TEST-COMPUTED.                                               RL2054.2
025500     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2054.2
025600     02 FILLER                   PIC X(17)  VALUE                 RL2054.2
025700            "       COMPUTED=".                                   RL2054.2
025800     02 COMPUTED-X.                                               RL2054.2
025900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2054.2
026000     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2054.2
026100                                 PIC -9(9).9(9).                  RL2054.2
026200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2054.2
026300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2054.2
026400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2054.2
026500     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2054.2
026600         04 COMPUTED-18V0                    PIC -9(18).          RL2054.2
026700         04 FILLER                           PIC X.               RL2054.2
026800     03 FILLER PIC X(50) VALUE SPACE.                             RL2054.2
026900 01  TEST-CORRECT.                                                RL2054.2
027000     02 FILLER PIC X(30) VALUE SPACE.                             RL2054.2
027100     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2054.2
027200     02 CORRECT-X.                                                RL2054.2
027300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2054.2
027400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2054.2
027500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2054.2
027600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2054.2
027700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2054.2
027800     03      CR-18V0 REDEFINES CORRECT-A.                         RL2054.2
027900         04 CORRECT-18V0                     PIC -9(18).          RL2054.2
028000         04 FILLER                           PIC X.               RL2054.2
028100     03 FILLER PIC X(2) VALUE SPACE.                              RL2054.2
028200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2054.2
028300 01  CCVS-C-1.                                                    RL2054.2
028400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2054.2
028500-    "SS  PARAGRAPH-NAME                                          RL2054.2
028600-    "       REMARKS".                                            RL2054.2
028700     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2054.2
028800 01  CCVS-C-2.                                                    RL2054.2
028900     02 FILLER                     PIC X        VALUE SPACE.      RL2054.2
029000     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2054.2
029100     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2054.2
029200     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2054.2
029300     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2054.2
029400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2054.2
029500 01  REC-CT                        PIC 99       VALUE ZERO.       RL2054.2
029600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2054.2
029700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2054.2
029800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2054.2
029900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2054.2
030000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2054.2
030100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2054.2
030200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2054.2
030300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2054.2
030400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2054.2
030500 01  CCVS-H-1.                                                    RL2054.2
030600     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2054.2
030700     02  FILLER                    PIC X(42)    VALUE             RL2054.2
030800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2054.2
030900     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2054.2
031000 01  CCVS-H-2A.                                                   RL2054.2
031100   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2054.2
031200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2054.2
031300   02  FILLER                        PIC XXXX   VALUE             RL2054.2
031400     "4.2 ".                                                      RL2054.2
031500   02  FILLER                        PIC X(28)  VALUE             RL2054.2
031600            " COPY - NOT FOR DISTRIBUTION".                       RL2054.2
031700   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2054.2
031800                                                                  RL2054.2
031900 01  CCVS-H-2B.                                                   RL2054.2
032000   02  FILLER                        PIC X(15)  VALUE             RL2054.2
032100            "TEST RESULT OF ".                                    RL2054.2
032200   02  TEST-ID                       PIC X(9).                    RL2054.2
032300   02  FILLER                        PIC X(4)   VALUE             RL2054.2
032400            " IN ".                                               RL2054.2
032500   02  FILLER                        PIC X(12)  VALUE             RL2054.2
032600     " HIGH       ".                                              RL2054.2
032700   02  FILLER                        PIC X(22)  VALUE             RL2054.2
032800            " LEVEL VALIDATION FOR ".                             RL2054.2
032900   02  FILLER                        PIC X(58)  VALUE             RL2054.2
033000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2054.2
033100 01  CCVS-H-3.                                                    RL2054.2
033200     02  FILLER                      PIC X(34)  VALUE             RL2054.2
033300            " FOR OFFICIAL USE ONLY    ".                         RL2054.2
033400     02  FILLER                      PIC X(58)  VALUE             RL2054.2
033500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2054.2
033600     02  FILLER                      PIC X(28)  VALUE             RL2054.2
033700            "  COPYRIGHT   1985 ".                                RL2054.2
033800 01  CCVS-E-1.                                                    RL2054.2
033900     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2054.2
034000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2054.2
034100     02 ID-AGAIN                     PIC X(9).                    RL2054.2
034200     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2054.2
034300 01  CCVS-E-2.                                                    RL2054.2
034400     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2054.2
034500     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2054.2
034600     02 CCVS-E-2-2.                                               RL2054.2
034700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2054.2
034800         03 FILLER                   PIC X      VALUE SPACE.      RL2054.2
034900         03 ENDER-DESC               PIC X(44)  VALUE             RL2054.2
035000            "ERRORS ENCOUNTERED".                                 RL2054.2
035100 01  CCVS-E-3.                                                    RL2054.2
035200     02  FILLER                      PIC X(22)  VALUE             RL2054.2
035300            " FOR OFFICIAL USE ONLY".                             RL2054.2
035400     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2054.2
035500     02  FILLER                      PIC X(58)  VALUE             RL2054.2
035600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2054.2
035700     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2054.2
035800     02 FILLER                       PIC X(15)  VALUE             RL2054.2
035900             " COPYRIGHT 1985".                                   RL2054.2
036000 01  CCVS-E-4.                                                    RL2054.2
036100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2054.2
036200     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2054.2
036300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2054.2
036400     02 FILLER                       PIC X(40)  VALUE             RL2054.2
036500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2054.2
036600 01  XXINFO.                                                      RL2054.2
036700     02 FILLER                       PIC X(19)  VALUE             RL2054.2
036800            "*** INFORMATION ***".                                RL2054.2
036900     02 INFO-TEXT.                                                RL2054.2
037000       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2054.2
037100       04 XXCOMPUTED                 PIC X(20).                   RL2054.2
037200       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2054.2
037300       04 XXCORRECT                  PIC X(20).                   RL2054.2
037400     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2054.2
037500 01  HYPHEN-LINE.                                                 RL2054.2
037600     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2054.2
037700     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2054.2
037800-    "*****************************************".                 RL2054.2
037900     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2054.2
038000-    "******************************".                            RL2054.2
038100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2054.2
038200     "RL205A".                                                    RL2054.2
038300 PROCEDURE DIVISION.                                              RL2054.2
038400 DECLARATIVES.                                                    RL2054.2
038500 USE-RL205-TEST SECTION.                                          RL2054.2
038600     USE      AFTER ERROR PROCEDURE RL-FD1  RL-FS2.               RL2054.2
038700 USE-PAR-001.                                                     RL2054.2
038800     ADD      010000  TO ERROR-COUNTER-06V00.                     RL2054.2
038900 USE-PAR-EXIT.                                                    RL2054.2
039000     EXIT.                                                        RL2054.2
039100 END DECLARATIVES.                                                RL2054.2
039200 CCVS1 SECTION.                                                   RL2054.2
039300 OPEN-FILES.                                                      RL2054.2
039400     OPEN    OUTPUT PRINT-FILE.                                   RL2054.2
039500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2054.2
039600     MOVE    SPACE TO TEST-RESULTS.                               RL2054.2
039700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2054.2
039800     MOVE    ZERO TO REC-SKL-SUB.                                 RL2054.2
039900     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2054.2
040000 CCVS-INIT-FILE.                                                  RL2054.2
040100     ADD     1 TO REC-SKL-SUB.                                    RL2054.2
040200     MOVE    FILE-RECORD-INFO-SKELETON                            RL2054.2
040300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2054.2
040400 CCVS-INIT-EXIT.                                                  RL2054.2
040500     GO TO CCVS1-EXIT.                                            RL2054.2
040600 CLOSE-FILES.                                                     RL2054.2
040700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2054.2
040800 TERMINATE-CCVS.                                                  RL2054.2
040900*    EXIT PROGRAM.                                                RL2054.2
041000*TERMINATE-CALL.                                                  RL2054.2
041100     STOP     RUN.                                                RL2054.2
041200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2054.2
041300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2054.2
041400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2054.2
041500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2054.2
041600     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2054.2
041700 PRINT-DETAIL.                                                    RL2054.2
041800     IF REC-CT NOT EQUAL TO ZERO                                  RL2054.2
041900             MOVE "." TO PARDOT-X                                 RL2054.2
042000             MOVE REC-CT TO DOTVALUE.                             RL2054.2
042100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2054.2
042200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2054.2
042300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2054.2
042400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2054.2
042500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2054.2
042600     MOVE SPACE TO CORRECT-X.                                     RL2054.2
042700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2054.2
042800     MOVE     SPACE TO RE-MARK.                                   RL2054.2
042900 HEAD-ROUTINE.                                                    RL2054.2
043000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2054.2
043100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2054.2
043200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2054.2
043300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2054.2
043400 COLUMN-NAMES-ROUTINE.                                            RL2054.2
043500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2054.2
043600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2054.2
043700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2054.2
043800 END-ROUTINE.                                                     RL2054.2
043900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2054.2
044000 END-RTN-EXIT.                                                    RL2054.2
044100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2054.2
044200 END-ROUTINE-1.                                                   RL2054.2
044300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2054.2
044400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2054.2
044500      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2054.2
044600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2054.2
044700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2054.2
044800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2054.2
044900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2054.2
045000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2054.2
045100  END-ROUTINE-12.                                                 RL2054.2
045200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2054.2
045300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2054.2
045400         MOVE "NO " TO ERROR-TOTAL                                RL2054.2
045500         ELSE                                                     RL2054.2
045600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2054.2
045700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2054.2
045800     PERFORM WRITE-LINE.                                          RL2054.2
045900 END-ROUTINE-13.                                                  RL2054.2
046000     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2054.2
046100         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2054.2
046200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2054.2
046300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2054.2
046400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2054.2
046500      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2054.2
046600          MOVE "NO " TO ERROR-TOTAL                               RL2054.2
046700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2054.2
046800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2054.2
046900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2054.2
047000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2054.2
047100 WRITE-LINE.                                                      RL2054.2
047200     ADD 1 TO RECORD-COUNT.                                       RL2054.2
047300*    IF RECORD-COUNT GREATER 50                                   RL2054.2
047400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2054.2
047500*        MOVE SPACE TO DUMMY-RECORD                               RL2054.2
047600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2054.2
047700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2054.2
047800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2054.2
047900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2054.2
048000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2054.2
048100*        MOVE ZERO TO RECORD-COUNT.                               RL2054.2
048200     PERFORM WRT-LN.                                              RL2054.2
048300 WRT-LN.                                                          RL2054.2
048400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2054.2
048500     MOVE SPACE TO DUMMY-RECORD.                                  RL2054.2
048600 BLANK-LINE-PRINT.                                                RL2054.2
048700     PERFORM WRT-LN.                                              RL2054.2
048800 FAIL-ROUTINE.                                                    RL2054.2
048900     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2054.2
049000            GO TO   FAIL-ROUTINE-WRITE.                           RL2054.2
049100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2054.2
049200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2054.2
049300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2054.2
049400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2054.2
049500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2054.2
049600     GO TO  FAIL-ROUTINE-EX.                                      RL2054.2
049700 FAIL-ROUTINE-WRITE.                                              RL2054.2
049800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2054.2
049900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2054.2
050000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2054.2
050100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2054.2
050200 FAIL-ROUTINE-EX. EXIT.                                           RL2054.2
050300 BAIL-OUT.                                                        RL2054.2
050400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2054.2
050500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2054.2
050600 BAIL-OUT-WRITE.                                                  RL2054.2
050700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2054.2
050800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2054.2
050900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2054.2
051000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2054.2
051100 BAIL-OUT-EX. EXIT.                                               RL2054.2
051200 CCVS1-EXIT.                                                      RL2054.2
051300     EXIT.                                                        RL2054.2
051400 SECT-RL205-0001 SECTION.                                         RL2054.2
051500 REL-INIT-001.                                                    RL2054.2
051600     OPEN     OUTPUT  RL-FD1.                                     RL2054.2
051700     OPEN     OUTPUT  RL-FS2.                                     RL2054.2
051800     MOVE     "RL-FD1"  TO XFILE-NAME (1).                        RL2054.2
051900     MOVE     "R1-F-G"  TO XRECORD-NAME (1).                      RL2054.2
052000     MOVE     ZERO      TO XRECORD-NUMBER (1).                    RL2054.2
052100     MOVE ".XXX."       TO XPROGRAM-NAME (1).                     RL2054.2
052200     MOVE     000240    TO XRECORD-LENGTH (1).                    RL2054.2
052300     MOVE     0001      TO XBLOCK-SIZE (1).                       RL2054.2
052400     MOVE     "RC"      TO CHARS-OR-RECORDS (1).                  RL2054.2
052500     MOVE     "RL"      TO XFILE-ORGANIZATION (1).                RL2054.2
052600     MOVE     "S"       TO XLABEL-TYPE (1).                       RL2054.2
052700     MOVE     000300    TO RL-FD1-FILESIZE.                       RL2054.2
052800     MOVE     000300    TO RECORDS-IN-FILE (1).                   RL2054.2
052900     MOVE     00001     TO WRK-RL-FD1-RECKEY.                     RL2054.2
053000     MOVE     00300     TO WRK-DU-05V00-002.                      RL2054.2
053100     MOVE     ZERO      TO EXCUT-COUNTER-06V00.                   RL2054.2
053200     MOVE     ZERO      TO INV-KEY-COUNTER.                       RL2054.2
053300     MOVE     "REL-TEST-001" TO PAR-NAME.                         RL2054.2
053400     MOVE     ZERO TO REC-CT.                                     RL2054.2
053500     MOVE     "RL-FS2"  TO XFILE-NAME (2).                        RL2054.2
053600     MOVE     "R1-F-G"  TO XRECORD-NAME (2).                      RL2054.2
053700     MOVE     ZERO      TO XRECORD-NUMBER (2).                    RL2054.2
053800     MOVE ".XXX."       TO XPROGRAM-NAME (2).                     RL2054.2
053900     MOVE     000240    TO XRECORD-LENGTH (2).                    RL2054.2
054000     MOVE     0001      TO XBLOCK-SIZE (2).                       RL2054.2
054100     MOVE     "RC"      TO CHARS-OR-RECORDS (2).                  RL2054.2
054200     MOVE     "RL"      TO XFILE-ORGANIZATION (2).                RL2054.2
054300     MOVE     "S"       TO XLABEL-TYPE (2).                       RL2054.2
054400     MOVE     00300     TO RL-FS2-FILESIZE.                       RL2054.2
054500     MOVE     00300     TO RECORDS-IN-FILE (2).                   RL2054.2
054600     MOVE     00001     TO  WRK-RL-FS2-RECKEY.                    RL2054.2
054700     MOVE     00300     TO WRK-DU-05V00-004.                      RL2054.2
054800 REL-TEST-001-R1.                                                 RL2054.2
054900     ADD      0001 TO   XRECORD-NUMBER (1).                       RL2054.2
055000     MOVE     WRK-RL-FD1-RECKEY  TO XRECORD-KEY (1).              RL2054.2
055100     MOVE     WRK-RL-FD1-FILLER  TO ALTERNATE-KEY1 (1).           RL2054.2
055200     MOVE     FILE-RECORD-INFO (1) TO RL-FD1R1-F-G-240.           RL2054.2
055300     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
055400     WRITE    RL-FD1R1-F-G-240                                    RL2054.2
055500              INVALID KEY                                         RL2054.2
055600              ADD       000001 TO INV-KEY-COUNTER.                RL2054.2
055700     ADD      000001  TO EXCUT-COUNTER-06V00.                     RL2054.2
055800     ADD      00001   TO WRK-RL-FD1-RECKEY.                       RL2054.2
055900     SUBTRACT 00001   FROM WRK-DU-05V00-002.                      RL2054.2
056000     IF       XRECORD-NUMBER (1) LESS THAN RL-FD1-FILESIZE        RL2054.2
056100              GO TO REL-TEST-001-R1.                              RL2054.2
056200     CLOSE    RL-FD1.                                             RL2054.2
056300 REL-TEST-001-01.                                                 RL2054.2
056400     MOVE     "CREATE FILE RL-FD1"  TO FEATURE.                   RL2054.2
056500     MOVE     01 TO REC-CT.                                       RL2054.2
056600     IF       EXCUT-COUNTER-06V00  NOT EQUAL TO RL-FD1-FILESIZE   RL2054.2
056700              PERFORM   FAIL                                      RL2054.2
056800              MOVE      RL-FD1-FILESIZE  TO  CORRECT-N            RL2054.2
056900              MOVE      EXCUT-COUNTER-06V00 TO COMPUTED-N         RL2054.2
057000              MOVE      "INCORRECT NUMBER OF WRITES" TO RE-MARK   RL2054.2
057100              PERFORM   PRINT-DETAIL                              RL2054.2
057200              GO TO     REL-INIT-001-R2.                          RL2054.2
057300     IF       INV-KEY-COUNTER NOT EQUAL TO ZERO                   RL2054.2
057400              PERFORM   FAIL                                      RL2054.2
057500              MOVE      INV-KEY-COUNTER TO COMPUTED-N             RL2054.2
057600              MOVE      ZERO TO  CORRECT-N                        RL2054.2
057700              MOVE      "INVALID KEY ON WRITE" TO RE-MARK         RL2054.2
057800              PERFORM   PRINT-DETAIL                              RL2054.2
057900              GO TO     REL-INIT-001-R2.                          RL2054.2
058000*                                                                 RL2054.2
058100*    01                                                           RL2054.2
058200*                                                                 RL2054.2
058300     PERFORM  PASS.                                               RL2054.2
058400     PERFORM  REL-WRITE-001.                                      RL2054.2
058500 REL-INIT-001-R2.                                                 RL2054.2
058600     MOVE     ZERO   TO INV-KEY-COUNTER.                          RL2054.2
058700     MOVE     ZERO   TO EXCUT-COUNTER-06V00.                      RL2054.2
058800 REL-TEST-001-R2.                                                 RL2054.2
058900     ADD      0001  TO  XRECORD-NUMBER (2).                       RL2054.2
059000     MOVE     WRK-RL-FS2-RECKEY  TO  XRECORD-KEY (2).             RL2054.2
059100     MOVE     WRK-RL-FS2-FILLER  TO  ALTERNATE-KEY1 (2).          RL2054.2
059200     MOVE     FILE-RECORD-INFO (2) TO RL-FS2R1-F-G-240.           RL2054.2
059300     WRITE    RL-FS2R1-F-G-240                                    RL2054.2
059400              INVALID KEY                                         RL2054.2
059500              ADD   000001 TO INV-KEY-COUNTER.                    RL2054.2
059600     ADD      000001 TO EXCUT-COUNTER-06V00.                      RL2054.2
059700     ADD      00001  TO  WRK-RL-FS2-RECKEY.                       RL2054.2
059800     SUBTRACT 00001  FROM WRK-DU-05V00-004.                       RL2054.2
059900     IF       XRECORD-NUMBER (2) LESS THAN RL-FS2-FILESIZE        RL2054.2
060000              GO TO  REL-TEST-001-R2.                             RL2054.2
060100     CLOSE    RL-FS2.                                             RL2054.2
060200 REL-TEST-001-02.                                                 RL2054.2
060300     MOVE     "CREATE FILE RL-FS2" TO  FEATURE.                   RL2054.2
060400     MOVE     02  TO REC-CT.                                      RL2054.2
060500     IF       EXCUT-COUNTER-06V00 NOT EQUAL TO RL-FS2-FILESIZE    RL2054.2
060600              PERFORM   FAIL                                      RL2054.2
060700              MOVE      RL-FS2-FILESIZE  TO  CORRECT-N            RL2054.2
060800              MOVE      EXCUT-COUNTER-06V00 TO COMPUTED-N         RL2054.2
060900              MOVE      "INCORRECT NUMBER OF WRITES" TO RE-MARK   RL2054.2
061000              PERFORM   PRINT-DETAIL                              RL2054.2
061100              GO  TO    REL-EXIT-001.                             RL2054.2
061200*                                                                 RL2054.2
061300*    02                                                           RL2054.2
061400*                                                                 RL2054.2
061500     IF       INV-KEY-COUNTER  NOT EQUAL TO ZERO                  RL2054.2
061600              PERFORM   FAIL                                      RL2054.2
061700              MOVE      INV-KEY-COUNTER  TO COMPUTED-N            RL2054.2
061800              MOVE      ZERO TO CORRECT-N                         RL2054.2
061900              MOVE      "INVALID KEY ON WRITE" TO RE-MARK         RL2054.2
062000              PERFORM   PRINT-DETAIL                              RL2054.2
062100              GO TO     REL-EXIT-001.                             RL2054.2
062200     PERFORM  PASS.                                               RL2054.2
062300     PERFORM  REL-WRITE-001.                                      RL2054.2
062400     GO TO    REL-EXIT-001.                                       RL2054.2
062500 REL-WRITE-001.                                                   RL2054.2
062600     PERFORM  PRINT-DETAIL.                                       RL2054.2
062700 REL-DELETE-001.                                                  RL2054.2
062800     PERFORM  DE-LETE.                                            RL2054.2
062900     PERFORM  PRINT-DETAIL.                                       RL2054.2
063000 REL-EXIT-001.                                                    RL2054.2
063100     EXIT.                                                        RL2054.2
063200 REL-INIT-002.                                                    RL2054.2
063300     PERFORM  BLANK-LINE-PRINT.                                   RL2054.2
063400     MOVE     "THE FOLLOWING  TESTS ACCESS A FILE DEFINED AS      RL2054.2
063500-             "ACCESS MODE IS DYNAMIC."  TO  PRINT-REC.           RL2054.2
063600     PERFORM  WRITE-LINE.                                         RL2054.2
063700     PERFORM  BLANK-LINE-PRINT.                                   RL2054.2
063800     MOVE     "READ NEXT"  TO FEATURE.                            RL2054.2
063900     MOVE     ZERO  TO  REC-CT.                                   RL2054.2
064000     MOVE     "REL-TEST-002"  TO  PAR-NAME.                       RL2054.2
064100 REL-INIT-002-R1.                                                 RL2054.2
064200     OPEN  INPUT  RL-FD1.                                         RL2054.2
064300     PERFORM  REL-INIT-002-R.                                     RL2054.2
064400 REL-TEST-002-R1.                                                 RL2054.2
064500     READ     RL-FD1 NEXT.                                        RL2054.2
064600     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
064700     PERFORM  REL-VERIFY-002                                      RL2054.2
064800     IF       EXCUT-COUNTER-06V00 LESS THAN  10                   RL2054.2
064900              GO TO REL-TEST-002-R1.                              RL2054.2
065000     CLOSE    RL-FD1.                                             RL2054.2
065100 REL-TEST-002-01.                                                 RL2054.2
065200     MOVE     01  TO REC-CT.                                      RL2054.2
065300     PERFORM  REL-TEST-002.                                       RL2054.2
065400     GO TO    REL-EXIT-002-01.                                    RL2054.2
065500*                                                                 RL2054.2
065600*    01                                                           RL2054.2
065700*                                                                 RL2054.2
065800 REL-DELETE-002-01.                                               RL2054.2
065900     MOVE     01  TO REC-CT.                                      RL2054.2
066000     PERFORM  DE-LETE.                                            RL2054.2
066100     PERFORM  REL-WRITE-002.                                      RL2054.2
066200 REL-EXIT-002-01.                                                 RL2054.2
066300     EXIT.                                                        RL2054.2
066400 REL-INIT-002-R2.                                                 RL2054.2
066500     PERFORM  REL-INIT-002-R.                                     RL2054.2
066600     OPEN     INPUT     RL-FD1.                                   RL2054.2
066700 REL-TEST-002-R2.                                                 RL2054.2
066800     MOVE     SPACE TO FILE-RECORD-INFO (9).                      RL2054.2
066900     MOVE     SPACE TO FILE-RECORD-INFO (1).                      RL2054.2
067000     READ     RL-FD1   NEXT RECORD                                RL2054.2
067100              INTO   FILE-RECORD-INFO (9).                        RL2054.2
067200     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
067300     PERFORM  REL-VERIFY-002.                                     RL2054.2
067400     IF       XRECORD-NUMBER (9) NOT EQUAL TO LOGICAL-FILE-REC    RL2054.2
067500              ADD   000100  TO ERROR-COUNTER-06V00.               RL2054.2
067600     IF       EXCUT-COUNTER-06V00 LESS THAN 10                    RL2054.2
067700              GO TO  REL-TEST-002-R2.                             RL2054.2
067800     CLOSE    RL-FD1.                                             RL2054.2
067900 REL-TEST-002-02.                                                 RL2054.2
068000     MOVE     02 TO REC-CT.                                       RL2054.2
068100     PERFORM  REL-TEST-002.                                       RL2054.2
068200*                                                                 RL2054.2
068300*    02                                                           RL2054.2
068400*                                                                 RL2054.2
068500     GO TO    REL-EXIT-002-02.                                    RL2054.2
068600 REL-DELETE-002-02.                                               RL2054.2
068700     MOVE     02  TO REC-CT.                                      RL2054.2
068800     PERFORM  DE-LETE.                                            RL2054.2
068900     PERFORM  REL-WRITE-002.                                      RL2054.2
069000 REL-EXIT-002-02.                                                 RL2054.2
069100     EXIT.                                                        RL2054.2
069200 REL-INIT-002-R3.                                                 RL2054.2
069300     OPEN     INPUT  RL-FD1.                                      RL2054.2
069400     PERFORM  REL-INIT-002-R.                                     RL2054.2
069500 REL-TEST-002-R3.                                                 RL2054.2
069600     MOVE     SPACE TO FILE-RECORD-INFO (9).                      RL2054.2
069700     MOVE     SPACE TO FILE-RECORD-INFO (1).                      RL2054.2
069800     READ     RL-FD1  NEXT                                        RL2054.2
069900              INTO   FILE-RECORD-INFO (9).                        RL2054.2
070000     MOVE     RL-FD1R1-F-G-240 TO FILE-RECORD-INFO (1).           RL2054.2
070100     PERFORM  REL-VERIFY-002.                                     RL2054.2
070200     IF       XRECORD-NUMBER (9) NOT EQUAL TO LOGICAL-FILE-REC    RL2054.2
070300              ADD   000100  TO  ERROR-COUNTER-06V00.              RL2054.2
070400     IF       EXCUT-COUNTER-06V00 LESS THAN 10                    RL2054.2
070500              GO TO  REL-TEST-002-R3.                             RL2054.2
070600     CLOSE    RL-FD1.                                             RL2054.2
070700 REL-TEST-002-03.                                                 RL2054.2
070800     MOVE     03 TO REC-CT.                                       RL2054.2
070900     PERFORM  REL-TEST-002.                                       RL2054.2
071000*                                                                 RL2054.2
071100*    03                                                           RL2054.2
071200*                                                                 RL2054.2
071300     GO  TO   REL-EXIT-002-03.                                    RL2054.2
071400 REL-DELETE-002-03.                                               RL2054.2
071500     MOVE     03  TO REC-CT.                                      RL2054.2
071600     PERFORM  DE-LETE.                                            RL2054.2
071700     PERFORM  REL-WRITE-002.                                      RL2054.2
071800 REL-EXIT-002-03.                                                 RL2054.2
071900     EXIT.                                                        RL2054.2
072000 REL-INIT-002-R4.                                                 RL2054.2
072100     OPEN     INPUT  RL-FD1.                                      RL2054.2
072200     PERFORM  REL-INIT-002-R.                                     RL2054.2
072300     MOVE     RL-FD1-FILESIZE TO ERROR-COUNTER-06V00.             RL2054.2
072400     ADD      000001  TO ERROR-COUNTER-06V00.                     RL2054.2
072500 REL-TEST-002-R4.                                                 RL2054.2
072600     MOVE     SPACE TO FILE-RECORD-INFO (9).                      RL2054.2
072700     MOVE     SPACE TO FILE-RECORD-INFO (1).                      RL2054.2
072800     READ     RL-FD1  NEXT INTO  FILE-RECORD-INFO (9) AT END      RL2054.2
072900              SUBTRACT   000001  FROM  ERROR-COUNTER-06V00        RL2054.2
073000             GO TO   REL-TEST-002-04.                             RL2054.2
073100     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
073200     PERFORM  REL-VERIFY-002.                                     RL2054.2
073300     IF      XRECORD-NUMBER (9) NOT EQUAL TO LOGICAL-FILE-REC     RL2054.2
073400             ADD  00100  TO ERROR-COUNTER-06V00.                  RL2054.2
073500     IF       EXCUT-COUNTER-06V00  GREATER THAN RL-FD1-FILESIZE   RL2054.2
073600              NEXT SENTENCE                                       RL2054.2
073700              ELSE                                                RL2054.2
073800              GO TO    REL-TEST-002-R4.                           RL2054.2
073900*                                                                 RL2054.2
074000*    TEST REL-002-04 TESTS THE COBOL CONSTRUCT  "READ FILE-       RL2054.2
074100*    NAME NEXT INTO IDENTIFIER AT END".  THE TEST READS THE FILE  RL2054.2
074200*    SEQUENTIALY VIA THE RELATIVE KEY  (RECORD KEY IS THE KEY OF  RL2054.2
074300*    REFERENCE) UNTIL AN END-OF-FILE CONDITION OCCURS.  A CHECK   RL2054.2
074400*    IS MADE TO VERIFY THAT THE PROPER RECORDS WERE RETRIVED AND  RL2054.2
074500*    THE AT END PATH WAS TAKEN ON THE 301 ST READ.                RL2054.2
074600*                                                                 RL2054.2
074700 REL-TEST-002-04.                                                 RL2054.2
074800     CLOSE   RL-FD1.                                              RL2054.2
074900     MOVE     04   TO REC-CT.                                     RL2054.2
075000     PERFORM  REL-TEST-002.                                       RL2054.2
075100*   .04                                                           RL2054.2
075200     GO TO    REL-EXIT-002-04.                                    RL2054.2
075300 REL-DELETE-002-04.                                               RL2054.2
075400     MOVE     04  TO REC-CT.                                      RL2054.2
075500     PERFORM  DE-LETE.                                            RL2054.2
075600     PERFORM  REL-WRITE-002.                                      RL2054.2
075700 REL-EXIT-002-04.                                                 RL2054.2
075800     EXIT.                                                        RL2054.2
075900 REL-INIT-002-R5.                                                 RL2054.2
076000     OPEN     INPUT  RL-FD1.                                      RL2054.2
076100     PERFORM  REL-INIT-002-R.                                     RL2054.2
076200     MOVE    "READ"  TO FEATURE.                                  RL2054.2
076300     MOVE     ZERO TO WRK-RL-FD1-RECKEY.                          RL2054.2
076400 REL-TEST-002-R5.                                                 RL2054.2
076500     ADD      00005   TO WRK-RL-FD1-RECKEY.                       RL2054.2
076600     ADD      000004  TO LOGICAL-FILE-REC.                        RL2054.2
076700     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
076800     READ RL-FD1.                                                 RL2054.2
076900     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
077000     PERFORM  REL-VERIFY-002.                                     RL2054.2
077100     MOVE     SPACE TO FILE-RECORD-INFO (1).                      RL2054.2
077200     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
077300              GO TO    REL-TEST-002-R5.                           RL2054.2
077400     CLOSE    RL-FD1.                                             RL2054.2
077500 REL-TEST-002-05.                                                 RL2054.2
077600     MOVE     05   TO REC-CT.                                     RL2054.2
077700     PERFORM  REL-TEST-002.                                       RL2054.2
077800*   .05                                                           RL2054.2
077900     GO TO    REL-EXIT-002-05.                                    RL2054.2
078000 REL-DELETE-002-05.                                               RL2054.2
078100     MOVE     05  TO REC-CT.                                      RL2054.2
078200     PERFORM  DE-LETE.                                            RL2054.2
078300     PERFORM  REL-WRITE-002.                                      RL2054.2
078400 REL-EXIT-002-05.                                                 RL2054.2
078500     EXIT.                                                        RL2054.2
078600 REL-INIT-002-R6.                                                 RL2054.2
078700     OPEN     INPUT  RL-FD1.                                      RL2054.2
078800     PERFORM  REL-INIT-002-R.                                     RL2054.2
078900     MOVE     ZERO TO WRK-RL-FD1-RECKEY.                          RL2054.2
079000 REL-TEST-002-R6.                                                 RL2054.2
079100     MOVE     SPACE TO FILE-RECORD-INFO (9).                      RL2054.2
079200     MOVE     SPACE TO FILE-RECORD-INFO (1).                      RL2054.2
079300     ADD      00005   TO WRK-RL-FD1-RECKEY.                       RL2054.2
079400     ADD      000004  TO LOGICAL-FILE-REC.                        RL2054.2
079500     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
079600     READ     RL-FD1  INTO   FILE-RECORD-INFO (9).                RL2054.2
079700     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
079800     PERFORM  REL-VERIFY-002.                                     RL2054.2
079900     IF       XRECORD-NUMBER (9)   NOT EQUAL   TO LOGICAL-FILE-RECRL2054.2
080000              ADD   000100 TO ERROR-COUNTER-06V00.                RL2054.2
080100     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
080200              GO TO    REL-TEST-002-R6.                           RL2054.2
080300     CLOSE    RL-FD1.                                             RL2054.2
080400 REL-TEST-002-06.                                                 RL2054.2
080500     MOVE     06   TO REC-CT.                                     RL2054.2
080600     PERFORM  REL-TEST-002.                                       RL2054.2
080700*   .06                                                           RL2054.2
080800     GO TO    REL-EXIT-002-06.                                    RL2054.2
080900 REL-DELETE-002-06.                                               RL2054.2
081000     MOVE     06  TO REC-CT.                                      RL2054.2
081100     PERFORM  DE-LETE.                                            RL2054.2
081200     PERFORM  REL-WRITE-002.                                      RL2054.2
081300 REL-EXIT-002-06.                                                 RL2054.2
081400     EXIT.                                                        RL2054.2
081500 REL-INIT-002-R7.                                                 RL2054.2
081600     OPEN     INPUT  RL-FD1.                                      RL2054.2
081700     PERFORM  REL-INIT-002-R.                                     RL2054.2
081800     MOVE     ZERO  TO WRK-RL-FD1-RECKEY.                         RL2054.2
081900     MOVE     ZERO  TO LOGICAL-FILE-REC.                          RL2054.2
082000 REL-TEST-002-R7.                                                 RL2054.2
082100     MOVE      SPACE TO FILE-RECORD-INFO (1).                     RL2054.2
082200     ADD      00005   TO WRK-RL-FD1-RECKEY.                       RL2054.2
082300     ADD   0004  TO LOGICAL-FILE-REC.                             RL2054.2
082400     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
082500     READ   RL-FD1 RECORD.                                        RL2054.2
082600     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
082700     PERFORM  REL-VERIFY-002.                                     RL2054.2
082800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
082900              GO TO    REL-TEST-002-R7.                           RL2054.2
083000     CLOSE    RL-FD1.                                             RL2054.2
083100 REL-TEST-002-07.                                                 RL2054.2
083200     MOVE     07   TO REC-CT.                                     RL2054.2
083300     PERFORM  REL-TEST-002.                                       RL2054.2
083400*   .07                                                           RL2054.2
083500     GO TO    REL-EXIT-002-07.                                    RL2054.2
083600 REL-DELETE-002-07.                                               RL2054.2
083700     MOVE     07  TO REC-CT.                                      RL2054.2
083800     PERFORM  DE-LETE.                                            RL2054.2
083900     PERFORM  REL-WRITE-002.                                      RL2054.2
084000 REL-EXIT-002-07.                                                 RL2054.2
084100     EXIT.                                                        RL2054.2
084200 REL-INIT-002-R8.                                                 RL2054.2
084300     OPEN     INPUT  RL-FD1.                                      RL2054.2
084400     PERFORM  REL-INIT-002-R.                                     RL2054.2
084500     MOVE     00301  TO WRK-RL-FD1-RECKEY.                        RL2054.2
084600     MOVE     SPACE TO RL-FD1R1-F-G-240.                          RL2054.2
084700 REL-TEST-002-R8.                                                 RL2054.2
084800     ADD      00005   TO WRK-RL-FD1-RECKEY.                       RL2054.2
084900     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
085000     READ   RL-FD1 RECORD INVALID                                 RL2054.2
085100              SUBTRACT  000001  FROM ERROR-COUNTER-06V00.         RL2054.2
085200     ADD     000001  TO EXCUT-COUNTER-06V00.                      RL2054.2
085300     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
085400              GO TO    REL-TEST-002-R8.                           RL2054.2
085500     CLOSE    RL-FD1.                                             RL2054.2
085600 REL-TEST-002-08.                                                 RL2054.2
085700     MOVE     08   TO REC-CT.                                     RL2054.2
085800     PERFORM  REL-TEST-002.                                       RL2054.2
085900*   .08                                                           RL2054.2
086000     GO TO    REL-EXIT-002-08.                                    RL2054.2
086100 REL-DELETE-002-08.                                               RL2054.2
086200     MOVE     08  TO REC-CT.                                      RL2054.2
086300     PERFORM  DE-LETE.                                            RL2054.2
086400     PERFORM  REL-WRITE-002.                                      RL2054.2
086500 REL-EXIT-002-08.                                                 RL2054.2
086600     EXIT.                                                        RL2054.2
086700 REL-INIT-002-R9.                                                 RL2054.2
086800     OPEN     INPUT  RL-FD1.                                      RL2054.2
086900     PERFORM  REL-INIT-002-R.                                     RL2054.2
087000     MOVE     00301  TO WRK-RL-FD1-RECKEY.                        RL2054.2
087100     MOVE     SPACE TO RL-FD1R1-F-G-240.                          RL2054.2
087200 REL-TEST-002-R9.                                                 RL2054.2
087300     ADD     00004  TO WRK-RL-FD1-RECKEY.                         RL2054.2
087400     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
087500     MOVE     SPACE TO FILE-RECORD-INFO (1).                      RL2054.2
087600     READ   RL-FD1 RECORD INVALID KEY                             RL2054.2
087700              SUBTRACT  000001  FROM ERROR-COUNTER-06V00.         RL2054.2
087800     ADD    00001  TO EXCUT-COUNTER-06V00.                        RL2054.2
087900     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
088000              GO TO    REL-TEST-002-R9.                           RL2054.2
088100     CLOSE    RL-FD1.                                             RL2054.2
088200 REL-TEST-002-09.                                                 RL2054.2
088300     MOVE     09   TO REC-CT.                                     RL2054.2
088400     PERFORM  REL-TEST-002.                                       RL2054.2
088500*   .09                                                           RL2054.2
088600     GO TO    REL-EXIT-002-09.                                    RL2054.2
088700 REL-DELETE-002-09.                                               RL2054.2
088800     MOVE     09  TO REC-CT.                                      RL2054.2
088900     PERFORM  DE-LETE.                                            RL2054.2
089000     PERFORM  REL-WRITE-002.                                      RL2054.2
089100 REL-EXIT-002-09.                                                 RL2054.2
089200     GO TO    REL-EXIT-002.                                       RL2054.2
089300 REL-INIT-002-R.                                                  RL2054.2
089400     MOVE     00010  TO ERROR-COUNTER-06V00.                      RL2054.2
089500     MOVE     ZERO TO EXCUT-COUNTER-06V00.                        RL2054.2
089600     MOVE     ZERO TO INV-KEY-COUNTER.                            RL2054.2
089700     MOVE     ZERO TO LOGICAL-FILE-REC.                           RL2054.2
089800 REL-VERIFY-002.                                                  RL2054.2
089900     ADD      000001    TO  EXCUT-COUNTER-06V00.                  RL2054.2
090000     ADD      000001    TO  LOGICAL-FILE-REC.                     RL2054.2
090100     IF       XRECORD-NUMBER (1) EQUAL TO  LOGICAL-FILE-REC       RL2054.2
090200              SUBTRACT   000001 FROM  ERROR-COUNTER-06V00.        RL2054.2
090300 REL-TEST-002.                                                    RL2054.2
090400     IF       ERROR-COUNTER-06V00 EQUAL TO ZERO                   RL2054.2
090500              PERFORM   PASS                                      RL2054.2
090600              ELSE                                                RL2054.2
090700              PERFORM   FAIL                                      RL2054.2
090800              MOVE      ZERO TO   CORRECT-N                       RL2054.2
090900              MOVE      ERROR-COUNTER-06V00 TO COMPUTED-N         RL2054.2
091000              MOVE      "SEE PARA. - REL-TEST-002" TO RE-MARK.    RL2054.2
091100     PERFORM  REL-WRITE-002.                                      RL2054.2
091200*                                                                 RL2054.2
091300*       EACH TEST IS EXECUTED 10 TIMES EXCEPT FOR  REL-TEST-002-04RL2054.2
091400*    WHICH IS EXECUTED 300 TIMES.  FOLLOWING THE LAST             RL2054.2
091500*    EXECUTION A TEST IS MADE ON ERROR-COUNTER-06V00 WHICH IS     RL2054.2
091600*    EXPECTED TO BE ZERO.  IF ERROR-COUNTER-06V00 IS NOT ZERO     RL2054.2
091700*    THE VALUE IN THE COUNTER INDICATES HOW THE EXECUTION FAILED  RL2054.2
091800*    AND THE NUMBER OF TIMES THE UNEXPECTED ACTION OCCURRED       RL2054.2
091900*    DURING THE TEST.  BEFORE THE TEST BEGINS ERROR-COUNTER-06V00 RL2054.2
092000*    IS INITIALIZED WITH A VALUE.  EACH TIME THE CORRECT RECORD   RL2054.2
092100*    WAS MADE AVAILABLE FOLLOWING THE READ, OR AN INVALID KEY     RL2054.2
092200*    CONDITION OCCURRED THAT WAS EXPECTED FOLLOWING A READ OR     RL2054.2
092300*    START, ERROR-COUNTER-06V00 IS DECREMENTED BY 1.              RL2054.2
092400*    FOR EACH EXECUTION THAT DID NOT PRODUCE THE EXPECTED         RL2054.2
092500*    RESULTS  THE ERROR-COUNTER-06V00 IS INCREMENTED BY THE VALUE RL2054.2
092600*    FOR THE ACTION LISTED BELOW, E.G., VALUE 20003 WOULD INDICATERL2054.2
092700*    THAT OF THE 10 EXECUTIONS DURING THE TEST (READING LEFT TO   RL2054.2
092800*    RIGHT)  2 INVALID KEY CONDITIONS AND 3 RECORDS RETRIEVED     RL2054.2
092900*    AS A RESULT OF THE READ OR START WAS NOT-AS EXPECTED.        RL2054.2
093000*                                                                 RL2054.2
093100*                                                                 RL2054.2
093200*                                                                 RL2054.2
093300*    COMPUTED RESULT                 INDICATED                    RL2054.2
093400*       INCREMENTS                     ACTION                     RL2054.2
093500*                                                                 RL2054.2
093600*       000100          THE  RECORD  FOUND IN THE IDENTIFIER      RL2054.2
093700*                       SPECIFIED IN THE INTO PHRASE OF THE       RL2054.2
093800*                       READ STATEMENT WAS NOT THE RECORD         RL2054.2
093900*                       EXPECTED FOLLOWING EXECUTION OF THE       RL2054.2
094000*                       READ.                                     RL2054.2
094100*                                                                 RL2054.2
094200*       000001          THE RECORD RETREIVED FROM THE FILE        RL2054.2
094300*                       FOLLOWING THE READ WAS NOT THE ONE        RL2054.2
094400*                       EXPECTED.                                 RL2054.2
094500*                                                                 RL2054.2
094600*       010000          AN UNEXPECTED INVALID KEY OR AT END       RL2054.2
094700*                       CONDITION OCCURRED.  NOTE - ASSUMPTION    RL2054.2
094800*                       IS THAT THE "USE" STATEMENT IS ONLY       RL2054.2
094900*                       EXECUTED WHEN AN INVALID KEY OR AT END    RL2054.2
095000*                       CONDITION OCCURS AND THE INVALID KEY OR   RL2054.2
095100*                       AT END PHRASE HAS NOT BEEN SPECIFIED.     RL2054.2
095200*                                                                 RL2054.2
095300 REL-WRITE-002.                                                   RL2054.2
095400     PERFORM  PRINT-DETAIL.                                       RL2054.2
095500 REL-EXIT-002.                                                    RL2054.2
095600     EXIT.                                                        RL2054.2
095700 REL-INIT-003.                                                    RL2054.2
095800     OPEN     INPUT  RL-FD1.                                      RL2054.2
095900     OPEN     INPUT  RL-FS2.                                      RL2054.2
096000     PERFORM  BLANK-LINE-PRINT.                                   RL2054.2
096100     MOVE     "THE FOLLOWING TESTS ACCESS A FILE DEFINE AS        RL2054.2
096200-             "ACCESS MODE IS SEQUENTIAL"  TO PRINT-REC.          RL2054.2
096300     PERFORM  WRITE-LINE.                                         RL2054.2
096400     PERFORM  BLANK-LINE-PRINT.                                   RL2054.2
096500     MOVE     "START  EQUAL TO"  TO FEATURE.                      RL2054.2
096600     MOVE     "REL-TEST-003"  TO PAR-NAME.                        RL2054.2
096700     MOVE     ZERO TO REC-CT.                                     RL2054.2
096800     PERFORM  REL-INIT-003-R.                                     RL2054.2
096900 REL-TEST-003-R1.                                                 RL2054.2
097000     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
097100     MOVE     WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                    RL2054.2
097200     START    RL-FS2.                                             RL2054.2
097300     READ     RL-FS2       RECORD  AT END                         RL2054.2
097400             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
097500              GO TO       REL-TEST-003-01.                        RL2054.2
097600     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
097700     PERFORM  REL-VERIFY-003A.                                    RL2054.2
097800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
097900              GO TO    REL-TEST-003-R1.                           RL2054.2
098000 REL-TEST-003-01.                                                 RL2054.2
098100     MOVE     01   TO REC-CT.                                     RL2054.2
098200     PERFORM  REL-TEST-003.                                       RL2054.2
098300*   .01                                                           RL2054.2
098400     GO TO    REL-EXIT-003-01.                                    RL2054.2
098500 REL-DELETE-003-01.                                               RL2054.2
098600     PERFORM  DE-LETE.                                            RL2054.2
098700     PERFORM  REL-WRITE-003.                                      RL2054.2
098800 REL-EXIT-003-01.                                                 RL2054.2
098900     EXIT.                                                        RL2054.2
099000 REL-INIT-003-R2.                                                 RL2054.2
099100     PERFORM  REL-INIT-003-R.                                     RL2054.2
099200 REL-TEST-003-R2.                                                 RL2054.2
099300     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
099400     MOVE     WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                    RL2054.2
099500     START    RL-FS2  KEY EQUAL TO RL-FS2-KEY.                    RL2054.2
099600     READ     RL-FS2       RECORD  AT END                         RL2054.2
099700             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
099800              GO TO       REL-TEST-003-02.                        RL2054.2
099900     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
100000     PERFORM  REL-VERIFY-003A.                                    RL2054.2
100100     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
100200              GO TO    REL-TEST-003-R2.                           RL2054.2
100300 REL-TEST-003-02.                                                 RL2054.2
100400     MOVE     02   TO REC-CT.                                     RL2054.2
100500     PERFORM  REL-TEST-003.                                       RL2054.2
100600*   .02                                                           RL2054.2
100700     GO TO    REL-EXIT-003-02.                                    RL2054.2
100800 REL-DELETE-003-02.                                               RL2054.2
100900     MOVE     02  TO REC-CT.                                      RL2054.2
101000     PERFORM  DE-LETE.                                            RL2054.2
101100     PERFORM  REL-WRITE-003.                                      RL2054.2
101200 REL-EXIT-003-02.                                                 RL2054.2
101300     EXIT.                                                        RL2054.2
101400 REL-INIT-003-R3.                                                 RL2054.2
101500     PERFORM  REL-INIT-003-R.                                     RL2054.2
101600 REL-TEST-003-R3.                                                 RL2054.2
101700     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
101800     MOVE     WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                    RL2054.2
101900     START    RL-FS2  KEY IS EQUAL TO  RL-FS2-KEY.                RL2054.2
102000     READ     RL-FS2       RECORD  AT END                         RL2054.2
102100             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
102200              GO TO       REL-TEST-003-03.                        RL2054.2
102300     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
102400     PERFORM  REL-VERIFY-003A.                                    RL2054.2
102500     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
102600              GO TO    REL-TEST-003-R3.                           RL2054.2
102700 REL-TEST-003-03.                                                 RL2054.2
102800     MOVE     03   TO REC-CT.                                     RL2054.2
102900     PERFORM  REL-TEST-003.                                       RL2054.2
103000*   .03                                                           RL2054.2
103100     GO TO    REL-EXIT-003-03.                                    RL2054.2
103200 REL-DELETE-003-03.                                               RL2054.2
103300     MOVE     03  TO REC-CT.                                      RL2054.2
103400     PERFORM  DE-LETE.                                            RL2054.2
103500     PERFORM  REL-WRITE-003.                                      RL2054.2
103600 REL-EXIT-003-03.                                                 RL2054.2
103700     EXIT.                                                        RL2054.2
103800 REL-INIT-003-R4.                                                 RL2054.2
103900     PERFORM  REL-INIT-003-R.                                     RL2054.2
104000 REL-TEST-003-R4.                                                 RL2054.2
104100     ADD     00003   TO  WRK-RL-FS2-RECKEY.                       RL2054.2
104200     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
104300     START   RL-FS2  KEY IS EQUAL  RL-FS2-KEY.                    RL2054.2
104400     READ    RL-FS2       RECORD  AT END                          RL2054.2
104500             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
104600             GO TO       REL-TEST-003-04.                         RL2054.2
104700     MOVE    RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).          RL2054.2
104800     PERFORM REL-VERIFY-003A.                                     RL2054.2
104900     IF      EXCUT-COUNTER-06V00  LESS THAN 10                    RL2054.2
105000             GO TO    REL-TEST-003-R4.                            RL2054.2
105100 REL-TEST-003-04.                                                 RL2054.2
105200     MOVE     04   TO REC-CT.                                     RL2054.2
105300     PERFORM  REL-TEST-003.                                       RL2054.2
105400*   .04                                                           RL2054.2
105500     GO TO    REL-EXIT-003-04.                                    RL2054.2
105600 REL-DELETE-003-04.                                               RL2054.2
105700     MOVE     04  TO REC-CT.                                      RL2054.2
105800     PERFORM  DE-LETE.                                            RL2054.2
105900     PERFORM  REL-WRITE-003.                                      RL2054.2
106000 REL-EXIT-003-04.                                                 RL2054.2
106100     EXIT.                                                        RL2054.2
106200 REL-INIT-003-R5.                                                 RL2054.2
106300     PERFORM  REL-INIT-003-R.                                     RL2054.2
106400 REL-TEST-003-R5.                                                 RL2054.2
106500     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
106600     MOVE     WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                    RL2054.2
106700     START    RL-FS2  KEY IS  =  RL-FS2-KEY.                      RL2054.2
106800     READ     RL-FS2       RECORD  AT END                         RL2054.2
106900             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
107000              GO TO       REL-TEST-003-05.                        RL2054.2
107100     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
107200     PERFORM  REL-VERIFY-003A.                                    RL2054.2
107300     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
107400              GO TO    REL-TEST-003-R5.                           RL2054.2
107500 REL-TEST-003-05.                                                 RL2054.2
107600     MOVE     05   TO REC-CT.                                     RL2054.2
107700     PERFORM  REL-TEST-003.                                       RL2054.2
107800*   .05                                                           RL2054.2
107900     GO TO    REL-EXIT-003-05.                                    RL2054.2
108000 REL-DELETE-003-05.                                               RL2054.2
108100     MOVE     05  TO REC-CT.                                      RL2054.2
108200     PERFORM  DE-LETE.                                            RL2054.2
108300     PERFORM  REL-WRITE-003.                                      RL2054.2
108400 REL-EXIT-003-05.                                                 RL2054.2
108500     EXIT.                                                        RL2054.2
108600 REL-INIT-003-R6.                                                 RL2054.2
108700     PERFORM  REL-INIT-003-R.                                     RL2054.2
108800     ADD      000001 TO LOGICAL-FILE-REC.                         RL2054.2
108900     MOVE     "START  GREATER THAN"  TO FEATURE.                  RL2054.2
109000 REL-TEST-003-R6.                                                 RL2054.2
109100     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
109200     MOVE     WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                    RL2054.2
109300     START    RL-FS2  KEY IS GREATER THAN  RL-FS2-KEY.            RL2054.2
109400     READ     RL-FS2       RECORD  AT END                         RL2054.2
109500             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
109600              GO TO       REL-TEST-003-06.                        RL2054.2
109700     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
109800     PERFORM  REL-VERIFY-003A.                                    RL2054.2
109900     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
110000              GO TO    REL-TEST-003-R6.                           RL2054.2
110100 REL-TEST-003-06.                                                 RL2054.2
110200     MOVE     06   TO REC-CT.                                     RL2054.2
110300     PERFORM  REL-TEST-003.                                       RL2054.2
110400*   .06                                                           RL2054.2
110500     GO TO    REL-EXIT-003-06.                                    RL2054.2
110600 REL-DELETE-003-06.                                               RL2054.2
110700     MOVE     06  TO REC-CT.                                      RL2054.2
110800     PERFORM  DE-LETE.                                            RL2054.2
110900     PERFORM  REL-WRITE-003.                                      RL2054.2
111000 REL-EXIT-003-06.                                                 RL2054.2
111100     EXIT.                                                        RL2054.2
111200 REL-INIT-003-R7.                                                 RL2054.2
111300     PERFORM  REL-INIT-003-R.                                     RL2054.2
111400     ADD      000001 TO LOGICAL-FILE-REC.                         RL2054.2
111500 REL-TEST-003-R7.                                                 RL2054.2
111600     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
111700     MOVE     WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                    RL2054.2
111800     START    RL-FS2  KEY GREATER THAN RL-FS2-KEY.                RL2054.2
111900     READ     RL-FS2       RECORD  AT END                         RL2054.2
112000             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
112100              GO TO       REL-TEST-003-07.                        RL2054.2
112200     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
112300     PERFORM  REL-VERIFY-003A.                                    RL2054.2
112400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
112500              GO TO    REL-TEST-003-R7.                           RL2054.2
112600 REL-TEST-003-07.                                                 RL2054.2
112700     MOVE     07   TO REC-CT.                                     RL2054.2
112800     PERFORM  REL-TEST-003.                                       RL2054.2
112900*   .07                                                           RL2054.2
113000     GO TO    REL-EXIT-003-07.                                    RL2054.2
113100 REL-DELETE-003-07.                                               RL2054.2
113200     MOVE     07  TO REC-CT.                                      RL2054.2
113300     PERFORM  DE-LETE.                                            RL2054.2
113400     PERFORM  REL-WRITE-003.                                      RL2054.2
113500 REL-EXIT-003-07.                                                 RL2054.2
113600     EXIT.                                                        RL2054.2
113700 REL-INIT-003-R8.                                                 RL2054.2
113800     PERFORM  REL-INIT-003-R.                                     RL2054.2
113900     ADD      00001  TO LOGICAL-FILE-REC.                         RL2054.2
114000 REL-TEST-003-R8.                                                 RL2054.2
114100     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
114200     MOVE   WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                      RL2054.2
114300     START   RL-FS2 KEY IS GREATER RL-FS2-KEY.                    RL2054.2
114400     READ     RL-FS2       RECORD  AT END                         RL2054.2
114500             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
114600              GO TO       REL-TEST-003-08.                        RL2054.2
114700     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
114800     PERFORM  REL-VERIFY-003A.                                    RL2054.2
114900     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
115000              GO TO    REL-TEST-003-R8.                           RL2054.2
115100 REL-TEST-003-08.                                                 RL2054.2
115200     MOVE     08   TO REC-CT.                                     RL2054.2
115300     PERFORM  REL-TEST-003.                                       RL2054.2
115400*   .08                                                           RL2054.2
115500     GO TO    REL-EXIT-003-08.                                    RL2054.2
115600 REL-DELETE-003-08.                                               RL2054.2
115700     MOVE     08  TO REC-CT.                                      RL2054.2
115800     PERFORM  DE-LETE.                                            RL2054.2
115900     PERFORM  REL-WRITE-003.                                      RL2054.2
116000 REL-EXIT-003-08.                                                 RL2054.2
116100     EXIT.                                                        RL2054.2
116200 REL-INIT-003-R9.                                                 RL2054.2
116300     PERFORM  REL-INIT-003-R.                                     RL2054.2
116400     ADD    00001  TO LOGICAL-FILE-REC.                           RL2054.2
116500 REL-TEST-003-R9.                                                 RL2054.2
116600     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
116700     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
116800     START   RL-FS2 KEY IS > RL-FS2-KEY.                          RL2054.2
116900     READ     RL-FS2       RECORD  AT END                         RL2054.2
117000             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
117100              GO TO       REL-TEST-003-09.                        RL2054.2
117200     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
117300     PERFORM   REL-VERIFY-003A.                                   RL2054.2
117400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
117500              GO TO REL-TEST-003-R9.                              RL2054.2
117600 REL-TEST-003-09.                                                 RL2054.2
117700     MOVE     09   TO REC-CT.                                     RL2054.2
117800     PERFORM  REL-TEST-003.                                       RL2054.2
117900*   .09                                                           RL2054.2
118000     GO TO    REL-EXIT-003-09.                                    RL2054.2
118100 REL-DELETE-003-09.                                               RL2054.2
118200     MOVE     09  TO REC-CT.                                      RL2054.2
118300     PERFORM  DE-LETE.                                            RL2054.2
118400     PERFORM  REL-WRITE-003.                                      RL2054.2
118500 REL-EXIT-003-09.                                                 RL2054.2
118600     EXIT.                                                        RL2054.2
118700 REL-INIT-003-R10.                                                RL2054.2
118800     PERFORM  REL-INIT-003-R.                                     RL2054.2
118900     ADD     00001  TO LOGICAL-FILE-REC.                          RL2054.2
119000 REL-TEST-003-R10.                                                RL2054.2
119100     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
119200     MOVE   WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                      RL2054.2
119300     START   RL-FS2 KEY > RL-FS2-KEY.                             RL2054.2
119400     READ     RL-FS2       RECORD  AT END                         RL2054.2
119500             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
119600              GO TO       REL-TEST-003-10.                        RL2054.2
119700     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
119800     PERFORM  REL-VERIFY-003A.                                    RL2054.2
119900     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
120000              GO TO    REL-TEST-003-R10.                          RL2054.2
120100 REL-TEST-003-10.                                                 RL2054.2
120200     MOVE     10   TO REC-CT.                                     RL2054.2
120300     PERFORM  REL-TEST-003.                                       RL2054.2
120400*   .10                                                           RL2054.2
120500     GO TO    REL-EXIT-003-10.                                    RL2054.2
120600 REL-DELETE-003-10.                                               RL2054.2
120700     MOVE     10  TO REC-CT.                                      RL2054.2
120800     PERFORM  DE-LETE.                                            RL2054.2
120900     PERFORM  REL-WRITE-003.                                      RL2054.2
121000 REL-EXIT-003-10.                                                 RL2054.2
121100     EXIT.                                                        RL2054.2
121200 REL-INIT-003-R11.                                                RL2054.2
121300     MOVE     "START  NOT LESS THAN" TO FEATURE.                  RL2054.2
121400     PERFORM  REL-INIT-003-R.                                     RL2054.2
121500 REL-TEST-003-R11.                                                RL2054.2
121600     ADD      00003   TO  WRK-RL-FS2-RECKEY.                      RL2054.2
121700     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
121800     START   RL-FS2 KEY IS NOT LESS THAN RL-FS2-KEY.              RL2054.2
121900     READ     RL-FS2       RECORD  AT END                         RL2054.2
122000             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
122100              GO TO       REL-TEST-003-11.                        RL2054.2
122200     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
122300     PERFORM  REL-VERIFY-003A.                                    RL2054.2
122400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
122500              GO TO    REL-TEST-003-R11.                          RL2054.2
122600 REL-TEST-003-11.                                                 RL2054.2
122700     MOVE     11   TO REC-CT.                                     RL2054.2
122800     PERFORM  REL-TEST-003.                                       RL2054.2
122900*   .11                                                           RL2054.2
123000     GO TO    REL-EXIT-003-11.                                    RL2054.2
123100 REL-DELETE-003-11.                                               RL2054.2
123200     MOVE     11  TO REC-CT.                                      RL2054.2
123300     PERFORM  DE-LETE.                                            RL2054.2
123400     PERFORM  REL-WRITE-003.                                      RL2054.2
123500 REL-EXIT-003-11.                                                 RL2054.2
123600     EXIT.                                                        RL2054.2
123700 REL-INIT-003-R12.                                                RL2054.2
123800     PERFORM  REL-INIT-003-R.                                     RL2054.2
123900 REL-TEST-003-R12.                                                RL2054.2
124000     ADD     0003 TO  WRK-RL-FS2-RECKEY.                          RL2054.2
124100     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
124200     START  RL-FS2 KEY IS NOT LESS  RL-FS2-KEY.                   RL2054.2
124300     READ     RL-FS2       RECORD  AT END                         RL2054.2
124400             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
124500              GO TO       REL-TEST-003-12.                        RL2054.2
124600     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
124700     PERFORM  REL-VERIFY-003A.                                    RL2054.2
124800     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
124900              GO TO    REL-TEST-003-R12.                          RL2054.2
125000 REL-TEST-003-12.                                                 RL2054.2
125100     MOVE     12   TO REC-CT.                                     RL2054.2
125200     PERFORM  REL-TEST-003.                                       RL2054.2
125300*   .12                                                           RL2054.2
125400     GO TO    REL-EXIT-003-12.                                    RL2054.2
125500 REL-DELETE-003-12.                                               RL2054.2
125600     MOVE     12  TO REC-CT.                                      RL2054.2
125700     PERFORM  DE-LETE.                                            RL2054.2
125800     PERFORM  REL-WRITE-003.                                      RL2054.2
125900 REL-EXIT-003-12.                                                 RL2054.2
126000     EXIT.                                                        RL2054.2
126100 REL-INIT-003-R13.                                                RL2054.2
126200     PERFORM  REL-INIT-003-R.                                     RL2054.2
126300 REL-TEST-003-R13.                                                RL2054.2
126400     ADD     003  TO  WRK-RL-FS2-RECKEY.                          RL2054.2
126500     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
126600     START   RL-FS2  KEY NOT LESS THAN RL-FS2-KEY.                RL2054.2
126700     READ     RL-FS2       RECORD  AT END                         RL2054.2
126800             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
126900              GO TO       REL-TEST-003-13.                        RL2054.2
127000     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
127100     PERFORM  REL-VERIFY-003A.                                    RL2054.2
127200     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
127300              GO TO    REL-TEST-003-R13.                          RL2054.2
127400 REL-TEST-003-13.                                                 RL2054.2
127500     MOVE     13   TO REC-CT.                                     RL2054.2
127600     PERFORM  REL-TEST-003.                                       RL2054.2
127700*   .13                                                           RL2054.2
127800     GO TO    REL-EXIT-003-13.                                    RL2054.2
127900 REL-DELETE-003-13.                                               RL2054.2
128000     MOVE     13  TO REC-CT.                                      RL2054.2
128100     PERFORM  DE-LETE.                                            RL2054.2
128200     PERFORM  REL-WRITE-003.                                      RL2054.2
128300 REL-EXIT-003-13.                                                 RL2054.2
128400     EXIT.                                                        RL2054.2
128500 REL-INIT-003-R14.                                                RL2054.2
128600     PERFORM  REL-INIT-003-R.                                     RL2054.2
128700 REL-TEST-003-R14.                                                RL2054.2
128800     ADD    003 TO  WRK-RL-FS2-RECKEY.                            RL2054.2
128900     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
129000     START   RL-FS2 KEY IS NOT  < RL-FS2-KEY.                     RL2054.2
129100     READ     RL-FS2       RECORD  AT END                         RL2054.2
129200             ADD   010000  TO ERROR-COUNTER-06V00                 RL2054.2
129300              GO TO       REL-TEST-003-14.                        RL2054.2
129400     MOVE     RL-FS2R1-F-G-240  TO  FILE-RECORD-INFO (2).         RL2054.2
129500     PERFORM  REL-VERIFY-003A.                                    RL2054.2
129600     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
129700              GO TO    REL-TEST-003-R14.                          RL2054.2
129800 REL-TEST-003-14.                                                 RL2054.2
129900     MOVE     14   TO REC-CT.                                     RL2054.2
130000     PERFORM  REL-TEST-003.                                       RL2054.2
130100*   .14                                                           RL2054.2
130200     GO TO    REL-EXIT-003-14.                                    RL2054.2
130300 REL-DELETE-003-14.                                               RL2054.2
130400     MOVE     14  TO REC-CT.                                      RL2054.2
130500     PERFORM  DE-LETE.                                            RL2054.2
130600     PERFORM  REL-WRITE-003.                                      RL2054.2
130700 REL-EXIT-003-14.                                                 RL2054.2
130800     EXIT.                                                        RL2054.2
130900 REL-INIT-003-R15.                                                RL2054.2
131000     PERFORM  BLANK-LINE-PRINT.                                   RL2054.2
131100     MOVE     "THE FOLLOWING TESTS ACCESS A FILE DEFINED AS       RL2054.2
131200-             "ACCESS MODE IS DYNAMIC"  TO PRINT-REC.             RL2054.2
131300     PERFORM  WRITE-LINE.                                         RL2054.2
131400     PERFORM  BLANK-LINE-PRINT.                                   RL2054.2
131500     MOVE     "START  EQUAL TO " TO FEATURE.                      RL2054.2
131600     PERFORM  REL-INIT-003-R.                                     RL2054.2
131700 REL-TEST-003-R15.                                                RL2054.2
131800     ADD     0002 TO WRK-RL-FD1-RECKEY.                           RL2054.2
131900     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
132000     START   RL-FD1 KEY IS EQUAL TO RL-FD1-KEY INVALID KEY        RL2054.2
132100               ADD 010000  TO ERROR-COUNTER-06V00.                RL2054.2
132200     READ     RL-FD1  NEXT RECORD  AT END                         RL2054.2
132300              ADD   010000  TO ERROR-COUNTER-06V00                RL2054.2
132400              GO TO       REL-TEST-003-15.                        RL2054.2
132500     MOVE     RL-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).         RL2054.2
132600     PERFORM  REL-VERIFY-003B.                                    RL2054.2
132700     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
132800              GO TO    REL-TEST-003-R15.                          RL2054.2
132900 REL-TEST-003-15.                                                 RL2054.2
133000     MOVE     15   TO REC-CT.                                     RL2054.2
133100     PERFORM  REL-TEST-003.                                       RL2054.2
133200*   .15                                                           RL2054.2
133300     GO TO    REL-EXIT-003-15.                                    RL2054.2
133400 REL-DELETE-003-15.                                               RL2054.2
133500     MOVE     15  TO REC-CT.                                      RL2054.2
133600     PERFORM  DE-LETE.                                            RL2054.2
133700     PERFORM  REL-WRITE-003.                                      RL2054.2
133800 REL-EXIT-003-15.                                                 RL2054.2
133900     EXIT.                                                        RL2054.2
134000 REL-INIT-003-R16.                                                RL2054.2
134100     MOVE     "START  INVALID KEY"  TO FEATURE.                   RL2054.2
134200     PERFORM  REL-INIT-003-R.                                     RL2054.2
134300     MOVE    RL-FD1-FILESIZE TO WRK-RL-FD1-RECKEY.                RL2054.2
134400     MOVE     RL-FD1-FILESIZE  TO  LOGICAL-FILE-REC.              RL2054.2
134500 REL-TEST-003-R16.                                                RL2054.2
134600     ADD    0002  TO WRK-RL-FD1-RECKEY.                           RL2054.2
134700     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
134800     START   RL-FD1 KEY IS EQUAL TO RL-FD1-KEY INVALID            RL2054.2
134900              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        RL2054.2
135000     ADD      000001 TO EXCUT-COUNTER-06V00.                      RL2054.2
135100     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
135200              GO TO    REL-TEST-003-R16.                          RL2054.2
135300 REL-TEST-003-16.                                                 RL2054.2
135400     MOVE     16   TO REC-CT.                                     RL2054.2
135500     PERFORM  REL-TEST-003.                                       RL2054.2
135600*   .16                                                           RL2054.2
135700     GO TO    REL-EXIT-003-16.                                    RL2054.2
135800 REL-DELETE-003-16.                                               RL2054.2
135900     MOVE     16  TO REC-CT.                                      RL2054.2
136000     PERFORM  DE-LETE.                                            RL2054.2
136100     PERFORM  REL-WRITE-003.                                      RL2054.2
136200 REL-EXIT-003-16.                                                 RL2054.2
136300     EXIT.                                                        RL2054.2
136400 REL-INIT-003-R17.                                                RL2054.2
136500     PERFORM  REL-INIT-003-R.                                     RL2054.2
136600     MOVE     RL-FD1-FILESIZE  TO  LOGICAL-FILE-REC.              RL2054.2
136700     MOVE     RL-FD1-FILESIZE  TO  WRK-RL-FD1-RECKEY.             RL2054.2
136800 REL-TEST-003-R17.                                                RL2054.2
136900     ADD      00003   TO WRK-RL-FD1-RECKEY.                       RL2054.2
137000     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
137100     START    RL-FD1   INVALID KEY                                RL2054.2
137200              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        RL2054.2
137300     ADD      000001 TO EXCUT-COUNTER-06V00.                      RL2054.2
137400     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
137500              GO TO    REL-TEST-003-R17.                          RL2054.2
137600 REL-TEST-003-17.                                                 RL2054.2
137700     MOVE     17   TO REC-CT.                                     RL2054.2
137800     PERFORM  REL-TEST-003.                                       RL2054.2
137900*   .17                                                           RL2054.2
138000     GO TO    REL-EXIT-003-17.                                    RL2054.2
138100 REL-DELETE-003-17.                                               RL2054.2
138200     MOVE     17  TO REC-CT.                                      RL2054.2
138300     PERFORM  DE-LETE.                                            RL2054.2
138400     PERFORM  REL-WRITE-003.                                      RL2054.2
138500 REL-EXIT-003-17.                                                 RL2054.2
138600     EXIT.                                                        RL2054.2
138700 REL-INIT-003-R18.                                                RL2054.2
138800     PERFORM  REL-INIT-003-R.                                     RL2054.2
138900     MOVE     RL-FD1-FILESIZE  TO  LOGICAL-FILE-REC.              RL2054.2
139000     MOVE     RL-FD1-FILESIZE  TO  WRK-RL-FD1-RECKEY.             RL2054.2
139100 REL-TEST-003-R18.                                                RL2054.2
139200     ADD      00003   TO WRK-RL-FD1-RECKEY.                       RL2054.2
139300     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
139400     START    RL-FD1  ; INVALID KEY                               RL2054.2
139500              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        RL2054.2
139600     ADD      000001 TO EXCUT-COUNTER-06V00.                      RL2054.2
139700     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
139800              GO TO    REL-TEST-003-R18.                          RL2054.2
139900 REL-TEST-003-18.                                                 RL2054.2
140000     MOVE     18   TO REC-CT.                                     RL2054.2
140100     PERFORM  REL-TEST-003.                                       RL2054.2
140200*   .18                                                           RL2054.2
140300     GO TO    REL-EXIT-003-18.                                    RL2054.2
140400 REL-DELETE-003-18.                                               RL2054.2
140500     MOVE     18  TO REC-CT.                                      RL2054.2
140600     PERFORM  DE-LETE.                                            RL2054.2
140700     PERFORM  REL-WRITE-003.                                      RL2054.2
140800 REL-EXIT-003-18.                                                 RL2054.2
140900     EXIT.                                                        RL2054.2
141000 REL-INIT-003-R19.                                                RL2054.2
141100     PERFORM  REL-INIT-003-R.                                     RL2054.2
141200     MOVE     RL-FD1-FILESIZE  TO  WRK-RL-FD1-RECKEY.             RL2054.2
141300     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
141400 REL-TEST-003-R19.                                                RL2054.2
141500     ADD      000002  TO WRK-RL-FD1-RECKEY.                       RL2054.2
141600     MOVE     WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                    RL2054.2
141700     START    RL-FD1 KEY IS EQUAL TO  RL-FD1-KEY ; INVALID KEY    RL2054.2
141800              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        RL2054.2
141900     ADD      000001  TO EXCUT-COUNTER-06V00.                     RL2054.2
142000     IF       EXCUT-COUNTER-06V00  LESS THAN 10                   RL2054.2
142100              GO TO    REL-TEST-003-R19.                          RL2054.2
142200 REL-TEST-003-19.                                                 RL2054.2
142300     MOVE     19   TO REC-CT.                                     RL2054.2
142400     PERFORM  REL-TEST-003.                                       RL2054.2
142500*   .19                                                           RL2054.2
142600     GO TO    REL-END-003.                                        RL2054.2
142700 REL-DELETE-003-19.                                               RL2054.2
142800     MOVE     19  TO REC-CT.                                      RL2054.2
142900     PERFORM  DE-LETE.                                            RL2054.2
143000     PERFORM  REL-WRITE-003.                                      RL2054.2
143100 REL-EXIT-003-19.                                                 RL2054.2
143200     EXIT.                                                        RL2054.2
143300 REL-INIT-003-R.                                                  RL2054.2
143400     MOVE     ZERO TO LOGICAL-FILE-REC.                           RL2054.2
143500     MOVE     ZERO TO EXCUT-COUNTER-06V00.                        RL2054.2
143600     MOVE     00055  TO WRK-DU-05V00-002.                         RL2054.2
143700     MOVE     00050  TO WRK-DU-05V00-004.                         RL2054.2
143800     MOVE     ZERO TO  WRK-RL-FS2-RECKEY.                         RL2054.2
143900     MOVE    ZERO TO WRK-RL-FD1-RECKEY.                           RL2054.2
144000     MOVE     10  TO ERROR-COUNTER-06V00.                         RL2054.2
144100 REL-VERIFY-003A.                                                 RL2054.2
144200     IF       ASCEND                                              RL2054.2
144300              ADD    000003  TO LOGICAL-FILE-REC                  RL2054.2
144400              ELSE                                                RL2054.2
144500              SUBTRACT   000003  FROM  LOGICAL-FILE-REC.          RL2054.2
144600     IF       LOGICAL-FILE-REC  EQUAL  TO XRECORD-NUMBER (2)      RL2054.2
144700              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        RL2054.2
144800     ADD      000001 TO EXCUT-COUNTER-06V00.                      RL2054.2
144900 REL-VERIFY-003B.                                                 RL2054.2
145000     IF       ASCEND                                              RL2054.2
145100              ADD    000002  TO LOGICAL-FILE-REC                  RL2054.2
145200              ELSE                                                RL2054.2
145300              SUBTRACT   000002  FROM  LOGICAL-FILE-REC.          RL2054.2
145400     IF       LOGICAL-FILE-REC  EQUAL  TO XRECORD-NUMBER (1)      RL2054.2
145500              SUBTRACT   000001  FROM ERROR-COUNTER-06V00.        RL2054.2
145600     ADD      000001 TO EXCUT-COUNTER-06V00.                      RL2054.2
145700 REL-TEST-003.                                                    RL2054.2
145800     IF       EXCUT-COUNTER-06V00  NOT EQUAL  TO 000010           RL2054.2
145900              MULTIPLY  100  BY   EXCUT-COUNTER-06V00             RL2054.2
146000              ADD  EXCUT-COUNTER-06V00 TO ERROR-COUNTER-06V00.    RL2054.2
146100     IF       ERROR-COUNTER-06V00 EQUAL TO ZERO                   RL2054.2
146200              PERFORM PASS                                        RL2054.2
146300              ELSE                                                RL2054.2
146400              PERFORM   FAIL                                      RL2054.2
146500              MOVE   ZERO  TO CORRECT-N                           RL2054.2
146600              MOVE     ERROR-COUNTER-06V00  TO COMPUTED-N         RL2054.2
146700              MOVE    "SEE PARA. - REL-TEST-003"  TO RE-MARK.     RL2054.2
146800     PERFORM   REL-WRITE-003.                                     RL2054.2
146900*                                                                 RL2054.2
147000*       EACH TEST IS EXECUTED 10 TIMES.  FOLLOWING THE 10TH       RL2054.2
147100*    EXECUTION A TEST IS MADE ON ERROR-COUNTER-06V00 WHICH IS     RL2054.2
147200*    EXPECTED TO BE ZERO.  IF ERROR-COUNTER-06V00 IS NOT ZERO     RL2054.2
147300*    THE VALUE IN THE COUNTER INDICATES HOW THE EXECUTION FAILED  RL2054.2
147400*    AND THE NUMBER OF TIMES THE UNEXPECTED ACTION OCCURRED       RL2054.2
147500*    DURING THE TEST.  BEFORE THE TEST BEGINS ERROR-COUNTER-06V00 RL2054.2
147600*    IS LOADED WITH THE VALUE 10.  EACH TIME THE CORRECT RECORD   RL2054.2
147700*    WAS MADE AVAILABLE FOLLOWING THE READ, OR AN INVALID KEY     RL2054.2
147800*    CONDITION OCCURRED THAT WAS EXPECTED FOLLOWING A READ OR     RL2054.2
147900*    START, ERROR-COUNTER-06V00 IS DECREMENTED BY 1.              RL2054.2
148000*    FOR EACH ACTION THAT DID NOT OCCUR AS                        RL2054.2
148100*    EXPECTED THE ERROR-COUNTER-06V00 IS INCREMENTED BY THE VALUE RL2054.2
148200*    FOR THE ACTION LISTED BELOW, E.G., VALUE 20003 WOULD INDICATERL2054.2
148300*    THAT OF THE 10 EXECUTIONS DURING THE TEST (READING LEFT TO   RL2054.2
148400*    RIGHT)  2 INVALID KEY CONDITIONS AND 3 RECORDS RETRIEVED     RL2054.2
148500*    AS A RESULT OF THE READ OR START WAS NOT AS EXPECTED.        RL2054.2
148600*                                                                 RL2054.2
148700*    COMPUTED RESULT                 INDICATED                    RL2054.2
148800*       INCREMENTS                     ACTION                     RL2054.2
148900*                                                                 RL2054.2
149000*       000001          THE RECORD RETREIVED FROM THE FILE        RL2054.2
149100*                       FOLLOWING THE READ WAS NOT THE ONE        RL2054.2
149200*                       EXPECTED.                                 RL2054.2
149300*                                                                 RL2054.2
149400*       000100          INDICATES,BY 10"S THE NUMBER OF TIMES THE RL2054.2
149500*                       TEST WAS EXECUTED.                        RL2054.2
149600*                                                                 RL2054.2
149700*       010000          AN UNEXPECTED INVALID KEY OR AT END       RL2054.2
149800*                       CONDITION OCCURRED.  NOTE - ASSUMPTION    RL2054.2
149900*                       IS THAT THE "USE" STATEMENT IS ONLY       RL2054.2
150000*                       EXECUTED WHEN AN INVALID KEY OR AT END    RL2054.2
150100*                       CONDITION OCCURS AND THE INVALID KEY OR   RL2054.2
150200*                       AT END PHRASE HAS NOT BEEN SPECIFIED.     RL2054.2
150300*                                                                 RL2054.2
150400 REL-WRITE-003.                                                   RL2054.2
150500     PERFORM  PRINT-DETAIL.                                       RL2054.2
150600 REL-END-003.                                                     RL2054.2
150700     CLOSE   RL-FD1.                                              RL2054.2
150800     CLOSE   RL-FS2.                                              RL2054.2
150900 REL-EXIT-003.                                                    RL2054.2
151000     EXIT.                                                        RL2054.2
151100*                                                                 RL2054.2
151200*    THE FOLLOWING SECTION CONTAINS ALL THE NEW TESTS FOR 8X:     RL2054.2
151300*                                                                 RL2054.2
151400 NEW-COBOL-8X-TESTS SECTION.                                      RL2054.2
151500*==========================                                       RL2054.2
151600 REL-8X-INIT-1.                                                   RL2054.2
151700     MOVE   "XVII-70 2.2.37" TO ANSI-REFERENCE.                   RL2054.2
151800     MOVE   "REL-8X-TEST-1"  TO PAR-NAME.                         RL2054.2
151900     OPEN    I-O RL-FD1.                                          RL2054.2
152000* DELETE THE NEXT LINE TO DELETE THE TEST                         RL2054.2
152100*    GO TO REL-8X-INIT-1-BETA.                                    RL2054.2
152200 REL-8X-INIT-1-ALPHA.                                             RL2054.2
152300     GO TO REL-8X-DELETE-1.                                       RL2054.2
152400 REL-8X-INIT-1-BETA.                                              RL2054.2
152500     MOVE    LOW-VALUES TO WRK-RL-FD1-RECKEY-CHAR.                RL2054.2
152600     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
152700     MOVE    301 TO RECORDS-IN-FILE (1).                          RL2054.2
152800     MOVE    FILE-RECORD-INFO (1) TO RL-FD1R1-F-G-240.            RL2054.2
152900     WRITE   RL-FD1R1-F-G-240                                     RL2054.2
153000             INVALID KEY                                          RL2054.2
153100                 MOVE  "INVALID KEY ON WRITING NEW 1ST RECORD"    RL2054.2
153200                     TO RE-MARK                                   RL2054.2
153300                 PERFORM FAIL                                     RL2054.2
153400                 PERFORM PRINT-DETAIL                             RL2054.2
153500                 GO TO   REL-8X-INIT-2.                           RL2054.2
153600     MOVE    302 TO RECORDS-IN-FILE (1).                          RL2054.2
153700     MOVE    FILE-RECORD-INFO (1) TO RL-FD1R1-F-G-240.            RL2054.2
153800     MOVE    ZERO TO RL-FD1-KEY.                                  RL2054.2
153900     WRITE   RL-FD1R1-F-G-240                                     RL2054.2
154000             INVALID KEY                                          RL2054.2
154100                 MOVE   "INVALID KEY ON WRITING NEW 2ND RECORD"   RL2054.2
154200                      TO RE-MARK                                  RL2054.2
154300                 PERFORM FAIL                                     RL2054.2
154400                 PERFORM PRINT-DETAIL                             RL2054.2
154500                 GO TO   REL-8X-INIT-2.                           RL2054.2
154600     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
154700     GO TO   REL-8X-TEST-1-0.                                     RL2054.2
154800 REL-8X-DELETE-1.                                                 RL2054.2
154900     PERFORM DE-LETE.                                             RL2054.2
155000     PERFORM PRINT-DETAIL.                                        RL2054.2
155100     GO TO   REL-8X-INIT-2.                                       RL2054.2
155200 REL-8X-TEST-1-0.                                                 RL2054.2
155300     READ    RL-FD1 INTO FILE-RECORD-INFO (1).                    RL2054.2
155400 REL-8X-TEST-1-1.                                                 RL2054.2
155500     IF      RECORDS-IN-FILE (1) = 301                            RL2054.2
155600             PERFORM PASS                                         RL2054.2
155700             GO TO   REL-8X-WRITE-1.                              RL2054.2
155800     MOVE   "FIRST RECORD JUST INSERTED NOT FOUND" TO RE-MARK.    RL2054.2
155900     PERFORM FAIL.                                                RL2054.2
156000 REL-8X-WRITE-1.                                                  RL2054.2
156100     PERFORM PRINT-DETAIL.                                        RL2054.2
156200*                                                                 RL2054.2
156300 REL-8X-INIT-2.                                                   RL2054.2
156400     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
156500     MOVE   "REL-8X-TEST-2"  TO PAR-NAME.                         RL2054.2
156600     OPEN    I-O RL-FS2.                                          RL2054.2
156700     MOVE    123 TO WRK-RL-FS2-RECKEY.                            RL2054.2
156800     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
156900     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
157000     GO TO   REL-8X-TEST-2-0.                                     RL2054.2
157100 REL-8X-DELETE-2.                                                 RL2054.2
157200     PERFORM DE-LETE.                                             RL2054.2
157300     PERFORM PRINT-DETAIL.                                        RL2054.2
157400     GO TO   REL-8X-INIT-3.                                       RL2054.2
157500 REL-8X-TEST-2-0.                                                 RL2054.2
157600     START   RL-FS2                                               RL2054.2
157700             KEY IS GREATER THAN OR EQUAL TO RL-FS2-KEY.          RL2054.2
157800     READ    RL-FS2  NEXT    INTO FILE-RECORD-INFO (1)            RL2054.2
157900             AT END  MOVE "AT END ENCOUNTERED" TO RE-MARK         RL2054.2
158000                     PERFORM FAIL                                 RL2054.2
158100                     PERFORM PRINT-DETAIL                         RL2054.2
158200                     GO TO   REL-8X-INIT-3.                       RL2054.2
158300 REL-8X-TEST-2-1.                                                 RL2054.2
158400     IF      XRECORD-NUMBER (1) < 123                             RL2054.2
158500             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
158600                  TO RE-MARK                                      RL2054.2
158700             MOVE    123 TO CORRECT-N                             RL2054.2
158800             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
158900             PERFORM FAIL                                         RL2054.2
159000             PERFORM PRINT-DETAIL                                 RL2054.2
159100     ELSE                                                         RL2054.2
159200             PERFORM PASS                                         RL2054.2
159300             PERFORM PRINT-DETAIL.                                RL2054.2
159400*                                                                 RL2054.2
159500 REL-8X-INIT-3.                                                   RL2054.2
159600     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
159700     MOVE   "REL-8X-TEST-3"  TO PAR-NAME.                         RL2054.2
159800     MOVE    154 TO WRK-RL-FS2-RECKEY.                            RL2054.2
159900     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
160000     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
160100     GO TO   REL-8X-TEST-3-0.                                     RL2054.2
160200 REL-8X-DELETE-3.                                                 RL2054.2
160300     PERFORM DE-LETE.                                             RL2054.2
160400     PERFORM PRINT-DETAIL.                                        RL2054.2
160500     GO TO   REL-8X-INIT-4.                                       RL2054.2
160600 REL-8X-TEST-3-0.                                                 RL2054.2
160700     START   RL-FS2                                               RL2054.2
160800             KEY GREATER OR EQUAL TO RL-FS2-KEY.                  RL2054.2
160900     READ    RL-FS2  NEXT    INTO FILE-RECORD-INFO (1)            RL2054.2
161000             AT END  MOVE "AT END ENCOUNTERED" TO RE-MARK         RL2054.2
161100                     PERFORM FAIL                                 RL2054.2
161200                     PERFORM PRINT-DETAIL                         RL2054.2
161300                     GO TO   REL-8X-INIT-4.                       RL2054.2
161400 REL-8X-TEST-3-1.                                                 RL2054.2
161500     IF      XRECORD-NUMBER (1) < 154                             RL2054.2
161600             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
161700                  TO RE-MARK                                      RL2054.2
161800             MOVE    154 TO CORRECT-N                             RL2054.2
161900             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
162000             PERFORM FAIL                                         RL2054.2
162100             PERFORM PRINT-DETAIL                                 RL2054.2
162200     ELSE                                                         RL2054.2
162300             PERFORM PASS                                         RL2054.2
162400             PERFORM PRINT-DETAIL.                                RL2054.2
162500*                                                                 RL2054.2
162600 REL-8X-INIT-4.                                                   RL2054.2
162700     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
162800     MOVE   "REL-8X-TEST-4"  TO PAR-NAME.                         RL2054.2
162900     MOVE    226 TO WRK-RL-FS2-RECKEY.                            RL2054.2
163000     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
163100     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
163200     GO TO   REL-8X-TEST-4-0.                                     RL2054.2
163300 REL-8X-DELETE-4.                                                 RL2054.2
163400     PERFORM DE-LETE.                                             RL2054.2
163500     PERFORM PRINT-DETAIL.                                        RL2054.2
163600     GO TO   REL-8X-INIT-5.                                       RL2054.2
163700 REL-8X-TEST-4-0.                                                 RL2054.2
163800     START   RL-FS2                                               RL2054.2
163900             KEY IS >= RL-FS2-KEY.                                RL2054.2
164000     READ    RL-FS2  NEXT    INTO FILE-RECORD-INFO (1)            RL2054.2
164100             AT END  MOVE "AT END ENCOUNTERED" TO RE-MARK         RL2054.2
164200                     PERFORM FAIL                                 RL2054.2
164300                     PERFORM PRINT-DETAIL                         RL2054.2
164400                     GO TO   REL-8X-INIT-5.                       RL2054.2
164500 REL-8X-TEST-4-1.                                                 RL2054.2
164600     IF      XRECORD-NUMBER (1) < 226                             RL2054.2
164700             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
164800                  TO RE-MARK                                      RL2054.2
164900             MOVE    226 TO CORRECT-N                             RL2054.2
165000             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
165100             PERFORM FAIL                                         RL2054.2
165200             PERFORM PRINT-DETAIL                                 RL2054.2
165300     ELSE                                                         RL2054.2
165400             PERFORM PASS                                         RL2054.2
165500             PERFORM PRINT-DETAIL.                                RL2054.2
165600*                                                                 RL2054.2
165700 REL-8X-INIT-5.                                                   RL2054.2
165800     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
165900     MOVE   "REL-8X-TEST-5"  TO PAR-NAME.                         RL2054.2
166000     MOVE    300 TO WRK-RL-FS2-RECKEY.                            RL2054.2
166100     MOVE    WRK-RL-FS2-RECKEY TO RL-FS2-KEY.                     RL2054.2
166200     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
166300     GO TO   REL-8X-TEST-5-0.                                     RL2054.2
166400 REL-8X-DELETE-5.                                                 RL2054.2
166500     PERFORM DE-LETE.                                             RL2054.2
166600     PERFORM PRINT-DETAIL.                                        RL2054.2
166700     GO TO   REL-8X-INIT-6.                                       RL2054.2
166800 REL-8X-TEST-5-0.                                                 RL2054.2
166900     START   RL-FS2                                               RL2054.2
167000             KEY >=  RL-FS2-KEY.                                  RL2054.2
167100     READ    RL-FS2  NEXT    INTO FILE-RECORD-INFO (1)            RL2054.2
167200             AT END  MOVE "AT END ENCOUNTERED" TO RE-MARK         RL2054.2
167300                     PERFORM FAIL                                 RL2054.2
167400                     PERFORM PRINT-DETAIL                         RL2054.2
167500                     GO TO   REL-8X-INIT-6.                       RL2054.2
167600 REL-8X-TEST-5-1.                                                 RL2054.2
167700     IF      XRECORD-NUMBER (1) < 300                             RL2054.2
167800             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
167900                  TO RE-MARK                                      RL2054.2
168000             MOVE    300 TO CORRECT-N                             RL2054.2
168100             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
168200             PERFORM FAIL                                         RL2054.2
168300             PERFORM PRINT-DETAIL                                 RL2054.2
168400     ELSE                                                         RL2054.2
168500             PERFORM PASS                                         RL2054.2
168600             PERFORM PRINT-DETAIL.                                RL2054.2
168700*                                                                 RL2054.2
168800 REL-8X-INIT-6.                                                   RL2054.2
168900     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
169000     MOVE   "REL-8X-TEST-6"  TO PAR-NAME.                         RL2054.2
169100     MOVE    123 TO WRK-RL-FD1-RECKEY.                            RL2054.2
169200     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
169300     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
169400     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
169500     MOVE    1 TO REC-CT.                                         RL2054.2
169600     GO TO   REL-8X-TEST-6-0.                                     RL2054.2
169700 REL-8X-DELETE-6.                                                 RL2054.2
169800     PERFORM DE-LETE.                                             RL2054.2
169900     PERFORM PRINT-DETAIL.                                        RL2054.2
170000     GO TO   REL-8X-INIT-7.                                       RL2054.2
170100 REL-8X-TEST-6-0.                                                 RL2054.2
170200     START   RL-FD1                                               RL2054.2
170300             KEY IS GREATER THAN OR EQUAL TO RL-FD1-KEY           RL2054.2
170400             NOT INVALID KEY                                      RL2054.2
170500                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
170600 REL-8X-TEST-6-1.                                                 RL2054.2
170700     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
170800             MOVE   "NOT INVALID KEY DID NOT EXECUTE"             RL2054.2
170900                  TO RE-MARK                                      RL2054.2
171000             MOVE   "A"  TO CORRECT-X                             RL2054.2
171100             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
171200             PERFORM FAIL                                         RL2054.2
171300             PERFORM PRINT-DETAIL                                 RL2054.2
171400     ELSE                                                         RL2054.2
171500             PERFORM PASS                                         RL2054.2
171600             PERFORM PRINT-DETAIL.                                RL2054.2
171700     ADD     1 TO REC-CT.                                         RL2054.2
171800 REL-8X-TEST-6-2.                                                 RL2054.2
171900     MOVE   "REL-8X-TEST-6-2" TO PAR-NAME.                        RL2054.2
172000     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
172100     IF      XRECORD-NUMBER (1) < 123                             RL2054.2
172200             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
172300                  TO RE-MARK                                      RL2054.2
172400             PERFORM FAIL                                         RL2054.2
172500             PERFORM PRINT-DETAIL                                 RL2054.2
172600     ELSE                                                         RL2054.2
172700             PERFORM PASS                                         RL2054.2
172800             PERFORM PRINT-DETAIL.                                RL2054.2
172900*                                                                 RL2054.2
173000 REL-8X-INIT-7.                                                   RL2054.2
173100     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
173200     MOVE   "REL-8X-TEST-7"  TO PAR-NAME.                         RL2054.2
173300     MOVE    154 TO WRK-RL-FD1-RECKEY.                            RL2054.2
173400     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
173500     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
173600     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
173700     MOVE    1 TO REC-CT.                                         RL2054.2
173800     GO TO   REL-8X-TEST-7-0.                                     RL2054.2
173900 REL-8X-DELETE-7.                                                 RL2054.2
174000     PERFORM DE-LETE.                                             RL2054.2
174100     PERFORM PRINT-DETAIL.                                        RL2054.2
174200     GO TO   REL-8X-INIT-8.                                       RL2054.2
174300 REL-8X-TEST-7-0.                                                 RL2054.2
174400     START   RL-FD1                                               RL2054.2
174500             KEY GREATER OR EQUAL TO RL-FD1-KEY                   RL2054.2
174600             NOT INVALID KEY                                      RL2054.2
174700                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
174800 REL-8X-TEST-7-1.                                                 RL2054.2
174900     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
175000             MOVE   "NOT INVALID KEY DID NOT EXECUTE"             RL2054.2
175100                  TO RE-MARK                                      RL2054.2
175200             MOVE   "A"  TO CORRECT-X                             RL2054.2
175300             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
175400             PERFORM FAIL                                         RL2054.2
175500             PERFORM PRINT-DETAIL                                 RL2054.2
175600     ELSE                                                         RL2054.2
175700             PERFORM PASS                                         RL2054.2
175800             PERFORM PRINT-DETAIL.                                RL2054.2
175900     ADD     1 TO REC-CT.                                         RL2054.2
176000 REL-8X-TEST-7-2.                                                 RL2054.2
176100     MOVE   "REL-8X-TEST-7-2" TO PAR-NAME.                        RL2054.2
176200     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
176300     IF      XRECORD-NUMBER (1) < 154                             RL2054.2
176400             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
176500                  TO RE-MARK                                      RL2054.2
176600             MOVE    154 TO CORRECT-N                             RL2054.2
176700             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
176800             PERFORM FAIL                                         RL2054.2
176900             PERFORM PRINT-DETAIL                                 RL2054.2
177000     ELSE                                                         RL2054.2
177100             PERFORM PASS                                         RL2054.2
177200             PERFORM PRINT-DETAIL.                                RL2054.2
177300*                                                                 RL2054.2
177400 REL-8X-INIT-8.                                                   RL2054.2
177500     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
177600     MOVE   "REL-8X-TEST-8"  TO PAR-NAME.                         RL2054.2
177700     MOVE    226 TO WRK-RL-FD1-RECKEY.                            RL2054.2
177800     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
177900     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
178000     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
178100     MOVE    1 TO REC-CT.                                         RL2054.2
178200     GO TO   REL-8X-TEST-8-0.                                     RL2054.2
178300 REL-8X-DELETE-8.                                                 RL2054.2
178400     PERFORM DE-LETE.                                             RL2054.2
178500     PERFORM PRINT-DETAIL.                                        RL2054.2
178600     GO TO   REL-8X-INIT-9.                                       RL2054.2
178700 REL-8X-TEST-8-0.                                                 RL2054.2
178800     START   RL-FD1                                               RL2054.2
178900             KEY IS >= RL-FD1-KEY                                 RL2054.2
179000             NOT INVALID KEY                                      RL2054.2
179100                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
179200 REL-8X-TEST-8-1.                                                 RL2054.2
179300     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
179400             MOVE   "NOT INVALID KEY DID NOT EXECUTE"             RL2054.2
179500                  TO RE-MARK                                      RL2054.2
179600             MOVE   "A"  TO CORRECT-X                             RL2054.2
179700             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
179800             PERFORM FAIL                                         RL2054.2
179900             PERFORM PRINT-DETAIL                                 RL2054.2
180000     ELSE                                                         RL2054.2
180100             PERFORM PASS                                         RL2054.2
180200             PERFORM PRINT-DETAIL.                                RL2054.2
180300     ADD     1 TO REC-CT.                                         RL2054.2
180400 REL-8X-TEST-8-2.                                                 RL2054.2
180500     MOVE   "REL-8X-TEST-8-2" TO PAR-NAME.                        RL2054.2
180600     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
180700     IF      XRECORD-NUMBER (1) < 226                             RL2054.2
180800             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
180900                  TO RE-MARK                                      RL2054.2
181000             MOVE    226 TO CORRECT-N                             RL2054.2
181100             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
181200             PERFORM FAIL                                         RL2054.2
181300             PERFORM PRINT-DETAIL                                 RL2054.2
181400     ELSE                                                         RL2054.2
181500             PERFORM PASS                                         RL2054.2
181600             PERFORM PRINT-DETAIL.                                RL2054.2
181700*                                                                 RL2054.2
181800 REL-8X-INIT-9.                                                   RL2054.2
181900     MOVE   "VIII-33 4.7.2"  TO ANSI-REFERENCE.                   RL2054.2
182000     MOVE   "REL-8X-TEST-9"  TO PAR-NAME.                         RL2054.2
182100     MOVE    300 TO WRK-RL-FD1-RECKEY.                            RL2054.2
182200     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
182300     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
182400     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
182500     MOVE    1 TO REC-CT.                                         RL2054.2
182600     GO TO   REL-8X-TEST-9-0.                                     RL2054.2
182700 REL-8X-DELETE-9.                                                 RL2054.2
182800     PERFORM DE-LETE.                                             RL2054.2
182900     PERFORM PRINT-DETAIL.                                        RL2054.2
183000     GO TO   REL-8X-INIT-10.                                      RL2054.2
183100 REL-8X-TEST-9-0.                                                 RL2054.2
183200     START   RL-FD1                                               RL2054.2
183300             KEY >= RL-FD1-KEY                                    RL2054.2
183400             NOT INVALID KEY                                      RL2054.2
183500                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
183600 REL-8X-TEST-9-1.                                                 RL2054.2
183700     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
183800             MOVE   "NOT INVALID KEY DID NOT EXECUTE"             RL2054.2
183900                  TO RE-MARK                                      RL2054.2
184000             MOVE   "A"  TO CORRECT-X                             RL2054.2
184100             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
184200             PERFORM FAIL                                         RL2054.2
184300             PERFORM PRINT-DETAIL                                 RL2054.2
184400     ELSE                                                         RL2054.2
184500             PERFORM PASS                                         RL2054.2
184600             PERFORM PRINT-DETAIL.                                RL2054.2
184700     ADD     1 TO REC-CT.                                         RL2054.2
184800 REL-8X-TEST-9-2.                                                 RL2054.2
184900     MOVE   "REL-8X-TEST-9-2" TO PAR-NAME.                        RL2054.2
185000     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
185100     IF      XRECORD-NUMBER (1) < 300                             RL2054.2
185200             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
185300                  TO RE-MARK                                      RL2054.2
185400             MOVE    300 TO CORRECT-N                             RL2054.2
185500             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
185600             PERFORM FAIL                                         RL2054.2
185700             PERFORM PRINT-DETAIL                                 RL2054.2
185800     ELSE                                                         RL2054.2
185900             PERFORM PASS                                         RL2054.2
186000             PERFORM PRINT-DETAIL.                                RL2054.2
186100*                                                                 RL2054.2
186200 REL-8X-INIT-10.                                                  RL2054.2
186300     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
186400     MOVE   "REL-8X-TEST-10"  TO PAR-NAME.                        RL2054.2
186500     MOVE    200 TO WRK-RL-FD1-RECKEY.                            RL2054.2
186600     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
186700     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
186800     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
186900     MOVE    1 TO REC-CT.                                         RL2054.2
187000     GO TO   REL-8X-TEST-10-0.                                    RL2054.2
187100 REL-8X-DELETE-10.                                                RL2054.2
187200     PERFORM DE-LETE.                                             RL2054.2
187300     PERFORM PRINT-DETAIL.                                        RL2054.2
187400     GO TO   REL-8X-INIT-11.                                      RL2054.2
187500 REL-8X-TEST-10-0.                                                RL2054.2
187600     START   RL-FD1                                               RL2054.2
187700             KEY >= RL-FD1-KEY                                    RL2054.2
187800                 INVALID KEY                                      RL2054.2
187900                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
188000 REL-8X-TEST-10-1.                                                RL2054.2
188100     IF      WRK-XN-00001 NOT = SPACE                             RL2054.2
188200             MOVE   "INVALID KEY SHOULD NOT EXECUTE"              RL2054.2
188300                  TO RE-MARK                                      RL2054.2
188400             MOVE    SPACE TO CORRECT-X                           RL2054.2
188500             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
188600             PERFORM FAIL                                         RL2054.2
188700             PERFORM PRINT-DETAIL                                 RL2054.2
188800     ELSE                                                         RL2054.2
188900             PERFORM PASS                                         RL2054.2
189000             PERFORM PRINT-DETAIL.                                RL2054.2
189100     ADD     1 TO REC-CT.                                         RL2054.2
189200 REL-8X-TEST-10-2.                                                RL2054.2
189300     MOVE   "REL-8X-TEST-10-2" TO PAR-NAME.                       RL2054.2
189400     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
189500     IF      XRECORD-NUMBER (1) < 200                             RL2054.2
189600             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
189700                  TO RE-MARK                                      RL2054.2
189800             MOVE    200 TO CORRECT-N                             RL2054.2
189900             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
190000             PERFORM FAIL                                         RL2054.2
190100             PERFORM PRINT-DETAIL                                 RL2054.2
190200     ELSE                                                         RL2054.2
190300             PERFORM PASS                                         RL2054.2
190400             PERFORM PRINT-DETAIL.                                RL2054.2
190500*                                                                 RL2054.2
190600 REL-8X-INIT-11.                                                  RL2054.2
190700     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
190800     MOVE   "REL-8X-TEST-11"  TO PAR-NAME.                        RL2054.2
190900     MOVE    555 TO WRK-RL-FD1-RECKEY.                            RL2054.2
191000     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
191100     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
191200     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
191300     MOVE    1 TO REC-CT.                                         RL2054.2
191400     GO TO   REL-8X-TEST-11-0.                                    RL2054.2
191500 REL-8X-DELETE-11.                                                RL2054.2
191600     PERFORM DE-LETE.                                             RL2054.2
191700     PERFORM PRINT-DETAIL.                                        RL2054.2
191800     GO TO   REL-8X-INIT-12.                                      RL2054.2
191900 REL-8X-TEST-11-0.                                                RL2054.2
192000     START   RL-FD1                                               RL2054.2
192100             KEY >=  RL-FD1-KEY                                   RL2054.2
192200                 INVALID KEY                                      RL2054.2
192300                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
192400 REL-8X-TEST-11-1.                                                RL2054.2
192500     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
192600             MOVE   "INVALID KEY SHOULD HAVE EXECUTED"            RL2054.2
192700                  TO RE-MARK                                      RL2054.2
192800             MOVE   "A" TO CORRECT-X                              RL2054.2
192900             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
193000             PERFORM FAIL                                         RL2054.2
193100             PERFORM PRINT-DETAIL                                 RL2054.2
193200     ELSE                                                         RL2054.2
193300             PERFORM PASS                                         RL2054.2
193400             PERFORM PRINT-DETAIL.                                RL2054.2
193500*                                                                 RL2054.2
193600 REL-8X-INIT-12.                                                  RL2054.2
193700     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
193800     MOVE   "REL-8X-TEST-12"  TO PAR-NAME.                        RL2054.2
193900     MOVE    027 TO WRK-RL-FD1-RECKEY.                            RL2054.2
194000     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
194100     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
194200     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
194300     MOVE    1 TO REC-CT.                                         RL2054.2
194400     GO TO   REL-8X-TEST-12-0.                                    RL2054.2
194500 REL-8X-DELETE-12.                                                RL2054.2
194600     PERFORM DE-LETE.                                             RL2054.2
194700     PERFORM PRINT-DETAIL.                                        RL2054.2
194800     GO TO   REL-8X-INIT-13.                                      RL2054.2
194900 REL-8X-TEST-12-0.                                                RL2054.2
195000     START   RL-FD1                                               RL2054.2
195100             KEY >= RL-FD1-KEY                                    RL2054.2
195200             NOT INVALID KEY                                      RL2054.2
195300                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
195400 REL-8X-TEST-12-1.                                                RL2054.2
195500     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
195600             MOVE   "NOT INVALID KEY SHOULD HAVE EXECUTED"        RL2054.2
195700                  TO RE-MARK                                      RL2054.2
195800             MOVE   "A" TO CORRECT-X                              RL2054.2
195900             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
196000             PERFORM FAIL                                         RL2054.2
196100             PERFORM PRINT-DETAIL                                 RL2054.2
196200     ELSE                                                         RL2054.2
196300             PERFORM PASS                                         RL2054.2
196400             PERFORM PRINT-DETAIL.                                RL2054.2
196500     ADD     1 TO REC-CT.                                         RL2054.2
196600 REL-8X-TEST-12-2.                                                RL2054.2
196700     MOVE   "REL-8X-TEST-12-2" TO PAR-NAME.                       RL2054.2
196800     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
196900     IF      XRECORD-NUMBER (1) < 27                              RL2054.2
197000             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
197100                  TO RE-MARK                                      RL2054.2
197200             MOVE    27 TO CORRECT-N                              RL2054.2
197300             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
197400             PERFORM FAIL                                         RL2054.2
197500             PERFORM PRINT-DETAIL                                 RL2054.2
197600     ELSE                                                         RL2054.2
197700             PERFORM PASS                                         RL2054.2
197800             PERFORM PRINT-DETAIL.                                RL2054.2
197900*                                                                 RL2054.2
198000 REL-8X-INIT-13.                                                  RL2054.2
198100     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
198200     MOVE   "REL-8X-TEST-13"  TO PAR-NAME.                        RL2054.2
198300     MOVE    555 TO WRK-RL-FD1-RECKEY.                            RL2054.2
198400     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
198500     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
198600     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
198700     MOVE    1 TO REC-CT.                                         RL2054.2
198800     GO TO   REL-8X-TEST-13-0.                                    RL2054.2
198900 REL-8X-DELETE-13.                                                RL2054.2
199000     PERFORM DE-LETE.                                             RL2054.2
199100     PERFORM PRINT-DETAIL.                                        RL2054.2
199200     GO TO   REL-8X-INIT-14.                                      RL2054.2
199300 REL-8X-TEST-13-0.                                                RL2054.2
199400     START   RL-FD1                                               RL2054.2
199500             KEY >= RL-FD1-KEY                                    RL2054.2
199600             NOT INVALID KEY                                      RL2054.2
199700                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
199800 REL-8X-TEST-13-1.                                                RL2054.2
199900     IF      WRK-XN-00001 NOT = SPACE                             RL2054.2
200000             MOVE   "NOT INVALID KEY SHOULD NOT HAVE EXECUTED"    RL2054.2
200100                  TO RE-MARK                                      RL2054.2
200200             MOVE    SPACE TO CORRECT-X                           RL2054.2
200300             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
200400             PERFORM FAIL                                         RL2054.2
200500             PERFORM PRINT-DETAIL                                 RL2054.2
200600     ELSE                                                         RL2054.2
200700             PERFORM PASS                                         RL2054.2
200800             PERFORM PRINT-DETAIL.                                RL2054.2
200900*                                                                 RL2054.2
201000 REL-8X-INIT-14.                                                  RL2054.2
201100     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
201200     MOVE   "REL-8X-TEST-14"  TO PAR-NAME.                        RL2054.2
201300     MOVE    101 TO WRK-RL-FD1-RECKEY.                            RL2054.2
201400     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
201500     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
201600     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
201700     MOVE    1 TO REC-CT.                                         RL2054.2
201800     GO TO   REL-8X-TEST-14-0.                                    RL2054.2
201900 REL-8X-DELETE-14.                                                RL2054.2
202000     PERFORM DE-LETE.                                             RL2054.2
202100     PERFORM PRINT-DETAIL.                                        RL2054.2
202200     GO TO   REL-8X-INIT-15.                                      RL2054.2
202300 REL-8X-TEST-14-0.                                                RL2054.2
202400     START   RL-FD1                                               RL2054.2
202500             KEY >= RL-FD1-KEY                                    RL2054.2
202600                 INVALID KEY                                      RL2054.2
202700                 MOVE "B" TO WRK-XN-00001                         RL2054.2
202800             NOT INVALID KEY                                      RL2054.2
202900                 MOVE "A" TO WRK-XN-00001.                        RL2054.2
203000 REL-8X-TEST-14-1.                                                RL2054.2
203100     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
203200             MOVE   "NOT INVALID KEY SHOULD HAVE EXECUTED"        RL2054.2
203300                  TO RE-MARK                                      RL2054.2
203400             MOVE   "A" TO CORRECT-X                              RL2054.2
203500             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
203600             PERFORM FAIL                                         RL2054.2
203700             PERFORM PRINT-DETAIL                                 RL2054.2
203800     ELSE                                                         RL2054.2
203900             PERFORM PASS                                         RL2054.2
204000             PERFORM PRINT-DETAIL.                                RL2054.2
204100     ADD     1 TO REC-CT.                                         RL2054.2
204200 REL-8X-TEST-14-2.                                                RL2054.2
204300     MOVE   "REL-8X-TEST-14-2" TO PAR-NAME.                       RL2054.2
204400     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
204500     IF      XRECORD-NUMBER (1) < 101                             RL2054.2
204600             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
204700                  TO RE-MARK                                      RL2054.2
204800             MOVE    101 TO CORRECT-N                             RL2054.2
204900             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
205000             PERFORM FAIL                                         RL2054.2
205100             PERFORM PRINT-DETAIL                                 RL2054.2
205200     ELSE                                                         RL2054.2
205300             PERFORM PASS                                         RL2054.2
205400             PERFORM PRINT-DETAIL.                                RL2054.2
205500*                                                                 RL2054.2
205600 REL-8X-INIT-15.                                                  RL2054.2
205700     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
205800     MOVE   "REL-8X-TEST-15"  TO PAR-NAME.                        RL2054.2
205900     MOVE    666 TO WRK-RL-FD1-RECKEY.                            RL2054.2
206000     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
206100     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
206200     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
206300     MOVE    1 TO REC-CT.                                         RL2054.2
206400     GO TO   REL-8X-TEST-15-0.                                    RL2054.2
206500 REL-8X-DELETE-15.                                                RL2054.2
206600     PERFORM DE-LETE.                                             RL2054.2
206700     PERFORM PRINT-DETAIL.                                        RL2054.2
206800     GO TO   REL-8X-INIT-16.                                      RL2054.2
206900 REL-8X-TEST-15-0.                                                RL2054.2
207000     START   RL-FD1                                               RL2054.2
207100             KEY >= RL-FD1-KEY                                    RL2054.2
207200                 INVALID KEY                                      RL2054.2
207300                 MOVE "A" TO WRK-XN-00001                         RL2054.2
207400             NOT INVALID KEY                                      RL2054.2
207500                 MOVE "B" TO WRK-XN-00001.                        RL2054.2
207600 REL-8X-TEST-15-1.                                                RL2054.2
207700     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
207800             MOVE   "INVALID KEY SHOULD HAVE EXECUTED"            RL2054.2
207900                  TO RE-MARK                                      RL2054.2
208000             MOVE   "A" TO CORRECT-X                              RL2054.2
208100             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
208200             PERFORM FAIL                                         RL2054.2
208300             PERFORM PRINT-DETAIL                                 RL2054.2
208400     ELSE                                                         RL2054.2
208500             PERFORM PASS                                         RL2054.2
208600             PERFORM PRINT-DETAIL.                                RL2054.2
208700*                                                                 RL2054.2
208800 REL-8X-INIT-16.                                                  RL2054.2
208900     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
209000     MOVE   "REL-8X-TEST-16"  TO PAR-NAME.                        RL2054.2
209100     MOVE    200 TO WRK-RL-FD1-RECKEY.                            RL2054.2
209200     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
209300     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
209400     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
209500     MOVE    SPACE  TO WRK-XN-00002.                              RL2054.2
209600     MOVE    1 TO REC-CT.                                         RL2054.2
209700     GO TO   REL-8X-TEST-16-0.                                    RL2054.2
209800 REL-8X-DELETE-16.                                                RL2054.2
209900     PERFORM DE-LETE.                                             RL2054.2
210000     PERFORM PRINT-DETAIL.                                        RL2054.2
210100     GO TO   REL-8X-INIT-17.                                      RL2054.2
210200 REL-8X-TEST-16-0.                                                RL2054.2
210300     START   RL-FD1                                               RL2054.2
210400             KEY >= RL-FD1-KEY                                    RL2054.2
210500                 INVALID KEY                                      RL2054.2
210600                 MOVE "A" TO WRK-XN-00001                         RL2054.2
210700     END-START                                                    RL2054.2
210800     MOVE   "Z" TO WRK-XN-00002.                                  RL2054.2
210900 REL-8X-TEST-16-1.                                                RL2054.2
211000     IF      WRK-XN-00001 NOT = SPACE                             RL2054.2
211100             MOVE   "INVALID KEY SHOULD NOT HAVE EXECUTED"        RL2054.2
211200                  TO RE-MARK                                      RL2054.2
211300             MOVE    SPACE TO CORRECT-X                           RL2054.2
211400             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
211500             PERFORM FAIL                                         RL2054.2
211600             PERFORM PRINT-DETAIL                                 RL2054.2
211700     ELSE                                                         RL2054.2
211800             PERFORM PASS                                         RL2054.2
211900             PERFORM PRINT-DETAIL.                                RL2054.2
212000     ADD     1 TO REC-CT.                                         RL2054.2
212100 REL-8X-TEST-16-2.                                                RL2054.2
212200     MOVE   "REL-8X-TEST-16-2" TO PAR-NAME.                       RL2054.2
212300     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
212400     IF      XRECORD-NUMBER (1) < 200                             RL2054.2
212500             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
212600                  TO RE-MARK                                      RL2054.2
212700             MOVE    200 TO CORRECT-N                             RL2054.2
212800             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
212900             PERFORM FAIL                                         RL2054.2
213000             PERFORM PRINT-DETAIL                                 RL2054.2
213100     ELSE                                                         RL2054.2
213200             PERFORM PASS                                         RL2054.2
213300             PERFORM PRINT-DETAIL.                                RL2054.2
213400     ADD     1 TO REC-CT.                                         RL2054.2
213500 REL-8X-TEST-16-3.                                                RL2054.2
213600     MOVE   "REL-8X-TEST-16-3" TO PAR-NAME.                       RL2054.2
213700     IF      WRK-XN-00002 NOT = "Z"                               RL2054.2
213800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL2054.2
213900             MOVE   "Z"  TO CORRECT-X                             RL2054.2
214000             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
214100             PERFORM FAIL                                         RL2054.2
214200             PERFORM PRINT-DETAIL                                 RL2054.2
214300     ELSE                                                         RL2054.2
214400             PERFORM PASS                                         RL2054.2
214500             PERFORM PRINT-DETAIL.                                RL2054.2
214600*                                                                 RL2054.2
214700 REL-8X-INIT-17.                                                  RL2054.2
214800     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
214900     MOVE   "REL-8X-TEST-17"  TO PAR-NAME.                        RL2054.2
215000     MOVE    555 TO WRK-RL-FD1-RECKEY.                            RL2054.2
215100     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
215200     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
215300     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
215400     MOVE    SPACE  TO WRK-XN-00002.                              RL2054.2
215500     MOVE    1 TO REC-CT.                                         RL2054.2
215600     GO TO   REL-8X-TEST-17-0.                                    RL2054.2
215700 REL-8X-DELETE-17.                                                RL2054.2
215800     PERFORM DE-LETE.                                             RL2054.2
215900     PERFORM PRINT-DETAIL.                                        RL2054.2
216000     GO TO   REL-8X-INIT-18.                                      RL2054.2
216100 REL-8X-TEST-17-0.                                                RL2054.2
216200     START   RL-FD1                                               RL2054.2
216300             KEY >= RL-FD1-KEY                                    RL2054.2
216400                 INVALID KEY                                      RL2054.2
216500                 MOVE "A" TO WRK-XN-00001                         RL2054.2
216600     END-START                                                    RL2054.2
216700     MOVE   "Z" TO WRK-XN-00002.                                  RL2054.2
216800 REL-8X-TEST-17-1.                                                RL2054.2
216900     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
217000             MOVE   "INVALID KEY SHOULD HAVE EXECUTED"            RL2054.2
217100                  TO RE-MARK                                      RL2054.2
217200             MOVE   "A" TO CORRECT-X                              RL2054.2
217300             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
217400             PERFORM FAIL                                         RL2054.2
217500             PERFORM PRINT-DETAIL                                 RL2054.2
217600     ELSE                                                         RL2054.2
217700             PERFORM PASS                                         RL2054.2
217800             PERFORM PRINT-DETAIL.                                RL2054.2
217900     ADD     1 TO REC-CT.                                         RL2054.2
218000 REL-8X-TEST-17-2.                                                RL2054.2
218100     MOVE   "REL-8X-TEST-17-2" TO PAR-NAME.                       RL2054.2
218200     IF      WRK-XN-00002 NOT = "Z"                               RL2054.2
218300             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL2054.2
218400             MOVE   "Z"  TO CORRECT-X                             RL2054.2
218500             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
218600             PERFORM FAIL                                         RL2054.2
218700             PERFORM PRINT-DETAIL                                 RL2054.2
218800     ELSE                                                         RL2054.2
218900             PERFORM PASS                                         RL2054.2
219000             PERFORM PRINT-DETAIL.                                RL2054.2
219100*                                                                 RL2054.2
219200 REL-8X-INIT-18.                                                  RL2054.2
219300     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
219400     MOVE   "REL-8X-TEST-18"  TO PAR-NAME.                        RL2054.2
219500     MOVE    027 TO WRK-RL-FD1-RECKEY.                            RL2054.2
219600     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
219700     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
219800     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
219900     MOVE    SPACE  TO WRK-XN-00002.                              RL2054.2
220000     MOVE    1 TO REC-CT.                                         RL2054.2
220100     GO TO   REL-8X-TEST-18-0.                                    RL2054.2
220200 REL-8X-DELETE-18.                                                RL2054.2
220300     PERFORM DE-LETE.                                             RL2054.2
220400     PERFORM PRINT-DETAIL.                                        RL2054.2
220500     GO TO   REL-8X-INIT-19.                                      RL2054.2
220600 REL-8X-TEST-18-0.                                                RL2054.2
220700     START   RL-FD1                                               RL2054.2
220800             KEY >= RL-FD1-KEY                                    RL2054.2
220900             NOT INVALID KEY                                      RL2054.2
221000                 MOVE "A" TO WRK-XN-00001                         RL2054.2
221100     END-START                                                    RL2054.2
221200     MOVE   "Z" TO WRK-XN-00002.                                  RL2054.2
221300 REL-8X-TEST-18-1.                                                RL2054.2
221400     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
221500             MOVE   "NOT INVALID KEY SHOULD HAVE EXECUTED"        RL2054.2
221600                  TO RE-MARK                                      RL2054.2
221700             MOVE   "A" TO CORRECT-X                              RL2054.2
221800             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
221900             PERFORM FAIL                                         RL2054.2
222000             PERFORM PRINT-DETAIL                                 RL2054.2
222100     ELSE                                                         RL2054.2
222200             PERFORM PASS                                         RL2054.2
222300             PERFORM PRINT-DETAIL.                                RL2054.2
222400     ADD     1 TO REC-CT.                                         RL2054.2
222500 REL-8X-TEST-18-2.                                                RL2054.2
222600     MOVE   "REL-8X-TEST-18-2" TO PAR-NAME.                       RL2054.2
222700     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
222800     IF      XRECORD-NUMBER (1) < 27                              RL2054.2
222900             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
223000                  TO RE-MARK                                      RL2054.2
223100             MOVE    27 TO CORRECT-N                              RL2054.2
223200             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
223300             PERFORM FAIL                                         RL2054.2
223400             PERFORM PRINT-DETAIL                                 RL2054.2
223500     ELSE                                                         RL2054.2
223600             PERFORM PASS                                         RL2054.2
223700             PERFORM PRINT-DETAIL.                                RL2054.2
223800     ADD     1 TO REC-CT.                                         RL2054.2
223900 REL-8X-TEST-18-3.                                                RL2054.2
224000     MOVE   "REL-8X-TEST-18-3" TO PAR-NAME.                       RL2054.2
224100     IF      WRK-XN-00002 NOT = "Z"                               RL2054.2
224200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL2054.2
224300             MOVE   "Z"  TO CORRECT-X                             RL2054.2
224400             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
224500             PERFORM FAIL                                         RL2054.2
224600             PERFORM PRINT-DETAIL                                 RL2054.2
224700     ELSE                                                         RL2054.2
224800             PERFORM PASS                                         RL2054.2
224900             PERFORM PRINT-DETAIL.                                RL2054.2
225000*                                                                 RL2054.2
225100 REL-8X-INIT-19.                                                  RL2054.2
225200     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
225300     MOVE   "REL-8X-TEST-19"  TO PAR-NAME.                        RL2054.2
225400     MOVE    555 TO WRK-RL-FD1-RECKEY.                            RL2054.2
225500     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
225600     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
225700     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
225800     MOVE    SPACE  TO WRK-XN-00002.                              RL2054.2
225900     MOVE    1 TO REC-CT.                                         RL2054.2
226000     GO TO   REL-8X-TEST-19-0.                                    RL2054.2
226100 REL-8X-DELETE-19.                                                RL2054.2
226200     PERFORM DE-LETE.                                             RL2054.2
226300     PERFORM PRINT-DETAIL.                                        RL2054.2
226400     GO TO   REL-8X-INIT-20.                                      RL2054.2
226500 REL-8X-TEST-19-0.                                                RL2054.2
226600     START   RL-FD1                                               RL2054.2
226700             KEY >= RL-FD1-KEY                                    RL2054.2
226800             NOT INVALID KEY                                      RL2054.2
226900                 MOVE "A" TO WRK-XN-00001                         RL2054.2
227000     END-START                                                    RL2054.2
227100     MOVE   "Z" TO WRK-XN-00002.                                  RL2054.2
227200 REL-8X-TEST-19-1.                                                RL2054.2
227300     IF      WRK-XN-00001 NOT = SPACE                             RL2054.2
227400             MOVE   "NOT INVALID KEY SHOULD NOT HAVE EXECUTED"    RL2054.2
227500                  TO RE-MARK                                      RL2054.2
227600             MOVE    SPACE TO CORRECT-X                           RL2054.2
227700             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
227800             PERFORM FAIL                                         RL2054.2
227900             PERFORM PRINT-DETAIL                                 RL2054.2
228000     ELSE                                                         RL2054.2
228100             PERFORM PASS                                         RL2054.2
228200             PERFORM PRINT-DETAIL.                                RL2054.2
228300     ADD     1 TO REC-CT.                                         RL2054.2
228400 REL-8X-TEST-19-2.                                                RL2054.2
228500     MOVE   "REL-8X-TEST-19-2" TO PAR-NAME.                       RL2054.2
228600     IF      WRK-XN-00002 NOT = "Z"                               RL2054.2
228700             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL2054.2
228800             MOVE   "Z"  TO CORRECT-X                             RL2054.2
228900             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
229000             PERFORM FAIL                                         RL2054.2
229100             PERFORM PRINT-DETAIL                                 RL2054.2
229200     ELSE                                                         RL2054.2
229300             PERFORM PASS                                         RL2054.2
229400             PERFORM PRINT-DETAIL.                                RL2054.2
229500*                                                                 RL2054.2
229600 REL-8X-INIT-20.                                                  RL2054.2
229700     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
229800     MOVE   "REL-8X-TEST-20"  TO PAR-NAME.                        RL2054.2
229900     MOVE    101 TO WRK-RL-FD1-RECKEY.                            RL2054.2
230000     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
230100     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
230200     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
230300     MOVE    1 TO REC-CT.                                         RL2054.2
230400     GO TO   REL-8X-TEST-20-0.                                    RL2054.2
230500 REL-8X-DELETE-20.                                                RL2054.2
230600     PERFORM DE-LETE.                                             RL2054.2
230700     PERFORM PRINT-DETAIL.                                        RL2054.2
230800     GO TO   REL-8X-INIT-21.                                      RL2054.2
230900 REL-8X-TEST-20-0.                                                RL2054.2
231000     START   RL-FD1                                               RL2054.2
231100             KEY >= RL-FD1-KEY                                    RL2054.2
231200                 INVALID KEY                                      RL2054.2
231300                 MOVE "B" TO WRK-XN-00001                         RL2054.2
231400             NOT INVALID KEY                                      RL2054.2
231500                 MOVE "A" TO WRK-XN-00001                         RL2054.2
231600     END-START                                                    RL2054.2
231700     MOVE   "Z" TO WRK-XN-00002.                                  RL2054.2
231800 REL-8X-TEST-20-1.                                                RL2054.2
231900     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
232000             MOVE   "NOT INVALID KEY SHOULD HAVE EXECUTED"        RL2054.2
232100                  TO RE-MARK                                      RL2054.2
232200             MOVE   "A" TO CORRECT-X                              RL2054.2
232300             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
232400             PERFORM FAIL                                         RL2054.2
232500             PERFORM PRINT-DETAIL                                 RL2054.2
232600     ELSE                                                         RL2054.2
232700             PERFORM PASS                                         RL2054.2
232800             PERFORM PRINT-DETAIL.                                RL2054.2
232900     ADD     1 TO REC-CT.                                         RL2054.2
233000 REL-8X-TEST-20-2.                                                RL2054.2
233100     MOVE   "REL-8X-TEST-20-2" TO PAR-NAME.                       RL2054.2
233200     READ    RL-FD1  INTO FILE-RECORD-INFO (1).                   RL2054.2
233300     IF      XRECORD-NUMBER (1) < 101                             RL2054.2
233400             MOVE   "RECORD NUMBER LESS THAN RELATIVE KEY"        RL2054.2
233500                  TO RE-MARK                                      RL2054.2
233600             MOVE    101 TO CORRECT-N                             RL2054.2
233700             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2054.2
233800             PERFORM FAIL                                         RL2054.2
233900             PERFORM PRINT-DETAIL                                 RL2054.2
234000     ELSE                                                         RL2054.2
234100             PERFORM PASS                                         RL2054.2
234200             PERFORM PRINT-DETAIL.                                RL2054.2
234300     ADD     1 TO REC-CT.                                         RL2054.2
234400 REL-8X-TEST-20-3.                                                RL2054.2
234500     MOVE   "REL-8X-TEST-20-3" TO PAR-NAME.                       RL2054.2
234600     IF      WRK-XN-00002 NOT = "Z"                               RL2054.2
234700             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL2054.2
234800             MOVE   "Z"  TO CORRECT-X                             RL2054.2
234900             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
235000             PERFORM FAIL                                         RL2054.2
235100             PERFORM PRINT-DETAIL                                 RL2054.2
235200     ELSE                                                         RL2054.2
235300             PERFORM PASS                                         RL2054.2
235400             PERFORM PRINT-DETAIL.                                RL2054.2
235500*                                                                 RL2054.2
235600 REL-8X-INIT-21.                                                  RL2054.2
235700     MOVE   "VIII-33 4.7.2 (GR7)"  TO ANSI-REFERENCE.             RL2054.2
235800     MOVE   "REL-8X-TEST-21"  TO PAR-NAME.                        RL2054.2
235900     MOVE    666 TO WRK-RL-FD1-RECKEY.                            RL2054.2
236000     MOVE    WRK-RL-FD1-RECKEY TO RL-FD1-KEY.                     RL2054.2
236100     MOVE    SPACES TO FILE-RECORD-INFO (1).                      RL2054.2
236200     MOVE    SPACE  TO WRK-XN-00001.                              RL2054.2
236300     MOVE    SPACE  TO WRK-XN-00002.                              RL2054.2
236400     MOVE    1 TO REC-CT.                                         RL2054.2
236500     GO TO   REL-8X-TEST-21-0.                                    RL2054.2
236600 REL-8X-DELETE-21.                                                RL2054.2
236700     PERFORM DE-LETE.                                             RL2054.2
236800     PERFORM PRINT-DETAIL.                                        RL2054.2
236900     GO TO   REL-8X-END-21.                                       RL2054.2
237000 REL-8X-TEST-21-0.                                                RL2054.2
237100     START   RL-FD1                                               RL2054.2
237200             KEY >= RL-FD1-KEY                                    RL2054.2
237300                 INVALID KEY                                      RL2054.2
237400                 MOVE "A" TO WRK-XN-00001                         RL2054.2
237500             NOT INVALID KEY                                      RL2054.2
237600                 MOVE "B" TO WRK-XN-00001                         RL2054.2
237700     END-START                                                    RL2054.2
237800     MOVE   "Z" TO WRK-XN-00002.                                  RL2054.2
237900 REL-8X-TEST-21-1.                                                RL2054.2
238000     IF      WRK-XN-00001 NOT = "A"                               RL2054.2
238100             MOVE   "INVALID KEY SHOULD HAVE EXECUTED"            RL2054.2
238200                  TO RE-MARK                                      RL2054.2
238300             MOVE   "A" TO CORRECT-X                              RL2054.2
238400             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
238500             PERFORM FAIL                                         RL2054.2
238600             PERFORM PRINT-DETAIL                                 RL2054.2
238700     ELSE                                                         RL2054.2
238800             PERFORM PASS                                         RL2054.2
238900             PERFORM PRINT-DETAIL.                                RL2054.2
239000     ADD     1 TO REC-CT.                                         RL2054.2
239100 REL-8X-TEST-21-2.                                                RL2054.2
239200     MOVE   "REL-8X-TEST-21-2" TO PAR-NAME.                       RL2054.2
239300     IF      WRK-XN-00002 NOT = "Z"                               RL2054.2
239400             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         RL2054.2
239500             MOVE   "Z"  TO CORRECT-X                             RL2054.2
239600             MOVE    WRK-XN-00001 TO COMPUTED-X                   RL2054.2
239700             PERFORM FAIL                                         RL2054.2
239800             PERFORM PRINT-DETAIL                                 RL2054.2
239900     ELSE                                                         RL2054.2
240000             PERFORM PASS                                         RL2054.2
240100             PERFORM PRINT-DETAIL.                                RL2054.2
240200 REL-8X-END-21.                                                   RL2054.2
240300     CLOSE   RL-FD1.                                              RL2054.2
240400     CLOSE   RL-FS2.                                              RL2054.2
240500 REL-8X-EXIT.                                                     RL2054.2
240600     EXIT.                                                        RL2054.2
240700*                                                                 RL2054.2
240800 CCVS-EXIT SECTION.                                               RL2054.2
240900 CCVS-999999.                                                     RL2054.2
241000     GO TO CLOSE-FILES.                                           RL2054.2
      *END-OF,RL205A                                                                  
