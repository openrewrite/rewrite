      *HEADER,COBOL,SQ131A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1314.2
000200 PROGRAM-ID.                                                      SQ1314.2
000300     SQ131A.                                                      SQ1314.2
000400****************************************************************  SQ1314.2
000500*                                                              *  SQ1314.2
000600*    VALIDATION FOR:-                                          *  SQ1314.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1314.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1314.2
000900*                             REVISED 1986, AUGUST             *  SQ1314.2
001000*                                                              *  SQ1314.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1314.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1314.2
001300*                                                              *  SQ1314.2
001400****************************************************************  SQ1314.2
001500*                                                              *  SQ1314.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1314.2
001700*                                                              *  SQ1314.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE.              *  SQ1314.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1314.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1314.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1314.2
002200*                                                              *  SQ1314.2
002300*                                                              *  SQ1314.2
002400****************************************************************  SQ1314.2
002500*                                                              *  SQ1314.2
002600*    SQ131A ATTEMPTS TO OPEN IN THE I-O MODE A MASS STORAGE    *  SQ1314.2
002700*    FILE WHICH IS ALREADY OPEN IN THE OUTPUT MODE.  THIS      *  SQ1314.2
002800*    SHOULD RESULT IN RECOGNITION OF A LOGIC ERROR CONDITION   *  SQ1314.2
002900*    AND RETURN OF I-O STATUS OF "41".  THE PROGRAM DOES NOT   *  SQ1314.2
003000*    CONTAIN DECLARATIVE PROCEDURES, AND IN THIS CASE THE      *  SQ1314.2
003100*    STANDARD ALLOWS THE IMPLEMENTOR TO TERMINATE EXECUTION OF *  SQ1314.2
003200*    THE PROGRAM AS PART OF THE EXECUTION OF THE OPEN          *  SQ1314.2
003300*    STATEMENT.  HOWEVER, THE STANDARD ALSO ALLOWS EXECUTION   *  SQ1314.2
003400*    OF THE PROGRAM TO CONTINUE, AND THERE ARE TESTS TO COVER  *  SQ1314.2
003500*    THIS CASE.                                                *  SQ1314.2
003600*                                                              *  SQ1314.2
003700*    THE PROGRAM CONTAINS NO PROVISION FOR DELETION OF         *  SQ1314.2
003800*    OPERATIONS ON THE FILES, BUT INDIVIDUAL SUBORDINATE TESTS *  SQ1314.2
003900*    MAY BE DELETED.                                           *  SQ1314.2
004000*                                                              *  SQ1314.2
004100****************************************************************  SQ1314.2
004200*                                                                 SQ1314.2
004300 ENVIRONMENT DIVISION.                                            SQ1314.2
004400 CONFIGURATION SECTION.                                           SQ1314.2
004500 SOURCE-COMPUTER.                                                 SQ1314.2
004600     XXXXX082.                                                    SQ1314.2
004700 OBJECT-COMPUTER.                                                 SQ1314.2
004800     XXXXX083.                                                    SQ1314.2
004900*                                                                 SQ1314.2
005000 INPUT-OUTPUT SECTION.                                            SQ1314.2
005100 FILE-CONTROL.                                                    SQ1314.2
005200     SELECT PRINT-FILE ASSIGN TO                                  SQ1314.2
005300     XXXXX055.                                                    SQ1314.2
005400*                                                                 SQ1314.2
005500*    SELECT RAW-DATA   ASSIGN TO                                  SQ1314.2
005600*    XXXXX062                                                     SQ1314.2
005700*          ORGANIZATION IS INDEXED                                SQ1314.2
005800*          ACCESS MODE  IS RANDOM                                 SQ1314.2
005900*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1314.2
006000*                                                                 SQ1314.2
006100     SELECT SQ-FS1 ASSIGN TO                                      SQ1314.2
006200     XXXXX014                                                     SQ1314.2
006300            FILE STATUS IS SQ-FS1-STATUS.                         SQ1314.2
006400*                                                                 SQ1314.2
006500*                                                                 SQ1314.2
006600 DATA DIVISION.                                                   SQ1314.2
006700 FILE SECTION.                                                    SQ1314.2
006800 FD  PRINT-FILE                                                   SQ1314.2
006900C    LABEL RECORDS                                                SQ1314.2
007000C    XXXXX084                                                     SQ1314.2
007100C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1314.2
007200               .                                                  SQ1314.2
007300 01  PRINT-REC    PICTURE X(120).                                 SQ1314.2
007400 01  DUMMY-RECORD PICTURE X(120).                                 SQ1314.2
007500*                                                                 SQ1314.2
007600*FD  RAW-DATA.                                                    SQ1314.2
007700*01  RAW-DATA-SATZ.                                               SQ1314.2
007800*    05  RAW-DATA-KEY        PIC X(6).                            SQ1314.2
007900*    05  C-DATE              PIC 9(6).                            SQ1314.2
008000*    05  C-TIME              PIC 9(8).                            SQ1314.2
008100*    05  NO-OF-TESTS         PIC 99.                              SQ1314.2
008200*    05  C-OK                PIC 999.                             SQ1314.2
008300*    05  C-ALL               PIC 999.                             SQ1314.2
008400*    05  C-FAIL              PIC 999.                             SQ1314.2
008500*    05  C-DELETED           PIC 999.                             SQ1314.2
008600*    05  C-INSPECT           PIC 999.                             SQ1314.2
008700*    05  C-NOTE              PIC X(13).                           SQ1314.2
008800*    05  C-INDENT            PIC X.                               SQ1314.2
008900*    05  C-ABORT             PIC X(8).                            SQ1314.2
009000*                                                                 SQ1314.2
009100 FD  SQ-FS1                                                       SQ1314.2
009200C    LABEL RECORD IS STANDARD                                     SQ1314.2
009300                .                                                 SQ1314.2
009400 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1314.2
009500*                                                                 SQ1314.2
009600 WORKING-STORAGE SECTION.                                         SQ1314.2
009700*                                                                 SQ1314.2
009800***************************************************************   SQ1314.2
009900*                                                             *   SQ1314.2
010000*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1314.2
010100*                                                             *   SQ1314.2
010200***************************************************************   SQ1314.2
010300*                                                                 SQ1314.2
010400 01  SQ-FS1-STATUS.                                               SQ1314.2
010500   03  SQ-FS1-KEY-1   PIC X.                                      SQ1314.2
010600   03  SQ-FS1-KEY-2   PIC X.                                      SQ1314.2
010700*                                                                 SQ1314.2
010800*                                                                 SQ1314.2
010900***************************************************************   SQ1314.2
011000*                                                             *   SQ1314.2
011100*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1314.2
011200*                                                             *   SQ1314.2
011300***************************************************************   SQ1314.2
011400*                                                                 SQ1314.2
011500 01  REC-SKEL-SUB   PIC 99.                                       SQ1314.2
011600*                                                                 SQ1314.2
011700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1314.2
011800     03 FILE-RECORD-INFO-SKELETON.                                SQ1314.2
011900        05 FILLER                 PICTURE X(48)       VALUE       SQ1314.2
012000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1314.2
012100        05 FILLER                 PICTURE X(46)       VALUE       SQ1314.2
012200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1314.2
012300        05 FILLER                 PICTURE X(26)       VALUE       SQ1314.2
012400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1314.2
012500        05 FILLER                 PICTURE X(37)       VALUE       SQ1314.2
012600             ",RECKEY=                             ".             SQ1314.2
012700        05 FILLER                 PICTURE X(38)       VALUE       SQ1314.2
012800             ",ALTKEY1=                             ".            SQ1314.2
012900        05 FILLER                 PICTURE X(38)       VALUE       SQ1314.2
013000             ",ALTKEY2=                             ".            SQ1314.2
013100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1314.2
013200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1314.2
013300        05 FILE-RECORD-INFO-P1-120.                               SQ1314.2
013400           07 FILLER              PIC X(5).                       SQ1314.2
013500           07 XFILE-NAME          PIC X(6).                       SQ1314.2
013600           07 FILLER              PIC X(8).                       SQ1314.2
013700           07 XRECORD-NAME        PIC X(6).                       SQ1314.2
013800           07 FILLER              PIC X(1).                       SQ1314.2
013900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1314.2
014000           07 FILLER              PIC X(7).                       SQ1314.2
014100           07 XRECORD-NUMBER      PIC 9(6).                       SQ1314.2
014200           07 FILLER              PIC X(6).                       SQ1314.2
014300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1314.2
014400           07 FILLER              PIC X(5).                       SQ1314.2
014500           07 ODO-NUMBER          PIC 9(4).                       SQ1314.2
014600           07 FILLER              PIC X(5).                       SQ1314.2
014700           07 XPROGRAM-NAME       PIC X(5).                       SQ1314.2
014800           07 FILLER              PIC X(7).                       SQ1314.2
014900           07 XRECORD-LENGTH      PIC 9(6).                       SQ1314.2
015000           07 FILLER              PIC X(7).                       SQ1314.2
015100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1314.2
015200           07 FILLER              PIC X(1).                       SQ1314.2
015300           07 XBLOCK-SIZE         PIC 9(4).                       SQ1314.2
015400           07 FILLER              PIC X(6).                       SQ1314.2
015500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1314.2
015600           07 FILLER              PIC X(5).                       SQ1314.2
015700           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1314.2
015800           07 FILLER              PIC X(6).                       SQ1314.2
015900           07 XLABEL-TYPE         PIC X(1).                       SQ1314.2
016000        05 FILE-RECORD-INFO-P121-240.                             SQ1314.2
016100           07 FILLER              PIC X(8).                       SQ1314.2
016200           07 XRECORD-KEY         PIC X(29).                      SQ1314.2
016300           07 FILLER              PIC X(9).                       SQ1314.2
016400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1314.2
016500           07 FILLER              PIC X(9).                       SQ1314.2
016600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1314.2
016700           07 FILLER              PIC X(7).                       SQ1314.2
016800*                                                                 SQ1314.2
016900 01  TEST-RESULTS.                                                SQ1314.2
017000     02 FILLER              PIC X      VALUE SPACE.               SQ1314.2
017100     02  PAR-NAME.                                                SQ1314.2
017200       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1314.2
017300       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1314.2
017400       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1314.2
017500     02 FILLER              PIC X      VALUE SPACE.               SQ1314.2
017600     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1314.2
017700     02 FILLER              PIC X      VALUE SPACE.               SQ1314.2
017800     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1314.2
017900     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1314.2
018000     02 RE-MARK             PIC X(61).                            SQ1314.2
018100 01  TEST-COMPUTED.                                               SQ1314.2
018200   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1314.2
018300   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1314.2
018400   02 COMPUTED-X.                                                 SQ1314.2
018500     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1314.2
018600     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1314.2
018700     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1314.2
018800     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1314.2
018900     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1314.2
019000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1314.2
019100        04 COMPUTED-18V0                   PIC -9(18).            SQ1314.2
019200        04 FILLER                          PIC X.                 SQ1314.2
019300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1314.2
019400 01  TEST-CORRECT.                                                SQ1314.2
019500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1314.2
019600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1314.2
019700     02 CORRECT-X.                                                SQ1314.2
019800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1314.2
019900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1314.2
020000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1314.2
020100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1314.2
020200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1314.2
020300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1314.2
020400         04 CORRECT-18V0                     PIC -9(18).          SQ1314.2
020500         04 FILLER                           PIC X.               SQ1314.2
020600     03 FILLER PIC X(2) VALUE SPACE.                              SQ1314.2
020700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1314.2
020800*                                                                 SQ1314.2
020900 01  CCVS-C-1.                                                    SQ1314.2
021000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1314.2
021100     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1314.2
021200     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1314.2
021300     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1314.2
021400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1314.2
021500     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1314.2
021600     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1314.2
021700     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1314.2
021800 01  CCVS-C-2.                                                    SQ1314.2
021900     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1314.2
022000     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1314.2
022100     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1314.2
022200     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1314.2
022300     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1314.2
022400*                                                                 SQ1314.2
022500 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1314.2
022600 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1314.2
022700 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1314.2
022800 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1314.2
022900 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1314.2
023000 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1314.2
023100 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1314.2
023200 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1314.2
023300 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1314.2
023400 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1314.2
023500 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1314.2
023600 01  CCVS-H-1.                                                    SQ1314.2
023700     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1314.2
023800     02  FILLER          PIC X(42)    VALUE                       SQ1314.2
023900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1314.2
024000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1314.2
024100 01  CCVS-H-2A.                                                   SQ1314.2
024200   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1314.2
024300   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1314.2
024400   02  FILLER            PIC XXXX   VALUE                         SQ1314.2
024500     "4.2 ".                                                      SQ1314.2
024600   02  FILLER            PIC X(28)  VALUE                         SQ1314.2
024700            " COPY - NOT FOR DISTRIBUTION".                       SQ1314.2
024800   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1314.2
024900*                                                                 SQ1314.2
025000 01  CCVS-H-2B.                                                   SQ1314.2
025100   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1314.2
025200   02  TEST-ID           PIC X(9).                                SQ1314.2
025300   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1314.2
025400   02  FILLER            PIC X(12)  VALUE                         SQ1314.2
025500     " HIGH       ".                                              SQ1314.2
025600   02  FILLER            PIC X(22)  VALUE                         SQ1314.2
025700            " LEVEL VALIDATION FOR ".                             SQ1314.2
025800   02  FILLER            PIC X(58)  VALUE                         SQ1314.2
025900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1314.2
026000 01  CCVS-H-3.                                                    SQ1314.2
026100     02  FILLER          PIC X(34)  VALUE                         SQ1314.2
026200            " FOR OFFICIAL USE ONLY    ".                         SQ1314.2
026300     02  FILLER          PIC X(58)  VALUE                         SQ1314.2
026400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1314.2
026500     02  FILLER          PIC X(28)  VALUE                         SQ1314.2
026600            "  COPYRIGHT   1985,1986 ".                           SQ1314.2
026700 01  CCVS-E-1.                                                    SQ1314.2
026800     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1314.2
026900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1314.2
027000     02 ID-AGAIN         PIC X(9).                                SQ1314.2
027100     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1314.2
027200 01  CCVS-E-2.                                                    SQ1314.2
027300     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1314.2
027400     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1314.2
027500     02  CCVS-E-2-2.                                              SQ1314.2
027600         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1314.2
027700         03 FILLER         PIC X      VALUE SPACE.                SQ1314.2
027800         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1314.2
027900            "ERRORS ENCOUNTERED".                                 SQ1314.2
028000 01  CCVS-E-3.                                                    SQ1314.2
028100     02  FILLER          PIC X(22)  VALUE                         SQ1314.2
028200            " FOR OFFICIAL USE ONLY".                             SQ1314.2
028300     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1314.2
028400     02  FILLER          PIC X(58)  VALUE                         SQ1314.2
028500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1314.2
028600     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1314.2
028700     02  FILLER          PIC X(20)  VALUE                         SQ1314.2
028800             " COPYRIGHT 1985,1986".                              SQ1314.2
028900 01  CCVS-E-4.                                                    SQ1314.2
029000     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1314.2
029100     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1314.2
029200     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1314.2
029300     02 FILLER           PIC X(40)  VALUE                         SQ1314.2
029400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1314.2
029500 01  XXINFO.                                                      SQ1314.2
029600     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1314.2
029700     02 INFO-TEXT.                                                SQ1314.2
029800       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1314.2
029900       04 XXCOMPUTED         PIC X(20).                           SQ1314.2
030000       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1314.2
030100       04 XXCORRECT          PIC X(20).                           SQ1314.2
030200     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1314.2
030300 01  HYPHEN-LINE.                                                 SQ1314.2
030400     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1314.2
030500     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1314.2
030600-    "*****************************************".                 SQ1314.2
030700     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1314.2
030800-    "******************************".                            SQ1314.2
030900 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1314.2
031000     "SQ131A".                                                    SQ1314.2
031100*                                                                 SQ1314.2
031200*                                                                 SQ1314.2
031300 PROCEDURE DIVISION.                                              SQ1314.2
031400 CCVS1 SECTION.                                                   SQ1314.2
031500 OPEN-FILES.                                                      SQ1314.2
031600*    OPEN    I-O RAW-DATA.                                        SQ1314.2
031700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1314.2
031800*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1314.2
031900*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1314.2
032000*    ADD     1           TO C-NO-OF-TESTS.                        SQ1314.2
032100*    ACCEPT  C-DATE      FROM DATE.                               SQ1314.2
032200*    ACCEPT  C-TIME      FROM TIME.                               SQ1314.2
032300*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1314.2
032400*END-E-1.                                                         SQ1314.2
032500*    CLOSE   RAW-DATA.                                            SQ1314.2
032600     OPEN    OUTPUT PRINT-FILE.                                   SQ1314.2
032700     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1314.2
032800     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1314.2
032900     MOVE    SPACE TO TEST-RESULTS.                               SQ1314.2
033000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1314.2
033100     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1314.2
033200     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1314.2
033300     GO TO CCVS1-EXIT.                                            SQ1314.2
033400*                                                                 SQ1314.2
033500 CCVS-INIT-FILE.                                                  SQ1314.2
033600     ADD     1 TO REC-SKL-SUB.                                    SQ1314.2
033700     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1314.2
033800                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1314.2
033900*                                                                 SQ1314.2
034000 CLOSE-FILES.                                                     SQ1314.2
034100     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1314.2
034200     CLOSE   PRINT-FILE.                                          SQ1314.2
034300*    OPEN    I-O RAW-DATA.                                        SQ1314.2
034400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1314.2
034500*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1314.2
034600*    MOVE   "OK.     " TO C-ABORT.                                SQ1314.2
034700*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1314.2
034800*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1314.2
034900*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1314.2
035000*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1314.2
035100*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1314.2
035200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1314.2
035300*END-E-2.                                                         SQ1314.2
035400*    CLOSE   RAW-DATA.                                            SQ1314.2
035500 TERMINATE-CCVS.                                                  SQ1314.2
035600*    EXIT    PROGRAM.                                             SQ1314.2
035700     STOP    RUN.                                                 SQ1314.2
035800*                                                                 SQ1314.2
035900 INSPT.                                                           SQ1314.2
036000     MOVE   "INSPT" TO P-OR-F.                                    SQ1314.2
036100     ADD     1 TO INSPECT-COUNTER.                                SQ1314.2
036200     PERFORM PRINT-DETAIL.                                        SQ1314.2
036300                                                                  SQ1314.2
036400 PASS.                                                            SQ1314.2
036500     MOVE   "PASS " TO P-OR-F.                                    SQ1314.2
036600     ADD     1 TO PASS-COUNTER.                                   SQ1314.2
036700     PERFORM PRINT-DETAIL.                                        SQ1314.2
036800*                                                                 SQ1314.2
036900 FAIL.                                                            SQ1314.2
037000     MOVE   "FAIL*" TO P-OR-F.                                    SQ1314.2
037100     ADD     1 TO ERROR-COUNTER.                                  SQ1314.2
037200     PERFORM PRINT-DETAIL.                                        SQ1314.2
037300*                                                                 SQ1314.2
037400 DE-LETE.                                                         SQ1314.2
037500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1314.2
037600     MOVE   "*****" TO P-OR-F.                                    SQ1314.2
037700     ADD     1 TO DELETE-COUNTER.                                 SQ1314.2
037800     PERFORM PRINT-DETAIL.                                        SQ1314.2
037900*                                                                 SQ1314.2
038000 PRINT-DETAIL.                                                    SQ1314.2
038100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1314.2
038200         MOVE   "." TO PARDOT-X                                   SQ1314.2
038300         MOVE    REC-CT TO DOTVALUE.                              SQ1314.2
038400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1314.2
038500     PERFORM WRITE-LINE.                                          SQ1314.2
038600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1314.2
038700         PERFORM WRITE-LINE                                       SQ1314.2
038800         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1314.2
038900     ELSE                                                         SQ1314.2
039000         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1314.2
039100     MOVE    SPACE TO P-OR-F.                                     SQ1314.2
039200     MOVE    SPACE TO COMPUTED-X.                                 SQ1314.2
039300     MOVE    SPACE TO CORRECT-X.                                  SQ1314.2
039400     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1314.2
039500     MOVE    SPACE TO RE-MARK.                                    SQ1314.2
039600*                                                                 SQ1314.2
039700 HEAD-ROUTINE.                                                    SQ1314.2
039800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1314.2
039900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1314.2
040000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1314.2
040100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1314.2
040200 COLUMN-NAMES-ROUTINE.                                            SQ1314.2
040300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1314.2
040400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1314.2
040500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1314.2
040600 END-ROUTINE.                                                     SQ1314.2
040700     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1314.2
040800     PERFORM WRITE-LINE 5 TIMES.                                  SQ1314.2
040900 END-RTN-EXIT.                                                    SQ1314.2
041000     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1314.2
041100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1314.2
041200*                                                                 SQ1314.2
041300 END-ROUTINE-1.                                                   SQ1314.2
041400     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1314.2
041500     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1314.2
041600     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1314.2
041700     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1314.2
041800     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1314.2
041900     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1314.2
042000     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1314.2
042100     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1314.2
042200     PERFORM WRITE-LINE.                                          SQ1314.2
042300     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1314.2
042400     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1314.2
042500         MOVE   "NO " TO ERROR-TOTAL                              SQ1314.2
042600     ELSE                                                         SQ1314.2
042700         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1314.2
042800     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1314.2
042900     PERFORM WRITE-LINE.                                          SQ1314.2
043000 END-ROUTINE-13.                                                  SQ1314.2
043100     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1314.2
043200         MOVE   "NO " TO ERROR-TOTAL                              SQ1314.2
043300     ELSE                                                         SQ1314.2
043400         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1314.2
043500     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1314.2
043600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1314.2
043700     PERFORM WRITE-LINE.                                          SQ1314.2
043800     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1314.2
043900         MOVE   "NO " TO ERROR-TOTAL                              SQ1314.2
044000     ELSE                                                         SQ1314.2
044100         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1314.2
044200     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1314.2
044300     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1314.2
044400     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1314.2
044500*                                                                 SQ1314.2
044600 WRITE-LINE.                                                      SQ1314.2
044700     ADD     1 TO RECORD-COUNT.                                   SQ1314.2
044800*    IF RECORD-COUNT GREATER 50                                   SQ1314.2
044900*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1314.2
045000*        MOVE  SPACE TO DUMMY-RECORD                              SQ1314.2
045100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1314.2
045200*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1314.2
045300*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1314.2
045400*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1314.2
045500*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1314.2
045600*        MOVE  ZERO TO RECORD-COUNT.                              SQ1314.2
045700     PERFORM WRT-LN.                                              SQ1314.2
045800*                                                                 SQ1314.2
045900 WRT-LN.                                                          SQ1314.2
046000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1314.2
046100     MOVE    SPACE TO DUMMY-RECORD.                               SQ1314.2
046200 BLANK-LINE-PRINT.                                                SQ1314.2
046300     PERFORM WRT-LN.                                              SQ1314.2
046400 FAIL-ROUTINE.                                                    SQ1314.2
046500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1314.2
046600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1314.2
046700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1314.2
046800     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1314.2
046900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1314.2
047000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1314.2
047100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1314.2
047200     GO TO   FAIL-ROUTINE-EX.                                     SQ1314.2
047300 FAIL-ROUTINE-WRITE.                                              SQ1314.2
047400     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1314.2
047500     PERFORM WRITE-LINE                                           SQ1314.2
047600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1314.2
047700     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1314.2
047800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1314.2
047900     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1314.2
048000 FAIL-ROUTINE-EX.                                                 SQ1314.2
048100     EXIT.                                                        SQ1314.2
048200 BAIL-OUT.                                                        SQ1314.2
048300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1314.2
048400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1314.2
048500 BAIL-OUT-WRITE.                                                  SQ1314.2
048600     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1314.2
048700     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1314.2
048800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1314.2
048900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1314.2
049000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1314.2
049100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1314.2
049200 BAIL-OUT-EX.                                                     SQ1314.2
049300     EXIT.                                                        SQ1314.2
049400 CCVS1-EXIT.                                                      SQ1314.2
049500     EXIT.                                                        SQ1314.2
049600*                                                                 SQ1314.2
049700****************************************************************  SQ1314.2
049800*                                                              *  SQ1314.2
049900*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1314.2
050000*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1314.2
050100*                                                              *  SQ1314.2
050200****************************************************************  SQ1314.2
050300*                                                                 SQ1314.2
050400 SECT-SQ131A-MAIN SECTION.                                        SQ1314.2
050500*                                                                 SQ1314.2
050600*    THE FIRST ACTION IS TO CREATE THE FILE BY MEANS OF AN        SQ1314.2
050700*    OPEN OUTPUT STATEMENT.                                       SQ1314.2
050800*                                                                 SQ1314.2
050900 SEQ-INIT-01.                                                     SQ1314.2
051000*                                                                 SQ1314.2
051100     MOVE    0 TO REC-CT                                          SQ1314.2
051200     MOVE   "CREATE FILE, OPEN OUTPUT" TO FEATURE                 SQ1314.2
051300     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME                          SQ1314.2
051400     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1314.2
051500 SEQ-TEST-OP-01.                                                  SQ1314.2
051600     OPEN    OUTPUT SQ-FS1.                                       SQ1314.2
051700*                                                                 SQ1314.2
051800*    CHECK THE I-O STATUS VALUE RETURNED BY THE FIRST OPEN.       SQ1314.2
051900*                                                                 SQ1314.2
052000     ADD     1 TO REC-CT.                                         SQ1314.2
052100     GO TO   SEQ-TEST-OP-01-01.                                   SQ1314.2
052200 SEQ-DELETE-01-01.                                                SQ1314.2
052300     PERFORM DE-LETE.                                             SQ1314.2
052400     GO TO   SEQ-TEST-01-01-END.                                  SQ1314.2
052500 SEQ-TEST-OP-01-01.                                               SQ1314.2
052600     IF  SQ-FS1-STATUS = "00"                                     SQ1314.2
052700         PERFORM PASS                                             SQ1314.2
052800     ELSE                                                         SQ1314.2
052900         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1314.2
053000         MOVE   "00" TO CORRECT-A                                 SQ1314.2
053100         MOVE   "UNEXPECTED I-O STATUS CODE FROM OPEN OUTPUT"     SQ1314.2
053200                    TO RE-MARK                                    SQ1314.2
053300         MOVE   "VII-3, 1.5.3(1)A" TO ANSI-REFERENCE              SQ1314.2
053400         PERFORM FAIL.                                            SQ1314.2
053500 SEQ-TEST-01-01-END.                                              SQ1314.2
053600*                                                                 SQ1314.2
053700*                                                                 SQ1314.2
053800*    HAVING OPENED THE FILE FOR OUTPUT, THE NEXT ACTION IS TO     SQ1314.2
053900*    ATTEMPT TO OPEN IT FOR I-O.  THE STANDARD PERMITS THE        SQ1314.2
054000*    TERMINATION OF PROGRAM EXECUTION ON SUCH AN ATTEMPT TO       SQ1314.2
054100*    OPEN A FILE WHICH IS ALREADY OPEN, BUT ALSO ALLOWS           SQ1314.2
054200*    EXECUTION TO CONTINUE.                                       SQ1314.2
054300*                                                                 SQ1314.2
054400     MOVE    SPACE TO DUMMY-RECORD                                SQ1314.2
054500     PERFORM WRITE-LINE.                                          SQ1314.2
054600     MOVE   "ABOUT TO ATTEMPT TO OPEN AN OPEN FILE"               SQ1314.2
054700               TO DUMMY-RECORD                                    SQ1314.2
054800     PERFORM WRITE-LINE.                                          SQ1314.2
054900     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ1314.2
055000               TO DUMMY-RECORD                                    SQ1314.2
055100     PERFORM WRITE-LINE 3 TIMES.                                  SQ1314.2
055200*                                                                 SQ1314.2
055300 SEQ-INIT-02.                                                     SQ1314.2
055400     MOVE    0 TO REC-CT                                          SQ1314.2
055500     MOVE   "OPEN I-O ON AN OPEN FILE" TO FEATURE                 SQ1314.2
055600     MOVE   "SEQ-TEST-OP-02" TO PAR-NAME                          SQ1314.2
055700     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1314.2
055800 SEQ-TEST-OP-02.                                                  SQ1314.2
055900     OPEN    I-O SQ-FS1.                                          SQ1314.2
056000*                                                                 SQ1314.2
056100*    CHECK THE I-O STATUS VALUE RETURNED BY THE SECOND OPEN.      SQ1314.2
056200*                                                                 SQ1314.2
056300     ADD     1 TO REC-CT.                                         SQ1314.2
056400     GO TO   SEQ-TEST-OP-02-01.                                   SQ1314.2
056500 SEQ-DELETE-02-01.                                                SQ1314.2
056600     PERFORM DE-LETE.                                             SQ1314.2
056700     GO TO   SEQ-TEST-02-01-END.                                  SQ1314.2
056800 SEQ-TEST-OP-02-01.                                               SQ1314.2
056900     IF  SQ-FS1-STATUS = "41"                                     SQ1314.2
057000         PERFORM PASS                                             SQ1314.2
057100     ELSE                                                         SQ1314.2
057200         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1314.2
057300         MOVE   "41" TO CORRECT-A                                 SQ1314.2
057400         MOVE   "UNEXPECTED I-O STATUS CODE FROM OPEN I-O"        SQ1314.2
057500                    TO RE-MARK                                    SQ1314.2
057600         MOVE   "VII-4, 1.5.3(4)A" TO ANSI-REFERENCE              SQ1314.2
057700         PERFORM FAIL.                                            SQ1314.2
057800 SEQ-TEST-02-01-END.                                              SQ1314.2
057900*                                                                 SQ1314.2
058000*                                                                 SQ1314.2
058100 CCVS-EXIT SECTION.                                               SQ1314.2
058200 CCVS-999999.                                                     SQ1314.2
058300     GO TO   CLOSE-FILES.                                         SQ1314.2
      *END-OF,SQ131A                                                                  
