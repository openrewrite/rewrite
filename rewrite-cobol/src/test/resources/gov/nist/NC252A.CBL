      *HEADER,COBOL,NC252A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2524.2
000200 PROGRAM-ID.                                                      NC2524.2
000300     NC252A.                                                      NC2524.2
000400****************************************************************  NC2524.2
000500*                                                              *  NC2524.2
000600*    VALIDATION FOR:-                                          *  NC2524.2
000700*                                                              *  NC2524.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2524.2
000900*                                                              *  NC2524.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2524.2
001100*                                                              *  NC2524.2
001200****************************************************************  NC2524.2
001300*                                                              *  NC2524.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2524.2
001500*                                                              *  NC2524.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2524.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2524.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2524.2
001900*                                                              *  NC2524.2
002000****************************************************************  NC2524.2
002100*                                                              *  NC2524.2
002200*    THIS PROGRAM TESTS THE "REDEFINES" AND "RENAMES" CLAUSES. *  NC2524.2
002300*                                                              *  NC2524.2
002400****************************************************************  NC2524.2
002500 ENVIRONMENT DIVISION.                                            NC2524.2
002600 CONFIGURATION SECTION.                                           NC2524.2
002700 SOURCE-COMPUTER.                                                 NC2524.2
002800     XXXXX082.                                                    NC2524.2
002900 OBJECT-COMPUTER.                                                 NC2524.2
003000     XXXXX083.                                                    NC2524.2
003100 INPUT-OUTPUT SECTION.                                            NC2524.2
003200 FILE-CONTROL.                                                    NC2524.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC2524.2
003400     XXXXX055.                                                    NC2524.2
003500 DATA DIVISION.                                                   NC2524.2
003600 FILE SECTION.                                                    NC2524.2
003700 FD  PRINT-FILE.                                                  NC2524.2
003800 01  PRINT-REC PICTURE X(120).                                    NC2524.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC2524.2
004000 WORKING-STORAGE SECTION.                                         NC2524.2
004100 01  WS-REMAINDERS.                                               NC2524.2
004200   03  WS-REM                    PIC 99 OCCURS 20.                NC2524.2
004300 01  WRK-XN-00001-1              PIC X.                           NC2524.2
004400 01  WRK-XN-00001-2              PIC X.                           NC2524.2
004500 01  WS-46.                                                       NC2524.2
004600   03  WS-1-20                   PIC X(20).                       NC2524.2
004700   03  WS-21-40                  PIC X(20).                       NC2524.2
004800   03  WS-41-46                  PIC X(6).                        NC2524.2
004900 77  11A                PICTURE 9999  VALUE 9.                    NC2524.2
005000 77  11B   PICTURE 99; VALUE 8.                                   NC2524.2
005100 77  1111C PICTURE 99 VALUE 9.                                    NC2524.2
005200 77  WRK-DS-02V00                 PICTURE S99.                    NC2524.2
005300     88 TEST-2NUC-COND-99         VALUE 99.                       NC2524.2
005400 77  A99-DS-02V00                 PICTURE S99    VALUE 99.        NC2524.2
005500 77  WRK-DS-18V00                 PICTURE S9(18).                 NC2524.2
005600 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC2524.2
005700                                  VALUE 111111111111111111.       NC2524.2
005800 77  A18TWOS-DS-18V00             PICTURE S9(18)                  NC2524.2
005900                                  VALUE 222222222222222222.       NC2524.2
006000 77  WRK-DS-05V00                 PICTURE S9(5).                  NC2524.2
006100 77  A02TWOS-DU-02V00             PICTURE 99     VALUE 22.        NC2524.2
006200 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  NC2524.2
006300 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         NC2524.2
006400 77  AZERO-DS-05V05               PICTURE S9(5)V9(5) VALUE ZERO.  NC2524.2
006500 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC2524.2
006600 77  WRK-DS-0201P                 PICTURE S99P.                   NC2524.2
006700 77  A05ONES-DS-05V00             PICTURE S9(5)  VALUE 11111.     NC2524.2
006800 77  WRK-DS-09V00                 PICTURE S9(9).                  NC2524.2
006900 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC2524.2
007000 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09                        NC2524.2
007100                                  PICTURE S9(18).                 NC2524.2
007200 77  XRAY                    PICTURE IS X.                        NC2524.2
007300 77  W-1                     PICTURE IS 9.                        NC2524.2
007400 77  W-2                     PICTURE IS 99.                       NC2524.2
007500 77  W-3                     PICTURE IS 999.                      NC2524.2
007600 77  W-5                PICTURE 99  VALUE ZERO.                   NC2524.2
007700 77  W-9                     PICTURE 999.                         NC2524.2
007800 77  W-11               PICTURE S99V9.                            NC2524.2
007900 77  D-1                PICTURE S9V99  VALUE 1.06.                NC2524.2
008000 77  D-7                PICTURE S99V99  VALUE 1.09.               NC2524.2
008100 77  ONE                     PICTURE IS 9      VALUE IS 1.        NC2524.2
008200 77  TWO                     PICTURE IS S9     VALUE IS 2.        NC2524.2
008300 77  THREE                   PICTURE IS S9     VALUE IS 3.        NC2524.2
008400 77  FOUR                    PICTURE IS S9     VALUE IS 4.        NC2524.2
008500 77  FIVE                    PICTURE IS S9     VALUE IS 5.        NC2524.2
008600 77  SIX                     PICTURE IS S9     VALUE IS 6.        NC2524.2
008700 77  SEVEN                   PICTURE IS S9     VALUE IS 7.        NC2524.2
008800 77  EIGHT                   PICTURE IS 9      VALUE IS 8.        NC2524.2
008900 77  NINE                    PICTURE IS S9     VALUE IS 9.        NC2524.2
009000 77  TEN                     PICTURE IS S99    VALUE IS 10.       NC2524.2
009100 77  FIFTEEN                 PICTURE IS S99    VALUE IS 15.       NC2524.2
009200 77  TWENTY                  PICTURE IS S99    VALUE IS 20.       NC2524.2
009300 77  TWENTY-5                PICTURE IS S99    VALUE IS 25.       NC2524.2
009400    1 COMPUTE-DATA.                                               NC2524.2
009500                                                                02NC2524.2
009600     COMPUTE-1                    PICTURE 999V9999  VALUE ZERO.   NC2524.2
009700     2 COMPUTE-1A            PICTURE 9(3)V9(4) VALUE 654.1873.    NC2524.2
009800     2 COMPUTE-2             PICTURE 9999V9    VALUE ZERO.        NC2524.2
009900     02 COMPUTE-3            PICTURE 999V99    VALUE ZERO.        NC2524.2
010000     2 COMPUTE-3A            PICTURE 999V99    VALUE 86.14.       NC2524.2
010100     2 COMPUTE-3B            PICTURE 999V99    VALUE 33.75.       NC2524.2
010200     2 COMPUTE-4             PICTURE 999       VALUE ZERO.        NC2524.2
010300     2 COMPUTE-4A            PICTURE 999       VALUE 124.         NC2524.2
010400     2 COMPUTE-4B            PICTURE 999       VALUE 217.         NC2524.2
010500     2 COMPUTE-5             PICTURE 9999V99   VALUE ZERO.        NC2524.2
010600     02 COMPUTE-5A           PICTURE 999V9     VALUE 11.1.        NC2524.2
010700     2 COMPUTE-6             PICTURE 999V9     VALUE ZERO.        NC2524.2
010800     2 COMPUTE-6A            PICTURE 999V9     VALUE 374.4.       NC2524.2
010900     2 COMPUTE-7             PICTURE 999       VALUE ZERO.        NC2524.2
011000     2 COMPUTE-8             PICTURE 999       VALUE ZERO.        NC2524.2
011100     02 COMPUTE-9            PICTURE 9999      VALUE ZERO.        NC2524.2
011200     2 COMPUTE-10            PICTURE 999V9999  VALUE ZERO.        NC2524.2
011300     2 COMPUTE-11            PICTURE 999V9     VALUE ZERO.        NC2524.2
011400     2 COMPUTE-11A           PICTURE 999V9     VALUE 371.2.       NC2524.2
011500     2 COMPUTE-11B           PICTURE 999V9     VALUE 468.9.       NC2524.2
011600     2 COMPUTE-12            PICTURE 99V99     VALUE ZERO.        NC2524.2
011700     2 COMPUTE-12A           PICTURE 999V9     VALUE 336.4.       NC2524.2
011800     2 COMPUTE-12B           PICTURE 999V9     VALUE 281.7.       NC2524.2
011900 01  RENAMES-DATA.                                                NC2524.2
012000     02  NAME1.                                                   NC2524.2
012100         03 NAME1A PICTURE XX VALUE SPACE.                        NC2524.2
012200         03 NAME1B PICTURE XXX VALUE SPACE.                       NC2524.2
012300     02  NAME2 PICTURE X(10) VALUE SPACE.                         NC2524.2
012400     02  NAME3.                                                   NC2524.2
012500         09 NAME3A PICTURE XXX VALUE SPACE.                       NC2524.2
012600         09 NAME3B PICTURE XX VALUE SPACE.                        NC2524.2
012700 66  RENAME1 RENAMES NAME1 THRU NAME3.                            NC2524.2
012800 66  RENAME2 RENAMES NAME1A THRU NAME1B.                          NC2524.2
012900 66  RENAME3 RENAMES NAME2.                                       NC2524.2
013000 66  RENAME4 RENAMES NAME1.                                       NC2524.2
013100 01  GRP-FOR-RENAMES.                                             NC2524.2
013200     03  SUB-GRP-FOR-RENAMES-1.                                   NC2524.2
013300     05  ELEM-FOR-RENAMES-1      PICTURE X    VALUE "X".          NC2524.2
013400     05  FILLER                  PICTURE XX   VALUE SPACE.        NC2524.2
013500     03  SUB-GRP-FOR-RENAMES-2.                                   NC2524.2
013600     49  ELEM-FOR-RENAMES-2      PICTURE 999  VALUE 123.          NC2524.2
013700     49  FILLER                  PICTURE 9    VALUE ZERO.         NC2524.2
013800     49  ELEM-FOR-RENAMES-3      PICTURE XXXX VALUE ZERO.         NC2524.2
013900     66  RENAMES-TEST-1 RENAMES ELEM-FOR-RENAMES-2.               NC2524.2
014000     66  RENAMES-TEST-2 RENAMES SUB-GRP-FOR-RENAMES-1             NC2524.2
014100         OF GRP-FOR-RENAMES.                                      NC2524.2
014200     66  RENAMES-TEST-3 RENAMES SUB-GRP-FOR-RENAMES-1             NC2524.2
014300         THRU ELEM-FOR-RENAMES-2.                                 NC2524.2
014400     66  RENAMES-TEST-4 RENAMES ELEM-FOR-RENAMES-1                NC2524.2
014500         THRU ELEM-FOR-RENAMES-2 IN GRP-FOR-RENAMES.              NC2524.2
014600 01  T-RENAMES-DATA.                                              NC2524.2
014700     02 TAG-1.                                                    NC2524.2
014800        03 TAG-1A       PICTURE XXXX.                             NC2524.2
014900         03 TAG-1B      PICTURE XXXXXX.                           NC2524.2
015000     02 NAME-2          PICTURE XXXXXXX.                          NC2524.2
015100 66  RENAME-5 RENAMES TAG-1A THRU TAG-1B.                         NC2524.2
015200 66  RENAME-6 RENAMES TAG-1A THRU NAME-2 OF T-RENAMES-DATA.       NC2524.2
015300 01  U-RENAMES-DATA.                                              NC2524.2
015400     02 UNIT-1.                                                   NC2524.2
015500         03 UNIT-1A     PICTURE XXXXXXX VALUE "VERMONT".          NC2524.2
015600         03 UNIT-1B     PICTURE XXXX    VALUE "OHIO".             NC2524.2
015700     02 NAME-2          PICTURE XXXXX   VALUE "MAINE".            NC2524.2
015800 66  RENAME-5 RENAMES UNIT-1A THROUGH UNIT-1B.                    NC2524.2
015900 66  RENAME-6 RENAMES UNIT-1A THRU NAME-2 OF U-RENAMES-DATA.      NC2524.2
016000 01  V-RENAMES-DATA.                                              NC2524.2
016100     02 ITEM-1          PICTURE X(5).                             NC2524.2
016200     02 TABLE-2.                                                  NC2524.2
016300         03 TABLE-ITEM-2 PICTURE XXX OCCURS 5 TIMES.              NC2524.2
016400 66  RENAME-7 RENAMES ITEM-1 THRU TABLE-2.                        NC2524.2
016500 01  W-RENAMES-DATA.                                              NC2524.2
016600     02 WIDGET-1        PICTURE 99V9.                             NC2524.2
016700     02 WIDGET-2        PICTURE ***,***.**.                       NC2524.2
016800     02 WIDGET-3        PICTURE XXXX.                             NC2524.2
016900     02 WIDGET-4        PICTURE 9(4).                             NC2524.2
017000     02 WIDGET-5        PICTURE 9(4).                             NC2524.2
017100 66  RENAME-8  RENAMES WIDGET-1 THRU WIDGET-3.                    NC2524.2
017200 66  RENAME-9  RENAMES WIDGET-3 THRU WIDGET-5.                    NC2524.2
017300 66  RENAME-10 RENAMES WIDGET-4 THRU WIDGET-5.                    NC2524.2
017400 66  RENAME-11 RENAMES WIDGET-2.                                  NC2524.2
017500 66  RENAME-12 RENAMES WIDGET-4.                                  NC2524.2
017600 01  REDEF10.                                                     NC2524.2
017700     02  RDFDATA1                PICTURE X(10) VALUE "ABC98765DE".NC2524.2
017800     02  RDFDATA2                PICTURE 9(4)V99 VALUE 9116.44.   NC2524.2
017900     02  RDFDATA3.                                                NC2524.2
018000         08  RDFDATA4            PICTURE X(6)  VALUE "ALLDON".    NC2524.2
018100         08  RDFDATA5            PICTURE XX99  VALUE "XX66".      NC2524.2
018200     02  RDF3 REDEFINES RDFDATA3.                                 NC2524.2
018300         03  RDF3-4              PICTURE X(8).                    NC2524.2
018400         03  RDF3-5              PIC 99.                          NC2524.2
018500         03  RDF3-5-1 REDEFINES RDF3-5.                           NC2524.2
018600             04  RDF3-5-14  PIC 9.                                NC2524.2
018700             04  RDF3-5-15  PIC 9.                                NC2524.2
018800                 88  HARD  VALUE 0.                               NC2524.2
018900                 88 SOFT  VALUE 1.                                NC2524.2
019000     02  RDFDATA6                PICTURE A(20) VALUE              NC2524.2
019100     "ZYXWVUTSRQPONMLKJIHG".                                      NC2524.2
019200     66  RDF3-5-16 RENAMES RDF3-5.                                NC2524.2
019300 01  REDEF11 REDEFINES REDEF10.                                   NC2524.2
019400     02  RDFDATA7                PICTURE X(20).                   NC2524.2
019500     02  RDF8.                                                    NC2524.2
019600         03   RDFDATA8 OCCURS 36 TIMES PICTURE XX.                NC2524.2
019700     02  RDEF8 REDEFINES RDF8.                                    NC2524.2
019800         03  RDF8-1              PICTURE X(50).                   NC2524.2
019900         03  RDF8-2              PIC X(9).                        NC2524.2
020000         03  RDF8-3 REDEFINES RDF8-2.                             NC2524.2
020100             04  RDF8-4          PIC X(5).                        NC2524.2
020200             04  RDF8-5          PICTURE XX.                      NC2524.2
020300             04  RDF8-6  PIC XX.                                  NC2524.2
020400         03  RDF8-8              PIC X(13).                       NC2524.2
020500     66  RDF8-7 RENAMES RDF8-5 THRU RDF8-6.                       NC2524.2
020600 01  REDEF12 REDEFINES REDEF10.                                   NC2524.2
020700     02  RDFDATA9                PICTURE A(3).                    NC2524.2
020800     02  RDFDATA10     PIC 9(5).                                  NC2524.2
020900     02  RDFDATA11.                                               NC2524.2
021000         03  RDFDATA12.                                           NC2524.2
021100             04  RDFDATA13       PICTURE XX.                      NC2524.2
021200             04  RDFDATA14 OCCURS 6 TIMES PICTURE 9.              NC2524.2
021300         03  RDFDATA15           PICTURE X(8).                    NC2524.2
021400     02  RDFDATA16               PICTURE 99.                      NC2524.2
021500     02  RDFDATA17               PICTURE X(80).                   NC2524.2
021600     02  RDFDATA18               PICTURE X(14).                   NC2524.2
021700 01  GRP-REDEF125   REDEFINES REDEF10.                            NC2524.2
021800     02 AN0020-X-0001             PIC X(26).                      NC2524.2
021900     02 AN0002-O036F-X-0002       PIC XX  OCCURS 36 TIMES.        NC2524.2
022000 01  WRK-DU-05V00-0001            PIC 9(5).                       NC2524.2
022100 01  WRK-DS-05V00-0002            PIC S9(5).                      NC2524.2
022200 01  WRK-CS-05V00-0003            PIC S9(5) COMP.                 NC2524.2
022300 01  WRK-DU-04V02-0004            PIC 9(4)V9(2).                  NC2524.2
022400 01  WRK-DS-04V01-0005            PIC S9(4)V9.                    NC2524.2
022500 01  NE-0008                      PIC $9(4).99-.                  NC2524.2
022600 01  NE-0009                      PIC ***99.                      NC2524.2
022700 01  NE-04V01-0006     PIC ****.9.                                NC2524.2
022800 01  GRP-0010.                                                    NC2524.2
022900     02 WRK-DU-03V00-L-0011       PIC 9(03) SYNC LEFT.            NC2524.2
023000     02 WRK-O005F-0012        OCCURS   5  TIMES.                  NC2524.2
023100        03 WRK-O003F-0013     OCCURS   3  TIMES.                  NC2524.2
023200           05 WRK-DS-03V04-0003F-0014 PIC S9(3)V9999              NC2524.2
023300                                            OCCURS 3 TIMES.       NC2524.2
023400 01  DS-02V00-0001                PIC S99  VALUE  16.             NC2524.2
023500 01  DS-03V00-0002                PIC S999 VALUE  174.            NC2524.2
023600 01  CS-05V00-0003                PIC S9(5) COMP  VALUE 10.       NC2524.2
023700 01    TA--X           PIC 9(5)  COMP VALUE ZERO.                 NC2524.2
023800 01  REDEF13.                                                     NC2524.2
023900     02  FILLER                  PICTURE X(57)   VALUE            NC2524.2
024000     "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA". NC2524.2
024100     02  FILLER                  PICTURE X(57)   VALUE            NC2524.2
024200     "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA". NC2524.2
024300     02  FILLER                  PICTURE X(6)    VALUE "AAAAAA".  NC2524.2
024400 01  MINUS-NAMES.                                                 NC2524.2
024500     02  WHOLE-FIELD              PICTURE S9(18).                 NC2524.2
024600     02  PLUS-NAME1  PICTURE S9(18) VALUE +333333333333333333.    NC2524.2
024700     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC2524.2
024800     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC2524.2
024900     02  ALPHA-LIT                PICTURE X(5)  VALUE SPACE.      NC2524.2
025000     02  SNEG-LIT2                PICTURE S9(5)  VALUE -70718.    NC2524.2
025100 01  TEST-RESULTS.                                                NC2524.2
025200     02 FILLER                   PIC X      VALUE SPACE.          NC2524.2
025300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2524.2
025400     02 FILLER                   PIC X      VALUE SPACE.          NC2524.2
025500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2524.2
025600     02 FILLER                   PIC X      VALUE SPACE.          NC2524.2
025700     02  PAR-NAME.                                                NC2524.2
025800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2524.2
025900       03  PARDOT-X              PIC X      VALUE SPACE.          NC2524.2
026000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2524.2
026100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2524.2
026200     02 RE-MARK                  PIC X(61).                       NC2524.2
026300 01  TEST-COMPUTED.                                               NC2524.2
026400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2524.2
026500     02 FILLER                   PIC X(17)  VALUE                 NC2524.2
026600            "       COMPUTED=".                                   NC2524.2
026700     02 COMPUTED-X.                                               NC2524.2
026800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2524.2
026900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2524.2
027000                                 PIC -9(9).9(9).                  NC2524.2
027100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2524.2
027200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2524.2
027300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2524.2
027400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2524.2
027500         04 COMPUTED-18V0                    PIC -9(18).          NC2524.2
027600         04 FILLER                           PIC X.               NC2524.2
027700     03 FILLER PIC X(50) VALUE SPACE.                             NC2524.2
027800 01  TEST-CORRECT.                                                NC2524.2
027900     02 FILLER PIC X(30) VALUE SPACE.                             NC2524.2
028000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2524.2
028100     02 CORRECT-X.                                                NC2524.2
028200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2524.2
028300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2524.2
028400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2524.2
028500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2524.2
028600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2524.2
028700     03      CR-18V0 REDEFINES CORRECT-A.                         NC2524.2
028800         04 CORRECT-18V0                     PIC -9(18).          NC2524.2
028900         04 FILLER                           PIC X.               NC2524.2
029000     03 FILLER PIC X(2) VALUE SPACE.                              NC2524.2
029100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2524.2
029200 01  CCVS-C-1.                                                    NC2524.2
029300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2524.2
029400-    "SS  PARAGRAPH-NAME                                          NC2524.2
029500-    "       REMARKS".                                            NC2524.2
029600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2524.2
029700 01  CCVS-C-2.                                                    NC2524.2
029800     02 FILLER                     PIC X        VALUE SPACE.      NC2524.2
029900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2524.2
030000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2524.2
030100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2524.2
030200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2524.2
030300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2524.2
030400 01  REC-CT                        PIC 99       VALUE ZERO.       NC2524.2
030500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2524.2
030600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2524.2
030700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2524.2
030800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2524.2
030900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2524.2
031000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2524.2
031100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2524.2
031200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2524.2
031300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2524.2
031400 01  CCVS-H-1.                                                    NC2524.2
031500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2524.2
031600     02  FILLER                    PIC X(42)    VALUE             NC2524.2
031700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2524.2
031800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2524.2
031900 01  CCVS-H-2A.                                                   NC2524.2
032000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2524.2
032100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2524.2
032200   02  FILLER                        PIC XXXX   VALUE             NC2524.2
032300     "4.2 ".                                                      NC2524.2
032400   02  FILLER                        PIC X(28)  VALUE             NC2524.2
032500            " COPY - NOT FOR DISTRIBUTION".                       NC2524.2
032600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2524.2
032700                                                                  NC2524.2
032800 01  CCVS-H-2B.                                                   NC2524.2
032900   02  FILLER                        PIC X(15)  VALUE             NC2524.2
033000            "TEST RESULT OF ".                                    NC2524.2
033100   02  TEST-ID                       PIC X(9).                    NC2524.2
033200   02  FILLER                        PIC X(4)   VALUE             NC2524.2
033300            " IN ".                                               NC2524.2
033400   02  FILLER                        PIC X(12)  VALUE             NC2524.2
033500     " HIGH       ".                                              NC2524.2
033600   02  FILLER                        PIC X(22)  VALUE             NC2524.2
033700            " LEVEL VALIDATION FOR ".                             NC2524.2
033800   02  FILLER                        PIC X(58)  VALUE             NC2524.2
033900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2524.2
034000 01  CCVS-H-3.                                                    NC2524.2
034100     02  FILLER                      PIC X(34)  VALUE             NC2524.2
034200            " FOR OFFICIAL USE ONLY    ".                         NC2524.2
034300     02  FILLER                      PIC X(58)  VALUE             NC2524.2
034400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2524.2
034500     02  FILLER                      PIC X(28)  VALUE             NC2524.2
034600            "  COPYRIGHT   1985 ".                                NC2524.2
034700 01  CCVS-E-1.                                                    NC2524.2
034800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2524.2
034900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2524.2
035000     02 ID-AGAIN                     PIC X(9).                    NC2524.2
035100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2524.2
035200 01  CCVS-E-2.                                                    NC2524.2
035300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2524.2
035400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2524.2
035500     02 CCVS-E-2-2.                                               NC2524.2
035600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2524.2
035700         03 FILLER                   PIC X      VALUE SPACE.      NC2524.2
035800         03 ENDER-DESC               PIC X(44)  VALUE             NC2524.2
035900            "ERRORS ENCOUNTERED".                                 NC2524.2
036000 01  CCVS-E-3.                                                    NC2524.2
036100     02  FILLER                      PIC X(22)  VALUE             NC2524.2
036200            " FOR OFFICIAL USE ONLY".                             NC2524.2
036300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2524.2
036400     02  FILLER                      PIC X(58)  VALUE             NC2524.2
036500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2524.2
036600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2524.2
036700     02 FILLER                       PIC X(15)  VALUE             NC2524.2
036800             " COPYRIGHT 1985".                                   NC2524.2
036900 01  CCVS-E-4.                                                    NC2524.2
037000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2524.2
037100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2524.2
037200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2524.2
037300     02 FILLER                       PIC X(40)  VALUE             NC2524.2
037400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2524.2
037500 01  XXINFO.                                                      NC2524.2
037600     02 FILLER                       PIC X(19)  VALUE             NC2524.2
037700            "*** INFORMATION ***".                                NC2524.2
037800     02 INFO-TEXT.                                                NC2524.2
037900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2524.2
038000       04 XXCOMPUTED                 PIC X(20).                   NC2524.2
038100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2524.2
038200       04 XXCORRECT                  PIC X(20).                   NC2524.2
038300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2524.2
038400 01  HYPHEN-LINE.                                                 NC2524.2
038500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2524.2
038600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2524.2
038700-    "*****************************************".                 NC2524.2
038800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2524.2
038900-    "******************************".                            NC2524.2
039000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2524.2
039100     "NC252A".                                                    NC2524.2
039200 PROCEDURE DIVISION.                                              NC2524.2
039300 CCVS1 SECTION.                                                   NC2524.2
039400 OPEN-FILES.                                                      NC2524.2
039500     OPEN     OUTPUT PRINT-FILE.                                  NC2524.2
039600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2524.2
039700     MOVE    SPACE TO TEST-RESULTS.                               NC2524.2
039800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2524.2
039900     GO TO CCVS1-EXIT.                                            NC2524.2
040000 CLOSE-FILES.                                                     NC2524.2
040100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2524.2
040200 TERMINATE-CCVS.                                                  NC2524.2
040300     STOP     RUN.                                                NC2524.2
040400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2524.2
040500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2524.2
040600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2524.2
040700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2524.2
040800     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2524.2
040900 PRINT-DETAIL.                                                    NC2524.2
041000     IF REC-CT NOT EQUAL TO ZERO                                  NC2524.2
041100             MOVE "." TO PARDOT-X                                 NC2524.2
041200             MOVE REC-CT TO DOTVALUE.                             NC2524.2
041300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2524.2
041400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2524.2
041500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2524.2
041600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2524.2
041700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2524.2
041800     MOVE SPACE TO CORRECT-X.                                     NC2524.2
041900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2524.2
042000     MOVE     SPACE TO RE-MARK.                                   NC2524.2
042100 HEAD-ROUTINE.                                                    NC2524.2
042200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2524.2
042300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2524.2
042400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2524.2
042500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2524.2
042600 COLUMN-NAMES-ROUTINE.                                            NC2524.2
042700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2524.2
042800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2524.2
042900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2524.2
043000 END-ROUTINE.                                                     NC2524.2
043100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2524.2
043200 END-RTN-EXIT.                                                    NC2524.2
043300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2524.2
043400 END-ROUTINE-1.                                                   NC2524.2
043500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2524.2
043600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2524.2
043700      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2524.2
043800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2524.2
043900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2524.2
044000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2524.2
044100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2524.2
044200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2524.2
044300  END-ROUTINE-12.                                                 NC2524.2
044400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2524.2
044500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2524.2
044600         MOVE "NO " TO ERROR-TOTAL                                NC2524.2
044700         ELSE                                                     NC2524.2
044800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2524.2
044900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2524.2
045000     PERFORM WRITE-LINE.                                          NC2524.2
045100 END-ROUTINE-13.                                                  NC2524.2
045200     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2524.2
045300         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2524.2
045400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2524.2
045500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2524.2
045600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2524.2
045700      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2524.2
045800          MOVE "NO " TO ERROR-TOTAL                               NC2524.2
045900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2524.2
046000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2524.2
046100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2524.2
046200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2524.2
046300 WRITE-LINE.                                                      NC2524.2
046400     ADD 1 TO RECORD-COUNT.                                       NC2524.2
046500*    IF RECORD-COUNT GREATER 50                                   NC2524.2
046600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2524.2
046700*        MOVE SPACE TO DUMMY-RECORD                               NC2524.2
046800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2524.2
046900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2524.2
047000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2524.2
047100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2524.2
047200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2524.2
047300*        MOVE ZERO TO RECORD-COUNT.                               NC2524.2
047400     PERFORM WRT-LN.                                              NC2524.2
047500 WRT-LN.                                                          NC2524.2
047600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2524.2
047700     MOVE SPACE TO DUMMY-RECORD.                                  NC2524.2
047800 BLANK-LINE-PRINT.                                                NC2524.2
047900     PERFORM WRT-LN.                                              NC2524.2
048000 FAIL-ROUTINE.                                                    NC2524.2
048100     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2524.2
048200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2524.2
048300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2524.2
048400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2524.2
048500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2524.2
048600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2524.2
048700     GO TO  FAIL-ROUTINE-EX.                                      NC2524.2
048800 FAIL-ROUTINE-WRITE.                                              NC2524.2
048900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2524.2
049000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2524.2
049100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2524.2
049200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2524.2
049300 FAIL-ROUTINE-EX. EXIT.                                           NC2524.2
049400 BAIL-OUT.                                                        NC2524.2
049500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2524.2
049600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2524.2
049700 BAIL-OUT-WRITE.                                                  NC2524.2
049800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2524.2
049900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2524.2
050000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2524.2
050100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2524.2
050200 BAIL-OUT-EX. EXIT.                                               NC2524.2
050300 CCVS1-EXIT.                                                      NC2524.2
050400     EXIT.                                                        NC2524.2
050500 SECT-NC252A-001 SECTION.                                         NC2524.2
050600 RDF-INIT.                                                        NC2524.2
050700     MOVE "REDEFINES " TO FEATURE.                                NC2524.2
050800 RDF-TEST-1.                                                      NC2524.2
050900     IF HARD                                                      NC2524.2
051000         MOVE RDF3-5-15 TO COMPUTED-A                             NC2524.2
051100         MOVE 6 TO CORRECT-A                                      NC2524.2
051200         PERFORM FAIL                                             NC2524.2
051300         GO TO RDF-WRITE-1.                                       NC2524.2
051400*    NOTE 88 LEVEL CONDITION TEST ON REDEFINED FIELD.             NC2524.2
051500     PERFORM PASS.                                                NC2524.2
051600     GO TO RDF-WRITE-1.                                           NC2524.2
051700 RDF-DELETE-1.                                                    NC2524.2
051800     PERFORM DE-LETE.                                             NC2524.2
051900 RDF-WRITE-1.                                                     NC2524.2
052000     MOVE "RDF-TEST-1 " TO PAR-NAME.                              NC2524.2
052100     PERFORM PRINT-DETAIL.                                        NC2524.2
052200 RDF-TEST-2.                                                      NC2524.2
052300     IF RDF3-5-16 EQUAL TO 66                                     NC2524.2
052400         PERFORM PASS                                             NC2524.2
052500         GO TO RDF-WRITE-2.                                       NC2524.2
052600*    NOTE USING A RENAMES DATANAME THAT IS ALSO REDEFINED.        NC2524.2
052700     MOVE RDF3-5-16 TO COMPUTED-A.                                NC2524.2
052800     MOVE 66 TO CORRECT-A.                                        NC2524.2
052900     PERFORM FAIL.                                                NC2524.2
053000     GO TO RDF-WRITE-2.                                           NC2524.2
053100 RDF-DELETE-2.                                                    NC2524.2
053200     PERFORM DE-LETE.                                             NC2524.2
053300 RDF-WRITE-2.                                                     NC2524.2
053400     MOVE "RDF-TEST-2 " TO PAR-NAME.                              NC2524.2
053500     PERFORM PRINT-DETAIL.                                        NC2524.2
053600 RDF-TEST-003.                                                    NC2524.2
053700     IF   AN0002-O036F-X-0002  (8)  EQUAL TO  "LK"                NC2524.2
053800          PERFORM  PASS                                           NC2524.2
053900          GO TO   RDF-WRITE-003.                                  NC2524.2
054000*                                                                 NC2524.2
054100*    NOTE  REFERENCING    SUBSCRIPTED DATA ITEM WHICH IS          NC2524.2
054200*          SUBORDINATE TO A REDEFINES CLAUSE.                     NC2524.2
054300*                                                                 NC2524.2
054400     MOVE  AN0002-O036F-X-0002 (8) TO  COMPUTED-A.                NC2524.2
054500     MOVE   "LK"  TO CORRECT-A.                                   NC2524.2
054600     PERFORM  FAIL.                                               NC2524.2
054700     GO TO    RDF-WRITE-003.                                      NC2524.2
054800 RDF-DELETE-003.                                                  NC2524.2
054900     PERFORM  DE-LETE.                                            NC2524.2
055000 RDF-WRITE-003.                                                   NC2524.2
055100     MOVE     "RDF-TEST-003"  TO  PAR-NAME.                       NC2524.2
055200     PERFORM  PRINT-DETAIL.                                       NC2524.2
055300 RDF-TEST-4.                                                      NC2524.2
055400     IF RDFDATA7 EQUAL TO "ABC98765DE911644ALLD"                  NC2524.2
055500         PERFORM PASS                                             NC2524.2
055600         GO TO RDF-WRITE-4.                                       NC2524.2
055700*    NOTE THIS IS THE FIRST REFERENCE TO THESE REDEFINED          NC2524.2
055800*        DATANAMES, SO, THE FIELDS CONTAIN THE WORKING-STORAGE    NC2524.2
055900*        ASSIGNED VALUES.                                         NC2524.2
056000     MOVE RDFDATA7 TO COMPUTED-A.                                 NC2524.2
056100     MOVE "ABC98765DE911644ALLD" TO CORRECT-A.                    NC2524.2
056200     PERFORM FAIL.                                                NC2524.2
056300     GO TO RDF-WRITE-4.                                           NC2524.2
056400 RDF-DELETE-4.                                                    NC2524.2
056500     PERFORM DE-LETE.                                             NC2524.2
056600 RDF-WRITE-4.                                                     NC2524.2
056700     MOVE "RDF-TEST-4 " TO PAR-NAME.                              NC2524.2
056800     PERFORM PRINT-DETAIL.                                        NC2524.2
056900 RDF-TEST-5.                                                      NC2524.2
057000     IF RDFDATA8 (13) EQUAL TO "HG"                               NC2524.2
057100         PERFORM PASS                                             NC2524.2
057200         GO TO RDF-WRITE-5.                                       NC2524.2
057300     MOVE "HG" TO CORRECT-A.                                      NC2524.2
057400     MOVE RDFDATA8 (13) TO COMPUTED-A.                            NC2524.2
057500     PERFORM FAIL.                                                NC2524.2
057600     GO TO RDF-WRITE-5.                                           NC2524.2
057700 RDF-DELETE-5.                                                    NC2524.2
057800     PERFORM DE-LETE.                                             NC2524.2
057900 RDF-WRITE-5.                                                     NC2524.2
058000     MOVE "RDF-TEST-5 " TO PAR-NAME.                              NC2524.2
058100     PERFORM PRINT-DETAIL.                                        NC2524.2
058200 RDF-TEST-6.                                                      NC2524.2
058300     IF RDFDATA2 EQUAL TO 9116.44                                 NC2524.2
058400         PERFORM PASS                                             NC2524.2
058500         GO TO RDF-WRITE-6.                                       NC2524.2
058600     MOVE 9116.44 TO COMPUTED-N.                                  NC2524.2
058700     MOVE RDFDATA2 TO CORRECT-N.                                  NC2524.2
058800     PERFORM FAIL.                                                NC2524.2
058900     GO TO RDF-WRITE-6.                                           NC2524.2
059000 RDF-DELETE-6.                                                    NC2524.2
059100     PERFORM DE-LETE.                                             NC2524.2
059200 RDF-WRITE-6.                                                     NC2524.2
059300     MOVE "RDF-TEST-6 " TO PAR-NAME.                              NC2524.2
059400     PERFORM PRINT-DETAIL.                                        NC2524.2
059500 RDF-TEST-7.                                                      NC2524.2
059600     IF RDFDATA16 EQUAL TO 66                                     NC2524.2
059700         PERFORM PASS                                             NC2524.2
059800         GO TO RDF-WRITE-7.                                       NC2524.2
059900     MOVE RDFDATA16 TO COMPUTED-A.                                NC2524.2
060000     MOVE 66 TO CORRECT-A.                                        NC2524.2
060100     PERFORM FAIL.                                                NC2524.2
060200     GO TO RDF-WRITE-7.                                           NC2524.2
060300 RDF-DELETE-7.                                                    NC2524.2
060400     PERFORM DE-LETE.                                             NC2524.2
060500 RDF-WRITE-7.                                                     NC2524.2
060600     MOVE "RDF-TEST-7 " TO PAR-NAME.                              NC2524.2
060700     PERFORM PRINT-DETAIL.                                        NC2524.2
060800 RDF-TEST-8.                                                      NC2524.2
060900     MOVE SPACE TO REDEF12.                                       NC2524.2
061000     IF REDEF11 EQUAL TO SPACE                                    NC2524.2
061100         PERFORM PASS                                             NC2524.2
061200         GO TO RDF-WRITE-8.                                       NC2524.2
061300     MOVE "SPACE EXPECTED " TO CORRECT-A.                         NC2524.2
061400     MOVE "NON BLANK CHARACTERS" TO COMPUTED-A.                   NC2524.2
061500     MOVE "REDEF11 CONTAINS NON BLANKS" TO RE-MARK.               NC2524.2
061600     PERFORM FAIL.                                                NC2524.2
061700     GO TO RDF-WRITE-8.                                           NC2524.2
061800 RDF-DELETE-8.                                                    NC2524.2
061900     PERFORM DE-LETE.                                             NC2524.2
062000 RDF-WRITE-8.                                                     NC2524.2
062100     MOVE "RDF-TEST-8 " TO PAR-NAME.                              NC2524.2
062200     PERFORM PRINT-DETAIL.                                        NC2524.2
062300 RDF-TEST-9.                                                      NC2524.2
062400     MOVE ZERO TO REDEF12.                                        NC2524.2
062500     MOVE SPACE TO REDEF11.                                       NC2524.2
062600*    NOTE  CHECKS RDFDATA18 WHICH SHOULD NOT BE DISTURBED BY THE  NC2524.2
062700*        MOVE SPACE STATEMENT TO A SHORTER REDEFINED AREA.        NC2524.2
062800     IF RDFDATA18 EQUAL TO ZERO                                   NC2524.2
062900         PERFORM PASS                                             NC2524.2
063000         GO TO RDF-WRITE-9.                                       NC2524.2
063100     MOVE "00000000000000" TO CORRECT-A.                          NC2524.2
063200     MOVE RDFDATA18 TO COMPUTED-A.                                NC2524.2
063300     PERFORM FAIL.                                                NC2524.2
063400     GO TO RDF-WRITE-9.                                           NC2524.2
063500 RDF-DELETE-9.                                                    NC2524.2
063600     PERFORM DE-LETE.                                             NC2524.2
063700 RDF-WRITE-9.                                                     NC2524.2
063800     MOVE "RDF-TEST-9 " TO PAR-NAME.                              NC2524.2
063900     PERFORM PRINT-DETAIL.                                        NC2524.2
064000 RDF-TEST-10.                                                     NC2524.2
064100     MOVE ZERO TO REDEF12.                                        NC2524.2
064200     MOVE "MOVING DATA TO A REDEFINED FIELD CAN BE RISKY "        NC2524.2
064300         TO REDEF10.                                              NC2524.2
064400     IF RDFDATA8 (14) EQUAL TO "00"                               NC2524.2
064500         PERFORM PASS                                             NC2524.2
064600         GO TO RDF-WRITE-10.                                      NC2524.2
064700     MOVE 00 TO CORRECT-A.                                        NC2524.2
064800     MOVE RDFDATA8 (14) TO COMPUTED-A.                            NC2524.2
064900     PERFORM FAIL.                                                NC2524.2
065000     GO TO RDF-WRITE-10.                                          NC2524.2
065100 RDF-DELETE-10.                                                   NC2524.2
065200     PERFORM DE-LETE.                                             NC2524.2
065300 RDF-WRITE-10.                                                    NC2524.2
065400     MOVE "RDF-TEST-10 " TO PAR-NAME.                             NC2524.2
065500     PERFORM PRINT-DETAIL.                                        NC2524.2
065600 RDF-INIT-11.                                                     NC2524.2
065700     MOVE REDEF13 TO REDEF12.                                     NC2524.2
065800     MOVE "RDF-TEST-11 " TO PAR-NAME.                             NC2524.2
065900 RDF-TEST-11.                                                     NC2524.2
066000     IF REDEF10 EQUAL TO                                          NC2524.2
066100         "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"         NC2524.2
066200         PERFORM PASS                                             NC2524.2
066300         PERFORM PRINT-DETAIL                                     NC2524.2
066400         GO TO   RDF-TEST-12.                                     NC2524.2
066500     MOVE    1 TO REC-CT.                                         NC2524.2
066600     MOVE    REDEF10 TO WS-46.                                    NC2524.2
066700     MOVE   "AAAAAAAAAAAAAAAAAAAA" TO CORRECT-A.                  NC2524.2
066800     MOVE    WS-1-20 TO COMPUTED-A.                               NC2524.2
066900     MOVE   "FIELDS DIDNT COMPARE EQUAL " TO RE-MARK.             NC2524.2
067000     PERFORM FAIL.                                                NC2524.2
067100     PERFORM PRINT-DETAIL.                                        NC2524.2
067200     ADD     1 TO REC-CT.                                         NC2524.2
067300     MOVE   "AAAAAAAAAAAAAAAAAAAA" TO CORRECT-A.                  NC2524.2
067400     MOVE    WS-21-40 TO COMPUTED-A.                              NC2524.2
067500     MOVE   "FIELDS DIDNT COMPARE EQUAL " TO RE-MARK.             NC2524.2
067600     PERFORM FAIL.                                                NC2524.2
067700     PERFORM PRINT-DETAIL.                                        NC2524.2
067800     ADD     1 TO REC-CT.                                         NC2524.2
067900     MOVE   "AAAAAA"  TO CORRECT-A.                               NC2524.2
068000     MOVE    WS-41-46 TO COMPUTED-A.                              NC2524.2
068100     MOVE   "FIELDS DIDNT COMPARE EQUAL " TO RE-MARK.             NC2524.2
068200     PERFORM FAIL.                                                NC2524.2
068300     PERFORM PRINT-DETAIL.                                        NC2524.2
068400     GO TO   RDF-TEST-12.                                         NC2524.2
068500 RDF-DELETE-11.                                                   NC2524.2
068600     PERFORM DE-LETE.                                             NC2524.2
068700 RDF-TEST-12.                                                     NC2524.2
068800     MOVE 11 TO RDFDATA16.                                        NC2524.2
068900*    NOTE 88 LEVEL TEST ON REDEFINED AREA.                        NC2524.2
069000     IF  SOFT                                                     NC2524.2
069100         PERFORM PASS                                             NC2524.2
069200     ELSE                                                         NC2524.2
069300         MOVE "CONDITION-NAME TEST" TO RE-MARK                    NC2524.2
069400         PERFORM FAIL.                                            NC2524.2
069500     GO TO RDF-WRITE-12.                                          NC2524.2
069600 RDF-DELETE-12.                                                   NC2524.2
069700     PERFORM DE-LETE.                                             NC2524.2
069800 RDF-WRITE-12.                                                    NC2524.2
069900     MOVE "RDF-TEST-12" TO PAR-NAME.                              NC2524.2
070000     PERFORM PRINT-DETAIL.                                        NC2524.2
070100 RDF-TEST-13.                                                     NC2524.2
070200     MOVE REDEF13 TO REDEF12.                                     NC2524.2
070300     MOVE SPACE TO REDEF10.                                       NC2524.2
070400     IF RDF8-7 EQUAL TO SPACE                                     NC2524.2
070500         MOVE RDF8-7 TO COMPUTED-A                                NC2524.2
070600         MOVE "AAAA" TO CORRECT-A                                 NC2524.2
070700         PERFORM FAIL                                             NC2524.2
070800         GO TO RDF-WRITE-13.                                      NC2524.2
070900     PERFORM PASS.                                                NC2524.2
071000     GO TO RDF-WRITE-13.                                          NC2524.2
071100 RDF-DELETE-13.                                                   NC2524.2
071200     PERFORM DE-LETE.                                             NC2524.2
071300 RDF-WRITE-13.                                                    NC2524.2
071400     MOVE "RDF-TEST-13 " TO PAR-NAME.                             NC2524.2
071500     PERFORM PRINT-DETAIL.                                        NC2524.2
071600 RDF-TEST-14.                                                     NC2524.2
071700     MOVE SPACE TO REDEF12.                                       NC2524.2
071800     MOVE REDEF13 TO REDEF10.                                     NC2524.2
071900     IF RDF8-3 EQUAL TO "AAAAAAAAA"                               NC2524.2
072000         MOVE RDF8-3 TO COMPUTED-A                                NC2524.2
072100         MOVE SPACE TO CORRECT-A                                  NC2524.2
072200         PERFORM FAIL                                             NC2524.2
072300         GO TO RDF-WRITE-14.                                      NC2524.2
072400     PERFORM PASS.                                                NC2524.2
072500     GO TO RDF-WRITE-14.                                          NC2524.2
072600 RDF-DELETE-14.                                                   NC2524.2
072700     PERFORM DE-LETE.                                             NC2524.2
072800 RDF-WRITE-14.                                                    NC2524.2
072900     MOVE "RDF-TEST-14 " TO PAR-NAME.                             NC2524.2
073000     PERFORM PRINT-DETAIL.                                        NC2524.2
073100 RNM-INIT.                                                        NC2524.2
073200     PERFORM END-ROUTINE.                                         NC2524.2
073300     MOVE "RENAMES" TO FEATURE.                                   NC2524.2
073400 RENAM-TEST-1.                                                    NC2524.2
073500     MOVE "AB" TO NAME1A.                                         NC2524.2
073600     MOVE "CD" TO NAME1B.                                         NC2524.2
073700     IF RENAME4 EQUAL TO "ABCD "                                  NC2524.2
073800              PERFORM PASS                                        NC2524.2
073900              GO TO RNM-WRITE-1.                                  NC2524.2
074000     MOVE RENAME4 TO COMPUTED-A.                                  NC2524.2
074100     MOVE "ABCD" TO CORRECT-A.                                    NC2524.2
074200*    NOTE  CORRECT ANSWER IS   ABCD-BLANK.                        NC2524.2
074300     PERFORM FAIL.                                                NC2524.2
074400     GO TO RNM-WRITE-1.                                           NC2524.2
074500 RNM-DELETE-1.                                                    NC2524.2
074600     PERFORM DE-LETE.                                             NC2524.2
074700 RNM-WRITE-1.                                                     NC2524.2
074800     MOVE "RENAM-TEST-1" TO PAR-NAME.                             NC2524.2
074900     PERFORM PRINT-DETAIL.                                        NC2524.2
075000 RENAM-TEST-2.                                                    NC2524.2
075100     MOVE ALL "A" TO RENAMES-DATA.                                NC2524.2
075200     IF RENAME3 EQUAL TO "AAAAAAAAAA"                             NC2524.2
075300              PERFORM PASS                                        NC2524.2
075400              GO TO RNM-WRITE-2.                                  NC2524.2
075500     MOVE RENAME3 TO COMPUTED-A.                                  NC2524.2
075600     MOVE "AAAAAAAAAA" TO CORRECT-A.                              NC2524.2
075700     PERFORM FAIL.                                                NC2524.2
075800     GO TO RNM-WRITE-2.                                           NC2524.2
075900 RNM-DELETE-2.                                                    NC2524.2
076000     PERFORM DE-LETE.                                             NC2524.2
076100 RNM-WRITE-2.                                                     NC2524.2
076200     MOVE "RENAM-TEST-2" TO PAR-NAME.                             NC2524.2
076300     PERFORM PRINT-DETAIL.                                        NC2524.2
076400 RENAM-TEST-3.                                                    NC2524.2
076500     MOVE ALL "A" TO RENAMES-DATA.                                NC2524.2
076600     MOVE ALL "X" TO RENAME1.                                     NC2524.2
076700     IF NAME1 NOT EQUAL TO "XXXXX" GO TO RNM-FAIL-3.              NC2524.2
076800     IF NAME2 NOT EQUAL TO "XXXXXXXXXX" GO TO RNM-FAIL-3.         NC2524.2
076900     IF NAME3 NOT EQUAL TO "XXXXX" GO TO RNM-FAIL-3.              NC2524.2
077000     PERFORM PASS.                                                NC2524.2
077100     GO TO RNM-WRITE-3.                                           NC2524.2
077200 RNM-DELETE-3.                                                    NC2524.2
077300     PERFORM DE-LETE.                                             NC2524.2
077400     GO TO RNM-WRITE-3.                                           NC2524.2
077500 RNM-FAIL-3.                                                      NC2524.2
077600     MOVE RENAMES-DATA TO COMPUTED-A.                             NC2524.2
077700     MOVE "XXXXXXXXXXXXXXXXXXXX" TO CORRECT-A.                    NC2524.2
077800     PERFORM FAIL.                                                NC2524.2
077900 RNM-WRITE-3.                                                     NC2524.2
078000     MOVE "RENAM-TEST-3" TO PAR-NAME.                             NC2524.2
078100     PERFORM PRINT-DETAIL.                                        NC2524.2
078200 RENAM-TEST-4.                                                    NC2524.2
078300     IF RENAMES-TEST-1 EQUAL TO 123                               NC2524.2
078400         PERFORM PASS                                             NC2524.2
078500         GO TO RENAM-WRITE-4.                                     NC2524.2
078600     MOVE RENAMES-TEST-1 TO COMPUTED-A.                           NC2524.2
078700     MOVE 123 TO CORRECT-A.                                       NC2524.2
078800     PERFORM FAIL.                                                NC2524.2
078900     GO TO RENAM-WRITE-4.                                         NC2524.2
079000 RENAM-DELETE-4.                                                  NC2524.2
079100     PERFORM DE-LETE.                                             NC2524.2
079200 RENAM-WRITE-4.                                                   NC2524.2
079300     MOVE "RENAM-TEST-4" TO PAR-NAME.                             NC2524.2
079400     PERFORM PRINT-DETAIL.                                        NC2524.2
079500 RENAM-TEST-5.                                                    NC2524.2
079600     IF RENAMES-TEST-3 EQUAL TO "X  123"                          NC2524.2
079700         PERFORM PASS                                             NC2524.2
079800         GO TO RENAM-WRITE-5.                                     NC2524.2
079900     MOVE RENAMES-TEST-3 TO COMPUTED-A.                           NC2524.2
080000     MOVE "X  123" TO CORRECT-A.                                  NC2524.2
080100     PERFORM FAIL.                                                NC2524.2
080200     GO TO RENAM-WRITE-5.                                         NC2524.2
080300 RENAM-DELETE-5.                                                  NC2524.2
080400     PERFORM DE-LETE.                                             NC2524.2
080500 RENAM-WRITE-5.                                                   NC2524.2
080600     MOVE "RENAM-TEST-5" TO PAR-NAME.                             NC2524.2
080700     PERFORM PRINT-DETAIL.                                        NC2524.2
080800 RENAM-TEST-6.                                                    NC2524.2
080900     IF RENAMES-TEST-4 EQUAL TO "X  123"                          NC2524.2
081000         PERFORM PASS                                             NC2524.2
081100         GO TO RENAM-WRITE-6.                                     NC2524.2
081200     MOVE RENAMES-TEST-4 TO COMPUTED-A.                           NC2524.2
081300     MOVE "X  123" TO CORRECT-A.                                  NC2524.2
081400     PERFORM FAIL.                                                NC2524.2
081500     GO TO RENAM-WRITE-6.                                         NC2524.2
081600 RENAM-DELETE-6.                                                  NC2524.2
081700     PERFORM DE-LETE.                                             NC2524.2
081800 RENAM-WRITE-6.                                                   NC2524.2
081900     MOVE "RENAM-TEST-6" TO PAR-NAME.                             NC2524.2
082000     PERFORM PRINT-DETAIL.                                        NC2524.2
082100 RENAM-TEST-7.                                                    NC2524.2
082200     IF RENAMES-TEST-2 EQUAL TO "X  "                             NC2524.2
082300         PERFORM PASS                                             NC2524.2
082400         GO TO RENAM-WRITE-7.                                     NC2524.2
082500     MOVE RENAMES-TEST-2 TO COMPUTED-A.                           NC2524.2
082600     MOVE "X  " TO CORRECT-A.                                     NC2524.2
082700     PERFORM FAIL.                                                NC2524.2
082800     GO TO RENAM-WRITE-7.                                         NC2524.2
082900 RENAM-DELETE-7.                                                  NC2524.2
083000     PERFORM DE-LETE.                                             NC2524.2
083100 RENAM-WRITE-7.                                                   NC2524.2
083200     MOVE "RENAM-TEST-7" TO PAR-NAME.                             NC2524.2
083300     PERFORM PRINT-DETAIL.                                        NC2524.2
083400 RENAM-INIT-C.                                                    NC2524.2
083500     MOVE     "QUALIFIED RENAMES" TO FEATURE.                     NC2524.2
083600 RENAM-TEST-8.                                                    NC2524.2
083700     MOVE     "IOWA" TO TAG-1A.                                   NC2524.2
083800     MOVE     "OREGON" TO TAG-1B.                                 NC2524.2
083900     MOVE     "CALIFORNIA" TO RENAME-5 OF T-RENAMES-DATA.         NC2524.2
084000     IF       TAG-1 EQUAL TO "CALIFORNIA"                         NC2524.2
084100              PERFORM PASS GO TO RENAM-WRITE-8.                   NC2524.2
084200     GO       TO RENAM-FAIL-8.                                    NC2524.2
084300 RENAM-DELETE-8.                                                  NC2524.2
084400     PERFORM  DE-LETE.                                            NC2524.2
084500     GO       TO RENAM-WRITE-8.                                   NC2524.2
084600 RENAM-FAIL-8.                                                    NC2524.2
084700     PERFORM FAIL.                                                NC2524.2
084800     MOVE     TAG-1 TO COMPUTED-A.                                NC2524.2
084900     MOVE     "CALIFORNIA" TO CORRECT-A.                          NC2524.2
085000 RENAM-WRITE-8.                                                   NC2524.2
085100     MOVE     "RENAM-TEST-8 " TO PAR-NAME.                        NC2524.2
085200     PERFORM  PRINT-DETAIL.                                       NC2524.2
085300 RENAM-TEST-9.                                                    NC2524.2
085400     IF       UNIT-1 EQUAL TO "VERMONTOHIO"                       NC2524.2
085500              PERFORM PASS GO TO RENAM-WRITE-9.                   NC2524.2
085600*        NOTE THIS TEST FURTHER CHECKS THE RESULTS OF             NC2524.2
085700*             THE PREVIOUS TEST - THIS ITEM SHOULD BE UNCHANGED.  NC2524.2
085800     GO       TO RENAM-FAIL-9.                                    NC2524.2
085900 RENAM-DELETE-9.                                                  NC2524.2
086000     PERFORM  DE-LETE.                                            NC2524.2
086100     GO       TO RENAM-WRITE-9.                                   NC2524.2
086200 RENAM-FAIL-9.                                                    NC2524.2
086300     PERFORM FAIL.                                                NC2524.2
086400     MOVE     UNIT-1 TO COMPUTED-A.                               NC2524.2
086500     MOVE     "VERMONTOHIO" TO CORRECT-A.                         NC2524.2
086600 RENAM-WRITE-9.                                                   NC2524.2
086700     MOVE     "RENAM-TEST-9 " TO PAR-NAME.                        NC2524.2
086800     PERFORM  PRINT-DETAIL.                                       NC2524.2
086900 RENAM-TEST-10.                                                   NC2524.2
087000     MOVE     "IOWAOREGONFLORIDA" TO T-RENAMES-DATA.              NC2524.2
087100     IF       RENAME-6 IN T-RENAMES-DATA EQUAL TO                 NC2524.2
087200              "IOWAOREGONFLORIDA"                                 NC2524.2
087300              PERFORM PASS GO TO RENAM-WRITE-10.                  NC2524.2
087400     GO       TO RENAM-FAIL-10.                                   NC2524.2
087500 RENAM-DELETE-10.                                                 NC2524.2
087600     PERFORM  DE-LETE.                                            NC2524.2
087700     GO       TO RENAM-WRITE-10.                                  NC2524.2
087800 RENAM-FAIL-10.                                                   NC2524.2
087900     PERFORM FAIL.                                                NC2524.2
088000     MOVE    RENAME-6 IN T-RENAMES-DATA TO COMPUTED-A.            NC2524.2
088100     MOVE   "IOWAOREGONFLORIDA" TO CORRECT-A.                     NC2524.2
088200 RENAM-WRITE-10.                                                  NC2524.2
088300     MOVE   "RENAM-TEST-10" TO PAR-NAME.                          NC2524.2
088400     PERFORM PRINT-DETAIL.                                        NC2524.2
088500 RENAM-TEST-11.                                                   NC2524.2
088600     MOVE   "BOSTO" TO ITEM-1.                                    NC2524.2
088700     MOVE   "N M" TO TABLE-ITEM-2 (1).                            NC2524.2
088800     MOVE   "ASS" TO TABLE-ITEM-2 (2).                            NC2524.2
088900     MOVE   "ACH" TO TABLE-ITEM-2 (3).                            NC2524.2
089000     MOVE   "USE" TO TABLE-ITEM-2 (4).                            NC2524.2
089100     MOVE   "TTS" TO TABLE-ITEM-2 (5).                            NC2524.2
089200     IF      RENAME-7 EQUAL TO "BOSTON MASSACHUSETTS"             NC2524.2
089300             PERFORM PASS GO TO RENAM-WRITE-11.                   NC2524.2
089400     GO      TO RENAM-FAIL-11.                                    NC2524.2
089500 RENAM-DELETE-11.                                                 NC2524.2
089600     PERFORM  DE-LETE.                                            NC2524.2
089700     GO       TO RENAM-WRITE-11.                                  NC2524.2
089800 RENAM-FAIL-11.                                                   NC2524.2
089900     PERFORM FAIL.                                                NC2524.2
090000     MOVE     RENAME-7 TO COMPUTED-A.                             NC2524.2
090100     MOVE     "BOSTON MASSACHUSETTS" TO CORRECT-A.                NC2524.2
090200 RENAM-WRITE-11.                                                  NC2524.2
090300     MOVE     "RENAMES A TABLE" TO FEATURE.                       NC2524.2
090400     MOVE     "RENAM-TEST-11" TO PAR-NAME.                        NC2524.2
090500     PERFORM  PRINT-DETAIL.                                       NC2524.2
090600 RENAM-INIT-D.                                                    NC2524.2
090700     MOVE     "RENAMED DATA ---" TO FEATURE.                      NC2524.2
090800     PERFORM  PRINT-DETAIL.                                       NC2524.2
090900 RENAM-TEST-12.                                                   NC2524.2
091000     MOVE     SPACE TO W-RENAMES-DATA.                            NC2524.2
091100     MOVE     12.3 TO WIDGET-1.                                   NC2524.2
091200     MOVE     45678.9 TO WIDGET-2.                                NC2524.2
091300     MOVE     ZERO TO WIDGET-3.                                   NC2524.2
091400     IF       RENAME-8 EQUAL TO "123*45,678.900000"               NC2524.2
091500              PERFORM PASS GO TO RENAM-WRITE-12.                  NC2524.2
091600     GO       TO RENAM-FAIL-12.                                   NC2524.2
091700 RENAM-DELETE-12.                                                 NC2524.2
091800     PERFORM  DE-LETE.                                            NC2524.2
091900     GO       TO RENAM-WRITE-12.                                  NC2524.2
092000 RENAM-FAIL-12.                                                   NC2524.2
092100     PERFORM FAIL.                                                NC2524.2
092200     MOVE     RENAME-8 TO COMPUTED-A.                             NC2524.2
092300     MOVE     "123*45,678.900000" TO CORRECT-A.                   NC2524.2
092400 RENAM-WRITE-12.                                                  NC2524.2
092500     MOVE     "  GROUP COMPARISON" TO FEATURE                     NC2524.2
092600     MOVE     "RENAM-TEST-12" TO PAR-NAME.                        NC2524.2
092700     PERFORM  PRINT-DETAIL.                                       NC2524.2
092800 RENAM-TEST-13.                                                   NC2524.2
092900     MOVE     SPACE TO W-RENAMES-DATA.                            NC2524.2
093000     MOVE     "123456789" TO RENAME-10.                           NC2524.2
093100     IF       RENAME-9 EQUAL TO "    12345678"                    NC2524.2
093200              PERFORM PASS GO TO RENAM-WRITE-13.                  NC2524.2
093300     GO       TO RENAM-FAIL-13.                                   NC2524.2
093400 RENAM-DELETE-13.                                                 NC2524.2
093500     PERFORM  DE-LETE.                                            NC2524.2
093600     GO       TO RENAM-WRITE-13.                                  NC2524.2
093700 RENAM-FAIL-13.                                                   NC2524.2
093800     PERFORM FAIL.                                                NC2524.2
093900     MOVE     RENAME-9 TO COMPUTED-A                              NC2524.2
094000     MOVE     "    12345678" TO CORRECT-A.                        NC2524.2
094100 RENAM-WRITE-13.                                                  NC2524.2
094200     MOVE     "  GRP MOVE, COMPARE" TO FEATURE.                   NC2524.2
094300     MOVE     "RENAM-TEST-13" TO PAR-NAME.                        NC2524.2
094400     PERFORM  PRINT-DETAIL.                                       NC2524.2
094500 RENAM-TEST-14.                                                   NC2524.2
094600     MOVE     SPACE TO W-RENAMES-DATA.                            NC2524.2
094700     MOVE     123456 TO RENAME-10                                 NC2524.2
094800     IF       WIDGET-4 EQUAL TO 1234                              NC2524.2
094900              PERFORM PASS GO TO RENAM-WRITE-14.                  NC2524.2
095000     GO       TO RENAM-FAIL-14.                                   NC2524.2
095100 RENAM-DELETE-14.                                                 NC2524.2
095200     PERFORM  DE-LETE.                                            NC2524.2
095300     GO       TO RENAM-WRITE-14.                                  NC2524.2
095400 RENAM-FAIL-14.                                                   NC2524.2
095500     PERFORM FAIL.                                                NC2524.2
095600     MOVE     WIDGET-4 TO COMPUTED-N.                             NC2524.2
095700     MOVE     1234 TO CORRECT-N.                                  NC2524.2
095800 RENAM-WRITE-14.                                                  NC2524.2
095900     MOVE     "  GROUP MOVE" TO FEATURE.                          NC2524.2
096000     MOVE     "RENAM-TEST-14" TO PAR-NAME.                        NC2524.2
096100     PERFORM  PRINT-DETAIL.                                       NC2524.2
096200 RENAM-TEST-15.                                                   NC2524.2
096300     MOVE     SPACE TO W-RENAMES-DATA.                            NC2524.2
096400     MOVE     234.5 TO RENAME-11.                                 NC2524.2
096500     IF       WIDGET-2 EQUAL TO "****234.50"                      NC2524.2
096600              PERFORM PASS GO TO RENAM-WRITE-15.                  NC2524.2
096700     GO       TO RENAM-FAIL-15.                                   NC2524.2
096800 RENAM-DELETE-15.                                                 NC2524.2
096900     PERFORM  DE-LETE.                                            NC2524.2
097000     GO       TO RENAM-WRITE-15.                                  NC2524.2
097100 RENAM-FAIL-15.                                                   NC2524.2
097200     PERFORM FAIL.                                                NC2524.2
097300     MOVE     WIDGET-2 TO COMPUTED-A.                             NC2524.2
097400     MOVE     "****234.50" TO CORRECT-A.                          NC2524.2
097500 RENAM-WRITE-15.                                                  NC2524.2
097600     MOVE     "  EDITED MOVE" TO FEATURE.                         NC2524.2
097700     MOVE     "RENAM-TEST-15" TO PAR-NAME.                        NC2524.2
097800     PERFORM  PRINT-DETAIL.                                       NC2524.2
097900 RENAM-INIT-E.                                                    NC2524.2
098000     MOVE     "  ADD, SIZE ERROR" TO FEATURE.                     NC2524.2
098100*        NOTE THE NEXT TWO TESTS ARE INTERRELATED.                NC2524.2
098200 RENAM-TEST-16.                                                   NC2524.2
098300     MOVE     8000 TO WIDGET-4.                                   NC2524.2
098400     ADD      3500 TO RENAME-12 ON SIZE ERROR                     NC2524.2
098500              PERFORM PASS GO TO RENAM-WRITE-16.                  NC2524.2
098600     GO       TO RENAM-FAIL-16.                                   NC2524.2
098700 RENAM-DELETE-16.                                                 NC2524.2
098800     PERFORM  DE-LETE.                                            NC2524.2
098900     GO       TO RENAM-WRITE-16.                                  NC2524.2
099000 RENAM-FAIL-16.                                                   NC2524.2
099100     PERFORM FAIL.                                                NC2524.2
099200     MOVE     "SIZE ERROR DID NOT OCCUR" TO RE-MARK.              NC2524.2
099300 RENAM-WRITE-16.                                                  NC2524.2
099400     MOVE     "RENAM-TEST-16" TO PAR-NAME.                        NC2524.2
099500     PERFORM  PRINT-DETAIL.                                       NC2524.2
099600 RENAM-TEST-17.                                                   NC2524.2
099700     IF       RENAME-12 EQUAL TO 8000                             NC2524.2
099800              PERFORM PASS GO TO RENAM-WRITE-17.                  NC2524.2
099900     GO       TO RENAM-FAIL-17.                                   NC2524.2
100000 RENAM-DELETE-17.                                                 NC2524.2
100100     PERFORM  DE-LETE.                                            NC2524.2
100200     GO       TO RENAM-WRITE-17.                                  NC2524.2
100300 RENAM-FAIL-17.                                                   NC2524.2
100400     PERFORM FAIL.                                                NC2524.2
100500     MOVE     RENAME-12 TO COMPUTED-N.                            NC2524.2
100600     MOVE     8000 TO CORRECT-N.                                  NC2524.2
100700 RENAM-WRITE-17.                                                  NC2524.2
100800     MOVE     "RENAM-TEST-17" TO PAR-NAME.                        NC2524.2
100900     PERFORM  PRINT-DETAIL.                                       NC2524.2
101000 RENAM-TEST-18.                                                   NC2524.2
101100     MOVE     SPACE TO U-RENAMES-DATA.                            NC2524.2
101200     MOVE     "CHICAGO ILLINOIS" TO RENAME-5 OF U-RENAMES-DATA.   NC2524.2
101300     IF       U-RENAMES-DATA EQUAL TO "CHICAGO ILL     "          NC2524.2
101400              PERFORM PASS GO TO RENAM-WRITE-18.                  NC2524.2
101500     PERFORM  FAIL.                                               NC2524.2
101600     GO       TO RENAM-FAIL-18.                                   NC2524.2
101700 RENAM-DELETE-18.                                                 NC2524.2
101800     PERFORM  DE-LETE.                                            NC2524.2
101900     GO       TO RENAM-WRITE-18.                                  NC2524.2
102000 RENAM-FAIL-18.                                                   NC2524.2
102100     PERFORM  FAIL.                                               NC2524.2
102200     MOVE     U-RENAMES-DATA TO COMPUTED-A.                       NC2524.2
102300     MOVE     "CHICAGO ILL     " TO CORRECT-A.                    NC2524.2
102400 RENAM-WRITE-18.                                                  NC2524.2
102500     MOVE     "  THROUGH" TO FEATURE.                             NC2524.2
102600     MOVE     "RENAM-TEST-18" TO PAR-NAME.                        NC2524.2
102700     PERFORM  PRINT-DETAIL.                                       NC2524.2
102800*                                                                 NC2524.2
102900 COMPUTING SECTION.                                               NC2524.2
103000 COMPUTE-INIT.                                                    NC2524.2
103100     MOVE SPACES TO TEST-RESULTS.                                 NC2524.2
103200     PERFORM END-ROUTINE.                                         NC2524.2
103300     MOVE "THE COMPUTED RESULT FOR THE FOLLOWING TESTS"           NC2524.2
103400             TO RE-MARK.                                          NC2524.2
103500     PERFORM PRINT-DETAIL.                                        NC2524.2
103600     MOVE "IS ALLOWED TO DEVIATE FROM THE INDICATED"              NC2524.2
103700             TO RE-MARK.                                          NC2524.2
103800     PERFORM PRINT-DETAIL.                                        NC2524.2
103900     MOVE "CORRECT RESULT BY" TO RE-MARK.                         NC2524.2
104000     PERFORM PRINT-DETAIL.                                        NC2524.2
104100     MOVE "+ OR - (CORRECT RESULT * (.2 ** 5))."                  NC2524.2
104200             TO RE-MARK.                                          NC2524.2
104300     PERFORM PRINT-DETAIL.                                        NC2524.2
104400     MOVE "COMPUTE        " TO FEATURE.                           NC2524.2
104500 COMP-TEST-1.                                                     NC2524.2
104600     COMPUTE COMPUTE-1 = COMPUTE-1A.                              NC2524.2
104700     IF ( COMPUTE-1 < 654.20038) AND                              NC2524.2
104800        ( COMPUTE-1 > 654.17422) THEN                             NC2524.2
104900              PERFORM PASS                                        NC2524.2
105000              GO TO COMP-WRITE-1.                                 NC2524.2
105100     PERFORM FAIL.                                                NC2524.2
105200     MOVE COMPUTE-1 TO COMPUTED-N.                                NC2524.2
105300     MOVE "+654.1873" TO CORRECT-A.                               NC2524.2
105400     GO TO COMP-WRITE-1.                                          NC2524.2
105500 COMP-DELETE-1.                                                   NC2524.2
105600     PERFORM DE-LETE.                                             NC2524.2
105700 COMP-WRITE-1.                                                    NC2524.2
105800     MOVE "COMP-TEST-1" TO PAR-NAME.                              NC2524.2
105900     PERFORM PRINT-DETAIL.                                        NC2524.2
106000 COMP-TEST-2.                                                     NC2524.2
106100     COMPUTE COMPUTE-2 = 2233.9                                   NC2524.2
106200     IF ( COMPUTE-2 < 2233.94468) AND                             NC2524.2
106300         ( COMPUTE-2 > 2233.85532) THEN                           NC2524.2
106400              PERFORM PASS                                        NC2524.2
106500              GO TO COMP-WRITE-2.                                 NC2524.2
106600     PERFORM FAIL.                                                NC2524.2
106700     MOVE COMPUTE-2 TO COMPUTED-N.                                NC2524.2
106800     MOVE "+2233.9" TO CORRECT-A.                                 NC2524.2
106900     GO TO COMP-WRITE-2.                                          NC2524.2
107000 COMP-DELETE-2.                                                   NC2524.2
107100     PERFORM DE-LETE.                                             NC2524.2
107200 COMP-WRITE-2.                                                    NC2524.2
107300     MOVE "COMP-TEST-2" TO PAR-NAME.                              NC2524.2
107400     PERFORM PRINT-DETAIL.                                        NC2524.2
107500 COMP-TEST-3.                                                     NC2524.2
107600     COMPUTE COMPUTE-3 = COMPUTE-3A - COMPUTE-3B.                 NC2524.2
107700     IF ( COMPUTE-3 NOT < 52.39105) AND                           NC2524.2
107800        ( COMPUTE-3 NOT > 52.38895) THEN                          NC2524.2
107900              PERFORM FAIL                                        NC2524.2
108000               MOVE COMPUTE-3 TO COMPUTED-N                       NC2524.2
108100              MOVE "+52.39" TO CORRECT-A                          NC2524.2
108200              GO TO COMP-WRITE-3.                                 NC2524.2
108300     PERFORM PASS.                                                NC2524.2
108400     GO TO COMP-WRITE-3.                                          NC2524.2
108500 COMP-DELETE-3.                                                   NC2524.2
108600     PERFORM DE-LETE.                                             NC2524.2
108700 COMP-WRITE-3.                                                    NC2524.2
108800     MOVE "COMP-TEST-3" TO PAR-NAME.                              NC2524.2
108900     PERFORM PRINT-DETAIL.                                        NC2524.2
109000 COMP-TEST-4.                                                     NC2524.2
109100     COMPUTE COMPUTE-4 = COMPUTE-4A + COMPUTE-4B.                 NC2524.2
109200        IF COMPUTE-4 NOT = 341                                    NC2524.2
109300              PERFORM FAIL                                        NC2524.2
109400              MOVE COMPUTE-4 TO COMPUTED-N                        NC2524.2
109500              MOVE "+341" TO CORRECT-A                            NC2524.2
109600              GO TO COMP-WRITE-4.                                 NC2524.2
109700     PERFORM PASS.                                                NC2524.2
109800     GO TO COMP-WRITE-4.                                          NC2524.2
109900 COMP-DELETE-4.                                                   NC2524.2
110000     PERFORM DE-LETE.                                             NC2524.2
110100 COMP-WRITE-4.                                                    NC2524.2
110200     MOVE "COMP-TEST-4" TO PAR-NAME.                              NC2524.2
110300     PERFORM PRINT-DETAIL.                                        NC2524.2
110400 COMP-TEST-5.                                                     NC2524.2
110500     COMPUTE COMPUTE-5 = COMPUTE-5A * 36.1                        NC2524.2
110600     IF ( COMPUTE-5 > 400.71801) OR                               NC2524.2
110700        ( COMPUTE-5 < 400.70199) THEN                             NC2524.2
110800              PERFORM FAIL                                        NC2524.2
110900              MOVE COMPUTE-5 TO COMPUTED-N                        NC2524.2
111000              MOVE "+400.71" TO CORRECT-A                         NC2524.2
111100              GO TO COMP-WRITE-5.                                 NC2524.2
111200     PERFORM PASS.                                                NC2524.2
111300     GO TO COMP-WRITE-5.                                          NC2524.2
111400 COMP-DELETE-5.                                                   NC2524.2
111500     PERFORM DE-LETE.                                             NC2524.2
111600 COMP-WRITE-5.                                                    NC2524.2
111700     MOVE "COMP-TEST-5" TO PAR-NAME.                              NC2524.2
111800     PERFORM PRINT-DETAIL.                                        NC2524.2
111900 COMP-TEST-6.                                                     NC2524.2
112000     COMPUTE COMPUTE-6 = COMPUTE-6A / 6.0                         NC2524.2
112100     IF ( COMPUTE-6 > 62.40125) OR                                NC2524.2
112200        ( COMPUTE-6 < 62.39875) THEN                              NC2524.2
112300              PERFORM FAIL                                        NC2524.2
112400              MOVE COMPUTE-6 TO COMPUTED-N                        NC2524.2
112500              MOVE "+062.40" TO CORRECT-A                         NC2524.2
112600              GO TO COMP-WRITE-6.                                 NC2524.2
112700     PERFORM PASS.                                                NC2524.2
112800     GO TO COMP-WRITE-6.                                          NC2524.2
112900 COMP-DELETE-6.                                                   NC2524.2
113000     PERFORM DE-LETE.                                             NC2524.2
113100 COMP-WRITE-6.                                                    NC2524.2
113200     MOVE "COMP-TEST-6" TO PAR-NAME.                              NC2524.2
113300     PERFORM PRINT-DETAIL.                                        NC2524.2
113400 COMP-TEST-7.                                                     NC2524.2
113500     COMPUTE COMPUTE-7 = 2.0 ** 4.                                NC2524.2
113600        IF COMPUTE-7 = 16                                         NC2524.2
113700              PERFORM PASS                                        NC2524.2
113800              GO TO COMP-WRITE-7.                                 NC2524.2
113900     PERFORM FAIL.                                                NC2524.2
114000     MOVE COMPUTE-7 TO COMPUTED-N.                                NC2524.2
114100     MOVE "+16" TO CORRECT-A.                                     NC2524.2
114200     GO TO COMP-WRITE-7.                                          NC2524.2
114300 COMP-DELETE-7.                                                   NC2524.2
114400     PERFORM DE-LETE.                                             NC2524.2
114500 COMP-WRITE-7.                                                    NC2524.2
114600     MOVE "COMP-TEST-7" TO PAR-NAME.                              NC2524.2
114700     PERFORM PRINT-DETAIL.                                        NC2524.2
114800 COMP-TEST-8.                                                     NC2524.2
114900     COMPUTE COMPUTE-8 = (((24.0 + 1) * (60 - 10)) / 125) ** 2.   NC2524.2
115000             IF COMPUTE-8 = 100                                   NC2524.2
115100          PERFORM PASS                                            NC2524.2
115200          GO TO COMP-WRITE-8.                                     NC2524.2
115300     PERFORM FAIL.                                                NC2524.2
115400     MOVE COMPUTE-8 TO COMPUTED-N.                                NC2524.2
115500     MOVE "+100" TO CORRECT-A.                                    NC2524.2
115600     GO TO COMP-WRITE-8.                                          NC2524.2
115700 COMP-DELETE-8.                                                   NC2524.2
115800     PERFORM DE-LETE.                                             NC2524.2
115900 COMP-WRITE-8.                                                    NC2524.2
116000     MOVE "COMP-TEST-8" TO PAR-NAME.                              NC2524.2
116100     PERFORM PRINT-DETAIL.                                        NC2524.2
116200 COMP-TEST-9.                                                     NC2524.2
116300     COMPUTE COMPUTE-9 ROUNDED = COMPUTE-6A * 7.0                 NC2524.2
116400     IF (COMPUTE-9 > 2621.05242) OR                               NC2524.2
116500        (COMPUTE-9 < 2620.94758) THEN                             NC2524.2
116600              PERFORM FAIL                                        NC2524.2
116700              MOVE COMPUTE-9 TO COMPUTED-N                        NC2524.2
116800              MOVE "+2621" TO CORRECT-A                           NC2524.2
116900              GO TO COMP-WRITE-9.                                 NC2524.2
117000     PERFORM PASS.                                                NC2524.2
117100     GO TO COMP-WRITE-9.                                          NC2524.2
117200 COMP-DELETE-9.                                                   NC2524.2
117300     PERFORM DE-LETE.                                             NC2524.2
117400 COMP-WRITE-9.                                                    NC2524.2
117500     MOVE "COMP-TEST-9" TO PAR-NAME.                              NC2524.2
117600     PERFORM PRINT-DETAIL.                                        NC2524.2
117700 COMP-TEST-10.                                                    NC2524.2
117800     COMPUTE COMPUTE-10 = COMPUTE-1A + COMPUTE-6A ON SIZE ERROR   NC2524.2
117900     MOVE "R" TO XRAY.                                            NC2524.2
118000     IF XRAY EQUAL TO "R"                                         NC2524.2
118100              PERFORM PASS                                        NC2524.2
118200              GO TO COMP-WRITE-10.                                NC2524.2
118300     PERFORM FAIL.                                                NC2524.2
118400     MOVE "OSE NOT EXECUTED" TO RE-MARK.                          NC2524.2
118500     GO TO COMP-WRITE-10.                                         NC2524.2
118600 COMP-DELETE-10.                                                  NC2524.2
118700     PERFORM DE-LETE.                                             NC2524.2
118800 COMP-WRITE-10.                                                   NC2524.2
118900     MOVE "COMP-TEST-10" TO PAR-NAME.                             NC2524.2
119000     PERFORM PRINT-DETAIL.                                        NC2524.2
119100 COMP-TEST-11.                                                    NC2524.2
119200     IF (COMPUTE-10 > 0.00002) OR                                 NC2524.2
119300        (COMPUTE-10 < -0.00002)                                   NC2524.2
119400              PERFORM FAIL                                        NC2524.2
119500              MOVE COMPUTE-10 TO COMPUTED-N                       NC2524.2
119600              MOVE ZERO TO CORRECT-N                              NC2524.2
119700              GO TO COMP-WRITE-11.                                NC2524.2
119800     PERFORM PASS.                                                NC2524.2
119900     GO TO COMP-WRITE-11.                                         NC2524.2
120000 COMP-DELETE-11.                                                  NC2524.2
120100     PERFORM DE-LETE.                                             NC2524.2
120200 COMP-WRITE-11.                                                   NC2524.2
120300     MOVE "COMP-TEST-11" TO PAR-NAME.                             NC2524.2
120400     PERFORM PRINT-DETAIL.                                        NC2524.2
120500 COMP-TEST-12.                                                    NC2524.2
120600     COMPUTE COMPUTE-11 = COMPUTE-11A + COMPUTE-11B - 121.6       NC2524.2
120700     IF ( COMPUTE-11 < 718.51437) AND                             NC2524.2
120800        ( COMPUTE-11 > 718.48563) THEN                            NC2524.2
120900              PERFORM PASS                                        NC2524.2
121000              GO TO COMP-WRITE-12.                                NC2524.2
121100     PERFORM FAIL.                                                NC2524.2
121200     MOVE COMPUTE-11 TO COMPUTED-N.                               NC2524.2
121300     MOVE "+718.5" TO CORRECT-A.                                  NC2524.2
121400     GO TO COMP-WRITE-12.                                         NC2524.2
121500 COMP-DELETE-12.                                                  NC2524.2
121600     PERFORM DE-LETE.                                             NC2524.2
121700 COMP-WRITE-12.                                                   NC2524.2
121800     MOVE "COMP-TEST-12" TO PAR-NAME.                             NC2524.2
121900     PERFORM PRINT-DETAIL.                                        NC2524.2
122000 COMP-TEST-13.                                                    NC2524.2
122100     COMPUTE COMPUTE-12 = COMPUTE-12A * 5.1 / 281.7.              NC2524.2
122200     IF (COMPUTE-12 < 6.09012) AND                                NC2524.2
122300        (COMPUTE-12 > 6.08988) THEN                               NC2524.2
122400              PERFORM PASS                                        NC2524.2
122500              GO TO COMP-WRITE-13.                                NC2524.2
122600     PERFORM FAIL.                                                NC2524.2
122700     MOVE COMPUTE-12 TO COMPUTED-N.                               NC2524.2
122800     MOVE "+6.09" TO CORRECT-A.                                   NC2524.2
122900     GO TO COMP-WRITE-13.                                         NC2524.2
123000 COMP-DELETE-13.                                                  NC2524.2
123100     PERFORM DE-LETE.                                             NC2524.2
123200 COMP-WRITE-13.                                                   NC2524.2
123300     MOVE "COMP-TEST-13" TO PAR-NAME.                             NC2524.2
123400     PERFORM PRINT-DETAIL.                                        NC2524.2
123500 COMPUTE-ROUTINE SECTION.                                         NC2524.2
123600 COMPUTE-TEST.                                                    NC2524.2
123700     MOVE "COMPUTE" TO FEATURE.                                   NC2524.2
123800     MOVE ZERO TO W-1.                                            NC2524.2
123900     MOVE ZERO TO W-2.                                            NC2524.2
124000 COMP-TEST-14.                                                    NC2524.2
124100     COMPUTE W-1 = NINE.                                          NC2524.2
124200        IF W-1 = 9                                                NC2524.2
124300              PERFORM PASS                                        NC2524.2
124400              GO TO COMP-WRITE-14.                                NC2524.2
124500     PERFORM FAIL.                                                NC2524.2
124600     MOVE W-1 TO COMPUTED-A.                                      NC2524.2
124700     MOVE 9 TO W-1.                                               NC2524.2
124800     MOVE 9 TO CORRECT-A.                                         NC2524.2
124900     GO TO COMP-WRITE-14.                                         NC2524.2
125000 COMP-DELETE-14.                                                  NC2524.2
125100     PERFORM DE-LETE.                                             NC2524.2
125200 COMP-WRITE-14.                                                   NC2524.2
125300     MOVE "COMP-TEST-14" TO PAR-NAME.                             NC2524.2
125400     PERFORM PRINT-DETAIL.                                        NC2524.2
125500 COMP-TEST-15.                                                    NC2524.2
125600     COMPUTE W-2 = W-1 + 20.                                      NC2524.2
125700        IF W-2 = 29                                               NC2524.2
125800              PERFORM PASS                                        NC2524.2
125900              GO TO COMP-WRITE-15.                                NC2524.2
126000     PERFORM FAIL.                                                NC2524.2
126100     MOVE W-2 TO COMPUTED-N.                                      NC2524.2
126200     MOVE "+29" TO CORRECT-A.                                     NC2524.2
126300     MOVE 29 TO W-2.                                              NC2524.2
126400     GO TO COMP-WRITE-15.                                         NC2524.2
126500 COMP-DELETE-15.                                                  NC2524.2
126600     PERFORM DE-LETE.                                             NC2524.2
126700 COMP-WRITE-15.                                                   NC2524.2
126800     MOVE "COMP-TEST-15" TO PAR-NAME.                             NC2524.2
126900     PERFORM PRINT-DETAIL.                                        NC2524.2
127000 COMP-TEST-16.                                                    NC2524.2
127100     MOVE ZERO TO W-11.                                           NC2524.2
127200     COMPUTE W-11 = W-1 - W-2.                                    NC2524.2
127300     IF ( W-11 > -20.00040) AND                                   NC2524.2
127400        ( W-11 < -19.99960) THEN                                  NC2524.2
127500              PERFORM PASS                                        NC2524.2
127600              GO TO COMP-WRITE-16.                                NC2524.2
127700     PERFORM FAIL.                                                NC2524.2
127800     MOVE W-11 TO COMPUTED-N.                                     NC2524.2
127900     MOVE "-20" TO CORRECT-A.                                     NC2524.2
128000     GO TO COMP-WRITE-16.                                         NC2524.2
128100 COMP-DELETE-16.                                                  NC2524.2
128200     PERFORM DE-LETE.                                             NC2524.2
128300 COMP-WRITE-16.                                                   NC2524.2
128400     MOVE "COMP-TEST-16" TO PAR-NAME.                             NC2524.2
128500     PERFORM PRINT-DETAIL.                                        NC2524.2
128600 COMP-TEST-17.                                                    NC2524.2
128700     MOVE ZERO TO W-3.                                            NC2524.2
128800     COMPUTE W-3 = TEN * 30.                                      NC2524.2
128900        IF W-3 = 300                                              NC2524.2
129000              PERFORM PASS                                        NC2524.2
129100              GO TO COMP-WRITE-17.                                NC2524.2
129200     PERFORM FAIL.                                                NC2524.2
129300     MOVE W-3 TO COMPUTED-N.                                      NC2524.2
129400     MOVE "+300" TO CORRECT-A.                                    NC2524.2
129500     GO TO COMP-WRITE-17.                                         NC2524.2
129600 COMP-DELETE-17.                                                  NC2524.2
129700     PERFORM DE-LETE.                                             NC2524.2
129800 COMP-WRITE-17.                                                   NC2524.2
129900     MOVE "COMP-TEST-17" TO PAR-NAME.                             NC2524.2
130000     PERFORM PRINT-DETAIL.                                        NC2524.2
130100 COMP-TEST-18.                                                    NC2524.2
130200     MOVE ZERO TO W-5.                                            NC2524.2
130300     COMPUTE W-5 = 42 / SEVEN.                                    NC2524.2
130400        IF W-5 = 6                                                NC2524.2
130500              PERFORM PASS                                        NC2524.2
130600              GO TO COMP-WRITE-18.                                NC2524.2
130700     PERFORM FAIL.                                                NC2524.2
130800     MOVE W-5 TO COMPUTED-N.                                      NC2524.2
130900     MOVE "+6" TO CORRECT-A.                                      NC2524.2
131000     GO TO COMP-WRITE-18.                                         NC2524.2
131100 COMP-DELETE-18.                                                  NC2524.2
131200     PERFORM DE-LETE.                                             NC2524.2
131300 COMP-WRITE-18.                                                   NC2524.2
131400     MOVE "COMP-TEST-18" TO PAR-NAME.                             NC2524.2
131500     PERFORM PRINT-DETAIL.                                        NC2524.2
131600 COMP-TEST-19.                                                    NC2524.2
131700     MOVE ZERO TO W-2.                                            NC2524.2
131800     COMPUTE W-2 = FOUR ** 3.                                     NC2524.2
131900        IF W-2 = 64                                               NC2524.2
132000              PERFORM PASS                                        NC2524.2
132100              GO TO COMP-WRITE-19.                                NC2524.2
132200     PERFORM FAIL.                                                NC2524.2
132300     MOVE W-2 TO COMPUTED-N.                                      NC2524.2
132400     MOVE "+64" TO CORRECT-A.                                     NC2524.2
132500     GO TO COMP-WRITE-19.                                         NC2524.2
132600 COMP-DELETE-19.                                                  NC2524.2
132700     PERFORM DE-LETE.                                             NC2524.2
132800 COMP-WRITE-19.                                                   NC2524.2
132900     MOVE "COMP-TEST-19" TO PAR-NAME.                             NC2524.2
133000     PERFORM PRINT-DETAIL.                                        NC2524.2
133100 COMP-TEST-20.                                                    NC2524.2
133200     MOVE 555 TO W-3.                                             NC2524.2
133300     COMPUTE W-3 = TWENTY-5 + 101 + 222.                          NC2524.2
133400        IF W-3 = 348                                              NC2524.2
133500              PERFORM PASS                                        NC2524.2
133600              GO TO COMP-WRITE-20.                                NC2524.2
133700              PERFORM FAIL.                                       NC2524.2
133800              MOVE W-3 TO COMPUTED-N.                             NC2524.2
133900              MOVE "+348" TO CORRECT-A.                           NC2524.2
134000     GO TO COMP-WRITE-20.                                         NC2524.2
134100 COMP-DELETE-20.                                                  NC2524.2
134200     PERFORM DE-LETE.                                             NC2524.2
134300 COMP-WRITE-20.                                                   NC2524.2
134400     MOVE "COMP-TEST-20" TO PAR-NAME.                             NC2524.2
134500     PERFORM PRINT-DETAIL.                                        NC2524.2
134600 COMP-TEST-21.                                                    NC2524.2
134700     MOVE ZERO TO W-9.                                            NC2524.2
134800     COMPUTE W-9 = TWO * (3 + 4).                                 NC2524.2
134900        IF W-9 = 14                                               NC2524.2
135000              PERFORM PASS                                        NC2524.2
135100              GO TO COMP-WRITE-21.                                NC2524.2
135200     PERFORM FAIL.                                                NC2524.2
135300     MOVE W-9 TO COMPUTED-N.                                      NC2524.2
135400     MOVE "+14" TO CORRECT-A.                                     NC2524.2
135500     GO TO COMP-WRITE-21.                                         NC2524.2
135600 COMP-DELETE-21.                                                  NC2524.2
135700     PERFORM DE-LETE.                                             NC2524.2
135800 COMP-WRITE-21.                                                   NC2524.2
135900     MOVE "COMP-TEST-21" TO PAR-NAME.                             NC2524.2
136000     PERFORM PRINT-DETAIL.                                        NC2524.2
136100  COMP-TEST-22.                                                   NC2524.2
136200     MOVE ZERO TO W-9.                                            NC2524.2
136300     COMPUTE W-9 = (TWO + (3 * FOUR) / (2 * THREE)) ** 2 - 1.     NC2524.2
136400             IF W-9 = 15     PERFORM PASS                         NC2524.2
136500         GO TO COMP-WRITE-22.                                     NC2524.2
136600     PERFORM FAIL.                                                NC2524.2
136700     MOVE W-9 TO COMPUTED-N.                                      NC2524.2
136800     MOVE "+15" TO CORRECT-A.                                     NC2524.2
136900     GO TO COMP-WRITE-22.                                         NC2524.2
137000 COMP-DELETE-22.                                                  NC2524.2
137100     PERFORM DE-LETE.                                             NC2524.2
137200 COMP-WRITE-22.                                                   NC2524.2
137300     MOVE "COMP-TEST-22" TO PAR-NAME.                             NC2524.2
137400     PERFORM PRINT-DETAIL.                                        NC2524.2
137500 COMP-TEST-23.                                                    NC2524.2
137600     MOVE ZERO TO XRAY.                                           NC2524.2
137700     MOVE 10 TO W-2.                                              NC2524.2
137800     COMPUTE W-2 = 96 + TWENTY ON SIZE ERROR                      NC2524.2
137900     MOVE 8 TO XRAY.                                              NC2524.2
138000     IF XRAY IS EQUAL TO "8"                                      NC2524.2
138100              PERFORM PASS                                        NC2524.2
138200              GO TO COMP-WRITE-23.                                NC2524.2
138300     PERFORM FAIL.                                                NC2524.2
138400     MOVE "8" TO CORRECT-A.                                       NC2524.2
138500     MOVE XRAY TO COMPUTED-A.                                     NC2524.2
138600     MOVE "OSE NOT EXECUTED" TO RE-MARK.                          NC2524.2
138700     GO TO COMP-WRITE-23.                                         NC2524.2
138800 COMP-DELETE-23.                                                  NC2524.2
138900     PERFORM DE-LETE.                                             NC2524.2
139000 COMP-WRITE-23.                                                   NC2524.2
139100     MOVE "COMP-TEST-23" TO PAR-NAME.                             NC2524.2
139200     PERFORM PRINT-DETAIL.                                        NC2524.2
139300 COMP-TEST-24.                                                    NC2524.2
139400        IF W-2 = 10                                               NC2524.2
139500              PERFORM PASS                                        NC2524.2
139600              GO TO COMP-WRITE-24.                                NC2524.2
139700     PERFORM FAIL.                                                NC2524.2
139800     MOVE W-2 TO COMPUTED-A.                                      NC2524.2
139900     MOVE "10" TO CORRECT-A.                                      NC2524.2
140000     MOVE "NOT PROTECTED BY OES" TO RE-MARK.                      NC2524.2
140100     GO TO COMP-WRITE-24.                                         NC2524.2
140200 COMP-DELETE-24.                                                  NC2524.2
140300     PERFORM DE-LETE.                                             NC2524.2
140400 COMP-WRITE-24.                                                   NC2524.2
140500     MOVE "COMP-TEST-24" TO PAR-NAME.                             NC2524.2
140600     PERFORM PRINT-DETAIL.                                        NC2524.2
140700 COMP-TEST-25.                                                    NC2524.2
140800     MOVE ZERO TO W-11.                                           NC2524.2
140900     COMPUTE W-11 ROUNDED = D-1 + D-7.                            NC2524.2
141000     IF ( W-11 < 2.20004) AND                                     NC2524.2
141100        ( W-11 > 2.19996) THEN                                    NC2524.2
141200              PERFORM PASS                                        NC2524.2
141300              GO TO COMP-WRITE-25.                                NC2524.2
141400     PERFORM FAIL.                                                NC2524.2
141500     MOVE W-11 TO COMPUTED-N.                                     NC2524.2
141600     MOVE "+2.2" TO CORRECT-A.                                    NC2524.2
141700     GO TO COMP-WRITE-25.                                         NC2524.2
141800 COMP-DELETE-25.                                                  NC2524.2
141900     PERFORM DE-LETE.                                             NC2524.2
142000 COMP-WRITE-25.                                                   NC2524.2
142100     MOVE "COMP-TEST-25" TO PAR-NAME.                             NC2524.2
142200     PERFORM PRINT-DETAIL.                                        NC2524.2
142300 COMP-TEST-26.                                                    NC2524.2
142400     MOVE ZERO TO W-11.                                           NC2524.2
142500     COMPUTE W-11 ROUNDED = 25 / 10.                              NC2524.2
142600     IF ( W-11 < 2.50005) AND                                     NC2524.2
142700        ( W-11 > 2.49995) THEN                                    NC2524.2
142800              PERFORM PASS                                        NC2524.2
142900              GO TO COMP-WRITE-26.                                NC2524.2
143000     PERFORM FAIL.                                                NC2524.2
143100     MOVE W-11 TO COMPUTED-N.                                     NC2524.2
143200     MOVE "+2.5" TO CORRECT-A.                                    NC2524.2
143300     GO TO COMP-WRITE-26.                                         NC2524.2
143400 COMP-DELETE-26.                                                  NC2524.2
143500     PERFORM DE-LETE.                                             NC2524.2
143600 COMP-WRITE-26.                                                   NC2524.2
143700     MOVE "COMP-TEST-26" TO PAR-NAME.                             NC2524.2
143800     PERFORM PRINT-DETAIL.                                        NC2524.2
143900 CTST-END.                                                        NC2524.2
144000     EXIT.                                                        NC2524.2
144100 COMP-INIT-A.                                                     NC2524.2
144200     MOVE     "COMPUTE" TO FEATURE.                               NC2524.2
144300 COMP-TEST-27.                                                    NC2524.2
144400     MOVE     ZERO TO WRK-DS-02V00.                               NC2524.2
144500     COMPUTE  WRK-DS-02V00 = -9.                                  NC2524.2
144600        IF WRK-DS-02V00 = -9                                      NC2524.2
144700              PERFORM PASS                                        NC2524.2
144800              GO TO COMP-WRITE-27.                                NC2524.2
144900     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC2524.2
145000     MOVE     -9 TO CORRECT-N.                                    NC2524.2
145100     PERFORM  FAIL.                                               NC2524.2
145200     GO TO COMP-WRITE-27.                                         NC2524.2
145300 COMP-DELETE-27.                                                  NC2524.2
145400     PERFORM DE-LETE.                                             NC2524.2
145500 COMP-WRITE-27.                                                   NC2524.2
145600     MOVE     "COMP-TEST-27" TO PAR-NAME.                         NC2524.2
145700     PERFORM  PRINT-DETAIL.                                       NC2524.2
145800 COMP-TEST-28.                                                    NC2524.2
145900     MOVE     ZERO TO WRK-DS-02V00.                               NC2524.2
146000     COMPUTE  WRK-DS-02V00 = A99-DS-02V00.                        NC2524.2
146100        IF WRK-DS-02V00 = 99                                      NC2524.2
146200              PERFORM PASS                                        NC2524.2
146300              GO TO COMP-WRITE-28.                                NC2524.2
146400     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC2524.2
146500     MOVE     99 TO CORRECT-N.                                    NC2524.2
146600     PERFORM  FAIL.                                               NC2524.2
146700     GO TO COMP-WRITE-28.                                         NC2524.2
146800 COMP-DELETE-28.                                                  NC2524.2
146900     PERFORM DE-LETE.                                             NC2524.2
147000 COMP-WRITE-28.                                                   NC2524.2
147100     MOVE     "COMP-TEST-28" TO PAR-NAME.                         NC2524.2
147200     PERFORM  PRINT-DETAIL.                                       NC2524.2
147300 COMP-TEST-29.                                                    NC2524.2
147400     MOVE     ZERO TO WRK-DS-18V00.                               NC2524.2
147500     COMPUTE  WRK-DS-18V00 = A18ONES-DS-18V00 + A18ONES-DS-18V00. NC2524.2
147600     IF       WRK-DS-18V00 = 222222222222222222                   NC2524.2
147700              PERFORM PASS                                        NC2524.2
147800              GO TO COMP-WRITE-29.                                NC2524.2
147900     MOVE     WRK-DS-18V00 TO COMPUTED-18V0.                      NC2524.2
148000     MOVE     222222222222222222 TO CORRECT-18V0.                 NC2524.2
148100     PERFORM  FAIL.                                               NC2524.2
148200     GO TO COMP-WRITE-29.                                         NC2524.2
148300 COMP-DELETE-29.                                                  NC2524.2
148400     PERFORM DE-LETE.                                             NC2524.2
148500 COMP-WRITE-29.                                                   NC2524.2
148600     MOVE     "COMP-TEST-29" TO PAR-NAME.                         NC2524.2
148700     PERFORM  PRINT-DETAIL.                                       NC2524.2
148800 COMP-TEST-30.                                                    NC2524.2
148900     MOVE     ZERO TO WRK-DS-18V00.                               NC2524.2
149000     COMPUTE  WRK-DS-18V00 = A18TWOS-DS-18V00 - A18ONES-DS-18V00. NC2524.2
149100     IF       WRK-DS-18V00 = 111111111111111111                   NC2524.2
149200              PERFORM PASS                                        NC2524.2
149300              GO TO COMP-WRITE-30.                                NC2524.2
149400     MOVE     WRK-DS-18V00 TO COMPUTED-18V0.                      NC2524.2
149500     MOVE     111111111111111111 TO CORRECT-18V0.                 NC2524.2
149600     PERFORM  FAIL.                                               NC2524.2
149700     GO TO COMP-WRITE-30.                                         NC2524.2
149800 COMP-DELETE-30.                                                  NC2524.2
149900     PERFORM DE-LETE.                                             NC2524.2
150000 COMP-WRITE-30.                                                   NC2524.2
150100     MOVE     "COMP-TEST-30" TO PAR-NAME.                         NC2524.2
150200     PERFORM  PRINT-DETAIL.                                       NC2524.2
150300 COMP-TEST-31.                                                    NC2524.2
150400     MOVE   ZERO TO   TA--X.                                      NC2524.2
150500     COMPUTE     TA--X  =  3 * A02TWOS-DU-02V00.                  NC2524.2
150600        IF TA--X = 66                                             NC2524.2
150700              PERFORM PASS                                        NC2524.2
150800              GO TO COMP-WRITE-31.                                NC2524.2
150900     MOVE       TA--X TO COMPUTED-N                               NC2524.2
151000     MOVE     66 TO CORRECT-N.                                    NC2524.2
151100     PERFORM  FAIL.                                               NC2524.2
151200     GO TO COMP-WRITE-31.                                         NC2524.2
151300 COMP-DELETE-31.                                                  NC2524.2
151400     PERFORM DE-LETE.                                             NC2524.2
151500 COMP-WRITE-31.                                                   NC2524.2
151600     MOVE     "COMP-TEST-31" TO PAR-NAME.                         NC2524.2
151700     PERFORM  PRINT-DETAIL.                                       NC2524.2
151800 COMP-TEST-32.                                                    NC2524.2
151900     MOVE     ZERO TO WRK-DS-05V00.                               NC2524.2
152000     COMPUTE  WRK-DS-05V00 = A02TWOS-DU-02V00 / A02TWOS-DS-03V02. NC2524.2
152100        IF WRK-DS-05V00 = 1                                       NC2524.2
152200              PERFORM PASS                                        NC2524.2
152300              GO TO COMP-WRITE-32.                                NC2524.2
152400     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC2524.2
152500     MOVE     1 TO CORRECT-N.                                     NC2524.2
152600     PERFORM  FAIL.                                               NC2524.2
152700     GO TO COMP-WRITE-32.                                         NC2524.2
152800 COMP-DELETE-32.                                                  NC2524.2
152900     PERFORM DE-LETE.                                             NC2524.2
153000 COMP-WRITE-32.                                                   NC2524.2
153100     MOVE     "COMP-TEST-32" TO PAR-NAME.                         NC2524.2
153200     PERFORM  PRINT-DETAIL.                                       NC2524.2
153300 COMP-TEST-33.                                                    NC2524.2
153400     MOVE     ZERO TO WRK-DS-05V00.                               NC2524.2
153500     COMPUTE  WRK-DS-05V00 = 3 ** ATWO-DS-01V00.                  NC2524.2
153600        IF WRK-DS-05V00 = 9                                       NC2524.2
153700              PERFORM PASS                                        NC2524.2
153800              GO TO COMP-WRITE-33.                                NC2524.2
153900     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC2524.2
154000     MOVE     9 TO CORRECT-N.                                     NC2524.2
154100     PERFORM  FAIL.                                               NC2524.2
154200     GO TO COMP-WRITE-33.                                         NC2524.2
154300 COMP-DELETE-33.                                                  NC2524.2
154400              PERFORM DE-LETE.                                    NC2524.2
154500 COMP-WRITE-33.                                                   NC2524.2
154600     MOVE     "COMP-TEST-33" TO PAR-NAME.                         NC2524.2
154700     PERFORM  PRINT-DETAIL.                                       NC2524.2
154800 COMP-TEST-34.                                                    NC2524.2
154900     MOVE     ZERO TO WRK-DS-02V00.                               NC2524.2
155000     COMPUTE  WRK-DS-02V00 ROUNDED  = A99-DS-02V00                NC2524.2
155100              +   AZERO-DS-05V05 - 2.5.                           NC2524.2
155200        IF WRK-DS-02V00 = 97                                      NC2524.2
155300              PERFORM PASS                                        NC2524.2
155400              GO TO COMP-WRITE-34.                                NC2524.2
155500     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC2524.2
155600     MOVE     97 TO CORRECT-N.                                    NC2524.2
155700     PERFORM  FAIL.                                               NC2524.2
155800     GO TO COMP-WRITE-34.                                         NC2524.2
155900 COMP-DELETE-34.                                                  NC2524.2
156000     PERFORM DE-LETE.                                             NC2524.2
156100 COMP-WRITE-34.                                                   NC2524.2
156200     MOVE     "COMP-TEST-34" TO PAR-NAME.                         NC2524.2
156300     PERFORM  PRINT-DETAIL.                                       NC2524.2
156400 COMP-TEST-35.                                                    NC2524.2
156500     MOVE     ZERO TO WRK-DS-02V00.                               NC2524.2
156600     COMPUTE  WRK-DS-02V00 = A99-DS-02V00 + AZERO-DS-05V05        NC2524.2
156700              ON SIZE ERROR                                       NC2524.2
156800              MOVE "SIZE ERR SHOULD NOT EXCUTE" TO RE-MARK        NC2524.2
156900              PERFORM FAIL                                        NC2524.2
157000              GO TO COMP-WRITE-35.                                NC2524.2
157100     PERFORM  PASS.                                               NC2524.2
157200     GO       TO COMP-WRITE-35.                                   NC2524.2
157300 COMP-DELETE-35.                                                  NC2524.2
157400     PERFORM  DE-LETE.                                            NC2524.2
157500 COMP-WRITE-35.                                                   NC2524.2
157600     MOVE     "COMP-TEST-35" TO PAR-NAME.                         NC2524.2
157700     PERFORM  PRINT-DETAIL.                                       NC2524.2
157800 COMP-TEST-36.                                                    NC2524.2
157900     IF       TEST-2NUC-COND-99                                   NC2524.2
158000              PERFORM PASS                                        NC2524.2
158100              GO TO COMP-WRITE-36.                                NC2524.2
158200*    NOTE     THIS TEST DEPENDS UPON THE RESULT OF COMP-TEST-35.  NC2524.2
158300     MOVE     WRK-DS-02V00 TO COMPUTED-N.                         NC2524.2
158400     MOVE     99 TO CORRECT-N.                                    NC2524.2
158500     PERFORM  FAIL.                                               NC2524.2
158600     GO TO COMP-WRITE-36.                                         NC2524.2
158700 COMP-DELETE-36.                                                  NC2524.2
158800     PERFORM DE-LETE.                                             NC2524.2
158900 COMP-WRITE-36.                                                   NC2524.2
159000     MOVE     "COMP-TEST-36" TO PAR-NAME.                         NC2524.2
159100     PERFORM  PRINT-DETAIL.                                       NC2524.2
159200 COMP-TEST-37.                                                    NC2524.2
159300     MOVE     ZERO TO WRK-DS-0201P.                               NC2524.2
159400     COMPUTE  WRK-DS-0201P ROUNDED = A05ONES-DS-05V00 / 5         NC2524.2
159500              ON SIZE ERROR                                       NC2524.2
159600              PERFORM PASS                                        NC2524.2
159700              GO TO COMP-WRITE-37.                                NC2524.2
159800     MOVE     "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.            NC2524.2
159900     PERFORM  FAIL.                                               NC2524.2
160000     GO TO COMP-WRITE-37.                                         NC2524.2
160100 COMP-DELETE-37.                                                  NC2524.2
160200     PERFORM DE-LETE.                                             NC2524.2
160300 COMP-WRITE-37.                                                   NC2524.2
160400     MOVE     "COMP-TEST-37" TO PAR-NAME.                         NC2524.2
160500     PERFORM  PRINT-DETAIL.                                       NC2524.2
160600 COMP-TEST-38.                                                    NC2524.2
160700        IF WRK-DS-0201P = ZERO                                    NC2524.2
160800              PERFORM PASS                                        NC2524.2
160900              GO TO COMP-WRITE-38.                                NC2524.2
161000*    NOTE     THIS TEST DEPENDS UPON THE RESULT OF COMP-TEST-37.  NC2524.2
161100     MOVE     WRK-DS-0201P TO COMPUTED-N.                         NC2524.2
161200     MOVE     ZERO TO CORRECT-N.                                  NC2524.2
161300     PERFORM  FAIL.                                               NC2524.2
161400     GO TO COMP-WRITE-38.                                         NC2524.2
161500 COMP-DELETE-38.                                                  NC2524.2
161600     PERFORM DE-LETE.                                             NC2524.2
161700 COMP-WRITE-38.                                                   NC2524.2
161800     MOVE     "COMP-TEST-38" TO PAR-NAME.                         NC2524.2
161900     PERFORM  PRINT-DETAIL.                                       NC2524.2
162000 COMP-TEST-39-42.                                                 NC2524.2
162100     MOVE SPACES TO TEST-RESULTS.                                 NC2524.2
162200     MOVE "NOT USED" TO RE-MARK.                                  NC2524.2
162300     MOVE "COMP-TEST-39" TO PAR-NAME.                             NC2524.2
162400     PERFORM PRINT-DETAIL.                                        NC2524.2
162500     MOVE "NOT USED" TO RE-MARK.                                  NC2524.2
162600     MOVE "COMP-TEST-40" TO PAR-NAME.                             NC2524.2
162700     PERFORM PRINT-DETAIL.                                        NC2524.2
162800     MOVE "NOT USED" TO RE-MARK.                                  NC2524.2
162900     MOVE "COMP-TEST-41" TO PAR-NAME.                             NC2524.2
163000     PERFORM PRINT-DETAIL.                                        NC2524.2
163100     MOVE "NOT USED" TO RE-MARK.                                  NC2524.2
163200     MOVE "COMP-TEST-42" TO PAR-NAME.                             NC2524.2
163300     PERFORM PRINT-DETAIL.                                        NC2524.2
163400     MOVE "COMPUTE" TO FEATURE.                                   NC2524.2
163500 COMP-TEST-43.                                                    NC2524.2
163600     MOVE    ZEROS TO WHOLE-FIELD.                                NC2524.2
163700     COMPUTE WHOLE-FIELD =                                        NC2524.2
163800     (1 + (2 - (3 + (4 - (5 + (6 - (7 + (8 - (9 + (10 -           NC2524.2
163900     EVEN-NAME1)))))))))).                                        NC2524.2
164000     IF (WHOLE-FIELD < 10.0002) AND                               NC2524.2
164100        (WHOLE-FIELD > 9.9998) PERFORM PASS                       NC2524.2
164200         GO TO COMP-WRITE-43.                                     NC2524.2
164300     MOVE WHOLE-FIELD TO COMPUTED-18V0.                           NC2524.2
164400     MOVE 10 TO CORRECT-18V0.                                     NC2524.2
164500     PERFORM FAIL.                                                NC2524.2
164600     GO TO COMP-WRITE-43.                                         NC2524.2
164700 COMP-DELETE-43.                                                  NC2524.2
164800     PERFORM DE-LETE.                                             NC2524.2
164900 COMP-WRITE-43.                                                   NC2524.2
165000     MOVE "COMP-TEST-43" TO PAR-NAME.                             NC2524.2
165100     PERFORM PRINT-DETAIL.                                        NC2524.2
165200 COMP-TEST-44.                                                    NC2524.2
165300     MOVE    ZEROS TO WHOLE-FIELD.                                NC2524.2
165400     COMPUTE  WHOLE-FIELD =                                       NC2524.2
165500     (ONE + (TWO - (THREE + (FOUR - (FIVE + (SIX - (SEVEN +       NC2524.2
165600         (EIGHT - (NINE + (TEN - EVEN-NAME1)))))))))).            NC2524.2
165700        IF WHOLE-FIELD = 10    PERFORM PASS                       NC2524.2
165800         GO TO COMP-WRITE-44.                                     NC2524.2
165900     MOVE WHOLE-FIELD TO COMPUTED-18V0.                           NC2524.2
166000     MOVE 10 TO CORRECT-18V0.                                     NC2524.2
166100     PERFORM FAIL.                                                NC2524.2
166200     GO TO COMP-WRITE-44.                                         NC2524.2
166300 COMP-DELETE-44.                                                  NC2524.2
166400     PERFORM DE-LETE.                                             NC2524.2
166500 COMP-WRITE-44.                                                   NC2524.2
166600     MOVE "COMP-TEST-44" TO PAR-NAME.                             NC2524.2
166700     PERFORM PRINT-DETAIL.                                        NC2524.2
166800 COMP-INT-045.                                                    NC2524.2
166900     MOVE    "COMPUTE SERIES"  TO FEATURE.                        NC2524.2
167000     MOVE    "COMP-TEST-045"  TO PAR-NAME.                        NC2524.2
167100 COMP-TEST-045.                                                   NC2524.2
167200     COMPUTE  WRK-DS-05V00-0002                                   NC2524.2
167300              WRK-DS-04V01-0005   ROUNDED                         NC2524.2
167400              WRK-DS-03V04-0003F-0014  (2, 2, 2) =  174 / 16.     NC2524.2
167500*                                                                 NC2524.2
167600*    IDENTIFIER SERIES  -  WITH AND WITHOUT ROUNDED  -            NC2524.2
167700*             SUBSCRIPTED DATA ITEM.                              NC2524.2
167800*                                                                 NC2524.2
167900     MOVE "COMP-TEST-045-1" TO PAR-NAME.                          NC2524.2
168000        IF WRK-DS-05V00-0002 NOT = 10                             NC2524.2
168100              MOVE      +00010  TO  CORRECT-N                     NC2524.2
168200              MOVE     WRK-DS-05V00-0002   TO COMPUTED-N          NC2524.2
168300              PERFORM COMP-WRITE-045 GO TO COMP-TEST-045-2.       NC2524.2
168400     PERFORM PASS.  PERFORM COMP-WRITE-045.                       NC2524.2
168500 COMP-TEST-045-2.                                                 NC2524.2
168600     MOVE "COMP-TEST-045-2" TO PAR-NAME.                          NC2524.2
168700     IF (WRK-DS-04V01-0005 > 10.9002180) OR                       NC2524.2
168800        (WRK-DS-04V01-0005 < 10.8997820) PERFORM FAIL             NC2524.2
168900              MOVE +10.9 TO CORRECT-N                             NC2524.2
169000              MOVE WRK-DS-04V01-0005 TO COMPUTED-N                NC2524.2
169100              PERFORM COMP-WRITE-045 GO TO COMP-TEST-045-3.       NC2524.2
169200     PERFORM PASS.  PERFORM COMP-WRITE-045.                       NC2524.2
169300 COMP-TEST-045-3.                                                 NC2524.2
169400     MOVE "COMP-TEST-045-3" TO PAR-NAME.                          NC2524.2
169500     IF (WRK-DS-03V04-0003F-0014 (2, 2, 2) > 10.87521750) OR      NC2524.2
169600        (WRK-DS-03V04-0003F-0014 (2, 2, 2) < 10.87479250)         NC2524.2
169700             PERFORM FAIL MOVE +010.8750 TO CORRECT-N             NC2524.2
169800             MOVE WRK-DS-03V04-0003F-0014 (2, 2, 2) TO COMPUTED-N NC2524.2
169900             GO TO COMP-WRITE-045.                                NC2524.2
170000     PERFORM PASS.                                                NC2524.2
170100     GO TO      COMP-WRITE-045.                                   NC2524.2
170200 COMP-DELETE-045.                                                 NC2524.2
170300     PERFORM  DE-LETE.                                            NC2524.2
170400 COMP-WRITE-045.                                                  NC2524.2
170500     PERFORM  PRINT-DETAIL.                                       NC2524.2
170600 COMP-TEST-045-EXIT.                                              NC2524.2
170700     EXIT.                                                        NC2524.2
170800 CCVS-EXIT SECTION.                                               NC2524.2
170900 CCVS-999999.                                                     NC2524.2
171000     GO TO CLOSE-FILES.                                           NC2524.2
      *END-OF,NC252A                                                                  
