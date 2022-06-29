      *HEADER,COBOL,SQ123A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1234.2
000200 PROGRAM-ID.                                                      SQ1234.2
000300     SQ123A.                                                      SQ1234.2
000400****************************************************************  SQ1234.2
000500*                                                              *  SQ1234.2
000600*    VALIDATION FOR:-                                          *  SQ1234.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1234.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1234.2
000900*                             REVISED 1986, AUGUST             *  SQ1234.2
001000*                                                              *  SQ1234.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1234.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1234.2
001300*                                                              *  SQ1234.2
001400****************************************************************  SQ1234.2
001500*                                                              *  SQ1234.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1234.2
001700*                                                              *  SQ1234.2
001800*            X-14   SEQUENTIAL NON-UNIT MASS STORAGE FILE      *  SQ1234.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1234.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1234.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1234.2
002200*                                                              *  SQ1234.2
002300****************************************************************  SQ1234.2
002400*                                                              *  SQ1234.2
002500*    THIS PROGRAM OPENS FOR OUTPUT A FILE WHICH IS ASSIGNED    *  SQ1234.2
002600*    TO A MEDIUM WHICH IS NOT A REEL/UNIT MEDIUM.  A CLOSE     *  SQ1234.2
002700*    REEL STATEMENT IS EXECUTED.  THIS SHOULD HAVE NO EFFECT   *  SQ1234.2
002800*    ON THE FILE, EXCEPT TO CAUSE I-O STATUS 07.  THE FILE     *  SQ1234.2
002900*    SHOULD REMAIN OPEN.  A NORMAL, UNQUALIFIED, CLOSE         *  SQ1234.2
003000*    STATEMENT IS THEN EXECUTED, WHICH SHOULD BE SUCCESSFUL    *  SQ1234.2
003100*    AND CLOSE THE FILE.  THERE IS AN ERROR DECLARATIVE FOR    *  SQ1234.2
003200*    THE FILE, WHICH SHOULD NOT BE ENTERED.                    *  SQ1234.2
003300*                                                              *  SQ1234.2
003400****************************************************************  SQ1234.2
003500*                                                                 SQ1234.2
003600 ENVIRONMENT DIVISION.                                            SQ1234.2
003700 CONFIGURATION SECTION.                                           SQ1234.2
003800 SOURCE-COMPUTER.                                                 SQ1234.2
003900     XXXXX082.                                                    SQ1234.2
004000 OBJECT-COMPUTER.                                                 SQ1234.2
004100     XXXXX083.                                                    SQ1234.2
004200*                                                                 SQ1234.2
004300 INPUT-OUTPUT SECTION.                                            SQ1234.2
004400 FILE-CONTROL.                                                    SQ1234.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SQ1234.2
004600     XXXXX055.                                                    SQ1234.2
004700*                                                                 SQ1234.2
004800*    SELECT RAW-DATA   ASSIGN TO                                  SQ1234.2
004900*    XXXXX062                                                     SQ1234.2
005000*          ORGANIZATION IS INDEXED                                SQ1234.2
005100*          ACCESS MODE  IS RANDOM                                 SQ1234.2
005200*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1234.2
005300*                                                                 SQ1234.2
005400     SELECT SQ-FS4 ASSIGN                                         SQ1234.2
005500     XXXXX014                                                     SQ1234.2
005600            SEQUENTIAL                                            SQ1234.2
005700            ACCESS IS SEQUENTIAL                                  SQ1234.2
005800            STATUS IS SQ-FS4-STATUS.                              SQ1234.2
005900*                                                                 SQ1234.2
006000*                                                                 SQ1234.2
006100 DATA DIVISION.                                                   SQ1234.2
006200 FILE SECTION.                                                    SQ1234.2
006300 FD  PRINT-FILE                                                   SQ1234.2
006400C    LABEL RECORDS                                                SQ1234.2
006500C    XXXXX084                                                     SQ1234.2
006600C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1234.2
006700               .                                                  SQ1234.2
006800 01  PRINT-REC    PICTURE X(120).                                 SQ1234.2
006900 01  DUMMY-RECORD PICTURE X(120).                                 SQ1234.2
007000*                                                                 SQ1234.2
007100*FD  RAW-DATA.                                                    SQ1234.2
007200*01  RAW-DATA-SATZ.                                               SQ1234.2
007300*    05  RAW-DATA-KEY        PIC X(6).                            SQ1234.2
007400*    05  C-DATE              PIC 9(6).                            SQ1234.2
007500*    05  C-TIME              PIC 9(8).                            SQ1234.2
007600*    05  NO-OF-TESTS         PIC 99.                              SQ1234.2
007700*    05  C-OK                PIC 999.                             SQ1234.2
007800*    05  C-ALL               PIC 999.                             SQ1234.2
007900*    05  C-FAIL              PIC 999.                             SQ1234.2
008000*    05  C-DELETED           PIC 999.                             SQ1234.2
008100*    05  C-INSPECT           PIC 999.                             SQ1234.2
008200*    05  C-NOTE              PIC X(13).                           SQ1234.2
008300*    05  C-INDENT            PIC X.                               SQ1234.2
008400*    05  C-ABORT             PIC X(8).                            SQ1234.2
008500*                                                                 SQ1234.2
008600 FD  SQ-FS4                                                       SQ1234.2
008700C    LABEL RECORD IS STANDARD                                     SQ1234.2
008800                .                                                 SQ1234.2
008900 01  SQ-FS4R1-F-G-120  PIC X(120).                                SQ1234.2
009000*                                                                 SQ1234.2
009100 WORKING-STORAGE SECTION.                                         SQ1234.2
009200*                                                                 SQ1234.2
009300***************************************************************   SQ1234.2
009400*                                                             *   SQ1234.2
009500*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1234.2
009600*                                                             *   SQ1234.2
009700***************************************************************   SQ1234.2
009800*                                                                 SQ1234.2
009900 01  SQ-FS4-STATUS.                                               SQ1234.2
010000   03  SQ-FS4-KEY-1   PIC X.                                      SQ1234.2
010100   03  SQ-FS4-KEY-2   PIC X.                                      SQ1234.2
010200*                                                                 SQ1234.2
010300 01  DELETE-SW.                                                   SQ1234.2
010400     03  DELETE-SW-1 PIC X.                                       SQ1234.2
010500     03  DELETE-SW-1-GROUP.                                       SQ1234.2
010600         05  DELETE-SW-2 PIC X.                                   SQ1234.2
010700*                                                                 SQ1234.2
010800 01  DECL-EXEC-I  PIC X(12).                                      SQ1234.2
010900 01  DECL-EXEC-O  PIC X(12).                                      SQ1234.2
011000 01  DECL-EXEC-SW PIC X.                                          SQ1234.2
011100*                                                                 SQ1234.2
011200***************************************************************   SQ1234.2
011300*                                                             *   SQ1234.2
011400*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1234.2
011500*                                                             *   SQ1234.2
011600***************************************************************   SQ1234.2
011700*                                                                 SQ1234.2
011800 01  REC-SKEL-SUB   PIC 99.                                       SQ1234.2
011900*                                                                 SQ1234.2
012000 01  FILE-RECORD-INFORMATION-REC.                                 SQ1234.2
012100     03 FILE-RECORD-INFO-SKELETON.                                SQ1234.2
012200        05 FILLER                 PICTURE X(48)       VALUE       SQ1234.2
012300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1234.2
012400        05 FILLER                 PICTURE X(46)       VALUE       SQ1234.2
012500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1234.2
012600        05 FILLER                 PICTURE X(26)       VALUE       SQ1234.2
012700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1234.2
012800        05 FILLER                 PICTURE X(37)       VALUE       SQ1234.2
012900             ",RECKEY=                             ".             SQ1234.2
013000        05 FILLER                 PICTURE X(38)       VALUE       SQ1234.2
013100             ",ALTKEY1=                             ".            SQ1234.2
013200        05 FILLER                 PICTURE X(38)       VALUE       SQ1234.2
013300             ",ALTKEY2=                             ".            SQ1234.2
013400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1234.2
013500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1234.2
013600        05 FILE-RECORD-INFO-P1-120.                               SQ1234.2
013700           07 FILLER              PIC X(5).                       SQ1234.2
013800           07 XFILE-NAME          PIC X(6).                       SQ1234.2
013900           07 FILLER              PIC X(8).                       SQ1234.2
014000           07 XRECORD-NAME        PIC X(6).                       SQ1234.2
014100           07 FILLER              PIC X(1).                       SQ1234.2
014200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1234.2
014300           07 FILLER              PIC X(7).                       SQ1234.2
014400           07 XRECORD-NUMBER      PIC 9(6).                       SQ1234.2
014500           07 FILLER              PIC X(6).                       SQ1234.2
014600           07 UPDATE-NUMBER       PIC 9(2).                       SQ1234.2
014700           07 FILLER              PIC X(5).                       SQ1234.2
014800           07 ODO-NUMBER          PIC 9(4).                       SQ1234.2
014900           07 FILLER              PIC X(5).                       SQ1234.2
015000           07 XPROGRAM-NAME       PIC X(5).                       SQ1234.2
015100           07 FILLER              PIC X(7).                       SQ1234.2
015200           07 XRECORD-LENGTH      PIC 9(6).                       SQ1234.2
015300           07 FILLER              PIC X(7).                       SQ1234.2
015400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1234.2
015500           07 FILLER              PIC X(1).                       SQ1234.2
015600           07 XBLOCK-SIZE         PIC 9(4).                       SQ1234.2
015700           07 FILLER              PIC X(6).                       SQ1234.2
015800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1234.2
015900           07 FILLER              PIC X(5).                       SQ1234.2
016000           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1234.2
016100           07 FILLER              PIC X(6).                       SQ1234.2
016200           07 XLABEL-TYPE         PIC X(1).                       SQ1234.2
016300        05 FILE-RECORD-INFO-P121-240.                             SQ1234.2
016400           07 FILLER              PIC X(8).                       SQ1234.2
016500           07 XRECORD-KEY         PIC X(29).                      SQ1234.2
016600           07 FILLER              PIC X(9).                       SQ1234.2
016700           07 ALTERNATE-KEY1      PIC X(29).                      SQ1234.2
016800           07 FILLER              PIC X(9).                       SQ1234.2
016900           07 ALTERNATE-KEY2      PIC X(29).                      SQ1234.2
017000           07 FILLER              PIC X(7).                       SQ1234.2
017100*                                                                 SQ1234.2
017200 01  TEST-RESULTS.                                                SQ1234.2
017300     02 FILLER              PIC X      VALUE SPACE.               SQ1234.2
017400     02  PAR-NAME.                                                SQ1234.2
017500       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1234.2
017600       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1234.2
017700       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1234.2
017800     02 FILLER              PIC X      VALUE SPACE.               SQ1234.2
017900     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1234.2
018000     02 FILLER              PIC X      VALUE SPACE.               SQ1234.2
018100     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1234.2
018200     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1234.2
018300     02 RE-MARK             PIC X(61).                            SQ1234.2
018400 01  TEST-COMPUTED.                                               SQ1234.2
018500   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1234.2
018600   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1234.2
018700   02 COMPUTED-X.                                                 SQ1234.2
018800     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1234.2
018900     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1234.2
019000     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1234.2
019100     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1234.2
019200     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1234.2
019300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1234.2
019400        04 COMPUTED-18V0                   PIC -9(18).            SQ1234.2
019500        04 FILLER                          PIC X.                 SQ1234.2
019600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1234.2
019700 01  TEST-CORRECT.                                                SQ1234.2
019800     02 FILLER PIC X(30) VALUE SPACE.                             SQ1234.2
019900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1234.2
020000     02 CORRECT-X.                                                SQ1234.2
020100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1234.2
020200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1234.2
020300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1234.2
020400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1234.2
020500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1234.2
020600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1234.2
020700         04 CORRECT-18V0                     PIC -9(18).          SQ1234.2
020800         04 FILLER                           PIC X.               SQ1234.2
020900     03 FILLER PIC X(2) VALUE SPACE.                              SQ1234.2
021000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1234.2
021100*                                                                 SQ1234.2
021200 01  CCVS-C-1.                                                    SQ1234.2
021300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1234.2
021400     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1234.2
021500     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1234.2
021600     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1234.2
021700     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1234.2
021800     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1234.2
021900     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1234.2
022000     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1234.2
022100 01  CCVS-C-2.                                                    SQ1234.2
022200     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1234.2
022300     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1234.2
022400     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1234.2
022500     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1234.2
022600     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1234.2
022700*                                                                 SQ1234.2
022800 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1234.2
022900 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1234.2
023000 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1234.2
023100 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1234.2
023200 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1234.2
023300 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1234.2
023400 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1234.2
023500 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1234.2
023600 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1234.2
023700 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1234.2
023800 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1234.2
023900 01  CCVS-H-1.                                                    SQ1234.2
024000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1234.2
024100     02  FILLER          PIC X(42)    VALUE                       SQ1234.2
024200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1234.2
024300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1234.2
024400 01  CCVS-H-2A.                                                   SQ1234.2
024500   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1234.2
024600   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1234.2
024700   02  FILLER            PIC XXXX   VALUE                         SQ1234.2
024800     "4.2 ".                                                      SQ1234.2
024900   02  FILLER            PIC X(28)  VALUE                         SQ1234.2
025000            " COPY - NOT FOR DISTRIBUTION".                       SQ1234.2
025100   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1234.2
025200*                                                                 SQ1234.2
025300 01  CCVS-H-2B.                                                   SQ1234.2
025400   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1234.2
025500   02  TEST-ID           PIC X(9).                                SQ1234.2
025600   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1234.2
025700   02  FILLER            PIC X(12)  VALUE                         SQ1234.2
025800     " HIGH       ".                                              SQ1234.2
025900   02  FILLER            PIC X(22)  VALUE                         SQ1234.2
026000            " LEVEL VALIDATION FOR ".                             SQ1234.2
026100   02  FILLER            PIC X(58)  VALUE                         SQ1234.2
026200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1234.2
026300 01  CCVS-H-3.                                                    SQ1234.2
026400     02  FILLER          PIC X(34)  VALUE                         SQ1234.2
026500            " FOR OFFICIAL USE ONLY    ".                         SQ1234.2
026600     02  FILLER          PIC X(58)  VALUE                         SQ1234.2
026700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1234.2
026800     02  FILLER          PIC X(28)  VALUE                         SQ1234.2
026900            "  COPYRIGHT   1985,1986 ".                           SQ1234.2
027000 01  CCVS-E-1.                                                    SQ1234.2
027100     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1234.2
027200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1234.2
027300     02 ID-AGAIN         PIC X(9).                                SQ1234.2
027400     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1234.2
027500 01  CCVS-E-2.                                                    SQ1234.2
027600     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1234.2
027700     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1234.2
027800     02  CCVS-E-2-2.                                              SQ1234.2
027900         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1234.2
028000         03 FILLER         PIC X      VALUE SPACE.                SQ1234.2
028100         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1234.2
028200            "ERRORS ENCOUNTERED".                                 SQ1234.2
028300 01  CCVS-E-3.                                                    SQ1234.2
028400     02  FILLER          PIC X(22)  VALUE                         SQ1234.2
028500            " FOR OFFICIAL USE ONLY".                             SQ1234.2
028600     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1234.2
028700     02  FILLER          PIC X(58)  VALUE                         SQ1234.2
028800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1234.2
028900     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1234.2
029000     02  FILLER          PIC X(20)  VALUE                         SQ1234.2
029100             " COPYRIGHT 1985,1986".                              SQ1234.2
029200 01  CCVS-E-4.                                                    SQ1234.2
029300     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1234.2
029400     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1234.2
029500     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1234.2
029600     02 FILLER           PIC X(40)  VALUE                         SQ1234.2
029700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1234.2
029800 01  XXINFO.                                                      SQ1234.2
029900     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1234.2
030000     02 INFO-TEXT.                                                SQ1234.2
030100       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1234.2
030200       04 XXCOMPUTED         PIC X(20).                           SQ1234.2
030300       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1234.2
030400       04 XXCORRECT          PIC X(20).                           SQ1234.2
030500     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1234.2
030600 01  HYPHEN-LINE.                                                 SQ1234.2
030700     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1234.2
030800     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1234.2
030900-    "*****************************************".                 SQ1234.2
031000     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1234.2
031100-    "******************************".                            SQ1234.2
031200 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1234.2
031300     "SQ123A".                                                    SQ1234.2
031400*                                                                 SQ1234.2
031500*                                                                 SQ1234.2
031600 PROCEDURE DIVISION.                                              SQ1234.2
031700 DECLARATIVES.                                                    SQ1234.2
031800*                                                                 SQ1234.2
031900*    FIRST, A DECLARATIVE FOR THE PRINT FILE, IN CASE THERE       SQ1234.2
032000*    IS AN ERROR ON PRINTING DURING EXECUTION OF THE OUTPUT       SQ1234.2
032100*    DECLARATIVE.  PAGE VII-51, 4.6.4(7) STATES THAT THE          SQ1234.2
032200*    INVOCATION OF THE SAME DECLARATIVE TWICE IS ILLEGAL.         SQ1234.2
032300*                                                                 SQ1234.2
032400 SECT-SQ123A-0000 SECTION.                                        SQ1234.2
032500     USE AFTER STANDARD ERROR PROCEDURE ON PRINT-FILE.            SQ1234.2
032600 PRINT-FILE-ERROR-PROCESS.                                        SQ1234.2
032700     EXIT.                                                        SQ1234.2
032800*                                                                 SQ1234.2
032900 SECT-SQ123A-0001 SECTION.                                        SQ1234.2
033000     USE AFTER EXCEPTION PROCEDURE INPUT.                         SQ1234.2
033100 INPUT-ERROR-PROCESS.                                             SQ1234.2
033200     MOVE   "EXECUTED" TO DECL-EXEC-I.                            SQ1234.2
033300*                                                                 SQ1234.2
033400 SECT-SQ123A-0002 SECTION.                                        SQ1234.2
033500     USE AFTER STANDARD EXCEPTION PROCEDURE OUTPUT.               SQ1234.2
033600 OUTPUT-ERROR-PROCESS.                                            SQ1234.2
033700     MOVE   "EXECUTED" TO DECL-EXEC-O.                            SQ1234.2
033800*                                                                 SQ1234.2
033900     IF DECL-EXEC-SW NOT = SPACE                                  SQ1234.2
034000         GO TO   END-DECLS.                                       SQ1234.2
034100*                                                                 SQ1234.2
034200     MOVE    1 TO REC-CT.                                         SQ1234.2
034300     MOVE   "CLOSE AFTER CLOSE REEL"  TO FEATURE.                 SQ1234.2
034400     MOVE   "DECL-CLOSE-02" TO PAR-NAME.                          SQ1234.2
034500     GO TO   DECL-CLOSE-02.                                       SQ1234.2
034600 DECL-DELETE-02.                                                  SQ1234.2
034700     PERFORM DECL-DE-LETE.                                        SQ1234.2
034800     GO TO   DECL-TEST-01-END.                                    SQ1234.2
034900 DECL-CLOSE-02.                                                   SQ1234.2
035000     MOVE    SQ-FS4-STATUS TO COMPUTED-A                          SQ1234.2
035100     MOVE   "00" TO CORRECT-A                                     SQ1234.2
035200     MOVE   "DECLARATIVE ENTERED ON CLOSE OF FILE WHICH IS OPEN"  SQ1234.2
035300               TO RE-MARK                                         SQ1234.2
035400     MOVE   "VII-3, VII-38,4.2.4(3)F" TO ANSI-REFERENCE           SQ1234.2
035500     PERFORM DECL-FAIL.                                           SQ1234.2
035600 DECL-TEST-01-END.                                                SQ1234.2
035700*                                                                 SQ1234.2
035800     GO TO   END-DECLS.                                           SQ1234.2
035900*                                                                 SQ1234.2
036000*                                                                 SQ1234.2
036100 DECL-PASS.                                                       SQ1234.2
036200     MOVE   "PASS " TO P-OR-F.                                    SQ1234.2
036300     ADD     1 TO PASS-COUNTER.                                   SQ1234.2
036400     PERFORM DECL-PRINT-DETAIL.                                   SQ1234.2
036500*                                                                 SQ1234.2
036600 DECL-FAIL.                                                       SQ1234.2
036700     MOVE   "FAIL*" TO P-OR-F.                                    SQ1234.2
036800     ADD     1 TO ERROR-COUNTER.                                  SQ1234.2
036900     PERFORM DECL-PRINT-DETAIL.                                   SQ1234.2
037000*                                                                 SQ1234.2
037100 DECL-DE-LETE.                                                    SQ1234.2
037200     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1234.2
037300     MOVE   "*****" TO P-OR-F.                                    SQ1234.2
037400     ADD     1 TO DELETE-COUNTER.                                 SQ1234.2
037500     PERFORM DECL-PRINT-DETAIL.                                   SQ1234.2
037600*                                                                 SQ1234.2
037700 DECL-PRINT-DETAIL.                                               SQ1234.2
037800     IF REC-CT NOT EQUAL TO ZERO                                  SQ1234.2
037900             MOVE "." TO PARDOT-X                                 SQ1234.2
038000             MOVE REC-CT TO DOTVALUE.                             SQ1234.2
038100     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1234.2
038200     PERFORM DECL-WRITE-LINE.                                     SQ1234.2
038300     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1234.2
038400         PERFORM DECL-WRITE-LINE                                  SQ1234.2
038500         PERFORM DECL-FAIL-ROUTINE THRU DECL-FAIL-EX              SQ1234.2
038600     ELSE                                                         SQ1234.2
038700         PERFORM DECL-BAIL THRU DECL-BAIL-EX.                     SQ1234.2
038800     MOVE    SPACE TO P-OR-F.                                     SQ1234.2
038900     MOVE    SPACE TO COMPUTED-X.                                 SQ1234.2
039000     MOVE    SPACE TO CORRECT-X.                                  SQ1234.2
039100     IF REC-CT EQUAL TO ZERO                                      SQ1234.2
039200         MOVE    SPACE TO PAR-NAME.                               SQ1234.2
039300     MOVE    SPACE TO RE-MARK.                                    SQ1234.2
039400*                                                                 SQ1234.2
039500 DECL-WRITE-LINE.                                                 SQ1234.2
039600     ADD     1 TO RECORD-COUNT.                                   SQ1234.2
039700*    IF RECORD-COUNT GREATER 50                                   SQ1234.2
039800*        MOVE    DUMMY-RECORD TO DUMMY-HOLD                       SQ1234.2
039900*        MOVE    SPACE TO DUMMY-RECORD                            SQ1234.2
040000*        WRITE   DUMMY-RECORD AFTER ADVANCING PAGE                SQ1234.2
040100*        MOVE    CCVS-C-1 TO DUMMY-RECORD PERFORM DECL-WRT-LN     SQ1234.2
040200*        MOVE    CCVS-C-2 TO DUMMY-RECORD                         SQ1234.2
040300*        PERFORM DECL-WRT-LN 2 TIMES                              SQ1234.2
040400*        MOVE    HYPHEN-LINE TO DUMMY-RECORD                      SQ1234.2
040500*        PERFORM DECL-WRT-LN                                      SQ1234.2
040600*        MOVE    DUMMY-HOLD TO DUMMY-RECORD                       SQ1234.2
040700*        MOVE    ZERO TO RECORD-COUNT.                            SQ1234.2
040800     PERFORM DECL-WRT-LN.                                         SQ1234.2
040900*                                                                 SQ1234.2
041000 DECL-WRT-LN.                                                     SQ1234.2
041100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1234.2
041200     MOVE    SPACE TO DUMMY-RECORD.                               SQ1234.2
041300*                                                                 SQ1234.2
041400 DECL-FAIL-ROUTINE.                                               SQ1234.2
041500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.      SQ1234.2
041600     IF CORRECT-X NOT EQUAL TO SPACE GO TO DECL-FAIL-WRITE.       SQ1234.2
041700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1234.2
041800     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1234.2
041900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1234.2
042000     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1234.2
042100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1234.2
042200     GO TO   DECL-FAIL-EX.                                        SQ1234.2
042300 DECL-FAIL-WRITE.                                                 SQ1234.2
042400     MOVE    TEST-COMPUTED TO PRINT-REC                           SQ1234.2
042500     PERFORM DECL-WRITE-LINE                                      SQ1234.2
042600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1234.2
042700     MOVE    TEST-CORRECT TO PRINT-REC                            SQ1234.2
042800     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1234.2
042900     MOVE    SPACES TO COR-ANSI-REFERENCE.                        SQ1234.2
043000 DECL-FAIL-EX.                                                    SQ1234.2
043100     EXIT.                                                        SQ1234.2
043200*                                                                 SQ1234.2
043300 DECL-BAIL.                                                       SQ1234.2
043400     IF COMPUTED-A NOT EQUAL TO SPACE GO TO DECL-BAIL-WRITE.      SQ1234.2
043500     IF CORRECT-A EQUAL TO SPACE GO TO DECL-BAIL-EX.              SQ1234.2
043600 DECL-BAIL-WRITE.                                                 SQ1234.2
043700     MOVE    CORRECT-A TO XXCORRECT.                              SQ1234.2
043800     MOVE    COMPUTED-A TO XXCOMPUTED.                            SQ1234.2
043900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1234.2
044000     PERFORM DECL-WRITE-LINE 2 TIMES.                             SQ1234.2
044100 DECL-BAIL-EX.                                                    SQ1234.2
044200     EXIT.                                                        SQ1234.2
044300*                                                                 SQ1234.2
044400 END-DECLS.                                                       SQ1234.2
044500 END DECLARATIVES.                                                SQ1234.2
044600*                                                                 SQ1234.2
044700*                                                                 SQ1234.2
044800 CCVS1 SECTION.                                                   SQ1234.2
044900 OPEN-FILES.                                                      SQ1234.2
045000*    OPEN    I-O RAW-DATA.                                        SQ1234.2
045100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1234.2
045200*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1234.2
045300*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1234.2
045400*    ADD     1           TO C-NO-OF-TESTS.                        SQ1234.2
045500*    ACCEPT  C-DATE      FROM DATE.                               SQ1234.2
045600*    ACCEPT  C-TIME      FROM TIME.                               SQ1234.2
045700*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1234.2
045800*END-E-1.                                                         SQ1234.2
045900*    CLOSE   RAW-DATA.                                            SQ1234.2
046000     OPEN    OUTPUT PRINT-FILE.                                   SQ1234.2
046100     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1234.2
046200     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1234.2
046300     MOVE    SPACE TO TEST-RESULTS.                               SQ1234.2
046400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1234.2
046500     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1234.2
046600     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1234.2
046700     GO TO CCVS1-EXIT.                                            SQ1234.2
046800*                                                                 SQ1234.2
046900 CCVS-INIT-FILE.                                                  SQ1234.2
047000     ADD     1 TO REC-SKL-SUB.                                    SQ1234.2
047100     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1234.2
047200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1234.2
047300*                                                                 SQ1234.2
047400 CLOSE-FILES.                                                     SQ1234.2
047500     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1234.2
047600     CLOSE   PRINT-FILE.                                          SQ1234.2
047700*    OPEN    I-O RAW-DATA.                                        SQ1234.2
047800*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1234.2
047900*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1234.2
048000*    MOVE   "OK.     " TO C-ABORT.                                SQ1234.2
048100*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1234.2
048200*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1234.2
048300*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1234.2
048400*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1234.2
048500*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1234.2
048600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1234.2
048700*END-E-2.                                                         SQ1234.2
048800*    CLOSE   RAW-DATA.                                            SQ1234.2
048900 TERMINATE-CCVS.                                                  SQ1234.2
049000*    EXIT    PROGRAM.                                             SQ1234.2
049100     STOP    RUN.                                                 SQ1234.2
049200*                                                                 SQ1234.2
049300 INSPT.                                                           SQ1234.2
049400     MOVE   "INSPT" TO P-OR-F.                                    SQ1234.2
049500     ADD     1 TO INSPECT-COUNTER.                                SQ1234.2
049600     PERFORM PRINT-DETAIL.                                        SQ1234.2
049700*                                                                 SQ1234.2
049800 PASS.                                                            SQ1234.2
049900     MOVE   "PASS " TO P-OR-F.                                    SQ1234.2
050000     ADD     1 TO PASS-COUNTER.                                   SQ1234.2
050100     PERFORM PRINT-DETAIL.                                        SQ1234.2
050200*                                                                 SQ1234.2
050300 FAIL.                                                            SQ1234.2
050400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1234.2
050500     ADD     1 TO ERROR-COUNTER.                                  SQ1234.2
050600     PERFORM PRINT-DETAIL.                                        SQ1234.2
050700*                                                                 SQ1234.2
050800 DE-LETE.                                                         SQ1234.2
050900     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1234.2
051000     MOVE   "*****" TO P-OR-F.                                    SQ1234.2
051100     ADD     1 TO DELETE-COUNTER.                                 SQ1234.2
051200     PERFORM PRINT-DETAIL.                                        SQ1234.2
051300*                                                                 SQ1234.2
051400 PRINT-DETAIL.                                                    SQ1234.2
051500     IF REC-CT NOT EQUAL TO ZERO                                  SQ1234.2
051600         MOVE   "." TO PARDOT-X                                   SQ1234.2
051700         MOVE    REC-CT TO DOTVALUE.                              SQ1234.2
051800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1234.2
051900     PERFORM WRITE-LINE.                                          SQ1234.2
052000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1234.2
052100         PERFORM WRITE-LINE                                       SQ1234.2
052200         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1234.2
052300     ELSE                                                         SQ1234.2
052400         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1234.2
052500     MOVE    SPACE TO P-OR-F.                                     SQ1234.2
052600     MOVE    SPACE TO COMPUTED-X.                                 SQ1234.2
052700     MOVE    SPACE TO CORRECT-X.                                  SQ1234.2
052800     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1234.2
052900     MOVE    SPACE TO RE-MARK.                                    SQ1234.2
053000*                                                                 SQ1234.2
053100 HEAD-ROUTINE.                                                    SQ1234.2
053200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1234.2
053300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1234.2
053400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1234.2
053500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1234.2
053600 COLUMN-NAMES-ROUTINE.                                            SQ1234.2
053700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1234.2
053800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1234.2
053900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1234.2
054000 END-ROUTINE.                                                     SQ1234.2
054100     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1234.2
054200     PERFORM WRITE-LINE 5 TIMES.                                  SQ1234.2
054300 END-RTN-EXIT.                                                    SQ1234.2
054400     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1234.2
054500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1234.2
054600*                                                                 SQ1234.2
054700 END-ROUTINE-1.                                                   SQ1234.2
054800     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1234.2
054900     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1234.2
055000     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1234.2
055100     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1234.2
055200     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1234.2
055300     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1234.2
055400     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1234.2
055500     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1234.2
055600     PERFORM WRITE-LINE.                                          SQ1234.2
055700     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1234.2
055800     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1234.2
055900         MOVE   "NO " TO ERROR-TOTAL                              SQ1234.2
056000     ELSE                                                         SQ1234.2
056100         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1234.2
056200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1234.2
056300     PERFORM WRITE-LINE.                                          SQ1234.2
056400 END-ROUTINE-13.                                                  SQ1234.2
056500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1234.2
056600         MOVE   "NO " TO ERROR-TOTAL                              SQ1234.2
056700     ELSE                                                         SQ1234.2
056800         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1234.2
056900     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1234.2
057000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1234.2
057100     PERFORM WRITE-LINE.                                          SQ1234.2
057200     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1234.2
057300         MOVE   "NO " TO ERROR-TOTAL                              SQ1234.2
057400     ELSE                                                         SQ1234.2
057500         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1234.2
057600     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1234.2
057700     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1234.2
057800     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1234.2
057900*                                                                 SQ1234.2
058000 WRITE-LINE.                                                      SQ1234.2
058100     ADD     1 TO RECORD-COUNT.                                   SQ1234.2
058200*    IF RECORD-COUNT GREATER 50                                   SQ1234.2
058300*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1234.2
058400*        MOVE  SPACE TO DUMMY-RECORD                              SQ1234.2
058500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1234.2
058600*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1234.2
058700*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1234.2
058800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1234.2
058900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1234.2
059000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1234.2
059100     PERFORM WRT-LN.                                              SQ1234.2
059200*                                                                 SQ1234.2
059300 WRT-LN.                                                          SQ1234.2
059400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1234.2
059500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1234.2
059600 BLANK-LINE-PRINT.                                                SQ1234.2
059700     PERFORM WRT-LN.                                              SQ1234.2
059800 FAIL-ROUTINE.                                                    SQ1234.2
059900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1234.2
060000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1234.2
060100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1234.2
060200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1234.2
060300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1234.2
060400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1234.2
060500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1234.2
060600     GO TO   FAIL-ROUTINE-EX.                                     SQ1234.2
060700 FAIL-ROUTINE-WRITE.                                              SQ1234.2
060800     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1234.2
060900     PERFORM WRITE-LINE                                           SQ1234.2
061000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1234.2
061100     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1234.2
061200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1234.2
061300     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1234.2
061400 FAIL-ROUTINE-EX.                                                 SQ1234.2
061500     EXIT.                                                        SQ1234.2
061600 BAIL-OUT.                                                        SQ1234.2
061700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1234.2
061800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1234.2
061900 BAIL-OUT-WRITE.                                                  SQ1234.2
062000     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1234.2
062100     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1234.2
062200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1234.2
062300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1234.2
062400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1234.2
062500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1234.2
062600 BAIL-OUT-EX.                                                     SQ1234.2
062700     EXIT.                                                        SQ1234.2
062800 CCVS1-EXIT.                                                      SQ1234.2
062900     EXIT.                                                        SQ1234.2
063000*                                                                 SQ1234.2
063100****************************************************************  SQ1234.2
063200*                                                              *  SQ1234.2
063300*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1234.2
063400*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1234.2
063500*                                                              *  SQ1234.2
063600****************************************************************  SQ1234.2
063700*                                                                 SQ1234.2
063800 SECT-SQ123A-0004 SECTION.                                        SQ1234.2
063900 STA-INIT.                                                        SQ1234.2
064000     MOVE    SPACE TO DELETE-SW.                                  SQ1234.2
064100*                                                                 SQ1234.2
064200     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1234.2
064300     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1234.2
064400     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1234.2
064500     MOVE    125 TO XRECORD-LENGTH (1).                           SQ1234.2
064600     MOVE   "RC" TO CHARS-OR-RECORDS (1).                         SQ1234.2
064700     MOVE    1   TO XBLOCK-SIZE (1).                              SQ1234.2
064800     MOVE    0   TO RECORDS-IN-FILE (1).                          SQ1234.2
064900     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1234.2
065000     MOVE   "S"  TO XLABEL-TYPE (1).                              SQ1234.2
065100     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1234.2
065200*                                                                 SQ1234.2
065300*    OPEN THE FILE IN THE OUTPUT MODE                             SQ1234.2
065400*                                                                 SQ1234.2
065500 SEQ-INIT-01.                                                     SQ1234.2
065600     MOVE    0 TO REC-CT.                                         SQ1234.2
065700     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1234.2
065800     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1234.2
065900     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1234.2
066000     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1234.2
066100     MOVE   "OPEN, CREATE FILE"  TO FEATURE.                      SQ1234.2
066200     MOVE   "SEQ-TEST-OP-01" TO PAR-NAME.                         SQ1234.2
066300     GO TO   SEQ-TEST-OP-01.                                      SQ1234.2
066400 SEQ-DELETE-01.                                                   SQ1234.2
066500     MOVE   "*" TO DELETE-SW-1.                                   SQ1234.2
066600     GO TO   SEQ-DELETE-01-01.                                    SQ1234.2
066700 SEQ-TEST-OP-01.                                                  SQ1234.2
066800     OPEN    OUTPUT SQ-FS4.                                       SQ1234.2
066900*                                                                 SQ1234.2
067000*    CHECK I-O STATUS RETURNED FROM OPEN OUTPUT                   SQ1234.2
067100*                                                                 SQ1234.2
067200     ADD    1 TO REC-CT.                                          SQ1234.2
067300     IF DELETE-SW NOT = SPACE                                     SQ1234.2
067400         GO TO   SEQ-DELETE-01-01.                                SQ1234.2
067500     GO TO   SEQ-TEST-OP-01-01.                                   SQ1234.2
067600 SEQ-DELETE-01-01.                                                SQ1234.2
067700     PERFORM DE-LETE.                                             SQ1234.2
067800     GO TO   SEQ-TEST-01-01-END.                                  SQ1234.2
067900 SEQ-TEST-OP-01-01.                                               SQ1234.2
068000     IF SQ-FS4-STATUS = "00"                                      SQ1234.2
068100         PERFORM PASS                                             SQ1234.2
068200     ELSE                                                         SQ1234.2
068300         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1234.2
068400         MOVE   "00" TO CORRECT-A                                 SQ1234.2
068500         MOVE   "UNEXPECTED ERROR CODE FROM OPEN OUTPUT"          SQ1234.2
068600                   TO RE-MARK                                     SQ1234.2
068700         MOVE   "VII-3, VII-23" TO ANSI-REFERENCE                 SQ1234.2
068800         PERFORM FAIL.                                            SQ1234.2
068900 SEQ-TEST-01-01-END.                                              SQ1234.2
069000*                                                                 SQ1234.2
069100*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ1234.2
069200*                                                                 SQ1234.2
069300     ADD     1 TO REC-CT.                                         SQ1234.2
069400     IF DELETE-SW NOT = SPACE                                     SQ1234.2
069500         GO TO   SEQ-DELETE-01-02.                                SQ1234.2
069600     GO TO   SEQ-TEST-OP-01-02.                                   SQ1234.2
069700 SEQ-DELETE-01-02.                                                SQ1234.2
069800     PERFORM DE-LETE.                                             SQ1234.2
069900     GO TO   SEQ-TEST-01-02-END.                                  SQ1234.2
070000 SEQ-TEST-OP-01-02.                                               SQ1234.2
070100     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ1234.2
070200         PERFORM PASS                                             SQ1234.2
070300     ELSE                                                         SQ1234.2
070400         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ1234.2
070500         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1234.2
070600         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ1234.2
070700                   TO RE-MARK                                     SQ1234.2
070800         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1234.2
070900         PERFORM FAIL.                                            SQ1234.2
071000 SEQ-TEST-01-02-END.                                              SQ1234.2
071100*                                                                 SQ1234.2
071200*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ1234.2
071300*                                                                 SQ1234.2
071400     ADD     1 TO REC-CT.                                         SQ1234.2
071500     IF DELETE-SW NOT = SPACE                                     SQ1234.2
071600         GO TO   SEQ-DELETE-01-03.                                SQ1234.2
071700     GO TO   SEQ-TEST-OP-01-03.                                   SQ1234.2
071800 SEQ-DELETE-01-03.                                                SQ1234.2
071900     PERFORM DE-LETE.                                             SQ1234.2
072000     GO TO   SEQ-TEST-01-03-END.                                  SQ1234.2
072100 SEQ-TEST-OP-01-03.                                               SQ1234.2
072200     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ1234.2
072300         PERFORM PASS                                             SQ1234.2
072400     ELSE                                                         SQ1234.2
072500         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ1234.2
072600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1234.2
072700         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ1234.2
072800                   TO RE-MARK                                     SQ1234.2
072900         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1234.2
073000         PERFORM FAIL.                                            SQ1234.2
073100 SEQ-TEST-01-03-END.                                              SQ1234.2
073200*                                                                 SQ1234.2
073300*                                                                 SQ1234.2
073400*    THE FILE HAS BEEN CREATED.  WE NOW EXECUTE CLOSE REEL        SQ1234.2
073500*                                                                 SQ1234.2
073600 SEQ-INIT-02.                                                     SQ1234.2
073700     MOVE    0 TO REC-CT.                                         SQ1234.2
073800     MOVE   "*"  TO DECL-EXEC-SW.                                 SQ1234.2
073900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1234.2
074000     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1234.2
074100     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1234.2
074200     MOVE   "CLOSE REEL, NON-REEL"  TO FEATURE.                   SQ1234.2
074300     MOVE   "SEQ-TEST-CR-02" TO PAR-NAME.                         SQ1234.2
074400     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1234.2
074500         GO TO SEQ-DELETE-02.                                     SQ1234.2
074600     GO TO   SEQ-TEST-CR-02.                                      SQ1234.2
074700 SEQ-DELETE-02.                                                   SQ1234.2
074800     MOVE   "*" TO DELETE-SW-2.                                   SQ1234.2
074900     GO TO   SEQ-DELETE-02-01.                                    SQ1234.2
075000 SEQ-TEST-CR-02.                                                  SQ1234.2
075100     CLOSE   SQ-FS4 REEL.                                         SQ1234.2
075200*                                                                 SQ1234.2
075300*    CHECK I-O STATUS RETURNED FROM CLOSE REEL                    SQ1234.2
075400*                                                                 SQ1234.2
075500     ADD    1 TO REC-CT.                                          SQ1234.2
075600     IF DELETE-SW NOT = SPACE                                     SQ1234.2
075700         GO TO   SEQ-DELETE-02-01.                                SQ1234.2
075800     GO TO   SEQ-TEST-CR-02-01.                                   SQ1234.2
075900 SEQ-DELETE-02-01.                                                SQ1234.2
076000     PERFORM DE-LETE.                                             SQ1234.2
076100     GO TO   SEQ-TEST-02-01-END.                                  SQ1234.2
076200 SEQ-TEST-CR-02-01.                                               SQ1234.2
076300     IF SQ-FS4-STATUS = "07"                                      SQ1234.2
076400         PERFORM PASS                                             SQ1234.2
076500     ELSE                                                         SQ1234.2
076600         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1234.2
076700         MOVE   "07" TO CORRECT-A                                 SQ1234.2
076800         MOVE   "UNEXPECTED I-O STATUS FROM CLOSE REEL"           SQ1234.2
076900                   TO RE-MARK                                     SQ1234.2
077000         MOVE   "VII-3, VII-38,4.2.4(3)F" TO ANSI-REFERENCE       SQ1234.2
077100         PERFORM FAIL.                                            SQ1234.2
077200 SEQ-TEST-02-01-END.                                              SQ1234.2
077300*                                                                 SQ1234.2
077400*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ1234.2
077500*                                                                 SQ1234.2
077600     ADD     1 TO REC-CT.                                         SQ1234.2
077700     IF DELETE-SW NOT = SPACE                                     SQ1234.2
077800         GO TO   SEQ-DELETE-02-02.                                SQ1234.2
077900     GO TO   SEQ-TEST-CR-02-02.                                   SQ1234.2
078000 SEQ-DELETE-02-02.                                                SQ1234.2
078100     PERFORM DE-LETE.                                             SQ1234.2
078200     GO TO   SEQ-TEST-02-02-END.                                  SQ1234.2
078300 SEQ-TEST-CR-02-02.                                               SQ1234.2
078400     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ1234.2
078500         PERFORM PASS                                             SQ1234.2
078600     ELSE                                                         SQ1234.2
078700         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ1234.2
078800         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1234.2
078900         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ1234.2
079000                   TO RE-MARK                                     SQ1234.2
079100         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1234.2
079200         PERFORM FAIL.                                            SQ1234.2
079300 SEQ-TEST-02-02-END.                                              SQ1234.2
079400*                                                                 SQ1234.2
079500*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ1234.2
079600*                                                                 SQ1234.2
079700     ADD     1 TO REC-CT.                                         SQ1234.2
079800     IF DELETE-SW NOT = SPACE                                     SQ1234.2
079900         GO TO   SEQ-DELETE-02-03.                                SQ1234.2
080000     GO TO   SEQ-TEST-CR-02-03.                                   SQ1234.2
080100 SEQ-DELETE-02-03.                                                SQ1234.2
080200     PERFORM DE-LETE.                                             SQ1234.2
080300     GO TO   SEQ-TEST-02-03-END.                                  SQ1234.2
080400 SEQ-TEST-CR-02-03.                                               SQ1234.2
080500     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ1234.2
080600         PERFORM PASS                                             SQ1234.2
080700     ELSE                                                         SQ1234.2
080800         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ1234.2
080900         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1234.2
081000         MOVE   "UNEXPECTED EXECUTION OF OUTPUT DECLARATIVE"      SQ1234.2
081100                   TO RE-MARK                                     SQ1234.2
081200         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1234.2
081300         PERFORM FAIL.                                            SQ1234.2
081400 SEQ-TEST-02-03-END.                                              SQ1234.2
081500     MOVE    SPACE TO DELETE-SW-2.                                SQ1234.2
081600*                                                                 SQ1234.2
081700*                                                                 SQ1234.2
081800*    NOW EXECUTE A NORMAL CLOSE ON THE EMPTY FILE.                SQ1234.2
081900*                                                                 SQ1234.2
082000 SEQ-INIT-03.                                                     SQ1234.2
082100     MOVE    0 TO REC-CT.                                         SQ1234.2
082200     MOVE    SPACE TO DECL-EXEC-SW.                               SQ1234.2
082300     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1234.2
082400     MOVE   "NOT EXECUTED" TO DECL-EXEC-I.                        SQ1234.2
082500     MOVE   "NOT EXECUTED" TO DECL-EXEC-O.                        SQ1234.2
082600     MOVE   "CLOSE AFTER CLOSE REEL"  TO FEATURE.                 SQ1234.2
082700     MOVE   "SEQ-TEST-CL-03" TO PAR-NAME.                         SQ1234.2
082800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1234.2
082900         GO TO SEQ-DELETE-03.                                     SQ1234.2
083000     GO TO   SEQ-TEST-CL-03.                                      SQ1234.2
083100 SEQ-DELETE-03.                                                   SQ1234.2
083200     MOVE   "*" TO DELETE-SW-2.                                   SQ1234.2
083300     GO TO   SEQ-DELETE-03-01.                                    SQ1234.2
083400 SEQ-TEST-CL-03.                                                  SQ1234.2
083500     CLOSE   SQ-FS4.                                              SQ1234.2
083600*                                                                 SQ1234.2
083700*    CHECK I-O STATUS RETURNED FROM CLOSE                         SQ1234.2
083800*                                                                 SQ1234.2
083900     ADD    1 TO REC-CT.                                          SQ1234.2
084000     IF DELETE-SW NOT = SPACE                                     SQ1234.2
084100         GO TO   SEQ-DELETE-03-01.                                SQ1234.2
084200     GO TO   SEQ-TEST-CL-03-01.                                   SQ1234.2
084300 SEQ-DELETE-03-01.                                                SQ1234.2
084400     PERFORM DE-LETE.                                             SQ1234.2
084500     GO TO   SEQ-TEST-03-01-END.                                  SQ1234.2
084600 SEQ-TEST-CL-03-01.                                               SQ1234.2
084700     IF SQ-FS4-STATUS = "00"                                      SQ1234.2
084800         PERFORM PASS                                             SQ1234.2
084900     ELSE                                                         SQ1234.2
085000         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1234.2
085100         MOVE   "00" TO CORRECT-A                                 SQ1234.2
085200         MOVE   "UNEXPECTED ERROR CODE FROM CLOSE"                SQ1234.2
085300                   TO RE-MARK                                     SQ1234.2
085400         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1234.2
085500         PERFORM FAIL.                                            SQ1234.2
085600 SEQ-TEST-03-01-END.                                              SQ1234.2
085700*                                                                 SQ1234.2
085800*    CHECK EXECUTION OF INPUT DECLARATIVE                         SQ1234.2
085900*                                                                 SQ1234.2
086000     ADD     1 TO REC-CT.                                         SQ1234.2
086100     IF DELETE-SW NOT = SPACE                                     SQ1234.2
086200         GO TO   SEQ-DELETE-03-02.                                SQ1234.2
086300     GO TO   SEQ-TEST-CL-03-02.                                   SQ1234.2
086400 SEQ-DELETE-03-02.                                                SQ1234.2
086500     PERFORM DE-LETE.                                             SQ1234.2
086600     GO TO   SEQ-TEST-03-02-END.                                  SQ1234.2
086700 SEQ-TEST-CL-03-02.                                               SQ1234.2
086800     IF DECL-EXEC-I = "NOT EXECUTED"                              SQ1234.2
086900         PERFORM PASS                                             SQ1234.2
087000     ELSE                                                         SQ1234.2
087100         MOVE    DECL-EXEC-I TO COMPUTED-A                        SQ1234.2
087200         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1234.2
087300         MOVE   "UNEXPECTED EXECUTION OF INPUT DECLARATIVE"       SQ1234.2
087400                   TO RE-MARK                                     SQ1234.2
087500         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1234.2
087600         PERFORM FAIL.                                            SQ1234.2
087700 SEQ-TEST-03-02-END.                                              SQ1234.2
087800*                                                                 SQ1234.2
087900*    CHECK EXECUTION OF OUTPUT DECLARATIVE                        SQ1234.2
088000*                                                                 SQ1234.2
088100     ADD     1 TO REC-CT.                                         SQ1234.2
088200     IF DELETE-SW NOT = SPACE                                     SQ1234.2
088300         GO TO   SEQ-DELETE-03-03.                                SQ1234.2
088400     GO TO   SEQ-TEST-CL-03-03.                                   SQ1234.2
088500 SEQ-DELETE-03-03.                                                SQ1234.2
088600     PERFORM DE-LETE.                                             SQ1234.2
088700     GO TO   SEQ-TEST-03-03-END.                                  SQ1234.2
088800 SEQ-TEST-CL-03-03.                                               SQ1234.2
088900     IF DECL-EXEC-O = "NOT EXECUTED"                              SQ1234.2
089000         PERFORM PASS                                             SQ1234.2
089100     ELSE                                                         SQ1234.2
089200         MOVE    DECL-EXEC-O TO COMPUTED-A                        SQ1234.2
089300         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1234.2
089400         MOVE   "UNEXPECTED EXECUTION OF OUTPUT DECLARATIVE"      SQ1234.2
089500                   TO RE-MARK                                     SQ1234.2
089600         MOVE   "VII-2,1.3.5, VII-51,4.6.4(5)" TO ANSI-REFERENCE  SQ1234.2
089700         PERFORM FAIL.                                            SQ1234.2
089800 SEQ-TEST-03-03-END.                                              SQ1234.2
089900     MOVE    SPACE TO DELETE-SW-2.                                SQ1234.2
090000*                                                                 SQ1234.2
090100*                                                                 SQ1234.2
090200 CCVS-EXIT SECTION.                                               SQ1234.2
090300 CCVS-999999.                                                     SQ1234.2
090400     GO TO   CLOSE-FILES.                                         SQ1234.2
      *END-OF,SQ123A                                                                  
