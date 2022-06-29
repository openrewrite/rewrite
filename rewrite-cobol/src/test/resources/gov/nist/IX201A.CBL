      *HEADER,COBOL,IX201A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2014.2
000200 PROGRAM-ID.                                                      IX2014.2
000300     IX201A.                                                      IX2014.2
000400****************************************************************  IX2014.2
000500*                                                              *  IX2014.2
000600*    VALIDATION FOR:-                                          *  IX2014.2
000700*                                                              *  IX2014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2014.2
000900*                                                              *  IX2014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2014.2
001100*                                                              *  IX2014.2
001200****************************************************************  IX2014.2
001300*    THIS PROGRAM IS THE FIRST OF A SERIES WHICH PROCESSES AN     IX2014.2
001400*    INDEXED FILE.  THE FUNCTION OF THIS PROGRAM IS TO CREATE AN  IX2014.2
001500*    INDEXED FILE SEQUENTIALLY (ACCESS MODE SEQUENTIAL) AND VERIFYIX2014.2
001600*    THAT IT WAS CREATED CORRECTLY.  THE FILE IS IDENTIFIED AS    IX2014.2
001700*    "IX-FS1" AND IS PASSED TO IX202 FOR PROCESSING.              IX2014.2
001800*                                                                 IX2014.2
001900*                                                                 IX2014.2
002000*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2014.2
002100*                                                                 IX2014.2
002200*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2014.2
002300*                    CLAUSE FOR DATA FILE IX-FS1                  IX2014.2
002400*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2014.2
002500*                    CLAUSE FOR INDEX FILE IX-FS1                 IX2014.2
002600*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2014.2
002700*             X-62   FOR RAW-DATA                                 IX2014.2
002800*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2014.2
002900*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2014.2
003000*                                                                 IX2014.2
003100*         NOTE:  X-CARDS 44 AND 62       ARE OPTIONAL             IX2014.2
003200*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2014.2
003300*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2014.2
003400*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2014.2
003500*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2014.2
003600*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2014.2
003700*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2014.2
003800*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2014.2
003900*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2014.2
004000*               THEY ARE AS FOLLOWS                               IX2014.2
004100*                                                                 IX2014.2
004200*                  P  SELECTS X-CARDS 62                          IX2014.2
004300*                  J  SELECTS X-CARD 44                           IX2014.2
004400*                                                                 IX2014.2
004500******************************************************            IX2014.2
004600 ENVIRONMENT DIVISION.                                            IX2014.2
004700 CONFIGURATION SECTION.                                           IX2014.2
004800 SOURCE-COMPUTER.                                                 IX2014.2
004900     XXXXX082.                                                    IX2014.2
005000 OBJECT-COMPUTER.                                                 IX2014.2
005100     XXXXX083.                                                    IX2014.2
005200 INPUT-OUTPUT SECTION.                                            IX2014.2
005300 FILE-CONTROL.                                                    IX2014.2
005400*    SELECT RAW-DATA   ASSIGN TO                                  IX2014.2
005500*    XXXXX062                                                     IX2014.2
005600*           ORGANIZATION IS INDEXED                               IX2014.2
005700*           ACCESS MODE IS RANDOM                                 IX2014.2
005800*           RECORD KEY IS RAW-DATA-KEY.                           IX2014.2
005900     SELECT PRINT-FILE ASSIGN TO                                  IX2014.2
006000     XXXXX055.                                                    IX2014.2
006100     SELECT   IX-FS1 ASSIGN TO                                    IX2014.2
006200     XXXXP024                                                     IX2014.2
006300*    XXXXP044                                                     IX2014.2
006400     ORGANIZATION IS INDEXED                                      IX2014.2
006500     RECORD KEY IS IX-FS1-KEY                                     IX2014.2
006600     ACCESS MODE IS SEQUENTIAL.                                   IX2014.2
006700 DATA DIVISION.                                                   IX2014.2
006800 FILE SECTION.                                                    IX2014.2
006900*                                                                 IX2014.2
007000*FD  RAW-DATA.                                                    IX2014.2
007100*                                                                 IX2014.2
007200*01  RAW-DATA-SATZ.                                               IX2014.2
007300*    05  RAW-DATA-KEY        PIC X(6).                            IX2014.2
007400*    05  C-DATE              PIC 9(6).                            IX2014.2
007500*    05  C-TIME              PIC 9(8).                            IX2014.2
007600*    05  C-NO-OF-TESTS       PIC 99.                              IX2014.2
007700*    05  C-OK                PIC 999.                             IX2014.2
007800*    05  C-ALL               PIC 999.                             IX2014.2
007900*    05  C-FAIL              PIC 999.                             IX2014.2
008000*    05  C-DELETED           PIC 999.                             IX2014.2
008100*    05  C-INSPECT           PIC 999.                             IX2014.2
008200*    05  C-NOTE              PIC X(13).                           IX2014.2
008300*    05  C-INDENT            PIC X.                               IX2014.2
008400*    05  C-ABORT             PIC X(8).                            IX2014.2
008500 FD  PRINT-FILE.                                                  IX2014.2
008600 01  PRINT-REC PICTURE X(120).                                    IX2014.2
008700 01  DUMMY-RECORD PICTURE X(120).                                 IX2014.2
008800 FD  IX-FS1                                                       IX2014.2
008900C    LABEL RECORD IS STANDARD                                     IX2014.2
009000C    DATA RECORD IS IX-FS1R1-F-G-240                              IX2014.2
009100     BLOCK CONTAINS 1 RECORDS                                     IX2014.2
009200     RECORD CONTAINS 240 CHARACTERS.                              IX2014.2
009300 01  IX-FS1R1-F-G-240.                                            IX2014.2
009400     03 IX-FS1-WRK-120 PIC X(120).                                IX2014.2
009500     03 IX-FS1-GRP-120.                                           IX2014.2
009600     05 FILLER   PIC   X(8).                                      IX2014.2
009700     05 IX-FS1-KEY  PIC X(29).                                    IX2014.2
009800     05 FILLER PIC X(83).                                         IX2014.2
009900 WORKING-STORAGE SECTION.                                         IX2014.2
010000 01  GRP-0101.                                                    IX2014.2
010100     02 FILLER   PIC X(10)  VALUE "ABCDLKJXYZ".                   IX2014.2
010200     02 WRK-DU-09V00-001 PIC 9(9)  VALUE ZERO.                    IX2014.2
010300     02 FILLER  PIC X(10)  VALUE "ZIF,.$-+CD".                    IX2014.2
010400 01  FILE-RECORD-INFORMATION-REC.                                 IX2014.2
010500     03 FILE-RECORD-INFO-SKELETON.                                IX2014.2
010600        05 FILLER                 PICTURE X(48)       VALUE       IX2014.2
010700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2014.2
010800        05 FILLER                 PICTURE X(46)       VALUE       IX2014.2
010900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2014.2
011000        05 FILLER                 PICTURE X(26)       VALUE       IX2014.2
011100             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2014.2
011200        05 FILLER                 PICTURE X(37)       VALUE       IX2014.2
011300             ",RECKEY=                             ".             IX2014.2
011400        05 FILLER                 PICTURE X(38)       VALUE       IX2014.2
011500             ",ALTKEY1=                             ".            IX2014.2
011600        05 FILLER                 PICTURE X(38)       VALUE       IX2014.2
011700             ",ALTKEY2=                             ".            IX2014.2
011800        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2014.2
011900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2014.2
012000        05 FILE-RECORD-INFO-P1-120.                               IX2014.2
012100           07 FILLER              PIC X(5).                       IX2014.2
012200           07 XFILE-NAME           PIC X(6).                      IX2014.2
012300           07 FILLER              PIC X(8).                       IX2014.2
012400           07 XRECORD-NAME         PIC X(6).                      IX2014.2
012500           07 FILLER              PIC X(1).                       IX2014.2
012600           07 REELUNIT-NUMBER     PIC 9(1).                       IX2014.2
012700           07 FILLER              PIC X(7).                       IX2014.2
012800           07 XRECORD-NUMBER       PIC 9(6).                      IX2014.2
012900           07 FILLER              PIC X(6).                       IX2014.2
013000           07 UPDATE-NUMBER       PIC 9(2).                       IX2014.2
013100           07 FILLER              PIC X(5).                       IX2014.2
013200           07 ODO-NUMBER          PIC 9(4).                       IX2014.2
013300           07 FILLER              PIC X(5).                       IX2014.2
013400           07 XPROGRAM-NAME        PIC X(5).                      IX2014.2
013500           07 FILLER              PIC X(7).                       IX2014.2
013600           07 XRECORD-LENGTH       PIC 9(6).                      IX2014.2
013700           07 FILLER              PIC X(7).                       IX2014.2
013800           07 CHARS-OR-RECORDS    PIC X(2).                       IX2014.2
013900           07 FILLER              PIC X(1).                       IX2014.2
014000           07 XBLOCK-SIZE          PIC 9(4).                      IX2014.2
014100           07 FILLER              PIC X(6).                       IX2014.2
014200           07 RECORDS-IN-FILE     PIC 9(6).                       IX2014.2
014300           07 FILLER              PIC X(5).                       IX2014.2
014400           07 XFILE-ORGANIZATION   PIC X(2).                      IX2014.2
014500           07 FILLER              PIC X(6).                       IX2014.2
014600           07 XLABEL-TYPE          PIC X(1).                      IX2014.2
014700        05 FILE-RECORD-INFO-P121-240.                             IX2014.2
014800           07 FILLER              PIC X(8).                       IX2014.2
014900           07 XRECORD-KEY          PIC X(29).                     IX2014.2
015000           07 FILLER              PIC X(9).                       IX2014.2
015100           07 ALTERNATE-KEY1      PIC X(29).                      IX2014.2
015200           07 FILLER              PIC X(9).                       IX2014.2
015300           07 ALTERNATE-KEY2      PIC X(29).                      IX2014.2
015400           07 FILLER              PIC X(7).                       IX2014.2
015500 01  TEST-RESULTS.                                                IX2014.2
015600     02 FILLER                   PIC X      VALUE SPACE.          IX2014.2
015700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2014.2
015800     02 FILLER                   PIC X      VALUE SPACE.          IX2014.2
015900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2014.2
016000     02 FILLER                   PIC X      VALUE SPACE.          IX2014.2
016100     02  PAR-NAME.                                                IX2014.2
016200       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2014.2
016300       03  PARDOT-X              PIC X      VALUE SPACE.          IX2014.2
016400       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2014.2
016500     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2014.2
016600     02 RE-MARK                  PIC X(61).                       IX2014.2
016700 01  TEST-COMPUTED.                                               IX2014.2
016800     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2014.2
016900     02 FILLER                   PIC X(17)  VALUE                 IX2014.2
017000            "       COMPUTED=".                                   IX2014.2
017100     02 COMPUTED-X.                                               IX2014.2
017200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2014.2
017300     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2014.2
017400                                 PIC -9(9).9(9).                  IX2014.2
017500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2014.2
017600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2014.2
017700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2014.2
017800     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2014.2
017900         04 COMPUTED-18V0                    PIC -9(18).          IX2014.2
018000         04 FILLER                           PIC X.               IX2014.2
018100     03 FILLER PIC X(50) VALUE SPACE.                             IX2014.2
018200 01  TEST-CORRECT.                                                IX2014.2
018300     02 FILLER PIC X(30) VALUE SPACE.                             IX2014.2
018400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2014.2
018500     02 CORRECT-X.                                                IX2014.2
018600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2014.2
018700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2014.2
018800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2014.2
018900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2014.2
019000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2014.2
019100     03      CR-18V0 REDEFINES CORRECT-A.                         IX2014.2
019200         04 CORRECT-18V0                     PIC -9(18).          IX2014.2
019300         04 FILLER                           PIC X.               IX2014.2
019400     03 FILLER PIC X(2) VALUE SPACE.                              IX2014.2
019500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2014.2
019600 01  CCVS-C-1.                                                    IX2014.2
019700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2014.2
019800-    "SS  PARAGRAPH-NAME                                          IX2014.2
019900-    "       REMARKS".                                            IX2014.2
020000     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2014.2
020100 01  CCVS-C-2.                                                    IX2014.2
020200     02 FILLER                     PIC X        VALUE SPACE.      IX2014.2
020300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2014.2
020400     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2014.2
020500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2014.2
020600     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2014.2
020700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2014.2
020800 01  REC-CT                        PIC 99       VALUE ZERO.       IX2014.2
020900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2014.2
021000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2014.2
021100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2014.2
021200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2014.2
021300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2014.2
021400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2014.2
021500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2014.2
021600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2014.2
021700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2014.2
021800 01  CCVS-H-1.                                                    IX2014.2
021900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2014.2
022000     02  FILLER                    PIC X(42)    VALUE             IX2014.2
022100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2014.2
022200     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2014.2
022300 01  CCVS-H-2A.                                                   IX2014.2
022400   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2014.2
022500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2014.2
022600   02  FILLER                        PIC XXXX   VALUE             IX2014.2
022700     "4.2 ".                                                      IX2014.2
022800   02  FILLER                        PIC X(28)  VALUE             IX2014.2
022900            " COPY - NOT FOR DISTRIBUTION".                       IX2014.2
023000   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2014.2
023100                                                                  IX2014.2
023200 01  CCVS-H-2B.                                                   IX2014.2
023300   02  FILLER                        PIC X(15)  VALUE             IX2014.2
023400            "TEST RESULT OF ".                                    IX2014.2
023500   02  TEST-ID                       PIC X(9).                    IX2014.2
023600   02  FILLER                        PIC X(4)   VALUE             IX2014.2
023700            " IN ".                                               IX2014.2
023800   02  FILLER                        PIC X(12)  VALUE             IX2014.2
023900     " HIGH       ".                                              IX2014.2
024000   02  FILLER                        PIC X(22)  VALUE             IX2014.2
024100            " LEVEL VALIDATION FOR ".                             IX2014.2
024200   02  FILLER                        PIC X(58)  VALUE             IX2014.2
024300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2014.2
024400 01  CCVS-H-3.                                                    IX2014.2
024500     02  FILLER                      PIC X(34)  VALUE             IX2014.2
024600            " FOR OFFICIAL USE ONLY    ".                         IX2014.2
024700     02  FILLER                      PIC X(58)  VALUE             IX2014.2
024800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2014.2
024900     02  FILLER                      PIC X(28)  VALUE             IX2014.2
025000            "  COPYRIGHT   1985 ".                                IX2014.2
025100 01  CCVS-E-1.                                                    IX2014.2
025200     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2014.2
025300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2014.2
025400     02 ID-AGAIN                     PIC X(9).                    IX2014.2
025500     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2014.2
025600 01  CCVS-E-2.                                                    IX2014.2
025700     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2014.2
025800     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2014.2
025900     02 CCVS-E-2-2.                                               IX2014.2
026000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2014.2
026100         03 FILLER                   PIC X      VALUE SPACE.      IX2014.2
026200         03 ENDER-DESC               PIC X(44)  VALUE             IX2014.2
026300            "ERRORS ENCOUNTERED".                                 IX2014.2
026400 01  CCVS-E-3.                                                    IX2014.2
026500     02  FILLER                      PIC X(22)  VALUE             IX2014.2
026600            " FOR OFFICIAL USE ONLY".                             IX2014.2
026700     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2014.2
026800     02  FILLER                      PIC X(58)  VALUE             IX2014.2
026900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2014.2
027000     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2014.2
027100     02 FILLER                       PIC X(15)  VALUE             IX2014.2
027200             " COPYRIGHT 1985".                                   IX2014.2
027300 01  CCVS-E-4.                                                    IX2014.2
027400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2014.2
027500     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2014.2
027600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2014.2
027700     02 FILLER                       PIC X(40)  VALUE             IX2014.2
027800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2014.2
027900 01  XXINFO.                                                      IX2014.2
028000     02 FILLER                       PIC X(19)  VALUE             IX2014.2
028100            "*** INFORMATION ***".                                IX2014.2
028200     02 INFO-TEXT.                                                IX2014.2
028300       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2014.2
028400       04 XXCOMPUTED                 PIC X(20).                   IX2014.2
028500       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2014.2
028600       04 XXCORRECT                  PIC X(20).                   IX2014.2
028700     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2014.2
028800 01  HYPHEN-LINE.                                                 IX2014.2
028900     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2014.2
029000     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2014.2
029100-    "*****************************************".                 IX2014.2
029200     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2014.2
029300-    "******************************".                            IX2014.2
029400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2014.2
029500     "IX201A".                                                    IX2014.2
029600 PROCEDURE DIVISION.                                              IX2014.2
029700 CCVS1 SECTION.                                                   IX2014.2
029800 OPEN-FILES.                                                      IX2014.2
029900*    OPEN I-O RAW-DATA.                                           IX2014.2
030000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2014.2
030100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2014.2
030200*    MOVE "ABORTED " TO C-ABORT.                                  IX2014.2
030300*    ADD 1 TO C-NO-OF-TESTS.                                      IX2014.2
030400*    ACCEPT C-DATE  FROM DATE.                                    IX2014.2
030500*    ACCEPT C-TIME  FROM TIME.                                    IX2014.2
030600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2014.2
030700*END-E-1.                                                         IX2014.2
030800*    CLOSE RAW-DATA.                                              IX2014.2
030900     OPEN    OUTPUT PRINT-FILE.                                   IX2014.2
031000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2014.2
031100     MOVE    SPACE TO TEST-RESULTS.                               IX2014.2
031200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2014.2
031300     MOVE    ZERO TO REC-SKL-SUB.                                 IX2014.2
031400     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2014.2
031500 CCVS-INIT-FILE.                                                  IX2014.2
031600     ADD     1 TO REC-SKL-SUB.                                    IX2014.2
031700     MOVE    FILE-RECORD-INFO-SKELETON                            IX2014.2
031800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2014.2
031900 CCVS-INIT-EXIT.                                                  IX2014.2
032000     GO TO CCVS1-EXIT.                                            IX2014.2
032100 CLOSE-FILES.                                                     IX2014.2
032200*    OPEN I-O RAW-DATA.                                           IX2014.2
032300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2014.2
032400*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2014.2
032500*    MOVE "OK.     " TO C-ABORT.                                  IX2014.2
032600*    MOVE PASS-COUNTER TO C-OK.                                   IX2014.2
032700*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2014.2
032800*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2014.2
032900*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2014.2
033000*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2014.2
033100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2014.2
033200*END-E-2.                                                         IX2014.2
033300*    CLOSE RAW-DATA.                                              IX2014.2
033400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2014.2
033500 TERMINATE-CCVS.                                                  IX2014.2
033600*    EXIT PROGRAM.                                                IX2014.2
033700*TERMINATE-CALL.                                                  IX2014.2
033800     STOP     RUN.                                                IX2014.2
033900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2014.2
034000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2014.2
034100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2014.2
034200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2014.2
034300     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2014.2
034400 PRINT-DETAIL.                                                    IX2014.2
034500     IF REC-CT NOT EQUAL TO ZERO                                  IX2014.2
034600             MOVE "." TO PARDOT-X                                 IX2014.2
034700             MOVE REC-CT TO DOTVALUE.                             IX2014.2
034800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2014.2
034900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2014.2
035000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2014.2
035100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2014.2
035200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2014.2
035300     MOVE SPACE TO CORRECT-X.                                     IX2014.2
035400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2014.2
035500     MOVE     SPACE TO RE-MARK.                                   IX2014.2
035600 HEAD-ROUTINE.                                                    IX2014.2
035700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2014.2
035800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2014.2
035900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2014.2
036000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2014.2
036100 COLUMN-NAMES-ROUTINE.                                            IX2014.2
036200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2014.2
036300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2014.2
036400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2014.2
036500 END-ROUTINE.                                                     IX2014.2
036600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2014.2
036700 END-RTN-EXIT.                                                    IX2014.2
036800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2014.2
036900 END-ROUTINE-1.                                                   IX2014.2
037000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2014.2
037100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2014.2
037200      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2014.2
037300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2014.2
037400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2014.2
037500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2014.2
037600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2014.2
037700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2014.2
037800  END-ROUTINE-12.                                                 IX2014.2
037900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2014.2
038000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2014.2
038100         MOVE "NO " TO ERROR-TOTAL                                IX2014.2
038200         ELSE                                                     IX2014.2
038300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2014.2
038400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2014.2
038500     PERFORM WRITE-LINE.                                          IX2014.2
038600 END-ROUTINE-13.                                                  IX2014.2
038700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2014.2
038800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2014.2
038900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2014.2
039000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2014.2
039100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2014.2
039200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2014.2
039300          MOVE "NO " TO ERROR-TOTAL                               IX2014.2
039400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2014.2
039500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2014.2
039600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2014.2
039700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2014.2
039800 WRITE-LINE.                                                      IX2014.2
039900     ADD 1 TO RECORD-COUNT.                                       IX2014.2
040000*    IF RECORD-COUNT GREATER 42                                   IX2014.2
040100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2014.2
040200*        MOVE SPACE TO DUMMY-RECORD                               IX2014.2
040300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2014.2
040400*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2014.2
040500*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2014.2
040600*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2014.2
040700*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2014.2
040800*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2014.2
040900*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2014.2
041000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2014.2
041100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2014.2
041200*        MOVE ZERO TO RECORD-COUNT.                               IX2014.2
041300     PERFORM WRT-LN.                                              IX2014.2
041400 WRT-LN.                                                          IX2014.2
041500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2014.2
041600     MOVE SPACE TO DUMMY-RECORD.                                  IX2014.2
041700 BLANK-LINE-PRINT.                                                IX2014.2
041800     PERFORM WRT-LN.                                              IX2014.2
041900 FAIL-ROUTINE.                                                    IX2014.2
042000     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2014.2
042100            GO TO   FAIL-ROUTINE-WRITE.                           IX2014.2
042200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2014.2
042300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2014.2
042400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2014.2
042500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2014.2
042600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2014.2
042700     GO TO  FAIL-ROUTINE-EX.                                      IX2014.2
042800 FAIL-ROUTINE-WRITE.                                              IX2014.2
042900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2014.2
043000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2014.2
043100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2014.2
043200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2014.2
043300 FAIL-ROUTINE-EX. EXIT.                                           IX2014.2
043400 BAIL-OUT.                                                        IX2014.2
043500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2014.2
043600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2014.2
043700 BAIL-OUT-WRITE.                                                  IX2014.2
043800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2014.2
043900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2014.2
044000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2014.2
044100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2014.2
044200 BAIL-OUT-EX. EXIT.                                               IX2014.2
044300 CCVS1-EXIT.                                                      IX2014.2
044400     EXIT.                                                        IX2014.2
044500 SECT-IX-01-001 SECTION.                                          IX2014.2
044600 WRITE-INIT-GF-01.                                                IX2014.2
044700     MOVE     "WRITE       IX-FS1" TO FEATURE.                    IX2014.2
044800     OPEN     OUTPUT    IX-FS1.                                   IX2014.2
044900     MOVE     "IX-FS1" TO XFILE-NAME (1).                         IX2014.2
045000     MOVE     "IX-F-G" TO XRECORD-NAME (1).                       IX2014.2
045100     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   IX2014.2
045200     MOVE     000240  TO XRECORD-LENGTH (1).                      IX2014.2
045300     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   IX2014.2
045400     MOVE     0001     TO XBLOCK-SIZE (1).                        IX2014.2
045500     MOVE     000500   TO RECORDS-IN-FILE (1).                    IX2014.2
045600     MOVE     "IX"  TO XFILE-ORGANIZATION (1).                    IX2014.2
045700     MOVE     "S"      TO XLABEL-TYPE (1).                        IX2014.2
045800     MOVE     000001   TO XRECORD-NUMBER (1).                     IX2014.2
045900 WRITE-TEST-GF-01.                                                IX2014.2
046000     MOVE     XRECORD-NUMBER (1) TO WRK-DU-09V00-001.             IX2014.2
046100     MOVE     GRP-0101 TO XRECORD-KEY (1).                        IX2014.2
046200     MOVE     FILE-RECORD-INFO (1) TO IX-FS1R1-F-G-240.           IX2014.2
046300     WRITE    IX-FS1R1-F-G-240                                    IX2014.2
046400              INVALID KEY GO TO WRITE-FAIL-GF-01.                 IX2014.2
046500     IF       XRECORD-NUMBER (1) EQUAL TO 500                     IX2014.2
046600              PERFORM PASS                                        IX2014.2
046700              GO TO WRITE-WRITE-GF-01.                            IX2014.2
046800     ADD      000001 TO XRECORD-NUMBER (1).                       IX2014.2
046900     GO       TO WRITE-TEST-GF-01.                                IX2014.2
047000 WRITE-FAIL-GF-01.                                                IX2014.2
047100     MOVE "BOUNDARY VIOLATION. WRITE FAILED; IX-41" TO RE-MARK.   IX2014.2
047200     PERFORM FAIL.                                                IX2014.2
047300 WRITE-WRITE-GF-01.                                               IX2014.2
047400     MOVE     "WRITE-TEST-GF-01" TO PAR-NAME                      IX2014.2
047500     MOVE     "FILE CREATED, LFILE "  TO COMPUTED-A.              IX2014.2
047600     MOVE     XRECORD-NUMBER (1) TO CORRECT-18V0.                 IX2014.2
047700     PERFORM  PRINT-DETAIL.                                       IX2014.2
047800     CLOSE    IX-FS1.                                             IX2014.2
047900 READ-INIT-F1-01.                                                 IX2014.2
048000     OPEN     INPUT     IX-FS1.                                   IX2014.2
048100     MOVE     ZERO TO WRK-DU-09V00-001.                           IX2014.2
048200 READ-TEST-F1-01.                                                 IX2014.2
048300     READ     IX-FS1                                              IX2014.2
048400              AT END GO TO READ-TEST-F1-01-1.                     IX2014.2
048500     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX2014.2
048600     ADD      1  TO WRK-DU-09V00-001.                             IX2014.2
048700     IF       WRK-DU-09V00-001 GREATER 500                        IX2014.2
048800              MOVE "MORE THAN 500 RECORDS" TO RE-MARK             IX2014.2
048900              GO TO READ-TEST-F1-01-1.                            IX2014.2
049000     GO       TO READ-TEST-F1-01.                                 IX2014.2
049100 READ-TEST-F1-01-1.                                               IX2014.2
049200     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 IX2014.2
049300              MOVE "READ FAILED; IX-28, 4.5.2" TO RE-MARK         IX2014.2
049400              PERFORM FAIL                                        IX2014.2
049500              ELSE                                                IX2014.2
049600              PERFORM PASS.                                       IX2014.2
049700 READ-WRITE-F1-01.                                                IX2014.2
049800     MOVE     "READ-TEST-F1-01" TO PAR-NAME.                      IX2014.2
049900     MOVE     "READ TO VERIFY " TO FEATURE.                       IX2014.2
050000     MOVE     "FILE VERIFIED, LFILE" TO COMPUTED-A.               IX2014.2
050100     MOVE     XRECORD-NUMBER (1) TO CORRECT-18V0.                 IX2014.2
050200     PERFORM  PRINT-DETAIL.                                       IX2014.2
050300     CLOSE    IX-FS1.                                             IX2014.2
050400 CCVS-EXIT SECTION.                                               IX2014.2
050500 CCVS-999999.                                                     IX2014.2
050600     GO TO CLOSE-FILES.                                           IX2014.2
      *END-OF,IX201A                                                                  
