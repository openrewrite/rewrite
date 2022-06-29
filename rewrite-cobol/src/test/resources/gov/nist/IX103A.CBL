      *HEADER,COBOL,IX101A,SUBPRG,IX103A                                              
000100 IDENTIFICATION DIVISION.                                         IX1034.2
000200 PROGRAM-ID.                                                      IX1034.2
000300     IX103A.                                                      IX1034.2
000400****************************************************************  IX1034.2
000500*                                                              *  IX1034.2
000600*    VALIDATION FOR:-                                          *  IX1034.2
000700*                                                              *  IX1034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1034.2
000900*                                                              *  IX1034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1034.2
001100*                                                              *  IX1034.2
001200****************************************************************  IX1034.2
001300*                                                                 IX1034.2
001400*         NEW TEST:                                               IX1034.2
001500*              SELECT ...      INDEXED  ...                       IX1034.2
001600*              (WITHOUT THE OPTIONAL WORD <ORGANIZATION>)         IX1034.2
001700*                                                                 IX1034.2
001800*    THIS PROGRAM IS THE THIRD OF A SERIES.  ITS FUNCTION         IX1034.2
001900*    IS TO PROCESS THE FILE SEQUENTIALLY (ACCESS MODE IS          IX1034.2
002000*    SEQUENTIAL).  THE FILE USED IS THAT RESULTING FROM IX102.    IX1034.2
002100*                                                                 IX1034.2
002200*    FIRST, THE FILE IS VERIFIED FOR ACCURACY OF ITS 500 RECORDS. IX1034.2
002300*    SECONDLY, RECORDS OF THE FILE ARE SELECTIVELY DELETED AND    IX1034.2
002400*    THIRDLY THE ACCURACY OF EACH RECORD IN THE FILE IS AGAIN     IX1034.2
002500*    VERIFIED.                                                    IX1034.2
002600*                                                                 IX1034.2
002700*                                                                 IX1034.2
002800*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX1034.2
002900*                                                                 IX1034.2
003000*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1034.2
003100*                    CLAUSE FOR DATA FILE IX-FS1                  IX1034.2
003200*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1034.2
003300*                    CLAUSE FOR INDEX FILE IX-FS1                 IX1034.2
003400*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX1034.2
003500*             X-62   IMPLEMENTOR-NAME FOR RAW-DATA                IX1034.2
003600*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX1034.2
003700*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX1034.2
003800*                                                                 IX1034.2
003900*         NOTE:  X-CARDS 44 AND 62          ARE OPTIONAL          IX1034.2
004000*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX1034.2
004100*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX1034.2
004200*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX1034.2
004300*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX1034.2
004400*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX1034.2
004500*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX1034.2
004600*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX1034.2
004700*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX1034.2
004800*               THEY ARE AS FOLLOWS                               IX1034.2
004900*                                                                 IX1034.2
005000*                  J  SELECTS X-CARD 44                           IX1034.2
005100*                  P  SELECTS X-CARD 62                           IX1034.2
005200*                                                                 IX1034.2
005300******************************************************            IX1034.2
005400 ENVIRONMENT DIVISION.                                            IX1034.2
005500 CONFIGURATION SECTION.                                           IX1034.2
005600 SOURCE-COMPUTER.                                                 IX1034.2
005700     XXXXX082.                                                    IX1034.2
005800 OBJECT-COMPUTER.                                                 IX1034.2
005900     XXXXX083.                                                    IX1034.2
006000 INPUT-OUTPUT SECTION.                                            IX1034.2
006100 FILE-CONTROL.                                                    IX1034.2
006200*    SELECT RAW-DATA   ASSIGN TO                                  IX1034.2
006300*    XXXXX062                                                     IX1034.2
006400*           ORGANIZATION IS INDEXED                               IX1034.2
006500*           ACCESS MODE IS RANDOM                                 IX1034.2
006600*           RECORD KEY IS RAW-DATA-KEY.                           IX1034.2
006700     SELECT PRINT-FILE ASSIGN TO                                  IX1034.2
006800     XXXXX055.                                                    IX1034.2
006900     SELECT   IX-FS1 ASSIGN TO                                    IX1034.2
007000     XXXXD024                                                     IX1034.2
007100*    XXXXD044                                                     IX1034.2
007200                     INDEXED                                      IX1034.2
007300     RECORD    IX-FS1-KEY.                                        IX1034.2
007400*    THE ABSENCE OF THE ACCESS CLAUSE IS TREATED AS THOUGH        IX1034.2
007500*        SEQUENTIAL HAD BEEN SPECIFIED.                           IX1034.2
007600 DATA DIVISION.                                                   IX1034.2
007700 FILE SECTION.                                                    IX1034.2
007800*                                                                 IX1034.2
007900*FD  RAW-DATA.                                                    IX1034.2
008000*                                                                 IX1034.2
008100*01  RAW-DATA-SATZ.                                               IX1034.2
008200*    05  RAW-DATA-KEY        PIC X(6).                            IX1034.2
008300*    05  C-DATE              PIC 9(6).                            IX1034.2
008400*    05  C-TIME              PIC 9(8).                            IX1034.2
008500*    05  C-NO-OF-TESTS       PIC 99.                              IX1034.2
008600*    05  C-OK                PIC 999.                             IX1034.2
008700*    05  C-ALL               PIC 999.                             IX1034.2
008800*    05  C-FAIL              PIC 999.                             IX1034.2
008900*    05  C-DELETED           PIC 999.                             IX1034.2
009000*    05  C-INSPECT           PIC 999.                             IX1034.2
009100*    05  C-NOTE              PIC X(13).                           IX1034.2
009200*    05  C-INDENT            PIC X.                               IX1034.2
009300*    05  C-ABORT             PIC X(8).                            IX1034.2
009400 FD  PRINT-FILE.                                                  IX1034.2
009500 01  PRINT-REC PICTURE X(120).                                    IX1034.2
009600 01  DUMMY-RECORD PICTURE X(120).                                 IX1034.2
009700 FD  IX-FS1                                                       IX1034.2
009800C    LABEL RECORD STANDARD                                        IX1034.2
009900C    DATA RECORDS ARE  IX-FS1R1-F-G-240                           IX1034.2
010000     ; BLOCK CONTAINS 01 RECORDS                                  IX1034.2
010100     RECORD CONTAINS  240.                                        IX1034.2
010200 01  IX-FS1R1-F-G-240.                                            IX1034.2
010300     05 IX-FS1-REC-120   PIC  X(120).                             IX1034.2
010400     05 IX-FS1-REC-120-240.                                       IX1034.2
010500        10 FILLER   PIC X(8).                                     IX1034.2
010600        10 IX-FS1-KEY   PIC X(29).                                IX1034.2
010700        10 FILLER        PIC X(83).                               IX1034.2
010800 WORKING-STORAGE SECTION.                                         IX1034.2
010900 01  WRK-CS-09V00-006 PIC S9(09) USAGE COMP VALUE ZERO.           IX1034.2
011000 01  WRK-CS-09V00-007 PIC S9(09) USAGE COMP VALUE ZERO.           IX1034.2
011100 01  WRK-CS-09V00-008 PIC S9(09) USAGE COMP VALUE ZERO.           IX1034.2
011200 01  WRK-CS-09V00-009 PIC S9(09) USAGE COMP VALUE ZERO.           IX1034.2
011300 01  WRK-CS-09V00-010 PIC S9(09) USAGE COMP VALUE ZERO.           IX1034.2
011400 01  WRK-CS-09V00-011 PIC S9(09) USAGE COMP VALUE ZERO.           IX1034.2
011500 01  I-O-ERROR-IX-FS1 PIC X(3) VALUE "NO ".                       IX1034.2
011600 01  IX-WRK-KEY.                                                  IX1034.2
011700     03 FILLER         PIC X(10).                                 IX1034.2
011800     03 WRK-DU-09V00-001  PIC 9(9).                               IX1034.2
011900     03 FILLER         PIC X(10).                                 IX1034.2
012000 01  DUMMY-WRK-REC.                                               IX1034.2
012100     02 DUMMY-WRK1       PIC X(120).                              IX1034.2
012200     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX1034.2
012300        03 FILLER   PIC X(5).                                     IX1034.2
012400        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX1034.2
012500 01  FILE-RECORD-INFORMATION-REC.                                 IX1034.2
012600     03 FILE-RECORD-INFO-SKELETON.                                IX1034.2
012700        05 FILLER                 PICTURE X(48)       VALUE       IX1034.2
012800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1034.2
012900        05 FILLER                 PICTURE X(46)       VALUE       IX1034.2
013000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1034.2
013100        05 FILLER                 PICTURE X(26)       VALUE       IX1034.2
013200             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1034.2
013300        05 FILLER                 PICTURE X(37)       VALUE       IX1034.2
013400             ",RECKEY=                             ".             IX1034.2
013500        05 FILLER                 PICTURE X(38)       VALUE       IX1034.2
013600             ",ALTKEY1=                             ".            IX1034.2
013700        05 FILLER                 PICTURE X(38)       VALUE       IX1034.2
013800             ",ALTKEY2=                             ".            IX1034.2
013900        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1034.2
014000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1034.2
014100        05 FILE-RECORD-INFO-P1-120.                               IX1034.2
014200           07 FILLER              PIC X(5).                       IX1034.2
014300           07 XFILE-NAME           PIC X(6).                      IX1034.2
014400           07 FILLER              PIC X(8).                       IX1034.2
014500           07 XRECORD-NAME         PIC X(6).                      IX1034.2
014600           07 FILLER              PIC X(1).                       IX1034.2
014700           07 REELUNIT-NUMBER     PIC 9(1).                       IX1034.2
014800           07 FILLER              PIC X(7).                       IX1034.2
014900           07 XRECORD-NUMBER       PIC 9(6).                      IX1034.2
015000           07 FILLER              PIC X(6).                       IX1034.2
015100           07 UPDATE-NUMBER       PIC 9(2).                       IX1034.2
015200           07 FILLER              PIC X(5).                       IX1034.2
015300           07 ODO-NUMBER          PIC 9(4).                       IX1034.2
015400           07 FILLER              PIC X(5).                       IX1034.2
015500           07 XPROGRAM-NAME        PIC X(5).                      IX1034.2
015600           07 FILLER              PIC X(7).                       IX1034.2
015700           07 XRECORD-LENGTH       PIC 9(6).                      IX1034.2
015800           07 FILLER              PIC X(7).                       IX1034.2
015900           07 CHARS-OR-RECORDS    PIC X(2).                       IX1034.2
016000           07 FILLER              PIC X(1).                       IX1034.2
016100           07 XBLOCK-SIZE          PIC 9(4).                      IX1034.2
016200           07 FILLER              PIC X(6).                       IX1034.2
016300           07 RECORDS-IN-FILE     PIC 9(6).                       IX1034.2
016400           07 FILLER              PIC X(5).                       IX1034.2
016500           07 XFILE-ORGANIZATION   PIC X(2).                      IX1034.2
016600           07 FILLER              PIC X(6).                       IX1034.2
016700           07 XLABEL-TYPE          PIC X(1).                      IX1034.2
016800        05 FILE-RECORD-INFO-P121-240.                             IX1034.2
016900           07 FILLER              PIC X(8).                       IX1034.2
017000           07 XRECORD-KEY          PIC X(29).                     IX1034.2
017100           07 FILLER              PIC X(9).                       IX1034.2
017200           07 ALTERNATE-KEY1      PIC X(29).                      IX1034.2
017300           07 FILLER              PIC X(9).                       IX1034.2
017400           07 ALTERNATE-KEY2      PIC X(29).                      IX1034.2
017500           07 FILLER              PIC X(7).                       IX1034.2
017600 01  TEST-RESULTS.                                                IX1034.2
017700     02 FILLER                   PIC X      VALUE SPACE.          IX1034.2
017800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1034.2
017900     02 FILLER                   PIC X      VALUE SPACE.          IX1034.2
018000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1034.2
018100     02 FILLER                   PIC X      VALUE SPACE.          IX1034.2
018200     02  PAR-NAME.                                                IX1034.2
018300       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1034.2
018400       03  PARDOT-X              PIC X      VALUE SPACE.          IX1034.2
018500       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1034.2
018600     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1034.2
018700     02 RE-MARK                  PIC X(61).                       IX1034.2
018800 01  TEST-COMPUTED.                                               IX1034.2
018900     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1034.2
019000     02 FILLER                   PIC X(17)  VALUE                 IX1034.2
019100            "       COMPUTED=".                                   IX1034.2
019200     02 COMPUTED-X.                                               IX1034.2
019300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1034.2
019400     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1034.2
019500                                 PIC -9(9).9(9).                  IX1034.2
019600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1034.2
019700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1034.2
019800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1034.2
019900     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1034.2
020000         04 COMPUTED-18V0                    PIC -9(18).          IX1034.2
020100         04 FILLER                           PIC X.               IX1034.2
020200     03 FILLER PIC X(50) VALUE SPACE.                             IX1034.2
020300 01  TEST-CORRECT.                                                IX1034.2
020400     02 FILLER PIC X(30) VALUE SPACE.                             IX1034.2
020500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1034.2
020600     02 CORRECT-X.                                                IX1034.2
020700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1034.2
020800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1034.2
020900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1034.2
021000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1034.2
021100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1034.2
021200     03      CR-18V0 REDEFINES CORRECT-A.                         IX1034.2
021300         04 CORRECT-18V0                     PIC -9(18).          IX1034.2
021400         04 FILLER                           PIC X.               IX1034.2
021500     03 FILLER PIC X(2) VALUE SPACE.                              IX1034.2
021600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1034.2
021700 01  CCVS-C-1.                                                    IX1034.2
021800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1034.2
021900-    "SS  PARAGRAPH-NAME                                          IX1034.2
022000-    "       REMARKS".                                            IX1034.2
022100     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1034.2
022200 01  CCVS-C-2.                                                    IX1034.2
022300     02 FILLER                     PIC X        VALUE SPACE.      IX1034.2
022400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1034.2
022500     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1034.2
022600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1034.2
022700     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1034.2
022800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1034.2
022900 01  REC-CT                        PIC 99       VALUE ZERO.       IX1034.2
023000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1034.2
023100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1034.2
023200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1034.2
023300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1034.2
023400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1034.2
023500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1034.2
023600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1034.2
023700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1034.2
023800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1034.2
023900 01  CCVS-H-1.                                                    IX1034.2
024000     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1034.2
024100     02  FILLER                    PIC X(42)    VALUE             IX1034.2
024200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1034.2
024300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1034.2
024400 01  CCVS-H-2A.                                                   IX1034.2
024500   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1034.2
024600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1034.2
024700   02  FILLER                        PIC XXXX   VALUE             IX1034.2
024800     "4.2 ".                                                      IX1034.2
024900   02  FILLER                        PIC X(28)  VALUE             IX1034.2
025000            " COPY - NOT FOR DISTRIBUTION".                       IX1034.2
025100   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1034.2
025200                                                                  IX1034.2
025300 01  CCVS-H-2B.                                                   IX1034.2
025400   02  FILLER                        PIC X(15)  VALUE             IX1034.2
025500            "TEST RESULT OF ".                                    IX1034.2
025600   02  TEST-ID                       PIC X(9).                    IX1034.2
025700   02  FILLER                        PIC X(4)   VALUE             IX1034.2
025800            " IN ".                                               IX1034.2
025900   02  FILLER                        PIC X(12)  VALUE             IX1034.2
026000     " HIGH       ".                                              IX1034.2
026100   02  FILLER                        PIC X(22)  VALUE             IX1034.2
026200            " LEVEL VALIDATION FOR ".                             IX1034.2
026300   02  FILLER                        PIC X(58)  VALUE             IX1034.2
026400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1034.2
026500 01  CCVS-H-3.                                                    IX1034.2
026600     02  FILLER                      PIC X(34)  VALUE             IX1034.2
026700            " FOR OFFICIAL USE ONLY    ".                         IX1034.2
026800     02  FILLER                      PIC X(58)  VALUE             IX1034.2
026900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1034.2
027000     02  FILLER                      PIC X(28)  VALUE             IX1034.2
027100            "  COPYRIGHT   1985 ".                                IX1034.2
027200 01  CCVS-E-1.                                                    IX1034.2
027300     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1034.2
027400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1034.2
027500     02 ID-AGAIN                     PIC X(9).                    IX1034.2
027600     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1034.2
027700 01  CCVS-E-2.                                                    IX1034.2
027800     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1034.2
027900     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1034.2
028000     02 CCVS-E-2-2.                                               IX1034.2
028100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1034.2
028200         03 FILLER                   PIC X      VALUE SPACE.      IX1034.2
028300         03 ENDER-DESC               PIC X(44)  VALUE             IX1034.2
028400            "ERRORS ENCOUNTERED".                                 IX1034.2
028500 01  CCVS-E-3.                                                    IX1034.2
028600     02  FILLER                      PIC X(22)  VALUE             IX1034.2
028700            " FOR OFFICIAL USE ONLY".                             IX1034.2
028800     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1034.2
028900     02  FILLER                      PIC X(58)  VALUE             IX1034.2
029000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1034.2
029100     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1034.2
029200     02 FILLER                       PIC X(15)  VALUE             IX1034.2
029300             " COPYRIGHT 1985".                                   IX1034.2
029400 01  CCVS-E-4.                                                    IX1034.2
029500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1034.2
029600     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1034.2
029700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1034.2
029800     02 FILLER                       PIC X(40)  VALUE             IX1034.2
029900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1034.2
030000 01  XXINFO.                                                      IX1034.2
030100     02 FILLER                       PIC X(19)  VALUE             IX1034.2
030200            "*** INFORMATION ***".                                IX1034.2
030300     02 INFO-TEXT.                                                IX1034.2
030400       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1034.2
030500       04 XXCOMPUTED                 PIC X(20).                   IX1034.2
030600       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1034.2
030700       04 XXCORRECT                  PIC X(20).                   IX1034.2
030800     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1034.2
030900 01  HYPHEN-LINE.                                                 IX1034.2
031000     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1034.2
031100     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1034.2
031200-    "*****************************************".                 IX1034.2
031300     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1034.2
031400-    "******************************".                            IX1034.2
031500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1034.2
031600     "IX103A".                                                    IX1034.2
031700 PROCEDURE DIVISION.                                              IX1034.2
031800 DECLARATIVES.                                                    IX1034.2
031900 USE-IX103-TEST  SECTION.                                         IX1034.2
032000     USE   AFTER  STANDARD  EXCEPTION PROCEDURE                   IX1034.2
032100             IX-FS1.                                              IX1034.2
032200 USE-PAR-001.                                                     IX1034.2
032300     ADD   1   TO WRK-CS-09V00-009.                               IX1034.2
032400 USE-PAR-EXIT.                                                    IX1034.2
032500     EXIT.                                                        IX1034.2
032600 END DECLARATIVES.                                                IX1034.2
032700 CCVS1 SECTION.                                                   IX1034.2
032800 OPEN-FILES.                                                      IX1034.2
032900*    OPEN I-O RAW-DATA.                                           IX1034.2
033000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1034.2
033100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1034.2
033200*    MOVE "ABORTED " TO C-ABORT.                                  IX1034.2
033300*    ADD 1 TO C-NO-OF-TESTS.                                      IX1034.2
033400*    ACCEPT C-DATE  FROM DATE.                                    IX1034.2
033500*    ACCEPT C-TIME  FROM TIME.                                    IX1034.2
033600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1034.2
033700*END-E-1.                                                         IX1034.2
033800*    CLOSE RAW-DATA.                                              IX1034.2
033900     OPEN    OUTPUT PRINT-FILE.                                   IX1034.2
034000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1034.2
034100     MOVE    SPACE TO TEST-RESULTS.                               IX1034.2
034200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1034.2
034300     MOVE    ZERO TO REC-SKL-SUB.                                 IX1034.2
034400     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1034.2
034500 CCVS-INIT-FILE.                                                  IX1034.2
034600     ADD     1 TO REC-SKL-SUB.                                    IX1034.2
034700     MOVE    FILE-RECORD-INFO-SKELETON                            IX1034.2
034800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1034.2
034900 CCVS-INIT-EXIT.                                                  IX1034.2
035000     GO TO CCVS1-EXIT.                                            IX1034.2
035100 CLOSE-FILES.                                                     IX1034.2
035200*    OPEN I-O RAW-DATA.                                           IX1034.2
035300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1034.2
035400*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1034.2
035500*    MOVE "OK.     " TO C-ABORT.                                  IX1034.2
035600*    MOVE PASS-COUNTER TO C-OK.                                   IX1034.2
035700*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1034.2
035800*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1034.2
035900*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1034.2
036000*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1034.2
036100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1034.2
036200*END-E-2.                                                         IX1034.2
036300*    CLOSE RAW-DATA.                                              IX1034.2
036400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1034.2
036500 TERMINATE-CCVS.                                                  IX1034.2
036600*    EXIT PROGRAM.                                                IX1034.2
036700*TERMINATE-CALL.                                                  IX1034.2
036800     STOP     RUN.                                                IX1034.2
036900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1034.2
037000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1034.2
037100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1034.2
037200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1034.2
037300     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1034.2
037400 PRINT-DETAIL.                                                    IX1034.2
037500     IF REC-CT NOT EQUAL TO ZERO                                  IX1034.2
037600             MOVE "." TO PARDOT-X                                 IX1034.2
037700             MOVE REC-CT TO DOTVALUE.                             IX1034.2
037800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1034.2
037900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1034.2
038000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1034.2
038100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1034.2
038200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1034.2
038300     MOVE SPACE TO CORRECT-X.                                     IX1034.2
038400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1034.2
038500     MOVE     SPACE TO RE-MARK.                                   IX1034.2
038600 HEAD-ROUTINE.                                                    IX1034.2
038700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1034.2
038800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1034.2
038900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1034.2
039000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1034.2
039100 COLUMN-NAMES-ROUTINE.                                            IX1034.2
039200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1034.2
039300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1034.2
039400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1034.2
039500 END-ROUTINE.                                                     IX1034.2
039600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1034.2
039700 END-RTN-EXIT.                                                    IX1034.2
039800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1034.2
039900 END-ROUTINE-1.                                                   IX1034.2
040000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1034.2
040100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1034.2
040200      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1034.2
040300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1034.2
040400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1034.2
040500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1034.2
040600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1034.2
040700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1034.2
040800  END-ROUTINE-12.                                                 IX1034.2
040900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1034.2
041000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1034.2
041100         MOVE "NO " TO ERROR-TOTAL                                IX1034.2
041200         ELSE                                                     IX1034.2
041300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1034.2
041400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1034.2
041500     PERFORM WRITE-LINE.                                          IX1034.2
041600 END-ROUTINE-13.                                                  IX1034.2
041700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1034.2
041800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1034.2
041900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1034.2
042000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1034.2
042100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1034.2
042200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1034.2
042300          MOVE "NO " TO ERROR-TOTAL                               IX1034.2
042400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1034.2
042500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1034.2
042600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1034.2
042700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1034.2
042800 WRITE-LINE.                                                      IX1034.2
042900     ADD 1 TO RECORD-COUNT.                                       IX1034.2
043000*    IF RECORD-COUNT GREATER 42                                   IX1034.2
043100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1034.2
043200*        MOVE SPACE TO DUMMY-RECORD                               IX1034.2
043300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1034.2
043400*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1034.2
043500*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1034.2
043600*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1034.2
043700*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1034.2
043800*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1034.2
043900*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1034.2
044000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1034.2
044100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1034.2
044200*        MOVE ZERO TO RECORD-COUNT.                               IX1034.2
044300     PERFORM WRT-LN.                                              IX1034.2
044400 WRT-LN.                                                          IX1034.2
044500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1034.2
044600     MOVE SPACE TO DUMMY-RECORD.                                  IX1034.2
044700 BLANK-LINE-PRINT.                                                IX1034.2
044800     PERFORM WRT-LN.                                              IX1034.2
044900 FAIL-ROUTINE.                                                    IX1034.2
045000     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1034.2
045100            GO TO   FAIL-ROUTINE-WRITE.                           IX1034.2
045200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1034.2
045300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1034.2
045400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1034.2
045500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1034.2
045600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1034.2
045700     GO TO  FAIL-ROUTINE-EX.                                      IX1034.2
045800 FAIL-ROUTINE-WRITE.                                              IX1034.2
045900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1034.2
046000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1034.2
046100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1034.2
046200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1034.2
046300 FAIL-ROUTINE-EX. EXIT.                                           IX1034.2
046400 BAIL-OUT.                                                        IX1034.2
046500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1034.2
046600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1034.2
046700 BAIL-OUT-WRITE.                                                  IX1034.2
046800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1034.2
046900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1034.2
047000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1034.2
047100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1034.2
047200 BAIL-OUT-EX. EXIT.                                               IX1034.2
047300 CCVS1-EXIT.                                                      IX1034.2
047400     EXIT.                                                        IX1034.2
047500 SECT-IX-03-001 SECTION.                                          IX1034.2
047600 INX-INIT-006.                                                    IX1034.2
047700*    THIS FILE "IX-FS1" IS ACCESSED SEQUENTIALLY AND HAS          IX1034.2
047800*    ASSOCIATED WITH IT A RECORD KEY WHICH AT ALL TIMES SHOULD    IX1034.2
047900*    CONTAIN THE INDEX OF THE RECORD PREVIOUSLY READ.             IX1034.2
048000     OPEN INPUT IX-FS1.                                           IX1034.2
048100     MOVE     "INX-TEST-006" TO   PAR-NAME.                       IX1034.2
048200     MOVE     ZERO TO             WRK-CS-09V00-006.               IX1034.2
048300     MOVE     ZERO TO             WRK-CS-09V00-007.               IX1034.2
048400     MOVE     ZERO TO             WRK-CS-09V00-008.               IX1034.2
048500     MOVE     ZERO TO             WRK-CS-09V00-009.               IX1034.2
048600     MOVE     ZERO TO             WRK-CS-09V00-010.               IX1034.2
048700     MOVE     ZERO TO             WRK-CS-09V00-011.               IX1034.2
048800     MOVE     SPACE TO  FILE-RECORD-INFO (1).                     IX1034.2
048900     MOVE    ZERO TO WRK-DU-09V00-001.                            IX1034.2
049000     MOVE    IX-FS1-KEY TO COMPUTED-A.                            IX1034.2
049100     MOVE    SPACE TO P-OR-F.                                     IX1034.2
049200     MOVE    "INFORMATION" TO CORRECT-A.                          IX1034.2
049300     MOVE    "KEY AFTER OPEN" TO RE-MARK.                         IX1034.2
049400     MOVE    "RECORD KEY ON OPEN"  TO FEATURE.                    IX1034.2
049500     PERFORM PRINT-DETAIL.                                        IX1034.2
049600     MOVE    "INX-TEST-006"  TO PAR-NAME.                         IX1034.2
049700     MOVE    "READ SEQUENTIAL  "  TO FEATURE.                     IX1034.2
049800 INX-TEST-006-R.                                                  IX1034.2
049900     ADD      1 TO WRK-CS-09V00-006.                              IX1034.2
050000     READ     IX-FS1                                              IX1034.2
050100              AT END GO TO READ-TEST-F1-01.                       IX1034.2
050200     MOVE     IX-FS1R1-F-G-240    TO FILE-RECORD-INFO (1).        IX1034.2
050300     IF       UPDATE-NUMBER (1) EQUAL TO 00                       IX1034.2
050400             ADD 1 TO WRK-CS-09V00-007                            IX1034.2
050500              GO TO INX-TEST-006-2.                               IX1034.2
050600     IF       UPDATE-NUMBER (1) EQUAL TO 01                       IX1034.2
050700              ADD 1 TO WRK-CS-09V00-008                           IX1034.2
050800              GO TO INX-TEST-006-2.                               IX1034.2
050900     ADD      1 TO WRK-CS-09V00-009.                              IX1034.2
051000 INX-TEST-006-2.                                                  IX1034.2
051100     MOVE    XRECORD-KEY (1)  TO  IX-WRK-KEY.                     IX1034.2
051200     IF      WRK-DU-09V00-001  NOT EQUAL TO XRECORD-NUMBER (1)    IX1034.2
051300              ADD 1 TO  WRK-CS-09V00-010.                         IX1034.2
051400     IF       WRK-CS-09V00-006  GREATER 501                       IX1034.2
051500              GO TO READ-TEST-F1-01.                              IX1034.2
051600     GO TO    INX-TEST-006-R.                                     IX1034.2
051700 READ-TEST-F1-01.                                                 IX1034.2
051800     MOVE    "READ-TEST-F1-01"  TO PAR-NAME.                      IX1034.2
051900     MOVE    "READ SEQUENTIAL  "  TO FEATURE.                     IX1034.2
052000*                                                                 IX1034.2
052100*  TEST   1                                                       IX1034.2
052200*                                                                 IX1034.2
052300     IF       WRK-CS-09V00-006 NOT EQUAL TO 501                   IX1034.2
052400              MOVE "INCORRECT RECORD COUNT"  TO RE-MARK           IX1034.2
052500              MOVE  WRK-CS-09V00-006 TO COMPUTED-18V0             IX1034.2
052600              MOVE  500  TO             CORRECT-18V0              IX1034.2
052700              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1034.2
052800              PERFORM FAIL                                        IX1034.2
052900              ELSE                                                IX1034.2
053000              PERFORM PASS.                                       IX1034.2
053100     PERFORM  PRINT-DETAIL.                                       IX1034.2
053200 READ-TEST-F1-02.                                                 IX1034.2
053300     MOVE    "READ-TEST-F1-02"  TO PAR-NAME.                      IX1034.2
053400     MOVE    "READ SEQUENTIAL  "  TO FEATURE.                     IX1034.2
053500*                                                                 IX1034.2
053600*  TEST   2                                                       IX1034.2
053700*                                                                 IX1034.2
053800     IF       WRK-CS-09V00-007 EQUAL TO 400                       IX1034.2
053900              PERFORM PASS                                        IX1034.2
054000              ELSE                                                IX1034.2
054100              MOVE "NON-UPDATED RECORDS" TO COMPUTED-A            IX1034.2
054200              MOVE  WRK-CS-09V00-007 TO CORRECT-18V0              IX1034.2
054300              MOVE "SHOULD BE 400; IX-28 4.5.2" TO RE-MARK        IX1034.2
054400              PERFORM FAIL.                                       IX1034.2
054500     PERFORM  PRINT-DETAIL.                                       IX1034.2
054600 READ-TEST-F1-03.                                                 IX1034.2
054700     MOVE    "READ-TEST-F1-03"  TO PAR-NAME.                      IX1034.2
054800     MOVE    "READ SEQUENTIAL  "  TO FEATURE.                     IX1034.2
054900*                                                                 IX1034.2
055000*  TEST   3                                                       IX1034.2
055100*                                                                 IX1034.2
055200     IF      WRK-CS-09V00-008 EQUAL TO 100                        IX1034.2
055300              PERFORM PASS                                        IX1034.2
055400              ELSE                                                IX1034.2
055500             MOVE WRK-CS-09V00-008 TO COMPUTED-18V0               IX1034.2
055600             MOVE 100             TO  CORRECT-18V0                IX1034.2
055700              MOVE "IX-28 4.5.2                       " TO RE-MARKIX1034.2
055800              PERFORM FAIL.                                       IX1034.2
055900     PERFORM  PRINT-DETAIL.                                       IX1034.2
056000 READ-TEST-F1-04.                                                 IX1034.2
056100     MOVE    "READ-TEST-F1-04"  TO PAR-NAME.                      IX1034.2
056200     MOVE    "READ SEQUENTIAL  "  TO FEATURE.                     IX1034.2
056300*                                                                 IX1034.2
056400*  TEST   4                                                       IX1034.2
056500*                                                                 IX1034.2
056600     IF      WRK-CS-09V00-009 EQUAL TO ZERO                       IX1034.2
056700             PERFORM PASS                                         IX1034.2
056800             ELSE                                                 IX1034.2
056900             MOVE WRK-CS-09V00-009 TO COMPUTED-18V0               IX1034.2
057000             MOVE  ZERO            TO CORRECT-18V0                IX1034.2
057100             MOVE "BAD-UPDATES" TO RE-MARK                        IX1034.2
057200             PERFORM FAIL.                                        IX1034.2
057300     PERFORM PRINT-DETAIL.                                        IX1034.2
057400 READ-TEST-F1-05.                                                 IX1034.2
057500     MOVE    "READ-TEST-F1-05"  TO PAR-NAME.                      IX1034.2
057600     MOVE    "READ SEQUENTIAL  "  TO FEATURE.                     IX1034.2
057700*                                                                 IX1034.2
057800*  TEST   5                                                       IX1034.2
057900*                                                                 IX1034.2
058000     IF      WRK-CS-09V00-010 EQUAL TO ZERO                       IX1034.2
058100             PERFORM PASS                                         IX1034.2
058200             ELSE                                                 IX1034.2
058300             MOVE WRK-CS-09V00-010 TO COMPUTED-18V0               IX1034.2
058400             MOVE ZERO             TO CORRECT-18V0                IX1034.2
058500             MOVE "KEY VS RECORD; IX-28" TO RE-MARK               IX1034.2
058600             PERFORM FAIL.                                        IX1034.2
058700     PERFORM PRINT-DETAIL.                                        IX1034.2
058800     CLOSE    IX-FS1.                                             IX1034.2
058900 DELETE-INIT-GF-01.                                               IX1034.2
059000*                                                                 IX1034.2
059100*  TEST   1                                                       IX1034.2
059200*                                                                 IX1034.2
059300     MOVE     "DELETE-TEST-GF-01" TO PAR-NAME                     IX1034.2
059400     OPEN     I-O IX-FS1.                                         IX1034.2
059500     MOVE     ZERO TO WRK-CS-09V00-006                            IX1034.2
059600     MOVE     ZERO TO WRK-CS-09V00-007                            IX1034.2
059700     MOVE     ZERO TO WRK-CS-09V00-008                            IX1034.2
059800     MOVE     ZERO TO WRK-CS-09V00-009                            IX1034.2
059900     MOVE     ZERO TO WRK-CS-09V00-010                            IX1034.2
060000     MOVE     ZERO TO WRK-CS-09V00-011                            IX1034.2
060100     MOVE     SPACE TO  FILE-RECORD-INFO (1).                     IX1034.2
060200     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
060300 DELETE-TEST-GF-01.                                               IX1034.2
060400     ADD      1 TO WRK-CS-09V00-006                               IX1034.2
060500     ADD      1 TO WRK-CS-09V00-007.                              IX1034.2
060600     READ     IX-FS1                                              IX1034.2
060700              AT END                                              IX1034.2
060800             MOVE  "AT END PATH TAKEN"  TO RE-MARK                IX1034.2
060900             GO TO  DELETE-TEST-GF-01-3.                          IX1034.2
061000     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX1034.2
061100     IF       WRK-CS-09V00-007  EQUAL TO 4                        IX1034.2
061200              GO TO DELETE-TEST-GF-01-2.                          IX1034.2
061300     IF       WRK-CS-09V00-006 GREATER 501                        IX1034.2
061400              MOVE  "AT END NOT TAKEN"  TO RE-MARK                IX1034.2
061500              GO TO DELETE-TEST-GF-01-3.                          IX1034.2
061600     GO TO    DELETE-TEST-GF-01.                                  IX1034.2
061700 DELETE-TEST-GF-01-2.                                             IX1034.2
061800     MOVE  CCVS-PGM-ID  TO  XPROGRAM-NAME (1).                    IX1034.2
061900     MOVE  99 TO UPDATE-NUMBER (1).                               IX1034.2
062000     MOVE     FILE-RECORD-INFO (1) TO IX-FS1R1-F-G-240.           IX1034.2
062100     DELETE   IX-FS1.                                             IX1034.2
062200     MOVE     ZERO TO  WRK-CS-09V00-007.                          IX1034.2
062300     ADD      1 TO  WRK-CS-09V00-008                              IX1034.2
062400     GO TO    DELETE-TEST-GF-01.                                  IX1034.2
062500 DELETE-TEST-GF-01-3.                                             IX1034.2
062600     IF       WRK-CS-09V00-006 NOT EQUAL TO 501                   IX1034.2
062700              MOVE WRK-CS-09V00-006 TO COMPUTED-18V0              IX1034.2
062800              MOVE              501 TO CORRECT-18V0               IX1034.2
062900              MOVE "IX-21 4.3.2                       " TO RE-MARKIX1034.2
063000              PERFORM FAIL                                        IX1034.2
063100              ELSE                                                IX1034.2
063200              PERFORM PASS.                                       IX1034.2
063300     PERFORM  PRINT-DETAIL.                                       IX1034.2
063400 DELETE-TEST-GF-02.                                               IX1034.2
063500     MOVE     "DELETE-TEST-GF-02" TO PAR-NAME                     IX1034.2
063600     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
063700*                                                                 IX1034.2
063800*  TEST   2                                                       IX1034.2
063900*                                                                 IX1034.2
064000     IF       WRK-CS-09V00-008 NOT EQUAL TO 125                   IX1034.2
064100              MOVE WRK-CS-09V00-008 TO COMPUTED-18V0              IX1034.2
064200              MOVE 125              TO CORRECT-18V0               IX1034.2
064300              MOVE "DELETED RECORDS" TO RE-MARK                   IX1034.2
064400              PERFORM FAIL                                        IX1034.2
064500              ELSE                                                IX1034.2
064600              PERFORM PASS.                                       IX1034.2
064700     PERFORM  PRINT-DETAIL.                                       IX1034.2
064800 DELETE-TEST-GF-03.                                               IX1034.2
064900     MOVE     "DELETE-TEST-GF-03" TO PAR-NAME                     IX1034.2
065000     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
065100*                                                                 IX1034.2
065200*  TEST   3                                                       IX1034.2
065300*                                                                 IX1034.2
065400       IF WRK-CS-09V00-009 EQUAL TO ZERO                          IX1034.2
065500         PERFORM PASS                                             IX1034.2
065600         ELSE                                                     IX1034.2
065700         PERFORM FAIL                                             IX1034.2
065800         MOVE WRK-CS-09V00-009 TO COMPUTED-18V0                   IX1034.2
065900         MOVE ZERO TO CORRECT-18V0                                IX1034.2
066000         MOVE "INVALID KEY" TO RE-MARK.                           IX1034.2
066100       PERFORM PRINT-DETAIL.                                      IX1034.2
066200     CLOSE    IX-FS1.                                             IX1034.2
066300 DELETE-INIT-GF-04.                                               IX1034.2
066400     MOVE     "DELETE-TEST-GF-04" TO PAR-NAME                     IX1034.2
066500     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
066600*                                                                 IX1034.2
066700*  TEST   4                                                       IX1034.2
066800*                                                                 IX1034.2
066900     MOVE     "DELETE-TEST-GF-04" TO PAR-NAME.                    IX1034.2
067000     MOVE     ZERO TO   WRK-CS-09V00-006                          IX1034.2
067100     MOVE     ZERO TO   WRK-CS-09V00-007                          IX1034.2
067200     MOVE     ZERO TO   WRK-CS-09V00-008                          IX1034.2
067300     MOVE     ZERO TO   WRK-CS-09V00-009                          IX1034.2
067400     MOVE     ZERO TO   WRK-CS-09V00-010                          IX1034.2
067500     MOVE     ZERO TO   WRK-CS-09V00-011                          IX1034.2
067600     MOVE     SPACE  TO  FILE-RECORD-INFO (1).                    IX1034.2
067700     MOVE    ZERO TO WRK-DU-09V00-001.                            IX1034.2
067800     OPEN     INPUT  IX-FS1.                                      IX1034.2
067900 DELETE-TEST-GF-04.                                               IX1034.2
068000     ADD      1 TO WRK-CS-09V00-006.                              IX1034.2
068100     ADD      1 TO WRK-CS-09V00-007.                              IX1034.2
068200     ADD      1 TO WRK-CS-09V00-008.                              IX1034.2
068300     READ     IX-FS1  AT END  GO TO DELETE-TEST-GF-04-3.          IX1034.2
068400     MOVE     IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).           IX1034.2
068500     IF       UPDATE-NUMBER (1) EQUAL TO 99                       IX1034.2
068600              ADD  1 TO WRK-CS-09V00-009.                         IX1034.2
068700     IF       WRK-CS-09V00-007  EQUAL TO 4                        IX1034.2
068800              MOVE 01 TO WRK-CS-09V00-007                         IX1034.2
068900              ADD 1 TO WRK-CS-09V00-008.                          IX1034.2
069000     MOVE    XRECORD-KEY (1)  TO  IX-WRK-KEY.                     IX1034.2
069100     MOVE    WRK-CS-09V00-008  TO WRK-DU-09V00-001.               IX1034.2
069200     IF      IX-WRK-KEY  EQUAL TO IX-FS1-KEY                      IX1034.2
069300              ADD 1 TO  WRK-CS-09V00-010.                         IX1034.2
069400     IF       XRECORD-NUMBER (1) EQUAL TO  WRK-CS-09V00-008       IX1034.2
069500              ADD 1 TO  WRK-CS-09V00-011.                         IX1034.2
069600     IF       WRK-CS-09V00-006 GREATER  501                       IX1034.2
069700              GO TO DELETE-TEST-GF-04-3.                          IX1034.2
069800     GO TO    DELETE-TEST-GF-04.                                  IX1034.2
069900 DELETE-TEST-GF-04-3.                                             IX1034.2
070000     IF       WRK-CS-09V00-006 NOT EQUAL TO 376                   IX1034.2
070100              MOVE "INCORRECT RECORD COUNT"  TO RE-MARK           IX1034.2
070200              MOVE WRK-CS-09V00-006 TO COMPUTED-18V0              IX1034.2
070300              MOVE 376 TO CORRECT-18V0                            IX1034.2
070400              MOVE "IX-21 4.3.2                       " TO RE-MARKIX1034.2
070500              PERFORM  FAIL                                       IX1034.2
070600              ELSE                                                IX1034.2
070700              PERFORM  PASS.                                      IX1034.2
070800     PERFORM  PRINT-DETAIL.                                       IX1034.2
070900 DELETE-TEST-GF-05.                                               IX1034.2
071000     MOVE     "DELETE-TEST-GF-05" TO PAR-NAME                     IX1034.2
071100     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
071200*                                                                 IX1034.2
071300*  TEST   5                                                       IX1034.2
071400*                                                                 IX1034.2
071500     IF       WRK-CS-09V00-009 NOT EQUAL TO ZERO                  IX1034.2
071600             MOVE    ZERO TO CORRECT-18V0                         IX1034.2
071700              MOVE WRK-CS-09V00-009 TO COMPUTED-18V0              IX1034.2
071800              MOVE "DELETED RECORDS" TO RE-MARK                   IX1034.2
071900              MOVE "IX-21 4.3.2                       " TO RE-MARKIX1034.2
072000              PERFORM FAIL                                        IX1034.2
072100              ELSE                                                IX1034.2
072200              PERFORM PASS.                                       IX1034.2
072300     PERFORM  PRINT-DETAIL.                                       IX1034.2
072400 DELETE-TEST-GF-06.                                               IX1034.2
072500     MOVE     "DELETE-TEST-GF-06" TO PAR-NAME                     IX1034.2
072600     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
072700*                                                                 IX1034.2
072800*  TEST   6                                                       IX1034.2
072900*                                                                 IX1034.2
073000     IF       WRK-CS-09V00-010 NOT EQUAL TO 375                   IX1034.2
073100             MOVE    375  TO CORRECT-18V0                         IX1034.2
073200              MOVE "KEY MISMATCH" TO RE-MARK                      IX1034.2
073300              MOVE WRK-CS-09V00-010 TO COMPUTED-18V0              IX1034.2
073400              MOVE "IX-21 4.3.2                       " TO RE-MARKIX1034.2
073500              PERFORM FAIL                                        IX1034.2
073600              ELSE                                                IX1034.2
073700              PERFORM PASS.                                       IX1034.2
073800     PERFORM  PRINT-DETAIL.                                       IX1034.2
073900 DELETE-TEST-GF-07.                                               IX1034.2
074000     MOVE     "DELETE-TEST-GF-07" TO PAR-NAME                     IX1034.2
074100     MOVE    "DELETE   "   TO FEATURE.                            IX1034.2
074200*                                                                 IX1034.2
074300*  TEST   7                                                       IX1034.2
074400*                                                                 IX1034.2
074500     IF      WRK-CS-09V00-011  NOT EQUAL TO  375                  IX1034.2
074600             MOVE   375  TO CORRECT-18V0                          IX1034.2
074700             MOVE   "INCORRECT RECORD FOUND"  TO RE-MARK          IX1034.2
074800             MOVE   WRK-CS-09V00-011  TO COMPUTED-18V0            IX1034.2
074900              MOVE "IX-21 4.3.2                       " TO RE-MARKIX1034.2
075000             PERFORM   FAIL                                       IX1034.2
075100             ELSE                                                 IX1034.2
075200             PERFORM   PASS.                                      IX1034.2
075300     PERFORM   PRINT-DETAIL.                                      IX1034.2
075400     CLOSE    IX-FS1.                                             IX1034.2
075500 CCVS-EXIT SECTION.                                               IX1034.2
075600 CCVS-999999.                                                     IX1034.2
075700     GO TO CLOSE-FILES.                                           IX1034.2
      *END-OF,IX103A                                                                  
