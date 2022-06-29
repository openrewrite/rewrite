      *HEADER,COBOL,IX217A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2174.2
000200 PROGRAM-ID.                                                      IX2174.2
000300     IX217A.                                                      IX2174.2
000400****************************************************************  IX2174.2
000500*                                                              *  IX2174.2
000600*    VALIDATION FOR:-                                          *  IX2174.2
000700*                                                              *  IX2174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2174.2
000900*                                                              *  IX2174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2174.2
001100*                                                              *  IX2174.2
001200****************************************************************  IX2174.2
001300*                                                                 IX2174.2
001400*                      THE FUNCTION OF THIS PROGRAM IS TO CREATE  IX2174.2
001500*     THE  OPTIONAL  BUT  NOT EXISTING INDEXED FILES BY THE OPEN  IX2174.2
001600*     I-O  AND  THE OPEN EXTEND STATEMENTS. THE FILE STATUS CODE  IX2174.2
001700*     FOR  BOTH  FILES  MUST  BE  "05" AFTER PROCESSING THE OPEN  IX2174.2
001800*     STATEMENT. FILE IX-FS1 CONTAINS 50 RECORDS AFTER CORRECT    IX2174.2
001900*     EXECUTION AND FILE IX-VS1 CONTAINS 25 LONG RECORDS (240)    IX2174.2
002000*     AND 25 SHORT RECORDS (200) AFTER CORRECT EXECUTION.         IX2174.2
002100*                                                                 IX2174.2
002200*                                                                 IX2174.2
002300*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2174.2
002400*                                                                 IX2174.2
002500*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2174.2
002600*                    CLAUSE FOR DATA FILE IX-FS1                  IX2174.2
002700*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2174.2
002800*                    CLAUSE FOR DATA FILE IX-VS1                  IX2174.2
002900*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2174.2
003000*                    CLAUSE FOR INDEX FILE IX-FS1                 IX2174.2
003100*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2174.2
003200*                    CLAUSE FOR INDEX FILE IX-VS1                 IX2174.2
003300*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2174.2
003400*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2174.2
003500*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2174.2
003600*                                                                 IX2174.2
003700*         NOTE:  X-CARDS 44 AND 62          ARE OPTIONAL          IX2174.2
003800*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2174.2
003900*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2174.2
004000*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2174.2
004100*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2174.2
004200*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2174.2
004300*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2174.2
004400*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2174.2
004500*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2174.2
004600*               THEY ARE AS FOLLOWS                               IX2174.2
004700*                                                                 IX2174.2
004800*                  J  SELECTS X-CARD 44                           IX2174.2
004900*                  J  SELECTS X-CARD 45                           IX2174.2
005000*                  C  SELECTS X-CARD 84                           IX2174.2
005100*                                                                 IX2174.2
005200******************************************************            IX2174.2
005300 ENVIRONMENT DIVISION.                                            IX2174.2
005400 CONFIGURATION SECTION.                                           IX2174.2
005500 SOURCE-COMPUTER.                                                 IX2174.2
005600     XXXXX082.                                                    IX2174.2
005700 OBJECT-COMPUTER.                                                 IX2174.2
005800     XXXXX083.                                                    IX2174.2
005900 INPUT-OUTPUT SECTION.                                            IX2174.2
006000 FILE-CONTROL.                                                    IX2174.2
006100     SELECT PRINT-FILE ASSIGN TO                                  IX2174.2
006200     XXXXX055.                                                    IX2174.2
006300                                                                  IX2174.2
006400     SELECT OPTIONAL   IX-FS1 ASSIGN TO                           IX2174.2
006500     XXXXP024                                                     IX2174.2
006600*    XXXXP044                                                     IX2174.2
006700     ORGANIZATION IS INDEXED                                      IX2174.2
006800     RECORD KEY IS IX-FS1-KEY                                     IX2174.2
006900     ACCESS MODE IS DYNAMIC                                       IX2174.2
007000     FILE STATUS IS IX-FS1-STATUS.                                IX2174.2
007100                                                                  IX2174.2
007200     SELECT OPTIONAL   IX-VS1 ASSIGN TO                           IX2174.2
007300     XXXXP025                                                     IX2174.2
007400*    XXXXP045                                                     IX2174.2
007500     ORGANIZATION IS INDEXED                                      IX2174.2
007600     RECORD KEY IS IX-VS1-KEY                                     IX2174.2
007700     ACCESS MODE IS SEQUENTIAL                                    IX2174.2
007800     FILE STATUS IS IX-VS1-STATUS.                                IX2174.2
007900                                                                  IX2174.2
008000 DATA DIVISION.                                                   IX2174.2
008100 FILE SECTION.                                                    IX2174.2
008200 FD  PRINT-FILE.                                                  IX2174.2
008300 01  PRINT-REC PICTURE X(120).                                    IX2174.2
008400 01  DUMMY-RECORD PICTURE X(120).                                 IX2174.2
008500                                                                  IX2174.2
008600 FD  IX-FS1                                                       IX2174.2
008700C    LABEL RECORD IS STANDARD                                     IX2174.2
008800C    DATA RECORD IS IX-FS1R1-F-G-240                              IX2174.2
008900     BLOCK CONTAINS 1 RECORDS                                     IX2174.2
009000     RECORD CONTAINS 240 CHARACTERS.                              IX2174.2
009100 01  IX-FS1R1-F-G-240.                                            IX2174.2
009200     03 IX-FS1-WRK-120 PIC X(120).                                IX2174.2
009300     03 IX-FS1-GRP-120.                                           IX2174.2
009400     05 FILLER   PIC   X(8).                                      IX2174.2
009500     05 IX-FS1-KEY  PIC X(29).                                    IX2174.2
009600     05 FILLER PIC X(83).                                         IX2174.2
009700                                                                  IX2174.2
009800 FD  IX-VS1                                                       IX2174.2
009900C    LABEL RECORD IS STANDARD                                     IX2174.2
010000C    DATA RECORD IS IX-VS1R1-F-G-240 IX-VS1R1-F-G-200             IX2174.2
010100     BLOCK CONTAINS 1 RECORDS                                     IX2174.2
010200     RECORD VARYING  200 TO 240 DEPENDING REC-LENGTH.             IX2174.2
010300                                                                  IX2174.2
010400 01  IX-VS1R1-F-G-240.                                            IX2174.2
010500     03 IX-VS1-WRK-120 PIC X(120).                                IX2174.2
010600     03 IX-VS1-GRP-120.                                           IX2174.2
010700        05 FILLER   PIC   X(8).                                   IX2174.2
010800        05 IX-VS1-KEY  PIC X(29).                                 IX2174.2
010900        05 FILLER PIC X(83).                                      IX2174.2
011000                                                                  IX2174.2
011100 01  IX-VS1R1-F-G-200.                                            IX2174.2
011200     03 IX-VS1-WRK-120-SHORT PIC X(120).                          IX2174.2
011300     03 IX-VS1-GRP-80.                                            IX2174.2
011400        05 FILLER   PIC   X(8).                                   IX2174.2
011500        05 FILLER-KEY  PIC X(29).                                 IX2174.2
011600        05 VIERZIG PIC X(43).                                     IX2174.2
011700                                                                  IX2174.2
011800 WORKING-STORAGE SECTION.                                         IX2174.2
011900 01  REC-LENGTH       PIC 9999 VALUE ZERO.                        IX2174.2
012000 01  STATUS-ERROR     PIC    9 VALUE ZERO.                        IX2174.2
012100 01  GRP-0101.                                                    IX2174.2
012200     02 FILLER   PIC X(10)  VALUE "ABCDLKJXYZ".                   IX2174.2
012300     02 WRK-DU-09V00-001 PIC 9(9)  VALUE ZERO.                    IX2174.2
012400     02 FILLER  PIC X(10)  VALUE "ZIF,.$-+CD".                    IX2174.2
012500 01  FILE-RECORD-INFORMATION-REC.                                 IX2174.2
012600     03 FILE-RECORD-INFO-SKELETON.                                IX2174.2
012700        05 FILLER                 PICTURE X(48)       VALUE       IX2174.2
012800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2174.2
012900        05 FILLER                 PICTURE X(46)       VALUE       IX2174.2
013000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2174.2
013100        05 FILLER                 PICTURE X(26)       VALUE       IX2174.2
013200             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2174.2
013300        05 FILLER                 PICTURE X(37)       VALUE       IX2174.2
013400             ",RECKEY=                             ".             IX2174.2
013500        05 FILLER                 PICTURE X(38)       VALUE       IX2174.2
013600             ",ALTKEY1=                             ".            IX2174.2
013700        05 FILLER                 PICTURE X(38)       VALUE       IX2174.2
013800             ",ALTKEY2=                             ".            IX2174.2
013900        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2174.2
014000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2174.2
014100        05 FILE-RECORD-INFO-P1-120.                               IX2174.2
014200           07 FILLER              PIC X(5).                       IX2174.2
014300           07 XFILE-NAME           PIC X(6).                      IX2174.2
014400           07 FILLER              PIC X(8).                       IX2174.2
014500           07 XRECORD-NAME         PIC X(6).                      IX2174.2
014600           07 FILLER              PIC X(1).                       IX2174.2
014700           07 REELUNIT-NUMBER     PIC 9(1).                       IX2174.2
014800           07 FILLER              PIC X(7).                       IX2174.2
014900           07 XRECORD-NUMBER       PIC 9(6).                      IX2174.2
015000           07 FILLER              PIC X(6).                       IX2174.2
015100           07 UPDATE-NUMBER       PIC 9(2).                       IX2174.2
015200           07 FILLER              PIC X(5).                       IX2174.2
015300           07 ODO-NUMBER          PIC 9(4).                       IX2174.2
015400           07 FILLER              PIC X(5).                       IX2174.2
015500           07 XPROGRAM-NAME        PIC X(5).                      IX2174.2
015600           07 FILLER              PIC X(7).                       IX2174.2
015700           07 XRECORD-LENGTH       PIC 9(6).                      IX2174.2
015800           07 FILLER              PIC X(7).                       IX2174.2
015900           07 CHARS-OR-RECORDS    PIC X(2).                       IX2174.2
016000           07 FILLER              PIC X(1).                       IX2174.2
016100           07 XBLOCK-SIZE          PIC 9(4).                      IX2174.2
016200           07 FILLER              PIC X(6).                       IX2174.2
016300           07 RECORDS-IN-FILE     PIC 9(6).                       IX2174.2
016400           07 FILLER              PIC X(5).                       IX2174.2
016500           07 XFILE-ORGANIZATION   PIC X(2).                      IX2174.2
016600           07 FILLER              PIC X(6).                       IX2174.2
016700           07 XLABEL-TYPE          PIC X(1).                      IX2174.2
016800        05 FILE-RECORD-INFO-P121-240.                             IX2174.2
016900           07 FILLER              PIC X(8).                       IX2174.2
017000           07 XRECORD-KEY          PIC X(29).                     IX2174.2
017100           07 FILLER              PIC X(9).                       IX2174.2
017200           07 ALTERNATE-KEY1      PIC X(29).                      IX2174.2
017300           07 FILLER              PIC X(9).                       IX2174.2
017400           07 ALTERNATE-KEY2      PIC X(29).                      IX2174.2
017500           07 FILLER              PIC X(7).                       IX2174.2
017600                                                                  IX2174.2
017700 01  IX-FS1-STATUS.                                               IX2174.2
017800     05  IX-FS1-STAT1        PIC X.                               IX2174.2
017900     05  IX-FS1-STAT2        PIC X.                               IX2174.2
018000                                                                  IX2174.2
018100 01  IX-VS1-STATUS.                                               IX2174.2
018200     05  IX-VS1-STAT1        PIC X.                               IX2174.2
018300     05  IX-VS1-STAT2        PIC X.                               IX2174.2
018400                                                                  IX2174.2
018500 01  TEST-RESULTS.                                                IX2174.2
018600     02 FILLER                   PIC X      VALUE SPACE.          IX2174.2
018700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2174.2
018800     02 FILLER                   PIC X      VALUE SPACE.          IX2174.2
018900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2174.2
019000     02 FILLER                   PIC X      VALUE SPACE.          IX2174.2
019100     02  PAR-NAME.                                                IX2174.2
019200       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2174.2
019300       03  PARDOT-X              PIC X      VALUE SPACE.          IX2174.2
019400       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2174.2
019500     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2174.2
019600     02 RE-MARK                  PIC X(61).                       IX2174.2
019700 01  TEST-COMPUTED.                                               IX2174.2
019800     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2174.2
019900     02 FILLER                   PIC X(17)  VALUE                 IX2174.2
020000            "       COMPUTED=".                                   IX2174.2
020100     02 COMPUTED-X.                                               IX2174.2
020200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2174.2
020300     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2174.2
020400                                 PIC -9(9).9(9).                  IX2174.2
020500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2174.2
020600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2174.2
020700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2174.2
020800     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2174.2
020900         04 COMPUTED-18V0                    PIC -9(18).          IX2174.2
021000         04 FILLER                           PIC X.               IX2174.2
021100     03 FILLER PIC X(50) VALUE SPACE.                             IX2174.2
021200 01  TEST-CORRECT.                                                IX2174.2
021300     02 FILLER PIC X(30) VALUE SPACE.                             IX2174.2
021400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2174.2
021500     02 CORRECT-X.                                                IX2174.2
021600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2174.2
021700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2174.2
021800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2174.2
021900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2174.2
022000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2174.2
022100     03      CR-18V0 REDEFINES CORRECT-A.                         IX2174.2
022200         04 CORRECT-18V0                     PIC -9(18).          IX2174.2
022300         04 FILLER                           PIC X.               IX2174.2
022400     03 FILLER PIC X(2) VALUE SPACE.                              IX2174.2
022500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2174.2
022600 01  CCVS-C-1.                                                    IX2174.2
022700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2174.2
022800-    "SS  PARAGRAPH-NAME                                          IX2174.2
022900-    "       REMARKS".                                            IX2174.2
023000     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2174.2
023100 01  CCVS-C-2.                                                    IX2174.2
023200     02 FILLER                     PIC X        VALUE SPACE.      IX2174.2
023300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2174.2
023400     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2174.2
023500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2174.2
023600     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2174.2
023700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2174.2
023800 01  REC-CT                        PIC 99       VALUE ZERO.       IX2174.2
023900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2174.2
024000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2174.2
024100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2174.2
024200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2174.2
024300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2174.2
024400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2174.2
024500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2174.2
024600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2174.2
024700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2174.2
024800 01  CCVS-H-1.                                                    IX2174.2
024900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2174.2
025000     02  FILLER                    PIC X(42)    VALUE             IX2174.2
025100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2174.2
025200     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2174.2
025300 01  CCVS-H-2A.                                                   IX2174.2
025400   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2174.2
025500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2174.2
025600   02  FILLER                        PIC XXXX   VALUE             IX2174.2
025700     "4.2 ".                                                      IX2174.2
025800   02  FILLER                        PIC X(28)  VALUE             IX2174.2
025900            " COPY - NOT FOR DISTRIBUTION".                       IX2174.2
026000   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2174.2
026100                                                                  IX2174.2
026200 01  CCVS-H-2B.                                                   IX2174.2
026300   02  FILLER                        PIC X(15)  VALUE             IX2174.2
026400            "TEST RESULT OF ".                                    IX2174.2
026500   02  TEST-ID                       PIC X(9).                    IX2174.2
026600   02  FILLER                        PIC X(4)   VALUE             IX2174.2
026700            " IN ".                                               IX2174.2
026800   02  FILLER                        PIC X(12)  VALUE             IX2174.2
026900     " HIGH       ".                                              IX2174.2
027000   02  FILLER                        PIC X(22)  VALUE             IX2174.2
027100            " LEVEL VALIDATION FOR ".                             IX2174.2
027200   02  FILLER                        PIC X(58)  VALUE             IX2174.2
027300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2174.2
027400 01  CCVS-H-3.                                                    IX2174.2
027500     02  FILLER                      PIC X(34)  VALUE             IX2174.2
027600            " FOR OFFICIAL USE ONLY    ".                         IX2174.2
027700     02  FILLER                      PIC X(58)  VALUE             IX2174.2
027800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2174.2
027900     02  FILLER                      PIC X(28)  VALUE             IX2174.2
028000            "  COPYRIGHT   1985 ".                                IX2174.2
028100 01  CCVS-E-1.                                                    IX2174.2
028200     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2174.2
028300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2174.2
028400     02 ID-AGAIN                     PIC X(9).                    IX2174.2
028500     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2174.2
028600 01  CCVS-E-2.                                                    IX2174.2
028700     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2174.2
028800     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2174.2
028900     02 CCVS-E-2-2.                                               IX2174.2
029000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2174.2
029100         03 FILLER                   PIC X      VALUE SPACE.      IX2174.2
029200         03 ENDER-DESC               PIC X(44)  VALUE             IX2174.2
029300            "ERRORS ENCOUNTERED".                                 IX2174.2
029400 01  CCVS-E-3.                                                    IX2174.2
029500     02  FILLER                      PIC X(22)  VALUE             IX2174.2
029600            " FOR OFFICIAL USE ONLY".                             IX2174.2
029700     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2174.2
029800     02  FILLER                      PIC X(58)  VALUE             IX2174.2
029900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2174.2
030000     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2174.2
030100     02 FILLER                       PIC X(15)  VALUE             IX2174.2
030200             " COPYRIGHT 1985".                                   IX2174.2
030300 01  CCVS-E-4.                                                    IX2174.2
030400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2174.2
030500     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2174.2
030600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2174.2
030700     02 FILLER                       PIC X(40)  VALUE             IX2174.2
030800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2174.2
030900 01  XXINFO.                                                      IX2174.2
031000     02 FILLER                       PIC X(19)  VALUE             IX2174.2
031100            "*** INFORMATION ***".                                IX2174.2
031200     02 INFO-TEXT.                                                IX2174.2
031300       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2174.2
031400       04 XXCOMPUTED                 PIC X(20).                   IX2174.2
031500       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2174.2
031600       04 XXCORRECT                  PIC X(20).                   IX2174.2
031700     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2174.2
031800 01  HYPHEN-LINE.                                                 IX2174.2
031900     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2174.2
032000     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2174.2
032100-    "*****************************************".                 IX2174.2
032200     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2174.2
032300-    "******************************".                            IX2174.2
032400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2174.2
032500     "IX217A".                                                    IX2174.2
032600 PROCEDURE DIVISION.                                              IX2174.2
032700 CCVS1 SECTION.                                                   IX2174.2
032800 OPEN-FILES.                                                      IX2174.2
032900     OPEN    OUTPUT PRINT-FILE.                                   IX2174.2
033000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2174.2
033100     MOVE    SPACE TO TEST-RESULTS.                               IX2174.2
033200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2174.2
033300     MOVE    ZERO TO REC-SKL-SUB.                                 IX2174.2
033400     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2174.2
033500 CCVS-INIT-FILE.                                                  IX2174.2
033600     ADD     1 TO REC-SKL-SUB.                                    IX2174.2
033700     MOVE    FILE-RECORD-INFO-SKELETON                            IX2174.2
033800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2174.2
033900 CCVS-INIT-EXIT.                                                  IX2174.2
034000     GO TO CCVS1-EXIT.                                            IX2174.2
034100 CLOSE-FILES.                                                     IX2174.2
034200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2174.2
034300 TERMINATE-CCVS.                                                  IX2174.2
034400     STOP     RUN.                                                IX2174.2
034500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2174.2
034600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2174.2
034700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2174.2
034800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2174.2
034900     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2174.2
035000 PRINT-DETAIL.                                                    IX2174.2
035100     IF REC-CT NOT EQUAL TO ZERO                                  IX2174.2
035200             MOVE "." TO PARDOT-X                                 IX2174.2
035300             MOVE REC-CT TO DOTVALUE.                             IX2174.2
035400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2174.2
035500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2174.2
035600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2174.2
035700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2174.2
035800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2174.2
035900     MOVE SPACE TO CORRECT-X.                                     IX2174.2
036000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2174.2
036100     MOVE     SPACE TO RE-MARK.                                   IX2174.2
036200 HEAD-ROUTINE.                                                    IX2174.2
036300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2174.2
036400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2174.2
036500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2174.2
036600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2174.2
036700 COLUMN-NAMES-ROUTINE.                                            IX2174.2
036800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2174.2
036900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2174.2
037000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2174.2
037100 END-ROUTINE.                                                     IX2174.2
037200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2174.2
037300 END-RTN-EXIT.                                                    IX2174.2
037400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2174.2
037500 END-ROUTINE-1.                                                   IX2174.2
037600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2174.2
037700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2174.2
037800      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2174.2
037900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2174.2
038000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2174.2
038100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2174.2
038200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2174.2
038300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2174.2
038400  END-ROUTINE-12.                                                 IX2174.2
038500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2174.2
038600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2174.2
038700         MOVE "NO " TO ERROR-TOTAL                                IX2174.2
038800         ELSE                                                     IX2174.2
038900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2174.2
039000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2174.2
039100     PERFORM WRITE-LINE.                                          IX2174.2
039200 END-ROUTINE-13.                                                  IX2174.2
039300     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2174.2
039400         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2174.2
039500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2174.2
039600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2174.2
039700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2174.2
039800      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2174.2
039900          MOVE "NO " TO ERROR-TOTAL                               IX2174.2
040000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2174.2
040100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2174.2
040200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2174.2
040300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2174.2
040400 WRITE-LINE.                                                      IX2174.2
040500     ADD 1 TO RECORD-COUNT.                                       IX2174.2
040600*    IF RECORD-COUNT GREATER 42                                   IX2174.2
040700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2174.2
040800*        MOVE SPACE TO DUMMY-RECORD                               IX2174.2
040900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2174.2
041000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2174.2
041100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2174.2
041200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2174.2
041300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2174.2
041400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2174.2
041500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2174.2
041600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2174.2
041700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2174.2
041800*        MOVE ZERO TO RECORD-COUNT.                               IX2174.2
041900     PERFORM WRT-LN.                                              IX2174.2
042000 WRT-LN.                                                          IX2174.2
042100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2174.2
042200     MOVE SPACE TO DUMMY-RECORD.                                  IX2174.2
042300 BLANK-LINE-PRINT.                                                IX2174.2
042400     PERFORM WRT-LN.                                              IX2174.2
042500 FAIL-ROUTINE.                                                    IX2174.2
042600     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2174.2
042700            GO TO   FAIL-ROUTINE-WRITE.                           IX2174.2
042800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2174.2
042900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2174.2
043000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2174.2
043100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2174.2
043200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2174.2
043300     GO TO  FAIL-ROUTINE-EX.                                      IX2174.2
043400 FAIL-ROUTINE-WRITE.                                              IX2174.2
043500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2174.2
043600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2174.2
043700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2174.2
043800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2174.2
043900 FAIL-ROUTINE-EX. EXIT.                                           IX2174.2
044000 BAIL-OUT.                                                        IX2174.2
044100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2174.2
044200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2174.2
044300 BAIL-OUT-WRITE.                                                  IX2174.2
044400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2174.2
044500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2174.2
044600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2174.2
044700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2174.2
044800 BAIL-OUT-EX. EXIT.                                               IX2174.2
044900 CCVS1-EXIT.                                                      IX2174.2
045000     EXIT.                                                        IX2174.2
045100                                                                  IX2174.2
045200******************************************************************IX2174.2
045300*   TEST  1                                                      *IX2174.2
045400*         OPEN I-O  (ACCESS IS DYNAMIC) OPTIONAL NOT EXISTING FILEIX2174.2
045500*                                       05  EXPECTED             *IX2174.2
045600*         IX-3, 1.3.4 (1) D                                      *IX2174.2
045700******************************************************************IX2174.2
045800 SECT-IX-01-001 SECTION.                                          IX2174.2
045900 OPN-INIT-GF-01.                                                  IX2174.2
046000     MOVE SPACES TO IX-FS1-STATUS.                                IX2174.2
046100     MOVE "OPEN I-O:    05 EXP." TO FEATURE.                      IX2174.2
046200     MOVE "OPN-TEST-GF-01      " TO PAR-NAME.                     IX2174.2
046300 OPN-TEST-GF-01.                                                  IX2174.2
046400     OPEN                                                         IX2174.2
046500          I-O     IX-FS1.                                         IX2174.2
046600     IF IX-FS1-STATUS      = "05"                                 IX2174.2
046700         GO TO OPN-PASS-GF-01.                                    IX2174.2
046800 OPN-FAIL-GF-01.                                                  IX2174.2
046900     MOVE "IX-3, 1.3.4, (1) D. " TO RE-MARK.                      IX2174.2
047000     PERFORM FAIL.                                                IX2174.2
047100     MOVE IX-FS1-STATUS TO COMPUTED-A.                            IX2174.2
047200     MOVE "05" TO CORRECT-X.                                      IX2174.2
047300     GO TO OPN-WRITE-GF-01.                                       IX2174.2
047400 OPN-PASS-GF-01.                                                  IX2174.2
047500     PERFORM PASS.                                                IX2174.2
047600 OPN-WRITE-GF-01.                                                 IX2174.2
047700     PERFORM PRINT-DETAIL.                                        IX2174.2
047800                                                                  IX2174.2
047900******************************************************************IX2174.2
048000*   TEST  2                                                      *IX2174.2
048100*         WRITE                            00 EXPECTED           *IX2174.2
048200*         IX-3, 1.3.4 (1) A                                      *IX2174.2
048300******************************************************************IX2174.2
048400 WRI-INIT-GF-01.                                                  IX2174.2
048500     MOVE    240 TO REC-LENGTH.                                   IX2174.2
048600     MOVE   ZERO TO STATUS-ERROR.                                 IX2174.2
048700     MOVE     "WRI-TEST-GF-01  " TO PAR-NAME                      IX2174.2
048800     MOVE     "WRITE (OPT)F 00 EXP." TO FEATURE.                  IX2174.2
048900     MOVE     "IX-FS1" TO XFILE-NAME (1).                         IX2174.2
049000     MOVE    "IX-F-G" TO XRECORD-NAME (1).                        IX2174.2
049100     MOVE  CCVS-PGM-ID TO XPROGRAM-NAME (1).                      IX2174.2
049200     MOVE    000240  TO XRECORD-LENGTH (1).                       IX2174.2
049300     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   IX2174.2
049400     MOVE     0001     TO XBLOCK-SIZE (1).                        IX2174.2
049500     MOVE     000500   TO RECORDS-IN-FILE (1).                    IX2174.2
049600     MOVE    "IX"  TO XFILE-ORGANIZATION (1).                     IX2174.2
049700     MOVE     "S"      TO XLABEL-TYPE (1).                        IX2174.2
049800     MOVE     000001   TO XRECORD-NUMBER (1).                     IX2174.2
049900 WRI-TEST-GF-01.                                                  IX2174.2
050000     MOVE    XRECORD-NUMBER (1) TO WRK-DU-09V00-001.              IX2174.2
050100     MOVE    GRP-0101 TO XRECORD-KEY (1).                         IX2174.2
050200     MOVE    FILE-RECORD-INFO (1) TO IX-FS1R1-F-G-240.            IX2174.2
050300     WRITE   IX-FS1R1-F-G-240                                     IX2174.2
050400              INVALID KEY GO TO WRI-FAIL-GF-01.                   IX2174.2
050500     IF IX-FS1-STATUS NOT EQUAL TO "00"                           IX2174.2
050600          MOVE 1 TO STATUS-ERROR.                                 IX2174.2
050700     IF      XRECORD-NUMBER (1) EQUAL TO 50                       IX2174.2
050800             GO TO WRI-TEST-GF-01-1.                              IX2174.2
050900     ADD      000001 TO XRECORD-NUMBER (1).                       IX2174.2
051000     GO       TO WRI-TEST-GF-01.                                  IX2174.2
051100 WRI-TEST-GF-01-1.                                                IX2174.2
051200     IF STATUS-ERROR EQUAL TO ZERO                                IX2174.2
051300                GO TO WRI-PASS-GF-01.                             IX2174.2
051400 WRI-FAIL-GF-01.                                                  IX2174.2
051500     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX2174.2
051600     PERFORM FAIL.                                                IX2174.2
051700     GO TO WRI-WRITE-GF-01.                                       IX2174.2
051800 WRI-PASS-GF-01.                                                  IX2174.2
051900     PERFORM PASS.                                                IX2174.2
052000 WRI-WRITE-GF-01.                                                 IX2174.2
052100     PERFORM PRINT-DETAIL.                                        IX2174.2
052200                                                                  IX2174.2
052300     CLOSE    IX-FS1.                                             IX2174.2
052400******************************************************************IX2174.2
052500*   TEST  3                                                      *IX2174.2
052600*         READ                               00 EXPECTED         *IX2174.2
052700*         IX-3, 1.3.4 (1) A                                      *IX2174.2
052800******************************************************************IX2174.2
052900 READ-INIT-F1-01.                                                 IX2174.2
053000     OPEN     INPUT     IX-FS1.                                   IX2174.2
053100     MOVE   ZERO TO WRK-DU-09V00-001.                             IX2174.2
053200 READ-TEST-F1-01.                                                 IX2174.2
053300     READ     IX-FS1 NEXT RECORD                                  IX2174.2
053400              AT END GO TO READ-TEST-F1-01-1.                     IX2174.2
053500     MOVE    IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).            IX2174.2
053600     ADD     1  TO WRK-DU-09V00-001.                              IX2174.2
053700     IF      WRK-DU-09V00-001     GREATER 50                      IX2174.2
053800             MOVE "MORE THAN 50  RECORDS" TO RE-MARK              IX2174.2
053900              GO TO READ-TEST-F1-01-1.                            IX2174.2
054000     IF XRECORD-NUMBER (1) = WRK-DU-09V00-001                     IX2174.2
054100             GO TO READ-TEST-F1-01                                IX2174.2
054200         ELSE                                                     IX2174.2
054300             MOVE "WRONG RECORD NUMBER" TO RE-MARK                IX2174.2
054400             PERFORM FAIL                                         IX2174.2
054500             MOVE "READ (TO VERIFY)" TO FEATURE                   IX2174.2
054600             MOVE "READ-TEST-F1-01" TO PAR-NAME                   IX2174.2
054700             PERFORM PRINT-DETAIL                                 IX2174.2
054800             GO TO READ-TEST-F1-01-3.                             IX2174.2
054900 READ-TEST-F1-01-1.                                               IX2174.2
055000     IF       XRECORD-NUMBER (1) NOT EQUAL TO 50                  IX2174.2
055100              PERFORM FAIL                                        IX2174.2
055200              ELSE                                                IX2174.2
055300              PERFORM PASS.                                       IX2174.2
055400     GO       TO READ-TEST-F1-01-2.                               IX2174.2
055500 READ-TEST-F1-01-2.                                               IX2174.2
055600     MOVE     "READ  (TO VERIFY)   " TO FEATURE.                  IX2174.2
055700     MOVE     "READ-TEST-F1-01" TO PAR-NAME.                      IX2174.2
055800     MOVE     "FILE VERIFIED, LFILE" TO COMPUTED-A.               IX2174.2
055900     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  IX2174.2
056000     PERFORM  PRINT-DETAIL.                                       IX2174.2
056100 READ-TEST-F1-01-3.                                               IX2174.2
056200     CLOSE   IX-FS1.                                              IX2174.2
056300                                                                  IX2174.2
056400******************************************************************IX2174.2
056500*   TEST  4                                                      *IX2174.2
056600*       OPEN EXTEND (ACCESS IS DYNAMIC) OPTIONAL NOT EXISTING FILEIX2174.2
056700*                                       05  EXPECTED             *IX2174.2
056800*         IX-3, 1.3.4 (1) D                                      *IX2174.2
056900******************************************************************IX2174.2
057000 OPN-INIT-GF-02.                                                  IX2174.2
057100     MOVE SPACES TO IX-VS1-STATUS.                                IX2174.2
057200     MOVE "OPEN EXTEND: 05 EXP." TO FEATURE.                      IX2174.2
057300     MOVE "OPN-TEST-GF-02      " TO PAR-NAME.                     IX2174.2
057400 OPN-TEST-GF-02.                                                  IX2174.2
057500     OPEN                                                         IX2174.2
057600          EXTEND  IX-VS1.                                         IX2174.2
057700     IF IX-VS1-STATUS      = "05"                                 IX2174.2
057800         GO TO OPN-PASS-GF-02.                                    IX2174.2
057900 OPN-FAIL-GF-02.                                                  IX2174.2
058000     MOVE "IX-3, 1.3.4, (1) D. " TO RE-MARK.                      IX2174.2
058100     PERFORM FAIL.                                                IX2174.2
058200     MOVE IX-VS1-STATUS TO COMPUTED-A.                            IX2174.2
058300     MOVE "05" TO CORRECT-X.                                      IX2174.2
058400     GO TO OPN-WRITE-GF-02.                                       IX2174.2
058500 OPN-PASS-GF-02.                                                  IX2174.2
058600     PERFORM PASS.                                                IX2174.2
058700 OPN-WRITE-GF-02.                                                 IX2174.2
058800     PERFORM PRINT-DETAIL.                                        IX2174.2
058900                                                                  IX2174.2
059000******************************************************************IX2174.2
059100*   TEST  5                                                      *IX2174.2
059200*         WRITE                            00 EXPECTED           *IX2174.2
059300*         IX-3, 1.3.4 (1) A                                      *IX2174.2
059400******************************************************************IX2174.2
059500 WRI-INIT-GF-02.                                                  IX2174.2
059600     MOVE    240 TO REC-LENGTH.                                   IX2174.2
059700     MOVE   ZERO TO STATUS-ERROR.                                 IX2174.2
059800     MOVE     "WRI-TEST-GF-02  " TO PAR-NAME                      IX2174.2
059900     MOVE     "WRITE (OPT)S 00 EXP." TO FEATURE.                  IX2174.2
060000     MOVE     "IX-VS1" TO XFILE-NAME (1).                         IX2174.2
060100     MOVE    " LONG " TO XRECORD-NAME (1).                        IX2174.2
060200     MOVE  CCVS-PGM-ID TO XPROGRAM-NAME (1).                      IX2174.2
060300     MOVE    000240  TO XRECORD-LENGTH (1).                       IX2174.2
060400     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   IX2174.2
060500     MOVE     0001     TO XBLOCK-SIZE (1).                        IX2174.2
060600     MOVE     000500   TO RECORDS-IN-FILE (1).                    IX2174.2
060700     MOVE    "IX"  TO XFILE-ORGANIZATION (1).                     IX2174.2
060800     MOVE     "S"      TO XLABEL-TYPE (1).                        IX2174.2
060900     MOVE     000001   TO XRECORD-NUMBER (1).                     IX2174.2
061000 WRI-TEST-GF-02.                                                  IX2174.2
061100     MOVE    XRECORD-NUMBER (1) TO WRK-DU-09V00-001.              IX2174.2
061200     MOVE    GRP-0101 TO XRECORD-KEY (1).                         IX2174.2
061300     MOVE    FILE-RECORD-INFO (1) TO IX-VS1R1-F-G-240.            IX2174.2
061400     IF XRECORD-NUMBER (1) LESS THAN 26                           IX2174.2
061500         WRITE   IX-VS1R1-F-G-240                                 IX2174.2
061600              INVALID KEY GO TO WRI-FAIL-GF-02.                   IX2174.2
061700     IF IX-VS1-STATUS NOT EQUAL TO "00"                           IX2174.2
061800          MOVE 1 TO STATUS-ERROR.                                 IX2174.2
061900     IF XRECORD-NUMBER (1) GREATER THAN 25                        IX2174.2
062000         WRITE   IX-VS1R1-F-G-200                                 IX2174.2
062100              INVALID KEY GO TO WRI-FAIL-GF-02.                   IX2174.2
062200     IF IX-VS1-STATUS NOT EQUAL TO "00"                           IX2174.2
062300          MOVE 1 TO STATUS-ERROR.                                 IX2174.2
062400     IF      XRECORD-NUMBER (1) EQUAL TO 50                       IX2174.2
062500             GO TO WRI-TEST-GF-02-1.                              IX2174.2
062600     IF      XRECORD-NUMBER (1) EQUAL TO 25                       IX2174.2
062700             MOVE " SHORT" TO XRECORD-NAME   (1)                  IX2174.2
062800             MOVE 200 TO REC-LENGTH                               IX2174.2
062900             MOVE 000200 TO XRECORD-LENGTH (1).                   IX2174.2
063000     ADD      000001 TO XRECORD-NUMBER (1).                       IX2174.2
063100     GO       TO WRI-TEST-GF-02.                                  IX2174.2
063200 WRI-TEST-GF-02-1.                                                IX2174.2
063300     IF STATUS-ERROR EQUAL TO ZERO                                IX2174.2
063400                GO TO WRI-PASS-GF-02.                             IX2174.2
063500 WRI-FAIL-GF-02.                                                  IX2174.2
063600     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX2174.2
063700     PERFORM FAIL.                                                IX2174.2
063800     GO TO WRI-WRITE-GF-02.                                       IX2174.2
063900 WRI-PASS-GF-02.                                                  IX2174.2
064000     PERFORM PASS.                                                IX2174.2
064100 WRI-WRITE-GF-02.                                                 IX2174.2
064200     PERFORM PRINT-DETAIL.                                        IX2174.2
064300                                                                  IX2174.2
064400     CLOSE    IX-VS1.                                             IX2174.2
064500******************************************************************IX2174.2
064600*   TEST  6                                                      *IX2174.2
064700*         READ                               00 EXPECTED         *IX2174.2
064800*         IX-3, 1.3.4 (1) A                                      *IX2174.2
064900******************************************************************IX2174.2
065000 READ-INIT-F1-02.                                                 IX2174.2
065100     OPEN     INPUT     IX-VS1.                                   IX2174.2
065200     MOVE   ZERO TO WRK-DU-09V00-001.                             IX2174.2
065300 READ-TEST-F1-02.                                                 IX2174.2
065400     READ     IX-VS1 NEXT RECORD                                  IX2174.2
065500              AT END GO TO READ-TEST-F1-02-1.                     IX2174.2
065600     MOVE    IX-VS1R1-F-G-240 TO FILE-RECORD-INFO (1).            IX2174.2
065700     ADD     1  TO WRK-DU-09V00-001.                              IX2174.2
065800     IF      WRK-DU-09V00-001     GREATER 50                      IX2174.2
065900             MOVE "MORE THAN 50  RECORDS" TO RE-MARK              IX2174.2
066000              GO TO READ-TEST-F1-02-1.                            IX2174.2
066100     IF XRECORD-NUMBER (1) = WRK-DU-09V00-001                     IX2174.2
066200              GO TO READ-TEST-F1-02                               IX2174.2
066300         ELSE                                                     IX2174.2
066400              MOVE "WRONG RECORD NUMBER" TO RE-MARK               IX2174.2
066500              PERFORM FAIL                                        IX2174.2
066600              MOVE "READ (TO VERIFY)" TO FEATURE                  IX2174.2
066700              MOVE "READ-TEST-F1-02" TO PAR-NAME                  IX2174.2
066800              PERFORM PRINT-DETAIL                                IX2174.2
066900              GO TO READ-TEST-F1-02-3.                            IX2174.2
067000 READ-TEST-F1-02-1.                                               IX2174.2
067100     IF       XRECORD-NUMBER (1) NOT EQUAL TO 50                  IX2174.2
067200              PERFORM FAIL                                        IX2174.2
067300              ELSE                                                IX2174.2
067400              PERFORM PASS.                                       IX2174.2
067500 READ-TEST-F1-02-2.                                               IX2174.2
067600     MOVE     "READ  (TO VERIFY)   " TO FEATURE.                  IX2174.2
067700     MOVE     "READ-TEST-F1-02" TO PAR-NAME.                      IX2174.2
067800     MOVE     "FILE VERIFIED, LFILE" TO COMPUTED-A.               IX2174.2
067900     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  IX2174.2
068000     PERFORM  PRINT-DETAIL.                                       IX2174.2
068100 READ-TEST-F1-02-3.                                               IX2174.2
068200     CLOSE   IX-VS1.                                              IX2174.2
068300                                                                  IX2174.2
068400                                                                  IX2174.2
068500 CCVS-EXIT SECTION.                                               IX2174.2
068600 CCVS-999999.                                                     IX2174.2
068700     GO TO CLOSE-FILES.                                           IX2174.2
      *END-OF,IX217A                                                                  
