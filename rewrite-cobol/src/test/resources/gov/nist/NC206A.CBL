      *HEADER,COBOL,NC206A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2064.2
000200 PROGRAM-ID.                                                      NC2064.2
000300****************************************************************  NC2064.2
000400*                                                              *  NC2064.2
000500*    VALIDATION FOR:-                                          *  NC2064.2
000600*                                                              *  NC2064.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2064.2
000800*                                                              *  NC2064.2
000900*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2064.2
001000*                                                              *  NC2064.2
001100****************************************************************  NC2064.2
001200*                                                              *  NC2064.2
001300*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2064.2
001400*                                                              *  NC2064.2
001500*        X-55  - SYSTEM PRINTER NAME.                          *  NC2064.2
001600*        X-82  - SOURCE COMPUTER NAME.                         *  NC2064.2
001700*        X-83  - OBJECT COMPUTER NAME.                         *  NC2064.2
001800*                                                              *  NC2064.2
001900****************************************************************  NC2064.2
002000     NC206A.                                                      NC2064.2
002100*                                                              *  NC2064.2
002200*    PROGRAM NC206A TESTS THE ACCESSING OF ELEMENTARY ITEMS    *  NC2064.2
002300*    USING FORMAT 1 QUALIFICATION WITH UP TO 5 LEVELS OF       *  NC2064.2
002400*    QUALIFIERS.  SINGLE DIMENSION TABLES ARE ALSO ACCESSES    *  NC2064.2
002500*    USING SUBSCRIPTS QUALIFIED TO ONE LEVEL.                  *  NC2064.2
002600*                                                              *  NC2064.2
002700****************************************************************  NC2064.2
002800 ENVIRONMENT DIVISION.                                            NC2064.2
002900 CONFIGURATION SECTION.                                           NC2064.2
003000 SOURCE-COMPUTER.                                                 NC2064.2
003100     XXXXX082.                                                    NC2064.2
003200 OBJECT-COMPUTER.                                                 NC2064.2
003300     XXXXX083.                                                    NC2064.2
003400 INPUT-OUTPUT SECTION.                                            NC2064.2
003500 FILE-CONTROL.                                                    NC2064.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2064.2
003700     XXXXX055.                                                    NC2064.2
003800 DATA DIVISION.                                                   NC2064.2
003900 FILE SECTION.                                                    NC2064.2
004000 FD  PRINT-FILE.                                                  NC2064.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2064.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2064.2
004300 WORKING-STORAGE SECTION.                                         NC2064.2
004400 77  MAX-NAME-1        PICTURE S9(18) VALUE +1.                   NC2064.2
004500 01  TABLE-LEVEL-5A.                                              NC2064.2
004600     02  TABLE-LEVEL-4A.                                          NC2064.2
004700         03  TABLE-LEVEL-3A.                                      NC2064.2
004800             04  TABLE-LEVEL-2A.                                  NC2064.2
004900                 05  TABLE-LEVEL-1A.                              NC2064.2
005000                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3A2A1A0A".NC2064.2
005100                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3A2A1A0B".NC2064.2
005200                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3A2A1A0C".NC2064.2
005300                   06 TBL-LEVEL-0D PIC X(12) VALUE "5A4A3A2A1A0D".NC2064.2
005400                 05  TABLE-LEVEL-1B.                              NC2064.2
005500                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3A2A1B0A".NC2064.2
005600                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3A2A1B0B".NC2064.2
005700                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3A2A1B0C".NC2064.2
005800             04  TABLE-LEVEL-2B.                                  NC2064.2
005900                 05  TABLE-LEVEL-1A.                              NC2064.2
006000                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3A2B1A0A".NC2064.2
006100                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3A2B1A0B".NC2064.2
006200                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3A2B1A0C".NC2064.2
006300                 05  TABLE-LEVEL-1B.                              NC2064.2
006400                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3A2B1B0A".NC2064.2
006500                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3A2B1B0B".NC2064.2
006600                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3A2B1B0C".NC2064.2
006700         03  TABLE-LEVEL-3B.                                      NC2064.2
006800             04  TABLE-LEVEL-2A.                                  NC2064.2
006900                 05  TABLE-LEVEL-1A.                              NC2064.2
007000                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3B2A1A0A".NC2064.2
007100                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3B2A1A0B".NC2064.2
007200                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3B2A1A0C".NC2064.2
007300                 05  TABLE-LEVEL-1B.                              NC2064.2
007400                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3B2A1B0A".NC2064.2
007500                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3B2A1B0B".NC2064.2
007600                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3B2A1B0C".NC2064.2
007700             04  TABLE-LEVEL-2B.                                  NC2064.2
007800                 05  TABLE-LEVEL-1A.                              NC2064.2
007900                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3B2B1A0A".NC2064.2
008000                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3B2B1A0B".NC2064.2
008100                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3B2B1A0C".NC2064.2
008200                 05  TABLE-LEVEL-1B.                              NC2064.2
008300                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4A3B2B1B0A".NC2064.2
008400                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4A3B2B1B0B".NC2064.2
008500                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4A3B2B1B0C".NC2064.2
008600     02  TABLE-LEVEL-4B.                                          NC2064.2
008700         03  TABLE-LEVEL-3A.                                      NC2064.2
008800             04  TABLE-LEVEL-2A.                                  NC2064.2
008900                 05  TABLE-LEVEL-1A.                              NC2064.2
009000                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3A2A1A0A".NC2064.2
009100                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3A2A1A0B".NC2064.2
009200                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3A2A1A0C".NC2064.2
009300                 05  TABLE-LEVEL-1B.                              NC2064.2
009400                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3A2A1B0A".NC2064.2
009500                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3A2A1B0B".NC2064.2
009600                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3A2A1B0C".NC2064.2
009700             04  TABLE-LEVEL-2B.                                  NC2064.2
009800                 05  TABLE-LEVEL-1A.                              NC2064.2
009900                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3A2B1A0A".NC2064.2
010000                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3A2B1A0B".NC2064.2
010100                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3A2B1A0C".NC2064.2
010200                 05  TABLE-LEVEL-1B.                              NC2064.2
010300                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3A2B1B0A".NC2064.2
010400                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3A2B1B0B".NC2064.2
010500                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3A2B1B0C".NC2064.2
010600         03  TABLE-LEVEL-3B.                                      NC2064.2
010700             04  TABLE-LEVEL-2A.                                  NC2064.2
010800                 05  TABLE-LEVEL-1A.                              NC2064.2
010900                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3B2A1A0A".NC2064.2
011000                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3B2A1A0B".NC2064.2
011100                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3B2A1A0C".NC2064.2
011200                 05  TABLE-LEVEL-1B.                              NC2064.2
011300                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3B2A1B0A".NC2064.2
011400                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3B2A1B0B".NC2064.2
011500                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3B2A1B0C".NC2064.2
011600             04  TABLE-LEVEL-2B.                                  NC2064.2
011700                 05  TABLE-LEVEL-1A.                              NC2064.2
011800                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3B2B1A0A".NC2064.2
011900                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3B2B1A0B".NC2064.2
012000                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3B2B1A0C".NC2064.2
012100                 05  TABLE-LEVEL-1B.                              NC2064.2
012200                   06 TBL-LEVEL-0A PIC X(12) VALUE "5A4B3B2B1B0A".NC2064.2
012300                   06 TBL-LEVEL-0B PIC X(12) VALUE "5A4B3B2B1B0B".NC2064.2
012400                   06 TBL-LEVEL-0C PIC X(12) VALUE "5A4B3B2B1B0C".NC2064.2
012500 01  TABLE-LEVEL-5B.                                              NC2064.2
012600     02  TABLE-LEVEL-4A.                                          NC2064.2
012700         03  TABLE-LEVEL-3A.                                      NC2064.2
012800             04  TABLE-LEVEL-2A.                                  NC2064.2
012900             05  TABLE-LEVEL-1A.                                  NC2064.2
013000                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
013100                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
013200                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
013300             05  TABLE-LEVEL-1B.                                  NC2064.2
013400                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
013500                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
013600                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
013700             04  TABLE-LEVEL-2B.                                  NC2064.2
013800             05  TABLE-LEVEL-1A.                                  NC2064.2
013900                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
014000                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
014100                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
014200             05  TABLE-LEVEL-1B.                                  NC2064.2
014300                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
014400                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
014500                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
014600         03  TABLE-LEVEL-3B.                                      NC2064.2
014700             04  TABLE-LEVEL-2A.                                  NC2064.2
014800             05  TABLE-LEVEL-1A.                                  NC2064.2
014900                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
015000                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
015100                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
015200             05  TABLE-LEVEL-1B.                                  NC2064.2
015300                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
015400                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
015500                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
015600             04  TABLE-LEVEL-2B.                                  NC2064.2
015700             05  TABLE-LEVEL-1A.                                  NC2064.2
015800                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
015900                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
016000                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
016100             05  TABLE-LEVEL-1B.                                  NC2064.2
016200                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
016300                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
016400                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
016500     02  TABLE-LEVEL-4B.                                          NC2064.2
016600         03  TABLE-LEVEL-3A.                                      NC2064.2
016700             04  TABLE-LEVEL-2A.                                  NC2064.2
016800             05  TABLE-LEVEL-1A.                                  NC2064.2
016900                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
017000                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
017100                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
017200             05  TABLE-LEVEL-1B.                                  NC2064.2
017300                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
017400                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
017500                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
017600             04  TABLE-LEVEL-2B.                                  NC2064.2
017700             05  TABLE-LEVEL-1A.                                  NC2064.2
017800                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
017900                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
018000                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
018100             05  TABLE-LEVEL-1B.                                  NC2064.2
018200                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
018300                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
018400                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
018500         03  TABLE-LEVEL-3B.                                      NC2064.2
018600             04  TABLE-LEVEL-2A.                                  NC2064.2
018700             05  TABLE-LEVEL-1A.                                  NC2064.2
018800                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
018900                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
019000                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
019100             05  TABLE-LEVEL-1B.                                  NC2064.2
019200                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
019300                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
019400                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
019500             04  TABLE-LEVEL-2B.                                  NC2064.2
019600             05  TABLE-LEVEL-1A.                                  NC2064.2
019700                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
019800                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
019900                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
020000             05  TABLE-LEVEL-1B.                                  NC2064.2
020100                 06  TBL-LEVEL-0A  PIC X VALUE SPACE.             NC2064.2
020200                 06  TBL-LEVEL-0B  PIC X VALUE SPACE.             NC2064.2
020300                 06  TBL-LEVEL-0C  PIC X VALUE SPACE.             NC2064.2
020400 01  QUAL-SUB-TABLE.                                              NC2064.2
020500     02  AX.                                                      NC2064.2
020600         03  AX-1 OCCURS 5 TIMES.                                 NC2064.2
020700              04  AX-2 PIC X.                                     NC2064.2
020800              04  AX-3 PIC X.                                     NC2064.2
020900     02  BX.                                                      NC2064.2
021000         03  AX-1 OCCURS 2 TIMES.                                 NC2064.2
021100              04  AX-2 PIC 9.                                     NC2064.2
021200              04  AX-3 PIC 9.                                     NC2064.2
021300     02  CX.                                                      NC2064.2
021400         03  CX-SUB PIC 9 VALUE 2.                                NC2064.2
021500     02  DX.                                                      NC2064.2
021600         03  CX-SUB USAGE IS INDEX.                               NC2064.2
021700 01  TEST-RESULTS.                                                NC2064.2
021800     02 FILLER                   PIC X      VALUE SPACE.          NC2064.2
021900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2064.2
022000     02 FILLER                   PIC X      VALUE SPACE.          NC2064.2
022100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2064.2
022200     02 FILLER                   PIC X      VALUE SPACE.          NC2064.2
022300     02  PAR-NAME.                                                NC2064.2
022400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2064.2
022500       03  PARDOT-X              PIC X      VALUE SPACE.          NC2064.2
022600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2064.2
022700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2064.2
022800     02 RE-MARK                  PIC X(61).                       NC2064.2
022900 01  TEST-COMPUTED.                                               NC2064.2
023000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2064.2
023100     02 FILLER                   PIC X(17)  VALUE                 NC2064.2
023200            "       COMPUTED=".                                   NC2064.2
023300     02 COMPUTED-X.                                               NC2064.2
023400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2064.2
023500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2064.2
023600                                 PIC -9(9).9(9).                  NC2064.2
023700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2064.2
023800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2064.2
023900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2064.2
024000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2064.2
024100         04 COMPUTED-18V0                    PIC -9(18).          NC2064.2
024200         04 FILLER                           PIC X.               NC2064.2
024300     03 FILLER PIC X(50) VALUE SPACE.                             NC2064.2
024400 01  TEST-CORRECT.                                                NC2064.2
024500     02 FILLER PIC X(30) VALUE SPACE.                             NC2064.2
024600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2064.2
024700     02 CORRECT-X.                                                NC2064.2
024800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2064.2
024900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2064.2
025000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2064.2
025100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2064.2
025200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2064.2
025300     03      CR-18V0 REDEFINES CORRECT-A.                         NC2064.2
025400         04 CORRECT-18V0                     PIC -9(18).          NC2064.2
025500         04 FILLER                           PIC X.               NC2064.2
025600     03 FILLER PIC X(2) VALUE SPACE.                              NC2064.2
025700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2064.2
025800 01  CCVS-C-1.                                                    NC2064.2
025900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2064.2
026000-    "SS  PARAGRAPH-NAME                                          NC2064.2
026100-    "       REMARKS".                                            NC2064.2
026200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2064.2
026300 01  CCVS-C-2.                                                    NC2064.2
026400     02 FILLER                     PIC X        VALUE SPACE.      NC2064.2
026500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2064.2
026600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2064.2
026700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2064.2
026800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2064.2
026900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2064.2
027000 01  REC-CT                        PIC 99       VALUE ZERO.       NC2064.2
027100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2064.2
027200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2064.2
027300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2064.2
027400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2064.2
027500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2064.2
027600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2064.2
027700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2064.2
027800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2064.2
027900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2064.2
028000 01  CCVS-H-1.                                                    NC2064.2
028100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2064.2
028200     02  FILLER                    PIC X(42)    VALUE             NC2064.2
028300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2064.2
028400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2064.2
028500 01  CCVS-H-2A.                                                   NC2064.2
028600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2064.2
028700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2064.2
028800   02  FILLER                        PIC XXXX   VALUE             NC2064.2
028900     "4.2 ".                                                      NC2064.2
029000   02  FILLER                        PIC X(28)  VALUE             NC2064.2
029100            " COPY - NOT FOR DISTRIBUTION".                       NC2064.2
029200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2064.2
029300                                                                  NC2064.2
029400 01  CCVS-H-2B.                                                   NC2064.2
029500   02  FILLER                        PIC X(15)  VALUE             NC2064.2
029600            "TEST RESULT OF ".                                    NC2064.2
029700   02  TEST-ID                       PIC X(9).                    NC2064.2
029800   02  FILLER                        PIC X(4)   VALUE             NC2064.2
029900            " IN ".                                               NC2064.2
030000   02  FILLER                        PIC X(12)  VALUE             NC2064.2
030100     " HIGH       ".                                              NC2064.2
030200   02  FILLER                        PIC X(22)  VALUE             NC2064.2
030300            " LEVEL VALIDATION FOR ".                             NC2064.2
030400   02  FILLER                        PIC X(58)  VALUE             NC2064.2
030500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2064.2
030600 01  CCVS-H-3.                                                    NC2064.2
030700     02  FILLER                      PIC X(34)  VALUE             NC2064.2
030800            " FOR OFFICIAL USE ONLY    ".                         NC2064.2
030900     02  FILLER                      PIC X(58)  VALUE             NC2064.2
031000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2064.2
031100     02  FILLER                      PIC X(28)  VALUE             NC2064.2
031200            "  COPYRIGHT   1985 ".                                NC2064.2
031300 01  CCVS-E-1.                                                    NC2064.2
031400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2064.2
031500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2064.2
031600     02 ID-AGAIN                     PIC X(9).                    NC2064.2
031700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2064.2
031800 01  CCVS-E-2.                                                    NC2064.2
031900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2064.2
032000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2064.2
032100     02 CCVS-E-2-2.                                               NC2064.2
032200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2064.2
032300         03 FILLER                   PIC X      VALUE SPACE.      NC2064.2
032400         03 ENDER-DESC               PIC X(44)  VALUE             NC2064.2
032500            "ERRORS ENCOUNTERED".                                 NC2064.2
032600 01  CCVS-E-3.                                                    NC2064.2
032700     02  FILLER                      PIC X(22)  VALUE             NC2064.2
032800            " FOR OFFICIAL USE ONLY".                             NC2064.2
032900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2064.2
033000     02  FILLER                      PIC X(58)  VALUE             NC2064.2
033100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2064.2
033200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2064.2
033300     02 FILLER                       PIC X(15)  VALUE             NC2064.2
033400             " COPYRIGHT 1985".                                   NC2064.2
033500 01  CCVS-E-4.                                                    NC2064.2
033600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2064.2
033700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2064.2
033800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2064.2
033900     02 FILLER                       PIC X(40)  VALUE             NC2064.2
034000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2064.2
034100 01  XXINFO.                                                      NC2064.2
034200     02 FILLER                       PIC X(19)  VALUE             NC2064.2
034300            "*** INFORMATION ***".                                NC2064.2
034400     02 INFO-TEXT.                                                NC2064.2
034500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2064.2
034600       04 XXCOMPUTED                 PIC X(20).                   NC2064.2
034700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2064.2
034800       04 XXCORRECT                  PIC X(20).                   NC2064.2
034900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2064.2
035000 01  HYPHEN-LINE.                                                 NC2064.2
035100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2064.2
035200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2064.2
035300-    "*****************************************".                 NC2064.2
035400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2064.2
035500-    "******************************".                            NC2064.2
035600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2064.2
035700     "NC206A".                                                    NC2064.2
035800 PROCEDURE DIVISION.                                              NC2064.2
035900 CCVS1 SECTION.                                                   NC2064.2
036000 OPEN-FILES.                                                      NC2064.2
036100     OPEN     OUTPUT PRINT-FILE.                                  NC2064.2
036200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2064.2
036300     MOVE    SPACE TO TEST-RESULTS.                               NC2064.2
036400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2064.2
036500     GO TO CCVS1-EXIT.                                            NC2064.2
036600 CLOSE-FILES.                                                     NC2064.2
036700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2064.2
036800 TERMINATE-CCVS.                                                  NC2064.2
036900*    EXIT PROGRAM.                                                NC2064.2
037000*TERMINATE-CALL.                                                  NC2064.2
037100     STOP     RUN.                                                NC2064.2
037200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2064.2
037300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2064.2
037400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2064.2
037500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2064.2
037600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2064.2
037700 PRINT-DETAIL.                                                    NC2064.2
037800     IF REC-CT NOT EQUAL TO ZERO                                  NC2064.2
037900             MOVE "." TO PARDOT-X                                 NC2064.2
038000             MOVE REC-CT TO DOTVALUE.                             NC2064.2
038100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2064.2
038200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2064.2
038300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2064.2
038400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2064.2
038500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2064.2
038600     MOVE SPACE TO CORRECT-X.                                     NC2064.2
038700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2064.2
038800     MOVE     SPACE TO RE-MARK.                                   NC2064.2
038900 HEAD-ROUTINE.                                                    NC2064.2
039000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2064.2
039100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2064.2
039200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2064.2
039300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2064.2
039400 COLUMN-NAMES-ROUTINE.                                            NC2064.2
039500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2064.2
039600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2064.2
039700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2064.2
039800 END-ROUTINE.                                                     NC2064.2
039900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2064.2
040000 END-RTN-EXIT.                                                    NC2064.2
040100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2064.2
040200 END-ROUTINE-1.                                                   NC2064.2
040300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2064.2
040400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2064.2
040500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2064.2
040600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2064.2
040700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2064.2
040800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2064.2
040900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2064.2
041000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2064.2
041100  END-ROUTINE-12.                                                 NC2064.2
041200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2064.2
041300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2064.2
041400         MOVE "NO " TO ERROR-TOTAL                                NC2064.2
041500         ELSE                                                     NC2064.2
041600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2064.2
041700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2064.2
041800     PERFORM WRITE-LINE.                                          NC2064.2
041900 END-ROUTINE-13.                                                  NC2064.2
042000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2064.2
042100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2064.2
042200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2064.2
042300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2064.2
042400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2064.2
042500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2064.2
042600          MOVE "NO " TO ERROR-TOTAL                               NC2064.2
042700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2064.2
042800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2064.2
042900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2064.2
043000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2064.2
043100 WRITE-LINE.                                                      NC2064.2
043200     ADD 1 TO RECORD-COUNT.                                       NC2064.2
043300*    IF RECORD-COUNT GREATER 50                                   NC2064.2
043400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2064.2
043500*        MOVE SPACE TO DUMMY-RECORD                               NC2064.2
043600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2064.2
043700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2064.2
043800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2064.2
043900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2064.2
044000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2064.2
044100*        MOVE ZERO TO RECORD-COUNT.                               NC2064.2
044200     PERFORM WRT-LN.                                              NC2064.2
044300 WRT-LN.                                                          NC2064.2
044400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2064.2
044500     MOVE SPACE TO DUMMY-RECORD.                                  NC2064.2
044600 BLANK-LINE-PRINT.                                                NC2064.2
044700     PERFORM WRT-LN.                                              NC2064.2
044800 FAIL-ROUTINE.                                                    NC2064.2
044900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2064.2
045000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2064.2
045100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2064.2
045200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2064.2
045300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2064.2
045400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2064.2
045500     GO TO  FAIL-ROUTINE-EX.                                      NC2064.2
045600 FAIL-ROUTINE-WRITE.                                              NC2064.2
045700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2064.2
045800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2064.2
045900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2064.2
046000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2064.2
046100 FAIL-ROUTINE-EX. EXIT.                                           NC2064.2
046200 BAIL-OUT.                                                        NC2064.2
046300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2064.2
046400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2064.2
046500 BAIL-OUT-WRITE.                                                  NC2064.2
046600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2064.2
046700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2064.2
046800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2064.2
046900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2064.2
047000 BAIL-OUT-EX. EXIT.                                               NC2064.2
047100 CCVS1-EXIT.                                                      NC2064.2
047200     EXIT.                                                        NC2064.2
047300 SECT-NC206A-001 SECTION.                                         NC2064.2
047400 NC-06-001.                                                       NC2064.2
047500*    5 LEVELS OF QUALIFICATION ARE USED IN ORDER TO MAKE THE      NC2064.2
047600*    IDENTIFIERS UNIQUE. SEE THE 01 WORKING-STORAGE ENTRIES       NC2064.2
047700*    CALLED TABLE-LEVEL-5A AND TABLE-LEVEL-5B.                    NC2064.2
047800 QAL-INIT-F1-1.                                                   NC2064.2
047900     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
048000     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
048100     MOVE "QAL-TEST-F1-1 " TO PAR-NAME.                           NC2064.2
048200 QAL-TEST-F1-1.                                                   NC2064.2
048300     IF TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
048400         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
048500         TO "5A4A3A2A1A0A"                                        NC2064.2
048600         PERFORM PASS                                             NC2064.2
048700         GO TO QAL-WRITE-F1-1.                                    NC2064.2
048800     GO TO QAL-FAIL-F1-1.                                         NC2064.2
048900 QAL-DELETE-F1-1.                                                 NC2064.2
049000     PERFORM DE-LETE.                                             NC2064.2
049100     GO TO QAL-WRITE-F1-1.                                        NC2064.2
049200 QAL-FAIL-F1-1.                                                   NC2064.2
049300     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF     NC2064.2
049400         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
049500         COMPUTED-A.                                              NC2064.2
049600     MOVE "5A4A3A2A1A0A" TO CORRECT-A.                            NC2064.2
049700     PERFORM FAIL.                                                NC2064.2
049800 QAL-WRITE-F1-1.                                                  NC2064.2
049900     PERFORM PRINT-DETAIL.                                        NC2064.2
050000*                                                                 NC2064.2
050100 QAL-INIT-F1-2.                                                   NC2064.2
050200     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
050300     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
050400     MOVE "QAL-TEST-F1-2 " TO PAR-NAME.                           NC2064.2
050500 QAL-TEST-F1-2.                                                   NC2064.2
050600     IF TBL-LEVEL-0A IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2064.2
050700         TABLE-LEVEL-3A OF TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
050800         TO "5A4A3A2A1B0A"                                        NC2064.2
050900         PERFORM PASS                                             NC2064.2
051000         GO TO QAL-WRITE-F1-2.                                    NC2064.2
051100     GO TO QAL-FAIL-F1-2.                                         NC2064.2
051200 QAL-DELETE-F1-2.                                                 NC2064.2
051300     PERFORM DE-LETE.                                             NC2064.2
051400     GO TO QAL-WRITE-F1-2.                                        NC2064.2
051500 QAL-FAIL-F1-2.                                                   NC2064.2
051600     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN     NC2064.2
051700         TABLE-LEVEL-3A OF TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
051800         COMPUTED-A.                                              NC2064.2
051900     MOVE "5A4A3A2A1B0A" TO CORRECT-A.                            NC2064.2
052000     PERFORM FAIL.                                                NC2064.2
052100 QAL-WRITE-F1-2.                                                  NC2064.2
052200     PERFORM PRINT-DETAIL.                                        NC2064.2
052300*                                                                 NC2064.2
052400 QAL-INIT-F1-3.                                                   NC2064.2
052500     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
052600     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
052700     MOVE "QAL-TEST-F1-3" TO PAR-NAME.                            NC2064.2
052800 QAL-TEST-F1-3.                                                   NC2064.2
052900     IF TBL-LEVEL-0A IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2064.2
053000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
053100         TO "5A4A3A2B1A0A"                                        NC2064.2
053200         PERFORM PASS                                             NC2064.2
053300         GO TO QAL-WRITE-F1-3.                                    NC2064.2
053400     GO TO QAL-FAIL-F1-3.                                         NC2064.2
053500 QAL-DELETE-F1-3.                                                 NC2064.2
053600     PERFORM DE-LETE.                                             NC2064.2
053700     GO TO QAL-WRITE-F1-3.                                        NC2064.2
053800 QAL-FAIL-F1-3.                                                   NC2064.2
053900     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN     NC2064.2
054000         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
054100         COMPUTED-A.                                              NC2064.2
054200     MOVE "5A4A3A2B1A0A" TO CORRECT-A.                            NC2064.2
054300     PERFORM FAIL.                                                NC2064.2
054400 QAL-WRITE-F1-3.                                                  NC2064.2
054500     PERFORM PRINT-DETAIL.                                        NC2064.2
054600*                                                                 NC2064.2
054700 QAL-INIT-F1-4.                                                   NC2064.2
054800     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
054900     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
055000     MOVE "QAL-TEST-F1-4 " TO PAR-NAME.                           NC2064.2
055100 QAL-TEST-F1-4.                                                   NC2064.2
055200     IF TBL-LEVEL-0A OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
055300         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
055400         TO "5A4A3A2B1B0A"                                        NC2064.2
055500         PERFORM PASS                                             NC2064.2
055600         GO TO QAL-WRITE-F1-4.                                    NC2064.2
055700     GO TO QAL-FAIL-F1-4.                                         NC2064.2
055800 QAL-DELETE-F1-4.                                                 NC2064.2
055900     PERFORM DE-LETE.                                             NC2064.2
056000     GO TO QAL-WRITE-F1-4.                                        NC2064.2
056100 QAL-FAIL-F1-4.                                                   NC2064.2
056200     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
056300         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
056400         COMPUTED-A.                                              NC2064.2
056500     MOVE "5A4A3A2B1B0A" TO CORRECT-A.                            NC2064.2
056600     PERFORM FAIL.                                                NC2064.2
056700 QAL-WRITE-F1-4.                                                  NC2064.2
056800     PERFORM PRINT-DETAIL.                                        NC2064.2
056900*                                                                 NC2064.2
057000 QAL-INIT-F1-5.                                                   NC2064.2
057100     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
057200     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
057300     MOVE "QAL-TEST-F1-5 " TO PAR-NAME.                           NC2064.2
057400 QAL-TEST-F1-5.                                                   NC2064.2
057500     IF TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
057600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
057700         TO "5A4A3B2A1A0A"                                        NC2064.2
057800         PERFORM PASS                                             NC2064.2
057900         GO TO QAL-WRITE-F1-5.                                    NC2064.2
058000     GO TO QAL-FAIL-F1-5.                                         NC2064.2
058100 QAL-DELETE-F1-5.                                                 NC2064.2
058200     PERFORM DE-LETE.                                             NC2064.2
058300     GO TO QAL-WRITE-F1-5.                                        NC2064.2
058400 QAL-FAIL-F1-5.                                                   NC2064.2
058500     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF     NC2064.2
058600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
058700         COMPUTED-A.                                              NC2064.2
058800     MOVE "5A4A3B2A1A0A" TO CORRECT-A.                            NC2064.2
058900     PERFORM FAIL.                                                NC2064.2
059000 QAL-WRITE-F1-5.                                                  NC2064.2
059100     PERFORM PRINT-DETAIL.                                        NC2064.2
059200*                                                                 NC2064.2
059300 QAL-INIT-F1-6.                                                   NC2064.2
059400     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
059500     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
059600     MOVE "QAL-TEST-F1-6 " TO PAR-NAME.                           NC2064.2
059700 QAL-TEST-F1-6.                                                   NC2064.2
059800     IF TBL-LEVEL-0A OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2064.2
059900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
060000         TO "5A4A3B2A1B0A"                                        NC2064.2
060100         PERFORM PASS                                             NC2064.2
060200         GO TO QAL-WRITE-F1-6.                                    NC2064.2
060300     GO TO QAL-FAIL-F1-6.                                         NC2064.2
060400 QAL-DELETE-F1-6.                                                 NC2064.2
060500     PERFORM DE-LETE.                                             NC2064.2
060600     GO TO QAL-WRITE-F1-6.                                        NC2064.2
060700 QAL-FAIL-F1-6.                                                   NC2064.2
060800     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN     NC2064.2
060900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
061000         COMPUTED-A.                                              NC2064.2
061100     MOVE "5A4A3B2A1B0A" TO CORRECT-A.                            NC2064.2
061200     PERFORM FAIL.                                                NC2064.2
061300 QAL-WRITE-F1-6.                                                  NC2064.2
061400     PERFORM PRINT-DETAIL.                                        NC2064.2
061500*                                                                 NC2064.2
061600 QAL-INIT-F1-7.                                                   NC2064.2
061700     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
061800     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
061900     MOVE "QAL-TEST-F1-7 " TO PAR-NAME.                           NC2064.2
062000 QAL-TEST-F1-7.                                                   NC2064.2
062100     IF TBL-LEVEL-0A OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN       NC2064.2
062200         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
062300         TO "5A4A3B2B1A0A"                                        NC2064.2
062400         PERFORM PASS                                             NC2064.2
062500         GO TO QAL-WRITE-F1-7.                                    NC2064.2
062600     GO TO QAL-FAIL-F1-7.                                         NC2064.2
062700 QAL-DELETE-F1-7.                                                 NC2064.2
062800     PERFORM DE-LETE.                                             NC2064.2
062900     GO TO QAL-WRITE-F1-7.                                        NC2064.2
063000 QAL-FAIL-F1-7.                                                   NC2064.2
063100     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN     NC2064.2
063200         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
063300         COMPUTED-A.                                              NC2064.2
063400     MOVE "5A4A3B2B1A0A" TO CORRECT-A.                            NC2064.2
063500     PERFORM FAIL.                                                NC2064.2
063600 QAL-WRITE-F1-7.                                                  NC2064.2
063700     PERFORM PRINT-DETAIL.                                        NC2064.2
063800*                                                                 NC2064.2
063900 QAL-INIT-F1-8.                                                   NC2064.2
064000     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
064100     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
064200     MOVE "QAL-TEST-F1-8 " TO PAR-NAME.                           NC2064.2
064300 QAL-TEST-F1-8.                                                   NC2064.2
064400     IF TBL-LEVEL-0A IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN       NC2064.2
064500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
064600         TO "5A4A3B2B1B0A"                                        NC2064.2
064700         PERFORM PASS                                             NC2064.2
064800         GO TO QAL-WRITE-F1-8.                                    NC2064.2
064900     GO TO QAL-FAIL-F1-8.                                         NC2064.2
065000 QAL-DELETE-F1-8.                                                 NC2064.2
065100     PERFORM DE-LETE.                                             NC2064.2
065200     GO TO QAL-WRITE-F1-8.                                        NC2064.2
065300 QAL-FAIL-F1-8.                                                   NC2064.2
065400     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN     NC2064.2
065500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
065600         COMPUTED-A.                                              NC2064.2
065700     MOVE "5A4A3B2B1B0A" TO CORRECT-A.                            NC2064.2
065800     PERFORM FAIL.                                                NC2064.2
065900 QAL-WRITE-F1-8.                                                  NC2064.2
066000     PERFORM PRINT-DETAIL.                                        NC2064.2
066100*                                                                 NC2064.2
066200 QAL-INIT-F1-9.                                                   NC2064.2
066300     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
066400     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
066500     MOVE "QAL-TEST-F1-9 " TO PAR-NAME.                           NC2064.2
066600 QAL-TEST-F1-9.                                                   NC2064.2
066700     IF TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
066800         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
066900         TO "5A4B3A2A1A0A"                                        NC2064.2
067000         PERFORM PASS                                             NC2064.2
067100         GO TO QAL-WRITE-F1-9.                                    NC2064.2
067200     GO TO QAL-FAIL-F1-9.                                         NC2064.2
067300 QAL-DELETE-F1-9.                                                 NC2064.2
067400     PERFORM DE-LETE.                                             NC2064.2
067500     GO TO QAL-WRITE-F1-9.                                        NC2064.2
067600 QAL-FAIL-F1-9.                                                   NC2064.2
067700     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF     NC2064.2
067800         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
067900         COMPUTED-A.                                              NC2064.2
068000     MOVE "5A4B3A2A1A0A" TO CORRECT-A.                            NC2064.2
068100     PERFORM FAIL.                                                NC2064.2
068200 QAL-WRITE-F1-9.                                                  NC2064.2
068300     PERFORM PRINT-DETAIL.                                        NC2064.2
068400*                                                                 NC2064.2
068500 QAL-INIT-F1-10.                                                  NC2064.2
068600     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
068700     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
068800     MOVE "QAL-TEST-F1-10 " TO PAR-NAME.                          NC2064.2
068900 QAL-TEST-F1-10.                                                  NC2064.2
069000     IF TBL-LEVEL-0A IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF       NC2064.2
069100         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
069200         TO "5A4B3A2A1B0A"                                        NC2064.2
069300         PERFORM PASS                                             NC2064.2
069400         GO TO QAL-WRITE-F1-10.                                   NC2064.2
069500     GO TO QAL-FAIL-F1-10.                                        NC2064.2
069600 QAL-DELETE-F1-10.                                                NC2064.2
069700     PERFORM DE-LETE.                                             NC2064.2
069800     GO TO QAL-WRITE-F1-10.                                       NC2064.2
069900 QAL-FAIL-F1-10.                                                  NC2064.2
070000     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF     NC2064.2
070100         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
070200         COMPUTED-A.                                              NC2064.2
070300     MOVE "5A4B3A2A1B0A" TO CORRECT-A.                            NC2064.2
070400     PERFORM FAIL.                                                NC2064.2
070500 QAL-WRITE-F1-10.                                                 NC2064.2
070600     PERFORM PRINT-DETAIL.                                        NC2064.2
070700*                                                                 NC2064.2
070800 QAL-INIT-F1-11.                                                  NC2064.2
070900     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
071000     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
071100     MOVE "QAL-TEST-F1-11 " TO PAR-NAME.                          NC2064.2
071200 QAL-TEST-F1-11.                                                  NC2064.2
071300     IF TBL-LEVEL-0A IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF       NC2064.2
071400         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
071500         TO "5A4B3A2B1A0A"                                        NC2064.2
071600         PERFORM PASS                                             NC2064.2
071700         GO TO QAL-WRITE-F1-11.                                   NC2064.2
071800     GO TO QAL-FAIL-F1-11.                                        NC2064.2
071900 QAL-DELETE-F1-11.                                                NC2064.2
072000     PERFORM DE-LETE.                                             NC2064.2
072100     GO TO QAL-WRITE-F1-11.                                       NC2064.2
072200 QAL-FAIL-F1-11.                                                  NC2064.2
072300     MOVE "5A4B3A2B1A0A" TO CORRECT-A.                            NC2064.2
072400     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF     NC2064.2
072500         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
072600         COMPUTED-A.                                              NC2064.2
072700     PERFORM FAIL.                                                NC2064.2
072800 QAL-WRITE-F1-11.                                                 NC2064.2
072900     PERFORM PRINT-DETAIL.                                        NC2064.2
073000*                                                                 NC2064.2
073100 QAL-INIT-F1-12.                                                  NC2064.2
073200     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
073300     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
073400     MOVE "QAL-TEST-F1-12 " TO PAR-NAME.                          NC2064.2
073500 QAL-TEST-F1-12.                                                  NC2064.2
073600     IF TBL-LEVEL-0A OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
073700         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
073800         TO "5A4B3A2B1B0A"                                        NC2064.2
073900         PERFORM PASS                                             NC2064.2
074000         GO TO QAL-WRITE-F1-12.                                   NC2064.2
074100     GO TO QAL-FAIL-F1-12.                                        NC2064.2
074200 QAL-DELETE-F1-12.                                                NC2064.2
074300     PERFORM DE-LETE.                                             NC2064.2
074400     GO TO QAL-WRITE-F1-12.                                       NC2064.2
074500 QAL-FAIL-F1-12.                                                  NC2064.2
074600     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
074700         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
074800         COMPUTED-A.                                              NC2064.2
074900     MOVE "5A4B3A2B1B0A" TO CORRECT-A.                            NC2064.2
075000     PERFORM FAIL.                                                NC2064.2
075100 QAL-WRITE-F1-12.                                                 NC2064.2
075200     PERFORM PRINT-DETAIL.                                        NC2064.2
075300*                                                                 NC2064.2
075400 QAL-INIT-F1-13.                                                  NC2064.2
075500     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
075600     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
075700     MOVE "QAL-TEST-F1-13 " TO PAR-NAME.                          NC2064.2
075800 QAL-TEST-F1-13.                                                  NC2064.2
075900     IF TBL-LEVEL-0A OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF       NC2064.2
076000         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
076100         TO "5A4B3B2A1A0A"                                        NC2064.2
076200         PERFORM PASS                                             NC2064.2
076300         GO TO QAL-WRITE-F1-13.                                   NC2064.2
076400     GO TO QAL-FAIL-F1-13.                                        NC2064.2
076500 QAL-DELETE-F1-13.                                                NC2064.2
076600     PERFORM DE-LETE.                                             NC2064.2
076700     GO TO QAL-WRITE-F1-13.                                       NC2064.2
076800 QAL-FAIL-F1-13.                                                  NC2064.2
076900     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF     NC2064.2
077000         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2064.2
077100         TO COMPUTED-A.                                           NC2064.2
077200     MOVE "5A4B3B2A1A0A" TO CORRECT-A.                            NC2064.2
077300     PERFORM FAIL.                                                NC2064.2
077400 QAL-WRITE-F1-13.                                                 NC2064.2
077500     PERFORM PRINT-DETAIL.                                        NC2064.2
077600*                                                                 NC2064.2
077700 QAL-INIT-F1-14.                                                  NC2064.2
077800     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
077900     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
078000     MOVE "QAL-TEST-F1-14 " TO PAR-NAME.                          NC2064.2
078100 QAL-TEST-F1-14.                                                  NC2064.2
078200     IF TBL-LEVEL-0A IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN       NC2064.2
078300         TABLE-LEVEL-3B OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
078400         TO "5A4B3B2A1B0A"                                        NC2064.2
078500         PERFORM PASS                                             NC2064.2
078600         GO TO QAL-WRITE-F1-14.                                   NC2064.2
078700     GO TO QAL-FAIL-F1-14.                                        NC2064.2
078800 QAL-DELETE-F1-14.                                                NC2064.2
078900     PERFORM DE-LETE.                                             NC2064.2
079000     GO TO QAL-WRITE-F1-14.                                       NC2064.2
079100 QAL-FAIL-F1-14.                                                  NC2064.2
079200     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN     NC2064.2
079300         TABLE-LEVEL-3B OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
079400         COMPUTED-A.                                              NC2064.2
079500     MOVE "5A4B3B2A1B0A" TO CORRECT-A.                            NC2064.2
079600     PERFORM FAIL.                                                NC2064.2
079700 QAL-WRITE-F1-14.                                                 NC2064.2
079800     PERFORM PRINT-DETAIL.                                        NC2064.2
079900*                                                                 NC2064.2
080000 QAL-INIT-F1-15.                                                  NC2064.2
080100     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
080200     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
080300     MOVE "QAL-TEST-F1-15 " TO PAR-NAME.                          NC2064.2
080400 QAL-TEST-F1-15.                                                  NC2064.2
080500     IF TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2064.2
080600         TABLE-LEVEL-3B IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
080700         TO "5A4B3B2B1A0A"                                        NC2064.2
080800         PERFORM PASS                                             NC2064.2
080900         GO TO QAL-WRITE-F1-15.                                   NC2064.2
081000     GO TO QAL-FAIL-F1-15.                                        NC2064.2
081100 QAL-DELETE-F1-15.                                                NC2064.2
081200     PERFORM DE-LETE.                                             NC2064.2
081300     GO TO QAL-WRITE-F1-15.                                       NC2064.2
081400 QAL-FAIL-F1-15.                                                  NC2064.2
081500     MOVE "5A4B3B2B1A0A" TO CORRECT-A.                            NC2064.2
081600     MOVE TBL-LEVEL-0A OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN     NC2064.2
081700         TABLE-LEVEL-3B IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
081800         COMPUTED-A.                                              NC2064.2
081900     PERFORM FAIL.                                                NC2064.2
082000 QAL-WRITE-F1-15.                                                 NC2064.2
082100     PERFORM PRINT-DETAIL.                                        NC2064.2
082200*                                                                 NC2064.2
082300 QAL-INIT-F1-16.                                                  NC2064.2
082400     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
082500     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
082600     MOVE "QAL-TEST-F1-16 " TO PAR-NAME.                          NC2064.2
082700 QAL-TEST-F1-16.                                                  NC2064.2
082800     IF TBL-LEVEL-0A IN TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
082900         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
083000         TO "5A4B3B2B1B0A"                                        NC2064.2
083100         PERFORM PASS                                             NC2064.2
083200         GO TO QAL-WRITE-F1-16.                                   NC2064.2
083300     GO TO QAL-FAIL-F1-16.                                        NC2064.2
083400 QAL-DELETE-F1-16.                                                NC2064.2
083500     PERFORM DE-LETE.                                             NC2064.2
083600     GO TO QAL-WRITE-F1-16.                                       NC2064.2
083700 QAL-FAIL-F1-16.                                                  NC2064.2
083800     MOVE TBL-LEVEL-0A IN TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
083900         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
084000         COMPUTED-A.                                              NC2064.2
084100     MOVE "5A4B3B2B1B0A" TO CORRECT-A.                            NC2064.2
084200     PERFORM FAIL.                                                NC2064.2
084300 QAL-WRITE-F1-16.                                                 NC2064.2
084400     PERFORM PRINT-DETAIL.                                        NC2064.2
084500*                                                                 NC2064.2
084600 QAL-INIT-F1-17.                                                  NC2064.2
084700     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
084800     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
084900     MOVE "QAL-TEST-F1-17" TO PAR-NAME.                           NC2064.2
085000 QAL-TEST-F1-17.                                                  NC2064.2
085100     IF TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
085200         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
085300         TO "5A4A3A2A1A0B"                                        NC2064.2
085400         PERFORM PASS                                             NC2064.2
085500         GO TO QAL-WRITE-F1-17.                                   NC2064.2
085600     GO TO QAL-FAIL-F1-17.                                        NC2064.2
085700 QAL-DELETE-F1-17.                                                NC2064.2
085800     PERFORM DE-LETE.                                             NC2064.2
085900     GO TO QAL-WRITE-F1-17.                                       NC2064.2
086000 QAL-FAIL-F1-17.                                                  NC2064.2
086100     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF     NC2064.2
086200         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
086300         COMPUTED-A.                                              NC2064.2
086400     MOVE "5A4A3A2A1A0B" TO CORRECT-A.                            NC2064.2
086500     PERFORM FAIL.                                                NC2064.2
086600 QAL-WRITE-F1-17.                                                 NC2064.2
086700     PERFORM PRINT-DETAIL.                                        NC2064.2
086800*                                                                 NC2064.2
086900 QAL-INIT-F1-18.                                                  NC2064.2
087000     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
087100     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
087200     MOVE "QAL-TEST-F1-18" TO PAR-NAME.                           NC2064.2
087300 QAL-TEST-F1-18.                                                  NC2064.2
087400     IF TBL-LEVEL-0B IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2064.2
087500         TABLE-LEVEL-3A OF TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
087600         TO "5A4A3A2A1B0B"                                        NC2064.2
087700         PERFORM PASS                                             NC2064.2
087800         GO TO QAL-WRITE-F1-18.                                   NC2064.2
087900     GO TO QAL-FAIL-F1-18.                                        NC2064.2
088000 QAL-DELETE-F1-18.                                                NC2064.2
088100     PERFORM DE-LETE.                                             NC2064.2
088200     GO TO QAL-WRITE-F1-18.                                       NC2064.2
088300 QAL-FAIL-F1-18.                                                  NC2064.2
088400     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN     NC2064.2
088500         TABLE-LEVEL-3A OF TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
088600         COMPUTED-A.                                              NC2064.2
088700     MOVE "5A4A3A2A1B0B" TO CORRECT-A.                            NC2064.2
088800     PERFORM FAIL.                                                NC2064.2
088900 QAL-WRITE-F1-18.                                                 NC2064.2
089000     PERFORM PRINT-DETAIL.                                        NC2064.2
089100*                                                                 NC2064.2
089200 QAL-INIT-F1-19.                                                  NC2064.2
089300     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
089400     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
089500     MOVE "QAL-TEST-F1-19" TO PAR-NAME.                           NC2064.2
089600 QAL-TEST-F1-19.                                                  NC2064.2
089700     IF TBL-LEVEL-0B IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2064.2
089800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
089900         TO "5A4A3A2B1A0B"                                        NC2064.2
090000         PERFORM PASS                                             NC2064.2
090100         GO TO QAL-WRITE-F1-19.                                   NC2064.2
090200     GO TO QAL-FAIL-F1-19.                                        NC2064.2
090300 QAL-DELETE-F1-19.                                                NC2064.2
090400     PERFORM DE-LETE.                                             NC2064.2
090500     GO TO QAL-WRITE-F1-19.                                       NC2064.2
090600 QAL-FAIL-F1-19.                                                  NC2064.2
090700     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN     NC2064.2
090800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
090900         COMPUTED-A.                                              NC2064.2
091000     MOVE "5A4A3A2B1A0B" TO CORRECT-A.                            NC2064.2
091100     PERFORM FAIL.                                                NC2064.2
091200 QAL-WRITE-F1-19.                                                 NC2064.2
091300     PERFORM PRINT-DETAIL.                                        NC2064.2
091400*                                                                 NC2064.2
091500 QAL-INIT-F1-20.                                                  NC2064.2
091600     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
091700     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
091800     MOVE "QAL-TEST-F1-20" TO PAR-NAME.                           NC2064.2
091900 QAL-TEST-F1-20.                                                  NC2064.2
092000     IF TBL-LEVEL-0B OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
092100         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
092200         TO "5A4A3A2B1B0B"                                        NC2064.2
092300         PERFORM PASS                                             NC2064.2
092400         GO TO QAL-WRITE-F1-20.                                   NC2064.2
092500     GO TO QAL-FAIL-F1-20.                                        NC2064.2
092600 QAL-DELETE-F1-20.                                                NC2064.2
092700     PERFORM DE-LETE.                                             NC2064.2
092800     GO TO QAL-WRITE-F1-20.                                       NC2064.2
092900 QAL-FAIL-F1-20.                                                  NC2064.2
093000     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
093100         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
093200         COMPUTED-A.                                              NC2064.2
093300     MOVE "5A4A3A2B1B0B" TO CORRECT-A.                            NC2064.2
093400     PERFORM FAIL.                                                NC2064.2
093500 QAL-WRITE-F1-20.                                                 NC2064.2
093600     PERFORM PRINT-DETAIL.                                        NC2064.2
093700*                                                                 NC2064.2
093800 QAL-INIT-F1-21.                                                  NC2064.2
093900     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
094000     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
094100     MOVE "QAL-TEST-F1-21" TO PAR-NAME.                           NC2064.2
094200 QAL-TEST-F1-21.                                                  NC2064.2
094300     IF TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
094400         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
094500         TO "5A4A3B2A1A0B"                                        NC2064.2
094600         PERFORM PASS                                             NC2064.2
094700         GO TO QAL-WRITE-F1-21.                                   NC2064.2
094800     GO TO QAL-FAIL-F1-21.                                        NC2064.2
094900 QAL-DELETE-F1-21.                                                NC2064.2
095000     PERFORM DE-LETE.                                             NC2064.2
095100     GO TO QAL-WRITE-F1-21.                                       NC2064.2
095200 QAL-FAIL-F1-21.                                                  NC2064.2
095300     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF     NC2064.2
095400         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
095500         COMPUTED-A.                                              NC2064.2
095600     MOVE "5A4A3B2A1A0B" TO CORRECT-A.                            NC2064.2
095700     PERFORM FAIL.                                                NC2064.2
095800 QAL-WRITE-F1-21.                                                 NC2064.2
095900     PERFORM PRINT-DETAIL.                                        NC2064.2
096000*                                                                 NC2064.2
096100 QAL-INIT-F1-22.                                                  NC2064.2
096200     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
096300     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
096400     MOVE "QAL-TEST-F1-22" TO PAR-NAME.                           NC2064.2
096500 QAL-TEST-F1-22.                                                  NC2064.2
096600     IF TBL-LEVEL-0B OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2064.2
096700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
096800         TO "5A4A3B2A1B0B"                                        NC2064.2
096900         PERFORM PASS                                             NC2064.2
097000         GO TO QAL-WRITE-F1-22.                                   NC2064.2
097100     GO TO QAL-FAIL-F1-22.                                        NC2064.2
097200 QAL-DELETE-F1-22.                                                NC2064.2
097300     PERFORM DE-LETE.                                             NC2064.2
097400     GO TO QAL-WRITE-F1-22.                                       NC2064.2
097500 QAL-FAIL-F1-22.                                                  NC2064.2
097600     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN     NC2064.2
097700         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
097800         COMPUTED-A.                                              NC2064.2
097900     MOVE "5A4A3B2A1B0B" TO CORRECT-A.                            NC2064.2
098000     PERFORM FAIL.                                                NC2064.2
098100 QAL-WRITE-F1-22.                                                 NC2064.2
098200     PERFORM PRINT-DETAIL.                                        NC2064.2
098300*                                                                 NC2064.2
098400 QAL-INIT-F1-23.                                                  NC2064.2
098500     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
098600     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
098700     MOVE "QAL-TEST-F1-23" TO PAR-NAME.                           NC2064.2
098800 QAL-TEST-F1-23.                                                  NC2064.2
098900     IF TBL-LEVEL-0B OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN       NC2064.2
099000         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
099100         TO "5A4A3B2B1A0B"                                        NC2064.2
099200         PERFORM PASS                                             NC2064.2
099300         GO TO QAL-WRITE-F1-23.                                   NC2064.2
099400     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN     NC2064.2
099500         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
099600         COMPUTED-A.                                              NC2064.2
099700     MOVE "5A4A3B2B1A0B" TO CORRECT-A.                            NC2064.2
099800     PERFORM FAIL.                                                NC2064.2
099900     GO TO QAL-WRITE-F1-23.                                       NC2064.2
100000 QAL-DELETE-F1-23.                                                NC2064.2
100100     PERFORM DE-LETE.                                             NC2064.2
100200 QAL-WRITE-F1-23.                                                 NC2064.2
100300     PERFORM PRINT-DETAIL.                                        NC2064.2
100400*                                                                 NC2064.2
100500 QAL-INIT-F1-24.                                                  NC2064.2
100600     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
100700     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
100800     MOVE "QAL-TEST-F1-24" TO PAR-NAME.                           NC2064.2
100900 QAL-TEST-F1-24.                                                  NC2064.2
101000     IF TBL-LEVEL-0B IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN       NC2064.2
101100         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
101200         TO "5A4A3B2B1B0B"                                        NC2064.2
101300         PERFORM PASS                                             NC2064.2
101400         GO TO QAL-WRITE-F1-24.                                   NC2064.2
101500     GO TO QAL-FAIL-F1-24.                                        NC2064.2
101600 QAL-DELETE-F1-24.                                                NC2064.2
101700     PERFORM DE-LETE.                                             NC2064.2
101800     GO TO QAL-WRITE-F1-24.                                       NC2064.2
101900 QAL-FAIL-F1-24.                                                  NC2064.2
102000     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN     NC2064.2
102100         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
102200         COMPUTED-A.                                              NC2064.2
102300     MOVE "5A4A3B2B1B0B" TO CORRECT-A.                            NC2064.2
102400     PERFORM FAIL.                                                NC2064.2
102500 QAL-WRITE-F1-24.                                                 NC2064.2
102600     PERFORM PRINT-DETAIL.                                        NC2064.2
102700*                                                                 NC2064.2
102800 QAL-INIT-F1-25.                                                  NC2064.2
102900     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
103000     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
103100     MOVE "QAL-TEST-F1-25" TO PAR-NAME.                           NC2064.2
103200 QAL-TEST-F1-25.                                                  NC2064.2
103300     IF TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
103400         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
103500         TO "5A4B3A2A1A0B"                                        NC2064.2
103600         PERFORM PASS                                             NC2064.2
103700         GO TO QAL-WRITE-F1-25.                                   NC2064.2
103800     GO TO QAL-FAIL-F1-25.                                        NC2064.2
103900 QAL-DELETE-F1-25.                                                NC2064.2
104000     PERFORM DE-LETE.                                             NC2064.2
104100     GO TO QAL-WRITE-F1-25.                                       NC2064.2
104200 QAL-FAIL-F1-25.                                                  NC2064.2
104300     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF     NC2064.2
104400         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
104500         COMPUTED-A.                                              NC2064.2
104600     MOVE "5A4B3A2A1A0B" TO CORRECT-A.                            NC2064.2
104700     PERFORM FAIL.                                                NC2064.2
104800 QAL-WRITE-F1-25.                                                 NC2064.2
104900     PERFORM PRINT-DETAIL.                                        NC2064.2
105000*                                                                 NC2064.2
105100 QAL-INIT-F1-26.                                                  NC2064.2
105200     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
105300     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
105400     MOVE "QAL-TEST-F1-26" TO PAR-NAME.                           NC2064.2
105500 QAL-TEST-F1-26.                                                  NC2064.2
105600     IF TBL-LEVEL-0B IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF       NC2064.2
105700         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
105800         TO "5A4B3A2A1B0B"                                        NC2064.2
105900         PERFORM PASS                                             NC2064.2
106000         GO TO QAL-WRITE-F1-26.                                   NC2064.2
106100     GO TO QAL-FAIL-F1-26.                                        NC2064.2
106200 QAL-DELETE-F1-26.                                                NC2064.2
106300     PERFORM DE-LETE.                                             NC2064.2
106400     GO TO QAL-WRITE-F1-26.                                       NC2064.2
106500 QAL-FAIL-F1-26.                                                  NC2064.2
106600     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF     NC2064.2
106700         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
106800         COMPUTED-A.                                              NC2064.2
106900     MOVE "5A4B3A2A1B0B" TO CORRECT-A.                            NC2064.2
107000     PERFORM FAIL.                                                NC2064.2
107100 QAL-WRITE-F1-26.                                                 NC2064.2
107200     PERFORM PRINT-DETAIL.                                        NC2064.2
107300*                                                                 NC2064.2
107400 QAL-INIT-F1-27.                                                  NC2064.2
107500     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
107600     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
107700     MOVE "QAL-TEST-F1-27" TO PAR-NAME.                           NC2064.2
107800 QAL-TEST-F1-27.                                                  NC2064.2
107900     IF TBL-LEVEL-0B IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF       NC2064.2
108000         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
108100         TO "5A4B3A2B1A0B"                                        NC2064.2
108200         PERFORM PASS                                             NC2064.2
108300         GO TO QAL-WRITE-F1-27.                                   NC2064.2
108400     GO TO QAL-FAIL-F1-27.                                        NC2064.2
108500 QAL-DELETE-F1-27.                                                NC2064.2
108600     PERFORM DE-LETE.                                             NC2064.2
108700     GO TO QAL-WRITE-F1-27.                                       NC2064.2
108800 QAL-FAIL-F1-27.                                                  NC2064.2
108900     MOVE "5A4B3A2B1A0B" TO CORRECT-A.                            NC2064.2
109000     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF     NC2064.2
109100         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
109200         COMPUTED-A.                                              NC2064.2
109300     PERFORM FAIL.                                                NC2064.2
109400 QAL-WRITE-F1-27.                                                 NC2064.2
109500     PERFORM PRINT-DETAIL.                                        NC2064.2
109600*                                                                 NC2064.2
109700 QAL-INIT-F1-28.                                                  NC2064.2
109800     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
109900     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
110000     MOVE "QAL-TEST-F1-28" TO PAR-NAME.                           NC2064.2
110100 QAL-TEST-F1-28.                                                  NC2064.2
110200     IF TBL-LEVEL-0B OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
110300         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
110400         TO "5A4B3A2B1B0B"                                        NC2064.2
110500         PERFORM PASS                                             NC2064.2
110600         GO TO QAL-WRITE-F1-28.                                   NC2064.2
110700     GO TO QAL-FAIL-F1-28.                                        NC2064.2
110800 QAL-DELETE-F1-28.                                                NC2064.2
110900     PERFORM DE-LETE.                                             NC2064.2
111000     GO TO QAL-WRITE-F1-28.                                       NC2064.2
111100 QAL-FAIL-F1-28.                                                  NC2064.2
111200     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
111300         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
111400         COMPUTED-A.                                              NC2064.2
111500     MOVE "5A4B3A2B1B0B" TO CORRECT-A.                            NC2064.2
111600     PERFORM FAIL.                                                NC2064.2
111700 QAL-WRITE-F1-28.                                                 NC2064.2
111800     PERFORM PRINT-DETAIL.                                        NC2064.2
111900*                                                                 NC2064.2
112000 QAL-INIT-F1-29.                                                  NC2064.2
112100     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
112200     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
112300     MOVE "QAL-TEST-F1-29" TO PAR-NAME.                           NC2064.2
112400 QAL-TEST-F1-29.                                                  NC2064.2
112500     IF TBL-LEVEL-0B OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF       NC2064.2
112600         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
112700         TO "5A4B3B2A1A0B"                                        NC2064.2
112800         PERFORM PASS                                             NC2064.2
112900         GO TO QAL-WRITE-F1-29.                                   NC2064.2
113000     GO TO QAL-FAIL-F1-29.                                        NC2064.2
113100 QAL-DELETE-F1-29.                                                NC2064.2
113200     PERFORM DE-LETE.                                             NC2064.2
113300     GO TO QAL-WRITE-F1-29.                                       NC2064.2
113400 QAL-FAIL-F1-29.                                                  NC2064.2
113500     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF     NC2064.2
113600         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2064.2
113700         TO COMPUTED-A.                                           NC2064.2
113800     MOVE "5A4B3B2A1A0B" TO CORRECT-A.                            NC2064.2
113900     PERFORM FAIL.                                                NC2064.2
114000 QAL-WRITE-F1-29.                                                 NC2064.2
114100     PERFORM PRINT-DETAIL.                                        NC2064.2
114200*                                                                 NC2064.2
114300 QAL-INIT-F1-30.                                                  NC2064.2
114400     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
114500     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
114600     MOVE "QAL-TEST-F1-30" TO PAR-NAME.                           NC2064.2
114700 QAL-TEST-F1-30.                                                  NC2064.2
114800     IF TBL-LEVEL-0B IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN       NC2064.2
114900         TABLE-LEVEL-3B OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
115000         TO "5A4B3B2A1B0B"                                        NC2064.2
115100         PERFORM PASS                                             NC2064.2
115200         GO TO QAL-WRITE-F1-30.                                   NC2064.2
115300     GO TO QAL-FAIL-F1-30.                                        NC2064.2
115400 QAL-DELETE-F1-30.                                                NC2064.2
115500     PERFORM DE-LETE.                                             NC2064.2
115600     GO TO QAL-WRITE-F1-30.                                       NC2064.2
115700 QAL-FAIL-F1-30.                                                  NC2064.2
115800     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN     NC2064.2
115900         TABLE-LEVEL-3B OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
116000         COMPUTED-A.                                              NC2064.2
116100     MOVE "5A4B3B2A1B0B" TO CORRECT-A.                            NC2064.2
116200     PERFORM FAIL.                                                NC2064.2
116300 QAL-WRITE-F1-30.                                                 NC2064.2
116400     PERFORM PRINT-DETAIL.                                        NC2064.2
116500*                                                                 NC2064.2
116600 QAL-INIT-F1-31.                                                  NC2064.2
116700     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
116800     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
116900     MOVE "QAL-TEST-F1-31" TO PAR-NAME.                           NC2064.2
117000 QAL-TEST-F1-31.                                                  NC2064.2
117100     IF TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2064.2
117200         TABLE-LEVEL-3B IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
117300         TO "5A4B3B2B1A0B"                                        NC2064.2
117400         PERFORM PASS                                             NC2064.2
117500         GO TO QAL-WRITE-F1-31.                                   NC2064.2
117600     GO TO QAL-FAIL-F1-31.                                        NC2064.2
117700 QAL-DELETE-F1-31.                                                NC2064.2
117800     PERFORM DE-LETE.                                             NC2064.2
117900     GO TO QAL-WRITE-F1-31.                                       NC2064.2
118000 QAL-FAIL-F1-31.                                                  NC2064.2
118100     MOVE "5A4B3B2B1A0B" TO CORRECT-A.                            NC2064.2
118200     MOVE TBL-LEVEL-0B OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN     NC2064.2
118300         TABLE-LEVEL-3B IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
118400         COMPUTED-A.                                              NC2064.2
118500     PERFORM FAIL.                                                NC2064.2
118600 QAL-WRITE-F1-31.                                                 NC2064.2
118700     PERFORM PRINT-DETAIL.                                        NC2064.2
118800*                                                                 NC2064.2
118900 QAL-INIT-F1-32.                                                  NC2064.2
119000     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
119100     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
119200     MOVE "QAL-TEST-F1-32" TO PAR-NAME.                           NC2064.2
119300 QAL-TEST-F1-32.                                                  NC2064.2
119400     IF TBL-LEVEL-0B IN TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
119500         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
119600         TO "5A4B3B2B1B0B"                                        NC2064.2
119700         PERFORM PASS                                             NC2064.2
119800         GO TO QAL-WRITE-F1-32.                                   NC2064.2
119900     GO TO QAL-FAIL-F1-32.                                        NC2064.2
120000 QAL-DELETE-F1-32.                                                NC2064.2
120100     PERFORM DE-LETE.                                             NC2064.2
120200     GO TO QAL-WRITE-F1-32.                                       NC2064.2
120300 QAL-FAIL-F1-32.                                                  NC2064.2
120400     MOVE TBL-LEVEL-0B IN TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
120500         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
120600         COMPUTED-A.                                              NC2064.2
120700     MOVE "5A4B3B2B1B0B" TO CORRECT-A.                            NC2064.2
120800     PERFORM FAIL.                                                NC2064.2
120900 QAL-WRITE-F1-32.                                                 NC2064.2
121000     PERFORM PRINT-DETAIL.                                        NC2064.2
121100*                                                                 NC2064.2
121200 QAL-INIT-F1-33.                                                  NC2064.2
121300     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
121400     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
121500     MOVE "QAL-TEST-F1-33" TO PAR-NAME.                           NC2064.2
121600 QAL-TEST-F1-33.                                                  NC2064.2
121700     IF TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
121800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
121900         TO "5A4A3A2A1A0C"                                        NC2064.2
122000         PERFORM PASS                                             NC2064.2
122100         GO TO QAL-WRITE-F1-33.                                   NC2064.2
122200     GO TO QAL-FAIL-F1-33.                                        NC2064.2
122300 QAL-DELETE-F1-33.                                                NC2064.2
122400     PERFORM DE-LETE.                                             NC2064.2
122500     GO TO QAL-WRITE-F1-33.                                       NC2064.2
122600 QAL-FAIL-F1-33.                                                  NC2064.2
122700     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF     NC2064.2
122800         TABLE-LEVEL-3A IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
122900         COMPUTED-A.                                              NC2064.2
123000     MOVE "5A4A3A2A1A0C" TO CORRECT-A.                            NC2064.2
123100     PERFORM FAIL.                                                NC2064.2
123200 QAL-WRITE-F1-33.                                                 NC2064.2
123300     PERFORM PRINT-DETAIL.                                        NC2064.2
123400*                                                                 NC2064.2
123500 QAL-INIT-F1-34.                                                  NC2064.2
123600     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
123700     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
123800     MOVE "QAL-TEST-F1-34" TO PAR-NAME.                           NC2064.2
123900 QAL-TEST-F1-34.                                                  NC2064.2
124000     IF TBL-LEVEL-0C IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2064.2
124100         TABLE-LEVEL-3A OF TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
124200         TO "5A4A3A2A1B0C"                                        NC2064.2
124300         PERFORM PASS                                             NC2064.2
124400         GO TO QAL-WRITE-F1-34.                                   NC2064.2
124500     GO TO QAL-FAIL-F1-34.                                        NC2064.2
124600 QAL-DELETE-F1-34.                                                NC2064.2
124700     PERFORM DE-LETE.                                             NC2064.2
124800     GO TO QAL-WRITE-F1-34.                                       NC2064.2
124900 QAL-FAIL-F1-34.                                                  NC2064.2
125000     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN     NC2064.2
125100         TABLE-LEVEL-3A OF TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
125200         COMPUTED-A.                                              NC2064.2
125300     MOVE "5A4A3A2A1B0C" TO CORRECT-A.                            NC2064.2
125400     PERFORM FAIL.                                                NC2064.2
125500 QAL-WRITE-F1-34.                                                 NC2064.2
125600     PERFORM PRINT-DETAIL.                                        NC2064.2
125700*                                                                 NC2064.2
125800 QAL-INIT-F1-35.                                                  NC2064.2
125900     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
126000     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
126100     MOVE "QAL-TEST-F1-35" TO PAR-NAME.                           NC2064.2
126200 QAL-TEST-F1-35.                                                  NC2064.2
126300     IF TBL-LEVEL-0C IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2064.2
126400         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
126500         TO "5A4A3A2B1A0C"                                        NC2064.2
126600         PERFORM PASS                                             NC2064.2
126700         GO TO QAL-WRITE-F1-35.                                   NC2064.2
126800     GO TO QAL-FAIL-F1-35.                                        NC2064.2
126900 QAL-DELETE-F1-35.                                                NC2064.2
127000     PERFORM DE-LETE.                                             NC2064.2
127100     GO TO QAL-WRITE-F1-35.                                       NC2064.2
127200 QAL-FAIL-F1-35.                                                  NC2064.2
127300     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN     NC2064.2
127400         TABLE-LEVEL-3A IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
127500         COMPUTED-A.                                              NC2064.2
127600     MOVE "5A4A3A2B1A0C" TO CORRECT-A.                            NC2064.2
127700     PERFORM FAIL.                                                NC2064.2
127800 QAL-WRITE-F1-35.                                                 NC2064.2
127900     PERFORM PRINT-DETAIL.                                        NC2064.2
128000*                                                                 NC2064.2
128100 QAL-INIT-F1-36.                                                  NC2064.2
128200     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
128300     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
128400     MOVE "QAL-TEST-F1-36" TO PAR-NAME.                           NC2064.2
128500 QAL-TEST-F1-36.                                                  NC2064.2
128600     IF TBL-LEVEL-0C OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
128700         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
128800         TO "5A4A3A2B1B0C"                                        NC2064.2
128900         PERFORM PASS                                             NC2064.2
129000         GO TO QAL-WRITE-F1-36.                                   NC2064.2
129100     GO TO QAL-FAIL-F1-36.                                        NC2064.2
129200 QAL-DELETE-F1-36.                                                NC2064.2
129300     PERFORM DE-LETE.                                             NC2064.2
129400     GO TO QAL-WRITE-F1-36.                                       NC2064.2
129500 QAL-FAIL-F1-36.                                                  NC2064.2
129600     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
129700         TABLE-LEVEL-3A OF TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
129800         COMPUTED-A.                                              NC2064.2
129900     MOVE "5A4A3A2B1B0C" TO CORRECT-A.                            NC2064.2
130000     PERFORM FAIL.                                                NC2064.2
130100 QAL-WRITE-F1-36.                                                 NC2064.2
130200     PERFORM PRINT-DETAIL.                                        NC2064.2
130300*                                                                 NC2064.2
130400 QAL-INIT-F1-37.                                                  NC2064.2
130500     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
130600     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
130700     MOVE "QAL-TEST-F1-37" TO PAR-NAME.                           NC2064.2
130800 QAL-TEST-F1-37.                                                  NC2064.2
130900     IF TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
131000         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
131100         TO "5A4A3B2A1A0C"                                        NC2064.2
131200         PERFORM PASS                                             NC2064.2
131300         GO TO QAL-WRITE-F1-37.                                   NC2064.2
131400     GO TO QAL-FAIL-F1-37.                                        NC2064.2
131500 QAL-DELETE-F1-37.                                                NC2064.2
131600     PERFORM DE-LETE.                                             NC2064.2
131700     GO TO QAL-WRITE-F1-37.                                       NC2064.2
131800 QAL-FAIL-F1-37.                                                  NC2064.2
131900     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF     NC2064.2
132000         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
132100         COMPUTED-A.                                              NC2064.2
132200     MOVE "5A4A3B2A1A0C" TO CORRECT-A.                            NC2064.2
132300     PERFORM FAIL.                                                NC2064.2
132400 QAL-WRITE-F1-37.                                                 NC2064.2
132500     PERFORM PRINT-DETAIL.                                        NC2064.2
132600*                                                                 NC2064.2
132700 QAL-INIT-F1-38.                                                  NC2064.2
132800     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
132900     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
133000     MOVE "QAL-TEST-F1-38" TO PAR-NAME.                           NC2064.2
133100 QAL-TEST-F1-38.                                                  NC2064.2
133200     IF TBL-LEVEL-0C OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN       NC2064.2
133300         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
133400         TO "5A4A3B2A1B0C"                                        NC2064.2
133500         PERFORM PASS                                             NC2064.2
133600         GO TO QAL-WRITE-F1-38.                                   NC2064.2
133700     GO TO QAL-FAIL-F1-38.                                        NC2064.2
133800 QAL-DELETE-F1-38.                                                NC2064.2
133900     PERFORM DE-LETE.                                             NC2064.2
134000     GO TO QAL-WRITE-F1-38.                                       NC2064.2
134100 QAL-FAIL-F1-38.                                                  NC2064.2
134200     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1B OF TABLE-LEVEL-2A IN     NC2064.2
134300         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
134400         COMPUTED-A.                                              NC2064.2
134500     MOVE "5A4A3B2A1B0C" TO CORRECT-A.                            NC2064.2
134600     PERFORM FAIL.                                                NC2064.2
134700 QAL-WRITE-F1-38.                                                 NC2064.2
134800     PERFORM PRINT-DETAIL.                                        NC2064.2
134900*                                                                 NC2064.2
135000 QAL-INIT-F1-39.                                                  NC2064.2
135100     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
135200     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
135300     MOVE "QAL-TEST-F1-39" TO PAR-NAME.                           NC2064.2
135400 QAL-TEST-F1-39.                                                  NC2064.2
135500     IF TBL-LEVEL-0C OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN       NC2064.2
135600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A EQUAL NC2064.2
135700         TO "5A4A3B2B1A0C"                                        NC2064.2
135800         PERFORM PASS                                             NC2064.2
135900         GO TO QAL-WRITE-F1-39.                                   NC2064.2
136000     GO TO QAL-FAIL-F1-39.                                        NC2064.2
136100 QAL-DELETE-F1-39.                                                NC2064.2
136200     PERFORM DE-LETE.                                             NC2064.2
136300     GO TO QAL-WRITE-F1-39.                                       NC2064.2
136400 QAL-FAIL-F1-39.                                                  NC2064.2
136500     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1A OF TABLE-LEVEL-2B IN     NC2064.2
136600         TABLE-LEVEL-3B IN TABLE-LEVEL-4A IN TABLE-LEVEL-5A TO    NC2064.2
136700         COMPUTED-A.                                              NC2064.2
136800     MOVE "5A4A3B2B1A0C" TO CORRECT-A.                            NC2064.2
136900     PERFORM FAIL.                                                NC2064.2
137000 QAL-WRITE-F1-39.                                                 NC2064.2
137100     PERFORM PRINT-DETAIL.                                        NC2064.2
137200*                                                                 NC2064.2
137300 QAL-INIT-F1-40.                                                  NC2064.2
137400     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
137500     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
137600     MOVE "QAL-TEST-F1-40" TO PAR-NAME.                           NC2064.2
137700 QAL-TEST-F1-40.                                                  NC2064.2
137800     IF TBL-LEVEL-0C IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN       NC2064.2
137900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A EQUAL NC2064.2
138000         TO "5A4A3B2B1B0C"                                        NC2064.2
138100         PERFORM PASS                                             NC2064.2
138200         GO TO QAL-WRITE-F1-40.                                   NC2064.2
138300     GO TO QAL-FAIL-F1-40.                                        NC2064.2
138400 QAL-DELETE-F1-40.                                                NC2064.2
138500     PERFORM DE-LETE.                                             NC2064.2
138600     GO TO QAL-WRITE-F1-40.                                       NC2064.2
138700 QAL-FAIL-F1-40.                                                  NC2064.2
138800     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1B IN TABLE-LEVEL-2B IN     NC2064.2
138900         TABLE-LEVEL-3B IN TABLE-LEVEL-4A OF TABLE-LEVEL-5A TO    NC2064.2
139000         COMPUTED-A.                                              NC2064.2
139100     MOVE "5A4A3B2B1B0C" TO CORRECT-A.                            NC2064.2
139200     PERFORM FAIL.                                                NC2064.2
139300 QAL-WRITE-F1-40.                                                 NC2064.2
139400     PERFORM PRINT-DETAIL.                                        NC2064.2
139500*                                                                 NC2064.2
139600 QAL-INIT-F1-41.                                                  NC2064.2
139700     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
139800     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
139900     MOVE "QAL-TEST-F1-41" TO PAR-NAME.                           NC2064.2
140000 QAL-TEST-F1-41.                                                  NC2064.2
140100     IF TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2A OF       NC2064.2
140200         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
140300         TO "5A4B3A2A1A0C"                                        NC2064.2
140400         PERFORM PASS                                             NC2064.2
140500         GO TO QAL-WRITE-F1-41.                                   NC2064.2
140600     GO TO QAL-FAIL-F1-41.                                        NC2064.2
140700 QAL-DELETE-F1-41.                                                NC2064.2
140800     PERFORM DE-LETE.                                             NC2064.2
140900     GO TO QAL-WRITE-F1-41.                                       NC2064.2
141000 QAL-FAIL-F1-41.                                                  NC2064.2
141100     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF     NC2064.2
141200         TABLE-LEVEL-3A OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
141300         COMPUTED-A.                                              NC2064.2
141400     MOVE "5A4B3A2A1A0C" TO CORRECT-A.                            NC2064.2
141500     PERFORM FAIL.                                                NC2064.2
141600 QAL-WRITE-F1-41.                                                 NC2064.2
141700     PERFORM PRINT-DETAIL.                                        NC2064.2
141800*                                                                 NC2064.2
141900 QAL-INIT-F1-42.                                                  NC2064.2
142000     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
142100     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
142200     MOVE "QAL-TEST-F1-42" TO PAR-NAME.                           NC2064.2
142300 QAL-TEST-F1-42.                                                  NC2064.2
142400     IF TBL-LEVEL-0C IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF       NC2064.2
142500         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
142600         TO "5A4B3A2A1B0C"                                        NC2064.2
142700         PERFORM PASS                                             NC2064.2
142800         GO TO QAL-WRITE-F1-42.                                   NC2064.2
142900     GO TO QAL-FAIL-F1-42.                                        NC2064.2
143000 QAL-DELETE-F1-42.                                                NC2064.2
143100     PERFORM DE-LETE.                                             NC2064.2
143200     GO TO QAL-WRITE-F1-42.                                       NC2064.2
143300 QAL-FAIL-F1-42.                                                  NC2064.2
143400     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1B OF TABLE-LEVEL-2A OF     NC2064.2
143500         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
143600         COMPUTED-A.                                              NC2064.2
143700     MOVE "5A4B3A2A1B0C" TO CORRECT-A.                            NC2064.2
143800     PERFORM FAIL.                                                NC2064.2
143900 QAL-WRITE-F1-42.                                                 NC2064.2
144000     PERFORM PRINT-DETAIL.                                        NC2064.2
144100*                                                                 NC2064.2
144200 QAL-INIT-F1-43.                                                  NC2064.2
144300     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
144400     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
144500     MOVE "QAL-TEST-F1-43" TO PAR-NAME.                           NC2064.2
144600 QAL-TEST-F1-43.                                                  NC2064.2
144700     IF TBL-LEVEL-0C IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF       NC2064.2
144800         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
144900         TO "5A4B3A2B1A0C"                                        NC2064.2
145000         PERFORM PASS                                             NC2064.2
145100         GO TO QAL-WRITE-F1-43.                                   NC2064.2
145200     GO TO QAL-FAIL-F1-43.                                        NC2064.2
145300 QAL-DELETE-F1-43.                                                NC2064.2
145400     PERFORM DE-LETE.                                             NC2064.2
145500     GO TO QAL-WRITE-F1-43.                                       NC2064.2
145600 QAL-FAIL-F1-43.                                                  NC2064.2
145700     MOVE "5A4B3A2B1A0C" TO CORRECT-A.                            NC2064.2
145800     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1A OF TABLE-LEVEL-2B OF     NC2064.2
145900         TABLE-LEVEL-3A OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
146000         COMPUTED-A.                                              NC2064.2
146100     PERFORM FAIL.                                                NC2064.2
146200 QAL-WRITE-F1-43.                                                 NC2064.2
146300     PERFORM PRINT-DETAIL.                                        NC2064.2
146400*                                                                 NC2064.2
146500 QAL-INIT-F1-44.                                                  NC2064.2
146600     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
146700     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
146800     MOVE "QAL-TEST-F1-44" TO PAR-NAME.                           NC2064.2
146900 QAL-TEST-F1-44.                                                  NC2064.2
147000     IF TBL-LEVEL-0C OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
147100         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
147200         TO "5A4B3A2B1B0C"                                        NC2064.2
147300         PERFORM PASS                                             NC2064.2
147400         GO TO QAL-WRITE-F1-44.                                   NC2064.2
147500     GO TO QAL-FAIL-F1-44.                                        NC2064.2
147600 QAL-DELETE-F1-44.                                                NC2064.2
147700     PERFORM DE-LETE.                                             NC2064.2
147800     GO TO QAL-WRITE-F1-44.                                       NC2064.2
147900 QAL-FAIL-F1-44.                                                  NC2064.2
148000     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
148100         TABLE-LEVEL-3A IN TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
148200         COMPUTED-A.                                              NC2064.2
148300     MOVE "5A4B3A2B1B0C" TO CORRECT-A.                            NC2064.2
148400     PERFORM FAIL.                                                NC2064.2
148500 QAL-WRITE-F1-44.                                                 NC2064.2
148600     PERFORM PRINT-DETAIL.                                        NC2064.2
148700*                                                                 NC2064.2
148800 QAL-INIT-F1-45.                                                  NC2064.2
148900     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
149000     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
149100     MOVE "QAL-TEST-F1-45" TO PAR-NAME.                           NC2064.2
149200 QAL-TEST-F1-45.                                                  NC2064.2
149300     IF TBL-LEVEL-0C OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF       NC2064.2
149400         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
149500         TO "5A4B3B2A1A0C"                                        NC2064.2
149600         PERFORM PASS                                             NC2064.2
149700         GO TO QAL-WRITE-F1-45.                                   NC2064.2
149800     GO TO QAL-FAIL-F1-45.                                        NC2064.2
149900 QAL-DELETE-F1-45.                                                NC2064.2
150000     PERFORM DE-LETE.                                             NC2064.2
150100     GO TO QAL-WRITE-F1-45.                                       NC2064.2
150200 QAL-FAIL-F1-45.                                                  NC2064.2
150300     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1A OF TABLE-LEVEL-2A OF     NC2064.2
150400         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A       NC2064.2
150500         TO COMPUTED-A.                                           NC2064.2
150600     MOVE "5A4B3B2A1A0C" TO CORRECT-A.                            NC2064.2
150700     PERFORM FAIL.                                                NC2064.2
150800 QAL-WRITE-F1-45.                                                 NC2064.2
150900     PERFORM PRINT-DETAIL.                                        NC2064.2
151000*                                                                 NC2064.2
151100 QAL-INIT-F1-46.                                                  NC2064.2
151200     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
151300     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
151400     MOVE "QAL-TEST-F1-46" TO PAR-NAME.                           NC2064.2
151500 QAL-TEST-F1-46.                                                  NC2064.2
151600     IF TBL-LEVEL-0C IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN       NC2064.2
151700         TABLE-LEVEL-3B OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
151800         TO "5A4B3B2A1B0C"                                        NC2064.2
151900         PERFORM PASS                                             NC2064.2
152000         GO TO QAL-WRITE-F1-46.                                   NC2064.2
152100     GO TO QAL-FAIL-F1-46.                                        NC2064.2
152200 QAL-DELETE-F1-46.                                                NC2064.2
152300     PERFORM DE-LETE.                                             NC2064.2
152400     GO TO QAL-WRITE-F1-46.                                       NC2064.2
152500 QAL-FAIL-F1-46.                                                  NC2064.2
152600     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1B IN TABLE-LEVEL-2A IN     NC2064.2
152700         TABLE-LEVEL-3B OF TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
152800         COMPUTED-A.                                              NC2064.2
152900     MOVE "5A4B3B2A1B0C" TO CORRECT-A.                            NC2064.2
153000     PERFORM FAIL.                                                NC2064.2
153100 QAL-WRITE-F1-46.                                                 NC2064.2
153200     PERFORM PRINT-DETAIL.                                        NC2064.2
153300*                                                                 NC2064.2
153400 QAL-INIT-F1-47.                                                  NC2064.2
153500     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
153600     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
153700     MOVE "QAL-TEST-F1-47" TO PAR-NAME.                           NC2064.2
153800 QAL-TEST-F1-47.                                                  NC2064.2
153900     IF TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN       NC2064.2
154000         TABLE-LEVEL-3B IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A EQUAL NC2064.2
154100         TO "5A4B3B2B1A0C"                                        NC2064.2
154200         PERFORM PASS                                             NC2064.2
154300         GO TO QAL-WRITE-F1-47.                                   NC2064.2
154400     GO TO QAL-FAIL-F1-47.                                        NC2064.2
154500 QAL-DELETE-F1-47.                                                NC2064.2
154600     PERFORM DE-LETE.                                             NC2064.2
154700     GO TO QAL-WRITE-F1-47.                                       NC2064.2
154800 QAL-FAIL-F1-47.                                                  NC2064.2
154900     MOVE "5A4B3B2B1A0C" TO CORRECT-A.                            NC2064.2
155000     MOVE TBL-LEVEL-0C OF TABLE-LEVEL-1A IN TABLE-LEVEL-2B IN     NC2064.2
155100         TABLE-LEVEL-3B IN TABLE-LEVEL-4B IN TABLE-LEVEL-5A TO    NC2064.2
155200         COMPUTED-A.                                              NC2064.2
155300     PERFORM FAIL.                                                NC2064.2
155400 QAL-WRITE-F1-47.                                                 NC2064.2
155500     PERFORM PRINT-DETAIL.                                        NC2064.2
155600*                                                                 NC2064.2
155700 QAL-INIT-F1-48.                                                  NC2064.2
155800     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
155900     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
156000     MOVE "QAL-TEST-F1-48" TO PAR-NAME.                           NC2064.2
156100 QAL-TEST-F1-48.                                                  NC2064.2
156200     IF TBL-LEVEL-0C IN TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF       NC2064.2
156300         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A EQUAL NC2064.2
156400         TO "5A4B3B2B1B0C"                                        NC2064.2
156500         PERFORM PASS                                             NC2064.2
156600         GO TO QAL-WRITE-F1-48.                                   NC2064.2
156700     GO TO QAL-FAIL-F1-48.                                        NC2064.2
156800 QAL-DELETE-F1-48.                                                NC2064.2
156900     PERFORM DE-LETE.                                             NC2064.2
157000     GO TO QAL-WRITE-F1-48.                                       NC2064.2
157100 QAL-FAIL-F1-48.                                                  NC2064.2
157200     MOVE TBL-LEVEL-0C IN TABLE-LEVEL-1B OF TABLE-LEVEL-2B OF     NC2064.2
157300         TABLE-LEVEL-3B OF TABLE-LEVEL-4B OF TABLE-LEVEL-5A TO    NC2064.2
157400         COMPUTED-A.                                              NC2064.2
157500     MOVE "5A4B3B2B1B0C" TO CORRECT-A.                            NC2064.2
157600     PERFORM FAIL.                                                NC2064.2
157700 QAL-WRITE-F1-48.                                                 NC2064.2
157800     PERFORM PRINT-DETAIL.                                        NC2064.2
157900*                                                                 NC2064.2
158000 QAL-INIT-F1-49.                                                  NC2064.2
158100     MOVE "IV-19 4.3.8.1"  TO ANSI-REFERENCE.                     NC2064.2
158200     MOVE "QUAL LIMITS "   TO FEATURE.                            NC2064.2
158300     MOVE "QAL-TEST-F1-49" TO PAR-NAME.                           NC2064.2
158400 QAL-TEST-F1-49.                                                  NC2064.2
158500     IF TBL-LEVEL-0D EQUAL TO "5A4A3A2A1A0D"                      NC2064.2
158600         PERFORM PASS                                             NC2064.2
158700         GO TO QAL-WRITE-F1-49.                                   NC2064.2
158800     GO TO QAL-FAIL-F1-49.                                        NC2064.2
158900 QAL-DELETE-F1-49.                                                NC2064.2
159000     PERFORM DE-LETE.                                             NC2064.2
159100     GO TO QAL-WRITE-F1-49.                                       NC2064.2
159200 QAL-FAIL-F1-49.                                                  NC2064.2
159300     MOVE "5A4A3A2A1A0D" TO CORRECT-A.                            NC2064.2
159400     MOVE TBL-LEVEL-0D IN TABLE-LEVEL-5A TO COMPUTED-A.           NC2064.2
159500*    NOTE  TBL-LEVEL-0D IS UNIQUE AND NEED NOT BE QUALIFIED       NC2064.2
159600*         HOWEVER, REFERENCE IS MADE TO IT BOTH QUALIFIED AND     NC2064.2
159700*         UNQUALIFIED TO INSURE THE ABILITY TO DO SO.             NC2064.2
159800     PERFORM FAIL.                                                NC2064.2
159900 QAL-WRITE-F1-49.                                                 NC2064.2
160000     PERFORM PRINT-DETAIL.                                        NC2064.2
160100*                                                                 NC2064.2
160200     PERFORM END-ROUTINE.                                         NC2064.2
160300*                                                                 NC2064.2
160400 SUB-INIT-F1-0.                                                   NC2064.2
160500     PERFORM END-ROUTINE.                                         NC2064.2
160600     MOVE "AA1122DD33" TO AX.                                     NC2064.2
160700     MOVE "IV-19 4.3.8.1"    TO ANSI-REFERENCE.                   NC2064.2
160800     MOVE "QUALIFIED SUBSCRIPTS" TO FEATURE.                      NC2064.2
160900*                                                                 NC2064.2
161000 SUB-INIT-F1-1.                                                   NC2064.2
161100     MOVE "SUB-TEST-F1-1" TO PAR-NAME.                            NC2064.2
161200 SUB-TEST-F1-1.                                                   NC2064.2
161300     IF AX-2 IN AX (CX-SUB OF CX) EQUAL TO "1"                    NC2064.2
161400         PERFORM PASS                                             NC2064.2
161500         GO TO SUB-WRITE-F1-1.                                    NC2064.2
161600     GO TO SUB-FAIL-F1-1.                                         NC2064.2
161700 SUB-DELETE-F1-1.                                                 NC2064.2
161800     PERFORM DE-LETE.                                             NC2064.2
161900     GO TO SUB-WRITE-F1-1.                                        NC2064.2
162000 SUB-FAIL-F1-1.                                                   NC2064.2
162100     MOVE AX-2 IN AX (CX-SUB OF CX) TO COMPUTED-A.                NC2064.2
162200     MOVE 1 TO CORRECT-A.                                         NC2064.2
162300     PERFORM FAIL.                                                NC2064.2
162400 SUB-WRITE-F1-1.                                                  NC2064.2
162500     PERFORM PRINT-DETAIL.                                        NC2064.2
162600*                                                                 NC2064.2
162700 SUB-INIT-F1-2.                                                   NC2064.2
162800     MOVE "SUB-TEST-F1-2" TO PAR-NAME.                            NC2064.2
162900     MOVE "IV-19 4.3.8.1"    TO ANSI-REFERENCE.                   NC2064.2
163000     MOVE "QUALIFIED SUBSCRIPTS" TO FEATURE.                      NC2064.2
163100     MOVE 4 TO CX.                                                NC2064.2
163200     MOVE "DD" TO CORRECT-A.                                      NC2064.2
163300 SUB-TEST-F1-2.                                                   NC2064.2
163400     MOVE AX-1 OF AX (CX-SUB OF CX) TO COMPUTED-A.                NC2064.2
163500     IF COMPUTED-A EQUAL TO CORRECT-A                             NC2064.2
163600         PERFORM PASS                                             NC2064.2
163700         MOVE SPACE TO COMPUTED-A CORRECT-A                       NC2064.2
163800         GO TO   SUB-WRITE-F1-2.                                  NC2064.2
163900     GO TO SUB-FAIL-F1-2.                                         NC2064.2
164000 SUB-DELETE-F1-2.                                                 NC2064.2
164100     PERFORM DE-LETE.                                             NC2064.2
164200     GO TO SUB-WRITE-F1-2.                                        NC2064.2
164300 SUB-FAIL-F1-2.                                                   NC2064.2
164400     PERFORM FAIL.                                                NC2064.2
164500 SUB-WRITE-F1-2.                                                  NC2064.2
164600     PERFORM PRINT-DETAIL.                                        NC2064.2
164700*                                                                 NC2064.2
164800 SUB-INIT-F1-3.                                                   NC2064.2
164900     MOVE "SUB-TEST-F1-3" TO PAR-NAME.                            NC2064.2
165000     MOVE "IV-19 4.3.8.1"    TO ANSI-REFERENCE.                   NC2064.2
165100     MOVE "QUALIFIED SUBSCRIPTS" TO FEATURE.                      NC2064.2
165200     MOVE 5 TO CX.                                                NC2064.2
165300     MOVE 5 TO AX-3 OF BX (1)  AX-2 OF BX (1).                    NC2064.2
165400     MOVE AX-1 IN BX (1) TO AX-1 OF AX (CX-SUB OF CX).            NC2064.2
165500 SUB-TEST-F1-3.                                                   NC2064.2
165600     IF AX-1 OF BX (1) EQUAL TO AX-1 IN AX (CX-SUB IN CX)         NC2064.2
165700         PERFORM PASS                                             NC2064.2
165800         GO TO SUB-WRITE-F1-3.                                    NC2064.2
165900     GO TO SUB-FAIL-F1-3.                                         NC2064.2
166000 SUB-DELETE-F1-3.                                                 NC2064.2
166100     PERFORM DE-LETE.                                             NC2064.2
166200     GO TO SUB-WRITE-F1-3.                                        NC2064.2
166300 SUB-FAIL-F1-3.                                                   NC2064.2
166400     MOVE AX-1 OF BX (1) TO COMPUTED-A.                           NC2064.2
166500     MOVE AX-1 IN AX (CX-SUB IN CX) TO CORRECT-A.                 NC2064.2
166600     PERFORM FAIL.                                                NC2064.2
166700     MOVE "UNEQUAL COMPARISON" TO RE-MARK.                        NC2064.2
166800 SUB-WRITE-F1-3.                                                  NC2064.2
166900     PERFORM PRINT-DETAIL.                                        NC2064.2
167000*                                                                 NC2064.2
167100 SUB-INIT-F1-4.                                                   NC2064.2
167200     MOVE "IV-19 4.3.8.1"    TO ANSI-REFERENCE.                   NC2064.2
167300     MOVE "QUALIFIED SUBSCRIPTS" TO FEATURE.                      NC2064.2
167400     MOVE "SUB-TEST-F1-4" TO PAR-NAME.                            NC2064.2
167500     MOVE 1 TO CX.                                                NC2064.2
167600     MOVE 11 TO AX-1 OF BX (CX-SUB IN CX).                        NC2064.2
167700     ADD AX-3 IN BX (CX-SUB OF CX) TO AX-2 IN BX (CX-SUB IN CX).  NC2064.2
167800 SUB-TEST-F1-4.                                                   NC2064.2
167900     IF AX-2 IN BX (CX-SUB IN CX) EQUAL TO AX-2 IN AX (3)         NC2064.2
168000         PERFORM PASS                                             NC2064.2
168100         GO TO SUB-WRITE-F1-4.                                    NC2064.2
168200     GO TO SUB-FAIL-F1-4.                                         NC2064.2
168300 SUB-DELETE-F1-4.                                                 NC2064.2
168400     PERFORM DE-LETE.                                             NC2064.2
168500     GO TO SUB-WRITE-F1-4.                                        NC2064.2
168600 SUB-FAIL-F1-4.                                                   NC2064.2
168700     MOVE AX-2 IN BX (CX-SUB IN CX) TO COMPUTED-A.                NC2064.2
168800     MOVE AX-2 IN AX (3) TO CORRECT-A.                            NC2064.2
168900     MOVE "UNEQUAL COMPARISON" TO RE-MARK.                        NC2064.2
169000     PERFORM FAIL.                                                NC2064.2
169100 SUB-WRITE-F1-4.                                                  NC2064.2
169200     PERFORM PRINT-DETAIL.                                        NC2064.2
169300 CCVS-EXIT SECTION.                                               NC2064.2
169400 CCVS-999999.                                                     NC2064.2
169500     GO TO CLOSE-FILES.                                           NC2064.2
      *END-OF,NC206A                                                                  
