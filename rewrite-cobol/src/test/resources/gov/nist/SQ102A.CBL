      *HEADER,COBOL,SQ102A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1024.2
000200 PROGRAM-ID.                                                      SQ1024.2
000300     SQ102A.                                                      SQ1024.2
000400****************************************************************  SQ1024.2
000500*                                                              *  SQ1024.2
000600*    VALIDATION FOR:-                                          *  SQ1024.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1024.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1024.2
000900*                             REVISED 1986, AUGUST             *  SQ1024.2
001000*                                                              *  SQ1024.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1024.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1024.2
001300*                                                              *  SQ1024.2
001400****************************************************************  SQ1024.2
001500*                                                              *  SQ1024.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1024.2
001700*                                                              *  SQ1024.2
001800*            X-01   SEQUENTIAL MAGNETIC TAPE                   *  SQ1024.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1024.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1024.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1024.2
002200*                                                              *  SQ1024.2
002300****************************************************************  SQ1024.2
002400*                                                              *  SQ1024.2
002500*    SQ102A CREATES A MAGNETIC TAPE FILE CONTAINING 750 FIXED  *  SQ1024.2
002600*    LENGTH RECORDS, EACH 120 CHARACTERS LONG.  THE FILE IS    *  SQ1024.2
002700*    READ TWICE.  THE FIRST PASS CHECKS THAT ALL THE EXPECTED  *  SQ1024.2
002800*    RECORDS ARE PRESENT.  THE SECOND PASS PERFORMS SIMILAR    *  SQ1024.2
002900*    CHECKS, BUT USES ALL FOUR VARIANTS OF THE READ STATEMENT  *  SQ1024.2
003000*    WITH THE END PHRASE THAT CAN BE PRODUCED BY INCLUDING OR  *  SQ1024.2
003100*    OMITTING THE OPTIONAL WORDS "RECORD" AND "AT".            *  SQ1024.2
003200*                                                              *  SQ1024.2
003300*    THE PROGRAM OMITS THE OPTIONAL WORDS "ORGANIZATION IS"    *  SQ1024.2
003400*    FROM THE "ORGANIZATION IS SEQUENTIAL" CLAUSE OF THE       *  SQ1024.2
003500*    FILE-CONTROL ENTRY, AND PLACES THE ASSIGN CLAUSE IN A     *  SQ1024.2
003600*    POSITION OTHER THAN FIRST IN THE SAME ENTRY.              *  SQ1024.2
003700*                                                              *  SQ1024.2
003800****************************************************************  SQ1024.2
003900*                                                                 SQ1024.2
004000*                                                                 SQ1024.2
004100 ENVIRONMENT DIVISION.                                            SQ1024.2
004200 CONFIGURATION SECTION.                                           SQ1024.2
004300 SOURCE-COMPUTER.                                                 SQ1024.2
004400     XXXXX082.                                                    SQ1024.2
004500 OBJECT-COMPUTER.                                                 SQ1024.2
004600     XXXXX083.                                                    SQ1024.2
004700*                                                                 SQ1024.2
004800 INPUT-OUTPUT SECTION.                                            SQ1024.2
004900 FILE-CONTROL.                                                    SQ1024.2
005000     SELECT PRINT-FILE ASSIGN TO                                  SQ1024.2
005100     XXXXX055.                                                    SQ1024.2
005200*                                                                 SQ1024.2
005300*    SELECT RAW-DATA   ASSIGN TO                                  SQ1024.2
005400*    XXXXX062                                                     SQ1024.2
005500*          ORGANIZATION IS INDEXED                                SQ1024.2
005600*          ACCESS MODE  IS RANDOM                                 SQ1024.2
005700*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1024.2
005800*                                                                 SQ1024.2
005900     SELECT SQ-FS1                                                SQ1024.2
006000            ACCESS MODE IS SEQUENTIAL                             SQ1024.2
006100            SEQUENTIAL                                            SQ1024.2
006200            ASSIGN TO                                             SQ1024.2
006300     XXXXX001                                                     SQ1024.2
006400            .                                                     SQ1024.2
006500*                                                                 SQ1024.2
006600*                                                                 SQ1024.2
006700 DATA DIVISION.                                                   SQ1024.2
006800 FILE SECTION.                                                    SQ1024.2
006900 FD  PRINT-FILE                                                   SQ1024.2
007000C    LABEL RECORDS                                                SQ1024.2
007100C    XXXXX084                                                     SQ1024.2
007200C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1024.2
007300               .                                                  SQ1024.2
007400 01  PRINT-REC    PICTURE X(120).                                 SQ1024.2
007500 01  DUMMY-RECORD PICTURE X(120).                                 SQ1024.2
007600*                                                                 SQ1024.2
007700*FD  RAW-DATA.                                                    SQ1024.2
007800*01  RAW-DATA-SATZ.                                               SQ1024.2
007900*    05  RAW-DATA-KEY        PIC X(6).                            SQ1024.2
008000*    05  C-DATE              PIC 9(6).                            SQ1024.2
008100*    05  C-TIME              PIC 9(8).                            SQ1024.2
008200*    05  NO-OF-TESTS         PIC 99.                              SQ1024.2
008300*    05  C-OK                PIC 999.                             SQ1024.2
008400*    05  C-ALL               PIC 999.                             SQ1024.2
008500*    05  C-FAIL              PIC 999.                             SQ1024.2
008600*    05  C-DELETED           PIC 999.                             SQ1024.2
008700*    05  C-INSPECT           PIC 999.                             SQ1024.2
008800*    05  C-NOTE              PIC X(13).                           SQ1024.2
008900*    05  C-INDENT            PIC X.                               SQ1024.2
009000*    05  C-ABORT             PIC X(8).                            SQ1024.2
009100*                                                                 SQ1024.2
009200 FD  SQ-FS1                                                       SQ1024.2
009300C    LABEL RECORD IS STANDARD                                     SQ1024.2
009400                .                                                 SQ1024.2
009500 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1024.2
009600*                                                                 SQ1024.2
009700 WORKING-STORAGE SECTION.                                         SQ1024.2
009800*                                                                 SQ1024.2
009900***************************************************************   SQ1024.2
010000*                                                             *   SQ1024.2
010100*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1024.2
010200*                                                             *   SQ1024.2
010300***************************************************************   SQ1024.2
010400*                                                                 SQ1024.2
010500 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            SQ1024.2
010600 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 SQ1024.2
010700 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             SQ1024.2
010800*                                                                 SQ1024.2
010900***************************************************************   SQ1024.2
011000*                                                             *   SQ1024.2
011100*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1024.2
011200*                                                             *   SQ1024.2
011300***************************************************************   SQ1024.2
011400*                                                                 SQ1024.2
011500 01  REC-SKEL-SUB   PIC 99.                                       SQ1024.2
011600*                                                                 SQ1024.2
011700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1024.2
011800     03 FILE-RECORD-INFO-SKELETON.                                SQ1024.2
011900        05 FILLER                 PICTURE X(48)       VALUE       SQ1024.2
012000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1024.2
012100        05 FILLER                 PICTURE X(46)       VALUE       SQ1024.2
012200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1024.2
012300        05 FILLER                 PICTURE X(26)       VALUE       SQ1024.2
012400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1024.2
012500        05 FILLER                 PICTURE X(37)       VALUE       SQ1024.2
012600             ",RECKEY=                             ".             SQ1024.2
012700        05 FILLER                 PICTURE X(38)       VALUE       SQ1024.2
012800             ",ALTKEY1=                             ".            SQ1024.2
012900        05 FILLER                 PICTURE X(38)       VALUE       SQ1024.2
013000             ",ALTKEY2=                             ".            SQ1024.2
013100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1024.2
013200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1024.2
013300        05 FILE-RECORD-INFO-P1-120.                               SQ1024.2
013400           07 FILLER              PIC X(5).                       SQ1024.2
013500           07 XFILE-NAME          PIC X(6).                       SQ1024.2
013600           07 FILLER              PIC X(8).                       SQ1024.2
013700           07 XRECORD-NAME        PIC X(6).                       SQ1024.2
013800           07 FILLER              PIC X(1).                       SQ1024.2
013900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1024.2
014000           07 FILLER              PIC X(7).                       SQ1024.2
014100           07 XRECORD-NUMBER      PIC 9(6).                       SQ1024.2
014200           07 FILLER              PIC X(6).                       SQ1024.2
014300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1024.2
014400           07 FILLER              PIC X(5).                       SQ1024.2
014500           07 ODO-NUMBER          PIC 9(4).                       SQ1024.2
014600           07 FILLER              PIC X(5).                       SQ1024.2
014700           07 XPROGRAM-NAME       PIC X(5).                       SQ1024.2
014800           07 FILLER              PIC X(7).                       SQ1024.2
014900           07 XRECORD-LENGTH      PIC 9(6).                       SQ1024.2
015000           07 FILLER              PIC X(7).                       SQ1024.2
015100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1024.2
015200           07 FILLER              PIC X(1).                       SQ1024.2
015300           07 XBLOCK-SIZE         PIC 9(4).                       SQ1024.2
015400           07 FILLER              PIC X(6).                       SQ1024.2
015500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1024.2
015600           07 FILLER              PIC X(5).                       SQ1024.2
015700           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1024.2
015800           07 FILLER              PIC X(6).                       SQ1024.2
015900           07 XLABEL-TYPE         PIC X(1).                       SQ1024.2
016000        05 FILE-RECORD-INFO-P121-240.                             SQ1024.2
016100           07 FILLER              PIC X(8).                       SQ1024.2
016200           07 XRECORD-KEY         PIC X(29).                      SQ1024.2
016300           07 FILLER              PIC X(9).                       SQ1024.2
016400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1024.2
016500           07 FILLER              PIC X(9).                       SQ1024.2
016600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1024.2
016700           07 FILLER              PIC X(7).                       SQ1024.2
016800*                                                                 SQ1024.2
016900 01  TEST-RESULTS.                                                SQ1024.2
017000     02 FILLER              PIC X      VALUE SPACE.               SQ1024.2
017100     02  PAR-NAME.                                                SQ1024.2
017200       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1024.2
017300       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1024.2
017400       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1024.2
017500     02 FILLER              PIC X      VALUE SPACE.               SQ1024.2
017600     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1024.2
017700     02 FILLER              PIC X      VALUE SPACE.               SQ1024.2
017800     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1024.2
017900     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1024.2
018000     02 RE-MARK             PIC X(61).                            SQ1024.2
018100 01  TEST-COMPUTED.                                               SQ1024.2
018200   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1024.2
018300   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1024.2
018400   02 COMPUTED-X.                                                 SQ1024.2
018500     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1024.2
018600     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1024.2
018700     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1024.2
018800     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1024.2
018900     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1024.2
019000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1024.2
019100        04 COMPUTED-18V0                   PIC -9(18).            SQ1024.2
019200        04 FILLER                          PIC X.                 SQ1024.2
019300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1024.2
019400 01  TEST-CORRECT.                                                SQ1024.2
019500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1024.2
019600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1024.2
019700     02 CORRECT-X.                                                SQ1024.2
019800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1024.2
019900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1024.2
020000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1024.2
020100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1024.2
020200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1024.2
020300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1024.2
020400         04 CORRECT-18V0                     PIC -9(18).          SQ1024.2
020500         04 FILLER                           PIC X.               SQ1024.2
020600     03 FILLER PIC X(2) VALUE SPACE.                              SQ1024.2
020700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1024.2
020800 01  CCVS-C-1.                                                    SQ1024.2
020900     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1024.2
021000     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1024.2
021100     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1024.2
021200     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1024.2
021300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1024.2
021400     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1024.2
021500     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1024.2
021600     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1024.2
021700 01  CCVS-C-2.                                                    SQ1024.2
021800     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1024.2
021900     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1024.2
022000     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1024.2
022100     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1024.2
022200     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1024.2
022300 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1024.2
022400 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1024.2
022500 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1024.2
022600 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1024.2
022700 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1024.2
022800 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1024.2
022900 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1024.2
023000 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1024.2
023100 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1024.2
023200 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1024.2
023300 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1024.2
023400 01  CCVS-H-1.                                                    SQ1024.2
023500     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1024.2
023600     02  FILLER          PIC X(42)    VALUE                       SQ1024.2
023700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1024.2
023800     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1024.2
023900 01  CCVS-H-2A.                                                   SQ1024.2
024000   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1024.2
024100   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1024.2
024200   02  FILLER            PIC XXXX   VALUE                         SQ1024.2
024300     "4.2 ".                                                      SQ1024.2
024400   02  FILLER            PIC X(28)  VALUE                         SQ1024.2
024500            " COPY - NOT FOR DISTRIBUTION".                       SQ1024.2
024600   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1024.2
024700*                                                                 SQ1024.2
024800 01  CCVS-H-2B.                                                   SQ1024.2
024900   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1024.2
025000   02  TEST-ID           PIC X(9).                                SQ1024.2
025100   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1024.2
025200   02  FILLER            PIC X(12)  VALUE                         SQ1024.2
025300     " HIGH       ".                                              SQ1024.2
025400   02  FILLER            PIC X(22)  VALUE                         SQ1024.2
025500            " LEVEL VALIDATION FOR ".                             SQ1024.2
025600   02  FILLER            PIC X(58)  VALUE                         SQ1024.2
025700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1024.2
025800 01  CCVS-H-3.                                                    SQ1024.2
025900     02  FILLER          PIC X(34)  VALUE                         SQ1024.2
026000            " FOR OFFICIAL USE ONLY    ".                         SQ1024.2
026100     02  FILLER          PIC X(58)  VALUE                         SQ1024.2
026200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1024.2
026300     02  FILLER          PIC X(28)  VALUE                         SQ1024.2
026400            "  COPYRIGHT   1985,1986 ".                           SQ1024.2
026500 01  CCVS-E-1.                                                    SQ1024.2
026600     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1024.2
026700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1024.2
026800     02 ID-AGAIN         PIC X(9).                                SQ1024.2
026900     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1024.2
027000 01  CCVS-E-2.                                                    SQ1024.2
027100     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1024.2
027200     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1024.2
027300     02  CCVS-E-2-2.                                              SQ1024.2
027400         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1024.2
027500         03 FILLER         PIC X      VALUE SPACE.                SQ1024.2
027600         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1024.2
027700            "ERRORS ENCOUNTERED".                                 SQ1024.2
027800 01  CCVS-E-3.                                                    SQ1024.2
027900     02  FILLER          PIC X(22)  VALUE                         SQ1024.2
028000            " FOR OFFICIAL USE ONLY".                             SQ1024.2
028100     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1024.2
028200     02  FILLER          PIC X(58)  VALUE                         SQ1024.2
028300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1024.2
028400     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1024.2
028500     02  FILLER          PIC X(20)  VALUE                         SQ1024.2
028600             " COPYRIGHT 1985,1986".                              SQ1024.2
028700 01  CCVS-E-4.                                                    SQ1024.2
028800     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1024.2
028900     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1024.2
029000     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1024.2
029100     02 FILLER           PIC X(40)  VALUE                         SQ1024.2
029200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1024.2
029300 01  XXINFO.                                                      SQ1024.2
029400     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1024.2
029500     02 INFO-TEXT.                                                SQ1024.2
029600       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1024.2
029700       04 XXCOMPUTED         PIC X(20).                           SQ1024.2
029800       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1024.2
029900       04 XXCORRECT          PIC X(20).                           SQ1024.2
030000     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1024.2
030100 01  HYPHEN-LINE.                                                 SQ1024.2
030200     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1024.2
030300     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1024.2
030400-    "*****************************************".                 SQ1024.2
030500     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1024.2
030600-    "******************************".                            SQ1024.2
030700 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1024.2
030800     "SQ102A".                                                    SQ1024.2
030900*                                                                 SQ1024.2
031000*                                                                 SQ1024.2
031100 PROCEDURE DIVISION.                                              SQ1024.2
031200*                                                                 SQ1024.2
031300 CCVS1 SECTION.                                                   SQ1024.2
031400 OPEN-FILES.                                                      SQ1024.2
031500*    OPEN    I-O RAW-DATA.                                        SQ1024.2
031600*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1024.2
031700*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1024.2
031800*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1024.2
031900*    ADD     1           TO C-NO-OF-TESTS.                        SQ1024.2
032000*    ACCEPT  C-DATE      FROM DATE.                               SQ1024.2
032100*    ACCEPT  C-TIME      FROM TIME.                               SQ1024.2
032200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1024.2
032300*END-E-1.                                                         SQ1024.2
032400*    CLOSE   RAW-DATA.                                            SQ1024.2
032500     OPEN    OUTPUT PRINT-FILE.                                   SQ1024.2
032600     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1024.2
032700     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1024.2
032800     MOVE    SPACE TO TEST-RESULTS.                               SQ1024.2
032900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1024.2
033000     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1024.2
033100     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1024.2
033200     GO TO CCVS1-EXIT.                                            SQ1024.2
033300*                                                                 SQ1024.2
033400 CCVS-INIT-FILE.                                                  SQ1024.2
033500     ADD     1 TO REC-SKL-SUB.                                    SQ1024.2
033600     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1024.2
033700                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1024.2
033800*                                                                 SQ1024.2
033900 CLOSE-FILES.                                                     SQ1024.2
034000     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1024.2
034100     CLOSE   PRINT-FILE.                                          SQ1024.2
034200*    OPEN    I-O RAW-DATA.                                        SQ1024.2
034300*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1024.2
034400*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1024.2
034500*    MOVE   "OK.     " TO C-ABORT.                                SQ1024.2
034600*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1024.2
034700*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1024.2
034800*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1024.2
034900*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1024.2
035000*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1024.2
035100*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1024.2
035200*END-E-2.                                                         SQ1024.2
035300*    CLOSE   RAW-DATA.                                            SQ1024.2
035400 TERMINATE-CCVS.                                                  SQ1024.2
035500*    EXIT    PROGRAM.                                             SQ1024.2
035600     STOP    RUN.                                                 SQ1024.2
035700*                                                                 SQ1024.2
035800 INSPT.                                                           SQ1024.2
035900     MOVE   "INSPT" TO P-OR-F.                                    SQ1024.2
036000     ADD     1 TO INSPECT-COUNTER.                                SQ1024.2
036100     PERFORM PRINT-DETAIL.                                        SQ1024.2
036200*                                                                 SQ1024.2
036300 PASS.                                                            SQ1024.2
036400     MOVE   "PASS " TO P-OR-F.                                    SQ1024.2
036500     ADD     1 TO PASS-COUNTER.                                   SQ1024.2
036600     PERFORM PRINT-DETAIL.                                        SQ1024.2
036700*                                                                 SQ1024.2
036800 FAIL.                                                            SQ1024.2
036900     MOVE   "FAIL*" TO P-OR-F.                                    SQ1024.2
037000     ADD     1 TO ERROR-COUNTER.                                  SQ1024.2
037100     PERFORM PRINT-DETAIL.                                        SQ1024.2
037200*                                                                 SQ1024.2
037300 DE-LETE.                                                         SQ1024.2
037400     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1024.2
037500     MOVE   "*****" TO P-OR-F.                                    SQ1024.2
037600     ADD     1 TO DELETE-COUNTER.                                 SQ1024.2
037700     PERFORM PRINT-DETAIL.                                        SQ1024.2
037800*                                                                 SQ1024.2
037900 PRINT-DETAIL.                                                    SQ1024.2
038000     IF REC-CT NOT EQUAL TO ZERO                                  SQ1024.2
038100         MOVE   "." TO PARDOT-X                                   SQ1024.2
038200         MOVE    REC-CT TO DOTVALUE.                              SQ1024.2
038300     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1024.2
038400     PERFORM WRITE-LINE.                                          SQ1024.2
038500     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1024.2
038600         PERFORM WRITE-LINE                                       SQ1024.2
038700         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1024.2
038800     ELSE                                                         SQ1024.2
038900         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1024.2
039000     MOVE    SPACE TO P-OR-F.                                     SQ1024.2
039100     MOVE    SPACE TO COMPUTED-X.                                 SQ1024.2
039200     MOVE    SPACE TO CORRECT-X.                                  SQ1024.2
039300     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1024.2
039400     MOVE    SPACE TO RE-MARK.                                    SQ1024.2
039500*                                                                 SQ1024.2
039600 HEAD-ROUTINE.                                                    SQ1024.2
039700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1024.2
039800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1024.2
039900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1024.2
040000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1024.2
040100 COLUMN-NAMES-ROUTINE.                                            SQ1024.2
040200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1024.2
040300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1024.2
040400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1024.2
040500 END-ROUTINE.                                                     SQ1024.2
040600     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1024.2
040700     PERFORM WRITE-LINE 5 TIMES.                                  SQ1024.2
040800 END-RTN-EXIT.                                                    SQ1024.2
040900     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1024.2
041000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1024.2
041100*                                                                 SQ1024.2
041200 END-ROUTINE-1.                                                   SQ1024.2
041300     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1024.2
041400     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1024.2
041500     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1024.2
041600     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1024.2
041700     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1024.2
041800     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1024.2
041900     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1024.2
042000     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1024.2
042100     PERFORM WRITE-LINE.                                          SQ1024.2
042200     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1024.2
042300     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1024.2
042400         MOVE   "NO " TO ERROR-TOTAL                              SQ1024.2
042500     ELSE                                                         SQ1024.2
042600         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1024.2
042700     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1024.2
042800     PERFORM WRITE-LINE.                                          SQ1024.2
042900 END-ROUTINE-13.                                                  SQ1024.2
043000     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1024.2
043100         MOVE   "NO " TO ERROR-TOTAL                              SQ1024.2
043200     ELSE                                                         SQ1024.2
043300         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1024.2
043400     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1024.2
043500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1024.2
043600     PERFORM WRITE-LINE.                                          SQ1024.2
043700     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1024.2
043800         MOVE   "NO " TO ERROR-TOTAL                              SQ1024.2
043900     ELSE                                                         SQ1024.2
044000         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1024.2
044100     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1024.2
044200     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1024.2
044300     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1024.2
044400*                                                                 SQ1024.2
044500 WRITE-LINE.                                                      SQ1024.2
044600     ADD     1 TO RECORD-COUNT.                                   SQ1024.2
044700*    IF RECORD-COUNT GREATER 50                                   SQ1024.2
044800*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1024.2
044900*        MOVE  SPACE TO DUMMY-RECORD                              SQ1024.2
045000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1024.2
045100*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1024.2
045200*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1024.2
045300*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1024.2
045400*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1024.2
045500*        MOVE  ZERO TO RECORD-COUNT.                              SQ1024.2
045600     PERFORM WRT-LN.                                              SQ1024.2
045700*                                                                 SQ1024.2
045800 WRT-LN.                                                          SQ1024.2
045900     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1024.2
046000     MOVE    SPACE TO DUMMY-RECORD.                               SQ1024.2
046100 BLANK-LINE-PRINT.                                                SQ1024.2
046200     PERFORM WRT-LN.                                              SQ1024.2
046300 FAIL-ROUTINE.                                                    SQ1024.2
046400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1024.2
046500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1024.2
046600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1024.2
046700     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1024.2
046800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1024.2
046900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1024.2
047000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1024.2
047100     GO TO   FAIL-ROUTINE-EX.                                     SQ1024.2
047200 FAIL-ROUTINE-WRITE.                                              SQ1024.2
047300     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1024.2
047400     PERFORM WRITE-LINE                                           SQ1024.2
047500     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1024.2
047600     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1024.2
047700     PERFORM WRITE-LINE 2 TIMES.                                  SQ1024.2
047800     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1024.2
047900 FAIL-ROUTINE-EX.                                                 SQ1024.2
048000     EXIT.                                                        SQ1024.2
048100 BAIL-OUT.                                                        SQ1024.2
048200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1024.2
048300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1024.2
048400 BAIL-OUT-WRITE.                                                  SQ1024.2
048500     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1024.2
048600     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1024.2
048700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1024.2
048800     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1024.2
048900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1024.2
049000     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1024.2
049100 BAIL-OUT-EX.                                                     SQ1024.2
049200     EXIT.                                                        SQ1024.2
049300 CCVS1-EXIT.                                                      SQ1024.2
049400     EXIT.                                                        SQ1024.2
049500*                                                                 SQ1024.2
049600****************************************************************  SQ1024.2
049700*                                                              *  SQ1024.2
049800*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1024.2
049900*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1024.2
050000*                                                              *  SQ1024.2
050100****************************************************************  SQ1024.2
050200*                                                                 SQ1024.2
050300 SECT-SQ102-0001 SECTION.                                         SQ1024.2
050400 SEQ-INIT-WR-01.                                                  SQ1024.2
050500     MOVE    "SQ-FS1" TO XFILE-NAME (1).                          SQ1024.2
050600     MOVE    "R1-F-G" TO XRECORD-NAME (1).                        SQ1024.2
050700     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   SQ1024.2
050800     MOVE     000120 TO XRECORD-LENGTH (1).                       SQ1024.2
050900     MOVE    "RC"    TO CHARS-OR-RECORDS (1).                     SQ1024.2
051000     MOVE     0001   TO XBLOCK-SIZE (1).                          SQ1024.2
051100     MOVE     000750 TO RECORDS-IN-FILE (1).                      SQ1024.2
051200     MOVE    "SQ"    TO XFILE-ORGANIZATION (1).                   SQ1024.2
051300     MOVE    "S"     TO XLABEL-TYPE (1).                          SQ1024.2
051400     MOVE     ZERO   TO XRECORD-NUMBER (1).                       SQ1024.2
051500     MOVE    "CREATE 750 RECORD FILE" TO FEATURE.                 SQ1024.2
051600     MOVE    "SEQ-TEST-WR-01" TO PAR-NAME.                        SQ1024.2
051700*                                                                 SQ1024.2
051800 SEQ-TEST-WR-01.                                                  SQ1024.2
051900     OPEN OUTPUT SQ-FS1.                                          SQ1024.2
052000*                                                                 SQ1024.2
052100 SEQ-TEST-WR-01-LOOP.                                             SQ1024.2
052200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1024.2
052300     MOVE    FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.    SQ1024.2
052400     WRITE   SQ-FS1R1-F-G-120.                                    SQ1024.2
052500     IF XRECORD-NUMBER (1) LESS THAN 750                          SQ1024.2
052600         GO TO   SEQ-TEST-WR-01-LOOP.                             SQ1024.2
052700*                                                                 SQ1024.2
052800     CLOSE   SQ-FS1.                                              SQ1024.2
052900*                                                                 SQ1024.2
053000*    A SEQUENTIAL TAPE FILE HAS BEEN CREATED.  IT CONTAINS 750    SQ1024.2
053100*    RECORDS, EACH 120 CHARACTERS LONG.  THE FILE WILL NOW BE     SQ1024.2
053200*    READ AND THE RECORDS VERIFIED.                               SQ1024.2
053300*                                                                 SQ1024.2
053400 SEQ-INIT-GF-02.                                                  SQ1024.2
053500     MOVE   "SEQ-TEST-GF-02" TO PAR-NAME.                         SQ1024.2
053600     MOVE   "VERIFY NEW FILE" TO FEATURE.                         SQ1024.2
053700     MOVE    1      TO REC-CT.                                    SQ1024.2
053800     GO TO  SEQ-TEST-GF-02-01.                                    SQ1024.2
053900 SEQ-DELETE-02-01.                                                SQ1024.2
054000     GO TO   SEQ-DELETE-02-02.                                    SQ1024.2
054100 SEQ-TEST-GF-02-01.                                               SQ1024.2
054200     OPEN INPUT SQ-FS1.                                           SQ1024.2
054300*                                                                 SQ1024.2
054400 SEQ-INIT-GF-02-02.                                               SQ1024.2
054500     MOVE    FILE-RECORD-INFO-P1-120 (1)                          SQ1024.2
054600                TO FILE-RECORD-INFO-P1-120 (2).                   SQ1024.2
054700     MOVE    ZERO   TO XRECORD-NUMBER (2).                        SQ1024.2
054800     GO TO   SEQ-TEST-GF-02-02.                                   SQ1024.2
054900 SEQ-DELETE-02-02.                                                SQ1024.2
055000     PERFORM DE-LETE.                                             SQ1024.2
055100     ADD     1 TO REC-CT.                                         SQ1024.2
055200     PERFORM DE-LETE.                                             SQ1024.2
055300     GO TO   SEQ-DELETE-GF-02-05.                                 SQ1024.2
055400 SEQ-TEST-GF-02-02.                                               SQ1024.2
055500 SEQ-TEST-GF-02-02-LOOP.                                          SQ1024.2
055600     READ SQ-FS1                                                  SQ1024.2
055700          AT END GO TO SEQ-TEST-GF-02-02-1.                       SQ1024.2
055800     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1024.2
055900     ADD    1 TO XRECORD-NUMBER (2).                              SQ1024.2
056000     IF XRECORD-NUMBER (2) GREATER THAN 750                       SQ1024.2
056100         GO TO   SEQ-TEST-GF-02-02-1.                             SQ1024.2
056200     IF FILE-RECORD-INFO-P1-120 (1)                               SQ1024.2
056300                NOT EQUAL TO FILE-RECORD-INFO-P1-120 (2)          SQ1024.2
056400         ADD     1 TO RECORDS-IN-ERROR.                           SQ1024.2
056500     GO TO   SEQ-TEST-GF-02-02-LOOP.                              SQ1024.2
056600*                                                                 SQ1024.2
056700 SEQ-TEST-GF-02-02-1.                                             SQ1024.2
056800     IF XRECORD-NUMBER (2) = 750                                  SQ1024.2
056900         PERFORM PASS                                             SQ1024.2
057000     ELSE                                                         SQ1024.2
057100         MOVE   "RECORD COUNTING ERROR" TO RE-MARK                SQ1024.2
057200         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1024.2
057300         MOVE    750 TO CORRECT-18V0                              SQ1024.2
057400         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
057500         PERFORM FAIL.                                            SQ1024.2
057600*                                                                 SQ1024.2
057700     ADD     1 TO REC-CT.                                         SQ1024.2
057800     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1024.2
057900         PERFORM PASS                                             SQ1024.2
058000     ELSE                                                         SQ1024.2
058100         MOVE   "RECORD CONTENT ERRORS" TO RE-MARK                SQ1024.2
058200         MOVE    ZERO TO CORRECT-18V0                             SQ1024.2
058300         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1024.2
058400         MOVE   "VII-44; 4.4.2" TO ANSI-REFERENCE                 SQ1024.2
058500         PERFORM FAIL.                                            SQ1024.2
058600*                                                                 SQ1024.2
058700 SEQ-INIT-GF-02-05.                                               SQ1024.2
058800     GO TO   SEQ-TEST-GF-02-05.                                   SQ1024.2
058900 SEQ-DELETE-GF-02-05.                                             SQ1024.2
059000     GO TO   SEQ-TEST-GF-02-END.                                  SQ1024.2
059100 SEQ-TEST-GF-02-05.                                               SQ1024.2
059200     CLOSE   SQ-FS1.                                              SQ1024.2
059300 SEQ-TEST-GF-02-END.                                              SQ1024.2
059400*                                                                 SQ1024.2
059500*                                                                 SQ1024.2
059600 SEQ-INIT-GF-03.                                                  SQ1024.2
059700     GO TO  SEQ-TEST-GF-03.                                       SQ1024.2
059800 SEQ-DELETE-03.                                                   SQ1024.2
059900     GO TO   SEQ-TEST-03-END.                                     SQ1024.2
060000 SEQ-TEST-GF-03.                                                  SQ1024.2
060100     OPEN    INPUT SQ-FS1.                                        SQ1024.2
060200 SEQ-TEST-03-END.                                                 SQ1024.2
060300*                                                                 SQ1024.2
060400*                                                                 SQ1024.2
060500*    THIS SERIES OF TESTS CHECKS FOUR LEVEL 1 VARIANTS OF         SQ1024.2
060600*    THE READ STATEMENT                                           SQ1024.2
060700*                                                                 SQ1024.2
060800 SEQ-INIT-GF-04.                                                  SQ1024.2
060900     MOVE    ZERO TO XRECORD-NUMBER (2).                          SQ1024.2
061000     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1024.2
061100     MOVE   "READ...RECORD AT END"     TO FEATURE.                SQ1024.2
061200     MOVE   "SEQ-TEST-GF-O4" TO PAR-NAME.                         SQ1024.2
061300     MOVE    ZERO TO ERROR-FLAG.                                  SQ1024.2
061400     MOVE    1 TO REC-CT.                                         SQ1024.2
061500     GO TO  SEQ-TEST-GF-04.                                       SQ1024.2
061600 SEQ-DELETE-04.                                                   SQ1024.2
061700     PERFORM DE-LETE.                                             SQ1024.2
061800     ADD     1 TO REC-CT.                                         SQ1024.2
061900     PERFORM DE-LETE.                                             SQ1024.2
062000     GO TO   SEQ-TEST-04-END.                                     SQ1024.2
062100 SEQ-TEST-GF-04.                                                  SQ1024.2
062200     READ SQ-FS1 RECORD AT END                                    SQ1024.2
062300         MOVE    1 TO EOF-FLAG                                    SQ1024.2
062400         GO TO   SEQ-TEST-GF-04-01.                               SQ1024.2
062500     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1024.2
062600     ADD    1 TO XRECORD-NUMBER (2)                               SQ1024.2
062700     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1024.2
062800         ADD     1 TO RECORDS-IN-ERROR                            SQ1024.2
062900         MOVE    1 TO ERROR-FLAG.                                 SQ1024.2
063000     IF XRECORD-NUMBER (2) LESS THAN 200                          SQ1024.2
063100         GO TO   SEQ-TEST-GF-04.                                  SQ1024.2
063200*                                                                 SQ1024.2
063300 SEQ-TEST-GF-04-01.                                               SQ1024.2
063400     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1024.2
063500         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1024.2
063600         MOVE    750 TO CORRECT-18V0                              SQ1024.2
063700         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1024.2
063800         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
063900         PERFORM FAIL                                             SQ1024.2
064000     ELSE                                                         SQ1024.2
064100         PERFORM PASS.                                            SQ1024.2
064200*                                                                 SQ1024.2
064300 SEQ-TEST-GF-04-02.                                               SQ1024.2
064400     ADD     1 TO REC-CT.                                         SQ1024.2
064500     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1024.2
064600         PERFORM PASS                                             SQ1024.2
064700     ELSE                                                         SQ1024.2
064800         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1024.2
064900         MOVE    ZERO TO CORRECT-18V0                             SQ1024.2
065000         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1024.2
065100         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
065200         PERFORM FAIL.                                            SQ1024.2
065300 SEQ-TEST-04-END.                                                 SQ1024.2
065400*                                                                 SQ1024.2
065500*                                                                 SQ1024.2
065600 SEQ-INIT-GF-O5.                                                  SQ1024.2
065700     MOVE    1 TO REC-CT.                                         SQ1024.2
065800     IF EOF-FLAG EQUAL TO 1                                       SQ1024.2
065900         GO TO SEQ-DELETE-05.                                     SQ1024.2
066000     MOVE    ZERO TO ERROR-FLAG.                                  SQ1024.2
066100     MOVE   "READ...AT END..." TO FEATURE                         SQ1024.2
066200     MOVE   "SEQ-TEST-GF-O5" TO PAR-NAME.                         SQ1024.2
066300     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1024.2
066400     GO TO  SEQ-TEST-GF-05.                                       SQ1024.2
066500 SEQ-DELETE-05.                                                   SQ1024.2
066600     PERFORM DE-LETE.                                             SQ1024.2
066700     ADD     1 TO REC-CT.                                         SQ1024.2
066800     PERFORM DE-LETE.                                             SQ1024.2
066900     GO TO   SEQ-TEST-05-END.                                     SQ1024.2
067000 SEQ-TEST-GF-05.                                                  SQ1024.2
067100     READ SQ-FS1 AT END                                           SQ1024.2
067200         MOVE    1 TO EOF-FLAG                                    SQ1024.2
067300         GO TO   SEQ-TEST-GF-05-01.                               SQ1024.2
067400     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1024.2
067500     ADD    1 TO XRECORD-NUMBER (2)                               SQ1024.2
067600     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1024.2
067700         ADD     1 TO RECORDS-IN-ERROR                            SQ1024.2
067800         MOVE    1 TO ERROR-FLAG.                                 SQ1024.2
067900     IF XRECORD-NUMBER (2) LESS THAN 400                          SQ1024.2
068000         GO TO   SEQ-TEST-GF-05.                                  SQ1024.2
068100*                                                                 SQ1024.2
068200 SEQ-TEST-GF-05-01.                                               SQ1024.2
068300     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1024.2
068400         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1024.2
068500         MOVE    750 TO CORRECT-18V0                              SQ1024.2
068600         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1024.2
068700         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
068800         PERFORM FAIL                                             SQ1024.2
068900     ELSE                                                         SQ1024.2
069000         PERFORM PASS.                                            SQ1024.2
069100*                                                                 SQ1024.2
069200 SEQ-TEST-GF-05-02.                                               SQ1024.2
069300     ADD     1 TO REC-CT.                                         SQ1024.2
069400     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1024.2
069500         PERFORM PASS                                             SQ1024.2
069600     ELSE                                                         SQ1024.2
069700         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1024.2
069800         MOVE    ZERO TO CORRECT-18V0                             SQ1024.2
069900         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1024.2
070000         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
070100         PERFORM FAIL.                                            SQ1024.2
070200 SEQ-TEST-05-END.                                                 SQ1024.2
070300*                                                                 SQ1024.2
070400*                                                                 SQ1024.2
070500 SEQ-INIT-GF-O6.                                                  SQ1024.2
070600     MOVE    1 TO REC-CT.                                         SQ1024.2
070700     IF EOF-FLAG EQUAL TO 1                                       SQ1024.2
070800         GO TO SEQ-DELETE-06.                                     SQ1024.2
070900     MOVE    ZERO TO ERROR-FLAG.                                  SQ1024.2
071000     MOVE   "READ...RECORD END..." TO FEATURE                     SQ1024.2
071100     MOVE   "SEQ-TEST-GF-O6" TO PAR-NAME.                         SQ1024.2
071200     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1024.2
071300     GO TO  SEQ-TEST-GF-06.                                       SQ1024.2
071400 SEQ-DELETE-06.                                                   SQ1024.2
071500     PERFORM DE-LETE.                                             SQ1024.2
071600     ADD     1 TO REC-CT.                                         SQ1024.2
071700     PERFORM DE-LETE.                                             SQ1024.2
071800     GO TO   SEQ-TEST-06-END.                                     SQ1024.2
071900 SEQ-TEST-GF-06.                                                  SQ1024.2
072000     READ SQ-FS1 RECORD END                                       SQ1024.2
072100         MOVE    1 TO EOF-FLAG                                    SQ1024.2
072200         GO TO   SEQ-TEST-GF-06-01.                               SQ1024.2
072300     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1024.2
072400     ADD    1 TO XRECORD-NUMBER (2)                               SQ1024.2
072500     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1024.2
072600         ADD     1 TO RECORDS-IN-ERROR                            SQ1024.2
072700         MOVE    1 TO ERROR-FLAG.                                 SQ1024.2
072800     IF XRECORD-NUMBER (2) LESS THAN 600                          SQ1024.2
072900         GO TO   SEQ-TEST-GF-06.                                  SQ1024.2
073000*                                                                 SQ1024.2
073100 SEQ-TEST-GF-06-01.                                               SQ1024.2
073200     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1024.2
073300         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1024.2
073400         MOVE    750 TO CORRECT-18V0                              SQ1024.2
073500         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1024.2
073600         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
073700         PERFORM FAIL                                             SQ1024.2
073800     ELSE                                                         SQ1024.2
073900         PERFORM PASS.                                            SQ1024.2
074000*                                                                 SQ1024.2
074100 SEQ-TEST-GF-06-02.                                               SQ1024.2
074200     ADD     1 TO REC-CT.                                         SQ1024.2
074300     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1024.2
074400         PERFORM PASS                                             SQ1024.2
074500     ELSE                                                         SQ1024.2
074600         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1024.2
074700         MOVE    ZERO TO CORRECT-18V0                             SQ1024.2
074800         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1024.2
074900         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
075000         PERFORM FAIL.                                            SQ1024.2
075100 SEQ-TEST-06-END.                                                 SQ1024.2
075200*                                                                 SQ1024.2
075300*                                                                 SQ1024.2
075400 SEQ-INIT-GF-O7.                                                  SQ1024.2
075500     MOVE    1 TO REC-CT.                                         SQ1024.2
075600     IF EOF-FLAG EQUAL TO 1                                       SQ1024.2
075700         GO TO SEQ-DELETE-07.                                     SQ1024.2
075800     MOVE    ZERO TO ERROR-FLAG.                                  SQ1024.2
075900     MOVE   "READ... END..." TO FEATURE                           SQ1024.2
076000     MOVE   "SEQ-TEST-GF-O7" TO PAR-NAME.                         SQ1024.2
076100     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1024.2
076200     GO TO  SEQ-TEST-GF-07.                                       SQ1024.2
076300 SEQ-DELETE-07.                                                   SQ1024.2
076400     PERFORM DE-LETE.                                             SQ1024.2
076500     ADD     1 TO REC-CT.                                         SQ1024.2
076600     PERFORM DE-LETE.                                             SQ1024.2
076700     GO TO   SEQ-TEST-07-END.                                     SQ1024.2
076800 SEQ-TEST-GF-07.                                                  SQ1024.2
076900     READ SQ-FS1 END                                              SQ1024.2
077000         MOVE    1 TO EOF-FLAG                                    SQ1024.2
077100         GO TO   SEQ-TEST-GF-07-01.                               SQ1024.2
077200     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1024.2
077300     ADD    1 TO XRECORD-NUMBER (2)                               SQ1024.2
077400     IF XRECORD-NUMBER (2) NOT EQUAL TO XRECORD-NUMBER (1)        SQ1024.2
077500         ADD     1 TO RECORDS-IN-ERROR                            SQ1024.2
077600         MOVE    1 TO ERROR-FLAG.                                 SQ1024.2
077700     IF XRECORD-NUMBER (2) LESS THAN 750                          SQ1024.2
077800         GO TO   SEQ-TEST-GF-07.                                  SQ1024.2
077900*                                                                 SQ1024.2
078000 SEQ-TEST-GF-07-01.                                               SQ1024.2
078100     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1024.2
078200         MOVE   "PREMATURE END OF FILE" TO RE-MARK                SQ1024.2
078300         MOVE    750 TO CORRECT-18V0                              SQ1024.2
078400         MOVE    XRECORD-NUMBER (2) TO COMPUTED-18V0              SQ1024.2
078500         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
078600         PERFORM FAIL                                             SQ1024.2
078700     ELSE                                                         SQ1024.2
078800         PERFORM PASS.                                            SQ1024.2
078900*                                                                 SQ1024.2
079000 SEQ-TEST-GF-07-02.                                               SQ1024.2
079100     ADD     1 TO REC-CT.                                         SQ1024.2
079200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1024.2
079300         PERFORM PASS                                             SQ1024.2
079400     ELSE                                                         SQ1024.2
079500         MOVE    ERROR-FLAG TO COMPUTED-18V0                      SQ1024.2
079600         MOVE    ZERO TO CORRECT-18V0                             SQ1024.2
079700         MOVE   "ERROR IN ONE OR MORE RECORDS" TO RE-MARK         SQ1024.2
079800         MOVE   "VII-44" TO ANSI-REFERENCE                        SQ1024.2
079900         PERFORM FAIL.                                            SQ1024.2
080000 SEQ-TEST-07-END.                                                 SQ1024.2
080100*                                                                 SQ1024.2
080200*                                                                 SQ1024.2
080300 SEQ-INIT-GF-O8.                                                  SQ1024.2
080400     MOVE    1 TO REC-CT.                                         SQ1024.2
080500     IF EOF-FLAG EQUAL TO 1                                       SQ1024.2
080600         GO TO SEQ-DELETE-08.                                     SQ1024.2
080700     MOVE   "READ... END... AT EOF" TO FEATURE                    SQ1024.2
080800     MOVE   "SEQ-TEST-GF-O8" TO PAR-NAME.                         SQ1024.2
080900     GO TO  SEQ-TEST-GF-08.                                       SQ1024.2
081000 SEQ-DELETE-08.                                                   SQ1024.2
081100     PERFORM DE-LETE.                                             SQ1024.2
081200     GO TO   SEQ-TEST-08-END.                                     SQ1024.2
081300 SEQ-TEST-GF-08.                                                  SQ1024.2
081400     READ SQ-FS1 END                                              SQ1024.2
081500         MOVE    1 TO EOF-FLAG.                                   SQ1024.2
081600*                                                                 SQ1024.2
081700 SEQ-TEST-GF-08-01.                                               SQ1024.2
081800     IF EOF-FLAG NOT EQUAL TO 1                                   SQ1024.2
081900         MOVE    EOF-FLAG TO COMPUTED-18V0                        SQ1024.2
082000         MOVE    1 TO CORRECT-18V0                                SQ1024.2
082100         MOVE   "EOF NOT FOUND AFTER 750 RECORDS" TO RE-MARK      SQ1024.2
082200         PERFORM FAIL                                             SQ1024.2
082300     ELSE                                                         SQ1024.2
082400         PERFORM PASS.                                            SQ1024.2
082500 SEQ-TEST-08-END.                                                 SQ1024.2
082600*                                                                 SQ1024.2
082700*                                                                 SQ1024.2
082800 SEQ-INIT-GF-O9.                                                  SQ1024.2
082900     GO TO  SEQ-TEST-GF-09.                                       SQ1024.2
083000 SEQ-DELETE-09.                                                   SQ1024.2
083100     GO TO   SEQ-TEST-09-END.                                     SQ1024.2
083200 SEQ-TEST-GF-09.                                                  SQ1024.2
083300     CLOSE SQ-FS1.                                                SQ1024.2
083400 SEQ-TEST-09-END.                                                 SQ1024.2
083500*                                                                 SQ1024.2
083600*                                                                 SQ1024.2
083700 TERMINATE-ROUTINE.                                               SQ1024.2
083800     EXIT.                                                        SQ1024.2
083900 CCVS-EXIT SECTION.                                               SQ1024.2
084000 CCVS-999999.                                                     SQ1024.2
084100     GO TO CLOSE-FILES.                                           SQ1024.2
      *END-OF,SQ102A                                                                  
