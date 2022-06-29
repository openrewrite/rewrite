      *HEADER,COBOL,SQ103A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1034.2
000200 PROGRAM-ID.                                                      SQ1034.2
000300     SQ103A.                                                      SQ1034.2
000400****************************************************************  SQ1034.2
000500*                                                              *  SQ1034.2
000600*    VALIDATION FOR:-                                          *  SQ1034.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1034.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1034.2
000900*                             REVISED 1986, AUGUST             *  SQ1034.2
001000*                                                              *  SQ1034.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1034.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1034.2
001300*                                                              *  SQ1034.2
001400****************************************************************  SQ1034.2
001500*                                                              *  SQ1034.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1034.2
001700*                                                              *  SQ1034.2
001800*            X-01   SEQUENTIAL MAGNETIC TAPE FILE                 SQ1034.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1034.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1034.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1034.2
002200*                                                              *  SQ1034.2
002300****************************************************************  SQ1034.2
002400*                                                              *  SQ1034.2
002500*    THIS PROGRAM CREATES A TAPE FILE OF 500 FIXED LENGTH      *  SQ1034.2
002600*    RECORDS, EACH 120 CHARACTERS LONG.  THE FILE IS CLOSED    *  SQ1034.2
002700*    AND OPENED AGAIN AS AN INPUT FILE.  THE FILE IS READ      *  SQ1034.2
002800*    USING A READ STATEMENT WITH THE AT END PHRASE.  RECORDS   *  SQ1034.2
002900*    ARE COUNTED AND COMPARED WITH THE VALUES WRITTEN TO       *  SQ1034.2
003000*    ENSURE THAT THEY WERE PROCESSED CORRECTLY.  THERE IS A    *  SQ1034.2
003100*    DECLARATIVE PROCEDURE FOR THE FILE AND TESTS ARE MADE     *  SQ1034.2
003200*    DURING THIS PASS TO CHECK THAT IT IS NOT ENTERED AFTER    *  SQ1034.2
003300*    I-O STATEMENT, INCLUDING THAT WHICH RAISES THE AT END     *  SQ1034.2
003400*    CONDITION.  THE FILE IS CLOSED AND OPENED IN THE INPUT    *  SQ1034.2
003500*    MODE AGAIN.  ON THIS PASS, THE FILE IS READ USING READ    *  SQ1034.2
003600*    STATEMENTS WITHOUT THE AT END PHRASE.  FIRST HUNDRED      *  SQ1034.2
003700*    RECORDS ARE READ USING A READ STATEMENT WITH THE OPTIONAL *  SQ1034.2
003800*    WORD "RECORD", THE REMAINDER WITHOUT IT.  ON THIS PASS,   *  SQ1034.2
003900*    THE AT END CONDITION SHOULD CAUSE EXECUTION OF THE        *  SQ1034.2
004000*    DECLARATIVE PROCEDURE.                                    *  SQ1034.2
004100*                                                              *  SQ1034.2
004200*    THE FILE-CONTROL ENTRY FOR THE FILE CONTAINS A FILE       *  SQ1034.2
004300*    STATUS CLAUSE, AND TESTS CHECK THAT EACH I-O OPERATION    *  SQ1034.2
004400*    RETURNS THE APPROPRIATE STATUS VALUE.                     *  SQ1034.2
004500*                                                              *  SQ1034.2
004600****************************************************************  SQ1034.2
004700*                                                                 SQ1034.2
004800*                                                                 SQ1034.2
004900 ENVIRONMENT DIVISION.                                            SQ1034.2
005000 CONFIGURATION SECTION.                                           SQ1034.2
005100 SOURCE-COMPUTER.                                                 SQ1034.2
005200     XXXXX082.                                                    SQ1034.2
005300 OBJECT-COMPUTER.                                                 SQ1034.2
005400     XXXXX083.                                                    SQ1034.2
005500*                                                                 SQ1034.2
005600 INPUT-OUTPUT SECTION.                                            SQ1034.2
005700 FILE-CONTROL.                                                    SQ1034.2
005800     SELECT PRINT-FILE ASSIGN TO                                  SQ1034.2
005900     XXXXX055.                                                    SQ1034.2
006000*                                                                 SQ1034.2
006100*    SELECT RAW-DATA   ASSIGN TO                                  SQ1034.2
006200*    XXXXX062                                                     SQ1034.2
006300*          ORGANIZATION IS INDEXED                                SQ1034.2
006400*          ACCESS MODE  IS RANDOM                                 SQ1034.2
006500*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1034.2
006600*                                                                 SQ1034.2
006700     SELECT SQ-FS2 ASSIGN TO                                      SQ1034.2
006800     XXXXX001                                                     SQ1034.2
006900            ACCESS MODE IS SEQUENTIAL                             SQ1034.2
007000            FILE STATUS IS SQ-FS2-STATUS                          SQ1034.2
007100            ORGANIZATION SEQUENTIAL                               SQ1034.2
007200            .                                                     SQ1034.2
007300*                                                                 SQ1034.2
007400*                                                                 SQ1034.2
007500 DATA DIVISION.                                                   SQ1034.2
007600 FILE SECTION.                                                    SQ1034.2
007700 FD  PRINT-FILE                                                   SQ1034.2
007800C    LABEL RECORDS                                                SQ1034.2
007900C    XXXXX084                                                     SQ1034.2
008000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1034.2
008100               .                                                  SQ1034.2
008200 01  PRINT-REC    PICTURE X(120).                                 SQ1034.2
008300 01  DUMMY-RECORD PICTURE X(120).                                 SQ1034.2
008400*                                                                 SQ1034.2
008500*FD  RAW-DATA.                                                    SQ1034.2
008600*01  RAW-DATA-SATZ.                                               SQ1034.2
008700*    05  RAW-DATA-KEY        PIC X(6).                            SQ1034.2
008800*    05  C-DATE              PIC 9(6).                            SQ1034.2
008900*    05  C-TIME              PIC 9(8).                            SQ1034.2
009000*    05  NO-OF-TESTS         PIC 99.                              SQ1034.2
009100*    05  C-OK                PIC 999.                             SQ1034.2
009200*    05  C-ALL               PIC 999.                             SQ1034.2
009300*    05  C-FAIL              PIC 999.                             SQ1034.2
009400*    05  C-DELETED           PIC 999.                             SQ1034.2
009500*    05  C-INSPECT           PIC 999.                             SQ1034.2
009600*    05  C-NOTE              PIC X(13).                           SQ1034.2
009700*    05  C-INDENT            PIC X.                               SQ1034.2
009800*    05  C-ABORT             PIC X(8).                            SQ1034.2
009900*                                                                 SQ1034.2
010000 FD  SQ-FS2                                                       SQ1034.2
010100C    LABEL RECORD IS STANDARD                                     SQ1034.2
010200                .                                                 SQ1034.2
010300 01  SQ-FS2R1-F-G-120 PIC X(120).                                 SQ1034.2
010400*                                                                 SQ1034.2
010500 WORKING-STORAGE SECTION.                                         SQ1034.2
010600*                                                                 SQ1034.2
010700***************************************************************   SQ1034.2
010800*                                                             *   SQ1034.2
010900*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1034.2
011000*                                                             *   SQ1034.2
011100***************************************************************   SQ1034.2
011200*                                                                 SQ1034.2
011300 01  SQ-FS2-STATUS.                                               SQ1034.2
011400   03  SQ-FS2-KEY-1   PIC X.                                      SQ1034.2
011500   03  SQ-FS2-KEY-2   PIC X.                                      SQ1034.2
011600*                                                                 SQ1034.2
011700 01  SQ-FS2-STATUS-COPY PIC XX.                                   SQ1034.2
011800 01  DECL-EXEC-SW       PIC 9.                                    SQ1034.2
011900 01  DECL-EXEC-COUNT    PIC 99999.                                SQ1034.2
012000 01  EOF-FLAG           PIC 9  VALUE 0.                           SQ1034.2
012100 01  WRK-CS-09V00       PICTURE S9(9) USAGE COMPUTATIONAL.        SQ1034.2
012200 01  RECORDS-IN-ERROR   PIC S9(5) USAGE COMP.                     SQ1034.2
012300 01  ERROR-FLAG         PICTURE 9.                                SQ1034.2
012400*                                                                 SQ1034.2
012500***************************************************************   SQ1034.2
012600*                                                             *   SQ1034.2
012700*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1034.2
012800*                                                             *   SQ1034.2
012900***************************************************************   SQ1034.2
013000*                                                                 SQ1034.2
013100 01  REC-SKEL-SUB   PIC 99.                                       SQ1034.2
013200*                                                                 SQ1034.2
013300 01  FILE-RECORD-INFORMATION-REC.                                 SQ1034.2
013400     03 FILE-RECORD-INFO-SKELETON.                                SQ1034.2
013500        05 FILLER                 PICTURE X(48)       VALUE       SQ1034.2
013600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1034.2
013700        05 FILLER                 PICTURE X(46)       VALUE       SQ1034.2
013800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1034.2
013900        05 FILLER                 PICTURE X(26)       VALUE       SQ1034.2
014000             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1034.2
014100        05 FILLER                 PICTURE X(37)       VALUE       SQ1034.2
014200             ",RECKEY=                             ".             SQ1034.2
014300        05 FILLER                 PICTURE X(38)       VALUE       SQ1034.2
014400             ",ALTKEY1=                             ".            SQ1034.2
014500        05 FILLER                 PICTURE X(38)       VALUE       SQ1034.2
014600             ",ALTKEY2=                             ".            SQ1034.2
014700        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1034.2
014800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1034.2
014900        05 FILE-RECORD-INFO-P1-120.                               SQ1034.2
015000           07 FILLER              PIC X(5).                       SQ1034.2
015100           07 XFILE-NAME          PIC X(6).                       SQ1034.2
015200           07 FILLER              PIC X(8).                       SQ1034.2
015300           07 XRECORD-NAME        PIC X(6).                       SQ1034.2
015400           07 FILLER              PIC X(1).                       SQ1034.2
015500           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1034.2
015600           07 FILLER              PIC X(7).                       SQ1034.2
015700           07 XRECORD-NUMBER      PIC 9(6).                       SQ1034.2
015800           07 FILLER              PIC X(6).                       SQ1034.2
015900           07 UPDATE-NUMBER       PIC 9(2).                       SQ1034.2
016000           07 FILLER              PIC X(5).                       SQ1034.2
016100           07 ODO-NUMBER          PIC 9(4).                       SQ1034.2
016200           07 FILLER              PIC X(5).                       SQ1034.2
016300           07 XPROGRAM-NAME       PIC X(5).                       SQ1034.2
016400           07 FILLER              PIC X(7).                       SQ1034.2
016500           07 XRECORD-LENGTH      PIC 9(6).                       SQ1034.2
016600           07 FILLER              PIC X(7).                       SQ1034.2
016700           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1034.2
016800           07 FILLER              PIC X(1).                       SQ1034.2
016900           07 XBLOCK-SIZE         PIC 9(4).                       SQ1034.2
017000           07 FILLER              PIC X(6).                       SQ1034.2
017100           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1034.2
017200           07 FILLER              PIC X(5).                       SQ1034.2
017300           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1034.2
017400           07 FILLER              PIC X(6).                       SQ1034.2
017500           07 XLABEL-TYPE         PIC X(1).                       SQ1034.2
017600        05 FILE-RECORD-INFO-P121-240.                             SQ1034.2
017700           07 FILLER              PIC X(8).                       SQ1034.2
017800           07 XRECORD-KEY         PIC X(29).                      SQ1034.2
017900           07 FILLER              PIC X(9).                       SQ1034.2
018000           07 ALTERNATE-KEY1      PIC X(29).                      SQ1034.2
018100           07 FILLER              PIC X(9).                       SQ1034.2
018200           07 ALTERNATE-KEY2      PIC X(29).                      SQ1034.2
018300           07 FILLER              PIC X(7).                       SQ1034.2
018400*                                                                 SQ1034.2
018500 01  TEST-RESULTS.                                                SQ1034.2
018600     02 FILLER              PIC X      VALUE SPACE.               SQ1034.2
018700     02  PAR-NAME.                                                SQ1034.2
018800       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1034.2
018900       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1034.2
019000       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1034.2
019100     02 FILLER              PIC X      VALUE SPACE.               SQ1034.2
019200     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1034.2
019300     02 FILLER              PIC X      VALUE SPACE.               SQ1034.2
019400     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1034.2
019500     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1034.2
019600     02 RE-MARK             PIC X(61).                            SQ1034.2
019700 01  TEST-COMPUTED.                                               SQ1034.2
019800   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1034.2
019900   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1034.2
020000   02 COMPUTED-X.                                                 SQ1034.2
020100     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1034.2
020200     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1034.2
020300     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1034.2
020400     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1034.2
020500     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1034.2
020600     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1034.2
020700        04 COMPUTED-18V0                   PIC -9(18).            SQ1034.2
020800        04 FILLER                          PIC X.                 SQ1034.2
020900     03 FILLER PIC X(50) VALUE SPACE.                             SQ1034.2
021000 01  TEST-CORRECT.                                                SQ1034.2
021100     02 FILLER PIC X(30) VALUE SPACE.                             SQ1034.2
021200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1034.2
021300     02 CORRECT-X.                                                SQ1034.2
021400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1034.2
021500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1034.2
021600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1034.2
021700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1034.2
021800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1034.2
021900     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1034.2
022000         04 CORRECT-18V0                     PIC -9(18).          SQ1034.2
022100         04 FILLER                           PIC X.               SQ1034.2
022200     03 FILLER PIC X(2) VALUE SPACE.                              SQ1034.2
022300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1034.2
022400*                                                                 SQ1034.2
022500 01  CCVS-C-1.                                                    SQ1034.2
022600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1034.2
022700     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1034.2
022800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1034.2
022900     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1034.2
023000     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1034.2
023100     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1034.2
023200     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1034.2
023300     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1034.2
023400 01  CCVS-C-2.                                                    SQ1034.2
023500     02 FILLER  PIC X(20)  VALUE  SPACE.                          SQ1034.2
023600     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1034.2
023700     02 FILLER  PIC X(18)  VALUE  SPACE.                          SQ1034.2
023800     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1034.2
023900     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1034.2
024000*                                                                 SQ1034.2
024100 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1034.2
024200 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1034.2
024300 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1034.2
024400 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1034.2
024500 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1034.2
024600 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1034.2
024700 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1034.2
024800 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1034.2
024900 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1034.2
025000 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1034.2
025100 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1034.2
025200 01  CCVS-H-1.                                                    SQ1034.2
025300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1034.2
025400     02  FILLER          PIC X(42)    VALUE                       SQ1034.2
025500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1034.2
025600     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1034.2
025700 01  CCVS-H-2A.                                                   SQ1034.2
025800   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1034.2
025900   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1034.2
026000   02  FILLER            PIC XXXX   VALUE                         SQ1034.2
026100     "4.2 ".                                                      SQ1034.2
026200   02  FILLER            PIC X(28)  VALUE                         SQ1034.2
026300            " COPY - NOT FOR DISTRIBUTION".                       SQ1034.2
026400   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1034.2
026500*                                                                 SQ1034.2
026600 01  CCVS-H-2B.                                                   SQ1034.2
026700   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1034.2
026800   02  TEST-ID           PIC X(9).                                SQ1034.2
026900   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1034.2
027000   02  FILLER            PIC X(12)  VALUE                         SQ1034.2
027100     " HIGH       ".                                              SQ1034.2
027200   02  FILLER            PIC X(22)  VALUE                         SQ1034.2
027300            " LEVEL VALIDATION FOR ".                             SQ1034.2
027400   02  FILLER            PIC X(58)  VALUE                         SQ1034.2
027500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1034.2
027600 01  CCVS-H-3.                                                    SQ1034.2
027700     02  FILLER          PIC X(34)  VALUE                         SQ1034.2
027800            " FOR OFFICIAL USE ONLY    ".                         SQ1034.2
027900     02  FILLER          PIC X(58)  VALUE                         SQ1034.2
028000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1034.2
028100     02  FILLER          PIC X(28)  VALUE                         SQ1034.2
028200            "  COPYRIGHT   1985,1986 ".                           SQ1034.2
028300 01  CCVS-E-1.                                                    SQ1034.2
028400     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1034.2
028500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1034.2
028600     02 ID-AGAIN         PIC X(9).                                SQ1034.2
028700     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1034.2
028800 01  CCVS-E-2.                                                    SQ1034.2
028900     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1034.2
029000     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1034.2
029100     02  CCVS-E-2-2.                                              SQ1034.2
029200         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1034.2
029300         03 FILLER         PIC X      VALUE SPACE.                SQ1034.2
029400         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1034.2
029500            "ERRORS ENCOUNTERED".                                 SQ1034.2
029600 01  CCVS-E-3.                                                    SQ1034.2
029700     02  FILLER          PIC X(22)  VALUE                         SQ1034.2
029800            " FOR OFFICIAL USE ONLY".                             SQ1034.2
029900     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1034.2
030000     02  FILLER          PIC X(58)  VALUE                         SQ1034.2
030100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1034.2
030200     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1034.2
030300     02  FILLER          PIC X(20)  VALUE                         SQ1034.2
030400             " COPYRIGHT 1985,1986".                              SQ1034.2
030500 01  CCVS-E-4.                                                    SQ1034.2
030600     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1034.2
030700     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1034.2
030800     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1034.2
030900     02 FILLER           PIC X(40)  VALUE                         SQ1034.2
031000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1034.2
031100 01  XXINFO.                                                      SQ1034.2
031200     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1034.2
031300     02 INFO-TEXT.                                                SQ1034.2
031400       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1034.2
031500       04 XXCOMPUTED         PIC X(20).                           SQ1034.2
031600       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1034.2
031700       04 XXCORRECT          PIC X(20).                           SQ1034.2
031800     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1034.2
031900 01  HYPHEN-LINE.                                                 SQ1034.2
032000     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1034.2
032100     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1034.2
032200-    "*****************************************".                 SQ1034.2
032300     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1034.2
032400-    "******************************".                            SQ1034.2
032500 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1034.2
032600     "SQ103A".                                                    SQ1034.2
032700*                                                                 SQ1034.2
032800*                                                                 SQ1034.2
032900 PROCEDURE DIVISION.                                              SQ1034.2
033000 DECLARATIVES.                                                    SQ1034.2
033100 SECT-SQ103-0001 SECTION.                                         SQ1034.2
033200     USE AFTER STANDARD EXCEPTION PROCEDURE ON SQ-FS2.            SQ1034.2
033300 TEST-STATUS.                                                     SQ1034.2
033400     MOVE    ZERO TO DECL-EXEC-SW.                                SQ1034.2
033500     ADD     1    TO DECL-EXEC-COUNT.                             SQ1034.2
033600 END DECLARATIVES.                                                SQ1034.2
033700*                                                                 SQ1034.2
033800 CCVS1 SECTION.                                                   SQ1034.2
033900 OPEN-FILES.                                                      SQ1034.2
034000*    OPEN    I-O RAW-DATA.                                        SQ1034.2
034100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1034.2
034200*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1034.2
034300*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1034.2
034400*    ADD     1           TO C-NO-OF-TESTS.                        SQ1034.2
034500*    ACCEPT  C-DATE      FROM DATE.                               SQ1034.2
034600*    ACCEPT  C-TIME      FROM TIME.                               SQ1034.2
034700*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1034.2
034800*END-E-1.                                                         SQ1034.2
034900*    CLOSE   RAW-DATA.                                            SQ1034.2
035000     OPEN    OUTPUT PRINT-FILE.                                   SQ1034.2
035100     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1034.2
035200     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1034.2
035300     MOVE    SPACE TO TEST-RESULTS.                               SQ1034.2
035400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1034.2
035500     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1034.2
035600     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1034.2
035700     GO TO CCVS1-EXIT.                                            SQ1034.2
035800*                                                                 SQ1034.2
035900 CCVS-INIT-FILE.                                                  SQ1034.2
036000     ADD     1 TO REC-SKL-SUB.                                    SQ1034.2
036100     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1034.2
036200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1034.2
036300*                                                                 SQ1034.2
036400 CLOSE-FILES.                                                     SQ1034.2
036500     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1034.2
036600     CLOSE   PRINT-FILE.                                          SQ1034.2
036700*    OPEN    I-O RAW-DATA.                                        SQ1034.2
036800*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1034.2
036900*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1034.2
037000*    MOVE   "OK.     " TO C-ABORT.                                SQ1034.2
037100*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1034.2
037200*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1034.2
037300*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1034.2
037400*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1034.2
037500*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1034.2
037600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1034.2
037700*END-E-2.                                                         SQ1034.2
037800*    CLOSE   RAW-DATA.                                            SQ1034.2
037900 TERMINATE-CCVS.                                                  SQ1034.2
038000*    EXIT    PROGRAM.                                             SQ1034.2
038100     STOP    RUN.                                                 SQ1034.2
038200*                                                                 SQ1034.2
038300 INSPT.                                                           SQ1034.2
038400     MOVE   "INSPT" TO P-OR-F.                                    SQ1034.2
038500     ADD     1 TO INSPECT-COUNTER.                                SQ1034.2
038600     PERFORM PRINT-DETAIL.                                        SQ1034.2
038700*                                                                 SQ1034.2
038800 PASS.                                                            SQ1034.2
038900     MOVE   "PASS " TO P-OR-F.                                    SQ1034.2
039000     ADD     1 TO PASS-COUNTER.                                   SQ1034.2
039100     PERFORM PRINT-DETAIL.                                        SQ1034.2
039200*                                                                 SQ1034.2
039300 FAIL.                                                            SQ1034.2
039400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1034.2
039500     ADD     1 TO ERROR-COUNTER.                                  SQ1034.2
039600     PERFORM PRINT-DETAIL.                                        SQ1034.2
039700*                                                                 SQ1034.2
039800 DE-LETE.                                                         SQ1034.2
039900     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1034.2
040000     MOVE   "*****" TO P-OR-F.                                    SQ1034.2
040100     ADD     1 TO DELETE-COUNTER.                                 SQ1034.2
040200     PERFORM PRINT-DETAIL.                                        SQ1034.2
040300*                                                                 SQ1034.2
040400 PRINT-DETAIL.                                                    SQ1034.2
040500     IF REC-CT NOT EQUAL TO ZERO                                  SQ1034.2
040600         MOVE   "." TO PARDOT-X                                   SQ1034.2
040700         MOVE    REC-CT TO DOTVALUE.                              SQ1034.2
040800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1034.2
040900     PERFORM WRITE-LINE.                                          SQ1034.2
041000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1034.2
041100         PERFORM WRITE-LINE                                       SQ1034.2
041200         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1034.2
041300     ELSE                                                         SQ1034.2
041400         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1034.2
041500     MOVE    SPACE TO P-OR-F.                                     SQ1034.2
041600     MOVE    SPACE TO COMPUTED-X.                                 SQ1034.2
041700     MOVE    SPACE TO CORRECT-X.                                  SQ1034.2
041800     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1034.2
041900     MOVE    SPACE TO RE-MARK.                                    SQ1034.2
042000*                                                                 SQ1034.2
042100 HEAD-ROUTINE.                                                    SQ1034.2
042200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1034.2
042300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1034.2
042400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1034.2
042500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1034.2
042600 COLUMN-NAMES-ROUTINE.                                            SQ1034.2
042700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1034.2
042800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1034.2
042900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1034.2
043000 END-ROUTINE.                                                     SQ1034.2
043100     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1034.2
043200     PERFORM WRITE-LINE 5 TIMES.                                  SQ1034.2
043300 END-RTN-EXIT.                                                    SQ1034.2
043400     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1034.2
043500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1034.2
043600*                                                                 SQ1034.2
043700 END-ROUTINE-1.                                                   SQ1034.2
043800     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1034.2
043900     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1034.2
044000     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1034.2
044100     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1034.2
044200     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1034.2
044300     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1034.2
044400     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1034.2
044500     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1034.2
044600     PERFORM WRITE-LINE.                                          SQ1034.2
044700     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1034.2
044800     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1034.2
044900         MOVE   "NO " TO ERROR-TOTAL                              SQ1034.2
045000     ELSE                                                         SQ1034.2
045100         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1034.2
045200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1034.2
045300     PERFORM WRITE-LINE.                                          SQ1034.2
045400 END-ROUTINE-13.                                                  SQ1034.2
045500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1034.2
045600         MOVE   "NO " TO ERROR-TOTAL                              SQ1034.2
045700     ELSE                                                         SQ1034.2
045800         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1034.2
045900     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1034.2
046000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1034.2
046100     PERFORM WRITE-LINE.                                          SQ1034.2
046200     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1034.2
046300         MOVE   "NO " TO ERROR-TOTAL                              SQ1034.2
046400     ELSE                                                         SQ1034.2
046500         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1034.2
046600     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1034.2
046700     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1034.2
046800     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1034.2
046900*                                                                 SQ1034.2
047000 WRITE-LINE.                                                      SQ1034.2
047100     ADD     1 TO RECORD-COUNT.                                   SQ1034.2
047200*    IF RECORD-COUNT GREATER 50                                   SQ1034.2
047300*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1034.2
047400*        MOVE  SPACE TO DUMMY-RECORD                              SQ1034.2
047500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1034.2
047600*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1034.2
047700*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1034.2
047800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1034.2
047900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1034.2
048000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1034.2
048100     PERFORM WRT-LN.                                              SQ1034.2
048200*                                                                 SQ1034.2
048300 WRT-LN.                                                          SQ1034.2
048400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1034.2
048500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1034.2
048600 BLANK-LINE-PRINT.                                                SQ1034.2
048700     PERFORM WRT-LN.                                              SQ1034.2
048800 FAIL-ROUTINE.                                                    SQ1034.2
048900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1034.2
049000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1034.2
049100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1034.2
049200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1034.2
049300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1034.2
049400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1034.2
049500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1034.2
049600     GO TO   FAIL-ROUTINE-EX.                                     SQ1034.2
049700 FAIL-ROUTINE-WRITE.                                              SQ1034.2
049800     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1034.2
049900     PERFORM WRITE-LINE                                           SQ1034.2
050000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1034.2
050100     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1034.2
050200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1034.2
050300     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1034.2
050400 FAIL-ROUTINE-EX.                                                 SQ1034.2
050500     EXIT.                                                        SQ1034.2
050600 BAIL-OUT.                                                        SQ1034.2
050700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1034.2
050800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1034.2
050900 BAIL-OUT-WRITE.                                                  SQ1034.2
051000     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1034.2
051100     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1034.2
051200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1034.2
051300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1034.2
051400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1034.2
051500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1034.2
051600 BAIL-OUT-EX.                                                     SQ1034.2
051700     EXIT.                                                        SQ1034.2
051800 CCVS1-EXIT.                                                      SQ1034.2
051900     EXIT.                                                        SQ1034.2
052000*                                                                 SQ1034.2
052100****************************************************************  SQ1034.2
052200*                                                              *  SQ1034.2
052300*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1034.2
052400*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1034.2
052500*                                                              *  SQ1034.2
052600****************************************************************  SQ1034.2
052700*                                                                 SQ1034.2
052800 SECT-SQ103-0002 SECTION.                                         SQ1034.2
052900 INITIAL-PARA.                                                    SQ1034.2
053000     MOVE   "SQ-FS2"     TO XFILE-NAME (1).                       SQ1034.2
053100     MOVE   "R1-F-G"     TO XRECORD-NAME (1).                     SQ1034.2
053200     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1034.2
053300     MOVE    120         TO XRECORD-LENGTH (1).                   SQ1034.2
053400     MOVE   "RC"         TO CHARS-OR-RECORDS (1).                 SQ1034.2
053500     MOVE    0001        TO XBLOCK-SIZE (1).                      SQ1034.2
053600     MOVE    500         TO RECORDS-IN-FILE (1).                  SQ1034.2
053700     MOVE   "SQ"         TO XFILE-ORGANIZATION (1).               SQ1034.2
053800     MOVE   "S"          TO XLABEL-TYPE (1).                      SQ1034.2
053900*                                                                 SQ1034.2
054000 SEQ-INIT-01.                                                     SQ1034.2
054100     MOVE   "SEQ-TEST-GF-01" TO PAR-NAME.                         SQ1034.2
054200     MOVE   "OPEN OUTPUT, TAPE FILE" TO FEATURE.                  SQ1034.2
054300     MOVE    1      TO REC-CT.                                    SQ1034.2
054400     MOVE   "**"    TO SQ-FS2-STATUS.                             SQ1034.2
054500     MOVE    1      TO DECL-EXEC-SW.                              SQ1034.2
054600     MOVE    000001 TO XRECORD-NUMBER (1).                        SQ1034.2
054700     MOVE    ZERO   TO DECL-EXEC-COUNT.                           SQ1034.2
054800     GO TO   SEQ-TEST-GF-01.                                      SQ1034.2
054900 SEQ-DELETE-01.                                                   SQ1034.2
055000     PERFORM DE-LETE.                                             SQ1034.2
055100     ADD     1 TO REC-CT                                          SQ1034.2
055200     PERFORM DE-LETE.                                             SQ1034.2
055300     GO TO   SEQ-DELETE-02.                                       SQ1034.2
055400 SEQ-TEST-GF-01.                                                  SQ1034.2
055500     OPEN OUTPUT SQ-FS2.                                          SQ1034.2
055600     IF DECL-EXEC-SW = 1                                          SQ1034.2
055700         PERFORM PASS                                             SQ1034.2
055800     ELSE                                                         SQ1034.2
055900         MOVE    1 TO CORRECT-18V0                                SQ1034.2
056000         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
056100         MOVE   "UNEXPECTED EXECUTION OF DECLARATIVE" TO RE-MARK  SQ1034.2
056200         PERFORM FAIL.                                            SQ1034.2
056300     ADD     1 TO REC-CT.                                         SQ1034.2
056400     IF SQ-FS2-STATUS EQUAL "00"                                  SQ1034.2
056500         PERFORM PASS                                             SQ1034.2
056600     ELSE                                                         SQ1034.2
056700         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
056800         MOVE   "00" TO CORRECT-A                                 SQ1034.2
056900         MOVE   "UNEXPECTED I-O STATUS FROM OPEN" TO RE-MARK      SQ1034.2
057000         PERFORM FAIL.                                            SQ1034.2
057100*                                                                 SQ1034.2
057200 SEQ-INIT-02.                                                     SQ1034.2
057300     MOVE    ZERO TO DECL-EXEC-COUNT.                             SQ1034.2
057400     MOVE   "**"  TO SQ-FS2-STATUS.                               SQ1034.2
057500     MOVE   "00"  TO SQ-FS2-STATUS-COPY.                          SQ1034.2
057600     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1034.2
057700     GO TO   SEQ-TEST-GF-02.                                      SQ1034.2
057800 SEQ-DELETE-02.                                                   SQ1034.2
057900     MOVE    1 TO REC-CT.                                         SQ1034.2
058000     MOVE   "SEQ-TEST-GF-02"    TO PAR-NAME.                      SQ1034.2
058100     MOVE   "WRITE 500 RECORDS" TO FEATURE.                       SQ1034.2
058200     PERFORM DE-LETE.                                             SQ1034.2
058300     ADD     1 TO REC-CT.                                         SQ1034.2
058400     PERFORM DE-LETE.                                             SQ1034.2
058500     GO TO   SEQ-TEST-02-END.                                     SQ1034.2
058600 SEQ-TEST-GF-02.                                                  SQ1034.2
058700     MOVE    1 TO REC-CT.                                         SQ1034.2
058800     MOVE   "SEQ-TEST-GF-02"    TO PAR-NAME.                      SQ1034.2
058900     MOVE   "WRITE 500 RECORDS" TO FEATURE.                       SQ1034.2
059000 SEQ-TEST-GF-02-LOOP.                                             SQ1034.2
059100     ADD     1 TO XRECORD-NUMBER (1).                             SQ1034.2
059200     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS2R1-F-G-120.        SQ1034.2
059300     WRITE SQ-FS2R1-F-G-120.                                      SQ1034.2
059400     IF SQ-FS2-STATUS NOT = "00"                                  SQ1034.2
059500         MOVE    SQ-FS2-STATUS TO SQ-FS2-STATUS-COPY.             SQ1034.2
059600     IF XRECORD-NUMBER (1) LESS THAN 500                          SQ1034.2
059700           GO TO SEQ-TEST-GF-02-LOOP.                             SQ1034.2
059800*                                                                 SQ1034.2
059900     IF DECL-EXEC-COUNT = ZERO                                    SQ1034.2
060000         PERFORM PASS                                             SQ1034.2
060100     ELSE                                                         SQ1034.2
060200         MOVE    DECL-EXEC-COUNT TO COMPUTED-18V0                 SQ1034.2
060300         MOVE    1 TO CORRECT-18V0                                SQ1034.2
060400         MOVE   "DECLARATIVE ENTERED AT LEAST ONCE" TO RE-MARK    SQ1034.2
060500         PERFORM FAIL.                                            SQ1034.2
060600     ADD     1 TO REC-CT.                                         SQ1034.2
060700     IF SQ-FS2-STATUS-COPY EQUAL TO "00"                          SQ1034.2
060800         PERFORM PASS                                             SQ1034.2
060900     ELSE                                                         SQ1034.2
061000         MOVE    SQ-FS2-STATUS-COPY TO COMPUTED-A                 SQ1034.2
061100         MOVE   "00" TO CORRECT-A                                 SQ1034.2
061200         MOVE   "AT LEAST ONE UNSUCCESSFUL WRITE" TO RE-MARK      SQ1034.2
061300         PERFORM FAIL.                                            SQ1034.2
061400 SEQ-TEST-02-END.                                                 SQ1034.2
061500*                                                                 SQ1034.2
061600 SEQ-INIT-03.                                                     SQ1034.2
061700     MOVE    1 TO REC-CT.                                         SQ1034.2
061800     MOVE   "SEQ-TEST-GF-03" TO PAR-NAME.                         SQ1034.2
061900     MOVE   "CLOSE FILE FROM OUTPUT" TO FEATURE.                  SQ1034.2
062000     MOVE    1 TO DECL-EXEC-SW.                                   SQ1034.2
062100     MOVE   "**" TO SQ-FS2-STATUS.                                SQ1034.2
062200     GO TO   SEQ-TEST-GF-03.                                      SQ1034.2
062300 SEQ-DELETE-03.                                                   SQ1034.2
062400     PERFORM DE-LETE.                                             SQ1034.2
062500     ADD     1 TO REC-CT.                                         SQ1034.2
062600     PERFORM DE-LETE.                                             SQ1034.2
062700     GO TO   SEQ-TEST-03-END.                                     SQ1034.2
062800 SEQ-TEST-GF-03.                                                  SQ1034.2
062900     CLOSE SQ-FS2.                                                SQ1034.2
063000     IF DECL-EXEC-SW = 1                                          SQ1034.2
063100         PERFORM PASS                                             SQ1034.2
063200     ELSE                                                         SQ1034.2
063300         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
063400         MOVE    1 TO CORRECT-18V0                                SQ1034.2
063500         MOVE   "UNEXPECTED EXECUTION OF DECLARATIVE ON CLOSE"    SQ1034.2
063600                    TO RE-MARK                                    SQ1034.2
063700         PERFORM FAIL.                                            SQ1034.2
063800     ADD     1 TO REC-CT.                                         SQ1034.2
063900     IF SQ-FS2-STATUS = "00"                                      SQ1034.2
064000         PERFORM PASS                                             SQ1034.2
064100     ELSE                                                         SQ1034.2
064200         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
064300         MOVE   "00" TO CORRECT-A                                 SQ1034.2
064400         MOVE   "I-O STATUS AFTER CLOSE INDICATES FAILURE"        SQ1034.2
064500                    TO RE-MARK                                    SQ1034.2
064600         PERFORM FAIL.                                            SQ1034.2
064700 SEQ-TEST-03-END.                                                 SQ1034.2
064800*                                                                 SQ1034.2
064900*    A SEQUENTIAL TAPE FILE HAS BEEN CREATED.  IT CONTAINS 500    SQ1034.2
065000*    FIXED-LENGTH RECORDS, EACH 120 CHARACTERS LONG.  THE NEXT    SQ1034.2
065100*    GROUP OF TESTS READS THIS FILE, COUNTING THE RECORDS AND     SQ1034.2
065200*    CHECKING THEIR CONTENT.                                      SQ1034.2
065300*                                                                 SQ1034.2
065400 SEQ-INIT-04.                                                     SQ1034.2
065500     MOVE    1 TO REC-CT.                                         SQ1034.2
065600     MOVE   "SEQ-TEST-GF-04" TO PAR-NAME.                         SQ1034.2
065700     MOVE   "OPEN NEWLY-WRITTEN FILE" TO FEATURE.                 SQ1034.2
065800     MOVE    1 TO DECL-EXEC-SW.                                   SQ1034.2
065900     MOVE   "**" TO SQ-FS2-STATUS.                                SQ1034.2
066000     GO TO   SEQ-TEST-GF-04.                                      SQ1034.2
066100 SEQ-DELETE-04.                                                   SQ1034.2
066200     PERFORM DE-LETE.                                             SQ1034.2
066300     ADD     1 TO REC-CT.                                         SQ1034.2
066400     PERFORM DE-LETE.                                             SQ1034.2
066500     GO TO SEQ-TEST-04-END.                                       SQ1034.2
066600 SEQ-TEST-GF-04.                                                  SQ1034.2
066700     OPEN INPUT SQ-FS2.                                           SQ1034.2
066800     IF SQ-FS2-STATUS = "00"                                      SQ1034.2
066900         PERFORM PASS                                             SQ1034.2
067000     ELSE                                                         SQ1034.2
067100         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
067200         MOVE   "00" TO CORRECT-A                                 SQ1034.2
067300         MOVE   "I-O STATUS INDICATES ABNORMAL OPEN" TO RE-MARK   SQ1034.2
067400         PERFORM FAIL.                                            SQ1034.2
067500     ADD     1 TO REC-CT.                                         SQ1034.2
067600     IF DECL-EXEC-SW = 1                                          SQ1034.2
067700         PERFORM PASS                                             SQ1034.2
067800     ELSE                                                         SQ1034.2
067900         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
068000         MOVE    1 TO CORRECT-18V0                                SQ1034.2
068100         MOVE   "UNEXPECTED EXECUTION OF DECLARATIVE" TO RE-MARK  SQ1034.2
068200         PERFORM FAIL.                                            SQ1034.2
068300 SEQ-TEST-04-END.                                                 SQ1034.2
068400*                                                                 SQ1034.2
068500 SEQ-INIT-05.                                                     SQ1034.2
068600     MOVE    1 TO REC-CT.                                         SQ1034.2
068700     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1034.2
068800     MOVE    ZERO TO DECL-EXEC-COUNT.                             SQ1034.2
068900     MOVE    ZERO TO EOF-FLAG.                                    SQ1034.2
069000     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1034.2
069100     MOVE   "**"  TO SQ-FS2-STATUS.                               SQ1034.2
069200     MOVE   "00"  TO SQ-FS2-STATUS-COPY.                          SQ1034.2
069300     MOVE   "READ 500 RECORD FILE" TO FEATURE.                    SQ1034.2
069400     MOVE   "SEQ-TEST-GF-05" TO PAR-NAME.                         SQ1034.2
069500     GO TO   SEQ-TEST-GF-05.                                      SQ1034.2
069600 SEQ-DELETE-05.                                                   SQ1034.2
069700     PERFORM DE-LETE.                                             SQ1034.2
069800     ADD     1 TO REC-CT.                                         SQ1034.2
069900     PERFORM DE-LETE.                                             SQ1034.2
070000     ADD     1 TO REC-CT.                                         SQ1034.2
070100     PERFORM DE-LETE.                                             SQ1034.2
070200     ADD     1 TO REC-CT.                                         SQ1034.2
070300     PERFORM DE-LETE.                                             SQ1034.2
070400     ADD     1 TO REC-CT.                                         SQ1034.2
070500     PERFORM DE-LETE.                                             SQ1034.2
070600     ADD     1 TO REC-CT.                                         SQ1034.2
070700     PERFORM DE-LETE.                                             SQ1034.2
070800     GO TO   SEQ-TEST-05-END.                                     SQ1034.2
070900 SEQ-TEST-GF-05.                                                  SQ1034.2
071000 SEQ-TEST-GF-05-LOOP.                                             SQ1034.2
071100     READ    SQ-FS2 RECORD END                                    SQ1034.2
071200                 MOVE    1 TO EOF-FLAG                            SQ1034.2
071300                 GO TO SEQ-TEST-GF-05-02.                         SQ1034.2
071400     IF SQ-FS2-STATUS = "10"                                      SQ1034.2
071500         GO TO   SEQ-TEST-GF-05-02.                               SQ1034.2
071600     IF SQ-FS2-STATUS NOT = "00"                                  SQ1034.2
071700         MOVE    SQ-FS2-STATUS TO SQ-FS2-STATUS-COPY.             SQ1034.2
071800     ADD     1 TO XRECORD-NUMBER (1).                             SQ1034.2
071900     IF SQ-FS2R1-F-G-120 NOT EQUAL FILE-RECORD-INFO-P1-120 (1)    SQ1034.2
072000         ADD     1 TO RECORDS-IN-ERROR.                           SQ1034.2
072100     IF XRECORD-NUMBER (1) LESS THAN OR EQUAL TO 500              SQ1034.2
072200         GO TO   SEQ-TEST-GF-05-LOOP.                             SQ1034.2
072300*                                                                 SQ1034.2
072400 SEQ-TEST-GF-05-02.                                               SQ1034.2
072500     IF XRECORD-NUMBER (1) = 500                                  SQ1034.2
072600         PERFORM PASS                                             SQ1034.2
072700     ELSE                                                         SQ1034.2
072800         MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0              SQ1034.2
072900         MOVE    500 TO CORRECT-18V0                              SQ1034.2
073000         MOVE   "UNEXPECTED NUMBER OF RECORDS BEFORE EOF"         SQ1034.2
073100                    TO RE-MARK                                    SQ1034.2
073200         PERFORM FAIL.                                            SQ1034.2
073300     ADD     1 TO REC-CT.                                         SQ1034.2
073400     IF DECL-EXEC-COUNT = ZERO                                    SQ1034.2
073500         PERFORM PASS                                             SQ1034.2
073600     ELSE                                                         SQ1034.2
073700         MOVE    DECL-EXEC-COUNT TO COMPUTED-18V0                 SQ1034.2
073800         MOVE    1 TO CORRECT-18V0                                SQ1034.2
073900         MOVE   "DECLARATIVE ENTERED AT LEAST ONCE" TO RE-MARK    SQ1034.2
074000         PERFORM FAIL.                                            SQ1034.2
074100     ADD     1 TO REC-CT.                                         SQ1034.2
074200     IF SQ-FS2-STATUS-COPY EQUAL TO "00"                          SQ1034.2
074300         PERFORM PASS                                             SQ1034.2
074400     ELSE                                                         SQ1034.2
074500         MOVE    SQ-FS2-STATUS-COPY TO COMPUTED-A                 SQ1034.2
074600         MOVE   "00" TO CORRECT-A                                 SQ1034.2
074700         MOVE   "AT LEAST ONE UNSUCCESSFUL READ" TO RE-MARK       SQ1034.2
074800         PERFORM FAIL.                                            SQ1034.2
074900     ADD     1 TO REC-CT.                                         SQ1034.2
075000     IF EOF-FLAG = 1                                              SQ1034.2
075100         PERFORM PASS                                             SQ1034.2
075200     ELSE                                                         SQ1034.2
075300         MOVE    EOF-FLAG TO COMPUTED-18V0                        SQ1034.2
075400         MOVE    1 TO CORRECT-18V0                                SQ1034.2
075500         MOVE   "AT END STATEMENT NOT EXECUTED" TO RE-MARK        SQ1034.2
075600         MOVE   "VII-46, 4.4.4(10)C" TO ANSI-REFERENCE            SQ1034.2
075700         PERFORM FAIL.                                            SQ1034.2
075800     ADD     1 TO REC-CT.                                         SQ1034.2
075900     IF SQ-FS2-STATUS EQUAL TO "10"                               SQ1034.2
076000         PERFORM PASS                                             SQ1034.2
076100     ELSE                                                         SQ1034.2
076200         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
076300         MOVE   "10" TO CORRECT-A                                 SQ1034.2
076400         MOVE   "UNEXPECTED I-O STATUS FROM FINAL READ"           SQ1034.2
076500                    TO RE-MARK                                    SQ1034.2
076600         MOVE   "VII-46, 4.4.4(10)A, VII-3" TO ANSI-REFERENCE     SQ1034.2
076700         PERFORM FAIL.                                            SQ1034.2
076800     ADD     1 TO REC-CT.                                         SQ1034.2
076900     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1034.2
077000         PERFORM PASS                                             SQ1034.2
077100     ELSE                                                         SQ1034.2
077200         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1034.2
077300         MOVE    ZERO TO CORRECT-18V0                             SQ1034.2
077400         MOVE   "INCORRECT RECORD CONTENTS FOUND" TO RE-MARK      SQ1034.2
077500         PERFORM FAIL.                                            SQ1034.2
077600 SEQ-TEST-05-END.                                                 SQ1034.2
077700*                                                                 SQ1034.2
077800 SEQ-INIT-06.                                                     SQ1034.2
077900     MOVE   "SEQ-TEST-GF-06" TO PAR-NAME.                         SQ1034.2
078000     MOVE   "CLOSE FILE FROM INPUT" TO FEATURE.                   SQ1034.2
078100     MOVE    1 TO REC-CT.                                         SQ1034.2
078200     MOVE    1 TO DECL-EXEC-SW.                                   SQ1034.2
078300     MOVE   "**" TO SQ-FS2-STATUS.                                SQ1034.2
078400     GO TO   SEQ-TEST-GF-06.                                      SQ1034.2
078500 SEQ-DELETE-06.                                                   SQ1034.2
078600     PERFORM DE-LETE.                                             SQ1034.2
078700     ADD     1 TO REC-CT.                                         SQ1034.2
078800     PERFORM DE-LETE.                                             SQ1034.2
078900     GO TO   SEQ-TEST-06-END.                                     SQ1034.2
079000 SEQ-TEST-GF-06.                                                  SQ1034.2
079100     CLOSE SQ-FS2.                                                SQ1034.2
079200     IF DECL-EXEC-SW = 1                                          SQ1034.2
079300         PERFORM PASS                                             SQ1034.2
079400     ELSE                                                         SQ1034.2
079500         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
079600         MOVE    1 TO CORRECT-18V0                                SQ1034.2
079700         MOVE   "UNEXPECTED EXECUTION OF DECLARATIVE ON CLOSE"    SQ1034.2
079800                    TO RE-MARK                                    SQ1034.2
079900         PERFORM FAIL.                                            SQ1034.2
080000     ADD     1 TO REC-CT.                                         SQ1034.2
080100     IF SQ-FS2-STATUS = "00"                                      SQ1034.2
080200         PERFORM PASS                                             SQ1034.2
080300     ELSE                                                         SQ1034.2
080400         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
080500         MOVE   "00" TO CORRECT-A                                 SQ1034.2
080600         MOVE   "I-O STATUS AFTER CLOSE INDICATES FAILURE"        SQ1034.2
080700                    TO RE-MARK                                    SQ1034.2
080800         PERFORM FAIL.                                            SQ1034.2
080900 SEQ-TEST-06-END.                                                 SQ1034.2
081000*                                                                 SQ1034.2
081100*                                                                 SQ1034.2
081200*    TWO OPTIONS FOR THE READ STATEMENT ARE CHECKED IN THIS       SQ1034.2
081300*    SERIES OF TESTS, THE ABSENCE OF ALL OPTIONAL PHRASES, AND    SQ1034.2
081400*    THE ABSENCE OF ALL EXCEPT THE OPTIONAL WORD "RECORD".        SQ1034.2
081500*                                                                 SQ1034.2
081600 SEQ-INIT-07.                                                     SQ1034.2
081700     MOVE    1 TO REC-CT.                                         SQ1034.2
081800     MOVE   "SEQ-TEST-GF-07" TO PAR-NAME.                         SQ1034.2
081900     MOVE   "OPEN FILE FOR INPUT" TO FEATURE.                     SQ1034.2
082000     MOVE    1 TO DECL-EXEC-SW.                                   SQ1034.2
082100     MOVE   "**" TO SQ-FS2-STATUS.                                SQ1034.2
082200     GO TO   SEQ-TEST-GF-07.                                      SQ1034.2
082300 SEQ-DELETE-07.                                                   SQ1034.2
082400     PERFORM DE-LETE.                                             SQ1034.2
082500     ADD     1 TO REC-CT.                                         SQ1034.2
082600     PERFORM DE-LETE.                                             SQ1034.2
082700     GO TO SEQ-TEST-07-END.                                       SQ1034.2
082800 SEQ-TEST-GF-07.                                                  SQ1034.2
082900     OPEN INPUT SQ-FS2.                                           SQ1034.2
083000     IF SQ-FS2-STATUS = "00"                                      SQ1034.2
083100         PERFORM PASS                                             SQ1034.2
083200     ELSE                                                         SQ1034.2
083300         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
083400         MOVE   "00" TO CORRECT-A                                 SQ1034.2
083500         MOVE   "I-O STATUS INDICATES ABNORMAL OPEN" TO RE-MARK   SQ1034.2
083600         PERFORM FAIL.                                            SQ1034.2
083700     ADD     1 TO REC-CT.                                         SQ1034.2
083800     IF DECL-EXEC-SW = 1                                          SQ1034.2
083900         PERFORM PASS                                             SQ1034.2
084000     ELSE                                                         SQ1034.2
084100         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
084200         MOVE    1 TO CORRECT-18V0                                SQ1034.2
084300         MOVE   "UNEXPECTED EXECUTION OF DECLARATIVE" TO RE-MARK  SQ1034.2
084400         PERFORM FAIL.                                            SQ1034.2
084500 SEQ-TEST-07-END.                                                 SQ1034.2
084600*                                                                 SQ1034.2
084700 SEQ-INIT-08.                                                     SQ1034.2
084800     MOVE    1 TO REC-CT.                                         SQ1034.2
084900     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1034.2
085000     MOVE    ZERO TO XRECORD-NUMBER (1).                          SQ1034.2
085100     MOVE    ZERO TO DECL-EXEC-COUNT.                             SQ1034.2
085200     MOVE    ZERO TO EOF-FLAG.                                    SQ1034.2
085300     MOVE   "**"  TO SQ-FS2-STATUS.                               SQ1034.2
085400     MOVE   "00"  TO SQ-FS2-STATUS-COPY.                          SQ1034.2
085500     MOVE   "READ ... RECORD" TO FEATURE.                         SQ1034.2
085600     MOVE   "SEQ-TEST-GF-08" TO PAR-NAME.                         SQ1034.2
085700     GO TO   SEQ-TEST-GF-08.                                      SQ1034.2
085800 SEQ-DELETE-08.                                                   SQ1034.2
085900     PERFORM DE-LETE.                                             SQ1034.2
086000     ADD     1 TO REC-CT.                                         SQ1034.2
086100     PERFORM DE-LETE.                                             SQ1034.2
086200     ADD     1 TO REC-CT.                                         SQ1034.2
086300     PERFORM DE-LETE.                                             SQ1034.2
086400     ADD     1 TO REC-CT.                                         SQ1034.2
086500     PERFORM DE-LETE.                                             SQ1034.2
086600     GO TO   SEQ-TEST-08-END.                                     SQ1034.2
086700 SEQ-TEST-GF-08.                                                  SQ1034.2
086800 SEQ-TEST-GF-08-LOOP.                                             SQ1034.2
086900     READ    SQ-FS2 RECORD.                                       SQ1034.2
087000     IF SQ-FS2-STATUS = "10"                                      SQ1034.2
087100         MOVE    1 TO EOF-FLAG                                    SQ1034.2
087200         GO TO   SEQ-TEST-GF-08-02.                               SQ1034.2
087300     IF SQ-FS2-STATUS NOT = "00"                                  SQ1034.2
087400         MOVE    SQ-FS2-STATUS TO SQ-FS2-STATUS-COPY.             SQ1034.2
087500     ADD     1 TO XRECORD-NUMBER (1).                             SQ1034.2
087600     IF SQ-FS2R1-F-G-120 NOT EQUAL FILE-RECORD-INFO-P1-120 (1)    SQ1034.2
087700         ADD     1 TO RECORDS-IN-ERROR.                           SQ1034.2
087800     IF XRECORD-NUMBER (1) LESS THAN 100                          SQ1034.2
087900         GO TO   SEQ-TEST-GF-08-LOOP.                             SQ1034.2
088000*                                                                 SQ1034.2
088100 SEQ-TEST-GF-08-02.                                               SQ1034.2
088200     IF XRECORD-NUMBER (1) = 100                                  SQ1034.2
088300         PERFORM PASS                                             SQ1034.2
088400     ELSE                                                         SQ1034.2
088500         MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0              SQ1034.2
088600         MOVE    100 TO CORRECT-18V0                              SQ1034.2
088700         MOVE   "UNEXPECTED NUMBER OF RECORDS BEFORE EOF"         SQ1034.2
088800                    TO RE-MARK                                    SQ1034.2
088900         PERFORM FAIL.                                            SQ1034.2
089000     ADD     1 TO REC-CT.                                         SQ1034.2
089100     IF DECL-EXEC-COUNT = ZERO                                    SQ1034.2
089200         PERFORM PASS                                             SQ1034.2
089300     ELSE                                                         SQ1034.2
089400         MOVE    DECL-EXEC-COUNT TO COMPUTED-18V0                 SQ1034.2
089500         MOVE    1 TO CORRECT-18V0                                SQ1034.2
089600         MOVE   "DECLARATIVE ENTERED AT LEAST ONCE" TO RE-MARK    SQ1034.2
089700         PERFORM FAIL.                                            SQ1034.2
089800     ADD     1 TO REC-CT.                                         SQ1034.2
089900     IF SQ-FS2-STATUS-COPY EQUAL TO "00"                          SQ1034.2
090000         PERFORM PASS                                             SQ1034.2
090100     ELSE                                                         SQ1034.2
090200         MOVE    SQ-FS2-STATUS-COPY TO COMPUTED-A                 SQ1034.2
090300         MOVE   "00" TO CORRECT-A                                 SQ1034.2
090400         MOVE   "AT LEAST ONE UNSUCCESSFUL READ" TO RE-MARK       SQ1034.2
090500         PERFORM FAIL.                                            SQ1034.2
090600     ADD     1 TO REC-CT.                                         SQ1034.2
090700     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1034.2
090800         PERFORM PASS                                             SQ1034.2
090900     ELSE                                                         SQ1034.2
091000         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1034.2
091100         MOVE    ZERO TO CORRECT-18V0                             SQ1034.2
091200         MOVE   "INCORRECT RECORD CONTENTS FOUND" TO RE-MARK      SQ1034.2
091300         PERFORM FAIL.                                            SQ1034.2
091400 SEQ-TEST-08-END.                                                 SQ1034.2
091500*                                                                 SQ1034.2
091600 SEQ-INIT-09.                                                     SQ1034.2
091700     MOVE    1 TO REC-CT.                                         SQ1034.2
091800     MOVE    ZERO TO DECL-EXEC-COUNT.                             SQ1034.2
091900     MOVE   "**"  TO SQ-FS2-STATUS.                               SQ1034.2
092000     MOVE   "00"  TO SQ-FS2-STATUS-COPY.                          SQ1034.2
092100     MOVE   "READ ..." TO FEATURE.                                SQ1034.2
092200     MOVE   "SEQ-TEST-GF-09" TO PAR-NAME.                         SQ1034.2
092300     GO TO   SEQ-TEST-GF-09.                                      SQ1034.2
092400 SEQ-DELETE-09.                                                   SQ1034.2
092500     PERFORM DE-LETE.                                             SQ1034.2
092600     ADD     1 TO REC-CT.                                         SQ1034.2
092700     PERFORM DE-LETE.                                             SQ1034.2
092800     ADD     1 TO REC-CT.                                         SQ1034.2
092900     PERFORM DE-LETE.                                             SQ1034.2
093000     ADD     1 TO REC-CT.                                         SQ1034.2
093100     PERFORM DE-LETE.                                             SQ1034.2
093200     GO TO   SEQ-TEST-09-END.                                     SQ1034.2
093300 SEQ-TEST-GF-09.                                                  SQ1034.2
093400     IF EOF-FLAG = 1                                              SQ1034.2
093500         GO TO   SEQ-TEST-GF-09-02.                               SQ1034.2
093600 SEQ-TEST-GF-09-LOOP.                                             SQ1034.2
093700     READ    SQ-FS2.                                              SQ1034.2
093800     IF SQ-FS2-STATUS = "10"                                      SQ1034.2
093900         MOVE    1 TO EOF-FLAG                                    SQ1034.2
094000         GO TO   SEQ-TEST-GF-09-02.                               SQ1034.2
094100     IF SQ-FS2-STATUS NOT = "00"                                  SQ1034.2
094200         MOVE    SQ-FS2-STATUS TO SQ-FS2-STATUS-COPY.             SQ1034.2
094300     ADD     1 TO XRECORD-NUMBER (1).                             SQ1034.2
094400     IF SQ-FS2R1-F-G-120 NOT EQUAL FILE-RECORD-INFO-P1-120 (1)    SQ1034.2
094500         ADD     1 TO RECORDS-IN-ERROR.                           SQ1034.2
094600     IF XRECORD-NUMBER (1) LESS THAN OR EQUAL TO 499              SQ1034.2
094700         GO TO   SEQ-TEST-GF-09-LOOP.                             SQ1034.2
094800*                                                                 SQ1034.2
094900 SEQ-TEST-GF-09-02.                                               SQ1034.2
095000     IF XRECORD-NUMBER (1) = 500                                  SQ1034.2
095100         PERFORM PASS                                             SQ1034.2
095200     ELSE                                                         SQ1034.2
095300         MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0              SQ1034.2
095400         MOVE    500 TO CORRECT-18V0                              SQ1034.2
095500         MOVE   "UNEXPECTED NUMBER OF RECORDS BEFORE EOF"         SQ1034.2
095600                    TO RE-MARK                                    SQ1034.2
095700         PERFORM FAIL.                                            SQ1034.2
095800     ADD     1 TO REC-CT.                                         SQ1034.2
095900     IF DECL-EXEC-COUNT = ZERO                                    SQ1034.2
096000         PERFORM PASS                                             SQ1034.2
096100     ELSE                                                         SQ1034.2
096200         MOVE    DECL-EXEC-COUNT TO COMPUTED-18V0                 SQ1034.2
096300         MOVE    1 TO CORRECT-18V0                                SQ1034.2
096400         MOVE   "DECLARATIVE ENTERED AT LEAST ONCE" TO RE-MARK    SQ1034.2
096500         PERFORM FAIL.                                            SQ1034.2
096600     ADD     1 TO REC-CT.                                         SQ1034.2
096700     IF SQ-FS2-STATUS-COPY EQUAL TO "00"                          SQ1034.2
096800         PERFORM PASS                                             SQ1034.2
096900     ELSE                                                         SQ1034.2
097000         MOVE    SQ-FS2-STATUS-COPY TO COMPUTED-A                 SQ1034.2
097100         MOVE   "00" TO CORRECT-A                                 SQ1034.2
097200         MOVE   "AT LEAST ONE UNSUCCESSFUL READ" TO RE-MARK       SQ1034.2
097300         PERFORM FAIL.                                            SQ1034.2
097400     ADD     1 TO REC-CT.                                         SQ1034.2
097500     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1034.2
097600         PERFORM PASS                                             SQ1034.2
097700     ELSE                                                         SQ1034.2
097800         MOVE    RECORDS-IN-ERROR TO COMPUTED-18V0                SQ1034.2
097900         MOVE    ZERO TO CORRECT-18V0                             SQ1034.2
098000         MOVE   "INCORRECT RECORD CONTENTS FOUND" TO RE-MARK      SQ1034.2
098100         PERFORM FAIL.                                            SQ1034.2
098200 SEQ-TEST-09-END.                                                 SQ1034.2
098300*                                                                 SQ1034.2
098400 SEQ-INIT-10.                                                     SQ1034.2
098500     MOVE    1 TO REC-CT.                                         SQ1034.2
098600     MOVE    1 TO DECL-EXEC-SW.                                   SQ1034.2
098700     MOVE   "**" TO SQ-FS2-STATUS.                                SQ1034.2
098800     MOVE   "SEQ-TEST-GF-10" TO PAR-NAME.                         SQ1034.2
098900     MOVE   "READ ... RAISING AT END" TO FEATURE.                 SQ1034.2
099000     IF EOF-FLAG NOT EQUAL TO ZERO                                SQ1034.2
099100         GO TO   SEQ-DELETE-10.                                   SQ1034.2
099200     GO TO   SEQ-TEST-GF-10.                                      SQ1034.2
099300 SEQ-DELETE-10.                                                   SQ1034.2
099400     PERFORM DE-LETE.                                             SQ1034.2
099500     ADD     1 TO REC-CT.                                         SQ1034.2
099600     PERFORM DE-LETE.                                             SQ1034.2
099700     GO TO   SEQ-TEST-10-END.                                     SQ1034.2
099800 SEQ-TEST-GF-10.                                                  SQ1034.2
099900     READ    SQ-FS2.                                              SQ1034.2
100000     IF DECL-EXEC-SW = 0                                          SQ1034.2
100100         PERFORM PASS                                             SQ1034.2
100200     ELSE                                                         SQ1034.2
100300         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
100400         MOVE    ZERO TO CORRECT-18V0                             SQ1034.2
100500         MOVE   "DECLARATIVE NOT EXECUTED" TO RE-MARK             SQ1034.2
100600         MOVE   "VII-46, 4.4.4(10)C" TO ANSI-REFERENCE            SQ1034.2
100700         PERFORM FAIL.                                            SQ1034.2
100800     ADD     1 TO REC-CT.                                         SQ1034.2
100900     IF SQ-FS2-STATUS = "10"                                      SQ1034.2
101000         PERFORM PASS                                             SQ1034.2
101100     ELSE                                                         SQ1034.2
101200         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
101300         MOVE   "10" TO CORRECT-A                                 SQ1034.2
101400         MOVE   "I-O STATUS FOR END OF FILE EXPECTED" TO RE-MARK  SQ1034.2
101500         PERFORM FAIL.                                            SQ1034.2
101600 SEQ-TEST-10-END.                                                 SQ1034.2
101700*                                                                 SQ1034.2
101800 SEQ-INIT-11.                                                     SQ1034.2
101900     MOVE   "SEQ-TEST-GF-11" TO PAR-NAME.                         SQ1034.2
102000     MOVE   "CLOSE FILE FROM INPUT" TO FEATURE.                   SQ1034.2
102100     MOVE    1 TO REC-CT.                                         SQ1034.2
102200     MOVE    1 TO DECL-EXEC-SW.                                   SQ1034.2
102300     MOVE   "**" TO SQ-FS2-STATUS.                                SQ1034.2
102400     GO TO   SEQ-TEST-GF-11.                                      SQ1034.2
102500 SEQ-DELETE-11.                                                   SQ1034.2
102600     PERFORM DE-LETE.                                             SQ1034.2
102700     ADD     1 TO REC-CT.                                         SQ1034.2
102800     PERFORM DE-LETE.                                             SQ1034.2
102900     GO TO   SEQ-TEST-11-END.                                     SQ1034.2
103000 SEQ-TEST-GF-11.                                                  SQ1034.2
103100     CLOSE SQ-FS2.                                                SQ1034.2
103200     IF DECL-EXEC-SW = 1                                          SQ1034.2
103300         PERFORM PASS                                             SQ1034.2
103400     ELSE                                                         SQ1034.2
103500         MOVE    DECL-EXEC-SW TO COMPUTED-18V0                    SQ1034.2
103600         MOVE    1 TO CORRECT-18V0                                SQ1034.2
103700         MOVE   "UNEXPECTED EXECUTION OF DECLARATIVE ON CLOSE"    SQ1034.2
103800                    TO RE-MARK                                    SQ1034.2
103900         PERFORM FAIL.                                            SQ1034.2
104000     ADD     1 TO REC-CT.                                         SQ1034.2
104100     IF SQ-FS2-STATUS = "00"                                      SQ1034.2
104200         PERFORM PASS                                             SQ1034.2
104300     ELSE                                                         SQ1034.2
104400         MOVE    SQ-FS2-STATUS TO COMPUTED-A                      SQ1034.2
104500         MOVE   "00" TO CORRECT-A                                 SQ1034.2
104600         MOVE   "I-O STATUS AFTER CLOSE INDICATES FAILURE"        SQ1034.2
104700                    TO RE-MARK                                    SQ1034.2
104800         PERFORM FAIL.                                            SQ1034.2
104900 SEQ-TEST-11-END.                                                 SQ1034.2
105000*                                                                 SQ1034.2
105100 TERMINATE-ROUTINE.                                               SQ1034.2
105200     EXIT.                                                        SQ1034.2
105300 CCVS-EXIT SECTION.                                               SQ1034.2
105400 CCVS-999999.                                                     SQ1034.2
105500     GO TO CLOSE-FILES.                                           SQ1034.2
      *END-OF,SQ103A                                                                  
