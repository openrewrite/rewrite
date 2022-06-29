      *HEADER,COBOL,SQ132A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1324.2
000200 PROGRAM-ID.                                                      SQ1324.2
000300     SQ132A.                                                      SQ1324.2
000400***************************************************************   SQ1324.2
000500*                                                             *   SQ1324.2
000600*    VALIDATION FOR:-                                         *   SQ1324.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1324.2
000800*    USING CCVS85 VERSION 4.2.                                *   SQ1324.2
000900*                                                             *   SQ1324.2
001000*    CREATION DATE     /     VALIDATION DATE                  *   SQ1324.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1324.2
001200*                                                             *   SQ1324.2
001300***************************************************************   SQ1324.2
001400*                                                             *   SQ1324.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                    *   SQ1324.2
001600*                                                             *   SQ1324.2
001700*            X-14   SEQUENTIAL MASS STORAGE FILE.             *   SQ1324.2
001800*            X-55   SYSTEM PRINTER                            *   SQ1324.2
001900*            X-82   SOURCE-COMPUTER                           *   SQ1324.2
002000*            X-83   OBJECT-COMPUTER                           *   SQ1324.2
002100*            X-84   LABEL RECORDS OPTION                      *   SQ1324.2
002200*                                                             *   SQ1324.2
002300***************************************************************   SQ1324.2
002400*                                                             *   SQ1324.2
002500*    THIS PROGRAM CHECKS FOR THE CORRECT RESPONSE TOCLOSING   *   SQ1324.2
002600*    AN UNOPENED FILE.  THE TEST FOR CORRECT I-O STATUS CODE  *   SQ1324.2
002700*    42 IS IN THE DECLARATIVES.  AN ABNORMAL TERMINATION IS   *   SQ1324.2
002800*    POSSIBLE AFTER THE TEST OF THE I-O STATUS CODE IS        *   SQ1324.2
002900*    ACCOMPLISHED BUT BEFORE CONTROL IS RETURNED TO THE MAIN  *   SQ1324.2
003000*    LINE CODE.                                               *   SQ1324.2
003100*                                                             *   SQ1324.2
003200***************************************************************   SQ1324.2
003300*                                                                 SQ1324.2
003400 ENVIRONMENT DIVISION.                                            SQ1324.2
003500 CONFIGURATION SECTION.                                           SQ1324.2
003600 SOURCE-COMPUTER.                                                 SQ1324.2
003700     XXXXX082.                                                    SQ1324.2
003800 OBJECT-COMPUTER.                                                 SQ1324.2
003900     XXXXX083.                                                    SQ1324.2
004000*                                                                 SQ1324.2
004100 INPUT-OUTPUT SECTION.                                            SQ1324.2
004200 FILE-CONTROL.                                                    SQ1324.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ1324.2
004400     XXXXX055.                                                    SQ1324.2
004500*                                                                 SQ1324.2
004600     SELECT SQ-FS1 ASSIGN TO                                      SQ1324.2
004700     XXXXX014                                                     SQ1324.2
004800            FILE STATUS SQ-FS1-STATUS.                            SQ1324.2
004900*                                                                 SQ1324.2
005000*                                                                 SQ1324.2
005100 DATA DIVISION.                                                   SQ1324.2
005200 FILE SECTION.                                                    SQ1324.2
005300 FD  PRINT-FILE                                                   SQ1324.2
005400C    LABEL RECORDS                                                SQ1324.2
005500C    XXXXX084                                                     SQ1324.2
005600C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1324.2
005700               .                                                  SQ1324.2
005800 01  PRINT-REC    PICTURE X(120).                                 SQ1324.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 SQ1324.2
006000*                                                                 SQ1324.2
006100 FD  SQ-FS1                                                       SQ1324.2
006200C    LABEL RECORD IS STANDARD                                     SQ1324.2
006300                .                                                 SQ1324.2
006400 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1324.2
006500*                                                                 SQ1324.2
006600 WORKING-STORAGE SECTION.                                         SQ1324.2
006700*                                                                 SQ1324.2
006800**************************************************************    SQ1324.2
006900*                                                            *    SQ1324.2
007000*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE  *    SQ1324.2
007100*                                                            *    SQ1324.2
007200**************************************************************    SQ1324.2
007300*                                                                 SQ1324.2
007400 01  SQ-FS1-STATUS.                                               SQ1324.2
007500   03  SQ-FS1-KEY-1   PIC X.                                      SQ1324.2
007600   03  SQ-FS1-KEY-2   PIC X.                                      SQ1324.2
007700*                                                                 SQ1324.2
007800**************************************************************    SQ1324.2
007900*                                                            *    SQ1324.2
008000*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS             *    SQ1324.2
008100*                                                            *    SQ1324.2
008200**************************************************************    SQ1324.2
008300*                                                                 SQ1324.2
008400 01  REC-SKEL-SUB   PIC 99.                                       SQ1324.2
008500*                                                                 SQ1324.2
008600 01  FILE-RECORD-INFORMATION-REC.                                 SQ1324.2
008700     03 FILE-RECORD-INFO-SKELETON.                                SQ1324.2
008800        05 FILLER                 PICTURE X(48)       VALUE       SQ1324.2
008900             "FILE=      ,RECORD=     /0,RECNO=000000,UPDT=00".   SQ1324.2
009000        05 FILLER                 PICTURE X(46)       VALUE       SQ1324.2
009100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ =0000".     SQ1324.2
009200        05 FILLER                 PICTURE X(26)       VALUE       SQ1324.2
009300             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1324.2
009400        05 FILLER                 PICTURE X(37)       VALUE       SQ1324.2
009500             ",RECKEY=                             ".             SQ1324.2
009600        05 FILLER                 PICTURE X(38)       VALUE       SQ1324.2
009700             ",ALTKEY1=                             ".            SQ1324.2
009800        05 FILLER                 PICTURE X(38)       VALUE       SQ1324.2
009900             ",ALTKEY2=                             ".            SQ1324.2
010000        05 FILLER                 PICTURE X(7)       VALUE SPACE. SQ1324.2
010100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1324.2
010200        05 FILE-RECORD-INFO-P1-120.                               SQ1324.2
010300           07 FILLER              PIC X(5).                       SQ1324.2
010400           07 XFILE-NAME          PIC X(6).                       SQ1324.2
010500           07 FILLER              PIC X(8).                       SQ1324.2
010600           07 XRECORD-NAME        PIC X(6).                       SQ1324.2
010700           07 FILLER              PIC X(1).                       SQ1324.2
010800           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1324.2
010900           07 FILLER              PIC X(7).                       SQ1324.2
011000           07 XRECORD-NUMBER      PIC 9(6).                       SQ1324.2
011100           07 FILLER              PIC X(6).                       SQ1324.2
011200           07 UPDATE-NUMBER       PIC 9(2).                       SQ1324.2
011300           07 FILLER              PIC X(5).                       SQ1324.2
011400           07 ODO-NUMBER          PIC 9(4).                       SQ1324.2
011500           07 FILLER              PIC X(5).                       SQ1324.2
011600           07 XPROGRAM-NAME       PIC X(5).                       SQ1324.2
011700           07 FILLER              PIC X(7).                       SQ1324.2
011800           07 XRECORD-LENGTH      PIC 9(6).                       SQ1324.2
011900           07 FILLER              PIC X(7).                       SQ1324.2
012000           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1324.2
012100           07 FILLER              PIC X(1).                       SQ1324.2
012200           07 XBLOCK-SIZE         PIC 9(4).                       SQ1324.2
012300           07 FILLER              PIC X(6).                       SQ1324.2
012400           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1324.2
012500           07 FILLER              PIC X(5).                       SQ1324.2
012600           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1324.2
012700           07 FILLER              PIC X(6).                       SQ1324.2
012800           07 XLABEL-TYPE         PIC X(1).                       SQ1324.2
012900        05 FILE-RECORD-INFO-P121-240.                             SQ1324.2
013000           07 FILLER              PIC X(8).                       SQ1324.2
013100           07 XRECORD-KEY         PIC X(29).                      SQ1324.2
013200           07 FILLER              PIC X(9).                       SQ1324.2
013300           07 ALTERNATE-KEY1      PIC X(29).                      SQ1324.2
013400           07 FILLER              PIC X(9).                       SQ1324.2
013500           07 ALTERNATE-KEY2      PIC X(29).                      SQ1324.2
013600           07 FILLER              PIC X(7).                       SQ1324.2
013700*                                                                 SQ1324.2
013800 01  TEST-RESULTS.                                                SQ1324.2
013900     02 FILLER              PIC X      VALUE SPACE.               SQ1324.2
014000     02  PAR-NAME.                                                SQ1324.2
014100       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1324.2
014200       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1324.2
014300       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1324.2
014400     02 FILLER              PIC X      VALUE SPACE.               SQ1324.2
014500     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1324.2
014600     02 FILLER              PIC X      VALUE SPACE.               SQ1324.2
014700     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1324.2
014800     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1324.2
014900     02 RE-MARK             PIC X(61).                            SQ1324.2
015000 01  TEST-COMPUTED.                                               SQ1324.2
015100   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1324.2
015200   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1324.2
015300   02 COMPUTED-X.                                                 SQ1324.2
015400     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1324.2
015500     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1324.2
015600     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1324.2
015700     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1324.2
015800     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1324.2
015900     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1324.2
016000        04 COMPUTED-18V0                   PIC -9(18).            SQ1324.2
016100        04 FILLER                          PIC X.                 SQ1324.2
016200     03 FILLER PIC X(50) VALUE SPACE.                             SQ1324.2
016300 01  TEST-CORRECT.                                                SQ1324.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             SQ1324.2
016500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1324.2
016600     02 CORRECT-X.                                                SQ1324.2
016700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1324.2
016800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1324.2
016900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1324.2
017000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1324.2
017100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1324.2
017200     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1324.2
017300         04 CORRECT-18V0                     PIC -9(18).          SQ1324.2
017400         04 FILLER                           PIC X.               SQ1324.2
017500     03 FILLER PIC X(2) VALUE SPACE.                              SQ1324.2
017600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1324.2
017700*                                                                 SQ1324.2
017800 01  CCVS-C-1.                                                    SQ1324.2
017900     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1324.2
018000     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1324.2
018100     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1324.2
018200     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1324.2
018300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1324.2
018400     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1324.2
018500     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1324.2
018600     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1324.2
018700 01  CCVS-C-2.                                                    SQ1324.2
018800     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1324.2
018900     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1324.2
019000     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1324.2
019100     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1324.2
019200     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1324.2
019300*                                                                 SQ1324.2
019400 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1324.2
019500 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1324.2
019600 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1324.2
019700 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1324.2
019800 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1324.2
019900 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1324.2
020000 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1324.2
020100 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1324.2
020200 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1324.2
020300 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1324.2
020400 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1324.2
020500 01  CCVS-H-1.                                                    SQ1324.2
020600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1324.2
020700     02  FILLER          PIC X(42)    VALUE                       SQ1324.2
020800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1324.2
020900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1324.2
021000 01  CCVS-H-2A.                                                   SQ1324.2
021100   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1324.2
021200   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1324.2
021300   02  FILLER            PIC XXXX   VALUE                         SQ1324.2
021400     "4.2 ".                                                      SQ1324.2
021500   02  FILLER            PIC X(28)  VALUE                         SQ1324.2
021600            " COPY - NOT FOR DISTRIBUTION".                       SQ1324.2
021700   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1324.2
021800*                                                                 SQ1324.2
021900 01  CCVS-H-2B.                                                   SQ1324.2
022000   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF".       SQ1324.2
022100   02  TEST-ID           PIC X(9).                                SQ1324.2
022200   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1324.2
022300   02  FILLER            PIC X(12)  VALUE                         SQ1324.2
022400     " HIGH       ".                                              SQ1324.2
022500   02  FILLER            PIC X(22)  VALUE                         SQ1324.2
022600            " LEVEL VALIDATION FOR ".                             SQ1324.2
022700   02  FILLER            PIC X(58)  VALUE                         SQ1324.2
022800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1324.2
022900 01  CCVS-H-3.                                                    SQ1324.2
023000     02  FILLER          PIC X(34)  VALUE                         SQ1324.2
023100            " FOR OFFICIAL USE ONLY    ".                         SQ1324.2
023200     02  FILLER          PIC X(58)  VALUE                         SQ1324.2
023300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1324.2
023400     02  FILLER          PIC X(28)  VALUE                         SQ1324.2
023500            "  COPYRIGHT   1985,1986 ".                           SQ1324.2
023600 01  CCVS-E-1.                                                    SQ1324.2
023700     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1324.2
023800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1324.2
023900     02 ID-AGAIN         PIC X(9).                                SQ1324.2
024000     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1324.2
024100 01  CCVS-E-2.                                                    SQ1324.2
024200     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1324.2
024300     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1324.2
024400     02  CCVS-E-2-2.                                              SQ1324.2
024500         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1324.2
024600         03 FILLER         PIC X      VALUE SPACE.                SQ1324.2
024700         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1324.2
024800            "ERRORS ENCOUNTERED".                                 SQ1324.2
024900 01  CCVS-E-3.                                                    SQ1324.2
025000     02  FILLER          PIC X(22)  VALUE                         SQ1324.2
025100            " FOR OFFICIAL USE ONLY".                             SQ1324.2
025200     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1324.2
025300     02  FILLER          PIC X(58)  VALUE                         SQ1324.2
025400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1324.2
025500     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1324.2
025600     02  FILLER          PIC X(20)  VALUE                         SQ1324.2
025700             " COPYRIGHT 1985,1986".                              SQ1324.2
025800 01  CCVS-E-4.                                                    SQ1324.2
025900     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1324.2
026000     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1324.2
026100     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1324.2
026200     02 FILLER           PIC X(40)  VALUE                         SQ1324.2
026300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1324.2
026400 01  XXINFO.                                                      SQ1324.2
026500     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1324.2
026600     02 INFO-TEXT.                                                SQ1324.2
026700       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1324.2
026800       04 XXCOMPUTED         PIC X(20).                           SQ1324.2
026900       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1324.2
027000       04 XXCORRECT          PIC X(20).                           SQ1324.2
027100     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1324.2
027200 01  HYPHEN-LINE.                                                 SQ1324.2
027300     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1324.2
027400     02 FILLER  PIC IS X(65)   VALUE IS  "************************SQ1324.2
027500-    "*****************************************".                 SQ1324.2
027600     02 FILLER  PIC IS X(54)   VALUE IS  "************************SQ1324.2
027700-    "******************************".                            SQ1324.2
027800 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1324.2
027900     "SQ132A".                                                    SQ1324.2
028000*                                                                 SQ1324.2
028100*                                                                 SQ1324.2
028200 PROCEDURE DIVISION.                                              SQ1324.2
028300 DECLARATIVES.                                                    SQ1324.2
028400 SQ132A-DECLARATIVE-001-SECT SECTION.                             SQ1324.2
028500     USE AFTER STANDARD ERROR PROCEDURE SQ-FS1.                   SQ1324.2
028600 SQ-FS1-ERROR-PROCEDURE.                                          SQ1324.2
028700 DECL-CLOSE-01.                                                   SQ1324.2
028800     IF SQ-FS1-STATUS = "42"                                      SQ1324.2
028900         PERFORM DECL-PASS                                        SQ1324.2
029000         GO TO DECL-ABNORMAL-TERM                                 SQ1324.2
029100     ELSE                                                         SQ1324.2
029200         MOVE   "42"           TO CORRECT-A                       SQ1324.2
029300         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1324.2
029400         MOVE   "STATUS FOR CLOSE OF UNOPENED FILE INCORRECT"     SQ1324.2
029500                   TO RE-MARK                                     SQ1324.2
029600         PERFORM DECL-FAIL                                        SQ1324.2
029700         GO TO DECL-ABNORMAL-TERM                                 SQ1324.2
029800     END-IF.                                                      SQ1324.2
029900*                                                                 SQ1324.2
030000 DECL-PASS.                                                       SQ1324.2
030100     MOVE   "PASS " TO P-OR-F.                                    SQ1324.2
030200     ADD     1 TO PASS-COUNTER.                                   SQ1324.2
030300     PERFORM DECL-PRINT-DETAIL.                                   SQ1324.2
030400*                                                                 SQ1324.2
030500 DECL-FAIL.                                                       SQ1324.2
030600     MOVE   "FAIL*" TO P-OR-F.                                    SQ1324.2
030700     ADD     1 TO ERROR-COUNTER.                                  SQ1324.2
030800     PERFORM DECL-PRINT-DETAIL.                                   SQ1324.2
030900*                                                                 SQ1324.2
031000 DECL-PRINT-DETAIL.                                               SQ1324.2
031100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1324.2
031200             MOVE "." TO PARDOT-X                                 SQ1324.2
031300             MOVE REC-CT TO DOTVALUE.                             SQ1324.2
031400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1324.2
031500     PERFORM DECL-WRITE-LINE.                                     SQ1324.2
031600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1324.2
031700         PERFORM DECL-WRITE-LINE                                  SQ1324.2
031800         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1324.2
031900     ELSE                                                         SQ1324.2
032000         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1324.2
032100     MOVE    SPACE TO P-OR-F.                                     SQ1324.2
032200     MOVE    SPACE TO COMPUTED-X.                                 SQ1324.2
032300     MOVE    SPACE TO CORRECT-X.                                  SQ1324.2
032400     IF REC-CT EQUAL TO ZERO                                      SQ1324.2
032500         MOVE    SPACE TO PAR-NAME.                               SQ1324.2
032600     MOVE    SPACE TO RE-MARK.                                    SQ1324.2
032700*                                                                 SQ1324.2
032800 DECL-WRITE-LINE.                                                 SQ1324.2
032900     ADD     1 TO RECORD-COUNT.                                   SQ1324.2
033000*    IF RECORD-COUNT GREATER 50                                   SQ1324.2
033100*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1324.2
033200*        MOVE    SPACE TO DUMMY-RECORD                            SQ1324.2
033300*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1324.2
033400*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1324.2
033500*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1324.2
033600*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1324.2
033700*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1324.2
033800*        PERFORM DECL-WRT-LN                                      SQ1324.2
033900*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1324.2
034000*        MOVE    ZERO TO RECORD-COUNT.                            SQ1324.2
034100     PERFORM DECL-WRT-LN.                                         SQ1324.2
034200*                                                                 SQ1324.2
034300 DECL-WRT-LN.                                                     SQ1324.2
034400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1324.2
034500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1324.2
034600*                                                                 SQ1324.2
034700 DECL-FAIL-ROUTINE.                                               SQ1324.2
034800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1324.2
034900     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1324.2
035000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1324.2
035100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1324.2
035200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1324.2
035300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1324.2
035400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1324.2
035500     GO TO   DECL-FAIL-EX.                                        SQ1324.2
035600 DECL-FAIL-WRITE.                                                 SQ1324.2
035700     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1324.2
035800     PERFORM DECL-WRITE-LINE                                      SQ1324.2
035900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1324.2
036000     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1324.2
036100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1324.2
036200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1324.2
036300 DECL-FAIL-EX.                                                    SQ1324.2
036400     EXIT.                                                        SQ1324.2
036500*                                                                 SQ1324.2
036600 DECL-BAIL.                                                       SQ1324.2
036700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1324.2
036800     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1324.2
036900 DECL-BAIL-WRITE.                                                 SQ1324.2
037000     MOVE    CORRECT-A TO XXCORRECT.                              SQ1324.2
037100     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1324.2
037200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1324.2
037300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1324.2
037400     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1324.2
037500     MOVE SPACES TO INF-ANSI-REFERENCE.                           SQ1324.2
037600 DECL-BAIL-EX.                                                    SQ1324.2
037700     EXIT.                                                        SQ1324.2
037800*                                                                 SQ1324.2
037900 DECL-ABNORMAL-TERM.                                              SQ1324.2
038000     MOVE SPACE TO DUMMY-RECORD.                                  SQ1324.2
038100     PERFORM DECL-WRITE-LINE.                                     SQ1324.2
038200     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ1324.2
038300             TO DUMMY-RECORD.                                     SQ1324.2
038400     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1324.2
038500*                                                                 SQ1324.2
038600 END-DECLS.                                                       SQ1324.2
038700     EXIT.                                                        SQ1324.2
038800 END DECLARATIVES.                                                SQ1324.2
038900*                                                                 SQ1324.2
039000*                                                                 SQ1324.2
039100 CCVS1 SECTION.                                                   SQ1324.2
039200 OPEN-FILES.                                                      SQ1324.2
039300     OPEN    OUTPUT PRINT-FILE.                                   SQ1324.2
039400     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1324.2
039500     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1324.2
039600     MOVE    SPACE TO TEST-RESULTS.                               SQ1324.2
039700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1324.2
039800     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1324.2
039900     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1324.2
040000     GO TO CCVS1-EXIT.                                            SQ1324.2
040100*                                                                 SQ1324.2
040200 CCVS-INIT-FILE.                                                  SQ1324.2
040300     ADD     1 TO REC-SKL-SUB.                                    SQ1324.2
040400     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1324.2
040500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1324.2
040600*                                                                 SQ1324.2
040700 CLOSE-FILES.                                                     SQ1324.2
040800     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1324.2
040900     CLOSE   PRINT-FILE.                                          SQ1324.2
041000 TERMINATE-CCVS.                                                  SQ1324.2
041100     STOP    RUN.                                                 SQ1324.2
041200*                                                                 SQ1324.2
041300 INSPT.                                                           SQ1324.2
041400     MOVE   "INSPT" TO P-OR-F.                                    SQ1324.2
041500     ADD     1 TO INSPECT-COUNTER.                                SQ1324.2
041600     PERFORM PRINT-DETAIL.                                        SQ1324.2
041700                                                                  SQ1324.2
041800 PASS.                                                            SQ1324.2
041900     MOVE   "PASS " TO P-OR-F.                                    SQ1324.2
042000     ADD     1 TO PASS-COUNTER.                                   SQ1324.2
042100     PERFORM PRINT-DETAIL.                                        SQ1324.2
042200*                                                                 SQ1324.2
042300 FAIL.                                                            SQ1324.2
042400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1324.2
042500     ADD     1 TO ERROR-COUNTER.                                  SQ1324.2
042600     PERFORM PRINT-DETAIL.                                        SQ1324.2
042700*                                                                 SQ1324.2
042800 DE-LETE.                                                         SQ1324.2
042900     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1324.2
043000     MOVE   "*****" TO P-OR-F.                                    SQ1324.2
043100     ADD     1 TO DELETE-COUNTER.                                 SQ1324.2
043200     PERFORM PRINT-DETAIL.                                        SQ1324.2
043300*                                                                 SQ1324.2
043400 PRINT-DETAIL.                                                    SQ1324.2
043500     IF REC-CT NOT EQUAL TO ZERO                                  SQ1324.2
043600         MOVE   "." TO PARDOT-X                                   SQ1324.2
043700         MOVE    REC-CT TO DOTVALUE.                              SQ1324.2
043800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1324.2
043900     PERFORM WRITE-LINE.                                          SQ1324.2
044000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1324.2
044100         PERFORM WRITE-LINE                                       SQ1324.2
044200         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1324.2
044300     ELSE                                                         SQ1324.2
044400         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1324.2
044500     MOVE    SPACE TO P-OR-F.                                     SQ1324.2
044600     MOVE    SPACE TO COMPUTED-X.                                 SQ1324.2
044700     MOVE    SPACE TO CORRECT-X.                                  SQ1324.2
044800     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1324.2
044900     MOVE    SPACE TO RE-MARK.                                    SQ1324.2
045000*                                                                 SQ1324.2
045100 HEAD-ROUTINE.                                                    SQ1324.2
045200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1324.2
045300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1324.2
045400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1324.2
045500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1324.2
045600 COLUMN-NAMES-ROUTINE.                                            SQ1324.2
045700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1324.2
045800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1324.2
045900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1324.2
046000 END-ROUTINE.                                                     SQ1324.2
046100     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1324.2
046200     PERFORM WRITE-LINE 5 TIMES.                                  SQ1324.2
046300 END-RTN-EXIT.                                                    SQ1324.2
046400     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1324.2
046500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1324.2
046600*                                                                 SQ1324.2
046700 END-ROUTINE-1.                                                   SQ1324.2
046800     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1324.2
046900     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1324.2
047000     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1324.2
047100     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1324.2
047200     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1324.2
047300     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1324.2
047400     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1324.2
047500     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1324.2
047600     PERFORM WRITE-LINE.                                          SQ1324.2
047700     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1324.2
047800     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1324.2
047900         MOVE   "NO " TO ERROR-TOTAL                              SQ1324.2
048000     ELSE                                                         SQ1324.2
048100         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1324.2
048200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1324.2
048300     PERFORM WRITE-LINE.                                          SQ1324.2
048400 END-ROUTINE-13.                                                  SQ1324.2
048500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1324.2
048600         MOVE   "NO " TO ERROR-TOTAL                              SQ1324.2
048700     ELSE                                                         SQ1324.2
048800         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1324.2
048900     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1324.2
049000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1324.2
049100     PERFORM WRITE-LINE.                                          SQ1324.2
049200     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1324.2
049300         MOVE   "NO " TO ERROR-TOTAL                              SQ1324.2
049400     ELSE                                                         SQ1324.2
049500         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1324.2
049600     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1324.2
049700     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1324.2
049800     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1324.2
049900*                                                                 SQ1324.2
050000 WRITE-LINE.                                                      SQ1324.2
050100     ADD     1 TO RECORD-COUNT.                                   SQ1324.2
050200*    IF RECORD-COUNT GREATER 50                                   SQ1324.2
050300*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1324.2
050400*        MOVE  SPACE TO DUMMY-RECORD                              SQ1324.2
050500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1324.2
050600*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1324.2
050700*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1324.2
050800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1324.2
050900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1324.2
051000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1324.2
051100     PERFORM WRT-LN.                                              SQ1324.2
051200*                                                                 SQ1324.2
051300 WRT-LN.                                                          SQ1324.2
051400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1324.2
051500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1324.2
051600 BLANK-LINE-PRINT.                                                SQ1324.2
051700     PERFORM WRT-LN.                                              SQ1324.2
051800 FAIL-ROUTINE.                                                    SQ1324.2
051900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1324.2
052000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1324.2
052100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1324.2
052200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1324.2
052300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1324.2
052400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1324.2
052500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1324.2
052600     GO TO   FAIL-ROUTINE-EX.                                     SQ1324.2
052700 FAIL-ROUTINE-WRITE.                                              SQ1324.2
052800     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1324.2
052900     PERFORM WRITE-LINE                                           SQ1324.2
053000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1324.2
053100     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1324.2
053200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1324.2
053300     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1324.2
053400 FAIL-ROUTINE-EX.                                                 SQ1324.2
053500     EXIT.                                                        SQ1324.2
053600 BAIL-OUT.                                                        SQ1324.2
053700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1324.2
053800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1324.2
053900 BAIL-OUT-WRITE.                                                  SQ1324.2
054000     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1324.2
054100     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1324.2
054200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1324.2
054300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1324.2
054400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1324.2
054500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1324.2
054600 BAIL-OUT-EX.                                                     SQ1324.2
054700     EXIT.                                                        SQ1324.2
054800 CCVS1-EXIT.                                                      SQ1324.2
054900     EXIT.                                                        SQ1324.2
055000*                                                                 SQ1324.2
055100***************************************************************   SQ1324.2
055200*                                                             *   SQ1324.2
055300*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND*   SQ1324.2
055400*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.       *   SQ1324.2
055500*                                                             *   SQ1324.2
055600***************************************************************   SQ1324.2
055700*                                                                 SQ1324.2
055800 SECT-SQ132A-0001 SECTION.                                        SQ1324.2
055900*                                                                 SQ1324.2
056000*    THIS TEST CLOSES A FILE THAT HAS NEVER BEEN OPENED.          SQ1324.2
056100*    I-O STATUS CODE 42 SHOULD BE GENERATED.                      SQ1324.2
056200*                                                                 SQ1324.2
056300 CLOSE-INIT-O1.                                                   SQ1324.2
056400     MOVE   "CLOSED UNOPENED FILE" TO FEATURE.                    SQ1324.2
056500     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1324.2
056600     MOVE   "CLOS-TEST-01" TO PAR-NAME.                           SQ1324.2
056700     MOVE   1 TO REC-CT.                                          SQ1324.2
056800*                                                                 SQ1324.2
056900 CLOSE-TEST-01.                                                   SQ1324.2
057000     IF REC-CT = 0                                                SQ1324.2
057100         OPEN INPUT SQ-FS1.                                       SQ1324.2
057200*    THIS IF STATEMENT SHOULD NEVER BE TRUE.  IT IS INCLUDED IN   SQ1324.2
057300*    AN ATTEMPT TO AVOID A COMPILER DETECTING THE CLOSE OF AN     SQ1324.2
057400*    UNOPENED FILE WITHOUT EXECUTING THE PROGRAM.  HOWEVER, IF    SQ1324.2
057500*    THE DETECTION IS MADE AT COMPILE TIME, THE TEST SHOULD BE    SQ1324.2
057600*    CONSIDERED PASSED.                                           SQ1324.2
057700*                                                                 SQ1324.2
057800     CLOSE SQ-FS1.                                                SQ1324.2
057900*                                                                 SQ1324.2
058000 CCVS-EXIT SECTION.                                               SQ1324.2
058100 CCVS-999999.                                                     SQ1324.2
058200     GO TO   CLOSE-FILES.                                         SQ1324.2
      *END-OF,SQ132A                                                                  
