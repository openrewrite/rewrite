      *HEADER,COBOL,SQ228A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2284.2
000200 PROGRAM-ID.                                                      SQ2284.2
000300     SQ228A.                                                      SQ2284.2
000400****************************************************************  SQ2284.2
000500*                                                              *  SQ2284.2
000600*    VALIDATION FOR:-                                          *  SQ2284.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2284.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ2284.2
000900*                             REVISED 1986, AUGUST             *  SQ2284.2
001000*                                                              *  SQ2284.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ2284.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2284.2
001300*                                                              *  SQ2284.2
001400****************************************************************  SQ2284.2
001500*                                                              *  SQ2284.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2284.2
001700*                                                              *  SQ2284.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ2284.2
001900*            X-55   SYSTEM PRINTER                             *  SQ2284.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ2284.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ2284.2
002200*                                                              *  SQ2284.2
002300****************************************************************  SQ2284.2
002400*                                                              *  SQ2284.2
002500*    SPLIT FROM SQ227A, THIS PROGRAM REPEATS THE SEQUENCE OF   *  SQ2284.2
002600*    FILE HANDLING ROUTINES IN ORDER TO CARRY OUT THE ISOLATED *  SQ2284.2
002700*    SPLIT TEST THAT CHECKS FOR THE CORRECT RESPONSE TO A      *  SQ2284.2
002800*    RE-WRITE ON A RECORD THAT IS TOO LONG FOR THE FILE.       *  SQ2284.2
002900*    (SEE SQ227A).                                             *  SQ2284.2
003000*                                                              *  SQ2284.2
003100****************************************************************  SQ2284.2
003200*                                                                 SQ2284.2
003300 ENVIRONMENT DIVISION.                                            SQ2284.2
003400 CONFIGURATION SECTION.                                           SQ2284.2
003500 SOURCE-COMPUTER.                                                 SQ2284.2
003600     XXXXX082.                                                    SQ2284.2
003700 OBJECT-COMPUTER.                                                 SQ2284.2
003800     XXXXX083.                                                    SQ2284.2
003900*                                                                 SQ2284.2
004000 INPUT-OUTPUT SECTION.                                            SQ2284.2
004100 FILE-CONTROL.                                                    SQ2284.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ2284.2
004300     XXXXX055.                                                    SQ2284.2
004400*                                                                 SQ2284.2
004500*    SELECT RAW-DATA   ASSIGN TO                                  SQ2284.2
004600*    XXXXX062                                                     SQ2284.2
004700*          ORGANIZATION IS INDEXED                                SQ2284.2
004800*          ACCESS MODE  IS RANDOM                                 SQ2284.2
004900*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ2284.2
005000*                                                                 SQ2284.2
005100     SELECT SQ-FS4                                                SQ2284.2
005200            ASSIGN                                                SQ2284.2
005300     XXXXX014                                                     SQ2284.2
005400            STATUS SQ-FS4-STATUS OF STATUS-GROUP                  SQ2284.2
005500            ACCESS MODE IS SEQUENTIAL                             SQ2284.2
005600            ORGANIZATION IS SEQUENTIAL                            SQ2284.2
005700            .                                                     SQ2284.2
005800*                                                                 SQ2284.2
005900*                                                                 SQ2284.2
006000 DATA DIVISION.                                                   SQ2284.2
006100 FILE SECTION.                                                    SQ2284.2
006200 FD  PRINT-FILE                                                   SQ2284.2
006300C    LABEL RECORDS                                                SQ2284.2
006400C    XXXXX084                                                     SQ2284.2
006500C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2284.2
006600               .                                                  SQ2284.2
006700 01  PRINT-REC    PICTURE X(120).                                 SQ2284.2
006800 01  DUMMY-RECORD PICTURE X(120).                                 SQ2284.2
006900*                                                                 SQ2284.2
007000*FD  RAW-DATA.                                                    SQ2284.2
007100*01  RAW-DATA-SATZ.                                               SQ2284.2
007200*    05  RAW-DATA-KEY        PIC X(6).                            SQ2284.2
007300*    05  C-DATE              PIC 9(6).                            SQ2284.2
007400*    05  C-TIME              PIC 9(8).                            SQ2284.2
007500*    05  NO-OF-TESTS         PIC 99.                              SQ2284.2
007600*    05  C-OK                PIC 999.                             SQ2284.2
007700*    05  C-ALL               PIC 999.                             SQ2284.2
007800*    05  C-FAIL              PIC 999.                             SQ2284.2
007900*    05  C-DELETED           PIC 999.                             SQ2284.2
008000*    05  C-INSPECT           PIC 999.                             SQ2284.2
008100*    05  C-NOTE              PIC X(13).                           SQ2284.2
008200*    05  C-INDENT            PIC X.                               SQ2284.2
008300*    05  C-ABORT             PIC X(8).                            SQ2284.2
008400*                                                                 SQ2284.2
008500 FD  SQ-FS4                                                       SQ2284.2
008600C    LABEL RECORD IS STANDARD                                     SQ2284.2
008700     BLOCK  138 CHARACTERS                                        SQ2284.2
008800     RECORD VARYING SIZE FROM 50 TO 138 CHARACTERS                SQ2284.2
008900       DEPENDING ON SQ-FS4-RECSIZE                                SQ2284.2
009000                .                                                 SQ2284.2
009100 01  SQ-FS4R1-F-G-120.                                            SQ2284.2
009200        05 FFILE-RECORD-INFO-P1-120.                              SQ2284.2
009300           07 FILLER              PIC X(5).                       SQ2284.2
009400           07 FFILE-NAME          PIC X(6).                       SQ2284.2
009500           07 FILLER              PIC X(8).                       SQ2284.2
009600           07 FRECORD-NAME        PIC X(6).                       SQ2284.2
009700           07 FILLER              PIC X(1).                       SQ2284.2
009800           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ2284.2
009900           07 FILLER              PIC X(7).                       SQ2284.2
010000           07 FRECORD-NUMBER      PIC 9(6).                       SQ2284.2
010100           07 FILLER              PIC X(6).                       SQ2284.2
010200           07 FUPDATE-NUMBER      PIC 9(2).                       SQ2284.2
010300           07 FILLER              PIC X(5).                       SQ2284.2
010400           07 FODO-NUMBER         PIC 9(4).                       SQ2284.2
010500           07 FILLER              PIC X(5).                       SQ2284.2
010600           07 FPROGRAM-NAME       PIC X(5).                       SQ2284.2
010700           07 FILLER              PIC X(7).                       SQ2284.2
010800           07 FRECORD-LENGTH      PIC 9(6).                       SQ2284.2
010900           07 FILLER              PIC X(7).                       SQ2284.2
011000           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ2284.2
011100           07 FILLER              PIC X(1).                       SQ2284.2
011200           07 FBLOCK-SIZE         PIC 9(4).                       SQ2284.2
011300           07 FILLER              PIC X(6).                       SQ2284.2
011400           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ2284.2
011500           07 FILLER              PIC X(5).                       SQ2284.2
011600           07 FFILE-ORGANIZATION  PIC X(2).                       SQ2284.2
011700           07 FILLER              PIC X(6).                       SQ2284.2
011800           07 FLABEL-TYPE         PIC X(1).                       SQ2284.2
011900*                                                                 SQ2284.2
012000 01  SQ-FS4R2-F-G-138.                                            SQ2284.2
012100     03  FILLER PIC X(120).                                       SQ2284.2
012200     03  EXT-18 PIC X(18).                                        SQ2284.2
012300*                                                                 SQ2284.2
012400 WORKING-STORAGE SECTION.                                         SQ2284.2
012500*                                                                 SQ2284.2
012600***************************************************************   SQ2284.2
012700*                                                             *   SQ2284.2
012800*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2284.2
012900*                                                             *   SQ2284.2
013000***************************************************************   SQ2284.2
013100*                                                                 SQ2284.2
013200 01  STATUS-GROUP.                                                SQ2284.2
013300     04  SQ-FS4-STATUS.                                           SQ2284.2
013400         07  SQ-FS4-KEY-1   PIC X.                                SQ2284.2
013500         07  SQ-FS4-KEY-2   PIC X.                                SQ2284.2
013600*                                                                 SQ2284.2
013700 01  DELETE-SW.                                                   SQ2284.2
013800     03  DELETE-SW-1 PIC X.                                       SQ2284.2
013900     03  DELETE-SW-1-GROUP.                                       SQ2284.2
014000         05  DELETE-SW-2 PIC X.                                   SQ2284.2
014100*                                                                 SQ2284.2
014200 01  DECL-EXEC-I-O PIC X(12).                                     SQ2284.2
014300*                                                                 SQ2284.2
014400 01  DECL-EXEC-SW PIC X.                                          SQ2284.2
014500*                                                                 SQ2284.2
014600 01  SQ-FS4-RECSIZE PIC 999.                                      SQ2284.2
014700*                                                                 SQ2284.2
014800***************************************************************   SQ2284.2
014900*                                                             *   SQ2284.2
015000*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2284.2
015100*                                                             *   SQ2284.2
015200***************************************************************   SQ2284.2
015300*                                                                 SQ2284.2
015400 01  REC-SKEL-SUB   PIC 99.                                       SQ2284.2
015500*                                                                 SQ2284.2
015600 01  FILE-RECORD-INFORMATION-REC.                                 SQ2284.2
015700     03 FILE-RECORD-INFO-SKELETON.                                SQ2284.2
015800        05 FILLER                 PICTURE X(48)       VALUE       SQ2284.2
015900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2284.2
016000        05 FILLER                 PICTURE X(46)       VALUE       SQ2284.2
016100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2284.2
016200        05 FILLER                 PICTURE X(26)       VALUE       SQ2284.2
016300             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2284.2
016400        05 FILLER                 PICTURE X(37)       VALUE       SQ2284.2
016500             ",RECKEY=                             ".             SQ2284.2
016600        05 FILLER                 PICTURE X(38)       VALUE       SQ2284.2
016700             ",ALTKEY1=                             ".            SQ2284.2
016800        05 FILLER                 PICTURE X(38)       VALUE       SQ2284.2
016900             ",ALTKEY2=                             ".            SQ2284.2
017000        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2284.2
017100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2284.2
017200        05 FILE-RECORD-INFO-P1-120.                               SQ2284.2
017300           07 FILLER              PIC X(5).                       SQ2284.2
017400           07 XFILE-NAME          PIC X(6).                       SQ2284.2
017500           07 FILLER              PIC X(8).                       SQ2284.2
017600           07 XRECORD-NAME        PIC X(6).                       SQ2284.2
017700           07 FILLER              PIC X(1).                       SQ2284.2
017800           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2284.2
017900           07 FILLER              PIC X(7).                       SQ2284.2
018000           07 XRECORD-NUMBER      PIC 9(6).                       SQ2284.2
018100           07 FILLER              PIC X(6).                       SQ2284.2
018200           07 UPDATE-NUMBER       PIC 9(2).                       SQ2284.2
018300           07 FILLER              PIC X(5).                       SQ2284.2
018400           07 ODO-NUMBER          PIC 9(4).                       SQ2284.2
018500           07 FILLER              PIC X(5).                       SQ2284.2
018600           07 XPROGRAM-NAME       PIC X(5).                       SQ2284.2
018700           07 FILLER              PIC X(7).                       SQ2284.2
018800           07 XRECORD-LENGTH      PIC 9(6).                       SQ2284.2
018900           07 FILLER              PIC X(7).                       SQ2284.2
019000           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2284.2
019100           07 FILLER              PIC X(1).                       SQ2284.2
019200           07 XBLOCK-SIZE         PIC 9(4).                       SQ2284.2
019300           07 FILLER              PIC X(6).                       SQ2284.2
019400           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2284.2
019500           07 FILLER              PIC X(5).                       SQ2284.2
019600           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2284.2
019700           07 FILLER              PIC X(6).                       SQ2284.2
019800           07 XLABEL-TYPE         PIC X(1).                       SQ2284.2
019900        05 FILE-RECORD-INFO-P121-240.                             SQ2284.2
020000           07 FILLER              PIC X(8).                       SQ2284.2
020100           07 XRECORD-KEY         PIC X(29).                      SQ2284.2
020200           07 FILLER              PIC X(9).                       SQ2284.2
020300           07 ALTERNATE-KEY1      PIC X(29).                      SQ2284.2
020400           07 FILLER              PIC X(9).                       SQ2284.2
020500           07 ALTERNATE-KEY2      PIC X(29).                      SQ2284.2
020600           07 FILLER              PIC X(7).                       SQ2284.2
020700*                                                                 SQ2284.2
020800 01  TEST-RESULTS.                                                SQ2284.2
020900     02 FILLER              PIC X      VALUE SPACE.               SQ2284.2
021000     02  PAR-NAME.                                                SQ2284.2
021100       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2284.2
021200       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2284.2
021300       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2284.2
021400     02 FILLER              PIC X      VALUE SPACE.               SQ2284.2
021500     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2284.2
021600     02 FILLER              PIC X      VALUE SPACE.               SQ2284.2
021700     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2284.2
021800     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2284.2
021900     02 RE-MARK             PIC X(61).                            SQ2284.2
022000 01  TEST-COMPUTED.                                               SQ2284.2
022100   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2284.2
022200   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2284.2
022300   02 COMPUTED-X.                                                 SQ2284.2
022400     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2284.2
022500     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2284.2
022600     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2284.2
022700     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2284.2
022800     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2284.2
022900     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2284.2
023000        04 COMPUTED-18V0                   PIC -9(18).            SQ2284.2
023100        04 FILLER                          PIC X.                 SQ2284.2
023200     03 FILLER PIC X(50) VALUE SPACE.                             SQ2284.2
023300 01  TEST-CORRECT.                                                SQ2284.2
023400     02 FILLER PIC X(30) VALUE SPACE.                             SQ2284.2
023500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2284.2
023600     02 CORRECT-X.                                                SQ2284.2
023700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2284.2
023800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2284.2
023900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2284.2
024000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2284.2
024100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2284.2
024200     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2284.2
024300         04 CORRECT-18V0                     PIC -9(18).          SQ2284.2
024400         04 FILLER                           PIC X.               SQ2284.2
024500     03 FILLER PIC X(2) VALUE SPACE.                              SQ2284.2
024600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2284.2
024700*                                                                 SQ2284.2
024800 01  CCVS-C-1.                                                    SQ2284.2
024900     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2284.2
025000     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ2284.2
025100     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2284.2
025200     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ2284.2
025300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2284.2
025400     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ2284.2
025500     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ2284.2
025600     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ2284.2
025700 01  CCVS-C-2.                                                    SQ2284.2
025800     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2284.2
025900     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ2284.2
026000     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2284.2
026100     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ2284.2
026200     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ2284.2
026300*                                                                 SQ2284.2
026400 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2284.2
026500 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2284.2
026600 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2284.2
026700 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2284.2
026800 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2284.2
026900 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2284.2
027000 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2284.2
027100 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2284.2
027200 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2284.2
027300 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2284.2
027400 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2284.2
027500 01  CCVS-H-1.                                                    SQ2284.2
027600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2284.2
027700     02  FILLER          PIC X(42)    VALUE                       SQ2284.2
027800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2284.2
027900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2284.2
028000 01  CCVS-H-2A.                                                   SQ2284.2
028100   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2284.2
028200   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2284.2
028300   02  FILLER            PIC XXXX   VALUE                         SQ2284.2
028400     "4.2 ".                                                      SQ2284.2
028500   02  FILLER            PIC X(28)  VALUE                         SQ2284.2
028600            " COPY - NOT FOR DISTRIBUTION".                       SQ2284.2
028700   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2284.2
028800*                                                                 SQ2284.2
028900 01  CCVS-H-2B.                                                   SQ2284.2
029000   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2284.2
029100   02  TEST-ID           PIC X(9).                                SQ2284.2
029200   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2284.2
029300   02  FILLER            PIC X(12)  VALUE                         SQ2284.2
029400     " HIGH       ".                                              SQ2284.2
029500   02  FILLER            PIC X(22)  VALUE                         SQ2284.2
029600            " LEVEL VALIDATION FOR ".                             SQ2284.2
029700   02  FILLER            PIC X(58)  VALUE                         SQ2284.2
029800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2284.2
029900 01  CCVS-H-3.                                                    SQ2284.2
030000     02  FILLER          PIC X(34)  VALUE                         SQ2284.2
030100            " FOR OFFICIAL USE ONLY    ".                         SQ2284.2
030200     02  FILLER          PIC X(58)  VALUE                         SQ2284.2
030300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2284.2
030400     02  FILLER          PIC X(28)  VALUE                         SQ2284.2
030500            "  COPYRIGHT   1985,1986 ".                           SQ2284.2
030600 01  CCVS-E-1.                                                    SQ2284.2
030700     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2284.2
030800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2284.2
030900     02 ID-AGAIN         PIC X(9).                                SQ2284.2
031000     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2284.2
031100 01  CCVS-E-2.                                                    SQ2284.2
031200     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2284.2
031300     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2284.2
031400     02  CCVS-E-2-2.                                              SQ2284.2
031500         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2284.2
031600         03 FILLER         PIC X      VALUE SPACE.                SQ2284.2
031700         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2284.2
031800            "ERRORS ENCOUNTERED".                                 SQ2284.2
031900 01  CCVS-E-3.                                                    SQ2284.2
032000     02  FILLER          PIC X(22)  VALUE                         SQ2284.2
032100            " FOR OFFICIAL USE ONLY".                             SQ2284.2
032200     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2284.2
032300     02  FILLER          PIC X(58)  VALUE                         SQ2284.2
032400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2284.2
032500     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2284.2
032600     02  FILLER          PIC X(20)  VALUE                         SQ2284.2
032700             " COPYRIGHT 1985,1986".                              SQ2284.2
032800 01  CCVS-E-4.                                                    SQ2284.2
032900     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2284.2
033000     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2284.2
033100     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2284.2
033200     02 FILLER           PIC X(40)  VALUE                         SQ2284.2
033300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2284.2
033400 01  XXINFO.                                                      SQ2284.2
033500     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2284.2
033600     02 INFO-TEXT.                                                SQ2284.2
033700       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2284.2
033800       04 XXCOMPUTED         PIC X(20).                           SQ2284.2
033900       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2284.2
034000       04 XXCORRECT          PIC X(20).                           SQ2284.2
034100     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2284.2
034200 01  HYPHEN-LINE.                                                 SQ2284.2
034300     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2284.2
034400     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2284.2
034500-    "*****************************************".                 SQ2284.2
034600     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2284.2
034700-    "******************************".                            SQ2284.2
034800 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2284.2
034900     "SQ228A".                                                    SQ2284.2
035000*                                                                 SQ2284.2
035100*                                                                 SQ2284.2
035200 PROCEDURE DIVISION.                                              SQ2284.2
035300 DECLARATIVES.                                                    SQ2284.2
035400*                                                                 SQ2284.2
035500 SECT-SQ228A-0001 SECTION.                                        SQ2284.2
035600     USE AFTER EXCEPTION PROCEDURE I-O.                           SQ2284.2
035700 I-O-ERROR-PROCESS.                                               SQ2284.2
035800     MOVE   "EXECUTED" TO DECL-EXEC-I-O.                          SQ2284.2
035900     IF DECL-EXEC-SW NOT = SPACE                                  SQ2284.2
036000         GO TO   END-DECLS.                                       SQ2284.2
036100*                                                                 SQ2284.2
036200     MOVE    1 TO REC-CT.                                         SQ2284.2
036300     MOVE   "DCL-REWRITE-01" TO PAR-NAME.                         SQ2284.2
036400     GO TO   DCL-REWRITE-01-01.                                   SQ2284.2
036500 DECL-DELETE-01-01.                                               SQ2284.2
036600     PERFORM DECL-DE-LETE.                                        SQ2284.2
036700     GO TO   DECL-TEST-01-01-END.                                 SQ2284.2
036800 DCL-REWRITE-01-01.                                               SQ2284.2
036900 DECL-TEST-01-01-END.                                             SQ2284.2
037000*                                                                 SQ2284.2
037100     ADD     1 TO REC-CT.                                         SQ2284.2
037200     GO TO   DCL-REWRITE-01-02.                                   SQ2284.2
037300 DECL-DELETE-01-02.                                               SQ2284.2
037400     PERFORM DECL-DE-LETE.                                        SQ2284.2
037500     GO TO   DECL-TEST-01-02-END.                                 SQ2284.2
037600 DCL-REWRITE-01-02.                                               SQ2284.2
037700 DECL-TEST-01-02-END.                                             SQ2284.2
037800*                                                                 SQ2284.2
037900     ADD     1 TO REC-CT.                                         SQ2284.2
038000     GO TO   DCL-REWRITE-01-03.                                   SQ2284.2
038100 DECL-DELETE-01-03.                                               SQ2284.2
038200     PERFORM DECL-DE-LETE.                                        SQ2284.2
038300     GO TO   DECL-TEST-01-03-END.                                 SQ2284.2
038400 DCL-REWRITE-01-03.                                               SQ2284.2
038500 DECL-TEST-01-03-END.                                             SQ2284.2
038600*                                                                 SQ2284.2
038700     PERFORM DECL-WRITE-LINE.                                     SQ2284.2
038800     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ2284.2
038900               TO DUMMY-RECORD.                                   SQ2284.2
039000     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ2284.2
039100     GO TO   END-DECLS.                                           SQ2284.2
039200*                                                                 SQ2284.2
039300*                                                                 SQ2284.2
039400 DECL-PASS.                                                       SQ2284.2
039500     MOVE   "PASS " TO P-OR-F.                                    SQ2284.2
039600     ADD     1 TO PASS-COUNTER.                                   SQ2284.2
039700     PERFORM DECL-PRINT-DETAIL.                                   SQ2284.2
039800*                                                                 SQ2284.2
039900 DECL-FAIL.                                                       SQ2284.2
040000     MOVE   "FAIL*" TO P-OR-F.                                    SQ2284.2
040100     ADD     1 TO ERROR-COUNTER.                                  SQ2284.2
040200     PERFORM DECL-PRINT-DETAIL.                                   SQ2284.2
040300*                                                                 SQ2284.2
040400 DECL-DE-LETE.                                                    SQ2284.2
040500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2284.2
040600     MOVE   "*****" TO P-OR-F.                                    SQ2284.2
040700     ADD     1 TO DELETE-COUNTER.                                 SQ2284.2
040800     PERFORM DECL-PRINT-DETAIL.                                   SQ2284.2
040900*                                                                 SQ2284.2
041000 DECL-PRINT-DETAIL.                                               SQ2284.2
041100     IF REC-CT NOT EQUAL TO ZERO                                  SQ2284.2
041200             MOVE "." TO PARDOT-X                                 SQ2284.2
041300             MOVE REC-CT TO DOTVALUE.                             SQ2284.2
041400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2284.2
041500     PERFORM DECL-WRITE-LINE.                                     SQ2284.2
041600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2284.2
041700         PERFORM DECL-WRITE-LINE                                  SQ2284.2
041800         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ2284.2
041900     ELSE                                                         SQ2284.2
042000         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ2284.2
042100     MOVE    SPACE TO P-OR-F.                                     SQ2284.2
042200     MOVE    SPACE TO COMPUTED-X.                                 SQ2284.2
042300     MOVE    SPACE TO CORRECT-X.                                  SQ2284.2
042400     IF REC-CT EQUAL TO ZERO                                      SQ2284.2
042500         MOVE    SPACE TO PAR-NAME.                               SQ2284.2
042600     MOVE    SPACE TO RE-MARK.                                    SQ2284.2
042700*                                                                 SQ2284.2
042800 DECL-WRITE-LINE.                                                 SQ2284.2
042900     ADD     1 TO RECORD-COUNT.                                   SQ2284.2
043000*    IF RECORD-COUNT GREATER 50                                   SQ2284.2
043100*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ2284.2
043200*        MOVE    SPACE TO DUMMY-RECORD                            SQ2284.2
043300*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ2284.2
043400*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ2284.2
043500*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ2284.2
043600*        PERFORM DECL-WRT-LN 2 TIMES                              SQ2284.2
043700*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ2284.2
043800*        PERFORM DECL-WRT-LN                                      SQ2284.2
043900*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ2284.2
044000*        MOVE    ZERO TO RECORD-COUNT.                            SQ2284.2
044100     PERFORM DECL-WRT-LN.                                         SQ2284.2
044200*                                                                 SQ2284.2
044300 DECL-WRT-LN.                                                     SQ2284.2
044400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2284.2
044500     MOVE    SPACE TO DUMMY-RECORD.                               SQ2284.2
044600*                                                                 SQ2284.2
044700 DECL-FAIL-ROUTINE.                                               SQ2284.2
044800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ2284.2
044900     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ2284.2
045000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2284.2
045100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2284.2
045200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2284.2
045300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2284.2
045400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2284.2
045500     GO TO   DECL-FAIL-EX.                                        SQ2284.2
045600 DECL-FAIL-WRITE.                                                 SQ2284.2
045700     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ2284.2
045800     PERFORM DECL-WRITE-LINE                                      SQ2284.2
045900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2284.2
046000     MOVE    TEST-CORRECT TO PRINT-REC                            SQ2284.2
046100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2284.2
046200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ2284.2
046300 DECL-FAIL-EX.                                                    SQ2284.2
046400     EXIT.                                                        SQ2284.2
046500*                                                                 SQ2284.2
046600 DECL-BAIL.                                                       SQ2284.2
046700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ2284.2
046800     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ2284.2
046900 DECL-BAIL-WRITE.                                                 SQ2284.2
047000     MOVE    CORRECT-A TO XXCORRECT.                              SQ2284.2
047100     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ2284.2
047200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2284.2
047300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2284.2
047400 DECL-BAIL-EX.                                                    SQ2284.2
047500     EXIT.                                                        SQ2284.2
047600*                                                                 SQ2284.2
047700 END-DECLS.                                                       SQ2284.2
047800 END DECLARATIVES.                                                SQ2284.2
047900*                                                                 SQ2284.2
048000*                                                                 SQ2284.2
048100 CCVS1 SECTION.                                                   SQ2284.2
048200 OPEN-FILES.                                                      SQ2284.2
048300*    OPEN    I-O RAW-DATA.                                        SQ2284.2
048400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2284.2
048500*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ2284.2
048600*    MOVE   "ABORTED "   TO C-ABORT.                              SQ2284.2
048700*    ADD     1           TO C-NO-OF-TESTS.                        SQ2284.2
048800*    ACCEPT  C-DATE      FROM DATE.                               SQ2284.2
048900*    ACCEPT  C-TIME      FROM TIME.                               SQ2284.2
049000*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2284.2
049100*END-E-1.                                                         SQ2284.2
049200*    CLOSE   RAW-DATA.                                            SQ2284.2
049300     OPEN    OUTPUT PRINT-FILE.                                   SQ2284.2
049400     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2284.2
049500     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2284.2
049600     MOVE    SPACE TO TEST-RESULTS.                               SQ2284.2
049700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2284.2
049800     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2284.2
049900     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2284.2
050000     GO TO CCVS1-EXIT.                                            SQ2284.2
050100*                                                                 SQ2284.2
050200 CCVS-INIT-FILE.                                                  SQ2284.2
050300     ADD     1 TO REC-SKL-SUB.                                    SQ2284.2
050400     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2284.2
050500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2284.2
050600*                                                                 SQ2284.2
050700 CLOSE-FILES.                                                     SQ2284.2
050800     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2284.2
050900     CLOSE   PRINT-FILE.                                          SQ2284.2
051000*    OPEN    I-O RAW-DATA.                                        SQ2284.2
051100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2284.2
051200*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ2284.2
051300*    MOVE   "OK.     " TO C-ABORT.                                SQ2284.2
051400*    MOVE    PASS-COUNTER  TO C-OK.                               SQ2284.2
051500*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ2284.2
051600*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ2284.2
051700*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ2284.2
051800*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ2284.2
051900*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2284.2
052000*END-E-2.                                                         SQ2284.2
052100*    CLOSE   RAW-DATA.                                            SQ2284.2
052200 TERMINATE-CCVS.                                                  SQ2284.2
052300*    EXIT    PROGRAM.                                             SQ2284.2
052400     STOP    RUN.                                                 SQ2284.2
052500*                                                                 SQ2284.2
052600 INSPT.                                                           SQ2284.2
052700     MOVE   "INSPT" TO P-OR-F.                                    SQ2284.2
052800     ADD     1 TO INSPECT-COUNTER.                                SQ2284.2
052900     PERFORM PRINT-DETAIL.                                        SQ2284.2
053000*                                                                 SQ2284.2
053100 PASS.                                                            SQ2284.2
053200     MOVE   "PASS " TO P-OR-F.                                    SQ2284.2
053300     ADD     1 TO PASS-COUNTER.                                   SQ2284.2
053400     PERFORM PRINT-DETAIL.                                        SQ2284.2
053500*                                                                 SQ2284.2
053600 FAIL.                                                            SQ2284.2
053700     MOVE   "FAIL*" TO P-OR-F.                                    SQ2284.2
053800     ADD     1 TO ERROR-COUNTER.                                  SQ2284.2
053900     PERFORM PRINT-DETAIL.                                        SQ2284.2
054000*                                                                 SQ2284.2
054100 DE-LETE.                                                         SQ2284.2
054200     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2284.2
054300     MOVE   "*****" TO P-OR-F.                                    SQ2284.2
054400     ADD     1 TO DELETE-COUNTER.                                 SQ2284.2
054500     PERFORM PRINT-DETAIL.                                        SQ2284.2
054600*                                                                 SQ2284.2
054700 PRINT-DETAIL.                                                    SQ2284.2
054800     IF REC-CT NOT EQUAL TO ZERO                                  SQ2284.2
054900         MOVE   "." TO PARDOT-X                                   SQ2284.2
055000         MOVE    REC-CT TO DOTVALUE.                              SQ2284.2
055100     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2284.2
055200     PERFORM WRITE-LINE.                                          SQ2284.2
055300     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2284.2
055400         PERFORM WRITE-LINE                                       SQ2284.2
055500         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2284.2
055600     ELSE                                                         SQ2284.2
055700         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2284.2
055800     MOVE    SPACE TO P-OR-F.                                     SQ2284.2
055900     MOVE    SPACE TO COMPUTED-X.                                 SQ2284.2
056000     MOVE    SPACE TO CORRECT-X.                                  SQ2284.2
056100     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2284.2
056200     MOVE    SPACE TO RE-MARK.                                    SQ2284.2
056300*                                                                 SQ2284.2
056400 HEAD-ROUTINE.                                                    SQ2284.2
056500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2284.2
056600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2284.2
056700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2284.2
056800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2284.2
056900 COLUMN-NAMES-ROUTINE.                                            SQ2284.2
057000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2284.2
057100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2284.2
057200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2284.2
057300 END-ROUTINE.                                                     SQ2284.2
057400     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2284.2
057500     PERFORM WRITE-LINE 5 TIMES.                                  SQ2284.2
057600 END-RTN-EXIT.                                                    SQ2284.2
057700     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2284.2
057800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2284.2
057900*                                                                 SQ2284.2
058000 END-ROUTINE-1.                                                   SQ2284.2
058100     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2284.2
058200     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2284.2
058300     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2284.2
058400     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2284.2
058500     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2284.2
058600     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2284.2
058700     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2284.2
058800     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2284.2
058900     PERFORM WRITE-LINE.                                          SQ2284.2
059000     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2284.2
059100     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2284.2
059200         MOVE   "NO " TO ERROR-TOTAL                              SQ2284.2
059300     ELSE                                                         SQ2284.2
059400         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2284.2
059500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2284.2
059600     PERFORM WRITE-LINE.                                          SQ2284.2
059700 END-ROUTINE-13.                                                  SQ2284.2
059800     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2284.2
059900         MOVE   "NO " TO ERROR-TOTAL                              SQ2284.2
060000     ELSE                                                         SQ2284.2
060100         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2284.2
060200     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2284.2
060300     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2284.2
060400     PERFORM WRITE-LINE.                                          SQ2284.2
060500     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2284.2
060600         MOVE   "NO " TO ERROR-TOTAL                              SQ2284.2
060700     ELSE                                                         SQ2284.2
060800         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2284.2
060900     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2284.2
061000     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2284.2
061100     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2284.2
061200*                                                                 SQ2284.2
061300 WRITE-LINE.                                                      SQ2284.2
061400     ADD     1 TO RECORD-COUNT.                                   SQ2284.2
061500*    IF RECORD-COUNT GREATER 50                                   SQ2284.2
061600*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2284.2
061700*        MOVE  SPACE TO DUMMY-RECORD                              SQ2284.2
061800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2284.2
061900*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2284.2
062000*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2284.2
062100*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2284.2
062200*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2284.2
062300*        MOVE  ZERO TO RECORD-COUNT.                              SQ2284.2
062400     PERFORM WRT-LN.                                              SQ2284.2
062500*                                                                 SQ2284.2
062600 WRT-LN.                                                          SQ2284.2
062700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2284.2
062800     MOVE    SPACE TO DUMMY-RECORD.                               SQ2284.2
062900 BLANK-LINE-PRINT.                                                SQ2284.2
063000     PERFORM WRT-LN.                                              SQ2284.2
063100 FAIL-ROUTINE.                                                    SQ2284.2
063200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2284.2
063300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2284.2
063400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2284.2
063500     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2284.2
063600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2284.2
063700     PERFORM WRITE-LINE 2 TIMES.                                  SQ2284.2
063800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2284.2
063900     GO TO   FAIL-ROUTINE-EX.                                     SQ2284.2
064000 FAIL-ROUTINE-WRITE.                                              SQ2284.2
064100     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2284.2
064200     PERFORM WRITE-LINE                                           SQ2284.2
064300     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2284.2
064400     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2284.2
064500     PERFORM WRITE-LINE 2 TIMES.                                  SQ2284.2
064600     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2284.2
064700 FAIL-ROUTINE-EX.                                                 SQ2284.2
064800     EXIT.                                                        SQ2284.2
064900 BAIL-OUT.                                                        SQ2284.2
065000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2284.2
065100     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2284.2
065200 BAIL-OUT-WRITE.                                                  SQ2284.2
065300     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2284.2
065400     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2284.2
065500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2284.2
065600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2284.2
065700     PERFORM WRITE-LINE 2 TIMES.                                  SQ2284.2
065800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2284.2
065900 BAIL-OUT-EX.                                                     SQ2284.2
066000     EXIT.                                                        SQ2284.2
066100 CCVS1-EXIT.                                                      SQ2284.2
066200     EXIT.                                                        SQ2284.2
066300*                                                                 SQ2284.2
066400****************************************************************  SQ2284.2
066500*                                                              *  SQ2284.2
066600*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2284.2
066700*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2284.2
066800*                                                              *  SQ2284.2
066900****************************************************************  SQ2284.2
067000*                                                                 SQ2284.2
067100 SECT-SQ228A-0002 SECTION.                                        SQ2284.2
067200 STA-INIT.                                                        SQ2284.2
067300     MOVE    SPACE TO DELETE-SW.                                  SQ2284.2
067400*                                                                 SQ2284.2
067500     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ2284.2
067600     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ2284.2
067700     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ2284.2
067800     MOVE    120 TO XRECORD-LENGTH (1).                           SQ2284.2
067900     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ2284.2
068000     MOVE    1   TO XBLOCK-SIZE (1).                              SQ2284.2
068100     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ2284.2
068200     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ2284.2
068300     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ2284.2
068400*                                                                 SQ2284.2
068500*    OPEN THE FILE IN THE OUTPUT MODE                             SQ2284.2
068600*                                                                 SQ2284.2
068700 SEQ-INIT-01.                                                     SQ2284.2
068800     MOVE    0 TO REC-CT.                                         SQ2284.2
068900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2284.2
069000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2284.2
069100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2284.2
069200     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2284.2
069300     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ2284.2
069400     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ2284.2
069500     GO TO   SEQ-TEST-OP-01.                                      SQ2284.2
069600 SEQ-TEST-OP-01.                                                  SQ2284.2
069700     OPEN    OUTPUT SQ-FS4.                                       SQ2284.2
069800 SEQ-INIT-02.                                                     SQ2284.2
069900     MOVE    0 TO REC-CT.                                         SQ2284.2
070000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2284.2
070100     ADD     1 TO XRECORD-NUMBER (1).                             SQ2284.2
070200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2284.2
070300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2284.2
070400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ2284.2
070500     MOVE   "987654321123456789" TO EXT-18.                       SQ2284.2
070600     MOVE    120 TO SQ-FS4-RECSIZE.                               SQ2284.2
070700     MOVE   "WRITE A RECORD"  TO FEATURE.                         SQ2284.2
070800     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ2284.2
070900 SEQ-TEST-WR-02.                                                  SQ2284.2
071000     WRITE   SQ-FS4R2-F-G-138.                                    SQ2284.2
071100 SEQ-INIT-03.                                                     SQ2284.2
071200     MOVE    0 TO REC-CT.                                         SQ2284.2
071300     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2284.2
071400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2284.2
071500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2284.2
071600     MOVE   "CLOSE FILE" TO FEATURE.                              SQ2284.2
071700     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ2284.2
071800 SEQ-TEST-CL-03.                                                  SQ2284.2
071900     CLOSE   SQ-FS4.                                              SQ2284.2
072000 SEQ-INIT-04.                                                     SQ2284.2
072100     MOVE    0 TO REC-CT.                                         SQ2284.2
072200     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2284.2
072300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2284.2
072400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2284.2
072500     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2284.2
072600     MOVE   "OPEN FILE FOR I-O" TO FEATURE.                       SQ2284.2
072700     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ2284.2
072800 SEQ-TEST-OP-04.                                                  SQ2284.2
072900     OPEN    I-O SQ-FS4.                                          SQ2284.2
073000 SEQ-INIT-05.                                                     SQ2284.2
073100     MOVE    0 TO REC-CT.                                         SQ2284.2
073200     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2284.2
073300     ADD     1 TO XRECORD-NUMBER (1).                             SQ2284.2
073400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2284.2
073500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2284.2
073600     MOVE    ZERO TO SQ-FS4-RECSIZE.                              SQ2284.2
073700     MOVE    SPACE TO SQ-FS4R2-F-G-138.                           SQ2284.2
073800     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ2284.2
073900     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ2284.2
074000 SEQ-TEST-RD-05.                                                  SQ2284.2
074100     READ    SQ-FS4.                                              SQ2284.2
074200 SEQ-INIT-06.                                                     SQ2284.2
074300     MOVE    0 TO REC-CT.                                         SQ2284.2
074400     MOVE    SPACE TO DECL-EXEC-SW.                               SQ2284.2
074500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2284.2
074600     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2284.2
074700     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ2284.2
074800     MOVE   "ABCDEFGHIJKLMNOPQR" TO EXT-18.                       SQ2284.2
074900     MOVE    130 TO SQ-FS4-RECSIZE.                               SQ2284.2
075000     MOVE   "REWRITE DIFFERENT SIZE REC" TO FEATURE.              SQ2284.2
075100     MOVE   "SEQ-TEST-RW-06" TO PAR-NAME.                         SQ2284.2
075200 SEQ-TEST-RW-06.                                                  SQ2284.2
075300     REWRITE SQ-FS4R1-F-G-120.                                    SQ2284.2
075400     MOVE    0 TO REC-CT.                                         SQ2284.2
075500*                                                                 SQ2284.2
075600*    CHECK I-O STATUS RETURNED FROM REWRITE                       SQ2284.2
075700*                                                                 SQ2284.2
075800     ADD    1 TO REC-CT.                                          SQ2284.2
075900 SEQ-TEST-06-01-END.                                              SQ2284.2
076000*                                                                 SQ2284.2
076100*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2284.2
076200*                                                                 SQ2284.2
076300     ADD     1 TO REC-CT.                                         SQ2284.2
076400     IF DELETE-SW NOT = SPACE                                     SQ2284.2
076500         GO TO   SEQ-DELETE-06-02.                                SQ2284.2
076600     GO TO   SEQ-TEST-RW-06-02.                                   SQ2284.2
076700 SEQ-DELETE-06-02.                                                SQ2284.2
076800     PERFORM DE-LETE.                                             SQ2284.2
076900     GO TO   SEQ-TEST-06-02-END.                                  SQ2284.2
077000 SEQ-TEST-RW-06-02.                                               SQ2284.2
077100     IF DECL-EXEC-I-O = "EXECUTED"                                SQ2284.2
077200         PERFORM PASS                                             SQ2284.2
077300     ELSE                                                         SQ2284.2
077400         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2284.2
077500         MOVE   "EXECUTED" TO CORRECT-A                           SQ2284.2
077600         MOVE   "DECLARATIVE NOT EXECUTED ON REWRITE"             SQ2284.2
077700                   TO RE-MARK                                     SQ2284.2
077800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2284.2
077900         PERFORM FAIL.                                            SQ2284.2
078000 SEQ-TEST-06-02-END.                                              SQ2284.2
078100 CCVS-EXIT SECTION.                                               SQ2284.2
078200 CCVS-999999.                                                     SQ2284.2
078300     GO TO CLOSE-FILES.                                           SQ2284.2
      *END-OF,SQ228A                                                                  
