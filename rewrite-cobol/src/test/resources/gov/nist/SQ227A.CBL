      *HEADER,COBOL,SQ227A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2274.2
000200 PROGRAM-ID.                                                      SQ2274.2
000300     SQ227A.                                                      SQ2274.2
000400****************************************************************  SQ2274.2
000500*                                                              *  SQ2274.2
000600*    VALIDATION FOR:-                                          *  SQ2274.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2274.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ2274.2
000900*                             REVISED 1986, AUGUST             *  SQ2274.2
001000*                                                              *  SQ2274.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ2274.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2274.2
001300*                                                              *  SQ2274.2
001400****************************************************************  SQ2274.2
001500*                                                              *  SQ2274.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2274.2
001700*                                                              *  SQ2274.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ2274.2
001900*            X-55   SYSTEM PRINTER                             *  SQ2274.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ2274.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ2274.2
002200*                                                              *  SQ2274.2
002300****************************************************************  SQ2274.2
002400*                                                              *  SQ2274.2
002500*    THIS PROGRAM OPENS FOR OUTPUT A FILE WHICH IS ASSIGNED    *  SQ2274.2
002600*    TO A MASS STORAGE MEDIUM. ONE RECORD IS THEN WRITTEN TO   *  SQ2274.2
002700*    THIS FILE WHICH IS THEN CLOSED.  THE FILE IS THEN OPENED  *  SQ2274.2
002800*    FOR I-O, AND A READ STATEMENT ON THE FILE IS CARRIED OUT. *  SQ2274.2
002900*    A REWRITE ON A RECORD THAT IS TOO LONG FOR THE FILE IS    *  SQ2274.2
003000*    ATTEMPTED WHICH SHOULD CAUSE AN EXCEPTION CONDITION WITH  *  SQ2274.2
003100*    I-O STATUS "44".  THIS LOGIC ERROR SHOULD CAUSE ENTRY TO  *  SQ2274.2
003200*    THE APPLICABLE ERROR DECLARATIVE.                         *  SQ2274.2
003300*                                                              *  SQ2274.2
003400*    THIS PROGRAM HAS BEEN SPLIT FROM V2.0 ONWARDS.            *  SQ2274.2
003500*    THE NEW PROGRAM IS SQ228A.                                *  SQ2274.2
003600****************************************************************  SQ2274.2
003700*                                                                 SQ2274.2
003800 ENVIRONMENT DIVISION.                                            SQ2274.2
003900 CONFIGURATION SECTION.                                           SQ2274.2
004000 SOURCE-COMPUTER.                                                 SQ2274.2
004100     XXXXX082.                                                    SQ2274.2
004200 OBJECT-COMPUTER.                                                 SQ2274.2
004300     XXXXX083.                                                    SQ2274.2
004400*                                                                 SQ2274.2
004500 INPUT-OUTPUT SECTION.                                            SQ2274.2
004600 FILE-CONTROL.                                                    SQ2274.2
004700     SELECT PRINT-FILE ASSIGN TO                                  SQ2274.2
004800     XXXXX055.                                                    SQ2274.2
004900*                                                                 SQ2274.2
005000*    SELECT RAW-DATA   ASSIGN TO                                  SQ2274.2
005100*    XXXXX062                                                     SQ2274.2
005200*          ORGANIZATION IS INDEXED                                SQ2274.2
005300*          ACCESS MODE  IS RANDOM                                 SQ2274.2
005400*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ2274.2
005500*                                                                 SQ2274.2
005600     SELECT SQ-FS4                                                SQ2274.2
005700            ASSIGN                                                SQ2274.2
005800     XXXXX014                                                     SQ2274.2
005900            STATUS SQ-FS4-STATUS OF STATUS-GROUP                  SQ2274.2
006000            ACCESS MODE IS SEQUENTIAL                             SQ2274.2
006100            ORGANIZATION IS SEQUENTIAL                            SQ2274.2
006200            .                                                     SQ2274.2
006300*                                                                 SQ2274.2
006400*                                                                 SQ2274.2
006500 DATA DIVISION.                                                   SQ2274.2
006600 FILE SECTION.                                                    SQ2274.2
006700 FD  PRINT-FILE                                                   SQ2274.2
006800C    LABEL RECORDS                                                SQ2274.2
006900C    XXXXX084                                                     SQ2274.2
007000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2274.2
007100               .                                                  SQ2274.2
007200 01  PRINT-REC    PICTURE X(120).                                 SQ2274.2
007300 01  DUMMY-RECORD PICTURE X(120).                                 SQ2274.2
007400*                                                                 SQ2274.2
007500*FD  RAW-DATA.                                                    SQ2274.2
007600*01  RAW-DATA-SATZ.                                               SQ2274.2
007700*    05  RAW-DATA-KEY        PIC X(6).                            SQ2274.2
007800*    05  C-DATE              PIC 9(6).                            SQ2274.2
007900*    05  C-TIME              PIC 9(8).                            SQ2274.2
008000*    05  NO-OF-TESTS         PIC 99.                              SQ2274.2
008100*    05  C-OK                PIC 999.                             SQ2274.2
008200*    05  C-ALL               PIC 999.                             SQ2274.2
008300*    05  C-FAIL              PIC 999.                             SQ2274.2
008400*    05  C-DELETED           PIC 999.                             SQ2274.2
008500*    05  C-INSPECT           PIC 999.                             SQ2274.2
008600*    05  C-NOTE              PIC X(13).                           SQ2274.2
008700*    05  C-INDENT            PIC X.                               SQ2274.2
008800*    05  C-ABORT             PIC X(8).                            SQ2274.2
008900*                                                                 SQ2274.2
009000 FD  SQ-FS4                                                       SQ2274.2
009100C    LABEL RECORD IS STANDARD                                     SQ2274.2
009200     BLOCK CONTAINS 138 CHARACTERS                                SQ2274.2
009300     RECORD VARYING SIZE FROM 50 TO 138 CHARACTERS                SQ2274.2
009400       DEPENDING ON SQ-FS4-RECSIZE                                SQ2274.2
009500                .                                                 SQ2274.2
009600 01  SQ-FS4R1-F-G-120.                                            SQ2274.2
009700        05 FFILE-RECORD-INFO-P1-120.                              SQ2274.2
009800           07 FILLER              PIC X(5).                       SQ2274.2
009900           07 FFILE-NAME          PIC X(6).                       SQ2274.2
010000           07 FILLER              PIC X(8).                       SQ2274.2
010100           07 FRECORD-NAME        PIC X(6).                       SQ2274.2
010200           07 FILLER              PIC X(1).                       SQ2274.2
010300           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ2274.2
010400           07 FILLER              PIC X(7).                       SQ2274.2
010500           07 FRECORD-NUMBER      PIC 9(6).                       SQ2274.2
010600           07 FILLER              PIC X(6).                       SQ2274.2
010700           07 FUPDATE-NUMBER      PIC 9(2).                       SQ2274.2
010800           07 FILLER              PIC X(5).                       SQ2274.2
010900           07 FODO-NUMBER         PIC 9(4).                       SQ2274.2
011000           07 FILLER              PIC X(5).                       SQ2274.2
011100           07 FPROGRAM-NAME       PIC X(5).                       SQ2274.2
011200           07 FILLER              PIC X(7).                       SQ2274.2
011300           07 FRECORD-LENGTH      PIC 9(6).                       SQ2274.2
011400           07 FILLER              PIC X(7).                       SQ2274.2
011500           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ2274.2
011600           07 FILLER              PIC X(1).                       SQ2274.2
011700           07 FBLOCK-SIZE         PIC 9(4).                       SQ2274.2
011800           07 FILLER              PIC X(6).                       SQ2274.2
011900           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ2274.2
012000           07 FILLER              PIC X(5).                       SQ2274.2
012100           07 FFILE-ORGANIZATION  PIC X(2).                       SQ2274.2
012200           07 FILLER              PIC X(6).                       SQ2274.2
012300           07 FLABEL-TYPE         PIC X(1).                       SQ2274.2
012400*                                                                 SQ2274.2
012500 01  SQ-FS4R2-F-G-138.                                            SQ2274.2
012600     03  FILLER PIC X(120).                                       SQ2274.2
012700     03  EXT-18 PIC X(18).                                        SQ2274.2
012800*                                                                 SQ2274.2
012900 WORKING-STORAGE SECTION.                                         SQ2274.2
013000*                                                                 SQ2274.2
013100***************************************************************   SQ2274.2
013200*                                                             *   SQ2274.2
013300*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2274.2
013400*                                                             *   SQ2274.2
013500***************************************************************   SQ2274.2
013600*                                                                 SQ2274.2
013700 01  STATUS-GROUP.                                                SQ2274.2
013800     04  SQ-FS4-STATUS.                                           SQ2274.2
013900         07  SQ-FS4-KEY-1   PIC X.                                SQ2274.2
014000         07  SQ-FS4-KEY-2   PIC X.                                SQ2274.2
014100*                                                                 SQ2274.2
014200 01  DELETE-SW.                                                   SQ2274.2
014300     03  DELETE-SW-1 PIC X.                                       SQ2274.2
014400     03  DELETE-SW-1-GROUP.                                       SQ2274.2
014500         05  DELETE-SW-2 PIC X.                                   SQ2274.2
014600*                                                                 SQ2274.2
014700 01  DECL-EXEC-I-O PIC X(12).                                     SQ2274.2
014800*                                                                 SQ2274.2
014900 01  DECL-EXEC-SW PIC X.                                          SQ2274.2
015000*                                                                 SQ2274.2
015100 01  SQ-FS4-RECSIZE PIC 999.                                      SQ2274.2
015200*                                                                 SQ2274.2
015300***************************************************************   SQ2274.2
015400*                                                             *   SQ2274.2
015500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2274.2
015600*                                                             *   SQ2274.2
015700***************************************************************   SQ2274.2
015800*                                                                 SQ2274.2
015900 01  REC-SKEL-SUB   PIC 99.                                       SQ2274.2
016000*                                                                 SQ2274.2
016100 01  FILE-RECORD-INFORMATION-REC.                                 SQ2274.2
016200     03 FILE-RECORD-INFO-SKELETON.                                SQ2274.2
016300        05 FILLER                 PICTURE X(48)       VALUE       SQ2274.2
016400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2274.2
016500        05 FILLER                 PICTURE X(46)       VALUE       SQ2274.2
016600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2274.2
016700        05 FILLER                 PICTURE X(26)       VALUE       SQ2274.2
016800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2274.2
016900        05 FILLER                 PICTURE X(37)       VALUE       SQ2274.2
017000             ",RECKEY=                             ".             SQ2274.2
017100        05 FILLER                 PICTURE X(38)       VALUE       SQ2274.2
017200             ",ALTKEY1=                             ".            SQ2274.2
017300        05 FILLER                 PICTURE X(38)       VALUE       SQ2274.2
017400             ",ALTKEY2=                             ".            SQ2274.2
017500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2274.2
017600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2274.2
017700        05 FILE-RECORD-INFO-P1-120.                               SQ2274.2
017800           07 FILLER              PIC X(5).                       SQ2274.2
017900           07 XFILE-NAME          PIC X(6).                       SQ2274.2
018000           07 FILLER              PIC X(8).                       SQ2274.2
018100           07 XRECORD-NAME        PIC X(6).                       SQ2274.2
018200           07 FILLER              PIC X(1).                       SQ2274.2
018300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2274.2
018400           07 FILLER              PIC X(7).                       SQ2274.2
018500           07 XRECORD-NUMBER      PIC 9(6).                       SQ2274.2
018600           07 FILLER              PIC X(6).                       SQ2274.2
018700           07 UPDATE-NUMBER       PIC 9(2).                       SQ2274.2
018800           07 FILLER              PIC X(5).                       SQ2274.2
018900           07 ODO-NUMBER          PIC 9(4).                       SQ2274.2
019000           07 FILLER              PIC X(5).                       SQ2274.2
019100           07 XPROGRAM-NAME       PIC X(5).                       SQ2274.2
019200           07 FILLER              PIC X(7).                       SQ2274.2
019300           07 XRECORD-LENGTH      PIC 9(6).                       SQ2274.2
019400           07 FILLER              PIC X(7).                       SQ2274.2
019500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2274.2
019600           07 FILLER              PIC X(1).                       SQ2274.2
019700           07 XBLOCK-SIZE         PIC 9(4).                       SQ2274.2
019800           07 FILLER              PIC X(6).                       SQ2274.2
019900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2274.2
020000           07 FILLER              PIC X(5).                       SQ2274.2
020100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2274.2
020200           07 FILLER              PIC X(6).                       SQ2274.2
020300           07 XLABEL-TYPE         PIC X(1).                       SQ2274.2
020400        05 FILE-RECORD-INFO-P121-240.                             SQ2274.2
020500           07 FILLER              PIC X(8).                       SQ2274.2
020600           07 XRECORD-KEY         PIC X(29).                      SQ2274.2
020700           07 FILLER              PIC X(9).                       SQ2274.2
020800           07 ALTERNATE-KEY1      PIC X(29).                      SQ2274.2
020900           07 FILLER              PIC X(9).                       SQ2274.2
021000           07 ALTERNATE-KEY2      PIC X(29).                      SQ2274.2
021100           07 FILLER              PIC X(7).                       SQ2274.2
021200*                                                                 SQ2274.2
021300 01  TEST-RESULTS.                                                SQ2274.2
021400     02 FILLER              PIC X      VALUE SPACE.               SQ2274.2
021500     02  PAR-NAME.                                                SQ2274.2
021600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2274.2
021700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2274.2
021800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2274.2
021900     02 FILLER              PIC X      VALUE SPACE.               SQ2274.2
022000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2274.2
022100     02 FILLER              PIC X      VALUE SPACE.               SQ2274.2
022200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2274.2
022300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2274.2
022400     02 RE-MARK             PIC X(61).                            SQ2274.2
022500 01  TEST-COMPUTED.                                               SQ2274.2
022600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2274.2
022700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2274.2
022800   02 COMPUTED-X.                                                 SQ2274.2
022900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2274.2
023000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2274.2
023100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2274.2
023200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2274.2
023300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2274.2
023400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2274.2
023500        04 COMPUTED-18V0                   PIC -9(18).            SQ2274.2
023600        04 FILLER                          PIC X.                 SQ2274.2
023700     03 FILLER PIC X(50) VALUE SPACE.                             SQ2274.2
023800 01  TEST-CORRECT.                                                SQ2274.2
023900     02 FILLER PIC X(30) VALUE SPACE.                             SQ2274.2
024000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2274.2
024100     02 CORRECT-X.                                                SQ2274.2
024200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2274.2
024300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2274.2
024400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2274.2
024500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2274.2
024600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2274.2
024700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2274.2
024800         04 CORRECT-18V0                     PIC -9(18).          SQ2274.2
024900         04 FILLER                           PIC X.               SQ2274.2
025000     03 FILLER PIC X(2) VALUE SPACE.                              SQ2274.2
025100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2274.2
025200*                                                                 SQ2274.2
025300 01  CCVS-C-1.                                                    SQ2274.2
025400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2274.2
025500     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ2274.2
025600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2274.2
025700     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ2274.2
025800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2274.2
025900     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ2274.2
026000     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ2274.2
026100     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ2274.2
026200 01  CCVS-C-2.                                                    SQ2274.2
026300     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2274.2
026400     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ2274.2
026500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2274.2
026600     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ2274.2
026700     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ2274.2
026800*                                                                 SQ2274.2
026900 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2274.2
027000 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2274.2
027100 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2274.2
027200 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2274.2
027300 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2274.2
027400 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2274.2
027500 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2274.2
027600 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2274.2
027700 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2274.2
027800 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2274.2
027900 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2274.2
028000 01  CCVS-H-1.                                                    SQ2274.2
028100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2274.2
028200     02  FILLER          PIC X(42)    VALUE                       SQ2274.2
028300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2274.2
028400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2274.2
028500 01  CCVS-H-2A.                                                   SQ2274.2
028600   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2274.2
028700   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2274.2
028800   02  FILLER            PIC XXXX   VALUE                         SQ2274.2
028900     "4.2 ".                                                      SQ2274.2
029000   02  FILLER            PIC X(28)  VALUE                         SQ2274.2
029100            " COPY - NOT FOR DISTRIBUTION".                       SQ2274.2
029200   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2274.2
029300*                                                                 SQ2274.2
029400 01  CCVS-H-2B.                                                   SQ2274.2
029500   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2274.2
029600   02  TEST-ID           PIC X(9).                                SQ2274.2
029700   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2274.2
029800   02  FILLER            PIC X(12)  VALUE                         SQ2274.2
029900     " HIGH       ".                                              SQ2274.2
030000   02  FILLER            PIC X(22)  VALUE                         SQ2274.2
030100            " LEVEL VALIDATION FOR ".                             SQ2274.2
030200   02  FILLER            PIC X(58)  VALUE                         SQ2274.2
030300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2274.2
030400 01  CCVS-H-3.                                                    SQ2274.2
030500     02  FILLER          PIC X(34)  VALUE                         SQ2274.2
030600            " FOR OFFICIAL USE ONLY    ".                         SQ2274.2
030700     02  FILLER          PIC X(58)  VALUE                         SQ2274.2
030800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2274.2
030900     02  FILLER          PIC X(28)  VALUE                         SQ2274.2
031000            "  COPYRIGHT   1985,1986 ".                           SQ2274.2
031100 01  CCVS-E-1.                                                    SQ2274.2
031200     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2274.2
031300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2274.2
031400     02 ID-AGAIN         PIC X(9).                                SQ2274.2
031500     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2274.2
031600 01  CCVS-E-2.                                                    SQ2274.2
031700     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2274.2
031800     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2274.2
031900     02  CCVS-E-2-2.                                              SQ2274.2
032000         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2274.2
032100         03 FILLER         PIC X      VALUE SPACE.                SQ2274.2
032200         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2274.2
032300            "ERRORS ENCOUNTERED".                                 SQ2274.2
032400 01  CCVS-E-3.                                                    SQ2274.2
032500     02  FILLER          PIC X(22)  VALUE                         SQ2274.2
032600            " FOR OFFICIAL USE ONLY".                             SQ2274.2
032700     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2274.2
032800     02  FILLER          PIC X(58)  VALUE                         SQ2274.2
032900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2274.2
033000     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2274.2
033100     02  FILLER          PIC X(20)  VALUE                         SQ2274.2
033200             " COPYRIGHT 1985,1986".                              SQ2274.2
033300 01  CCVS-E-4.                                                    SQ2274.2
033400     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2274.2
033500     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2274.2
033600     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2274.2
033700     02 FILLER           PIC X(40)  VALUE                         SQ2274.2
033800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2274.2
033900 01  XXINFO.                                                      SQ2274.2
034000     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2274.2
034100     02 INFO-TEXT.                                                SQ2274.2
034200       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2274.2
034300       04 XXCOMPUTED         PIC X(20).                           SQ2274.2
034400       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2274.2
034500       04 XXCORRECT          PIC X(20).                           SQ2274.2
034600     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2274.2
034700 01  HYPHEN-LINE.                                                 SQ2274.2
034800     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2274.2
034900     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2274.2
035000-    "*****************************************".                 SQ2274.2
035100     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2274.2
035200-    "******************************".                            SQ2274.2
035300 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2274.2
035400     "SQ227A".                                                    SQ2274.2
035500*                                                                 SQ2274.2
035600*                                                                 SQ2274.2
035700 PROCEDURE DIVISION.                                              SQ2274.2
035800 DECLARATIVES.                                                    SQ2274.2
035900*                                                                 SQ2274.2
036000 SECT-SQ227A-0001 SECTION.                                        SQ2274.2
036100     USE AFTER EXCEPTION PROCEDURE I-O.                           SQ2274.2
036200 I-O-ERROR-PROCESS.                                               SQ2274.2
036300     MOVE   "EXECUTED" TO DECL-EXEC-I-O.                          SQ2274.2
036400     IF DECL-EXEC-SW NOT = SPACE                                  SQ2274.2
036500         GO TO   END-DECLS.                                       SQ2274.2
036600*                                                                 SQ2274.2
036700     MOVE    1 TO REC-CT.                                         SQ2274.2
036800     MOVE   "DCL-REWRITE-01" TO PAR-NAME.                         SQ2274.2
036900     GO TO   DCL-REWRITE-01-01.                                   SQ2274.2
037000 DECL-DELETE-01-01.                                               SQ2274.2
037100     PERFORM DECL-DE-LETE.                                        SQ2274.2
037200     GO TO   DECL-TEST-01-01-END.                                 SQ2274.2
037300 DCL-REWRITE-01-01.                                               SQ2274.2
037400     IF SQ-FS4-STATUS = "44"                                      SQ2274.2
037500         PERFORM DECL-PASS                                        SQ2274.2
037600     ELSE                                                         SQ2274.2
037700         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
037800         MOVE   "44" TO CORRECT-A                                 SQ2274.2
037900         MOVE   "UNEXPECTED I-O STATUS ON FAILED REWRITE"         SQ2274.2
038000                   TO RE-MARK                                     SQ2274.2
038100         MOVE   "VII-4, VII-48,4.5.4(2)" TO ANSI-REFERENCE        SQ2274.2
038200         PERFORM DECL-FAIL.                                       SQ2274.2
038300 DECL-TEST-01-01-END.                                             SQ2274.2
038400*                                                                 SQ2274.2
038500     ADD     1 TO REC-CT.                                         SQ2274.2
038600     GO TO   DCL-REWRITE-01-02.                                   SQ2274.2
038700 DECL-DELETE-01-02.                                               SQ2274.2
038800     PERFORM DECL-DE-LETE.                                        SQ2274.2
038900     GO TO   DECL-TEST-01-02-END.                                 SQ2274.2
039000 DCL-REWRITE-01-02.                                               SQ2274.2
039100     IF SQ-FS4R1-F-G-120 = FILE-RECORD-INFO-P1-120 (1)            SQ2274.2
039200         PERFORM DECL-PASS                                        SQ2274.2
039300     ELSE                                                         SQ2274.2
039400         MOVE   "FIRST 120 CHARACTERS OF RECORD AREA CHANGED"     SQ2274.2
039500                   TO RE-MARK                                     SQ2274.2
039600         MOVE   "VII-4, VII-49,4.5.4(9)" TO ANSI-REFERENCE        SQ2274.2
039700         PERFORM DECL-FAIL.                                       SQ2274.2
039800 DECL-TEST-01-02-END.                                             SQ2274.2
039900*                                                                 SQ2274.2
040000     ADD     1 TO REC-CT.                                         SQ2274.2
040100     GO TO   DCL-REWRITE-01-03.                                   SQ2274.2
040200 DECL-DELETE-01-03.                                               SQ2274.2
040300     PERFORM DECL-DE-LETE.                                        SQ2274.2
040400     GO TO   DECL-TEST-01-03-END.                                 SQ2274.2
040500 DCL-REWRITE-01-03.                                               SQ2274.2
040600     IF EXT-18 = "ABCDEFGHIJKLMNOPQR"                             SQ2274.2
040700         PERFORM DECL-PASS                                        SQ2274.2
040800     ELSE                                                         SQ2274.2
040900         MOVE    EXT-18 TO COMPUTED-A                             SQ2274.2
041000         MOVE   "ABCDEFGHIJKLMNOPQR" TO CORRECT-A                 SQ2274.2
041100         MOVE   "LAST 18 CHARACTERS OF RECORD CHANGED"            SQ2274.2
041200                   TO RE-MARK                                     SQ2274.2
041300         MOVE   "VII-4, VII-49,4.5.4(9)" TO ANSI-REFERENCE        SQ2274.2
041400         PERFORM DECL-FAIL.                                       SQ2274.2
041500 DECL-TEST-01-03-END.                                             SQ2274.2
041600*                                                                 SQ2274.2
041700     PERFORM DECL-WRITE-LINE.                                     SQ2274.2
041800     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ2274.2
041900               TO DUMMY-RECORD.                                   SQ2274.2
042000     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ2274.2
042100     GO TO   END-DECLS.                                           SQ2274.2
042200*                                                                 SQ2274.2
042300*                                                                 SQ2274.2
042400 DECL-PASS.                                                       SQ2274.2
042500     MOVE   "PASS " TO P-OR-F.                                    SQ2274.2
042600     ADD     1 TO PASS-COUNTER.                                   SQ2274.2
042700     PERFORM DECL-PRINT-DETAIL.                                   SQ2274.2
042800*                                                                 SQ2274.2
042900 DECL-FAIL.                                                       SQ2274.2
043000     MOVE   "FAIL*" TO P-OR-F.                                    SQ2274.2
043100     ADD     1 TO ERROR-COUNTER.                                  SQ2274.2
043200     PERFORM DECL-PRINT-DETAIL.                                   SQ2274.2
043300*                                                                 SQ2274.2
043400 DECL-DE-LETE.                                                    SQ2274.2
043500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2274.2
043600     MOVE   "*****" TO P-OR-F.                                    SQ2274.2
043700     ADD     1 TO DELETE-COUNTER.                                 SQ2274.2
043800     PERFORM DECL-PRINT-DETAIL.                                   SQ2274.2
043900*                                                                 SQ2274.2
044000 DECL-PRINT-DETAIL.                                               SQ2274.2
044100     IF REC-CT NOT EQUAL TO ZERO                                  SQ2274.2
044200             MOVE "." TO PARDOT-X                                 SQ2274.2
044300             MOVE REC-CT TO DOTVALUE.                             SQ2274.2
044400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2274.2
044500     PERFORM DECL-WRITE-LINE.                                     SQ2274.2
044600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2274.2
044700         PERFORM DECL-WRITE-LINE                                  SQ2274.2
044800         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ2274.2
044900     ELSE                                                         SQ2274.2
045000         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ2274.2
045100     MOVE    SPACE TO P-OR-F.                                     SQ2274.2
045200     MOVE    SPACE TO COMPUTED-X.                                 SQ2274.2
045300     MOVE    SPACE TO CORRECT-X.                                  SQ2274.2
045400     IF REC-CT EQUAL TO ZERO                                      SQ2274.2
045500         MOVE    SPACE TO PAR-NAME.                               SQ2274.2
045600     MOVE    SPACE TO RE-MARK.                                    SQ2274.2
045700*                                                                 SQ2274.2
045800 DECL-WRITE-LINE.                                                 SQ2274.2
045900     ADD     1 TO RECORD-COUNT.                                   SQ2274.2
046000*    IF RECORD-COUNT GREATER 50                                   SQ2274.2
046100*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ2274.2
046200*        MOVE    SPACE TO DUMMY-RECORD                            SQ2274.2
046300*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ2274.2
046400*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ2274.2
046500*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ2274.2
046600*        PERFORM DECL-WRT-LN 2 TIMES                              SQ2274.2
046700*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ2274.2
046800*        PERFORM DECL-WRT-LN                                      SQ2274.2
046900*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ2274.2
047000*        MOVE    ZERO TO RECORD-COUNT.                            SQ2274.2
047100     PERFORM DECL-WRT-LN.                                         SQ2274.2
047200*                                                                 SQ2274.2
047300 DECL-WRT-LN.                                                     SQ2274.2
047400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2274.2
047500     MOVE    SPACE TO DUMMY-RECORD.                               SQ2274.2
047600*                                                                 SQ2274.2
047700 DECL-FAIL-ROUTINE.                                               SQ2274.2
047800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ2274.2
047900     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ2274.2
048000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2274.2
048100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2274.2
048200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2274.2
048300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2274.2
048400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2274.2
048500     GO TO   DECL-FAIL-EX.                                        SQ2274.2
048600 DECL-FAIL-WRITE.                                                 SQ2274.2
048700     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ2274.2
048800     PERFORM DECL-WRITE-LINE                                      SQ2274.2
048900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2274.2
049000     MOVE    TEST-CORRECT TO PRINT-REC                            SQ2274.2
049100     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2274.2
049200     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ2274.2
049300 DECL-FAIL-EX.                                                    SQ2274.2
049400     EXIT.                                                        SQ2274.2
049500*                                                                 SQ2274.2
049600 DECL-BAIL.                                                       SQ2274.2
049700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ2274.2
049800     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ2274.2
049900 DECL-BAIL-WRITE.                                                 SQ2274.2
050000     MOVE    CORRECT-A TO XXCORRECT.                              SQ2274.2
050100     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ2274.2
050200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2274.2
050300     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2274.2
050400 DECL-BAIL-EX.                                                    SQ2274.2
050500     EXIT.                                                        SQ2274.2
050600*                                                                 SQ2274.2
050700 END-DECLS.                                                       SQ2274.2
050800 END DECLARATIVES.                                                SQ2274.2
050900*                                                                 SQ2274.2
051000*                                                                 SQ2274.2
051100 CCVS1 SECTION.                                                   SQ2274.2
051200 OPEN-FILES.                                                      SQ2274.2
051300*    OPEN    I-O RAW-DATA.                                        SQ2274.2
051400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2274.2
051500*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ2274.2
051600*    MOVE   "ABORTED "   TO C-ABORT.                              SQ2274.2
051700*    ADD     1           TO C-NO-OF-TESTS.                        SQ2274.2
051800*    ACCEPT  C-DATE      FROM DATE.                               SQ2274.2
051900*    ACCEPT  C-TIME      FROM TIME.                               SQ2274.2
052000*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2274.2
052100*END-E-1.                                                         SQ2274.2
052200*    CLOSE   RAW-DATA.                                            SQ2274.2
052300     OPEN    OUTPUT PRINT-FILE.                                   SQ2274.2
052400     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2274.2
052500     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2274.2
052600     MOVE    SPACE TO TEST-RESULTS.                               SQ2274.2
052700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2274.2
052800     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2274.2
052900     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2274.2
053000     GO TO CCVS1-EXIT.                                            SQ2274.2
053100*                                                                 SQ2274.2
053200 CCVS-INIT-FILE.                                                  SQ2274.2
053300     ADD     1 TO REC-SKL-SUB.                                    SQ2274.2
053400     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2274.2
053500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2274.2
053600*                                                                 SQ2274.2
053700 CLOSE-FILES.                                                     SQ2274.2
053800     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2274.2
053900     CLOSE   PRINT-FILE.                                          SQ2274.2
054000*    OPEN    I-O RAW-DATA.                                        SQ2274.2
054100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2274.2
054200*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ2274.2
054300*    MOVE   "OK.     " TO C-ABORT.                                SQ2274.2
054400*    MOVE    PASS-COUNTER  TO C-OK.                               SQ2274.2
054500*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ2274.2
054600*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ2274.2
054700*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ2274.2
054800*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ2274.2
054900*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2274.2
055000*END-E-2.                                                         SQ2274.2
055100*    CLOSE   RAW-DATA.                                            SQ2274.2
055200 TERMINATE-CCVS.                                                  SQ2274.2
055300*    EXIT    PROGRAM.                                             SQ2274.2
055400     STOP    RUN.                                                 SQ2274.2
055500*                                                                 SQ2274.2
055600 INSPT.                                                           SQ2274.2
055700     MOVE   "INSPT" TO P-OR-F.                                    SQ2274.2
055800     ADD     1 TO INSPECT-COUNTER.                                SQ2274.2
055900     PERFORM PRINT-DETAIL.                                        SQ2274.2
056000*                                                                 SQ2274.2
056100 PASS.                                                            SQ2274.2
056200     MOVE   "PASS " TO P-OR-F.                                    SQ2274.2
056300     ADD     1 TO PASS-COUNTER.                                   SQ2274.2
056400     PERFORM PRINT-DETAIL.                                        SQ2274.2
056500*                                                                 SQ2274.2
056600 FAIL.                                                            SQ2274.2
056700     MOVE   "FAIL*" TO P-OR-F.                                    SQ2274.2
056800     ADD     1 TO ERROR-COUNTER.                                  SQ2274.2
056900     PERFORM PRINT-DETAIL.                                        SQ2274.2
057000*                                                                 SQ2274.2
057100 DE-LETE.                                                         SQ2274.2
057200     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2274.2
057300     MOVE   "*****" TO P-OR-F.                                    SQ2274.2
057400     ADD     1 TO DELETE-COUNTER.                                 SQ2274.2
057500     PERFORM PRINT-DETAIL.                                        SQ2274.2
057600*                                                                 SQ2274.2
057700 PRINT-DETAIL.                                                    SQ2274.2
057800     IF REC-CT NOT EQUAL TO ZERO                                  SQ2274.2
057900         MOVE   "." TO PARDOT-X                                   SQ2274.2
058000         MOVE    REC-CT TO DOTVALUE.                              SQ2274.2
058100     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2274.2
058200     PERFORM WRITE-LINE.                                          SQ2274.2
058300     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2274.2
058400         PERFORM WRITE-LINE                                       SQ2274.2
058500         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2274.2
058600     ELSE                                                         SQ2274.2
058700         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2274.2
058800     MOVE    SPACE TO P-OR-F.                                     SQ2274.2
058900     MOVE    SPACE TO COMPUTED-X.                                 SQ2274.2
059000     MOVE    SPACE TO CORRECT-X.                                  SQ2274.2
059100     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2274.2
059200     MOVE    SPACE TO RE-MARK.                                    SQ2274.2
059300*                                                                 SQ2274.2
059400 HEAD-ROUTINE.                                                    SQ2274.2
059500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2274.2
059600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2274.2
059700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2274.2
059800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2274.2
059900 COLUMN-NAMES-ROUTINE.                                            SQ2274.2
060000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2274.2
060100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2274.2
060200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2274.2
060300 END-ROUTINE.                                                     SQ2274.2
060400     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2274.2
060500     PERFORM WRITE-LINE 5 TIMES.                                  SQ2274.2
060600 END-RTN-EXIT.                                                    SQ2274.2
060700     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2274.2
060800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2274.2
060900*                                                                 SQ2274.2
061000 END-ROUTINE-1.                                                   SQ2274.2
061100     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2274.2
061200     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2274.2
061300     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2274.2
061400     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2274.2
061500     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2274.2
061600     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2274.2
061700     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2274.2
061800     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2274.2
061900     PERFORM WRITE-LINE.                                          SQ2274.2
062000     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2274.2
062100     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2274.2
062200         MOVE   "NO " TO ERROR-TOTAL                              SQ2274.2
062300     ELSE                                                         SQ2274.2
062400         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2274.2
062500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2274.2
062600     PERFORM WRITE-LINE.                                          SQ2274.2
062700 END-ROUTINE-13.                                                  SQ2274.2
062800     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2274.2
062900         MOVE   "NO " TO ERROR-TOTAL                              SQ2274.2
063000     ELSE                                                         SQ2274.2
063100         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2274.2
063200     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2274.2
063300     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2274.2
063400     PERFORM WRITE-LINE.                                          SQ2274.2
063500     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2274.2
063600         MOVE   "NO " TO ERROR-TOTAL                              SQ2274.2
063700     ELSE                                                         SQ2274.2
063800         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2274.2
063900     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2274.2
064000     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2274.2
064100     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2274.2
064200*                                                                 SQ2274.2
064300 WRITE-LINE.                                                      SQ2274.2
064400     ADD     1 TO RECORD-COUNT.                                   SQ2274.2
064500*    IF RECORD-COUNT GREATER 50                                   SQ2274.2
064600*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2274.2
064700*        MOVE  SPACE TO DUMMY-RECORD                              SQ2274.2
064800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2274.2
064900*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2274.2
065000*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2274.2
065100*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2274.2
065200*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2274.2
065300*        MOVE  ZERO TO RECORD-COUNT.                              SQ2274.2
065400     PERFORM WRT-LN.                                              SQ2274.2
065500*                                                                 SQ2274.2
065600 WRT-LN.                                                          SQ2274.2
065700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2274.2
065800     MOVE    SPACE TO DUMMY-RECORD.                               SQ2274.2
065900 BLANK-LINE-PRINT.                                                SQ2274.2
066000     PERFORM WRT-LN.                                              SQ2274.2
066100 FAIL-ROUTINE.                                                    SQ2274.2
066200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2274.2
066300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2274.2
066400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2274.2
066500     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2274.2
066600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2274.2
066700     PERFORM WRITE-LINE 2 TIMES.                                  SQ2274.2
066800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2274.2
066900     GO TO   FAIL-ROUTINE-EX.                                     SQ2274.2
067000 FAIL-ROUTINE-WRITE.                                              SQ2274.2
067100     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2274.2
067200     PERFORM WRITE-LINE                                           SQ2274.2
067300     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2274.2
067400     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2274.2
067500     PERFORM WRITE-LINE 2 TIMES.                                  SQ2274.2
067600     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2274.2
067700 FAIL-ROUTINE-EX.                                                 SQ2274.2
067800     EXIT.                                                        SQ2274.2
067900 BAIL-OUT.                                                        SQ2274.2
068000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2274.2
068100     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2274.2
068200 BAIL-OUT-WRITE.                                                  SQ2274.2
068300     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2274.2
068400     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2274.2
068500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2274.2
068600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2274.2
068700     PERFORM WRITE-LINE 2 TIMES.                                  SQ2274.2
068800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2274.2
068900 BAIL-OUT-EX.                                                     SQ2274.2
069000     EXIT.                                                        SQ2274.2
069100 CCVS1-EXIT.                                                      SQ2274.2
069200     EXIT.                                                        SQ2274.2
069300*                                                                 SQ2274.2
069400****************************************************************  SQ2274.2
069500*                                                              *  SQ2274.2
069600*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2274.2
069700*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2274.2
069800*                                                              *  SQ2274.2
069900****************************************************************  SQ2274.2
070000*                                                                 SQ2274.2
070100 SECT-SQ227A-0002 SECTION.                                        SQ2274.2
070200 STA-INIT.                                                        SQ2274.2
070300     MOVE    SPACE TO DELETE-SW.                                  SQ2274.2
070400*                                                                 SQ2274.2
070500     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ2274.2
070600     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ2274.2
070700     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ2274.2
070800     MOVE    120 TO XRECORD-LENGTH (1).                           SQ2274.2
070900     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ2274.2
071000     MOVE    1   TO XBLOCK-SIZE (1).                              SQ2274.2
071100     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ2274.2
071200     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ2274.2
071300     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ2274.2
071400*                                                                 SQ2274.2
071500*    OPEN THE FILE IN THE OUTPUT MODE                             SQ2274.2
071600*                                                                 SQ2274.2
071700 SEQ-INIT-01.                                                     SQ2274.2
071800     MOVE    0 TO REC-CT.                                         SQ2274.2
071900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2274.2
072000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2274.2
072100     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2274.2
072200     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2274.2
072300     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ2274.2
072400     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ2274.2
072500     GO TO   SEQ-TEST-OP-01.                                      SQ2274.2
072600 SEQ-DELETE-01.                                                   SQ2274.2
072700     MOVE   "*" TO DELETE-SW-1.                                   SQ2274.2
072800     GO TO   SEQ-DELETE-01-01.                                    SQ2274.2
072900 SEQ-TEST-OP-01.                                                  SQ2274.2
073000     OPEN    OUTPUT SQ-FS4.                                       SQ2274.2
073100*                                                                 SQ2274.2
073200*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ2274.2
073300*                                                                 SQ2274.2
073400     ADD    1 TO REC-CT.                                          SQ2274.2
073500     IF DELETE-SW NOT = SPACE                                     SQ2274.2
073600         GO TO   SEQ-DELETE-01-01.                                SQ2274.2
073700     GO TO   SEQ-TEST-OP-01-01.                                   SQ2274.2
073800 SEQ-DELETE-01-01.                                                SQ2274.2
073900     PERFORM DE-LETE.                                             SQ2274.2
074000     GO TO   SEQ-TEST-01-01-END.                                  SQ2274.2
074100 SEQ-TEST-OP-01-01.                                               SQ2274.2
074200     IF SQ-FS4-STATUS = "00"                                      SQ2274.2
074300         PERFORM PASS                                             SQ2274.2
074400     ELSE                                                         SQ2274.2
074500         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
074600         MOVE   "00" TO CORRECT-A                                 SQ2274.2
074700         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ2274.2
074800                   TO RE-MARK                                     SQ2274.2
074900         MOVE   "VII-3, VII-43" TO ANSI-REFERENCE                 SQ2274.2
075000         PERFORM FAIL.                                            SQ2274.2
075100 SEQ-TEST-01-01-END.                                              SQ2274.2
075200*                                                                 SQ2274.2
075300*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2274.2
075400*                                                                 SQ2274.2
075500     ADD     1 TO REC-CT.                                         SQ2274.2
075600     IF DELETE-SW NOT = SPACE                                     SQ2274.2
075700         GO TO   SEQ-DELETE-01-02.                                SQ2274.2
075800     GO TO   SEQ-TEST-OP-01-02.                                   SQ2274.2
075900 SEQ-DELETE-01-02.                                                SQ2274.2
076000     PERFORM DE-LETE.                                             SQ2274.2
076100     GO TO   SEQ-TEST-01-02-END.                                  SQ2274.2
076200 SEQ-TEST-OP-01-02.                                               SQ2274.2
076300     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2274.2
076400         PERFORM PASS                                             SQ2274.2
076500     ELSE                                                         SQ2274.2
076600         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2274.2
076700         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2274.2
076800         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2274.2
076900                   TO RE-MARK                                     SQ2274.2
077000         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2274.2
077100         PERFORM FAIL.                                            SQ2274.2
077200 SEQ-TEST-01-02-END.                                              SQ2274.2
077300*                                                                 SQ2274.2
077400*                                                                 SQ2274.2
077500*    A NEW FILE IS OPEN.  WE NOW WRITE ONE RECORD OF 120 CHARS.   SQ2274.2
077600*                                                                 SQ2274.2
077700 SEQ-INIT-02.                                                     SQ2274.2
077800     MOVE    0 TO REC-CT.                                         SQ2274.2
077900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2274.2
078000     ADD     1 TO XRECORD-NUMBER (1).                             SQ2274.2
078100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2274.2
078200     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2274.2
078300     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ2274.2
078400     MOVE   "987654321123456789" TO EXT-18.                       SQ2274.2
078500     MOVE    120 TO SQ-FS4-RECSIZE.                               SQ2274.2
078600     MOVE   "WRITE A RECORD"  TO FEATURE.                         SQ2274.2
078700     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ2274.2
078800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2274.2
078900         GO TO SEQ-DELETE-02.                                     SQ2274.2
079000     GO TO   SEQ-TEST-WR-02.                                      SQ2274.2
079100 SEQ-DELETE-02.                                                   SQ2274.2
079200     MOVE   "*" TO DELETE-SW-2.                                   SQ2274.2
079300     GO TO   SEQ-DELETE-02-01.                                    SQ2274.2
079400 SEQ-TEST-WR-02.                                                  SQ2274.2
079500     WRITE   SQ-FS4R2-F-G-138.                                    SQ2274.2
079600*                                                                 SQ2274.2
079700*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ2274.2
079800*                                                                 SQ2274.2
079900     ADD    1 TO REC-CT.                                          SQ2274.2
080000     IF DELETE-SW NOT = SPACE                                     SQ2274.2
080100         GO TO   SEQ-DELETE-02-01.                                SQ2274.2
080200     GO TO   SEQ-TEST-WR-02-01.                                   SQ2274.2
080300 SEQ-DELETE-02-01.                                                SQ2274.2
080400     PERFORM DE-LETE.                                             SQ2274.2
080500     GO TO   SEQ-TEST-02-01-END.                                  SQ2274.2
080600 SEQ-TEST-WR-02-01.                                               SQ2274.2
080700     IF SQ-FS4-STATUS = "00"                                      SQ2274.2
080800         PERFORM PASS                                             SQ2274.2
080900     ELSE                                                         SQ2274.2
081000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
081100         MOVE   "00" TO CORRECT-A                                 SQ2274.2
081200         MOVE   "UNEXPECTED ERROR CODE FROM WRITE"                SQ2274.2
081300                   TO RE-MARK                                     SQ2274.2
081400         MOVE   "VII-3, VII-53" TO ANSI-REFERENCE                 SQ2274.2
081500         PERFORM FAIL.                                            SQ2274.2
081600 SEQ-TEST-02-01-END.                                              SQ2274.2
081700*                                                                 SQ2274.2
081800*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2274.2
081900*                                                                 SQ2274.2
082000     ADD     1 TO REC-CT.                                         SQ2274.2
082100     IF DELETE-SW NOT = SPACE                                     SQ2274.2
082200         GO TO   SEQ-DELETE-02-02.                                SQ2274.2
082300     GO TO   SEQ-TEST-WR-02-02.                                   SQ2274.2
082400 SEQ-DELETE-02-02.                                                SQ2274.2
082500     PERFORM DE-LETE.                                             SQ2274.2
082600     GO TO   SEQ-TEST-02-02-END.                                  SQ2274.2
082700 SEQ-TEST-WR-02-02.                                               SQ2274.2
082800     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2274.2
082900         PERFORM PASS                                             SQ2274.2
083000     ELSE                                                         SQ2274.2
083100         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2274.2
083200         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2274.2
083300         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2274.2
083400                   TO RE-MARK                                     SQ2274.2
083500         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2274.2
083600         PERFORM FAIL.                                            SQ2274.2
083700 SEQ-TEST-02-02-END.                                              SQ2274.2
083800*                                                                 SQ2274.2
083900*                                                                 SQ2274.2
084000*    NOW CLOSE THE FILE.                                          SQ2274.2
084100*                                                                 SQ2274.2
084200 SEQ-INIT-03.                                                     SQ2274.2
084300     MOVE    0 TO REC-CT.                                         SQ2274.2
084400     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2274.2
084500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2274.2
084600     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2274.2
084700     MOVE   "CLOSE FILE" TO FEATURE.                              SQ2274.2
084800     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ2274.2
084900     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2274.2
085000         GO TO SEQ-DELETE-03.                                     SQ2274.2
085100     GO TO   SEQ-TEST-CL-03.                                      SQ2274.2
085200 SEQ-DELETE-03.                                                   SQ2274.2
085300     MOVE   "*" TO DELETE-SW-2.                                   SQ2274.2
085400     GO TO   SEQ-DELETE-03-01.                                    SQ2274.2
085500 SEQ-TEST-CL-03.                                                  SQ2274.2
085600     CLOSE   SQ-FS4.                                              SQ2274.2
085700*                                                                 SQ2274.2
085800*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ2274.2
085900*                                                                 SQ2274.2
086000     ADD    1 TO REC-CT.                                          SQ2274.2
086100     IF DELETE-SW NOT = SPACE                                     SQ2274.2
086200         GO TO   SEQ-DELETE-03-01.                                SQ2274.2
086300     GO TO   SEQ-TEST-CL-03-01.                                   SQ2274.2
086400 SEQ-DELETE-03-01.                                                SQ2274.2
086500     PERFORM DE-LETE.                                             SQ2274.2
086600     GO TO   SEQ-TEST-03-01-END.                                  SQ2274.2
086700 SEQ-TEST-CL-03-01.                                               SQ2274.2
086800     IF SQ-FS4-STATUS = "00"                                      SQ2274.2
086900         PERFORM PASS                                             SQ2274.2
087000     ELSE                                                         SQ2274.2
087100         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
087200         MOVE   "00" TO CORRECT-A                                 SQ2274.2
087300         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ2274.2
087400                   TO RE-MARK                                     SQ2274.2
087500         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ2274.2
087600         PERFORM FAIL.                                            SQ2274.2
087700 SEQ-TEST-03-01-END.                                              SQ2274.2
087800*                                                                 SQ2274.2
087900*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2274.2
088000*                                                                 SQ2274.2
088100     ADD     1 TO REC-CT.                                         SQ2274.2
088200     IF DELETE-SW NOT = SPACE                                     SQ2274.2
088300         GO TO   SEQ-DELETE-03-02.                                SQ2274.2
088400     GO TO   SEQ-TEST-CL-03-02.                                   SQ2274.2
088500 SEQ-DELETE-03-02.                                                SQ2274.2
088600     PERFORM DE-LETE.                                             SQ2274.2
088700     GO TO   SEQ-TEST-03-02-END.                                  SQ2274.2
088800 SEQ-TEST-CL-03-02.                                               SQ2274.2
088900     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2274.2
089000         PERFORM PASS                                             SQ2274.2
089100     ELSE                                                         SQ2274.2
089200         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2274.2
089300         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2274.2
089400         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2274.2
089500                   TO RE-MARK                                     SQ2274.2
089600         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2274.2
089700         PERFORM FAIL.                                            SQ2274.2
089800 SEQ-TEST-03-02-END.                                              SQ2274.2
089900     MOVE    SPACE TO DELETE-SW-2.                                SQ2274.2
090000*                                                                 SQ2274.2
090100*                                                                 SQ2274.2
090200*    OPEN THE FILE IN THE I-O MODE                                SQ2274.2
090300*                                                                 SQ2274.2
090400 SEQ-INIT-04.                                                     SQ2274.2
090500     MOVE    0 TO REC-CT.                                         SQ2274.2
090600     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2274.2
090700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2274.2
090800     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2274.2
090900     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2274.2
091000     MOVE   "OPEN FILE FOR I-O" TO FEATURE.                       SQ2274.2
091100     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ2274.2
091200     IF DELETE-SW NOT = SPACE                                     SQ2274.2
091300         GO TO SEQ-DELETE-04-01.                                  SQ2274.2
091400     GO TO   SEQ-TEST-OP-04.                                      SQ2274.2
091500 SEQ-DELETE-04.                                                   SQ2274.2
091600     MOVE   "*" TO DELETE-SW-2.                                   SQ2274.2
091700     GO TO   SEQ-DELETE-04-01.                                    SQ2274.2
091800 SEQ-TEST-OP-04.                                                  SQ2274.2
091900     OPEN    I-O SQ-FS4.                                          SQ2274.2
092000*                                                                 SQ2274.2
092100*    CHECK I-O STATUS RETURNED FROM OPEN I-O                      SQ2274.2
092200*                                                                 SQ2274.2
092300     ADD    1 TO REC-CT.                                          SQ2274.2
092400     IF DELETE-SW NOT = SPACE                                     SQ2274.2
092500         GO TO   SEQ-DELETE-04-01.                                SQ2274.2
092600     GO TO   SEQ-TEST-OP-04-01.                                   SQ2274.2
092700 SEQ-DELETE-04-01.                                                SQ2274.2
092800     PERFORM DE-LETE.                                             SQ2274.2
092900     GO TO   SEQ-TEST-04-01-END.                                  SQ2274.2
093000 SEQ-TEST-OP-04-01.                                               SQ2274.2
093100     IF SQ-FS4-STATUS = "00"                                      SQ2274.2
093200         PERFORM PASS                                             SQ2274.2
093300     ELSE                                                         SQ2274.2
093400         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
093500         MOVE   "00" TO CORRECT-A                                 SQ2274.2
093600         MOVE   "UNEXPECTED I-O STATUS CODE FROM OPEN I-O"        SQ2274.2
093700                   TO RE-MARK                                     SQ2274.2
093800         MOVE   "VII-3, VII-40" TO ANSI-REFERENCE                 SQ2274.2
093900         PERFORM FAIL.                                            SQ2274.2
094000 SEQ-TEST-04-01-END.                                              SQ2274.2
094100*                                                                 SQ2274.2
094200     ADD     1 TO REC-CT.                                         SQ2274.2
094300     IF DELETE-SW NOT = SPACE                                     SQ2274.2
094400         GO TO   SEQ-DELETE-04-02.                                SQ2274.2
094500     GO TO   SEQ-TEST-OP-04-02.                                   SQ2274.2
094600 SEQ-DELETE-04-02.                                                SQ2274.2
094700     PERFORM DE-LETE.                                             SQ2274.2
094800     GO TO   SEQ-TEST-04-02-END.                                  SQ2274.2
094900 SEQ-TEST-OP-04-02.                                               SQ2274.2
095000     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2274.2
095100         PERFORM PASS                                             SQ2274.2
095200     ELSE                                                         SQ2274.2
095300         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2274.2
095400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2274.2
095500         MOVE  "UNEXPECTED EXECUTION OF I-O DECLARATIVE ON OPEN"  SQ2274.2
095600                   TO RE-MARK                                     SQ2274.2
095700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2274.2
095800         PERFORM FAIL.                                            SQ2274.2
095900 SEQ-TEST-04-02-END.                                              SQ2274.2
096000*                                                                 SQ2274.2
096100*                                                                 SQ2274.2
096200*    THE FILE IS OPEN FOR I-O.  WE READ THE ONLY RECORD.          SQ2274.2
096300*                                                                 SQ2274.2
096400 SEQ-INIT-05.                                                     SQ2274.2
096500     MOVE    0 TO REC-CT.                                         SQ2274.2
096600     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2274.2
096700     ADD     1 TO XRECORD-NUMBER (1).                             SQ2274.2
096800     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2274.2
096900     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2274.2
097000     MOVE    ZERO TO SQ-FS4-RECSIZE.                              SQ2274.2
097100     MOVE    SPACE TO SQ-FS4R2-F-G-138.                           SQ2274.2
097200     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ2274.2
097300     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ2274.2
097400     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2274.2
097500         GO TO SEQ-DELETE-05.                                     SQ2274.2
097600     GO TO   SEQ-TEST-RD-05.                                      SQ2274.2
097700 SEQ-DELETE-05.                                                   SQ2274.2
097800     MOVE   "*" TO DELETE-SW-2.                                   SQ2274.2
097900     GO TO   SEQ-DELETE-05-01.                                    SQ2274.2
098000 SEQ-TEST-RD-05.                                                  SQ2274.2
098100     READ    SQ-FS4.                                              SQ2274.2
098200*                                                                 SQ2274.2
098300*    CHECK I-O STATUS RETURNED FROM READ                          SQ2274.2
098400*                                                                 SQ2274.2
098500     ADD    1 TO REC-CT.                                          SQ2274.2
098600     IF DELETE-SW NOT = SPACE                                     SQ2274.2
098700         GO TO   SEQ-DELETE-05-01.                                SQ2274.2
098800     GO TO   SEQ-TEST-RD-05-01.                                   SQ2274.2
098900 SEQ-DELETE-05-01.                                                SQ2274.2
099000     PERFORM DE-LETE.                                             SQ2274.2
099100     GO TO   SEQ-TEST-05-01-END.                                  SQ2274.2
099200 SEQ-TEST-RD-05-01.                                               SQ2274.2
099300     IF SQ-FS4-STATUS = "00"                                      SQ2274.2
099400         PERFORM PASS                                             SQ2274.2
099500     ELSE                                                         SQ2274.2
099600         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
099700         MOVE   "00" TO CORRECT-A                                 SQ2274.2
099800         MOVE   "UNEXPECTED STATUS CODE FROM READ"                SQ2274.2
099900                   TO RE-MARK                                     SQ2274.2
100000         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ2274.2
100100         PERFORM FAIL.                                            SQ2274.2
100200 SEQ-TEST-05-01-END.                                              SQ2274.2
100300*                                                                 SQ2274.2
100400*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2274.2
100500*                                                                 SQ2274.2
100600     ADD     1 TO REC-CT.                                         SQ2274.2
100700     IF DELETE-SW NOT = SPACE                                     SQ2274.2
100800         GO TO   SEQ-DELETE-05-02.                                SQ2274.2
100900     GO TO   SEQ-TEST-RD-05-02.                                   SQ2274.2
101000 SEQ-DELETE-05-02.                                                SQ2274.2
101100     PERFORM DE-LETE.                                             SQ2274.2
101200     GO TO   SEQ-TEST-05-02-END.                                  SQ2274.2
101300 SEQ-TEST-RD-05-02.                                               SQ2274.2
101400     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2274.2
101500         PERFORM PASS                                             SQ2274.2
101600     ELSE                                                         SQ2274.2
101700         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2274.2
101800         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2274.2
101900         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2274.2
102000                   TO RE-MARK                                     SQ2274.2
102100         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2274.2
102200         PERFORM FAIL.                                            SQ2274.2
102300 SEQ-TEST-05-02-END.                                              SQ2274.2
102400*                                                                 SQ2274.2
102500*    CHECK THAT THE CORRECT RECORD HAS BEEN RETURNED, BY          SQ2274.2
102600*    CHECKING THE RECORD-NUMBER FIELD.                            SQ2274.2
102700*                                                                 SQ2274.2
102800     ADD     1 TO REC-CT.                                         SQ2274.2
102900     IF DELETE-SW NOT = SPACE                                     SQ2274.2
103000         GO TO   SEQ-DELETE-05-03.                                SQ2274.2
103100     GO TO   SEQ-TEST-RD-05-03.                                   SQ2274.2
103200 SEQ-DELETE-05-03.                                                SQ2274.2
103300     PERFORM DE-LETE.                                             SQ2274.2
103400     GO TO   SEQ-TEST-05-03-END.                                  SQ2274.2
103500 SEQ-TEST-RD-05-03.                                               SQ2274.2
103600     IF FRECORD-NUMBER = XRECORD-NUMBER (1)                       SQ2274.2
103700         PERFORM PASS                                             SQ2274.2
103800     ELSE                                                         SQ2274.2
103900         MOVE    FRECORD-NUMBER TO COMPUTED-18V0                  SQ2274.2
104000         MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0               SQ2274.2
104100         MOVE   "INCORRECT RECORD NUMBER FOUND" TO RE-MARK        SQ2274.2
104200         PERFORM FAIL.                                            SQ2274.2
104300 SEQ-TEST-05-03-END.                                              SQ2274.2
104400*                                                                 SQ2274.2
104500*    CHECK THE LENGTH OF THE RECORD RETURNED                      SQ2274.2
104600*                                                                 SQ2274.2
104700     ADD     1 TO REC-CT.                                         SQ2274.2
104800     IF DELETE-SW NOT = SPACE                                     SQ2274.2
104900         GO TO   SEQ-DELETE-05-04.                                SQ2274.2
105000     GO TO   SEQ-TEST-RD-05-04.                                   SQ2274.2
105100 SEQ-DELETE-05-04.                                                SQ2274.2
105200     PERFORM DE-LETE.                                             SQ2274.2
105300     GO TO   SEQ-TEST-05-04-END.                                  SQ2274.2
105400 SEQ-TEST-RD-05-04.                                               SQ2274.2
105500     IF SQ-FS4-RECSIZE = 120                                      SQ2274.2
105600         PERFORM PASS                                             SQ2274.2
105700     ELSE                                                         SQ2274.2
105800         MOVE    SQ-FS4-RECSIZE TO COMPUTED-18V0                  SQ2274.2
105900         MOVE    120 TO CORRECT-18V0                              SQ2274.2
106000         MOVE   "INCORRECT RECORD LENGTH RETURNED" TO RE-MARK     SQ2274.2
106100         MOVE   "VII-32, 3.8.4(11)" TO ANSI-REFERENCE             SQ2274.2
106200         PERFORM FAIL.                                            SQ2274.2
106300 SEQ-TEST-05-04-END.                                              SQ2274.2
106400     MOVE    SPACE TO DELETE-SW-2.                                SQ2274.2
106500*                                                                 SQ2274.2
106600*                                                                 SQ2274.2
106700*    TRY TO WRITE A RECORD OF A DIFFERENT SIZE THAN READ          SQ2274.2
106800*                                                                 SQ2274.2
106900 SEQ-INIT-06.                                                     SQ2274.2
107000     MOVE    0 TO REC-CT.                                         SQ2274.2
107100     MOVE    SPACE TO DECL-EXEC-SW.                               SQ2274.2
107200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2274.2
107300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2274.2
107400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ2274.2
107500     MOVE   "ABCDEFGHIJKLMNOPQR" TO EXT-18.                       SQ2274.2
107600     MOVE    130 TO SQ-FS4-RECSIZE.                               SQ2274.2
107700     MOVE   "REWRITE DIFFERENT SIZE REC" TO FEATURE.              SQ2274.2
107800     MOVE   "SEQ-TEST-RW-06" TO PAR-NAME.                         SQ2274.2
107900     IF DELETE-SW NOT = SPACE                                     SQ2274.2
108000         GO TO SEQ-DELETE-06-01.                                  SQ2274.2
108100     GO TO   SEQ-TEST-RW-06.                                      SQ2274.2
108200 SEQ-DELETE-06.                                                   SQ2274.2
108300     MOVE   "*" TO DELETE-SW-2.                                   SQ2274.2
108400     GO TO   SEQ-DELETE-06-01.                                    SQ2274.2
108500 SEQ-TEST-RW-06.                                                  SQ2274.2
108600     REWRITE SQ-FS4R1-F-G-120.                                    SQ2274.2
108700     MOVE    0 TO REC-CT.                                         SQ2274.2
108800*                                                                 SQ2274.2
108900*    CHECK I-O STATUS RETURNED FROM REWRITE                       SQ2274.2
109000*                                                                 SQ2274.2
109100     ADD    1 TO REC-CT.                                          SQ2274.2
109200     IF DELETE-SW NOT = SPACE                                     SQ2274.2
109300         GO TO   SEQ-DELETE-06-01.                                SQ2274.2
109400     GO TO   SEQ-TEST-RW-06-01.                                   SQ2274.2
109500 SEQ-DELETE-06-01.                                                SQ2274.2
109600     PERFORM DE-LETE.                                             SQ2274.2
109700     GO TO   SEQ-TEST-06-01-END.                                  SQ2274.2
109800 SEQ-TEST-RW-06-01.                                               SQ2274.2
109900     IF SQ-FS4-STATUS = "44"                                      SQ2274.2
110000         PERFORM PASS                                             SQ2274.2
110100     ELSE                                                         SQ2274.2
110200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2274.2
110300         MOVE   "44" TO CORRECT-A                                 SQ2274.2
110400         MOVE   "UNEXPECTED STATUS CODE FROM REWRITE              SQ2274.2
110500-        "OF DIFF SZ REC THAN READ"                               SQ2274.2
110600                   TO RE-MARK                                     SQ2274.2
110700         MOVE   "VII-41.3.5(4)D, VII-48" TO ANSI-REFERENCE        SQ2274.2
110800         PERFORM FAIL.                                            SQ2274.2
110900 SEQ-TEST-06-01-END.                                              SQ2274.2
111000 CCVS-EXIT SECTION.                                               SQ2274.2
111100 CCVS-999999.                                                     SQ2274.2
111200     GO TO   CLOSE-FILES.                                         SQ2274.2
      *END-OF,SQ227A                                                                  
