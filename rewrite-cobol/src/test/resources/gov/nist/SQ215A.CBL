      *HEADER,COBOL,SQ215A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2154.2
000200 PROGRAM-ID.                                                      SQ2154.2
000300     SQ215A.                                                      SQ2154.2
000400****************************************************************  SQ2154.2
000500*                                                              *  SQ2154.2
000600*    VALIDATION FOR:-                                          *  SQ2154.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2154.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ2154.2
000900*                                                              *  SQ2154.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ2154.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2154.2
001200*                                                              *  SQ2154.2
001300****************************************************************  SQ2154.2
001400*                                                              *  SQ2154.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2154.2
001600*                                                              *  SQ2154.2
001700*            X-14   SEQUENTIAL MASS STORAGE                    *  SQ2154.2
001800*            X-55   SYSTEM PRINTER                             *  SQ2154.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ2154.2
002000*            X-83   OBJECT-COMPUTER                            *  SQ2154.2
002100*            X-84   LABEL RECORDS OPTION                       *  SQ2154.2
002200*                                                              *  SQ2154.2
002300****************************************************************  SQ2154.2
002400*                                                              *  SQ2154.2
002500*    SQ215A TESTS THE CLOSE STATEMENT WITH THE WITH LOCK PHRASE*  SQ2154.2
002600*    A MASS STORAGE FILE IS CREATED, ONE RECORD IS WRITTEN     *  SQ2154.2
002700*    TO IT, AND IT IS CLOSED WITH LOCK.  AN ATTEMPT IS THEN    *  SQ2154.2
002800*    MADE TO REOPEN THE FILE.  I-O STATUS 38 IS EXPECTED AND   *  SQ2154.2
002900*    TESTED IN THE DECLARATIVES.                               *  SQ2154.2
003000*                                                              *  SQ2154.2
003100*    THIS PROGRAM HAS BEEN SPLIT FROM V2.0 ONWARDS.            *  SQ2154.2
003200*    THE NEW PROGRAMS ARE SQ229A AND SQ230A.                   *  SQ2154.2
003300****************************************************************  SQ2154.2
003400*                                                              *  SQ2154.2
003500*                                                                 SQ2154.2
003600 ENVIRONMENT DIVISION.                                            SQ2154.2
003700 CONFIGURATION SECTION.                                           SQ2154.2
003800 SOURCE-COMPUTER.                                                 SQ2154.2
003900     XXXXX082.                                                    SQ2154.2
004000 OBJECT-COMPUTER.                                                 SQ2154.2
004100     XXXXX083.                                                    SQ2154.2
004200*                                                                 SQ2154.2
004300 INPUT-OUTPUT SECTION.                                            SQ2154.2
004400 FILE-CONTROL.                                                    SQ2154.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SQ2154.2
004600     XXXXX055.                                                    SQ2154.2
004700     SELECT SQ-FS1 ASSIGN TO                                      SQ2154.2
004800     XXXXX014                                                     SQ2154.2
004900            FILE STATUS IS SQ-FS1-STATUS.                         SQ2154.2
005000*                                                                 SQ2154.2
005100*                                                                 SQ2154.2
005200 DATA DIVISION.                                                   SQ2154.2
005300 FILE SECTION.                                                    SQ2154.2
005400 FD  PRINT-FILE                                                   SQ2154.2
005500C    LABEL RECORDS                                                SQ2154.2
005600C    XXXXX084                                                     SQ2154.2
005700C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2154.2
005800               .                                                  SQ2154.2
005900 01  PRINT-REC    PICTURE X(120).                                 SQ2154.2
006000 01  DUMMY-RECORD PICTURE X(120).                                 SQ2154.2
006100*                                                                 SQ2154.2
006200 FD  SQ-FS1                                                       SQ2154.2
006300C    LABEL RECORD IS STANDARD                                     SQ2154.2
006400                .                                                 SQ2154.2
006500 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2154.2
006600*                                                                 SQ2154.2
006700 WORKING-STORAGE SECTION.                                         SQ2154.2
006800*                                                                 SQ2154.2
006900***************************************************************   SQ2154.2
007000*                                                             *   SQ2154.2
007100*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2154.2
007200*                                                             *   SQ2154.2
007300***************************************************************   SQ2154.2
007400*                                                                 SQ2154.2
007500 01  SQ-FS1-STATUS.                                               SQ2154.2
007600   03  SQ-FS1-KEY-1   PIC X.                                      SQ2154.2
007700   03  SQ-FS1-KEY-2   PIC X.                                      SQ2154.2
007800*                                                                 SQ2154.2
007900***************************************************************   SQ2154.2
008000*                                                             *   SQ2154.2
008100*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2154.2
008200*                                                             *   SQ2154.2
008300***************************************************************   SQ2154.2
008400*                                                                 SQ2154.2
008500 01  REC-SKEL-SUB   PIC 99.                                       SQ2154.2
008600*                                                                 SQ2154.2
008700 01  FILE-RECORD-INFORMATION-REC.                                 SQ2154.2
008800     03 FILE-RECORD-INFO-SKELETON.                                SQ2154.2
008900        05 FILLER                 PICTURE X(48)       VALUE       SQ2154.2
009000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2154.2
009100        05 FILLER                 PICTURE X(46)       VALUE       SQ2154.2
009200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2154.2
009300        05 FILLER                 PICTURE X(26)       VALUE       SQ2154.2
009400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2154.2
009500        05 FILLER                 PICTURE X(37)       VALUE       SQ2154.2
009600             ",RECKEY=                             ".             SQ2154.2
009700        05 FILLER                 PICTURE X(38)       VALUE       SQ2154.2
009800             ",ALTKEY1=                             ".            SQ2154.2
009900        05 FILLER                 PICTURE X(38)       VALUE       SQ2154.2
010000             ",ALTKEY2=                             ".            SQ2154.2
010100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2154.2
010200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2154.2
010300        05 FILE-RECORD-INFO-P1-120.                               SQ2154.2
010400           07 FILLER              PIC X(5).                       SQ2154.2
010500           07 XFILE-NAME          PIC X(6).                       SQ2154.2
010600           07 FILLER              PIC X(8).                       SQ2154.2
010700           07 XRECORD-NAME        PIC X(6).                       SQ2154.2
010800           07 FILLER              PIC X(1).                       SQ2154.2
010900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2154.2
011000           07 FILLER              PIC X(7).                       SQ2154.2
011100           07 XRECORD-NUMBER      PIC 9(6).                       SQ2154.2
011200           07 FILLER              PIC X(6).                       SQ2154.2
011300           07 UPDATE-NUMBER       PIC 9(2).                       SQ2154.2
011400           07 FILLER              PIC X(5).                       SQ2154.2
011500           07 ODO-NUMBER          PIC 9(4).                       SQ2154.2
011600           07 FILLER              PIC X(5).                       SQ2154.2
011700           07 XPROGRAM-NAME       PIC X(5).                       SQ2154.2
011800           07 FILLER              PIC X(7).                       SQ2154.2
011900           07 XRECORD-LENGTH      PIC 9(6).                       SQ2154.2
012000           07 FILLER              PIC X(7).                       SQ2154.2
012100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2154.2
012200           07 FILLER              PIC X(1).                       SQ2154.2
012300           07 XBLOCK-SIZE         PIC 9(4).                       SQ2154.2
012400           07 FILLER              PIC X(6).                       SQ2154.2
012500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2154.2
012600           07 FILLER              PIC X(5).                       SQ2154.2
012700           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2154.2
012800           07 FILLER              PIC X(6).                       SQ2154.2
012900           07 XLABEL-TYPE         PIC X(1).                       SQ2154.2
013000        05 FILE-RECORD-INFO-P121-240.                             SQ2154.2
013100           07 FILLER              PIC X(8).                       SQ2154.2
013200           07 XRECORD-KEY         PIC X(29).                      SQ2154.2
013300           07 FILLER              PIC X(9).                       SQ2154.2
013400           07 ALTERNATE-KEY1      PIC X(29).                      SQ2154.2
013500           07 FILLER              PIC X(9).                       SQ2154.2
013600           07 ALTERNATE-KEY2      PIC X(29).                      SQ2154.2
013700           07 FILLER              PIC X(7).                       SQ2154.2
013800*                                                                 SQ2154.2
013900 01  TEST-RESULTS.                                                SQ2154.2
014000     02 FILLER              PIC X      VALUE SPACE.               SQ2154.2
014100     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2154.2
014200     02 FILLER              PIC X      VALUE SPACE.               SQ2154.2
014300     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2154.2
014400     02 FILLER              PIC X      VALUE SPACE.               SQ2154.2
014500     02  PAR-NAME.                                                SQ2154.2
014600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2154.2
014700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2154.2
014800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2154.2
014900     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2154.2
015000     02 RE-MARK             PIC X(61).                            SQ2154.2
015100 01  TEST-COMPUTED.                                               SQ2154.2
015200   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2154.2
015300   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2154.2
015400   02 COMPUTED-X.                                                 SQ2154.2
015500     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2154.2
015600     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2154.2
015700     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2154.2
015800     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2154.2
015900     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2154.2
016000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2154.2
016100        04 COMPUTED-18V0                   PIC -9(18).            SQ2154.2
016200        04 FILLER                          PIC X.                 SQ2154.2
016300     03 FILLER PIC X(50) VALUE SPACE.                             SQ2154.2
016400 01  TEST-CORRECT.                                                SQ2154.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             SQ2154.2
016600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2154.2
016700     02 CORRECT-X.                                                SQ2154.2
016800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2154.2
016900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2154.2
017000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2154.2
017100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2154.2
017200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2154.2
017300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2154.2
017400         04 CORRECT-18V0                     PIC -9(18).          SQ2154.2
017500         04 FILLER                           PIC X.               SQ2154.2
017600     03 FILLER PIC X(2) VALUE SPACE.                              SQ2154.2
017700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2154.2
017800 01  CCVS-C-1.                                                    SQ2154.2
017900     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ2154.2
018000     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ2154.2
018100-    "SS  PARAGRAPH-NAME                                          SQ2154.2
018200-    "       REMARKS".                                            SQ2154.2
018300     02 FILLER           PIC X(17)    VALUE SPACE.                SQ2154.2
018400 01  CCVS-C-2.                                                    SQ2154.2
018500     02 FILLER           PIC XXXX     VALUE SPACE.                SQ2154.2
018600     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ2154.2
018700     02 FILLER           PIC X(16)    VALUE SPACE.                SQ2154.2
018800     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ2154.2
018900     02 FILLER           PIC X(90)    VALUE SPACE.                SQ2154.2
019000 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2154.2
019100 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2154.2
019200 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2154.2
019300 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2154.2
019400 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2154.2
019500 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2154.2
019600 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2154.2
019700 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2154.2
019800 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2154.2
019900 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2154.2
020000 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2154.2
020100 01  CCVS-H-1.                                                    SQ2154.2
020200     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2154.2
020300     02  FILLER          PIC X(42)    VALUE                       SQ2154.2
020400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2154.2
020500     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2154.2
020600 01  CCVS-H-2A.                                                   SQ2154.2
020700   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2154.2
020800   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2154.2
020900   02  FILLER            PIC XXXX   VALUE                         SQ2154.2
021000     "4.2 ".                                                      SQ2154.2
021100   02  FILLER            PIC X(28)  VALUE                         SQ2154.2
021200            " COPY - NOT FOR DISTRIBUTION".                       SQ2154.2
021300   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2154.2
021400*                                                                 SQ2154.2
021500 01  CCVS-H-2B.                                                   SQ2154.2
021600   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2154.2
021700   02  TEST-ID           PIC X(9).                                SQ2154.2
021800   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2154.2
021900   02  FILLER            PIC X(12)  VALUE                         SQ2154.2
022000     " HIGH       ".                                              SQ2154.2
022100   02  FILLER            PIC X(22)  VALUE                         SQ2154.2
022200            " LEVEL VALIDATION FOR ".                             SQ2154.2
022300   02  FILLER            PIC X(58)  VALUE                         SQ2154.2
022400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2154.2
022500 01  CCVS-H-3.                                                    SQ2154.2
022600     02  FILLER          PIC X(34)  VALUE                         SQ2154.2
022700            " FOR OFFICIAL USE ONLY    ".                         SQ2154.2
022800     02  FILLER          PIC X(58)  VALUE                         SQ2154.2
022900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2154.2
023000     02  FILLER          PIC X(28)  VALUE                         SQ2154.2
023100            "  COPYRIGHT   1985,1986 ".                           SQ2154.2
023200 01  CCVS-E-1.                                                    SQ2154.2
023300     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2154.2
023400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2154.2
023500     02 ID-AGAIN         PIC X(9).                                SQ2154.2
023600     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2154.2
023700 01  CCVS-E-2.                                                    SQ2154.2
023800     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2154.2
023900     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2154.2
024000     02  CCVS-E-2-2.                                              SQ2154.2
024100         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2154.2
024200         03 FILLER         PIC X      VALUE SPACE.                SQ2154.2
024300         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2154.2
024400            "ERRORS ENCOUNTERED".                                 SQ2154.2
024500 01  CCVS-E-3.                                                    SQ2154.2
024600     02  FILLER          PIC X(22)  VALUE                         SQ2154.2
024700            " FOR OFFICIAL USE ONLY".                             SQ2154.2
024800     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2154.2
024900     02  FILLER          PIC X(58)  VALUE                         SQ2154.2
025000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2154.2
025100     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2154.2
025200     02  FILLER          PIC X(20)  VALUE                         SQ2154.2
025300             " COPYRIGHT 1985,1986".                              SQ2154.2
025400 01  CCVS-E-4.                                                    SQ2154.2
025500     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2154.2
025600     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2154.2
025700     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2154.2
025800     02 FILLER           PIC X(40)  VALUE                         SQ2154.2
025900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2154.2
026000 01  XXINFO.                                                      SQ2154.2
026100     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2154.2
026200     02 INFO-TEXT.                                                SQ2154.2
026300       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2154.2
026400       04 XXCOMPUTED         PIC X(20).                           SQ2154.2
026500       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2154.2
026600       04 XXCORRECT          PIC X(20).                           SQ2154.2
026700     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2154.2
026800 01  HYPHEN-LINE.                                                 SQ2154.2
026900     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2154.2
027000     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2154.2
027100-    "*****************************************".                 SQ2154.2
027200     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2154.2
027300-    "******************************".                            SQ2154.2
027400 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2154.2
027500     "SQ215A".                                                    SQ2154.2
027600*                                                                 SQ2154.2
027700 PROCEDURE DIVISION.                                              SQ2154.2
027800 DECLARATIVES.                                                    SQ2154.2
027900 SQ-FS1-DECLARATIVE SECTION.                                      SQ2154.2
028000     USE AFTER STANDARD EXCEPTION PROCEDURE ON SQ-FS1.            SQ2154.2
028100 OUTPUT-ERROR-PROCESS.                                            SQ2154.2
028200     IF SQ-FS1-STATUS = "38"                                      SQ2154.2
028300             PERFORM PASS-DECL                                    SQ2154.2
028400             GO TO ABNORMAL-TERM-DECL                             SQ2154.2
028500     ELSE                                                         SQ2154.2
028600             MOVE "38" TO CORRECT-A                               SQ2154.2
028700             MOVE SQ-FS1-STATUS TO COMPUTED-A                     SQ2154.2
028800             MOVE "STATUS AFTER OPEN AFTER LOCK INCORRECT"        SQ2154.2
028900                     TO RE-MARK                                   SQ2154.2
029000             MOVE "VII-3, FILE STATUS" TO ANSI-REFERENCE          SQ2154.2
029100             PERFORM FAIL-DECL                                    SQ2154.2
029200             GO TO ABNORMAL-TERM-DECL                             SQ2154.2
029300     END-IF.                                                      SQ2154.2
029400*                                                                 SQ2154.2
029500 PASS-DECL.                                                       SQ2154.2
029600     MOVE   "PASS " TO P-OR-F.                                    SQ2154.2
029700     ADD     1 TO PASS-COUNTER.                                   SQ2154.2
029800     PERFORM PRINT-DETAIL-DECL.                                   SQ2154.2
029900*                                                                 SQ2154.2
030000 FAIL-DECL.                                                       SQ2154.2
030100     MOVE   "FAIL*" TO P-OR-F.                                    SQ2154.2
030200     ADD     1 TO ERROR-COUNTER.                                  SQ2154.2
030300     PERFORM PRINT-DETAIL-DECL.                                   SQ2154.2
030400*                                                                 SQ2154.2
030500 PRINT-DETAIL-DECL.                                               SQ2154.2
030600     IF REC-CT NOT EQUAL TO ZERO                                  SQ2154.2
030700         MOVE   "." TO PARDOT-X                                   SQ2154.2
030800         MOVE    REC-CT TO DOTVALUE.                              SQ2154.2
030900     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2154.2
031000     PERFORM WRITE-LINE-DECL.                                     SQ2154.2
031100     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2154.2
031200         PERFORM WRITE-LINE-DECL                                  SQ2154.2
031300         PERFORM FAIL-ROUTINE-DECL THRU FAIL-ROUTINE-EX-DECL      SQ2154.2
031400     ELSE                                                         SQ2154.2
031500         PERFORM BAIL-OUT-DECL THRU BAIL-OUT-EX-DECL.             SQ2154.2
031600     MOVE    SPACE TO P-OR-F.                                     SQ2154.2
031700     MOVE    SPACE TO COMPUTED-X.                                 SQ2154.2
031800     MOVE    SPACE TO CORRECT-X.                                  SQ2154.2
031900     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2154.2
032000     MOVE    SPACE TO RE-MARK.                                    SQ2154.2
032100*                                                                 SQ2154.2
032200 WRITE-LINE-DECL.                                                 SQ2154.2
032300     ADD     1 TO RECORD-COUNT.                                   SQ2154.2
032400*    IF RECORD-COUNT GREATER 50                                   SQ2154.2
032500*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2154.2
032600*        MOVE  SPACE TO DUMMY-RECORD                              SQ2154.2
032700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2154.2
032800*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-DECL       SQ2154.2
032900*        MOVE  CCVS-C-2 TO DUMMY-RECORD                           SQ2154.2
033000*        PERFORM WRT-LN-DECL 2 TIMES                              SQ2154.2
033100*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-DECL    SQ2154.2
033200*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2154.2
033300*        MOVE  ZERO TO RECORD-COUNT.                              SQ2154.2
033400     PERFORM WRT-LN-DECL.                                         SQ2154.2
033500*                                                                 SQ2154.2
033600 WRT-LN-DECL.                                                     SQ2154.2
033700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2154.2
033800     MOVE    SPACE TO DUMMY-RECORD.                               SQ2154.2
033900 BLANK-LINE-PRINT-DECL.                                           SQ2154.2
034000     PERFORM WRT-LN-DECL.                                         SQ2154.2
034100 FAIL-ROUTINE-DECL.                                               SQ2154.2
034200     IF COMPUTED-X NOT EQUAL TO SPACE                             SQ2154.2
034300             GO TO FAIL-ROUTINE-WRITE-DECL.                       SQ2154.2
034400     IF CORRECT-X NOT EQUAL TO SPACE                              SQ2154.2
034500             GO TO FAIL-ROUTINE-WRITE-DECL.                       SQ2154.2
034600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2154.2
034700     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2154.2
034800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2154.2
034900     PERFORM WRITE-LINE-DECL 2 TIMES.                             SQ2154.2
035000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2154.2
035100     GO TO   FAIL-ROUTINE-EX-DECL.                                SQ2154.2
035200 FAIL-ROUTINE-WRITE-DECL.                                         SQ2154.2
035300     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2154.2
035400     PERFORM WRITE-LINE-DECL                                      SQ2154.2
035500     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2154.2
035600     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2154.2
035700     PERFORM WRITE-LINE-DECL 2 TIMES.                             SQ2154.2
035800     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2154.2
035900 FAIL-ROUTINE-EX-DECL.                                            SQ2154.2
036000     EXIT.                                                        SQ2154.2
036100 BAIL-OUT-DECL.                                                   SQ2154.2
036200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-DECL.  SQ2154.2
036300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-DECL.          SQ2154.2
036400 BAIL-OUT-WRITE-DECL.                                             SQ2154.2
036500     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2154.2
036600     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2154.2
036700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2154.2
036800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2154.2
036900     PERFORM WRITE-LINE-DECL 2 TIMES.                             SQ2154.2
037000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2154.2
037100 BAIL-OUT-EX-DECL.                                                SQ2154.2
037200     EXIT.                                                        SQ2154.2
037300*                                                                 SQ2154.2
037400 ABNORMAL-TERM-DECL.                                              SQ2154.2
037500     MOVE    SPACE TO DUMMY-RECORD                                SQ2154.2
037600     PERFORM WRITE-LINE-DECL                                      SQ2154.2
037700     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ2154.2
037800               TO DUMMY-RECORD                                    SQ2154.2
037900     PERFORM WRITE-LINE-DECL 3 TIMES.                             SQ2154.2
038000*                                                                 SQ2154.2
038100 EXIT-DECL.                                                       SQ2154.2
038200     EXIT.                                                        SQ2154.2
038300 END DECLARATIVES.                                                SQ2154.2
038400*                                                                 SQ2154.2
038500 CCVS1 SECTION.                                                   SQ2154.2
038600 OPEN-FILES.                                                      SQ2154.2
038700     OPEN    OUTPUT PRINT-FILE.                                   SQ2154.2
038800     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2154.2
038900     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2154.2
039000     MOVE    SPACE TO TEST-RESULTS.                               SQ2154.2
039100     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2154.2
039200     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2154.2
039300     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2154.2
039400     GO TO CCVS1-EXIT.                                            SQ2154.2
039500*                                                                 SQ2154.2
039600 CCVS-INIT-FILE.                                                  SQ2154.2
039700     ADD     1 TO REC-SKL-SUB.                                    SQ2154.2
039800     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2154.2
039900                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2154.2
040000*                                                                 SQ2154.2
040100 CLOSE-FILES.                                                     SQ2154.2
040200     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2154.2
040300     CLOSE   PRINT-FILE.                                          SQ2154.2
040400 TERMINATE-CCVS.                                                  SQ2154.2
040500     STOP    RUN.                                                 SQ2154.2
040600*                                                                 SQ2154.2
040700 INSPT.                                                           SQ2154.2
040800     MOVE   "INSPT" TO P-OR-F.                                    SQ2154.2
040900     ADD     1 TO INSPECT-COUNTER.                                SQ2154.2
041000     PERFORM PRINT-DETAIL.                                        SQ2154.2
041100                                                                  SQ2154.2
041200 PASS.                                                            SQ2154.2
041300     MOVE   "PASS " TO P-OR-F.                                    SQ2154.2
041400     ADD     1 TO PASS-COUNTER.                                   SQ2154.2
041500     PERFORM PRINT-DETAIL.                                        SQ2154.2
041600*                                                                 SQ2154.2
041700 FAIL.                                                            SQ2154.2
041800     MOVE   "FAIL*" TO P-OR-F.                                    SQ2154.2
041900     ADD     1 TO ERROR-COUNTER.                                  SQ2154.2
042000     PERFORM PRINT-DETAIL.                                        SQ2154.2
042100*                                                                 SQ2154.2
042200 DE-LETE.                                                         SQ2154.2
042300     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2154.2
042400     MOVE   "*****" TO P-OR-F.                                    SQ2154.2
042500     ADD     1 TO DELETE-COUNTER.                                 SQ2154.2
042600     PERFORM PRINT-DETAIL.                                        SQ2154.2
042700*                                                                 SQ2154.2
042800 PRINT-DETAIL.                                                    SQ2154.2
042900     IF REC-CT NOT EQUAL TO ZERO                                  SQ2154.2
043000         MOVE   "." TO PARDOT-X                                   SQ2154.2
043100         MOVE    REC-CT TO DOTVALUE.                              SQ2154.2
043200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2154.2
043300     PERFORM WRITE-LINE.                                          SQ2154.2
043400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2154.2
043500         PERFORM WRITE-LINE                                       SQ2154.2
043600         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2154.2
043700     ELSE                                                         SQ2154.2
043800         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2154.2
043900     MOVE    SPACE TO P-OR-F.                                     SQ2154.2
044000     MOVE    SPACE TO COMPUTED-X.                                 SQ2154.2
044100     MOVE    SPACE TO CORRECT-X.                                  SQ2154.2
044200     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2154.2
044300     MOVE    SPACE TO RE-MARK.                                    SQ2154.2
044400*                                                                 SQ2154.2
044500 HEAD-ROUTINE.                                                    SQ2154.2
044600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2154.2
044700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2154.2
044800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2154.2
044900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2154.2
045000 COLUMN-NAMES-ROUTINE.                                            SQ2154.2
045100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2154.2
045200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2154.2
045300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2154.2
045400 END-ROUTINE.                                                     SQ2154.2
045500     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2154.2
045600     PERFORM WRITE-LINE 5 TIMES.                                  SQ2154.2
045700 END-RTN-EXIT.                                                    SQ2154.2
045800     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2154.2
045900     PERFORM WRITE-LINE 2 TIMES.                                  SQ2154.2
046000*                                                                 SQ2154.2
046100 END-ROUTINE-1.                                                   SQ2154.2
046200     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2154.2
046300     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2154.2
046400     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2154.2
046500     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2154.2
046600     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2154.2
046700     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2154.2
046800     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2154.2
046900     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2154.2
047000     PERFORM WRITE-LINE.                                          SQ2154.2
047100     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2154.2
047200     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2154.2
047300         MOVE   "NO " TO ERROR-TOTAL                              SQ2154.2
047400     ELSE                                                         SQ2154.2
047500         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2154.2
047600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2154.2
047700     PERFORM WRITE-LINE.                                          SQ2154.2
047800 END-ROUTINE-13.                                                  SQ2154.2
047900     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2154.2
048000         MOVE   "NO " TO ERROR-TOTAL                              SQ2154.2
048100     ELSE                                                         SQ2154.2
048200         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2154.2
048300     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2154.2
048400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2154.2
048500     PERFORM WRITE-LINE.                                          SQ2154.2
048600     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2154.2
048700         MOVE   "NO " TO ERROR-TOTAL                              SQ2154.2
048800     ELSE                                                         SQ2154.2
048900         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2154.2
049000     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2154.2
049100     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2154.2
049200     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2154.2
049300*                                                                 SQ2154.2
049400 WRITE-LINE.                                                      SQ2154.2
049500     ADD     1 TO RECORD-COUNT.                                   SQ2154.2
049600*    IF RECORD-COUNT GREATER 50                                   SQ2154.2
049700*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2154.2
049800*        MOVE  SPACE TO DUMMY-RECORD                              SQ2154.2
049900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2154.2
050000*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2154.2
050100*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2154.2
050200*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2154.2
050300*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2154.2
050400*        MOVE  ZERO TO RECORD-COUNT.                              SQ2154.2
050500     PERFORM WRT-LN.                                              SQ2154.2
050600*                                                                 SQ2154.2
050700 WRT-LN.                                                          SQ2154.2
050800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2154.2
050900     MOVE    SPACE TO DUMMY-RECORD.                               SQ2154.2
051000 BLANK-LINE-PRINT.                                                SQ2154.2
051100     PERFORM WRT-LN.                                              SQ2154.2
051200 FAIL-ROUTINE.                                                    SQ2154.2
051300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2154.2
051400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2154.2
051500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2154.2
051600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2154.2
051700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2154.2
051800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2154.2
051900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2154.2
052000     GO TO   FAIL-ROUTINE-EX.                                     SQ2154.2
052100 FAIL-ROUTINE-WRITE.                                              SQ2154.2
052200     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2154.2
052300     PERFORM WRITE-LINE                                           SQ2154.2
052400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2154.2
052500     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2154.2
052600     PERFORM WRITE-LINE 2 TIMES.                                  SQ2154.2
052700     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2154.2
052800 FAIL-ROUTINE-EX.                                                 SQ2154.2
052900     EXIT.                                                        SQ2154.2
053000 BAIL-OUT.                                                        SQ2154.2
053100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2154.2
053200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2154.2
053300 BAIL-OUT-WRITE.                                                  SQ2154.2
053400     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2154.2
053500     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2154.2
053600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2154.2
053700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2154.2
053800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2154.2
053900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2154.2
054000 BAIL-OUT-EX.                                                     SQ2154.2
054100     EXIT.                                                        SQ2154.2
054200 CCVS1-EXIT.                                                      SQ2154.2
054300     EXIT.                                                        SQ2154.2
054400*                                                                 SQ2154.2
054500****************************************************************  SQ2154.2
054600*                                                              *  SQ2154.2
054700*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2154.2
054800*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2154.2
054900*                                                              *  SQ2154.2
055000****************************************************************  SQ2154.2
055100*                                                                 SQ2154.2
055200 SECT-SQ215A-0001 SECTION.                                        SQ2154.2
055300 WRITE-INIT-GF-01.                                                SQ2154.2
055400*                                                                 SQ2154.2
055500*        THIS TEST CREATES FILE SQ-FS1 AND CLOSES IT WITH LOCK.   SQ2154.2
055600*        FIRST IT SETS UP A SKELETON RECORD IN WORKING STORAGE.   SQ2154.2
055700*                                                                 SQ2154.2
055800     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2154.2
055900     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2154.2
056000     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ2154.2
056100     MOVE 120      TO XRECORD-LENGTH (1).                         SQ2154.2
056200     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       SQ2154.2
056300     MOVE 1        TO XBLOCK-SIZE (1).                            SQ2154.2
056400     MOVE 1        TO RECORDS-IN-FILE (1).                        SQ2154.2
056500     MOVE "SQ"     TO XFILE-ORGANIZATION (1).                     SQ2154.2
056600     MOVE "S"      TO XLABEL-TYPE (1).                            SQ2154.2
056700     MOVE 1        TO XRECORD-NUMBER (1).                         SQ2154.2
056800*                                                                 SQ2154.2
056900 WRITE-OPEN-01.                                                   SQ2154.2
057000     MOVE    1 TO REC-CT.                                         SQ2154.2
057100     MOVE   "WRITE-OPEN-01" TO PAR-NAME.                          SQ2154.2
057200     MOVE   "OPEN OUTPUT - NEW FILE" TO FEATURE.                  SQ2154.2
057300     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2154.2
057400     OPEN    OUTPUT SQ-FS1.                                       SQ2154.2
057500     IF SQ-FS1-STATUS = "00"                                      SQ2154.2
057600         PERFORM PASS                                             SQ2154.2
057700     ELSE                                                         SQ2154.2
057800         MOVE   "00" TO CORRECT-A                                 SQ2154.2
057900         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2154.2
058000         MOVE   "FILE OPEN FAILED, FURTHER TESTS ABANDONED"       SQ2154.2
058100                   TO RE-MARK                                     SQ2154.2
058200         MOVE   "VII-3, VII-40, FILE STATUS" TO ANSI-REFERENCE    SQ2154.2
058300         PERFORM FAIL                                             SQ2154.2
058400         GO TO   CCVS-EXIT                                        SQ2154.2
058500     END-IF.                                                      SQ2154.2
058600*                                                                 SQ2154.2
058700*        WRITE A SINGLE RECORD TO THE FILE                        SQ2154.2
058800*                                                                 SQ2154.2
058900 WRITE-INIT-01.                                                   SQ2154.2
059000     MOVE    1 TO REC-CT.                                         SQ2154.2
059100     MOVE   "WRITE-TEST-01" TO PAR-NAME                           SQ2154.2
059200     MOVE   "SEQUENTIAL WRITE" TO FEATURE.                        SQ2154.2
059300 WRITE-TEST-01-01.                                                SQ2154.2
059400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.     SQ2154.2
059500     WRITE   SQ-FS1R1-F-G-120.                                    SQ2154.2
059600     IF SQ-FS1-STATUS = "00"                                      SQ2154.2
059700         PERFORM PASS                                             SQ2154.2
059800     ELSE                                                         SQ2154.2
059900         MOVE   "00" TO CORRECT-A                                 SQ2154.2
060000         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2154.2
060100         MOVE   "WRITING FAILED, FURTHER TESTS ABANDONED"         SQ2154.2
060200                   TO RE-MARK                                     SQ2154.2
060300         MOVE   "VII-3, VII-53, FILE STATUS" TO ANSI-REFERENCE    SQ2154.2
060400         PERFORM FAIL                                             SQ2154.2
060500         GO TO   CCVS-EXIT                                        SQ2154.2
060600     END-IF.                                                      SQ2154.2
060700*                                                                 SQ2154.2
060800*        CLOSE THE FILE WITH LOCK, SO IT SHOULD NOT REOPEN        SQ2154.2
060900*                                                                 SQ2154.2
061000 CLOSE-INIT-01.                                                   SQ2154.2
061100     MOVE    1 TO REC-CT.                                         SQ2154.2
061200     MOVE   "CLOSE-TEST-01"   TO PAR-NAME.                        SQ2154.2
061300     MOVE   "CLOSE WITH LOCK" TO FEATURE.                         SQ2154.2
061400     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2154.2
061500 CLOSE-TEST-01.                                                   SQ2154.2
061600     CLOSE   SQ-FS1 WITH LOCK.                                    SQ2154.2
061700     IF SQ-FS1-STATUS = "00"                                      SQ2154.2
061800         PERFORM PASS                                             SQ2154.2
061900     ELSE                                                         SQ2154.2
062000         MOVE   "00" TO CORRECT-A                                 SQ2154.2
062100         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ2154.2
062200         MOVE   "CLOSE WITH LOCK FAILED, FURTHER TESTS ABANDONED" SQ2154.2
062300                   TO RE-MARK                                     SQ2154.2
062400         MOVE   "VII-3, VII-38, FILE STATUS" TO ANSI-REFERENCE    SQ2154.2
062500         PERFORM FAIL                                             SQ2154.2
062600         GO TO   CCVS-EXIT                                        SQ2154.2
062700     END-IF.                                                      SQ2154.2
062800*                                                                 SQ2154.2
062900*        HAVING LOCKED THE FILE, WE NOW TRY TO REOPEN IT.         SQ2154.2
063000*        THE TEST PASSES IF THE FILE CANNOT BE OPENED AND         SQ2154.2
063100*        THE APPROPRIATE I-O STATUS VALUE IS RETURNED.            SQ2154.2
063200*        AN IMPLEMENTATION MAY TERMINATE EXECUTION OF A           SQ2154.2
063300*        PROGRAM WHICH ATTEMPTS TO REOPEN A LOCKED FILE,          SQ2154.2
063400*        OR MAY RETURN CONTROL TO THE STATEMENT FOLLOWING         SQ2154.2
063500*        THE OPEN STATEMENT.                                      SQ2154.2
063600*                                                                 SQ2154.2
063700 OPEN-INIT-01.                                                    SQ2154.2
063800*                                                                 SQ2154.2
063900     MOVE    1 TO REC-CT.                                         SQ2154.2
064000     MOVE   "OPEN-TEST-01"    TO PAR-NAME.                        SQ2154.2
064100     MOVE   "OPEN AFTER LOCK" TO FEATURE.                         SQ2154.2
064200     MOVE   "**" TO SQ-FS1-STATUS.                                SQ2154.2
064300 OPEN-TEST-01.                                                    SQ2154.2
064400     OPEN    OUTPUT SQ-FS1.                                       SQ2154.2
064500*                                                                 SQ2154.2
064600 CCVS-EXIT SECTION.                                               SQ2154.2
064700 CCVS-999999.                                                     SQ2154.2
064800     GO TO CLOSE-FILES.                                           SQ2154.2
      *END-OF,SQ215A                                                                  
