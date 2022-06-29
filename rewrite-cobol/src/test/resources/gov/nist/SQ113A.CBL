      *HEADER,COBOL,SQ113A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1134.2
000200 PROGRAM-ID.                                                      SQ1134.2
000300     SQ113A.                                                      SQ1134.2
000400****************************************************************  SQ1134.2
000500*                                                              *  SQ1134.2
000600*    VALIDATION FOR:-                                          *  SQ1134.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1134.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1134.2
000900*                             REVISED 1986, AUGUST             *  SQ1134.2
001000*                                                              *  SQ1134.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1134.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1134.2
001300*                                                              *  SQ1134.2
001400****************************************************************  SQ1134.2
001500*                                                              *  SQ1134.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1134.2
001700*                                                              *  SQ1134.2
001800*            X-01   SEQUENTIAL MAGNETIC TAPE                   *  SQ1134.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1134.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1134.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1134.2
002200*                                                              *  SQ1134.2
002300****************************************************************  SQ1134.2
002400*                                                              *  SQ1134.2
002500*    SQ113A CREATES A MAGNETIC TAPE FILE CONTAINING 750 FIXED  *  SQ1134.2
002600*    LENGTH RECORDS, EACH 120 CHARACTERS LONG.  THE FILE IS    *  SQ1134.2
002700*    READ TWICE.  THE FIRST PASS CHECKS THAT ALL THE EXPECTED  *  SQ1134.2
002800*    RECORDS ARE PRESENT.  THE SECOND PASS PERFORMS SIMILAR    *  SQ1134.2
002900*    CHECKS, BUT USES ALL FOUR VARIANTS OF THE READ STATEMENT  *  SQ1134.2
003000*    WITH THE END PHRASE THAT CAN BE PRODUCED BY INCLUDING OR  *  SQ1134.2
003100*    OMITTING THE OPTIONAL WORDS "RECORD" AND "AT".            *  SQ1134.2
003200*                                                              *  SQ1134.2
003300*    THE PROGRAM IS ALMOST IDENTICAL TO SQ102A, AND IS DERIVED *  SQ1134.2
003400*    FROM THAT PROGRAM BY INCLUDING A FILE STATUS CLAUSE IN    *  SQ1134.2
003500*    THE FILE-CONTROL ENTRY FOR THE TEST FILE, AND INCLUDING   *  SQ1134.2
003600*    TESTS ON THE I-O STATUS RETURNED AFTER EACH OPERATION ON  *  SQ1134.2
003700*    THE FILE                                                  *  SQ1134.2
003800*                                                              *  SQ1134.2
003900*    THE PROGRAM OMITS THE OPTIONAL WORDS "ORGANIZATION IS"    *  SQ1134.2
004000*    FROM THE "ORGANIZATION IS SEQUENTIAL" CLAUSE OF THE       *  SQ1134.2
004100*    FILE-CONTROL ENTRY, AND PLACES THE ASSIGN CLAUSE IN A     *  SQ1134.2
004200*    POSITION OTHER THAN FIRST IN THE SAME ENTRY.              *  SQ1134.2
004300*                                                              *  SQ1134.2
004400****************************************************************  SQ1134.2
004500*                                                                 SQ1134.2
004600*                                                                 SQ1134.2
004700 ENVIRONMENT DIVISION.                                            SQ1134.2
004800 CONFIGURATION SECTION.                                           SQ1134.2
004900 SOURCE-COMPUTER.                                                 SQ1134.2
005000     XXXXX082.                                                    SQ1134.2
005100 OBJECT-COMPUTER.                                                 SQ1134.2
005200     XXXXX083.                                                    SQ1134.2
005300*                                                                 SQ1134.2
005400 INPUT-OUTPUT SECTION.                                            SQ1134.2
005500 FILE-CONTROL.                                                    SQ1134.2
005600     SELECT PRINT-FILE ASSIGN TO                                  SQ1134.2
005700     XXXXX055.                                                    SQ1134.2
005800*                                                                 SQ1134.2
005900*    SELECT RAW-DATA   ASSIGN TO                                  SQ1134.2
006000*    XXXXX062                                                     SQ1134.2
006100*          ORGANIZATION IS INDEXED                                SQ1134.2
006200*          ACCESS MODE  IS RANDOM                                 SQ1134.2
006300*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1134.2
006400*                                                                 SQ1134.2
006500     SELECT SQ-FS1                                                SQ1134.2
006600            ACCESS MODE IS SEQUENTIAL                             SQ1134.2
006700            SEQUENTIAL                                            SQ1134.2
006800            ASSIGN TO                                             SQ1134.2
006900     XXXXX001                                                     SQ1134.2
007000            FILE STATUS IS SQ-FS1-STATUS.                         SQ1134.2
007100*                                                                 SQ1134.2
007200*                                                                 SQ1134.2
007300 DATA DIVISION.                                                   SQ1134.2
007400 FILE SECTION.                                                    SQ1134.2
007500 FD  PRINT-FILE                                                   SQ1134.2
007600C    LABEL RECORDS                                                SQ1134.2
007700C    XXXXX084                                                     SQ1134.2
007800C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1134.2
007900               .                                                  SQ1134.2
008000 01  PRINT-REC    PICTURE X(120).                                 SQ1134.2
008100 01  DUMMY-RECORD PICTURE X(120).                                 SQ1134.2
008200*                                                                 SQ1134.2
008300*FD  RAW-DATA.                                                    SQ1134.2
008400*01  RAW-DATA-SATZ.                                               SQ1134.2
008500*    05  RAW-DATA-KEY        PIC X(6).                            SQ1134.2
008600*    05  C-DATE              PIC 9(6).                            SQ1134.2
008700*    05  C-TIME              PIC 9(8).                            SQ1134.2
008800*    05  NO-OF-TESTS         PIC 99.                              SQ1134.2
008900*    05  C-OK                PIC 999.                             SQ1134.2
009000*    05  C-ALL               PIC 999.                             SQ1134.2
009100*    05  C-FAIL              PIC 999.                             SQ1134.2
009200*    05  C-DELETED           PIC 999.                             SQ1134.2
009300*    05  C-INSPECT           PIC 999.                             SQ1134.2
009400*    05  C-NOTE              PIC X(13).                           SQ1134.2
009500*    05  C-INDENT            PIC X.                               SQ1134.2
009600*    05  C-ABORT             PIC X(8).                            SQ1134.2
009700*                                                                 SQ1134.2
009800 FD  SQ-FS1                                                       SQ1134.2
009900C    LABEL RECORD IS STANDARD                                     SQ1134.2
010000                .                                                 SQ1134.2
010100 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1134.2
010200*                                                                 SQ1134.2
010300 WORKING-STORAGE SECTION.                                         SQ1134.2
010400*                                                                 SQ1134.2
010500***************************************************************   SQ1134.2
010600*                                                             *   SQ1134.2
010700*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1134.2
010800*                                                             *   SQ1134.2
010900***************************************************************   SQ1134.2
011000*                                                                 SQ1134.2
011100 01  SQ-FS1-STATUS.                                               SQ1134.2
011200   03  SQ-FS1-KEY-1   PIC X.                                      SQ1134.2
011300   03  SQ-FS1-KEY-2   PIC X.                                      SQ1134.2
011400*                                                                 SQ1134.2
011500 01  SQ-FS1-STATUS-COPY PIC XX.                                   SQ1134.2
011600*                                                                 SQ1134.2
011700 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                SQ1134.2
011800 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            SQ1134.2
011900 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 SQ1134.2
012000 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             SQ1134.2
012100*                                                                 SQ1134.2
012200***************************************************************   SQ1134.2
012300*                                                             *   SQ1134.2
012400*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1134.2
012500*                                                             *   SQ1134.2
012600***************************************************************   SQ1134.2
012700*                                                                 SQ1134.2
012800 01  REC-SKEL-SUB   PIC 99.                                       SQ1134.2
012900*                                                                 SQ1134.2
013000 01  FILE-RECORD-INFORMATION-REC.                                 SQ1134.2
013100     03 FILE-RECORD-INFO-SKELETON.                                SQ1134.2
013200        05 FILLER                 PICTURE X(48)       VALUE       SQ1134.2
013300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1134.2
013400        05 FILLER                 PICTURE X(46)       VALUE       SQ1134.2
013500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1134.2
013600        05 FILLER                 PICTURE X(26)       VALUE       SQ1134.2
013700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1134.2
013800        05 FILLER                 PICTURE X(37)       VALUE       SQ1134.2
013900             ",RECKEY=                             ".             SQ1134.2
014000        05 FILLER                 PICTURE X(38)       VALUE       SQ1134.2
014100             ",ALTKEY1=                             ".            SQ1134.2
014200        05 FILLER                 PICTURE X(38)       VALUE       SQ1134.2
014300             ",ALTKEY2=                             ".            SQ1134.2
014400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1134.2
014500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1134.2
014600        05 FILE-RECORD-INFO-P1-120.                               SQ1134.2
014700           07 FILLER              PIC X(5).                       SQ1134.2
014800           07 XFILE-NAME          PIC X(6).                       SQ1134.2
014900           07 FILLER              PIC X(8).                       SQ1134.2
015000           07 XRECORD-NAME        PIC X(6).                       SQ1134.2
015100           07 FILLER              PIC X(1).                       SQ1134.2
015200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1134.2
015300           07 FILLER              PIC X(7).                       SQ1134.2
015400           07 XRECORD-NUMBER      PIC 9(6).                       SQ1134.2
015500           07 FILLER              PIC X(6).                       SQ1134.2
015600           07 UPDATE-NUMBER       PIC 9(2).                       SQ1134.2
015700           07 FILLER              PIC X(5).                       SQ1134.2
015800           07 ODO-NUMBER          PIC 9(4).                       SQ1134.2
015900           07 FILLER              PIC X(5).                       SQ1134.2
016000           07 XPROGRAM-NAME       PIC X(5).                       SQ1134.2
016100           07 FILLER              PIC X(7).                       SQ1134.2
016200           07 XRECORD-LENGTH      PIC 9(6).                       SQ1134.2
016300           07 FILLER              PIC X(7).                       SQ1134.2
016400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1134.2
016500           07 FILLER              PIC X(1).                       SQ1134.2
016600           07 XBLOCK-SIZE         PIC 9(4).                       SQ1134.2
016700           07 FILLER              PIC X(6).                       SQ1134.2
016800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1134.2
016900           07 FILLER              PIC X(5).                       SQ1134.2
017000           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1134.2
017100           07 FILLER              PIC X(6).                       SQ1134.2
017200           07 XLABEL-TYPE         PIC X(1).                       SQ1134.2
017300        05 FILE-RECORD-INFO-P121-240.                             SQ1134.2
017400           07 FILLER              PIC X(8).                       SQ1134.2
017500           07 XRECORD-KEY         PIC X(29).                      SQ1134.2
017600           07 FILLER              PIC X(9).                       SQ1134.2
017700           07 ALTERNATE-KEY1      PIC X(29).                      SQ1134.2
017800           07 FILLER              PIC X(9).                       SQ1134.2
017900           07 ALTERNATE-KEY2      PIC X(29).                      SQ1134.2
018000           07 FILLER              PIC X(7).                       SQ1134.2
018100*                                                                 SQ1134.2
018200 01  TEST-RESULTS.                                                SQ1134.2
018300     02 FILLER              PIC X      VALUE SPACE.               SQ1134.2
018400     02  PAR-NAME.                                                SQ1134.2
018500       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1134.2
018600       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1134.2
018700       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1134.2
018800     02 FILLER              PIC X      VALUE SPACE.               SQ1134.2
018900     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1134.2
019000     02 FILLER              PIC X      VALUE SPACE.               SQ1134.2
019100     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1134.2
019200     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1134.2
019300     02 RE-MARK             PIC X(61).                            SQ1134.2
019400 01  TEST-COMPUTED.                                               SQ1134.2
019500   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1134.2
019600   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1134.2
019700   02 COMPUTED-X.                                                 SQ1134.2
019800     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1134.2
019900     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1134.2
020000     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1134.2
020100     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1134.2
020200     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1134.2
020300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1134.2
020400        04 COMPUTED-18V0                   PIC -9(18).            SQ1134.2
020500        04 FILLER                          PIC X.                 SQ1134.2
020600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1134.2
020700 01  TEST-CORRECT.                                                SQ1134.2
020800     02 FILLER PIC X(30) VALUE SPACE.                             SQ1134.2
020900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1134.2
021000     02 CORRECT-X.                                                SQ1134.2
021100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1134.2
021200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1134.2
021300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1134.2
021400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1134.2
021500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1134.2
021600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1134.2
021700         04 CORRECT-18V0                     PIC -9(18).          SQ1134.2
021800         04 FILLER                           PIC X.               SQ1134.2
021900     03 FILLER PIC X(2) VALUE SPACE.                              SQ1134.2
022000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1134.2
022100*                                                                 SQ1134.2
022200 01  CCVS-C-1.                                                    SQ1134.2
022300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1134.2
022400     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1134.2
022500     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1134.2
022600     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1134.2
022700     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1134.2
022800     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1134.2
022900     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1134.2
023000     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1134.2
023100 01  CCVS-C-2.                                                    SQ1134.2
023200     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1134.2
023300     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1134.2
023400     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1134.2
023500     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1134.2
023600     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1134.2
023700*                                                                 SQ1134.2
023800 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1134.2
023900 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1134.2
024000 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1134.2
024100 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1134.2
024200 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1134.2
024300 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1134.2
024400 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1134.2
024500 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1134.2
024600 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1134.2
024700 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1134.2
024800 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1134.2
024900 01  CCVS-H-1.                                                    SQ1134.2
025000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1134.2
025100     02  FILLER          PIC X(42)    VALUE                       SQ1134.2
025200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1134.2
025300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1134.2
025400 01  CCVS-H-2A.                                                   SQ1134.2
025500   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1134.2
025600   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1134.2
025700   02  FILLER            PIC XXXX   VALUE                         SQ1134.2
025800     "4.2 ".                                                      SQ1134.2
025900   02  FILLER            PIC X(28)  VALUE                         SQ1134.2
026000            " COPY - NOT FOR DISTRIBUTION".                       SQ1134.2
026100   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1134.2
026200*                                                                 SQ1134.2
026300 01  CCVS-H-2B.                                                   SQ1134.2
026400   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1134.2
026500   02  TEST-ID           PIC X(9).                                SQ1134.2
026600   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1134.2
026700   02  FILLER            PIC X(12)  VALUE                         SQ1134.2
026800     " HIGH       ".                                              SQ1134.2
026900   02  FILLER            PIC X(22)  VALUE                         SQ1134.2
027000            " LEVEL VALIDATION FOR ".                             SQ1134.2
027100   02  FILLER            PIC X(58)  VALUE                         SQ1134.2
027200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1134.2
027300 01  CCVS-H-3.                                                    SQ1134.2
027400     02  FILLER          PIC X(34)  VALUE                         SQ1134.2
027500            " FOR OFFICIAL USE ONLY    ".                         SQ1134.2
027600     02  FILLER          PIC X(58)  VALUE                         SQ1134.2
027700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1134.2
027800     02  FILLER          PIC X(28)  VALUE                         SQ1134.2
027900            "  COPYRIGHT   1985,1986 ".                           SQ1134.2
028000 01  CCVS-E-1.                                                    SQ1134.2
028100     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1134.2
028200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1134.2
028300     02 ID-AGAIN         PIC X(9).                                SQ1134.2
028400     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1134.2
028500 01  CCVS-E-2.                                                    SQ1134.2
028600     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1134.2
028700     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1134.2
028800     02  CCVS-E-2-2.                                              SQ1134.2
028900         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1134.2
029000         03 FILLER         PIC X      VALUE SPACE.                SQ1134.2
029100         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1134.2
029200            "ERRORS ENCOUNTERED".                                 SQ1134.2
029300 01  CCVS-E-3.                                                    SQ1134.2
029400     02  FILLER          PIC X(22)  VALUE                         SQ1134.2
029500            " FOR OFFICIAL USE ONLY".                             SQ1134.2
029600     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1134.2
029700     02  FILLER          PIC X(58)  VALUE                         SQ1134.2
029800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1134.2
029900     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1134.2
030000     02  FILLER          PIC X(20)  VALUE                         SQ1134.2
030100             " COPYRIGHT 1985,1986".                              SQ1134.2
030200 01  CCVS-E-4.                                                    SQ1134.2
030300     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1134.2
030400     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1134.2
030500     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1134.2
030600     02 FILLER           PIC X(40)  VALUE                         SQ1134.2
030700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1134.2
030800 01  XXINFO.                                                      SQ1134.2
030900     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1134.2
031000     02 INFO-TEXT.                                                SQ1134.2
031100       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1134.2
031200       04 XXCOMPUTED         PIC X(20).                           SQ1134.2
031300       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1134.2
031400       04 XXCORRECT          PIC X(20).                           SQ1134.2
031500     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1134.2
031600 01  HYPHEN-LINE.                                                 SQ1134.2
031700     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1134.2
031800     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1134.2
031900-    "*****************************************".                 SQ1134.2
032000     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1134.2
032100-    "******************************".                            SQ1134.2
032200 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1134.2
032300     "SQ113A".                                                    SQ1134.2
032400*                                                                 SQ1134.2
032500*                                                                 SQ1134.2
032600 PROCEDURE DIVISION.                                              SQ1134.2
032700*                                                                 SQ1134.2
032800 CCVS1 SECTION.                                                   SQ1134.2
032900 OPEN-FILES.                                                      SQ1134.2
033000*    OPEN    I-O RAW-DATA.                                        SQ1134.2
033100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1134.2
033200*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1134.2
033300*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1134.2
033400*    ADD     1           TO C-NO-OF-TESTS.                        SQ1134.2
033500*    ACCEPT  C-DATE      FROM DATE.                               SQ1134.2
033600*    ACCEPT  C-TIME      FROM TIME.                               SQ1134.2
033700*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1134.2
033800*END-E-1.                                                         SQ1134.2
033900*    CLOSE   RAW-DATA.                                            SQ1134.2
034000     OPEN    OUTPUT PRINT-FILE.                                   SQ1134.2
034100     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1134.2
034200     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1134.2
034300     MOVE    SPACE TO TEST-RESULTS.                               SQ1134.2
034400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1134.2
034500     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1134.2
034600     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1134.2
034700     GO TO CCVS1-EXIT.                                            SQ1134.2
034800*                                                                 SQ1134.2
034900 CCVS-INIT-FILE.                                                  SQ1134.2
035000     ADD     1 TO REC-SKL-SUB.                                    SQ1134.2
035100     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1134.2
035200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1134.2
035300*                                                                 SQ1134.2
035400 CLOSE-FILES.                                                     SQ1134.2
035500     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1134.2
035600     CLOSE   PRINT-FILE.                                          SQ1134.2
035700*    OPEN    I-O RAW-DATA.                                        SQ1134.2
035800*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1134.2
035900*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1134.2
036000*    MOVE   "OK.     " TO C-ABORT.                                SQ1134.2
036100*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1134.2
036200*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1134.2
036300*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1134.2
036400*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1134.2
036500*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1134.2
036600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1134.2
036700*END-E-2.                                                         SQ1134.2
036800*    CLOSE   RAW-DATA.                                            SQ1134.2
036900 TERMINATE-CCVS.                                                  SQ1134.2
037000*    EXIT    PROGRAM.                                             SQ1134.2
037100     STOP    RUN.                                                 SQ1134.2
037200*                                                                 SQ1134.2
037300 INSPT.                                                           SQ1134.2
037400     MOVE   "INSPT" TO P-OR-F.                                    SQ1134.2
037500     ADD     1 TO INSPECT-COUNTER.                                SQ1134.2
037600     PERFORM PRINT-DETAIL.                                        SQ1134.2
037700*                                                                 SQ1134.2
037800 PASS.                                                            SQ1134.2
037900     MOVE   "PASS " TO P-OR-F.                                    SQ1134.2
038000     ADD     1 TO PASS-COUNTER.                                   SQ1134.2
038100     PERFORM PRINT-DETAIL.                                        SQ1134.2
038200*                                                                 SQ1134.2
038300 FAIL.                                                            SQ1134.2
038400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1134.2
038500     ADD     1 TO ERROR-COUNTER.                                  SQ1134.2
038600     PERFORM PRINT-DETAIL.                                        SQ1134.2
038700*                                                                 SQ1134.2
038800 DE-LETE.                                                         SQ1134.2
038900     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1134.2
039000     MOVE   "*****" TO P-OR-F.                                    SQ1134.2
039100     ADD     1 TO DELETE-COUNTER.                                 SQ1134.2
039200     PERFORM PRINT-DETAIL.                                        SQ1134.2
039300*                                                                 SQ1134.2
039400 PRINT-DETAIL.                                                    SQ1134.2
039500     IF REC-CT NOT EQUAL TO ZERO                                  SQ1134.2
039600         MOVE   "." TO PARDOT-X                                   SQ1134.2
039700         MOVE    REC-CT TO DOTVALUE.                              SQ1134.2
039800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1134.2
039900     PERFORM WRITE-LINE.                                          SQ1134.2
040000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1134.2
040100         PERFORM WRITE-LINE                                       SQ1134.2
040200         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1134.2
040300     ELSE                                                         SQ1134.2
040400         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1134.2
040500     MOVE    SPACE TO P-OR-F.                                     SQ1134.2
040600     MOVE    SPACE TO COMPUTED-X.                                 SQ1134.2
040700     MOVE    SPACE TO CORRECT-X.                                  SQ1134.2
040800     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1134.2
040900     MOVE    SPACE TO RE-MARK.                                    SQ1134.2
041000*                                                                 SQ1134.2
041100 HEAD-ROUTINE.                                                    SQ1134.2
041200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1134.2
041300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1134.2
041400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1134.2
041500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1134.2
041600 COLUMN-NAMES-ROUTINE.                                            SQ1134.2
041700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1134.2
041800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1134.2
041900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1134.2
042000 END-ROUTINE.                                                     SQ1134.2
042100     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1134.2
042200     PERFORM WRITE-LINE 5 TIMES.                                  SQ1134.2
042300 END-RTN-EXIT.                                                    SQ1134.2
042400     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1134.2
042500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1134.2
042600*                                                                 SQ1134.2
042700 END-ROUTINE-1.                                                   SQ1134.2
042800     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1134.2
042900     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1134.2
043000     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1134.2
043100     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1134.2
043200     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1134.2
043300     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1134.2
043400     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1134.2
043500     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1134.2
043600     PERFORM WRITE-LINE.                                          SQ1134.2
043700     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1134.2
043800     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1134.2
043900         MOVE   "NO " TO ERROR-TOTAL                              SQ1134.2
044000     ELSE                                                         SQ1134.2
044100         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1134.2
044200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1134.2
044300     PERFORM WRITE-LINE.                                          SQ1134.2
044400 END-ROUTINE-13.                                                  SQ1134.2
044500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1134.2
044600         MOVE   "NO " TO ERROR-TOTAL                              SQ1134.2
044700     ELSE                                                         SQ1134.2
044800         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1134.2
044900     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1134.2
045000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1134.2
045100     PERFORM WRITE-LINE.                                          SQ1134.2
045200     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1134.2
045300         MOVE   "NO " TO ERROR-TOTAL                              SQ1134.2
045400     ELSE                                                         SQ1134.2
045500         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1134.2
045600     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1134.2
045700     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1134.2
045800     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1134.2
045900*                                                                 SQ1134.2
046000 WRITE-LINE.                                                      SQ1134.2
046100     ADD     1 TO RECORD-COUNT.                                   SQ1134.2
046200*    IF RECORD-COUNT GREATER 50                                   SQ1134.2
046300*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1134.2
046400*        MOVE  SPACE TO DUMMY-RECORD                              SQ1134.2
046500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1134.2
046600*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1134.2
046700*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1134.2
046800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1134.2
046900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1134.2
047000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1134.2
047100     PERFORM WRT-LN.                                              SQ1134.2
047200*                                                                 SQ1134.2
047300 WRT-LN.                                                          SQ1134.2
047400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1134.2
047500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1134.2
047600 BLANK-LINE-PRINT.                                                SQ1134.2
047700     PERFORM WRT-LN.                                              SQ1134.2
047800 FAIL-ROUTINE.                                                    SQ1134.2
047900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1134.2
048000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1134.2
048100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1134.2
048200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1134.2
048300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1134.2
048400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1134.2
048500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1134.2
048600     GO TO   FAIL-ROUTINE-EX.                                     SQ1134.2
048700 FAIL-ROUTINE-WRITE.                                              SQ1134.2
048800     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1134.2
048900     PERFORM WRITE-LINE                                           SQ1134.2
049000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1134.2
049100     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1134.2
049200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1134.2
049300     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1134.2
049400 FAIL-ROUTINE-EX.                                                 SQ1134.2
049500     EXIT.                                                        SQ1134.2
049600 BAIL-OUT.                                                        SQ1134.2
049700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1134.2
049800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1134.2
049900 BAIL-OUT-WRITE.                                                  SQ1134.2
050000     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1134.2
050100     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1134.2
050200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1134.2
050300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1134.2
050400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1134.2
050500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1134.2
050600 BAIL-OUT-EX.                                                     SQ1134.2
050700     EXIT.                                                        SQ1134.2
050800 CCVS1-EXIT.                                                      SQ1134.2
050900     EXIT.                                                        SQ1134.2
051000*                                                                 SQ1134.2
051100****************************************************************  SQ1134.2
051200*                                                              *  SQ1134.2
051300*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1134.2
051400*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1134.2
051500*                                                              *  SQ1134.2
051600****************************************************************  SQ1134.2
051700*                                                                 SQ1134.2
051800 SECT-SQ113-0001 SECTION.                                         SQ1134.2
051900 SEQ-INIT-WR-01.                                                  SQ1134.2
052000     MOVE    "SQ-FS1" TO XFILE-NAME (1).                          SQ1134.2
052100     MOVE    "R1-F-G" TO XRECORD-NAME (1).                        SQ1134.2
052200     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   SQ1134.2
052300     MOVE     000120 TO XRECORD-LENGTH (1).                       SQ1134.2
052400     MOVE    "RC"    TO CHARS-OR-RECORDS (1).                     SQ1134.2
052500     MOVE     0001   TO XBLOCK-SIZE (1).                          SQ1134.2
052600     MOVE     000750 TO RECORDS-IN-FILE (1).                      SQ1134.2
052700     MOVE    "SQ"    TO XFILE-ORGANIZATION (1).                   SQ1134.2
052800     MOVE    "S"     TO XLABEL-TYPE (1).                          SQ1134.2
052900     MOVE     ZERO   TO XRECORD-NUMBER (1).                       SQ1134.2
053000     MOVE    "CREATE 750 RECORD FILE" TO FEATURE.                 SQ1134.2
053100     MOVE    "SEQ-TEST-WR-01" TO PAR-NAME.                        SQ1134.2
053200     MOVE    "**"    TO SQ-FS1-STATUS.                            SQ1134.2
053300     MOVE    "00"    TO SQ-FS1-STATUS-COPY.                       SQ1134.2
053400*                                                                 SQ1134.2
053500 SEQ-TEST-WR-01.                                                  SQ1134.2
053600     OPEN OUTPUT SQ-FS1.                                          SQ1134.2
053700     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
053800         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
053900         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
054000*                                                                 SQ1134.2
054100 SEQ-TEST-WR-01-LOOP.                                             SQ1134.2
054200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1134.2
054300     MOVE    FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.    SQ1134.2
054400     WRITE   SQ-FS1R1-F-G-120.                                    SQ1134.2
054500     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
054600         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
054700         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
054800     IF XRECORD-NUMBER (1) LESS THAN 750                          SQ1134.2
054900         GO TO   SEQ-TEST-WR-01-LOOP.                             SQ1134.2
055000*                                                                 SQ1134.2
055100     CLOSE   SQ-FS1.                                              SQ1134.2
055200     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
055300         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
055400         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
055500*                                                                 SQ1134.2
055600     IF SQ-FS1-STATUS-COPY EQUAL "00"                             SQ1134.2
055700         PERFORM PASS                                             SQ1134.2
055800     ELSE                                                         SQ1134.2
055900         MOVE    SQ-FS1-STATUS-COPY TO COMPUTED-A                 SQ1134.2
056000         MOVE   "00" TO CORRECT-A                                 SQ1134.2
056100         MOVE   "ERROR DURING FILE CREATION" TO RE-MARK           SQ1134.2
056200         PERFORM FAIL.                                            SQ1134.2
056300*                                                                 SQ1134.2
056400*    A SEQUENTIAL TAPE FILE HAS BEEN CREATED.  IT CONTAINS 750    SQ1134.2
056500*    RECORDS, EACH 120 CHARACTERS LONG.  THE FILE WILL NOW BE     SQ1134.2
056600*    READ AND THE RECORDS VERIFIED.                               SQ1134.2
056700*                                                                 SQ1134.2
056800 SEQ-INIT-GF-02.                                                  SQ1134.2
056900     MOVE   "SEQ-TEST-GF-02" TO PAR-NAME.                         SQ1134.2
057000     MOVE   "VERIFY NEW FILE" TO FEATURE.                         SQ1134.2
057100     MOVE   "**"    TO SQ-FS1-STATUS.                             SQ1134.2
057200     MOVE    1      TO REC-CT.                                    SQ1134.2
057300     GO TO  SEQ-TEST-GF-02-01.                                    SQ1134.2
057400 SEQ-DELETE-02-01.                                                SQ1134.2
057500     PERFORM DE-LETE.                                             SQ1134.2
057600     GO TO   SEQ-DELETE-02-02.                                    SQ1134.2
057700 SEQ-TEST-GF-02-01.                                               SQ1134.2
057800     OPEN INPUT SQ-FS1.                                           SQ1134.2
057900     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
058000         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1134.2
058100         MOVE   "00" TO CORRECT-A                                 SQ1134.2
058200         MOVE   "FAILURE FILE STATUS FROM OPEN" TO RE-MARK        SQ1134.2
058300         PERFORM FAIL                                             SQ1134.2
058400         GO TO   SEQ-DELETE-02-02                                 SQ1134.2
058500     ELSE                                                         SQ1134.2
058600         PERFORM PASS.                                            SQ1134.2
058700*                                                                 SQ1134.2
058800 SEQ-INIT-GF-02-02.                                               SQ1134.2
058900     MOVE    FILE-RECORD-INFO-P1-120 (1)                          SQ1134.2
059000                TO FILE-RECORD-INFO-P1-120 (2).                   SQ1134.2
059100     MOVE    ZERO   TO XRECORD-NUMBER (2).                        SQ1134.2
059200     MOVE   "00"    TO SQ-FS1-STATUS-COPY.                        SQ1134.2
059300     GO TO   SEQ-TEST-GF-02-02.                                   SQ1134.2
059400 SEQ-DELETE-02-02.                                                SQ1134.2
059500     ADD     1 TO REC-CT.                                         SQ1134.2
059600     PERFORM DE-LETE.                                             SQ1134.2
059700     ADD     1 TO REC-CT.                                         SQ1134.2
059800     PERFORM DE-LETE.                                             SQ1134.2
059900     ADD     1 TO REC-CT.                                         SQ1134.2
060000     PERFORM DE-LETE.                                             SQ1134.2
060100     GO TO   SEQ-DELETE-GF-02-05.                                 SQ1134.2
060200 SEQ-TEST-GF-02-02.                                               SQ1134.2
060300     ADD     1 TO REC-CT.                                         SQ1134.2
060400 SEQ-TEST-GF-02-02-LOOP.                                          SQ1134.2
060500     READ SQ-FS1                                                  SQ1134.2
060600          AT END GO TO SEQ-TEST-GF-02-02-1.                       SQ1134.2
060700     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
060800         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
060900         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
061000     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1134.2
061100     ADD    1 TO XRECORD-NUMBER (2).                              SQ1134.2
061200     IF XRECORD-NUMBER (2) GREATER THAN 750                       SQ1134.2
061300         GO TO   SEQ-TEST-GF-02-02-1.                             SQ1134.2
061400     IF FILE-RECORD-INFO-P1-120 (1)                               SQ1134.2
061500                NOT EQUAL TO FILE-RECORD-INFO-P1-120 (2)          SQ1134.2
061600         ADD     1 TO RECORDS-IN-ERROR.                           SQ1134.2
061700     GO TO   SEQ-TEST-GF-02-02-LOOP.                              SQ1134.2
061800*                                                                 SQ1134.2
061900 SEQ-TEST-GF-02-02-1.                                             SQ1134.2
062000     IF XRECORD-NUMBER (2) = 750                                  SQ1134.2
062100         PERFORM PASS                                             SQ1134.2
062200     ELSE                                                         SQ1134.2
062300         MOVE   "RECORD COUNTING ERROR" TO RE-MARK                SQ1134.2
062400         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1134.2
062500         MOVE    750 TO CORRECT-18V0                              SQ1134.2
062600         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
062700         PERFORM FAIL.                                            SQ1134.2
062800*                                                                 SQ1134.2
062900     ADD     1 TO REC-CT.                                         SQ1134.2
063000     IF SQ-FS1-STATUS-COPY NOT EQUAL "00"                         SQ1134.2
063100         MOVE   "AT LEAST ONE UNSUCCESSFUL READ" TO RE-MARK       SQ1134.2
063200         MOVE    SQ-FS1-STATUS-COPY TO COMPUTED-A                 SQ1134.2
063300         MOVE   "00" TO CORRECT-A                                 SQ1134.2
063400         PERFORM FAIL                                             SQ1134.2
063500     ELSE                                                         SQ1134.2
063600         PERFORM PASS.                                            SQ1134.2
063700*                                                                 SQ1134.2
063800     ADD     1 TO REC-CT.                                         SQ1134.2
063900     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1134.2
064000         PERFORM PASS                                             SQ1134.2
064100     ELSE                                                         SQ1134.2
064200         MOVE   "RECORD CONTENT ERRORS" TO RE-MARK                SQ1134.2
064300         MOVE    ZERO TO CORRECT-18V0                             SQ1134.2
064400         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1134.2
064500         MOVE   "VII-44; 4.4.2" TO ANSI-REFERENCE                 SQ1134.2
064600         PERFORM FAIL.                                            SQ1134.2
064700*                                                                 SQ1134.2
064800 SEQ-INIT-GF-02-05.                                               SQ1134.2
064900     MOVE   "**" TO SQ-FS1-STATUS.                                SQ1134.2
065000     GO TO   SEQ-TEST-GF-02-05.                                   SQ1134.2
065100 SEQ-DELETE-GF-02-05.                                             SQ1134.2
065200     ADD     1 TO REC-CT.                                         SQ1134.2
065300     PERFORM DE-LETE.                                             SQ1134.2
065400     GO TO   SEQ-TEST-GF-02-END.                                  SQ1134.2
065500 SEQ-TEST-GF-02-05.                                               SQ1134.2
065600     ADD     1 TO REC-CT.                                         SQ1134.2
065700     CLOSE   SQ-FS1.                                              SQ1134.2
065800     IF SQ-FS1-STATUS EQUAL "00"                                  SQ1134.2
065900         PERFORM PASS                                             SQ1134.2
066000     ELSE                                                         SQ1134.2
066100         MOVE   "UNEXPECTED FILE STATUS ON CLOSE" TO RE-MARK      SQ1134.2
066200         MOVE   "00" TO CORRECT-A                                 SQ1134.2
066300         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1134.2
066400         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ1134.2
066500         PERFORM FAIL.                                            SQ1134.2
066600*                                                                 SQ1134.2
066700 SEQ-TEST-GF-02-END.                                              SQ1134.2
066800*                                                                 SQ1134.2
066900*                                                                 SQ1134.2
067000 SEQ-INIT-GF-03.                                                  SQ1134.2
067100     MOVE   "SEQ-TEST-GF-03" TO PAR-NAME.                         SQ1134.2
067200     MOVE   "OPEN FILE FOR INPUT" TO FEATURE.                     SQ1134.2
067300     MOVE   "**"    TO SQ-FS1-STATUS.                             SQ1134.2
067400     MOVE    1      TO REC-CT.                                    SQ1134.2
067500     GO TO  SEQ-TEST-GF-03.                                       SQ1134.2
067600 SEQ-DELETE-03.                                                   SQ1134.2
067700     PERFORM DE-LETE.                                             SQ1134.2
067800     GO TO   SEQ-TEST-03-END.                                     SQ1134.2
067900 SEQ-TEST-GF-03.                                                  SQ1134.2
068000     OPEN    INPUT SQ-FS1.                                        SQ1134.2
068100     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
068200         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1134.2
068300         MOVE   "00" TO CORRECT-A                                 SQ1134.2
068400         MOVE   "FAILURE FILE STATUS FROM OPEN" TO RE-MARK        SQ1134.2
068500         PERFORM FAIL                                             SQ1134.2
068600     ELSE                                                         SQ1134.2
068700         PERFORM PASS.                                            SQ1134.2
068800 SEQ-TEST-03-END.                                                 SQ1134.2
068900*                                                                 SQ1134.2
069000*                                                                 SQ1134.2
069100*    THIS SERIES OF TESTS CHECKS FOUR LEVEL 1 VARIANTS OF         SQ1134.2
069200*    THE READ STATEMENT                                           SQ1134.2
069300*                                                                 SQ1134.2
069400 SEQ-INIT-GF-04.                                                  SQ1134.2
069500     MOVE    ZERO TO XRECORD-NUMBER (2).                          SQ1134.2
069600     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1134.2
069700     MOVE   "READ...RECORD AT END"     TO FEATURE.                SQ1134.2
069800     MOVE   "SEQ-TEST-GF-O4" TO PAR-NAME.                         SQ1134.2
069900     MOVE    ZERO TO ERROR-FLAG.                                  SQ1134.2
070000     MOVE   "**"  TO SQ-FS1-STATUS-COPY.                          SQ1134.2
070100     MOVE    1 TO REC-CT.                                         SQ1134.2
070200     GO TO  SEQ-TEST-GF-04.                                       SQ1134.2
070300 SEQ-DELETE-04.                                                   SQ1134.2
070400     PERFORM DE-LETE.                                             SQ1134.2
070500     ADD     1 TO REC-CT.                                         SQ1134.2
070600     PERFORM DE-LETE.                                             SQ1134.2
070700     ADD     1 TO REC-CT.                                         SQ1134.2
070800     PERFORM DE-LETE.                                             SQ1134.2
070900     GO TO   SEQ-TEST-04-END.                                     SQ1134.2
071000 SEQ-TEST-GF-04.                                                  SQ1134.2
071100     READ SQ-FS1 RECORD AT END                                    SQ1134.2
071200         MOVE    1 TO EOF-FLAG                                    SQ1134.2
071300         GO TO   SEQ-TEST-GF-04-01.                               SQ1134.2
071400     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
071500         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
071600         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
071700     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1134.2
071800     ADD    1 TO XRECORD-NUMBER (2)                               SQ1134.2
071900     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1134.2
072000         ADD     1 TO RECORDS-IN-ERROR                            SQ1134.2
072100         MOVE    1 TO ERROR-FLAG.                                 SQ1134.2
072200     IF XRECORD-NUMBER (2) LESS THAN 200                          SQ1134.2
072300         GO TO   SEQ-TEST-GF-04.                                  SQ1134.2
072400*                                                                 SQ1134.2
072500 SEQ-TEST-GF-04-01.                                               SQ1134.2
072600     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1134.2
072700         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1134.2
072800         MOVE    750 TO CORRECT-18V0                              SQ1134.2
072900         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1134.2
073000         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
073100         PERFORM FAIL                                             SQ1134.2
073200     ELSE                                                         SQ1134.2
073300         PERFORM PASS.                                            SQ1134.2
073400*                                                                 SQ1134.2
073500 SEQ-TEST-GF-04-02.                                               SQ1134.2
073600     ADD     1 TO REC-CT.                                         SQ1134.2
073700     IF SQ-FS1-STATUS-COPY = "**"                                 SQ1134.2
073800         PERFORM PASS                                             SQ1134.2
073900     ELSE                                                         SQ1134.2
074000         MOVE    SQ-FS1-STATUS-COPY TO COMPUTED-A                 SQ1134.2
074100         MOVE   "**" TO CORRECT-A                                 SQ1134.2
074200         MOVE   "UNEXPECTED FILE STATUS FOR AT LEAST ONE READ"    SQ1134.2
074300                   TO RE-MARK                                     SQ1134.2
074400         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
074500         PERFORM FAIL.                                            SQ1134.2
074600*                                                                 SQ1134.2
074700 SEQ-TEST-GF-04-03.                                               SQ1134.2
074800     ADD     1 TO REC-CT.                                         SQ1134.2
074900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1134.2
075000         PERFORM PASS                                             SQ1134.2
075100     ELSE                                                         SQ1134.2
075200         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1134.2
075300         MOVE    ZERO TO CORRECT-18V0                             SQ1134.2
075400         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1134.2
075500         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
075600         PERFORM FAIL.                                            SQ1134.2
075700 SEQ-TEST-04-END.                                                 SQ1134.2
075800*                                                                 SQ1134.2
075900*                                                                 SQ1134.2
076000 SEQ-INIT-GF-O5.                                                  SQ1134.2
076100     IF EOF-FLAG EQUAL TO 1                                       SQ1134.2
076200         GO TO SEQ-DELETE-05.                                     SQ1134.2
076300     MOVE    ZERO TO ERROR-FLAG.                                  SQ1134.2
076400     MOVE   "READ...AT END..." TO FEATURE                         SQ1134.2
076500     MOVE   "SEQ-TEST-GF-O5" TO PAR-NAME.                         SQ1134.2
076600     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1134.2
076700     MOVE   "**"  TO SQ-FS1-STATUS-COPY.                          SQ1134.2
076800     MOVE    1 TO REC-CT.                                         SQ1134.2
076900     GO TO  SEQ-TEST-GF-05.                                       SQ1134.2
077000 SEQ-DELETE-05.                                                   SQ1134.2
077100     PERFORM DE-LETE.                                             SQ1134.2
077200     ADD     1 TO REC-CT.                                         SQ1134.2
077300     PERFORM DE-LETE.                                             SQ1134.2
077400     ADD     1 TO REC-CT.                                         SQ1134.2
077500     PERFORM DE-LETE.                                             SQ1134.2
077600     GO TO   SEQ-TEST-05-END.                                     SQ1134.2
077700 SEQ-TEST-GF-05.                                                  SQ1134.2
077800     READ SQ-FS1 AT END                                           SQ1134.2
077900         MOVE    1 TO EOF-FLAG                                    SQ1134.2
078000         GO TO   SEQ-TEST-GF-05-01.                               SQ1134.2
078100     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
078200         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
078300         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
078400     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1134.2
078500     ADD    1 TO XRECORD-NUMBER (2)                               SQ1134.2
078600     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1134.2
078700         ADD     1 TO RECORDS-IN-ERROR                            SQ1134.2
078800         MOVE    1 TO ERROR-FLAG.                                 SQ1134.2
078900     IF XRECORD-NUMBER (2) LESS THAN 400                          SQ1134.2
079000         GO TO   SEQ-TEST-GF-05.                                  SQ1134.2
079100*                                                                 SQ1134.2
079200 SEQ-TEST-GF-05-01.                                               SQ1134.2
079300     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1134.2
079400         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1134.2
079500         MOVE    750 TO CORRECT-18V0                              SQ1134.2
079600         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1134.2
079700         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
079800         PERFORM FAIL                                             SQ1134.2
079900     ELSE                                                         SQ1134.2
080000         PERFORM PASS.                                            SQ1134.2
080100*                                                                 SQ1134.2
080200 SEQ-TEST-GF-05-02.                                               SQ1134.2
080300     ADD     1 TO REC-CT.                                         SQ1134.2
080400     IF SQ-FS1-STATUS-COPY = "**"                                 SQ1134.2
080500         PERFORM PASS                                             SQ1134.2
080600     ELSE                                                         SQ1134.2
080700         MOVE    SQ-FS1-STATUS-COPY TO COMPUTED-A                 SQ1134.2
080800         MOVE   "**" TO CORRECT-A                                 SQ1134.2
080900         MOVE   "UNEXPECTED FILE STATUS FOR AT LEAST ONE READ"    SQ1134.2
081000                   TO RE-MARK                                     SQ1134.2
081100         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
081200         PERFORM FAIL.                                            SQ1134.2
081300*                                                                 SQ1134.2
081400 SEQ-TEST-GF-05-03.                                               SQ1134.2
081500     ADD     1 TO REC-CT.                                         SQ1134.2
081600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1134.2
081700         PERFORM PASS                                             SQ1134.2
081800     ELSE                                                         SQ1134.2
081900         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1134.2
082000         MOVE    ZERO TO CORRECT-18V0                             SQ1134.2
082100         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1134.2
082200         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
082300         PERFORM FAIL.                                            SQ1134.2
082400 SEQ-TEST-05-END.                                                 SQ1134.2
082500*                                                                 SQ1134.2
082600*                                                                 SQ1134.2
082700 SEQ-INIT-GF-O6.                                                  SQ1134.2
082800     IF EOF-FLAG EQUAL TO 1                                       SQ1134.2
082900         GO TO SEQ-DELETE-06.                                     SQ1134.2
083000     MOVE    ZERO TO ERROR-FLAG.                                  SQ1134.2
083100     MOVE   "READ...RECORD END..." TO FEATURE                     SQ1134.2
083200     MOVE   "SEQ-TEST-GF-O6" TO PAR-NAME.                         SQ1134.2
083300     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1134.2
083400     MOVE   "**"  TO SQ-FS1-STATUS-COPY.                          SQ1134.2
083500     MOVE    1 TO REC-CT.                                         SQ1134.2
083600     GO TO  SEQ-TEST-GF-06.                                       SQ1134.2
083700 SEQ-DELETE-06.                                                   SQ1134.2
083800     PERFORM DE-LETE.                                             SQ1134.2
083900     ADD     1 TO REC-CT.                                         SQ1134.2
084000     PERFORM DE-LETE.                                             SQ1134.2
084100     ADD     1 TO REC-CT.                                         SQ1134.2
084200     PERFORM DE-LETE.                                             SQ1134.2
084300     GO TO   SEQ-TEST-06-END.                                     SQ1134.2
084400 SEQ-TEST-GF-06.                                                  SQ1134.2
084500     READ SQ-FS1 RECORD END                                       SQ1134.2
084600         MOVE    1 TO EOF-FLAG                                    SQ1134.2
084700         GO TO   SEQ-TEST-GF-06-01.                               SQ1134.2
084800     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
084900         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
085000         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
085100     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1134.2
085200     ADD    1 TO XRECORD-NUMBER (2)                               SQ1134.2
085300     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1134.2
085400         ADD     1 TO RECORDS-IN-ERROR                            SQ1134.2
085500         MOVE    1 TO ERROR-FLAG.                                 SQ1134.2
085600     IF XRECORD-NUMBER (2) LESS THAN 600                          SQ1134.2
085700         GO TO   SEQ-TEST-GF-06.                                  SQ1134.2
085800*                                                                 SQ1134.2
085900 SEQ-TEST-GF-06-01.                                               SQ1134.2
086000     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1134.2
086100         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1134.2
086200         MOVE    750 TO CORRECT-18V0                              SQ1134.2
086300         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1134.2
086400         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
086500         PERFORM FAIL                                             SQ1134.2
086600     ELSE                                                         SQ1134.2
086700         PERFORM PASS.                                            SQ1134.2
086800*                                                                 SQ1134.2
086900 SEQ-TEST-GF-06-02.                                               SQ1134.2
087000     ADD     1 TO REC-CT.                                         SQ1134.2
087100     IF SQ-FS1-STATUS-COPY = "**"                                 SQ1134.2
087200         PERFORM PASS                                             SQ1134.2
087300     ELSE                                                         SQ1134.2
087400         MOVE    SQ-FS1-STATUS-COPY TO COMPUTED-A                 SQ1134.2
087500         MOVE   "**" TO CORRECT-A                                 SQ1134.2
087600         MOVE   "UNEXPECTED FILE STATUS FOR AT LEAST ONE READ"    SQ1134.2
087700                   TO RE-MARK                                     SQ1134.2
087800         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
087900         PERFORM FAIL.                                            SQ1134.2
088000*                                                                 SQ1134.2
088100 SEQ-TEST-GF-06-03.                                               SQ1134.2
088200     ADD     1 TO REC-CT.                                         SQ1134.2
088300     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1134.2
088400         PERFORM PASS                                             SQ1134.2
088500     ELSE                                                         SQ1134.2
088600         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1134.2
088700         MOVE    ZERO TO CORRECT-18V0                             SQ1134.2
088800         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1134.2
088900         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
089000         PERFORM FAIL.                                            SQ1134.2
089100 SEQ-TEST-06-END.                                                 SQ1134.2
089200*                                                                 SQ1134.2
089300*                                                                 SQ1134.2
089400 SEQ-INIT-GF-O7.                                                  SQ1134.2
089500     IF EOF-FLAG EQUAL TO 1                                       SQ1134.2
089600         GO TO SEQ-DELETE-07.                                     SQ1134.2
089700     MOVE    ZERO TO ERROR-FLAG.                                  SQ1134.2
089800     MOVE   "READ... END..." TO FEATURE                           SQ1134.2
089900     MOVE   "SEQ-TEST-GF-O7" TO PAR-NAME.                         SQ1134.2
090000     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1134.2
090100     MOVE   "**"  TO SQ-FS1-STATUS-COPY.                          SQ1134.2
090200     MOVE    1 TO REC-CT.                                         SQ1134.2
090300     GO TO  SEQ-TEST-GF-07.                                       SQ1134.2
090400 SEQ-DELETE-07.                                                   SQ1134.2
090500     PERFORM DE-LETE.                                             SQ1134.2
090600     ADD     1 TO REC-CT.                                         SQ1134.2
090700     PERFORM DE-LETE.                                             SQ1134.2
090800     ADD     1 TO REC-CT.                                         SQ1134.2
090900     PERFORM DE-LETE.                                             SQ1134.2
091000     GO TO   SEQ-TEST-07-END.                                     SQ1134.2
091100 SEQ-TEST-GF-07.                                                  SQ1134.2
091200     READ SQ-FS1 END                                              SQ1134.2
091300         MOVE    1 TO EOF-FLAG                                    SQ1134.2
091400         GO TO   SEQ-TEST-GF-07-01.                               SQ1134.2
091500     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
091600         MOVE    SQ-FS1-STATUS TO SQ-FS1-STATUS-COPY              SQ1134.2
091700         MOVE   "00" TO SQ-FS1-STATUS.                            SQ1134.2
091800     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1134.2
091900     ADD    1 TO XRECORD-NUMBER (2)                               SQ1134.2
092000     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1134.2
092100         ADD     1 TO RECORDS-IN-ERROR                            SQ1134.2
092200         MOVE    1 TO ERROR-FLAG.                                 SQ1134.2
092300     IF XRECORD-NUMBER (2) LESS THAN 750                          SQ1134.2
092400         GO TO   SEQ-TEST-GF-07.                                  SQ1134.2
092500*                                                                 SQ1134.2
092600 SEQ-TEST-GF-07-01.                                               SQ1134.2
092700     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1134.2
092800         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1134.2
092900         MOVE    750 TO CORRECT-18V0                              SQ1134.2
093000         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1134.2
093100         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
093200         PERFORM FAIL                                             SQ1134.2
093300     ELSE                                                         SQ1134.2
093400         PERFORM PASS.                                            SQ1134.2
093500*                                                                 SQ1134.2
093600 SEQ-TEST-GF-07-02.                                               SQ1134.2
093700     ADD     1 TO REC-CT.                                         SQ1134.2
093800     IF SQ-FS1-STATUS-COPY = "**"                                 SQ1134.2
093900         PERFORM PASS                                             SQ1134.2
094000     ELSE                                                         SQ1134.2
094100         MOVE    SQ-FS1-STATUS-COPY TO COMPUTED-A                 SQ1134.2
094200         MOVE   "**" TO CORRECT-A                                 SQ1134.2
094300         MOVE   "UNEXPECTED FILE STATUS FOR AT LEAST ONE READ"    SQ1134.2
094400                   TO RE-MARK                                     SQ1134.2
094500         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
094600         PERFORM FAIL.                                            SQ1134.2
094700*                                                                 SQ1134.2
094800 SEQ-TEST-GF-07-03.                                               SQ1134.2
094900     ADD     1 TO REC-CT.                                         SQ1134.2
095000     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1134.2
095100         PERFORM PASS                                             SQ1134.2
095200     ELSE                                                         SQ1134.2
095300         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1134.2
095400         MOVE    ZERO TO CORRECT-18V0                             SQ1134.2
095500         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1134.2
095600         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1134.2
095700         PERFORM FAIL.                                            SQ1134.2
095800 SEQ-TEST-07-END.                                                 SQ1134.2
095900*                                                                 SQ1134.2
096000*                                                                 SQ1134.2
096100 SEQ-INIT-GF-O8.                                                  SQ1134.2
096200     IF EOF-FLAG EQUAL TO 1                                       SQ1134.2
096300         GO TO SEQ-DELETE-08.                                     SQ1134.2
096400     MOVE   "READ... END... AT EOF" TO FEATURE                    SQ1134.2
096500     MOVE   "SEQ-TEST-GF-O8" TO PAR-NAME.                         SQ1134.2
096600     MOVE   "**"  TO SQ-FS1-STATUS.                               SQ1134.2
096700     MOVE    1 TO REC-CT.                                         SQ1134.2
096800     GO TO  SEQ-TEST-GF-08.                                       SQ1134.2
096900 SEQ-DELETE-08.                                                   SQ1134.2
097000     PERFORM DE-LETE.                                             SQ1134.2
097100     GO TO   SEQ-TEST-08-END.                                     SQ1134.2
097200 SEQ-TEST-GF-08.                                                  SQ1134.2
097300     READ SQ-FS1 END                                              SQ1134.2
097400         MOVE    1 TO EOF-FLAG.                                   SQ1134.2
097500     IF SQ-FS1-STATUS NOT EQUAL TO "10"                           SQ1134.2
097600         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1134.2
097700         MOVE   "00" TO CORRECT-A                                 SQ1134.2
097800         MOVE   "EXPECTED EOF STATUS CODE NOT RETURNED"           SQ1134.2
097900                    TO RE-MARK                                    SQ1134.2
098000         MOVE   "VII-3, VII-44" TO ANSI-REFERENCE                 SQ1134.2
098100         PERFORM FAIL                                             SQ1134.2
098200     ELSE                                                         SQ1134.2
098300         PERFORM PASS.                                            SQ1134.2
098400*                                                                 SQ1134.2
098500 SEQ-TEST-GF-08-02.                                               SQ1134.2
098600     ADD     1 TO REC-CT.                                         SQ1134.2
098700     IF EOF-FLAG NOT EQUAL TO 1                                   SQ1134.2
098800         MOVE    EOF-FLAG TO COMPUTED-18V0                        SQ1134.2
098900         MOVE    1 TO CORRECT-18V0                                SQ1134.2
099000         MOVE   "EOF NOT FOUND AFTER 750 RECORDS" TO RE-MARK      SQ1134.2
099100         PERFORM FAIL                                             SQ1134.2
099200     ELSE                                                         SQ1134.2
099300         PERFORM PASS.                                            SQ1134.2
099400 SEQ-TEST-08-END.                                                 SQ1134.2
099500*                                                                 SQ1134.2
099600*                                                                 SQ1134.2
099700 SEQ-INIT-GF-O9.                                                  SQ1134.2
099800     MOVE   "CLOSE FILE " TO FEATURE                              SQ1134.2
099900     MOVE   "SEQ-TEST-GF-O9" TO PAR-NAME.                         SQ1134.2
100000     MOVE   "**"  TO SQ-FS1-STATUS.                               SQ1134.2
100100     MOVE    1 TO REC-CT.                                         SQ1134.2
100200     GO TO  SEQ-TEST-GF-09.                                       SQ1134.2
100300 SEQ-DELETE-09.                                                   SQ1134.2
100400     PERFORM DE-LETE.                                             SQ1134.2
100500     GO TO   SEQ-TEST-09-END.                                     SQ1134.2
100600 SEQ-TEST-GF-09.                                                  SQ1134.2
100700     CLOSE SQ-FS1.                                                SQ1134.2
100800     IF SQ-FS1-STATUS NOT EQUAL TO "00"                           SQ1134.2
100900         MOVE    SQ-FS1-STATUS TO COMPUTED-A                      SQ1134.2
101000         MOVE   "00" TO CORRECT-A                                 SQ1134.2
101100         MOVE   "UNEXPECTED FILE STATUS FROM CLOSE" TO RE-MARK    SQ1134.2
101200         PERFORM FAIL                                             SQ1134.2
101300     ELSE                                                         SQ1134.2
101400         PERFORM PASS.                                            SQ1134.2
101500 SEQ-TEST-09-END.                                                 SQ1134.2
101600*                                                                 SQ1134.2
101700*                                                                 SQ1134.2
101800 TERMINATE-ROUTINE.                                               SQ1134.2
101900     EXIT.                                                        SQ1134.2
102000 CCVS-EXIT SECTION.                                               SQ1134.2
102100 CCVS-999999.                                                     SQ1134.2
102200     GO TO CLOSE-FILES.                                           SQ1134.2
      *END-OF,SQ113A                                                                  
