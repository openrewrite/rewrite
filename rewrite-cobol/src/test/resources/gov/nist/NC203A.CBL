      *HEADER,COBOL,NC203A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2034.2
000200 PROGRAM-ID.                                                      NC2034.2
000300     NC203A.                                                      NC2034.2
000400*                                                                 NC2034.2
000500****************************************************************  NC2034.2
000600*                                                              *  NC2034.2
000700*    VALIDATION FOR:-                                          *  NC2034.2
000800*                                                              *  NC2034.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2034.2
001000*                                                              *  NC2034.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2034.2
001200*                                                              *  NC2034.2
001300****************************************************************  NC2034.2
001400*                                                              *  NC2034.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2034.2
001600*                                                              *  NC2034.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2034.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2034.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2034.2
002000*                                                              *  NC2034.2
002100****************************************************************  NC2034.2
002200*    THIS PROGRAM TESTS FORMAT 4 OF THE DIVIDE STATEMENT.      *  NC2034.2
002300*                                                                 NC2034.2
002400****************************************************************  NC2034.2
002500*    THIS COMMENT ENTRY SHOULD APPEAR AS THE LAST LINE BEFORE     NC2034.2
002600*    THE ENVIRONMENT DIVISION.                                    NC2034.2
002700 ENVIRONMENT DIVISION.                                            NC2034.2
002800 CONFIGURATION SECTION.                                           NC2034.2
002900 SOURCE-COMPUTER.                                                 NC2034.2
003000     XXXXX082.                                                    NC2034.2
003100 OBJECT-COMPUTER.                                                 NC2034.2
003200     XXXXX083.                                                    NC2034.2
003300 INPUT-OUTPUT SECTION.                                            NC2034.2
003400 FILE-CONTROL.                                                    NC2034.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC2034.2
003600     XXXXX055.                                                    NC2034.2
003700 DATA DIVISION.                                                   NC2034.2
003800 FILE SECTION.                                                    NC2034.2
003900 FD  PRINT-FILE.                                                  NC2034.2
004000 01  PRINT-REC PICTURE X(120).                                    NC2034.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC2034.2
004200 WORKING-STORAGE SECTION.                                         NC2034.2
004300 01  WS-REMAINDERS.                                               NC2034.2
004400   03  WS-REM                    PIC 99 OCCURS 20.                NC2034.2
004500 01  WRK-XN-00001-1              PIC X.                           NC2034.2
004600 01  WRK-XN-00001-2              PIC X.                           NC2034.2
004700 01  WS-46.                                                       NC2034.2
004800   03  WS-1-20                   PIC X(20).                       NC2034.2
004900   03  WS-21-40                  PIC X(20).                       NC2034.2
005000   03  WS-41-46                  PIC X(6).                        NC2034.2
005100 77  11A                PICTURE 9999  VALUE 9.                    NC2034.2
005200 77  11B   PICTURE 99; VALUE 8.                                   NC2034.2
005300 77  1111C PICTURE 99 VALUE 9.                                    NC2034.2
005400 77  WRK-DS-02V00                 PICTURE S99.                    NC2034.2
005500     88 TEST-2NUC-COND-99         VALUE 99.                       NC2034.2
005600 77  A99-DS-02V00                 PICTURE S99    VALUE 99.        NC2034.2
005700 77  WRK-DS-18V00                 PICTURE S9(18).                 NC2034.2
005800 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC2034.2
005900                                  VALUE 111111111111111111.       NC2034.2
006000 77  A18TWOS-DS-18V00             PICTURE S9(18)                  NC2034.2
006100                                  VALUE 222222222222222222.       NC2034.2
006200 77  WRK-DS-05V00                 PICTURE S9(5).                  NC2034.2
006300 77  A02TWOS-DU-02V00             PICTURE 99     VALUE 22.        NC2034.2
006400 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  NC2034.2
006500 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         NC2034.2
006600 77  AZERO-DS-05V05               PICTURE S9(5)V9(5) VALUE ZERO.  NC2034.2
006700 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC2034.2
006800 77  WRK-DS-0201P                 PICTURE S99P.                   NC2034.2
006900 77  A05ONES-DS-05V00             PICTURE S9(5)  VALUE 11111.     NC2034.2
007000 77  WRK-DS-09V00                 PICTURE S9(9).                  NC2034.2
007100 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC2034.2
007200 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09                        NC2034.2
007300                                  PICTURE S9(18).                 NC2034.2
007400 77  XRAY                    PICTURE IS X.                        NC2034.2
007500 77  W-1                     PICTURE IS 9.                        NC2034.2
007600 77  W-2                     PICTURE IS 99.                       NC2034.2
007700 77  W-3                     PICTURE IS 999.                      NC2034.2
007800 77  W-5                PICTURE 99  VALUE ZERO.                   NC2034.2
007900 77  W-9                     PICTURE 999.                         NC2034.2
008000 77  W-11               PICTURE S99V9.                            NC2034.2
008100 77  D-1                PICTURE S9V99  VALUE 1.06.                NC2034.2
008200 77  D-7                PICTURE S99V99  VALUE 1.09.               NC2034.2
008300 77  ONE                     PICTURE IS 9      VALUE IS 1.        NC2034.2
008400 77  TWO                     PICTURE IS S9     VALUE IS 2.        NC2034.2
008500 77  THREE                   PICTURE IS S9     VALUE IS 3.        NC2034.2
008600 77  FOUR                    PICTURE IS S9     VALUE IS 4.        NC2034.2
008700 77  FIVE                    PICTURE IS S9     VALUE IS 5.        NC2034.2
008800 77  SIX                     PICTURE IS S9     VALUE IS 6.        NC2034.2
008900 77  SEVEN                   PICTURE IS S9     VALUE IS 7.        NC2034.2
009000 77  EIGHT                   PICTURE IS 9      VALUE IS 8.        NC2034.2
009100 77  NINE                    PICTURE IS S9     VALUE IS 9.        NC2034.2
009200 77  TEN                     PICTURE IS S99    VALUE IS 10.       NC2034.2
009300 77  FIFTEEN                 PICTURE IS S99    VALUE IS 15.       NC2034.2
009400 77  TWENTY                  PICTURE IS S99    VALUE IS 20.       NC2034.2
009500 77  TWENTY-5                PICTURE IS S99    VALUE IS 25.       NC2034.2
009600 77  25COUNT PICTURE 999 VALUE ZERO.                              NC2034.2
009700 77  25ANS PICTURE  99 VALUE ZERO.                                NC2034.2
009800 77  25REM PICTURE 99 VALUE ZERO.                                 NC2034.2
009900 77  DIV-30-Y1 PICTURE 999 USAGE COMP SYNC RIGHT VALUE 31.        NC2034.2
010000 77  DIV-30-Y2 PICTURE 999 USAGE COMP VALUE 54.                   NC2034.2
010100 77  DIV-30-Y3 PICTURE 999 VALUE 151.                             NC2034.2
010200 77  DIV-30-Y4         PICTURE 9(4) SYNC RIGHT VALUE 1010.        NC2034.2
010300 77  DIV-Z1-30 PICTURE 999 USAGE COMP VALUE ZERO.                 NC2034.2
010400 77  DIV-Z2-30 PICTURE 999 SYNC RIGHT VALUE ZERO.                 NC2034.2
010500 77  DIV-Z3-30 PICTURE 999 USAGE COMP SYNC RIGHT VALUE ZERO.      NC2034.2
010600 77  DIV-Z4-30 PICTURE 999 VALUE ZERO.                            NC2034.2
010700 77  DIV-30-A1 PICTURE 999 SYNC RIGHT VALUE ZERO.                 NC2034.2
010800 77  DIV-30-A2 PICTURE 999 VALUE ZERO.                            NC2034.2
010900 77  DIV-30-A3 PICTURE 999 USAGE COMP SYNC RIGHT VALUE ZERO.      NC2034.2
011000 77  DIV-30-A4 PICTURE 999 USAGE COMP VALUE ZERO.                 NC2034.2
011100 01  DIV-ENTRIES.                                                 NC2034.2
011200     02 DIV11                PICTURE 999       VALUE 105.         NC2034.2
011300     02 DIV12                PICTURE 9999      VALUE 1000.        NC2034.2
011400     02 DIV13                PICTURE 999.                         NC2034.2
011500     02 DIV14                PICTURE 99.                          NC2034.2
011600     02 DIV15                PICTURE 9V9       VALUE 1.1.         NC2034.2
011700     02 DIV16                PICTURE 99V99     VALUE 89.10.       NC2034.2
011800     02 DIV17                PICTURE 99V99.                       NC2034.2
011900     02 DIV18                PICTURE 9999.                        NC2034.2
012000     02 DIV19                PICTURE 99        VALUE 14.          NC2034.2
012100     02 DIV20                PICTURE 9999      VALUE 2147.        NC2034.2
012200     02 DIV21                PICTURE 999.                         NC2034.2
012300     02 DIV22                     PICTURE 99.                     NC2034.2
012400 01  WRK-DU-1V17-1                PIC 9V9(17).                    NC2034.2
012500 01  WRK-DU-1V5-1                 PIC 9V9(5).                     NC2034.2
012600 01  WRK-DU-2V1-1                 PIC 99V9.                       NC2034.2
012700 01  WRK-DU-05V00-0001            PIC 9(5).                       NC2034.2
012800 01  WRK-DS-05V00-0002            PIC S9(5).                      NC2034.2
012900 01  WRK-CS-05V00-0003            PIC S9(5) COMP.                 NC2034.2
013000 01  WRK-DU-04V02-0004            PIC 9(4)V9(2).                  NC2034.2
013100 01  WRK-DS-04V01-0005            PIC S9(4)V9.                    NC2034.2
013200 01  WRK-NE-1                     PIC .9999/99999,99999,99.       NC2034.2
013300 01  NE-0008                      PIC $9(4).99-.                  NC2034.2
013400 01  NE-0009                      PIC ***99.                      NC2034.2
013500 01  NE-04V01-0006     PIC ****.9.                                NC2034.2
013600 01  GRP-0010.                                                    NC2034.2
013700     02 WRK-DU-03V00-L-0011       PIC 9(03) SYNC LEFT.            NC2034.2
013800     02 WRK-O005F-0012        OCCURS   5  TIMES.                  NC2034.2
013900        03 WRK-O003F-0013     OCCURS   3  TIMES.                  NC2034.2
014000           05 WRK-DS-03V04-O003F-0014 PIC S9(3)V9999              NC2034.2
014100                                            OCCURS 3 TIMES.       NC2034.2
014200 01  DS-02V00-0001                PIC S99  VALUE  16.             NC2034.2
014300 01  DS-03V00-0002                PIC S999 VALUE  174.            NC2034.2
014400 01  CS-05V00-0003                PIC S9(5) COMP  VALUE 10.       NC2034.2
014500 01    TA--X           PIC 9(5)  COMP VALUE ZERO.                 NC2034.2
014600 01  MINUS-NAMES.                                                 NC2034.2
014700     02  WHOLE-FIELD              PICTURE S9(18).                 NC2034.2
014800     02  PLUS-NAME1  PICTURE S9(18) VALUE +333333333333333333.    NC2034.2
014900     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC2034.2
015000     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC2034.2
015100     02  ALPHA-LIT                PICTURE X(5)  VALUE SPACE.      NC2034.2
015200     02  SNEG-LIT2                PICTURE S9(5)  VALUE -70718.    NC2034.2
015300 01  TEST-RESULTS.                                                NC2034.2
015400     02 FILLER                   PIC X      VALUE SPACE.          NC2034.2
015500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2034.2
015600     02 FILLER                   PIC X      VALUE SPACE.          NC2034.2
015700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2034.2
015800     02 FILLER                   PIC X      VALUE SPACE.          NC2034.2
015900     02  PAR-NAME.                                                NC2034.2
016000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2034.2
016100       03  PARDOT-X              PIC X      VALUE SPACE.          NC2034.2
016200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2034.2
016300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2034.2
016400     02 RE-MARK                  PIC X(61).                       NC2034.2
016500 01  TEST-COMPUTED.                                               NC2034.2
016600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2034.2
016700     02 FILLER                   PIC X(17)  VALUE                 NC2034.2
016800            "       COMPUTED=".                                   NC2034.2
016900     02 COMPUTED-X.                                               NC2034.2
017000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2034.2
017100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2034.2
017200                                 PIC -9(9).9(9).                  NC2034.2
017300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2034.2
017400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2034.2
017500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2034.2
017600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2034.2
017700         04 COMPUTED-18V0                    PIC -9(18).          NC2034.2
017800         04 FILLER                           PIC X.               NC2034.2
017900     03 FILLER PIC X(50) VALUE SPACE.                             NC2034.2
018000 01  TEST-CORRECT.                                                NC2034.2
018100     02 FILLER PIC X(30) VALUE SPACE.                             NC2034.2
018200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2034.2
018300     02 CORRECT-X.                                                NC2034.2
018400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2034.2
018500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2034.2
018600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2034.2
018700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2034.2
018800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2034.2
018900     03      CR-18V0 REDEFINES CORRECT-A.                         NC2034.2
019000         04 CORRECT-18V0                     PIC -9(18).          NC2034.2
019100         04 FILLER                           PIC X.               NC2034.2
019200     03 FILLER PIC X(2) VALUE SPACE.                              NC2034.2
019300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2034.2
019400 01  CCVS-C-1.                                                    NC2034.2
019500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2034.2
019600-    "SS  PARAGRAPH-NAME                                          NC2034.2
019700-    "       REMARKS".                                            NC2034.2
019800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2034.2
019900 01  CCVS-C-2.                                                    NC2034.2
020000     02 FILLER                     PIC X        VALUE SPACE.      NC2034.2
020100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2034.2
020200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2034.2
020300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2034.2
020400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2034.2
020500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2034.2
020600 01  REC-CT                        PIC 99       VALUE ZERO.       NC2034.2
020700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2034.2
020800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2034.2
020900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2034.2
021000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2034.2
021100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2034.2
021200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2034.2
021300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2034.2
021400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2034.2
021500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2034.2
021600 01  CCVS-H-1.                                                    NC2034.2
021700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2034.2
021800     02  FILLER                    PIC X(42)    VALUE             NC2034.2
021900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2034.2
022000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2034.2
022100 01  CCVS-H-2A.                                                   NC2034.2
022200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2034.2
022300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2034.2
022400   02  FILLER                        PIC XXXX   VALUE             NC2034.2
022500     "4.2 ".                                                      NC2034.2
022600   02  FILLER                        PIC X(28)  VALUE             NC2034.2
022700            " COPY - NOT FOR DISTRIBUTION".                       NC2034.2
022800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2034.2
022900                                                                  NC2034.2
023000 01  CCVS-H-2B.                                                   NC2034.2
023100   02  FILLER                        PIC X(15)  VALUE             NC2034.2
023200            "TEST RESULT OF ".                                    NC2034.2
023300   02  TEST-ID                       PIC X(9).                    NC2034.2
023400   02  FILLER                        PIC X(4)   VALUE             NC2034.2
023500            " IN ".                                               NC2034.2
023600   02  FILLER                        PIC X(12)  VALUE             NC2034.2
023700     " HIGH       ".                                              NC2034.2
023800   02  FILLER                        PIC X(22)  VALUE             NC2034.2
023900            " LEVEL VALIDATION FOR ".                             NC2034.2
024000   02  FILLER                        PIC X(58)  VALUE             NC2034.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2034.2
024200 01  CCVS-H-3.                                                    NC2034.2
024300     02  FILLER                      PIC X(34)  VALUE             NC2034.2
024400            " FOR OFFICIAL USE ONLY    ".                         NC2034.2
024500     02  FILLER                      PIC X(58)  VALUE             NC2034.2
024600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2034.2
024700     02  FILLER                      PIC X(28)  VALUE             NC2034.2
024800            "  COPYRIGHT   1985 ".                                NC2034.2
024900 01  CCVS-E-1.                                                    NC2034.2
025000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2034.2
025100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2034.2
025200     02 ID-AGAIN                     PIC X(9).                    NC2034.2
025300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2034.2
025400 01  CCVS-E-2.                                                    NC2034.2
025500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2034.2
025600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2034.2
025700     02 CCVS-E-2-2.                                               NC2034.2
025800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2034.2
025900         03 FILLER                   PIC X      VALUE SPACE.      NC2034.2
026000         03 ENDER-DESC               PIC X(44)  VALUE             NC2034.2
026100            "ERRORS ENCOUNTERED".                                 NC2034.2
026200 01  CCVS-E-3.                                                    NC2034.2
026300     02  FILLER                      PIC X(22)  VALUE             NC2034.2
026400            " FOR OFFICIAL USE ONLY".                             NC2034.2
026500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2034.2
026600     02  FILLER                      PIC X(58)  VALUE             NC2034.2
026700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2034.2
026800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2034.2
026900     02 FILLER                       PIC X(15)  VALUE             NC2034.2
027000             " COPYRIGHT 1985".                                   NC2034.2
027100 01  CCVS-E-4.                                                    NC2034.2
027200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2034.2
027300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2034.2
027400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2034.2
027500     02 FILLER                       PIC X(40)  VALUE             NC2034.2
027600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2034.2
027700 01  XXINFO.                                                      NC2034.2
027800     02 FILLER                       PIC X(19)  VALUE             NC2034.2
027900            "*** INFORMATION ***".                                NC2034.2
028000     02 INFO-TEXT.                                                NC2034.2
028100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2034.2
028200       04 XXCOMPUTED                 PIC X(20).                   NC2034.2
028300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2034.2
028400       04 XXCORRECT                  PIC X(20).                   NC2034.2
028500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2034.2
028600 01  HYPHEN-LINE.                                                 NC2034.2
028700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2034.2
028800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2034.2
028900-    "*****************************************".                 NC2034.2
029000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2034.2
029100-    "******************************".                            NC2034.2
029200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2034.2
029300     "NC203A".                                                    NC2034.2
029400 PROCEDURE DIVISION.                                              NC2034.2
029500 CCVS1 SECTION.                                                   NC2034.2
029600 OPEN-FILES.                                                      NC2034.2
029700     OPEN     OUTPUT PRINT-FILE.                                  NC2034.2
029800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2034.2
029900     MOVE    SPACE TO TEST-RESULTS.                               NC2034.2
030000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2034.2
030100     GO TO CCVS1-EXIT.                                            NC2034.2
030200 CLOSE-FILES.                                                     NC2034.2
030300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2034.2
030400 TERMINATE-CCVS.                                                  NC2034.2
030500*    EXIT PROGRAM.                                                NC2034.2
030600*TERMINATE-CALL.                                                  NC2034.2
030700     STOP     RUN.                                                NC2034.2
030800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2034.2
030900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2034.2
031000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2034.2
031100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2034.2
031200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2034.2
031300 PRINT-DETAIL.                                                    NC2034.2
031400     IF REC-CT NOT EQUAL TO ZERO                                  NC2034.2
031500             MOVE "." TO PARDOT-X                                 NC2034.2
031600             MOVE REC-CT TO DOTVALUE.                             NC2034.2
031700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2034.2
031800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2034.2
031900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2034.2
032000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2034.2
032100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2034.2
032200     MOVE SPACE TO CORRECT-X.                                     NC2034.2
032300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2034.2
032400     MOVE     SPACE TO RE-MARK.                                   NC2034.2
032500 HEAD-ROUTINE.                                                    NC2034.2
032600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2034.2
032700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2034.2
032800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2034.2
032900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2034.2
033000 COLUMN-NAMES-ROUTINE.                                            NC2034.2
033100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2034.2
033200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2034.2
033300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2034.2
033400 END-ROUTINE.                                                     NC2034.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2034.2
033600 END-RTN-EXIT.                                                    NC2034.2
033700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2034.2
033800 END-ROUTINE-1.                                                   NC2034.2
033900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2034.2
034000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2034.2
034100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2034.2
034200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2034.2
034300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2034.2
034400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2034.2
034500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2034.2
034600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2034.2
034700  END-ROUTINE-12.                                                 NC2034.2
034800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2034.2
034900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2034.2
035000         MOVE "NO " TO ERROR-TOTAL                                NC2034.2
035100         ELSE                                                     NC2034.2
035200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2034.2
035300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2034.2
035400     PERFORM WRITE-LINE.                                          NC2034.2
035500 END-ROUTINE-13.                                                  NC2034.2
035600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2034.2
035700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2034.2
035800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2034.2
035900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2034.2
036000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2034.2
036100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2034.2
036200          MOVE "NO " TO ERROR-TOTAL                               NC2034.2
036300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2034.2
036400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2034.2
036500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2034.2
036600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2034.2
036700 WRITE-LINE.                                                      NC2034.2
036800     ADD 1 TO RECORD-COUNT.                                       NC2034.2
036900*    IF RECORD-COUNT GREATER 50                                   NC2034.2
037000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2034.2
037100*        MOVE SPACE TO DUMMY-RECORD                               NC2034.2
037200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2034.2
037300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2034.2
037400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2034.2
037500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2034.2
037600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2034.2
037700*        MOVE ZERO TO RECORD-COUNT.                               NC2034.2
037800     PERFORM WRT-LN.                                              NC2034.2
037900 WRT-LN.                                                          NC2034.2
038000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2034.2
038100     MOVE SPACE TO DUMMY-RECORD.                                  NC2034.2
038200 BLANK-LINE-PRINT.                                                NC2034.2
038300     PERFORM WRT-LN.                                              NC2034.2
038400 FAIL-ROUTINE.                                                    NC2034.2
038500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2034.2
038600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2034.2
038700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2034.2
038800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2034.2
038900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2034.2
039000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2034.2
039100     GO TO  FAIL-ROUTINE-EX.                                      NC2034.2
039200 FAIL-ROUTINE-WRITE.                                              NC2034.2
039300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2034.2
039400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2034.2
039500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2034.2
039600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2034.2
039700 FAIL-ROUTINE-EX. EXIT.                                           NC2034.2
039800 BAIL-OUT.                                                        NC2034.2
039900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2034.2
040000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2034.2
040100 BAIL-OUT-WRITE.                                                  NC2034.2
040200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2034.2
040300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2034.2
040400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2034.2
040500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2034.2
040600 BAIL-OUT-EX. EXIT.                                               NC2034.2
040700 CCVS1-EXIT.                                                      NC2034.2
040800     EXIT.                                                        NC2034.2
040900 SECT-NC203A-001 SECTION.                                         NC2034.2
041000 DIV-INIT-F4-1.                                                   NC2034.2
041100     MOVE   "DIV-TEST-F4-1" TO PAR-NAME.                          NC2034.2
041200     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
041300     MOVE   "DIVIDE" TO FEATURE.                                  NC2034.2
041400     MOVE    111111.0 TO WRK-DS-06V06.                            NC2034.2
041500 DIV-TEST-F4-1.                                                   NC2034.2
041600     DIVIDE  22 INTO WRK-DS-06V06 GIVING WRK-DS-05V00             NC2034.2
041700             REMAINDER WRK-DS-02V00.                              NC2034.2
041800     ADD     WRK-DS-02V00 TO WRK-DS-05V00.                        NC2034.2
041900     IF      WRK-DS-05V00 EQUAL TO 5061                           NC2034.2
042000              PERFORM PASS                                        NC2034.2
042100              GO TO DIV-WRITE-F4-1.                               NC2034.2
042200     GO TO DIV-FAIL-F4-1.                                         NC2034.2
042300 DIV-DELETE-F4-1.                                                 NC2034.2
042400     PERFORM DE-LETE.                                             NC2034.2
042500     GO TO DIV-WRITE-F4-1.                                        NC2034.2
042600 DIV-FAIL-F4-1.                                                   NC2034.2
042700     MOVE     WRK-DS-05V00 TO COMPUTED-N.                         NC2034.2
042800     MOVE     5061 TO CORRECT-N.                                  NC2034.2
042900     PERFORM  FAIL.                                               NC2034.2
043000 DIV-WRITE-F4-1.                                                  NC2034.2
043100     PERFORM  PRINT-DETAIL.                                       NC2034.2
043200*                                                                 NC2034.2
043300 DIV-INIT-F4-2.                                                   NC2034.2
043400     MOVE "DIV-TEST-F4-2" TO PAR-NAME.                            NC2034.2
043500     MOVE 105  TO DIV11.                                          NC2034.2
043600     MOVE 1000 TO DIV12.                                          NC2034.2
043700 DIV-TEST-F4-2.                                                   NC2034.2
043800     DIVIDE DIV11 INTO DIV12 GIVING DIV13 REMAINDER DIV14.        NC2034.2
043900     IF DIV14 IS EQUAL TO 55                                      NC2034.2
044000              PERFORM PASS                                        NC2034.2
044100              GO TO DIV-WRITE-F4-2.                               NC2034.2
044200     GO TO DIV-FAIL-F4-2.                                         NC2034.2
044300 DIV-DELETE-F4-2.                                                 NC2034.2
044400     PERFORM DE-LETE.                                             NC2034.2
044500     GO TO DIV-WRITE-F4-2.                                        NC2034.2
044600 DIV-FAIL-F4-2.                                                   NC2034.2
044700     PERFORM FAIL.                                                NC2034.2
044800     MOVE DIV14 TO COMPUTED-N.                                    NC2034.2
044900     MOVE "+55" TO CORRECT-A.                                     NC2034.2
045000 DIV-WRITE-F4-2.                                                  NC2034.2
045100     PERFORM PRINT-DETAIL.                                        NC2034.2
045200*                                                                 NC2034.2
045300 DIV-INIT-F4-3.                                                   NC2034.2
045400     MOVE "DIV-TEST-F4-3" TO PAR-NAME.                            NC2034.2
045500     MOVE   14 TO DIV19.                                          NC2034.2
045600     MOVE 2147 TO DIV20.                                          NC2034.2
045700 DIV-TEST-F4-3.                                                   NC2034.2
045800     DIVIDE DIV19 INTO DIV20 GIVING DIV21 ROUNDED REMAINDER       NC2034.2
045900     DIV22.                                                       NC2034.2
046000     IF DIV22 IS EQUAL TO 05                                      NC2034.2
046100              PERFORM PASS                                        NC2034.2
046200              GO TO DIV-WRITE-F4-3.                               NC2034.2
046300     GO TO DIV-FAIL-F4-3.                                         NC2034.2
046400 DIV-DELETE-F4-3.                                                 NC2034.2
046500     PERFORM DE-LETE.                                             NC2034.2
046600     GO TO DIV-WRITE-F4-3.                                        NC2034.2
046700 DIV-FAIL-F4-3.                                                   NC2034.2
046800     PERFORM FAIL.                                                NC2034.2
046900     MOVE DIV22 TO COMPUTED-N.                                    NC2034.2
047000     MOVE "+05" TO CORRECT-A.                                     NC2034.2
047100 DIV-WRITE-F4-3.                                                  NC2034.2
047200     PERFORM PRINT-DETAIL.                                        NC2034.2
047300*                                                                 NC2034.2
047400 DIV-INIT-F4-4.                                                   NC2034.2
047500     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
047600     MOVE    ZERO TO 25COUNT.                                     NC2034.2
047700     MOVE    ZERO TO 25ANS.                                       NC2034.2
047800     MOVE    ZERO TO 25REM.                                       NC2034.2
047900     MOVE    1    TO REC-CT.                                      NC2034.2
048000 DIV-INIT-F4-4-0.                                                 NC2034.2
048100     MOVE   "DIV-TEST-F4-4-0"  TO PAR-NAME.                       NC2034.2
048200 DIV-TEST-F4-4-0.                                                 NC2034.2
048300     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
048400             ON SIZE ERROR                                        NC2034.2
048500             PERFORM PASS                                         NC2034.2
048600             GO TO DIV-WRITE-F4-4-0.                              NC2034.2
048700     GO TO DIV-FAIL-F4-4-0.                                       NC2034.2
048800 DIV-FAIL-F4-4-0.                                                 NC2034.2
048900     MOVE   "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK.         NC2034.2
049000     PERFORM FAIL.                                                NC2034.2
049100 DIV-WRITE-F4-4-0.                                                NC2034.2
049200     PERFORM PRINT-DETAIL.                                        NC2034.2
049300*                                                                 NC2034.2
049400 DIV-INIT-F4-4-1.                                                 NC2034.2
049500     MOVE   "DIV-TEST-F4-4-1" TO PAR-NAME.                        NC2034.2
049600     ADD     1 TO REC-CT.                                         NC2034.2
049700 DIV-TEST-F4-4-1.                                                 NC2034.2
049800     IF      25ANS NOT = ZERO                                     NC2034.2
049900             GO TO DIV-FAIL-F4-4-1.                               NC2034.2
050000     PERFORM PASS                                                 NC2034.2
050100     GO TO DIV-WRITE-F4-4-1.                                      NC2034.2
050200 DIV-DELETE-F4-4-1.                                               NC2034.2
050300     PERFORM DE-LETE.                                             NC2034.2
050400     GO TO DIV-WRITE-F4-4-1.                                      NC2034.2
050500 DIV-FAIL-F4-4-1.                                                 NC2034.2
050600     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
050700     MOVE    ZERO TO CORRECT-N                                    NC2034.2
050800     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
050900     PERFORM FAIL.                                                NC2034.2
051000 DIV-WRITE-F4-4-1.                                                NC2034.2
051100     PERFORM PRINT-DETAIL.                                        NC2034.2
051200*                                                                 NC2034.2
051300 DIV-INIT-F4-4-2.                                                 NC2034.2
051400     MOVE   "DIV-TEST-F4-4-2" TO PAR-NAME.                        NC2034.2
051500     ADD     1 TO REC-CT.                                         NC2034.2
051600 DIV-TEST-F4-4-2.                                                 NC2034.2
051700     IF      25REM NOT = ZERO                                     NC2034.2
051800             GO TO DIV-FAIL-F4-4-2.                               NC2034.2
051900     PERFORM PASS                                                 NC2034.2
052000     GO TO DIV-WRITE-F4-4-2.                                      NC2034.2
052100 DIV-DELETE-F4-4-2.                                               NC2034.2
052200     PERFORM DE-LETE.                                             NC2034.2
052300     GO TO DIV-WRITE-F4-4-2.                                      NC2034.2
052400 DIV-FAIL-F4-4-2.                                                 NC2034.2
052500     MOVE    25REM TO COMPUTED-N                                  NC2034.2
052600     MOVE    ZERO TO CORRECT-N                                    NC2034.2
052700     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
052800     PERFORM FAIL.                                                NC2034.2
052900 DIV-WRITE-F4-4-2.                                                NC2034.2
053000     PERFORM PRINT-DETAIL.                                        NC2034.2
053100*                                                                 NC2034.2
053200 DIV-INIT-F4-5.                                                   NC2034.2
053300     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
053400     MOVE ZERO TO 25ANS.                                          NC2034.2
053500     MOVE ZERO TO 25REM.                                          NC2034.2
053600     MOVE 3    TO 25COUNT.                                        NC2034.2
053700     MOVE 1    TO REC-CT.                                         NC2034.2
053800 DIV-INIT-F4-5-0.                                                 NC2034.2
053900     MOVE   "DIV-TEST-F4-5-0"  TO PAR-NAME.                       NC2034.2
054000 DIV-TEST-F4-5-0.                                                 NC2034.2
054100     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
054200             ON SIZE ERROR                                        NC2034.2
054300             GO TO DIV-FAIL-F4-5-0.                               NC2034.2
054400     PERFORM PASS.                                                NC2034.2
054500     GO TO DIV-WRITE-F4-5-0.                                      NC2034.2
054600 DIV-DELETE-F4-5-0.                                               NC2034.2
054700     PERFORM DE-LETE.                                             NC2034.2
054800     GO TO DIV-WRITE-F4-5-0.                                      NC2034.2
054900 DIV-FAIL-F4-5-0.                                                 NC2034.2
055000     MOVE   "SIZE ERROR SHOULD NOT HAVE OCCURED"                  NC2034.2
055100          TO RE-MARK                                              NC2034.2
055200     PERFORM FAIL.                                                NC2034.2
055300 DIV-WRITE-F4-5-0.                                                NC2034.2
055400     PERFORM PRINT-DETAIL.                                        NC2034.2
055500*                                                                 NC2034.2
055600 DIV-INIT-F4-5-1.                                                 NC2034.2
055700     MOVE   "DIV-TEST-F4-5-1" TO PAR-NAME.                        NC2034.2
055800     ADD     1 TO REC-CT.                                         NC2034.2
055900 DIV-TEST-F4-5-1.                                                 NC2034.2
056000     IF      25ANS NOT = 33                                       NC2034.2
056100             GO TO DIV-FAIL-F4-5-1.                               NC2034.2
056200     PERFORM PASS                                                 NC2034.2
056300     GO TO DIV-WRITE-F4-5-1.                                      NC2034.2
056400 DIV-DELETE-F4-5-1.                                               NC2034.2
056500     PERFORM DE-LETE.                                             NC2034.2
056600     GO TO DIV-WRITE-F4-5-1.                                      NC2034.2
056700 DIV-FAIL-F4-5-1.                                                 NC2034.2
056800     MOVE    33 TO CORRECT-N                                      NC2034.2
056900     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
057000     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
057100     PERFORM FAIL.                                                NC2034.2
057200 DIV-WRITE-F4-5-1.                                                NC2034.2
057300     PERFORM PRINT-DETAIL.                                        NC2034.2
057400*                                                                 NC2034.2
057500 DIV-INIT-F4-5-2.                                                 NC2034.2
057600     MOVE   "DIV-TEST-F4-5-2" TO PAR-NAME.                        NC2034.2
057700     ADD     1 TO REC-CT.                                         NC2034.2
057800 DIV-TEST-F4-5-2.                                                 NC2034.2
057900     IF      25REM NOT = 1                                        NC2034.2
058000             GO TO DIV-FAIL-F4-5-2.                               NC2034.2
058100     PERFORM PASS                                                 NC2034.2
058200     GO TO DIV-WRITE-F4-5-2.                                      NC2034.2
058300 DIV-DELETE-F4-5-2.                                               NC2034.2
058400     PERFORM DE-LETE.                                             NC2034.2
058500     GO TO DIV-WRITE-F4-5-2.                                      NC2034.2
058600 DIV-FAIL-F4-5-2.                                                 NC2034.2
058700     MOVE    25REM TO COMPUTED-N                                  NC2034.2
058800     MOVE    1     TO CORRECT-N                                   NC2034.2
058900     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
059000     PERFORM FAIL.                                                NC2034.2
059100 DIV-WRITE-F4-5-2.                                                NC2034.2
059200     PERFORM PRINT-DETAIL.                                        NC2034.2
059300*                                                                 NC2034.2
059400 DIV-INIT-F4-6.                                                   NC2034.2
059500     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
059600     MOVE    40   TO 25COUNT.                                     NC2034.2
059700     MOVE    ZERO TO 25ANS.                                       NC2034.2
059800     MOVE    ZERO TO 25REM.                                       NC2034.2
059900     MOVE    1    TO REC-CT.                                      NC2034.2
060000 DIV-INIT-F4-6-0.                                                 NC2034.2
060100     MOVE   "DIV-TEST-F4-6-0"  TO PAR-NAME.                       NC2034.2
060200 DIV-TEST-F4-6-0.                                                 NC2034.2
060300     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
060400             ON SIZE ERROR                                        NC2034.2
060500             GO TO DIV-FAIL-F4-6-0.                               NC2034.2
060600     PERFORM PASS.                                                NC2034.2
060700     GO TO DIV-WRITE-F4-6-0.                                      NC2034.2
060800 DIV-DELETE-F4-6-0.                                               NC2034.2
060900     PERFORM DE-LETE.                                             NC2034.2
061000     GO TO DIV-WRITE-F4-6-0.                                      NC2034.2
061100 DIV-FAIL-F4-6-0.                                                 NC2034.2
061200     MOVE   "SIZE ERROR SHOULD NOT HAVE OCCURED"                  NC2034.2
061300          TO RE-MARK                                              NC2034.2
061400     PERFORM FAIL.                                                NC2034.2
061500 DIV-WRITE-F4-6-0.                                                NC2034.2
061600     PERFORM PRINT-DETAIL.                                        NC2034.2
061700*                                                                 NC2034.2
061800 DIV-INIT-F4-6-1.                                                 NC2034.2
061900     MOVE   "DIV-TEST-F4-6-1" TO PAR-NAME.                        NC2034.2
062000     ADD     1 TO REC-CT.                                         NC2034.2
062100 DIV-TEST-F4-6-1.                                                 NC2034.2
062200     IF      25ANS NOT = 2                                        NC2034.2
062300             GO TO DIV-FAIL-F4-6-1.                               NC2034.2
062400     PERFORM PASS                                                 NC2034.2
062500     GO TO DIV-WRITE-F4-6-1.                                      NC2034.2
062600 DIV-DELETE-F4-6-1.                                               NC2034.2
062700     PERFORM DE-LETE.                                             NC2034.2
062800     GO TO DIV-WRITE-F4-6-1.                                      NC2034.2
062900 DIV-FAIL-F4-6-1.                                                 NC2034.2
063000     MOVE    2 TO CORRECT-N                                       NC2034.2
063100     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
063200     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
063300     PERFORM FAIL.                                                NC2034.2
063400 DIV-WRITE-F4-6-1.                                                NC2034.2
063500     PERFORM PRINT-DETAIL.                                        NC2034.2
063600*                                                                 NC2034.2
063700 DIV-INIT-F4-6-2.                                                 NC2034.2
063800     MOVE   "DIV-TEST-F4-6-2" TO PAR-NAME.                        NC2034.2
063900 DIV-TEST-F4-6-2.                                                 NC2034.2
064000     ADD     1 TO REC-CT.                                         NC2034.2
064100     IF      25REM NOT = 20                                       NC2034.2
064200             GO TO DIV-FAIL-F4-6-2.                               NC2034.2
064300     PERFORM PASS                                                 NC2034.2
064400     GO TO DIV-WRITE-F4-6-2.                                      NC2034.2
064500 DIV-DELETE-F4-6-2.                                               NC2034.2
064600     PERFORM DE-LETE.                                             NC2034.2
064700     GO TO DIV-WRITE-F4-6-2.                                      NC2034.2
064800 DIV-FAIL-F4-6-2.                                                 NC2034.2
064900     MOVE    25REM TO COMPUTED-N                                  NC2034.2
065000     MOVE    20    TO CORRECT-N                                   NC2034.2
065100     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
065200     PERFORM FAIL.                                                NC2034.2
065300 DIV-WRITE-F4-6-2.                                                NC2034.2
065400     PERFORM PRINT-DETAIL.                                        NC2034.2
065500*                                                                 NC2034.2
065600 DIV-INIT-F4-7.                                                   NC2034.2
065700     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
065800     MOVE   "DIV-TEST-F4-7-0"  TO PAR-NAME.                       NC2034.2
065900     MOVE    16   TO DS-02V00-0001.                               NC2034.2
066000     MOVE    174  TO DS-03V00-0002.                               NC2034.2
066100     MOVE    ZERO TO WRK-DS-04V01-0005.                           NC2034.2
066200     MOVE    ZERO TO NE-0009.                                     NC2034.2
066300     MOVE    1    TO REC-CT.                                      NC2034.2
066400 DIV-TEST-F4-7-0.                                                 NC2034.2
066500     DIVIDE   DS-02V00-0001  INTO  DS-03V00-0002                  NC2034.2
066600              GIVING  WRK-DS-04V01-0005  REMAINDER  NE-0009.      NC2034.2
066700*                                                                 NC2034.2
066800*    REMAINDER RECEIVING FIELD DESCRIBED AS NUMERIC EDITED.       NC2034.2
066900*                 I1 = 16                                         NC2034.2
067000*                 I2 = 174                                        NC2034.2
067100*                                                                 NC2034.2
067200 DIV-INIT-F4-7-1.                                                 NC2034.2
067300     MOVE     "DIV-TEST-F4-7-1" TO  PAR-NAME.                     NC2034.2
067400 DIV-TEST-F4-7-1.                                                 NC2034.2
067500     IF      NE-0009  EQUAL TO "***01"                            NC2034.2
067600             PERFORM   PASS                                       NC2034.2
067700             GO TO DIV-WRITE-F4-7-1.                              NC2034.2
067800     GO TO DIV-FAIL-F4-7-1.                                       NC2034.2
067900 DIV-FAIL-F4-7-1.                                                 NC2034.2
068000     PERFORM  FAIL.                                               NC2034.2
068100     MOVE    "***01"  TO CORRECT-A.                               NC2034.2
068200     MOVE     NE-0009 TO COMPUTED-A.                              NC2034.2
068300 DIV-DELETE-F4-7-1.                                               NC2034.2
068400     PERFORM   DE-LETE.                                           NC2034.2
068500     GO TO    DIV-WRITE-F4-7-1.                                   NC2034.2
068600 DIV-WRITE-F4-7-1.                                                NC2034.2
068700     PERFORM  PRINT-DETAIL.                                       NC2034.2
068800*                                                                 NC2034.2
068900 DIV-INIT-F4-7-2.                                                 NC2034.2
069000     MOVE   "DIV-TEST-F4-7-2" TO PAR-NAME.                        NC2034.2
069100     ADD     1 TO REC-CT.                                         NC2034.2
069200 DIV-TEST-F4-7-2.                                                 NC2034.2
069300     IF      WRK-DS-04V01-0005 NOT = 10.8                         NC2034.2
069400             GO TO DIV-FAIL-F4-7-2.                               NC2034.2
069500     PERFORM PASS                                                 NC2034.2
069600     GO TO DIV-WRITE-F4-7-2.                                      NC2034.2
069700 DIV-DELETE-F4-7-2.                                               NC2034.2
069800     PERFORM   DE-LETE.                                           NC2034.2
069900     GO TO    DIV-WRITE-F4-7-2.                                   NC2034.2
070000 DIV-FAIL-F4-7-2.                                                 NC2034.2
070100     MOVE    WRK-DS-04V01-0005 TO COMPUTED-N                      NC2034.2
070200     MOVE    10.8  TO CORRECT-N                                   NC2034.2
070300     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
070400     PERFORM FAIL.                                                NC2034.2
070500 DIV-WRITE-F4-7-2.                                                NC2034.2
070600     PERFORM PRINT-DETAIL.                                        NC2034.2
070700*                                                                 NC2034.2
070800 DIV-INIT-F4-8.                                                   NC2034.2
070900     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
071000     MOVE    16   TO DS-02V00-0001.                               NC2034.2
071100     MOVE    174  TO DS-03V00-0002.                               NC2034.2
071200     MOVE    ZERO TO WRK-DS-04V01-0005.                           NC2034.2
071300     MOVE    ZERO TO NE-04V01-0006.                               NC2034.2
071400     MOVE    1    TO REC-CT.                                      NC2034.2
071500     MOVE   "DIV-TEST-F4-8-0"  TO PAR-NAME.                       NC2034.2
071600     MOVE   "DIVIDE"  TO FEATURE.                                 NC2034.2
071700*                                                                 NC2034.2
071800 DIV-TEST-F4-8-0.                                                 NC2034.2
071900     DIVIDE  DS-02V00-0001  INTO  DS-03V00-0002                   NC2034.2
072000             GIVING NE-04V01-0006    REMAINDER WRK-DS-05V00-0002. NC2034.2
072100*                                                                 NC2034.2
072200*    GIVING    RECEIVING FIELD DESCRIBED AS NUMERIC EDITED.       NC2034.2
072300*    INTERMEDIATE STORAGE SHOULD BE USED TO CALCULATE THE         NC2034.2
072400*    REMAINDER                                                    NC2034.2
072500*                 I1 = 16                                         NC2034.2
072600*                 I2 = 174                                        NC2034.2
072700*                                                                 NC2034.2
072800 DIV-INIT-F4-8-1.                                                 NC2034.2
072900     MOVE     "DIV-TEST-F4-8-1" TO  PAR-NAME.                     NC2034.2
073000 DIV-TEST-F4-8-1.                                                 NC2034.2
073100     IF      WRK-DS-05V00-0002  EQUAL TO 00001                    NC2034.2
073200             PERFORM   PASS                                       NC2034.2
073300             GO TO DIV-WRITE-F4-8-1.                              NC2034.2
073400     GO TO DIV-FAIL-F4-8-1.                                       NC2034.2
073500 DIV-DELETE-F4-8-1.                                               NC2034.2
073600     PERFORM   DE-LETE.                                           NC2034.2
073700     GO TO    DIV-WRITE-F4-8-1.                                   NC2034.2
073800 DIV-FAIL-F4-8-1.                                                 NC2034.2
073900     PERFORM  FAIL.                                               NC2034.2
074000     MOVE     00001     TO  CORRECT-A.                            NC2034.2
074100     MOVE   WRK-DS-05V00-0002  TO COMPUTED-A.                     NC2034.2
074200 DIV-WRITE-F4-8-1.                                                NC2034.2
074300     PERFORM  PRINT-DETAIL.                                       NC2034.2
074400*                                                                 NC2034.2
074500 DIV-INIT-F4-8-2.                                                 NC2034.2
074600     MOVE   "DIV-TEST-F4-8-2" TO PAR-NAME.                        NC2034.2
074700     ADD     1 TO REC-CT.                                         NC2034.2
074800 DIV-TEST-F4-8-2.                                                 NC2034.2
074900     IF      NE-04V01-0006 NOT = "**10.8"                         NC2034.2
075000             GO TO DIV-FAIL-F4-8-2.                               NC2034.2
075100     PERFORM PASS                                                 NC2034.2
075200     GO TO DIV-WRITE-F4-8-2.                                      NC2034.2
075300 DIV-DELETE-F4-8-2.                                               NC2034.2
075400     PERFORM   DE-LETE.                                           NC2034.2
075500     GO TO    DIV-WRITE-F4-8-2.                                   NC2034.2
075600 DIV-FAIL-F4-8-2.                                                 NC2034.2
075700     MOVE    NE-04V01-0006 TO COMPUTED-A                          NC2034.2
075800     MOVE   "**10.8" TO CORRECT-A                                 NC2034.2
075900     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
076000     PERFORM FAIL.                                                NC2034.2
076100 DIV-WRITE-F4-8-2.                                                NC2034.2
076200     PERFORM PRINT-DETAIL.                                        NC2034.2
076300*                                                                 NC2034.2
076400 DIV-INIT-F4-9.                                                   NC2034.2
076500     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
076600     MOVE    ZERO TO 25COUNT.                                     NC2034.2
076700     MOVE    ZERO TO 25ANS.                                       NC2034.2
076800     MOVE    ZERO TO 25REM.                                       NC2034.2
076900     MOVE    1    TO REC-CT.                                      NC2034.2
077000 DIV-INIT-F4-9-0.                                                 NC2034.2
077100     MOVE   "DIV-TEST-F4-9-0"  TO PAR-NAME.                       NC2034.2
077200 DIV-TEST-F4-9-0.                                                 NC2034.2
077300     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
077400         NOT ON SIZE ERROR                                        NC2034.2
077500         GO TO DIV-FAIL-F4-9-0.                                   NC2034.2
077600     PERFORM PASS.                                                NC2034.2
077700     GO TO DIV-WRITE-F4-9-0.                                      NC2034.2
077800 DIV-DELETE-F4-9-0.                                               NC2034.2
077900     PERFORM   DE-LETE.                                           NC2034.2
078000     GO TO    DIV-WRITE-F4-9-0.                                   NC2034.2
078100 DIV-FAIL-F4-9-0.                                                 NC2034.2
078200     MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"          NC2034.2
078300           TO RE-MARK                                             NC2034.2
078400     PERFORM FAIL.                                                NC2034.2
078500 DIV-WRITE-F4-9-0.                                                NC2034.2
078600     PERFORM PRINT-DETAIL.                                        NC2034.2
078700*                                                                 NC2034.2
078800 DIV-INIT-F4-9-1.                                                 NC2034.2
078900     MOVE   "DIV-TEST-F4-9-1" TO PAR-NAME.                        NC2034.2
079000     ADD     1 TO REC-CT.                                         NC2034.2
079100 DIV-TEST-F4-9-1.                                                 NC2034.2
079200     IF      25ANS NOT = ZERO                                     NC2034.2
079300             GO TO DIV-FAIL-F4-9-1.                               NC2034.2
079400     PERFORM PASS                                                 NC2034.2
079500     GO TO DIV-WRITE-F4-9-1.                                      NC2034.2
079600 DIV-DELETE-F4-9-1.                                               NC2034.2
079700     PERFORM   DE-LETE.                                           NC2034.2
079800     GO TO    DIV-WRITE-F4-9-1.                                   NC2034.2
079900 DIV-FAIL-F4-9-1.                                                 NC2034.2
080000     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
080100     MOVE    ZERO TO CORRECT-N                                    NC2034.2
080200     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
080300     PERFORM FAIL.                                                NC2034.2
080400 DIV-WRITE-F4-9-1.                                                NC2034.2
080500     PERFORM PRINT-DETAIL.                                        NC2034.2
080600*                                                                 NC2034.2
080700 DIV-INIT-F4-9-2.                                                 NC2034.2
080800     MOVE   "DIV-TEST-F4-9-2" TO PAR-NAME.                        NC2034.2
080900     ADD     1 TO REC-CT.                                         NC2034.2
081000 DIV-TEST-F4-9-2.                                                 NC2034.2
081100     IF      25REM NOT = ZERO                                     NC2034.2
081200             GO TO DIV-FAIL-F4-9-2.                               NC2034.2
081300     PERFORM PASS                                                 NC2034.2
081400     GO TO DIV-WRITE-F4-9-2.                                      NC2034.2
081500 DIV-DELETE-F4-9-2.                                               NC2034.2
081600     PERFORM   DE-LETE.                                           NC2034.2
081700     GO TO    DIV-WRITE-F4-9-2.                                   NC2034.2
081800 DIV-FAIL-F4-9-2.                                                 NC2034.2
081900     MOVE    25REM TO COMPUTED-N                                  NC2034.2
082000     MOVE    ZERO TO CORRECT-N                                    NC2034.2
082100     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
082200     PERFORM FAIL.                                                NC2034.2
082300 DIV-WRITE-F4-9-2.                                                NC2034.2
082400     PERFORM PRINT-DETAIL.                                        NC2034.2
082500*                                                                 NC2034.2
082600 DIV-INIT-F4-10.                                                  NC2034.2
082700     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
082800     MOVE ZERO TO 25ANS.                                          NC2034.2
082900     MOVE ZERO TO 25REM.                                          NC2034.2
083000     MOVE 3    TO 25COUNT.                                        NC2034.2
083100     MOVE 1    TO REC-CT.                                         NC2034.2
083200 DIV-INIT-F4-10-0.                                                NC2034.2
083300     MOVE   "DIV-TEST-F4-10-0"  TO PAR-NAME.                      NC2034.2
083400 DIV-TEST-F4-10-0.                                                NC2034.2
083500     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
083600         NOT ON SIZE ERROR                                        NC2034.2
083700             PERFORM PASS                                         NC2034.2
083800             GO TO DIV-WRITE-F4-10-0.                             NC2034.2
083900     GO TO   DIV-FAIL-F4-10-0.                                    NC2034.2
084000 DIV-DELETE-F4-10-0.                                              NC2034.2
084100     PERFORM DE-LETE.                                             NC2034.2
084200     GO TO DIV-WRITE-F4-10-0.                                     NC2034.2
084300 DIV-FAIL-F4-10-0.                                                NC2034.2
084400     MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED" TO RE-MARK.  NC2034.2
084500     PERFORM FAIL.                                                NC2034.2
084600 DIV-WRITE-F4-10-0.                                               NC2034.2
084700     PERFORM PRINT-DETAIL.                                        NC2034.2
084800*                                                                 NC2034.2
084900 DIV-INIT-F4-10-1.                                                NC2034.2
085000     MOVE   "DIV-TEST-F4-10-1" TO PAR-NAME.                       NC2034.2
085100     ADD     1 TO REC-CT.                                         NC2034.2
085200 DIV-TEST-F4-10-1.                                                NC2034.2
085300     IF      25ANS NOT = 33                                       NC2034.2
085400             GO TO DIV-FAIL-F4-10-1.                              NC2034.2
085500     PERFORM PASS                                                 NC2034.2
085600     GO TO DIV-WRITE-F4-10-1.                                     NC2034.2
085700 DIV-DELETE-F4-10-1.                                              NC2034.2
085800     PERFORM DE-LETE.                                             NC2034.2
085900     GO TO DIV-WRITE-F4-10-1.                                     NC2034.2
086000 DIV-FAIL-F4-10-1.                                                NC2034.2
086100     MOVE    33 TO CORRECT-N                                      NC2034.2
086200     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
086300     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
086400     PERFORM FAIL.                                                NC2034.2
086500 DIV-WRITE-F4-10-1.                                               NC2034.2
086600     PERFORM PRINT-DETAIL.                                        NC2034.2
086700*                                                                 NC2034.2
086800 DIV-INIT-F4-10-2.                                                NC2034.2
086900     MOVE   "DIV-TEST-F4-10-2" TO PAR-NAME.                       NC2034.2
087000     ADD     1 TO REC-CT.                                         NC2034.2
087100 DIV-TEST-F4-10-2.                                                NC2034.2
087200     IF      25REM NOT = 1                                        NC2034.2
087300             GO TO DIV-FAIL-F4-10-2.                              NC2034.2
087400     PERFORM PASS                                                 NC2034.2
087500     GO TO DIV-WRITE-F4-10-2.                                     NC2034.2
087600 DIV-DELETE-F4-10-2.                                              NC2034.2
087700     PERFORM DE-LETE.                                             NC2034.2
087800     GO TO DIV-WRITE-F4-10-2.                                     NC2034.2
087900 DIV-FAIL-F4-10-2.                                                NC2034.2
088000     MOVE    25REM TO COMPUTED-N                                  NC2034.2
088100     MOVE    1     TO CORRECT-N                                   NC2034.2
088200     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
088300     PERFORM FAIL.                                                NC2034.2
088400 DIV-WRITE-F4-10-2.                                               NC2034.2
088500     PERFORM PRINT-DETAIL.                                        NC2034.2
088600*                                                                 NC2034.2
088700 DIV-INIT-F4-11.                                                  NC2034.2
088800     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
088900     MOVE    ZERO TO 25COUNT.                                     NC2034.2
089000     MOVE    ZERO TO 25ANS.                                       NC2034.2
089100     MOVE    ZERO TO 25REM.                                       NC2034.2
089200     MOVE    1    TO REC-CT.                                      NC2034.2
089300 DIV-INIT-F4-11-0.                                                NC2034.2
089400     MOVE   "DIV-TEST-F4-11-0"  TO PAR-NAME.                      NC2034.2
089500 DIV-TEST-F4-11-0.                                                NC2034.2
089600     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
089700             ON SIZE ERROR                                        NC2034.2
089800             PERFORM PASS                                         NC2034.2
089900             GO TO   DIV-WRITE-F4-11-0                            NC2034.2
090000         NOT ON SIZE ERROR                                        NC2034.2
090100             GO TO DIV-FAIL-F4-11-0.                              NC2034.2
090200 DIV-DELETE-F4-11-0.                                              NC2034.2
090300     PERFORM DE-LETE.                                             NC2034.2
090400     GO TO DIV-WRITE-F4-11-0.                                     NC2034.2
090500 DIV-FAIL-F4-11-0.                                                NC2034.2
090600     MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"          NC2034.2
090700          TO RE-MARK                                              NC2034.2
090800     PERFORM FAIL.                                                NC2034.2
090900 DIV-WRITE-F4-11-0.                                               NC2034.2
091000     PERFORM PRINT-DETAIL.                                        NC2034.2
091100*                                                                 NC2034.2
091200 DIV-INIT-F4-11-1.                                                NC2034.2
091300     MOVE   "DIV-TEST-F4-11-1" TO PAR-NAME.                       NC2034.2
091400     ADD     1 TO REC-CT.                                         NC2034.2
091500 DIV-TEST-F4-11-1.                                                NC2034.2
091600     IF      25ANS NOT = ZERO                                     NC2034.2
091700             GO TO DIV-FAIL-F4-11-1.                              NC2034.2
091800     PERFORM PASS                                                 NC2034.2
091900     GO TO DIV-WRITE-F4-11-1.                                     NC2034.2
092000 DIV-DELETE-F4-11-1.                                              NC2034.2
092100     PERFORM DE-LETE.                                             NC2034.2
092200     GO TO DIV-WRITE-F4-11-1.                                     NC2034.2
092300 DIV-FAIL-F4-11-1.                                                NC2034.2
092400     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
092500     MOVE    ZERO TO CORRECT-N                                    NC2034.2
092600     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
092700     PERFORM FAIL.                                                NC2034.2
092800 DIV-WRITE-F4-11-1.                                               NC2034.2
092900     PERFORM PRINT-DETAIL.                                        NC2034.2
093000*                                                                 NC2034.2
093100 DIV-INIT-F4-11-2.                                                NC2034.2
093200     MOVE   "DIV-TEST-F4-11-2" TO PAR-NAME.                       NC2034.2
093300     ADD     1 TO REC-CT.                                         NC2034.2
093400 DIV-TEST-F4-11-2.                                                NC2034.2
093500     IF      25REM NOT = ZERO                                     NC2034.2
093600             GO TO DIV-FAIL-F4-11-2.                              NC2034.2
093700     PERFORM PASS                                                 NC2034.2
093800     GO TO DIV-WRITE-F4-11-2.                                     NC2034.2
093900 DIV-DELETE-F4-11-2.                                              NC2034.2
094000     PERFORM DE-LETE.                                             NC2034.2
094100     GO TO DIV-WRITE-F4-11-2.                                     NC2034.2
094200 DIV-FAIL-F4-11-2.                                                NC2034.2
094300     MOVE    25REM TO COMPUTED-N                                  NC2034.2
094400     MOVE    ZERO TO CORRECT-N                                    NC2034.2
094500     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
094600     PERFORM FAIL.                                                NC2034.2
094700 DIV-WRITE-F4-11-2.                                               NC2034.2
094800     PERFORM PRINT-DETAIL.                                        NC2034.2
094900*                                                                 NC2034.2
095000 DIV-INIT-F4-12.                                                  NC2034.2
095100     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
095200     MOVE ZERO TO 25ANS.                                          NC2034.2
095300     MOVE ZERO TO 25REM.                                          NC2034.2
095400     MOVE 3    TO 25COUNT.                                        NC2034.2
095500     MOVE 1    TO REC-CT.                                         NC2034.2
095600 DIV-INIT-F4-12-0.                                                NC2034.2
095700     MOVE   "DIV-TEST-F4-12-0"  TO PAR-NAME.                      NC2034.2
095800 DIV-TEST-F4-12-0.                                                NC2034.2
095900     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
096000             ON SIZE ERROR                                        NC2034.2
096100             GO TO DIV-FAIL-F4-12-0                               NC2034.2
096200         NOT ON SIZE ERROR                                        NC2034.2
096300             PERFORM PASS                                         NC2034.2
096400             GO TO DIV-WRITE-F4-12-0.                             NC2034.2
096500 DIV-DELETE-F4-12-0.                                              NC2034.2
096600     PERFORM DE-LETE.                                             NC2034.2
096700     GO TO DIV-WRITE-F4-12-0.                                     NC2034.2
096800 DIV-FAIL-F4-12-0.                                                NC2034.2
096900     MOVE   "ON SIZE ERROR SHOULD NOT HAVE EXECUTED"              NC2034.2
097000          TO RE-MARK                                              NC2034.2
097100     PERFORM FAIL.                                                NC2034.2
097200 DIV-WRITE-F4-12-0.                                               NC2034.2
097300     PERFORM PRINT-DETAIL.                                        NC2034.2
097400*                                                                 NC2034.2
097500 DIV-INIT-F4-12-1.                                                NC2034.2
097600     MOVE   "DIV-TEST-F4-12-1" TO PAR-NAME.                       NC2034.2
097700     ADD     1 TO REC-CT.                                         NC2034.2
097800 DIV-TEST-F4-12-1.                                                NC2034.2
097900     IF      25ANS NOT = 33                                       NC2034.2
098000             GO TO DIV-FAIL-F4-12-1.                              NC2034.2
098100     PERFORM PASS                                                 NC2034.2
098200     GO TO DIV-WRITE-F4-12-1.                                     NC2034.2
098300 DIV-DELETE-F4-12-1.                                              NC2034.2
098400     PERFORM DE-LETE.                                             NC2034.2
098500     GO TO DIV-WRITE-F4-12-1.                                     NC2034.2
098600 DIV-FAIL-F4-12-1.                                                NC2034.2
098700     MOVE    33 TO CORRECT-N                                      NC2034.2
098800     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
098900     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
099000     PERFORM FAIL.                                                NC2034.2
099100 DIV-WRITE-F4-12-1.                                               NC2034.2
099200     PERFORM PRINT-DETAIL.                                        NC2034.2
099300*                                                                 NC2034.2
099400 DIV-INIT-F4-12-2.                                                NC2034.2
099500     MOVE   "DIV-TEST-F4-12-2" TO PAR-NAME.                       NC2034.2
099600     ADD     1 TO REC-CT.                                         NC2034.2
099700 DIV-TEST-F4-12-2.                                                NC2034.2
099800     IF      25REM NOT = 1                                        NC2034.2
099900             GO TO DIV-FAIL-F4-12-2.                              NC2034.2
100000     PERFORM PASS                                                 NC2034.2
100100     GO TO DIV-WRITE-F4-12-2.                                     NC2034.2
100200 DIV-DELETE-F4-12-2.                                              NC2034.2
100300     PERFORM DE-LETE.                                             NC2034.2
100400     GO TO DIV-WRITE-F4-12-2.                                     NC2034.2
100500 DIV-FAIL-F4-12-2.                                                NC2034.2
100600     MOVE    25REM TO COMPUTED-N                                  NC2034.2
100700     MOVE    1     TO CORRECT-N                                   NC2034.2
100800     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
100900     PERFORM FAIL.                                                NC2034.2
101000 DIV-WRITE-F4-12-2.                                               NC2034.2
101100     PERFORM PRINT-DETAIL.                                        NC2034.2
101200*                                                                 NC2034.2
101300 DIV-INIT-F4-13.                                                  NC2034.2
101400     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
101500     MOVE    ZERO TO 25COUNT.                                     NC2034.2
101600     MOVE    ZERO TO 25ANS.                                       NC2034.2
101700     MOVE    ZERO TO 25REM.                                       NC2034.2
101800     MOVE    1    TO REC-CT.                                      NC2034.2
101900     MOVE   "DIV-TEST-F4-13-0"  TO PAR-NAME.                      NC2034.2
102000 DIV-TEST-F4-13-0.                                                NC2034.2
102100     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
102200             ON SIZE ERROR                                        NC2034.2
102300             MOVE    "A" TO WRK-XN-00001-1                        NC2034.2
102400     END-DIVIDE                                                   NC2034.2
102500     MOVE   "B" TO WRK-XN-00001-2.                                NC2034.2
102600*                                                                 NC2034.2
102700 DIV-INIT-F4-13-1.                                                NC2034.2
102800     MOVE   "DIV-TEST-F4-13-1" TO PAR-NAME.                       NC2034.2
102900 DIV-TEST-F4-13-1.                                                NC2034.2
103000     IF      25ANS NOT = ZERO                                     NC2034.2
103100             GO TO DIV-FAIL-F4-13-1.                              NC2034.2
103200     PERFORM PASS                                                 NC2034.2
103300     GO TO DIV-WRITE-F4-13-1.                                     NC2034.2
103400 DIV-DELETE-F4-13-1.                                              NC2034.2
103500     PERFORM DE-LETE.                                             NC2034.2
103600     GO TO DIV-WRITE-F4-13-1.                                     NC2034.2
103700 DIV-FAIL-F4-13-1.                                                NC2034.2
103800     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
103900     MOVE    ZERO TO CORRECT-N                                    NC2034.2
104000     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
104100     PERFORM FAIL.                                                NC2034.2
104200 DIV-WRITE-F4-13-1.                                               NC2034.2
104300     PERFORM PRINT-DETAIL.                                        NC2034.2
104400*                                                                 NC2034.2
104500 DIV-INIT-F4-13-2.                                                NC2034.2
104600     MOVE   "DIV-TEST-F4-13-2" TO PAR-NAME.                       NC2034.2
104700     ADD     1 TO REC-CT.                                         NC2034.2
104800 DIV-TEST-F4-13-2.                                                NC2034.2
104900     IF      25REM NOT = ZERO                                     NC2034.2
105000             GO TO DIV-FAIL-F4-13-2.                              NC2034.2
105100     PERFORM PASS                                                 NC2034.2
105200     GO TO DIV-WRITE-F4-13-2.                                     NC2034.2
105300 DIV-DELETE-F4-13-2.                                              NC2034.2
105400     PERFORM DE-LETE.                                             NC2034.2
105500     GO TO DIV-WRITE-F4-13-2.                                     NC2034.2
105600 DIV-FAIL-F4-13-2.                                                NC2034.2
105700     MOVE    25REM TO COMPUTED-N                                  NC2034.2
105800     MOVE    ZERO TO CORRECT-N                                    NC2034.2
105900     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
106000     PERFORM FAIL.                                                NC2034.2
106100 DIV-WRITE-F4-13-2.                                               NC2034.2
106200     PERFORM PRINT-DETAIL.                                        NC2034.2
106300*                                                                 NC2034.2
106400 DIV-INIT-F4-13-3.                                                NC2034.2
106500     MOVE   "DIV-TEST-F4-13-3" TO PAR-NAME.                       NC2034.2
106600     ADD     1 TO REC-CT.                                         NC2034.2
106700 DIV-TEST-F4-13-3.                                                NC2034.2
106800     IF      WRK-XN-00001-1 NOT = "A"                             NC2034.2
106900             GO TO DIV-FAIL-F4-13-3.                              NC2034.2
107000     PERFORM PASS                                                 NC2034.2
107100     GO TO DIV-WRITE-F4-13-3.                                     NC2034.2
107200 DIV-DELETE-F4-13-3.                                              NC2034.2
107300     PERFORM DE-LETE.                                             NC2034.2
107400     GO TO DIV-WRITE-F4-13-3.                                     NC2034.2
107500 DIV-FAIL-F4-13-3.                                                NC2034.2
107600     MOVE   "ON SIZE ERROR SHOULD HAVE EXECUTED"                  NC2034.2
107700          TO RE-MARK                                              NC2034.2
107800     MOVE   "A"    TO CORRECT-A                                   NC2034.2
107900     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2034.2
108000     PERFORM FAIL.                                                NC2034.2
108100 DIV-WRITE-F4-13-3.                                               NC2034.2
108200     PERFORM PRINT-DETAIL.                                        NC2034.2
108300*                                                                 NC2034.2
108400 DIV-INIT-F4-13-4.                                                NC2034.2
108500     MOVE   "DIV-TEST-F4-13-4" TO PAR-NAME.                       NC2034.2
108600     ADD     1 TO REC-CT.                                         NC2034.2
108700 DIV-TEST-F4-13-4.                                                NC2034.2
108800     IF      WRK-XN-00001-2 NOT = "B"                             NC2034.2
108900             GO TO DIV-FAIL-F4-13-4.                              NC2034.2
109000     PERFORM PASS                                                 NC2034.2
109100     GO TO DIV-WRITE-F4-13-4.                                     NC2034.2
109200 DIV-DELETE-F4-13-4.                                              NC2034.2
109300     PERFORM DE-LETE.                                             NC2034.2
109400     GO TO DIV-WRITE-F4-13-4.                                     NC2034.2
109500 DIV-FAIL-F4-13-4.                                                NC2034.2
109600     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2034.2
109700     MOVE   "B" TO CORRECT-A                                      NC2034.2
109800     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2034.2
109900     PERFORM FAIL.                                                NC2034.2
110000 DIV-WRITE-F4-13-4.                                               NC2034.2
110100     PERFORM PRINT-DETAIL.                                        NC2034.2
110200*                                                                 NC2034.2
110300 DIV-INIT-F4-14.                                                  NC2034.2
110400     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
110500     MOVE   "DIV-TEST-F4-14-0"  TO PAR-NAME.                      NC2034.2
110600     MOVE ZERO TO 25ANS.                                          NC2034.2
110700     MOVE ZERO TO 25REM.                                          NC2034.2
110800     MOVE SPACE TO WRK-XN-00001-1.                                NC2034.2
110900     MOVE SPACE TO WRK-XN-00001-2.                                NC2034.2
111000     MOVE 3    TO 25COUNT.                                        NC2034.2
111100     MOVE 1    TO REC-CT.                                         NC2034.2
111200 DIV-TEST-F4-14-0.                                                NC2034.2
111300     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
111400             ON SIZE ERROR                                        NC2034.2
111500             MOVE    "A" TO WRK-XN-00001-1                        NC2034.2
111600     END-DIVIDE                                                   NC2034.2
111700     MOVE   "B" TO WRK-XN-00001-2.                                NC2034.2
111800*                                                                 NC2034.2
111900 DIV-INIT-F4-14-1.                                                NC2034.2
112000     MOVE   "DIV-TEST-F4-14-1" TO PAR-NAME.                       NC2034.2
112100 DIV-TEST-F4-14-1.                                                NC2034.2
112200     IF      25ANS NOT = 33                                       NC2034.2
112300             GO TO DIV-FAIL-F4-14-1.                              NC2034.2
112400     PERFORM PASS                                                 NC2034.2
112500     GO TO DIV-WRITE-F4-14-1.                                     NC2034.2
112600 DIV-DELETE-F4-14-1.                                              NC2034.2
112700     PERFORM DE-LETE.                                             NC2034.2
112800     GO TO DIV-WRITE-F4-14-1.                                     NC2034.2
112900 DIV-FAIL-F4-14-1.                                                NC2034.2
113000     MOVE    33 TO CORRECT-N                                      NC2034.2
113100     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
113200     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
113300     PERFORM FAIL.                                                NC2034.2
113400 DIV-WRITE-F4-14-1.                                               NC2034.2
113500     PERFORM PRINT-DETAIL.                                        NC2034.2
113600*                                                                 NC2034.2
113700 DIV-INIT-F4-14-2.                                                NC2034.2
113800     MOVE   "DIV-TEST-F4-14-2" TO PAR-NAME.                       NC2034.2
113900     ADD     1 TO REC-CT.                                         NC2034.2
114000 DIV-TEST-F4-14-2.                                                NC2034.2
114100     IF      25REM NOT = 1                                        NC2034.2
114200             GO TO DIV-FAIL-F4-14-2.                              NC2034.2
114300     PERFORM PASS                                                 NC2034.2
114400     GO TO DIV-WRITE-F4-14-2.                                     NC2034.2
114500 DIV-DELETE-F4-14-2.                                              NC2034.2
114600     PERFORM DE-LETE.                                             NC2034.2
114700     GO TO DIV-WRITE-F4-14-2.                                     NC2034.2
114800 DIV-FAIL-F4-14-2.                                                NC2034.2
114900     MOVE    25REM TO COMPUTED-N                                  NC2034.2
115000     MOVE    1     TO CORRECT-N                                   NC2034.2
115100     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
115200     PERFORM FAIL.                                                NC2034.2
115300 DIV-WRITE-F4-14-2.                                               NC2034.2
115400     PERFORM PRINT-DETAIL.                                        NC2034.2
115500*                                                                 NC2034.2
115600 DIV-INIT-F4-14-3.                                                NC2034.2
115700     MOVE   "DIV-TEST-F4-14-3" TO PAR-NAME.                       NC2034.2
115800     ADD     1 TO REC-CT.                                         NC2034.2
115900 DIV-TEST-F4-14-3.                                                NC2034.2
116000     IF      WRK-XN-00001-1 NOT = SPACE                           NC2034.2
116100             GO TO DIV-FAIL-F4-14-3.                              NC2034.2
116200     PERFORM PASS                                                 NC2034.2
116300     GO TO DIV-WRITE-F4-14-3.                                     NC2034.2
116400 DIV-DELETE-F4-14-3.                                              NC2034.2
116500     PERFORM DE-LETE.                                             NC2034.2
116600     GO TO DIV-WRITE-F4-14-3.                                     NC2034.2
116700 DIV-FAIL-F4-14-3.                                                NC2034.2
116800     MOVE   "ON SIZE ERROR SHOULD NOT HAVE EXECUTED"              NC2034.2
116900          TO RE-MARK.                                             NC2034.2
117000     MOVE    SPACE TO CORRECT-A                                   NC2034.2
117100     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2034.2
117200     PERFORM FAIL.                                                NC2034.2
117300 DIV-WRITE-F4-14-3.                                               NC2034.2
117400     PERFORM PRINT-DETAIL.                                        NC2034.2
117500*                                                                 NC2034.2
117600 DIV-INIT-F4-14-4.                                                NC2034.2
117700     MOVE   "DIV-TEST-F4-14-4" TO PAR-NAME.                       NC2034.2
117800     ADD     1 TO REC-CT.                                         NC2034.2
117900 DIV-TEST-F4-14-4.                                                NC2034.2
118000     IF      WRK-XN-00001-2 NOT = "B"                             NC2034.2
118100             GO TO DIV-FAIL-F4-14-4.                              NC2034.2
118200     PERFORM PASS                                                 NC2034.2
118300     GO TO DIV-WRITE-F4-14-4.                                     NC2034.2
118400 DIV-DELETE-F4-14-4.                                              NC2034.2
118500     PERFORM DE-LETE.                                             NC2034.2
118600     GO TO DIV-WRITE-F4-14-4.                                     NC2034.2
118700 DIV-FAIL-F4-14-4.                                                NC2034.2
118800     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2034.2
118900     MOVE   "B" TO CORRECT-A                                      NC2034.2
119000     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2034.2
119100     PERFORM FAIL.                                                NC2034.2
119200 DIV-WRITE-F4-14-4.                                               NC2034.2
119300     PERFORM PRINT-DETAIL.                                        NC2034.2
119400*                                                                 NC2034.2
119500 DIV-INIT-F4-15.                                                  NC2034.2
119600     MOVE   "VI-82 6.11.4 GR9" TO ANSI-REFERENCE.                 NC2034.2
119700     MOVE    ZERO  TO 25COUNT.                                    NC2034.2
119800     MOVE    ZERO  TO 25ANS.                                      NC2034.2
119900     MOVE    ZERO  TO 25REM.                                      NC2034.2
120000     MOVE    1     TO REC-CT.                                     NC2034.2
120100     MOVE    SPACE TO WRK-XN-00001-1.                             NC2034.2
120200     MOVE    SPACE TO WRK-XN-00001-2.                             NC2034.2
120300     MOVE   "DIV-TEST-F4-15-0"  TO PAR-NAME.                      NC2034.2
120400 DIV-TEST-F4-15-0.                                                NC2034.2
120500     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
120600         NOT ON SIZE ERROR                                        NC2034.2
120700             MOVE    "A" TO WRK-XN-00001-1                        NC2034.2
120800     END-DIVIDE                                                   NC2034.2
120900     MOVE   "B" TO WRK-XN-00001-2.                                NC2034.2
121000*                                                                 NC2034.2
121100 DIV-INIT-F4-15-1.                                                NC2034.2
121200     MOVE   "DIV-TEST-F4-15-1" TO PAR-NAME.                       NC2034.2
121300     ADD     1 TO REC-CT.                                         NC2034.2
121400 DIV-TEST-F4-15-1.                                                NC2034.2
121500     IF      25ANS NOT = ZERO                                     NC2034.2
121600             GO TO DIV-FAIL-F4-15-1.                              NC2034.2
121700     PERFORM PASS                                                 NC2034.2
121800     GO TO DIV-WRITE-F4-15-1.                                     NC2034.2
121900 DIV-DELETE-F4-15-1.                                              NC2034.2
122000     PERFORM DE-LETE.                                             NC2034.2
122100     GO TO DIV-WRITE-F4-15-1.                                     NC2034.2
122200 DIV-FAIL-F4-15-1.                                                NC2034.2
122300     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
122400     MOVE    ZERO TO CORRECT-N                                    NC2034.2
122500     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
122600     PERFORM FAIL.                                                NC2034.2
122700 DIV-WRITE-F4-15-1.                                               NC2034.2
122800     PERFORM PRINT-DETAIL.                                        NC2034.2
122900*                                                                 NC2034.2
123000 DIV-INIT-F4-15-2.                                                NC2034.2
123100     MOVE   "DIV-TEST-F4-15-2" TO PAR-NAME.                       NC2034.2
123200     ADD     1 TO REC-CT.                                         NC2034.2
123300 DIV-TEST-F4-15-2.                                                NC2034.2
123400     IF      25REM NOT = ZERO                                     NC2034.2
123500             GO TO DIV-FAIL-F4-15-2.                              NC2034.2
123600     PERFORM PASS                                                 NC2034.2
123700     GO TO DIV-WRITE-F4-15-2.                                     NC2034.2
123800 DIV-DELETE-F4-15-2.                                              NC2034.2
123900     PERFORM DE-LETE.                                             NC2034.2
124000     GO TO DIV-WRITE-F4-15-2.                                     NC2034.2
124100 DIV-FAIL-F4-15-2.                                                NC2034.2
124200     MOVE    25REM TO COMPUTED-N                                  NC2034.2
124300     MOVE    ZERO TO CORRECT-N                                    NC2034.2
124400     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
124500     PERFORM FAIL.                                                NC2034.2
124600 DIV-WRITE-F4-15-2.                                               NC2034.2
124700     PERFORM PRINT-DETAIL.                                        NC2034.2
124800*                                                                 NC2034.2
124900 DIV-INIT-F4-15-3.                                                NC2034.2
125000     MOVE   "DIV-TEST-F4-15-3" TO PAR-NAME.                       NC2034.2
125100     ADD     1 TO REC-CT.                                         NC2034.2
125200 DIV-TEST-F4-15-3.                                                NC2034.2
125300     IF      WRK-XN-00001-1 = "A"                                 NC2034.2
125400             GO TO DIV-FAIL-F4-15-3.                              NC2034.2
125500     PERFORM PASS                                                 NC2034.2
125600     GO TO DIV-WRITE-F4-15-3.                                     NC2034.2
125700 DIV-DELETE-F4-15-3.                                              NC2034.2
125800     PERFORM DE-LETE.                                             NC2034.2
125900     GO TO DIV-WRITE-F4-15-3.                                     NC2034.2
126000 DIV-FAIL-F4-15-3.                                                NC2034.2
126100     MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"          NC2034.2
126200          TO RE-MARK                                              NC2034.2
126300     MOVE    SPACE TO CORRECT-A                                   NC2034.2
126400     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2034.2
126500     PERFORM FAIL.                                                NC2034.2
126600 DIV-WRITE-F4-15-3.                                               NC2034.2
126700     PERFORM PRINT-DETAIL.                                        NC2034.2
126800*                                                                 NC2034.2
126900 DIV-INIT-F4-15-4.                                                NC2034.2
127000     MOVE   "DIV-TEST-F4-15-4" TO PAR-NAME.                       NC2034.2
127100     ADD     1 TO REC-CT.                                         NC2034.2
127200 DIV-TEST-F4-15-4.                                                NC2034.2
127300     IF      WRK-XN-00001-2 NOT = "B"                             NC2034.2
127400             GO TO DIV-FAIL-F4-15-4.                              NC2034.2
127500     PERFORM PASS                                                 NC2034.2
127600     GO TO DIV-WRITE-F4-15-4.                                     NC2034.2
127700 DIV-DELETE-F4-15-4.                                              NC2034.2
127800     PERFORM DE-LETE.                                             NC2034.2
127900     GO TO DIV-WRITE-F4-15-4.                                     NC2034.2
128000 DIV-FAIL-F4-15-4.                                                NC2034.2
128100     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2034.2
128200     MOVE   "B" TO CORRECT-A                                      NC2034.2
128300     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2034.2
128400     PERFORM FAIL.                                                NC2034.2
128500 DIV-WRITE-F4-15-4.                                               NC2034.2
128600     PERFORM PRINT-DETAIL.                                        NC2034.2
128700*                                                                 NC2034.2
128800 DIV-INIT-F4-16.                                                  NC2034.2
128900     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
129000     MOVE ZERO TO 25ANS.                                          NC2034.2
129100     MOVE ZERO TO 25REM.                                          NC2034.2
129200     MOVE 3    TO 25COUNT.                                        NC2034.2
129300     MOVE 1    TO REC-CT.                                         NC2034.2
129400     MOVE   "DIV-TEST-F4-16-0"  TO PAR-NAME.                      NC2034.2
129500 DIV-TEST-F4-16-0.                                                NC2034.2
129600     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
129700         NOT ON SIZE ERROR                                        NC2034.2
129800             MOVE    "A" TO WRK-XN-00001-1                        NC2034.2
129900     END-DIVIDE                                                   NC2034.2
130000     MOVE   "B" TO WRK-XN-00001-2.                                NC2034.2
130100*                                                                 NC2034.2
130200 DIV-INIT-F4-16-1.                                                NC2034.2
130300     MOVE   "DIV-TEST-F4-16-1" TO PAR-NAME.                       NC2034.2
130400 DIV-TEST-F4-16-1.                                                NC2034.2
130500     IF      25ANS NOT = 33                                       NC2034.2
130600             GO TO DIV-FAIL-F4-16-1.                              NC2034.2
130700     PERFORM PASS                                                 NC2034.2
130800     GO TO DIV-WRITE-F4-16-1.                                     NC2034.2
130900 DIV-DELETE-F4-16-1.                                              NC2034.2
131000     PERFORM DE-LETE.                                             NC2034.2
131100     GO TO DIV-WRITE-F4-16-1.                                     NC2034.2
131200 DIV-FAIL-F4-16-1.                                                NC2034.2
131300     MOVE    33 TO CORRECT-N                                      NC2034.2
131400     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
131500     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
131600     PERFORM FAIL.                                                NC2034.2
131700 DIV-WRITE-F4-16-1.                                               NC2034.2
131800     PERFORM PRINT-DETAIL.                                        NC2034.2
131900*                                                                 NC2034.2
132000 DIV-INIT-F4-16-2.                                                NC2034.2
132100     MOVE   "DIV-TEST-F4-16-2" TO PAR-NAME.                       NC2034.2
132200     ADD     1 TO REC-CT.                                         NC2034.2
132300 DIV-TEST-F4-16-2.                                                NC2034.2
132400     IF      25REM NOT = 1                                        NC2034.2
132500             GO TO DIV-FAIL-F4-16-2.                              NC2034.2
132600     PERFORM PASS                                                 NC2034.2
132700     GO TO DIV-WRITE-F4-16-2.                                     NC2034.2
132800 DIV-DELETE-F4-16-2.                                              NC2034.2
132900     PERFORM DE-LETE.                                             NC2034.2
133000     GO TO DIV-WRITE-F4-16-2.                                     NC2034.2
133100 DIV-FAIL-F4-16-2.                                                NC2034.2
133200     MOVE    25REM TO COMPUTED-N                                  NC2034.2
133300     MOVE    1     TO CORRECT-N                                   NC2034.2
133400     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
133500     PERFORM FAIL.                                                NC2034.2
133600 DIV-WRITE-F4-16-2.                                               NC2034.2
133700     PERFORM PRINT-DETAIL.                                        NC2034.2
133800*                                                                 NC2034.2
133900 DIV-INIT-F4-16-3.                                                NC2034.2
134000     MOVE   "DIV-TEST-F4-16-3" TO PAR-NAME.                       NC2034.2
134100     ADD     1 TO REC-CT.                                         NC2034.2
134200 DIV-TEST-F4-16-3.                                                NC2034.2
134300     IF      WRK-XN-00001-1 NOT = "A"                             NC2034.2
134400             GO TO DIV-FAIL-F4-16-3.                              NC2034.2
134500     PERFORM PASS                                                 NC2034.2
134600     GO TO DIV-WRITE-F4-16-3.                                     NC2034.2
134700 DIV-DELETE-F4-16-3.                                              NC2034.2
134800     PERFORM DE-LETE.                                             NC2034.2
134900     GO TO DIV-WRITE-F4-16-3.                                     NC2034.2
135000 DIV-FAIL-F4-16-3.                                                NC2034.2
135100     MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"              NC2034.2
135200          TO RE-MARK                                              NC2034.2
135300     MOVE   "A" TO CORRECT-A                                      NC2034.2
135400     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2034.2
135500     PERFORM FAIL.                                                NC2034.2
135600 DIV-WRITE-F4-16-3.                                               NC2034.2
135700     PERFORM PRINT-DETAIL.                                        NC2034.2
135800*                                                                 NC2034.2
135900 DIV-INIT-F4-16-4.                                                NC2034.2
136000     MOVE   "DIV-TEST-F4-16-4" TO PAR-NAME.                       NC2034.2
136100     ADD     1 TO REC-CT.                                         NC2034.2
136200 DIV-TEST-F4-16-4.                                                NC2034.2
136300     IF      WRK-XN-00001-2 NOT = "B"                             NC2034.2
136400             GO TO DIV-FAIL-F4-16-4.                              NC2034.2
136500     PERFORM PASS                                                 NC2034.2
136600     GO TO DIV-WRITE-F4-16-4.                                     NC2034.2
136700 DIV-DELETE-F4-16-4.                                              NC2034.2
136800     PERFORM DE-LETE.                                             NC2034.2
136900     GO TO DIV-WRITE-F4-16-4.                                     NC2034.2
137000 DIV-FAIL-F4-16-4.                                                NC2034.2
137100     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2034.2
137200     MOVE   "B" TO CORRECT-A                                      NC2034.2
137300     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2034.2
137400     PERFORM FAIL.                                                NC2034.2
137500 DIV-WRITE-F4-16-4.                                               NC2034.2
137600     PERFORM PRINT-DETAIL.                                        NC2034.2
137700*                                                                 NC2034.2
137800 DIV-INIT-F4-17.                                                  NC2034.2
137900     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
138000     MOVE    ZERO TO 25COUNT.                                     NC2034.2
138100     MOVE    ZERO TO 25ANS.                                       NC2034.2
138200     MOVE    ZERO TO 25REM.                                       NC2034.2
138300     MOVE    1    TO REC-CT.                                      NC2034.2
138400     MOVE    SPACE TO WRK-XN-00001-1.                             NC2034.2
138500     MOVE    SPACE TO WRK-XN-00001-2.                             NC2034.2
138600     MOVE   "DIV-TEST-F4-17-0"  TO PAR-NAME.                      NC2034.2
138700 DIV-TEST-F4-17-0.                                                NC2034.2
138800     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
138900             ON SIZE ERROR                                        NC2034.2
139000             MOVE   "A" TO WRK-XN-00001-1                         NC2034.2
139100         NOT ON SIZE ERROR                                        NC2034.2
139200             MOVE   "B" TO WRK-XN-00001-1                         NC2034.2
139300     END-DIVIDE                                                   NC2034.2
139400     MOVE   "C" TO WRK-XN-00001-2.                                NC2034.2
139500*                                                                 NC2034.2
139600 DIV-INIT-F4-17-1.                                                NC2034.2
139700     MOVE   "DIV-TEST-F4-17-1" TO PAR-NAME.                       NC2034.2
139800     ADD     1 TO REC-CT.                                         NC2034.2
139900 DIV-TEST-F4-17-1.                                                NC2034.2
140000     IF      25ANS NOT = ZERO                                     NC2034.2
140100             GO TO DIV-FAIL-F4-17-1.                              NC2034.2
140200     PERFORM PASS                                                 NC2034.2
140300     GO TO DIV-WRITE-F4-17-1.                                     NC2034.2
140400 DIV-DELETE-F4-17-1.                                              NC2034.2
140500     PERFORM DE-LETE.                                             NC2034.2
140600     GO TO DIV-WRITE-F4-17-1.                                     NC2034.2
140700 DIV-FAIL-F4-17-1.                                                NC2034.2
140800     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
140900     MOVE    ZERO TO CORRECT-N                                    NC2034.2
141000     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
141100     PERFORM FAIL.                                                NC2034.2
141200 DIV-WRITE-F4-17-1.                                               NC2034.2
141300     PERFORM PRINT-DETAIL.                                        NC2034.2
141400*                                                                 NC2034.2
141500 DIV-INIT-F4-17-2.                                                NC2034.2
141600     MOVE   "DIV-TEST-F4-17-2" TO PAR-NAME.                       NC2034.2
141700     ADD     1 TO REC-CT.                                         NC2034.2
141800 DIV-TEST-F4-17-2.                                                NC2034.2
141900     IF      25REM NOT = ZERO                                     NC2034.2
142000             GO TO DIV-FAIL-F4-17-2.                              NC2034.2
142100     PERFORM PASS                                                 NC2034.2
142200     GO TO DIV-WRITE-F4-17-2.                                     NC2034.2
142300 DIV-DELETE-F4-17-2.                                              NC2034.2
142400     PERFORM DE-LETE.                                             NC2034.2
142500     GO TO DIV-WRITE-F4-17-2.                                     NC2034.2
142600 DIV-FAIL-F4-17-2.                                                NC2034.2
142700     MOVE    25REM TO COMPUTED-N                                  NC2034.2
142800     MOVE    ZERO TO CORRECT-N                                    NC2034.2
142900     MOVE   "SIZE ERROR SHOULD HAVE OCCURED" TO RE-MARK           NC2034.2
143000     PERFORM FAIL.                                                NC2034.2
143100 DIV-WRITE-F4-17-2.                                               NC2034.2
143200     PERFORM PRINT-DETAIL.                                        NC2034.2
143300*                                                                 NC2034.2
143400 DIV-INIT-F4-17-3.                                                NC2034.2
143500     MOVE   "DIV-TEST-F4-17-3" TO PAR-NAME.                       NC2034.2
143600     ADD     1 TO REC-CT.                                         NC2034.2
143700 DIV-TEST-F4-17-3.                                                NC2034.2
143800     IF      WRK-XN-00001-1 NOT = "A"                             NC2034.2
143900             GO TO DIV-FAIL-F4-17-3.                              NC2034.2
144000     PERFORM PASS                                                 NC2034.2
144100     GO TO DIV-WRITE-F4-17-3.                                     NC2034.2
144200 DIV-DELETE-F4-17-3.                                              NC2034.2
144300     PERFORM DE-LETE.                                             NC2034.2
144400     GO TO DIV-WRITE-F4-17-3.                                     NC2034.2
144500 DIV-FAIL-F4-17-3.                                                NC2034.2
144600     MOVE   "ON SIZE ERROR SHOULD HAVE BEEN EXECUTED"             NC2034.2
144700          TO RE-MARK                                              NC2034.2
144800     MOVE   "A" TO CORRECT-A                                      NC2034.2
144900     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2034.2
145000     PERFORM FAIL.                                                NC2034.2
145100 DIV-WRITE-F4-17-3.                                               NC2034.2
145200     PERFORM PRINT-DETAIL.                                        NC2034.2
145300*                                                                 NC2034.2
145400 DIV-INIT-F4-17-4.                                                NC2034.2
145500     MOVE   "DIV-TEST-F4-17-4" TO PAR-NAME.                       NC2034.2
145600     ADD     1 TO REC-CT.                                         NC2034.2
145700 DIV-TEST-F4-17-4.                                                NC2034.2
145800     IF      WRK-XN-00001-2 NOT = "C"                             NC2034.2
145900             GO TO DIV-FAIL-F4-17-4.                              NC2034.2
146000     PERFORM PASS                                                 NC2034.2
146100     GO TO DIV-WRITE-F4-17-4.                                     NC2034.2
146200 DIV-DELETE-F4-17-4.                                              NC2034.2
146300     PERFORM DE-LETE.                                             NC2034.2
146400     GO TO DIV-WRITE-F4-17-4.                                     NC2034.2
146500 DIV-FAIL-F4-17-4.                                                NC2034.2
146600     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2034.2
146700     MOVE   "C" TO CORRECT-A                                      NC2034.2
146800     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2034.2
146900     PERFORM FAIL.                                                NC2034.2
147000 DIV-WRITE-F4-17-4.                                               NC2034.2
147100     PERFORM PRINT-DETAIL.                                        NC2034.2
147200*                                                                 NC2034.2
147300 DIV-INIT-F4-18.                                                  NC2034.2
147400     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2034.2
147500     MOVE    SPACE TO WRK-XN-00001-1.                             NC2034.2
147600     MOVE    SPACE TO WRK-XN-00001-2.                             NC2034.2
147700     MOVE     ZERO TO 25ANS.                                      NC2034.2
147800     MOVE     ZERO TO 25REM.                                      NC2034.2
147900     MOVE     3    TO 25COUNT.                                    NC2034.2
148000     MOVE 1    TO REC-CT.                                         NC2034.2
148100     MOVE   "DIV-TEST-F4-18-0"  TO PAR-NAME.                      NC2034.2
148200 DIV-TEST-F4-18-0.                                                NC2034.2
148300     DIVIDE  25COUNT INTO 100 GIVING 25ANS REMAINDER 25REM        NC2034.2
148400             ON SIZE ERROR                                        NC2034.2
148500             MOVE   "A" TO WRK-XN-00001-1                         NC2034.2
148600         NOT ON SIZE ERROR                                        NC2034.2
148700             MOVE   "B" TO WRK-XN-00001-1                         NC2034.2
148800     END-DIVIDE                                                   NC2034.2
148900     MOVE   "B" TO WRK-XN-00001-2.                                NC2034.2
149000*                                                                 NC2034.2
149100 DIV-INIT-F4-18-1.                                                NC2034.2
149200     MOVE   "DIV-TEST-F4-18-1" TO PAR-NAME.                       NC2034.2
149300 DIV-TEST-F4-18-1.                                                NC2034.2
149400     IF      25ANS NOT = 33                                       NC2034.2
149500             GO TO DIV-FAIL-F4-18-1.                              NC2034.2
149600     PERFORM PASS                                                 NC2034.2
149700     GO TO DIV-WRITE-F4-18-1.                                     NC2034.2
149800 DIV-DELETE-F4-18-1.                                              NC2034.2
149900     PERFORM DE-LETE.                                             NC2034.2
150000     GO TO DIV-WRITE-F4-18-1.                                     NC2034.2
150100 DIV-FAIL-F4-18-1.                                                NC2034.2
150200     MOVE    33 TO CORRECT-N                                      NC2034.2
150300     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
150400     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
150500     PERFORM FAIL.                                                NC2034.2
150600 DIV-WRITE-F4-18-1.                                               NC2034.2
150700     PERFORM PRINT-DETAIL.                                        NC2034.2
150800*                                                                 NC2034.2
150900 DIV-INIT-F4-18-2.                                                NC2034.2
151000     MOVE   "DIV-TEST-F4-18-2" TO PAR-NAME.                       NC2034.2
151100     ADD     1 TO REC-CT.                                         NC2034.2
151200 DIV-TEST-F4-18-2.                                                NC2034.2
151300     IF      25REM NOT = 1                                        NC2034.2
151400             GO TO DIV-FAIL-F4-18-2.                              NC2034.2
151500     PERFORM PASS                                                 NC2034.2
151600     GO TO DIV-WRITE-F4-18-2.                                     NC2034.2
151700 DIV-DELETE-F4-18-2.                                              NC2034.2
151800     PERFORM DE-LETE.                                             NC2034.2
151900     GO TO DIV-WRITE-F4-18-2.                                     NC2034.2
152000 DIV-FAIL-F4-18-2.                                                NC2034.2
152100     MOVE    25REM TO COMPUTED-N                                  NC2034.2
152200     MOVE    1     TO CORRECT-N                                   NC2034.2
152300     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
152400     PERFORM FAIL.                                                NC2034.2
152500 DIV-WRITE-F4-18-2.                                               NC2034.2
152600     PERFORM PRINT-DETAIL.                                        NC2034.2
152700*                                                                 NC2034.2
152800 DIV-INIT-F4-18-3.                                                NC2034.2
152900     MOVE   "DIV-TEST-F4-18-3" TO PAR-NAME.                       NC2034.2
153000     ADD     1 TO REC-CT.                                         NC2034.2
153100 DIV-TEST-F4-18-3.                                                NC2034.2
153200     IF      WRK-XN-00001-1 NOT = "B"                             NC2034.2
153300             GO TO DIV-FAIL-F4-18-3.                              NC2034.2
153400     PERFORM PASS                                                 NC2034.2
153500     GO TO DIV-WRITE-F4-18-3.                                     NC2034.2
153600 DIV-DELETE-F4-18-3.                                              NC2034.2
153700     PERFORM DE-LETE.                                             NC2034.2
153800     GO TO DIV-WRITE-F4-18-3.                                     NC2034.2
153900 DIV-FAIL-F4-18-3.                                                NC2034.2
154000     MOVE   "ON SIZE ERROR SHOULD HAVE BEEN EXECUTED"             NC2034.2
154100          TO RE-MARK                                              NC2034.2
154200     MOVE   "B" TO CORRECT-A                                      NC2034.2
154300     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2034.2
154400     PERFORM FAIL.                                                NC2034.2
154500 DIV-WRITE-F4-18-3.                                               NC2034.2
154600     PERFORM PRINT-DETAIL.                                        NC2034.2
154700*                                                                 NC2034.2
154800 DIV-INIT-F4-18-4.                                                NC2034.2
154900     MOVE   "DIV-TEST-F4-18-4" TO PAR-NAME.                       NC2034.2
155000     ADD     1 TO REC-CT.                                         NC2034.2
155100 DIV-TEST-F4-18-4.                                                NC2034.2
155200     IF      WRK-XN-00001-2 NOT = "B"                             NC2034.2
155300             GO TO DIV-FAIL-F4-18-4.                              NC2034.2
155400     PERFORM PASS                                                 NC2034.2
155500     GO TO DIV-WRITE-F4-18-4.                                     NC2034.2
155600 DIV-DELETE-F4-18-4.                                              NC2034.2
155700     PERFORM DE-LETE.                                             NC2034.2
155800     GO TO DIV-WRITE-F4-18-4.                                     NC2034.2
155900 DIV-FAIL-F4-18-4.                                                NC2034.2
156000     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2034.2
156100     MOVE   "B" TO CORRECT-A                                      NC2034.2
156200     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2034.2
156300     PERFORM FAIL.                                                NC2034.2
156400 DIV-WRITE-F4-18-4.                                               NC2034.2
156500     PERFORM PRINT-DETAIL.                                        NC2034.2
156600*                                                                 NC2034.2
156700 DIV-INIT-F4-19.                                                  NC2034.2
156800     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2034.2
156900     MOVE ZERO TO 25ANS.                                          NC2034.2
157000     MOVE ZERO TO 25REM.                                          NC2034.2
157100     MOVE ZERO TO WS-REMAINDERS.                                  NC2034.2
157200     MOVE 6    TO 25COUNT.                                        NC2034.2
157300     MOVE 1    TO REC-CT.                                         NC2034.2
157400     MOVE   "DIV-TEST-F4-19-0"  TO PAR-NAME.                      NC2034.2
157500 DIV-TEST-F4-19-0.                                                NC2034.2
157600     DIVIDE  25COUNT INTO 100 GIVING 25ANS                        NC2034.2
157700             REMAINDER WS-REM (25ANS)                             NC2034.2
157800             ON SIZE ERROR                                        NC2034.2
157900             GO TO DIV-FAIL-F4-19-0.                              NC2034.2
158000     PERFORM PASS.                                                NC2034.2
158100     GO TO DIV-WRITE-F4-19-0.                                     NC2034.2
158200 DIV-DELETE-F4-19-0.                                              NC2034.2
158300     PERFORM DE-LETE.                                             NC2034.2
158400     GO TO DIV-WRITE-F4-19-0.                                     NC2034.2
158500 DIV-FAIL-F4-19-0.                                                NC2034.2
158600     MOVE   "SIZE ERROR SHOULD NOT HAVE OCCURED"                  NC2034.2
158700           TO RE-MARK                                             NC2034.2
158800     PERFORM FAIL.                                                NC2034.2
158900 DIV-WRITE-F4-19-0.                                               NC2034.2
159000     PERFORM PRINT-DETAIL.                                        NC2034.2
159100*                                                                 NC2034.2
159200 DIV-INIT-F4-19-1.                                                NC2034.2
159300     MOVE   "DIV-TEST-F4-19-1" TO PAR-NAME.                       NC2034.2
159400     ADD     1 TO REC-CT.                                         NC2034.2
159500 DIV-TEST-F4-19-1.                                                NC2034.2
159600     IF      25ANS NOT = 16                                       NC2034.2
159700             GO TO DIV-FAIL-F4-19-1.                              NC2034.2
159800     PERFORM PASS                                                 NC2034.2
159900     GO TO DIV-WRITE-F4-19-1.                                     NC2034.2
160000 DIV-DELETE-F4-19-1.                                              NC2034.2
160100     PERFORM DE-LETE.                                             NC2034.2
160200     GO TO DIV-WRITE-F4-19-1.                                     NC2034.2
160300 DIV-FAIL-F4-19-1.                                                NC2034.2
160400     MOVE    16 TO CORRECT-N                                      NC2034.2
160500     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
160600     MOVE   "INVALID QUOTIENT" TO RE-MARK                         NC2034.2
160700     PERFORM FAIL.                                                NC2034.2
160800 DIV-WRITE-F4-19-1.                                               NC2034.2
160900     PERFORM PRINT-DETAIL.                                        NC2034.2
161000*                                                                 NC2034.2
161100 DIV-INIT-F4-19-2.                                                NC2034.2
161200     MOVE   "DIV-TEST-F4-19-2" TO PAR-NAME.                       NC2034.2
161300     ADD     1 TO REC-CT.                                         NC2034.2
161400 DIV-TEST-F4-19-2.                                                NC2034.2
161500     IF      WS-REM (25ANS) NOT = 4                               NC2034.2
161600             GO TO DIV-FAIL-F4-19-2.                              NC2034.2
161700     PERFORM PASS                                                 NC2034.2
161800     GO TO DIV-WRITE-F4-19-2.                                     NC2034.2
161900 DIV-DELETE-F4-19-2.                                              NC2034.2
162000     PERFORM DE-LETE.                                             NC2034.2
162100     GO TO DIV-WRITE-F4-19-2.                                     NC2034.2
162200 DIV-FAIL-F4-19-2.                                                NC2034.2
162300     MOVE    WS-REM (25ANS) TO COMPUTED-N                         NC2034.2
162400     MOVE    4     TO CORRECT-N                                   NC2034.2
162500     MOVE   "INVALID REMAINDER" TO RE-MARK                        NC2034.2
162600     PERFORM FAIL                                                 NC2034.2
162700     PERFORM PRINT-DETAIL                                         NC2034.2
162800     ADD     1 TO REC-CT                                          NC2034.2
162900     MOVE    25ANS TO COMPUTED-N                                  NC2034.2
163000     MOVE    16    TO CORRECT-N                                   NC2034.2
163100     MOVE   "INVALID SUBSCRIPT FOR REMAINDER" TO RE-MARK          NC2034.2
163200     PERFORM FAIL.                                                NC2034.2
163300 DIV-WRITE-F4-19-2.                                               NC2034.2
163400     PERFORM PRINT-DETAIL.                                        NC2034.2
163500*                                                                 NC2034.2
163600 DIV-INIT-F4-20.                                                  NC2034.2
163700     MOVE "DIV-TEST-F4-20" TO PAR-NAME.                           NC2034.2
163800     MOVE 10.0 TO WRK-DU-2V1-1.                                   NC2034.2
163900     MOVE 3.14159265358979323 TO WRK-DU-1V17-1.                   NC2034.2
164000     MOVE    ZERO TO REC-CT.                                      NC2034.2
164100 DIV-TEST-F4-20.                                                  NC2034.2
164200     DIVIDE WRK-DU-2V1-1 INTO WRK-DU-1V17-1 GIVING WRK-DU-1V5-1   NC2034.2
164300         ROUNDED REMAINDER WRK-NE-1                               NC2034.2
164400          ON SIZE ERROR GO TO DIV-FAIL-F4-20.                     NC2034.2
164500     GO TO  DIV-TEST-F4-20-1.                                     NC2034.2
164600 DIV-DELETE-F4-20.                                                NC2034.2
164700     PERFORM DE-LETE.                                             NC2034.2
164800     PERFORM PRINT-DETAIL.                                        NC2034.2
164900     GO TO CCVS-EXIT.                                             NC2034.2
165000 DIV-FAIL-F4-20.                                                  NC2034.2
165100     PERFORM FAIL.                                                NC2034.2
165200     MOVE "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.         NC2034.2
165300     PERFORM PRINT-DETAIL.                                        NC2034.2
165400*                                                                 NC2034.2
165500 DIV-TEST-F4-20-1.                                                NC2034.2
165600     MOVE   "DIV-TEST-F4-20-1" TO PAR-NAME.                       NC2034.2
165700     MOVE 1 TO REC-CT.                                            NC2034.2
165800     IF WRK-DU-1V5-1 = 0.31416                                    NC2034.2
165900          PERFORM PASS                                            NC2034.2
166000          GO TO DIV-WRITE-F4-20-1                                 NC2034.2
166100     ELSE                                                         NC2034.2
166200          GO TO DIV-FAIL-F4-20-1.                                 NC2034.2
166300 DIV-DELETE-F4-20-1.                                              NC2034.2
166400     PERFORM DE-LETE.                                             NC2034.2
166500     GO TO DIV-WRITE-F4-20-1.                                     NC2034.2
166600 DIV-FAIL-F4-20-1.                                                NC2034.2
166700     PERFORM FAIL                                                 NC2034.2
166800     MOVE WRK-DU-1V5-1 TO COMPUTED-N                              NC2034.2
166900     MOVE 0.31416 TO CORRECT-N.                                   NC2034.2
167000 DIV-WRITE-F4-20-1.                                               NC2034.2
167100     PERFORM PRINT-DETAIL.                                        NC2034.2
167200*                                                                 NC2034.2
167300 DIV-TEST-F4-20-2.                                                NC2034.2
167400     ADD 1 TO REC-CT.                                             NC2034.2
167500     MOVE   "DIV-TEST-F4-20-2" TO PAR-NAME.                       NC2034.2
167600     IF WRK-NE-1 = ".0000/92653,58979,32"                         NC2034.2
167700            PERFORM PASS                                          NC2034.2
167800            GO TO DIV-WRITE-F4-20-2                               NC2034.2
167900     ELSE                                                         NC2034.2
168000            GO TO DIV-FAIL-F4-20-2.                               NC2034.2
168100 DIV-DELETE-F4-20-2.                                              NC2034.2
168200     PERFORM DE-LETE.                                             NC2034.2
168300     GO TO DIV-WRITE-F4-20-2.                                     NC2034.2
168400 DIV-FAIL-F4-20-2.                                                NC2034.2
168500     PERFORM FAIL                                                 NC2034.2
168600     MOVE WRK-NE-1 TO COMPUTED-A                                  NC2034.2
168700     MOVE ".0000/92653,58979,32"  TO CORRECT-A.                   NC2034.2
168800 DIV-WRITE-F4-20-2.                                               NC2034.2
168900     PERFORM PRINT-DETAIL.                                        NC2034.2
169000*                                                                 NC2034.2
169100 CCVS-EXIT SECTION.                                               NC2034.2
169200 CCVS-999999.                                                     NC2034.2
169300     GO TO CLOSE-FILES.                                           NC2034.2
      *END-OF,NC203A                                                                  
