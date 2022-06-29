      *HEADER,COBOL,SQ106A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1064.2
000200 PROGRAM-ID.                                                      SQ1064.2
000300     SQ106A.                                                      SQ1064.2
000400****************************************************************  SQ1064.2
000500*                                                              *  SQ1064.2
000600*    VALIDATION FOR:-                                          *  SQ1064.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1064.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1064.2
000900*                             REVISED 1986, AUGUST             *  SQ1064.2
001000*                                                              *  SQ1064.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1064.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1064.2
001300*                                                              *  SQ1064.2
001400****************************************************************  SQ1064.2
001500*                                                              *  SQ1064.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1064.2
001700*                                                              *  SQ1064.2
001800*            X-01   SEQUENTIAL MAGNETIC TAPE FILE              *  SQ1064.2
001900*            X-55   SYSTEM PRINTER                             *  SQ1064.2
002000*            X-82   SOURCE-COMPUTER                            *  SQ1064.2
002100*            X-83   OBJECT-COMPUTER.                           *  SQ1064.2
002200*                                                              *  SQ1064.2
002300****************************************************************  SQ1064.2
002400                                                                  SQ1064.2
002500*    THIS PROGRAM BUILDS A SEQUENTIAL TAPE FILE WHICH CONTAINS *  SQ1064.2
002600*    BOTH 120 CHARACTER AND 151 CHARACTER RECORDS.             *  SQ1064.2
002700*                                                              *  SQ1064.2
002800*    THE SEQUENCE OF RECORD TYPES IS SLSSLSLLLSS, WHERE S      *  SQ1064.2
002900*    INDICATES 120 CHARACTERS AND L INDICATES 151 CHARACTERS,  *  SQ1064.2
003000*    FOR A TOTAL OF 11 RECORDS IN THE FILE.  SIX OF THE        *  SQ1064.2
003100*    RECORDS ARE WRITEN USING WRITE FROM, THE OTHERS USING A   *  SQ1064.2
003200*    WRITE STATEMENT WITHOUT THE FROM PHRASE.  THE FILE IS     *  SQ1064.2
003300*    THEN CLOSED AND REOPENED FOR INPUT.  IT IS READ USING     *  SQ1064.2
003400*    TWELVE DIFFERENT FORMATS OF THE READ STATEMENT.  THE      *  SQ1064.2
003500*    VARIANTS ARE PRODUCED BY INCLUDING OR OMITTING THE NOT AT *  SQ1064.2
003600*    END AND END-READ PHRASES, AND INCLUDING OR EXCLUDING THE  *  SQ1064.2
003700*    OPTIONAL WORD "AT" IN THE END AND NOT END PHRASES.        *  SQ1064.2
003800*    FIELDS IN EACH RECORD ARE CHECKED AGAINST THE EXPECTED    *  SQ1064.2
003900*    VALUES.                                                   *  SQ1064.2
004000*                                                              *  SQ1064.2
004100*    WHERE A SHORT RECORD IS EXPECTED, A CHECK IS MADE THAT    *  SQ1064.2
004200*    THE RECORD AREA DOES NOT CONTAIN THE VALUES THAT WERE     *  SQ1064.2
004300*    PRESENT IN THAT PART OF THE RECORD AREA BEYOND THE RECORD *  SQ1064.2
004400*    WHEN IT WAS WRITTEN.  THIS ASSUMPTION IS NOT FULLY        *  SQ1064.2
004500*    JUSTIFIED, AS THE CONTENT OF THE RECORD AREA BEYOND THE   *  SQ1064.2
004600*    END OF THE RECORD WHEN A SHORT RECORD IS READ IS          *  SQ1064.2
004700*    UNDEFINED, BUT IT IS UNLIKELY THAT THE VALUES TESTED FOR  *  SQ1064.2
004800*    WOULD OCCUR BY CHANCE.                                    *  SQ1064.2
004900*                                                              *  SQ1064.2
005000****************************************************************  SQ1064.2
005100*                                                                 SQ1064.2
005200 ENVIRONMENT DIVISION.                                            SQ1064.2
005300 CONFIGURATION SECTION.                                           SQ1064.2
005400 SOURCE-COMPUTER.                                                 SQ1064.2
005500     XXXXX082.                                                    SQ1064.2
005600 OBJECT-COMPUTER.                                                 SQ1064.2
005700     XXXXX083.                                                    SQ1064.2
005800 INPUT-OUTPUT SECTION.                                            SQ1064.2
005900 FILE-CONTROL.                                                    SQ1064.2
006000*    SELECT RAW-DATA   ASSIGN TO                                  SQ1064.2
006100*    XXXXX062                                                     SQ1064.2
006200*           ORGANIZATION IS INDEXED                               SQ1064.2
006300*           ACCESS MODE IS RANDOM                                 SQ1064.2
006400*           RECORD KEY IS RAW-DATA-KEY.                           SQ1064.2
006500*                                                                 SQ1064.2
006600     SELECT PRINT-FILE ASSIGN TO                                  SQ1064.2
006700     XXXXX055.                                                    SQ1064.2
006800*                                                                 SQ1064.2
006900     SELECT SQ-VS6 ASSIGN                                         SQ1064.2
007000     XXXXX001                                                     SQ1064.2
007100     STATUS SQ-STATUS                                             SQ1064.2
007200     ORGANIZATION IS SEQUENTIAL.                                  SQ1064.2
007300*                                                                 SQ1064.2
007400*                                                                 SQ1064.2
007500 DATA DIVISION.                                                   SQ1064.2
007600 FILE SECTION.                                                    SQ1064.2
007700*                                                                 SQ1064.2
007800*FD  RAW-DATA.                                                    SQ1064.2
007900*                                                                 SQ1064.2
008000*01  RAW-DATA-SATZ.                                               SQ1064.2
008100*    05  RAW-DATA-KEY        PIC X(6).                            SQ1064.2
008200*    05  C-DATE              PIC 9(6).                            SQ1064.2
008300*    05  C-TIME              PIC 9(8).                            SQ1064.2
008400*    05  C-NO-OF-TESTS       PIC 99.                              SQ1064.2
008500*    05  C-OK                PIC 999.                             SQ1064.2
008600*    05  C-ALL               PIC 999.                             SQ1064.2
008700*    05  C-FAIL              PIC 999.                             SQ1064.2
008800*    05  C-DELETED           PIC 999.                             SQ1064.2
008900*    05  C-INSPECT           PIC 999.                             SQ1064.2
009000*    05  C-NOTE              PIC X(13).                           SQ1064.2
009100*    05  C-INDENT            PIC X.                               SQ1064.2
009200*    05  C-ABORT             PIC X(8).                            SQ1064.2
009300*                                                                 SQ1064.2
009400 FD  PRINT-FILE                                                   SQ1064.2
009500C    LABEL RECORDS                                                SQ1064.2
009600C    XXXXX084                                                     SQ1064.2
009700C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1064.2
009800               .                                                  SQ1064.2
009900 01  PRINT-REC PICTURE X(120).                                    SQ1064.2
010000 01  DUMMY-RECORD PICTURE X(120).                                 SQ1064.2
010100*                                                                 SQ1064.2
010200*                                                                 SQ1064.2
010300 FD  SQ-VS6                                                       SQ1064.2
010400C    LABEL RECORDS ARE STANDARD                                   SQ1064.2
010500C    DATA RECORDS ARE SQ-VS6R1-M-G-120  SQ-VS6R2-M-G-151          SQ1064.2
010600     RECORD CONTAINS 120 TO 151 CHARACTERS.                       SQ1064.2
010700*                                                                 SQ1064.2
010800 01  SQ-VS6R1-M-G-120.                                            SQ1064.2
010900     02  SQ-VS6R1-FIRST PIC X(120).                               SQ1064.2
011000*                                                                 SQ1064.2
011100 01  SQ-VS6R2-M-G-151.                                            SQ1064.2
011200     02  SQ-VS6R2-FIRST  PIC X(120).                              SQ1064.2
011300     02  SQ-VS6R2-SECOND.                                         SQ1064.2
011400         05  SQ-VS6R2-SECOND-L.                                   SQ1064.2
011500             07  LONG-OR-SHORT   PIC X(5).                        SQ1064.2
011600             07  SQ-VS6-RECNO    PIC X(5).                        SQ1064.2
011700         05  SQ-VS6R2-SECOND-R.                                   SQ1064.2
011800             07  SQ-VS6-FILLER   PIC X(21).                       SQ1064.2
011900*                                                                 SQ1064.2
012000*                                                                 SQ1064.2
012100 WORKING-STORAGE SECTION.                                         SQ1064.2
012200*                                                                 SQ1064.2
012300***************************************************************   SQ1064.2
012400*                                                             *   SQ1064.2
012500*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1064.2
012600*                                                             *   SQ1064.2
012700***************************************************************   SQ1064.2
012800*                                                                 SQ1064.2
012900 01  SQ-STATUS.                                                   SQ1064.2
013000   03  SQ-STATUS-1 PIC X.                                         SQ1064.2
013100   03  SQ-STATUS-2 PIC X.                                         SQ1064.2
013200*                                                                 SQ1064.2
013300 01  BUFFER-COPY.                                                 SQ1064.2
013400   03  BUFFER-COPY-120    PIC X(120).                             SQ1064.2
013500   03  BUFFER-COPY-SECOND.                                        SQ1064.2
013600       05  BUFFER-COPY-SECOND-L.                                  SQ1064.2
013700           07  BUFFER-COPY-L-OR-S PIC X(5).                       SQ1064.2
013800           07  BUFFER-COPY-RECNO  PIC 9(5).                       SQ1064.2
013900       05  BUFFER-COPY-SECOND-R.                                  SQ1064.2
014000           07  BUFFER-COPY-END    PIC X(21).                      SQ1064.2
014100*                                                                 SQ1064.2
014200 01  EOF-FLAG      PIC X(12).                                     SQ1064.2
014300 01  NOT-EOF-FLAG  PIC X(12).                                     SQ1064.2
014400 01  END-READ-FLAG PIC X(12).                                     SQ1064.2
014500*                                                                 SQ1064.2
014600 01  DELETE-SW.                                                   SQ1064.2
014700   03  DELETE-SW-1 PIC X.                                         SQ1064.2
014800   03  DELETE-SW-1-GROUP.                                         SQ1064.2
014900     05  DELETE-SW-2 PIC X.                                       SQ1064.2
015000     05  DELETE-SW-2-GROUP.                                       SQ1064.2
015100         07  DELETE-SW-3 PIC X.                                   SQ1064.2
015200*                                                                 SQ1064.2
015300***************************************************************   SQ1064.2
015400*                                                             *   SQ1064.2
015500*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1064.2
015600*                                                             *   SQ1064.2
015700***************************************************************   SQ1064.2
015800*                                                                 SQ1064.2
015900 01  REC-SKEL-SUB   PIC 99.                                       SQ1064.2
016000*                                                                 SQ1064.2
016100 01  FILE-RECORD-INFORMATION-REC.                                 SQ1064.2
016200     03 FILE-RECORD-INFO-SKELETON.                                SQ1064.2
016300        05 FILLER                 PICTURE X(48)       VALUE       SQ1064.2
016400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1064.2
016500        05 FILLER                 PICTURE X(46)       VALUE       SQ1064.2
016600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1064.2
016700        05 FILLER                 PICTURE X(26)       VALUE       SQ1064.2
016800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1064.2
016900        05 FILLER                 PICTURE X(37)       VALUE       SQ1064.2
017000             ",RECKEY=                             ".             SQ1064.2
017100        05 FILLER                 PICTURE X(38)       VALUE       SQ1064.2
017200             ",ALTKEY1=                             ".            SQ1064.2
017300        05 FILLER                 PICTURE X(38)       VALUE       SQ1064.2
017400             ",ALTKEY2=                             ".            SQ1064.2
017500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1064.2
017600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1064.2
017700        05 FILE-RECORD-INFO-P1-120.                               SQ1064.2
017800           07 FILLER              PIC X(5).                       SQ1064.2
017900           07 XFILE-NAME          PIC X(6).                       SQ1064.2
018000           07 FILLER              PIC X(8).                       SQ1064.2
018100           07 XRECORD-NAME        PIC X(6).                       SQ1064.2
018200           07 FILLER              PIC X(1).                       SQ1064.2
018300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1064.2
018400           07 FILLER              PIC X(7).                       SQ1064.2
018500           07 XRECORD-NUMBER      PIC 9(6).                       SQ1064.2
018600           07 FILLER              PIC X(6).                       SQ1064.2
018700           07 UPDATE-NUMBER       PIC 9(2).                       SQ1064.2
018800           07 FILLER              PIC X(5).                       SQ1064.2
018900           07 ODO-NUMBER          PIC 9(4).                       SQ1064.2
019000           07 FILLER              PIC X(5).                       SQ1064.2
019100           07 XPROGRAM-NAME       PIC X(5).                       SQ1064.2
019200           07 FILLER              PIC X(7).                       SQ1064.2
019300           07 XRECORD-LENGTH      PIC 9(6).                       SQ1064.2
019400           07 FILLER              PIC X(7).                       SQ1064.2
019500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1064.2
019600           07 FILLER              PIC X(1).                       SQ1064.2
019700           07 XBLOCK-SIZE         PIC 9(4).                       SQ1064.2
019800           07 FILLER              PIC X(6).                       SQ1064.2
019900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1064.2
020000           07 FILLER              PIC X(5).                       SQ1064.2
020100           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1064.2
020200           07 FILLER              PIC X(6).                       SQ1064.2
020300           07 XLABEL-TYPE         PIC X(1).                       SQ1064.2
020400        05 FILE-RECORD-INFO-P121-240.                             SQ1064.2
020500           07 FILLER              PIC X(8).                       SQ1064.2
020600           07 XRECORD-KEY         PIC X(29).                      SQ1064.2
020700           07 FILLER              PIC X(9).                       SQ1064.2
020800           07 ALTERNATE-KEY1      PIC X(29).                      SQ1064.2
020900           07 FILLER              PIC X(9).                       SQ1064.2
021000           07 ALTERNATE-KEY2      PIC X(29).                      SQ1064.2
021100           07 FILLER              PIC X(7).                       SQ1064.2
021200*                                                                 SQ1064.2
021300 01  TEST-RESULTS.                                                SQ1064.2
021400     02 FILLER              PIC X      VALUE SPACE.               SQ1064.2
021500     02  PAR-NAME.                                                SQ1064.2
021600       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1064.2
021700       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1064.2
021800       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1064.2
021900     02 FILLER              PIC X      VALUE SPACE.               SQ1064.2
022000     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1064.2
022100     02 FILLER              PIC X      VALUE SPACE.               SQ1064.2
022200     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1064.2
022300     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1064.2
022400     02 RE-MARK             PIC X(61).                            SQ1064.2
022500 01  TEST-COMPUTED.                                               SQ1064.2
022600   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1064.2
022700   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1064.2
022800   02 COMPUTED-X.                                                 SQ1064.2
022900     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1064.2
023000     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1064.2
023100     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1064.2
023200     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1064.2
023300     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1064.2
023400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1064.2
023500        04 COMPUTED-18V0                   PIC -9(18).            SQ1064.2
023600        04 FILLER                          PIC X.                 SQ1064.2
023700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1064.2
023800 01  TEST-CORRECT.                                                SQ1064.2
023900     02 FILLER PIC X(30) VALUE SPACE.                             SQ1064.2
024000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1064.2
024100     02 CORRECT-X.                                                SQ1064.2
024200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1064.2
024300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1064.2
024400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1064.2
024500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1064.2
024600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1064.2
024700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1064.2
024800         04 CORRECT-18V0                     PIC -9(18).          SQ1064.2
024900         04 FILLER                           PIC X.               SQ1064.2
025000     03 FILLER PIC X(2) VALUE SPACE.                              SQ1064.2
025100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1064.2
025200*                                                                 SQ1064.2
025300 01  CCVS-C-1.                                                    SQ1064.2
025400     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1064.2
025500     02 FILLER  PIC IS X(17)    VALUE "PARAGRAPH-NAME".           SQ1064.2
025600     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1064.2
025700     02 FILLER  PIC IS X(24)    VALUE IS "FEATURE".               SQ1064.2
025800     02 FILLER  PIC IS X        VALUE  SPACE.                     SQ1064.2
025900     02 FILLER  PIC IS X(5)     VALUE "PASS ".                    SQ1064.2
026000     02 FILLER  PIC IS X(9)     VALUE  SPACE.                     SQ1064.2
026100     02 FILLER  PIC IS X(62)    VALUE "REMARKS".                  SQ1064.2
026200 01  CCVS-C-2.                                                    SQ1064.2
026300     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1064.2
026400     02 FILLER  PIC X(6)   VALUE "TESTED".                        SQ1064.2
026500     02 FILLER  PIC X(19)  VALUE  SPACE.                          SQ1064.2
026600     02 FILLER  PIC X(4)   VALUE "FAIL".                          SQ1064.2
026700     02 FILLER  PIC X(72)  VALUE  SPACE.                          SQ1064.2
026800*                                                                 SQ1064.2
026900 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1064.2
027000 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1064.2
027100 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1064.2
027200 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1064.2
027300 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1064.2
027400 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1064.2
027500 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1064.2
027600 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1064.2
027700 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1064.2
027800 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1064.2
027900 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1064.2
028000 01  CCVS-H-1.                                                    SQ1064.2
028100     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1064.2
028200     02  FILLER          PIC X(42)    VALUE                       SQ1064.2
028300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1064.2
028400     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1064.2
028500 01  CCVS-H-2A.                                                   SQ1064.2
028600   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1064.2
028700   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1064.2
028800   02  FILLER            PIC XXXX   VALUE                         SQ1064.2
028900     "4.2 ".                                                      SQ1064.2
029000   02  FILLER            PIC X(28)  VALUE                         SQ1064.2
029100            " COPY - NOT FOR DISTRIBUTION".                       SQ1064.2
029200   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1064.2
029300*                                                                 SQ1064.2
029400 01  CCVS-H-2B.                                                   SQ1064.2
029500   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1064.2
029600   02  TEST-ID           PIC X(9).                                SQ1064.2
029700   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1064.2
029800   02  FILLER            PIC X(12)  VALUE                         SQ1064.2
029900     " HIGH       ".                                              SQ1064.2
030000   02  FILLER            PIC X(22)  VALUE                         SQ1064.2
030100            " LEVEL VALIDATION FOR ".                             SQ1064.2
030200   02  FILLER            PIC X(58)  VALUE                         SQ1064.2
030300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1064.2
030400 01  CCVS-H-3.                                                    SQ1064.2
030500     02  FILLER          PIC X(34)  VALUE                         SQ1064.2
030600            " FOR OFFICIAL USE ONLY    ".                         SQ1064.2
030700     02  FILLER          PIC X(58)  VALUE                         SQ1064.2
030800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1064.2
030900     02  FILLER          PIC X(28)  VALUE                         SQ1064.2
031000            "  COPYRIGHT   1985,1986 ".                           SQ1064.2
031100 01  CCVS-E-1.                                                    SQ1064.2
031200     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1064.2
031300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1064.2
031400     02 ID-AGAIN         PIC X(9).                                SQ1064.2
031500     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1064.2
031600 01  CCVS-E-2.                                                    SQ1064.2
031700     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1064.2
031800     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1064.2
031900     02  CCVS-E-2-2.                                              SQ1064.2
032000         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1064.2
032100         03 FILLER         PIC X      VALUE SPACE.                SQ1064.2
032200         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1064.2
032300            "ERRORS ENCOUNTERED".                                 SQ1064.2
032400 01  CCVS-E-3.                                                    SQ1064.2
032500     02  FILLER          PIC X(22)  VALUE                         SQ1064.2
032600            " FOR OFFICIAL USE ONLY".                             SQ1064.2
032700     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1064.2
032800     02  FILLER          PIC X(58)  VALUE                         SQ1064.2
032900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1064.2
033000     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1064.2
033100     02  FILLER          PIC X(20)  VALUE                         SQ1064.2
033200             " COPYRIGHT 1985,1986".                              SQ1064.2
033300 01  CCVS-E-4.                                                    SQ1064.2
033400     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1064.2
033500     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1064.2
033600     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1064.2
033700     02 FILLER           PIC X(40)  VALUE                         SQ1064.2
033800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1064.2
033900 01  XXINFO.                                                      SQ1064.2
034000     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1064.2
034100     02 INFO-TEXT.                                                SQ1064.2
034200       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1064.2
034300       04 XXCOMPUTED         PIC X(20).                           SQ1064.2
034400       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1064.2
034500       04 XXCORRECT          PIC X(20).                           SQ1064.2
034600     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1064.2
034700 01  HYPHEN-LINE.                                                 SQ1064.2
034800     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1064.2
034900     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1064.2
035000-    "*****************************************".                 SQ1064.2
035100     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1064.2
035200-    "******************************".                            SQ1064.2
035300 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1064.2
035400     "SQ106A".                                                    SQ1064.2
035500*                                                                 SQ1064.2
035600*                                                                 SQ1064.2
035700 PROCEDURE DIVISION.                                              SQ1064.2
035800 CCVS1 SECTION.                                                   SQ1064.2
035900 OPEN-FILES.                                                      SQ1064.2
036000*    OPEN    I-O RAW-DATA.                                        SQ1064.2
036100*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1064.2
036200*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1064.2
036300*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1064.2
036400*    ADD     1           TO C-NO-OF-TESTS.                        SQ1064.2
036500*    ACCEPT  C-DATE      FROM DATE.                               SQ1064.2
036600*    ACCEPT  C-TIME      FROM TIME.                               SQ1064.2
036700*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1064.2
036800*END-E-1.                                                         SQ1064.2
036900*    CLOSE   RAW-DATA.                                            SQ1064.2
037000     OPEN    OUTPUT PRINT-FILE.                                   SQ1064.2
037100     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1064.2
037200     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1064.2
037300     MOVE    SPACE TO TEST-RESULTS.                               SQ1064.2
037400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1064.2
037500     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1064.2
037600     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1064.2
037700     GO TO CCVS1-EXIT.                                            SQ1064.2
037800*                                                                 SQ1064.2
037900 CCVS-INIT-FILE.                                                  SQ1064.2
038000     ADD     1 TO REC-SKL-SUB.                                    SQ1064.2
038100     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1064.2
038200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1064.2
038300*                                                                 SQ1064.2
038400 CLOSE-FILES.                                                     SQ1064.2
038500     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1064.2
038600     CLOSE   PRINT-FILE.                                          SQ1064.2
038700*    OPEN    I-O RAW-DATA.                                        SQ1064.2
038800*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1064.2
038900*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1064.2
039000*    MOVE   "OK.     " TO C-ABORT.                                SQ1064.2
039100*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1064.2
039200*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1064.2
039300*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1064.2
039400*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1064.2
039500*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1064.2
039600*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1064.2
039700*END-E-2.                                                         SQ1064.2
039800*    CLOSE   RAW-DATA.                                            SQ1064.2
039900 TERMINATE-CCVS.                                                  SQ1064.2
040000*    EXIT    PROGRAM.                                             SQ1064.2
040100     STOP    RUN.                                                 SQ1064.2
040200*                                                                 SQ1064.2
040300 INSPT.                                                           SQ1064.2
040400     MOVE   "INSPT" TO P-OR-F.                                    SQ1064.2
040500     ADD     1 TO INSPECT-COUNTER.                                SQ1064.2
040600     PERFORM PRINT-DETAIL.                                        SQ1064.2
040700*                                                                 SQ1064.2
040800 PASS.                                                            SQ1064.2
040900     MOVE   "PASS " TO P-OR-F.                                    SQ1064.2
041000     ADD     1 TO PASS-COUNTER.                                   SQ1064.2
041100     PERFORM PRINT-DETAIL.                                        SQ1064.2
041200*                                                                 SQ1064.2
041300 FAIL.                                                            SQ1064.2
041400     MOVE   "FAIL*" TO P-OR-F.                                    SQ1064.2
041500     ADD     1 TO ERROR-COUNTER.                                  SQ1064.2
041600     PERFORM PRINT-DETAIL.                                        SQ1064.2
041700*                                                                 SQ1064.2
041800 DE-LETE.                                                         SQ1064.2
041900     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1064.2
042000     MOVE   "*****" TO P-OR-F.                                    SQ1064.2
042100     ADD     1 TO DELETE-COUNTER.                                 SQ1064.2
042200     PERFORM PRINT-DETAIL.                                        SQ1064.2
042300*                                                                 SQ1064.2
042400 PRINT-DETAIL.                                                    SQ1064.2
042500     IF REC-CT NOT EQUAL TO ZERO                                  SQ1064.2
042600         MOVE   "." TO PARDOT-X                                   SQ1064.2
042700         MOVE    REC-CT TO DOTVALUE.                              SQ1064.2
042800     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1064.2
042900     PERFORM WRITE-LINE.                                          SQ1064.2
043000     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1064.2
043100         PERFORM WRITE-LINE                                       SQ1064.2
043200         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1064.2
043300     ELSE                                                         SQ1064.2
043400         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1064.2
043500     MOVE    SPACE TO P-OR-F.                                     SQ1064.2
043600     MOVE    SPACE TO COMPUTED-X.                                 SQ1064.2
043700     MOVE    SPACE TO CORRECT-X.                                  SQ1064.2
043800     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1064.2
043900     MOVE    SPACE TO RE-MARK.                                    SQ1064.2
044000*                                                                 SQ1064.2
044100 HEAD-ROUTINE.                                                    SQ1064.2
044200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1064.2
044300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1064.2
044400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1064.2
044500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1064.2
044600 COLUMN-NAMES-ROUTINE.                                            SQ1064.2
044700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1064.2
044800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1064.2
044900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1064.2
045000 END-ROUTINE.                                                     SQ1064.2
045100     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1064.2
045200     PERFORM WRITE-LINE 5 TIMES.                                  SQ1064.2
045300 END-RTN-EXIT.                                                    SQ1064.2
045400     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1064.2
045500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1064.2
045600*                                                                 SQ1064.2
045700 END-ROUTINE-1.                                                   SQ1064.2
045800     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1064.2
045900     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1064.2
046000     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1064.2
046100     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1064.2
046200     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1064.2
046300     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1064.2
046400     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1064.2
046500     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1064.2
046600     PERFORM WRITE-LINE.                                          SQ1064.2
046700     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1064.2
046800     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1064.2
046900         MOVE   "NO " TO ERROR-TOTAL                              SQ1064.2
047000     ELSE                                                         SQ1064.2
047100         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1064.2
047200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1064.2
047300     PERFORM WRITE-LINE.                                          SQ1064.2
047400 END-ROUTINE-13.                                                  SQ1064.2
047500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1064.2
047600         MOVE   "NO " TO ERROR-TOTAL                              SQ1064.2
047700     ELSE                                                         SQ1064.2
047800         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1064.2
047900     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1064.2
048000     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1064.2
048100     PERFORM WRITE-LINE.                                          SQ1064.2
048200     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1064.2
048300         MOVE   "NO " TO ERROR-TOTAL                              SQ1064.2
048400     ELSE                                                         SQ1064.2
048500         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1064.2
048600     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1064.2
048700     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1064.2
048800     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1064.2
048900*                                                                 SQ1064.2
049000 WRITE-LINE.                                                      SQ1064.2
049100     ADD     1 TO RECORD-COUNT.                                   SQ1064.2
049200*    IF RECORD-COUNT GREATER 50                                   SQ1064.2
049300*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1064.2
049400*        MOVE  SPACE TO DUMMY-RECORD                              SQ1064.2
049500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1064.2
049600*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1064.2
049700*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1064.2
049800*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1064.2
049900*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1064.2
050000*        MOVE  ZERO TO RECORD-COUNT.                              SQ1064.2
050100     PERFORM WRT-LN.                                              SQ1064.2
050200*                                                                 SQ1064.2
050300 WRT-LN.                                                          SQ1064.2
050400     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1064.2
050500     MOVE    SPACE TO DUMMY-RECORD.                               SQ1064.2
050600 BLANK-LINE-PRINT.                                                SQ1064.2
050700     PERFORM WRT-LN.                                              SQ1064.2
050800 FAIL-ROUTINE.                                                    SQ1064.2
050900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1064.2
051000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1064.2
051100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1064.2
051200     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1064.2
051300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1064.2
051400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1064.2
051500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1064.2
051600     GO TO   FAIL-ROUTINE-EX.                                     SQ1064.2
051700 FAIL-ROUTINE-WRITE.                                              SQ1064.2
051800     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1064.2
051900     PERFORM WRITE-LINE                                           SQ1064.2
052000     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1064.2
052100     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1064.2
052200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1064.2
052300     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1064.2
052400 FAIL-ROUTINE-EX.                                                 SQ1064.2
052500     EXIT.                                                        SQ1064.2
052600 BAIL-OUT.                                                        SQ1064.2
052700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1064.2
052800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1064.2
052900 BAIL-OUT-WRITE.                                                  SQ1064.2
053000     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1064.2
053100     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1064.2
053200     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1064.2
053300     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1064.2
053400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1064.2
053500     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1064.2
053600 BAIL-OUT-EX.                                                     SQ1064.2
053700     EXIT.                                                        SQ1064.2
053800 CCVS1-EXIT.                                                      SQ1064.2
053900     EXIT.                                                        SQ1064.2
054000*                                                                 SQ1064.2
054100****************************************************************  SQ1064.2
054200*                                                              *  SQ1064.2
054300*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1064.2
054400*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1064.2
054500*                                                              *  SQ1064.2
054600****************************************************************  SQ1064.2
054700*                                                                 SQ1064.2
054800 SECT-SQ106-0001 SECTION.                                         SQ1064.2
054900 SEQ-INIT-01.                                                     SQ1064.2
055000     MOVE    SPACE TO DELETE-SW.                                  SQ1064.2
055100*                                                                 SQ1064.2
055200     MOVE   "SQ-VS6" TO XFILE-NAME (1).                           SQ1064.2
055300     MOVE    CCVS-PGM-ID TO XPROGRAM-NAME (1).                    SQ1064.2
055400     MOVE   "RC"  TO CHARS-OR-RECORDS (1).                        SQ1064.2
055500     MOVE    0001 TO XBLOCK-SIZE (1).                             SQ1064.2
055600     MOVE    11   TO RECORDS-IN-FILE (1).                         SQ1064.2
055700     MOVE   "SQ"  TO XFILE-ORGANIZATION (1).                      SQ1064.2
055800     MOVE   "S"   TO XLABEL-TYPE (1).                             SQ1064.2
055900     MOVE    0    TO XRECORD-NUMBER (1).                          SQ1064.2
056000     MOVE    ZERO TO BUFFER-COPY-RECNO.                           SQ1064.2
056100     MOVE   "MULTIPLE REC LENGTHS" TO BUFFER-COPY-END.            SQ1064.2
056200*                                                                 SQ1064.2
056300*    THE FIRST ACTION IS TO OPEN THE FILE FOR OUTPUT, AND SO      SQ1064.2
056400*    CREATE IT.  IF THE OPEN IS DELETED, ALL SUCCEDING TESTS      SQ1064.2
056500*    ARE AUTOMATICALLY DELETED WITH IT.  A SUBORDINATE TEST       SQ1064.2
056600*    CHECKS THE I-O STATUS RETURNED FROM THE OPEN OPERATION.      SQ1064.2
056700*                                                                 SQ1064.2
056800     MOVE   "**"  TO SQ-STATUS.                                   SQ1064.2
056900     MOVE   "OPEN FILE FOR OUTPUT" TO FEATURE.                    SQ1064.2
057000     MOVE   "SEQ-TEST-GF-01" TO PAR-NAME.                         SQ1064.2
057100     GO TO   SEQ-TEST-GF-01.                                      SQ1064.2
057200 SEQ-DELETE-01.                                                   SQ1064.2
057300     MOVE   "*" TO DELETE-SW-1.                                   SQ1064.2
057400     GO TO   SEQ-DELETE-01-01.                                    SQ1064.2
057500 SEQ-TEST-GF-01.                                                  SQ1064.2
057600     OPEN OUTPUT SQ-VS6.                                          SQ1064.2
057700     GO TO   SEQ-TEST-GF-01-01.                                   SQ1064.2
057800 SEQ-DELETE-01-01.                                                SQ1064.2
057900     PERFORM DE-LETE.                                             SQ1064.2
058000     GO TO   SEQ-TEST-01-01-END.                                  SQ1064.2
058100 SEQ-TEST-GF-01-01.                                               SQ1064.2
058200     IF SQ-STATUS = "00"                                          SQ1064.2
058300         PERFORM PASS                                             SQ1064.2
058400     ELSE                                                         SQ1064.2
058500         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
058600         MOVE   "00" TO CORRECT-A                                 SQ1064.2
058700         MOVE   "UNEXPECTED I-O STATUS FROM OPEN" TO RE-MARK      SQ1064.2
058800         MOVE   "VII-2, VII-39" TO ANSI-REFERENCE                 SQ1064.2
058900         PERFORM FAIL                                             SQ1064.2
059000         MOVE   "*" TO DELETE-SW-1.                               SQ1064.2
059100 SEQ-TEST-01-01-END.                                              SQ1064.2
059200*                                                                 SQ1064.2
059300*                                                                 SQ1064.2
059400*    UNLESS AN ERROR OCCURRED DURING EXECUTION OF THE OPEN        SQ1064.2
059500*    STATEMENT, THE FILE IS NOW OPEN, AND READY FOR RECORDS TO    SQ1064.2
059600*    BE WRITTEN TO IT.  IF AN ERROR I-O STATUS VALUE WAS          SQ1064.2
059700*    RETURNED, ALL THE REMAINING TESTS ARE DELETED.               SQ1064.2
059800*                                                                 SQ1064.2
059900*    CREATE A SHORT RECORD USING THE BASIC WRITE STATEMENT        SQ1064.2
060000*                                                                 SQ1064.2
060100 SEQ-INIT-02.                                                     SQ1064.2
060200     MOVE    1 TO REC-CT.                                         SQ1064.2
060300     MOVE   "WRITE SHORT RECORD" TO FEATURE.                      SQ1064.2
060400     MOVE   "SEQ-TEST-WR-02" TO PAR-NAME.                         SQ1064.2
060500     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
060600     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
060700     MOVE   "R1-M-G" TO XRECORD-NAME (1).                         SQ1064.2
060800     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1064.2
060900     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
061000     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
061100     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
061200         GO TO SEQ-DELETE-02.                                     SQ1064.2
061300     GO TO SEQ-TEST-WR-02.                                        SQ1064.2
061400 SEQ-DELETE-02.                                                   SQ1064.2
061500     GO TO   SEQ-DELETE-02-01.                                    SQ1064.2
061600 SEQ-TEST-WR-02.                                                  SQ1064.2
061700     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R2-FIRST.       SQ1064.2
061800     MOVE    BUFFER-COPY-SECOND TO SQ-VS6R2-SECOND.               SQ1064.2
061900     WRITE   SQ-VS6R1-M-G-120.                                    SQ1064.2
062000     GO TO   SEQ-TEST-WR-02-01.                                   SQ1064.2
062100 SEQ-DELETE-02-01.                                                SQ1064.2
062200     PERFORM DE-LETE.                                             SQ1064.2
062300     GO TO   SEQ-TEST-02-01-END.                                  SQ1064.2
062400 SEQ-TEST-WR-02-01.                                               SQ1064.2
062500     IF SQ-STATUS = "00"                                          SQ1064.2
062600         PERFORM PASS                                             SQ1064.2
062700     ELSE                                                         SQ1064.2
062800         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
062900         MOVE   "00" TO CORRECT-A                                 SQ1064.2
063000         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
063100         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
063200         PERFORM FAIL.                                            SQ1064.2
063300 SEQ-TEST-02-01-END.                                              SQ1064.2
063400*                                                                 SQ1064.2
063500*    CREATE A LONG RECORD USING THE BASIC WRITE STATEMENT         SQ1064.2
063600*                                                                 SQ1064.2
063700 SEQ-INIT-03.                                                     SQ1064.2
063800     MOVE    1 TO REC-CT.                                         SQ1064.2
063900     MOVE   "WRITE LONG RECORD" TO FEATURE.                       SQ1064.2
064000     MOVE   "SEQ-TEST-WR-03" TO PAR-NAME.                         SQ1064.2
064100     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
064200     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
064300     MOVE   "R2-M-G" TO XRECORD-NAME (1).                         SQ1064.2
064400     MOVE    151 TO XRECORD-LENGTH (1).                           SQ1064.2
064500     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
064600     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
064700     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
064800         GO TO SEQ-DELETE-03.                                     SQ1064.2
064900     GO TO SEQ-TEST-WR-03.                                        SQ1064.2
065000 SEQ-DELETE-03.                                                   SQ1064.2
065100     GO TO   SEQ-DELETE-03-01.                                    SQ1064.2
065200 SEQ-TEST-WR-03.                                                  SQ1064.2
065300     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R2-FIRST.       SQ1064.2
065400     MOVE    BUFFER-COPY-SECOND TO SQ-VS6R2-SECOND.               SQ1064.2
065500     WRITE   SQ-VS6R2-M-G-151.                                    SQ1064.2
065600     GO TO   SEQ-TEST-WR-03-01.                                   SQ1064.2
065700 SEQ-DELETE-03-01.                                                SQ1064.2
065800     PERFORM DE-LETE.                                             SQ1064.2
065900     GO TO   SEQ-TEST-03-01-END.                                  SQ1064.2
066000 SEQ-TEST-WR-03-01.                                               SQ1064.2
066100     IF SQ-STATUS = "00"                                          SQ1064.2
066200         PERFORM PASS                                             SQ1064.2
066300     ELSE                                                         SQ1064.2
066400         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
066500         MOVE   "00" TO CORRECT-A                                 SQ1064.2
066600         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
066700         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
066800         PERFORM FAIL.                                            SQ1064.2
066900 SEQ-TEST-03-01-END.                                              SQ1064.2
067000*                                                                 SQ1064.2
067100*                                                                 SQ1064.2
067200*    CREATE A SHORT RECORD USING WRITE FROM, WITH A 151           SQ1064.2
067300*    CHARACTER AREA AS THE SOURCE.                                SQ1064.2
067400*                                                                 SQ1064.2
067500 SEQ-INIT-04.                                                     SQ1064.2
067600     MOVE    1 TO REC-CT.                                         SQ1064.2
067700     MOVE   "WRITE SHORT RECORD FROM" TO FEATURE.                 SQ1064.2
067800     MOVE   "SEQ-TEST-WR-04" TO PAR-NAME.                         SQ1064.2
067900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
068000     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
068100     MOVE   "R1-M-G" TO XRECORD-NAME (1).                         SQ1064.2
068200     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1064.2
068300     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
068400     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
068500     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
068600         GO TO SEQ-DELETE-04.                                     SQ1064.2
068700     GO TO SEQ-TEST-WR-04.                                        SQ1064.2
068800 SEQ-DELETE-04.                                                   SQ1064.2
068900     GO TO   SEQ-DELETE-04-01.                                    SQ1064.2
069000 SEQ-TEST-WR-04.                                                  SQ1064.2
069100     MOVE    FILE-RECORD-INFO-P1-120 (1) TO BUFFER-COPY-120.      SQ1064.2
069200     WRITE   SQ-VS6R1-M-G-120 FROM BUFFER-COPY.                   SQ1064.2
069300     GO TO   SEQ-TEST-WR-04-01.                                   SQ1064.2
069400 SEQ-DELETE-04-01.                                                SQ1064.2
069500     PERFORM DE-LETE.                                             SQ1064.2
069600     GO TO   SEQ-TEST-04-01-END.                                  SQ1064.2
069700 SEQ-TEST-WR-04-01.                                               SQ1064.2
069800     IF SQ-STATUS = "00"                                          SQ1064.2
069900         PERFORM PASS                                             SQ1064.2
070000     ELSE                                                         SQ1064.2
070100         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
070200         MOVE   "00" TO CORRECT-A                                 SQ1064.2
070300         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
070400         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
070500         PERFORM FAIL.                                            SQ1064.2
070600 SEQ-TEST-04-01-END.                                              SQ1064.2
070700*                                                                 SQ1064.2
070800*                                                                 SQ1064.2
070900*    CREATE A SHORT RECORD USING WRITE FROM, WITH A 151           SQ1064.2
071000*    CHARACTER SOURCE FIELD.                                      SQ1064.2
071100*                                                                 SQ1064.2
071200 SEQ-INIT-05.                                                     SQ1064.2
071300     MOVE    1 TO REC-CT.                                         SQ1064.2
071400     MOVE   "WRITE SHORT RECORD FROM" TO FEATURE.                 SQ1064.2
071500     MOVE   "SEQ-TEST-WR-05" TO PAR-NAME.                         SQ1064.2
071600     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
071700     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
071800     MOVE   "R1-M-G" TO XRECORD-NAME (1).                         SQ1064.2
071900     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1064.2
072000     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
072100     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
072200     IF DELETE-SW-1 NOT EQUAL SPACE                               SQ1064.2
072300         GO TO SEQ-DELETE-05.                                     SQ1064.2
072400     GO TO SEQ-TEST-WR-05.                                        SQ1064.2
072500 SEQ-DELETE-05.                                                   SQ1064.2
072600     GO TO   SEQ-DELETE-05-01.                                    SQ1064.2
072700 SEQ-TEST-WR-05.                                                  SQ1064.2
072800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO BUFFER-COPY-120.      SQ1064.2
072900     WRITE   SQ-VS6R1-M-G-120 FROM BUFFER-COPY.                   SQ1064.2
073000     GO TO   SEQ-TEST-WR-05-01.                                   SQ1064.2
073100 SEQ-DELETE-05-01.                                                SQ1064.2
073200     PERFORM DE-LETE.                                             SQ1064.2
073300     GO TO   SEQ-TEST-05-01-END.                                  SQ1064.2
073400 SEQ-TEST-WR-05-01.                                               SQ1064.2
073500     IF SQ-STATUS = "00"                                          SQ1064.2
073600         PERFORM PASS                                             SQ1064.2
073700     ELSE                                                         SQ1064.2
073800         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
073900         MOVE   "00" TO CORRECT-A                                 SQ1064.2
074000         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
074100         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
074200         PERFORM FAIL.                                            SQ1064.2
074300 SEQ-TEST-05-01-END.                                              SQ1064.2
074400*                                                                 SQ1064.2
074500*                                                                 SQ1064.2
074600*    CREATE A LONG RECORD USING WRITE FROM.                       SQ1064.2
074700*                                                                 SQ1064.2
074800 SEQ-INIT-06.                                                     SQ1064.2
074900     MOVE    1 TO REC-CT.                                         SQ1064.2
075000     MOVE   "WRITE LONG RECORD FROM" TO FEATURE.                  SQ1064.2
075100     MOVE   "SEQ-TEST-WR-06" TO PAR-NAME.                         SQ1064.2
075200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
075300     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
075400     MOVE   "R2-M-G" TO XRECORD-NAME (1).                         SQ1064.2
075500     MOVE    151 TO XRECORD-LENGTH (1).                           SQ1064.2
075600     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
075700     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
075800     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
075900         GO TO SEQ-DELETE-06.                                     SQ1064.2
076000     GO TO SEQ-TEST-WR-06.                                        SQ1064.2
076100 SEQ-DELETE-06.                                                   SQ1064.2
076200     GO TO   SEQ-DELETE-06-01.                                    SQ1064.2
076300 SEQ-TEST-WR-06.                                                  SQ1064.2
076400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO BUFFER-COPY-120.      SQ1064.2
076500     WRITE   SQ-VS6R2-M-G-151 FROM BUFFER-COPY.                   SQ1064.2
076600     GO TO   SEQ-TEST-WR-06-01.                                   SQ1064.2
076700 SEQ-DELETE-06-01.                                                SQ1064.2
076800     PERFORM DE-LETE.                                             SQ1064.2
076900     GO TO   SEQ-TEST-06-01-END.                                  SQ1064.2
077000 SEQ-TEST-WR-06-01.                                               SQ1064.2
077100     IF SQ-STATUS = "00"                                          SQ1064.2
077200         PERFORM PASS                                             SQ1064.2
077300     ELSE                                                         SQ1064.2
077400         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
077500         MOVE   "00" TO CORRECT-A                                 SQ1064.2
077600         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
077700         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
077800         PERFORM FAIL.                                            SQ1064.2
077900 SEQ-TEST-06-01-END.                                              SQ1064.2
078000*                                                                 SQ1064.2
078100*                                                                 SQ1064.2
078200*    CREATE A SHORT RECORD USING WRITE FROM, WITH A 151           SQ1064.2
078300*    CHARACTER AREA AS THE SOURCE.                                SQ1064.2
078400*                                                                 SQ1064.2
078500 SEQ-INIT-07.                                                     SQ1064.2
078600     MOVE    1 TO REC-CT.                                         SQ1064.2
078700     MOVE   "WRITE SHORT RECORD FROM" TO FEATURE.                 SQ1064.2
078800     MOVE   "SEQ-TEST-WR-07" TO PAR-NAME.                         SQ1064.2
078900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
079000     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
079100     MOVE   "R1-M-G" TO XRECORD-NAME (1).                         SQ1064.2
079200     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1064.2
079300     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
079400     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
079500     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
079600         GO TO SEQ-DELETE-07.                                     SQ1064.2
079700     GO TO SEQ-TEST-WR-07.                                        SQ1064.2
079800 SEQ-DELETE-07.                                                   SQ1064.2
079900     GO TO   SEQ-DELETE-07-01.                                    SQ1064.2
080000 SEQ-TEST-WR-07.                                                  SQ1064.2
080100     MOVE    FILE-RECORD-INFO-P1-120 (1) TO BUFFER-COPY-120.      SQ1064.2
080200     WRITE   SQ-VS6R1-M-G-120 FROM BUFFER-COPY.                   SQ1064.2
080300     GO TO   SEQ-TEST-WR-07-01.                                   SQ1064.2
080400 SEQ-DELETE-07-01.                                                SQ1064.2
080500     PERFORM DE-LETE.                                             SQ1064.2
080600     GO TO   SEQ-TEST-07-01-END.                                  SQ1064.2
080700 SEQ-TEST-WR-07-01.                                               SQ1064.2
080800     IF SQ-STATUS = "00"                                          SQ1064.2
080900         PERFORM PASS                                             SQ1064.2
081000     ELSE                                                         SQ1064.2
081100         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
081200         MOVE   "00" TO CORRECT-A                                 SQ1064.2
081300         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
081400         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
081500         PERFORM FAIL.                                            SQ1064.2
081600 SEQ-TEST-07-01-END.                                              SQ1064.2
081700*                                                                 SQ1064.2
081800*                                                                 SQ1064.2
081900*    CREATE A LONG RECORD USING WRITE FROM, USING A 151           SQ1064.2
082000*    CHARACTER SOURCE AREA.                                       SQ1064.2
082100*                                                                 SQ1064.2
082200 SEQ-INIT-08.                                                     SQ1064.2
082300     MOVE    1 TO REC-CT.                                         SQ1064.2
082400     MOVE   "WRITE LONG RECORD FROM" TO FEATURE.                  SQ1064.2
082500     MOVE   "SEQ-TEST-WR-08" TO PAR-NAME.                         SQ1064.2
082600     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
082700     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
082800     MOVE   "R2-M-G" TO XRECORD-NAME (1).                         SQ1064.2
082900     MOVE    151 TO XRECORD-LENGTH (1).                           SQ1064.2
083000     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
083100     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
083200     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
083300         GO TO SEQ-DELETE-08.                                     SQ1064.2
083400     GO TO SEQ-TEST-WR-08.                                        SQ1064.2
083500 SEQ-DELETE-08.                                                   SQ1064.2
083600     GO TO   SEQ-DELETE-08-01.                                    SQ1064.2
083700 SEQ-TEST-WR-08.                                                  SQ1064.2
083800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO BUFFER-COPY-120.      SQ1064.2
083900     WRITE   SQ-VS6R2-M-G-151 FROM BUFFER-COPY.                   SQ1064.2
084000     GO TO   SEQ-TEST-WR-08-01.                                   SQ1064.2
084100 SEQ-DELETE-08-01.                                                SQ1064.2
084200     PERFORM DE-LETE.                                             SQ1064.2
084300     GO TO   SEQ-TEST-08-01-END.                                  SQ1064.2
084400 SEQ-TEST-WR-08-01.                                               SQ1064.2
084500     IF SQ-STATUS = "00"                                          SQ1064.2
084600         PERFORM PASS                                             SQ1064.2
084700     ELSE                                                         SQ1064.2
084800         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
084900         MOVE   "00" TO CORRECT-A                                 SQ1064.2
085000         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
085100         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
085200         PERFORM FAIL.                                            SQ1064.2
085300 SEQ-TEST-08-01-END.                                              SQ1064.2
085400*                                                                 SQ1064.2
085500*                                                                 SQ1064.2
085600*    CREATE A LONG RECORD USING WRITE FROM, USING A 151           SQ1064.2
085700*    CHARACTER SOURCE AREA.                                       SQ1064.2
085800*                                                                 SQ1064.2
085900 SEQ-INIT-09.                                                     SQ1064.2
086000     MOVE    1 TO REC-CT.                                         SQ1064.2
086100     MOVE   "WRITE LONG RECORD FROM" TO FEATURE.                  SQ1064.2
086200     MOVE   "SEQ-TEST-WR-09" TO PAR-NAME.                         SQ1064.2
086300     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
086400     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
086500     MOVE   "R2-M-G" TO XRECORD-NAME (1).                         SQ1064.2
086600     MOVE    151 TO XRECORD-LENGTH (1).                           SQ1064.2
086700     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
086800     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
086900     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
087000         GO TO SEQ-DELETE-09.                                     SQ1064.2
087100     GO TO SEQ-TEST-WR-09.                                        SQ1064.2
087200 SEQ-DELETE-09.                                                   SQ1064.2
087300     GO TO   SEQ-DELETE-09-01.                                    SQ1064.2
087400 SEQ-TEST-WR-09.                                                  SQ1064.2
087500     MOVE    FILE-RECORD-INFO-P1-120 (1) TO BUFFER-COPY-120.      SQ1064.2
087600     WRITE   SQ-VS6R2-M-G-151 FROM BUFFER-COPY.                   SQ1064.2
087700     GO TO   SEQ-TEST-WR-09-01.                                   SQ1064.2
087800 SEQ-DELETE-09-01.                                                SQ1064.2
087900     PERFORM DE-LETE.                                             SQ1064.2
088000     GO TO   SEQ-TEST-09-01-END.                                  SQ1064.2
088100 SEQ-TEST-WR-09-01.                                               SQ1064.2
088200     IF SQ-STATUS = "00"                                          SQ1064.2
088300         PERFORM PASS                                             SQ1064.2
088400     ELSE                                                         SQ1064.2
088500         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
088600         MOVE   "00" TO CORRECT-A                                 SQ1064.2
088700         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
088800         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
088900         PERFORM FAIL.                                            SQ1064.2
089000 SEQ-TEST-09-01-END.                                              SQ1064.2
089100*                                                                 SQ1064.2
089200*                                                                 SQ1064.2
089300*    CREATE A LONG RECORD USING THE BASIC WRITE STATEMENT         SQ1064.2
089400*                                                                 SQ1064.2
089500 SEQ-INIT-10.                                                     SQ1064.2
089600     MOVE    1 TO REC-CT.                                         SQ1064.2
089700     MOVE   "WRITE LONG RECORD" TO FEATURE.                       SQ1064.2
089800     MOVE   "SEQ-TEST-WR-10" TO PAR-NAME.                         SQ1064.2
089900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
090000     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
090100     MOVE   "R2-M-G" TO XRECORD-NAME (1).                         SQ1064.2
090200     MOVE    151 TO XRECORD-LENGTH (1).                           SQ1064.2
090300     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
090400     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
090500     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
090600         GO TO SEQ-DELETE-10.                                     SQ1064.2
090700     GO TO SEQ-TEST-WR-10.                                        SQ1064.2
090800 SEQ-DELETE-10.                                                   SQ1064.2
090900     GO TO   SEQ-DELETE-10-01.                                    SQ1064.2
091000 SEQ-TEST-WR-10.                                                  SQ1064.2
091100     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R2-FIRST.       SQ1064.2
091200     MOVE    BUFFER-COPY-SECOND TO SQ-VS6R2-SECOND.               SQ1064.2
091300     WRITE   SQ-VS6R2-M-G-151.                                    SQ1064.2
091400     GO TO   SEQ-TEST-WR-10-01.                                   SQ1064.2
091500 SEQ-DELETE-10-01.                                                SQ1064.2
091600     PERFORM DE-LETE.                                             SQ1064.2
091700     GO TO   SEQ-TEST-10-01-END.                                  SQ1064.2
091800 SEQ-TEST-WR-10-01.                                               SQ1064.2
091900     IF SQ-STATUS = "00"                                          SQ1064.2
092000         PERFORM PASS                                             SQ1064.2
092100     ELSE                                                         SQ1064.2
092200         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
092300         MOVE   "00" TO CORRECT-A                                 SQ1064.2
092400         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
092500         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
092600         PERFORM FAIL.                                            SQ1064.2
092700 SEQ-TEST-10-01-END.                                              SQ1064.2
092800*                                                                 SQ1064.2
092900*                                                                 SQ1064.2
093000*    CREATE A SHORT RECORD USING THE BASIC WRITE STATEMENT        SQ1064.2
093100*                                                                 SQ1064.2
093200 SEQ-INIT-11.                                                     SQ1064.2
093300     MOVE    1 TO REC-CT.                                         SQ1064.2
093400     MOVE   "WRITE SHORT RECORD" TO FEATURE.                      SQ1064.2
093500     MOVE   "SEQ-TEST-WR-11" TO PAR-NAME.                         SQ1064.2
093600     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
093700     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
093800     MOVE   "R1-M-G" TO XRECORD-NAME (1).                         SQ1064.2
093900     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1064.2
094000     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
094100     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
094200     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
094300         GO TO SEQ-DELETE-11.                                     SQ1064.2
094400     GO TO SEQ-TEST-WR-11.                                        SQ1064.2
094500 SEQ-DELETE-11.                                                   SQ1064.2
094600     GO TO   SEQ-DELETE-11-01.                                    SQ1064.2
094700 SEQ-TEST-WR-11.                                                  SQ1064.2
094800     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R2-FIRST.       SQ1064.2
094900     MOVE    BUFFER-COPY-SECOND TO SQ-VS6R2-SECOND.               SQ1064.2
095000     WRITE   SQ-VS6R1-M-G-120.                                    SQ1064.2
095100     GO TO   SEQ-TEST-WR-11-01.                                   SQ1064.2
095200 SEQ-DELETE-11-01.                                                SQ1064.2
095300     PERFORM DE-LETE.                                             SQ1064.2
095400     GO TO   SEQ-TEST-11-01-END.                                  SQ1064.2
095500 SEQ-TEST-WR-11-01.                                               SQ1064.2
095600     IF SQ-STATUS = "00"                                          SQ1064.2
095700         PERFORM PASS                                             SQ1064.2
095800     ELSE                                                         SQ1064.2
095900         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
096000         MOVE   "00" TO CORRECT-A                                 SQ1064.2
096100         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
096200         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
096300         PERFORM FAIL.                                            SQ1064.2
096400 SEQ-TEST-11-01-END.                                              SQ1064.2
096500*                                                                 SQ1064.2
096600*    CREATE A SHORT RECORD USING THE BASIC WRITE STATEMENT        SQ1064.2
096700*                                                                 SQ1064.2
096800 SEQ-INIT-12.                                                     SQ1064.2
096900     MOVE    1 TO REC-CT.                                         SQ1064.2
097000     MOVE   "WRITE SHORT RECORD" TO FEATURE.                      SQ1064.2
097100     MOVE   "SEQ-TEST-WR-12" TO PAR-NAME.                         SQ1064.2
097200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
097300     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
097400     MOVE   "R1-M-G" TO XRECORD-NAME (1).                         SQ1064.2
097500     MOVE    120 TO XRECORD-LENGTH (1).                           SQ1064.2
097600     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
097700     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
097800     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
097900         GO TO SEQ-DELETE-12.                                     SQ1064.2
098000     GO TO SEQ-TEST-WR-12.                                        SQ1064.2
098100 SEQ-DELETE-12.                                                   SQ1064.2
098200     GO TO   SEQ-DELETE-12-01.                                    SQ1064.2
098300 SEQ-TEST-WR-12.                                                  SQ1064.2
098400     MOVE    FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R2-FIRST.       SQ1064.2
098500     MOVE    BUFFER-COPY-SECOND TO SQ-VS6R2-SECOND.               SQ1064.2
098600     WRITE   SQ-VS6R1-M-G-120.                                    SQ1064.2
098700     GO TO   SEQ-TEST-WR-12-01.                                   SQ1064.2
098800 SEQ-DELETE-12-01.                                                SQ1064.2
098900     PERFORM DE-LETE.                                             SQ1064.2
099000     GO TO   SEQ-TEST-12-01-END.                                  SQ1064.2
099100 SEQ-TEST-WR-12-01.                                               SQ1064.2
099200     IF SQ-STATUS = "00"                                          SQ1064.2
099300         PERFORM PASS                                             SQ1064.2
099400     ELSE                                                         SQ1064.2
099500         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
099600         MOVE   "00" TO CORRECT-A                                 SQ1064.2
099700         MOVE   "UNEXPECTED I-O STATUS FROM WRITE" TO RE-MARK     SQ1064.2
099800         MOVE   "VII-3, VII-53,4.7.4(6)" TO ANSI-REFERENCE        SQ1064.2
099900         PERFORM FAIL.                                            SQ1064.2
100000 SEQ-TEST-12-01-END.                                              SQ1064.2
100100*                                                                 SQ1064.2
100200*                                                                 SQ1064.2
100300*    ALL REQUIRED RECORDS HAVE BEEN WRITTEN, SO THE FILE          SQ1064.2
100400*    CAN BE CLOSED.                                               SQ1064.2
100500*                                                                 SQ1064.2
100600 SEQ-INIT-13.                                                     SQ1064.2
100700     MOVE    1 TO REC-CT.                                         SQ1064.2
100800     MOVE   "CLOSE NEW FILE" TO FEATURE.                          SQ1064.2
100900     MOVE   "SEQ-TEST-WR-13" TO PAR-NAME.                         SQ1064.2
101000     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
101100     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
101200         GO TO SEQ-DELETE-13.                                     SQ1064.2
101300     GO TO SEQ-TEST-WR-13.                                        SQ1064.2
101400 SEQ-DELETE-13.                                                   SQ1064.2
101500     GO TO   SEQ-DELETE-13-01.                                    SQ1064.2
101600 SEQ-TEST-WR-13.                                                  SQ1064.2
101700     CLOSE   SQ-VS6.                                              SQ1064.2
101800     GO TO   SEQ-TEST-WR-13-01.                                   SQ1064.2
101900 SEQ-DELETE-13-01.                                                SQ1064.2
102000     PERFORM DE-LETE.                                             SQ1064.2
102100     GO TO   SEQ-TEST-13-01-END.                                  SQ1064.2
102200 SEQ-TEST-WR-13-01.                                               SQ1064.2
102300     IF SQ-STATUS = "00"                                          SQ1064.2
102400         PERFORM PASS                                             SQ1064.2
102500     ELSE                                                         SQ1064.2
102600         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
102700         MOVE   "00" TO CORRECT-A                                 SQ1064.2
102800         MOVE   "UNEXPECTED I-O STATUS FROM CLOSE" TO RE-MARK     SQ1064.2
102900         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1064.2
103000         PERFORM FAIL.                                            SQ1064.2
103100 SEQ-TEST-13-01-END.                                              SQ1064.2
103200*                                                                 SQ1064.2
103300*                                                                 SQ1064.2
103400*    A SEQUENTIAL TAPE FILE CONTAINING 11 RECORDS HAS BEEN        SQ1064.2
103500*    CREATED.  THE FILE CONTAINS RECORDS OF 120 CHARACTERS AND    SQ1064.2
103600*    RECORDS OF 151 CHARACTERS.  THE SEQUENCE IN WHICH THE        SQ1064.2
103700*    RECORDS WERE WRITTEN IS SLSSLSLLLSS.  THE NEXT GROUP OF      SQ1064.2
103800*    OPENS THE FILE FOR INPUT AND READS IT, USING TWELVE          SQ1064.2
103900*    DIFFERENT FORMATS OF THE READ STATEMENT.  DELETION OF THIS   SQ1064.2
104000*    TEST CAUSES DELETION OF ALL SUBSEQUENT TESTS.                SQ1064.2
104100*                                                                 SQ1064.2
104200*                                                                 SQ1064.2
104300 SEQ-INIT-14.                                                     SQ1064.2
104400     MOVE   "**"  TO SQ-STATUS.                                   SQ1064.2
104500     MOVE   "OPEN FILE FOR INPUT" TO FEATURE.                     SQ1064.2
104600     MOVE   "SEQ-TEST-GF-14" TO PAR-NAME.                         SQ1064.2
104700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
104800         GO TO SEQ-DELETE-14.                                     SQ1064.2
104900     GO TO   SEQ-TEST-GF-14.                                      SQ1064.2
105000 SEQ-DELETE-14.                                                   SQ1064.2
105100     MOVE   "*" TO DELETE-SW-1.                                   SQ1064.2
105200     GO TO   SEQ-DELETE-14-01.                                    SQ1064.2
105300 SEQ-TEST-GF-14.                                                  SQ1064.2
105400     OPEN INPUT SQ-VS6.                                           SQ1064.2
105500     GO TO   SEQ-TEST-GF-14-01.                                   SQ1064.2
105600 SEQ-DELETE-14-01.                                                SQ1064.2
105700     PERFORM DE-LETE.                                             SQ1064.2
105800     GO TO   SEQ-TEST-14-01-END.                                  SQ1064.2
105900 SEQ-TEST-GF-14-01.                                               SQ1064.2
106000     IF SQ-STATUS = "00"                                          SQ1064.2
106100         PERFORM PASS                                             SQ1064.2
106200     ELSE                                                         SQ1064.2
106300         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
106400         MOVE   "00" TO CORRECT-A                                 SQ1064.2
106500         MOVE   "UNEXPECTED I-O STATUS FROM OPEN" TO RE-MARK      SQ1064.2
106600         MOVE   "VII-2, VII-39" TO ANSI-REFERENCE                 SQ1064.2
106700         PERFORM FAIL                                             SQ1064.2
106800         MOVE   "*" TO DELETE-SW-1.                               SQ1064.2
106900 SEQ-TEST-14-01-END.                                              SQ1064.2
107000*                                                                 SQ1064.2
107100*                                                                 SQ1064.2
107200*    UNLESS AN ERROR OCCURRED DURING EXECUTION OF THE OPEN        SQ1064.2
107300*    STATEMENT, THE FILE IS NOW OPEN, AND READY FOR RECORDS TO    SQ1064.2
107400*    BE WRITTEN TO IT.  IF AN ERROR I-O STATUS VALUE WAS          SQ1064.2
107500*    RETURNED, ALL THE REMAINING TESTS ARE DELETED.               SQ1064.2
107600*                                                                 SQ1064.2
107700*    READ A SHORT RECORD, USING READ ... AT END                   SQ1064.2
107800*                                                                 SQ1064.2
107900 SEQ-INIT-15.                                                     SQ1064.2
108000     MOVE    1 TO REC-CT.                                         SQ1064.2
108100     MOVE    1 TO XRECORD-NUMBER (1).                             SQ1064.2
108200     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
108300     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
108400     MOVE    1 TO BUFFER-COPY-RECNO.                              SQ1064.2
108500     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
108600     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
108700     MOVE   "READ SHORT AT END" TO FEATURE.                       SQ1064.2
108800     MOVE   "SEQ-TEST-RD-15" TO PAR-NAME.                         SQ1064.2
108900     IF DELETE-SW NOT = SPACE                                     SQ1064.2
109000         GO TO   SEQ-DELETE-15.                                   SQ1064.2
109100     GO TO   SEQ-TEST-RD-15.                                      SQ1064.2
109200 SEQ-DELETE-15.                                                   SQ1064.2
109300     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
109400     GO TO   SEQ-DELETE-15-01.                                    SQ1064.2
109500*                                                                 SQ1064.2
109600*    EXECUTE THE READ STATEMENT                                   SQ1064.2
109700*                                                                 SQ1064.2
109800 SEQ-TEST-RD-15.                                                  SQ1064.2
109900     READ    SQ-VS6 AT END                                        SQ1064.2
110000                MOVE    "EXECUTED" TO EOF-FLAG.                   SQ1064.2
110100     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
110200     GO TO   SEQ-TEST-RD-15-01.                                   SQ1064.2
110300*                                                                 SQ1064.2
110400*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
110500*                                                                 SQ1064.2
110600 SEQ-DELETE-15-01.                                                SQ1064.2
110700     PERFORM DE-LETE.                                             SQ1064.2
110800     GO TO   SEQ-TEST-15-01-END.                                  SQ1064.2
110900 SEQ-TEST-RD-15-01.                                               SQ1064.2
111000     IF SQ-STATUS = "00"                                          SQ1064.2
111100         PERFORM PASS                                             SQ1064.2
111200     ELSE                                                         SQ1064.2
111300         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
111400         MOVE   "00" TO CORRECT-A                                 SQ1064.2
111500         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
111600         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
111700         PERFORM FAIL.                                            SQ1064.2
111800 SEQ-TEST-15-01-END.                                              SQ1064.2
111900*                                                                 SQ1064.2
112000*    CHECK THE RECORD NUMBER OF THE RECORD READ                   SQ1064.2
112100*                                                                 SQ1064.2
112200     ADD     1 TO REC-CT.                                         SQ1064.2
112300     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
112400         GO TO   SEQ-DELETE-15-02.                                SQ1064.2
112500     GO TO   SEQ-TEST-RD-15-02.                                   SQ1064.2
112600 SEQ-DELETE-15-02.                                                SQ1064.2
112700     PERFORM DE-LETE.                                             SQ1064.2
112800     GO TO   SEQ-TEST-15-02-END.                                  SQ1064.2
112900 SEQ-TEST-RD-15-02.                                               SQ1064.2
113000     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
113100         PERFORM PASS                                             SQ1064.2
113200     ELSE                                                         SQ1064.2
113300         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
113400         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
113500         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
113600         PERFORM FAIL.                                            SQ1064.2
113700 SEQ-TEST-15-02-END.                                              SQ1064.2
113800*                                                                 SQ1064.2
113900*    CHECK THE EXTENDED PART OF THE RECORD                        SQ1064.2
114000*                                                                 SQ1064.2
114100     ADD     1 TO REC-CT.                                         SQ1064.2
114200*    IF DELETE-SW NOT = TO SPACE                                  SQ1064.2
114300*        GO TO SEQ-DELETE-15-03.                                  SQ1064.2
114400*    GO TO   SEQ-TEST-RD-15-03.                                   SQ1064.2
114500 SEQ-DELETE-15-03.                                                SQ1064.2
114600     PERFORM DE-LETE.                                             SQ1064.2
114700     GO TO   SEQ-TEST-15-03-END.                                  SQ1064.2
114800 SEQ-TEST-RD-15-03.                                               SQ1064.2
114900     IF SQ-VS6R2-SECOND-L NOT = BUFFER-COPY-SECOND-L              SQ1064.2
115000         PERFORM PASS                                             SQ1064.2
115100     ELSE                                                         SQ1064.2
115200         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
115300         MOVE   "**UNDEFINED**" TO CORRECT-A                      SQ1064.2
115400         MOVE   "BUFFER CONTENTS FOR LONG RECORD" TO RE-MARK      SQ1064.2
115500         PERFORM FAIL.                                            SQ1064.2
115600 SEQ-TEST-15-03-END.                                              SQ1064.2
115700*                                                                 SQ1064.2
115800*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
115900*                                                                 SQ1064.2
116000     ADD     1 TO REC-CT.                                         SQ1064.2
116100     IF DELETE-SW NOT = SPACE                                     SQ1064.2
116200         GO TO   SEQ-DELETE-15-04.                                SQ1064.2
116300     GO TO   SEQ-TEST-RD-15-04.                                   SQ1064.2
116400 SEQ-DELETE-15-04.                                                SQ1064.2
116500     PERFORM DE-LETE.                                             SQ1064.2
116600     GO TO   SEQ-TEST-15-04-END.                                  SQ1064.2
116700 SEQ-TEST-RD-15-04.                                               SQ1064.2
116800     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
116900         PERFORM PASS                                             SQ1064.2
117000     ELSE                                                         SQ1064.2
117100         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
117200         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
117300         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
117400         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
117500         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
117600         PERFORM FAIL.                                            SQ1064.2
117700 SEQ-TEST-15-04-END.                                              SQ1064.2
117800     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
117900*                                                                 SQ1064.2
118000*                                                                 SQ1064.2
118100*    READ A LONG RECORD, USING READ ... END                       SQ1064.2
118200*                                                                 SQ1064.2
118300 SEQ-INIT-16.                                                     SQ1064.2
118400     MOVE    1 TO REC-CT.                                         SQ1064.2
118500     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
118600     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
118700     MOVE   "LONG"  TO BUFFER-COPY-L-OR-S.                        SQ1064.2
118800     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
118900     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
119000     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
119100     MOVE   "READ LONG END" TO FEATURE.                           SQ1064.2
119200     MOVE   "SEQ-TEST-RD-16" TO PAR-NAME.                         SQ1064.2
119300     IF DELETE-SW NOT = SPACE                                     SQ1064.2
119400         GO TO   SEQ-DELETE-16.                                   SQ1064.2
119500     GO TO   SEQ-TEST-RD-16.                                      SQ1064.2
119600  SEQ-DELETE-16.                                                  SQ1064.2
119700     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
119800     GO TO   SEQ-DELETE-16-01.                                    SQ1064.2
119900*                                                                 SQ1064.2
120000*   EXECUTE THE READ STATEMENT                                    SQ1064.2
120100*                                                                 SQ1064.2
120200 SEQ-TEST-RD-16.                                                  SQ1064.2
120300     READ    SQ-VS6 END                                           SQ1064.2
120400                MOVE   "EXECUTED" TO EOF-FLAG.                    SQ1064.2
120500     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
120600     GO TO   SEQ-TEST-RD-16-01.                                   SQ1064.2
120700 SEQ-DELETE-16-01.                                                SQ1064.2
120800     PERFORM DE-LETE.                                             SQ1064.2
120900     GO TO   SEQ-TEST-16-01-END.                                  SQ1064.2
121000*                                                                 SQ1064.2
121100*   CHECK THE FILE STATUS RETURNED                                SQ1064.2
121200*                                                                 SQ1064.2
121300 SEQ-TEST-RD-16-01.                                               SQ1064.2
121400     IF SQ-STATUS = "00"                                          SQ1064.2
121500         PERFORM PASS                                             SQ1064.2
121600     ELSE                                                         SQ1064.2
121700         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
121800         MOVE   "00" TO CORRECT-A                                 SQ1064.2
121900         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
122000         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
122100         PERFORM FAIL.                                            SQ1064.2
122200 SEQ-TEST-16-01-END.                                              SQ1064.2
122300*                                                                 SQ1064.2
122400*    CHECK THE IDENTITY OF THE RECORD RETURNED                    SQ1064.2
122500*                                                                 SQ1064.2
122600     ADD     1 TO REC-CT.                                         SQ1064.2
122700     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
122800         GO TO   SEQ-DELETE-16-02.                                SQ1064.2
122900     GO TO   SEQ-TEST-RD-16-02.                                   SQ1064.2
123000 SEQ-DELETE-16-02.                                                SQ1064.2
123100     PERFORM DE-LETE.                                             SQ1064.2
123200     GO TO   SEQ-TEST-16-02-END.                                  SQ1064.2
123300 SEQ-TEST-RD-16-02.                                               SQ1064.2
123400     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
123500         PERFORM PASS                                             SQ1064.2
123600     ELSE                                                         SQ1064.2
123700         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
123800         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
123900         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
124000         PERFORM FAIL.                                            SQ1064.2
124100 SEQ-TEST-16-02-END.                                              SQ1064.2
124200*                                                                 SQ1064.2
124300*    CHECK THE RECORD EXTENSION AREA                              SQ1064.2
124400*                                                                 SQ1064.2
124500     ADD     1 TO REC-CT.                                         SQ1064.2
124600     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1064.2
124700         GO TO SEQ-DELETE-16-03.                                  SQ1064.2
124800     GO TO   SEQ-TEST-RD-16-03.                                   SQ1064.2
124900 SEQ-DELETE-16-03.                                                SQ1064.2
125000     PERFORM DE-LETE.                                             SQ1064.2
125100     GO TO   SEQ-TEST-16-03-END.                                  SQ1064.2
125200 SEQ-TEST-RD-16-03.                                               SQ1064.2
125300     IF SQ-VS6R2-SECOND-L = BUFFER-COPY-SECOND-L                  SQ1064.2
125400         PERFORM PASS                                             SQ1064.2
125500     ELSE                                                         SQ1064.2
125600         MOVE SQ-VS6R2-SECOND-L TO COMPUTED-A                     SQ1064.2
125700         MOVE    BUFFER-COPY-SECOND-L TO CORRECT-A                SQ1064.2
125800         MOVE   "INCORRECT VALUE IN BUFFER EXTENSION" TO RE-MARK  SQ1064.2
125900         PERFORM FAIL.                                            SQ1064.2
126000 SEQ-TEST-16-03-END.                                              SQ1064.2
126100*                                                                 SQ1064.2
126200*    CHECK EXECUTION OF THE END PHRASE                            SQ1064.2
126300*                                                                 SQ1064.2
126400     ADD     1 TO REC-CT.                                         SQ1064.2
126500     IF DELETE-SW NOT = SPACE                                     SQ1064.2
126600         GO TO   SEQ-DELETE-16-04.                                SQ1064.2
126700     GO TO   SEQ-TEST-RD-16-04.                                   SQ1064.2
126800 SEQ-DELETE-16-04.                                                SQ1064.2
126900     PERFORM DE-LETE.                                             SQ1064.2
127000     GO TO   SEQ-TEST-16-04-END.                                  SQ1064.2
127100 SEQ-TEST-RD-16-04.                                               SQ1064.2
127200     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
127300         PERFORM PASS                                             SQ1064.2
127400     ELSE                                                         SQ1064.2
127500         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
127600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
127700         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
127800         MOVE   "V11-46,4.4.4(10)" TO ANSI-REFERENCE              SQ1064.2
127900         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
128000         PERFORM FAIL.                                            SQ1064.2
128100 SEQ-TEST-16-04-END.                                              SQ1064.2
128200     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
128300*                                                                 SQ1064.2
128400*                                                                 SQ1064.2
128500*    READ A SHORT RECORD, USING READ ... AT END ... NOT AT END    SQ1064.2
128600*                                                                 SQ1064.2
128700 SEQ-INIT-17.                                                     SQ1064.2
128800     MOVE    1 TO REC-CT.                                         SQ1064.2
128900     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
129000     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
129100     MOVE   "SHORT"  TO BUFFER-COPY-L-OR-S.                       SQ1064.2
129200     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
129300     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
129400     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
129500     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
129600     MOVE   "READ SHORT AT END N A E" TO FEATURE.                 SQ1064.2
129700     MOVE   "SEQ-TEST-RD-17" TO PAR-NAME.                         SQ1064.2
129800     IF DELETE-SW NOT = SPACE                                     SQ1064.2
129900         GO TO   SEQ-DELETE-17.                                   SQ1064.2
130000     GO TO   SEQ-TEST-RD-17.                                      SQ1064.2
130100 SEQ-DELETE-17.                                                   SQ1064.2
130200     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
130300     GO TO   SEQ-DELETE-17-01.                                    SQ1064.2
130400*                                                                 SQ1064.2
130500*    EXECUTE THE READ STATEMENT                                   SQ1064.2
130600*                                                                 SQ1064.2
130700 SEQ-TEST-RD-17.                                                  SQ1064.2
130800     READ    SQ-VS6                                               SQ1064.2
130900        AT END                                                    SQ1064.2
131000                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
131100        NOT AT END                                                SQ1064.2
131200                MOVE   "EXECUTED" TO NOT-EOF-FLAG.                SQ1064.2
131300     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
131400     GO TO   SEQ-TEST-RD-17-01.                                   SQ1064.2
131500*                                                                 SQ1064.2
131600*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
131700*                                                                 SQ1064.2
131800 SEQ-DELETE-17-01.                                                SQ1064.2
131900     PERFORM DE-LETE.                                             SQ1064.2
132000     GO TO   SEQ-TEST-17-01-END.                                  SQ1064.2
132100 SEQ-TEST-RD-17-01.                                               SQ1064.2
132200     IF SQ-STATUS = "00"                                          SQ1064.2
132300         PERFORM PASS                                             SQ1064.2
132400     ELSE                                                         SQ1064.2
132500         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
132600         MOVE   "00" TO CORRECT-A                                 SQ1064.2
132700         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
132800         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
132900         PERFORM FAIL.                                            SQ1064.2
133000 SEQ-TEST-17-01-END.                                              SQ1064.2
133100*                                                                 SQ1064.2
133200*    CHECK THE RECORD NUMBER OF THE RECORD READ                   SQ1064.2
133300*                                                                 SQ1064.2
133400     ADD     1 TO REC-CT.                                         SQ1064.2
133500     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
133600         GO TO   SEQ-DELETE-17-02.                                SQ1064.2
133700     GO TO   SEQ-TEST-RD-17-02.                                   SQ1064.2
133800 SEQ-DELETE-17-02.                                                SQ1064.2
133900     PERFORM DE-LETE.                                             SQ1064.2
134000     GO TO   SEQ-TEST-17-02-END.                                  SQ1064.2
134100 SEQ-TEST-RD-17-02.                                               SQ1064.2
134200     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
134300         PERFORM PASS                                             SQ1064.2
134400     ELSE                                                         SQ1064.2
134500         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
134600         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
134700         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
134800         PERFORM FAIL.                                            SQ1064.2
134900 SEQ-TEST-17-02-END.                                              SQ1064.2
135000*                                                                 SQ1064.2
135100*    CHECK THE EXTENDED PART OF THE RECORD                        SQ1064.2
135200*                                                                 SQ1064.2
135300     ADD     1 TO REC-CT.                                         SQ1064.2
135400*    IF DELETE-SW NOT = TO SPACE                                  SQ1064.2
135500*        GO TO SEQ-DELETE-17-03.                                  SQ1064.2
135600*    GO TO   SEQ-TEST-RD-17-03.                                   SQ1064.2
135700 SEQ-DELETE-17-03.                                                SQ1064.2
135800     PERFORM DE-LETE.                                             SQ1064.2
135900     GO TO   SEQ-TEST-17-03-END.                                  SQ1064.2
136000 SEQ-TEST-RD-17-03.                                               SQ1064.2
136100     IF SQ-VS6R2-SECOND-L NOT = BUFFER-COPY-SECOND-L              SQ1064.2
136200         PERFORM PASS                                             SQ1064.2
136300     ELSE                                                         SQ1064.2
136400         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
136500         MOVE   "**UNDEFINED**" TO CORRECT-A                      SQ1064.2
136600         MOVE   "BUFFER CONTENTS FOR LONG RECORD" TO RE-MARK      SQ1064.2
136700         PERFORM FAIL.                                            SQ1064.2
136800 SEQ-TEST-17-03-END.                                              SQ1064.2
136900*                                                                 SQ1064.2
137000*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
137100*                                                                 SQ1064.2
137200     ADD     1 TO REC-CT.                                         SQ1064.2
137300     IF DELETE-SW NOT = SPACE                                     SQ1064.2
137400         GO TO   SEQ-DELETE-17-04.                                SQ1064.2
137500     GO TO   SEQ-TEST-RD-17-04.                                   SQ1064.2
137600 SEQ-DELETE-17-04.                                                SQ1064.2
137700     PERFORM DE-LETE.                                             SQ1064.2
137800     GO TO   SEQ-TEST-17-04-END.                                  SQ1064.2
137900 SEQ-TEST-RD-17-04.                                               SQ1064.2
138000     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
138100         PERFORM PASS                                             SQ1064.2
138200     ELSE                                                         SQ1064.2
138300         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
138400         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
138500         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
138600         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
138700         PERFORM FAIL.                                            SQ1064.2
138800 SEQ-TEST-17-04-END.                                              SQ1064.2
138900*                                                                 SQ1064.2
139000*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
139100*                                                                 SQ1064.2
139200     ADD     1 TO REC-CT.                                         SQ1064.2
139300     IF DELETE-SW NOT = SPACE                                     SQ1064.2
139400         GO TO   SEQ-DELETE-17-05.                                SQ1064.2
139500     GO TO   SEQ-TEST-RD-17-05.                                   SQ1064.2
139600 SEQ-DELETE-17-05.                                                SQ1064.2
139700     PERFORM DE-LETE.                                             SQ1064.2
139800     GO TO   SEQ-TEST-17-05-END.                                  SQ1064.2
139900 SEQ-TEST-RD-17-05.                                               SQ1064.2
140000     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
140100         PERFORM PASS                                             SQ1064.2
140200     ELSE                                                         SQ1064.2
140300         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
140400         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
140500         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
140600         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
140700         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
140800         PERFORM FAIL.                                            SQ1064.2
140900 SEQ-TEST-17-05-END.                                              SQ1064.2
141000     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
141100*                                                                 SQ1064.2
141200*                                                                 SQ1064.2
141300*    READ A SHORT RECORD, USING READ ... END ... NOT AT END       SQ1064.2
141400*                                                                 SQ1064.2
141500 SEQ-INIT-18.                                                     SQ1064.2
141600     MOVE    1 TO REC-CT.                                         SQ1064.2
141700     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
141800     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
141900     MOVE   "SHORT"  TO BUFFER-COPY-L-OR-S.                       SQ1064.2
142000     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
142100     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
142200     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
142300     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
142400     MOVE   "READ SHORT END N A E" TO FEATURE.                    SQ1064.2
142500     MOVE   "SEQ-TEST-RD-18" TO PAR-NAME.                         SQ1064.2
142600     IF DELETE-SW NOT = SPACE                                     SQ1064.2
142700         GO TO   SEQ-DELETE-18.                                   SQ1064.2
142800     GO TO   SEQ-TEST-RD-18.                                      SQ1064.2
142900 SEQ-DELETE-18.                                                   SQ1064.2
143000     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
143100     GO TO   SEQ-DELETE-18-01.                                    SQ1064.2
143200*                                                                 SQ1064.2
143300*    EXECUTE THE READ STATEMENT                                   SQ1064.2
143400*                                                                 SQ1064.2
143500 SEQ-TEST-RD-18.                                                  SQ1064.2
143600     READ    SQ-VS6                                               SQ1064.2
143700        END                                                       SQ1064.2
143800                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
143900        NOT AT END                                                SQ1064.2
144000                MOVE   "EXECUTED" TO NOT-EOF-FLAG.                SQ1064.2
144100     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
144200     GO TO   SEQ-TEST-RD-18-01.                                   SQ1064.2
144300*                                                                 SQ1064.2
144400*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
144500*                                                                 SQ1064.2
144600 SEQ-DELETE-18-01.                                                SQ1064.2
144700     PERFORM DE-LETE.                                             SQ1064.2
144800     GO TO   SEQ-TEST-18-01-END.                                  SQ1064.2
144900 SEQ-TEST-RD-18-01.                                               SQ1064.2
145000     IF SQ-STATUS = "00"                                          SQ1064.2
145100         PERFORM PASS                                             SQ1064.2
145200     ELSE                                                         SQ1064.2
145300         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
145400         MOVE   "00" TO CORRECT-A                                 SQ1064.2
145500         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
145600         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
145700         PERFORM FAIL.                                            SQ1064.2
145800 SEQ-TEST-18-01-END.                                              SQ1064.2
145900*                                                                 SQ1064.2
146000*    CHECK THE RECORD NUMBER OF THE RECORD READ                   SQ1064.2
146100*                                                                 SQ1064.2
146200     ADD     1 TO REC-CT.                                         SQ1064.2
146300     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
146400         GO TO   SEQ-DELETE-18-02.                                SQ1064.2
146500     GO TO   SEQ-TEST-RD-18-02.                                   SQ1064.2
146600 SEQ-DELETE-18-02.                                                SQ1064.2
146700     PERFORM DE-LETE.                                             SQ1064.2
146800     GO TO   SEQ-TEST-18-02-END.                                  SQ1064.2
146900 SEQ-TEST-RD-18-02.                                               SQ1064.2
147000     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
147100         PERFORM PASS                                             SQ1064.2
147200     ELSE                                                         SQ1064.2
147300         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
147400         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
147500         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
147600         PERFORM FAIL.                                            SQ1064.2
147700 SEQ-TEST-18-02-END.                                              SQ1064.2
147800*                                                                 SQ1064.2
147900*    CHECK THE EXTENDED PART OF THE RECORD                        SQ1064.2
148000*                                                                 SQ1064.2
148100     ADD     1 TO REC-CT.                                         SQ1064.2
148200*    IF DELETE-SW NOT = TO SPACE                                  SQ1064.2
148300*        GO TO SEQ-DELETE-18-03.                                  SQ1064.2
148400*    GO TO   SEQ-TEST-RD-18-03.                                   SQ1064.2
148500 SEQ-DELETE-18-03.                                                SQ1064.2
148600     PERFORM DE-LETE.                                             SQ1064.2
148700     GO TO   SEQ-TEST-18-03-END.                                  SQ1064.2
148800 SEQ-TEST-RD-18-03.                                               SQ1064.2
148900     IF SQ-VS6R2-SECOND-L NOT = BUFFER-COPY-SECOND-L              SQ1064.2
149000         PERFORM PASS                                             SQ1064.2
149100     ELSE                                                         SQ1064.2
149200         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
149300         MOVE   "**UNDEFINED**" TO CORRECT-A                      SQ1064.2
149400         MOVE   "BUFFER CONTENTS FOR LONG RECORD" TO RE-MARK      SQ1064.2
149500         PERFORM FAIL.                                            SQ1064.2
149600 SEQ-TEST-18-03-END.                                              SQ1064.2
149700*                                                                 SQ1064.2
149800*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
149900*                                                                 SQ1064.2
150000     ADD     1 TO REC-CT.                                         SQ1064.2
150100     IF DELETE-SW NOT = SPACE                                     SQ1064.2
150200         GO TO   SEQ-DELETE-18-04.                                SQ1064.2
150300     GO TO   SEQ-TEST-RD-18-04.                                   SQ1064.2
150400 SEQ-DELETE-18-04.                                                SQ1064.2
150500     PERFORM DE-LETE.                                             SQ1064.2
150600     GO TO   SEQ-TEST-18-04-END.                                  SQ1064.2
150700 SEQ-TEST-RD-18-04.                                               SQ1064.2
150800     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
150900         PERFORM PASS                                             SQ1064.2
151000     ELSE                                                         SQ1064.2
151100         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
151200         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
151300         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
151400         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
151500         PERFORM FAIL.                                            SQ1064.2
151600 SEQ-TEST-18-04-END.                                              SQ1064.2
151700*                                                                 SQ1064.2
151800*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
151900*                                                                 SQ1064.2
152000     ADD     1 TO REC-CT.                                         SQ1064.2
152100     IF DELETE-SW NOT = SPACE                                     SQ1064.2
152200         GO TO   SEQ-DELETE-18-05.                                SQ1064.2
152300     GO TO   SEQ-TEST-RD-18-05.                                   SQ1064.2
152400 SEQ-DELETE-18-05.                                                SQ1064.2
152500     PERFORM DE-LETE.                                             SQ1064.2
152600     GO TO   SEQ-TEST-18-05-END.                                  SQ1064.2
152700 SEQ-TEST-RD-18-05.                                               SQ1064.2
152800     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
152900         PERFORM PASS                                             SQ1064.2
153000     ELSE                                                         SQ1064.2
153100         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
153200         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
153300         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
153400         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
153500         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
153600         PERFORM FAIL.                                            SQ1064.2
153700 SEQ-TEST-18-05-END.                                              SQ1064.2
153800     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
153900*                                                                 SQ1064.2
154000*                                                                 SQ1064.2
154100*    READ A LONG RECORD, USING READ ... AT END ... NOT END ...    SQ1064.2
154200*                                                                 SQ1064.2
154300 SEQ-INIT-19.                                                     SQ1064.2
154400     MOVE    1 TO REC-CT.                                         SQ1064.2
154500     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
154600     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
154700     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
154800     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
154900     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
155000     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
155100     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
155200     MOVE   "READ LONG AT END NOT END" TO FEATURE.                SQ1064.2
155300     MOVE   "SEQ-TEST-RD-19" TO PAR-NAME.                         SQ1064.2
155400     IF DELETE-SW NOT = SPACE                                     SQ1064.2
155500         GO TO   SEQ-DELETE-19.                                   SQ1064.2
155600     GO TO   SEQ-TEST-RD-19.                                      SQ1064.2
155700 SEQ-DELETE-19.                                                   SQ1064.2
155800     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
155900     GO TO   SEQ-DELETE-19-01.                                    SQ1064.2
156000*                                                                 SQ1064.2
156100*   EXECUTE THE READ STATEMENT                                    SQ1064.2
156200*                                                                 SQ1064.2
156300 SEQ-TEST-RD-19.                                                  SQ1064.2
156400     READ    SQ-VS6                                               SQ1064.2
156500        AT END                                                    SQ1064.2
156600                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
156700        NOT END                                                   SQ1064.2
156800                MOVE   "EXECUTED" TO NOT-EOF-FLAG.                SQ1064.2
156900     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
157000     GO TO   SEQ-TEST-RD-19-01.                                   SQ1064.2
157100 SEQ-DELETE-19-01.                                                SQ1064.2
157200     PERFORM DE-LETE.                                             SQ1064.2
157300     GO TO   SEQ-TEST-19-01-END.                                  SQ1064.2
157400*                                                                 SQ1064.2
157500*   CHECK THE FILE STATUS RETURNED                                SQ1064.2
157600*                                                                 SQ1064.2
157700 SEQ-TEST-RD-19-01.                                               SQ1064.2
157800     IF SQ-STATUS = "00"                                          SQ1064.2
157900         PERFORM PASS                                             SQ1064.2
158000     ELSE                                                         SQ1064.2
158100         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
158200         MOVE   "00" TO CORRECT-A                                 SQ1064.2
158300         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
158400         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
158500         PERFORM FAIL.                                            SQ1064.2
158600 SEQ-TEST-19-01-END.                                              SQ1064.2
158700*                                                                 SQ1064.2
158800*    CHECK THE IDENTITY OF THE RECORD RETURNED                    SQ1064.2
158900*                                                                 SQ1064.2
159000     ADD     1 TO REC-CT.                                         SQ1064.2
159100     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
159200         GO TO   SEQ-DELETE-19-02.                                SQ1064.2
159300     GO TO   SEQ-TEST-RD-19-02.                                   SQ1064.2
159400 SEQ-DELETE-19-02.                                                SQ1064.2
159500     PERFORM DE-LETE.                                             SQ1064.2
159600     GO TO   SEQ-TEST-19-02-END.                                  SQ1064.2
159700 SEQ-TEST-RD-19-02.                                               SQ1064.2
159800     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
159900         PERFORM PASS                                             SQ1064.2
160000     ELSE                                                         SQ1064.2
160100         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
160200         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
160300         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
160400         PERFORM FAIL.                                            SQ1064.2
160500 SEQ-TEST-19-02-END.                                              SQ1064.2
160600*                                                                 SQ1064.2
160700*    CHECK THE RECORD EXTENSION AREA                              SQ1064.2
160800*                                                                 SQ1064.2
160900     ADD     1 TO REC-CT.                                         SQ1064.2
161000     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1064.2
161100         GO TO SEQ-DELETE-19-03.                                  SQ1064.2
161200     GO TO   SEQ-TEST-RD-19-03.                                   SQ1064.2
161300 SEQ-DELETE-19-03.                                                SQ1064.2
161400     PERFORM DE-LETE.                                             SQ1064.2
161500     GO TO   SEQ-TEST-19-03-END.                                  SQ1064.2
161600 SEQ-TEST-RD-19-03.                                               SQ1064.2
161700     IF SQ-VS6R2-SECOND-L = BUFFER-COPY-SECOND-L                  SQ1064.2
161800         PERFORM PASS                                             SQ1064.2
161900     ELSE                                                         SQ1064.2
162000         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
162100         MOVE    BUFFER-COPY-SECOND-L TO CORRECT-A                SQ1064.2
162200         MOVE   "INCORRECT VALUE IN BUFFER EXTENSION" TO RE-MARK  SQ1064.2
162300         PERFORM FAIL.                                            SQ1064.2
162400 SEQ-TEST-19-03-END.                                              SQ1064.2
162500*                                                                 SQ1064.2
162600*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
162700*                                                                 SQ1064.2
162800     ADD     1 TO REC-CT.                                         SQ1064.2
162900     IF DELETE-SW NOT = SPACE                                     SQ1064.2
163000         GO TO   SEQ-DELETE-19-04.                                SQ1064.2
163100     GO TO   SEQ-TEST-RD-19-04.                                   SQ1064.2
163200 SEQ-DELETE-19-04.                                                SQ1064.2
163300     PERFORM DE-LETE.                                             SQ1064.2
163400     GO TO   SEQ-TEST-19-04-END.                                  SQ1064.2
163500 SEQ-TEST-RD-19-04.                                               SQ1064.2
163600     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
163700         PERFORM PASS                                             SQ1064.2
163800     ELSE                                                         SQ1064.2
163900         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
164000         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
164100         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
164200         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
164300         PERFORM FAIL.                                            SQ1064.2
164400 SEQ-TEST-19-04-END.                                              SQ1064.2
164500*                                                                 SQ1064.2
164600*    CHECK EXECUTION OF THE END PHRASE                            SQ1064.2
164700*                                                                 SQ1064.2
164800     ADD     1 TO REC-CT.                                         SQ1064.2
164900     IF DELETE-SW NOT = SPACE                                     SQ1064.2
165000         GO TO   SEQ-DELETE-19-05.                                SQ1064.2
165100     GO TO   SEQ-TEST-RD-19-05.                                   SQ1064.2
165200 SEQ-DELETE-19-05.                                                SQ1064.2
165300     PERFORM DE-LETE.                                             SQ1064.2
165400     GO TO   SEQ-TEST-19-05-END.                                  SQ1064.2
165500 SEQ-TEST-RD-19-05.                                               SQ1064.2
165600     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
165700         PERFORM PASS                                             SQ1064.2
165800     ELSE                                                         SQ1064.2
165900         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
166000         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
166100         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
166200         MOVE   "V11-46,4.4.4(10)" TO ANSI-REFERENCE              SQ1064.2
166300         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
166400         PERFORM FAIL.                                            SQ1064.2
166500 SEQ-TEST-19-05-END.                                              SQ1064.2
166600     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
166700*                                                                 SQ1064.2
166800*                                                                 SQ1064.2
166900*    READ A SHORT RECORD, USING READ ... END ... NOT END          SQ1064.2
167000*                                                                 SQ1064.2
167100 SEQ-INIT-20.                                                     SQ1064.2
167200     MOVE    1 TO REC-CT.                                         SQ1064.2
167300     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
167400     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
167500     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
167600     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
167700     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
167800     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
167900     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
168000     MOVE   "READ SHORT END NOT END" TO FEATURE.                  SQ1064.2
168100     MOVE   "SEQ-TEST-RD-20" TO PAR-NAME.                         SQ1064.2
168200     IF DELETE-SW NOT = SPACE                                     SQ1064.2
168300         GO TO   SEQ-DELETE-20.                                   SQ1064.2
168400     GO TO   SEQ-TEST-RD-20.                                      SQ1064.2
168500 SEQ-DELETE-20.                                                   SQ1064.2
168600     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
168700     GO TO   SEQ-DELETE-20-01.                                    SQ1064.2
168800*                                                                 SQ1064.2
168900*    EXECUTE THE READ STATEMENT                                   SQ1064.2
169000*                                                                 SQ1064.2
169100 SEQ-TEST-RD-20.                                                  SQ1064.2
169200     READ    SQ-VS6                                               SQ1064.2
169300        END                                                       SQ1064.2
169400                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
169500        NOT END                                                   SQ1064.2
169600                MOVE   "EXECUTED" TO NOT-EOF-FLAG.                SQ1064.2
169700     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
169800     GO TO   SEQ-TEST-RD-20-01.                                   SQ1064.2
169900*                                                                 SQ1064.2
170000*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
170100*                                                                 SQ1064.2
170200 SEQ-DELETE-20-01.                                                SQ1064.2
170300     PERFORM DE-LETE.                                             SQ1064.2
170400     GO TO   SEQ-TEST-20-01-END.                                  SQ1064.2
170500 SEQ-TEST-RD-20-01.                                               SQ1064.2
170600     IF SQ-STATUS = "00"                                          SQ1064.2
170700         PERFORM PASS                                             SQ1064.2
170800     ELSE                                                         SQ1064.2
170900         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
171000         MOVE   "00" TO CORRECT-A                                 SQ1064.2
171100         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
171200         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
171300         PERFORM FAIL.                                            SQ1064.2
171400 SEQ-TEST-20-01-END.                                              SQ1064.2
171500*                                                                 SQ1064.2
171600*    CHECK THE RECORD NUMBER OF THE RECORD READ                   SQ1064.2
171700*                                                                 SQ1064.2
171800     ADD     1 TO REC-CT.                                         SQ1064.2
171900     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
172000         GO TO   SEQ-DELETE-20-02.                                SQ1064.2
172100     GO TO   SEQ-TEST-RD-20-02.                                   SQ1064.2
172200 SEQ-DELETE-20-02.                                                SQ1064.2
172300     PERFORM DE-LETE.                                             SQ1064.2
172400     GO TO   SEQ-TEST-20-02-END.                                  SQ1064.2
172500 SEQ-TEST-RD-20-02.                                               SQ1064.2
172600     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
172700         PERFORM PASS                                             SQ1064.2
172800     ELSE                                                         SQ1064.2
172900         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
173000         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
173100         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
173200         PERFORM FAIL.                                            SQ1064.2
173300 SEQ-TEST-20-02-END.                                              SQ1064.2
173400*                                                                 SQ1064.2
173500*    CHECK THE EXTENDED PART OF THE RECORD                        SQ1064.2
173600*                                                                 SQ1064.2
173700     ADD     1 TO REC-CT.                                         SQ1064.2
173800*    IF DELETE-SW NOT = TO SPACE                                  SQ1064.2
173900*        GO TO SEQ-DELETE-20-03.                                  SQ1064.2
174000*    GO TO   SEQ-TEST-RD-20-03.                                   SQ1064.2
174100 SEQ-DELETE-20-03.                                                SQ1064.2
174200     PERFORM DE-LETE.                                             SQ1064.2
174300     GO TO   SEQ-TEST-20-03-END.                                  SQ1064.2
174400 SEQ-TEST-RD-20-03.                                               SQ1064.2
174500     IF SQ-VS6R2-SECOND-L NOT = BUFFER-COPY-SECOND-L              SQ1064.2
174600         PERFORM PASS                                             SQ1064.2
174700     ELSE                                                         SQ1064.2
174800         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
174900         MOVE   "**UNDEFINED**" TO CORRECT-A                      SQ1064.2
175000         MOVE   "BUFFER CONTENTS FOR LONG RECORD" TO RE-MARK      SQ1064.2
175100         PERFORM FAIL.                                            SQ1064.2
175200 SEQ-TEST-20-03-END.                                              SQ1064.2
175300*                                                                 SQ1064.2
175400*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
175500*                                                                 SQ1064.2
175600     ADD     1 TO REC-CT.                                         SQ1064.2
175700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
175800         GO TO   SEQ-DELETE-20-04.                                SQ1064.2
175900     GO TO   SEQ-TEST-RD-20-04.                                   SQ1064.2
176000 SEQ-DELETE-20-04.                                                SQ1064.2
176100     PERFORM DE-LETE.                                             SQ1064.2
176200     GO TO   SEQ-TEST-20-04-END.                                  SQ1064.2
176300 SEQ-TEST-RD-20-04.                                               SQ1064.2
176400     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
176500         PERFORM PASS                                             SQ1064.2
176600     ELSE                                                         SQ1064.2
176700         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
176800         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
176900         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
177000         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
177100         PERFORM FAIL.                                            SQ1064.2
177200 SEQ-TEST-20-04-END.                                              SQ1064.2
177300*                                                                 SQ1064.2
177400*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
177500*                                                                 SQ1064.2
177600     ADD     1 TO REC-CT.                                         SQ1064.2
177700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
177800         GO TO   SEQ-DELETE-20-05.                                SQ1064.2
177900     GO TO   SEQ-TEST-RD-20-05.                                   SQ1064.2
178000 SEQ-DELETE-20-05.                                                SQ1064.2
178100     PERFORM DE-LETE.                                             SQ1064.2
178200     GO TO   SEQ-TEST-20-05-END.                                  SQ1064.2
178300 SEQ-TEST-RD-20-05.                                               SQ1064.2
178400     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
178500         PERFORM PASS                                             SQ1064.2
178600     ELSE                                                         SQ1064.2
178700         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
178800         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
178900         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
179000         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
179100         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
179200         PERFORM FAIL.                                            SQ1064.2
179300 SEQ-TEST-20-05-END.                                              SQ1064.2
179400     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
179500*                                                                 SQ1064.2
179600*                                                                 SQ1064.2
179700*    READ A LONG RECORD,                                          SQ1064.2
179800*    USING READ ... AT END ... END-READ                           SQ1064.2
179900*                                                                 SQ1064.2
180000 SEQ-INIT-21.                                                     SQ1064.2
180100     MOVE    1 TO REC-CT.                                         SQ1064.2
180200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
180300     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
180400     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
180500     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
180600     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
180700     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
180800     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
180900     MOVE   "NOT EXECUTED"  TO END-READ-FLAG.                     SQ1064.2
181000     MOVE   "READ LONG AT END END-RD" TO FEATURE.                 SQ1064.2
181100     MOVE   "SEQ-TEST-RD-21" TO PAR-NAME.                         SQ1064.2
181200     IF DELETE-SW NOT = SPACE                                     SQ1064.2
181300         GO TO   SEQ-DELETE-21.                                   SQ1064.2
181400     GO TO   SEQ-TEST-RD-21.                                      SQ1064.2
181500 SEQ-DELETE-21.                                                   SQ1064.2
181600     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
181700     GO TO   SEQ-DELETE-21-01.                                    SQ1064.2
181800*                                                                 SQ1064.2
181900*   EXECUTE THE READ STATEMENT                                    SQ1064.2
182000*                                                                 SQ1064.2
182100 SEQ-TEST-RD-21.                                                  SQ1064.2
182200     READ    SQ-VS6                                               SQ1064.2
182300        AT END                                                    SQ1064.2
182400                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
182500     END-READ                                                     SQ1064.2
182600     MOVE   "EXECUTED" TO END-READ-FLAG.                          SQ1064.2
182700     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
182800     GO TO   SEQ-TEST-RD-21-01.                                   SQ1064.2
182900 SEQ-DELETE-21-01.                                                SQ1064.2
183000     PERFORM DE-LETE.                                             SQ1064.2
183100     GO TO   SEQ-TEST-21-01-END.                                  SQ1064.2
183200*                                                                 SQ1064.2
183300*   CHECK THE FILE STATUS RETURNED                                SQ1064.2
183400*                                                                 SQ1064.2
183500 SEQ-TEST-RD-21-01.                                               SQ1064.2
183600     IF SQ-STATUS = "00"                                          SQ1064.2
183700         PERFORM PASS                                             SQ1064.2
183800     ELSE                                                         SQ1064.2
183900         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
184000         MOVE   "00" TO CORRECT-A                                 SQ1064.2
184100         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
184200         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
184300         PERFORM FAIL.                                            SQ1064.2
184400 SEQ-TEST-21-01-END.                                              SQ1064.2
184500*                                                                 SQ1064.2
184600*    CHECK THE IDENTITY OF THE RECORD RETURNED                    SQ1064.2
184700*                                                                 SQ1064.2
184800     ADD     1 TO REC-CT.                                         SQ1064.2
184900     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
185000         GO TO   SEQ-DELETE-21-02.                                SQ1064.2
185100     GO TO   SEQ-TEST-RD-21-02.                                   SQ1064.2
185200 SEQ-DELETE-21-02.                                                SQ1064.2
185300     PERFORM DE-LETE.                                             SQ1064.2
185400     GO TO   SEQ-TEST-21-02-END.                                  SQ1064.2
185500 SEQ-TEST-RD-21-02.                                               SQ1064.2
185600     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
185700         PERFORM PASS                                             SQ1064.2
185800     ELSE                                                         SQ1064.2
185900         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
186000         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
186100         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
186200         PERFORM FAIL.                                            SQ1064.2
186300 SEQ-TEST-21-02-END.                                              SQ1064.2
186400*                                                                 SQ1064.2
186500*    CHECK THE RECORD EXTENSION AREA                              SQ1064.2
186600*                                                                 SQ1064.2
186700     ADD     1 TO REC-CT.                                         SQ1064.2
186800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1064.2
186900         GO TO SEQ-DELETE-21-03.                                  SQ1064.2
187000     GO TO   SEQ-TEST-RD-21-03.                                   SQ1064.2
187100 SEQ-DELETE-21-03.                                                SQ1064.2
187200     PERFORM DE-LETE.                                             SQ1064.2
187300     GO TO   SEQ-TEST-21-03-END.                                  SQ1064.2
187400 SEQ-TEST-RD-21-03.                                               SQ1064.2
187500     IF SQ-VS6R2-SECOND-L = BUFFER-COPY-SECOND-L                  SQ1064.2
187600         PERFORM PASS                                             SQ1064.2
187700     ELSE                                                         SQ1064.2
187800         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
187900         MOVE    BUFFER-COPY-SECOND-L TO CORRECT-A                SQ1064.2
188000         MOVE   "INCORRECT VALUE IN BUFFER EXTENSION" TO RE-MARK  SQ1064.2
188100         PERFORM FAIL.                                            SQ1064.2
188200 SEQ-TEST-21-03-END.                                              SQ1064.2
188300*                                                                 SQ1064.2
188400*    CHECK EXECUTION OF THE STATEMENT AFTER END-READ              SQ1064.2
188500*                                                                 SQ1064.2
188600     ADD     1 TO REC-CT.                                         SQ1064.2
188700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
188800         GO TO   SEQ-DELETE-21-04.                                SQ1064.2
188900     GO TO   SEQ-TEST-RD-21-04.                                   SQ1064.2
189000 SEQ-DELETE-21-04.                                                SQ1064.2
189100     PERFORM DE-LETE.                                             SQ1064.2
189200     GO TO   SEQ-TEST-21-04-END.                                  SQ1064.2
189300 SEQ-TEST-RD-21-04.                                               SQ1064.2
189400     IF END-READ-FLAG = "EXECUTED"                                SQ1064.2
189500         PERFORM PASS                                             SQ1064.2
189600     ELSE                                                         SQ1064.2
189700         MOVE    END-READ-FLAG TO COMPUTED-A                      SQ1064.2
189800         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
189900         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
190000         MOVE   "IV-40, VII-47,4.4.4(14)" TO ANSI-REFERENCE       SQ1064.2
190100         PERFORM FAIL.                                            SQ1064.2
190200 SEQ-TEST-21-04-END.                                              SQ1064.2
190300*                                                                 SQ1064.2
190400*    CHECK EXECUTION OF THE END PHRASE                            SQ1064.2
190500*                                                                 SQ1064.2
190600     ADD     1 TO REC-CT.                                         SQ1064.2
190700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
190800         GO TO   SEQ-DELETE-21-05.                                SQ1064.2
190900     GO TO   SEQ-TEST-RD-21-05.                                   SQ1064.2
191000 SEQ-DELETE-21-05.                                                SQ1064.2
191100     PERFORM DE-LETE.                                             SQ1064.2
191200     GO TO   SEQ-TEST-21-05-END.                                  SQ1064.2
191300 SEQ-TEST-RD-21-05.                                               SQ1064.2
191400     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
191500         PERFORM PASS                                             SQ1064.2
191600     ELSE                                                         SQ1064.2
191700         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
191800         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
191900         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
192000         MOVE   "V11-46,4.4.4(10)" TO ANSI-REFERENCE              SQ1064.2
192100         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
192200         PERFORM FAIL.                                            SQ1064.2
192300 SEQ-TEST-21-05-END.                                              SQ1064.2
192400     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
192500*                                                                 SQ1064.2
192600*                                                                 SQ1064.2
192700*    READ A LONG RECORD,                                          SQ1064.2
192800*    USING READ ... END ... END-READ                              SQ1064.2
192900*                                                                 SQ1064.2
193000 SEQ-INIT-22.                                                     SQ1064.2
193100     MOVE    1 TO REC-CT.                                         SQ1064.2
193200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
193300     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
193400     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
193500     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
193600     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
193700     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
193800     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
193900     MOVE   "NOT EXECUTED"  TO END-READ-FLAG.                     SQ1064.2
194000     MOVE   "READ LONG END END-READ" TO FEATURE.                  SQ1064.2
194100     MOVE   "SEQ-TEST-RD-22" TO PAR-NAME.                         SQ1064.2
194200     IF DELETE-SW NOT = SPACE                                     SQ1064.2
194300         GO TO   SEQ-DELETE-22.                                   SQ1064.2
194400     GO TO   SEQ-TEST-RD-22.                                      SQ1064.2
194500 SEQ-DELETE-22.                                                   SQ1064.2
194600     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
194700     GO TO   SEQ-DELETE-22-01.                                    SQ1064.2
194800*                                                                 SQ1064.2
194900*   EXECUTE THE READ STATEMENT                                    SQ1064.2
195000*                                                                 SQ1064.2
195100 SEQ-TEST-RD-22.                                                  SQ1064.2
195200     READ    SQ-VS6                                               SQ1064.2
195300        END                                                       SQ1064.2
195400                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
195500     END-READ                                                     SQ1064.2
195600     MOVE   "EXECUTED" TO END-READ-FLAG.                          SQ1064.2
195700     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
195800     GO TO   SEQ-TEST-RD-22-01.                                   SQ1064.2
195900 SEQ-DELETE-22-01.                                                SQ1064.2
196000     PERFORM DE-LETE.                                             SQ1064.2
196100     GO TO   SEQ-TEST-22-01-END.                                  SQ1064.2
196200*                                                                 SQ1064.2
196300*   CHECK THE FILE STATUS RETURNED                                SQ1064.2
196400*                                                                 SQ1064.2
196500 SEQ-TEST-RD-22-01.                                               SQ1064.2
196600     IF SQ-STATUS = "00"                                          SQ1064.2
196700         PERFORM PASS                                             SQ1064.2
196800     ELSE                                                         SQ1064.2
196900         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
197000         MOVE   "00" TO CORRECT-A                                 SQ1064.2
197100         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
197200         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
197300         PERFORM FAIL.                                            SQ1064.2
197400 SEQ-TEST-22-01-END.                                              SQ1064.2
197500*                                                                 SQ1064.2
197600*    CHECK THE IDENTITY OF THE RECORD RETURNED                    SQ1064.2
197700*                                                                 SQ1064.2
197800     ADD     1 TO REC-CT.                                         SQ1064.2
197900     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
198000         GO TO   SEQ-DELETE-22-02.                                SQ1064.2
198100     GO TO   SEQ-TEST-RD-22-02.                                   SQ1064.2
198200 SEQ-DELETE-22-02.                                                SQ1064.2
198300     PERFORM DE-LETE.                                             SQ1064.2
198400     GO TO   SEQ-TEST-22-02-END.                                  SQ1064.2
198500 SEQ-TEST-RD-22-02.                                               SQ1064.2
198600     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
198700         PERFORM PASS                                             SQ1064.2
198800     ELSE                                                         SQ1064.2
198900         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
199000         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
199100         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
199200         PERFORM FAIL.                                            SQ1064.2
199300 SEQ-TEST-22-02-END.                                              SQ1064.2
199400*                                                                 SQ1064.2
199500*    CHECK THE RECORD EXTENSION AREA                              SQ1064.2
199600*                                                                 SQ1064.2
199700     ADD     1 TO REC-CT.                                         SQ1064.2
199800     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1064.2
199900         GO TO SEQ-DELETE-22-03.                                  SQ1064.2
200000     GO TO   SEQ-TEST-RD-22-03.                                   SQ1064.2
200100 SEQ-DELETE-22-03.                                                SQ1064.2
200200     PERFORM DE-LETE.                                             SQ1064.2
200300     GO TO   SEQ-TEST-22-03-END.                                  SQ1064.2
200400 SEQ-TEST-RD-22-03.                                               SQ1064.2
200500     IF SQ-VS6R2-SECOND-L = BUFFER-COPY-SECOND-L                  SQ1064.2
200600         PERFORM PASS                                             SQ1064.2
200700     ELSE                                                         SQ1064.2
200800         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
200900         MOVE    BUFFER-COPY-SECOND-L TO CORRECT-A                SQ1064.2
201000         MOVE   "INCORRECT VALUE IN BUFFER EXTENSION" TO RE-MARK  SQ1064.2
201100         PERFORM FAIL.                                            SQ1064.2
201200 SEQ-TEST-22-03-END.                                              SQ1064.2
201300*                                                                 SQ1064.2
201400*    CHECK EXECUTION OF THE STATEMENT AFTER END-READ              SQ1064.2
201500*                                                                 SQ1064.2
201600     ADD     1 TO REC-CT.                                         SQ1064.2
201700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
201800         GO TO   SEQ-DELETE-22-04.                                SQ1064.2
201900     GO TO   SEQ-TEST-RD-22-04.                                   SQ1064.2
202000 SEQ-DELETE-22-04.                                                SQ1064.2
202100     PERFORM DE-LETE.                                             SQ1064.2
202200     GO TO   SEQ-TEST-22-04-END.                                  SQ1064.2
202300 SEQ-TEST-RD-22-04.                                               SQ1064.2
202400     IF END-READ-FLAG = "EXECUTED"                                SQ1064.2
202500         PERFORM PASS                                             SQ1064.2
202600     ELSE                                                         SQ1064.2
202700         MOVE    END-READ-FLAG TO COMPUTED-A                      SQ1064.2
202800         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
202900         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
203000         MOVE   "IV-40, VII-47,4.4.4(14)" TO ANSI-REFERENCE       SQ1064.2
203100         PERFORM FAIL.                                            SQ1064.2
203200 SEQ-TEST-22-04-END.                                              SQ1064.2
203300*                                                                 SQ1064.2
203400*    CHECK EXECUTION OF THE END PHRASE                            SQ1064.2
203500*                                                                 SQ1064.2
203600     ADD     1 TO REC-CT.                                         SQ1064.2
203700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
203800         GO TO   SEQ-DELETE-22-05.                                SQ1064.2
203900     GO TO   SEQ-TEST-RD-22-05.                                   SQ1064.2
204000 SEQ-DELETE-22-05.                                                SQ1064.2
204100     PERFORM DE-LETE.                                             SQ1064.2
204200     GO TO   SEQ-TEST-22-05-END.                                  SQ1064.2
204300 SEQ-TEST-RD-22-05.                                               SQ1064.2
204400     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
204500         PERFORM PASS                                             SQ1064.2
204600     ELSE                                                         SQ1064.2
204700         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
204800         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
204900         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
205000         MOVE   "V11-46,4.4.4(10)" TO ANSI-REFERENCE              SQ1064.2
205100         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
205200         PERFORM FAIL.                                            SQ1064.2
205300 SEQ-TEST-22-05-END.                                              SQ1064.2
205400     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
205500*                                                                 SQ1064.2
205600*                                                                 SQ1064.2
205700*    READ A LONG RECORD,                                          SQ1064.2
205800*    USING READ ... AT END ... NOT AT END ... END-READ            SQ1064.2
205900*                                                                 SQ1064.2
206000 SEQ-INIT-23.                                                     SQ1064.2
206100     MOVE    1 TO REC-CT.                                         SQ1064.2
206200     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
206300     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
206400     MOVE   "LONG" TO BUFFER-COPY-L-OR-S.                         SQ1064.2
206500     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
206600     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
206700     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
206800     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
206900     MOVE   "NOT EXECUTED"  TO END-READ-FLAG.                     SQ1064.2
207000     MOVE   "READ LONG AT END NAE E-R" TO FEATURE.                SQ1064.2
207100     MOVE   "SEQ-TEST-RD-23" TO PAR-NAME.                         SQ1064.2
207200     IF DELETE-SW NOT = SPACE                                     SQ1064.2
207300         GO TO   SEQ-DELETE-23.                                   SQ1064.2
207400     GO TO   SEQ-TEST-RD-23.                                      SQ1064.2
207500 SEQ-DELETE-23.                                                   SQ1064.2
207600     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
207700     GO TO   SEQ-DELETE-23-01.                                    SQ1064.2
207800*                                                                 SQ1064.2
207900*   EXECUTE THE READ STATEMENT                                    SQ1064.2
208000*                                                                 SQ1064.2
208100 SEQ-TEST-RD-23.                                                  SQ1064.2
208200     READ    SQ-VS6                                               SQ1064.2
208300        AT END                                                    SQ1064.2
208400                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
208500        NOT AT END                                                SQ1064.2
208600                MOVE   "EXECUTED" TO NOT-EOF-FLAG                 SQ1064.2
208700     END-READ                                                     SQ1064.2
208800     MOVE   "EXECUTED" TO END-READ-FLAG.                          SQ1064.2
208900     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
209000     GO TO   SEQ-TEST-RD-23-01.                                   SQ1064.2
209100 SEQ-DELETE-23-01.                                                SQ1064.2
209200     PERFORM DE-LETE.                                             SQ1064.2
209300     GO TO   SEQ-TEST-23-01-END.                                  SQ1064.2
209400*                                                                 SQ1064.2
209500*   CHECK THE FILE STATUS RETURNED                                SQ1064.2
209600*                                                                 SQ1064.2
209700 SEQ-TEST-RD-23-01.                                               SQ1064.2
209800     IF SQ-STATUS = "00"                                          SQ1064.2
209900         PERFORM PASS                                             SQ1064.2
210000     ELSE                                                         SQ1064.2
210100         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
210200         MOVE   "00" TO CORRECT-A                                 SQ1064.2
210300         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
210400         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
210500         PERFORM FAIL.                                            SQ1064.2
210600 SEQ-TEST-23-01-END.                                              SQ1064.2
210700*                                                                 SQ1064.2
210800*    CHECK THE IDENTITY OF THE RECORD RETURNED                    SQ1064.2
210900*                                                                 SQ1064.2
211000     ADD     1 TO REC-CT.                                         SQ1064.2
211100     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
211200         GO TO   SEQ-DELETE-23-02.                                SQ1064.2
211300     GO TO   SEQ-TEST-RD-23-02.                                   SQ1064.2
211400 SEQ-DELETE-23-02.                                                SQ1064.2
211500     PERFORM DE-LETE.                                             SQ1064.2
211600     GO TO   SEQ-TEST-23-02-END.                                  SQ1064.2
211700 SEQ-TEST-RD-23-02.                                               SQ1064.2
211800     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
211900         PERFORM PASS                                             SQ1064.2
212000     ELSE                                                         SQ1064.2
212100         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
212200         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
212300         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
212400         PERFORM FAIL.                                            SQ1064.2
212500 SEQ-TEST-23-02-END.                                              SQ1064.2
212600*                                                                 SQ1064.2
212700*    CHECK THE RECORD EXTENSION AREA                              SQ1064.2
212800*                                                                 SQ1064.2
212900     ADD     1 TO REC-CT.                                         SQ1064.2
213000     IF DELETE-SW NOT EQUAL TO SPACE                              SQ1064.2
213100         GO TO SEQ-DELETE-23-03.                                  SQ1064.2
213200     GO TO   SEQ-TEST-RD-23-03.                                   SQ1064.2
213300 SEQ-DELETE-23-03.                                                SQ1064.2
213400     PERFORM DE-LETE.                                             SQ1064.2
213500     GO TO   SEQ-TEST-23-03-END.                                  SQ1064.2
213600 SEQ-TEST-RD-23-03.                                               SQ1064.2
213700     IF SQ-VS6R2-SECOND-L = BUFFER-COPY-SECOND-L                  SQ1064.2
213800         PERFORM PASS                                             SQ1064.2
213900     ELSE                                                         SQ1064.2
214000         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
214100         MOVE    BUFFER-COPY-SECOND-L TO CORRECT-A                SQ1064.2
214200         MOVE   "INCORRECT VALUE IN BUFFER EXTENSION" TO RE-MARK  SQ1064.2
214300         PERFORM FAIL.                                            SQ1064.2
214400 SEQ-TEST-23-03-END.                                              SQ1064.2
214500*                                                                 SQ1064.2
214600*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
214700*                                                                 SQ1064.2
214800     ADD     1 TO REC-CT.                                         SQ1064.2
214900     IF DELETE-SW NOT = SPACE                                     SQ1064.2
215000         GO TO   SEQ-DELETE-23-04.                                SQ1064.2
215100     GO TO   SEQ-TEST-RD-23-04.                                   SQ1064.2
215200 SEQ-DELETE-23-04.                                                SQ1064.2
215300     PERFORM DE-LETE.                                             SQ1064.2
215400     GO TO   SEQ-TEST-23-04-END.                                  SQ1064.2
215500 SEQ-TEST-RD-23-04.                                               SQ1064.2
215600     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
215700         PERFORM PASS                                             SQ1064.2
215800     ELSE                                                         SQ1064.2
215900         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
216000         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
216100         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
216200         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
216300         PERFORM FAIL.                                            SQ1064.2
216400 SEQ-TEST-23-04-END.                                              SQ1064.2
216500*                                                                 SQ1064.2
216600*    CHECK EXECUTION OF THE STATEMENT AFTER END-READ              SQ1064.2
216700*                                                                 SQ1064.2
216800     ADD     1 TO REC-CT.                                         SQ1064.2
216900     IF DELETE-SW NOT = SPACE                                     SQ1064.2
217000         GO TO   SEQ-DELETE-23-05.                                SQ1064.2
217100     GO TO   SEQ-TEST-RD-23-05.                                   SQ1064.2
217200 SEQ-DELETE-23-05.                                                SQ1064.2
217300     PERFORM DE-LETE.                                             SQ1064.2
217400     GO TO   SEQ-TEST-23-05-END.                                  SQ1064.2
217500 SEQ-TEST-RD-23-05.                                               SQ1064.2
217600     IF END-READ-FLAG = "EXECUTED"                                SQ1064.2
217700         PERFORM PASS                                             SQ1064.2
217800     ELSE                                                         SQ1064.2
217900         MOVE    END-READ-FLAG TO COMPUTED-A                      SQ1064.2
218000         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
218100         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
218200         MOVE   "IV-40, VII-47,4.4.4(14)" TO ANSI-REFERENCE       SQ1064.2
218300         PERFORM FAIL.                                            SQ1064.2
218400 SEQ-TEST-23-05-END.                                              SQ1064.2
218500*                                                                 SQ1064.2
218600*    CHECK EXECUTION OF THE END PHRASE                            SQ1064.2
218700*                                                                 SQ1064.2
218800     ADD     1 TO REC-CT.                                         SQ1064.2
218900     IF DELETE-SW NOT = SPACE                                     SQ1064.2
219000         GO TO   SEQ-DELETE-23-06.                                SQ1064.2
219100     GO TO   SEQ-TEST-RD-23-06.                                   SQ1064.2
219200 SEQ-DELETE-23-06.                                                SQ1064.2
219300     PERFORM DE-LETE.                                             SQ1064.2
219400     GO TO   SEQ-TEST-23-06-END.                                  SQ1064.2
219500 SEQ-TEST-RD-23-06.                                               SQ1064.2
219600     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
219700         PERFORM PASS                                             SQ1064.2
219800     ELSE                                                         SQ1064.2
219900         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
220000         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
220100         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
220200         MOVE   "V11-46,4.4.4(10)" TO ANSI-REFERENCE              SQ1064.2
220300         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
220400         PERFORM FAIL.                                            SQ1064.2
220500 SEQ-TEST-23-06-END.                                              SQ1064.2
220600     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
220700*                                                                 SQ1064.2
220800*                                                                 SQ1064.2
220900*    READ A SHORT RECORD,                                         SQ1064.2
221000*    USING READ ... END ... NOT AT END ... END-READ               SQ1064.2
221100*                                                                 SQ1064.2
221200 SEQ-INIT-24.                                                     SQ1064.2
221300     MOVE    1 TO REC-CT.                                         SQ1064.2
221400     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
221500     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
221600     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
221700     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
221800     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
221900     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
222000     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
222100     MOVE   "NOT EXECUTED"  TO END-READ-FLAG.                     SQ1064.2
222200     MOVE   "READ SHORT END N A R E-R" TO FEATURE.                SQ1064.2
222300     MOVE   "SEQ-TEST-RD-24" TO PAR-NAME.                         SQ1064.2
222400     IF DELETE-SW NOT = SPACE                                     SQ1064.2
222500         GO TO   SEQ-DELETE-24.                                   SQ1064.2
222600     GO TO   SEQ-TEST-RD-24.                                      SQ1064.2
222700 SEQ-DELETE-24.                                                   SQ1064.2
222800     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
222900     GO TO   SEQ-DELETE-24-01.                                    SQ1064.2
223000*                                                                 SQ1064.2
223100*    EXECUTE THE READ STATEMENT                                   SQ1064.2
223200*                                                                 SQ1064.2
223300 SEQ-TEST-RD-24.                                                  SQ1064.2
223400     READ    SQ-VS6                                               SQ1064.2
223500        END                                                       SQ1064.2
223600                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
223700        NOT AT END                                                SQ1064.2
223800                MOVE   "EXECUTED" TO NOT-EOF-FLAG                 SQ1064.2
223900     END-READ                                                     SQ1064.2
224000     MOVE   "EXECUTED" TO END-READ-FLAG.                          SQ1064.2
224100     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
224200     GO TO   SEQ-TEST-RD-24-01.                                   SQ1064.2
224300*                                                                 SQ1064.2
224400*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
224500*                                                                 SQ1064.2
224600 SEQ-DELETE-24-01.                                                SQ1064.2
224700     PERFORM DE-LETE.                                             SQ1064.2
224800     GO TO   SEQ-TEST-24-01-END.                                  SQ1064.2
224900 SEQ-TEST-RD-24-01.                                               SQ1064.2
225000     IF SQ-STATUS = "00"                                          SQ1064.2
225100         PERFORM PASS                                             SQ1064.2
225200     ELSE                                                         SQ1064.2
225300         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
225400         MOVE   "00" TO CORRECT-A                                 SQ1064.2
225500         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
225600         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
225700         PERFORM FAIL.                                            SQ1064.2
225800 SEQ-TEST-24-01-END.                                              SQ1064.2
225900*                                                                 SQ1064.2
226000*    CHECK THE RECORD NUMBER OF THE RECORD READ                   SQ1064.2
226100*                                                                 SQ1064.2
226200     ADD     1 TO REC-CT.                                         SQ1064.2
226300     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
226400         GO TO   SEQ-DELETE-24-02.                                SQ1064.2
226500     GO TO   SEQ-TEST-RD-24-02.                                   SQ1064.2
226600 SEQ-DELETE-24-02.                                                SQ1064.2
226700     PERFORM DE-LETE.                                             SQ1064.2
226800     GO TO   SEQ-TEST-24-02-END.                                  SQ1064.2
226900 SEQ-TEST-RD-24-02.                                               SQ1064.2
227000     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
227100         PERFORM PASS                                             SQ1064.2
227200     ELSE                                                         SQ1064.2
227300         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
227400         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
227500         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
227600         PERFORM FAIL.                                            SQ1064.2
227700 SEQ-TEST-24-02-END.                                              SQ1064.2
227800*                                                                 SQ1064.2
227900*    CHECK THE EXTENDED PART OF THE RECORD                        SQ1064.2
228000*                                                                 SQ1064.2
228100     ADD     1 TO REC-CT.                                         SQ1064.2
228200*    IF DELETE-SW NOT = TO SPACE                                  SQ1064.2
228300*        GO TO SEQ-DELETE-24-03.                                  SQ1064.2
228400*    GO TO   SEQ-TEST-RD-24-03.                                   SQ1064.2
228500 SEQ-DELETE-24-03.                                                SQ1064.2
228600     PERFORM DE-LETE.                                             SQ1064.2
228700     GO TO   SEQ-TEST-24-03-END.                                  SQ1064.2
228800 SEQ-TEST-RD-24-03.                                               SQ1064.2
228900     IF SQ-VS6R2-SECOND-L NOT = BUFFER-COPY-SECOND-L              SQ1064.2
229000         PERFORM PASS                                             SQ1064.2
229100     ELSE                                                         SQ1064.2
229200         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
229300         MOVE   "**UNDEFINED**" TO CORRECT-A                      SQ1064.2
229400         MOVE   "BUFFER CONTENTS FOR LONG RECORD" TO RE-MARK      SQ1064.2
229500         PERFORM FAIL.                                            SQ1064.2
229600 SEQ-TEST-24-03-END.                                              SQ1064.2
229700*                                                                 SQ1064.2
229800*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
229900*                                                                 SQ1064.2
230000     ADD     1 TO REC-CT.                                         SQ1064.2
230100     IF DELETE-SW NOT = SPACE                                     SQ1064.2
230200         GO TO   SEQ-DELETE-24-04.                                SQ1064.2
230300     GO TO   SEQ-TEST-RD-24-04.                                   SQ1064.2
230400 SEQ-DELETE-24-04.                                                SQ1064.2
230500     PERFORM DE-LETE.                                             SQ1064.2
230600     GO TO   SEQ-TEST-24-04-END.                                  SQ1064.2
230700 SEQ-TEST-RD-24-04.                                               SQ1064.2
230800     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
230900         PERFORM PASS                                             SQ1064.2
231000     ELSE                                                         SQ1064.2
231100         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
231200         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
231300         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
231400         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
231500         PERFORM FAIL.                                            SQ1064.2
231600 SEQ-TEST-24-04-END.                                              SQ1064.2
231700*                                                                 SQ1064.2
231800*                                                                 SQ1064.2
231900*    CHECK EXECUTION OF THE STATEMENT AFTER END-READ              SQ1064.2
232000*                                                                 SQ1064.2
232100     ADD     1 TO REC-CT.                                         SQ1064.2
232200     IF DELETE-SW NOT = SPACE                                     SQ1064.2
232300         GO TO   SEQ-DELETE-24-05.                                SQ1064.2
232400     GO TO   SEQ-TEST-RD-24-05.                                   SQ1064.2
232500 SEQ-DELETE-24-05.                                                SQ1064.2
232600     PERFORM DE-LETE.                                             SQ1064.2
232700     GO TO   SEQ-TEST-24-05-END.                                  SQ1064.2
232800 SEQ-TEST-RD-24-05.                                               SQ1064.2
232900     IF END-READ-FLAG = "EXECUTED"                                SQ1064.2
233000         PERFORM PASS                                             SQ1064.2
233100     ELSE                                                         SQ1064.2
233200         MOVE    END-READ-FLAG TO COMPUTED-A                      SQ1064.2
233300         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
233400         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
233500         MOVE   "IV-40, VII-47,4.4.4(14)" TO ANSI-REFERENCE       SQ1064.2
233600         PERFORM FAIL.                                            SQ1064.2
233700 SEQ-TEST-24-05-END.                                              SQ1064.2
233800*                                                                 SQ1064.2
233900*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
234000*                                                                 SQ1064.2
234100     ADD     1 TO REC-CT.                                         SQ1064.2
234200     IF DELETE-SW NOT = SPACE                                     SQ1064.2
234300         GO TO   SEQ-DELETE-24-06.                                SQ1064.2
234400     GO TO   SEQ-TEST-RD-24-06.                                   SQ1064.2
234500 SEQ-DELETE-24-06.                                                SQ1064.2
234600     PERFORM DE-LETE.                                             SQ1064.2
234700     GO TO   SEQ-TEST-24-06-END.                                  SQ1064.2
234800 SEQ-TEST-RD-24-06.                                               SQ1064.2
234900     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
235000         PERFORM PASS                                             SQ1064.2
235100     ELSE                                                         SQ1064.2
235200         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
235300         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
235400         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
235500         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
235600         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
235700         PERFORM FAIL.                                            SQ1064.2
235800 SEQ-TEST-24-06-END.                                              SQ1064.2
235900     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
236000*                                                                 SQ1064.2
236100*                                                                 SQ1064.2
236200*    READ A SHORT RECORD,                                         SQ1064.2
236300*    USING READ ... AT END ... NOT END ... END-READ               SQ1064.2
236400*                                                                 SQ1064.2
236500 SEQ-INIT-25.                                                     SQ1064.2
236600     MOVE    1 TO REC-CT.                                         SQ1064.2
236700     ADD     1 TO XRECORD-NUMBER (1).                             SQ1064.2
236800     MOVE   "**********" TO SQ-VS6R2-SECOND-L.                    SQ1064.2
236900     MOVE   "SHORT" TO BUFFER-COPY-L-OR-S.                        SQ1064.2
237000     ADD     1 TO BUFFER-COPY-RECNO.                              SQ1064.2
237100     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
237200     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
237300     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
237400     MOVE   "NOT EXECUTED"  TO END-READ-FLAG.                     SQ1064.2
237500     MOVE   "READ SHORT A END N E E-R" TO FEATURE.                SQ1064.2
237600     MOVE   "SEQ-TEST-RD-25" TO PAR-NAME.                         SQ1064.2
237700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
237800         GO TO   SEQ-DELETE-25.                                   SQ1064.2
237900     GO TO   SEQ-TEST-RD-25.                                      SQ1064.2
238000 SEQ-DELETE-25.                                                   SQ1064.2
238100     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
238200     GO TO   SEQ-DELETE-25-01.                                    SQ1064.2
238300*                                                                 SQ1064.2
238400*    EXECUTE THE READ STATEMENT                                   SQ1064.2
238500*                                                                 SQ1064.2
238600 SEQ-TEST-RD-25.                                                  SQ1064.2
238700     READ    SQ-VS6                                               SQ1064.2
238800        AT END                                                    SQ1064.2
238900                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
239000        NOT END                                                   SQ1064.2
239100                MOVE   "EXECUTED" TO NOT-EOF-FLAG                 SQ1064.2
239200     END-READ                                                     SQ1064.2
239300     MOVE   "EXECUTED" TO END-READ-FLAG.                          SQ1064.2
239400     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
239500     GO TO   SEQ-TEST-RD-25-01.                                   SQ1064.2
239600*                                                                 SQ1064.2
239700*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
239800*                                                                 SQ1064.2
239900 SEQ-DELETE-25-01.                                                SQ1064.2
240000     PERFORM DE-LETE.                                             SQ1064.2
240100     GO TO   SEQ-TEST-25-01-END.                                  SQ1064.2
240200 SEQ-TEST-RD-25-01.                                               SQ1064.2
240300     IF SQ-STATUS = "00"                                          SQ1064.2
240400         PERFORM PASS                                             SQ1064.2
240500     ELSE                                                         SQ1064.2
240600         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
240700         MOVE   "00" TO CORRECT-A                                 SQ1064.2
240800         MOVE   "UNEXPECTED I-O STATUS ON READ" TO RE-MARK        SQ1064.2
240900         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
241000         PERFORM FAIL.                                            SQ1064.2
241100 SEQ-TEST-25-01-END.                                              SQ1064.2
241200*                                                                 SQ1064.2
241300*    CHECK THE RECORD NUMBER OF THE RECORD READ                   SQ1064.2
241400*                                                                 SQ1064.2
241500     ADD     1 TO REC-CT.                                         SQ1064.2
241600     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
241700         GO TO   SEQ-DELETE-25-02.                                SQ1064.2
241800     GO TO   SEQ-TEST-RD-25-02.                                   SQ1064.2
241900 SEQ-DELETE-25-02.                                                SQ1064.2
242000     PERFORM DE-LETE.                                             SQ1064.2
242100     GO TO   SEQ-TEST-25-02-END.                                  SQ1064.2
242200 SEQ-TEST-RD-25-02.                                               SQ1064.2
242300     IF XRECORD-NUMBER (1) = XRECORD-NUMBER (2)                   SQ1064.2
242400         PERFORM PASS                                             SQ1064.2
242500     ELSE                                                         SQ1064.2
242600         MOVE    XRECORD-NUMBER (2) TO COMPUTED-A                 SQ1064.2
242700         MOVE    XRECORD-NUMBER (1) TO CORRECT-A                  SQ1064.2
242800         MOVE   "UNEXPECTED RECORD RETURNED" TO RE-MARK           SQ1064.2
242900         PERFORM FAIL.                                            SQ1064.2
243000 SEQ-TEST-25-02-END.                                              SQ1064.2
243100*                                                                 SQ1064.2
243200*    CHECK THE EXTENDED PART OF THE RECORD                        SQ1064.2
243300*                                                                 SQ1064.2
243400     ADD     1 TO REC-CT.                                         SQ1064.2
243500*    IF DELETE-SW NOT = TO SPACE                                  SQ1064.2
243600*        GO TO SEQ-DELETE-25-03.                                  SQ1064.2
243700*    GO TO   SEQ-TEST-RD-25-03.                                   SQ1064.2
243800 SEQ-DELETE-25-03.                                                SQ1064.2
243900     PERFORM DE-LETE.                                             SQ1064.2
244000     GO TO   SEQ-TEST-25-03-END.                                  SQ1064.2
244100 SEQ-TEST-RD-25-03.                                               SQ1064.2
244200     IF SQ-VS6R2-SECOND-L NOT = BUFFER-COPY-SECOND-L              SQ1064.2
244300         PERFORM PASS                                             SQ1064.2
244400     ELSE                                                         SQ1064.2
244500         MOVE    SQ-VS6R2-SECOND-L TO COMPUTED-A                  SQ1064.2
244600         MOVE   "**UNDEFINED**" TO CORRECT-A                      SQ1064.2
244700         MOVE   "BUFFER CONTENTS FOR LONG RECORD" TO RE-MARK      SQ1064.2
244800         PERFORM FAIL.                                            SQ1064.2
244900 SEQ-TEST-25-03-END.                                              SQ1064.2
245000*                                                                 SQ1064.2
245100*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
245200*                                                                 SQ1064.2
245300     ADD     1 TO REC-CT.                                         SQ1064.2
245400     IF DELETE-SW NOT = SPACE                                     SQ1064.2
245500         GO TO   SEQ-DELETE-25-04.                                SQ1064.2
245600     GO TO   SEQ-TEST-RD-25-04.                                   SQ1064.2
245700 SEQ-DELETE-25-04.                                                SQ1064.2
245800     PERFORM DE-LETE.                                             SQ1064.2
245900     GO TO   SEQ-TEST-25-04-END.                                  SQ1064.2
246000 SEQ-TEST-RD-25-04.                                               SQ1064.2
246100     IF NOT-EOF-FLAG = "EXECUTED"                                 SQ1064.2
246200         PERFORM PASS                                             SQ1064.2
246300     ELSE                                                         SQ1064.2
246400         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
246500         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
246600         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
246700         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
246800         PERFORM FAIL.                                            SQ1064.2
246900 SEQ-TEST-25-04-END.                                              SQ1064.2
247000*                                                                 SQ1064.2
247100*                                                                 SQ1064.2
247200*    CHECK EXECUTION OF THE STATEMENT AFTER END-READ              SQ1064.2
247300*                                                                 SQ1064.2
247400     ADD     1 TO REC-CT.                                         SQ1064.2
247500     IF DELETE-SW NOT = SPACE                                     SQ1064.2
247600         GO TO   SEQ-DELETE-25-05.                                SQ1064.2
247700     GO TO   SEQ-TEST-RD-25-05.                                   SQ1064.2
247800 SEQ-DELETE-25-05.                                                SQ1064.2
247900     PERFORM DE-LETE.                                             SQ1064.2
248000     GO TO   SEQ-TEST-25-05-END.                                  SQ1064.2
248100 SEQ-TEST-RD-25-05.                                               SQ1064.2
248200     IF END-READ-FLAG = "EXECUTED"                                SQ1064.2
248300         PERFORM PASS                                             SQ1064.2
248400     ELSE                                                         SQ1064.2
248500         MOVE    END-READ-FLAG TO COMPUTED-A                      SQ1064.2
248600         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
248700         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
248800         MOVE   "IV-40, VII-47,4.4.4(14)" TO ANSI-REFERENCE       SQ1064.2
248900         PERFORM FAIL.                                            SQ1064.2
249000 SEQ-TEST-25-05-END.                                              SQ1064.2
249100*                                                                 SQ1064.2
249200*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
249300*                                                                 SQ1064.2
249400     ADD     1 TO REC-CT.                                         SQ1064.2
249500     IF DELETE-SW NOT = SPACE                                     SQ1064.2
249600         GO TO   SEQ-DELETE-25-06.                                SQ1064.2
249700     GO TO   SEQ-TEST-RD-25-06.                                   SQ1064.2
249800 SEQ-DELETE-25-06.                                                SQ1064.2
249900     PERFORM DE-LETE.                                             SQ1064.2
250000     GO TO   SEQ-TEST-25-06-END.                                  SQ1064.2
250100 SEQ-TEST-RD-25-06.                                               SQ1064.2
250200     IF EOF-FLAG = "NOT EXECUTED"                                 SQ1064.2
250300         PERFORM PASS                                             SQ1064.2
250400     ELSE                                                         SQ1064.2
250500         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
250600         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
250700         MOVE   "AT END BRANCH TAKEN BEFORE EOF" TO RE-MARK       SQ1064.2
250800         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
250900         MOVE   "*" TO DELETE-SW-2                                SQ1064.2
251000         PERFORM FAIL.                                            SQ1064.2
251100 SEQ-TEST-25-06-END.                                              SQ1064.2
251200     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
251300*                                                                 SQ1064.2
251400*                                                                 SQ1064.2
251500*    READ AT END OF FILE, RAISING EOF CONDITION                   SQ1064.2
251600*    USING READ ... END ... NOT END ... END-READ                  SQ1064.2
251700*                                                                 SQ1064.2
251800 SEQ-INIT-26.                                                     SQ1064.2
251900     MOVE    1 TO REC-CT.                                         SQ1064.2
252000     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
252100     MOVE   "NOT EXECUTED"  TO EOF-FLAG.                          SQ1064.2
252200     MOVE   "NOT EXECUTED"  TO NOT-EOF-FLAG.                      SQ1064.2
252300     MOVE   "NOT EXECUTED"  TO END-READ-FLAG.                     SQ1064.2
252400     MOVE   "READ SHORT A END N E E-R" TO FEATURE.                SQ1064.2
252500     MOVE   "SEQ-TEST-RD-26" TO PAR-NAME.                         SQ1064.2
252600     IF DELETE-SW NOT = SPACE                                     SQ1064.2
252700         GO TO   SEQ-DELETE-26.                                   SQ1064.2
252800     GO TO   SEQ-TEST-RD-26.                                      SQ1064.2
252900 SEQ-DELETE-26.                                                   SQ1064.2
253000     MOVE   "*" TO DELETE-SW-3.                                   SQ1064.2
253100     GO TO   SEQ-DELETE-26-01.                                    SQ1064.2
253200*                                                                 SQ1064.2
253300*    EXECUTE THE READ STATEMENT                                   SQ1064.2
253400*                                                                 SQ1064.2
253500 SEQ-TEST-RD-26.                                                  SQ1064.2
253600     READ    SQ-VS6                                               SQ1064.2
253700        END                                                       SQ1064.2
253800                MOVE   "EXECUTED" TO EOF-FLAG                     SQ1064.2
253900        NOT END                                                   SQ1064.2
254000                MOVE   "EXECUTED" TO NOT-EOF-FLAG                 SQ1064.2
254100     END-READ                                                     SQ1064.2
254200     MOVE   "EXECUTED" TO END-READ-FLAG.                          SQ1064.2
254300     MOVE    SQ-VS6R1-M-G-120 TO FILE-RECORD-INFO-P1-120 (2).     SQ1064.2
254400     GO TO   SEQ-TEST-RD-26-01.                                   SQ1064.2
254500*                                                                 SQ1064.2
254600*    CHECK THE FILE STATUS VALUE                                  SQ1064.2
254700*                                                                 SQ1064.2
254800 SEQ-DELETE-26-01.                                                SQ1064.2
254900     PERFORM DE-LETE.                                             SQ1064.2
255000     GO TO   SEQ-TEST-26-01-END.                                  SQ1064.2
255100 SEQ-TEST-RD-26-01.                                               SQ1064.2
255200     IF SQ-STATUS = "10"                                          SQ1064.2
255300         PERFORM PASS                                             SQ1064.2
255400     ELSE                                                         SQ1064.2
255500         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
255600         MOVE   "10" TO CORRECT-A                                 SQ1064.2
255700         MOVE   "I-O STATUS FOR EOF NOT RETURNED" TO RE-MARK      SQ1064.2
255800         MOVE   "VII-3, VII-44,4.4.4(3)" TO ANSI-REFERENCE        SQ1064.2
255900         PERFORM FAIL.                                            SQ1064.2
256000 SEQ-TEST-26-01-END.                                              SQ1064.2
256100*                                                                 SQ1064.2
256200*                                                                 SQ1064.2
256300*    CHECK EXECUTION OF THE NOT END PATH                          SQ1064.2
256400*                                                                 SQ1064.2
256500     ADD     1 TO REC-CT.                                         SQ1064.2
256600     IF DELETE-SW NOT = SPACE                                     SQ1064.2
256700         GO TO   SEQ-DELETE-26-02.                                SQ1064.2
256800     GO TO   SEQ-TEST-RD-26-02.                                   SQ1064.2
256900 SEQ-DELETE-26-02.                                                SQ1064.2
257000     PERFORM DE-LETE.                                             SQ1064.2
257100     GO TO   SEQ-TEST-26-02-END.                                  SQ1064.2
257200 SEQ-TEST-RD-26-02.                                               SQ1064.2
257300     IF NOT-EOF-FLAG = "NOT EXECUTED"                             SQ1064.2
257400         PERFORM PASS                                             SQ1064.2
257500     ELSE                                                         SQ1064.2
257600         MOVE    NOT-EOF-FLAG TO COMPUTED-A                       SQ1064.2
257700         MOVE   "NOT EXECUTED" TO CORRECT-A                       SQ1064.2
257800         MOVE   "NOT END PATH EXECUTED AT EOF" TO RE-MARK         SQ1064.2
257900         MOVE   "VII-46,4.4.4(11)" TO ANSI-REFERENCE              SQ1064.2
258000         PERFORM FAIL.                                            SQ1064.2
258100 SEQ-TEST-26-02-END.                                              SQ1064.2
258200*                                                                 SQ1064.2
258300*                                                                 SQ1064.2
258400*    CHECK EXECUTION OF THE STATEMENT AFTER END-READ              SQ1064.2
258500*                                                                 SQ1064.2
258600     ADD     1 TO REC-CT.                                         SQ1064.2
258700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
258800         GO TO   SEQ-DELETE-26-03.                                SQ1064.2
258900     GO TO   SEQ-TEST-RD-26-03.                                   SQ1064.2
259000 SEQ-DELETE-26-03.                                                SQ1064.2
259100     PERFORM DE-LETE.                                             SQ1064.2
259200     GO TO   SEQ-TEST-26-03-END.                                  SQ1064.2
259300 SEQ-TEST-RD-26-03.                                               SQ1064.2
259400     IF END-READ-FLAG = "EXECUTED"                                SQ1064.2
259500         PERFORM PASS                                             SQ1064.2
259600     ELSE                                                         SQ1064.2
259700         MOVE    END-READ-FLAG TO COMPUTED-A                      SQ1064.2
259800         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
259900         MOVE   "NOT END PATH NOT EXECUTED" TO RE-MARK            SQ1064.2
260000         MOVE   "IV-40, VII-47,4.4.4(14)" TO ANSI-REFERENCE       SQ1064.2
260100         PERFORM FAIL.                                            SQ1064.2
260200 SEQ-TEST-26-03-END.                                              SQ1064.2
260300*                                                                 SQ1064.2
260400*    CHECK EXECUTION OF THE END PATH                              SQ1064.2
260500*                                                                 SQ1064.2
260600     ADD     1 TO REC-CT.                                         SQ1064.2
260700     IF DELETE-SW NOT = SPACE                                     SQ1064.2
260800         GO TO   SEQ-DELETE-26-04.                                SQ1064.2
260900     GO TO   SEQ-TEST-RD-26-04.                                   SQ1064.2
261000 SEQ-DELETE-26-04.                                                SQ1064.2
261100     PERFORM DE-LETE.                                             SQ1064.2
261200     GO TO   SEQ-TEST-26-04-END.                                  SQ1064.2
261300 SEQ-TEST-RD-26-04.                                               SQ1064.2
261400     IF EOF-FLAG = "EXECUTED"                                     SQ1064.2
261500         PERFORM PASS                                             SQ1064.2
261600     ELSE                                                         SQ1064.2
261700         MOVE    EOF-FLAG TO COMPUTED-A                           SQ1064.2
261800         MOVE   "EXECUTED" TO CORRECT-A                           SQ1064.2
261900         MOVE   "AT END BRANCH NOT TAKEN AT EOF" TO RE-MARK       SQ1064.2
262000         MOVE   "V11-46, 4.4.4(10)" TO ANSI-REFERENCE             SQ1064.2
262100         PERFORM FAIL.                                            SQ1064.2
262200 SEQ-TEST-26-04-END.                                              SQ1064.2
262300     MOVE    SPACE TO DELETE-SW-3.                                SQ1064.2
262400*                                                                 SQ1064.2
262500*                                                                 SQ1064.2
262600*    THE END OF THE FILE HAS BEEN REACHED, SO IT CAN BE CLOSED    SQ1064.2
262700*                                                                 SQ1064.2
262800 SEQ-INIT-27.                                                     SQ1064.2
262900     MOVE    1 TO REC-CT.                                         SQ1064.2
263000     MOVE   "CLOSE FILE AFTER READING" TO FEATURE.                SQ1064.2
263100     MOVE   "SEQ-TEST-CL-27" TO PAR-NAME.                         SQ1064.2
263200     MOVE   "**" TO SQ-STATUS.                                    SQ1064.2
263300     IF DELETE-SW NOT EQUAL SPACE                                 SQ1064.2
263400         GO TO SEQ-DELETE-27.                                     SQ1064.2
263500     GO TO SEQ-TEST-CL-27.                                        SQ1064.2
263600 SEQ-DELETE-27.                                                   SQ1064.2
263700     GO TO   SEQ-DELETE-27-01.                                    SQ1064.2
263800 SEQ-TEST-CL-27.                                                  SQ1064.2
263900     CLOSE   SQ-VS6.                                              SQ1064.2
264000     GO TO   SEQ-TEST-CL-27-01.                                   SQ1064.2
264100 SEQ-DELETE-27-01.                                                SQ1064.2
264200     PERFORM DE-LETE.                                             SQ1064.2
264300     GO TO   SEQ-TEST-27-01-END.                                  SQ1064.2
264400 SEQ-TEST-CL-27-01.                                               SQ1064.2
264500     IF SQ-STATUS = "00"                                          SQ1064.2
264600         PERFORM PASS                                             SQ1064.2
264700     ELSE                                                         SQ1064.2
264800         MOVE    SQ-STATUS TO COMPUTED-A                          SQ1064.2
264900         MOVE   "00" TO CORRECT-A                                 SQ1064.2
265000         MOVE   "UNEXPECTED I-O STATUS FROM CLOSE" TO RE-MARK     SQ1064.2
265100         MOVE   "VII-3, VII-38,4.2.4(4)" TO ANSI-REFERENCE        SQ1064.2
265200         PERFORM FAIL.                                            SQ1064.2
265300 SEQ-TEST-27-01-END.                                              SQ1064.2
265400*                                                                 SQ1064.2
265500*                                                                 SQ1064.2
265600 TERMINATE-ROUTINE.                                               SQ1064.2
265700     EXIT.                                                        SQ1064.2
265800 CCVS-EXIT SECTION.                                               SQ1064.2
265900 CCVS-999999.                                                     SQ1064.2
266000     GO TO CLOSE-FILES.                                           SQ1064.2
      *END-OF,SQ106A                                                                  
