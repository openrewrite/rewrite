      *HEADER,COBOL,IX201A,SUBPRG,IX202A                                              
000100 IDENTIFICATION DIVISION.                                         IX2024.2
000200 PROGRAM-ID.                                                      IX2024.2
000300     IX202A.                                                      IX2024.2
000400****************************************************************  IX2024.2
000500*                                                              *  IX2024.2
000600*    VALIDATION FOR:-                                          *  IX2024.2
000700*                                                              *  IX2024.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2024.2
000900*                                                              *  IX2024.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2024.2
001100*                                                              *  IX2024.2
001200****************************************************************  IX2024.2
001300*    THE FUNCTION OF THIS PROGRAM IS TO PROCESS AN INDEXED FILE   IX2024.2
001400*    RANDOMLY USING THE ACCESS MODE IS DYNAMIC CLAUSE.  THE FILE  IX2024.2
001500*    USED AS INPUT IS THAT CREATED BY IX201A.                     IX2024.2
001600*                                                                 IX2024.2
001700*    FIRST THE FILE IS VERIFIED AS TO THE EXISTANCE AND ACCURACY  IX2024.2
001800*    OF THE 500 RECORDS CREATED IN THE FIRST RUN UNIT.  SECONDLY, IX2024.2
001900*    RECORDS OF THE FILE ARE SELECTIVELY UPDATED; AND THIRDLY, THEIX2024.2
002000*    ACCURACY OF EACH RECORD IN THE FILE IS AGAIN VERIFIED.       IX2024.2
002100*                                                                 IX2024.2
002200*                                                                 IX2024.2
002300*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2024.2
002400*                                                                 IX2024.2
002500*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2024.2
002600*                    CLAUSE FOR DATA FILE IX-FS1                  IX2024.2
002700*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2024.2
002800*                    CLAUSE FOR INDEX FILE IX-FS1                 IX2024.2
002900*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2024.2
003000*             X-62   FOR RAW-DATA                                 IX2024.2
003100*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2024.2
003200*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2024.2
003300*                                                                 IX2024.2
003400*         NOTE:  X-CARDS 44 AND 62       ARE OPTIONAL             IX2024.2
003500*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2024.2
003600*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2024.2
003700*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2024.2
003800*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2024.2
003900*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2024.2
004000*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2024.2
004100*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2024.2
004200*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2024.2
004300*               THEY ARE AS FOLLOWS                               IX2024.2
004400*                                                                 IX2024.2
004500*                  P  SELECTS X-CARDS 62                          IX2024.2
004600*                  J  SELECTS X-CARD 44                           IX2024.2
004700*                                                                 IX2024.2
004800******************************************************            IX2024.2
004900 ENVIRONMENT DIVISION.                                            IX2024.2
005000 CONFIGURATION SECTION.                                           IX2024.2
005100 SOURCE-COMPUTER.                                                 IX2024.2
005200     XXXXX082.                                                    IX2024.2
005300 OBJECT-COMPUTER.                                                 IX2024.2
005400     XXXXX083.                                                    IX2024.2
005500 INPUT-OUTPUT SECTION.                                            IX2024.2
005600 FILE-CONTROL.                                                    IX2024.2
005700*    SELECT RAW-DATA   ASSIGN TO                                  IX2024.2
005800*    XXXXX062                                                     IX2024.2
005900*           ORGANIZATION IS INDEXED                               IX2024.2
006000*           ACCESS MODE IS RANDOM                                 IX2024.2
006100*           RECORD KEY IS RAW-DATA-KEY.                           IX2024.2
006200     SELECT PRINT-FILE ASSIGN TO                                  IX2024.2
006300     XXXXX055.                                                    IX2024.2
006400     SELECT   IX-FD1 ASSIGN                                       IX2024.2
006500     XXXXP024                                                     IX2024.2
006600*    XXXXP044                                                     IX2024.2
006700        ACCESS MODE IS DYNAMIC                                    IX2024.2
006800        ; ORGANIZATION INDEXED                                    IX2024.2
006900      RECORD KEY IX-FD1-KEY.                                      IX2024.2
007000 DATA DIVISION.                                                   IX2024.2
007100 FILE SECTION.                                                    IX2024.2
007200*                                                                 IX2024.2
007300*FD  RAW-DATA.                                                    IX2024.2
007400*                                                                 IX2024.2
007500*01  RAW-DATA-SATZ.                                               IX2024.2
007600*    05  RAW-DATA-KEY        PIC X(6).                            IX2024.2
007700*    05  C-DATE              PIC 9(6).                            IX2024.2
007800*    05  C-TIME              PIC 9(8).                            IX2024.2
007900*    05  C-NO-OF-TESTS       PIC 99.                              IX2024.2
008000*    05  C-OK                PIC 999.                             IX2024.2
008100*    05  C-ALL               PIC 999.                             IX2024.2
008200*    05  C-FAIL              PIC 999.                             IX2024.2
008300*    05  C-DELETED           PIC 999.                             IX2024.2
008400*    05  C-INSPECT           PIC 999.                             IX2024.2
008500*    05  C-NOTE              PIC X(13).                           IX2024.2
008600*    05  C-INDENT            PIC X.                               IX2024.2
008700*    05  C-ABORT             PIC X(8).                            IX2024.2
008800 FD  PRINT-FILE.                                                  IX2024.2
008900 01  PRINT-REC PICTURE X(120).                                    IX2024.2
009000 01  DUMMY-RECORD PICTURE X(120).                                 IX2024.2
009100 FD  IX-FD1                                                       IX2024.2
009200C    LABEL RECORDS STANDARD                                       IX2024.2
009300C    DATA RECORD IX-FS1R1-F-G-240                                 IX2024.2
009400     BLOCK   1   RECORDS                                          IX2024.2
009500     RECORD    240  CHARACTERS.                                   IX2024.2
009600 01  IX-FS1R1-F-G-240.                                            IX2024.2
009700     05 IX-FD1-REC-120   PIC  X(120).                             IX2024.2
009800     05 IX-FD1-REC-120-240.                                       IX2024.2
009900        10 FILLER   PIC X(8).                                     IX2024.2
010000        10 IX-FD1-KEY   PIC X(29).                                IX2024.2
010100        10 FILLER        PIC X(83).                               IX2024.2
010200 WORKING-STORAGE SECTION.                                         IX2024.2
010300 01  WRK-CS-09V00 PIC S9(09)      USAGE COMP VALUE ZERO.          IX2024.2
010400 01  WRK-DS-09V00-002 PIC S9(9) VALUE ZERO.                       IX2024.2
010500 01  WRK-CS-09V00-002 PIC S9(09)       USAGE COMP VALUE ZERO.     IX2024.2
010600 01  I-O-ERROR-IX-FD1 PIC X(3) VALUE "NO ".                       IX2024.2
010700 01  WRK-CS-09V00-001 PIC S9(09)       USAGE COMP VALUE ZERO.     IX2024.2
010800 01  WRK-CS-09V00-004 PIC S9(09)       USAGE COMP VALUE ZERO.     IX2024.2
010900 01  WRK-CS-09V00-005 PIC S9(09)       USAGE COMP VALUE ZERO.     IX2024.2
011000 01  IX-WRK-KEY.                                                  IX2024.2
011100     02 FILLER   PIC  X(10)  VALUE "ABCDLKJXYZ".                  IX2024.2
011200     02  WRK-DU-09V00-001 PIC 9(9) VALUE ZERO.                    IX2024.2
011300     02 FILLER   PIC  X(10)  VALUE "ZIF,.$-+CD".                  IX2024.2
011400 01  DUMMY-WRK-REC.                                               IX2024.2
011500     02 DUMMY-WRK1       PIC X(120).                              IX2024.2
011600     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX2024.2
011700        03 FILLER   PIC X(5).                                     IX2024.2
011800        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX2024.2
011900 01  FILE-RECORD-INFORMATION-REC.                                 IX2024.2
012000     03 FILE-RECORD-INFO-SKELETON.                                IX2024.2
012100        05 FILLER                 PICTURE X(48)       VALUE       IX2024.2
012200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2024.2
012300        05 FILLER                 PICTURE X(46)       VALUE       IX2024.2
012400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2024.2
012500        05 FILLER                 PICTURE X(26)       VALUE       IX2024.2
012600             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2024.2
012700        05 FILLER                 PICTURE X(37)       VALUE       IX2024.2
012800             ",RECKEY=                             ".             IX2024.2
012900        05 FILLER                 PICTURE X(38)       VALUE       IX2024.2
013000             ",ALTKEY1=                             ".            IX2024.2
013100        05 FILLER                 PICTURE X(38)       VALUE       IX2024.2
013200             ",ALTKEY2=                             ".            IX2024.2
013300        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2024.2
013400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2024.2
013500        05 FILE-RECORD-INFO-P1-120.                               IX2024.2
013600           07 FILLER              PIC X(5).                       IX2024.2
013700           07 XFILE-NAME           PIC X(6).                      IX2024.2
013800           07 FILLER              PIC X(8).                       IX2024.2
013900           07 XRECORD-NAME         PIC X(6).                      IX2024.2
014000           07 FILLER              PIC X(1).                       IX2024.2
014100           07 REELUNIT-NUMBER     PIC 9(1).                       IX2024.2
014200           07 FILLER              PIC X(7).                       IX2024.2
014300           07 XRECORD-NUMBER       PIC 9(6).                      IX2024.2
014400           07 FILLER              PIC X(6).                       IX2024.2
014500           07 UPDATE-NUMBER       PIC 9(2).                       IX2024.2
014600           07 FILLER              PIC X(5).                       IX2024.2
014700           07 ODO-NUMBER          PIC 9(4).                       IX2024.2
014800           07 FILLER              PIC X(5).                       IX2024.2
014900           07 XPROGRAM-NAME        PIC X(5).                      IX2024.2
015000           07 FILLER              PIC X(7).                       IX2024.2
015100           07 XRECORD-LENGTH       PIC 9(6).                      IX2024.2
015200           07 FILLER              PIC X(7).                       IX2024.2
015300           07 CHARS-OR-RECORDS    PIC X(2).                       IX2024.2
015400           07 FILLER              PIC X(1).                       IX2024.2
015500           07 XBLOCK-SIZE          PIC 9(4).                      IX2024.2
015600           07 FILLER              PIC X(6).                       IX2024.2
015700           07 RECORDS-IN-FILE     PIC 9(6).                       IX2024.2
015800           07 FILLER              PIC X(5).                       IX2024.2
015900           07 XFILE-ORGANIZATION   PIC X(2).                      IX2024.2
016000           07 FILLER              PIC X(6).                       IX2024.2
016100           07 XLABEL-TYPE          PIC X(1).                      IX2024.2
016200        05 FILE-RECORD-INFO-P121-240.                             IX2024.2
016300           07 FILLER              PIC X(8).                       IX2024.2
016400           07 XRECORD-KEY          PIC X(29).                     IX2024.2
016500           07 FILLER              PIC X(9).                       IX2024.2
016600           07 ALTERNATE-KEY1      PIC X(29).                      IX2024.2
016700           07 FILLER              PIC X(9).                       IX2024.2
016800           07 ALTERNATE-KEY2      PIC X(29).                      IX2024.2
016900           07 FILLER              PIC X(7).                       IX2024.2
017000 01  TEST-RESULTS.                                                IX2024.2
017100     02 FILLER                   PIC X      VALUE SPACE.          IX2024.2
017200     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2024.2
017300     02 FILLER                   PIC X      VALUE SPACE.          IX2024.2
017400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2024.2
017500     02 FILLER                   PIC X      VALUE SPACE.          IX2024.2
017600     02  PAR-NAME.                                                IX2024.2
017700       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2024.2
017800       03  PARDOT-X              PIC X      VALUE SPACE.          IX2024.2
017900       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2024.2
018000     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2024.2
018100     02 RE-MARK                  PIC X(61).                       IX2024.2
018200 01  TEST-COMPUTED.                                               IX2024.2
018300     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2024.2
018400     02 FILLER                   PIC X(17)  VALUE                 IX2024.2
018500            "       COMPUTED=".                                   IX2024.2
018600     02 COMPUTED-X.                                               IX2024.2
018700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2024.2
018800     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2024.2
018900                                 PIC -9(9).9(9).                  IX2024.2
019000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2024.2
019100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2024.2
019200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2024.2
019300     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2024.2
019400         04 COMPUTED-18V0                    PIC -9(18).          IX2024.2
019500         04 FILLER                           PIC X.               IX2024.2
019600     03 FILLER PIC X(50) VALUE SPACE.                             IX2024.2
019700 01  TEST-CORRECT.                                                IX2024.2
019800     02 FILLER PIC X(30) VALUE SPACE.                             IX2024.2
019900     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2024.2
020000     02 CORRECT-X.                                                IX2024.2
020100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2024.2
020200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2024.2
020300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2024.2
020400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2024.2
020500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2024.2
020600     03      CR-18V0 REDEFINES CORRECT-A.                         IX2024.2
020700         04 CORRECT-18V0                     PIC -9(18).          IX2024.2
020800         04 FILLER                           PIC X.               IX2024.2
020900     03 FILLER PIC X(2) VALUE SPACE.                              IX2024.2
021000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2024.2
021100 01  CCVS-C-1.                                                    IX2024.2
021200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2024.2
021300-    "SS  PARAGRAPH-NAME                                          IX2024.2
021400-    "       REMARKS".                                            IX2024.2
021500     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2024.2
021600 01  CCVS-C-2.                                                    IX2024.2
021700     02 FILLER                     PIC X        VALUE SPACE.      IX2024.2
021800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2024.2
021900     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2024.2
022000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2024.2
022100     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2024.2
022200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2024.2
022300 01  REC-CT                        PIC 99       VALUE ZERO.       IX2024.2
022400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2024.2
022500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2024.2
022600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2024.2
022700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2024.2
022800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2024.2
022900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2024.2
023000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2024.2
023100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2024.2
023200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2024.2
023300 01  CCVS-H-1.                                                    IX2024.2
023400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2024.2
023500     02  FILLER                    PIC X(42)    VALUE             IX2024.2
023600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2024.2
023700     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2024.2
023800 01  CCVS-H-2A.                                                   IX2024.2
023900   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2024.2
024000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2024.2
024100   02  FILLER                        PIC XXXX   VALUE             IX2024.2
024200     "4.2 ".                                                      IX2024.2
024300   02  FILLER                        PIC X(28)  VALUE             IX2024.2
024400            " COPY - NOT FOR DISTRIBUTION".                       IX2024.2
024500   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2024.2
024600                                                                  IX2024.2
024700 01  CCVS-H-2B.                                                   IX2024.2
024800   02  FILLER                        PIC X(15)  VALUE             IX2024.2
024900            "TEST RESULT OF ".                                    IX2024.2
025000   02  TEST-ID                       PIC X(9).                    IX2024.2
025100   02  FILLER                        PIC X(4)   VALUE             IX2024.2
025200            " IN ".                                               IX2024.2
025300   02  FILLER                        PIC X(12)  VALUE             IX2024.2
025400     " HIGH       ".                                              IX2024.2
025500   02  FILLER                        PIC X(22)  VALUE             IX2024.2
025600            " LEVEL VALIDATION FOR ".                             IX2024.2
025700   02  FILLER                        PIC X(58)  VALUE             IX2024.2
025800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2024.2
025900 01  CCVS-H-3.                                                    IX2024.2
026000     02  FILLER                      PIC X(34)  VALUE             IX2024.2
026100            " FOR OFFICIAL USE ONLY    ".                         IX2024.2
026200     02  FILLER                      PIC X(58)  VALUE             IX2024.2
026300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2024.2
026400     02  FILLER                      PIC X(28)  VALUE             IX2024.2
026500            "  COPYRIGHT   1985 ".                                IX2024.2
026600 01  CCVS-E-1.                                                    IX2024.2
026700     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2024.2
026800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2024.2
026900     02 ID-AGAIN                     PIC X(9).                    IX2024.2
027000     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2024.2
027100 01  CCVS-E-2.                                                    IX2024.2
027200     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2024.2
027300     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2024.2
027400     02 CCVS-E-2-2.                                               IX2024.2
027500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2024.2
027600         03 FILLER                   PIC X      VALUE SPACE.      IX2024.2
027700         03 ENDER-DESC               PIC X(44)  VALUE             IX2024.2
027800            "ERRORS ENCOUNTERED".                                 IX2024.2
027900 01  CCVS-E-3.                                                    IX2024.2
028000     02  FILLER                      PIC X(22)  VALUE             IX2024.2
028100            " FOR OFFICIAL USE ONLY".                             IX2024.2
028200     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2024.2
028300     02  FILLER                      PIC X(58)  VALUE             IX2024.2
028400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2024.2
028500     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2024.2
028600     02 FILLER                       PIC X(15)  VALUE             IX2024.2
028700             " COPYRIGHT 1985".                                   IX2024.2
028800 01  CCVS-E-4.                                                    IX2024.2
028900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2024.2
029000     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2024.2
029100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2024.2
029200     02 FILLER                       PIC X(40)  VALUE             IX2024.2
029300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2024.2
029400 01  XXINFO.                                                      IX2024.2
029500     02 FILLER                       PIC X(19)  VALUE             IX2024.2
029600            "*** INFORMATION ***".                                IX2024.2
029700     02 INFO-TEXT.                                                IX2024.2
029800       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2024.2
029900       04 XXCOMPUTED                 PIC X(20).                   IX2024.2
030000       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2024.2
030100       04 XXCORRECT                  PIC X(20).                   IX2024.2
030200     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2024.2
030300 01  HYPHEN-LINE.                                                 IX2024.2
030400     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2024.2
030500     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2024.2
030600-    "*****************************************".                 IX2024.2
030700     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2024.2
030800-    "******************************".                            IX2024.2
030900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2024.2
031000     "IX202A".                                                    IX2024.2
031100 PROCEDURE DIVISION.                                              IX2024.2
031200 CCVS1 SECTION.                                                   IX2024.2
031300 OPEN-FILES.                                                      IX2024.2
031400*    OPEN I-O RAW-DATA.                                           IX2024.2
031500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2024.2
031600*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2024.2
031700*    MOVE "ABORTED " TO C-ABORT.                                  IX2024.2
031800*    ADD 1 TO C-NO-OF-TESTS.                                      IX2024.2
031900*    ACCEPT C-DATE  FROM DATE.                                    IX2024.2
032000*    ACCEPT C-TIME  FROM TIME.                                    IX2024.2
032100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2024.2
032200*END-E-1.                                                         IX2024.2
032300*    CLOSE RAW-DATA.                                              IX2024.2
032400     OPEN    OUTPUT PRINT-FILE.                                   IX2024.2
032500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2024.2
032600     MOVE    SPACE TO TEST-RESULTS.                               IX2024.2
032700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2024.2
032800     MOVE    ZERO TO REC-SKL-SUB.                                 IX2024.2
032900     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2024.2
033000 CCVS-INIT-FILE.                                                  IX2024.2
033100     ADD     1 TO REC-SKL-SUB.                                    IX2024.2
033200     MOVE    FILE-RECORD-INFO-SKELETON                            IX2024.2
033300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2024.2
033400 CCVS-INIT-EXIT.                                                  IX2024.2
033500     GO TO CCVS1-EXIT.                                            IX2024.2
033600 CLOSE-FILES.                                                     IX2024.2
033700*    OPEN I-O RAW-DATA.                                           IX2024.2
033800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2024.2
033900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2024.2
034000*    MOVE "OK.     " TO C-ABORT.                                  IX2024.2
034100*    MOVE PASS-COUNTER TO C-OK.                                   IX2024.2
034200*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2024.2
034300*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2024.2
034400*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2024.2
034500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2024.2
034600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2024.2
034700*END-E-2.                                                         IX2024.2
034800*    CLOSE RAW-DATA.                                              IX2024.2
034900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2024.2
035000 TERMINATE-CCVS.                                                  IX2024.2
035100*    EXIT PROGRAM.                                                IX2024.2
035200*TERMINATE-CALL.                                                  IX2024.2
035300     STOP     RUN.                                                IX2024.2
035400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2024.2
035500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2024.2
035600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2024.2
035700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2024.2
035800     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2024.2
035900 PRINT-DETAIL.                                                    IX2024.2
036000     IF REC-CT NOT EQUAL TO ZERO                                  IX2024.2
036100             MOVE "." TO PARDOT-X                                 IX2024.2
036200             MOVE REC-CT TO DOTVALUE.                             IX2024.2
036300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2024.2
036400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2024.2
036500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2024.2
036600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2024.2
036700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2024.2
036800     MOVE SPACE TO CORRECT-X.                                     IX2024.2
036900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2024.2
037000     MOVE     SPACE TO RE-MARK.                                   IX2024.2
037100 HEAD-ROUTINE.                                                    IX2024.2
037200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2024.2
037300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2024.2
037400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2024.2
037500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2024.2
037600 COLUMN-NAMES-ROUTINE.                                            IX2024.2
037700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2024.2
037800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2024.2
037900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2024.2
038000 END-ROUTINE.                                                     IX2024.2
038100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2024.2
038200 END-RTN-EXIT.                                                    IX2024.2
038300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2024.2
038400 END-ROUTINE-1.                                                   IX2024.2
038500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2024.2
038600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2024.2
038700      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2024.2
038800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2024.2
038900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2024.2
039000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2024.2
039100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2024.2
039200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2024.2
039300  END-ROUTINE-12.                                                 IX2024.2
039400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2024.2
039500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2024.2
039600         MOVE "NO " TO ERROR-TOTAL                                IX2024.2
039700         ELSE                                                     IX2024.2
039800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2024.2
039900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2024.2
040000     PERFORM WRITE-LINE.                                          IX2024.2
040100 END-ROUTINE-13.                                                  IX2024.2
040200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2024.2
040300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2024.2
040400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2024.2
040500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2024.2
040600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2024.2
040700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2024.2
040800          MOVE "NO " TO ERROR-TOTAL                               IX2024.2
040900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2024.2
041000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2024.2
041100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2024.2
041200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2024.2
041300 WRITE-LINE.                                                      IX2024.2
041400     ADD 1 TO RECORD-COUNT.                                       IX2024.2
041500*    IF RECORD-COUNT GREATER 42                                   IX2024.2
041600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2024.2
041700*        MOVE SPACE TO DUMMY-RECORD                               IX2024.2
041800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2024.2
041900*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2024.2
042000*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2024.2
042100*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2024.2
042200*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2024.2
042300*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2024.2
042400*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2024.2
042500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2024.2
042600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2024.2
042700*        MOVE ZERO TO RECORD-COUNT.                               IX2024.2
042800     PERFORM WRT-LN.                                              IX2024.2
042900 WRT-LN.                                                          IX2024.2
043000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2024.2
043100     MOVE SPACE TO DUMMY-RECORD.                                  IX2024.2
043200 BLANK-LINE-PRINT.                                                IX2024.2
043300     PERFORM WRT-LN.                                              IX2024.2
043400 FAIL-ROUTINE.                                                    IX2024.2
043500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2024.2
043600            GO TO   FAIL-ROUTINE-WRITE.                           IX2024.2
043700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2024.2
043800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2024.2
043900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2024.2
044000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2024.2
044100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2024.2
044200     GO TO  FAIL-ROUTINE-EX.                                      IX2024.2
044300 FAIL-ROUTINE-WRITE.                                              IX2024.2
044400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2024.2
044500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2024.2
044600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2024.2
044700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2024.2
044800 FAIL-ROUTINE-EX. EXIT.                                           IX2024.2
044900 BAIL-OUT.                                                        IX2024.2
045000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2024.2
045100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2024.2
045200 BAIL-OUT-WRITE.                                                  IX2024.2
045300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2024.2
045400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2024.2
045500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2024.2
045600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2024.2
045700 BAIL-OUT-EX. EXIT.                                               IX2024.2
045800 CCVS1-EXIT.                                                      IX2024.2
045900     EXIT.                                                        IX2024.2
046000 SECT-IX-02-001 SECTION.                                          IX2024.2
046100 READ-INIT-F2-01.                                                 IX2024.2
046200     OPEN    INPUT IX-FD1.                                        IX2024.2
046300     MOVE     "READ-TEST-F2-01" TO PAR-NAME.                      IX2024.2
046400     MOVE     ZERO TO WRK-DU-09V00-001.                           IX2024.2
046500     MOVE     IX-WRK-KEY  TO IX-FD1-KEY.                          IX2024.2
046600     MOVE     ZERO TO   WRK-CS-09V00-002                          IX2024.2
046700     MOVE     ZERO  TO  WRK-DU-09V00-001                          IX2024.2
046800     MOVE     "READ RANDOM   "  TO FEATURE.                       IX2024.2
046900 READ-TEST-F2-01-R.                                               IX2024.2
047000     ADD      1 TO WRK-DU-09V00-001                               IX2024.2
047100     MOVE     IX-WRK-KEY TO IX-FD1-KEY.                           IX2024.2
047200     IF       WRK-DU-09V00-001  GREATER  501                      IX2024.2
047300              MOVE "IX-28; FORMAT 2  " TO RE-MARK                 IX2024.2
047400              MOVE "INVALID KEY NOT TAKEN" TO COMPUTED-A          IX2024.2
047500              MOVE    WRK-DU-09V00-001  TO CORRECT-18V0           IX2024.2
047600              PERFORM FAIL                                        IX2024.2
047700              PERFORM PRINT-DETAIL                                IX2024.2
047800              GO TO READ-WRITE-F2-01.                             IX2024.2
047900     READ     IX-FD1                                              IX2024.2
048000              INVALID KEY GO TO READ-WRITE-F2-01.                 IX2024.2
048100     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX2024.2
048200     IF       XRECORD-NUMBER (1) EQUAL TO WRK-DU-09V00-001        IX2024.2
048300              GO TO READ-TEST-F2-01-R.                            IX2024.2
048400     MOVE     "YES" TO I-O-ERROR-IX-FD1.                          IX2024.2
048500     ADD      1 TO WRK-CS-09V00-002                               IX2024.2
048600     GO TO    READ-TEST-F2-01-R.                                  IX2024.2
048700 READ-WRITE-F2-01.                                                IX2024.2
048800     IF       WRK-DU-09V00-001  NOT EQUAL TO 501                  IX2024.2
048900              MOVE "IX-28; FORMAT 2  " TO RE-MARK                 IX2024.2
049000              MOVE "WRONG KEY/NOT 500" TO CORRECT-A               IX2024.2
049100              MOVE    WRK-DU-09V00-001  TO  COMPUTED-18V0         IX2024.2
049200              PERFORM FAIL                                        IX2024.2
049300              ELSE                                                IX2024.2
049400              PERFORM PASS.                                       IX2024.2
049500     PERFORM PRINT-DETAIL.                                        IX2024.2
049600 READ-TEST-F2-01-1.                                               IX2024.2
049700     MOVE "READ-TEST-F2-01-1" TO PAR-NAME.                        IX2024.2
049800     MOVE "READ TOO LESS RECORDS" TO RE-MARK.                     IX2024.2
049900     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 IX2024.2
050000              MOVE "IX-28; FORMAT 2  " TO RE-MARK                 IX2024.2
050100              MOVE "WRONG RECORD/NOT 500" TO CORRECT-A            IX2024.2
050200              MOVE  XRECORD-NUMBER (1) TO COMPUTED-18V0           IX2024.2
050300              PERFORM FAIL                                        IX2024.2
050400              ELSE                                                IX2024.2
050500              PERFORM PASS.                                       IX2024.2
050600     PERFORM PRINT-DETAIL.                                        IX2024.2
050700 READ-TEST-F2-01-2.                                               IX2024.2
050800     MOVE "READ-TEST-F2-01-2" TO PAR-NAME.                        IX2024.2
050900     MOVE "READ TOO MUCH RECORDS" TO RE-MARK.                     IX2024.2
051000     IF       WRK-DU-09V00-001 NOT EQUAL TO 501                   IX2024.2
051100              MOVE "IX-28; FORMAT 2  " TO RE-MARK                 IX2024.2
051200              MOVE "INCORRECT RECORD COUNT" TO RE-MARK            IX2024.2
051300              MOVE  WRK-DU-09V00-001 TO COMPUTED-18V0             IX2024.2
051400              MOVE 501  TO CORRECT-18V0                           IX2024.2
051500              PERFORM FAIL                                        IX2024.2
051600              ELSE                                                IX2024.2
051700              PERFORM PASS.                                       IX2024.2
051800     PERFORM  PRINT-DETAIL.                                       IX2024.2
051900 READ-TEST-F2-01-3.                                               IX2024.2
052000     MOVE "READ-TEST-F2-01-3" TO PAR-NAME.                        IX2024.2
052100     MOVE "READ WRONG    RECORDS" TO RE-MARK.                     IX2024.2
052200     IF       I-O-ERROR-IX-FD1 EQUAL TO "YES"                     IX2024.2
052300              MOVE "IX-28; FORMAT 2  " TO RE-MARK                 IX2024.2
052400              MOVE WRK-CS-09V00-002 TO COMPUTED-18V0              IX2024.2
052500              MOVE "RECORDS DID NOT COMPARE" TO RE-MARK           IX2024.2
052600              PERFORM FAIL                                        IX2024.2
052700              ELSE                                                IX2024.2
052800              PERFORM PASS.                                       IX2024.2
052900     PERFORM  PRINT-DETAIL.                                       IX2024.2
053000     CLOSE    IX-FD1.                                             IX2024.2
053100*                                                                 IX2024.2
053200*  U P D A T E      READ & REWRITE                                IX2024.2
053300*                                                                 IX2024.2
053400 RWRT-INIT-GF-01-R .                                              IX2024.2
053500     MOVE     "RWRT-TEST-GF-01" TO PAR-NAME.                      IX2024.2
053600     MOVE     "REWRITE   "   TO FEATURE.                          IX2024.2
053700     OPEN     I-O IX-FD1.                                         IX2024.2
053800     MOVE     ZERO TO IX-FD1-KEY.                                 IX2024.2
053900     MOVE     ZERO TO WRK-CS-09V00-002.                           IX2024.2
054000     MOVE     ZERO TO WRK-DU-09V00-001.                           IX2024.2
054100     MOVE     SPACE TO  FILE-RECORD-INFO (1).                     IX2024.2
054200 RWRT-TEST-GF-01-R.                                               IX2024.2
054300     ADD      5 TO  WRK-DU-09V00-001.                             IX2024.2
054400     MOVE     IX-WRK-KEY TO IX-FD1-KEY.                           IX2024.2
054500     IF       WRK-DU-09V00-001  GREATER 505                       IX2024.2
054600              MOVE "INVALID KEY/NOT TAKEN" TO COMPUTED-A          IX2024.2
054700              MOVE    WRK-DU-09V00-001  TO CORRECT-18V0           IX2024.2
054800              PERFORM FAIL                                        IX2024.2
054900              PERFORM PRINT-DETAIL                                IX2024.2
055000              GO TO RWRT-TEST-GF-01-3.                            IX2024.2
055100     READ     IX-FD1                                              IX2024.2
055200              INVALID KEY GO TO RWRT-TEST-GF-01-1.                IX2024.2
055300     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1)            IX2024.2
055400     ADD      01 TO UPDATE-NUMBER (1).                            IX2024.2
055500     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   IX2024.2
055600     MOVE     FILE-RECORD-INFO (1) TO IX-FS1R1-F-G-240.           IX2024.2
055700     REWRITE  IX-FS1R1-F-G-240                                    IX2024.2
055800              INVALID KEY GO TO RWRT-TEST-GF-01-2.                IX2024.2
055900     GO TO    RWRT-TEST-GF-01-R.                                  IX2024.2
056000 RWRT-TEST-GF-01-1.                                               IX2024.2
056100     MOVE     "RWRT-TEST-GF-01-1" TO PAR-NAME.                    IX2024.2
056200     MOVE     "READ INVALID" TO FEATURE.                          IX2024.2
056300     IF       WRK-DU-09V00-001  LESS THAN 501                     IX2024.2
056400              ADD 1 TO  WRK-CS-09V00-001                          IX2024.2
056500              GO TO RWRT-TEST-GF-01-R.                            IX2024.2
056600     PERFORM  PASS.                                               IX2024.2
056700     PERFORM  PRINT-DETAIL.                                       IX2024.2
056800     GO TO    RWRT-TEST-GF-01-3.                                  IX2024.2
056900 RWRT-TEST-GF-01-2.                                               IX2024.2
057000     ADD      1 TO WRK-CS-09V00-005.                              IX2024.2
057100     IF       WRK-DU-09V00-001  LESS THAN 501                     IX2024.2
057200              GO TO RWRT-TEST-GF-01-R.                            IX2024.2
057300 RWRT-TEST-GF-01-3.                                               IX2024.2
057400     MOVE     "RWRT-TEST-GF-03-1" TO PAR-NAME.                    IX2024.2
057500     MOVE     "READ INVALID" TO FEATURE.                          IX2024.2
057600     IF       WRK-CS-09V00-004 NOT EQUAL TO ZERO                  IX2024.2
057700              MOVE "IX-28; FORMAT 2  " TO RE-MARK                 IX2024.2
057800              MOVE "INVALID KEY ON READ" TO COMPUTED-A            IX2024.2
057900              MOVE WRK-CS-09V00-004 TO CORRECT-18V0               IX2024.2
058000              PERFORM FAIL                                        IX2024.2
058100              ELSE                                                IX2024.2
058200              PERFORM PASS.                                       IX2024.2
058300     PERFORM  PRINT-DETAIL.                                       IX2024.2
058400 RWRT-TEST-GF-02-1.                                               IX2024.2
058500     MOVE     "RWRT-TEST-GF-02-1" TO PAR-NAME.                    IX2024.2
058600     MOVE     "REWRITE   "   TO FEATURE.                          IX2024.2
058700     IF       WRK-CS-09V00-005 NOT EQUAL TO ZERO                  IX2024.2
058800              MOVE "IX-33; 4.6.2     " TO RE-MARK                 IX2024.2
058900              MOVE "INVALID KEY ON REWRITE" TO COMPUTED-A         IX2024.2
059000              MOVE  WRK-CS-09V00-005 TO CORRECT-18V0              IX2024.2
059100              PERFORM FAIL                                        IX2024.2
059200              ELSE                                                IX2024.2
059300              PERFORM PASS.                                       IX2024.2
059400     PERFORM  PRINT-DETAIL.                                       IX2024.2
059500     CLOSE    IX-FD1.                                             IX2024.2
059600 READ-INIT-F2-02.                                                 IX2024.2
059700     MOVE     "READ-TEST-F2-02" TO PAR-NAME.                      IX2024.2
059800     MOVE     "READ   "   TO FEATURE.                             IX2024.2
059900     OPEN     INPUT  IX-FD1.                                      IX2024.2
060000     MOVE     501  TO WRK-DU-09V00-001.                           IX2024.2
060100     MOVE     ZERO TO WRK-CS-09V00-004.                           IX2024.2
060200     MOVE     ZERO TO WRK-CS-09V00-005.                           IX2024.2
060300     MOVE     ZERO TO WRK-CS-09V00-002.                           IX2024.2
060400     MOVE     SPACE TO  FILE-RECORD-INFO (1).                     IX2024.2
060500 READ-TEST-F2-02-R.                                               IX2024.2
060600     IF       WRK-DU-09V00-001  EQUAL TO ZERO                     IX2024.2
060700              MOVE "INVALID KEY/NOT TAKEN" TO COMPUTED-A          IX2024.2
060800              MOVE    WRK-DU-09V00-001  TO  COMPUTED-18V0         IX2024.2
060900              MOVE    ZERO TO CORRECT-18V0                        IX2024.2
061000              PERFORM FAIL                                        IX2024.2
061100              PERFORM PRINT-DETAIL                                IX2024.2
061200              GO TO READ-TEST-F2-02-1-0.                          IX2024.2
061300     SUBTRACT 1  FROM WRK-DU-09V00-001.                           IX2024.2
061400     MOVE     IX-WRK-KEY TO IX-FD1-KEY.                           IX2024.2
061500     READ     IX-FD1                                              IX2024.2
061600              INVALID KEY  GO TO READ-TEST-F2-02-1.               IX2024.2
061700     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX2024.2
061800     IF       UPDATE-NUMBER (1) EQUAL TO 00                       IX2024.2
061900              ADD 1 TO WRK-CS-09V00-004.                          IX2024.2
062000     IF       UPDATE-NUMBER (1) EQUAL TO 01                       IX2024.2
062100              ADD 1 TO WRK-CS-09V00-005.                          IX2024.2
062200     GO TO    READ-TEST-F2-02-R.                                  IX2024.2
062300 READ-TEST-F2-02-1.                                               IX2024.2
062400     IF       WRK-DU-09V00-001  GREATER ZERO                      IX2024.2
062500              ADD 1 TO WRK-CS-09V00-002                           IX2024.2
062600              GO TO READ-TEST-F2-02-R.                            IX2024.2
062700     PERFORM  PASS.                                               IX2024.2
062800     PERFORM  PRINT-DETAIL.                                       IX2024.2
062900 READ-TEST-F2-02-1-0.                                             IX2024.2
063000     MOVE     "READ-TEST-F2-02-1  " TO PAR-NAME.                  IX2024.2
063100     MOVE     "READ   "   TO FEATURE.                             IX2024.2
063200     IF       WRK-CS-09V00-004 NOT EQUAL TO 400                   IX2024.2
063300              MOVE "NON-UPDATED RECORDS" TO COMPUTED-A            IX2024.2
063400              MOVE WRK-CS-09V00-004 TO CORRECT-18V0               IX2024.2
063500              MOVE "SHOULD BE 400" TO RE-MARK                     IX2024.2
063600              PERFORM FAIL                                        IX2024.2
063700              ELSE                                                IX2024.2
063800              PERFORM PASS.                                       IX2024.2
063900     PERFORM  PRINT-DETAIL.                                       IX2024.2
064000 READ-TEST-F2-02-2.                                               IX2024.2
064100     MOVE     "READ-TEST-F2-02-2" TO PAR-NAME.                    IX2024.2
064200     MOVE     "READ   "   TO FEATURE.                             IX2024.2
064300     IF       WRK-CS-09V00-005 NOT EQUAL TO 100                   IX2024.2
064400              MOVE "UPDATED RECORDS" TO COMPUTED-A                IX2024.2
064500              MOVE WRK-CS-09V00-005 TO CORRECT-18V0               IX2024.2
064600              MOVE "SHOULD BE 100" TO RE-MARK                     IX2024.2
064700              PERFORM FAIL                                        IX2024.2
064800              ELSE                                                IX2024.2
064900              PERFORM PASS.                                       IX2024.2
065000     PERFORM  PRINT-DETAIL.                                       IX2024.2
065100 READ-TEST-F2-02-3.                                               IX2024.2
065200     MOVE     "READ-TEST-F2-02-3" TO PAR-NAME.                    IX2024.2
065300     MOVE     "READ   "   TO FEATURE.                             IX2024.2
065400     IF       WRK-CS-09V00-002 GREATER 1                          IX2024.2
065500              MOVE WRK-CS-09V00-002 TO COMPUTED-N                 IX2024.2
065600              MOVE  "INVALID KEY/READS" TO CORRECT-A              IX2024.2
065700              PERFORM FAIL                                        IX2024.2
065800              ELSE                                                IX2024.2
065900              PERFORM PASS.                                       IX2024.2
066000     PERFORM PRINT-DETAIL.                                        IX2024.2
066100     CLOSE    IX-FD1.                                             IX2024.2
066200 CCVS-EXIT SECTION.                                               IX2024.2
066300 CCVS-999999.                                                     IX2024.2
066400     GO TO CLOSE-FILES.                                           IX2024.2
      *END-OF,IX202A                                                                  
