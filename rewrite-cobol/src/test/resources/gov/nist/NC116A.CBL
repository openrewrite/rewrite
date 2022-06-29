      *HEADER,COBOL,NC116A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1164.2
000200 PROGRAM-ID.                                                      NC1164.2
000300     NC116A.                                                      NC1164.2
000400****************************************************************  NC1164.2
000500*                                                              *  NC1164.2
000600*    VALIDATION FOR:-                                          *  NC1164.2
000700*                                                              *  NC1164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1164.2
000900*                                                              *  NC1164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1164.2
001100*                                                              *  NC1164.2
001200****************************************************************  NC1164.2
001300*                                                              *  NC1164.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1164.2
001500*                                                              *  NC1164.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1164.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1164.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1164.2
001900*                                                              *  NC1164.2
002000****************************************************************  NC1164.2
002100*                                                                 NC1164.2
002200*    PROGRAM NC116A TESTS THE USE OF THE SIGN CLAUSE              NC1164.2
002300*    USING THE "IF" AND "MOVE" STATEMENTS.  ALL COMBINATIONS      NC1164.2
002400*    OF THE SIGN CLAUSE PHRASES ARE TESTED USING DATA ITEMS OF    NC1164.2
002500*    VARIOUS LENGTHS.                                             NC1164.2
002600*                                                                 NC1164.2
002700 ENVIRONMENT DIVISION.                                            NC1164.2
002800 CONFIGURATION SECTION.                                           NC1164.2
002900 SOURCE-COMPUTER.                                                 NC1164.2
003000     XXXXX082.                                                    NC1164.2
003100 OBJECT-COMPUTER.                                                 NC1164.2
003200     XXXXX083.                                                    NC1164.2
003300 INPUT-OUTPUT SECTION.                                            NC1164.2
003400 FILE-CONTROL.                                                    NC1164.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1164.2
003600     XXXXX055.                                                    NC1164.2
003700 DATA DIVISION.                                                   NC1164.2
003800 FILE SECTION.                                                    NC1164.2
003900 FD  PRINT-FILE.                                                  NC1164.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1164.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1164.2
004200 WORKING-STORAGE SECTION.                                         NC1164.2
004300*=======================                                          NC1164.2
004400 01  TEST-17-DATA  SIGN TRAILING.                                 NC1164.2
004500   03  TEST-17-A          PIC S9(4).                              NC1164.2
004600   03  TEST-17-B          PIC S9(4).                              NC1164.2
004700   03  TEST-17-GROUP SIGN LEADING SEPARATE.                       NC1164.2
004800     05  TEST-17-C        PIC S9(4).                              NC1164.2
004900     05  FILLER           REDEFINES TEST-17-C.                    NC1164.2
005000       07  TEST-17-C-SIGN PIC X.                                  NC1164.2
005100       07  FILLER         PIC X(4).                               NC1164.2
005200                                                                  NC1164.2
005300 01  TEST-18-DATA  SIGN TRAILING.                                 NC1164.2
005400   03  TEST-18-A          PIC S9(4).                              NC1164.2
005500   03  TEST-18-B          PIC S9(4)  SIGN LEADING SEPARATE.       NC1164.2
005600   03  TEST-18-BX         REDEFINES TEST-18-B.                    NC1164.2
005700     05  TEST-18-B-SIGN   PIC X.                                  NC1164.2
005800     05  FILLER           PIC X(4).                               NC1164.2
005900   03  TEST-18-C          PIC S9(4).                              NC1164.2
006000 01  DS-LS-5  PICTURE S99999                                      NC1164.2
006100         SIGN IS LEADING SEPARATE CHARACTER                       NC1164.2
006200         VALUE +91275.                                            NC1164.2
006300 01  GRP-001 REDEFINES DS-LS-5.                                   NC1164.2
006400     02  TEST1-AN-1 PICTURE X.                                    NC1164.2
006500     02  TEST1-AN-5 PICTURE X(5).                                 NC1164.2
006600 01  DS-LS-4 PICTURE S9999                                        NC1164.2
006700         SIGN IS LEADING SEPARATE CHARACTER                       NC1164.2
006800         VALUE -9127.                                             NC1164.2
006900 01  GRP-002 REDEFINES DS-LS-4.                                   NC1164.2
007000     02  TEST1N-AN-1 PICTURE X.                                   NC1164.2
007100     02  TEST1N-AN-4 PICTURE X(4).                                NC1164.2
007200 01  DS-TS-5  PICTURE S99999                                      NC1164.2
007300         SIGN IS TRAILING SEPARATE CHARACTER                      NC1164.2
007400         VALUE +80361.                                            NC1164.2
007500 01  GRP-003 REDEFINES DS-TS-5.                                   NC1164.2
007600     02  TEST2-AN-5 PICTURE X(5).                                 NC1164.2
007700     02  TEST2-AN-1 PICTURE X.                                    NC1164.2
007800 01  DS-TS-4 PICTURE S9999                                        NC1164.2
007900         SIGN IS TRAILING SEPARATE CHARACTER                      NC1164.2
008000         VALUE -8036.                                             NC1164.2
008100 01  GRP-004 REDEFINES DS-TS-4.                                   NC1164.2
008200     02  TEST2N-AN-4 PICTURE X(4).                                NC1164.2
008300     02  TEST2N-AN-1 PICTURE X.                                   NC1164.2
008400 01  DS-L-5  PICTURE S99999  VALUE +91275                         NC1164.2
008500         SIGN IS LEADING.                                         NC1164.2
008600 01  GRP-005 REDEFINES DS-L-5.                                    NC1164.2
008700     02  TEST3-AN-1  PICTURE X.                                   NC1164.2
008800     02  TEST3-AN-4  PICTURE X(4).                                NC1164.2
008900 01  DS-L-4  PICTURE S9999   VALUE -9127                          NC1164.2
009000         SIGN IS LEADING.                                         NC1164.2
009100 01  GRP-006 REDEFINES DS-L-4.                                    NC1164.2
009200     02  TEST3N-AN-1 PICTURE X.                                   NC1164.2
009300     02  TEST3N-AN-3 PICTURE XXX.                                 NC1164.2
009400 01  DS-T-5  PICTURE S99999  VALUE +83621                         NC1164.2
009500         SIGN IS TRAILING.                                        NC1164.2
009600 01  GRP-007 REDEFINES DS-T-5.                                    NC1164.2
009700     02  TEST4-AN-4  PICTURE X(4).                                NC1164.2
009800     02  TEST4-AN-1  PICTURE X.                                   NC1164.2
009900 01  DS-T-4  PICTURE S9999   VALUE -3621                          NC1164.2
010000         SIGN IS TRAILING.                                        NC1164.2
010100 01  GRP-008 REDEFINES DS-T-4.                                    NC1164.2
010200     02  TEST4N-AN-3 PICTURE XXX.                                 NC1164.2
010300     02  TEST4N-AN-1 PICTURE X.                                   NC1164.2
010400 01  DU-005  PICTURE 9(5) VALUE ZERO.                             NC1164.2
010500 01  DS-005  PICTURE S9(5) VALUE 0.                               NC1164.2
010600 01  CU-005  PICTURE 9(5)  USAGE COMPUTATIONAL  VALUE 0.          NC1164.2
010700 01  CS-005  PICTURE S9(5)  USAGE COMPUTATIONAL  VALUE 0.         NC1164.2
010800 01  WRK-DS-LS-5 PICTURE S99999   VALUE ZERO                      NC1164.2
010900         SIGN LEADING SEPARATE.                                   NC1164.2
011000 01  GRP-09 REDEFINES WRK-DS-LS-5 PICTURE X(6).                   NC1164.2
011100 01  WRK-DS-TS-5 PICTURE S99999  VALUE ZERO                       NC1164.2
011200         SIGN TRAILING SEPARATE.                                  NC1164.2
011300 01  GRP-10 REDEFINES WRK-DS-TS-5 PICTURE X(6).                   NC1164.2
011400 01  WRK-DS-L-5 PICTURE S99999   VALUE ZERO                       NC1164.2
011500         SIGN LEADING.                                            NC1164.2
011600 01  WRK-DS-T-5 PICTURE S99999   VALUE ZERO                       NC1164.2
011700         SIGN TRAILING.                                           NC1164.2
011800 01  AN-006  PICTURE X(6) VALUE SPACE.                            NC1164.2
011900 01  DS-L-00008         PIC S9(8) SIGN LEADING VALUE +01234567.   NC1164.2
012000 01  AN-00008-X-1 REDEFINES DS-L-00008      PIC X(8).             NC1164.2
012100 01  DS-T-00008         PIC S9(8) SIGN TRAILING VALUE -01234567.  NC1164.2
012200 01  AN-00008-X-2 REDEFINES DS-T-00008      PIC X(8).             NC1164.2
012300 01  DS-T-00008-1       PIC S9(8) SIGN TRAILING VALUE +01234567.  NC1164.2
012400 01  AN-00008-X-5 REDEFINES DS-T-00008-1     PIC X(8).            NC1164.2
012500 01  DS-LS-00008        PIC S9(8)                                 NC1164.2
012600                        SIGN IS LEADING SEPARATE CHARACTER        NC1164.2
012700                        VALUE -07654321.                          NC1164.2
012800 01  AN-00009-X-3 REDEFINES DS-LS-00008     PIC X(9).             NC1164.2
012900 01  DS-LS-00008-1      PIC S9(8)                                 NC1164.2
013000                        SIGN IS LEADING SEPARATE CHARACTER        NC1164.2
013100                        VALUE +07654321.                          NC1164.2
013200 01  AN-00009-X-6 REDEFINES DS-LS-00008-1   PIC X(9).             NC1164.2
013300 01  DS-TS-00008        PIC S9(8)                                 NC1164.2
013400                        SIGN TRAILING SEPARATE                    NC1164.2
013500                        VALUE +07654321.                          NC1164.2
013600 01  AN-00009-X-4 REDEFINES DS-TS-00008     PIC X(9).             NC1164.2
013700 01  CS-00007-1         PIC S9(7) COMPUTATIONAL VALUE +1234567.   NC1164.2
013800 01  CU-00007-1         PIC  9(7) COMPUTATIONAL VALUE  1234567.   NC1164.2
013900 01  DS-00007-1         PIC S9(7) DISPLAY       VALUE +1234567.   NC1164.2
014000 01  DU-00007-1         PIC  9(7) DISPLAY       VALUE  1234567.   NC1164.2
014100 01  CS-00007-2         PIC S9(7) COMPUTATIONAL VALUE -1234567.   NC1164.2
014200 01  CU-00007-2        PIC  9(7) COMPUTATIONAL VALUE  1234567.    NC1164.2
014300 01  DS-00007-2         PIC S9(7) DISPLAY       VALUE -1234567.   NC1164.2
014400 01  DU-00007-2         PIC  9(7) DISPLAY       VALUE  1234567.   NC1164.2
014500 01  CS-00007-3         PIC S9(7) COMPUTATIONAL VALUE -7654321.   NC1164.2
014600 01  CU-00007-3         PIC  9(7) COMPUTATIONAL VALUE  7654321.   NC1164.2
014700 01  DS-00007-3         PIC S9(7) DISPLAY       VALUE -7654321.   NC1164.2
014800 01  DU-00007-3         PIC  9(7) DISPLAY       VALUE  7654321.   NC1164.2
014900 01  CS-00007-4         PIC S9(7) COMPUTATIONAL VALUE +7654321.   NC1164.2
015000 01  CU-00007-4         PIC  9(7) COMPUTATIONAL VALUE  7654321.   NC1164.2
015100 01  DS-00007-4         PIC S9(7) DISPLAY       VALUE +7654321.   NC1164.2
015200 01  DU-00007-4         PIC  9(7) DISPLAY       VALUE  7654321.   NC1164.2
015300 01  TEST-RESULTS.                                                NC1164.2
015400     02 FILLER                   PIC X      VALUE SPACE.          NC1164.2
015500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1164.2
015600     02 FILLER                   PIC X      VALUE SPACE.          NC1164.2
015700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1164.2
015800     02 FILLER                   PIC X      VALUE SPACE.          NC1164.2
015900     02  PAR-NAME.                                                NC1164.2
016000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1164.2
016100       03  PARDOT-X              PIC X      VALUE SPACE.          NC1164.2
016200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1164.2
016300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1164.2
016400     02 RE-MARK                  PIC X(61).                       NC1164.2
016500 01  TEST-COMPUTED.                                               NC1164.2
016600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1164.2
016700     02 FILLER                   PIC X(17)  VALUE                 NC1164.2
016800            "       COMPUTED=".                                   NC1164.2
016900     02 COMPUTED-X.                                               NC1164.2
017000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1164.2
017100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1164.2
017200                                 PIC -9(9).9(9).                  NC1164.2
017300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1164.2
017400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1164.2
017500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1164.2
017600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1164.2
017700         04 COMPUTED-18V0                    PIC -9(18).          NC1164.2
017800         04 FILLER                           PIC X.               NC1164.2
017900     03 FILLER PIC X(50) VALUE SPACE.                             NC1164.2
018000 01  TEST-CORRECT.                                                NC1164.2
018100     02 FILLER PIC X(30) VALUE SPACE.                             NC1164.2
018200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1164.2
018300     02 CORRECT-X.                                                NC1164.2
018400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1164.2
018500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1164.2
018600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1164.2
018700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1164.2
018800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1164.2
018900     03      CR-18V0 REDEFINES CORRECT-A.                         NC1164.2
019000         04 CORRECT-18V0                     PIC -9(18).          NC1164.2
019100         04 FILLER                           PIC X.               NC1164.2
019200     03 FILLER PIC X(2) VALUE SPACE.                              NC1164.2
019300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1164.2
019400 01  CCVS-C-1.                                                    NC1164.2
019500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1164.2
019600-    "SS  PARAGRAPH-NAME                                          NC1164.2
019700-    "       REMARKS".                                            NC1164.2
019800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1164.2
019900 01  CCVS-C-2.                                                    NC1164.2
020000     02 FILLER                     PIC X        VALUE SPACE.      NC1164.2
020100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1164.2
020200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1164.2
020300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1164.2
020400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1164.2
020500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1164.2
020600 01  REC-CT                        PIC 99       VALUE ZERO.       NC1164.2
020700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1164.2
020800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1164.2
020900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1164.2
021000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1164.2
021100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1164.2
021200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1164.2
021300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1164.2
021400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1164.2
021500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1164.2
021600 01  CCVS-H-1.                                                    NC1164.2
021700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1164.2
021800     02  FILLER                    PIC X(42)    VALUE             NC1164.2
021900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1164.2
022000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1164.2
022100 01  CCVS-H-2A.                                                   NC1164.2
022200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1164.2
022300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1164.2
022400   02  FILLER                        PIC XXXX   VALUE             NC1164.2
022500     "4.2 ".                                                      NC1164.2
022600   02  FILLER                        PIC X(28)  VALUE             NC1164.2
022700            " COPY - NOT FOR DISTRIBUTION".                       NC1164.2
022800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1164.2
022900                                                                  NC1164.2
023000 01  CCVS-H-2B.                                                   NC1164.2
023100   02  FILLER                        PIC X(15)  VALUE             NC1164.2
023200            "TEST RESULT OF ".                                    NC1164.2
023300   02  TEST-ID                       PIC X(9).                    NC1164.2
023400   02  FILLER                        PIC X(4)   VALUE             NC1164.2
023500            " IN ".                                               NC1164.2
023600   02  FILLER                        PIC X(12)  VALUE             NC1164.2
023700     " HIGH       ".                                              NC1164.2
023800   02  FILLER                        PIC X(22)  VALUE             NC1164.2
023900            " LEVEL VALIDATION FOR ".                             NC1164.2
024000   02  FILLER                        PIC X(58)  VALUE             NC1164.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1164.2
024200 01  CCVS-H-3.                                                    NC1164.2
024300     02  FILLER                      PIC X(34)  VALUE             NC1164.2
024400            " FOR OFFICIAL USE ONLY    ".                         NC1164.2
024500     02  FILLER                      PIC X(58)  VALUE             NC1164.2
024600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1164.2
024700     02  FILLER                      PIC X(28)  VALUE             NC1164.2
024800            "  COPYRIGHT   1985 ".                                NC1164.2
024900 01  CCVS-E-1.                                                    NC1164.2
025000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1164.2
025100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1164.2
025200     02 ID-AGAIN                     PIC X(9).                    NC1164.2
025300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1164.2
025400 01  CCVS-E-2.                                                    NC1164.2
025500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1164.2
025600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1164.2
025700     02 CCVS-E-2-2.                                               NC1164.2
025800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1164.2
025900         03 FILLER                   PIC X      VALUE SPACE.      NC1164.2
026000         03 ENDER-DESC               PIC X(44)  VALUE             NC1164.2
026100            "ERRORS ENCOUNTERED".                                 NC1164.2
026200 01  CCVS-E-3.                                                    NC1164.2
026300     02  FILLER                      PIC X(22)  VALUE             NC1164.2
026400            " FOR OFFICIAL USE ONLY".                             NC1164.2
026500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1164.2
026600     02  FILLER                      PIC X(58)  VALUE             NC1164.2
026700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1164.2
026800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1164.2
026900     02 FILLER                       PIC X(15)  VALUE             NC1164.2
027000             " COPYRIGHT 1985".                                   NC1164.2
027100 01  CCVS-E-4.                                                    NC1164.2
027200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1164.2
027300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1164.2
027400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1164.2
027500     02 FILLER                       PIC X(40)  VALUE             NC1164.2
027600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1164.2
027700 01  XXINFO.                                                      NC1164.2
027800     02 FILLER                       PIC X(19)  VALUE             NC1164.2
027900            "*** INFORMATION ***".                                NC1164.2
028000     02 INFO-TEXT.                                                NC1164.2
028100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1164.2
028200       04 XXCOMPUTED                 PIC X(20).                   NC1164.2
028300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1164.2
028400       04 XXCORRECT                  PIC X(20).                   NC1164.2
028500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1164.2
028600 01  HYPHEN-LINE.                                                 NC1164.2
028700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1164.2
028800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1164.2
028900-    "*****************************************".                 NC1164.2
029000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1164.2
029100-    "******************************".                            NC1164.2
029200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1164.2
029300     "NC116A".                                                    NC1164.2
029400 PROCEDURE DIVISION.                                              NC1164.2
029500 CCVS1 SECTION.                                                   NC1164.2
029600 OPEN-FILES.                                                      NC1164.2
029700     OPEN     OUTPUT PRINT-FILE.                                  NC1164.2
029800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1164.2
029900     MOVE    SPACE TO TEST-RESULTS.                               NC1164.2
030000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1164.2
030100     GO TO CCVS1-EXIT.                                            NC1164.2
030200 CLOSE-FILES.                                                     NC1164.2
030300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1164.2
030400 TERMINATE-CCVS.                                                  NC1164.2
030500*    EXIT PROGRAM.                                                NC1164.2
030600*TERMINATE-CALL.                                                  NC1164.2
030700     STOP     RUN.                                                NC1164.2
030800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1164.2
030900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1164.2
031000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1164.2
031100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1164.2
031200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1164.2
031300 PRINT-DETAIL.                                                    NC1164.2
031400     IF REC-CT NOT EQUAL TO ZERO                                  NC1164.2
031500             MOVE "." TO PARDOT-X                                 NC1164.2
031600             MOVE REC-CT TO DOTVALUE.                             NC1164.2
031700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1164.2
031800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1164.2
031900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1164.2
032000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1164.2
032100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1164.2
032200     MOVE SPACE TO CORRECT-X.                                     NC1164.2
032300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1164.2
032400     MOVE     SPACE TO RE-MARK.                                   NC1164.2
032500 HEAD-ROUTINE.                                                    NC1164.2
032600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1164.2
032700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1164.2
032800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1164.2
032900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1164.2
033000 COLUMN-NAMES-ROUTINE.                                            NC1164.2
033100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1164.2
033200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1164.2
033300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1164.2
033400 END-ROUTINE.                                                     NC1164.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1164.2
033600 END-RTN-EXIT.                                                    NC1164.2
033700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1164.2
033800 END-ROUTINE-1.                                                   NC1164.2
033900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1164.2
034000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1164.2
034100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1164.2
034200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1164.2
034300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1164.2
034400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1164.2
034500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1164.2
034600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1164.2
034700  END-ROUTINE-12.                                                 NC1164.2
034800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1164.2
034900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1164.2
035000         MOVE "NO " TO ERROR-TOTAL                                NC1164.2
035100         ELSE                                                     NC1164.2
035200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1164.2
035300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1164.2
035400     PERFORM WRITE-LINE.                                          NC1164.2
035500 END-ROUTINE-13.                                                  NC1164.2
035600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1164.2
035700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1164.2
035800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1164.2
035900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1164.2
036000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1164.2
036100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1164.2
036200          MOVE "NO " TO ERROR-TOTAL                               NC1164.2
036300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1164.2
036400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1164.2
036500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1164.2
036600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1164.2
036700 WRITE-LINE.                                                      NC1164.2
036800     ADD 1 TO RECORD-COUNT.                                       NC1164.2
036900*    IF RECORD-COUNT GREATER 42                                   NC1164.2
037000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1164.2
037100*        MOVE SPACE TO DUMMY-RECORD                               NC1164.2
037200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1164.2
037300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1164.2
037400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1164.2
037500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1164.2
037600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1164.2
037700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1164.2
037800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1164.2
037900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1164.2
038000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1164.2
038100*        MOVE ZERO TO RECORD-COUNT.                               NC1164.2
038200     PERFORM WRT-LN.                                              NC1164.2
038300 WRT-LN.                                                          NC1164.2
038400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1164.2
038500     MOVE SPACE TO DUMMY-RECORD.                                  NC1164.2
038600 BLANK-LINE-PRINT.                                                NC1164.2
038700     PERFORM WRT-LN.                                              NC1164.2
038800 FAIL-ROUTINE.                                                    NC1164.2
038900     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1164.2
039000            GO TO FAIL-ROUTINE-WRITE.                             NC1164.2
039100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1164.2
039200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1164.2
039300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1164.2
039400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1164.2
039500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1164.2
039600     GO TO  FAIL-ROUTINE-EX.                                      NC1164.2
039700 FAIL-ROUTINE-WRITE.                                              NC1164.2
039800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1164.2
039900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1164.2
040000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1164.2
040100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1164.2
040200 FAIL-ROUTINE-EX. EXIT.                                           NC1164.2
040300 BAIL-OUT.                                                        NC1164.2
040400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1164.2
040500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1164.2
040600 BAIL-OUT-WRITE.                                                  NC1164.2
040700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1164.2
040800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1164.2
040900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1164.2
041000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1164.2
041100 BAIL-OUT-EX. EXIT.                                               NC1164.2
041200 CCVS1-EXIT.                                                      NC1164.2
041300     EXIT.                                                        NC1164.2
041400 SECT-NC116A-001 SECTION.                                         NC1164.2
041500 SIG-INIT-GF-1.                                                   NC1164.2
041600     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
041700     MOVE "SIG-TEST-GF-1" TO PAR-NAME.                            NC1164.2
041800     MOVE 1 TO REC-CT.                                            NC1164.2
041900     MOVE "SIGN LEADING SEPARATE" TO FEATURE.                     NC1164.2
042000     MOVE "LEADING SIGN EQUAL PLUS" TO RE-MARK.                   NC1164.2
042100 SIG-TEST-GF-1-1.                                                 NC1164.2
042200*        THIS TEST CHECKS THE SIGN AND THE NUMERIC CHARACTERS     NC1164.2
042300*    OF AN ITEM DEFINED AS SIGN IS LEADING SEPARATE CHARACTER.    NC1164.2
042400     IF TEST1-AN-1 EQUAL TO "+"                                   NC1164.2
042500         PERFORM PASS                                             NC1164.2
042600         GO TO SIG-WRTE-GF-1-1.                                   NC1164.2
042700     GO TO SIG-FAIL-GF-1-1.                                       NC1164.2
042800 SIG-DELETE-GF-1-1.                                               NC1164.2
042900     PERFORM DE-LETE.                                             NC1164.2
043000     PERFORM PRINT-DETAIL.                                        NC1164.2
043100     GO TO   SIG-INIT-GF-2.                                       NC1164.2
043200 SIG-FAIL-GF-1-1.                                                 NC1164.2
043300     PERFORM FAIL.                                                NC1164.2
043400     MOVE "+" TO CORRECT-A.                                       NC1164.2
043500     MOVE TEST1-AN-1 TO COMPUTED-A.                               NC1164.2
043600 SIG-WRTE-GF-1-1.                                                 NC1164.2
043700     PERFORM PRINT-DETAIL.                                        NC1164.2
043800 SIG-TEST-GF-1-2.                                                 NC1164.2
043900     IF TEST1-AN-5 EQUAL TO "91275"                               NC1164.2
044000         PERFORM PASS                                             NC1164.2
044100         GO TO SIG-WRTE-GF-1-2.                                   NC1164.2
044200 SIG-FAIL-GF-1-2.                                                 NC1164.2
044300     PERFORM FAIL.                                                NC1164.2
044400     MOVE "NUMERIC CHARACTERS" TO RE-MARK.                        NC1164.2
044500     MOVE "91275" TO CORRECT-A.                                   NC1164.2
044600     MOVE TEST1-AN-5 TO COMPUTED-A.                               NC1164.2
044700 SIG-WRTE-GF-1-2.                                                 NC1164.2
044800     MOVE 2 TO REC-CT.                                            NC1164.2
044900     PERFORM PRINT-DETAIL.                                        NC1164.2
045000 SIG-TEST-GF-1-3.                                                 NC1164.2
045100     IF TEST1N-AN-1 NOT EQUAL TO  "-"                             NC1164.2
045200         GO TO SIG-FAIL-GF-1-3.                                   NC1164.2
045300     PERFORM PASS.                                                NC1164.2
045400     GO TO SIG-WRTE-GF-1-3.                                       NC1164.2
045500 SIG-FAIL-GF-1-3.                                                 NC1164.2
045600     PERFORM FAIL.                                                NC1164.2
045700     MOVE "-" TO CORRECT-A.                                       NC1164.2
045800     MOVE TEST1N-AN-1 TO COMPUTED-A.                              NC1164.2
045900 SIG-WRTE-GF-1-3.                                                 NC1164.2
046000     MOVE 3 TO REC-CT.                                            NC1164.2
046100     MOVE "LEADING SIGN EQUAL MINUS" TO RE-MARK.                  NC1164.2
046200     PERFORM PRINT-DETAIL.                                        NC1164.2
046300 SIG-TEST-GF-1-4.                                                 NC1164.2
046400     IF TEST1N-AN-4 NOT EQUAL TO "9127"                           NC1164.2
046500         GO TO SIG-FAIL-GF-1-4.                                   NC1164.2
046600     PERFORM PASS.                                                NC1164.2
046700     GO TO SIG-WRTE-GF-1-4.                                       NC1164.2
046800 SIG-FAIL-GF-1-4.                                                 NC1164.2
046900     PERFORM FAIL.                                                NC1164.2
047000     MOVE "NUMERIC CHARACTERS" TO RE-MARK.                        NC1164.2
047100     MOVE "9127" TO CORRECT-A.                                    NC1164.2
047200     MOVE TEST1N-AN-4 TO COMPUTED-A.                              NC1164.2
047300 SIG-WRTE-GF-1-4.                                                 NC1164.2
047400     MOVE 4 TO REC-CT.                                            NC1164.2
047500     PERFORM PRINT-DETAIL.                                        NC1164.2
047600 SIG-INIT-GF-2.                                                   NC1164.2
047700     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
047800     MOVE "SIG-TEST-GF-2" TO PAR-NAME.                            NC1164.2
047900     MOVE 1 TO REC-CT.                                            NC1164.2
048000     MOVE "SIGN TRLNG SEPARATE" TO FEATURE.                       NC1164.2
048100     MOVE "TRAILING SIGN EQUAL PLUS" TO RE-MARK.                  NC1164.2
048200*        THIS TEST CHECKS THE SIGN AND THE NUMERIC CHARACTERS     NC1164.2
048300*    OF AN ITEM DEFINED AS SIGN IS TRAILING SEPARATE CHARACTER.   NC1164.2
048400 SIG-TEST-GF-2-1.                                                 NC1164.2
048500     IF TEST2-AN-1 EQUAL TO "+"                                   NC1164.2
048600         PERFORM PASS                                             NC1164.2
048700         GO TO SIG-WRTE-GF-2-1.                                   NC1164.2
048800     GO TO SIG-FAIL-GF-2-1.                                       NC1164.2
048900 SIG-DELETE-GF-2-1.                                               NC1164.2
049000     PERFORM DE-LETE.                                             NC1164.2
049100     PERFORM PRINT-DETAIL.                                        NC1164.2
049200     GO TO   SIG-INIT-GF-3.                                       NC1164.2
049300 SIG-FAIL-GF-2-1.                                                 NC1164.2
049400     PERFORM FAIL.                                                NC1164.2
049500     MOVE "+" TO CORRECT-A.                                       NC1164.2
049600     MOVE TEST2-AN-1 TO COMPUTED-A.                               NC1164.2
049700 SIG-WRTE-GF-2-1.                                                 NC1164.2
049800     PERFORM PRINT-DETAIL.                                        NC1164.2
049900 SIGNTEST-GF-2-2.                                                 NC1164.2
050000     IF TEST2-AN-5 EQUAL TO "80361"                               NC1164.2
050100         PERFORM PASS                                             NC1164.2
050200         GO TO SIG-WRTE-GF-2-2.                                   NC1164.2
050300     GO TO SIG-FAIL-GF-2-2.                                       NC1164.2
050400 SIG-FAIL-GF-2-2.                                                 NC1164.2
050500     PERFORM FAIL.                                                NC1164.2
050600     MOVE "80361" TO CORRECT-A.                                   NC1164.2
050700     MOVE TEST2-AN-5 TO COMPUTED-A.                               NC1164.2
050800     MOVE "NUMERIC CHARACTERS" TO RE-MARK.                        NC1164.2
050900 SIG-WRTE-GF-2-2.                                                 NC1164.2
051000     MOVE 2 TO REC-CT.                                            NC1164.2
051100     PERFORM PRINT-DETAIL.                                        NC1164.2
051200 SIG-TEST-GF-2-3.                                                 NC1164.2
051300     IF TEST2N-AN-1 NOT EQUAL TO "-"                              NC1164.2
051400         GO TO SIG-FAIL-GF-2-3.                                   NC1164.2
051500     PERFORM PASS.                                                NC1164.2
051600     GO TO SIG-WRTE-GF-2-3.                                       NC1164.2
051700 SIG-FAIL-GF-2-3.                                                 NC1164.2
051800     PERFORM FAIL.                                                NC1164.2
051900     MOVE "-" TO CORRECT-A.                                       NC1164.2
052000     MOVE TEST2N-AN-1 TO COMPUTED-A.                              NC1164.2
052100 SIG-WRTE-GF-2-3.                                                 NC1164.2
052200     MOVE 3 TO REC-CT.                                            NC1164.2
052300     MOVE "TRAILING SIGN EQUAL MINUS" TO RE-MARK.                 NC1164.2
052400     PERFORM PRINT-DETAIL.                                        NC1164.2
052500 SIG-TEST-GF-2-4.                                                 NC1164.2
052600     IF TEST2N-AN-4 NOT EQUAL TO "8036"                           NC1164.2
052700         GO TO SIG-FAIL-GF-2-4.                                   NC1164.2
052800     PERFORM PASS.                                                NC1164.2
052900     GO TO SIG-WRTE-GF-2-4.                                       NC1164.2
053000 SIG-FAIL-GF-2-4.                                                 NC1164.2
053100     PERFORM FAIL.                                                NC1164.2
053200     MOVE "8036" TO CORRECT-A.                                    NC1164.2
053300     MOVE TEST2N-AN-4 TO COMPUTED-A.                              NC1164.2
053400     MOVE "NUMERIC CHARACTERS" TO RE-MARK.                        NC1164.2
053500 SIG-WRTE-GF-2-4.                                                 NC1164.2
053600     MOVE 4 TO REC-CT.                                            NC1164.2
053700     PERFORM PRINT-DETAIL.                                        NC1164.2
053800 SIG-INIT-GF-3.                                                   NC1164.2
053900     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
054000     MOVE "SIG-TEST-GF-3" TO PAR-NAME.                            NC1164.2
054100     MOVE "SIGN LEADING" TO FEATURE.                              NC1164.2
054200     MOVE 1 TO REC-CT.                                            NC1164.2
054300     MOVE "POSITIVE NUMERIC ITEM" TO RE-MARK.                     NC1164.2
054400*        THIS TEST CHECKS ALL BUT THE LEADING CHARACTER OF AN     NC1164.2
054500*    ITEM DEFINED AS SIGN IS LEADING. (NOT SEPARATE CHAR.)        NC1164.2
054600 SIG-TEST-GF-3-1.                                                 NC1164.2
054700     IF TEST3-AN-4 EQUAL TO "1275"                                NC1164.2
054800         PERFORM PASS                                             NC1164.2
054900         GO TO SIG-WRTE-GF-3-1.                                   NC1164.2
055000     GO TO SIG-FAIL-GF-3-1.                                       NC1164.2
055100 SIG-DELETE-GF-3-1.                                               NC1164.2
055200     PERFORM DE-LETE.                                             NC1164.2
055300     PERFORM PRINT-DETAIL.                                        NC1164.2
055400     GO TO   SIG-INIT-GF-4.                                       NC1164.2
055500 SIG-FAIL-GF-3-1.                                                 NC1164.2
055600     PERFORM FAIL.                                                NC1164.2
055700     MOVE "1275" TO CORRECT-A.                                    NC1164.2
055800     MOVE TEST3-AN-4 TO COMPUTED-A.                               NC1164.2
055900 SIG-WRTE-GF-3-1.                                                 NC1164.2
056000     PERFORM PRINT-DETAIL.                                        NC1164.2
056100 SIG-TEST-GF-3-2.                                                 NC1164.2
056200     IF TEST3N-AN-3 NOT EQUAL TO "127"                            NC1164.2
056300         GO TO SIG-FAIL-GF-3-2.                                   NC1164.2
056400     PERFORM PASS.                                                NC1164.2
056500     GO TO SIG-WRTE-GF-3-2.                                       NC1164.2
056600 SIG-DELETE-GF-3-2.                                               NC1164.2
056700     PERFORM DE-LETE.                                             NC1164.2
056800 SIG-FAIL-GF-3-2.                                                 NC1164.2
056900     PERFORM FAIL.                                                NC1164.2
057000     MOVE "127" TO CORRECT-A.                                     NC1164.2
057100     MOVE TEST3N-AN-3 TO COMPUTED-A.                              NC1164.2
057200 SIG-WRTE-GF-3-2.                                                 NC1164.2
057300     MOVE 2 TO REC-CT.                                            NC1164.2
057400     MOVE "NEGATIVE NUMERIC ITEM" TO RE-MARK.                     NC1164.2
057500     PERFORM PRINT-DETAIL.                                        NC1164.2
057600 SIG-INIT-GF-4.                                                   NC1164.2
057700     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
057800     MOVE "SIG-TEST-GF-4" TO PAR-NAME.                            NC1164.2
057900     MOVE "SIGN TRAILING" TO FEATURE.                             NC1164.2
058000     MOVE 1 TO REC-CT.                                            NC1164.2
058100     MOVE "POSITIVE NUMERIC ITEM" TO RE-MARK.                     NC1164.2
058200*        THIS TEST CHECKS ALL BUT THE TRAILING CHARACTER OF AN    NC1164.2
058300*    ITEM DEFINED AS SIGN IS TRAILING. (NOT SEPARATE CHAR.)       NC1164.2
058400 SIG-TEST-GF-4-1.                                                 NC1164.2
058500     IF TEST4-AN-4 EQUAL TO "8362"                                NC1164.2
058600         PERFORM PASS                                             NC1164.2
058700         GO TO SIG-WRTE-GF-4-1.                                   NC1164.2
058800     GO TO SIG-FAIL-GF-4-1.                                       NC1164.2
058900 SIG-DELETE-GF-4-1.                                               NC1164.2
059000     PERFORM DE-LETE.                                             NC1164.2
059100     PERFORM PRINT-DETAIL.                                        NC1164.2
059200     GO TO   SIG-INIT-GF-5.                                       NC1164.2
059300 SIG-FAIL-GF-4-1.                                                 NC1164.2
059400     PERFORM FAIL.                                                NC1164.2
059500     MOVE "8362" TO CORRECT-A.                                    NC1164.2
059600     MOVE TEST4-AN-4 TO COMPUTED-A.                               NC1164.2
059700 SIG-WRTE-GF-4-1.                                                 NC1164.2
059800     PERFORM PRINT-DETAIL.                                        NC1164.2
059900 SIG-TEST-GF-4-2.                                                 NC1164.2
060000     IF TEST4N-AN-3 NOT EQUAL TO "362"                            NC1164.2
060100         GO TO SIG-FAIL-GF-4-2.                                   NC1164.2
060200     PERFORM PASS.                                                NC1164.2
060300     GO TO SIG-WRTE-GF-4-2.                                       NC1164.2
060400 SIG-DELETE-GF-4-2.                                               NC1164.2
060500     PERFORM DE-LETE.                                             NC1164.2
060600 SIG-FAIL-GF-4-2.                                                 NC1164.2
060700     PERFORM FAIL.                                                NC1164.2
060800     MOVE "362" TO CORRECT-A.                                     NC1164.2
060900     MOVE TEST4N-AN-3 TO COMPUTED-A.                              NC1164.2
061000 SIG-WRTE-GF-4-2.                                                 NC1164.2
061100     MOVE 2 TO REC-CT.                                            NC1164.2
061200     MOVE "NEGATIVE NUMERIC ITEM" TO RE-MARK.                     NC1164.2
061300     PERFORM PRINT-DETAIL.                                        NC1164.2
061400 SIG-INIT-GF-5.                                                   NC1164.2
061500     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
061600     MOVE "SIG-TEST-GF-5" TO PAR-NAME.                            NC1164.2
061700     MOVE "COMPARE SIGNED ITEMS" TO FEATURE.                      NC1164.2
061800     MOVE "LEADING SIGN" TO RE-MARK.                              NC1164.2
061900     MOVE 1 TO REC-CT.                                            NC1164.2
062000*        THIS SERIES OF TESTS COMPARE A SIGNED DISPLAY ITEM WITH  NC1164.2
062100*    LEADING SIGN TO FOUR SIGNED AND UNSIGNED COMPUTATIONAL AND   NC1164.2
062200*    DISPLAY ITEMS.                                               NC1164.2
062300 SIG-TEST-GF-5-1.                                                 NC1164.2
062400     IF DS-L-00008 EQUAL TO CS-00007-1                            NC1164.2
062500              PERFORM PASS                                        NC1164.2
062600              GO TO SIG-WRITE-GF-5-1.                             NC1164.2
062700     GO TO SIG-FAIL-GF-5-1.                                       NC1164.2
062800 SIG-DELETE-GF-5-1.                                               NC1164.2
062900     PERFORM DE-LETE.                                             NC1164.2
063000     PERFORM PRINT-DETAIL.                                        NC1164.2
063100     GO TO   SIG-INIT-GF-6.                                       NC1164.2
063200 SIG-FAIL-GF-5-1.                                                 NC1164.2
063300     PERFORM FAIL.                                                NC1164.2
063400     MOVE AN-00008-X-1 TO COMPUTED-A.                             NC1164.2
063500     MOVE CS-00007-1 TO CORRECT-18V0.                             NC1164.2
063600 SIG-WRITE-GF-5-1.                                                NC1164.2
063700     PERFORM PRINT-DETAIL.                                        NC1164.2
063800 SIG-TEST-GF-5-2.                                                 NC1164.2
063900     MOVE 2 TO REC-CT.                                            NC1164.2
064000     IF DS-L-00008 EQUAL TO CU-00007-1                            NC1164.2
064100              PERFORM PASS                                        NC1164.2
064200              GO TO SIG-WRITE-GF-5-2.                             NC1164.2
064300 SIG-FAIL-GF-5-2.                                                 NC1164.2
064400     PERFORM FAIL.                                                NC1164.2
064500     MOVE AN-00008-X-1 TO COMPUTED-A.                             NC1164.2
064600     MOVE CU-00007-1 TO CORRECT-18V0.                             NC1164.2
064700 SIG-WRITE-GF-5-2.                                                NC1164.2
064800     PERFORM PRINT-DETAIL.                                        NC1164.2
064900 SIG-TEST-GF-5-3.                                                 NC1164.2
065000     MOVE 3 TO REC-CT.                                            NC1164.2
065100     IF DS-L-00008 EQUAL TO DS-00007-1                            NC1164.2
065200              PERFORM PASS                                        NC1164.2
065300              GO TO SIG-WRITE-GF-5-3.                             NC1164.2
065400 SIG-FAIL-GF-5-3.                                                 NC1164.2
065500     PERFORM FAIL.                                                NC1164.2
065600     MOVE AN-00008-X-1 TO COMPUTED-A.                             NC1164.2
065700     MOVE DS-00007-1 TO CORRECT-18V0.                             NC1164.2
065800 SIG-WRITE-GF-5-3.                                                NC1164.2
065900     PERFORM PRINT-DETAIL.                                        NC1164.2
066000 SIG-TEST-GF-5-4.                                                 NC1164.2
066100     MOVE 4 TO REC-CT.                                            NC1164.2
066200     IF DS-L-00008 EQUAL TO DU-00007-1                            NC1164.2
066300              PERFORM PASS                                        NC1164.2
066400              GO TO SIG-WRITE-GF-5-4.                             NC1164.2
066500 SIG-FAIL-GF-5-4.                                                 NC1164.2
066600     PERFORM FAIL.                                                NC1164.2
066700     MOVE AN-00008-X-1 TO COMPUTED-A.                             NC1164.2
066800     MOVE DU-00007-1 TO CORRECT-18V0.                             NC1164.2
066900 SIG-WRITE-GF-5-4.                                                NC1164.2
067000     PERFORM PRINT-DETAIL.                                        NC1164.2
067100 SIG-INIT-GF-6.                                                   NC1164.2
067200     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
067300     MOVE "SIG-TEST-GF-6" TO PAR-NAME.                            NC1164.2
067400     MOVE "COMPARE SIGNED ITEMS" TO FEATURE.                      NC1164.2
067500     MOVE "TRAILING SIGN" TO RE-MARK.                             NC1164.2
067600*        THIS SERIES OF TESTS COMPARE A SIGNED DISPLAY ITEM WITH  NC1164.2
067700*    TRAILING SIGN TO FOUR SIGNED AND UNSIGNED COMPUTATIONAL AND  NC1164.2
067800*    DISPLAY ITEMS.                                               NC1164.2
067900 SIG-TEST-GF-6-1.                                                 NC1164.2
068000     MOVE 1 TO REC-CT.                                            NC1164.2
068100     IF DS-T-00008 EQUAL TO CS-00007-2                            NC1164.2
068200              PERFORM PASS                                        NC1164.2
068300              GO TO SIG-WRITE-GF-6-1.                             NC1164.2
068400     GO TO SIG-FAIL-GF-6-1.                                       NC1164.2
068500 SIG-DELETE-GF-6-1.                                               NC1164.2
068600     PERFORM DE-LETE.                                             NC1164.2
068700     PERFORM PRINT-DETAIL.                                        NC1164.2
068800     GO TO   SIG-INIT-GF-7.                                       NC1164.2
068900 SIG-FAIL-GF-6-1.                                                 NC1164.2
069000     PERFORM FAIL.                                                NC1164.2
069100     MOVE AN-00008-X-2 TO COMPUTED-A.                             NC1164.2
069200     MOVE CS-00007-2 TO CORRECT-18V0.                             NC1164.2
069300 SIG-WRITE-GF-6-1.                                                NC1164.2
069400     PERFORM PRINT-DETAIL.                                        NC1164.2
069500 SIG-TEST-GF-6-2.                                                 NC1164.2
069600     MOVE 2 TO REC-CT.                                            NC1164.2
069700     IF DS-T-00008-1 EQUAL TO CU-00007-2                          NC1164.2
069800              PERFORM PASS                                        NC1164.2
069900              GO TO SIG-WRITE-GF-6-2.                             NC1164.2
070000 SIG-FAIL-GF-6-2.                                                 NC1164.2
070100     PERFORM FAIL.                                                NC1164.2
070200     MOVE AN-00008-X-5 TO COMPUTED-A.                             NC1164.2
070300     MOVE CU-00007-2 TO CORRECT-18V0.                             NC1164.2
070400 SIG-WRITE-GF-6-2.                                                NC1164.2
070500     PERFORM PRINT-DETAIL.                                        NC1164.2
070600 SIG-TEST-GF-6-3.                                                 NC1164.2
070700     MOVE 3 TO REC-CT.                                            NC1164.2
070800     IF DS-T-00008 EQUAL TO DS-00007-2                            NC1164.2
070900              PERFORM PASS                                        NC1164.2
071000              GO TO SIG-WRITE-GF-6-3.                             NC1164.2
071100 SIG-FAIL-GF-6-3.                                                 NC1164.2
071200     PERFORM FAIL.                                                NC1164.2
071300     MOVE AN-00008-X-2 TO COMPUTED-A.                             NC1164.2
071400     MOVE DS-00007-2 TO CORRECT-18V0.                             NC1164.2
071500 SIG-WRITE-GF-6-3.                                                NC1164.2
071600     PERFORM PRINT-DETAIL.                                        NC1164.2
071700 SIG-TEST-GF-6-4.                                                 NC1164.2
071800     MOVE 4 TO REC-CT.                                            NC1164.2
071900     IF DS-T-00008-1 EQUAL TO DU-00007-2                          NC1164.2
072000              PERFORM PASS                                        NC1164.2
072100              GO TO SIG-WRITE-GF-6-4.                             NC1164.2
072200 SIG-FAIL-GF-6-4.                                                 NC1164.2
072300     PERFORM FAIL.                                                NC1164.2
072400     MOVE AN-00008-X-5 TO COMPUTED-A.                             NC1164.2
072500     MOVE DU-00007-2  TO CORRECT-18V0.                            NC1164.2
072600 SIG-WRITE-GF-6-4.                                                NC1164.2
072700     PERFORM PRINT-DETAIL.                                        NC1164.2
072800 SIG-INIT-GF-7.                                                   NC1164.2
072900     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
073000     MOVE "SIG-TEST-GF-7" TO PAR-NAME.                            NC1164.2
073100     MOVE "COMPARE SIGNED ITEMS" TO FEATURE.                      NC1164.2
073200     MOVE "LEADING SIGN SEPARATE CHARACTER" TO RE-MARK.           NC1164.2
073300*        THIS SERIES OF TESTS COMPARE A SIGNED DISPLAY ITEM WITH  NC1164.2
073400*    LEADING SIGN AND SEPARATE CHARACTER TO FOUR SIGNED AND       NC1164.2
073500*    UNSIGNED COMPUTATIONAL AND DISPLAY ITEMS.                    NC1164.2
073600 SIG-TEST-GF-7-1.                                                 NC1164.2
073700     MOVE 1 TO REC-CT.                                            NC1164.2
073800     IF DS-LS-00008 EQUAL TO CS-00007-3                           NC1164.2
073900              PERFORM PASS                                        NC1164.2
074000         GO TO SIG-WRITE-GF-7-1.                                  NC1164.2
074100     GO TO SIG-FAIL-GF-7-1.                                       NC1164.2
074200 SIG-DELETE-GF-7-1.                                               NC1164.2
074300     PERFORM DE-LETE.                                             NC1164.2
074400     PERFORM PRINT-DETAIL.                                        NC1164.2
074500     GO TO   SIG-INIT-GF-8.                                       NC1164.2
074600 SIG-FAIL-GF-7-1.                                                 NC1164.2
074700     PERFORM FAIL.                                                NC1164.2
074800     MOVE AN-00009-X-3 TO COMPUTED-A.                             NC1164.2
074900     MOVE CS-00007-3 TO CORRECT-18V0.                             NC1164.2
075000 SIG-WRITE-GF-7-1.                                                NC1164.2
075100     PERFORM PRINT-DETAIL.                                        NC1164.2
075200 SIG-TEST-GF-7-2.                                                 NC1164.2
075300     MOVE 2 TO REC-CT.                                            NC1164.2
075400     IF DS-LS-00008-1 EQUAL TO CU-00007-3                         NC1164.2
075500              PERFORM PASS                                        NC1164.2
075600              GO TO SIG-WRITE-GF-7-2.                             NC1164.2
075700 SIG-FAIL-GF-7-2.                                                 NC1164.2
075800     PERFORM FAIL.                                                NC1164.2
075900     MOVE AN-00009-X-6 TO COMPUTED-A.                             NC1164.2
076000     MOVE CU-00007-3 TO CORRECT-18V0.                             NC1164.2
076100 SIG-WRITE-GF-7-2.                                                NC1164.2
076200     PERFORM PRINT-DETAIL.                                        NC1164.2
076300 SIG-TEST-GF-7-3.                                                 NC1164.2
076400     MOVE 3 TO REC-CT.                                            NC1164.2
076500     IF DS-LS-00008 EQUAL TO DS-00007-3                           NC1164.2
076600              PERFORM PASS.                                       NC1164.2
076700              GO TO SIG-WRITE-GF-7-3.                             NC1164.2
076800 SIG-FAIL-GF-7-3.                                                 NC1164.2
076900     PERFORM FAIL.                                                NC1164.2
077000     MOVE AN-00009-X-3 TO COMPUTED-A.                             NC1164.2
077100     MOVE DS-00007-3 TO CORRECT-18V0.                             NC1164.2
077200 SIG-WRITE-GF-7-3.                                                NC1164.2
077300     PERFORM PRINT-DETAIL.                                        NC1164.2
077400 SIG-TEST-GF-7-4.                                                 NC1164.2
077500     MOVE 4 TO REC-CT.                                            NC1164.2
077600     IF DS-LS-00008-1 EQUAL TO DU-00007-3                         NC1164.2
077700              PERFORM PASS                                        NC1164.2
077800              GO TO SIG-WRITE-GF-7-4.                             NC1164.2
077900 SIG-FAIL-GF-7-4.                                                 NC1164.2
078000     PERFORM FAIL.                                                NC1164.2
078100     MOVE AN-00009-X-6 TO COMPUTED-A.                             NC1164.2
078200     MOVE DU-00007-3 TO CORRECT-18V0.                             NC1164.2
078300 SIG-WRITE-GF-7-4.                                                NC1164.2
078400     PERFORM PRINT-DETAIL.                                        NC1164.2
078500 SIG-INIT-GF-8.                                                   NC1164.2
078600     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
078700     MOVE "SIG-TEST-GF-8" TO PAR-NAME.                            NC1164.2
078800     MOVE "COMPARE SIGN ITEMS" TO FEATURE.                        NC1164.2
078900     MOVE "TRAILING SIGN SEPARATE CHARACTR" TO RE-MARK.           NC1164.2
079000*        THIS SERIES OF TESTS COMPARE A SIGNED DISPLAY ITEM WITH  NC1164.2
079100*    TRAILING SIGN AND SEPARATE CHARACTER TO FOUR SIGNED AND      NC1164.2
079200*    UNSIGNED COMPUTATIONAL AND DISPLAY ITEMS.                    NC1164.2
079300 SIG-TEST-GF-8-1.                                                 NC1164.2
079400     MOVE 1 TO REC-CT.                                            NC1164.2
079500     IF DS-TS-00008 EQUAL TO CS-00007-4                           NC1164.2
079600              PERFORM PASS                                        NC1164.2
079700              GO TO SIG-WRITE-GF-8-1.                             NC1164.2
079800     GO TO SIG-FAIL-GF-8-1.                                       NC1164.2
079900 SIG-DELETE-GF-8-1.                                               NC1164.2
080000     PERFORM DE-LETE.                                             NC1164.2
080100     PERFORM PRINT-DETAIL.                                        NC1164.2
080200     GO TO   SIG-INIT-GF-9.                                       NC1164.2
080300 SIG-FAIL-GF-8-1.                                                 NC1164.2
080400     PERFORM FAIL.                                                NC1164.2
080500     MOVE AN-00009-X-4 TO COMPUTED-A.                             NC1164.2
080600     MOVE CS-00007-4 TO CORRECT-18V0.                             NC1164.2
080700 SIG-WRITE-GF-8-1.                                                NC1164.2
080800     PERFORM PRINT-DETAIL.                                        NC1164.2
080900 SIG-TEST-GF-8-2.                                                 NC1164.2
081000     MOVE 2 TO REC-CT.                                            NC1164.2
081100     IF DS-TS-00008 EQUAL TO CU-00007-4                           NC1164.2
081200              PERFORM PASS                                        NC1164.2
081300              GO TO SIG-WRITE-GF-8-2.                             NC1164.2
081400 SIG-FAIL-GF-8-2.                                                 NC1164.2
081500     PERFORM FAIL.                                                NC1164.2
081600     MOVE AN-00009-X-4 TO COMPUTED-A.                             NC1164.2
081700     MOVE CU-00007-4 TO CORRECT-18V0.                             NC1164.2
081800 SIG-WRITE-GF-8-2.                                                NC1164.2
081900     PERFORM PRINT-DETAIL.                                        NC1164.2
082000 SIG-TEST-GF-8-3.                                                 NC1164.2
082100     MOVE 3 TO REC-CT.                                            NC1164.2
082200     IF DS-TS-00008 EQUAL TO DS-00007-4                           NC1164.2
082300              PERFORM PASS                                        NC1164.2
082400              GO TO SIG-WRITE-GF-8-3.                             NC1164.2
082500 SIG-FAIL-GF-8-3.                                                 NC1164.2
082600     PERFORM FAIL.                                                NC1164.2
082700     MOVE AN-00009-X-4 TO COMPUTED-A.                             NC1164.2
082800     MOVE DS-00007-4 TO CORRECT-18V0.                             NC1164.2
082900 SIG-WRITE-GF-8-3.                                                NC1164.2
083000     PERFORM PRINT-DETAIL.                                        NC1164.2
083100 SIG-TEST-GF-8-4.                                                 NC1164.2
083200     MOVE 4 TO REC-CT.                                            NC1164.2
083300     IF DS-TS-00008 EQUAL TO DU-00007-4                           NC1164.2
083400              PERFORM PASS                                        NC1164.2
083500              GO TO SIG-WRITE-GF-8-4.                             NC1164.2
083600 SIG-FAIL-GF-8-4.                                                 NC1164.2
083700     PERFORM FAIL.                                                NC1164.2
083800     MOVE AN-00009-X-4 TO COMPUTED-A.                             NC1164.2
083900     MOVE DU-00007-4 TO CORRECT-18V0.                             NC1164.2
084000 SIG-WRITE-GF-8-4.                                                NC1164.2
084100     PERFORM PRINT-DETAIL.                                        NC1164.2
084200 SIG-INIT-GF-9.                                                   NC1164.2
084300     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
084400     MOVE "SIG-TEST-GF-9" TO PAR-NAME.                            NC1164.2
084500     MOVE "SENDING ITEM DS-LS" TO FEATURE.                        NC1164.2
084600     MOVE 1 TO REC-CT.                                            NC1164.2
084700*        THIS TEST MOVES A NUMERIC ITEM WITH SIGN IS LEADING      NC1164.2
084800*    SEPARATE CHARACTER TO UNSIGNED DISPLAY, SIGNED DISPLAY,      NC1164.2
084900*    UNSIGNED COMPUTATIONAL AND SIGNED COMPUTATIONAL ITEMS.       NC1164.2
085000 SIG-TEST-GF-9-1.                                                 NC1164.2
085100     MOVE DS-LS-5 TO DU-005.                                      NC1164.2
085200     IF DU-005 EQUAL TO 91275                                     NC1164.2
085300         PERFORM PASS                                             NC1164.2
085400         GO TO SIG-WRTE-GF-9-1.                                   NC1164.2
085500     GO TO SIG-FAIL-GF-9-1.                                       NC1164.2
085600 SIG-DELETE-GF-9-1.                                               NC1164.2
085700     PERFORM DE-LETE.                                             NC1164.2
085800     PERFORM PRINT-DETAIL.                                        NC1164.2
085900     GO TO   SIG-INIT-GF-10.                                      NC1164.2
086000 SIG-FAIL-GF-9-1.                                                 NC1164.2
086100     PERFORM FAIL.                                                NC1164.2
086200     MOVE DU-005 TO COMPUTED-18V0.                                NC1164.2
086300     MOVE 91275 TO CORRECT-18V0.                                  NC1164.2
086400     MOVE "MOVE DS-LS-5 TO DU-005" TO RE-MARK.                    NC1164.2
086500 SIG-WRTE-GF-9-1.                                                 NC1164.2
086600     PERFORM PRINT-DETAIL.                                        NC1164.2
086700 SIG-TEST-GF-9-2.                                                 NC1164.2
086800     MOVE DS-LS-5 TO DS-005.                                      NC1164.2
086900     IF DS-005 EQUAL TO +91275                                    NC1164.2
087000         PERFORM PASS                                             NC1164.2
087100         GO TO SIG-WRTE-GF-9-2.                                   NC1164.2
087200 SIG-FAIL-GF-9-2.                                                 NC1164.2
087300     PERFORM FAIL.                                                NC1164.2
087400     MOVE DS-005 TO COMPUTED-18V0.                                NC1164.2
087500     MOVE +91275 TO CORRECT-18V0.                                 NC1164.2
087600     MOVE "MOVE DS-LS-5 TO DS-005" TO RE-MARK.                    NC1164.2
087700 SIG-WRTE-GF-9-2.                                                 NC1164.2
087800     MOVE 2 TO REC-CT.                                            NC1164.2
087900     PERFORM PRINT-DETAIL.                                        NC1164.2
088000 SIG-TEST-GF-9-3.                                                 NC1164.2
088100     MOVE DS-LS-5 TO CU-005.                                      NC1164.2
088200     IF CU-005 EQUAL TO 91275                                     NC1164.2
088300         PERFORM PASS                                             NC1164.2
088400         GO TO SIG-WRTE-GF-9-3.                                   NC1164.2
088500 SIG-FAIL-GF-9-3.                                                 NC1164.2
088600     PERFORM FAIL.                                                NC1164.2
088700     MOVE CU-005 TO COMPUTED-18V0.                                NC1164.2
088800     MOVE 91275 TO CORRECT-18V0.                                  NC1164.2
088900     MOVE "MOVE DS-LS-5 TO CU-005" TO RE-MARK.                    NC1164.2
089000 SIG-WRTE-GF-9-3.                                                 NC1164.2
089100     MOVE 3 TO REC-CT.                                            NC1164.2
089200     PERFORM PRINT-DETAIL.                                        NC1164.2
089300 SIG-TEST-GF-9-4.                                                 NC1164.2
089400     MOVE DS-LS-5 TO CS-005.                                      NC1164.2
089500     IF CS-005 EQUAL TO +91275                                    NC1164.2
089600         PERFORM PASS                                             NC1164.2
089700         GO TO SIG-WRTE-GF-9-4.                                   NC1164.2
089800 SIG-FAIL-GF-9-4.                                                 NC1164.2
089900     PERFORM FAIL.                                                NC1164.2
090000     MOVE CS-005 TO COMPUTED-18V0.                                NC1164.2
090100     MOVE +91275 TO CORRECT-18V0.                                 NC1164.2
090200     MOVE "MOVE DS-LS-5 TO CS-005" TO RE-MARK.                    NC1164.2
090300 SIG-WRTE-GF-9-4.                                                 NC1164.2
090400     MOVE 4 TO REC-CT.                                            NC1164.2
090500     PERFORM PRINT-DETAIL.                                        NC1164.2
090600 SIG-INIT-GF-10.                                                  NC1164.2
090700     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
090800     MOVE "SIG-TEST-GF-10" TO PAR-NAME.                           NC1164.2
090900     MOVE "SENDING ITEM DS-TS-4" TO FEATURE.                      NC1164.2
091000*        THIS TEST MOVES A NUMERIC ITEM WITH SIGN IS TRAILING     NC1164.2
091100*    SEPARATE CHARACTER TO UNSIGNED DISPLAY, SIGNED DISPLAY,      NC1164.2
091200*    UNSIGNED COMPUTATIONAL AND SIGNED COMPUTATIONAL ITEMS.       NC1164.2
091300 SIG-TEST-GF-10-1.                                                NC1164.2
091400     MOVE DS-TS-4 TO DU-005.                                      NC1164.2
091500     IF DU-005 NOT EQUAL TO 08036                                 NC1164.2
091600         GO TO SIG-FAIL-GF-10-1.                                  NC1164.2
091700     PERFORM PASS.                                                NC1164.2
091800     GO TO SIG-WRTE-GF-10-1.                                      NC1164.2
091900 SIG-DELETE-GF-10-1.                                              NC1164.2
092000     PERFORM DE-LETE.                                             NC1164.2
092100     PERFORM PRINT-DETAIL.                                        NC1164.2
092200     GO TO   SIG-INIT-GF-11.                                      NC1164.2
092300 SIG-FAIL-GF-10-1.                                                NC1164.2
092400     PERFORM FAIL.                                                NC1164.2
092500     MOVE DU-005 TO COMPUTED-18V0.                                NC1164.2
092600     MOVE 08036 TO CORRECT-18V0.                                  NC1164.2
092700     MOVE "MOVE DS-TS-4 TO DU-005" TO RE-MARK.                    NC1164.2
092800 SIG-WRTE-GF-10-1.                                                NC1164.2
092900     MOVE 1 TO REC-CT.                                            NC1164.2
093000     PERFORM PRINT-DETAIL.                                        NC1164.2
093100 SIG-TEST-GF-10-2.                                                NC1164.2
093200     MOVE DS-TS-4 TO DS-005.                                      NC1164.2
093300     IF DS-005 NOT EQUAL TO -08036                                NC1164.2
093400         GO TO SIG-FAIL-GF-10-2.                                  NC1164.2
093500     PERFORM PASS.                                                NC1164.2
093600     GO TO SIG-WRTE-GF-10-2.                                      NC1164.2
093700 SIG-FAIL-GF-10-2.                                                NC1164.2
093800     PERFORM FAIL.                                                NC1164.2
093900     MOVE DS-005 TO COMPUTED-18V0.                                NC1164.2
094000     MOVE -08036 TO CORRECT-18V0.                                 NC1164.2
094100     MOVE "MOVE DS-TS-4 TO DS-005" TO RE-MARK.                    NC1164.2
094200 SIG-WRTE-GF-10-2.                                                NC1164.2
094300     MOVE 2 TO REC-CT.                                            NC1164.2
094400     PERFORM PRINT-DETAIL.                                        NC1164.2
094500 SIG-TEST-GF-10-3.                                                NC1164.2
094600     MOVE DS-TS-4 TO CU-005.                                      NC1164.2
094700     IF CU-005 NOT EQUAL TO 08036                                 NC1164.2
094800         GO TO SIG-FAIL-GF-10-3.                                  NC1164.2
094900     PERFORM PASS.                                                NC1164.2
095000     GO TO SIG-WRTE-GF-10-3.                                      NC1164.2
095100 SIG-FAIL-GF-10-3.                                                NC1164.2
095200     PERFORM FAIL.                                                NC1164.2
095300     MOVE CU-005 TO COMPUTED-18V0.                                NC1164.2
095400     MOVE 08036 TO CORRECT-18V0.                                  NC1164.2
095500     MOVE "MOVE DS-TS-4 TO CU-005" TO RE-MARK.                    NC1164.2
095600 SIG-WRTE-GF-10-3.                                                NC1164.2
095700     MOVE 3 TO REC-CT.                                            NC1164.2
095800     PERFORM PRINT-DETAIL.                                        NC1164.2
095900 SIG-TEST-GF-10-4.                                                NC1164.2
096000     MOVE DS-TS-4 TO CS-005.                                      NC1164.2
096100     IF CS-005 NOT EQUAL TO -08036                                NC1164.2
096200         GO TO SIG-FAIL-GF-10-4.                                  NC1164.2
096300     PERFORM PASS.                                                NC1164.2
096400     GO TO SIG-WRTE-GF-10-4.                                      NC1164.2
096500 SIG-FAIL-GF-10-4.                                                NC1164.2
096600     PERFORM FAIL.                                                NC1164.2
096700     MOVE CS-005 TO COMPUTED-18V0.                                NC1164.2
096800     MOVE -08036 TO CORRECT-18V0.                                 NC1164.2
096900     MOVE "MOVE DS-TS-4 TO CS-005" TO RE-MARK.                    NC1164.2
097000 SIG-WRTE-GF-10-4.                                                NC1164.2
097100     MOVE 4 TO REC-CT.                                            NC1164.2
097200     PERFORM PRINT-DETAIL.                                        NC1164.2
097300 SIG-INIT-GF-11.                                                  NC1164.2
097400     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
097500     MOVE "SIG-TEST-GF-11" TO PAR-NAME.                           NC1164.2
097600     MOVE "SIGN SEPARATE ITEMS" TO FEATURE.                       NC1164.2
097700*        THIS TEST CONTAINS MOVE STATEMENTS WHERE BOTH THE        NC1164.2
097800*    SENDING AND RECEIVING ITEM HAVE SEPARATE SIGN CHARACTERS.    NC1164.2
097900 SIG-TEST-GF-11-1.                                                NC1164.2
098000     MOVE DS-LS-4 TO WRK-DS-LS-5.                                 NC1164.2
098100     IF GRP-09 EQUAL TO "-09127"                                  NC1164.2
098200         PERFORM PASS                                             NC1164.2
098300         GO TO SIG-WRTE-GF-11-1.                                  NC1164.2
098400     GO TO SIG-FAIL-GF-11-1.                                      NC1164.2
098500 SIG-DELETE-GF-11-1.                                              NC1164.2
098600     PERFORM DE-LETE.                                             NC1164.2
098700     PERFORM PRINT-DETAIL.                                        NC1164.2
098800     GO TO   SIG-INIT-GF-12.                                      NC1164.2
098900 SIG-FAIL-GF-11-1.                                                NC1164.2
099000     PERFORM FAIL.                                                NC1164.2
099100     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
099200     MOVE "-09127" TO CORRECT-A.                                  NC1164.2
099300     MOVE "MOVE DS-LS-4 TO WRK-DS-LS-5" TO RE-MARK.               NC1164.2
099400 SIG-WRTE-GF-11-1.                                                NC1164.2
099500     MOVE 1 TO REC-CT.                                            NC1164.2
099600     PERFORM PRINT-DETAIL.                                        NC1164.2
099700 SIG-TEST-GF-11-2.                                                NC1164.2
099800     MOVE DS-LS-4 TO WRK-DS-TS-5.                                 NC1164.2
099900     IF GRP-10 NOT EQUAL TO "09127-"                              NC1164.2
100000         GO TO SIG-FAIL-GF-11-2.                                  NC1164.2
100100     PERFORM PASS.                                                NC1164.2
100200     GO TO SIG-WRTE-GF-11-2.                                      NC1164.2
100300 SIG-FAIL-GF-11-2.                                                NC1164.2
100400     PERFORM FAIL.                                                NC1164.2
100500     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
100600     MOVE "09127-" TO CORRECT-A.                                  NC1164.2
100700     MOVE "MOVE DS-LS-4 TO WRK-DS-TS-S" TO RE-MARK.               NC1164.2
100800 SIG-WRTE-GF-11-2.                                                NC1164.2
100900     MOVE 2 TO REC-CT.                                            NC1164.2
101000     PERFORM PRINT-DETAIL.                                        NC1164.2
101100 SIG-TEST-GF-11-3.                                                NC1164.2
101200     MOVE DS-TS-5 TO WRK-DS-LS-5.                                 NC1164.2
101300     IF GRP-09 EQUAL TO "+80361"                                  NC1164.2
101400         PERFORM PASS                                             NC1164.2
101500         GO TO SIG-WRTE-GF-11-3.                                  NC1164.2
101600 SIG-FAIL-GF-11-3.                                                NC1164.2
101700     PERFORM FAIL.                                                NC1164.2
101800     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
101900     MOVE "+80361" TO CORRECT-A.                                  NC1164.2
102000     MOVE "MOVE DS-TS-5 TO WRK-DS-LS-5" TO RE-MARK.               NC1164.2
102100 SIG-WRTE-GF-11-3.                                                NC1164.2
102200     MOVE 3 TO REC-CT.                                            NC1164.2
102300     PERFORM PRINT-DETAIL.                                        NC1164.2
102400 SIG-TEST-GF-11-4.                                                NC1164.2
102500     MOVE DS-TS-5 TO WRK-DS-TS-5.                                 NC1164.2
102600     IF GRP-10 NOT EQUAL TO "80361+"                              NC1164.2
102700         GO TO SIG-FAIL-GF-11-4.                                  NC1164.2
102800     PERFORM PASS.                                                NC1164.2
102900     GO TO SIG-WRTE-GF-11-4.                                      NC1164.2
103000 SIG-FAIL-GF-11-4.                                                NC1164.2
103100     PERFORM FAIL.                                                NC1164.2
103200     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
103300     MOVE "80361+" TO CORRECT-A.                                  NC1164.2
103400     MOVE "MOVE DS-TS-5 TO WRK-DS-TS-5" TO RE-MARK.               NC1164.2
103500 SIG-WRTE-GF-11-4.                                                NC1164.2
103600     MOVE 4 TO REC-CT.                                            NC1164.2
103700     PERFORM PRINT-DETAIL.                                        NC1164.2
103800 SIG-INIT-GF-12.                                                  NC1164.2
103900     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
104000     MOVE "SIG-TEST-GF-12" TO PAR-NAME.                           NC1164.2
104100     MOVE "SIGN CLAUSE ITEMS" TO FEATURE.                         NC1164.2
104200*        THIS TEST CONTAINS MOVE STATEMENTS WITH A SIGN IS        NC1164.2
104300*    SEPARATE SENDING ITEM AND SIGN CLAUSE RECEIVING ITEMS,       NC1164.2
104400*    BUT RECEIVING ITEMS SIGNS ARE NOT SEPARATE.                  NC1164.2
104500 SIG-TEST-GF-12-1.                                                NC1164.2
104600     MOVE DS-LS-5 TO WRK-DS-L-5.                                  NC1164.2
104700     IF WRK-DS-L-5 NOT EQUAL TO +91275                            NC1164.2
104800         GO TO SIG-FAIL-GF-12-1.                                  NC1164.2
104900     PERFORM PASS.                                                NC1164.2
105000     GO TO SIG-WRTE-GF-12-1.                                      NC1164.2
105100 SIG-DELETE-GF-12-1.                                              NC1164.2
105200     PERFORM DE-LETE.                                             NC1164.2
105300     PERFORM PRINT-DETAIL.                                        NC1164.2
105400     GO TO   SIG-INIT-GF-13.                                      NC1164.2
105500 SIG-FAIL-GF-12-1.                                                NC1164.2
105600     PERFORM FAIL.                                                NC1164.2
105700     MOVE WRK-DS-L-5 TO COMPUTED-18V0.                            NC1164.2
105800     MOVE "+91275" TO CORRECT-A.                                  NC1164.2
105900     MOVE "MOVE DS-LS-5 TO WRK-DS-L-5" TO RE-MARK.                NC1164.2
106000 SIG-WRTE-GF-12-1.                                                NC1164.2
106100     MOVE 1 TO REC-CT.                                            NC1164.2
106200     PERFORM PRINT-DETAIL.                                        NC1164.2
106300 SIG-TEST-GF-12-2.                                                NC1164.2
106400     MOVE DS-LS-5 TO WRK-DS-T-5.                                  NC1164.2
106500     IF WRK-DS-T-5 NOT EQUAL TO +91275                            NC1164.2
106600         GO TO SIG-FAIL-GF-12-2.                                  NC1164.2
106700     PERFORM PASS.                                                NC1164.2
106800     GO TO SIG-WRTE-GF-12-2.                                      NC1164.2
106900 SIG-FAIL-GF-12-2.                                                NC1164.2
107000     PERFORM FAIL.                                                NC1164.2
107100     MOVE "+91275" TO CORRECT-A.                                  NC1164.2
107200     MOVE WRK-DS-T-5 TO COMPUTED-18V0.                            NC1164.2
107300     MOVE "MOVE DS-LS-5 TO WRK-DS-T-5" TO RE-MARK.                NC1164.2
107400 SIG-WRTE-GF-12-2.                                                NC1164.2
107500     MOVE 2 TO REC-CT.                                            NC1164.2
107600     PERFORM PRINT-DETAIL.                                        NC1164.2
107700 SIG-TEST-GF-12-3.                                                NC1164.2
107800     MOVE DS-TS-5 TO WRK-DS-L-5.                                  NC1164.2
107900     IF WRK-DS-L-5 NOT EQUAL TO +80361                            NC1164.2
108000         GO TO SIG-FAIL-GF-12-3.                                  NC1164.2
108100     PERFORM PASS.                                                NC1164.2
108200     GO TO SIG-WRITE-GF-12-3.                                     NC1164.2
108300 SIG-FAIL-GF-12-3.                                                NC1164.2
108400     PERFORM FAIL.                                                NC1164.2
108500     MOVE "+80361" TO CORRECT-A.                                  NC1164.2
108600     MOVE WRK-DS-L-5 TO COMPUTED-18V0.                            NC1164.2
108700     MOVE "MOVE DS-TS-5 TO WRK-DS-L-5" TO RE-MARK.                NC1164.2
108800 SIG-WRITE-GF-12-3.                                               NC1164.2
108900     MOVE 3 TO REC-CT.                                            NC1164.2
109000     PERFORM PRINT-DETAIL.                                        NC1164.2
109100 SIG-TEST-GF-12-4.                                                NC1164.2
109200     MOVE DS-TS-5 TO WRK-DS-T-5.                                  NC1164.2
109300     IF WRK-DS-T-5 NOT EQUAL TO +80361                            NC1164.2
109400         GO TO SIG-FAIL-GF-12-4.                                  NC1164.2
109500     PERFORM PASS.                                                NC1164.2
109600     GO TO SIG-WRITE-GF-12-4.                                     NC1164.2
109700 SIG-FAIL-GF-12-4.                                                NC1164.2
109800     PERFORM FAIL.                                                NC1164.2
109900     MOVE "+80361" TO CORRECT-A.                                  NC1164.2
110000     MOVE WRK-DS-T-5 TO COMPUTED-18V0.                            NC1164.2
110100     MOVE "MOVE DS-TS-5 TO WRK-DS-T-5" TO RE-MARK.                NC1164.2
110200 SIG-WRITE-GF-12-4.                                               NC1164.2
110300     MOVE 4 TO REC-CT.                                            NC1164.2
110400     PERFORM PRINT-DETAIL.                                        NC1164.2
110500 SIG-INIT-GF-13.                                                  NC1164.2
110600     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
110700     MOVE "SIG-TEST-GF-13" TO PAR-NAME.                           NC1164.2
110800*        THIS TEST CONTAINS MOVE STATEMENTS WITH A SIGN IS        NC1164.2
110900*    SEPARATE RECEIVING ITEM AND SENDING ITEMS WITH A SIGN        NC1164.2
111000*    CLAUSE BUT THE SIGN IS NOT SEPARATE.                         NC1164.2
111100     MOVE ZERO TO WRK-DS-LS-5.                                    NC1164.2
111200     MOVE ZERO TO WRK-DS-TS-5.                                    NC1164.2
111300 SIG-TEST-GF-13-1.                                                NC1164.2
111400     MOVE DS-L-5 TO WRK-DS-LS-5.                                  NC1164.2
111500     IF GRP-09 EQUAL TO "+91275"                                  NC1164.2
111600         PERFORM PASS                                             NC1164.2
111700         GO TO SIG-WRITE-GF-13-1.                                 NC1164.2
111800     GO TO SIG-FAIL-GF-13-1.                                      NC1164.2
111900 SIG-DELETE-GF-13-1.                                              NC1164.2
112000     PERFORM DE-LETE.                                             NC1164.2
112100     PERFORM PRINT-DETAIL.                                        NC1164.2
112200     GO TO   SIG-INIT-GF-14.                                      NC1164.2
112300 SIG-FAIL-GF-13-1.                                                NC1164.2
112400     PERFORM FAIL.                                                NC1164.2
112500     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
112600     MOVE "+91275" TO CORRECT-A.                                  NC1164.2
112700     MOVE "MOVE DS-L-5 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
112800 SIG-WRITE-GF-13-1.                                               NC1164.2
112900     MOVE 1 TO REC-CT.                                            NC1164.2
113000     PERFORM PRINT-DETAIL.                                        NC1164.2
113100 SIG-TEST-GF-13-2.                                                NC1164.2
113200     MOVE DS-T-4 TO WRK-DS-LS-5.                                  NC1164.2
113300     IF GRP-09 EQUAL TO "-03621"                                  NC1164.2
113400         PERFORM PASS                                             NC1164.2
113500         GO TO SIG-WRITE-GF-13-2.                                 NC1164.2
113600 SIG-FAIL-GF-13-2.                                                NC1164.2
113700     PERFORM FAIL.                                                NC1164.2
113800     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
113900     MOVE "-03621" TO CORRECT-A.                                  NC1164.2
114000     MOVE "MOVE DS-T-4 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
114100 SIG-WRITE-GF-13-2.                                               NC1164.2
114200     MOVE 2 TO REC-CT.                                            NC1164.2
114300     PERFORM PRINT-DETAIL.                                        NC1164.2
114400 SIG-TEST-GF-13-3.                                                NC1164.2
114500     MOVE DS-L-5 TO WRK-DS-TS-5.                                  NC1164.2
114600     IF GRP-10 EQUAL TO "91275+"                                  NC1164.2
114700         PERFORM PASS                                             NC1164.2
114800         GO TO SIG-WRITE-GF-13-3.                                 NC1164.2
114900 SIG-FAIL-GF-13-3.                                                NC1164.2
115000     PERFORM FAIL.                                                NC1164.2
115100     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
115200     MOVE "91275+" TO CORRECT-A.                                  NC1164.2
115300     MOVE "MOVE DS-L-5 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
115400 SIG-WRITE-GF-13-3.                                               NC1164.2
115500     MOVE 3 TO REC-CT.                                            NC1164.2
115600     PERFORM PRINT-DETAIL.                                        NC1164.2
115700 SIG-TEST-GF-13-4.                                                NC1164.2
115800     MOVE DS-T-4 TO WRK-DS-TS-5.                                  NC1164.2
115900     IF GRP-10 EQUAL TO "03621-"                                  NC1164.2
116000         PERFORM PASS                                             NC1164.2
116100         GO TO SIG-WRITE-GF-13-4.                                 NC1164.2
116200 SIG-FAIL-GF-13-4.                                                NC1164.2
116300     PERFORM FAIL.                                                NC1164.2
116400     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
116500     MOVE "03621-" TO CORRECT-A.                                  NC1164.2
116600     MOVE "MOVE DS-T-4 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
116700 SIG-WRITE-GF-13-4.                                               NC1164.2
116800     MOVE 4 TO REC-CT.                                            NC1164.2
116900     PERFORM PRINT-DETAIL.                                        NC1164.2
117000 SIG-INIT-GF-14.                                                  NC1164.2
117100     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
117200     MOVE "SIG-TEST-GF-14" TO PAR-NAME.                           NC1164.2
117300     MOVE "SIGNED NUM. TO ALPHA" TO FEATURE.                      NC1164.2
117400*        THIS TEST CONTAINS MOVE STATEMENTS WITH A SIGNED         NC1164.2
117500*    NUMERIC SENDING ITEM AND ALPHANUMERIC RECEIVING ITEM.        NC1164.2
117600*    THE OPERATIONAL SIGN SHOULD NOT BE MOVED AND SPACE           NC1164.2
117700*    FILLING ON THE RIGHT SHOULD OCCUR.                           NC1164.2
117800 SIG-TEST-GF-14-1.                                                NC1164.2
117900     MOVE DS-LS-5 TO AN-006.                                      NC1164.2
118000     IF AN-006 EQUAL TO "91275 "                                  NC1164.2
118100         PERFORM PASS                                             NC1164.2
118200         GO TO SIG-WRITE-GF-14-1.                                 NC1164.2
118300     GO TO SIG-FAIL-GF-14-1.                                      NC1164.2
118400 SIG-DELETE-GF-14-1.                                              NC1164.2
118500     PERFORM DE-LETE.                                             NC1164.2
118600     PERFORM PRINT-DETAIL.                                        NC1164.2
118700     GO TO   SIG-INIT-GF-15.                                      NC1164.2
118800 SIG-FAIL-GF-14-1.                                                NC1164.2
118900     PERFORM FAIL.                                                NC1164.2
119000     MOVE AN-006 TO COMPUTED-A.                                   NC1164.2
119100     MOVE "91275 " TO CORRECT-A.                                  NC1164.2
119200     MOVE "MOVE DS-LS-5 TO AN-006" TO RE-MARK.                    NC1164.2
119300 SIG-WRITE-GF-14-1.                                               NC1164.2
119400     MOVE 1 TO REC-CT.                                            NC1164.2
119500     PERFORM PRINT-DETAIL.                                        NC1164.2
119600 SIG-TEST-GF-14-2.                                                NC1164.2
119700     MOVE SPACE TO AN-006.                                        NC1164.2
119800     MOVE DS-TS-4 TO AN-006.                                      NC1164.2
119900     IF AN-006 NOT EQUAL TO "8036  "                              NC1164.2
120000         GO TO SIG-FAIL-GF-14-2.                                  NC1164.2
120100     PERFORM PASS.                                                NC1164.2
120200     GO TO SIG-WRITE-GF-14-2.                                     NC1164.2
120300 SIG-FAIL-GF-14-2.                                                NC1164.2
120400     PERFORM FAIL.                                                NC1164.2
120500     MOVE AN-006 TO COMPUTED-A.                                   NC1164.2
120600     MOVE "8036  " TO CORRECT-A.                                  NC1164.2
120700     MOVE "MOVE DS-TS-4 TO AN-006" TO RE-MARK.                    NC1164.2
120800 SIG-WRITE-GF-14-2.                                               NC1164.2
120900     MOVE 2 TO REC-CT.                                            NC1164.2
121000     PERFORM PRINT-DETAIL.                                        NC1164.2
121100 SIG-TEST-GF-14-3.                                                NC1164.2
121200     MOVE SPACE TO AN-006.                                        NC1164.2
121300     MOVE DS-L-4 TO AN-006.                                       NC1164.2
121400     IF AN-006 EQUAL TO "9127  "                                  NC1164.2
121500         PERFORM PASS                                             NC1164.2
121600         GO TO SIG-WRITE-GF-14-3.                                 NC1164.2
121700 SIG-FAIL-GF-14-3.                                                NC1164.2
121800     PERFORM FAIL.                                                NC1164.2
121900     MOVE AN-006 TO COMPUTED-A.                                   NC1164.2
122000     MOVE "9127  " TO CORRECT-A.                                  NC1164.2
122100     MOVE "MOVE DS-L-4 TO AN-006" TO RE-MARK.                     NC1164.2
122200 SIG-WRITE-GF-14-3.                                               NC1164.2
122300     MOVE 3 TO REC-CT.                                            NC1164.2
122400     PERFORM PRINT-DETAIL.                                        NC1164.2
122500 SIG-TEST-GF-14-4.                                                NC1164.2
122600     MOVE SPACE TO AN-006.                                        NC1164.2
122700     MOVE DS-T-5 TO AN-006.                                       NC1164.2
122800     IF AN-006 NOT EQUAL TO "83621 "                              NC1164.2
122900         GO TO SIG-FAIL-GF-14-4.                                  NC1164.2
123000     PERFORM PASS.                                                NC1164.2
123100     GO TO SIG-WRITE-GF-14-4.                                     NC1164.2
123200 SIG-FAIL-GF-14-4.                                                NC1164.2
123300     PERFORM FAIL.                                                NC1164.2
123400     MOVE AN-006 TO COMPUTED-A.                                   NC1164.2
123500     MOVE "83621 " TO CORRECT-A.                                  NC1164.2
123600     MOVE "MOVE DS-T-5 TO AN-006" TO RE-MARK.                     NC1164.2
123700 SIG-WRITE-GF-14-4.                                               NC1164.2
123800     MOVE 4 TO REC-CT.                                            NC1164.2
123900     PERFORM PRINT-DETAIL.                                        NC1164.2
124000 SIG-INIT-GF-15.                                                  NC1164.2
124100     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
124200     MOVE "SIG-TEST-GF-15" TO PAR-NAME.                           NC1164.2
124300     MOVE 15759 TO DU-005 CU-005.                                 NC1164.2
124400     MOVE -15759 TO DS-005 CS-005.                                NC1164.2
124500*        THIS TEST MOVES SIGNED AND UNSIGNED DISPLAY ITEMS,       NC1164.2
124600*    SIGNED AND UNSIGNED COMPUTATIONAL ITEMS, AND SIGNED AND      NC1164.2
124700*    UNSIGNED NUMERIC LITERALS TO A NUMERIC ITEM WITH SIGN        NC1164.2
124800*    LEADING SEPARATE.                                            NC1164.2
124900     MOVE "RECEIVING ITEM DS-LS" TO FEATURE.                      NC1164.2
125000 SIG-TEST-GF-15-1.                                                NC1164.2
125100     MOVE SPACE TO GRP-09.                                        NC1164.2
125200     MOVE DU-005 TO WRK-DS-LS-5.                                  NC1164.2
125300     IF GRP-09 EQUAL TO "+15759"                                  NC1164.2
125400         PERFORM PASS                                             NC1164.2
125500         GO TO SIG-WRITE-GF-15-1.                                 NC1164.2
125600     GO TO SIG-FAIL-GF-15-1.                                      NC1164.2
125700 SIG-DELETE-GF-15-1.                                              NC1164.2
125800     PERFORM DE-LETE.                                             NC1164.2
125900     PERFORM PRINT-DETAIL.                                        NC1164.2
126000     GO TO   SIG-INIT-GF-16.                                      NC1164.2
126100 SIG-FAIL-GF-15-1.                                                NC1164.2
126200     PERFORM FAIL.                                                NC1164.2
126300     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
126400     MOVE "+15759" TO CORRECT-A.                                  NC1164.2
126500     MOVE "MOVE DU-005 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
126600 SIG-WRITE-GF-15-1.                                               NC1164.2
126700     MOVE 1 TO REC-CT.                                            NC1164.2
126800     PERFORM PRINT-DETAIL.                                        NC1164.2
126900 SIG-TEST-GF-15-2.                                                NC1164.2
127000     MOVE SPACE TO GRP-09.                                        NC1164.2
127100     MOVE CU-005 TO WRK-DS-LS-5.                                  NC1164.2
127200     IF GRP-09 NOT EQUAL TO "+15759"                              NC1164.2
127300         GO TO SIG-FAIL-GF-15-2.                                  NC1164.2
127400     PERFORM PASS                                                 NC1164.2
127500     GO TO SIG-WRITE-GF-15-2.                                     NC1164.2
127600 SIG-FAIL-GF-15-2.                                                NC1164.2
127700     PERFORM FAIL.                                                NC1164.2
127800     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
127900     MOVE "+15759" TO CORRECT-A.                                  NC1164.2
128000     MOVE "MOVE CU-005 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
128100 SIG-WRITE-GF-15-2.                                               NC1164.2
128200     MOVE 2 TO REC-CT.                                            NC1164.2
128300     PERFORM PRINT-DETAIL.                                        NC1164.2
128400 SIG-TEST-GF-15-3.                                                NC1164.2
128500     MOVE SPACE TO GRP-09.                                        NC1164.2
128600     MOVE DS-005 TO WRK-DS-LS-5.                                  NC1164.2
128700     IF GRP-09 EQUAL TO "-15759"                                  NC1164.2
128800         PERFORM PASS                                             NC1164.2
128900         GO TO SIG-WRITE-GF-15-3.                                 NC1164.2
129000 SIG-FAIL-GF-15-3.                                                NC1164.2
129100     PERFORM FAIL.                                                NC1164.2
129200     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
129300     MOVE "-15759" TO CORRECT-A.                                  NC1164.2
129400     MOVE "MOVE DS-005 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
129500 SIG-WRITE-GF-15-3.                                               NC1164.2
129600     MOVE 3 TO REC-CT.                                            NC1164.2
129700     PERFORM PRINT-DETAIL.                                        NC1164.2
129800 SIG-TEST-GF-15-4.                                                NC1164.2
129900     MOVE SPACE TO GRP-09.                                        NC1164.2
130000     MOVE CS-005 TO WRK-DS-LS-5.                                  NC1164.2
130100     IF GRP-09 NOT EQUAL TO "-15759"                              NC1164.2
130200         GO TO SIG-FAIL-GF-15-4.                                  NC1164.2
130300     PERFORM PASS.                                                NC1164.2
130400     GO TO SIG-WRITE-GF-15-4.                                     NC1164.2
130500 SIG-FAIL-GF-15-4.                                                NC1164.2
130600     PERFORM FAIL.                                                NC1164.2
130700     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
130800     MOVE "-15759" TO CORRECT-A.                                  NC1164.2
130900     MOVE "MOVE CS-005 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
131000 SIG-WRITE-GF-15-4.                                               NC1164.2
131100     MOVE 4 TO REC-CT.                                            NC1164.2
131200     PERFORM PRINT-DETAIL.                                        NC1164.2
131300 SIG-TEST-GF-15-5.                                                NC1164.2
131400     MOVE SPACE TO GRP-09.                                        NC1164.2
131500     MOVE 15759 TO WRK-DS-LS-5.                                   NC1164.2
131600     IF GRP-09 EQUAL TO "+15759"                                  NC1164.2
131700         PERFORM PASS                                             NC1164.2
131800         GO TO SIG-WRITE-GF-15-5.                                 NC1164.2
131900 SIG-FAIL-GF-15-5.                                                NC1164.2
132000     PERFORM FAIL.                                                NC1164.2
132100     MOVE "+15759" TO CORRECT-A.                                  NC1164.2
132200     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
132300     MOVE "MOVE 15759 TO WRK-DS-LS-5" TO RE-MARK.                 NC1164.2
132400 SIG-WRITE-GF-15-5.                                               NC1164.2
132500     MOVE 5 TO REC-CT.                                            NC1164.2
132600     PERFORM PRINT-DETAIL.                                        NC1164.2
132700 SIG-TEST-GF-15-6.                                                NC1164.2
132800     MOVE SPACE TO GRP-09.                                        NC1164.2
132900     MOVE -15759 TO WRK-DS-LS-5.                                  NC1164.2
133000     IF GRP-09 NOT EQUAL TO "-15759"                              NC1164.2
133100         GO TO SIG-FAIL-GF-15-6.                                  NC1164.2
133200     PERFORM PASS.                                                NC1164.2
133300     GO TO SIG-WRITE-GF-15-6.                                     NC1164.2
133400 SIG-FAIL-GF-15-6.                                                NC1164.2
133500     PERFORM FAIL.                                                NC1164.2
133600     MOVE GRP-09 TO COMPUTED-A.                                   NC1164.2
133700     MOVE "-15759" TO CORRECT-A.                                  NC1164.2
133800     MOVE "MOVE -15759 TO WRK-DS-LS-5" TO RE-MARK.                NC1164.2
133900 SIG-WRITE-GF-15-6.                                               NC1164.2
134000     MOVE 6 TO REC-CT.                                            NC1164.2
134100     PERFORM PRINT-DETAIL.                                        NC1164.2
134200 SIG-INIT-GF-16.                                                  NC1164.2
134300     MOVE "VI-42 5.12.4"  TO ANSI-REFERENCE.                      NC1164.2
134400     MOVE "SIG-TEST-GF-16" TO PAR-NAME.                           NC1164.2
134500     MOVE "RECEIVING ITEM DS-TS" TO FEATURE.                      NC1164.2
134600*        THIS TEST MOVES SIGNED AND UNSIGNED DISPLAY ITEMS,       NC1164.2
134700*    SIGNED AND UNSIGNED COMPUTATIONAL ITEMS, AND SIGNED AND      NC1164.2
134800*    UNSIGNED NUMERIC LITERALS TO A NUMERIC ITEM WITH SIGN        NC1164.2
134900*    TRAILING SEPARATE CLAUSE.                                    NC1164.2
135000     MOVE SPACE TO GRP-10.                                        NC1164.2
135100     MOVE DU-005 TO WRK-DS-TS-5.                                  NC1164.2
135200 SIG-TEST-GF-16-1.                                                NC1164.2
135300     IF GRP-10 EQUAL TO "15759+"                                  NC1164.2
135400         PERFORM PASS                                             NC1164.2
135500         GO TO SIG-WRITE-GF-16-1.                                 NC1164.2
135600     GO TO SIG-FAIL-GF-16-1.                                      NC1164.2
135700 SIG-DELETE-GF-16-1.                                              NC1164.2
135800     PERFORM DE-LETE.                                             NC1164.2
135900     PERFORM PRINT-DETAIL.                                        NC1164.2
136000     GO TO   SIG-INIT-GF-17.                                      NC1164.2
136100 SIG-FAIL-GF-16-1.                                                NC1164.2
136200     PERFORM FAIL.                                                NC1164.2
136300     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
136400     MOVE "15759+" TO CORRECT-A.                                  NC1164.2
136500     MOVE "MOVE DU-005 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
136600 SIG-WRITE-GF-16-1.                                               NC1164.2
136700     MOVE 1 TO REC-CT.                                            NC1164.2
136800     PERFORM PRINT-DETAIL.                                        NC1164.2
136900 SIG-TEST-GF-16-2.                                                NC1164.2
137000     MOVE SPACE TO GRP-10.                                        NC1164.2
137100     MOVE CU-005 TO WRK-DS-TS-5.                                  NC1164.2
137200     IF GRP-10 NOT EQUAL TO "15759+"                              NC1164.2
137300         GO TO SIG-FAIL-GF-16-2.                                  NC1164.2
137400     PERFORM PASS.                                                NC1164.2
137500     GO TO SIG-WRITE-GF-16-2.                                     NC1164.2
137600 SIG-FAIL-GF-16-2.                                                NC1164.2
137700     PERFORM FAIL.                                                NC1164.2
137800     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
137900     MOVE "15759+" TO CORRECT-A.                                  NC1164.2
138000     MOVE "MOVE CU-005 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
138100 SIG-WRITE-GF-16-2.                                               NC1164.2
138200     MOVE 2 TO REC-CT.                                            NC1164.2
138300     PERFORM PRINT-DETAIL.                                        NC1164.2
138400 SIG-TEST-GF-16-3.                                                NC1164.2
138500     MOVE SPACE TO GRP-10.                                        NC1164.2
138600     MOVE DS-005 TO WRK-DS-TS-5.                                  NC1164.2
138700     IF GRP-10 EQUAL TO "15759-"                                  NC1164.2
138800         PERFORM PASS                                             NC1164.2
138900         GO TO SIG-WRITE-GF-16-3.                                 NC1164.2
139000 SIG-FAIL-GF-16-3.                                                NC1164.2
139100     PERFORM FAIL.                                                NC1164.2
139200     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
139300     MOVE "15759-" TO CORRECT-A.                                  NC1164.2
139400     MOVE "MOVE DS-005 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
139500 SIG-WRITE-GF-16-3.                                               NC1164.2
139600     MOVE 3 TO REC-CT.                                            NC1164.2
139700     PERFORM PRINT-DETAIL.                                        NC1164.2
139800 SIG-TEST-GF-16-4.                                                NC1164.2
139900     MOVE SPACE TO GRP-10.                                        NC1164.2
140000     MOVE CS-005 TO WRK-DS-TS-5.                                  NC1164.2
140100     IF GRP-10 NOT EQUAL TO "15759-"                              NC1164.2
140200         GO TO SIG-FAIL-GF-16-4.                                  NC1164.2
140300     PERFORM PASS.                                                NC1164.2
140400     GO TO SIG-WRITE-GF-16-4.                                     NC1164.2
140500 SIG-FAIL-GF-16-4.                                                NC1164.2
140600     PERFORM FAIL.                                                NC1164.2
140700     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
140800     MOVE "15759-" TO CORRECT-A.                                  NC1164.2
140900     MOVE "MOVE CS-005 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
141000 SIG-WRITE-GF-16-4.                                               NC1164.2
141100     MOVE 4 TO REC-CT.                                            NC1164.2
141200     PERFORM PRINT-DETAIL.                                        NC1164.2
141300 SIG-TEST-GF-16-5.                                                NC1164.2
141400     MOVE SPACE TO GRP-10.                                        NC1164.2
141500     MOVE 15759 TO WRK-DS-TS-5.                                   NC1164.2
141600     IF GRP-10 EQUAL TO "15759+"                                  NC1164.2
141700         PERFORM PASS                                             NC1164.2
141800         GO TO SIG-WRITE-GF-16-5.                                 NC1164.2
141900 SIG-FAIL-GF-16-5.                                                NC1164.2
142000     PERFORM FAIL.                                                NC1164.2
142100     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
142200     MOVE "15759+" TO CORRECT-A.                                  NC1164.2
142300     MOVE "MOVE 15759 TO WRK-DS-TS-5" TO RE-MARK.                 NC1164.2
142400 SIG-WRITE-GF-16-5.                                               NC1164.2
142500     MOVE 5 TO REC-CT.                                            NC1164.2
142600     PERFORM PRINT-DETAIL.                                        NC1164.2
142700 SIG-TEST-GF-16-6.                                                NC1164.2
142800     MOVE SPACE TO GRP-10.                                        NC1164.2
142900     MOVE -15759 TO WRK-DS-TS-5.                                  NC1164.2
143000     IF GRP-10 NOT EQUAL TO "15759-"                              NC1164.2
143100        GO TO SIG-FAIL-GF-16-6.                                   NC1164.2
143200     PERFORM PASS.                                                NC1164.2
143300     GO TO SIG-WRITE-GF-16-6.                                     NC1164.2
143400 SIG-FAIL-GF-16-6.                                                NC1164.2
143500     PERFORM FAIL.                                                NC1164.2
143600     MOVE GRP-10 TO COMPUTED-A.                                   NC1164.2
143700     MOVE "15759-" TO CORRECT-A.                                  NC1164.2
143800     MOVE "MOVE -15759 TO WRK-DS-TS-5" TO RE-MARK.                NC1164.2
143900 SIG-WRITE-GF-16-6.                                               NC1164.2
144000     MOVE 6 TO REC-CT.                                            NC1164.2
144100     PERFORM PRINT-DETAIL.                                        NC1164.2
144200*                                                                 NC1164.2
144300 SIG-INIT-GF-17.                                                  NC1164.2
144400     MOVE   "VI-42 5.12.4 GR2"  TO ANSI-REFERENCE.                NC1164.2
144500     MOVE   "SIG-TEST-GF-17" TO PAR-NAME.                         NC1164.2
144600     MOVE   "PRECEDENCE OF SUBORDINATE SIGN CLAUSE" TO FEATURE.   NC1164.2
144700     MOVE    1234 TO TEST-17-C.                                   NC1164.2
144800     MOVE    0 TO REC-CT.                                         NC1164.2
144900 SIG-TEST-GF-17.                                                  NC1164.2
145000     IF      TEST-17-C-SIGN = "+"                                 NC1164.2
145100             PERFORM PASS                                         NC1164.2
145200             GO TO   SIG-WRITE-GF-17                              NC1164.2
145300     ELSE                                                         NC1164.2
145400             GO TO   SIG-FAIL-GF-17.                              NC1164.2
145500 SIG-DELETE-GF-17.                                                NC1164.2
145600     PERFORM DE-LETE.                                             NC1164.2
145700     GO TO   SIG-WRITE-GF-17.                                     NC1164.2
145800 SIG-FAIL-GF-17.                                                  NC1164.2
145900     PERFORM FAIL.                                                NC1164.2
146000     MOVE   "POSITIVE SIGN EXPECTED" TO RE-MARK.                  NC1164.2
146100     MOVE   "+"             TO CORRECT-X.                         NC1164.2
146200     MOVE    TEST-17-C-SIGN TO COMPUTED-X.                        NC1164.2
146300 SIG-WRITE-GF-17.                                                 NC1164.2
146400     PERFORM PRINT-DETAIL.                                        NC1164.2
146500*                                                                 NC1164.2
146600 SIG-INIT-GF-18.                                                  NC1164.2
146700     MOVE   "VI-42 5.12.4 GR3"  TO ANSI-REFERENCE.                NC1164.2
146800     MOVE   "SIG-TEST-GF-18" TO PAR-NAME.                         NC1164.2
146900     MOVE   "PRECEDENCE OF SUBORDINATE SIGN CLAUSE" TO FEATURE.   NC1164.2
147000     MOVE    1234 TO TEST-18-B.                                   NC1164.2
147100     MOVE    0 TO REC-CT.                                         NC1164.2
147200 SIG-TEST-GF-18.                                                  NC1164.2
147300     IF      TEST-18-B-SIGN = "+"                                 NC1164.2
147400             PERFORM PASS                                         NC1164.2
147500             GO TO   SIG-WRITE-GF-18                              NC1164.2
147600     ELSE                                                         NC1164.2
147700             GO TO   SIG-FAIL-GF-18.                              NC1164.2
147800 SIG-DELETE-GF-18.                                                NC1164.2
147900     PERFORM DE-LETE.                                             NC1164.2
148000     GO TO   SIG-WRITE-GF-18.                                     NC1164.2
148100 SIG-FAIL-GF-18.                                                  NC1164.2
148200     PERFORM FAIL.                                                NC1164.2
148300     MOVE   "POSITIVE SIGN EXPECTED" TO RE-MARK.                  NC1164.2
148400     MOVE   "+"             TO CORRECT-X.                         NC1164.2
148500     MOVE    TEST-18-B-SIGN TO COMPUTED-X.                        NC1164.2
148600 SIG-WRITE-GF-18.                                                 NC1164.2
148700     PERFORM PRINT-DETAIL.                                        NC1164.2
148800*                                                                 NC1164.2
148900 TERMINATE-ROUTINE.                                               NC1164.2
149000     EXIT.                                                        NC1164.2
149100 CCVS-EXIT SECTION.                                               NC1164.2
149200 CCVS-999999.                                                     NC1164.2
149300     GO TO CLOSE-FILES.                                           NC1164.2
      *END-OF,NC116A                                                                  
