      *HEADER,COBOL,SQ135A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1354.2
000200 PROGRAM-ID.                                                      SQ1354.2
000300     SQ135A.                                                      SQ1354.2
000400****************************************************************  SQ1354.2
000500*                                                              *  SQ1354.2
000600*    VALIDATION FOR:-                                          *  SQ1354.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1354.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ1354.2
000900*                                                              *  SQ1354.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ1354.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1354.2
001200*                                                              *  SQ1354.2
001300****************************************************************  SQ1354.2
001400*                                                              *  SQ1354.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1354.2
001600*                                                              *  SQ1354.2
001700*            X-14   SEQUENTIAL MASS STORAGE                    *  SQ1354.2
001800*            X-55   SYSTEM PRINTER                             *  SQ1354.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ1354.2
002000*            X-83   OBJECT-COMPUTER.                           *  SQ1354.2
002100*            X-84   LABEL RECORDS OPTION                          SQ1354.2
002200*                                                              *  SQ1354.2
002300****************************************************************  SQ1354.2
002400*                                                              *  SQ1354.2
002500*    SPLIT FROM SQ215A, THE PROGRAM REPEATS THE SEQUENCE OF    *  SQ1354.2
002600*    FILE HANDLING ROUTINES IN ORDER TO CARRY OUT THE ISOLATED *  SQ1354.2
002700*    SPLIT TEST THAT CHECKS FOR THE CORRECT RESPONSE TO CLOSE  *  SQ1354.2
002800*    AN ALREADY CLOSED FILE.  I-O STATUS 42 IS EXPECTED AND    *  SQ1354.2
002900*    TESTED IN THE DECLARATIVES.                               *  SQ1354.2
003000****************************************************************  SQ1354.2
003100*                                                                 SQ1354.2
003200 ENVIRONMENT DIVISION.                                            SQ1354.2
003300 CONFIGURATION SECTION.                                           SQ1354.2
003400 SOURCE-COMPUTER.                                                 SQ1354.2
003500     XXXXX082.                                                    SQ1354.2
003600 OBJECT-COMPUTER.                                                 SQ1354.2
003700     XXXXX083.                                                    SQ1354.2
003800*                                                                 SQ1354.2
003900 INPUT-OUTPUT SECTION.                                            SQ1354.2
004000 FILE-CONTROL.                                                    SQ1354.2
004100     SELECT PRINT-FILE ASSIGN TO                                  SQ1354.2
004200     XXXXX055.                                                    SQ1354.2
004300*                                                                 SQ1354.2
004400     SELECT SQ-FS1 ASSIGN TO                                      SQ1354.2
004500     XXXXX014                                                     SQ1354.2
004600            FILE STATUS IS SQ-FS1-STATUS.                         SQ1354.2
004700*                                                                 SQ1354.2
004800*                                                                 SQ1354.2
004900 DATA DIVISION.                                                   SQ1354.2
005000 FILE SECTION.                                                    SQ1354.2
005100 FD  PRINT-FILE                                                   SQ1354.2
005200C    LABEL RECORDS                                                SQ1354.2
005300C    XXXXX084                                                     SQ1354.2
005400C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1354.2
005500               .                                                  SQ1354.2
005600 01  PRINT-REC    PICTURE X(120).                                 SQ1354.2
005700 01  DUMMY-RECORD PICTURE X(120).                                 SQ1354.2
005800*                                                                 SQ1354.2
005900 FD  SQ-FS1                                                       SQ1354.2
006000C    LABEL RECORD IS STANDARD                                     SQ1354.2
006100                .                                                 SQ1354.2
006200 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1354.2
006300*                                                                 SQ1354.2
006400 WORKING-STORAGE SECTION.                                         SQ1354.2
006500*                                                                 SQ1354.2
006600***************************************************************   SQ1354.2
006700*                                                             *   SQ1354.2
006800*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1354.2
006900*                                                             *   SQ1354.2
007000***************************************************************   SQ1354.2
007100*                                                                 SQ1354.2
007200 01  SQ-FS1-STATUS.                                               SQ1354.2
007300   03  SQ-FS1-KEY-1   PIC X.                                      SQ1354.2
007400   03  SQ-FS1-KEY-2   PIC X.                                      SQ1354.2
007500*                                                                 SQ1354.2
007600***************************************************************   SQ1354.2
007700*                                                             *   SQ1354.2
007800*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1354.2
007900*                                                             *   SQ1354.2
008000***************************************************************   SQ1354.2
008100*                                                                 SQ1354.2
008200 01  REC-SKEL-SUB   PIC 99.                                       SQ1354.2
008300*                                                                 SQ1354.2
008400 01  FILE-RECORD-INFORMATION-REC.                                 SQ1354.2
008500     03 FILE-RECORD-INFO-SKELETON.                                SQ1354.2
008600        05 FILLER                 PICTURE X(48)       VALUE       SQ1354.2
008700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1354.2
008800        05 FILLER                 PICTURE X(46)       VALUE       SQ1354.2
008900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1354.2
009000        05 FILLER                 PICTURE X(26)       VALUE       SQ1354.2
009100             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1354.2
009200        05 FILLER                 PICTURE X(37)       VALUE       SQ1354.2
009300             ",RECKEY=                             ".             SQ1354.2
009400        05 FILLER                 PICTURE X(38)       VALUE       SQ1354.2
009500             ",ALTKEY1=                             ".            SQ1354.2
009600        05 FILLER                 PICTURE X(38)       VALUE       SQ1354.2
009700             ",ALTKEY2=                             ".            SQ1354.2
009800        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1354.2
009900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1354.2
010000        05 FILE-RECORD-INFO-P1-120.                               SQ1354.2
010100           07 FILLER              PIC X(5).                       SQ1354.2
010200           07 XFILE-NAME          PIC X(6).                       SQ1354.2
010300           07 FILLER              PIC X(8).                       SQ1354.2
010400           07 XRECORD-NAME        PIC X(6).                       SQ1354.2
010500           07 FILLER              PIC X(1).                       SQ1354.2
010600           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1354.2
010700           07 FILLER              PIC X(7).                       SQ1354.2
010800           07 XRECORD-NUMBER      PIC 9(6).                       SQ1354.2
010900           07 FILLER              PIC X(6).                       SQ1354.2
011000           07 UPDATE-NUMBER       PIC 9(2).                       SQ1354.2
011100           07 FILLER              PIC X(5).                       SQ1354.2
011200           07 ODO-NUMBER          PIC 9(4).                       SQ1354.2
011300           07 FILLER              PIC X(5).                       SQ1354.2
011400           07 XPROGRAM-NAME       PIC X(5).                       SQ1354.2
011500           07 FILLER              PIC X(7).                       SQ1354.2
011600           07 XRECORD-LENGTH      PIC 9(6).                       SQ1354.2
011700           07 FILLER              PIC X(7).                       SQ1354.2
011800           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1354.2
011900           07 FILLER              PIC X(1).                       SQ1354.2
012000           07 XBLOCK-SIZE         PIC 9(4).                       SQ1354.2
012100           07 FILLER              PIC X(6).                       SQ1354.2
012200           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1354.2
012300           07 FILLER              PIC X(5).                       SQ1354.2
012400           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1354.2
012500           07 FILLER              PIC X(6).                       SQ1354.2
012600           07 XLABEL-TYPE         PIC X(1).                       SQ1354.2
012700        05 FILE-RECORD-INFO-P121-240.                             SQ1354.2
012800           07 FILLER              PIC X(8).                       SQ1354.2
012900           07 XRECORD-KEY         PIC X(29).                      SQ1354.2
013000           07 FILLER              PIC X(9).                       SQ1354.2
013100           07 ALTERNATE-KEY1      PIC X(29).                      SQ1354.2
013200           07 FILLER              PIC X(9).                       SQ1354.2
013300           07 ALTERNATE-KEY2      PIC X(29).                      SQ1354.2
013400           07 FILLER              PIC X(7).                       SQ1354.2
013500*                                                                 SQ1354.2
013600 01  TEST-RESULTS.                                                SQ1354.2
013700     02 FILLER              PIC X      VALUE SPACE.               SQ1354.2
013800     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1354.2
013900     02 FILLER              PIC X      VALUE SPACE.               SQ1354.2
014000     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1354.2
014100     02 FILLER              PIC X      VALUE SPACE.               SQ1354.2
014200     02  PAR-NAME.                                                SQ1354.2
014300       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1354.2
014400       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1354.2
014500       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1354.2
014600     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1354.2
014700     02 RE-MARK             PIC X(61).                            SQ1354.2
014800 01  TEST-COMPUTED.                                               SQ1354.2
014900   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1354.2
015000   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1354.2
015100   02 COMPUTED-X.                                                 SQ1354.2
015200     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1354.2
015300     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1354.2
015400     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1354.2
015500     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1354.2
015600     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1354.2
015700     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1354.2
015800        04 COMPUTED-18V0                   PIC -9(18).            SQ1354.2
015900        04 FILLER                          PIC X.                 SQ1354.2
016000     03 FILLER PIC X(50) VALUE SPACE.                             SQ1354.2
016100 01  TEST-CORRECT.                                                SQ1354.2
016200     02 FILLER PIC X(30) VALUE SPACE.                             SQ1354.2
016300     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1354.2
016400     02 CORRECT-X.                                                SQ1354.2
016500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1354.2
016600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1354.2
016700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1354.2
016800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1354.2
016900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1354.2
017000     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1354.2
017100         04 CORRECT-18V0                     PIC -9(18).          SQ1354.2
017200         04 FILLER                           PIC X.               SQ1354.2
017300     03 FILLER PIC X(2) VALUE SPACE.                              SQ1354.2
017400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1354.2
017500 01  CCVS-C-1.                                                    SQ1354.2
017600     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ1354.2
017700     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ1354.2
017800-    "SS  PARAGRAPH-NAME                                          SQ1354.2
017900-    "       REMARKS".                                            SQ1354.2
018000     02 FILLER           PIC X(17)    VALUE SPACE.                SQ1354.2
018100 01  CCVS-C-2.                                                    SQ1354.2
018200     02 FILLER           PIC XXXX     VALUE SPACE.                SQ1354.2
018300     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ1354.2
018400     02 FILLER           PIC X(16)    VALUE SPACE.                SQ1354.2
018500     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ1354.2
018600     02 FILLER           PIC X(90)    VALUE SPACE.                SQ1354.2
018700 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1354.2
018800 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1354.2
018900 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1354.2
019000 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1354.2
019100 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1354.2
019200 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1354.2
019300 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1354.2
019400 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1354.2
019500 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1354.2
019600 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1354.2
019700 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1354.2
019800 01  CCVS-H-1.                                                    SQ1354.2
019900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1354.2
020000     02  FILLER          PIC X(42)    VALUE                       SQ1354.2
020100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1354.2
020200     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1354.2
020300 01  CCVS-H-2A.                                                   SQ1354.2
020400   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1354.2
020500   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1354.2
020600   02  FILLER            PIC XXXX   VALUE                         SQ1354.2
020700     "4.2 ".                                                      SQ1354.2
020800   02  FILLER            PIC X(28)  VALUE                         SQ1354.2
020900            " COPY - NOT FOR DISTRIBUTION".                       SQ1354.2
021000   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1354.2
021100*                                                                 SQ1354.2
021200 01  CCVS-H-2B.                                                   SQ1354.2
021300   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1354.2
021400   02  TEST-ID           PIC X(9).                                SQ1354.2
021500   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1354.2
021600   02  FILLER            PIC X(12)  VALUE                         SQ1354.2
021700     " HIGH       ".                                              SQ1354.2
021800   02  FILLER            PIC X(22)  VALUE                         SQ1354.2
021900            " LEVEL VALIDATION FOR ".                             SQ1354.2
022000   02  FILLER            PIC X(58)  VALUE                         SQ1354.2
022100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1354.2
022200 01  CCVS-H-3.                                                    SQ1354.2
022300     02  FILLER          PIC X(34)  VALUE                         SQ1354.2
022400            " FOR OFFICIAL USE ONLY    ".                         SQ1354.2
022500     02  FILLER          PIC X(58)  VALUE                         SQ1354.2
022600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1354.2
022700     02  FILLER          PIC X(28)  VALUE                         SQ1354.2
022800            "  COPYRIGHT   1985,1986 ".                           SQ1354.2
022900 01  CCVS-E-1.                                                    SQ1354.2
023000     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1354.2
023100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1354.2
023200     02 ID-AGAIN         PIC X(9).                                SQ1354.2
023300     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1354.2
023400 01  CCVS-E-2.                                                    SQ1354.2
023500     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1354.2
023600     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1354.2
023700     02  CCVS-E-2-2.                                              SQ1354.2
023800         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1354.2
023900         03 FILLER         PIC X      VALUE SPACE.                SQ1354.2
024000         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1354.2
024100            "ERRORS ENCOUNTERED".                                 SQ1354.2
024200 01  CCVS-E-3.                                                    SQ1354.2
024300     02  FILLER          PIC X(22)  VALUE                         SQ1354.2
024400            " FOR OFFICIAL USE ONLY".                             SQ1354.2
024500     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1354.2
024600     02  FILLER          PIC X(58)  VALUE                         SQ1354.2
024700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1354.2
024800     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1354.2
024900     02  FILLER          PIC X(20)  VALUE                         SQ1354.2
025000             " COPYRIGHT 1985,1986".                              SQ1354.2
025100 01  CCVS-E-4.                                                    SQ1354.2
025200     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1354.2
025300     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1354.2
025400     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1354.2
025500     02 FILLER           PIC X(40)  VALUE                         SQ1354.2
025600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1354.2
025700 01  XXINFO.                                                      SQ1354.2
025800     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1354.2
025900     02 INFO-TEXT.                                                SQ1354.2
026000       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1354.2
026100       04 XXCOMPUTED         PIC X(20).                           SQ1354.2
026200       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1354.2
026300       04 XXCORRECT          PIC X(20).                           SQ1354.2
026400     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1354.2
026500 01  HYPHEN-LINE.                                                 SQ1354.2
026600     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1354.2
026700     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1354.2
026800-    "*****************************************".                 SQ1354.2
026900     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1354.2
027000-    "******************************".                            SQ1354.2
027100 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1354.2
027200     "SQ135A".                                                    SQ1354.2
027300*                                                                 SQ1354.2
027400 PROCEDURE DIVISION.                                              SQ1354.2
027500 DECLARATIVES.                                                    SQ1354.2
027600 SQ-FS1-DECLARATIVE SECTION.                                      SQ1354.2
027700     USE AFTER STANDARD EXCEPTION PROCEDURE ON SQ-FS1.            SQ1354.2
027800 OUTPUT-ERROR-PROCESS.                                            SQ1354.2
027900     IF SQ-FS1-STATUS = "42"                                      SQ1354.2
028000             PERFORM PASS-DECL                                    SQ1354.2
028100             GO TO ABNORMAL-TERM-DECL                             SQ1354.2
028200     ELSE                                                         SQ1354.2
028300             MOVE "42" TO CORRECT-A                               SQ1354.2
028400             MOVE SQ-FS1-STATUS TO COMPUTED-A                     SQ1354.2
028500             MOVE "STATUS AFTER OPEN OF A CLOSED FILE INCORRECT"  SQ1354.2
028600                     TO RE-MARK                                   SQ1354.2
028700             MOVE "VII-3, FILE STATUS" TO ANSI-REFERENCE          SQ1354.2
028800             PERFORM FAIL-DECL                                    SQ1354.2
028900             GO TO ABNORMAL-TERM-DECL                             SQ1354.2
029000     END-IF.                                                      SQ1354.2
029100*                                                                 SQ1354.2
029200 PASS-DECL.                                                       SQ1354.2
029300     MOVE   "PASS " TO P-OR-F.                                    SQ1354.2
029400     ADD     1 TO PASS-COUNTER.                                   SQ1354.2
029500     PERFORM PRINT-DETAIL-DECL.                                   SQ1354.2
029600*                                                                 SQ1354.2
029700 FAIL-DECL.                                                       SQ1354.2
029800     MOVE   "FAIL*" TO P-OR-F.                                    SQ1354.2
029900     ADD     1 TO ERROR-COUNTER.                                  SQ1354.2
030000     PERFORM PRINT-DETAIL-DECL.                                   SQ1354.2
030100*                                                                 SQ1354.2
030200 PRINT-DETAIL-DECL.                                               SQ1354.2
030300     IF REC-CT NOT EQUAL TO ZERO                                  SQ1354.2
030400         MOVE   "." TO PARDOT-X                                   SQ1354.2
030500         MOVE    REC-CT TO DOTVALUE.                              SQ1354.2
030600     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1354.2
030700     PERFORM WRITE-LINE-DECL.                                     SQ1354.2
030800     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1354.2
030900         PERFORM WRITE-LINE-DECL                                  SQ1354.2
031000         PERFORM FAIL-ROUTINE-DECL THRU FAIL-ROUTINE-EX-DECL      SQ1354.2
031100     ELSE                                                         SQ1354.2
031200         PERFORM BAIL-OUT-DECL THRU BAIL-OUT-EX-DECL.             SQ1354.2
031300     MOVE    SPACE TO P-OR-F.                                     SQ1354.2
031400     MOVE    SPACE TO COMPUTED-X.                                 SQ1354.2
031500     MOVE    SPACE TO CORRECT-X.                                  SQ1354.2
031600     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1354.2
031700     MOVE    SPACE TO RE-MARK.                                    SQ1354.2
031800*                                                                 SQ1354.2
031900 WRITE-LINE-DECL.                                                 SQ1354.2
032000     ADD     1 TO RECORD-COUNT.                                   SQ1354.2
032100*    IF RECORD-COUNT GREATER 50                                   SQ1354.2
032200*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1354.2
032300*        MOVE  SPACE TO DUMMY-RECORD                              SQ1354.2
032400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1354.2
032500*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-DECL       SQ1354.2
032600*        MOVE  CCVS-C-2 TO DUMMY-RECORD                           SQ1354.2
032700*        PERFORM WRT-LN-DECL 2 TIMES                              SQ1354.2
032800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-DECL    SQ1354.2
032900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1354.2
033000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1354.2
033100     PERFORM WRT-LN-DECL.                                         SQ1354.2
033200*                                                                 SQ1354.2
033300 WRT-LN-DECL.                                                     SQ1354.2
033400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1354.2
033500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1354.2
033600 BLANK-LINE-PRINT-DECL.                                           SQ1354.2
033700     PERFORM WRT-LN-DECL.                                         SQ1354.2
033800 FAIL-ROUTINE-DECL.                                               SQ1354.2
033900     IF COMPUTED-X NOT EQUAL TO SPACE                             SQ1354.2
034000             GO TO FAIL-ROUTINE-WRITE-DECL.                       SQ1354.2
034100     IF CORRECT-X NOT EQUAL TO SPACE                              SQ1354.2
034200             GO TO FAIL-ROUTINE-WRITE-DECL.                       SQ1354.2
034300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1354.2
034400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1354.2
034500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1354.2
034600     PERFORM WRITE-LINE-DECL 2 TIMES.                             SQ1354.2
034700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1354.2
034800     GO TO   FAIL-ROUTINE-EX-DECL.                                SQ1354.2
034900 FAIL-ROUTINE-WRITE-DECL.                                         SQ1354.2
035000     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1354.2
035100     PERFORM WRITE-LINE-DECL                                      SQ1354.2
035200     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1354.2
035300     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1354.2
035400     PERFORM WRITE-LINE-DECL 2 TIMES.                             SQ1354.2
035500     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1354.2
035600 FAIL-ROUTINE-EX-DECL.                                            SQ1354.2
035700     EXIT.                                                        SQ1354.2
035800 BAIL-OUT-DECL.                                                   SQ1354.2
035900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-DECL.  SQ1354.2
036000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-DECL.          SQ1354.2
036100 BAIL-OUT-WRITE-DECL.                                             SQ1354.2
036200     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1354.2
036300     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1354.2
036400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1354.2
036500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1354.2
036600     PERFORM WRITE-LINE-DECL 2 TIMES.                             SQ1354.2
036700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1354.2
036800 BAIL-OUT-EX-DECL.                                                SQ1354.2
036900     EXIT.                                                        SQ1354.2
037000*                                                                 SQ1354.2
037100 ABNORMAL-TERM-DECL.                                              SQ1354.2
037200     MOVE    SPACE TO DUMMY-RECORD.                               SQ1354.2
037300     PERFORM WRITE-LINE-DECL.                                     SQ1354.2
037400     MOVE    "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"   SQ1354.2
037500                     TO DUMMY-RECORD.                             SQ1354.2
037600     PERFORM WRITE-LINE-DECL 3 TIMES.                             SQ1354.2
037700*                                                                 SQ1354.2
037800 EXIT-DECL.                                                       SQ1354.2
037900     EXIT.                                                        SQ1354.2
038000 END DECLARATIVES.                                                SQ1354.2
038100*                                                                 SQ1354.2
038200 CCVS1 SECTION.                                                   SQ1354.2
038300 OPEN-FILES.                                                      SQ1354.2
038400     OPEN    OUTPUT PRINT-FILE.                                   SQ1354.2
038500     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1354.2
038600     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1354.2
038700     MOVE    SPACE TO TEST-RESULTS.                               SQ1354.2
038800     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1354.2
038900     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1354.2
039000     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1354.2
039100     GO TO CCVS1-EXIT.                                            SQ1354.2
039200*                                                                 SQ1354.2
039300 CCVS-INIT-FILE.                                                  SQ1354.2
039400     ADD     1 TO REC-SKL-SUB.                                    SQ1354.2
039500     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1354.2
039600                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1354.2
039700*                                                                 SQ1354.2
039800 CLOSE-FILES.                                                     SQ1354.2
039900     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1354.2
040000     CLOSE   PRINT-FILE.                                          SQ1354.2
040100 TERMINATE-CCVS.                                                  SQ1354.2
040200*    EXIT    PROGRAM.                                             SQ1354.2
040300     STOP    RUN.                                                 SQ1354.2
040400*                                                                 SQ1354.2
040500 INSPT.                                                           SQ1354.2
040600     MOVE   "INSPT" TO P-OR-F.                                    SQ1354.2
040700     ADD     1 TO INSPECT-COUNTER.                                SQ1354.2
040800     PERFORM PRINT-DETAIL.                                        SQ1354.2
040900                                                                  SQ1354.2
041000 PASS.                                                            SQ1354.2
041100     MOVE   "PASS " TO P-OR-F.                                    SQ1354.2
041200     ADD     1 TO PASS-COUNTER.                                   SQ1354.2
041300     PERFORM PRINT-DETAIL.                                        SQ1354.2
041400*                                                                 SQ1354.2
041500 FAIL.                                                            SQ1354.2
041600     MOVE   "FAIL*" TO P-OR-F.                                    SQ1354.2
041700     ADD     1 TO ERROR-COUNTER.                                  SQ1354.2
041800     PERFORM PRINT-DETAIL.                                        SQ1354.2
041900*                                                                 SQ1354.2
042000 DE-LETE.                                                         SQ1354.2
042100     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1354.2
042200     MOVE   "*****" TO P-OR-F.                                    SQ1354.2
042300     ADD     1 TO DELETE-COUNTER.                                 SQ1354.2
042400     PERFORM PRINT-DETAIL.                                        SQ1354.2
042500*                                                                 SQ1354.2
042600 PRINT-DETAIL.                                                    SQ1354.2
042700     IF REC-CT NOT EQUAL TO ZERO                                  SQ1354.2
042800         MOVE   "." TO PARDOT-X                                   SQ1354.2
042900         MOVE    REC-CT TO DOTVALUE.                              SQ1354.2
043000     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1354.2
043100     PERFORM WRITE-LINE.                                          SQ1354.2
043200     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1354.2
043300         PERFORM WRITE-LINE                                       SQ1354.2
043400         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1354.2
043500     ELSE                                                         SQ1354.2
043600         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1354.2
043700     MOVE    SPACE TO P-OR-F.                                     SQ1354.2
043800     MOVE    SPACE TO COMPUTED-X.                                 SQ1354.2
043900     MOVE    SPACE TO CORRECT-X.                                  SQ1354.2
044000     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1354.2
044100     MOVE    SPACE TO RE-MARK.                                    SQ1354.2
044200*                                                                 SQ1354.2
044300 HEAD-ROUTINE.                                                    SQ1354.2
044400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1354.2
044500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1354.2
044600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1354.2
044700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1354.2
044800 COLUMN-NAMES-ROUTINE.                                            SQ1354.2
044900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1354.2
045000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1354.2
045100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1354.2
045200 END-ROUTINE.                                                     SQ1354.2
045300     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1354.2
045400     PERFORM WRITE-LINE 5 TIMES.                                  SQ1354.2
045500 END-RTN-EXIT.                                                    SQ1354.2
045600     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1354.2
045700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1354.2
045800*                                                                 SQ1354.2
045900 END-ROUTINE-1.                                                   SQ1354.2
046000     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1354.2
046100     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1354.2
046200     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1354.2
046300     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1354.2
046400     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1354.2
046500     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1354.2
046600     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1354.2
046700     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1354.2
046800     PERFORM WRITE-LINE.                                          SQ1354.2
046900     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1354.2
047000     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1354.2
047100         MOVE   "NO " TO ERROR-TOTAL                              SQ1354.2
047200     ELSE                                                         SQ1354.2
047300         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1354.2
047400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1354.2
047500     PERFORM WRITE-LINE.                                          SQ1354.2
047600 END-ROUTINE-13.                                                  SQ1354.2
047700     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1354.2
047800         MOVE   "NO " TO ERROR-TOTAL                              SQ1354.2
047900     ELSE                                                         SQ1354.2
048000         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1354.2
048100     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1354.2
048200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1354.2
048300     PERFORM WRITE-LINE.                                          SQ1354.2
048400     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1354.2
048500         MOVE   "NO " TO ERROR-TOTAL                              SQ1354.2
048600     ELSE                                                         SQ1354.2
048700         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1354.2
048800     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1354.2
048900     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1354.2
049000     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1354.2
049100*                                                                 SQ1354.2
049200 WRITE-LINE.                                                      SQ1354.2
049300     ADD     1 TO RECORD-COUNT.                                   SQ1354.2
049400*    IF RECORD-COUNT GREATER 50                                   SQ1354.2
049500*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1354.2
049600*        MOVE  SPACE TO DUMMY-RECORD                              SQ1354.2
049700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1354.2
049800*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1354.2
049900*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1354.2
050000*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1354.2
050100*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1354.2
050200*        MOVE  ZERO TO RECORD-COUNT.                              SQ1354.2
050300     PERFORM WRT-LN.                                              SQ1354.2
050400*                                                                 SQ1354.2
050500 WRT-LN.                                                          SQ1354.2
050600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1354.2
050700     MOVE    SPACE TO DUMMY-RECORD.                               SQ1354.2
050800 BLANK-LINE-PRINT.                                                SQ1354.2
050900     PERFORM WRT-LN.                                              SQ1354.2
051000 FAIL-ROUTINE.                                                    SQ1354.2
051100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1354.2
051200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1354.2
051300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1354.2
051400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1354.2
051500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1354.2
051600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1354.2
051700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1354.2
051800     GO TO   FAIL-ROUTINE-EX.                                     SQ1354.2
051900 FAIL-ROUTINE-WRITE.                                              SQ1354.2
052000     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1354.2
052100     PERFORM WRITE-LINE                                           SQ1354.2
052200     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1354.2
052300     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1354.2
052400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1354.2
052500     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1354.2
052600 FAIL-ROUTINE-EX.                                                 SQ1354.2
052700     EXIT.                                                        SQ1354.2
052800 BAIL-OUT.                                                        SQ1354.2
052900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1354.2
053000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1354.2
053100 BAIL-OUT-WRITE.                                                  SQ1354.2
053200     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1354.2
053300     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1354.2
053400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1354.2
053500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1354.2
053600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1354.2
053700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1354.2
053800 BAIL-OUT-EX.                                                     SQ1354.2
053900     EXIT.                                                        SQ1354.2
054000 CCVS1-EXIT.                                                      SQ1354.2
054100     EXIT.                                                        SQ1354.2
054200*                                                                 SQ1354.2
054300****************************************************************  SQ1354.2
054400*                                                              *  SQ1354.2
054500*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1354.2
054600*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1354.2
054700*                                                              *  SQ1354.2
054800****************************************************************  SQ1354.2
054900*                                                                 SQ1354.2
055000 SECT-SQ135A-0001 SECTION.                                        SQ1354.2
055100 WRITE-INIT-GF-01.                                                SQ1354.2
055200*                                                                 SQ1354.2
055300*        THIS TEST CREATES FILE SQ-FS1 AND CLOSES IT.             SQ1354.2
055400*        FIRST IT SETS UP A SKELETON RECORD IN WORKING STORAGE.   SQ1354.2
055500*                                                                 SQ1354.2
055600     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1354.2
055700     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1354.2
055800     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1354.2
055900     MOVE 120      TO XRECORD-LENGTH (1).                         SQ1354.2
056000     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       SQ1354.2
056100     MOVE 1        TO XBLOCK-SIZE (1).                            SQ1354.2
056200     MOVE 1        TO RECORDS-IN-FILE (1).                        SQ1354.2
056300     MOVE "SQ"     TO XFILE-ORGANIZATION (1).                     SQ1354.2
056400     MOVE "S"      TO XLABEL-TYPE (1).                            SQ1354.2
056500     MOVE 1        TO XRECORD-NUMBER (1).                         SQ1354.2
056600*                                                                 SQ1354.2
056700 WRITE-OPEN-01.                                                   SQ1354.2
056800     OPEN    OUTPUT SQ-FS1.                                       SQ1354.2
056900*                                                                 SQ1354.2
057000*        WRITE A SINGLE RECORD TO THE FILE                        SQ1354.2
057100*                                                                 SQ1354.2
057200 WRITE-INIT-01.                                                   SQ1354.2
057300 WRITE-TEST-01-01.                                                SQ1354.2
057400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.     SQ1354.2
057500     WRITE   SQ-FS1R1-F-G-120.                                    SQ1354.2
057600*                                                                 SQ1354.2
057700 CLOSE-INIT-01.                                                   SQ1354.2
057800 CLOSE-TEST-01.                                                   SQ1354.2
057900     CLOSE   SQ-FS1.                                              SQ1354.2
058000*                                                                 SQ1354.2
058100*        HAVING CLOSED THE FILE, WE NOW TRY TO CLOSE IT AGAIN.    SQ1354.2
058200*        THE TEST PASSES IF THE FILE CANNOT BE RECLOSED AND       SQ1354.2
058300*        THE APPROPRIATE I-O STATUS VALUE IS RETURNED.            SQ1354.2
058400*        AN IMPLEMENTATION MAY TERMINATE EXECUTION OF A           SQ1354.2
058500*        PROGRAM WHICH ATTEMPTS TO RECLOSE AN ALREADY CLOSED FILE.SQ1354.2
058600 CLOSE-INIT-02.                                                   SQ1354.2
058700*                                                                 SQ1354.2
058800     MOVE    1 TO REC-CT.                                         SQ1354.2
058900     MOVE   "CLOSE-TEST-02" TO PAR-NAME                           SQ1354.2
059000     MOVE   "CLOSE OF CLOSED FILE" TO FEATURE.                    SQ1354.2
059100 CLOSE-TEST-02.                                                   SQ1354.2
059200     CLOSE   SQ-FS1.                                              SQ1354.2
059300*                                                                 SQ1354.2
059400 CCVS-EXIT SECTION.                                               SQ1354.2
059500 CCVS-999999.                                                     SQ1354.2
059600     GO TO CLOSE-FILES.                                           SQ1354.2
      *END-OF,SQ135A                                                                  
