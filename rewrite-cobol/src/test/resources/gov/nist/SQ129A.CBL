      *HEADER,COBOL,SQ129A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1294.2
000200 PROGRAM-ID.                                                      SQ1294.2
000300     SQ129A.                                                      SQ1294.2
000400****************************************************************  SQ1294.2
000500*                                                              *  SQ1294.2
000600*    VALIDATION FOR:-                                          *  SQ1294.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1294.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1294.2
000900*                             REVISED 1986, AUGUST             *  SQ1294.2
001000*                                                              *  SQ1294.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1294.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1294.2
001300*                                                              *  SQ1294.2
001400****************************************************************  SQ1294.2
001500*                                                              *  SQ1294.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1294.2
001700*                                                              *  SQ1294.2
001800*            X-01   SEQUENTIAL MAGNETIC TAPE FILE.             *  SQ1294.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1294.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1294.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1294.2
002200*                                                              *  SQ1294.2
002300*                                                              *  SQ1294.2
002400****************************************************************  SQ1294.2
002500*                                                              *  SQ1294.2
002600*    SQ129A ATTEMPTS TO OPEN FOR INPUT A MAGNETIC TAPE FILE    *  SQ1294.2
002700*    WHICH IS NOT PRESENT.  THIS SHOULD RESULT IN A PERMANENT  *  SQ1294.2
002800*    ERROR AND AN I-O STATUS OF "35".                          *  SQ1294.2
002900*                                                              *  SQ1294.2
003000*    THIS PROGRAM HAS BEEN SPLIT FROM V2.0 ONWARDS.            *  SQ1294.2
003100*    THE NEW PROGRAMS ARE SQ141A AND SQ142A.                   *  SQ1294.2
003200*                                                              *  SQ1294.2
003300*                                                              *  SQ1294.2
003400****************************************************************  SQ1294.2
003500*                                                                 SQ1294.2
003600 ENVIRONMENT DIVISION.                                            SQ1294.2
003700 CONFIGURATION SECTION.                                           SQ1294.2
003800 SOURCE-COMPUTER.                                                 SQ1294.2
003900     XXXXX082.                                                    SQ1294.2
004000 OBJECT-COMPUTER.                                                 SQ1294.2
004100     XXXXX083.                                                    SQ1294.2
004200*                                                                 SQ1294.2
004300 INPUT-OUTPUT SECTION.                                            SQ1294.2
004400 FILE-CONTROL.                                                    SQ1294.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SQ1294.2
004600     XXXXX055.                                                    SQ1294.2
004700*                                                                 SQ1294.2
004800*    SELECT RAW-DATA   ASSIGN TO                                  SQ1294.2
004900*    XXXXX062                                                     SQ1294.2
005000*          ORGANIZATION IS INDEXED                                SQ1294.2
005100*          ACCESS MODE  IS RANDOM                                 SQ1294.2
005200*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1294.2
005300*                                                                 SQ1294.2
005400     SELECT SQ-FS1 ASSIGN TO                                      SQ1294.2
005500     XXXXX001                                                     SQ1294.2
005600            FILE STATUS IS SQ-FS1-STATUS.                         SQ1294.2
005700*                                                                 SQ1294.2
005800*                                                                 SQ1294.2
005900 DATA DIVISION.                                                   SQ1294.2
006000 FILE SECTION.                                                    SQ1294.2
006100 FD  PRINT-FILE                                                   SQ1294.2
006200C    LABEL RECORDS                                                SQ1294.2
006300C    XXXXX084                                                     SQ1294.2
006400C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1294.2
006500               .                                                  SQ1294.2
006600 01  PRINT-REC    PICTURE X(120).                                 SQ1294.2
006700 01  DUMMY-RECORD PICTURE X(120).                                 SQ1294.2
006800*                                                                 SQ1294.2
006900*FD  RAW-DATA.                                                    SQ1294.2
007000*01  RAW-DATA-SATZ.                                               SQ1294.2
007100*    05  RAW-DATA-KEY        PIC X(6).                            SQ1294.2
007200*    05  C-DATE              PIC 9(6).                            SQ1294.2
007300*    05  C-TIME              PIC 9(8).                            SQ1294.2
007400*    05  NO-OF-TESTS         PIC 99.                              SQ1294.2
007500*    05  C-OK                PIC 999.                             SQ1294.2
007600*    05  C-ALL               PIC 999.                             SQ1294.2
007700*    05  C-FAIL              PIC 999.                             SQ1294.2
007800*    05  C-DELETED           PIC 999.                             SQ1294.2
007900*    05  C-INSPECT           PIC 999.                             SQ1294.2
008000*    05  C-NOTE              PIC X(13).                           SQ1294.2
008100*    05  C-INDENT            PIC X.                               SQ1294.2
008200*    05  C-ABORT             PIC X(8).                            SQ1294.2
008300*                                                                 SQ1294.2
008400 FD  SQ-FS1                                                       SQ1294.2
008500C    LABEL RECORD IS STANDARD                                     SQ1294.2
008600                .                                                 SQ1294.2
008700 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1294.2
008800*                                                                 SQ1294.2
008900 WORKING-STORAGE SECTION.                                         SQ1294.2
009000*                                                                 SQ1294.2
009100***************************************************************   SQ1294.2
009200*                                                             *   SQ1294.2
009300*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1294.2
009400*                                                             *   SQ1294.2
009500***************************************************************   SQ1294.2
009600*                                                                 SQ1294.2
009700 01  SQ-FS1-STATUS.                                               SQ1294.2
009800   03  SQ-FS1-KEY-1   PIC X.                                      SQ1294.2
009900   03  SQ-FS1-KEY-2   PIC X.                                      SQ1294.2
010000*                                                                 SQ1294.2
010100 01  DECL-EXEC-SW  PIC 9.                                         SQ1294.2
010200*                                                                 SQ1294.2
010300***************************************************************   SQ1294.2
010400*                                                             *   SQ1294.2
010500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1294.2
010600*                                                             *   SQ1294.2
010700***************************************************************   SQ1294.2
010800*                                                                 SQ1294.2
010900 01  REC-SKEL-SUB   PIC 99.                                       SQ1294.2
011000*                                                                 SQ1294.2
011100 01  FILE-RECORD-INFORMATION-REC.                                 SQ1294.2
011200     03 FILE-RECORD-INFO-SKELETON.                                SQ1294.2
011300        05 FILLER                 PICTURE X(48)       VALUE       SQ1294.2
011400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1294.2
011500        05 FILLER                 PICTURE X(46)       VALUE       SQ1294.2
011600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1294.2
011700        05 FILLER                 PICTURE X(26)       VALUE       SQ1294.2
011800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1294.2
011900        05 FILLER                 PICTURE X(37)       VALUE       SQ1294.2
012000             ",RECKEY=                             ".             SQ1294.2
012100        05 FILLER                 PICTURE X(38)       VALUE       SQ1294.2
012200             ",ALTKEY1=                             ".            SQ1294.2
012300        05 FILLER                 PICTURE X(38)       VALUE       SQ1294.2
012400             ",ALTKEY2=                             ".            SQ1294.2
012500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1294.2
012600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1294.2
012700        05 FILE-RECORD-INFO-P1-120.                               SQ1294.2
012800           07 FILLER              PIC X(5).                       SQ1294.2
012900           07 XFILE-NAME          PIC X(6).                       SQ1294.2
013000           07 FILLER              PIC X(8).                       SQ1294.2
013100           07 XRECORD-NAME        PIC X(6).                       SQ1294.2
013200           07 FILLER              PIC X(1).                       SQ1294.2
013300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1294.2
013400           07 FILLER              PIC X(7).                       SQ1294.2
013500           07 XRECORD-NUMBER      PIC 9(6).                       SQ1294.2
013600           07 FILLER              PIC X(6).                       SQ1294.2
013700           07 UPDATE-NUMBER       PIC 9(2).                       SQ1294.2
013800           07 FILLER              PIC X(5).                       SQ1294.2
013900           07 ODO-NUMBER          PIC 9(4).                       SQ1294.2
014000           07 FILLER              PIC X(5).                       SQ1294.2
014100           07 XPROGRAM-NAME       PIC X(5).                       SQ1294.2
014200           07 FILLER              PIC X(7).                       SQ1294.2
014300           07 XRECORD-LENGTH      PIC 9(6).                       SQ1294.2
014400           07 FILLER              PIC X(7).                       SQ1294.2
014500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1294.2
014600           07 FILLER              PIC X(1).                       SQ1294.2
014700           07 XBLOCK-SIZE         PIC 9(4).                       SQ1294.2
014800           07 FILLER              PIC X(6).                       SQ1294.2
014900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1294.2
015000           07 FILLER              PIC X(5).                       SQ1294.2
015100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1294.2
015200           07 FILLER              PIC X(6).                       SQ1294.2
015300           07 XLABEL-TYPE         PIC X(1).                       SQ1294.2
015400        05 FILE-RECORD-INFO-P121-240.                             SQ1294.2
015500           07 FILLER              PIC X(8).                       SQ1294.2
015600           07 XRECORD-KEY         PIC X(29).                      SQ1294.2
015700           07 FILLER              PIC X(9).                       SQ1294.2
015800           07 ALTERNATE-KEY1      PIC X(29).                      SQ1294.2
015900           07 FILLER              PIC X(9).                       SQ1294.2
016000           07 ALTERNATE-KEY2      PIC X(29).                      SQ1294.2
016100           07 FILLER              PIC X(7).                       SQ1294.2
016200*                                                                 SQ1294.2
016300 01  TEST-RESULTS.                                                SQ1294.2
016400     02 FILLER              PIC X      VALUE SPACE.               SQ1294.2
016500     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1294.2
016600     02 FILLER              PIC X      VALUE SPACE.               SQ1294.2
016700     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1294.2
016800     02 FILLER              PIC X      VALUE SPACE.               SQ1294.2
016900     02  PAR-NAME.                                                SQ1294.2
017000       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1294.2
017100       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1294.2
017200       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1294.2
017300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1294.2
017400     02 RE-MARK             PIC X(61).                            SQ1294.2
017500 01  TEST-COMPUTED.                                               SQ1294.2
017600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1294.2
017700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1294.2
017800   02 COMPUTED-X.                                                 SQ1294.2
017900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1294.2
018000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1294.2
018100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1294.2
018200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1294.2
018300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1294.2
018400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1294.2
018500        04 COMPUTED-18V0                   PIC -9(18).            SQ1294.2
018600        04 FILLER                          PIC X.                 SQ1294.2
018700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1294.2
018800 01  TEST-CORRECT.                                                SQ1294.2
018900     02 FILLER PIC X(30) VALUE SPACE.                             SQ1294.2
019000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1294.2
019100     02 CORRECT-X.                                                SQ1294.2
019200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1294.2
019300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1294.2
019400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1294.2
019500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1294.2
019600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1294.2
019700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1294.2
019800         04 CORRECT-18V0                     PIC -9(18).          SQ1294.2
019900         04 FILLER                           PIC X.               SQ1294.2
020000     03 FILLER PIC X(2) VALUE SPACE.                              SQ1294.2
020100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1294.2
020200 01  CCVS-C-1.                                                    SQ1294.2
020300     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ1294.2
020400     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ1294.2
020500-    "SS  PARAGRAPH-NAME                                          SQ1294.2
020600-    "       REMARKS".                                            SQ1294.2
020700     02 FILLER           PIC X(17)    VALUE SPACE.                SQ1294.2
020800 01  CCVS-C-2.                                                    SQ1294.2
020900     02 FILLER           PIC XXXX     VALUE SPACE.                SQ1294.2
021000     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ1294.2
021100     02 FILLER           PIC X(16)    VALUE SPACE.                SQ1294.2
021200     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ1294.2
021300     02 FILLER           PIC X(90)    VALUE SPACE.                SQ1294.2
021400 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1294.2
021500 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1294.2
021600 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1294.2
021700 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1294.2
021800 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1294.2
021900 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1294.2
022000 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1294.2
022100 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1294.2
022200 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1294.2
022300 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1294.2
022400 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1294.2
022500 01  CCVS-H-1.                                                    SQ1294.2
022600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1294.2
022700     02  FILLER          PIC X(42)    VALUE                       SQ1294.2
022800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1294.2
022900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1294.2
023000 01  CCVS-H-2A.                                                   SQ1294.2
023100   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1294.2
023200   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1294.2
023300   02  FILLER            PIC XXXX   VALUE                         SQ1294.2
023400     "4.2 ".                                                      SQ1294.2
023500   02  FILLER            PIC X(28)  VALUE                         SQ1294.2
023600            " COPY - NOT FOR DISTRIBUTION".                       SQ1294.2
023700   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1294.2
023800*                                                                 SQ1294.2
023900 01  CCVS-H-2B.                                                   SQ1294.2
024000   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1294.2
024100   02  TEST-ID           PIC X(9).                                SQ1294.2
024200   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1294.2
024300   02  FILLER            PIC X(12)  VALUE                         SQ1294.2
024400     " HIGH       ".                                              SQ1294.2
024500   02  FILLER            PIC X(22)  VALUE                         SQ1294.2
024600            " LEVEL VALIDATION FOR ".                             SQ1294.2
024700   02  FILLER            PIC X(58)  VALUE                         SQ1294.2
024800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1294.2
024900 01  CCVS-H-3.                                                    SQ1294.2
025000     02  FILLER          PIC X(34)  VALUE                         SQ1294.2
025100            " FOR OFFICIAL USE ONLY    ".                         SQ1294.2
025200     02  FILLER          PIC X(58)  VALUE                         SQ1294.2
025300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1294.2
025400     02  FILLER          PIC X(28)  VALUE                         SQ1294.2
025500            "  COPYRIGHT   1985,1986 ".                           SQ1294.2
025600 01  CCVS-E-1.                                                    SQ1294.2
025700     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1294.2
025800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1294.2
025900     02 ID-AGAIN         PIC X(9).                                SQ1294.2
026000     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1294.2
026100 01  CCVS-E-2.                                                    SQ1294.2
026200     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1294.2
026300     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1294.2
026400     02  CCVS-E-2-2.                                              SQ1294.2
026500         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1294.2
026600         03 FILLER         PIC X      VALUE SPACE.                SQ1294.2
026700         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1294.2
026800            "ERRORS ENCOUNTERED".                                 SQ1294.2
026900 01  CCVS-E-3.                                                    SQ1294.2
027000     02  FILLER          PIC X(22)  VALUE                         SQ1294.2
027100            " FOR OFFICIAL USE ONLY".                             SQ1294.2
027200     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1294.2
027300     02  FILLER          PIC X(58)  VALUE                         SQ1294.2
027400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1294.2
027500     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1294.2
027600     02  FILLER          PIC X(20)  VALUE                         SQ1294.2
027700             " COPYRIGHT 1985,1986".                              SQ1294.2
027800 01  CCVS-E-4.                                                    SQ1294.2
027900     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1294.2
028000     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1294.2
028100     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1294.2
028200     02 FILLER           PIC X(40)  VALUE                         SQ1294.2
028300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1294.2
028400 01  XXINFO.                                                      SQ1294.2
028500     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1294.2
028600     02 INFO-TEXT.                                                SQ1294.2
028700       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1294.2
028800       04 XXCOMPUTED         PIC X(20).                           SQ1294.2
028900       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1294.2
029000       04 XXCORRECT          PIC X(20).                           SQ1294.2
029100     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1294.2
029200 01  HYPHEN-LINE.                                                 SQ1294.2
029300     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1294.2
029400     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1294.2
029500-    "*****************************************".                 SQ1294.2
029600     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1294.2
029700-    "******************************".                            SQ1294.2
029800 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1294.2
029900     "SQ129A".                                                    SQ1294.2
030000*                                                                 SQ1294.2
030100*                                                                 SQ1294.2
030200 PROCEDURE DIVISION.                                              SQ1294.2
030300 DECLARATIVES.                                                    SQ1294.2
030400 SQ129A-DECLARATIVE-001-SECT SECTION.                             SQ1294.2
030500     USE AFTER STANDARD EXCEPTION PROCEDURE ON INPUT.             SQ1294.2
030600 INPUT-ERROR-PROCEDURE.                                           SQ1294.2
030700     IF DECL-EXEC-SW NOT = 9                                      SQ1294.2
030800         GO TO   NOT-DECL-9.                                      SQ1294.2
030900*                                                                 SQ1294.2
031000*    DECLARATIVE PROCEDURE ENTERED FROM OPEN INPUT                SQ1294.2
031100*                                                                 SQ1294.2
031200 DECL-OPEN-TEST.                                                  SQ1294.2
031300     MOVE   "DECL-OPEN-TEST" TO PAR-NAME.                         SQ1294.2
031400     MOVE    1               TO REC-CT.                           SQ1294.2
031500     IF SQ-FS1-STATUS = "35"                                      SQ1294.2
031600         PERFORM DECL-PASS                                        SQ1294.2
031700     ELSE                                                         SQ1294.2
031800         MOVE   "35"           TO CORRECT-A                       SQ1294.2
031900         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1294.2
032000         MOVE   "INCORRECT FILE STATUS FOR NON-AVAILABLE FILE"    SQ1294.2
032100                   TO RE-MARK                                     SQ1294.2
032200         PERFORM DECL-FAIL.                                       SQ1294.2
032300     MOVE    SPACE TO DUMMY-RECORD                                SQ1294.2
032400     PERFORM DECL-WRITE-LINE                                      SQ1294.2
032500     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1294.2
032600     GO TO   END-DECLS.                                           SQ1294.2
032700*                                                                 SQ1294.2
032800*                                                                 SQ1294.2
032900 NOT-DECL-9.                                                      SQ1294.2
033000     MOVE   "NOT-DECL-9"  TO PAR-NAME.                            SQ1294.2
033100     MOVE    DECL-EXEC-SW TO COMPUTED-18V0.                       SQ1294.2
033200     MOVE    9            TO CORRECT-18V0.                        SQ1294.2
033300     PERFORM DECL-FAIL.                                           SQ1294.2
033400     GO TO   END-DECLS.                                           SQ1294.2
033500*                                                                 SQ1294.2
033600*                                                                 SQ1294.2
033700*                                                                 SQ1294.2
033800 DECL-PASS.                                                       SQ1294.2
033900     MOVE   "PASS " TO P-OR-F.                                    SQ1294.2
034000     ADD     1 TO PASS-COUNTER.                                   SQ1294.2
034100     PERFORM DECL-PRINT-DETAIL.                                   SQ1294.2
034200*                                                                 SQ1294.2
034300 DECL-FAIL.                                                       SQ1294.2
034400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1294.2
034500     ADD     1 TO ERROR-COUNTER.                                  SQ1294.2
034600     PERFORM DECL-PRINT-DETAIL.                                   SQ1294.2
034700*                                                                 SQ1294.2
034800 DECL-PRINT-DETAIL.                                               SQ1294.2
034900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1294.2
035000             MOVE "." TO PARDOT-X                                 SQ1294.2
035100             MOVE REC-CT TO DOTVALUE.                             SQ1294.2
035200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1294.2
035300     PERFORM DECL-WRITE-LINE.                                     SQ1294.2
035400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1294.2
035500         PERFORM DECL-WRITE-LINE                                  SQ1294.2
035600         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1294.2
035700     ELSE                                                         SQ1294.2
035800         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1294.2
035900     MOVE    SPACE TO P-OR-F.                                     SQ1294.2
036000     MOVE    SPACE TO COMPUTED-X.                                 SQ1294.2
036100     MOVE    SPACE TO CORRECT-X.                                  SQ1294.2
036200     IF REC-CT EQUAL TO ZERO                                      SQ1294.2
036300         MOVE    SPACE TO PAR-NAME.                               SQ1294.2
036400     MOVE    SPACE TO RE-MARK.                                    SQ1294.2
036500*                                                                 SQ1294.2
036600 DECL-WRITE-LINE.                                                 SQ1294.2
036700     ADD     1 TO RECORD-COUNT.                                   SQ1294.2
036800*    IF RECORD-COUNT GREATER 50                                   SQ1294.2
036900*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1294.2
037000*        MOVE    SPACE TO DUMMY-RECORD                            SQ1294.2
037100*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1294.2
037200*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1294.2
037300*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1294.2
037400*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1294.2
037500*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1294.2
037600*        PERFORM DECL-WRT-LN                                      SQ1294.2
037700*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1294.2
037800*        MOVE    ZERO TO RECORD-COUNT.                            SQ1294.2
037900     PERFORM DECL-WRT-LN.                                         SQ1294.2
038000*                                                                 SQ1294.2
038100 DECL-WRT-LN.                                                     SQ1294.2
038200     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1294.2
038300     MOVE    SPACE TO DUMMY-RECORD.                               SQ1294.2
038400*                                                                 SQ1294.2
038500 DECL-FAIL-ROUTINE.                                               SQ1294.2
038600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1294.2
038700     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1294.2
038800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1294.2
038900     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1294.2
039000     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1294.2
039100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1294.2
039200     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1294.2
039300     GO TO   DECL-FAIL-EX.                                        SQ1294.2
039400 DECL-FAIL-WRITE.                                                 SQ1294.2
039500     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1294.2
039600     PERFORM DECL-WRITE-LINE                                      SQ1294.2
039700     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1294.2
039800     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1294.2
039900     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1294.2
040000     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1294.2
040100 DECL-FAIL-EX.                                                    SQ1294.2
040200     EXIT.                                                        SQ1294.2
040300*                                                                 SQ1294.2
040400 DECL-BAIL.                                                       SQ1294.2
040500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1294.2
040600     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1294.2
040700 DECL-BAIL-WRITE.                                                 SQ1294.2
040800     MOVE    CORRECT-A TO XXCORRECT.                              SQ1294.2
040900     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1294.2
041000     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1294.2
041100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1294.2
041200 DECL-BAIL-EX.                                                    SQ1294.2
041300     EXIT.                                                        SQ1294.2
041400*                                                                 SQ1294.2
041500 END-DECLS.                                                       SQ1294.2
041600     MOVE    ZERO TO DECL-EXEC-SW.                                SQ1294.2
041700 END DECLARATIVES.                                                SQ1294.2
041800*                                                                 SQ1294.2
041900*                                                                 SQ1294.2
042000 CCVS1 SECTION.                                                   SQ1294.2
042100 OPEN-FILES.                                                      SQ1294.2
042200*    OPEN    I-O RAW-DATA.                                        SQ1294.2
042300*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1294.2
042400*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1294.2
042500*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1294.2
042600*    ADD     1           TO C-NO-OF-TESTS.                        SQ1294.2
042700*    ACCEPT  C-DATE      FROM DATE.                               SQ1294.2
042800*    ACCEPT  C-TIME      FROM TIME.                               SQ1294.2
042900*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1294.2
043000*END-E-1.                                                         SQ1294.2
043100*    CLOSE   RAW-DATA.                                            SQ1294.2
043200     OPEN    OUTPUT PRINT-FILE.                                   SQ1294.2
043300     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1294.2
043400     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1294.2
043500     MOVE    SPACE TO TEST-RESULTS.                               SQ1294.2
043600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1294.2
043700     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1294.2
043800     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1294.2
043900     GO TO CCVS1-EXIT.                                            SQ1294.2
044000*                                                                 SQ1294.2
044100 CCVS-INIT-FILE.                                                  SQ1294.2
044200     ADD     1 TO REC-SKL-SUB.                                    SQ1294.2
044300     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1294.2
044400                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1294.2
044500*                                                                 SQ1294.2
044600 CLOSE-FILES.                                                     SQ1294.2
044700     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1294.2
044800     CLOSE   PRINT-FILE.                                          SQ1294.2
044900*    OPEN    I-O RAW-DATA.                                        SQ1294.2
045000*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1294.2
045100*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1294.2
045200*    MOVE   "OK.     " TO C-ABORT.                                SQ1294.2
045300*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1294.2
045400*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1294.2
045500*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1294.2
045600*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1294.2
045700*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1294.2
045800*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1294.2
045900*END-E-2.                                                         SQ1294.2
046000*    CLOSE   RAW-DATA.                                            SQ1294.2
046100 TERMINATE-CCVS.                                                  SQ1294.2
046200*    EXIT    PROGRAM.                                             SQ1294.2
046300     STOP    RUN.                                                 SQ1294.2
046400*                                                                 SQ1294.2
046500 INSPT.                                                           SQ1294.2
046600     MOVE   "INSPT" TO P-OR-F.                                    SQ1294.2
046700     ADD     1 TO INSPECT-COUNTER.                                SQ1294.2
046800     PERFORM PRINT-DETAIL.                                        SQ1294.2
046900                                                                  SQ1294.2
047000 PASS.                                                            SQ1294.2
047100     MOVE   "PASS " TO P-OR-F.                                    SQ1294.2
047200     ADD     1 TO PASS-COUNTER.                                   SQ1294.2
047300     PERFORM PRINT-DETAIL.                                        SQ1294.2
047400*                                                                 SQ1294.2
047500 FAIL.                                                            SQ1294.2
047600     MOVE   "FAIL*" TO P-OR-F.                                    SQ1294.2
047700     ADD     1 TO ERROR-COUNTER.                                  SQ1294.2
047800     PERFORM PRINT-DETAIL.                                        SQ1294.2
047900*                                                                 SQ1294.2
048000 DE-LETE.                                                         SQ1294.2
048100     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1294.2
048200     MOVE   "*****" TO P-OR-F.                                    SQ1294.2
048300     ADD     1 TO DELETE-COUNTER.                                 SQ1294.2
048400     PERFORM PRINT-DETAIL.                                        SQ1294.2
048500*                                                                 SQ1294.2
048600 PRINT-DETAIL.                                                    SQ1294.2
048700     IF REC-CT NOT EQUAL TO ZERO                                  SQ1294.2
048800         MOVE   "." TO PARDOT-X                                   SQ1294.2
048900         MOVE    REC-CT TO DOTVALUE.                              SQ1294.2
049000     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1294.2
049100     PERFORM WRITE-LINE.                                          SQ1294.2
049200     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1294.2
049300         PERFORM WRITE-LINE                                       SQ1294.2
049400         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1294.2
049500     ELSE                                                         SQ1294.2
049600         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1294.2
049700     MOVE    SPACE TO P-OR-F.                                     SQ1294.2
049800     MOVE    SPACE TO COMPUTED-X.                                 SQ1294.2
049900     MOVE    SPACE TO CORRECT-X.                                  SQ1294.2
050000     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1294.2
050100     MOVE    SPACE TO RE-MARK.                                    SQ1294.2
050200*                                                                 SQ1294.2
050300 HEAD-ROUTINE.                                                    SQ1294.2
050400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1294.2
050500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1294.2
050600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1294.2
050700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1294.2
050800 COLUMN-NAMES-ROUTINE.                                            SQ1294.2
050900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1294.2
051000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1294.2
051100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1294.2
051200 END-ROUTINE.                                                     SQ1294.2
051300     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1294.2
051400     PERFORM WRITE-LINE 5 TIMES.                                  SQ1294.2
051500 END-RTN-EXIT.                                                    SQ1294.2
051600     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1294.2
051700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1294.2
051800*                                                                 SQ1294.2
051900 END-ROUTINE-1.                                                   SQ1294.2
052000     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1294.2
052100     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1294.2
052200     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1294.2
052300     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1294.2
052400     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1294.2
052500     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1294.2
052600     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1294.2
052700     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1294.2
052800     PERFORM WRITE-LINE.                                          SQ1294.2
052900     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1294.2
053000     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1294.2
053100         MOVE   "NO " TO ERROR-TOTAL                              SQ1294.2
053200     ELSE                                                         SQ1294.2
053300         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1294.2
053400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1294.2
053500     PERFORM WRITE-LINE.                                          SQ1294.2
053600 END-ROUTINE-13.                                                  SQ1294.2
053700     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1294.2
053800         MOVE   "NO " TO ERROR-TOTAL                              SQ1294.2
053900     ELSE                                                         SQ1294.2
054000         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1294.2
054100     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1294.2
054200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1294.2
054300     PERFORM WRITE-LINE.                                          SQ1294.2
054400     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1294.2
054500         MOVE   "NO " TO ERROR-TOTAL                              SQ1294.2
054600     ELSE                                                         SQ1294.2
054700         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1294.2
054800     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1294.2
054900     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1294.2
055000     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1294.2
055100*                                                                 SQ1294.2
055200 WRITE-LINE.                                                      SQ1294.2
055300     ADD     1 TO RECORD-COUNT.                                   SQ1294.2
055400*    IF RECORD-COUNT GREATER 50                                   SQ1294.2
055500*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1294.2
055600*        MOVE  SPACE TO DUMMY-RECORD                              SQ1294.2
055700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1294.2
055800*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1294.2
055900*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1294.2
056000*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1294.2
056100*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1294.2
056200*        MOVE  ZERO TO RECORD-COUNT.                              SQ1294.2
056300     PERFORM WRT-LN.                                              SQ1294.2
056400*                                                                 SQ1294.2
056500 WRT-LN.                                                          SQ1294.2
056600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1294.2
056700     MOVE    SPACE TO DUMMY-RECORD.                               SQ1294.2
056800 BLANK-LINE-PRINT.                                                SQ1294.2
056900     PERFORM WRT-LN.                                              SQ1294.2
057000 FAIL-ROUTINE.                                                    SQ1294.2
057100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1294.2
057200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1294.2
057300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1294.2
057400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1294.2
057500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1294.2
057600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1294.2
057700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1294.2
057800     GO TO   FAIL-ROUTINE-EX.                                     SQ1294.2
057900 FAIL-ROUTINE-WRITE.                                              SQ1294.2
058000     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1294.2
058100     PERFORM WRITE-LINE                                           SQ1294.2
058200     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1294.2
058300     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1294.2
058400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1294.2
058500     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1294.2
058600 FAIL-ROUTINE-EX.                                                 SQ1294.2
058700     EXIT.                                                        SQ1294.2
058800 BAIL-OUT.                                                        SQ1294.2
058900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1294.2
059000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1294.2
059100 BAIL-OUT-WRITE.                                                  SQ1294.2
059200     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1294.2
059300     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1294.2
059400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1294.2
059500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1294.2
059600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1294.2
059700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1294.2
059800 BAIL-OUT-EX.                                                     SQ1294.2
059900     EXIT.                                                        SQ1294.2
060000 CCVS1-EXIT.                                                      SQ1294.2
060100     EXIT.                                                        SQ1294.2
060200*                                                                 SQ1294.2
060300****************************************************************  SQ1294.2
060400*                                                              *  SQ1294.2
060500*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1294.2
060600*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1294.2
060700*                                                              *  SQ1294.2
060800****************************************************************  SQ1294.2
060900*                                                                 SQ1294.2
061000 SECT-SQ129A-MAIN SECTION.                                        SQ1294.2
061100 OPEN-INIT-01.                                                    SQ1294.2
061200*                                                                 SQ1294.2
061300*    THIS PROGRAM ATTEMPTS TO OPEN A FILE WHICH IS NOT            SQ1294.2
061400*    PRESENT AND AVAILABLE TO IT.                                 SQ1294.2
061500*                                                                 SQ1294.2
061600     MOVE    1 TO REC-CT                                          SQ1294.2
061700     MOVE   "OPEN ABSENT FILE INPUT" TO FEATURE                   SQ1294.2
061800     MOVE   "OPEN-TEST-01" TO PAR-NAME                            SQ1294.2
061900     MOVE    9 TO DECL-EXEC-SW                                    SQ1294.2
062000     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1294.2
062100 OPEN-TEST-01.                                                    SQ1294.2
062200     OPEN    INPUT SQ-FS1.                                        SQ1294.2
062300 CCVS-EXIT SECTION.                                               SQ1294.2
062400 CCVS-999999.                                                     SQ1294.2
062500     GO TO   CLOSE-FILES.                                         SQ1294.2
      *END-OF,SQ129A                                                                  
