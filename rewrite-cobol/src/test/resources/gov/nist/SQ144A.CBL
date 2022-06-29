      *HEADER,COBOL,SQ144A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1444.2
000200 PROGRAM-ID.                                                      SQ1444.2
000300     SQ144A.                                                      SQ1444.2
000400****************************************************************  SQ1444.2
000500*                                                              *  SQ1444.2
000600*    VALIDATION FOR:-                                          *  SQ1444.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1444.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1444.2
000900*                             REVISED 1986, AUGUST             *  SQ1444.2
001000*                                                              *  SQ1444.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1444.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1444.2
001300*                                                              *  SQ1444.2
001400****************************************************************  SQ1444.2
001500*                                                              *  SQ1444.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1444.2
001700*                                                              *  SQ1444.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1444.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1444.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1444.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1444.2
002200*                                                              *  SQ1444.2
002300****************************************************************  SQ1444.2
002400*                                                              *  SQ1444.2
002500*    SPLIT FROM SQ133A, THIS PROGRAM REPEATS THE SEQUENCE OF   *  SQ1444.2
002600*    FILE HANDLING ROUTINES IN ORDER TO CARRY OUT THE ISOLATED *  SQ1444.2
002700*    SPLIT TEST THAT CHECKS FOR THE CORRECT RESPONSE WHEN AN   *  SQ1444.2
002800*    ATTEMPT IS MADE TO REWRITE AFTER AT END.  (SEE SQ133A).   *  SQ1444.2
002900*                                                              *  SQ1444.2
003000****************************************************************  SQ1444.2
003100*                                                                 SQ1444.2
003200 ENVIRONMENT DIVISION.                                            SQ1444.2
003300 CONFIGURATION SECTION.                                           SQ1444.2
003400 SOURCE-COMPUTER.                                                 SQ1444.2
003500     XXXXX082.                                                    SQ1444.2
003600 OBJECT-COMPUTER.                                                 SQ1444.2
003700     XXXXX083.                                                    SQ1444.2
003800*                                                                 SQ1444.2
003900 INPUT-OUTPUT SECTION.                                            SQ1444.2
004000 FILE-CONTROL.                                                    SQ1444.2
004100     SELECT PRINT-FILE ASSIGN TO                                  SQ1444.2
004200     XXXXX055.                                                    SQ1444.2
004300*                                                                 SQ1444.2
004400*    SELECT RAW-DATA   ASSIGN TO                                  SQ1444.2
004500*    XXXXX062                                                     SQ1444.2
004600*          ORGANIZATION IS INDEXED                                SQ1444.2
004700*          ACCESS MODE  IS RANDOM                                 SQ1444.2
004800*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1444.2
004900*                                                                 SQ1444.2
005000     SELECT SQ-FS4                                                SQ1444.2
005100            ASSIGN                                                SQ1444.2
005200     XXXXX014                                                     SQ1444.2
005300            STATUS SQ-FS4-STATUS OF STATUS-GROUP                  SQ1444.2
005400            SEQUENTIAL                                            SQ1444.2
005500            .                                                     SQ1444.2
005600*                                                                 SQ1444.2
005700*                                                                 SQ1444.2
005800 DATA DIVISION.                                                   SQ1444.2
005900 FILE SECTION.                                                    SQ1444.2
006000 FD  PRINT-FILE                                                   SQ1444.2
006100C    LABEL RECORDS                                                SQ1444.2
006200C    XXXXX084                                                     SQ1444.2
006300C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1444.2
006400               .                                                  SQ1444.2
006500 01  PRINT-REC    PICTURE X(120).                                 SQ1444.2
006600 01  DUMMY-RECORD PICTURE X(120).                                 SQ1444.2
006700*                                                                 SQ1444.2
006800*FD  RAW-DATA.                                                    SQ1444.2
006900*01  RAW-DATA-SATZ.                                               SQ1444.2
007000*    05  RAW-DATA-KEY        PIC X(6).                            SQ1444.2
007100*    05  C-DATE              PIC 9(6).                            SQ1444.2
007200*    05  C-TIME              PIC 9(8).                            SQ1444.2
007300*    05  NO-OF-TESTS         PIC 99.                              SQ1444.2
007400*    05  C-OK                PIC 999.                             SQ1444.2
007500*    05  C-ALL               PIC 999.                             SQ1444.2
007600*    05  C-FAIL              PIC 999.                             SQ1444.2
007700*    05  C-DELETED           PIC 999.                             SQ1444.2
007800*    05  C-INSPECT           PIC 999.                             SQ1444.2
007900*    05  C-NOTE              PIC X(13).                           SQ1444.2
008000*    05  C-INDENT            PIC X.                               SQ1444.2
008100*    05  C-ABORT             PIC X(8).                            SQ1444.2
008200*                                                                 SQ1444.2
008300 FD  SQ-FS4                                                       SQ1444.2
008400C    LABEL RECORD IS STANDARD                                     SQ1444.2
008500     BLOCK  120 CHARACTERS                                        SQ1444.2
008600     RECORD CONTAINS 120 CHARACTERS                               SQ1444.2
008700                .                                                 SQ1444.2
008800 01  SQ-FS4R1-F-G-120.                                            SQ1444.2
008900        05 FFILE-RECORD-INFO-P1-120.                              SQ1444.2
009000           07 FILLER              PIC X(5).                       SQ1444.2
009100           07 FFILE-NAME          PIC X(6).                       SQ1444.2
009200           07 FILLER              PIC X(8).                       SQ1444.2
009300           07 FRECORD-NAME        PIC X(6).                       SQ1444.2
009400           07 FILLER              PIC X(1).                       SQ1444.2
009500           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ1444.2
009600           07 FILLER              PIC X(7).                       SQ1444.2
009700           07 FRECORD-NUMBER      PIC 9(6).                       SQ1444.2
009800           07 FILLER              PIC X(6).                       SQ1444.2
009900           07 FUPDATE-NUMBER      PIC 9(2).                       SQ1444.2
010000           07 FILLER              PIC X(5).                       SQ1444.2
010100           07 FODO-NUMBER         PIC 9(4).                       SQ1444.2
010200           07 FILLER              PIC X(5).                       SQ1444.2
010300           07 FPROGRAM-NAME       PIC X(5).                       SQ1444.2
010400           07 FILLER              PIC X(7).                       SQ1444.2
010500           07 FRECORD-LENGTH      PIC 9(6).                       SQ1444.2
010600           07 FILLER              PIC X(7).                       SQ1444.2
010700           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ1444.2
010800           07 FILLER              PIC X(1).                       SQ1444.2
010900           07 FBLOCK-SIZE         PIC 9(4).                       SQ1444.2
011000           07 FILLER              PIC X(6).                       SQ1444.2
011100           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ1444.2
011200           07 FILLER              PIC X(5).                       SQ1444.2
011300           07 FFILE-ORGANIZATION  PIC X(2).                       SQ1444.2
011400           07 FILLER              PIC X(6).                       SQ1444.2
011500           07 FLABEL-TYPE         PIC X(1).                       SQ1444.2
011600*                                                                 SQ1444.2
011700 WORKING-STORAGE SECTION.                                         SQ1444.2
011800*                                                                 SQ1444.2
011900***************************************************************   SQ1444.2
012000*                                                             *   SQ1444.2
012100*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1444.2
012200*                                                             *   SQ1444.2
012300***************************************************************   SQ1444.2
012400*                                                                 SQ1444.2
012500 01  STATUS-GROUP.                                                SQ1444.2
012600     04  SQ-FS4-STATUS.                                           SQ1444.2
012700         07  SQ-FS4-KEY-1   PIC X.                                SQ1444.2
012800         07  SQ-FS4-KEY-2   PIC X.                                SQ1444.2
012900*                                                                 SQ1444.2
013000 01  DELETE-SW.                                                   SQ1444.2
013100     03  DELETE-SW-1 PIC X.                                       SQ1444.2
013200     03  DELETE-SW-1-GROUP.                                       SQ1444.2
013300         05  DELETE-SW-2 PIC X.                                   SQ1444.2
013400*                                                                 SQ1444.2
013500 01  DECL-EXEC-I-O PIC X(12).                                     SQ1444.2
013600*                                                                 SQ1444.2
013700 01  DECL-EXEC-SW PIC X.                                          SQ1444.2
013800*                                                                 SQ1444.2
013900***************************************************************   SQ1444.2
014000*                                                             *   SQ1444.2
014100*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1444.2
014200*                                                             *   SQ1444.2
014300***************************************************************   SQ1444.2
014400*                                                                 SQ1444.2
014500 01  REC-SKEL-SUB   PIC 99.                                       SQ1444.2
014600*                                                                 SQ1444.2
014700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1444.2
014800     03 FILE-RECORD-INFO-SKELETON.                                SQ1444.2
014900        05 FILLER                 PICTURE X(48)       VALUE       SQ1444.2
015000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1444.2
015100        05 FILLER                 PICTURE X(46)       VALUE       SQ1444.2
015200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1444.2
015300        05 FILLER                 PICTURE X(26)       VALUE       SQ1444.2
015400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1444.2
015500        05 FILLER                 PICTURE X(37)       VALUE       SQ1444.2
015600             ",RECKEY=                             ".             SQ1444.2
015700        05 FILLER                 PICTURE X(38)       VALUE       SQ1444.2
015800             ",ALTKEY1=                             ".            SQ1444.2
015900        05 FILLER                 PICTURE X(38)       VALUE       SQ1444.2
016000             ",ALTKEY2=                             ".            SQ1444.2
016100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1444.2
016200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1444.2
016300        05 FILE-RECORD-INFO-P1-120.                               SQ1444.2
016400           07 FILLER              PIC X(5).                       SQ1444.2
016500           07 XFILE-NAME          PIC X(6).                       SQ1444.2
016600           07 FILLER              PIC X(8).                       SQ1444.2
016700           07 XRECORD-NAME        PIC X(6).                       SQ1444.2
016800           07 FILLER              PIC X(1).                       SQ1444.2
016900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1444.2
017000           07 FILLER              PIC X(7).                       SQ1444.2
017100           07 XRECORD-NUMBER      PIC 9(6).                       SQ1444.2
017200           07 FILLER              PIC X(6).                       SQ1444.2
017300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1444.2
017400           07 FILLER              PIC X(5).                       SQ1444.2
017500           07 ODO-NUMBER          PIC 9(4).                       SQ1444.2
017600           07 FILLER              PIC X(5).                       SQ1444.2
017700           07 XPROGRAM-NAME       PIC X(5).                       SQ1444.2
017800           07 FILLER              PIC X(7).                       SQ1444.2
017900           07 XRECORD-LENGTH      PIC 9(6).                       SQ1444.2
018000           07 FILLER              PIC X(7).                       SQ1444.2
018100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1444.2
018200           07 FILLER              PIC X(1).                       SQ1444.2
018300           07 XBLOCK-SIZE         PIC 9(4).                       SQ1444.2
018400           07 FILLER              PIC X(6).                       SQ1444.2
018500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1444.2
018600           07 FILLER              PIC X(5).                       SQ1444.2
018700           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1444.2
018800           07 FILLER              PIC X(6).                       SQ1444.2
018900           07 XLABEL-TYPE         PIC X(1).                       SQ1444.2
019000        05 FILE-RECORD-INFO-P121-240.                             SQ1444.2
019100           07 FILLER              PIC X(8).                       SQ1444.2
019200           07 XRECORD-KEY         PIC X(29).                      SQ1444.2
019300           07 FILLER              PIC X(9).                       SQ1444.2
019400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1444.2
019500           07 FILLER              PIC X(9).                       SQ1444.2
019600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1444.2
019700           07 FILLER              PIC X(7).                       SQ1444.2
019800*                                                                 SQ1444.2
019900 01  TEST-RESULTS.                                                SQ1444.2
020000     02 FILLER              PIC X      VALUE SPACE.               SQ1444.2
020100     02  PAR-NAME.                                                SQ1444.2
020200       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1444.2
020300       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1444.2
020400       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1444.2
020500     02 FILLER              PIC X      VALUE SPACE.               SQ1444.2
020600     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1444.2
020700     02 FILLER              PIC X      VALUE SPACE.               SQ1444.2
020800     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1444.2
020900     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1444.2
021000     02 RE-MARK             PIC X(61).                            SQ1444.2
021100 01  TEST-COMPUTED.                                               SQ1444.2
021200   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1444.2
021300   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1444.2
021400   02 COMPUTED-X.                                                 SQ1444.2
021500     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1444.2
021600     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1444.2
021700     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1444.2
021800     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1444.2
021900     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1444.2
022000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1444.2
022100        04 COMPUTED-18V0                   PIC -9(18).            SQ1444.2
022200        04 FILLER                          PIC X.                 SQ1444.2
022300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1444.2
022400 01  TEST-CORRECT.                                                SQ1444.2
022500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1444.2
022600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1444.2
022700     02 CORRECT-X.                                                SQ1444.2
022800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1444.2
022900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1444.2
023000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1444.2
023100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1444.2
023200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1444.2
023300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1444.2
023400         04 CORRECT-18V0                     PIC -9(18).          SQ1444.2
023500         04 FILLER                           PIC X.               SQ1444.2
023600     03 FILLER PIC X(2) VALUE SPACE.                              SQ1444.2
023700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1444.2
023800*                                                                 SQ1444.2
023900 01  CCVS-C-1.                                                    SQ1444.2
024000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1444.2
024100     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1444.2
024200     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1444.2
024300     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1444.2
024400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1444.2
024500     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1444.2
024600     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1444.2
024700     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1444.2
024800 01  CCVS-C-2.                                                    SQ1444.2
024900     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1444.2
025000     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1444.2
025100     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1444.2
025200     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1444.2
025300     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1444.2
025400*                                                                 SQ1444.2
025500 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1444.2
025600 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1444.2
025700 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1444.2
025800 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1444.2
025900 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1444.2
026000 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1444.2
026100 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1444.2
026200 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1444.2
026300 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1444.2
026400 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1444.2
026500 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1444.2
026600 01  CCVS-H-1.                                                    SQ1444.2
026700     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1444.2
026800     02  FILLER          PIC X(42)    VALUE                       SQ1444.2
026900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1444.2
027000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1444.2
027100 01  CCVS-H-2A.                                                   SQ1444.2
027200   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1444.2
027300   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1444.2
027400   02  FILLER            PIC XXXX   VALUE                         SQ1444.2
027500     "4.2 ".                                                      SQ1444.2
027600   02  FILLER            PIC X(28)  VALUE                         SQ1444.2
027700            " COPY - NOT FOR DISTRIBUTION".                       SQ1444.2
027800   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1444.2
027900*                                                                 SQ1444.2
028000 01  CCVS-H-2B.                                                   SQ1444.2
028100   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1444.2
028200   02  TEST-ID           PIC X(9).                                SQ1444.2
028300   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1444.2
028400   02  FILLER            PIC X(12)  VALUE                         SQ1444.2
028500     " HIGH       ".                                              SQ1444.2
028600   02  FILLER            PIC X(22)  VALUE                         SQ1444.2
028700            " LEVEL VALIDATION FOR ".                             SQ1444.2
028800   02  FILLER            PIC X(58)  VALUE                         SQ1444.2
028900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1444.2
029000 01  CCVS-H-3.                                                    SQ1444.2
029100     02  FILLER          PIC X(34)  VALUE                         SQ1444.2
029200            " FOR OFFICIAL USE ONLY    ".                         SQ1444.2
029300     02  FILLER          PIC X(58)  VALUE                         SQ1444.2
029400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1444.2
029500     02  FILLER          PIC X(28)  VALUE                         SQ1444.2
029600            "  COPYRIGHT   1985,1986 ".                           SQ1444.2
029700 01  CCVS-E-1.                                                    SQ1444.2
029800     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1444.2
029900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1444.2
030000     02 ID-AGAIN         PIC X(9).                                SQ1444.2
030100     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1444.2
030200 01  CCVS-E-2.                                                    SQ1444.2
030300     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1444.2
030400     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1444.2
030500     02  CCVS-E-2-2.                                              SQ1444.2
030600         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1444.2
030700         03 FILLER         PIC X      VALUE SPACE.                SQ1444.2
030800         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1444.2
030900            "ERRORS ENCOUNTERED".                                 SQ1444.2
031000 01  CCVS-E-3.                                                    SQ1444.2
031100     02  FILLER          PIC X(22)  VALUE                         SQ1444.2
031200            " FOR OFFICIAL USE ONLY".                             SQ1444.2
031300     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1444.2
031400     02  FILLER          PIC X(58)  VALUE                         SQ1444.2
031500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1444.2
031600     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1444.2
031700     02  FILLER          PIC X(20)  VALUE                         SQ1444.2
031800             " COPYRIGHT 1985,1986".                              SQ1444.2
031900 01  CCVS-E-4.                                                    SQ1444.2
032000     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1444.2
032100     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1444.2
032200     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1444.2
032300     02 FILLER           PIC X(40)  VALUE                         SQ1444.2
032400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1444.2
032500 01  XXINFO.                                                      SQ1444.2
032600     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1444.2
032700     02 INFO-TEXT.                                                SQ1444.2
032800       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1444.2
032900       04 XXCOMPUTED         PIC X(20).                           SQ1444.2
033000       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1444.2
033100       04 XXCORRECT          PIC X(20).                           SQ1444.2
033200     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1444.2
033300 01  HYPHEN-LINE.                                                 SQ1444.2
033400     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1444.2
033500     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1444.2
033600-    "*****************************************".                 SQ1444.2
033700     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1444.2
033800-    "******************************".                            SQ1444.2
033900 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1444.2
034000     "SQ144A".                                                    SQ1444.2
034100*                                                                 SQ1444.2
034200*                                                                 SQ1444.2
034300 PROCEDURE DIVISION.                                              SQ1444.2
034400 DECLARATIVES.                                                    SQ1444.2
034500*                                                                 SQ1444.2
034600 SECT-SQ144A-0001 SECTION.                                        SQ1444.2
034700     USE AFTER EXCEPTION PROCEDURE I-O.                           SQ1444.2
034800 I-O-ERROR-PROCESS.                                               SQ1444.2
034900     MOVE   "EXECUTED" TO DECL-EXEC-I-O.                          SQ1444.2
035000     IF DECL-EXEC-SW NOT = SPACE                                  SQ1444.2
035100         GO TO   END-DECLS.                                       SQ1444.2
035200*                                                                 SQ1444.2
035300     MOVE    1 TO REC-CT.                                         SQ1444.2
035400     MOVE   "REWRITE AFTER FAILED RD"  TO FEATURE.                SQ1444.2
035500     MOVE   "DCL-REWRITE-01" TO PAR-NAME.                         SQ1444.2
035600     GO TO   DCL-REWRITE-01.                                      SQ1444.2
035700 DECL-DELETE-01.                                                  SQ1444.2
035800     PERFORM DECL-DE-LETE.                                        SQ1444.2
035900     GO TO   DECL-TEST-01-END.                                    SQ1444.2
036000 DCL-REWRITE-01.                                                  SQ1444.2
036100 DECL-TEST-01-END.                                                SQ1444.2
036200*                                                                 SQ1444.2
036300     PERFORM DECL-WRITE-LINE.                                     SQ1444.2
036400     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ1444.2
036500               TO DUMMY-RECORD.                                   SQ1444.2
036600     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1444.2
036700     GO TO   END-DECLS.                                           SQ1444.2
036800*                                                                 SQ1444.2
036900*                                                                 SQ1444.2
037000 DECL-PASS.                                                       SQ1444.2
037100     MOVE   "PASS " TO P-OR-F.                                    SQ1444.2
037200     ADD     1 TO PASS-COUNTER.                                   SQ1444.2
037300     PERFORM DECL-PRINT-DETAIL.                                   SQ1444.2
037400*                                                                 SQ1444.2
037500 DECL-FAIL.                                                       SQ1444.2
037600     MOVE   "FAIL*" TO P-OR-F.                                    SQ1444.2
037700     ADD     1 TO ERROR-COUNTER.                                  SQ1444.2
037800     PERFORM DECL-PRINT-DETAIL.                                   SQ1444.2
037900*                                                                 SQ1444.2
038000 DECL-DE-LETE.                                                    SQ1444.2
038100     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1444.2
038200     MOVE   "*****" TO P-OR-F.                                    SQ1444.2
038300     ADD     1 TO DELETE-COUNTER.                                 SQ1444.2
038400     PERFORM DECL-PRINT-DETAIL.                                   SQ1444.2
038500*                                                                 SQ1444.2
038600 DECL-PRINT-DETAIL.                                               SQ1444.2
038700     IF REC-CT NOT EQUAL TO ZERO                                  SQ1444.2
038800             MOVE "." TO PARDOT-X                                 SQ1444.2
038900             MOVE REC-CT TO DOTVALUE.                             SQ1444.2
039000     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1444.2
039100     PERFORM DECL-WRITE-LINE.                                     SQ1444.2
039200     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1444.2
039300         PERFORM DECL-WRITE-LINE                                  SQ1444.2
039400         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1444.2
039500     ELSE                                                         SQ1444.2
039600         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1444.2
039700     MOVE    SPACE TO P-OR-F.                                     SQ1444.2
039800     MOVE    SPACE TO COMPUTED-X.                                 SQ1444.2
039900     MOVE    SPACE TO CORRECT-X.                                  SQ1444.2
040000     IF REC-CT EQUAL TO ZERO                                      SQ1444.2
040100         MOVE    SPACE TO PAR-NAME.                               SQ1444.2
040200     MOVE    SPACE TO RE-MARK.                                    SQ1444.2
040300*                                                                 SQ1444.2
040400 DECL-WRITE-LINE.                                                 SQ1444.2
040500     ADD     1 TO RECORD-COUNT.                                   SQ1444.2
040600*    IF RECORD-COUNT GREATER 50                                   SQ1444.2
040700*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1444.2
040800*        MOVE    SPACE TO DUMMY-RECORD                            SQ1444.2
040900*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1444.2
041000*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1444.2
041100*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1444.2
041200*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1444.2
041300*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1444.2
041400*        PERFORM DECL-WRT-LN                                      SQ1444.2
041500*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1444.2
041600*        MOVE    ZERO TO RECORD-COUNT.                            SQ1444.2
041700     PERFORM DECL-WRT-LN.                                         SQ1444.2
041800*                                                                 SQ1444.2
041900 DECL-WRT-LN.                                                     SQ1444.2
042000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1444.2
042100     MOVE    SPACE TO DUMMY-RECORD.                               SQ1444.2
042200*                                                                 SQ1444.2
042300 DECL-FAIL-ROUTINE.                                               SQ1444.2
042400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1444.2
042500     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1444.2
042600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1444.2
042700     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1444.2
042800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1444.2
042900     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1444.2
043000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1444.2
043100     GO TO   DECL-FAIL-EX.                                        SQ1444.2
043200 DECL-FAIL-WRITE.                                                 SQ1444.2
043300     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1444.2
043400     PERFORM DECL-WRITE-LINE                                      SQ1444.2
043500     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1444.2
043600     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1444.2
043700     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1444.2
043800     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1444.2
043900 DECL-FAIL-EX.                                                    SQ1444.2
044000     EXIT.                                                        SQ1444.2
044100*                                                                 SQ1444.2
044200 DECL-BAIL.                                                       SQ1444.2
044300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1444.2
044400     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1444.2
044500 DECL-BAIL-WRITE.                                                 SQ1444.2
044600     MOVE    CORRECT-A TO XXCORRECT.                              SQ1444.2
044700     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1444.2
044800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1444.2
044900     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1444.2
045000 DECL-BAIL-EX.                                                    SQ1444.2
045100     EXIT.                                                        SQ1444.2
045200*                                                                 SQ1444.2
045300 END-DECLS.                                                       SQ1444.2
045400 END DECLARATIVES.                                                SQ1444.2
045500*                                                                 SQ1444.2
045600*                                                                 SQ1444.2
045700 CCVS1 SECTION.                                                   SQ1444.2
045800 OPEN-FILES.                                                      SQ1444.2
045900*    OPEN    I-O RAW-DATA.                                        SQ1444.2
046000*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1444.2
046100*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1444.2
046200*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1444.2
046300*    ADD     1           TO C-NO-OF-TESTS.                        SQ1444.2
046400*    ACCEPT  C-DATE      FROM DATE.                               SQ1444.2
046500*    ACCEPT  C-TIME      FROM TIME.                               SQ1444.2
046600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1444.2
046700*END-E-1.                                                         SQ1444.2
046800*    CLOSE   RAW-DATA.                                            SQ1444.2
046900     OPEN    OUTPUT PRINT-FILE.                                   SQ1444.2
047000     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1444.2
047100     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1444.2
047200     MOVE    SPACE TO TEST-RESULTS.                               SQ1444.2
047300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1444.2
047400     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1444.2
047500     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1444.2
047600     GO TO CCVS1-EXIT.                                            SQ1444.2
047700*                                                                 SQ1444.2
047800 CCVS-INIT-FILE.                                                  SQ1444.2
047900     ADD     1 TO REC-SKL-SUB.                                    SQ1444.2
048000     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1444.2
048100                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1444.2
048200*                                                                 SQ1444.2
048300 CLOSE-FILES.                                                     SQ1444.2
048400     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1444.2
048500     CLOSE   PRINT-FILE.                                          SQ1444.2
048600*    OPEN    I-O RAW-DATA.                                        SQ1444.2
048700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1444.2
048800*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1444.2
048900*    MOVE   "OK.     " TO C-ABORT.                                SQ1444.2
049000*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1444.2
049100*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1444.2
049200*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1444.2
049300*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1444.2
049400*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1444.2
049500*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1444.2
049600*END-E-2.                                                         SQ1444.2
049700*    CLOSE   RAW-DATA.                                            SQ1444.2
049800 TERMINATE-CCVS.                                                  SQ1444.2
049900*    EXIT    PROGRAM.                                             SQ1444.2
050000     STOP    RUN.                                                 SQ1444.2
050100*                                                                 SQ1444.2
050200 INSPT.                                                           SQ1444.2
050300     MOVE   "INSPT" TO P-OR-F.                                    SQ1444.2
050400     ADD     1 TO INSPECT-COUNTER.                                SQ1444.2
050500     PERFORM PRINT-DETAIL.                                        SQ1444.2
050600*                                                                 SQ1444.2
050700 PASS.                                                            SQ1444.2
050800     MOVE   "PASS " TO P-OR-F.                                    SQ1444.2
050900     ADD     1 TO PASS-COUNTER.                                   SQ1444.2
051000     PERFORM PRINT-DETAIL.                                        SQ1444.2
051100*                                                                 SQ1444.2
051200 FAIL.                                                            SQ1444.2
051300     MOVE   "FAIL*" TO P-OR-F.                                    SQ1444.2
051400     ADD     1 TO ERROR-COUNTER.                                  SQ1444.2
051500     PERFORM PRINT-DETAIL.                                        SQ1444.2
051600*                                                                 SQ1444.2
051700 DE-LETE.                                                         SQ1444.2
051800     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1444.2
051900     MOVE   "*****" TO P-OR-F.                                    SQ1444.2
052000     ADD     1 TO DELETE-COUNTER.                                 SQ1444.2
052100     PERFORM PRINT-DETAIL.                                        SQ1444.2
052200*                                                                 SQ1444.2
052300 PRINT-DETAIL.                                                    SQ1444.2
052400     IF REC-CT NOT EQUAL TO ZERO                                  SQ1444.2
052500         MOVE   "." TO PARDOT-X                                   SQ1444.2
052600         MOVE    REC-CT TO DOTVALUE.                              SQ1444.2
052700     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1444.2
052800     PERFORM WRITE-LINE.                                          SQ1444.2
052900     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1444.2
053000         PERFORM WRITE-LINE                                       SQ1444.2
053100         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1444.2
053200     ELSE                                                         SQ1444.2
053300         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1444.2
053400     MOVE    SPACE TO P-OR-F.                                     SQ1444.2
053500     MOVE    SPACE TO COMPUTED-X.                                 SQ1444.2
053600     MOVE    SPACE TO CORRECT-X.                                  SQ1444.2
053700     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1444.2
053800     MOVE    SPACE TO RE-MARK.                                    SQ1444.2
053900*                                                                 SQ1444.2
054000 HEAD-ROUTINE.                                                    SQ1444.2
054100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1444.2
054200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1444.2
054300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1444.2
054400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1444.2
054500 COLUMN-NAMES-ROUTINE.                                            SQ1444.2
054600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1444.2
054700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1444.2
054800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1444.2
054900 END-ROUTINE.                                                     SQ1444.2
055000     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1444.2
055100     PERFORM WRITE-LINE 5 TIMES.                                  SQ1444.2
055200 END-RTN-EXIT.                                                    SQ1444.2
055300     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1444.2
055400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1444.2
055500*                                                                 SQ1444.2
055600 END-ROUTINE-1.                                                   SQ1444.2
055700     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1444.2
055800     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1444.2
055900     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1444.2
056000     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1444.2
056100     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1444.2
056200     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1444.2
056300     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1444.2
056400     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1444.2
056500     PERFORM WRITE-LINE.                                          SQ1444.2
056600     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1444.2
056700     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1444.2
056800         MOVE   "NO " TO ERROR-TOTAL                              SQ1444.2
056900     ELSE                                                         SQ1444.2
057000         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1444.2
057100     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1444.2
057200     PERFORM WRITE-LINE.                                          SQ1444.2
057300 END-ROUTINE-13.                                                  SQ1444.2
057400     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1444.2
057500         MOVE   "NO " TO ERROR-TOTAL                              SQ1444.2
057600     ELSE                                                         SQ1444.2
057700         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1444.2
057800     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1444.2
057900     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1444.2
058000     PERFORM WRITE-LINE.                                          SQ1444.2
058100     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1444.2
058200         MOVE   "NO " TO ERROR-TOTAL                              SQ1444.2
058300     ELSE                                                         SQ1444.2
058400         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1444.2
058500     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1444.2
058600     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1444.2
058700     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1444.2
058800*                                                                 SQ1444.2
058900 WRITE-LINE.                                                      SQ1444.2
059000     ADD     1 TO RECORD-COUNT.                                   SQ1444.2
059100*    IF RECORD-COUNT GREATER 50                                   SQ1444.2
059200*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1444.2
059300*        MOVE  SPACE TO DUMMY-RECORD                              SQ1444.2
059400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1444.2
059500*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1444.2
059600*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1444.2
059700*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1444.2
059800*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1444.2
059900*        MOVE  ZERO TO RECORD-COUNT.                              SQ1444.2
060000     PERFORM WRT-LN.                                              SQ1444.2
060100*                                                                 SQ1444.2
060200 WRT-LN.                                                          SQ1444.2
060300     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1444.2
060400     MOVE    SPACE TO DUMMY-RECORD.                               SQ1444.2
060500 BLANK-LINE-PRINT.                                                SQ1444.2
060600     PERFORM WRT-LN.                                              SQ1444.2
060700 FAIL-ROUTINE.                                                    SQ1444.2
060800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1444.2
060900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1444.2
061000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1444.2
061100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1444.2
061200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1444.2
061300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1444.2
061400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1444.2
061500     GO TO   FAIL-ROUTINE-EX.                                     SQ1444.2
061600 FAIL-ROUTINE-WRITE.                                              SQ1444.2
061700     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1444.2
061800     PERFORM WRITE-LINE                                           SQ1444.2
061900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1444.2
062000     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1444.2
062100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1444.2
062200     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1444.2
062300 FAIL-ROUTINE-EX.                                                 SQ1444.2
062400     EXIT.                                                        SQ1444.2
062500 BAIL-OUT.                                                        SQ1444.2
062600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1444.2
062700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1444.2
062800 BAIL-OUT-WRITE.                                                  SQ1444.2
062900     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1444.2
063000     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1444.2
063100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1444.2
063200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1444.2
063300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1444.2
063400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1444.2
063500 BAIL-OUT-EX.                                                     SQ1444.2
063600     EXIT.                                                        SQ1444.2
063700 CCVS1-EXIT.                                                      SQ1444.2
063800     EXIT.                                                        SQ1444.2
063900*                                                                 SQ1444.2
064000****************************************************************  SQ1444.2
064100*                                                              *  SQ1444.2
064200*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1444.2
064300*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1444.2
064400*                                                              *  SQ1444.2
064500****************************************************************  SQ1444.2
064600*                                                                 SQ1444.2
064700 SECT-SQ144A-0002 SECTION.                                        SQ1444.2
064800 STA-INIT.                                                        SQ1444.2
064900     MOVE    SPACE TO DELETE-SW.                                  SQ1444.2
065000*                                                                 SQ1444.2
065100     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1444.2
065200     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1444.2
065300     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1444.2
065400     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1444.2
065500     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ1444.2
065600     MOVE    1   TO XBLOCK-SIZE (1).                              SQ1444.2
065700     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ1444.2
065800     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1444.2
065900     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1444.2
066000*                                                                 SQ1444.2
066100*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1444.2
066200*                                                                 SQ1444.2
066300 SEQ-INIT-01.                                                     SQ1444.2
066400     MOVE    0 TO REC-CT.                                         SQ1444.2
066500     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1444.2
066600     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
066700     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
066800     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1444.2
066900     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1444.2
067000     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1444.2
067100     GO TO   SEQ-TEST-OP-01.                                      SQ1444.2
067200 SEQ-DELETE-01.                                                   SQ1444.2
067300     MOVE   "*" TO DELETE-SW-1.                                   SQ1444.2
067400 SEQ-TEST-OP-01.                                                  SQ1444.2
067500     OPEN    OUTPUT SQ-FS4.                                       SQ1444.2
067600 SEQ-INIT-02.                                                     SQ1444.2
067700     MOVE    0 TO REC-CT.                                         SQ1444.2
067800     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1444.2
067900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1444.2
068000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
068100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
068200     MOVE   "WRITE A RECORD"  TO FEATURE.                         SQ1444.2
068300     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1444.2
068400 SEQ-TEST-WR-02.                                                  SQ1444.2
068500     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ1444.2
068600     WRITE   SQ-FS4R1-F-G-120.                                    SQ1444.2
068700 SEQ-INIT-03.                                                     SQ1444.2
068800     MOVE    0 TO REC-CT.                                         SQ1444.2
068900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1444.2
069000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
069100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
069200     MOVE   "CLOSE FILE" TO FEATURE.                              SQ1444.2
069300     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ1444.2
069400 SEQ-TEST-CL-03.                                                  SQ1444.2
069500     CLOSE   SQ-FS4.                                              SQ1444.2
069600 SEQ-INIT-04.                                                     SQ1444.2
069700     MOVE    0 TO REC-CT.                                         SQ1444.2
069800     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1444.2
069900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
070000     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
070100     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1444.2
070200     MOVE   "OPEN FILE FOR I-O" TO FEATURE.                       SQ1444.2
070300     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ1444.2
070400 SEQ-TEST-OP-04.                                                  SQ1444.2
070500     OPEN    I-O SQ-FS4.                                          SQ1444.2
070600 SEQ-INIT-05.                                                     SQ1444.2
070700     MOVE    0 TO REC-CT.                                         SQ1444.2
070800     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1444.2
070900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1444.2
071000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
071100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
071200     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ1444.2
071300     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ1444.2
071400 SEQ-TEST-RD-05.                                                  SQ1444.2
071500     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ1444.2
071600     READ    SQ-FS4.                                              SQ1444.2
071700 SEQ-INIT-06.                                                     SQ1444.2
071800     MOVE    0 TO REC-CT.                                         SQ1444.2
071900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1444.2
072000     ADD     1 TO XRECORD-NUMBER (1).                             SQ1444.2
072100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
072200     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
072300     MOVE   "READ GIVING AT END" TO FEATURE.                      SQ1444.2
072400     MOVE   "SEQ-TEST-RD-06" TO PAR-NAME.                         SQ1444.2
072500 SEQ-TEST-RD-06.                                                  SQ1444.2
072600     READ    SQ-FS4 RECORD.                                       SQ1444.2
072700 SEQ-INIT-07.                                                     SQ1444.2
072800     MOVE    0 TO REC-CT.                                         SQ1444.2
072900     MOVE    SPACE TO DECL-EXEC-SW.                               SQ1444.2
073000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1444.2
073100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1444.2
073200     MOVE   "REWRITE AFTER AT END" TO FEATURE.                    SQ1444.2
073300     MOVE   "SEQ-TEST-RW-07" TO PAR-NAME.                         SQ1444.2
073400 SEQ-TEST-RW-07.                                                  SQ1444.2
073500     REWRITE SQ-FS4R1-F-G-120.                                    SQ1444.2
073600     MOVE    0 TO REC-CT.                                         SQ1444.2
073700     MOVE   "REWRITE AFTER AT END" TO FEATURE.                    SQ1444.2
073800     MOVE   "SEQ-TEST-RW-07" TO PAR-NAME.                         SQ1444.2
073900*                                                                 SQ1444.2
074000*    CHECK I-O STATUS RETURNED FROM REWRITE                       SQ1444.2
074100*                                                                 SQ1444.2
074200     ADD    1 TO REC-CT.                                          SQ1444.2
074300 SEQ-TEST-07-01-END.                                              SQ1444.2
074400*                                                                 SQ1444.2
074500*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1444.2
074600*                                                                 SQ1444.2
074700     ADD     1 TO REC-CT.                                         SQ1444.2
074800     IF DELETE-SW NOT = SPACE                                     SQ1444.2
074900         GO TO   SEQ-DELETE-07-02.                                SQ1444.2
075000     GO TO   SEQ-TEST-RW-07-02.                                   SQ1444.2
075100 SEQ-DELETE-07-02.                                                SQ1444.2
075200     PERFORM DE-LETE.                                             SQ1444.2
075300     GO TO   SEQ-TEST-07-02-END.                                  SQ1444.2
075400 SEQ-TEST-RW-07-02.                                               SQ1444.2
075500     IF DECL-EXEC-I-O = "EXECUTED"                                SQ1444.2
075600         PERFORM PASS                                             SQ1444.2
075700     ELSE                                                         SQ1444.2
075800         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1444.2
075900         MOVE   "EXECUTED" TO CORRECT-A                           SQ1444.2
076000         MOVE   "I-O DECLARATIVE NOT EXECUTED"                    SQ1444.2
076100                   TO RE-MARK                                     SQ1444.2
076200         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1444.2
076300         PERFORM FAIL.                                            SQ1444.2
076400 SEQ-TEST-07-02-END.                                              SQ1444.2
076500*                                                                 SQ1444.2
076600*                                                                 SQ1444.2
076700 CCVS-EXIT SECTION.                                               SQ1444.2
076800 CCVS-999999.                                                     SQ1444.2
076900     GO TO   CLOSE-FILES.                                         SQ1444.2
      *END-OF,SQ144A                                                                  
