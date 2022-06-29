      *HEADER,COBOL,IX109A,SUBPRG,IX111A                                              
000100 IDENTIFICATION DIVISION.                                         IX1114.2
000200 PROGRAM-ID.                                                      IX1114.2
000300     IX111A.                                                      IX1114.2
000400****************************************************************  IX1114.2
000500*                                                              *  IX1114.2
000600*    VALIDATION FOR:-                                          *  IX1114.2
000700*                                                              *  IX1114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1114.2
000900*                                                              *  IX1114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1114.2
001100*                                                              *  IX1114.2
001200****************************************************************  IX1114.2
001300*                                                                 IX1114.2
001400*    1. THE ROUTINE CHECKS THE PERMANENT ERROR CONDITIONS WITH    IX1114.2
001500*       UNSUCCESSFUL COMPLETION FOR AN OPEN STATEMENT FOR A FILE  IX1114.2
001600*       WHICH IS NOT PRESENT (STATUS CODE 35 EXPECTED).           IX1114.2
001700*       THIS PROGRAM USES THE FILE IX-NOP WHICH DOES NOT EXIST.   IX1114.2
001800*                                                                 IX1114.2
001900*    2. THE ROUTINE CHECKS THE FILE STATUS CODES:                 IX1114.2
002000*           35  -  AFTER OPEN INPUT  (NOT EXISTING FILE)          IX1114.2
002100                                                                  IX1114.2
002200*    3. X-CARDS USED IN THIS PROGRAM (WITH THE OPT CODE):         IX1114.2
002300*                                                                 IX1114.2
002400*                 XXXXX025    (FOR THE NON EXISTING IX-NOP)       IX1114.2
002500*                 XXXXX055.                                       IX1114.2
002600*                 XXXXX082.                                       IX1114.2
002700*                 XXXXX083.                                       IX1114.2
002800*                                                                 IX1114.2
002900*                                                                 IX1114.2
003000 ENVIRONMENT DIVISION.                                            IX1114.2
003100 CONFIGURATION SECTION.                                           IX1114.2
003200 SOURCE-COMPUTER.                                                 IX1114.2
003300     XXXXX082.                                                    IX1114.2
003400 OBJECT-COMPUTER.                                                 IX1114.2
003500     XXXXX083.                                                    IX1114.2
003600 INPUT-OUTPUT SECTION.                                            IX1114.2
003700 FILE-CONTROL.                                                    IX1114.2
003800     SELECT PRINT-FILE ASSIGN TO                                  IX1114.2
003900     XXXXX055.                                                    IX1114.2
004000*                                                                 IX1114.2
004100     SELECT IX-NOP ASSIGN                                         IX1114.2
004200     XXXXX025                                                     IX1114.2
004300     ORGANIZATION IS INDEXED                                      IX1114.2
004400     ACCESS MODE IS SEQUENTIAL                                    IX1114.2
004500     RECORD KEY IS IX-NOP-KEY                                     IX1114.2
004600     FILE STATUS IS IX-NOP-STATUS.                                IX1114.2
004700                                                                  IX1114.2
004800                                                                  IX1114.2
004900 DATA DIVISION.                                                   IX1114.2
005000                                                                  IX1114.2
005100 FILE SECTION.                                                    IX1114.2
005200                                                                  IX1114.2
005300 FD  PRINT-FILE.                                                  IX1114.2
005400                                                                  IX1114.2
005500 01  PRINT-REC               PIC X(120).                          IX1114.2
005600                                                                  IX1114.2
005700 01  DUMMY-RECORD            PIC X(120).                          IX1114.2
005800                                                                  IX1114.2
005900 FD  IX-NOP                                                       IX1114.2
006000C       LABEL RECORD STANDARD                                     IX1114.2
006100C       DATA RECORDS IX-NOPR1-F-G-240                             IX1114.2
006200        RECORD 240                                                IX1114.2
006300        BLOCK CONTAINS 2 RECORDS.                                 IX1114.2
006400                                                                  IX1114.2
006500 01  IX-NOPR1-F-G-240.                                            IX1114.2
006600     05  IX-NOP-REC-120      PIC X(120).                          IX1114.2
006700     05  IX-NOP-REC-120-240.                                      IX1114.2
006800         10  FILLER          PIC X(8).                            IX1114.2
006900         10  IX-NOP-KEY      PIC X(29).                           IX1114.2
007000         10  FILLER          PIC X(9).                            IX1114.2
007100         10  IX-NOP-ALTER-KEY  PIC X(29).                         IX1114.2
007200         10  FILLER            PIC X(45).                         IX1114.2
007300                                                                  IX1114.2
007400                                                                  IX1114.2
007500 WORKING-STORAGE SECTION.                                         IX1114.2
007600                                                                  IX1114.2
007700 01  IX-NOP-STATUS           PIC XX.                              IX1114.2
007800                                                                  IX1114.2
007900 01  TEST-RESULTS.                                                IX1114.2
008000     02 FILLER                   PIC X      VALUE SPACE.          IX1114.2
008100     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1114.2
008200     02 FILLER                   PIC X      VALUE SPACE.          IX1114.2
008300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1114.2
008400     02 FILLER                   PIC X      VALUE SPACE.          IX1114.2
008500     02  PAR-NAME.                                                IX1114.2
008600       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1114.2
008700       03  PARDOT-X              PIC X      VALUE SPACE.          IX1114.2
008800       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1114.2
008900     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1114.2
009000     02 RE-MARK                  PIC X(61).                       IX1114.2
009100 01  TEST-COMPUTED.                                               IX1114.2
009200     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1114.2
009300     02 FILLER                   PIC X(17)  VALUE                 IX1114.2
009400            "       COMPUTED=".                                   IX1114.2
009500     02 COMPUTED-X.                                               IX1114.2
009600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1114.2
009700     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1114.2
009800                                 PIC -9(9).9(9).                  IX1114.2
009900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1114.2
010000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1114.2
010100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1114.2
010200     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1114.2
010300         04 COMPUTED-18V0                    PIC -9(18).          IX1114.2
010400         04 FILLER                           PIC X.               IX1114.2
010500     03 FILLER PIC X(50) VALUE SPACE.                             IX1114.2
010600 01  TEST-CORRECT.                                                IX1114.2
010700     02 FILLER PIC X(30) VALUE SPACE.                             IX1114.2
010800     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1114.2
010900     02 CORRECT-X.                                                IX1114.2
011000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1114.2
011100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1114.2
011200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1114.2
011300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1114.2
011400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1114.2
011500     03      CR-18V0 REDEFINES CORRECT-A.                         IX1114.2
011600         04 CORRECT-18V0                     PIC -9(18).          IX1114.2
011700         04 FILLER                           PIC X.               IX1114.2
011800     03 FILLER PIC X(2) VALUE SPACE.                              IX1114.2
011900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1114.2
012000 01  CCVS-C-1.                                                    IX1114.2
012100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1114.2
012200-    "SS  PARAGRAPH-NAME                                          IX1114.2
012300-    "       REMARKS".                                            IX1114.2
012400     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1114.2
012500 01  CCVS-C-2.                                                    IX1114.2
012600     02 FILLER                     PIC X        VALUE SPACE.      IX1114.2
012700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1114.2
012800     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1114.2
012900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1114.2
013000     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1114.2
013100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1114.2
013200 01  REC-CT                        PIC 99       VALUE ZERO.       IX1114.2
013300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1114.2
013400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1114.2
013500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1114.2
013600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1114.2
013700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1114.2
013800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1114.2
013900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1114.2
014000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1114.2
014100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1114.2
014200 01  CCVS-H-1.                                                    IX1114.2
014300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1114.2
014400     02  FILLER                    PIC X(42)    VALUE             IX1114.2
014500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1114.2
014600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1114.2
014700 01  CCVS-H-2A.                                                   IX1114.2
014800   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1114.2
014900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1114.2
015000   02  FILLER                        PIC XXXX   VALUE             IX1114.2
015100     "4.2 ".                                                      IX1114.2
015200   02  FILLER                        PIC X(28)  VALUE             IX1114.2
015300            " COPY - NOT FOR DISTRIBUTION".                       IX1114.2
015400   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1114.2
015500                                                                  IX1114.2
015600 01  CCVS-H-2B.                                                   IX1114.2
015700   02  FILLER                        PIC X(15)  VALUE             IX1114.2
015800            "TEST RESULT OF ".                                    IX1114.2
015900   02  TEST-ID                       PIC X(9).                    IX1114.2
016000   02  FILLER                        PIC X(4)   VALUE             IX1114.2
016100            " IN ".                                               IX1114.2
016200   02  FILLER                        PIC X(12)  VALUE             IX1114.2
016300     " HIGH       ".                                              IX1114.2
016400   02  FILLER                        PIC X(22)  VALUE             IX1114.2
016500            " LEVEL VALIDATION FOR ".                             IX1114.2
016600   02  FILLER                        PIC X(58)  VALUE             IX1114.2
016700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1114.2
016800 01  CCVS-H-3.                                                    IX1114.2
016900     02  FILLER                      PIC X(34)  VALUE             IX1114.2
017000            " FOR OFFICIAL USE ONLY    ".                         IX1114.2
017100     02  FILLER                      PIC X(58)  VALUE             IX1114.2
017200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1114.2
017300     02  FILLER                      PIC X(28)  VALUE             IX1114.2
017400            "  COPYRIGHT   1985 ".                                IX1114.2
017500 01  CCVS-E-1.                                                    IX1114.2
017600     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1114.2
017700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1114.2
017800     02 ID-AGAIN                     PIC X(9).                    IX1114.2
017900     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1114.2
018000 01  CCVS-E-2.                                                    IX1114.2
018100     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1114.2
018200     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1114.2
018300     02 CCVS-E-2-2.                                               IX1114.2
018400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1114.2
018500         03 FILLER                   PIC X      VALUE SPACE.      IX1114.2
018600         03 ENDER-DESC               PIC X(44)  VALUE             IX1114.2
018700            "ERRORS ENCOUNTERED".                                 IX1114.2
018800 01  CCVS-E-3.                                                    IX1114.2
018900     02  FILLER                      PIC X(22)  VALUE             IX1114.2
019000            " FOR OFFICIAL USE ONLY".                             IX1114.2
019100     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1114.2
019200     02  FILLER                      PIC X(58)  VALUE             IX1114.2
019300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1114.2
019400     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1114.2
019500     02 FILLER                       PIC X(15)  VALUE             IX1114.2
019600             " COPYRIGHT 1985".                                   IX1114.2
019700 01  CCVS-E-4.                                                    IX1114.2
019800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1114.2
019900     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1114.2
020000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1114.2
020100     02 FILLER                       PIC X(40)  VALUE             IX1114.2
020200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1114.2
020300 01  XXINFO.                                                      IX1114.2
020400     02 FILLER                       PIC X(19)  VALUE             IX1114.2
020500            "*** INFORMATION ***".                                IX1114.2
020600     02 INFO-TEXT.                                                IX1114.2
020700       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1114.2
020800       04 XXCOMPUTED                 PIC X(20).                   IX1114.2
020900       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1114.2
021000       04 XXCORRECT                  PIC X(20).                   IX1114.2
021100     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1114.2
021200 01  HYPHEN-LINE.                                                 IX1114.2
021300     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1114.2
021400     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1114.2
021500-    "*****************************************".                 IX1114.2
021600     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1114.2
021700-    "******************************".                            IX1114.2
021800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1114.2
021900     "IX111A".                                                    IX1114.2
022000 PROCEDURE DIVISION.                                              IX1114.2
022100 DECLARATIVES.                                                    IX1114.2
022200                                                                  IX1114.2
022300 SECT-IX111-0001 SECTION.                                         IX1114.2
022400     USE AFTER EXCEPTION PROCEDURE ON IX-NOP.                     IX1114.2
022500 INPUT-PROCESS.                                                   IX1114.2
022600     IF IX-NOP-STATUS = "35"                                      IX1114.2
022700             PERFORM PASS-DECL                                    IX1114.2
022800             GO TO ABNORMAL-TERM-DECL                             IX1114.2
022900     ELSE                                                         IX1114.2
023000             MOVE "35" TO CORRECT-A                               IX1114.2
023100             MOVE IX-NOP-STATUS TO COMPUTED-A                     IX1114.2
023200             MOVE "STATUS FOR OPEN INPUT OF FILE THAT IS NOT      IX1114.2
023300-                 "PRESENT INCORRECT" TO RE-MARK                  IX1114.2
023400             MOVE "IX-2, FILE STATUS" TO ANSI-REFERENCE           IX1114.2
023500             PERFORM FAIL-DECL                                    IX1114.2
023600             GO TO ABNORMAL-TERM-DECL                             IX1114.2
023700     END-IF.                                                      IX1114.2
023800                                                                  IX1114.2
023900 PASS-DECL.                                                       IX1114.2
024000     MOVE   "PASS " TO P-OR-F.                                    IX1114.2
024100     ADD     1 TO PASS-COUNTER.                                   IX1114.2
024200     PERFORM PRINT-DETAIL-DECL.                                   IX1114.2
024300*                                                                 IX1114.2
024400 FAIL-DECL.                                                       IX1114.2
024500     MOVE   "FAIL*" TO P-OR-F.                                    IX1114.2
024600     ADD     1 TO ERROR-COUNTER.                                  IX1114.2
024700     PERFORM PRINT-DETAIL-DECL.                                   IX1114.2
024800*                                                                 IX1114.2
024900 PRINT-DETAIL-DECL.                                               IX1114.2
025000     IF REC-CT NOT EQUAL TO ZERO                                  IX1114.2
025100         MOVE   "." TO PARDOT-X                                   IX1114.2
025200         MOVE   REC-CT TO DOTVALUE.                               IX1114.2
025300     MOVE    TEST-RESULTS TO PRINT-REC.                           IX1114.2
025400     PERFORM WRITE-LINE-DECL.                                     IX1114.2
025500     IF P-OR-F EQUAL TO "FAIL*"                                   IX1114.2
025600         PERFORM WRITE-LINE-DECL                                  IX1114.2
025700         PERFORM FAIL-ROUTINE-DECL THRU FAIL-ROUTINE-EX-DECL      IX1114.2
025800     ELSE                                                         IX1114.2
025900         PERFORM BAIL-OUT-DECL THRU BAIL-OUT-EX-DECL.             IX1114.2
026000     MOVE    SPACE TO P-OR-F.                                     IX1114.2
026100     MOVE    SPACE TO COMPUTED-X.                                 IX1114.2
026200     MOVE    SPACE TO CORRECT-X.                                  IX1114.2
026300     IF REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.             IX1114.2
026400     MOVE    SPACE TO RE-MARK.                                    IX1114.2
026500*                                                                 IX1114.2
026600 WRITE-LINE-DECL.                                                 IX1114.2
026700     ADD     1 TO RECORD-COUNT.                                   IX1114.2
026800     PERFORM WRT-LN-DECL.                                         IX1114.2
026900*                                                                 IX1114.2
027000 WRT-LN-DECL.                                                     IX1114.2
027100     WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                IX1114.2
027200     MOVE    SPACE TO DUMMY-RECORD.                               IX1114.2
027300 BLANK-LINE-PRINT-DECL.                                           IX1114.2
027400     PERFORM WRT-LN-DECL.                                         IX1114.2
027500 FAIL-ROUTINE-DECL.                                               IX1114.2
027600     IF COMPUTED-X NOT EQUAL TO SPACE                             IX1114.2
027700             GO TO FAIL-ROUTINE-WRITE-DECL.                       IX1114.2
027800     IF CORRECT-X NOT EQUAL TO SPACE                              IX1114.2
027900             GO TO FAIL-ROUTINE-WRITE-DECL.                       IX1114.2
028000     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                IX1114.2
028100     MOVE   "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.  IX1114.2
028200     MOVE    XXINFO TO DUMMY-RECORD.                              IX1114.2
028300     PERFORM WRITE-LINE-DECL 2 TIMES.                             IX1114.2
028400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        IX1114.2
028500     GO TO   FAIL-ROUTINE-EX-DECL.                                IX1114.2
028600 FAIL-ROUTINE-WRITE-DECL.                                         IX1114.2
028700     MOVE    TEST-COMPUTED  TO PRINT-REC                          IX1114.2
028800     PERFORM WRITE-LINE-DECL                                      IX1114.2
028900     MOVE    ANSI-REFERENCE TO COR-ANSI-REFERENCE.                IX1114.2
029000     MOVE    TEST-CORRECT   TO PRINT-REC                          IX1114.2
029100     PERFORM WRITE-LINE-DECL 2 TIMES.                             IX1114.2
029200     MOVE    SPACES         TO COR-ANSI-REFERENCE.                IX1114.2
029300 FAIL-ROUTINE-EX-DECL.                                            IX1114.2
029400     EXIT.                                                        IX1114.2
029500 BAIL-OUT-DECL.                                                   IX1114.2
029600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-DECL.  IX1114.2
029700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-DECL.          IX1114.2
029800 BAIL-OUT-WRITE-DECL.                                             IX1114.2
029900     MOVE    CORRECT-A      TO XXCORRECT.                         IX1114.2
030000     MOVE    COMPUTED-A     TO XXCOMPUTED.                        IX1114.2
030100     MOVE    ANSI-REFERENCE TO INF-ANSI-REFERENCE.                IX1114.2
030200     MOVE    XXINFO TO DUMMY-RECORD.                              IX1114.2
030300     PERFORM WRITE-LINE-DECL 2 TIMES.                             IX1114.2
030400     MOVE    SPACES TO INF-ANSI-REFERENCE.                        IX1114.2
030500 BAIL-OUT-EX-DECL.                                                IX1114.2
030600     EXIT.                                                        IX1114.2
030700*                                                                 IX1114.2
030800 ABNORMAL-TERM-DECL.                                              IX1114.2
030900     MOVE    SPACE TO DUMMY-RECORD.                               IX1114.2
031000     PERFORM WRITE-LINE-DECL.                                     IX1114.2
031100     MOVE    "ABNORMAL TERMINATION AT THIS POINT IS ACCEPTABLE"   IX1114.2
031200                     TO DUMMY-RECORD.                             IX1114.2
031300     PERFORM WRITE-LINE-DECL 3 TIMES.                             IX1114.2
031400*                                                                 IX1114.2
031500 EXIT-DECL.                                                       IX1114.2
031600     EXIT.                                                        IX1114.2
031700 END DECLARATIVES.                                                IX1114.2
031800                                                                  IX1114.2
031900                                                                  IX1114.2
032000 CCVS1 SECTION.                                                   IX1114.2
032100 OPEN-FILES.                                                      IX1114.2
032200     OPEN    OUTPUT PRINT-FILE.                                   IX1114.2
032300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1114.2
032400     MOVE    SPACE TO TEST-RESULTS.                               IX1114.2
032500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1114.2
032600     MOVE    ZERO TO REC-SKL-SUB.                                 IX1114.2
032700 CCVS-INIT-EXIT.                                                  IX1114.2
032800     GO TO CCVS1-EXIT.                                            IX1114.2
032900 CLOSE-FILES.                                                     IX1114.2
033000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1114.2
033100 TERMINATE-CCVS.                                                  IX1114.2
033200     STOP     RUN.                                                IX1114.2
033300 HEAD-ROUTINE.                                                    IX1114.2
033400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1114.2
033500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1114.2
033600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1114.2
033700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1114.2
033800 COLUMN-NAMES-ROUTINE.                                            IX1114.2
033900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1114.2
034000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1114.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1114.2
034200 END-ROUTINE.                                                     IX1114.2
034300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1114.2
034400 END-RTN-EXIT.                                                    IX1114.2
034500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1114.2
034600 END-ROUTINE-1.                                                   IX1114.2
034700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1114.2
034800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1114.2
034900      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1114.2
035000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1114.2
035100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1114.2
035200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1114.2
035300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1114.2
035400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1114.2
035500  END-ROUTINE-12.                                                 IX1114.2
035600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1114.2
035700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1114.2
035800         MOVE "NO " TO ERROR-TOTAL                                IX1114.2
035900         ELSE                                                     IX1114.2
036000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1114.2
036100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1114.2
036200     PERFORM WRITE-LINE.                                          IX1114.2
036300 END-ROUTINE-13.                                                  IX1114.2
036400     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1114.2
036500         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1114.2
036600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1114.2
036700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1114.2
036800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1114.2
036900      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1114.2
037000          MOVE "NO " TO ERROR-TOTAL                               IX1114.2
037100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1114.2
037200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1114.2
037300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1114.2
037400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1114.2
037500 WRITE-LINE.                                                      IX1114.2
037600     ADD 1 TO RECORD-COUNT.                                       IX1114.2
037700*    IF RECORD-COUNT GREATER 42                                   IX1114.2
037800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1114.2
037900*        MOVE SPACE TO DUMMY-RECORD                               IX1114.2
038000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1114.2
038100*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1114.2
038200*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1114.2
038300*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1114.2
038400*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1114.2
038500*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1114.2
038600*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1114.2
038700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1114.2
038800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1114.2
038900*        MOVE ZERO TO RECORD-COUNT.                               IX1114.2
039000     PERFORM WRT-LN.                                              IX1114.2
039100 WRT-LN.                                                          IX1114.2
039200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1114.2
039300     MOVE SPACE TO DUMMY-RECORD.                                  IX1114.2
039400 BLANK-LINE-PRINT.                                                IX1114.2
039500     PERFORM WRT-LN.                                              IX1114.2
039600 FAIL-ROUTINE.                                                    IX1114.2
039700     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1114.2
039800            GO TO   FAIL-ROUTINE-WRITE.                           IX1114.2
039900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1114.2
040000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1114.2
040100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1114.2
040200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1114.2
040300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1114.2
040400     GO TO  FAIL-ROUTINE-EX.                                      IX1114.2
040500 FAIL-ROUTINE-WRITE.                                              IX1114.2
040600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1114.2
040700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1114.2
040800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1114.2
040900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1114.2
041000 FAIL-ROUTINE-EX. EXIT.                                           IX1114.2
041100 BAIL-OUT.                                                        IX1114.2
041200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1114.2
041300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1114.2
041400 BAIL-OUT-WRITE.                                                  IX1114.2
041500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1114.2
041600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1114.2
041700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1114.2
041800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1114.2
041900 BAIL-OUT-EX. EXIT.                                               IX1114.2
042000 CCVS1-EXIT.                                                      IX1114.2
042100     EXIT.                                                        IX1114.2
042200                                                                  IX1114.2
042300 SECT-IX111A-0003 SECTION.                                        IX1114.2
042400 SEQ-INIT-010.                                                    IX1114.2
042500                                                                  IX1114.2
042600******************************************************************IX1114.2
042700*   TEST  1                                                      *IX1114.2
042800*         OPEN INPUT  (FILE DOES NOT EXIST)   35  EXPECTED       *IX1114.2
042900*         IX-4, 1.3.4 (4) B                                      *IX1114.2
043000******************************************************************IX1114.2
043100 OPN-INIT-GF-01-0.                                                IX1114.2
043200     MOVE SPACES TO IX-NOP-STATUS.                                IX1114.2
043300     MOVE "OPEN INPUT   35 EXP." TO FEATURE.                      IX1114.2
043400     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1114.2
043500 OPN-TEST-GF-01-0.                                                IX1114.2
043600     OPEN                                                         IX1114.2
043700        INPUT IX-NOP.                                             IX1114.2
043800                                                                  IX1114.2
043900 TERMINATE-ROUTINE.                                               IX1114.2
044000     EXIT.                                                        IX1114.2
044100                                                                  IX1114.2
044200 CCVS-EXIT SECTION.                                               IX1114.2
044300 CCVS-999999.                                                     IX1114.2
044400     GO TO CLOSE-FILES.                                           IX1114.2
      *END-OF,IX111A                                                          TES06680
