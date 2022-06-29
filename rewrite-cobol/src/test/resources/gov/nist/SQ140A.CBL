      *HEADER,COBOL,SQ140A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1404.2
000200 PROGRAM-ID.                                                      SQ1404.2
000300     SQ140A.                                                      SQ1404.2
000400****************************************************************  SQ1404.2
000500*                                                              *  SQ1404.2
000600*    VALIDATION FOR:-                                          *  SQ1404.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1404.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1404.2
000900*                             REVISED 1986, AUGUST             *  SQ1404.2
001000*                                                              *  SQ1404.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1404.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1404.2
001300*                                                              *  SQ1404.2
001400****************************************************************  SQ1404.2
001500*                                                              *  SQ1404.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1404.2
001700*                                                              *  SQ1404.2
001800*            X-01   SEQUENTIAL MAGNETIC TAPE FILE.             *  SQ1404.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1404.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1404.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1404.2
002200*                                                              *  SQ1404.2
002300*                                                              *  SQ1404.2
002400****************************************************************  SQ1404.2
002500*                                                              *  SQ1404.2
002600*    SPLIT FROM SQ125A, THIS PROGRAM REPEATS THE SEQUENCE OF   *  SQ1404.2
002700*    FILE HANDLING ROUTINES IN ORDER TO CARRY OUT THE ISOLATED *  SQ1404.2
002800*    SPLIT TEST THAT CHECKS FOR THE CORRECT RESPONSE TO AN     *  SQ1404.2
002900*    OPEN FOR OUTPUT FOR A MAGNETIC TAPE FILE WHICH IS ALREADY *  SQ1404.2
003000*    OPEN IN THE OUTPUT MODE.  (SEE SQ125A).                   *  SQ1404.2
003100*                                                              *  SQ1404.2
003200****************************************************************  SQ1404.2
003300*                                                                 SQ1404.2
003400 ENVIRONMENT DIVISION.                                            SQ1404.2
003500 CONFIGURATION SECTION.                                           SQ1404.2
003600 SOURCE-COMPUTER.                                                 SQ1404.2
003700     XXXXX082.                                                    SQ1404.2
003800 OBJECT-COMPUTER.                                                 SQ1404.2
003900     XXXXX083.                                                    SQ1404.2
004000*                                                                 SQ1404.2
004100 INPUT-OUTPUT SECTION.                                            SQ1404.2
004200 FILE-CONTROL.                                                    SQ1404.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ1404.2
004400     XXXXX055.                                                    SQ1404.2
004500*                                                                 SQ1404.2
004600*    SELECT RAW-DATA   ASSIGN TO                                  SQ1404.2
004700*    XXXXX062                                                     SQ1404.2
004800*          ORGANIZATION IS INDEXED                                SQ1404.2
004900*          ACCESS MODE  IS RANDOM                                 SQ1404.2
005000*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1404.2
005100*                                                                 SQ1404.2
005200     SELECT SQ-FS1 ASSIGN TO                                      SQ1404.2
005300     XXXXX001                                                     SQ1404.2
005400            FILE STATUS IS SQ-FS1-STATUS.                         SQ1404.2
005500*                                                                 SQ1404.2
005600*                                                                 SQ1404.2
005700 DATA DIVISION.                                                   SQ1404.2
005800 FILE SECTION.                                                    SQ1404.2
005900 FD  PRINT-FILE                                                   SQ1404.2
006000C    LABEL RECORDS                                                SQ1404.2
006100C    XXXXX084                                                     SQ1404.2
006200C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1404.2
006300               .                                                  SQ1404.2
006400 01  PRINT-REC    PICTURE X(120).                                 SQ1404.2
006500 01  DUMMY-RECORD PICTURE X(120).                                 SQ1404.2
006600*                                                                 SQ1404.2
006700*FD  RAW-DATA.                                                    SQ1404.2
006800*01  RAW-DATA-SATZ.                                               SQ1404.2
006900*    05  RAW-DATA-KEY        PIC X(6).                            SQ1404.2
007000*    05  C-DATE              PIC 9(6).                            SQ1404.2
007100*    05  C-TIME              PIC 9(8).                            SQ1404.2
007200*    05  NO-OF-TESTS         PIC 99.                              SQ1404.2
007300*    05  C-OK                PIC 999.                             SQ1404.2
007400*    05  C-ALL               PIC 999.                             SQ1404.2
007500*    05  C-FAIL              PIC 999.                             SQ1404.2
007600*    05  C-DELETED           PIC 999.                             SQ1404.2
007700*    05  C-INSPECT           PIC 999.                             SQ1404.2
007800*    05  C-NOTE              PIC X(13).                           SQ1404.2
007900*    05  C-INDENT            PIC X.                               SQ1404.2
008000*    05  C-ABORT             PIC X(8).                            SQ1404.2
008100*                                                                 SQ1404.2
008200 FD  SQ-FS1                                                       SQ1404.2
008300C    LABEL RECORD IS STANDARD                                     SQ1404.2
008400                .                                                 SQ1404.2
008500 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1404.2
008600*                                                                 SQ1404.2
008700 WORKING-STORAGE SECTION.                                         SQ1404.2
008800*                                                                 SQ1404.2
008900***************************************************************   SQ1404.2
009000*                                                             *   SQ1404.2
009100*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1404.2
009200*                                                             *   SQ1404.2
009300***************************************************************   SQ1404.2
009400*                                                                 SQ1404.2
009500 01  SQ-FS1-STATUS.                                               SQ1404.2
009600   03  SQ-FS1-KEY-1   PIC X.                                      SQ1404.2
009700   03  SQ-FS1-KEY-2   PIC X.                                      SQ1404.2
009800*                                                                 SQ1404.2
009900 01  DECL-EXEC-SW  PIC 9.                                         SQ1404.2
010000*                                                                 SQ1404.2
010100*                                                                 SQ1404.2
010200***************************************************************   SQ1404.2
010300*                                                             *   SQ1404.2
010400*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1404.2
010500*                                                             *   SQ1404.2
010600***************************************************************   SQ1404.2
010700*                                                                 SQ1404.2
010800 01  REC-SKEL-SUB   PIC 99.                                       SQ1404.2
010900*                                                                 SQ1404.2
011000 01  FILE-RECORD-INFORMATION-REC.                                 SQ1404.2
011100     03 FILE-RECORD-INFO-SKELETON.                                SQ1404.2
011200        05 FILLER                 PICTURE X(48)       VALUE       SQ1404.2
011300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1404.2
011400        05 FILLER                 PICTURE X(46)       VALUE       SQ1404.2
011500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1404.2
011600        05 FILLER                 PICTURE X(26)       VALUE       SQ1404.2
011700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1404.2
011800        05 FILLER                 PICTURE X(37)       VALUE       SQ1404.2
011900             ",RECKEY=                             ".             SQ1404.2
012000        05 FILLER                 PICTURE X(38)       VALUE       SQ1404.2
012100             ",ALTKEY1=                             ".            SQ1404.2
012200        05 FILLER                 PICTURE X(38)       VALUE       SQ1404.2
012300             ",ALTKEY2=                             ".            SQ1404.2
012400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1404.2
012500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1404.2
012600        05 FILE-RECORD-INFO-P1-120.                               SQ1404.2
012700           07 FILLER              PIC X(5).                       SQ1404.2
012800           07 XFILE-NAME          PIC X(6).                       SQ1404.2
012900           07 FILLER              PIC X(8).                       SQ1404.2
013000           07 XRECORD-NAME        PIC X(6).                       SQ1404.2
013100           07 FILLER              PIC X(1).                       SQ1404.2
013200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1404.2
013300           07 FILLER              PIC X(7).                       SQ1404.2
013400           07 XRECORD-NUMBER      PIC 9(6).                       SQ1404.2
013500           07 FILLER              PIC X(6).                       SQ1404.2
013600           07 UPDATE-NUMBER       PIC 9(2).                       SQ1404.2
013700           07 FILLER              PIC X(5).                       SQ1404.2
013800           07 ODO-NUMBER          PIC 9(4).                       SQ1404.2
013900           07 FILLER              PIC X(5).                       SQ1404.2
014000           07 XPROGRAM-NAME       PIC X(5).                       SQ1404.2
014100           07 FILLER              PIC X(7).                       SQ1404.2
014200           07 XRECORD-LENGTH      PIC 9(6).                       SQ1404.2
014300           07 FILLER              PIC X(7).                       SQ1404.2
014400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1404.2
014500           07 FILLER              PIC X(1).                       SQ1404.2
014600           07 XBLOCK-SIZE         PIC 9(4).                       SQ1404.2
014700           07 FILLER              PIC X(6).                       SQ1404.2
014800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1404.2
014900           07 FILLER              PIC X(5).                       SQ1404.2
015000           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1404.2
015100           07 FILLER              PIC X(6).                       SQ1404.2
015200           07 XLABEL-TYPE         PIC X(1).                       SQ1404.2
015300        05 FILE-RECORD-INFO-P121-240.                             SQ1404.2
015400           07 FILLER              PIC X(8).                       SQ1404.2
015500           07 XRECORD-KEY         PIC X(29).                      SQ1404.2
015600           07 FILLER              PIC X(9).                       SQ1404.2
015700           07 ALTERNATE-KEY1      PIC X(29).                      SQ1404.2
015800           07 FILLER              PIC X(9).                       SQ1404.2
015900           07 ALTERNATE-KEY2      PIC X(29).                      SQ1404.2
016000           07 FILLER              PIC X(7).                       SQ1404.2
016100*                                                                 SQ1404.2
016200 01  TEST-RESULTS.                                                SQ1404.2
016300     02 FILLER              PIC X      VALUE SPACE.               SQ1404.2
016400     02  PAR-NAME.                                                SQ1404.2
016500       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1404.2
016600       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1404.2
016700       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1404.2
016800     02 FILLER              PIC X      VALUE SPACE.               SQ1404.2
016900     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1404.2
017000     02 FILLER              PIC X      VALUE SPACE.               SQ1404.2
017100     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1404.2
017200     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1404.2
017300     02 RE-MARK             PIC X(61).                            SQ1404.2
017400 01  TEST-COMPUTED.                                               SQ1404.2
017500   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1404.2
017600   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1404.2
017700   02 COMPUTED-X.                                                 SQ1404.2
017800     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1404.2
017900     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1404.2
018000     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1404.2
018100     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1404.2
018200     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1404.2
018300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1404.2
018400        04 COMPUTED-18V0                   PIC -9(18).            SQ1404.2
018500        04 FILLER                          PIC X.                 SQ1404.2
018600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1404.2
018700 01  TEST-CORRECT.                                                SQ1404.2
018800     02 FILLER PIC X(30) VALUE SPACE.                             SQ1404.2
018900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1404.2
019000     02 CORRECT-X.                                                SQ1404.2
019100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1404.2
019200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1404.2
019300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1404.2
019400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1404.2
019500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1404.2
019600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1404.2
019700         04 CORRECT-18V0                     PIC -9(18).          SQ1404.2
019800         04 FILLER                           PIC X.               SQ1404.2
019900     03 FILLER PIC X(2) VALUE SPACE.                              SQ1404.2
020000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1404.2
020100*                                                                 SQ1404.2
020200 01  CCVS-C-1.                                                    SQ1404.2
020300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1404.2
020400     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1404.2
020500     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1404.2
020600     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1404.2
020700     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1404.2
020800     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1404.2
020900     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1404.2
021000     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1404.2
021100 01  CCVS-C-2.                                                    SQ1404.2
021200     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1404.2
021300     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1404.2
021400     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1404.2
021500     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1404.2
021600     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1404.2
021700*                                                                 SQ1404.2
021800 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1404.2
021900 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1404.2
022000 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1404.2
022100 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1404.2
022200 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1404.2
022300 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1404.2
022400 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1404.2
022500 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1404.2
022600 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1404.2
022700 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1404.2
022800 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1404.2
022900 01  CCVS-H-1.                                                    SQ1404.2
023000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1404.2
023100     02  FILLER          PIC X(42)    VALUE                       SQ1404.2
023200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1404.2
023300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1404.2
023400 01  CCVS-H-2A.                                                   SQ1404.2
023500   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1404.2
023600   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1404.2
023700   02  FILLER            PIC XXXX   VALUE                         SQ1404.2
023800     "4.2 ".                                                      SQ1404.2
023900   02  FILLER            PIC X(28)  VALUE                         SQ1404.2
024000            " COPY - NOT FOR DISTRIBUTION".                       SQ1404.2
024100   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1404.2
024200*                                                                 SQ1404.2
024300 01  CCVS-H-2B.                                                   SQ1404.2
024400   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1404.2
024500   02  TEST-ID           PIC X(9).                                SQ1404.2
024600   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1404.2
024700   02  FILLER            PIC X(12)  VALUE                         SQ1404.2
024800     " HIGH       ".                                              SQ1404.2
024900   02  FILLER            PIC X(22)  VALUE                         SQ1404.2
025000            " LEVEL VALIDATION FOR ".                             SQ1404.2
025100   02  FILLER            PIC X(58)  VALUE                         SQ1404.2
025200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1404.2
025300 01  CCVS-H-3.                                                    SQ1404.2
025400     02  FILLER          PIC X(34)  VALUE                         SQ1404.2
025500            " FOR OFFICIAL USE ONLY    ".                         SQ1404.2
025600     02  FILLER          PIC X(58)  VALUE                         SQ1404.2
025700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1404.2
025800     02  FILLER          PIC X(28)  VALUE                         SQ1404.2
025900            "  COPYRIGHT   1985,1986 ".                           SQ1404.2
026000 01  CCVS-E-1.                                                    SQ1404.2
026100     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1404.2
026200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1404.2
026300     02 ID-AGAIN         PIC X(9).                                SQ1404.2
026400     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1404.2
026500 01  CCVS-E-2.                                                    SQ1404.2
026600     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1404.2
026700     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1404.2
026800     02  CCVS-E-2-2.                                              SQ1404.2
026900         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1404.2
027000         03 FILLER         PIC X      VALUE SPACE.                SQ1404.2
027100         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1404.2
027200            "ERRORS ENCOUNTERED".                                 SQ1404.2
027300 01  CCVS-E-3.                                                    SQ1404.2
027400     02  FILLER          PIC X(22)  VALUE                         SQ1404.2
027500            " FOR OFFICIAL USE ONLY".                             SQ1404.2
027600     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1404.2
027700     02  FILLER          PIC X(58)  VALUE                         SQ1404.2
027800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1404.2
027900     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1404.2
028000     02  FILLER          PIC X(20)  VALUE                         SQ1404.2
028100             " COPYRIGHT 1985,1986".                              SQ1404.2
028200 01  CCVS-E-4.                                                    SQ1404.2
028300     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1404.2
028400     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1404.2
028500     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1404.2
028600     02 FILLER           PIC X(40)  VALUE                         SQ1404.2
028700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1404.2
028800 01  XXINFO.                                                      SQ1404.2
028900     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1404.2
029000     02 INFO-TEXT.                                                SQ1404.2
029100       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1404.2
029200       04 XXCOMPUTED         PIC X(20).                           SQ1404.2
029300       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1404.2
029400       04 XXCORRECT          PIC X(20).                           SQ1404.2
029500     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1404.2
029600 01  HYPHEN-LINE.                                                 SQ1404.2
029700     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1404.2
029800     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1404.2
029900-    "*****************************************".                 SQ1404.2
030000     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1404.2
030100-    "******************************".                            SQ1404.2
030200 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1404.2
030300     "SQ140A".                                                    SQ1404.2
030400*                                                                 SQ1404.2
030500*                                                                 SQ1404.2
030600 PROCEDURE DIVISION.                                              SQ1404.2
030700 DECLARATIVES.                                                    SQ1404.2
030800 SQ140A-DECLARATIVE-001-SECT SECTION.                             SQ1404.2
030900     USE AFTER STANDARD EXCEPTION PROCEDURE SQ-FS1.               SQ1404.2
031000 INPUT-ERROR-PROCEDURE.                                           SQ1404.2
031100     IF DECL-EXEC-SW NOT = 9                                      SQ1404.2
031200         GO TO   NOT-DECL-9.                                      SQ1404.2
031300*                                                                 SQ1404.2
031400*    DECLARATIVE PROCEDURE ENTERED FROM SECOND OPEN OUTPUT        SQ1404.2
031500*                                                                 SQ1404.2
031600 DECL-OPEN-TEST.                                                  SQ1404.2
031700     MOVE    SPACE TO DUMMY-RECORD                                SQ1404.2
031800     PERFORM DECL-WRITE-LINE                                      SQ1404.2
031900     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ1404.2
032000               TO DUMMY-RECORD                                    SQ1404.2
032100     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1404.2
032200     GO TO   END-DECLS.                                           SQ1404.2
032300*                                                                 SQ1404.2
032400*                                                                 SQ1404.2
032500 NOT-DECL-9.                                                      SQ1404.2
032600     MOVE   "NOT-DECL-9"  TO PAR-NAME.                            SQ1404.2
032700     MOVE    DECL-EXEC-SW TO COMPUTED-18V0.                       SQ1404.2
032800     MOVE    9            TO CORRECT-18V0.                        SQ1404.2
032900     MOVE   "UNEXPECTED ENTRY TO DECLARATIVES" TO RE-MARK.        SQ1404.2
033000     PERFORM DECL-FAIL.                                           SQ1404.2
033100     GO TO   END-DECLS.                                           SQ1404.2
033200*                                                                 SQ1404.2
033300*                                                                 SQ1404.2
033400*                                                                 SQ1404.2
033500 DECL-PASS.                                                       SQ1404.2
033600     MOVE   "PASS " TO P-OR-F.                                    SQ1404.2
033700     ADD     1 TO PASS-COUNTER.                                   SQ1404.2
033800     PERFORM DECL-PRINT-DETAIL.                                   SQ1404.2
033900*                                                                 SQ1404.2
034000 DECL-FAIL.                                                       SQ1404.2
034100     MOVE   "FAIL*" TO P-OR-F.                                    SQ1404.2
034200     ADD     1 TO ERROR-COUNTER.                                  SQ1404.2
034300     PERFORM DECL-PRINT-DETAIL.                                   SQ1404.2
034400*                                                                 SQ1404.2
034500 DECL-PRINT-DETAIL.                                               SQ1404.2
034600     IF REC-CT NOT EQUAL TO ZERO                                  SQ1404.2
034700             MOVE "." TO PARDOT-X                                 SQ1404.2
034800             MOVE REC-CT TO DOTVALUE.                             SQ1404.2
034900     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1404.2
035000     PERFORM DECL-WRITE-LINE.                                     SQ1404.2
035100     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1404.2
035200         PERFORM DECL-WRITE-LINE                                  SQ1404.2
035300         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1404.2
035400     ELSE                                                         SQ1404.2
035500         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1404.2
035600     MOVE    SPACE TO P-OR-F.                                     SQ1404.2
035700     MOVE    SPACE TO COMPUTED-X.                                 SQ1404.2
035800     MOVE    SPACE TO CORRECT-X.                                  SQ1404.2
035900     IF REC-CT EQUAL TO ZERO                                      SQ1404.2
036000         MOVE    SPACE TO PAR-NAME.                               SQ1404.2
036100     MOVE    SPACE TO RE-MARK.                                    SQ1404.2
036200*                                                                 SQ1404.2
036300 DECL-WRITE-LINE.                                                 SQ1404.2
036400     ADD     1 TO RECORD-COUNT.                                   SQ1404.2
036500*    IF RECORD-COUNT GREATER 50                                   SQ1404.2
036600*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1404.2
036700*        MOVE    SPACE TO DUMMY-RECORD                            SQ1404.2
036800*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1404.2
036900*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1404.2
037000*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1404.2
037100*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1404.2
037200*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1404.2
037300*        PERFORM DECL-WRT-LN                                      SQ1404.2
037400*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1404.2
037500*        MOVE    ZERO TO RECORD-COUNT.                            SQ1404.2
037600     PERFORM DECL-WRT-LN.                                         SQ1404.2
037700*                                                                 SQ1404.2
037800 DECL-WRT-LN.                                                     SQ1404.2
037900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1404.2
038000     MOVE    SPACE TO DUMMY-RECORD.                               SQ1404.2
038100*                                                                 SQ1404.2
038200 DECL-FAIL-ROUTINE.                                               SQ1404.2
038300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1404.2
038400     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1404.2
038500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1404.2
038600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1404.2
038700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1404.2
038800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1404.2
038900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1404.2
039000     GO TO   DECL-FAIL-EX.                                        SQ1404.2
039100 DECL-FAIL-WRITE.                                                 SQ1404.2
039200     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1404.2
039300     PERFORM DECL-WRITE-LINE                                      SQ1404.2
039400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1404.2
039500     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1404.2
039600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1404.2
039700     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1404.2
039800 DECL-FAIL-EX.                                                    SQ1404.2
039900     EXIT.                                                        SQ1404.2
040000*                                                                 SQ1404.2
040100 DECL-BAIL.                                                       SQ1404.2
040200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1404.2
040300     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1404.2
040400 DECL-BAIL-WRITE.                                                 SQ1404.2
040500     MOVE    CORRECT-A TO XXCORRECT.                              SQ1404.2
040600     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1404.2
040700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1404.2
040800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1404.2
040900 DECL-BAIL-EX.                                                    SQ1404.2
041000     EXIT.                                                        SQ1404.2
041100*                                                                 SQ1404.2
041200 END-DECLS.                                                       SQ1404.2
041300     MOVE    ZERO TO DECL-EXEC-SW.                                SQ1404.2
041400 END DECLARATIVES.                                                SQ1404.2
041500*                                                                 SQ1404.2
041600*                                                                 SQ1404.2
041700 CCVS1 SECTION.                                                   SQ1404.2
041800 OPEN-FILES.                                                      SQ1404.2
041900*    OPEN    I-O RAW-DATA.                                        SQ1404.2
042000*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1404.2
042100*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1404.2
042200*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1404.2
042300*    ADD     1           TO C-NO-OF-TESTS.                        SQ1404.2
042400*    ACCEPT  C-DATE      FROM DATE.                               SQ1404.2
042500*    ACCEPT  C-TIME      FROM TIME.                               SQ1404.2
042600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1404.2
042700*END-E-1.                                                         SQ1404.2
042800*    CLOSE   RAW-DATA.                                            SQ1404.2
042900     OPEN    OUTPUT PRINT-FILE.                                   SQ1404.2
043000     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1404.2
043100     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1404.2
043200     MOVE    SPACE TO TEST-RESULTS.                               SQ1404.2
043300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1404.2
043400     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1404.2
043500     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1404.2
043600     GO TO CCVS1-EXIT.                                            SQ1404.2
043700*                                                                 SQ1404.2
043800 CCVS-INIT-FILE.                                                  SQ1404.2
043900     ADD     1 TO REC-SKL-SUB.                                    SQ1404.2
044000     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1404.2
044100                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1404.2
044200*                                                                 SQ1404.2
044300 CLOSE-FILES.                                                     SQ1404.2
044400     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1404.2
044500     CLOSE   PRINT-FILE.                                          SQ1404.2
044600*    OPEN    I-O RAW-DATA.                                        SQ1404.2
044700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1404.2
044800*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1404.2
044900*    MOVE   "OK.     " TO C-ABORT.                                SQ1404.2
045000*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1404.2
045100*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1404.2
045200*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1404.2
045300*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1404.2
045400*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1404.2
045500*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1404.2
045600*END-E-2.                                                         SQ1404.2
045700*    CLOSE   RAW-DATA.                                            SQ1404.2
045800 TERMINATE-CCVS.                                                  SQ1404.2
045900*    EXIT    PROGRAM.                                             SQ1404.2
046000     STOP    RUN.                                                 SQ1404.2
046100*                                                                 SQ1404.2
046200 INSPT.                                                           SQ1404.2
046300     MOVE   "INSPT" TO P-OR-F.                                    SQ1404.2
046400     ADD     1 TO INSPECT-COUNTER.                                SQ1404.2
046500     PERFORM PRINT-DETAIL.                                        SQ1404.2
046600                                                                  SQ1404.2
046700 PASS.                                                            SQ1404.2
046800     MOVE   "PASS " TO P-OR-F.                                    SQ1404.2
046900     ADD     1 TO PASS-COUNTER.                                   SQ1404.2
047000     PERFORM PRINT-DETAIL.                                        SQ1404.2
047100*                                                                 SQ1404.2
047200 FAIL.                                                            SQ1404.2
047300     MOVE   "FAIL*" TO P-OR-F.                                    SQ1404.2
047400     ADD     1 TO ERROR-COUNTER.                                  SQ1404.2
047500     PERFORM PRINT-DETAIL.                                        SQ1404.2
047600*                                                                 SQ1404.2
047700 DE-LETE.                                                         SQ1404.2
047800     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1404.2
047900     MOVE   "*****" TO P-OR-F.                                    SQ1404.2
048000     ADD     1 TO DELETE-COUNTER.                                 SQ1404.2
048100     PERFORM PRINT-DETAIL.                                        SQ1404.2
048200*                                                                 SQ1404.2
048300 PRINT-DETAIL.                                                    SQ1404.2
048400     IF REC-CT NOT EQUAL TO ZERO                                  SQ1404.2
048500         MOVE   "." TO PARDOT-X                                   SQ1404.2
048600         MOVE    REC-CT TO DOTVALUE.                              SQ1404.2
048700     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1404.2
048800     PERFORM WRITE-LINE.                                          SQ1404.2
048900     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1404.2
049000         PERFORM WRITE-LINE                                       SQ1404.2
049100         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1404.2
049200     ELSE                                                         SQ1404.2
049300         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1404.2
049400     MOVE    SPACE TO P-OR-F.                                     SQ1404.2
049500     MOVE    SPACE TO COMPUTED-X.                                 SQ1404.2
049600     MOVE    SPACE TO CORRECT-X.                                  SQ1404.2
049700     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1404.2
049800     MOVE    SPACE TO RE-MARK.                                    SQ1404.2
049900*                                                                 SQ1404.2
050000 HEAD-ROUTINE.                                                    SQ1404.2
050100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1404.2
050200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1404.2
050300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1404.2
050400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1404.2
050500 COLUMN-NAMES-ROUTINE.                                            SQ1404.2
050600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1404.2
050700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1404.2
050800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1404.2
050900 END-ROUTINE.                                                     SQ1404.2
051000     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1404.2
051100     PERFORM WRITE-LINE 5 TIMES.                                  SQ1404.2
051200 END-RTN-EXIT.                                                    SQ1404.2
051300     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1404.2
051400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1404.2
051500*                                                                 SQ1404.2
051600 END-ROUTINE-1.                                                   SQ1404.2
051700     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1404.2
051800     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1404.2
051900     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1404.2
052000     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1404.2
052100     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1404.2
052200     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1404.2
052300     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1404.2
052400     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1404.2
052500     PERFORM WRITE-LINE.                                          SQ1404.2
052600     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1404.2
052700     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1404.2
052800         MOVE   "NO " TO ERROR-TOTAL                              SQ1404.2
052900     ELSE                                                         SQ1404.2
053000         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1404.2
053100     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1404.2
053200     PERFORM WRITE-LINE.                                          SQ1404.2
053300 END-ROUTINE-13.                                                  SQ1404.2
053400     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1404.2
053500         MOVE   "NO " TO ERROR-TOTAL                              SQ1404.2
053600     ELSE                                                         SQ1404.2
053700         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1404.2
053800     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1404.2
053900     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1404.2
054000     PERFORM WRITE-LINE.                                          SQ1404.2
054100     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1404.2
054200         MOVE   "NO " TO ERROR-TOTAL                              SQ1404.2
054300     ELSE                                                         SQ1404.2
054400         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1404.2
054500     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1404.2
054600     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1404.2
054700     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1404.2
054800*                                                                 SQ1404.2
054900 WRITE-LINE.                                                      SQ1404.2
055000     ADD     1 TO RECORD-COUNT.                                   SQ1404.2
055100*    IF RECORD-COUNT GREATER 50                                   SQ1404.2
055200*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1404.2
055300*        MOVE  SPACE TO DUMMY-RECORD                              SQ1404.2
055400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1404.2
055500*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1404.2
055600*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1404.2
055700*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1404.2
055800*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1404.2
055900*        MOVE  ZERO TO RECORD-COUNT.                              SQ1404.2
056000     PERFORM WRT-LN.                                              SQ1404.2
056100*                                                                 SQ1404.2
056200 WRT-LN.                                                          SQ1404.2
056300     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1404.2
056400     MOVE    SPACE TO DUMMY-RECORD.                               SQ1404.2
056500 BLANK-LINE-PRINT.                                                SQ1404.2
056600     PERFORM WRT-LN.                                              SQ1404.2
056700 FAIL-ROUTINE.                                                    SQ1404.2
056800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1404.2
056900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1404.2
057000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1404.2
057100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1404.2
057200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1404.2
057300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1404.2
057400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1404.2
057500     GO TO   FAIL-ROUTINE-EX.                                     SQ1404.2
057600 FAIL-ROUTINE-WRITE.                                              SQ1404.2
057700     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1404.2
057800     PERFORM WRITE-LINE                                           SQ1404.2
057900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1404.2
058000     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1404.2
058100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1404.2
058200     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1404.2
058300 FAIL-ROUTINE-EX.                                                 SQ1404.2
058400     EXIT.                                                        SQ1404.2
058500 BAIL-OUT.                                                        SQ1404.2
058600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1404.2
058700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1404.2
058800 BAIL-OUT-WRITE.                                                  SQ1404.2
058900     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1404.2
059000     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1404.2
059100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1404.2
059200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1404.2
059300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1404.2
059400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1404.2
059500 BAIL-OUT-EX.                                                     SQ1404.2
059600     EXIT.                                                        SQ1404.2
059700 CCVS1-EXIT.                                                      SQ1404.2
059800     EXIT.                                                        SQ1404.2
059900*                                                                 SQ1404.2
060000****************************************************************  SQ1404.2
060100*                                                              *  SQ1404.2
060200*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1404.2
060300*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1404.2
060400*                                                              *  SQ1404.2
060500****************************************************************  SQ1404.2
060600*                                                                 SQ1404.2
060700 SECT-SQ140A-MAIN SECTION.                                        SQ1404.2
060800*                                                                 SQ1404.2
060900*    THE FIRST ACTION IS TO CREATE THE FILE BY MEANS OF AN        SQ1404.2
061000*    OPEN OUTPUT STATEMENT.                                       SQ1404.2
061100*                                                                 SQ1404.2
061200 SEQ-INIT-01.                                                     SQ1404.2
061300*                                                                 SQ1404.2
061400     MOVE    1 TO REC-CT                                          SQ1404.2
061500     MOVE   "CREATE FILE, OPEN OUTPUT" TO FEATURE                 SQ1404.2
061600     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME                          SQ1404.2
061700     MOVE    1 TO DECL-EXEC-SW                                    SQ1404.2
061800     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1404.2
061900 SEQ-TEST-OP-01.                                                  SQ1404.2
062000     OPEN    OUTPUT SQ-FS1.                                       SQ1404.2
062100 SEQ-INIT-02.                                                     SQ1404.2
062200     MOVE    1 TO REC-CT                                          SQ1404.2
062300     MOVE   "OPEN OUTPUT ON OPEN FILE" TO FEATURE                 SQ1404.2
062400     MOVE   "SEQ-TEST-OP-02" TO PAR-NAME                          SQ1404.2
062500     MOVE    9 TO DECL-EXEC-SW                                    SQ1404.2
062600     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1404.2
062700 SEQ-TEST-OP-02.                                                  SQ1404.2
062800     OPEN    OUTPUT SQ-FS1.                                       SQ1404.2
062900*                                                                 SQ1404.2
063000*    CHECK EXECUTION OF DECLARATIVE.                              SQ1404.2
063100*                                                                 SQ1404.2
063200     MOVE   "SEQ-TEST-OP-02" TO PAR-NAME.                         SQ1404.2
063300     MOVE    1 TO REC-CT.                                         SQ1404.2
063400 SEQ-TEST-02-01-END.                                              SQ1404.2
063500*                                                                 SQ1404.2
063600*    CHECK THE I-O STATUS VALUE RETURNED BY THE SECOND OPEN.      SQ1404.2
063700*                                                                 SQ1404.2
063800     ADD     1 TO REC-CT.                                         SQ1404.2
063900     GO TO   SEQ-TEST-OP-02-02.                                   SQ1404.2
064000 SEQ-DELETE-02-02.                                                SQ1404.2
064100     PERFORM DE-LETE.                                             SQ1404.2
064200     GO TO   SEQ-TEST-02-02-END.                                  SQ1404.2
064300 SEQ-TEST-OP-02-02.                                               SQ1404.2
064400     IF  SQ-FS1-STATUS = "41"                                     SQ1404.2
064500         PERFORM PASS                                             SQ1404.2
064600     ELSE                                                         SQ1404.2
064700         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1404.2
064800         MOVE   "41" TO CORRECT-A                                 SQ1404.2
064900         MOVE   "UNEXPECTED I-O STATUS CODE FROM OPEN OUTPUT"     SQ1404.2
065000                    TO RE-MARK                                    SQ1404.2
065100         MOVE   "VII-4, 1.5.3(4)A" TO ANSI-REFERENCE              SQ1404.2
065200         PERFORM FAIL.                                            SQ1404.2
065300 SEQ-TEST-02-02-END.                                              SQ1404.2
065400*                                                                 SQ1404.2
065500*                                                                 SQ1404.2
065600 CCVS-EXIT SECTION.                                               SQ1404.2
065700 CCVS-999999.                                                     SQ1404.2
065800     GO TO   CLOSE-FILES.                                         SQ1404.2
      *END-OF,SQ140A                                                                  
