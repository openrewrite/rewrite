      *HEADER,COBOL,SQ229A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2294.2
000200 PROGRAM-ID.                                                      SQ2294.2
000300     SQ229A.                                                      SQ2294.2
000400****************************************************************  SQ2294.2
000500*                                                              *  SQ2294.2
000600*    VALIDATION FOR:-                                          *  SQ2294.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2294.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ2294.2
000900*                                                              *  SQ2294.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ2294.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2294.2
001200*                                                              *  SQ2294.2
001300****************************************************************  SQ2294.2
001400*                                                              *  SQ2294.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2294.2
001600*                                                              *  SQ2294.2
001700*            X-01   SEQUENTIAL TAPE                            *  SQ2294.2
001800*            X-55   SYSTEM PRINTER                             *  SQ2294.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ2294.2
002000*            X-83   OBJECT-COMPUTER                            *  SQ2294.2
002100*            X-84   LABEL RECORDS OPTION.                      *  SQ2294.2
002200*                                                              *  SQ2294.2
002300****************************************************************  SQ2294.2
002400*                                                              *  SQ2294.2
002500*    THIS PROGRM CHECKS FOR THE CORRECT RESPONSE TO READING    *  SQ2294.2
002600*    A FILE OPEN IN THE EXTEND MODE.  THE TEST FOR CORRECT     *  SQ2294.2
002700*    I-O STATUS CODE 47 IS IN THE DECLARATIVES.  AN ABNORMAL   *  SQ2294.2
002800*    TERMINATION IS POSSIBLE AFTER THE TEST OF THE I-O STATUS  *  SQ2294.2
002900*    CODE IS ACCOMPLISHED BUT BEFORE CONTROL IS RETURNED TO THE*  SQ2294.2
003000*    MAIN LINE CODE.                                           *  SQ2294.2
003100*                                                              *  SQ2294.2
003200****************************************************************  SQ2294.2
003300*                                                                 SQ2294.2
003400 ENVIRONMENT DIVISION.                                            SQ2294.2
003500 CONFIGURATION SECTION.                                           SQ2294.2
003600 SOURCE-COMPUTER.                                                 SQ2294.2
003700     XXXXX082.                                                    SQ2294.2
003800 OBJECT-COMPUTER.                                                 SQ2294.2
003900     XXXXX083.                                                    SQ2294.2
004000*                                                                 SQ2294.2
004100 INPUT-OUTPUT SECTION.                                            SQ2294.2
004200 FILE-CONTROL.                                                    SQ2294.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ2294.2
004400     XXXXX055.                                                    SQ2294.2
004500*                                                                 SQ2294.2
004600     SELECT SQ-FS1 ASSIGN TO                                      SQ2294.2
004700     XXXXX001                                                     SQ2294.2
004800            FILE STATUS IS SQ-FS1-STATUS.                         SQ2294.2
004900*                                                                 SQ2294.2
005000*                                                                 SQ2294.2
005100 DATA DIVISION.                                                   SQ2294.2
005200 FILE SECTION.                                                    SQ2294.2
005300 FD  PRINT-FILE                                                   SQ2294.2
005400C    LABEL RECORDS                                                SQ2294.2
005500C    XXXXX084                                                     SQ2294.2
005600C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2294.2
005700               .                                                  SQ2294.2
005800 01  PRINT-REC    PICTURE X(120).                                 SQ2294.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 SQ2294.2
006000*                                                                 SQ2294.2
006100 FD  SQ-FS1                                                       SQ2294.2
006200C    LABEL RECORD IS STANDARD                                     SQ2294.2
006300                .                                                 SQ2294.2
006400 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2294.2
006500*                                                                 SQ2294.2
006600 WORKING-STORAGE SECTION.                                         SQ2294.2
006700*                                                                 SQ2294.2
006800***************************************************************   SQ2294.2
006900*                                                             *   SQ2294.2
007000*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2294.2
007100*                                                             *   SQ2294.2
007200***************************************************************   SQ2294.2
007300*                                                                 SQ2294.2
007400 01  SQ-FS1-STATUS.                                               SQ2294.2
007500   03  SQ-FS1-KEY-1   PIC X.                                      SQ2294.2
007600   03  SQ-FS1-KEY-2   PIC X.                                      SQ2294.2
007700*                                                                 SQ2294.2
007800***************************************************************   SQ2294.2
007900*                                                             *   SQ2294.2
008000*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2294.2
008100*                                                             *   SQ2294.2
008200***************************************************************   SQ2294.2
008300*                                                                 SQ2294.2
008400 01  REC-SKEL-SUB   PIC 99.                                       SQ2294.2
008500*                                                                 SQ2294.2
008600 01  FILE-RECORD-INFORMATION-REC.                                 SQ2294.2
008700     03 FILE-RECORD-INFO-SKELETON.                                SQ2294.2
008800        05 FILLER                 PICTURE X(48)       VALUE       SQ2294.2
008900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2294.2
009000        05 FILLER                 PICTURE X(46)       VALUE       SQ2294.2
009100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2294.2
009200        05 FILLER                 PICTURE X(26)       VALUE       SQ2294.2
009300             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2294.2
009400        05 FILLER                 PICTURE X(37)       VALUE       SQ2294.2
009500             ",RECKEY=                             ".             SQ2294.2
009600        05 FILLER                 PICTURE X(38)       VALUE       SQ2294.2
009700             ",ALTKEY1=                             ".            SQ2294.2
009800        05 FILLER                 PICTURE X(38)       VALUE       SQ2294.2
009900             ",ALTKEY2=                             ".            SQ2294.2
010000        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2294.2
010100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2294.2
010200        05 FILE-RECORD-INFO-P1-120.                               SQ2294.2
010300           07 FILLER              PIC X(5).                       SQ2294.2
010400           07 XFILE-NAME          PIC X(6).                       SQ2294.2
010500           07 FILLER              PIC X(8).                       SQ2294.2
010600           07 XRECORD-NAME        PIC X(6).                       SQ2294.2
010700           07 FILLER              PIC X(1).                       SQ2294.2
010800           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2294.2
010900           07 FILLER              PIC X(7).                       SQ2294.2
011000           07 XRECORD-NUMBER      PIC 9(6).                       SQ2294.2
011100           07 FILLER              PIC X(6).                       SQ2294.2
011200           07 UPDATE-NUMBER       PIC 9(2).                       SQ2294.2
011300           07 FILLER              PIC X(5).                       SQ2294.2
011400           07 ODO-NUMBER          PIC 9(4).                       SQ2294.2
011500           07 FILLER              PIC X(5).                       SQ2294.2
011600           07 XPROGRAM-NAME       PIC X(5).                       SQ2294.2
011700           07 FILLER              PIC X(7).                       SQ2294.2
011800           07 XRECORD-LENGTH      PIC 9(6).                       SQ2294.2
011900           07 FILLER              PIC X(7).                       SQ2294.2
012000           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2294.2
012100           07 FILLER              PIC X(1).                       SQ2294.2
012200           07 XBLOCK-SIZE         PIC 9(4).                       SQ2294.2
012300           07 FILLER              PIC X(6).                       SQ2294.2
012400           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2294.2
012500           07 FILLER              PIC X(5).                       SQ2294.2
012600           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2294.2
012700           07 FILLER              PIC X(6).                       SQ2294.2
012800           07 XLABEL-TYPE         PIC X(1).                       SQ2294.2
012900        05 FILE-RECORD-INFO-P121-240.                             SQ2294.2
013000           07 FILLER              PIC X(8).                       SQ2294.2
013100           07 XRECORD-KEY         PIC X(29).                      SQ2294.2
013200           07 FILLER              PIC X(9).                       SQ2294.2
013300           07 ALTERNATE-KEY1      PIC X(29).                      SQ2294.2
013400           07 FILLER              PIC X(9).                       SQ2294.2
013500           07 ALTERNATE-KEY2      PIC X(29).                      SQ2294.2
013600           07 FILLER              PIC X(7).                       SQ2294.2
013700*                                                                 SQ2294.2
013800 01  TEST-RESULTS.                                                SQ2294.2
013900     02 FILLER              PIC X      VALUE SPACE.               SQ2294.2
014000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2294.2
014100     02 FILLER              PIC X      VALUE SPACE.               SQ2294.2
014200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2294.2
014300     02 FILLER              PIC X      VALUE SPACE.               SQ2294.2
014400     02  PAR-NAME.                                                SQ2294.2
014500       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2294.2
014600       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2294.2
014700       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2294.2
014800     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2294.2
014900     02 RE-MARK             PIC X(61).                            SQ2294.2
015000 01  TEST-COMPUTED.                                               SQ2294.2
015100   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2294.2
015200   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2294.2
015300   02 COMPUTED-X.                                                 SQ2294.2
015400     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2294.2
015500     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2294.2
015600     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2294.2
015700     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2294.2
015800     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2294.2
015900     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2294.2
016000        04 COMPUTED-18V0                   PIC -9(18).            SQ2294.2
016100        04 FILLER                          PIC X.                 SQ2294.2
016200     03 FILLER PIC X(50) VALUE SPACE.                             SQ2294.2
016300 01  TEST-CORRECT.                                                SQ2294.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             SQ2294.2
016500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2294.2
016600     02 CORRECT-X.                                                SQ2294.2
016700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2294.2
016800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2294.2
016900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2294.2
017000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2294.2
017100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2294.2
017200     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2294.2
017300         04 CORRECT-18V0                     PIC -9(18).          SQ2294.2
017400         04 FILLER                           PIC X.               SQ2294.2
017500     03 FILLER PIC X(2) VALUE SPACE.                              SQ2294.2
017600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2294.2
017700 01  CCVS-C-1.                                                    SQ2294.2
017800     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ2294.2
017900     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ2294.2
018000-    "SS  PARAGRAPH-NAME                                          SQ2294.2
018100-    "       REMARKS".                                            SQ2294.2
018200     02 FILLER           PIC X(17)    VALUE SPACE.                SQ2294.2
018300 01  CCVS-C-2.                                                    SQ2294.2
018400     02 FILLER           PIC XXXX     VALUE SPACE.                SQ2294.2
018500     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ2294.2
018600     02 FILLER           PIC X(16)    VALUE SPACE.                SQ2294.2
018700     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ2294.2
018800     02 FILLER           PIC X(90)    VALUE SPACE.                SQ2294.2
018900 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2294.2
019000 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2294.2
019100 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2294.2
019200 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2294.2
019300 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2294.2
019400 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2294.2
019500 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2294.2
019600 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2294.2
019700 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2294.2
019800 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2294.2
019900 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2294.2
020000 01  CCVS-H-1.                                                    SQ2294.2
020100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2294.2
020200     02  FILLER          PIC X(42)    VALUE                       SQ2294.2
020300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2294.2
020400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2294.2
020500 01  CCVS-H-2A.                                                   SQ2294.2
020600   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2294.2
020700   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2294.2
020800   02  FILLER            PIC XXXX   VALUE                         SQ2294.2
020900     "4.2 ".                                                      SQ2294.2
021000   02  FILLER            PIC X(28)  VALUE                         SQ2294.2
021100            " COPY - NOT FOR DISTRIBUTION".                       SQ2294.2
021200   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2294.2
021300*                                                                 SQ2294.2
021400 01  CCVS-H-2B.                                                   SQ2294.2
021500   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2294.2
021600   02  TEST-ID           PIC X(9).                                SQ2294.2
021700   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2294.2
021800   02  FILLER            PIC X(12)  VALUE                         SQ2294.2
021900     " HIGH       ".                                              SQ2294.2
022000   02  FILLER            PIC X(22)  VALUE                         SQ2294.2
022100            " LEVEL VALIDATION FOR ".                             SQ2294.2
022200   02  FILLER            PIC X(58)  VALUE                         SQ2294.2
022300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2294.2
022400 01  CCVS-H-3.                                                    SQ2294.2
022500     02  FILLER          PIC X(34)  VALUE                         SQ2294.2
022600            " FOR OFFICIAL USE ONLY    ".                         SQ2294.2
022700     02  FILLER          PIC X(58)  VALUE                         SQ2294.2
022800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2294.2
022900     02  FILLER          PIC X(28)  VALUE                         SQ2294.2
023000            "  COPYRIGHT   1985,1986 ".                           SQ2294.2
023100 01  CCVS-E-1.                                                    SQ2294.2
023200     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2294.2
023300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2294.2
023400     02 ID-AGAIN         PIC X(9).                                SQ2294.2
023500     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2294.2
023600 01  CCVS-E-2.                                                    SQ2294.2
023700     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2294.2
023800     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2294.2
023900     02  CCVS-E-2-2.                                              SQ2294.2
024000         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2294.2
024100         03 FILLER         PIC X      VALUE SPACE.                SQ2294.2
024200         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2294.2
024300            "ERRORS ENCOUNTERED".                                 SQ2294.2
024400 01  CCVS-E-3.                                                    SQ2294.2
024500     02  FILLER          PIC X(22)  VALUE                         SQ2294.2
024600            " FOR OFFICIAL USE ONLY".                             SQ2294.2
024700     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2294.2
024800     02  FILLER          PIC X(58)  VALUE                         SQ2294.2
024900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2294.2
025000     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2294.2
025100     02  FILLER          PIC X(20)  VALUE                         SQ2294.2
025200             " COPYRIGHT 1985,1986".                              SQ2294.2
025300 01  CCVS-E-4.                                                    SQ2294.2
025400     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2294.2
025500     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2294.2
025600     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2294.2
025700     02 FILLER           PIC X(40)  VALUE                         SQ2294.2
025800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2294.2
025900 01  XXINFO.                                                      SQ2294.2
026000     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2294.2
026100     02 INFO-TEXT.                                                SQ2294.2
026200       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2294.2
026300       04 XXCOMPUTED         PIC X(20).                           SQ2294.2
026400       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2294.2
026500       04 XXCORRECT          PIC X(20).                           SQ2294.2
026600     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2294.2
026700 01  HYPHEN-LINE.                                                 SQ2294.2
026800     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2294.2
026900     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2294.2
027000-    "*****************************************".                 SQ2294.2
027100     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2294.2
027200-    "******************************".                            SQ2294.2
027300 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2294.2
027400     "SQ229A".                                                    SQ2294.2
027500*                                                                 SQ2294.2
027600 PROCEDURE DIVISION.                                              SQ2294.2
027700 DECLARATIVES.                                                    SQ2294.2
027800 SQ-FS1-DECLARATIVE SECTION.                                      SQ2294.2
027900     USE AFTER EXCEPTION PROCEDURE ON SQ-FS1.                     SQ2294.2
028000 INPUT-ERROR-PROCESS.                                             SQ2294.2
028100     IF SQ-FS1-STATUS = "47"                                      SQ2294.2
028200             PERFORM DECL-PASS                                    SQ2294.2
028300             GO TO DECL-ABNORMAL-TERM                             SQ2294.2
028400     ELSE                                                         SQ2294.2
028500             MOVE "47" TO CORRECT-A                               SQ2294.2
028600             MOVE SQ-FS1-STATUS TO COMPUTED-A                     SQ2294.2
028700             MOVE "STATUS FOR READ OF FILE OPEN EXTEND INCORRECT" SQ2294.2
028800                     TO RE-MARK                                   SQ2294.2
028900             MOVE "VII-5, 1.3.5(4)F" TO ANSI-REFERENCE            SQ2294.2
029000             PERFORM DECL-FAIL                                    SQ2294.2
029100             GO TO DECL-ABNORMAL-TERM                             SQ2294.2
029200     END-IF.                                                      SQ2294.2
029300*                                                                 SQ2294.2
029400 DECL-PASS.                                                       SQ2294.2
029500     MOVE   "PASS " TO P-OR-F.                                    SQ2294.2
029600     ADD     1 TO PASS-COUNTER.                                   SQ2294.2
029700     PERFORM DECL-PRINT-DETAIL.                                   SQ2294.2
029800*                                                                 SQ2294.2
029900 DECL-FAIL.                                                       SQ2294.2
030000     MOVE   "FAIL*" TO P-OR-F.                                    SQ2294.2
030100     ADD     1 TO ERROR-COUNTER.                                  SQ2294.2
030200     PERFORM DECL-PRINT-DETAIL.                                   SQ2294.2
030300*                                                                 SQ2294.2
030400 DECL-PRINT-DETAIL.                                               SQ2294.2
030500     IF REC-CT NOT EQUAL TO ZERO                                  SQ2294.2
030600             MOVE "." TO PARDOT-X                                 SQ2294.2
030700             MOVE REC-CT TO DOTVALUE.                             SQ2294.2
030800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2294.2
030900     PERFORM DECL-WRITE-LINE.                                     SQ2294.2
031000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2294.2
031100         PERFORM DECL-WRITE-LINE                                  SQ2294.2
031200         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ2294.2
031300     ELSE                                                         SQ2294.2
031400         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ2294.2
031500     MOVE    SPACE TO P-OR-F.                                     SQ2294.2
031600     MOVE    SPACE TO COMPUTED-X.                                 SQ2294.2
031700     MOVE    SPACE TO CORRECT-X.                                  SQ2294.2
031800     IF REC-CT EQUAL TO ZERO                                      SQ2294.2
031900         MOVE    SPACE TO PAR-NAME.                               SQ2294.2
032000     MOVE    SPACE TO RE-MARK.                                    SQ2294.2
032100*                                                                 SQ2294.2
032200 DECL-WRITE-LINE.                                                 SQ2294.2
032300     ADD     1 TO RECORD-COUNT.                                   SQ2294.2
032400*    IF RECORD-COUNT GREATER 50                                   SQ2294.2
032500*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ2294.2
032600*        MOVE    SPACE TO DUMMY-RECORD                            SQ2294.2
032700*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ2294.2
032800*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ2294.2
032900*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ2294.2
033000*        PERFORM DECL-WRT-LN 2 TIMES                              SQ2294.2
033100*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ2294.2
033200*        PERFORM DECL-WRT-LN                                      SQ2294.2
033300*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ2294.2
033400*        MOVE    ZERO TO RECORD-COUNT.                            SQ2294.2
033500     PERFORM DECL-WRT-LN.                                         SQ2294.2
033600*                                                                 SQ2294.2
033700 DECL-WRT-LN.                                                     SQ2294.2
033800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2294.2
033900     MOVE    SPACE TO DUMMY-RECORD.                               SQ2294.2
034000*                                                                 SQ2294.2
034100 DECL-FAIL-ROUTINE.                                               SQ2294.2
034200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ2294.2
034300     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ2294.2
034400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2294.2
034500     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2294.2
034600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2294.2
034700     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2294.2
034800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2294.2
034900     GO TO   DECL-FAIL-EX.                                        SQ2294.2
035000 DECL-FAIL-WRITE.                                                 SQ2294.2
035100     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ2294.2
035200     PERFORM DECL-WRITE-LINE                                      SQ2294.2
035300     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2294.2
035400     MOVE    TEST-CORRECT TO PRINT-REC                            SQ2294.2
035500     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2294.2
035600     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ2294.2
035700 DECL-FAIL-EX.                                                    SQ2294.2
035800     EXIT.                                                        SQ2294.2
035900*                                                                 SQ2294.2
036000 DECL-BAIL.                                                       SQ2294.2
036100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ2294.2
036200     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ2294.2
036300 DECL-BAIL-WRITE.                                                 SQ2294.2
036400     MOVE    CORRECT-A TO XXCORRECT.                              SQ2294.2
036500     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ2294.2
036600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE                 SQ2294.2
036700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2294.2
036800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2294.2
036900     MOVE    SPACE TO INF-ANSI-REFERENCE.                         SQ2294.2
037000 DECL-BAIL-EX.                                                    SQ2294.2
037100     EXIT.                                                        SQ2294.2
037200*                                                                 SQ2294.2
037300 DECL-ABNORMAL-TERM.                                              SQ2294.2
037400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2294.2
037500     PERFORM DECL-WRITE-LINE.                                     SQ2294.2
037600     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ2294.2
037700             TO DUMMY-RECORD.                                     SQ2294.2
037800     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ2294.2
037900*                                                                 SQ2294.2
038000 END-DECLS.                                                       SQ2294.2
038100     EXIT.                                                        SQ2294.2
038200 END DECLARATIVES.                                                SQ2294.2
038300*                                                                 SQ2294.2
038400*                                                                 SQ2294.2
038500 CCVS1 SECTION.                                                   SQ2294.2
038600 OPEN-FILES.                                                      SQ2294.2
038700     OPEN    OUTPUT PRINT-FILE.                                   SQ2294.2
038800     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2294.2
038900     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2294.2
039000     MOVE    SPACE TO TEST-RESULTS.                               SQ2294.2
039100     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2294.2
039200     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2294.2
039300     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2294.2
039400     GO TO CCVS1-EXIT.                                            SQ2294.2
039500*                                                                 SQ2294.2
039600 CCVS-INIT-FILE.                                                  SQ2294.2
039700     ADD     1 TO REC-SKL-SUB.                                    SQ2294.2
039800     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2294.2
039900                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2294.2
040000*                                                                 SQ2294.2
040100 CLOSE-FILES.                                                     SQ2294.2
040200     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2294.2
040300     CLOSE   PRINT-FILE.                                          SQ2294.2
040400 TERMINATE-CCVS.                                                  SQ2294.2
040500     STOP    RUN.                                                 SQ2294.2
040600*                                                                 SQ2294.2
040700 INSPT.                                                           SQ2294.2
040800     MOVE   "INSPT" TO P-OR-F.                                    SQ2294.2
040900     ADD     1 TO INSPECT-COUNTER.                                SQ2294.2
041000     PERFORM PRINT-DETAIL.                                        SQ2294.2
041100                                                                  SQ2294.2
041200 PASS.                                                            SQ2294.2
041300     MOVE   "PASS " TO P-OR-F.                                    SQ2294.2
041400     ADD     1 TO PASS-COUNTER.                                   SQ2294.2
041500     PERFORM PRINT-DETAIL.                                        SQ2294.2
041600*                                                                 SQ2294.2
041700 FAIL.                                                            SQ2294.2
041800     MOVE   "FAIL*" TO P-OR-F.                                    SQ2294.2
041900     ADD     1 TO ERROR-COUNTER.                                  SQ2294.2
042000     PERFORM PRINT-DETAIL.                                        SQ2294.2
042100*                                                                 SQ2294.2
042200 DE-LETE.                                                         SQ2294.2
042300     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2294.2
042400     MOVE   "*****" TO P-OR-F.                                    SQ2294.2
042500     ADD     1 TO DELETE-COUNTER.                                 SQ2294.2
042600     PERFORM PRINT-DETAIL.                                        SQ2294.2
042700*                                                                 SQ2294.2
042800 PRINT-DETAIL.                                                    SQ2294.2
042900     IF REC-CT NOT EQUAL TO ZERO                                  SQ2294.2
043000         MOVE   "." TO PARDOT-X                                   SQ2294.2
043100         MOVE    REC-CT TO DOTVALUE.                              SQ2294.2
043200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2294.2
043300     PERFORM WRITE-LINE.                                          SQ2294.2
043400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2294.2
043500         PERFORM WRITE-LINE                                       SQ2294.2
043600         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2294.2
043700     ELSE                                                         SQ2294.2
043800         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2294.2
043900     MOVE    SPACE TO P-OR-F.                                     SQ2294.2
044000     MOVE    SPACE TO COMPUTED-X.                                 SQ2294.2
044100     MOVE    SPACE TO CORRECT-X.                                  SQ2294.2
044200     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2294.2
044300     MOVE    SPACE TO RE-MARK.                                    SQ2294.2
044400*                                                                 SQ2294.2
044500 HEAD-ROUTINE.                                                    SQ2294.2
044600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2294.2
044700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2294.2
044800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2294.2
044900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2294.2
045000 COLUMN-NAMES-ROUTINE.                                            SQ2294.2
045100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2294.2
045200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2294.2
045300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2294.2
045400 END-ROUTINE.                                                     SQ2294.2
045500     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2294.2
045600     PERFORM WRITE-LINE 5 TIMES.                                  SQ2294.2
045700 END-RTN-EXIT.                                                    SQ2294.2
045800     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2294.2
045900     PERFORM WRITE-LINE 2 TIMES.                                  SQ2294.2
046000*                                                                 SQ2294.2
046100 END-ROUTINE-1.                                                   SQ2294.2
046200     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2294.2
046300     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2294.2
046400     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2294.2
046500     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2294.2
046600     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2294.2
046700     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2294.2
046800     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2294.2
046900     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2294.2
047000     PERFORM WRITE-LINE.                                          SQ2294.2
047100     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2294.2
047200     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2294.2
047300         MOVE   "NO " TO ERROR-TOTAL                              SQ2294.2
047400     ELSE                                                         SQ2294.2
047500         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2294.2
047600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2294.2
047700     PERFORM WRITE-LINE.                                          SQ2294.2
047800 END-ROUTINE-13.                                                  SQ2294.2
047900     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2294.2
048000         MOVE   "NO " TO ERROR-TOTAL                              SQ2294.2
048100     ELSE                                                         SQ2294.2
048200         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2294.2
048300     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2294.2
048400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2294.2
048500     PERFORM WRITE-LINE.                                          SQ2294.2
048600     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2294.2
048700         MOVE   "NO " TO ERROR-TOTAL                              SQ2294.2
048800     ELSE                                                         SQ2294.2
048900         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2294.2
049000     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2294.2
049100     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2294.2
049200     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2294.2
049300*                                                                 SQ2294.2
049400 WRITE-LINE.                                                      SQ2294.2
049500     ADD     1 TO RECORD-COUNT.                                   SQ2294.2
049600*    IF RECORD-COUNT GREATER 50                                   SQ2294.2
049700*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2294.2
049800*        MOVE  SPACE TO DUMMY-RECORD                              SQ2294.2
049900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2294.2
050000*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2294.2
050100*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2294.2
050200*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2294.2
050300*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2294.2
050400*        MOVE  ZERO TO RECORD-COUNT.                              SQ2294.2
050500     PERFORM WRT-LN.                                              SQ2294.2
050600*                                                                 SQ2294.2
050700 WRT-LN.                                                          SQ2294.2
050800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2294.2
050900     MOVE    SPACE TO DUMMY-RECORD.                               SQ2294.2
051000 BLANK-LINE-PRINT.                                                SQ2294.2
051100     PERFORM WRT-LN.                                              SQ2294.2
051200 FAIL-ROUTINE.                                                    SQ2294.2
051300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2294.2
051400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2294.2
051500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2294.2
051600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2294.2
051700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2294.2
051800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2294.2
051900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2294.2
052000     GO TO   FAIL-ROUTINE-EX.                                     SQ2294.2
052100 FAIL-ROUTINE-WRITE.                                              SQ2294.2
052200     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2294.2
052300     PERFORM WRITE-LINE                                           SQ2294.2
052400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2294.2
052500     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2294.2
052600     PERFORM WRITE-LINE 2 TIMES.                                  SQ2294.2
052700     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2294.2
052800 FAIL-ROUTINE-EX.                                                 SQ2294.2
052900     EXIT.                                                        SQ2294.2
053000 BAIL-OUT.                                                        SQ2294.2
053100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2294.2
053200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2294.2
053300 BAIL-OUT-WRITE.                                                  SQ2294.2
053400     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2294.2
053500     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2294.2
053600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2294.2
053700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2294.2
053800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2294.2
053900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2294.2
054000 BAIL-OUT-EX.                                                     SQ2294.2
054100     EXIT.                                                        SQ2294.2
054200 CCVS1-EXIT.                                                      SQ2294.2
054300     EXIT.                                                        SQ2294.2
054400*                                                                 SQ2294.2
054500****************************************************************  SQ2294.2
054600*                                                              *  SQ2294.2
054700*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2294.2
054800*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2294.2
054900*                                                              *  SQ2294.2
055000****************************************************************  SQ2294.2
055100*                                                                 SQ2294.2
055200 SECT-SQ229A-0001 SECTION.                                        SQ2294.2
055300 WRITE-INIT-GF-01.                                                SQ2294.2
055400*                                                                 SQ2294.2
055500*        THIS TEST CREATES FILE SQ-FS1 AND CLOSES IT.             SQ2294.2
055600*        FIRST IT SETS UP A SKELETON RECORD IN WORKING STORAGE.   SQ2294.2
055700*                                                                 SQ2294.2
055800     MOVE "SQ-FS1"     TO XFILE-NAME (1).                         SQ2294.2
055900     MOVE "R1-F-G"     TO XRECORD-NAME (1).                       SQ2294.2
056000     MOVE  CCVS-PGM-ID TO XPROGRAM-NAME (1).                      SQ2294.2
056100     MOVE 120          TO XRECORD-LENGTH (1).                     SQ2294.2
056200     MOVE "RC"         TO CHARS-OR-RECORDS (1).                   SQ2294.2
056300     MOVE 1            TO XBLOCK-SIZE (1).                        SQ2294.2
056400     MOVE 1            TO RECORDS-IN-FILE (1).                    SQ2294.2
056500     MOVE "SQ"         TO XFILE-ORGANIZATION (1).                 SQ2294.2
056600     MOVE "S"          TO XLABEL-TYPE (1).                        SQ2294.2
056700     MOVE 1            TO XRECORD-NUMBER (1).                     SQ2294.2
056800*                                                                 SQ2294.2
056900 WRITE-OPEN-01.                                                   SQ2294.2
057000     OPEN    OUTPUT SQ-FS1.                                       SQ2294.2
057100*                                                                 SQ2294.2
057200*        WRITE A SINGLE RECORD TO THE FILE                        SQ2294.2
057300*                                                                 SQ2294.2
057400 WRITE-INIT-01.                                                   SQ2294.2
057500 WRITE-TEST-01-01.                                                SQ2294.2
057600     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.     SQ2294.2
057700     WRITE   SQ-FS1R1-F-G-120.                                    SQ2294.2
057800*                                                                 SQ2294.2
057900 CLOSE-INIT-01.                                                   SQ2294.2
058000 CLOSE-TEST-01.                                                   SQ2294.2
058100     CLOSE   SQ-FS1.                                              SQ2294.2
058200*                                                                 SQ2294.2
058300*        THIS TEST OPENS THE FILE JUST CREATED IN THE EXTEND      SQ2294.2
058400*        MODE.  WE ATTEMPT TO READ A RECORD FROM THE FILE AND     SQ2294.2
058500*        EXAMINE IN A DECLARATIVE THE I-O STATUS RETURNED.  IT IS SQ2294.2
058600*        POSSIBLE THAT THE SYSTEM ACTION MAY BE ABNORMAL PROGRAM  SQ2294.2
058700*        TERMINATION AFTER THE DECLARATIVE IS EXECUTED.           SQ2294.2
058800*                                                                 SQ2294.2
058900 OPEN-INIT-01.                                                    SQ2294.2
059000*                                                                 SQ2294.2
059100 OPEN-TEST-01.                                                    SQ2294.2
059200     OPEN   EXTEND SQ-FS1.                                        SQ2294.2
059300*                                                                 SQ2294.2
059400 READ-INIT-01.                                                    SQ2294.2
059500     MOVE 1 TO REC-CT.                                            SQ2294.2
059600     MOVE "**" TO SQ-FS1-STATUS.                                  SQ2294.2
059700     MOVE "READ-TEST-01" TO PAR-NAME.                             SQ2294.2
059800     MOVE "READ OF EXTEND FILE" TO FEATURE.                       SQ2294.2
059900*                                                                 SQ2294.2
060000 READ-TEST-01.                                                    SQ2294.2
060100     READ    SQ-FS1.                                              SQ2294.2
060200*                                                                 SQ2294.2
060300 CLOSE-INIT-02.                                                   SQ2294.2
060400 CLOSE-TEST-02.                                                   SQ2294.2
060500     CLOSE   SQ-FS1.                                              SQ2294.2
060600*                                                                 SQ2294.2
060700 CCVS-EXIT SECTION.                                               SQ2294.2
060800 CCVS-999999.                                                     SQ2294.2
060900     GO TO CLOSE-FILES.                                           SQ2294.2
      *END-OF,SQ229A                                                                  
