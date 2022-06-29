      *HEADER,COBOL,SQ104A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1044.2
000200 PROGRAM-ID.                                                      SQ1044.2
000300     SQ104A.                                                      SQ1044.2
000400****************************************************************  SQ1044.2
000500*                                                              *  SQ1044.2
000600*    VALIDATION FOR:-                                          *  SQ1044.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1044.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1044.2
000900*                             REVISED 1986, AUGUST             *  SQ1044.2
001000*                                                              *  SQ1044.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1044.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1044.2
001300*                                                              *  SQ1044.2
001400****************************************************************  SQ1044.2
001500*                                                              *  SQ1044.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1044.2
001700*                                                              *  SQ1044.2
001800*            X-14   SEQUENTIAL MASS STORAGE                    *  SQ1044.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1044.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1044.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1044.2
002200*                                                              *  SQ1044.2
002300****************************************************************  SQ1044.2
002400*                                                              *  SQ1044.2
002500*    SQ104A CREATES A SEQUENTIAL MASS STORAGE FILE CONTAINING  *  SQ1044.2
002600*    649 FIXED LENGTH RECORDS, EACH 120 CHARACTERS LONG.  THE  *  SQ1044.2
002700*    FILE READ IS TWICE.  THE FIRST PASS CHECKS THAT ALL THE   *  SQ1044.2
002800*    EXPECTED RECORDS ARE PRESENT.  THE SECOND PASS PERFORMS   *  SQ1044.2
002900*    SIMILAR CHECKS, BUT USES ALL FOUR VARIANTS OF THE READ    *  SQ1044.2
003000*    STATEMENT WITH THE END PHRASE THAT CAN BE PRODUCED BY     *  SQ1044.2
003100*    INCLUDING OR OMITTING THE OPTIONAL WORDS "RECORD" AND     *  SQ1044.2
003200*    "AT".                                                     *  SQ1044.2
003300*                                                              *  SQ1044.2
003400*    THE PROGRAM OMITS THE OPTIONAL WORDS "ORGANIZATION IS"    *  SQ1044.2
003500*    FROM THE "ORGANIZATION IS SEQUENTIAL" CLAUSE OF THE       *  SQ1044.2
003600*    FILE-CONTROL ENTRY, AND PLACES THE ASSIGN CLAUSE IN A     *  SQ1044.2
003700*    POSITION OTHER THAN FIRST IN THE SAME ENTRY.              *  SQ1044.2
003800*                                                              *  SQ1044.2
003900****************************************************************  SQ1044.2
004000*                                                                 SQ1044.2
004100*                                                                 SQ1044.2
004200 ENVIRONMENT DIVISION.                                            SQ1044.2
004300 CONFIGURATION SECTION.                                           SQ1044.2
004400 SOURCE-COMPUTER.                                                 SQ1044.2
004500     XXXXX082.                                                    SQ1044.2
004600 OBJECT-COMPUTER.                                                 SQ1044.2
004700     XXXXX083.                                                    SQ1044.2
004800*                                                                 SQ1044.2
004900 INPUT-OUTPUT SECTION.                                            SQ1044.2
005000 FILE-CONTROL.                                                    SQ1044.2
005100     SELECT PRINT-FILE ASSIGN TO                                  SQ1044.2
005200     XXXXX055.                                                    SQ1044.2
005300*                                                                 SQ1044.2
005400*    SELECT RAW-DATA   ASSIGN TO                                  SQ1044.2
005500*    XXXXX062                                                     SQ1044.2
005600*          ORGANIZATION IS INDEXED                                SQ1044.2
005700*          ACCESS MODE  IS RANDOM                                 SQ1044.2
005800*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1044.2
005900*                                                                 SQ1044.2
006000     SELECT SQ-FS3                                                SQ1044.2
006100            ACCESS MODE SEQUENTIAL                                SQ1044.2
006200            ASSIGN TO                                             SQ1044.2
006300     XXXXX014                                                     SQ1044.2
006400            ORGANIZATION IS SEQUENTIAL                            SQ1044.2
006500            .                                                     SQ1044.2
006600*                                                                 SQ1044.2
006700*                                                                 SQ1044.2
006800 DATA DIVISION.                                                   SQ1044.2
006900 FILE SECTION.                                                    SQ1044.2
007000*                                                                 SQ1044.2
007100*FD  RAW-DATA.                                                    SQ1044.2
007200*01  RAW-DATA-SATZ.                                               SQ1044.2
007300*    05  RAW-DATA-KEY        PIC X(6).                            SQ1044.2
007400*    05  C-DATE              PIC 9(6).                            SQ1044.2
007500*    05  C-TIME              PIC 9(8).                            SQ1044.2
007600*    05  NO-OF-TESTS         PIC 99.                              SQ1044.2
007700*    05  C-OK                PIC 999.                             SQ1044.2
007800*    05  C-ALL               PIC 999.                             SQ1044.2
007900*    05  C-FAIL              PIC 999.                             SQ1044.2
008000*    05  C-DELETED           PIC 999.                             SQ1044.2
008100*    05  C-INSPECT           PIC 999.                             SQ1044.2
008200*    05  C-NOTE              PIC X(13).                           SQ1044.2
008300*    05  C-INDENT            PIC X.                               SQ1044.2
008400*    05  C-ABORT             PIC X(8).                            SQ1044.2
008500*                                                                 SQ1044.2
008600 FD  SQ-FS3                                                       SQ1044.2
008700C    LABEL RECORD IS STANDARD                                     SQ1044.2
008800C    DATA  RECORD SQ-FS3R1-F-G-120                                SQ1044.2
008900     BLOCK CONTAINS 120 CHARACTERS                                SQ1044.2
009000     RECORD CONTAINS 120 CHARACTERS.                              SQ1044.2
009100 01  SQ-FS3R1-F-G-120 PIC X(120).                                 SQ1044.2
009200*                                                                 SQ1044.2
009300 FD  PRINT-FILE                                                   SQ1044.2
009400C    LABEL RECORDS                                                SQ1044.2
009500C    XXXXX084                                                     SQ1044.2
009600C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1044.2
009700               .                                                  SQ1044.2
009800 01  PRINT-REC    PICTURE X(120).                                 SQ1044.2
009900 01  DUMMY-RECORD PICTURE X(120).                                 SQ1044.2
010000*                                                                 SQ1044.2
010100 WORKING-STORAGE SECTION.                                         SQ1044.2
010200*                                                                 SQ1044.2
010300***************************************************************   SQ1044.2
010400*                                                             *   SQ1044.2
010500*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1044.2
010600*                                                             *   SQ1044.2
010700***************************************************************   SQ1044.2
010800*                                                                 SQ1044.2
010900 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            SQ1044.2
011000 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 SQ1044.2
011100 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             SQ1044.2
011200*                                                                 SQ1044.2
011300***************************************************************   SQ1044.2
011400*                                                             *   SQ1044.2
011500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1044.2
011600*                                                             *   SQ1044.2
011700***************************************************************   SQ1044.2
011800*                                                                 SQ1044.2
011900 01  REC-SKEL-SUB   PIC 99.                                       SQ1044.2
012000*                                                                 SQ1044.2
012100 01  FILE-RECORD-INFORMATION-REC.                                 SQ1044.2
012200     03 FILE-RECORD-INFO-SKELETON.                                SQ1044.2
012300        05 FILLER                 PICTURE X(48)       VALUE       SQ1044.2
012400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1044.2
012500        05 FILLER                 PICTURE X(46)       VALUE       SQ1044.2
012600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1044.2
012700        05 FILLER                 PICTURE X(26)       VALUE       SQ1044.2
012800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1044.2
012900        05 FILLER                 PICTURE X(37)       VALUE       SQ1044.2
013000             ",RECKEY=                             ".             SQ1044.2
013100        05 FILLER                 PICTURE X(38)       VALUE       SQ1044.2
013200             ",ALTKEY1=                             ".            SQ1044.2
013300        05 FILLER                 PICTURE X(38)       VALUE       SQ1044.2
013400             ",ALTKEY2=                             ".            SQ1044.2
013500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1044.2
013600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1044.2
013700        05 FILE-RECORD-INFO-P1-120.                               SQ1044.2
013800           07 FILLER              PIC X(5).                       SQ1044.2
013900           07 XFILE-NAME          PIC X(6).                       SQ1044.2
014000           07 FILLER              PIC X(8).                       SQ1044.2
014100           07 XRECORD-NAME        PIC X(6).                       SQ1044.2
014200           07 FILLER              PIC X(1).                       SQ1044.2
014300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1044.2
014400           07 FILLER              PIC X(7).                       SQ1044.2
014500           07 XRECORD-NUMBER      PIC 9(6).                       SQ1044.2
014600           07 FILLER              PIC X(6).                       SQ1044.2
014700           07 UPDATE-NUMBER       PIC 9(2).                       SQ1044.2
014800           07 FILLER              PIC X(5).                       SQ1044.2
014900           07 ODO-NUMBER          PIC 9(4).                       SQ1044.2
015000           07 FILLER              PIC X(5).                       SQ1044.2
015100           07 XPROGRAM-NAME       PIC X(5).                       SQ1044.2
015200           07 FILLER              PIC X(7).                       SQ1044.2
015300           07 XRECORD-LENGTH      PIC 9(6).                       SQ1044.2
015400           07 FILLER              PIC X(7).                       SQ1044.2
015500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1044.2
015600           07 FILLER              PIC X(1).                       SQ1044.2
015700           07 XBLOCK-SIZE         PIC 9(4).                       SQ1044.2
015800           07 FILLER              PIC X(6).                       SQ1044.2
015900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1044.2
016000           07 FILLER              PIC X(5).                       SQ1044.2
016100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1044.2
016200           07 FILLER              PIC X(6).                       SQ1044.2
016300           07 XLABEL-TYPE         PIC X(1).                       SQ1044.2
016400        05 FILE-RECORD-INFO-P121-240.                             SQ1044.2
016500           07 FILLER              PIC X(8).                       SQ1044.2
016600           07 XRECORD-KEY         PIC X(29).                      SQ1044.2
016700           07 FILLER              PIC X(9).                       SQ1044.2
016800           07 ALTERNATE-KEY1      PIC X(29).                      SQ1044.2
016900           07 FILLER              PIC X(9).                       SQ1044.2
017000           07 ALTERNATE-KEY2      PIC X(29).                      SQ1044.2
017100           07 FILLER              PIC X(7).                       SQ1044.2
017200*                                                                 SQ1044.2
017300 01  TEST-RESULTS.                                                SQ1044.2
017400     02 FILLER              PIC X      VALUE SPACE.               SQ1044.2
017500     02  PAR-NAME.                                                SQ1044.2
017600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1044.2
017700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1044.2
017800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1044.2
017900     02 FILLER              PIC X      VALUE SPACE.               SQ1044.2
018000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1044.2
018100     02 FILLER              PIC X      VALUE SPACE.               SQ1044.2
018200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1044.2
018300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1044.2
018400     02 RE-MARK             PIC X(61).                            SQ1044.2
018500 01  TEST-COMPUTED.                                               SQ1044.2
018600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1044.2
018700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1044.2
018800   02 COMPUTED-X.                                                 SQ1044.2
018900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1044.2
019000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1044.2
019100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1044.2
019200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1044.2
019300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1044.2
019400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1044.2
019500        04 COMPUTED-18V0                   PIC -9(18).            SQ1044.2
019600        04 FILLER                          PIC X.                 SQ1044.2
019700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1044.2
019800 01  TEST-CORRECT.                                                SQ1044.2
019900     02 FILLER PIC X(30) VALUE SPACE.                             SQ1044.2
020000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1044.2
020100     02 CORRECT-X.                                                SQ1044.2
020200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1044.2
020300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1044.2
020400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1044.2
020500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1044.2
020600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1044.2
020700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1044.2
020800         04 CORRECT-18V0                     PIC -9(18).          SQ1044.2
020900         04 FILLER                           PIC X.               SQ1044.2
021000     03 FILLER PIC X(2) VALUE SPACE.                              SQ1044.2
021100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1044.2
021200 01  CCVS-C-1.                                                    SQ1044.2
021300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1044.2
021400     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1044.2
021500     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1044.2
021600     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1044.2
021700     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1044.2
021800     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1044.2
021900     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1044.2
022000     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1044.2
022100 01  CCVS-C-2.                                                    SQ1044.2
022200     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1044.2
022300     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1044.2
022400     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1044.2
022500     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1044.2
022600     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1044.2
022700 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1044.2
022800 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1044.2
022900 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1044.2
023000 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1044.2
023100 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1044.2
023200 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1044.2
023300 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1044.2
023400 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1044.2
023500 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1044.2
023600 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1044.2
023700 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1044.2
023800 01  CCVS-H-1.                                                    SQ1044.2
023900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1044.2
024000     02  FILLER          PIC X(42)    VALUE                       SQ1044.2
024100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1044.2
024200     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1044.2
024300 01  CCVS-H-2A.                                                   SQ1044.2
024400   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1044.2
024500   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1044.2
024600   02  FILLER            PIC XXXX   VALUE                         SQ1044.2
024700     "4.2 ".                                                      SQ1044.2
024800   02  FILLER            PIC X(28)  VALUE                         SQ1044.2
024900            " COPY - NOT FOR DISTRIBUTION".                       SQ1044.2
025000   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1044.2
025100*                                                                 SQ1044.2
025200 01  CCVS-H-2B.                                                   SQ1044.2
025300   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1044.2
025400   02  TEST-ID           PIC X(9).                                SQ1044.2
025500   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1044.2
025600   02  FILLER            PIC X(12)  VALUE                         SQ1044.2
025700     " HIGH       ".                                              SQ1044.2
025800   02  FILLER            PIC X(22)  VALUE                         SQ1044.2
025900            " LEVEL VALIDATION FOR ".                             SQ1044.2
026000   02  FILLER            PIC X(58)  VALUE                         SQ1044.2
026100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1044.2
026200 01  CCVS-H-3.                                                    SQ1044.2
026300     02  FILLER          PIC X(34)  VALUE                         SQ1044.2
026400            " FOR OFFICIAL USE ONLY    ".                         SQ1044.2
026500     02  FILLER          PIC X(58)  VALUE                         SQ1044.2
026600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1044.2
026700     02  FILLER          PIC X(28)  VALUE                         SQ1044.2
026800            "  COPYRIGHT   1985,1986 ".                           SQ1044.2
026900 01  CCVS-E-1.                                                    SQ1044.2
027000     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1044.2
027100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1044.2
027200     02 ID-AGAIN         PIC X(9).                                SQ1044.2
027300     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1044.2
027400 01  CCVS-E-2.                                                    SQ1044.2
027500     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1044.2
027600     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1044.2
027700     02  CCVS-E-2-2.                                              SQ1044.2
027800         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1044.2
027900         03 FILLER         PIC X      VALUE SPACE.                SQ1044.2
028000         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1044.2
028100            "ERRORS ENCOUNTERED".                                 SQ1044.2
028200 01  CCVS-E-3.                                                    SQ1044.2
028300     02  FILLER          PIC X(22)  VALUE                         SQ1044.2
028400            " FOR OFFICIAL USE ONLY".                             SQ1044.2
028500     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1044.2
028600     02  FILLER          PIC X(58)  VALUE                         SQ1044.2
028700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1044.2
028800     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1044.2
028900     02  FILLER          PIC X(20)  VALUE                         SQ1044.2
029000             " COPYRIGHT 1985,1986".                              SQ1044.2
029100 01  CCVS-E-4.                                                    SQ1044.2
029200     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1044.2
029300     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1044.2
029400     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1044.2
029500     02 FILLER           PIC X(40)  VALUE                         SQ1044.2
029600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1044.2
029700 01  XXINFO.                                                      SQ1044.2
029800     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1044.2
029900     02 INFO-TEXT.                                                SQ1044.2
030000       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1044.2
030100       04 XXCOMPUTED         PIC X(20).                           SQ1044.2
030200       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1044.2
030300       04 XXCORRECT          PIC X(20).                           SQ1044.2
030400     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1044.2
030500 01  HYPHEN-LINE.                                                 SQ1044.2
030600     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1044.2
030700     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1044.2
030800-    "*****************************************".                 SQ1044.2
030900     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1044.2
031000-    "******************************".                            SQ1044.2
031100 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1044.2
031200     "SQ104A".                                                    SQ1044.2
031300*                                                                 SQ1044.2
031400*                                                                 SQ1044.2
031500 PROCEDURE DIVISION.                                              SQ1044.2
031600*                                                                 SQ1044.2
031700 CCVS1 SECTION.                                                   SQ1044.2
031800 OPEN-FILES.                                                      SQ1044.2
031900*    OPEN    I-O RAW-DATA.                                        SQ1044.2
032000*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1044.2
032100*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1044.2
032200*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1044.2
032300*    ADD     1           TO C-NO-OF-TESTS.                        SQ1044.2
032400*    ACCEPT  C-DATE      FROM DATE.                               SQ1044.2
032500*    ACCEPT  C-TIME      FROM TIME.                               SQ1044.2
032600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1044.2
032700*END-E-1.                                                         SQ1044.2
032800*    CLOSE   RAW-DATA.                                            SQ1044.2
032900     OPEN    OUTPUT PRINT-FILE.                                   SQ1044.2
033000     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1044.2
033100     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1044.2
033200     MOVE    SPACE TO TEST-RESULTS.                               SQ1044.2
033300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1044.2
033400     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1044.2
033500     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1044.2
033600     GO TO CCVS1-EXIT.                                            SQ1044.2
033700*                                                                 SQ1044.2
033800 CCVS-INIT-FILE.                                                  SQ1044.2
033900     ADD     1 TO REC-SKL-SUB.                                    SQ1044.2
034000     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1044.2
034100                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1044.2
034200*                                                                 SQ1044.2
034300 CLOSE-FILES.                                                     SQ1044.2
034400     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1044.2
034500     CLOSE   PRINT-FILE.                                          SQ1044.2
034600*    OPEN    I-O RAW-DATA.                                        SQ1044.2
034700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1044.2
034800*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1044.2
034900*    MOVE   "OK.     " TO C-ABORT.                                SQ1044.2
035000*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1044.2
035100*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1044.2
035200*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1044.2
035300*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1044.2
035400*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1044.2
035500*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1044.2
035600*END-E-2.                                                         SQ1044.2
035700*    CLOSE   RAW-DATA.                                            SQ1044.2
035800 TERMINATE-CCVS.                                                  SQ1044.2
035900*    EXIT    PROGRAM.                                             SQ1044.2
036000     STOP    RUN.                                                 SQ1044.2
036100*                                                                 SQ1044.2
036200 INSPT.                                                           SQ1044.2
036300     MOVE   "INSPT" TO P-OR-F.                                    SQ1044.2
036400     ADD     1 TO INSPECT-COUNTER.                                SQ1044.2
036500     PERFORM PRINT-DETAIL.                                        SQ1044.2
036600*                                                                 SQ1044.2
036700 PASS.                                                            SQ1044.2
036800     MOVE   "PASS " TO P-OR-F.                                    SQ1044.2
036900     ADD     1 TO PASS-COUNTER.                                   SQ1044.2
037000     PERFORM PRINT-DETAIL.                                        SQ1044.2
037100*                                                                 SQ1044.2
037200 FAIL.                                                            SQ1044.2
037300     MOVE   "FAIL*" TO P-OR-F.                                    SQ1044.2
037400     ADD     1 TO ERROR-COUNTER.                                  SQ1044.2
037500     PERFORM PRINT-DETAIL.                                        SQ1044.2
037600*                                                                 SQ1044.2
037700 DE-LETE.                                                         SQ1044.2
037800     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1044.2
037900     MOVE   "*****" TO P-OR-F.                                    SQ1044.2
038000     ADD     1 TO DELETE-COUNTER.                                 SQ1044.2
038100     PERFORM PRINT-DETAIL.                                        SQ1044.2
038200*                                                                 SQ1044.2
038300 PRINT-DETAIL.                                                    SQ1044.2
038400     IF REC-CT NOT EQUAL TO ZERO                                  SQ1044.2
038500         MOVE   "." TO PARDOT-X                                   SQ1044.2
038600         MOVE    REC-CT TO DOTVALUE.                              SQ1044.2
038700     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1044.2
038800     PERFORM WRITE-LINE.                                          SQ1044.2
038900     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1044.2
039000         PERFORM WRITE-LINE                                       SQ1044.2
039100         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1044.2
039200     ELSE                                                         SQ1044.2
039300         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1044.2
039400     MOVE    SPACE TO P-OR-F.                                     SQ1044.2
039500     MOVE    SPACE TO COMPUTED-X.                                 SQ1044.2
039600     MOVE    SPACE TO CORRECT-X.                                  SQ1044.2
039700     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1044.2
039800     MOVE    SPACE TO RE-MARK.                                    SQ1044.2
039900*                                                                 SQ1044.2
040000 HEAD-ROUTINE.                                                    SQ1044.2
040100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1044.2
040200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1044.2
040300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1044.2
040400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1044.2
040500 COLUMN-NAMES-ROUTINE.                                            SQ1044.2
040600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1044.2
040700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1044.2
040800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1044.2
040900 END-ROUTINE.                                                     SQ1044.2
041000     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1044.2
041100     PERFORM WRITE-LINE 5 TIMES.                                  SQ1044.2
041200 END-RTN-EXIT.                                                    SQ1044.2
041300     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1044.2
041400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1044.2
041500*                                                                 SQ1044.2
041600 END-ROUTINE-1.                                                   SQ1044.2
041700     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1044.2
041800     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1044.2
041900     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1044.2
042000     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1044.2
042100     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1044.2
042200     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1044.2
042300     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1044.2
042400     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1044.2
042500     PERFORM WRITE-LINE.                                          SQ1044.2
042600     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1044.2
042700     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1044.2
042800         MOVE   "NO " TO ERROR-TOTAL                              SQ1044.2
042900     ELSE                                                         SQ1044.2
043000         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1044.2
043100     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1044.2
043200     PERFORM WRITE-LINE.                                          SQ1044.2
043300 END-ROUTINE-13.                                                  SQ1044.2
043400     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1044.2
043500         MOVE   "NO " TO ERROR-TOTAL                              SQ1044.2
043600     ELSE                                                         SQ1044.2
043700         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1044.2
043800     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1044.2
043900     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1044.2
044000     PERFORM WRITE-LINE.                                          SQ1044.2
044100     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1044.2
044200         MOVE   "NO " TO ERROR-TOTAL                              SQ1044.2
044300     ELSE                                                         SQ1044.2
044400         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1044.2
044500     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1044.2
044600     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1044.2
044700     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1044.2
044800*                                                                 SQ1044.2
044900 WRITE-LINE.                                                      SQ1044.2
045000     ADD     1 TO RECORD-COUNT.                                   SQ1044.2
045100*    IF RECORD-COUNT GREATER 50                                   SQ1044.2
045200*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1044.2
045300*        MOVE  SPACE TO DUMMY-RECORD                              SQ1044.2
045400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1044.2
045500*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1044.2
045600*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1044.2
045700*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1044.2
045800*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1044.2
045900*        MOVE  ZERO TO RECORD-COUNT.                              SQ1044.2
046000     PERFORM WRT-LN.                                              SQ1044.2
046100*                                                                 SQ1044.2
046200 WRT-LN.                                                          SQ1044.2
046300     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1044.2
046400     MOVE    SPACE TO DUMMY-RECORD.                               SQ1044.2
046500 BLANK-LINE-PRINT.                                                SQ1044.2
046600     PERFORM WRT-LN.                                              SQ1044.2
046700 FAIL-ROUTINE.                                                    SQ1044.2
046800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1044.2
046900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1044.2
047000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1044.2
047100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1044.2
047200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1044.2
047300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1044.2
047400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1044.2
047500     GO TO   FAIL-ROUTINE-EX.                                     SQ1044.2
047600 FAIL-ROUTINE-WRITE.                                              SQ1044.2
047700     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1044.2
047800     PERFORM WRITE-LINE                                           SQ1044.2
047900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1044.2
048000     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1044.2
048100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1044.2
048200     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1044.2
048300 FAIL-ROUTINE-EX.                                                 SQ1044.2
048400     EXIT.                                                        SQ1044.2
048500 BAIL-OUT.                                                        SQ1044.2
048600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1044.2
048700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1044.2
048800 BAIL-OUT-WRITE.                                                  SQ1044.2
048900     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1044.2
049000     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1044.2
049100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1044.2
049200     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1044.2
049300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1044.2
049400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1044.2
049500 BAIL-OUT-EX.                                                     SQ1044.2
049600     EXIT.                                                        SQ1044.2
049700 CCVS1-EXIT.                                                      SQ1044.2
049800     EXIT.                                                        SQ1044.2
049900*                                                                 SQ1044.2
050000****************************************************************  SQ1044.2
050100*                                                              *  SQ1044.2
050200*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1044.2
050300*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1044.2
050400*                                                              *  SQ1044.2
050500****************************************************************  SQ1044.2
050600*                                                                 SQ1044.2
050700 SECT-SQ104-0001 SECTION.                                         SQ1044.2
050800 SEQ-INIT-WR-01.                                                  SQ1044.2
050900     MOVE    "SQ-FS3" TO XFILE-NAME (1).                          SQ1044.2
051000     MOVE    "R1-F-G" TO XRECORD-NAME (1).                        SQ1044.2
051100     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   SQ1044.2
051200     MOVE     000120 TO XRECORD-LENGTH (1).                       SQ1044.2
051300     MOVE    "CH"    TO CHARS-OR-RECORDS (1).                     SQ1044.2
051400     MOVE     0120   TO XBLOCK-SIZE (1).                          SQ1044.2
051500     MOVE     000649 TO RECORDS-IN-FILE (1).                      SQ1044.2
051600     MOVE    "SQ"    TO XFILE-ORGANIZATION (1).                   SQ1044.2
051700     MOVE    "S"     TO XLABEL-TYPE (1).                          SQ1044.2
051800     MOVE     ZERO   TO XRECORD-NUMBER (1).                       SQ1044.2
051900     MOVE    "CREATE 649 RECORD FILE" TO FEATURE.                 SQ1044.2
052000     MOVE    "SEQ-TEST-WR-01" TO PAR-NAME.                        SQ1044.2
052100*                                                                 SQ1044.2
052200 SEQ-TEST-WR-01.                                                  SQ1044.2
052300     OPEN OUTPUT SQ-FS3.                                          SQ1044.2
052400*                                                                 SQ1044.2
052500 SEQ-TEST-WR-01-LOOP.                                             SQ1044.2
052600     ADD     1 TO XRECORD-NUMBER (1).                             SQ1044.2
052700     MOVE    FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS3R1-F-G-120.    SQ1044.2
052800     WRITE   SQ-FS3R1-F-G-120.                                    SQ1044.2
052900     IF XRECORD-NUMBER (1) LESS THAN 649                          SQ1044.2
053000         GO TO   SEQ-TEST-WR-01-LOOP.                             SQ1044.2
053100*                                                                 SQ1044.2
053200     CLOSE   SQ-FS3.                                              SQ1044.2
053300*                                                                 SQ1044.2
053400*    A SEQUENTIAL TAPE FILE HAS BEEN CREATED.  IT CONTAINS 649    SQ1044.2
053500*    RECORDS, EACH 120 CHARACTERS LONG.  THE FILE WILL NOW BE     SQ1044.2
053600*    READ AND THE RECORDS VERIFIED.                               SQ1044.2
053700*                                                                 SQ1044.2
053800 SEQ-INIT-GF-02.                                                  SQ1044.2
053900     MOVE   "SEQ-TEST-GF-02" TO PAR-NAME.                         SQ1044.2
054000     MOVE   "VERIFY NEW FILE" TO FEATURE.                         SQ1044.2
054100     MOVE    1      TO REC-CT.                                    SQ1044.2
054200     GO TO  SEQ-TEST-GF-02-01.                                    SQ1044.2
054300 SEQ-DELETE-02-01.                                                SQ1044.2
054400     GO TO   SEQ-DELETE-02-02.                                    SQ1044.2
054500 SEQ-TEST-GF-02-01.                                               SQ1044.2
054600     OPEN INPUT SQ-FS3.                                           SQ1044.2
054700*                                                                 SQ1044.2
054800 SEQ-INIT-GF-02-02.                                               SQ1044.2
054900     MOVE    FILE-RECORD-INFO-P1-120 (1)                          SQ1044.2
055000                TO FILE-RECORD-INFO-P1-120 (2).                   SQ1044.2
055100     MOVE    ZERO   TO XRECORD-NUMBER (2).                        SQ1044.2
055200     GO TO   SEQ-TEST-GF-02-02.                                   SQ1044.2
055300 SEQ-DELETE-02-02.                                                SQ1044.2
055400     PERFORM DE-LETE.                                             SQ1044.2
055500     ADD     1 TO REC-CT.                                         SQ1044.2
055600     PERFORM DE-LETE.                                             SQ1044.2
055700     GO TO   SEQ-DELETE-GF-02-05.                                 SQ1044.2
055800 SEQ-TEST-GF-02-02.                                               SQ1044.2
055900 SEQ-TEST-GF-02-02-LOOP.                                          SQ1044.2
056000     READ SQ-FS3                                                  SQ1044.2
056100          AT END GO TO SEQ-TEST-GF-02-02-1.                       SQ1044.2
056200     MOVE   SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1044.2
056300     ADD    1 TO XRECORD-NUMBER (2).                              SQ1044.2
056400     IF XRECORD-NUMBER (2) GREATER THAN 649                       SQ1044.2
056500         GO TO   SEQ-TEST-GF-02-02-1.                             SQ1044.2
056600     IF FILE-RECORD-INFO-P1-120 (1)                               SQ1044.2
056700                NOT EQUAL TO FILE-RECORD-INFO-P1-120 (2)          SQ1044.2
056800         ADD     1 TO RECORDS-IN-ERROR.                           SQ1044.2
056900     GO TO   SEQ-TEST-GF-02-02-LOOP.                              SQ1044.2
057000*                                                                 SQ1044.2
057100 SEQ-TEST-GF-02-02-1.                                             SQ1044.2
057200     IF XRECORD-NUMBER (2) = 649                                  SQ1044.2
057300         PERFORM PASS                                             SQ1044.2
057400     ELSE                                                         SQ1044.2
057500         MOVE   "RECORD COUNTING ERROR" TO RE-MARK                SQ1044.2
057600         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1044.2
057700         MOVE    649 TO CORRECT-18V0                              SQ1044.2
057800         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
057900         PERFORM FAIL.                                            SQ1044.2
058000*                                                                 SQ1044.2
058100     ADD     1 TO REC-CT.                                         SQ1044.2
058200     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1044.2
058300         PERFORM PASS                                             SQ1044.2
058400     ELSE                                                         SQ1044.2
058500         MOVE   "RECORD CONTENT ERRORS" TO RE-MARK                SQ1044.2
058600         MOVE    ZERO TO CORRECT-18V0                             SQ1044.2
058700         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1044.2
058800         MOVE   "VII-44; 4.4.2" TO ANSI-REFERENCE                 SQ1044.2
058900         PERFORM FAIL.                                            SQ1044.2
059000*                                                                 SQ1044.2
059100 SEQ-INIT-GF-02-05.                                               SQ1044.2
059200     GO TO   SEQ-TEST-GF-02-05.                                   SQ1044.2
059300 SEQ-DELETE-GF-02-05.                                             SQ1044.2
059400     GO TO   SEQ-TEST-GF-02-END.                                  SQ1044.2
059500 SEQ-TEST-GF-02-05.                                               SQ1044.2
059600     CLOSE   SQ-FS3.                                              SQ1044.2
059700 SEQ-TEST-GF-02-END.                                              SQ1044.2
059800*                                                                 SQ1044.2
059900*                                                                 SQ1044.2
060000 SEQ-INIT-GF-03.                                                  SQ1044.2
060100     GO TO  SEQ-TEST-GF-03.                                       SQ1044.2
060200 SEQ-DELETE-03.                                                   SQ1044.2
060300     GO TO   SEQ-TEST-03-END.                                     SQ1044.2
060400 SEQ-TEST-GF-03.                                                  SQ1044.2
060500     OPEN    INPUT SQ-FS3.                                        SQ1044.2
060600 SEQ-TEST-03-END.                                                 SQ1044.2
060700*                                                                 SQ1044.2
060800*                                                                 SQ1044.2
060900*    THIS SERIES OF TESTS CHECKS FOUR LEVEL 1 VARIANTS OF         SQ1044.2
061000*    THE READ STATEMENT                                           SQ1044.2
061100*                                                                 SQ1044.2
061200 SEQ-INIT-GF-04.                                                  SQ1044.2
061300     MOVE    ZERO TO XRECORD-NUMBER (2).                          SQ1044.2
061400     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1044.2
061500     MOVE   "READ...RECORD AT END"     TO FEATURE.                SQ1044.2
061600     MOVE   "SEQ-TEST-GF-O4" TO PAR-NAME.                         SQ1044.2
061700     MOVE    ZERO TO ERROR-FLAG.                                  SQ1044.2
061800     MOVE    1 TO REC-CT.                                         SQ1044.2
061900     GO TO  SEQ-TEST-GF-04.                                       SQ1044.2
062000 SEQ-DELETE-04.                                                   SQ1044.2
062100     PERFORM DE-LETE.                                             SQ1044.2
062200     ADD     1 TO REC-CT.                                         SQ1044.2
062300     PERFORM DE-LETE.                                             SQ1044.2
062400     GO TO   SEQ-TEST-04-END.                                     SQ1044.2
062500 SEQ-TEST-GF-04.                                                  SQ1044.2
062600     READ SQ-FS3 RECORD AT END                                    SQ1044.2
062700         MOVE    1 TO EOF-FLAG                                    SQ1044.2
062800         GO TO   SEQ-TEST-GF-04-01.                               SQ1044.2
062900     MOVE   SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1044.2
063000     ADD    1 TO XRECORD-NUMBER (2)                               SQ1044.2
063100     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1044.2
063200         ADD     1 TO RECORDS-IN-ERROR                            SQ1044.2
063300         MOVE    1 TO ERROR-FLAG.                                 SQ1044.2
063400     IF XRECORD-NUMBER (2) LESS THAN 50                           SQ1044.2
063500         GO TO   SEQ-TEST-GF-04.                                  SQ1044.2
063600*                                                                 SQ1044.2
063700 SEQ-TEST-GF-04-01.                                               SQ1044.2
063800     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1044.2
063900         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1044.2
064000         MOVE    649 TO CORRECT-18V0                              SQ1044.2
064100         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1044.2
064200         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
064300         PERFORM FAIL                                             SQ1044.2
064400     ELSE                                                         SQ1044.2
064500         PERFORM PASS.                                            SQ1044.2
064600*                                                                 SQ1044.2
064700 SEQ-TEST-GF-04-02.                                               SQ1044.2
064800     ADD     1 TO REC-CT.                                         SQ1044.2
064900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1044.2
065000         PERFORM PASS                                             SQ1044.2
065100     ELSE                                                         SQ1044.2
065200         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1044.2
065300         MOVE    ZERO TO CORRECT-18V0                             SQ1044.2
065400         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1044.2
065500         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
065600         PERFORM FAIL.                                            SQ1044.2
065700 SEQ-TEST-04-END.                                                 SQ1044.2
065800*                                                                 SQ1044.2
065900*                                                                 SQ1044.2
066000 SEQ-INIT-GF-O5.                                                  SQ1044.2
066100     IF EOF-FLAG EQUAL TO 1                                       SQ1044.2
066200         GO TO SEQ-DELETE-05.                                     SQ1044.2
066300     MOVE    ZERO TO ERROR-FLAG.                                  SQ1044.2
066400     MOVE   "READ...AT END..." TO FEATURE                         SQ1044.2
066500     MOVE   "SEQ-TEST-GF-O5" TO PAR-NAME.                         SQ1044.2
066600     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1044.2
066700     MOVE    1 TO REC-CT.                                         SQ1044.2
066800     GO TO  SEQ-TEST-GF-05.                                       SQ1044.2
066900 SEQ-DELETE-05.                                                   SQ1044.2
067000     PERFORM DE-LETE.                                             SQ1044.2
067100     ADD     1 TO REC-CT.                                         SQ1044.2
067200     PERFORM DE-LETE.                                             SQ1044.2
067300     GO TO   SEQ-TEST-05-END.                                     SQ1044.2
067400 SEQ-TEST-GF-05.                                                  SQ1044.2
067500     READ SQ-FS3 AT END                                           SQ1044.2
067600         MOVE    1 TO EOF-FLAG                                    SQ1044.2
067700         GO TO   SEQ-TEST-GF-05-01.                               SQ1044.2
067800     MOVE   SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1044.2
067900     ADD    1 TO XRECORD-NUMBER (2)                               SQ1044.2
068000     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1044.2
068100         ADD     1 TO RECORDS-IN-ERROR                            SQ1044.2
068200         MOVE    1 TO ERROR-FLAG.                                 SQ1044.2
068300     IF XRECORD-NUMBER (2) LESS THAN 200                          SQ1044.2
068400         GO TO   SEQ-TEST-GF-05.                                  SQ1044.2
068500*                                                                 SQ1044.2
068600 SEQ-TEST-GF-05-01.                                               SQ1044.2
068700     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1044.2
068800         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1044.2
068900         MOVE    649 TO CORRECT-18V0                              SQ1044.2
069000         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1044.2
069100         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
069200         PERFORM FAIL                                             SQ1044.2
069300     ELSE                                                         SQ1044.2
069400         PERFORM PASS.                                            SQ1044.2
069500*                                                                 SQ1044.2
069600 SEQ-TEST-GF-05-02.                                               SQ1044.2
069700     ADD     1 TO REC-CT.                                         SQ1044.2
069800     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1044.2
069900         PERFORM PASS                                             SQ1044.2
070000     ELSE                                                         SQ1044.2
070100         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1044.2
070200         MOVE    ZERO TO CORRECT-18V0                             SQ1044.2
070300         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1044.2
070400         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
070500         PERFORM FAIL.                                            SQ1044.2
070600 SEQ-TEST-05-END.                                                 SQ1044.2
070700*                                                                 SQ1044.2
070800*                                                                 SQ1044.2
070900 SEQ-INIT-GF-O6.                                                  SQ1044.2
071000     IF EOF-FLAG EQUAL TO 1                                       SQ1044.2
071100         GO TO SEQ-DELETE-06.                                     SQ1044.2
071200     MOVE    ZERO TO ERROR-FLAG.                                  SQ1044.2
071300     MOVE   "READ...RECORD END..." TO FEATURE                     SQ1044.2
071400     MOVE   "SEQ-TEST-GF-O6" TO PAR-NAME.                         SQ1044.2
071500     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1044.2
071600     MOVE    1 TO REC-CT.                                         SQ1044.2
071700     GO TO  SEQ-TEST-GF-06.                                       SQ1044.2
071800 SEQ-DELETE-06.                                                   SQ1044.2
071900     PERFORM DE-LETE.                                             SQ1044.2
072000     ADD     1 TO REC-CT.                                         SQ1044.2
072100     PERFORM DE-LETE.                                             SQ1044.2
072200     GO TO   SEQ-TEST-06-END.                                     SQ1044.2
072300 SEQ-TEST-GF-06.                                                  SQ1044.2
072400     READ SQ-FS3 RECORD END                                       SQ1044.2
072500         MOVE    1 TO EOF-FLAG                                    SQ1044.2
072600         GO TO   SEQ-TEST-GF-06-01.                               SQ1044.2
072700     MOVE   SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1044.2
072800     ADD    1 TO XRECORD-NUMBER (2)                               SQ1044.2
072900     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1044.2
073000         ADD     1 TO RECORDS-IN-ERROR                            SQ1044.2
073100         MOVE    1 TO ERROR-FLAG.                                 SQ1044.2
073200     IF XRECORD-NUMBER (2) LESS THAN 499                          SQ1044.2
073300         GO TO   SEQ-TEST-GF-06.                                  SQ1044.2
073400*                                                                 SQ1044.2
073500 SEQ-TEST-GF-06-01.                                               SQ1044.2
073600     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1044.2
073700         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1044.2
073800         MOVE    649 TO CORRECT-18V0                              SQ1044.2
073900         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1044.2
074000         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
074100         PERFORM FAIL                                             SQ1044.2
074200     ELSE                                                         SQ1044.2
074300         PERFORM PASS.                                            SQ1044.2
074400*                                                                 SQ1044.2
074500 SEQ-TEST-GF-06-02.                                               SQ1044.2
074600     ADD     1 TO REC-CT.                                         SQ1044.2
074700     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1044.2
074800         PERFORM PASS                                             SQ1044.2
074900     ELSE                                                         SQ1044.2
075000         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1044.2
075100         MOVE    ZERO TO CORRECT-18V0                             SQ1044.2
075200         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1044.2
075300         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
075400         PERFORM FAIL.                                            SQ1044.2
075500 SEQ-TEST-06-END.                                                 SQ1044.2
075600*                                                                 SQ1044.2
075700*                                                                 SQ1044.2
075800 SEQ-INIT-GF-O7.                                                  SQ1044.2
075900     IF EOF-FLAG EQUAL TO 1                                       SQ1044.2
076000         GO TO SEQ-DELETE-07.                                     SQ1044.2
076100     MOVE    ZERO TO ERROR-FLAG.                                  SQ1044.2
076200     MOVE   "READ... END..." TO FEATURE                           SQ1044.2
076300     MOVE   "SEQ-TEST-GF-O7" TO PAR-NAME.                         SQ1044.2
076400     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1044.2
076500     MOVE    1 TO REC-CT.                                         SQ1044.2
076600     GO TO  SEQ-TEST-GF-07.                                       SQ1044.2
076700 SEQ-DELETE-07.                                                   SQ1044.2
076800     PERFORM DE-LETE.                                             SQ1044.2
076900     ADD     1 TO REC-CT.                                         SQ1044.2
077000     PERFORM DE-LETE.                                             SQ1044.2
077100     GO TO   SEQ-TEST-07-END.                                     SQ1044.2
077200 SEQ-TEST-GF-07.                                                  SQ1044.2
077300     READ SQ-FS3 END                                              SQ1044.2
077400         MOVE    1 TO EOF-FLAG                                    SQ1044.2
077500         GO TO   SEQ-TEST-GF-07-01.                               SQ1044.2
077600     MOVE   SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1044.2
077700     ADD    1 TO XRECORD-NUMBER (2)                               SQ1044.2
077800     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1044.2
077900         ADD     1 TO RECORDS-IN-ERROR                            SQ1044.2
078000         MOVE    1 TO ERROR-FLAG.                                 SQ1044.2
078100     IF XRECORD-NUMBER (2) LESS THAN 649                          SQ1044.2
078200         GO TO   SEQ-TEST-GF-07.                                  SQ1044.2
078300*                                                                 SQ1044.2
078400 SEQ-TEST-GF-07-01.                                               SQ1044.2
078500     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1044.2
078600         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1044.2
078700         MOVE    649 TO CORRECT-18V0                              SQ1044.2
078800         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1044.2
078900         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
079000         PERFORM FAIL                                             SQ1044.2
079100     ELSE                                                         SQ1044.2
079200         PERFORM PASS.                                            SQ1044.2
079300*                                                                 SQ1044.2
079400 SEQ-TEST-GF-07-02.                                               SQ1044.2
079500     ADD     1 TO REC-CT.                                         SQ1044.2
079600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1044.2
079700         PERFORM PASS                                             SQ1044.2
079800     ELSE                                                         SQ1044.2
079900         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1044.2
080000         MOVE    ZERO TO CORRECT-18V0                             SQ1044.2
080100         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1044.2
080200         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1044.2
080300         PERFORM FAIL.                                            SQ1044.2
080400 SEQ-TEST-07-END.                                                 SQ1044.2
080500*                                                                 SQ1044.2
080600*                                                                 SQ1044.2
080700 SEQ-INIT-GF-O8.                                                  SQ1044.2
080800     IF EOF-FLAG EQUAL TO 1                                       SQ1044.2
080900         GO TO SEQ-DELETE-08.                                     SQ1044.2
081000     MOVE   "READ... END... AT EOF" TO FEATURE                    SQ1044.2
081100     MOVE   "SEQ-TEST-GF-O8" TO PAR-NAME.                         SQ1044.2
081200     MOVE    1 TO REC-CT.                                         SQ1044.2
081300     GO TO  SEQ-TEST-GF-08.                                       SQ1044.2
081400 SEQ-DELETE-08.                                                   SQ1044.2
081500     PERFORM DE-LETE.                                             SQ1044.2
081600     GO TO   SEQ-TEST-08-END.                                     SQ1044.2
081700 SEQ-TEST-GF-08.                                                  SQ1044.2
081800     READ SQ-FS3 END                                              SQ1044.2
081900         MOVE    1 TO EOF-FLAG.                                   SQ1044.2
082000*                                                                 SQ1044.2
082100 SEQ-TEST-GF-08-01.                                               SQ1044.2
082200     IF EOF-FLAG NOT EQUAL TO 1                                   SQ1044.2
082300         MOVE    EOF-FLAG TO COMPUTED-18V0                        SQ1044.2
082400         MOVE    1 TO CORRECT-18V0                                SQ1044.2
082500         MOVE   "EOF NOT FOUND AFTER 649 RECORDS" TO RE-MARK      SQ1044.2
082600         PERFORM FAIL                                             SQ1044.2
082700     ELSE                                                         SQ1044.2
082800         PERFORM PASS.                                            SQ1044.2
082900 SEQ-TEST-08-END.                                                 SQ1044.2
083000*                                                                 SQ1044.2
083100*                                                                 SQ1044.2
083200 SEQ-INIT-GF-O9.                                                  SQ1044.2
083300     GO TO  SEQ-TEST-GF-09.                                       SQ1044.2
083400 SEQ-DELETE-09.                                                   SQ1044.2
083500     GO TO   SEQ-TEST-09-END.                                     SQ1044.2
083600 SEQ-TEST-GF-09.                                                  SQ1044.2
083700     CLOSE SQ-FS3.                                                SQ1044.2
083800 SEQ-TEST-09-END.                                                 SQ1044.2
083900*                                                                 SQ1044.2
084000*                                                                 SQ1044.2
084100 TERMINATE-ROUTINE.                                               SQ1044.2
084200     EXIT.                                                        SQ1044.2
084300 CCVS-EXIT SECTION.                                               SQ1044.2
084400 CCVS-999999.                                                     SQ1044.2
084500     GO TO CLOSE-FILES.                                           SQ1044.2
      *END-OF,SQ104A                                                                  
