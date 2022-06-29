      *HEADER,COBOL,IX108A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1084.2
000200 PROGRAM-ID.                                                      IX1084.2
000300     IX108A.                                                      IX1084.2
000400****************************************************************  IX1084.2
000500*                                                              *  IX1084.2
000600*    VALIDATION FOR:-                                          *  IX1084.2
000700*                                                              *  IX1084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1084.2
000900*                                                              *  IX1084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1084.2
001100*                                                              *  IX1084.2
001200****************************************************************  IX1084.2
001300*                                                                 IX1084.2
001400*         NEW TESTS:                                              IX1084.2
001500*                                                                 IX1084.2
001600*         READ STATEMENT WITH THE PHRASES:                        IX1084.2
001700*         READ ...  NOT AT END   AND   END-READ                   IX1084.2
001800*         FOR FORMAT 1 AND 2 OF THE READ STATEMENT                IX1084.2
001900*                                                                 IX1084.2
002000*         DELETE STATEMENT WITH THE PHRASES:                      IX1084.2
002100*         DELETE ... NOT INVALID  AND  END-DELETE                 IX1084.2
002200*                                                                 IX1084.2
002300*         REWRITE STATEMENT WITH THE PHRASES:                     IX1084.2
002400*         REWRITE  ...   NOT INVALID KEY   AND   END-REWRITE      IX1084.2
002500*                                                                 IX1084.2
002600*         WRITE STATEMENT WITH THE PHRASES:                       IX1084.2
002700*         WRITE  ...   NOT INVALID KEY   AND  END-WRITE           IX1084.2
002800*                                                                 IX1084.2
002900*                                                                 IX1084.2
003000*                                                                 IX1084.2
003100*                                                                 IX1084.2
003200*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX1084.2
003300*                                                                 IX1084.2
003400*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1084.2
003500*                    CLAUSE FOR DATA FILE IX-FD1                  IX1084.2
003600*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1084.2
003700*                    CLAUSE FOR INDEX FILE IX-FD1                 IX1084.2
003800*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1084.2
003900*                    CLAUSE FOR DATA FILE IX-FD2                  IX1084.2
004000*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1084.2
004100*                    CLAUSE FOR INDEX FILE IX-FD2                 IX1084.2
004200*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX1084.2
004300*             X-62   FOR RAW-DATA                                 IX1084.2
004400*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX1084.2
004500*             X-84   LABEL RECORDS OF PRINT-FILE                  IX1084.2
004600*                                                                 IX1084.2
004700*         NOTE:  X-CARDS 45, 62 AND 84   ARE OPTIONAL             IX1084.2
004800*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX1084.2
004900*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX1084.2
005000*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX1084.2
005100*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX1084.2
005200*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX1084.2
005300*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX1084.2
005400*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX1084.2
005500*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX1084.2
005600*               THEY ARE AS FOLLOWS                               IX1084.2
005700*                                                                 IX1084.2
005800*                  J  SELECTS X-CARD 45                           IX1084.2
005900*                                                                 IX1084.2
006000******************************************************            IX1084.2
006100 ENVIRONMENT DIVISION.                                            IX1084.2
006200 CONFIGURATION SECTION.                                           IX1084.2
006300 SOURCE-COMPUTER.                                                 IX1084.2
006400     XXXXX082.                                                    IX1084.2
006500 OBJECT-COMPUTER.                                                 IX1084.2
006600     XXXXX083.                                                    IX1084.2
006700 INPUT-OUTPUT SECTION.                                            IX1084.2
006800 FILE-CONTROL.                                                    IX1084.2
006900*    SELECT RAW-DATA   ASSIGN TO                                  IX1084.2
007000*    XXXXX062                                                     IX1084.2
007100*           ORGANIZATION IS INDEXED                               IX1084.2
007200*           ACCESS MODE IS RANDOM                                 IX1084.2
007300*           RECORD KEY IS RAW-DATA-KEY.                           IX1084.2
007400     SELECT PRINT-FILE ASSIGN TO                                  IX1084.2
007500     XXXXX055.                                                    IX1084.2
007600     SELECT   IX-FS1 ASSIGN                                       IX1084.2
007700     XXXXX024                                                     IX1084.2
007800*    XXXXX044                                                     IX1084.2
007900             ORGANIZATION IS INDEXED                              IX1084.2
008000        ACCESS SEQUENTIAL                                         IX1084.2
008100        FILE STATUS IS IX-FS1-STATUS                              IX1084.2
008200        RECORD  IX-FS1-KEY.                                       IX1084.2
008300     SELECT   IX-FS2 ASSIGN                                       IX1084.2
008400     XXXXX025                                                     IX1084.2
008500*    XXXXX045                                                     IX1084.2
008600             ORGANIZATION IS INDEXED                              IX1084.2
008700        ACCESS RANDOM                                             IX1084.2
008800        FILE STATUS IS IX-FS2-STATUS                              IX1084.2
008900        RECORD  IX-FS2-KEY.                                       IX1084.2
009000 DATA DIVISION.                                                   IX1084.2
009100 FILE SECTION.                                                    IX1084.2
009200*                                                                 IX1084.2
009300*FD  RAW-DATA.                                                    IX1084.2
009400*                                                                 IX1084.2
009500*01  RAW-DATA-SATZ.                                               IX1084.2
009600*    05  RAW-DATA-KEY        PIC X(6).                            IX1084.2
009700*    05  C-DATE              PIC 9(6).                            IX1084.2
009800*    05  C-TIME              PIC 9(8).                            IX1084.2
009900*    05  C-NO-OF-TESTS       PIC 99.                              IX1084.2
010000*    05  C-OK                PIC 999.                             IX1084.2
010100*    05  C-ALL               PIC 999.                             IX1084.2
010200*    05  C-FAIL              PIC 999.                             IX1084.2
010300*    05  C-DELETED           PIC 999.                             IX1084.2
010400*    05  C-INSPECT           PIC 999.                             IX1084.2
010500*    05  C-NOTE              PIC X(13).                           IX1084.2
010600*    05  C-INDENT            PIC X.                               IX1084.2
010700*    05  C-ABORT             PIC X(8).                            IX1084.2
010800 FD  PRINT-FILE.                                                  IX1084.2
010900 01  PRINT-REC PICTURE X(120).                                    IX1084.2
011000 01  DUMMY-RECORD PICTURE X(120).                                 IX1084.2
011100 FD  IX-FS1                                                       IX1084.2
011200C    LABEL RECORDS ARE STANDARD                                   IX1084.2
011300C    DATA RECORDS IX-FS1R1-F-G-240                                IX1084.2
011400     BLOCK CONTAINS 480.                                          IX1084.2
011500                                                                  IX1084.2
011600 01  IX-FS1R1-F-G-240.                                            IX1084.2
011700     05 IX-FS1-REC-120    PIC X(120).                             IX1084.2
011800     05 IX-FS1-REC-120-240.                                       IX1084.2
011900     10  FILLER PICTURE X(8).                                     IX1084.2
012000     10  IX-FS1-KEY PIC X(29).                                    IX1084.2
012100     10  FILLER PIC X(83).                                        IX1084.2
012200                                                                  IX1084.2
012300 FD  IX-FS2                                                       IX1084.2
012400C    LABEL RECORDS ARE STANDARD                                   IX1084.2
012500C    DATA RECORDS IX-FS2R1-F-G-240                                IX1084.2
012600     BLOCK CONTAINS 480.                                          IX1084.2
012700                                                                  IX1084.2
012800 01  IX-FS2R1-F-G-240.                                            IX1084.2
012900     05 IX-FS2-REC-120    PIC X(120).                             IX1084.2
013000     05 IX-FS2-REC-120-240.                                       IX1084.2
013100     10  FILLER PICTURE X(8).                                     IX1084.2
013200     10  IX-FS2-KEY PIC X(29).                                    IX1084.2
013300     10  FILLER PIC X(83).                                        IX1084.2
013400 WORKING-STORAGE SECTION.                                         IX1084.2
013500 01  SWITCHES-FOR-TEST.                                           IX1084.2
013600     05  SWITCH-NOT-INVALID    PIC 9 VALUE ZERO.                  IX1084.2
013700     05  SWITCH-END-XXX        PIC 9 VALUE ZERO.                  IX1084.2
013800     05  SWITCH-END-X9X        PIC 9 VALUE ZERO.                  IX1084.2
013900     05  SWITCH-IF             PIC 9 VALUE ZERO.                  IX1084.2
014000 01  GRP-0101.                                                    IX1084.2
014100     02 FILLER PIC X(10) VALUE "ABCD921XYZ".                      IX1084.2
014200     02  WRK-DU-09V00-001 PIC 9(9) VALUE ZERO.                    IX1084.2
014300     02 FILLER PIC X(10) VALUE "Z2F()$+-AB".                      IX1084.2
014400 01  GRP-0001.                                                    IX1084.2
014500     05 WRK-CS-09V00-012  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
014600     05 WRK-CS-09V00-013  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
014700     05 WRK-CS-09V00-014  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
014800     05 WRK-CS-09V00-015  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
014900     05 WRK-CS-09V00-016  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
015000     05 WRK-CS-09V00-017  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
015100     05 WRK-CS-09V00-018  PIC S9(9) USAGE COMP  VALUE ZERO.       IX1084.2
015200     05 IX-FS1-STATUS     PIC  XX  VALUE  SPACE.                  IX1084.2
015300     05 IX-FS2-STATUS     PIC  XX  VALUE  SPACE.                  IX1084.2
015400     05 WRK-XN-0002-001   PIC  X(2) VALUE  SPACE.                 IX1084.2
015500     05 WRK-XN-0002-002   PIC  X(2) VALUE  SPACE.                 IX1084.2
015600     05 WRK-XN-0002-003   PIC  X(2) VALUE  SPACE.                 IX1084.2
015700     05 WRK-XN-0002-004   PIC  X(2) VALUE  SPACE.                 IX1084.2
015800     05 WRK-XN-0002-005   PIC  X(2) VALUE  SPACE.                 IX1084.2
015900     05 WRK-XN-0002-006   PIC  X(2) VALUE  SPACE.                 IX1084.2
016000     05 WRK-XN-0002-007   PIC  X(2) VALUE  SPACE.                 IX1084.2
016100     05 WRK-XN-0002-008   PIC  X(2) VALUE  SPACE.                 IX1084.2
016200     05 WRK-XN-0002-009   PIC  X(2) VALUE  SPACE.                 IX1084.2
016300 01  DUMMY-WRK-REC.                                               IX1084.2
016400     02 DUMMY-WRK1       PIC X(120).                              IX1084.2
016500     02 DUMMY-WRK2  REDEFINES  DUMMY-WRK1.                        IX1084.2
016600        03 FILLER   PIC X(5).                                     IX1084.2
016700        03 DUMMY-WRK-INDENT-5  PIC X(115).                        IX1084.2
016800 01  FILE-RECORD-INFORMATION-REC.                                 IX1084.2
016900     03 FILE-RECORD-INFO-SKELETON.                                IX1084.2
017000        05 FILLER                 PICTURE X(48)       VALUE       IX1084.2
017100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1084.2
017200        05 FILLER                 PICTURE X(46)       VALUE       IX1084.2
017300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1084.2
017400        05 FILLER                 PICTURE X(26)       VALUE       IX1084.2
017500             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1084.2
017600        05 FILLER                 PICTURE X(37)       VALUE       IX1084.2
017700             ",RECKEY=                             ".             IX1084.2
017800        05 FILLER                 PICTURE X(38)       VALUE       IX1084.2
017900             ",ALTKEY1=                             ".            IX1084.2
018000        05 FILLER                 PICTURE X(38)       VALUE       IX1084.2
018100             ",ALTKEY2=                             ".            IX1084.2
018200        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1084.2
018300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1084.2
018400        05 FILE-RECORD-INFO-P1-120.                               IX1084.2
018500           07 FILLER              PIC X(5).                       IX1084.2
018600           07 XFILE-NAME           PIC X(6).                      IX1084.2
018700           07 FILLER              PIC X(8).                       IX1084.2
018800           07 XRECORD-NAME         PIC X(6).                      IX1084.2
018900           07 FILLER              PIC X(1).                       IX1084.2
019000           07 REELUNIT-NUMBER     PIC 9(1).                       IX1084.2
019100           07 FILLER              PIC X(7).                       IX1084.2
019200           07 XRECORD-NUMBER       PIC 9(6).                      IX1084.2
019300           07 FILLER              PIC X(6).                       IX1084.2
019400           07 UPDATE-NUMBER       PIC 9(2).                       IX1084.2
019500           07 FILLER              PIC X(5).                       IX1084.2
019600           07 ODO-NUMBER          PIC 9(4).                       IX1084.2
019700           07 FILLER              PIC X(5).                       IX1084.2
019800           07 XPROGRAM-NAME        PIC X(5).                      IX1084.2
019900           07 FILLER              PIC X(7).                       IX1084.2
020000           07 XRECORD-LENGTH       PIC 9(6).                      IX1084.2
020100           07 FILLER              PIC X(7).                       IX1084.2
020200           07 CHARS-OR-RECORDS    PIC X(2).                       IX1084.2
020300           07 FILLER              PIC X(1).                       IX1084.2
020400           07 XBLOCK-SIZE          PIC 9(4).                      IX1084.2
020500           07 FILLER              PIC X(6).                       IX1084.2
020600           07 RECORDS-IN-FILE     PIC 9(6).                       IX1084.2
020700           07 FILLER              PIC X(5).                       IX1084.2
020800           07 XFILE-ORGANIZATION   PIC X(2).                      IX1084.2
020900           07 FILLER              PIC X(6).                       IX1084.2
021000           07 XLABEL-TYPE          PIC X(1).                      IX1084.2
021100        05 FILE-RECORD-INFO-P121-240.                             IX1084.2
021200           07 FILLER              PIC X(8).                       IX1084.2
021300           07 XRECORD-KEY          PIC X(29).                     IX1084.2
021400           07 FILLER              PIC X(9).                       IX1084.2
021500           07 ALTERNATE-KEY1      PIC X(29).                      IX1084.2
021600           07 FILLER              PIC X(9).                       IX1084.2
021700           07 ALTERNATE-KEY2      PIC X(29).                      IX1084.2
021800           07 FILLER              PIC X(7).                       IX1084.2
021900 01  TEST-RESULTS.                                                IX1084.2
022000     02 FILLER                   PIC X      VALUE SPACE.          IX1084.2
022100     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1084.2
022200     02 FILLER                   PIC X      VALUE SPACE.          IX1084.2
022300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1084.2
022400     02 FILLER                   PIC X      VALUE SPACE.          IX1084.2
022500     02  PAR-NAME.                                                IX1084.2
022600       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1084.2
022700       03  PARDOT-X              PIC X      VALUE SPACE.          IX1084.2
022800       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1084.2
022900     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1084.2
023000     02 RE-MARK                  PIC X(61).                       IX1084.2
023100 01  TEST-COMPUTED.                                               IX1084.2
023200     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1084.2
023300     02 FILLER                   PIC X(17)  VALUE                 IX1084.2
023400            "       COMPUTED=".                                   IX1084.2
023500     02 COMPUTED-X.                                               IX1084.2
023600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1084.2
023700     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1084.2
023800                                 PIC -9(9).9(9).                  IX1084.2
023900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1084.2
024000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1084.2
024100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1084.2
024200     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1084.2
024300         04 COMPUTED-18V0                    PIC -9(18).          IX1084.2
024400         04 FILLER                           PIC X.               IX1084.2
024500     03 FILLER PIC X(50) VALUE SPACE.                             IX1084.2
024600 01  TEST-CORRECT.                                                IX1084.2
024700     02 FILLER PIC X(30) VALUE SPACE.                             IX1084.2
024800     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1084.2
024900     02 CORRECT-X.                                                IX1084.2
025000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1084.2
025100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1084.2
025200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1084.2
025300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1084.2
025400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1084.2
025500     03      CR-18V0 REDEFINES CORRECT-A.                         IX1084.2
025600         04 CORRECT-18V0                     PIC -9(18).          IX1084.2
025700         04 FILLER                           PIC X.               IX1084.2
025800     03 FILLER PIC X(2) VALUE SPACE.                              IX1084.2
025900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1084.2
026000 01  CCVS-C-1.                                                    IX1084.2
026100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1084.2
026200-    "SS  PARAGRAPH-NAME                                          IX1084.2
026300-    "       REMARKS".                                            IX1084.2
026400     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1084.2
026500 01  CCVS-C-2.                                                    IX1084.2
026600     02 FILLER                     PIC X        VALUE SPACE.      IX1084.2
026700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1084.2
026800     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1084.2
026900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1084.2
027000     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1084.2
027100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1084.2
027200 01  REC-CT                        PIC 99       VALUE ZERO.       IX1084.2
027300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1084.2
027400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1084.2
027500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1084.2
027600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1084.2
027700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1084.2
027800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1084.2
027900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1084.2
028000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1084.2
028100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1084.2
028200 01  CCVS-H-1.                                                    IX1084.2
028300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1084.2
028400     02  FILLER                    PIC X(42)    VALUE             IX1084.2
028500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1084.2
028600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1084.2
028700 01  CCVS-H-2A.                                                   IX1084.2
028800   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1084.2
028900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1084.2
029000   02  FILLER                        PIC XXXX   VALUE             IX1084.2
029100     "4.2 ".                                                      IX1084.2
029200   02  FILLER                        PIC X(28)  VALUE             IX1084.2
029300            " COPY - NOT FOR DISTRIBUTION".                       IX1084.2
029400   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1084.2
029500                                                                  IX1084.2
029600 01  CCVS-H-2B.                                                   IX1084.2
029700   02  FILLER                        PIC X(15)  VALUE             IX1084.2
029800            "TEST RESULT OF ".                                    IX1084.2
029900   02  TEST-ID                       PIC X(9).                    IX1084.2
030000   02  FILLER                        PIC X(4)   VALUE             IX1084.2
030100            " IN ".                                               IX1084.2
030200   02  FILLER                        PIC X(12)  VALUE             IX1084.2
030300     " HIGH       ".                                              IX1084.2
030400   02  FILLER                        PIC X(22)  VALUE             IX1084.2
030500            " LEVEL VALIDATION FOR ".                             IX1084.2
030600   02  FILLER                        PIC X(58)  VALUE             IX1084.2
030700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1084.2
030800 01  CCVS-H-3.                                                    IX1084.2
030900     02  FILLER                      PIC X(34)  VALUE             IX1084.2
031000            " FOR OFFICIAL USE ONLY    ".                         IX1084.2
031100     02  FILLER                      PIC X(58)  VALUE             IX1084.2
031200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1084.2
031300     02  FILLER                      PIC X(28)  VALUE             IX1084.2
031400            "  COPYRIGHT   1985 ".                                IX1084.2
031500 01  CCVS-E-1.                                                    IX1084.2
031600     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1084.2
031700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1084.2
031800     02 ID-AGAIN                     PIC X(9).                    IX1084.2
031900     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1084.2
032000 01  CCVS-E-2.                                                    IX1084.2
032100     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1084.2
032200     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1084.2
032300     02 CCVS-E-2-2.                                               IX1084.2
032400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1084.2
032500         03 FILLER                   PIC X      VALUE SPACE.      IX1084.2
032600         03 ENDER-DESC               PIC X(44)  VALUE             IX1084.2
032700            "ERRORS ENCOUNTERED".                                 IX1084.2
032800 01  CCVS-E-3.                                                    IX1084.2
032900     02  FILLER                      PIC X(22)  VALUE             IX1084.2
033000            " FOR OFFICIAL USE ONLY".                             IX1084.2
033100     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1084.2
033200     02  FILLER                      PIC X(58)  VALUE             IX1084.2
033300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1084.2
033400     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1084.2
033500     02 FILLER                       PIC X(15)  VALUE             IX1084.2
033600             " COPYRIGHT 1985".                                   IX1084.2
033700 01  CCVS-E-4.                                                    IX1084.2
033800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1084.2
033900     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1084.2
034000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1084.2
034100     02 FILLER                       PIC X(40)  VALUE             IX1084.2
034200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1084.2
034300 01  XXINFO.                                                      IX1084.2
034400     02 FILLER                       PIC X(19)  VALUE             IX1084.2
034500            "*** INFORMATION ***".                                IX1084.2
034600     02 INFO-TEXT.                                                IX1084.2
034700       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1084.2
034800       04 XXCOMPUTED                 PIC X(20).                   IX1084.2
034900       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1084.2
035000       04 XXCORRECT                  PIC X(20).                   IX1084.2
035100     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1084.2
035200 01  HYPHEN-LINE.                                                 IX1084.2
035300     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1084.2
035400     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1084.2
035500-    "*****************************************".                 IX1084.2
035600     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1084.2
035700-    "******************************".                            IX1084.2
035800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1084.2
035900     "IX108A".                                                    IX1084.2
036000 PROCEDURE DIVISION.                                              IX1084.2
036100 DECLARATIVES.                                                    IX1084.2
036200 IX-FS2-01 SECTION.                                               IX1084.2
036300     USE    AFTER STANDARD ERROR PROCEDURE ON IX-FS2.             IX1084.2
036400 IX-FS2-01-01.                                                    IX1084.2
036500     ADD      1 TO  WRK-CS-09V00-013.                             IX1084.2
036600     GO TO    IX-FS2-01-03                                        IX1084.2
036700              IX-FS2-01-05                                        IX1084.2
036800              DEPENDING ON  WRK-CS-09V00-012.                     IX1084.2
036900     GO TO    IX-FS2-01-EXIT.                                     IX1084.2
037000 IX-FS2-01-03.                                                    IX1084.2
037100*ENTRY FROM SEGMENT INX-TEST-001.                                 IX1084.2
037200*    SHOULD NOT ENTER HERE UNLESS SPACE ALLOCATION TOO SMALL.     IX1084.2
037300     ADD     1 TO  WRK-CS-09V00-014.                              IX1084.2
037400 IX-FS2-01-05.                                                    IX1084.2
037500     ADD      1 TO WRK-CS-09V00-017.                              IX1084.2
037600     IF       XRECORD-NUMBER (2) EQUAL TO 500                     IX1084.2
037700              MOVE   IX-FS2-STATUS TO WRK-XN-0002-002             IX1084.2
037800              MOVE  "10" TO WRK-XN-0002-003.                      IX1084.2
037900 IX-FS2-01-EXIT.                                                  IX1084.2
038000     EXIT.                                                        IX1084.2
038100 END DECLARATIVES.                                                IX1084.2
038200 CCVS1 SECTION.                                                   IX1084.2
038300 OPEN-FILES.                                                      IX1084.2
038400*    OPEN I-O RAW-DATA.                                           IX1084.2
038500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1084.2
038600*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1084.2
038700*    MOVE "ABORTED " TO C-ABORT.                                  IX1084.2
038800*    ADD 1 TO C-NO-OF-TESTS.                                      IX1084.2
038900*    ACCEPT C-DATE  FROM DATE.                                    IX1084.2
039000*    ACCEPT C-TIME  FROM TIME.                                    IX1084.2
039100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1084.2
039200*END-E-1.                                                         IX1084.2
039300*    CLOSE RAW-DATA.                                              IX1084.2
039400     OPEN    OUTPUT PRINT-FILE.                                   IX1084.2
039500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1084.2
039600     MOVE    SPACE TO TEST-RESULTS.                               IX1084.2
039700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1084.2
039800     MOVE    ZERO TO REC-SKL-SUB.                                 IX1084.2
039900     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1084.2
040000 CCVS-INIT-FILE.                                                  IX1084.2
040100     ADD     1 TO REC-SKL-SUB.                                    IX1084.2
040200     MOVE    FILE-RECORD-INFO-SKELETON                            IX1084.2
040300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1084.2
040400 CCVS-INIT-EXIT.                                                  IX1084.2
040500     GO TO CCVS1-EXIT.                                            IX1084.2
040600 CLOSE-FILES.                                                     IX1084.2
040700*    OPEN I-O RAW-DATA.                                           IX1084.2
040800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1084.2
040900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1084.2
041000*    MOVE "OK.     " TO C-ABORT.                                  IX1084.2
041100*    MOVE PASS-COUNTER TO C-OK.                                   IX1084.2
041200*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1084.2
041300*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1084.2
041400*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1084.2
041500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1084.2
041600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1084.2
041700*END-E-2.                                                         IX1084.2
041800*    CLOSE RAW-DATA.                                              IX1084.2
041900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1084.2
042000 TERMINATE-CCVS.                                                  IX1084.2
042100*    EXIT PROGRAM.                                                IX1084.2
042200*TERMINATE-CALL.                                                  IX1084.2
042300     STOP     RUN.                                                IX1084.2
042400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1084.2
042500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1084.2
042600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1084.2
042700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1084.2
042800     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1084.2
042900 PRINT-DETAIL.                                                    IX1084.2
043000     IF REC-CT NOT EQUAL TO ZERO                                  IX1084.2
043100             MOVE "." TO PARDOT-X                                 IX1084.2
043200             MOVE REC-CT TO DOTVALUE.                             IX1084.2
043300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1084.2
043400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1084.2
043500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1084.2
043600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1084.2
043700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1084.2
043800     MOVE SPACE TO CORRECT-X.                                     IX1084.2
043900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1084.2
044000     MOVE     SPACE TO RE-MARK.                                   IX1084.2
044100 HEAD-ROUTINE.                                                    IX1084.2
044200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1084.2
044300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1084.2
044400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1084.2
044500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1084.2
044600 COLUMN-NAMES-ROUTINE.                                            IX1084.2
044700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1084.2
044800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1084.2
044900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1084.2
045000 END-ROUTINE.                                                     IX1084.2
045100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1084.2
045200 END-RTN-EXIT.                                                    IX1084.2
045300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1084.2
045400 END-ROUTINE-1.                                                   IX1084.2
045500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1084.2
045600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1084.2
045700      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1084.2
045800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1084.2
045900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1084.2
046000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1084.2
046100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1084.2
046200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1084.2
046300  END-ROUTINE-12.                                                 IX1084.2
046400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1084.2
046500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1084.2
046600         MOVE "NO " TO ERROR-TOTAL                                IX1084.2
046700         ELSE                                                     IX1084.2
046800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1084.2
046900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1084.2
047000     PERFORM WRITE-LINE.                                          IX1084.2
047100 END-ROUTINE-13.                                                  IX1084.2
047200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1084.2
047300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1084.2
047400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1084.2
047500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1084.2
047600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1084.2
047700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1084.2
047800          MOVE "NO " TO ERROR-TOTAL                               IX1084.2
047900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1084.2
048000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1084.2
048100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1084.2
048200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1084.2
048300 WRITE-LINE.                                                      IX1084.2
048400     ADD 1 TO RECORD-COUNT.                                       IX1084.2
048500*    IF RECORD-COUNT GREATER 42                                   IX1084.2
048600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1084.2
048700*        MOVE SPACE TO DUMMY-RECORD                               IX1084.2
048800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1084.2
048900*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1084.2
049000*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1084.2
049100*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1084.2
049200*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1084.2
049300*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1084.2
049400*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1084.2
049500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1084.2
049600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1084.2
049700*        MOVE ZERO TO RECORD-COUNT.                               IX1084.2
049800     PERFORM WRT-LN.                                              IX1084.2
049900 WRT-LN.                                                          IX1084.2
050000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1084.2
050100     MOVE SPACE TO DUMMY-RECORD.                                  IX1084.2
050200 BLANK-LINE-PRINT.                                                IX1084.2
050300     PERFORM WRT-LN.                                              IX1084.2
050400 FAIL-ROUTINE.                                                    IX1084.2
050500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1084.2
050600            GO TO   FAIL-ROUTINE-WRITE.                           IX1084.2
050700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1084.2
050800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1084.2
050900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1084.2
051000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1084.2
051100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1084.2
051200     GO TO  FAIL-ROUTINE-EX.                                      IX1084.2
051300 FAIL-ROUTINE-WRITE.                                              IX1084.2
051400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1084.2
051500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1084.2
051600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1084.2
051700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1084.2
051800 FAIL-ROUTINE-EX. EXIT.                                           IX1084.2
051900 BAIL-OUT.                                                        IX1084.2
052000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1084.2
052100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1084.2
052200 BAIL-OUT-WRITE.                                                  IX1084.2
052300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1084.2
052400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1084.2
052500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1084.2
052600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1084.2
052700 BAIL-OUT-EX. EXIT.                                               IX1084.2
052800 CCVS1-EXIT.                                                      IX1084.2
052900     EXIT.                                                        IX1084.2
053000 SECT-IX-04-001 SECTION.                                          IX1084.2
053100******************************************************************IX1084.2
053200*                                                                *IX1084.2
053300*   TEST  1   CREATE INDEXED FILE IX-FS2                         *IX1084.2
053400*                                                                *IX1084.2
053500******************************************************************IX1084.2
053600 WRITE-INIT-GF-01.                                                IX1084.2
053700     MOVE     "WRITE NOT INVALID END-" TO  FEATURE.               IX1084.2
053800     MOVE     "IX-FS2"        TO  XFILE-NAME (2).                 IX1084.2
053900     MOVE     "R1-F-G"        TO  XRECORD-NAME (2).               IX1084.2
054000     MOVE     CCVS-PGM-ID     TO  XPROGRAM-NAME (2).              IX1084.2
054100     MOVE     000240          TO  XRECORD-LENGTH (2).             IX1084.2
054200     MOVE     "RC"            TO  CHARS-OR-RECORDS (2).           IX1084.2
054300     MOVE     0001            TO  XBLOCK-SIZE       (2).          IX1084.2
054400     MOVE     000500          TO  RECORDS-IN-FILE  (2).           IX1084.2
054500     MOVE    "IX"                TO XFILE-ORGANIZATION (2).       IX1084.2
054600     MOVE    "WRITE-TEST-GF-01" TO PAR-NAME.                      IX1084.2
054700     MOVE     "S"             TO  XLABEL-TYPE        (2).         IX1084.2
054800     MOVE     000001          TO  XRECORD-NUMBER     (2).         IX1084.2
054900     MOVE     1         TO  WRK-CS-09V00-012.                     IX1084.2
055000     MOVE     ZERO      TO  WRK-CS-09V00-013 WRK-CS-09V00-014     IX1084.2
055100                            WRK-CS-09V00-015 WRK-CS-09V00-016     IX1084.2
055200                            WRK-CS-09V00-017 WRK-CS-09V00-018.    IX1084.2
055300     MOVE     SPACE     TO  IX-FS1-STATUS.                        IX1084.2
055400     MOVE     SPACE     TO  IX-FS2-STATUS.                        IX1084.2
055500     MOVE    ZERO TO WRK-DU-09V00-001.                            IX1084.2
055600     OPEN     OUTPUT    IX-FS1.                                   IX1084.2
055700     OPEN     OUTPUT    IX-FS2.                                   IX1084.2
055800     MOVE GRP-0101 TO IX-FS2-KEY.                                 IX1084.2
055900     MOVE     IX-FS2-STATUS TO WRK-XN-0002-001.                   IX1084.2
056000 WRITE-TEST-GF-01.                                                IX1084.2
056100     MOVE ZERO TO SWITCH-NOT-INVALID.                             IX1084.2
056200     MOVE ZERO TO SWITCH-END-XXX.                                 IX1084.2
056300     MOVE ZERO TO SWITCH-END-X9X.                                 IX1084.2
056400     MOVE    "99"  TO IX-FS2-STATUS.                              IX1084.2
056500     MOVE     XRECORD-NUMBER (2) TO WRK-DU-09V00-001.             IX1084.2
056600     MOVE     GRP-0101 TO XRECORD-KEY (2).                        IX1084.2
056700     MOVE     "IX-FS2"        TO  XFILE-NAME (2).                 IX1084.2
056800     MOVE     FILE-RECORD-INFO (2) TO IX-FS2R1-F-G-240.           IX1084.2
056900 WRITE-TEST-GF-01-1.                                              IX1084.2
057000     WRITE   IX-FS2R1-F-G-240                                     IX1084.2
057100       NOT INVALID                                                IX1084.2
057200         MOVE 1 TO SWITCH-NOT-INVALID.                            IX1084.2
057300     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
057400         MOVE 0 TO SWITCH-NOT-INVALID                             IX1084.2
057500      ELSE                                                        IX1084.2
057600         MOVE "WRITE NOT INVALID" TO FEATURE                      IX1084.2
057700         PERFORM FAIL                                             IX1084.2
057800         MOVE "FILE IX-FS2 CANNOT BE CREATED CORRECTLY; IX-41"    IX1084.2
057900              TO RE-MARK                                          IX1084.2
058000         GO TO CCVS-EXIT.                                         IX1084.2
058100 WRITE-TEST-GF-01-2.                                              IX1084.2
058200     MOVE ZERO TO SWITCH-NOT-INVALID.                             IX1084.2
058300     MOVE ZERO TO SWITCH-END-XXX.                                 IX1084.2
058400     MOVE ZERO TO SWITCH-END-X9X.                                 IX1084.2
058500     MOVE     "IX-FS1"        TO  XFILE-NAME (2).                 IX1084.2
058600     WRITE   IX-FS1R1-F-G-240 FROM FILE-RECORD-INFO (2)           IX1084.2
058700          INVALID KEY GO TO WRITE-TEST-GF-01-2-1                  IX1084.2
058800          NOT INVALID                                             IX1084.2
058900         MOVE 1 TO SWITCH-NOT-INVALID                             IX1084.2
059000      END-WRITE.                                                  IX1084.2
059100 WRITE-TEST-GF-01-2-1.                                            IX1084.2
059200         MOVE 1 TO SWITCH-END-XXX.                                IX1084.2
059300     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
059400         MOVE 0 TO SWITCH-NOT-INVALID                             IX1084.2
059500      ELSE                                                        IX1084.2
059600         MOVE "WRITE NOT INVALID" TO FEATURE                      IX1084.2
059700         PERFORM FAIL                                             IX1084.2
059800         MOVE "FILE IX-FS1 CANNOT BE CREATED CORRECTLY; IX-41"    IX1084.2
059900              TO RE-MARK                                          IX1084.2
060000         GO TO CCVS-EXIT.                                         IX1084.2
060100     IF SWITCH-END-XXX     = 1                                    IX1084.2
060200         MOVE 0 TO SWITCH-END-XXX                                 IX1084.2
060300      ELSE                                                        IX1084.2
060400         MOVE "WRITE .. END-WRITE" TO FEATURE                     IX1084.2
060500         MOVE "FILE IX-FS1 CANNOT BE CREATED CORRECTLY; IX-41"    IX1084.2
060600              TO RE-MARK                                          IX1084.2
060700         PERFORM FAIL                                             IX1084.2
060800         GO TO CCVS-EXIT.                                         IX1084.2
060900     IF        IX-FS2-STATUS NOT EQUAL TO "00"                    IX1084.2
061000         ADD  1  TO WRK-CS-09V00-016.                             IX1084.2
061100     IF        IX-FS1-STATUS NOT EQUAL TO "00"                    IX1084.2
061200         ADD  1  TO WRK-CS-09V00-016.                             IX1084.2
061300     IF       XRECORD-NUMBER (2) EQUAL TO 100                     IX1084.2
061400              GO TO  WRITE-TEST-GF-01-3.                          IX1084.2
061500     ADD      01 TO  XRECORD-NUMBER (2).                          IX1084.2
061600     GO TO    WRITE-TEST-GF-01.                                   IX1084.2
061700 WRITE-TEST-GF-01-3.                                              IX1084.2
061800     IF       WRK-CS-09V00-014 NOT EQUAL TO ZERO                  IX1084.2
061900              MOVE "EXCEPTIONS/ERRORS" TO RE-MARK                 IX1084.2
062000              MOVE  ZERO TO CORRECT-18V0                          IX1084.2
062100              MOVE WRK-CS-09V00-014 TO COMPUTED-18V0              IX1084.2
062200              MOVE "IX-41 4.9.2                       " TO RE-MARKIX1084.2
062300              PERFORM   FAIL                                      IX1084.2
062400              ELSE                                                IX1084.2
062500              PERFORM   PASS.                                     IX1084.2
062600     PERFORM  PRINT-DETAIL.                                       IX1084.2
062700 WRITE-TEST-GF-02.                                                IX1084.2
062800     MOVE     "CREATE IX-FS2" TO  FEATURE                         IX1084.2
062900     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX1084.2
063000     IF       XRECORD-NUMBER (2) NOT EQUAL TO 100                 IX1084.2
063100              MOVE "INCORRECT COUNT" TO RE-MARK                   IX1084.2
063200              MOVE  500 TO CORRECT-18V0                           IX1084.2
063300             MOVE   XRECORD-NUMBER (2)  TO COMPUTED-18V0          IX1084.2
063400              MOVE "IX-41 4.9.2                       " TO RE-MARKIX1084.2
063500              PERFORM  FAIL                                       IX1084.2
063600              ELSE                                                IX1084.2
063700              PERFORM PASS.                                       IX1084.2
063800     PERFORM  PRINT-DETAIL.                                       IX1084.2
063900                                                                  IX1084.2
064000     CLOSE IX-FS1 IX-FS2.                                         IX1084.2
064100                                                                  IX1084.2
064200******************************************************************IX1084.2
064300*                                                                *IX1084.2
064400*   TESTS:      R E A D         NOT INVALID    END-READ          *IX1084.2
064500*                                                                *IX1084.2
064600******************************************************************IX1084.2
064700 READ-INIT-F1-01.                                                 IX1084.2
064800     OPEN INPUT IX-FS1.                                           IX1084.2
064900     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          IX1084.2
065000     MOVE "READ NOT AT END  " TO FEATURE.                         IX1084.2
065100     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
065200     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
065300 READ-TEST-F1-01-0.                                               IX1084.2
065400     READ  IX-FS1 AT END                                          IX1084.2
065500              GO TO READ-FAIL-F1-01                               IX1084.2
065600          NOT AT END                                              IX1084.2
065700           MOVE 1 TO SWITCH-NOT-INVALID.                          IX1084.2
065800 READ-TEST-F1-01.                                                 IX1084.2
065900     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
066000          GO TO READ-PASS-F1-01.                                  IX1084.2
066100 READ-FAIL-F1-01.                                                 IX1084.2
066200     MOVE "IX-28 4.5.4 (11) C                        " TO RE-MARK.IX1084.2
066300     PERFORM FAIL.                                                IX1084.2
066400     GO TO READ-WRITE-F1-01.                                      IX1084.2
066500 READ-PASS-F1-01.                                                 IX1084.2
066600     PERFORM PASS.                                                IX1084.2
066700 READ-WRITE-F1-01.                                                IX1084.2
066800     PERFORM PRINT-DETAIL.                                        IX1084.2
066900                                                                  IX1084.2
067000******************************************************************IX1084.2
067100*   TEST  READ .. NOT AT END   ... END-READ.                     *IX1084.2
067200*                                                                *IX1084.2
067300*         IX-28, 4.5.4                                           *IX1084.2
067400******************************************************************IX1084.2
067500 READ-INIT-F1-02.                                                 IX1084.2
067600     MOVE "READ-TEST-F1-02-1" TO PAR-NAME.                        IX1084.2
067700     MOVE "READ . NOT INV. . END-" TO FEATURE.                    IX1084.2
067800     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
067900     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
068000 READ-TEST-F1-02.                                                 IX1084.2
068100     READ  IX-FS1 AT END                                          IX1084.2
068200              GO TO READ-FAIL-F1-02-1                             IX1084.2
068300          NOT END                                                 IX1084.2
068400           MOVE 1 TO SWITCH-NOT-INVALID                           IX1084.2
068500        END-READ.                                                 IX1084.2
068600     MOVE 1 TO SWITCH-END-XXX.                                    IX1084.2
068700 READ-TEST-F1-02-1.                                               IX1084.2
068800     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
068900          GO TO READ-PASS-F1-02-1.                                IX1084.2
069000 READ-FAIL-F1-02-1.                                               IX1084.2
069100     MOVE "IX-28 4.5.4 (11) C                        " TO RE-MARK.IX1084.2
069200     PERFORM FAIL.                                                IX1084.2
069300     GO TO READ-WRITE-F1-02-1.                                    IX1084.2
069400 READ-PASS-F1-02-1.                                               IX1084.2
069500     PERFORM PASS.                                                IX1084.2
069600 READ-WRITE-F1-02-1.                                              IX1084.2
069700     PERFORM PRINT-DETAIL.                                        IX1084.2
069800                                                                  IX1084.2
069900 READ-TEST-F1-02-2.                                               IX1084.2
070000     MOVE "READ-TEST-F1-02-2" TO PAR-NAME.                        IX1084.2
070100     MOVE "END-READ.        " TO FEATURE.                         IX1084.2
070200     IF SWITCH-END-XXX     = 1                                    IX1084.2
070300          GO TO READ-PASS-F1-02-2.                                IX1084.2
070400 READ-FAIL-F1-02-2.                                               IX1084.2
070500     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
070600     PERFORM FAIL.                                                IX1084.2
070700     GO TO READ-WRITE-F1-02-2.                                    IX1084.2
070800 READ-PASS-F1-02-2.                                               IX1084.2
070900     PERFORM PASS.                                                IX1084.2
071000 READ-WRITE-F1-02-2.                                              IX1084.2
071100     PERFORM PRINT-DETAIL.                                        IX1084.2
071200                                                                  IX1084.2
071300******************************************************************IX1084.2
071400*   TEST: IF READ .. NOT AT END   ... END-READ ...      .        *IX1084.2
071500*                                                                *IX1084.2
071600*         IX-28, 4.5.4                                           *IX1084.2
071700******************************************************************IX1084.2
071800 READ-INIT-F1-03.                                                 IX1084.2
071900     MOVE "READ-TEST-F1-03-1" TO PAR-NAME.                        IX1084.2
072000     MOVE "IF . READ . NOT INV. . END-" TO FEATURE.               IX1084.2
072100     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
072200     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
072300     MOVE    1      TO  SWITCH-IF.                                IX1084.2
072400 READ-TEST-F1-03.                                                 IX1084.2
072500     IF SWITCH-IF = 1                                             IX1084.2
072600        READ  IX-FS1 AT END                                       IX1084.2
072700              GO TO READ-FAIL-F1-03-1                             IX1084.2
072800          NOT END                                                 IX1084.2
072900             MOVE 1 TO SWITCH-NOT-INVALID                         IX1084.2
073000        END-READ                                                  IX1084.2
073100             MOVE 1 TO SWITCH-END-XXX.                            IX1084.2
073200     MOVE 9 TO SWITCH-END-X9X.                                    IX1084.2
073300 READ-TEST-F1-03-1.                                               IX1084.2
073400     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
073500          GO TO READ-PASS-F1-03-1.                                IX1084.2
073600 READ-FAIL-F1-03-1.                                               IX1084.2
073700     MOVE "IX-28 4.5.4 (11) C                        " TO RE-MARK.IX1084.2
073800     PERFORM FAIL.                                                IX1084.2
073900     GO TO READ-WRITE-F1-03-1.                                    IX1084.2
074000 READ-PASS-F1-03-1.                                               IX1084.2
074100     PERFORM PASS.                                                IX1084.2
074200 READ-WRITE-F1-03-1.                                              IX1084.2
074300     PERFORM PRINT-DETAIL.                                        IX1084.2
074400                                                                  IX1084.2
074500 READ-TEST-F1-03-2.                                               IX1084.2
074600     MOVE "READ-TEST-F1-03-2" TO PAR-NAME.                        IX1084.2
074700     IF SWITCH-END-XXX     = 1                                    IX1084.2
074800          GO TO READ-PASS-F1-03-2.                                IX1084.2
074900 READ-FAIL-F1-03-2.                                               IX1084.2
075000     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
075100     PERFORM FAIL.                                                IX1084.2
075200     GO TO READ-WRITE-F1-03-2.                                    IX1084.2
075300 READ-PASS-F1-03-2.                                               IX1084.2
075400     PERFORM PASS.                                                IX1084.2
075500 READ-WRITE-F1-03-2.                                              IX1084.2
075600     PERFORM PRINT-DETAIL.                                        IX1084.2
075700                                                                  IX1084.2
075800 READ-TEST-F1-03-3.                                               IX1084.2
075900     MOVE "READ-TEST-F1-03-3" TO PAR-NAME.                        IX1084.2
076000     IF SWITCH-END-X9X     = 9                                    IX1084.2
076100          GO TO READ-PASS-F1-03-3.                                IX1084.2
076200 READ-FAIL-F1-03-3.                                               IX1084.2
076300     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
076400     PERFORM FAIL.                                                IX1084.2
076500     GO TO READ-WRITE-F1-03-3.                                    IX1084.2
076600 READ-PASS-F1-03-3.                                               IX1084.2
076700     PERFORM PASS.                                                IX1084.2
076800 READ-WRITE-F1-03-3.                                              IX1084.2
076900     PERFORM PRINT-DETAIL.                                        IX1084.2
077000                                                                  IX1084.2
077100     CLOSE IX-FS1.                                                IX1084.2
077200                                                                  IX1084.2
077300******************************************************************IX1084.2
077400*   TEST  READ    NOT INVALID                                    *IX1084.2
077500*                                                                *IX1084.2
077600*         IX-28, 4.5.4                                           *IX1084.2
077700******************************************************************IX1084.2
077800 READ-INIT-F2-01.                                                 IX1084.2
077900     MOVE     2     TO  WRK-CS-09V00-012.                         IX1084.2
078000     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX1084.2
078100     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX1084.2
078200     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX1084.2
078300     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX1084.2
078400     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX1084.2
078500     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX1084.2
078600     MOVE    SPACE TO IX-FS2-STATUS.                              IX1084.2
078700     OPEN     I-O  IX-FS2.                                        IX1084.2
078800     MOVE     SPACE     TO  WRK-XN-0002-002                       IX1084.2
078900     MOVE     SPACE     TO  WRK-XN-0002-003                       IX1084.2
079000     MOVE     SPACE     TO  WRK-XN-0002-004                       IX1084.2
079100     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1084.2
079200     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1084.2
079300     MOVE     SPACE TO IX-FS2-STATUS.                             IX1084.2
079400     MOVE "READ-TEST-F2-01" TO PAR-NAME.                          IX1084.2
079500     MOVE "READ NOT INVALID." TO FEATURE.                         IX1084.2
079600     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
079700     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
079800 READ-TEST-F2-01-0.                                               IX1084.2
079900     MOVE 1 TO WRK-DU-09V00-001.                                  IX1084.2
080000     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
080100     READ  IX-FS2 INVALID                                         IX1084.2
080200              GO TO READ-FAIL-F2-01                               IX1084.2
080300          NOT INVALID                                             IX1084.2
080400           MOVE 1 TO SWITCH-NOT-INVALID.                          IX1084.2
080500 READ-TEST-F2-01.                                                 IX1084.2
080600     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
080700          GO TO READ-PASS-F2-01.                                  IX1084.2
080800 READ-FAIL-F2-01.                                                 IX1084.2
080900     MOVE "IX-28 4.5.4 (11) C                        " TO RE-MARK.IX1084.2
081000     PERFORM FAIL.                                                IX1084.2
081100     GO TO READ-WRITE-F2-01.                                      IX1084.2
081200 READ-PASS-F2-01.                                                 IX1084.2
081300     PERFORM PASS.                                                IX1084.2
081400 READ-WRITE-F2-01.                                                IX1084.2
081500     PERFORM PRINT-DETAIL.                                        IX1084.2
081600                                                                  IX1084.2
081700******************************************************************IX1084.2
081800*   TEST  READ .. NOT INVALID  ... END-READ.                     *IX1084.2
081900*                                                                *IX1084.2
082000*         IX-28, 4.5.4                                           *IX1084.2
082100******************************************************************IX1084.2
082200 READ-INIT-F2-02.                                                 IX1084.2
082300     MOVE "READ-TEST-F2-02-1" TO PAR-NAME.                        IX1084.2
082400     MOVE "READ . NOT INV. . END-" TO FEATURE.                    IX1084.2
082500     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
082600     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
082700 READ-TEST-F2-02.                                                 IX1084.2
082800     MOVE 2 TO WRK-DU-09V00-001.                                  IX1084.2
082900     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
083000     READ  IX-FS2 INVALID KEY                                     IX1084.2
083100              GO TO READ-FAIL-F2-02-1                             IX1084.2
083200          NOT INVALID KEY                                         IX1084.2
083300           MOVE 1 TO SWITCH-NOT-INVALID                           IX1084.2
083400        END-READ.                                                 IX1084.2
083500     MOVE 1 TO SWITCH-END-XXX.                                    IX1084.2
083600 READ-TEST-F2-02-1.                                               IX1084.2
083700     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
083800          GO TO READ-PASS-F2-02-1.                                IX1084.2
083900 READ-FAIL-F2-02-1.                                               IX1084.2
084000     MOVE "IX-28 4.5.4 (11) C                        " TO RE-MARK.IX1084.2
084100     PERFORM FAIL.                                                IX1084.2
084200     GO TO READ-WRITE-F2-02-1.                                    IX1084.2
084300 READ-PASS-F2-02-1.                                               IX1084.2
084400     PERFORM PASS.                                                IX1084.2
084500 READ-WRITE-F2-02-1.                                              IX1084.2
084600     PERFORM PRINT-DETAIL.                                        IX1084.2
084700                                                                  IX1084.2
084800 READ-TEST-F2-02-2.                                               IX1084.2
084900     MOVE "READ-TEST-F2-02-2" TO PAR-NAME.                        IX1084.2
085000     MOVE "END-READ.        " TO FEATURE.                         IX1084.2
085100     IF SWITCH-END-XXX     = 1                                    IX1084.2
085200          GO TO READ-PASS-F2-02-2.                                IX1084.2
085300 READ-FAIL-F2-02-2.                                               IX1084.2
085400     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
085500     PERFORM FAIL.                                                IX1084.2
085600     GO TO READ-WRITE-F2-02-2.                                    IX1084.2
085700 READ-PASS-F2-02-2.                                               IX1084.2
085800     PERFORM PASS.                                                IX1084.2
085900 READ-WRITE-F2-02-2.                                              IX1084.2
086000     PERFORM PRINT-DETAIL.                                        IX1084.2
086100                                                                  IX1084.2
086200******************************************************************IX1084.2
086300*   TEST: IF READ .. NOT INVALID  ... END-READ ...      .        *IX1084.2
086400*                                                                *IX1084.2
086500*         IX-28, 4.5.4                                           *IX1084.2
086600******************************************************************IX1084.2
086700 READ-INIT-F2-03.                                                 IX1084.2
086800     MOVE "READ-TEST-F2-03-1" TO PAR-NAME.                        IX1084.2
086900     MOVE "IF . READ . NOT INV. . END-" TO FEATURE.               IX1084.2
087000     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
087100     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
087200     MOVE    1      TO  SWITCH-IF.                                IX1084.2
087300 READ-TEST-F2-03.                                                 IX1084.2
087400     MOVE 3 TO WRK-DU-09V00-001.                                  IX1084.2
087500     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
087600     IF SWITCH-IF = 1                                             IX1084.2
087700        READ  IX-FS2 INVALID KEY                                  IX1084.2
087800              GO TO READ-FAIL-F2-03-1                             IX1084.2
087900          NOT INVALID KEY                                         IX1084.2
088000             MOVE 1 TO SWITCH-NOT-INVALID                         IX1084.2
088100        END-READ                                                  IX1084.2
088200             MOVE 1 TO SWITCH-END-XXX.                            IX1084.2
088300     MOVE 9 TO SWITCH-END-X9X.                                    IX1084.2
088400 READ-TEST-F2-03-1.                                               IX1084.2
088500     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
088600          GO TO READ-PASS-F2-03-1.                                IX1084.2
088700 READ-FAIL-F2-03-1.                                               IX1084.2
088800     MOVE "IX-28 4.5.4 (11) C                        " TO RE-MARK.IX1084.2
088900     PERFORM FAIL.                                                IX1084.2
089000     GO TO READ-WRITE-F2-03-1.                                    IX1084.2
089100 READ-PASS-F2-03-1.                                               IX1084.2
089200     PERFORM PASS.                                                IX1084.2
089300 READ-WRITE-F2-03-1.                                              IX1084.2
089400     PERFORM PRINT-DETAIL.                                        IX1084.2
089500                                                                  IX1084.2
089600 READ-TEST-F2-03-2.                                               IX1084.2
089700     MOVE "READ-TEST-F2-03-2" TO PAR-NAME.                        IX1084.2
089800     IF SWITCH-END-XXX     = 1                                    IX1084.2
089900          GO TO READ-PASS-F2-03-2.                                IX1084.2
090000 READ-FAIL-F2-03-2.                                               IX1084.2
090100     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
090200     PERFORM FAIL.                                                IX1084.2
090300     GO TO READ-WRITE-F2-03-2.                                    IX1084.2
090400 READ-PASS-F2-03-2.                                               IX1084.2
090500     PERFORM PASS.                                                IX1084.2
090600 READ-WRITE-F2-03-2.                                              IX1084.2
090700     PERFORM PRINT-DETAIL.                                        IX1084.2
090800                                                                  IX1084.2
090900 READ-TEST-F2-03-3.                                               IX1084.2
091000     MOVE "READ-TEST-F2-03-3" TO PAR-NAME.                        IX1084.2
091100     IF SWITCH-END-X9X     = 9                                    IX1084.2
091200          GO TO READ-PASS-F2-03-3.                                IX1084.2
091300 READ-FAIL-F2-03-3.                                               IX1084.2
091400     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
091500     PERFORM FAIL.                                                IX1084.2
091600     GO TO READ-WRITE-F2-03-3.                                    IX1084.2
091700 READ-PASS-F2-03-3.                                               IX1084.2
091800     PERFORM PASS.                                                IX1084.2
091900 READ-WRITE-F2-03-3.                                              IX1084.2
092000     PERFORM PRINT-DETAIL.                                        IX1084.2
092100                                                                  IX1084.2
092200     CLOSE IX-FS2.                                                IX1084.2
092300                                                                  IX1084.2
092400******************************************************************IX1084.2
092500*                                                                *IX1084.2
092600*   TESTS:   D E L E T E        NOT INVALID    END-DELETE        *IX1084.2
092700*         FOR A FILE WHICH IS IN RANDOM ACCESS MODE              *IX1084.2
092800*                                                                *IX1084.2
092900*                                                                *IX1084.2
093000*   TEST  DELETE  NOT INVALID                                    *IX1084.2
093100*                                                                *IX1084.2
093200*         IX-21, 4.3.2                                           *IX1084.2
093300******************************************************************IX1084.2
093400 DELETE-INIT-GF-01.                                               IX1084.2
093500     MOVE     2     TO  WRK-CS-09V00-012.                         IX1084.2
093600     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX1084.2
093700     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX1084.2
093800     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX1084.2
093900     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX1084.2
094000     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX1084.2
094100     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX1084.2
094200     MOVE    SPACE TO IX-FS2-STATUS.                              IX1084.2
094300     OPEN     I-O  IX-FS2.                                        IX1084.2
094400     MOVE     SPACE     TO  WRK-XN-0002-002                       IX1084.2
094500     MOVE     SPACE     TO  WRK-XN-0002-003                       IX1084.2
094600     MOVE     SPACE     TO  WRK-XN-0002-004                       IX1084.2
094700     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1084.2
094800     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1084.2
094900     MOVE     SPACE TO IX-FS2-STATUS.                             IX1084.2
095000     MOVE "DELETE-TEST-GF-01" TO PAR-NAME.                        IX1084.2
095100     MOVE "DELETE NOT INVALID." TO FEATURE.                       IX1084.2
095200     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
095300     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
095400 DELETE-TEST-GF-01-0.                                             IX1084.2
095500     MOVE 1 TO WRK-DU-09V00-001.                                  IX1084.2
095600     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
095700     READ  IX-FS2 INVALID KEY                                     IX1084.2
095800              MOVE " READ FAILED " TO RE-MARK                     IX1084.2
095900              PERFORM FAIL                                        IX1084.2
096000              GO TO DELETE-WRITE-GF-01.                           IX1084.2
096100     DELETE IX-FS2 RECORD                                         IX1084.2
096200            INVALID KEY   MOVE "DELETE IS INVALID" TO RE-MARK     IX1084.2
096300                          PERFORM FAIL                            IX1084.2
096400                          GO TO DELETE-WRITE-GF-01                IX1084.2
096500          NOT INVALID KEY                                         IX1084.2
096600           MOVE 1 TO SWITCH-NOT-INVALID.                          IX1084.2
096700 DELETE-TEST-GF-01.                                               IX1084.2
096800     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
096900          GO TO DELETE-PASS-GF-01.                                IX1084.2
097000 DELETE-FAIL-GF-01.                                               IX1084.2
097100     MOVE "IX-21, 4.3.2                              " TO RE-MARK.IX1084.2
097200     PERFORM FAIL.                                                IX1084.2
097300     GO TO DELETE-WRITE-GF-01.                                    IX1084.2
097400 DELETE-PASS-GF-01.                                               IX1084.2
097500     PERFORM PASS.                                                IX1084.2
097600 DELETE-WRITE-GF-01.                                              IX1084.2
097700     PERFORM PRINT-DETAIL.                                        IX1084.2
097800                                                                  IX1084.2
097900******************************************************************IX1084.2
098000*   TEST  DELETE  NOT INVALID  ... END-DELETE.                   *IX1084.2
098100*                                                                *IX1084.2
098200*         IX-21, 4.3.2                                           *IX1084.2
098300******************************************************************IX1084.2
098400 DELETE-INIT-GF-02.                                               IX1084.2
098500     MOVE "DELETE-TEST-GF-02-1"   TO PAR-NAME.                    IX1084.2
098600     MOVE "DELETE NOT INV. . END-" TO FEATURE.                    IX1084.2
098700     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
098800     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
098900 DELETE-TEST-GF-02.                                               IX1084.2
099000     MOVE 2 TO WRK-DU-09V00-001.                                  IX1084.2
099100     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
099200     READ  IX-FS2 INVALID KEY                                     IX1084.2
099300              MOVE "READ FAILED " TO RE-MARK                      IX1084.2
099400              PERFORM FAIL                                        IX1084.2
099500              GO TO DELETE-WRITE-GF-02-1.                         IX1084.2
099600     DELETE IX-FS2 RECORD                                         IX1084.2
099700            INVALID       MOVE "DELETE IS INVALID" TO RE-MARK     IX1084.2
099800                          PERFORM FAIL                            IX1084.2
099900                          GO TO DELETE-WRITE-GF-02-1              IX1084.2
100000          NOT INVALID                                             IX1084.2
100100           MOVE 1 TO SWITCH-NOT-INVALID                           IX1084.2
100200        END-DELETE.                                               IX1084.2
100300     MOVE 1 TO SWITCH-END-XXX.                                    IX1084.2
100400 DELETE-TEST-GF-02-1.                                             IX1084.2
100500     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
100600          GO TO DELETE-PASS-GF-02-1.                              IX1084.2
100700 DELETE-FAIL-GF-02-1.                                             IX1084.2
100800     MOVE "IX-21, 4.3.2                              " TO RE-MARK.IX1084.2
100900     PERFORM FAIL.                                                IX1084.2
101000     GO TO DELETE-WRITE-GF-02-1.                                  IX1084.2
101100 DELETE-PASS-GF-02-1.                                             IX1084.2
101200     PERFORM PASS.                                                IX1084.2
101300 DELETE-WRITE-GF-02-1.                                            IX1084.2
101400     PERFORM PRINT-DETAIL.                                        IX1084.2
101500                                                                  IX1084.2
101600 DELETE-TEST-GF-02-2.                                             IX1084.2
101700     MOVE "DELETE-TEST-GF-02-2"   TO PAR-NAME.                    IX1084.2
101800     MOVE "END-DELETE.      " TO FEATURE.                         IX1084.2
101900     IF SWITCH-END-XXX     = 1                                    IX1084.2
102000          GO TO DELETE-PASS-GF-02-2.                              IX1084.2
102100 DELETE-FAIL-GF-02-2.                                             IX1084.2
102200     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
102300     PERFORM FAIL.                                                IX1084.2
102400     GO TO DELETE-WRITE-GF-02-2.                                  IX1084.2
102500 DELETE-PASS-GF-02-2.                                             IX1084.2
102600     PERFORM PASS.                                                IX1084.2
102700 DELETE-WRITE-GF-02-2.                                            IX1084.2
102800     PERFORM PRINT-DETAIL.                                        IX1084.2
102900                                                                  IX1084.2
103000******************************************************************IX1084.2
103100*   TEST: IF DELETE. NOT INVALID  ... END-DELETE ...    .        *IX1084.2
103200*                                                                *IX1084.2
103300*         IX-21, 4.3.2                                           *IX1084.2
103400******************************************************************IX1084.2
103500 DELETE-INIT-GF-03.                                               IX1084.2
103600     MOVE "DELETE-TEST-GF-03-1"   TO PAR-NAME.                    IX1084.2
103700     MOVE "IF .DELETE. NOT INV. . END-" TO FEATURE.               IX1084.2
103800     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
103900     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
104000     MOVE    1      TO  SWITCH-IF.                                IX1084.2
104100 DELETE-TEST-GF-03.                                               IX1084.2
104200     MOVE 3 TO WRK-DU-09V00-001.                                  IX1084.2
104300     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
104400     READ  IX-FS2 INVALID KEY                                     IX1084.2
104500              MOVE "READ FAILED " TO RE-MARK                      IX1084.2
104600              PERFORM FAIL                                        IX1084.2
104700              GO TO DELETE-WRITE-GF-03-1.                         IX1084.2
104800     IF SWITCH-IF = 1                                             IX1084.2
104900        DELETE IX-FS2 RECORD                                      IX1084.2
105000            INVALID KEY   MOVE "DELETE IS INVALID" TO RE-MARK     IX1084.2
105100                          PERFORM FAIL                            IX1084.2
105200                          GO TO DELETE-WRITE-GF-03-1              IX1084.2
105300          NOT INVALID KEY                                         IX1084.2
105400             MOVE 1 TO SWITCH-NOT-INVALID                         IX1084.2
105500        END-DELETE                                                IX1084.2
105600             MOVE 1 TO SWITCH-END-XXX.                            IX1084.2
105700     MOVE 9 TO SWITCH-END-X9X.                                    IX1084.2
105800 DELETE-TEST-GF-03-1.                                             IX1084.2
105900     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
106000          GO TO DELETE-PASS-GF-03-1.                              IX1084.2
106100 DELETE-FAIL-GF-03-1.                                             IX1084.2
106200     MOVE "IX-21, 4.3.2                              " TO RE-MARK.IX1084.2
106300     PERFORM FAIL.                                                IX1084.2
106400     GO TO DELETE-WRITE-GF-03-1.                                  IX1084.2
106500 DELETE-PASS-GF-03-1.                                             IX1084.2
106600     PERFORM PASS.                                                IX1084.2
106700 DELETE-WRITE-GF-03-1.                                            IX1084.2
106800     PERFORM PRINT-DETAIL.                                        IX1084.2
106900                                                                  IX1084.2
107000 DELETE-TEST-GF-03-2.                                             IX1084.2
107100     MOVE "END-DELETE" TO FEATURE.                                IX1084.2
107200     MOVE "DELETE-TEST-GF-03-2"   TO PAR-NAME.                    IX1084.2
107300     IF SWITCH-END-XXX     = 1                                    IX1084.2
107400          GO TO DELETE-PASS-GF-03-2.                              IX1084.2
107500 DELETE-FAIL-GF-03-2.                                             IX1084.2
107600     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
107700     PERFORM FAIL.                                                IX1084.2
107800     GO TO DELETE-WRITE-GF-03-2.                                  IX1084.2
107900 DELETE-PASS-GF-03-2.                                             IX1084.2
108000     PERFORM PASS.                                                IX1084.2
108100 DELETE-WRITE-GF-03-2.                                            IX1084.2
108200     PERFORM PRINT-DETAIL.                                        IX1084.2
108300                                                                  IX1084.2
108400 DELETE-TEST-GF-03-3.                                             IX1084.2
108500     MOVE "DELETE-TEST-GF-03-3"   TO PAR-NAME.                    IX1084.2
108600     IF SWITCH-END-X9X     = 9                                    IX1084.2
108700          GO TO DELETE-PASS-GF-03-3.                              IX1084.2
108800 DELETE-FAIL-GF-03-3.                                             IX1084.2
108900     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
109000     PERFORM FAIL.                                                IX1084.2
109100     GO TO DELETE-WRITE-GF-03-3.                                  IX1084.2
109200 DELETE-PASS-GF-03-3.                                             IX1084.2
109300     PERFORM PASS.                                                IX1084.2
109400 DELETE-WRITE-GF-03-3.                                            IX1084.2
109500     PERFORM PRINT-DETAIL.                                        IX1084.2
109600                                                                  IX1084.2
109700      CLOSE IX-FS2.                                               IX1084.2
109800                                                                  IX1084.2
109900******************************************************************IX1084.2
110000*                                                                *IX1084.2
110100*   TESTS:   R E W R I T E      NOT INVALID    END-REWRITE       *IX1084.2
110200*         FOR A FILE WHICH IS IN RANDOM ACCESS MODE              *IX1084.2
110300*                                                                *IX1084.2
110400*                                                                *IX1084.2
110500*   TEST  REWRITE NOT INVALID                                    *IX1084.2
110600*                                                                *IX1084.2
110700*         IX-33, 4.6.2                                           *IX1084.2
110800******************************************************************IX1084.2
110900 REWRITE-INIT-GF-01.                                              IX1084.2
111000     MOVE     2     TO  WRK-CS-09V00-012.                         IX1084.2
111100     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX1084.2
111200     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX1084.2
111300     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX1084.2
111400     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX1084.2
111500     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX1084.2
111600     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX1084.2
111700     MOVE    SPACE TO IX-FS2-STATUS.                              IX1084.2
111800     OPEN     I-O  IX-FS2.                                        IX1084.2
111900     MOVE     SPACE     TO  WRK-XN-0002-002                       IX1084.2
112000     MOVE     SPACE     TO  WRK-XN-0002-003                       IX1084.2
112100     MOVE     SPACE     TO  WRK-XN-0002-004                       IX1084.2
112200     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1084.2
112300     MOVE      IX-FS2-STATUS TO WRK-XN-0002-001                   IX1084.2
112400     MOVE     SPACE TO IX-FS2-STATUS.                             IX1084.2
112500     MOVE "REWRITE-TEST-GF-01" TO PAR-NAME.                       IX1084.2
112600     MOVE "REWRITE NOT INVALID." TO FEATURE.                      IX1084.2
112700     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
112800     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
112900 REWRITE-TEST-GF-01-0.                                            IX1084.2
113000     MOVE 5 TO WRK-DU-09V00-001.                                  IX1084.2
113100     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
113200     READ  IX-FS2 INVALID KEY                                     IX1084.2
113300              MOVE " READ FAILED " TO RE-MARK                     IX1084.2
113400              PERFORM FAIL                                        IX1084.2
113500              GO TO REWRITE-WRITE-GF-01.                          IX1084.2
113600     MOVE "RECORD REPLACED BY REWRITE STATEMENT" TO IX-FS2-REC-120IX1084.2
113700     REWRITE IX-FS2R1-F-G-240                                     IX1084.2
113800            INVALID KEY   MOVE "REWRITE IS INVALID" TO RE-MARK    IX1084.2
113900                          PERFORM FAIL                            IX1084.2
114000                          GO TO REWRITE-WRITE-GF-01               IX1084.2
114100          NOT INVALID KEY                                         IX1084.2
114200           MOVE 1 TO SWITCH-NOT-INVALID.                          IX1084.2
114300 REWRITE-TEST-GF-01.                                              IX1084.2
114400     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
114500          GO TO REWRITE-PASS-GF-01.                               IX1084.2
114600 REWRITE-FAIL-GF-01.                                              IX1084.2
114700     MOVE "IX-33, 4.6.2                              " TO RE-MARK.IX1084.2
114800     PERFORM FAIL.                                                IX1084.2
114900     GO TO REWRITE-WRITE-GF-01.                                   IX1084.2
115000 REWRITE-PASS-GF-01.                                              IX1084.2
115100     PERFORM PASS.                                                IX1084.2
115200 REWRITE-WRITE-GF-01.                                             IX1084.2
115300     PERFORM PRINT-DETAIL.                                        IX1084.2
115400                                                                  IX1084.2
115500******************************************************************IX1084.2
115600*   TEST  REWRITE NOT INVALID  ... END-DELETE.                   *IX1084.2
115700*                                                                *IX1084.2
115800*         IX-33, 4.6.2                                           *IX1084.2
115900******************************************************************IX1084.2
116000 REWRITE-INIT-GF-02.                                              IX1084.2
116100     MOVE "REWRITE-TEST-GF-02-1"  TO PAR-NAME.                    IX1084.2
116200     MOVE "REWRITE NOT INV. . END-" TO FEATURE.                   IX1084.2
116300     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
116400     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
116500 REWRITE-TEST-GF-02.                                              IX1084.2
116600     MOVE 6 TO WRK-DU-09V00-001.                                  IX1084.2
116700     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
116800     READ  IX-FS2 INVALID KEY                                     IX1084.2
116900              MOVE "READ FAILED " TO RE-MARK                      IX1084.2
117000              PERFORM FAIL                                        IX1084.2
117100              GO TO REWRITE-WRITE-GF-02-1.                        IX1084.2
117200     MOVE "RECORD REPLACED BY REWRITE STATEMENT" TO IX-FS2-REC-120IX1084.2
117300     REWRITE IX-FS2R1-F-G-240                                     IX1084.2
117400            INVALID KEY   MOVE "REWRITE IS INVALID" TO RE-MARK    IX1084.2
117500                          PERFORM FAIL                            IX1084.2
117600                          GO TO REWRITE-WRITE-GF-02-1             IX1084.2
117700          NOT INVALID                                             IX1084.2
117800           MOVE 1 TO SWITCH-NOT-INVALID                           IX1084.2
117900        END-REWRITE.                                              IX1084.2
118000     MOVE 1 TO SWITCH-END-XXX.                                    IX1084.2
118100 REWRITE-TEST-GF-02-1.                                            IX1084.2
118200     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
118300          GO TO REWRITE-PASS-GF-02-1.                             IX1084.2
118400 REWRITE-FAIL-GF-02-1.                                            IX1084.2
118500     MOVE "IX-33, 4.6.2                              " TO RE-MARK.IX1084.2
118600     PERFORM FAIL.                                                IX1084.2
118700     GO TO REWRITE-WRITE-GF-02-1.                                 IX1084.2
118800 REWRITE-PASS-GF-02-1.                                            IX1084.2
118900     PERFORM PASS.                                                IX1084.2
119000 REWRITE-WRITE-GF-02-1.                                           IX1084.2
119100     PERFORM PRINT-DETAIL.                                        IX1084.2
119200                                                                  IX1084.2
119300 REWRITE-TEST-GF-02-2.                                            IX1084.2
119400     MOVE "REWRITE-TEST-GF-02-2"  TO PAR-NAME.                    IX1084.2
119500     MOVE "END-REWRITE.     " TO FEATURE.                         IX1084.2
119600     IF SWITCH-END-XXX     = 1                                    IX1084.2
119700          GO TO REWRITE-PASS-GF-02-2.                             IX1084.2
119800 REWRITE-FAIL-GF-02-2.                                            IX1084.2
119900     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
120000     PERFORM FAIL.                                                IX1084.2
120100     GO TO REWRITE-WRITE-GF-02-2.                                 IX1084.2
120200 REWRITE-PASS-GF-02-2.                                            IX1084.2
120300     PERFORM PASS.                                                IX1084.2
120400 REWRITE-WRITE-GF-02-2.                                           IX1084.2
120500     PERFORM PRINT-DETAIL.                                        IX1084.2
120600                                                                  IX1084.2
120700******************************************************************IX1084.2
120800*   TEST: IF REWRITE. NOT INVALID ... END-DELETE ...    .        *IX1084.2
120900*                                                                *IX1084.2
121000*         IX-33, 4.6.2                                           *IX1084.2
121100******************************************************************IX1084.2
121200 REWRITE-INIT-GF-03.                                              IX1084.2
121300     MOVE "REWRITE-TEST-GF-03-1"  TO PAR-NAME.                    IX1084.2
121400     MOVE "IF .REWRITE. NOT INV. . END-" TO FEATURE.              IX1084.2
121500     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
121600     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
121700     MOVE    1      TO  SWITCH-IF.                                IX1084.2
121800 REWRITE-TEST-GF-03.                                              IX1084.2
121900     MOVE 7 TO WRK-DU-09V00-001.                                  IX1084.2
122000     MOVE GRP-0101  TO IX-FS2-KEY.                                IX1084.2
122100     READ  IX-FS2 INVALID KEY                                     IX1084.2
122200              MOVE "READ FAILED " TO RE-MARK                      IX1084.2
122300              PERFORM FAIL                                        IX1084.2
122400              GO TO REWRITE-WRITE-GF-03-1.                        IX1084.2
122500     IF SWITCH-IF = 1                                             IX1084.2
122600     MOVE "RECORD REPLACED BY REWRITE STATEMENT" TO IX-FS2-REC-120IX1084.2
122700        REWRITE IX-FS2R1-F-G-240                                  IX1084.2
122800            INVALID KEY   MOVE "REWRITE IS INVALID" TO RE-MARK    IX1084.2
122900                          PERFORM FAIL                            IX1084.2
123000                          GO TO REWRITE-WRITE-GF-03-1             IX1084.2
123100          NOT INVALID KEY                                         IX1084.2
123200             MOVE 1 TO SWITCH-NOT-INVALID                         IX1084.2
123300        END-REWRITE                                               IX1084.2
123400             MOVE 1 TO SWITCH-END-XXX.                            IX1084.2
123500     MOVE 9 TO SWITCH-END-X9X.                                    IX1084.2
123600 REWRITE-TEST-GF-03-1.                                            IX1084.2
123700     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
123800          GO TO REWRITE-PASS-GF-03-1.                             IX1084.2
123900 REWRITE-FAIL-GF-03-1.                                            IX1084.2
124000     MOVE "IX-33, 4.6.2                              " TO RE-MARK.IX1084.2
124100     PERFORM FAIL.                                                IX1084.2
124200     GO TO REWRITE-WRITE-GF-03-1.                                 IX1084.2
124300 REWRITE-PASS-GF-03-1.                                            IX1084.2
124400     PERFORM PASS.                                                IX1084.2
124500 REWRITE-WRITE-GF-03-1.                                           IX1084.2
124600     PERFORM PRINT-DETAIL.                                        IX1084.2
124700                                                                  IX1084.2
124800 REWRITE-TEST-GF-03-2.                                            IX1084.2
124900     MOVE "END-REWRITE" TO FEATURE.                               IX1084.2
125000     MOVE "REWRITE-TEST-GF-03-2"  TO PAR-NAME.                    IX1084.2
125100     IF SWITCH-END-XXX     = 1                                    IX1084.2
125200          GO TO REWRITE-PASS-GF-03-2.                             IX1084.2
125300 REWRITE-FAIL-GF-03-2.                                            IX1084.2
125400     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
125500     PERFORM FAIL.                                                IX1084.2
125600     GO TO REWRITE-WRITE-GF-03-2.                                 IX1084.2
125700 REWRITE-PASS-GF-03-2.                                            IX1084.2
125800     PERFORM PASS.                                                IX1084.2
125900 REWRITE-WRITE-GF-03-2.                                           IX1084.2
126000     PERFORM PRINT-DETAIL.                                        IX1084.2
126100                                                                  IX1084.2
126200 REWRITE-TEST-GF-03-3.                                            IX1084.2
126300     MOVE "REWRITE-TEST-GF-03-3"  TO PAR-NAME.                    IX1084.2
126400     IF SWITCH-END-X9X     = 9                                    IX1084.2
126500          GO TO REWRITE-PASS-GF-03-3.                             IX1084.2
126600 REWRITE-FAIL-GF-03-3.                                            IX1084.2
126700     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
126800     PERFORM FAIL.                                                IX1084.2
126900     GO TO REWRITE-WRITE-GF-03-3.                                 IX1084.2
127000 REWRITE-PASS-GF-03-3.                                            IX1084.2
127100     PERFORM PASS.                                                IX1084.2
127200 REWRITE-WRITE-GF-03-3.                                           IX1084.2
127300     PERFORM PRINT-DETAIL.                                        IX1084.2
127400                                                                  IX1084.2
127500     CLOSE IX-FS2.                                                IX1084.2
127600                                                                  IX1084.2
127700                                                                  IX1084.2
127800******************************************************************IX1084.2
127900*                                                                *IX1084.2
128000*   TESTS:   R E W R I T E      NOT INVALID    END-REWRITE       *IX1084.2
128100*         FOR A FILE WHICH IS IN SEQUENTIAL ACCESS MODE          *IX1084.2
128200*                                                                *IX1084.2
128300*                                                                *IX1084.2
128400*   TEST  REWRITE NOT INVALID                                    *IX1084.2
128500*                                                                *IX1084.2
128600*         IX-33, 4.6.2                                           *IX1084.2
128700******************************************************************IX1084.2
128800 RWR-SEQ-INIT-GF-01.                                              IX1084.2
128900     MOVE     2     TO  WRK-CS-09V00-012.                         IX1084.2
129000     MOVE     ZERO  TO  WRK-CS-09V00-013.                         IX1084.2
129100     MOVE     ZERO  TO  WRK-CS-09V00-014.                         IX1084.2
129200     MOVE     ZERO  TO  WRK-CS-09V00-015.                         IX1084.2
129300     MOVE     ZERO  TO  WRK-CS-09V00-016.                         IX1084.2
129400     MOVE     ZERO  TO  WRK-CS-09V00-017.                         IX1084.2
129500     MOVE     ZERO  TO  WRK-CS-09V00-018.                         IX1084.2
129600     MOVE    SPACE TO IX-FS1-STATUS.                              IX1084.2
129700     OPEN     I-O  IX-FS1.                                        IX1084.2
129800     MOVE     SPACE     TO  WRK-XN-0002-002                       IX1084.2
129900     MOVE     SPACE     TO  WRK-XN-0002-003                       IX1084.2
130000     MOVE     SPACE     TO  WRK-XN-0002-004                       IX1084.2
130100     MOVE      IX-FS1-STATUS TO WRK-XN-0002-001                   IX1084.2
130200     MOVE      IX-FS1-STATUS TO WRK-XN-0002-001                   IX1084.2
130300     MOVE     SPACE TO IX-FS1-STATUS.                             IX1084.2
130400     MOVE "RWR-SEQ-TEST-GF-01"  TO PAR-NAME.                      IX1084.2
130500     MOVE "REWRITE NOT INVALID." TO FEATURE.                      IX1084.2
130600     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
130700     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
130800 RWR-SEQ-TEST-GF-01-0.                                            IX1084.2
130900******************************************************************IX1084.2
131000*                                                                *IX1084.2
131100*   READ AND REWRITE THE FIRST THREE RECORDS                     *IX1084.2
131200*                                                                *IX1084.2
131300******************************************************************IX1084.2
131400     READ  IX-FS1 AT END                                          IX1084.2
131500              MOVE " READ FAILED " TO RE-MARK                     IX1084.2
131600              PERFORM FAIL                                        IX1084.2
131700              GO TO RWR-SEQ-WRITE-GF-01.                          IX1084.2
131800     MOVE "RECORD REPLACED BY REWRITE STATEMENT" TO IX-FS1-REC-120IX1084.2
131900     REWRITE IX-FS1R1-F-G-240                                     IX1084.2
132000            INVALID KEY   MOVE "REWRITE IS INVALID" TO RE-MARK    IX1084.2
132100                          PERFORM FAIL                            IX1084.2
132200                          GO TO RWR-SEQ-WRITE-GF-01               IX1084.2
132300          NOT INVALID KEY                                         IX1084.2
132400           MOVE 1 TO SWITCH-NOT-INVALID.                          IX1084.2
132500 RWR-SEQ-TEST-GF-01.                                              IX1084.2
132600     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
132700          GO TO RWR-SEQ-PASS-GF-01.                               IX1084.2
132800 RWR-SEQ-FAIL-GF-01.                                              IX1084.2
132900     MOVE "IX-33, 4.6.2                              " TO RE-MARK.IX1084.2
133000     PERFORM FAIL.                                                IX1084.2
133100     GO TO RWR-SEQ-WRITE-GF-01.                                   IX1084.2
133200 RWR-SEQ-PASS-GF-01.                                              IX1084.2
133300     PERFORM PASS.                                                IX1084.2
133400 RWR-SEQ-WRITE-GF-01.                                             IX1084.2
133500     PERFORM PRINT-DETAIL.                                        IX1084.2
133600                                                                  IX1084.2
133700******************************************************************IX1084.2
133800*   TEST  REWRITE NOT INVALID  ... END-DELETE.                   *IX1084.2
133900*                                                                *IX1084.2
134000*         IX-33, 4.6.2                                           *IX1084.2
134100******************************************************************IX1084.2
134200 RWR-SEQ-INIT-GF-02.                                              IX1084.2
134300     MOVE "RWR-SEQ-TEST-GF-02-1"  TO PAR-NAME.                    IX1084.2
134400     MOVE "REWRITE NOT INV. . END-" TO FEATURE.                   IX1084.2
134500     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
134600     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
134700 RWR-SEQ-TEST-GF-02.                                              IX1084.2
134800     READ  IX-FS1 AT END                                          IX1084.2
134900              MOVE "READ FAILED " TO RE-MARK                      IX1084.2
135000              PERFORM FAIL                                        IX1084.2
135100              GO TO RWR-SEQ-WRITE-GF-02-1.                        IX1084.2
135200     MOVE "RECORD REPLACED BY REWRITE STATEMENT" TO IX-FS1-REC-120IX1084.2
135300     REWRITE IX-FS1R1-F-G-240                                     IX1084.2
135400            INVALID       MOVE "REWRITE IS INVALID" TO RE-MARK    IX1084.2
135500                          PERFORM FAIL                            IX1084.2
135600                          GO TO RWR-SEQ-WRITE-GF-02-1             IX1084.2
135700          NOT INVALID                                             IX1084.2
135800           MOVE 1 TO SWITCH-NOT-INVALID                           IX1084.2
135900        END-REWRITE.                                              IX1084.2
136000     MOVE 1 TO SWITCH-END-XXX.                                    IX1084.2
136100 RWR-SEQ-TEST-GF-02-1.                                            IX1084.2
136200     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
136300          GO TO RWR-SEQ-PASS-GF-02-1.                             IX1084.2
136400 RWR-SEQ-FAIL-GF-02-1.                                            IX1084.2
136500     MOVE "IX-33, 4.6.2                              " TO RE-MARK.IX1084.2
136600     PERFORM FAIL.                                                IX1084.2
136700     GO TO RWR-SEQ-WRITE-GF-02-1.                                 IX1084.2
136800 RWR-SEQ-PASS-GF-02-1.                                            IX1084.2
136900     PERFORM PASS.                                                IX1084.2
137000 RWR-SEQ-WRITE-GF-02-1.                                           IX1084.2
137100     PERFORM PRINT-DETAIL.                                        IX1084.2
137200                                                                  IX1084.2
137300 RWR-SEQ-TEST-GF-02-2.                                            IX1084.2
137400     MOVE "RWR-SEQ-TEST-GF-02-2"  TO PAR-NAME.                    IX1084.2
137500     MOVE "END-REWRITE.     " TO FEATURE.                         IX1084.2
137600     IF SWITCH-END-XXX     = 1                                    IX1084.2
137700          GO TO RWR-SEQ-PASS-GF-02-2.                             IX1084.2
137800 RWR-SEQ-FAIL-GF-02-2.                                            IX1084.2
137900     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
138000     PERFORM FAIL.                                                IX1084.2
138100     GO TO RWR-SEQ-WRITE-GF-02-2.                                 IX1084.2
138200 RWR-SEQ-PASS-GF-02-2.                                            IX1084.2
138300     PERFORM PASS.                                                IX1084.2
138400 RWR-SEQ-WRITE-GF-02-2.                                           IX1084.2
138500     PERFORM PRINT-DETAIL.                                        IX1084.2
138600                                                                  IX1084.2
138700******************************************************************IX1084.2
138800*   TEST: IF REWRITE. NOT INVALID ... END-DELETE ...    .        *IX1084.2
138900*                                                                *IX1084.2
139000*         IX-33, 4.6.2                                           *IX1084.2
139100******************************************************************IX1084.2
139200 RWR-SEQ-INIT-GF-03.                                              IX1084.2
139300     MOVE "RWR-SEQ-TEST-GF-03-1"  TO PAR-NAME.                    IX1084.2
139400     MOVE "IF .REWRITE. NOT INV. . END-" TO FEATURE.              IX1084.2
139500     MOVE     ZERO  TO  SWITCH-NOT-INVALID.                       IX1084.2
139600     MOVE     ZERO  TO  SWITCH-END-XXX.                           IX1084.2
139700     MOVE    1      TO  SWITCH-IF.                                IX1084.2
139800 RWR-SEQ-TEST-GF-03.                                              IX1084.2
139900     READ  IX-FS1 END                                             IX1084.2
140000              MOVE "READ FAILED " TO RE-MARK                      IX1084.2
140100              PERFORM FAIL                                        IX1084.2
140200              GO TO RWR-SEQ-WRITE-GF-03-1.                        IX1084.2
140300     IF SWITCH-IF = 1                                             IX1084.2
140400     MOVE "RECORD REPLACED BY REWRITE STATEMENT" TO IX-FS1-REC-120IX1084.2
140500        REWRITE IX-FS1R1-F-G-240                                  IX1084.2
140600            INVALID KEY   MOVE "REWRITE IS INVALID" TO RE-MARK    IX1084.2
140700                          PERFORM FAIL                            IX1084.2
140800                          GO TO RWR-SEQ-WRITE-GF-03-1             IX1084.2
140900          NOT INVALID KEY                                         IX1084.2
141000             MOVE 1 TO SWITCH-NOT-INVALID                         IX1084.2
141100        END-REWRITE                                               IX1084.2
141200             MOVE 1 TO SWITCH-END-XXX.                            IX1084.2
141300     MOVE 9 TO SWITCH-END-X9X.                                    IX1084.2
141400 RWR-SEQ-TEST-GF-03-1.                                            IX1084.2
141500     IF SWITCH-NOT-INVALID = 1                                    IX1084.2
141600          GO TO RWR-SEQ-PASS-GF-03-1.                             IX1084.2
141700 RWR-SEQ-FAIL-GF-03-1.                                            IX1084.2
141800     MOVE "IX-33, 4.6.2                              " TO RE-MARK.IX1084.2
141900     PERFORM FAIL.                                                IX1084.2
142000     GO TO RWR-SEQ-WRITE-GF-03-1.                                 IX1084.2
142100 RWR-SEQ-PASS-GF-03-1.                                            IX1084.2
142200     PERFORM PASS.                                                IX1084.2
142300 RWR-SEQ-WRITE-GF-03-1.                                           IX1084.2
142400     PERFORM PRINT-DETAIL.                                        IX1084.2
142500                                                                  IX1084.2
142600 RWR-SEQ-TEST-GF-03-2.                                            IX1084.2
142700     MOVE "END-REWRITE" TO FEATURE.                               IX1084.2
142800     MOVE "RWR-SEQ-TEST-GF-03-2"  TO PAR-NAME.                    IX1084.2
142900     IF SWITCH-END-XXX     = 1                                    IX1084.2
143000          GO TO RWR-SEQ-PASS-GF-03-2.                             IX1084.2
143100 RWR-SEQ-FAIL-GF-03-2.                                            IX1084.2
143200     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
143300     PERFORM FAIL.                                                IX1084.2
143400     GO TO RWR-SEQ-WRITE-GF-03-2.                                 IX1084.2
143500 RWR-SEQ-PASS-GF-03-2.                                            IX1084.2
143600     PERFORM PASS.                                                IX1084.2
143700 RWR-SEQ-WRITE-GF-03-2.                                           IX1084.2
143800     PERFORM PRINT-DETAIL.                                        IX1084.2
143900                                                                  IX1084.2
144000 RWR-SEQ-TEST-GF-03-3.                                            IX1084.2
144100     MOVE "RWR-SEQ-TEST-GF-03-3"  TO PAR-NAME.                    IX1084.2
144200     IF SWITCH-END-X9X     = 9                                    IX1084.2
144300          GO TO RWR-SEQ-PASS-GF-03-3.                             IX1084.2
144400 RWR-SEQ-FAIL-GF-03-3.                                            IX1084.2
144500     MOVE "IX-32 4.5.4 (19)                          " TO RE-MARK.IX1084.2
144600     PERFORM FAIL.                                                IX1084.2
144700     GO TO RWR-SEQ-WRITE-GF-03-3.                                 IX1084.2
144800 RWR-SEQ-PASS-GF-03-3.                                            IX1084.2
144900     PERFORM PASS.                                                IX1084.2
145000 RWR-SEQ-WRITE-GF-03-3.                                           IX1084.2
145100     PERFORM PRINT-DETAIL.                                        IX1084.2
145200                                                                  IX1084.2
145300     CLOSE IX-FS1.                                                IX1084.2
145400                                                                  IX1084.2
145500                                                                  IX1084.2
145600 CCVS-EXIT SECTION.                                               IX1084.2
145700 CCVS-999999.                                                     IX1084.2
145800     GO TO CLOSE-FILES.                                           IX1084.2
      *END-OF,IX108A                                                                  
