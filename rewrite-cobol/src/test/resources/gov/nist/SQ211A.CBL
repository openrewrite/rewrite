      *HEADER,COBOL,SQ211A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2114.2
000200 PROGRAM-ID.                                                      SQ2114.2
000300     SQ211A.                                                      SQ2114.2
000400****************************************************************  SQ2114.2
000500*                                                              *  SQ2114.2
000600*    VALIDATION FOR:-                                          *  SQ2114.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2114.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ2114.2
000900*                                                              *  SQ2114.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ2114.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2114.2
001200*                                                              *  SQ2114.2
001300****************************************************************  SQ2114.2
001400*                                                              *  SQ2114.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2114.2
001600*                                                              *  SQ2114.2
001700*            X-01   SEQUENTIAL TAPE                            *  SQ2114.2
001800*            X-55   SYSTEM PRINTER                             *  SQ2114.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ2114.2
002000*            X-83   OBJECT-COMPUTER.                           *  SQ2114.2
002100*            X-84   LABEL RECORDS OPTION                       *  SQ2114.2
002200*                                                              *  SQ2114.2
002300****************************************************************  SQ2114.2
002400*                                                              *  SQ2114.2
002500*    SQ211A TESTS THE CLOSE STATEMENT WITH THE WITH LOCK PHRASE*  SQ2114.2
002600*    A MAGNETIC TAPE FILE WITH ONE RECORD IS CREATED AND IS    *  SQ2114.2
002700*    CLOSED WITH LOCK.  THE FILE IS THEN RE-OPENED AFTER IT HAS*  SQ2114.2
002800*    BEEN CLOSED WITH LOCK. THERE ARE NO DECLARATIVE           *  SQ2114.2
002900*    PROCEDURES. THE TEST FOR CORRECT I-O STATUS CODE IS IN    *  SQ2114.2
003000*    THE MAIN LINE CODE, THEREFORE AN ABNORMAL TERMINATION IS  *  SQ2114.2
003100*    POSSIBLE BEFORE THE TEST OF THE I-O STATUS CODE IS        *  SQ2114.2
003200*    ACCOMPLISHED.                                             *  SQ2114.2
003300*                                                              *  SQ2114.2
003400****************************************************************  SQ2114.2
003500*                                                                 SQ2114.2
003600 ENVIRONMENT DIVISION.                                            SQ2114.2
003700 CONFIGURATION SECTION.                                           SQ2114.2
003800 SOURCE-COMPUTER.                                                 SQ2114.2
003900     XXXXX082.                                                    SQ2114.2
004000 OBJECT-COMPUTER.                                                 SQ2114.2
004100     XXXXX083.                                                    SQ2114.2
004200*                                                                 SQ2114.2
004300 INPUT-OUTPUT SECTION.                                            SQ2114.2
004400 FILE-CONTROL.                                                    SQ2114.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SQ2114.2
004600     XXXXX055.                                                    SQ2114.2
004700*                                                                 SQ2114.2
004800     SELECT SQ-FS1 ASSIGN TO                                      SQ2114.2
004900     XXXXX001                                                     SQ2114.2
005000            FILE STATUS IS SQ-FS1-STATUS.                         SQ2114.2
005100*                                                                 SQ2114.2
005200*                                                                 SQ2114.2
005300 DATA DIVISION.                                                   SQ2114.2
005400 FILE SECTION.                                                    SQ2114.2
005500 FD  PRINT-FILE                                                   SQ2114.2
005600C    LABEL RECORDS                                                SQ2114.2
005700C    XXXXX084                                                     SQ2114.2
005800C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2114.2
005900               .                                                  SQ2114.2
006000 01  PRINT-REC    PICTURE X(120).                                 SQ2114.2
006100 01  DUMMY-RECORD PICTURE X(120).                                 SQ2114.2
006200*                                                                 SQ2114.2
006300 FD  SQ-FS1                                                       SQ2114.2
006400C    LABEL RECORD IS STANDARD                                     SQ2114.2
006500                .                                                 SQ2114.2
006600 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2114.2
006700*                                                                 SQ2114.2
006800 WORKING-STORAGE SECTION.                                         SQ2114.2
006900*                                                                 SQ2114.2
007000***************************************************************   SQ2114.2
007100*                                                             *   SQ2114.2
007200*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2114.2
007300*                                                             *   SQ2114.2
007400***************************************************************   SQ2114.2
007500*                                                                 SQ2114.2
007600 01  SQ-FS1-STATUS.                                               SQ2114.2
007700   03  SQ-FS1-KEY-1   PIC X.                                      SQ2114.2
007800   03  SQ-FS1-KEY-2   PIC X.                                      SQ2114.2
007900*                                                                 SQ2114.2
008000***************************************************************   SQ2114.2
008100*                                                             *   SQ2114.2
008200*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2114.2
008300*                                                             *   SQ2114.2
008400***************************************************************   SQ2114.2
008500*                                                                 SQ2114.2
008600 01  REC-SKEL-SUB   PIC 99.                                       SQ2114.2
008700*                                                                 SQ2114.2
008800 01  FILE-RECORD-INFORMATION-REC.                                 SQ2114.2
008900     03 FILE-RECORD-INFO-SKELETON.                                SQ2114.2
009000        05 FILLER                 PICTURE X(48)       VALUE       SQ2114.2
009100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2114.2
009200        05 FILLER                 PICTURE X(46)       VALUE       SQ2114.2
009300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2114.2
009400        05 FILLER                 PICTURE X(26)       VALUE       SQ2114.2
009500             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2114.2
009600        05 FILLER                 PICTURE X(37)       VALUE       SQ2114.2
009700             ",RECKEY=                             ".             SQ2114.2
009800        05 FILLER                 PICTURE X(38)       VALUE       SQ2114.2
009900             ",ALTKEY1=                             ".            SQ2114.2
010000        05 FILLER                 PICTURE X(38)       VALUE       SQ2114.2
010100             ",ALTKEY2=                             ".            SQ2114.2
010200        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2114.2
010300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2114.2
010400        05 FILE-RECORD-INFO-P1-120.                               SQ2114.2
010500           07 FILLER              PIC X(5).                       SQ2114.2
010600           07 XFILE-NAME          PIC X(6).                       SQ2114.2
010700           07 FILLER              PIC X(8).                       SQ2114.2
010800           07 XRECORD-NAME        PIC X(6).                       SQ2114.2
010900           07 FILLER              PIC X(1).                       SQ2114.2
011000           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2114.2
011100           07 FILLER              PIC X(7).                       SQ2114.2
011200           07 XRECORD-NUMBER      PIC 9(6).                       SQ2114.2
011300           07 FILLER              PIC X(6).                       SQ2114.2
011400           07 UPDATE-NUMBER       PIC 9(2).                       SQ2114.2
011500           07 FILLER              PIC X(5).                       SQ2114.2
011600           07 ODO-NUMBER          PIC 9(4).                       SQ2114.2
011700           07 FILLER              PIC X(5).                       SQ2114.2
011800           07 XPROGRAM-NAME       PIC X(5).                       SQ2114.2
011900           07 FILLER              PIC X(7).                       SQ2114.2
012000           07 XRECORD-LENGTH      PIC 9(6).                       SQ2114.2
012100           07 FILLER              PIC X(7).                       SQ2114.2
012200           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2114.2
012300           07 FILLER              PIC X(1).                       SQ2114.2
012400           07 XBLOCK-SIZE         PIC 9(4).                       SQ2114.2
012500           07 FILLER              PIC X(6).                       SQ2114.2
012600           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2114.2
012700           07 FILLER              PIC X(5).                       SQ2114.2
012800           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2114.2
012900           07 FILLER              PIC X(6).                       SQ2114.2
013000           07 XLABEL-TYPE         PIC X(1).                       SQ2114.2
013100        05 FILE-RECORD-INFO-P121-240.                             SQ2114.2
013200           07 FILLER              PIC X(8).                       SQ2114.2
013300           07 XRECORD-KEY         PIC X(29).                      SQ2114.2
013400           07 FILLER              PIC X(9).                       SQ2114.2
013500           07 ALTERNATE-KEY1      PIC X(29).                      SQ2114.2
013600           07 FILLER              PIC X(9).                       SQ2114.2
013700           07 ALTERNATE-KEY2      PIC X(29).                      SQ2114.2
013800           07 FILLER              PIC X(7).                       SQ2114.2
013900*                                                                 SQ2114.2
014000 01  TEST-RESULTS.                                                SQ2114.2
014100     02 FILLER              PIC X      VALUE SPACE.               SQ2114.2
014200     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2114.2
014300     02 FILLER              PIC X      VALUE SPACE.               SQ2114.2
014400     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2114.2
014500     02 FILLER              PIC X      VALUE SPACE.               SQ2114.2
014600     02  PAR-NAME.                                                SQ2114.2
014700       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2114.2
014800       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2114.2
014900       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2114.2
015000     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2114.2
015100     02 RE-MARK             PIC X(61).                            SQ2114.2
015200 01  TEST-COMPUTED.                                               SQ2114.2
015300   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2114.2
015400   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2114.2
015500   02 COMPUTED-X.                                                 SQ2114.2
015600     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2114.2
015700     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2114.2
015800     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2114.2
015900     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2114.2
016000     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2114.2
016100     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2114.2
016200        04 COMPUTED-18V0                   PIC -9(18).            SQ2114.2
016300        04 FILLER                          PIC X.                 SQ2114.2
016400     03 FILLER PIC X(50) VALUE SPACE.                             SQ2114.2
016500 01  TEST-CORRECT.                                                SQ2114.2
016600     02 FILLER PIC X(30) VALUE SPACE.                             SQ2114.2
016700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2114.2
016800     02 CORRECT-X.                                                SQ2114.2
016900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2114.2
017000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2114.2
017100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2114.2
017200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2114.2
017300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2114.2
017400     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2114.2
017500         04 CORRECT-18V0                     PIC -9(18).          SQ2114.2
017600         04 FILLER                           PIC X.               SQ2114.2
017700     03 FILLER PIC X(2) VALUE SPACE.                              SQ2114.2
017800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2114.2
017900 01  CCVS-C-1.                                                    SQ2114.2
018000     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ2114.2
018100     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ2114.2
018200-    "SS  PARAGRAPH-NAME                                          SQ2114.2
018300-    "       REMARKS".                                            SQ2114.2
018400     02 FILLER           PIC X(17)    VALUE SPACE.                SQ2114.2
018500 01  CCVS-C-2.                                                    SQ2114.2
018600     02 FILLER           PIC XXXX     VALUE SPACE.                SQ2114.2
018700     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ2114.2
018800     02 FILLER           PIC X(16)    VALUE SPACE.                SQ2114.2
018900     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ2114.2
019000     02 FILLER           PIC X(90)    VALUE SPACE.                SQ2114.2
019100 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2114.2
019200 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2114.2
019300 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2114.2
019400 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2114.2
019500 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2114.2
019600 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2114.2
019700 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2114.2
019800 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2114.2
019900 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2114.2
020000 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2114.2
020100 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2114.2
020200 01  CCVS-H-1.                                                    SQ2114.2
020300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2114.2
020400     02  FILLER          PIC X(42)    VALUE                       SQ2114.2
020500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2114.2
020600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2114.2
020700 01  CCVS-H-2A.                                                   SQ2114.2
020800   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2114.2
020900   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2114.2
021000   02  FILLER            PIC XXXX   VALUE                         SQ2114.2
021100     "4.2 ".                                                      SQ2114.2
021200   02  FILLER            PIC X(28)  VALUE                         SQ2114.2
021300            " COPY - NOT FOR DISTRIBUTION".                       SQ2114.2
021400   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2114.2
021500*                                                                 SQ2114.2
021600 01  CCVS-H-2B.                                                   SQ2114.2
021700   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2114.2
021800   02  TEST-ID           PIC X(9).                                SQ2114.2
021900   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2114.2
022000   02  FILLER            PIC X(12)  VALUE                         SQ2114.2
022100     " HIGH       ".                                              SQ2114.2
022200   02  FILLER            PIC X(22)  VALUE                         SQ2114.2
022300            " LEVEL VALIDATION FOR ".                             SQ2114.2
022400   02  FILLER            PIC X(58)  VALUE                         SQ2114.2
022500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2114.2
022600 01  CCVS-H-3.                                                    SQ2114.2
022700     02  FILLER          PIC X(34)  VALUE                         SQ2114.2
022800            " FOR OFFICIAL USE ONLY    ".                         SQ2114.2
022900     02  FILLER          PIC X(58)  VALUE                         SQ2114.2
023000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2114.2
023100     02  FILLER          PIC X(28)  VALUE                         SQ2114.2
023200            "  COPYRIGHT   1985,1986 ".                           SQ2114.2
023300 01  CCVS-E-1.                                                    SQ2114.2
023400     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2114.2
023500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2114.2
023600     02 ID-AGAIN         PIC X(9).                                SQ2114.2
023700     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2114.2
023800 01  CCVS-E-2.                                                    SQ2114.2
023900     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2114.2
024000     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2114.2
024100     02  CCVS-E-2-2.                                              SQ2114.2
024200         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2114.2
024300         03 FILLER         PIC X      VALUE SPACE.                SQ2114.2
024400         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2114.2
024500            "ERRORS ENCOUNTERED".                                 SQ2114.2
024600 01  CCVS-E-3.                                                    SQ2114.2
024700     02  FILLER          PIC X(22)  VALUE                         SQ2114.2
024800            " FOR OFFICIAL USE ONLY".                             SQ2114.2
024900     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2114.2
025000     02  FILLER          PIC X(58)  VALUE                         SQ2114.2
025100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2114.2
025200     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2114.2
025300     02  FILLER          PIC X(20)  VALUE                         SQ2114.2
025400             " COPYRIGHT 1985,1986".                              SQ2114.2
025500 01  CCVS-E-4.                                                    SQ2114.2
025600     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2114.2
025700     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2114.2
025800     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2114.2
025900     02 FILLER           PIC X(40)  VALUE                         SQ2114.2
026000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2114.2
026100 01  XXINFO.                                                      SQ2114.2
026200     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2114.2
026300     02 INFO-TEXT.                                                SQ2114.2
026400       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2114.2
026500       04 XXCOMPUTED         PIC X(20).                           SQ2114.2
026600       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2114.2
026700       04 XXCORRECT          PIC X(20).                           SQ2114.2
026800     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2114.2
026900 01  HYPHEN-LINE.                                                 SQ2114.2
027000     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2114.2
027100     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2114.2
027200-    "*****************************************".                 SQ2114.2
027300     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2114.2
027400-    "******************************".                            SQ2114.2
027500 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2114.2
027600     "SQ211A".                                                    SQ2114.2
027700*                                                                 SQ2114.2
027800 PROCEDURE DIVISION.                                              SQ2114.2
027900 CCVS1 SECTION.                                                   SQ2114.2
028000 OPEN-FILES.                                                      SQ2114.2
028100     OPEN    OUTPUT PRINT-FILE.                                   SQ2114.2
028200     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2114.2
028300     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2114.2
028400     MOVE    SPACE TO TEST-RESULTS.                               SQ2114.2
028500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2114.2
028600     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2114.2
028700     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2114.2
028800     GO TO CCVS1-EXIT.                                            SQ2114.2
028900*                                                                 SQ2114.2
029000 CCVS-INIT-FILE.                                                  SQ2114.2
029100     ADD     1 TO REC-SKL-SUB.                                    SQ2114.2
029200     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2114.2
029300                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2114.2
029400*                                                                 SQ2114.2
029500 CLOSE-FILES.                                                     SQ2114.2
029600     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2114.2
029700     CLOSE   PRINT-FILE.                                          SQ2114.2
029800 TERMINATE-CCVS.                                                  SQ2114.2
029900     STOP    RUN.                                                 SQ2114.2
030000*                                                                 SQ2114.2
030100 INSPT.                                                           SQ2114.2
030200     MOVE   "INSPT" TO P-OR-F.                                    SQ2114.2
030300     ADD     1 TO INSPECT-COUNTER.                                SQ2114.2
030400     PERFORM PRINT-DETAIL.                                        SQ2114.2
030500                                                                  SQ2114.2
030600 PASS.                                                            SQ2114.2
030700     MOVE   "PASS " TO P-OR-F.                                    SQ2114.2
030800     ADD     1 TO PASS-COUNTER.                                   SQ2114.2
030900     PERFORM PRINT-DETAIL.                                        SQ2114.2
031000*                                                                 SQ2114.2
031100 FAIL.                                                            SQ2114.2
031200     MOVE   "FAIL*" TO P-OR-F.                                    SQ2114.2
031300     ADD     1 TO ERROR-COUNTER.                                  SQ2114.2
031400     PERFORM PRINT-DETAIL.                                        SQ2114.2
031500*                                                                 SQ2114.2
031600 DE-LETE.                                                         SQ2114.2
031700     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2114.2
031800     MOVE   "*****" TO P-OR-F.                                    SQ2114.2
031900     ADD     1 TO DELETE-COUNTER.                                 SQ2114.2
032000     PERFORM PRINT-DETAIL.                                        SQ2114.2
032100*                                                                 SQ2114.2
032200 PRINT-DETAIL.                                                    SQ2114.2
032300     IF REC-CT NOT EQUAL TO ZERO                                  SQ2114.2
032400         MOVE   "." TO PARDOT-X                                   SQ2114.2
032500         MOVE    REC-CT TO DOTVALUE.                              SQ2114.2
032600     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2114.2
032700     PERFORM WRITE-LINE.                                          SQ2114.2
032800     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2114.2
032900         PERFORM WRITE-LINE                                       SQ2114.2
033000         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2114.2
033100     ELSE                                                         SQ2114.2
033200         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2114.2
033300     MOVE    SPACE TO P-OR-F.                                     SQ2114.2
033400     MOVE    SPACE TO COMPUTED-X.                                 SQ2114.2
033500     MOVE    SPACE TO CORRECT-X.                                  SQ2114.2
033600     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2114.2
033700     MOVE    SPACE TO RE-MARK.                                    SQ2114.2
033800*                                                                 SQ2114.2
033900 HEAD-ROUTINE.                                                    SQ2114.2
034000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2114.2
034100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2114.2
034200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2114.2
034300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2114.2
034400 COLUMN-NAMES-ROUTINE.                                            SQ2114.2
034500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2114.2
034600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2114.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2114.2
034800 END-ROUTINE.                                                     SQ2114.2
034900     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2114.2
035000     PERFORM WRITE-LINE 5 TIMES.                                  SQ2114.2
035100 END-RTN-EXIT.                                                    SQ2114.2
035200     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2114.2
035300     PERFORM WRITE-LINE 2 TIMES.                                  SQ2114.2
035400*                                                                 SQ2114.2
035500 END-ROUTINE-1.                                                   SQ2114.2
035600     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2114.2
035700     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2114.2
035800     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2114.2
035900     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2114.2
036000     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2114.2
036100     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2114.2
036200     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2114.2
036300     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2114.2
036400     PERFORM WRITE-LINE.                                          SQ2114.2
036500     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2114.2
036600     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2114.2
036700         MOVE   "NO " TO ERROR-TOTAL                              SQ2114.2
036800     ELSE                                                         SQ2114.2
036900         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2114.2
037000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2114.2
037100     PERFORM WRITE-LINE.                                          SQ2114.2
037200 END-ROUTINE-13.                                                  SQ2114.2
037300     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2114.2
037400         MOVE   "NO " TO ERROR-TOTAL                              SQ2114.2
037500     ELSE                                                         SQ2114.2
037600         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2114.2
037700     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2114.2
037800     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2114.2
037900     PERFORM WRITE-LINE.                                          SQ2114.2
038000     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2114.2
038100         MOVE   "NO " TO ERROR-TOTAL                              SQ2114.2
038200     ELSE                                                         SQ2114.2
038300         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2114.2
038400     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2114.2
038500     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2114.2
038600     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2114.2
038700*                                                                 SQ2114.2
038800 WRITE-LINE.                                                      SQ2114.2
038900     ADD     1 TO RECORD-COUNT.                                   SQ2114.2
039000*    IF RECORD-COUNT GREATER 50                                   SQ2114.2
039100*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2114.2
039200*        MOVE  SPACE TO DUMMY-RECORD                              SQ2114.2
039300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2114.2
039400*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2114.2
039500*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2114.2
039600*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2114.2
039700*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2114.2
039800*        MOVE  ZERO TO RECORD-COUNT.                              SQ2114.2
039900     PERFORM WRT-LN.                                              SQ2114.2
040000*                                                                 SQ2114.2
040100 WRT-LN.                                                          SQ2114.2
040200     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2114.2
040300     MOVE    SPACE TO DUMMY-RECORD.                               SQ2114.2
040400 BLANK-LINE-PRINT.                                                SQ2114.2
040500     PERFORM WRT-LN.                                              SQ2114.2
040600 FAIL-ROUTINE.                                                    SQ2114.2
040700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2114.2
040800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2114.2
040900     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2114.2
041000     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2114.2
041100     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2114.2
041200     PERFORM WRITE-LINE 2 TIMES.                                  SQ2114.2
041300     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2114.2
041400     GO TO   FAIL-ROUTINE-EX.                                     SQ2114.2
041500 FAIL-ROUTINE-WRITE.                                              SQ2114.2
041600     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2114.2
041700     PERFORM WRITE-LINE                                           SQ2114.2
041800     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2114.2
041900     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2114.2
042000     PERFORM WRITE-LINE 2 TIMES.                                  SQ2114.2
042100     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2114.2
042200 FAIL-ROUTINE-EX.                                                 SQ2114.2
042300     EXIT.                                                        SQ2114.2
042400 BAIL-OUT.                                                        SQ2114.2
042500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2114.2
042600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2114.2
042700 BAIL-OUT-WRITE.                                                  SQ2114.2
042800     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2114.2
042900     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2114.2
043000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2114.2
043100     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2114.2
043200     PERFORM WRITE-LINE 2 TIMES.                                  SQ2114.2
043300     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2114.2
043400 BAIL-OUT-EX.                                                     SQ2114.2
043500     EXIT.                                                        SQ2114.2
043600 CCVS1-EXIT.                                                      SQ2114.2
043700     EXIT.                                                        SQ2114.2
043800*                                                                 SQ2114.2
043900****************************************************************  SQ2114.2
044000*                                                              *  SQ2114.2
044100*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2114.2
044200*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2114.2
044300*                                                              *  SQ2114.2
044400****************************************************************  SQ2114.2
044500*                                                                 SQ2114.2
044600 SECT-SQ211A-0001 SECTION.                                        SQ2114.2
044700 WRITE-INIT-GF-01.                                                SQ2114.2
044800*                                                                 SQ2114.2
044900*        THIS TEST CREATES FILE SQ-FS1 AND CLOSES IT WITH LOCK.   SQ2114.2
045000*        FIRST IT SETS UP A SKELETON RECORD IN WORKING STORAGE.   SQ2114.2
045100*                                                                 SQ2114.2
045200     MOVE "SQ-FS1"     TO XFILE-NAME (1).                         SQ2114.2
045300     MOVE "R1-F-G"     TO XRECORD-NAME (1).                       SQ2114.2
045400     MOVE  CCVS-PGM-ID TO XPROGRAM-NAME (1).                      SQ2114.2
045500     MOVE 120          TO XRECORD-LENGTH (1).                     SQ2114.2
045600     MOVE "RC"         TO CHARS-OR-RECORDS (1).                   SQ2114.2
045700     MOVE 1            TO XBLOCK-SIZE (1).                        SQ2114.2
045800     MOVE 1            TO RECORDS-IN-FILE (1).                    SQ2114.2
045900     MOVE "SQ"         TO XFILE-ORGANIZATION (1).                 SQ2114.2
046000     MOVE "S"          TO XLABEL-TYPE (1).                        SQ2114.2
046100     MOVE 1            TO XRECORD-NUMBER (1).                     SQ2114.2
046200*                                                                 SQ2114.2
046300 WRITE-OPEN-01.                                                   SQ2114.2
046400     MOVE    1 TO REC-CT.                                         SQ2114.2
046500     MOVE   "WRITE-OPEN-01" TO PAR-NAME.                          SQ2114.2
046600     MOVE   "OPEN OUTPUT - NEW FILE" TO FEATURE.                  SQ2114.2
046700     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2114.2
046800     OPEN    OUTPUT SQ-FS1.                                       SQ2114.2
046900     IF SQ-FS1-STATUS = "00"                                      SQ2114.2
047000         PERFORM PASS                                             SQ2114.2
047100     ELSE                                                         SQ2114.2
047200         MOVE   "00" TO CORRECT-A                                 SQ2114.2
047300         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2114.2
047400         MOVE   "FILE OPEN FAILED, FURTHER TESTS ABANDONED"       SQ2114.2
047500                   TO RE-MARK                                     SQ2114.2
047600         MOVE   "VII-3, VII-40, FILE STATUS" TO ANSI-REFERENCE    SQ2114.2
047700         PERFORM FAIL                                             SQ2114.2
047800         GO TO   CCVS-EXIT                                        SQ2114.2
047900     END-IF.                                                      SQ2114.2
048000*                                                                 SQ2114.2
048100*        WRITE A SINGLE RECORD TO THE FILE                        SQ2114.2
048200*                                                                 SQ2114.2
048300 WRITE-INIT-01.                                                   SQ2114.2
048400     MOVE    1 TO REC-CT.                                         SQ2114.2
048500     MOVE   "WRITE-TEST-01" TO PAR-NAME                           SQ2114.2
048600     MOVE   "SEQUENTIAL WRITE" TO FEATURE.                        SQ2114.2
048700 WRITE-TEST-01-01.                                                SQ2114.2
048800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.     SQ2114.2
048900     WRITE   SQ-FS1R1-F-G-120.                                    SQ2114.2
049000     IF SQ-FS1-STATUS = "00"                                      SQ2114.2
049100         PERFORM PASS                                             SQ2114.2
049200     ELSE                                                         SQ2114.2
049300         MOVE   "00" TO CORRECT-A                                 SQ2114.2
049400         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2114.2
049500         MOVE   "WRITING FAILED, FURTHER TESTS ABANDONED"         SQ2114.2
049600                   TO RE-MARK                                     SQ2114.2
049700         MOVE   "VII-3, VII-53, FILE STATUS" TO ANSI-REFERENCE    SQ2114.2
049800         PERFORM FAIL                                             SQ2114.2
049900         GO TO   CCVS-EXIT                                        SQ2114.2
050000     END-IF.                                                      SQ2114.2
050100*                                                                 SQ2114.2
050200*        CLOSE THE FILE WITH LOCK, SO IT SHOULD NOT REOPEN        SQ2114.2
050300*                                                                 SQ2114.2
050400 CLOSE-INIT-01.                                                   SQ2114.2
050500     MOVE    1 TO REC-CT.                                         SQ2114.2
050600     MOVE   "CLOSE-TEST-01"   TO PAR-NAME.                        SQ2114.2
050700     MOVE   "CLOSE WITH LOCK" TO FEATURE.                         SQ2114.2
050800     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2114.2
050900 CLOSE-TEST-01.                                                   SQ2114.2
051000     CLOSE   SQ-FS1 WITH LOCK.                                    SQ2114.2
051100     IF SQ-FS1-STATUS = "00"                                      SQ2114.2
051200         PERFORM PASS                                             SQ2114.2
051300     ELSE                                                         SQ2114.2
051400         MOVE   "00" TO CORRECT-A                                 SQ2114.2
051500         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2114.2
051600         MOVE   "CLOSE WITH LOCK FAILED, FURTHER TESTS ABANDONED" SQ2114.2
051700                   TO RE-MARK                                     SQ2114.2
051800         MOVE   "VII-3, VII-38, FILE STATUS" TO ANSI-REFERENCE    SQ2114.2
051900         PERFORM FAIL                                             SQ2114.2
052000         GO TO   CCVS-EXIT                                        SQ2114.2
052100     END-IF.                                                      SQ2114.2
052200*                                                                 SQ2114.2
052300*        HAVING LOCKED THE FILE, WE NOW TRY TO REOPEN IT.         SQ2114.2
052400*        THE TEST PASSES IF THE FILE CANNOT BE OPENED AND         SQ2114.2
052500*        THE APPROPRIATE I-O STATUS VALUE IS RETURNED.            SQ2114.2
052600*        AN IMPLEMENTATION MAY TERMINATE EXECUTION OF THE         SQ2114.2
052700*        PROGRAM ON EXIT FROM THE DECLARATIVE ASSOCIATED          SQ2114.2
052800*        WITH THE FILE, OR MAY RETURN CONTROL TO THE              SQ2114.2
052900*        STATEMENT FOLLOWING THE OPEN STATEMENT.                  SQ2114.2
053000*                                                                 SQ2114.2
053100 OPEN-INIT-01.                                                    SQ2114.2
053200*                                                                 SQ2114.2
053300     MOVE   "OPEN AFTER LOCK" TO FEATURE.                         SQ2114.2
053400     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2114.2
053500 OPEN-TEST-01.                                                    SQ2114.2
053600     MOVE    1 TO REC-CT.                                         SQ2114.2
053700     MOVE   "OPEN-TEST-01"    TO PAR-NAME.                        SQ2114.2
053800     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ2114.2
053900                 TO DUMMY-RECORD.                                 SQ2114.2
054000     PERFORM WRITE-LINE 3 TIMES.                                  SQ2114.2
054100     OPEN INPUT SQ-FS1.                                           SQ2114.2
054200     IF SQ-FS1-STATUS = "38"                                      SQ2114.2
054300         PERFORM PASS                                             SQ2114.2
054400     ELSE                                                         SQ2114.2
054500         MOVE SQ-FS1-STATUS TO COMPUTED-A                         SQ2114.2
054600         MOVE    "38"       TO CORRECT-A                          SQ2114.2
054700         MOVE "STATUS OF OPEN AFTER CLOSE WITH LOCK INCORRECT"    SQ2114.2
054800                 TO RE-MARK                                       SQ2114.2
054900         PERFORM FAIL                                             SQ2114.2
055000     END-IF.                                                      SQ2114.2
055100*                                                                 SQ2114.2
055200 CCVS-EXIT SECTION.                                               SQ2114.2
055300 CCVS-999999.                                                     SQ2114.2
055400     GO TO CLOSE-FILES.                                           SQ2114.2
      *END-OF,SQ211A                                                                  
