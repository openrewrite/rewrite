      *HEADER,COBOL,SQ226A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2264.2
000200 PROGRAM-ID.                                                      SQ2264.2
000300     SQ226A.                                                      SQ2264.2
000400****************************************************************  SQ2264.2
000500*                                                              *  SQ2264.2
000600*    VALIDATION FOR:-                                          *  SQ2264.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2264.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ2264.2
000900*                             REVISED 1986, AUGUST             *  SQ2264.2
001000*                                                              *  SQ2264.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ2264.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2264.2
001300*                                                              *  SQ2264.2
001400****************************************************************  SQ2264.2
001500*                                                              *  SQ2264.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ2264.2
001700*                                                              *  SQ2264.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ2264.2
001900*            X-55   SYSTEM PRINTER                             *  SQ2264.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ2264.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ2264.2
002200*                                                              *  SQ2264.2
002300****************************************************************  SQ2264.2
002400*                                                              *  SQ2264.2
002500*    THIS PROGRAM OPENS FOR OUTPUT A FILE WHICH IS ASSIGNED    *  SQ2264.2
002600*    TO A MASS STORAGE MEDIUM, WRITES ONE RECORD AND CLOSES    *  SQ2264.2
002700*    THE FILE.  THE FILE IS THEN OPENED FOR INPUT, AND TWO     *  SQ2264.2
002800*    READ STATEMENTS EXECUTED.  THE SECOND SHOULD CAUSE AN     *  SQ2264.2
002900*    AT END CONDITION.  AN OPEN EXTEND STATEMENT IS THEN       *  SQ2264.2
003000*    EXECUTED.  THIS SHOULD CAUSE AN EXCEPTION CONDITION,      *  SQ2264.2
003100*    WITH I-O STATUS "41" AND ENTRY TO THE APPLICABLE ERROR    *  SQ2264.2
003200*    DECLARATIVE.  THERE ARE DECLARATIVES FOR ALL FOUR OPEN    *  SQ2264.2
003300*    MODES, AND EITHER THE "INPUT" OR THE "EXTEND" DECLARATIVE *  SQ2264.2
003400*    COULD BE CONSIDERED APPLICABLE.  THE STANDARD IS          *  SQ2264.2
003500*    AMBIGUOUS ON THIS POINT, SEE PAGE VII-51, 4.6.4, GENERAL  *  SQ2264.2
003600*    RULE (5), SUB-RULES B AND E.  THE PROGRAM ACCEPTS         *  SQ2264.2
003700*    EXECUTION OF EITHER DECLARATIVE AS CORRECT, SO LONG AS    *  SQ2264.2
003800*    ONLY ONE OF THEM IS EXECUTED.                             *  SQ2264.2
003900*                                                              *  SQ2264.2
004000****************************************************************  SQ2264.2
004100*                                                                 SQ2264.2
004200 ENVIRONMENT DIVISION.                                            SQ2264.2
004300 CONFIGURATION SECTION.                                           SQ2264.2
004400 SOURCE-COMPUTER.                                                 SQ2264.2
004500     XXXXX082.                                                    SQ2264.2
004600 OBJECT-COMPUTER.                                                 SQ2264.2
004700     XXXXX083.                                                    SQ2264.2
004800*                                                                 SQ2264.2
004900 INPUT-OUTPUT SECTION.                                            SQ2264.2
005000 FILE-CONTROL.                                                    SQ2264.2
005100     SELECT PRINT-FILE ASSIGN TO                                  SQ2264.2
005200     XXXXX055.                                                    SQ2264.2
005300*                                                                 SQ2264.2
005400*    SELECT RAW-DATA   ASSIGN TO                                  SQ2264.2
005500*    XXXXX062                                                     SQ2264.2
005600*          ORGANIZATION IS INDEXED                                SQ2264.2
005700*          ACCESS MODE  IS RANDOM                                 SQ2264.2
005800*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ2264.2
005900*                                                                 SQ2264.2
006000     SELECT SQ-FS4                                                SQ2264.2
006100            RESERVE 1                                             SQ2264.2
006200            ASSIGN                                                SQ2264.2
006300     XXXXX014                                                     SQ2264.2
006400            SEQUENTIAL                                            SQ2264.2
006500            STATUS IS SQ-FS4-STATUS OF STATUS-GROUP.              SQ2264.2
006600*                                                                 SQ2264.2
006700*                                                                 SQ2264.2
006800 DATA DIVISION.                                                   SQ2264.2
006900 FILE SECTION.                                                    SQ2264.2
007000 FD  PRINT-FILE                                                   SQ2264.2
007100C    LABEL RECORDS                                                SQ2264.2
007200C    XXXXX084                                                     SQ2264.2
007300C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2264.2
007400               .                                                  SQ2264.2
007500 01  PRINT-REC    PICTURE X(120).                                 SQ2264.2
007600 01  DUMMY-RECORD PICTURE X(120).                                 SQ2264.2
007700*                                                                 SQ2264.2
007800*FD  RAW-DATA.                                                    SQ2264.2
007900*01  RAW-DATA-SATZ.                                               SQ2264.2
008000*    05  RAW-DATA-KEY        PIC X(6).                            SQ2264.2
008100*    05  C-DATE              PIC 9(6).                            SQ2264.2
008200*    05  C-TIME              PIC 9(8).                            SQ2264.2
008300*    05  NO-OF-TESTS         PIC 99.                              SQ2264.2
008400*    05  C-OK                PIC 999.                             SQ2264.2
008500*    05  C-ALL               PIC 999.                             SQ2264.2
008600*    05  C-FAIL              PIC 999.                             SQ2264.2
008700*    05  C-DELETED           PIC 999.                             SQ2264.2
008800*    05  C-INSPECT           PIC 999.                             SQ2264.2
008900*    05  C-NOTE              PIC X(13).                           SQ2264.2
009000*    05  C-INDENT            PIC X.                               SQ2264.2
009100*    05  C-ABORT             PIC X(8).                            SQ2264.2
009200*                                                                 SQ2264.2
009300 FD  SQ-FS4                                                       SQ2264.2
009400C    LABEL RECORD IS STANDARD                                     SQ2264.2
009500     BLOCK  120                                                   SQ2264.2
009600     RECORD 120                                                   SQ2264.2
009700                .                                                 SQ2264.2
009800 01  SQ-FS4R1-F-G-120.                                            SQ2264.2
009900        05 FFILE-RECORD-INFO-P1-120.                              SQ2264.2
010000           07 FILLER              PIC X(5).                       SQ2264.2
010100           07 FFILE-NAME          PIC X(6).                       SQ2264.2
010200           07 FILLER              PIC X(8).                       SQ2264.2
010300           07 FRECORD-NAME        PIC X(6).                       SQ2264.2
010400           07 FILLER              PIC X(1).                       SQ2264.2
010500           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ2264.2
010600           07 FILLER              PIC X(7).                       SQ2264.2
010700           07 FRECORD-NUMBER      PIC 9(6).                       SQ2264.2
010800           07 FILLER              PIC X(6).                       SQ2264.2
010900           07 FUPDATE-NUMBER      PIC 9(2).                       SQ2264.2
011000           07 FILLER              PIC X(5).                       SQ2264.2
011100           07 FODO-NUMBER         PIC 9(4).                       SQ2264.2
011200           07 FILLER              PIC X(5).                       SQ2264.2
011300           07 FPROGRAM-NAME       PIC X(5).                       SQ2264.2
011400           07 FILLER              PIC X(7).                       SQ2264.2
011500           07 FRECORD-LENGTH      PIC 9(6).                       SQ2264.2
011600           07 FILLER              PIC X(7).                       SQ2264.2
011700           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ2264.2
011800           07 FILLER              PIC X(1).                       SQ2264.2
011900           07 FBLOCK-SIZE         PIC 9(4).                       SQ2264.2
012000           07 FILLER              PIC X(6).                       SQ2264.2
012100           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ2264.2
012200           07 FILLER              PIC X(5).                       SQ2264.2
012300           07 FFILE-ORGANIZATION  PIC X(2).                       SQ2264.2
012400           07 FILLER              PIC X(6).                       SQ2264.2
012500           07 FLABEL-TYPE         PIC X(1).                       SQ2264.2
012600*                                                                 SQ2264.2
012700 WORKING-STORAGE SECTION.                                         SQ2264.2
012800*                                                                 SQ2264.2
012900***************************************************************   SQ2264.2
013000*                                                             *   SQ2264.2
013100*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ2264.2
013200*                                                             *   SQ2264.2
013300***************************************************************   SQ2264.2
013400*                                                                 SQ2264.2
013500 01  STATUS-GROUP.                                                SQ2264.2
013600     04  SQ-FS4-STATUS.                                           SQ2264.2
013700         07  SQ-FS4-KEY-1   PIC X.                                SQ2264.2
013800         07  SQ-FS4-KEY-2   PIC X.                                SQ2264.2
013900*                                                                 SQ2264.2
014000 01  DELETE-SW.                                                   SQ2264.2
014100     03  DELETE-SW-1 PIC X.                                       SQ2264.2
014200     03  DELETE-SW-1-GROUP.                                       SQ2264.2
014300         05  DELETE-SW-2 PIC X.                                   SQ2264.2
014400*                                                                 SQ2264.2
014500 01  DECL-EXEC-E   PIC X(12).                                     SQ2264.2
014600 01  DECL-EXEC-I   PIC X(12).                                     SQ2264.2
014700 01  DECL-EXEC-I-O PIC X(12).                                     SQ2264.2
014800 01  DECL-EXEC-O   PIC X(12).                                     SQ2264.2
014900*                                                                 SQ2264.2
015000 01  DECL-EXEC-SW PIC X.                                          SQ2264.2
015100 01  DECL-EXEC-CT PIC 9.                                          SQ2264.2
015200*                                                                 SQ2264.2
015300***************************************************************   SQ2264.2
015400*                                                             *   SQ2264.2
015500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ2264.2
015600*                                                             *   SQ2264.2
015700***************************************************************   SQ2264.2
015800*                                                                 SQ2264.2
015900 01  REC-SKEL-SUB   PIC 99.                                       SQ2264.2
016000*                                                                 SQ2264.2
016100 01  FILE-RECORD-INFORMATION-REC.                                 SQ2264.2
016200     03 FILE-RECORD-INFO-SKELETON.                                SQ2264.2
016300        05 FILLER                 PICTURE X(48)       VALUE       SQ2264.2
016400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2264.2
016500        05 FILLER                 PICTURE X(46)       VALUE       SQ2264.2
016600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2264.2
016700        05 FILLER                 PICTURE X(26)       VALUE       SQ2264.2
016800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2264.2
016900        05 FILLER                 PICTURE X(37)       VALUE       SQ2264.2
017000             ",RECKEY=                             ".             SQ2264.2
017100        05 FILLER                 PICTURE X(38)       VALUE       SQ2264.2
017200             ",ALTKEY1=                             ".            SQ2264.2
017300        05 FILLER                 PICTURE X(38)       VALUE       SQ2264.2
017400             ",ALTKEY2=                             ".            SQ2264.2
017500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2264.2
017600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2264.2
017700        05 FILE-RECORD-INFO-P1-120.                               SQ2264.2
017800           07 FILLER              PIC X(5).                       SQ2264.2
017900           07 XFILE-NAME          PIC X(6).                       SQ2264.2
018000           07 FILLER              PIC X(8).                       SQ2264.2
018100           07 XRECORD-NAME        PIC X(6).                       SQ2264.2
018200           07 FILLER              PIC X(1).                       SQ2264.2
018300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2264.2
018400           07 FILLER              PIC X(7).                       SQ2264.2
018500           07 XRECORD-NUMBER      PIC 9(6).                       SQ2264.2
018600           07 FILLER              PIC X(6).                       SQ2264.2
018700           07 UPDATE-NUMBER       PIC 9(2).                       SQ2264.2
018800           07 FILLER              PIC X(5).                       SQ2264.2
018900           07 ODO-NUMBER          PIC 9(4).                       SQ2264.2
019000           07 FILLER              PIC X(5).                       SQ2264.2
019100           07 XPROGRAM-NAME       PIC X(5).                       SQ2264.2
019200           07 FILLER              PIC X(7).                       SQ2264.2
019300           07 XRECORD-LENGTH      PIC 9(6).                       SQ2264.2
019400           07 FILLER              PIC X(7).                       SQ2264.2
019500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2264.2
019600           07 FILLER              PIC X(1).                       SQ2264.2
019700           07 XBLOCK-SIZE         PIC 9(4).                       SQ2264.2
019800           07 FILLER              PIC X(6).                       SQ2264.2
019900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2264.2
020000           07 FILLER              PIC X(5).                       SQ2264.2
020100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ2264.2
020200           07 FILLER              PIC X(6).                       SQ2264.2
020300           07 XLABEL-TYPE         PIC X(1).                       SQ2264.2
020400        05 FILE-RECORD-INFO-P121-240.                             SQ2264.2
020500           07 FILLER              PIC X(8).                       SQ2264.2
020600           07 XRECORD-KEY         PIC X(29).                      SQ2264.2
020700           07 FILLER              PIC X(9).                       SQ2264.2
020800           07 ALTERNATE-KEY1      PIC X(29).                      SQ2264.2
020900           07 FILLER              PIC X(9).                       SQ2264.2
021000           07 ALTERNATE-KEY2      PIC X(29).                      SQ2264.2
021100           07 FILLER              PIC X(7).                       SQ2264.2
021200*                                                                 SQ2264.2
021300 01  TEST-RESULTS.                                                SQ2264.2
021400     02 FILLER              PIC X      VALUE SPACE.               SQ2264.2
021500     02  PAR-NAME.                                                SQ2264.2
021600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ2264.2
021700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ2264.2
021800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ2264.2
021900     02 FILLER              PIC X      VALUE SPACE.               SQ2264.2
022000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ2264.2
022100     02 FILLER              PIC X      VALUE SPACE.               SQ2264.2
022200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ2264.2
022300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ2264.2
022400     02 RE-MARK             PIC X(61).                            SQ2264.2
022500 01  TEST-COMPUTED.                                               SQ2264.2
022600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ2264.2
022700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ2264.2
022800   02 COMPUTED-X.                                                 SQ2264.2
022900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ2264.2
023000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ2264.2
023100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ2264.2
023200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ2264.2
023300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ2264.2
023400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2264.2
023500        04 COMPUTED-18V0                   PIC -9(18).            SQ2264.2
023600        04 FILLER                          PIC X.                 SQ2264.2
023700     03 FILLER PIC X(50) VALUE SPACE.                             SQ2264.2
023800 01  TEST-CORRECT.                                                SQ2264.2
023900     02 FILLER PIC X(30) VALUE SPACE.                             SQ2264.2
024000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2264.2
024100     02 CORRECT-X.                                                SQ2264.2
024200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ2264.2
024300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ2264.2
024400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ2264.2
024500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ2264.2
024600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ2264.2
024700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2264.2
024800         04 CORRECT-18V0                     PIC -9(18).          SQ2264.2
024900         04 FILLER                           PIC X.               SQ2264.2
025000     03 FILLER PIC X(2) VALUE SPACE.                              SQ2264.2
025100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ2264.2
025200*                                                                 SQ2264.2
025300 01  CCVS-C-1.                                                    SQ2264.2
025400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2264.2
025500     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ2264.2
025600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2264.2
025700     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ2264.2
025800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ2264.2
025900     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ2264.2
026000     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ2264.2
026100     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ2264.2
026200 01  CCVS-C-2.                                                    SQ2264.2
026300     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2264.2
026400     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ2264.2
026500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ2264.2
026600     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ2264.2
026700     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ2264.2
026800*                                                                 SQ2264.2
026900 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ2264.2
027000 01  REC-CT            PIC 99       VALUE ZERO.                   SQ2264.2
027100 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ2264.2
027200 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ2264.2
027300 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ2264.2
027400 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ2264.2
027500 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ2264.2
027600 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ2264.2
027700 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ2264.2
027800 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ2264.2
027900 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ2264.2
028000 01  CCVS-H-1.                                                    SQ2264.2
028100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2264.2
028200     02  FILLER          PIC X(42)    VALUE                       SQ2264.2
028300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ2264.2
028400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ2264.2
028500 01  CCVS-H-2A.                                                   SQ2264.2
028600   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ2264.2
028700   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ2264.2
028800   02  FILLER            PIC XXXX   VALUE                         SQ2264.2
028900     "4.2 ".                                                      SQ2264.2
029000   02  FILLER            PIC X(28)  VALUE                         SQ2264.2
029100            " COPY - NOT FOR DISTRIBUTION".                       SQ2264.2
029200   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ2264.2
029300*                                                                 SQ2264.2
029400 01  CCVS-H-2B.                                                   SQ2264.2
029500   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ2264.2
029600   02  TEST-ID           PIC X(9).                                SQ2264.2
029700   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ2264.2
029800   02  FILLER            PIC X(12)  VALUE                         SQ2264.2
029900     " HIGH       ".                                              SQ2264.2
030000   02  FILLER            PIC X(22)  VALUE                         SQ2264.2
030100            " LEVEL VALIDATION FOR ".                             SQ2264.2
030200   02  FILLER            PIC X(58)  VALUE                         SQ2264.2
030300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2264.2
030400 01  CCVS-H-3.                                                    SQ2264.2
030500     02  FILLER          PIC X(34)  VALUE                         SQ2264.2
030600            " FOR OFFICIAL USE ONLY    ".                         SQ2264.2
030700     02  FILLER          PIC X(58)  VALUE                         SQ2264.2
030800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2264.2
030900     02  FILLER          PIC X(28)  VALUE                         SQ2264.2
031000            "  COPYRIGHT   1985,1986 ".                           SQ2264.2
031100 01  CCVS-E-1.                                                    SQ2264.2
031200     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ2264.2
031300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ2264.2
031400     02 ID-AGAIN         PIC X(9).                                SQ2264.2
031500     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ2264.2
031600 01  CCVS-E-2.                                                    SQ2264.2
031700     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ2264.2
031800     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ2264.2
031900     02  CCVS-E-2-2.                                              SQ2264.2
032000         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ2264.2
032100         03 FILLER         PIC X      VALUE SPACE.                SQ2264.2
032200         03 ENDER-DESC     PIC X(44)  VALUE                       SQ2264.2
032300            "ERRORS ENCOUNTERED".                                 SQ2264.2
032400 01  CCVS-E-3.                                                    SQ2264.2
032500     02  FILLER          PIC X(22)  VALUE                         SQ2264.2
032600            " FOR OFFICIAL USE ONLY".                             SQ2264.2
032700     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ2264.2
032800     02  FILLER          PIC X(58)  VALUE                         SQ2264.2
032900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2264.2
033000     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ2264.2
033100     02  FILLER          PIC X(20)  VALUE                         SQ2264.2
033200             " COPYRIGHT 1985,1986".                              SQ2264.2
033300 01  CCVS-E-4.                                                    SQ2264.2
033400     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ2264.2
033500     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ2264.2
033600     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ2264.2
033700     02 FILLER           PIC X(40)  VALUE                         SQ2264.2
033800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2264.2
033900 01  XXINFO.                                                      SQ2264.2
034000     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ2264.2
034100     02 INFO-TEXT.                                                SQ2264.2
034200       04 FILLER             PIC X(8)   VALUE SPACE.              SQ2264.2
034300       04 XXCOMPUTED         PIC X(20).                           SQ2264.2
034400       04 FILLER             PIC X(5)   VALUE SPACE.              SQ2264.2
034500       04 XXCORRECT          PIC X(20).                           SQ2264.2
034600     02 INF-ANSI-REFERENCE PIC X(48).                             SQ2264.2
034700 01  HYPHEN-LINE.                                                 SQ2264.2
034800     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ2264.2
034900     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ2264.2
035000-    "*****************************************".                 SQ2264.2
035100     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ2264.2
035200-    "******************************".                            SQ2264.2
035300 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ2264.2
035400     "SQ226A".                                                    SQ2264.2
035500*                                                                 SQ2264.2
035600*                                                                 SQ2264.2
035700 PROCEDURE DIVISION.                                              SQ2264.2
035800 DECLARATIVES.                                                    SQ2264.2
035900*                                                                 SQ2264.2
036000*    FIRST, A DECLARATIVE FOR THE PRINT FILE, IN CASE THERE       SQ2264.2
036100*    IS AN ERROR ON PRINTING DURING EXECUTION OF THE OUTPUT       SQ2264.2
036200*    DECLARATIVE.  PAGE VII-51, 4.6.4(7) STATES THAT THE          SQ2264.2
036300*    INVOCATION OF THE SAME DECLARATIVE TWICE IS ILLEGAL.         SQ2264.2
036400*                                                                 SQ2264.2
036500 SECT-SQ226A-0000 SECTION.                                        SQ2264.2
036600     USE AFTER STANDARD ERROR PROCEDURE ON PRINT-FILE.            SQ2264.2
036700 PRINT-FILE-ERROR-PROCESS.                                        SQ2264.2
036800     EXIT.                                                        SQ2264.2
036900*                                                                 SQ2264.2
037000 SECT-SQ226A-0001 SECTION.                                        SQ2264.2
037100     USE AFTER EXCEPTION PROCEDURE EXTEND.                        SQ2264.2
037200 EXTEND-ERROR-PROCESS.                                            SQ2264.2
037300     MOVE   "EXECUTED" TO DECL-EXEC-E.                            SQ2264.2
037400     ADD     1 TO DECL-EXEC-CT.                                   SQ2264.2
037500     PERFORM OUTPUT-ERROR-PROCESS THRU END-DECLS.                 SQ2264.2
037600*                                                                 SQ2264.2
037700 SECT-SQ226A-0002 SECTION.                                        SQ2264.2
037800     USE AFTER STANDARD ERROR PROCEDURE ON INPUT.                 SQ2264.2
037900 INPUT-ERROR-PROCESS.                                             SQ2264.2
038000     MOVE   "EXECUTED" TO DECL-EXEC-I.                            SQ2264.2
038100     ADD     1 TO DECL-EXEC-CT.                                   SQ2264.2
038200     PERFORM OUTPUT-ERROR-PROCESS THRU END-DECLS.                 SQ2264.2
038300*                                                                 SQ2264.2
038400 SECT-SQ226A-0003 SECTION.                                        SQ2264.2
038500     USE AFTER EXCEPTION PROCEDURE I-O.                           SQ2264.2
038600 I-O-ERROR-PROCESS.                                               SQ2264.2
038700     MOVE   "EXECUTED" TO DECL-EXEC-I-O.                          SQ2264.2
038800     ADD     1 TO DECL-EXEC-CT.                                   SQ2264.2
038900     PERFORM OUTPUT-ERROR-PROCESS THRU END-DECLS.                 SQ2264.2
039000*                                                                 SQ2264.2
039100 SECT-SQ226A-0004 SECTION.                                        SQ2264.2
039200     USE AFTER ERROR PROCEDURE OUTPUT.                            SQ2264.2
039300 OUTPUT-ERROR-PROCESS.                                            SQ2264.2
039400     IF DECL-EXEC-CT = 0                                          SQ2264.2
039500         MOVE   "EXECUTED" TO DECL-EXEC-O                         SQ2264.2
039600         ADD     1 TO DECL-EXEC-CT.                               SQ2264.2
039700*                                                                 SQ2264.2
039800     IF DECL-EXEC-SW NOT = SPACE                                  SQ2264.2
039900         GO TO   END-DECLS.                                       SQ2264.2
040000*                                                                 SQ2264.2
040100     MOVE    1 TO REC-CT.                                         SQ2264.2
040200     MOVE   "OPEN EXTEND OPEN FILE"  TO FEATURE.                  SQ2264.2
040300     MOVE   "DECL-OPEN-02" TO PAR-NAME.                           SQ2264.2
040400     GO TO   DECL-OPEN-02.                                        SQ2264.2
040500 DECL-DELETE-02.                                                  SQ2264.2
040600     PERFORM DECL-DE-LETE.                                        SQ2264.2
040700     GO TO   DECL-TEST-01-END.                                    SQ2264.2
040800 DECL-OPEN-02.                                                    SQ2264.2
040900     IF SQ-FS4-STATUS = "41"                                      SQ2264.2
041000         PERFORM DECL-PASS                                        SQ2264.2
041100     ELSE                                                         SQ2264.2
041200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
041300         MOVE   "41" TO CORRECT-A                                 SQ2264.2
041400         MOVE   "UNEXPECTED I-O STATUS ON OPEN OF OPEN FILE"      SQ2264.2
041500                   TO RE-MARK                                     SQ2264.2
041600         MOVE   "VII-3, VII-38,4.2.4(3)F" TO ANSI-REFERENCE       SQ2264.2
041700         PERFORM DECL-FAIL.                                       SQ2264.2
041800 DECL-TEST-01-END.                                                SQ2264.2
041900*                                                                 SQ2264.2
042000     PERFORM DECL-WRITE-LINE.                                     SQ2264.2
042100     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ2264.2
042200               TO DUMMY-RECORD.                                   SQ2264.2
042300     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ2264.2
042400     GO TO   END-DECLS.                                           SQ2264.2
042500*                                                                 SQ2264.2
042600*                                                                 SQ2264.2
042700 DECL-PASS.                                                       SQ2264.2
042800     MOVE   "PASS " TO P-OR-F.                                    SQ2264.2
042900     ADD     1 TO PASS-COUNTER.                                   SQ2264.2
043000     PERFORM DECL-PRINT-DETAIL.                                   SQ2264.2
043100*                                                                 SQ2264.2
043200 DECL-FAIL.                                                       SQ2264.2
043300     MOVE   "FAIL*" TO P-OR-F.                                    SQ2264.2
043400     ADD     1 TO ERROR-COUNTER.                                  SQ2264.2
043500     PERFORM DECL-PRINT-DETAIL.                                   SQ2264.2
043600*                                                                 SQ2264.2
043700 DECL-DE-LETE.                                                    SQ2264.2
043800     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2264.2
043900     MOVE   "*****" TO P-OR-F.                                    SQ2264.2
044000     ADD     1 TO DELETE-COUNTER.                                 SQ2264.2
044100     PERFORM DECL-PRINT-DETAIL.                                   SQ2264.2
044200*                                                                 SQ2264.2
044300 DECL-PRINT-DETAIL.                                               SQ2264.2
044400     IF REC-CT NOT EQUAL TO ZERO                                  SQ2264.2
044500             MOVE "." TO PARDOT-X                                 SQ2264.2
044600             MOVE REC-CT TO DOTVALUE.                             SQ2264.2
044700     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2264.2
044800     PERFORM DECL-WRITE-LINE.                                     SQ2264.2
044900     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2264.2
045000         PERFORM DECL-WRITE-LINE                                  SQ2264.2
045100         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ2264.2
045200     ELSE                                                         SQ2264.2
045300         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ2264.2
045400     MOVE    SPACE TO P-OR-F.                                     SQ2264.2
045500     MOVE    SPACE TO COMPUTED-X.                                 SQ2264.2
045600     MOVE    SPACE TO CORRECT-X.                                  SQ2264.2
045700     IF REC-CT EQUAL TO ZERO                                      SQ2264.2
045800         MOVE    SPACE TO PAR-NAME.                               SQ2264.2
045900     MOVE    SPACE TO RE-MARK.                                    SQ2264.2
046000*                                                                 SQ2264.2
046100 DECL-WRITE-LINE.                                                 SQ2264.2
046200     ADD     1 TO RECORD-COUNT.                                   SQ2264.2
046300*    IF RECORD-COUNT GREATER 50                                   SQ2264.2
046400*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ2264.2
046500*        MOVE    SPACE TO DUMMY-RECORD                            SQ2264.2
046600*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ2264.2
046700*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ2264.2
046800*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ2264.2
046900*        PERFORM DECL-WRT-LN 2 TIMES                              SQ2264.2
047000*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ2264.2
047100*        PERFORM DECL-WRT-LN                                      SQ2264.2
047200*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ2264.2
047300*        MOVE    ZERO TO RECORD-COUNT.                            SQ2264.2
047400     PERFORM DECL-WRT-LN.                                         SQ2264.2
047500*                                                                 SQ2264.2
047600 DECL-WRT-LN.                                                     SQ2264.2
047700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2264.2
047800     MOVE    SPACE TO DUMMY-RECORD.                               SQ2264.2
047900*                                                                 SQ2264.2
048000 DECL-FAIL-ROUTINE.                                               SQ2264.2
048100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ2264.2
048200     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ2264.2
048300     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2264.2
048400     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2264.2
048500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2264.2
048600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2264.2
048700     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2264.2
048800     GO TO   DECL-FAIL-EX.                                        SQ2264.2
048900 DECL-FAIL-WRITE.                                                 SQ2264.2
049000     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ2264.2
049100     PERFORM DECL-WRITE-LINE                                      SQ2264.2
049200     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2264.2
049300     MOVE    TEST-CORRECT TO PRINT-REC                            SQ2264.2
049400     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2264.2
049500     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ2264.2
049600 DECL-FAIL-EX.                                                    SQ2264.2
049700     EXIT.                                                        SQ2264.2
049800*                                                                 SQ2264.2
049900 DECL-BAIL.                                                       SQ2264.2
050000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ2264.2
050100     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ2264.2
050200 DECL-BAIL-WRITE.                                                 SQ2264.2
050300     MOVE    CORRECT-A TO XXCORRECT.                              SQ2264.2
050400     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ2264.2
050500     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2264.2
050600     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ2264.2
050700 DECL-BAIL-EX.                                                    SQ2264.2
050800     EXIT.                                                        SQ2264.2
050900*                                                                 SQ2264.2
051000 END-DECLS.                                                       SQ2264.2
051100 END DECLARATIVES.                                                SQ2264.2
051200*                                                                 SQ2264.2
051300*                                                                 SQ2264.2
051400 CCVS1 SECTION.                                                   SQ2264.2
051500 OPEN-FILES.                                                      SQ2264.2
051600*    OPEN    I-O RAW-DATA.                                        SQ2264.2
051700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2264.2
051800*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ2264.2
051900*    MOVE   "ABORTED "   TO C-ABORT.                              SQ2264.2
052000*    ADD     1           TO C-NO-OF-TESTS.                        SQ2264.2
052100*    ACCEPT  C-DATE      FROM DATE.                               SQ2264.2
052200*    ACCEPT  C-TIME      FROM TIME.                               SQ2264.2
052300*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2264.2
052400*END-E-1.                                                         SQ2264.2
052500*    CLOSE   RAW-DATA.                                            SQ2264.2
052600     OPEN    OUTPUT PRINT-FILE.                                   SQ2264.2
052700     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ2264.2
052800     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ2264.2
052900     MOVE    SPACE TO TEST-RESULTS.                               SQ2264.2
053000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2264.2
053100     MOVE    ZERO TO REC-SKEL-SUB.                                SQ2264.2
053200     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ2264.2
053300     GO TO CCVS1-EXIT.                                            SQ2264.2
053400*                                                                 SQ2264.2
053500 CCVS-INIT-FILE.                                                  SQ2264.2
053600     ADD     1 TO REC-SKL-SUB.                                    SQ2264.2
053700     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ2264.2
053800                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2264.2
053900*                                                                 SQ2264.2
054000 CLOSE-FILES.                                                     SQ2264.2
054100     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ2264.2
054200     CLOSE   PRINT-FILE.                                          SQ2264.2
054300*    OPEN    I-O RAW-DATA.                                        SQ2264.2
054400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ2264.2
054500*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ2264.2
054600*    MOVE   "OK.     " TO C-ABORT.                                SQ2264.2
054700*    MOVE    PASS-COUNTER  TO C-OK.                               SQ2264.2
054800*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ2264.2
054900*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ2264.2
055000*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ2264.2
055100*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ2264.2
055200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ2264.2
055300*END-E-2.                                                         SQ2264.2
055400*    CLOSE   RAW-DATA.                                            SQ2264.2
055500 TERMINATE-CCVS.                                                  SQ2264.2
055600*    EXIT    PROGRAM.                                             SQ2264.2
055700     STOP    RUN.                                                 SQ2264.2
055800*                                                                 SQ2264.2
055900 INSPT.                                                           SQ2264.2
056000     MOVE   "INSPT" TO P-OR-F.                                    SQ2264.2
056100     ADD     1 TO INSPECT-COUNTER.                                SQ2264.2
056200     PERFORM PRINT-DETAIL.                                        SQ2264.2
056300*                                                                 SQ2264.2
056400 PASS.                                                            SQ2264.2
056500     MOVE   "PASS " TO P-OR-F.                                    SQ2264.2
056600     ADD     1 TO PASS-COUNTER.                                   SQ2264.2
056700     PERFORM PRINT-DETAIL.                                        SQ2264.2
056800*                                                                 SQ2264.2
056900 FAIL.                                                            SQ2264.2
057000     MOVE   "FAIL*" TO P-OR-F.                                    SQ2264.2
057100     ADD     1 TO ERROR-COUNTER.                                  SQ2264.2
057200     PERFORM PRINT-DETAIL.                                        SQ2264.2
057300*                                                                 SQ2264.2
057400 DE-LETE.                                                         SQ2264.2
057500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ2264.2
057600     MOVE   "*****" TO P-OR-F.                                    SQ2264.2
057700     ADD     1 TO DELETE-COUNTER.                                 SQ2264.2
057800     PERFORM PRINT-DETAIL.                                        SQ2264.2
057900*                                                                 SQ2264.2
058000 PRINT-DETAIL.                                                    SQ2264.2
058100     IF REC-CT NOT EQUAL TO ZERO                                  SQ2264.2
058200         MOVE   "." TO PARDOT-X                                   SQ2264.2
058300         MOVE    REC-CT TO DOTVALUE.                              SQ2264.2
058400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ2264.2
058500     PERFORM WRITE-LINE.                                          SQ2264.2
058600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ2264.2
058700         PERFORM WRITE-LINE                                       SQ2264.2
058800         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ2264.2
058900     ELSE                                                         SQ2264.2
059000         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ2264.2
059100     MOVE    SPACE TO P-OR-F.                                     SQ2264.2
059200     MOVE    SPACE TO COMPUTED-X.                                 SQ2264.2
059300     MOVE    SPACE TO CORRECT-X.                                  SQ2264.2
059400     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ2264.2
059500     MOVE    SPACE TO RE-MARK.                                    SQ2264.2
059600*                                                                 SQ2264.2
059700 HEAD-ROUTINE.                                                    SQ2264.2
059800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2264.2
059900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ2264.2
060000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2264.2
060100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ2264.2
060200 COLUMN-NAMES-ROUTINE.                                            SQ2264.2
060300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2264.2
060400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2264.2
060500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2264.2
060600 END-ROUTINE.                                                     SQ2264.2
060700     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ2264.2
060800     PERFORM WRITE-LINE 5 TIMES.                                  SQ2264.2
060900 END-RTN-EXIT.                                                    SQ2264.2
061000     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ2264.2
061100     PERFORM WRITE-LINE 2 TIMES.                                  SQ2264.2
061200*                                                                 SQ2264.2
061300 END-ROUTINE-1.                                                   SQ2264.2
061400     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ2264.2
061500     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ2264.2
061600     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ2264.2
061700     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ2264.2
061800     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ2264.2
061900     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ2264.2
062000     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ2264.2
062100     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ2264.2
062200     PERFORM WRITE-LINE.                                          SQ2264.2
062300     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ2264.2
062400     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ2264.2
062500         MOVE   "NO " TO ERROR-TOTAL                              SQ2264.2
062600     ELSE                                                         SQ2264.2
062700         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ2264.2
062800     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2264.2
062900     PERFORM WRITE-LINE.                                          SQ2264.2
063000 END-ROUTINE-13.                                                  SQ2264.2
063100     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ2264.2
063200         MOVE   "NO " TO ERROR-TOTAL                              SQ2264.2
063300     ELSE                                                         SQ2264.2
063400         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ2264.2
063500     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ2264.2
063600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ2264.2
063700     PERFORM WRITE-LINE.                                          SQ2264.2
063800     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ2264.2
063900         MOVE   "NO " TO ERROR-TOTAL                              SQ2264.2
064000     ELSE                                                         SQ2264.2
064100         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ2264.2
064200     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ2264.2
064300     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2264.2
064400     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2264.2
064500*                                                                 SQ2264.2
064600 WRITE-LINE.                                                      SQ2264.2
064700     ADD     1 TO RECORD-COUNT.                                   SQ2264.2
064800*    IF RECORD-COUNT GREATER 50                                   SQ2264.2
064900*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ2264.2
065000*        MOVE  SPACE TO DUMMY-RECORD                              SQ2264.2
065100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2264.2
065200*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ2264.2
065300*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ2264.2
065400*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ2264.2
065500*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ2264.2
065600*        MOVE  ZERO TO RECORD-COUNT.                              SQ2264.2
065700     PERFORM WRT-LN.                                              SQ2264.2
065800*                                                                 SQ2264.2
065900 WRT-LN.                                                          SQ2264.2
066000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ2264.2
066100     MOVE    SPACE TO DUMMY-RECORD.                               SQ2264.2
066200 BLANK-LINE-PRINT.                                                SQ2264.2
066300     PERFORM WRT-LN.                                              SQ2264.2
066400 FAIL-ROUTINE.                                                    SQ2264.2
066500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2264.2
066600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2264.2
066700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2264.2
066800     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ2264.2
066900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2264.2
067000     PERFORM WRITE-LINE 2 TIMES.                                  SQ2264.2
067100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2264.2
067200     GO TO   FAIL-ROUTINE-EX.                                     SQ2264.2
067300 FAIL-ROUTINE-WRITE.                                              SQ2264.2
067400     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ2264.2
067500     PERFORM WRITE-LINE                                           SQ2264.2
067600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ2264.2
067700     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ2264.2
067800     PERFORM WRITE-LINE 2 TIMES.                                  SQ2264.2
067900     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ2264.2
068000 FAIL-ROUTINE-EX.                                                 SQ2264.2
068100     EXIT.                                                        SQ2264.2
068200 BAIL-OUT.                                                        SQ2264.2
068300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2264.2
068400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2264.2
068500 BAIL-OUT-WRITE.                                                  SQ2264.2
068600     MOVE    CORRECT-A      TO XXCORRECT.                         SQ2264.2
068700     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ2264.2
068800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ2264.2
068900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ2264.2
069000     PERFORM WRITE-LINE 2 TIMES.                                  SQ2264.2
069100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ2264.2
069200 BAIL-OUT-EX.                                                     SQ2264.2
069300     EXIT.                                                        SQ2264.2
069400 CCVS1-EXIT.                                                      SQ2264.2
069500     EXIT.                                                        SQ2264.2
069600*                                                                 SQ2264.2
069700****************************************************************  SQ2264.2
069800*                                                              *  SQ2264.2
069900*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ2264.2
070000*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ2264.2
070100*                                                              *  SQ2264.2
070200****************************************************************  SQ2264.2
070300*                                                                 SQ2264.2
070400 SECT-SQ226A-0005 SECTION.                                        SQ2264.2
070500 STA-INIT.                                                        SQ2264.2
070600     MOVE    SPACE TO DELETE-SW.                                  SQ2264.2
070700*                                                                 SQ2264.2
070800     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ2264.2
070900     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ2264.2
071000     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ2264.2
071100     MOVE    120 TO XRECORD-LENGTH (1).                           SQ2264.2
071200     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ2264.2
071300     MOVE    1   TO XBLOCK-SIZE (1).                              SQ2264.2
071400     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ2264.2
071500     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ2264.2
071600     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ2264.2
071700*                                                                 SQ2264.2
071800*    OPEN THE FILE IN THE OUTPUT MODE                             SQ2264.2
071900*                                                                 SQ2264.2
072000 SEQ-INIT-01.                                                     SQ2264.2
072100     MOVE    0 TO REC-CT.                                         SQ2264.2
072200     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
072300     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2264.2
072400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
072500     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
072600     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
072700     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
072800     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
072900     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2264.2
073000     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ2264.2
073100     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ2264.2
073200     GO TO   SEQ-TEST-OP-01.                                      SQ2264.2
073300 SEQ-DELETE-01.                                                   SQ2264.2
073400     MOVE   "*" TO DELETE-SW-1.                                   SQ2264.2
073500     GO TO   SEQ-DELETE-01-01.                                    SQ2264.2
073600 SEQ-TEST-OP-01.                                                  SQ2264.2
073700     OPEN    OUTPUT SQ-FS4.                                       SQ2264.2
073800*                                                                 SQ2264.2
073900*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ2264.2
074000*                                                                 SQ2264.2
074100     ADD    1 TO REC-CT.                                          SQ2264.2
074200     IF DELETE-SW NOT = SPACE                                     SQ2264.2
074300         GO TO   SEQ-DELETE-01-01.                                SQ2264.2
074400     GO TO   SEQ-TEST-OP-01-01.                                   SQ2264.2
074500 SEQ-DELETE-01-01.                                                SQ2264.2
074600     PERFORM DE-LETE.                                             SQ2264.2
074700     GO TO   SEQ-TEST-01-01-END.                                  SQ2264.2
074800 SEQ-TEST-OP-01-01.                                               SQ2264.2
074900     IF SQ-FS4-STATUS = "00"                                      SQ2264.2
075000         PERFORM PASS                                             SQ2264.2
075100     ELSE                                                         SQ2264.2
075200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
075300         MOVE   "00" TO CORRECT-A                                 SQ2264.2
075400         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ2264.2
075500                   TO RE-MARK                                     SQ2264.2
075600         MOVE   "VII-3, VII-43" TO ANSI-REFERENCE                 SQ2264.2
075700         PERFORM FAIL.                                            SQ2264.2
075800 SEQ-TEST-01-01-END.                                              SQ2264.2
075900*                                                                 SQ2264.2
076000*    CHECK EXECUTION OF EXTEND DECLARATIVE                        SQ2264.2
076100*                                                                 SQ2264.2
076200     ADD     1 TO REC-CT.                                         SQ2264.2
076300     IF DELETE-SW NOT = SPACE                                     SQ2264.2
076400         GO TO   SEQ-DELETE-01-02.                                SQ2264.2
076500     GO TO   SEQ-TEST-OP-01-02.                                   SQ2264.2
076600 SEQ-DELETE-01-02.                                                SQ2264.2
076700     PERFORM DE-LETE.                                             SQ2264.2
076800     GO TO   SEQ-TEST-01-02-END.                                  SQ2264.2
076900 SEQ-TEST-OP-01-02.                                               SQ2264.2
077000     IF DECL-EXEC-E = "NOT EXECUTED"                              SQ2264.2
077100         PERFORM PASS                                             SQ2264.2
077200     ELSE                                                         SQ2264.2
077300         MOVE    DECL-EXEC-E TO COMPUTED-A                        SQ2264.2
077400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
077500         MOVE   "UNEXPECTED EXECUTION OF EXTEND DECLARATIVE"      SQ2264.2
077600                   TO RE-MARK                                     SQ2264.2
077700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
077800         PERFORM FAIL.                                            SQ2264.2
077900 SEQ-TEST-01-02-END.                                              SQ2264.2
078000*                                                                 SQ2264.2
078100*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ2264.2
078200*                                                                 SQ2264.2
078300     ADD     1 TO REC-CT.                                         SQ2264.2
078400     IF DELETE-SW NOT = SPACE                                     SQ2264.2
078500         GO TO   SEQ-DELETE-01-03.                                SQ2264.2
078600     GO TO   SEQ-TEST-OP-01-03.                                   SQ2264.2
078700 SEQ-DELETE-01-03.                                                SQ2264.2
078800     PERFORM DE-LETE.                                             SQ2264.2
078900     GO TO   SEQ-TEST-01-03-END.                                  SQ2264.2
079000 SEQ-TEST-OP-01-03.                                               SQ2264.2
079100     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ2264.2
079200         PERFORM PASS                                             SQ2264.2
079300     ELSE                                                         SQ2264.2
079400         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ2264.2
079500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
079600         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
079700                   TO RE-MARK                                     SQ2264.2
079800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
079900         PERFORM FAIL.                                            SQ2264.2
080000 SEQ-TEST-01-03-END.                                              SQ2264.2
080100*                                                                 SQ2264.2
080200*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
080300*                                                                 SQ2264.2
080400     ADD     1 TO REC-CT.                                         SQ2264.2
080500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
080600         GO TO   SEQ-DELETE-01-04.                                SQ2264.2
080700     GO TO   SEQ-TEST-OP-01-04.                                   SQ2264.2
080800 SEQ-DELETE-01-04.                                                SQ2264.2
080900     PERFORM DE-LETE.                                             SQ2264.2
081000     GO TO   SEQ-TEST-01-04-END.                                  SQ2264.2
081100 SEQ-TEST-OP-01-04.                                               SQ2264.2
081200     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
081300         PERFORM PASS                                             SQ2264.2
081400     ELSE                                                         SQ2264.2
081500         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
081600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
081700         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
081800                   TO RE-MARK                                     SQ2264.2
081900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
082000         PERFORM FAIL.                                            SQ2264.2
082100 SEQ-TEST-01-04-END.                                              SQ2264.2
082200*                                                                 SQ2264.2
082300*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
082400*                                                                 SQ2264.2
082500     ADD     1 TO REC-CT.                                         SQ2264.2
082600     IF DELETE-SW NOT = SPACE                                     SQ2264.2
082700         GO TO   SEQ-DELETE-01-05.                                SQ2264.2
082800     GO TO   SEQ-TEST-OP-01-05.                                   SQ2264.2
082900 SEQ-DELETE-01-05.                                                SQ2264.2
083000     PERFORM DE-LETE.                                             SQ2264.2
083100     GO TO   SEQ-TEST-01-05-END.                                  SQ2264.2
083200 SEQ-TEST-OP-01-05.                                               SQ2264.2
083300     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
083400         PERFORM PASS                                             SQ2264.2
083500     ELSE                                                         SQ2264.2
083600         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
083700         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
083800         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
083900                   TO RE-MARK                                     SQ2264.2
084000         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
084100         PERFORM FAIL.                                            SQ2264.2
084200 SEQ-TEST-01-05-END.                                              SQ2264.2
084300*                                                                 SQ2264.2
084400*                                                                 SQ2264.2
084500*    A NEW FILE IS OPEN.  WE NOW WRITE ONE RECORD.                SQ2264.2
084600*                                                                 SQ2264.2
084700 SEQ-INIT-02.                                                     SQ2264.2
084800     MOVE    0 TO REC-CT.                                         SQ2264.2
084900     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
085000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2264.2
085100     ADD     1 TO XRECORD-NUMBER (1).                             SQ2264.2
085200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
085300     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
085400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
085500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
085600     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
085700     MOVE   "WRITE A RECORD"  TO FEATURE.                         SQ2264.2
085800     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ2264.2
085900     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2264.2
086000         GO TO SEQ-DELETE-02.                                     SQ2264.2
086100     GO TO   SEQ-TEST-WR-02.                                      SQ2264.2
086200 SEQ-DELETE-02.                                                   SQ2264.2
086300     MOVE   "*" TO DELETE-SW-2.                                   SQ2264.2
086400     GO TO   SEQ-DELETE-02-01.                                    SQ2264.2
086500 SEQ-TEST-WR-02.                                                  SQ2264.2
086600     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ2264.2
086700     WRITE   SQ-FS4R1-F-G-120.                                    SQ2264.2
086800*                                                                 SQ2264.2
086900*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ2264.2
087000*                                                                 SQ2264.2
087100     ADD    1 TO REC-CT.                                          SQ2264.2
087200     IF DELETE-SW NOT = SPACE                                     SQ2264.2
087300         GO TO   SEQ-DELETE-02-01.                                SQ2264.2
087400     GO TO   SEQ-TEST-WR-02-01.                                   SQ2264.2
087500 SEQ-DELETE-02-01.                                                SQ2264.2
087600     PERFORM DE-LETE.                                             SQ2264.2
087700     GO TO   SEQ-TEST-02-01-END.                                  SQ2264.2
087800 SEQ-TEST-WR-02-01.                                               SQ2264.2
087900     IF SQ-FS4-STATUS = "00"                                      SQ2264.2
088000         PERFORM PASS                                             SQ2264.2
088100     ELSE                                                         SQ2264.2
088200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
088300         MOVE   "00" TO CORRECT-A                                 SQ2264.2
088400         MOVE   "UNEXPECTED ERROR CODE FROM WRITE"                SQ2264.2
088500                   TO RE-MARK                                     SQ2264.2
088600         MOVE   "VII-3, VII-53" TO ANSI-REFERENCE                 SQ2264.2
088700         PERFORM FAIL.                                            SQ2264.2
088800 SEQ-TEST-02-01-END.                                              SQ2264.2
088900*                                                                 SQ2264.2
089000*    CHECK EXECUTION OF EXTEND DECLARATIVE                        SQ2264.2
089100*                                                                 SQ2264.2
089200     ADD     1 TO REC-CT.                                         SQ2264.2
089300     IF DELETE-SW NOT = SPACE                                     SQ2264.2
089400         GO TO   SEQ-DELETE-02-02.                                SQ2264.2
089500     GO TO   SEQ-TEST-WR-02-02.                                   SQ2264.2
089600 SEQ-DELETE-02-02.                                                SQ2264.2
089700     PERFORM DE-LETE.                                             SQ2264.2
089800     GO TO   SEQ-TEST-02-02-END.                                  SQ2264.2
089900 SEQ-TEST-WR-02-02.                                               SQ2264.2
090000     IF DECL-EXEC-E = "NOT EXECUTED"                              SQ2264.2
090100         PERFORM PASS                                             SQ2264.2
090200     ELSE                                                         SQ2264.2
090300         MOVE    DECL-EXEC-E TO COMPUTED-A                        SQ2264.2
090400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
090500         MOVE   "UNEXPECTED EXECUTION OF EXTEND DECLARATIVE"      SQ2264.2
090600                   TO RE-MARK                                     SQ2264.2
090700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
090800         PERFORM FAIL.                                            SQ2264.2
090900 SEQ-TEST-02-02-END.                                              SQ2264.2
091000*                                                                 SQ2264.2
091100*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ2264.2
091200*                                                                 SQ2264.2
091300     ADD     1 TO REC-CT.                                         SQ2264.2
091400     IF DELETE-SW NOT = SPACE                                     SQ2264.2
091500         GO TO   SEQ-DELETE-02-03.                                SQ2264.2
091600     GO TO   SEQ-TEST-WR-02-03.                                   SQ2264.2
091700 SEQ-DELETE-02-03.                                                SQ2264.2
091800     PERFORM DE-LETE.                                             SQ2264.2
091900     GO TO   SEQ-TEST-02-03-END.                                  SQ2264.2
092000 SEQ-TEST-WR-02-03.                                               SQ2264.2
092100     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ2264.2
092200         PERFORM PASS                                             SQ2264.2
092300     ELSE                                                         SQ2264.2
092400         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ2264.2
092500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
092600         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
092700                   TO RE-MARK                                     SQ2264.2
092800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
092900         PERFORM FAIL.                                            SQ2264.2
093000 SEQ-TEST-02-03-END.                                              SQ2264.2
093100*                                                                 SQ2264.2
093200*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
093300*                                                                 SQ2264.2
093400     ADD     1 TO REC-CT.                                         SQ2264.2
093500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
093600         GO TO   SEQ-DELETE-02-04.                                SQ2264.2
093700     GO TO   SEQ-TEST-WR-02-04.                                   SQ2264.2
093800 SEQ-DELETE-02-04.                                                SQ2264.2
093900     PERFORM DE-LETE.                                             SQ2264.2
094000     GO TO   SEQ-TEST-02-04-END.                                  SQ2264.2
094100 SEQ-TEST-WR-02-04.                                               SQ2264.2
094200     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
094300         PERFORM PASS                                             SQ2264.2
094400     ELSE                                                         SQ2264.2
094500         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
094600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
094700         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
094800                   TO RE-MARK                                     SQ2264.2
094900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
095000         PERFORM FAIL.                                            SQ2264.2
095100 SEQ-TEST-02-04-END.                                              SQ2264.2
095200*                                                                 SQ2264.2
095300*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
095400*                                                                 SQ2264.2
095500     ADD     1 TO REC-CT.                                         SQ2264.2
095600     IF DELETE-SW NOT = SPACE                                     SQ2264.2
095700         GO TO   SEQ-DELETE-02-05.                                SQ2264.2
095800     GO TO   SEQ-TEST-WR-02-05.                                   SQ2264.2
095900 SEQ-DELETE-02-05.                                                SQ2264.2
096000     PERFORM DE-LETE.                                             SQ2264.2
096100     GO TO   SEQ-TEST-02-05-END.                                  SQ2264.2
096200 SEQ-TEST-WR-02-05.                                               SQ2264.2
096300     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
096400         PERFORM PASS                                             SQ2264.2
096500     ELSE                                                         SQ2264.2
096600         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
096700         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
096800         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
096900                   TO RE-MARK                                     SQ2264.2
097000         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
097100         PERFORM FAIL.                                            SQ2264.2
097200 SEQ-TEST-02-05-END.                                              SQ2264.2
097300     MOVE    SPACE TO DELETE-SW-2.                                SQ2264.2
097400*                                                                 SQ2264.2
097500*                                                                 SQ2264.2
097600*    NOW CLOSE THE FILE.                                          SQ2264.2
097700*                                                                 SQ2264.2
097800 SEQ-INIT-03.                                                     SQ2264.2
097900     MOVE    0 TO REC-CT.                                         SQ2264.2
098000     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
098100     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2264.2
098200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
098300     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
098400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
098500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
098600     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
098700     MOVE   "CLOSE FILE" TO FEATURE.                              SQ2264.2
098800     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ2264.2
098900     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2264.2
099000         GO TO SEQ-DELETE-03.                                     SQ2264.2
099100     GO TO   SEQ-TEST-CL-03.                                      SQ2264.2
099200 SEQ-DELETE-03.                                                   SQ2264.2
099300     MOVE   "*" TO DELETE-SW-2.                                   SQ2264.2
099400     GO TO   SEQ-DELETE-03-01.                                    SQ2264.2
099500 SEQ-TEST-CL-03.                                                  SQ2264.2
099600     CLOSE   SQ-FS4.                                              SQ2264.2
099700*                                                                 SQ2264.2
099800*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ2264.2
099900*                                                                 SQ2264.2
100000     ADD    1 TO REC-CT.                                          SQ2264.2
100100     IF DELETE-SW NOT = SPACE                                     SQ2264.2
100200         GO TO   SEQ-DELETE-03-01.                                SQ2264.2
100300     GO TO   SEQ-TEST-CL-03-01.                                   SQ2264.2
100400 SEQ-DELETE-03-01.                                                SQ2264.2
100500     PERFORM DE-LETE.                                             SQ2264.2
100600     GO TO   SEQ-TEST-03-01-END.                                  SQ2264.2
100700 SEQ-TEST-CL-03-01.                                               SQ2264.2
100800     IF SQ-FS4-STATUS = "00"                                      SQ2264.2
100900         PERFORM PASS                                             SQ2264.2
101000     ELSE                                                         SQ2264.2
101100         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
101200         MOVE   "00" TO CORRECT-A                                 SQ2264.2
101300         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ2264.2
101400                   TO RE-MARK                                     SQ2264.2
101500         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ2264.2
101600         PERFORM FAIL.                                            SQ2264.2
101700 SEQ-TEST-03-01-END.                                              SQ2264.2
101800*                                                                 SQ2264.2
101900*    CHECK EXECUTION OF EXTEND DECLARATIVE                        SQ2264.2
102000*                                                                 SQ2264.2
102100     ADD     1 TO REC-CT.                                         SQ2264.2
102200     IF DELETE-SW NOT = SPACE                                     SQ2264.2
102300         GO TO   SEQ-DELETE-03-02.                                SQ2264.2
102400     GO TO   SEQ-TEST-CL-03-02.                                   SQ2264.2
102500 SEQ-DELETE-03-02.                                                SQ2264.2
102600     PERFORM DE-LETE.                                             SQ2264.2
102700     GO TO   SEQ-TEST-03-02-END.                                  SQ2264.2
102800 SEQ-TEST-CL-03-02.                                               SQ2264.2
102900     IF DECL-EXEC-E = "NOT EXECUTED"                              SQ2264.2
103000         PERFORM PASS                                             SQ2264.2
103100     ELSE                                                         SQ2264.2
103200         MOVE    DECL-EXEC-E TO COMPUTED-A                        SQ2264.2
103300         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
103400         MOVE   "UNEXPECTED EXECUTION OF EXTEND DECLARATIVE"      SQ2264.2
103500                   TO RE-MARK                                     SQ2264.2
103600         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
103700         PERFORM FAIL.                                            SQ2264.2
103800 SEQ-TEST-03-02-END.                                              SQ2264.2
103900*                                                                 SQ2264.2
104000*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ2264.2
104100*                                                                 SQ2264.2
104200     ADD     1 TO REC-CT.                                         SQ2264.2
104300     IF DELETE-SW NOT = SPACE                                     SQ2264.2
104400         GO TO   SEQ-DELETE-03-03.                                SQ2264.2
104500     GO TO   SEQ-TEST-CL-03-03.                                   SQ2264.2
104600 SEQ-DELETE-03-03.                                                SQ2264.2
104700     PERFORM DE-LETE.                                             SQ2264.2
104800     GO TO   SEQ-TEST-03-03-END.                                  SQ2264.2
104900 SEQ-TEST-CL-03-03.                                               SQ2264.2
105000     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ2264.2
105100         PERFORM PASS                                             SQ2264.2
105200     ELSE                                                         SQ2264.2
105300         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ2264.2
105400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
105500         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
105600                   TO RE-MARK                                     SQ2264.2
105700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
105800         PERFORM FAIL.                                            SQ2264.2
105900 SEQ-TEST-03-03-END.                                              SQ2264.2
106000*                                                                 SQ2264.2
106100*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
106200*                                                                 SQ2264.2
106300     ADD     1 TO REC-CT.                                         SQ2264.2
106400     IF DELETE-SW NOT = SPACE                                     SQ2264.2
106500         GO TO   SEQ-DELETE-03-04.                                SQ2264.2
106600     GO TO   SEQ-TEST-CL-03-04.                                   SQ2264.2
106700 SEQ-DELETE-03-04.                                                SQ2264.2
106800     PERFORM DE-LETE.                                             SQ2264.2
106900     GO TO   SEQ-TEST-03-04-END.                                  SQ2264.2
107000 SEQ-TEST-CL-03-04.                                               SQ2264.2
107100     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
107200         PERFORM PASS                                             SQ2264.2
107300     ELSE                                                         SQ2264.2
107400         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
107500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
107600         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
107700                   TO RE-MARK                                     SQ2264.2
107800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
107900         PERFORM FAIL.                                            SQ2264.2
108000 SEQ-TEST-03-04-END.                                              SQ2264.2
108100*                                                                 SQ2264.2
108200*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
108300*                                                                 SQ2264.2
108400     ADD     1 TO REC-CT.                                         SQ2264.2
108500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
108600         GO TO   SEQ-DELETE-03-05.                                SQ2264.2
108700     GO TO   SEQ-TEST-CL-03-05.                                   SQ2264.2
108800 SEQ-DELETE-03-05.                                                SQ2264.2
108900     PERFORM DE-LETE.                                             SQ2264.2
109000     GO TO   SEQ-TEST-03-05-END.                                  SQ2264.2
109100 SEQ-TEST-CL-03-05.                                               SQ2264.2
109200     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
109300         PERFORM PASS                                             SQ2264.2
109400     ELSE                                                         SQ2264.2
109500         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
109600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
109700         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
109800                   TO RE-MARK                                     SQ2264.2
109900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
110000         PERFORM FAIL.                                            SQ2264.2
110100 SEQ-TEST-03-05-END.                                              SQ2264.2
110200     MOVE    SPACE TO DELETE-SW-2.                                SQ2264.2
110300*                                                                 SQ2264.2
110400*                                                                 SQ2264.2
110500*    OPEN THE FILE IN THE INPUT MODE                              SQ2264.2
110600*                                                                 SQ2264.2
110700 SEQ-INIT-04.                                                     SQ2264.2
110800     MOVE    0 TO REC-CT.                                         SQ2264.2
110900     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
111000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2264.2
111100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
111200     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
111300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
111400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
111500     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
111600     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2264.2
111700     MOVE   "OPEN FILE FOR INPUT"  TO FEATURE.                    SQ2264.2
111800     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ2264.2
111900     IF DELETE-SW NOT = SPACE                                     SQ2264.2
112000         GO TO SEQ-DELETE-04-01.                                  SQ2264.2
112100     GO TO   SEQ-TEST-OP-04.                                      SQ2264.2
112200 SEQ-DELETE-04.                                                   SQ2264.2
112300     MOVE   "*" TO DELETE-SW-2.                                   SQ2264.2
112400     GO TO   SEQ-DELETE-04-01.                                    SQ2264.2
112500 SEQ-TEST-OP-04.                                                  SQ2264.2
112600     OPEN    INPUT SQ-FS4.                                        SQ2264.2
112700*                                                                 SQ2264.2
112800*    CHECK I-O STATUS RETURNED FROM OPEN INPUT                    SQ2264.2
112900*                                                                 SQ2264.2
113000     ADD    1 TO REC-CT.                                          SQ2264.2
113100     IF DELETE-SW NOT = SPACE                                     SQ2264.2
113200         GO TO   SEQ-DELETE-04-01.                                SQ2264.2
113300     GO TO   SEQ-TEST-OP-04-01.                                   SQ2264.2
113400 SEQ-DELETE-04-01.                                                SQ2264.2
113500     PERFORM DE-LETE.                                             SQ2264.2
113600     GO TO   SEQ-TEST-04-01-END.                                  SQ2264.2
113700 SEQ-TEST-OP-04-01.                                               SQ2264.2
113800     IF SQ-FS4-STATUS = "00"                                      SQ2264.2
113900         PERFORM PASS                                             SQ2264.2
114000     ELSE                                                         SQ2264.2
114100         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
114200         MOVE   "00" TO CORRECT-A                                 SQ2264.2
114300         MOVE   "UNEXPECTED ERROR CODE FROM OPEN INPUT"           SQ2264.2
114400                   TO RE-MARK                                     SQ2264.2
114500         MOVE   "VII-3, VII-40" TO ANSI-REFERENCE                 SQ2264.2
114600         PERFORM FAIL.                                            SQ2264.2
114700 SEQ-TEST-04-01-END.                                              SQ2264.2
114800*                                                                 SQ2264.2
114900*    CHECK EXECUTION OF EXTEND DECLARATIVE                        SQ2264.2
115000*                                                                 SQ2264.2
115100     ADD     1 TO REC-CT.                                         SQ2264.2
115200     IF DELETE-SW NOT = SPACE                                     SQ2264.2
115300         GO TO   SEQ-DELETE-04-02.                                SQ2264.2
115400     GO TO   SEQ-TEST-OP-04-02.                                   SQ2264.2
115500 SEQ-DELETE-04-02.                                                SQ2264.2
115600     PERFORM DE-LETE.                                             SQ2264.2
115700     GO TO   SEQ-TEST-04-02-END.                                  SQ2264.2
115800 SEQ-TEST-OP-04-02.                                               SQ2264.2
115900     IF DECL-EXEC-E = "NOT EXECUTED"                              SQ2264.2
116000         PERFORM PASS                                             SQ2264.2
116100     ELSE                                                         SQ2264.2
116200         MOVE    DECL-EXEC-E TO COMPUTED-A                        SQ2264.2
116300         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
116400         MOVE   "UNEXPECTED EXECUTION OF EXTEND DECLARATIVE"      SQ2264.2
116500                   TO RE-MARK                                     SQ2264.2
116600         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
116700         PERFORM FAIL.                                            SQ2264.2
116800 SEQ-TEST-04-02-END.                                              SQ2264.2
116900*                                                                 SQ2264.2
117000*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ2264.2
117100*                                                                 SQ2264.2
117200     ADD     1 TO REC-CT.                                         SQ2264.2
117300     IF DELETE-SW NOT = SPACE                                     SQ2264.2
117400         GO TO   SEQ-DELETE-04-03.                                SQ2264.2
117500     GO TO   SEQ-TEST-OP-04-03.                                   SQ2264.2
117600 SEQ-DELETE-04-03.                                                SQ2264.2
117700     PERFORM DE-LETE.                                             SQ2264.2
117800     GO TO   SEQ-TEST-04-03-END.                                  SQ2264.2
117900 SEQ-TEST-OP-04-03.                                               SQ2264.2
118000     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ2264.2
118100         PERFORM PASS                                             SQ2264.2
118200     ELSE                                                         SQ2264.2
118300         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ2264.2
118400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
118500         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
118600                   TO RE-MARK                                     SQ2264.2
118700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
118800         PERFORM FAIL.                                            SQ2264.2
118900 SEQ-TEST-04-03-END.                                              SQ2264.2
119000*                                                                 SQ2264.2
119100*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
119200*                                                                 SQ2264.2
119300     ADD     1 TO REC-CT.                                         SQ2264.2
119400     IF DELETE-SW NOT = SPACE                                     SQ2264.2
119500         GO TO   SEQ-DELETE-04-04.                                SQ2264.2
119600     GO TO   SEQ-TEST-OP-04-04.                                   SQ2264.2
119700 SEQ-DELETE-04-04.                                                SQ2264.2
119800     PERFORM DE-LETE.                                             SQ2264.2
119900     GO TO   SEQ-TEST-04-04-END.                                  SQ2264.2
120000 SEQ-TEST-OP-04-04.                                               SQ2264.2
120100     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
120200         PERFORM PASS                                             SQ2264.2
120300     ELSE                                                         SQ2264.2
120400         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
120500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
120600         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
120700                   TO RE-MARK                                     SQ2264.2
120800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
120900         PERFORM FAIL.                                            SQ2264.2
121000 SEQ-TEST-04-04-END.                                              SQ2264.2
121100*                                                                 SQ2264.2
121200*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
121300*                                                                 SQ2264.2
121400     ADD     1 TO REC-CT.                                         SQ2264.2
121500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
121600         GO TO   SEQ-DELETE-04-05.                                SQ2264.2
121700     GO TO   SEQ-TEST-OP-04-05.                                   SQ2264.2
121800 SEQ-DELETE-04-05.                                                SQ2264.2
121900     PERFORM DE-LETE.                                             SQ2264.2
122000     GO TO   SEQ-TEST-04-05-END.                                  SQ2264.2
122100 SEQ-TEST-OP-04-05.                                               SQ2264.2
122200     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
122300         PERFORM PASS                                             SQ2264.2
122400     ELSE                                                         SQ2264.2
122500         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
122600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
122700         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
122800                   TO RE-MARK                                     SQ2264.2
122900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
123000         PERFORM FAIL.                                            SQ2264.2
123100 SEQ-TEST-04-05-END.                                              SQ2264.2
123200*                                                                 SQ2264.2
123300*                                                                 SQ2264.2
123400*    THE FILE IS OPEN FOR INPUT.  WE READ THE ONLY RECORD.        SQ2264.2
123500*                                                                 SQ2264.2
123600 SEQ-INIT-05.                                                     SQ2264.2
123700     MOVE    0 TO REC-CT.                                         SQ2264.2
123800     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
123900     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2264.2
124000     ADD     1 TO XRECORD-NUMBER (1).                             SQ2264.2
124100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
124200     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
124300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
124400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
124500     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
124600     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ2264.2
124700     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ2264.2
124800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2264.2
124900         GO TO SEQ-DELETE-05.                                     SQ2264.2
125000     GO TO   SEQ-TEST-RD-05.                                      SQ2264.2
125100 SEQ-DELETE-05.                                                   SQ2264.2
125200     MOVE   "*" TO DELETE-SW-2.                                   SQ2264.2
125300     GO TO   SEQ-DELETE-05-01.                                    SQ2264.2
125400 SEQ-TEST-RD-05.                                                  SQ2264.2
125500     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ2264.2
125600     READ    SQ-FS4.                                              SQ2264.2
125700*                                                                 SQ2264.2
125800*    CHECK I-O STATUS RETURNED FROM READ                          SQ2264.2
125900*                                                                 SQ2264.2
126000     ADD    1 TO REC-CT.                                          SQ2264.2
126100     IF DELETE-SW NOT = SPACE                                     SQ2264.2
126200         GO TO   SEQ-DELETE-05-01.                                SQ2264.2
126300     GO TO   SEQ-TEST-RD-05-01.                                   SQ2264.2
126400 SEQ-DELETE-05-01.                                                SQ2264.2
126500     PERFORM DE-LETE.                                             SQ2264.2
126600     GO TO   SEQ-TEST-05-01-END.                                  SQ2264.2
126700 SEQ-TEST-RD-05-01.                                               SQ2264.2
126800     IF SQ-FS4-STATUS = "00"                                      SQ2264.2
126900         PERFORM PASS                                             SQ2264.2
127000     ELSE                                                         SQ2264.2
127100         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
127200         MOVE   "00" TO CORRECT-A                                 SQ2264.2
127300         MOVE   "UNEXPECTED STATUS CODE FROM READ"                SQ2264.2
127400                   TO RE-MARK                                     SQ2264.2
127500         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ2264.2
127600         PERFORM FAIL.                                            SQ2264.2
127700 SEQ-TEST-05-01-END.                                              SQ2264.2
127800*                                                                 SQ2264.2
127900*    CHECK EXECUTION OF EXTEND DECLARATIVE                        SQ2264.2
128000*                                                                 SQ2264.2
128100     ADD     1 TO REC-CT.                                         SQ2264.2
128200     IF DELETE-SW NOT = SPACE                                     SQ2264.2
128300         GO TO   SEQ-DELETE-05-02.                                SQ2264.2
128400     GO TO   SEQ-TEST-RD-05-02.                                   SQ2264.2
128500 SEQ-DELETE-05-02.                                                SQ2264.2
128600     PERFORM DE-LETE.                                             SQ2264.2
128700     GO TO   SEQ-TEST-05-02-END.                                  SQ2264.2
128800 SEQ-TEST-RD-05-02.                                               SQ2264.2
128900     IF DECL-EXEC-E = "NOT EXECUTED"                              SQ2264.2
129000         PERFORM PASS                                             SQ2264.2
129100     ELSE                                                         SQ2264.2
129200         MOVE    DECL-EXEC-E TO COMPUTED-A                        SQ2264.2
129300         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
129400         MOVE   "UNEXPECTED EXECUTION OF EXTEND DECLARATIVE"      SQ2264.2
129500                   TO RE-MARK                                     SQ2264.2
129600         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
129700         PERFORM FAIL.                                            SQ2264.2
129800 SEQ-TEST-05-02-END.                                              SQ2264.2
129900*                                                                 SQ2264.2
130000*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ2264.2
130100*                                                                 SQ2264.2
130200     ADD     1 TO REC-CT.                                         SQ2264.2
130300     IF DELETE-SW NOT = SPACE                                     SQ2264.2
130400         GO TO   SEQ-DELETE-05-03.                                SQ2264.2
130500     GO TO   SEQ-TEST-RD-05-03.                                   SQ2264.2
130600 SEQ-DELETE-05-03.                                                SQ2264.2
130700     PERFORM DE-LETE.                                             SQ2264.2
130800     GO TO   SEQ-TEST-05-03-END.                                  SQ2264.2
130900 SEQ-TEST-RD-05-03.                                               SQ2264.2
131000     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ2264.2
131100         PERFORM PASS                                             SQ2264.2
131200     ELSE                                                         SQ2264.2
131300         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ2264.2
131400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
131500         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
131600                   TO RE-MARK                                     SQ2264.2
131700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
131800         PERFORM FAIL.                                            SQ2264.2
131900 SEQ-TEST-05-03-END.                                              SQ2264.2
132000*                                                                 SQ2264.2
132100*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
132200*                                                                 SQ2264.2
132300     ADD     1 TO REC-CT.                                         SQ2264.2
132400     IF DELETE-SW NOT = SPACE                                     SQ2264.2
132500         GO TO   SEQ-DELETE-05-04.                                SQ2264.2
132600     GO TO   SEQ-TEST-RD-05-04.                                   SQ2264.2
132700 SEQ-DELETE-05-04.                                                SQ2264.2
132800     PERFORM DE-LETE.                                             SQ2264.2
132900     GO TO   SEQ-TEST-05-04-END.                                  SQ2264.2
133000 SEQ-TEST-RD-05-04.                                               SQ2264.2
133100     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
133200         PERFORM PASS                                             SQ2264.2
133300     ELSE                                                         SQ2264.2
133400         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
133500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
133600         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
133700                   TO RE-MARK                                     SQ2264.2
133800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
133900         PERFORM FAIL.                                            SQ2264.2
134000 SEQ-TEST-05-04-END.                                              SQ2264.2
134100*                                                                 SQ2264.2
134200*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
134300*                                                                 SQ2264.2
134400     ADD     1 TO REC-CT.                                         SQ2264.2
134500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
134600         GO TO   SEQ-DELETE-05-05.                                SQ2264.2
134700     GO TO   SEQ-TEST-RD-05-05.                                   SQ2264.2
134800 SEQ-DELETE-05-05.                                                SQ2264.2
134900     PERFORM DE-LETE.                                             SQ2264.2
135000     GO TO   SEQ-TEST-05-05-END.                                  SQ2264.2
135100 SEQ-TEST-RD-05-05.                                               SQ2264.2
135200     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
135300         PERFORM PASS                                             SQ2264.2
135400     ELSE                                                         SQ2264.2
135500         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
135600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
135700         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
135800                   TO RE-MARK                                     SQ2264.2
135900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
136000         PERFORM FAIL.                                            SQ2264.2
136100 SEQ-TEST-05-05-END.                                              SQ2264.2
136200*                                                                 SQ2264.2
136300*    CHECK THAT THE CORRECT RECORD HAS BEEN RETURNED, BY          SQ2264.2
136400*    CHECKING THE RECORD-NUMBER FIELD.                            SQ2264.2
136500*                                                                 SQ2264.2
136600     ADD     1 TO REC-CT.                                         SQ2264.2
136700     IF DELETE-SW NOT = SPACE                                     SQ2264.2
136800         GO TO   SEQ-DELETE-05-06.                                SQ2264.2
136900     GO TO   SEQ-TEST-RD-05-06.                                   SQ2264.2
137000 SEQ-DELETE-05-06.                                                SQ2264.2
137100     PERFORM DE-LETE.                                             SQ2264.2
137200     GO TO   SEQ-TEST-05-06-END.                                  SQ2264.2
137300 SEQ-TEST-RD-05-06.                                               SQ2264.2
137400     IF FRECORD-NUMBER = XRECORD-NUMBER (1)                       SQ2264.2
137500         PERFORM PASS                                             SQ2264.2
137600     ELSE                                                         SQ2264.2
137700         MOVE    FRECORD-NUMBER TO COMPUTED-18V0                  SQ2264.2
137800         MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0               SQ2264.2
137900         MOVE   "INCORRECT RECORD NUMBER FOUND" TO RE-MARK        SQ2264.2
138000         PERFORM FAIL.                                            SQ2264.2
138100 SEQ-TEST-05-06-END.                                              SQ2264.2
138200     MOVE    SPACE TO DELETE-SW-2.                                SQ2264.2
138300*                                                                 SQ2264.2
138400*                                                                 SQ2264.2
138500*    ANOTHER READ SHOULD CAUSE THE AT END CONDITION.              SQ2264.2
138600*                                                                 SQ2264.2
138700 SEQ-INIT-06.                                                     SQ2264.2
138800     MOVE    0 TO REC-CT.                                         SQ2264.2
138900     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
139000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ2264.2
139100     ADD     1 TO XRECORD-NUMBER (1).                             SQ2264.2
139200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
139300     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
139400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
139500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
139600     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
139700     MOVE   "READ GIVING AT END" TO FEATURE.                      SQ2264.2
139800     MOVE   "SEQ-TEST-RD-06" TO PAR-NAME.                         SQ2264.2
139900     IF DELETE-SW NOT EQUAL TO SPACE                              SQ2264.2
140000         GO TO SEQ-DELETE-06.                                     SQ2264.2
140100     GO TO   SEQ-TEST-RD-06.                                      SQ2264.2
140200 SEQ-DELETE-06.                                                   SQ2264.2
140300     MOVE   "*" TO DELETE-SW-2.                                   SQ2264.2
140400     GO TO   SEQ-DELETE-06-01.                                    SQ2264.2
140500 SEQ-TEST-RD-06.                                                  SQ2264.2
140600     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ2264.2
140700     READ    SQ-FS4 RECORD.                                       SQ2264.2
140800*                                                                 SQ2264.2
140900*    CHECK I-O STATUS RETURNED FROM READ                          SQ2264.2
141000*                                                                 SQ2264.2
141100     ADD    1 TO REC-CT.                                          SQ2264.2
141200     IF DELETE-SW NOT = SPACE                                     SQ2264.2
141300         GO TO   SEQ-DELETE-06-01.                                SQ2264.2
141400     GO TO   SEQ-TEST-RD-06-01.                                   SQ2264.2
141500 SEQ-DELETE-06-01.                                                SQ2264.2
141600     PERFORM DE-LETE.                                             SQ2264.2
141700     GO TO   SEQ-TEST-06-01-END.                                  SQ2264.2
141800 SEQ-TEST-RD-06-01.                                               SQ2264.2
141900     IF SQ-FS4-STATUS = "10"                                      SQ2264.2
142000         PERFORM PASS                                             SQ2264.2
142100     ELSE                                                         SQ2264.2
142200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
142300         MOVE   "10" TO CORRECT-A                                 SQ2264.2
142400         MOVE   "AT END STATUS NOT RETURNED FROM READ"            SQ2264.2
142500                   TO RE-MARK                                     SQ2264.2
142600         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ2264.2
142700         PERFORM FAIL.                                            SQ2264.2
142800 SEQ-TEST-06-01-END.                                              SQ2264.2
142900*                                                                 SQ2264.2
143000*    CHECK EXECUTION OF EXTEND DECLARATIVE                        SQ2264.2
143100*                                                                 SQ2264.2
143200     ADD     1 TO REC-CT.                                         SQ2264.2
143300     IF DELETE-SW NOT = SPACE                                     SQ2264.2
143400         GO TO   SEQ-DELETE-06-02.                                SQ2264.2
143500     GO TO   SEQ-TEST-RD-06-02.                                   SQ2264.2
143600 SEQ-DELETE-06-02.                                                SQ2264.2
143700     PERFORM DE-LETE.                                             SQ2264.2
143800     GO TO   SEQ-TEST-06-02-END.                                  SQ2264.2
143900 SEQ-TEST-RD-06-02.                                               SQ2264.2
144000     IF DECL-EXEC-E = "NOT EXECUTED"                              SQ2264.2
144100         PERFORM PASS                                             SQ2264.2
144200     ELSE                                                         SQ2264.2
144300         MOVE    DECL-EXEC-E TO COMPUTED-A                        SQ2264.2
144400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
144500         MOVE   "UNEXPECTED EXECUTION OF EXTEND DECLARATIVE"      SQ2264.2
144600                   TO RE-MARK                                     SQ2264.2
144700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
144800         PERFORM FAIL.                                            SQ2264.2
144900 SEQ-TEST-06-02-END.                                              SQ2264.2
145000*                                                                 SQ2264.2
145100*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ2264.2
145200*                                                                 SQ2264.2
145300     ADD     1 TO REC-CT.                                         SQ2264.2
145400     IF DELETE-SW NOT = SPACE                                     SQ2264.2
145500         GO TO   SEQ-DELETE-06-03.                                SQ2264.2
145600     GO TO   SEQ-TEST-RD-06-03.                                   SQ2264.2
145700 SEQ-DELETE-06-03.                                                SQ2264.2
145800     PERFORM DE-LETE.                                             SQ2264.2
145900     GO TO   SEQ-TEST-06-03-END.                                  SQ2264.2
146000 SEQ-TEST-RD-06-03.                                               SQ2264.2
146100     IF DECL-EXEC-I = "EXECUTED"                                  SQ2264.2
146200         PERFORM PASS                                             SQ2264.2
146300     ELSE                                                         SQ2264.2
146400         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ2264.2
146500         MOVE   "EXECUTED" TO CORRECT-A                           SQ2264.2
146600         MOVE   "INPUT DECLARATIVE NOT EXECUTED AT END OF FILE"   SQ2264.2
146700                   TO RE-MARK                                     SQ2264.2
146800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
146900         PERFORM FAIL.                                            SQ2264.2
147000 SEQ-TEST-06-03-END.                                              SQ2264.2
147100*                                                                 SQ2264.2
147200*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
147300*                                                                 SQ2264.2
147400     ADD     1 TO REC-CT.                                         SQ2264.2
147500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
147600         GO TO   SEQ-DELETE-06-04.                                SQ2264.2
147700     GO TO   SEQ-TEST-RD-06-04.                                   SQ2264.2
147800 SEQ-DELETE-06-04.                                                SQ2264.2
147900     PERFORM DE-LETE.                                             SQ2264.2
148000     GO TO   SEQ-TEST-06-04-END.                                  SQ2264.2
148100 SEQ-TEST-RD-06-04.                                               SQ2264.2
148200     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
148300         PERFORM PASS                                             SQ2264.2
148400     ELSE                                                         SQ2264.2
148500         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
148600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
148700         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
148800                   TO RE-MARK                                     SQ2264.2
148900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
149000         PERFORM FAIL.                                            SQ2264.2
149100 SEQ-TEST-06-04-END.                                              SQ2264.2
149200*                                                                 SQ2264.2
149300*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
149400*                                                                 SQ2264.2
149500     ADD     1 TO REC-CT.                                         SQ2264.2
149600     IF DELETE-SW NOT = SPACE                                     SQ2264.2
149700         GO TO   SEQ-DELETE-06-05.                                SQ2264.2
149800     GO TO   SEQ-TEST-RD-06-05.                                   SQ2264.2
149900 SEQ-DELETE-06-05.                                                SQ2264.2
150000     PERFORM DE-LETE.                                             SQ2264.2
150100     GO TO   SEQ-TEST-06-05-END.                                  SQ2264.2
150200 SEQ-TEST-RD-06-05.                                               SQ2264.2
150300     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
150400         PERFORM PASS                                             SQ2264.2
150500     ELSE                                                         SQ2264.2
150600         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
150700         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
150800         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
150900                   TO RE-MARK                                     SQ2264.2
151000         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
151100         PERFORM FAIL.                                            SQ2264.2
151200 SEQ-TEST-06-05-END.                                              SQ2264.2
151300     MOVE    SPACE TO DELETE-SW-2.                                SQ2264.2
151400*                                                                 SQ2264.2
151500*                                                                 SQ2264.2
151600*    FINALLY, TRY TO OPEN THE FILE AGAIN, IN THE EXTEND MODE      SQ2264.2
151700*                                                                 SQ2264.2
151800 SEQ-INIT-07.                                                     SQ2264.2
151900     MOVE    0 TO REC-CT.                                         SQ2264.2
152000     MOVE    0 TO DECL-EXEC-CT.                                   SQ2264.2
152100     MOVE    SPACE TO DECL-EXEC-SW.                               SQ2264.2
152200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ2264.2
152300     MOVE   "NOT EXECUTED" TO DECL-EXEC-E.                        SQ2264.2
152400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ2264.2
152500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ2264.2
152600     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ2264.2
152700     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ2264.2
152800     MOVE   "OPEN FILE SECOND TIME"  TO FEATURE.                  SQ2264.2
152900     MOVE   "SEQ-TEST-OP-07" TO PAR-NAME.                         SQ2264.2
153000     IF DELETE-SW NOT = SPACE                                     SQ2264.2
153100         GO TO SEQ-DELETE-07-01.                                  SQ2264.2
153200     GO TO   SEQ-TEST-OP-07.                                      SQ2264.2
153300 SEQ-DELETE-07.                                                   SQ2264.2
153400     MOVE   "*" TO DELETE-SW-2.                                   SQ2264.2
153500     GO TO   SEQ-DELETE-07-01.                                    SQ2264.2
153600 SEQ-TEST-OP-07.                                                  SQ2264.2
153700     OPEN    EXTEND SQ-FS4.                                       SQ2264.2
153800     MOVE    0 TO REC-CT.                                         SQ2264.2
153900     MOVE   "OPEN FILE SECOND TIME"  TO FEATURE.                  SQ2264.2
154000     MOVE   "SEQ-TEST-OP-07" TO PAR-NAME.                         SQ2264.2
154100*                                                                 SQ2264.2
154200*    CHECK I-O STATUS RETURNED FROM OPEN EXTEND                   SQ2264.2
154300*                                                                 SQ2264.2
154400     ADD    1 TO REC-CT.                                          SQ2264.2
154500     IF DELETE-SW NOT = SPACE                                     SQ2264.2
154600         GO TO   SEQ-DELETE-07-01.                                SQ2264.2
154700     GO TO   SEQ-TEST-OP-07-01.                                   SQ2264.2
154800 SEQ-DELETE-07-01.                                                SQ2264.2
154900     PERFORM DE-LETE.                                             SQ2264.2
155000     GO TO   SEQ-TEST-07-01-END.                                  SQ2264.2
155100 SEQ-TEST-OP-07-01.                                               SQ2264.2
155200     IF SQ-FS4-STATUS = "41"                                      SQ2264.2
155300         PERFORM PASS                                             SQ2264.2
155400     ELSE                                                         SQ2264.2
155500         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ2264.2
155600         MOVE   "41" TO CORRECT-A                                 SQ2264.2
155700         MOVE   "UNEXPECTED STATUS CODE FROM SECOND OPEN"         SQ2264.2
155800                   TO RE-MARK                                     SQ2264.2
155900         MOVE   "VII-4,1.5.3(4)A, VII-40" TO ANSI-REFERENCE       SQ2264.2
156000         PERFORM FAIL.                                            SQ2264.2
156100 SEQ-TEST-07-01-END.                                              SQ2264.2
156200*                                                                 SQ2264.2
156300*    CHECK EXECUTION OF EXTEND AND INPUT DECLARATIVES             SQ2264.2
156400*                                                                 SQ2264.2
156500     ADD     1 TO REC-CT.                                         SQ2264.2
156600     IF DELETE-SW NOT = SPACE                                     SQ2264.2
156700         GO TO   SEQ-DELETE-07-02.                                SQ2264.2
156800     GO TO   SEQ-TEST-OP-07-02.                                   SQ2264.2
156900 SEQ-DELETE-07-02.                                                SQ2264.2
157000     PERFORM DE-LETE.                                             SQ2264.2
157100     GO TO   SEQ-TEST-07-02-END.                                  SQ2264.2
157200 SEQ-TEST-OP-07-02.                                               SQ2264.2
157300     IF DECL-EXEC-E = "EXECUTED" OR DECL-EXEC-I = "EXECUTED"      SQ2264.2
157400         PERFORM PASS                                             SQ2264.2
157500     ELSE                                                         SQ2264.2
157600         MOVE   "DECL NOT EXECUTED" TO COMPUTED-A                 SQ2264.2
157700         MOVE   "EXECUTED" TO CORRECT-A                           SQ2264.2
157800         MOVE   "EXECUTION OF ONE DECLARATIVE EXPECTED"           SQ2264.2
157900                   TO RE-MARK                                     SQ2264.2
158000         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
158100         PERFORM FAIL.                                            SQ2264.2
158200 SEQ-TEST-07-02-END.                                              SQ2264.2
158300*                                                                 SQ2264.2
158400*    CHECK NUMBER OF DECLARATIVES EXECUTED                        SQ2264.2
158500*                                                                 SQ2264.2
158600     ADD     1 TO REC-CT.                                         SQ2264.2
158700     IF DELETE-SW NOT = SPACE                                     SQ2264.2
158800         GO TO   SEQ-DELETE-07-03.                                SQ2264.2
158900     GO TO   SEQ-TEST-OP-07-03.                                   SQ2264.2
159000 SEQ-DELETE-07-03.                                                SQ2264.2
159100     PERFORM DE-LETE.                                             SQ2264.2
159200     GO TO   SEQ-TEST-07-03-END.                                  SQ2264.2
159300 SEQ-TEST-OP-07-03.                                               SQ2264.2
159400     IF DECL-EXEC-CT = 1                                          SQ2264.2
159500         PERFORM PASS                                             SQ2264.2
159600     ELSE                                                         SQ2264.2
159700         MOVE    DECL-EXEC-CT  TO COMPUTED-18V0                   SQ2264.2
159800         MOVE    1 TO CORRECT-18V0                                SQ2264.2
159900         MOVE   "ONLY ONE EXECUTION OF A DECLARATIVE EXPECTED"    SQ2264.2
160000                   TO RE-MARK                                     SQ2264.2
160100         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
160200         PERFORM FAIL.                                            SQ2264.2
160300 SEQ-TEST-07-03-END.                                              SQ2264.2
160400*                                                                 SQ2264.2
160500*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ2264.2
160600*                                                                 SQ2264.2
160700     ADD     1 TO REC-CT.                                         SQ2264.2
160800     IF DELETE-SW NOT = SPACE                                     SQ2264.2
160900         GO TO   SEQ-DELETE-07-04.                                SQ2264.2
161000     GO TO   SEQ-TEST-OP-07-04.                                   SQ2264.2
161100 SEQ-DELETE-07-04.                                                SQ2264.2
161200     PERFORM DE-LETE.                                             SQ2264.2
161300     GO TO   SEQ-TEST-07-04-END.                                  SQ2264.2
161400 SEQ-TEST-OP-07-04.                                               SQ2264.2
161500     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ2264.2
161600         PERFORM PASS                                             SQ2264.2
161700     ELSE                                                         SQ2264.2
161800         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ2264.2
161900         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
162000         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ2264.2
162100                   TO RE-MARK                                     SQ2264.2
162200         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
162300         PERFORM FAIL.                                            SQ2264.2
162400 SEQ-TEST-07-04-END.                                              SQ2264.2
162500*                                                                 SQ2264.2
162600*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ2264.2
162700*                                                                 SQ2264.2
162800     ADD     1 TO REC-CT.                                         SQ2264.2
162900     IF DELETE-SW NOT = SPACE                                     SQ2264.2
163000         GO TO   SEQ-DELETE-07-05.                                SQ2264.2
163100     GO TO   SEQ-TEST-OP-07-05.                                   SQ2264.2
163200 SEQ-DELETE-07-05.                                                SQ2264.2
163300     PERFORM DE-LETE.                                             SQ2264.2
163400     GO TO   SEQ-TEST-07-05-END.                                  SQ2264.2
163500 SEQ-TEST-OP-07-05.                                               SQ2264.2
163600     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ2264.2
163700         PERFORM PASS                                             SQ2264.2
163800     ELSE                                                         SQ2264.2
163900         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ2264.2
164000         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ2264.2
164100         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ2264.2
164200                   TO RE-MARK                                     SQ2264.2
164300         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ2264.2
164400         PERFORM FAIL.                                            SQ2264.2
164500 SEQ-TEST-07-05-END.                                              SQ2264.2
164600*                                                                 SQ2264.2
164700*                                                                 SQ2264.2
164800 CCVS-EXIT SECTION.                                               SQ2264.2
164900 CCVS-999999.                                                     SQ2264.2
165000     GO TO   CLOSE-FILES.                                         SQ2264.2
      *END-OF,SQ226A                                                                  
