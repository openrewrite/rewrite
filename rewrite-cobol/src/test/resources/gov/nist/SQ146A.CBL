      *HEADER,COBOL,SQ146A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1464.2
000200 PROGRAM-ID.                                                      SQ1464.2
000300     SQ146A.                                                      SQ1464.2
000400****************************************************************  SQ1464.2
000500*                                                              *  SQ1464.2
000600*    VALIDATION FOR:-                                          *  SQ1464.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1464.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ1464.2
000900*                                                              *  SQ1464.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ1464.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1464.2
001200*                                                              *  SQ1464.2
001300****************************************************************  SQ1464.2
001400*                                                              *  SQ1464.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1464.2
001600*                                                              *  SQ1464.2
001700*            X-01   SEQUENTIAL TAPE                            *  SQ1464.2
001800*            X-55   SYSTEM PRINTER                             *  SQ1464.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ1464.2
002000*            X-83   OBJECT-COMPUTER.                           *  SQ1464.2
002100*            X-84   LABEL RECORDS OPTION                       *  SQ1464.2
002200*                                                              *  SQ1464.2
002300****************************************************************  SQ1464.2
002400*                                                              *  SQ1464.2
002500*    THIS PROGRAM CHECKS FOR THE CORRECT RESPONSE TO CLOSE OF  *  SQ1464.2
002600*    AN ALREADY CLOSED FILE.  THE TEST FOR CORRECT I-O STATUS  *  SQ1464.2
002700*    CODE 42 IS IN THE MAIN LINE CODE, THEREFORE AN ABNORMAL   *  SQ1464.2
002800*    TERMINATION IS POSSIBLE BEFORE THE TEST OF THE I-O STATUS *  SQ1464.2
002900*    CODE IS ACCOMPLISHED.                                     *  SQ1464.2
003000*                                                              *  SQ1464.2
003100****************************************************************  SQ1464.2
003200*                                                                 SQ1464.2
003300 ENVIRONMENT DIVISION.                                            SQ1464.2
003400 CONFIGURATION SECTION.                                           SQ1464.2
003500 SOURCE-COMPUTER.                                                 SQ1464.2
003600     XXXXX082.                                                    SQ1464.2
003700 OBJECT-COMPUTER.                                                 SQ1464.2
003800     XXXXX083.                                                    SQ1464.2
003900*                                                                 SQ1464.2
004000 INPUT-OUTPUT SECTION.                                            SQ1464.2
004100 FILE-CONTROL.                                                    SQ1464.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ1464.2
004300     XXXXX055.                                                    SQ1464.2
004400*                                                                 SQ1464.2
004500     SELECT SQ-FS1 ASSIGN TO                                      SQ1464.2
004600     XXXXX001                                                     SQ1464.2
004700            FILE STATUS IS SQ-FS1-STATUS.                         SQ1464.2
004800*                                                                 SQ1464.2
004900*                                                                 SQ1464.2
005000 DATA DIVISION.                                                   SQ1464.2
005100 FILE SECTION.                                                    SQ1464.2
005200 FD  PRINT-FILE                                                   SQ1464.2
005300C    LABEL RECORDS                                                SQ1464.2
005400C    XXXXX084                                                     SQ1464.2
005500C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1464.2
005600               .                                                  SQ1464.2
005700 01  PRINT-REC    PICTURE X(120).                                 SQ1464.2
005800 01  DUMMY-RECORD PICTURE X(120).                                 SQ1464.2
005900*                                                                 SQ1464.2
006000 FD  SQ-FS1                                                       SQ1464.2
006100C    LABEL RECORD IS STANDARD                                     SQ1464.2
006200                .                                                 SQ1464.2
006300 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1464.2
006400*                                                                 SQ1464.2
006500 WORKING-STORAGE SECTION.                                         SQ1464.2
006600*                                                                 SQ1464.2
006700***************************************************************   SQ1464.2
006800*                                                             *   SQ1464.2
006900*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1464.2
007000*                                                             *   SQ1464.2
007100***************************************************************   SQ1464.2
007200*                                                                 SQ1464.2
007300 01  SQ-FS1-STATUS.                                               SQ1464.2
007400   03  SQ-FS1-KEY-1   PIC X.                                      SQ1464.2
007500   03  SQ-FS1-KEY-2   PIC X.                                      SQ1464.2
007600*                                                                 SQ1464.2
007700 01  DECL-EXEC-SW   PIC 9.                                        SQ1464.2
007800*                                                                 SQ1464.2
007900***************************************************************   SQ1464.2
008000*                                                             *   SQ1464.2
008100*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1464.2
008200*                                                             *   SQ1464.2
008300***************************************************************   SQ1464.2
008400*                                                                 SQ1464.2
008500 01  REC-SKEL-SUB   PIC 99.                                       SQ1464.2
008600*                                                                 SQ1464.2
008700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1464.2
008800     03 FILE-RECORD-INFO-SKELETON.                                SQ1464.2
008900        05 FILLER                 PICTURE X(48)       VALUE       SQ1464.2
009000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1464.2
009100        05 FILLER                 PICTURE X(46)       VALUE       SQ1464.2
009200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1464.2
009300        05 FILLER                 PICTURE X(26)       VALUE       SQ1464.2
009400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1464.2
009500        05 FILLER                 PICTURE X(37)       VALUE       SQ1464.2
009600             ",RECKEY=                             ".             SQ1464.2
009700        05 FILLER                 PICTURE X(38)       VALUE       SQ1464.2
009800             ",ALTKEY1=                             ".            SQ1464.2
009900        05 FILLER                 PICTURE X(38)       VALUE       SQ1464.2
010000             ",ALTKEY2=                             ".            SQ1464.2
010100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1464.2
010200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1464.2
010300        05 FILE-RECORD-INFO-P1-120.                               SQ1464.2
010400           07 FILLER              PIC X(5).                       SQ1464.2
010500           07 XFILE-NAME          PIC X(6).                       SQ1464.2
010600           07 FILLER              PIC X(8).                       SQ1464.2
010700           07 XRECORD-NAME        PIC X(6).                       SQ1464.2
010800           07 FILLER              PIC X(1).                       SQ1464.2
010900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1464.2
011000           07 FILLER              PIC X(7).                       SQ1464.2
011100           07 XRECORD-NUMBER      PIC 9(6).                       SQ1464.2
011200           07 FILLER              PIC X(6).                       SQ1464.2
011300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1464.2
011400           07 FILLER              PIC X(5).                       SQ1464.2
011500           07 ODO-NUMBER          PIC 9(4).                       SQ1464.2
011600           07 FILLER              PIC X(5).                       SQ1464.2
011700           07 XPROGRAM-NAME       PIC X(5).                       SQ1464.2
011800           07 FILLER              PIC X(7).                       SQ1464.2
011900           07 XRECORD-LENGTH      PIC 9(6).                       SQ1464.2
012000           07 FILLER              PIC X(7).                       SQ1464.2
012100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1464.2
012200           07 FILLER              PIC X(1).                       SQ1464.2
012300           07 XBLOCK-SIZE         PIC 9(4).                       SQ1464.2
012400           07 FILLER              PIC X(6).                       SQ1464.2
012500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1464.2
012600           07 FILLER              PIC X(5).                       SQ1464.2
012700           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1464.2
012800           07 FILLER              PIC X(6).                       SQ1464.2
012900           07 XLABEL-TYPE         PIC X(1).                       SQ1464.2
013000        05 FILE-RECORD-INFO-P121-240.                             SQ1464.2
013100           07 FILLER              PIC X(8).                       SQ1464.2
013200           07 XRECORD-KEY         PIC X(29).                      SQ1464.2
013300           07 FILLER              PIC X(9).                       SQ1464.2
013400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1464.2
013500           07 FILLER              PIC X(9).                       SQ1464.2
013600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1464.2
013700           07 FILLER              PIC X(7).                       SQ1464.2
013800*                                                                 SQ1464.2
013900 01  TEST-RESULTS.                                                SQ1464.2
014000     02 FILLER              PIC X      VALUE SPACE.               SQ1464.2
014100     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1464.2
014200     02 FILLER              PIC X      VALUE SPACE.               SQ1464.2
014300     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1464.2
014400     02 FILLER              PIC X      VALUE SPACE.               SQ1464.2
014500     02  PAR-NAME.                                                SQ1464.2
014600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1464.2
014700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1464.2
014800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1464.2
014900     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1464.2
015000     02 RE-MARK             PIC X(61).                            SQ1464.2
015100 01  TEST-COMPUTED.                                               SQ1464.2
015200   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1464.2
015300   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1464.2
015400   02 COMPUTED-X.                                                 SQ1464.2
015500     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1464.2
015600     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1464.2
015700     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1464.2
015800     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1464.2
015900     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1464.2
016000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1464.2
016100        04 COMPUTED-18V0                   PIC -9(18).            SQ1464.2
016200        04 FILLER                          PIC X.                 SQ1464.2
016300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1464.2
016400 01  TEST-CORRECT.                                                SQ1464.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1464.2
016600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1464.2
016700     02 CORRECT-X.                                                SQ1464.2
016800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1464.2
016900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1464.2
017000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1464.2
017100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1464.2
017200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1464.2
017300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1464.2
017400         04 CORRECT-18V0                     PIC -9(18).          SQ1464.2
017500         04 FILLER                           PIC X.               SQ1464.2
017600     03 FILLER PIC X(2) VALUE SPACE.                              SQ1464.2
017700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1464.2
017800 01  CCVS-C-1.                                                    SQ1464.2
017900     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ1464.2
018000     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ1464.2
018100-    "SS  PARAGRAPH-NAME                                          SQ1464.2
018200-    "       REMARKS".                                            SQ1464.2
018300     02 FILLER           PIC X(17)    VALUE SPACE.                SQ1464.2
018400 01  CCVS-C-2.                                                    SQ1464.2
018500     02 FILLER           PIC XXXX     VALUE SPACE.                SQ1464.2
018600     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ1464.2
018700     02 FILLER           PIC X(16)    VALUE SPACE.                SQ1464.2
018800     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ1464.2
018900     02 FILLER           PIC X(90)    VALUE SPACE.                SQ1464.2
019000 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1464.2
019100 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1464.2
019200 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1464.2
019300 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1464.2
019400 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1464.2
019500 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1464.2
019600 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1464.2
019700 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1464.2
019800 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1464.2
019900 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1464.2
020000 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1464.2
020100 01  CCVS-H-1.                                                    SQ1464.2
020200     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1464.2
020300     02  FILLER          PIC X(42)    VALUE                       SQ1464.2
020400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1464.2
020500     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1464.2
020600 01  CCVS-H-2A.                                                   SQ1464.2
020700   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1464.2
020800   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1464.2
020900   02  FILLER            PIC XXXX   VALUE                         SQ1464.2
021000     "4.2 ".                                                      SQ1464.2
021100   02  FILLER            PIC X(28)  VALUE                         SQ1464.2
021200            " COPY - NOT FOR DISTRIBUTION".                       SQ1464.2
021300   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1464.2
021400*                                                                 SQ1464.2
021500 01  CCVS-H-2B.                                                   SQ1464.2
021600   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1464.2
021700   02  TEST-ID           PIC X(9).                                SQ1464.2
021800   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1464.2
021900   02  FILLER            PIC X(12)  VALUE                         SQ1464.2
022000     " HIGH       ".                                              SQ1464.2
022100   02  FILLER            PIC X(22)  VALUE                         SQ1464.2
022200            " LEVEL VALIDATION FOR ".                             SQ1464.2
022300   02  FILLER            PIC X(58)  VALUE                         SQ1464.2
022400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1464.2
022500 01  CCVS-H-3.                                                    SQ1464.2
022600     02  FILLER          PIC X(34)  VALUE                         SQ1464.2
022700            " FOR OFFICIAL USE ONLY    ".                         SQ1464.2
022800     02  FILLER          PIC X(58)  VALUE                         SQ1464.2
022900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1464.2
023000     02  FILLER          PIC X(28)  VALUE                         SQ1464.2
023100            "  COPYRIGHT   1985,1986 ".                           SQ1464.2
023200 01  CCVS-E-1.                                                    SQ1464.2
023300     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1464.2
023400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1464.2
023500     02 ID-AGAIN         PIC X(9).                                SQ1464.2
023600     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1464.2
023700 01  CCVS-E-2.                                                    SQ1464.2
023800     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1464.2
023900     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1464.2
024000     02  CCVS-E-2-2.                                              SQ1464.2
024100         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1464.2
024200         03 FILLER         PIC X      VALUE SPACE.                SQ1464.2
024300         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1464.2
024400            "ERRORS ENCOUNTERED".                                 SQ1464.2
024500 01  CCVS-E-3.                                                    SQ1464.2
024600     02  FILLER          PIC X(22)  VALUE                         SQ1464.2
024700            " FOR OFFICIAL USE ONLY".                             SQ1464.2
024800     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1464.2
024900     02  FILLER          PIC X(58)  VALUE                         SQ1464.2
025000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1464.2
025100     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1464.2
025200     02  FILLER          PIC X(20)  VALUE                         SQ1464.2
025300             " COPYRIGHT 1985,1986".                              SQ1464.2
025400 01  CCVS-E-4.                                                    SQ1464.2
025500     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1464.2
025600     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1464.2
025700     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1464.2
025800     02 FILLER           PIC X(40)  VALUE                         SQ1464.2
025900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1464.2
026000 01  XXINFO.                                                      SQ1464.2
026100     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1464.2
026200     02 INFO-TEXT.                                                SQ1464.2
026300       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1464.2
026400       04 XXCOMPUTED         PIC X(20).                           SQ1464.2
026500       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1464.2
026600       04 XXCORRECT          PIC X(20).                           SQ1464.2
026700     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1464.2
026800 01  HYPHEN-LINE.                                                 SQ1464.2
026900     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1464.2
027000     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1464.2
027100-    "*****************************************".                 SQ1464.2
027200     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1464.2
027300-    "******************************".                            SQ1464.2
027400 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1464.2
027500     "SQ146A".                                                    SQ1464.2
027600*                                                                 SQ1464.2
027700 PROCEDURE DIVISION.                                              SQ1464.2
027800 CCVS1 SECTION.                                                   SQ1464.2
027900 OPEN-FILES.                                                      SQ1464.2
028000     OPEN    OUTPUT PRINT-FILE.                                   SQ1464.2
028100     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1464.2
028200     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1464.2
028300     MOVE    SPACE TO TEST-RESULTS.                               SQ1464.2
028400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1464.2
028500     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1464.2
028600     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1464.2
028700     GO TO CCVS1-EXIT.                                            SQ1464.2
028800*                                                                 SQ1464.2
028900 CCVS-INIT-FILE.                                                  SQ1464.2
029000     ADD     1 TO REC-SKL-SUB.                                    SQ1464.2
029100     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1464.2
029200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1464.2
029300*                                                                 SQ1464.2
029400 CLOSE-FILES.                                                     SQ1464.2
029500     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1464.2
029600     CLOSE   PRINT-FILE.                                          SQ1464.2
029700 TERMINATE-CCVS.                                                  SQ1464.2
029800     STOP    RUN.                                                 SQ1464.2
029900*                                                                 SQ1464.2
030000 INSPT.                                                           SQ1464.2
030100     MOVE   "INSPT" TO P-OR-F.                                    SQ1464.2
030200     ADD     1 TO INSPECT-COUNTER.                                SQ1464.2
030300     PERFORM PRINT-DETAIL.                                        SQ1464.2
030400                                                                  SQ1464.2
030500 PASS.                                                            SQ1464.2
030600     MOVE   "PASS " TO P-OR-F.                                    SQ1464.2
030700     ADD     1 TO PASS-COUNTER.                                   SQ1464.2
030800     PERFORM PRINT-DETAIL.                                        SQ1464.2
030900*                                                                 SQ1464.2
031000 FAIL.                                                            SQ1464.2
031100     MOVE   "FAIL*" TO P-OR-F.                                    SQ1464.2
031200     ADD     1 TO ERROR-COUNTER.                                  SQ1464.2
031300     PERFORM PRINT-DETAIL.                                        SQ1464.2
031400*                                                                 SQ1464.2
031500 DE-LETE.                                                         SQ1464.2
031600     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1464.2
031700     MOVE   "*****" TO P-OR-F.                                    SQ1464.2
031800     ADD     1 TO DELETE-COUNTER.                                 SQ1464.2
031900     PERFORM PRINT-DETAIL.                                        SQ1464.2
032000*                                                                 SQ1464.2
032100 PRINT-DETAIL.                                                    SQ1464.2
032200     IF REC-CT NOT EQUAL TO ZERO                                  SQ1464.2
032300         MOVE   "." TO PARDOT-X                                   SQ1464.2
032400         MOVE    REC-CT TO DOTVALUE.                              SQ1464.2
032500     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1464.2
032600     PERFORM WRITE-LINE.                                          SQ1464.2
032700     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1464.2
032800         PERFORM WRITE-LINE                                       SQ1464.2
032900         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1464.2
033000     ELSE                                                         SQ1464.2
033100         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1464.2
033200     MOVE    SPACE TO P-OR-F.                                     SQ1464.2
033300     MOVE    SPACE TO COMPUTED-X.                                 SQ1464.2
033400     MOVE    SPACE TO CORRECT-X.                                  SQ1464.2
033500     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1464.2
033600     MOVE    SPACE TO RE-MARK.                                    SQ1464.2
033700*                                                                 SQ1464.2
033800 HEAD-ROUTINE.                                                    SQ1464.2
033900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1464.2
034000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1464.2
034100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1464.2
034200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1464.2
034300 COLUMN-NAMES-ROUTINE.                                            SQ1464.2
034400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1464.2
034500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1464.2
034600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1464.2
034700 END-ROUTINE.                                                     SQ1464.2
034800     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1464.2
034900     PERFORM WRITE-LINE 5 TIMES.                                  SQ1464.2
035000 END-RTN-EXIT.                                                    SQ1464.2
035100     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1464.2
035200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1464.2
035300*                                                                 SQ1464.2
035400 END-ROUTINE-1.                                                   SQ1464.2
035500     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1464.2
035600     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1464.2
035700     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1464.2
035800     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1464.2
035900     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1464.2
036000     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1464.2
036100     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1464.2
036200     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1464.2
036300     PERFORM WRITE-LINE.                                          SQ1464.2
036400     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1464.2
036500     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1464.2
036600         MOVE   "NO " TO ERROR-TOTAL                              SQ1464.2
036700     ELSE                                                         SQ1464.2
036800         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1464.2
036900     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1464.2
037000     PERFORM WRITE-LINE.                                          SQ1464.2
037100 END-ROUTINE-13.                                                  SQ1464.2
037200     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1464.2
037300         MOVE   "NO " TO ERROR-TOTAL                              SQ1464.2
037400     ELSE                                                         SQ1464.2
037500         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1464.2
037600     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1464.2
037700     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1464.2
037800     PERFORM WRITE-LINE.                                          SQ1464.2
037900     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1464.2
038000         MOVE   "NO " TO ERROR-TOTAL                              SQ1464.2
038100     ELSE                                                         SQ1464.2
038200         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1464.2
038300     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1464.2
038400     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1464.2
038500     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1464.2
038600*                                                                 SQ1464.2
038700 WRITE-LINE.                                                      SQ1464.2
038800     ADD     1 TO RECORD-COUNT.                                   SQ1464.2
038900*    IF RECORD-COUNT GREATER 50                                   SQ1464.2
039000*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1464.2
039100*        MOVE  SPACE TO DUMMY-RECORD                              SQ1464.2
039200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1464.2
039300*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1464.2
039400*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1464.2
039500*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1464.2
039600*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1464.2
039700*        MOVE  ZERO TO RECORD-COUNT.                              SQ1464.2
039800     PERFORM WRT-LN.                                              SQ1464.2
039900*                                                                 SQ1464.2
040000 WRT-LN.                                                          SQ1464.2
040100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1464.2
040200     MOVE    SPACE TO DUMMY-RECORD.                               SQ1464.2
040300 BLANK-LINE-PRINT.                                                SQ1464.2
040400     PERFORM WRT-LN.                                              SQ1464.2
040500 FAIL-ROUTINE.                                                    SQ1464.2
040600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1464.2
040700     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1464.2
040800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1464.2
040900     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1464.2
041000     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1464.2
041100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1464.2
041200     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1464.2
041300     GO TO   FAIL-ROUTINE-EX.                                     SQ1464.2
041400 FAIL-ROUTINE-WRITE.                                              SQ1464.2
041500     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1464.2
041600     PERFORM WRITE-LINE                                           SQ1464.2
041700     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1464.2
041800     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1464.2
041900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1464.2
042000     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1464.2
042100 FAIL-ROUTINE-EX.                                                 SQ1464.2
042200     EXIT.                                                        SQ1464.2
042300 BAIL-OUT.                                                        SQ1464.2
042400     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1464.2
042500     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1464.2
042600 BAIL-OUT-WRITE.                                                  SQ1464.2
042700     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1464.2
042800     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1464.2
042900     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1464.2
043000     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1464.2
043100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1464.2
043200     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1464.2
043300 BAIL-OUT-EX.                                                     SQ1464.2
043400     EXIT.                                                        SQ1464.2
043500 CCVS1-EXIT.                                                      SQ1464.2
043600     EXIT.                                                        SQ1464.2
043700*                                                                 SQ1464.2
043800****************************************************************  SQ1464.2
043900*                                                              *  SQ1464.2
044000*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1464.2
044100*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1464.2
044200*                                                              *  SQ1464.2
044300****************************************************************  SQ1464.2
044400*                                                                 SQ1464.2
044500 SECT-SQ146A-0001 SECTION.                                        SQ1464.2
044600 WRITE-INIT-GF-01.                                                SQ1464.2
044700*                                                                 SQ1464.2
044800*        THIS TEST CREATES FILE SQ-FS1 AND CLOSES IT.             SQ1464.2
044900*        FIRST IT SETS UP A SKELETON RECORD IN WORKING STORAGE.   SQ1464.2
045000*                                                                 SQ1464.2
045100     MOVE "SQ-FS1"     TO XFILE-NAME (1).                         SQ1464.2
045200     MOVE "R1-F-G"     TO XRECORD-NAME (1).                       SQ1464.2
045300     MOVE  CCVS-PGM-ID TO XPROGRAM-NAME (1).                      SQ1464.2
045400     MOVE 120          TO XRECORD-LENGTH (1).                     SQ1464.2
045500     MOVE "RC"         TO CHARS-OR-RECORDS (1).                   SQ1464.2
045600     MOVE 1            TO XBLOCK-SIZE (1).                        SQ1464.2
045700     MOVE 1            TO RECORDS-IN-FILE (1).                    SQ1464.2
045800     MOVE "SQ"         TO XFILE-ORGANIZATION (1).                 SQ1464.2
045900     MOVE "S"          TO XLABEL-TYPE (1).                        SQ1464.2
046000     MOVE 1            TO XRECORD-NUMBER (1).                     SQ1464.2
046100*                                                                 SQ1464.2
046200 WRITE-OPEN-01.                                                   SQ1464.2
046300     OPEN    OUTPUT SQ-FS1.                                       SQ1464.2
046400*                                                                 SQ1464.2
046500*        WRITE A SINGLE RECORD TO THE FILE                        SQ1464.2
046600*                                                                 SQ1464.2
046700 WRITE-INIT-01.                                                   SQ1464.2
046800 WRITE-TEST-01-01.                                                SQ1464.2
046900     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.     SQ1464.2
047000     WRITE   SQ-FS1R1-F-G-120.                                    SQ1464.2
047100*                                                                 SQ1464.2
047200*        CLOSE THE FILE.                                          SQ1464.2
047300*                                                                 SQ1464.2
047400 CLOSE-INIT-01.                                                   SQ1464.2
047500 CLOSE-TEST-01.                                                   SQ1464.2
047600     CLOSE   SQ-FS1.                                              SQ1464.2
047700*                                                                 SQ1464.2
047800*        HAVING CLOSED THE FILE, WE NOW TRY TO CLOSE IT AGAIN.    SQ1464.2
047900*        THE TEST PASSES IF THE FILE CANNOT BE RECLOSED AND       SQ1464.2
048000*        THE APPROPRIATE I-O STATUS VALUE IS RETURNED.            SQ1464.2
048100*        AN IMPLEMENTATION MAY TERMINATE EXECUTION OF THE         SQ1464.2
048200*        PROGRAM ON EXECUTION OF THE CLOSE OR MAY RETURN CONTROL  SQ1464.2
048300*        TO THE STATEMENT FOLLOWING THE CLOSE STATEMENT.          SQ1464.2
048400*                                                                 SQ1464.2
048500 CLOSE-INIT-02.                                                   SQ1464.2
048600*                                                                 SQ1464.2
048700     MOVE   "CLOSE A CLOSED FILE" TO FEATURE.                     SQ1464.2
048800     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1464.2
048900     MOVE    1 TO REC-CT.                                         SQ1464.2
049000     MOVE   "CLOSE-TEST-02"   TO PAR-NAME.                        SQ1464.2
049100     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ1464.2
049200                 TO DUMMY-RECORD.                                 SQ1464.2
049300     PERFORM WRITE-LINE 3 TIMES.                                  SQ1464.2
049400*                                                                 SQ1464.2
049500 CLOSE-TEST-02.                                                   SQ1464.2
049600     CLOSE SQ-FS1.                                                SQ1464.2
049700     IF SQ-FS1-STATUS = "42"                                      SQ1464.2
049800         PERFORM PASS                                             SQ1464.2
049900     ELSE                                                         SQ1464.2
050000         MOVE   "42" TO CORRECT-A                                 SQ1464.2
050100         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1464.2
050200         MOVE   "STATUS OF CLOSE OF CLOSED FILE INCORRECT"        SQ1464.2
050300                   TO RE-MARK                                     SQ1464.2
050400         MOVE   "VII-3, FILE STATUS" TO ANSI-REFERENCE            SQ1464.2
050500         PERFORM FAIL                                             SQ1464.2
050600     END-IF.                                                      SQ1464.2
050700*                                                                 SQ1464.2
050800 CCVS-EXIT SECTION.                                               SQ1464.2
050900 CCVS-999999.                                                     SQ1464.2
051000     GO TO CLOSE-FILES.                                           SQ1464.2
      *END-OF,SQ146A                                                                  
