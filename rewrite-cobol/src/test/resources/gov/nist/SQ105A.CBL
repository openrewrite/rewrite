      *HEADER,COBOL,SQ105A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1054.2
000200 PROGRAM-ID.                                                      SQ1054.2
000300     SQ105A.                                                      SQ1054.2
000400****************************************************************  SQ1054.2
000500*                                                              *  SQ1054.2
000600*    VALIDATION FOR:-                                          *  SQ1054.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1054.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1054.2
000900*                             REVISED 1986, AUGUST             *  SQ1054.2
001000*                                                              *  SQ1054.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1054.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1054.2
001300*                                                              *  SQ1054.2
001400****************************************************************  SQ1054.2
001500*                                                              *  SQ1054.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1054.2
001700*                                                              *  SQ1054.2
001800*            X-14   SEQUENTIAL MASS STORAGE FILE               *  SQ1054.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1054.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1054.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1054.2
002200*                                                              *  SQ1054.2
002300****************************************************************  SQ1054.2
002400*                                                              *  SQ1054.2
002500*    SQ105A CREATES A SEQUENTIAL MASS STORAGE FILE CONTAINING  *  SQ1054.2
002600*    980 RECORDS, EACH 125 CHARACTERS LONG.  THERE ARE TWO     *  SQ1054.2
002700*    USE PROCEDURES IN THE DECLARATIVE SECTION, ONE FOR        *  SQ1054.2
002800*    EXCEPTION ON OUTPUT, THE OTHER FOR EXCEPTION ON INPUT.    *  SQ1054.2
002900*    THE FILE IS READ TWICE.  IN THE FIRST PASS, RECORDS ARE   *  SQ1054.2
003000*    READ WITH READ STATEMENTS WHICH DO NOT CONTAIN ANY        *  SQ1054.2
003100*    OPTIONAL PHRASES EXCEPT THE OPTIONAL WORD "RECORD".  ON   *  SQ1054.2
003200*    THE SECOND PASS, THE READ STATEMENT CONTAINS NO OPTIONAL  *  SQ1054.2
003300*    WORDS OR PHRASES AT ALL.  ON BOTH PASSES, THE END OF FILE *  SQ1054.2
003400*    SHOULD CAUSE EXECUTION OF THE DECLARATIVE PROCEDURE FOR   *  SQ1054.2
003500*    INPUT.  THE DECLARATIVE PROCEDURE FOR OUTPUT SHOULD BE    *  SQ1054.2
003600*    ON BOTH PASSES.                                           *  SQ1054.2
003700*                                                              *  SQ1054.2
003800*    THE OPTIONAL ORGANIZATION AND ACCESS MODE CLAUSES ARE     *  SQ1054.2
003900*    BOTH OMITTED.                                             *  SQ1054.2
004000*                                                              *  SQ1054.2
004100****************************************************************  SQ1054.2
004200*                                                                 SQ1054.2
004300*                                                                 SQ1054.2
004400 ENVIRONMENT DIVISION.                                            SQ1054.2
004500 CONFIGURATION SECTION.                                           SQ1054.2
004600 SOURCE-COMPUTER.                                                 SQ1054.2
004700     XXXXX082.                                                    SQ1054.2
004800 OBJECT-COMPUTER.                                                 SQ1054.2
004900     XXXXX083.                                                    SQ1054.2
005000*                                                                 SQ1054.2
005100 INPUT-OUTPUT SECTION.                                            SQ1054.2
005200 FILE-CONTROL.                                                    SQ1054.2
005300     SELECT PRINT-FILE ASSIGN TO                                  SQ1054.2
005400     XXXXX055.                                                    SQ1054.2
005500*                                                                 SQ1054.2
005600*    SELECT RAW-DATA   ASSIGN TO                                  SQ1054.2
005700*    XXXXX062                                                     SQ1054.2
005800*          ORGANIZATION IS INDEXED                                SQ1054.2
005900*          ACCESS MODE  IS RANDOM                                 SQ1054.2
006000*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1054.2
006100*                                                                 SQ1054.2
006200     SELECT SQ-FS4 ASSIGN                                         SQ1054.2
006300     XXXXX014                                                     SQ1054.2
006400     STATUS SQ-FS4-STATUS.                                        SQ1054.2
006500*                                                                 SQ1054.2
006600*                                                                 SQ1054.2
006700 DATA DIVISION.                                                   SQ1054.2
006800 FILE SECTION.                                                    SQ1054.2
006900 FD  PRINT-FILE                                                   SQ1054.2
007000C    LABEL RECORDS                                                SQ1054.2
007100C    XXXXX084                                                     SQ1054.2
007200C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1054.2
007300               .                                                  SQ1054.2
007400 01  PRINT-REC    PICTURE X(120).                                 SQ1054.2
007500 01  DUMMY-RECORD PICTURE X(120).                                 SQ1054.2
007600*                                                                 SQ1054.2
007700*FD  RAW-DATA.                                                    SQ1054.2
007800*01  RAW-DATA-SATZ.                                               SQ1054.2
007900*    05  RAW-DATA-KEY        PIC X(6).                            SQ1054.2
008000*    05  C-DATE              PIC 9(6).                            SQ1054.2
008100*    05  C-TIME              PIC 9(8).                            SQ1054.2
008200*    05  NO-OF-TESTS         PIC 99.                              SQ1054.2
008300*    05  C-OK                PIC 999.                             SQ1054.2
008400*    05  C-ALL               PIC 999.                             SQ1054.2
008500*    05  C-FAIL              PIC 999.                             SQ1054.2
008600*    05  C-DELETED           PIC 999.                             SQ1054.2
008700*    05  C-INSPECT           PIC 999.                             SQ1054.2
008800*    05  C-NOTE              PIC X(13).                           SQ1054.2
008900*    05  C-INDENT            PIC X.                               SQ1054.2
009000*    05  C-ABORT             PIC X(8).                            SQ1054.2
009100*                                                                 SQ1054.2
009200 FD  SQ-FS4                                                       SQ1054.2
009300     BLOCK CONTAINS 2 RECORDS                                     SQ1054.2
009400     RECORD 125                                                   SQ1054.2
009500C    LABEL RECORD STANDARD                                        SQ1054.2
009600     DATA RECORDS SQ-FS4R1-F-G-125.                               SQ1054.2
009700*                                                                 SQ1054.2
009800 01  SQ-FS4R1-F-G-125.                                            SQ1054.2
009900     02  SQ-FS4-FIRST  PIC X(120).                                SQ1054.2
010000     02  SQ-FS4-REC-NO   PIC  9(5).                               SQ1054.2
010100*                                                                 SQ1054.2
010200 WORKING-STORAGE SECTION.                                         SQ1054.2
010300*                                                                 SQ1054.2
010400***************************************************************   SQ1054.2
010500*                                                             *   SQ1054.2
010600*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1054.2
010700*                                                             *   SQ1054.2
010800***************************************************************   SQ1054.2
010900*                                                                 SQ1054.2
011000 01  SQ-FS4-STATUS.                                               SQ1054.2
011100   03  SQ-FS4-STATUS-1   PIC X.                                   SQ1054.2
011200   03  SQ-FS4-STATUS-2   PIC X.                                   SQ1054.2
011300*                                                                 SQ1054.2
011400 01  SQ-FS4-STATUS-COPY PIC XX.                                   SQ1054.2
011500 01  DECL-EXEC-SW.                                                SQ1054.2
011600     05  DECL-EXEC-SW-O PIC X.                                    SQ1054.2
011700     05  DECL-EXEC-SW-I PIC X.                                    SQ1054.2
011800 01  WRK-CS-09V00  PIC  S9(9) USAGE COMPUTATIONAL VALUE ZERO.     SQ1054.2
011900 01  EOF-FLAG  PIC 9  VALUE ZERO.                                 SQ1054.2
012000 01  RECORDS-IN-ERROR  PIC S9(5) USAGE COMP VALUE ZERO.           SQ1054.2
012100 01  ERROR-FLAG PIC 9  VALUE ZERO.                                SQ1054.2
012200 01  PERM-ERRORS  PIC S9(5) USAGE COMP VALUE ZERO.                SQ1054.2
012300*                                                                 SQ1054.2
012400 01  MAJOR-DELETIONS PIC 99.                                      SQ1054.2
012500 01  COUNT-OF-RECS   PIC 9(5).                                    SQ1054.2
012600*                                                                 SQ1054.2
012700*                                                                 SQ1054.2
012800***************************************************************   SQ1054.2
012900*                                                             *   SQ1054.2
013000*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1054.2
013100*                                                             *   SQ1054.2
013200***************************************************************   SQ1054.2
013300*                                                                 SQ1054.2
013400 01  REC-SKEL-SUB   PIC 99.                                       SQ1054.2
013500*                                                                 SQ1054.2
013600 01  FILE-RECORD-INFORMATION-REC.                                 SQ1054.2
013700     03 FILE-RECORD-INFO-SKELETON.                                SQ1054.2
013800        05 FILLER                 PICTURE X(48)       VALUE       SQ1054.2
013900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1054.2
014000        05 FILLER                 PICTURE X(46)       VALUE       SQ1054.2
014100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1054.2
014200        05 FILLER                 PICTURE X(26)       VALUE       SQ1054.2
014300             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1054.2
014400        05 FILLER                 PICTURE X(37)       VALUE       SQ1054.2
014500             ",RECKEY=                             ".             SQ1054.2
014600        05 FILLER                 PICTURE X(38)       VALUE       SQ1054.2
014700             ",ALTKEY1=                             ".            SQ1054.2
014800        05 FILLER                 PICTURE X(38)       VALUE       SQ1054.2
014900             ",ALTKEY2=                             ".            SQ1054.2
015000        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1054.2
015100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1054.2
015200        05 FILE-RECORD-INFO-P1-120.                               SQ1054.2
015300           07 FILLER              PIC X(5).                       SQ1054.2
015400           07 XFILE-NAME          PIC X(6).                       SQ1054.2
015500           07 FILLER              PIC X(8).                       SQ1054.2
015600           07 XRECORD-NAME        PIC X(6).                       SQ1054.2
015700           07 FILLER              PIC X(1).                       SQ1054.2
015800           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1054.2
015900           07 FILLER              PIC X(7).                       SQ1054.2
016000           07 XRECORD-NUMBER      PIC 9(6).                       SQ1054.2
016100           07 FILLER              PIC X(6).                       SQ1054.2
016200           07 UPDATE-NUMBER       PIC 9(2).                       SQ1054.2
016300           07 FILLER              PIC X(5).                       SQ1054.2
016400           07 ODO-NUMBER          PIC 9(4).                       SQ1054.2
016500           07 FILLER              PIC X(5).                       SQ1054.2
016600           07 XPROGRAM-NAME       PIC X(5).                       SQ1054.2
016700           07 FILLER              PIC X(7).                       SQ1054.2
016800           07 XRECORD-LENGTH      PIC 9(6).                       SQ1054.2
016900           07 FILLER              PIC X(7).                       SQ1054.2
017000           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1054.2
017100           07 FILLER              PIC X(1).                       SQ1054.2
017200           07 XBLOCK-SIZE         PIC 9(4).                       SQ1054.2
017300           07 FILLER              PIC X(6).                       SQ1054.2
017400           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1054.2
017500           07 FILLER              PIC X(5).                       SQ1054.2
017600           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1054.2
017700           07 FILLER              PIC X(6).                       SQ1054.2
017800           07 XLABEL-TYPE         PIC X(1).                       SQ1054.2
017900        05 FILE-RECORD-INFO-P121-240.                             SQ1054.2
018000           07 FILLER              PIC X(8).                       SQ1054.2
018100           07 XRECORD-KEY         PIC X(29).                      SQ1054.2
018200           07 FILLER              PIC X(9).                       SQ1054.2
018300           07 ALTERNATE-KEY1      PIC X(29).                      SQ1054.2
018400           07 FILLER              PIC X(9).                       SQ1054.2
018500           07 ALTERNATE-KEY2      PIC X(29).                      SQ1054.2
018600           07 FILLER              PIC X(7).                       SQ1054.2
018700*                                                                 SQ1054.2
018800 01  TEST-RESULTS.                                                SQ1054.2
018900     02 FILLER              PIC X      VALUE SPACE.               SQ1054.2
019000     02  PAR-NAME.                                                SQ1054.2
019100       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1054.2
019200       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1054.2
019300       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1054.2
019400     02 FILLER              PIC X      VALUE SPACE.               SQ1054.2
019500     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1054.2
019600     02 FILLER              PIC X      VALUE SPACE.               SQ1054.2
019700     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1054.2
019800     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1054.2
019900     02 RE-MARK             PIC X(61).                            SQ1054.2
020000 01  TEST-COMPUTED.                                               SQ1054.2
020100   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1054.2
020200   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1054.2
020300   02 COMPUTED-X.                                                 SQ1054.2
020400     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1054.2
020500     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1054.2
020600     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1054.2
020700     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1054.2
020800     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1054.2
020900     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1054.2
021000        04 COMPUTED-18V0                   PIC -9(18).            SQ1054.2
021100        04 FILLER                          PIC X.                 SQ1054.2
021200     03 FILLER PIC X(50) VALUE SPACE.                             SQ1054.2
021300 01  TEST-CORRECT.                                                SQ1054.2
021400     02 FILLER PIC X(30) VALUE SPACE.                             SQ1054.2
021500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1054.2
021600     02 CORRECT-X.                                                SQ1054.2
021700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1054.2
021800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1054.2
021900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1054.2
022000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1054.2
022100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1054.2
022200     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1054.2
022300         04 CORRECT-18V0                     PIC -9(18).          SQ1054.2
022400         04 FILLER                           PIC X.               SQ1054.2
022500     03 FILLER PIC X(2) VALUE SPACE.                              SQ1054.2
022600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1054.2
022700*                                                                 SQ1054.2
022800 01  CCVS-C-1.                                                    SQ1054.2
022900     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1054.2
023000     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1054.2
023100     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1054.2
023200     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1054.2
023300     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1054.2
023400     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1054.2
023500     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1054.2
023600     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1054.2
023700 01  CCVS-C-2.                                                    SQ1054.2
023800     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1054.2
023900     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1054.2
024000     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1054.2
024100     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1054.2
024200     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1054.2
024300*                                                                 SQ1054.2
024400 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1054.2
024500 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1054.2
024600 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1054.2
024700 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1054.2
024800 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1054.2
024900 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1054.2
025000 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1054.2
025100 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1054.2
025200 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1054.2
025300 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1054.2
025400 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1054.2
025500 01  CCVS-H-1.                                                    SQ1054.2
025600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1054.2
025700     02  FILLER          PIC X(42)    VALUE                       SQ1054.2
025800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1054.2
025900     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1054.2
026000 01  CCVS-H-2A.                                                   SQ1054.2
026100   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1054.2
026200   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1054.2
026300   02  FILLER            PIC XXXX   VALUE                         SQ1054.2
026400     "4.2 ".                                                      SQ1054.2
026500   02  FILLER            PIC X(28)  VALUE                         SQ1054.2
026600            " COPY - NOT FOR DISTRIBUTION".                       SQ1054.2
026700   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1054.2
026800*                                                                 SQ1054.2
026900 01  CCVS-H-2B.                                                   SQ1054.2
027000   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1054.2
027100   02  TEST-ID           PIC X(9).                                SQ1054.2
027200   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1054.2
027300   02  FILLER            PIC X(12)  VALUE                         SQ1054.2
027400     " HIGH       ".                                              SQ1054.2
027500   02  FILLER            PIC X(22)  VALUE                         SQ1054.2
027600            " LEVEL VALIDATION FOR ".                             SQ1054.2
027700   02  FILLER            PIC X(58)  VALUE                         SQ1054.2
027800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1054.2
027900 01  CCVS-H-3.                                                    SQ1054.2
028000     02  FILLER          PIC X(34)  VALUE                         SQ1054.2
028100            " FOR OFFICIAL USE ONLY    ".                         SQ1054.2
028200     02  FILLER          PIC X(58)  VALUE                         SQ1054.2
028300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1054.2
028400     02  FILLER          PIC X(28)  VALUE                         SQ1054.2
028500            "  COPYRIGHT   1985,1986 ".                           SQ1054.2
028600 01  CCVS-E-1.                                                    SQ1054.2
028700     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1054.2
028800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1054.2
028900     02 ID-AGAIN         PIC X(9).                                SQ1054.2
029000     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1054.2
029100 01  CCVS-E-2.                                                    SQ1054.2
029200     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1054.2
029300     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1054.2
029400     02  CCVS-E-2-2.                                              SQ1054.2
029500         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1054.2
029600         03 FILLER         PIC X      VALUE SPACE.                SQ1054.2
029700         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1054.2
029800            "ERRORS ENCOUNTERED".                                 SQ1054.2
029900 01  CCVS-E-3.                                                    SQ1054.2
030000     02  FILLER          PIC X(22)  VALUE                         SQ1054.2
030100            " FOR OFFICIAL USE ONLY".                             SQ1054.2
030200     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1054.2
030300     02  FILLER          PIC X(58)  VALUE                         SQ1054.2
030400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1054.2
030500     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1054.2
030600     02  FILLER          PIC X(20)  VALUE                         SQ1054.2
030700             " COPYRIGHT 1985,1986".                              SQ1054.2
030800 01  CCVS-E-4.                                                    SQ1054.2
030900     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1054.2
031000     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1054.2
031100     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1054.2
031200     02 FILLER           PIC X(40)  VALUE                         SQ1054.2
031300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1054.2
031400 01  XXINFO.                                                      SQ1054.2
031500     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1054.2
031600     02 INFO-TEXT.                                                SQ1054.2
031700       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1054.2
031800       04 XXCOMPUTED         PIC X(20).                           SQ1054.2
031900       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1054.2
032000       04 XXCORRECT          PIC X(20).                           SQ1054.2
032100     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1054.2
032200 01  HYPHEN-LINE.                                                 SQ1054.2
032300     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1054.2
032400     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1054.2
032500-    "*****************************************".                 SQ1054.2
032600     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1054.2
032700-    "******************************".                            SQ1054.2
032800 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1054.2
032900     "SQ105A".                                                    SQ1054.2
033000*                                                                 SQ1054.2
033100*                                                                 SQ1054.2
033200 PROCEDURE DIVISION.                                              SQ1054.2
033300 DECLARATIVES.                                                    SQ1054.2
033400 SECT-SQ105-0001 SECTION.                                         SQ1054.2
033500     USE AFTER STANDARD ERROR PROCEDURE OUTPUT.                   SQ1054.2
033600 OUTPUT-ERROR-PROCESS.                                            SQ1054.2
033700     MOVE   "O" TO DECL-EXEC-SW-O.                                SQ1054.2
033800     MOVE    2     TO PERM-ERRORS.                                SQ1054.2
033900     ADD     1     TO RECORDS-IN-ERROR.                           SQ1054.2
034000     IF SQ-FS4-STATUS-1 EQUAL TO "3"                              SQ1054.2
034100          MOVE    1 TO PERM-ERRORS.                               SQ1054.2
034200 SECT-SQ105-0002 SECTION.                                         SQ1054.2
034300     USE AFTER ERROR PROCEDURE ON INPUT.                          SQ1054.2
034400 INPUT-PROCESS.                                                   SQ1054.2
034500     MOVE   "I" TO DECL-EXEC-SW-I.                                SQ1054.2
034600     IF SQ-FS4-STATUS-1 EQUAL TO "1"                              SQ1054.2
034700          MOVE    1 TO EOF-FLAG.                                  SQ1054.2
034800     IF SQ-FS4-STATUS-1 GREATER THAN "1"                          SQ1054.2
034900          MOVE    1 TO PERM-ERRORS.                               SQ1054.2
035000*                                                                 SQ1054.2
035100 END DECLARATIVES.                                                SQ1054.2
035200*                                                                 SQ1054.2
035300*                                                                 SQ1054.2
035400 CCVS1 SECTION.                                                   SQ1054.2
035500 OPEN-FILES.                                                      SQ1054.2
035600*    OPEN    I-O RAW-DATA.                                        SQ1054.2
035700*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1054.2
035800*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1054.2
035900*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1054.2
036000*    ADD     1           TO C-NO-OF-TESTS.                        SQ1054.2
036100*    ACCEPT  C-DATE      FROM DATE.                               SQ1054.2
036200*    ACCEPT  C-TIME      FROM TIME.                               SQ1054.2
036300*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1054.2
036400*END-E-1.                                                         SQ1054.2
036500*    CLOSE   RAW-DATA.                                            SQ1054.2
036600     OPEN    OUTPUT PRINT-FILE.                                   SQ1054.2
036700     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1054.2
036800     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1054.2
036900     MOVE    SPACE TO TEST-RESULTS.                               SQ1054.2
037000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1054.2
037100     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1054.2
037200     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1054.2
037300     GO TO CCVS1-EXIT.                                            SQ1054.2
037400*                                                                 SQ1054.2
037500 CCVS-INIT-FILE.                                                  SQ1054.2
037600     ADD     1 TO REC-SKL-SUB.                                    SQ1054.2
037700     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1054.2
037800                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1054.2
037900*                                                                 SQ1054.2
038000 CLOSE-FILES.                                                     SQ1054.2
038100     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1054.2
038200     CLOSE   PRINT-FILE.                                          SQ1054.2
038300*    OPEN    I-O RAW-DATA.                                        SQ1054.2
038400*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1054.2
038500*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1054.2
038600*    MOVE   "OK.     " TO C-ABORT.                                SQ1054.2
038700*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1054.2
038800*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1054.2
038900*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1054.2
039000*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1054.2
039100*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1054.2
039200*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1054.2
039300*END-E-2.                                                         SQ1054.2
039400*    CLOSE   RAW-DATA.                                            SQ1054.2
039500 TERMINATE-CCVS.                                                  SQ1054.2
039600*    EXIT    PROGRAM.                                             SQ1054.2
039700     STOP    RUN.                                                 SQ1054.2
039800*                                                                 SQ1054.2
039900 INSPT.                                                           SQ1054.2
040000     MOVE   "INSPT" TO P-OR-F.                                    SQ1054.2
040100     ADD     1 TO INSPECT-COUNTER.                                SQ1054.2
040200     PERFORM PRINT-DETAIL.                                        SQ1054.2
040300*                                                                 SQ1054.2
040400 PASS.                                                            SQ1054.2
040500     MOVE   "PASS " TO P-OR-F.                                    SQ1054.2
040600     ADD     1 TO PASS-COUNTER.                                   SQ1054.2
040700     PERFORM PRINT-DETAIL.                                        SQ1054.2
040800*                                                                 SQ1054.2
040900 FAIL.                                                            SQ1054.2
041000     MOVE   "FAIL*" TO P-OR-F.                                    SQ1054.2
041100     ADD     1 TO ERROR-COUNTER.                                  SQ1054.2
041200     PERFORM PRINT-DETAIL.                                        SQ1054.2
041300*                                                                 SQ1054.2
041400 DE-LETE.                                                         SQ1054.2
041500     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1054.2
041600     MOVE   "*****" TO P-OR-F.                                    SQ1054.2
041700     ADD     1 TO DELETE-COUNTER.                                 SQ1054.2
041800     PERFORM PRINT-DETAIL.                                        SQ1054.2
041900*                                                                 SQ1054.2
042000 PRINT-DETAIL.                                                    SQ1054.2
042100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1054.2
042200         MOVE   "." TO PARDOT-X                                   SQ1054.2
042300         MOVE    REC-CT TO DOTVALUE.                              SQ1054.2
042400     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1054.2
042500     PERFORM WRITE-LINE.                                          SQ1054.2
042600     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1054.2
042700         PERFORM WRITE-LINE                                       SQ1054.2
042800         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1054.2
042900     ELSE                                                         SQ1054.2
043000         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1054.2
043100     MOVE    SPACE TO P-OR-F.                                     SQ1054.2
043200     MOVE    SPACE TO COMPUTED-X.                                 SQ1054.2
043300     MOVE    SPACE TO CORRECT-X.                                  SQ1054.2
043400     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1054.2
043500     MOVE    SPACE TO RE-MARK.                                    SQ1054.2
043600*                                                                 SQ1054.2
043700 HEAD-ROUTINE.                                                    SQ1054.2
043800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1054.2
043900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1054.2
044000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1054.2
044100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1054.2
044200 COLUMN-NAMES-ROUTINE.                                            SQ1054.2
044300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1054.2
044400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1054.2
044500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1054.2
044600 END-ROUTINE.                                                     SQ1054.2
044700     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1054.2
044800     PERFORM WRITE-LINE 5 TIMES.                                  SQ1054.2
044900 END-RTN-EXIT.                                                    SQ1054.2
045000     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1054.2
045100     PERFORM WRITE-LINE 2 TIMES.                                  SQ1054.2
045200*                                                                 SQ1054.2
045300 END-ROUTINE-1.                                                   SQ1054.2
045400     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1054.2
045500     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1054.2
045600     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1054.2
045700     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1054.2
045800     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1054.2
045900     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1054.2
046000     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1054.2
046100     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1054.2
046200     PERFORM WRITE-LINE.                                          SQ1054.2
046300     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1054.2
046400     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1054.2
046500         MOVE   "NO " TO ERROR-TOTAL                              SQ1054.2
046600     ELSE                                                         SQ1054.2
046700         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1054.2
046800     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1054.2
046900     PERFORM WRITE-LINE.                                          SQ1054.2
047000 END-ROUTINE-13.                                                  SQ1054.2
047100     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1054.2
047200         MOVE   "NO " TO ERROR-TOTAL                              SQ1054.2
047300     ELSE                                                         SQ1054.2
047400         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1054.2
047500     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1054.2
047600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1054.2
047700     PERFORM WRITE-LINE.                                          SQ1054.2
047800     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1054.2
047900         MOVE   "NO " TO ERROR-TOTAL                              SQ1054.2
048000     ELSE                                                         SQ1054.2
048100         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1054.2
048200     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1054.2
048300     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1054.2
048400     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1054.2
048500*                                                                 SQ1054.2
048600 WRITE-LINE.                                                      SQ1054.2
048700     ADD     1 TO RECORD-COUNT.                                   SQ1054.2
048800*    IF RECORD-COUNT GREATER 50                                   SQ1054.2
048900*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1054.2
049000*        MOVE  SPACE TO DUMMY-RECORD                              SQ1054.2
049100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1054.2
049200*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1054.2
049300*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1054.2
049400*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1054.2
049500*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1054.2
049600*        MOVE  ZERO TO RECORD-COUNT.                              SQ1054.2
049700     PERFORM WRT-LN.                                              SQ1054.2
049800*                                                                 SQ1054.2
049900 WRT-LN.                                                          SQ1054.2
050000     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1054.2
050100     MOVE    SPACE TO DUMMY-RECORD.                               SQ1054.2
050200 BLANK-LINE-PRINT.                                                SQ1054.2
050300     PERFORM WRT-LN.                                              SQ1054.2
050400 FAIL-ROUTINE.                                                    SQ1054.2
050500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1054.2
050600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1054.2
050700     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1054.2
050800     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1054.2
050900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1054.2
051000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1054.2
051100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1054.2
051200     GO TO   FAIL-ROUTINE-EX.                                     SQ1054.2
051300 FAIL-ROUTINE-WRITE.                                              SQ1054.2
051400     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1054.2
051500     PERFORM WRITE-LINE                                           SQ1054.2
051600     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1054.2
051700     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1054.2
051800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1054.2
051900     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1054.2
052000 FAIL-ROUTINE-EX.                                                 SQ1054.2
052100     EXIT.                                                        SQ1054.2
052200 BAIL-OUT.                                                        SQ1054.2
052300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1054.2
052400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1054.2
052500 BAIL-OUT-WRITE.                                                  SQ1054.2
052600     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1054.2
052700     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1054.2
052800     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1054.2
052900     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1054.2
053000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1054.2
053100     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1054.2
053200 BAIL-OUT-EX.                                                     SQ1054.2
053300     EXIT.                                                        SQ1054.2
053400 CCVS1-EXIT.                                                      SQ1054.2
053500     EXIT.                                                        SQ1054.2
053600*                                                                 SQ1054.2
053700****************************************************************  SQ1054.2
053800*                                                              *  SQ1054.2
053900*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1054.2
054000*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1054.2
054100*                                                              *  SQ1054.2
054200****************************************************************  SQ1054.2
054300*                                                                 SQ1054.2
054400 SECT-SQ105-0003 SECTION.                                         SQ1054.2
054500 INITIAL-PARA.                                                    SQ1054.2
054600     MOVE   "SQ-FS4" TO XFILE-NAME (1).                           SQ1054.2
054700     MOVE   "R1-F-G" TO XRECORD-NAME (1).                         SQ1054.2
054800     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1054.2
054900     MOVE    000125 TO XRECORD-LENGTH (1).                        SQ1054.2
055000     MOVE   "RC" TO CHARS-OR-RECORDS (1).                         SQ1054.2
055100     MOVE    0002 TO XBLOCK-SIZE (1).                             SQ1054.2
055200     MOVE    000980 TO RECORDS-IN-FILE (1).                       SQ1054.2
055300     MOVE   "SQ" TO XFILE-ORGANIZATION (1).                       SQ1054.2
055400     MOVE   "S" TO XLABEL-TYPE (1).                               SQ1054.2
055500     MOVE    ZERO TO MAJOR-DELETIONS.                             SQ1054.2
055600*                                                                 SQ1054.2
055700*    THE INITIAL ACTIONS ARE TO CREATE A FILE FOR USE             SQ1054.2
055800*    IN LATER TESTS.  FILE STATUS VALUES AND DECLARATIVE          SQ1054.2
055900*    EXECUTION DURING THE CREATION PROCESS ARE MONITORED.         SQ1054.2
056000*                                                                 SQ1054.2
056100 SEQ-INIT-01.                                                     SQ1054.2
056200     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1054.2
056300     MOVE    0 TO COUNT-OF-RECS.                                  SQ1054.2
056400     MOVE   "CREATE 980 RECORD FILE" TO FEATURE.                  SQ1054.2
056500     MOVE   "SEQ-TEST-WR-01" TO PAR-NAME.                         SQ1054.2
056600     MOVE   "**"    TO SQ-FS4-STATUS.                             SQ1054.2
056700     MOVE   "00"    TO SQ-FS4-STATUS-COPY.                        SQ1054.2
056800     MOVE   "**"    TO DECL-EXEC-SW.                              SQ1054.2
056900     MOVE    1 TO REC-CT.                                         SQ1054.2
057000     GO TO   SEQ-TEST-WR-01.                                      SQ1054.2
057100 SEQ-DELETE-01.                                                   SQ1054.2
057200     MOVE    1 TO MAJOR-DELETIONS.                                SQ1054.2
057300     GO TO   SEQ-DELETE-01-01.                                    SQ1054.2
057400 SEQ-TEST-WR-01.                                                  SQ1054.2
057500     OPEN    OUTPUT SQ-FS4.                                       SQ1054.2
057600     IF SQ-FS4-STATUS NOT EQUAL TO "00"                           SQ1054.2
057700         MOVE    SQ-FS4-STATUS TO SQ-FS4-STATUS-COPY              SQ1054.2
057800         MOVE   "00" TO SQ-FS4-STATUS.                            SQ1054.2
057900*                                                                 SQ1054.2
058000 SEQ-TEST-WR-01-LOOP.                                             SQ1054.2
058100     ADD     1 TO XRECORD-NUMBER (1).                             SQ1054.2
058200     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4-FIRST.         SQ1054.2
058300     ADD     1 TO COUNT-OF-RECS.                                  SQ1054.2
058400     MOVE    COUNT-OF-RECS TO SQ-FS4-REC-NO.                      SQ1054.2
058500     WRITE   SQ-FS4R1-F-G-125.                                    SQ1054.2
058600     IF SQ-FS4-STATUS NOT EQUAL TO "00"                           SQ1054.2
058700         MOVE    SQ-FS4-STATUS TO SQ-FS4-STATUS-COPY              SQ1054.2
058800         MOVE   "00" TO SQ-FS4-STATUS.                            SQ1054.2
058900     IF PERM-ERRORS EQUAL TO 1                                    SQ1054.2
059000         GO TO   SEQ-TEST-WR-01-LOOP-EXIT.                        SQ1054.2
059100     IF COUNT-OF-RECS LESS THAN 980                               SQ1054.2
059200         GO TO   SEQ-TEST-WR-01-LOOP.                             SQ1054.2
059300*                                                                 SQ1054.2
059400 SEQ-TEST-WR-01-LOOP-EXIT.                                        SQ1054.2
059500*                                                                 SQ1054.2
059600     CLOSE   SQ-FS4.                                              SQ1054.2
059700     IF SQ-FS4-STATUS NOT EQUAL TO "00"                           SQ1054.2
059800         MOVE    SQ-FS4-STATUS TO SQ-FS4-STATUS-COPY              SQ1054.2
059900         MOVE   "00" TO SQ-FS4-STATUS.                            SQ1054.2
060000*                                                                 SQ1054.2
060100     GO TO   SEQ-TEST-WR-01-01.                                   SQ1054.2
060200 SEQ-DELETE-01-01.                                                SQ1054.2
060300     PERFORM DE-LETE.                                             SQ1054.2
060400     GO TO   SEQ-INIT-WR-01-02.                                   SQ1054.2
060500 SEQ-TEST-WR-01-01.                                               SQ1054.2
060600*                                                                 SQ1054.2
060700     IF SQ-FS4-STATUS-COPY EQUAL "00"                             SQ1054.2
060800         PERFORM PASS                                             SQ1054.2
060900     ELSE                                                         SQ1054.2
061000         MOVE    SQ-FS4-STATUS-COPY TO COMPUTED-A                 SQ1054.2
061100         MOVE   "00" TO CORRECT-A                                 SQ1054.2
061200         MOVE   "ERROR I-O STATUS DURING FILE CREATION"           SQ1054.2
061300                    TO RE-MARK                                    SQ1054.2
061400         PERFORM FAIL.                                            SQ1054.2
061500*                                                                 SQ1054.2
061600 SEQ-INIT-WR-01-02.                                               SQ1054.2
061700     ADD     1 TO REC-CT.                                         SQ1054.2
061800     IF MAJOR-DELETIONS = 1                                       SQ1054.2
061900         GO TO   SEQ-DELETE-01-02.                                SQ1054.2
062000     GO TO   SEQ-TEST-WR-01-02.                                   SQ1054.2
062100 SEQ-DELETE-01-02.                                                SQ1054.2
062200     PERFORM DE-LETE.                                             SQ1054.2
062300     GO TO   SEQ-TEST-01-END.                                     SQ1054.2
062400 SEQ-TEST-WR-01-02.                                               SQ1054.2
062500     IF DECL-EXEC-SW = "**"                                       SQ1054.2
062600         PERFORM PASS                                             SQ1054.2
062700     ELSE                                                         SQ1054.2
062800         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
062900         MOVE   "**" TO CORRECT-A                                 SQ1054.2
063000         MOVE                                                     SQ1054.2
063100          "UNEXPECTED DECLARATIVE EXECUTION DURING FILE CREATION" SQ1054.2
063200              TO RE-MARK                                          SQ1054.2
063300         PERFORM FAIL.                                            SQ1054.2
063400 SEQ-TEST-01-END.                                                 SQ1054.2
063500*                                                                 SQ1054.2
063600*    A SEQUENTIAL MASS STORAGE FILE WITH 125 CHARACTER RECORDS,   SQ1054.2
063700*    TWO RECORDS PER BLOCK, HAS BEEN CREATED.  THE FILE           SQ1054.2
063800*    CONTAINS 980 RECORDS.  THE FOLLOWING TESTS READ AND          SQ1054.2
063900*    VERIFY THE RECORDS IN THE FILE.  THE READ STATEMENT DOES     SQ1054.2
064000*    NOT CONTAIN AN AT END PHRASE, SO THE INPUT DECLARATIVE       SQ1054.2
064100*    SHOULD BE EXECUTED AT THE END OF THE FILE.  THE READ         SQ1054.2
064200*    STATEMENT CONTAINS THE OPTIONAL WORD "RECORD"                SQ1054.2
064300*                                                                 SQ1054.2
064400 SEQ-INIT-02.                                                     SQ1054.2
064500     MOVE    1 TO REC-CT.                                         SQ1054.2
064600     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
064700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
064800     MOVE   "OPEN FILE FOR CHECK" TO FEATURE.                     SQ1054.2
064900     MOVE   "SEQ-TEST-GF-02" TO PAR-NAME.                         SQ1054.2
065000     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
065100         GO TO SEQ-DELETE-02.                                     SQ1054.2
065200     GO TO   SEQ-TEST-GF-02.                                      SQ1054.2
065300 SEQ-DELETE-02.                                                   SQ1054.2
065400     ADD     2 TO MAJOR-DELETIONS.                                SQ1054.2
065500     GO TO   SEQ-DELETE-02-01.                                    SQ1054.2
065600*                                                                 SQ1054.2
065700 SEQ-TEST-GF-02.                                                  SQ1054.2
065800     OPEN INPUT SQ-FS4.                                           SQ1054.2
065900     GO TO   SEQ-TEST-GF-02-01.                                   SQ1054.2
066000 SEQ-DELETE-02-01.                                                SQ1054.2
066100     PERFORM DE-LETE.                                             SQ1054.2
066200     GO TO   SEQ-TEST-02-01-END.                                  SQ1054.2
066300 SEQ-TEST-GF-02-01.                                               SQ1054.2
066400     IF SQ-FS4-STATUS = "00"                                      SQ1054.2
066500         PERFORM PASS                                             SQ1054.2
066600     ELSE                                                         SQ1054.2
066700         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1054.2
066800         MOVE   "00" TO CORRECT-A                                 SQ1054.2
066900         MOVE   "FAILURE STATUS CODE AFTER OPEN" TO RE-MARK       SQ1054.2
067000         PERFORM FAIL.                                            SQ1054.2
067100     IF SQ-FS4-STATUS GREATER THAN "10"                           SQ1054.2
067200         ADD     2 TO MAJOR-DELETIONS.                            SQ1054.2
067300 SEQ-TEST-02-01-END.                                              SQ1054.2
067400*                                                                 SQ1054.2
067500     ADD     1 TO REC-CT.                                         SQ1054.2
067600     GO TO   SEQ-TEST-GF-02-02.                                   SQ1054.2
067700 SEQ-DELETE-02-02.                                                SQ1054.2
067800     PERFORM DE-LETE.                                             SQ1054.2
067900     GO TO   SEQ-TEST-02-02-END.                                  SQ1054.2
068000 SEQ-TEST-GF-02-02.                                               SQ1054.2
068100     IF DECL-EXEC-SW = "**"                                       SQ1054.2
068200         PERFORM PASS                                             SQ1054.2
068300     ELSE                                                         SQ1054.2
068400         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
068500         MOVE   "**" TO CORRECT-A                                 SQ1054.2
068600         MOVE   "DECLARATIVE EXECUTED ON OPEN" TO RE-MARK         SQ1054.2
068700         PERFORM FAIL.                                            SQ1054.2
068800 SEQ-TEST-02-02-END.                                              SQ1054.2
068900*                                                                 SQ1054.2
069000 SEQ-INIT-03.                                                     SQ1054.2
069100     MOVE    1 TO REC-CT.                                         SQ1054.2
069200     MOVE   "READ ... RECORD" TO FEATURE.                         SQ1054.2
069300     MOVE   "SEQ-TEST-GF-03" TO PAR-NAME.                         SQ1054.2
069400     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
069500         GO TO SEQ-DELETE-03.                                     SQ1054.2
069600     GO TO   SEQ-TEST-GF-03.                                      SQ1054.2
069700 SEQ-DELETE-03.                                                   SQ1054.2
069800     ADD     4 TO MAJOR-DELETIONS.                                SQ1054.2
069900     GO TO   SEQ-DELETE-03-01.                                    SQ1054.2
070000 SEQ-TEST-GF-03.                                                  SQ1054.2
070100     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1054.2
070200     MOVE    ZERO TO COUNT-OF-RECS.                               SQ1054.2
070300     MOVE    ZERO TO PERM-ERRORS.                                 SQ1054.2
070400     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1054.2
070500     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
070600     MOVE   "00" TO SQ-FS4-STATUS-COPY.                           SQ1054.2
070700 SEQ-TEST-03-LOOP.                                                SQ1054.2
070800     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
070900     READ    SQ-FS4 RECORD.                                       SQ1054.2
071000     IF DECL-EXEC-SW NOT = "**"                                   SQ1054.2
071100          GO TO SEQ-TEST-GF-03-01.                                SQ1054.2
071200     IF SQ-FS4-STATUS = "10"                                      SQ1054.2
071300          GO TO SEQ-TEST-GF-03-LOOP-END.                          SQ1054.2
071400     IF SQ-FS4-STATUS NOT = "00"                                  SQ1054.2
071500          MOVE    SQ-FS4-STATUS TO SQ-FS4-STATUS-COPY.            SQ1054.2
071600     ADD     1 TO XRECORD-NUMBER (1).                             SQ1054.2
071700     ADD     1 TO COUNT-OF-RECS.                                  SQ1054.2
071800     IF SQ-FS4-FIRST NOT EQUAL TO FILE-RECORD-INFO-P1-120 (1)     SQ1054.2
071900        OR SQ-FS4-REC-NO NOT EQUAL TO COUNT-OF-RECS               SQ1054.2
072000         ADD     1 TO RECORDS-IN-ERROR.                           SQ1054.2
072100     IF COUNT-OF-RECS LESS THAN 980                               SQ1054.2
072200         GO TO   SEQ-TEST-03-LOOP.                                SQ1054.2
072300*                                                                 SQ1054.2
072400 SEQ-TEST-GF-03-LOOP-END.                                         SQ1054.2
072500     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
072600         GO TO   SEQ-DELETE-03-01.                                SQ1054.2
072700     GO TO   SEQ-TEST-GF-03-01.                                   SQ1054.2
072800 SEQ-DELETE-03-01.                                                SQ1054.2
072900     PERFORM DE-LETE.                                             SQ1054.2
073000     GO TO   SEQ-TEST-03-01-END.                                  SQ1054.2
073100 SEQ-TEST-GF-03-01.                                               SQ1054.2
073200     IF COUNT-OF-RECS EQUAL TO 980                                SQ1054.2
073300         PERFORM PASS                                             SQ1054.2
073400     ELSE                                                         SQ1054.2
073500         MOVE    COUNT-OF-RECS TO COMPUTED-18V0                   SQ1054.2
073600         MOVE    980 TO CORRECT-18V0                              SQ1054.2
073700         MOVE   "UNEXPECTED NUMBER OF RECORDS BEFORE EOF"         SQ1054.2
073800                    TO RE-MARK                                    SQ1054.2
073900         PERFORM FAIL.                                            SQ1054.2
074000 SEQ-TEST-03-01-END.                                              SQ1054.2
074100*                                                                 SQ1054.2
074200     ADD     1 TO REC-CT.                                         SQ1054.2
074300     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
074400         GO TO   SEQ-TEST-03-02-END.                              SQ1054.2
074500     GO TO   SEQ-TEST-GF-03-02.                                   SQ1054.2
074600 SEQ-DELETE-03-02.                                                SQ1054.2
074700     PERFORM DE-LETE.                                             SQ1054.2
074800     GO TO   SEQ-TEST-03-02-END.                                  SQ1054.2
074900 SEQ-TEST-GF-03-02.                                               SQ1054.2
075000     IF DECL-EXEC-SW = "**"                                       SQ1054.2
075100         PERFORM PASS                                             SQ1054.2
075200     ELSE                                                         SQ1054.2
075300         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
075400         MOVE   "**" TO CORRECT-A                                 SQ1054.2
075500         MOVE   "DECLARATIVES ENTERED AT LEAST ONCE" TO RE-MARK   SQ1054.2
075600         PERFORM FAIL.                                            SQ1054.2
075700 SEQ-TEST-03-02-END.                                              SQ1054.2
075800*                                                                 SQ1054.2
075900     ADD     1 TO REC-CT.                                         SQ1054.2
076000     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
076100         GO TO   SEQ-TEST-03-03-END.                              SQ1054.2
076200     GO TO   SEQ-TEST-GF-03-03.                                   SQ1054.2
076300 SEQ-DELETE-03-03.                                                SQ1054.2
076400     PERFORM DE-LETE.                                             SQ1054.2
076500     GO TO   SEQ-TEST-03-03-END.                                  SQ1054.2
076600 SEQ-TEST-GF-03-03.                                               SQ1054.2
076700     IF SQ-FS4-STATUS-COPY = "00"                                 SQ1054.2
076800         PERFORM PASS                                             SQ1054.2
076900     ELSE                                                         SQ1054.2
077000         MOVE    SQ-FS4-STATUS-COPY TO COMPUTED-A                 SQ1054.2
077100         MOVE   "00" TO CORRECT-A                                 SQ1054.2
077200         MOVE   "AT LEAST ONE UNSUCCESSFUL READ" TO RE-MARK       SQ1054.2
077300         MOVE   "VII-2" TO ANSI-REFERENCE                         SQ1054.2
077400         PERFORM FAIL.                                            SQ1054.2
077500 SEQ-TEST-03-03-END.                                              SQ1054.2
077600*                                                                 SQ1054.2
077700     ADD     1 TO REC-CT.                                         SQ1054.2
077800     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
077900         GO TO   SEQ-TEST-03-04-END.                              SQ1054.2
078000     GO TO   SEQ-TEST-GF-03-04.                                   SQ1054.2
078100 SEQ-DELETE-03-04.                                                SQ1054.2
078200     PERFORM DE-LETE.                                             SQ1054.2
078300     GO TO   SEQ-TEST-03-04-END.                                  SQ1054.2
078400 SEQ-TEST-GF-03-04.                                               SQ1054.2
078500     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1054.2
078600         PERFORM PASS                                             SQ1054.2
078700     ELSE                                                         SQ1054.2
078800         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1054.2
078900         MOVE    ZERO TO CORRECT-18V0                             SQ1054.2
079000         MOVE   "ONE OR MORE ERRORS IN RECORDS READ" TO RE-MARK   SQ1054.2
079100         PERFORM FAIL.                                            SQ1054.2
079200 SEQ-TEST-03-04-END.                                              SQ1054.2
079300*                                                                 SQ1054.2
079400*                                                                 SQ1054.2
079500 SEQ-INIT-04.                                                     SQ1054.2
079600     MOVE    1 TO REC-CT.                                         SQ1054.2
079700     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
079800     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
079900     MOVE   "READ ... RECORD, EOF" TO FEATURE.                    SQ1054.2
080000     MOVE   "SEQ-TEST-GF-04" TO PAR-NAME.                         SQ1054.2
080100     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
080200         GO TO SEQ-DELETE-04.                                     SQ1054.2
080300     GO TO   SEQ-TEST-GF-04.                                      SQ1054.2
080400 SEQ-DELETE-04.                                                   SQ1054.2
080500     ADD     8 TO MAJOR-DELETIONS.                                SQ1054.2
080600     PERFORM DE-LETE.                                             SQ1054.2
080700     GO TO   SEQ-DELETE-04-01.                                    SQ1054.2
080800 SEQ-TEST-GF-04.                                                  SQ1054.2
080900     READ    SQ-FS4 RECORD.                                       SQ1054.2
081000*                                                                 SQ1054.2
081100     GO TO   SEQ-TEST-GF-04-01.                                   SQ1054.2
081200 SEQ-DELETE-04-01.                                                SQ1054.2
081300     PERFORM DE-LETE.                                             SQ1054.2
081400     GO TO   SEQ-TEST-04-01-END.                                  SQ1054.2
081500 SEQ-TEST-GF-04-01.                                               SQ1054.2
081600     IF SQ-FS4-STATUS = "10"                                      SQ1054.2
081700         PERFORM PASS                                             SQ1054.2
081800     ELSE                                                         SQ1054.2
081900         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1054.2
082000         MOVE   "10" TO CORRECT-A                                 SQ1054.2
082100         MOVE   "END OF FILE STATUS CODE NOT GIVEN AT EOF"        SQ1054.2
082200                    TO RE-MARK                                    SQ1054.2
082300         MOVE   "VII-3, 1.3.5(2)A" TO ANSI-REFERENCE              SQ1054.2
082400         PERFORM FAIL.                                            SQ1054.2
082500 SEQ-TEST-04-01-END.                                              SQ1054.2
082600*                                                                 SQ1054.2
082700     ADD     1 TO REC-CT.                                         SQ1054.2
082800     IF MAJOR-DELETIONS NOT EQUAL TO 0                            SQ1054.2
082900         GO TO   SEQ-DELETE-04-02.                                SQ1054.2
083000     GO TO   SEQ-TEST-GF-04-02.                                   SQ1054.2
083100 SEQ-DELETE-04-02.                                                SQ1054.2
083200     PERFORM DE-LETE.                                             SQ1054.2
083300     GO TO   SEQ-TEST-04-02-END.                                  SQ1054.2
083400 SEQ-TEST-GF-04-02.                                               SQ1054.2
083500     IF DECL-EXEC-SW EQUAL "*I"                                   SQ1054.2
083600         PERFORM PASS                                             SQ1054.2
083700     ELSE                                                         SQ1054.2
083800         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
083900         MOVE   "*I" TO CORRECT-A                                 SQ1054.2
084000         MOVE   "CORRECT DECLARATIVE NOT EXECUTED AT EOF"         SQ1054.2
084100                   TO RE-MARK                                     SQ1054.2
084200         MOVE   "VII-2, VII-46, 4.4.4(10)C" TO ANSI-REFERENCE     SQ1054.2
084300         PERFORM FAIL.                                            SQ1054.2
084400 SEQ-TEST-04-02-END.                                              SQ1054.2
084500*                                                                 SQ1054.2
084600*                                                                 SQ1054.2
084700 SEQ-INIT-05.                                                     SQ1054.2
084800     MOVE    1 TO REC-CT.                                         SQ1054.2
084900     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
085000     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
085100     MOVE   "CLOSE AFTER READ" TO FEATURE.                        SQ1054.2
085200     MOVE   "SEQ-TEST-GF-05" TO PAR-NAME.                         SQ1054.2
085300     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
085400         GO TO   SEQ-DELETE-05.                                   SQ1054.2
085500     GO TO   SEQ-TEST-GF-05.                                      SQ1054.2
085600 SEQ-DELETE-05.                                                   SQ1054.2
085700     GO TO   SEQ-DELETE-05-01.                                    SQ1054.2
085800 SEQ-TEST-GF-05.                                                  SQ1054.2
085900     CLOSE SQ-FS4.                                                SQ1054.2
086000*                                                                 SQ1054.2
086100     GO TO SEQ-TEST-GF-05-01.                                     SQ1054.2
086200 SEQ-DELETE-05-01.                                                SQ1054.2
086300     PERFORM DE-LETE.                                             SQ1054.2
086400     GO TO   SEQ-TEST-05-01-END.                                  SQ1054.2
086500 SEQ-TEST-GF-05-01.                                               SQ1054.2
086600     IF SQ-FS4-STATUS = "00"                                      SQ1054.2
086700         PERFORM PASS                                             SQ1054.2
086800     ELSE                                                         SQ1054.2
086900         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1054.2
087000         MOVE   "00" TO CORRECT-A                                 SQ1054.2
087100         MOVE   "UNEXPECTED I-O STATUS VALUE FROM CLOSE"          SQ1054.2
087200                   TO RE-MARK                                     SQ1054.2
087300         MOVE   "VII-3, VII-38, 4.2.4(4)" TO ANSI-REFERENCE       SQ1054.2
087400         PERFORM FAIL.                                            SQ1054.2
087500 SEQ-TEST-05-01-END.                                              SQ1054.2
087600*                                                                 SQ1054.2
087700     ADD     1 TO REC-CT.                                         SQ1054.2
087800     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
087900         GO TO SEQ-DELETE-05-02.                                  SQ1054.2
088000     GO TO   SEQ-TEST-GF-05-02.                                   SQ1054.2
088100 SEQ-DELETE-05-02.                                                SQ1054.2
088200     PERFORM DE-LETE.                                             SQ1054.2
088300     GO TO   SEQ-TEST-05-02-END.                                  SQ1054.2
088400 SEQ-TEST-GF-05-02.                                               SQ1054.2
088500     IF DECL-EXEC-SW = "**"                                       SQ1054.2
088600         PERFORM PASS                                             SQ1054.2
088700     ELSE                                                         SQ1054.2
088800         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
088900         MOVE   "**" TO CORRECT-A                                 SQ1054.2
089000         MOVE   "DECLARATIVE EXECUTED ON CLOSE FILE" TO RE-MARK   SQ1054.2
089100         PERFORM FAIL.                                            SQ1054.2
089200 SEQ-TEST-05-02-END.                                              SQ1054.2
089300     IF MAJOR-DELETIONS NOT LESS THAN 8                           SQ1054.2
089400         SUBTRACT 8 FROM MAJOR-DELETIONS.                         SQ1054.2
089500     IF MAJOR-DELETIONS NOT LESS THAN 4                           SQ1054.2
089600         SUBTRACT 4 FROM MAJOR-DELETIONS.                         SQ1054.2
089700     IF MAJOR-DELETIONS NOT LESS THAN 2                           SQ1054.2
089800         SUBTRACT 2 FROM MAJOR-DELETIONS.                         SQ1054.2
089900*                                                                 SQ1054.2
090000*                                                                 SQ1054.2
090100*    HAVING PROCESSED THE FILE WITH A READ ... RECORD STATEMENT,  SQ1054.2
090200*    IT WILL NOW BE PROCESSED WITH A READ STATEMENT WITHOUT THE   SQ1054.2
090300*    OPTIONAL WORD RECORD, AND THE SAME TESTS CARRIED OUT.        SQ1054.2
090400*                                                                 SQ1054.2
090500 SEQ-INIT-06.                                                     SQ1054.2
090600     MOVE    1 TO REC-CT.                                         SQ1054.2
090700     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
090800     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
090900     MOVE   "OPEN FILE FOR CHECK" TO FEATURE.                     SQ1054.2
091000     MOVE   "SEQ-TEST-GF-06" TO PAR-NAME.                         SQ1054.2
091100     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
091200         GO TO SEQ-DELETE-06.                                     SQ1054.2
091300     GO TO   SEQ-TEST-GF-06.                                      SQ1054.2
091400 SEQ-DELETE-06.                                                   SQ1054.2
091500     ADD     2 TO MAJOR-DELETIONS.                                SQ1054.2
091600     GO TO   SEQ-DELETE-06-01.                                    SQ1054.2
091700*                                                                 SQ1054.2
091800 SEQ-TEST-GF-06.                                                  SQ1054.2
091900     OPEN INPUT SQ-FS4.                                           SQ1054.2
092000     GO TO   SEQ-TEST-GF-06-01.                                   SQ1054.2
092100 SEQ-DELETE-06-01.                                                SQ1054.2
092200     PERFORM DE-LETE.                                             SQ1054.2
092300     GO TO   SEQ-TEST-06-01-END.                                  SQ1054.2
092400 SEQ-TEST-GF-06-01.                                               SQ1054.2
092500     IF SQ-FS4-STATUS = "00"                                      SQ1054.2
092600         PERFORM PASS                                             SQ1054.2
092700     ELSE                                                         SQ1054.2
092800         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1054.2
092900         MOVE   "00" TO CORRECT-A                                 SQ1054.2
093000         MOVE   "FAILURE STATUS CODE AFTER OPEN" TO RE-MARK       SQ1054.2
093100         PERFORM FAIL.                                            SQ1054.2
093200     IF SQ-FS4-STATUS GREATER THAN "10"                           SQ1054.2
093300         ADD     2 TO MAJOR-DELETIONS.                            SQ1054.2
093400 SEQ-TEST-06-01-END.                                              SQ1054.2
093500*                                                                 SQ1054.2
093600     ADD     1 TO REC-CT.                                         SQ1054.2
093700     GO TO   SEQ-TEST-GF-06-02.                                   SQ1054.2
093800 SEQ-DELETE-06-02.                                                SQ1054.2
093900     PERFORM DE-LETE.                                             SQ1054.2
094000     GO TO   SEQ-TEST-06-02-END.                                  SQ1054.2
094100 SEQ-TEST-GF-06-02.                                               SQ1054.2
094200     IF DECL-EXEC-SW = "**"                                       SQ1054.2
094300         PERFORM PASS                                             SQ1054.2
094400     ELSE                                                         SQ1054.2
094500         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
094600         MOVE   "**" TO CORRECT-A                                 SQ1054.2
094700         MOVE   "DECLARATIVE EXECUTED ON OPEN" TO RE-MARK         SQ1054.2
094800         PERFORM FAIL.                                            SQ1054.2
094900 SEQ-TEST-06-02-END.                                              SQ1054.2
095000*                                                                 SQ1054.2
095100*                                                                 SQ1054.2
095200*    THE NEXT GROUP OF TEST READ THE RECORDS FROM THE FILE,       SQ1054.2
095300*    USING A READ STATEMENT WITHOUT OPTIONAL PHRASES.  THE        SQ1054.2
095400*    RECORDS RETURNED FROM THE FILE ARE CHECKED FOR EXPECTED      SQ1054.2
095500*    CONTENTS.  FILE STATUS VALUES AND EXECUTION OF               SQ1054.2
095600*    DECLARATIVE PROCEDURES ARE ALSO CHECKED.                     SQ1054.2
095700*                                                                 SQ1054.2
095800 SEQ-INIT-07.                                                     SQ1054.2
095900     MOVE    1 TO REC-CT.                                         SQ1054.2
096000     MOVE   "READ ..." TO FEATURE.                                SQ1054.2
096100     MOVE   "SEQ-TEST-GF-07" TO PAR-NAME.                         SQ1054.2
096200     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
096300         GO TO SEQ-DELETE-07.                                     SQ1054.2
096400     GO TO   SEQ-TEST-GF-07.                                      SQ1054.2
096500 SEQ-DELETE-07.                                                   SQ1054.2
096600     ADD     4 TO MAJOR-DELETIONS.                                SQ1054.2
096700     GO TO   SEQ-DELETE-07-01.                                    SQ1054.2
096800 SEQ-TEST-GF-07.                                                  SQ1054.2
096900     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1054.2
097000     MOVE    ZERO TO COUNT-OF-RECS.                               SQ1054.2
097100     MOVE    ZERO TO PERM-ERRORS.                                 SQ1054.2
097200     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1054.2
097300     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
097400     MOVE   "00" TO SQ-FS4-STATUS-COPY.                           SQ1054.2
097500 SEQ-TEST-07-LOOP.                                                SQ1054.2
097600     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
097700     READ    SQ-FS4.                                              SQ1054.2
097800     IF DECL-EXEC-SW NOT = "**"                                   SQ1054.2
097900          GO TO SEQ-TEST-GF-07-01.                                SQ1054.2
098000     IF SQ-FS4-STATUS = "10"                                      SQ1054.2
098100          GO TO SEQ-TEST-GF-07-LOOP-END.                          SQ1054.2
098200     IF SQ-FS4-STATUS NOT = "00"                                  SQ1054.2
098300          MOVE    SQ-FS4-STATUS TO SQ-FS4-STATUS-COPY.            SQ1054.2
098400     ADD     1 TO XRECORD-NUMBER (1).                             SQ1054.2
098500     ADD     1 TO COUNT-OF-RECS.                                  SQ1054.2
098600     IF SQ-FS4-FIRST NOT EQUAL TO FILE-RECORD-INFO-P1-120 (1)     SQ1054.2
098700        OR SQ-FS4-REC-NO NOT EQUAL TO COUNT-OF-RECS               SQ1054.2
098800         ADD     1 TO RECORDS-IN-ERROR.                           SQ1054.2
098900     IF COUNT-OF-RECS LESS THAN 980                               SQ1054.2
099000         GO TO   SEQ-TEST-07-LOOP.                                SQ1054.2
099100*                                                                 SQ1054.2
099200 SEQ-TEST-GF-07-LOOP-END.                                         SQ1054.2
099300     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
099400         GO TO   SEQ-DELETE-07-01.                                SQ1054.2
099500     GO TO   SEQ-TEST-GF-07-01.                                   SQ1054.2
099600 SEQ-DELETE-07-01.                                                SQ1054.2
099700     PERFORM DE-LETE.                                             SQ1054.2
099800     GO TO   SEQ-TEST-07-01-END.                                  SQ1054.2
099900 SEQ-TEST-GF-07-01.                                               SQ1054.2
100000     IF COUNT-OF-RECS EQUAL TO 980                                SQ1054.2
100100         PERFORM PASS                                             SQ1054.2
100200     ELSE                                                         SQ1054.2
100300         MOVE    COUNT-OF-RECS TO COMPUTED-18V0                   SQ1054.2
100400         MOVE    980 TO CORRECT-18V0                              SQ1054.2
100500         MOVE   "UNEXPECTED NUMBER OF RECORDS BEFORE EOF"         SQ1054.2
100600                    TO RE-MARK                                    SQ1054.2
100700         PERFORM FAIL.                                            SQ1054.2
100800 SEQ-TEST-07-01-END.                                              SQ1054.2
100900*                                                                 SQ1054.2
101000     ADD     1 TO REC-CT.                                         SQ1054.2
101100     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
101200         GO TO   SEQ-TEST-07-02-END.                              SQ1054.2
101300     GO TO   SEQ-TEST-GF-07-02.                                   SQ1054.2
101400 SEQ-DELETE-07-02.                                                SQ1054.2
101500     PERFORM DE-LETE.                                             SQ1054.2
101600     GO TO   SEQ-TEST-07-02-END.                                  SQ1054.2
101700 SEQ-TEST-GF-07-02.                                               SQ1054.2
101800     IF DECL-EXEC-SW = "**"                                       SQ1054.2
101900         PERFORM PASS                                             SQ1054.2
102000     ELSE                                                         SQ1054.2
102100         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
102200         MOVE   "**" TO CORRECT-A                                 SQ1054.2
102300         MOVE   "DECLARATIVES ENTERED AT LEAST ONCE" TO RE-MARK   SQ1054.2
102400         PERFORM FAIL.                                            SQ1054.2
102500 SEQ-TEST-07-02-END.                                              SQ1054.2
102600*                                                                 SQ1054.2
102700     ADD     1 TO REC-CT.                                         SQ1054.2
102800     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
102900         GO TO   SEQ-TEST-07-03-END.                              SQ1054.2
103000     GO TO   SEQ-TEST-GF-07-03.                                   SQ1054.2
103100 SEQ-DELETE-07-03.                                                SQ1054.2
103200     PERFORM DE-LETE.                                             SQ1054.2
103300     GO TO   SEQ-TEST-07-03-END.                                  SQ1054.2
103400 SEQ-TEST-GF-07-03.                                               SQ1054.2
103500     IF SQ-FS4-STATUS-COPY = "00"                                 SQ1054.2
103600         PERFORM PASS                                             SQ1054.2
103700     ELSE                                                         SQ1054.2
103800         MOVE    SQ-FS4-STATUS-COPY TO COMPUTED-A                 SQ1054.2
103900         MOVE   "00" TO CORRECT-A                                 SQ1054.2
104000         MOVE   "AT LEAST ONE UNSUCCESSFUL READ" TO RE-MARK       SQ1054.2
104100         MOVE   "VII-2" TO ANSI-REFERENCE                         SQ1054.2
104200         PERFORM FAIL.                                            SQ1054.2
104300 SEQ-TEST-07-03-END.                                              SQ1054.2
104400*                                                                 SQ1054.2
104500     ADD     1 TO REC-CT.                                         SQ1054.2
104600     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
104700         GO TO   SEQ-TEST-07-04-END.                              SQ1054.2
104800     GO TO   SEQ-TEST-GF-07-04.                                   SQ1054.2
104900 SEQ-DELETE-07-04.                                                SQ1054.2
105000     PERFORM DE-LETE.                                             SQ1054.2
105100     GO TO   SEQ-TEST-07-04-END.                                  SQ1054.2
105200 SEQ-TEST-GF-07-04.                                               SQ1054.2
105300     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1054.2
105400         PERFORM PASS                                             SQ1054.2
105500     ELSE                                                         SQ1054.2
105600         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1054.2
105700         MOVE    ZERO TO CORRECT-18V0                             SQ1054.2
105800         MOVE   "ONE OR MORE ERRORS IN RECORDS READ" TO RE-MARK   SQ1054.2
105900         PERFORM FAIL.                                            SQ1054.2
106000 SEQ-TEST-07-04-END.                                              SQ1054.2
106100*                                                                 SQ1054.2
106200*                                                                 SQ1054.2
106300*    THE NEXT TEST EXECUTES ONE READ STATEMENT WITH THE FILE      SQ1054.2
106400*    POSITIONED AFTER THE LAST RECORD.  THE READ STATEMENT DOES   SQ1054.2
106500*    NOT CONTAIN AN AT END PHRASE, SO THE APPROPRIATE             SQ1054.2
106600*    DECLARATIVE SHOULD BE EXECUTED.                              SQ1054.2
106700*                                                                 SQ1054.2
106800 SEQ-INIT-08.                                                     SQ1054.2
106900     MOVE    1 TO REC-CT.                                         SQ1054.2
107000     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
107100     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
107200     MOVE   "READ ... EOF" TO FEATURE.                            SQ1054.2
107300     MOVE   "SEQ-TEST-GF-08" TO PAR-NAME.                         SQ1054.2
107400     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
107500         GO TO SEQ-DELETE-08.                                     SQ1054.2
107600     GO TO   SEQ-TEST-GF-08.                                      SQ1054.2
107700 SEQ-DELETE-08.                                                   SQ1054.2
107800     ADD     8 TO MAJOR-DELETIONS.                                SQ1054.2
107900     PERFORM DE-LETE.                                             SQ1054.2
108000     GO TO   SEQ-DELETE-08-01.                                    SQ1054.2
108100 SEQ-TEST-GF-08.                                                  SQ1054.2
108200     READ    SQ-FS4.                                              SQ1054.2
108300*                                                                 SQ1054.2
108400     GO TO   SEQ-TEST-GF-08-01.                                   SQ1054.2
108500 SEQ-DELETE-08-01.                                                SQ1054.2
108600     PERFORM DE-LETE.                                             SQ1054.2
108700     GO TO   SEQ-TEST-08-01-END.                                  SQ1054.2
108800 SEQ-TEST-GF-08-01.                                               SQ1054.2
108900     IF SQ-FS4-STATUS = "10"                                      SQ1054.2
109000         PERFORM PASS                                             SQ1054.2
109100     ELSE                                                         SQ1054.2
109200         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1054.2
109300         MOVE   "10" TO CORRECT-A                                 SQ1054.2
109400         MOVE   "END OF FILE STATUS CODE NOT GIVEN AT EOF"        SQ1054.2
109500                    TO RE-MARK                                    SQ1054.2
109600         MOVE   "VII-3, 1.3.5(2)A" TO ANSI-REFERENCE              SQ1054.2
109700         PERFORM FAIL.                                            SQ1054.2
109800 SEQ-TEST-08-01-END.                                              SQ1054.2
109900*                                                                 SQ1054.2
110000     ADD     1 TO REC-CT.                                         SQ1054.2
110100     IF MAJOR-DELETIONS NOT EQUAL TO 0                            SQ1054.2
110200         GO TO   SEQ-DELETE-08-02.                                SQ1054.2
110300     GO TO   SEQ-TEST-GF-08-02.                                   SQ1054.2
110400 SEQ-DELETE-08-02.                                                SQ1054.2
110500     PERFORM DE-LETE.                                             SQ1054.2
110600     GO TO   SEQ-TEST-08-02-END.                                  SQ1054.2
110700 SEQ-TEST-GF-08-02.                                               SQ1054.2
110800     IF DECL-EXEC-SW EQUAL "*I"                                   SQ1054.2
110900         PERFORM PASS                                             SQ1054.2
111000     ELSE                                                         SQ1054.2
111100         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
111200         MOVE   "*I" TO CORRECT-A                                 SQ1054.2
111300         MOVE   "CORRECT DECLARATIVE NOT EXECUTED AT EOF"         SQ1054.2
111400                  TO RE-MARK                                      SQ1054.2
111500         MOVE   "VII-2, VII-46, 4.4.4(10)C" TO ANSI-REFERENCE     SQ1054.2
111600         PERFORM FAIL.                                            SQ1054.2
111700 SEQ-TEST-08-02-END.                                              SQ1054.2
111800*                                                                 SQ1054.2
111900*                                                                 SQ1054.2
112000*    CLOSE THE FILE AND CHECK FILE STATUS AND THAT THE            SQ1054.2
112100*    DECLARATIVE IS NOT EXECUTED                                  SQ1054.2
112200*                                                                 SQ1054.2
112300 SEQ-INIT-09.                                                     SQ1054.2
112400     MOVE    1 TO REC-CT.                                         SQ1054.2
112500     MOVE   "**" TO SQ-FS4-STATUS.                                SQ1054.2
112600     MOVE   "**" TO DECL-EXEC-SW.                                 SQ1054.2
112700     MOVE   "CLOSE AFTER READ" TO FEATURE.                        SQ1054.2
112800     MOVE   "SEQ-TEST-GF-09" TO PAR-NAME.                         SQ1054.2
112900     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
113000         GO TO   SEQ-DELETE-09.                                   SQ1054.2
113100     GO TO   SEQ-TEST-GF-09.                                      SQ1054.2
113200 SEQ-DELETE-09.                                                   SQ1054.2
113300     GO TO   SEQ-DELETE-09-01.                                    SQ1054.2
113400 SEQ-TEST-GF-09.                                                  SQ1054.2
113500     CLOSE SQ-FS4.                                                SQ1054.2
113600*                                                                 SQ1054.2
113700     GO TO SEQ-TEST-GF-09-01.                                     SQ1054.2
113800 SEQ-DELETE-09-01.                                                SQ1054.2
113900     PERFORM DE-LETE.                                             SQ1054.2
114000     GO TO   SEQ-TEST-09-01-END.                                  SQ1054.2
114100 SEQ-TEST-GF-09-01.                                               SQ1054.2
114200     IF SQ-FS4-STATUS = "00"                                      SQ1054.2
114300         PERFORM PASS                                             SQ1054.2
114400     ELSE                                                         SQ1054.2
114500         MOVE    SQ-FS4-STATUS TO COMPUTED-A                      SQ1054.2
114600         MOVE   "00" TO CORRECT-A                                 SQ1054.2
114700         MOVE   "UNEXPECTED I-O STATUS VALUE FROM CLOSE"          SQ1054.2
114800                   TO RE-MARK                                     SQ1054.2
114900         MOVE   "VII-3, VII-38, 4.2.4(4)" TO ANSI-REFERENCE       SQ1054.2
115000         PERFORM FAIL.                                            SQ1054.2
115100 SEQ-TEST-09-01-END.                                              SQ1054.2
115200*                                                                 SQ1054.2
115300     ADD     1 TO REC-CT.                                         SQ1054.2
115400     IF MAJOR-DELETIONS NOT = 0                                   SQ1054.2
115500         GO TO SEQ-DELETE-09-02.                                  SQ1054.2
115600     GO TO   SEQ-TEST-GF-09-02.                                   SQ1054.2
115700 SEQ-DELETE-09-02.                                                SQ1054.2
115800     PERFORM DE-LETE.                                             SQ1054.2
115900     GO TO   SEQ-TEST-09-02-END.                                  SQ1054.2
116000 SEQ-TEST-GF-09-02.                                               SQ1054.2
116100     IF DECL-EXEC-SW = "**"                                       SQ1054.2
116200         PERFORM PASS                                             SQ1054.2
116300     ELSE                                                         SQ1054.2
116400         MOVE    DECL-EXEC-SW TO COMPUTED-A                       SQ1054.2
116500         MOVE   "**" TO CORRECT-A                                 SQ1054.2
116600         MOVE   "DECLARATIVE EXECUTED ON CLOSE FILE" TO RE-MARK   SQ1054.2
116700         PERFORM FAIL.                                            SQ1054.2
116800 SEQ-TEST-09-02-END.                                              SQ1054.2
116900*                                                                 SQ1054.2
117000 TERMINATE-ROUTINE.                                               SQ1054.2
117100     EXIT.                                                        SQ1054.2
117200 CCVS-EXIT SECTION.                                               SQ1054.2
117300 CCVS-999999.                                                     SQ1054.2
117400     GO TO CLOSE-FILES.                                           SQ1054.2
      *END-OF,SQ105A                                                                  
