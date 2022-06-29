      *HEADER,COBOL,IX101A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1014.2
000200 PROGRAM-ID.                                                      IX1014.2
000300     IX101A.                                                      IX1014.2
000400****************************************************************  IX1014.2
000500*                                                              *  IX1014.2
000600*    VALIDATION FOR:-                                          *  IX1014.2
000700*                                                              *  IX1014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1014.2
000900*                                                              *  IX1014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1014.2
001100*                                                              *  IX1014.2
001200****************************************************************  IX1014.2
001300*    THIS PROGRAM IS THE FIRST OF A SERIES WHICH PROCESSES AN     IX1014.2
001400*    INDEXED FILE.  THE FUNCTION OF THIS PROGRAM IS TO CREATE AN  IX1014.2
001500*    INDEXED FILE SEQUENTIALLY (ACCESS MODE SEQUENTIAL) AND VERIFYIX1014.2
001600*    THAT IT WAS CREATED AS EXPECTED.  THE FILE IS IDENTIFIED AS  IX1014.2
001700*    "IX-FS1" AND IS PASSED TO PROGRAM IX102 FOR PROCESSING.      IX1014.2
001800*                                                                 IX1014.2
001900*                                                                 IX1014.2
002000*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX1014.2
002100*                                                                 IX1014.2
002200*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1014.2
002300*                    CLAUSE FOR DATA FILE IX-FS1                  IX1014.2
002400*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1014.2
002500*                    CLAUSE FOR INDEX FILE IX-FS1                 IX1014.2
002600*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX1014.2
002700*             X-62   IMPLEMENTOR-NAME FOR RAW-DATA (OPTIONAL)     IX1014.2
002800*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX1014.2
002900*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX1014.2
003000*                                                                 IX1014.2
003100*         NOTE:  X-CARDS 44 AND 62          ARE OPTIONAL          IX1014.2
003200*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX1014.2
003300*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX1014.2
003400*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX1014.2
003500*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX1014.2
003600*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX1014.2
003700*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX1014.2
003800*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX1014.2
003900*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX1014.2
004000*               THEY ARE AS FOLLOWS                               IX1014.2
004100*                                                                 IX1014.2
004200*                  J  SELECTS X-CARD 44                           IX1014.2
004300*                                                                 IX1014.2
004400******************************************************            IX1014.2
004500 ENVIRONMENT DIVISION.                                            IX1014.2
004600 CONFIGURATION SECTION.                                           IX1014.2
004700 SOURCE-COMPUTER.                                                 IX1014.2
004800     XXXXX082.                                                    IX1014.2
004900 OBJECT-COMPUTER.                                                 IX1014.2
005000     XXXXX083.                                                    IX1014.2
005100 INPUT-OUTPUT SECTION.                                            IX1014.2
005200 FILE-CONTROL.                                                    IX1014.2
005300*    SELECT RAW-DATA   ASSIGN TO                                  IX1014.2
005400*    XXXXX062                                                     IX1014.2
005500*           ORGANIZATION IS INDEXED                               IX1014.2
005600*           ACCESS MODE IS RANDOM                                 IX1014.2
005700*           RECORD KEY IS RAW-DATA-KEY.                           IX1014.2
005800     SELECT PRINT-FILE ASSIGN TO                                  IX1014.2
005900     XXXXX055.                                                    IX1014.2
006000     SELECT   IX-FS1 ASSIGN TO                                    IX1014.2
006100     XXXXP024                                                     IX1014.2
006200*    XXXXP044                                                     IX1014.2
006300     ORGANIZATION IS INDEXED                                      IX1014.2
006400     RECORD KEY IS IX-FS1-KEY                                     IX1014.2
006500     ACCESS MODE IS SEQUENTIAL.                                   IX1014.2
006600 DATA DIVISION.                                                   IX1014.2
006700 FILE SECTION.                                                    IX1014.2
006800*                                                                 IX1014.2
006900*FD  RAW-DATA.                                                    IX1014.2
007000*                                                                 IX1014.2
007100*01  RAW-DATA-SATZ.                                               IX1014.2
007200*    05  RAW-DATA-KEY        PIC X(6).                            IX1014.2
007300*    05  C-DATE              PIC 9(6).                            IX1014.2
007400*    05  C-TIME              PIC 9(8).                            IX1014.2
007500*    05  C-NO-OF-TESTS       PIC 99.                              IX1014.2
007600*    05  C-OK                PIC 999.                             IX1014.2
007700*    05  C-ALL               PIC 999.                             IX1014.2
007800*    05  C-FAIL              PIC 999.                             IX1014.2
007900*    05  C-DELETED           PIC 999.                             IX1014.2
008000*    05  C-INSPECT           PIC 999.                             IX1014.2
008100*    05  C-NOTE              PIC X(13).                           IX1014.2
008200*    05  C-INDENT            PIC X.                               IX1014.2
008300*    05  C-ABORT             PIC X(8).                            IX1014.2
008400 FD  PRINT-FILE.                                                  IX1014.2
008500 01  PRINT-REC PICTURE X(120).                                    IX1014.2
008600 01  DUMMY-RECORD PICTURE X(120).                                 IX1014.2
008700 FD  IX-FS1                                                       IX1014.2
008800C    LABEL RECORD IS STANDARD                                     IX1014.2
008900C    DATA RECORD IS IX-FS1R1-F-G-240                              IX1014.2
009000     BLOCK CONTAINS 1 RECORDS                                     IX1014.2
009100     RECORD CONTAINS 240 CHARACTERS.                              IX1014.2
009200 01  IX-FS1R1-F-G-240.                                            IX1014.2
009300     03 IX-FS1-WRK-120 PIC X(120).                                IX1014.2
009400     03 IX-FS1-GRP-120.                                           IX1014.2
009500     05 FILLER   PIC   X(8).                                      IX1014.2
009600     05 IX-FS1-KEY  PIC X(29).                                    IX1014.2
009700     05 FILLER PIC X(83).                                         IX1014.2
009800 WORKING-STORAGE SECTION.                                         IX1014.2
009900 01  GRP-0101.                                                    IX1014.2
010000     02 FILLER   PIC X(10)  VALUE "ABCDLKJXYZ".                   IX1014.2
010100     02 WRK-DU-09V00-001 PIC 9(9)  VALUE ZERO.                    IX1014.2
010200     02 FILLER  PIC X(10)  VALUE "ZIF,.$-+CD".                    IX1014.2
010300 01  FILE-RECORD-INFORMATION-REC.                                 IX1014.2
010400     03 FILE-RECORD-INFO-SKELETON.                                IX1014.2
010500        05 FILLER                 PICTURE X(48)       VALUE       IX1014.2
010600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1014.2
010700        05 FILLER                 PICTURE X(46)       VALUE       IX1014.2
010800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1014.2
010900        05 FILLER                 PICTURE X(26)       VALUE       IX1014.2
011000             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1014.2
011100        05 FILLER                 PICTURE X(37)       VALUE       IX1014.2
011200             ",RECKEY=                             ".             IX1014.2
011300        05 FILLER                 PICTURE X(38)       VALUE       IX1014.2
011400             ",ALTKEY1=                             ".            IX1014.2
011500        05 FILLER                 PICTURE X(38)       VALUE       IX1014.2
011600             ",ALTKEY2=                             ".            IX1014.2
011700        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1014.2
011800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1014.2
011900        05 FILE-RECORD-INFO-P1-120.                               IX1014.2
012000           07 FILLER              PIC X(5).                       IX1014.2
012100           07 XFILE-NAME           PIC X(6).                      IX1014.2
012200           07 FILLER              PIC X(8).                       IX1014.2
012300           07 XRECORD-NAME         PIC X(6).                      IX1014.2
012400           07 FILLER              PIC X(1).                       IX1014.2
012500           07 REELUNIT-NUMBER     PIC 9(1).                       IX1014.2
012600           07 FILLER              PIC X(7).                       IX1014.2
012700           07 XRECORD-NUMBER       PIC 9(6).                      IX1014.2
012800           07 FILLER              PIC X(6).                       IX1014.2
012900           07 UPDATE-NUMBER       PIC 9(2).                       IX1014.2
013000           07 FILLER              PIC X(5).                       IX1014.2
013100           07 ODO-NUMBER          PIC 9(4).                       IX1014.2
013200           07 FILLER              PIC X(5).                       IX1014.2
013300           07 XPROGRAM-NAME        PIC X(5).                      IX1014.2
013400           07 FILLER              PIC X(7).                       IX1014.2
013500           07 XRECORD-LENGTH       PIC 9(6).                      IX1014.2
013600           07 FILLER              PIC X(7).                       IX1014.2
013700           07 CHARS-OR-RECORDS    PIC X(2).                       IX1014.2
013800           07 FILLER              PIC X(1).                       IX1014.2
013900           07 XBLOCK-SIZE          PIC 9(4).                      IX1014.2
014000           07 FILLER              PIC X(6).                       IX1014.2
014100           07 RECORDS-IN-FILE     PIC 9(6).                       IX1014.2
014200           07 FILLER              PIC X(5).                       IX1014.2
014300           07 XFILE-ORGANIZATION   PIC X(2).                      IX1014.2
014400           07 FILLER              PIC X(6).                       IX1014.2
014500           07 XLABEL-TYPE          PIC X(1).                      IX1014.2
014600        05 FILE-RECORD-INFO-P121-240.                             IX1014.2
014700           07 FILLER              PIC X(8).                       IX1014.2
014800           07 XRECORD-KEY          PIC X(29).                     IX1014.2
014900           07 FILLER              PIC X(9).                       IX1014.2
015000           07 ALTERNATE-KEY1      PIC X(29).                      IX1014.2
015100           07 FILLER              PIC X(9).                       IX1014.2
015200           07 ALTERNATE-KEY2      PIC X(29).                      IX1014.2
015300           07 FILLER              PIC X(7).                       IX1014.2
015400 01  TEST-RESULTS.                                                IX1014.2
015500     02 FILLER                   PIC X      VALUE SPACE.          IX1014.2
015600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1014.2
015700     02 FILLER                   PIC X      VALUE SPACE.          IX1014.2
015800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1014.2
015900     02 FILLER                   PIC X      VALUE SPACE.          IX1014.2
016000     02  PAR-NAME.                                                IX1014.2
016100       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1014.2
016200       03  PARDOT-X              PIC X      VALUE SPACE.          IX1014.2
016300       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1014.2
016400     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1014.2
016500     02 RE-MARK                  PIC X(61).                       IX1014.2
016600 01  TEST-COMPUTED.                                               IX1014.2
016700     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1014.2
016800     02 FILLER                   PIC X(17)  VALUE                 IX1014.2
016900            "       COMPUTED=".                                   IX1014.2
017000     02 COMPUTED-X.                                               IX1014.2
017100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1014.2
017200     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1014.2
017300                                 PIC -9(9).9(9).                  IX1014.2
017400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1014.2
017500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1014.2
017600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1014.2
017700     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1014.2
017800         04 COMPUTED-18V0                    PIC -9(18).          IX1014.2
017900         04 FILLER                           PIC X.               IX1014.2
018000     03 FILLER PIC X(50) VALUE SPACE.                             IX1014.2
018100 01  TEST-CORRECT.                                                IX1014.2
018200     02 FILLER PIC X(30) VALUE SPACE.                             IX1014.2
018300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1014.2
018400     02 CORRECT-X.                                                IX1014.2
018500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1014.2
018600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1014.2
018700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1014.2
018800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1014.2
018900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1014.2
019000     03      CR-18V0 REDEFINES CORRECT-A.                         IX1014.2
019100         04 CORRECT-18V0                     PIC -9(18).          IX1014.2
019200         04 FILLER                           PIC X.               IX1014.2
019300     03 FILLER PIC X(2) VALUE SPACE.                              IX1014.2
019400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1014.2
019500 01  CCVS-C-1.                                                    IX1014.2
019600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1014.2
019700-    "SS  PARAGRAPH-NAME                                          IX1014.2
019800-    "       REMARKS".                                            IX1014.2
019900     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1014.2
020000 01  CCVS-C-2.                                                    IX1014.2
020100     02 FILLER                     PIC X        VALUE SPACE.      IX1014.2
020200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1014.2
020300     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1014.2
020400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1014.2
020500     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1014.2
020600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1014.2
020700 01  REC-CT                        PIC 99       VALUE ZERO.       IX1014.2
020800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1014.2
020900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1014.2
021000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1014.2
021100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1014.2
021200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1014.2
021300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1014.2
021400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1014.2
021500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1014.2
021600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1014.2
021700 01  CCVS-H-1.                                                    IX1014.2
021800     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1014.2
021900     02  FILLER                    PIC X(42)    VALUE             IX1014.2
022000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1014.2
022100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1014.2
022200 01  CCVS-H-2A.                                                   IX1014.2
022300   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1014.2
022400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1014.2
022500   02  FILLER                        PIC XXXX   VALUE             IX1014.2
022600     "4.2 ".                                                      IX1014.2
022700   02  FILLER                        PIC X(28)  VALUE             IX1014.2
022800            " COPY - NOT FOR DISTRIBUTION".                       IX1014.2
022900   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1014.2
023000                                                                  IX1014.2
023100 01  CCVS-H-2B.                                                   IX1014.2
023200   02  FILLER                        PIC X(15)  VALUE             IX1014.2
023300            "TEST RESULT OF ".                                    IX1014.2
023400   02  TEST-ID                       PIC X(9).                    IX1014.2
023500   02  FILLER                        PIC X(4)   VALUE             IX1014.2
023600            " IN ".                                               IX1014.2
023700   02  FILLER                        PIC X(12)  VALUE             IX1014.2
023800     " HIGH       ".                                              IX1014.2
023900   02  FILLER                        PIC X(22)  VALUE             IX1014.2
024000            " LEVEL VALIDATION FOR ".                             IX1014.2
024100   02  FILLER                        PIC X(58)  VALUE             IX1014.2
024200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1014.2
024300 01  CCVS-H-3.                                                    IX1014.2
024400     02  FILLER                      PIC X(34)  VALUE             IX1014.2
024500            " FOR OFFICIAL USE ONLY    ".                         IX1014.2
024600     02  FILLER                      PIC X(58)  VALUE             IX1014.2
024700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1014.2
024800     02  FILLER                      PIC X(28)  VALUE             IX1014.2
024900            "  COPYRIGHT   1985 ".                                IX1014.2
025000 01  CCVS-E-1.                                                    IX1014.2
025100     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1014.2
025200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1014.2
025300     02 ID-AGAIN                     PIC X(9).                    IX1014.2
025400     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1014.2
025500 01  CCVS-E-2.                                                    IX1014.2
025600     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1014.2
025700     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1014.2
025800     02 CCVS-E-2-2.                                               IX1014.2
025900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1014.2
026000         03 FILLER                   PIC X      VALUE SPACE.      IX1014.2
026100         03 ENDER-DESC               PIC X(44)  VALUE             IX1014.2
026200            "ERRORS ENCOUNTERED".                                 IX1014.2
026300 01  CCVS-E-3.                                                    IX1014.2
026400     02  FILLER                      PIC X(22)  VALUE             IX1014.2
026500            " FOR OFFICIAL USE ONLY".                             IX1014.2
026600     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1014.2
026700     02  FILLER                      PIC X(58)  VALUE             IX1014.2
026800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1014.2
026900     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1014.2
027000     02 FILLER                       PIC X(15)  VALUE             IX1014.2
027100             " COPYRIGHT 1985".                                   IX1014.2
027200 01  CCVS-E-4.                                                    IX1014.2
027300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1014.2
027400     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1014.2
027500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1014.2
027600     02 FILLER                       PIC X(40)  VALUE             IX1014.2
027700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1014.2
027800 01  XXINFO.                                                      IX1014.2
027900     02 FILLER                       PIC X(19)  VALUE             IX1014.2
028000            "*** INFORMATION ***".                                IX1014.2
028100     02 INFO-TEXT.                                                IX1014.2
028200       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1014.2
028300       04 XXCOMPUTED                 PIC X(20).                   IX1014.2
028400       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1014.2
028500       04 XXCORRECT                  PIC X(20).                   IX1014.2
028600     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1014.2
028700 01  HYPHEN-LINE.                                                 IX1014.2
028800     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1014.2
028900     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1014.2
029000-    "*****************************************".                 IX1014.2
029100     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1014.2
029200-    "******************************".                            IX1014.2
029300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1014.2
029400     "IX101A".                                                    IX1014.2
029500 PROCEDURE DIVISION.                                              IX1014.2
029600 CCVS1 SECTION.                                                   IX1014.2
029700 OPEN-FILES.                                                      IX1014.2
029800*    OPEN I-O RAW-DATA.                                           IX1014.2
029900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1014.2
030000*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1014.2
030100*    MOVE "ABORTED " TO C-ABORT.                                  IX1014.2
030200*    ADD 1 TO C-NO-OF-TESTS.                                      IX1014.2
030300*    ACCEPT C-DATE  FROM DATE.                                    IX1014.2
030400*    ACCEPT C-TIME  FROM TIME.                                    IX1014.2
030500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1014.2
030600*END-E-1.                                                         IX1014.2
030700*    CLOSE RAW-DATA.                                              IX1014.2
030800     OPEN    OUTPUT PRINT-FILE.                                   IX1014.2
030900     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1014.2
031000     MOVE    SPACE TO TEST-RESULTS.                               IX1014.2
031100     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1014.2
031200     MOVE    ZERO TO REC-SKL-SUB.                                 IX1014.2
031300     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1014.2
031400 CCVS-INIT-FILE.                                                  IX1014.2
031500     ADD     1 TO REC-SKL-SUB.                                    IX1014.2
031600     MOVE    FILE-RECORD-INFO-SKELETON                            IX1014.2
031700          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1014.2
031800 CCVS-INIT-EXIT.                                                  IX1014.2
031900     GO TO CCVS1-EXIT.                                            IX1014.2
032000 CLOSE-FILES.                                                     IX1014.2
032100*    OPEN I-O RAW-DATA.                                           IX1014.2
032200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1014.2
032300*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1014.2
032400*    MOVE "OK.     " TO C-ABORT.                                  IX1014.2
032500*    MOVE PASS-COUNTER TO C-OK.                                   IX1014.2
032600*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1014.2
032700*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1014.2
032800*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1014.2
032900*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1014.2
033000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1014.2
033100*END-E-2.                                                         IX1014.2
033200*    CLOSE RAW-DATA.                                              IX1014.2
033300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1014.2
033400 TERMINATE-CCVS.                                                  IX1014.2
033500*    EXIT PROGRAM.                                                IX1014.2
033600*TERMINATE-CALL.                                                  IX1014.2
033700     STOP     RUN.                                                IX1014.2
033800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1014.2
033900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1014.2
034000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1014.2
034100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1014.2
034200     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1014.2
034300 PRINT-DETAIL.                                                    IX1014.2
034400     IF REC-CT NOT EQUAL TO ZERO                                  IX1014.2
034500             MOVE "." TO PARDOT-X                                 IX1014.2
034600             MOVE REC-CT TO DOTVALUE.                             IX1014.2
034700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1014.2
034800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1014.2
034900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1014.2
035000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1014.2
035100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1014.2
035200     MOVE SPACE TO CORRECT-X.                                     IX1014.2
035300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1014.2
035400     MOVE     SPACE TO RE-MARK.                                   IX1014.2
035500 HEAD-ROUTINE.                                                    IX1014.2
035600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1014.2
035700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1014.2
035800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1014.2
035900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1014.2
036000 COLUMN-NAMES-ROUTINE.                                            IX1014.2
036100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1014.2
036200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1014.2
036300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1014.2
036400 END-ROUTINE.                                                     IX1014.2
036500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1014.2
036600 END-RTN-EXIT.                                                    IX1014.2
036700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1014.2
036800 END-ROUTINE-1.                                                   IX1014.2
036900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1014.2
037000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1014.2
037100      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1014.2
037200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1014.2
037300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1014.2
037400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1014.2
037500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1014.2
037600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1014.2
037700  END-ROUTINE-12.                                                 IX1014.2
037800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1014.2
037900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1014.2
038000         MOVE "NO " TO ERROR-TOTAL                                IX1014.2
038100         ELSE                                                     IX1014.2
038200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1014.2
038300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1014.2
038400     PERFORM WRITE-LINE.                                          IX1014.2
038500 END-ROUTINE-13.                                                  IX1014.2
038600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1014.2
038700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1014.2
038800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1014.2
038900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1014.2
039000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1014.2
039100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1014.2
039200          MOVE "NO " TO ERROR-TOTAL                               IX1014.2
039300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1014.2
039400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1014.2
039500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1014.2
039600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1014.2
039700 WRITE-LINE.                                                      IX1014.2
039800     ADD 1 TO RECORD-COUNT.                                       IX1014.2
039900*    IF RECORD-COUNT GREATER 42                                   IX1014.2
040000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1014.2
040100*        MOVE SPACE TO DUMMY-RECORD                               IX1014.2
040200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1014.2
040300*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1014.2
040400*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1014.2
040500*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1014.2
040600*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1014.2
040700*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1014.2
040800*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1014.2
040900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1014.2
041000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1014.2
041100*        MOVE ZERO TO RECORD-COUNT.                               IX1014.2
041200     PERFORM WRT-LN.                                              IX1014.2
041300 WRT-LN.                                                          IX1014.2
041400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1014.2
041500     MOVE SPACE TO DUMMY-RECORD.                                  IX1014.2
041600 BLANK-LINE-PRINT.                                                IX1014.2
041700     PERFORM WRT-LN.                                              IX1014.2
041800 FAIL-ROUTINE.                                                    IX1014.2
041900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1014.2
042000            GO TO   FAIL-ROUTINE-WRITE.                           IX1014.2
042100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1014.2
042200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1014.2
042300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1014.2
042400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1014.2
042500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1014.2
042600     GO TO  FAIL-ROUTINE-EX.                                      IX1014.2
042700 FAIL-ROUTINE-WRITE.                                              IX1014.2
042800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1014.2
042900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1014.2
043000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1014.2
043100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1014.2
043200 FAIL-ROUTINE-EX. EXIT.                                           IX1014.2
043300 BAIL-OUT.                                                        IX1014.2
043400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1014.2
043500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1014.2
043600 BAIL-OUT-WRITE.                                                  IX1014.2
043700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1014.2
043800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1014.2
043900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1014.2
044000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1014.2
044100 BAIL-OUT-EX. EXIT.                                               IX1014.2
044200 CCVS1-EXIT.                                                      IX1014.2
044300     EXIT.                                                        IX1014.2
044400 SECT-IX-01-001 SECTION.                                          IX1014.2
044500 WRITE-INIT-GF-01.                                                IX1014.2
044600     MOVE     "FILE CREATE IX-FS1" TO FEATURE.                    IX1014.2
044700     OPEN     OUTPUT    IX-FS1.                                   IX1014.2
044800     MOVE     "IX-FS1" TO XFILE-NAME (1).                         IX1014.2
044900     MOVE    "IX-F-G" TO XRECORD-NAME (1).                        IX1014.2
045000     MOVE  CCVS-PGM-ID TO XPROGRAM-NAME (1).                      IX1014.2
045100     MOVE    000240  TO XRECORD-LENGTH (1).                       IX1014.2
045200     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   IX1014.2
045300     MOVE     0001     TO XBLOCK-SIZE (1).                        IX1014.2
045400     MOVE     000500   TO RECORDS-IN-FILE (1).                    IX1014.2
045500     MOVE    "IX"  TO XFILE-ORGANIZATION (1).                     IX1014.2
045600     MOVE     "S"      TO XLABEL-TYPE (1).                        IX1014.2
045700     MOVE     000001   TO XRECORD-NUMBER (1).                     IX1014.2
045800 WRITE-TEST-GF-01.                                                IX1014.2
045900     MOVE    XRECORD-NUMBER (1) TO WRK-DU-09V00-001.              IX1014.2
046000     MOVE    GRP-0101 TO XRECORD-KEY (1).                         IX1014.2
046100     MOVE    FILE-RECORD-INFO (1) TO IX-FS1R1-F-G-240.            IX1014.2
046200     WRITE   IX-FS1R1-F-G-240                                     IX1014.2
046300              INVALID KEY GO TO WRITE-FAIL-GF-01.                 IX1014.2
046400     IF      XRECORD-NUMBER (1) EQUAL TO 500                      IX1014.2
046500             GO TO WRITE-PASS-GF-01.                              IX1014.2
046600     ADD      000001 TO XRECORD-NUMBER (1).                       IX1014.2
046700     GO       TO WRITE-TEST-GF-01.                                IX1014.2
046800 WRITE-FAIL-GF-01.                                                IX1014.2
046900     MOVE     "IX-41 4.9.2        " TO RE-MARK.                   IX1014.2
047000     PERFORM FAIL.                                                IX1014.2
047100     GO TO WRITE-WRITE-GF-01.                                     IX1014.2
047200 WRITE-PASS-GF-01.                                                IX1014.2
047300     PERFORM PASS.                                                IX1014.2
047400 WRITE-WRITE-GF-01.                                               IX1014.2
047500     MOVE     "WRITE-TEST-GF-01" TO PAR-NAME                      IX1014.2
047600     MOVE     "FILE CREATED, LFILE "  TO COMPUTED-A.              IX1014.2
047700     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  IX1014.2
047800     PERFORM  PRINT-DETAIL.                                       IX1014.2
047900     CLOSE    IX-FS1.                                             IX1014.2
048000 READ-INIT-GF-01.                                                 IX1014.2
048100     OPEN     INPUT     IX-FS1.                                   IX1014.2
048200     MOVE   ZERO TO WRK-DU-09V00-001.                             IX1014.2
048300 READ-TEST-GF-01.                                                 IX1014.2
048400     READ     IX-FS1                                              IX1014.2
048500              AT END GO TO READ-TEST-GF-01-1.                     IX1014.2
048600     MOVE    IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).            IX1014.2
048700     ADD     1  TO WRK-DU-09V00-001.                              IX1014.2
048800     IF      WRK-DU-09V00-001 GREATER 500                         IX1014.2
048900             MOVE "MORE THAN 500 RECORDS" TO RE-MARK              IX1014.2
049000              GO TO READ-TEST-GF-01-1.                            IX1014.2
049100     GO       TO READ-TEST-GF-01.                                 IX1014.2
049200 READ-TEST-GF-01-1.                                               IX1014.2
049300     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 IX1014.2
049400              MOVE "IX-28 4.5.2 " TO RE-MARK                      IX1014.2
049500              PERFORM FAIL                                        IX1014.2
049600              ELSE                                                IX1014.2
049700              PERFORM PASS.                                       IX1014.2
049800     GO       TO READ-WRITE-GF-01.                                IX1014.2
049900 READ-WRITE-GF-01.                                                IX1014.2
050000     MOVE     "READ-TEST-GF-01" TO PAR-NAME.                      IX1014.2
050100     MOVE     "FILE VERIFIED, LFILE" TO COMPUTED-A.               IX1014.2
050200     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  IX1014.2
050300     PERFORM  PRINT-DETAIL.                                       IX1014.2
050400     CLOSE   IX-FS1.                                              IX1014.2
050500 CCVS-EXIT SECTION.                                               IX1014.2
050600 CCVS-999999.                                                     IX1014.2
050700     GO TO   CLOSE-FILES.                                         IX1014.2
      *END-OF,IX101A                                                                  
