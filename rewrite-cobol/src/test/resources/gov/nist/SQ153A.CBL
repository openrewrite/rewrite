      *HEADER,COBOL,SQ153A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1534.2
000200 PROGRAM-ID.                                                      SQ1534.2
000300     SQ153A.                                                      SQ1534.2
000400****************************************************************  SQ1534.2
000500*                                                              *  SQ1534.2
000600*    VALIDATION FOR:-                                          *  SQ1534.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1534.2
000800*    USING CCVS85 VERSION 3.0.                                 *  SQ1534.2
000900*                                                              *  SQ1534.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ1534.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1534.2
001200*                                                              *  SQ1534.2
001300****************************************************************  SQ1534.2
001400*                                                              *  SQ1534.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1534.2
001600*                                                              *  SQ1534.2
001700*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1534.2
001800*            X-55   SYSTEM PRINTER                             *  SQ1534.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ1534.2
002000*            X-83   OBJECT-COMPUTER                            *  SQ1534.2
002100*            X-84   LABEL RECORDS OPTION.                      *  SQ1534.2
002200*                                                              *  SQ1534.2
002300****************************************************************  SQ1534.2
002400*                                                              *  SQ1534.2
002500*    THIS PROGRAM CHECKS FOR THE CORRECT RESPONSE TO WRITING TO*  SQ1534.2
002600*    A FILE OPEN IN THE I-O MODE.  THE TEST FOR CORRECT I-O    *  SQ1534.2
002700*    STATUS 48 IS IN THE DECLARATIVES.  AN ABNORMAL TERMINATION*  SQ1534.2
002800*    IS POSSIBLE AFTER THE TEST OF THE I-O STATUS CODE IS      *  SQ1534.2
002900*    ACCOMPLISHED BUT BEFORE CONTROL IS RETURNED TO THE MAIN   *  SQ1534.2
003000*    LINE CODE.                                                *  SQ1534.2
003100*                                                              *  SQ1534.2
003200****************************************************************  SQ1534.2
003300*                                                                 SQ1534.2
003400 ENVIRONMENT DIVISION.                                            SQ1534.2
003500 CONFIGURATION SECTION.                                           SQ1534.2
003600 SOURCE-COMPUTER.                                                 SQ1534.2
003700     XXXXX082.                                                    SQ1534.2
003800 OBJECT-COMPUTER.                                                 SQ1534.2
003900     XXXXX083.                                                    SQ1534.2
004000*                                                                 SQ1534.2
004100 INPUT-OUTPUT SECTION.                                            SQ1534.2
004200 FILE-CONTROL.                                                    SQ1534.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ1534.2
004400     XXXXX055.                                                    SQ1534.2
004500*                                                                 SQ1534.2
004600     SELECT SQ-FS4                                                SQ1534.2
004700            ASSIGN                                                SQ1534.2
004800     XXXXX014                                                     SQ1534.2
004900            FILE STATUS SQ-FS4-STATUS                             SQ1534.2
005000            ORGANIZATION IS SEQUENTIAL                            SQ1534.2
005100            .                                                     SQ1534.2
005200*                                                                 SQ1534.2
005300*                                                                 SQ1534.2
005400 DATA DIVISION.                                                   SQ1534.2
005500 FILE SECTION.                                                    SQ1534.2
005600 FD  PRINT-FILE                                                   SQ1534.2
005700C    LABEL RECORDS                                                SQ1534.2
005800C    XXXXX084                                                     SQ1534.2
005900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1534.2
006000               .                                                  SQ1534.2
006100 01  PRINT-REC    PICTURE X(120).                                 SQ1534.2
006200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1534.2
006300*                                                                 SQ1534.2
006400 FD  SQ-FS4                                                       SQ1534.2
006500C    LABEL RECORD IS STANDARD                                     SQ1534.2
006600     BLOCK  CONTAINS 120 CHARACTERS                               SQ1534.2
006700     RECORD CONTAINS 120 CHARACTERS                               SQ1534.2
006800                .                                                 SQ1534.2
006900 01  SQ-FS4R1-F-G-120.                                            SQ1534.2
007000        05 FFILE-RECORD-INFO-P1-120    PICTURE X(120).            SQ1534.2
007100*                                                                 SQ1534.2
007200 WORKING-STORAGE SECTION.                                         SQ1534.2
007300*                                                                 SQ1534.2
007400***************************************************************   SQ1534.2
007500*                                                             *   SQ1534.2
007600*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1534.2
007700*                                                             *   SQ1534.2
007800***************************************************************   SQ1534.2
007900*                                                                 SQ1534.2
008000 01  STATUS-GROUP.                                                SQ1534.2
008100     04  SQ-FS4-STATUS    PICTURE XX.                             SQ1534.2
008200*                                                                 SQ1534.2
008300***************************************************************   SQ1534.2
008400*                                                             *   SQ1534.2
008500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1534.2
008600*                                                             *   SQ1534.2
008700***************************************************************   SQ1534.2
008800*                                                                 SQ1534.2
008900 01  FILE-RECORD-INFORMATION-REC.                                 SQ1534.2
009000     03 FILE-RECORD-INFO-SKELETON.                                SQ1534.2
009100        05 FILLER                 PICTURE X(48)       VALUE       SQ1534.2
009200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1534.2
009300        05 FILLER                 PICTURE X(46)       VALUE       SQ1534.2
009400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1534.2
009500        05 FILLER                 PICTURE X(26)       VALUE       SQ1534.2
009600             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1534.2
009700        05 FILLER                 PICTURE X(37)       VALUE       SQ1534.2
009800             ",RECKEY=                             ".             SQ1534.2
009900        05 FILLER                 PICTURE X(38)       VALUE       SQ1534.2
010000             ",ALTKEY1=                             ".            SQ1534.2
010100        05 FILLER                 PICTURE X(38)       VALUE       SQ1534.2
010200             ",ALTKEY2=                             ".            SQ1534.2
010300        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1534.2
010400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1534.2
010500        05 FILE-RECORD-INFO-P1-120.                               SQ1534.2
010600           07 FILLER              PIC X(5).                       SQ1534.2
010700           07 XFILE-NAME          PIC X(6).                       SQ1534.2
010800           07 FILLER              PIC X(8).                       SQ1534.2
010900           07 XRECORD-NAME        PIC X(6).                       SQ1534.2
011000           07 FILLER              PIC X(1).                       SQ1534.2
011100           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1534.2
011200           07 FILLER              PIC X(7).                       SQ1534.2
011300           07 XRECORD-NUMBER      PIC 9(6).                       SQ1534.2
011400           07 FILLER              PIC X(6).                       SQ1534.2
011500           07 UPDATE-NUMBER       PIC 9(2).                       SQ1534.2
011600           07 FILLER              PIC X(5).                       SQ1534.2
011700           07 ODO-NUMBER          PIC 9(4).                       SQ1534.2
011800           07 FILLER              PIC X(5).                       SQ1534.2
011900           07 XPROGRAM-NAME       PIC X(5).                       SQ1534.2
012000           07 FILLER              PIC X(7).                       SQ1534.2
012100           07 XRECORD-LENGTH      PIC 9(6).                       SQ1534.2
012200           07 FILLER              PIC X(7).                       SQ1534.2
012300           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1534.2
012400           07 FILLER              PIC X(1).                       SQ1534.2
012500           07 XBLOCK-SIZE         PIC 9(4).                       SQ1534.2
012600           07 FILLER              PIC X(6).                       SQ1534.2
012700           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1534.2
012800           07 FILLER              PIC X(5).                       SQ1534.2
012900           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1534.2
013000           07 FILLER              PIC X(6).                       SQ1534.2
013100           07 XLABEL-TYPE         PIC X(1).                       SQ1534.2
013200        05 FILE-RECORD-INFO-P121-240.                             SQ1534.2
013300           07 FILLER              PIC X(8).                       SQ1534.2
013400           07 XRECORD-KEY         PIC X(29).                      SQ1534.2
013500           07 FILLER              PIC X(9).                       SQ1534.2
013600           07 ALTERNATE-KEY1      PIC X(29).                      SQ1534.2
013700           07 FILLER              PIC X(9).                       SQ1534.2
013800           07 ALTERNATE-KEY2      PIC X(29).                      SQ1534.2
013900           07 FILLER              PIC X(7).                       SQ1534.2
014000*                                                                 SQ1534.2
014100 01  TEST-RESULTS.                                                SQ1534.2
014200     02 FILLER              PIC X      VALUE SPACE.               SQ1534.2
014300     02  PAR-NAME.                                                SQ1534.2
014400       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1534.2
014500       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1534.2
014600       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1534.2
014700     02 FILLER              PIC X      VALUE SPACE.               SQ1534.2
014800     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1534.2
014900     02 FILLER              PIC X      VALUE SPACE.               SQ1534.2
015000     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1534.2
015100     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1534.2
015200     02 RE-MARK             PIC X(61).                            SQ1534.2
015300 01  TEST-COMPUTED.                                               SQ1534.2
015400   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1534.2
015500   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1534.2
015600   02 COMPUTED-X.                                                 SQ1534.2
015700     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1534.2
015800     03 FILLER PIC X(50) VALUE SPACE.                             SQ1534.2
015900 01  TEST-CORRECT.                                                SQ1534.2
016000     02 FILLER PIC X(30) VALUE SPACE.                             SQ1534.2
016100     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1534.2
016200     02 CORRECT-X.                                                SQ1534.2
016300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1534.2
016400     03 FILLER PIC X(2) VALUE SPACE.                              SQ1534.2
016500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1534.2
016600*                                                                 SQ1534.2
016700 01  CCVS-C-1.                                                    SQ1534.2
016800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1534.2
016900     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1534.2
017000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1534.2
017100     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1534.2
017200     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1534.2
017300     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1534.2
017400     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1534.2
017500     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1534.2
017600 01  CCVS-C-2.                                                    SQ1534.2
017700     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1534.2
017800     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1534.2
017900     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1534.2
018000     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1534.2
018100     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1534.2
018200*                                                                 SQ1534.2
018300 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1534.2
018400 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1534.2
018500 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1534.2
018600 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1534.2
018700 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1534.2
018800 01  DUMMY-HOLD        PIC X(120)    VALUE SPACE.                 SQ1534.2
018900 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1534.2
019000 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1534.2
019100 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1534.2
019200 01  CCVS-H-1.                                                    SQ1534.2
019300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1534.2
019400     02  FILLER          PIC X(42)    VALUE                       SQ1534.2
019500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1534.2
019600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1534.2
019700 01  CCVS-H-2A.                                                   SQ1534.2
019800   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1534.2
019900   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1534.2
020000   02  FILLER            PIC XXXX   VALUE                         SQ1534.2
020100     "4.2 ".                                                      SQ1534.2
020200   02  FILLER            PIC X(28)  VALUE                         SQ1534.2
020300            " COPY - NOT FOR DISTRIBUTION".                       SQ1534.2
020400   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1534.2
020500*                                                                 SQ1534.2
020600 01  CCVS-H-2B.                                                   SQ1534.2
020700   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1534.2
020800   02  TEST-ID           PIC X(9).                                SQ1534.2
020900   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1534.2
021000   02  FILLER            PIC X(12)  VALUE                         SQ1534.2
021100     " HIGH       ".                                              SQ1534.2
021200   02  FILLER            PIC X(22)  VALUE                         SQ1534.2
021300            " LEVEL VALIDATION FOR ".                             SQ1534.2
021400   02  FILLER            PIC X(58)  VALUE                         SQ1534.2
021500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1534.2
021600 01  CCVS-H-3.                                                    SQ1534.2
021700     02  FILLER          PIC X(34)  VALUE                         SQ1534.2
021800            " FOR OFFICIAL USE ONLY    ".                         SQ1534.2
021900     02  FILLER          PIC X(58)  VALUE                         SQ1534.2
022000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1534.2
022100     02  FILLER          PIC X(28)  VALUE                         SQ1534.2
022200            "  COPYRIGHT   1985,1986 ".                           SQ1534.2
022300 01  CCVS-E-1.                                                    SQ1534.2
022400     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1534.2
022500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1534.2
022600     02 ID-AGAIN         PIC X(9).                                SQ1534.2
022700     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1534.2
022800 01  CCVS-E-2.                                                    SQ1534.2
022900     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1534.2
023000     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1534.2
023100     02  CCVS-E-2-2.                                              SQ1534.2
023200         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1534.2
023300         03 FILLER         PIC X      VALUE SPACE.                SQ1534.2
023400         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1534.2
023500            "ERRORS ENCOUNTERED".                                 SQ1534.2
023600 01  CCVS-E-3.                                                    SQ1534.2
023700     02  FILLER          PIC X(22)  VALUE                         SQ1534.2
023800            " FOR OFFICIAL USE ONLY".                             SQ1534.2
023900     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1534.2
024000     02  FILLER          PIC X(58)  VALUE                         SQ1534.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1534.2
024200     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1534.2
024300     02  FILLER          PIC X(20)  VALUE                         SQ1534.2
024400             " COPYRIGHT 1985,1986".                              SQ1534.2
024500 01  CCVS-E-4.                                                    SQ1534.2
024600     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1534.2
024700     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1534.2
024800     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1534.2
024900     02 FILLER           PIC X(40)  VALUE                         SQ1534.2
025000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1534.2
025100 01  XXINFO.                                                      SQ1534.2
025200     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1534.2
025300     02 INFO-TEXT.                                                SQ1534.2
025400       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1534.2
025500       04 XXCOMPUTED         PIC X(20).                           SQ1534.2
025600       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1534.2
025700       04 XXCORRECT          PIC X(20).                           SQ1534.2
025800     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1534.2
025900 01  HYPHEN-LINE.                                                 SQ1534.2
026000     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1534.2
026100     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1534.2
026200-    "*****************************************".                 SQ1534.2
026300     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1534.2
026400-    "******************************".                            SQ1534.2
026500 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1534.2
026600     "SQ153A".                                                    SQ1534.2
026700*                                                                 SQ1534.2
026800*                                                                 SQ1534.2
026900 PROCEDURE DIVISION.                                              SQ1534.2
027000 DECLARATIVES.                                                    SQ1534.2
027100*                                                                 SQ1534.2
027200 SECT-SQ153A-0001 SECTION.                                        SQ1534.2
027300     USE AFTER STANDARD EXCEPTION PROCEDURE I-O.                  SQ1534.2
027400 O-ERROR-PROCESS.                                                 SQ1534.2
027500     IF SQ-FS4-STATUS = "48"                                      SQ1534.2
027600             PERFORM DECL-PASS                                    SQ1534.2
027700             GO TO ABNORMAL-TERM-DECL                             SQ1534.2
027800     ELSE                                                         SQ1534.2
027900             MOVE "48" TO CORRECT-A                               SQ1534.2
028000             MOVE SQ-FS4-STATUS TO COMPUTED-A                     SQ1534.2
028100             MOVE "STATUS FOR WRITE OF FILE OPEN I-O INCORRECT"   SQ1534.2
028200                     TO RE-MARK                                   SQ1534.2
028300             MOVE "VII-5, 1.3.5(4)G" TO ANSI-REFERENCE            SQ1534.2
028400             PERFORM DECL-FAIL                                    SQ1534.2
028500             GO TO ABNORMAL-TERM-DECL                             SQ1534.2
028600     END-IF.                                                      SQ1534.2
028700*                                                                 SQ1534.2
028800*                                                                 SQ1534.2
028900 DECL-PASS.                                                       SQ1534.2
029000     MOVE   "PASS " TO P-OR-F.                                    SQ1534.2
029100     ADD     1 TO PASS-COUNTER.                                   SQ1534.2
029200     PERFORM DECL-PRINT-DETAIL.                                   SQ1534.2
029300*                                                                 SQ1534.2
029400 DECL-FAIL.                                                       SQ1534.2
029500     MOVE   "FAIL*" TO P-OR-F.                                    SQ1534.2
029600     ADD     1 TO ERROR-COUNTER.                                  SQ1534.2
029700     PERFORM DECL-PRINT-DETAIL.                                   SQ1534.2
029800*                                                                 SQ1534.2
029900 DECL-DE-LETE.                                                    SQ1534.2
030000     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1534.2
030100     MOVE   "*****" TO P-OR-F.                                    SQ1534.2
030200     ADD     1 TO DELETE-COUNTER.                                 SQ1534.2
030300     PERFORM DECL-PRINT-DETAIL.                                   SQ1534.2
030400*                                                                 SQ1534.2
030500 DECL-PRINT-DETAIL.                                               SQ1534.2
030600     IF REC-CT NOT EQUAL TO ZERO                                  SQ1534.2
030700             MOVE "." TO PARDOT-X                                 SQ1534.2
030800             MOVE REC-CT TO DOTVALUE.                             SQ1534.2
030900     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1534.2
031000     PERFORM DECL-WRITE-LINE.                                     SQ1534.2
031100     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1534.2
031200         PERFORM DECL-WRITE-LINE                                  SQ1534.2
031300         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1534.2
031400     ELSE                                                         SQ1534.2
031500         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1534.2
031600     MOVE    SPACE TO P-OR-F.                                     SQ1534.2
031700     MOVE    SPACE TO COMPUTED-X.                                 SQ1534.2
031800     MOVE    SPACE TO CORRECT-X.                                  SQ1534.2
031900     IF REC-CT EQUAL TO ZERO                                      SQ1534.2
032000         MOVE    SPACE TO PAR-NAME.                               SQ1534.2
032100     MOVE    SPACE TO RE-MARK.                                    SQ1534.2
032200*                                                                 SQ1534.2
032300 DECL-WRITE-LINE.                                                 SQ1534.2
032400     ADD     1 TO RECORD-COUNT.                                   SQ1534.2
032500*    IF RECORD-COUNT GREATER 50                                   SQ1534.2
032600*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1534.2
032700*        MOVE    SPACE TO DUMMY-RECORD                            SQ1534.2
032800*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1534.2
032900*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1534.2
033000*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1534.2
033100*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1534.2
033200*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1534.2
033300*        PERFORM DECL-WRT-LN                                      SQ1534.2
033400*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1534.2
033500*        MOVE    ZERO TO RECORD-COUNT.                            SQ1534.2
033600     PERFORM DECL-WRT-LN.                                         SQ1534.2
033700*                                                                 SQ1534.2
033800 DECL-WRT-LN.                                                     SQ1534.2
033900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1534.2
034000     MOVE    SPACE TO DUMMY-RECORD.                               SQ1534.2
034100*                                                                 SQ1534.2
034200 DECL-FAIL-ROUTINE.                                               SQ1534.2
034300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1534.2
034400     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1534.2
034500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1534.2
034600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1534.2
034700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1534.2
034800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1534.2
034900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1534.2
035000     GO TO   DECL-FAIL-EX.                                        SQ1534.2
035100 DECL-FAIL-WRITE.                                                 SQ1534.2
035200     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1534.2
035300     PERFORM DECL-WRITE-LINE                                      SQ1534.2
035400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1534.2
035500     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1534.2
035600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1534.2
035700     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1534.2
035800 DECL-FAIL-EX.                                                    SQ1534.2
035900     EXIT.                                                        SQ1534.2
036000*                                                                 SQ1534.2
036100 DECL-BAIL.                                                       SQ1534.2
036200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1534.2
036300     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1534.2
036400 DECL-BAIL-WRITE.                                                 SQ1534.2
036500     MOVE    CORRECT-A TO XXCORRECT.                              SQ1534.2
036600     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1534.2
036700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1534.2
036800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1534.2
036900 DECL-BAIL-EX.                                                    SQ1534.2
037000     EXIT.                                                        SQ1534.2
037100*                                                                 SQ1534.2
037200 ABNORMAL-TERM-DECL.                                              SQ1534.2
037300     MOVE SPACE TO DUMMY-RECORD.                                  SQ1534.2
037400     PERFORM DECL-WRITE-LINE.                                     SQ1534.2
037500     MOVE "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"      SQ1534.2
037600             TO DUMMY-RECORD.                                     SQ1534.2
037700     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1534.2
037800*                                                                 SQ1534.2
037900 END DECLARATIVES.                                                SQ1534.2
038000*                                                                 SQ1534.2
038100*                                                                 SQ1534.2
038200 CCVS1 SECTION.                                                   SQ1534.2
038300 OPEN-FILES.                                                      SQ1534.2
038400     OPEN    OUTPUT PRINT-FILE.                                   SQ1534.2
038500     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1534.2
038600     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1534.2
038700     MOVE    SPACE TO TEST-RESULTS.                               SQ1534.2
038800     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1534.2
038900     GO TO CCVS1-EXIT.                                            SQ1534.2
039000*                                                                 SQ1534.2
039100 CLOSE-FILES.                                                     SQ1534.2
039200     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1534.2
039300     CLOSE   PRINT-FILE.                                          SQ1534.2
039400 TERMINATE-CCVS.                                                  SQ1534.2
039500     STOP    RUN.                                                 SQ1534.2
039600*                                                                 SQ1534.2
039700 INSPT.                                                           SQ1534.2
039800     MOVE   "INSPT" TO P-OR-F.                                    SQ1534.2
039900     ADD     1 TO INSPECT-COUNTER.                                SQ1534.2
040000     PERFORM PRINT-DETAIL.                                        SQ1534.2
040100*                                                                 SQ1534.2
040200 PASS.                                                            SQ1534.2
040300     MOVE   "PASS " TO P-OR-F.                                    SQ1534.2
040400     ADD     1 TO PASS-COUNTER.                                   SQ1534.2
040500     PERFORM PRINT-DETAIL.                                        SQ1534.2
040600*                                                                 SQ1534.2
040700 FAIL.                                                            SQ1534.2
040800     MOVE   "FAIL*" TO P-OR-F.                                    SQ1534.2
040900     ADD     1 TO ERROR-COUNTER.                                  SQ1534.2
041000     PERFORM PRINT-DETAIL.                                        SQ1534.2
041100*                                                                 SQ1534.2
041200 DE-LETE.                                                         SQ1534.2
041300     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1534.2
041400     MOVE   "*****" TO P-OR-F.                                    SQ1534.2
041500     ADD     1 TO DELETE-COUNTER.                                 SQ1534.2
041600     PERFORM PRINT-DETAIL.                                        SQ1534.2
041700*                                                                 SQ1534.2
041800 PRINT-DETAIL.                                                    SQ1534.2
041900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1534.2
042000         MOVE   "." TO PARDOT-X                                   SQ1534.2
042100         MOVE    REC-CT TO DOTVALUE.                              SQ1534.2
042200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1534.2
042300     PERFORM WRITE-LINE.                                          SQ1534.2
042400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1534.2
042500         PERFORM WRITE-LINE                                       SQ1534.2
042600         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1534.2
042700     ELSE                                                         SQ1534.2
042800         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1534.2
042900     MOVE    SPACE TO P-OR-F.                                     SQ1534.2
043000     MOVE    SPACE TO COMPUTED-X.                                 SQ1534.2
043100     MOVE    SPACE TO CORRECT-X.                                  SQ1534.2
043200     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1534.2
043300     MOVE    SPACE TO RE-MARK.                                    SQ1534.2
043400*                                                                 SQ1534.2
043500 HEAD-ROUTINE.                                                    SQ1534.2
043600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1534.2
043700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1534.2
043800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1534.2
043900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1534.2
044000 COLUMN-NAMES-ROUTINE.                                            SQ1534.2
044100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1534.2
044200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1534.2
044300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1534.2
044400 END-ROUTINE.                                                     SQ1534.2
044500     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1534.2
044600     PERFORM WRITE-LINE 5 TIMES.                                  SQ1534.2
044700 END-RTN-EXIT.                                                    SQ1534.2
044800     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1534.2
044900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1534.2
045000*                                                                 SQ1534.2
045100 END-ROUTINE-1.                                                   SQ1534.2
045200     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1534.2
045300     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1534.2
045400     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1534.2
045500     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1534.2
045600     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1534.2
045700     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1534.2
045800     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1534.2
045900     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1534.2
046000     PERFORM WRITE-LINE.                                          SQ1534.2
046100     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1534.2
046200     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1534.2
046300         MOVE   "NO " TO ERROR-TOTAL                              SQ1534.2
046400     ELSE                                                         SQ1534.2
046500         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1534.2
046600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1534.2
046700     PERFORM WRITE-LINE.                                          SQ1534.2
046800 END-ROUTINE-13.                                                  SQ1534.2
046900     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1534.2
047000         MOVE   "NO " TO ERROR-TOTAL                              SQ1534.2
047100     ELSE                                                         SQ1534.2
047200         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1534.2
047300     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1534.2
047400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1534.2
047500     PERFORM WRITE-LINE.                                          SQ1534.2
047600     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1534.2
047700         MOVE   "NO " TO ERROR-TOTAL                              SQ1534.2
047800     ELSE                                                         SQ1534.2
047900         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1534.2
048000     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1534.2
048100     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1534.2
048200     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1534.2
048300*                                                                 SQ1534.2
048400 WRITE-LINE.                                                      SQ1534.2
048500     ADD     1 TO RECORD-COUNT.                                   SQ1534.2
048600*    IF RECORD-COUNT GREATER 50                                   SQ1534.2
048700*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1534.2
048800*        MOVE  SPACE TO DUMMY-RECORD                              SQ1534.2
048900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1534.2
049000*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1534.2
049100*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1534.2
049200*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1534.2
049300*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1534.2
049400*        MOVE  ZERO TO RECORD-COUNT.                              SQ1534.2
049500     PERFORM WRT-LN.                                              SQ1534.2
049600*                                                                 SQ1534.2
049700 WRT-LN.                                                          SQ1534.2
049800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1534.2
049900     MOVE    SPACE TO DUMMY-RECORD.                               SQ1534.2
050000 BLANK-LINE-PRINT.                                                SQ1534.2
050100     PERFORM WRT-LN.                                              SQ1534.2
050200 FAIL-ROUTINE.                                                    SQ1534.2
050300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1534.2
050400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1534.2
050500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1534.2
050600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1534.2
050700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1534.2
050800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1534.2
050900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1534.2
051000     GO TO   FAIL-ROUTINE-EX.                                     SQ1534.2
051100 FAIL-ROUTINE-WRITE.                                              SQ1534.2
051200     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1534.2
051300     PERFORM WRITE-LINE                                           SQ1534.2
051400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1534.2
051500     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1534.2
051600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1534.2
051700     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1534.2
051800 FAIL-ROUTINE-EX.                                                 SQ1534.2
051900     EXIT.                                                        SQ1534.2
052000 BAIL-OUT.                                                        SQ1534.2
052100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1534.2
052200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1534.2
052300 BAIL-OUT-WRITE.                                                  SQ1534.2
052400     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1534.2
052500     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1534.2
052600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1534.2
052700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1534.2
052800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1534.2
052900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1534.2
053000 BAIL-OUT-EX.                                                     SQ1534.2
053100     EXIT.                                                        SQ1534.2
053200 CCVS1-EXIT.                                                      SQ1534.2
053300     EXIT.                                                        SQ1534.2
053400*                                                                 SQ1534.2
053500****************************************************************  SQ1534.2
053600*                                                              *  SQ1534.2
053700*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1534.2
053800*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1534.2
053900*                                                              *  SQ1534.2
054000****************************************************************  SQ1534.2
054100*                                                                 SQ1534.2
054200 SECT-SQ153A-0002 SECTION.                                        SQ1534.2
054300*                                                                 SQ1534.2
054400*    THIS TEST CREATES FILE SQ-FS4 AND CLOSES IT.                 SQ1534.2
054500*    FIRST IT SETS UP A SKELETON RECORD IN WORKING STORAGE.       SQ1534.2
054600*                                                                 SQ1534.2
054700 WRITE-INIT-GF-01.                                                SQ1534.2
054800     MOVE "SQ-FS4" TO XFILE-NAME (1).                             SQ1534.2
054900     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1534.2
055000     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1534.2
055100     MOVE 120 TO XRECORD-LENGTH (1).                              SQ1534.2
055200     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1534.2
055300     MOVE 1 TO XBLOCK-SIZE (1).                                   SQ1534.2
055400     MOVE 1 TO RECORDS-IN-FILE (1).                               SQ1534.2
055500     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1534.2
055600     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1534.2
055700     MOVE 1 TO XRECORD-NUMBER (1).                                SQ1534.2
055800*                                                                 SQ1534.2
055900 WRITE-OPEN-01.                                                   SQ1534.2
056000     OPEN OUTPUT SQ-FS4.                                          SQ1534.2
056100*                                                                 SQ1534.2
056200 WRITE-TEST-01-01.                                                SQ1534.2
056300     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.        SQ1534.2
056400     WRITE SQ-FS4R1-F-G-120.                                      SQ1534.2
056500*                                                                 SQ1534.2
056600 CLOSE-TEST-01.                                                   SQ1534.2
056700     CLOSE SQ-FS4.                                                SQ1534.2
056800*                                                                 SQ1534.2
056900 OPEN-TEST-02.                                                    SQ1534.2
057000     OPEN I-O SQ-FS4.                                             SQ1534.2
057100*                                                                 SQ1534.2
057200*        THIS TEST OPENS THE FILE JUST CREATED IN THE I-O MODE.   SQ1534.2
057300*        WE ATTEMPT TO WRITE ANOTHER RECORD AND EXAMINE IN A      SQ1534.2
057400*        DECLARATIVE THE I-O STATUS RETURNED.  IT IS POSSIBLE     SQ1534.2
057500*        THAT THE SYSTEM ACTION MAY BE ABNORMAL PROGRAM           SQ1534.2
057600*        TERMINATION AFTER THE DECLARATIVE IS EXECUTED.  THE      SQ1534.2
057700*        RECORD NUMBER FIELD IN THE RECORD TO BE WRITTEN IS       SQ1534.2
057800*        CHANGED FROM THAT IN THE RECORD ORIGINALLY WRITTEN TO    SQ1534.2
057900*        AID IN ESTABLISHING THE ORIGIN OF THE RECORD IN ANY      SQ1534.2
058000*        SUBSEQUENT EXAMINATION OF THE FILE.                      SQ1534.2
058100*                                                                 SQ1534.2
058200 WRITE-INIT-02.                                                   SQ1534.2
058300     MOVE 1 TO REC-CT.                                            SQ1534.2
058400     MOVE "WRITE-TEST-02" TO PAR-NAME.                            SQ1534.2
058500     MOVE "WRITE TO I-O FILE" TO FEATURE.                         SQ1534.2
058600     MOVE 2 TO XRECORD-NUMBER (1).                                SQ1534.2
058700 WRITE-TEST-02.                                                   SQ1534.2
058800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.        SQ1534.2
058900     WRITE SQ-FS4R1-F-G-120.                                      SQ1534.2
059000*                                                                 SQ1534.2
059100 CLOSE-TEST-02.                                                   SQ1534.2
059200     CLOSE SQ-FS4.                                                SQ1534.2
059300*                                                                 SQ1534.2
059400 CCVS-EXIT SECTION.                                               SQ1534.2
059500 CCVS-999999.                                                     SQ1534.2
059600     GO TO   CLOSE-FILES.                                         SQ1534.2
      *END-OF,SQ153A                                                                  
