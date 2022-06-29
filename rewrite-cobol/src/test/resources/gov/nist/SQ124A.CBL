      *HEADER,COBOL,SQ124A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1244.2
000200 PROGRAM-ID.                                                      SQ1244.2
000300     SQ124A.                                                      SQ1244.2
000400****************************************************************  SQ1244.2
000500*                                                              *  SQ1244.2
000600*    VALIDATION FOR:-                                          *  SQ1244.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1244.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1244.2
000900*                             REVISED 1986, AUGUST             *  SQ1244.2
001000*                                                              *  SQ1244.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1244.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1244.2
001300*                                                              *  SQ1244.2
001400****************************************************************  SQ1244.2
001500*                                                              *  SQ1244.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1244.2
001700*                                                              *  SQ1244.2
001800*            X-14   SEQUENTIAL NON-UNIT MASS STORAGE FILE      *  SQ1244.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1244.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1244.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1244.2
002200*                                                              *  SQ1244.2
002300****************************************************************  SQ1244.2
002400*                                                              *  SQ1244.2
002500*    THIS PROGRAM OPENS FOR OUTPUT A FILE WHICH IS ASSIGNED    *  SQ1244.2
002600*    TO A MEDIUM WHICH IS NOT A REEL/UNIT MEDIUM.  ONE RECORD  *  SQ1244.2
002700*    IS WRITTEN TO THE FILE AND A CLOSE UNIT STATEMENT IS      *  SQ1244.2
002800*    EXECUTED.  THIS SHOULD HAVE NO EFFECT ON THE FILE, EXCEPT *  SQ1244.2
002900*    TO CAUSE I-O STATUS 07.  THE FILE SHOULD REMAIN OPEN.  A  *  SQ1244.2
003000*    SECOND RECORD IS THEN WRITTEN AND A  NORMAL, UNQUALIFIED, *  SQ1244.2
003100*    CLOSE STATEMENT IS EXECUTED. THIS SHOULD BE SUCCESSFUL    *  SQ1244.2
003200*    AND CLOSE THE FILE.  THE FILE IS THEN REOPENED FOR INPUT  *  SQ1244.2
003300*    AND THE TWO RECORDS CHECKED.  A CLOSE UNIT STATEMENT IS   *  SQ1244.2
003400*    EXECUTED BEFORE THE FIRST RECORD IS READ, AND AGAIN THIS  *  SQ1244.2
003500*    SHOULD HAVE NO EFFECT ON SUBSEQUENT OPERATIONS ON THE     *  SQ1244.2
003600*    FILE.  AFTER THE TWO RECORDS HAVE BEEN READ, A FURTHER    *  SQ1244.2
003700*    READ STATEMENT IS EXECUTED TO RAISE THE AT END CONDITION, *  SQ1244.2
003800*    WHICH IS CHECKED, AND THE FILE IS CLOSED.                 *  SQ1244.2
003900*                                                              *  SQ1244.2
004000****************************************************************  SQ1244.2
004100*                                                                 SQ1244.2
004200 ENVIRONMENT DIVISION.                                            SQ1244.2
004300 CONFIGURATION SECTION.                                           SQ1244.2
004400 SOURCE-COMPUTER.                                                 SQ1244.2
004500     XXXXX082.                                                    SQ1244.2
004600 OBJECT-COMPUTER.                                                 SQ1244.2
004700     XXXXX083.                                                    SQ1244.2
004800*                                                                 SQ1244.2
004900 INPUT-OUTPUT SECTION.                                            SQ1244.2
005000 FILE-CONTROL.                                                    SQ1244.2
005100     SELECT PRINT-FILE ASSIGN TO                                  SQ1244.2
005200     XXXXX055.                                                    SQ1244.2
005300*                                                                 SQ1244.2
005400*    SELECT RAW-DATA   ASSIGN TO                                  SQ1244.2
005500*    XXXXX062                                                     SQ1244.2
005600*          ORGANIZATION IS INDEXED                                SQ1244.2
005700*          ACCESS MODE  IS RANDOM                                 SQ1244.2
005800*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1244.2
005900*                                                                 SQ1244.2
006000     SELECT SQ-FS4 ASSIGN                                         SQ1244.2
006100     XXXXX014                                                     SQ1244.2
006200            ORGANIZATION IS SEQUENTIAL                            SQ1244.2
006300            ACCESS SEQUENTIAL                                     SQ1244.2
006400            FILE STATUS SQ-FS4-STATUS                             SQ1244.2
006500                          IN STATUS-GROUP                         SQ1244.2
006600            .                                                     SQ1244.2
006700*                                                                 SQ1244.2
006800*                                                                 SQ1244.2
006900 DATA DIVISION.                                                   SQ1244.2
007000 FILE SECTION.                                                    SQ1244.2
007100 FD  PRINT-FILE                                                   SQ1244.2
007200C    LABEL RECORDS                                                SQ1244.2
007300C    XXXXX084                                                     SQ1244.2
007400C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1244.2
007500               .                                                  SQ1244.2
007600 01  PRINT-REC    PICTURE X(120).                                 SQ1244.2
007700 01  DUMMY-RECORD PICTURE X(120).                                 SQ1244.2
007800*                                                                 SQ1244.2
007900*FD  RAW-DATA.                                                    SQ1244.2
008000*01  RAW-DATA-SATZ.                                               SQ1244.2
008100*    05  RAW-DATA-KEY        PIC X(6).                            SQ1244.2
008200*    05  C-DATE              PIC 9(6).                            SQ1244.2
008300*    05  C-TIME              PIC 9(8).                            SQ1244.2
008400*    05  NO-OF-TESTS         PIC 99.                              SQ1244.2
008500*    05  C-OK                PIC 999.                             SQ1244.2
008600*    05  C-ALL               PIC 999.                             SQ1244.2
008700*    05  C-FAIL              PIC 999.                             SQ1244.2
008800*    05  C-DELETED           PIC 999.                             SQ1244.2
008900*    05  C-INSPECT           PIC 999.                             SQ1244.2
009000*    05  C-NOTE              PIC X(13).                           SQ1244.2
009100*    05  C-INDENT            PIC X.                               SQ1244.2
009200*    05  C-ABORT             PIC X(8).                            SQ1244.2
009300*                                                                 SQ1244.2
009400 FD  SQ-FS4                                                       SQ1244.2
009500C    LABEL RECORD IS STANDARD                                     SQ1244.2
009600                .                                                 SQ1244.2
009700 01  SQ-FS4R1-F-G-120.                                            SQ1244.2
009800        05 SQ-FS4R1-RECORD-INFO-P1-120.                           SQ1244.2
009900           07 FILLER              PIC X(5).                       SQ1244.2
010000           07 FFILE-NAME          PIC X(6).                       SQ1244.2
010100           07 FILLER              PIC X(8).                       SQ1244.2
010200           07 FRECORD-NAME        PIC X(6).                       SQ1244.2
010300           07 FILLER              PIC X(1).                       SQ1244.2
010400           07 FREELUNIT-NUMBER    PIC 9(1).                       SQ1244.2
010500           07 FILLER              PIC X(7).                       SQ1244.2
010600           07 FRECORD-NUMBER      PIC 9(6).                       SQ1244.2
010700           07 FILLER              PIC X(6).                       SQ1244.2
010800           07 FUPDATE-NUMBER      PIC 9(2).                       SQ1244.2
010900           07 FILLER              PIC X(5).                       SQ1244.2
011000           07 FODO-NUMBER         PIC 9(4).                       SQ1244.2
011100           07 FILLER              PIC X(5).                       SQ1244.2
011200           07 FPROGRAM-NAME       PIC X(5).                       SQ1244.2
011300           07 FILLER              PIC X(7).                       SQ1244.2
011400           07 FRECORD-LENGTH      PIC 9(6).                       SQ1244.2
011500           07 FILLER              PIC X(7).                       SQ1244.2
011600           07 FCHARS-OR-RECORDS   PIC X(2).                       SQ1244.2
011700           07 FILLER              PIC X(1).                       SQ1244.2
011800           07 FBLOCK-SIZE         PIC 9(4).                       SQ1244.2
011900           07 FILLER              PIC X(6).                       SQ1244.2
012000           07 FRECORDS-IN-FILE    PIC 9(6).                       SQ1244.2
012100           07 FILLER              PIC X(5).                       SQ1244.2
012200           07 FFILE-ORGANIZATION  PIC X(2).                       SQ1244.2
012300           07 FILLER              PIC X(6).                       SQ1244.2
012400           07 FLABEL-TYPE         PIC X(1).                       SQ1244.2
012500*                                                                 SQ1244.2
012600 WORKING-STORAGE SECTION.                                         SQ1244.2
012700*                                                                 SQ1244.2
012800***************************************************************   SQ1244.2
012900*                                                             *   SQ1244.2
013000*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1244.2
013100*                                                             *   SQ1244.2
013200***************************************************************   SQ1244.2
013300*                                                                 SQ1244.2
013400 01  STATUS-GROUP.                                                SQ1244.2
013500     03  SQ-FS4-STATUS.                                           SQ1244.2
013600       05  SQ-FS4-KEY-1   PIC X.                                  SQ1244.2
013700       05  SQ-FS4-KEY-2   PIC X.                                  SQ1244.2
013800*                                                                 SQ1244.2
013900 01  DELETE-SW.                                                   SQ1244.2
014000     03  DELETE-SW-1 PIC X.                                       SQ1244.2
014100     03  DELETE-SW-1-GROUP.                                       SQ1244.2
014200         05  DELETE-SW-2 PIC X.                                   SQ1244.2
014300         05  DELETE-SW-2-GROUP.                                   SQ1244.2
014400             07  DELETE-SW-3 PIC X.                               SQ1244.2
014500*                                                                 SQ1244.2
014600 01  AT-END-SW  PIC X(12).                                        SQ1244.2
014700 01  NOT-END-SW PIC X(12).                                        SQ1244.2
014800*                                                                 SQ1244.2
014900***************************************************************   SQ1244.2
015000*                                                             *   SQ1244.2
015100*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1244.2
015200*                                                             *   SQ1244.2
015300***************************************************************   SQ1244.2
015400*                                                                 SQ1244.2
015500 01  REC-SKEL-SUB   PIC 99.                                       SQ1244.2
015600*                                                                 SQ1244.2
015700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1244.2
015800     03 FILE-RECORD-INFO-SKELETON.                                SQ1244.2
015900        05 FILLER                 PICTURE X(48)       VALUE       SQ1244.2
016000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1244.2
016100        05 FILLER                 PICTURE X(46)       VALUE       SQ1244.2
016200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1244.2
016300        05 FILLER                 PICTURE X(26)       VALUE       SQ1244.2
016400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1244.2
016500        05 FILLER                 PICTURE X(37)       VALUE       SQ1244.2
016600             ",RECKEY=                             ".             SQ1244.2
016700        05 FILLER                 PICTURE X(38)       VALUE       SQ1244.2
016800             ",ALTKEY1=                             ".            SQ1244.2
016900        05 FILLER                 PICTURE X(38)       VALUE       SQ1244.2
017000             ",ALTKEY2=                             ".            SQ1244.2
017100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1244.2
017200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1244.2
017300        05 FILE-RECORD-INFO-P1-120.                               SQ1244.2
017400           07 FILLER              PIC X(5).                       SQ1244.2
017500           07 XFILE-NAME          PIC X(6).                       SQ1244.2
017600           07 FILLER              PIC X(8).                       SQ1244.2
017700           07 XRECORD-NAME        PIC X(6).                       SQ1244.2
017800           07 FILLER              PIC X(1).                       SQ1244.2
017900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1244.2
018000           07 FILLER              PIC X(7).                       SQ1244.2
018100           07 XRECORD-NUMBER      PIC 9(6).                       SQ1244.2
018200           07 FILLER              PIC X(6).                       SQ1244.2
018300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1244.2
018400           07 FILLER              PIC X(5).                       SQ1244.2
018500           07 ODO-NUMBER          PIC 9(4).                       SQ1244.2
018600           07 FILLER              PIC X(5).                       SQ1244.2
018700           07 XPROGRAM-NAME       PIC X(5).                       SQ1244.2
018800           07 FILLER              PIC X(7).                       SQ1244.2
018900           07 XRECORD-LENGTH      PIC 9(6).                       SQ1244.2
019000           07 FILLER              PIC X(7).                       SQ1244.2
019100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1244.2
019200           07 FILLER              PIC X(1).                       SQ1244.2
019300           07 XBLOCK-SIZE         PIC 9(4).                       SQ1244.2
019400           07 FILLER              PIC X(6).                       SQ1244.2
019500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1244.2
019600           07 FILLER              PIC X(5).                       SQ1244.2
019700           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1244.2
019800           07 FILLER              PIC X(6).                       SQ1244.2
019900           07 XLABEL-TYPE         PIC X(1).                       SQ1244.2
020000        05 FILE-RECORD-INFO-P121-240.                             SQ1244.2
020100           07 FILLER              PIC X(8).                       SQ1244.2
020200           07 XRECORD-KEY         PIC X(29).                      SQ1244.2
020300           07 FILLER              PIC X(9).                       SQ1244.2
020400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1244.2
020500           07 FILLER              PIC X(9).                       SQ1244.2
020600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1244.2
020700           07 FILLER              PIC X(7).                       SQ1244.2
020800*                                                                 SQ1244.2
020900 01  TEST-RESULTS.                                                SQ1244.2
021000     02 FILLER              PIC X      VALUE SPACE.               SQ1244.2
021100     02  PAR-NAME.                                                SQ1244.2
021200       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1244.2
021300       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1244.2
021400       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1244.2
021500     02 FILLER              PIC X      VALUE SPACE.               SQ1244.2
021600     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1244.2
021700     02 FILLER              PIC X      VALUE SPACE.               SQ1244.2
021800     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1244.2
021900     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1244.2
022000     02 RE-MARK             PIC X(61).                            SQ1244.2
022100 01  TEST-COMPUTED.                                               SQ1244.2
022200   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1244.2
022300   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1244.2
022400   02 COMPUTED-X.                                                 SQ1244.2
022500     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1244.2
022600     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1244.2
022700     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1244.2
022800     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1244.2
022900     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1244.2
023000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1244.2
023100        04 COMPUTED-18V0                   PIC -9(18).            SQ1244.2
023200        04 FILLER                          PIC X.                 SQ1244.2
023300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1244.2
023400 01  TEST-CORRECT.                                                SQ1244.2
023500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1244.2
023600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1244.2
023700     02 CORRECT-X.                                                SQ1244.2
023800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1244.2
023900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1244.2
024000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1244.2
024100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1244.2
024200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1244.2
024300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1244.2
024400         04 CORRECT-18V0                     PIC -9(18).          SQ1244.2
024500         04 FILLER                           PIC X.               SQ1244.2
024600     03 FILLER PIC X(2) VALUE SPACE.                              SQ1244.2
024700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1244.2
024800*                                                                 SQ1244.2
024900 01  CCVS-C-1.                                                    SQ1244.2
025000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1244.2
025100     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1244.2
025200     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1244.2
025300     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1244.2
025400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1244.2
025500     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1244.2
025600     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1244.2
025700     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1244.2
025800 01  CCVS-C-2.                                                    SQ1244.2
025900     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1244.2
026000     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1244.2
026100     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1244.2
026200     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1244.2
026300     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1244.2
026400*                                                                 SQ1244.2
026500 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1244.2
026600 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1244.2
026700 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1244.2
026800 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1244.2
026900 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1244.2
027000 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1244.2
027100 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1244.2
027200 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1244.2
027300 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1244.2
027400 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1244.2
027500 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1244.2
027600 01  CCVS-H-1.                                                    SQ1244.2
027700     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1244.2
027800     02  FILLER          PIC X(42)    VALUE                       SQ1244.2
027900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1244.2
028000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1244.2
028100 01  CCVS-H-2A.                                                   SQ1244.2
028200   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1244.2
028300   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1244.2
028400   02  FILLER            PIC XXXX   VALUE                         SQ1244.2
028500     "4.2 ".                                                      SQ1244.2
028600   02  FILLER            PIC X(28)  VALUE                         SQ1244.2
028700            " COPY - NOT FOR DISTRIBUTION".                       SQ1244.2
028800   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1244.2
028900*                                                                 SQ1244.2
029000 01  CCVS-H-2B.                                                   SQ1244.2
029100   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1244.2
029200   02  TEST-ID           PIC X(9).                                SQ1244.2
029300   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1244.2
029400   02  FILLER            PIC X(12)  VALUE                         SQ1244.2
029500     " HIGH       ".                                              SQ1244.2
029600   02  FILLER            PIC X(22)  VALUE                         SQ1244.2
029700            " LEVEL VALIDATION FOR ".                             SQ1244.2
029800   02  FILLER            PIC X(58)  VALUE                         SQ1244.2
029900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1244.2
030000 01  CCVS-H-3.                                                    SQ1244.2
030100     02  FILLER          PIC X(34)  VALUE                         SQ1244.2
030200            " FOR OFFICIAL USE ONLY    ".                         SQ1244.2
030300     02  FILLER          PIC X(58)  VALUE                         SQ1244.2
030400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1244.2
030500     02  FILLER          PIC X(28)  VALUE                         SQ1244.2
030600            "  COPYRIGHT   1985,1986 ".                           SQ1244.2
030700 01  CCVS-E-1.                                                    SQ1244.2
030800     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1244.2
030900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1244.2
031000     02 ID-AGAIN         PIC X(9).                                SQ1244.2
031100     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1244.2
031200 01  CCVS-E-2.                                                    SQ1244.2
031300     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1244.2
031400     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1244.2
031500     02  CCVS-E-2-2.                                              SQ1244.2
031600         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1244.2
031700         03 FILLER         PIC X      VALUE SPACE.                SQ1244.2
031800         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1244.2
031900            "ERRORS ENCOUNTERED".                                 SQ1244.2
032000 01  CCVS-E-3.                                                    SQ1244.2
032100     02  FILLER          PIC X(22)  VALUE                         SQ1244.2
032200            " FOR OFFICIAL USE ONLY".                             SQ1244.2
032300     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1244.2
032400     02  FILLER          PIC X(58)  VALUE                         SQ1244.2
032500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1244.2
032600     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1244.2
032700     02  FILLER          PIC X(20)  VALUE                         SQ1244.2
032800             " COPYRIGHT 1985,1986".                              SQ1244.2
032900 01  CCVS-E-4.                                                    SQ1244.2
033000     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1244.2
033100     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1244.2
033200     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1244.2
033300     02 FILLER           PIC X(40)  VALUE                         SQ1244.2
033400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1244.2
033500 01  XXINFO.                                                      SQ1244.2
033600     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1244.2
033700     02 INFO-TEXT.                                                SQ1244.2
033800       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1244.2
033900       04 XXCOMPUTED         PIC X(20).                           SQ1244.2
034000       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1244.2
034100       04 XXCORRECT          PIC X(20).                           SQ1244.2
034200     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1244.2
034300 01  HYPHEN-LINE.                                                 SQ1244.2
034400     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1244.2
034500     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1244.2
034600-    "*****************************************".                 SQ1244.2
034700     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1244.2
034800-    "******************************".                            SQ1244.2
034900 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1244.2
035000     "SQ124A".                                                    SQ1244.2
035100*                                                                 SQ1244.2
035200*                                                                 SQ1244.2
035300 PROCEDURE DIVISION.                                              SQ1244.2
035400 CCVS1 SECTION.                                                   SQ1244.2
035500 OPEN-FILES.                                                      SQ1244.2
035600*    OPEN    I-O RAW-DATA.                                        SQ1244.2
035700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1244.2
035800*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1244.2
035900*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1244.2
036000*    ADD     1           TO C-NO-OF-TESTS.                        SQ1244.2
036100*    ACCEPT  C-DATE      FROM DATE.                               SQ1244.2
036200*    ACCEPT  C-TIME      FROM TIME.                               SQ1244.2
036300*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1244.2
036400*END-E-1.                                                         SQ1244.2
036500*    CLOSE   RAW-DATA.                                            SQ1244.2
036600     OPEN    OUTPUT PRINT-FILE.                                   SQ1244.2
036700     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1244.2
036800     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1244.2
036900     MOVE    SPACE TO TEST-RESULTS.                               SQ1244.2
037000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1244.2
037100     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1244.2
037200     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1244.2
037300     GO TO CCVS1-EXIT.                                            SQ1244.2
037400*                                                                 SQ1244.2
037500 CCVS-INIT-FILE.                                                  SQ1244.2
037600     ADD     1 TO REC-SKL-SUB.                                    SQ1244.2
037700     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1244.2
037800                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1244.2
037900*                                                                 SQ1244.2
038000 CLOSE-FILES.                                                     SQ1244.2
038100     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1244.2
038200     CLOSE   PRINT-FILE.                                          SQ1244.2
038300*    OPEN    I-O RAW-DATA.                                        SQ1244.2
038400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1244.2
038500*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1244.2
038600*    MOVE   "OK.     " TO C-ABORT.                                SQ1244.2
038700*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1244.2
038800*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1244.2
038900*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1244.2
039000*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1244.2
039100*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1244.2
039200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1244.2
039300*END-E-2.                                                         SQ1244.2
039400*    CLOSE   RAW-DATA.                                            SQ1244.2
039500 TERMINATE-CCVS.                                                  SQ1244.2
039600*    EXIT    PROGRAM.                                             SQ1244.2
039700     STOP    RUN.                                                 SQ1244.2
039800*                                                                 SQ1244.2
039900 INSPT.                                                           SQ1244.2
040000     MOVE   "INSPT" TO P-OR-F.                                    SQ1244.2
040100     ADD     1 TO INSPECT-COUNTER.                                SQ1244.2
040200     PERFORM PRINT-DETAIL.                                        SQ1244.2
040300*                                                                 SQ1244.2
040400 PASS.                                                            SQ1244.2
040500     MOVE   "PASS " TO P-OR-F.                                    SQ1244.2
040600     ADD     1 TO PASS-COUNTER.                                   SQ1244.2
040700     PERFORM PRINT-DETAIL.                                        SQ1244.2
040800*                                                                 SQ1244.2
040900 FAIL.                                                            SQ1244.2
041000     MOVE   "FAIL*" TO P-OR-F.                                    SQ1244.2
041100     ADD     1 TO ERROR-COUNTER.                                  SQ1244.2
041200     PERFORM PRINT-DETAIL.                                        SQ1244.2
041300*                                                                 SQ1244.2
041400 DE-LETE.                                                         SQ1244.2
041500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1244.2
041600     MOVE   "*****" TO P-OR-F.                                    SQ1244.2
041700     ADD     1 TO DELETE-COUNTER.                                 SQ1244.2
041800     PERFORM PRINT-DETAIL.                                        SQ1244.2
041900*                                                                 SQ1244.2
042000 PRINT-DETAIL.                                                    SQ1244.2
042100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1244.2
042200         MOVE   "." TO PARDOT-X                                   SQ1244.2
042300         MOVE    REC-CT TO DOTVALUE.                              SQ1244.2
042400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1244.2
042500     PERFORM WRITE-LINE.                                          SQ1244.2
042600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1244.2
042700         PERFORM WRITE-LINE                                       SQ1244.2
042800         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1244.2
042900     ELSE                                                         SQ1244.2
043000         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1244.2
043100     MOVE    SPACE TO P-OR-F.                                     SQ1244.2
043200     MOVE    SPACE TO COMPUTED-X.                                 SQ1244.2
043300     MOVE    SPACE TO CORRECT-X.                                  SQ1244.2
043400     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1244.2
043500     MOVE    SPACE TO RE-MARK.                                    SQ1244.2
043600*                                                                 SQ1244.2
043700 HEAD-ROUTINE.                                                    SQ1244.2
043800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1244.2
043900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1244.2
044000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1244.2
044100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1244.2
044200 COLUMN-NAMES-ROUTINE.                                            SQ1244.2
044300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1244.2
044400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1244.2
044500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1244.2
044600 END-ROUTINE.                                                     SQ1244.2
044700     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1244.2
044800     PERFORM WRITE-LINE 5 TIMES.                                  SQ1244.2
044900 END-RTN-EXIT.                                                    SQ1244.2
045000     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1244.2
045100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1244.2
045200*                                                                 SQ1244.2
045300 END-ROUTINE-1.                                                   SQ1244.2
045400     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1244.2
045500     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1244.2
045600     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1244.2
045700     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1244.2
045800     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1244.2
045900     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1244.2
046000     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1244.2
046100     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1244.2
046200     PERFORM WRITE-LINE.                                          SQ1244.2
046300     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1244.2
046400     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1244.2
046500         MOVE   "NO " TO ERROR-TOTAL                              SQ1244.2
046600     ELSE                                                         SQ1244.2
046700         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1244.2
046800     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1244.2
046900     PERFORM WRITE-LINE.                                          SQ1244.2
047000 END-ROUTINE-13.                                                  SQ1244.2
047100     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1244.2
047200         MOVE   "NO " TO ERROR-TOTAL                              SQ1244.2
047300     ELSE                                                         SQ1244.2
047400         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1244.2
047500     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1244.2
047600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1244.2
047700     PERFORM WRITE-LINE.                                          SQ1244.2
047800     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1244.2
047900         MOVE   "NO " TO ERROR-TOTAL                              SQ1244.2
048000     ELSE                                                         SQ1244.2
048100         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1244.2
048200     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1244.2
048300     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1244.2
048400     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1244.2
048500*                                                                 SQ1244.2
048600 WRITE-LINE.                                                      SQ1244.2
048700     ADD     1 TO RECORD-COUNT.                                   SQ1244.2
048800*    IF RECORD-COUNT GREATER 50                                   SQ1244.2
048900*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1244.2
049000*        MOVE  SPACE TO DUMMY-RECORD                              SQ1244.2
049100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1244.2
049200*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1244.2
049300*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1244.2
049400*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1244.2
049500*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1244.2
049600*        MOVE  ZERO TO RECORD-COUNT.                              SQ1244.2
049700     PERFORM WRT-LN.                                              SQ1244.2
049800*                                                                 SQ1244.2
049900 WRT-LN.                                                          SQ1244.2
050000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1244.2
050100     MOVE    SPACE TO DUMMY-RECORD.                               SQ1244.2
050200 BLANK-LINE-PRINT.                                                SQ1244.2
050300     PERFORM WRT-LN.                                              SQ1244.2
050400 FAIL-ROUTINE.                                                    SQ1244.2
050500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1244.2
050600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1244.2
050700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1244.2
050800     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1244.2
050900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1244.2
051000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1244.2
051100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1244.2
051200     GO TO   FAIL-ROUTINE-EX.                                     SQ1244.2
051300 FAIL-ROUTINE-WRITE.                                              SQ1244.2
051400     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1244.2
051500     PERFORM WRITE-LINE                                           SQ1244.2
051600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1244.2
051700     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1244.2
051800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1244.2
051900     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1244.2
052000 FAIL-ROUTINE-EX.                                                 SQ1244.2
052100     EXIT.                                                        SQ1244.2
052200 BAIL-OUT.                                                        SQ1244.2
052300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1244.2
052400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1244.2
052500 BAIL-OUT-WRITE.                                                  SQ1244.2
052600     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1244.2
052700     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1244.2
052800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1244.2
052900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1244.2
053000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1244.2
053100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1244.2
053200 BAIL-OUT-EX.                                                     SQ1244.2
053300     EXIT.                                                        SQ1244.2
053400 CCVS1-EXIT.                                                      SQ1244.2
053500     EXIT.                                                        SQ1244.2
053600*                                                                 SQ1244.2
053700****************************************************************  SQ1244.2
053800*                                                              *  SQ1244.2
053900*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1244.2
054000*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1244.2
054100*                                                              *  SQ1244.2
054200****************************************************************  SQ1244.2
054300*                                                                 SQ1244.2
054400 SECT-SQ124A-0004 SECTION.                                        SQ1244.2
054500 STA-INIT.                                                        SQ1244.2
054600     MOVE    SPACE TO DELETE-SW.                                  SQ1244.2
054700*                                                                 SQ1244.2
054800     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1244.2
054900     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1244.2
055000     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1244.2
055100     MOVE    125 TO XRECORD-LENGTH (1).                           SQ1244.2
055200     MOVE   "RC" TO CHARS-OR-RECORDS (1).                         SQ1244.2
055300     MOVE    1   TO XBLOCK-SIZE (1).                              SQ1244.2
055400     MOVE    2   TO RECORDS-IN-FILE (1).                          SQ1244.2
055500     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1244.2
055600     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1244.2
055700     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1244.2
055800*                                                                 SQ1244.2
055900*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1244.2
056000*    DELETION OF THE OPEN OPERATION DELETES EVERY TEST            SQ1244.2
056100*    IN THE PROGRAM                                               SQ1244.2
056200*                                                                 SQ1244.2
056300 SEQ-INIT-01.                                                     SQ1244.2
056400     MOVE    0 TO REC-CT.                                         SQ1244.2
056500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
056600     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1244.2
056700     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1244.2
056800     GO TO   SEQ-TEST-OP-01.                                      SQ1244.2
056900 SEQ-DELETE-01.                                                   SQ1244.2
057000     MOVE   "*" TO DELETE-SW-1.                                   SQ1244.2
057100     GO TO   SEQ-DELETE-01-01.                                    SQ1244.2
057200 SEQ-TEST-OP-01.                                                  SQ1244.2
057300     OPEN    OUTPUT SQ-FS4.                                       SQ1244.2
057400*                                                                 SQ1244.2
057500*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ1244.2
057600*                                                                 SQ1244.2
057700     ADD    1 TO REC-CT.                                          SQ1244.2
057800     IF DELETE-SW NOT = SPACE                                     SQ1244.2
057900         GO TO   SEQ-DELETE-01-01.                                SQ1244.2
058000     GO TO   SEQ-TEST-OP-01-01.                                   SQ1244.2
058100 SEQ-DELETE-01-01.                                                SQ1244.2
058200     PERFORM DE-LETE.                                             SQ1244.2
058300     GO TO   SEQ-TEST-01-01-END.                                  SQ1244.2
058400 SEQ-TEST-OP-01-01.                                               SQ1244.2
058500     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
058600         PERFORM PASS                                             SQ1244.2
058700     ELSE                                                         SQ1244.2
058800         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
058900         MOVE   "00" TO CORRECT-A                                 SQ1244.2
059000         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ1244.2
059100                   TO RE-MARK                                     SQ1244.2
059200         MOVE   "VII-3, VII-23" TO ANSI-REFERENCE                 SQ1244.2
059300         PERFORM FAIL.                                            SQ1244.2
059400 SEQ-TEST-01-01-END.                                              SQ1244.2
059500*                                                                 SQ1244.2
059600*                                                                 SQ1244.2
059700*    THE FILE HAS BEEN CREATED.  WE NOW WRITE ONE RECORD          SQ1244.2
059800*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
059900*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
060000*                                                                 SQ1244.2
060100 SEQ-INIT-02.                                                     SQ1244.2
060200     MOVE    0 TO REC-CT.                                         SQ1244.2
060300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
060400     ADD     1 TO XRECORD-NUMBER (1).                             SQ1244.2
060500     MOVE   "WRITE FIRST RECORD" TO FEATURE.                      SQ1244.2
060600     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1244.2
060700     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
060800         GO TO SEQ-DELETE-02.                                     SQ1244.2
060900     GO TO   SEQ-TEST-WR-02.                                      SQ1244.2
061000 SEQ-DELETE-02.                                                   SQ1244.2
061100     MOVE   "*" TO DELETE-SW-2.                                   SQ1244.2
061200     GO TO   SEQ-DELETE-02-01.                                    SQ1244.2
061300 SEQ-TEST-WR-02.                                                  SQ1244.2
061400     MOVE    FILE-RECORD-INFO (1) TO SQ-FS4R1-F-G-120.            SQ1244.2
061500     WRITE   SQ-FS4R1-F-G-120.                                    SQ1244.2
061600*                                                                 SQ1244.2
061700*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ1244.2
061800*                                                                 SQ1244.2
061900     ADD    1 TO REC-CT.                                          SQ1244.2
062000     IF DELETE-SW NOT = SPACE                                     SQ1244.2
062100         GO TO   SEQ-DELETE-02-01.                                SQ1244.2
062200     GO TO   SEQ-TEST-WR-02-01.                                   SQ1244.2
062300 SEQ-DELETE-02-01.                                                SQ1244.2
062400     PERFORM DE-LETE.                                             SQ1244.2
062500     GO TO   SEQ-TEST-02-01-END.                                  SQ1244.2
062600 SEQ-TEST-WR-02-01.                                               SQ1244.2
062700     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
062800         PERFORM PASS                                             SQ1244.2
062900     ELSE                                                         SQ1244.2
063000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
063100         MOVE   "00" TO CORRECT-A                                 SQ1244.2
063200         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1244.2
063300         MOVE   "VII-3, VII-53,4.7.4(5)" TO ANSI-REFERENCE        SQ1244.2
063400         PERFORM FAIL.                                            SQ1244.2
063500 SEQ-TEST-02-01-END.                                              SQ1244.2
063600     MOVE    SPACE TO DELETE-SW-2.                                SQ1244.2
063700*                                                                 SQ1244.2
063800*                                                                 SQ1244.2
063900*    ONE RECORD HAS BEEN WRITTEN.  WE NOW EXECUTE CLOSE REEL      SQ1244.2
064000*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
064100*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
064200*                                                                 SQ1244.2
064300 SEQ-INIT-03.                                                     SQ1244.2
064400     MOVE    0 TO REC-CT.                                         SQ1244.2
064500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
064600     MOVE   "CLOSE UNIT, NON-UNIT" TO FEATURE.                    SQ1244.2
064700     MOVE   "SEQ-TEST-CR-03" TO PAR-NAME.                         SQ1244.2
064800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
064900         GO TO SEQ-DELETE-03.                                     SQ1244.2
065000     GO TO   SEQ-TEST-CR-03.                                      SQ1244.2
065100 SEQ-DELETE-03.                                                   SQ1244.2
065200     MOVE   "*" TO DELETE-SW-2.                                   SQ1244.2
065300     GO TO   SEQ-DELETE-03-01.                                    SQ1244.2
065400 SEQ-TEST-CR-03.                                                  SQ1244.2
065500     CLOSE   SQ-FS4 UNIT.                                         SQ1244.2
065600*                                                                 SQ1244.2
065700*    CHECK I-O STATUS RETURNED FROM CLOSE REEL                    SQ1244.2
065800*                                                                 SQ1244.2
065900     ADD    1 TO REC-CT.                                          SQ1244.2
066000     IF DELETE-SW NOT = SPACE                                     SQ1244.2
066100         GO TO   SEQ-DELETE-03-01.                                SQ1244.2
066200     GO TO   SEQ-TEST-CR-03-01.                                   SQ1244.2
066300 SEQ-DELETE-03-01.                                                SQ1244.2
066400     PERFORM DE-LETE.                                             SQ1244.2
066500     GO TO   SEQ-TEST-03-01-END.                                  SQ1244.2
066600 SEQ-TEST-CR-03-01.                                               SQ1244.2
066700     IF SQ-FS4-STATUS = "07"                                      SQ1244.2
066800         PERFORM PASS                                             SQ1244.2
066900     ELSE                                                         SQ1244.2
067000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
067100         MOVE   "07" TO CORRECT-A                                 SQ1244.2
067200         MOVE   "UNEXPECTED I-O STATUS FROM CLOSE REEL"           SQ1244.2
067300                   TO RE-MARK                                     SQ1244.2
067400         MOVE   "VII-3, VII-38,4.2.4(3)F" TO ANSI-REFERENCE       SQ1244.2
067500         PERFORM FAIL.                                            SQ1244.2
067600 SEQ-TEST-03-01-END.                                              SQ1244.2
067700     MOVE    SPACE TO DELETE-SW-2.                                SQ1244.2
067800*                                                                 SQ1244.2
067900*    THE FILE SHOULD STILL BE OPEN. WE NOW WRITE ONE MORE RECORD  SQ1244.2
068000*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
068100*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
068200*                                                                 SQ1244.2
068300 SEQ-INIT-04.                                                     SQ1244.2
068400     MOVE    0 TO REC-CT.                                         SQ1244.2
068500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
068600     ADD     1 TO XRECORD-NUMBER (1).                             SQ1244.2
068700     MOVE   "WRITE SECOND RECORD" TO FEATURE.                     SQ1244.2
068800     MOVE   "SEQ-TEST-WR-04" TO PAR-NAME.                         SQ1244.2
068900     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
069000         GO TO SEQ-DELETE-04.                                     SQ1244.2
069100     GO TO   SEQ-TEST-WR-04.                                      SQ1244.2
069200 SEQ-DELETE-04.                                                   SQ1244.2
069300     MOVE   "*" TO DELETE-SW-2.                                   SQ1244.2
069400     GO TO   SEQ-DELETE-04-01.                                    SQ1244.2
069500 SEQ-TEST-WR-04.                                                  SQ1244.2
069600     MOVE    FILE-RECORD-INFO (1) TO SQ-FS4R1-F-G-120.            SQ1244.2
069700     WRITE   SQ-FS4R1-F-G-120.                                    SQ1244.2
069800*                                                                 SQ1244.2
069900*    CHECK I-O STATUS RETURNED FROM WRITE                         SQ1244.2
070000*                                                                 SQ1244.2
070100     ADD    1 TO REC-CT.                                          SQ1244.2
070200     IF DELETE-SW NOT = SPACE                                     SQ1244.2
070300         GO TO   SEQ-DELETE-04-01.                                SQ1244.2
070400     GO TO   SEQ-TEST-WR-04-01.                                   SQ1244.2
070500 SEQ-DELETE-04-01.                                                SQ1244.2
070600     PERFORM DE-LETE.                                             SQ1244.2
070700     GO TO   SEQ-TEST-04-01-END.                                  SQ1244.2
070800 SEQ-TEST-WR-04-01.                                               SQ1244.2
070900     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
071000         PERFORM PASS                                             SQ1244.2
071100     ELSE                                                         SQ1244.2
071200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
071300         MOVE   "00" TO CORRECT-A                                 SQ1244.2
071400         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1244.2
071500         MOVE   "VII-3, VII-53,4.7.4(5)" TO ANSI-REFERENCE        SQ1244.2
071600         PERFORM FAIL.                                            SQ1244.2
071700 SEQ-TEST-04-01-END.                                              SQ1244.2
071800     MOVE    SPACE TO DELETE-SW-2.                                SQ1244.2
071900*                                                                 SQ1244.2
072000*                                                                 SQ1244.2
072100*    NOW EXECUTE A NORMAL CLOSE ON THE FILE.                      SQ1244.2
072200*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
072300*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
072400*                                                                 SQ1244.2
072500 SEQ-INIT-05.                                                     SQ1244.2
072600     MOVE    0 TO REC-CT.                                         SQ1244.2
072700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
072800     MOVE   "CLOSE AFTER CLOSE REEL"  TO FEATURE.                 SQ1244.2
072900     MOVE   "SEQ-TEST-CL-05" TO PAR-NAME.                         SQ1244.2
073000     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
073100         GO TO SEQ-DELETE-05.                                     SQ1244.2
073200     GO TO   SEQ-TEST-CL-05.                                      SQ1244.2
073300 SEQ-DELETE-05.                                                   SQ1244.2
073400     MOVE   "*" TO DELETE-SW-2.                                   SQ1244.2
073500     GO TO   SEQ-DELETE-05-01.                                    SQ1244.2
073600 SEQ-TEST-CL-05.                                                  SQ1244.2
073700     CLOSE   SQ-FS4.                                              SQ1244.2
073800*                                                                 SQ1244.2
073900*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ1244.2
074000*                                                                 SQ1244.2
074100     ADD    1 TO REC-CT.                                          SQ1244.2
074200     IF DELETE-SW NOT = SPACE                                     SQ1244.2
074300         GO TO   SEQ-DELETE-05-01.                                SQ1244.2
074400     GO TO   SEQ-TEST-CL-05-01.                                   SQ1244.2
074500 SEQ-DELETE-05-01.                                                SQ1244.2
074600     PERFORM DE-LETE.                                             SQ1244.2
074700     GO TO   SEQ-TEST-05-01-END.                                  SQ1244.2
074800 SEQ-TEST-CL-05-01.                                               SQ1244.2
074900     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
075000         PERFORM PASS                                             SQ1244.2
075100     ELSE                                                         SQ1244.2
075200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
075300         MOVE   "00" TO CORRECT-A                                 SQ1244.2
075400         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ1244.2
075500                   TO RE-MARK                                     SQ1244.2
075600         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1244.2
075700         PERFORM FAIL.                                            SQ1244.2
075800 SEQ-TEST-05-01-END.                                              SQ1244.2
075900     MOVE    SPACE TO DELETE-SW-2.                                SQ1244.2
076000*                                                                 SQ1244.2
076100*                                                                 SQ1244.2
076200*    NOW OPEN THE FILE FOR INPUT.                                 SQ1244.2
076300*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
076400*    AND ALSO DELETES THE SUBSEQUENT OPERATIONS.                  SQ1244.2
076500*                                                                 SQ1244.2
076600 SEQ-INIT-06.                                                     SQ1244.2
076700     MOVE    0 TO REC-CT.                                         SQ1244.2
076800     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1244.2
076900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
077000     MOVE   "OPEN FILE FOR INPUT"  TO FEATURE.                    SQ1244.2
077100     MOVE   "SEQ-TEST-OP-06" TO PAR-NAME.                         SQ1244.2
077200     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
077300         GO TO SEQ-DELETE-06.                                     SQ1244.2
077400     GO TO   SEQ-TEST-OP-06.                                      SQ1244.2
077500 SEQ-DELETE-06.                                                   SQ1244.2
077600     MOVE   "*" TO DELETE-SW-2.                                   SQ1244.2
077700     GO TO   SEQ-DELETE-06-01.                                    SQ1244.2
077800 SEQ-TEST-OP-06.                                                  SQ1244.2
077900     OPEN    INPUT SQ-FS4.                                        SQ1244.2
078000     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ1244.2
078100*                                                                 SQ1244.2
078200*    CHECK I-O STATUS RETURNED FROM OPEN                          SQ1244.2
078300*                                                                 SQ1244.2
078400     ADD    1 TO REC-CT.                                          SQ1244.2
078500     IF DELETE-SW NOT = SPACE                                     SQ1244.2
078600         GO TO   SEQ-DELETE-06-01.                                SQ1244.2
078700     GO TO   SEQ-TEST-OP-06-01.                                   SQ1244.2
078800 SEQ-DELETE-06-01.                                                SQ1244.2
078900     PERFORM DE-LETE.                                             SQ1244.2
079000     GO TO   SEQ-TEST-06-01-END.                                  SQ1244.2
079100 SEQ-TEST-OP-06-01.                                               SQ1244.2
079200     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
079300         PERFORM PASS                                             SQ1244.2
079400     ELSE                                                         SQ1244.2
079500         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
079600         MOVE   "00" TO CORRECT-A                                 SQ1244.2
079700         MOVE   "UNEXPECTED ERROR CODE FROM OPEN INPUT"           SQ1244.2
079800                   TO RE-MARK                                     SQ1244.2
079900         MOVE   "VII-3, VII-43,4.3.4(23)" TO ANSI-REFERENCE       SQ1244.2
080000         PERFORM FAIL.                                            SQ1244.2
080100 SEQ-TEST-06-01-END.                                              SQ1244.2
080200*                                                                 SQ1244.2
080300*                                                                 SQ1244.2
080400*    WE NOW EXECUTE CLOSE REEL BEFORE ANY RECORD HAS BEEN READ.   SQ1244.2
080500*    APART FROM SETTING I-O STATUS 07 THIS SHOULD HAVE NO EFFECT  SQ1244.2
080600*    ON THE FILE OR THE SUBSEQUENT RETRIEVAL OF RECORDS.          SQ1244.2
080700*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
080800*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
080900*                                                                 SQ1244.2
081000 SEQ-INIT-07.                                                     SQ1244.2
081100     MOVE    0 TO REC-CT.                                         SQ1244.2
081200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
081300     MOVE   "CLOSE UNIT, NON-UNIT" TO FEATURE.                    SQ1244.2
081400     MOVE   "SEQ-TEST-CU-07" TO PAR-NAME.                         SQ1244.2
081500     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
081600         GO TO SEQ-DELETE-07.                                     SQ1244.2
081700     GO TO   SEQ-TEST-CU-07.                                      SQ1244.2
081800 SEQ-DELETE-07.                                                   SQ1244.2
081900     MOVE   "*" TO DELETE-SW-3.                                   SQ1244.2
082000     GO TO   SEQ-DELETE-07-01.                                    SQ1244.2
082100 SEQ-TEST-CU-07.                                                  SQ1244.2
082200     CLOSE   SQ-FS4 UNIT.                                         SQ1244.2
082300*                                                                 SQ1244.2
082400*    CHECK I-O STATUS RETURNED FROM CLOSE REEL                    SQ1244.2
082500*                                                                 SQ1244.2
082600     ADD    1 TO REC-CT.                                          SQ1244.2
082700     IF DELETE-SW NOT = SPACE                                     SQ1244.2
082800         GO TO   SEQ-DELETE-07-01.                                SQ1244.2
082900     GO TO   SEQ-TEST-CU-07-01.                                   SQ1244.2
083000 SEQ-DELETE-07-01.                                                SQ1244.2
083100     PERFORM DE-LETE.                                             SQ1244.2
083200     GO TO   SEQ-TEST-07-01-END.                                  SQ1244.2
083300 SEQ-TEST-CU-07-01.                                               SQ1244.2
083400     IF SQ-FS4-STATUS = "07"                                      SQ1244.2
083500         PERFORM PASS                                             SQ1244.2
083600     ELSE                                                         SQ1244.2
083700         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
083800         MOVE   "07" TO CORRECT-A                                 SQ1244.2
083900         MOVE   "UNEXPECTED I-O STATUS FROM CLOSE UNIT"           SQ1244.2
084000                   TO RE-MARK                                     SQ1244.2
084100         MOVE   "VII-3, VII-38,4.2.4(3)F" TO ANSI-REFERENCE       SQ1244.2
084200         PERFORM FAIL.                                            SQ1244.2
084300 SEQ-TEST-07-01-END.                                              SQ1244.2
084400     MOVE    SPACE TO DELETE-SW-3.                                SQ1244.2
084500*                                                                 SQ1244.2
084600*    THE FILE SHOULD STILL BE OPEN. WE NOW READ A RECORD.         SQ1244.2
084700*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
084800*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
084900*                                                                 SQ1244.2
085000 SEQ-INIT-08.                                                     SQ1244.2
085100     MOVE    0 TO REC-CT.                                         SQ1244.2
085200     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
085300     MOVE   "NOT EXECUTED" TO AT-END-SW.                          SQ1244.2
085400     MOVE   "NOT EXECUTED" TO NOT-END-SW.                         SQ1244.2
085500     ADD     1 TO XRECORD-NUMBER (1).                             SQ1244.2
085600     MOVE   "READ FIRST RECORD" TO FEATURE.                       SQ1244.2
085700     MOVE   "SEQ-TEST-RD-08" TO PAR-NAME.                         SQ1244.2
085800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
085900         GO TO SEQ-DELETE-08.                                     SQ1244.2
086000     GO TO   SEQ-TEST-RD-08.                                      SQ1244.2
086100 SEQ-DELETE-08.                                                   SQ1244.2
086200     MOVE   "*" TO DELETE-SW-3.                                   SQ1244.2
086300     GO TO   SEQ-DELETE-08-01.                                    SQ1244.2
086400 SEQ-TEST-RD-08.                                                  SQ1244.2
086500     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ1244.2
086600     READ    SQ-FS4                                               SQ1244.2
086700     AT END                                                       SQ1244.2
086800         MOVE   "EXECUTED" TO AT-END-SW                           SQ1244.2
086900     NOT END                                                      SQ1244.2
087000         MOVE   "EXECUTED" TO NOT-END-SW.                         SQ1244.2
087100*                                                                 SQ1244.2
087200*    CHECK I-O STATUS RETURNED FROM READ                          SQ1244.2
087300*                                                                 SQ1244.2
087400     ADD    1 TO REC-CT.                                          SQ1244.2
087500     IF DELETE-SW NOT = SPACE                                     SQ1244.2
087600         GO TO   SEQ-DELETE-08-01.                                SQ1244.2
087700     GO TO   SEQ-TEST-RD-08-01.                                   SQ1244.2
087800 SEQ-DELETE-08-01.                                                SQ1244.2
087900     PERFORM DE-LETE.                                             SQ1244.2
088000     GO TO   SEQ-TEST-08-01-END.                                  SQ1244.2
088100 SEQ-TEST-RD-08-01.                                               SQ1244.2
088200     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
088300         PERFORM PASS                                             SQ1244.2
088400     ELSE                                                         SQ1244.2
088500         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
088600         MOVE   "00" TO CORRECT-A                                 SQ1244.2
088700         MOVE   "UNEXPECTED I-O STATUS FROM READ" TO RE-MARK      SQ1244.2
088800         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1244.2
088900         PERFORM FAIL.                                            SQ1244.2
089000 SEQ-TEST-08-01-END.                                              SQ1244.2
089100*                                                                 SQ1244.2
089200*    CHECK EXECUTION OF THE AT END PATH                           SQ1244.2
089300*                                                                 SQ1244.2
089400     ADD     1 TO REC-CT.                                         SQ1244.2
089500     IF DELETE-SW NOT = SPACE                                     SQ1244.2
089600         GO TO   SEQ-DELETE-08-02.                                SQ1244.2
089700     GO TO   SEQ-TEST-RD-08-02.                                   SQ1244.2
089800 SEQ-DELETE-08-02.                                                SQ1244.2
089900     PERFORM DE-LETE.                                             SQ1244.2
090000     GO TO   SEQ-TEST-08-02-END.                                  SQ1244.2
090100 SEQ-TEST-RD-08-02.                                               SQ1244.2
090200     IF AT-END-SW = "NOT EXECUTED"                                SQ1244.2
090300         PERFORM PASS                                             SQ1244.2
090400     ELSE                                                         SQ1244.2
090500         MOVE    AT-END-SW TO COMPUTED-A                          SQ1244.2
090600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1244.2
090700         MOVE   "UNEXPECTED EXECUTION OF AT END PATH" TO RE-MARK  SQ1244.2
090800         MOVE   "VII-46, 4.4.4(11)" TO ANSI-REFERENCE             SQ1244.2
090900         PERFORM FAIL.                                            SQ1244.2
091000 SEQ-TEST-08-02-END.                                              SQ1244.2
091100*                                                                 SQ1244.2
091200*    CHECK EXECUTION OF THE NOT AT END PATH                       SQ1244.2
091300*                                                                 SQ1244.2
091400     ADD     1 TO REC-CT.                                         SQ1244.2
091500     IF DELETE-SW NOT = SPACE                                     SQ1244.2
091600         GO TO   SEQ-DELETE-08-03.                                SQ1244.2
091700     GO TO   SEQ-TEST-RD-08-03.                                   SQ1244.2
091800 SEQ-DELETE-08-03.                                                SQ1244.2
091900     PERFORM DE-LETE.                                             SQ1244.2
092000     GO TO   SEQ-TEST-08-03-END.                                  SQ1244.2
092100 SEQ-TEST-RD-08-03.                                               SQ1244.2
092200     IF NOT-END-SW = "EXECUTED"                                   SQ1244.2
092300         PERFORM PASS                                             SQ1244.2
092400     ELSE                                                         SQ1244.2
092500         MOVE    NOT-END-SW TO COMPUTED-A                         SQ1244.2
092600         MOVE   "EXECUTED" TO CORRECT-A                           SQ1244.2
092700         MOVE   "UNEXPECTED NON-EXECUTION OF AT END PATH"         SQ1244.2
092800                   TO RE-MARK                                     SQ1244.2
092900         MOVE   "VII-46, 4.4.4(11)" TO ANSI-REFERENCE             SQ1244.2
093000         PERFORM FAIL.                                            SQ1244.2
093100 SEQ-TEST-08-03-END.                                              SQ1244.2
093200*                                                                 SQ1244.2
093300*    CHECK THE RECORD NUMBER OF THE RECORD JUST READ.             SQ1244.2
093400*                                                                 SQ1244.2
093500     ADD     1 TO REC-CT.                                         SQ1244.2
093600     IF DELETE-SW NOT = SPACE                                     SQ1244.2
093700         GO TO   SEQ-DELETE-08-04.                                SQ1244.2
093800     GO TO   SEQ-TEST-RD-08-04.                                   SQ1244.2
093900 SEQ-DELETE-08-04.                                                SQ1244.2
094000     PERFORM DE-LETE.                                             SQ1244.2
094100     GO TO   SEQ-TEST-08-04-END.                                  SQ1244.2
094200 SEQ-TEST-RD-08-04.                                               SQ1244.2
094300     IF FRECORD-NUMBER = XRECORD-NUMBER (1)                       SQ1244.2
094400         PERFORM PASS                                             SQ1244.2
094500     ELSE                                                         SQ1244.2
094600         MOVE    FRECORD-NUMBER TO COMPUTED-A                     SQ1244.2
094700         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1244.2
094800         MOVE   "INCORRECT RECORD NUMBER FOUND" TO RE-MARK        SQ1244.2
094900         PERFORM FAIL.                                            SQ1244.2
095000 SEQ-TEST-08-04-END.                                              SQ1244.2
095100     MOVE    SPACE TO DELETE-SW-3.                                SQ1244.2
095200*                                                                 SQ1244.2
095300*                                                                 SQ1244.2
095400*    WE NOW READ THE SECOND AND FINAL RECORD.                     SQ1244.2
095500*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
095600*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
095700*                                                                 SQ1244.2
095800 SEQ-INIT-09.                                                     SQ1244.2
095900     MOVE    0 TO REC-CT.                                         SQ1244.2
096000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
096100     MOVE   "NOT EXECUTED" TO AT-END-SW.                          SQ1244.2
096200     MOVE   "NOT EXECUTED" TO NOT-END-SW.                         SQ1244.2
096300     ADD     1 TO XRECORD-NUMBER (1).                             SQ1244.2
096400     MOVE   "READ SECOND RECORD" TO FEATURE.                      SQ1244.2
096500     MOVE   "SEQ-TEST-RD-09" TO PAR-NAME.                         SQ1244.2
096600     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
096700         GO TO SEQ-DELETE-09.                                     SQ1244.2
096800     GO TO   SEQ-TEST-RD-09.                                      SQ1244.2
096900 SEQ-DELETE-09.                                                   SQ1244.2
097000     MOVE   "*" TO DELETE-SW-3.                                   SQ1244.2
097100     GO TO   SEQ-DELETE-09-01.                                    SQ1244.2
097200 SEQ-TEST-RD-09.                                                  SQ1244.2
097300     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ1244.2
097400     READ    SQ-FS4                                               SQ1244.2
097500     END                                                          SQ1244.2
097600          MOVE   "EXECUTED" TO AT-END-SW                          SQ1244.2
097700     NOT AT END                                                   SQ1244.2
097800          MOVE   "EXECUTED" TO NOT-END-SW.                        SQ1244.2
097900*                                                                 SQ1244.2
098000*    CHECK I-O STATUS RETURNED FROM READ                          SQ1244.2
098100*                                                                 SQ1244.2
098200     ADD    1 TO REC-CT.                                          SQ1244.2
098300     IF DELETE-SW NOT = SPACE                                     SQ1244.2
098400         GO TO   SEQ-DELETE-09-01.                                SQ1244.2
098500     GO TO   SEQ-TEST-RD-09-01.                                   SQ1244.2
098600 SEQ-DELETE-09-01.                                                SQ1244.2
098700     PERFORM DE-LETE.                                             SQ1244.2
098800     GO TO   SEQ-TEST-09-01-END.                                  SQ1244.2
098900 SEQ-TEST-RD-09-01.                                               SQ1244.2
099000     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
099100         PERFORM PASS                                             SQ1244.2
099200     ELSE                                                         SQ1244.2
099300         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
099400         MOVE   "00" TO CORRECT-A                                 SQ1244.2
099500         MOVE   "UNEXPECTED I-O STATUS FROM READ" TO RE-MARK      SQ1244.2
099600         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1244.2
099700         PERFORM FAIL.                                            SQ1244.2
099800 SEQ-TEST-09-01-END.                                              SQ1244.2
099900*                                                                 SQ1244.2
100000*    CHECK EXECUTION OF THE AT END PATH                           SQ1244.2
100100*                                                                 SQ1244.2
100200     ADD     1 TO REC-CT.                                         SQ1244.2
100300     IF DELETE-SW NOT = SPACE                                     SQ1244.2
100400         GO TO   SEQ-DELETE-09-02.                                SQ1244.2
100500     GO TO   SEQ-TEST-RD-09-02.                                   SQ1244.2
100600 SEQ-DELETE-09-02.                                                SQ1244.2
100700     PERFORM DE-LETE.                                             SQ1244.2
100800     GO TO   SEQ-TEST-09-02-END.                                  SQ1244.2
100900 SEQ-TEST-RD-09-02.                                               SQ1244.2
101000     IF AT-END-SW = "NOT EXECUTED"                                SQ1244.2
101100         PERFORM PASS                                             SQ1244.2
101200     ELSE                                                         SQ1244.2
101300         MOVE    AT-END-SW TO COMPUTED-A                          SQ1244.2
101400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1244.2
101500         MOVE   "UNEXPECTED EXECUTION OF AT END PATH" TO RE-MARK  SQ1244.2
101600         MOVE   "VII-46, 4.4.4(11)" TO ANSI-REFERENCE             SQ1244.2
101700         PERFORM FAIL.                                            SQ1244.2
101800 SEQ-TEST-09-02-END.                                              SQ1244.2
101900*                                                                 SQ1244.2
102000*    CHECK EXECUTION OF THE NOT AT END PATH                       SQ1244.2
102100*                                                                 SQ1244.2
102200     ADD     1 TO REC-CT.                                         SQ1244.2
102300     IF DELETE-SW NOT = SPACE                                     SQ1244.2
102400         GO TO   SEQ-DELETE-09-03.                                SQ1244.2
102500     GO TO   SEQ-TEST-RD-09-03.                                   SQ1244.2
102600 SEQ-DELETE-09-03.                                                SQ1244.2
102700     PERFORM DE-LETE.                                             SQ1244.2
102800     GO TO   SEQ-TEST-09-03-END.                                  SQ1244.2
102900 SEQ-TEST-RD-09-03.                                               SQ1244.2
103000     IF NOT-END-SW = "EXECUTED"                                   SQ1244.2
103100         PERFORM PASS                                             SQ1244.2
103200     ELSE                                                         SQ1244.2
103300         MOVE    NOT-END-SW TO COMPUTED-A                         SQ1244.2
103400         MOVE   "EXECUTED" TO CORRECT-A                           SQ1244.2
103500         MOVE   "UNEXPECTED NON-EXECUTION OF AT END PATH"         SQ1244.2
103600                   TO RE-MARK                                     SQ1244.2
103700         MOVE   "VII-46, 4.4.4(11)" TO ANSI-REFERENCE             SQ1244.2
103800         PERFORM FAIL.                                            SQ1244.2
103900 SEQ-TEST-09-03-END.                                              SQ1244.2
104000*                                                                 SQ1244.2
104100*    CHECK THE RECORD NUMBER OF THE RECORD JUST READ.             SQ1244.2
104200*                                                                 SQ1244.2
104300     ADD     1 TO REC-CT.                                         SQ1244.2
104400     IF DELETE-SW NOT = SPACE                                     SQ1244.2
104500         GO TO   SEQ-DELETE-09-04.                                SQ1244.2
104600     GO TO   SEQ-TEST-RD-09-04.                                   SQ1244.2
104700 SEQ-DELETE-09-04.                                                SQ1244.2
104800     PERFORM DE-LETE.                                             SQ1244.2
104900     GO TO   SEQ-TEST-09-04-END.                                  SQ1244.2
105000 SEQ-TEST-RD-09-04.                                               SQ1244.2
105100     IF FRECORD-NUMBER = XRECORD-NUMBER (1)                       SQ1244.2
105200         PERFORM PASS                                             SQ1244.2
105300     ELSE                                                         SQ1244.2
105400         MOVE    FRECORD-NUMBER TO COMPUTED-A                     SQ1244.2
105500         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1244.2
105600         MOVE   "INCORRECT RECORD NUMBER FOUND" TO RE-MARK        SQ1244.2
105700         PERFORM FAIL.                                            SQ1244.2
105800 SEQ-TEST-09-04-END.                                              SQ1244.2
105900     MOVE    SPACE TO DELETE-SW-3.                                SQ1244.2
106000*                                                                 SQ1244.2
106100*                                                                 SQ1244.2
106200*    WE NOW ATTEMPT ANOTHER READ, WHICH SHOULD RAISE AT END.      SQ1244.2
106300*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS      SQ1244.2
106400*    BUT DOES NOT AFFECT SUBSEQUENT OPERATIONS.                   SQ1244.2
106500*                                                                 SQ1244.2
106600 SEQ-INIT-10.                                                     SQ1244.2
106700     MOVE    0 TO REC-CT.                                         SQ1244.2
106800     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
106900     MOVE   "NOT EXECUTED" TO AT-END-SW.                          SQ1244.2
107000     MOVE   "NOT EXECUTED" TO NOT-END-SW.                         SQ1244.2
107100     ADD     1 TO XRECORD-NUMBER (1).                             SQ1244.2
107200     MOVE   "READ TO GIVE AT END" TO FEATURE.                     SQ1244.2
107300     MOVE   "SEQ-TEST-RD-10" TO PAR-NAME.                         SQ1244.2
107400     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
107500         GO TO SEQ-DELETE-10.                                     SQ1244.2
107600     GO TO   SEQ-TEST-RD-10.                                      SQ1244.2
107700 SEQ-DELETE-10.                                                   SQ1244.2
107800     MOVE   "*" TO DELETE-SW-3.                                   SQ1244.2
107900     GO TO   SEQ-DELETE-10-01.                                    SQ1244.2
108000 SEQ-TEST-RD-10.                                                  SQ1244.2
108100     MOVE    SPACE TO SQ-FS4R1-F-G-120.                           SQ1244.2
108200     READ    SQ-FS4                                               SQ1244.2
108300     END                                                          SQ1244.2
108400          MOVE   "EXECUTED" TO AT-END-SW                          SQ1244.2
108500     NOT AT END                                                   SQ1244.2
108600          MOVE   "EXECUTED" TO NOT-END-SW.                        SQ1244.2
108700*                                                                 SQ1244.2
108800*    CHECK I-O STATUS RETURNED FROM READ                          SQ1244.2
108900*                                                                 SQ1244.2
109000     ADD    1 TO REC-CT.                                          SQ1244.2
109100     IF DELETE-SW NOT = SPACE                                     SQ1244.2
109200         GO TO   SEQ-DELETE-10-01.                                SQ1244.2
109300     GO TO   SEQ-TEST-RD-10-01.                                   SQ1244.2
109400 SEQ-DELETE-10-01.                                                SQ1244.2
109500     PERFORM DE-LETE.                                             SQ1244.2
109600     GO TO   SEQ-TEST-10-01-END.                                  SQ1244.2
109700 SEQ-TEST-RD-10-01.                                               SQ1244.2
109800     IF SQ-FS4-STATUS = "10"                                      SQ1244.2
109900         PERFORM PASS                                             SQ1244.2
110000     ELSE                                                         SQ1244.2
110100         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
110200         MOVE   "10" TO CORRECT-A                                 SQ1244.2
110300         MOVE   "UNEXPECTED I-O STATUS AFTER LAST RECORD"         SQ1244.2
110400                    TO RE-MARK                                    SQ1244.2
110500         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1244.2
110600         PERFORM FAIL.                                            SQ1244.2
110700 SEQ-TEST-10-01-END.                                              SQ1244.2
110800*                                                                 SQ1244.2
110900*    CHECK EXECUTION OF THE AT END PATH                           SQ1244.2
111000*                                                                 SQ1244.2
111100     ADD     1 TO REC-CT.                                         SQ1244.2
111200     IF DELETE-SW NOT = SPACE                                     SQ1244.2
111300         GO TO   SEQ-DELETE-10-02.                                SQ1244.2
111400     GO TO   SEQ-TEST-RD-10-02.                                   SQ1244.2
111500 SEQ-DELETE-10-02.                                                SQ1244.2
111600     PERFORM DE-LETE.                                             SQ1244.2
111700     GO TO   SEQ-TEST-10-02-END.                                  SQ1244.2
111800 SEQ-TEST-RD-10-02.                                               SQ1244.2
111900     IF AT-END-SW = "EXECUTED"                                    SQ1244.2
112000         PERFORM PASS                                             SQ1244.2
112100     ELSE                                                         SQ1244.2
112200         MOVE    AT-END-SW TO COMPUTED-A                          SQ1244.2
112300         MOVE   "EXECUTED" TO CORRECT-A                           SQ1244.2
112400         MOVE   "UNEXPECTED NON-EXECUTION OF AT END PATH"         SQ1244.2
112500                    TO RE-MARK                                    SQ1244.2
112600         MOVE   "VII-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1244.2
112700         PERFORM FAIL.                                            SQ1244.2
112800 SEQ-TEST-10-02-END.                                              SQ1244.2
112900*                                                                 SQ1244.2
113000*    CHECK EXECUTION OF THE NOT AT END PATH                       SQ1244.2
113100*                                                                 SQ1244.2
113200     ADD     1 TO REC-CT.                                         SQ1244.2
113300     IF DELETE-SW NOT = SPACE                                     SQ1244.2
113400         GO TO   SEQ-DELETE-10-03.                                SQ1244.2
113500     GO TO   SEQ-TEST-RD-10-03.                                   SQ1244.2
113600 SEQ-DELETE-10-03.                                                SQ1244.2
113700     PERFORM DE-LETE.                                             SQ1244.2
113800     GO TO   SEQ-TEST-10-03-END.                                  SQ1244.2
113900 SEQ-TEST-RD-10-03.                                               SQ1244.2
114000     IF NOT-END-SW = "NOT EXECUTED"                               SQ1244.2
114100         PERFORM PASS                                             SQ1244.2
114200     ELSE                                                         SQ1244.2
114300         MOVE    NOT-END-SW TO COMPUTED-A                         SQ1244.2
114400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1244.2
114500         MOVE   "UNEXPECTED EXECUTION OF AT END PATH" TO RE-MARK  SQ1244.2
114600         MOVE   "VII-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1244.2
114700         PERFORM FAIL.                                            SQ1244.2
114800 SEQ-TEST-10-03-END.                                              SQ1244.2
114900     MOVE    SPACE TO DELETE-SW-3.                                SQ1244.2
115000*                                                                 SQ1244.2
115100*                                                                 SQ1244.2
115200*    NOW EXECUTE A NORMAL CLOSE ON THE FILE.                      SQ1244.2
115300*    DELETION OF THE OPERATION DELETES ITS SUBORDINATE TESTS.     SQ1244.2
115400*                                                                 SQ1244.2
115500 SEQ-INIT-11.                                                     SQ1244.2
115600     MOVE    0 TO REC-CT.                                         SQ1244.2
115700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1244.2
115800     MOVE   "CLOSE AFTER READING" TO FEATURE.                     SQ1244.2
115900     MOVE   "SEQ-TEST-CL-11" TO PAR-NAME.                         SQ1244.2
116000     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1244.2
116100         GO TO SEQ-DELETE-11.                                     SQ1244.2
116200     GO TO   SEQ-TEST-CL-11.                                      SQ1244.2
116300 SEQ-DELETE-11.                                                   SQ1244.2
116400     MOVE   "*" TO DELETE-SW-3.                                   SQ1244.2
116500     GO TO   SEQ-DELETE-11-01.                                    SQ1244.2
116600 SEQ-TEST-CL-11.                                                  SQ1244.2
116700     CLOSE   SQ-FS4.                                              SQ1244.2
116800*                                                                 SQ1244.2
116900*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ1244.2
117000*                                                                 SQ1244.2
117100     ADD    1 TO REC-CT.                                          SQ1244.2
117200     IF DELETE-SW NOT = SPACE                                     SQ1244.2
117300         GO TO   SEQ-DELETE-11-01.                                SQ1244.2
117400     GO TO   SEQ-TEST-CL-11-01.                                   SQ1244.2
117500 SEQ-DELETE-11-01.                                                SQ1244.2
117600     PERFORM DE-LETE.                                             SQ1244.2
117700     GO TO   SEQ-TEST-11-01-END.                                  SQ1244.2
117800 SEQ-TEST-CL-11-01.                                               SQ1244.2
117900     IF SQ-FS4-STATUS = "00"                                      SQ1244.2
118000         PERFORM PASS                                             SQ1244.2
118100     ELSE                                                         SQ1244.2
118200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1244.2
118300         MOVE   "00" TO CORRECT-A                                 SQ1244.2
118400         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ1244.2
118500                   TO RE-MARK                                     SQ1244.2
118600         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1244.2
118700         PERFORM FAIL.                                            SQ1244.2
118800 SEQ-TEST-11-01-END.                                              SQ1244.2
118900*                                                                 SQ1244.2
119000 CCVS-EXIT SECTION.                                               SQ1244.2
119100 CCVS-999999.                                                     SQ1244.2
119200     GO TO   CLOSE-FILES.                                         SQ1244.2
      *END-OF,SQ124A                                                                  
