      *HEADER,COBOL,SQ101M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1014.2
000200 PROGRAM-ID.                                                      SQ1014.2
000300     SQ101M.                                                      SQ1014.2
000400****************************************************************  SQ1014.2
000500*                                                              *  SQ1014.2
000600*    VALIDATION FOR:-                                          *  SQ1014.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1014.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1014.2
000900*                             REVISED 1986, AUGUST             *  SQ1014.2
001000*                                                              *  SQ1014.2
001100*    CREATION DATE     /     VALIDATION DATE                   *  SQ1014.2
001200*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1014.2
001300*                                                              *  SQ1014.2
001400****************************************************************  SQ1014.2
001500*                                                              *  SQ1014.2
001600*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SQ1014.2
001700*                                                              *  SQ1014.2
001800*            X-55   SYSTEM PRINTER                             *  SQ1014.2
001900*            X-82   SOURCE-COMPUTER                            *  SQ1014.2
002000*            X-83   OBJECT-COMPUTER.                           *  SQ1014.2
002100*                                                              *  SQ1014.2
002200****************************************************************  SQ1014.2
002300*                                                              *  SQ1014.2
002400*    SQ101M TESTS THE LEVEL 1 FACILITIES FOR CONTROL OF THE    *  SQ1014.2
002500*    POSITION OF LINES ON A PRINTED PAGE.  THE PRINCIPAL       *  SQ1014.2
002600*    FACILITY IS THE ADVANCING PHRASE OF THE WRITE STATEMENT   *  SQ1014.2
002700*    WHEN THE ASSOCIATED PHYSICAL FILE IS DESIGNATED AS A      *  SQ1014.2
002800*    PRINTER.  THE FROM PHRASE OF THE WRITE STATEMENT IS ALSO  *  SQ1014.2
002900*    TESTED.                                                   *  SQ1014.2
003000*                                                              *  SQ1014.2
003100*    ALL POSSIBLE LEVEL 1 COMBINATIONS OF THE FORMAT OF THE    *  SQ1014.2
003200*    ADVANCING PHRASE ARE TESTED WITH AND WITHOUT THE FROM     *  SQ1014.2
003300*    PHRASE.  THE VALUES USED FOR INTEGER INCLUDE ONE-DIGIT    *  SQ1014.2
003400*    AND TWO-DIGIT UNSIGNED NUMERIC LITERALS, EIGHTEEN DIGIT   *  SQ1014.2
003500*    NUMERIC LITERALS WITH LEADING ZEROS, AND THE FIGURATIVE   *  SQ1014.2
003600*    CONSTANT ZERO.  DATA ITEMS USED AS IDENTIFIER-2 INCLUDE   *  SQ1014.2
003700*    LEVEL 77, LEVEL 01, AND SUBORDINATE ELEMENTARY ITEMS.  A  *  SQ1014.2
003800*    SIMILAR RANGE OF TYPES, BUT INCLUDING GROUP ITEMS, IS     *  SQ1014.2
003900*    USED FOR IDENTIFIER-2.                                    *  SQ1014.2
004000*                                                              *  SQ1014.2
004100*    IN ADDITION TO THE ABOVE TESTS, A TEST IS MADE TO ENSURE  *  SQ1014.2
004200*    THAT CHARACTERS DESIGNATED TO BE PRINTED IN COLUMN 1 DO   *  SQ1014.2
004300*    NOT ACT AS CARRIAGE CONTROL CHARACTERS.                   *  SQ1014.2
004400*                                                              *  SQ1014.2
004500*    BECAUSE OF THE NATURE OF THESE TESTS A "PASS" OR "FAIL"   *  SQ1014.2
004600*    CANNOT BE DETERMINED WITHIN THE PROGRAM.  THE PRINTED     *  SQ1014.2
004700*    OUTPUT MUST BE EXAMINED TO DETERMINE WHETHER EACH TEST    *  SQ1014.2
004800*    HAS BEEN PASSED OR FAILED.                                *  SQ1014.2
004900*                                                              *  SQ1014.2
005000****************************************************************  SQ1014.2
005100*                                                                 SQ1014.2
005200*                                                                 SQ1014.2
005300 ENVIRONMENT DIVISION.                                            SQ1014.2
005400 CONFIGURATION SECTION.                                           SQ1014.2
005500 SOURCE-COMPUTER.                                                 SQ1014.2
005600     XXXXX082.                                                    SQ1014.2
005700 OBJECT-COMPUTER.                                                 SQ1014.2
005800     XXXXX083.                                                    SQ1014.2
005900*                                                                 SQ1014.2
006000 INPUT-OUTPUT SECTION.                                            SQ1014.2
006100 FILE-CONTROL.                                                    SQ1014.2
006200     SELECT PRINT-FILE ASSIGN TO                                  SQ1014.2
006300     XXXXX055.                                                    SQ1014.2
006400*                                                                 SQ1014.2
006500*    SELECT RAW-DATA   ASSIGN TO                                  SQ1014.2
006600*    XXXXX062                                                     SQ1014.2
006700*          ORGANIZATION IS INDEXED                                SQ1014.2
006800*          ACCESS MODE  IS RANDOM                                 SQ1014.2
006900*          RECORD-KEY   IS RAW-DATA-KEY.                          SQ1014.2
007000*                                                                 SQ1014.2
007100*                                                                 SQ1014.2
007200 DATA DIVISION.                                                   SQ1014.2
007300 FILE SECTION.                                                    SQ1014.2
007400 FD  PRINT-FILE                                                   SQ1014.2
007500C    LABEL RECORDS                                                SQ1014.2
007600C    XXXXX084                                                     SQ1014.2
007700C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1014.2
007800               .                                                  SQ1014.2
007900 01  PRINT-REC    PICTURE X(120).                                 SQ1014.2
008000 01  DUMMY-RECORD PICTURE X(120).                                 SQ1014.2
008100*                                                                 SQ1014.2
008200*FD  RAW-DATA.                                                    SQ1014.2
008300*01  RAW-DATA-SATZ.                                               SQ1014.2
008400*    05  RAW-DATA-KEY        PIC X(6).                            SQ1014.2
008500*    05  C-DATE              PIC 9(6).                            SQ1014.2
008600*    05  C-TIME              PIC 9(8).                            SQ1014.2
008700*    05  NO-OF-TESTS         PIC 99.                              SQ1014.2
008800*    05  C-OK                PIC 999.                             SQ1014.2
008900*    05  C-ALL               PIC 999.                             SQ1014.2
009000*    05  C-FAIL              PIC 999.                             SQ1014.2
009100*    05  C-DELETED           PIC 999.                             SQ1014.2
009200*    05  C-INSPECT           PIC 999.                             SQ1014.2
009300*    05  C-NOTE              PIC X(13).                           SQ1014.2
009400*    05  C-INDENT            PIC X.                               SQ1014.2
009500*    05  C-ABORT             PIC X(8).                            SQ1014.2
009600*                                                                 SQ1014.2
009700*                                                                 SQ1014.2
009800 WORKING-STORAGE SECTION.                                         SQ1014.2
009900*                                                                 SQ1014.2
010000***************************************************************   SQ1014.2
010100*                                                             *   SQ1014.2
010200*    WORKING-STORAGE DATA ITEMS SPECIFIC TO THIS TEST SUITE   *   SQ1014.2
010300*                                                             *   SQ1014.2
010400***************************************************************   SQ1014.2
010500*                                                                 SQ1014.2
010600 77  QU-OTE             PIC X   VALUE QUOTE.                      SQ1014.2
010700 77  IDENTIFIER-2       PIC 99       VALUE 0.                     SQ1014.2
010800 77  LONG-ZERO          PIC 9(18)    VALUE 0.                     SQ1014.2
010900 77  LONG-ONE           PIC 9(18)    VALUE 1.                     SQ1014.2
011000 01  IDENT-2-S99        PIC S99.                                  SQ1014.2
011100 01  IDENT-2-C99        PIC 99  COMP.                             SQ1014.2
011200*                                                                 SQ1014.2
011300 01  ONE                PIC 9        VALUE 1.                     SQ1014.2
011400*                                                                 SQ1014.2
011500 01  SPACING-VALUES-1.                                            SQ1014.2
011600     02 TWO               PIC 9      VALUE 2.                     SQ1014.2
011700     02 SPACING-VALUES-2.                                         SQ1014.2
011800        03 THREE            PIC 9    VALUE 3.                     SQ1014.2
011900        03 SPACING-VALUES-3.                                      SQ1014.2
012000           04 FOUR            PIC 9  VALUE 4.                     SQ1014.2
012100*                                                                 SQ1014.2
012200 01  CHAR-LINE.                                                   SQ1014.2
012300     03  LIN-CH   PIC X.                                          SQ1014.2
012400     03  FILLER   PIC X.                                          SQ1014.2
012500     03  LIN-SER  PIC 999.                                        SQ1014.2
012600     03  FILLER   PIC X(115).                                     SQ1014.2
012700*                                                                 SQ1014.2
012800 77  SEVENTY-SEVEN     PIC X(120)   VALUE "  THIS WAS WRITTEN FROMSQ1014.2
012900-    " A 77 LEVEL ENTRY.  IT SHOULD BE 2 LINES BELOW AND 1 LINE ABSQ1014.2
013000-    "OVE THE BRACKETING WRT-TEST LINES.".                        SQ1014.2
013100*                                                                 SQ1014.2
013200 77  SEVENTY-SEVEN-2    PIC X(120)   VALUE "THIS LINE WAS WRITTEN SQ1014.2
013300-    "FROM A 77 LEVEL ENTRY.  IT SHOULD BE 7 LINES BELOW AND 1 LINSQ1014.2
013400-    "E ABOVE THE BRACKETING WRT-TEST LINES.".                    SQ1014.2
013500*                                                                 SQ1014.2
013600 01  OH-ONE             PIC X(120)   VALUE "THIS LINE WAS WRITTEN SQ1014.2
013700-    "FROM AN 01 LEVEL ENTRY. IT SHOULD BE 1 LINE BELOW AND 5 LINESQ1014.2
013800-    "S ABOVE THE BRACKETING WRT-TEST LINES.".                    SQ1014.2
013900*                                                                 SQ1014.2
014000 01  LEVEL-1.                                                     SQ1014.2
014100     03 OH-THREE        PIC X(120)   VALUE "THIS LINE WAS WRITTEN SQ1014.2
014200-       "FROM AN 03 LEVEL ENTRY. IT SHOULD BE 1 LINE BELOW AND 6 LSQ1014.2
014300-       "INES ABOVE THE BRACKETING WRT-TEST LINES.".              SQ1014.2
014400*                                                                 SQ1014.2
014500 01  TEST-LINE-1.                                                 SQ1014.2
014600     02 FILLER          PIC X(20)  VALUE SPACE.                   SQ1014.2
014700     02 FILLER          PIC X(20)  VALUE "THIS LINE SHOULD BE ".  SQ1014.2
014800     02 LINES-BELOW-1   PIC XX.                                   SQ1014.2
014900     02 FILLER          PIC X(17)  VALUE " LINES BELOW AND ".     SQ1014.2
015000     02 LINES-ABOVE-1   PIC XX.                                   SQ1014.2
015100     02 FILLER          PIC X(59)  VALUE                          SQ1014.2
015200                  " LINES ABOVE THE BRACKETING WRT-TEST LINES".   SQ1014.2
015300*                                                                 SQ1014.2
015400 01  LEVEL-ONE.                                                   SQ1014.2
015500     02 LEVEL-TWO.                                                SQ1014.2
015600        03 TEST-LINE-2.                                           SQ1014.2
015700           04 FILLER          PIC X(20)   VALUE SPACE.            SQ1014.2
015800           04 FILLER          PIC X(20)   VALUE "THIS LINE SHOULD SQ1014.2
015900-             "BE ".                                              SQ1014.2
016000           04 LINES-BELOW-2   PIC XX.                             SQ1014.2
016100           04 FILLER          PIC X(17) VALUE " LINES BELOW AND ".SQ1014.2
016200           04 LINES-ABOVE-2   PIC XX.                             SQ1014.2
016300           04 FILLER          PIC X(59)   VALUE                   SQ1014.2
016400                  " LINES ABOVE THE BRACKETING WRT-TEST LINES".   SQ1014.2
016500*                                                                 SQ1014.2
016600 01  OVERPRINTED-LINE.                                            SQ1014.2
016700     02 FILLER            PIC X(20)  VALUE SPACE.                 SQ1014.2
016800     02 FILLER            PIC X(9)   VALUE "WRT-TEST-".           SQ1014.2
016900     02 OVERPRINTED-TEST  PIC XX.                                 SQ1014.2
017000     02 FILLER            PIC X(89)  VALUE "/ THIS LINE SHOULD BE SQ1014.2
017100-       "OVERPRINTED. AAAAAAA".                                   SQ1014.2
017200 01  OVERPRINT-LINE.                                              SQ1014.2
017300     02 FILLER            PIC X(68)  VALUE SPACE.                 SQ1014.2
017400     02 FILLER            PIC X(17)  VALUE "BBBBBBB WRT-TEST-".   SQ1014.2
017500     02 OVERPRINT-TEST    PIC XX.                                 SQ1014.2
017600     02 FILLER            PIC X(33)  VALUE                        SQ1014.2
017700            "/ THIS LINE SHOULD OVERPRINT".                       SQ1014.2
017800 01  LAST-LINE.                                                   SQ1014.2
017900     02 FILLER            PIC X(20)  VALUE SPACE.                 SQ1014.2
018000     02 FILLER            PIC X(100) VALUE "THIS LINE SHOULD BE 1 SQ1014.2
018100-       "LINE BELOW THE WRT-TEST LINE AND ALSO BE THE LAST LINE ONSQ1014.2
018200-       " THIS PAGE".                                             SQ1014.2
018300*                                                                 SQ1014.2
018400 01  NEW-PAGE-LINE.                                               SQ1014.2
018500     02 FILLER            PIC X(20)  VALUE SPACE.                 SQ1014.2
018600     02 FILLER            PIC X(100) VALUE "THIS LINE SHOULD APPEASQ1014.2
018700-       "R AT THE TOP OF A NEW PAGE".                             SQ1014.2
018800 01  NEXT-LINE.                                                   SQ1014.2
018900     02 FILLER            PIC X(20)  VALUE SPACE.                 SQ1014.2
019000     02 FILLER            PIC X(100) VALUE "A WRT-TEST LINE SHOULDSQ1014.2
019100-       " FOLLOW IMMEDIATELY ON THE NEXT LINE".                   SQ1014.2
019200*                                                                 SQ1014.2
019300 01  NOTE-1.                                                      SQ1014.2
019400     02 FILLER          PIC X(40)    VALUE "BECAUSE OF THE NATURE SQ1014.2
019500-    "OF THESE TESTS A ".                                         SQ1014.2
019600     02 FILLER          PIC X        VALUE QUOTE.                 SQ1014.2
019700     02 FILLER          PIC X(4)     VALUE "PASS".                SQ1014.2
019800     02 FILLER          PIC X        VALUE QUOTE.                 SQ1014.2
019900     02 FILLER          PIC X(4)     VALUE " OR ".                SQ1014.2
020000     02 FILLER          PIC X        VALUE QUOTE.                 SQ1014.2
020100     02 FILLER          PIC X(4)     VALUE "FAIL".                SQ1014.2
020200     02 FILLER          PIC X        VALUE QUOTE.                 SQ1014.2
020300     02 FILLER          PIC X(64)    VALUE " CANNOT BE DETERMINED SQ1014.2
020400-    "WITHIN THE PROGRAM. THE USER MUST VISUALLY".                SQ1014.2
020500 01  NOTE-2             PIC X(120)   VALUE "CHECK THE POSITION OF SQ1014.2
020600-    "EACH LINE TO DETERMINE THE ACCURACY OF THE VARIOUS WRITE OPTSQ1014.2
020700-    "IONS.  NOTE THAT SOME OVERPRINTING".                        SQ1014.2
020800 01  NOTE-3             PIC X(120)   VALUE "SHOULD OCCUR, AND IN ESQ1014.2
020900-    "VERY CASE THE OVERPRINTED LINE WILL READ---".               SQ1014.2
021000 01  NOTE-4.                                                      SQ1014.2
021100     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ1014.2
021200     02 FILLER          PIC X(100)   VALUE "WRT-TEST-XX/ THIS LINESQ1014.2
021300-    " SHOULD BE OVERPRINTED. AAAAAAA".                           SQ1014.2
021400 01  NOTE-5             PIC X(120)   VALUE "---AND THE LINE WHICH SQ1014.2
021500-    "OVERPRINTS SHOULD READ---".                                 SQ1014.2
021600 01  NOTE-6.                                                      SQ1014.2
021700     02 FILLER          PIC X(68)    VALUE SPACE.                 SQ1014.2
021800     02 FILLER          PIC X(52)    VALUE "BBBBBBB WRT-TEST-XX/ TSQ1014.2
021900-    "HIS LINE SHOULD OVERPRINT".                                 SQ1014.2
022000 01  NOTE-7             PIC X(120)   VALUE "ONLY FIVE OF THE LETTESQ1014.2
022100-    "RS A AND B SHOULD BE JUMBLED TOGETHER; THE REST SHOULD BE RESQ1014.2
022200-    "ADABLE.  IF ANY OTHER LINE IS".                             SQ1014.2
022300*                                                                 SQ1014.2
022400 01  NOTE-8             PIC X(120)   VALUE                        SQ1014.2
022500         "INVOLVED IN OVERPRINTING, AN ERROR HAS OCCURRED".       SQ1014.2
022600*                                                                 SQ1014.2
022700***************************************************************   SQ1014.2
022800*                                                             *   SQ1014.2
022900*    WORKING-STORAGE DATA ITEMS USED BY THE CCVS              *   SQ1014.2
023000*                                                             *   SQ1014.2
023100***************************************************************   SQ1014.2
023200*                                                                 SQ1014.2
023300 01  REC-SKEL-SUB   PIC 99.                                       SQ1014.2
023400*                                                                 SQ1014.2
023500 01  FILE-RECORD-INFORMATION-REC.                                 SQ1014.2
023600     03 FILE-RECORD-INFO-SKELETON.                                SQ1014.2
023700        05 FILLER                 PICTURE X(48)       VALUE       SQ1014.2
023800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1014.2
023900        05 FILLER                 PICTURE X(46)       VALUE       SQ1014.2
024000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1014.2
024100        05 FILLER                 PICTURE X(26)       VALUE       SQ1014.2
024200             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1014.2
024300        05 FILLER                 PICTURE X(37)       VALUE       SQ1014.2
024400             ",RECKEY=                             ".             SQ1014.2
024500        05 FILLER                 PICTURE X(38)       VALUE       SQ1014.2
024600             ",ALTKEY1=                             ".            SQ1014.2
024700        05 FILLER                 PICTURE X(38)       VALUE       SQ1014.2
024800             ",ALTKEY2=                             ".            SQ1014.2
024900        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1014.2
025000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1014.2
025100        05 FILE-RECORD-INFO-P1-120.                               SQ1014.2
025200           07 FILLER              PIC X(5).                       SQ1014.2
025300           07 XFILE-NAME          PIC X(6).                       SQ1014.2
025400           07 FILLER              PIC X(8).                       SQ1014.2
025500           07 XRECORD-NAME        PIC X(6).                       SQ1014.2
025600           07 FILLER              PIC X(1).                       SQ1014.2
025700           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1014.2
025800           07 FILLER              PIC X(7).                       SQ1014.2
025900           07 XRECORD-NUMBER      PIC 9(6).                       SQ1014.2
026000           07 FILLER              PIC X(6).                       SQ1014.2
026100           07 UPDATE-NUMBER       PIC 9(2).                       SQ1014.2
026200           07 FILLER              PIC X(5).                       SQ1014.2
026300           07 ODO-NUMBER          PIC 9(4).                       SQ1014.2
026400           07 FILLER              PIC X(5).                       SQ1014.2
026500           07 XPROGRAM-NAME       PIC X(5).                       SQ1014.2
026600           07 FILLER              PIC X(7).                       SQ1014.2
026700           07 XRECORD-LENGTH      PIC 9(6).                       SQ1014.2
026800           07 FILLER              PIC X(7).                       SQ1014.2
026900           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1014.2
027000           07 FILLER              PIC X(1).                       SQ1014.2
027100           07 XBLOCK-SIZE         PIC 9(4).                       SQ1014.2
027200           07 FILLER              PIC X(6).                       SQ1014.2
027300           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1014.2
027400           07 FILLER              PIC X(5).                       SQ1014.2
027500           07 XFILE-ORGANIZATION  PIC X(2).                       SQ1014.2
027600           07 FILLER              PIC X(6).                       SQ1014.2
027700           07 XLABEL-TYPE         PIC X(1).                       SQ1014.2
027800        05 FILE-RECORD-INFO-P121-240.                             SQ1014.2
027900           07 FILLER              PIC X(8).                       SQ1014.2
028000           07 XRECORD-KEY         PIC X(29).                      SQ1014.2
028100           07 FILLER              PIC X(9).                       SQ1014.2
028200           07 ALTERNATE-KEY1      PIC X(29).                      SQ1014.2
028300           07 FILLER              PIC X(9).                       SQ1014.2
028400           07 ALTERNATE-KEY2      PIC X(29).                      SQ1014.2
028500           07 FILLER              PIC X(7).                       SQ1014.2
028600*                                                                 SQ1014.2
028700 01  TEST-RESULTS.                                                SQ1014.2
028800     02 FILLER              PIC X      VALUE SPACE.               SQ1014.2
028900     02 FEATURE             PIC X(24)  VALUE SPACE.               SQ1014.2
029000     02 FILLER              PIC X      VALUE SPACE.               SQ1014.2
029100     02 P-OR-F              PIC X(5)   VALUE SPACE.               SQ1014.2
029200     02 FILLER              PIC X      VALUE SPACE.               SQ1014.2
029300     02  PAR-NAME.                                                SQ1014.2
029400       03 FILLER              PIC X(14)  VALUE SPACE.             SQ1014.2
029500       03 PARDOT-X            PIC X      VALUE SPACE.             SQ1014.2
029600       03 DOTVALUE            PIC 99     VALUE ZERO.              SQ1014.2
029700     02 FILLER              PIC X(9)   VALUE SPACE.               SQ1014.2
029800     02 RE-MARK             PIC X(61).                            SQ1014.2
029900 01  TEST-COMPUTED.                                               SQ1014.2
030000   02 FILLER  PIC X(30)  VALUE SPACE.                             SQ1014.2
030100   02 FILLER  PIC X(17)  VALUE "      COMPUTED =".                SQ1014.2
030200   02 COMPUTED-X.                                                 SQ1014.2
030300     03 COMPUTED-A    PIC X(20)  VALUE SPACE.                     SQ1014.2
030400     03 COMPUTED-N    REDEFINES COMPUTED-A PIC -9(9).9(9).        SQ1014.2
030500     03 COMPUTED-0V18 REDEFINES COMPUTED-A PIC -.9(18).           SQ1014.2
030600     03 COMPUTED-4V14 REDEFINES COMPUTED-A PIC -9(4).9(14).       SQ1014.2
030700     03 COMPUTED-14V4 REDEFINES COMPUTED-A PIC -9(14).9(4).       SQ1014.2
030800     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1014.2
030900        04 COMPUTED-18V0                   PIC -9(18).            SQ1014.2
031000        04 FILLER                          PIC X.                 SQ1014.2
031100     03 FILLER PIC X(50) VALUE SPACE.                             SQ1014.2
031200 01  TEST-CORRECT.                                                SQ1014.2
031300     02 FILLER PIC X(30) VALUE SPACE.                             SQ1014.2
031400     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1014.2
031500     02 CORRECT-X.                                                SQ1014.2
031600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SQ1014.2
031700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SQ1014.2
031800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SQ1014.2
031900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SQ1014.2
032000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SQ1014.2
032100     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1014.2
032200         04 CORRECT-18V0                     PIC -9(18).          SQ1014.2
032300         04 FILLER                           PIC X.               SQ1014.2
032400     03 FILLER PIC X(2) VALUE SPACE.                              SQ1014.2
032500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SQ1014.2
032600 01  CCVS-C-1.                                                    SQ1014.2
032700     02 FILLER  PIC IS X(4)     VALUE SPACE.                      SQ1014.2
032800     02 FILLER  PIC IS X(98)    VALUE IS "FEATURE               PASQ1014.2
032900-    "SS  PARAGRAPH-NAME                                          SQ1014.2
033000-    "       REMARKS".                                            SQ1014.2
033100     02 FILLER           PIC X(17)    VALUE SPACE.                SQ1014.2
033200 01  CCVS-C-2.                                                    SQ1014.2
033300     02 FILLER           PIC XXXX     VALUE SPACE.                SQ1014.2
033400     02 FILLER           PIC X(6)     VALUE "TESTED".             SQ1014.2
033500     02 FILLER           PIC X(16)    VALUE SPACE.                SQ1014.2
033600     02 FILLER           PIC X(4)     VALUE "FAIL".               SQ1014.2
033700     02 FILLER           PIC X(90)    VALUE SPACE.                SQ1014.2
033800 01  REC-SKL-SUB       PIC 9(2)     VALUE ZERO.                   SQ1014.2
033900 01  REC-CT            PIC 99       VALUE ZERO.                   SQ1014.2
034000 01  DELETE-COUNTER    PIC 999      VALUE ZERO.                   SQ1014.2
034100 01  ERROR-COUNTER     PIC 999      VALUE ZERO.                   SQ1014.2
034200 01  INSPECT-COUNTER   PIC 999      VALUE ZERO.                   SQ1014.2
034300 01  PASS-COUNTER      PIC 999      VALUE ZERO.                   SQ1014.2
034400 01  TOTAL-ERROR       PIC 999      VALUE ZERO.                   SQ1014.2
034500 01  ERROR-HOLD        PIC 999      VALUE ZERO.                   SQ1014.2
034600 01  DUMMY-HOLD        PIC X(120)   VALUE SPACE.                  SQ1014.2
034700 01  RECORD-COUNT      PIC 9(5)     VALUE ZERO.                   SQ1014.2
034800 01  ANSI-REFERENCE    PIC X(48)    VALUE SPACES.                 SQ1014.2
034900 01  CCVS-H-1.                                                    SQ1014.2
035000     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1014.2
035100     02  FILLER          PIC X(42)    VALUE                       SQ1014.2
035200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SQ1014.2
035300     02  FILLER          PIC X(39)    VALUE SPACES.               SQ1014.2
035400 01  CCVS-H-2A.                                                   SQ1014.2
035500   02  FILLER            PIC X(40)  VALUE SPACE.                  SQ1014.2
035600   02  FILLER            PIC X(7)   VALUE "CCVS85 ".              SQ1014.2
035700   02  FILLER            PIC XXXX   VALUE                         SQ1014.2
035800     "4.2 ".                                                      SQ1014.2
035900   02  FILLER            PIC X(28)  VALUE                         SQ1014.2
036000            " COPY - NOT FOR DISTRIBUTION".                       SQ1014.2
036100   02  FILLER            PIC X(41)  VALUE SPACE.                  SQ1014.2
036200*                                                                 SQ1014.2
036300 01  CCVS-H-2B.                                                   SQ1014.2
036400   02  FILLER            PIC X(15)  VALUE "TEST RESULT OF ".      SQ1014.2
036500   02  TEST-ID           PIC X(9).                                SQ1014.2
036600   02  FILLER            PIC X(4)   VALUE " IN ".                 SQ1014.2
036700   02  FILLER            PIC X(12)  VALUE                         SQ1014.2
036800     " HIGH       ".                                              SQ1014.2
036900   02  FILLER            PIC X(22)  VALUE                         SQ1014.2
037000            " LEVEL VALIDATION FOR ".                             SQ1014.2
037100   02  FILLER            PIC X(58)  VALUE                         SQ1014.2
037200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1014.2
037300 01  CCVS-H-3.                                                    SQ1014.2
037400     02  FILLER          PIC X(34)  VALUE                         SQ1014.2
037500            " FOR OFFICIAL USE ONLY    ".                         SQ1014.2
037600     02  FILLER          PIC X(58)  VALUE                         SQ1014.2
037700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1014.2
037800     02  FILLER          PIC X(28)  VALUE                         SQ1014.2
037900            "  COPYRIGHT   1985,1986 ".                           SQ1014.2
038000 01  CCVS-E-1.                                                    SQ1014.2
038100     02 FILLER           PIC X(52)  VALUE SPACE.                  SQ1014.2
038200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SQ1014.2
038300     02 ID-AGAIN         PIC X(9).                                SQ1014.2
038400     02 FILLER           PIC X(45)  VALUE SPACES.                 SQ1014.2
038500 01  CCVS-E-2.                                                    SQ1014.2
038600     02  FILLER          PIC X(31)  VALUE SPACE.                  SQ1014.2
038700     02  FILLER          PIC X(21)  VALUE SPACE.                  SQ1014.2
038800     02  CCVS-E-2-2.                                              SQ1014.2
038900         03 ERROR-TOTAL    PIC XXX    VALUE SPACE.                SQ1014.2
039000         03 FILLER         PIC X      VALUE SPACE.                SQ1014.2
039100         03 ENDER-DESC     PIC X(44)  VALUE                       SQ1014.2
039200            "ERRORS ENCOUNTERED".                                 SQ1014.2
039300 01  CCVS-E-3.                                                    SQ1014.2
039400     02  FILLER          PIC X(22)  VALUE                         SQ1014.2
039500            " FOR OFFICIAL USE ONLY".                             SQ1014.2
039600     02  FILLER          PIC X(12)  VALUE SPACE.                  SQ1014.2
039700     02  FILLER          PIC X(58)  VALUE                         SQ1014.2
039800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1014.2
039900     02  FILLER          PIC X(8)   VALUE SPACE.                  SQ1014.2
040000     02  FILLER          PIC X(20)  VALUE                         SQ1014.2
040100             " COPYRIGHT 1985,1986".                              SQ1014.2
040200 01  CCVS-E-4.                                                    SQ1014.2
040300     02 CCVS-E-4-1       PIC XXX    VALUE SPACE.                  SQ1014.2
040400     02 FILLER           PIC X(4)   VALUE " OF ".                 SQ1014.2
040500     02 CCVS-E-4-2       PIC XXX    VALUE SPACE.                  SQ1014.2
040600     02 FILLER           PIC X(40)  VALUE                         SQ1014.2
040700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1014.2
040800 01  XXINFO.                                                      SQ1014.2
040900     02 FILLER           PIC X(19)  VALUE "*** INFORMATION ***".  SQ1014.2
041000     02 INFO-TEXT.                                                SQ1014.2
041100       04 FILLER             PIC X(8)   VALUE SPACE.              SQ1014.2
041200       04 XXCOMPUTED         PIC X(20).                           SQ1014.2
041300       04 FILLER             PIC X(5)   VALUE SPACE.              SQ1014.2
041400       04 XXCORRECT          PIC X(20).                           SQ1014.2
041500     02 INF-ANSI-REFERENCE PIC X(48).                             SQ1014.2
041600 01  HYPHEN-LINE.                                                 SQ1014.2
041700     02 FILLER  PIC IS X VALUE IS SPACE.                          SQ1014.2
041800     02 FILLER  PIC IS X(65)    VALUE IS "************************SQ1014.2
041900-    "*****************************************".                 SQ1014.2
042000     02 FILLER  PIC IS X(54)    VALUE IS "************************SQ1014.2
042100-    "******************************".                            SQ1014.2
042200 01  CCVS-PGM-ID  PIC X(9)   VALUE                                SQ1014.2
042300     "SQ101M".                                                    SQ1014.2
042400 PROCEDURE DIVISION.                                              SQ1014.2
042500 CCVS1 SECTION.                                                   SQ1014.2
042600 OPEN-FILES.                                                      SQ1014.2
042700*    OPEN    I-O RAW-DATA.                                        SQ1014.2
042800*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1014.2
042900*    READ    RAW-DATA INVALID KEY GO TO END-E-1.                  SQ1014.2
043000*    MOVE   "ABORTED "   TO C-ABORT.                              SQ1014.2
043100*    ADD     1           TO C-NO-OF-TESTS.                        SQ1014.2
043200*    ACCEPT  C-DATE      FROM DATE.                               SQ1014.2
043300*    ACCEPT  C-TIME      FROM TIME.                               SQ1014.2
043400*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1014.2
043500*END-E-1.                                                         SQ1014.2
043600*    CLOSE   RAW-DATA.                                            SQ1014.2
043700     OPEN    OUTPUT PRINT-FILE.                                   SQ1014.2
043800     MOVE    CCVS-PGM-ID TO TEST-ID.                              SQ1014.2
043900     MOVE    CCVS-PGM-ID TO ID-AGAIN.                             SQ1014.2
044000     MOVE    SPACE TO TEST-RESULTS.                               SQ1014.2
044100     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1014.2
044200     MOVE    ZERO TO REC-SKEL-SUB.                                SQ1014.2
044300     PERFORM CCVS-INIT-FILE 10 TIMES.                             SQ1014.2
044400     GO TO CCVS1-EXIT.                                            SQ1014.2
044500*                                                                 SQ1014.2
044600 CCVS-INIT-FILE.                                                  SQ1014.2
044700     ADD     1 TO REC-SKL-SUB.                                    SQ1014.2
044800     MOVE    FILE-RECORD-INFO-SKELETON TO                         SQ1014.2
044900                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1014.2
045000*                                                                 SQ1014.2
045100 CLOSE-FILES.                                                     SQ1014.2
045200     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1014.2
045300     CLOSE   PRINT-FILE.                                          SQ1014.2
045400*    OPEN    I-O RAW-DATA.                                        SQ1014.2
045500*    MOVE    CCVS-PGM-ID TO RAW-DATA-KEY.                         SQ1014.2
045600*    READ    RAW-DATA INVALID KEY GO TO END-E-2.                  SQ1014.2
045700*    MOVE   "OK.     " TO C-ABORT.                                SQ1014.2
045800*    MOVE    PASS-COUNTER  TO C-OK.                               SQ1014.2
045900*    MOVE    ERROR-HOLD    TO C-ALL.                              SQ1014.2
046000*    MOVE    ERROR-COUNTER TO C-FAIL.                             SQ1014.2
046100*    MOVE    DELETE-CNT    TO C-DELETED.                          SQ1014.2
046200*    MOVE    INSPECT-COUNTER TO C-INSPECT.                        SQ1014.2
046300*    REWRITE RAW-DATA-SATZ INVALID KEY CONTINUE.                  SQ1014.2
046400*END-E-2.                                                         SQ1014.2
046500*    CLOSE   RAW-DATA.                                            SQ1014.2
046600 TERMINATE-CCVS.                                                  SQ1014.2
046700*    EXIT    PROGRAM.                                             SQ1014.2
046800     STOP    RUN.                                                 SQ1014.2
046900*                                                                 SQ1014.2
047000 INSPT.                                                           SQ1014.2
047100     MOVE   "INSPT" TO P-OR-F.                                    SQ1014.2
047200     ADD     1 TO INSPECT-COUNTER.                                SQ1014.2
047300*                                                                 SQ1014.2
047400 PASS.                                                            SQ1014.2
047500     MOVE   "PASS " TO P-OR-F.                                    SQ1014.2
047600     ADD     1 TO PASS-COUNTER.                                   SQ1014.2
047700*                                                                 SQ1014.2
047800 FAIL.                                                            SQ1014.2
047900     MOVE   "FAIL*" TO P-OR-F.                                    SQ1014.2
048000     ADD     1 TO ERROR-COUNTER.                                  SQ1014.2
048100*                                                                 SQ1014.2
048200 DE-LETE.                                                         SQ1014.2
048300     MOVE   "****TEST DELETED****" TO RE-MARK.                    SQ1014.2
048400     MOVE   "*****" TO P-OR-F.                                    SQ1014.2
048500     ADD     1 TO DELETE-COUNTER.                                 SQ1014.2
048600     PERFORM PRINT-DETAIL.                                        SQ1014.2
048700*                                                                 SQ1014.2
048800 PRINT-DETAIL.                                                    SQ1014.2
048900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1014.2
049000         MOVE   "." TO PARDOT-X                                   SQ1014.2
049100         MOVE    REC-CT TO DOTVALUE.                              SQ1014.2
049200     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1014.2
049300     PERFORM WRITE-LINE.                                          SQ1014.2
049400     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1014.2
049500         PERFORM WRITE-LINE                                       SQ1014.2
049600         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1014.2
049700     ELSE                                                         SQ1014.2
049800         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1014.2
049900     MOVE    SPACE TO P-OR-F.                                     SQ1014.2
050000     MOVE    SPACE TO COMPUTED-X.                                 SQ1014.2
050100     MOVE    SPACE TO CORRECT-X.                                  SQ1014.2
050200     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             SQ1014.2
050300     MOVE    SPACE TO RE-MARK.                                    SQ1014.2
050400*                                                                 SQ1014.2
050500 HEAD-ROUTINE.                                                    SQ1014.2
050600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1014.2
050700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SQ1014.2
050800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1014.2
050900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SQ1014.2
051000 COLUMN-NAMES-ROUTINE.                                            SQ1014.2
051100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1014.2
051200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1014.2
051300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1014.2
051400 END-ROUTINE.                                                     SQ1014.2
051500     MOVE    HYPHEN-LINE TO DUMMY-RECORD.                         SQ1014.2
051600     PERFORM WRITE-LINE 5 TIMES.                                  SQ1014.2
051700 END-RTN-EXIT.                                                    SQ1014.2
051800     MOVE    CCVS-E-1 TO DUMMY-RECORD.                            SQ1014.2
051900     PERFORM WRITE-LINE 2 TIMES.                                  SQ1014.2
052000*                                                                 SQ1014.2
052100 END-ROUTINE-1.                                                   SQ1014.2
052200     ADD     ERROR-COUNTER   TO ERROR-HOLD                        SQ1014.2
052300     ADD     INSPECT-COUNTER TO ERROR-HOLD.                       SQ1014.2
052400     ADD     DELETE-COUNTER  TO ERROR-HOLD.                       SQ1014.2
052500     ADD     PASS-COUNTER    TO ERROR-HOLD.                       SQ1014.2
052600     MOVE    PASS-COUNTER    TO CCVS-E-4-1.                       SQ1014.2
052700     MOVE    ERROR-HOLD      TO CCVS-E-4-2.                       SQ1014.2
052800     MOVE    CCVS-E-4        TO CCVS-E-2-2.                       SQ1014.2
052900     MOVE    CCVS-E-2        TO DUMMY-RECORD                      SQ1014.2
053000     PERFORM WRITE-LINE.                                          SQ1014.2
053100     MOVE   "TEST(S) FAILED" TO ENDER-DESC.                       SQ1014.2
053200     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1014.2
053300         MOVE   "NO " TO ERROR-TOTAL                              SQ1014.2
053400     ELSE                                                         SQ1014.2
053500         MOVE    ERROR-COUNTER TO ERROR-TOTAL.                    SQ1014.2
053600     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1014.2
053700     PERFORM WRITE-LINE.                                          SQ1014.2
053800 END-ROUTINE-13.                                                  SQ1014.2
053900     IF DELETE-COUNTER IS EQUAL TO ZERO                           SQ1014.2
054000         MOVE   "NO " TO ERROR-TOTAL                              SQ1014.2
054100     ELSE                                                         SQ1014.2
054200         MOVE    DELETE-COUNTER TO ERROR-TOTAL.                   SQ1014.2
054300     MOVE   "TEST(S) DELETED     " TO ENDER-DESC.                 SQ1014.2
054400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            SQ1014.2
054500     PERFORM WRITE-LINE.                                          SQ1014.2
054600     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1014.2
054700         MOVE   "NO " TO ERROR-TOTAL                              SQ1014.2
054800     ELSE                                                         SQ1014.2
054900         MOVE    INSPECT-COUNTER TO ERROR-TOTAL.                  SQ1014.2
055000     MOVE   "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.           SQ1014.2
055100     MOVE    CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1014.2
055200     MOVE    CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1014.2
055300*                                                                 SQ1014.2
055400 WRITE-LINE.                                                      SQ1014.2
055500     ADD     1 TO RECORD-COUNT.                                   SQ1014.2
055600*    IF RECORD-COUNT GREATER 50                                   SQ1014.2
055700*        MOVE  DUMMY-RECORD TO DUMMY-HOLD                         SQ1014.2
055800*        MOVE  SPACE TO DUMMY-RECORD                              SQ1014.2
055900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1014.2
056000*        MOVE  CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN            SQ1014.2
056100*        MOVE  CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    SQ1014.2
056200*        MOVE  HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN         SQ1014.2
056300*        MOVE  DUMMY-HOLD TO DUMMY-RECORD                         SQ1014.2
056400*        MOVE  ZERO TO RECORD-COUNT.                              SQ1014.2
056500     PERFORM WRT-LN.                                              SQ1014.2
056600*                                                                 SQ1014.2
056700 WRT-LN.                                                          SQ1014.2
056800     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                SQ1014.2
056900     MOVE    SPACE TO DUMMY-RECORD.                               SQ1014.2
057000 BLANK-LINE-PRINT.                                                SQ1014.2
057100     PERFORM WRT-LN.                                              SQ1014.2
057200 FAIL-ROUTINE.                                                    SQ1014.2
057300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1014.2
057400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1014.2
057500     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1014.2
057600     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  SQ1014.2
057700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1014.2
057800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1014.2
057900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1014.2
058000     GO TO   FAIL-ROUTINE-EX.                                     SQ1014.2
058100 FAIL-ROUTINE-WRITE.                                              SQ1014.2
058200     MOVE    TEST-COMPUTED  TO PRINT-REC                          SQ1014.2
058300     PERFORM WRITE-LINE                                           SQ1014.2
058400     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                SQ1014.2
058500     MOVE    TEST-CORRECT   TO PRINT-REC                          SQ1014.2
058600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1014.2
058700     MOVE    SPACES         TO COR-ANSI-REFERENCE.                SQ1014.2
058800 FAIL-ROUTINE-EX.                                                 SQ1014.2
058900     EXIT.                                                        SQ1014.2
059000 BAIL-OUT.                                                        SQ1014.2
059100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1014.2
059200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1014.2
059300 BAIL-OUT-WRITE.                                                  SQ1014.2
059400     MOVE    CORRECT-A      TO XXCORRECT.                         SQ1014.2
059500     MOVE    COMPUTED-A     TO XXCOMPUTED.                        SQ1014.2
059600     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                SQ1014.2
059700     MOVE    XXINFO TO DUMMY-RECORD.                              SQ1014.2
059800     PERFORM WRITE-LINE 2 TIMES.                                  SQ1014.2
059900     MOVE    SPACES TO INF-ANSI-REFERENCE.                        SQ1014.2
060000 BAIL-OUT-EX.                                                     SQ1014.2
060100     EXIT.                                                        SQ1014.2
060200 CCVS1-EXIT.                                                      SQ1014.2
060300     EXIT.                                                        SQ1014.2
060400*                                                                 SQ1014.2
060500****************************************************************  SQ1014.2
060600*                                                              *  SQ1014.2
060700*    THIS POINT MARKS THE END OF THE CCVS MONITOR ROUTINES AND *  SQ1014.2
060800*    THE START OF THE TESTS OF SPECIFIC COBOL FEATURES.        *  SQ1014.2
060900*                                                              *  SQ1014.2
061000****************************************************************  SQ1014.2
061100*                                                                 SQ1014.2
061200 SECT-SQ101-0001 SECTION.                                         SQ1014.2
061300 WRT-PREAMBLE.                                                    SQ1014.2
061400     MOVE NOTE-1 TO PRINT-REC.                                    SQ1014.2
061500     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
061600     MOVE NOTE-2 TO PRINT-REC.                                    SQ1014.2
061700     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
061800     MOVE NOTE-3 TO PRINT-REC.                                    SQ1014.2
061900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
062000     MOVE NOTE-4 TO PRINT-REC.                                    SQ1014.2
062100     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
062200     MOVE NOTE-5 TO PRINT-REC.                                    SQ1014.2
062300     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
062400     MOVE NOTE-6 TO PRINT-REC.                                    SQ1014.2
062500     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
062600     MOVE NOTE-7 TO PRINT-REC.                                    SQ1014.2
062700     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
062800     MOVE NOTE-8 TO PRINT-REC.                                    SQ1014.2
062900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
063000     MOVE SPACE TO DUMMY-RECORD.                                  SQ1014.2
063100     PERFORM BLANK-LINE-PRINT.                                    SQ1014.2
063200*                                                                 SQ1014.2
063300****************************************************************  SQ1014.2
063400*                                                              *  SQ1014.2
063500*    THE STANDARD PAGE HEADING OF THE MONITOR OUTPUT AND THE   *  SQ1014.2
063600*    PREAMBLE DESCRIBING MONTORING REQUIREMENTS WILL HAVE USED *  SQ1014.2
063700*    24 LINES ON THE FIRST PAGE OF PRINTED OUTPUT.  THE TESTS  *  SQ1014.2
063800*    ARE ARRANGED WHEREVER POSSIBLE SO THAT THE BLANK LINES    *  SQ1014.2
063900*    WHICH MUST BE COUNTED DO NOT INCLUDE A BOUNDARY BETWEEN   *  SQ1014.2
064000*    TWO PAGES.                                                *  SQ1014.2
064100*                                                              *  SQ1014.2
064200*    IT IS ASSUMED THAT A PHYSICAL PAGE CAN SHOW AT LEAST 60   *  SQ1014.2
064300*    PRINTED LINES.  THERE ARE THUS AT LEAST 36 LINES LEFT ON  *  SQ1014.2
064400*    THIS FIRST PAGE.                                          *  SQ1014.2
064500*                                                              *  SQ1014.2
064600****************************************************************  SQ1014.2
064700*                                                                 SQ1014.2
064800 WRT-INIT-GF-01.                                                  SQ1014.2
064900*                                                                 SQ1014.2
065000*    THIS TEST ADVANCES THE PRINT POSITION 8 LINES                SQ1014.2
065100*                                                                 SQ1014.2
065200     MOVE   "WRT BEFORE ADV INT LINES" TO FEATURE.                SQ1014.2
065300     MOVE   "WRT-TEST-GF-01" TO PAR-NAME.                         SQ1014.2
065400     GO TO   WRT-TEST-GF-01.                                      SQ1014.2
065500 WRT-DELETE-GF-01.                                                SQ1014.2
065600     PERFORM DE-LETE.                                             SQ1014.2
065700     GO TO   WRT-END-GF-01.                                       SQ1014.2
065800 WRT-TEST-GF-01.                                                  SQ1014.2
065900     PERFORM INSPT.                                               SQ1014.2
066000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
066100     MOVE "1" TO LINES-BELOW-1.                                   SQ1014.2
066200     MOVE "8" TO LINES-ABOVE-1.                                   SQ1014.2
066300     MOVE TEST-LINE-1 TO PRINT-REC.                               SQ1014.2
066400     WRITE PRINT-REC BEFORE ADVANCING 7 LINES.                    SQ1014.2
066500 WRT-END-GF-01.                                                   SQ1014.2
066600*                                                                 SQ1014.2
066700 WRT-INIT-GF-02.                                                  SQ1014.2
066800*                                                                 SQ1014.2
066900*    THIS TEST ADVANCES THE PRINT POSITION 7 LINES                SQ1014.2
067000*                                                                 SQ1014.2
067100     MOVE   "WRT BEFORE ADV INT LINE" TO FEATURE.                 SQ1014.2
067200     MOVE   "WRT-TEST-GF-02" TO PAR-NAME.                         SQ1014.2
067300     GO TO   WRT-TEST-GF-02.                                      SQ1014.2
067400 WRT-DELETE-GF-02.                                                SQ1014.2
067500     PERFORM DE-LETE.                                             SQ1014.2
067600     GO TO   WRT-END-GF-02.                                       SQ1014.2
067700 WRT-TEST-GF-02.                                                  SQ1014.2
067800     PERFORM INSPT.                                               SQ1014.2
067900     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
068000     MOVE "1" TO LINES-BELOW-1.                                   SQ1014.2
068100     MOVE "7" TO LINES-ABOVE-1.                                   SQ1014.2
068200     MOVE TEST-LINE-1 TO DUMMY-RECORD.                            SQ1014.2
068300     WRITE DUMMY-RECORD BEFORE ADVANCING 6 LINE.                  SQ1014.2
068400 WRT-END-GF-02.                                                   SQ1014.2
068500*                                                                 SQ1014.2
068600 WRT-INIT-GF-03.                                                  SQ1014.2
068700*                                                                 SQ1014.2
068800*    THIS TEST ADVANCES THE PRINT POSITION 6 LINES                SQ1014.2
068900*                                                                 SQ1014.2
069000     MOVE   "WRT BEFORE INTEGER LINES" TO FEATURE.                SQ1014.2
069100     MOVE   "WRT-TEST-GF-03" TO PAR-NAME.                         SQ1014.2
069200     GO TO   WRT-TEST-GF-03.                                      SQ1014.2
069300 WRT-DELETE-GF-03.                                                SQ1014.2
069400     PERFORM DE-LETE.                                             SQ1014.2
069500     GO TO   WRT-END-GF-03.                                       SQ1014.2
069600 WRT-TEST-GF-03.                                                  SQ1014.2
069700     PERFORM INSPT.                                               SQ1014.2
069800     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
069900     MOVE   "1" TO LINES-BELOW-2.                                 SQ1014.2
070000     MOVE   "6" TO LINES-ABOVE-2.                                 SQ1014.2
070100     MOVE    TEST-LINE-2 TO PRINT-REC.                            SQ1014.2
070200     WRITE   PRINT-REC BEFORE 5 LINES.                            SQ1014.2
070300 WRT-END-GF-03.                                                   SQ1014.2
070400*                                                                 SQ1014.2
070500 WRT-INIT-GF-04.                                                  SQ1014.2
070600*                                                                 SQ1014.2
070700*    THIS TEST ADVANCES THE PRINT POSITION 5 LINES                SQ1014.2
070800*                                                                 SQ1014.2
070900     MOVE   "WRT BEFORE INTEGER LINE" TO FEATURE.                 SQ1014.2
071000     MOVE   "WRT-TEST-GF-04" TO PAR-NAME.                         SQ1014.2
071100     GO TO   WRT-TEST-GF-04.                                      SQ1014.2
071200 WRT-DELETE-GF-04.                                                SQ1014.2
071300     PERFORM DE-LETE.                                             SQ1014.2
071400     GO TO   WRT-END-GF-04.                                       SQ1014.2
071500 WRT-TEST-GF-04.                                                  SQ1014.2
071600     PERFORM INSPT.                                               SQ1014.2
071700     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
071800     MOVE "1" TO LINES-BELOW-2.                                   SQ1014.2
071900     MOVE "5" TO LINES-ABOVE-2.                                   SQ1014.2
072000     MOVE TEST-LINE-2 TO DUMMY-RECORD.                            SQ1014.2
072100     WRITE DUMMY-RECORD BEFORE 4 LINE.                            SQ1014.2
072200 WRT-END-GF-04.                                                   SQ1014.2
072300*                                                                 SQ1014.2
072400 WRT-INIT-GF-05.                                                  SQ1014.2
072500*                                                                 SQ1014.2
072600*    THIS TEST ADVANCES THE PRINT POSITION 5 LINES                SQ1014.2
072700*                                                                 SQ1014.2
072800     MOVE   "WRT AFTER ADV INT LINES" TO FEATURE.                 SQ1014.2
072900     MOVE   "WRT-TEST-GF-05" TO PAR-NAME.                         SQ1014.2
073000     GO TO   WRT-TEST-GF-05.                                      SQ1014.2
073100 WRT-DELETE-GF-05.                                                SQ1014.2
073200     PERFORM DE-LETE.                                             SQ1014.2
073300     GO TO   WRT-END-GF-05.                                       SQ1014.2
073400 WRT-TEST-GF-05.                                                  SQ1014.2
073500     PERFORM INSPT.                                               SQ1014.2
073600     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
073700     MOVE "4" TO LINES-BELOW-1.                                   SQ1014.2
073800     MOVE "1" TO LINES-ABOVE-1.                                   SQ1014.2
073900     MOVE TEST-LINE-1 TO PRINT-REC.                               SQ1014.2
074000     WRITE PRINT-REC AFTER ADVANCING 3 LINES.                     SQ1014.2
074100 WRT-END-GF-05.                                                   SQ1014.2
074200*                                                                 SQ1014.2
074300 WRT-INIT-GF-06.                                                  SQ1014.2
074400*                                                                 SQ1014.2
074500*    THE NEXT TEST IN NORMAL SEQUENCE WOULD COME VERY NEAR TO     SQ1014.2
074600*    OVERFLOWING THE CURRENT PAGE, SO THE FIRST OF THE TESTS OF   SQ1014.2
074700*    ADVANCING PAGE IS INSERTED HERE.  IT SHOULD LEAVE LINE 2     SQ1014.2
074800*    ON THE NEW PAGE AS THE CURRENT LINE, SO THAT THE FIRST       SQ1014.2
074900*    WRT-TEST LINE ON THE NEW PAGE IS THE THIRD PRINTABLE LINE.   SQ1014.2
075000*                                                                 SQ1014.2
075100     MOVE   "WRT BEFORE ADV PAGE" TO FEATURE.                     SQ1014.2
075200     MOVE   "WRT-TEST-GF-06" TO PAR-NAME.                         SQ1014.2
075300     GO TO   WRT-TEST-GF-06.                                      SQ1014.2
075400 WRT-DELETE-GF-06.                                                SQ1014.2
075500     PERFORM DE-LETE.                                             SQ1014.2
075600     GO TO   WRT-END-GF-06.                                       SQ1014.2
075700 WRT-TEST-GF-06.                                                  SQ1014.2
075800     PERFORM INSPT.                                               SQ1014.2
075900     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
076000     MOVE    LAST-LINE TO PRINT-REC.                              SQ1014.2
076100     WRITE   PRINT-REC BEFORE ADVANCING PAGE.                     SQ1014.2
076200     MOVE    NEW-PAGE-LINE TO PRINT-REC.                          SQ1014.2
076300     WRITE   PRINT-REC BEFORE ADVANCING 1 LINE.                   SQ1014.2
076400     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
076500     WRITE   PRINT-REC BEFORE ADVANCING 0 LINE.                   SQ1014.2
076600 WRT-END-GF-06.                                                   SQ1014.2
076700*                                                                 SQ1014.2
076800 WRT-INIT-GF-07.                                                  SQ1014.2
076900*                                                                 SQ1014.2
077000*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES TO LINE 6      SQ1014.2
077100*                                                                 SQ1014.2
077200     MOVE   "WRT AFTER ADV INT LINE" TO FEATURE.                  SQ1014.2
077300     MOVE   "WRT-TEST-GF-07" TO PAR-NAME.                         SQ1014.2
077400     GO TO   WRT-TEST-GF-07.                                      SQ1014.2
077500 WRT-DELETE-GF-07.                                                SQ1014.2
077600     PERFORM DE-LETE.                                             SQ1014.2
077700     GO TO   WRT-END-GF-07.                                       SQ1014.2
077800 WRT-TEST-GF-07.                                                  SQ1014.2
077900     PERFORM INSPT.                                               SQ1014.2
078000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
078100     MOVE   "3" TO LINES-BELOW-1.                                 SQ1014.2
078200     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
078300     MOVE    TEST-LINE-1 TO DUMMY-RECORD.                         SQ1014.2
078400     WRITE   DUMMY-RECORD AFTER ADVANCING 2 LINES.                SQ1014.2
078500 WRT-END-GF-07.                                                   SQ1014.2
078600*                                                                 SQ1014.2
078700 WRT-INIT-GF-08.                                                  SQ1014.2
078800*                                                                 SQ1014.2
078900*    THIS TEST IS IN TWO PARTS.  IT PRINTS ON THE LINE BELOW      SQ1014.2
079000*    THE MARKER LINE, THEN OVERPRINTS THAT LINE, ADVANCING ZERO   SQ1014.2
079100*    LINES AFTER PRINTING.                                        SQ1014.2
079200*    TOTAL PAPER ADVANCE IS TWO LINES, TO LINE 8.                 SQ1014.2
079300*                                                                 SQ1014.2
079400     MOVE   "WRT AFTER INT LINE/S" TO FEATURE.                    SQ1014.2
079500     MOVE   "WRT-TEST-GF-08" TO PAR-NAME.                         SQ1014.2
079600     GO TO   WRT-TEST-GF-08.                                      SQ1014.2
079700 WRT-DELETE-GF-08.                                                SQ1014.2
079800     PERFORM DE-LETE.                                             SQ1014.2
079900     GO TO   WRT-END-GF-08.                                       SQ1014.2
080000 WRT-TEST-GF-08.                                                  SQ1014.2
080100     PERFORM INSPT.                                               SQ1014.2
080200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
080300     MOVE   "08" TO OVERPRINTED-TEST.                             SQ1014.2
080400     MOVE    OVERPRINTED-LINE TO PRINT-REC.                       SQ1014.2
080500     WRITE   PRINT-REC AFTER 1 LINES.                             SQ1014.2
080600     MOVE   "08" TO OVERPRINT-TEST.                               SQ1014.2
080700     MOVE    OVERPRINT-LINE TO PRINT-REC.                         SQ1014.2
080800     WRITE   PRINT-REC AFTER 0 LINE.                              SQ1014.2
080900 WRT-END-GF-08.                                                   SQ1014.2
081000*                                                                 SQ1014.2
081100 WRT-INIT-GF-09.                                                  SQ1014.2
081200*                                                                 SQ1014.2
081300*    THIS TEST ADVANCES THE PRINT POSITION 12 LINES, TO LINE 20   SQ1014.2
081400*                                                                 SQ1014.2
081500     MOVE    1 TO REC-CT.                                         SQ1014.2
081600     MOVE   "WRT FRM BFR ADV INT LINS" TO FEATURE.                SQ1014.2
081700     MOVE   "WRT-TEST-GF-09" TO PAR-NAME.                         SQ1014.2
081800     GO TO   WRT-TEST-GF-09.                                      SQ1014.2
081900 WRT-DELETE-GF-09.                                                SQ1014.2
082000     PERFORM DE-LETE.                                             SQ1014.2
082100     GO TO   WRT-END-GF-09.                                       SQ1014.2
082200 WRT-TEST-GF-09.                                                  SQ1014.2
082300     PERFORM INSPT.                                               SQ1014.2
082400     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
082500     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
082600     MOVE   "11" TO LINES-ABOVE-1.                                SQ1014.2
082700     WRITE  PRINT-REC FROM TEST-LINE-1 BEFORE ADVANCING 10 LINES. SQ1014.2
082800 WRT-END-GF-09.                                                   SQ1014.2
082900*                                                                 SQ1014.2
083000 WRT-INIT-GF-10.                                                  SQ1014.2
083100*                                                                 SQ1014.2
083200*    THIS TEST ADVANCES THE PRINT POSITION 22 LINES, TO LINE 44   SQ1014.2
083300*                                                                 SQ1014.2
083400     MOVE   "WRT FRM BFR ADV INT LINE" TO FEATURE.                SQ1014.2
083500     MOVE   "WRT-TEST-GF-10" TO PAR-NAME.                         SQ1014.2
083600     GO TO   WRT-TEST-GF-10.                                      SQ1014.2
083700 WRT-DELETE-GF-10.                                                SQ1014.2
083800     PERFORM DE-LETE.                                             SQ1014.2
083900     GO TO   WRT-END-GF-10.                                       SQ1014.2
084000 WRT-TEST-GF-10.                                                  SQ1014.2
084100     PERFORM INSPT.                                               SQ1014.2
084200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
084300     MOVE   "1" TO LINES-BELOW-2.                                 SQ1014.2
084400     MOVE   "21" TO LINES-ABOVE-2.                                SQ1014.2
084500     WRITE DUMMY-RECORD FROM TEST-LINE-2 BEFORE ADVANCING 20 LINE.SQ1014.2
084600 WRT-END-GF-10.                                                   SQ1014.2
084700*                                                                 SQ1014.2
084800 WRT-INIT-GF-11.                                                  SQ1014.2
084900*                                                                 SQ1014.2
085000*    THIS TEST ADVANCES THE PRINT POSITION 13 LINES, TO LINE 57   SQ1014.2
085100*                                                                 SQ1014.2
085200     MOVE   "WRT FRM BEFORE INT LINES" TO FEATURE.                SQ1014.2
085300     MOVE   "WRT-TEST-GF-11" TO PAR-NAME.                         SQ1014.2
085400     GO TO   WRT-TEST-GF-11.                                      SQ1014.2
085500 WRT-DELETE-GF-11.                                                SQ1014.2
085600     PERFORM DE-LETE.                                             SQ1014.2
085700     GO TO   WRT-END-GF-11.                                       SQ1014.2
085800 WRT-TEST-GF-11.                                                  SQ1014.2
085900     PERFORM INSPT.                                               SQ1014.2
086000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
086100     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
086200     MOVE   "12" TO LINES-ABOVE-1.                                SQ1014.2
086300     WRITE   PRINT-REC FROM TEST-LINE-1 BEFORE 11 LINES.          SQ1014.2
086400 WRT-END-GF-11.                                                   SQ1014.2
086500*                                                                 SQ1014.2
086600 WRT-INIT-GF-12.                                                  SQ1014.2
086700*                                                                 SQ1014.2
086800*    THE NEXT TEST IN NORMAL SEQUENCE WOULD PROBABLY OVERFLOW     SQ1014.2
086900*    THE CURRENT PAGE, SO ANOTHER TEST OF ADVANCING PAGE IS       SQ1014.2
087000*    INSERTED HERE.  IT SHOULD LEAVE LINE 2 ON THE NEW PAGE AS    SQ1014.2
087100*    THE CURRENT LINE, SO THAT THE FIRST WRT-TEST LINE ON THE     SQ1014.2
087200*    NEW PAGE IS ON THE THIRD PRINTABLE LINE.                     SQ1014.2
087300*                                                                 SQ1014.2
087400     MOVE   "WRITE BEFORE PAGE" TO FEATURE.                       SQ1014.2
087500     MOVE   "WRT-TEST-GF-12" TO PAR-NAME.                         SQ1014.2
087600     GO TO   WRT-TEST-GF-12.                                      SQ1014.2
087700 WRT-DELETE-GF-12.                                                SQ1014.2
087800     PERFORM DE-LETE.                                             SQ1014.2
087900     GO TO   WRT-END-GF-12.                                       SQ1014.2
088000 WRT-TEST-GF-12.                                                  SQ1014.2
088100     PERFORM INSPT.                                               SQ1014.2
088200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
088300     MOVE    LAST-LINE TO PRINT-REC.                              SQ1014.2
088400     WRITE   PRINT-REC BEFORE PAGE.                               SQ1014.2
088500     MOVE    NEW-PAGE-LINE TO PRINT-REC.                          SQ1014.2
088600     WRITE   PRINT-REC BEFORE ADVANCING 1 LINE.                   SQ1014.2
088700     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
088800     WRITE   PRINT-REC BEFORE ADVANCING 0 LINE.                   SQ1014.2
088900 WRT-END-GF-12.                                                   SQ1014.2
089000*                                                                 SQ1014.2
089100 WRT-INIT-GF-13.                                                  SQ1014.2
089200*                                                                 SQ1014.2
089300*    THIS TEST ADVANCES THE PRINT POSITION 42 LINES, TO LINE 44   SQ1014.2
089400*                                                                 SQ1014.2
089500     MOVE   "WRT FROM BEFORE INT LINE" TO FEATURE.                SQ1014.2
089600     MOVE   "WRT-TEST-GF-13" TO PAR-NAME.                         SQ1014.2
089700     GO TO   WRT-TEST-GF-13.                                      SQ1014.2
089800 WRT-DELETE-GF-13.                                                SQ1014.2
089900     PERFORM DE-LETE.                                             SQ1014.2
090000     GO TO   WRT-END-GF-13.                                       SQ1014.2
090100 WRT-TEST-GF-13.                                                  SQ1014.2
090200     PERFORM INSPT.                                               SQ1014.2
090300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
090400     MOVE "1" TO LINES-BELOW-2.                                   SQ1014.2
090500     MOVE "41" TO LINES-ABOVE-2.                                  SQ1014.2
090600     WRITE DUMMY-RECORD FROM TEST-LINE-2 BEFORE 40 LINE.          SQ1014.2
090700 WRT-END-GF-13.                                                   SQ1014.2
090800*                                                                 SQ1014.2
090900 WRT-INIT-GF-14.                                                  SQ1014.2
091000*                                                                 SQ1014.2
091100*    THE NEXT TEST IN NORMAL SEQUENCE WOULD PROBABLY OVERFLOW     SQ1014.2
091200*    THE CURRENT PAGE, SO ANOTHER TEST OF ADVANCING PAGE IS       SQ1014.2
091300*    INSERTED HERE.  IT SHOULD LEAVE LINE 2 ON THE NEW PAGE AS    SQ1014.2
091400*    THE CURRENT LINE, SO THAT THE FIRST WRT-TEST LINE ON THE     SQ1014.2
091500*    NEW PAGE IS ON THE THIRD PRINTABLE LINE.                     SQ1014.2
091600*                                                                 SQ1014.2
091700     MOVE   "WRT AFTER ADV PAGE" TO FEATURE.                      SQ1014.2
091800     MOVE   "WRT-TEST-GF-14" TO PAR-NAME.                         SQ1014.2
091900     GO TO   WRT-TEST-GF-14.                                      SQ1014.2
092000 WRT-DELETE-GF-14.                                                SQ1014.2
092100     PERFORM DE-LETE.                                             SQ1014.2
092200     GO TO   WRT-END-GF-14.                                       SQ1014.2
092300 WRT-TEST-GF-14.                                                  SQ1014.2
092400     PERFORM INSPT.                                               SQ1014.2
092500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
092600     MOVE NEW-PAGE-LINE TO PRINT-REC.                             SQ1014.2
092700     WRITE PRINT-REC AFTER ADVANCING PAGE.                        SQ1014.2
092800     MOVE NEXT-LINE TO PRINT-REC.                                 SQ1014.2
092900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
093000 WRT-END-GF-14.                                                   SQ1014.2
093100*                                                                 SQ1014.2
093200 WRT-INIT-GF-15.                                                  SQ1014.2
093300*                                                                 SQ1014.2
093400*    THIS TEST ADVANCES THE PRINT POSITION 52 LINES, TO LINE 54   SQ1014.2
093500*                                                                 SQ1014.2
093600     MOVE   "WRT FRM AFT ADV INT LINS" TO FEATURE.                SQ1014.2
093700     MOVE   "WRT-TEST-GF-15" TO PAR-NAME.                         SQ1014.2
093800     GO TO   WRT-TEST-GF-15.                                      SQ1014.2
093900 WRT-DELETE-GF-15.                                                SQ1014.2
094000     PERFORM DE-LETE.                                             SQ1014.2
094100     GO TO   WRT-END-GF-15.                                       SQ1014.2
094200 WRT-TEST-GF-15.                                                  SQ1014.2
094300     PERFORM INSPT.                                               SQ1014.2
094400     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
094500     MOVE "51" TO LINES-BELOW-1.                                  SQ1014.2
094600     MOVE "1" TO LINES-ABOVE-1.                                   SQ1014.2
094700     WRITE PRINT-REC FROM TEST-LINE-1 AFTER ADVANCING 50 LINES.   SQ1014.2
094800 WRT-END-GF-15.                                                   SQ1014.2
094900*                                                                 SQ1014.2
095000 WRT-INIT-GF-16.                                                  SQ1014.2
095100*                                                                 SQ1014.2
095200*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 55    SQ1014.2
095300*                                                                 SQ1014.2
095400     MOVE   "WRT FRM AFT ADV INT LINE" TO FEATURE.                SQ1014.2
095500     MOVE   "WRT-TEST-GF-16" TO PAR-NAME.                         SQ1014.2
095600     GO TO   WRT-TEST-GF-16.                                      SQ1014.2
095700 WRT-DELETE-GF-16.                                                SQ1014.2
095800     PERFORM DE-LETE.                                             SQ1014.2
095900     GO TO   WRT-END-GF-16.                                       SQ1014.2
096000 WRT-TEST-GF-16.                                                  SQ1014.2
096100     PERFORM INSPT.                                               SQ1014.2
096200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
096300     MOVE   "2" TO LINES-BELOW-1.                                 SQ1014.2
096400     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
096500     WRITE   PRINT-REC FROM TEST-LINE-1 AFTER ADVANCING 1 LINE.   SQ1014.2
096600 WRT-END-GF-16.                                                   SQ1014.2
096700*                                                                 SQ1014.2
096800 WRT-INIT-GF-17.                                                  SQ1014.2
096900*                                                                 SQ1014.2
097000*    THE NEXT TEST IN NORMAL SEQUENCE WOULD PROBABLY OVERFLOW     SQ1014.2
097100*    THE CURRENT PAGE, SO ANOTHER TEST OF ADVANCING PAGE IS       SQ1014.2
097200*    INSERTED HERE.  IT SHOULD LEAVE LINE 2 ON THE NEW PAGE AS    SQ1014.2
097300*    THE CURRENT LINE, SO THAT THE FIRST WRT-TEST LINE ON THE     SQ1014.2
097400*    NEW PAGE IS ON THE THIRD PRINTABLE LINE.                     SQ1014.2
097500*                                                                 SQ1014.2
097600     MOVE   "WRITE AFTER PAGE" TO FEATURE.                        SQ1014.2
097700     MOVE   "WRT-TEST-GF-17" TO PAR-NAME.                         SQ1014.2
097800     GO TO   WRT-TEST-GF-17.                                      SQ1014.2
097900 WRT-DELETE-GF-17.                                                SQ1014.2
098000     PERFORM DE-LETE.                                             SQ1014.2
098100     GO TO   WRT-END-GF-17.                                       SQ1014.2
098200 WRT-TEST-GF-17.                                                  SQ1014.2
098300     PERFORM INSPT.                                               SQ1014.2
098400     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
098500     MOVE    NEW-PAGE-LINE TO PRINT-REC.                          SQ1014.2
098600     WRITE   PRINT-REC AFTER PAGE.                                SQ1014.2
098700     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
098800     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
098900 WRT-END-GF-17.                                                   SQ1014.2
099000*                                                                 SQ1014.2
099100 WRT-INIT-GF-18.                                                  SQ1014.2
099200*                                                                 SQ1014.2
099300*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 5     SQ1014.2
099400*                                                                 SQ1014.2
099500     MOVE   "WRT FRM AFTER INT LINE/S" TO FEATURE.                SQ1014.2
099600     MOVE   "WRT-TEST-GF-18" TO PAR-NAME.                         SQ1014.2
099700     GO TO   WRT-TEST-GF-18.                                      SQ1014.2
099800 WRT-DELETE-GF-18.                                                SQ1014.2
099900     PERFORM DE-LETE.                                             SQ1014.2
100000     GO TO   WRT-END-GF-18.                                       SQ1014.2
100100 WRT-TEST-GF-18.                                                  SQ1014.2
100200     PERFORM INSPT.                                               SQ1014.2
100300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
100400     MOVE "18" TO OVERPRINTED-TEST.                               SQ1014.2
100500     WRITE PRINT-REC FROM OVERPRINTED-LINE AFTER                  SQ1014.2
100600              000000000000000001 LINE.                            SQ1014.2
100700     MOVE "18" TO OVERPRINT-TEST.                                 SQ1014.2
100800     WRITE PRINT-REC FROM OVERPRINT-LINE AFTER                    SQ1014.2
100900              000000000000000000 LINES.                           SQ1014.2
101000 WRT-END-GF-18.                                                   SQ1014.2
101100*                                                                 SQ1014.2
101200 WRT-INIT-GF-19.                                                  SQ1014.2
101300*                                                                 SQ1014.2
101400*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 8     SQ1014.2
101500*                                                                 SQ1014.2
101600     MOVE   "WRITE" TO FEATURE.                                   SQ1014.2
101700     MOVE   "WRT-TEST-GF-19" TO PAR-NAME.                         SQ1014.2
101800     GO TO   WRT-TEST-GF-19.                                      SQ1014.2
101900 WRT-DELETE-GF-19.                                                SQ1014.2
102000     PERFORM DE-LETE.                                             SQ1014.2
102100     GO TO   WRT-END-GF-19.                                       SQ1014.2
102200 WRT-TEST-GF-19.                                                  SQ1014.2
102300     PERFORM INSPT.                                               SQ1014.2
102400     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
102500     MOVE   "2" TO LINES-BELOW-1.                                 SQ1014.2
102600     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
102700     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
102800     WRITE   PRINT-REC.                                           SQ1014.2
102900 WRT-END-GF-19.                                                   SQ1014.2
103000*                                                                 SQ1014.2
103100 WRT-INIT-GF-20.                                                  SQ1014.2
103200*                                                                 SQ1014.2
103300*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 11    SQ1014.2
103400*                                                                 SQ1014.2
103500     MOVE   "WRITE FROM" TO FEATURE.                              SQ1014.2
103600     MOVE   "WRT-TEST-GF-20" TO PAR-NAME.                         SQ1014.2
103700     GO TO   WRT-TEST-GF-20.                                      SQ1014.2
103800 WRT-DELETE-GF-20.                                                SQ1014.2
103900     PERFORM DE-LETE.                                             SQ1014.2
104000     GO TO   WRT-END-GF-20.                                       SQ1014.2
104100 WRT-TEST-GF-20.                                                  SQ1014.2
104200     PERFORM INSPT.                                               SQ1014.2
104300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
104400     MOVE   "2" TO LINES-BELOW-1.                                 SQ1014.2
104500     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
104600     WRITE   PRINT-REC FROM TEST-LINE-1.                          SQ1014.2
104700 WRT-END-GF-20.                                                   SQ1014.2
104800*                                                                 SQ1014.2
104900 WRT-INIT-GF-21.                                                  SQ1014.2
105000*                                                                 SQ1014.2
105100*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 14    SQ1014.2
105200*    IDENTIFIER-1 IS A SUBORDINATE GROUP ITEM                     SQ1014.2
105300*                                                                 SQ1014.2
105400     MOVE   "WRT SUBGR BFR ADV INT" TO FEATURE.                   SQ1014.2
105500     MOVE   "WRT-TEST-GF-21" TO PAR-NAME.                         SQ1014.2
105600     GO TO   WRT-TEST-GF-21.                                      SQ1014.2
105700 WRT-DELETE-GF-21.                                                SQ1014.2
105800     PERFORM DE-LETE.                                             SQ1014.2
105900     GO TO   WRT-END-GF-21.                                       SQ1014.2
106000 WRT-TEST-GF-21.                                                  SQ1014.2
106100     PERFORM INSPT.                                               SQ1014.2
106200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
106300     MOVE   "1" TO LINES-BELOW-2.                                 SQ1014.2
106400     MOVE   "2" TO LINES-ABOVE-2.                                 SQ1014.2
106500     WRITE   PRINT-REC FROM TEST-LINE-2 BEFORE ADVANCING 1.       SQ1014.2
106600 WRT-END-GF-21.                                                   SQ1014.2
106700*                                                                 SQ1014.2
106800 WRT-INIT-GF-22.                                                  SQ1014.2
106900*                                                                 SQ1014.2
107000*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 18    SQ1014.2
107100*    IDENTIFIER-1 IS A SUBORDINATE GROUP ITEM                     SQ1014.2
107200*                                                                 SQ1014.2
107300     MOVE   "WRT SUBGRP BEFORE INT" TO FEATURE.                   SQ1014.2
107400     MOVE   "WRT-TEST-GF-22" TO PAR-NAME.                         SQ1014.2
107500     GO TO   WRT-TEST-GF-22.                                      SQ1014.2
107600 WRT-DELETE-GF-22.                                                SQ1014.2
107700     PERFORM DE-LETE.                                             SQ1014.2
107800     GO TO   WRT-END-GF-22.                                       SQ1014.2
107900 WRT-TEST-GF-22.                                                  SQ1014.2
108000     PERFORM INSPT.                                               SQ1014.2
108100     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
108200     MOVE   "1" TO LINES-BELOW-2.                                 SQ1014.2
108300     MOVE   "3" TO LINES-ABOVE-2.                                 SQ1014.2
108400     WRITE   PRINT-REC FROM TEST-LINE-2 BEFORE 2.                 SQ1014.2
108500 WRT-END-GF-22.                                                   SQ1014.2
108600*                                                                 SQ1014.2
108700 WRT-INIT-GF-23.                                                  SQ1014.2
108800*                                                                 SQ1014.2
108900*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 21    SQ1014.2
109000*                                                                 SQ1014.2
109100     MOVE   "WRT FROM 77 AFT ADV INT" TO FEATURE.                 SQ1014.2
109200     MOVE   "WRT-TEST-23" TO PAR-NAME.                            SQ1014.2
109300     GO TO   WRT-TEST-GF-23.                                      SQ1014.2
109400 WRT-DELETE-GF-23.                                                SQ1014.2
109500     PERFORM DE-LETE.                                             SQ1014.2
109600     GO TO   WRT-END-GF-23.                                       SQ1014.2
109700 WRT-TEST-GF-23.                                                  SQ1014.2
109800     PERFORM INSPT.                                               SQ1014.2
109900     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
110000     WRITE PRINT-REC FROM SEVENTY-SEVEN AFTER ADVANCING 1.        SQ1014.2
110100 WRT-END-GF-23.                                                   SQ1014.2
110200*                                                                 SQ1014.2
110300 WRT-INIT-GF-24.                                                  SQ1014.2
110400*                                                                 SQ1014.2
110500*    THIS TEST ADVANCES THE PRINT POSITION 5 LINES, TO LINE 26    SQ1014.2
110600*                                                                 SQ1014.2
110700     MOVE   "WRT FROM AFTER INT" TO FEATURE.                      SQ1014.2
110800     MOVE   "WRT-TEST-GF-24" TO PAR-NAME.                         SQ1014.2
110900     GO TO   WRT-TEST-GF-24.                                      SQ1014.2
111000 WRT-DELETE-GF-24.                                                SQ1014.2
111100     PERFORM DE-LETE.                                             SQ1014.2
111200     GO TO   WRT-END-GF-24.                                       SQ1014.2
111300 WRT-TEST-GF-24.                                                  SQ1014.2
111400     PERFORM INSPT.                                               SQ1014.2
111500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
111600     MOVE   "4" TO LINES-BELOW-1.                                 SQ1014.2
111700     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
111800     WRITE   PRINT-REC FROM TEST-LINE-1 AFTER 3.                  SQ1014.2
111900 WRT-END-GF-24.                                                   SQ1014.2
112000*                                                                 SQ1014.2
112100 WRT-INIT-GF-25.                                                  SQ1014.2
112200*                                                                 SQ1014.2
112300*    THIS TEST ADVANCES THE PRINT POSITION 2 LINES, TO LINE 28    SQ1014.2
112400*                                                                 SQ1014.2
112500     MOVE   "WRT BEFORE ADV ZERO" TO FEATURE.                     SQ1014.2
112600     MOVE   "WRT-TEST-GF-25" TO PAR-NAME.                         SQ1014.2
112700     GO TO   WRT-TEST-GF-25.                                      SQ1014.2
112800 WRT-DELETE-GF-25.                                                SQ1014.2
112900     PERFORM DE-LETE.                                             SQ1014.2
113000     GO TO   WRT-END-GF-25.                                       SQ1014.2
113100 WRT-TEST-GF-25.                                                  SQ1014.2
113200     PERFORM INSPT.                                               SQ1014.2
113300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
113400     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
113500     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
113600     MOVE TEST-LINE-1 TO PRINT-REC.                               SQ1014.2
113700     WRITE PRINT-REC BEFORE ADVANCING ZERO.                       SQ1014.2
113800 WRT-END-GF-25.                                                   SQ1014.2
113900*                                                                 SQ1014.2
114000 WRT-INIT-GF-26.                                                  SQ1014.2
114100*                                                                 SQ1014.2
114200*    THIS TEST ADVANCES THE PRINT POSITION 7 LINES, TO LINE 35    SQ1014.2
114300*                                                                 SQ1014.2
114400     MOVE   "WRT BEFORE INT" TO FEATURE.                          SQ1014.2
114500     MOVE   "WRT-TEST-GF-26" TO PAR-NAME.                         SQ1014.2
114600     GO TO   WRT-TEST-GF-26.                                      SQ1014.2
114700 WRT-DELETE-GF-26.                                                SQ1014.2
114800     PERFORM DE-LETE.                                             SQ1014.2
114900     GO TO   WRT-END-GF-26.                                       SQ1014.2
115000 WRT-TEST-GF-26.                                                  SQ1014.2
115100     PERFORM INSPT.                                               SQ1014.2
115200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
115300     MOVE   "1" TO LINES-BELOW-2.                                 SQ1014.2
115400     MOVE   "6" TO LINES-ABOVE-2.                                 SQ1014.2
115500     MOVE    TEST-LINE-2 TO PRINT-REC.                            SQ1014.2
115600     WRITE   PRINT-REC BEFORE 5.                                  SQ1014.2
115700 WRT-END-GF-26.                                                   SQ1014.2
115800*                                                                 SQ1014.2
115900 WRT-INIT-GF-27.                                                  SQ1014.2
116000*                                                                 SQ1014.2
116100*    THIS TEST ADVANCES THE PRINT POSITION 7 LINES, TO LINE 42    SQ1014.2
116200*                                                                 SQ1014.2
116300     MOVE   "WRT AFTER ADVANCING INT" TO FEATURE.                 SQ1014.2
116400     MOVE   "WRT-TEST-GF-27" TO PAR-NAME.                         SQ1014.2
116500     GO TO   WRT-TEST-GF-27.                                      SQ1014.2
116600 WRT-DELETE-GF-27.                                                SQ1014.2
116700     PERFORM DE-LETE.                                             SQ1014.2
116800     GO TO   WRT-END-GF-27.                                       SQ1014.2
116900 WRT-TEST-GF-27.                                                  SQ1014.2
117000     PERFORM INSPT.                                               SQ1014.2
117100     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
117200     MOVE   "6" TO LINES-BELOW-2.                                 SQ1014.2
117300     MOVE   "1" TO LINES-ABOVE-2.                                 SQ1014.2
117400     MOVE    TEST-LINE-2 TO PRINT-REC.                            SQ1014.2
117500     WRITE   PRINT-REC AFTER ADVANCING 5.                         SQ1014.2
117600 WRT-END-GF-27.                                                   SQ1014.2
117700*                                                                 SQ1014.2
117800 WRT-INIT-GF-28.                                                  SQ1014.2
117900*                                                                 SQ1014.2
118000*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 45    SQ1014.2
118100*                                                                 SQ1014.2
118200     MOVE   "WRT AFTER INT" TO FEATURE.                           SQ1014.2
118300     MOVE   "WRT-TEST-GF-28" TO PAR-NAME.                         SQ1014.2
118400     GO TO   WRT-TEST-GF-28.                                      SQ1014.2
118500 WRT-DELETE-GF-28.                                                SQ1014.2
118600     PERFORM DE-LETE.                                             SQ1014.2
118700     GO TO   WRT-END-GF-28.                                       SQ1014.2
118800 WRT-TEST-GF-28.                                                  SQ1014.2
118900     PERFORM INSPT.                                               SQ1014.2
119000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
119100     MOVE   "2" TO LINES-BELOW-2.                                 SQ1014.2
119200     MOVE   "1" TO LINES-ABOVE-2.                                 SQ1014.2
119300     MOVE    TEST-LINE-2 TO PRINT-REC.                            SQ1014.2
119400     WRITE   PRINT-REC AFTER 1.                                   SQ1014.2
119500 WRT-END-GF-28.                                                   SQ1014.2
119600*                                                                 SQ1014.2
119700 WRT-INIT-GF-29.                                                  SQ1014.2
119800*                                                                 SQ1014.2
119900*    THIS TEST ADVANCES THE PRINT POSITION TO A NEW PAGE.  IT     SQ1014.2
120000*    SHOULD LEAVE LINE 2 ON THE NEW PAGE AS THE CURRENT LINE, SO  SQ1014.2
120100*    THAT THE FIRST WRT-TEST LINE ON THE NEW PAGE IS ON THE       SQ1014.2
120200*    THIRD PRINTABLE LINE.                                        SQ1014.2
120300*                                                                 SQ1014.2
120400     MOVE   "WRT FROM BEFORE ADV PAGE" TO FEATURE.                SQ1014.2
120500     MOVE   "WRT-TEST-GF-29" TO PAR-NAME.                         SQ1014.2
120600     GO TO   WRT-TEST-GF-29.                                      SQ1014.2
120700 WRT-DELETE-GF-29.                                                SQ1014.2
120800     PERFORM DE-LETE.                                             SQ1014.2
120900     GO TO   WRT-END-GF-29.                                       SQ1014.2
121000 WRT-TEST-GF-29.                                                  SQ1014.2
121100     PERFORM INSPT.                                               SQ1014.2
121200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
121300     WRITE   DUMMY-RECORD FROM LAST-LINE BEFORE ADVANCING PAGE.   SQ1014.2
121400     MOVE    NEW-PAGE-LINE TO PRINT-REC.                          SQ1014.2
121500     WRITE   PRINT-REC BEFORE ADVANCING 1 LINE.                   SQ1014.2
121600     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
121700     WRITE   PRINT-REC BEFORE ADVANCING 0 LINE.                   SQ1014.2
121800 WRT-END-GF-29.                                                   SQ1014.2
121900*                                                                 SQ1014.2
122000 WRT-INIT-GF-30.                                                  SQ1014.2
122100*                                                                 SQ1014.2
122200*    THIS TEST ADVANCES THE PRINT POSITION MANY LINES,            SQ1014.2
122300*    OVERFLOWING SEVERAL PAGES.  NO SPECIAL PROVISION IS MADE     SQ1014.2
122400*    FOR OVERFLOW HANDLING.                                       SQ1014.2
122500*                                                                 SQ1014.2
122600     MOVE   "CHARACTERS IN COLUMN 1" TO FEATURE.                  SQ1014.2
122700     MOVE   "WRT-TEST-GF-30" TO PAR-NAME.                         SQ1014.2
122800     GO TO   WRT-TEST-GF-30.                                      SQ1014.2
122900 WRT-DELETE-GF-30.                                                SQ1014.2
123000     PERFORM DE-LETE.                                             SQ1014.2
123100     GO TO   WRT-END-GF-30.                                       SQ1014.2
123200 WRT-TEST-GF-30.                                                  SQ1014.2
123300     PERFORM INSPT.                                               SQ1014.2
123400     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
123500     PERFORM BLANK-LINE-PRINT.                                    SQ1014.2
123600     MOVE " COLUMN 1 OF EACH OF THE LINES BELOW CONTAINS A NON-BLASQ1014.2
123700-    "NK CHARACTER.  IN THE PAST, CHARACTERS IN THIS POSITION MIGHSQ1014.2
123800-    "T"     TO PRINT-REC.                                        SQ1014.2
123900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1014.2
124000     MOVE " HAVE CONTROLLED PAPER MOVEMENT AND BEEN SUPPRESSED.  TSQ1014.2
124100-    "HIS PRACTICE DOES NOT CONFORM TO THE STANDARD."             SQ1014.2
124200             TO PRINT-REC.                                        SQ1014.2
124300     WRITE   PRINT-REC AFTER ADVANCING 1 LINES.                   SQ1014.2
124400     MOVE   " ALL LINES SHOULD BE PRINTED SINGLE-SPACED, AND EACH SQ1014.2
124500-    " CONTAINS A SEQUENCE NUMBER, STARTING WITH 001"             SQ1014.2
124600             TO PRINT-REC.                                        SQ1014.2
124700     WRITE   PRINT-REC AFTER ADVANCING 1 LINES.                   SQ1014.2
124800     MOVE " THE CHARACTERS PRINTED SHOULD BE AS FOLLOWS --- 0 1 2 SQ1014.2
124900-    "3 4 5 6 7 8 9 + - * / = $ , . ; ( ) < > A B C . . . Z AND"  SQ1014.2
125000             TO PRINT-REC.                                        SQ1014.2
125100     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
125200     MOVE   " a b c . . . z AND QUOTE"  TO PRINT-REC.             SQ1014.2
125300     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
125400     MOVE   " PRINTING BEGINS ON THE NEXT LINE BELOW ---"         SQ1014.2
125500             TO PRINT-REC.                                        SQ1014.2
125600     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
125700     MOVE    SPACES TO CHAR-LINE.                                 SQ1014.2
125800     MOVE    ZERO TO LIN-SER.                                     SQ1014.2
125900     MOVE "0" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126000     MOVE "1" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126100     MOVE "2" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126200     MOVE "3" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126300     MOVE "4" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126400     MOVE "5" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126500     MOVE "6" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126600     MOVE "7" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126700     MOVE "8" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126800     MOVE "9" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
126900     MOVE "+" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127000     MOVE "-" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127100     MOVE "*" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127200     MOVE "/" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127300     MOVE "=" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127400     MOVE "$" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127500     MOVE "," TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127600     MOVE "." TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127700     MOVE ";" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127800     MOVE "(" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
127900     MOVE ")" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128000     MOVE "<" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128100     MOVE ">" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128200     MOVE "A" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128300     MOVE "B" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128400     MOVE "C" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128500     MOVE "D" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128600     MOVE "E" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128700     MOVE "F" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128800     MOVE "G" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
128900     MOVE "H" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129000     MOVE "I" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129100     MOVE "J" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129200     MOVE "K" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129300     MOVE "L" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129400     MOVE "M" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129500     MOVE "N" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129600     MOVE "O" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129700     MOVE "P" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129800     MOVE "Q" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
129900     MOVE "R" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130000     MOVE "S" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130100     MOVE "T" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130200     MOVE "U" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130300     MOVE "V" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130400     MOVE "W" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130500     MOVE "X" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130600     MOVE "Y" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130700     MOVE "Z" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130800     MOVE "a" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
130900     MOVE "b" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131000     MOVE "c" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131100     MOVE "d" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131200     MOVE "e" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131300     MOVE "f" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131400     MOVE "g" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131500     MOVE "h" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131600     MOVE "i" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131700     MOVE "j" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131800     MOVE "k" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
131900     MOVE "l" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132000     MOVE "m" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132100     MOVE "n" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132200     MOVE "o" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132300     MOVE "p" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132400     MOVE "q" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132500     MOVE "r" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132600     MOVE "s" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132700     MOVE "t" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132800     MOVE "u" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
132900     MOVE "v" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
133000     MOVE "w" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
133100     MOVE "x" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
133200     MOVE "y" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
133300     MOVE "z" TO LIN-CH PERFORM COL-1-CHAR-PRINT.                 SQ1014.2
133400     MOVE QU-OTE TO LIN-CH PERFORM COL-1-CHAR-PRINT.              SQ1014.2
133500 WRT-END-GF-30.                                                   SQ1014.2
133600*                                                                 SQ1014.2
133700 WRT-INIT-GF-31.                                                  SQ1014.2
133800*                                                                 SQ1014.2
133900*    THIS TEST ADVANCES THE PRINT POSITION TO A NEW PAGE.  IT     SQ1014.2
134000*    SHOULD LEAVE LINE 2 ON THE NEW PAGE AS THE CURRENT LINE, SO  SQ1014.2
134100*    THAT THE FIRST WRT-TEST LINE ON THE NEW PAGE IS ON THE       SQ1014.2
134200*    THIRD PRINTABLE LINE.                                        SQ1014.2
134300*                                                                 SQ1014.2
134400     MOVE   "WRT FROM BEFORE PAGE" TO FEATURE.                    SQ1014.2
134500     MOVE   "WRT-TEST-GF-31" TO PAR-NAME.                         SQ1014.2
134600     GO TO   WRT-TEST-GF-31.                                      SQ1014.2
134700 WRT-DELETE-GF-31.                                                SQ1014.2
134800     PERFORM DE-LETE.                                             SQ1014.2
134900     GO TO   WRT-END-GF-31.                                       SQ1014.2
135000 WRT-TEST-GF-31.                                                  SQ1014.2
135100     PERFORM INSPT.                                               SQ1014.2
135200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
135300     WRITE   DUMMY-RECORD FROM LAST-LINE BEFORE PAGE.             SQ1014.2
135400     MOVE    NEW-PAGE-LINE TO PRINT-REC.                          SQ1014.2
135500     WRITE   PRINT-REC BEFORE ADVANCING 1 LINE.                   SQ1014.2
135600     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
135700     WRITE   PRINT-REC BEFORE ADVANCING 0 LINE.                   SQ1014.2
135800 WRT-END-GF-31.                                                   SQ1014.2
135900*                                                                 SQ1014.2
136000 WRT-INIT-GF-32.                                                  SQ1014.2
136100*                                                                 SQ1014.2
136200*    THIS TEST ADVANCES THE PRINT POSITION 2 LINES, TO LINE 5     SQ1014.2
136300*                                                                 SQ1014.2
136400     MOVE   "WRT FRM BFR ADV P99 LINE" TO FEATURE.                SQ1014.2
136500     MOVE   "WRT-TEST-GF-32" TO PAR-NAME.                         SQ1014.2
136600     GO TO   WRT-TEST-GF-32.                                      SQ1014.2
136700 WRT-DELETE-GF-32.                                                SQ1014.2
136800     PERFORM DE-LETE.                                             SQ1014.2
136900     GO TO   WRT-END-GF-32.                                       SQ1014.2
137000 WRT-TEST-GF-32.                                                  SQ1014.2
137100     PERFORM INSPT.                                               SQ1014.2
137200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
137300     MOVE    0 TO IDENTIFIER-2.                                   SQ1014.2
137400     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
137500     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
137600     WRITE PRINT-REC FROM TEST-LINE-1 BEFORE ADVANCING            SQ1014.2
137700              IDENTIFIER-2 LINE.                                  SQ1014.2
137800 WRT-END-GF-32.                                                   SQ1014.2
137900*                                                                 SQ1014.2
138000 WRT-INIT-GF-33.                                                  SQ1014.2
138100*                                                                 SQ1014.2
138200*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 8     SQ1014.2
138300*                                                                 SQ1014.2
138400     MOVE   "WRT FRM BFR ADV P99 LINS" TO FEATURE.                SQ1014.2
138500     MOVE   "WRT-TEST-GF-33" TO PAR-NAME.                         SQ1014.2
138600     GO TO   WRT-TEST-GF-33.                                      SQ1014.2
138700 WRT-DELETE-GF-33.                                                SQ1014.2
138800     PERFORM DE-LETE.                                             SQ1014.2
138900     GO TO   WRT-END-GF-33.                                       SQ1014.2
139000 WRT-TEST-GF-33.                                                  SQ1014.2
139100     PERFORM INSPT.                                               SQ1014.2
139200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
139300     MOVE    1 TO IDENTIFIER-2.                                   SQ1014.2
139400     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
139500     MOVE   "2" TO LINES-ABOVE-1.                                 SQ1014.2
139600     WRITE PRINT-REC FROM TEST-LINE-1 BEFORE ADVANCING            SQ1014.2
139700              IDENTIFIER-2 LINES.                                 SQ1014.2
139800 WRT-END-GF-33.                                                   SQ1014.2
139900*                                                                 SQ1014.2
140000 WRT-INIT-GF-34.                                                  SQ1014.2
140100*                                                                 SQ1014.2
140200*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 12    SQ1014.2
140300*                                                                 SQ1014.2
140400     MOVE "WRT FRM BEFORE ADV PIC99" TO FEATURE.                  SQ1014.2
140500     MOVE "WRT-TEST-GF-34" TO PAR-NAME.                           SQ1014.2
140600     GO TO   WRT-TEST-GF-34.                                      SQ1014.2
140700 WRT-DELETE-GF-34.                                                SQ1014.2
140800     PERFORM DE-LETE.                                             SQ1014.2
140900     GO TO   WRT-END-GF-34.                                       SQ1014.2
141000 WRT-TEST-GF-34.                                                  SQ1014.2
141100     PERFORM INSPT.                                               SQ1014.2
141200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
141300     MOVE    2 TO IDENTIFIER-2.                                   SQ1014.2
141400     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
141500     MOVE   "3" TO LINES-ABOVE-1.                                 SQ1014.2
141600     WRITE   PRINT-REC FROM TEST-LINE-1 BEFORE ADVANCING          SQ1014.2
141700              IDENTIFIER-2.                                       SQ1014.2
141800 WRT-END-GF-34.                                                   SQ1014.2
141900*                                                                 SQ1014.2
142000 WRT-INIT-GF-35.                                                  SQ1014.2
142100*                                                                 SQ1014.2
142200*    THIS TEST ADVANCES THE PRINT POSITION 5 LINES, TO LINE 17    SQ1014.2
142300*                                                                 SQ1014.2
142400     MOVE   "WRT FROM BEFORE P99 LINE" TO FEATURE.                SQ1014.2
142500     MOVE   "WRT-TEST-GF-35" TO PAR-NAME.                         SQ1014.2
142600     GO TO   WRT-TEST-GF-35.                                      SQ1014.2
142700 WRT-DELETE-GF-35.                                                SQ1014.2
142800     PERFORM DE-LETE.                                             SQ1014.2
142900     GO TO   WRT-END-GF-35.                                       SQ1014.2
143000 WRT-TEST-GF-35.                                                  SQ1014.2
143100     PERFORM INSPT.                                               SQ1014.2
143200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
143300     MOVE    3 TO IDENTIFIER-2.                                   SQ1014.2
143400     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
143500     MOVE   "4" TO LINES-ABOVE-1.                                 SQ1014.2
143600     WRITE PRINT-REC FROM TEST-LINE-1 BEFORE IDENTIFIER-2 LINE.   SQ1014.2
143700 WRT-END-GF-35.                                                   SQ1014.2
143800*                                                                 SQ1014.2
143900 WRT-INIT-GF-36.                                                  SQ1014.2
144000*                                                                 SQ1014.2
144100*    THIS TEST ADVANCES THE PRINT POSITION 6 LINES, TO LINE 23    SQ1014.2
144200*                                                                 SQ1014.2
144300     MOVE   "WRT FRM BEFORE P99 LINES" TO FEATURE.                SQ1014.2
144400     MOVE   "WRT-TEST-GF-36" TO PAR-NAME.                         SQ1014.2
144500     GO TO   WRT-TEST-GF-36.                                      SQ1014.2
144600 WRT-DELETE-GF-36.                                                SQ1014.2
144700     PERFORM DE-LETE.                                             SQ1014.2
144800     GO TO   WRT-END-GF-36.                                       SQ1014.2
144900 WRT-TEST-GF-36.                                                  SQ1014.2
145000     PERFORM INSPT.                                               SQ1014.2
145100     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
145200     MOVE    4 TO IDENTIFIER-2.                                   SQ1014.2
145300     WRITE   PRINT-REC FROM OH-ONE BEFORE IDENTIFIER-2 LINES.     SQ1014.2
145400 WRT-END-GF-36.                                                   SQ1014.2
145500*                                                                 SQ1014.2
145600 WRT-INIT-GF-37.                                                  SQ1014.2
145700*                                                                 SQ1014.2
145800*    THIS TEST ADVANCES THE PRINT POSITION 7 LINES, TO LINE 29    SQ1014.2
145900*                                                                 SQ1014.2
146000     MOVE   "WRT FROM 03 BEFORE PIC99" TO FEATURE.                SQ1014.2
146100     MOVE   "WRT-TEST-GF-37" TO PAR-NAME.                         SQ1014.2
146200     GO TO   WRT-TEST-GF-37.                                      SQ1014.2
146300 WRT-DELETE-GF-37.                                                SQ1014.2
146400     PERFORM DE-LETE.                                             SQ1014.2
146500     GO TO   WRT-END-GF-37.                                       SQ1014.2
146600 WRT-TEST-GF-37.                                                  SQ1014.2
146700     PERFORM INSPT.                                               SQ1014.2
146800     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
146900     MOVE    5 TO IDENTIFIER-2.                                   SQ1014.2
147000     WRITE   PRINT-REC FROM OH-THREE BEFORE IDENTIFIER-2.         SQ1014.2
147100 WRT-END-GF-37.                                                   SQ1014.2
147200*                                                                 SQ1014.2
147300 WRT-INIT-GF-38.                                                  SQ1014.2
147400*                                                                 SQ1014.2
147500*    THIS TEST ADVANCES THE PRINT POSITION 8 LINES, TO LINE 37    SQ1014.2
147600*                                                                 SQ1014.2
147700     MOVE   "WRT FRM AFT ADV P99 LINE" TO FEATURE.                SQ1014.2
147800     MOVE   "WRT-TEST-GF-38" TO PAR-NAME.                         SQ1014.2
147900     GO TO   WRT-TEST-GF-38.                                      SQ1014.2
148000 WRT-DELETE-GF-38.                                                SQ1014.2
148100     PERFORM DE-LETE.                                             SQ1014.2
148200     GO TO   WRT-END-GF-38.                                       SQ1014.2
148300 WRT-TEST-GF-38.                                                  SQ1014.2
148400     PERFORM INSPT.                                               SQ1014.2
148500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
148600     MOVE    6 TO IDENTIFIER-2.                                   SQ1014.2
148700     WRITE   PRINT-REC FROM SEVENTY-SEVEN-2 AFTER ADVANCING       SQ1014.2
148800              IDENTIFIER-2 LINE.                                  SQ1014.2
148900 WRT-END-GF-38.                                                   SQ1014.2
149000*                                                                 SQ1014.2
149100 WRT-INIT-GF-39.                                                  SQ1014.2
149200*                                                                 SQ1014.2
149300*    THIS TEST ADVANCES THE PRINT POSITION 9 LINES, TO LINE 46    SQ1014.2
149400*                                                                 SQ1014.2
149500     MOVE   "WRT FRM AFT ADV P99 LINS" TO FEATURE.                SQ1014.2
149600     MOVE   "WRT-TEST-GF-39" TO PAR-NAME.                         SQ1014.2
149700     GO TO   WRT-TEST-GF-39.                                      SQ1014.2
149800 WRT-DELETE-GF-39.                                                SQ1014.2
149900     PERFORM DE-LETE.                                             SQ1014.2
150000     GO TO   WRT-END-GF-39.                                       SQ1014.2
150100 WRT-TEST-GF-39.                                                  SQ1014.2
150200     PERFORM INSPT.                                               SQ1014.2
150300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
150400     MOVE    7 TO IDENTIFIER-2.                                   SQ1014.2
150500     MOVE   "8" TO LINES-BELOW-1.                                 SQ1014.2
150600     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
150700     WRITE   PRINT-REC FROM TEST-LINE-1 AFTER ADVANCING           SQ1014.2
150800              IDENTIFIER-2 LINES.                                 SQ1014.2
150900 WRT-END-GF-39.                                                   SQ1014.2
151000*                                                                 SQ1014.2
151100 WRT-INIT-GF-40.                                                  SQ1014.2
151200*                                                                 SQ1014.2
151300*    THIS TEST ADVANCES THE PRINT POSITION 10 LINES, TO LINE 56   SQ1014.2
151400*                                                                 SQ1014.2
151500     MOVE "WRT FRM AFT ADV ID2" TO FEATURE.                       SQ1014.2
151600     MOVE "WRT-TEST-44" TO PAR-NAME.                              SQ1014.2
151700     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
151800     MOVE 8 TO IDENTIFIER-2.                                      SQ1014.2
151900     MOVE "9" TO LINES-BELOW-1.                                   SQ1014.2
152000     MOVE "1" TO LINES-ABOVE-1.                                   SQ1014.2
152100     WRITE PRINT-REC FROM TEST-LINE-1 AFTER ADVANCING             SQ1014.2
152200              IDENTIFIER-2.                                       SQ1014.2
152300 WRT-END-GF-40.                                                   SQ1014.2
152400*                                                                 SQ1014.2
152500 WRT-INIT-GF-41.                                                  SQ1014.2
152600*                                                                 SQ1014.2
152700*    THIS TEST ADVANCES THE PRINT POSITION TO A NEW PAGE.  IT     SQ1014.2
152800*    SHOULD LEAVE LINE 2 ON THE NEW PAGE AS THE CURRENT LINE, SO  SQ1014.2
152900*    THAT THE FIRST WRT-TEST LINE ON THE NEW PAGE IS ON THE       SQ1014.2
153000*    THIRD PRINTABLE LINE.                                        SQ1014.2
153100*                                                                 SQ1014.2
153200     MOVE   "WRT FROM AFTER ADV PAGE" TO FEATURE.                 SQ1014.2
153300     MOVE   "WRT-TEST-GF-41" TO PAR-NAME.                         SQ1014.2
153400     GO TO   WRT-TEST-GF-41.                                      SQ1014.2
153500 WRT-DELETE-GF-41.                                                SQ1014.2
153600     PERFORM DE-LETE.                                             SQ1014.2
153700     GO TO   WRT-END-GF-41.                                       SQ1014.2
153800 WRT-TEST-GF-41.                                                  SQ1014.2
153900     PERFORM INSPT.                                               SQ1014.2
154000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
154100     WRITE   PRINT-REC FROM NEW-PAGE-LINE AFTER ADVANCING PAGE.   SQ1014.2
154200     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
154300     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
154400 WRT-END-GF-41.                                                   SQ1014.2
154500*                                                                 SQ1014.2
154600 WRT-INIT-GF-42.                                                  SQ1014.2
154700*                                                                 SQ1014.2
154800*    THIS TEST ADVANCES THE PRINT POSITION 2 LINES, TO LINE 5     SQ1014.2
154900*                                                                 SQ1014.2
155000     MOVE   "WRT FROM AFTER ID2 LINE" TO FEATURE.                 SQ1014.2
155100     MOVE   "WRT-TEST-GF-42" TO PAR-NAME.                         SQ1014.2
155200     GO TO   WRT-TEST-GF-42.                                      SQ1014.2
155300 WRT-DELETE-GF-42.                                                SQ1014.2
155400     PERFORM DE-LETE.                                             SQ1014.2
155500     GO TO   WRT-END-GF-42.                                       SQ1014.2
155600 WRT-TEST-GF-42.                                                  SQ1014.2
155700     PERFORM INSPT.                                               SQ1014.2
155800     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
155900     MOVE    0 TO IDENTIFIER-2.                                   SQ1014.2
156000     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
156100     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
156200     WRITE PRINT-REC FROM TEST-LINE-1 AFTER IDENTIFIER-2 LINE.    SQ1014.2
156300 WRT-END-GF-42.                                                   SQ1014.2
156400*                                                                 SQ1014.2
156500 WRT-INIT-GF-43.                                                  SQ1014.2
156600*                                                                 SQ1014.2
156700*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 9     SQ1014.2
156800*                                                                 SQ1014.2
156900     MOVE   "WRT FROM AFTER S99 LINES" TO FEATURE.                SQ1014.2
157000     MOVE   "WRT-TEST-GF-43" TO PAR-NAME.                         SQ1014.2
157100     GO TO   WRT-TEST-GF-43.                                      SQ1014.2
157200 WRT-DELETE-GF-43.                                                SQ1014.2
157300     PERFORM DE-LETE.                                             SQ1014.2
157400     GO TO   WRT-END-GF-43.                                       SQ1014.2
157500 WRT-TEST-GF-43.                                                  SQ1014.2
157600     PERFORM INSPT.                                               SQ1014.2
157700     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
157800     MOVE    2 TO IDENT-2-S99.                                    SQ1014.2
157900     MOVE   "3" TO LINES-BELOW-1.                                 SQ1014.2
158000     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
158100     WRITE   PRINT-REC FROM TEST-LINE-1 AFTER IDENT-2-S99 LINES.  SQ1014.2
158200 WRT-END-GF-43.                                                   SQ1014.2
158300*                                                                 SQ1014.2
158400 WRT-INIT-GF-44.                                                  SQ1014.2
158500*                                                                 SQ1014.2
158600*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 12    SQ1014.2
158700*                                                                 SQ1014.2
158800     MOVE   "WRT FROM AFTER C99" TO FEATURE.                      SQ1014.2
158900     MOVE   "WRT-TEST-GF-44" TO PAR-NAME.                         SQ1014.2
159000     GO TO   WRT-TEST-GF-44.                                      SQ1014.2
159100 WRT-DELETE-GF-44.                                                SQ1014.2
159200     PERFORM DE-LETE.                                             SQ1014.2
159300     GO TO   WRT-END-GF-44.                                       SQ1014.2
159400 WRT-TEST-GF-44.                                                  SQ1014.2
159500     PERFORM INSPT.                                               SQ1014.2
159600     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
159700     MOVE    1 TO IDENT-2-C99.                                    SQ1014.2
159800     MOVE   "2" TO LINES-BELOW-1.                                 SQ1014.2
159900     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
160000     WRITE   PRINT-REC FROM TEST-LINE-1 AFTER IDENT-2-C99.        SQ1014.2
160100 WRT-END-GF-44.                                                   SQ1014.2
160200*                                                                 SQ1014.2
160300 WRT-INIT-GF-45.                                                  SQ1014.2
160400*                                                                 SQ1014.2
160500*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 16    SQ1014.2
160600*                                                                 SQ1014.2
160700     MOVE   "WRT BEFORE ADV S99 LINE" TO FEATURE.                 SQ1014.2
160800     MOVE   "WRT-TEST-GF-45" TO PAR-NAME.                         SQ1014.2
160900     GO TO   WRT-TEST-GF-45.                                      SQ1014.2
161000 WRT-DELETE-GF-45.                                                SQ1014.2
161100     PERFORM DE-LETE.                                             SQ1014.2
161200     GO TO   WRT-END-GF-45.                                       SQ1014.2
161300 WRT-TEST-GF-45.                                                  SQ1014.2
161400     PERFORM INSPT.                                               SQ1014.2
161500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
161600     MOVE    2 TO IDENT-2-S99.                                    SQ1014.2
161700     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
161800     MOVE   "3" TO LINES-ABOVE-1.                                 SQ1014.2
161900     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
162000     WRITE PRINT-REC BEFORE ADVANCING IDENT-2-S99 LINE.           SQ1014.2
162100 WRT-END-GF-45.                                                   SQ1014.2
162200*                                                                 SQ1014.2
162300 WRT-INIT-GF-46.                                                  SQ1014.2
162400*                                                                 SQ1014.2
162500*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 20    SQ1014.2
162600*                                                                 SQ1014.2
162700     MOVE   "WRT BEFORE ADV S99 LINES" TO FEATURE.                SQ1014.2
162800     MOVE   "WRT-TEST-GF-46" TO PAR-NAME.                         SQ1014.2
162900     GO TO   WRT-TEST-GF-46.                                      SQ1014.2
163000 WRT-DELETE-GF-46.                                                SQ1014.2
163100     PERFORM DE-LETE.                                             SQ1014.2
163200     GO TO   WRT-END-GF-46.                                       SQ1014.2
163300 WRT-TEST-GF-46.                                                  SQ1014.2
163400     PERFORM INSPT.                                               SQ1014.2
163500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
163600     MOVE    2 TO IDENT-2-S99.                                    SQ1014.2
163700     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
163800     MOVE   "3" TO LINES-ABOVE-1.                                 SQ1014.2
163900     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
164000     WRITE   PRINT-REC BEFORE ADVANCING IDENT-2-S99 LINES.        SQ1014.2
164100 WRT-END-GF-46.                                                   SQ1014.2
164200*                                                                 SQ1014.2
164300 WRT-INIT-GF-47.                                                  SQ1014.2
164400*                                                                 SQ1014.2
164500*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 24    SQ1014.2
164600*                                                                 SQ1014.2
164700     MOVE   "WRT BEFORE ADV S99" TO FEATURE.                      SQ1014.2
164800     MOVE   "WRT-TEST-GF-47" TO PAR-NAME.                         SQ1014.2
164900     GO TO   WRT-TEST-GF-47.                                      SQ1014.2
165000 WRT-DELETE-GF-47.                                                SQ1014.2
165100     PERFORM DE-LETE.                                             SQ1014.2
165200     GO TO   WRT-END-GF-47.                                       SQ1014.2
165300 WRT-TEST-GF-47.                                                  SQ1014.2
165400     PERFORM INSPT.                                               SQ1014.2
165500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
165600     MOVE    2 TO IDENT-2-S99.                                    SQ1014.2
165700     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
165800     MOVE   "3" TO LINES-ABOVE-1.                                 SQ1014.2
165900     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
166000     WRITE   PRINT-REC BEFORE ADVANCING IDENT-2-S99.              SQ1014.2
166100 WRT-END-GF-47.                                                   SQ1014.2
166200*                                                                 SQ1014.2
166300 WRT-INIT-GF-48.                                                  SQ1014.2
166400*                                                                 SQ1014.2
166500*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 27    SQ1014.2
166600*                                                                 SQ1014.2
166700     MOVE   "WRT BEFORE PIC9 LINE" TO FEATURE.                    SQ1014.2
166800     MOVE   "WRT-TEST-GF-48" TO PAR-NAME.                         SQ1014.2
166900     GO TO   WRT-TEST-GF-48.                                      SQ1014.2
167000 WRT-DELETE-GF-48.                                                SQ1014.2
167100     PERFORM DE-LETE.                                             SQ1014.2
167200     GO TO   WRT-END-GF-48.                                       SQ1014.2
167300 WRT-TEST-GF-48.                                                  SQ1014.2
167400     PERFORM INSPT.                                               SQ1014.2
167500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
167600     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
167700     MOVE   "2" TO LINES-ABOVE-1.                                 SQ1014.2
167800     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
167900     WRITE   PRINT-REC BEFORE ONE LINE.                           SQ1014.2
168000     PERFORM INSPT.                                               SQ1014.2
168100 WRT-END-GF-48.                                                   SQ1014.2
168200*                                                                 SQ1014.2
168300 WRT-INIT-GF-49.                                                  SQ1014.2
168400*                                                                 SQ1014.2
168500*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 31    SQ1014.2
168600*                                                                 SQ1014.2
168700     MOVE   "WRT BEFORE PIC9 LINES" TO FEATURE.                   SQ1014.2
168800     MOVE   "WRT-TEST-GF-49" TO PAR-NAME.                         SQ1014.2
168900     GO TO   WRT-TEST-GF-49.                                      SQ1014.2
169000 WRT-DELETE-GF-49.                                                SQ1014.2
169100     PERFORM DE-LETE.                                             SQ1014.2
169200     GO TO   WRT-END-GF-49.                                       SQ1014.2
169300 WRT-TEST-GF-49.                                                  SQ1014.2
169400     PERFORM INSPT.                                               SQ1014.2
169500     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
169600     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
169700     MOVE   "3" TO LINES-ABOVE-1.                                 SQ1014.2
169800     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
169900     WRITE   PRINT-REC BEFORE TWO LINES.                          SQ1014.2
170000 WRT-END-GF-49.                                                   SQ1014.2
170100*                                                                 SQ1014.2
170200 WRT-INIT-GF-50.                                                  SQ1014.2
170300*                                                                 SQ1014.2
170400*    THIS TEST ADVANCES THE PRINT POSITION 5 LINES, TO LINE 36    SQ1014.2
170500*                                                                 SQ1014.2
170600     MOVE   "WRT BEFORE PIC9" TO FEATURE.                         SQ1014.2
170700     MOVE   "WRT-TEST-GF-50" TO PAR-NAME.                         SQ1014.2
170800     GO TO   WRT-TEST-GF-50.                                      SQ1014.2
170900 WRT-DELETE-GF-50.                                                SQ1014.2
171000     PERFORM DE-LETE.                                             SQ1014.2
171100     GO TO   WRT-END-GF-50.                                       SQ1014.2
171200 WRT-TEST-GF-50.                                                  SQ1014.2
171300     PERFORM INSPT.                                               SQ1014.2
171400     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
171500     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
171600     MOVE   "4" TO LINES-ABOVE-1.                                 SQ1014.2
171700     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
171800     WRITE   PRINT-REC BEFORE THREE.                              SQ1014.2
171900 WRT-END-GF-50.                                                   SQ1014.2
172000*                                                                 SQ1014.2
172100 WRT-INIT-GF-51.                                                  SQ1014.2
172200*                                                                 SQ1014.2
172300*    THIS TEST ADVANCES THE PRINT POSITION 6 LINES, TO LINE 42    SQ1014.2
172400*                                                                 SQ1014.2
172500     MOVE   "WRT AFTER ADV PIC9 LINE" TO FEATURE.                 SQ1014.2
172600     MOVE   "WRT-TEST-GF-51" TO PAR-NAME.                         SQ1014.2
172700     GO TO   WRT-TEST-GF-51.                                      SQ1014.2
172800 WRT-DELETE-GF-51.                                                SQ1014.2
172900     PERFORM DE-LETE.                                             SQ1014.2
173000     GO TO   WRT-END-GF-51.                                       SQ1014.2
173100 WRT-TEST-GF-51.                                                  SQ1014.2
173200     PERFORM INSPT.                                               SQ1014.2
173300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
173400     MOVE   "5" TO LINES-BELOW-1.                                 SQ1014.2
173500     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
173600     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
173700     WRITE   PRINT-REC AFTER ADVANCING FOUR LINE.                 SQ1014.2
173800 WRT-END-GF-51.                                                   SQ1014.2
173900*                                                                 SQ1014.2
174000 WRT-INIT-GF-52.                                                  SQ1014.2
174100*                                                                 SQ1014.2
174200*    THIS TEST ADVANCES THE PRINT POSITION 2 LINES, TO LINE 44    SQ1014.2
174300*                                                                 SQ1014.2
174400     MOVE   "WRT AFT ADV 9(18) LINES" TO FEATURE.                 SQ1014.2
174500     MOVE   "WRT-TEST-GF-52" TO PAR-NAME.                         SQ1014.2
174600     GO TO   WRT-TEST-GF-52.                                      SQ1014.2
174700 WRT-DELETE-GF-52.                                                SQ1014.2
174800     PERFORM DE-LETE.                                             SQ1014.2
174900     GO TO   WRT-END-GF-52.                                       SQ1014.2
175000 WRT-TEST-GF-52.                                                  SQ1014.2
175100     PERFORM INSPT.                                               SQ1014.2
175200     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
175300     MOVE   "1" TO LINES-BELOW-1.                                 SQ1014.2
175400     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
175500     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
175600     WRITE   PRINT-REC AFTER ADVANCING LONG-ZERO LINES.           SQ1014.2
175700 WRT-END-GF-52.                                                   SQ1014.2
175800*                                                                 SQ1014.2
175900 WRT-INIT-GF-53.                                                  SQ1014.2
176000*                                                                 SQ1014.2
176100*    THIS TEST ADVANCES THE PRINT POSITION 3 LINES, TO LINE 47    SQ1014.2
176200*                                                                 SQ1014.2
176300     MOVE   "WRT AFTER ADV 9(18)" TO FEATURE.                     SQ1014.2
176400     MOVE   "WRT-TEST-GF-53" TO PAR-NAME.                         SQ1014.2
176500     GO TO   WRT-TEST-GF-53.                                      SQ1014.2
176600 WRT-DELETE-GF-53.                                                SQ1014.2
176700     PERFORM DE-LETE.                                             SQ1014.2
176800     GO TO   WRT-END-GF-53.                                       SQ1014.2
176900 WRT-TEST-GF-53.                                                  SQ1014.2
177000     PERFORM INSPT.                                               SQ1014.2
177100     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
177200     MOVE   "2" TO LINES-BELOW-1.                                 SQ1014.2
177300     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
177400     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
177500     WRITE   PRINT-REC AFTER ADVANCING LONG-ONE.                  SQ1014.2
177600 WRT-END-GF-53.                                                   SQ1014.2
177700*                                                                 SQ1014.2
177800 WRT-INIT-GF-54.                                                  SQ1014.2
177900*                                                                 SQ1014.2
178000*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 51    SQ1014.2
178100*                                                                 SQ1014.2
178200     MOVE   "WRT AFTER S99 LINE" TO FEATURE.                      SQ1014.2
178300     MOVE   "WRT-TEST-GF-54" TO PAR-NAME.                         SQ1014.2
178400     GO TO   WRT-TEST-GF-54.                                      SQ1014.2
178500 WRT-DELETE-GF-54.                                                SQ1014.2
178600     PERFORM DE-LETE.                                             SQ1014.2
178700     GO TO   WRT-END-GF-54.                                       SQ1014.2
178800 WRT-TEST-GF-54.                                                  SQ1014.2
178900     PERFORM INSPT.                                               SQ1014.2
179000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
179100     MOVE    2 TO IDENT-2-S99.                                    SQ1014.2
179200     MOVE   "3" TO LINES-BELOW-1.                                 SQ1014.2
179300     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
179400     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
179500     WRITE   PRINT-REC AFTER IDENT-2-S99 LINE.                    SQ1014.2
179600 WRT-END-GF-54.                                                   SQ1014.2
179700*                                                                 SQ1014.2
179800 WRT-INIT-GF-55.                                                  SQ1014.2
179900*                                                                 SQ1014.2
180000*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 55    SQ1014.2
180100*                                                                 SQ1014.2
180200     MOVE   "WRT AFTER PIC99 LINES" TO FEATURE.                   SQ1014.2
180300     MOVE   "WRT-TEST-GF-55" TO PAR-NAME.                         SQ1014.2
180400     GO TO   WRT-TEST-GF-55.                                      SQ1014.2
180500 WRT-DELETE-GF-55.                                                SQ1014.2
180600     PERFORM DE-LETE.                                             SQ1014.2
180700     GO TO   WRT-END-GF-55.                                       SQ1014.2
180800 WRT-TEST-GF-55.                                                  SQ1014.2
180900     PERFORM INSPT.                                               SQ1014.2
181000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
181100     MOVE    2 TO IDENTIFIER-2.                                   SQ1014.2
181200     MOVE   "3" TO LINES-BELOW-1.                                 SQ1014.2
181300     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
181400     MOVE    TEST-LINE-1 TO PRINT-REC.                            SQ1014.2
181500     WRITE   PRINT-REC AFTER IDENTIFIER-2 LINES.                  SQ1014.2
181600 WRT-END-GF-55.                                                   SQ1014.2
181700*                                                                 SQ1014.2
181800 WRT-INIT-GF-56.                                                  SQ1014.2
181900*                                                                 SQ1014.2
182000*    THIS TEST ADVANCES THE PRINT POSITION 4 LINES, TO LINE 59    SQ1014.2
182100*                                                                 SQ1014.2
182200     MOVE   "WRT AFTER PIC99" TO FEATURE.                         SQ1014.2
182300     MOVE   "WRT-TEST-GF-56" TO PAR-NAME.                         SQ1014.2
182400     GO TO   WRT-TEST-GF-56.                                      SQ1014.2
182500 WRT-DELETE-GF-56.                                                SQ1014.2
182600     PERFORM DE-LETE.                                             SQ1014.2
182700     GO TO   WRT-END-GF-56.                                       SQ1014.2
182800 WRT-TEST-GF-56.                                                  SQ1014.2
182900     PERFORM INSPT.                                               SQ1014.2
183000     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
183100     MOVE    2 TO IDENTIFIER-2.                                   SQ1014.2
183200     MOVE   "3" TO LINES-BELOW-1.                                 SQ1014.2
183300     MOVE   "1" TO LINES-ABOVE-1.                                 SQ1014.2
183400     MOVE TEST-LINE-1 TO PRINT-REC.                               SQ1014.2
183500     WRITE PRINT-REC AFTER IDENTIFIER-2.                          SQ1014.2
183600 WRT-END-GF-56.                                                   SQ1014.2
183700*                                                                 SQ1014.2
183800 WRT-INIT-GF-57.                                                  SQ1014.2
183900*                                                                 SQ1014.2
184000*    THIS TEST ADVANCES THE PRINT POSITION TO A NEW PAGE.  IT     SQ1014.2
184100*    SHOULD LEAVE LINE 2 ON THE NEW PAGE AS THE CURRENT LINE, SO  SQ1014.2
184200*    THAT THE FIRST WRT-TEST LINE ON THE NEW PAGE IS ON THE       SQ1014.2
184300*    THIRD PRINTABLE LINE.                                        SQ1014.2
184400*                                                                 SQ1014.2
184500     MOVE   "WRT FROM AFTER PAGE" TO FEATURE.                     SQ1014.2
184600     MOVE   "WRT-TEST-GF-57" TO PAR-NAME.                         SQ1014.2
184700     GO TO   WRT-TEST-GF-57.                                      SQ1014.2
184800 WRT-DELETE-GF-57.                                                SQ1014.2
184900     PERFORM DE-LETE.                                             SQ1014.2
185000     GO TO   WRT-END-GF-57.                                       SQ1014.2
185100 WRT-TEST-GF-57.                                                  SQ1014.2
185200     PERFORM INSPT.                                               SQ1014.2
185300     PERFORM WRITE-TEST-LINE.                                     SQ1014.2
185400     WRITE   PRINT-REC FROM NEW-PAGE-LINE AFTER PAGE.             SQ1014.2
185500     MOVE    NEXT-LINE TO PRINT-REC.                              SQ1014.2
185600     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
185700 WRT-END-GF-57.                                                   SQ1014.2
185800*                                                                 SQ1014.2
185900 AFTER-LAST-TEST.                                                 SQ1014.2
186000     MOVE    "FINAL WRT TEST LINE" TO FEATURE.                    SQ1014.2
186100     MOVE     "AFTER-LAST-TEST" TO PAR-NAME.                      SQ1014.2
186200     PERFORM  WRITE-TEST-LINE.                                    SQ1014.2
186300*                                                                 SQ1014.2
186400*                                                                 SQ1014.2
186500 SQ-END-ROUTINE.                                                  SQ1014.2
186600     GO TO CCVS-EXIT.                                             SQ1014.2
186700*                                                                 SQ1014.2
186800 WRITE-TEST-LINE.                                                 SQ1014.2
186900     MOVE    TEST-RESULTS TO PRINT-REC.                           SQ1014.2
187000     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
187100     MOVE    SPACE TO PRINT-REC.                                  SQ1014.2
187200     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
187300*                                                                 SQ1014.2
187400 COL-1-CHAR-PRINT.                                                SQ1014.2
187500     ADD     1 TO LIN-SER                                         SQ1014.2
187600     MOVE    CHAR-LINE TO PRINT-REC                               SQ1014.2
187700     WRITE   PRINT-REC AFTER ADVANCING 1 LINE.                    SQ1014.2
187800*                                                                 SQ1014.2
187900 CCVS-EXIT SECTION.                                               SQ1014.2
188000 CCVS-999999.                                                     SQ1014.2
188100     GO TO CLOSE-FILES.                                           SQ1014.2
      *END-OF,SQ101M                                                                  
