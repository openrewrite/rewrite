      *HEADER,COBOL,NC251A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2514.2
000200 PROGRAM-ID.                                                      NC2514.2
000300     NC251A.                                                      NC2514.2
000400****************************************************************  NC2514.2
000500*                                                              *  NC2514.2
000600*    VALIDATION FOR:-                                          *  NC2514.2
000700*                                                              *  NC2514.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2514.2
000900*                                                              *  NC2514.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2514.2
001100*                                                              *  NC2514.2
001200****************************************************************  NC2514.2
001300*                                                              *  NC2514.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2514.2
001500*                                                              *  NC2514.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2514.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2514.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2514.2
001900*                                                              *  NC2514.2
002000****************************************************************  NC2514.2
002100*                                                              *  NC2514.2
002200*    THIS PROGRAM TESTS FORMAT 5 OF THE DIVIDE STATEMENT.      *  NC2514.2
002300*                                                              *  NC2514.2
002400****************************************************************  NC2514.2
002500 ENVIRONMENT DIVISION.                                            NC2514.2
002600 CONFIGURATION SECTION.                                           NC2514.2
002700 SOURCE-COMPUTER.                                                 NC2514.2
002800     XXXXX082.                                                    NC2514.2
002900 OBJECT-COMPUTER.                                                 NC2514.2
003000     XXXXX083.                                                    NC2514.2
003100 INPUT-OUTPUT SECTION.                                            NC2514.2
003200 FILE-CONTROL.                                                    NC2514.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC2514.2
003400     XXXXX055.                                                    NC2514.2
003500 DATA DIVISION.                                                   NC2514.2
003600 FILE SECTION.                                                    NC2514.2
003700 FD  PRINT-FILE.                                                  NC2514.2
003800 01  PRINT-REC PICTURE X(120).                                    NC2514.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC2514.2
004000 WORKING-STORAGE SECTION.                                         NC2514.2
004100 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC2514.2
004200 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC2514.2
004300 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC2514.2
004400 01  WS-REMAINDERS.                                               NC2514.2
004500   03  WS-REM                    PIC 99 OCCURS 20.                NC2514.2
004600 01  WRK-XN-00001-1              PIC X.                           NC2514.2
004700 01  WRK-XN-00001-2              PIC X.                           NC2514.2
004800 01  WS-46.                                                       NC2514.2
004900   03  WS-1-20                   PIC X(20).                       NC2514.2
005000   03  WS-21-40                  PIC X(20).                       NC2514.2
005100   03  WS-41-46                  PIC X(6).                        NC2514.2
005200 77  11A                PICTURE 9999  VALUE 9.                    NC2514.2
005300 77  11B   PICTURE 99; VALUE 8.                                   NC2514.2
005400 77  1111C PICTURE 99 VALUE 9.                                    NC2514.2
005500 77  WRK-DS-02V00                 PICTURE S99.                    NC2514.2
005600     88 TEST-2NUC-COND-99         VALUE 99.                       NC2514.2
005700 77  A99-DS-02V00                 PICTURE S99    VALUE 99.        NC2514.2
005800 77  WRK-DS-18V00                 PICTURE S9(18).                 NC2514.2
005900 77  WRK-DU-2V1-1                 PICTURE S99V9.                  NC2514.2
006000 77  A18ONES-DS-18V00             PICTURE S9(18)                  NC2514.2
006100                                  VALUE 111111111111111111.       NC2514.2
006200 77  A18TWOS-DS-18V00             PICTURE S9(18)                  NC2514.2
006300                                  VALUE 222222222222222222.       NC2514.2
006400 77  WRK-DS-05V00                 PICTURE S9(5).                  NC2514.2
006500 77  A02TWOS-DU-02V00             PICTURE 99     VALUE 22.        NC2514.2
006600 77  A02TWOS-DS-03V02             PICTURE S999V99 VALUE +022.00.  NC2514.2
006700 77  ATWO-DS-01V00                PICTURE S9     VALUE 2.         NC2514.2
006800 77  AZERO-DS-05V05               PICTURE S9(5)V9(5) VALUE ZERO.  NC2514.2
006900 77  WRK-DS-06V06                 PICTURE S9(6)V9(6).             NC2514.2
007000 77  WRK-DS-0201P                 PICTURE S99P.                   NC2514.2
007100 77  A05ONES-DS-05V00             PICTURE S9(5)  VALUE 11111.     NC2514.2
007200 77  WRK-DS-09V00                 PICTURE S9(9).                  NC2514.2
007300 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC2514.2
007400 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09                        NC2514.2
007500                                  PICTURE S9(18).                 NC2514.2
007600 77  XRAY                    PICTURE IS X.                        NC2514.2
007700 77  W-1                     PICTURE IS 9.                        NC2514.2
007800 77  W-2                     PICTURE IS 99.                       NC2514.2
007900 77  W-3                     PICTURE IS 999.                      NC2514.2
008000 77  W-5                PICTURE 99  VALUE ZERO.                   NC2514.2
008100 77  W-9                     PICTURE 999.                         NC2514.2
008200 77  W-11               PICTURE S99V9.                            NC2514.2
008300 77  D-1                PICTURE S9V99  VALUE 1.06.                NC2514.2
008400 77  D-7                PICTURE S99V99  VALUE 1.09.               NC2514.2
008500 77  ONE                     PICTURE IS 9      VALUE IS 1.        NC2514.2
008600 77  TWO                     PICTURE IS S9     VALUE IS 2.        NC2514.2
008700 77  THREE                   PICTURE IS S9     VALUE IS 3.        NC2514.2
008800 77  FOUR                    PICTURE IS S9     VALUE IS 4.        NC2514.2
008900 77  FIVE                    PICTURE IS S9     VALUE IS 5.        NC2514.2
009000 77  SIX                     PICTURE IS S9     VALUE IS 6.        NC2514.2
009100 77  SEVEN                   PICTURE IS S9     VALUE IS 7.        NC2514.2
009200 77  EIGHT                   PICTURE IS 9      VALUE IS 8.        NC2514.2
009300 77  NINE                    PICTURE IS S9     VALUE IS 9.        NC2514.2
009400 77  TEN                     PICTURE IS S99    VALUE IS 10.       NC2514.2
009500 77  FIFTEEN                 PICTURE IS S99    VALUE IS 15.       NC2514.2
009600 77  TWENTY                  PICTURE IS S99    VALUE IS 20.       NC2514.2
009700 77  TWENTY-5                PICTURE IS S99    VALUE IS 25.       NC2514.2
009800 77  25COUNT PICTURE 999 VALUE ZERO.                              NC2514.2
009900 77  25ANS PICTURE  99 VALUE ZERO.                                NC2514.2
010000 77  25REM PICTURE 99 VALUE ZERO.                                 NC2514.2
010100 77  DIV-30-Y1 PICTURE 999 USAGE COMP SYNC RIGHT VALUE 31.        NC2514.2
010200 77  DIV-30-Y2 PICTURE 999 USAGE COMP VALUE 54.                   NC2514.2
010300 77  DIV-30-Y3 PICTURE 999 VALUE 151.                             NC2514.2
010400 77  DIV-30-Y4         PICTURE 9(4) SYNC RIGHT VALUE 1010.        NC2514.2
010500 77  DIV-Z1-30 PICTURE 999 USAGE COMP VALUE ZERO.                 NC2514.2
010600 77  DIV-Z2-30 PICTURE 999 SYNC RIGHT VALUE ZERO.                 NC2514.2
010700 77  DIV-Z3-30 PICTURE 999 USAGE COMP SYNC RIGHT VALUE ZERO.      NC2514.2
010800 77  DIV-Z4-30 PICTURE 999 VALUE ZERO.                            NC2514.2
010900 77  DIV-30-A1 PICTURE 999 SYNC RIGHT VALUE ZERO.                 NC2514.2
011000 77  DIV-30-A2 PICTURE 999 VALUE ZERO.                            NC2514.2
011100 77  DIV-30-A3 PICTURE 999 USAGE COMP SYNC RIGHT VALUE ZERO.      NC2514.2
011200 77  DIV-30-A4 PICTURE 999 USAGE COMP VALUE ZERO.                 NC2514.2
011300 01  DIV-ENTRIES.                                                 NC2514.2
011400     02 DIV11                PICTURE 999       VALUE 105.         NC2514.2
011500     02 DIV12                PICTURE 9999      VALUE 1000.        NC2514.2
011600     02 DIV13                PICTURE 999.                         NC2514.2
011700     02 DIV14                PICTURE 99.                          NC2514.2
011800     02 DIV15                PICTURE 9V9       VALUE 1.1.         NC2514.2
011900     02 DIV16                PICTURE 99V99     VALUE 89.10.       NC2514.2
012000     02 DIV17                PICTURE 99V99.                       NC2514.2
012100     02 DIV18                PICTURE 9999.                        NC2514.2
012200     02 DIV19                PICTURE 99        VALUE 14.          NC2514.2
012300     02 DIV20                PICTURE 9999      VALUE 2147.        NC2514.2
012400     02 DIV21                PICTURE 999.                         NC2514.2
012500     02 DIV22                     PICTURE 99.                     NC2514.2
012600 01  WRK-DU-05V00-0001            PIC 9(5).                       NC2514.2
012700 01  WRK-DS-05V00-0002            PIC S9(5).                      NC2514.2
012800 01  WRK-CS-05V00-0003            PIC S9(5) COMP.                 NC2514.2
012900 01  WRK-DU-04V02-0004            PIC 9(4)V9(2).                  NC2514.2
013000 01  WRK-DS-04V01-0005            PIC S9(4)V9.                    NC2514.2
013100 01  NE-0008                      PIC $9(4).99-.                  NC2514.2
013200 01  NE-0009                      PIC ***99.                      NC2514.2
013300 01  NE-04V01-0006     PIC ****.9.                                NC2514.2
013400 01  GRP-0010.                                                    NC2514.2
013500     02 WRK-DU-03V00-L-0011       PIC 9(03) SYNC LEFT.            NC2514.2
013600     02 WRK-O005F-0012        OCCURS   5  TIMES.                  NC2514.2
013700        03 WRK-O003F-0013     OCCURS   3  TIMES.                  NC2514.2
013800           05 WRK-DS-03V04-O003F-0014 PIC S9(3)V9999              NC2514.2
013900                                            OCCURS 3 TIMES.       NC2514.2
014000 01  DS-02V00-0001                PIC S99  VALUE  16.             NC2514.2
014100 01  DS-03V00-0002                PIC S999 VALUE  174.            NC2514.2
014200 01  CS-05V00-0003                PIC S9(5) COMP  VALUE 10.       NC2514.2
014300 01    TA--X           PIC 9(5)  COMP VALUE ZERO.                 NC2514.2
014400 01  MINUS-NAMES.                                                 NC2514.2
014500     02  WHOLE-FIELD              PICTURE S9(18).                 NC2514.2
014600     02  PLUS-NAME1  PICTURE S9(18) VALUE +333333333333333333.    NC2514.2
014700     02  EVEN-NAME1  PICTURE S9(18) VALUE +1.                     NC2514.2
014800     02  PLUS-NAME2  PICTURE S9(18) VALUE +999999999999999999.    NC2514.2
014900     02  ALPHA-LIT                PICTURE X(5)  VALUE SPACE.      NC2514.2
015000     02  SNEG-LIT2                PICTURE S9(5)  VALUE -70718.    NC2514.2
015100 01  TEST-RESULTS.                                                NC2514.2
015200     02 FILLER                   PIC X      VALUE SPACE.          NC2514.2
015300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2514.2
015400     02 FILLER                   PIC X      VALUE SPACE.          NC2514.2
015500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2514.2
015600     02 FILLER                   PIC X      VALUE SPACE.          NC2514.2
015700     02  PAR-NAME.                                                NC2514.2
015800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2514.2
015900       03  PARDOT-X              PIC X      VALUE SPACE.          NC2514.2
016000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2514.2
016100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2514.2
016200     02 RE-MARK                  PIC X(61).                       NC2514.2
016300 01  TEST-COMPUTED.                                               NC2514.2
016400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2514.2
016500     02 FILLER                   PIC X(17)  VALUE                 NC2514.2
016600            "       COMPUTED=".                                   NC2514.2
016700     02 COMPUTED-X.                                               NC2514.2
016800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2514.2
016900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2514.2
017000                                 PIC -9(9).9(9).                  NC2514.2
017100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2514.2
017200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2514.2
017300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2514.2
017400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2514.2
017500         04 COMPUTED-18V0                    PIC -9(18).          NC2514.2
017600         04 FILLER                           PIC X.               NC2514.2
017700     03 FILLER PIC X(50) VALUE SPACE.                             NC2514.2
017800 01  TEST-CORRECT.                                                NC2514.2
017900     02 FILLER PIC X(30) VALUE SPACE.                             NC2514.2
018000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2514.2
018100     02 CORRECT-X.                                                NC2514.2
018200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2514.2
018300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2514.2
018400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2514.2
018500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2514.2
018600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2514.2
018700     03      CR-18V0 REDEFINES CORRECT-A.                         NC2514.2
018800         04 CORRECT-18V0                     PIC -9(18).          NC2514.2
018900         04 FILLER                           PIC X.               NC2514.2
019000     03 FILLER PIC X(2) VALUE SPACE.                              NC2514.2
019100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2514.2
019200 01  CCVS-C-1.                                                    NC2514.2
019300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2514.2
019400-    "SS  PARAGRAPH-NAME                                          NC2514.2
019500-    "       REMARKS".                                            NC2514.2
019600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2514.2
019700 01  CCVS-C-2.                                                    NC2514.2
019800     02 FILLER                     PIC X        VALUE SPACE.      NC2514.2
019900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2514.2
020000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2514.2
020100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2514.2
020200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2514.2
020300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2514.2
020400 01  REC-CT                        PIC 99       VALUE ZERO.       NC2514.2
020500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2514.2
020600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2514.2
020700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2514.2
020800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2514.2
020900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2514.2
021000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2514.2
021100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2514.2
021200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2514.2
021300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2514.2
021400 01  CCVS-H-1.                                                    NC2514.2
021500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2514.2
021600     02  FILLER                    PIC X(42)    VALUE             NC2514.2
021700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2514.2
021800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2514.2
021900 01  CCVS-H-2A.                                                   NC2514.2
022000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2514.2
022100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2514.2
022200   02  FILLER                        PIC XXXX   VALUE             NC2514.2
022300     "4.2 ".                                                      NC2514.2
022400   02  FILLER                        PIC X(28)  VALUE             NC2514.2
022500            " COPY - NOT FOR DISTRIBUTION".                       NC2514.2
022600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2514.2
022700                                                                  NC2514.2
022800 01  CCVS-H-2B.                                                   NC2514.2
022900   02  FILLER                        PIC X(15)  VALUE             NC2514.2
023000            "TEST RESULT OF ".                                    NC2514.2
023100   02  TEST-ID                       PIC X(9).                    NC2514.2
023200   02  FILLER                        PIC X(4)   VALUE             NC2514.2
023300            " IN ".                                               NC2514.2
023400   02  FILLER                        PIC X(12)  VALUE             NC2514.2
023500     " HIGH       ".                                              NC2514.2
023600   02  FILLER                        PIC X(22)  VALUE             NC2514.2
023700            " LEVEL VALIDATION FOR ".                             NC2514.2
023800   02  FILLER                        PIC X(58)  VALUE             NC2514.2
023900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2514.2
024000 01  CCVS-H-3.                                                    NC2514.2
024100     02  FILLER                      PIC X(34)  VALUE             NC2514.2
024200            " FOR OFFICIAL USE ONLY    ".                         NC2514.2
024300     02  FILLER                      PIC X(58)  VALUE             NC2514.2
024400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2514.2
024500     02  FILLER                      PIC X(28)  VALUE             NC2514.2
024600            "  COPYRIGHT   1985 ".                                NC2514.2
024700 01  CCVS-E-1.                                                    NC2514.2
024800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2514.2
024900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2514.2
025000     02 ID-AGAIN                     PIC X(9).                    NC2514.2
025100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2514.2
025200 01  CCVS-E-2.                                                    NC2514.2
025300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2514.2
025400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2514.2
025500     02 CCVS-E-2-2.                                               NC2514.2
025600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2514.2
025700         03 FILLER                   PIC X      VALUE SPACE.      NC2514.2
025800         03 ENDER-DESC               PIC X(44)  VALUE             NC2514.2
025900            "ERRORS ENCOUNTERED".                                 NC2514.2
026000 01  CCVS-E-3.                                                    NC2514.2
026100     02  FILLER                      PIC X(22)  VALUE             NC2514.2
026200            " FOR OFFICIAL USE ONLY".                             NC2514.2
026300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2514.2
026400     02  FILLER                      PIC X(58)  VALUE             NC2514.2
026500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2514.2
026600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2514.2
026700     02 FILLER                       PIC X(15)  VALUE             NC2514.2
026800             " COPYRIGHT 1985".                                   NC2514.2
026900 01  CCVS-E-4.                                                    NC2514.2
027000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2514.2
027100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2514.2
027200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2514.2
027300     02 FILLER                       PIC X(40)  VALUE             NC2514.2
027400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2514.2
027500 01  XXINFO.                                                      NC2514.2
027600     02 FILLER                       PIC X(19)  VALUE             NC2514.2
027700            "*** INFORMATION ***".                                NC2514.2
027800     02 INFO-TEXT.                                                NC2514.2
027900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2514.2
028000       04 XXCOMPUTED                 PIC X(20).                   NC2514.2
028100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2514.2
028200       04 XXCORRECT                  PIC X(20).                   NC2514.2
028300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2514.2
028400 01  HYPHEN-LINE.                                                 NC2514.2
028500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2514.2
028600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2514.2
028700-    "*****************************************".                 NC2514.2
028800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2514.2
028900-    "******************************".                            NC2514.2
029000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2514.2
029100     "NC251A".                                                    NC2514.2
029200 PROCEDURE DIVISION.                                              NC2514.2
029300 CCVS1 SECTION.                                                   NC2514.2
029400 OPEN-FILES.                                                      NC2514.2
029500     OPEN     OUTPUT PRINT-FILE.                                  NC2514.2
029600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2514.2
029700     MOVE    SPACE TO TEST-RESULTS.                               NC2514.2
029800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2514.2
029900     GO TO CCVS1-EXIT.                                            NC2514.2
030000 CLOSE-FILES.                                                     NC2514.2
030100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2514.2
030200 TERMINATE-CCVS.                                                  NC2514.2
030300*    EXIT PROGRAM.                                                NC2514.2
030400*TERMINATE-CALL.                                                  NC2514.2
030500     STOP     RUN.                                                NC2514.2
030600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2514.2
030700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2514.2
030800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2514.2
030900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2514.2
031000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2514.2
031100 PRINT-DETAIL.                                                    NC2514.2
031200     IF REC-CT NOT EQUAL TO ZERO                                  NC2514.2
031300             MOVE "." TO PARDOT-X                                 NC2514.2
031400             MOVE REC-CT TO DOTVALUE.                             NC2514.2
031500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2514.2
031600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2514.2
031700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2514.2
031800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2514.2
031900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2514.2
032000     MOVE SPACE TO CORRECT-X.                                     NC2514.2
032100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2514.2
032200     MOVE     SPACE TO RE-MARK.                                   NC2514.2
032300 HEAD-ROUTINE.                                                    NC2514.2
032400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2514.2
032500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2514.2
032600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2514.2
032700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2514.2
032800 COLUMN-NAMES-ROUTINE.                                            NC2514.2
032900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2514.2
033000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2514.2
033100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2514.2
033200 END-ROUTINE.                                                     NC2514.2
033300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2514.2
033400 END-RTN-EXIT.                                                    NC2514.2
033500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2514.2
033600 END-ROUTINE-1.                                                   NC2514.2
033700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2514.2
033800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2514.2
033900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2514.2
034000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2514.2
034100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2514.2
034200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2514.2
034300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2514.2
034400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2514.2
034500  END-ROUTINE-12.                                                 NC2514.2
034600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2514.2
034700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2514.2
034800         MOVE "NO " TO ERROR-TOTAL                                NC2514.2
034900         ELSE                                                     NC2514.2
035000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2514.2
035100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2514.2
035200     PERFORM WRITE-LINE.                                          NC2514.2
035300 END-ROUTINE-13.                                                  NC2514.2
035400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2514.2
035500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2514.2
035600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2514.2
035700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2514.2
035800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2514.2
035900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2514.2
036000          MOVE "NO " TO ERROR-TOTAL                               NC2514.2
036100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2514.2
036200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2514.2
036300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2514.2
036400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2514.2
036500 WRITE-LINE.                                                      NC2514.2
036600     ADD 1 TO RECORD-COUNT.                                       NC2514.2
036700*    IF RECORD-COUNT GREATER 50                                   NC2514.2
036800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2514.2
036900*        MOVE SPACE TO DUMMY-RECORD                               NC2514.2
037000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2514.2
037100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2514.2
037200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2514.2
037300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2514.2
037400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2514.2
037500*        MOVE ZERO TO RECORD-COUNT.                               NC2514.2
037600     PERFORM WRT-LN.                                              NC2514.2
037700 WRT-LN.                                                          NC2514.2
037800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2514.2
037900     MOVE SPACE TO DUMMY-RECORD.                                  NC2514.2
038000 BLANK-LINE-PRINT.                                                NC2514.2
038100     PERFORM WRT-LN.                                              NC2514.2
038200 FAIL-ROUTINE.                                                    NC2514.2
038300     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2514.2
038400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2514.2
038500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2514.2
038600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2514.2
038700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2514.2
038800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2514.2
038900     GO TO  FAIL-ROUTINE-EX.                                      NC2514.2
039000 FAIL-ROUTINE-WRITE.                                              NC2514.2
039100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2514.2
039200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2514.2
039300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2514.2
039400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2514.2
039500 FAIL-ROUTINE-EX. EXIT.                                           NC2514.2
039600 BAIL-OUT.                                                        NC2514.2
039700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2514.2
039800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2514.2
039900 BAIL-OUT-WRITE.                                                  NC2514.2
040000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2514.2
040100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2514.2
040200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2514.2
040300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2514.2
040400 BAIL-OUT-EX. EXIT.                                               NC2514.2
040500 CCVS1-EXIT.                                                      NC2514.2
040600     EXIT.                                                        NC2514.2
040700 SECT-NC251A-001 SECTION.                                         NC2514.2
040800 DIV-TEST-4.                                                      NC2514.2
040900     DIVIDE DIV16 BY DIV15 GIVING DIV17 REMAINDER DIV18.          NC2514.2
041000     IF DIV18 IS EQUAL TO ZERO                                    NC2514.2
041100              PERFORM PASS                                        NC2514.2
041200              GO TO DIV-WRITE-4.                                  NC2514.2
041300     PERFORM FAIL.                                                NC2514.2
041400     MOVE DIV18 TO COMPUTED-N.                                    NC2514.2
041500     MOVE "0000" TO CORRECT-A.                                    NC2514.2
041600     GO TO DIV-WRITE-4.                                           NC2514.2
041700 DIV-DELETE-4.                                                    NC2514.2
041800     PERFORM DE-LETE.                                             NC2514.2
041900 DIV-WRITE-4.                                                     NC2514.2
042000     MOVE "DIV-TEST-4" TO PAR-NAME.                               NC2514.2
042100     PERFORM PRINT-DETAIL.                                        NC2514.2
042200 DIV-TEST-5.                                                      NC2514.2
042300     MOVE ZERO TO DIV21.                                          NC2514.2
042400     MOVE ZERO TO DIV22.                                          NC2514.2
042500     DIVIDE DIV20 BY DIV19 GIVING DIV21 ROUNDED REMAINDER         NC2514.2
042600     DIV22.                                                       NC2514.2
042700     IF DIV22 IS EQUAL TO 05                                      NC2514.2
042800              PERFORM PASS                                        NC2514.2
042900              GO TO DIV-WRITE-5.                                  NC2514.2
043000     PERFORM FAIL.                                                NC2514.2
043100     MOVE DIV22 TO COMPUTED-N.                                    NC2514.2
043200     MOVE "+05" TO CORRECT-A.                                     NC2514.2
043300     GO TO DIV-WRITE-5.                                           NC2514.2
043400 DIV-DELETE-5.                                                    NC2514.2
043500     PERFORM DE-LETE.                                             NC2514.2
043600 DIV-WRITE-5.                                                     NC2514.2
043700     MOVE "DIV-TEST-5" TO PAR-NAME.                               NC2514.2
043800     PERFORM PRINT-DETAIL.                                        NC2514.2
043900*                                                                 NC2514.2
044000 DIV-INIT-F5-3.                                                   NC2514.2
044100     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
044200     MOVE   "DIV-TEST-F5-3-0"  TO PAR-NAME.                       NC2514.2
044300     MOVE    40   TO 25COUNT.                                     NC2514.2
044400     MOVE    ZERO TO 25ANS.                                       NC2514.2
044500     MOVE    ZERO TO 25REM.                                       NC2514.2
044600     MOVE    1    TO REC-CT.                                      NC2514.2
044700 DIV-TEST-F5-3-0.                                                 NC2514.2
044800     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
044900             ON SIZE ERROR                                        NC2514.2
045000             MOVE   "SIZE ERROR SHOULD NOT HAVE OCCURED"          NC2514.2
045100                  TO RE-MARK                                      NC2514.2
045200             PERFORM FAIL                                         NC2514.2
045300             PERFORM PRINT-DETAIL                                 NC2514.2
045400             GO TO   DIV-TEST-F5-3-1.                             NC2514.2
045500     PERFORM PASS.                                                NC2514.2
045600     PERFORM PRINT-DETAIL.                                        NC2514.2
045700     GO TO   DIV-TEST-F5-3-1.                                     NC2514.2
045800 DIV-DELETE-F5-3.                                                 NC2514.2
045900     PERFORM DE-LETE.                                             NC2514.2
046000     PERFORM PRINT-DETAIL.                                        NC2514.2
046100     GO TO   DIV-INIT-F5-4.                                       NC2514.2
046200 DIV-TEST-F5-3-1.                                                 NC2514.2
046300     MOVE   "DIV-TEST-F5-3-1" TO PAR-NAME.                        NC2514.2
046400     ADD     1 TO REC-CT.                                         NC2514.2
046500     IF      25ANS NOT = 2                                        NC2514.2
046600             MOVE    2 TO CORRECT-N                               NC2514.2
046700             MOVE    25ANS TO COMPUTED-N                          NC2514.2
046800             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
046900             PERFORM FAIL                                         NC2514.2
047000             PERFORM PRINT-DETAIL                                 NC2514.2
047100     ELSE                                                         NC2514.2
047200             PERFORM PASS                                         NC2514.2
047300             PERFORM PRINT-DETAIL.                                NC2514.2
047400 DIV-TEST-F5-3-2.                                                 NC2514.2
047500     MOVE   "DIV-TEST-F5-3-2" TO PAR-NAME.                        NC2514.2
047600     ADD     1 TO REC-CT.                                         NC2514.2
047700     IF      25REM NOT = 20                                       NC2514.2
047800             MOVE    25REM TO COMPUTED-N                          NC2514.2
047900             MOVE    20    TO CORRECT-N                           NC2514.2
048000             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
048100             PERFORM FAIL                                         NC2514.2
048200             PERFORM PRINT-DETAIL                                 NC2514.2
048300     ELSE                                                         NC2514.2
048400             PERFORM PASS                                         NC2514.2
048500             PERFORM PRINT-DETAIL.                                NC2514.2
048600*                                                                 NC2514.2
048700 DIV-INIT-F5-4.                                                   NC2514.2
048800     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
048900     MOVE   "DIV-TEST-F5-4-0"  TO PAR-NAME.                       NC2514.2
049000     MOVE    ZERO TO 25COUNT.                                     NC2514.2
049100     MOVE    ZERO TO 25ANS.                                       NC2514.2
049200     MOVE    ZERO TO 25REM.                                       NC2514.2
049300     MOVE    1    TO REC-CT.                                      NC2514.2
049400 DIV-TEST-F5-4-0.                                                 NC2514.2
049500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
049600             ON SIZE ERROR                                        NC2514.2
049700             PERFORM PASS                                         NC2514.2
049800             PERFORM PRINT-DETAIL                                 NC2514.2
049900             GO TO   DIV-TEST-F5-4-1.                             NC2514.2
050000     MOVE   "ON SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK.      NC2514.2
050100     PERFORM FAIL.                                                NC2514.2
050200     PERFORM PRINT-DETAIL.                                        NC2514.2
050300     GO TO   DIV-TEST-F5-4-1.                                     NC2514.2
050400 DIV-DELETE-F5-4.                                                 NC2514.2
050500     PERFORM DE-LETE.                                             NC2514.2
050600     PERFORM PRINT-DETAIL.                                        NC2514.2
050700     GO TO   DIV-INIT-F5-5.                                       NC2514.2
050800 DIV-TEST-F5-4-1.                                                 NC2514.2
050900     MOVE   "DIV-TEST-F5-4-1" TO PAR-NAME.                        NC2514.2
051000     ADD     1 TO REC-CT.                                         NC2514.2
051100     IF      25ANS NOT = 0                                        NC2514.2
051200             MOVE    0 TO CORRECT-N                               NC2514.2
051300             MOVE    25ANS TO COMPUTED-N                          NC2514.2
051400             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
051500             PERFORM FAIL                                         NC2514.2
051600             PERFORM PRINT-DETAIL                                 NC2514.2
051700     ELSE                                                         NC2514.2
051800             PERFORM PASS                                         NC2514.2
051900             PERFORM PRINT-DETAIL.                                NC2514.2
052000 DIV-TEST-F5-4-2.                                                 NC2514.2
052100     MOVE   "DIV-TEST-F5-4-2" TO PAR-NAME.                        NC2514.2
052200     ADD     1 TO REC-CT.                                         NC2514.2
052300     IF      25REM NOT = ZERO                                     NC2514.2
052400             MOVE    25REM TO COMPUTED-N                          NC2514.2
052500             MOVE    ZERO  TO CORRECT-N                           NC2514.2
052600             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
052700             PERFORM FAIL                                         NC2514.2
052800             PERFORM PRINT-DETAIL                                 NC2514.2
052900     ELSE                                                         NC2514.2
053000             PERFORM PASS                                         NC2514.2
053100             PERFORM PRINT-DETAIL.                                NC2514.2
053200*                                                                 NC2514.2
053300 DIV-INIT-F5-5.                                                   NC2514.2
053400     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
053500     MOVE   "DIV-TEST-F5-5-0"  TO PAR-NAME.                       NC2514.2
053600     MOVE    3    TO 25COUNT.                                     NC2514.2
053700     MOVE    ZERO TO 25ANS.                                       NC2514.2
053800     MOVE    ZERO TO 25REM.                                       NC2514.2
053900     MOVE    1    TO REC-CT.                                      NC2514.2
054000 DIV-TEST-F5-5-0.                                                 NC2514.2
054100     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
054200             ON SIZE ERROR                                        NC2514.2
054300             MOVE   "SIZE ERROR SHOULD NOT HAVE OCCURED"          NC2514.2
054400                  TO RE-MARK                                      NC2514.2
054500             PERFORM FAIL                                         NC2514.2
054600             PERFORM PRINT-DETAIL                                 NC2514.2
054700             GO TO   DIV-TEST-F5-5-1.                             NC2514.2
054800     PERFORM PASS.                                                NC2514.2
054900     PERFORM PRINT-DETAIL.                                        NC2514.2
055000     GO TO   DIV-TEST-F5-5-1.                                     NC2514.2
055100 DIV-DELETE-F5-5.                                                 NC2514.2
055200     PERFORM DE-LETE.                                             NC2514.2
055300     PERFORM PRINT-DETAIL.                                        NC2514.2
055400     GO TO   DIV-TEST-12.                                         NC2514.2
055500 DIV-TEST-F5-5-1.                                                 NC2514.2
055600     MOVE   "DIV-TEST-F5-5-1" TO PAR-NAME.                        NC2514.2
055700     ADD     1 TO REC-CT.                                         NC2514.2
055800     IF      25ANS NOT = 33                                       NC2514.2
055900             MOVE    33    TO CORRECT-N                           NC2514.2
056000             MOVE    25ANS TO COMPUTED-N                          NC2514.2
056100             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
056200             PERFORM FAIL                                         NC2514.2
056300             PERFORM PRINT-DETAIL                                 NC2514.2
056400     ELSE                                                         NC2514.2
056500             PERFORM PASS                                         NC2514.2
056600             PERFORM PRINT-DETAIL.                                NC2514.2
056700 DIV-TEST-F5-5-2.                                                 NC2514.2
056800     MOVE   "DIV-TEST-F5-5-2" TO PAR-NAME.                        NC2514.2
056900     ADD     1 TO REC-CT.                                         NC2514.2
057000     IF      25REM NOT = 1                                        NC2514.2
057100             MOVE    25REM TO COMPUTED-N                          NC2514.2
057200             MOVE    1     TO CORRECT-N                           NC2514.2
057300             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
057400             PERFORM FAIL                                         NC2514.2
057500             PERFORM PRINT-DETAIL                                 NC2514.2
057600     ELSE                                                         NC2514.2
057700             PERFORM PASS                                         NC2514.2
057800             PERFORM PRINT-DETAIL.                                NC2514.2
057900*                                                                 NC2514.2
058000 DIV-TEST-12.                                                     NC2514.2
058100     DIVIDE 230 BY DIV-30-Y2 GIVING DIV-Z2-30 REMAINDER           NC2514.2
058200     DIV-30-A2.                                                   NC2514.2
058300     IF DIV-Z2-30 EQUAL TO 4 AND DIV-30-A2 EQUAL TO 14            NC2514.2
058400              PERFORM PASS                                        NC2514.2
058500              GO TO DIV-WRITE-12.                                 NC2514.2
058600     PERFORM FAIL.                                                NC2514.2
058700     MOVE   4 TO CORRECT-N.                                       NC2514.2
058800     MOVE DIV-30-A3 TO COMPUTED-N.                                NC2514.2
058900     GO TO DIV-WRITE-12.                                          NC2514.2
059000 DIV-DELETE-12.                                                   NC2514.2
059100     PERFORM DE-LETE.                                             NC2514.2
059200 DIV-WRITE-12.                                                    NC2514.2
059300     MOVE "DIV-TEST-12" TO PAR-NAME.                              NC2514.2
059400     PERFORM PRINT-DETAIL.                                        NC2514.2
059500*                                                                 NC2514.2
059600 DIV-INIT-F5-7.                                                   NC2514.2
059700     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
059800     MOVE   "DIV-TEST-F5-7-0"  TO PAR-NAME.                       NC2514.2
059900     MOVE    151  TO DIV-30-Y3.                                   NC2514.2
060000     MOVE    ZERO TO DIV-Z3-30.                                   NC2514.2
060100     MOVE    ZERO TO DIV-30-A3.                                   NC2514.2
060200     MOVE    1    TO REC-CT.                                      NC2514.2
060300 DIV-TEST-F5-7-0.                                                 NC2514.2
060400     DIVIDE 681 BY DIV-30-Y3 GIVING DIV-Z3-30 REMAINDER           NC2514.2
060500     DIV-30-A3.                                                   NC2514.2
060600     GO TO   DIV-TEST-F5-7-1.                                     NC2514.2
060700 DIV-DELETE-F5-7.                                                 NC2514.2
060800     PERFORM DE-LETE.                                             NC2514.2
060900     PERFORM PRINT-DETAIL.                                        NC2514.2
061000     GO TO   DIV-INIT-F5-8.                                       NC2514.2
061100 DIV-TEST-F5-7-1.                                                 NC2514.2
061200     MOVE   "DIV-TEST-F5-7-1" TO PAR-NAME.                        NC2514.2
061300     ADD     1 TO REC-CT.                                         NC2514.2
061400     IF      DIV-Z3-30 NOT EQUAL TO 4                             NC2514.2
061500             MOVE    4     TO CORRECT-N                           NC2514.2
061600             MOVE    DIV-Z3-30 TO COMPUTED-N                      NC2514.2
061700             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
061800             PERFORM FAIL                                         NC2514.2
061900             PERFORM PRINT-DETAIL                                 NC2514.2
062000     ELSE                                                         NC2514.2
062100             PERFORM PASS                                         NC2514.2
062200             PERFORM PRINT-DETAIL.                                NC2514.2
062300 DIV-TEST-F5-7-2.                                                 NC2514.2
062400     MOVE   "DIV-TEST-F5-7-2" TO PAR-NAME.                        NC2514.2
062500     ADD     1 TO REC-CT.                                         NC2514.2
062600     IF      DIV-30-A3 NOT EQUAL TO 77                            NC2514.2
062700             MOVE    DIV-30-A3 TO COMPUTED-N                      NC2514.2
062800             MOVE    77    TO CORRECT-N                           NC2514.2
062900             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
063000             PERFORM FAIL                                         NC2514.2
063100             PERFORM PRINT-DETAIL                                 NC2514.2
063200     ELSE                                                         NC2514.2
063300             PERFORM PASS                                         NC2514.2
063400             PERFORM PRINT-DETAIL.                                NC2514.2
063500*                                                                 NC2514.2
063600 DIV-INIT-F5-8.                                                   NC2514.2
063700     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
063800     MOVE   "DIV-TEST-F5-8-0"  TO PAR-NAME.                       NC2514.2
063900     MOVE    1010 TO DIV-30-Y4.                                   NC2514.2
064000     MOVE    ZERO TO DIV-Z4-30.                                   NC2514.2
064100     MOVE    ZERO TO DIV-30-A4.                                   NC2514.2
064200     MOVE    1    TO REC-CT.                                      NC2514.2
064300 DIV-TEST-F5-8-0.                                                 NC2514.2
064400     DIVIDE  4150 BY DIV-30-Y4 GIVING DIV-Z4-30 REMAINDER         NC2514.2
064500     DIV-30-A4.                                                   NC2514.2
064600     GO TO   DIV-TEST-F5-8-1.                                     NC2514.2
064700 DIV-DELETE-F5-8.                                                 NC2514.2
064800     PERFORM DE-LETE.                                             NC2514.2
064900     PERFORM PRINT-DETAIL.                                        NC2514.2
065000     GO TO   DIV-INIT-F5-9.                                       NC2514.2
065100 DIV-TEST-F5-8-1.                                                 NC2514.2
065200     MOVE   "DIV-TEST-F5-8-1" TO PAR-NAME.                        NC2514.2
065300     ADD     1 TO REC-CT.                                         NC2514.2
065400     IF      DIV-Z4-30 NOT EQUAL TO 4                             NC2514.2
065500             MOVE    4     TO CORRECT-N                           NC2514.2
065600             MOVE    DIV-Z4-30 TO COMPUTED-N                      NC2514.2
065700             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
065800             PERFORM FAIL                                         NC2514.2
065900             PERFORM PRINT-DETAIL                                 NC2514.2
066000     ELSE                                                         NC2514.2
066100             PERFORM PASS                                         NC2514.2
066200             PERFORM PRINT-DETAIL.                                NC2514.2
066300 DIV-TEST-F5-8-2.                                                 NC2514.2
066400     MOVE   "DIV-TEST-F5-8-2" TO PAR-NAME.                        NC2514.2
066500     ADD     1 TO REC-CT.                                         NC2514.2
066600     IF      DIV-30-A4 NOT EQUAL TO 110                           NC2514.2
066700             MOVE    DIV-30-A4 TO COMPUTED-N                      NC2514.2
066800             MOVE    110   TO CORRECT-N                           NC2514.2
066900             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
067000             PERFORM FAIL                                         NC2514.2
067100             PERFORM PRINT-DETAIL                                 NC2514.2
067200     ELSE                                                         NC2514.2
067300             PERFORM PASS                                         NC2514.2
067400             PERFORM PRINT-DETAIL.                                NC2514.2
067500*                                                                 NC2514.2
067600*                                                                 NC2514.2
067700 DIV-INIT-F5-9.                                                   NC2514.2
067800     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
067900     MOVE   "DIV-TEST-F5-9-0"  TO PAR-NAME.                       NC2514.2
068000     MOVE    31   TO DIV-30-Y1.                                   NC2514.2
068100     MOVE    ZERO TO DIV-Z1-30.                                   NC2514.2
068200     MOVE    ZERO TO DIV-30-A1.                                   NC2514.2
068300     MOVE    1    TO REC-CT.                                      NC2514.2
068400 DIV-TEST-F5-9-0.                                                 NC2514.2
068500     DIVIDE 150 BY DIV-30-Y1 GIVING DIV-Z1-30 REMAINDER DIV-30-A1.NC2514.2
068600     GO TO   DIV-TEST-F5-9-1.                                     NC2514.2
068700 DIV-DELETE-F5-9.                                                 NC2514.2
068800     PERFORM DE-LETE.                                             NC2514.2
068900     PERFORM PRINT-DETAIL.                                        NC2514.2
069000     GO TO   DIV-INIT-F5-10.                                      NC2514.2
069100 DIV-TEST-F5-9-1.                                                 NC2514.2
069200     MOVE   "DIV-TEST-F5-9-1" TO PAR-NAME.                        NC2514.2
069300     ADD     1 TO REC-CT.                                         NC2514.2
069400     IF      DIV-Z1-30 NOT EQUAL TO 4                             NC2514.2
069500             MOVE    4     TO CORRECT-N                           NC2514.2
069600             MOVE    DIV-Z1-30 TO COMPUTED-N                      NC2514.2
069700             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
069800             PERFORM FAIL                                         NC2514.2
069900             PERFORM PRINT-DETAIL                                 NC2514.2
070000     ELSE                                                         NC2514.2
070100             PERFORM PASS                                         NC2514.2
070200             PERFORM PRINT-DETAIL.                                NC2514.2
070300 DIV-TEST-F5-9-2.                                                 NC2514.2
070400     MOVE   "DIV-TEST-F5-9-2" TO PAR-NAME.                        NC2514.2
070500     ADD     1 TO REC-CT.                                         NC2514.2
070600     IF      DIV-30-A1 NOT EQUAL TO 26                            NC2514.2
070700             MOVE    DIV-30-A4 TO COMPUTED-N                      NC2514.2
070800             MOVE    26    TO CORRECT-N                           NC2514.2
070900             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
071000             PERFORM FAIL                                         NC2514.2
071100             PERFORM PRINT-DETAIL                                 NC2514.2
071200     ELSE                                                         NC2514.2
071300             PERFORM PASS                                         NC2514.2
071400             PERFORM PRINT-DETAIL.                                NC2514.2
071500*                                                                 NC2514.2
071600 DIV-INIT-F5-10.                                                  NC2514.2
071700     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2514.2
071800     MOVE   "DIV-TEST-F5-10-0" TO PAR-NAME.                       NC2514.2
071900     MOVE    40   TO 25COUNT.                                     NC2514.2
072000     MOVE    ZERO TO 25ANS.                                       NC2514.2
072100     MOVE    ZERO TO 25REM.                                       NC2514.2
072200     MOVE    1    TO REC-CT.                                      NC2514.2
072300 DIV-TEST-F5-10-0.                                                NC2514.2
072400     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
072500         NOT ON SIZE ERROR                                        NC2514.2
072600             PERFORM PASS                                         NC2514.2
072700             PERFORM PRINT-DETAIL                                 NC2514.2
072800             GO TO   DIV-TEST-F5-10-1.                            NC2514.2
072900     MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED" TO RE-MARK.  NC2514.2
073000     PERFORM FAIL.                                                NC2514.2
073100     PERFORM PRINT-DETAIL.                                        NC2514.2
073200     GO TO   DIV-TEST-F5-10-1.                                    NC2514.2
073300 DIV-DELETE-F5-10.                                                NC2514.2
073400     PERFORM DE-LETE.                                             NC2514.2
073500     PERFORM PRINT-DETAIL.                                        NC2514.2
073600     GO TO   DIV-INIT-F5-11.                                      NC2514.2
073700 DIV-TEST-F5-10-1.                                                NC2514.2
073800     MOVE   "DIV-TEST-F5-10-1" TO PAR-NAME.                       NC2514.2
073900     ADD     1 TO REC-CT.                                         NC2514.2
074000     IF      25ANS NOT = 2                                        NC2514.2
074100             MOVE    2 TO CORRECT-N                               NC2514.2
074200             MOVE    25ANS TO COMPUTED-N                          NC2514.2
074300             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
074400             PERFORM FAIL                                         NC2514.2
074500             PERFORM PRINT-DETAIL                                 NC2514.2
074600     ELSE                                                         NC2514.2
074700             PERFORM PASS                                         NC2514.2
074800             PERFORM PRINT-DETAIL.                                NC2514.2
074900 DIV-TEST-F5-10-2.                                                NC2514.2
075000     MOVE   "DIV-TEST-F5-10-2" TO PAR-NAME.                       NC2514.2
075100     ADD     1 TO REC-CT.                                         NC2514.2
075200     IF      25REM NOT = 20                                       NC2514.2
075300             MOVE    25REM TO COMPUTED-N                          NC2514.2
075400             MOVE    20    TO CORRECT-N                           NC2514.2
075500             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
075600             PERFORM FAIL                                         NC2514.2
075700             PERFORM PRINT-DETAIL                                 NC2514.2
075800     ELSE                                                         NC2514.2
075900             PERFORM PASS                                         NC2514.2
076000             PERFORM PRINT-DETAIL.                                NC2514.2
076100*                                                                 NC2514.2
076200 DIV-INIT-F5-11.                                                  NC2514.2
076300     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2514.2
076400     MOVE   "DIV-TEST-F5-11-0"  TO PAR-NAME.                      NC2514.2
076500     MOVE    ZERO TO 25COUNT.                                     NC2514.2
076600     MOVE    ZERO TO 25ANS.                                       NC2514.2
076700     MOVE    ZERO TO 25REM.                                       NC2514.2
076800     MOVE    1    TO REC-CT.                                      NC2514.2
076900 DIV-TEST-F5-11-0.                                                NC2514.2
077000     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
077100         NOT ON SIZE ERROR                                        NC2514.2
077200             MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"  NC2514.2
077300                  TO RE-MARK                                      NC2514.2
077400             PERFORM FAIL                                         NC2514.2
077500             PERFORM PRINT-DETAIL                                 NC2514.2
077600             GO TO   DIV-TEST-F5-11-1.                            NC2514.2
077700     PERFORM PASS.                                                NC2514.2
077800     PERFORM PRINT-DETAIL.                                        NC2514.2
077900     GO TO   DIV-TEST-F5-11-1.                                    NC2514.2
078000 DIV-DELETE-F5-11.                                                NC2514.2
078100     PERFORM DE-LETE.                                             NC2514.2
078200     PERFORM PRINT-DETAIL.                                        NC2514.2
078300     GO TO   DIV-INIT-F5-12.                                      NC2514.2
078400 DIV-TEST-F5-11-1.                                                NC2514.2
078500     MOVE   "DIV-TEST-F5-11-1" TO PAR-NAME.                       NC2514.2
078600     ADD     1 TO REC-CT.                                         NC2514.2
078700     IF      25ANS NOT = 0                                        NC2514.2
078800             MOVE    0 TO CORRECT-N                               NC2514.2
078900             MOVE    25ANS TO COMPUTED-N                          NC2514.2
079000             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
079100             PERFORM FAIL                                         NC2514.2
079200             PERFORM PRINT-DETAIL                                 NC2514.2
079300     ELSE                                                         NC2514.2
079400             PERFORM PASS                                         NC2514.2
079500             PERFORM PRINT-DETAIL.                                NC2514.2
079600 DIV-TEST-F5-11-2.                                                NC2514.2
079700     MOVE   "DIV-TEST-F5-11-2" TO PAR-NAME.                       NC2514.2
079800     ADD     1 TO REC-CT.                                         NC2514.2
079900     IF      25REM NOT = ZERO                                     NC2514.2
080000             MOVE    25REM TO COMPUTED-N                          NC2514.2
080100             MOVE    ZERO  TO CORRECT-N                           NC2514.2
080200             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
080300             PERFORM FAIL                                         NC2514.2
080400             PERFORM PRINT-DETAIL                                 NC2514.2
080500     ELSE                                                         NC2514.2
080600             PERFORM PASS                                         NC2514.2
080700             PERFORM PRINT-DETAIL.                                NC2514.2
080800*                                                                 NC2514.2
080900 DIV-INIT-F5-12.                                                  NC2514.2
081000     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2514.2
081100     MOVE   "DIV-TEST-F5-12-0" TO PAR-NAME.                       NC2514.2
081200     MOVE    40   TO 25COUNT.                                     NC2514.2
081300     MOVE    ZERO TO 25ANS.                                       NC2514.2
081400     MOVE    ZERO TO 25REM.                                       NC2514.2
081500     MOVE    1    TO REC-CT.                                      NC2514.2
081600 DIV-TEST-F5-12-0.                                                NC2514.2
081700     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
081800             ON SIZE ERROR                                        NC2514.2
081900             MOVE   "ON SIZE ERROR SHOULD NOT HAVE EXECUTED"      NC2514.2
082000                  TO RE-MARK                                      NC2514.2
082100             PERFORM FAIL                                         NC2514.2
082200             PERFORM PRINT-DETAIL                                 NC2514.2
082300             GO TO   DIV-TEST-F5-12-1                             NC2514.2
082400         NOT ON SIZE ERROR                                        NC2514.2
082500             PERFORM PASS                                         NC2514.2
082600             PERFORM PRINT-DETAIL                                 NC2514.2
082700             GO TO   DIV-TEST-F5-12-1.                            NC2514.2
082800 DIV-DELETE-F5-12.                                                NC2514.2
082900     PERFORM DE-LETE.                                             NC2514.2
083000     PERFORM PRINT-DETAIL.                                        NC2514.2
083100     GO TO   DIV-INIT-F5-13.                                      NC2514.2
083200 DIV-TEST-F5-12-1.                                                NC2514.2
083300     MOVE   "DIV-TEST-F5-12-1" TO PAR-NAME.                       NC2514.2
083400     ADD     1 TO REC-CT.                                         NC2514.2
083500     IF      25ANS NOT = 2                                        NC2514.2
083600             MOVE    2 TO CORRECT-N                               NC2514.2
083700             MOVE    25ANS TO COMPUTED-N                          NC2514.2
083800             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
083900             PERFORM FAIL                                         NC2514.2
084000             PERFORM PRINT-DETAIL                                 NC2514.2
084100     ELSE                                                         NC2514.2
084200             PERFORM PASS                                         NC2514.2
084300             PERFORM PRINT-DETAIL.                                NC2514.2
084400 DIV-TEST-F5-12-2.                                                NC2514.2
084500     MOVE   "DIV-TEST-F5-12-2" TO PAR-NAME.                       NC2514.2
084600     ADD     1 TO REC-CT.                                         NC2514.2
084700     IF      25REM NOT = 20                                       NC2514.2
084800             MOVE    25REM TO COMPUTED-N                          NC2514.2
084900             MOVE    20    TO CORRECT-N                           NC2514.2
085000             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
085100             PERFORM FAIL                                         NC2514.2
085200             PERFORM PRINT-DETAIL                                 NC2514.2
085300     ELSE                                                         NC2514.2
085400             PERFORM PASS                                         NC2514.2
085500             PERFORM PRINT-DETAIL.                                NC2514.2
085600*                                                                 NC2514.2
085700 DIV-INIT-F5-13.                                                  NC2514.2
085800     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2514.2
085900     MOVE   "DIV-TEST-F5-13-0"  TO PAR-NAME.                      NC2514.2
086000     MOVE    ZERO TO 25COUNT.                                     NC2514.2
086100     MOVE    ZERO TO 25ANS.                                       NC2514.2
086200     MOVE    ZERO TO 25REM.                                       NC2514.2
086300     MOVE    1    TO REC-CT.                                      NC2514.2
086400 DIV-TEST-F5-13-0.                                                NC2514.2
086500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
086600             ON SIZE ERROR                                        NC2514.2
086700             PERFORM PASS                                         NC2514.2
086800             PERFORM PRINT-DETAIL                                 NC2514.2
086900             GO TO   DIV-TEST-F5-13-1                             NC2514.2
087000         NOT ON SIZE ERROR                                        NC2514.2
087100             MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"  NC2514.2
087200                  TO RE-MARK                                      NC2514.2
087300             PERFORM FAIL                                         NC2514.2
087400             PERFORM PRINT-DETAIL                                 NC2514.2
087500             GO TO   DIV-TEST-F5-13-1.                            NC2514.2
087600 DIV-DELETE-F5-13.                                                NC2514.2
087700     PERFORM DE-LETE.                                             NC2514.2
087800     PERFORM PRINT-DETAIL.                                        NC2514.2
087900     GO TO   DIV-INIT-F5-14.                                      NC2514.2
088000 DIV-TEST-F5-13-1.                                                NC2514.2
088100     MOVE   "DIV-TEST-F5-13-1" TO PAR-NAME.                       NC2514.2
088200     ADD     1 TO REC-CT.                                         NC2514.2
088300     IF      25ANS NOT = 0                                        NC2514.2
088400             MOVE    0 TO CORRECT-N                               NC2514.2
088500             MOVE    25ANS TO COMPUTED-N                          NC2514.2
088600             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
088700             PERFORM FAIL                                         NC2514.2
088800             PERFORM PRINT-DETAIL                                 NC2514.2
088900     ELSE                                                         NC2514.2
089000             PERFORM PASS                                         NC2514.2
089100             PERFORM PRINT-DETAIL.                                NC2514.2
089200 DIV-TEST-F5-13-2.                                                NC2514.2
089300     MOVE   "DIV-TEST-F5-13-2" TO PAR-NAME.                       NC2514.2
089400     ADD     1 TO REC-CT.                                         NC2514.2
089500     IF      25REM NOT = ZERO                                     NC2514.2
089600             MOVE    25REM TO COMPUTED-N                          NC2514.2
089700             MOVE    ZERO  TO CORRECT-N                           NC2514.2
089800             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
089900             PERFORM FAIL                                         NC2514.2
090000             PERFORM PRINT-DETAIL                                 NC2514.2
090100     ELSE                                                         NC2514.2
090200             PERFORM PASS                                         NC2514.2
090300             PERFORM PRINT-DETAIL.                                NC2514.2
090400*                                                                 NC2514.2
090500 DIV-INIT-F5-14.                                                  NC2514.2
090600     MOVE   "VI-82 6.11.4 GR9" TO ANSI-REFERENCE.                 NC2514.2
090700     MOVE   "DIV-TEST-F5-14-0" TO PAR-NAME.                       NC2514.2
090800     MOVE    40    TO 25COUNT.                                    NC2514.2
090900     MOVE    ZERO  TO 25ANS.                                      NC2514.2
091000     MOVE    ZERO  TO 25REM.                                      NC2514.2
091100     MOVE    1     TO REC-CT.                                     NC2514.2
091200     MOVE    SPACE TO WRK-XN-00001-1.                             NC2514.2
091300     MOVE    SPACE TO WRK-XN-00001-2.                             NC2514.2
091400 DIV-TEST-F5-14-0.                                                NC2514.2
091500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
091600             ON SIZE ERROR                                        NC2514.2
091700             MOVE   "A" TO WRK-XN-00001-1                         NC2514.2
091800     END-DIVIDE                                                   NC2514.2
091900     MOVE   "B" TO WRK-XN-00001-2.                                NC2514.2
092000     GO TO   DIV-TEST-F5-14-1.                                    NC2514.2
092100 DIV-DELETE-F5-14.                                                NC2514.2
092200     PERFORM DE-LETE.                                             NC2514.2
092300     PERFORM PRINT-DETAIL.                                        NC2514.2
092400     GO TO   DIV-INIT-F5-15.                                      NC2514.2
092500 DIV-TEST-F5-14-1.                                                NC2514.2
092600     MOVE   "DIV-TEST-F5-14-1" TO PAR-NAME.                       NC2514.2
092700     ADD     1 TO REC-CT.                                         NC2514.2
092800     IF      25ANS NOT = 2                                        NC2514.2
092900             MOVE    2 TO CORRECT-N                               NC2514.2
093000             MOVE    25ANS TO COMPUTED-N                          NC2514.2
093100             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
093200             PERFORM FAIL                                         NC2514.2
093300             PERFORM PRINT-DETAIL                                 NC2514.2
093400     ELSE                                                         NC2514.2
093500             PERFORM PASS                                         NC2514.2
093600             PERFORM PRINT-DETAIL.                                NC2514.2
093700 DIV-TEST-F5-14-2.                                                NC2514.2
093800     MOVE   "DIV-TEST-F5-14-2" TO PAR-NAME.                       NC2514.2
093900     ADD     1 TO REC-CT.                                         NC2514.2
094000     IF      25REM NOT = 20                                       NC2514.2
094100             MOVE    25REM TO COMPUTED-N                          NC2514.2
094200             MOVE    20    TO CORRECT-N                           NC2514.2
094300             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
094400             PERFORM FAIL                                         NC2514.2
094500             PERFORM PRINT-DETAIL                                 NC2514.2
094600     ELSE                                                         NC2514.2
094700             PERFORM PASS                                         NC2514.2
094800             PERFORM PRINT-DETAIL.                                NC2514.2
094900 DIV-TEST-F5-14-3.                                                NC2514.2
095000     MOVE   "DIV-TEST-F5-14-3" TO PAR-NAME.                       NC2514.2
095100     ADD     1 TO REC-CT.                                         NC2514.2
095200     IF      WRK-XN-00001-1 = SPACE                               NC2514.2
095300             PERFORM PASS                                         NC2514.2
095400             PERFORM PRINT-DETAIL                                 NC2514.2
095500     ELSE                                                         NC2514.2
095600             MOVE   "ON SIZE ERROR SHOULD NOT HAVE EXECUTED"      NC2514.2
095700                  TO RE-MARK                                      NC2514.2
095800             MOVE    SPACE TO CORRECT-A                           NC2514.2
095900             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
096000             PERFORM FAIL                                         NC2514.2
096100             PERFORM PRINT-DETAIL.                                NC2514.2
096200 DIV-TEST-F5-14-4.                                                NC2514.2
096300     MOVE   "DIV-TEST-F5-14-4" TO PAR-NAME.                       NC2514.2
096400     ADD     1 TO REC-CT.                                         NC2514.2
096500     IF      WRK-XN-00001-2 = "B"                                 NC2514.2
096600             PERFORM PASS                                         NC2514.2
096700             PERFORM PRINT-DETAIL                                 NC2514.2
096800     ELSE                                                         NC2514.2
096900             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2514.2
097000             MOVE   "B" TO CORRECT-A                              NC2514.2
097100             MOVE    WRK-XN-00001-2 TO COMPUTED-A                 NC2514.2
097200             PERFORM FAIL                                         NC2514.2
097300             PERFORM PRINT-DETAIL.                                NC2514.2
097400*                                                                 NC2514.2
097500 DIV-INIT-F5-15.                                                  NC2514.2
097600     MOVE   "VI-82 6.11.4 GR9" TO ANSI-REFERENCE.                 NC2514.2
097700     MOVE   "DIV-TEST-F5-15-0"  TO PAR-NAME.                      NC2514.2
097800     MOVE    ZERO  TO 25COUNT.                                    NC2514.2
097900     MOVE    ZERO  TO 25ANS.                                      NC2514.2
098000     MOVE    ZERO  TO 25REM.                                      NC2514.2
098100     MOVE    SPACE TO WRK-XN-00001-1.                             NC2514.2
098200     MOVE    SPACE TO WRK-XN-00001-2.                             NC2514.2
098300     MOVE    1     TO REC-CT.                                     NC2514.2
098400 DIV-TEST-F5-15-0.                                                NC2514.2
098500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
098600             ON SIZE ERROR                                        NC2514.2
098700             MOVE   "A" TO WRK-XN-00001-1                         NC2514.2
098800     END-DIVIDE                                                   NC2514.2
098900     MOVE   "B" TO WRK-XN-00001-2.                                NC2514.2
099000     GO TO   DIV-TEST-F5-15-1.                                    NC2514.2
099100 DIV-DELETE-F5-15.                                                NC2514.2
099200     PERFORM DE-LETE.                                             NC2514.2
099300     PERFORM PRINT-DETAIL.                                        NC2514.2
099400     GO TO   DIV-INIT-F5-16.                                      NC2514.2
099500 DIV-TEST-F5-15-1.                                                NC2514.2
099600     MOVE   "DIV-TEST-F5-15-1" TO PAR-NAME.                       NC2514.2
099700     ADD     1 TO REC-CT.                                         NC2514.2
099800     IF      25ANS NOT = 0                                        NC2514.2
099900             MOVE    0 TO CORRECT-N                               NC2514.2
100000             MOVE    25ANS TO COMPUTED-N                          NC2514.2
100100             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
100200             PERFORM FAIL                                         NC2514.2
100300             PERFORM PRINT-DETAIL                                 NC2514.2
100400     ELSE                                                         NC2514.2
100500             PERFORM PASS                                         NC2514.2
100600             PERFORM PRINT-DETAIL.                                NC2514.2
100700 DIV-TEST-F5-15-2.                                                NC2514.2
100800     MOVE   "DIV-TEST-F5-15-2" TO PAR-NAME.                       NC2514.2
100900     ADD     1 TO REC-CT.                                         NC2514.2
101000     IF      25REM NOT = ZERO                                     NC2514.2
101100             MOVE    25REM TO COMPUTED-N                          NC2514.2
101200             MOVE    ZERO  TO CORRECT-N                           NC2514.2
101300             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
101400             PERFORM FAIL                                         NC2514.2
101500             PERFORM PRINT-DETAIL                                 NC2514.2
101600     ELSE                                                         NC2514.2
101700             PERFORM PASS                                         NC2514.2
101800             PERFORM PRINT-DETAIL.                                NC2514.2
101900 DIV-TEST-F5-15-3.                                                NC2514.2
102000     MOVE   "DIV-TEST-F5-15-3" TO PAR-NAME.                       NC2514.2
102100     ADD     1 TO REC-CT.                                         NC2514.2
102200     IF      WRK-XN-00001-1 = "A"                                 NC2514.2
102300             PERFORM PASS                                         NC2514.2
102400             PERFORM PRINT-DETAIL                                 NC2514.2
102500     ELSE                                                         NC2514.2
102600             MOVE   "ON SIZE ERROR SHOULD HAVE EXECUTED"          NC2514.2
102700                  TO RE-MARK                                      NC2514.2
102800             MOVE   "A" TO CORRECT-A                              NC2514.2
102900             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
103000             PERFORM FAIL                                         NC2514.2
103100             PERFORM PRINT-DETAIL.                                NC2514.2
103200 DIV-TEST-F5-15-4.                                                NC2514.2
103300     MOVE   "DIV-TEST-F5-15-4" TO PAR-NAME.                       NC2514.2
103400     ADD     1 TO REC-CT.                                         NC2514.2
103500     IF      WRK-XN-00001-2 = "B"                                 NC2514.2
103600             PERFORM PASS                                         NC2514.2
103700             PERFORM PRINT-DETAIL                                 NC2514.2
103800     ELSE                                                         NC2514.2
103900             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2514.2
104000             MOVE    SPACE TO CORRECT-A                           NC2514.2
104100             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
104200             PERFORM FAIL                                         NC2514.2
104300             PERFORM PRINT-DETAIL.                                NC2514.2
104400*                                                                 NC2514.2
104500 DIV-INIT-F5-16.                                                  NC2514.2
104600     MOVE   "VI-82 6.11.4 GR9" TO ANSI-REFERENCE.                 NC2514.2
104700     MOVE   "DIV-TEST-F5-16-0" TO PAR-NAME.                       NC2514.2
104800     MOVE    40    TO 25COUNT.                                    NC2514.2
104900     MOVE    ZERO  TO 25ANS.                                      NC2514.2
105000     MOVE    ZERO  TO 25REM.                                      NC2514.2
105100     MOVE    1     TO REC-CT.                                     NC2514.2
105200     MOVE    SPACE TO WRK-XN-00001-1.                             NC2514.2
105300     MOVE    SPACE TO WRK-XN-00001-2.                             NC2514.2
105400 DIV-TEST-F5-16-0.                                                NC2514.2
105500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
105600         NOT ON SIZE ERROR                                        NC2514.2
105700             MOVE   "A" TO WRK-XN-00001-1                         NC2514.2
105800     END-DIVIDE                                                   NC2514.2
105900     MOVE   "B" TO WRK-XN-00001-2.                                NC2514.2
106000     GO TO   DIV-TEST-F5-16-1.                                    NC2514.2
106100 DIV-DELETE-F5-16.                                                NC2514.2
106200     PERFORM DE-LETE.                                             NC2514.2
106300     PERFORM PRINT-DETAIL.                                        NC2514.2
106400     GO TO   DIV-INIT-F5-17.                                      NC2514.2
106500 DIV-TEST-F5-16-1.                                                NC2514.2
106600     MOVE   "DIV-TEST-F5-16-1" TO PAR-NAME.                       NC2514.2
106700     ADD     1 TO REC-CT.                                         NC2514.2
106800     IF      25ANS NOT = 2                                        NC2514.2
106900             MOVE    2 TO CORRECT-N                               NC2514.2
107000             MOVE    25ANS TO COMPUTED-N                          NC2514.2
107100             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
107200             PERFORM FAIL                                         NC2514.2
107300             PERFORM PRINT-DETAIL                                 NC2514.2
107400     ELSE                                                         NC2514.2
107500             PERFORM PASS                                         NC2514.2
107600             PERFORM PRINT-DETAIL.                                NC2514.2
107700 DIV-TEST-F5-16-2.                                                NC2514.2
107800     MOVE   "DIV-TEST-F5-16-2" TO PAR-NAME.                       NC2514.2
107900     ADD     1 TO REC-CT.                                         NC2514.2
108000     IF      25REM NOT = 20                                       NC2514.2
108100             MOVE    25REM TO COMPUTED-N                          NC2514.2
108200             MOVE    20    TO CORRECT-N                           NC2514.2
108300             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
108400             PERFORM FAIL                                         NC2514.2
108500             PERFORM PRINT-DETAIL                                 NC2514.2
108600     ELSE                                                         NC2514.2
108700             PERFORM PASS                                         NC2514.2
108800             PERFORM PRINT-DETAIL.                                NC2514.2
108900 DIV-TEST-F5-16-3.                                                NC2514.2
109000     MOVE   "DIV-TEST-F5-16-3" TO PAR-NAME.                       NC2514.2
109100     ADD     1 TO REC-CT.                                         NC2514.2
109200     IF      WRK-XN-00001-1 = "A"                                 NC2514.2
109300             PERFORM PASS                                         NC2514.2
109400             PERFORM PRINT-DETAIL                                 NC2514.2
109500     ELSE                                                         NC2514.2
109600             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC2514.2
109700                  TO RE-MARK                                      NC2514.2
109800             MOVE   "A" TO CORRECT-A                              NC2514.2
109900             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
110000             PERFORM FAIL                                         NC2514.2
110100             PERFORM PRINT-DETAIL.                                NC2514.2
110200 DIV-TEST-F5-16-4.                                                NC2514.2
110300     MOVE   "DIV-TEST-F5-16-4" TO PAR-NAME.                       NC2514.2
110400     ADD     1 TO REC-CT.                                         NC2514.2
110500     IF      WRK-XN-00001-2 = "B"                                 NC2514.2
110600             PERFORM PASS                                         NC2514.2
110700             PERFORM PRINT-DETAIL                                 NC2514.2
110800     ELSE                                                         NC2514.2
110900             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2514.2
111000             MOVE   "B" TO CORRECT-A                              NC2514.2
111100             MOVE    WRK-XN-00001-2 TO COMPUTED-A                 NC2514.2
111200             PERFORM FAIL                                         NC2514.2
111300             PERFORM PRINT-DETAIL.                                NC2514.2
111400*                                                                 NC2514.2
111500 DIV-INIT-F5-17.                                                  NC2514.2
111600     MOVE   "VI-82 6.11.4 GR9" TO ANSI-REFERENCE.                 NC2514.2
111700     MOVE   "DIV-TEST-F5-17-0"  TO PAR-NAME.                      NC2514.2
111800     MOVE    ZERO  TO 25COUNT.                                    NC2514.2
111900     MOVE    ZERO  TO 25ANS.                                      NC2514.2
112000     MOVE    ZERO  TO 25REM.                                      NC2514.2
112100     MOVE    SPACE TO WRK-XN-00001-1.                             NC2514.2
112200     MOVE    SPACE TO WRK-XN-00001-2.                             NC2514.2
112300     MOVE    1     TO REC-CT.                                     NC2514.2
112400 DIV-TEST-F5-17-0.                                                NC2514.2
112500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
112600         NOT ON SIZE ERROR                                        NC2514.2
112700             MOVE   "A" TO WRK-XN-00001-1                         NC2514.2
112800     END-DIVIDE                                                   NC2514.2
112900     MOVE   "B" TO WRK-XN-00001-2.                                NC2514.2
113000     GO TO   DIV-TEST-F5-17-1.                                    NC2514.2
113100 DIV-DELETE-F5-17.                                                NC2514.2
113200     PERFORM DE-LETE.                                             NC2514.2
113300     PERFORM PRINT-DETAIL.                                        NC2514.2
113400     GO TO   DIV-INIT-F5-18.                                      NC2514.2
113500 DIV-TEST-F5-17-1.                                                NC2514.2
113600     MOVE   "DIV-TEST-F5-17-1" TO PAR-NAME.                       NC2514.2
113700     ADD     1 TO REC-CT.                                         NC2514.2
113800     IF      25ANS NOT = 0                                        NC2514.2
113900             MOVE    0 TO CORRECT-N                               NC2514.2
114000             MOVE    25ANS TO COMPUTED-N                          NC2514.2
114100             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
114200             PERFORM FAIL                                         NC2514.2
114300             PERFORM PRINT-DETAIL                                 NC2514.2
114400     ELSE                                                         NC2514.2
114500             PERFORM PASS                                         NC2514.2
114600             PERFORM PRINT-DETAIL.                                NC2514.2
114700 DIV-TEST-F5-17-2.                                                NC2514.2
114800     MOVE   "DIV-TEST-F5-17-2" TO PAR-NAME.                       NC2514.2
114900     ADD     1 TO REC-CT.                                         NC2514.2
115000     IF      25REM NOT = ZERO                                     NC2514.2
115100             MOVE    25REM TO COMPUTED-N                          NC2514.2
115200             MOVE    ZERO  TO CORRECT-N                           NC2514.2
115300             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
115400             PERFORM FAIL                                         NC2514.2
115500             PERFORM PRINT-DETAIL                                 NC2514.2
115600     ELSE                                                         NC2514.2
115700             PERFORM PASS                                         NC2514.2
115800             PERFORM PRINT-DETAIL.                                NC2514.2
115900 DIV-TEST-F5-17-3.                                                NC2514.2
116000     MOVE   "DIV-TEST-F5-17-3" TO PAR-NAME.                       NC2514.2
116100     ADD     1 TO REC-CT.                                         NC2514.2
116200     IF      WRK-XN-00001-1 = SPACE                               NC2514.2
116300             PERFORM PASS                                         NC2514.2
116400             PERFORM PRINT-DETAIL                                 NC2514.2
116500     ELSE                                                         NC2514.2
116600             MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"  NC2514.2
116700                  TO RE-MARK                                      NC2514.2
116800             MOVE    SPACE TO CORRECT-A                           NC2514.2
116900             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
117000             PERFORM FAIL                                         NC2514.2
117100             PERFORM PRINT-DETAIL.                                NC2514.2
117200 DIV-TEST-F5-17-4.                                                NC2514.2
117300     MOVE   "DIV-TEST-F5-17-4" TO PAR-NAME.                       NC2514.2
117400     ADD     1 TO REC-CT.                                         NC2514.2
117500     IF      WRK-XN-00001-2 = "B"                                 NC2514.2
117600             PERFORM PASS                                         NC2514.2
117700             PERFORM PRINT-DETAIL                                 NC2514.2
117800     ELSE                                                         NC2514.2
117900             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2514.2
118000             MOVE   "B" TO CORRECT-A                              NC2514.2
118100             MOVE    WRK-XN-00001-2 TO COMPUTED-A                 NC2514.2
118200             PERFORM FAIL                                         NC2514.2
118300             PERFORM PRINT-DETAIL.                                NC2514.2
118400*                                                                 NC2514.2
118500 DIV-INIT-F5-18.                                                  NC2514.2
118600     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2514.2
118700     MOVE   "DIV-TEST-F5-18-0" TO PAR-NAME.                       NC2514.2
118800     MOVE    40   TO 25COUNT.                                     NC2514.2
118900     MOVE    ZERO  TO 25ANS.                                      NC2514.2
119000     MOVE    ZERO  TO 25REM.                                      NC2514.2
119100     MOVE    1     TO REC-CT.                                     NC2514.2
119200     MOVE    SPACE TO WRK-XN-00001-1.                             NC2514.2
119300     MOVE    SPACE TO WRK-XN-00001-2.                             NC2514.2
119400 DIV-TEST-F5-18-0.                                                NC2514.2
119500     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
119600             ON SIZE ERROR                                        NC2514.2
119700             MOVE   "A" TO WRK-XN-00001-1                         NC2514.2
119800         NOT ON SIZE ERROR                                        NC2514.2
119900             MOVE   "B" TO WRK-XN-00001-1                         NC2514.2
120000     END-DIVIDE                                                   NC2514.2
120100     MOVE   "C" TO WRK-XN-00001-2.                                NC2514.2
120200     GO TO   DIV-TEST-F5-18-1.                                    NC2514.2
120300 DIV-DELETE-F5-18.                                                NC2514.2
120400     PERFORM DE-LETE.                                             NC2514.2
120500     PERFORM PRINT-DETAIL.                                        NC2514.2
120600     GO TO   DIV-INIT-F5-19.                                      NC2514.2
120700 DIV-TEST-F5-18-1.                                                NC2514.2
120800     MOVE   "DIV-TEST-F5-18-1" TO PAR-NAME.                       NC2514.2
120900     ADD     1 TO REC-CT.                                         NC2514.2
121000     IF      25ANS NOT = 2                                        NC2514.2
121100             MOVE    2 TO CORRECT-N                               NC2514.2
121200             MOVE    25ANS TO COMPUTED-N                          NC2514.2
121300             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
121400             PERFORM FAIL                                         NC2514.2
121500             PERFORM PRINT-DETAIL                                 NC2514.2
121600     ELSE                                                         NC2514.2
121700             PERFORM PASS                                         NC2514.2
121800             PERFORM PRINT-DETAIL.                                NC2514.2
121900 DIV-TEST-F5-18-2.                                                NC2514.2
122000     MOVE   "DIV-TEST-F5-18-2" TO PAR-NAME.                       NC2514.2
122100     ADD     1 TO REC-CT.                                         NC2514.2
122200     IF      25REM NOT = 20                                       NC2514.2
122300             MOVE    25REM TO COMPUTED-N                          NC2514.2
122400             MOVE    20    TO CORRECT-N                           NC2514.2
122500             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
122600             PERFORM FAIL                                         NC2514.2
122700             PERFORM PRINT-DETAIL                                 NC2514.2
122800     ELSE                                                         NC2514.2
122900             PERFORM PASS                                         NC2514.2
123000             PERFORM PRINT-DETAIL.                                NC2514.2
123100 DIV-TEST-F5-18-3.                                                NC2514.2
123200     MOVE   "DIV-TEST-F5-18-3" TO PAR-NAME.                       NC2514.2
123300     ADD     1 TO REC-CT.                                         NC2514.2
123400     IF      WRK-XN-00001-1 = "B"                                 NC2514.2
123500             PERFORM PASS                                         NC2514.2
123600             PERFORM PRINT-DETAIL                                 NC2514.2
123700     ELSE                                                         NC2514.2
123800             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC2514.2
123900                  TO RE-MARK                                      NC2514.2
124000             MOVE   "B" TO CORRECT-A                              NC2514.2
124100             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
124200             PERFORM FAIL                                         NC2514.2
124300             PERFORM PRINT-DETAIL.                                NC2514.2
124400 DIV-TEST-F5-18-4.                                                NC2514.2
124500     MOVE   "DIV-TEST-F5-18-4" TO PAR-NAME.                       NC2514.2
124600     ADD     1 TO REC-CT.                                         NC2514.2
124700     IF      WRK-XN-00001-2 = "C"                                 NC2514.2
124800             PERFORM PASS                                         NC2514.2
124900             PERFORM PRINT-DETAIL                                 NC2514.2
125000     ELSE                                                         NC2514.2
125100             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2514.2
125200             MOVE   "C"    TO CORRECT-A                           NC2514.2
125300             MOVE    WRK-XN-00001-2 TO COMPUTED-A                 NC2514.2
125400             PERFORM FAIL                                         NC2514.2
125500             PERFORM PRINT-DETAIL.                                NC2514.2
125600*                                                                 NC2514.2
125700 DIV-INIT-F5-19.                                                  NC2514.2
125800     MOVE   "VI-82 6.11.4 GR8" TO ANSI-REFERENCE.                 NC2514.2
125900     MOVE   "DIV-TEST-F5-19-0"  TO PAR-NAME.                      NC2514.2
126000     MOVE    ZERO  TO 25COUNT.                                    NC2514.2
126100     MOVE    ZERO  TO 25ANS.                                      NC2514.2
126200     MOVE    ZERO  TO 25REM.                                      NC2514.2
126300     MOVE    1     TO REC-CT.                                     NC2514.2
126400     MOVE    SPACE TO WRK-XN-00001-1.                             NC2514.2
126500     MOVE    SPACE TO WRK-XN-00001-2.                             NC2514.2
126600 DIV-TEST-F5-19-0.                                                NC2514.2
126700     DIVIDE  100 BY 25COUNT GIVING 25ANS REMAINDER 25REM          NC2514.2
126800             ON SIZE ERROR                                        NC2514.2
126900             MOVE   "A" TO WRK-XN-00001-1                         NC2514.2
127000         NOT ON SIZE ERROR                                        NC2514.2
127100             MOVE   "B" TO WRK-XN-00001-1                         NC2514.2
127200     END-DIVIDE                                                   NC2514.2
127300     MOVE   "C" TO WRK-XN-00001-2.                                NC2514.2
127400     GO TO   DIV-TEST-F5-19-1.                                    NC2514.2
127500 DIV-DELETE-F5-19.                                                NC2514.2
127600     PERFORM DE-LETE.                                             NC2514.2
127700     PERFORM PRINT-DETAIL.                                        NC2514.2
127800     GO TO   DIV-INIT-F5-20.                                      NC2514.2
127900 DIV-TEST-F5-19-1.                                                NC2514.2
128000     MOVE   "DIV-TEST-F5-19-1" TO PAR-NAME.                       NC2514.2
128100     ADD     1 TO REC-CT.                                         NC2514.2
128200     IF      25ANS NOT = 0                                        NC2514.2
128300             MOVE    0 TO CORRECT-N                               NC2514.2
128400             MOVE    25ANS TO COMPUTED-N                          NC2514.2
128500             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
128600             PERFORM FAIL                                         NC2514.2
128700             PERFORM PRINT-DETAIL                                 NC2514.2
128800     ELSE                                                         NC2514.2
128900             PERFORM PASS                                         NC2514.2
129000             PERFORM PRINT-DETAIL.                                NC2514.2
129100 DIV-TEST-F5-19-2.                                                NC2514.2
129200     MOVE   "DIV-TEST-F5-19-2" TO PAR-NAME.                       NC2514.2
129300     ADD     1 TO REC-CT.                                         NC2514.2
129400     IF      25REM NOT = ZERO                                     NC2514.2
129500             MOVE    25REM TO COMPUTED-N                          NC2514.2
129600             MOVE    ZERO  TO CORRECT-N                           NC2514.2
129700             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
129800             PERFORM FAIL                                         NC2514.2
129900             PERFORM PRINT-DETAIL                                 NC2514.2
130000     ELSE                                                         NC2514.2
130100             PERFORM PASS                                         NC2514.2
130200             PERFORM PRINT-DETAIL.                                NC2514.2
130300 DIV-TEST-F5-19-3.                                                NC2514.2
130400     MOVE   "DIV-TEST-F5-19-3" TO PAR-NAME.                       NC2514.2
130500     ADD     1 TO REC-CT.                                         NC2514.2
130600     IF      WRK-XN-00001-1 = "A"                                 NC2514.2
130700             PERFORM PASS                                         NC2514.2
130800             PERFORM PRINT-DETAIL                                 NC2514.2
130900     ELSE                                                         NC2514.2
131000             MOVE   "ON SIZE ERROR SHOULD HAVE EXECUTED"          NC2514.2
131100                  TO RE-MARK                                      NC2514.2
131200             MOVE   "A" TO CORRECT-A                              NC2514.2
131300             MOVE    WRK-XN-00001-1 TO COMPUTED-A                 NC2514.2
131400             PERFORM FAIL                                         NC2514.2
131500             PERFORM PRINT-DETAIL.                                NC2514.2
131600 DIV-TEST-F5-19-4.                                                NC2514.2
131700     MOVE   "DIV-TEST-F5-19-4" TO PAR-NAME.                       NC2514.2
131800     ADD     1 TO REC-CT.                                         NC2514.2
131900     IF      WRK-XN-00001-2 = "C"                                 NC2514.2
132000             PERFORM PASS                                         NC2514.2
132100             PERFORM PRINT-DETAIL                                 NC2514.2
132200     ELSE                                                         NC2514.2
132300             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2514.2
132400             MOVE   "C"    TO CORRECT-A                           NC2514.2
132500             MOVE    WRK-XN-00001-2 TO COMPUTED-A                 NC2514.2
132600             PERFORM FAIL                                         NC2514.2
132700             PERFORM PRINT-DETAIL.                                NC2514.2
132800*                                                                 NC2514.2
132900 DIV-INIT-F5-20.                                                  NC2514.2
133000     MOVE   "VI-82 6.11.4 GR4" TO ANSI-REFERENCE.                 NC2514.2
133100     MOVE   "DIV-TEST-F5-20-0"  TO PAR-NAME.                      NC2514.2
133200     MOVE ZERO TO 25ANS.                                          NC2514.2
133300     MOVE ZERO TO 25REM.                                          NC2514.2
133400     MOVE ZERO TO WS-REMAINDERS.                                  NC2514.2
133500     MOVE 6    TO 25COUNT.                                        NC2514.2
133600     MOVE 1    TO REC-CT.                                         NC2514.2
133700 DIV-TEST-F5-20-0.                                                NC2514.2
133800     DIVIDE  100 BY 25COUNT GIVING 25ANS                          NC2514.2
133900             REMAINDER WS-REM (25ANS)                             NC2514.2
134000             ON SIZE ERROR                                        NC2514.2
134100             MOVE   "SIZE ERROR SHOULD NOT HAVE OCCURED"          NC2514.2
134200                  TO RE-MARK                                      NC2514.2
134300             PERFORM FAIL                                         NC2514.2
134400             PERFORM PRINT-DETAIL                                 NC2514.2
134500             GO TO   DIV-TEST-F5-20-1.                            NC2514.2
134600     PERFORM PASS.                                                NC2514.2
134700     PERFORM PRINT-DETAIL.                                        NC2514.2
134800     GO TO   DIV-TEST-F5-20-1.                                    NC2514.2
134900 DIV-DELETE-F5-20.                                                NC2514.2
135000     PERFORM DE-LETE.                                             NC2514.2
135100     PERFORM PRINT-DETAIL.                                        NC2514.2
135200     GO TO   CCVS-EXIT.                                           NC2514.2
135300 DIV-TEST-F5-20-1.                                                NC2514.2
135400     MOVE   "DIV-TEST-F5-20-1" TO PAR-NAME.                       NC2514.2
135500     ADD     1 TO REC-CT.                                         NC2514.2
135600     IF      25ANS NOT = 16                                       NC2514.2
135700             MOVE    16 TO CORRECT-N                              NC2514.2
135800             MOVE    25ANS TO COMPUTED-N                          NC2514.2
135900             MOVE   "INVALID QUOTIENT" TO RE-MARK                 NC2514.2
136000             PERFORM FAIL                                         NC2514.2
136100             PERFORM PRINT-DETAIL                                 NC2514.2
136200     ELSE                                                         NC2514.2
136300             PERFORM PASS                                         NC2514.2
136400             PERFORM PRINT-DETAIL.                                NC2514.2
136500 DIV-TEST-F5-20-2.                                                NC2514.2
136600     MOVE   "DIV-TEST-F5-20-2" TO PAR-NAME.                       NC2514.2
136700     ADD     1 TO REC-CT.                                         NC2514.2
136800     IF      WS-REM (25ANS) NOT = 4                               NC2514.2
136900             MOVE    WS-REM (25ANS) TO COMPUTED-N                 NC2514.2
137000             MOVE    4     TO CORRECT-N                           NC2514.2
137100             MOVE   "INVALID REMAINDER" TO RE-MARK                NC2514.2
137200             PERFORM FAIL                                         NC2514.2
137300             PERFORM PRINT-DETAIL                                 NC2514.2
137400             ADD     1 TO REC-CT                                  NC2514.2
137500             MOVE    25ANS TO COMPUTED-N                          NC2514.2
137600             MOVE    16    TO CORRECT-N                           NC2514.2
137700             MOVE   "INVALID SUBSCRIPT FOR REMAINDER" TO RE-MARK  NC2514.2
137800             PERFORM FAIL                                         NC2514.2
137900             PERFORM PRINT-DETAIL                                 NC2514.2
138000     ELSE                                                         NC2514.2
138100             PERFORM PASS                                         NC2514.2
138200             PERFORM PRINT-DETAIL.                                NC2514.2
138300*                                                                 NC2514.2
138400 DIV-INIT-F5-21.                                                  NC2514.2
138500     MOVE "DIV-TEST-F5-21" TO PAR-NAME.                           NC2514.2
138600     MOVE 10.0 TO WRK-DU-2V1-1.                                   NC2514.2
138700     MOVE    ZERO TO REC-CT.                                      NC2514.2
138800 DIVIDE-REMAINDER-TEST-7.                                         NC2514.2
138900     DIVIDE WRK-DU-1V17-1 BY WRK-DU-2V1-1 GIVING WRK-DU-1V5-1     NC2514.2
139000     REMAINDER WRK-NE-1 ON SIZE ERROR GO TO DIV-FAIL-F5-21.       NC2514.2
139100     GO TO DIV-TEST-F5-21-1.                                      NC2514.2
139200 DIV-DELETE-F5-21.                                                NC2514.2
139300     PERFORM DE-LETE.                                             NC2514.2
139400     PERFORM PRINT-DETAIL.                                        NC2514.2
139500     GO TO CCVS-EXIT.                                             NC2514.2
139600 DIV-FAIL-F5-21.                                                  NC2514.2
139700     PERFORM FAIL.                                                NC2514.2
139800     MOVE "SIZE ERROR BAD" TO RE-MARK.                            NC2514.2
139900     PERFORM PRINT-DETAIL.                                        NC2514.2
140000 DIV-TEST-F5-21-1.                                                NC2514.2
140100     MOVE "DIV-TEST-F5-21-1" TO ANSI-REFERENCE.                   NC2514.2
140200     MOVE 1 TO REC-CT.                                            NC2514.2
140300     IF WRK-DU-1V5-1 = 0.31415 PERFORM PASS PERFORM PRINT-DETAIL  NC2514.2
140400     ELSE                                                         NC2514.2
140500     PERFORM FAIL MOVE WRK-DU-1V5-1 TO COMPUTED-N MOVE 0.31415    NC2514.2
140600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC2514.2
140700     ADD 1 TO REC-CT.                                             NC2514.2
140800 DIV-TEST-F5-21-2.                                                NC2514.2
140900     MOVE "DIV-TEST-F5-21-2" TO ANSI-REFERENCE.                   NC2514.2
141000     IF WRK-NE-1 = ".0000/92653,58979,32"  PERFORM PASS           NC2514.2
141100     PERFORM PRINT-DETAIL ELSE                                    NC2514.2
141200     PERFORM FAIL MOVE WRK-NE-1 TO COMPUTED-A MOVE                NC2514.2
141300     ".0000/92653,58979,32"  TO CORRECT-A PERFORM PRINT-DETAIL.   NC2514.2
141400*                                                                 NC2514.2
141500 CCVS-EXIT SECTION.                                               NC2514.2
141600 CCVS-999999.                                                     NC2514.2
141700     GO TO CLOSE-FILES.                                           NC2514.2
      *END-OF,NC251A                                                                  
