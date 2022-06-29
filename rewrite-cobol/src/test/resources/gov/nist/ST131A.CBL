      *HEADER,COBOL,ST131A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1314.2
000200 PROGRAM-ID.                                                      ST1314.2
000300     ST131A.                                                      ST1314.2
000400****************************************************************  ST1314.2
000500*                                                              *  ST1314.2
000600*    VALIDATION FOR:-                                          *  ST1314.2
000700*                                                              *  ST1314.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1314.2
000900*                                                              *  ST1314.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1314.2
001100*                                                              *  ST1314.2
001200****************************************************************  ST1314.2
001300*                                                              *  ST1314.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1314.2
001500*                                                              *  ST1314.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1314.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1314.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1314.2
001900*                                                              *  ST1314.2
002000****************************************************************  ST1314.2
002100*    THIS PROGRAM CONTAINS 3 SORTS USING NUMERIC OR ALPHABETIC    ST1314.2
002200*    KEYS - BUT NOT BOTH IN THE SAME KEY  DUE TO DIFFERING        ST1314.2
002300*    COLLATING SEQUENCES AMONG COMPUTERS. EXTERNAL FILES ARE      ST1314.2
002400*    GENERATED INTERNALLY FOR SUBSEQUENT USE. THE SELECT CLAUSE   ST1314.2
002500*    IS HIGHLY DEPENDENT ON HARDWARE. THE USER SHOULD EXERCISE THEST1314.2
002600*    VARIOUS OPTIONS OF HARDWARE ASSIGNMENTS TO THE EXTENT THEY   ST1314.2
002700*    ARE AVAILABLE. THE SORT OF A MULTI-REEL FILE IS EXERCISED    ST1314.2
002800*    IN PROGRAM ST202. HOWEVER THE EXERCISE OF THE "FOR MULTIPLE  ST1314.2
002900*    REEL-UNIT" OF THE GIVING OPTION IS NOT DUE TO THE INDETER-   ST1314.2
003000*    MINATE LENGTH OF SUCH A FILE (E.G. RECORDING DENSITY OR SIZE ST1314.2
003100*    OF UNIT) AND PROCESSING COST. SORT INPUT-OUTPUT OPTIONS      ST1314.2
003200*    WILL BE EXERCISED AS FOLLOWS.                                ST1314.2
003300*        SORT 1  USING        GIVING                              ST1314.2
003400*        SORT 2  INPUT PROC   GIVING                              ST1314.2
003500*        SORT 3  INPUT PROC   OUTPUT PROC                         ST1314.2
003600                                                                  ST1314.2
003700 ENVIRONMENT DIVISION.                                            ST1314.2
003800 CONFIGURATION SECTION.                                           ST1314.2
003900 SOURCE-COMPUTER.                                                 ST1314.2
004000     XXXXX082.                                                    ST1314.2
004100 OBJECT-COMPUTER.                                                 ST1314.2
004200     XXXXX083.                                                    ST1314.2
004300 INPUT-OUTPUT SECTION.                                            ST1314.2
004400 FILE-CONTROL.                                                    ST1314.2
004500     SELECT PRINT-FILE ASSIGN TO                                  ST1314.2
004600     XXXXX055.                                                    ST1314.2
004700     SELECT SORT1 ASSIGN TO                                       ST1314.2
004800     XXXXX027.                                                    ST1314.2
004900     SELECT SORT2 ASSIGN TO                                       ST1314.2
005000     XXXXX028.                                                    ST1314.2
005100     SELECT SORT3 ASSIGN TO                                       ST1314.2
005200     XXXXX029.                                                    ST1314.2
005300     SELECT FILE1 ASSIGN TO                                       ST1314.2
005400     XXXXX001.                                                    ST1314.2
005500     SELECT FILE2 ASSIGN TO                                       ST1314.2
005600     XXXXX014.                                                    ST1314.2
005700     SELECT FILE3 ASSIGN TO                                       ST1314.2
005800     XXXXX015.                                                    ST1314.2
005900 I-O-CONTROL.                                                     ST1314.2
006000     SAME RECORD AREA FOR SORT1 SORT2                             ST1314.2
006100     SAME RECORD AREA FOR SORT3 FILE3.                            ST1314.2
006200 DATA DIVISION.                                                   ST1314.2
006300 FILE SECTION.                                                    ST1314.2
006400 FD  PRINT-FILE.                                                  ST1314.2
006500 01  PRINT-REC PICTURE X(120).                                    ST1314.2
006600 01  DUMMY-RECORD PICTURE X(120).                                 ST1314.2
006700 FD  FILE1                                                        ST1314.2
006800C    LABEL RECORDS ARE STANDARD                                   ST1314.2
006900C    VALUE OF                                                     ST1314.2
007000C    XXXXX074                                                     ST1314.2
007100C    IS                                                           ST1314.2
007200C    XXXXX075                                                     ST1314.2
007300     BLOCK CONTAINS 10 RECORDS                                    ST1314.2
007400C    DATA RECORD R1                                               ST1314.2
007500     .                                                            ST1314.2
007600 01  R1.                                                          ST1314.2
007700     02  FILLER PICTURE X(120).                                   ST1314.2
007800 FD  FILE2                                                        ST1314.2
007900C    LABEL RECORDS ARE STANDARD                                   ST1314.2
008000C    VALUE OF                                                     ST1314.2
008100C    XXXXX074                                                     ST1314.2
008200C    IS                                                           ST1314.2
008300C    XXXXX076                                                     ST1314.2
008400     BLOCK CONTAINS 10 RECORDS                                    ST1314.2
008500C    DATA RECORD R2                                               ST1314.2
008600     .                                                            ST1314.2
008700 01  R2.                                                          ST1314.2
008800     02  R2-KEYS.                                                 ST1314.2
008900         03  R2-1 PICTURE 999.                                    ST1314.2
009000         03  R2-2 PICTURE AA.                                     ST1314.2
009100         03  R2-3 PICTURE AA.                                     ST1314.2
009200     02  FILLER PICTURE X(113).                                   ST1314.2
009300 FD  FILE3                                                        ST1314.2
009400     BLOCK CONTAINS 10 RECORDS                                    ST1314.2
009500C    LABEL RECORDS ARE STANDARD                                   ST1314.2
009600C    VALUE OF                                                     ST1314.2
009700C    XXXXX074                                                     ST1314.2
009800C    IS                                                           ST1314.2
009900C    XXXXX077                                                     ST1314.2
010000C    DATA RECORD IS R3                                            ST1314.2
010100     .                                                            ST1314.2
010200 01  R3.                                                          ST1314.2
010300     02  R3-KEYS.                                                 ST1314.2
010400         03  R3-1 PICTURE 999.                                    ST1314.2
010500         03  R3-2 PICTURE AA.                                     ST1314.2
010600         03  R3-3 PICTURE AA.                                     ST1314.2
010700         03  R3-4 PICTURE 9999.                                   ST1314.2
010800     02  FILLER PICTURE X(109).                                   ST1314.2
010900 SD  SORT1                                                        ST1314.2
011000     RECORD CONTAINS 120 CHARACTERS                               ST1314.2
011100     DATA RECORD IS S1.                                           ST1314.2
011200 01  S1.                                                          ST1314.2
011300     02  S1-KEYS.                                                 ST1314.2
011400         03  S1-1 PICTURE 999.                                    ST1314.2
011500         03  S1-2 PICTURE AA.                                     ST1314.2
011600     02  FILLER PICTURE X(115).                                   ST1314.2
011700 SD  SORT2                                                        ST1314.2
011800     RECORD 120                                                   ST1314.2
011900     DATA RECORD IS S2.                                           ST1314.2
012000 01  S2.                                                          ST1314.2
012100     02  S2-KEYS.                                                 ST1314.2
012200         03  S2-1 PICTURE 999.                                    ST1314.2
012300         03  S2-2 PICTURE AA.                                     ST1314.2
012400         03  S2-3 PICTURE AA.                                     ST1314.2
012500     02  FILLER PICTURE X(113).                                   ST1314.2
012600 SD  SORT3                                                        ST1314.2
012700     RECORD 120 CHARACTERS                                        ST1314.2
012800     DATA RECORD S3.                                              ST1314.2
012900 01  S3.                                                          ST1314.2
013000     02  S3-KEYS.                                                 ST1314.2
013100         03  S3-1 PICTURE 999.                                    ST1314.2
013200         03  S3-2 PICTURE AA.                                     ST1314.2
013300         03  S3-3 PICTURE AA.                                     ST1314.2
013400         03  S3-4 PICTURE 9999.                                   ST1314.2
013500     02  FILLER PICTURE X(109).                                   ST1314.2
013600 WORKING-STORAGE SECTION.                                         ST1314.2
013700 77  SUBSCRIPT-1 PICTURE  99 COMPUTATIONAL VALUE ZERO.            ST1314.2
013800 77  C0 PICTURE  99 COMPUTATIONAL VALUE ZERO.                     ST1314.2
013900 77  C1 PICTURE  99 COMPUTATIONAL VALUE 1.                        ST1314.2
014000 77  CA PICTURE A VALUE "A".                                      ST1314.2
014100 77  CB PICTURE A VALUE "B".                                      ST1314.2
014200 01  ALPHA-TABLE.                                                 ST1314.2
014300     02  ALPHA-TAB PICTURE IS A(25) VALUE IS "ABCDEFGHIJKLMNPQRSTUST1314.2
014400-    "VWXYZ".                                                     ST1314.2
014500     02  ALPHA-TBL REDEFINES ALPHA-TAB PICTURE A OCCURS 25 TIMES. ST1314.2
014600 01  W-KEYS.                                                      ST1314.2
014700     02  W-S3-KEYS.                                               ST1314.2
014800         03  W-S2-KEYS.                                           ST1314.2
014900             04  W-S1-KEYS.                                       ST1314.2
015000                 05  S1-1W PICTURE 999 VALUE 567.                 ST1314.2
015100                 05  S1-2W.                                       ST1314.2
015200                     06  S1-2W-A PICTURE A.                       ST1314.2
015300                     06  S1-2W-B PICTURE A.                       ST1314.2
015400             04  S2-3W.                                           ST1314.2
015500                 05  S2-3W-A PICTURE A.                           ST1314.2
015600                 05  S2-3W-B PICTURE A.                           ST1314.2
015700         03  S3-4W PICTURE 9999 VALUE 7051.                       ST1314.2
015800 01  FILE-RECORD-INFORMATION-REC.                                 ST1314.2
015900     03 FILE-RECORD-INFO-SKELETON.                                ST1314.2
016000        05 FILLER                 PICTURE X(48)       VALUE       ST1314.2
016100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1314.2
016200        05 FILLER                 PICTURE X(46)       VALUE       ST1314.2
016300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1314.2
016400        05 FILLER                 PICTURE X(26)       VALUE       ST1314.2
016500             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1314.2
016600        05 FILLER                 PICTURE X(37)       VALUE       ST1314.2
016700             ",RECKEY=                             ".             ST1314.2
016800        05 FILLER                 PICTURE X(38)       VALUE       ST1314.2
016900             ",ALTKEY1=                             ".            ST1314.2
017000        05 FILLER                 PICTURE X(38)       VALUE       ST1314.2
017100             ",ALTKEY2=                             ".            ST1314.2
017200        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1314.2
017300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1314.2
017400        05 FILE-RECORD-INFO-P1-120.                               ST1314.2
017500           07 FILLER              PIC X(5).                       ST1314.2
017600           07 XFILE-NAME           PIC X(6).                      ST1314.2
017700           07 FILLER              PIC X(8).                       ST1314.2
017800           07 XRECORD-NAME         PIC X(6).                      ST1314.2
017900           07 FILLER              PIC X(1).                       ST1314.2
018000           07 REELUNIT-NUMBER     PIC 9(1).                       ST1314.2
018100           07 FILLER              PIC X(7).                       ST1314.2
018200           07 XRECORD-NUMBER       PIC 9(6).                      ST1314.2
018300           07 FILLER              PIC X(6).                       ST1314.2
018400           07 UPDATE-NUMBER       PIC 9(2).                       ST1314.2
018500           07 FILLER              PIC X(5).                       ST1314.2
018600           07 ODO-NUMBER          PIC 9(4).                       ST1314.2
018700           07 FILLER              PIC X(5).                       ST1314.2
018800           07 XPROGRAM-NAME        PIC X(5).                      ST1314.2
018900           07 FILLER              PIC X(7).                       ST1314.2
019000           07 XRECORD-LENGTH       PIC 9(6).                      ST1314.2
019100           07 FILLER              PIC X(7).                       ST1314.2
019200           07 CHARS-OR-RECORDS    PIC X(2).                       ST1314.2
019300           07 FILLER              PIC X(1).                       ST1314.2
019400           07 XBLOCK-SIZE          PIC 9(4).                      ST1314.2
019500           07 FILLER              PIC X(6).                       ST1314.2
019600           07 RECORDS-IN-FILE     PIC 9(6).                       ST1314.2
019700           07 FILLER              PIC X(5).                       ST1314.2
019800           07 XFILE-ORGANIZATION   PIC X(2).                      ST1314.2
019900           07 FILLER              PIC X(6).                       ST1314.2
020000           07 XLABEL-TYPE          PIC X(1).                      ST1314.2
020100        05 FILE-RECORD-INFO-P121-240.                             ST1314.2
020200           07 FILLER              PIC X(8).                       ST1314.2
020300           07 XRECORD-KEY          PIC X(29).                     ST1314.2
020400           07 FILLER              PIC X(9).                       ST1314.2
020500           07 ALTERNATE-KEY1      PIC X(29).                      ST1314.2
020600           07 FILLER              PIC X(9).                       ST1314.2
020700           07 ALTERNATE-KEY2      PIC X(29).                      ST1314.2
020800           07 FILLER              PIC X(7).                       ST1314.2
020900 01  TEST-RESULTS.                                                ST1314.2
021000     02 FILLER                   PIC X      VALUE SPACE.          ST1314.2
021100     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1314.2
021200     02 FILLER                   PIC X      VALUE SPACE.          ST1314.2
021300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1314.2
021400     02 FILLER                   PIC X      VALUE SPACE.          ST1314.2
021500     02  PAR-NAME.                                                ST1314.2
021600       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1314.2
021700       03  PARDOT-X              PIC X      VALUE SPACE.          ST1314.2
021800       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1314.2
021900     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1314.2
022000     02 RE-MARK                  PIC X(61).                       ST1314.2
022100 01  TEST-COMPUTED.                                               ST1314.2
022200     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1314.2
022300     02 FILLER                   PIC X(17)  VALUE                 ST1314.2
022400            "       COMPUTED=".                                   ST1314.2
022500     02 COMPUTED-X.                                               ST1314.2
022600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1314.2
022700     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1314.2
022800                                 PIC -9(9).9(9).                  ST1314.2
022900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1314.2
023000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1314.2
023100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1314.2
023200     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1314.2
023300         04 COMPUTED-18V0                    PIC -9(18).          ST1314.2
023400         04 FILLER                           PIC X.               ST1314.2
023500     03 FILLER PIC X(50) VALUE SPACE.                             ST1314.2
023600 01  TEST-CORRECT.                                                ST1314.2
023700     02 FILLER PIC X(30) VALUE SPACE.                             ST1314.2
023800     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1314.2
023900     02 CORRECT-X.                                                ST1314.2
024000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1314.2
024100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1314.2
024200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1314.2
024300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1314.2
024400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1314.2
024500     03      CR-18V0 REDEFINES CORRECT-A.                         ST1314.2
024600         04 CORRECT-18V0                     PIC -9(18).          ST1314.2
024700         04 FILLER                           PIC X.               ST1314.2
024800     03 FILLER PIC X(2) VALUE SPACE.                              ST1314.2
024900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1314.2
025000 01  CCVS-C-1.                                                    ST1314.2
025100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1314.2
025200-    "SS  PARAGRAPH-NAME                                          ST1314.2
025300-    "       REMARKS".                                            ST1314.2
025400     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1314.2
025500 01  CCVS-C-2.                                                    ST1314.2
025600     02 FILLER                     PIC X        VALUE SPACE.      ST1314.2
025700     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1314.2
025800     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1314.2
025900     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1314.2
026000     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1314.2
026100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1314.2
026200 01  REC-CT                        PIC 99       VALUE ZERO.       ST1314.2
026300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1314.2
026400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1314.2
026500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1314.2
026600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1314.2
026700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1314.2
026800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1314.2
026900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1314.2
027000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1314.2
027100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1314.2
027200 01  CCVS-H-1.                                                    ST1314.2
027300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1314.2
027400     02  FILLER                    PIC X(42)    VALUE             ST1314.2
027500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1314.2
027600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1314.2
027700 01  CCVS-H-2A.                                                   ST1314.2
027800   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1314.2
027900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1314.2
028000   02  FILLER                        PIC XXXX   VALUE             ST1314.2
028100     "4.2 ".                                                      ST1314.2
028200   02  FILLER                        PIC X(28)  VALUE             ST1314.2
028300            " COPY - NOT FOR DISTRIBUTION".                       ST1314.2
028400   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1314.2
028500                                                                  ST1314.2
028600 01  CCVS-H-2B.                                                   ST1314.2
028700   02  FILLER                        PIC X(15)  VALUE             ST1314.2
028800            "TEST RESULT OF ".                                    ST1314.2
028900   02  TEST-ID                       PIC X(9).                    ST1314.2
029000   02  FILLER                        PIC X(4)   VALUE             ST1314.2
029100            " IN ".                                               ST1314.2
029200   02  FILLER                        PIC X(12)  VALUE             ST1314.2
029300     " HIGH       ".                                              ST1314.2
029400   02  FILLER                        PIC X(22)  VALUE             ST1314.2
029500            " LEVEL VALIDATION FOR ".                             ST1314.2
029600   02  FILLER                        PIC X(58)  VALUE             ST1314.2
029700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1314.2
029800 01  CCVS-H-3.                                                    ST1314.2
029900     02  FILLER                      PIC X(34)  VALUE             ST1314.2
030000            " FOR OFFICIAL USE ONLY    ".                         ST1314.2
030100     02  FILLER                      PIC X(58)  VALUE             ST1314.2
030200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1314.2
030300     02  FILLER                      PIC X(28)  VALUE             ST1314.2
030400            "  COPYRIGHT   1985 ".                                ST1314.2
030500 01  CCVS-E-1.                                                    ST1314.2
030600     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1314.2
030700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1314.2
030800     02 ID-AGAIN                     PIC X(9).                    ST1314.2
030900     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1314.2
031000 01  CCVS-E-2.                                                    ST1314.2
031100     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1314.2
031200     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1314.2
031300     02 CCVS-E-2-2.                                               ST1314.2
031400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1314.2
031500         03 FILLER                   PIC X      VALUE SPACE.      ST1314.2
031600         03 ENDER-DESC               PIC X(44)  VALUE             ST1314.2
031700            "ERRORS ENCOUNTERED".                                 ST1314.2
031800 01  CCVS-E-3.                                                    ST1314.2
031900     02  FILLER                      PIC X(22)  VALUE             ST1314.2
032000            " FOR OFFICIAL USE ONLY".                             ST1314.2
032100     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1314.2
032200     02  FILLER                      PIC X(58)  VALUE             ST1314.2
032300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1314.2
032400     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1314.2
032500     02 FILLER                       PIC X(15)  VALUE             ST1314.2
032600             " COPYRIGHT 1985".                                   ST1314.2
032700 01  CCVS-E-4.                                                    ST1314.2
032800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1314.2
032900     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1314.2
033000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1314.2
033100     02 FILLER                       PIC X(40)  VALUE             ST1314.2
033200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1314.2
033300 01  XXINFO.                                                      ST1314.2
033400     02 FILLER                       PIC X(19)  VALUE             ST1314.2
033500            "*** INFORMATION ***".                                ST1314.2
033600     02 INFO-TEXT.                                                ST1314.2
033700       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1314.2
033800       04 XXCOMPUTED                 PIC X(20).                   ST1314.2
033900       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1314.2
034000       04 XXCORRECT                  PIC X(20).                   ST1314.2
034100     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1314.2
034200 01  HYPHEN-LINE.                                                 ST1314.2
034300     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1314.2
034400     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1314.2
034500-    "*****************************************".                 ST1314.2
034600     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1314.2
034700-    "******************************".                            ST1314.2
034800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1314.2
034900     "ST131A".                                                    ST1314.2
035000 PROCEDURE DIVISION.                                              ST1314.2
035100 CCVS1 SECTION.                                                   ST1314.2
035200 OPEN-FILES.                                                      ST1314.2
035300     OPEN     OUTPUT PRINT-FILE.                                  ST1314.2
035400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1314.2
035500     MOVE    SPACE TO TEST-RESULTS.                               ST1314.2
035600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1314.2
035700     GO TO CCVS1-EXIT.                                            ST1314.2
035800 CLOSE-FILES.                                                     ST1314.2
035900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1314.2
036000 TERMINATE-CCVS.                                                  ST1314.2
036100*    EXIT PROGRAM.                                                ST1314.2
036200*TERMINATE-CALL.                                                  ST1314.2
036300     STOP     RUN.                                                ST1314.2
036400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1314.2
036500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1314.2
036600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1314.2
036700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1314.2
036800     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1314.2
036900 PRINT-DETAIL.                                                    ST1314.2
037000     IF REC-CT NOT EQUAL TO ZERO                                  ST1314.2
037100             MOVE "." TO PARDOT-X                                 ST1314.2
037200             MOVE REC-CT TO DOTVALUE.                             ST1314.2
037300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1314.2
037400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1314.2
037500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1314.2
037600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1314.2
037700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1314.2
037800     MOVE SPACE TO CORRECT-X.                                     ST1314.2
037900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1314.2
038000     MOVE     SPACE TO RE-MARK.                                   ST1314.2
038100 HEAD-ROUTINE.                                                    ST1314.2
038200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1314.2
038300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1314.2
038400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1314.2
038500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1314.2
038600 COLUMN-NAMES-ROUTINE.                                            ST1314.2
038700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1314.2
038800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1314.2
038900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1314.2
039000 END-ROUTINE.                                                     ST1314.2
039100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1314.2
039200 END-RTN-EXIT.                                                    ST1314.2
039300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1314.2
039400 END-ROUTINE-1.                                                   ST1314.2
039500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1314.2
039600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1314.2
039700      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1314.2
039800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1314.2
039900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1314.2
040000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1314.2
040100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1314.2
040200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1314.2
040300  END-ROUTINE-12.                                                 ST1314.2
040400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1314.2
040500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1314.2
040600         MOVE "NO " TO ERROR-TOTAL                                ST1314.2
040700         ELSE                                                     ST1314.2
040800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1314.2
040900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1314.2
041000     PERFORM WRITE-LINE.                                          ST1314.2
041100 END-ROUTINE-13.                                                  ST1314.2
041200     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1314.2
041300         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1314.2
041400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1314.2
041500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1314.2
041600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1314.2
041700      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1314.2
041800          MOVE "NO " TO ERROR-TOTAL                               ST1314.2
041900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1314.2
042000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1314.2
042100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1314.2
042200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1314.2
042300 WRITE-LINE.                                                      ST1314.2
042400     ADD 1 TO RECORD-COUNT.                                       ST1314.2
042500*    IF RECORD-COUNT GREATER 42                                   ST1314.2
042600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1314.2
042700*        MOVE SPACE TO DUMMY-RECORD                               ST1314.2
042800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1314.2
042900*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1314.2
043000*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1314.2
043100*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1314.2
043200*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1314.2
043300*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1314.2
043400*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1314.2
043500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1314.2
043600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1314.2
043700*        MOVE ZERO TO RECORD-COUNT.                               ST1314.2
043800     PERFORM WRT-LN.                                              ST1314.2
043900 WRT-LN.                                                          ST1314.2
044000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1314.2
044100     MOVE SPACE TO DUMMY-RECORD.                                  ST1314.2
044200 BLANK-LINE-PRINT.                                                ST1314.2
044300     PERFORM WRT-LN.                                              ST1314.2
044400 FAIL-ROUTINE.                                                    ST1314.2
044500     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1314.2
044600            GO TO   FAIL-ROUTINE-WRITE.                           ST1314.2
044700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1314.2
044800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1314.2
044900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1314.2
045000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1314.2
045100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1314.2
045200     GO TO  FAIL-ROUTINE-EX.                                      ST1314.2
045300 FAIL-ROUTINE-WRITE.                                              ST1314.2
045400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1314.2
045500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1314.2
045600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1314.2
045700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1314.2
045800 FAIL-ROUTINE-EX. EXIT.                                           ST1314.2
045900 BAIL-OUT.                                                        ST1314.2
046000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1314.2
046100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1314.2
046200 BAIL-OUT-WRITE.                                                  ST1314.2
046300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1314.2
046400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1314.2
046500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1314.2
046600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1314.2
046700 BAIL-OUT-EX. EXIT.                                               ST1314.2
046800 CCVS1-EXIT.                                                      ST1314.2
046900     EXIT.                                                        ST1314.2
047000 P1-CREATE-F1.                                                    ST1314.2
047100     OPEN    OUTPUT FILE1.                                        ST1314.2
047200     MOVE CA TO S1-2W-A.                                          ST1314.2
047300     MOVE CB TO S2-3W-A.                                          ST1314.2
047400 P2-CREATE-F1.                                                    ST1314.2
047500     PERFORM P4-CREATE-F1 2 TIMES.                                ST1314.2
047600 P3-CREATE-F1.                                                    ST1314.2
047700     MOVE CA TO S2-3W-A.                                          ST1314.2
047800     PERFORM P4-CREATE-F1 2 TIMES.                                ST1314.2
047900     CLOSE FILE1.                                                 ST1314.2
048000     GO TO FIRST-SORT.                                            ST1314.2
048100 P4-CREATE-F1.                                                    ST1314.2
048200     MOVE C0 TO SUBSCRIPT-1.                                      ST1314.2
048300     PERFORM P5-CREATE-F1 25 TIMES.                               ST1314.2
048400 P5-CREATE-F1.                                                    ST1314.2
048500     ADD C1 TO SUBSCRIPT-1.                                       ST1314.2
048600     SUBTRACT C1 FROM S3-4W.                                      ST1314.2
048700     MOVE ALPHA-TBL (SUBSCRIPT-1) TO S1-2W-B S2-3W-B.             ST1314.2
048800     MOVE W-S3-KEYS TO R1.                                        ST1314.2
048900     WRITE R1.                                                    ST1314.2
049000 F1-NOTE.                                                         ST1314.2
049100*    NOTE.                                                        ST1314.2
049200*      KEY-1 WILL BE 567 IN ALL RECORDS.                          ST1314.2
049300*      KEY-2 WILL BE >A> IN FIRST LETTER WITH 4 OCCURRENCES OF THEST1314.2
049400*        ALPHABET IN THE SECOND LETTER.                           ST1314.2
049500*      KEY-3 WILL BE >A> OR >B> IN FIRST LETTER WITH 2 OCCURRENCESST1314.2
049600*        OF THE ALPHABET FOR EACH IN THE SECOND LETTER.           ST1314.2
049700*      KEY-4 WILL VARY FROM 7050 THRU 6951.                       ST1314.2
049800*      THE LETTER "O" HAS BEEN OMITTED.                           ST1314.2
049900 SRT-1 SECTION.                                                   ST1314.2
050000 FIRST-SORT.                                                      ST1314.2
050100     SORT SORT1                                                   ST1314.2
050200         ON DESCENDING KEY S1-1                                   ST1314.2
050300         ON ASCENDING KEY S1-2                                    ST1314.2
050400         USING FILE1                                              ST1314.2
050500         GIVING FILE2.                                            ST1314.2
050600*    NOTE SORT STATEMENT WITH ALL OPTIONAL WORDS.                 ST1314.2
050700*    NOTE OUTPUT WILL BE TESTED IN THE FOLLOWING INPUT PROCEDURE. ST1314.2
050800 SRT-2 SECTION.                                                   ST1314.2
050900 SECOND-SORT.                                                     ST1314.2
051000     SORT SORT2                                                   ST1314.2
051100         ASCENDING S2-1                                           ST1314.2
051200         DESCENDING S2-2                                          ST1314.2
051300         ASCENDING S2-3                                           ST1314.2
051400         INPUT PROCEDURE SRT-2-INPUT                              ST1314.2
051500         GIVING FILE3.                                            ST1314.2
051600*      NOTE SORT STATEMENT WITH ALL OPTIONAL WORDS OMITTED.       ST1314.2
051700     GO TO SRT-3.                                                 ST1314.2
051800 SRT-2-INPUT SECTION.                                             ST1314.2
051900 OPEN-1.                                                          ST1314.2
052000     OPEN     INPUT FILE2.                                        ST1314.2
052100     MOVE     "SORT, INPUT PROC" TO FEATURE.                      ST1314.2
052200 SORT-TEST-1.                                                     ST1314.2
052300     PERFORM  READ-RELEASE-FILE2.                                 ST1314.2
052400     IF       W-S1-KEYS EQUAL TO "567AA"                          ST1314.2
052500              PERFORM PASS-1 GO TO SORT-WRITE-1.                  ST1314.2
052600     GO       TO SORT-FAIL-1.                                     ST1314.2
052700 SORT-DELETE-1.                                                   ST1314.2
052800     PERFORM  DE-LETE-1.                                          ST1314.2
052900     GO       TO SORT-WRITE-1.                                    ST1314.2
053000 SORT-FAIL-1.                                                     ST1314.2
053100     MOVE     W-S1-KEYS TO COMPUTED-A.                            ST1314.2
053200     MOVE     "567AA" TO CORRECT-A.                               ST1314.2
053300     PERFORM  FAIL-1.                                             ST1314.2
053400 SORT-WRITE-1.                                                    ST1314.2
053500     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1314.2
053600     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
053700 SORT-TEST-2.                                                     ST1314.2
053800     PERFORM  READ-RELEASE-FILE2 35 TIMES.                        ST1314.2
053900     IF       W-S1-KEYS EQUAL TO "567AI"                          ST1314.2
054000              PERFORM PASS-1 GO TO SORT-WRITE-2.                  ST1314.2
054100     GO       TO SORT-FAIL-2.                                     ST1314.2
054200 SORT-DELETE-2.                                                   ST1314.2
054300     PERFORM  DE-LETE-1.                                          ST1314.2
054400     GO       TO SORT-WRITE-2.                                    ST1314.2
054500 SORT-FAIL-2.                                                     ST1314.2
054600     MOVE     W-S1-KEYS TO COMPUTED-A.                            ST1314.2
054700     MOVE     "567AI" TO CORRECT-A.                               ST1314.2
054800     PERFORM  FAIL-1.                                             ST1314.2
054900 SORT-WRITE-2.                                                    ST1314.2
055000     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         ST1314.2
055100     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
055200 SORT-TEST-3.                                                     ST1314.2
055300     PERFORM  READ-RELEASE-FILE2 35 TIMES.                        ST1314.2
055400     IF       W-S1-KEYS EQUAL TO "567AS"                          ST1314.2
055500              PERFORM PASS-1 GO TO SORT-WRITE-3.                  ST1314.2
055600     GO       TO SORT-FAIL-3.                                     ST1314.2
055700 SORT-DELETE-3.                                                   ST1314.2
055800     PERFORM  DE-LETE-1.                                          ST1314.2
055900     GO       TO SORT-WRITE-3.                                    ST1314.2
056000 SORT-FAIL-3.                                                     ST1314.2
056100     MOVE     W-S1-KEYS TO COMPUTED-A.                            ST1314.2
056200     MOVE     "567AS" TO CORRECT-A.                               ST1314.2
056300     PERFORM  FAIL-1.                                             ST1314.2
056400 SORT-WRITE-3.                                                    ST1314.2
056500     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         ST1314.2
056600     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
056700 SORT-TEST-4.                                                     ST1314.2
056800     PERFORM  READ-RELEASE-FILE2 29 TIMES.                        ST1314.2
056900     IF       W-S1-KEYS EQUAL TO "567AZ"                          ST1314.2
057000              PERFORM PASS-1 GO TO SORT-WRITE-4.                  ST1314.2
057100     GO       TO SORT-FAIL-4.                                     ST1314.2
057200 SORT-DELETE-4.                                                   ST1314.2
057300     PERFORM  DE-LETE-1.                                          ST1314.2
057400     GO       TO SORT-WRITE-4.                                    ST1314.2
057500 SORT-FAIL-4.                                                     ST1314.2
057600     MOVE     W-S1-KEYS TO COMPUTED-A.                            ST1314.2
057700     MOVE     "567AZ" TO CORRECT-A.                               ST1314.2
057800     PERFORM  FAIL-1.                                             ST1314.2
057900 SORT-WRITE-4.                                                    ST1314.2
058000     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         ST1314.2
058100     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
058200 CLOSE-1.                                                         ST1314.2
058300     CLOSE    FILE2.                                              ST1314.2
058400     GO       TO EXIT-1.                                          ST1314.2
058500 READ-RELEASE-FILE2.                                              ST1314.2
058600     READ     FILE2 AT END GO TO TERMINAL-1.                      ST1314.2
058700     MOVE     R2 TO W-S3-KEYS.                                    ST1314.2
058800     RELEASE  S2 FROM R2.                                         ST1314.2
058900 TERMINAL-1.                                                      ST1314.2
059000     PERFORM  FAIL-1.                                             ST1314.2
059100     MOVE     "TERMINAL-1" TO PAR-NAME.                           ST1314.2
059200     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1314.2
059300     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
059400     MOVE     SPACE TO FEATURE.                                   ST1314.2
059500     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              ST1314.2
059600     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
059700     MOVE     "LAST SUCCESSFUL TEST." TO RE-MARK.                 ST1314.2
059800     PERFORM  PRINT-DETAIL-1.                                     ST1314.2
059900     GO       TO CLOSE-1.                                         ST1314.2
060000 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1314.2
060100 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1314.2
060200 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1314.2
060300 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1314.2
060400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1314.2
060500 PRINT-DETAIL-1.                                                  ST1314.2
060600     IF REC-CT NOT EQUAL TO ZERO                                  ST1314.2
060700             MOVE "." TO PARDOT-X                                 ST1314.2
060800             MOVE REC-CT TO DOTVALUE.                             ST1314.2
060900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    ST1314.2
061000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             ST1314.2
061100        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             ST1314.2
061200          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             ST1314.2
061300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1314.2
061400     MOVE SPACE TO CORRECT-X.                                     ST1314.2
061500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1314.2
061600     MOVE     SPACE TO RE-MARK.                                   ST1314.2
061700 WRITE-LINE-1.                                                    ST1314.2
061800     ADD 1 TO RECORD-COUNT.                                       ST1314.2
061900*    IF RECORD-COUNT GREATER 50                                   ST1314.2
062000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1314.2
062100*        MOVE SPACE TO DUMMY-RECORD                               ST1314.2
062200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1314.2
062300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           ST1314.2
062400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   ST1314.2
062500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        ST1314.2
062600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1314.2
062700*        MOVE ZERO TO RECORD-COUNT.                               ST1314.2
062800     PERFORM WRT-LN-1.                                            ST1314.2
062900 WRT-LN-1.                                                        ST1314.2
063000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1314.2
063100     MOVE SPACE TO DUMMY-RECORD.                                  ST1314.2
063200 BLANK-LINE-PRINT-1.                                              ST1314.2
063300     PERFORM WRT-LN-1.                                            ST1314.2
063400 FAIL-ROUTINE-1.                                                  ST1314.2
063500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     ST1314.2
063600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      ST1314.2
063700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1314.2
063800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1314.2
063900     GO TO FAIL-ROUTINE-EX-1.                                     ST1314.2
064000 FAIL-RTN-WRITE-1.                                                ST1314.2
064100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         ST1314.2
064200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. ST1314.2
064300 FAIL-ROUTINE-EX-1. EXIT.                                         ST1314.2
064400 BAIL-OUT-1.                                                      ST1314.2
064500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     ST1314.2
064600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             ST1314.2
064700 BAIL-OUT-WRITE-1.                                                ST1314.2
064800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1314.2
064900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1314.2
065000 BAIL-OUT-EX-1. EXIT.                                             ST1314.2
065100 EXIT-1.                                                          ST1314.2
065200     EXIT.                                                        ST1314.2
065300 SRT-3 SECTION.                                                   ST1314.2
065400 THIRD-SORT.                                                      ST1314.2
065500     SORT SORT3                                                   ST1314.2
065600         ON DESCENDING KEY S3-1 S3-2 S3-3                         ST1314.2
065700         ASCENDING S3-4                                           ST1314.2
065800       INPUT PROCEDURE IS SRT3-INPUT                              ST1314.2
065900       OUTPUT PROCEDURE SRT3-OUTPUT-1 THRU SRT3-OUTPUT-2.         ST1314.2
066000 NOTE-SORT-3.                                                     ST1314.2
066100*    NOTE SORT STATEMENT WITH INCLUSION-OMISSION OF OPTIONAL      ST1314.2
066200*        WORDS AND THRU OPTION. THE OUTPUT OF SRT-2 IS TESTED     ST1314.2
066300*        IN THE INPUT PROCEDURE OF THIS (THIRD) SORT.  THE OUTPUT ST1314.2
066400*        OF THE THIRD SORT IS TESTED IN THE OUTPUT PROCEDURE      ST1314.2
066500*        WITHOUT THE GENERATION OF AN OUTPUT FILE.                ST1314.2
066600 END-FIRST-PROGRAM.                                               ST1314.2
066700     GO TO CCVS-EXIT.                                             ST1314.2
066800 SRT3-INPUT SECTION.                                              ST1314.2
066900 OPEN-2.                                                          ST1314.2
067000     OPEN     INPUT FILE3.                                        ST1314.2
067100     MOVE     "SORT, INPUT PROC" TO FEATURE.                      ST1314.2
067200 SORT-TEST-5.                                                     ST1314.2
067300     PERFORM  READ-RELEASE-FILE3.                                 ST1314.2
067400     MOVE     R3-KEYS TO W-S3-KEYS.                               ST1314.2
067500     IF       W-S2-KEYS EQUAL TO "567AZAZ"                        ST1314.2
067600              PERFORM PASS-2 GO TO SORT-WRITE-5.                  ST1314.2
067700     GO       TO SORT-FAIL-5.                                     ST1314.2
067800 SORT-DELETE-5.                                                   ST1314.2
067900     PERFORM  DE-LETE-2.                                          ST1314.2
068000     GO       TO SORT-WRITE-5.                                    ST1314.2
068100 SORT-FAIL-5.                                                     ST1314.2
068200     MOVE     W-S2-KEYS TO COMPUTED-A.                            ST1314.2
068300     MOVE     "567AZAZ" TO CORRECT-A.                             ST1314.2
068400     PERFORM  FAIL-2.                                             ST1314.2
068500 SORT-WRITE-5.                                                    ST1314.2
068600     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         ST1314.2
068700     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
068800 SORT-TEST-6.                                                     ST1314.2
068900     PERFORM  READ-RELEASE-FILE3 35 TIMES.                        ST1314.2
069000     MOVE     R3-KEYS TO W-S3-KEYS.                               ST1314.2
069100     IF       W-S2-KEYS EQUAL TO "567ARBR"                        ST1314.2
069200              PERFORM PASS-2 GO TO SORT-WRITE-6.                  ST1314.2
069300     GO       TO SORT-FAIL-6.                                     ST1314.2
069400 SORT-DELETE-6.                                                   ST1314.2
069500     PERFORM  DE-LETE-2.                                          ST1314.2
069600     GO       TO SORT-WRITE-6.                                    ST1314.2
069700 SORT-FAIL-6.                                                     ST1314.2
069800     MOVE     W-S2-KEYS TO COMPUTED-A.                            ST1314.2
069900     MOVE     "567ARBR" TO CORRECT-A.                             ST1314.2
070000     PERFORM  FAIL-2.                                             ST1314.2
070100 SORT-WRITE-6.                                                    ST1314.2
070200     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1314.2
070300     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
070400 SORT-TEST-7.                                                     ST1314.2
070500     PERFORM  READ-RELEASE-FILE3 35 TIMES.                        ST1314.2
070600     MOVE     R3-KEYS TO W-S3-KEYS.                               ST1314.2
070700     IF       W-S2-KEYS EQUAL TO "567AHBH"                        ST1314.2
070800              PERFORM PASS-2 GO TO SORT-WRITE-7.                  ST1314.2
070900     GO       TO SORT-FAIL-7.                                     ST1314.2
071000 SORT-DELETE-7.                                                   ST1314.2
071100     PERFORM  DE-LETE-2.                                          ST1314.2
071200     GO       TO SORT-WRITE-7.                                    ST1314.2
071300 SORT-FAIL-7.                                                     ST1314.2
071400     MOVE     W-S2-KEYS TO COMPUTED-A.                            ST1314.2
071500     MOVE     "567AHBH" TO CORRECT-A.                             ST1314.2
071600     PERFORM  FAIL-2.                                             ST1314.2
071700 SORT-WRITE-7.                                                    ST1314.2
071800     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1314.2
071900     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
072000 SORT-TEST-8.                                                     ST1314.2
072100     PERFORM  READ-RELEASE-FILE3 29 TIMES.                        ST1314.2
072200     MOVE     R3-KEYS TO W-S3-KEYS.                               ST1314.2
072300     IF       W-S2-KEYS EQUAL TO "567AABA"                        ST1314.2
072400              PERFORM PASS-2 GO TO SORT-WRITE-8.                  ST1314.2
072500     GO       TO SORT-FAIL-8.                                     ST1314.2
072600 SORT-DELETE-8.                                                   ST1314.2
072700     PERFORM  DE-LETE-2.                                          ST1314.2
072800     GO       TO SORT-WRITE-8.                                    ST1314.2
072900 SORT-FAIL-8.                                                     ST1314.2
073000     MOVE     W-S2-KEYS TO COMPUTED-A.                            ST1314.2
073100     MOVE     "567AABA" TO CORRECT-A.                             ST1314.2
073200     PERFORM  FAIL-2.                                             ST1314.2
073300 SORT-WRITE-8.                                                    ST1314.2
073400     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1314.2
073500     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
073600 CLOSE-2.                                                         ST1314.2
073700     CLOSE    FILE3.                                              ST1314.2
073800     GO       TO EXIT-2.                                          ST1314.2
073900 READ-RELEASE-FILE3.                                              ST1314.2
074000     READ     FILE3 AT END GO TO TERMINAL-2.                      ST1314.2
074100     RELEASE  S3.                                                 ST1314.2
074200*    NOTE    READ AND RELEASE ARE THE ONLY STATEMENTS NECESSARY   ST1314.2
074300*    TO USE FILE3 AS INPUT TO THE THIRD SORT.  THIS IS SINCE      ST1314.2
074400*    THE RECORD AREAS ARE THE SAME FROM THE CLAUSE                ST1314.2
074500*            SAME RECORD AREA SORT3 FILE3.                        ST1314.2
074600 TERMINAL-2.                                                      ST1314.2
074700     PERFORM  FAIL-2.                                             ST1314.2
074800     MOVE     "TERMINAL-2" TO PAR-NAME.                           ST1314.2
074900     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1314.2
075000     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
075100     MOVE     SPACE TO FEATURE.                                   ST1314.2
075200     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              ST1314.2
075300     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
075400     MOVE     "LAST SUCCESSFUL TEST" TO RE-MARK.                  ST1314.2
075500     PERFORM  PRINT-DETAIL-2.                                     ST1314.2
075600     GO       TO CLOSE-2.                                         ST1314.2
075700 INSPT-2. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1314.2
075800 PASS-2.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1314.2
075900 FAIL-2.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1314.2
076000 DE-LETE-2.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1314.2
076100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1314.2
076200 PRINT-DETAIL-2.                                                  ST1314.2
076300     IF REC-CT NOT EQUAL TO ZERO                                  ST1314.2
076400             MOVE "." TO PARDOT-X                                 ST1314.2
076500             MOVE REC-CT TO DOTVALUE.                             ST1314.2
076600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-2.    ST1314.2
076700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-2             ST1314.2
076800        PERFORM FAIL-ROUTINE-2 THRU FAIL-ROUTINE-EX-2             ST1314.2
076900          ELSE PERFORM BAIL-OUT-2 THRU BAIL-OUT-EX-2.             ST1314.2
077000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1314.2
077100     MOVE SPACE TO CORRECT-X.                                     ST1314.2
077200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1314.2
077300     MOVE     SPACE TO RE-MARK.                                   ST1314.2
077400 WRITE-LINE-2.                                                    ST1314.2
077500     ADD 1 TO RECORD-COUNT.                                       ST1314.2
077600*    IF RECORD-COUNT GREATER 50                                   ST1314.2
077700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1314.2
077800*        MOVE SPACE TO DUMMY-RECORD                               ST1314.2
077900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1314.2
078000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-2           ST1314.2
078100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-2 2 TIMES   ST1314.2
078200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-2        ST1314.2
078300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1314.2
078400*        MOVE ZERO TO RECORD-COUNT.                               ST1314.2
078500     PERFORM WRT-LN-2.                                            ST1314.2
078600 WRT-LN-2.                                                        ST1314.2
078700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1314.2
078800     MOVE SPACE TO DUMMY-RECORD.                                  ST1314.2
078900 BLANK-LINE-PRINT-2.                                              ST1314.2
079000     PERFORM WRT-LN-2.                                            ST1314.2
079100 FAIL-ROUTINE-2.                                                  ST1314.2
079200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.     ST1314.2
079300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.      ST1314.2
079400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1314.2
079500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   ST1314.2
079600     GO TO FAIL-ROUTINE-EX-2.                                     ST1314.2
079700 FAIL-RTN-WRITE-2.                                                ST1314.2
079800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-2         ST1314.2
079900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-2 2 TIMES. ST1314.2
080000 FAIL-ROUTINE-EX-2. EXIT.                                         ST1314.2
080100 BAIL-OUT-2.                                                      ST1314.2
080200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-2.     ST1314.2
080300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-2.             ST1314.2
080400 BAIL-OUT-WRITE-2.                                                ST1314.2
080500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1314.2
080600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   ST1314.2
080700 BAIL-OUT-EX-2. EXIT.                                             ST1314.2
080800 EXIT-2.                                                          ST1314.2
080900     EXIT.                                                        ST1314.2
081000 SRT3-OUTPUT-1 SECTION.                                           ST1314.2
081100 INIT-3.                                                          ST1314.2
081200     MOVE     "SORT, OUTPUT PROC" TO FEATURE.                     ST1314.2
081300 SORT-TEST-9.                                                     ST1314.2
081400     PERFORM  RETURN-SORT3.                                       ST1314.2
081500     IF       S3-KEYS EQUAL TO "567AZBZ7001"                      ST1314.2
081600              PERFORM PASS-3 GO TO SORT-WRITE-9.                  ST1314.2
081700     GO       TO SORT-FAIL-9.                                     ST1314.2
081800 SORT-DELETE-9.                                                   ST1314.2
081900     PERFORM  DE-LETE-3.                                          ST1314.2
082000     GO       TO SORT-WRITE-9.                                    ST1314.2
082100 SORT-FAIL-9.                                                     ST1314.2
082200     MOVE     S3-KEYS TO COMPUTED-A.                              ST1314.2
082300     MOVE     "567AZBZ7001" TO CORRECT-A.                         ST1314.2
082400     PERFORM  FAIL-3.                                             ST1314.2
082500 SORT-WRITE-9.                                                    ST1314.2
082600     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1314.2
082700     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
082800 SORT-TEST-10.                                                    ST1314.2
082900     PERFORM  RETURN-SORT3.                                       ST1314.2
083000     IF       S3-KEYS EQUAL TO "567AZBZ7026"                      ST1314.2
083100              PERFORM PASS-3 GO TO SORT-WRITE-10.                 ST1314.2
083200     GO       TO SORT-FAIL-10.                                    ST1314.2
083300 SORT-DELETE-10.                                                  ST1314.2
083400     PERFORM  DE-LETE-3.                                          ST1314.2
083500     GO       TO SORT-WRITE-10.                                   ST1314.2
083600 SORT-FAIL-10.                                                    ST1314.2
083700     MOVE     S3-KEYS TO COMPUTED-A.                              ST1314.2
083800     MOVE     "567AZBZ7026" TO CORRECT-A.                         ST1314.2
083900     PERFORM  FAIL-3.                                             ST1314.2
084000 SORT-WRITE-10.                                                   ST1314.2
084100     MOVE     "SORT-TEST-10" TO PAR-NAME.                         ST1314.2
084200     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
084300 SORT-TEST-11.                                                    ST1314.2
084400     PERFORM  RETURN-SORT3 35 TIMES.                              ST1314.2
084500     IF       S3-KEYS EQUAL TO "567AQBQ7010"                      ST1314.2
084600              PERFORM PASS-3 GO TO SORT-WRITE-11.                 ST1314.2
084700     GO       TO SORT-FAIL-11.                                    ST1314.2
084800 SORT-DELETE-11.                                                  ST1314.2
084900     PERFORM  DE-LETE-3.                                          ST1314.2
085000     GO       TO SORT-WRITE-11.                                   ST1314.2
085100 SORT-FAIL-11.                                                    ST1314.2
085200     MOVE     S3-KEYS TO COMPUTED-A.                              ST1314.2
085300     MOVE     "567AQBQ7010" TO CORRECT-A.                         ST1314.2
085400     PERFORM  FAIL-3.                                             ST1314.2
085500 SORT-WRITE-11.                                                   ST1314.2
085600     MOVE     "SORT-TEST-11" TO PAR-NAME.                         ST1314.2
085700     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
085800 SORT-TEST-12.                                                    ST1314.2
085900     PERFORM  RETURN-SORT3.                                       ST1314.2
086000     IF       S3-KEYS EQUAL TO "567AQBQ7035"                      ST1314.2
086100              PERFORM PASS-3 GO TO SORT-WRITE-12.                 ST1314.2
086200     GO       TO SORT-FAIL-12.                                    ST1314.2
086300 SORT-DELETE-12.                                                  ST1314.2
086400     PERFORM  DE-LETE-3.                                          ST1314.2
086500     GO       TO SORT-WRITE-12.                                   ST1314.2
086600 SORT-FAIL-12.                                                    ST1314.2
086700     MOVE     S3-KEYS TO COMPUTED-A.                              ST1314.2
086800     MOVE     "567AQBQ7035" TO CORRECT-A.                         ST1314.2
086900     PERFORM  FAIL-3.                                             ST1314.2
087000 SORT-WRITE-12.                                                   ST1314.2
087100     MOVE     "SORT-TEST-12" TO PAR-NAME.                         ST1314.2
087200     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
087300 SORT-TEST-13.                                                    ST1314.2
087400     PERFORM  RETURN-SORT3 35 TIMES.                              ST1314.2
087500     IF       S3-KEYS EQUAL TO "567AGBG7019"                      ST1314.2
087600              PERFORM PASS-3 GO TO SORT-WRITE-13.                 ST1314.2
087700     GO       TO SORT-FAIL-13.                                    ST1314.2
087800 SORT-DELETE-13.                                                  ST1314.2
087900     PERFORM  DE-LETE-3.                                          ST1314.2
088000     GO       TO SORT-WRITE-13.                                   ST1314.2
088100 SORT-FAIL-13.                                                    ST1314.2
088200     MOVE     S3-KEYS TO COMPUTED-A.                              ST1314.2
088300     MOVE     "567AGBG7019" TO CORRECT-A.                         ST1314.2
088400     PERFORM  FAIL-3.                                             ST1314.2
088500 SORT-WRITE-13.                                                   ST1314.2
088600     MOVE     "SORT-TEST-13" TO PAR-NAME.                         ST1314.2
088700     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
088800 SORT-TEST-14.                                                    ST1314.2
088900     PERFORM  RETURN-SORT3 27 TIMES.                              ST1314.2
089000     IF       S3-KEYS EQUAL TO "567AAAA7000"                      ST1314.2
089100              PERFORM PASS-3 GO TO SORT-WRITE-14.                 ST1314.2
089200     GO       TO SORT-FAIL-14.                                    ST1314.2
089300 SORT-DELETE-14.                                                  ST1314.2
089400     PERFORM  DE-LETE-3.                                          ST1314.2
089500     GO       TO SORT-WRITE-14.                                   ST1314.2
089600 SORT-FAIL-14.                                                    ST1314.2
089700     MOVE     S3-KEYS TO COMPUTED-A.                              ST1314.2
089800     MOVE     "567AAAA7000" TO CORRECT-A.                         ST1314.2
089900     PERFORM  FAIL-3.                                             ST1314.2
090000 SORT-WRITE-14.                                                   ST1314.2
090100     MOVE     "SORT-TEST-14" TO PAR-NAME.                         ST1314.2
090200     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
090300 SORT-TEST-15.                                                    ST1314.2
090400     RETURN   SORT3 RECORD AT END                                 ST1314.2
090500              PERFORM PASS-3 GO TO SORT-WRITE-15.                 ST1314.2
090600*    NOTE     THE FOLLOWING SENTENCES SHOULD NOT BE EXECUTED.     ST1314.2
090700     PERFORM FAIL-3.                                              ST1314.2
090800     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1314.2
090900     GO       TO SORT-WRITE-15.                                   ST1314.2
091000 SORT-DELETE-15.                                                  ST1314.2
091100     PERFORM  DE-LETE-3.                                          ST1314.2
091200 SORT-WRITE-15.                                                   ST1314.2
091300     MOVE     "SORT-TEST-15" TO PAR-NAME.                         ST1314.2
091400     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
091500 CLOSE-3.                                                         ST1314.2
091600     GO       TO EXIT-3.                                          ST1314.2
091700 SRT3-OUTPUT-2 SECTION.                                           ST1314.2
091800 RETURN-SORT3.                                                    ST1314.2
091900     RETURN   SORT3 RECORD AT END GO TO TERMINAL-3.               ST1314.2
092000*    NOTE     RETURN VERB WITH ALL OPTIONS EXCEPT INTO.           ST1314.2
092100 TERMINAL-3.                                                      ST1314.2
092200     PERFORM  FAIL-3.                                             ST1314.2
092300     MOVE     "TERMINAL-3" TO PAR-NAME.                           ST1314.2
092400     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1314.2
092500     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
092600     MOVE     SPACE TO FEATURE.                                   ST1314.2
092700     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              ST1314.2
092800     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
092900     MOVE     "LAST SUCCESSFUL TEST." TO RE-MARK                  ST1314.2
093000     PERFORM  PRINT-DETAIL-3.                                     ST1314.2
093100     GO       TO CLOSE-3.                                         ST1314.2
093200 PASS-3.                                                          ST1314.2
093300     MOVE "PASS" TO P-OR-F.  ADD 1 TO PASS-COUNTER.               ST1314.2
093400 FAIL-3.                                                          ST1314.2
093500     ADD      1 TO ERROR-COUNTER.                                 ST1314.2
093600     MOVE "FAIL*" TO P-OR-F.                                      ST1314.2
093700 DE-LETE-3.                                                       ST1314.2
093800     MOVE     SPACE TO P-OR-F.                                    ST1314.2
093900     MOVE     "    ************    " TO COMPUTED-A.               ST1314.2
094000     MOVE     "    ************    " TO CORRECT-A.                ST1314.2
094100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1314.2
094200     ADD 1 TO DELETE-COUNTER.                                     ST1314.2
094300 PRINT-DETAIL-3.                                                  ST1314.2
094400     IF REC-CT NOT EQUAL TO ZERO                                  ST1314.2
094500             MOVE "." TO PARDOT-X                                 ST1314.2
094600             MOVE REC-CT TO DOTVALUE.                             ST1314.2
094700     MOVE     TEST-RESULTS TO PRINT-REC.                          ST1314.2
094800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1314.2
094900     MOVE     SPACE TO P-OR-F.                                    ST1314.2
095000     MOVE     SPACE TO COMPUTED-A.                                ST1314.2
095100     MOVE SPACE TO CORRECT-A.                                     ST1314.2
095200     IF     REC-CT EQUAL TO ZERO                                  ST1314.2
095300              MOVE SPACE TO PAR-NAME.                             ST1314.2
095400     MOVE     SPACE TO RE-MARK.                                   ST1314.2
095500 EXIT-3.                                                          ST1314.2
095600     EXIT.                                                        ST1314.2
095700 END-CCVS SECTION.                                                ST1314.2
095800 CCVS-EXIT SECTION.                                               ST1314.2
095900 CCVS-999999.                                                     ST1314.2
096000     GO TO CLOSE-FILES.                                           ST1314.2
      *END-OF,ST131A                                                                  
