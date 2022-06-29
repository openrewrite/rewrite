      *HEADER,COBOL,SQ122A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1224.2
000200 PROGRAM-ID.                                                      SQ1224.2
000300     SQ122A.                                                      SQ1224.2
000400****************************************************************  SQ1224.2
000500*                                                              *  SQ1224.2
000600*    VALIDATION FOR:-                                          *  SQ1224.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1224.2
000800*    USING CCVS85 VERSION 3.1                                  *  SQ1224.2
000900*                                                              *  SQ1224.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ1224.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1224.2
001200*                                                              *  SQ1224.2
001300****************************************************************  SQ1224.2
001400*                                                              *  SQ1224.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1224.2
001600*                                                              *  SQ1224.2
001700*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1224.2
001800*            X-55   SYSTEM PRINTER                             *  SQ1224.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ1224.2
002000*            X-83   OBJECT-COMPUTER                            *  SQ1224.2
002100*            X-84   LABEL RECORDS OPTION                       *  SQ1224.2
002200*                                                              *  SQ1224.2
002300****************************************************************  SQ1224.2
002400*                                                              *  SQ1224.2
002500*    A ONE RECORD FILE WITH TWO CHARACTERS PER BLOCK IS CREATED*  SQ1224.2
002600*    WITH THE INTENTION THAT IT SHOULD END PART-WAY THROUGH A  *  SQ1224.2
002700*    BLOCK.                     THE FILE IS RE-OPENED AND      *  SQ1224.2
002800*    THREE READ STATEMENTS EXECUTED.  THE FIRST SHOULD BE      *  SQ1224.2
002900*    EXECUTED SUCCESSFULLY, THE SECOND RAISE THE AT END        *  SQ1224.2
003000*    CONDITION, AND THE THIRD, WHICH IS A READ AFTER END OF    *  SQ1224.2
003100*    FILE, SHOULD CAUSE THE I-O STATUS CODE 46.                *  SQ1224.2
003200*                                                              *  SQ1224.2
003300****************************************************************  SQ1224.2
003400*                                                                 SQ1224.2
003500 ENVIRONMENT DIVISION.                                            SQ1224.2
003600 CONFIGURATION SECTION.                                           SQ1224.2
003700 SOURCE-COMPUTER.                                                 SQ1224.2
003800     XXXXX082.                                                    SQ1224.2
003900 OBJECT-COMPUTER.                                                 SQ1224.2
004000     XXXXX083.                                                    SQ1224.2
004100*                                                                 SQ1224.2
004200 INPUT-OUTPUT SECTION.                                            SQ1224.2
004300 FILE-CONTROL.                                                    SQ1224.2
004400     SELECT PRINT-FILE ASSIGN TO                                  SQ1224.2
004500     XXXXX055.                                                    SQ1224.2
004600*                                                                 SQ1224.2
004700     SELECT SQ-FS4 ASSIGN                                         SQ1224.2
004800     XXXXX014                                                     SQ1224.2
004900            FILE STATUS IS SQ-FS4-STATUS.                         SQ1224.2
005000*                                                                 SQ1224.2
005100*                                                                 SQ1224.2
005200 DATA DIVISION.                                                   SQ1224.2
005300 FILE SECTION.                                                    SQ1224.2
005400 FD  PRINT-FILE                                                   SQ1224.2
005500C    LABEL RECORDS                                                SQ1224.2
005600C    XXXXX084                                                     SQ1224.2
005700C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1224.2
005800               .                                                  SQ1224.2
005900 01  PRINT-REC    PICTURE X(120).                                 SQ1224.2
006000 01  DUMMY-RECORD PICTURE X(120).                                 SQ1224.2
006100*                                                                 SQ1224.2
006200 FD  SQ-FS4                                                       SQ1224.2
006300C    LABEL RECORD IS STANDARD                                     SQ1224.2
006400     BLOCK  2 RECORDS                                             SQ1224.2
006500     RECORD 125                                                   SQ1224.2
006600                .                                                 SQ1224.2
006700 01  SQ-FS4R1-F-G-125.                                            SQ1224.2
006800     05  SQ-FS4-FIRST  PIC X(120).                                SQ1224.2
006900     05  SQ-FS4-REC-NO PIC 99999.                                 SQ1224.2
007000*                                                                 SQ1224.2
007100 WORKING-STORAGE SECTION.                                         SQ1224.2
007200*                                                                 SQ1224.2
007300***************************************************************   SQ1224.2
007400*                                                             *   SQ1224.2
007500*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1224.2
007600*                                                             *   SQ1224.2
007700***************************************************************   SQ1224.2
007800*                                                                 SQ1224.2
007900 01  SQ-FS4-STATUS.                                               SQ1224.2
008000   03  SQ-FS4-KEY-1   PIC X.                                      SQ1224.2
008100   03  SQ-FS4-KEY-2   PIC X.                                      SQ1224.2
008200*                                                                 SQ1224.2
008300***************************************************************   SQ1224.2
008400*                                                             *   SQ1224.2
008500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1224.2
008600*                                                             *   SQ1224.2
008700***************************************************************   SQ1224.2
008800*                                                                 SQ1224.2
008900 01  REC-SKEL-SUB   PIC 99.                                       SQ1224.2
009000*                                                                 SQ1224.2
009100 01  FILE-RECORD-INFORMATION-REC.                                 SQ1224.2
009200     03 FILE-RECORD-INFO-SKELETON.                                SQ1224.2
009300        05 FILLER                 PICTURE X(48)       VALUE       SQ1224.2
009400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1224.2
009500        05 FILLER                 PICTURE X(46)       VALUE       SQ1224.2
009600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1224.2
009700        05 FILLER                 PICTURE X(26)       VALUE       SQ1224.2
009800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1224.2
009900        05 FILLER                 PICTURE X(37)       VALUE       SQ1224.2
010000             ",RECKEY=                             ".             SQ1224.2
010100        05 FILLER                 PICTURE X(38)       VALUE       SQ1224.2
010200             ",ALTKEY1=                             ".            SQ1224.2
010300        05 FILLER                 PICTURE X(38)       VALUE       SQ1224.2
010400             ",ALTKEY2=                             ".            SQ1224.2
010500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1224.2
010600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1224.2
010700        05 FILE-RECORD-INFO-P1-120.                               SQ1224.2
010800           07 FILLER              PIC X(5).                       SQ1224.2
010900           07 XFILE-NAME          PIC X(6).                       SQ1224.2
011000           07 FILLER              PIC X(8).                       SQ1224.2
011100           07 XRECORD-NAME        PIC X(6).                       SQ1224.2
011200           07 FILLER              PIC X(1).                       SQ1224.2
011300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1224.2
011400           07 FILLER              PIC X(7).                       SQ1224.2
011500           07 XRECORD-NUMBER      PIC 9(6).                       SQ1224.2
011600           07 FILLER              PIC X(6).                       SQ1224.2
011700           07 UPDATE-NUMBER       PIC 9(2).                       SQ1224.2
011800           07 FILLER              PIC X(5).                       SQ1224.2
011900           07 ODO-NUMBER          PIC 9(4).                       SQ1224.2
012000           07 FILLER              PIC X(5).                       SQ1224.2
012100           07 XPROGRAM-NAME       PIC X(5).                       SQ1224.2
012200           07 FILLER              PIC X(7).                       SQ1224.2
012300           07 XRECORD-LENGTH      PIC 9(6).                       SQ1224.2
012400           07 FILLER              PIC X(7).                       SQ1224.2
012500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1224.2
012600           07 FILLER              PIC X(1).                       SQ1224.2
012700           07 XBLOCK-SIZE         PIC 9(4).                       SQ1224.2
012800           07 FILLER              PIC X(6).                       SQ1224.2
012900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1224.2
013000           07 FILLER              PIC X(5).                       SQ1224.2
013100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1224.2
013200           07 FILLER              PIC X(6).                       SQ1224.2
013300           07 XLABEL-TYPE         PIC X(1).                       SQ1224.2
013400        05 FILE-RECORD-INFO-P121-240.                             SQ1224.2
013500           07 FILLER              PIC X(8).                       SQ1224.2
013600           07 XRECORD-KEY         PIC X(29).                      SQ1224.2
013700           07 FILLER              PIC X(9).                       SQ1224.2
013800           07 ALTERNATE-KEY1      PIC X(29).                      SQ1224.2
013900           07 FILLER              PIC X(9).                       SQ1224.2
014000           07 ALTERNATE-KEY2      PIC X(29).                      SQ1224.2
014100           07 FILLER              PIC X(7).                       SQ1224.2
014200*                                                                 SQ1224.2
014300 01  TEST-RESULTS.                                                SQ1224.2
014400     02 FILLER              PIC X      VALUE SPACE.               SQ1224.2
014500     02  PAR-NAME.                                                SQ1224.2
014600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1224.2
014700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1224.2
014800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1224.2
014900     02 FILLER              PIC X      VALUE SPACE.               SQ1224.2
015000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1224.2
015100     02 FILLER              PIC X      VALUE SPACE.               SQ1224.2
015200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1224.2
015300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1224.2
015400     02 RE-MARK             PIC X(61).                            SQ1224.2
015500 01  TEST-COMPUTED.                                               SQ1224.2
015600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1224.2
015700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1224.2
015800   02 COMPUTED-X.                                                 SQ1224.2
015900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1224.2
016000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1224.2
016100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1224.2
016200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1224.2
016300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1224.2
016400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1224.2
016500        04 COMPUTED-18V0                   PIC -9(18).            SQ1224.2
016600        04 FILLER                          PIC X.                 SQ1224.2
016700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1224.2
016800 01  TEST-CORRECT.                                                SQ1224.2
016900     02 FILLER PIC X(30) VALUE SPACE.                             SQ1224.2
017000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1224.2
017100     02 CORRECT-X.                                                SQ1224.2
017200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1224.2
017300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1224.2
017400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1224.2
017500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1224.2
017600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1224.2
017700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1224.2
017800         04 CORRECT-18V0                     PIC -9(18).          SQ1224.2
017900         04 FILLER                           PIC X.               SQ1224.2
018000     03 FILLER PIC X(2) VALUE SPACE.                              SQ1224.2
018100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1224.2
018200*                                                                 SQ1224.2
018300 01  CCVS-C-1.                                                    SQ1224.2
018400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1224.2
018500     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1224.2
018600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1224.2
018700     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1224.2
018800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1224.2
018900     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1224.2
019000     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1224.2
019100     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1224.2
019200 01  CCVS-C-2.                                                    SQ1224.2
019300     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1224.2
019400     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1224.2
019500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1224.2
019600     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1224.2
019700     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1224.2
019800*                                                                 SQ1224.2
019900 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1224.2
020000 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1224.2
020100 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1224.2
020200 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1224.2
020300 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1224.2
020400 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1224.2
020500 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1224.2
020600 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1224.2
020700 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1224.2
020800 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1224.2
020900 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1224.2
021000 01  CCVS-H-1.                                                    SQ1224.2
021100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1224.2
021200     02  FILLER          PIC X(42)    VALUE                       SQ1224.2
021300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1224.2
021400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1224.2
021500 01  CCVS-H-2A.                                                   SQ1224.2
021600   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1224.2
021700   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1224.2
021800   02  FILLER            PIC XXXX   VALUE                         SQ1224.2
021900     "4.2 ".                                                      SQ1224.2
022000   02  FILLER            PIC X(28)  VALUE                         SQ1224.2
022100            " COPY - NOT FOR DISTRIBUTION".                       SQ1224.2
022200   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1224.2
022300*                                                                 SQ1224.2
022400 01  CCVS-H-2B.                                                   SQ1224.2
022500   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1224.2
022600   02  TEST-ID           PIC X(9).                                SQ1224.2
022700   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1224.2
022800   02  FILLER            PIC X(12)  VALUE                         SQ1224.2
022900     " HIGH       ".                                              SQ1224.2
023000   02  FILLER            PIC X(22)  VALUE                         SQ1224.2
023100            " LEVEL VALIDATION FOR ".                             SQ1224.2
023200   02  FILLER            PIC X(58)  VALUE                         SQ1224.2
023300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1224.2
023400 01  CCVS-H-3.                                                    SQ1224.2
023500     02  FILLER          PIC X(34)  VALUE                         SQ1224.2
023600            " FOR OFFICIAL USE ONLY    ".                         SQ1224.2
023700     02  FILLER          PIC X(58)  VALUE                         SQ1224.2
023800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1224.2
023900     02  FILLER          PIC X(28)  VALUE                         SQ1224.2
024000            "  COPYRIGHT   1985,1986 ".                           SQ1224.2
024100 01  CCVS-E-1.                                                    SQ1224.2
024200     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1224.2
024300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1224.2
024400     02 ID-AGAIN         PIC X(9).                                SQ1224.2
024500     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1224.2
024600 01  CCVS-E-2.                                                    SQ1224.2
024700     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1224.2
024800     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1224.2
024900     02  CCVS-E-2-2.                                              SQ1224.2
025000         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1224.2
025100         03 FILLER         PIC X      VALUE SPACE.                SQ1224.2
025200         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1224.2
025300            "ERRORS ENCOUNTERED".                                 SQ1224.2
025400 01  CCVS-E-3.                                                    SQ1224.2
025500     02  FILLER          PIC X(22)  VALUE                         SQ1224.2
025600            " FOR OFFICIAL USE ONLY".                             SQ1224.2
025700     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1224.2
025800     02  FILLER          PIC X(58)  VALUE                         SQ1224.2
025900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1224.2
026000     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1224.2
026100     02  FILLER          PIC X(20)  VALUE                         SQ1224.2
026200             " COPYRIGHT 1985,1986".                              SQ1224.2
026300 01  CCVS-E-4.                                                    SQ1224.2
026400     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1224.2
026500     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1224.2
026600     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1224.2
026700     02 FILLER           PIC X(40)  VALUE                         SQ1224.2
026800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1224.2
026900 01  XXINFO.                                                      SQ1224.2
027000     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1224.2
027100     02 INFO-TEXT.                                                SQ1224.2
027200       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1224.2
027300       04 XXCOMPUTED         PIC X(20).                           SQ1224.2
027400       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1224.2
027500       04 XXCORRECT          PIC X(20).                           SQ1224.2
027600     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1224.2
027700 01  HYPHEN-LINE.                                                 SQ1224.2
027800     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1224.2
027900     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1224.2
028000-    "*****************************************".                 SQ1224.2
028100     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1224.2
028200-    "******************************".                            SQ1224.2
028300 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1224.2
028400     "SQ122A".                                                    SQ1224.2
028500*                                                                 SQ1224.2
028600*                                                                 SQ1224.2
028700 PROCEDURE DIVISION.                                              SQ1224.2
028800 DECLARATIVES.                                                    SQ1224.2
028900 SECT-SQ122A-0002 SECTION.                                        SQ1224.2
029000     USE AFTER EXCEPTION PROCEDURE INPUT.                         SQ1224.2
029100 INPUT-ERROR-PROCESS.                                             SQ1224.2
029200     IF SQ-FS4-STATUS = "10"                                      SQ1224.2
029300             GO TO END-DECLS.                                     SQ1224.2
029400     IF SQ-FS4-STATUS = "46"                                      SQ1224.2
029500             PERFORM DECL-PASS                                    SQ1224.2
029600             GO TO ABNORMAL-TERM-DECL                             SQ1224.2
029700     ELSE                                                         SQ1224.2
029800             MOVE "46" TO CORRECT-A                               SQ1224.2
029900             MOVE SQ-FS4-STATUS TO COMPUTED-A                     SQ1224.2
030000             MOVE "STATUS OF READ AFTER EOF READ INCORRECT"       SQ1224.2
030100                     TO RE-MARK                                   SQ1224.2
030200             MOVE "VII-4, 1.3.5(4)E, FILE STATUS"                 SQ1224.2
030300                     TO ANSI-REFERENCE                            SQ1224.2
030400             PERFORM DECL-FAIL                                    SQ1224.2
030500             GO TO ABNORMAL-TERM-DECL                             SQ1224.2
030600     END-IF.                                                      SQ1224.2
030700*                                                                 SQ1224.2
030800 DECL-PASS.                                                       SQ1224.2
030900     MOVE   "PASS " TO P-OR-F.                                    SQ1224.2
031000     ADD     1 TO PASS-COUNTER.                                   SQ1224.2
031100     PERFORM DECL-PRINT-DETAIL.                                   SQ1224.2
031200*                                                                 SQ1224.2
031300 DECL-FAIL.                                                       SQ1224.2
031400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1224.2
031500     ADD     1 TO ERROR-COUNTER.                                  SQ1224.2
031600     PERFORM DECL-PRINT-DETAIL.                                   SQ1224.2
031700*                                                                 SQ1224.2
031800 DECL-PRINT-DETAIL.                                               SQ1224.2
031900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1224.2
032000             MOVE "." TO PARDOT-X                                 SQ1224.2
032100             MOVE REC-CT TO DOTVALUE.                             SQ1224.2
032200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1224.2
032300     PERFORM DECL-WRITE-LINE.                                     SQ1224.2
032400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1224.2
032500         PERFORM DECL-WRITE-LINE                                  SQ1224.2
032600         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1224.2
032700     ELSE                                                         SQ1224.2
032800         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1224.2
032900     MOVE    SPACE TO P-OR-F.                                     SQ1224.2
033000     MOVE    SPACE TO COMPUTED-X.                                 SQ1224.2
033100     MOVE    SPACE TO CORRECT-X.                                  SQ1224.2
033200     IF REC-CT EQUAL TO ZERO                                      SQ1224.2
033300         MOVE    SPACE TO PAR-NAME.                               SQ1224.2
033400     MOVE    SPACE TO RE-MARK.                                    SQ1224.2
033500*                                                                 SQ1224.2
033600 DECL-WRITE-LINE.                                                 SQ1224.2
033700     ADD     1 TO RECORD-COUNT.                                   SQ1224.2
033800*    IF RECORD-COUNT GREATER 50                                   SQ1224.2
033900*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1224.2
034000*        MOVE    SPACE TO DUMMY-RECORD                            SQ1224.2
034100*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1224.2
034200*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1224.2
034300*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1224.2
034400*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1224.2
034500*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1224.2
034600*        PERFORM DECL-WRT-LN                                      SQ1224.2
034700*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1224.2
034800*        MOVE    ZERO TO RECORD-COUNT.                            SQ1224.2
034900     PERFORM DECL-WRT-LN.                                         SQ1224.2
035000*                                                                 SQ1224.2
035100 DECL-WRT-LN.                                                     SQ1224.2
035200     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1224.2
035300     MOVE    SPACE TO DUMMY-RECORD.                               SQ1224.2
035400*                                                                 SQ1224.2
035500 DECL-FAIL-ROUTINE.                                               SQ1224.2
035600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1224.2
035700     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1224.2
035800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1224.2
035900     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1224.2
036000     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1224.2
036100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1224.2
036200     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1224.2
036300     GO TO   DECL-FAIL-EX.                                        SQ1224.2
036400 DECL-FAIL-WRITE.                                                 SQ1224.2
036500     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1224.2
036600     PERFORM DECL-WRITE-LINE                                      SQ1224.2
036700     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1224.2
036800     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1224.2
036900     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1224.2
037000     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1224.2
037100 DECL-FAIL-EX.                                                    SQ1224.2
037200     EXIT.                                                        SQ1224.2
037300*                                                                 SQ1224.2
037400 DECL-BAIL.                                                       SQ1224.2
037500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1224.2
037600     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1224.2
037700 DECL-BAIL-WRITE.                                                 SQ1224.2
037800     MOVE    CORRECT-A TO XXCORRECT.                              SQ1224.2
037900     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1224.2
038000     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1224.2
038100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1224.2
038200 DECL-BAIL-EX.                                                    SQ1224.2
038300     EXIT.                                                        SQ1224.2
038400*                                                                 SQ1224.2
038500 ABNORMAL-TERM-DECL.                                              SQ1224.2
038600     MOVE SPACE TO DUMMY-RECORD.                                  SQ1224.2
038700     PERFORM DECL-WRITE-LINE.                                     SQ1224.2
038800     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ1224.2
038900             TO DUMMY-RECORD.                                     SQ1224.2
039000     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1224.2
039100*                                                                 SQ1224.2
039200 END-DECLS.                                                       SQ1224.2
039300 END DECLARATIVES.                                                SQ1224.2
039400*                                                                 SQ1224.2
039500*                                                                 SQ1224.2
039600 CCVS1 SECTION.                                                   SQ1224.2
039700 OPEN-FILES.                                                      SQ1224.2
039800     OPEN    OUTPUT PRINT-FILE.                                   SQ1224.2
039900     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1224.2
040000     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1224.2
040100     MOVE    SPACE TO TEST-RESULTS.                               SQ1224.2
040200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1224.2
040300     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1224.2
040400     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1224.2
040500     GO TO CCVS1-EXIT.                                            SQ1224.2
040600*                                                                 SQ1224.2
040700 CCVS-INIT-FILE.                                                  SQ1224.2
040800     ADD     1 TO REC-SKL-SUB.                                    SQ1224.2
040900     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1224.2
041000                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1224.2
041100*                                                                 SQ1224.2
041200 CLOSE-FILES.                                                     SQ1224.2
041300     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1224.2
041400     CLOSE   PRINT-FILE.                                          SQ1224.2
041500 TERMINATE-CCVS.                                                  SQ1224.2
041600     STOP    RUN.                                                 SQ1224.2
041700*                                                                 SQ1224.2
041800 INSPT.                                                           SQ1224.2
041900     MOVE   "INSPT" TO P-OR-F.                                    SQ1224.2
042000     ADD     1 TO INSPECT-COUNTER.                                SQ1224.2
042100     PERFORM PRINT-DETAIL.                                        SQ1224.2
042200*                                                                 SQ1224.2
042300 PASS.                                                            SQ1224.2
042400     MOVE   "PASS " TO P-OR-F.                                    SQ1224.2
042500     ADD     1 TO PASS-COUNTER.                                   SQ1224.2
042600     PERFORM PRINT-DETAIL.                                        SQ1224.2
042700*                                                                 SQ1224.2
042800 FAIL.                                                            SQ1224.2
042900     MOVE   "FAIL*" TO P-OR-F.                                    SQ1224.2
043000     ADD     1 TO ERROR-COUNTER.                                  SQ1224.2
043100     PERFORM PRINT-DETAIL.                                        SQ1224.2
043200*                                                                 SQ1224.2
043300 DE-LETE.                                                         SQ1224.2
043400     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1224.2
043500     MOVE   "*****" TO P-OR-F.                                    SQ1224.2
043600     ADD     1 TO DELETE-COUNTER.                                 SQ1224.2
043700     PERFORM PRINT-DETAIL.                                        SQ1224.2
043800*                                                                 SQ1224.2
043900 PRINT-DETAIL.                                                    SQ1224.2
044000     IF REC-CT NOT EQUAL TO ZERO                                  SQ1224.2
044100         MOVE   "." TO PARDOT-X                                   SQ1224.2
044200         MOVE    REC-CT TO DOTVALUE.                              SQ1224.2
044300     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1224.2
044400     PERFORM WRITE-LINE.                                          SQ1224.2
044500     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1224.2
044600         PERFORM WRITE-LINE                                       SQ1224.2
044700         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1224.2
044800     ELSE                                                         SQ1224.2
044900         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1224.2
045000     MOVE    SPACE TO P-OR-F.                                     SQ1224.2
045100     MOVE    SPACE TO COMPUTED-X.                                 SQ1224.2
045200     MOVE    SPACE TO CORRECT-X.                                  SQ1224.2
045300     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1224.2
045400     MOVE    SPACE TO RE-MARK.                                    SQ1224.2
045500*                                                                 SQ1224.2
045600 HEAD-ROUTINE.                                                    SQ1224.2
045700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1224.2
045800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1224.2
045900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1224.2
046000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1224.2
046100 COLUMN-NAMES-ROUTINE.                                            SQ1224.2
046200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1224.2
046300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1224.2
046400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1224.2
046500 END-ROUTINE.                                                     SQ1224.2
046600     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1224.2
046700     PERFORM WRITE-LINE 5 TIMES.                                  SQ1224.2
046800 END-RTN-EXIT.                                                    SQ1224.2
046900     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1224.2
047000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1224.2
047100*                                                                 SQ1224.2
047200 END-ROUTINE-1.                                                   SQ1224.2
047300     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1224.2
047400     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1224.2
047500     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1224.2
047600     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1224.2
047700     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1224.2
047800     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1224.2
047900     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1224.2
048000     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1224.2
048100     PERFORM WRITE-LINE.                                          SQ1224.2
048200     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1224.2
048300     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1224.2
048400         MOVE   "NO " TO ERROR-TOTAL                              SQ1224.2
048500     ELSE                                                         SQ1224.2
048600         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1224.2
048700     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1224.2
048800     PERFORM WRITE-LINE.                                          SQ1224.2
048900 END-ROUTINE-13.                                                  SQ1224.2
049000     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1224.2
049100         MOVE   "NO " TO ERROR-TOTAL                              SQ1224.2
049200     ELSE                                                         SQ1224.2
049300         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1224.2
049400     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1224.2
049500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1224.2
049600     PERFORM WRITE-LINE.                                          SQ1224.2
049700     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1224.2
049800         MOVE   "NO " TO ERROR-TOTAL                              SQ1224.2
049900     ELSE                                                         SQ1224.2
050000         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1224.2
050100     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1224.2
050200     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1224.2
050300     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1224.2
050400*                                                                 SQ1224.2
050500 WRITE-LINE.                                                      SQ1224.2
050600     ADD     1 TO RECORD-COUNT.                                   SQ1224.2
050700*    IF RECORD-COUNT GREATER 50                                   SQ1224.2
050800*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1224.2
050900*        MOVE  SPACE TO DUMMY-RECORD                              SQ1224.2
051000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1224.2
051100*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1224.2
051200*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1224.2
051300*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1224.2
051400*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1224.2
051500*        MOVE  ZERO TO RECORD-COUNT.                              SQ1224.2
051600     PERFORM WRT-LN.                                              SQ1224.2
051700*                                                                 SQ1224.2
051800 WRT-LN.                                                          SQ1224.2
051900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1224.2
052000     MOVE    SPACE TO DUMMY-RECORD.                               SQ1224.2
052100 BLANK-LINE-PRINT.                                                SQ1224.2
052200     PERFORM WRT-LN.                                              SQ1224.2
052300 FAIL-ROUTINE.                                                    SQ1224.2
052400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1224.2
052500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1224.2
052600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1224.2
052700     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1224.2
052800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1224.2
052900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1224.2
053000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1224.2
053100     GO TO   FAIL-ROUTINE-EX.                                     SQ1224.2
053200 FAIL-ROUTINE-WRITE.                                              SQ1224.2
053300     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1224.2
053400     PERFORM WRITE-LINE                                           SQ1224.2
053500     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1224.2
053600     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1224.2
053700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1224.2
053800     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1224.2
053900 FAIL-ROUTINE-EX.                                                 SQ1224.2
054000     EXIT.                                                        SQ1224.2
054100 BAIL-OUT.                                                        SQ1224.2
054200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1224.2
054300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1224.2
054400 BAIL-OUT-WRITE.                                                  SQ1224.2
054500     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1224.2
054600     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1224.2
054700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1224.2
054800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1224.2
054900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1224.2
055000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1224.2
055100 BAIL-OUT-EX.                                                     SQ1224.2
055200     EXIT.                                                        SQ1224.2
055300 CCVS1-EXIT.                                                      SQ1224.2
055400     EXIT.                                                        SQ1224.2
055500*                                                                 SQ1224.2
055600****************************************************************  SQ1224.2
055700*                                                              *  SQ1224.2
055800*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1224.2
055900*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1224.2
056000*                                                              *  SQ1224.2
056100****************************************************************  SQ1224.2
056200*                                                                 SQ1224.2
056300 SECT-SQ122A-0004 SECTION.                                        SQ1224.2
056400 STA-INIT.                                                        SQ1224.2
056500*                                                                 SQ1224.2
056600     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1224.2
056700     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1224.2
056800     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1224.2
056900     MOVE    125 TO XRECORD-LENGTH (1).                           SQ1224.2
057000     MOVE   "RC" TO CHARS-OR-RECORDS (1).                         SQ1224.2
057100     MOVE    2   TO XBLOCK-SIZE (1).                              SQ1224.2
057200     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ1224.2
057300     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1224.2
057400     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1224.2
057500     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1224.2
057600*                                                                 SQ1224.2
057700*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1224.2
057800*                                                                 SQ1224.2
057900 SEQ-INIT-01.                                                     SQ1224.2
058000     MOVE    0 TO REC-CT.                                         SQ1224.2
058100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
058200     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1224.2
058300     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1224.2
058400 SEQ-TEST-OP-01.                                                  SQ1224.2
058500     OPEN    OUTPUT SQ-FS4.                                       SQ1224.2
058600*                                                                 SQ1224.2
058700*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ1224.2
058800*                                                                 SQ1224.2
058900     ADD    1 TO REC-CT.                                          SQ1224.2
059000 SEQ-TEST-OP-01-01.                                               SQ1224.2
059100     IF SQ-FS4-STATUS = "00"                                      SQ1224.2
059200         PERFORM PASS                                             SQ1224.2
059300     ELSE                                                         SQ1224.2
059400         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1224.2
059500         MOVE   "00" TO CORRECT-A                                 SQ1224.2
059600         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ1224.2
059700                   TO RE-MARK                                     SQ1224.2
059800         MOVE   "VII-3, VII-23" TO ANSI-REFERENCE                 SQ1224.2
059900         PERFORM FAIL.                                            SQ1224.2
060000 SEQ-TEST-01-01-END.                                              SQ1224.2
060100*                                                                 SQ1224.2
060200*                                                                 SQ1224.2
060300*    THE FILE HAS BEEN CREATED.  WE NOW WRITE ONE RECORD TO IT.   SQ1224.2
060400*                                                                 SQ1224.2
060500 SEQ-INIT-02.                                                     SQ1224.2
060600     MOVE    0 TO REC-CT.                                         SQ1224.2
060700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
060800     ADD     1 TO XRECORD-NUMBER (1).                             SQ1224.2
060900     MOVE   "WRITE ONE RECORD"  TO FEATURE.                       SQ1224.2
061000     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1224.2
061100 SEQ-TEST-WR-02.                                                  SQ1224.2
061200     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4-FIRST.         SQ1224.2
061300     MOVE    XRECORD-NUMBER (1) TO SQ-FS4-REC-NO.                 SQ1224.2
061400     WRITE   SQ-FS4R1-F-G-125.                                    SQ1224.2
061500*                                                                 SQ1224.2
061600*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ1224.2
061700*                                                                 SQ1224.2
061800     ADD    1 TO REC-CT.                                          SQ1224.2
061900 SEQ-TEST-WR-02-01.                                               SQ1224.2
062000     IF SQ-FS4-STATUS = "00"                                      SQ1224.2
062100         PERFORM PASS                                             SQ1224.2
062200     ELSE                                                         SQ1224.2
062300         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1224.2
062400         MOVE   "00" TO CORRECT-A                                 SQ1224.2
062500         MOVE   "UNEXPECTED ERROR CODE FROM WRITE"                SQ1224.2
062600                   TO RE-MARK                                     SQ1224.2
062700         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1224.2
062800         PERFORM FAIL.                                            SQ1224.2
062900 SEQ-TEST-02-01-END.                                              SQ1224.2
063000*                                                                 SQ1224.2
063100*                                                                 SQ1224.2
063200*    HAVING WRITTEN ONE RECORD, CLOSE THE FILE.                   SQ1224.2
063300*                                                                 SQ1224.2
063400 SEQ-INIT-03.                                                     SQ1224.2
063500     MOVE    0 TO REC-CT.                                         SQ1224.2
063600     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
063700     MOVE   "CLOSE AFTER CREATE"  TO FEATURE.                     SQ1224.2
063800     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ1224.2
063900 SEQ-TEST-CL-03.                                                  SQ1224.2
064000     CLOSE   SQ-FS4.                                              SQ1224.2
064100*                                                                 SQ1224.2
064200*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ1224.2
064300*                                                                 SQ1224.2
064400     ADD    1 TO REC-CT.                                          SQ1224.2
064500 SEQ-TEST-CL-03-01.                                               SQ1224.2
064600     IF SQ-FS4-STATUS = "00"                                      SQ1224.2
064700         PERFORM PASS                                             SQ1224.2
064800     ELSE                                                         SQ1224.2
064900         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1224.2
065000         MOVE   "00" TO CORRECT-A                                 SQ1224.2
065100         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ1224.2
065200                   TO RE-MARK                                     SQ1224.2
065300         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1224.2
065400         PERFORM FAIL.                                            SQ1224.2
065500 SEQ-TEST-03-01-END.                                              SQ1224.2
065600*                                                                 SQ1224.2
065700*                                                                 SQ1224.2
065800*    CREATION OF THE FILE IS NOW COMPLETE.  THE NEXT ACTION       SQ1224.2
065900*    IS TO OPEN THE FILE IN THE INPUT MODE                        SQ1224.2
066000*                                                                 SQ1224.2
066100 SEQ-INIT-04.                                                     SQ1224.2
066200     MOVE    0 TO REC-CT.                                         SQ1224.2
066300     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1224.2
066400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
066500     MOVE   "OPEN, TO READ FILE"  TO FEATURE.                     SQ1224.2
066600     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ1224.2
066700 SEQ-TEST-OP-04.                                                  SQ1224.2
066800*                                                                 SQ1224.2
066900*    OPEN THE TEST FILE AND CLEAR THE RECORD AREA, JUST IN        SQ1224.2
067000*    CASE THERE IS A SINGLE BUFFER WHICH STILL HAS A COPY OF      SQ1224.2
067100*    THE LAST RECORD WRITTEN IN IT.                               SQ1224.2
067200*                                                                 SQ1224.2
067300     OPEN    INPUT SQ-FS4.                                        SQ1224.2
067400     MOVE    SPACE TO SQ-FS4R1-F-G-125.                           SQ1224.2
067500*                                                                 SQ1224.2
067600*    CHECK I-O STATUS RETURNED FROM OPEN INPUT                    SQ1224.2
067700*                                                                 SQ1224.2
067800     ADD    1 TO REC-CT.                                          SQ1224.2
067900 SEQ-TEST-OP-04-01.                                               SQ1224.2
068000     IF SQ-FS4-STATUS = "00"                                      SQ1224.2
068100         PERFORM PASS                                             SQ1224.2
068200     ELSE                                                         SQ1224.2
068300         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1224.2
068400         MOVE   "00" TO CORRECT-A                                 SQ1224.2
068500         MOVE   "UNEXPECTED ERROR CODE FROM OPEN INPUT"           SQ1224.2
068600                   TO RE-MARK                                     SQ1224.2
068700         MOVE   "VII-3, VII-23" TO ANSI-REFERENCE                 SQ1224.2
068800         PERFORM FAIL.                                            SQ1224.2
068900 SEQ-TEST-04-01-END.                                              SQ1224.2
069000*                                                                 SQ1224.2
069100*                                                                 SQ1224.2
069200*    READ THE FIRST (AND ONLY) RECORD FROM THE FILE               SQ1224.2
069300*                                                                 SQ1224.2
069400 SEQ-INIT-05.                                                     SQ1224.2
069500     MOVE    0 TO REC-CT.                                         SQ1224.2
069600     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
069700     MOVE   "READ FIRST RECORD"  TO FEATURE.                      SQ1224.2
069800     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ1224.2
069900 SEQ-TEST-RD-05.                                                  SQ1224.2
070000     READ    SQ-FS4.                                              SQ1224.2
070100     MOVE    SQ-FS4R1-F-G-125 TO FILE-RECORD-INFO (2).            SQ1224.2
070200*                                                                 SQ1224.2
070300*    CHECK I-O STATUS RETURNED FROM READ                          SQ1224.2
070400*                                                                 SQ1224.2
070500     ADD    1 TO REC-CT.                                          SQ1224.2
070600 SEQ-TEST-RD-05-01.                                               SQ1224.2
070700     IF SQ-FS4-STATUS = "00"                                      SQ1224.2
070800         PERFORM PASS                                             SQ1224.2
070900     ELSE                                                         SQ1224.2
071000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1224.2
071100         MOVE   "00" TO CORRECT-A                                 SQ1224.2
071200         MOVE   "UNEXPECTED I-O STATUS FROM READ"                 SQ1224.2
071300                   TO RE-MARK                                     SQ1224.2
071400         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1224.2
071500         PERFORM FAIL.                                            SQ1224.2
071600 SEQ-TEST-05-01-END.                                              SQ1224.2
071700*                                                                 SQ1224.2
071800*                                                                 SQ1224.2
071900*    READ AGAIN, TO RAISE THE AT END CONDITION                    SQ1224.2
072000*                                                                 SQ1224.2
072100 SEQ-INIT-06.                                                     SQ1224.2
072200     MOVE    0 TO REC-CT.                                         SQ1224.2
072300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
072400     MOVE   "READ, GIVING AT END"  TO FEATURE.                    SQ1224.2
072500     MOVE   "SEQ-TEST-RD-06" TO PAR-NAME.                         SQ1224.2
072600 SEQ-TEST-RD-06.                                                  SQ1224.2
072700     READ    SQ-FS4.                                              SQ1224.2
072800*                                                                 SQ1224.2
072900*    CHECK I-O STATUS RETURNED FROM READ                          SQ1224.2
073000*                                                                 SQ1224.2
073100     ADD    1 TO REC-CT.                                          SQ1224.2
073200 SEQ-TEST-RD-06-01.                                               SQ1224.2
073300     IF SQ-FS4-STATUS = "10"                                      SQ1224.2
073400         PERFORM PASS                                             SQ1224.2
073500     ELSE                                                         SQ1224.2
073600         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1224.2
073700         MOVE   "10" TO CORRECT-A                                 SQ1224.2
073800         MOVE   "UNEXPECTED I-O STATUS AT END OF FILE"            SQ1224.2
073900                   TO RE-MARK                                     SQ1224.2
074000         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1224.2
074100         PERFORM FAIL.                                            SQ1224.2
074200 SEQ-TEST-06-01-END.                                              SQ1224.2
074300*                                                                 SQ1224.2
074400*                                                                 SQ1224.2
074500*    READ AGAIN, AFTER AT END, TO RAISE I-O STATUS 46             SQ1224.2
074600*                                                                 SQ1224.2
074700 SEQ-INIT-07.                                                     SQ1224.2
074800     MOVE    0 TO REC-CT.                                         SQ1224.2
074900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1224.2
075000     MOVE   "READ AFTER AT END"  TO FEATURE.                      SQ1224.2
075100     MOVE   "SEQ-TEST-RD-07" TO PAR-NAME.                         SQ1224.2
075200 SEQ-TEST-RD-07.                                                  SQ1224.2
075300     READ    SQ-FS4.                                              SQ1224.2
075400 CCVS-EXIT SECTION.                                               SQ1224.2
075500 CCVS-999999.                                                     SQ1224.2
075600     GO TO   CLOSE-FILES.                                         SQ1224.2
      *END-OF,SQ122A                                                                  
