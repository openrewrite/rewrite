      *HEADER,COBOL,SQ148A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1484.2
000200 PROGRAM-ID.                                                      SQ1484.2
000300     SQ148A.                                                      SQ1484.2
000400****************************************************************  SQ1484.2
000500*                                                              *  SQ1484.2
000600*    VALIDATION FOR:-                                          *  SQ1484.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1484.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ1484.2
000900*                                                              *  SQ1484.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ1484.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1484.2
001200*                                                              *  SQ1484.2
001300****************************************************************  SQ1484.2
001400*                                                              *  SQ1484.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1484.2
001600*                                                              *  SQ1484.2
001700*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1484.2
001800*            X-55   SYSTEM PRINTER                             *  SQ1484.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ1484.2
002000*            X-83   OBJECT-COMPUTER                            *  SQ1484.2
002100*            X-84   LABEL RECORDS OPTION.                      *  SQ1484.2
002200*                                                              *  SQ1484.2
002300****************************************************************  SQ1484.2
002400*                                                              *  SQ1484.2
002500*    THIS PROGRAM CHECKS FOR THE CORRECT RESPONSE TO READING   *  SQ1484.2
002600*    A FILE OPEN IN THE OUTPUT MODE.  THE TEST FOR CORRECT     *  SQ1484.2
002700*    I-O STATUS CODE 47 IS IN THE DECLARATIVES. AN ABNORMAL    *  SQ1484.2
002800*    TERMINATION IS POSSIBLE AFTER THE TEST OF THE I-O STATUS  *  SQ1484.2
002900*    CODE IS ACCOMPLISHED BUT BEFORE CONTROL IS RETURNED TO    *  SQ1484.2
003000*    THE MAIN LINE CODE.                                       *  SQ1484.2
003100*                                                              *  SQ1484.2
003200****************************************************************  SQ1484.2
003300*                                                                 SQ1484.2
003400 ENVIRONMENT DIVISION.                                            SQ1484.2
003500 CONFIGURATION SECTION.                                           SQ1484.2
003600 SOURCE-COMPUTER.                                                 SQ1484.2
003700     XXXXX082.                                                    SQ1484.2
003800 OBJECT-COMPUTER.                                                 SQ1484.2
003900     XXXXX083.                                                    SQ1484.2
004000*                                                                 SQ1484.2
004100 INPUT-OUTPUT SECTION.                                            SQ1484.2
004200 FILE-CONTROL.                                                    SQ1484.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ1484.2
004400     XXXXX055.                                                    SQ1484.2
004500*                                                                 SQ1484.2
004600     SELECT SQ-FS4                                                SQ1484.2
004700            ASSIGN                                                SQ1484.2
004800     XXXXX014                                                     SQ1484.2
004900            FILE STATUS SQ-FS4-STATUS                             SQ1484.2
005000            ORGANIZATION IS SEQUENTIAL                            SQ1484.2
005100            .                                                     SQ1484.2
005200*                                                                 SQ1484.2
005300*                                                                 SQ1484.2
005400 DATA DIVISION.                                                   SQ1484.2
005500 FILE SECTION.                                                    SQ1484.2
005600 FD  PRINT-FILE                                                   SQ1484.2
005700C    LABEL RECORDS                                                SQ1484.2
005800C    XXXXX084                                                     SQ1484.2
005900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1484.2
006000               .                                                  SQ1484.2
006100 01  PRINT-REC    PICTURE X(120).                                 SQ1484.2
006200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1484.2
006300*                                                                 SQ1484.2
006400 FD  SQ-FS4                                                       SQ1484.2
006500C    LABEL RECORD IS STANDARD                                     SQ1484.2
006600     BLOCK  CONTAINS 120 CHARACTERS                               SQ1484.2
006700     RECORD CONTAINS 120 CHARACTERS                               SQ1484.2
006800                .                                                 SQ1484.2
006900 01  SQ-FS4R1-F-G-120.                                            SQ1484.2
007000        05 FFILE-RECORD-INFO-P1-120.                              SQ1484.2
007100           07 FILLER              PIC X(5).                       SQ1484.2
007200           07 FFILE-NAME          PIC X(6).                       SQ1484.2
007300           07 FILLER              PIC X(8).                       SQ1484.2
007400           07 FRECORD-NAME        PIC X(6).                       SQ1484.2
007500           07 FILLER              PIC X(1).                       SQ1484.2
007600           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ1484.2
007700           07 FILLER              PIC X(7).                       SQ1484.2
007800           07 FRECORD-NUMBER      PIC 9(6).                       SQ1484.2
007900           07 FILLER              PIC X(6).                       SQ1484.2
008000           07 FUPDATE-NUMBER      PIC 9(2).                       SQ1484.2
008100           07 FILLER              PIC X(5).                       SQ1484.2
008200           07 FODO-NUMBER         PIC 9(4).                       SQ1484.2
008300           07 FILLER              PIC X(5).                       SQ1484.2
008400           07 FPROGRAM-NAME       PIC X(5).                       SQ1484.2
008500           07 FILLER              PIC X(7).                       SQ1484.2
008600           07 FRECORD-LENGTH      PIC 9(6).                       SQ1484.2
008700           07 FILLER              PIC X(7).                       SQ1484.2
008800           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ1484.2
008900           07 FILLER              PIC X(1).                       SQ1484.2
009000           07 FBLOCK-SIZE         PIC 9(4).                       SQ1484.2
009100           07 FILLER              PIC X(6).                       SQ1484.2
009200           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ1484.2
009300           07 FILLER              PIC X(5).                       SQ1484.2
009400           07 FFILE-ORGANIZATION  PIC X(2).                       SQ1484.2
009500           07 FILLER              PIC X(6).                       SQ1484.2
009600           07 FLABEL-TYPE         PIC X(1).                       SQ1484.2
009700*                                                                 SQ1484.2
009800 WORKING-STORAGE SECTION.                                         SQ1484.2
009900*                                                                 SQ1484.2
010000***************************************************************   SQ1484.2
010100*                                                             *   SQ1484.2
010200*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1484.2
010300*                                                             *   SQ1484.2
010400***************************************************************   SQ1484.2
010500*                                                                 SQ1484.2
010600 01  STATUS-GROUP.                                                SQ1484.2
010700     04  SQ-FS4-STATUS.                                           SQ1484.2
010800         07  SQ-FS4-KEY-1   PIC X.                                SQ1484.2
010900         07  SQ-FS4-KEY-2   PIC X.                                SQ1484.2
011000*                                                                 SQ1484.2
011100***************************************************************   SQ1484.2
011200*                                                             *   SQ1484.2
011300*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1484.2
011400*                                                             *   SQ1484.2
011500***************************************************************   SQ1484.2
011600*                                                                 SQ1484.2
011700 01  REC-SKEL-SUB   PIC 99.                                       SQ1484.2
011800*                                                                 SQ1484.2
011900 01  FILE-RECORD-INFORMATION-REC.                                 SQ1484.2
012000     03 FILE-RECORD-INFO-SKELETON.                                SQ1484.2
012100        05 FILLER                 PICTURE X(48)       VALUE       SQ1484.2
012200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1484.2
012300        05 FILLER                 PICTURE X(46)       VALUE       SQ1484.2
012400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1484.2
012500        05 FILLER                 PICTURE X(26)       VALUE       SQ1484.2
012600             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1484.2
012700        05 FILLER                 PICTURE X(37)       VALUE       SQ1484.2
012800             ",RECKEY=                             ".             SQ1484.2
012900        05 FILLER                 PICTURE X(38)       VALUE       SQ1484.2
013000             ",ALTKEY1=                             ".            SQ1484.2
013100        05 FILLER                 PICTURE X(38)       VALUE       SQ1484.2
013200             ",ALTKEY2=                             ".            SQ1484.2
013300        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1484.2
013400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1484.2
013500        05 FILE-RECORD-INFO-P1-120.                               SQ1484.2
013600           07 FILLER              PIC X(5).                       SQ1484.2
013700           07 XFILE-NAME          PIC X(6).                       SQ1484.2
013800           07 FILLER              PIC X(8).                       SQ1484.2
013900           07 XRECORD-NAME        PIC X(6).                       SQ1484.2
014000           07 FILLER              PIC X(1).                       SQ1484.2
014100           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1484.2
014200           07 FILLER              PIC X(7).                       SQ1484.2
014300           07 XRECORD-NUMBER      PIC 9(6).                       SQ1484.2
014400           07 FILLER              PIC X(6).                       SQ1484.2
014500           07 UPDATE-NUMBER       PIC 9(2).                       SQ1484.2
014600           07 FILLER              PIC X(5).                       SQ1484.2
014700           07 ODO-NUMBER          PIC 9(4).                       SQ1484.2
014800           07 FILLER              PIC X(5).                       SQ1484.2
014900           07 XPROGRAM-NAME       PIC X(5).                       SQ1484.2
015000           07 FILLER              PIC X(7).                       SQ1484.2
015100           07 XRECORD-LENGTH      PIC 9(6).                       SQ1484.2
015200           07 FILLER              PIC X(7).                       SQ1484.2
015300           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1484.2
015400           07 FILLER              PIC X(1).                       SQ1484.2
015500           07 XBLOCK-SIZE         PIC 9(4).                       SQ1484.2
015600           07 FILLER              PIC X(6).                       SQ1484.2
015700           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1484.2
015800           07 FILLER              PIC X(5).                       SQ1484.2
015900           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1484.2
016000           07 FILLER              PIC X(6).                       SQ1484.2
016100           07 XLABEL-TYPE         PIC X(1).                       SQ1484.2
016200        05 FILE-RECORD-INFO-P121-240.                             SQ1484.2
016300           07 FILLER              PIC X(8).                       SQ1484.2
016400           07 XRECORD-KEY         PIC X(29).                      SQ1484.2
016500           07 FILLER              PIC X(9).                       SQ1484.2
016600           07 ALTERNATE-KEY1      PIC X(29).                      SQ1484.2
016700           07 FILLER              PIC X(9).                       SQ1484.2
016800           07 ALTERNATE-KEY2      PIC X(29).                      SQ1484.2
016900           07 FILLER              PIC X(7).                       SQ1484.2
017000*                                                                 SQ1484.2
017100 01  TEST-RESULTS.                                                SQ1484.2
017200     02 FILLER              PIC X      VALUE SPACE.               SQ1484.2
017300     02  PAR-NAME.                                                SQ1484.2
017400       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1484.2
017500       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1484.2
017600       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1484.2
017700     02 FILLER              PIC X      VALUE SPACE.               SQ1484.2
017800     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1484.2
017900     02 FILLER              PIC X      VALUE SPACE.               SQ1484.2
018000     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1484.2
018100     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1484.2
018200     02 RE-MARK             PIC X(61).                            SQ1484.2
018300 01  TEST-COMPUTED.                                               SQ1484.2
018400   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1484.2
018500   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1484.2
018600   02 COMPUTED-X.                                                 SQ1484.2
018700     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1484.2
018800     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1484.2
018900     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1484.2
019000     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1484.2
019100     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1484.2
019200     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1484.2
019300        04 COMPUTED-18V0                   PIC -9(18).            SQ1484.2
019400        04 FILLER                          PIC X.                 SQ1484.2
019500     03 FILLER PIC X(50) VALUE SPACE.                             SQ1484.2
019600 01  TEST-CORRECT.                                                SQ1484.2
019700     02 FILLER PIC X(30) VALUE SPACE.                             SQ1484.2
019800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1484.2
019900     02 CORRECT-X.                                                SQ1484.2
020000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1484.2
020100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1484.2
020200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1484.2
020300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1484.2
020400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1484.2
020500     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1484.2
020600         04 CORRECT-18V0                     PIC -9(18).          SQ1484.2
020700         04 FILLER                           PIC X.               SQ1484.2
020800     03 FILLER PIC X(2) VALUE SPACE.                              SQ1484.2
020900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1484.2
021000*                                                                 SQ1484.2
021100 01  CCVS-C-1.                                                    SQ1484.2
021200     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1484.2
021300     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1484.2
021400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1484.2
021500     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1484.2
021600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1484.2
021700     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1484.2
021800     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1484.2
021900     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1484.2
022000 01  CCVS-C-2.                                                    SQ1484.2
022100     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1484.2
022200     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1484.2
022300     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1484.2
022400     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1484.2
022500     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1484.2
022600*                                                                 SQ1484.2
022700 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1484.2
022800 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1484.2
022900 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1484.2
023000 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1484.2
023100 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1484.2
023200 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1484.2
023300 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1484.2
023400 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1484.2
023500 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1484.2
023600 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1484.2
023700 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1484.2
023800 01  CCVS-H-1.                                                    SQ1484.2
023900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1484.2
024000     02  FILLER          PIC X(42)    VALUE                       SQ1484.2
024100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1484.2
024200     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1484.2
024300 01  CCVS-H-2A.                                                   SQ1484.2
024400   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1484.2
024500   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1484.2
024600   02  FILLER            PIC XXXX   VALUE                         SQ1484.2
024700     "4.2 ".                                                      SQ1484.2
024800   02  FILLER            PIC X(28)  VALUE                         SQ1484.2
024900            " COPY - NOT FOR DISTRIBUTION".                       SQ1484.2
025000   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1484.2
025100*                                                                 SQ1484.2
025200 01  CCVS-H-2B.                                                   SQ1484.2
025300   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1484.2
025400   02  TEST-ID           PIC X(9).                                SQ1484.2
025500   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1484.2
025600   02  FILLER            PIC X(12)  VALUE                         SQ1484.2
025700     " HIGH       ".                                              SQ1484.2
025800   02  FILLER            PIC X(22)  VALUE                         SQ1484.2
025900            " LEVEL VALIDATION FOR ".                             SQ1484.2
026000   02  FILLER            PIC X(58)  VALUE                         SQ1484.2
026100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1484.2
026200 01  CCVS-H-3.                                                    SQ1484.2
026300     02  FILLER          PIC X(34)  VALUE                         SQ1484.2
026400            " FOR OFFICIAL USE ONLY    ".                         SQ1484.2
026500     02  FILLER          PIC X(58)  VALUE                         SQ1484.2
026600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1484.2
026700     02  FILLER          PIC X(28)  VALUE                         SQ1484.2
026800            "  COPYRIGHT   1985,1986 ".                           SQ1484.2
026900 01  CCVS-E-1.                                                    SQ1484.2
027000     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1484.2
027100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1484.2
027200     02 ID-AGAIN         PIC X(9).                                SQ1484.2
027300     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1484.2
027400 01  CCVS-E-2.                                                    SQ1484.2
027500     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1484.2
027600     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1484.2
027700     02  CCVS-E-2-2.                                              SQ1484.2
027800         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1484.2
027900         03 FILLER         PIC X      VALUE SPACE.                SQ1484.2
028000         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1484.2
028100            "ERRORS ENCOUNTERED".                                 SQ1484.2
028200 01  CCVS-E-3.                                                    SQ1484.2
028300     02  FILLER          PIC X(22)  VALUE                         SQ1484.2
028400            " FOR OFFICIAL USE ONLY".                             SQ1484.2
028500     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1484.2
028600     02  FILLER          PIC X(58)  VALUE                         SQ1484.2
028700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1484.2
028800     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1484.2
028900     02  FILLER          PIC X(20)  VALUE                         SQ1484.2
029000             " COPYRIGHT 1985,1986".                              SQ1484.2
029100 01  CCVS-E-4.                                                    SQ1484.2
029200     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1484.2
029300     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1484.2
029400     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1484.2
029500     02 FILLER           PIC X(40)  VALUE                         SQ1484.2
029600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1484.2
029700 01  XXINFO.                                                      SQ1484.2
029800     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1484.2
029900     02 INFO-TEXT.                                                SQ1484.2
030000       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1484.2
030100       04 XXCOMPUTED         PIC X(20).                           SQ1484.2
030200       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1484.2
030300       04 XXCORRECT          PIC X(20).                           SQ1484.2
030400     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1484.2
030500 01  HYPHEN-LINE.                                                 SQ1484.2
030600     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1484.2
030700     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1484.2
030800-    "*****************************************".                 SQ1484.2
030900     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1484.2
031000-    "******************************".                            SQ1484.2
031100 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1484.2
031200     "SQ148A".                                                    SQ1484.2
031300*                                                                 SQ1484.2
031400*                                                                 SQ1484.2
031500 PROCEDURE DIVISION.                                              SQ1484.2
031600 DECLARATIVES.                                                    SQ1484.2
031700*                                                                 SQ1484.2
031800 SQ148A-DECLARATIVE-001-SECT SECTION.                             SQ1484.2
031900     USE AFTER STANDARD EXCEPTION PROCEDURE OUTPUT.               SQ1484.2
032000 READ-ERROR-PROCESS.                                              SQ1484.2
032100     IF  SQ-FS4-STATUS = "47"                                     SQ1484.2
032200             PERFORM DECL-PASS                                    SQ1484.2
032300             GO TO DECL-ABNORMAL-TERM                             SQ1484.2
032400     ELSE                                                         SQ1484.2
032500             MOVE "47" TO CORRECT-A                               SQ1484.2
032600             MOVE SQ-FS4-STATUS TO COMPUTED-A                     SQ1484.2
032700             MOVE "STATUS FOR READ OF FILE OPEN OUTPUT INCORRECT" SQ1484.2
032800                     TO RE-MARK                                   SQ1484.2
032900             MOVE "VII-5, 1.3.5(4)F" TO ANSI-REFERENCE            SQ1484.2
033000             PERFORM DECL-FAIL                                    SQ1484.2
033100             GO TO DECL-ABNORMAL-TERM                             SQ1484.2
033200     END-IF.                                                      SQ1484.2
033300*                                                                 SQ1484.2
033400 DECL-PASS.                                                       SQ1484.2
033500     MOVE   "PASS " TO P-OR-F.                                    SQ1484.2
033600     ADD     1 TO PASS-COUNTER.                                   SQ1484.2
033700     PERFORM DECL-PRINT-DETAIL.                                   SQ1484.2
033800*                                                                 SQ1484.2
033900 DECL-FAIL.                                                       SQ1484.2
034000     MOVE   "FAIL*" TO P-OR-F.                                    SQ1484.2
034100     ADD     1 TO ERROR-COUNTER.                                  SQ1484.2
034200     PERFORM DECL-PRINT-DETAIL.                                   SQ1484.2
034300*                                                                 SQ1484.2
034400 DECL-DE-LETE.                                                    SQ1484.2
034500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1484.2
034600     MOVE   "*****" TO P-OR-F.                                    SQ1484.2
034700     ADD     1 TO DELETE-COUNTER.                                 SQ1484.2
034800     PERFORM DECL-PRINT-DETAIL.                                   SQ1484.2
034900*                                                                 SQ1484.2
035000 DECL-PRINT-DETAIL.                                               SQ1484.2
035100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1484.2
035200             MOVE "." TO PARDOT-X                                 SQ1484.2
035300             MOVE REC-CT TO DOTVALUE.                             SQ1484.2
035400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1484.2
035500     PERFORM DECL-WRITE-LINE.                                     SQ1484.2
035600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1484.2
035700         PERFORM DECL-WRITE-LINE                                  SQ1484.2
035800         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1484.2
035900     ELSE                                                         SQ1484.2
036000         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1484.2
036100     MOVE    SPACE TO P-OR-F.                                     SQ1484.2
036200     MOVE    SPACE TO COMPUTED-X.                                 SQ1484.2
036300     MOVE    SPACE TO CORRECT-X.                                  SQ1484.2
036400     IF REC-CT EQUAL TO ZERO                                      SQ1484.2
036500         MOVE    SPACE TO PAR-NAME.                               SQ1484.2
036600     MOVE    SPACE TO RE-MARK.                                    SQ1484.2
036700*                                                                 SQ1484.2
036800 DECL-WRITE-LINE.                                                 SQ1484.2
036900     ADD     1 TO RECORD-COUNT.                                   SQ1484.2
037000*    IF RECORD-COUNT GREATER 50                                   SQ1484.2
037100*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1484.2
037200*        MOVE    SPACE TO DUMMY-RECORD                            SQ1484.2
037300*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1484.2
037400*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1484.2
037500*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1484.2
037600*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1484.2
037700*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1484.2
037800*        PERFORM DECL-WRT-LN                                      SQ1484.2
037900*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1484.2
038000*        MOVE    ZERO TO RECORD-COUNT.                            SQ1484.2
038100     PERFORM DECL-WRT-LN.                                         SQ1484.2
038200*                                                                 SQ1484.2
038300 DECL-WRT-LN.                                                     SQ1484.2
038400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1484.2
038500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1484.2
038600*                                                                 SQ1484.2
038700 DECL-FAIL-ROUTINE.                                               SQ1484.2
038800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1484.2
038900     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1484.2
039000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1484.2
039100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1484.2
039200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1484.2
039300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1484.2
039400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1484.2
039500     GO TO   DECL-FAIL-EX.                                        SQ1484.2
039600 DECL-FAIL-WRITE.                                                 SQ1484.2
039700     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1484.2
039800     PERFORM DECL-WRITE-LINE                                      SQ1484.2
039900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1484.2
040000     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1484.2
040100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1484.2
040200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1484.2
040300 DECL-FAIL-EX.                                                    SQ1484.2
040400     EXIT.                                                        SQ1484.2
040500*                                                                 SQ1484.2
040600 DECL-BAIL.                                                       SQ1484.2
040700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1484.2
040800     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1484.2
040900 DECL-BAIL-WRITE.                                                 SQ1484.2
041000     MOVE    CORRECT-A TO XXCORRECT.                              SQ1484.2
041100     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1484.2
041200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1484.2
041300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1484.2
041400     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1484.2
041500     MOVE    SPACE TO INF-ANSI-REFERENCE.                         SQ1484.2
041600 DECL-BAIL-EX.                                                    SQ1484.2
041700     EXIT.                                                        SQ1484.2
041800*                                                                 SQ1484.2
041900 DECL-ABNORMAL-TERM.                                              SQ1484.2
042000     MOVE SPACE TO DUMMY-RECORD.                                  SQ1484.2
042100     PERFORM DECL-WRITE-LINE.                                     SQ1484.2
042200     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ1484.2
042300             TO DUMMY-RECORD.                                     SQ1484.2
042400     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1484.2
042500*                                                                 SQ1484.2
042600 END-DECLS.                                                       SQ1484.2
042700     EXIT.                                                        SQ1484.2
042800 END DECLARATIVES.                                                SQ1484.2
042900*                                                                 SQ1484.2
043000*                                                                 SQ1484.2
043100 CCVS1 SECTION.                                                   SQ1484.2
043200 OPEN-FILES.                                                      SQ1484.2
043300     OPEN    OUTPUT PRINT-FILE.                                   SQ1484.2
043400     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1484.2
043500     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1484.2
043600     MOVE    SPACE TO TEST-RESULTS.                               SQ1484.2
043700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1484.2
043800     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1484.2
043900     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1484.2
044000     GO TO CCVS1-EXIT.                                            SQ1484.2
044100*                                                                 SQ1484.2
044200 CCVS-INIT-FILE.                                                  SQ1484.2
044300     ADD     1 TO REC-SKL-SUB.                                    SQ1484.2
044400     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1484.2
044500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1484.2
044600*                                                                 SQ1484.2
044700 CLOSE-FILES.                                                     SQ1484.2
044800     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1484.2
044900     CLOSE   PRINT-FILE.                                          SQ1484.2
045000 TERMINATE-CCVS.                                                  SQ1484.2
045100     STOP    RUN.                                                 SQ1484.2
045200*                                                                 SQ1484.2
045300 INSPT.                                                           SQ1484.2
045400     MOVE   "INSPT" TO P-OR-F.                                    SQ1484.2
045500     ADD     1 TO INSPECT-COUNTER.                                SQ1484.2
045600     PERFORM PRINT-DETAIL.                                        SQ1484.2
045700*                                                                 SQ1484.2
045800 PASS.                                                            SQ1484.2
045900     MOVE   "PASS " TO P-OR-F.                                    SQ1484.2
046000     ADD     1 TO PASS-COUNTER.                                   SQ1484.2
046100     PERFORM PRINT-DETAIL.                                        SQ1484.2
046200*                                                                 SQ1484.2
046300 FAIL.                                                            SQ1484.2
046400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1484.2
046500     ADD     1 TO ERROR-COUNTER.                                  SQ1484.2
046600     PERFORM PRINT-DETAIL.                                        SQ1484.2
046700*                                                                 SQ1484.2
046800 DE-LETE.                                                         SQ1484.2
046900     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1484.2
047000     MOVE   "*****" TO P-OR-F.                                    SQ1484.2
047100     ADD     1 TO DELETE-COUNTER.                                 SQ1484.2
047200     PERFORM PRINT-DETAIL.                                        SQ1484.2
047300*                                                                 SQ1484.2
047400 PRINT-DETAIL.                                                    SQ1484.2
047500     IF REC-CT NOT EQUAL TO ZERO                                  SQ1484.2
047600         MOVE   "." TO PARDOT-X                                   SQ1484.2
047700         MOVE    REC-CT TO DOTVALUE.                              SQ1484.2
047800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1484.2
047900     PERFORM WRITE-LINE.                                          SQ1484.2
048000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1484.2
048100         PERFORM WRITE-LINE                                       SQ1484.2
048200         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1484.2
048300     ELSE                                                         SQ1484.2
048400         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1484.2
048500     MOVE    SPACE TO P-OR-F.                                     SQ1484.2
048600     MOVE    SPACE TO COMPUTED-X.                                 SQ1484.2
048700     MOVE    SPACE TO CORRECT-X.                                  SQ1484.2
048800     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1484.2
048900     MOVE    SPACE TO RE-MARK.                                    SQ1484.2
049000*                                                                 SQ1484.2
049100 HEAD-ROUTINE.                                                    SQ1484.2
049200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1484.2
049300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1484.2
049400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1484.2
049500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1484.2
049600 COLUMN-NAMES-ROUTINE.                                            SQ1484.2
049700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1484.2
049800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1484.2
049900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1484.2
050000 END-ROUTINE.                                                     SQ1484.2
050100     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1484.2
050200     PERFORM WRITE-LINE 5 TIMES.                                  SQ1484.2
050300 END-RTN-EXIT.                                                    SQ1484.2
050400     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1484.2
050500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1484.2
050600*                                                                 SQ1484.2
050700 END-ROUTINE-1.                                                   SQ1484.2
050800     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1484.2
050900     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1484.2
051000     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1484.2
051100     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1484.2
051200     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1484.2
051300     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1484.2
051400     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1484.2
051500     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1484.2
051600     PERFORM WRITE-LINE.                                          SQ1484.2
051700     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1484.2
051800     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1484.2
051900         MOVE   "NO " TO ERROR-TOTAL                              SQ1484.2
052000     ELSE                                                         SQ1484.2
052100         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1484.2
052200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1484.2
052300     PERFORM WRITE-LINE.                                          SQ1484.2
052400 END-ROUTINE-13.                                                  SQ1484.2
052500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1484.2
052600         MOVE   "NO " TO ERROR-TOTAL                              SQ1484.2
052700     ELSE                                                         SQ1484.2
052800         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1484.2
052900     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1484.2
053000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1484.2
053100     PERFORM WRITE-LINE.                                          SQ1484.2
053200     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1484.2
053300         MOVE   "NO " TO ERROR-TOTAL                              SQ1484.2
053400     ELSE                                                         SQ1484.2
053500         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1484.2
053600     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1484.2
053700     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1484.2
053800     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1484.2
053900*                                                                 SQ1484.2
054000 WRITE-LINE.                                                      SQ1484.2
054100     ADD     1 TO RECORD-COUNT.                                   SQ1484.2
054200*    IF RECORD-COUNT GREATER 50                                   SQ1484.2
054300*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1484.2
054400*        MOVE  SPACE TO DUMMY-RECORD                              SQ1484.2
054500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1484.2
054600*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1484.2
054700*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1484.2
054800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1484.2
054900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1484.2
055000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1484.2
055100     PERFORM WRT-LN.                                              SQ1484.2
055200*                                                                 SQ1484.2
055300 WRT-LN.                                                          SQ1484.2
055400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1484.2
055500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1484.2
055600 BLANK-LINE-PRINT.                                                SQ1484.2
055700     PERFORM WRT-LN.                                              SQ1484.2
055800 FAIL-ROUTINE.                                                    SQ1484.2
055900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1484.2
056000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1484.2
056100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1484.2
056200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1484.2
056300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1484.2
056400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1484.2
056500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1484.2
056600     GO TO   FAIL-ROUTINE-EX.                                     SQ1484.2
056700 FAIL-ROUTINE-WRITE.                                              SQ1484.2
056800     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1484.2
056900     PERFORM WRITE-LINE                                           SQ1484.2
057000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1484.2
057100     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1484.2
057200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1484.2
057300     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1484.2
057400 FAIL-ROUTINE-EX.                                                 SQ1484.2
057500     EXIT.                                                        SQ1484.2
057600 BAIL-OUT.                                                        SQ1484.2
057700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1484.2
057800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1484.2
057900 BAIL-OUT-WRITE.                                                  SQ1484.2
058000     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1484.2
058100     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1484.2
058200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1484.2
058300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1484.2
058400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1484.2
058500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1484.2
058600 BAIL-OUT-EX.                                                     SQ1484.2
058700     EXIT.                                                        SQ1484.2
058800 CCVS1-EXIT.                                                      SQ1484.2
058900     EXIT.                                                        SQ1484.2
059000*                                                                 SQ1484.2
059100****************************************************************  SQ1484.2
059200*                                                              *  SQ1484.2
059300*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1484.2
059400*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1484.2
059500*                                                              *  SQ1484.2
059600****************************************************************  SQ1484.2
059700*                                                                 SQ1484.2
059800 SECT-SQ148A-0002 SECTION.                                        SQ1484.2
059900 STA-INIT.                                                        SQ1484.2
060000*                                                                 SQ1484.2
060100     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1484.2
060200     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1484.2
060300     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1484.2
060400     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1484.2
060500     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ1484.2
060600     MOVE    1   TO XBLOCK-SIZE (1).                              SQ1484.2
060700     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ1484.2
060800     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1484.2
060900     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1484.2
061000*                                                                 SQ1484.2
061100*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1484.2
061200*                                                                 SQ1484.2
061300 SEQ-INIT-01.                                                     SQ1484.2
061400     MOVE    1 TO REC-CT.                                         SQ1484.2
061500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1484.2
061600     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1484.2
061700     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1484.2
061800     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1484.2
061900 SEQ-TEST-OP-01.                                                  SQ1484.2
062000     OPEN    OUTPUT SQ-FS4.                                       SQ1484.2
062100*                                                                 SQ1484.2
062200*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ1484.2
062300*                                                                 SQ1484.2
062400 SEQ-TEST-OP-01-01.                                               SQ1484.2
062500     IF SQ-FS4-STATUS = "00"                                      SQ1484.2
062600         PERFORM PASS                                             SQ1484.2
062700     ELSE                                                         SQ1484.2
062800         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1484.2
062900         MOVE   "00" TO CORRECT-A                                 SQ1484.2
063000         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ1484.2
063100                   TO RE-MARK                                     SQ1484.2
063200         MOVE   "VII-3, VII-43" TO ANSI-REFERENCE                 SQ1484.2
063300         PERFORM FAIL.                                            SQ1484.2
063400 SEQ-TEST-01-01-END.                                              SQ1484.2
063500*                                                                 SQ1484.2
063600*                                                                 SQ1484.2
063700*    A NEW FILE IS OPEN.  WE NOW ATTEMPT TO READ A RECORD.        SQ1484.2
063800*                                                                 SQ1484.2
063900 SEQ-INIT-02.                                                     SQ1484.2
064000     MOVE    1 TO REC-CT.                                         SQ1484.2
064100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1484.2
064200     MOVE   "READ IN OUTPUT MODE" TO FEATURE.                     SQ1484.2
064300     MOVE   "SEQ-TEST-RD-02" TO PAR-NAME.                         SQ1484.2
064400 SEQ-TEST-RD-02.                                                  SQ1484.2
064500     READ    SQ-FS4.                                              SQ1484.2
064600*                                                                 SQ1484.2
064700 CLOSE-TEST-03.                                                   SQ1484.2
064800     CLOSE SQ-FS4.                                                SQ1484.2
064900*                                                                 SQ1484.2
065000 CCVS-EXIT SECTION.                                               SQ1484.2
065100 CCVS-999999.                                                     SQ1484.2
065200     GO TO   CLOSE-FILES.                                         SQ1484.2
      *END-OF,SQ148A                                                                  
