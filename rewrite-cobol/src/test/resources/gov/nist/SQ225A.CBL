      *HEADER,COBOL,SQ225A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2254.2
000200 PROGRAM-ID.                                                      SQ2254.2
000300     SQ225A.                                                      SQ2254.2
000400****************************************************************  SQ2254.2
000500*                                                              *  SQ2254.2
000600*    VALIDATION FOR:-                                          *  SQ2254.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2254.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ2254.2
000900*                             REVISED 1986, AUGUST             *  SQ2254.2
001000*                                                              *  SQ2254.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ2254.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2254.2
001300*                                                              *  SQ2254.2
001400****************************************************************  SQ2254.2
001500*                                                              *  SQ2254.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2254.2
001700*                                                              *  SQ2254.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE.              *  SQ2254.2
001900*            X-55   SYSTEM PRINTER                             *  SQ2254.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ2254.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ2254.2
002200*                                                              *  SQ2254.2
002300*                                                              *  SQ2254.2
002400****************************************************************  SQ2254.2
002500*                                                              *  SQ2254.2
002600*    SQ225A ATTEMPTS TO OPEN FOR EXTEND A MASS STORAGE FILE    *  SQ2254.2
002700*    WHICH IS NOT PRESENT.  THIS SHOULD RESULT IN A PERMANENT  *  SQ2254.2
002800*    ERROR AND AN I-O STATUS OF "35".  THE PROGRAM CONTAINS AN *  SQ2254.2
002900*    APPLICABLE DECLARATIVE PROCEDURE, WHICH SHOULD BE         *  SQ2254.2
003000*    EXECUTED.  THE STANDARD ALLOWS THE IMPLEMENTOR TO         *  SQ2254.2
003100*    TERMINATE EXECUTION ON EXIT FROM THE DECLARATIVE, OR TO   *  SQ2254.2
003200*    CONTINUE EXECUTION IN THE MAIN PROGRAM.                   *  SQ2254.2
003300*                                                              *  SQ2254.2
003400****************************************************************  SQ2254.2
003500*                                                                 SQ2254.2
003600 ENVIRONMENT DIVISION.                                            SQ2254.2
003700 CONFIGURATION SECTION.                                           SQ2254.2
003800 SOURCE-COMPUTER.                                                 SQ2254.2
003900     XXXXX082.                                                    SQ2254.2
004000 OBJECT-COMPUTER.                                                 SQ2254.2
004100     XXXXX083.                                                    SQ2254.2
004200*                                                                 SQ2254.2
004300 INPUT-OUTPUT SECTION.                                            SQ2254.2
004400 FILE-CONTROL.                                                    SQ2254.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SQ2254.2
004600     XXXXX055.                                                    SQ2254.2
004700*                                                                 SQ2254.2
004800*    SELECT RAW-DATA   ASSIGN TO                                  SQ2254.2
004900*    XXXXX062                                                     SQ2254.2
005000*          ORGANIZATION IS INDEXED                                SQ2254.2
005100*          ACCESS MODE  IS RANDOM                                 SQ2254.2
005200*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ2254.2
005300*                                                                 SQ2254.2
005400     SELECT SQ-FS1 ASSIGN TO                                      SQ2254.2
005500     XXXXX014                                                     SQ2254.2
005600            FILE STATUS IS SQ-FS1-STATUS.                         SQ2254.2
005700*                                                                 SQ2254.2
005800*                                                                 SQ2254.2
005900 DATA DIVISION.                                                   SQ2254.2
006000 FILE SECTION.                                                    SQ2254.2
006100 FD  PRINT-FILE                                                   SQ2254.2
006200C    LABEL RECORDS                                                SQ2254.2
006300C    XXXXX084                                                     SQ2254.2
006400C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2254.2
006500               .                                                  SQ2254.2
006600 01  PRINT-REC    PICTURE X(120).                                 SQ2254.2
006700 01  DUMMY-RECORD PICTURE X(120).                                 SQ2254.2
006800*                                                                 SQ2254.2
006900*FD  RAW-DATA.                                                    SQ2254.2
007000*01  RAW-DATA-SATZ.                                               SQ2254.2
007100*    05  RAW-DATA-KEY        PIC X(6).                            SQ2254.2
007200*    05  C-DATE              PIC 9(6).                            SQ2254.2
007300*    05  C-TIME              PIC 9(8).                            SQ2254.2
007400*    05  NO-OF-TESTS         PIC 99.                              SQ2254.2
007500*    05  C-OK                PIC 999.                             SQ2254.2
007600*    05  C-ALL               PIC 999.                             SQ2254.2
007700*    05  C-FAIL              PIC 999.                             SQ2254.2
007800*    05  C-DELETED           PIC 999.                             SQ2254.2
007900*    05  C-INSPECT           PIC 999.                             SQ2254.2
008000*    05  C-NOTE              PIC X(13).                           SQ2254.2
008100*    05  C-INDENT            PIC X.                               SQ2254.2
008200*    05  C-ABORT             PIC X(8).                            SQ2254.2
008300*                                                                 SQ2254.2
008400 FD  SQ-FS1                                                       SQ2254.2
008500C    LABEL RECORD IS STANDARD                                     SQ2254.2
008600                .                                                 SQ2254.2
008700 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2254.2
008800*                                                                 SQ2254.2
008900 WORKING-STORAGE SECTION.                                         SQ2254.2
009000*                                                                 SQ2254.2
009100***************************************************************   SQ2254.2
009200*                                                             *   SQ2254.2
009300*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2254.2
009400*                                                             *   SQ2254.2
009500***************************************************************   SQ2254.2
009600*                                                                 SQ2254.2
009700 01  SQ-FS1-STATUS.                                               SQ2254.2
009800   03  SQ-FS1-KEY-1   PIC X.                                      SQ2254.2
009900   03  SQ-FS1-KEY-2   PIC X.                                      SQ2254.2
010000*                                                                 SQ2254.2
010100 01  DECL-EXEC-SW  PIC 9.                                         SQ2254.2
010200*                                                                 SQ2254.2
010300***************************************************************   SQ2254.2
010400*                                                             *   SQ2254.2
010500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2254.2
010600*                                                             *   SQ2254.2
010700***************************************************************   SQ2254.2
010800*                                                                 SQ2254.2
010900 01  REC-SKEL-SUB   PIC 99.                                       SQ2254.2
011000*                                                                 SQ2254.2
011100 01  FILE-RECORD-INFORMATION-REC.                                 SQ2254.2
011200     03 FILE-RECORD-INFO-SKELETON.                                SQ2254.2
011300        05 FILLER                 PICTURE X(48)       VALUE       SQ2254.2
011400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2254.2
011500        05 FILLER                 PICTURE X(46)       VALUE       SQ2254.2
011600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2254.2
011700        05 FILLER                 PICTURE X(26)       VALUE       SQ2254.2
011800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2254.2
011900        05 FILLER                 PICTURE X(37)       VALUE       SQ2254.2
012000             ",RECKEY=                             ".             SQ2254.2
012100        05 FILLER                 PICTURE X(38)       VALUE       SQ2254.2
012200             ",ALTKEY1=                             ".            SQ2254.2
012300        05 FILLER                 PICTURE X(38)       VALUE       SQ2254.2
012400             ",ALTKEY2=                             ".            SQ2254.2
012500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2254.2
012600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2254.2
012700        05 FILE-RECORD-INFO-P1-120.                               SQ2254.2
012800           07 FILLER              PIC X(5).                       SQ2254.2
012900           07 XFILE-NAME          PIC X(6).                       SQ2254.2
013000           07 FILLER              PIC X(8).                       SQ2254.2
013100           07 XRECORD-NAME        PIC X(6).                       SQ2254.2
013200           07 FILLER              PIC X(1).                       SQ2254.2
013300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2254.2
013400           07 FILLER              PIC X(7).                       SQ2254.2
013500           07 XRECORD-NUMBER      PIC 9(6).                       SQ2254.2
013600           07 FILLER              PIC X(6).                       SQ2254.2
013700           07 UPDATE-NUMBER       PIC 9(2).                       SQ2254.2
013800           07 FILLER              PIC X(5).                       SQ2254.2
013900           07 ODO-NUMBER          PIC 9(4).                       SQ2254.2
014000           07 FILLER              PIC X(5).                       SQ2254.2
014100           07 XPROGRAM-NAME       PIC X(5).                       SQ2254.2
014200           07 FILLER              PIC X(7).                       SQ2254.2
014300           07 XRECORD-LENGTH      PIC 9(6).                       SQ2254.2
014400           07 FILLER              PIC X(7).                       SQ2254.2
014500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2254.2
014600           07 FILLER              PIC X(1).                       SQ2254.2
014700           07 XBLOCK-SIZE         PIC 9(4).                       SQ2254.2
014800           07 FILLER              PIC X(6).                       SQ2254.2
014900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2254.2
015000           07 FILLER              PIC X(5).                       SQ2254.2
015100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2254.2
015200           07 FILLER              PIC X(6).                       SQ2254.2
015300           07 XLABEL-TYPE         PIC X(1).                       SQ2254.2
015400        05 FILE-RECORD-INFO-P121-240.                             SQ2254.2
015500           07 FILLER              PIC X(8).                       SQ2254.2
015600           07 XRECORD-KEY         PIC X(29).                      SQ2254.2
015700           07 FILLER              PIC X(9).                       SQ2254.2
015800           07 ALTERNATE-KEY1      PIC X(29).                      SQ2254.2
015900           07 FILLER              PIC X(9).                       SQ2254.2
016000           07 ALTERNATE-KEY2      PIC X(29).                      SQ2254.2
016100           07 FILLER              PIC X(7).                       SQ2254.2
016200*                                                                 SQ2254.2
016300 01  TEST-RESULTS.                                                SQ2254.2
016400     02 FILLER              PIC X      VALUE SPACE.               SQ2254.2
016500     02  PAR-NAME.                                                SQ2254.2
016600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2254.2
016700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2254.2
016800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2254.2
016900     02 FILLER              PIC X      VALUE SPACE.               SQ2254.2
017000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2254.2
017100     02 FILLER              PIC X      VALUE SPACE.               SQ2254.2
017200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2254.2
017300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2254.2
017400     02 RE-MARK             PIC X(61).                            SQ2254.2
017500 01  TEST-COMPUTED.                                               SQ2254.2
017600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2254.2
017700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2254.2
017800   02 COMPUTED-X.                                                 SQ2254.2
017900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2254.2
018000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2254.2
018100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2254.2
018200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2254.2
018300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2254.2
018400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2254.2
018500        04 COMPUTED-18V0                   PIC -9(18).            SQ2254.2
018600        04 FILLER                          PIC X.                 SQ2254.2
018700     03 FILLER PIC X(50) VALUE SPACE.                             SQ2254.2
018800 01  TEST-CORRECT.                                                SQ2254.2
018900     02 FILLER PIC X(30) VALUE SPACE.                             SQ2254.2
019000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2254.2
019100     02 CORRECT-X.                                                SQ2254.2
019200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2254.2
019300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2254.2
019400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2254.2
019500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2254.2
019600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2254.2
019700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2254.2
019800         04 CORRECT-18V0                     PIC -9(18).          SQ2254.2
019900         04 FILLER                           PIC X.               SQ2254.2
020000     03 FILLER PIC X(2) VALUE SPACE.                              SQ2254.2
020100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2254.2
020200*                                                                 SQ2254.2
020300 01  CCVS-C-1.                                                    SQ2254.2
020400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2254.2
020500     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ2254.2
020600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2254.2
020700     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ2254.2
020800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2254.2
020900     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ2254.2
021000     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ2254.2
021100     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ2254.2
021200 01  CCVS-C-2.                                                    SQ2254.2
021300     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2254.2
021400     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ2254.2
021500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2254.2
021600     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ2254.2
021700     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ2254.2
021800*                                                                 SQ2254.2
021900 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2254.2
022000 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2254.2
022100 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2254.2
022200 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2254.2
022300 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2254.2
022400 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2254.2
022500 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2254.2
022600 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2254.2
022700 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2254.2
022800 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2254.2
022900 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2254.2
023000 01  CCVS-H-1.                                                    SQ2254.2
023100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2254.2
023200     02  FILLER          PIC X(42)    VALUE                       SQ2254.2
023300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2254.2
023400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2254.2
023500 01  CCVS-H-2A.                                                   SQ2254.2
023600   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2254.2
023700   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2254.2
023800   02  FILLER            PIC XXXX   VALUE                         SQ2254.2
023900     "4.2 ".                                                      SQ2254.2
024000   02  FILLER            PIC X(28)  VALUE                         SQ2254.2
024100            " COPY - NOT FOR DISTRIBUTION".                       SQ2254.2
024200   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2254.2
024300*                                                                 SQ2254.2
024400 01  CCVS-H-2B.                                                   SQ2254.2
024500   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2254.2
024600   02  TEST-ID           PIC X(9).                                SQ2254.2
024700   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2254.2
024800   02  FILLER            PIC X(12)  VALUE                         SQ2254.2
024900     " HIGH       ".                                              SQ2254.2
025000   02  FILLER            PIC X(22)  VALUE                         SQ2254.2
025100            " LEVEL VALIDATION FOR ".                             SQ2254.2
025200   02  FILLER            PIC X(58)  VALUE                         SQ2254.2
025300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2254.2
025400 01  CCVS-H-3.                                                    SQ2254.2
025500     02  FILLER          PIC X(34)  VALUE                         SQ2254.2
025600            " FOR OFFICIAL USE ONLY    ".                         SQ2254.2
025700     02  FILLER          PIC X(58)  VALUE                         SQ2254.2
025800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2254.2
025900     02  FILLER          PIC X(28)  VALUE                         SQ2254.2
026000            "  COPYRIGHT   1985,1986 ".                           SQ2254.2
026100 01  CCVS-E-1.                                                    SQ2254.2
026200     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2254.2
026300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2254.2
026400     02 ID-AGAIN         PIC X(9).                                SQ2254.2
026500     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2254.2
026600 01  CCVS-E-2.                                                    SQ2254.2
026700     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2254.2
026800     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2254.2
026900     02  CCVS-E-2-2.                                              SQ2254.2
027000         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2254.2
027100         03 FILLER         PIC X      VALUE SPACE.                SQ2254.2
027200         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2254.2
027300            "ERRORS ENCOUNTERED".                                 SQ2254.2
027400 01  CCVS-E-3.                                                    SQ2254.2
027500     02  FILLER          PIC X(22)  VALUE                         SQ2254.2
027600            " FOR OFFICIAL USE ONLY".                             SQ2254.2
027700     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2254.2
027800     02  FILLER          PIC X(58)  VALUE                         SQ2254.2
027900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2254.2
028000     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2254.2
028100     02  FILLER          PIC X(20)  VALUE                         SQ2254.2
028200             " COPYRIGHT 1985,1986".                              SQ2254.2
028300 01  CCVS-E-4.                                                    SQ2254.2
028400     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2254.2
028500     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2254.2
028600     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2254.2
028700     02 FILLER           PIC X(40)  VALUE                         SQ2254.2
028800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2254.2
028900 01  XXINFO.                                                      SQ2254.2
029000     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2254.2
029100     02 INFO-TEXT.                                                SQ2254.2
029200       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2254.2
029300       04 XXCOMPUTED         PIC X(20).                           SQ2254.2
029400       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2254.2
029500       04 XXCORRECT          PIC X(20).                           SQ2254.2
029600     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2254.2
029700 01  HYPHEN-LINE.                                                 SQ2254.2
029800     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2254.2
029900     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2254.2
030000-    "*****************************************".                 SQ2254.2
030100     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2254.2
030200-    "******************************".                            SQ2254.2
030300 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2254.2
030400     "SQ225A".                                                    SQ2254.2
030500*                                                                 SQ2254.2
030600*                                                                 SQ2254.2
030700 PROCEDURE DIVISION.                                              SQ2254.2
030800 DECLARATIVES.                                                    SQ2254.2
030900 SQ225A-DECLARATIVE-001-SECT SECTION.                             SQ2254.2
031000     USE AFTER ERROR PROCEDURE EXTEND.                            SQ2254.2
031100 INPUT-ERROR-PROCEDURE.                                           SQ2254.2
031200     IF DECL-EXEC-SW NOT = 9                                      SQ2254.2
031300         GO TO   NOT-DECL-9.                                      SQ2254.2
031400*                                                                 SQ2254.2
031500*    DECLARATIVE PROCEDURE ENTERED FROM OPEN INPUT                SQ2254.2
031600*                                                                 SQ2254.2
031700 DECL-OPEN-TEST.                                                  SQ2254.2
031800     MOVE   "EXEC USE ON OPEN FAILURE" TO FEATURE.                SQ2254.2
031900     MOVE   "DECL-OPEN-TEST" TO PAR-NAME.                         SQ2254.2
032000     MOVE    1               TO REC-CT.                           SQ2254.2
032100     IF SQ-FS1-STATUS = "35"                                      SQ2254.2
032200         PERFORM DECL-PASS                                        SQ2254.2
032300     ELSE                                                         SQ2254.2
032400         MOVE   "35"           TO CORRECT-A                       SQ2254.2
032500         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2254.2
032600         MOVE   "INCORRECT FILE STATUS FOR NON-AVAILABLE FILE"    SQ2254.2
032700                   TO RE-MARK                                     SQ2254.2
032800         PERFORM DECL-FAIL.                                       SQ2254.2
032900     MOVE    SPACE TO DUMMY-RECORD                                SQ2254.2
033000     PERFORM DECL-WRITE-LINE                                      SQ2254.2
033100     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ2254.2
033200               TO DUMMY-RECORD                                    SQ2254.2
033300     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ2254.2
033400     GO TO   END-DECLS.                                           SQ2254.2
033500*                                                                 SQ2254.2
033600*                                                                 SQ2254.2
033700 NOT-DECL-9.                                                      SQ2254.2
033800     MOVE   "NOT-DECL-9"  TO PAR-NAME.                            SQ2254.2
033900     MOVE    DECL-EXEC-SW TO COMPUTED-18V0.                       SQ2254.2
034000     MOVE    9            TO CORRECT-18V0.                        SQ2254.2
034100     PERFORM DECL-FAIL.                                           SQ2254.2
034200     GO TO   END-DECLS.                                           SQ2254.2
034300*                                                                 SQ2254.2
034400*                                                                 SQ2254.2
034500 DECL-PASS.                                                       SQ2254.2
034600     MOVE   "PASS " TO P-OR-F.                                    SQ2254.2
034700     ADD     1 TO PASS-COUNTER.                                   SQ2254.2
034800     PERFORM DECL-PRINT-DETAIL.                                   SQ2254.2
034900*                                                                 SQ2254.2
035000 DECL-FAIL.                                                       SQ2254.2
035100     MOVE   "FAIL*" TO P-OR-F.                                    SQ2254.2
035200     ADD     1 TO ERROR-COUNTER.                                  SQ2254.2
035300     PERFORM DECL-PRINT-DETAIL.                                   SQ2254.2
035400*                                                                 SQ2254.2
035500 DECL-PRINT-DETAIL.                                               SQ2254.2
035600     IF REC-CT NOT EQUAL TO ZERO                                  SQ2254.2
035700             MOVE "." TO PARDOT-X                                 SQ2254.2
035800             MOVE REC-CT TO DOTVALUE.                             SQ2254.2
035900     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2254.2
036000     PERFORM DECL-WRITE-LINE.                                     SQ2254.2
036100     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2254.2
036200         PERFORM DECL-WRITE-LINE                                  SQ2254.2
036300         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ2254.2
036400     ELSE                                                         SQ2254.2
036500         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ2254.2
036600     MOVE    SPACE TO P-OR-F.                                     SQ2254.2
036700     MOVE    SPACE TO COMPUTED-X.                                 SQ2254.2
036800     MOVE    SPACE TO CORRECT-X.                                  SQ2254.2
036900     IF REC-CT EQUAL TO ZERO                                      SQ2254.2
037000         MOVE    SPACE TO PAR-NAME.                               SQ2254.2
037100     MOVE    SPACE TO RE-MARK.                                    SQ2254.2
037200*                                                                 SQ2254.2
037300 DECL-WRITE-LINE.                                                 SQ2254.2
037400     ADD     1 TO RECORD-COUNT.                                   SQ2254.2
037500*    IF RECORD-COUNT GREATER 50                                   SQ2254.2
037600*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ2254.2
037700*        MOVE    SPACE TO DUMMY-RECORD                            SQ2254.2
037800*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ2254.2
037900*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ2254.2
038000*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ2254.2
038100*        PERFORM DECL-WRT-LN 2 TIMES                              SQ2254.2
038200*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ2254.2
038300*        PERFORM DECL-WRT-LN                                      SQ2254.2
038400*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ2254.2
038500*        MOVE    ZERO TO RECORD-COUNT.                            SQ2254.2
038600     PERFORM DECL-WRT-LN.                                         SQ2254.2
038700*                                                                 SQ2254.2
038800 DECL-WRT-LN.                                                     SQ2254.2
038900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2254.2
039000     MOVE    SPACE TO DUMMY-RECORD.                               SQ2254.2
039100*                                                                 SQ2254.2
039200 DECL-FAIL-ROUTINE.                                               SQ2254.2
039300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ2254.2
039400     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ2254.2
039500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2254.2
039600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2254.2
039700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2254.2
039800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2254.2
039900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2254.2
040000     GO TO   DECL-FAIL-EX.                                        SQ2254.2
040100 DECL-FAIL-WRITE.                                                 SQ2254.2
040200     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ2254.2
040300     PERFORM DECL-WRITE-LINE                                      SQ2254.2
040400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2254.2
040500     MOVE    TEST-CORRECT TO PRINT-REC                            SQ2254.2
040600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2254.2
040700     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ2254.2
040800 DECL-FAIL-EX.                                                    SQ2254.2
040900     EXIT.                                                        SQ2254.2
041000*                                                                 SQ2254.2
041100 DECL-BAIL.                                                       SQ2254.2
041200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ2254.2
041300     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ2254.2
041400 DECL-BAIL-WRITE.                                                 SQ2254.2
041500     MOVE    CORRECT-A TO XXCORRECT.                              SQ2254.2
041600     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ2254.2
041700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2254.2
041800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2254.2
041900 DECL-BAIL-EX.                                                    SQ2254.2
042000     EXIT.                                                        SQ2254.2
042100*                                                                 SQ2254.2
042200 END-DECLS.                                                       SQ2254.2
042300     MOVE    ZERO TO DECL-EXEC-SW.                                SQ2254.2
042400 END DECLARATIVES.                                                SQ2254.2
042500*                                                                 SQ2254.2
042600*                                                                 SQ2254.2
042700 CCVS1 SECTION.                                                   SQ2254.2
042800 OPEN-FILES.                                                      SQ2254.2
042900*    OPEN    I-O RAW-DATA.                                        SQ2254.2
043000*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2254.2
043100*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ2254.2
043200*    MOVE   "ABORTED "   TO C-ABORT.                              SQ2254.2
043300*    ADD     1           TO C-NO-OF-TESTS.                        SQ2254.2
043400*    ACCEPT  C-DATE      FROM DATE.                               SQ2254.2
043500*    ACCEPT  C-TIME      FROM TIME.                               SQ2254.2
043600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2254.2
043700*END-E-1.                                                         SQ2254.2
043800*    CLOSE   RAW-DATA.                                            SQ2254.2
043900     OPEN    OUTPUT PRINT-FILE.                                   SQ2254.2
044000     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2254.2
044100     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2254.2
044200     MOVE    SPACE TO TEST-RESULTS.                               SQ2254.2
044300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2254.2
044400     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2254.2
044500     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2254.2
044600     GO TO CCVS1-EXIT.                                            SQ2254.2
044700*                                                                 SQ2254.2
044800 CCVS-INIT-FILE.                                                  SQ2254.2
044900     ADD     1 TO REC-SKL-SUB.                                    SQ2254.2
045000     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2254.2
045100                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2254.2
045200*                                                                 SQ2254.2
045300 CLOSE-FILES.                                                     SQ2254.2
045400     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2254.2
045500     CLOSE   PRINT-FILE.                                          SQ2254.2
045600*    OPEN    I-O RAW-DATA.                                        SQ2254.2
045700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2254.2
045800*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ2254.2
045900*    MOVE   "OK.     " TO C-ABORT.                                SQ2254.2
046000*    MOVE    PASS-COUNTER  TO C-OK.                               SQ2254.2
046100*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ2254.2
046200*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ2254.2
046300*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ2254.2
046400*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ2254.2
046500*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2254.2
046600*END-E-2.                                                         SQ2254.2
046700*    CLOSE   RAW-DATA.                                            SQ2254.2
046800 TERMINATE-CCVS.                                                  SQ2254.2
046900*    EXIT    PROGRAM.                                             SQ2254.2
047000     STOP    RUN.                                                 SQ2254.2
047100*                                                                 SQ2254.2
047200 INSPT.                                                           SQ2254.2
047300     MOVE   "INSPT" TO P-OR-F.                                    SQ2254.2
047400     ADD     1 TO INSPECT-COUNTER.                                SQ2254.2
047500     PERFORM PRINT-DETAIL.                                        SQ2254.2
047600                                                                  SQ2254.2
047700 PASS.                                                            SQ2254.2
047800     MOVE   "PASS " TO P-OR-F.                                    SQ2254.2
047900     ADD     1 TO PASS-COUNTER.                                   SQ2254.2
048000     PERFORM PRINT-DETAIL.                                        SQ2254.2
048100*                                                                 SQ2254.2
048200 FAIL.                                                            SQ2254.2
048300     MOVE   "FAIL*" TO P-OR-F.                                    SQ2254.2
048400     ADD     1 TO ERROR-COUNTER.                                  SQ2254.2
048500     PERFORM PRINT-DETAIL.                                        SQ2254.2
048600*                                                                 SQ2254.2
048700 DE-LETE.                                                         SQ2254.2
048800     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2254.2
048900     MOVE   "*****" TO P-OR-F.                                    SQ2254.2
049000     ADD     1 TO DELETE-COUNTER.                                 SQ2254.2
049100     PERFORM PRINT-DETAIL.                                        SQ2254.2
049200*                                                                 SQ2254.2
049300 PRINT-DETAIL.                                                    SQ2254.2
049400     IF REC-CT NOT EQUAL TO ZERO                                  SQ2254.2
049500         MOVE   "." TO PARDOT-X                                   SQ2254.2
049600         MOVE    REC-CT TO DOTVALUE.                              SQ2254.2
049700     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2254.2
049800     PERFORM WRITE-LINE.                                          SQ2254.2
049900     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2254.2
050000         PERFORM WRITE-LINE                                       SQ2254.2
050100         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2254.2
050200     ELSE                                                         SQ2254.2
050300         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2254.2
050400     MOVE    SPACE TO P-OR-F.                                     SQ2254.2
050500     MOVE    SPACE TO COMPUTED-X.                                 SQ2254.2
050600     MOVE    SPACE TO CORRECT-X.                                  SQ2254.2
050700     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2254.2
050800     MOVE    SPACE TO RE-MARK.                                    SQ2254.2
050900*                                                                 SQ2254.2
051000 HEAD-ROUTINE.                                                    SQ2254.2
051100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2254.2
051200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2254.2
051300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2254.2
051400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2254.2
051500 COLUMN-NAMES-ROUTINE.                                            SQ2254.2
051600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2254.2
051700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2254.2
051800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2254.2
051900 END-ROUTINE.                                                     SQ2254.2
052000     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2254.2
052100     PERFORM WRITE-LINE 5 TIMES.                                  SQ2254.2
052200 END-RTN-EXIT.                                                    SQ2254.2
052300     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2254.2
052400     PERFORM WRITE-LINE 2 TIMES.                                  SQ2254.2
052500*                                                                 SQ2254.2
052600 END-ROUTINE-1.                                                   SQ2254.2
052700     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2254.2
052800     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2254.2
052900     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2254.2
053000     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2254.2
053100     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2254.2
053200     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2254.2
053300     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2254.2
053400     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2254.2
053500     PERFORM WRITE-LINE.                                          SQ2254.2
053600     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2254.2
053700     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2254.2
053800         MOVE   "NO " TO ERROR-TOTAL                              SQ2254.2
053900     ELSE                                                         SQ2254.2
054000         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2254.2
054100     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2254.2
054200     PERFORM WRITE-LINE.                                          SQ2254.2
054300 END-ROUTINE-13.                                                  SQ2254.2
054400     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2254.2
054500         MOVE   "NO " TO ERROR-TOTAL                              SQ2254.2
054600     ELSE                                                         SQ2254.2
054700         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2254.2
054800     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2254.2
054900     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2254.2
055000     PERFORM WRITE-LINE.                                          SQ2254.2
055100     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2254.2
055200         MOVE   "NO " TO ERROR-TOTAL                              SQ2254.2
055300     ELSE                                                         SQ2254.2
055400         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2254.2
055500     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2254.2
055600     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2254.2
055700     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2254.2
055800*                                                                 SQ2254.2
055900 WRITE-LINE.                                                      SQ2254.2
056000     ADD     1 TO RECORD-COUNT.                                   SQ2254.2
056100*    IF RECORD-COUNT GREATER 50                                   SQ2254.2
056200*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2254.2
056300*        MOVE  SPACE TO DUMMY-RECORD                              SQ2254.2
056400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2254.2
056500*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2254.2
056600*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2254.2
056700*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2254.2
056800*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2254.2
056900*        MOVE  ZERO TO RECORD-COUNT.                              SQ2254.2
057000     PERFORM WRT-LN.                                              SQ2254.2
057100*                                                                 SQ2254.2
057200 WRT-LN.                                                          SQ2254.2
057300     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2254.2
057400     MOVE    SPACE TO DUMMY-RECORD.                               SQ2254.2
057500 BLANK-LINE-PRINT.                                                SQ2254.2
057600     PERFORM WRT-LN.                                              SQ2254.2
057700 FAIL-ROUTINE.                                                    SQ2254.2
057800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2254.2
057900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2254.2
058000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2254.2
058100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2254.2
058200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2254.2
058300     PERFORM WRITE-LINE 2 TIMES.                                  SQ2254.2
058400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2254.2
058500     GO TO   FAIL-ROUTINE-EX.                                     SQ2254.2
058600 FAIL-ROUTINE-WRITE.                                              SQ2254.2
058700     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2254.2
058800     PERFORM WRITE-LINE                                           SQ2254.2
058900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2254.2
059000     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2254.2
059100     PERFORM WRITE-LINE 2 TIMES.                                  SQ2254.2
059200     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2254.2
059300 FAIL-ROUTINE-EX.                                                 SQ2254.2
059400     EXIT.                                                        SQ2254.2
059500 BAIL-OUT.                                                        SQ2254.2
059600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2254.2
059700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2254.2
059800 BAIL-OUT-WRITE.                                                  SQ2254.2
059900     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2254.2
060000     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2254.2
060100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2254.2
060200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2254.2
060300     PERFORM WRITE-LINE 2 TIMES.                                  SQ2254.2
060400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2254.2
060500 BAIL-OUT-EX.                                                     SQ2254.2
060600     EXIT.                                                        SQ2254.2
060700 CCVS1-EXIT.                                                      SQ2254.2
060800     EXIT.                                                        SQ2254.2
060900*                                                                 SQ2254.2
061000****************************************************************  SQ2254.2
061100*                                                              *  SQ2254.2
061200*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2254.2
061300*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2254.2
061400*                                                              *  SQ2254.2
061500****************************************************************  SQ2254.2
061600*                                                                 SQ2254.2
061700 SECT-SQ225A-MAIN SECTION.                                        SQ2254.2
061800 OPEN-INIT-01.                                                    SQ2254.2
061900*                                                                 SQ2254.2
062000*    THIS PROGRAM ATTEMPTS TO OPEN A FILE WHICH IS NOT            SQ2254.2
062100*    PRESENT AND AVAILABLE TO IT.                                 SQ2254.2
062200*                                                                 SQ2254.2
062300     MOVE    9 TO DECL-EXEC-SW                                    SQ2254.2
062400     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2254.2
062500 OPEN-TEST-01.                                                    SQ2254.2
062600     OPEN    EXTEND SQ-FS1.                                       SQ2254.2
062700     MOVE    1 TO REC-CT                                          SQ2254.2
062800     MOVE   "OPEN ABSENT FILE EXTEND" TO FEATURE                  SQ2254.2
062900     MOVE   "OPEN-TEST-01" TO PAR-NAME                            SQ2254.2
063000     IF      DECL-EXEC-SW = 0                                     SQ2254.2
063100         PERFORM PASS                                             SQ2254.2
063200     ELSE                                                         SQ2254.2
063300         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ2254.2
063400         MOVE    ZERO         TO CORRECT-18V0                     SQ2254.2
063500         MOVE   "DECLARATIVE NOT EXECUTED" TO RE-MARK             SQ2254.2
063600         MOVE   "V11-2, 1.3.5" TO ANSI-REFERENCE                  SQ2254.2
063700         PERFORM FAIL.                                            SQ2254.2
063800*                                                                 SQ2254.2
063900     ADD     1 TO REC-CT.                                         SQ2254.2
064000     IF  SQ-FS1-STATUS NOT = "35"                                 SQ2254.2
064100         MOVE   "INCORRECT STATUS CODE RETURNED" TO RE-MARK       SQ2254.2
064200         MOVE   "VII-4, 1.5.3(3)C" TO ANSI-REFERENCE              SQ2254.2
064300         MOVE   "35" TO CORRECT-A                                 SQ2254.2
064400         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2254.2
064500         PERFORM FAIL                                             SQ2254.2
064600     ELSE                                                         SQ2254.2
064700         PERFORM PASS.                                            SQ2254.2
064800*                                                                 SQ2254.2
064900*                                                                 SQ2254.2
065000 CCVS-EXIT SECTION.                                               SQ2254.2
065100 CCVS-999999.                                                     SQ2254.2
065200     GO TO   CLOSE-FILES.                                         SQ2254.2
      *END-OF,SQ225A                                                                  
