      *HEADER,COBOL,SQ136A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1364.2
000200 PROGRAM-ID.                                                      SQ1364.2
000300     SQ136A.                                                      SQ1364.2
000400****************************************************************  SQ1364.2
000500*                                                              *  SQ1364.2
000600*    VALIDATION FOR:-                                          *  SQ1364.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1364.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1364.2
000900*                             REVISED 1986, AUGUST             *  SQ1364.2
001000*                                                              *  SQ1364.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1364.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1364.2
001300*                                                              *  SQ1364.2
001400****************************************************************  SQ1364.2
001500*                                                              *  SQ1364.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1364.2
001700*                                                              *  SQ1364.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1364.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1364.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1364.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1364.2
002200*                                                              *  SQ1364.2
002300****************************************************************  SQ1364.2
002400*                                                              *  SQ1364.2
002500*    SPLIT FROM SQ122A, THIS PROGRAM REPEATS THE SEQUENCE OF   *  SQ1364.2
002600*    FILE HANDLING ROUTINES IN ORDER TO CARRY OUT THE ISOLATED *  SQ1364.2
002700*    SPLIT TEST THAT CHECKS FOR THE CORRECT RESPONSE TO READING*  SQ1364.2
002800*    PAST THE END OF A FILE. (SEE SQ122A).                     *  SQ1364.2
002900*                                                              *  SQ1364.2
003000****************************************************************  SQ1364.2
003100*                                                                 SQ1364.2
003200 ENVIRONMENT DIVISION.                                            SQ1364.2
003300 CONFIGURATION SECTION.                                           SQ1364.2
003400 SOURCE-COMPUTER.                                                 SQ1364.2
003500     XXXXX082.                                                    SQ1364.2
003600 OBJECT-COMPUTER.                                                 SQ1364.2
003700     XXXXX083.                                                    SQ1364.2
003800*                                                                 SQ1364.2
003900 INPUT-OUTPUT SECTION.                                            SQ1364.2
004000 FILE-CONTROL.                                                    SQ1364.2
004100     SELECT PRINT-FILE ASSIGN TO                                  SQ1364.2
004200     XXXXX055.                                                    SQ1364.2
004300*                                                                 SQ1364.2
004400*    SELECT RAW-DATA   ASSIGN TO                                  SQ1364.2
004500*    XXXXX062                                                     SQ1364.2
004600*          ORGANIZATION IS INDEXED                                SQ1364.2
004700*          ACCESS MODE  IS RANDOM                                 SQ1364.2
004800*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1364.2
004900*                                                                 SQ1364.2
005000     SELECT SQ-FS4 ASSIGN                                         SQ1364.2
005100     XXXXX014                                                     SQ1364.2
005200            FILE STATUS IS SQ-FS4-STATUS.                         SQ1364.2
005300*                                                                 SQ1364.2
005400*                                                                 SQ1364.2
005500 DATA DIVISION.                                                   SQ1364.2
005600 FILE SECTION.                                                    SQ1364.2
005700 FD  PRINT-FILE                                                   SQ1364.2
005800C    LABEL RECORDS                                                SQ1364.2
005900C    XXXXX084                                                     SQ1364.2
006000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1364.2
006100               .                                                  SQ1364.2
006200 01  PRINT-REC    PICTURE X(120).                                 SQ1364.2
006300 01  DUMMY-RECORD PICTURE X(120).                                 SQ1364.2
006400*                                                                 SQ1364.2
006500*FD  RAW-DATA.                                                    SQ1364.2
006600*01  RAW-DATA-SATZ.                                               SQ1364.2
006700*    05  RAW-DATA-KEY        PIC X(6).                            SQ1364.2
006800*    05  C-DATE              PIC 9(6).                            SQ1364.2
006900*    05  C-TIME              PIC 9(8).                            SQ1364.2
007000*    05  NO-OF-TESTS         PIC 99.                              SQ1364.2
007100*    05  C-OK                PIC 999.                             SQ1364.2
007200*    05  C-ALL               PIC 999.                             SQ1364.2
007300*    05  C-FAIL              PIC 999.                             SQ1364.2
007400*    05  C-DELETED           PIC 999.                             SQ1364.2
007500*    05  C-INSPECT           PIC 999.                             SQ1364.2
007600*    05  C-NOTE              PIC X(13).                           SQ1364.2
007700*    05  C-INDENT            PIC X.                               SQ1364.2
007800*    05  C-ABORT             PIC X(8).                            SQ1364.2
007900*                                                                 SQ1364.2
008000 FD  SQ-FS4                                                       SQ1364.2
008100C    LABEL RECORD IS STANDARD                                     SQ1364.2
008200     BLOCK  2 RECORDS                                             SQ1364.2
008300     RECORD 125                                                   SQ1364.2
008400                .                                                 SQ1364.2
008500 01  SQ-FS4R1-F-G-125.                                            SQ1364.2
008600     05  SQ-FS4-FIRST  PIC X(120).                                SQ1364.2
008700     05  SQ-FS4-REC-NO PIC 99999.                                 SQ1364.2
008800*                                                                 SQ1364.2
008900 WORKING-STORAGE SECTION.                                         SQ1364.2
009000*                                                                 SQ1364.2
009100***************************************************************   SQ1364.2
009200*                                                             *   SQ1364.2
009300*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1364.2
009400*                                                             *   SQ1364.2
009500***************************************************************   SQ1364.2
009600*                                                                 SQ1364.2
009700 01  SQ-FS4-STATUS.                                               SQ1364.2
009800   03  SQ-FS4-KEY-1   PIC X.                                      SQ1364.2
009900   03  SQ-FS4-KEY-2   PIC X.                                      SQ1364.2
010000*                                                                 SQ1364.2
010100 01  DELETE-SW.                                                   SQ1364.2
010200     03  DELETE-SW-1 PIC X.                                       SQ1364.2
010300     03  DELETE-SW-1-GROUP.                                       SQ1364.2
010400         05  DELETE-SW-2 PIC X.                                   SQ1364.2
010500*                                                                 SQ1364.2
010600 01  DECL-EXEC-I  PIC X(12).                                      SQ1364.2
010700 01  DECL-EXEC-O  PIC X(12).                                      SQ1364.2
010800 01  DECL-EXEC-SW PIC X.                                          SQ1364.2
010900*                                                                 SQ1364.2
011000***************************************************************   SQ1364.2
011100*                                                             *   SQ1364.2
011200*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1364.2
011300*                                                             *   SQ1364.2
011400***************************************************************   SQ1364.2
011500*                                                                 SQ1364.2
011600 01  REC-SKEL-SUB   PIC 99.                                       SQ1364.2
011700*                                                                 SQ1364.2
011800 01  FILE-RECORD-INFORMATION-REC.                                 SQ1364.2
011900     03 FILE-RECORD-INFO-SKELETON.                                SQ1364.2
012000        05 FILLER                 PICTURE X(48)       VALUE       SQ1364.2
012100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1364.2
012200        05 FILLER                 PICTURE X(46)       VALUE       SQ1364.2
012300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1364.2
012400        05 FILLER                 PICTURE X(26)       VALUE       SQ1364.2
012500             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1364.2
012600        05 FILLER                 PICTURE X(37)       VALUE       SQ1364.2
012700             ",RECKEY=                             ".             SQ1364.2
012800        05 FILLER                 PICTURE X(38)       VALUE       SQ1364.2
012900             ",ALTKEY1=                             ".            SQ1364.2
013000        05 FILLER                 PICTURE X(38)       VALUE       SQ1364.2
013100             ",ALTKEY2=                             ".            SQ1364.2
013200        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1364.2
013300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1364.2
013400        05 FILE-RECORD-INFO-P1-120.                               SQ1364.2
013500           07 FILLER              PIC X(5).                       SQ1364.2
013600           07 XFILE-NAME          PIC X(6).                       SQ1364.2
013700           07 FILLER              PIC X(8).                       SQ1364.2
013800           07 XRECORD-NAME        PIC X(6).                       SQ1364.2
013900           07 FILLER              PIC X(1).                       SQ1364.2
014000           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1364.2
014100           07 FILLER              PIC X(7).                       SQ1364.2
014200           07 XRECORD-NUMBER      PIC 9(6).                       SQ1364.2
014300           07 FILLER              PIC X(6).                       SQ1364.2
014400           07 UPDATE-NUMBER       PIC 9(2).                       SQ1364.2
014500           07 FILLER              PIC X(5).                       SQ1364.2
014600           07 ODO-NUMBER          PIC 9(4).                       SQ1364.2
014700           07 FILLER              PIC X(5).                       SQ1364.2
014800           07 XPROGRAM-NAME       PIC X(5).                       SQ1364.2
014900           07 FILLER              PIC X(7).                       SQ1364.2
015000           07 XRECORD-LENGTH      PIC 9(6).                       SQ1364.2
015100           07 FILLER              PIC X(7).                       SQ1364.2
015200           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1364.2
015300           07 FILLER              PIC X(1).                       SQ1364.2
015400           07 XBLOCK-SIZE         PIC 9(4).                       SQ1364.2
015500           07 FILLER              PIC X(6).                       SQ1364.2
015600           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1364.2
015700           07 FILLER              PIC X(5).                       SQ1364.2
015800           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1364.2
015900           07 FILLER              PIC X(6).                       SQ1364.2
016000           07 XLABEL-TYPE         PIC X(1).                       SQ1364.2
016100        05 FILE-RECORD-INFO-P121-240.                             SQ1364.2
016200           07 FILLER              PIC X(8).                       SQ1364.2
016300           07 XRECORD-KEY         PIC X(29).                      SQ1364.2
016400           07 FILLER              PIC X(9).                       SQ1364.2
016500           07 ALTERNATE-KEY1      PIC X(29).                      SQ1364.2
016600           07 FILLER              PIC X(9).                       SQ1364.2
016700           07 ALTERNATE-KEY2      PIC X(29).                      SQ1364.2
016800           07 FILLER              PIC X(7).                       SQ1364.2
016900*                                                                 SQ1364.2
017000 01  TEST-RESULTS.                                                SQ1364.2
017100     02 FILLER              PIC X      VALUE SPACE.               SQ1364.2
017200     02  PAR-NAME.                                                SQ1364.2
017300       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1364.2
017400       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1364.2
017500       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1364.2
017600     02 FILLER              PIC X      VALUE SPACE.               SQ1364.2
017700     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1364.2
017800     02 FILLER              PIC X      VALUE SPACE.               SQ1364.2
017900     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1364.2
018000     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1364.2
018100     02 RE-MARK             PIC X(61).                            SQ1364.2
018200 01  TEST-COMPUTED.                                               SQ1364.2
018300   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1364.2
018400   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1364.2
018500   02 COMPUTED-X.                                                 SQ1364.2
018600     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1364.2
018700     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1364.2
018800     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1364.2
018900     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1364.2
019000     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1364.2
019100     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1364.2
019200        04 COMPUTED-18V0                   PIC -9(18).            SQ1364.2
019300        04 FILLER                          PIC X.                 SQ1364.2
019400     03 FILLER PIC X(50) VALUE SPACE.                             SQ1364.2
019500 01  TEST-CORRECT.                                                SQ1364.2
019600     02 FILLER PIC X(30) VALUE SPACE.                             SQ1364.2
019700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1364.2
019800     02 CORRECT-X.                                                SQ1364.2
019900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1364.2
020000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1364.2
020100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1364.2
020200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1364.2
020300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1364.2
020400     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1364.2
020500         04 CORRECT-18V0                     PIC -9(18).          SQ1364.2
020600         04 FILLER                           PIC X.               SQ1364.2
020700     03 FILLER PIC X(2) VALUE SPACE.                              SQ1364.2
020800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1364.2
020900*                                                                 SQ1364.2
021000 01  CCVS-C-1.                                                    SQ1364.2
021100     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1364.2
021200     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1364.2
021300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1364.2
021400     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1364.2
021500     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1364.2
021600     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1364.2
021700     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1364.2
021800     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1364.2
021900 01  CCVS-C-2.                                                    SQ1364.2
022000     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1364.2
022100     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1364.2
022200     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1364.2
022300     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1364.2
022400     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1364.2
022500*                                                                 SQ1364.2
022600 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1364.2
022700 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1364.2
022800 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1364.2
022900 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1364.2
023000 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1364.2
023100 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1364.2
023200 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1364.2
023300 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1364.2
023400 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1364.2
023500 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1364.2
023600 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1364.2
023700 01  CCVS-H-1.                                                    SQ1364.2
023800     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1364.2
023900     02  FILLER          PIC X(42)    VALUE                       SQ1364.2
024000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1364.2
024100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1364.2
024200 01  CCVS-H-2A.                                                   SQ1364.2
024300   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1364.2
024400   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1364.2
024500   02  FILLER            PIC XXXX   VALUE                         SQ1364.2
024600     "4.2 ".                                                      SQ1364.2
024700   02  FILLER            PIC X(28)  VALUE                         SQ1364.2
024800            " COPY - NOT FOR DISTRIBUTION".                       SQ1364.2
024900   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1364.2
025000*                                                                 SQ1364.2
025100 01  CCVS-H-2B.                                                   SQ1364.2
025200   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1364.2
025300   02  TEST-ID           PIC X(9).                                SQ1364.2
025400   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1364.2
025500   02  FILLER            PIC X(12)  VALUE                         SQ1364.2
025600     " HIGH       ".                                              SQ1364.2
025700   02  FILLER            PIC X(22)  VALUE                         SQ1364.2
025800            " LEVEL VALIDATION FOR ".                             SQ1364.2
025900   02  FILLER            PIC X(58)  VALUE                         SQ1364.2
026000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1364.2
026100 01  CCVS-H-3.                                                    SQ1364.2
026200     02  FILLER          PIC X(34)  VALUE                         SQ1364.2
026300            " FOR OFFICIAL USE ONLY    ".                         SQ1364.2
026400     02  FILLER          PIC X(58)  VALUE                         SQ1364.2
026500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1364.2
026600     02  FILLER          PIC X(28)  VALUE                         SQ1364.2
026700            "  COPYRIGHT   1985,1986 ".                           SQ1364.2
026800 01  CCVS-E-1.                                                    SQ1364.2
026900     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1364.2
027000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1364.2
027100     02 ID-AGAIN         PIC X(9).                                SQ1364.2
027200     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1364.2
027300 01  CCVS-E-2.                                                    SQ1364.2
027400     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1364.2
027500     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1364.2
027600     02  CCVS-E-2-2.                                              SQ1364.2
027700         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1364.2
027800         03 FILLER         PIC X      VALUE SPACE.                SQ1364.2
027900         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1364.2
028000            "ERRORS ENCOUNTERED".                                 SQ1364.2
028100 01  CCVS-E-3.                                                    SQ1364.2
028200     02  FILLER          PIC X(22)  VALUE                         SQ1364.2
028300            " FOR OFFICIAL USE ONLY".                             SQ1364.2
028400     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1364.2
028500     02  FILLER          PIC X(58)  VALUE                         SQ1364.2
028600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1364.2
028700     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1364.2
028800     02  FILLER          PIC X(20)  VALUE                         SQ1364.2
028900             " COPYRIGHT 1985,1986".                              SQ1364.2
029000 01  CCVS-E-4.                                                    SQ1364.2
029100     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1364.2
029200     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1364.2
029300     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1364.2
029400     02 FILLER           PIC X(40)  VALUE                         SQ1364.2
029500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1364.2
029600 01  XXINFO.                                                      SQ1364.2
029700     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1364.2
029800     02 INFO-TEXT.                                                SQ1364.2
029900       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1364.2
030000       04 XXCOMPUTED         PIC X(20).                           SQ1364.2
030100       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1364.2
030200       04 XXCORRECT          PIC X(20).                           SQ1364.2
030300     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1364.2
030400 01  HYPHEN-LINE.                                                 SQ1364.2
030500     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1364.2
030600     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1364.2
030700-    "*****************************************".                 SQ1364.2
030800     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1364.2
030900-    "******************************".                            SQ1364.2
031000 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1364.2
031100     "SQ136A".                                                    SQ1364.2
031200*                                                                 SQ1364.2
031300*                                                                 SQ1364.2
031400 PROCEDURE DIVISION.                                              SQ1364.2
031500 DECLARATIVES.                                                    SQ1364.2
031600*                                                                 SQ1364.2
031700*    FIRST, A DECLARATIVE FOR THE PRINT FILE, IN CASE THERE       SQ1364.2
031800*    IS AN ERROR ON PRINTING DURING EXECUTION OF THE OUTPUT       SQ1364.2
031900*    DECLARATIVE.  PAGE VII-51, 4.6.4(7) STATES THAT THE          SQ1364.2
032000*    INVOCATION OF THE SAME DECLARATIVE TWICE IS ILLEGAL.         SQ1364.2
032100*                                                                 SQ1364.2
032200 SECT-SQ136A-0000 SECTION.                                        SQ1364.2
032300     USE AFTER STANDARD ERROR PROCEDURE ON PRINT-FILE.            SQ1364.2
032400 PRINT-FILE-ERROR-PROCESS.                                        SQ1364.2
032500     EXIT.                                                        SQ1364.2
032600*                                                                 SQ1364.2
032700 SECT-SQ136A-0001 SECTION.                                        SQ1364.2
032800     USE AFTER STANDARD EXCEPTION PROCEDURE OUTPUT.               SQ1364.2
032900 OUTPUT-ERROR-PROCESS.                                            SQ1364.2
033000     MOVE   "EXECUTED" TO DECL-EXEC-O.                            SQ1364.2
033100*                                                                 SQ1364.2
033200 SECT-SQ136A-0002 SECTION.                                        SQ1364.2
033300     USE AFTER EXCEPTION PROCEDURE INPUT.                         SQ1364.2
033400 INPUT-ERROR-PROCESS.                                             SQ1364.2
033500     MOVE   "EXECUTED" TO DECL-EXEC-I.                            SQ1364.2
033600*                                                                 SQ1364.2
033700     IF DECL-EXEC-SW NOT = SPACE                                  SQ1364.2
033800         GO TO   END-DECLS.                                       SQ1364.2
033900*                                                                 SQ1364.2
034000     MOVE    1 TO REC-CT.                                         SQ1364.2
034100     MOVE   "READ AFTER EOF READ" TO FEATURE.                     SQ1364.2
034200     MOVE   "DECL-EOF-READ" TO PAR-NAME.                          SQ1364.2
034300     GO TO   DECL-EOF-READ-01.                                    SQ1364.2
034400 DECL-DELETE-01.                                                  SQ1364.2
034500     PERFORM DECL-DE-LETE.                                        SQ1364.2
034600     GO TO   DECL-TEST-01-END.                                    SQ1364.2
034700 DECL-EOF-READ-01.                                                SQ1364.2
034800 DECL-TEST-01-END.                                                SQ1364.2
034900*                                                                 SQ1364.2
035000     ADD     1 TO REC-CT.                                         SQ1364.2
035100     GO TO   DECL-EOF-READ-02.                                    SQ1364.2
035200 DECL-DELETE-02.                                                  SQ1364.2
035300     PERFORM DECL-DE-LETE.                                        SQ1364.2
035400     GO TO   DECL-TEST-02-END.                                    SQ1364.2
035500 DECL-EOF-READ-02.                                                SQ1364.2
035600 DECL-TEST-02-END.                                                SQ1364.2
035700*                                                                 SQ1364.2
035800     MOVE    SPACE TO DUMMY-RECORD.                               SQ1364.2
035900     PERFORM DECL-WRITE-LINE.                                     SQ1364.2
036000     MOVE   "ABNORMAL TERMINATION OF PROGRAM HERE IS ACCEPTABLE"  SQ1364.2
036100                TO DUMMY-RECORD.                                  SQ1364.2
036200     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1364.2
036300     GO TO   END-DECLS.                                           SQ1364.2
036400*                                                                 SQ1364.2
036500*                                                                 SQ1364.2
036600 DECL-PASS.                                                       SQ1364.2
036700     MOVE   "PASS " TO P-OR-F.                                    SQ1364.2
036800     ADD     1 TO PASS-COUNTER.                                   SQ1364.2
036900     PERFORM DECL-PRINT-DETAIL.                                   SQ1364.2
037000*                                                                 SQ1364.2
037100 DECL-FAIL.                                                       SQ1364.2
037200     MOVE   "FAIL*" TO P-OR-F.                                    SQ1364.2
037300     ADD     1 TO ERROR-COUNTER.                                  SQ1364.2
037400     PERFORM DECL-PRINT-DETAIL.                                   SQ1364.2
037500*                                                                 SQ1364.2
037600 DECL-DE-LETE.                                                    SQ1364.2
037700     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1364.2
037800     MOVE   "*****" TO P-OR-F.                                    SQ1364.2
037900     ADD     1 TO DELETE-COUNTER.                                 SQ1364.2
038000     PERFORM DECL-PRINT-DETAIL.                                   SQ1364.2
038100*                                                                 SQ1364.2
038200 DECL-PRINT-DETAIL.                                               SQ1364.2
038300     IF REC-CT NOT EQUAL TO ZERO                                  SQ1364.2
038400             MOVE "." TO PARDOT-X                                 SQ1364.2
038500             MOVE REC-CT TO DOTVALUE.                             SQ1364.2
038600     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1364.2
038700     PERFORM DECL-WRITE-LINE.                                     SQ1364.2
038800     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1364.2
038900         PERFORM DECL-WRITE-LINE                                  SQ1364.2
039000         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1364.2
039100     ELSE                                                         SQ1364.2
039200         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1364.2
039300     MOVE    SPACE TO P-OR-F.                                     SQ1364.2
039400     MOVE    SPACE TO COMPUTED-X.                                 SQ1364.2
039500     MOVE    SPACE TO CORRECT-X.                                  SQ1364.2
039600     IF REC-CT EQUAL TO ZERO                                      SQ1364.2
039700         MOVE    SPACE TO PAR-NAME.                               SQ1364.2
039800     MOVE    SPACE TO RE-MARK.                                    SQ1364.2
039900*                                                                 SQ1364.2
040000 DECL-WRITE-LINE.                                                 SQ1364.2
040100     ADD     1 TO RECORD-COUNT.                                   SQ1364.2
040200*    IF RECORD-COUNT GREATER 50                                   SQ1364.2
040300*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1364.2
040400*        MOVE    SPACE TO DUMMY-RECORD                            SQ1364.2
040500*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1364.2
040600*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1364.2
040700*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1364.2
040800*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1364.2
040900*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1364.2
041000*        PERFORM DECL-WRT-LN                                      SQ1364.2
041100*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1364.2
041200*        MOVE    ZERO TO RECORD-COUNT.                            SQ1364.2
041300     PERFORM DECL-WRT-LN.                                         SQ1364.2
041400*                                                                 SQ1364.2
041500 DECL-WRT-LN.                                                     SQ1364.2
041600     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1364.2
041700     MOVE    SPACE TO DUMMY-RECORD.                               SQ1364.2
041800*                                                                 SQ1364.2
041900 DECL-FAIL-ROUTINE.                                               SQ1364.2
042000     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1364.2
042100     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1364.2
042200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1364.2
042300     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1364.2
042400     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1364.2
042500     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1364.2
042600     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1364.2
042700     GO TO   DECL-FAIL-EX.                                        SQ1364.2
042800 DECL-FAIL-WRITE.                                                 SQ1364.2
042900     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1364.2
043000     PERFORM DECL-WRITE-LINE                                      SQ1364.2
043100     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1364.2
043200     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1364.2
043300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1364.2
043400     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1364.2
043500 DECL-FAIL-EX.                                                    SQ1364.2
043600     EXIT.                                                        SQ1364.2
043700*                                                                 SQ1364.2
043800 DECL-BAIL.                                                       SQ1364.2
043900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1364.2
044000     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1364.2
044100 DECL-BAIL-WRITE.                                                 SQ1364.2
044200     MOVE    CORRECT-A TO XXCORRECT.                              SQ1364.2
044300     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1364.2
044400     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1364.2
044500     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1364.2
044600 DECL-BAIL-EX.                                                    SQ1364.2
044700     EXIT.                                                        SQ1364.2
044800*                                                                 SQ1364.2
044900 END-DECLS.                                                       SQ1364.2
045000 END DECLARATIVES.                                                SQ1364.2
045100*                                                                 SQ1364.2
045200*                                                                 SQ1364.2
045300 CCVS1 SECTION.                                                   SQ1364.2
045400 OPEN-FILES.                                                      SQ1364.2
045500*    OPEN    I-O RAW-DATA.                                        SQ1364.2
045600*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1364.2
045700*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1364.2
045800*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1364.2
045900*    ADD     1           TO C-NO-OF-TESTS.                        SQ1364.2
046000*    ACCEPT  C-DATE      FROM DATE.                               SQ1364.2
046100*    ACCEPT  C-TIME      FROM TIME.                               SQ1364.2
046200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1364.2
046300*END-E-1.                                                         SQ1364.2
046400*    CLOSE   RAW-DATA.                                            SQ1364.2
046500     OPEN    OUTPUT PRINT-FILE.                                   SQ1364.2
046600     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1364.2
046700     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1364.2
046800     MOVE    SPACE TO TEST-RESULTS.                               SQ1364.2
046900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1364.2
047000     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1364.2
047100     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1364.2
047200     GO TO CCVS1-EXIT.                                            SQ1364.2
047300*                                                                 SQ1364.2
047400 CCVS-INIT-FILE.                                                  SQ1364.2
047500     ADD     1 TO REC-SKL-SUB.                                    SQ1364.2
047600     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1364.2
047700                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1364.2
047800*                                                                 SQ1364.2
047900 CLOSE-FILES.                                                     SQ1364.2
048000     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1364.2
048100     CLOSE   PRINT-FILE.                                          SQ1364.2
048200*    OPEN    I-O RAW-DATA.                                        SQ1364.2
048300*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1364.2
048400*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1364.2
048500*    MOVE   "OK.     " TO C-ABORT.                                SQ1364.2
048600*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1364.2
048700*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1364.2
048800*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1364.2
048900*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1364.2
049000*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1364.2
049100*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1364.2
049200*END-E-2.                                                         SQ1364.2
049300*    CLOSE   RAW-DATA.                                            SQ1364.2
049400 TERMINATE-CCVS.                                                  SQ1364.2
049500*    EXIT    PROGRAM.                                             SQ1364.2
049600     STOP    RUN.                                                 SQ1364.2
049700*                                                                 SQ1364.2
049800 INSPT.                                                           SQ1364.2
049900     MOVE   "INSPT" TO P-OR-F.                                    SQ1364.2
050000     ADD     1 TO INSPECT-COUNTER.                                SQ1364.2
050100     PERFORM PRINT-DETAIL.                                        SQ1364.2
050200*                                                                 SQ1364.2
050300 PASS.                                                            SQ1364.2
050400     MOVE   "PASS " TO P-OR-F.                                    SQ1364.2
050500     ADD     1 TO PASS-COUNTER.                                   SQ1364.2
050600     PERFORM PRINT-DETAIL.                                        SQ1364.2
050700*                                                                 SQ1364.2
050800 FAIL.                                                            SQ1364.2
050900     MOVE   "FAIL*" TO P-OR-F.                                    SQ1364.2
051000     ADD     1 TO ERROR-COUNTER.                                  SQ1364.2
051100     PERFORM PRINT-DETAIL.                                        SQ1364.2
051200*                                                                 SQ1364.2
051300 DE-LETE.                                                         SQ1364.2
051400     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1364.2
051500     MOVE   "*****" TO P-OR-F.                                    SQ1364.2
051600     ADD     1 TO DELETE-COUNTER.                                 SQ1364.2
051700     PERFORM PRINT-DETAIL.                                        SQ1364.2
051800*                                                                 SQ1364.2
051900 PRINT-DETAIL.                                                    SQ1364.2
052000     IF REC-CT NOT EQUAL TO ZERO                                  SQ1364.2
052100         MOVE   "." TO PARDOT-X                                   SQ1364.2
052200         MOVE    REC-CT TO DOTVALUE.                              SQ1364.2
052300     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1364.2
052400     PERFORM WRITE-LINE.                                          SQ1364.2
052500     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1364.2
052600         PERFORM WRITE-LINE                                       SQ1364.2
052700         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1364.2
052800     ELSE                                                         SQ1364.2
052900         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1364.2
053000     MOVE    SPACE TO P-OR-F.                                     SQ1364.2
053100     MOVE    SPACE TO COMPUTED-X.                                 SQ1364.2
053200     MOVE    SPACE TO CORRECT-X.                                  SQ1364.2
053300     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1364.2
053400     MOVE    SPACE TO RE-MARK.                                    SQ1364.2
053500*                                                                 SQ1364.2
053600 HEAD-ROUTINE.                                                    SQ1364.2
053700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1364.2
053800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1364.2
053900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1364.2
054000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1364.2
054100 COLUMN-NAMES-ROUTINE.                                            SQ1364.2
054200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1364.2
054300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1364.2
054400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1364.2
054500 END-ROUTINE.                                                     SQ1364.2
054600     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1364.2
054700     PERFORM WRITE-LINE 5 TIMES.                                  SQ1364.2
054800 END-RTN-EXIT.                                                    SQ1364.2
054900     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1364.2
055000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1364.2
055100*                                                                 SQ1364.2
055200 END-ROUTINE-1.                                                   SQ1364.2
055300     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1364.2
055400     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1364.2
055500     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1364.2
055600     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1364.2
055700     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1364.2
055800     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1364.2
055900     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1364.2
056000     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1364.2
056100     PERFORM WRITE-LINE.                                          SQ1364.2
056200     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1364.2
056300     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1364.2
056400         MOVE   "NO " TO ERROR-TOTAL                              SQ1364.2
056500     ELSE                                                         SQ1364.2
056600         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1364.2
056700     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1364.2
056800     PERFORM WRITE-LINE.                                          SQ1364.2
056900 END-ROUTINE-13.                                                  SQ1364.2
057000     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1364.2
057100         MOVE   "NO " TO ERROR-TOTAL                              SQ1364.2
057200     ELSE                                                         SQ1364.2
057300         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1364.2
057400     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1364.2
057500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1364.2
057600     PERFORM WRITE-LINE.                                          SQ1364.2
057700     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1364.2
057800         MOVE   "NO " TO ERROR-TOTAL                              SQ1364.2
057900     ELSE                                                         SQ1364.2
058000         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1364.2
058100     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1364.2
058200     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1364.2
058300     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1364.2
058400*                                                                 SQ1364.2
058500 WRITE-LINE.                                                      SQ1364.2
058600     ADD     1 TO RECORD-COUNT.                                   SQ1364.2
058700*    IF RECORD-COUNT GREATER 50                                   SQ1364.2
058800*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1364.2
058900*        MOVE  SPACE TO DUMMY-RECORD                              SQ1364.2
059000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1364.2
059100*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1364.2
059200*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1364.2
059300*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1364.2
059400*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1364.2
059500*        MOVE  ZERO TO RECORD-COUNT.                              SQ1364.2
059600     PERFORM WRT-LN.                                              SQ1364.2
059700*                                                                 SQ1364.2
059800 WRT-LN.                                                          SQ1364.2
059900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1364.2
060000     MOVE    SPACE TO DUMMY-RECORD.                               SQ1364.2
060100 BLANK-LINE-PRINT.                                                SQ1364.2
060200     PERFORM WRT-LN.                                              SQ1364.2
060300 FAIL-ROUTINE.                                                    SQ1364.2
060400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1364.2
060500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1364.2
060600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1364.2
060700     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1364.2
060800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1364.2
060900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1364.2
061000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1364.2
061100     GO TO   FAIL-ROUTINE-EX.                                     SQ1364.2
061200 FAIL-ROUTINE-WRITE.                                              SQ1364.2
061300     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1364.2
061400     PERFORM WRITE-LINE                                           SQ1364.2
061500     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1364.2
061600     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1364.2
061700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1364.2
061800     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1364.2
061900 FAIL-ROUTINE-EX.                                                 SQ1364.2
062000     EXIT.                                                        SQ1364.2
062100 BAIL-OUT.                                                        SQ1364.2
062200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1364.2
062300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1364.2
062400 BAIL-OUT-WRITE.                                                  SQ1364.2
062500     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1364.2
062600     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1364.2
062700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1364.2
062800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1364.2
062900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1364.2
063000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1364.2
063100 BAIL-OUT-EX.                                                     SQ1364.2
063200     EXIT.                                                        SQ1364.2
063300 CCVS1-EXIT.                                                      SQ1364.2
063400     EXIT.                                                        SQ1364.2
063500*                                                                 SQ1364.2
063600****************************************************************  SQ1364.2
063700*                                                              *  SQ1364.2
063800*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1364.2
063900*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1364.2
064000*                                                              *  SQ1364.2
064100****************************************************************  SQ1364.2
064200*                                                                 SQ1364.2
064300 SECT-SQ136A-0004 SECTION.                                        SQ1364.2
064400 STA-INIT.                                                        SQ1364.2
064500     MOVE    SPACE TO DELETE-SW.                                  SQ1364.2
064600*                                                                 SQ1364.2
064700     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1364.2
064800     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1364.2
064900     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1364.2
065000     MOVE    125 TO XRECORD-LENGTH (1).                           SQ1364.2
065100     MOVE   "RC" TO CHARS-OR-RECORDS (1).                         SQ1364.2
065200     MOVE    2   TO XBLOCK-SIZE (1).                              SQ1364.2
065300     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ1364.2
065400     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1364.2
065500     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1364.2
065600     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1364.2
065700*                                                                 SQ1364.2
065800*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1364.2
065900*                                                                 SQ1364.2
066000 SEQ-INIT-01.                                                     SQ1364.2
066100     MOVE    0 TO REC-CT.                                         SQ1364.2
066200     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1364.2
066300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
066400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
066500     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
066600     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1364.2
066700     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1364.2
066800     GO TO   SEQ-TEST-OP-01.                                      SQ1364.2
066900 SEQ-DELETE-01.                                                   SQ1364.2
067000     MOVE   "*" TO DELETE-SW-1.                                   SQ1364.2
067100 SEQ-TEST-OP-01.                                                  SQ1364.2
067200     OPEN    OUTPUT SQ-FS4.                                       SQ1364.2
067300 SEQ-INIT-02.                                                     SQ1364.2
067400     MOVE    0 TO REC-CT.                                         SQ1364.2
067500     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1364.2
067600     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
067700     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
067800     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
067900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1364.2
068000     MOVE   "WRITE ONE RECORD"  TO FEATURE.                       SQ1364.2
068100     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1364.2
068200     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1364.2
068300         GO TO SEQ-DELETE-02.                                     SQ1364.2
068400     GO TO   SEQ-TEST-WR-02.                                      SQ1364.2
068500 SEQ-DELETE-02.                                                   SQ1364.2
068600     MOVE   "*" TO DELETE-SW-2.                                   SQ1364.2
068700 SEQ-TEST-WR-02.                                                  SQ1364.2
068800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4-FIRST.         SQ1364.2
068900     MOVE    XRECORD-NUMBER (1) TO SQ-FS4-REC-NO.                 SQ1364.2
069000     WRITE   SQ-FS4R1-F-G-125.                                    SQ1364.2
069100 SEQ-INIT-03.                                                     SQ1364.2
069200     MOVE    0 TO REC-CT.                                         SQ1364.2
069300     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1364.2
069400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
069500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
069600     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
069700     MOVE   "CLOSE AFTER CREATE"  TO FEATURE.                     SQ1364.2
069800     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ1364.2
069900     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1364.2
070000         GO TO SEQ-DELETE-03.                                     SQ1364.2
070100     GO TO   SEQ-TEST-CL-03.                                      SQ1364.2
070200 SEQ-DELETE-03.                                                   SQ1364.2
070300     MOVE   "*" TO DELETE-SW-2.                                   SQ1364.2
070400 SEQ-TEST-CL-03.                                                  SQ1364.2
070500     CLOSE   SQ-FS4.                                              SQ1364.2
070600 SEQ-INIT-04.                                                     SQ1364.2
070700     MOVE    0 TO REC-CT.                                         SQ1364.2
070800     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1364.2
070900     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1364.2
071000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
071100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
071200     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
071300     MOVE   "OPEN, TO READ FILE"  TO FEATURE.                     SQ1364.2
071400     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ1364.2
071500     IF DELETE-SW NOT = SPACE                                     SQ1364.2
071600         GO TO SEQ-DELETE-04.                                     SQ1364.2
071700     GO TO   SEQ-TEST-OP-04.                                      SQ1364.2
071800 SEQ-DELETE-04.                                                   SQ1364.2
071900     MOVE   "*" TO DELETE-SW-2.                                   SQ1364.2
072000 SEQ-TEST-OP-04.                                                  SQ1364.2
072100     OPEN    INPUT SQ-FS4.                                        SQ1364.2
072200     MOVE    SPACE TO SQ-FS4R1-F-G-125.                           SQ1364.2
072300*                                                                 SQ1364.2
072400*                                                                 SQ1364.2
072500*    READ THE FIRST (AND ONLY) RECORD FROM THE FILE               SQ1364.2
072600*                                                                 SQ1364.2
072700 SEQ-INIT-05.                                                     SQ1364.2
072800     MOVE    0 TO REC-CT.                                         SQ1364.2
072900     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1364.2
073000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
073100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
073200     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
073300     MOVE   "READ FIRST RECORD"  TO FEATURE.                      SQ1364.2
073400     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ1364.2
073500     IF DELETE-SW NOT = SPACE                                     SQ1364.2
073600         GO TO SEQ-DELETE-05.                                     SQ1364.2
073700     GO TO   SEQ-TEST-RD-05.                                      SQ1364.2
073800 SEQ-DELETE-05.                                                   SQ1364.2
073900     MOVE   "*" TO DELETE-SW-2.                                   SQ1364.2
074000 SEQ-TEST-RD-05.                                                  SQ1364.2
074100     READ    SQ-FS4.                                              SQ1364.2
074200     MOVE    SQ-FS4R1-F-G-125 TO FILE-RECORD-INFO (2).            SQ1364.2
074300*                                                                 SQ1364.2
074400*                                                                 SQ1364.2
074500*    READ AGAIN, TO RAISE THE AT END CONDITION                    SQ1364.2
074600*                                                                 SQ1364.2
074700 SEQ-INIT-06.                                                     SQ1364.2
074800     MOVE    0 TO REC-CT.                                         SQ1364.2
074900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1364.2
075000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
075100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
075200     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
075300     MOVE   "READ, GIVING AT END"  TO FEATURE.                    SQ1364.2
075400     MOVE   "SEQ-TEST-RD-06" TO PAR-NAME.                         SQ1364.2
075500     IF DELETE-SW NOT = SPACE                                     SQ1364.2
075600         GO TO SEQ-DELETE-06.                                     SQ1364.2
075700     GO TO   SEQ-TEST-RD-06.                                      SQ1364.2
075800 SEQ-DELETE-06.                                                   SQ1364.2
075900     MOVE   "*" TO DELETE-SW-2.                                   SQ1364.2
076000 SEQ-TEST-RD-06.                                                  SQ1364.2
076100     READ    SQ-FS4.                                              SQ1364.2
076200*                                                                 SQ1364.2
076300*                                                                 SQ1364.2
076400*    READ AGAIN, AFTER AT END, TO RAISE I-O STATUS 46             SQ1364.2
076500*                                                                 SQ1364.2
076600 SEQ-INIT-07.                                                     SQ1364.2
076700     MOVE    0 TO REC-CT.                                         SQ1364.2
076800     MOVE    SPACE TO DECL-EXEC-SW.                               SQ1364.2
076900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1364.2
077000     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1364.2
077100     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1364.2
077200     MOVE   "READ AFTER AT END"  TO FEATURE.                      SQ1364.2
077300     MOVE   "SEQ-TEST-RD-07" TO PAR-NAME.                         SQ1364.2
077400     IF DELETE-SW NOT = SPACE                                     SQ1364.2
077500         GO TO SEQ-DELETE-07.                                     SQ1364.2
077600     GO TO   SEQ-TEST-RD-07.                                      SQ1364.2
077700 SEQ-DELETE-07.                                                   SQ1364.2
077800     MOVE   "*" TO DELETE-SW-2.                                   SQ1364.2
077900     GO TO   SEQ-DELETE-07-01.                                    SQ1364.2
078000 SEQ-TEST-RD-07.                                                  SQ1364.2
078100     READ    SQ-FS4.                                              SQ1364.2
078200*                                                                 SQ1364.2
078300*    THE TESTS FOLLOWING THIS READ STATEMENT MAY NOT BE           SQ1364.2
078400*    EXECUTED.  THE IMPLEMENTOR MAY LEGITIMATELY TERMINATE        SQ1364.2
078500*    EXECUTION ON EXIT FROM THE DECLARATIVE.                      SQ1364.2
078600*                                                                 SQ1364.2
078700     MOVE    ZERO TO REC-CT.                                      SQ1364.2
078800     MOVE   "READ AFTER AT END"  TO FEATURE.                      SQ1364.2
078900     MOVE   "SEQ-TEST-RD-07" TO PAR-NAME.                         SQ1364.2
079000*                                                                 SQ1364.2
079100*    CHECK I-O STATUS RETURNED FROM READ AFTER AT END             SQ1364.2
079200*                                                                 SQ1364.2
079300     ADD    1 TO REC-CT.                                          SQ1364.2
079400     IF DELETE-SW NOT = SPACE                                     SQ1364.2
079500         GO TO   SEQ-DELETE-07-01.                                SQ1364.2
079600     GO TO   SEQ-TEST-RD-07-01.                                   SQ1364.2
079700 SEQ-DELETE-07-01.                                                SQ1364.2
079800     PERFORM DE-LETE.                                             SQ1364.2
079900     GO TO   SEQ-TEST-07-01-END.                                  SQ1364.2
080000 SEQ-TEST-RD-07-01.                                               SQ1364.2
080100     IF SQ-FS4-STATUS = "46"                                      SQ1364.2
080200         PERFORM PASS                                             SQ1364.2
080300     ELSE                                                         SQ1364.2
080400         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1364.2
080500         MOVE   "46" TO CORRECT-A                                 SQ1364.2
080600         MOVE   "UNEXPECTED I-O STATUS BEYOND END OF FILE"        SQ1364.2
080700                   TO RE-MARK                                     SQ1364.2
080800         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1364.2
080900         PERFORM FAIL.                                            SQ1364.2
081000 SEQ-TEST-07-01-END.                                              SQ1364.2
081100 CCVS-EXIT SECTION.                                               SQ1364.2
081200 CCVS-999999.                                                     SQ1364.2
081300     GO TO   CLOSE-FILES.                                         SQ1364.2
      *END-OF,SQ136A                                                                  
