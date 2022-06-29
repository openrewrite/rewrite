      *HEADER,COBOL,NC207A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2074.2
000200 PROGRAM-ID.                                                      NC2074.2
000300     NC207A.                                                      NC2074.2
000400****************************************************************  NC2074.2
000500*                                                              *  NC2074.2
000600*    VALIDATION FOR:-                                          *  NC2074.2
000700*                                                              *  NC2074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2074.2
000900*                                                              *  NC2074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2074.2
001100*                                                              *  NC2074.2
001200****************************************************************  NC2074.2
001300*                                                              *  NC2074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2074.2
001500*                                                              *  NC2074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2074.2
001900*                                                              *  NC2074.2
002000****************************************************************  NC2074.2
002100*                                                              *  NC2074.2
002200*    PROGRAM NC207A TESTS THE USE OF FORMAT 1 QUALIFICATION    *  NC2074.2
002300*    USING FORMATS 1, 2 AND 3 OF THE "ADD" STATEMENT, FORMATS  *  NC2074.2
002400*    2 AND 3 OF THE "SUBTRACT" STATEMENT, FORMAT 2 OF THE      *  NC2074.2
002500*    "MULTIPLY" STATEMENT AND FORMAT 3 OF THE "DIVIDE"*           NC2074.2
002600*    STATEMENT.                                                *  NC2074.2
002700*    THE MAJORITY OF TESTST USE UP TO FIVE LEVELS OF           *  NC2074.2
002800*    QUALIFICATION BUT THE MINIMUM REQUIREMENT OF 49 LEVELS IN *  NC2074.2
002900*    THE NUCLEUS IS ALSO TESTED.                               *  NC2074.2
003000*                                                              *  NC2074.2
003100****************************************************************  NC2074.2
003200 ENVIRONMENT DIVISION.                                            NC2074.2
003300 CONFIGURATION SECTION.                                           NC2074.2
003400 SOURCE-COMPUTER.                                                 NC2074.2
003500     XXXXX082.                                                    NC2074.2
003600 OBJECT-COMPUTER.                                                 NC2074.2
003700     XXXXX083.                                                    NC2074.2
003800 INPUT-OUTPUT SECTION.                                            NC2074.2
003900 FILE-CONTROL.                                                    NC2074.2
004000     SELECT PRINT-FILE ASSIGN TO                                  NC2074.2
004100     XXXXX055.                                                    NC2074.2
004200 DATA DIVISION.                                                   NC2074.2
004300 FILE SECTION.                                                    NC2074.2
004400 FD  PRINT-FILE.                                                  NC2074.2
004500 01  PRINT-REC PICTURE X(120).                                    NC2074.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 NC2074.2
004700 WORKING-STORAGE SECTION.                                         NC2074.2
004800 77  ACCUMULATOR1                PIC 9(18) VALUE ZERO.            NC2074.2
004900 77  ACCUMULATOR2                PIC 9(18) VALUE ZERO.            NC2074.2
005000 01  TABLE-LEVEL-5A.                                              NC2074.2
005100     02  TABLE-LEVEL-4A.                                          NC2074.2
005200         03  TABLE-LEVEL-3A.                                      NC2074.2
005300             04  TABLE-LEVEL-2A.                                  NC2074.2
005400                 05  TABLE-LEVEL-1A.                              NC2074.2
005500                     06  TBL-ITEM-1   PIC 9  VALUE 1.             NC2074.2
005600                 05  TABLE-LEVEL-1B.                              NC2074.2
005700                     06  TBL-ITEM-1   PIC 9(2)  VALUE 2.          NC2074.2
005800             04  TABLE-LEVEL-2B.                                  NC2074.2
005900                 05  TABLE-LEVEL-1A.                              NC2074.2
006000                     06  TBL-ITEM-1   PIC 9(3)  VALUE 3.          NC2074.2
006100                 05  TABLE-LEVEL-1B.                              NC2074.2
006200                     06  TBL-ITEM-1   PIC 9(4)  VALUE 4.          NC2074.2
006300         03  TABLE-LEVEL-3B.                                      NC2074.2
006400             04  TABLE-LEVEL-2A.                                  NC2074.2
006500                 05  TABLE-LEVEL-1A.                              NC2074.2
006600                     06  TBL-ITEM-1   PIC 9(5)  VALUE 5.          NC2074.2
006700                 05  TABLE-LEVEL-1B.                              NC2074.2
006800                     06  TBL-ITEM-1   PIC 9(6)  VALUE 6.          NC2074.2
006900             04  TABLE-LEVEL-2B.                                  NC2074.2
007000                 05  TABLE-LEVEL-1A.                              NC2074.2
007100                     06  TBL-ITEM-1   PIC 9(7)  VALUE 7.          NC2074.2
007200                 05  TABLE-LEVEL-1B.                              NC2074.2
007300                     06  TBL-ITEM-1   PIC 9(8)  VALUE 8.          NC2074.2
007400     02  TABLE-LEVEL-4B.                                          NC2074.2
007500         03  TABLE-LEVEL-3A.                                      NC2074.2
007600             04  TABLE-LEVEL-2A.                                  NC2074.2
007700                 05  TABLE-LEVEL-1A.                              NC2074.2
007800                     06  TBL-ITEM-1   PIC 9(9)  VALUE 9.          NC2074.2
007900                 05  TABLE-LEVEL-1B.                              NC2074.2
008000                     06  TBL-ITEM-1   PIC 9(10) VALUE 10.         NC2074.2
008100             04  TABLE-LEVEL-2B.                                  NC2074.2
008200                 05  TABLE-LEVEL-1A.                              NC2074.2
008300                     06  TBL-ITEM-1   PIC 9(11) VALUE 11.         NC2074.2
008400                 05  TABLE-LEVEL-1B.                              NC2074.2
008500                     06  TBL-ITEM-1   PIC 9(12) VALUE 12.         NC2074.2
008600         03  TABLE-LEVEL-3B.                                      NC2074.2
008700             04  TABLE-LEVEL-2A.                                  NC2074.2
008800                 05  TABLE-LEVEL-1A.                              NC2074.2
008900                     06  TBL-ITEM-1   PIC 9(13) VALUE 13.         NC2074.2
009000                 05  TABLE-LEVEL-1B.                              NC2074.2
009100                     06  TBL-ITEM-1   PIC 9(14) VALUE 14.         NC2074.2
009200             04  TABLE-LEVEL-2B.                                  NC2074.2
009300                 05  TABLE-LEVEL-1A.                              NC2074.2
009400                     06  TBL-ITEM-1   PIC 9(15) VALUE 15.         NC2074.2
009500                 05  TABLE-LEVEL-1B.                              NC2074.2
009600                     06  TBL-ITEM-1   PIC 9(16) VALUE 16.         NC2074.2
009700 01  TABLE-LEVEL-5B.                                              NC2074.2
009800     02  TABLE-LEVEL-4A.                                          NC2074.2
009900         03  TABLE-LEVEL-3A.                                      NC2074.2
010000             04  TABLE-LEVEL-2A.                                  NC2074.2
010100                 05  TABLE-LEVEL-1A.                              NC2074.2
010200                     06  TBL-ITEM-1   PIC 9(16) VALUE 16.         NC2074.2
010300                 05  TABLE-LEVEL-1B.                              NC2074.2
010400                     06  TBL-ITEM-1   PIC 9(15) VALUE 15.         NC2074.2
010500             04  TABLE-LEVEL-2B.                                  NC2074.2
010600                 05  TABLE-LEVEL-1A.                              NC2074.2
010700                     06  TBL-ITEM-1   PIC 9(14) VALUE 14.         NC2074.2
010800                 05  TABLE-LEVEL-1B.                              NC2074.2
010900                     06  TBL-ITEM-1   PIC 9(13) VALUE 13.         NC2074.2
011000         03  TABLE-LEVEL-3B.                                      NC2074.2
011100             04  TABLE-LEVEL-2A.                                  NC2074.2
011200                 05  TABLE-LEVEL-1A.                              NC2074.2
011300                     06  TBL-ITEM-1   PIC 9(12) VALUE 12.         NC2074.2
011400                 05  TABLE-LEVEL-1B.                              NC2074.2
011500                     06  TBL-ITEM-1   PIC 9(11) VALUE 11.         NC2074.2
011600             04  TABLE-LEVEL-2B.                                  NC2074.2
011700                 05  TABLE-LEVEL-1A.                              NC2074.2
011800                     06  TBL-ITEM-1   PIC 9(10) VALUE 10.         NC2074.2
011900                 05  TABLE-LEVEL-1B.                              NC2074.2
012000                     06  TBL-ITEM-1   PIC 9(9)  VALUE 9.          NC2074.2
012100     02  TABLE-LEVEL-4B.                                          NC2074.2
012200         03  TABLE-LEVEL-3A.                                      NC2074.2
012300             04  TABLE-LEVEL-2A.                                  NC2074.2
012400                 05  TABLE-LEVEL-1A.                              NC2074.2
012500                     06  TBL-ITEM-1   PIC 9(8)  VALUE 8.          NC2074.2
012600                 05  TABLE-LEVEL-1B.                              NC2074.2
012700                     06  TBL-ITEM-1   PIC 9(7)  VALUE 7.          NC2074.2
012800             04  TABLE-LEVEL-2B.                                  NC2074.2
012900                 05  TABLE-LEVEL-1A.                              NC2074.2
013000                     06  TBL-ITEM-1   PIC 9(6)  VALUE 6.          NC2074.2
013100                 05  TABLE-LEVEL-1B.                              NC2074.2
013200                     06  TBL-ITEM-1   PIC 9(5)  VALUE 5.          NC2074.2
013300         03  TABLE-LEVEL-3B.                                      NC2074.2
013400             04  TABLE-LEVEL-2A.                                  NC2074.2
013500                 05  TABLE-LEVEL-1A.                              NC2074.2
013600                     06  TBL-ITEM-1   PIC 9(4)  VALUE 4.          NC2074.2
013700                 05  TABLE-LEVEL-1B.                              NC2074.2
013800                     06  TBL-ITEM-1   PIC 9(3)  VALUE 3.          NC2074.2
013900             04  TABLE-LEVEL-2B.                                  NC2074.2
014000                 05  TABLE-LEVEL-1A.                              NC2074.2
014100                     06  TBL-ITEM-1   PIC 9(2)  VALUE 2.          NC2074.2
014200                 05  TABLE-LEVEL-1B.                              NC2074.2
014300                     06  TBL-ITEM-1   PIC 99 VALUE 1.             NC2074.2
014400                                                                  NC2074.2
014500 01  TABLE-LEVEL-5C.                                              NC2074.2
014600     02  TABLE-LEVEL-4A.                                          NC2074.2
014700         03  TABLE-LEVEL-3A.                                      NC2074.2
014800             04  TABLE-LEVEL-2A.                                  NC2074.2
014900                 05  TABLE-LEVEL-1A.                              NC2074.2
015000                     06  TBL-ITEM-1   PIC 9  VALUE 1.             NC2074.2
015100                 05  TABLE-LEVEL-1B.                              NC2074.2
015200                     06  TBL-ITEM-1   PIC 9(2)  VALUE  2.         NC2074.2
015300             04  TABLE-LEVEL-2B.                                  NC2074.2
015400                 05  TABLE-LEVEL-1A.                              NC2074.2
015500                     06  TBL-ITEM-1   PIC 9(3)  VALUE  3.         NC2074.2
015600                 05  TABLE-LEVEL-1B.                              NC2074.2
015700                     06  TBL-ITEM-1   PIC 9(4)  VALUE  4.         NC2074.2
015800         03  TABLE-LEVEL-3B.                                      NC2074.2
015900             04  TABLE-LEVEL-2A.                                  NC2074.2
016000                 05  TABLE-LEVEL-1A.                              NC2074.2
016100                     06  TBL-ITEM-1   PIC 9(5)  VALUE  5.         NC2074.2
016200                 05  TABLE-LEVEL-1B.                              NC2074.2
016300                     06  TBL-ITEM-1   PIC 9(6)  VALUE  6.         NC2074.2
016400             04  TABLE-LEVEL-2B.                                  NC2074.2
016500                 05  TABLE-LEVEL-1A.                              NC2074.2
016600                     06  TBL-ITEM-1   PIC 9(7)  VALUE  7.         NC2074.2
016700                 05  TABLE-LEVEL-1B.                              NC2074.2
016800                     06  TBL-ITEM-1   PIC 9(8)  VALUE  8.         NC2074.2
016900     02  TABLE-LEVEL-4B.                                          NC2074.2
017000         03  TABLE-LEVEL-3A.                                      NC2074.2
017100             04  TABLE-LEVEL-2A.                                  NC2074.2
017200                 05  TABLE-LEVEL-1A.                              NC2074.2
017300                     06  TBL-ITEM-1   PIC 9(9)  VALUE  9.         NC2074.2
017400                 05  TABLE-LEVEL-1B.                              NC2074.2
017500                     06  TBL-ITEM-1   PIC 9(10) VALUE 10.         NC2074.2
017600             04  TABLE-LEVEL-2B.                                  NC2074.2
017700                 05  TABLE-LEVEL-1A.                              NC2074.2
017800                     06  TBL-ITEM-1   PIC 9(11) VALUE 11.         NC2074.2
017900                 05  TABLE-LEVEL-1B.                              NC2074.2
018000                     06  TBL-ITEM-1   PIC 9(12) VALUE 12.         NC2074.2
018100         03  TABLE-LEVEL-3B.                                      NC2074.2
018200             04  TABLE-LEVEL-2A.                                  NC2074.2
018300                 05  TABLE-LEVEL-1A.                              NC2074.2
018400                     06  TBL-ITEM-1   PIC 9(13) VALUE 13.         NC2074.2
018500                 05  TABLE-LEVEL-1B.                              NC2074.2
018600                     06  TBL-ITEM-1   PIC 9(14) VALUE 14.         NC2074.2
018700             04  TABLE-LEVEL-2B.                                  NC2074.2
018800                 05  TABLE-LEVEL-1A.                              NC2074.2
018900                     06  TBL-ITEM-1   PIC 9(15) VALUE 15.         NC2074.2
019000                 05  TABLE-LEVEL-1B.                              NC2074.2
019100                     06  TBL-ITEM-1   PIC 9(16) VALUE 16.         NC2074.2
019200 01  TABLE-5B-INIT.                                               NC2074.2
019300     02 FILLER         PIC 9(16)  VALUE  16.                      NC2074.2
019400     02 FILLER         PIC 9(15)  VALUE  15.                      NC2074.2
019500     02 FILLER         PIC 9(14)  VALUE  14.                      NC2074.2
019600     02 FILLER         PIC 9(13)  VALUE  13.                      NC2074.2
019700     02 FILLER         PIC 9(12)  VALUE  12.                      NC2074.2
019800     02 FILLER         PIC 9(11)  VALUE  11.                      NC2074.2
019900     02 FILLER         PIC 9(10)  VALUE  10.                      NC2074.2
020000     02 FILLER         PIC 9(9)   VALUE  9.                       NC2074.2
020100     02 FILLER         PIC 9(8)   VALUE  8.                       NC2074.2
020200     02 FILLER         PIC 9(7)   VALUE  7.                       NC2074.2
020300     02 FILLER         PIC 9(6)   VALUE  6.                       NC2074.2
020400     02 FILLER         PIC 9(5)   VALUE  5.                       NC2074.2
020500     02 FILLER         PIC 9(4)   VALUE  4.                       NC2074.2
020600     02 FILLER         PIC 9(3)   VALUE  3.                       NC2074.2
020700     02 FILLER         PIC 9(2)   VALUE  2.                       NC2074.2
020800     02 FILLER         PIC 9(2)   VALUE  1.                       NC2074.2
020900 01  FIRST-GROUP.                                                 NC2074.2
021000  02 GROUP-02.                                                    NC2074.2
021100   03 GROUP-03.                                                   NC2074.2
021200    04 GROUP-04.                                                  NC2074.2
021300     05 GROUP-05.                                                 NC2074.2
021400      06 GROUP-06.                                                NC2074.2
021500       07 GROUP-07.                                               NC2074.2
021600        08 GROUP-08.                                              NC2074.2
021700         09 GROUP-09.                                             NC2074.2
021800          10 GROUP-10.                                            NC2074.2
021900           11 GROUP-11.                                           NC2074.2
022000            12 GROUP-12.                                          NC2074.2
022100             13 GROUP-13.                                         NC2074.2
022200              14 GROUP-14.                                        NC2074.2
022300               15 GROUP-15.                                       NC2074.2
022400                16 GROUP-16.                                      NC2074.2
022500                 17 GROUP-17.                                     NC2074.2
022600                  18 GROUP-18.                                    NC2074.2
022700                   19 GROUP-19.                                   NC2074.2
022800                    20 GROUP-20.                                  NC2074.2
022900                     21 GROUP-21.                                 NC2074.2
023000                      22 GROUP-22.                                NC2074.2
023100                       23 GROUP-23.                               NC2074.2
023200                        24 GROUP-24.                              NC2074.2
023300                         25 GROUP-25.                             NC2074.2
023400                          26 GROUP-26.                            NC2074.2
023500                           27 GROUP-27.                           NC2074.2
023600                            28 GROUP-28.                          NC2074.2
023700                             29 GROUP-29.                         NC2074.2
023800                              30 GROUP-30.                        NC2074.2
023900                               31 GROUP-31.                       NC2074.2
024000                                32 GROUP-32.                      NC2074.2
024100                                 33 GROUP-33.                     NC2074.2
024200                                  34 GROUP-34.                    NC2074.2
024300                                   35 GROUP-35.                   NC2074.2
024400                                    36 GROUP-36.                  NC2074.2
024500                                     37 GROUP-37.                 NC2074.2
024600                                      38 GROUP-38.                NC2074.2
024700                                       39 GROUP-39.               NC2074.2
024800                                        40 GROUP-40.              NC2074.2
024900                                         41 GROUP-41.             NC2074.2
025000                                          42 GROUP-42.            NC2074.2
025100                                           43 GROUP-43.           NC2074.2
025200                                            44 GROUP-44.          NC2074.2
025300                                             45 GROUP-45.         NC2074.2
025400                                              46 GROUP-46.        NC2074.2
025500                                               47 GROUP-47.       NC2074.2
025600                                                48 GROUP-48.      NC2074.2
025700                   49 GROUP-49-1  PIC 9(4)           VALUE 1.     NC2074.2
025800                   49 GROUP-49-2  PIC S9(3)  COMP    VALUE 2.     NC2074.2
025900                   49 GROUP-49-3  PIC S9(15) COMP    VALUE 3.     NC2074.2
026000                   49 GROUP-49-4  PIC S9(8)  COMP    VALUE 4.     NC2074.2
026100                   49 GROUP-49-5  PIC 9(8)           VALUE 5.     NC2074.2
026200 01  SECOND-GROUP.                                                NC2074.2
026300  02 GROUP-02.                                                    NC2074.2
026400   03 GROUP-03.                                                   NC2074.2
026500    04 GROUP-04.                                                  NC2074.2
026600     05 GROUP-05.                                                 NC2074.2
026700      06 GROUP-06.                                                NC2074.2
026800       07 GROUP-07.                                               NC2074.2
026900        08 GROUP-08.                                              NC2074.2
027000         09 GROUP-09.                                             NC2074.2
027100          10 GROUP-10.                                            NC2074.2
027200           11 GROUP-11.                                           NC2074.2
027300            12 GROUP-12.                                          NC2074.2
027400             13 GROUP-13.                                         NC2074.2
027500              14 GROUP-14.                                        NC2074.2
027600               15 GROUP-15.                                       NC2074.2
027700                16 GROUP-16.                                      NC2074.2
027800                 17 GROUP-17.                                     NC2074.2
027900                  18 GROUP-18.                                    NC2074.2
028000                   19 GROUP-19.                                   NC2074.2
028100                    20 GROUP-20.                                  NC2074.2
028200                     21 GROUP-21.                                 NC2074.2
028300                      22 GROUP-22.                                NC2074.2
028400                       23 GROUP-23.                               NC2074.2
028500                        24 GROUP-24.                              NC2074.2
028600                         25 GROUP-25.                             NC2074.2
028700                          26 GROUP-26.                            NC2074.2
028800                           27 GROUP-27.                           NC2074.2
028900                            28 GROUP-28.                          NC2074.2
029000                             29 GROUP-29.                         NC2074.2
029100                              30 GROUP-30.                        NC2074.2
029200                               31 GROUP-31.                       NC2074.2
029300                                32 GROUP-32.                      NC2074.2
029400                                 33 GROUP-33.                     NC2074.2
029500                                  34 GROUP-34.                    NC2074.2
029600                                   35 GROUP-35.                   NC2074.2
029700                                    36 GROUP-36.                  NC2074.2
029800                                     37 GROUP-37.                 NC2074.2
029900                                      38 GROUP-38.                NC2074.2
030000                                       39 GROUP-39.               NC2074.2
030100                                        40 GROUP-40.              NC2074.2
030200                                         41 GROUP-41.             NC2074.2
030300                                          42 GROUP-42.            NC2074.2
030400                                           43 GROUP-43.           NC2074.2
030500                                            44 GROUP-44.          NC2074.2
030600                                             45 GROUP-45.         NC2074.2
030700                                              46 GROUP-46.        NC2074.2
030800                                               47 GROUP-47.       NC2074.2
030900                                                48 GROUP-48.      NC2074.2
031000                   49 GROUP-49-1  PIC 9(4)           VALUE 100.   NC2074.2
031100                   49 GROUP-49-2  PIC S9(3)  COMP    VALUE 200.   NC2074.2
031200                   49 GROUP-49-3  PIC S9(15) COMP    VALUE 300.   NC2074.2
031300                   49 GROUP-49-4  PIC S9(8)  COMP    VALUE 400.   NC2074.2
031400                   49 GROUP-49-5  PIC 9(8)           VALUE 500.   NC2074.2
031500                     88 LEVEL-49-OK VALUE 500.                    NC2074.2
031600 01  TEST-RESULTS.                                                NC2074.2
031700     02 FILLER                   PIC X      VALUE SPACE.          NC2074.2
031800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2074.2
031900     02 FILLER                   PIC X      VALUE SPACE.          NC2074.2
032000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2074.2
032100     02 FILLER                   PIC X      VALUE SPACE.          NC2074.2
032200     02  PAR-NAME.                                                NC2074.2
032300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2074.2
032400       03  PARDOT-X              PIC X      VALUE SPACE.          NC2074.2
032500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2074.2
032600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2074.2
032700     02 RE-MARK                  PIC X(61).                       NC2074.2
032800 01  TEST-COMPUTED.                                               NC2074.2
032900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2074.2
033000     02 FILLER                   PIC X(17)  VALUE                 NC2074.2
033100            "       COMPUTED=".                                   NC2074.2
033200     02 COMPUTED-X.                                               NC2074.2
033300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2074.2
033400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2074.2
033500                                 PIC -9(9).9(9).                  NC2074.2
033600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2074.2
033700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2074.2
033800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2074.2
033900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2074.2
034000         04 COMPUTED-18V0                    PIC -9(18).          NC2074.2
034100         04 FILLER                           PIC X.               NC2074.2
034200     03 FILLER PIC X(50) VALUE SPACE.                             NC2074.2
034300 01  TEST-CORRECT.                                                NC2074.2
034400     02 FILLER PIC X(30) VALUE SPACE.                             NC2074.2
034500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2074.2
034600     02 CORRECT-X.                                                NC2074.2
034700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2074.2
034800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2074.2
034900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2074.2
035000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2074.2
035100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2074.2
035200     03      CR-18V0 REDEFINES CORRECT-A.                         NC2074.2
035300         04 CORRECT-18V0                     PIC -9(18).          NC2074.2
035400         04 FILLER                           PIC X.               NC2074.2
035500     03 FILLER PIC X(2) VALUE SPACE.                              NC2074.2
035600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2074.2
035700 01  CCVS-C-1.                                                    NC2074.2
035800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2074.2
035900-    "SS  PARAGRAPH-NAME                                          NC2074.2
036000-    "       REMARKS".                                            NC2074.2
036100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2074.2
036200 01  CCVS-C-2.                                                    NC2074.2
036300     02 FILLER                     PIC X        VALUE SPACE.      NC2074.2
036400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2074.2
036500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2074.2
036600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2074.2
036700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2074.2
036800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2074.2
036900 01  REC-CT                        PIC 99       VALUE ZERO.       NC2074.2
037000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2074.2
037100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2074.2
037200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2074.2
037300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2074.2
037400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2074.2
037500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2074.2
037600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2074.2
037700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2074.2
037800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2074.2
037900 01  CCVS-H-1.                                                    NC2074.2
038000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2074.2
038100     02  FILLER                    PIC X(42)    VALUE             NC2074.2
038200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2074.2
038300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2074.2
038400 01  CCVS-H-2A.                                                   NC2074.2
038500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2074.2
038600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2074.2
038700   02  FILLER                        PIC XXXX   VALUE             NC2074.2
038800     "4.2 ".                                                      NC2074.2
038900   02  FILLER                        PIC X(28)  VALUE             NC2074.2
039000            " COPY - NOT FOR DISTRIBUTION".                       NC2074.2
039100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2074.2
039200                                                                  NC2074.2
039300 01  CCVS-H-2B.                                                   NC2074.2
039400   02  FILLER                        PIC X(15)  VALUE             NC2074.2
039500            "TEST RESULT OF ".                                    NC2074.2
039600   02  TEST-ID                       PIC X(9).                    NC2074.2
039700   02  FILLER                        PIC X(4)   VALUE             NC2074.2
039800            " IN ".                                               NC2074.2
039900   02  FILLER                        PIC X(12)  VALUE             NC2074.2
040000     " HIGH       ".                                              NC2074.2
040100   02  FILLER                        PIC X(22)  VALUE             NC2074.2
040200            " LEVEL VALIDATION FOR ".                             NC2074.2
040300   02  FILLER                        PIC X(58)  VALUE             NC2074.2
040400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2074.2
040500 01  CCVS-H-3.                                                    NC2074.2
040600     02  FILLER                      PIC X(34)  VALUE             NC2074.2
040700            " FOR OFFICIAL USE ONLY    ".                         NC2074.2
040800     02  FILLER                      PIC X(58)  VALUE             NC2074.2
040900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2074.2
041000     02  FILLER                      PIC X(28)  VALUE             NC2074.2
041100            "  COPYRIGHT   1985 ".                                NC2074.2
041200 01  CCVS-E-1.                                                    NC2074.2
041300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2074.2
041400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2074.2
041500     02 ID-AGAIN                     PIC X(9).                    NC2074.2
041600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2074.2
041700 01  CCVS-E-2.                                                    NC2074.2
041800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2074.2
041900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2074.2
042000     02 CCVS-E-2-2.                                               NC2074.2
042100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2074.2
042200         03 FILLER                   PIC X      VALUE SPACE.      NC2074.2
042300         03 ENDER-DESC               PIC X(44)  VALUE             NC2074.2
042400            "ERRORS ENCOUNTERED".                                 NC2074.2
042500 01  CCVS-E-3.                                                    NC2074.2
042600     02  FILLER                      PIC X(22)  VALUE             NC2074.2
042700            " FOR OFFICIAL USE ONLY".                             NC2074.2
042800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2074.2
042900     02  FILLER                      PIC X(58)  VALUE             NC2074.2
043000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2074.2
043100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2074.2
043200     02 FILLER                       PIC X(15)  VALUE             NC2074.2
043300             " COPYRIGHT 1985".                                   NC2074.2
043400 01  CCVS-E-4.                                                    NC2074.2
043500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2074.2
043600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2074.2
043700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2074.2
043800     02 FILLER                       PIC X(40)  VALUE             NC2074.2
043900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2074.2
044000 01  XXINFO.                                                      NC2074.2
044100     02 FILLER                       PIC X(19)  VALUE             NC2074.2
044200            "*** INFORMATION ***".                                NC2074.2
044300     02 INFO-TEXT.                                                NC2074.2
044400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2074.2
044500       04 XXCOMPUTED                 PIC X(20).                   NC2074.2
044600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2074.2
044700       04 XXCORRECT                  PIC X(20).                   NC2074.2
044800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2074.2
044900 01  HYPHEN-LINE.                                                 NC2074.2
045000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2074.2
045100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2074.2
045200-    "*****************************************".                 NC2074.2
045300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2074.2
045400-    "******************************".                            NC2074.2
045500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2074.2
045600     "NC207".                                                     NC2074.2
045700 PROCEDURE DIVISION.                                              NC2074.2
045800 CCVS1 SECTION.                                                   NC2074.2
045900 OPEN-FILES.                                                      NC2074.2
046000     OPEN     OUTPUT PRINT-FILE.                                  NC2074.2
046100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2074.2
046200     MOVE    SPACE TO TEST-RESULTS.                               NC2074.2
046300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2074.2
046400     GO TO CCVS1-EXIT.                                            NC2074.2
046500 CLOSE-FILES.                                                     NC2074.2
046600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2074.2
046700 TERMINATE-CCVS.                                                  NC2074.2
046800*    EXIT PROGRAM.                                                NC2074.2
046900*TERMINATE-CALL.                                                  NC2074.2
047000     STOP     RUN.                                                NC2074.2
047100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2074.2
047200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2074.2
047300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2074.2
047400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2074.2
047500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2074.2
047600 PRINT-DETAIL.                                                    NC2074.2
047700     IF REC-CT NOT EQUAL TO ZERO                                  NC2074.2
047800             MOVE "." TO PARDOT-X                                 NC2074.2
047900             MOVE REC-CT TO DOTVALUE.                             NC2074.2
048000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2074.2
048100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2074.2
048200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2074.2
048300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2074.2
048400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2074.2
048500     MOVE SPACE TO CORRECT-X.                                     NC2074.2
048600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2074.2
048700     MOVE     SPACE TO RE-MARK.                                   NC2074.2
048800 HEAD-ROUTINE.                                                    NC2074.2
048900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2074.2
049000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2074.2
049100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2074.2
049200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2074.2
049300 COLUMN-NAMES-ROUTINE.                                            NC2074.2
049400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2074.2
049500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2074.2
049600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2074.2
049700 END-ROUTINE.                                                     NC2074.2
049800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2074.2
049900 END-RTN-EXIT.                                                    NC2074.2
050000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2074.2
050100 END-ROUTINE-1.                                                   NC2074.2
050200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2074.2
050300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2074.2
050400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2074.2
050500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2074.2
050600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2074.2
050700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2074.2
050800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2074.2
050900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2074.2
051000  END-ROUTINE-12.                                                 NC2074.2
051100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2074.2
051200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2074.2
051300         MOVE "NO " TO ERROR-TOTAL                                NC2074.2
051400         ELSE                                                     NC2074.2
051500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2074.2
051600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2074.2
051700     PERFORM WRITE-LINE.                                          NC2074.2
051800 END-ROUTINE-13.                                                  NC2074.2
051900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2074.2
052000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2074.2
052100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2074.2
052200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2074.2
052300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2074.2
052400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2074.2
052500          MOVE "NO " TO ERROR-TOTAL                               NC2074.2
052600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2074.2
052700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2074.2
052800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2074.2
052900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2074.2
053000 WRITE-LINE.                                                      NC2074.2
053100     ADD 1 TO RECORD-COUNT.                                       NC2074.2
053200*    IF RECORD-COUNT GREATER 50                                   NC2074.2
053300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2074.2
053400*        MOVE SPACE TO DUMMY-RECORD                               NC2074.2
053500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2074.2
053600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2074.2
053700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2074.2
053800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2074.2
053900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2074.2
054000*        MOVE ZERO TO RECORD-COUNT.                               NC2074.2
054100     PERFORM WRT-LN.                                              NC2074.2
054200 WRT-LN.                                                          NC2074.2
054300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2074.2
054400     MOVE SPACE TO DUMMY-RECORD.                                  NC2074.2
054500 BLANK-LINE-PRINT.                                                NC2074.2
054600     PERFORM WRT-LN.                                              NC2074.2
054700 FAIL-ROUTINE.                                                    NC2074.2
054800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2074.2
054900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2074.2
055000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2074.2
055100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2074.2
055200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2074.2
055300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2074.2
055400     GO TO  FAIL-ROUTINE-EX.                                      NC2074.2
055500 FAIL-ROUTINE-WRITE.                                              NC2074.2
055600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2074.2
055700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2074.2
055800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2074.2
055900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2074.2
056000 FAIL-ROUTINE-EX. EXIT.                                           NC2074.2
056100 BAIL-OUT.                                                        NC2074.2
056200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2074.2
056300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2074.2
056400 BAIL-OUT-WRITE.                                                  NC2074.2
056500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2074.2
056600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2074.2
056700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2074.2
056800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2074.2
056900 BAIL-OUT-EX. EXIT.                                               NC2074.2
057000 CCVS1-EXIT.                                                      NC2074.2
057100     EXIT.                                                        NC2074.2
057200 SECT-NC207A-001 SECTION.                                         NC2074.2
057300 ADD-INIT-F1-1.                                                   NC2074.2
057400     MOVE "ADD-TEST-F1-1 " TO PAR-NAME.                           NC2074.2
057500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
057600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
057700     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
057800 ADD-TEST-F1-1.                                                   NC2074.2
057900     ADD TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF        NC2074.2
058000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
058100         TO ACCUMULATOR1.                                         NC2074.2
058200     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
058300         PERFORM PASS                                             NC2074.2
058400         GO TO ADD-WRITE-F1-1.                                    NC2074.2
058500     GO TO ADD-FAIL-F1-1.                                         NC2074.2
058600 ADD-DELETE-F1-1.                                                 NC2074.2
058700     PERFORM DE-LETE.                                             NC2074.2
058800     GO TO ADD-WRITE-F1-1.                                        NC2074.2
058900 ADD-FAIL-F1-1.                                                   NC2074.2
059000     MOVE 1 TO CORRECT-N.                                         NC2074.2
059100     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2074.2
059200         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
059300         TO COMPUTED-N.                                           NC2074.2
059400     PERFORM FAIL.                                                NC2074.2
059500 ADD-WRITE-F1-1.                                                  NC2074.2
059600     PERFORM PRINT-DETAIL.                                        NC2074.2
059700*                                                                 NC2074.2
059800 ADD-INIT-F1-2.                                                   NC2074.2
059900     MOVE "ADD-TEST-F1-2 " TO PAR-NAME.                           NC2074.2
060000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
060100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
060200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
060300 ADD-TEST-F1-2.                                                   NC2074.2
060400     ADD TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF        NC2074.2
060500         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
060600         ACCUMULATOR1.                                            NC2074.2
060700     IF ACCUMULATOR1 EQUAL TO 2                                   NC2074.2
060800         PERFORM PASS                                             NC2074.2
060900         GO TO ADD-WRITE-F1-2.                                    NC2074.2
061000     GO TO ADD-FAIL-F1-2.                                         NC2074.2
061100 ADD-DELETE-F1-2.                                                 NC2074.2
061200     PERFORM DE-LETE.                                             NC2074.2
061300     GO TO ADD-WRITE-F1-2.                                        NC2074.2
061400 ADD-FAIL-F1-2.                                                   NC2074.2
061500     MOVE 2 TO CORRECT-N.                                         NC2074.2
061600     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF       NC2074.2
061700         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
061800         COMPUTED-N.                                              NC2074.2
061900     PERFORM FAIL.                                                NC2074.2
062000 ADD-WRITE-F1-2.                                                  NC2074.2
062100     PERFORM PRINT-DETAIL.                                        NC2074.2
062200*                                                                 NC2074.2
062300 ADD-INIT-F1-3.                                                   NC2074.2
062400     MOVE "ADD-TEST-F1-3 " TO PAR-NAME.                           NC2074.2
062500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
062600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
062700     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
062800 ADD-TEST-F1-3.                                                   NC2074.2
062900     ADD TBL-ITEM-1 IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN        NC2074.2
063000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2074.2
063100         ACCUMULATOR1.                                            NC2074.2
063200     IF ACCUMULATOR1 EQUAL TO 3                                   NC2074.2
063300         PERFORM PASS                                             NC2074.2
063400         GO TO ADD-WRITE-F1-3.                                    NC2074.2
063500     GO TO ADD-FAIL-F1-3.                                         NC2074.2
063600 ADD-DELETE-F1-3.                                                 NC2074.2
063700     PERFORM DE-LETE.                                             NC2074.2
063800     GO TO ADD-WRITE-F1-3.                                        NC2074.2
063900 ADD-FAIL-F1-3.                                                   NC2074.2
064000     MOVE TBL-ITEM-1 IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2074.2
064100         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2074.2
064200         COMPUTED-N.                                              NC2074.2
064300     MOVE 3 TO CORRECT-N.                                         NC2074.2
064400     PERFORM FAIL.                                                NC2074.2
064500 ADD-WRITE-F1-3.                                                  NC2074.2
064600     PERFORM PRINT-DETAIL.                                        NC2074.2
064700*                                                                 NC2074.2
064800 ADD-INIT-F1-4.                                                   NC2074.2
064900     MOVE "ADD-TEST-F1-4 " TO PAR-NAME.                           NC2074.2
065000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
065100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
065200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
065300 ADD-TEST-F1-4.                                                   NC2074.2
065400     ADD TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF        NC2074.2
065500         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2074.2
065600         ACCUMULATOR1.                                            NC2074.2
065700     IF ACCUMULATOR1 EQUAL TO 4                                   NC2074.2
065800         PERFORM PASS                                             NC2074.2
065900         GO TO ADD-WRITE-F1-4.                                    NC2074.2
066000     GO TO ADD-FAIL-F1-4.                                         NC2074.2
066100 ADD-DELETE-F1-4.                                                 NC2074.2
066200     PERFORM DE-LETE.                                             NC2074.2
066300     GO TO ADD-WRITE-F1-4.                                        NC2074.2
066400 ADD-FAIL-F1-4.                                                   NC2074.2
066500     MOVE 4 TO CORRECT-N.                                         NC2074.2
066600     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2074.2
066700         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2074.2
066800         COMPUTED-N.                                              NC2074.2
066900     PERFORM FAIL.                                                NC2074.2
067000 ADD-WRITE-F1-4.                                                  NC2074.2
067100     PERFORM PRINT-DETAIL.                                        NC2074.2
067200*                                                                 NC2074.2
067300 ADD-INIT-F1-5.                                                   NC2074.2
067400     MOVE "ADD-TEST-F1-5 " TO PAR-NAME.                           NC2074.2
067500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
067600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
067700     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
067800 ADD-TEST-F1-5.                                                   NC2074.2
067900     ADD TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF        NC2074.2
068000         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
068100         ACCUMULATOR1.                                            NC2074.2
068200     IF ACCUMULATOR1 EQUAL TO 5                                   NC2074.2
068300         PERFORM PASS                                             NC2074.2
068400         GO TO ADD-WRITE-F1-5.                                    NC2074.2
068500     GO TO ADD-FAIL-F1-5.                                         NC2074.2
068600 ADD-DELETE-F1-5.                                                 NC2074.2
068700     PERFORM DE-LETE.                                             NC2074.2
068800     GO TO ADD-WRITE-F1-5.                                        NC2074.2
068900 ADD-FAIL-F1-5.                                                   NC2074.2
069000     MOVE 5 TO CORRECT-N.                                         NC2074.2
069100     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF       NC2074.2
069200         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
069300         COMPUTED-N.                                              NC2074.2
069400     PERFORM FAIL.                                                NC2074.2
069500 ADD-WRITE-F1-5.                                                  NC2074.2
069600     PERFORM PRINT-DETAIL.                                        NC2074.2
069700*                                                                 NC2074.2
069800 ADD-INIT-F1-6.                                                   NC2074.2
069900     MOVE "ADD-TEST-F1-6 " TO PAR-NAME.                           NC2074.2
070000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
070100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
070200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
070300 ADD-TEST-F1-6.                                                   NC2074.2
070400     ADD TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN        NC2074.2
070500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
070600         ACCUMULATOR1.                                            NC2074.2
070700     IF ACCUMULATOR1 EQUAL TO 6                                   NC2074.2
070800         PERFORM PASS                                             NC2074.2
070900         GO TO ADD-WRITE-F1-6.                                    NC2074.2
071000     GO TO ADD-FAIL-F1-6.                                         NC2074.2
071100 ADD-DELETE-F1-6.                                                 NC2074.2
071200     PERFORM DE-LETE.                                             NC2074.2
071300     GO TO ADD-WRITE-F1-6.                                        NC2074.2
071400 ADD-FAIL-F1-6.                                                   NC2074.2
071500     MOVE 6 TO CORRECT-N.                                         NC2074.2
071600     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2074.2
071700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
071800         COMPUTED-N.                                              NC2074.2
071900     PERFORM FAIL.                                                NC2074.2
072000 ADD-WRITE-F1-6.                                                  NC2074.2
072100     PERFORM PRINT-DETAIL.                                        NC2074.2
072200*                                                                 NC2074.2
072300 ADD-INIT-F1-7.                                                   NC2074.2
072400     MOVE "ADD-TEST-F1-7" TO PAR-NAME.                            NC2074.2
072500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
072600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
072700     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
072800 ADD-TEST-F1-7.                                                   NC2074.2
072900     ADD TBL-ITEM-1 IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF        NC2074.2
073000         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
073100         ACCUMULATOR1.                                            NC2074.2
073200     IF ACCUMULATOR1 EQUAL TO 7                                   NC2074.2
073300         PERFORM PASS                                             NC2074.2
073400         GO TO ADD-WRITE-F1-7.                                    NC2074.2
073500     GO TO ADD-FAIL-F1-7.                                         NC2074.2
073600 ADD-DELETE-F1-7.                                                 NC2074.2
073700     PERFORM DE-LETE.                                             NC2074.2
073800     GO TO ADD-WRITE-F1-7.                                        NC2074.2
073900 ADD-FAIL-F1-7.                                                   NC2074.2
074000     MOVE TBL-ITEM-1 IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF       NC2074.2
074100         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2074.2
074200         COMPUTED-N.                                              NC2074.2
074300     MOVE 7 TO CORRECT-N.                                         NC2074.2
074400     PERFORM FAIL.                                                NC2074.2
074500 ADD-WRITE-F1-7.                                                  NC2074.2
074600     PERFORM PRINT-DETAIL.                                        NC2074.2
074700*                                                                 NC2074.2
074800 ADD-INIT-F1-8.                                                   NC2074.2
074900     MOVE "ADD-TEST-F1-8 " TO PAR-NAME.                           NC2074.2
075000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
075100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
075200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
075300 ADD-TEST-F1-8.                                                   NC2074.2
075400     ADD TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN        NC2074.2
075500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2074.2
075600         ACCUMULATOR1.                                            NC2074.2
075700     IF ACCUMULATOR1 EQUAL TO 8                                   NC2074.2
075800         PERFORM PASS                                             NC2074.2
075900         GO TO ADD-WRITE-F1-8.                                    NC2074.2
076000     GO TO ADD-FAIL-F1-8.                                         NC2074.2
076100 ADD-DELETE-F1-8.                                                 NC2074.2
076200     PERFORM DE-LETE.                                             NC2074.2
076300     GO TO ADD-WRITE-F1-8.                                        NC2074.2
076400 ADD-FAIL-F1-8.                                                   NC2074.2
076500     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN       NC2074.2
076600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2074.2
076700         COMPUTED-N.                                              NC2074.2
076800     MOVE 8 TO CORRECT-N.                                         NC2074.2
076900     PERFORM FAIL.                                                NC2074.2
077000 ADD-WRITE-F1-8.                                                  NC2074.2
077100     PERFORM PRINT-DETAIL.                                        NC2074.2
077200*                                                                 NC2074.2
077300 ADD-INIT-F2-9.                                                   NC2074.2
077400     MOVE "ADD-TEST-F2-9 " TO PAR-NAME.                           NC2074.2
077500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
077600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
077700     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
077800 ADD-TEST-F2-9.                                                   NC2074.2
077900     ADD TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF        NC2074.2
078000         TABLE-LEVEL-3A IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A       NC2074.2
078100         TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF        NC2074.2
078200         TABLE-LEVEL-3A IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A       NC2074.2
078300         GIVING ACCUMULATOR2.                                     NC2074.2
078400     IF ACCUMULATOR2 EQUAL TO 18                                  NC2074.2
078500         PERFORM PASS                                             NC2074.2
078600         GO TO ADD-WRITE-F2-9.                                    NC2074.2
078700     GO TO ADD-FAIL-F2-9.                                         NC2074.2
078800 ADD-DELETE-F2-9.                                                 NC2074.2
078900     PERFORM DE-LETE.                                             NC2074.2
079000     GO TO ADD-WRITE-F2-9.                                        NC2074.2
079100 ADD-FAIL-F2-9.                                                   NC2074.2
079200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
079300     MOVE 18 TO CORRECT-N.                                        NC2074.2
079400     PERFORM FAIL.                                                NC2074.2
079500 ADD-WRITE-F2-9.                                                  NC2074.2
079600     PERFORM PRINT-DETAIL.                                        NC2074.2
079700*                                                                 NC2074.2
079800 ADD-INIT-F2-10.                                                  NC2074.2
079900     MOVE "ADD-TEST-F2-10 " TO PAR-NAME.                          NC2074.2
080000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
080100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
080200     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
080300 ADD-TEST-F2-10.                                                  NC2074.2
080400     ADD TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN        NC2074.2
080500         TABLE-LEVEL-3A IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A       NC2074.2
080600         TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN        NC2074.2
080700         TABLE-LEVEL-3A IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A       NC2074.2
080800         GIVING ACCUMULATOR2.                                     NC2074.2
080900     IF ACCUMULATOR2 EQUAL TO 20                                  NC2074.2
081000         PERFORM PASS                                             NC2074.2
081100         GO TO ADD-WRITE-F2-10.                                   NC2074.2
081200     GO TO ADD-FAIL-F2-10.                                        NC2074.2
081300 ADD-DELETE-F2-10.                                                NC2074.2
081400     PERFORM DE-LETE.                                             NC2074.2
081500     GO TO ADD-WRITE-F2-10.                                       NC2074.2
081600 ADD-FAIL-F2-10.                                                  NC2074.2
081700     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
081800     MOVE 20 TO CORRECT-N.                                        NC2074.2
081900     PERFORM FAIL.                                                NC2074.2
082000 ADD-WRITE-F2-10.                                                 NC2074.2
082100     PERFORM PRINT-DETAIL.                                        NC2074.2
082200*                                                                 NC2074.2
082300 ADD-INIT-F2-11.                                                  NC2074.2
082400     MOVE "ADD-TEST-F2-11 " TO PAR-NAME.                          NC2074.2
082500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
082600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
082700     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
082800 ADD-TEST-F2-11.                                                  NC2074.2
082900     ADD TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF        NC2074.2
083000         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A       NC2074.2
083100         TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF        NC2074.2
083200         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A       NC2074.2
083300         GIVING ACCUMULATOR2.                                     NC2074.2
083400     IF ACCUMULATOR2 EQUAL TO 22                                  NC2074.2
083500         PERFORM PASS                                             NC2074.2
083600         GO TO ADD-WRITE-F2-11.                                   NC2074.2
083700     GO TO ADD-FAIL-F2-11.                                        NC2074.2
083800 ADD-DELETE-F2-11.                                                NC2074.2
083900     PERFORM DE-LETE.                                             NC2074.2
084000     GO TO ADD-WRITE-F2-11.                                       NC2074.2
084100 ADD-FAIL-F2-11.                                                  NC2074.2
084200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
084300     MOVE 22 TO CORRECT-N.                                        NC2074.2
084400     PERFORM FAIL.                                                NC2074.2
084500 ADD-WRITE-F2-11.                                                 NC2074.2
084600     PERFORM PRINT-DETAIL.                                        NC2074.2
084700*                                                                 NC2074.2
084800 ADD-INIT-F2-12.                                                  NC2074.2
084900     MOVE "ADD-TEST-F2-12 " TO PAR-NAME.                          NC2074.2
085000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
085100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
085200     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
085300 ADD-TEST-F2-12.                                                  NC2074.2
085400     ADD TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN        NC2074.2
085500         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
085600         TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN        NC2074.2
085700         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
085800         GIVING ACCUMULATOR2.                                     NC2074.2
085900     IF ACCUMULATOR2 EQUAL TO 24                                  NC2074.2
086000         PERFORM PASS                                             NC2074.2
086100         GO TO ADD-WRITE-F2-12.                                   NC2074.2
086200     GO TO ADD-FAIL-F2-12.                                        NC2074.2
086300 ADD-DELETE-F2-12.                                                NC2074.2
086400     PERFORM DE-LETE.                                             NC2074.2
086500     GO TO ADD-WRITE-F2-12.                                       NC2074.2
086600 ADD-FAIL-F2-12.                                                  NC2074.2
086700     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
086800     MOVE 24 TO CORRECT-N.                                        NC2074.2
086900     PERFORM FAIL.                                                NC2074.2
087000 ADD-WRITE-F2-12.                                                 NC2074.2
087100     PERFORM PRINT-DETAIL.                                        NC2074.2
087200*                                                                 NC2074.2
087300 ADD-INIT-F2-13.                                                  NC2074.2
087400     MOVE "ADD-TEST-F2-13 " TO PAR-NAME.                          NC2074.2
087500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
087600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
087700     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
087800 ADD-TEST-F2-13.                                                  NC2074.2
087900     ADD TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A IN        NC2074.2
088000         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
088100         TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A IN        NC2074.2
088200         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
088300         GIVING ACCUMULATOR2.                                     NC2074.2
088400     IF ACCUMULATOR2 EQUAL TO 26                                  NC2074.2
088500         PERFORM PASS                                             NC2074.2
088600         GO TO ADD-WRITE-F2-13.                                   NC2074.2
088700     GO TO ADD-FAIL-F2-13.                                        NC2074.2
088800 ADD-DELETE-F2-13.                                                NC2074.2
088900     PERFORM DE-LETE.                                             NC2074.2
089000     GO TO ADD-WRITE-F2-13.                                       NC2074.2
089100 ADD-FAIL-F2-13.                                                  NC2074.2
089200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
089300     MOVE 26 TO CORRECT-N.                                        NC2074.2
089400     PERFORM FAIL.                                                NC2074.2
089500 ADD-WRITE-F2-13.                                                 NC2074.2
089600     PERFORM PRINT-DETAIL.                                        NC2074.2
089700*                                                                 NC2074.2
089800 ADD-INIT-F2-14.                                                  NC2074.2
089900     MOVE "ADD-TEST-F2-14 " TO PAR-NAME.                          NC2074.2
090000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
090100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
090200     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
090300 ADD-TEST-F2-14.                                                  NC2074.2
090400     ADD TBL-ITEM-1 IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN        NC2074.2
090500         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
090600         TBL-ITEM-1 IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN        NC2074.2
090700         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
090800         GIVING ACCUMULATOR2.                                     NC2074.2
090900     IF ACCUMULATOR2 EQUAL TO 28                                  NC2074.2
091000         PERFORM PASS                                             NC2074.2
091100         GO TO ADD-WRITE-F2-14.                                   NC2074.2
091200     GO TO ADD-FAIL-F2-14.                                        NC2074.2
091300 ADD-DELETE-F2-14.                                                NC2074.2
091400     PERFORM DE-LETE.                                             NC2074.2
091500     GO TO ADD-WRITE-F2-14.                                       NC2074.2
091600 ADD-FAIL-F2-14.                                                  NC2074.2
091700     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
091800     MOVE 28 TO CORRECT-N.                                        NC2074.2
091900     PERFORM FAIL.                                                NC2074.2
092000 ADD-WRITE-F2-14.                                                 NC2074.2
092100     PERFORM PRINT-DETAIL.                                        NC2074.2
092200*                                                                 NC2074.2
092300 ADD-INIT-F2-15.                                                  NC2074.2
092400     MOVE "ADD-TEST-F2-15 " TO PAR-NAME.                          NC2074.2
092500     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
092600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
092700     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
092800 ADD-TEST-F2-15.                                                  NC2074.2
092900     ADD TBL-ITEM-1 IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN        NC2074.2
093000         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
093100         TBL-ITEM-1 IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN        NC2074.2
093200         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
093300         GIVING ACCUMULATOR2.                                     NC2074.2
093400     IF ACCUMULATOR2 EQUAL TO 30                                  NC2074.2
093500         PERFORM PASS                                             NC2074.2
093600         GO TO ADD-WRITE-F2-15.                                   NC2074.2
093700     GO TO ADD-FAIL-F2-15.                                        NC2074.2
093800 ADD-DELETE-F2-15.                                                NC2074.2
093900     PERFORM DE-LETE.                                             NC2074.2
094000     GO TO ADD-WRITE-F2-15.                                       NC2074.2
094100 ADD-FAIL-F2-15.                                                  NC2074.2
094200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
094300     MOVE 30 TO CORRECT-N.                                        NC2074.2
094400     PERFORM FAIL.                                                NC2074.2
094500 ADD-WRITE-F2-15.                                                 NC2074.2
094600     PERFORM PRINT-DETAIL.                                        NC2074.2
094700*                                                                 NC2074.2
094800 ADD-INIT-F2-16.                                                  NC2074.2
094900     MOVE "ADD-TEST-F2-16 " TO PAR-NAME.                          NC2074.2
095000     MOVE "QUALIFIED ADDITION" TO FEATURE.                        NC2074.2
095100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
095200     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
095300 ADD-TEST-F2-16.                                                  NC2074.2
095400     ADD TBL-ITEM-1 IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF        NC2074.2
095500         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
095600         TBL-ITEM-1 IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF        NC2074.2
095700         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
095800         GIVING ACCUMULATOR2.                                     NC2074.2
095900     IF ACCUMULATOR2 EQUAL TO 32                                  NC2074.2
096000         PERFORM PASS                                             NC2074.2
096100         GO TO ADD-WRITE-F2-16.                                   NC2074.2
096200     GO TO ADD-FAIL-F2-16.                                        NC2074.2
096300 ADD-DELETE-F2-16.                                                NC2074.2
096400     PERFORM DE-LETE.                                             NC2074.2
096500     GO TO ADD-WRITE-F2-16.                                       NC2074.2
096600 ADD-FAIL-F2-16.                                                  NC2074.2
096700     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
096800     MOVE 32 TO CORRECT-N.                                        NC2074.2
096900     PERFORM FAIL.                                                NC2074.2
097000 ADD-WRITE-F2-16.                                                 NC2074.2
097100     PERFORM PRINT-DETAIL.                                        NC2074.2
097200*                                                                 NC2074.2
097300 ADD-INIT-F3-17.                                                  NC2074.2
097400     MOVE "ADD-TEST-F3-17" TO PAR-NAME.                           NC2074.2
097500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
097600     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
097700     ADD CORRESPONDING TABLE-LEVEL-5A TO TABLE-LEVEL-5B.          NC2074.2
097800 ADD-TEST-F3-17.                                                  NC2074.2
097900     IF  TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF        NC2074.2
098000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
098100         EQUAL TO 17                                              NC2074.2
098200         PERFORM PASS                                             NC2074.2
098300         GO TO ADD-WRITE-F3-17.                                   NC2074.2
098400     GO TO ADD-FAIL-F3-17.                                        NC2074.2
098500 ADD-DELETE-F3-17.                                                NC2074.2
098600     PERFORM DE-LETE.                                             NC2074.2
098700     GO TO ADD-WRITE-F3-17.                                       NC2074.2
098800 ADD-FAIL-F3-17.                                                  NC2074.2
098900     MOVE 17 TO CORRECT-N.                                        NC2074.2
099000     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2074.2
099100         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
099200         TO COMPUTED-N.                                           NC2074.2
099300     PERFORM FAIL.                                                NC2074.2
099400 ADD-WRITE-F3-17.                                                 NC2074.2
099500     PERFORM PRINT-DETAIL.                                        NC2074.2
099600*                                                                 NC2074.2
099700 ADD-INIT-F3-18.                                                  NC2074.2
099800     MOVE "ADD-TEST-F3-18" TO PAR-NAME.                           NC2074.2
099900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
100000     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
100100 ADD-TEST-F3-18.                                                  NC2074.2
100200     IF  TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF        NC2074.2
100300         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
100400         EQUAL TO 17                                              NC2074.2
100500         PERFORM PASS                                             NC2074.2
100600         GO TO ADD-WRITE-F3-18.                                   NC2074.2
100700 ADD-DELETE-F3-18.                                                NC2074.2
100800     PERFORM DE-LETE.                                             NC2074.2
100900     GO TO ADD-WRITE-F3-18.                                       NC2074.2
101000 ADD-FAIL-F3-18.                                                  NC2074.2
101100     MOVE 17 TO CORRECT-N.                                        NC2074.2
101200     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF       NC2074.2
101300         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
101400     TO  COMPUTED-N.                                              NC2074.2
101500     PERFORM FAIL.                                                NC2074.2
101600 ADD-WRITE-F3-18.                                                 NC2074.2
101700     PERFORM PRINT-DETAIL.                                        NC2074.2
101800*                                                                 NC2074.2
101900 ADD-INIT-F3-19.                                                  NC2074.2
102000     MOVE "ADD-TEST-F3-19" TO PAR-NAME.                           NC2074.2
102100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
102200     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
102300 ADD-TEST-F3-19.                                                  NC2074.2
102400     IF  TBL-ITEM-1 IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN        NC2074.2
102500         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5B       NC2074.2
102600         EQUAL TO 17                                              NC2074.2
102700         PERFORM PASS                                             NC2074.2
102800         GO TO ADD-WRITE-F3-19.                                   NC2074.2
102900     GO TO ADD-FAIL-F3-19.                                        NC2074.2
103000 ADD-DELETE-F3-19.                                                NC2074.2
103100     PERFORM DE-LETE.                                             NC2074.2
103200     GO TO ADD-WRITE-F3-19.                                       NC2074.2
103300 ADD-FAIL-F3-19.                                                  NC2074.2
103400     MOVE 17 TO CORRECT-N.                                        NC2074.2
103500     MOVE TBL-ITEM-1 IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2074.2
103600         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5B       NC2074.2
103700     TO  COMPUTED-N.                                              NC2074.2
103800     PERFORM FAIL.                                                NC2074.2
103900 ADD-WRITE-F3-19.                                                 NC2074.2
104000     PERFORM PRINT-DETAIL.                                        NC2074.2
104100*                                                                 NC2074.2
104200 ADD-INIT-F3-20.                                                  NC2074.2
104300     MOVE "ADD-TEST-F3-20" TO PAR-NAME.                           NC2074.2
104400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
104500     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
104600 ADD-TEST-F3-20.                                                  NC2074.2
104700     IF  TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF        NC2074.2
104800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5B       NC2074.2
104900         EQUAL TO 17                                              NC2074.2
105000         PERFORM PASS                                             NC2074.2
105100         GO TO ADD-WRITE-F3-20.                                   NC2074.2
105200     GO TO ADD-FAIL-F3-20.                                        NC2074.2
105300 ADD-DELETE-F3-20.                                                NC2074.2
105400     PERFORM DE-LETE.                                             NC2074.2
105500     GO TO ADD-WRITE-F3-20.                                       NC2074.2
105600 ADD-FAIL-F3-20.                                                  NC2074.2
105700     MOVE 17 TO CORRECT-N.                                        NC2074.2
105800     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2074.2
105900         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5B       NC2074.2
106000     TO  COMPUTED-N.                                              NC2074.2
106100     PERFORM FAIL.                                                NC2074.2
106200 ADD-WRITE-F3-20.                                                 NC2074.2
106300     PERFORM PRINT-DETAIL.                                        NC2074.2
106400*                                                                 NC2074.2
106500 ADD-INIT-F3-21.                                                  NC2074.2
106600     MOVE "ADD-TEST-F3-21" TO PAR-NAME.                           NC2074.2
106700     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
106800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
106900 ADD-TEST-F3-21.                                                  NC2074.2
107000     IF  TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF        NC2074.2
107100         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
107200         EQUAL TO 17                                              NC2074.2
107300         PERFORM PASS                                             NC2074.2
107400         GO TO ADD-WRITE-F3-21.                                   NC2074.2
107500     GO TO ADD-FAIL-F3-21.                                        NC2074.2
107600 ADD-DELETE-F3-21.                                                NC2074.2
107700     PERFORM DE-LETE.                                             NC2074.2
107800     GO TO ADD-WRITE-F3-21.                                       NC2074.2
107900 ADD-FAIL-F3-21.                                                  NC2074.2
108000     MOVE 17 TO CORRECT-N.                                        NC2074.2
108100     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF       NC2074.2
108200         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
108300     TO  COMPUTED-N.                                              NC2074.2
108400     PERFORM FAIL.                                                NC2074.2
108500 ADD-WRITE-F3-21.                                                 NC2074.2
108600     PERFORM PRINT-DETAIL.                                        NC2074.2
108700*                                                                 NC2074.2
108800 ADD-INIT-F3-22.                                                  NC2074.2
108900     MOVE "ADD-TEST-F3-22" TO PAR-NAME.                           NC2074.2
109000     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
109100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
109200 ADD-TEST-F3-22.                                                  NC2074.2
109300     IF  TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN        NC2074.2
109400         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
109500         EQUAL TO 17                                              NC2074.2
109600         PERFORM PASS                                             NC2074.2
109700         GO TO ADD-WRITE-F3-22.                                   NC2074.2
109800     GO TO ADD-FAIL-F3-22.                                        NC2074.2
109900 ADD-DELETE-F3-22.                                                NC2074.2
110000     PERFORM DE-LETE.                                             NC2074.2
110100     GO TO ADD-WRITE-F3-22.                                       NC2074.2
110200 ADD-FAIL-F3-22.                                                  NC2074.2
110300     MOVE 17 TO CORRECT-N.                                        NC2074.2
110400     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2074.2
110500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B TO    NC2074.2
110600         COMPUTED-N.                                              NC2074.2
110700     PERFORM FAIL.                                                NC2074.2
110800 ADD-WRITE-F3-22.                                                 NC2074.2
110900     PERFORM PRINT-DETAIL.                                        NC2074.2
111000*                                                                 NC2074.2
111100 ADD-INIT-F3-23.                                                  NC2074.2
111200     MOVE "ADD-TEST-F3-23" TO PAR-NAME.                           NC2074.2
111300     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
111400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
111500 ADD-TEST-F3-23.                                                  NC2074.2
111600     IF  TBL-ITEM-1 IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF        NC2074.2
111700         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
111800         EQUAL TO 17                                              NC2074.2
111900         PERFORM PASS                                             NC2074.2
112000         GO TO ADD-WRITE-F3-23.                                   NC2074.2
112100     GO TO ADD-FAIL-F3-23.                                        NC2074.2
112200 ADD-DELETE-F3-23.                                                NC2074.2
112300     PERFORM DE-LETE.                                             NC2074.2
112400     GO TO ADD-WRITE-F3-23.                                       NC2074.2
112500 ADD-FAIL-F3-23.                                                  NC2074.2
112600     MOVE 17 TO CORRECT-N.                                        NC2074.2
112700     MOVE TBL-ITEM-1 IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF       NC2074.2
112800         TABLE-LEVEL-3B OF TABLE-LEVEL-4A OF TABLE-LEVEL-5B TO    NC2074.2
112900         COMPUTED-N.                                              NC2074.2
113000     PERFORM FAIL.                                                NC2074.2
113100 ADD-WRITE-F3-23.                                                 NC2074.2
113200     PERFORM PRINT-DETAIL.                                        NC2074.2
113300*                                                                 NC2074.2
113400 ADD-INIT-F3-24.                                                  NC2074.2
113500     MOVE "ADD-TEST-F3-24" TO PAR-NAME.                           NC2074.2
113600     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
113700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
113800 ADD-TEST-F3-24.                                                  NC2074.2
113900     IF  TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN        NC2074.2
114000         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5B       NC2074.2
114100         EQUAL TO 17                                              NC2074.2
114200         PERFORM PASS                                             NC2074.2
114300         GO TO ADD-WRITE-F3-24.                                   NC2074.2
114400     GO TO ADD-FAIL-F3-24.                                        NC2074.2
114500 ADD-DELETE-F3-24.                                                NC2074.2
114600     PERFORM DE-LETE.                                             NC2074.2
114700     GO TO ADD-WRITE-F3-24.                                       NC2074.2
114800 ADD-FAIL-F3-24.                                                  NC2074.2
114900     MOVE 17 TO CORRECT-N.                                        NC2074.2
115000     MOVE TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN       NC2074.2
115100         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5B TO    NC2074.2
115200         COMPUTED-N.                                              NC2074.2
115300     PERFORM FAIL.                                                NC2074.2
115400 ADD-WRITE-F3-24.                                                 NC2074.2
115500     PERFORM PRINT-DETAIL.                                        NC2074.2
115600     PERFORM END-ROUTINE.                                         NC2074.2
115700*                                                                 NC2074.2
115800 ADD-INIT-F1-25.                                                  NC2074.2
115900*    ===-->  48 LEVELS OF QUALIFICATION  <--===                   NC2074.2
116000     MOVE   "ADD-TEST-F1-25 " TO PAR-NAME.                        NC2074.2
116100     MOVE   "VI-2 1.3.2" TO ANSI-REFERENCE.                       NC2074.2
116200     MOVE    ZERO TO ACCUMULATOR2.                                NC2074.2
116300 ADD-TEST-F1-25.                                                  NC2074.2
116400     ADD     GROUP-49-1  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
116500             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
116600             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
116700             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
116800             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
116900             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
117000             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
117100             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
117200             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
117300             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
117400             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
117500             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
117600             IN SECOND-GROUP                                      NC2074.2
117700          TO ACCUMULATOR2.                                        NC2074.2
117800     IF      ACCUMULATOR2 EQUAL TO 100                            NC2074.2
117900             PERFORM PASS                                         NC2074.2
118000             GO TO   ADD-WRITE-F1-25.                             NC2074.2
118100     GO TO ADD-FAIL-F1-25.                                        NC2074.2
118200 ADD-DELETE-F1-25.                                                NC2074.2
118300     PERFORM DE-LETE.                                             NC2074.2
118400     GO TO   ADD-WRITE-F1-25.                                     NC2074.2
118500 ADD-FAIL-F1-25.                                                  NC2074.2
118600     MOVE    100 TO CORRECT-N.                                    NC2074.2
118700     MOVE    GROUP-49-1  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
118800             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
118900             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
119000             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
119100             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
119200             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
119300             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
119400             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
119500             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
119600             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
119700             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
119800             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
119900             IN SECOND-GROUP                                      NC2074.2
120000          TO COMPUTED-N.                                          NC2074.2
120100     PERFORM FAIL.                                                NC2074.2
120200 ADD-WRITE-F1-25.                                                 NC2074.2
120300     PERFORM PRINT-DETAIL.                                        NC2074.2
120400*                                                                 NC2074.2
120500 SUB-INIT-F2-1.                                                   NC2074.2
120600     MOVE "SUB-TEST-F2-1  " TO PAR-NAME.                          NC2074.2
120700     MOVE "QUALIFIED SUBTRACT" TO FEATURE.                        NC2074.2
120800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
120900     MOVE    TABLE-5B-INIT  TO TABLE-LEVEL-5B.                    NC2074.2
121000     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
121100 SUB-TEST-F2-1.                                                   NC2074.2
121200     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
121300         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
121400         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
121500         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
121600         GIVING ACCUMULATOR1.                                     NC2074.2
121700     IF ACCUMULATOR1 EQUAL TO 15                                  NC2074.2
121800         PERFORM PASS                                             NC2074.2
121900         GO TO SUB-WRITE-F2-1.                                    NC2074.2
122000     GO TO SUB-FAIL-F2-1.                                         NC2074.2
122100 SUB-DELETE-F2-1.                                                 NC2074.2
122200     PERFORM DE-LETE.                                             NC2074.2
122300     GO TO SUB-WRITE-F2-1.                                        NC2074.2
122400 SUB-FAIL-F2-1.                                                   NC2074.2
122500     MOVE 15 TO CORRECT-N.                                        NC2074.2
122600     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
122700     PERFORM FAIL.                                                NC2074.2
122800 SUB-WRITE-F2-1.                                                  NC2074.2
122900     PERFORM PRINT-DETAIL.                                        NC2074.2
123000*                                                                 NC2074.2
123100 SUB-INIT-F2-2.                                                   NC2074.2
123200     MOVE "SUB-TEST-F2-2  " TO PAR-NAME.                          NC2074.2
123300     MOVE "QUALIFIED SUBTRACT" TO FEATURE.                        NC2074.2
123400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
123500     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
123600 SUB-TEST-F2-2.                                                   NC2074.2
123700     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
123800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
123900         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
124000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
124100         GIVING ACCUMULATOR1.                                     NC2074.2
124200     IF ACCUMULATOR1 EQUAL TO 13                                  NC2074.2
124300         PERFORM PASS                                             NC2074.2
124400         GO TO SUB-WRITE-F2-2.                                    NC2074.2
124500     GO TO SUB-FAIL-F2-2.                                         NC2074.2
124600 SUB-DELETE-F2-2.                                                 NC2074.2
124700     PERFORM DE-LETE.                                             NC2074.2
124800     GO TO SUB-WRITE-F2-2.                                        NC2074.2
124900 SUB-FAIL-F2-2.                                                   NC2074.2
125000     MOVE 13 TO CORRECT-N.                                        NC2074.2
125100     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
125200     PERFORM FAIL.                                                NC2074.2
125300 SUB-WRITE-F2-2.                                                  NC2074.2
125400     PERFORM PRINT-DETAIL.                                        NC2074.2
125500*                                                                 NC2074.2
125600 SUB-INIT-F2-3.                                                   NC2074.2
125700     MOVE "SUB-TEST-F2-3  " TO PAR-NAME.                          NC2074.2
125800     MOVE "QUALIFIED SUBTRACT" TO FEATURE.                        NC2074.2
125900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
126000     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
126100 SUB-TEST-F2-3.                                                   NC2074.2
126200     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
126300         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
126400         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
126500         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
126600         GIVING ACCUMULATOR1.                                     NC2074.2
126700     IF ACCUMULATOR1 EQUAL TO 11                                  NC2074.2
126800         PERFORM PASS                                             NC2074.2
126900         GO TO SUB-WRITE-F2-3.                                    NC2074.2
127000     GO TO SUB-FAIL-F2-3.                                         NC2074.2
127100 SUB-DELETE-F2-3.                                                 NC2074.2
127200     PERFORM DE-LETE.                                             NC2074.2
127300     GO TO SUB-WRITE-F2-3.                                        NC2074.2
127400 SUB-FAIL-F2-3.                                                   NC2074.2
127500     MOVE 11 TO CORRECT-N.                                        NC2074.2
127600     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
127700     PERFORM FAIL.                                                NC2074.2
127800 SUB-WRITE-F2-3.                                                  NC2074.2
127900     PERFORM PRINT-DETAIL.                                        NC2074.2
128000*                                                                 NC2074.2
128100 SUB-INIT-F2-4.                                                   NC2074.2
128200     MOVE "SUB-TEST-F2-4  " TO PAR-NAME.                          NC2074.2
128300     MOVE "QUALIFIED SUBTRACT" TO FEATURE.                        NC2074.2
128400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
128500     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
128600 SUB-TEST-F2-4.                                                   NC2074.2
128700     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
128800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
128900         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
129000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
129100         GIVING ACCUMULATOR1.                                     NC2074.2
129200     IF ACCUMULATOR1 EQUAL TO 9                                   NC2074.2
129300         PERFORM PASS                                             NC2074.2
129400         GO TO SUB-WRITE-F2-4.                                    NC2074.2
129500     GO TO SUB-FAIL-F2-4.                                         NC2074.2
129600 SUB-DELETE-F2-4.                                                 NC2074.2
129700     PERFORM DE-LETE.                                             NC2074.2
129800     GO TO SUB-WRITE-F2-4.                                        NC2074.2
129900 SUB-FAIL-F2-4.                                                   NC2074.2
130000     MOVE 9 TO CORRECT-N.                                         NC2074.2
130100     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
130200     PERFORM FAIL.                                                NC2074.2
130300 SUB-WRITE-F2-4.                                                  NC2074.2
130400     PERFORM PRINT-DETAIL.                                        NC2074.2
130500*                                                                 NC2074.2
130600 SUB-INIT-F2-5.                                                   NC2074.2
130700     MOVE "QUALIFIED SUBTRACT" TO FEATURE.                        NC2074.2
130800     MOVE "SUB-TEST-F2-5  " TO PAR-NAME.                          NC2074.2
130900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
131000     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
131100 SUB-TEST-F2-5.                                                   NC2074.2
131200     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
131300         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
131400         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
131500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
131600         GIVING ACCUMULATOR1.                                     NC2074.2
131700     IF ACCUMULATOR1 EQUAL TO 7                                   NC2074.2
131800         PERFORM PASS                                             NC2074.2
131900         GO TO SUB-WRITE-F2-5.                                    NC2074.2
132000     GO TO SUB-FAIL-F2-5.                                         NC2074.2
132100 SUB-DELETE-F2-5.                                                 NC2074.2
132200     PERFORM DE-LETE.                                             NC2074.2
132300     GO TO SUB-WRITE-F2-5.                                        NC2074.2
132400 SUB-FAIL-F2-5.                                                   NC2074.2
132500     MOVE 7 TO CORRECT-N.                                         NC2074.2
132600     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
132700     PERFORM FAIL.                                                NC2074.2
132800 SUB-WRITE-F2-5.                                                  NC2074.2
132900     PERFORM PRINT-DETAIL.                                        NC2074.2
133000*                                                                 NC2074.2
133100 SUB-INIT-F2-6.                                                   NC2074.2
133200     MOVE "SUB-TEST-F2-6  " TO PAR-NAME.                          NC2074.2
133300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
133400     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
133500 SUB-TEST-F2-6.                                                   NC2074.2
133600     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
133700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
133800         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
133900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
134000         GIVING ACCUMULATOR1.                                     NC2074.2
134100     IF ACCUMULATOR1 EQUAL TO 5                                   NC2074.2
134200         PERFORM PASS                                             NC2074.2
134300         GO TO SUB-WRITE-F2-6.                                    NC2074.2
134400     GO TO SUB-FAIL-F2-6.                                         NC2074.2
134500 SUB-DELETE-F2-6.                                                 NC2074.2
134600     PERFORM DE-LETE.                                             NC2074.2
134700     GO TO SUB-WRITE-F2-6.                                        NC2074.2
134800 SUB-FAIL-F2-6.                                                   NC2074.2
134900     MOVE 5 TO CORRECT-N.                                         NC2074.2
135000     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
135100     PERFORM FAIL.                                                NC2074.2
135200 SUB-WRITE-F2-6.                                                  NC2074.2
135300     PERFORM PRINT-DETAIL.                                        NC2074.2
135400*                                                                 NC2074.2
135500 SUB-INIT-F2-7.                                                   NC2074.2
135600     MOVE "SUB-TEST-F2-7  " TO PAR-NAME.                          NC2074.2
135700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
135800     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
135900 SUB-TEST-F2-7.                                                   NC2074.2
136000     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
136100         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
136200         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
136300         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
136400         GIVING ACCUMULATOR1.                                     NC2074.2
136500     IF ACCUMULATOR1 EQUAL TO 3                                   NC2074.2
136600         PERFORM PASS                                             NC2074.2
136700         GO TO SUB-WRITE-F2-7.                                    NC2074.2
136800     GO TO SUB-FAIL-F2-7.                                         NC2074.2
136900 SUB-DELETE-F2-7.                                                 NC2074.2
137000     PERFORM DE-LETE.                                             NC2074.2
137100     GO TO SUB-WRITE-F2-7.                                        NC2074.2
137200 SUB-FAIL-F2-7.                                                   NC2074.2
137300     MOVE 3 TO CORRECT-N.                                         NC2074.2
137400     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
137500     PERFORM FAIL.                                                NC2074.2
137600 SUB-WRITE-F2-7.                                                  NC2074.2
137700     PERFORM PRINT-DETAIL.                                        NC2074.2
137800*                                                                 NC2074.2
137900 SUB-INIT-F2-8.                                                   NC2074.2
138000     MOVE "SUB-TEST-F2-8  " TO PAR-NAME.                          NC2074.2
138100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
138200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
138300 SUB-TEST-F2-8.                                                   NC2074.2
138400     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
138500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
138600         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
138700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5B       NC2074.2
138800         GIVING ACCUMULATOR1.                                     NC2074.2
138900     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
139000         PERFORM PASS                                             NC2074.2
139100         GO TO SUB-WRITE-F2-8.                                    NC2074.2
139200     GO TO SUB-FAIL-F2-8.                                         NC2074.2
139300 SUB-DELETE-F2-8.                                                 NC2074.2
139400     PERFORM DE-LETE.                                             NC2074.2
139500     GO TO SUB-WRITE-F2-8.                                        NC2074.2
139600 SUB-FAIL-F2-8.                                                   NC2074.2
139700     MOVE 1 TO CORRECT-N.                                         NC2074.2
139800     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
139900     PERFORM FAIL.                                                NC2074.2
140000 SUB-WRITE-F2-8.                                                  NC2074.2
140100     PERFORM PRINT-DETAIL.                                        NC2074.2
140200*                                                                 NC2074.2
140300 SUB-INIT-F2-9.                                                   NC2074.2
140400     MOVE "SUB-TEST-F2-9  " TO PAR-NAME.                          NC2074.2
140500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
140600     MOVE 5 TO ACCUMULATOR2.                                      NC2074.2
140700 SUB-TEST-F2-9.                                                   NC2074.2
140800     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
140900         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
141000         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
141100         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
141200         GIVING ACCUMULATOR2.                                     NC2074.2
141300     IF ACCUMULATOR2 EQUAL TO ZERO                                NC2074.2
141400         PERFORM PASS                                             NC2074.2
141500         GO TO SUB-WRITE-F2-9.                                    NC2074.2
141600     GO TO SUB-FAIL-F2-9.                                         NC2074.2
141700 SUB-DELETE-F2-9.                                                 NC2074.2
141800     PERFORM DE-LETE.                                             NC2074.2
141900     GO TO SUB-WRITE-F2-9.                                        NC2074.2
142000 SUB-FAIL-F2-9.                                                   NC2074.2
142100     MOVE ZERO TO CORRECT-N.                                      NC2074.2
142200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
142300     PERFORM FAIL.                                                NC2074.2
142400 SUB-WRITE-F2-9.                                                  NC2074.2
142500     PERFORM PRINT-DETAIL.                                        NC2074.2
142600*                                                                 NC2074.2
142700 SUB-INIT-F2-10.                                                  NC2074.2
142800     MOVE "SUB-TEST-F2-10 " TO PAR-NAME.                          NC2074.2
142900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
143000     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
143100 SUB-TEST-F2-10.                                                  NC2074.2
143200     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
143300         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
143400         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
143500         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
143600         GIVING ACCUMULATOR2.                                     NC2074.2
143700     IF ACCUMULATOR2 EQUAL TO 3                                   NC2074.2
143800         PERFORM PASS                                             NC2074.2
143900         GO TO SUB-WRITE-F2-10.                                   NC2074.2
144000     GO TO SUB-FAIL-F2-10.                                        NC2074.2
144100 SUB-DELETE-F2-10.                                                NC2074.2
144200     PERFORM DE-LETE.                                             NC2074.2
144300     GO TO SUB-WRITE-F2-10.                                       NC2074.2
144400 SUB-FAIL-F2-10.                                                  NC2074.2
144500     MOVE 3 TO CORRECT-N.                                         NC2074.2
144600     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
144700     PERFORM FAIL.                                                NC2074.2
144800 SUB-WRITE-F2-10.                                                 NC2074.2
144900     PERFORM PRINT-DETAIL.                                        NC2074.2
145000*                                                                 NC2074.2
145100 SUB-INIT-F2-11.                                                  NC2074.2
145200     MOVE "SUB-TEST-F2-11 " TO PAR-NAME.                          NC2074.2
145300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
145400     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
145500 SUB-TEST-F2-11.                                                  NC2074.2
145600     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
145700         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
145800         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
145900         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
146000         GIVING ACCUMULATOR2.                                     NC2074.2
146100     IF ACCUMULATOR2 EQUAL TO 5                                   NC2074.2
146200         PERFORM PASS                                             NC2074.2
146300         GO TO SUB-WRITE-F2-11.                                   NC2074.2
146400     GO TO SUB-FAIL-F2-11.                                        NC2074.2
146500 SUB-DELETE-F2-11.                                                NC2074.2
146600     PERFORM DE-LETE.                                             NC2074.2
146700     GO TO SUB-WRITE-F2-11.                                       NC2074.2
146800 SUB-FAIL-F2-11.                                                  NC2074.2
146900     MOVE 5 TO CORRECT-N.                                         NC2074.2
147000     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
147100     PERFORM FAIL.                                                NC2074.2
147200 SUB-WRITE-F2-11.                                                 NC2074.2
147300     PERFORM PRINT-DETAIL.                                        NC2074.2
147400*                                                                 NC2074.2
147500 SUB-INIT-F2-12.                                                  NC2074.2
147600     MOVE "SUB-TEST-F2-12 " TO PAR-NAME.                          NC2074.2
147700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
147800     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
147900 SUB-TEST-F2-12.                                                  NC2074.2
148000     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
148100         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
148200         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
148300         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
148400         GIVING ACCUMULATOR2.                                     NC2074.2
148500     IF ACCUMULATOR2 EQUAL TO 7                                   NC2074.2
148600         PERFORM PASS                                             NC2074.2
148700         GO TO SUB-WRITE-F2-12.                                   NC2074.2
148800     GO TO SUB-FAIL-F2-12.                                        NC2074.2
148900 SUB-DELETE-F2-12.                                                NC2074.2
149000     PERFORM DE-LETE.                                             NC2074.2
149100     GO TO SUB-WRITE-F2-12.                                       NC2074.2
149200 SUB-FAIL-F2-12.                                                  NC2074.2
149300     MOVE 7 TO CORRECT-N.                                         NC2074.2
149400     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
149500     PERFORM FAIL.                                                NC2074.2
149600 SUB-WRITE-F2-12.                                                 NC2074.2
149700     PERFORM PRINT-DETAIL.                                        NC2074.2
149800*                                                                 NC2074.2
149900 SUB-INIT-F2-13.                                                  NC2074.2
150000     MOVE "SUB-TEST-F2-13 " TO PAR-NAME.                          NC2074.2
150100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
150200     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
150300 SUB-TEST-F2-13.                                                  NC2074.2
150400     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
150500         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
150600         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
150700         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
150800         GIVING ACCUMULATOR2.                                     NC2074.2
150900     IF ACCUMULATOR2 EQUAL TO 9                                   NC2074.2
151000         PERFORM PASS                                             NC2074.2
151100         GO TO SUB-WRITE-F2-13.                                   NC2074.2
151200     GO TO SUB-FAIL-F2-13.                                        NC2074.2
151300 SUB-DELETE-F2-13.                                                NC2074.2
151400     PERFORM DE-LETE.                                             NC2074.2
151500     GO TO SUB-WRITE-F2-13.                                       NC2074.2
151600 SUB-FAIL-F2-13.                                                  NC2074.2
151700     MOVE 9 TO CORRECT-N.                                         NC2074.2
151800     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
151900     PERFORM FAIL.                                                NC2074.2
152000 SUB-WRITE-F2-13.                                                 NC2074.2
152100     PERFORM PRINT-DETAIL.                                        NC2074.2
152200*                                                                 NC2074.2
152300 SUB-INIT-F2-14.                                                  NC2074.2
152400     MOVE "SUB-TEST-F2-14 " TO PAR-NAME.                          NC2074.2
152500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
152600     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
152700 SUB-TEST-F2-14.                                                  NC2074.2
152800     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
152900         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
153000         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
153100         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
153200         GIVING ACCUMULATOR2.                                     NC2074.2
153300     IF ACCUMULATOR2 EQUAL TO 11                                  NC2074.2
153400         PERFORM PASS                                             NC2074.2
153500         GO TO SUB-WRITE-F2-14.                                   NC2074.2
153600     GO TO SUB-FAIL-F2-14.                                        NC2074.2
153700 SUB-DELETE-F2-14.                                                NC2074.2
153800     PERFORM DE-LETE.                                             NC2074.2
153900     GO TO SUB-WRITE-F2-14.                                       NC2074.2
154000 SUB-FAIL-F2-14.                                                  NC2074.2
154100     MOVE 11 TO CORRECT-N.                                        NC2074.2
154200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
154300     PERFORM FAIL.                                                NC2074.2
154400 SUB-WRITE-F2-14.                                                 NC2074.2
154500     PERFORM PRINT-DETAIL.                                        NC2074.2
154600*                                                                 NC2074.2
154700 SUB-INIT-F2-15.                                                  NC2074.2
154800     MOVE "SUB-TEST-F2-15 " TO PAR-NAME.                          NC2074.2
154900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
155000     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
155100 SUB-TEST-F2-15.                                                  NC2074.2
155200     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
155300         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
155400         FROM TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
155500         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
155600         GIVING ACCUMULATOR2.                                     NC2074.2
155700     IF ACCUMULATOR2 EQUAL TO 13                                  NC2074.2
155800         PERFORM PASS                                             NC2074.2
155900         GO TO SUB-WRITE-F2-15.                                   NC2074.2
156000     GO TO SUB-FAIL-F2-15.                                        NC2074.2
156100 SUB-DELETE-F2-15.                                                NC2074.2
156200     PERFORM DE-LETE.                                             NC2074.2
156300     GO TO SUB-WRITE-F2-15.                                       NC2074.2
156400 SUB-FAIL-F2-15.                                                  NC2074.2
156500     MOVE 13 TO CORRECT-N.                                        NC2074.2
156600     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
156700     PERFORM FAIL.                                                NC2074.2
156800 SUB-WRITE-F2-15.                                                 NC2074.2
156900     PERFORM PRINT-DETAIL.                                        NC2074.2
157000*                                                                 NC2074.2
157100 SUB-INIT-F2-16.                                                  NC2074.2
157200     MOVE "SUB-TEST-F2-16 " TO PAR-NAME.                          NC2074.2
157300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
157400     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
157500 SUB-TEST-F2-16.                                                  NC2074.2
157600     SUBTRACT TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
157700         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5B       NC2074.2
157800         FROM TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
157900         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
158000         GIVING ACCUMULATOR2.                                     NC2074.2
158100     IF ACCUMULATOR2 EQUAL TO 15                                  NC2074.2
158200         PERFORM PASS                                             NC2074.2
158300         GO TO SUB-WRITE-F2-16.                                   NC2074.2
158400     GO TO SUB-FAIL-F2-16.                                        NC2074.2
158500 SUB-DELETE-F2-16.                                                NC2074.2
158600     PERFORM DE-LETE.                                             NC2074.2
158700     GO TO SUB-WRITE-F2-16.                                       NC2074.2
158800 SUB-FAIL-F2-16.                                                  NC2074.2
158900     MOVE 15 TO CORRECT-N.                                        NC2074.2
159000     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
159100     PERFORM FAIL.                                                NC2074.2
159200 SUB-WRITE-F2-16.                                                 NC2074.2
159300     PERFORM PRINT-DETAIL.                                        NC2074.2
159400*                                                                 NC2074.2
159500 SUB-INIT-F3-17.                                                  NC2074.2
159600     MOVE "SUB-TEST-F3-17" TO PAR-NAME.                           NC2074.2
159700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
159800     MOVE "       CORRESPONDING" TO FEATURE.                      NC2074.2
159900     SUBTRACT CORRESPONDING TABLE-LEVEL-5A FROM TABLE-LEVEL-5C.   NC2074.2
160000 SUB-TEST-F3-17.                                                  NC2074.2
160100     IF       TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
160200         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
160300         EQUAL TO ZERO                                            NC2074.2
160400         PERFORM PASS                                             NC2074.2
160500         GO TO SUB-WRITE-F3-17.                                   NC2074.2
160600     GO TO SUB-FAIL-F3-17.                                        NC2074.2
160700 SUB-DELETE-F3-17.                                                NC2074.2
160800     PERFORM DE-LETE.                                             NC2074.2
160900     GO TO SUB-WRITE-F3-17.                                       NC2074.2
161000 SUB-FAIL-F3-17.                                                  NC2074.2
161100     MOVE 00 TO CORRECT-N.                                        NC2074.2
161200     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
161300         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
161400         TO COMPUTED-N.                                           NC2074.2
161500     PERFORM FAIL.                                                NC2074.2
161600 SUB-WRITE-F3-17.                                                 NC2074.2
161700     PERFORM PRINT-DETAIL.                                        NC2074.2
161800*                                                                 NC2074.2
161900 SUB-INIT-F3-18.                                                  NC2074.2
162000     MOVE "SUB-TEST-F3-18" TO PAR-NAME.                           NC2074.2
162100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
162200 SUB-TEST-F3-18.                                                  NC2074.2
162300     IF       TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
162400         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
162500         EQUAL TO ZERO                                            NC2074.2
162600         PERFORM PASS                                             NC2074.2
162700         GO TO SUB-WRITE-F3-18.                                   NC2074.2
162800     GO TO SUB-FAIL-F3-18.                                        NC2074.2
162900 SUB-DELETE-F3-18.                                                NC2074.2
163000     PERFORM DE-LETE.                                             NC2074.2
163100     GO TO SUB-WRITE-F3-18.                                       NC2074.2
163200 SUB-FAIL-F3-18.                                                  NC2074.2
163300     MOVE 00 TO CORRECT-N.                                        NC2074.2
163400     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
163500         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
163600         TO COMPUTED-N.                                           NC2074.2
163700     PERFORM FAIL.                                                NC2074.2
163800 SUB-WRITE-F3-18.                                                 NC2074.2
163900     PERFORM PRINT-DETAIL.                                        NC2074.2
164000*                                                                 NC2074.2
164100 SUB-INIT-F3-19.                                                  NC2074.2
164200     MOVE "SUB-TEST-F3-19" TO PAR-NAME.                           NC2074.2
164300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
164400 SUB-TEST-F3-19.                                                  NC2074.2
164500     IF       TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
164600         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
164700         EQUAL TO ZERO                                            NC2074.2
164800         PERFORM PASS                                             NC2074.2
164900         GO TO SUB-WRITE-F3-19.                                   NC2074.2
165000     GO TO SUB-FAIL-F3-19.                                        NC2074.2
165100 SUB-DELETE-F3-19.                                                NC2074.2
165200     PERFORM DE-LETE.                                             NC2074.2
165300     GO TO SUB-WRITE-F3-19.                                       NC2074.2
165400 SUB-FAIL-F3-19.                                                  NC2074.2
165500     MOVE 00 TO CORRECT-N.                                        NC2074.2
165600     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
165700         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
165800         TO COMPUTED-N.                                           NC2074.2
165900     PERFORM FAIL.                                                NC2074.2
166000 SUB-WRITE-F3-19.                                                 NC2074.2
166100     PERFORM PRINT-DETAIL.                                        NC2074.2
166200*                                                                 NC2074.2
166300 SUB-INIT-F3-20.                                                  NC2074.2
166400     MOVE "SUB-TEST-F3-20" TO PAR-NAME.                           NC2074.2
166500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
166600 SUB-TEST-F3-20.                                                  NC2074.2
166700     IF       TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
166800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
166900         EQUAL TO ZERO                                            NC2074.2
167000         PERFORM PASS                                             NC2074.2
167100         GO TO SUB-WRITE-F3-20.                                   NC2074.2
167200     GO TO SUB-FAIL-F3-20.                                        NC2074.2
167300 SUB-DELETE-F3-20.                                                NC2074.2
167400     PERFORM DE-LETE.                                             NC2074.2
167500     GO TO SUB-WRITE-F3-20.                                       NC2074.2
167600 SUB-FAIL-F3-20.                                                  NC2074.2
167700     MOVE 00 TO CORRECT-N.                                        NC2074.2
167800     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
167900         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
168000         TO COMPUTED-N.                                           NC2074.2
168100     PERFORM FAIL.                                                NC2074.2
168200 SUB-WRITE-F3-20.                                                 NC2074.2
168300     PERFORM PRINT-DETAIL.                                        NC2074.2
168400*                                                                 NC2074.2
168500 SUB-INIT-F3-21.                                                  NC2074.2
168600     MOVE "SUB-TEST-F3-21" TO PAR-NAME.                           NC2074.2
168700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
168800 SUB-TEST-F3-21.                                                  NC2074.2
168900     IF       TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
169000         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
169100         EQUAL TO ZERO                                            NC2074.2
169200         PERFORM PASS                                             NC2074.2
169300         GO TO SUB-WRITE-F3-21.                                   NC2074.2
169400     GO TO SUB-FAIL-F3-21.                                        NC2074.2
169500 SUB-DELETE-F3-21.                                                NC2074.2
169600     PERFORM DE-LETE.                                             NC2074.2
169700     GO TO SUB-WRITE-F3-21.                                       NC2074.2
169800 SUB-FAIL-F3-21.                                                  NC2074.2
169900     MOVE 00 TO CORRECT-N.                                        NC2074.2
170000     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
170100         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
170200         TO COMPUTED-N.                                           NC2074.2
170300     PERFORM FAIL.                                                NC2074.2
170400 SUB-WRITE-F3-21.                                                 NC2074.2
170500     PERFORM PRINT-DETAIL.                                        NC2074.2
170600*                                                                 NC2074.2
170700 SUB-INIT-F3-22.                                                  NC2074.2
170800     MOVE "SUB-TEST-F3-22" TO PAR-NAME.                           NC2074.2
170900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
171000 SUB-TEST-F3-22.                                                  NC2074.2
171100     IF       TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
171200         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
171300         EQUAL TO ZERO                                            NC2074.2
171400         PERFORM PASS                                             NC2074.2
171500         GO TO SUB-WRITE-F3-22.                                   NC2074.2
171600     GO TO SUB-FAIL-F3-22.                                        NC2074.2
171700 SUB-DELETE-F3-22.                                                NC2074.2
171800     PERFORM DE-LETE.                                             NC2074.2
171900     GO TO SUB-WRITE-F3-22.                                       NC2074.2
172000 SUB-FAIL-F3-22.                                                  NC2074.2
172100     MOVE 00 TO CORRECT-N.                                        NC2074.2
172200     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
172300         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
172400         TO COMPUTED-N.                                           NC2074.2
172500     PERFORM FAIL.                                                NC2074.2
172600 SUB-WRITE-F3-22.                                                 NC2074.2
172700     PERFORM PRINT-DETAIL.                                        NC2074.2
172800*                                                                 NC2074.2
172900 SUB-INIT-F3-23.                                                  NC2074.2
173000     MOVE "SUB-TEST-F3-23" TO PAR-NAME.                           NC2074.2
173100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
173200 SUB-TEST-F3-23.                                                  NC2074.2
173300     IF       TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
173400         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
173500         EQUAL TO ZERO                                            NC2074.2
173600         PERFORM PASS                                             NC2074.2
173700         GO TO SUB-WRITE-F3-23.                                   NC2074.2
173800     GO TO SUB-FAIL-F3-23.                                        NC2074.2
173900 SUB-DELETE-F3-23.                                                NC2074.2
174000     PERFORM DE-LETE.                                             NC2074.2
174100     GO TO SUB-WRITE-F3-23.                                       NC2074.2
174200 SUB-FAIL-F3-23.                                                  NC2074.2
174300     MOVE 00 TO CORRECT-N.                                        NC2074.2
174400     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
174500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
174600         TO COMPUTED-N.                                           NC2074.2
174700     PERFORM FAIL.                                                NC2074.2
174800 SUB-WRITE-F3-23.                                                 NC2074.2
174900     PERFORM PRINT-DETAIL.                                        NC2074.2
175000*                                                                 NC2074.2
175100 SUB-INIT-F3-24.                                                  NC2074.2
175200     MOVE "SUB-TEST-F3-24" TO PAR-NAME.                           NC2074.2
175300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
175400 SUB-TEST-F3-24.                                                  NC2074.2
175500     IF       TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
175600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
175700         EQUAL TO ZERO                                            NC2074.2
175800         PERFORM PASS                                             NC2074.2
175900         GO TO SUB-WRITE-F3-24.                                   NC2074.2
176000     GO TO SUB-FAIL-F3-24.                                        NC2074.2
176100 SUB-DELETE-F3-24.                                                NC2074.2
176200     PERFORM DE-LETE.                                             NC2074.2
176300     GO TO SUB-WRITE-F3-24.                                       NC2074.2
176400 SUB-FAIL-F3-24.                                                  NC2074.2
176500     MOVE 00 TO CORRECT-N.                                        NC2074.2
176600     MOVE     TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
176700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5C       NC2074.2
176800         TO COMPUTED-N.                                           NC2074.2
176900     PERFORM FAIL.                                                NC2074.2
177000 SUB-WRITE-F3-24.                                                 NC2074.2
177100     PERFORM PRINT-DETAIL.                                        NC2074.2
177200*                                                                 NC2074.2
177300 SUB-INIT-F2-25.                                                  NC2074.2
177400*    ===-->  48 LEVELS OF QUALIFICATION  <--===                   NC2074.2
177500     MOVE   "SUB-TEST-F2-25 " TO PAR-NAME.                        NC2074.2
177600     MOVE   "VI-2 1.3.2" TO ANSI-REFERENCE.                       NC2074.2
177700     MOVE    ZERO TO ACCUMULATOR1.                                NC2074.2
177800 SUB-TEST-F2-25.                                                  NC2074.2
177900     SUBTRACT GROUP-49-2  OF GROUP-48 IN GROUP-47 OF GROUP-46     NC2074.2
178000              IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42     NC2074.2
178100              IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38     NC2074.2
178200              IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34     NC2074.2
178300              IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30     NC2074.2
178400              IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26     NC2074.2
178500              IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22     NC2074.2
178600              IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18     NC2074.2
178700              IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14     NC2074.2
178800              IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10     NC2074.2
178900              IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06     NC2074.2
179000              IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02     NC2074.2
179100              IN SECOND-GROUP                                     NC2074.2
179200         FROM 200                                                 NC2074.2
179300       GIVING ACCUMULATOR1.                                       NC2074.2
179400     IF      ACCUMULATOR1 EQUAL TO ZERO                           NC2074.2
179500             PERFORM PASS                                         NC2074.2
179600             GO TO   SUB-WRITE-F2-25.                             NC2074.2
179700     GO TO SUB-FAIL-F2-25.                                        NC2074.2
179800 SUB-DELETE-F2-25.                                                NC2074.2
179900     PERFORM DE-LETE.                                             NC2074.2
180000     GO TO   SUB-WRITE-F2-25.                                     NC2074.2
180100 SUB-FAIL-F2-25.                                                  NC2074.2
180200     MOVE    200 TO CORRECT-N.                                    NC2074.2
180300     MOVE    GROUP-49-2  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
180400             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
180500             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
180600             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
180700             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
180800             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
180900             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
181000             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
181100             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
181200             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
181300             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
181400             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
181500             IN SECOND-GROUP                                      NC2074.2
181600          TO COMPUTED-N.                                          NC2074.2
181700     PERFORM FAIL.                                                NC2074.2
181800 SUB-WRITE-F2-25.                                                 NC2074.2
181900     PERFORM PRINT-DETAIL.                                        NC2074.2
182000     PERFORM END-ROUTINE.                                         NC2074.2
182100*                                                                 NC2074.2
182200 MPY-INIT-F2-1.                                                   NC2074.2
182300     MOVE "MPY-TEST-F2-1  " TO PAR-NAME.                          NC2074.2
182400     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
182500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
182600 MPY-TEST-F2-1.                                                   NC2074.2
182700     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
182800     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
182900         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
183000         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
183100         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
183200     GIVING ACCUMULATOR1.                                         NC2074.2
183300     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
183400         PERFORM PASS                                             NC2074.2
183500         GO TO MPY-WRITE-F2-1.                                    NC2074.2
183600     GO TO MPY-FAIL-F2-1.                                         NC2074.2
183700 MPY-DELETE-F2-1.                                                 NC2074.2
183800     PERFORM DE-LETE.                                             NC2074.2
183900     GO TO MPY-WRITE-F2-1.                                        NC2074.2
184000 MPY-FAIL-F2-1.                                                   NC2074.2
184100     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
184200     MOVE 1 TO CORRECT-N.                                         NC2074.2
184300     PERFORM FAIL.                                                NC2074.2
184400 MPY-WRITE-F2-1.                                                  NC2074.2
184500     PERFORM PRINT-DETAIL.                                        NC2074.2
184600*                                                                 NC2074.2
184700 MPY-INIT-F2-2.                                                   NC2074.2
184800     MOVE "MPY-TEST-F2-2  " TO PAR-NAME.                          NC2074.2
184900     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
185000     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
185100     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
185200 MPY-TEST-F2-2.                                                   NC2074.2
185300     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
185400         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
185500         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
185600         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
185700         GIVING ACCUMULATOR1.                                     NC2074.2
185800     IF ACCUMULATOR1 EQUAL TO 4                                   NC2074.2
185900         PERFORM PASS                                             NC2074.2
186000         GO TO MPY-WRITE-F2-2.                                    NC2074.2
186100     GO TO MPY-FAIL-F2-2.                                         NC2074.2
186200 MPY-DELETE-F2-2.                                                 NC2074.2
186300     PERFORM DE-LETE.                                             NC2074.2
186400     GO TO MPY-WRITE-F2-2.                                        NC2074.2
186500 MPY-FAIL-F2-2.                                                   NC2074.2
186600     MOVE 4 TO CORRECT-N.                                         NC2074.2
186700     PERFORM FAIL                                                 NC2074.2
186800     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
186900 MPY-WRITE-F2-2.                                                  NC2074.2
187000     PERFORM PRINT-DETAIL.                                        NC2074.2
187100*                                                                 NC2074.2
187200 MPY-INIT-F2-3.                                                   NC2074.2
187300     MOVE "MPY-TEST-F2-3  " TO PAR-NAME.                          NC2074.2
187400     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
187500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
187600     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
187700 MPY-TEST-F2-3.                                                   NC2074.2
187800     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
187900         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 1  NC2074.2
188000     GIVING ACCUMULATOR1.                                         NC2074.2
188100     IF ACCUMULATOR1 EQUAL TO 3                                   NC2074.2
188200         PERFORM PASS                                             NC2074.2
188300         GO TO MPY-WRITE-F2-3.                                    NC2074.2
188400     GO TO MPY-FAIL-F2-3.                                         NC2074.2
188500 MPY-DELETE-F2-3.                                                 NC2074.2
188600     PERFORM DE-LETE.                                             NC2074.2
188700     GO TO MPY-WRITE-F2-3.                                        NC2074.2
188800 MPY-FAIL-F2-3.                                                   NC2074.2
188900     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
189000     MOVE 3 TO CORRECT-N.                                         NC2074.2
189100     PERFORM FAIL.                                                NC2074.2
189200 MPY-WRITE-F2-3.                                                  NC2074.2
189300     PERFORM PRINT-DETAIL.                                        NC2074.2
189400*                                                                 NC2074.2
189500 MPY-INIT-F2-4.                                                   NC2074.2
189600     MOVE "MPY-TEST-F2-4  " TO PAR-NAME.                          NC2074.2
189700     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
189800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
189900     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
190000 MPY-TEST-F2-4.                                                   NC2074.2
190100     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
190200         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 1  NC2074.2
190300     GIVING ACCUMULATOR1.                                         NC2074.2
190400     IF ACCUMULATOR1 EQUAL TO 4                                   NC2074.2
190500         PERFORM PASS                                             NC2074.2
190600         GO TO MPY-WRITE-F2-4.                                    NC2074.2
190700     GO TO MPY-FAIL-F2-4.                                         NC2074.2
190800 MPY-DELETE-F2-4.                                                 NC2074.2
190900     PERFORM DE-LETE.                                             NC2074.2
191000     GO TO MPY-WRITE-F2-4.                                        NC2074.2
191100 MPY-FAIL-F2-4.                                                   NC2074.2
191200     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
191300     MOVE 4 TO CORRECT-N.                                         NC2074.2
191400     PERFORM FAIL.                                                NC2074.2
191500 MPY-WRITE-F2-4.                                                  NC2074.2
191600     PERFORM PRINT-DETAIL.                                        NC2074.2
191700*                                                                 NC2074.2
191800 MPY-INIT-F2-5.                                                   NC2074.2
191900     MOVE "MPY-TEST-F2-5  " TO PAR-NAME.                          NC2074.2
192000     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
192100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
192200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
192300 MPY-TEST-F2-5.                                                   NC2074.2
192400     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
192500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
192600         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
192700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
192800     GIVING ACCUMULATOR1.                                         NC2074.2
192900     IF ACCUMULATOR1 EQUAL TO 25                                  NC2074.2
193000         PERFORM PASS                                             NC2074.2
193100         GO TO MPY-WRITE-F2-5.                                    NC2074.2
193200     GO TO MPY-FAIL-F2-5.                                         NC2074.2
193300 MPY-DELETE-F2-5.                                                 NC2074.2
193400     PERFORM DE-LETE.                                             NC2074.2
193500     GO TO MPY-WRITE-F2-5.                                        NC2074.2
193600 MPY-FAIL-F2-5.                                                   NC2074.2
193700     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
193800     MOVE 25 TO CORRECT-N.                                        NC2074.2
193900     PERFORM FAIL.                                                NC2074.2
194000 MPY-WRITE-F2-5.                                                  NC2074.2
194100     PERFORM PRINT-DETAIL.                                        NC2074.2
194200*                                                                 NC2074.2
194300 MPY-INIT-F2-6.                                                   NC2074.2
194400     MOVE "MPY-TEST-F2-6  " TO PAR-NAME.                          NC2074.2
194500     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
194600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
194700     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
194800 MPY-TEST-F2-6.                                                   NC2074.2
194900     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
195000         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
195100         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
195200         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
195300     GIVING ACCUMULATOR1.                                         NC2074.2
195400     IF ACCUMULATOR1 EQUAL TO 36                                  NC2074.2
195500         PERFORM PASS                                             NC2074.2
195600         GO TO MPY-WRITE-F2-6.                                    NC2074.2
195700     GO TO MPY-FAIL-F2-6.                                         NC2074.2
195800 MPY-DELETE-F2-6.                                                 NC2074.2
195900     PERFORM DE-LETE.                                             NC2074.2
196000     GO TO MPY-WRITE-F2-6.                                        NC2074.2
196100 MPY-FAIL-F2-6.                                                   NC2074.2
196200     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
196300     MOVE 36 TO CORRECT-N.                                        NC2074.2
196400     PERFORM FAIL.                                                NC2074.2
196500 MPY-WRITE-F2-6.                                                  NC2074.2
196600     PERFORM PRINT-DETAIL.                                        NC2074.2
196700*                                                                 NC2074.2
196800 MPY-INIT-F2-7.                                                   NC2074.2
196900     MOVE "MPY-TEST-F2-7  " TO PAR-NAME.                          NC2074.2
197000     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
197100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
197200     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
197300 MPY-TEST-F2-7.                                                   NC2074.2
197400     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
197500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 1  NC2074.2
197600     GIVING ACCUMULATOR1.                                         NC2074.2
197700     IF ACCUMULATOR1 EQUAL TO 7                                   NC2074.2
197800         PERFORM PASS                                             NC2074.2
197900         GO TO MPY-WRITE-F2-7.                                    NC2074.2
198000     GO TO MPY-FAIL-F2-7.                                         NC2074.2
198100 MPY-DELETE-F2-7.                                                 NC2074.2
198200     PERFORM DE-LETE.                                             NC2074.2
198300     GO TO MPY-WRITE-F2-7.                                        NC2074.2
198400 MPY-FAIL-F2-7.                                                   NC2074.2
198500     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
198600     MOVE 7 TO CORRECT-N.                                         NC2074.2
198700     PERFORM FAIL.                                                NC2074.2
198800 MPY-WRITE-F2-7.                                                  NC2074.2
198900     PERFORM PRINT-DETAIL.                                        NC2074.2
199000*                                                                 NC2074.2
199100 MPY-INIT-F2-8.                                                   NC2074.2
199200     MOVE "MPY-TEST-F2-8  " TO PAR-NAME.                          NC2074.2
199300     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
199400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
199500     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
199600 MPY-TEST-F2-8.                                                   NC2074.2
199700     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
199800         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 1  NC2074.2
199900     GIVING ACCUMULATOR1.                                         NC2074.2
200000     IF ACCUMULATOR1 EQUAL TO 8                                   NC2074.2
200100         PERFORM PASS                                             NC2074.2
200200         GO TO MPY-WRITE-F2-8.                                    NC2074.2
200300     GO TO MPY-FAIL-F2-8.                                         NC2074.2
200400 MPY-DELETE-F2-8.                                                 NC2074.2
200500     PERFORM DE-LETE.                                             NC2074.2
200600     GO TO MPY-WRITE-F2-8.                                        NC2074.2
200700 MPY-FAIL-F2-8.                                                   NC2074.2
200800     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
200900     MOVE 8 TO CORRECT-N.                                         NC2074.2
201000     PERFORM FAIL.                                                NC2074.2
201100 MPY-WRITE-F2-8.                                                  NC2074.2
201200     PERFORM PRINT-DETAIL.                                        NC2074.2
201300*                                                                 NC2074.2
201400 MPY-INIT-F2-9.                                                   NC2074.2
201500     MOVE "MPY-TEST-F2-9  " TO PAR-NAME.                          NC2074.2
201600     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
201700     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
201800     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
201900 MPY-TEST-F2-9.                                                   NC2074.2
202000     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
202100         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 1  NC2074.2
202200     GIVING ACCUMULATOR2.                                         NC2074.2
202300     IF ACCUMULATOR2 EQUAL TO 9                                   NC2074.2
202400         PERFORM PASS                                             NC2074.2
202500         GO TO MPY-WRITE-F2-9.                                    NC2074.2
202600     GO TO MPY-FAIL-F2-9.                                         NC2074.2
202700 MPY-DELETE-F2-9.                                                 NC2074.2
202800     PERFORM DE-LETE.                                             NC2074.2
202900     GO TO MPY-WRITE-F2-9.                                        NC2074.2
203000 MPY-FAIL-F2-9.                                                   NC2074.2
203100     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
203200     MOVE 9 TO CORRECT-N.                                         NC2074.2
203300     PERFORM FAIL.                                                NC2074.2
203400 MPY-WRITE-F2-9.                                                  NC2074.2
203500     PERFORM PRINT-DETAIL.                                        NC2074.2
203600*                                                                 NC2074.2
203700 MPY-INIT-F2-10.                                                  NC2074.2
203800     MOVE "MPY-TEST-F2-10 " TO PAR-NAME.                          NC2074.2
203900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
204000     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
204100     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
204200 MPY-TEST-F2-10.                                                  NC2074.2
204300     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
204400         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 1  NC2074.2
204500     GIVING ACCUMULATOR2.                                         NC2074.2
204600     IF ACCUMULATOR2 EQUAL TO 10                                  NC2074.2
204700         PERFORM PASS                                             NC2074.2
204800         GO TO MPY-WRITE-F2-10.                                   NC2074.2
204900     GO TO MPY-FAIL-F2-10.                                        NC2074.2
205000 MPY-DELETE-F2-10.                                                NC2074.2
205100     PERFORM DE-LETE.                                             NC2074.2
205200     GO TO MPY-WRITE-F2-10.                                       NC2074.2
205300 MPY-FAIL-F2-10.                                                  NC2074.2
205400     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
205500     MOVE 10 TO CORRECT-N.                                        NC2074.2
205600     PERFORM FAIL.                                                NC2074.2
205700 MPY-WRITE-F2-10.                                                 NC2074.2
205800     PERFORM PRINT-DETAIL.                                        NC2074.2
205900*                                                                 NC2074.2
206000 MPY-INIT-F2-11.                                                  NC2074.2
206100     MOVE "MPY-TEST-F2-11 " TO PAR-NAME.                          NC2074.2
206200     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
206300     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
206400     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
206500 MPY-TEST-F2-11.                                                  NC2074.2
206600     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
206700         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
206800         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
206900         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
207000     GIVING ACCUMULATOR2.                                         NC2074.2
207100     IF ACCUMULATOR2 EQUAL TO 121                                 NC2074.2
207200         PERFORM PASS                                             NC2074.2
207300         GO TO MPY-WRITE-F2-11.                                   NC2074.2
207400     GO TO MPY-FAIL-F2-11.                                        NC2074.2
207500 MPY-DELETE-F2-11.                                                NC2074.2
207600     PERFORM DE-LETE.                                             NC2074.2
207700     GO TO MPY-WRITE-F2-11.                                       NC2074.2
207800 MPY-FAIL-F2-11.                                                  NC2074.2
207900     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
208000     MOVE 121 TO CORRECT-N.                                       NC2074.2
208100     PERFORM FAIL.                                                NC2074.2
208200 MPY-WRITE-F2-11.                                                 NC2074.2
208300     PERFORM PRINT-DETAIL.                                        NC2074.2
208400*                                                                 NC2074.2
208500 MPY-INIT-F2-12.                                                  NC2074.2
208600     MOVE "MPY-TEST-F2-12 " TO PAR-NAME.                          NC2074.2
208700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
208800     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
208900     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
209000 MPY-TEST-F2-12.                                                  NC2074.2
209100     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
209200         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
209300         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
209400         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
209500     GIVING ACCUMULATOR2.                                         NC2074.2
209600     IF ACCUMULATOR2 EQUAL TO 144                                 NC2074.2
209700         PERFORM PASS                                             NC2074.2
209800         GO TO MPY-WRITE-F2-12.                                   NC2074.2
209900     GO TO MPY-FAIL-F2-12.                                        NC2074.2
210000 MPY-DELETE-F2-12.                                                NC2074.2
210100     PERFORM DE-LETE.                                             NC2074.2
210200     GO TO MPY-WRITE-F2-12.                                       NC2074.2
210300 MPY-FAIL-F2-12.                                                  NC2074.2
210400     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
210500     MOVE 144 TO CORRECT-N.                                       NC2074.2
210600     PERFORM FAIL.                                                NC2074.2
210700 MPY-WRITE-F2-12.                                                 NC2074.2
210800     PERFORM PRINT-DETAIL.                                        NC2074.2
210900*                                                                 NC2074.2
211000 MPY-INIT-F2-13.                                                  NC2074.2
211100     MOVE "MPY-TEST-F2-13 " TO PAR-NAME.                          NC2074.2
211200     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
211300     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
211400     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
211500 MPY-TEST-F2-13.                                                  NC2074.2
211600     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
211700         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 1  NC2074.2
211800     GIVING ACCUMULATOR2.                                         NC2074.2
211900     IF ACCUMULATOR2 EQUAL TO 13                                  NC2074.2
212000         PERFORM PASS                                             NC2074.2
212100         GO TO MPY-WRITE-F2-13.                                   NC2074.2
212200     GO TO MPY-FAIL-F2-13.                                        NC2074.2
212300 MPY-DELETE-F2-13.                                                NC2074.2
212400     PERFORM DE-LETE.                                             NC2074.2
212500     GO TO MPY-WRITE-F2-13.                                       NC2074.2
212600 MPY-FAIL-F2-13.                                                  NC2074.2
212700     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
212800     MOVE 13 TO CORRECT-N.                                        NC2074.2
212900     PERFORM FAIL.                                                NC2074.2
213000 MPY-WRITE-F2-13.                                                 NC2074.2
213100     PERFORM PRINT-DETAIL.                                        NC2074.2
213200*                                                                 NC2074.2
213300 MPY-INIT-F2-14.                                                  NC2074.2
213400     MOVE "MPY-TEST-F2-14 " TO PAR-NAME.                          NC2074.2
213500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
213600     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
213700     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
213800 MPY-TEST-F2-14.                                                  NC2074.2
213900     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
214000         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 1  NC2074.2
214100     GIVING ACCUMULATOR2.                                         NC2074.2
214200     IF ACCUMULATOR2 EQUAL TO 14                                  NC2074.2
214300         PERFORM PASS                                             NC2074.2
214400         GO TO MPY-WRITE-F2-14.                                   NC2074.2
214500     GO TO MPY-FAIL-F2-14.                                        NC2074.2
214600 MPY-DELETE-F2-14.                                                NC2074.2
214700     PERFORM DE-LETE.                                             NC2074.2
214800     GO TO MPY-WRITE-F2-14.                                       NC2074.2
214900 MPY-FAIL-F2-14.                                                  NC2074.2
215000     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
215100     MOVE 14 TO CORRECT-N.                                        NC2074.2
215200     PERFORM FAIL.                                                NC2074.2
215300 MPY-WRITE-F2-14.                                                 NC2074.2
215400     PERFORM PRINT-DETAIL.                                        NC2074.2
215500*                                                                 NC2074.2
215600 MPY-INIT-F2-15.                                                  NC2074.2
215700     MOVE "MPY-TEST-F2-15 " TO PAR-NAME.                          NC2074.2
215800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
215900     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
216000     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
216100 MPY-TEST-F2-15.                                                  NC2074.2
216200     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
216300         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
216400         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
216500         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
216600     GIVING ACCUMULATOR2.                                         NC2074.2
216700     IF ACCUMULATOR2 EQUAL TO 225                                 NC2074.2
216800         PERFORM PASS                                             NC2074.2
216900         GO TO MPY-WRITE-F2-15.                                   NC2074.2
217000     GO TO MPY-FAIL-F2-15.                                        NC2074.2
217100 MPY-DELETE-F2-15.                                                NC2074.2
217200     PERFORM DE-LETE.                                             NC2074.2
217300     GO TO MPY-WRITE-F2-15.                                       NC2074.2
217400 MPY-FAIL-F2-15.                                                  NC2074.2
217500     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
217600     MOVE 225 TO CORRECT-N.                                       NC2074.2
217700     PERFORM FAIL.                                                NC2074.2
217800 MPY-WRITE-F2-15.                                                 NC2074.2
217900     PERFORM PRINT-DETAIL.                                        NC2074.2
218000*                                                                 NC2074.2
218100 MPY-INIT-F2-16.                                                  NC2074.2
218200     MOVE "MPY-TEST-F2-16 " TO PAR-NAME.                          NC2074.2
218300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
218400     MOVE "QUALIFIED MULTIPLY" TO FEATURE.                        NC2074.2
218500     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
218600 MPY-TEST-F2-16.                                                  NC2074.2
218700     MULTIPLY TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
218800         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
218900         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
219000         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
219100     GIVING ACCUMULATOR2.                                         NC2074.2
219200     IF ACCUMULATOR2 EQUAL TO 256                                 NC2074.2
219300         PERFORM PASS                                             NC2074.2
219400         GO TO MPY-WRITE-F2-16.                                   NC2074.2
219500     GO TO MPY-FAIL-F2-16.                                        NC2074.2
219600 MPY-DELETE-F2-16.                                                NC2074.2
219700     PERFORM DE-LETE.                                             NC2074.2
219800     GO TO MPY-WRITE-F2-16.                                       NC2074.2
219900 MPY-FAIL-F2-16.                                                  NC2074.2
220000     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
220100     MOVE 256 TO CORRECT-N.                                       NC2074.2
220200     PERFORM FAIL.                                                NC2074.2
220300 MPY-WRITE-F2-16.                                                 NC2074.2
220400     PERFORM PRINT-DETAIL.                                        NC2074.2
220500*                                                                 NC2074.2
220600 MPY-INIT-F2-17.                                                  NC2074.2
220700*    ===-->  48 LEVELS OF QUALIFICATION  <--===                   NC2074.2
220800     MOVE   "MPY-TEST-F2-17 " TO PAR-NAME.                        NC2074.2
220900     MOVE   "VI-2 1.3.2" TO ANSI-REFERENCE.                       NC2074.2
221000     MOVE    ZERO TO ACCUMULATOR1.                                NC2074.2
221100 MPY-TEST-F2-17.                                                  NC2074.2
221200     MULTIPLY GROUP-49-3  OF GROUP-48 IN GROUP-47 OF GROUP-46     NC2074.2
221300              IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42     NC2074.2
221400              IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38     NC2074.2
221500              IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34     NC2074.2
221600              IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30     NC2074.2
221700              IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26     NC2074.2
221800              IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22     NC2074.2
221900              IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18     NC2074.2
222000              IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14     NC2074.2
222100              IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10     NC2074.2
222200              IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06     NC2074.2
222300              IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02     NC2074.2
222400              IN FIRST-GROUP                                      NC2074.2
222500           BY 3                                                   NC2074.2
222600       GIVING ACCUMULATOR1.                                       NC2074.2
222700     IF      ACCUMULATOR1 EQUAL TO 9                              NC2074.2
222800             PERFORM PASS                                         NC2074.2
222900             GO TO   MPY-WRITE-F2-17.                             NC2074.2
223000     GO TO MPY-FAIL-F2-17.                                        NC2074.2
223100 MPY-DELETE-F2-17.                                                NC2074.2
223200     PERFORM DE-LETE.                                             NC2074.2
223300     GO TO   MPY-WRITE-F2-17.                                     NC2074.2
223400 MPY-FAIL-F2-17.                                                  NC2074.2
223500     MOVE    9 TO CORRECT-N.                                      NC2074.2
223600     MOVE    GROUP-49-3  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
223700             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
223800             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
223900             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
224000             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
224100             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
224200             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
224300             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
224400             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
224500             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
224600             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
224700             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
224800             IN FIRST-GROUP                                       NC2074.2
224900          TO COMPUTED-N.                                          NC2074.2
225000     MOVE   "3 TIMES 3 SHOULD BE 9" TO RE-MARK.                   NC2074.2
225100     PERFORM FAIL.                                                NC2074.2
225200 MPY-WRITE-F2-17.                                                 NC2074.2
225300     PERFORM PRINT-DETAIL.                                        NC2074.2
225400     PERFORM END-ROUTINE.                                         NC2074.2
225500*                                                                 NC2074.2
225600 DIV-INIT-F3-1.                                                   NC2074.2
225700     MOVE "DIV-TEST-F3-1  " TO PAR-NAME                           NC2074.2
225800     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
225900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
226000     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
226100 DIV-TEST-F3-1.                                                   NC2074.2
226200     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
226300         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
226400         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
226500         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
226600     GIVING ACCUMULATOR1.                                         NC2074.2
226700     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
226800         PERFORM PASS                                             NC2074.2
226900         GO TO DIV-WRITE-F3-1.                                    NC2074.2
227000     GO TO DIV-FAIL-F3-1.                                         NC2074.2
227100 DIV-DELETE-F3-1.                                                 NC2074.2
227200     PERFORM DE-LETE.                                             NC2074.2
227300     GO TO DIV-WRITE-F3-1.                                        NC2074.2
227400 DIV-FAIL-F3-1.                                                   NC2074.2
227500     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
227600     MOVE 1 TO CORRECT-N.                                         NC2074.2
227700     PERFORM FAIL.                                                NC2074.2
227800 DIV-WRITE-F3-1.                                                  NC2074.2
227900     PERFORM PRINT-DETAIL.                                        NC2074.2
228000*                                                                 NC2074.2
228100 DIV-INIT-F3-2.                                                   NC2074.2
228200     MOVE "DIV-TEST-F3-2  " TO PAR-NAME                           NC2074.2
228300     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
228400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
228500     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
228600 DIV-TEST-F3-2.                                                   NC2074.2
228700     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
228800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
228900         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
229000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
229100     GIVING ACCUMULATOR1.                                         NC2074.2
229200     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
229300         PERFORM PASS                                             NC2074.2
229400         GO TO DIV-WRITE-F3-2.                                    NC2074.2
229500     GO TO DIV-FAIL-F3-2.                                         NC2074.2
229600 DIV-DELETE-F3-2.                                                 NC2074.2
229700     PERFORM DE-LETE.                                             NC2074.2
229800     GO TO DIV-WRITE-F3-2.                                        NC2074.2
229900 DIV-FAIL-F3-2.                                                   NC2074.2
230000     MOVE 1 TO CORRECT-N.                                         NC2074.2
230100     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
230200     PERFORM FAIL.                                                NC2074.2
230300 DIV-WRITE-F3-2.                                                  NC2074.2
230400     PERFORM PRINT-DETAIL.                                        NC2074.2
230500*                                                                 NC2074.2
230600 DIV-INIT-F3-3.                                                   NC2074.2
230700     MOVE "DIV-TEST-F3-3  " TO PAR-NAME                           NC2074.2
230800     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
230900     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
231000     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
231100 DIV-TEST-F3-3.                                                   NC2074.2
231200     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
231300         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 3  NC2074.2
231400     GIVING ACCUMULATOR1.                                         NC2074.2
231500     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
231600         PERFORM PASS                                             NC2074.2
231700         GO TO DIV-WRITE-F3-3.                                    NC2074.2
231800     GO TO DIV-FAIL-F3-3.                                         NC2074.2
231900 DIV-DELETE-F3-3.                                                 NC2074.2
232000     PERFORM DE-LETE.                                             NC2074.2
232100     GO TO DIV-WRITE-F3-3.                                        NC2074.2
232200 DIV-FAIL-F3-3.                                                   NC2074.2
232300     MOVE 1 TO CORRECT-N.                                         NC2074.2
232400     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
232500     PERFORM FAIL.                                                NC2074.2
232600 DIV-WRITE-F3-3.                                                  NC2074.2
232700     PERFORM PRINT-DETAIL.                                        NC2074.2
232800*                                                                 NC2074.2
232900 DIV-INIT-F3-4.                                                   NC2074.2
233000     MOVE "DIV-TEST-F3-4  " TO PAR-NAME                           NC2074.2
233100     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
233200     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
233300     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
233400 DIV-TEST-F3-4.                                                   NC2074.2
233500     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
233600         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 3  NC2074.2
233700     GIVING ACCUMULATOR1.                                         NC2074.2
233800     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
233900         PERFORM PASS                                             NC2074.2
234000         GO TO DIV-WRITE-F3-4.                                    NC2074.2
234100     GO TO DIV-FAIL-F3-4.                                         NC2074.2
234200 DIV-DELETE-F3-4.                                                 NC2074.2
234300     PERFORM DE-LETE.                                             NC2074.2
234400     GO TO DIV-WRITE-F3-4.                                        NC2074.2
234500 DIV-FAIL-F3-4.                                                   NC2074.2
234600     MOVE 1 TO CORRECT-N.                                         NC2074.2
234700     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
234800     PERFORM FAIL.                                                NC2074.2
234900 DIV-WRITE-F3-4.                                                  NC2074.2
235000     PERFORM PRINT-DETAIL.                                        NC2074.2
235100*                                                                 NC2074.2
235200 DIV-INIT-F3-5.                                                   NC2074.2
235300     MOVE "DIV-TEST-F3-5  " TO PAR-NAME                           NC2074.2
235400     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
235500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
235600     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
235700 DIV-TEST-F3-5.                                                   NC2074.2
235800     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
235900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
236000         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
236100         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
236200     GIVING ACCUMULATOR1.                                         NC2074.2
236300     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
236400         PERFORM PASS                                             NC2074.2
236500         GO TO DIV-WRITE-F3-5.                                    NC2074.2
236600     GO TO DIV-FAIL-F3-5.                                         NC2074.2
236700 DIV-DELETE-F3-5.                                                 NC2074.2
236800     PERFORM DE-LETE.                                             NC2074.2
236900     GO TO DIV-WRITE-F3-5.                                        NC2074.2
237000 DIV-FAIL-F3-5.                                                   NC2074.2
237100     MOVE 1 TO CORRECT-N.                                         NC2074.2
237200     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
237300     PERFORM FAIL.                                                NC2074.2
237400 DIV-WRITE-F3-5.                                                  NC2074.2
237500     PERFORM PRINT-DETAIL.                                        NC2074.2
237600*                                                                 NC2074.2
237700 DIV-INIT-F3-6.                                                   NC2074.2
237800     MOVE "DIV-TEST-F3-6  " TO PAR-NAME                           NC2074.2
237900     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
238000     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
238100     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
238200 DIV-TEST-F3-6.                                                   NC2074.2
238300     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
238400         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
238500         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
238600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A       NC2074.2
238700     GIVING ACCUMULATOR1.                                         NC2074.2
238800     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
238900         PERFORM PASS                                             NC2074.2
239000         GO TO DIV-WRITE-F3-6.                                    NC2074.2
239100     GO TO DIV-FAIL-F3-6.                                         NC2074.2
239200 DIV-DELETE-F3-6.                                                 NC2074.2
239300     PERFORM DE-LETE.                                             NC2074.2
239400     GO TO DIV-WRITE-F3-6.                                        NC2074.2
239500 DIV-FAIL-F3-6.                                                   NC2074.2
239600     MOVE 1 TO CORRECT-N.                                         NC2074.2
239700     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
239800     PERFORM FAIL.                                                NC2074.2
239900 DIV-WRITE-F3-6.                                                  NC2074.2
240000     PERFORM PRINT-DETAIL.                                        NC2074.2
240100*                                                                 NC2074.2
240200 DIV-INIT-F3-7.                                                   NC2074.2
240300     MOVE "DIV-TEST-F3-7  " TO PAR-NAME                           NC2074.2
240400     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
240500     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
240600     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
240700 DIV-TEST-F3-7.                                                   NC2074.2
240800     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
240900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 7  NC2074.2
241000     GIVING ACCUMULATOR1.                                         NC2074.2
241100     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
241200         PERFORM PASS                                             NC2074.2
241300         GO TO DIV-WRITE-F3-7.                                    NC2074.2
241400     GO TO DIV-FAIL-F3-7.                                         NC2074.2
241500 DIV-DELETE-F3-7.                                                 NC2074.2
241600     PERFORM DE-LETE.                                             NC2074.2
241700     GO TO DIV-WRITE-F3-7.                                        NC2074.2
241800 DIV-FAIL-F3-7.                                                   NC2074.2
241900     MOVE 1 TO CORRECT-N.                                         NC2074.2
242000     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
242100     PERFORM FAIL.                                                NC2074.2
242200 DIV-WRITE-F3-7.                                                  NC2074.2
242300     PERFORM PRINT-DETAIL.                                        NC2074.2
242400*                                                                 NC2074.2
242500 DIV-INIT-F3-8.                                                   NC2074.2
242600     MOVE "DIV-TEST-F3-8  " TO PAR-NAME                           NC2074.2
242700     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
242800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
242900     MOVE ZERO TO ACCUMULATOR1.                                   NC2074.2
243000 DIV-TEST-F3-8.                                                   NC2074.2
243100     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
243200         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A BY 8  NC2074.2
243300     GIVING ACCUMULATOR1.                                         NC2074.2
243400     IF ACCUMULATOR1 EQUAL TO 1                                   NC2074.2
243500         PERFORM PASS                                             NC2074.2
243600         GO TO DIV-WRITE-F3-8.                                    NC2074.2
243700     GO TO DIV-FAIL-F3-8.                                         NC2074.2
243800 DIV-DELETE-F3-8.                                                 NC2074.2
243900     PERFORM DE-LETE.                                             NC2074.2
244000     GO TO DIV-WRITE-F3-8.                                        NC2074.2
244100 DIV-FAIL-F3-8.                                                   NC2074.2
244200     MOVE 1 TO CORRECT-N.                                         NC2074.2
244300     MOVE ACCUMULATOR1 TO COMPUTED-N.                             NC2074.2
244400     PERFORM FAIL.                                                NC2074.2
244500 DIV-WRITE-F3-8.                                                  NC2074.2
244600     PERFORM PRINT-DETAIL.                                        NC2074.2
244700*                                                                 NC2074.2
244800 DIV-INIT-F3-9.                                                   NC2074.2
244900     MOVE "DIV-TEST-F3-9  " TO PAR-NAME                           NC2074.2
245000     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
245100     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
245200     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
245300 DIV-TEST-F3-9.                                                   NC2074.2
245400     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
245500         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 9  NC2074.2
245600     GIVING ACCUMULATOR2.                                         NC2074.2
245700     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
245800         PERFORM PASS                                             NC2074.2
245900         GO TO DIV-WRITE-F3-9.                                    NC2074.2
246000     GO TO DIV-FAIL-F3-9.                                         NC2074.2
246100 DIV-DELETE-F3-9.                                                 NC2074.2
246200     PERFORM DE-LETE.                                             NC2074.2
246300     GO TO DIV-WRITE-F3-9.                                        NC2074.2
246400 DIV-FAIL-F3-9.                                                   NC2074.2
246500     MOVE 1 TO CORRECT-N.                                         NC2074.2
246600     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
246700     PERFORM FAIL.                                                NC2074.2
246800 DIV-WRITE-F3-9.                                                  NC2074.2
246900     PERFORM PRINT-DETAIL.                                        NC2074.2
247000*                                                                 NC2074.2
247100 DIV-INIT-F3-10.                                                  NC2074.2
247200     MOVE "DIV-TEST-F3-10 " TO PAR-NAME                           NC2074.2
247300     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
247400     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
247500     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
247600 DIV-TEST-F3-10.                                                  NC2074.2
247700     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
247800         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 10 NC2074.2
247900     GIVING ACCUMULATOR2.                                         NC2074.2
248000     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
248100         PERFORM PASS                                             NC2074.2
248200         GO TO DIV-WRITE-F3-10.                                   NC2074.2
248300     GO TO DIV-FAIL-F3-10.                                        NC2074.2
248400 DIV-DELETE-F3-10.                                                NC2074.2
248500     PERFORM DE-LETE.                                             NC2074.2
248600     GO TO DIV-WRITE-F3-10.                                       NC2074.2
248700 DIV-FAIL-F3-10.                                                  NC2074.2
248800     MOVE 1 TO CORRECT-N.                                         NC2074.2
248900     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
249000     PERFORM FAIL.                                                NC2074.2
249100 DIV-WRITE-F3-10.                                                 NC2074.2
249200     PERFORM PRINT-DETAIL.                                        NC2074.2
249300*                                                                 NC2074.2
249400 DIV-INIT-F3-11.                                                  NC2074.2
249500     MOVE "DIV-TEST-F3-11 " TO PAR-NAME                           NC2074.2
249600     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
249700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
249800     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
249900 DIV-TEST-F3-11.                                                  NC2074.2
250000     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
250100         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
250200         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
250300         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
250400     GIVING ACCUMULATOR2.                                         NC2074.2
250500     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
250600         PERFORM PASS                                             NC2074.2
250700         GO TO DIV-WRITE-F3-11.                                   NC2074.2
250800     GO TO DIV-FAIL-F3-11.                                        NC2074.2
250900 DIV-DELETE-F3-11.                                                NC2074.2
251000     PERFORM DE-LETE.                                             NC2074.2
251100     GO TO DIV-WRITE-F3-11.                                       NC2074.2
251200 DIV-FAIL-F3-11.                                                  NC2074.2
251300     MOVE 1 TO CORRECT-N.                                         NC2074.2
251400     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
251500     PERFORM FAIL.                                                NC2074.2
251600 DIV-WRITE-F3-11.                                                 NC2074.2
251700     PERFORM PRINT-DETAIL.                                        NC2074.2
251800*                                                                 NC2074.2
251900 DIV-INIT-F3-12.                                                  NC2074.2
252000     MOVE "DIV-TEST-F3-12 " TO PAR-NAME                           NC2074.2
252100     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
252200     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
252300     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
252400 DIV-TEST-F3-12.                                                  NC2074.2
252500     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
252600         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
252700         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
252800         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
252900     GIVING ACCUMULATOR2.                                         NC2074.2
253000     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
253100         PERFORM PASS                                             NC2074.2
253200         GO TO DIV-WRITE-F3-12.                                   NC2074.2
253300     GO TO DIV-FAIL-F3-12.                                        NC2074.2
253400 DIV-DELETE-F3-12.                                                NC2074.2
253500     PERFORM DE-LETE.                                             NC2074.2
253600     GO TO DIV-WRITE-F3-12.                                       NC2074.2
253700 DIV-FAIL-F3-12.                                                  NC2074.2
253800     MOVE 1 TO CORRECT-N.                                         NC2074.2
253900     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
254000     PERFORM FAIL.                                                NC2074.2
254100 DIV-WRITE-F3-12.                                                 NC2074.2
254200     PERFORM PRINT-DETAIL.                                        NC2074.2
254300*                                                                 NC2074.2
254400 DIV-INIT-F3-13.                                                  NC2074.2
254500     MOVE "DIV-TEST-F3-13 " TO PAR-NAME                           NC2074.2
254600     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
254700     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
254800     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
254900 DIV-TEST-F3-13.                                                  NC2074.2
255000     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF   NC2074.2
255100         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 13 NC2074.2
255200     GIVING ACCUMULATOR2.                                         NC2074.2
255300     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
255400         PERFORM PASS                                             NC2074.2
255500         GO TO DIV-WRITE-F3-13.                                   NC2074.2
255600     GO TO DIV-FAIL-F3-13.                                        NC2074.2
255700 DIV-DELETE-F3-13.                                                NC2074.2
255800     PERFORM DE-LETE.                                             NC2074.2
255900     GO TO DIV-WRITE-F3-13.                                       NC2074.2
256000 DIV-FAIL-F3-13.                                                  NC2074.2
256100     MOVE 1 TO CORRECT-N.                                         NC2074.2
256200     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
256300     PERFORM FAIL.                                                NC2074.2
256400 DIV-WRITE-F3-13.                                                 NC2074.2
256500     PERFORM PRINT-DETAIL.                                        NC2074.2
256600*                                                                 NC2074.2
256700 DIV-INIT-F3-14.                                                  NC2074.2
256800     MOVE "DIV-TEST-F3-14 " TO PAR-NAME                           NC2074.2
256900     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
257000     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
257100     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
257200 DIV-TEST-F3-14.                                                  NC2074.2
257300     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2A OF   NC2074.2
257400         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A BY 14 NC2074.2
257500     GIVING ACCUMULATOR2.                                         NC2074.2
257600     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
257700         PERFORM PASS                                             NC2074.2
257800         GO TO DIV-WRITE-F3-14.                                   NC2074.2
257900     GO TO DIV-FAIL-F3-14.                                        NC2074.2
258000 DIV-DELETE-F3-14.                                                NC2074.2
258100     PERFORM DE-LETE.                                             NC2074.2
258200     GO TO DIV-WRITE-F3-14.                                       NC2074.2
258300 DIV-FAIL-F3-14.                                                  NC2074.2
258400     MOVE 1 TO CORRECT-N.                                         NC2074.2
258500     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
258600     PERFORM FAIL.                                                NC2074.2
258700 DIV-WRITE-F3-14.                                                 NC2074.2
258800     PERFORM PRINT-DETAIL.                                        NC2074.2
258900*                                                                 NC2074.2
259000 DIV-INIT-F3-15.                                                  NC2074.2
259100     MOVE "DIV-TEST-F3-15 " TO PAR-NAME                           NC2074.2
259200     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
259300     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
259400     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
259500 DIV-TEST-F3-15.                                                  NC2074.2
259600     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
259700         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
259800         BY   TBL-ITEM-1 OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B OF   NC2074.2
259900         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
260000     GIVING ACCUMULATOR2.                                         NC2074.2
260100     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
260200         PERFORM PASS                                             NC2074.2
260300         GO TO DIV-WRITE-F3-15.                                   NC2074.2
260400     GO TO DIV-FAIL-F3-15.                                        NC2074.2
260500 DIV-DELETE-F3-15.                                                NC2074.2
260600     PERFORM DE-LETE.                                             NC2074.2
260700     GO TO DIV-WRITE-F3-15.                                       NC2074.2
260800 DIV-FAIL-F3-15.                                                  NC2074.2
260900     MOVE 1 TO CORRECT-N.                                         NC2074.2
261000     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
261100     PERFORM FAIL.                                                NC2074.2
261200 DIV-WRITE-F3-15.                                                 NC2074.2
261300     PERFORM PRINT-DETAIL.                                        NC2074.2
261400*                                                                 NC2074.2
261500 DIV-INIT-F3-16.                                                  NC2074.2
261600     MOVE "DIV-TEST-F3-16 " TO PAR-NAME                           NC2074.2
261700     MOVE "QUALIFIED DIVIDE " TO  FEATURE.                        NC2074.2
261800     MOVE "IV-18 4.3.8.1" TO ANSI-REFERENCE.                      NC2074.2
261900     MOVE ZERO TO ACCUMULATOR2.                                   NC2074.2
262000 DIV-TEST-F3-16.                                                  NC2074.2
262100     DIVIDE   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
262200         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
262300         BY   TBL-ITEM-1 OF TABLE-LEVEL-1B IN TABLE-LEVEL-2B OF   NC2074.2
262400         TABLE-LEVEL-3B IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2074.2
262500     GIVING ACCUMULATOR2.                                         NC2074.2
262600     IF ACCUMULATOR2 EQUAL TO 1                                   NC2074.2
262700         PERFORM PASS                                             NC2074.2
262800         GO TO DIV-WRITE-F3-16.                                   NC2074.2
262900     GO TO DIV-FAIL-F3-16.                                        NC2074.2
263000 DIV-DELETE-F3-16.                                                NC2074.2
263100     PERFORM DE-LETE.                                             NC2074.2
263200     GO TO DIV-WRITE-F3-16.                                       NC2074.2
263300 DIV-FAIL-F3-16.                                                  NC2074.2
263400     MOVE 1 TO CORRECT-N.                                         NC2074.2
263500     MOVE ACCUMULATOR2 TO COMPUTED-N.                             NC2074.2
263600     PERFORM FAIL.                                                NC2074.2
263700 DIV-WRITE-F3-16.                                                 NC2074.2
263800     PERFORM PRINT-DETAIL.                                        NC2074.2
263900*                                                                 NC2074.2
264000 DIV-INIT-F3-17.                                                  NC2074.2
264100*    ===-->  48 LEVELS OF QUALIFICATION  <--===                   NC2074.2
264200     MOVE   "DIV-TEST-F3-17 " TO PAR-NAME.                        NC2074.2
264300     MOVE   "VI-2 1.3.2" TO ANSI-REFERENCE.                       NC2074.2
264400     MOVE    ZERO TO ACCUMULATOR1.                                NC2074.2
264500 DIV-TEST-F3-17.                                                  NC2074.2
264600     DIVIDE  GROUP-49-4  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
264700             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
264800             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
264900             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
265000             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
265100             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
265200             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
265300             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
265400             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
265500             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
265600             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
265700             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
265800             IN SECOND-GROUP                                      NC2074.2
265900          BY 40                                                   NC2074.2
266000       GIVING ACCUMULATOR1.                                       NC2074.2
266100     IF      ACCUMULATOR1 EQUAL TO 10                             NC2074.2
266200             PERFORM PASS                                         NC2074.2
266300             GO TO   DIV-WRITE-F3-17.                             NC2074.2
266400     GO TO DIV-FAIL-F3-17.                                        NC2074.2
266500 DIV-DELETE-F3-17.                                                NC2074.2
266600     PERFORM DE-LETE.                                             NC2074.2
266700     GO TO   DIV-WRITE-F3-17.                                     NC2074.2
266800 DIV-FAIL-F3-17.                                                  NC2074.2
266900     MOVE    10 TO CORRECT-N.                                     NC2074.2
267000     MOVE    GROUP-49-4  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
267100             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
267200             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
267300             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
267400             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
267500             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
267600             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
267700             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
267800             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
267900             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
268000             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
268100             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
268200             IN SECOND-GROUP                                      NC2074.2
268300          TO COMPUTED-N.                                          NC2074.2
268400     PERFORM FAIL.                                                NC2074.2
268500 DIV-WRITE-F3-17.                                                 NC2074.2
268600     PERFORM PRINT-DETAIL.                                        NC2074.2
268700     PERFORM END-ROUTINE.                                         NC2074.2
268800*                                                                 NC2074.2
268900 CND-INIT-GF-1.                                                   NC2074.2
269000*    ===-->  48 LEVELS OF QUALIFICATION  <--===                   NC2074.2
269100     MOVE   "CND-TEST-GF-1 " TO PAR-NAME.                         NC2074.2
269200     MOVE   "VI-2 1.3.2" TO ANSI-REFERENCE.                       NC2074.2
269300     MOVE   "CONDITION NAME " TO  FEATURE.                        NC2074.2
269400 CND-TEST-GF-1.                                                   NC2074.2
269500     IF      LEVEL-49-OK                                          NC2074.2
269600             PERFORM PASS                                         NC2074.2
269700             GO TO   CND-WRITE-GF-1.                              NC2074.2
269800     GO TO CND-FAIL-GF-1.                                         NC2074.2
269900 CND-DELETE-GF-1.                                                 NC2074.2
270000     PERFORM DE-LETE.                                             NC2074.2
270100     GO TO   CND-WRITE-GF-1.                                      NC2074.2
270200 CND-FAIL-GF-1.                                                   NC2074.2
270300     MOVE    500 TO CORRECT-N.                                    NC2074.2
270400     MOVE    GROUP-49-5  OF GROUP-48 IN GROUP-47 OF GROUP-46      NC2074.2
270500             IN GROUP-45 OF GROUP-44 IN GROUP-43 OF GROUP-42      NC2074.2
270600             IN GROUP-41 OF GROUP-40 IN GROUP-39 OF GROUP-38      NC2074.2
270700             IN GROUP-37 OF GROUP-36 IN GROUP-35 OF GROUP-34      NC2074.2
270800             IN GROUP-33 OF GROUP-32 IN GROUP-31 OF GROUP-30      NC2074.2
270900             IN GROUP-29 OF GROUP-28 IN GROUP-27 OF GROUP-26      NC2074.2
271000             IN GROUP-25 OF GROUP-24 IN GROUP-23 OF GROUP-22      NC2074.2
271100             IN GROUP-21 OF GROUP-20 IN GROUP-19 OF GROUP-18      NC2074.2
271200             IN GROUP-17 OF GROUP-16 IN GROUP-15 OF GROUP-14      NC2074.2
271300             IN GROUP-13 OF GROUP-12 IN GROUP-11 OF GROUP-10      NC2074.2
271400             IN GROUP-09 OF GROUP-08 IN GROUP-07 OF GROUP-06      NC2074.2
271500             IN GROUP-05 OF GROUP-04 IN GROUP-03 OF GROUP-02      NC2074.2
271600             IN SECOND-GROUP                                      NC2074.2
271700          TO COMPUTED-N.                                          NC2074.2
271800     PERFORM FAIL.                                                NC2074.2
271900 CND-WRITE-GF-1.                                                  NC2074.2
272000     PERFORM PRINT-DETAIL.                                        NC2074.2
272100 CCVS-EXIT SECTION.                                               NC2074.2
272200 CCVS-999999.                                                     NC2074.2
272300     GO TO CLOSE-FILES.                                           NC2074.2
      *END-OF,NC207A                                                                  
