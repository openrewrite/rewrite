      *HEADER,COBOL,NC246A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2464.2
000200 PROGRAM-ID.                                                      NC2464.2
000300     NC246A.                                                      NC2464.2
000400****************************************************************  NC2464.2
000500*                                                              *  NC2464.2
000600*    VALIDATION FOR:-                                          *  NC2464.2
000700*                                                              *  NC2464.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2464.2
000900*                                                              *  NC2464.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2464.2
001100*                                                              *  NC2464.2
001200****************************************************************  NC2464.2
001300*                                                              *  NC2464.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2464.2
001500*                                                              *  NC2464.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2464.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2464.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2464.2
001900*                                                              *  NC2464.2
002000****************************************************************  NC2464.2
002100*                                                              *  NC2464.2
002200*    PROGRAM NC246A TESTS THE USE OF QUALIFIED DATA NAMES AND  *  NC2464.2
002300*    SUBSCRIPTS WHEN ACCESSING A SEVEN-DIMENSIONAL TABLE.      *  NC2464.2
002400*    QUALIFIED CONDITION-NAMES AND RELATIVE INDEXING ARE ALSO  *  NC2464.2
002500*    USED IN ACCESSING THREE-DIMENSIONAL TABLES.               *  NC2464.2
002600*                                                              *  NC2464.2
002700****************************************************************  NC2464.2
002800*                                                              *  NC2464.2
002900*    DATA-NAMES MAY BE QUALIFIED AND THE NUMBER OF QUALIFIERS*    NC2464.2
003000*    PERMITTED MUST BE AT LEAST FIVE.  WHEN A SUBSCRIPT IS     *  NC2464.2
003100*    REPRESENTED BY A DATA-NAME, THE DATA-NAME MAY BE QUALIFIED*  NC2464.2
003200*    BUT NOT SUBSCRIPTED.                                      *  NC2464.2
003300*                                                              *  NC2464.2
003400****************************************************************  NC2464.2
003500*                                                              *  NC2464.2
003600*    STATEMENT DELETION INSTRUCTIONS                           *  NC2464.2
003700*                                                              *  NC2464.2
003800*    IF THE COMPILER REJECTS ANY OF THE TABLE REFERENCES IN  *    NC2464.2
003900*    THIS ROUTINE, DELETE THAT LINE OF CODE BY PLACING AN * IN *  NC2464.2
004000*    COLUMN 7.  LEAVE THE PERFORM STATEMENT.  THE TEST ELEMENT *  NC2464.2
004100*    DELETED APPEARS AS A FAILURE ON THE OUTPUT REPORT AND THE *  NC2464.2
004200*    COMPUTED RESULTS ARE SPACES.                              *  NC2464.2
004300*                                                              *  NC2464.2
004400****************************************************************  NC2464.2
004500 ENVIRONMENT DIVISION.                                            NC2464.2
004600 CONFIGURATION SECTION.                                           NC2464.2
004700 SOURCE-COMPUTER.                                                 NC2464.2
004800     XXXXX082.                                                    NC2464.2
004900 OBJECT-COMPUTER.                                                 NC2464.2
005000     XXXXX083.                                                    NC2464.2
005100 INPUT-OUTPUT SECTION.                                            NC2464.2
005200 FILE-CONTROL.                                                    NC2464.2
005300     SELECT PRINT-FILE ASSIGN TO                                  NC2464.2
005400     XXXXX055.                                                    NC2464.2
005500 DATA DIVISION.                                                   NC2464.2
005600 FILE SECTION.                                                    NC2464.2
005700 FD  PRINT-FILE.                                                  NC2464.2
005800 01  PRINT-REC PICTURE X(120).                                    NC2464.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 NC2464.2
006000 WORKING-STORAGE SECTION.                                         NC2464.2
006100 01  TABLE-A.                                                     NC2464.2
006200  02  L2 OCCURS 2.                                                NC2464.2
006300   03  L3 OCCURS 2.                                               NC2464.2
006400    04  L4 OCCURS 2.                                              NC2464.2
006500     05  L5 OCCURS 2.                                             NC2464.2
006600      06  L6 OCCURS 2.                                            NC2464.2
006700       07  L7 OCCURS 2.                                           NC2464.2
006800        08  L8 OCCURS 2.                                          NC2464.2
006900         09  ELEM1        PIC 99.                                 NC2464.2
007000         09  ELEM2        PIC 99.                                 NC2464.2
007100 01  TABLE-B.                                                     NC2464.2
007200  02  L2 OCCURS 2.                                                NC2464.2
007300   03  L3 OCCURS 2.                                               NC2464.2
007400    04  L4 OCCURS 2.                                              NC2464.2
007500     05  L5 OCCURS 2.                                             NC2464.2
007600      06  L6 OCCURS 2.                                            NC2464.2
007700       07  L7 OCCURS 2.                                           NC2464.2
007800        08  L8 OCCURS 2.                                          NC2464.2
007900         09  ELEM1        PIC 99.                                 NC2464.2
008000         09  ELEM2        PIC 99.                                 NC2464.2
008100 01  SUBSCRIPTS-GROUP-1.                                          NC2464.2
008200  02  SO2.                                                        NC2464.2
008300   03  SO3.                                                       NC2464.2
008400    04  SO4.                                                      NC2464.2
008500     05  SO5.                                                     NC2464.2
008600      06  SO6.                                                    NC2464.2
008700       07  SO7.                                                   NC2464.2
008800        08  SO8.                                                  NC2464.2
008900         09  SO9.                                                 NC2464.2
009000          10  S10.                                                NC2464.2
009100           11  S11.                                               NC2464.2
009200            12  S12.                                              NC2464.2
009300             13  S13.                                             NC2464.2
009400              14  S14.                                            NC2464.2
009500               15  S15.                                           NC2464.2
009600                16  S16.                                          NC2464.2
009700                 17  S17.                                         NC2464.2
009800                  18  S18.                                        NC2464.2
009900                   19  S19.                                       NC2464.2
010000                    20  S20.                                      NC2464.2
010100                     21  S21.                                     NC2464.2
010200                      22  S22.                                    NC2464.2
010300                       23  S23.                                   NC2464.2
010400                        24  S24.                                  NC2464.2
010500                         25  S25.                                 NC2464.2
010600                          26  S26.                                NC2464.2
010700                           27  S27.                               NC2464.2
010800                            28  S28.                              NC2464.2
010900                             29  S29.                             NC2464.2
011000                              30  S30.                            NC2464.2
011100                               31  S31.                           NC2464.2
011200                                32  S32.                          NC2464.2
011300                                 33  S33.                         NC2464.2
011400                                  34  S34.                        NC2464.2
011500                                   35  S35.                       NC2464.2
011600                                    36  S36.                      NC2464.2
011700                                     37  S37.                     NC2464.2
011800                                      38  S38.                    NC2464.2
011900                                       39  S39.                   NC2464.2
012000                                        40  S40.                  NC2464.2
012100                                         41  S41.                 NC2464.2
012200                                          42  S42.                NC2464.2
012300                                           43  S43.               NC2464.2
012400                                            44  S44.              NC2464.2
012500                                             45  S45.             NC2464.2
012600                                              46  S46.            NC2464.2
012700                                               47  S47.           NC2464.2
012800                                                48  S48.          NC2464.2
012900                                                 49  SUB1 PIC 9   NC2464.2
013000                                                     VALUE 1.     NC2464.2
013100                                                 49  SUB2 PIC 9   NC2464.2
013200                                                     VALUE 1.     NC2464.2
013300                                                 49  SUB3 PIC 9   NC2464.2
013400                                                     VALUE 1.     NC2464.2
013500                                                 49  SUB4 PIC 9   NC2464.2
013600                                                     VALUE 1.     NC2464.2
013700                                                 49  SUB5 PIC 9   NC2464.2
013800                                                     VALUE 1.     NC2464.2
013900                                                 49  SUB6 PIC 9   NC2464.2
014000                                                     VALUE 1.     NC2464.2
014100                                                 49  SUB7 PIC 9   NC2464.2
014200                                                     VALUE 1.     NC2464.2
014300 01  SUBSCRIPTS-GROUP-2.                                          NC2464.2
014400  02  SO2.                                                        NC2464.2
014500   03  SO3.                                                       NC2464.2
014600    04  SO4.                                                      NC2464.2
014700     05  SO5.                                                     NC2464.2
014800      06  SO6.                                                    NC2464.2
014900       07  SO7.                                                   NC2464.2
015000        08  SO8.                                                  NC2464.2
015100         09  SO9.                                                 NC2464.2
015200          10  S10.                                                NC2464.2
015300           11  S11.                                               NC2464.2
015400            12  S12.                                              NC2464.2
015500             13  S13.                                             NC2464.2
015600              14  S14.                                            NC2464.2
015700               15  S15.                                           NC2464.2
015800                16  S16.                                          NC2464.2
015900                 17  S17.                                         NC2464.2
016000                  18  S18.                                        NC2464.2
016100                   19  S19.                                       NC2464.2
016200                    20  S20.                                      NC2464.2
016300                     21  S21.                                     NC2464.2
016400                      22  S22.                                    NC2464.2
016500                       23  S23.                                   NC2464.2
016600                        24  S24.                                  NC2464.2
016700                         25  S25.                                 NC2464.2
016800                          26  S26.                                NC2464.2
016900                           27  S27.                               NC2464.2
017000                            28  S28.                              NC2464.2
017100                             29  S29.                             NC2464.2
017200                              30  S30.                            NC2464.2
017300                               31  S31.                           NC2464.2
017400                                32  S32.                          NC2464.2
017500                                 33  S33.                         NC2464.2
017600                                  34  S34.                        NC2464.2
017700                                   35  S35.                       NC2464.2
017800                                    36  S36.                      NC2464.2
017900                                     37  S37.                     NC2464.2
018000                                      38  S38.                    NC2464.2
018100                                       39  S39.                   NC2464.2
018200                                        40  S40.                  NC2464.2
018300                                         41  S41.                 NC2464.2
018400                                          42  S42.                NC2464.2
018500                                           43  S43.               NC2464.2
018600                                            44  S44.              NC2464.2
018700                                             45  S45.             NC2464.2
018800                                              46  S46.            NC2464.2
018900                                               47  S47.           NC2464.2
019000                                                48  S48.          NC2464.2
019100                                                 49  SUB1 PIC 9   NC2464.2
019200                                                     VALUE 2.     NC2464.2
019300                                                 49  SUB2 PIC 9   NC2464.2
019400                                                     VALUE 2.     NC2464.2
019500                                                 49  SUB3 PIC 9   NC2464.2
019600                                                     VALUE 2.     NC2464.2
019700                                                 49  SUB4 PIC 9   NC2464.2
019800                                                     VALUE 2.     NC2464.2
019900                                                 49  SUB5 PIC 9   NC2464.2
020000                                                     VALUE 2.     NC2464.2
020100                                                 49  SUB6 PIC 9   NC2464.2
020200                                                     VALUE 2.     NC2464.2
020300                                                 49  SUB7 PIC 9   NC2464.2
020400                                                     VALUE 2.     NC2464.2
020500 01  COMPARISON-VALUES.                                           NC2464.2
020600     02  EXPECTED-VALUE   PICTURE X(6).                           NC2464.2
020700     02  TEMP-VALUE       PICTURE X(6).                           NC2464.2
020800 01  GROUP-1-TABLE.                                               NC2464.2
020900     02  TABLE-LEVEL-2.                                           NC2464.2
021000         03  FILLER  PIC X(13)  VALUE "GROUP-1-TABLE".            NC2464.2
021100         03  TABLE-LEVEL-3.                                       NC2464.2
021200             04  FILLER  PIC X  VALUE SPACE.                      NC2464.2
021300             04  TABLE-LEVEL-4.                                   NC2464.2
021400                 05  FILLER  PIC X  VALUE "=".                    NC2464.2
021500                 05  TABLE-LEVEL-5.                               NC2464.2
021600                     06  FILLER  PIC X   VALUE SPACE.             NC2464.2
021700                     06  TABLE-ITEM   PICTURE X                   NC2464.2
021800                           OCCURS 15 TIMES                        NC2464.2
021900                           INDEXED BY IN1.                        NC2464.2
022000                     88  EQUALS-A   VALUE "A".                    NC2464.2
022100                     88  EQUALS-C   VALUE "C".                    NC2464.2
022200                     88  EQUALS-M   VALUE "M".                    NC2464.2
022300                 05  GROUP-1-ENTRY REDEFINES TABLE-LEVEL-5.       NC2464.2
022400                     06  FILLER  PIC X(16).                       NC2464.2
022500 01  GROUP-2-TABLE.                                               NC2464.2
022600     02  TABLE-LEVEL-2.                                           NC2464.2
022700         03  FILLER  PIC X(13)  VALUE "GROUP-2-TABLE".            NC2464.2
022800         03  TABLE-LEVEL-3.                                       NC2464.2
022900             04  FILLER  PIC X  VALUE SPACE.                      NC2464.2
023000             04  TABLE-LEVEL-4.                                   NC2464.2
023100                 05  FILLER  PIC X  VALUE "=".                    NC2464.2
023200                 05  TABLE-LEVEL-5.                               NC2464.2
023300                     06  FILLER  PIC X   VALUE SPACE.             NC2464.2
023400                     06  TABLE-ITEM   PICTURE X                   NC2464.2
023500                           OCCURS 12 TIMES                        NC2464.2
023600                           INDEXED BY IN2.                        NC2464.2
023700                     88  EQUALS-A   VALUE "A".                    NC2464.2
023800                     88  EQUALS-C   VALUE "C".                    NC2464.2
023900                     88  EQUALS-M   VALUE "M".                    NC2464.2
024000                 05  GROUP-2-ENTRY REDEFINES TABLE-LEVEL-5.       NC2464.2
024100                     06  FILLER  PIC X(13).                       NC2464.2
024200 01  GROUP-3-TABLE.                                               NC2464.2
024300     02  TABLE-LEVEL-2.                                           NC2464.2
024400         03  FILLER  PIC X(15)  VALUE "GROUP-3-TABLE =".          NC2464.2
024500         03  TABLE-LEVEL-3.                                       NC2464.2
024600             04  TABLE-LEVEL-4                                    NC2464.2
024700                   OCCURS 2 TIMES                                 NC2464.2
024800                   INDEXED BY IN3.                                NC2464.2
024900                 05  TABLE-LEVEL-5                                NC2464.2
025000                       OCCURS 2 TIMES                             NC2464.2
025100                       INDEXED BY IN4.                            NC2464.2
025200                   06  TABLE-ITEM      PICTURE X                  NC2464.2
025300                           OCCURS 4 TIMES                         NC2464.2
025400                           INDEXED BY IN5.                        NC2464.2
025500                     88  EQUALS-A   VALUE "A".                    NC2464.2
025600                     88  EQUALS-C   VALUE "C".                    NC2464.2
025700                     88  EQUALS-M   VALUE "M".                    NC2464.2
025800         03  GROUP-3-ENTRY REDEFINES TABLE-LEVEL-3.               NC2464.2
025900                     06  FILLER  PIC X(16).                       NC2464.2
026000 01  GROUP-4-TABLE.                                               NC2464.2
026100     02  UNQUAL-TABLE-2.                                          NC2464.2
026200         03  UNQUAL-TABLE-3.                                      NC2464.2
026300             04  UNQUAL-TABLE-4.                                  NC2464.2
026400                 05  FILLER  PIC X(15)  VALUE "GROUP-4-TABLE =".  NC2464.2
026500                05  UNQUAL-TABLE-5.                               NC2464.2
026600                     06  UNQUAL-ITEM  PIC X                       NC2464.2
026700                        OCCURS 15 TIMES.                          NC2464.2
026800 01  GROUP-5-TABLE.                                               NC2464.2
026900     02  TABLE5-LEVEL-2.                                          NC2464.2
027000         03  FILLER  PIC X(15)   VALUE "GROUP-5-TABLE =".         NC2464.2
027100         03  TABLE5-LEVEL-3.                                      NC2464.2
027200             04  TABLE5-LEVEL-4  OCCURS 2 TIMES.                  NC2464.2
027300                 05  TABLE5-LEVEL-5 OCCURS 2 TIMES.               NC2464.2
027400                     06  TABLE5-ITEM-UNQUAL   PIC X               NC2464.2
027500                           OCCURS 4 TIMES.                        NC2464.2
027600 01  FIRST-SUB    PIC  99    VALUE 1.                             NC2464.2
027700 01  FOURTH-SUB   PIC  99    VALUE 4.                             NC2464.2
027800 01  UNQUAL-SUB   PIC  99.                                        NC2464.2
027900 01  SUBSCRIPTS-PART1.                                            NC2464.2
028000     02  SUBSCRIPTS.                                              NC2464.2
028100         03  SUB1  PIC 9    VALUE 5.                              NC2464.2
028200         03  SUB2  PIC 99   VALUE 12.                             NC2464.2
028300         03  SUB3  PIC 999   USAGE COMP   VALUE 1.                NC2464.2
028400     02  SOME-MORE-SUBSCRIPTS.                                    NC2464.2
028500         03  SUB1   PIC 9   USAGE COMP   VALUE 3.                 NC2464.2
028600         03  SUB2   PIC 99  USAGE COMP   VALUE 7.                 NC2464.2
028700         03  SUB3   PIC 999   VALUE 15.                           NC2464.2
028800 01  SUBSCRIPTS-PART2.                                            NC2464.2
028900     02  SUB-PART2-LEVEL2.                                        NC2464.2
029000         03  SUB-PART2-LEVEL3.                                    NC2464.2
029100             04  SUB-PART2-LEVEL4.                                NC2464.2
029200                 05  SUBSCRIPTS.                                  NC2464.2
029300                     06  SUB1   PIC 999   VALUE 5.                NC2464.2
029400                     06  SUB2   PIC 99   VALUE 12.                NC2464.2
029500                     06  SUB3   PIC 99   USAGE COMP   VALUE 1.    NC2464.2
029600         03  SOME-MORE-SUBSCRIPTS.                                NC2464.2
029700             04  SUB1  PIC 999   USAGE COMP   VALUE 3.            NC2464.2
029800             04  SUB2   PIC 99   VALUE 7.                         NC2464.2
029900         04  SUB3  PIC 99  USAGE COMP  VALUE 15.                  NC2464.2
030000 01  TEST-RESULTS.                                                NC2464.2
030100     02 FILLER                   PIC X      VALUE SPACE.          NC2464.2
030200     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2464.2
030300     02 FILLER                   PIC X      VALUE SPACE.          NC2464.2
030400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2464.2
030500     02 FILLER                   PIC X      VALUE SPACE.          NC2464.2
030600     02  PAR-NAME.                                                NC2464.2
030700       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2464.2
030800       03  PARDOT-X              PIC X      VALUE SPACE.          NC2464.2
030900       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2464.2
031000     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2464.2
031100     02 RE-MARK                  PIC X(61).                       NC2464.2
031200 01  TEST-COMPUTED.                                               NC2464.2
031300     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2464.2
031400     02 FILLER                   PIC X(17)  VALUE                 NC2464.2
031500            "       COMPUTED=".                                   NC2464.2
031600     02 COMPUTED-X.                                               NC2464.2
031700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2464.2
031800     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2464.2
031900                                 PIC -9(9).9(9).                  NC2464.2
032000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2464.2
032100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2464.2
032200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2464.2
032300     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2464.2
032400         04 COMPUTED-18V0                    PIC -9(18).          NC2464.2
032500         04 FILLER                           PIC X.               NC2464.2
032600     03 FILLER PIC X(50) VALUE SPACE.                             NC2464.2
032700 01  TEST-CORRECT.                                                NC2464.2
032800     02 FILLER PIC X(30) VALUE SPACE.                             NC2464.2
032900     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2464.2
033000     02 CORRECT-X.                                                NC2464.2
033100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2464.2
033200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2464.2
033300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2464.2
033400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2464.2
033500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2464.2
033600     03      CR-18V0 REDEFINES CORRECT-A.                         NC2464.2
033700         04 CORRECT-18V0                     PIC -9(18).          NC2464.2
033800         04 FILLER                           PIC X.               NC2464.2
033900     03 FILLER PIC X(2) VALUE SPACE.                              NC2464.2
034000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2464.2
034100 01  CCVS-C-1.                                                    NC2464.2
034200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2464.2
034300-    "SS  PARAGRAPH-NAME                                          NC2464.2
034400-    "       REMARKS".                                            NC2464.2
034500     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2464.2
034600 01  CCVS-C-2.                                                    NC2464.2
034700     02 FILLER                     PIC X        VALUE SPACE.      NC2464.2
034800     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2464.2
034900     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2464.2
035000     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2464.2
035100     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2464.2
035200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2464.2
035300 01  REC-CT                        PIC 99       VALUE ZERO.       NC2464.2
035400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2464.2
035500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2464.2
035600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2464.2
035700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2464.2
035800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2464.2
035900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2464.2
036000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2464.2
036100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2464.2
036200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2464.2
036300 01  CCVS-H-1.                                                    NC2464.2
036400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2464.2
036500     02  FILLER                    PIC X(42)    VALUE             NC2464.2
036600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2464.2
036700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2464.2
036800 01  CCVS-H-2A.                                                   NC2464.2
036900   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2464.2
037000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2464.2
037100   02  FILLER                        PIC XXXX   VALUE             NC2464.2
037200     "4.2 ".                                                      NC2464.2
037300   02  FILLER                        PIC X(28)  VALUE             NC2464.2
037400            " COPY - NOT FOR DISTRIBUTION".                       NC2464.2
037500   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2464.2
037600                                                                  NC2464.2
037700 01  CCVS-H-2B.                                                   NC2464.2
037800   02  FILLER                        PIC X(15)  VALUE             NC2464.2
037900            "TEST RESULT OF ".                                    NC2464.2
038000   02  TEST-ID                       PIC X(9).                    NC2464.2
038100   02  FILLER                        PIC X(4)   VALUE             NC2464.2
038200            " IN ".                                               NC2464.2
038300   02  FILLER                        PIC X(12)  VALUE             NC2464.2
038400     " HIGH       ".                                              NC2464.2
038500   02  FILLER                        PIC X(22)  VALUE             NC2464.2
038600            " LEVEL VALIDATION FOR ".                             NC2464.2
038700   02  FILLER                        PIC X(58)  VALUE             NC2464.2
038800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2464.2
038900 01  CCVS-H-3.                                                    NC2464.2
039000     02  FILLER                      PIC X(34)  VALUE             NC2464.2
039100            " FOR OFFICIAL USE ONLY    ".                         NC2464.2
039200     02  FILLER                      PIC X(58)  VALUE             NC2464.2
039300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2464.2
039400     02  FILLER                      PIC X(28)  VALUE             NC2464.2
039500            "  COPYRIGHT   1985 ".                                NC2464.2
039600 01  CCVS-E-1.                                                    NC2464.2
039700     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2464.2
039800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2464.2
039900     02 ID-AGAIN                     PIC X(9).                    NC2464.2
040000     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2464.2
040100 01  CCVS-E-2.                                                    NC2464.2
040200     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2464.2
040300     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2464.2
040400     02 CCVS-E-2-2.                                               NC2464.2
040500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2464.2
040600         03 FILLER                   PIC X      VALUE SPACE.      NC2464.2
040700         03 ENDER-DESC               PIC X(44)  VALUE             NC2464.2
040800            "ERRORS ENCOUNTERED".                                 NC2464.2
040900 01  CCVS-E-3.                                                    NC2464.2
041000     02  FILLER                      PIC X(22)  VALUE             NC2464.2
041100            " FOR OFFICIAL USE ONLY".                             NC2464.2
041200     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2464.2
041300     02  FILLER                      PIC X(58)  VALUE             NC2464.2
041400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2464.2
041500     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2464.2
041600     02 FILLER                       PIC X(15)  VALUE             NC2464.2
041700             " COPYRIGHT 1985".                                   NC2464.2
041800 01  CCVS-E-4.                                                    NC2464.2
041900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2464.2
042000     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2464.2
042100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2464.2
042200     02 FILLER                       PIC X(40)  VALUE             NC2464.2
042300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2464.2
042400 01  XXINFO.                                                      NC2464.2
042500     02 FILLER                       PIC X(19)  VALUE             NC2464.2
042600            "*** INFORMATION ***".                                NC2464.2
042700     02 INFO-TEXT.                                                NC2464.2
042800       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2464.2
042900       04 XXCOMPUTED                 PIC X(20).                   NC2464.2
043000       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2464.2
043100       04 XXCORRECT                  PIC X(20).                   NC2464.2
043200     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2464.2
043300 01  HYPHEN-LINE.                                                 NC2464.2
043400     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2464.2
043500     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2464.2
043600-    "*****************************************".                 NC2464.2
043700     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2464.2
043800-    "******************************".                            NC2464.2
043900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2464.2
044000     "NC246A".                                                    NC2464.2
044100 PROCEDURE DIVISION.                                              NC2464.2
044200 CCVS1 SECTION.                                                   NC2464.2
044300 OPEN-FILES.                                                      NC2464.2
044400     OPEN     OUTPUT PRINT-FILE.                                  NC2464.2
044500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2464.2
044600     MOVE    SPACE TO TEST-RESULTS.                               NC2464.2
044700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2464.2
044800     GO TO CCVS1-EXIT.                                            NC2464.2
044900 CLOSE-FILES.                                                     NC2464.2
045000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2464.2
045100 TERMINATE-CCVS.                                                  NC2464.2
045200*    EXIT PROGRAM.                                                NC2464.2
045300*TERMINATE-CALL.                                                  NC2464.2
045400     STOP     RUN.                                                NC2464.2
045500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2464.2
045600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2464.2
045700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2464.2
045800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2464.2
045900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2464.2
046000 PRINT-DETAIL.                                                    NC2464.2
046100     IF REC-CT NOT EQUAL TO ZERO                                  NC2464.2
046200             MOVE "." TO PARDOT-X                                 NC2464.2
046300             MOVE REC-CT TO DOTVALUE.                             NC2464.2
046400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2464.2
046500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2464.2
046600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2464.2
046700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2464.2
046800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2464.2
046900     MOVE SPACE TO CORRECT-X.                                     NC2464.2
047000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2464.2
047100     MOVE     SPACE TO RE-MARK.                                   NC2464.2
047200 HEAD-ROUTINE.                                                    NC2464.2
047300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2464.2
047400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2464.2
047500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2464.2
047600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2464.2
047700 COLUMN-NAMES-ROUTINE.                                            NC2464.2
047800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2464.2
047900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2464.2
048000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2464.2
048100 END-ROUTINE.                                                     NC2464.2
048200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2464.2
048300 END-RTN-EXIT.                                                    NC2464.2
048400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2464.2
048500 END-ROUTINE-1.                                                   NC2464.2
048600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2464.2
048700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2464.2
048800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2464.2
048900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2464.2
049000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2464.2
049100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2464.2
049200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2464.2
049300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2464.2
049400  END-ROUTINE-12.                                                 NC2464.2
049500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2464.2
049600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2464.2
049700         MOVE "NO " TO ERROR-TOTAL                                NC2464.2
049800         ELSE                                                     NC2464.2
049900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2464.2
050000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2464.2
050100     PERFORM WRITE-LINE.                                          NC2464.2
050200 END-ROUTINE-13.                                                  NC2464.2
050300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2464.2
050400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2464.2
050500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2464.2
050600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2464.2
050700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2464.2
050800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2464.2
050900          MOVE "NO " TO ERROR-TOTAL                               NC2464.2
051000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2464.2
051100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2464.2
051200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2464.2
051300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2464.2
051400 WRITE-LINE.                                                      NC2464.2
051500     ADD 1 TO RECORD-COUNT.                                       NC2464.2
051600*    IF RECORD-COUNT GREATER 50                                   NC2464.2
051700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2464.2
051800*        MOVE SPACE TO DUMMY-RECORD                               NC2464.2
051900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2464.2
052000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2464.2
052100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2464.2
052200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2464.2
052300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2464.2
052400*        MOVE ZERO TO RECORD-COUNT.                               NC2464.2
052500     PERFORM WRT-LN.                                              NC2464.2
052600 WRT-LN.                                                          NC2464.2
052700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2464.2
052800     MOVE SPACE TO DUMMY-RECORD.                                  NC2464.2
052900 BLANK-LINE-PRINT.                                                NC2464.2
053000     PERFORM WRT-LN.                                              NC2464.2
053100 FAIL-ROUTINE.                                                    NC2464.2
053200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2464.2
053300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2464.2
053400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2464.2
053500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2464.2
053600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2464.2
053700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2464.2
053800     GO TO  FAIL-ROUTINE-EX.                                      NC2464.2
053900 FAIL-ROUTINE-WRITE.                                              NC2464.2
054000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2464.2
054100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2464.2
054200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2464.2
054300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2464.2
054400 FAIL-ROUTINE-EX. EXIT.                                           NC2464.2
054500 BAIL-OUT.                                                        NC2464.2
054600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2464.2
054700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2464.2
054800 BAIL-OUT-WRITE.                                                  NC2464.2
054900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2464.2
055000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2464.2
055100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2464.2
055200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2464.2
055300 BAIL-OUT-EX. EXIT.                                               NC2464.2
055400 CCVS1-EXIT.                                                      NC2464.2
055500     EXIT.                                                        NC2464.2
055600 SECT-NC246A-001 SECTION.                                         NC2464.2
055700*                                                                 NC2464.2
055800 TABLE-INIT.                                                      NC2464.2
055900     MOVE "INIT-TABLE" TO PAR-NAME.                               NC2464.2
056000     MOVE "STORE TABLE VALUES" TO FEATURE.                        NC2464.2
056100*                                                                 NC2464.2
056200*         THIS SECTION STORES THE LETTERS OF THE ALPHABET IN THE  NC2464.2
056300*    THREE TABLES WHOSE ITEMS ARE REFERENCED IN THE QUALIFICATION NC2464.2
056400*    TESTS IN THIS ROUTINE.  THE TABLE CONTENTS ARE AS FOLLOWS    NC2464.2
056500*            GROUP-1-TABLE     A,B,...,O.                         NC2464.2
056600*            GROUP-2-TABLE     L,K,J,...,B,A.                     NC2464.2
056700*            GROUP-3-TABLE     A,B,...,O,P.                       NC2464.2
056800*            GROUP-4-TABLE     A,B,...,O.                         NC2464.2
056900*            GROUP-5-TABLE     P,O,N,...,B,A.                     NC2464.2
057000*    THE TABLES ARE ALSO PRINTED ON THE OUTPUT REPORT.            NC2464.2
057100*                                                                 NC2464.2
057200     MOVE " ABCDEFGHIJKLMNO" TO GROUP-1-ENTRY.                    NC2464.2
057300     MOVE " LKJIHGFEDCBA" TO GROUP-2-ENTRY.                       NC2464.2
057400     MOVE "ABCDEFGHIJKLMNOP" TO GROUP-3-ENTRY.                    NC2464.2
057500     MOVE "ABCDEFGHIJKLMNO" TO UNQUAL-TABLE-5.                    NC2464.2
057600     MOVE "PONMLKJIHGFEDCBA" TO TABLE5-LEVEL-3.                   NC2464.2
057700*                                                                 NC2464.2
057800 TABLE-PRINT.                                                     NC2464.2
057900     MOVE GROUP-1-TABLE TO RE-MARK.                               NC2464.2
058000     MOVE "SEE REMARKS" TO COMPUTED-A.                            NC2464.2
058100     MOVE "ABCDEFGHIJKLMNO" TO CORRECT-A.                         NC2464.2
058200     MOVE 1 TO REC-CT.                                            NC2464.2
058300     PERFORM PRINT-DETAIL.                                        NC2464.2
058400     MOVE GROUP-2-TABLE TO RE-MARK.                               NC2464.2
058500     MOVE "LKJIHGFEDCBA" TO CORRECT-A.                            NC2464.2
058600     MOVE "SEE REMARKS" TO COMPUTED-A.                            NC2464.2
058700     MOVE 2 TO REC-CT.                                            NC2464.2
058800     PERFORM PRINT-DETAIL.                                        NC2464.2
058900     MOVE GROUP-3-TABLE TO RE-MARK.                               NC2464.2
059000     MOVE "ABCDEFGHIJKLMNOP" TO CORRECT-A.                        NC2464.2
059100     MOVE 3 TO REC-CT.                                            NC2464.2
059200     MOVE "SEE REMARKS" TO COMPUTED-A.                            NC2464.2
059300     PERFORM PRINT-DETAIL.                                        NC2464.2
059400     MOVE GROUP-4-TABLE TO RE-MARK.                               NC2464.2
059500     MOVE "ABCDEFGHIJKLMNO" TO CORRECT-A.                         NC2464.2
059600     MOVE "SEE REMARKS" TO COMPUTED-A.                            NC2464.2
059700     MOVE 4 TO REC-CT.                                            NC2464.2
059800     PERFORM PRINT-DETAIL.                                        NC2464.2
059900     MOVE GROUP-5-TABLE TO RE-MARK.                               NC2464.2
060000     MOVE "PONMLKJIHGFEDCBA" TO CORRECT-A.                        NC2464.2
060100     MOVE "SEE REMARKS" TO COMPUTED-A.                            NC2464.2
060200     MOVE 5 TO REC-CT.                                            NC2464.2
060300     PERFORM PRINT-DETAIL.                                        NC2464.2
060400*                                                                 NC2464.2
060500 QUAL-TEST-01.                                                    NC2464.2
060600     MOVE ZERO TO REC-CT.                                         NC2464.2
060700     MOVE SPACE TO TEMP-VALUE.                                    NC2464.2
060800     MOVE "QUAL-TEST-01" TO PAR-NAME.                             NC2464.2
060900     MOVE "QUALIFIED TABLE ITEM" TO FEATURE.                      NC2464.2
061000     MOVE "ONE DIMENSIONAL TABLE" TO RE-MARK.                     NC2464.2
061100     MOVE "A" TO EXPECTED-VALUE.                                  NC2464.2
061200*                                                                 NC2464.2
061300*         THIS TEST CONTAINS QUALIFIED DATA NAMES IN MOVE         NC2464.2
061400*    STATEMENTS.  THE DATA NAMES REFER TO SINGLE DIMENSIONAL      NC2464.2
061500*    TABLE ITEMS.  THE SUBSCRIPTS IN THIS TEST ARE CONSTANTS,     NC2464.2
061600*    UNQUALIFIED DATA NAMES AND INDEXES.  BOTH DIRECT AND RELATIVENC2464.2
061700*    INDEXING ARE USED.                                           NC2464.2
061800*                                                                 NC2464.2
061900 QUAL-TEST-01-01.                                                 NC2464.2
062000     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
062100          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
062200          OF GROUP-1-TABLE (1) TO TEMP-VALUE.                     NC2464.2
062300     PERFORM SECT-TH220-0003.                                     NC2464.2
062400*                                                                 NC2464.2
062500 QUAL-TEST-01-02.                                                 NC2464.2
062600     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
062700          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
062800          OF GROUP-1-TABLE (FIRST-SUB) TO TEMP-VALUE.             NC2464.2
062900     PERFORM SECT-TH220-0003.                                     NC2464.2
063000*                                                                 NC2464.2
063100 QUAL-TEST-01-03.                                                 NC2464.2
063200     SET IN1 TO 1.                                                NC2464.2
063300     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
063400          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
063500          OF GROUP-1-TABLE (IN1) TO TEMP-VALUE.                   NC2464.2
063600     PERFORM SECT-TH220-0003.                                     NC2464.2
063700*                                                                 NC2464.2
063800 QUAL-TEST-01-04.                                                 NC2464.2
063900     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
064000          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
064100          OF GROUP-2-TABLE (12) TO TEMP-VALUE.                    NC2464.2
064200     PERFORM SECT-TH220-0003.                                     NC2464.2
064300*                                                                 NC2464.2
064400 QUAL-TEST-01-05.                                                 NC2464.2
064500     SET IN1 TO 1.                                                NC2464.2
064600     MOVE "D" TO EXPECTED-VALUE.                                  NC2464.2
064700     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
064800          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
064900          OF GROUP-1-TABLE (IN1 + 3) TO TEMP-VALUE.               NC2464.2
065000     PERFORM SECT-TH220-0003.                                     NC2464.2
065100*                                                                 NC2464.2
065200 QUAL-TEST-01-06.                                                 NC2464.2
065300     SET IN1 TO 6.                                                NC2464.2
065400     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
065500          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
065600          OF GROUP-1-TABLE (IN1 - 2) TO TEMP-VALUE.               NC2464.2
065700     PERFORM SECT-TH220-0003.                                     NC2464.2
065800*                                                                 NC2464.2
065900 QUAL-TEST-01-07.                                                 NC2464.2
066000     MOVE 9 TO UNQUAL-SUB.                                        NC2464.2
066100     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF TABLE-LEVEL-4            NC2464.2
066200          OF TABLE-LEVEL-3 OF TABLE-LEVEL-2                       NC2464.2
066300          OF GROUP-2-TABLE (UNQUAL-SUB) TO TEMP-VALUE.            NC2464.2
066400     PERFORM SECT-TH220-0003.                                     NC2464.2
066500     GO TO QUAL-TEST-02.                                          NC2464.2
066600*                                                                 NC2464.2
066700 QUAL-DELETE-001.                                                 NC2464.2
066800     PERFORM DE-LETE.                                             NC2464.2
066900     PERFORM PRINT-DETAIL.                                        NC2464.2
067000*                                                                 NC2464.2
067100 QUAL-TEST-02.                                                    NC2464.2
067200     MOVE ZERO TO REC-CT.                                         NC2464.2
067300     MOVE "QUAL-TEST-02" TO PAR-NAME.                             NC2464.2
067400     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
067500*                                                                 NC2464.2
067600*         THIS TEST CONTAINS TWO QUALIFIED DATA NAMES IN IF       NC2464.2
067700*    STATEMENTS.  THE DATA NAMES REFER TO SINGLE DIMENSIONAL      NC2464.2
067800*    TABLE ITEMS.  THE SUBSCRIPTS IN THIS TEST ARE CONSTANTS,     NC2464.2
067900*    UNQUALIFIED DATA NAMES AND INDEXES.  BOTH DIRECT AND RELATIVENC2464.2
068000*    INDEXING ARE USED.                                           NC2464.2
068100*                                                                 NC2464.2
068200 QUAL-TEST-02-01.                                                 NC2464.2
068300     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
068400     IF TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
068500        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
068600        IN GROUP-1-TABLE (1) IS EQUAL TO                          NC2464.2
068700        TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
068800        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
068900        IN GROUP-2-TABLE (12)                                     NC2464.2
069000              MOVE "TRUE" TO TEMP-VALUE.                          NC2464.2
069100     PERFORM SECT-TH220-0003.                                     NC2464.2
069200*                                                                 NC2464.2
069300 QUAL-TEST-02-02.                                                 NC2464.2
069400     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
069500     IF TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
069600        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
069700        IN GROUP-1-TABLE (FIRST-SUB) IS NOT EQUAL TO              NC2464.2
069800        TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
069900        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
070000        IN GROUP-2-TABLE (FIRST-SUB)                              NC2464.2
070100              MOVE "TRUE" TO TEMP-VALUE.                          NC2464.2
070200     PERFORM SECT-TH220-0003.                                     NC2464.2
070300*                                                                 NC2464.2
070400 QUAL-TEST-02-03.                                                 NC2464.2
070500     SET IN1 TO 4.                                                NC2464.2
070600     SET IN2 TO 9.                                                NC2464.2
070700     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
070800     IF TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
070900        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
071000        IN GROUP-1-TABLE (IN1) IS EQUAL TO                        NC2464.2
071100        TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
071200        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
071300        IN GROUP-2-TABLE (IN2)                                    NC2464.2
071400              MOVE "TRUE" TO TEMP-VALUE.                          NC2464.2
071500     PERFORM SECT-TH220-0003.                                     NC2464.2
071600*                                                                 NC2464.2
071700 QUAL-TEST-02-04.                                                 NC2464.2
071800     SET IN1 IN2 TO 5.                                            NC2464.2
071900     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
072000     IF TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
072100        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
072200        IN GROUP-1-TABLE (IN1 - 1) EQUAL TO                       NC2464.2
072300        TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
072400        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
072500        IN GROUP-2-TABLE (IN2 + 4)                                NC2464.2
072600              MOVE "TRUE" TO TEMP-VALUE.                          NC2464.2
072700     PERFORM SECT-TH220-0003.                                     NC2464.2
072800*                                                                 NC2464.2
072900 QUAL-TEST-02-05.                                                 NC2464.2
073000     SET IN1 TO 5.                                                NC2464.2
073100     MOVE 8 TO UNQUAL-SUB.                                        NC2464.2
073200     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
073300     IF TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
073400        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
073500        IN GROUP-1-TABLE (IN1) EQUAL TO                           NC2464.2
073600        TABLE-ITEM IN TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
073700        IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                         NC2464.2
073800        IN GROUP-2-TABLE (UNQUAL-SUB)                             NC2464.2
073900              MOVE "TRUE" TO TEMP-VALUE.                          NC2464.2
074000     PERFORM SECT-TH220-0003.                                     NC2464.2
074100     GO TO QUAL-INIT-03.                                          NC2464.2
074200*                                                                 NC2464.2
074300 QUAL-DELETE-002.                                                 NC2464.2
074400     PERFORM DE-LETE.                                             NC2464.2
074500     PERFORM PRINT-DETAIL.                                        NC2464.2
074600*                                                                 NC2464.2
074700 QUAL-INIT-03.                                                    NC2464.2
074800     MOVE ZERO TO REC-CT.                                         NC2464.2
074900     MOVE "QUAL-TEST-03" TO PAR-NAME.                             NC2464.2
075000     MOVE "THREE DIMENSIONAL TABLE" TO RE-MARK.                   NC2464.2
075100     MOVE SPACE TO TEMP-VALUE.                                    NC2464.2
075200     MOVE "D" TO EXPECTED-VALUE.                                  NC2464.2
075300*                                                                 NC2464.2
075400*         THIS TEST CONTAINS QUALIFIED DATA NAMES IN MOVE         NC2464.2
075500*     STATEMENTS.  THE DATA NAMES REFER TO THREE DIMENSIONAL      NC2464.2
075600*    TABLE ITEMS.  THE SUBSCRIPTS IN THIS TEST ARE CONSTANTS,     NC2464.2
075700*    UNQUALIFIED DATA NAMES AND INDEXES.  BOTH DIRECT AND RELATIVENC2464.2
075800*    INDEXING ARE USED.                                           NC2464.2
075900*                                                                 NC2464.2
076000*                                                                 NC2464.2
076100 QUAL-TEST-03-01.                                                 NC2464.2
076200     MOVE TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
076300          OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                       NC2464.2
076400          OF GROUP-3-TABLE (1, 1, 4) TO TEMP-VALUE.               NC2464.2
076500     PERFORM SECT-TH220-0003.                                     NC2464.2
076600*                                                                 NC2464.2
076700 QUAL-TEST-03-02.                                                 NC2464.2
076800     MOVE TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
076900          OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                       NC2464.2
077000          OF GROUP-3-TABLE (FIRST-SUB, FIRST-SUB, FOURTH-SUB)     NC2464.2
077100              TO TEMP-VALUE.                                      NC2464.2
077200     PERFORM SECT-TH220-0003.                                     NC2464.2
077300*                                                                 NC2464.2
077400 QUAL-TEST-03-03.                                                 NC2464.2
077500     SET IN5 TO 4.                                                NC2464.2
077600     MOVE TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
077700          OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                       NC2464.2
077800          OF GROUP-3-TABLE (1, 1, IN5) TO TEMP-VALUE.             NC2464.2
077900     PERFORM SECT-TH220-0003.                                     NC2464.2
078000*                                                                 NC2464.2
078100 QUAL-TEST-03-04.                                                 NC2464.2
078200     SET IN3, IN4 TO 1.                                           NC2464.2
078300     SET IN5 TO 4.                                                NC2464.2
078400     MOVE TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
078500          OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                       NC2464.2
078600          OF GROUP-3-TABLE (IN3, IN4, IN5) TO TEMP-VALUE.         NC2464.2
078700     PERFORM SECT-TH220-0003.                                     NC2464.2
078800*                                                                 NC2464.2
078900 QUAL-TEST-03-05.                                                 NC2464.2
079000     SET IN3, IN4 TO 2.                                           NC2464.2
079100     SET IN5 TO 1.                                                NC2464.2
079200     MOVE TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
079300          OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                       NC2464.2
079400          IN GROUP-3-TABLE (IN3 - 1, IN4 - 1, IN5 + 3)            NC2464.2
079500              TO TEMP-VALUE.                                      NC2464.2
079600     PERFORM SECT-TH220-0003.                                     NC2464.2
079700     GO TO QUAL-INIT-04.                                          NC2464.2
079800*                                                                 NC2464.2
079900 QUAL-DELETE-003.                                                 NC2464.2
080000     PERFORM DE-LETE.                                             NC2464.2
080100     PERFORM PRINT-DETAIL.                                        NC2464.2
080200*                                                                 NC2464.2
080300 QUAL-INIT-04.                                                    NC2464.2
080400     MOVE "QUAL-TEST-04" TO PAR-NAME.                             NC2464.2
080500     MOVE ZERO TO REC-CT.                                         NC2464.2
080600     MOVE "QUALIFIED SUBSCRIPT" TO FEATURE.                       NC2464.2
080700     MOVE  "ONE DIMENSIONAL TABLE" TO RE-MARK.                    NC2464.2
080800     MOVE SPACE TO TEMP-VALUE.                                    NC2464.2
080900*                                                                 NC2464.2
081000*         THIS TEST CONTAINS UNQUALIFIED DATA NAMES WITH          NC2464.2
081100*    QUALIFIED SUBSCRIPTS IN MOVE STATEMENTS.  THE DATA NAMES     NC2464.2
081200*    REFER TO SINGLE DIMENSIONAL TABLE ITEMS.                     NC2464.2
081300*                                                                 NC2464.2
081400     MOVE "E" TO EXPECTED-VALUE.                                  NC2464.2
081500*                                                                 NC2464.2
081600 QUAL-TEST-04-01.                                                 NC2464.2
081700     MOVE UNQUAL-ITEM (SUB1 OF SUBSCRIPTS OF SUBSCRIPTS-PART1)    NC2464.2
081800                   TO TEMP-VALUE.                                 NC2464.2
081900     PERFORM SECT-TH220-0003.                                     NC2464.2
082000*                                                                 NC2464.2
082100 QUAL-TEST-04-02.                                                 NC2464.2
082200     MOVE UNQUAL-ITEM (SUB1 OF SUBSCRIPTS OF SUB-PART2-LEVEL4)    NC2464.2
082300                   TO TEMP-VALUE.                                 NC2464.2
082400     PERFORM SECT-TH220-0003.                                     NC2464.2
082500*                                                                 NC2464.2
082600 QUAL-TEST-04-03.                                                 NC2464.2
082700     MOVE UNQUAL-ITEM (SUB1 OF SUBSCRIPTS OF SUB-PART2-LEVEL4     NC2464.2
082800             OF SUB-PART2-LEVEL3 IN SUB-PART2-LEVEL2              NC2464.2
082900             IN SUBSCRIPTS-PART2)                                 NC2464.2
083000                   TO TEMP-VALUE.                                 NC2464.2
083100     PERFORM SECT-TH220-0003.                                     NC2464.2
083200*                                                                 NC2464.2
083300 QUAL-TEST-04-04.                                                 NC2464.2
083400     MOVE "C" TO EXPECTED-VALUE.                                  NC2464.2
083500     MOVE UNQUAL-ITEM (SUB1 OF SOME-MORE-SUBSCRIPTS OF            NC2464.2
083600             SUBSCRIPTS-PART1)                                    NC2464.2
083700                   TO TEMP-VALUE.                                 NC2464.2
083800     PERFORM SECT-TH220-0003.                                     NC2464.2
083900*                                                                 NC2464.2
084000 QUAL-TEST-04-05.                                                 NC2464.2
084100     MOVE "G" TO EXPECTED-VALUE.                                  NC2464.2
084200     MOVE UNQUAL-ITEM (SUB2 OF SOME-MORE-SUBSCRIPTS OF            NC2464.2
084300             SUB-PART2-LEVEL2)                                    NC2464.2
084400                   TO TEMP-VALUE.                                 NC2464.2
084500     PERFORM SECT-TH220-0003.                                     NC2464.2
084600     GO TO QUAL-INIT-05.                                          NC2464.2
084700*                                                                 NC2464.2
084800 QUAL-DELETE-004.                                                 NC2464.2
084900     PERFORM DE-LETE.                                             NC2464.2
085000     PERFORM PRINT-DETAIL.                                        NC2464.2
085100*                                                                 NC2464.2
085200 QUAL-INIT-05.                                                    NC2464.2
085300     MOVE "QUAL-TEST-05" TO PAR-NAME.                             NC2464.2
085400     MOVE ZERO TO REC-CT.                                         NC2464.2
085500     MOVE "THREE DIMENSIONAL TABLE" TO RE-MARK.                   NC2464.2
085600*                                                                 NC2464.2
085700*         THIS TEST CONTAINS UNQUALIFIED DATA NAMES WITH          NC2464.2
085800*    QUALIFIED SUBSCRIPTS IN MOVE STATEMENTS.  THE DATA NAMES     NC2464.2
085900*    REFER TO THREE DIMENSIONAL TABLE ITEMS.                      NC2464.2
086000*                                                                 NC2464.2
086100     MOVE SPACE TO TEMP-VALUE.                                    NC2464.2
086200     MOVE "N" TO EXPECTED-VALUE.                                  NC2464.2
086300*                                                                 NC2464.2
086400 QUAL-TEST-05-01.                                                 NC2464.2
086500     MOVE TABLE5-ITEM-UNQUAL (FIRST-SUB FIRST-SUB                 NC2464.2
086600             SUB1 OF SOME-MORE-SUBSCRIPTS OF SUB-PART2-LEVEL2     NC2464.2
086700             IN SUBSCRIPTS-PART2)                                 NC2464.2
086800                  TO TEMP-VALUE.                                  NC2464.2
086900     PERFORM SECT-TH220-0003.                                     NC2464.2
087000*                                                                 NC2464.2
087100 QUAL-TEST-05-02.                                                 NC2464.2
087200     MOVE TABLE5-ITEM-UNQUAL (SUB3 OF SUBSCRIPTS OF               NC2464.2
087300             SUBSCRIPTS-PART1 SUB3 OF SUBSCRIPTS OF               NC2464.2
087400             SUB-PART2-LEVEL4 IN SUB-PART2-LEVEL3 IN              NC2464.2
087500             SUB-PART2-LEVEL2 IN SUBSCRIPTS-PART2                 NC2464.2
087600             SUB1 OF SOME-MORE-SUBSCRIPTS OF SUB-PART2-LEVEL2     NC2464.2
087700             IN SUBSCRIPTS-PART2)                                 NC2464.2
087800                  TO TEMP-VALUE.                                  NC2464.2
087900     PERFORM SECT-TH220-0003.                                     NC2464.2
088000     GO TO QUAL-INIT-06.                                          NC2464.2
088100*                                                                 NC2464.2
088200 QUAL-DELETE-005.                                                 NC2464.2
088300     PERFORM DE-LETE.                                             NC2464.2
088400     PERFORM PRINT-DETAIL.                                        NC2464.2
088500*                                                                 NC2464.2
088600 QUAL-INIT-06.                                                    NC2464.2
088700     MOVE "QUAL-TEST-06" TO PAR-NAME.                             NC2464.2
088800     MOVE "ONE DIMENSIONAL TABLE" TO RE-MARK.                     NC2464.2
088900     MOVE ZERO TO REC-CT.                                         NC2464.2
089000*                                                                 NC2464.2
089100*         THIS TEST CONTAINS QUALIFIED DATA NAMES WITH            NC2464.2
089200*    QUALIFIED SUBSCRIPTS IN IF STATEMENTS.  THE DATA NAMES       NC2464.2
089300*    REFER TO SINGLE DIMENSIONAL TABLE ITEMS.                     NC2464.2
089400*                                                                 NC2464.2
089500     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
089600*                                                                 NC2464.2
089700 QUAL-TEST-06-01.                                                 NC2464.2
089800     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
089900     IF TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
090000              OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
090100              OF GROUP-1-TABLE (SUB3 IN SOME-MORE-SUBSCRIPTS      NC2464.2
090200              IN SUB-PART2-LEVEL2 IN SUBSCRIPTS-PART2)            NC2464.2
090300         IS EQUAL TO "O"                                          NC2464.2
090400                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
090500     PERFORM SECT-TH220-0003.                                     NC2464.2
090600*                                                                 NC2464.2
090700 QUAL-TEST-06-02.                                                 NC2464.2
090800     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
090900     IF TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
091000              OF TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
091100              OF GROUP-1-TABLE (SUB2 OF SUBSCRIPTS OF             NC2464.2
091200              SUB-PART2-LEVEL4 IN SUB-PART2-LEVEL3 IN             NC2464.2
091300              SUB-PART2-LEVEL2 IN SUBSCRIPTS-PART2)               NC2464.2
091400         IS EQUAL TO TABLE-ITEM OF TABLE-LEVEL-5                  NC2464.2
091500              IN TABLE-LEVEL-4 OF TABLE-LEVEL-3 IN                NC2464.2
091600              TABLE-LEVEL-2 OF GROUP-2-TABLE (SUB3 IN SUBSCRIPTS  NC2464.2
091700              OF SUBSCRIPTS-PART1)                                NC2464.2
091800                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
091900     PERFORM SECT-TH220-0003.                                     NC2464.2
092000     GO TO QUAL-INIT-07.                                          NC2464.2
092100*                                                                 NC2464.2
092200 QUAL-DELETE-006.                                                 NC2464.2
092300     PERFORM DE-LETE.                                             NC2464.2
092400     PERFORM PRINT-DETAIL.                                        NC2464.2
092500*                                                                 NC2464.2
092600 QUAL-INIT-07.                                                    NC2464.2
092700     MOVE "QUAL-TEST-07" TO PAR-NAME.                             NC2464.2
092800     MOVE "THREE DIMENSIONAL TABLE" TO RE-MARK.                   NC2464.2
092900     MOVE ZERO TO REC-CT.                                         NC2464.2
093000*                                                                 NC2464.2
093100*         THIS TEST CONTAINS QUALIFIED DATA NAMES WITH            NC2464.2
093200*    QUALIFIED SUBSCRIPTS IN IF STATEMENTS.  THE DATA NAMES       NC2464.2
093300*    REFER TO THREE DIMENSIONAL TABLE ITEMS.                      NC2464.2
093400*                                                                 NC2464.2
093500     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
093600*                                                                 NC2464.2
093700 QUAL-TEST-07-01.                                                 NC2464.2
093800     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
093900     IF TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
094000              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
094100              IN GROUP-3-TABLE (SUB3 IN SUBSCRIPTS IN             NC2464.2
094200              SUBSCRIPTS-PART1 SUB3 OF SUBSCRIPTS OF              NC2464.2
094300              SUBSCRIPTS-PART1 SUB1 OF SOME-MORE-SUBSCRIPTS OF    NC2464.2
094400              SUBSCRIPTS-PART1) IS EQUAL TO "C"                   NC2464.2
094500                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
094600     PERFORM SECT-TH220-0003.                                     NC2464.2
094700*                                                                 NC2464.2
094800 QUAL-TEST-07-02.                                                 NC2464.2
094900     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
095000     IF TABLE-ITEM OF TABLE-LEVEL-5 IN TABLE-LEVEL-4              NC2464.2
095100              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
095200              OF GROUP-3-TABLE (SUB3 IN SUBSCRIPTS IN             NC2464.2
095300              SUB-PART2-LEVEL4 OF SUB-PART2-LEVEL3 OF             NC2464.2
095400              SUB-PART2-LEVEL2 OF SUBSCRIPTS-PART2   SUB3 IN      NC2464.2
095500              SUBSCRIPTS IN SUB-PART2-LEVEL4 OF SUB-PART2-LEVEL3  NC2464.2
095600              IN SUB-PART2-LEVEL2 OF SUBSCRIPTS-PART2   SUB1 OF   NC2464.2
095700              SOME-MORE-SUBSCRIPTS OF SUB-PART2-LEVEL2            NC2464.2
095800              IN SUBSCRIPTS-PART2)                                NC2464.2
095900         IS EQUAL TO TABLE-ITEM OF TABLE-LEVEL-5 IN               NC2464.2
096000              TABLE-LEVEL-4 IN TABLE-LEVEL-3 OF TABLE-LEVEL-2     NC2464.2
096100              IN GROUP-3-TABLE (SUB3 OF SUBSCRIPTS IN             NC2464.2
096200              SUBSCRIPTS-PART1  SUB3 OF SUBSCRIPTS IN             NC2464.2
096300              SUB-PART2-LEVEL4 OF SUB-PART2-LEVEL3                NC2464.2
096400              OF SUB-PART2-LEVEL2 OF SUBSCRIPTS-PART2  SUB1       NC2464.2
096500              OF SOME-MORE-SUBSCRIPTS OF SUBSCRIPTS-PART1)        NC2464.2
096600                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
096700     PERFORM SECT-TH220-0003.                                     NC2464.2
096800     GO TO QUAL-INIT-08.                                          NC2464.2
096900*                                                                 NC2464.2
097000 QUAL-DELETE-007.                                                 NC2464.2
097100     PERFORM DE-LETE.                                             NC2464.2
097200     PERFORM PRINT-DETAIL.                                        NC2464.2
097300*                                                                 NC2464.2
097400 QUAL-INIT-08.                                                    NC2464.2
097500     MOVE "QUAL-TEST-08" TO PAR-NAME.                             NC2464.2
097600     MOVE "QUAL. CONDITION NAME" TO FEATURE.                      NC2464.2
097700     MOVE ZERO TO REC-CT.                                         NC2464.2
097800     MOVE "ONE DIMENSIONAL TABLE" TO RE-MARK.                     NC2464.2
097900*                                                                 NC2464.2
098000*         THIS TEST CONTAINS QUALIFIED CONDITION NAMES IN IF      NC2464.2
098100*    STATEMENTS.  THE CONDITION NAMES REFER TO SINGLE DIMENSIONAL NC2464.2
098200*    CONDITIONAL VARIABLES.  THE SUBSCRIPTS IN THIS TEST ARE      NC2464.2
098300*    CONSTANTS, UNQUALIFIED DATA NAMES AND INDEXES.  BOTH DIRECT  NC2464.2
098400*    AND RELATIVE INDEXING ARE USED.                              NC2464.2
098500*                                                                 NC2464.2
098600     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
098700*                                                                 NC2464.2
098800 QUAL-TEST-08-01.                                                 NC2464.2
098900     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
099000     IF EQUALS-M OF TABLE-LEVEL-5 OF TABLE-LEVEL-4                NC2464.2
099100              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
099200              OF GROUP-1-TABLE (13)                               NC2464.2
099300                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
099400     PERFORM SECT-TH220-0003.                                     NC2464.2
099500*                                                                 NC2464.2
099600 QUAL-TEST-08-02.                                                 NC2464.2
099700     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
099800     IF EQUALS-A OF TABLE-LEVEL-5 OF TABLE-LEVEL-4                NC2464.2
099900              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
100000              OF GROUP-1-TABLE (FIRST-SUB)                        NC2464.2
100100                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
100200     PERFORM SECT-TH220-0003.                                     NC2464.2
100300*                                                                 NC2464.2
100400 QUAL-TEST-08-03.                                                 NC2464.2
100500     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
100600     SET IN1 TO 3.                                                NC2464.2
100700     IF EQUALS-C OF TABLE-LEVEL-5 OF TABLE-LEVEL-4                NC2464.2
100800              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
100900              OF GROUP-1-TABLE (IN1)                              NC2464.2
101000                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
101100     PERFORM SECT-TH220-0003.                                     NC2464.2
101200*                                                                 NC2464.2
101300 QUAL-TEST-08-04.                                                 NC2464.2
101400     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
101500     SET IN1 TO 6.                                                NC2464.2
101600     IF EQUALS-A OF TABLE-LEVEL-5 OF TABLE-LEVEL-4                NC2464.2
101700              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
101800              OF GROUP-1-TABLE (IN1 - 5)                          NC2464.2
101900                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
102000     PERFORM SECT-TH220-0003.                                     NC2464.2
102100*                                                                 NC2464.2
102200 QUAL-TEST-08-05.                                                 NC2464.2
102300     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
102400     SET IN1 TO 1.                                                NC2464.2
102500     IF EQUALS-C OF TABLE-LEVEL-5 OF TABLE-LEVEL-4                NC2464.2
102600              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
102700              OF GROUP-1-TABLE (IN1 + 2)                          NC2464.2
102800                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
102900     PERFORM SECT-TH220-0003.                                     NC2464.2
103000     GO TO QUAL-INIT-09.                                          NC2464.2
103100*                                                                 NC2464.2
103200 QUAL-DELETE-008.                                                 NC2464.2
103300     PERFORM DE-LETE.                                             NC2464.2
103400     PERFORM PRINT-DETAIL.                                        NC2464.2
103500*                                                                 NC2464.2
103600 QUAL-INIT-09.                                                    NC2464.2
103700     MOVE "QUAL-TEST-09" TO PAR-NAME.                             NC2464.2
103800     MOVE ZERO TO REC-CT.                                         NC2464.2
103900     MOVE "THREE DIMENSIONAL TABLE" TO RE-MARK.                   NC2464.2
104000*                                                                 NC2464.2
104100*         THIS TEST CONTAINS QUALIFIED CONDITION NAMES IN IF      NC2464.2
104200*    STATEMENTS.  THE CONDITION NAMES REFER TO THREE DIMENSIONAL  NC2464.2
104300*    CONDITIONAL VARIABLES.  THE SUBSCRIPTS IN THIS TEST ARE      NC2464.2
104400*    CONSTANTS, UNQUALIFIED DATA NAMES AND INDEXES.  BOTH DIRECT  NC2464.2
104500*    AND RELATIVE INDEXING ARE USED.                              NC2464.2
104600*                                                                 NC2464.2
104700     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
104800*                                                                 NC2464.2
104900 QUAL-TEST-09-01.                                                 NC2464.2
105000     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
105100     IF EQUALS-M OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
105200              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
105300              OF GROUP-3-TABLE (2, 2, 1)                          NC2464.2
105400                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
105500     PERFORM SECT-TH220-0003.                                     NC2464.2
105600*                                                                 NC2464.2
105700 QUAL-TEST-09-02.                                                 NC2464.2
105800     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
105900     IF EQUALS-A OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
106000              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
106100              OF GROUP-3-TABLE (FIRST-SUB, FIRST-SUB, FIRST-SUB)  NC2464.2
106200                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
106300     PERFORM SECT-TH220-0003.                                     NC2464.2
106400*                                                                 NC2464.2
106500 QUAL-TEST-09-03.                                                 NC2464.2
106600     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
106700     SET IN5 TO 3.                                                NC2464.2
106800     IF EQUALS-C OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
106900              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
107000              OF GROUP-3-TABLE (1, 1, IN5)                        NC2464.2
107100                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
107200     PERFORM SECT-TH220-0003.                                     NC2464.2
107300*                                                                 NC2464.2
107400 QUAL-TEST-09-04.                                                 NC2464.2
107500     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
107600     SET IN3, IN4 TO 1.                                           NC2464.2
107700     SET IN5 TO 3.                                                NC2464.2
107800     IF EQUALS-C OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
107900              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
108000              OF GROUP-3-TABLE (IN3, IN4, IN5)                    NC2464.2
108100                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
108200     PERFORM SECT-TH220-0003.                                     NC2464.2
108300*                                                                 NC2464.2
108400 QUAL-TEST-09-05.                                                 NC2464.2
108500     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
108600     SET IN5 TO 1.                                                NC2464.2
108700     SET IN3, IN4 TO 2.                                           NC2464.2
108800     IF EQUALS-C OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
108900              IN TABLE-LEVEL-3 IN TABLE-LEVEL-2                   NC2464.2
109000              OF GROUP-3-TABLE (IN3 - 1, IN4 - 1, IN5 + 2)        NC2464.2
109100                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
109200     PERFORM SECT-TH220-0003.                                     NC2464.2
109300     GO TO QUAL-INIT-10.                                          NC2464.2
109400*                                                                 NC2464.2
109500 QUAL-DELETE-009.                                                 NC2464.2
109600     PERFORM DE-LETE.                                             NC2464.2
109700     PERFORM PRINT-DETAIL.                                        NC2464.2
109800*                                                                 NC2464.2
109900 QUAL-INIT-10.                                                    NC2464.2
110000     MOVE "QUAL-TEST-10" TO PAR-NAME.                             NC2464.2
110100     MOVE "QUALIFIED SUBSCRIPTS" TO RE-MARK.                      NC2464.2
110200     MOVE ZERO TO REC-CT.                                         NC2464.2
110300*                                                                 NC2464.2
110400*         THIS TEST CONTAINS QUALIFIED CONDITION NAMES WITH       NC2464.2
110500*    QUALIFIED SUBSCRIPTS.                                        NC2464.2
110600*                                                                 NC2464.2
110700     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
110800*                                                                 NC2464.2
110900 QUAL-TEST-10-01.                                                 NC2464.2
111000     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
111100     IF EQUALS-C OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
111200              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
111300              IN GROUP-1-TABLE (SUB1 OF SOME-MORE-SUBSCRIPTS      NC2464.2
111400              IN SUBSCRIPTS-PART1)                                NC2464.2
111500                    MOVE "TRUE" TO TEMP-VALUE.                    NC2464.2
111600     PERFORM SECT-TH220-0003.                                     NC2464.2
111700*                                                                 NC2464.2
111800 QUAL-TEST-10-02.                                                 NC2464.2
111900     IF NOT EQUALS-M OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
112000              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
112100              IN GROUP-2-TABLE (SUB2 OF SUBSCRIPTS                NC2464.2
112200              OF SUB-PART2-LEVEL4 OF SUB-PART2-LEVEL3             NC2464.2
112300              OF SUB-PART2-LEVEL2 OF SUBSCRIPTS-PART2)            NC2464.2
112400                    MOVE "TRUE" TO TEMP-VALUE.                    NC2464.2
112500     PERFORM SECT-TH220-0003.                                     NC2464.2
112600*                                                                 NC2464.2
112700 QUAL-TEST-10-03.                                                 NC2464.2
112800     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
112900     IF EQUALS-C OF TABLE-LEVEL-5 IN TABLE-LEVEL-4                NC2464.2
113000              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
113100              IN GROUP-3-TABLE (SUB3 OF SUBSCRIPTS OF             NC2464.2
113200              SUB-PART2-LEVEL4 IN SUB-PART2-LEVEL3 IN             NC2464.2
113300              SUB-PART2-LEVEL2 IN SUBSCRIPTS-PART2,               NC2464.2
113400              SUB3 IN SUBSCRIPTS IN SUBSCRIPTS-PART1,             NC2464.2
113500              SUB1 IN SOME-MORE-SUBSCRIPTS IN SUB-PART2-LEVEL2    NC2464.2
113600              IN SUBSCRIPTS-PART2)                                NC2464.2
113700                    MOVE "TRUE" TO TEMP-VALUE.                    NC2464.2
113800     PERFORM SECT-TH220-0003.                                     NC2464.2
113900*                                                                 NC2464.2
114000 QUAL-TEST-10-04.                                                 NC2464.2
114100     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
114200     IF NOT EQUALS-A OF TABLE-LEVEL-5 IN TABLE-LEVEL-4            NC2464.2
114300              IN TABLE-LEVEL-3 OF TABLE-LEVEL-2                   NC2464.2
114400              IN GROUP-3-TABLE (SUB3 OF SUBSCRIPTS OF             NC2464.2
114500              SUB-PART2-LEVEL4 IN SUB-PART2-LEVEL3 IN             NC2464.2
114600              SUB-PART2-LEVEL2 IN SUBSCRIPTS-PART2,               NC2464.2
114700              SUB3 IN SUBSCRIPTS OF SUB-PART2-LEVEL4 OF           NC2464.2
114800              SUB-PART2-LEVEL3 IN SUB-PART2-LEVEL2 IN             NC2464.2
114900              SUBSCRIPTS-PART2,  SUB1 OF SOME-MORE-SUBSCRIPTS     NC2464.2
115000              OF SUB-PART2-LEVEL2 IN SUBSCRIPTS-PART2)            NC2464.2
115100                    MOVE "TRUE" TO TEMP-VALUE.                    NC2464.2
115200     PERFORM SECT-TH220-0003.                                     NC2464.2
115300     GO TO QUAL-INIT-11.                                          NC2464.2
115400*                                                                 NC2464.2
115500 QUAL-DELETE-010.                                                 NC2464.2
115600     PERFORM DE-LETE.                                             NC2464.2
115700     PERFORM PRINT-DETAIL.                                        NC2464.2
115800*                                                                 NC2464.2
115900 QUAL-INIT-11.                                                    NC2464.2
116000     MOVE "QUAL-TEST-11" TO PAR-NAME.                             NC2464.2
116100     MOVE "QUALIFICATION" TO FEATURE.                             NC2464.2
116200     MOVE "INTERMEDIATE LEVELS SKIPPED" TO RE-MARK.               NC2464.2
116300     MOVE SPACE TO TEMP-VALUE.                                    NC2464.2
116400     MOVE ZERO TO REC-CT.                                         NC2464.2
116500*                                                                 NC2464.2
116600*         THIS TEST USES QUALIFIED DATA NAMES WITHOUT ALL OF THE  NC2464.2
116700*    INTERMEDIATE LEVELS SPECIFIED.  THERE ARE QUALIFIED TABLE    NC2464.2
116800*    ITEMS AND QUALIFIED SUBSCRIPTS INCLUDED IN THE TEST          NC2464.2
116900*    STATEMENTS.                                                  NC2464.2
117000*                                                                 NC2464.2
117100     MOVE "G" TO EXPECTED-VALUE.                                  NC2464.2
117200*                                                                 NC2464.2
117300 QUAL-TEST-11-01.                                                 NC2464.2
117400     MOVE TABLE-ITEM OF TABLE-LEVEL-5 OF GROUP-1-TABLE (7)        NC2464.2
117500              TO TEMP-VALUE.                                      NC2464.2
117600     PERFORM SECT-TH220-0003.                                     NC2464.2
117700*                                                                 NC2464.2
117800 QUAL-TEST-11-02.                                                 NC2464.2
117900     MOVE UNQUAL-ITEM (SUB2 OF SOME-MORE-SUBSCRIPTS OF            NC2464.2
118000              SUBSCRIPTS-PART2) TO TEMP-VALUE.                    NC2464.2
118100     PERFORM SECT-TH220-0003.                                     NC2464.2
118200*                                                                 NC2464.2
118300 QUAL-TEST-11-03.                                                 NC2464.2
118400     MOVE   TABLE-ITEM OF GROUP-1-TABLE (SUB2 OF                  NC2464.2
118500         SOME-MORE-SUBSCRIPTS OF SUB-PART2-LEVEL2) TO TEMP-VALUE. NC2464.2
118600     PERFORM SECT-TH220-0003.                                     NC2464.2
118700*                                                                 NC2464.2
118800 QUAL-TEST-11-04.                                                 NC2464.2
118900     MOVE "A" TO EXPECTED-VALUE.                                  NC2464.2
119000     MOVE TABLE-ITEM OF GROUP-3-TABLE (FIRST-SUB, SUB3 OF         NC2464.2
119100              SUBSCRIPTS OF SUBSCRIPTS-PART1,  SUB3 OF            NC2464.2
119200              SUB-PART2-LEVEL4) TO TEMP-VALUE.                    NC2464.2
119300     PERFORM SECT-TH220-0003.                                     NC2464.2
119400*                                                                 NC2464.2
119500 QUAL-TEST-11-05.                                                 NC2464.2
119600     MOVE "TRUE" TO EXPECTED-VALUE.                               NC2464.2
119700     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
119800     SET IN1 TO 3.                                                NC2464.2
119900     IF EQUALS-C OF TABLE-ITEM OF GROUP-1-TABLE (IN1)             NC2464.2
120000                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
120100     PERFORM SECT-TH220-0003.                                     NC2464.2
120200*                                                                 NC2464.2
120300 QUAL-TEST-11-06.                                                 NC2464.2
120400     MOVE "FALSE" TO TEMP-VALUE.                                  NC2464.2
120500     IF EQUALS-C OF TABLE-ITEM OF GROUP-3-TABLE                   NC2464.2
120600              (FIRST-SUB,  SUB3 OF SUB-PART2-LEVEL3, SUB1 OF      NC2464.2
120700              SOME-MORE-SUBSCRIPTS OF SUBSCRIPTS-PART2)           NC2464.2
120800                   MOVE "TRUE" TO TEMP-VALUE.                     NC2464.2
120900     PERFORM SECT-TH220-0003.                                     NC2464.2
121000     GO TO   QUAL-INIT-12.                                        NC2464.2
121100 QUAL-DELETE-011.                                                 NC2464.2
121200     PERFORM DE-LETE.                                             NC2464.2
121300     PERFORM PRINT-DETAIL.                                        NC2464.2
121400*                                                                 NC2464.2
121500 QUAL-INIT-12.                                                    NC2464.2
121600     MOVE   "IV-21 4.3.8.2.3 SR5 AND VI-2 1.3.2/4"                NC2464.2
121700          TO ANSI-REFERENCE.                                      NC2464.2
121800     MOVE   "QUAL-TEST-12" TO PAR-NAME.                           NC2464.2
121900     MOVE   "SEVEN DIMENSIONAL TABLE" TO RE-MARK.                 NC2464.2
122000     MOVE    ZEROES TO TABLE-A                                    NC2464.2
122100                       TABLE-B.                                   NC2464.2
122200     MOVE    27 TO ELEM1 OF L8 IN L7 IN L6 IN L5 IN L4 IN L3      NC2464.2
122300                         IN L2 OF TABLE-A (1, 2, 1, 2, 1, 1, 2).  NC2464.2
122400     GO TO   QUAL-TEST-12.                                        NC2464.2
122500 QUAL-DELETE-12.                                                  NC2464.2
122600     PERFORM DE-LETE.                                             NC2464.2
122700     PERFORM PRINT-DETAIL.                                        NC2464.2
122800     GO TO   CCVS-EXIT.                                           NC2464.2
122900 QUAL-TEST-12.                                                    NC2464.2
123000     IF      ELEM1 OF L8 IN L7 OF L6 OF L5 IN L4 IN L3 OF L2      NC2464.2
123100                   IN TABLE-A                                     NC2464.2
123200            (SUB1 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
123300                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
123400                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
123500                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
123600                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
123700                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
123800                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
123900                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
124000                  IN SUBSCRIPTS-GROUP-1,                          NC2464.2
124100             SUB2 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
124200                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
124300                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
124400                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
124500                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
124600                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
124700                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
124800                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
124900                  OF SUBSCRIPTS-GROUP-2,                          NC2464.2
125000             SUB3 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
125100                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
125200                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
125300                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
125400                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
125500                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
125600                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
125700                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
125800                  IN SUBSCRIPTS-GROUP-1,                          NC2464.2
125900             SUB4 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
126000                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
126100                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
126200                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
126300                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
126400                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
126500                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
126600                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
126700                  OF SUBSCRIPTS-GROUP-2,                          NC2464.2
126800             SUB5 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
126900                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
127000                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
127100                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
127200                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
127300                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
127400                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
127500                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
127600                  IN SUBSCRIPTS-GROUP-1,                          NC2464.2
127700             SUB6 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
127800                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
127900                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
128000                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
128100                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
128200                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
128300                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
128400                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
128500                  IN SUBSCRIPTS-GROUP-1,                          NC2464.2
128600             SUB7 OF S48 IN S47 OF S46 IN S45 OF S44 IN S43       NC2464.2
128700                  OF S42 IN S41 OF S40 IN S39 OF S38 IN S37       NC2464.2
128800                  OF S36 IN S35 OF S34 IN S33 OF S32 IN S31       NC2464.2
128900                  OF S30 IN S29 OF S28 IN S27 OF S26 IN S25       NC2464.2
129000                  OF S24 IN S23 OF S22 IN S21 OF S20 IN S19       NC2464.2
129100                  OF S18 IN S17 OF S16 IN S15 OF S14 IN S13       NC2464.2
129200                  OF S12 IN S11 OF S10 IN SO9 OF SO8 IN SO7       NC2464.2
129300                  OF SO6 IN SO5 OF SO4 IN SO3 OF SO2              NC2464.2
129400                  OF SUBSCRIPTS-GROUP-2)                          NC2464.2
129500                  = 27                                            NC2464.2
129600                  PERFORM PASS                                    NC2464.2
129700                  PERFORM PRINT-DETAIL                            NC2464.2
129800     ELSE                                                         NC2464.2
129900             MOVE  "QUALIFICATION FAILED" TO RE-MARK              NC2464.2
130000             PERFORM FAIL                                         NC2464.2
130100             PERFORM PRINT-DETAIL.                                NC2464.2
130200*                                                                 NC2464.2
130300     GO TO CCVS-EXIT.                                             NC2464.2
130400*                                                                 NC2464.2
130500 SECT-TH220-0003 SECTION.                                         NC2464.2
130600 SYNTAX-CHECK.                                                    NC2464.2
130700     ADD 1 TO REC-CT.                                             NC2464.2
130800     IF TEMP-VALUE IS EQUAL TO EXPECTED-VALUE                     NC2464.2
130900              PERFORM PASS                                        NC2464.2
131000              GO TO SYNTAX-CHECK-WRITE.                           NC2464.2
131100 SYNTAX-FAIL.                                                     NC2464.2
131200     MOVE TEMP-VALUE TO COMPUTED-A.                               NC2464.2
131300     MOVE EXPECTED-VALUE TO CORRECT-A.                            NC2464.2
131400     PERFORM FAIL.                                                NC2464.2
131500 SYNTAX-CHECK-WRITE.                                              NC2464.2
131600     PERFORM PRINT-DETAIL.                                        NC2464.2
131700     MOVE SPACE TO TEMP-VALUE.                                    NC2464.2
131800 CCVS-EXIT SECTION.                                               NC2464.2
131900 CCVS-999999.                                                     NC2464.2
132000     GO TO CLOSE-FILES.                                           NC2464.2
      *END-OF,NC246A                                                                  
