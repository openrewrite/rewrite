      *HEADER,COBOL,SQ130A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1304.2
000200 PROGRAM-ID.                                                      SQ1304.2
000300     SQ130A.                                                      SQ1304.2
000400****************************************************************  SQ1304.2
000500*                                                              *  SQ1304.2
000600*    VALIDATION FOR:-                                          *  SQ1304.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1304.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1304.2
000900*                             REVISED 1986, AUGUST             *  SQ1304.2
001000*                                                              *  SQ1304.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1304.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1304.2
001300*                                                              *  SQ1304.2
001400****************************************************************  SQ1304.2
001500*                                                              *  SQ1304.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1304.2
001700*                                                              *  SQ1304.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1304.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1304.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1304.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1304.2
002200*                                                              *  SQ1304.2
002300*                                                              *  SQ1304.2
002400****************************************************************  SQ1304.2
002500*                                                              *  SQ1304.2
002600*    SQ130A ATTEMPTS TO OPEN FOR INPUT-OUTPUT A MASS STORAGE   *  SQ1304.2
002700*    FILE WHICH IS NOT PRESENT.  THIS SHOULD RESULT IN A       *  SQ1304.2
002800*    PERMANENT ERROR AND AN I-O STATUS OF "35".  THE PROGRAM   *  SQ1304.2
002900*    DOES NOT CONTAIN AN APPLICABLE DECLARATIVE PROCEDURE. IN  *  SQ1304.2
003000*    THESE CIRCUMSTANCES THE STANDARD ALLOWS THE IMPLEMENTOR   *  SQ1304.2
003100*    TO TERMINATE EXECUTION OF THE PROGRAM OR TO CONTINUE.     *  SQ1304.2
003200*                                                              *  SQ1304.2
003300****************************************************************  SQ1304.2
003400*                                                                 SQ1304.2
003500 ENVIRONMENT DIVISION.                                            SQ1304.2
003600 CONFIGURATION SECTION.                                           SQ1304.2
003700 SOURCE-COMPUTER.                                                 SQ1304.2
003800     XXXXX082.                                                    SQ1304.2
003900 OBJECT-COMPUTER.                                                 SQ1304.2
004000     XXXXX083.                                                    SQ1304.2
004100*                                                                 SQ1304.2
004200 INPUT-OUTPUT SECTION.                                            SQ1304.2
004300 FILE-CONTROL.                                                    SQ1304.2
004400     SELECT PRINT-FILE ASSIGN TO                                  SQ1304.2
004500     XXXXX055.                                                    SQ1304.2
004600*                                                                 SQ1304.2
004700*    SELECT RAW-DATA   ASSIGN TO                                  SQ1304.2
004800*    XXXXX062                                                     SQ1304.2
004900*          ORGANIZATION IS INDEXED                                SQ1304.2
005000*          ACCESS MODE  IS RANDOM                                 SQ1304.2
005100*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1304.2
005200*                                                                 SQ1304.2
005300     SELECT SQ-FS1 ASSIGN TO                                      SQ1304.2
005400     XXXXX014                                                     SQ1304.2
005500            FILE STATUS IS SQ-FS1-STATUS.                         SQ1304.2
005600*                                                                 SQ1304.2
005700*                                                                 SQ1304.2
005800 DATA DIVISION.                                                   SQ1304.2
005900 FILE SECTION.                                                    SQ1304.2
006000 FD  PRINT-FILE                                                   SQ1304.2
006100C    LABEL RECORDS                                                SQ1304.2
006200C    XXXXX084                                                     SQ1304.2
006300C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1304.2
006400               .                                                  SQ1304.2
006500 01  PRINT-REC    PICTURE X(120).                                 SQ1304.2
006600 01  DUMMY-RECORD PICTURE X(120).                                 SQ1304.2
006700*                                                                 SQ1304.2
006800*FD  RAW-DATA.                                                    SQ1304.2
006900*01  RAW-DATA-SATZ.                                               SQ1304.2
007000*    05  RAW-DATA-KEY        PIC X(6).                            SQ1304.2
007100*    05  C-DATE              PIC 9(6).                            SQ1304.2
007200*    05  C-TIME              PIC 9(8).                            SQ1304.2
007300*    05  NO-OF-TESTS         PIC 99.                              SQ1304.2
007400*    05  C-OK                PIC 999.                             SQ1304.2
007500*    05  C-ALL               PIC 999.                             SQ1304.2
007600*    05  C-FAIL              PIC 999.                             SQ1304.2
007700*    05  C-DELETED           PIC 999.                             SQ1304.2
007800*    05  C-INSPECT           PIC 999.                             SQ1304.2
007900*    05  C-NOTE              PIC X(13).                           SQ1304.2
008000*    05  C-INDENT            PIC X.                               SQ1304.2
008100*    05  C-ABORT             PIC X(8).                            SQ1304.2
008200*                                                                 SQ1304.2
008300 FD  SQ-FS1                                                       SQ1304.2
008400C    LABEL RECORD IS STANDARD                                     SQ1304.2
008500                .                                                 SQ1304.2
008600 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1304.2
008700*                                                                 SQ1304.2
008800 WORKING-STORAGE SECTION.                                         SQ1304.2
008900*                                                                 SQ1304.2
009000***************************************************************   SQ1304.2
009100*                                                             *   SQ1304.2
009200*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1304.2
009300*                                                             *   SQ1304.2
009400***************************************************************   SQ1304.2
009500*                                                                 SQ1304.2
009600 01  SQ-FS1-STATUS.                                               SQ1304.2
009700   03  SQ-FS1-KEY-1   PIC X.                                      SQ1304.2
009800   03  SQ-FS1-KEY-2   PIC X.                                      SQ1304.2
009900*                                                                 SQ1304.2
010000*                                                                 SQ1304.2
010100***************************************************************   SQ1304.2
010200*                                                             *   SQ1304.2
010300*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1304.2
010400*                                                             *   SQ1304.2
010500***************************************************************   SQ1304.2
010600*                                                                 SQ1304.2
010700 01  REC-SKEL-SUB   PIC 99.                                       SQ1304.2
010800*                                                                 SQ1304.2
010900 01  FILE-RECORD-INFORMATION-REC.                                 SQ1304.2
011000     03 FILE-RECORD-INFO-SKELETON.                                SQ1304.2
011100        05 FILLER                 PICTURE X(48)       VALUE       SQ1304.2
011200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1304.2
011300        05 FILLER                 PICTURE X(46)       VALUE       SQ1304.2
011400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1304.2
011500        05 FILLER                 PICTURE X(26)       VALUE       SQ1304.2
011600             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1304.2
011700        05 FILLER                 PICTURE X(37)       VALUE       SQ1304.2
011800             ",RECKEY=                             ".             SQ1304.2
011900        05 FILLER                 PICTURE X(38)       VALUE       SQ1304.2
012000             ",ALTKEY1=                             ".            SQ1304.2
012100        05 FILLER                 PICTURE X(38)       VALUE       SQ1304.2
012200             ",ALTKEY2=                             ".            SQ1304.2
012300        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1304.2
012400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1304.2
012500        05 FILE-RECORD-INFO-P1-120.                               SQ1304.2
012600           07 FILLER              PIC X(5).                       SQ1304.2
012700           07 XFILE-NAME          PIC X(6).                       SQ1304.2
012800           07 FILLER              PIC X(8).                       SQ1304.2
012900           07 XRECORD-NAME        PIC X(6).                       SQ1304.2
013000           07 FILLER              PIC X(1).                       SQ1304.2
013100           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1304.2
013200           07 FILLER              PIC X(7).                       SQ1304.2
013300           07 XRECORD-NUMBER      PIC 9(6).                       SQ1304.2
013400           07 FILLER              PIC X(6).                       SQ1304.2
013500           07 UPDATE-NUMBER       PIC 9(2).                       SQ1304.2
013600           07 FILLER              PIC X(5).                       SQ1304.2
013700           07 ODO-NUMBER          PIC 9(4).                       SQ1304.2
013800           07 FILLER              PIC X(5).                       SQ1304.2
013900           07 XPROGRAM-NAME       PIC X(5).                       SQ1304.2
014000           07 FILLER              PIC X(7).                       SQ1304.2
014100           07 XRECORD-LENGTH      PIC 9(6).                       SQ1304.2
014200           07 FILLER              PIC X(7).                       SQ1304.2
014300           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1304.2
014400           07 FILLER              PIC X(1).                       SQ1304.2
014500           07 XBLOCK-SIZE         PIC 9(4).                       SQ1304.2
014600           07 FILLER              PIC X(6).                       SQ1304.2
014700           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1304.2
014800           07 FILLER              PIC X(5).                       SQ1304.2
014900           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1304.2
015000           07 FILLER              PIC X(6).                       SQ1304.2
015100           07 XLABEL-TYPE         PIC X(1).                       SQ1304.2
015200        05 FILE-RECORD-INFO-P121-240.                             SQ1304.2
015300           07 FILLER              PIC X(8).                       SQ1304.2
015400           07 XRECORD-KEY         PIC X(29).                      SQ1304.2
015500           07 FILLER              PIC X(9).                       SQ1304.2
015600           07 ALTERNATE-KEY1      PIC X(29).                      SQ1304.2
015700           07 FILLER              PIC X(9).                       SQ1304.2
015800           07 ALTERNATE-KEY2      PIC X(29).                      SQ1304.2
015900           07 FILLER              PIC X(7).                       SQ1304.2
016000*                                                                 SQ1304.2
016100 01  TEST-RESULTS.                                                SQ1304.2
016200     02 FILLER              PIC X      VALUE SPACE.               SQ1304.2
016300     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1304.2
016400     02 FILLER              PIC X      VALUE SPACE.               SQ1304.2
016500     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1304.2
016600     02 FILLER              PIC X      VALUE SPACE.               SQ1304.2
016700     02  PAR-NAME.                                                SQ1304.2
016800       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1304.2
016900       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1304.2
017000       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1304.2
017100     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1304.2
017200     02 RE-MARK             PIC X(61).                            SQ1304.2
017300 01  TEST-COMPUTED.                                               SQ1304.2
017400   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1304.2
017500   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1304.2
017600   02 COMPUTED-X.                                                 SQ1304.2
017700     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1304.2
017800     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1304.2
017900     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1304.2
018000     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1304.2
018100     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1304.2
018200     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1304.2
018300        04 COMPUTED-18V0                   PIC -9(18).            SQ1304.2
018400        04 FILLER                          PIC X.                 SQ1304.2
018500     03 FILLER PIC X(50) VALUE SPACE.                             SQ1304.2
018600 01  TEST-CORRECT.                                                SQ1304.2
018700     02 FILLER PIC X(30) VALUE SPACE.                             SQ1304.2
018800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1304.2
018900     02 CORRECT-X.                                                SQ1304.2
019000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1304.2
019100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1304.2
019200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1304.2
019300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1304.2
019400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1304.2
019500     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1304.2
019600         04 CORRECT-18V0                     PIC -9(18).          SQ1304.2
019700         04 FILLER                           PIC X.               SQ1304.2
019800     03 FILLER PIC X(2) VALUE SPACE.                              SQ1304.2
019900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1304.2
020000 01  CCVS-C-1.                                                    SQ1304.2
020100     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ1304.2
020200     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ1304.2
020300-    "SS  PARAGRAPH-NAME                                          SQ1304.2
020400-    "       REMARKS".                                            SQ1304.2
020500     02 FILLER           PIC X(17)    VALUE SPACE.                SQ1304.2
020600 01  CCVS-C-2.                                                    SQ1304.2
020700     02 FILLER           PIC XXXX     VALUE SPACE.                SQ1304.2
020800     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ1304.2
020900     02 FILLER           PIC X(16)    VALUE SPACE.                SQ1304.2
021000     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ1304.2
021100     02 FILLER           PIC X(90)    VALUE SPACE.                SQ1304.2
021200 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1304.2
021300 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1304.2
021400 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1304.2
021500 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1304.2
021600 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1304.2
021700 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1304.2
021800 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1304.2
021900 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1304.2
022000 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1304.2
022100 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1304.2
022200 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1304.2
022300 01  CCVS-H-1.                                                    SQ1304.2
022400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1304.2
022500     02  FILLER          PIC X(42)    VALUE                       SQ1304.2
022600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1304.2
022700     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1304.2
022800 01  CCVS-H-2A.                                                   SQ1304.2
022900   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1304.2
023000   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1304.2
023100   02  FILLER            PIC XXXX   VALUE                         SQ1304.2
023200     "4.2 ".                                                      SQ1304.2
023300   02  FILLER            PIC X(28)  VALUE                         SQ1304.2
023400            " COPY - NOT FOR DISTRIBUTION".                       SQ1304.2
023500   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1304.2
023600*                                                                 SQ1304.2
023700 01  CCVS-H-2B.                                                   SQ1304.2
023800   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1304.2
023900   02  TEST-ID           PIC X(9).                                SQ1304.2
024000   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1304.2
024100   02  FILLER            PIC X(12)  VALUE                         SQ1304.2
024200     " HIGH       ".                                              SQ1304.2
024300   02  FILLER            PIC X(22)  VALUE                         SQ1304.2
024400            " LEVEL VALIDATION FOR ".                             SQ1304.2
024500   02  FILLER            PIC X(58)  VALUE                         SQ1304.2
024600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1304.2
024700 01  CCVS-H-3.                                                    SQ1304.2
024800     02  FILLER          PIC X(34)  VALUE                         SQ1304.2
024900            " FOR OFFICIAL USE ONLY    ".                         SQ1304.2
025000     02  FILLER          PIC X(58)  VALUE                         SQ1304.2
025100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1304.2
025200     02  FILLER          PIC X(28)  VALUE                         SQ1304.2
025300            "  COPYRIGHT   1985,1986 ".                           SQ1304.2
025400 01  CCVS-E-1.                                                    SQ1304.2
025500     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1304.2
025600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1304.2
025700     02 ID-AGAIN         PIC X(9).                                SQ1304.2
025800     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1304.2
025900 01  CCVS-E-2.                                                    SQ1304.2
026000     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1304.2
026100     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1304.2
026200     02  CCVS-E-2-2.                                              SQ1304.2
026300         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1304.2
026400         03 FILLER         PIC X      VALUE SPACE.                SQ1304.2
026500         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1304.2
026600            "ERRORS ENCOUNTERED".                                 SQ1304.2
026700 01  CCVS-E-3.                                                    SQ1304.2
026800     02  FILLER          PIC X(22)  VALUE                         SQ1304.2
026900            " FOR OFFICIAL USE ONLY".                             SQ1304.2
027000     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1304.2
027100     02  FILLER          PIC X(58)  VALUE                         SQ1304.2
027200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1304.2
027300     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1304.2
027400     02  FILLER          PIC X(20)  VALUE                         SQ1304.2
027500             " COPYRIGHT 1985,1986".                              SQ1304.2
027600 01  CCVS-E-4.                                                    SQ1304.2
027700     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1304.2
027800     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1304.2
027900     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1304.2
028000     02 FILLER           PIC X(40)  VALUE                         SQ1304.2
028100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1304.2
028200 01  XXINFO.                                                      SQ1304.2
028300     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1304.2
028400     02 INFO-TEXT.                                                SQ1304.2
028500       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1304.2
028600       04 XXCOMPUTED         PIC X(20).                           SQ1304.2
028700       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1304.2
028800       04 XXCORRECT          PIC X(20).                           SQ1304.2
028900     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1304.2
029000 01  HYPHEN-LINE.                                                 SQ1304.2
029100     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1304.2
029200     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1304.2
029300-    "*****************************************".                 SQ1304.2
029400     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1304.2
029500-    "******************************".                            SQ1304.2
029600 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1304.2
029700     "SQ130A".                                                    SQ1304.2
029800*                                                                 SQ1304.2
029900*                                                                 SQ1304.2
030000 PROCEDURE DIVISION.                                              SQ1304.2
030100 CCVS1 SECTION.                                                   SQ1304.2
030200 OPEN-FILES.                                                      SQ1304.2
030300*    OPEN    I-O RAW-DATA.                                        SQ1304.2
030400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1304.2
030500*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1304.2
030600*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1304.2
030700*    ADD     1           TO C-NO-OF-TESTS.                        SQ1304.2
030800*    ACCEPT  C-DATE      FROM DATE.                               SQ1304.2
030900*    ACCEPT  C-TIME      FROM TIME.                               SQ1304.2
031000*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1304.2
031100*END-E-1.                                                         SQ1304.2
031200*    CLOSE   RAW-DATA.                                            SQ1304.2
031300     OPEN    OUTPUT PRINT-FILE.                                   SQ1304.2
031400     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1304.2
031500     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1304.2
031600     MOVE    SPACE TO TEST-RESULTS.                               SQ1304.2
031700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1304.2
031800     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1304.2
031900     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1304.2
032000     GO TO CCVS1-EXIT.                                            SQ1304.2
032100*                                                                 SQ1304.2
032200 CCVS-INIT-FILE.                                                  SQ1304.2
032300     ADD     1 TO REC-SKL-SUB.                                    SQ1304.2
032400     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1304.2
032500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1304.2
032600*                                                                 SQ1304.2
032700 CLOSE-FILES.                                                     SQ1304.2
032800     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1304.2
032900     CLOSE   PRINT-FILE.                                          SQ1304.2
033000*    OPEN    I-O RAW-DATA.                                        SQ1304.2
033100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1304.2
033200*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1304.2
033300*    MOVE   "OK.     " TO C-ABORT.                                SQ1304.2
033400*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1304.2
033500*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1304.2
033600*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1304.2
033700*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1304.2
033800*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1304.2
033900*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1304.2
034000*END-E-2.                                                         SQ1304.2
034100*    CLOSE   RAW-DATA.                                            SQ1304.2
034200 TERMINATE-CCVS.                                                  SQ1304.2
034300*    EXIT    PROGRAM.                                             SQ1304.2
034400     STOP    RUN.                                                 SQ1304.2
034500*                                                                 SQ1304.2
034600 INSPT.                                                           SQ1304.2
034700     MOVE   "INSPT" TO P-OR-F.                                    SQ1304.2
034800     ADD     1 TO INSPECT-COUNTER.                                SQ1304.2
034900     PERFORM PRINT-DETAIL.                                        SQ1304.2
035000*                                                                 SQ1304.2
035100 PASS.                                                            SQ1304.2
035200     MOVE   "PASS " TO P-OR-F.                                    SQ1304.2
035300     ADD     1 TO PASS-COUNTER.                                   SQ1304.2
035400     PERFORM PRINT-DETAIL.                                        SQ1304.2
035500*                                                                 SQ1304.2
035600 FAIL.                                                            SQ1304.2
035700     MOVE   "FAIL*" TO P-OR-F.                                    SQ1304.2
035800     ADD     1 TO ERROR-COUNTER.                                  SQ1304.2
035900     PERFORM PRINT-DETAIL.                                        SQ1304.2
036000*                                                                 SQ1304.2
036100 DE-LETE.                                                         SQ1304.2
036200     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1304.2
036300     MOVE   "*****" TO P-OR-F.                                    SQ1304.2
036400     ADD     1 TO DELETE-COUNTER.                                 SQ1304.2
036500     PERFORM PRINT-DETAIL.                                        SQ1304.2
036600*                                                                 SQ1304.2
036700 PRINT-DETAIL.                                                    SQ1304.2
036800     IF REC-CT NOT EQUAL TO ZERO                                  SQ1304.2
036900         MOVE   "." TO PARDOT-X                                   SQ1304.2
037000         MOVE    REC-CT TO DOTVALUE.                              SQ1304.2
037100     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1304.2
037200     PERFORM WRITE-LINE.                                          SQ1304.2
037300     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1304.2
037400         PERFORM WRITE-LINE                                       SQ1304.2
037500         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1304.2
037600     ELSE                                                         SQ1304.2
037700         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1304.2
037800     MOVE    SPACE TO P-OR-F.                                     SQ1304.2
037900     MOVE    SPACE TO COMPUTED-X.                                 SQ1304.2
038000     MOVE    SPACE TO CORRECT-X.                                  SQ1304.2
038100     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1304.2
038200     MOVE    SPACE TO RE-MARK.                                    SQ1304.2
038300*                                                                 SQ1304.2
038400 HEAD-ROUTINE.                                                    SQ1304.2
038500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1304.2
038600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1304.2
038700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1304.2
038800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1304.2
038900 COLUMN-NAMES-ROUTINE.                                            SQ1304.2
039000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1304.2
039100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1304.2
039200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1304.2
039300 END-ROUTINE.                                                     SQ1304.2
039400     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1304.2
039500     PERFORM WRITE-LINE 5 TIMES.                                  SQ1304.2
039600 END-RTN-EXIT.                                                    SQ1304.2
039700     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1304.2
039800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1304.2
039900*                                                                 SQ1304.2
040000 END-ROUTINE-1.                                                   SQ1304.2
040100     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1304.2
040200     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1304.2
040300     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1304.2
040400     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1304.2
040500     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1304.2
040600     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1304.2
040700     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1304.2
040800     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1304.2
040900     PERFORM WRITE-LINE.                                          SQ1304.2
041000     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1304.2
041100     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1304.2
041200         MOVE   "NO " TO ERROR-TOTAL                              SQ1304.2
041300     ELSE                                                         SQ1304.2
041400         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1304.2
041500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1304.2
041600     PERFORM WRITE-LINE.                                          SQ1304.2
041700 END-ROUTINE-13.                                                  SQ1304.2
041800     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1304.2
041900         MOVE   "NO " TO ERROR-TOTAL                              SQ1304.2
042000     ELSE                                                         SQ1304.2
042100         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1304.2
042200     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1304.2
042300     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1304.2
042400     PERFORM WRITE-LINE.                                          SQ1304.2
042500     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1304.2
042600         MOVE   "NO " TO ERROR-TOTAL                              SQ1304.2
042700     ELSE                                                         SQ1304.2
042800         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1304.2
042900     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1304.2
043000     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1304.2
043100     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1304.2
043200*                                                                 SQ1304.2
043300 WRITE-LINE.                                                      SQ1304.2
043400     ADD     1 TO RECORD-COUNT.                                   SQ1304.2
043500*    IF RECORD-COUNT GREATER 50                                   SQ1304.2
043600*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1304.2
043700*        MOVE  SPACE TO DUMMY-RECORD                              SQ1304.2
043800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1304.2
043900*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1304.2
044000*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1304.2
044100*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1304.2
044200*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1304.2
044300*        MOVE  ZERO TO RECORD-COUNT.                              SQ1304.2
044400     PERFORM WRT-LN.                                              SQ1304.2
044500*                                                                 SQ1304.2
044600 WRT-LN.                                                          SQ1304.2
044700     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1304.2
044800     MOVE    SPACE TO DUMMY-RECORD.                               SQ1304.2
044900 BLANK-LINE-PRINT.                                                SQ1304.2
045000     PERFORM WRT-LN.                                              SQ1304.2
045100 FAIL-ROUTINE.                                                    SQ1304.2
045200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1304.2
045300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1304.2
045400     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1304.2
045500     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1304.2
045600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1304.2
045700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1304.2
045800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1304.2
045900     GO TO   FAIL-ROUTINE-EX.                                     SQ1304.2
046000 FAIL-ROUTINE-WRITE.                                              SQ1304.2
046100     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1304.2
046200     PERFORM WRITE-LINE                                           SQ1304.2
046300     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1304.2
046400     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1304.2
046500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1304.2
046600     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1304.2
046700 FAIL-ROUTINE-EX.                                                 SQ1304.2
046800     EXIT.                                                        SQ1304.2
046900 BAIL-OUT.                                                        SQ1304.2
047000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1304.2
047100     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1304.2
047200 BAIL-OUT-WRITE.                                                  SQ1304.2
047300     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1304.2
047400     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1304.2
047500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1304.2
047600     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1304.2
047700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1304.2
047800     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1304.2
047900 BAIL-OUT-EX.                                                     SQ1304.2
048000     EXIT.                                                        SQ1304.2
048100 CCVS1-EXIT.                                                      SQ1304.2
048200     EXIT.                                                        SQ1304.2
048300*                                                                 SQ1304.2
048400****************************************************************  SQ1304.2
048500*                                                              *  SQ1304.2
048600*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1304.2
048700*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1304.2
048800*                                                              *  SQ1304.2
048900****************************************************************  SQ1304.2
049000*                                                                 SQ1304.2
049100 SECT-SQ130A-MAIN SECTION.                                        SQ1304.2
049200 OPEN-INIT-01.                                                    SQ1304.2
049300*                                                                 SQ1304.2
049400*    THIS PROGRAM ATTEMPTS TO OPEN IN THE INPUT-OUTPUT MODE       SQ1304.2
049500*    A FILE WHICH IS NOT PRESENT AND AVAILABLE TO IT.             SQ1304.2
049600*                                                                 SQ1304.2
049700     MOVE    1 TO REC-CT                                          SQ1304.2
049800     MOVE   "OPEN ABSENT FILE I-O" TO FEATURE                     SQ1304.2
049900     MOVE   "OPEN-TEST-01" TO PAR-NAME                            SQ1304.2
050000     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1304.2
050100*                                                                 SQ1304.2
050200     MOVE    SPACE TO DUMMY-RECORD.                               SQ1304.2
050300     PERFORM WRITE-LINE.                                          SQ1304.2
050400     MOVE   "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"    SQ1304.2
050500                TO DUMMY-RECORD.                                  SQ1304.2
050600     PERFORM WRITE-LINE.                                          SQ1304.2
050700     MOVE    SPACE TO DUMMY-RECORD.                               SQ1304.2
050800     PERFORM WRITE-LINE 3 TIMES.                                  SQ1304.2
050900*                                                                 SQ1304.2
051000 OPEN-TEST-01.                                                    SQ1304.2
051100     OPEN    I-O SQ-FS1.                                          SQ1304.2
051200     IF  SQ-FS1-STATUS NOT = "35"                                 SQ1304.2
051300         MOVE   "INCORRECT STATUS CODE RETURNED" TO RE-MARK       SQ1304.2
051400         MOVE   "VII-4, 1.5.3(3)C" TO ANSI-REFERENCE              SQ1304.2
051500         MOVE   "35" TO CORRECT-A                                 SQ1304.2
051600         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1304.2
051700         PERFORM FAIL                                             SQ1304.2
051800     ELSE                                                         SQ1304.2
051900         PERFORM PASS.                                            SQ1304.2
052000*                                                                 SQ1304.2
052100*                                                                 SQ1304.2
052200 CCVS-EXIT SECTION.                                               SQ1304.2
052300 CCVS-999999.                                                     SQ1304.2
052400     GO TO   CLOSE-FILES.                                         SQ1304.2
      *END-OF,SQ130A                                                                  
