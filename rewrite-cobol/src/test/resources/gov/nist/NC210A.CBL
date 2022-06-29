      *HEADER,COBOL,NC210A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2104.2
000200 PROGRAM-ID.                                                      NC2104.2
000300     NC210A.                                                      NC2104.2
000400****************************************************************  NC2104.2
000500*                                                              *  NC2104.2
000600*    VALIDATION FOR:-                                          *  NC2104.2
000700*                                                              *  NC2104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2104.2
000900*                                                              *  NC2104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2104.2
001100*                                                              *  NC2104.2
001200****************************************************************  NC2104.2
001300*                                                              *  NC2104.2
001400*    PROGRAM NC210A TESTS NESTED "IF" STATEMENTS, USING 63     *  NC2104.2
001500*    STATEMENTS AND 6 LEVELS OF NESTING IN ONE SENTENCE AND    *  NC2104.2
001600*    22 LEVELS OF NESTING IN A SECOND TEST.                    *  NC2104.2
001700*                                                              *  NC2104.2
001800*     X-CARDS USED ARE :-                                      *  NC2104.2
001900*                                                              *  NC2104.2
002000*      X-55  - SYSTEM PRINTER NAME.                            *  NC2104.2
002100*      X-82  - SOURCE COMPUTER NAME.                           *  NC2104.2
002200*      X-83  - OBJECT COMPUTER NAME.                           *  NC2104.2
002300*                                                              *  NC2104.2
002400****************************************************************  NC2104.2
002500 ENVIRONMENT DIVISION.                                            NC2104.2
002600 CONFIGURATION SECTION.                                           NC2104.2
002700 SOURCE-COMPUTER.                                                 NC2104.2
002800     XXXXX082.                                                    NC2104.2
002900 OBJECT-COMPUTER.                                                 NC2104.2
003000     XXXXX083.                                                    NC2104.2
003100 INPUT-OUTPUT SECTION.                                            NC2104.2
003200 FILE-CONTROL.                                                    NC2104.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC2104.2
003400     XXXXX055.                                                    NC2104.2
003500 DATA DIVISION.                                                   NC2104.2
003600 FILE SECTION.                                                    NC2104.2
003700 FD  PRINT-FILE.                                                  NC2104.2
003800 01  PRINT-REC PICTURE X(120).                                    NC2104.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC2104.2
004000 WORKING-STORAGE SECTION.                                         NC2104.2
004100 77  ACCUM-DATANAME PIC 99  VALUE ZERO.                           NC2104.2
004200 77  DATANAME-A  PIC 9 VALUE 0.                                   NC2104.2
004300 77  DATANAME-B  PIC 9 VALUE 0.                                   NC2104.2
004400 77  DATANAME-C  PIC 9 VALUE 0.                                   NC2104.2
004500 77  DATANAME-D  PIC 9 VALUE 0.                                   NC2104.2
004600 77  DATANAME-E  PIC 9 VALUE 0.                                   NC2104.2
004700 77  DATANAME-F  PIC 9 VALUE 0.                                   NC2104.2
004800 77  SUB-SCRIPT     PIC 99  VALUE 01.                             NC2104.2
004900 01  DATA-NAMES.                                                  NC2104.2
005000     02  ONE-A PIC 9 VALUE 0.                                     NC2104.2
005100     02  ONE-B PIC 9 VALUE 0.                                     NC2104.2
005200     02  ONE-C PIC 9 VALUE 0.                                     NC2104.2
005300     02  ONE-D PIC 9 VALUE 0.                                     NC2104.2
005400     02  ONE-E PIC 9 VALUE 0.                                     NC2104.2
005500     02  ONE-F PIC 9 VALUE 0.                                     NC2104.2
005600 01  ONE-X REDEFINES DATA-NAMES  PIC 9(6).                        NC2104.2
005700 01  BUILT-TABLE.                                                 NC2104.2
005800     02  A-POS                   PIC XX.                          NC2104.2
005900     02  B-POS                   PIC XX.                          NC2104.2
006000     02  C-POS                   PIC XX.                          NC2104.2
006100     02  D-POS                   PIC XXX.                         NC2104.2
006200     02  E-POS                   PIC XXX.                         NC2104.2
006300     02  F-POS                   PIC XXX.                         NC2104.2
006400 01  PARAGRAPH-NAME.                                              NC2104.2
006500     02  FILLER                  PIC X(11) VALUE "IF-TEST-GF-".   NC2104.2
006600     02  PAR-NUMBER              PIC 99  VALUE 00.                NC2104.2
006700 01  COMPARISON-TABLE.                                            NC2104.2
006800     02  FILLER PIC X(15) VALUE "A1B1C1D01E01F01".                NC2104.2
006900     02  FILLER PIC X(15) VALUE "A1B1C1D01E01F02".                NC2104.2
007000     02  FILLER PIC X(15) VALUE "A1B1C1D01E02F03".                NC2104.2
007100     02  FILLER PIC X(15) VALUE "A1B1C1D01E02F04".                NC2104.2
007200     02  FILLER PIC X(15) VALUE "A1B1C1D02E03F05".                NC2104.2
007300     02  FILLER PIC X(15) VALUE "A1B1C1D02E03F06".                NC2104.2
007400     02  FILLER PIC X(15) VALUE "A1B1C1D02E04F07".                NC2104.2
007500     02  FILLER PIC X(15) VALUE "A1B1C1D02E04F08".                NC2104.2
007600     02  FILLER PIC X(15) VALUE "A1B1C2D03E05F09".                NC2104.2
007700     02  FILLER PIC X(15) VALUE "A1B1C2D03E05F10".                NC2104.2
007800     02  FILLER PIC X(15) VALUE "A1B1C2D03E06F11".                NC2104.2
007900     02  FILLER PIC X(15) VALUE "A1B1C2D03E06F12".                NC2104.2
008000     02  FILLER PIC X(15) VALUE "A1B1C2D04E07F13".                NC2104.2
008100     02  FILLER PIC X(15) VALUE "A1B1C2D04E07F14".                NC2104.2
008200     02  FILLER PIC X(15) VALUE "A1B1C2D04E08F15".                NC2104.2
008300     02  FILLER PIC X(15) VALUE "A1B1C2D04E08F16".                NC2104.2
008400     02  FILLER PIC X(15) VALUE "A1B2C3D05E09F17".                NC2104.2
008500     02  FILLER PIC X(15) VALUE "A1B2C3D05E09F18".                NC2104.2
008600     02  FILLER PIC X(15) VALUE "A1B2C3D05E10F19".                NC2104.2
008700     02  FILLER PIC X(15) VALUE "A1B2C3D05E10F20".                NC2104.2
008800     02  FILLER PIC X(15) VALUE "A1B2C3D06E11F21".                NC2104.2
008900     02  FILLER PIC X(15) VALUE "A1B2C3D06E11F22".                NC2104.2
009000     02  FILLER PIC X(15) VALUE "A1B2C3D06E12F23".                NC2104.2
009100     02  FILLER PIC X(15) VALUE "A1B2C3D06E12F24".                NC2104.2
009200     02  FILLER PIC X(15) VALUE "A1B2C4D07E13F25".                NC2104.2
009300     02  FILLER PIC X(15) VALUE "A1B2C4D07E13F26".                NC2104.2
009400     02  FILLER PIC X(15) VALUE "A1B2C4D07E14F27".                NC2104.2
009500     02  FILLER PIC X(15) VALUE "A1B2C4D07E14F28".                NC2104.2
009600     02  FILLER PIC X(15) VALUE "A1B2C4D08E15F29".                NC2104.2
009700     02  FILLER PIC X(15) VALUE "A1B2C4D08E15F30".                NC2104.2
009800     02  FILLER PIC X(15) VALUE "A1B2C4D08E16F31".                NC2104.2
009900     02  FILLER PIC X(15) VALUE "A1B2C4D08E16F32".                NC2104.2
010000     02  FILLER PIC X(15) VALUE "A2B3C5D09E17F33".                NC2104.2
010100     02  FILLER PIC X(15) VALUE "A2B3C5D09E17F34".                NC2104.2
010200     02  FILLER PIC X(15) VALUE "A2B3C5D09E18F35".                NC2104.2
010300     02  FILLER PIC X(15) VALUE "A2B3C5D09E18F36".                NC2104.2
010400     02  FILLER PIC X(15) VALUE "A2B3C5D10E19F37".                NC2104.2
010500     02  FILLER PIC X(15) VALUE "A2B3C5D10E19F38".                NC2104.2
010600     02  FILLER PIC X(15) VALUE "A2B3C5D10E20F39".                NC2104.2
010700     02  FILLER PIC X(15) VALUE "A2B3C5D10E20F40".                NC2104.2
010800     02  FILLER PIC X(15) VALUE "A2B3C6D11E21F41".                NC2104.2
010900     02  FILLER PIC X(15) VALUE "A2B3C6D11E21F42".                NC2104.2
011000     02  FILLER PIC X(15) VALUE "A2B3C6D11E22F43".                NC2104.2
011100     02  FILLER PIC X(15) VALUE "A2B3C6D11E22F44".                NC2104.2
011200     02  FILLER PIC X(15) VALUE "A2B3C6D12E23F45".                NC2104.2
011300     02  FILLER PIC X(15) VALUE "A2B3C6D12E23F46".                NC2104.2
011400     02  FILLER PIC X(15) VALUE "A2B3C6D12E24F47".                NC2104.2
011500     02  FILLER PIC X(15) VALUE "A2B3C6D12E24F48".                NC2104.2
011600     02  FILLER PIC X(15) VALUE "A2B4C7D13E25F49".                NC2104.2
011700     02  FILLER PIC X(15) VALUE "A2B4C7D13E25F50".                NC2104.2
011800     02  FILLER PIC X(15) VALUE "A2B4C7D13E26F51".                NC2104.2
011900     02  FILLER PIC X(15) VALUE "A2B4C7D13E26F52".                NC2104.2
012000     02  FILLER PIC X(15) VALUE "A2B4C7D14E27F53".                NC2104.2
012100     02  FILLER PIC X(15) VALUE "A2B4C7D14E27F54".                NC2104.2
012200     02  FILLER PIC X(15) VALUE "A2B4C7D14E28F55".                NC2104.2
012300     02  FILLER PIC X(15) VALUE "A2B4C7D14E28F56".                NC2104.2
012400     02  FILLER PIC X(15) VALUE "A2B4C8D15E29F57".                NC2104.2
012500     02  FILLER PIC X(15) VALUE "A2B4C8D15E29F58".                NC2104.2
012600     02  FILLER PIC X(15) VALUE "A2B4C8D15E30F59".                NC2104.2
012700     02  FILLER PIC X(15) VALUE "A2B4C8D15E30F60".                NC2104.2
012800     02  FILLER PIC X(15) VALUE "A2B4C8D16E31F61".                NC2104.2
012900     02  FILLER PIC X(15) VALUE "A2B4C8D16E31F62".                NC2104.2
013000     02  FILLER PIC X(15) VALUE "A2B4C8D16E32F63".                NC2104.2
013100     02  FILLER PIC X(15) VALUE "A2B4C8D16E32F64".                NC2104.2
013200 01  COMP-TBL REDEFINES COMPARISON-TABLE.                         NC2104.2
013300     02  CORRECT-ENTRY OCCURS 64 TIMES PIC X(15).                 NC2104.2
013400 01  T-F  PIC X(5) VALUE "FALSE".                                 NC2104.2
013500 01  CHECK-PARA.                                                  NC2104.2
013600     02  FILLER  PIC X(21) VALUE "VALUE OF DATANAME AT ".         NC2104.2
013700     02  CHECK-VALU  PIC 99.                                      NC2104.2
013800     02  FILLER  PIC X(4) VALUE SPACES.                           NC2104.2
013900 01  TEST-RESULTS.                                                NC2104.2
014000     02 FILLER                   PIC X      VALUE SPACE.          NC2104.2
014100     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2104.2
014200     02 FILLER                   PIC X      VALUE SPACE.          NC2104.2
014300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2104.2
014400     02 FILLER                   PIC X      VALUE SPACE.          NC2104.2
014500     02  PAR-NAME.                                                NC2104.2
014600       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2104.2
014700       03  PARDOT-X              PIC X      VALUE SPACE.          NC2104.2
014800       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2104.2
014900     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2104.2
015000     02 RE-MARK                  PIC X(61).                       NC2104.2
015100 01  TEST-COMPUTED.                                               NC2104.2
015200     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2104.2
015300     02 FILLER                   PIC X(17)  VALUE                 NC2104.2
015400            "       COMPUTED=".                                   NC2104.2
015500     02 COMPUTED-X.                                               NC2104.2
015600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2104.2
015700     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2104.2
015800                                 PIC -9(9).9(9).                  NC2104.2
015900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2104.2
016000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2104.2
016100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2104.2
016200     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2104.2
016300         04 COMPUTED-18V0                    PIC -9(18).          NC2104.2
016400         04 FILLER                           PIC X.               NC2104.2
016500     03 FILLER PIC X(50) VALUE SPACE.                             NC2104.2
016600 01  TEST-CORRECT.                                                NC2104.2
016700     02 FILLER PIC X(30) VALUE SPACE.                             NC2104.2
016800     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2104.2
016900     02 CORRECT-X.                                                NC2104.2
017000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2104.2
017100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2104.2
017200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2104.2
017300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2104.2
017400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2104.2
017500     03      CR-18V0 REDEFINES CORRECT-A.                         NC2104.2
017600         04 CORRECT-18V0                     PIC -9(18).          NC2104.2
017700         04 FILLER                           PIC X.               NC2104.2
017800     03 FILLER PIC X(2) VALUE SPACE.                              NC2104.2
017900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2104.2
018000 01  CCVS-C-1.                                                    NC2104.2
018100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2104.2
018200-    "SS  PARAGRAPH-NAME                                          NC2104.2
018300-    "       REMARKS".                                            NC2104.2
018400     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2104.2
018500 01  CCVS-C-2.                                                    NC2104.2
018600     02 FILLER                     PIC X        VALUE SPACE.      NC2104.2
018700     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2104.2
018800     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2104.2
018900     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2104.2
019000     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2104.2
019100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2104.2
019200 01  REC-CT                        PIC 99       VALUE ZERO.       NC2104.2
019300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2104.2
019400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2104.2
019500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2104.2
019600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2104.2
019700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2104.2
019800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2104.2
019900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2104.2
020000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2104.2
020100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2104.2
020200 01  CCVS-H-1.                                                    NC2104.2
020300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2104.2
020400     02  FILLER                    PIC X(42)    VALUE             NC2104.2
020500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2104.2
020600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2104.2
020700 01  CCVS-H-2A.                                                   NC2104.2
020800   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2104.2
020900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2104.2
021000   02  FILLER                        PIC XXXX   VALUE             NC2104.2
021100     "4.2 ".                                                      NC2104.2
021200   02  FILLER                        PIC X(28)  VALUE             NC2104.2
021300            " COPY - NOT FOR DISTRIBUTION".                       NC2104.2
021400   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2104.2
021500                                                                  NC2104.2
021600 01  CCVS-H-2B.                                                   NC2104.2
021700   02  FILLER                        PIC X(15)  VALUE             NC2104.2
021800            "TEST RESULT OF ".                                    NC2104.2
021900   02  TEST-ID                       PIC X(9).                    NC2104.2
022000   02  FILLER                        PIC X(4)   VALUE             NC2104.2
022100            " IN ".                                               NC2104.2
022200   02  FILLER                        PIC X(12)  VALUE             NC2104.2
022300     " HIGH       ".                                              NC2104.2
022400   02  FILLER                        PIC X(22)  VALUE             NC2104.2
022500            " LEVEL VALIDATION FOR ".                             NC2104.2
022600   02  FILLER                        PIC X(58)  VALUE             NC2104.2
022700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2104.2
022800 01  CCVS-H-3.                                                    NC2104.2
022900     02  FILLER                      PIC X(34)  VALUE             NC2104.2
023000            " FOR OFFICIAL USE ONLY    ".                         NC2104.2
023100     02  FILLER                      PIC X(58)  VALUE             NC2104.2
023200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2104.2
023300     02  FILLER                      PIC X(28)  VALUE             NC2104.2
023400            "  COPYRIGHT   1985 ".                                NC2104.2
023500 01  CCVS-E-1.                                                    NC2104.2
023600     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2104.2
023700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2104.2
023800     02 ID-AGAIN                     PIC X(9).                    NC2104.2
023900     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2104.2
024000 01  CCVS-E-2.                                                    NC2104.2
024100     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2104.2
024200     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2104.2
024300     02 CCVS-E-2-2.                                               NC2104.2
024400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2104.2
024500         03 FILLER                   PIC X      VALUE SPACE.      NC2104.2
024600         03 ENDER-DESC               PIC X(44)  VALUE             NC2104.2
024700            "ERRORS ENCOUNTERED".                                 NC2104.2
024800 01  CCVS-E-3.                                                    NC2104.2
024900     02  FILLER                      PIC X(22)  VALUE             NC2104.2
025000            " FOR OFFICIAL USE ONLY".                             NC2104.2
025100     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2104.2
025200     02  FILLER                      PIC X(58)  VALUE             NC2104.2
025300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2104.2
025400     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2104.2
025500     02 FILLER                       PIC X(15)  VALUE             NC2104.2
025600             " COPYRIGHT 1985".                                   NC2104.2
025700 01  CCVS-E-4.                                                    NC2104.2
025800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2104.2
025900     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2104.2
026000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2104.2
026100     02 FILLER                       PIC X(40)  VALUE             NC2104.2
026200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2104.2
026300 01  XXINFO.                                                      NC2104.2
026400     02 FILLER                       PIC X(19)  VALUE             NC2104.2
026500            "*** INFORMATION ***".                                NC2104.2
026600     02 INFO-TEXT.                                                NC2104.2
026700       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2104.2
026800       04 XXCOMPUTED                 PIC X(20).                   NC2104.2
026900       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2104.2
027000       04 XXCORRECT                  PIC X(20).                   NC2104.2
027100     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2104.2
027200 01  HYPHEN-LINE.                                                 NC2104.2
027300     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2104.2
027400     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2104.2
027500-    "*****************************************".                 NC2104.2
027600     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2104.2
027700-    "******************************".                            NC2104.2
027800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2104.2
027900     "NC210A".                                                    NC2104.2
028000 PROCEDURE DIVISION.                                              NC2104.2
028100 CCVS1 SECTION.                                                   NC2104.2
028200 OPEN-FILES.                                                      NC2104.2
028300     OPEN     OUTPUT PRINT-FILE.                                  NC2104.2
028400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2104.2
028500     MOVE    SPACE TO TEST-RESULTS.                               NC2104.2
028600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2104.2
028700     GO TO CCVS1-EXIT.                                            NC2104.2
028800 CLOSE-FILES.                                                     NC2104.2
028900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2104.2
029000 TERMINATE-CCVS.                                                  NC2104.2
029100     STOP     RUN.                                                NC2104.2
029200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2104.2
029300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2104.2
029400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2104.2
029500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2104.2
029600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2104.2
029700 PRINT-DETAIL.                                                    NC2104.2
029800     IF REC-CT NOT EQUAL TO ZERO                                  NC2104.2
029900             MOVE "." TO PARDOT-X                                 NC2104.2
030000             MOVE REC-CT TO DOTVALUE.                             NC2104.2
030100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2104.2
030200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2104.2
030300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2104.2
030400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2104.2
030500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2104.2
030600     MOVE SPACE TO CORRECT-X.                                     NC2104.2
030700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2104.2
030800     MOVE     SPACE TO RE-MARK.                                   NC2104.2
030900 HEAD-ROUTINE.                                                    NC2104.2
031000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2104.2
031100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2104.2
031200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2104.2
031300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2104.2
031400 COLUMN-NAMES-ROUTINE.                                            NC2104.2
031500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2104.2
031600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2104.2
031700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2104.2
031800 END-ROUTINE.                                                     NC2104.2
031900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2104.2
032000 END-RTN-EXIT.                                                    NC2104.2
032100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2104.2
032200 END-ROUTINE-1.                                                   NC2104.2
032300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2104.2
032400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2104.2
032500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2104.2
032600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2104.2
032700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2104.2
032800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2104.2
032900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2104.2
033000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2104.2
033100  END-ROUTINE-12.                                                 NC2104.2
033200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2104.2
033300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2104.2
033400         MOVE "NO " TO ERROR-TOTAL                                NC2104.2
033500         ELSE                                                     NC2104.2
033600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2104.2
033700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2104.2
033800     PERFORM WRITE-LINE.                                          NC2104.2
033900 END-ROUTINE-13.                                                  NC2104.2
034000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2104.2
034100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2104.2
034200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2104.2
034300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2104.2
034400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2104.2
034500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2104.2
034600          MOVE "NO " TO ERROR-TOTAL                               NC2104.2
034700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2104.2
034800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2104.2
034900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2104.2
035000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2104.2
035100 WRITE-LINE.                                                      NC2104.2
035200     ADD 1 TO RECORD-COUNT.                                       NC2104.2
035300*    IF RECORD-COUNT GREATER 50                                   NC2104.2
035400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2104.2
035500*        MOVE SPACE TO DUMMY-RECORD                               NC2104.2
035600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2104.2
035700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2104.2
035800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2104.2
035900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2104.2
036000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2104.2
036100*        MOVE ZERO TO RECORD-COUNT.                               NC2104.2
036200     PERFORM WRT-LN.                                              NC2104.2
036300 WRT-LN.                                                          NC2104.2
036400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2104.2
036500     MOVE SPACE TO DUMMY-RECORD.                                  NC2104.2
036600 BLANK-LINE-PRINT.                                                NC2104.2
036700     PERFORM WRT-LN.                                              NC2104.2
036800 FAIL-ROUTINE.                                                    NC2104.2
036900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2104.2
037000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2104.2
037100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2104.2
037200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2104.2
037300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2104.2
037400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2104.2
037500     GO TO  FAIL-ROUTINE-EX.                                      NC2104.2
037600 FAIL-ROUTINE-WRITE.                                              NC2104.2
037700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2104.2
037800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2104.2
037900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2104.2
038000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2104.2
038100 FAIL-ROUTINE-EX. EXIT.                                           NC2104.2
038200 BAIL-OUT.                                                        NC2104.2
038300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2104.2
038400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2104.2
038500 BAIL-OUT-WRITE.                                                  NC2104.2
038600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2104.2
038700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2104.2
038800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2104.2
038900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2104.2
039000 BAIL-OUT-EX. EXIT.                                               NC2104.2
039100 CCVS1-EXIT.                                                      NC2104.2
039200     EXIT.                                                        NC2104.2
039300 SECT-NC210A-001 SECTION.                                         NC2104.2
039400 IF-INIT-GF-X.                                                    NC2104.2
039500     MOVE   "VI-89 6.15.4 GR1(C)" TO ANSI-REFERENCE.              NC2104.2
039600 IF-TEST-GF-X.                                                    NC2104.2
039700     IF DATANAME-A EQUAL TO ONE-A                                 NC2104.2
039800         MOVE "A1" TO A-POS                                       NC2104.2
039900         IF DATANAME-B EQUAL TO ONE-B                             NC2104.2
040000             MOVE "B1" TO B-POS                                   NC2104.2
040100             IF DATANAME-C EQUAL TO ONE-C                         NC2104.2
040200                 MOVE "C1" TO C-POS                               NC2104.2
040300                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
040400                     MOVE "D01" TO D-POS                          NC2104.2
040500                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
040600                         MOVE "E01" TO E-POS                      NC2104.2
040700                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
040800                             MOVE "F01" TO F-POS                  NC2104.2
040900                           ELSE                                   NC2104.2
041000                             MOVE "F02" TO F-POS                  NC2104.2
041100                       ELSE                                       NC2104.2
041200                         MOVE "E02" TO E-POS                      NC2104.2
041300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
041400                             MOVE "F03" TO F-POS                  NC2104.2
041500                           ELSE                                   NC2104.2
041600                             MOVE "F04" TO F-POS                  NC2104.2
041700                   ELSE                                           NC2104.2
041800                     MOVE "D02" TO D-POS                          NC2104.2
041900                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
042000                         MOVE "E03" TO E-POS                      NC2104.2
042100                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
042200                             MOVE "F05" TO F-POS                  NC2104.2
042300                           ELSE                                   NC2104.2
042400                             MOVE "F06" TO F-POS                  NC2104.2
042500                       ELSE                                       NC2104.2
042600                         MOVE "E04" TO E-POS                      NC2104.2
042700                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
042800                             MOVE "F07" TO F-POS                  NC2104.2
042900                           ELSE                                   NC2104.2
043000                             MOVE "F08" TO F-POS                  NC2104.2
043100               ELSE                                               NC2104.2
043200                 MOVE "C2" TO C-POS                               NC2104.2
043300                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
043400                     MOVE "D03" TO D-POS                          NC2104.2
043500                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
043600                         MOVE "E05" TO E-POS                      NC2104.2
043700                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
043800                             MOVE "F09" TO F-POS                  NC2104.2
043900                           ELSE                                   NC2104.2
044000                             MOVE "F10" TO F-POS                  NC2104.2
044100                       ELSE                                       NC2104.2
044200                         MOVE "E06" TO E-POS                      NC2104.2
044300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
044400                             MOVE "F11" TO F-POS                  NC2104.2
044500                           ELSE                                   NC2104.2
044600                             MOVE "F12" TO F-POS                  NC2104.2
044700                   ELSE                                           NC2104.2
044800                     MOVE "D04" TO D-POS                          NC2104.2
044900                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
045000                         MOVE "E07" TO E-POS                      NC2104.2
045100                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
045200                             MOVE "F13" TO F-POS                  NC2104.2
045300                           ELSE                                   NC2104.2
045400                             MOVE "F14" TO F-POS                  NC2104.2
045500                       ELSE                                       NC2104.2
045600                         MOVE "E08" TO E-POS                      NC2104.2
045700                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
045800                             MOVE "F15" TO F-POS                  NC2104.2
045900                           ELSE                                   NC2104.2
046000                             MOVE "F16" TO F-POS                  NC2104.2
046100           ELSE                                                   NC2104.2
046200             MOVE "B2" TO B-POS                                   NC2104.2
046300             IF DATANAME-C EQUAL TO ONE-C                         NC2104.2
046400                 MOVE "C3" TO C-POS                               NC2104.2
046500                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
046600                     MOVE "D05" TO D-POS                          NC2104.2
046700                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
046800                         MOVE "E09" TO E-POS                      NC2104.2
046900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
047000                             MOVE "F17" TO F-POS                  NC2104.2
047100                           ELSE                                   NC2104.2
047200                             MOVE "F18" TO F-POS                  NC2104.2
047300                       ELSE                                       NC2104.2
047400                         MOVE "E10" TO E-POS                      NC2104.2
047500                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
047600                             MOVE "F19" TO F-POS                  NC2104.2
047700                           ELSE                                   NC2104.2
047800                             MOVE "F20" TO F-POS                  NC2104.2
047900                   ELSE                                           NC2104.2
048000                     MOVE "D06" TO D-POS                          NC2104.2
048100                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
048200                         MOVE "E11" TO E-POS                      NC2104.2
048300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
048400                             MOVE "F21" TO F-POS                  NC2104.2
048500                           ELSE                                   NC2104.2
048600                             MOVE "F22" TO F-POS                  NC2104.2
048700                       ELSE                                       NC2104.2
048800                         MOVE "E12" TO E-POS                      NC2104.2
048900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
049000                             MOVE "F23" TO F-POS                  NC2104.2
049100                           ELSE                                   NC2104.2
049200                             MOVE "F24" TO F-POS                  NC2104.2
049300               ELSE                                               NC2104.2
049400                 MOVE "C4" TO C-POS                               NC2104.2
049500                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
049600                     MOVE "D07" TO D-POS                          NC2104.2
049700                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
049800                         MOVE "E13" TO E-POS                      NC2104.2
049900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
050000                             MOVE "F25" TO F-POS                  NC2104.2
050100                           ELSE                                   NC2104.2
050200                             MOVE "F26" TO F-POS                  NC2104.2
050300                       ELSE                                       NC2104.2
050400                         MOVE "E14" TO E-POS                      NC2104.2
050500                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
050600                             MOVE "F27" TO F-POS                  NC2104.2
050700                           ELSE                                   NC2104.2
050800                             MOVE "F28" TO F-POS                  NC2104.2
050900                   ELSE                                           NC2104.2
051000                     MOVE "D08" TO D-POS                          NC2104.2
051100                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
051200                         MOVE "E15" TO E-POS                      NC2104.2
051300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
051400                             MOVE "F29" TO F-POS                  NC2104.2
051500                           ELSE                                   NC2104.2
051600                             MOVE "F30" TO F-POS                  NC2104.2
051700                       ELSE                                       NC2104.2
051800                         MOVE "E16" TO E-POS                      NC2104.2
051900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
052000                             MOVE "F31" TO F-POS                  NC2104.2
052100                           ELSE                                   NC2104.2
052200                             MOVE "F32" TO F-POS                  NC2104.2
052300       ELSE                                                       NC2104.2
052400         MOVE "A2" TO A-POS                                       NC2104.2
052500         IF DATANAME-B EQUAL TO ONE-B                             NC2104.2
052600             MOVE "B3" TO B-POS                                   NC2104.2
052700             IF DATANAME-C EQUAL TO ONE-C                         NC2104.2
052800                 MOVE "C5" TO C-POS                               NC2104.2
052900                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
053000                     MOVE "D09" TO D-POS                          NC2104.2
053100                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
053200                         MOVE "E17" TO E-POS                      NC2104.2
053300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
053400                             MOVE "F33" TO F-POS                  NC2104.2
053500                           ELSE                                   NC2104.2
053600                             MOVE "F34" TO F-POS                  NC2104.2
053700                       ELSE                                       NC2104.2
053800                         MOVE "E18" TO E-POS                      NC2104.2
053900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
054000                             MOVE "F35" TO F-POS                  NC2104.2
054100                           ELSE                                   NC2104.2
054200                             MOVE "F36" TO F-POS                  NC2104.2
054300                   ELSE                                           NC2104.2
054400                     MOVE "D10" TO D-POS                          NC2104.2
054500                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
054600                         MOVE "E19" TO E-POS                      NC2104.2
054700                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
054800                             MOVE "F37" TO F-POS                  NC2104.2
054900                           ELSE                                   NC2104.2
055000                             MOVE "F38" TO F-POS                  NC2104.2
055100                       ELSE                                       NC2104.2
055200                         MOVE "E20" TO E-POS                      NC2104.2
055300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
055400                             MOVE "F39" TO F-POS                  NC2104.2
055500                           ELSE                                   NC2104.2
055600                             MOVE "F40" TO F-POS                  NC2104.2
055700               ELSE                                               NC2104.2
055800                 MOVE "C6" TO C-POS                               NC2104.2
055900                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
056000                     MOVE "D11" TO D-POS                          NC2104.2
056100                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
056200                         MOVE "E21" TO E-POS                      NC2104.2
056300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
056400                             MOVE "F41" TO F-POS                  NC2104.2
056500                           ELSE                                   NC2104.2
056600                             MOVE "F42" TO F-POS                  NC2104.2
056700                       ELSE                                       NC2104.2
056800                         MOVE "E22" TO E-POS                      NC2104.2
056900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
057000                             MOVE "F43" TO F-POS                  NC2104.2
057100                           ELSE                                   NC2104.2
057200                             MOVE "F44" TO F-POS                  NC2104.2
057300                   ELSE                                           NC2104.2
057400                     MOVE "D12" TO D-POS                          NC2104.2
057500                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
057600                         MOVE "E23" TO E-POS                      NC2104.2
057700                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
057800                             MOVE "F45" TO F-POS                  NC2104.2
057900                           ELSE                                   NC2104.2
058000                             MOVE "F46" TO F-POS                  NC2104.2
058100                       ELSE                                       NC2104.2
058200                         MOVE "E24" TO E-POS                      NC2104.2
058300                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
058400                             MOVE "F47" TO F-POS                  NC2104.2
058500                           ELSE                                   NC2104.2
058600                             MOVE "F48" TO F-POS                  NC2104.2
058700           ELSE                                                   NC2104.2
058800             MOVE "B4" TO B-POS                                   NC2104.2
058900             IF DATANAME-C EQUAL TO ONE-C                         NC2104.2
059000                 MOVE "C7" TO C-POS                               NC2104.2
059100                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
059200                     MOVE "D13" TO D-POS                          NC2104.2
059300                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
059400                         MOVE "E25" TO E-POS                      NC2104.2
059500                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
059600                             MOVE "F49" TO F-POS                  NC2104.2
059700                           ELSE                                   NC2104.2
059800                             MOVE "F50" TO F-POS                  NC2104.2
059900                       ELSE                                       NC2104.2
060000                         MOVE "E26" TO E-POS                      NC2104.2
060100                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
060200                             MOVE "F51" TO F-POS                  NC2104.2
060300                           ELSE                                   NC2104.2
060400                             MOVE "F52" TO F-POS                  NC2104.2
060500                   ELSE                                           NC2104.2
060600                     MOVE "D14" TO D-POS                          NC2104.2
060700                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
060800                         MOVE "E27" TO E-POS                      NC2104.2
060900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
061000                             MOVE "F53" TO F-POS                  NC2104.2
061100                           ELSE                                   NC2104.2
061200                             MOVE "F54" TO F-POS                  NC2104.2
061300                       ELSE                                       NC2104.2
061400                         MOVE "E28" TO E-POS                      NC2104.2
061500                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
061600                             MOVE "F55" TO F-POS                  NC2104.2
061700                           ELSE                                   NC2104.2
061800                             MOVE "F56" TO F-POS                  NC2104.2
061900               ELSE                                               NC2104.2
062000                 MOVE "C8" TO C-POS                               NC2104.2
062100                 IF DATANAME-D EQUAL TO ONE-D                     NC2104.2
062200                     MOVE "D15" TO D-POS                          NC2104.2
062300                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
062400                         MOVE "E29" TO E-POS                      NC2104.2
062500                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
062600                             MOVE "F57" TO F-POS                  NC2104.2
062700                           ELSE                                   NC2104.2
062800                             MOVE "F58" TO F-POS                  NC2104.2
062900                       ELSE                                       NC2104.2
063000                         MOVE "E30" TO E-POS                      NC2104.2
063100                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
063200                             MOVE "F59" TO F-POS                  NC2104.2
063300                           ELSE                                   NC2104.2
063400                             MOVE "F60" TO F-POS                  NC2104.2
063500                   ELSE                                           NC2104.2
063600                     MOVE "D16" TO D-POS                          NC2104.2
063700                     IF DATANAME-E EQUAL TO ONE-E                 NC2104.2
063800                         MOVE "E31" TO E-POS                      NC2104.2
063900                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
064000                             MOVE "F61" TO F-POS                  NC2104.2
064100                           ELSE                                   NC2104.2
064200                             MOVE "F62" TO F-POS                  NC2104.2
064300                       ELSE                                       NC2104.2
064400                         MOVE "E32" TO E-POS                      NC2104.2
064500                         IF DATANAME-F EQUAL TO ONE-F             NC2104.2
064600                             MOVE "F63" TO F-POS                  NC2104.2
064700                           ELSE                                   NC2104.2
064800                             MOVE "F64" TO F-POS.                 NC2104.2
064900     IF BUILT-TABLE EQUAL TO CORRECT-ENTRY (SUB-SCRIPT)           NC2104.2
065000         PERFORM PASS                                             NC2104.2
065100         GO TO IF-WRITE-GF-X.                                     NC2104.2
065200     GO TO IF-FAIL-GF-X.                                          NC2104.2
065300 IF-DELETE-GF-X.                                                  NC2104.2
065400     MOVE 63 TO PAR-NUMBER.                                       NC2104.2
065500     PERFORM DE-LETE.                                             NC2104.2
065600     ADD 63 TO DELETE-COUNTER.                                    NC2104.2
065700     GO TO IF-WRITE-GF-X.                                         NC2104.2
065800 IF-FAIL-GF-X.                                                    NC2104.2
065900     MOVE BUILT-TABLE TO COMPUTED-A.                              NC2104.2
066000     MOVE CORRECT-ENTRY (SUB-SCRIPT) TO CORRECT-A.                NC2104.2
066100     PERFORM FAIL.                                                NC2104.2
066200 IF-WRITE-GF-X.                                                   NC2104.2
066300     ADD 1 TO PAR-NUMBER.                                         NC2104.2
066400     MOVE PARAGRAPH-NAME TO PAR-NAME.                             NC2104.2
066500     PERFORM PRINT-DETAIL.                                        NC2104.2
066600     IF PAR-NUMBER EQUAL TO 64 GO TO IF-INIT-GF-Y.                NC2104.2
066700     ADD 1 TO  SUB-SCRIPT.                                        NC2104.2
066800     MOVE SPACES TO BUILT-TABLE.                                  NC2104.2
066900     ADD 1 TO ONE-X.                                              NC2104.2
067000     IF ONE-F EQUAL TO 2 ADD 8 TO ONE-X.                          NC2104.2
067100     IF ONE-E EQUAL TO 2 ADD 80 TO ONE-X.                         NC2104.2
067200     IF ONE-D EQUAL TO 2 ADD 800 TO ONE-X.                        NC2104.2
067300     IF ONE-C EQUAL TO 2 ADD 8000 TO ONE-X.                       NC2104.2
067400     IF ONE-B EQUAL TO 2 ADD 80000 TO ONE-X.                      NC2104.2
067500     GO TO IF-INIT-GF-X.                                          NC2104.2
067600*                                                                 NC2104.2
067700 IF-INIT-GF-Y.                                                    NC2104.2
067800     MOVE   "VI-89 6.15.4 GR1(C)" TO ANSI-REFERENCE.              NC2104.2
067900     ADD 1 TO PAR-NUMBER.                                         NC2104.2
068000     MOVE 22 TO ACCUM-DATANAME.                                   NC2104.2
068100 IF-TEST-GF-Y.                                                    NC2104.2
068200     MOVE "FALSE" TO T-F.                                         NC2104.2
068300     IF ACCUM-DATANAME NOT EQUAL TO 1                             NC2104.2
068400     IF ACCUM-DATANAME NOT EQUAL TO 2                             NC2104.2
068500     IF ACCUM-DATANAME NOT EQUAL TO 3                             NC2104.2
068600     IF ACCUM-DATANAME NOT EQUAL TO 4                             NC2104.2
068700     IF ACCUM-DATANAME NOT EQUAL TO 5                             NC2104.2
068800     IF ACCUM-DATANAME NOT EQUAL TO 6                             NC2104.2
068900     IF ACCUM-DATANAME NOT EQUAL TO 7                             NC2104.2
069000     IF ACCUM-DATANAME NOT EQUAL TO 8                             NC2104.2
069100     IF ACCUM-DATANAME NOT EQUAL TO 9                             NC2104.2
069200     IF ACCUM-DATANAME NOT EQUAL TO 10                            NC2104.2
069300     IF ACCUM-DATANAME NOT EQUAL TO 11                            NC2104.2
069400     IF ACCUM-DATANAME NOT EQUAL TO 12                            NC2104.2
069500     IF ACCUM-DATANAME NOT EQUAL TO 13                            NC2104.2
069600     IF ACCUM-DATANAME NOT EQUAL TO 14                            NC2104.2
069700     IF ACCUM-DATANAME NOT EQUAL TO 15                            NC2104.2
069800     IF ACCUM-DATANAME NOT EQUAL TO 16                            NC2104.2
069900     IF ACCUM-DATANAME NOT EQUAL TO 17                            NC2104.2
070000     IF ACCUM-DATANAME NOT EQUAL TO 18                            NC2104.2
070100     IF ACCUM-DATANAME NOT EQUAL TO 19                            NC2104.2
070200     IF ACCUM-DATANAME NOT EQUAL TO 20                            NC2104.2
070300     IF ACCUM-DATANAME NOT EQUAL TO 21                            NC2104.2
070400         MOVE "TRUE" TO T-F.                                      NC2104.2
070500     IF ACCUM-DATANAME EQUAL TO 22 AND T-F EQUAL TO "TRUE"        NC2104.2
070600         PERFORM PASS                                             NC2104.2
070700         PERFORM IF-WRITE-GF-Y                                    NC2104.2
070800         SUBTRACT 1 FROM ACCUM-DATANAME                           NC2104.2
070900         GO TO IF-TEST-GF-Y.                                      NC2104.2
071000     IF ACCUM-DATANAME LESS THAN 22 AND T-F EQUAL TO "FALSE"      NC2104.2
071100         PERFORM PASS                                             NC2104.2
071200         GO TO IF-WRITE-GF-Y                                      NC2104.2
071300     ELSE  GO TO IF-FAIL-GF-Y.                                    NC2104.2
071400 IF-DELETE-GF-Y.                                                  NC2104.2
071500     ADD 21 TO PAR-NUMBER.                                        NC2104.2
071600     PERFORM DE-LETE.                                             NC2104.2
071700     ADD 21 TO DELETE-COUNTER.                                    NC2104.2
071800     MOVE 1 TO ACCUM-DATANAME.                                    NC2104.2
071900     GO TO IF-WRITE-GF-Y.                                         NC2104.2
072000 IF-FAIL-GF-Y.                                                    NC2104.2
072100     MOVE "*****" TO COMPUTED-A  CORRECT-A.                       NC2104.2
072200     MOVE ACCUM-DATANAME TO CHECK-VALU.                           NC2104.2
072300     MOVE CHECK-PARA TO RE-MARK.                                  NC2104.2
072400     PERFORM FAIL.                                                NC2104.2
072500 IF-WRITE-GF-Y.                                                   NC2104.2
072600     MOVE PARAGRAPH-NAME TO PAR-NAME.                             NC2104.2
072700     ADD 1 TO PAR-NUMBER.                                         NC2104.2
072800     PERFORM PRINT-DETAIL.                                        NC2104.2
072900     IF ACCUM-DATANAME EQUAL TO 1 GO TO TEST-EXIT.                NC2104.2
073000     SUBTRACT 1 FROM ACCUM-DATANAME.                              NC2104.2
073100 IF-RETURN-GF-Y.                                                  NC2104.2
073200     GO TO IF-TEST-GF-Y.                                          NC2104.2
073300 TEST-EXIT.                                                       NC2104.2
073400     EXIT.                                                        NC2104.2
073500 CCVS-EXIT SECTION.                                               NC2104.2
073600 CCVS-999999.                                                     NC2104.2
073700     GO TO CLOSE-FILES.                                           NC2104.2
      *END-OF,NC210A                                                                  
