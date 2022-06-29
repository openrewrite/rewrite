      *HEADER,COBOL,IX101A,SUBPRG,IX102A                                              
000100 IDENTIFICATION DIVISION.                                         IX1024.2
000200 PROGRAM-ID.                                                      IX1024.2
000300     IX102A.                                                      IX1024.2
000400****************************************************************  IX1024.2
000500*                                                              *  IX1024.2
000600*    VALIDATION FOR:-                                          *  IX1024.2
000700*                                                              *  IX1024.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1024.2
000900*                                                              *  IX1024.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1024.2
001100*                                                              *  IX1024.2
001200****************************************************************  IX1024.2
001300*                                                                 IX1024.2
001400*         NEW TEST:                                               IX1024.2
001500*                  SELECT ... ASSIGN TO <LITERAL-1> ...           IX1024.2
001600*                                                                 IX1024.2
001700*         NOTE: <LITERAL-1> WILL BE ASSIGNED BY THE X-CARD X-24.  IX1024.2
001800*               X-24 SHOULD ASSIGN  A <LITERAL-1>}                IX1024.2
001900*                                                                 IX1024.2
002000*                                                                 IX1024.2
002100*    THE FUNCTION OF THIS PROGRAM IS TO PROCESS AN INDEXED FILE   IX1024.2
002200*    RANDOMLY (ACCESS MODE IS RANDOM).  THE FILE USED AS INPUT IS IX1024.2
002300*    THAT CREATED BY IX101.                                       IX1024.2
002400*                                                                 IX1024.2
002500*    FIRST THE FILE IS VERIFIED AS TO THE EXISTANCE AND ACCURACY  IX1024.2
002600*    OF THE 500 RECORDS CREATED IN IX101.  SECONDLY, RECORDS      IX1024.2
002700*    OF THE FILE ARE SELECTIVELY UPDATED; AND THIRDLY, THE        IX1024.2
002800*    ACCURACY OF EACH RECORD IN THE FILE IS AGAIN VERIFIED.       IX1024.2
002900*                                                                 IX1024.2
003000*                                                                 IX1024.2
003100*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX1024.2
003200*                                                                 IX1024.2
003300*             X-24   INDEXED FILE <LITERAL-1>      IN ASSGN TO    IX1024.2
003400*                    CLAUSE FOR DATA FILE IX-FS1                  IX1024.2
003500*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1024.2
003600*                    CLAUSE FOR INDEX FILE IX-FS1                 IX1024.2
003700*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX1024.2
003800*             X-62   IMPLEMENTOR-NAME FOR RAW-DATA                IX1024.2
003900*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX1024.2
004000*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX1024.2
004100*                                                                 IX1024.2
004200*         NOTE:  X-CARDS 44 AND 62          ARE OPTIONAL          IX1024.2
004300*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX1024.2
004400*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX1024.2
004500*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX1024.2
004600*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX1024.2
004700*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX1024.2
004800*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX1024.2
004900*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX1024.2
005000*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX1024.2
005100*               THEY ARE AS FOLLOWS                               IX1024.2
005200*                                                                 IX1024.2
005300*                  J  SELECTS X-CARD 44                           IX1024.2
005400*                  P  SELECTS X-CARD 62                           IX1024.2
005500*                                                                 IX1024.2
005600******************************************************            IX1024.2
005700 ENVIRONMENT DIVISION.                                            IX1024.2
005800 CONFIGURATION SECTION.                                           IX1024.2
005900 SOURCE-COMPUTER.                                                 IX1024.2
006000     XXXXX082.                                                    IX1024.2
006100 OBJECT-COMPUTER.                                                 IX1024.2
006200     XXXXX083.                                                    IX1024.2
006300 INPUT-OUTPUT SECTION.                                            IX1024.2
006400 FILE-CONTROL.                                                    IX1024.2
006500*    SELECT RAW-DATA   ASSIGN TO                                  IX1024.2
006600*    XXXXX062                                                     IX1024.2
006700*           ORGANIZATION IS INDEXED                               IX1024.2
006800*           ACCESS MODE IS RANDOM                                 IX1024.2
006900*           RECORD KEY IS RAW-DATA-KEY.                           IX1024.2
007000     SELECT PRINT-FILE ASSIGN TO                                  IX1024.2
007100     XXXXX055.                                                    IX1024.2
007200     SELECT   IX-FS1 ASSIGN                                       IX1024.2
007300     XXXXP024                                                     IX1024.2
007400*    XXXXP044                                                     IX1024.2
007500        ACCESS MODE IS RANDOM                                     IX1024.2
007600        ORGANIZATION INDEXED                                      IX1024.2
007700     RECORD KEY IX-FS1-KEY.                                       IX1024.2
007800 DATA DIVISION.                                                   IX1024.2
007900 FILE SECTION.                                                    IX1024.2
008000*                                                                 IX1024.2
008100*FD  RAW-DATA.                                                    IX1024.2
008200*                                                                 IX1024.2
008300*01  RAW-DATA-SATZ.                                               IX1024.2
008400*    05  RAW-DATA-KEY        PIC X(6).                            IX1024.2
008500*    05  C-DATE              PIC 9(6).                            IX1024.2
008600*    05  C-TIME              PIC 9(8).                            IX1024.2
008700*    05  C-NO-OF-TESTS       PIC 99.                              IX1024.2
008800*    05  C-OK                PIC 999.                             IX1024.2
008900*    05  C-ALL               PIC 999.                             IX1024.2
009000*    05  C-FAIL              PIC 999.                             IX1024.2
009100*    05  C-DELETED           PIC 999.                             IX1024.2
009200*    05  C-INSPECT           PIC 999.                             IX1024.2
009300*    05  C-NOTE              PIC X(13).                           IX1024.2
009400*    05  C-INDENT            PIC X.                               IX1024.2
009500*    05  C-ABORT             PIC X(8).                            IX1024.2
009600 FD  PRINT-FILE.                                                  IX1024.2
009700 01  PRINT-REC PICTURE X(120).                                    IX1024.2
009800 01  DUMMY-RECORD PICTURE X(120).                                 IX1024.2
009900 FD  IX-FS1                                                       IX1024.2
010000C    LABEL RECORDS STANDARD                                       IX1024.2
010100C    ; DATA RECORD IX-FS1R1-F-G-240                               IX1024.2
010200     BLOCK   1   RECORDS                                          IX1024.2
010300     RECORD    240  CHARACTERS.                                   IX1024.2
010400 01  IX-FS1R1-F-G-240.                                            IX1024.2
010500     05 IX-FS1-REC-120   PIC  X(120).                             IX1024.2
010600     05 IX-FS1-REC-120-240.                                       IX1024.2
010700        10 FILLER   PIC X(8).                                     IX1024.2
010800        10 IX-FS1-KEY   PIC X(29).                                IX1024.2
010900        10 FILLER        PIC X(83).                               IX1024.2
011000 WORKING-STORAGE SECTION.                                         IX1024.2
011100 01  WRK-CS-09V00 PIC S9(09)      USAGE COMP VALUE ZERO.          IX1024.2
011200 01  WRK-DS-09V00-002 PIC S9(9) VALUE ZERO.                       IX1024.2
011300 01  WRK-CS-09V00-002 PIC S9(09)       USAGE COMP VALUE ZERO.     IX1024.2
011400 01  I-O-ERROR-IX-FS1 PIC X(3) VALUE "NO ".                       IX1024.2
011500 01  WRK-CS-09V00-001 PIC S9(09)       USAGE COMP VALUE ZERO.     IX1024.2
011600 01  WRK-CS-09V00-004 PIC S9(09)       USAGE COMP VALUE ZERO.     IX1024.2
011700 01  WRK-CS-09V00-005 PIC S9(09)       USAGE COMP VALUE ZERO.     IX1024.2
011800 01  IX-WRK-KEY.                                                  IX1024.2
011900     02 FILLER   PIC  X(10)  VALUE "ABCDLKJXYZ".                  IX1024.2
012000     02  WRK-DU-09V00-001 PIC 9(9) VALUE ZERO.                    IX1024.2
012100     02 FILLER   PIC  X(10)  VALUE "ZIF,.$-+CD".                  IX1024.2
012200 01  DUMMY-WRK-REC.                                               IX1024.2
012300     02 DUMMY-WRK1       PIC X(120).                              IX1024.2
012400     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX1024.2
012500        03 FILLER   PIC X(5).                                     IX1024.2
012600        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX1024.2
012700 01  FILE-RECORD-INFORMATION-REC.                                 IX1024.2
012800     03 FILE-RECORD-INFO-SKELETON.                                IX1024.2
012900        05 FILLER                 PICTURE X(48)       VALUE       IX1024.2
013000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1024.2
013100        05 FILLER                 PICTURE X(46)       VALUE       IX1024.2
013200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1024.2
013300        05 FILLER                 PICTURE X(26)       VALUE       IX1024.2
013400             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1024.2
013500        05 FILLER                 PICTURE X(37)       VALUE       IX1024.2
013600             ",RECKEY=                             ".             IX1024.2
013700        05 FILLER                 PICTURE X(38)       VALUE       IX1024.2
013800             ",ALTKEY1=                             ".            IX1024.2
013900        05 FILLER                 PICTURE X(38)       VALUE       IX1024.2
014000             ",ALTKEY2=                             ".            IX1024.2
014100        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1024.2
014200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1024.2
014300        05 FILE-RECORD-INFO-P1-120.                               IX1024.2
014400           07 FILLER              PIC X(5).                       IX1024.2
014500           07 XFILE-NAME           PIC X(6).                      IX1024.2
014600           07 FILLER              PIC X(8).                       IX1024.2
014700           07 XRECORD-NAME         PIC X(6).                      IX1024.2
014800           07 FILLER              PIC X(1).                       IX1024.2
014900           07 REELUNIT-NUMBER     PIC 9(1).                       IX1024.2
015000           07 FILLER              PIC X(7).                       IX1024.2
015100           07 XRECORD-NUMBER       PIC 9(6).                      IX1024.2
015200           07 FILLER              PIC X(6).                       IX1024.2
015300           07 UPDATE-NUMBER       PIC 9(2).                       IX1024.2
015400           07 FILLER              PIC X(5).                       IX1024.2
015500           07 ODO-NUMBER          PIC 9(4).                       IX1024.2
015600           07 FILLER              PIC X(5).                       IX1024.2
015700           07 XPROGRAM-NAME        PIC X(5).                      IX1024.2
015800           07 FILLER              PIC X(7).                       IX1024.2
015900           07 XRECORD-LENGTH       PIC 9(6).                      IX1024.2
016000           07 FILLER              PIC X(7).                       IX1024.2
016100           07 CHARS-OR-RECORDS    PIC X(2).                       IX1024.2
016200           07 FILLER              PIC X(1).                       IX1024.2
016300           07 XBLOCK-SIZE          PIC 9(4).                      IX1024.2
016400           07 FILLER              PIC X(6).                       IX1024.2
016500           07 RECORDS-IN-FILE     PIC 9(6).                       IX1024.2
016600           07 FILLER              PIC X(5).                       IX1024.2
016700           07 XFILE-ORGANIZATION   PIC X(2).                      IX1024.2
016800           07 FILLER              PIC X(6).                       IX1024.2
016900           07 XLABEL-TYPE          PIC X(1).                      IX1024.2
017000        05 FILE-RECORD-INFO-P121-240.                             IX1024.2
017100           07 FILLER              PIC X(8).                       IX1024.2
017200           07 XRECORD-KEY          PIC X(29).                     IX1024.2
017300           07 FILLER              PIC X(9).                       IX1024.2
017400           07 ALTERNATE-KEY1      PIC X(29).                      IX1024.2
017500           07 FILLER              PIC X(9).                       IX1024.2
017600           07 ALTERNATE-KEY2      PIC X(29).                      IX1024.2
017700           07 FILLER              PIC X(7).                       IX1024.2
017800 01  TEST-RESULTS.                                                IX1024.2
017900     02 FILLER                   PIC X      VALUE SPACE.          IX1024.2
018000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1024.2
018100     02 FILLER                   PIC X      VALUE SPACE.          IX1024.2
018200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1024.2
018300     02 FILLER                   PIC X      VALUE SPACE.          IX1024.2
018400     02  PAR-NAME.                                                IX1024.2
018500       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1024.2
018600       03  PARDOT-X              PIC X      VALUE SPACE.          IX1024.2
018700       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1024.2
018800     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1024.2
018900     02 RE-MARK                  PIC X(61).                       IX1024.2
019000 01  TEST-COMPUTED.                                               IX1024.2
019100     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1024.2
019200     02 FILLER                   PIC X(17)  VALUE                 IX1024.2
019300            "       COMPUTED=".                                   IX1024.2
019400     02 COMPUTED-X.                                               IX1024.2
019500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1024.2
019600     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1024.2
019700                                 PIC -9(9).9(9).                  IX1024.2
019800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1024.2
019900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1024.2
020000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1024.2
020100     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1024.2
020200         04 COMPUTED-18V0                    PIC -9(18).          IX1024.2
020300         04 FILLER                           PIC X.               IX1024.2
020400     03 FILLER PIC X(50) VALUE SPACE.                             IX1024.2
020500 01  TEST-CORRECT.                                                IX1024.2
020600     02 FILLER PIC X(30) VALUE SPACE.                             IX1024.2
020700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1024.2
020800     02 CORRECT-X.                                                IX1024.2
020900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1024.2
021000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1024.2
021100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1024.2
021200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1024.2
021300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1024.2
021400     03      CR-18V0 REDEFINES CORRECT-A.                         IX1024.2
021500         04 CORRECT-18V0                     PIC -9(18).          IX1024.2
021600         04 FILLER                           PIC X.               IX1024.2
021700     03 FILLER PIC X(2) VALUE SPACE.                              IX1024.2
021800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1024.2
021900 01  CCVS-C-1.                                                    IX1024.2
022000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1024.2
022100-    "SS  PARAGRAPH-NAME                                          IX1024.2
022200-    "       REMARKS".                                            IX1024.2
022300     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1024.2
022400 01  CCVS-C-2.                                                    IX1024.2
022500     02 FILLER                     PIC X        VALUE SPACE.      IX1024.2
022600     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1024.2
022700     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1024.2
022800     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1024.2
022900     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1024.2
023000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1024.2
023100 01  REC-CT                        PIC 99       VALUE ZERO.       IX1024.2
023200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1024.2
023300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1024.2
023400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1024.2
023500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1024.2
023600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1024.2
023700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1024.2
023800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1024.2
023900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1024.2
024000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1024.2
024100 01  CCVS-H-1.                                                    IX1024.2
024200     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1024.2
024300     02  FILLER                    PIC X(42)    VALUE             IX1024.2
024400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1024.2
024500     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1024.2
024600 01  CCVS-H-2A.                                                   IX1024.2
024700   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1024.2
024800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1024.2
024900   02  FILLER                        PIC XXXX   VALUE             IX1024.2
025000     "4.2 ".                                                      IX1024.2
025100   02  FILLER                        PIC X(28)  VALUE             IX1024.2
025200            " COPY - NOT FOR DISTRIBUTION".                       IX1024.2
025300   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1024.2
025400                                                                  IX1024.2
025500 01  CCVS-H-2B.                                                   IX1024.2
025600   02  FILLER                        PIC X(15)  VALUE             IX1024.2
025700            "TEST RESULT OF ".                                    IX1024.2
025800   02  TEST-ID                       PIC X(9).                    IX1024.2
025900   02  FILLER                        PIC X(4)   VALUE             IX1024.2
026000            " IN ".                                               IX1024.2
026100   02  FILLER                        PIC X(12)  VALUE             IX1024.2
026200     " HIGH       ".                                              IX1024.2
026300   02  FILLER                        PIC X(22)  VALUE             IX1024.2
026400            " LEVEL VALIDATION FOR ".                             IX1024.2
026500   02  FILLER                        PIC X(58)  VALUE             IX1024.2
026600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1024.2
026700 01  CCVS-H-3.                                                    IX1024.2
026800     02  FILLER                      PIC X(34)  VALUE             IX1024.2
026900            " FOR OFFICIAL USE ONLY    ".                         IX1024.2
027000     02  FILLER                      PIC X(58)  VALUE             IX1024.2
027100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1024.2
027200     02  FILLER                      PIC X(28)  VALUE             IX1024.2
027300            "  COPYRIGHT   1985 ".                                IX1024.2
027400 01  CCVS-E-1.                                                    IX1024.2
027500     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1024.2
027600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1024.2
027700     02 ID-AGAIN                     PIC X(9).                    IX1024.2
027800     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1024.2
027900 01  CCVS-E-2.                                                    IX1024.2
028000     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1024.2
028100     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1024.2
028200     02 CCVS-E-2-2.                                               IX1024.2
028300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1024.2
028400         03 FILLER                   PIC X      VALUE SPACE.      IX1024.2
028500         03 ENDER-DESC               PIC X(44)  VALUE             IX1024.2
028600            "ERRORS ENCOUNTERED".                                 IX1024.2
028700 01  CCVS-E-3.                                                    IX1024.2
028800     02  FILLER                      PIC X(22)  VALUE             IX1024.2
028900            " FOR OFFICIAL USE ONLY".                             IX1024.2
029000     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1024.2
029100     02  FILLER                      PIC X(58)  VALUE             IX1024.2
029200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1024.2
029300     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1024.2
029400     02 FILLER                       PIC X(15)  VALUE             IX1024.2
029500             " COPYRIGHT 1985".                                   IX1024.2
029600 01  CCVS-E-4.                                                    IX1024.2
029700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1024.2
029800     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1024.2
029900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1024.2
030000     02 FILLER                       PIC X(40)  VALUE             IX1024.2
030100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1024.2
030200 01  XXINFO.                                                      IX1024.2
030300     02 FILLER                       PIC X(19)  VALUE             IX1024.2
030400            "*** INFORMATION ***".                                IX1024.2
030500     02 INFO-TEXT.                                                IX1024.2
030600       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1024.2
030700       04 XXCOMPUTED                 PIC X(20).                   IX1024.2
030800       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1024.2
030900       04 XXCORRECT                  PIC X(20).                   IX1024.2
031000     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1024.2
031100 01  HYPHEN-LINE.                                                 IX1024.2
031200     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1024.2
031300     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1024.2
031400-    "*****************************************".                 IX1024.2
031500     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1024.2
031600-    "******************************".                            IX1024.2
031700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1024.2
031800     "IX102A".                                                    IX1024.2
031900 PROCEDURE DIVISION.                                              IX1024.2
032000 CCVS1 SECTION.                                                   IX1024.2
032100 OPEN-FILES.                                                      IX1024.2
032200*    OPEN I-O RAW-DATA.                                           IX1024.2
032300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1024.2
032400*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1024.2
032500*    MOVE "ABORTED " TO C-ABORT.                                  IX1024.2
032600*    ADD 1 TO C-NO-OF-TESTS.                                      IX1024.2
032700*    ACCEPT C-DATE  FROM DATE.                                    IX1024.2
032800*    ACCEPT C-TIME  FROM TIME.                                    IX1024.2
032900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1024.2
033000*END-E-1.                                                         IX1024.2
033100*    CLOSE RAW-DATA.                                              IX1024.2
033200     OPEN    OUTPUT PRINT-FILE.                                   IX1024.2
033300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1024.2
033400     MOVE    SPACE TO TEST-RESULTS.                               IX1024.2
033500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1024.2
033600     MOVE    ZERO TO REC-SKL-SUB.                                 IX1024.2
033700     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1024.2
033800 CCVS-INIT-FILE.                                                  IX1024.2
033900     ADD     1 TO REC-SKL-SUB.                                    IX1024.2
034000     MOVE    FILE-RECORD-INFO-SKELETON                            IX1024.2
034100          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1024.2
034200 CCVS-INIT-EXIT.                                                  IX1024.2
034300     GO TO CCVS1-EXIT.                                            IX1024.2
034400 CLOSE-FILES.                                                     IX1024.2
034500*    OPEN I-O RAW-DATA.                                           IX1024.2
034600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1024.2
034700*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1024.2
034800*    MOVE "OK.     " TO C-ABORT.                                  IX1024.2
034900*    MOVE PASS-COUNTER TO C-OK.                                   IX1024.2
035000*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1024.2
035100*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1024.2
035200*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1024.2
035300*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1024.2
035400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1024.2
035500*END-E-2.                                                         IX1024.2
035600*    CLOSE RAW-DATA.                                              IX1024.2
035700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1024.2
035800 TERMINATE-CCVS.                                                  IX1024.2
035900*    EXIT PROGRAM.                                                IX1024.2
036000*TERMINATE-CALL.                                                  IX1024.2
036100     STOP     RUN.                                                IX1024.2
036200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1024.2
036300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1024.2
036400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1024.2
036500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1024.2
036600     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1024.2
036700 PRINT-DETAIL.                                                    IX1024.2
036800     IF REC-CT NOT EQUAL TO ZERO                                  IX1024.2
036900             MOVE "." TO PARDOT-X                                 IX1024.2
037000             MOVE REC-CT TO DOTVALUE.                             IX1024.2
037100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1024.2
037200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1024.2
037300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1024.2
037400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1024.2
037500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1024.2
037600     MOVE SPACE TO CORRECT-X.                                     IX1024.2
037700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1024.2
037800     MOVE     SPACE TO RE-MARK.                                   IX1024.2
037900 HEAD-ROUTINE.                                                    IX1024.2
038000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1024.2
038100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1024.2
038200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1024.2
038300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1024.2
038400 COLUMN-NAMES-ROUTINE.                                            IX1024.2
038500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1024.2
038600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1024.2
038700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1024.2
038800 END-ROUTINE.                                                     IX1024.2
038900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1024.2
039000 END-RTN-EXIT.                                                    IX1024.2
039100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1024.2
039200 END-ROUTINE-1.                                                   IX1024.2
039300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1024.2
039400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1024.2
039500      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1024.2
039600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1024.2
039700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1024.2
039800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1024.2
039900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1024.2
040000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1024.2
040100  END-ROUTINE-12.                                                 IX1024.2
040200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1024.2
040300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1024.2
040400         MOVE "NO " TO ERROR-TOTAL                                IX1024.2
040500         ELSE                                                     IX1024.2
040600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1024.2
040700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1024.2
040800     PERFORM WRITE-LINE.                                          IX1024.2
040900 END-ROUTINE-13.                                                  IX1024.2
041000     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1024.2
041100         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1024.2
041200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1024.2
041300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1024.2
041400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1024.2
041500      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1024.2
041600          MOVE "NO " TO ERROR-TOTAL                               IX1024.2
041700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1024.2
041800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1024.2
041900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1024.2
042000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1024.2
042100 WRITE-LINE.                                                      IX1024.2
042200     ADD 1 TO RECORD-COUNT.                                       IX1024.2
042300*    IF RECORD-COUNT GREATER 42                                   IX1024.2
042400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1024.2
042500*        MOVE SPACE TO DUMMY-RECORD                               IX1024.2
042600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1024.2
042700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1024.2
042800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1024.2
042900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1024.2
043000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1024.2
043100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1024.2
043200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1024.2
043300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1024.2
043400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1024.2
043500*        MOVE ZERO TO RECORD-COUNT.                               IX1024.2
043600     PERFORM WRT-LN.                                              IX1024.2
043700 WRT-LN.                                                          IX1024.2
043800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1024.2
043900     MOVE SPACE TO DUMMY-RECORD.                                  IX1024.2
044000 BLANK-LINE-PRINT.                                                IX1024.2
044100     PERFORM WRT-LN.                                              IX1024.2
044200 FAIL-ROUTINE.                                                    IX1024.2
044300     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1024.2
044400            GO TO   FAIL-ROUTINE-WRITE.                           IX1024.2
044500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1024.2
044600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1024.2
044700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1024.2
044800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1024.2
044900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1024.2
045000     GO TO  FAIL-ROUTINE-EX.                                      IX1024.2
045100 FAIL-ROUTINE-WRITE.                                              IX1024.2
045200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1024.2
045300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1024.2
045400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1024.2
045500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1024.2
045600 FAIL-ROUTINE-EX. EXIT.                                           IX1024.2
045700 BAIL-OUT.                                                        IX1024.2
045800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1024.2
045900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1024.2
046000 BAIL-OUT-WRITE.                                                  IX1024.2
046100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1024.2
046200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1024.2
046300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1024.2
046400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1024.2
046500 BAIL-OUT-EX. EXIT.                                               IX1024.2
046600 CCVS1-EXIT.                                                      IX1024.2
046700     EXIT.                                                        IX1024.2
046800 SECT-IX-02-001 SECTION.                                          IX1024.2
046900 READ-INIT-F2-01.                                                 IX1024.2
047000*                                                                 IX1024.2
047100*  TEST   1                                                       IX1024.2
047200*                                                                 IX1024.2
047300     OPEN     INPUT  IX-FS1.                                      IX1024.2
047400     MOVE     "READ-TEST-F2-01" TO PAR-NAME.                      IX1024.2
047500     MOVE    ZERO TO WRK-DU-09V00-001.                            IX1024.2
047600     MOVE    IX-WRK-KEY  TO IX-FS1-KEY.                           IX1024.2
047700     MOVE     ZERO TO   WRK-CS-09V00-002                          IX1024.2
047800     MOVE     ZERO  TO  WRK-DU-09V00-001                          IX1024.2
047900     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
048000 READ-TEST-F2-01.                                                 IX1024.2
048100     ADD      1 TO WRK-DU-09V00-001                               IX1024.2
048200     MOVE      IX-WRK-KEY TO IX-FS1-KEY.                          IX1024.2
048300     IF     WRK-DU-09V00-001  GREATER  501                        IX1024.2
048400              MOVE "INVALID KEY NOT TAKEN" TO COMPUTED-A          IX1024.2
048500             MOVE    WRK-DU-09V00-001  TO CORRECT-18V0            IX1024.2
048600              PERFORM FAIL                                        IX1024.2
048700              PERFORM PRINT-DETAIL                                IX1024.2
048800              GO TO READ-WRITE-F2-01.                             IX1024.2
048900     READ     IX-FS1                                              IX1024.2
049000             INVALID KEY GO TO READ-WRITE-F2-01.                  IX1024.2
049100     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX1024.2
049200     IF      XRECORD-NUMBER (1) EQUAL TO WRK-DU-09V00-001         IX1024.2
049300              GO TO READ-TEST-F2-01.                              IX1024.2
049400     MOVE    "YES" TO I-O-ERROR-IX-FS1.                           IX1024.2
049500     ADD      1 TO WRK-CS-09V00-002                               IX1024.2
049600     GO TO    READ-TEST-F2-01.                                    IX1024.2
049700 READ-WRITE-F2-01.                                                IX1024.2
049800     IF      WRK-DU-09V00-001  NOT EQUAL TO 501                   IX1024.2
049900              MOVE "WRONG KEY/NOT 500" TO CORRECT-A               IX1024.2
050000             MOVE    WRK-DU-09V00-001  TO  COMPUTED-18V0          IX1024.2
050100              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
050200              PERFORM FAIL                                        IX1024.2
050300              ELSE                                                IX1024.2
050400              PERFORM PASS.                                       IX1024.2
050500     PERFORM PRINT-DETAIL.                                        IX1024.2
050600 READ-TEST-F2-02.                                                 IX1024.2
050700     MOVE     "READ-TEST-F2-02" TO PAR-NAME.                      IX1024.2
050800     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
050900*                                                                 IX1024.2
051000*  TEST   2                                                       IX1024.2
051100*                                                                 IX1024.2
051200     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 IX1024.2
051300              MOVE "WRONG RECORD/NOT 500" TO CORRECT-A            IX1024.2
051400              MOVE  XRECORD-NUMBER (1) TO COMPUTED-18V0           IX1024.2
051500              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
051600              PERFORM FAIL                                        IX1024.2
051700              ELSE                                                IX1024.2
051800              PERFORM PASS.                                       IX1024.2
051900     PERFORM PRINT-DETAIL.                                        IX1024.2
052000 READ-TEST-F2-03.                                                 IX1024.2
052100     MOVE     "READ-TEST-F2-03" TO PAR-NAME.                      IX1024.2
052200     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
052300*                                                                 IX1024.2
052400*  TEST   3                                                       IX1024.2
052500*                                                                 IX1024.2
052600     IF      WRK-DU-09V00-001 NOT EQUAL TO 501                    IX1024.2
052700              MOVE "INCORRECT RECORD COUNT" TO RE-MARK            IX1024.2
052800              MOVE  WRK-DU-09V00-001 TO COMPUTED-18V0             IX1024.2
052900             MOVE 501  TO CORRECT-18V0                            IX1024.2
053000              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
053100              PERFORM FAIL                                        IX1024.2
053200              ELSE                                                IX1024.2
053300              PERFORM PASS.                                       IX1024.2
053400     PERFORM PRINT-DETAIL.                                        IX1024.2
053500 READ-TEST-F2-04.                                                 IX1024.2
053600     MOVE     "READ-TEST-F2-04" TO PAR-NAME.                      IX1024.2
053700     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
053800*                                                                 IX1024.2
053900*  TEST   4                                                       IX1024.2
054000*                                                                 IX1024.2
054100     IF       I-O-ERROR-IX-FS1 EQUAL TO "YES"                     IX1024.2
054200              MOVE WRK-CS-09V00-002 TO COMPUTED-18V0              IX1024.2
054300              MOVE "RECORDS DID NOT COMPARE" TO RE-MARK           IX1024.2
054400              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
054500              PERFORM FAIL                                        IX1024.2
054600              ELSE                                                IX1024.2
054700              PERFORM PASS.                                       IX1024.2
054800     PERFORM PRINT-DETAIL.                                        IX1024.2
054900     CLOSE    IX-FS1.                                             IX1024.2
055000 REWRITE-INIT-F2-01.                                              IX1024.2
055100     MOVE     "REWRITE-TEST-F2-01" TO PAR-NAME.                   IX1024.2
055200     OPEN I-O IX-FS1.                                             IX1024.2
055300     MOVE     ZERO TO IX-FS1-KEY.                                 IX1024.2
055400     MOVE     ZERO TO WRK-CS-09V00-002.                           IX1024.2
055500     MOVE     ZERO TO WRK-DU-09V00-001.                           IX1024.2
055600     MOVE    "REWRITE ... INVALID"   TO FEATURE.                  IX1024.2
055700     MOVE     SPACE TO  FILE-RECORD-INFO (1).                     IX1024.2
055800 REWRITE-TEST-F2-01.                                              IX1024.2
055900*                                                                 IX1024.2
056000*  REWRITE TEST  1                                                IX1024.2
056100*                                                                 IX1024.2
056200     ADD      5 TO  WRK-DU-09V00-001.                             IX1024.2
056300     MOVE    IX-WRK-KEY TO IX-FS1-KEY.                            IX1024.2
056400     IF    WRK-DU-09V00-001  GREATER 505                          IX1024.2
056500              MOVE "INVALID KEY/NOT TAKEN" TO COMPUTED-A          IX1024.2
056600             MOVE    WRK-DU-09V00-001  TO CORRECT-18V0            IX1024.2
056700              PERFORM FAIL                                        IX1024.2
056800              PERFORM PRINT-DETAIL                                IX1024.2
056900              GO TO REWRITE-TEST-F2-01-3.                         IX1024.2
057000     READ     IX-FS1                                              IX1024.2
057100              INVALID KEY GO TO REWRITE-TEST-F2-01-1.             IX1024.2
057200     MOVE    IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1)             IX1024.2
057300     ADD      01 TO UPDATE-NUMBER (1).                            IX1024.2
057400     MOVE  CCVS-PGM-ID  TO XPROGRAM-NAME (1).                     IX1024.2
057500     MOVE     FILE-RECORD-INFO (1) TO IX-FS1R1-F-G-240.           IX1024.2
057600     REWRITE    IX-FS1R1-F-G-240                                  IX1024.2
057700              INVALID KEY GO TO REWRITE-TEST-F2-01-2.             IX1024.2
057800     GO TO    REWRITE-TEST-F2-01.                                 IX1024.2
057900 REWRITE-TEST-F2-01-1.                                            IX1024.2
058000     IF      WRK-DU-09V00-001  LESS THAN 501                      IX1024.2
058100              ADD 1 TO  WRK-CS-09V00-004                          IX1024.2
058200              GO TO REWRITE-TEST-F2-01.                           IX1024.2
058300     PERFORM   PASS.                                              IX1024.2
058400     PERFORM    PRINT-DETAIL.                                     IX1024.2
058500 REWRITE-TEST-F2-02.                                              IX1024.2
058600     MOVE     "REWRITE-TEST-F2-02" TO PAR-NAME.                   IX1024.2
058700     MOVE    "REWRITE ... INVALID"   TO FEATURE.                  IX1024.2
058800*                                                                 IX1024.2
058900*  REWRITE TEST  2                                                IX1024.2
059000*                                                                 IX1024.2
059100     GO TO    REWRITE-TEST-F2-01-3.                               IX1024.2
059200 REWRITE-TEST-F2-01-2.                                            IX1024.2
059300     ADD      1 TO WRK-CS-09V00-005.                              IX1024.2
059400     IF      WRK-DU-09V00-001  LESS THAN 501                      IX1024.2
059500              GO TO REWRITE-TEST-F2-01.                           IX1024.2
059600 REWRITE-TEST-F2-01-3.                                            IX1024.2
059700     IF       WRK-CS-09V00-004 NOT EQUAL TO ZERO                  IX1024.2
059800              MOVE "INVALID KEY ON READ" TO COMPUTED-A            IX1024.2
059900              MOVE WRK-CS-09V00-004 TO CORRECT-18V0               IX1024.2
060000              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
060100              PERFORM FAIL                                        IX1024.2
060200              ELSE                                                IX1024.2
060300              PERFORM PASS.                                       IX1024.2
060400     PERFORM PRINT-DETAIL.                                        IX1024.2
060500 REWRITE-TEST-F2-03.                                              IX1024.2
060600     MOVE     "REWRITE-TEST-F2-03" TO PAR-NAME.                   IX1024.2
060700     MOVE    "REWRITE ... INVALID"   TO FEATURE.                  IX1024.2
060800*                                                                 IX1024.2
060900*  REWRITE TEST  3                                                IX1024.2
061000*                                                                 IX1024.2
061100     IF       WRK-CS-09V00-005 NOT EQUAL TO ZERO                  IX1024.2
061200              MOVE "INVALID KEY ON REWRITE" TO COMPUTED-A         IX1024.2
061300              MOVE  WRK-CS-09V00-005 TO CORRECT-18V0              IX1024.2
061400              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
061500              PERFORM FAIL                                        IX1024.2
061600              ELSE                                                IX1024.2
061700              PERFORM PASS.                                       IX1024.2
061800     PERFORM PRINT-DETAIL.                                        IX1024.2
061900     CLOSE    IX-FS1.                                             IX1024.2
062000 READ-INIT-F2-05.                                                 IX1024.2
062100     MOVE     "READ-TEST-F2-05" TO PAR-NAME.                      IX1024.2
062200     OPEN     INPUT  IX-FS1.                                      IX1024.2
062300     MOVE     501  TO WRK-DU-09V00-001.                           IX1024.2
062400     MOVE    ZERO TO WRK-CS-09V00-004.                            IX1024.2
062500     MOVE    ZERO TO WRK-CS-09V00-005.                            IX1024.2
062600     MOVE    ZERO TO WRK-CS-09V00-002.                            IX1024.2
062700     MOVE     SPACE TO  FILE-RECORD-INFO (1).                     IX1024.2
062800     MOVE    "READ  ... INVALID "   TO FEATURE.                   IX1024.2
062900 READ-TEST-F2-05.                                                 IX1024.2
063000     IF      WRK-DU-09V00-001  EQUAL TO ZERO                      IX1024.2
063100              MOVE "INVALID KEY/NOT TAKEN" TO COMPUTED-A          IX1024.2
063200             MOVE    WRK-DU-09V00-001  TO  COMPUTED-18V0          IX1024.2
063300             MOVE    ZERO TO CORRECT-18V0                         IX1024.2
063400              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
063500              PERFORM FAIL                                        IX1024.2
063600              PERFORM PRINT-DETAIL                                IX1024.2
063700              GO TO READ-TEST-F2-05.                              IX1024.2
063800     SUBTRACT     1  FROM WRK-DU-09V00-001.                       IX1024.2
063900     MOVE      IX-WRK-KEY TO IX-FS1-KEY.                          IX1024.2
064000     READ     IX-FS1                                              IX1024.2
064100              INVALID KEY  GO TO READ-TEST-F2-05-1.               IX1024.2
064200     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX1024.2
064300     IF     UPDATE-NUMBER (1) EQUAL TO 00                         IX1024.2
064400              ADD 1 TO WRK-CS-09V00-004.                          IX1024.2
064500     IF     UPDATE-NUMBER (1) EQUAL TO 01                         IX1024.2
064600              ADD 1 TO WRK-CS-09V00-005.                          IX1024.2
064700     GO TO    READ-TEST-F2-05.                                    IX1024.2
064800 READ-TEST-F2-05-1.                                               IX1024.2
064900     IF      WRK-DU-09V00-001  GREATER ZERO                       IX1024.2
065000             ADD 1 TO WRK-CS-09V00-002                            IX1024.2
065100              GO TO READ-TEST-F2-05.                              IX1024.2
065200     PERFORM   PASS.                                              IX1024.2
065300     PERFORM   PRINT-DETAIL.                                      IX1024.2
065400 READ-TEST-F2-06.                                                 IX1024.2
065500     MOVE     "READ-TEST-F2-06" TO PAR-NAME.                      IX1024.2
065600     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
065700*                                                                 IX1024.2
065800*  TEST   6                                                       IX1024.2
065900*                                                                 IX1024.2
066000     IF       WRK-CS-09V00-004 NOT EQUAL TO 400                   IX1024.2
066100              MOVE "NON-UPDATED RECORDS" TO COMPUTED-A            IX1024.2
066200              MOVE WRK-CS-09V00-004 TO CORRECT-18V0               IX1024.2
066300              MOVE "SHOULD BE 400" TO RE-MARK                     IX1024.2
066400              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
066500              PERFORM FAIL                                        IX1024.2
066600              ELSE                                                IX1024.2
066700              PERFORM PASS.                                       IX1024.2
066800     PERFORM PRINT-DETAIL.                                        IX1024.2
066900 READ-TEST-F2-07.                                                 IX1024.2
067000     MOVE     "READ-TEST-F2-07" TO PAR-NAME.                      IX1024.2
067100     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
067200*                                                                 IX1024.2
067300*  TEST   7                                                       IX1024.2
067400*                                                                 IX1024.2
067500     IF       WRK-CS-09V00-005   NOT EQUAL TO 100                 IX1024.2
067600              MOVE "UPDATED RECORDS" TO COMPUTED-A                IX1024.2
067700              MOVE WRK-CS-09V00-005 TO CORRECT-18V0               IX1024.2
067800              MOVE "SHOULD BE 100" TO RE-MARK                     IX1024.2
067900              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
068000              PERFORM FAIL                                        IX1024.2
068100              ELSE                                                IX1024.2
068200              PERFORM PASS.                                       IX1024.2
068300     PERFORM PRINT-DETAIL.                                        IX1024.2
068400 READ-TEST-F2-08.                                                 IX1024.2
068500     MOVE     "READ-TEST-F2-08" TO PAR-NAME.                      IX1024.2
068600     MOVE    "READ ... INVALID"  TO FEATURE.                      IX1024.2
068700*                                                                 IX1024.2
068800*  TEST   8                                                       IX1024.2
068900*                                                                 IX1024.2
069000     IF      WRK-CS-09V00-002 GREATER 1                           IX1024.2
069100             MOVE WRK-CS-09V00-002 TO COMPUTED-N                  IX1024.2
069200             MOVE  "INVALID KEY/READS" TO CORRECT-A               IX1024.2
069300              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1024.2
069400             PERFORM FAIL                                         IX1024.2
069500              ELSE                                                IX1024.2
069600              PERFORM PASS.                                       IX1024.2
069700     PERFORM PRINT-DETAIL.                                        IX1024.2
069800     CLOSE    IX-FS1.                                             IX1024.2
069900 CCVS-EXIT SECTION.                                               IX1024.2
070000 CCVS-999999.                                                     IX1024.2
070100     GO TO CLOSE-FILES.                                           IX1024.2
      *END-OF,IX102A                                                                  
