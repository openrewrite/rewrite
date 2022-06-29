      *HEADER,COBOL,SQ134A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1344.2
000200 PROGRAM-ID.                                                      SQ1344.2
000300     SQ134A.                                                      SQ1344.2
000400****************************************************************  SQ1344.2
000500*                                                              *  SQ1344.2
000600*    VALIDATION FOR:-                                          *  SQ1344.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1344.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1344.2
000900*                             REVISED 1986, AUGUST             *  SQ1344.2
001000*                                                              *  SQ1344.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1344.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1344.2
001300*                                                              *  SQ1344.2
001400****************************************************************  SQ1344.2
001500*                                                              *  SQ1344.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1344.2
001700*                                                              *  SQ1344.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1344.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1344.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1344.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1344.2
002200*                                                              *  SQ1344.2
002300****************************************************************  SQ1344.2
002400*                                                              *  SQ1344.2
002500*    THIS PROGRAM OPENS FOR OUTPUT A FILE WHICH IS ASSIGNED    *  SQ1344.2
002600*    TO A MASS STORAGE MEDIUM, WRITES ONE RECORD AND CLOSES    *  SQ1344.2
002700*    THE FILE.  TWO RECORD SIZES ARE DEFINED FOR THE FILE, BY  *  SQ1344.2
002800*    MEANS OF THE RECORD CONTAINS CLAUSE.  THE FILE IS THEN    *  SQ1344.2
002900*    OPENED FOR I-O, AND A READ STATEMENT AND A REWRITE        *  SQ1344.2
003000*    STATEMENT ARE EXECUTED.  THE REWRITE STATEMENT REFERENCES *  SQ1344.2
003100*    A RECORD OF A DIFFERENT SIZE TO THAT REFERENCED IN THE    *  SQ1344.2
003200*    WRITE STATEMENT, AND SHOULD CAUSE AN EXCEPTION CONDITION  *  SQ1344.2
003300*    WITH I-O STATUS "44".  THIS LOGIC ERROR SHOULD CAUSE      *  SQ1344.2
003400*    ENTRY TO THE APPLICABLE ERROR DECLARATIVE.                *  SQ1344.2
003500*                                                              *  SQ1344.2
003600*    THIS PROGRAM SHOULD BE RUN ONLY WHEN AN IMPLEMENTATION    *  SQ1344.2
003700*    PROVIDES VARIABLE LENGTH RECORDS FOR THE RECORDS CONTAINS *  SQ1344.2
003800*    INTEGER TO INTEGER CLAUSE                                 *  SQ1344.2
003900*                                                              *  SQ1344.2
004000*    THIS PROGRAM HAS BEEN SPLIT FROM V2.0 ONWARDS.            *  SQ1344.2
004100*    THE NEW PROGRAM IS SQ145A.                                *  SQ1344.2
004200****************************************************************  SQ1344.2
004300*                                                                 SQ1344.2
004400 ENVIRONMENT DIVISION.                                            SQ1344.2
004500 CONFIGURATION SECTION.                                           SQ1344.2
004600 SOURCE-COMPUTER.                                                 SQ1344.2
004700     XXXXX082.                                                    SQ1344.2
004800 OBJECT-COMPUTER.                                                 SQ1344.2
004900     XXXXX083.                                                    SQ1344.2
005000*                                                                 SQ1344.2
005100 INPUT-OUTPUT SECTION.                                            SQ1344.2
005200 FILE-CONTROL.                                                    SQ1344.2
005300     SELECT PRINT-FILE ASSIGN TO                                  SQ1344.2
005400     XXXXX055.                                                    SQ1344.2
005500*                                                                 SQ1344.2
005600*    SELECT RAW-DATA   ASSIGN TO                                  SQ1344.2
005700*    XXXXX062                                                     SQ1344.2
005800*          ORGANIZATION IS INDEXED                                SQ1344.2
005900*          ACCESS MODE  IS RANDOM                                 SQ1344.2
006000*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1344.2
006100*                                                                 SQ1344.2
006200     SELECT SQ-FS4                                                SQ1344.2
006300            ASSIGN                                                SQ1344.2
006400     XXXXX014                                                     SQ1344.2
006500            STATUS SQ-FS4-STATUS OF STATUS-GROUP                  SQ1344.2
006600            SEQUENTIAL                                            SQ1344.2
006700            .                                                     SQ1344.2
006800*                                                                 SQ1344.2
006900*                                                                 SQ1344.2
007000 DATA DIVISION.                                                   SQ1344.2
007100 FILE SECTION.                                                    SQ1344.2
007200 FD  PRINT-FILE                                                   SQ1344.2
007300C    LABEL RECORDS                                                SQ1344.2
007400C    XXXXX084                                                     SQ1344.2
007500C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1344.2
007600               .                                                  SQ1344.2
007700 01  PRINT-REC    PICTURE X(120).                                 SQ1344.2
007800 01  DUMMY-RECORD PICTURE X(120).                                 SQ1344.2
007900*                                                                 SQ1344.2
008000*FD  RAW-DATA.                                                    SQ1344.2
008100*01  RAW-DATA-SATZ.                                               SQ1344.2
008200*    05  RAW-DATA-KEY        PIC X(6).                            SQ1344.2
008300*    05  C-DATE              PIC 9(6).                            SQ1344.2
008400*    05  C-TIME              PIC 9(8).                            SQ1344.2
008500*    05  NO-OF-TESTS         PIC 99.                              SQ1344.2
008600*    05  C-OK                PIC 999.                             SQ1344.2
008700*    05  C-ALL               PIC 999.                             SQ1344.2
008800*    05  C-FAIL              PIC 999.                             SQ1344.2
008900*    05  C-DELETED           PIC 999.                             SQ1344.2
009000*    05  C-INSPECT           PIC 999.                             SQ1344.2
009100*    05  C-NOTE              PIC X(13).                           SQ1344.2
009200*    05  C-INDENT            PIC X.                               SQ1344.2
009300*    05  C-ABORT             PIC X(8).                            SQ1344.2
009400*                                                                 SQ1344.2
009500 FD  SQ-FS4                                                       SQ1344.2
009600C    LABEL RECORD IS STANDARD                                     SQ1344.2
009700     BLOCK  120 CHARACTERS                                        SQ1344.2
009800     RECORD CONTAINS 120 TO 138 CHARACTERS                        SQ1344.2
009900                .                                                 SQ1344.2
010000 01  SQ-FS4R1-F-G-120.                                            SQ1344.2
010100        05 FFILE-RECORD-INFO-P1-120.                              SQ1344.2
010200           07 FILLER              PIC X(5).                       SQ1344.2
010300           07 FFILE-NAME          PIC X(6).                       SQ1344.2
010400           07 FILLER              PIC X(8).                       SQ1344.2
010500           07 FRECORD-NAME        PIC X(6).                       SQ1344.2
010600           07 FILLER              PIC X(1).                       SQ1344.2
010700           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ1344.2
010800           07 FILLER              PIC X(7).                       SQ1344.2
010900           07 FRECORD-NUMBER      PIC 9(6).                       SQ1344.2
011000           07 FILLER              PIC X(6).                       SQ1344.2
011100           07 FUPDATE-NUMBER      PIC 9(2).                       SQ1344.2
011200           07 FILLER              PIC X(5).                       SQ1344.2
011300           07 FODO-NUMBER         PIC 9(4).                       SQ1344.2
011400           07 FILLER              PIC X(5).                       SQ1344.2
011500           07 FPROGRAM-NAME       PIC X(5).                       SQ1344.2
011600           07 FILLER              PIC X(7).                       SQ1344.2
011700           07 FRECORD-LENGTH      PIC 9(6).                       SQ1344.2
011800           07 FILLER              PIC X(7).                       SQ1344.2
011900           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ1344.2
012000           07 FILLER              PIC X(1).                       SQ1344.2
012100           07 FBLOCK-SIZE         PIC 9(4).                       SQ1344.2
012200           07 FILLER              PIC X(6).                       SQ1344.2
012300           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ1344.2
012400           07 FILLER              PIC X(5).                       SQ1344.2
012500           07 FFILE-ORGANIZATION  PIC X(2).                       SQ1344.2
012600           07 FILLER              PIC X(6).                       SQ1344.2
012700           07 FLABEL-TYPE         PIC X(1).                       SQ1344.2
012800*                                                                 SQ1344.2
012900 01  SQ-FS4R2-F-G-138.                                            SQ1344.2
013000     03  FILLER PIC X(120).                                       SQ1344.2
013100     03  EXT-18 PIC X(18).                                        SQ1344.2
013200*                                                                 SQ1344.2
013300 WORKING-STORAGE SECTION.                                         SQ1344.2
013400*                                                                 SQ1344.2
013500***************************************************************   SQ1344.2
013600*                                                             *   SQ1344.2
013700*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1344.2
013800*                                                             *   SQ1344.2
013900***************************************************************   SQ1344.2
014000*                                                                 SQ1344.2
014100 01  STATUS-GROUP.                                                SQ1344.2
014200     04  SQ-FS4-STATUS.                                           SQ1344.2
014300         07  SQ-FS4-KEY-1   PIC X.                                SQ1344.2
014400         07  SQ-FS4-KEY-2   PIC X.                                SQ1344.2
014500*                                                                 SQ1344.2
014600 01  DELETE-SW.                                                   SQ1344.2
014700     03  DELETE-SW-1 PIC X.                                       SQ1344.2
014800     03  DELETE-SW-1-GROUP.                                       SQ1344.2
014900         05  DELETE-SW-2 PIC X.                                   SQ1344.2
015000*                                                                 SQ1344.2
015100 01  DECL-EXEC-I-O PIC X(12).                                     SQ1344.2
015200*                                                                 SQ1344.2
015300 01  DECL-EXEC-SW PIC X.                                          SQ1344.2
015400*                                                                 SQ1344.2
015500***************************************************************   SQ1344.2
015600*                                                             *   SQ1344.2
015700*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1344.2
015800*                                                             *   SQ1344.2
015900***************************************************************   SQ1344.2
016000*                                                                 SQ1344.2
016100 01  REC-SKEL-SUB   PIC 99.                                       SQ1344.2
016200*                                                                 SQ1344.2
016300 01  FILE-RECORD-INFORMATION-REC.                                 SQ1344.2
016400     03 FILE-RECORD-INFO-SKELETON.                                SQ1344.2
016500        05 FILLER                 PICTURE X(48)       VALUE       SQ1344.2
016600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1344.2
016700        05 FILLER                 PICTURE X(46)       VALUE       SQ1344.2
016800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1344.2
016900        05 FILLER                 PICTURE X(26)       VALUE       SQ1344.2
017000             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1344.2
017100        05 FILLER                 PICTURE X(37)       VALUE       SQ1344.2
017200             ",RECKEY=                             ".             SQ1344.2
017300        05 FILLER                 PICTURE X(38)       VALUE       SQ1344.2
017400             ",ALTKEY1=                             ".            SQ1344.2
017500        05 FILLER                 PICTURE X(38)       VALUE       SQ1344.2
017600             ",ALTKEY2=                             ".            SQ1344.2
017700        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1344.2
017800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1344.2
017900        05 FILE-RECORD-INFO-P1-120.                               SQ1344.2
018000           07 FILLER              PIC X(5).                       SQ1344.2
018100           07 XFILE-NAME          PIC X(6).                       SQ1344.2
018200           07 FILLER              PIC X(8).                       SQ1344.2
018300           07 XRECORD-NAME        PIC X(6).                       SQ1344.2
018400           07 FILLER              PIC X(1).                       SQ1344.2
018500           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1344.2
018600           07 FILLER              PIC X(7).                       SQ1344.2
018700           07 XRECORD-NUMBER      PIC 9(6).                       SQ1344.2
018800           07 FILLER              PIC X(6).                       SQ1344.2
018900           07 UPDATE-NUMBER       PIC 9(2).                       SQ1344.2
019000           07 FILLER              PIC X(5).                       SQ1344.2
019100           07 ODO-NUMBER          PIC 9(4).                       SQ1344.2
019200           07 FILLER              PIC X(5).                       SQ1344.2
019300           07 XPROGRAM-NAME       PIC X(5).                       SQ1344.2
019400           07 FILLER              PIC X(7).                       SQ1344.2
019500           07 XRECORD-LENGTH      PIC 9(6).                       SQ1344.2
019600           07 FILLER              PIC X(7).                       SQ1344.2
019700           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1344.2
019800           07 FILLER              PIC X(1).                       SQ1344.2
019900           07 XBLOCK-SIZE         PIC 9(4).                       SQ1344.2
020000           07 FILLER              PIC X(6).                       SQ1344.2
020100           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1344.2
020200           07 FILLER              PIC X(5).                       SQ1344.2
020300           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1344.2
020400           07 FILLER              PIC X(6).                       SQ1344.2
020500           07 XLABEL-TYPE         PIC X(1).                       SQ1344.2
020600        05 FILE-RECORD-INFO-P121-240.                             SQ1344.2
020700           07 FILLER              PIC X(8).                       SQ1344.2
020800           07 XRECORD-KEY         PIC X(29).                      SQ1344.2
020900           07 FILLER              PIC X(9).                       SQ1344.2
021000           07 ALTERNATE-KEY1      PIC X(29).                      SQ1344.2
021100           07 FILLER              PIC X(9).                       SQ1344.2
021200           07 ALTERNATE-KEY2      PIC X(29).                      SQ1344.2
021300           07 FILLER              PIC X(7).                       SQ1344.2
021400*                                                                 SQ1344.2
021500 01  TEST-RESULTS.                                                SQ1344.2
021600     02 FILLER              PIC X      VALUE SPACE.               SQ1344.2
021700     02  PAR-NAME.                                                SQ1344.2
021800       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1344.2
021900       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1344.2
022000       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1344.2
022100     02 FILLER              PIC X      VALUE SPACE.               SQ1344.2
022200     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1344.2
022300     02 FILLER              PIC X      VALUE SPACE.               SQ1344.2
022400     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1344.2
022500     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1344.2
022600     02 RE-MARK             PIC X(61).                            SQ1344.2
022700 01  TEST-COMPUTED.                                               SQ1344.2
022800   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1344.2
022900   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1344.2
023000   02 COMPUTED-X.                                                 SQ1344.2
023100     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1344.2
023200     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1344.2
023300     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1344.2
023400     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1344.2
023500     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1344.2
023600     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1344.2
023700        04 COMPUTED-18V0                   PIC -9(18).            SQ1344.2
023800        04 FILLER                          PIC X.                 SQ1344.2
023900     03 FILLER PIC X(50) VALUE SPACE.                             SQ1344.2
024000 01  TEST-CORRECT.                                                SQ1344.2
024100     02 FILLER PIC X(30) VALUE SPACE.                             SQ1344.2
024200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1344.2
024300     02 CORRECT-X.                                                SQ1344.2
024400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1344.2
024500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1344.2
024600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1344.2
024700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1344.2
024800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1344.2
024900     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1344.2
025000         04 CORRECT-18V0                     PIC -9(18).          SQ1344.2
025100         04 FILLER                           PIC X.               SQ1344.2
025200     03 FILLER PIC X(2) VALUE SPACE.                              SQ1344.2
025300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1344.2
025400*                                                                 SQ1344.2
025500 01  CCVS-C-1.                                                    SQ1344.2
025600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1344.2
025700     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1344.2
025800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1344.2
025900     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1344.2
026000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1344.2
026100     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1344.2
026200     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1344.2
026300     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1344.2
026400 01  CCVS-C-2.                                                    SQ1344.2
026500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1344.2
026600     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1344.2
026700     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1344.2
026800     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1344.2
026900     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1344.2
027000*                                                                 SQ1344.2
027100 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1344.2
027200 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1344.2
027300 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1344.2
027400 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1344.2
027500 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1344.2
027600 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1344.2
027700 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1344.2
027800 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1344.2
027900 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1344.2
028000 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1344.2
028100 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1344.2
028200 01  CCVS-H-1.                                                    SQ1344.2
028300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1344.2
028400     02  FILLER          PIC X(42)    VALUE                       SQ1344.2
028500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1344.2
028600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1344.2
028700 01  CCVS-H-2A.                                                   SQ1344.2
028800   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1344.2
028900   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1344.2
029000   02  FILLER            PIC XXXX   VALUE                         SQ1344.2
029100     "4.2 ".                                                      SQ1344.2
029200   02  FILLER            PIC X(28)  VALUE                         SQ1344.2
029300            " COPY - NOT FOR DISTRIBUTION".                       SQ1344.2
029400   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1344.2
029500*                                                                 SQ1344.2
029600 01  CCVS-H-2B.                                                   SQ1344.2
029700   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1344.2
029800   02  TEST-ID           PIC X(9).                                SQ1344.2
029900   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1344.2
030000   02  FILLER            PIC X(12)  VALUE                         SQ1344.2
030100     " HIGH       ".                                              SQ1344.2
030200   02  FILLER            PIC X(22)  VALUE                         SQ1344.2
030300            " LEVEL VALIDATION FOR ".                             SQ1344.2
030400   02  FILLER            PIC X(58)  VALUE                         SQ1344.2
030500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1344.2
030600 01  CCVS-H-3.                                                    SQ1344.2
030700     02  FILLER          PIC X(34)  VALUE                         SQ1344.2
030800            " FOR OFFICIAL USE ONLY    ".                         SQ1344.2
030900     02  FILLER          PIC X(58)  VALUE                         SQ1344.2
031000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1344.2
031100     02  FILLER          PIC X(28)  VALUE                         SQ1344.2
031200            "  COPYRIGHT   1985,1986 ".                           SQ1344.2
031300 01  CCVS-E-1.                                                    SQ1344.2
031400     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1344.2
031500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1344.2
031600     02 ID-AGAIN         PIC X(9).                                SQ1344.2
031700     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1344.2
031800 01  CCVS-E-2.                                                    SQ1344.2
031900     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1344.2
032000     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1344.2
032100     02  CCVS-E-2-2.                                              SQ1344.2
032200         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1344.2
032300         03 FILLER         PIC X      VALUE SPACE.                SQ1344.2
032400         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1344.2
032500            "ERRORS ENCOUNTERED".                                 SQ1344.2
032600 01  CCVS-E-3.                                                    SQ1344.2
032700     02  FILLER          PIC X(22)  VALUE                         SQ1344.2
032800            " FOR OFFICIAL USE ONLY".                             SQ1344.2
032900     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1344.2
033000     02  FILLER          PIC X(58)  VALUE                         SQ1344.2
033100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1344.2
033200     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1344.2
033300     02  FILLER          PIC X(20)  VALUE                         SQ1344.2
033400             " COPYRIGHT 1985,1986".                              SQ1344.2
033500 01  CCVS-E-4.                                                    SQ1344.2
033600     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1344.2
033700     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1344.2
033800     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1344.2
033900     02 FILLER           PIC X(40)  VALUE                         SQ1344.2
034000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1344.2
034100 01  XXINFO.                                                      SQ1344.2
034200     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1344.2
034300     02 INFO-TEXT.                                                SQ1344.2
034400       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1344.2
034500       04 XXCOMPUTED         PIC X(20).                           SQ1344.2
034600       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1344.2
034700       04 XXCORRECT          PIC X(20).                           SQ1344.2
034800     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1344.2
034900 01  HYPHEN-LINE.                                                 SQ1344.2
035000     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1344.2
035100     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1344.2
035200-    "*****************************************".                 SQ1344.2
035300     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1344.2
035400-    "******************************".                            SQ1344.2
035500 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1344.2
035600     "SQ134A".                                                    SQ1344.2
035700*                                                                 SQ1344.2
035800*                                                                 SQ1344.2
035900 PROCEDURE DIVISION.                                              SQ1344.2
036000 DECLARATIVES.                                                    SQ1344.2
036100*                                                                 SQ1344.2
036200 SECT-SQ134A-0001 SECTION.                                        SQ1344.2
036300     USE AFTER EXCEPTION PROCEDURE I-O.                           SQ1344.2
036400 I-O-ERROR-PROCESS.                                               SQ1344.2
036500     MOVE   "EXECUTED" TO DECL-EXEC-I-O.                          SQ1344.2
036600     IF DECL-EXEC-SW NOT = SPACE                                  SQ1344.2
036700         GO TO   END-DECLS.                                       SQ1344.2
036800*                                                                 SQ1344.2
036900     MOVE    1 TO REC-CT.                                         SQ1344.2
037000     MOVE   "REWRITE SHORTER RECORD"  TO FEATURE.                 SQ1344.2
037100     MOVE   "DCL-REWRITE-01" TO PAR-NAME.                         SQ1344.2
037200     GO TO   DCL-REWRITE-01-01.                                   SQ1344.2
037300 DECL-DELETE-01-01.                                               SQ1344.2
037400     PERFORM DECL-DE-LETE.                                        SQ1344.2
037500     GO TO   DECL-TEST-01-01-END.                                 SQ1344.2
037600 DCL-REWRITE-01-01.                                               SQ1344.2
037700     IF SQ-FS4-STATUS = "44"                                      SQ1344.2
037800         PERFORM DECL-PASS                                        SQ1344.2
037900     ELSE                                                         SQ1344.2
038000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
038100         MOVE   "44" TO CORRECT-A                                 SQ1344.2
038200         MOVE   "UNEXPECTED I-O STATUS ON FAILED REWRITE"         SQ1344.2
038300                   TO RE-MARK                                     SQ1344.2
038400         MOVE   "VII-4, VII-48,4.5.4(2)" TO ANSI-REFERENCE        SQ1344.2
038500         PERFORM DECL-FAIL.                                       SQ1344.2
038600 DECL-TEST-01-01-END.                                             SQ1344.2
038700*                                                                 SQ1344.2
038800     ADD     1 TO REC-CT.                                         SQ1344.2
038900     GO TO   DCL-REWRITE-01-02.                                   SQ1344.2
039000 DECL-DELETE-01-02.                                               SQ1344.2
039100     PERFORM DECL-DE-LETE.                                        SQ1344.2
039200     GO TO   DECL-TEST-01-02-END.                                 SQ1344.2
039300 DCL-REWRITE-01-02.                                               SQ1344.2
039400     IF SQ-FS4R1-F-G-120 = FILE-RECORD-INFO-P1-120 (1)            SQ1344.2
039500         PERFORM DECL-PASS                                        SQ1344.2
039600     ELSE                                                         SQ1344.2
039700         MOVE   "FIRST 120 CHARACTERS OF RECORD AREA CHANGED"     SQ1344.2
039800                   TO RE-MARK                                     SQ1344.2
039900         MOVE   "VII-4, VII-49,4.5.4(9)" TO ANSI-REFERENCE        SQ1344.2
040000         PERFORM DECL-FAIL.                                       SQ1344.2
040100 DECL-TEST-01-02-END.                                             SQ1344.2
040200*                                                                 SQ1344.2
040300     ADD     1 TO REC-CT.                                         SQ1344.2
040400     GO TO   DCL-REWRITE-01-03.                                   SQ1344.2
040500 DECL-DELETE-01-03.                                               SQ1344.2
040600     PERFORM DECL-DE-LETE.                                        SQ1344.2
040700     GO TO   DECL-TEST-01-03-END.                                 SQ1344.2
040800 DCL-REWRITE-01-03.                                               SQ1344.2
040900     IF EXT-18 = "ABCDEFGHIJKLMNOPQR"                             SQ1344.2
041000         PERFORM DECL-PASS                                        SQ1344.2
041100     ELSE                                                         SQ1344.2
041200         MOVE    EXT-18 TO COMPUTED-A                             SQ1344.2
041300         MOVE   "ABCDEFGHIJKLMNOPQR" TO CORRECT-A                 SQ1344.2
041400         MOVE   "LAST 18 CHARACTERS OF RECORD CHANGED"            SQ1344.2
041500                   TO RE-MARK                                     SQ1344.2
041600         MOVE   "VII-4, VII-49,4.5.4(9)" TO ANSI-REFERENCE        SQ1344.2
041700         PERFORM DECL-FAIL.                                       SQ1344.2
041800 DECL-TEST-01-03-END.                                             SQ1344.2
041900*                                                                 SQ1344.2
042000     PERFORM DECL-WRITE-LINE.                                     SQ1344.2
042100     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ1344.2
042200               TO DUMMY-RECORD.                                   SQ1344.2
042300     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1344.2
042400     GO TO   END-DECLS.                                           SQ1344.2
042500*                                                                 SQ1344.2
042600*                                                                 SQ1344.2
042700 DECL-PASS.                                                       SQ1344.2
042800     MOVE   "PASS " TO P-OR-F.                                    SQ1344.2
042900     ADD     1 TO PASS-COUNTER.                                   SQ1344.2
043000     PERFORM DECL-PRINT-DETAIL.                                   SQ1344.2
043100*                                                                 SQ1344.2
043200 DECL-FAIL.                                                       SQ1344.2
043300     MOVE   "FAIL*" TO P-OR-F.                                    SQ1344.2
043400     ADD     1 TO ERROR-COUNTER.                                  SQ1344.2
043500     PERFORM DECL-PRINT-DETAIL.                                   SQ1344.2
043600*                                                                 SQ1344.2
043700 DECL-DE-LETE.                                                    SQ1344.2
043800     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1344.2
043900     MOVE   "*****" TO P-OR-F.                                    SQ1344.2
044000     ADD     1 TO DELETE-COUNTER.                                 SQ1344.2
044100     PERFORM DECL-PRINT-DETAIL.                                   SQ1344.2
044200*                                                                 SQ1344.2
044300 DECL-PRINT-DETAIL.                                               SQ1344.2
044400     IF REC-CT NOT EQUAL TO ZERO                                  SQ1344.2
044500             MOVE "." TO PARDOT-X                                 SQ1344.2
044600             MOVE REC-CT TO DOTVALUE.                             SQ1344.2
044700     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1344.2
044800     PERFORM DECL-WRITE-LINE.                                     SQ1344.2
044900     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1344.2
045000         PERFORM DECL-WRITE-LINE                                  SQ1344.2
045100         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1344.2
045200     ELSE                                                         SQ1344.2
045300         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1344.2
045400     MOVE    SPACE TO P-OR-F.                                     SQ1344.2
045500     MOVE    SPACE TO COMPUTED-X.                                 SQ1344.2
045600     MOVE    SPACE TO CORRECT-X.                                  SQ1344.2
045700     IF REC-CT EQUAL TO ZERO                                      SQ1344.2
045800         MOVE    SPACE TO PAR-NAME.                               SQ1344.2
045900     MOVE    SPACE TO RE-MARK.                                    SQ1344.2
046000*                                                                 SQ1344.2
046100 DECL-WRITE-LINE.                                                 SQ1344.2
046200     ADD     1 TO RECORD-COUNT.                                   SQ1344.2
046300*    IF RECORD-COUNT GREATER 50                                   SQ1344.2
046400*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1344.2
046500*        MOVE    SPACE TO DUMMY-RECORD                            SQ1344.2
046600*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1344.2
046700*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1344.2
046800*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1344.2
046900*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1344.2
047000*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1344.2
047100*        PERFORM DECL-WRT-LN                                      SQ1344.2
047200*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1344.2
047300*        MOVE    ZERO TO RECORD-COUNT.                            SQ1344.2
047400     PERFORM DECL-WRT-LN.                                         SQ1344.2
047500*                                                                 SQ1344.2
047600 DECL-WRT-LN.                                                     SQ1344.2
047700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1344.2
047800     MOVE    SPACE TO DUMMY-RECORD.                               SQ1344.2
047900*                                                                 SQ1344.2
048000 DECL-FAIL-ROUTINE.                                               SQ1344.2
048100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1344.2
048200     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1344.2
048300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1344.2
048400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1344.2
048500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1344.2
048600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1344.2
048700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1344.2
048800     GO TO   DECL-FAIL-EX.                                        SQ1344.2
048900 DECL-FAIL-WRITE.                                                 SQ1344.2
049000     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1344.2
049100     PERFORM DECL-WRITE-LINE                                      SQ1344.2
049200     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1344.2
049300     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1344.2
049400     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1344.2
049500     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1344.2
049600 DECL-FAIL-EX.                                                    SQ1344.2
049700     EXIT.                                                        SQ1344.2
049800*                                                                 SQ1344.2
049900 DECL-BAIL.                                                       SQ1344.2
050000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1344.2
050100     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1344.2
050200 DECL-BAIL-WRITE.                                                 SQ1344.2
050300     MOVE    CORRECT-A TO XXCORRECT.                              SQ1344.2
050400     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1344.2
050500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1344.2
050600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1344.2
050700 DECL-BAIL-EX.                                                    SQ1344.2
050800     EXIT.                                                        SQ1344.2
050900*                                                                 SQ1344.2
051000 END-DECLS.                                                       SQ1344.2
051100 END DECLARATIVES.                                                SQ1344.2
051200*                                                                 SQ1344.2
051300*                                                                 SQ1344.2
051400 CCVS1 SECTION.                                                   SQ1344.2
051500 OPEN-FILES.                                                      SQ1344.2
051600*    OPEN    I-O RAW-DATA.                                        SQ1344.2
051700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1344.2
051800*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1344.2
051900*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1344.2
052000*    ADD     1           TO C-NO-OF-TESTS.                        SQ1344.2
052100*    ACCEPT  C-DATE      FROM DATE.                               SQ1344.2
052200*    ACCEPT  C-TIME      FROM TIME.                               SQ1344.2
052300*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1344.2
052400*END-E-1.                                                         SQ1344.2
052500*    CLOSE   RAW-DATA.                                            SQ1344.2
052600     OPEN    OUTPUT PRINT-FILE.                                   SQ1344.2
052700     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1344.2
052800     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1344.2
052900     MOVE    SPACE TO TEST-RESULTS.                               SQ1344.2
053000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1344.2
053100     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1344.2
053200     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1344.2
053300     GO TO CCVS1-EXIT.                                            SQ1344.2
053400*                                                                 SQ1344.2
053500 CCVS-INIT-FILE.                                                  SQ1344.2
053600     ADD     1 TO REC-SKL-SUB.                                    SQ1344.2
053700     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1344.2
053800                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1344.2
053900*                                                                 SQ1344.2
054000 CLOSE-FILES.                                                     SQ1344.2
054100     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1344.2
054200     CLOSE   PRINT-FILE.                                          SQ1344.2
054300*    OPEN    I-O RAW-DATA.                                        SQ1344.2
054400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1344.2
054500*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1344.2
054600*    MOVE   "OK.     " TO C-ABORT.                                SQ1344.2
054700*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1344.2
054800*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1344.2
054900*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1344.2
055000*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1344.2
055100*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1344.2
055200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1344.2
055300*END-E-2.                                                         SQ1344.2
055400*    CLOSE   RAW-DATA.                                            SQ1344.2
055500 TERMINATE-CCVS.                                                  SQ1344.2
055600*    EXIT    PROGRAM.                                             SQ1344.2
055700     STOP    RUN.                                                 SQ1344.2
055800*                                                                 SQ1344.2
055900 INSPT.                                                           SQ1344.2
056000     MOVE   "INSPT" TO P-OR-F.                                    SQ1344.2
056100     ADD     1 TO INSPECT-COUNTER.                                SQ1344.2
056200     PERFORM PRINT-DETAIL.                                        SQ1344.2
056300*                                                                 SQ1344.2
056400 PASS.                                                            SQ1344.2
056500     MOVE   "PASS " TO P-OR-F.                                    SQ1344.2
056600     ADD     1 TO PASS-COUNTER.                                   SQ1344.2
056700     PERFORM PRINT-DETAIL.                                        SQ1344.2
056800*                                                                 SQ1344.2
056900 FAIL.                                                            SQ1344.2
057000     MOVE   "FAIL*" TO P-OR-F.                                    SQ1344.2
057100     ADD     1 TO ERROR-COUNTER.                                  SQ1344.2
057200     PERFORM PRINT-DETAIL.                                        SQ1344.2
057300*                                                                 SQ1344.2
057400 DE-LETE.                                                         SQ1344.2
057500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1344.2
057600     MOVE   "*****" TO P-OR-F.                                    SQ1344.2
057700     ADD     1 TO DELETE-COUNTER.                                 SQ1344.2
057800     PERFORM PRINT-DETAIL.                                        SQ1344.2
057900*                                                                 SQ1344.2
058000 PRINT-DETAIL.                                                    SQ1344.2
058100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1344.2
058200         MOVE   "." TO PARDOT-X                                   SQ1344.2
058300         MOVE    REC-CT TO DOTVALUE.                              SQ1344.2
058400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1344.2
058500     PERFORM WRITE-LINE.                                          SQ1344.2
058600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1344.2
058700         PERFORM WRITE-LINE                                       SQ1344.2
058800         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1344.2
058900     ELSE                                                         SQ1344.2
059000         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1344.2
059100     MOVE    SPACE TO P-OR-F.                                     SQ1344.2
059200     MOVE    SPACE TO COMPUTED-X.                                 SQ1344.2
059300     MOVE    SPACE TO CORRECT-X.                                  SQ1344.2
059400     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1344.2
059500     MOVE    SPACE TO RE-MARK.                                    SQ1344.2
059600*                                                                 SQ1344.2
059700 HEAD-ROUTINE.                                                    SQ1344.2
059800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1344.2
059900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1344.2
060000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1344.2
060100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1344.2
060200 COLUMN-NAMES-ROUTINE.                                            SQ1344.2
060300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1344.2
060400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1344.2
060500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1344.2
060600 END-ROUTINE.                                                     SQ1344.2
060700     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1344.2
060800     PERFORM WRITE-LINE 5 TIMES.                                  SQ1344.2
060900 END-RTN-EXIT.                                                    SQ1344.2
061000     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1344.2
061100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1344.2
061200*                                                                 SQ1344.2
061300 END-ROUTINE-1.                                                   SQ1344.2
061400     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1344.2
061500     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1344.2
061600     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1344.2
061700     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1344.2
061800     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1344.2
061900     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1344.2
062000     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1344.2
062100     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1344.2
062200     PERFORM WRITE-LINE.                                          SQ1344.2
062300     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1344.2
062400     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1344.2
062500         MOVE   "NO " TO ERROR-TOTAL                              SQ1344.2
062600     ELSE                                                         SQ1344.2
062700         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1344.2
062800     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1344.2
062900     PERFORM WRITE-LINE.                                          SQ1344.2
063000 END-ROUTINE-13.                                                  SQ1344.2
063100     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1344.2
063200         MOVE   "NO " TO ERROR-TOTAL                              SQ1344.2
063300     ELSE                                                         SQ1344.2
063400         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1344.2
063500     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1344.2
063600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1344.2
063700     PERFORM WRITE-LINE.                                          SQ1344.2
063800     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1344.2
063900         MOVE   "NO " TO ERROR-TOTAL                              SQ1344.2
064000     ELSE                                                         SQ1344.2
064100         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1344.2
064200     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1344.2
064300     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1344.2
064400     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1344.2
064500*                                                                 SQ1344.2
064600 WRITE-LINE.                                                      SQ1344.2
064700     ADD     1 TO RECORD-COUNT.                                   SQ1344.2
064800*    IF RECORD-COUNT GREATER 50                                   SQ1344.2
064900*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1344.2
065000*        MOVE  SPACE TO DUMMY-RECORD                              SQ1344.2
065100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1344.2
065200*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1344.2
065300*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1344.2
065400*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1344.2
065500*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1344.2
065600*        MOVE  ZERO TO RECORD-COUNT.                              SQ1344.2
065700     PERFORM WRT-LN.                                              SQ1344.2
065800*                                                                 SQ1344.2
065900 WRT-LN.                                                          SQ1344.2
066000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1344.2
066100     MOVE    SPACE TO DUMMY-RECORD.                               SQ1344.2
066200 BLANK-LINE-PRINT.                                                SQ1344.2
066300     PERFORM WRT-LN.                                              SQ1344.2
066400 FAIL-ROUTINE.                                                    SQ1344.2
066500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1344.2
066600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1344.2
066700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1344.2
066800     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1344.2
066900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1344.2
067000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1344.2
067100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1344.2
067200     GO TO   FAIL-ROUTINE-EX.                                     SQ1344.2
067300 FAIL-ROUTINE-WRITE.                                              SQ1344.2
067400     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1344.2
067500     PERFORM WRITE-LINE                                           SQ1344.2
067600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1344.2
067700     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1344.2
067800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1344.2
067900     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1344.2
068000 FAIL-ROUTINE-EX.                                                 SQ1344.2
068100     EXIT.                                                        SQ1344.2
068200 BAIL-OUT.                                                        SQ1344.2
068300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1344.2
068400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1344.2
068500 BAIL-OUT-WRITE.                                                  SQ1344.2
068600     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1344.2
068700     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1344.2
068800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1344.2
068900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1344.2
069000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1344.2
069100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1344.2
069200 BAIL-OUT-EX.                                                     SQ1344.2
069300     EXIT.                                                        SQ1344.2
069400 CCVS1-EXIT.                                                      SQ1344.2
069500     EXIT.                                                        SQ1344.2
069600*                                                                 SQ1344.2
069700****************************************************************  SQ1344.2
069800*                                                              *  SQ1344.2
069900*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1344.2
070000*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1344.2
070100*                                                              *  SQ1344.2
070200****************************************************************  SQ1344.2
070300*                                                                 SQ1344.2
070400 SECT-SQ134A-0002 SECTION.                                        SQ1344.2
070500 STA-INIT.                                                        SQ1344.2
070600     MOVE    SPACE TO DELETE-SW.                                  SQ1344.2
070700*                                                                 SQ1344.2
070800     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1344.2
070900     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1344.2
071000     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1344.2
071100     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1344.2
071200     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ1344.2
071300     MOVE    1   TO XBLOCK-SIZE (1).                              SQ1344.2
071400     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ1344.2
071500     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1344.2
071600     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1344.2
071700*                                                                 SQ1344.2
071800*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1344.2
071900*                                                                 SQ1344.2
072000 SEQ-INIT-01.                                                     SQ1344.2
072100     MOVE    0 TO REC-CT.                                         SQ1344.2
072200     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1344.2
072300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1344.2
072400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1344.2
072500     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1344.2
072600     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1344.2
072700     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1344.2
072800     GO TO   SEQ-TEST-OP-01.                                      SQ1344.2
072900 SEQ-DELETE-01.                                                   SQ1344.2
073000     MOVE   "*" TO DELETE-SW-1.                                   SQ1344.2
073100     GO TO   SEQ-DELETE-01-01.                                    SQ1344.2
073200 SEQ-TEST-OP-01.                                                  SQ1344.2
073300     OPEN    OUTPUT SQ-FS4.                                       SQ1344.2
073400*                                                                 SQ1344.2
073500*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ1344.2
073600*                                                                 SQ1344.2
073700     ADD    1 TO REC-CT.                                          SQ1344.2
073800     IF DELETE-SW NOT = SPACE                                     SQ1344.2
073900         GO TO   SEQ-DELETE-01-01.                                SQ1344.2
074000     GO TO   SEQ-TEST-OP-01-01.                                   SQ1344.2
074100 SEQ-DELETE-01-01.                                                SQ1344.2
074200     PERFORM DE-LETE.                                             SQ1344.2
074300     GO TO   SEQ-TEST-01-01-END.                                  SQ1344.2
074400 SEQ-TEST-OP-01-01.                                               SQ1344.2
074500     IF SQ-FS4-STATUS = "00"                                      SQ1344.2
074600         PERFORM PASS                                             SQ1344.2
074700     ELSE                                                         SQ1344.2
074800         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
074900         MOVE   "00" TO CORRECT-A                                 SQ1344.2
075000         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ1344.2
075100                   TO RE-MARK                                     SQ1344.2
075200         MOVE   "VII-3, VII-43" TO ANSI-REFERENCE                 SQ1344.2
075300         PERFORM FAIL.                                            SQ1344.2
075400 SEQ-TEST-01-01-END.                                              SQ1344.2
075500*                                                                 SQ1344.2
075600*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1344.2
075700*                                                                 SQ1344.2
075800     ADD     1 TO REC-CT.                                         SQ1344.2
075900     IF DELETE-SW NOT = SPACE                                     SQ1344.2
076000         GO TO   SEQ-DELETE-01-02.                                SQ1344.2
076100     GO TO   SEQ-TEST-OP-01-02.                                   SQ1344.2
076200 SEQ-DELETE-01-02.                                                SQ1344.2
076300     PERFORM DE-LETE.                                             SQ1344.2
076400     GO TO   SEQ-TEST-01-02-END.                                  SQ1344.2
076500 SEQ-TEST-OP-01-02.                                               SQ1344.2
076600     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1344.2
076700         PERFORM PASS                                             SQ1344.2
076800     ELSE                                                         SQ1344.2
076900         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1344.2
077000         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1344.2
077100         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1344.2
077200                   TO RE-MARK                                     SQ1344.2
077300         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1344.2
077400         PERFORM FAIL.                                            SQ1344.2
077500 SEQ-TEST-01-02-END.                                              SQ1344.2
077600*                                                                 SQ1344.2
077700*                                                                 SQ1344.2
077800*    A NEW FILE IS OPEN.  WE NOW WRITE ONE RECORD OF 138 CHARS.   SQ1344.2
077900*                                                                 SQ1344.2
078000 SEQ-INIT-02.                                                     SQ1344.2
078100     MOVE    0 TO REC-CT.                                         SQ1344.2
078200     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1344.2
078300     ADD     1 TO XRECORD-NUMBER (1).                             SQ1344.2
078400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1344.2
078500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1344.2
078600     MOVE   "WRITE A RECORD"  TO FEATURE.                         SQ1344.2
078700     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1344.2
078800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1344.2
078900         GO TO SEQ-DELETE-02.                                     SQ1344.2
079000     GO TO   SEQ-TEST-WR-02.                                      SQ1344.2
079100 SEQ-DELETE-02.                                                   SQ1344.2
079200     MOVE   "*" TO DELETE-SW-2.                                   SQ1344.2
079300     GO TO   SEQ-DELETE-02-01.                                    SQ1344.2
079400 SEQ-TEST-WR-02.                                                  SQ1344.2
079500     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ1344.2
079600     MOVE   "987654321123456789" TO EXT-18.                       SQ1344.2
079700     WRITE   SQ-FS4R2-F-G-138.                                    SQ1344.2
079800*                                                                 SQ1344.2
079900*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ1344.2
080000*                                                                 SQ1344.2
080100     ADD    1 TO REC-CT.                                          SQ1344.2
080200     IF DELETE-SW NOT = SPACE                                     SQ1344.2
080300         GO TO   SEQ-DELETE-02-01.                                SQ1344.2
080400     GO TO   SEQ-TEST-WR-02-01.                                   SQ1344.2
080500 SEQ-DELETE-02-01.                                                SQ1344.2
080600     PERFORM DE-LETE.                                             SQ1344.2
080700     GO TO   SEQ-TEST-02-01-END.                                  SQ1344.2
080800 SEQ-TEST-WR-02-01.                                               SQ1344.2
080900     IF SQ-FS4-STATUS = "00"                                      SQ1344.2
081000         PERFORM PASS                                             SQ1344.2
081100     ELSE                                                         SQ1344.2
081200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
081300         MOVE   "00" TO CORRECT-A                                 SQ1344.2
081400         MOVE   "UNEXPECTED ERROR CODE FROM WRITE"                SQ1344.2
081500                   TO RE-MARK                                     SQ1344.2
081600         MOVE   "VII-3, VII-53" TO ANSI-REFERENCE                 SQ1344.2
081700         PERFORM FAIL.                                            SQ1344.2
081800 SEQ-TEST-02-01-END.                                              SQ1344.2
081900*                                                                 SQ1344.2
082000*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1344.2
082100*                                                                 SQ1344.2
082200     ADD     1 TO REC-CT.                                         SQ1344.2
082300     IF DELETE-SW NOT = SPACE                                     SQ1344.2
082400         GO TO   SEQ-DELETE-02-02.                                SQ1344.2
082500     GO TO   SEQ-TEST-WR-02-02.                                   SQ1344.2
082600 SEQ-DELETE-02-02.                                                SQ1344.2
082700     PERFORM DE-LETE.                                             SQ1344.2
082800     GO TO   SEQ-TEST-02-02-END.                                  SQ1344.2
082900 SEQ-TEST-WR-02-02.                                               SQ1344.2
083000     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1344.2
083100         PERFORM PASS                                             SQ1344.2
083200     ELSE                                                         SQ1344.2
083300         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1344.2
083400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1344.2
083500         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1344.2
083600                   TO RE-MARK                                     SQ1344.2
083700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1344.2
083800         PERFORM FAIL.                                            SQ1344.2
083900 SEQ-TEST-02-02-END.                                              SQ1344.2
084000*                                                                 SQ1344.2
084100*                                                                 SQ1344.2
084200*    NOW CLOSE THE FILE.                                          SQ1344.2
084300*                                                                 SQ1344.2
084400 SEQ-INIT-03.                                                     SQ1344.2
084500     MOVE    0 TO REC-CT.                                         SQ1344.2
084600     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1344.2
084700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1344.2
084800     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1344.2
084900     MOVE   "CLOSE FILE" TO FEATURE.                              SQ1344.2
085000     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ1344.2
085100     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1344.2
085200         GO TO SEQ-DELETE-03.                                     SQ1344.2
085300     GO TO   SEQ-TEST-CL-03.                                      SQ1344.2
085400 SEQ-DELETE-03.                                                   SQ1344.2
085500     MOVE   "*" TO DELETE-SW-2.                                   SQ1344.2
085600     GO TO   SEQ-DELETE-03-01.                                    SQ1344.2
085700 SEQ-TEST-CL-03.                                                  SQ1344.2
085800     CLOSE   SQ-FS4.                                              SQ1344.2
085900*                                                                 SQ1344.2
086000*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ1344.2
086100*                                                                 SQ1344.2
086200     ADD    1 TO REC-CT.                                          SQ1344.2
086300     IF DELETE-SW NOT = SPACE                                     SQ1344.2
086400         GO TO   SEQ-DELETE-03-01.                                SQ1344.2
086500     GO TO   SEQ-TEST-CL-03-01.                                   SQ1344.2
086600 SEQ-DELETE-03-01.                                                SQ1344.2
086700     PERFORM DE-LETE.                                             SQ1344.2
086800     GO TO   SEQ-TEST-03-01-END.                                  SQ1344.2
086900 SEQ-TEST-CL-03-01.                                               SQ1344.2
087000     IF SQ-FS4-STATUS = "00"                                      SQ1344.2
087100         PERFORM PASS                                             SQ1344.2
087200     ELSE                                                         SQ1344.2
087300         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
087400         MOVE   "00" TO CORRECT-A                                 SQ1344.2
087500         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ1344.2
087600                   TO RE-MARK                                     SQ1344.2
087700         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1344.2
087800         PERFORM FAIL.                                            SQ1344.2
087900 SEQ-TEST-03-01-END.                                              SQ1344.2
088000*                                                                 SQ1344.2
088100*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1344.2
088200*                                                                 SQ1344.2
088300     ADD     1 TO REC-CT.                                         SQ1344.2
088400     IF DELETE-SW NOT = SPACE                                     SQ1344.2
088500         GO TO   SEQ-DELETE-03-02.                                SQ1344.2
088600     GO TO   SEQ-TEST-CL-03-02.                                   SQ1344.2
088700 SEQ-DELETE-03-02.                                                SQ1344.2
088800     PERFORM DE-LETE.                                             SQ1344.2
088900     GO TO   SEQ-TEST-03-02-END.                                  SQ1344.2
089000 SEQ-TEST-CL-03-02.                                               SQ1344.2
089100     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1344.2
089200         PERFORM PASS                                             SQ1344.2
089300     ELSE                                                         SQ1344.2
089400         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1344.2
089500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1344.2
089600         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1344.2
089700                   TO RE-MARK                                     SQ1344.2
089800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1344.2
089900         PERFORM FAIL.                                            SQ1344.2
090000 SEQ-TEST-03-02-END.                                              SQ1344.2
090100     MOVE    SPACE TO DELETE-SW-2.                                SQ1344.2
090200*                                                                 SQ1344.2
090300*                                                                 SQ1344.2
090400*    OPEN THE FILE IN THE I-O MODE                                SQ1344.2
090500*                                                                 SQ1344.2
090600 SEQ-INIT-04.                                                     SQ1344.2
090700     MOVE    0 TO REC-CT.                                         SQ1344.2
090800     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1344.2
090900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1344.2
091000     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1344.2
091100     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1344.2
091200     MOVE   "OPEN FILE FOR I-O" TO FEATURE.                       SQ1344.2
091300     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ1344.2
091400     IF DELETE-SW NOT = SPACE                                     SQ1344.2
091500         GO TO SEQ-DELETE-04-01.                                  SQ1344.2
091600     GO TO   SEQ-TEST-OP-04.                                      SQ1344.2
091700 SEQ-DELETE-04.                                                   SQ1344.2
091800     MOVE   "*" TO DELETE-SW-2.                                   SQ1344.2
091900     GO TO   SEQ-DELETE-04-01.                                    SQ1344.2
092000 SEQ-TEST-OP-04.                                                  SQ1344.2
092100     OPEN    I-O SQ-FS4.                                          SQ1344.2
092200*                                                                 SQ1344.2
092300*    CHECK I-O STATUS RETURNED FROM OPEN I-O                      SQ1344.2
092400*                                                                 SQ1344.2
092500     ADD    1 TO REC-CT.                                          SQ1344.2
092600     IF DELETE-SW NOT = SPACE                                     SQ1344.2
092700         GO TO   SEQ-DELETE-04-01.                                SQ1344.2
092800     GO TO   SEQ-TEST-OP-04-01.                                   SQ1344.2
092900 SEQ-DELETE-04-01.                                                SQ1344.2
093000     PERFORM DE-LETE.                                             SQ1344.2
093100     GO TO   SEQ-TEST-04-01-END.                                  SQ1344.2
093200 SEQ-TEST-OP-04-01.                                               SQ1344.2
093300     IF SQ-FS4-STATUS = "00"                                      SQ1344.2
093400         PERFORM PASS                                             SQ1344.2
093500     ELSE                                                         SQ1344.2
093600         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
093700         MOVE   "00" TO CORRECT-A                                 SQ1344.2
093800         MOVE   "UNEXPECTED I-O STATUS CODE FROM OPEN I-O"        SQ1344.2
093900                   TO RE-MARK                                     SQ1344.2
094000         MOVE   "VII-3, VII-40" TO ANSI-REFERENCE                 SQ1344.2
094100         PERFORM FAIL.                                            SQ1344.2
094200 SEQ-TEST-04-01-END.                                              SQ1344.2
094300*                                                                 SQ1344.2
094400     ADD     1 TO REC-CT.                                         SQ1344.2
094500     IF DELETE-SW NOT = SPACE                                     SQ1344.2
094600         GO TO   SEQ-DELETE-04-02.                                SQ1344.2
094700     GO TO   SEQ-TEST-OP-04-02.                                   SQ1344.2
094800 SEQ-DELETE-04-02.                                                SQ1344.2
094900     PERFORM DE-LETE.                                             SQ1344.2
095000     GO TO   SEQ-TEST-04-02-END.                                  SQ1344.2
095100 SEQ-TEST-OP-04-02.                                               SQ1344.2
095200     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1344.2
095300         PERFORM PASS                                             SQ1344.2
095400     ELSE                                                         SQ1344.2
095500         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1344.2
095600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1344.2
095700         MOVE  "UNEXPECTED EXECUTION OF I-O DECLARATIVE ON OPEN"  SQ1344.2
095800                   TO RE-MARK                                     SQ1344.2
095900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1344.2
096000         PERFORM FAIL.                                            SQ1344.2
096100 SEQ-TEST-04-02-END.                                              SQ1344.2
096200*                                                                 SQ1344.2
096300*                                                                 SQ1344.2
096400*    THE FILE IS OPEN FOR I-O.  WE READ THE ONLY RECORD.          SQ1344.2
096500*                                                                 SQ1344.2
096600 SEQ-INIT-05.                                                     SQ1344.2
096700     MOVE    0 TO REC-CT.                                         SQ1344.2
096800     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1344.2
096900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1344.2
097000     MOVE    SPACE TO SQ-FS4R2-F-G-138.                           SQ1344.2
097100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1344.2
097200     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1344.2
097300     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ1344.2
097400     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ1344.2
097500     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1344.2
097600         GO TO SEQ-DELETE-05.                                     SQ1344.2
097700     GO TO   SEQ-TEST-RD-05.                                      SQ1344.2
097800 SEQ-DELETE-05.                                                   SQ1344.2
097900     MOVE   "*" TO DELETE-SW-2.                                   SQ1344.2
098000     GO TO   SEQ-DELETE-05-01.                                    SQ1344.2
098100 SEQ-TEST-RD-05.                                                  SQ1344.2
098200     READ    SQ-FS4.                                              SQ1344.2
098300*                                                                 SQ1344.2
098400*    CHECK I-O STATUS RETURNED FROM READ                          SQ1344.2
098500*                                                                 SQ1344.2
098600     ADD    1 TO REC-CT.                                          SQ1344.2
098700     IF DELETE-SW NOT = SPACE                                     SQ1344.2
098800         GO TO   SEQ-DELETE-05-01.                                SQ1344.2
098900     GO TO   SEQ-TEST-RD-05-01.                                   SQ1344.2
099000 SEQ-DELETE-05-01.                                                SQ1344.2
099100     PERFORM DE-LETE.                                             SQ1344.2
099200     GO TO   SEQ-TEST-05-01-END.                                  SQ1344.2
099300 SEQ-TEST-RD-05-01.                                               SQ1344.2
099400     IF SQ-FS4-STATUS = "00"                                      SQ1344.2
099500         PERFORM PASS                                             SQ1344.2
099600     ELSE                                                         SQ1344.2
099700         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
099800         MOVE   "00" TO CORRECT-A                                 SQ1344.2
099900         MOVE   "UNEXPECTED STATUS CODE FROM READ"                SQ1344.2
100000                   TO RE-MARK                                     SQ1344.2
100100         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ1344.2
100200         PERFORM FAIL.                                            SQ1344.2
100300 SEQ-TEST-05-01-END.                                              SQ1344.2
100400*                                                                 SQ1344.2
100500*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1344.2
100600*                                                                 SQ1344.2
100700     ADD     1 TO REC-CT.                                         SQ1344.2
100800     IF DELETE-SW NOT = SPACE                                     SQ1344.2
100900         GO TO   SEQ-DELETE-05-02.                                SQ1344.2
101000     GO TO   SEQ-TEST-RD-05-02.                                   SQ1344.2
101100 SEQ-DELETE-05-02.                                                SQ1344.2
101200     PERFORM DE-LETE.                                             SQ1344.2
101300     GO TO   SEQ-TEST-05-02-END.                                  SQ1344.2
101400 SEQ-TEST-RD-05-02.                                               SQ1344.2
101500     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1344.2
101600         PERFORM PASS                                             SQ1344.2
101700     ELSE                                                         SQ1344.2
101800         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1344.2
101900         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1344.2
102000         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1344.2
102100                   TO RE-MARK                                     SQ1344.2
102200         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1344.2
102300         PERFORM FAIL.                                            SQ1344.2
102400 SEQ-TEST-05-02-END.                                              SQ1344.2
102500*                                                                 SQ1344.2
102600*    CHECK THAT THE CORRECT RECORD HAS BEEN RETURNED, BY          SQ1344.2
102700*    CHECKING THE RECORD-NUMBER FIELD.                            SQ1344.2
102800*                                                                 SQ1344.2
102900     ADD     1 TO REC-CT.                                         SQ1344.2
103000     IF DELETE-SW NOT = SPACE                                     SQ1344.2
103100         GO TO   SEQ-DELETE-05-03.                                SQ1344.2
103200     GO TO   SEQ-TEST-RD-05-03.                                   SQ1344.2
103300 SEQ-DELETE-05-03.                                                SQ1344.2
103400     PERFORM DE-LETE.                                             SQ1344.2
103500     GO TO   SEQ-TEST-05-03-END.                                  SQ1344.2
103600 SEQ-TEST-RD-05-03.                                               SQ1344.2
103700     IF FRECORD-NUMBER = XRECORD-NUMBER (1)                       SQ1344.2
103800         PERFORM PASS                                             SQ1344.2
103900     ELSE                                                         SQ1344.2
104000         MOVE    FRECORD-NUMBER TO COMPUTED-18V0                  SQ1344.2
104100         MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0               SQ1344.2
104200         MOVE   "INCORRECT RECORD NUMBER FOUND" TO RE-MARK        SQ1344.2
104300         PERFORM FAIL.                                            SQ1344.2
104400 SEQ-TEST-05-03-END.                                              SQ1344.2
104500     MOVE    SPACE TO DELETE-SW-2.                                SQ1344.2
104600*                                                                 SQ1344.2
104700*    FINALLY, TRY TO REWRITE A SMALLER RECORD                     SQ1344.2
104800*                                                                 SQ1344.2
104900 SEQ-INIT-06.                                                     SQ1344.2
105000     MOVE    0 TO REC-CT.                                         SQ1344.2
105100     MOVE    SPACE TO DECL-EXEC-SW.                               SQ1344.2
105200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1344.2
105300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1344.2
105400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ1344.2
105500     MOVE   "ABCDEFGHIJKLMNOPQR" TO EXT-18.                       SQ1344.2
105600     MOVE   "REWRITE SMALLER RECORD" TO FEATURE.                  SQ1344.2
105700     MOVE   "SEQ-TEST-RW-06" TO PAR-NAME.                         SQ1344.2
105800     IF DELETE-SW NOT = SPACE                                     SQ1344.2
105900         GO TO SEQ-DELETE-06-01.                                  SQ1344.2
106000     GO TO   SEQ-TEST-RW-06.                                      SQ1344.2
106100 SEQ-DELETE-06.                                                   SQ1344.2
106200     MOVE   "*" TO DELETE-SW-2.                                   SQ1344.2
106300     GO TO   SEQ-DELETE-06-01.                                    SQ1344.2
106400 SEQ-TEST-RW-06.                                                  SQ1344.2
106500     REWRITE SQ-FS4R1-F-G-120.                                    SQ1344.2
106600     MOVE    0 TO REC-CT.                                         SQ1344.2
106700     MOVE   "REWRITE SMALLER RECORD" TO FEATURE.                  SQ1344.2
106800     MOVE   "SEQ-TEST-RW-06" TO PAR-NAME.                         SQ1344.2
106900*                                                                 SQ1344.2
107000*    CHECK I-O STATUS RETURNED FROM REWRITE                       SQ1344.2
107100*                                                                 SQ1344.2
107200     ADD    1 TO REC-CT.                                          SQ1344.2
107300     IF DELETE-SW NOT = SPACE                                     SQ1344.2
107400         GO TO   SEQ-DELETE-06-01.                                SQ1344.2
107500     GO TO   SEQ-TEST-RW-06-01.                                   SQ1344.2
107600 SEQ-DELETE-06-01.                                                SQ1344.2
107700     PERFORM DE-LETE.                                             SQ1344.2
107800 SEQ-TEST-RW-06-01.                                               SQ1344.2
107900     IF SQ-FS4-STATUS = "44"                                      SQ1344.2
108000         PERFORM PASS                                             SQ1344.2
108100     ELSE                                                         SQ1344.2
108200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1344.2
108300         MOVE   "44" TO CORRECT-A                                 SQ1344.2
108400         MOVE   "UNEXPECTED STATUS CODE FROM REWRITE SHORTER"     SQ1344.2
108500                   TO RE-MARK                                     SQ1344.2
108600         MOVE   "VII-4,1.5.3(4)D, VII-48" TO ANSI-REFERENCE       SQ1344.2
108700         PERFORM FAIL.                                            SQ1344.2
108800 CCVS-EXIT SECTION.                                               SQ1344.2
108900 CCVS-999999.                                                     SQ1344.2
109000     GO TO   CLOSE-FILES.                                         SQ1344.2
      *END-OF,SQ134A                                                                  
