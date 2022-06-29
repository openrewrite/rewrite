      *HEADER,COBOL,SQ133A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1334.2
000200 PROGRAM-ID.                                                      SQ1334.2
000300     SQ133A.                                                      SQ1334.2
000400****************************************************************  SQ1334.2
000500*                                                              *  SQ1334.2
000600*    VALIDATION FOR:-                                          *  SQ1334.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1334.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1334.2
000900*                             REVISED 1986, AUGUST             *  SQ1334.2
001000*                                                              *  SQ1334.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1334.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1334.2
001300*                                                              *  SQ1334.2
001400****************************************************************  SQ1334.2
001500*                                                              *  SQ1334.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1334.2
001700*                                                              *  SQ1334.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1334.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1334.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1334.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1334.2
002200*                                                              *  SQ1334.2
002300****************************************************************  SQ1334.2
002400*                                                              *  SQ1334.2
002500*    THIS PROGRAM OPENS FOR OUTPUT A FILE WHICH IS ASSIGNED    *  SQ1334.2
002600*    TO A MASS STORAGE MEDIUM, WRITES ONE RECORD AND CLOSES    *  SQ1334.2
002700*    THE FILE.  THE FILE IS THEN OPENED FOR I-O, AND TWO READ  *  SQ1334.2
002800*    STATEMENTS EXECUTED.  THE SECOND SHOULD CAUSE AN AT END   *  SQ1334.2
002900*    CONDITION, AND THUS BE UNSUCCESSFUL.  A REWRITE STATEMENT *  SQ1334.2
003000*    IS THEN EXECUTED.  THIS SHOULD CAUSE AN EXCEPTION         *  SQ1334.2
003100*    CONDITION, WITH I-O STATUS "43" AND ENTRY TO THE          *  SQ1334.2
003200*    APPLICABLE ERROR DECLARATIVE.                             *  SQ1334.2
003300*                                                              *  SQ1334.2
003400*    THIS PROGRAM HAS BEEN SPLIT FROM V2.0 ONWARDS.            *  SQ1334.2
003500*    THE NEW PROGRAM IS SQ144A.                                *  SQ1334.2
003600****************************************************************  SQ1334.2
003700*                                                                 SQ1334.2
003800 ENVIRONMENT DIVISION.                                            SQ1334.2
003900 CONFIGURATION SECTION.                                           SQ1334.2
004000 SOURCE-COMPUTER.                                                 SQ1334.2
004100     XXXXX082.                                                    SQ1334.2
004200 OBJECT-COMPUTER.                                                 SQ1334.2
004300     XXXXX083.                                                    SQ1334.2
004400*                                                                 SQ1334.2
004500 INPUT-OUTPUT SECTION.                                            SQ1334.2
004600 FILE-CONTROL.                                                    SQ1334.2
004700     SELECT PRINT-FILE ASSIGN TO                                  SQ1334.2
004800     XXXXX055.                                                    SQ1334.2
004900*                                                                 SQ1334.2
005000*    SELECT RAW-DATA   ASSIGN TO                                  SQ1334.2
005100*    XXXXX062                                                     SQ1334.2
005200*          ORGANIZATION IS INDEXED                                SQ1334.2
005300*          ACCESS MODE  IS RANDOM                                 SQ1334.2
005400*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1334.2
005500*                                                                 SQ1334.2
005600     SELECT SQ-FS4                                                SQ1334.2
005700            ASSIGN                                                SQ1334.2
005800     XXXXX014                                                     SQ1334.2
005900            STATUS SQ-FS4-STATUS OF STATUS-GROUP                  SQ1334.2
006000            SEQUENTIAL                                            SQ1334.2
006100            .                                                     SQ1334.2
006200*                                                                 SQ1334.2
006300*                                                                 SQ1334.2
006400 DATA DIVISION.                                                   SQ1334.2
006500 FILE SECTION.                                                    SQ1334.2
006600 FD  PRINT-FILE                                                   SQ1334.2
006700C    LABEL RECORDS                                                SQ1334.2
006800C    XXXXX084                                                     SQ1334.2
006900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1334.2
007000               .                                                  SQ1334.2
007100 01  PRINT-REC    PICTURE X(120).                                 SQ1334.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1334.2
007300*                                                                 SQ1334.2
007400*FD  RAW-DATA.                                                    SQ1334.2
007500*01  RAW-DATA-SATZ.                                               SQ1334.2
007600*    05  RAW-DATA-KEY        PIC X(6).                            SQ1334.2
007700*    05  C-DATE              PIC 9(6).                            SQ1334.2
007800*    05  C-TIME              PIC 9(8).                            SQ1334.2
007900*    05  NO-OF-TESTS         PIC 99.                              SQ1334.2
008000*    05  C-OK                PIC 999.                             SQ1334.2
008100*    05  C-ALL               PIC 999.                             SQ1334.2
008200*    05  C-FAIL              PIC 999.                             SQ1334.2
008300*    05  C-DELETED           PIC 999.                             SQ1334.2
008400*    05  C-INSPECT           PIC 999.                             SQ1334.2
008500*    05  C-NOTE              PIC X(13).                           SQ1334.2
008600*    05  C-INDENT            PIC X.                               SQ1334.2
008700*    05  C-ABORT             PIC X(8).                            SQ1334.2
008800*                                                                 SQ1334.2
008900 FD  SQ-FS4                                                       SQ1334.2
009000C    LABEL RECORD IS STANDARD                                     SQ1334.2
009100     BLOCK  120 CHARACTERS                                        SQ1334.2
009200     RECORD CONTAINS 120 CHARACTERS                               SQ1334.2
009300                .                                                 SQ1334.2
009400 01  SQ-FS4R1-F-G-120.                                            SQ1334.2
009500        05 FFILE-RECORD-INFO-P1-120.                              SQ1334.2
009600           07 FILLER              PIC X(5).                       SQ1334.2
009700           07 FFILE-NAME          PIC X(6).                       SQ1334.2
009800           07 FILLER              PIC X(8).                       SQ1334.2
009900           07 FRECORD-NAME        PIC X(6).                       SQ1334.2
010000           07 FILLER              PIC X(1).                       SQ1334.2
010100           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ1334.2
010200           07 FILLER              PIC X(7).                       SQ1334.2
010300           07 FRECORD-NUMBER      PIC 9(6).                       SQ1334.2
010400           07 FILLER              PIC X(6).                       SQ1334.2
010500           07 FUPDATE-NUMBER      PIC 9(2).                       SQ1334.2
010600           07 FILLER              PIC X(5).                       SQ1334.2
010700           07 FODO-NUMBER         PIC 9(4).                       SQ1334.2
010800           07 FILLER              PIC X(5).                       SQ1334.2
010900           07 FPROGRAM-NAME       PIC X(5).                       SQ1334.2
011000           07 FILLER              PIC X(7).                       SQ1334.2
011100           07 FRECORD-LENGTH      PIC 9(6).                       SQ1334.2
011200           07 FILLER              PIC X(7).                       SQ1334.2
011300           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ1334.2
011400           07 FILLER              PIC X(1).                       SQ1334.2
011500           07 FBLOCK-SIZE         PIC 9(4).                       SQ1334.2
011600           07 FILLER              PIC X(6).                       SQ1334.2
011700           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ1334.2
011800           07 FILLER              PIC X(5).                       SQ1334.2
011900           07 FFILE-ORGANIZATION  PIC X(2).                       SQ1334.2
012000           07 FILLER              PIC X(6).                       SQ1334.2
012100           07 FLABEL-TYPE         PIC X(1).                       SQ1334.2
012200*                                                                 SQ1334.2
012300 WORKING-STORAGE SECTION.                                         SQ1334.2
012400*                                                                 SQ1334.2
012500***************************************************************   SQ1334.2
012600*                                                             *   SQ1334.2
012700*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1334.2
012800*                                                             *   SQ1334.2
012900***************************************************************   SQ1334.2
013000*                                                                 SQ1334.2
013100 01  STATUS-GROUP.                                                SQ1334.2
013200     04  SQ-FS4-STATUS.                                           SQ1334.2
013300         07  SQ-FS4-KEY-1   PIC X.                                SQ1334.2
013400         07  SQ-FS4-KEY-2   PIC X.                                SQ1334.2
013500*                                                                 SQ1334.2
013600 01  DELETE-SW.                                                   SQ1334.2
013700     03  DELETE-SW-1 PIC X.                                       SQ1334.2
013800     03  DELETE-SW-1-GROUP.                                       SQ1334.2
013900         05  DELETE-SW-2 PIC X.                                   SQ1334.2
014000*                                                                 SQ1334.2
014100 01  DECL-EXEC-I-O PIC X(12).                                     SQ1334.2
014200*                                                                 SQ1334.2
014300 01  DECL-EXEC-SW PIC X.                                          SQ1334.2
014400*                                                                 SQ1334.2
014500***************************************************************   SQ1334.2
014600*                                                             *   SQ1334.2
014700*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1334.2
014800*                                                             *   SQ1334.2
014900***************************************************************   SQ1334.2
015000*                                                                 SQ1334.2
015100 01  REC-SKEL-SUB   PIC 99.                                       SQ1334.2
015200*                                                                 SQ1334.2
015300 01  FILE-RECORD-INFORMATION-REC.                                 SQ1334.2
015400     03 FILE-RECORD-INFO-SKELETON.                                SQ1334.2
015500        05 FILLER                 PICTURE X(48)       VALUE       SQ1334.2
015600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1334.2
015700        05 FILLER                 PICTURE X(46)       VALUE       SQ1334.2
015800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1334.2
015900        05 FILLER                 PICTURE X(26)       VALUE       SQ1334.2
016000             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1334.2
016100        05 FILLER                 PICTURE X(37)       VALUE       SQ1334.2
016200             ",RECKEY=                             ".             SQ1334.2
016300        05 FILLER                 PICTURE X(38)       VALUE       SQ1334.2
016400             ",ALTKEY1=                             ".            SQ1334.2
016500        05 FILLER                 PICTURE X(38)       VALUE       SQ1334.2
016600             ",ALTKEY2=                             ".            SQ1334.2
016700        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1334.2
016800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1334.2
016900        05 FILE-RECORD-INFO-P1-120.                               SQ1334.2
017000           07 FILLER              PIC X(5).                       SQ1334.2
017100           07 XFILE-NAME          PIC X(6).                       SQ1334.2
017200           07 FILLER              PIC X(8).                       SQ1334.2
017300           07 XRECORD-NAME        PIC X(6).                       SQ1334.2
017400           07 FILLER              PIC X(1).                       SQ1334.2
017500           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1334.2
017600           07 FILLER              PIC X(7).                       SQ1334.2
017700           07 XRECORD-NUMBER      PIC 9(6).                       SQ1334.2
017800           07 FILLER              PIC X(6).                       SQ1334.2
017900           07 UPDATE-NUMBER       PIC 9(2).                       SQ1334.2
018000           07 FILLER              PIC X(5).                       SQ1334.2
018100           07 ODO-NUMBER          PIC 9(4).                       SQ1334.2
018200           07 FILLER              PIC X(5).                       SQ1334.2
018300           07 XPROGRAM-NAME       PIC X(5).                       SQ1334.2
018400           07 FILLER              PIC X(7).                       SQ1334.2
018500           07 XRECORD-LENGTH      PIC 9(6).                       SQ1334.2
018600           07 FILLER              PIC X(7).                       SQ1334.2
018700           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1334.2
018800           07 FILLER              PIC X(1).                       SQ1334.2
018900           07 XBLOCK-SIZE         PIC 9(4).                       SQ1334.2
019000           07 FILLER              PIC X(6).                       SQ1334.2
019100           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1334.2
019200           07 FILLER              PIC X(5).                       SQ1334.2
019300           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1334.2
019400           07 FILLER              PIC X(6).                       SQ1334.2
019500           07 XLABEL-TYPE         PIC X(1).                       SQ1334.2
019600        05 FILE-RECORD-INFO-P121-240.                             SQ1334.2
019700           07 FILLER              PIC X(8).                       SQ1334.2
019800           07 XRECORD-KEY         PIC X(29).                      SQ1334.2
019900           07 FILLER              PIC X(9).                       SQ1334.2
020000           07 ALTERNATE-KEY1      PIC X(29).                      SQ1334.2
020100           07 FILLER              PIC X(9).                       SQ1334.2
020200           07 ALTERNATE-KEY2      PIC X(29).                      SQ1334.2
020300           07 FILLER              PIC X(7).                       SQ1334.2
020400*                                                                 SQ1334.2
020500 01  TEST-RESULTS.                                                SQ1334.2
020600     02 FILLER              PIC X      VALUE SPACE.               SQ1334.2
020700     02  PAR-NAME.                                                SQ1334.2
020800       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1334.2
020900       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1334.2
021000       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1334.2
021100     02 FILLER              PIC X      VALUE SPACE.               SQ1334.2
021200     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1334.2
021300     02 FILLER              PIC X      VALUE SPACE.               SQ1334.2
021400     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1334.2
021500     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1334.2
021600     02 RE-MARK             PIC X(61).                            SQ1334.2
021700 01  TEST-COMPUTED.                                               SQ1334.2
021800   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1334.2
021900   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1334.2
022000   02 COMPUTED-X.                                                 SQ1334.2
022100     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1334.2
022200     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1334.2
022300     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1334.2
022400     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1334.2
022500     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1334.2
022600     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1334.2
022700        04 COMPUTED-18V0                   PIC -9(18).            SQ1334.2
022800        04 FILLER                          PIC X.                 SQ1334.2
022900     03 FILLER PIC X(50) VALUE SPACE.                             SQ1334.2
023000 01  TEST-CORRECT.                                                SQ1334.2
023100     02 FILLER PIC X(30) VALUE SPACE.                             SQ1334.2
023200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1334.2
023300     02 CORRECT-X.                                                SQ1334.2
023400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1334.2
023500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1334.2
023600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1334.2
023700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1334.2
023800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1334.2
023900     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1334.2
024000         04 CORRECT-18V0                     PIC -9(18).          SQ1334.2
024100         04 FILLER                           PIC X.               SQ1334.2
024200     03 FILLER PIC X(2) VALUE SPACE.                              SQ1334.2
024300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1334.2
024400*                                                                 SQ1334.2
024500 01  CCVS-C-1.                                                    SQ1334.2
024600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1334.2
024700     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1334.2
024800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1334.2
024900     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1334.2
025000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1334.2
025100     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1334.2
025200     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1334.2
025300     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1334.2
025400 01  CCVS-C-2.                                                    SQ1334.2
025500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1334.2
025600     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1334.2
025700     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1334.2
025800     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1334.2
025900     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1334.2
026000*                                                                 SQ1334.2
026100 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1334.2
026200 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1334.2
026300 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1334.2
026400 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1334.2
026500 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1334.2
026600 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1334.2
026700 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1334.2
026800 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1334.2
026900 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1334.2
027000 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1334.2
027100 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1334.2
027200 01  CCVS-H-1.                                                    SQ1334.2
027300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1334.2
027400     02  FILLER          PIC X(42)    VALUE                       SQ1334.2
027500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1334.2
027600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1334.2
027700 01  CCVS-H-2A.                                                   SQ1334.2
027800   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1334.2
027900   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1334.2
028000   02  FILLER            PIC XXXX   VALUE                         SQ1334.2
028100     "4.2 ".                                                      SQ1334.2
028200   02  FILLER            PIC X(28)  VALUE                         SQ1334.2
028300            " COPY - NOT FOR DISTRIBUTION".                       SQ1334.2
028400   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1334.2
028500*                                                                 SQ1334.2
028600 01  CCVS-H-2B.                                                   SQ1334.2
028700   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1334.2
028800   02  TEST-ID           PIC X(9).                                SQ1334.2
028900   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1334.2
029000   02  FILLER            PIC X(12)  VALUE                         SQ1334.2
029100     " HIGH       ".                                              SQ1334.2
029200   02  FILLER            PIC X(22)  VALUE                         SQ1334.2
029300            " LEVEL VALIDATION FOR ".                             SQ1334.2
029400   02  FILLER            PIC X(58)  VALUE                         SQ1334.2
029500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1334.2
029600 01  CCVS-H-3.                                                    SQ1334.2
029700     02  FILLER          PIC X(34)  VALUE                         SQ1334.2
029800            " FOR OFFICIAL USE ONLY    ".                         SQ1334.2
029900     02  FILLER          PIC X(58)  VALUE                         SQ1334.2
030000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1334.2
030100     02  FILLER          PIC X(28)  VALUE                         SQ1334.2
030200            "  COPYRIGHT   1985,1986 ".                           SQ1334.2
030300 01  CCVS-E-1.                                                    SQ1334.2
030400     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1334.2
030500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1334.2
030600     02 ID-AGAIN         PIC X(9).                                SQ1334.2
030700     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1334.2
030800 01  CCVS-E-2.                                                    SQ1334.2
030900     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1334.2
031000     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1334.2
031100     02  CCVS-E-2-2.                                              SQ1334.2
031200         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1334.2
031300         03 FILLER         PIC X      VALUE SPACE.                SQ1334.2
031400         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1334.2
031500            "ERRORS ENCOUNTERED".                                 SQ1334.2
031600 01  CCVS-E-3.                                                    SQ1334.2
031700     02  FILLER          PIC X(22)  VALUE                         SQ1334.2
031800            " FOR OFFICIAL USE ONLY".                             SQ1334.2
031900     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1334.2
032000     02  FILLER          PIC X(58)  VALUE                         SQ1334.2
032100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1334.2
032200     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1334.2
032300     02  FILLER          PIC X(20)  VALUE                         SQ1334.2
032400             " COPYRIGHT 1985,1986".                              SQ1334.2
032500 01  CCVS-E-4.                                                    SQ1334.2
032600     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1334.2
032700     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1334.2
032800     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1334.2
032900     02 FILLER           PIC X(40)  VALUE                         SQ1334.2
033000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1334.2
033100 01  XXINFO.                                                      SQ1334.2
033200     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1334.2
033300     02 INFO-TEXT.                                                SQ1334.2
033400       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1334.2
033500       04 XXCOMPUTED         PIC X(20).                           SQ1334.2
033600       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1334.2
033700       04 XXCORRECT          PIC X(20).                           SQ1334.2
033800     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1334.2
033900 01  HYPHEN-LINE.                                                 SQ1334.2
034000     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1334.2
034100     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1334.2
034200-    "*****************************************".                 SQ1334.2
034300     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1334.2
034400-    "******************************".                            SQ1334.2
034500 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1334.2
034600     "SQ133A".                                                    SQ1334.2
034700*                                                                 SQ1334.2
034800*                                                                 SQ1334.2
034900 PROCEDURE DIVISION.                                              SQ1334.2
035000 DECLARATIVES.                                                    SQ1334.2
035100*                                                                 SQ1334.2
035200 SECT-SQ133A-0001 SECTION.                                        SQ1334.2
035300     USE AFTER EXCEPTION PROCEDURE I-O.                           SQ1334.2
035400 I-O-ERROR-PROCESS.                                               SQ1334.2
035500     MOVE   "EXECUTED" TO DECL-EXEC-I-O.                          SQ1334.2
035600     IF DECL-EXEC-SW NOT = SPACE                                  SQ1334.2
035700         GO TO   END-DECLS.                                       SQ1334.2
035800*                                                                 SQ1334.2
035900     MOVE    1 TO REC-CT.                                         SQ1334.2
036000     MOVE   "REWRITE AFTER FAILED RD"  TO FEATURE.                SQ1334.2
036100     MOVE   "DCL-REWRITE-01" TO PAR-NAME.                         SQ1334.2
036200     GO TO   DCL-REWRITE-01.                                      SQ1334.2
036300 DECL-DELETE-01.                                                  SQ1334.2
036400     PERFORM DECL-DE-LETE.                                        SQ1334.2
036500     GO TO   DECL-TEST-01-END.                                    SQ1334.2
036600 DCL-REWRITE-01.                                                  SQ1334.2
036700     IF SQ-FS4-STATUS = "43"                                      SQ1334.2
036800         PERFORM DECL-PASS                                        SQ1334.2
036900     ELSE                                                         SQ1334.2
037000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
037100         MOVE   "43" TO CORRECT-A                                 SQ1334.2
037200         MOVE   "UNEXPECTED I-O STATUS ON FAILED REWRITE"         SQ1334.2
037300                   TO RE-MARK                                     SQ1334.2
037400         MOVE   "VII-4, VII-48,4.5.4(2)" TO ANSI-REFERENCE        SQ1334.2
037500         PERFORM DECL-FAIL.                                       SQ1334.2
037600 DECL-TEST-01-END.                                                SQ1334.2
037700*                                                                 SQ1334.2
037800     PERFORM DECL-WRITE-LINE.                                     SQ1334.2
037900     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ1334.2
038000               TO DUMMY-RECORD.                                   SQ1334.2
038100     PERFORM DECL-WRITE-LINE 3 TIMES.                             SQ1334.2
038200     GO TO   END-DECLS.                                           SQ1334.2
038300*                                                                 SQ1334.2
038400*                                                                 SQ1334.2
038500 DECL-PASS.                                                       SQ1334.2
038600     MOVE   "PASS " TO P-OR-F.                                    SQ1334.2
038700     ADD     1 TO PASS-COUNTER.                                   SQ1334.2
038800     PERFORM DECL-PRINT-DETAIL.                                   SQ1334.2
038900*                                                                 SQ1334.2
039000 DECL-FAIL.                                                       SQ1334.2
039100     MOVE   "FAIL*" TO P-OR-F.                                    SQ1334.2
039200     ADD     1 TO ERROR-COUNTER.                                  SQ1334.2
039300     PERFORM DECL-PRINT-DETAIL.                                   SQ1334.2
039400*                                                                 SQ1334.2
039500 DECL-DE-LETE.                                                    SQ1334.2
039600     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1334.2
039700     MOVE   "*****" TO P-OR-F.                                    SQ1334.2
039800     ADD     1 TO DELETE-COUNTER.                                 SQ1334.2
039900     PERFORM DECL-PRINT-DETAIL.                                   SQ1334.2
040000*                                                                 SQ1334.2
040100 DECL-PRINT-DETAIL.                                               SQ1334.2
040200     IF REC-CT NOT EQUAL TO ZERO                                  SQ1334.2
040300             MOVE "." TO PARDOT-X                                 SQ1334.2
040400             MOVE REC-CT TO DOTVALUE.                             SQ1334.2
040500     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1334.2
040600     PERFORM DECL-WRITE-LINE.                                     SQ1334.2
040700     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1334.2
040800         PERFORM DECL-WRITE-LINE                                  SQ1334.2
040900         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1334.2
041000     ELSE                                                         SQ1334.2
041100         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1334.2
041200     MOVE    SPACE TO P-OR-F.                                     SQ1334.2
041300     MOVE    SPACE TO COMPUTED-X.                                 SQ1334.2
041400     MOVE    SPACE TO CORRECT-X.                                  SQ1334.2
041500     IF REC-CT EQUAL TO ZERO                                      SQ1334.2
041600         MOVE    SPACE TO PAR-NAME.                               SQ1334.2
041700     MOVE    SPACE TO RE-MARK.                                    SQ1334.2
041800*                                                                 SQ1334.2
041900 DECL-WRITE-LINE.                                                 SQ1334.2
042000     ADD     1 TO RECORD-COUNT.                                   SQ1334.2
042100*    IF RECORD-COUNT GREATER 50                                   SQ1334.2
042200*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1334.2
042300*        MOVE    SPACE TO DUMMY-RECORD                            SQ1334.2
042400*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1334.2
042500*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1334.2
042600*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1334.2
042700*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1334.2
042800*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1334.2
042900*        PERFORM DECL-WRT-LN                                      SQ1334.2
043000*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1334.2
043100*        MOVE    ZERO TO RECORD-COUNT.                            SQ1334.2
043200     PERFORM DECL-WRT-LN.                                         SQ1334.2
043300*                                                                 SQ1334.2
043400 DECL-WRT-LN.                                                     SQ1334.2
043500     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1334.2
043600     MOVE    SPACE TO DUMMY-RECORD.                               SQ1334.2
043700*                                                                 SQ1334.2
043800 DECL-FAIL-ROUTINE.                                               SQ1334.2
043900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1334.2
044000     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1334.2
044100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1334.2
044200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1334.2
044300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1334.2
044400     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1334.2
044500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1334.2
044600     GO TO   DECL-FAIL-EX.                                        SQ1334.2
044700 DECL-FAIL-WRITE.                                                 SQ1334.2
044800     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1334.2
044900     PERFORM DECL-WRITE-LINE                                      SQ1334.2
045000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1334.2
045100     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1334.2
045200     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1334.2
045300     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1334.2
045400 DECL-FAIL-EX.                                                    SQ1334.2
045500     EXIT.                                                        SQ1334.2
045600*                                                                 SQ1334.2
045700 DECL-BAIL.                                                       SQ1334.2
045800     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1334.2
045900     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1334.2
046000 DECL-BAIL-WRITE.                                                 SQ1334.2
046100     MOVE    CORRECT-A TO XXCORRECT.                              SQ1334.2
046200     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1334.2
046300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1334.2
046400     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1334.2
046500 DECL-BAIL-EX.                                                    SQ1334.2
046600     EXIT.                                                        SQ1334.2
046700*                                                                 SQ1334.2
046800 END-DECLS.                                                       SQ1334.2
046900 END DECLARATIVES.                                                SQ1334.2
047000*                                                                 SQ1334.2
047100*                                                                 SQ1334.2
047200 CCVS1 SECTION.                                                   SQ1334.2
047300 OPEN-FILES.                                                      SQ1334.2
047400*    OPEN    I-O RAW-DATA.                                        SQ1334.2
047500*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1334.2
047600*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1334.2
047700*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1334.2
047800*    ADD     1           TO C-NO-OF-TESTS.                        SQ1334.2
047900*    ACCEPT  C-DATE      FROM DATE.                               SQ1334.2
048000*    ACCEPT  C-TIME      FROM TIME.                               SQ1334.2
048100*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1334.2
048200*END-E-1.                                                         SQ1334.2
048300*    CLOSE   RAW-DATA.                                            SQ1334.2
048400     OPEN    OUTPUT PRINT-FILE.                                   SQ1334.2
048500     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1334.2
048600     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1334.2
048700     MOVE    SPACE TO TEST-RESULTS.                               SQ1334.2
048800     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1334.2
048900     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1334.2
049000     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1334.2
049100     GO TO CCVS1-EXIT.                                            SQ1334.2
049200*                                                                 SQ1334.2
049300 CCVS-INIT-FILE.                                                  SQ1334.2
049400     ADD     1 TO REC-SKL-SUB.                                    SQ1334.2
049500     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1334.2
049600                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1334.2
049700*                                                                 SQ1334.2
049800 CLOSE-FILES.                                                     SQ1334.2
049900     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1334.2
050000     CLOSE   PRINT-FILE.                                          SQ1334.2
050100*    OPEN    I-O RAW-DATA.                                        SQ1334.2
050200*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1334.2
050300*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1334.2
050400*    MOVE   "OK.     " TO C-ABORT.                                SQ1334.2
050500*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1334.2
050600*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1334.2
050700*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1334.2
050800*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1334.2
050900*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1334.2
051000*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1334.2
051100*END-E-2.                                                         SQ1334.2
051200*    CLOSE   RAW-DATA.                                            SQ1334.2
051300 TERMINATE-CCVS.                                                  SQ1334.2
051400*    EXIT    PROGRAM.                                             SQ1334.2
051500     STOP    RUN.                                                 SQ1334.2
051600*                                                                 SQ1334.2
051700 INSPT.                                                           SQ1334.2
051800     MOVE   "INSPT" TO P-OR-F.                                    SQ1334.2
051900     ADD     1 TO INSPECT-COUNTER.                                SQ1334.2
052000     PERFORM PRINT-DETAIL.                                        SQ1334.2
052100*                                                                 SQ1334.2
052200 PASS.                                                            SQ1334.2
052300     MOVE   "PASS " TO P-OR-F.                                    SQ1334.2
052400     ADD     1 TO PASS-COUNTER.                                   SQ1334.2
052500     PERFORM PRINT-DETAIL.                                        SQ1334.2
052600*                                                                 SQ1334.2
052700 FAIL.                                                            SQ1334.2
052800     MOVE   "FAIL*" TO P-OR-F.                                    SQ1334.2
052900     ADD     1 TO ERROR-COUNTER.                                  SQ1334.2
053000     PERFORM PRINT-DETAIL.                                        SQ1334.2
053100*                                                                 SQ1334.2
053200 DE-LETE.                                                         SQ1334.2
053300     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1334.2
053400     MOVE   "*****" TO P-OR-F.                                    SQ1334.2
053500     ADD     1 TO DELETE-COUNTER.                                 SQ1334.2
053600     PERFORM PRINT-DETAIL.                                        SQ1334.2
053700*                                                                 SQ1334.2
053800 PRINT-DETAIL.                                                    SQ1334.2
053900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1334.2
054000         MOVE   "." TO PARDOT-X                                   SQ1334.2
054100         MOVE    REC-CT TO DOTVALUE.                              SQ1334.2
054200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1334.2
054300     PERFORM WRITE-LINE.                                          SQ1334.2
054400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1334.2
054500         PERFORM WRITE-LINE                                       SQ1334.2
054600         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1334.2
054700     ELSE                                                         SQ1334.2
054800         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1334.2
054900     MOVE    SPACE TO P-OR-F.                                     SQ1334.2
055000     MOVE    SPACE TO COMPUTED-X.                                 SQ1334.2
055100     MOVE    SPACE TO CORRECT-X.                                  SQ1334.2
055200     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1334.2
055300     MOVE    SPACE TO RE-MARK.                                    SQ1334.2
055400*                                                                 SQ1334.2
055500 HEAD-ROUTINE.                                                    SQ1334.2
055600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1334.2
055700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1334.2
055800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1334.2
055900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1334.2
056000 COLUMN-NAMES-ROUTINE.                                            SQ1334.2
056100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1334.2
056200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1334.2
056300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1334.2
056400 END-ROUTINE.                                                     SQ1334.2
056500     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1334.2
056600     PERFORM WRITE-LINE 5 TIMES.                                  SQ1334.2
056700 END-RTN-EXIT.                                                    SQ1334.2
056800     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1334.2
056900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1334.2
057000*                                                                 SQ1334.2
057100 END-ROUTINE-1.                                                   SQ1334.2
057200     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1334.2
057300     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1334.2
057400     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1334.2
057500     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1334.2
057600     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1334.2
057700     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1334.2
057800     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1334.2
057900     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1334.2
058000     PERFORM WRITE-LINE.                                          SQ1334.2
058100     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1334.2
058200     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1334.2
058300         MOVE   "NO " TO ERROR-TOTAL                              SQ1334.2
058400     ELSE                                                         SQ1334.2
058500         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1334.2
058600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1334.2
058700     PERFORM WRITE-LINE.                                          SQ1334.2
058800 END-ROUTINE-13.                                                  SQ1334.2
058900     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1334.2
059000         MOVE   "NO " TO ERROR-TOTAL                              SQ1334.2
059100     ELSE                                                         SQ1334.2
059200         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1334.2
059300     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1334.2
059400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1334.2
059500     PERFORM WRITE-LINE.                                          SQ1334.2
059600     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1334.2
059700         MOVE   "NO " TO ERROR-TOTAL                              SQ1334.2
059800     ELSE                                                         SQ1334.2
059900         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1334.2
060000     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1334.2
060100     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1334.2
060200     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1334.2
060300*                                                                 SQ1334.2
060400 WRITE-LINE.                                                      SQ1334.2
060500     ADD     1 TO RECORD-COUNT.                                   SQ1334.2
060600*    IF RECORD-COUNT GREATER 50                                   SQ1334.2
060700*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1334.2
060800*        MOVE  SPACE TO DUMMY-RECORD                              SQ1334.2
060900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1334.2
061000*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1334.2
061100*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1334.2
061200*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1334.2
061300*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1334.2
061400*        MOVE  ZERO TO RECORD-COUNT.                              SQ1334.2
061500     PERFORM WRT-LN.                                              SQ1334.2
061600*                                                                 SQ1334.2
061700 WRT-LN.                                                          SQ1334.2
061800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1334.2
061900     MOVE    SPACE TO DUMMY-RECORD.                               SQ1334.2
062000 BLANK-LINE-PRINT.                                                SQ1334.2
062100     PERFORM WRT-LN.                                              SQ1334.2
062200 FAIL-ROUTINE.                                                    SQ1334.2
062300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1334.2
062400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1334.2
062500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1334.2
062600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1334.2
062700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1334.2
062800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1334.2
062900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1334.2
063000     GO TO   FAIL-ROUTINE-EX.                                     SQ1334.2
063100 FAIL-ROUTINE-WRITE.                                              SQ1334.2
063200     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1334.2
063300     PERFORM WRITE-LINE                                           SQ1334.2
063400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1334.2
063500     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1334.2
063600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1334.2
063700     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1334.2
063800 FAIL-ROUTINE-EX.                                                 SQ1334.2
063900     EXIT.                                                        SQ1334.2
064000 BAIL-OUT.                                                        SQ1334.2
064100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1334.2
064200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1334.2
064300 BAIL-OUT-WRITE.                                                  SQ1334.2
064400     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1334.2
064500     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1334.2
064600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1334.2
064700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1334.2
064800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1334.2
064900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1334.2
065000 BAIL-OUT-EX.                                                     SQ1334.2
065100     EXIT.                                                        SQ1334.2
065200 CCVS1-EXIT.                                                      SQ1334.2
065300     EXIT.                                                        SQ1334.2
065400*                                                                 SQ1334.2
065500****************************************************************  SQ1334.2
065600*                                                              *  SQ1334.2
065700*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1334.2
065800*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1334.2
065900*                                                              *  SQ1334.2
066000****************************************************************  SQ1334.2
066100*                                                                 SQ1334.2
066200 SECT-SQ133A-0002 SECTION.                                        SQ1334.2
066300 STA-INIT.                                                        SQ1334.2
066400     MOVE    SPACE TO DELETE-SW.                                  SQ1334.2
066500*                                                                 SQ1334.2
066600     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1334.2
066700     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1334.2
066800     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1334.2
066900     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1334.2
067000     MOVE   "CC" TO CHARS-OR-RECORDS (1).                         SQ1334.2
067100     MOVE    1   TO XBLOCK-SIZE (1).                              SQ1334.2
067200     MOVE    1   TO RECORDS-IN-FILE (1).                          SQ1334.2
067300     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1334.2
067400     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1334.2
067500*                                                                 SQ1334.2
067600*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1334.2
067700*                                                                 SQ1334.2
067800 SEQ-INIT-01.                                                     SQ1334.2
067900     MOVE    0 TO REC-CT.                                         SQ1334.2
068000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1334.2
068100     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
068200     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
068300     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1334.2
068400     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1334.2
068500     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1334.2
068600     GO TO   SEQ-TEST-OP-01.                                      SQ1334.2
068700 SEQ-DELETE-01.                                                   SQ1334.2
068800     MOVE   "*" TO DELETE-SW-1.                                   SQ1334.2
068900     GO TO   SEQ-DELETE-01-01.                                    SQ1334.2
069000 SEQ-TEST-OP-01.                                                  SQ1334.2
069100     OPEN    OUTPUT SQ-FS4.                                       SQ1334.2
069200*                                                                 SQ1334.2
069300*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ1334.2
069400*                                                                 SQ1334.2
069500     ADD    1 TO REC-CT.                                          SQ1334.2
069600     IF DELETE-SW NOT = SPACE                                     SQ1334.2
069700         GO TO   SEQ-DELETE-01-01.                                SQ1334.2
069800     GO TO   SEQ-TEST-OP-01-01.                                   SQ1334.2
069900 SEQ-DELETE-01-01.                                                SQ1334.2
070000     PERFORM DE-LETE.                                             SQ1334.2
070100     GO TO   SEQ-TEST-01-01-END.                                  SQ1334.2
070200 SEQ-TEST-OP-01-01.                                               SQ1334.2
070300     IF SQ-FS4-STATUS = "00"                                      SQ1334.2
070400         PERFORM PASS                                             SQ1334.2
070500     ELSE                                                         SQ1334.2
070600         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
070700         MOVE   "00" TO CORRECT-A                                 SQ1334.2
070800         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ1334.2
070900                   TO RE-MARK                                     SQ1334.2
071000         MOVE   "VII-3, VII-43" TO ANSI-REFERENCE                 SQ1334.2
071100         PERFORM FAIL.                                            SQ1334.2
071200 SEQ-TEST-01-01-END.                                              SQ1334.2
071300*                                                                 SQ1334.2
071400*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1334.2
071500*                                                                 SQ1334.2
071600     ADD     1 TO REC-CT.                                         SQ1334.2
071700     IF DELETE-SW NOT = SPACE                                     SQ1334.2
071800         GO TO   SEQ-DELETE-01-02.                                SQ1334.2
071900     GO TO   SEQ-TEST-OP-01-02.                                   SQ1334.2
072000 SEQ-DELETE-01-02.                                                SQ1334.2
072100     PERFORM DE-LETE.                                             SQ1334.2
072200     GO TO   SEQ-TEST-01-02-END.                                  SQ1334.2
072300 SEQ-TEST-OP-01-02.                                               SQ1334.2
072400     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1334.2
072500         PERFORM PASS                                             SQ1334.2
072600     ELSE                                                         SQ1334.2
072700         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1334.2
072800         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1334.2
072900         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1334.2
073000                   TO RE-MARK                                     SQ1334.2
073100         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1334.2
073200         PERFORM FAIL.                                            SQ1334.2
073300 SEQ-TEST-01-02-END.                                              SQ1334.2
073400*                                                                 SQ1334.2
073500*                                                                 SQ1334.2
073600*    A NEW FILE IS OPEN.  WE NOW WRITE ONE RECORD.                SQ1334.2
073700*                                                                 SQ1334.2
073800 SEQ-INIT-02.                                                     SQ1334.2
073900     MOVE    0 TO REC-CT.                                         SQ1334.2
074000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1334.2
074100     ADD     1 TO XRECORD-NUMBER (1).                             SQ1334.2
074200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
074300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
074400     MOVE   "WRITE A RECORD"  TO FEATURE.                         SQ1334.2
074500     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1334.2
074600     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1334.2
074700         GO TO SEQ-DELETE-02.                                     SQ1334.2
074800     GO TO   SEQ-TEST-WR-02.                                      SQ1334.2
074900 SEQ-DELETE-02.                                                   SQ1334.2
075000     MOVE   "*" TO DELETE-SW-2.                                   SQ1334.2
075100     GO TO   SEQ-DELETE-02-01.                                    SQ1334.2
075200 SEQ-TEST-WR-02.                                                  SQ1334.2
075300     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.     SQ1334.2
075400     WRITE   SQ-FS4R1-F-G-120.                                    SQ1334.2
075500*                                                                 SQ1334.2
075600*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ1334.2
075700*                                                                 SQ1334.2
075800     ADD    1 TO REC-CT.                                          SQ1334.2
075900     IF DELETE-SW NOT = SPACE                                     SQ1334.2
076000         GO TO   SEQ-DELETE-02-01.                                SQ1334.2
076100     GO TO   SEQ-TEST-WR-02-01.                                   SQ1334.2
076200 SEQ-DELETE-02-01.                                                SQ1334.2
076300     PERFORM DE-LETE.                                             SQ1334.2
076400     GO TO   SEQ-TEST-02-01-END.                                  SQ1334.2
076500 SEQ-TEST-WR-02-01.                                               SQ1334.2
076600     IF SQ-FS4-STATUS = "00"                                      SQ1334.2
076700         PERFORM PASS                                             SQ1334.2
076800     ELSE                                                         SQ1334.2
076900         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
077000         MOVE   "00" TO CORRECT-A                                 SQ1334.2
077100         MOVE   "UNEXPECTED ERROR CODE FROM WRITE"                SQ1334.2
077200                   TO RE-MARK                                     SQ1334.2
077300         MOVE   "VII-3, VII-53" TO ANSI-REFERENCE                 SQ1334.2
077400         PERFORM FAIL.                                            SQ1334.2
077500 SEQ-TEST-02-01-END.                                              SQ1334.2
077600*                                                                 SQ1334.2
077700*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1334.2
077800*                                                                 SQ1334.2
077900     ADD     1 TO REC-CT.                                         SQ1334.2
078000     IF DELETE-SW NOT = SPACE                                     SQ1334.2
078100         GO TO   SEQ-DELETE-02-02.                                SQ1334.2
078200     GO TO   SEQ-TEST-WR-02-02.                                   SQ1334.2
078300 SEQ-DELETE-02-02.                                                SQ1334.2
078400     PERFORM DE-LETE.                                             SQ1334.2
078500     GO TO   SEQ-TEST-02-02-END.                                  SQ1334.2
078600 SEQ-TEST-WR-02-02.                                               SQ1334.2
078700     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1334.2
078800         PERFORM PASS                                             SQ1334.2
078900     ELSE                                                         SQ1334.2
079000         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1334.2
079100         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1334.2
079200         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1334.2
079300                   TO RE-MARK                                     SQ1334.2
079400         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1334.2
079500         PERFORM FAIL.                                            SQ1334.2
079600 SEQ-TEST-02-02-END.                                              SQ1334.2
079700*                                                                 SQ1334.2
079800*                                                                 SQ1334.2
079900*    NOW CLOSE THE FILE.                                          SQ1334.2
080000*                                                                 SQ1334.2
080100 SEQ-INIT-03.                                                     SQ1334.2
080200     MOVE    0 TO REC-CT.                                         SQ1334.2
080300     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1334.2
080400     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
080500     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
080600     MOVE   "CLOSE FILE" TO FEATURE.                              SQ1334.2
080700     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ1334.2
080800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1334.2
080900         GO TO SEQ-DELETE-03.                                     SQ1334.2
081000     GO TO   SEQ-TEST-CL-03.                                      SQ1334.2
081100 SEQ-DELETE-03.                                                   SQ1334.2
081200     MOVE   "*" TO DELETE-SW-2.                                   SQ1334.2
081300     GO TO   SEQ-DELETE-03-01.                                    SQ1334.2
081400 SEQ-TEST-CL-03.                                                  SQ1334.2
081500     CLOSE   SQ-FS4.                                              SQ1334.2
081600*                                                                 SQ1334.2
081700*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ1334.2
081800*                                                                 SQ1334.2
081900     ADD    1 TO REC-CT.                                          SQ1334.2
082000     IF DELETE-SW NOT = SPACE                                     SQ1334.2
082100         GO TO   SEQ-DELETE-03-01.                                SQ1334.2
082200     GO TO   SEQ-TEST-CL-03-01.                                   SQ1334.2
082300 SEQ-DELETE-03-01.                                                SQ1334.2
082400     PERFORM DE-LETE.                                             SQ1334.2
082500     GO TO   SEQ-TEST-03-01-END.                                  SQ1334.2
082600 SEQ-TEST-CL-03-01.                                               SQ1334.2
082700     IF SQ-FS4-STATUS = "00"                                      SQ1334.2
082800         PERFORM PASS                                             SQ1334.2
082900     ELSE                                                         SQ1334.2
083000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
083100         MOVE   "00" TO CORRECT-A                                 SQ1334.2
083200         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ1334.2
083300                   TO RE-MARK                                     SQ1334.2
083400         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1334.2
083500         PERFORM FAIL.                                            SQ1334.2
083600 SEQ-TEST-03-01-END.                                              SQ1334.2
083700*                                                                 SQ1334.2
083800*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1334.2
083900*                                                                 SQ1334.2
084000     ADD     1 TO REC-CT.                                         SQ1334.2
084100     IF DELETE-SW NOT = SPACE                                     SQ1334.2
084200         GO TO   SEQ-DELETE-03-02.                                SQ1334.2
084300     GO TO   SEQ-TEST-CL-03-02.                                   SQ1334.2
084400 SEQ-DELETE-03-02.                                                SQ1334.2
084500     PERFORM DE-LETE.                                             SQ1334.2
084600     GO TO   SEQ-TEST-03-02-END.                                  SQ1334.2
084700 SEQ-TEST-CL-03-02.                                               SQ1334.2
084800     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1334.2
084900         PERFORM PASS                                             SQ1334.2
085000     ELSE                                                         SQ1334.2
085100         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1334.2
085200         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1334.2
085300         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1334.2
085400                   TO RE-MARK                                     SQ1334.2
085500         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1334.2
085600         PERFORM FAIL.                                            SQ1334.2
085700 SEQ-TEST-03-02-END.                                              SQ1334.2
085800     MOVE    SPACE TO DELETE-SW-2.                                SQ1334.2
085900*                                                                 SQ1334.2
086000*                                                                 SQ1334.2
086100*    OPEN THE FILE IN THE I-O MODE                                SQ1334.2
086200*                                                                 SQ1334.2
086300 SEQ-INIT-04.                                                     SQ1334.2
086400     MOVE    0 TO REC-CT.                                         SQ1334.2
086500     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1334.2
086600     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
086700     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
086800     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1334.2
086900     MOVE   "OPEN FILE FOR I-O" TO FEATURE.                       SQ1334.2
087000     MOVE   "SEQ-TEST-OP-04" TO PAR-NAME.                         SQ1334.2
087100     IF DELETE-SW NOT = SPACE                                     SQ1334.2
087200         GO TO SEQ-DELETE-04-01.                                  SQ1334.2
087300     GO TO   SEQ-TEST-OP-04.                                      SQ1334.2
087400 SEQ-DELETE-04.                                                   SQ1334.2
087500     MOVE   "*" TO DELETE-SW-2.                                   SQ1334.2
087600     GO TO   SEQ-DELETE-04-01.                                    SQ1334.2
087700 SEQ-TEST-OP-04.                                                  SQ1334.2
087800     OPEN    I-O SQ-FS4.                                          SQ1334.2
087900*                                                                 SQ1334.2
088000*    CHECK I-O STATUS RETURNED FROM OPEN I-O                      SQ1334.2
088100*                                                                 SQ1334.2
088200     ADD    1 TO REC-CT.                                          SQ1334.2
088300     IF DELETE-SW NOT = SPACE                                     SQ1334.2
088400         GO TO   SEQ-DELETE-04-01.                                SQ1334.2
088500     GO TO   SEQ-TEST-OP-04-01.                                   SQ1334.2
088600 SEQ-DELETE-04-01.                                                SQ1334.2
088700     PERFORM DE-LETE.                                             SQ1334.2
088800     GO TO   SEQ-TEST-04-01-END.                                  SQ1334.2
088900 SEQ-TEST-OP-04-01.                                               SQ1334.2
089000     IF SQ-FS4-STATUS = "00"                                      SQ1334.2
089100         PERFORM PASS                                             SQ1334.2
089200     ELSE                                                         SQ1334.2
089300         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
089400         MOVE   "00" TO CORRECT-A                                 SQ1334.2
089500         MOVE   "UNEXPECTED I-O STATUS CODE FROM OPEN I-O"        SQ1334.2
089600                   TO RE-MARK                                     SQ1334.2
089700         MOVE   "VII-3, VII-40" TO ANSI-REFERENCE                 SQ1334.2
089800         PERFORM FAIL.                                            SQ1334.2
089900 SEQ-TEST-04-01-END.                                              SQ1334.2
090000*                                                                 SQ1334.2
090100*                                                                 SQ1334.2
090200     ADD     1 TO REC-CT.                                         SQ1334.2
090300     IF DELETE-SW NOT = SPACE                                     SQ1334.2
090400         GO TO   SEQ-DELETE-04-02.                                SQ1334.2
090500     GO TO   SEQ-TEST-OP-04-02.                                   SQ1334.2
090600 SEQ-DELETE-04-02.                                                SQ1334.2
090700     PERFORM DE-LETE.                                             SQ1334.2
090800     GO TO   SEQ-TEST-04-02-END.                                  SQ1334.2
090900 SEQ-TEST-OP-04-02.                                               SQ1334.2
091000     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1334.2
091100         PERFORM PASS                                             SQ1334.2
091200     ELSE                                                         SQ1334.2
091300         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1334.2
091400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1334.2
091500         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1334.2
091600                   TO RE-MARK                                     SQ1334.2
091700         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1334.2
091800         PERFORM FAIL.                                            SQ1334.2
091900 SEQ-TEST-04-02-END.                                              SQ1334.2
092000*                                                                 SQ1334.2
092100*                                                                 SQ1334.2
092200*    THE FILE IS OPEN FOR I-O.  WE READ THE ONLY RECORD.          SQ1334.2
092300*                                                                 SQ1334.2
092400 SEQ-INIT-05.                                                     SQ1334.2
092500     MOVE    0 TO REC-CT.                                         SQ1334.2
092600     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1334.2
092700     ADD     1 TO XRECORD-NUMBER (1).                             SQ1334.2
092800     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
092900     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
093000     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ1334.2
093100     MOVE   "SEQ-TEST-RD-05" TO PAR-NAME.                         SQ1334.2
093200     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1334.2
093300         GO TO SEQ-DELETE-05.                                     SQ1334.2
093400     GO TO   SEQ-TEST-RD-05.                                      SQ1334.2
093500 SEQ-DELETE-05.                                                   SQ1334.2
093600     MOVE   "*" TO DELETE-SW-2.                                   SQ1334.2
093700     GO TO   SEQ-DELETE-05-01.                                    SQ1334.2
093800 SEQ-TEST-RD-05.                                                  SQ1334.2
093900     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ1334.2
094000     READ    SQ-FS4.                                              SQ1334.2
094100*                                                                 SQ1334.2
094200*    CHECK I-O STATUS RETURNED FROM READ                          SQ1334.2
094300*                                                                 SQ1334.2
094400     ADD    1 TO REC-CT.                                          SQ1334.2
094500     IF DELETE-SW NOT = SPACE                                     SQ1334.2
094600         GO TO   SEQ-DELETE-05-01.                                SQ1334.2
094700     GO TO   SEQ-TEST-RD-05-01.                                   SQ1334.2
094800 SEQ-DELETE-05-01.                                                SQ1334.2
094900     PERFORM DE-LETE.                                             SQ1334.2
095000     GO TO   SEQ-TEST-05-01-END.                                  SQ1334.2
095100 SEQ-TEST-RD-05-01.                                               SQ1334.2
095200     IF SQ-FS4-STATUS = "00"                                      SQ1334.2
095300         PERFORM PASS                                             SQ1334.2
095400     ELSE                                                         SQ1334.2
095500         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
095600         MOVE   "00" TO CORRECT-A                                 SQ1334.2
095700         MOVE   "UNEXPECTED STATUS CODE FROM READ"                SQ1334.2
095800                   TO RE-MARK                                     SQ1334.2
095900         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ1334.2
096000         PERFORM FAIL.                                            SQ1334.2
096100 SEQ-TEST-05-01-END.                                              SQ1334.2
096200*                                                                 SQ1334.2
096300*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1334.2
096400*                                                                 SQ1334.2
096500     ADD     1 TO REC-CT.                                         SQ1334.2
096600     IF DELETE-SW NOT = SPACE                                     SQ1334.2
096700         GO TO   SEQ-DELETE-05-02.                                SQ1334.2
096800     GO TO   SEQ-TEST-RD-05-02.                                   SQ1334.2
096900 SEQ-DELETE-05-02.                                                SQ1334.2
097000     PERFORM DE-LETE.                                             SQ1334.2
097100     GO TO   SEQ-TEST-05-02-END.                                  SQ1334.2
097200 SEQ-TEST-RD-05-02.                                               SQ1334.2
097300     IF DECL-EXEC-I-O = "NOT EXECUTED"                            SQ1334.2
097400         PERFORM PASS                                             SQ1334.2
097500     ELSE                                                         SQ1334.2
097600         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1334.2
097700         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1334.2
097800         MOVE   "UNEXPECTED EXECUTION OF I-O DECLARATIVE"         SQ1334.2
097900                   TO RE-MARK                                     SQ1334.2
098000         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1334.2
098100         PERFORM FAIL.                                            SQ1334.2
098200 SEQ-TEST-05-02-END.                                              SQ1334.2
098300*                                                                 SQ1334.2
098400*    CHECK THAT THE CORRECT RECORD HAS BEEN RETURNED, BY          SQ1334.2
098500*    CHECKING THE RECORD-NUMBER FIELD.                            SQ1334.2
098600*                                                                 SQ1334.2
098700     ADD     1 TO REC-CT.                                         SQ1334.2
098800     IF DELETE-SW NOT = SPACE                                     SQ1334.2
098900         GO TO   SEQ-DELETE-05-03.                                SQ1334.2
099000     GO TO   SEQ-TEST-RD-05-03.                                   SQ1334.2
099100 SEQ-DELETE-05-03.                                                SQ1334.2
099200     PERFORM DE-LETE.                                             SQ1334.2
099300     GO TO   SEQ-TEST-05-03-END.                                  SQ1334.2
099400 SEQ-TEST-RD-05-03.                                               SQ1334.2
099500     IF FRECORD-NUMBER = XRECORD-NUMBER (1)                       SQ1334.2
099600         PERFORM PASS                                             SQ1334.2
099700     ELSE                                                         SQ1334.2
099800         MOVE    FRECORD-NUMBER TO COMPUTED-18V0                  SQ1334.2
099900         MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0               SQ1334.2
100000         MOVE   "INCORRECT RECORD NUMBER FOUND" TO RE-MARK        SQ1334.2
100100         PERFORM FAIL.                                            SQ1334.2
100200 SEQ-TEST-05-03-END.                                              SQ1334.2
100300     MOVE    SPACE TO DELETE-SW-2.                                SQ1334.2
100400*                                                                 SQ1334.2
100500*                                                                 SQ1334.2
100600*    ANOTHER READ SHOULD CAUSE THE AT END CONDITION.              SQ1334.2
100700*                                                                 SQ1334.2
100800 SEQ-INIT-06.                                                     SQ1334.2
100900     MOVE    0 TO REC-CT.                                         SQ1334.2
101000     MOVE   "*" TO DECL-EXEC-SW.                                  SQ1334.2
101100     ADD     1 TO XRECORD-NUMBER (1).                             SQ1334.2
101200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
101300     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
101400     MOVE   "READ GIVING AT END" TO FEATURE.                      SQ1334.2
101500     MOVE   "SEQ-TEST-RD-06" TO PAR-NAME.                         SQ1334.2
101600     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1334.2
101700         GO TO SEQ-DELETE-06.                                     SQ1334.2
101800     GO TO   SEQ-TEST-RD-06.                                      SQ1334.2
101900 SEQ-DELETE-06.                                                   SQ1334.2
102000     MOVE   "*" TO DELETE-SW-2.                                   SQ1334.2
102100     GO TO   SEQ-DELETE-06-01.                                    SQ1334.2
102200 SEQ-TEST-RD-06.                                                  SQ1334.2
102300     READ    SQ-FS4 RECORD.                                       SQ1334.2
102400*                                                                 SQ1334.2
102500*    CHECK I-O STATUS RETURNED FROM READ                          SQ1334.2
102600*                                                                 SQ1334.2
102700     ADD    1 TO REC-CT.                                          SQ1334.2
102800     IF DELETE-SW NOT = SPACE                                     SQ1334.2
102900         GO TO   SEQ-DELETE-06-01.                                SQ1334.2
103000     GO TO   SEQ-TEST-RD-06-01.                                   SQ1334.2
103100 SEQ-DELETE-06-01.                                                SQ1334.2
103200     PERFORM DE-LETE.                                             SQ1334.2
103300     GO TO   SEQ-TEST-06-01-END.                                  SQ1334.2
103400 SEQ-TEST-RD-06-01.                                               SQ1334.2
103500     IF SQ-FS4-STATUS = "10"                                      SQ1334.2
103600         PERFORM PASS                                             SQ1334.2
103700     ELSE                                                         SQ1334.2
103800         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
103900         MOVE   "10" TO CORRECT-A                                 SQ1334.2
104000         MOVE   "AT END STATUS NOT RETURNED FROM READ"            SQ1334.2
104100                   TO RE-MARK                                     SQ1334.2
104200         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ1334.2
104300         PERFORM FAIL.                                            SQ1334.2
104400 SEQ-TEST-06-01-END.                                              SQ1334.2
104500*                                                                 SQ1334.2
104600*    CHECK EXECUTION OF I-O DECLARATIVE                           SQ1334.2
104700*                                                                 SQ1334.2
104800     ADD     1 TO REC-CT.                                         SQ1334.2
104900     IF DELETE-SW NOT = SPACE                                     SQ1334.2
105000         GO TO   SEQ-DELETE-06-02.                                SQ1334.2
105100     GO TO   SEQ-TEST-RD-06-02.                                   SQ1334.2
105200 SEQ-DELETE-06-02.                                                SQ1334.2
105300     PERFORM DE-LETE.                                             SQ1334.2
105400     GO TO   SEQ-TEST-06-02-END.                                  SQ1334.2
105500 SEQ-TEST-RD-06-02.                                               SQ1334.2
105600     IF DECL-EXEC-I-O = "EXECUTED"                                SQ1334.2
105700         PERFORM PASS                                             SQ1334.2
105800     ELSE                                                         SQ1334.2
105900         MOVE    DECL-EXEC-I-O TO COMPUTED-A                      SQ1334.2
106000         MOVE   "EXECUTED" TO CORRECT-A                           SQ1334.2
106100         MOVE   "I-O DECLARATIVE NOT EXECUTED AT END OF FILE"     SQ1334.2
106200                   TO RE-MARK                                     SQ1334.2
106300         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1334.2
106400         PERFORM FAIL.                                            SQ1334.2
106500 SEQ-TEST-06-02-END.                                              SQ1334.2
106600*                                                                 SQ1334.2
106700*                                                                 SQ1334.2
106800*    FINALLY, TRY TO EXECUTE A REWRITE AFTER THE FAILED READ      SQ1334.2
106900*                                                                 SQ1334.2
107000 SEQ-INIT-07.                                                     SQ1334.2
107100     MOVE    0 TO REC-CT.                                         SQ1334.2
107200     MOVE    SPACE TO DECL-EXEC-SW.                               SQ1334.2
107300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1334.2
107400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I-O.                      SQ1334.2
107500     MOVE   "REWRITE AFTER AT END" TO FEATURE.                    SQ1334.2
107600     MOVE   "SEQ-TEST-RW-07" TO PAR-NAME.                         SQ1334.2
107700     IF DELETE-SW NOT = SPACE                                     SQ1334.2
107800         GO TO SEQ-DELETE-07-01.                                  SQ1334.2
107900     GO TO   SEQ-TEST-RW-07.                                      SQ1334.2
108000 SEQ-DELETE-07.                                                   SQ1334.2
108100     MOVE   "*" TO DELETE-SW-2.                                   SQ1334.2
108200     GO TO   SEQ-DELETE-07-01.                                    SQ1334.2
108300 SEQ-TEST-RW-07.                                                  SQ1334.2
108400     REWRITE SQ-FS4R1-F-G-120.                                    SQ1334.2
108500     MOVE    0 TO REC-CT.                                         SQ1334.2
108600     MOVE   "REWRITE AFTER AT END" TO FEATURE.                    SQ1334.2
108700     MOVE   "SEQ-TEST-RW-07" TO PAR-NAME.                         SQ1334.2
108800*                                                                 SQ1334.2
108900*    CHECK I-O STATUS RETURNED FROM REWRITE                       SQ1334.2
109000*                                                                 SQ1334.2
109100     ADD    1 TO REC-CT.                                          SQ1334.2
109200     IF DELETE-SW NOT = SPACE                                     SQ1334.2
109300         GO TO   SEQ-DELETE-07-01.                                SQ1334.2
109400     GO TO   SEQ-TEST-RW-07-01.                                   SQ1334.2
109500 SEQ-DELETE-07-01.                                                SQ1334.2
109600     PERFORM DE-LETE.                                             SQ1334.2
109700     GO TO   SEQ-TEST-07-01-END.                                  SQ1334.2
109800 SEQ-TEST-RW-07-01.                                               SQ1334.2
109900     IF SQ-FS4-STATUS = "43"                                      SQ1334.2
110000         PERFORM PASS                                             SQ1334.2
110100     ELSE                                                         SQ1334.2
110200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1334.2
110300         MOVE   "43" TO CORRECT-A                                 SQ1334.2
110400         MOVE   "UNEXPECTED STATUS CODE FROM FAILED REWRITE"      SQ1334.2
110500                   TO RE-MARK                                     SQ1334.2
110600         MOVE   "VII-4,1.5.3(4)C, VII-48" TO ANSI-REFERENCE       SQ1334.2
110700         PERFORM FAIL.                                            SQ1334.2
110800 SEQ-TEST-07-01-END.                                              SQ1334.2
110900*                                                                 SQ1334.2
111000*                                                                 SQ1334.2
111100 CCVS-EXIT SECTION.                                               SQ1334.2
111200 CCVS-999999.                                                     SQ1334.2
111300     GO TO   CLOSE-FILES.                                         SQ1334.2
      *END-OF,SQ133A                                                                  
