      *HEADER,COBOL,NC104A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1044.2
000200 PROGRAM-ID.                                                      NC1044.2
000300     NC104A.                                                      NC1044.2
000400****************************************************************  NC1044.2
000500*                                                              *  NC1044.2
000600*    VALIDATION FOR:-                                          *  NC1044.2
000700*                                                              *  NC1044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1044.2
000900*                                                              *  NC1044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1044.2
001100*                                                              *  NC1044.2
001200****************************************************************  NC1044.2
001300*                                                              *  NC1044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1044.2
001500*                                                              *  NC1044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1044.2
001900*                                                              *  NC1044.2
002000****************************************************************  NC1044.2
002100*                                                                 NC1044.2
002200*    PROGRAM NC104A TESTS FORMAT 1 OF THE MOVE STATEMENT          NC1044.2
002300*    WITH VARIOUS COMBINATIONS OF SENDING AND RECEIVING FIELDS.   NC1044.2
002400*                                                                 NC1044.2
002500*    (SEE ALSO NC105A).                                           NC1044.2
002600*                                                                 NC1044.2
002700                                                                  NC1044.2
002800 ENVIRONMENT DIVISION.                                            NC1044.2
002900 CONFIGURATION SECTION.                                           NC1044.2
003000 SOURCE-COMPUTER.                                                 NC1044.2
003100     XXXXX082.                                                    NC1044.2
003200 OBJECT-COMPUTER.                                                 NC1044.2
003300     XXXXX083.                                                    NC1044.2
003400 INPUT-OUTPUT SECTION.                                            NC1044.2
003500 FILE-CONTROL.                                                    NC1044.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC1044.2
003700     XXXXX055.                                                    NC1044.2
003800 DATA DIVISION.                                                   NC1044.2
003900 FILE SECTION.                                                    NC1044.2
004000 FD  PRINT-FILE                                                   NC1044.2
004100     LABEL RECORDS                                                NC1044.2
004200     XXXXX084                                                     NC1044.2
004300     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       NC1044.2
004400 01  PRINT-REC PICTURE X(120).                                    NC1044.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 NC1044.2
004600 WORKING-STORAGE SECTION.                                         NC1044.2
004700 01  MOVE1                              PICTURE IS 9(5)           NC1044.2
004800     VALUE IS 12345.                                              NC1044.2
004900 01  MOVE2                              PICTURE IS 9(5).          NC1044.2
005000 01  MOVE3                              PICTURE IS 99.            NC1044.2
005100 01  MOVE4                              PICTURE IS 9(7).          NC1044.2
005200 01  MOVE5                              PICTURE IS 99V999.        NC1044.2
005300 01  MOVE6                              PICTURE IS V99999.        NC1044.2
005400 01  MOVE8                              PICTURE IS 9(5)V99.       NC1044.2
005500 01  MOVE9                              PICTURE IS 9(7)V99.       NC1044.2
005600 01  MOVE10                             PICTURE IS $999.99.       NC1044.2
005700 01  MOVE11                             PICTURE IS $99,999.99.    NC1044.2
005800 01  MOVE12                             PICTURE IS $(5)9(3).      NC1044.2
005900 01  MOVE13                             PICTURE IS *(5)9(6).      NC1044.2
006000 01  MOVE14                             PICTURE IS +9(5).         NC1044.2
006100 01  MOVE15                             PICTURE IS 9(5)           NC1044.2
006200     VALUE IS 00000.                                              NC1044.2
006300 01  MOVE16                             PICTURE IS 9(5)CR.        NC1044.2
006400 01  MOVE17                             PICTURE IS $99,999.99     NC1044.2
006500     BLANK WHEN ZERO.                                             NC1044.2
006600 01  MOVE18                             PICTURE IS ZZZZZZ.        NC1044.2
006700 01  MOVE19                             PICTURE IS X(5).          NC1044.2
006800 01  MOVE20                             PICTURE IS X(4).          NC1044.2
006900 01  MOVE21                             PICTURE IS X(7).          NC1044.2
007000 01  MOVE22                             PICTURE IS XBX0XBX0X.     NC1044.2
007100 01  MOVE23                             PICTURE IS 999V99         NC1044.2
007200     VALUE IS 123.45.                                             NC1044.2
007300 01  MOVE24                             PICTURE IS XBXXXB000XXXX. NC1044.2
007400 01  MOVE25                             PICTURE IS 999.           NC1044.2
007500 01  MOVE26                             PICTURE IS 999V99.        NC1044.2
007600 01  MOVE27                             PICTURE IS 99PP.          NC1044.2
007700 01  MOVE29                             PICTURE IS 9999V999.      NC1044.2
007800 01  MOVE29A VALUE IS "$123.45".                                  NC1044.2
007900     02 MOVE30                          PICTURE IS $999.99.       NC1044.2
008000 01  MOVE31                             PICTURE IS X(9).          NC1044.2
008100 01  MOVE32                             PICTURE IS X(5)           NC1044.2
008200     VALUE IS "ABCDE".                                            NC1044.2
008300 01  MOVE33                             PICTURE IS A(5).          NC1044.2
008400 01  MOVE34                             PICTURE IS A(7).          NC1044.2
008500 01  MOVE35                             PICTURE IS A(3).          NC1044.2
008600 01  MOVE35A VALUE IS "1 A05".                                    NC1044.2
008700     02 MOVE36                          PICTURE IS XBA09.         NC1044.2
008800 01  MOVE37                             PICTURE IS AAAAA          NC1044.2
008900     VALUE IS "ABCDE".                                            NC1044.2
009000 01  MOVE39                             PICTURE IS 0XXXXX0.       NC1044.2
009100 01  MOVE47A.                                                     NC1044.2
009200     02 MOVE48                          PICTURE IS 9V9(17).       NC1044.2
009300     02 MOVE49                          PICTURE IS 9(5)           NC1044.2
009400     VALUE IS 00045.                                              NC1044.2
009500     02 MOVE50                          PICTURE IS X(5)           NC1044.2
009600     VALUE IS "12345".                                            NC1044.2
009700     02 MOVE51                          PICTURE IS S9(5)          NC1044.2
009800     VALUE IS -12345.                                             NC1044.2
009900     02 MOVE52                          PICTURE IS 9(5)-.         NC1044.2
010000 01  AN-DATANAMES.                                                NC1044.2
010100     02  ANDATA1       PICTURE X     VALUE SPACE.                 NC1044.2
010200     02  ANDATA2       PICTURE XX    VALUE SPACE.                 NC1044.2
010300     02  ANDATA3       PICTURE XXX   VALUE SPACE.                 NC1044.2
010400     02  ANDATA4       PICTURE X(4)  VALUE SPACE.                 NC1044.2
010500     02  ANDATA5       PICTURE X(5)  VALUE SPACE.                 NC1044.2
010600     02  ANDATA6       PICTURE X(6)  VALUE SPACE.                 NC1044.2
010700     02  ANDATA7       PICTURE X(7)  VALUE SPACE.                 NC1044.2
010800     02  ANDATA8       PICTURE X(8)  VALUE SPACE.                 NC1044.2
010900     02  ANDATA9       PICTURE X(9)  VALUE SPACE.                 NC1044.2
011000     02  ANDATA10      PICTURE X(10) VALUE SPACE.                 NC1044.2
011100     02  ANDATA11      PICTURE X(11) VALUE SPACE.                 NC1044.2
011200     02  ANDATA12      PICTURE X(12) VALUE SPACE.                 NC1044.2
011300     02  ANDATA13      PICTURE X(13) VALUE SPACE.                 NC1044.2
011400     02  ANDATA14      PICTURE X(14) VALUE SPACE.                 NC1044.2
011500     02  ANDATA15      PICTURE X(15) VALUE SPACE.                 NC1044.2
011600     02  ANDATA16      PICTURE X(16) VALUE SPACE.                 NC1044.2
011700     02  ANDATA17      PICTURE X(17) VALUE SPACE.                 NC1044.2
011800     02  ANDATA18      PICTURE X(18) VALUE SPACE.                 NC1044.2
011900     02  ANDATA19      PICTURE X(19) VALUE SPACE.                 NC1044.2
012000     02  ANDATA20      PICTURE X(20) VALUE SPACE.                 NC1044.2
012100     02  ANDATA21      PICTURE X(120) VALUE SPACE.                NC1044.2
012200 01  42-DATANAMES.                                                NC1044.2
012300     02  DNAME1   PICTURE 9      VALUE 1        COMPUTATIONAL.    NC1044.2
012400     02  DNAME2   PICTURE 99     VALUE 01       COMPUTATIONAL.    NC1044.2
012500     02  DNAME3   PICTURE 999    VALUE 001      COMPUTATIONAL.    NC1044.2
012600     02  DNAME4   PICTURE 9(4)   VALUE 0001     COMPUTATIONAL.    NC1044.2
012700     02  DNAME5   PICTURE 9(5)   VALUE 00001    COMPUTATIONAL.    NC1044.2
012800     02  DNAME6   PICTURE 9(6)   VALUE 000001   COMPUTATIONAL.    NC1044.2
012900     02  DNAME7   PICTURE 9(7)   VALUE 0000001  COMPUTATIONAL.    NC1044.2
013000     02  DNAME8   PICTURE 9(8)   VALUE 00000001 COMPUTATIONAL.    NC1044.2
013100     02  DNAME9   PICTURE 9(9)   VALUE 000000001.                 NC1044.2
013200     02  DNAME10  PICTURE 9(10)   VALUE 0000000001.               NC1044.2
013300     02  DNAME11  PICTURE 9(11)   VALUE 00000000001.              NC1044.2
013400     02  DNAME12  PICTURE 9(12)   VALUE 000000000001.             NC1044.2
013500     02  DNAME13  PICTURE 9(13)   VALUE 0000000000001.            NC1044.2
013600     02  DNAME14  PICTURE 9(14)   VALUE 00000000000001.           NC1044.2
013700     02  DNAME15  PICTURE 9(15)   VALUE 000000000000001.          NC1044.2
013800     02  DNAME16  PICTURE 9(16)   VALUE 0000000000000001.         NC1044.2
013900     02  DNAME17  PICTURE 9(17)   VALUE 00000000000000001.        NC1044.2
014000     02  DNAME18  PICTURE 9(18)   VALUE 000000000000000001.       NC1044.2
014100     02  DNAME19  PICTURE 9      VALUE 1.                         NC1044.2
014200     02  DNAME20  PICTURE 99     VALUE 11.                        NC1044.2
014300     02  DNAME21  PICTURE 999    VALUE 111.                       NC1044.2
014400     02  DNAME22  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
014500     02  DNAME23  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
014600     02  DNAME24  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
014700     02  DNAME25  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
014800     02  DNAME26  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
014900     02  DNAME27  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
015000     02  DNAME28  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
015100     02  DNAME29  PICTURE 9(18)  VALUE ZERO.                      NC1044.2
015200     02  DNAME30   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015300     02  DNAME31   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015400     02  DNAME32   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015500     02  DNAME33   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015600     02  DNAME34   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015700     02  DNAME35   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015800     02  DNAME36   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
015900     02  DNAME37   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
016000     02  DNAME38   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
016100     02  DNAME39   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
016200     02  DNAME40   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
016300     02  DNAME41   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
016400     02  DNAME42   PICTURE 9(18)  VALUE ZERO COMPUTATIONAL.       NC1044.2
016500 01  TEST-RESULTS.                                                NC1044.2
016600     02 FILLER                   PIC X      VALUE SPACE.          NC1044.2
016700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1044.2
016800     02 FILLER                   PIC X      VALUE SPACE.          NC1044.2
016900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1044.2
017000     02 FILLER                   PIC X      VALUE SPACE.          NC1044.2
017100     02  PAR-NAME.                                                NC1044.2
017200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1044.2
017300       03  PARDOT-X              PIC X      VALUE SPACE.          NC1044.2
017400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1044.2
017500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1044.2
017600     02 RE-MARK                  PIC X(61).                       NC1044.2
017700 01  TEST-COMPUTED.                                               NC1044.2
017800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1044.2
017900     02 FILLER                   PIC X(17)  VALUE                 NC1044.2
018000            "       COMPUTED=".                                   NC1044.2
018100     02 COMPUTED-X.                                               NC1044.2
018200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1044.2
018300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1044.2
018400                                 PIC -9(9).9(9).                  NC1044.2
018500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1044.2
018600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1044.2
018700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1044.2
018800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1044.2
018900         04 COMPUTED-18V0                    PIC -9(18).          NC1044.2
019000         04 FILLER                           PIC X.               NC1044.2
019100     03 FILLER PIC X(50) VALUE SPACE.                             NC1044.2
019200 01  TEST-CORRECT.                                                NC1044.2
019300     02 FILLER PIC X(30) VALUE SPACE.                             NC1044.2
019400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1044.2
019500     02 CORRECT-X.                                                NC1044.2
019600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1044.2
019700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1044.2
019800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1044.2
019900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1044.2
020000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1044.2
020100     03      CR-18V0 REDEFINES CORRECT-A.                         NC1044.2
020200         04 CORRECT-18V0                     PIC -9(18).          NC1044.2
020300         04 FILLER                           PIC X.               NC1044.2
020400     03 FILLER PIC X(2) VALUE SPACE.                              NC1044.2
020500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1044.2
020600 01  CCVS-C-1.                                                    NC1044.2
020700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1044.2
020800-    "SS  PARAGRAPH-NAME                                          NC1044.2
020900-    "       REMARKS".                                            NC1044.2
021000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1044.2
021100 01  CCVS-C-2.                                                    NC1044.2
021200     02 FILLER                     PIC X        VALUE SPACE.      NC1044.2
021300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1044.2
021400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1044.2
021500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1044.2
021600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1044.2
021700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1044.2
021800 01  REC-CT                        PIC 99       VALUE ZERO.       NC1044.2
021900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1044.2
022000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1044.2
022100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1044.2
022200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1044.2
022300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1044.2
022400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1044.2
022500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1044.2
022600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1044.2
022700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1044.2
022800 01  CCVS-H-1.                                                    NC1044.2
022900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1044.2
023000     02  FILLER                    PIC X(42)    VALUE             NC1044.2
023100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1044.2
023200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1044.2
023300 01  CCVS-H-2A.                                                   NC1044.2
023400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1044.2
023500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1044.2
023600   02  FILLER                        PIC XXXX   VALUE             NC1044.2
023700     "4.2 ".                                                      NC1044.2
023800   02  FILLER                        PIC X(28)  VALUE             NC1044.2
023900            " COPY - NOT FOR DISTRIBUTION".                       NC1044.2
024000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1044.2
024100                                                                  NC1044.2
024200 01  CCVS-H-2B.                                                   NC1044.2
024300   02  FILLER                        PIC X(15)  VALUE             NC1044.2
024400            "TEST RESULT OF ".                                    NC1044.2
024500   02  TEST-ID                       PIC X(9).                    NC1044.2
024600   02  FILLER                        PIC X(4)   VALUE             NC1044.2
024700            " IN ".                                               NC1044.2
024800   02  FILLER                        PIC X(12)  VALUE             NC1044.2
024900     " HIGH       ".                                              NC1044.2
025000   02  FILLER                        PIC X(22)  VALUE             NC1044.2
025100            " LEVEL VALIDATION FOR ".                             NC1044.2
025200   02  FILLER                        PIC X(58)  VALUE             NC1044.2
025300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1044.2
025400 01  CCVS-H-3.                                                    NC1044.2
025500     02  FILLER                      PIC X(34)  VALUE             NC1044.2
025600            " FOR OFFICIAL USE ONLY    ".                         NC1044.2
025700     02  FILLER                      PIC X(58)  VALUE             NC1044.2
025800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1044.2
025900     02  FILLER                      PIC X(28)  VALUE             NC1044.2
026000            "  COPYRIGHT   1985 ".                                NC1044.2
026100 01  CCVS-E-1.                                                    NC1044.2
026200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1044.2
026300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1044.2
026400     02 ID-AGAIN                     PIC X(9).                    NC1044.2
026500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1044.2
026600 01  CCVS-E-2.                                                    NC1044.2
026700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1044.2
026800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1044.2
026900     02 CCVS-E-2-2.                                               NC1044.2
027000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1044.2
027100         03 FILLER                   PIC X      VALUE SPACE.      NC1044.2
027200         03 ENDER-DESC               PIC X(44)  VALUE             NC1044.2
027300            "ERRORS ENCOUNTERED".                                 NC1044.2
027400 01  CCVS-E-3.                                                    NC1044.2
027500     02  FILLER                      PIC X(22)  VALUE             NC1044.2
027600            " FOR OFFICIAL USE ONLY".                             NC1044.2
027700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1044.2
027800     02  FILLER                      PIC X(58)  VALUE             NC1044.2
027900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1044.2
028000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1044.2
028100     02 FILLER                       PIC X(15)  VALUE             NC1044.2
028200             " COPYRIGHT 1985".                                   NC1044.2
028300 01  CCVS-E-4.                                                    NC1044.2
028400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1044.2
028500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1044.2
028600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1044.2
028700     02 FILLER                       PIC X(40)  VALUE             NC1044.2
028800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1044.2
028900 01  XXINFO.                                                      NC1044.2
029000     02 FILLER                       PIC X(19)  VALUE             NC1044.2
029100            "*** INFORMATION ***".                                NC1044.2
029200     02 INFO-TEXT.                                                NC1044.2
029300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1044.2
029400       04 XXCOMPUTED                 PIC X(20).                   NC1044.2
029500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1044.2
029600       04 XXCORRECT                  PIC X(20).                   NC1044.2
029700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1044.2
029800 01  HYPHEN-LINE.                                                 NC1044.2
029900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1044.2
030000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1044.2
030100-    "*****************************************".                 NC1044.2
030200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1044.2
030300-    "******************************".                            NC1044.2
030400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1044.2
030500     "NC104A".                                                    NC1044.2
030600 PROCEDURE DIVISION.                                              NC1044.2
030700 CCVS1 SECTION.                                                   NC1044.2
030800 OPEN-FILES.                                                      NC1044.2
030900     OPEN     OUTPUT PRINT-FILE.                                  NC1044.2
031000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1044.2
031100     MOVE    SPACE TO TEST-RESULTS.                               NC1044.2
031200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1044.2
031300     GO TO CCVS1-EXIT.                                            NC1044.2
031400 CLOSE-FILES.                                                     NC1044.2
031500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1044.2
031600 TERMINATE-CCVS.                                                  NC1044.2
031700*    EXIT PROGRAM.                                                NC1044.2
031800*TERMINATE-CALL.                                                  NC1044.2
031900     STOP     RUN.                                                NC1044.2
032000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1044.2
032100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1044.2
032200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1044.2
032300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1044.2
032400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1044.2
032500 PRINT-DETAIL.                                                    NC1044.2
032600     IF REC-CT NOT EQUAL TO ZERO                                  NC1044.2
032700             MOVE "." TO PARDOT-X                                 NC1044.2
032800             MOVE REC-CT TO DOTVALUE.                             NC1044.2
032900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1044.2
033000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1044.2
033100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1044.2
033200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1044.2
033300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1044.2
033400     MOVE SPACE TO CORRECT-X.                                     NC1044.2
033500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1044.2
033600     MOVE     SPACE TO RE-MARK.                                   NC1044.2
033700 HEAD-ROUTINE.                                                    NC1044.2
033800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1044.2
033900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1044.2
034000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1044.2
034100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1044.2
034200 COLUMN-NAMES-ROUTINE.                                            NC1044.2
034300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1044.2
034400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1044.2
034500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1044.2
034600 END-ROUTINE.                                                     NC1044.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1044.2
034800 END-RTN-EXIT.                                                    NC1044.2
034900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1044.2
035000 END-ROUTINE-1.                                                   NC1044.2
035100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1044.2
035200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1044.2
035300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1044.2
035400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1044.2
035500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1044.2
035600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1044.2
035700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1044.2
035800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1044.2
035900  END-ROUTINE-12.                                                 NC1044.2
036000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1044.2
036100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1044.2
036200         MOVE "NO " TO ERROR-TOTAL                                NC1044.2
036300         ELSE                                                     NC1044.2
036400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1044.2
036500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1044.2
036600     PERFORM WRITE-LINE.                                          NC1044.2
036700 END-ROUTINE-13.                                                  NC1044.2
036800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1044.2
036900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1044.2
037000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1044.2
037100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1044.2
037200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1044.2
037300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1044.2
037400          MOVE "NO " TO ERROR-TOTAL                               NC1044.2
037500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1044.2
037600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1044.2
037700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1044.2
037800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1044.2
037900 WRITE-LINE.                                                      NC1044.2
038000     ADD 1 TO RECORD-COUNT.                                       NC1044.2
038100*    IF RECORD-COUNT GREATER 42                                   NC1044.2
038200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1044.2
038300*        MOVE SPACE TO DUMMY-RECORD                               NC1044.2
038400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1044.2
038500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1044.2
038600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1044.2
038700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1044.2
038800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1044.2
038900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1044.2
039000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1044.2
039100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1044.2
039200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1044.2
039300*        MOVE ZERO TO RECORD-COUNT.                               NC1044.2
039400     PERFORM WRT-LN.                                              NC1044.2
039500 WRT-LN.                                                          NC1044.2
039600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1044.2
039700     MOVE SPACE TO DUMMY-RECORD.                                  NC1044.2
039800 BLANK-LINE-PRINT.                                                NC1044.2
039900     PERFORM WRT-LN.                                              NC1044.2
040000 FAIL-ROUTINE.                                                    NC1044.2
040100     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1044.2
040200            GO TO FAIL-ROUTINE-WRITE.                             NC1044.2
040300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1044.2
040400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1044.2
040500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1044.2
040600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1044.2
040700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1044.2
040800     GO TO  FAIL-ROUTINE-EX.                                      NC1044.2
040900 FAIL-ROUTINE-WRITE.                                              NC1044.2
041000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1044.2
041100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1044.2
041200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1044.2
041300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1044.2
041400 FAIL-ROUTINE-EX. EXIT.                                           NC1044.2
041500 BAIL-OUT.                                                        NC1044.2
041600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1044.2
041700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1044.2
041800 BAIL-OUT-WRITE.                                                  NC1044.2
041900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1044.2
042000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1044.2
042100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1044.2
042200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1044.2
042300 BAIL-OUT-EX. EXIT.                                               NC1044.2
042400 CCVS1-EXIT.                                                      NC1044.2
042500     EXIT.                                                        NC1044.2
042600 SECT-NC104A-001 SECTION.                                         NC1044.2
042700 MOVE-INIT-F1-1.                                                  NC1044.2
042800     MOVE "MOVE NUMERIC INTEGER" TO FEATURE.                      NC1044.2
042900     MOVE "V1-102 6.18.2"        TO ANSI-REFERENCE.               NC1044.2
043000     MOVE  12345 TO MOVE1.                                        NC1044.2
043100 MOVE-TEST-F1-1-0.                                                NC1044.2
043200     MOVE     MOVE1 TO MOVE2.                                     NC1044.2
043300 MOVE-TEST-F1-1-1.                                                NC1044.2
043400     IF       MOVE2 EQUAL TO 12345                                NC1044.2
043500              PERFORM PASS                                        NC1044.2
043600              ELSE                                                NC1044.2
043700              GO TO MOVE-FAIL-F1-1.                               NC1044.2
043800*    NOTE NI TO NI (NUMERIC INTEGRAL) MOVE, EQUAL SIZE.           NC1044.2
043900     GO TO    MOVE-WRITE-F1-1.                                    NC1044.2
044000 MOVE-DELETE-F1-1.                                                NC1044.2
044100     PERFORM  DE-LETE.                                            NC1044.2
044200     GO TO    MOVE-WRITE-F1-1.                                    NC1044.2
044300 MOVE-FAIL-F1-1.                                                  NC1044.2
044400     MOVE     MOVE2 TO COMPUTED-N.                                NC1044.2
044500     MOVE     12345 TO CORRECT-N.                                 NC1044.2
044600     PERFORM  FAIL.                                               NC1044.2
044700 MOVE-WRITE-F1-1.                                                 NC1044.2
044800     MOVE "MOVE-TEST-F1-1 " TO PAR-NAME.                          NC1044.2
044900     PERFORM  PRINT-DETAIL.                                       NC1044.2
045000 MOVE-INIT-F1-2.                                                  NC1044.2
045100     MOVE      12345 TO MOVE1.                                    NC1044.2
045200 MOVE-TEST-F1-2-0.                                                NC1044.2
045300     MOVE     MOVE1 TO MOVE3.                                     NC1044.2
045400 MOVE-TEST-F1-2-1.                                                NC1044.2
045500     IF       MOVE3 EQUAL TO 45                                   NC1044.2
045600              PERFORM PASS                                        NC1044.2
045700              ELSE                                                NC1044.2
045800              GO TO MOVE-FAIL-F1-2.                               NC1044.2
045900*    NOTE NI TO NI MOVE, WITH TRUNCATION.                         NC1044.2
046000     GO TO    MOVE-WRITE-F1-2.                                    NC1044.2
046100 MOVE-DELETE-F1-2.                                                NC1044.2
046200     PERFORM  DE-LETE.                                            NC1044.2
046300     GO TO    MOVE-WRITE-F1-2.                                    NC1044.2
046400 MOVE-FAIL-F1-2.                                                  NC1044.2
046500     MOVE     MOVE3 TO COMPUTED-N.                                NC1044.2
046600     MOVE     45 TO CORRECT-N.                                    NC1044.2
046700     PERFORM  FAIL.                                               NC1044.2
046800 MOVE-WRITE-F1-2.                                                 NC1044.2
046900     MOVE "MOVE-TEST-F1-2 " TO PAR-NAME.                          NC1044.2
047000     PERFORM  PRINT-DETAIL.                                       NC1044.2
047100 MOVE-INIT-F1-3.                                                  NC1044.2
047200     MOVE     12345 TO MOVE1.                                     NC1044.2
047300 MOVE-TEST-F1-3-0.                                                NC1044.2
047400     MOVE     MOVE1 TO MOVE4.                                     NC1044.2
047500 MOVE-TEST-F1-3-1.                                                NC1044.2
047600     IF       MOVE4 EQUAL TO 0012345                              NC1044.2
047700              PERFORM PASS                                        NC1044.2
047800              ELSE                                                NC1044.2
047900              GO TO MOVE-FAIL-F1-3.                               NC1044.2
048000*    NOTE NI TO NI MOVE, WITH ZERO PADDING.                       NC1044.2
048100     GO TO    MOVE-WRITE-F1-3.                                    NC1044.2
048200 MOVE-DELETE-F1-3.                                                NC1044.2
048300     PERFORM  DE-LETE.                                            NC1044.2
048400     GO TO    MOVE-WRITE-F1-3.                                    NC1044.2
048500 MOVE-FAIL-F1-3.                                                  NC1044.2
048600     MOVE     MOVE4 TO COMPUTED-N.                                NC1044.2
048700     MOVE     0012345 TO CORRECT-N.                               NC1044.2
048800     PERFORM  FAIL.                                               NC1044.2
048900 MOVE-WRITE-F1-3.                                                 NC1044.2
049000     MOVE "MOVE-TEST-F1-3 " TO PAR-NAME.                          NC1044.2
049100     PERFORM  PRINT-DETAIL.                                       NC1044.2
049200 MOVE-INIT-F1-4.                                                  NC1044.2
049300     MOVE     12345 TO MOVE1.                                     NC1044.2
049400 MOVE-TEST-F1-4-0.                                                NC1044.2
049500     MOVE     MOVE1 TO MOVE5.                                     NC1044.2
049600 MOVE-TEST-F1-4-1.                                                NC1044.2
049700     IF       MOVE5 EQUAL TO 45                                   NC1044.2
049800              PERFORM PASS                                        NC1044.2
049900              ELSE                                                NC1044.2
050000              GO TO MOVE-FAIL-F1-4.                               NC1044.2
050100*    NOTE NI TO NNI (NUMERIC NON INTEGER), LEFT TRUNCATION        NC1044.2
050200*    ZERO FILL ON RIGHT.                                          NC1044.2
050300     GO TO    MOVE-WRITE-F1-4.                                    NC1044.2
050400 MOVE-DELETE-F1-4.                                                NC1044.2
050500     PERFORM  DE-LETE.                                            NC1044.2
050600     GO TO    MOVE-WRITE-F1-4.                                    NC1044.2
050700 MOVE-FAIL-F1-4.                                                  NC1044.2
050800     MOVE     MOVE5 TO COMPUTED-N.                                NC1044.2
050900     MOVE     45 TO CORRECT-N.                                    NC1044.2
051000     PERFORM  FAIL.                                               NC1044.2
051100 MOVE-WRITE-F1-4.                                                 NC1044.2
051200     MOVE "MOVE-TEST-F1-4 " TO PAR-NAME.                          NC1044.2
051300     PERFORM  PRINT-DETAIL.                                       NC1044.2
051400 MOVE-INIT-F1-5.                                                  NC1044.2
051500     MOVE     12345 TO MOVE1.                                     NC1044.2
051600 MOVE-TEST-F1-5-0.                                                NC1044.2
051700     MOVE     MOVE1 TO MOVE48.                                    NC1044.2
051800 MOVE-TEST-F1-5-1.                                                NC1044.2
051900     IF       MOVE48 EQUAL TO 5                                   NC1044.2
052000              PERFORM PASS                                        NC1044.2
052100              ELSE                                                NC1044.2
052200              GO TO MOVE-FAIL-F1-5.                               NC1044.2
052300*    NOTE NI TO NNI MOVE, RECEIVING FIELD MAX SIZE.               NC1044.2
052400     GO TO    MOVE-WRITE-F1-5.                                    NC1044.2
052500 MOVE-DELETE-F1-5.                                                NC1044.2
052600     PERFORM  DE-LETE.                                            NC1044.2
052700     GO TO    MOVE-WRITE-F1-5.                                    NC1044.2
052800 MOVE-FAIL-F1-5.                                                  NC1044.2
052900     MOVE     MOVE48 TO COMPUTED-N.                               NC1044.2
053000     MOVE     5 TO CORRECT-N.                                     NC1044.2
053100     PERFORM  FAIL.                                               NC1044.2
053200 MOVE-WRITE-F1-5.                                                 NC1044.2
053300     MOVE "MOVE-TEST-F1-5 " TO PAR-NAME.                          NC1044.2
053400     PERFORM  PRINT-DETAIL.                                       NC1044.2
053500 MOVE-INIT-F1-6.                                                  NC1044.2
053600     MOVE     12345 TO MOVE1.                                     NC1044.2
053700 MOVE-TEST-F1-6-0.                                                NC1044.2
053800     MOVE     MOVE1 TO MOVE27.                                    NC1044.2
053900 MOVE-TEST-F1-6-1.                                                NC1044.2
054000     IF       MOVE27 EQUAL TO 2300                                NC1044.2
054100              PERFORM PASS                                        NC1044.2
054200              ELSE                                                NC1044.2
054300              GO TO MOVE-FAIL-F1-6.                               NC1044.2
054400*    NOTE NI TO NNI MOVE SCALING.                                 NC1044.2
054500     GO TO    MOVE-WRITE-F1-6.                                    NC1044.2
054600 MOVE-DELETE-F1-6.                                                NC1044.2
054700     PERFORM  DE-LETE.                                            NC1044.2
054800     GO TO    MOVE-WRITE-F1-6.                                    NC1044.2
054900 MOVE-FAIL-F1-6.                                                  NC1044.2
055000     MOVE     MOVE27 TO COMPUTED-N.                               NC1044.2
055100     MOVE     2300 TO CORRECT-N.                                  NC1044.2
055200     PERFORM  FAIL.                                               NC1044.2
055300 MOVE-WRITE-F1-6.                                                 NC1044.2
055400     MOVE "MOVE-TEST-F1-6 " TO PAR-NAME.                          NC1044.2
055500     PERFORM  PRINT-DETAIL.                                       NC1044.2
055600 MOVE-INIT-F1-7.                                                  NC1044.2
055700     MOVE     12345 TO MOVE1.                                     NC1044.2
055800 MOVE-TEST-F1-7-0.                                                NC1044.2
055900     MOVE     MOVE1 TO MOVE8.                                     NC1044.2
056000 MOVE-TEST-F1-7-1.                                                NC1044.2
056100     IF       MOVE1  EQUAL TO 12345.00                            NC1044.2
056200              PERFORM PASS                                        NC1044.2
056300              ELSE                                                NC1044.2
056400              GO TO MOVE-FAIL-F1-7.                               NC1044.2
056500*    NOTE NI TO NNI MOVE, ZERO PADDING ON RIGHT.                  NC1044.2
056600     GO TO    MOVE-WRITE-F1-7.                                    NC1044.2
056700 MOVE-DELETE-F1-7.                                                NC1044.2
056800     PERFORM  DE-LETE.                                            NC1044.2
056900     GO TO    MOVE-WRITE-F1-7.                                    NC1044.2
057000 MOVE-FAIL-F1-7.                                                  NC1044.2
057100     MOVE     MOVE8 TO COMPUTED-N.                                NC1044.2
057200     MOVE     12345.00  TO CORRECT-N.                             NC1044.2
057300     PERFORM  FAIL.                                               NC1044.2
057400 MOVE-WRITE-F1-7.                                                 NC1044.2
057500     MOVE "MOVE-TEST-F1-7 " TO PAR-NAME.                          NC1044.2
057600     PERFORM  PRINT-DETAIL.                                       NC1044.2
057700 MOVE-INIT-F1-8.                                                  NC1044.2
057800     MOVE     12345 TO MOVE1.                                     NC1044.2
057900 MOVE-TEST-F1-8-0.                                                NC1044.2
058000     MOVE     MOVE1 TO MOVE9.                                     NC1044.2
058100 MOVE-TEST-F1-8-1.                                                NC1044.2
058200     IF       MOVE9  EQUAL TO  012345.00                          NC1044.2
058300              PERFORM PASS                                        NC1044.2
058400              ELSE                                                NC1044.2
058500              GO TO MOVE-FAIL-F1-8.                               NC1044.2
058600*    NOTE NI TO NNI MOVE, ZERO PADDING LEFT AND RIGHT.            NC1044.2
058700     GO TO    MOVE-WRITE-F1-8.                                    NC1044.2
058800 MOVE-DELETE-F1-8.                                                NC1044.2
058900     PERFORM  DE-LETE.                                            NC1044.2
059000     GO TO    MOVE-WRITE-F1-8.                                    NC1044.2
059100 MOVE-FAIL-F1-8.                                                  NC1044.2
059200     MOVE     MOVE9 TO COMPUTED-N.                                NC1044.2
059300     MOVE     0012345.00 TO  CORRECT-N.                           NC1044.2
059400     PERFORM  FAIL.                                               NC1044.2
059500 MOVE-WRITE-F1-8.                                                 NC1044.2
059600     MOVE "MOVE-TEST-F1-8 " TO PAR-NAME.                          NC1044.2
059700     PERFORM  PRINT-DETAIL.                                       NC1044.2
059800 MOVE-INIT-F1-9.                                                  NC1044.2
059900     MOVE     12345 TO MOVE1.                                     NC1044.2
060000 MOVE-TEST-F1-9-0.                                                NC1044.2
060100     MOVE     MOVE1 TO MOVE10.                                    NC1044.2
060200 MOVE-TEST-F1-9-1.                                                NC1044.2
060300     IF       MOVE10 EQUAL TO "$345.00"                           NC1044.2
060400              PERFORM PASS                                        NC1044.2
060500              ELSE                                                NC1044.2
060600              GO TO MOVE-FAIL-F1-9.                               NC1044.2
060700*    NOTE NI TO NE MOVE, FIXED INSERTION, CURRENCY SIGN, PERIOD.  NC1044.2
060800     GO TO    MOVE-WRITE-F1-9.                                    NC1044.2
060900 MOVE-DELETE-F1-9.                                                NC1044.2
061000     PERFORM  DE-LETE.                                            NC1044.2
061100     GO TO    MOVE-WRITE-F1-9.                                    NC1044.2
061200 MOVE-FAIL-F1-9.                                                  NC1044.2
061300     MOVE     MOVE10 TO COMPUTED-A.                               NC1044.2
061400     MOVE "$345.00" TO CORRECT-A.                                 NC1044.2
061500     PERFORM  FAIL.                                               NC1044.2
061600 MOVE-WRITE-F1-9.                                                 NC1044.2
061700     MOVE "MOVE-TEST-F1-9 " TO PAR-NAME.                          NC1044.2
061800     PERFORM  PRINT-DETAIL.                                       NC1044.2
061900 MOVE-INIT-F1-10.                                                 NC1044.2
062000     MOVE     12345 TO MOVE1.                                     NC1044.2
062100 MOVE-TEST-F1-10-0.                                               NC1044.2
062200     MOVE     MOVE1 TO MOVE11.                                    NC1044.2
062300 MOVE-TEST-F1-10-1.                                               NC1044.2
062400     IF       MOVE11 EQUAL TO "$12,345.00"                        NC1044.2
062500              PERFORM PASS                                        NC1044.2
062600              ELSE                                                NC1044.2
062700              GO TO MOVE-FAIL-F1-10.                              NC1044.2
062800*    NOTE NI TO NE MOVE, FIXED INSERTION (CURRENCY SIGN,          NC1044.2
062900*    COMMA, PERIOD) ZERO FILL ON RIGHT.                           NC1044.2
063000     GO TO    MOVE-WRITE-F1-10.                                   NC1044.2
063100 MOVE-DELETE-F1-10.                                               NC1044.2
063200     PERFORM  DE-LETE.                                            NC1044.2
063300     GO TO    MOVE-WRITE-F1-10.                                   NC1044.2
063400 MOVE-FAIL-F1-10.                                                 NC1044.2
063500     MOVE     MOVE11 TO COMPUTED-A.                               NC1044.2
063600     MOVE "$12,345.00" TO CORRECT-A.                              NC1044.2
063700     PERFORM  FAIL.                                               NC1044.2
063800 MOVE-WRITE-F1-10.                                                NC1044.2
063900     MOVE "MOVE-TEST-F1-10" TO PAR-NAME.                          NC1044.2
064000     PERFORM  PRINT-DETAIL.                                       NC1044.2
064100 MOVE-INIT-F1-11.                                                 NC1044.2
064200     MOVE     00045 TO MOVE49.                                    NC1044.2
064300 MOVE-TEST-F1-11-0.                                               NC1044.2
064400     MOVE     MOVE49 TO MOVE12.                                   NC1044.2
064500 MOVE-TEST-F1-11-1.                                               NC1044.2
064600     IF       MOVE12 EQUAL TO "    $045"                          NC1044.2
064700              PERFORM PASS                                        NC1044.2
064800              ELSE                                                NC1044.2
064900              GO TO MOVE-FAIL-F1-11.                              NC1044.2
065000*    NOTE NI TO NE MOVE, FLOAT CURRENCY SIGN.                     NC1044.2
065100     GO TO    MOVE-WRITE-F1-11.                                   NC1044.2
065200 MOVE-DELETE-F1-11.                                               NC1044.2
065300     PERFORM  DE-LETE.                                            NC1044.2
065400     GO TO    MOVE-WRITE-F1-11.                                   NC1044.2
065500 MOVE-FAIL-F1-11.                                                 NC1044.2
065600     MOVE     MOVE12 TO COMPUTED-A.                               NC1044.2
065700     MOVE "    $045" TO CORRECT-A.                                NC1044.2
065800     PERFORM  FAIL.                                               NC1044.2
065900 MOVE-WRITE-F1-11.                                                NC1044.2
066000     MOVE "MOVE-TEST-F1-11" TO PAR-NAME.                          NC1044.2
066100     PERFORM  PRINT-DETAIL.                                       NC1044.2
066200 MOVE-INIT-F1-12.                                                 NC1044.2
066300     MOVE     00045 TO MOVE49.                                    NC1044.2
066400 MOVE-TEST-F1-12-0.                                               NC1044.2
066500     MOVE     MOVE49 TO MOVE13.                                   NC1044.2
066600 MOVE-TEST-F1-12-1.                                               NC1044.2
066700     IF       MOVE13 EQUAL TO "*****000045"                       NC1044.2
066800              PERFORM PASS                                        NC1044.2
066900              ELSE                                                NC1044.2
067000              GO TO MOVE-FAIL-F1-12.                              NC1044.2
067100*    NOTE NI TO NE MOVE, CHECK PROTECT.                           NC1044.2
067200     GO TO    MOVE-WRITE-F1-12.                                   NC1044.2
067300 MOVE-DELETE-F1-12.                                               NC1044.2
067400     PERFORM  DE-LETE.                                            NC1044.2
067500     GO TO    MOVE-WRITE-F1-12.                                   NC1044.2
067600 MOVE-FAIL-F1-12.                                                 NC1044.2
067700     MOVE     MOVE13 TO COMPUTED-A.                               NC1044.2
067800     MOVE "*****000045" TO CORRECT-A.                             NC1044.2
067900     PERFORM  FAIL.                                               NC1044.2
068000 MOVE-WRITE-F1-12.                                                NC1044.2
068100     MOVE "MOVE-TEST-F1-12" TO PAR-NAME.                          NC1044.2
068200     PERFORM  PRINT-DETAIL.                                       NC1044.2
068300 MOVE-INIT-F1-13.                                                 NC1044.2
068400     MOVE     12345 TO MOVE1.                                     NC1044.2
068500 MOVE-TEST-F1-13-0.                                               NC1044.2
068600     MOVE     MOVE1 TO MOVE14.                                    NC1044.2
068700 MOVE-TEST-F1-13-1.                                               NC1044.2
068800     IF       MOVE14 EQUAL TO "+12345"                            NC1044.2
068900              PERFORM PASS                                        NC1044.2
069000              ELSE                                                NC1044.2
069100              GO TO MOVE-FAIL-F1-13.                              NC1044.2
069200*    NOTE NI TO NE MOVE, REPORT SIGN.                             NC1044.2
069300     GO TO    MOVE-WRITE-F1-13.                                   NC1044.2
069400 MOVE-DELETE-F1-13.                                               NC1044.2
069500     PERFORM  DE-LETE.                                            NC1044.2
069600     GO TO    MOVE-WRITE-F1-13.                                   NC1044.2
069700 MOVE-FAIL-F1-13.                                                 NC1044.2
069800     MOVE     MOVE14 TO COMPUTED-A.                               NC1044.2
069900     MOVE "+12345" TO CORRECT-A.                                  NC1044.2
070000     PERFORM  FAIL.                                               NC1044.2
070100 MOVE-WRITE-F1-13.                                                NC1044.2
070200     MOVE "MOVE-TEST-F1-13" TO PAR-NAME.                          NC1044.2
070300     PERFORM  PRINT-DETAIL.                                       NC1044.2
070400 MOVE-INIT-F1-14.                                                 NC1044.2
070500     MOVE    -12345 TO MOVE51.                                    NC1044.2
070600 MOVE-TEST-F1-14-0.                                               NC1044.2
070700     MOVE     MOVE51 TO MOVE16.                                   NC1044.2
070800 MOVE-TEST-F1-14-1.                                               NC1044.2
070900     IF       MOVE16 EQUAL TO "12345CR"                           NC1044.2
071000              PERFORM PASS                                        NC1044.2
071100              ELSE                                                NC1044.2
071200              GO TO MOVE-FAIL-F1-14.                              NC1044.2
071300*    NOTE NI TO NE MOVE, REPORT SYMBOL CR.                        NC1044.2
071400     GO TO    MOVE-WRITE-F1-14.                                   NC1044.2
071500 MOVE-DELETE-F1-14.                                               NC1044.2
071600     PERFORM  DE-LETE.                                            NC1044.2
071700     GO TO    MOVE-WRITE-F1-14.                                   NC1044.2
071800 MOVE-FAIL-F1-14.                                                 NC1044.2
071900     MOVE     MOVE16 TO COMPUTED-A.                               NC1044.2
072000     MOVE "12345CR" TO CORRECT-A.                                 NC1044.2
072100     PERFORM  FAIL.                                               NC1044.2
072200 MOVE-WRITE-F1-14.                                                NC1044.2
072300     MOVE "MOVE-TEST-F1-14" TO PAR-NAME.                          NC1044.2
072400     PERFORM  PRINT-DETAIL.                                       NC1044.2
072500 MOVE-INIT-F1-15.                                                 NC1044.2
072600     MOVE    -12345 TO MOVE51.                                    NC1044.2
072700 MOVE-TEST-F1-15-0.                                               NC1044.2
072800     MOVE     MOVE51 TO MOVE52.                                   NC1044.2
072900 MOVE-TEST-F1-15-1.                                               NC1044.2
073000     IF       MOVE52 EQUAL TO "12345-"                            NC1044.2
073100              PERFORM PASS                                        NC1044.2
073200              ELSE                                                NC1044.2
073300              GO TO MOVE-FAIL-F1-15.                              NC1044.2
073400*    NOTE NI TO NE MOVE REPORT SIGN.                              NC1044.2
073500     GO TO    MOVE-WRITE-F1-15.                                   NC1044.2
073600 MOVE-DELETE-F1-15.                                               NC1044.2
073700     PERFORM  DE-LETE.                                            NC1044.2
073800     GO TO    MOVE-WRITE-F1-15.                                   NC1044.2
073900 MOVE-FAIL-F1-15.                                                 NC1044.2
074000     MOVE     MOVE52 TO COMPUTED-A.                               NC1044.2
074100     MOVE "12345-" TO CORRECT-A.                                  NC1044.2
074200     PERFORM  FAIL.                                               NC1044.2
074300 MOVE-WRITE-F1-15.                                                NC1044.2
074400     MOVE "MOVE-TEST-F1-15" TO PAR-NAME.                          NC1044.2
074500     PERFORM  PRINT-DETAIL.                                       NC1044.2
074600 MOVE-INIT-F1-16.                                                 NC1044.2
074700     MOVE     00000 TO MOVE15.                                    NC1044.2
074800 MOVE-TEST-F1-16-0.                                               NC1044.2
074900     MOVE     MOVE15 TO MOVE17.                                   NC1044.2
075000 MOVE-TEST-F1-16-1.                                               NC1044.2
075100     IF       MOVE17 EQUAL TO SPACE                               NC1044.2
075200              PERFORM PASS                                        NC1044.2
075300              ELSE                                                NC1044.2
075400              GO TO MOVE-FAIL-F1-16.                              NC1044.2
075500*    NOTE NI TO NE MOVE, BLANK WHEN ZERO CLAUSE.                  NC1044.2
075600     GO TO    MOVE-WRITE-F1-16.                                   NC1044.2
075700 MOVE-DELETE-F1-16.                                               NC1044.2
075800     PERFORM  DE-LETE.                                            NC1044.2
075900     GO TO    MOVE-WRITE-F1-16.                                   NC1044.2
076000 MOVE-FAIL-F1-16.                                                 NC1044.2
076100     MOVE     MOVE17 TO COMPUTED-A.                               NC1044.2
076200     MOVE     SPACE TO CORRECT-A.                                 NC1044.2
076300     PERFORM  FAIL.                                               NC1044.2
076400 MOVE-WRITE-F1-16.                                                NC1044.2
076500     MOVE "MOVE-TEST-F1-16" TO PAR-NAME.                          NC1044.2
076600     PERFORM  PRINT-DETAIL.                                       NC1044.2
076700 MOVE-INIT-F1-17.                                                 NC1044.2
076800     MOVE     00000 TO MOVE15.                                    NC1044.2
076900 MOVE-TEST-F1-17-0.                                               NC1044.2
077000     MOVE     MOVE15 TO MOVE18.                                   NC1044.2
077100 MOVE-TEST-F1-17-1.                                               NC1044.2
077200     IF       MOVE18 EQUAL TO SPACE                               NC1044.2
077300              PERFORM PASS                                        NC1044.2
077400              ELSE                                                NC1044.2
077500              GO TO MOVE-FAIL-F1-17.                              NC1044.2
077600*    NOTE NI TO NE MOVE, BLANK WHEN ZERO PICTURE.                 NC1044.2
077700     GO TO    MOVE-WRITE-F1-17.                                   NC1044.2
077800 MOVE-DELETE-F1-17.                                               NC1044.2
077900     PERFORM  DE-LETE.                                            NC1044.2
078000     GO TO    MOVE-WRITE-F1-17.                                   NC1044.2
078100 MOVE-FAIL-F1-17.                                                 NC1044.2
078200     MOVE     MOVE18 TO COMPUTED-A.                               NC1044.2
078300     MOVE     SPACE TO CORRECT-A.                                 NC1044.2
078400     PERFORM  FAIL.                                               NC1044.2
078500 MOVE-WRITE-F1-17.                                                NC1044.2
078600     MOVE "MOVE-TEST-F1-17" TO PAR-NAME.                          NC1044.2
078700     PERFORM  PRINT-DETAIL.                                       NC1044.2
078800 MOVE-INIT-F1-18.                                                 NC1044.2
078900     MOVE     12345 TO MOVE1.                                     NC1044.2
079000 MOVE-TEST-F1-18-0.                                               NC1044.2
079100     MOVE     MOVE1 TO MOVE19.                                    NC1044.2
079200 MOVE-TEST-F1-18-1.                                               NC1044.2
079300     IF       MOVE19 EQUAL TO 12345                               NC1044.2
079400              PERFORM PASS                                        NC1044.2
079500              ELSE                                                NC1044.2
079600              GO TO MOVE-FAIL-F1-18.                              NC1044.2
079700*    NOTE NI TO AN MOVE, EQUAL SIZE.                              NC1044.2
079800     GO TO    MOVE-WRITE-F1-18.                                   NC1044.2
079900 MOVE-DELETE-F1-18.                                               NC1044.2
080000     PERFORM  DE-LETE.                                            NC1044.2
080100     GO TO    MOVE-WRITE-F1-18.                                   NC1044.2
080200 MOVE-FAIL-F1-18.                                                 NC1044.2
080300     MOVE     MOVE19 TO COMPUTED-N.                               NC1044.2
080400     MOVE     12345 TO CORRECT-N.                                 NC1044.2
080500     PERFORM  FAIL.                                               NC1044.2
080600 MOVE-WRITE-F1-18.                                                NC1044.2
080700     MOVE "MOVE-TEST-F1-18" TO PAR-NAME.                          NC1044.2
080800     PERFORM  PRINT-DETAIL.                                       NC1044.2
080900 MOVE-INIT-F1-19.                                                 NC1044.2
081000     MOVE     12345 TO MOVE1.                                     NC1044.2
081100 MOVE-TEST-F1-19-0.                                               NC1044.2
081200     MOVE     MOVE1 TO MOVE20.                                    NC1044.2
081300 MOVE-TEST-F1-19-1.                                               NC1044.2
081400     IF       MOVE20 EQUAL TO 1234                                NC1044.2
081500              PERFORM PASS                                        NC1044.2
081600              ELSE                                                NC1044.2
081700              GO TO MOVE-FAIL-F1-19.                              NC1044.2
081800*    NOTE NI TO AN MOVE, TRUNCATION ON RIGHT.                     NC1044.2
081900     GO TO    MOVE-WRITE-F1-19.                                   NC1044.2
082000 MOVE-DELETE-F1-19.                                               NC1044.2
082100     PERFORM  DE-LETE.                                            NC1044.2
082200     GO TO    MOVE-WRITE-F1-19.                                   NC1044.2
082300 MOVE-FAIL-F1-19.                                                 NC1044.2
082400     MOVE     MOVE20 TO COMPUTED-N.                               NC1044.2
082500     MOVE     1234 TO CORRECT-N.                                  NC1044.2
082600     PERFORM  FAIL.                                               NC1044.2
082700 MOVE-WRITE-F1-19.                                                NC1044.2
082800     MOVE "MOVE-TEST-F1-19" TO PAR-NAME.                          NC1044.2
082900     PERFORM  PRINT-DETAIL.                                       NC1044.2
083000 MOVE-INIT-F1-20.                                                 NC1044.2
083100     MOVE     12345 TO MOVE1.                                     NC1044.2
083200 MOVE-TEST-F1-20-0.                                               NC1044.2
083300     MOVE     MOVE1 TO MOVE21.                                    NC1044.2
083400 MOVE-TEST-F1-20-1.                                               NC1044.2
083500     IF       MOVE21 EQUAL TO "12345  "                           NC1044.2
083600              PERFORM PASS                                        NC1044.2
083700              ELSE                                                NC1044.2
083800              GO TO MOVE-FAIL-F1-20.                              NC1044.2
083900*    NOTE NI TO AN MOVE, SPACE PADDING ON RIGHT.                  NC1044.2
084000     GO TO    MOVE-WRITE-F1-20.                                   NC1044.2
084100 MOVE-DELETE-F1-20.                                               NC1044.2
084200     PERFORM  DE-LETE.                                            NC1044.2
084300     GO TO    MOVE-WRITE-F1-20.                                   NC1044.2
084400 MOVE-FAIL-F1-20.                                                 NC1044.2
084500     MOVE     MOVE21 TO COMPUTED-A.                               NC1044.2
084600     MOVE "12345  " TO CORRECT-A.                                 NC1044.2
084700     PERFORM  FAIL.                                               NC1044.2
084800 MOVE-WRITE-F1-20.                                                NC1044.2
084900     MOVE "MOVE-TEST-F1-20" TO PAR-NAME.                          NC1044.2
085000     PERFORM  PRINT-DETAIL.                                       NC1044.2
085100 MOVE-INIT-F1-21.                                                 NC1044.2
085200     MOVE     12345 TO MOVE1.                                     NC1044.2
085300 MOVE-TEST-F1-21-0.                                               NC1044.2
085400     MOVE     MOVE1 TO MOVE22.                                    NC1044.2
085500 MOVE-TEST-F1-21-1.                                               NC1044.2
085600     IF       MOVE22 EQUAL TO "1 203 405"                         NC1044.2
085700              PERFORM PASS                                        NC1044.2
085800              ELSE                                                NC1044.2
085900              GO TO MOVE-FAIL-F1-21.                              NC1044.2
086000*    NOTE NI TO AE MOVE, ZERO AND SPACE INSERTION.                NC1044.2
086100     GO TO    MOVE-WRITE-F1-21.                                   NC1044.2
086200 MOVE-DELETE-F1-21.                                               NC1044.2
086300     PERFORM  DE-LETE.                                            NC1044.2
086400     GO TO    MOVE-WRITE-F1-21.                                   NC1044.2
086500 MOVE-FAIL-F1-21.                                                 NC1044.2
086600     MOVE     MOVE22 TO COMPUTED-A.                               NC1044.2
086700     MOVE "1 203 405" TO CORRECT-A.                               NC1044.2
086800     PERFORM  FAIL.                                               NC1044.2
086900 MOVE-WRITE-F1-21.                                                NC1044.2
087000     MOVE "MOVE-TEST-F1-21" TO PAR-NAME.                          NC1044.2
087100     PERFORM  PRINT-DETAIL.                                       NC1044.2
087200 MOVE-INIT-F1-22.                                                 NC1044.2
087300     MOVE     123.45 TO MOVE23.                                   NC1044.2
087400     MOVE    "MOVE NUM NON-INTEGER" TO FEATURE.                   NC1044.2
087500 MOVE-TEST-F1-22-0.                                               NC1044.2
087600     MOVE     MOVE23 TO MOVE4.                                    NC1044.2
087700 MOVE-TEST-F1-22-1.                                               NC1044.2
087800     IF       MOVE4 EQUAL TO 0000123                              NC1044.2
087900              PERFORM PASS                                        NC1044.2
088000              ELSE                                                NC1044.2
088100              GO TO MOVE-FAIL-F1-22.                              NC1044.2
088200*    NOTE NNI TO NI MOVE, ZERO PADDING ON LEFT, TRUNCATION.       NC1044.2
088300     GO TO    MOVE-WRITE-F1-22.                                   NC1044.2
088400 MOVE-DELETE-F1-22.                                               NC1044.2
088500     PERFORM  DE-LETE.                                            NC1044.2
088600     GO TO    MOVE-WRITE-F1-22.                                   NC1044.2
088700 MOVE-FAIL-F1-22.                                                 NC1044.2
088800     MOVE     MOVE23 TO COMPUTED-N.                               NC1044.2
088900     MOVE     0000123 TO CORRECT-N.                               NC1044.2
089000     PERFORM  FAIL.                                               NC1044.2
089100 MOVE-WRITE-F1-22.                                                NC1044.2
089200     MOVE "MOVE-TEST-F1-22" TO PAR-NAME.                          NC1044.2
089300     PERFORM  PRINT-DETAIL.                                       NC1044.2
089400 MOVE-INIT-F1-23.                                                 NC1044.2
089500     MOVE     123.45 TO MOVE23.                                   NC1044.2
089600 MOVE-TEST-F1-23-0.                                               NC1044.2
089700     MOVE     MOVE23 TO MOVE25.                                   NC1044.2
089800 MOVE-TEST-F1-23-1.                                               NC1044.2
089900     IF       MOVE25 EQUAL TO 123                                 NC1044.2
090000              PERFORM PASS                                        NC1044.2
090100              ELSE                                                NC1044.2
090200              GO TO MOVE-FAIL-F1-23.                              NC1044.2
090300*    NOTE NNI TO NI MOVE, TRUNCATION ON RIGHT.                    NC1044.2
090400     GO TO    MOVE-WRITE-F1-23.                                   NC1044.2
090500 MOVE-DELETE-F1-23.                                               NC1044.2
090600     PERFORM  DE-LETE.                                            NC1044.2
090700     GO TO    MOVE-WRITE-F1-23.                                   NC1044.2
090800 MOVE-FAIL-F1-23.                                                 NC1044.2
090900     MOVE     MOVE25 TO COMPUTED-N.                               NC1044.2
091000     MOVE     123 TO CORRECT-N.                                   NC1044.2
091100     PERFORM  FAIL.                                               NC1044.2
091200 MOVE-WRITE-F1-23.                                                NC1044.2
091300     MOVE "MOVE-TEST-F1-23" TO PAR-NAME.                          NC1044.2
091400     PERFORM  PRINT-DETAIL.                                       NC1044.2
091500 MOVE-INIT-F1-24.                                                 NC1044.2
091600     MOVE     123.45 TO MOVE23.                                   NC1044.2
091700 MOVE-TEST-F1-24-0.                                               NC1044.2
091800     MOVE     MOVE23 TO MOVE3.                                    NC1044.2
091900 MOVE-TEST-F1-24-1.                                               NC1044.2
092000     IF       MOVE3 EQUAL TO 23                                   NC1044.2
092100              PERFORM PASS                                        NC1044.2
092200              ELSE                                                NC1044.2
092300              GO TO MOVE-FAIL-F1-24.                              NC1044.2
092400*    NOTE NNI TO NI MOVE, TRUNCATION LEFT AND RIGHT.              NC1044.2
092500     GO TO    MOVE-WRITE-F1-24.                                   NC1044.2
092600 MOVE-DELETE-F1-24.                                               NC1044.2
092700     PERFORM  DE-LETE.                                            NC1044.2
092800     GO TO    MOVE-WRITE-F1-24.                                   NC1044.2
092900 MOVE-FAIL-F1-24.                                                 NC1044.2
093000     MOVE     MOVE3 TO COMPUTED-N.                                NC1044.2
093100     MOVE     23 TO CORRECT-N.                                    NC1044.2
093200     PERFORM  FAIL.                                               NC1044.2
093300 MOVE-WRITE-F1-24.                                                NC1044.2
093400     MOVE "MOVE-TEST-F1-24" TO PAR-NAME.                          NC1044.2
093500     PERFORM  PRINT-DETAIL.                                       NC1044.2
093600 MOVE-INIT-F1-25.                                                 NC1044.2
093700     MOVE     123.45 TO MOVE23.                                   NC1044.2
093800 MOVE-TEST-F1-25-0.                                               NC1044.2
093900     MOVE     MOVE23 TO MOVE27.                                   NC1044.2
094000 MOVE-TEST-F1-25-1.                                               NC1044.2
094100     IF       MOVE27 EQUAL TO 0100                                NC1044.2
094200              PERFORM PASS                                        NC1044.2
094300              ELSE                                                NC1044.2
094400              GO TO MOVE-FAIL-F1-25.                              NC1044.2
094500*    NOTE NNI TO NNI MOVE, SCALING.                               NC1044.2
094600     GO TO    MOVE-WRITE-F1-25.                                   NC1044.2
094700 MOVE-DELETE-F1-25.                                               NC1044.2
094800     PERFORM  DE-LETE.                                            NC1044.2
094900     GO TO    MOVE-WRITE-F1-25.                                   NC1044.2
095000 MOVE-FAIL-F1-25.                                                 NC1044.2
095100     MOVE     MOVE27 TO COMPUTED-N.                               NC1044.2
095200     MOVE     0100  TO CORRECT-N.                                 NC1044.2
095300     PERFORM  FAIL.                                               NC1044.2
095400 MOVE-WRITE-F1-25.                                                NC1044.2
095500     MOVE "MOVE-TEST-F1-25" TO PAR-NAME.                          NC1044.2
095600     PERFORM  PRINT-DETAIL.                                       NC1044.2
095700 MOVE-INIT-F1-26.                                                 NC1044.2
095800     MOVE     123.45 TO MOVE23.                                   NC1044.2
095900 MOVE-TEST-F1-26-0.                                               NC1044.2
096000     MOVE     MOVE23 TO MOVE6.                                    NC1044.2
096100 MOVE-TEST-F1-26-1.                                               NC1044.2
096200     IF       MOVE6  EQUAL TO .45000                              NC1044.2
096300              PERFORM PASS                                        NC1044.2
096400              ELSE                                                NC1044.2
096500              GO TO MOVE-FAIL-F1-26.                              NC1044.2
096600*    NOTE NNI TO NNI MOVE, TRUNCATION ON LEFT AND ZERO            NC1044.2
096700*    FILL ON RIGHT.                                               NC1044.2
096800     GO TO    MOVE-WRITE-F1-26.                                   NC1044.2
096900 MOVE-DELETE-F1-26.                                               NC1044.2
097000     PERFORM  DE-LETE.                                            NC1044.2
097100     GO TO    MOVE-WRITE-F1-26.                                   NC1044.2
097200 MOVE-FAIL-F1-26.                                                 NC1044.2
097300     MOVE     MOVE6 TO COMPUTED-N.                                NC1044.2
097400     MOVE     .45000  TO CORRECT-N.                               NC1044.2
097500     PERFORM  FAIL.                                               NC1044.2
097600 MOVE-WRITE-F1-26.                                                NC1044.2
097700     MOVE "MOVE-TEST-F1-26" TO PAR-NAME.                          NC1044.2
097800     PERFORM  PRINT-DETAIL.                                       NC1044.2
097900 MOVE-INIT-F1-27.                                                 NC1044.2
098000     MOVE     123.45 TO MOVE23.                                   NC1044.2
098100 MOVE-TEST-F1-27-0.                                               NC1044.2
098200     MOVE     MOVE23 TO MOVE29.                                   NC1044.2
098300 MOVE-TEST-F1-27-1.                                               NC1044.2
098400     IF       MOVE29 EQUAL TO 0123.450                            NC1044.2
098500              PERFORM PASS                                        NC1044.2
098600              ELSE                                                NC1044.2
098700              GO TO MOVE-FAIL-F1-27.                              NC1044.2
098800*    NOTE NNI TO NNI MOVE, ZERO PADDING ON LEFT AND RIGHT.        NC1044.2
098900     GO TO    MOVE-WRITE-F1-27.                                   NC1044.2
099000 MOVE-DELETE-F1-27.                                               NC1044.2
099100     PERFORM  DE-LETE.                                            NC1044.2
099200     GO TO    MOVE-WRITE-F1-27.                                   NC1044.2
099300 MOVE-FAIL-F1-27.                                                 NC1044.2
099400     MOVE     MOVE29 TO COMPUTED-N.                               NC1044.2
099500     MOVE     0123.450  TO CORRECT-N.                             NC1044.2
099600     PERFORM  FAIL.                                               NC1044.2
099700 MOVE-WRITE-F1-27.                                                NC1044.2
099800     MOVE "MOVE-TEST-F1-27" TO PAR-NAME.                          NC1044.2
099900     PERFORM  PRINT-DETAIL.                                       NC1044.2
100000 MOVE-INIT-F1-28.                                                 NC1044.2
100100     MOVE     123.45 TO MOVE23.                                   NC1044.2
100200 MOVE-TEST-F1-28-0.                                               NC1044.2
100300     MOVE     MOVE23 TO MOVE11.                                   NC1044.2
100400 MOVE-TEST-F1-28-1.                                               NC1044.2
100500     IF       MOVE11 EQUAL TO "$00,123.45"                        NC1044.2
100600              PERFORM PASS                                        NC1044.2
100700              ELSE                                                NC1044.2
100800              GO TO MOVE-FAIL-F1-28.                              NC1044.2
100900*    NOTE NNI TO NE MOVE, FIXED INSERTION, ZERO PADDING ON LEFT.  NC1044.2
101000     GO TO    MOVE-WRITE-F1-28.                                   NC1044.2
101100 MOVE-DELETE-F1-28.                                               NC1044.2
101200     PERFORM  DE-LETE.                                            NC1044.2
101300     GO TO    MOVE-WRITE-F1-28.                                   NC1044.2
101400 MOVE-FAIL-F1-28.                                                 NC1044.2
101500     MOVE     MOVE11 TO COMPUTED-A.                               NC1044.2
101600     MOVE "$00,123.45" TO CORRECT-A.                              NC1044.2
101700     PERFORM  FAIL.                                               NC1044.2
101800 MOVE-WRITE-F1-28.                                                NC1044.2
101900     MOVE "MOVE-TEST-F1-28" TO PAR-NAME.                          NC1044.2
102000     PERFORM  PRINT-DETAIL.                                       NC1044.2
102100 MOVE-INIT-F1-29.                                                 NC1044.2
102200     MOVE     123.45 TO MOVE23.                                   NC1044.2
102300 MOVE-TEST-F1-29-0.                                               NC1044.2
102400     MOVE     MOVE30 TO MOVE21.                                   NC1044.2
102500 MOVE-TEST-F1-29-1.                                               NC1044.2
102600     IF       MOVE21 EQUAL TO "$123.45"                           NC1044.2
102700              PERFORM PASS                                        NC1044.2
102800              ELSE                                                NC1044.2
102900              GO TO MOVE-FAIL-F1-29.                              NC1044.2
103000*    NOTE NE TO AN MOVE, EQUAL SIZE.                              NC1044.2
103100     GO TO    MOVE-WRITE-F1-29.                                   NC1044.2
103200 MOVE-DELETE-F1-29.                                               NC1044.2
103300     PERFORM  DE-LETE.                                            NC1044.2
103400     GO TO    MOVE-WRITE-F1-29.                                   NC1044.2
103500 MOVE-FAIL-F1-29.                                                 NC1044.2
103600     MOVE     MOVE21 TO COMPUTED-A.                               NC1044.2
103700     MOVE "$123.45" TO CORRECT-A.                                 NC1044.2
103800     PERFORM  FAIL.                                               NC1044.2
103900 MOVE-WRITE-F1-29.                                                NC1044.2
104000     MOVE "MOVE-TEST-F1-29" TO PAR-NAME.                          NC1044.2
104100     PERFORM  PRINT-DETAIL.                                       NC1044.2
104200 MOVE-INIT-F1-30.                                                 NC1044.2
104300     MOVE   "$123.45" TO MOVE29A.                                 NC1044.2
104400 MOVE-TEST-F1-30-0.                                               NC1044.2
104500     MOVE     MOVE30 TO MOVE31.                                   NC1044.2
104600 MOVE-TEST-F1-30-1.                                               NC1044.2
104700     IF       MOVE31 EQUAL TO "$123.45  "                         NC1044.2
104800              PERFORM PASS                                        NC1044.2
104900              ELSE                                                NC1044.2
105000              GO TO MOVE-FAIL-F1-30.                              NC1044.2
105100*    NOTE NE TO AN MOVE, SPACE PADDING ON RIGHT.                  NC1044.2
105200     GO TO    MOVE-WRITE-F1-30.                                   NC1044.2
105300 MOVE-DELETE-F1-30.                                               NC1044.2
105400     PERFORM  DE-LETE.                                            NC1044.2
105500     GO TO    MOVE-WRITE-F1-30.                                   NC1044.2
105600 MOVE-FAIL-F1-30.                                                 NC1044.2
105700     MOVE     MOVE31 TO COMPUTED-A.                               NC1044.2
105800     MOVE "$123.45  " TO CORRECT-A.                               NC1044.2
105900     PERFORM  FAIL.                                               NC1044.2
106000 MOVE-WRITE-F1-30.                                                NC1044.2
106100     MOVE "MOVE-TEST-F1-30" TO PAR-NAME.                          NC1044.2
106200     PERFORM  PRINT-DETAIL.                                       NC1044.2
106300 MOVE-INIT-F1-31.                                                 NC1044.2
106400     MOVE   "$123.45" TO MOVE29A.                                 NC1044.2
106500 MOVE-TEST-F1-31-0.                                               NC1044.2
106600     MOVE     MOVE30 TO MOVE20.                                   NC1044.2
106700 MOVE-TEST-F1-31-1.                                               NC1044.2
106800     IF       MOVE20 EQUAL TO "$123"                              NC1044.2
106900              PERFORM PASS                                        NC1044.2
107000              ELSE                                                NC1044.2
107100              GO TO MOVE-FAIL-F1-31.                              NC1044.2
107200*    NOTE NE TO AN MOVE, TRUNCATION ON RIGHT.                     NC1044.2
107300     GO TO    MOVE-WRITE-F1-31.                                   NC1044.2
107400 MOVE-DELETE-F1-31.                                               NC1044.2
107500     PERFORM  DE-LETE.                                            NC1044.2
107600     GO TO    MOVE-WRITE-F1-31.                                   NC1044.2
107700 MOVE-FAIL-F1-31.                                                 NC1044.2
107800     MOVE     MOVE20 TO COMPUTED-A.                               NC1044.2
107900     MOVE "$123" TO CORRECT-A.                                    NC1044.2
108000     PERFORM  FAIL.                                               NC1044.2
108100 MOVE-WRITE-F1-31.                                                NC1044.2
108200     MOVE "MOVE-TEST-F1-31" TO PAR-NAME.                          NC1044.2
108300     PERFORM  PRINT-DETAIL.                                       NC1044.2
108400 MOVE-INIT-F1-32.                                                 NC1044.2
108500     MOVE   "$123.45" TO MOVE29A.                                 NC1044.2
108600 MOVE-TEST-F1-32-0.                                               NC1044.2
108700     MOVE     MOVE30 TO MOVE24.                                   NC1044.2
108800 MOVE-TEST-F1-32-1.                                               NC1044.2
108900     IF       MOVE24 EQUAL TO "$ 123 000.45 "                     NC1044.2
109000              PERFORM PASS                                        NC1044.2
109100              ELSE                                                NC1044.2
109200              GO TO MOVE-FAIL-F1-32.                              NC1044.2
109300*    NOTE NE TO AE MOVE, SPACE AND ZERO INSERTION.                NC1044.2
109400     GO TO    MOVE-WRITE-F1-32.                                   NC1044.2
109500 MOVE-DELETE-F1-32.                                               NC1044.2
109600     PERFORM  DE-LETE.                                            NC1044.2
109700     GO TO    MOVE-WRITE-F1-32.                                   NC1044.2
109800 MOVE-FAIL-F1-32.                                                 NC1044.2
109900     MOVE     MOVE24 TO COMPUTED-A.                               NC1044.2
110000     MOVE "$ 123 000.45 " TO CORRECT-A.                           NC1044.2
110100     PERFORM  FAIL.                                               NC1044.2
110200 MOVE-WRITE-F1-32.                                                NC1044.2
110300     MOVE "MOVE-TEST-F1-32" TO PAR-NAME.                          NC1044.2
110400     PERFORM  PRINT-DETAIL.                                       NC1044.2
110500 MOVE-INIT-F1-33.                                                 NC1044.2
110600     MOVE    "MOVE ALPHANUMERIC   " TO FEATURE.                   NC1044.2
110700     MOVE    "12345" TO MOVE50.                                   NC1044.2
110800 MOVE-TEST-F1-33-0.                                               NC1044.2
110900     MOVE     MOVE50 TO MOVE2.                                    NC1044.2
111000 MOVE-TEST-F1-33-1.                                               NC1044.2
111100     IF       MOVE2 EQUAL TO 12345                                NC1044.2
111200              PERFORM PASS                                        NC1044.2
111300              ELSE                                                NC1044.2
111400              GO TO MOVE-FAIL-F1-33.                              NC1044.2
111500*    NOTE AN TO NI MOVE, EQUAL SIZE.                              NC1044.2
111600     GO TO    MOVE-WRITE-F1-33.                                   NC1044.2
111700 MOVE-DELETE-F1-33.                                               NC1044.2
111800     PERFORM  DE-LETE.                                            NC1044.2
111900     GO TO    MOVE-WRITE-F1-33.                                   NC1044.2
112000 MOVE-FAIL-F1-33.                                                 NC1044.2
112100     MOVE     MOVE2 TO COMPUTED-N.                                NC1044.2
112200     MOVE     12345 TO CORRECT-N.                                 NC1044.2
112300     PERFORM  FAIL.                                               NC1044.2
112400 MOVE-WRITE-F1-33.                                                NC1044.2
112500     MOVE "MOVE-TEST-F1-33" TO PAR-NAME.                          NC1044.2
112600     PERFORM  PRINT-DETAIL.                                       NC1044.2
112700 MOVE-INIT-F1-34.                                                 NC1044.2
112800     MOVE    "12345" TO MOVE50.                                   NC1044.2
112900 MOVE-TEST-F1-34-0.                                               NC1044.2
113000     MOVE     MOVE50 TO MOVE4.                                    NC1044.2
113100 MOVE-TEST-F1-34-1.                                               NC1044.2
113200     IF       MOVE4 EQUAL TO 0012345                              NC1044.2
113300              PERFORM PASS                                        NC1044.2
113400              ELSE                                                NC1044.2
113500              GO TO MOVE-FAIL-F1-34.                              NC1044.2
113600*    NOTE AN TO NI MOVE, ZERO PADDING ON LEFT.                    NC1044.2
113700     GO TO    MOVE-WRITE-F1-34.                                   NC1044.2
113800 MOVE-DELETE-F1-34.                                               NC1044.2
113900     PERFORM  DE-LETE.                                            NC1044.2
114000     GO TO    MOVE-WRITE-F1-34.                                   NC1044.2
114100 MOVE-FAIL-F1-34.                                                 NC1044.2
114200     MOVE     MOVE4 TO COMPUTED-N.                                NC1044.2
114300     MOVE     0012345  TO CORRECT-N.                              NC1044.2
114400     PERFORM  FAIL.                                               NC1044.2
114500 MOVE-WRITE-F1-34.                                                NC1044.2
114600     MOVE "MOVE-TEST-F1-34" TO PAR-NAME.                          NC1044.2
114700     PERFORM  PRINT-DETAIL.                                       NC1044.2
114800 MOVE-INIT-F1-35.                                                 NC1044.2
114900     MOVE    "12345" TO MOVE50.                                   NC1044.2
115000 MOVE-TEST-F1-35-0.                                               NC1044.2
115100     MOVE     MOVE50 TO MOVE3.                                    NC1044.2
115200 MOVE-TEST-F1-35-1.                                               NC1044.2
115300     IF       MOVE3 EQUAL TO 45                                   NC1044.2
115400              PERFORM PASS                                        NC1044.2
115500              ELSE                                                NC1044.2
115600              GO TO MOVE-FAIL-F1-35.                              NC1044.2
115700*    NOTE AN TO NI MOVE, TRUNCATION ON LEFT.                      NC1044.2
115800     GO TO    MOVE-WRITE-F1-35.                                   NC1044.2
115900 MOVE-DELETE-F1-35.                                               NC1044.2
116000     PERFORM  DE-LETE.                                            NC1044.2
116100     GO TO    MOVE-WRITE-F1-35.                                   NC1044.2
116200 MOVE-FAIL-F1-35.                                                 NC1044.2
116300     MOVE     MOVE50 TO COMPUTED-N.                               NC1044.2
116400     MOVE     45 TO CORRECT-N.                                    NC1044.2
116500     PERFORM  FAIL.                                               NC1044.2
116600 MOVE-WRITE-F1-35.                                                NC1044.2
116700     MOVE "MOVE-TEST-F1-35" TO PAR-NAME.                          NC1044.2
116800     PERFORM  PRINT-DETAIL.                                       NC1044.2
116900 MOVE-INIT-F1-36.                                                 NC1044.2
117000     MOVE    "12345" TO MOVE50.                                   NC1044.2
117100 MOVE-TEST-F1-36-0.                                               NC1044.2
117200     MOVE     MOVE50 TO MOVE26.                                   NC1044.2
117300 MOVE-TEST-F1-36-1.                                               NC1044.2
117400     IF       MOVE26 EQUAL TO 345.00                              NC1044.2
117500              PERFORM PASS                                        NC1044.2
117600              ELSE                                                NC1044.2
117700              GO TO MOVE-FAIL-F1-36.                              NC1044.2
117800*    NOTE AN TO NNI MOVE, ZERO FILL RIGHT, TRUNCATION LEFT.       NC1044.2
117900     GO TO    MOVE-WRITE-F1-36.                                   NC1044.2
118000 MOVE-DELETE-F1-36.                                               NC1044.2
118100     PERFORM  DE-LETE.                                            NC1044.2
118200     GO TO    MOVE-WRITE-F1-36.                                   NC1044.2
118300 MOVE-FAIL-F1-36.                                                 NC1044.2
118400     MOVE     MOVE26 TO COMPUTED-N.                               NC1044.2
118500     MOVE   345.00  TO CORRECT-N.                                 NC1044.2
118600     PERFORM  FAIL.                                               NC1044.2
118700 MOVE-WRITE-F1-36.                                                NC1044.2
118800     MOVE "MOVE-TEST-F1-36" TO PAR-NAME.                          NC1044.2
118900     PERFORM  PRINT-DETAIL.                                       NC1044.2
119000 MOVE-INIT-F1-37.                                                 NC1044.2
119100     MOVE    "12345" TO MOVE50.                                   NC1044.2
119200 MOVE-TEST-F1-37-0.                                               NC1044.2
119300     MOVE     MOVE50 TO MOVE9.                                    NC1044.2
119400 MOVE-TEST-F1-37-1.                                               NC1044.2
119500     IF       MOVE9 EQUAL TO  0012345.00                          NC1044.2
119600              PERFORM PASS                                        NC1044.2
119700              ELSE                                                NC1044.2
119800              GO TO MOVE-FAIL-F1-37.                              NC1044.2
119900*    NOTE AN TO NNI MOVE, ZERO PADDING LEFT AND RIGHT.            NC1044.2
120000     GO TO    MOVE-WRITE-F1-37.                                   NC1044.2
120100 MOVE-DELETE-F1-37.                                               NC1044.2
120200     PERFORM  DE-LETE.                                            NC1044.2
120300     GO TO    MOVE-WRITE-F1-37.                                   NC1044.2
120400 MOVE-FAIL-F1-37.                                                 NC1044.2
120500     MOVE     MOVE9 TO COMPUTED-N.                                NC1044.2
120600     MOVE  0012345.00   TO CORRECT-N.                             NC1044.2
120700     PERFORM  FAIL.                                               NC1044.2
120800 MOVE-WRITE-F1-37.                                                NC1044.2
120900     MOVE "MOVE-TEST-F1-37" TO PAR-NAME.                          NC1044.2
121000     PERFORM  PRINT-DETAIL.                                       NC1044.2
121100 MOVE-INIT-F1-38.                                                 NC1044.2
121200     MOVE    "12345" TO MOVE50.                                   NC1044.2
121300 MOVE-TEST-F1-38-0.                                               NC1044.2
121400     MOVE     MOVE50 TO MOVE16.                                   NC1044.2
121500 MOVE-TEST-F1-38-1.                                               NC1044.2
121600     IF       MOVE16 EQUAL TO "12345  "                           NC1044.2
121700              PERFORM PASS                                        NC1044.2
121800              ELSE                                                NC1044.2
121900              GO TO MOVE-FAIL-F1-38.                              NC1044.2
122000*    NOTE AN TO NE WITH CR SYMBOL.                                NC1044.2
122100     GO TO    MOVE-WRITE-F1-38.                                   NC1044.2
122200 MOVE-DELETE-F1-38.                                               NC1044.2
122300     PERFORM  DE-LETE.                                            NC1044.2
122400     GO TO    MOVE-WRITE-F1-38.                                   NC1044.2
122500 MOVE-FAIL-F1-38.                                                 NC1044.2
122600     MOVE     MOVE16 TO COMPUTED-A.                               NC1044.2
122700     MOVE "12345  " TO CORRECT-A.                                 NC1044.2
122800     PERFORM  FAIL.                                               NC1044.2
122900 MOVE-WRITE-F1-38.                                                NC1044.2
123000     MOVE "MOVE-TEST-F1-38" TO PAR-NAME.                          NC1044.2
123100     PERFORM  PRINT-DETAIL.                                       NC1044.2
123200 MOVE-INIT-F1-39.                                                 NC1044.2
123300     MOVE    "12345" TO MOVE50.                                   NC1044.2
123400 MOVE-TEST-F1-39-0.                                               NC1044.2
123500     MOVE     MOVE50 TO MOVE11.                                   NC1044.2
123600 MOVE-TEST-F1-39-1.                                               NC1044.2
123700     IF       MOVE11 EQUAL TO "$12,345.00"                        NC1044.2
123800              PERFORM PASS                                        NC1044.2
123900              ELSE                                                NC1044.2
124000              GO TO MOVE-FAIL-F1-39.                              NC1044.2
124100*    NOTE AN TO NNI MOVE, INSERTION CHARACTERS AND ZERO PADDING   NC1044.2
124200*    ON RIGHT.                                                    NC1044.2
124300     GO TO    MOVE-WRITE-F1-39.                                   NC1044.2
124400 MOVE-DELETE-F1-39.                                               NC1044.2
124500     PERFORM  DE-LETE.                                            NC1044.2
124600     GO TO    MOVE-WRITE-F1-39.                                   NC1044.2
124700 MOVE-FAIL-F1-39.                                                 NC1044.2
124800     MOVE     MOVE11 TO COMPUTED-A.                               NC1044.2
124900     MOVE "$12,345.00" TO CORRECT-A.                              NC1044.2
125000     PERFORM  FAIL.                                               NC1044.2
125100 MOVE-WRITE-F1-39.                                                NC1044.2
125200     MOVE "MOVE-TEST-F1-39" TO PAR-NAME.                          NC1044.2
125300     PERFORM  PRINT-DETAIL.                                       NC1044.2
125400 MOVE-INIT-F1-40.                                                 NC1044.2
125500     MOVE    "ABCDE" TO MOVE32.                                   NC1044.2
125600 MOVE-TEST-F1-40-0.                                               NC1044.2
125700     MOVE     MOVE32 TO MOVE21.                                   NC1044.2
125800 MOVE-TEST-F1-40-1.                                               NC1044.2
125900     IF       MOVE21 EQUAL TO "ABCDE  "                           NC1044.2
126000              PERFORM PASS                                        NC1044.2
126100              ELSE                                                NC1044.2
126200              GO TO MOVE-FAIL-F1-40.                              NC1044.2
126300*    NOTE AN TO AN MOVE, SPACE PADDING ON RIGHT.                  NC1044.2
126400     GO TO    MOVE-WRITE-F1-40.                                   NC1044.2
126500 MOVE-DELETE-F1-40.                                               NC1044.2
126600     PERFORM  DE-LETE.                                            NC1044.2
126700     GO TO    MOVE-WRITE-F1-40.                                   NC1044.2
126800 MOVE-FAIL-F1-40.                                                 NC1044.2
126900     MOVE     MOVE21 TO COMPUTED-A.                               NC1044.2
127000     MOVE "ABCDE  " TO CORRECT-A.                                 NC1044.2
127100     PERFORM  FAIL.                                               NC1044.2
127200 MOVE-WRITE-F1-40.                                                NC1044.2
127300     MOVE "MOVE-TEST-F1-40" TO PAR-NAME.                          NC1044.2
127400     PERFORM  PRINT-DETAIL.                                       NC1044.2
127500 MOVE-INIT-F1-41.                                                 NC1044.2
127600     MOVE    "ABCDE" TO MOVE32.                                   NC1044.2
127700 MOVE-TEST-F1-41-0.                                               NC1044.2
127800     MOVE     MOVE32 TO MOVE20.                                   NC1044.2
127900 MOVE-TEST-F1-41-1.                                               NC1044.2
128000     IF       MOVE20 EQUAL TO "ABCD"                              NC1044.2
128100              PERFORM PASS                                        NC1044.2
128200              ELSE                                                NC1044.2
128300              GO TO MOVE-FAIL-F1-41.                              NC1044.2
128400*    NOTE AN TO AN MOVE, TRUNCATION ON RIGHT.                     NC1044.2
128500     GO TO    MOVE-WRITE-F1-41.                                   NC1044.2
128600 MOVE-DELETE-F1-41.                                               NC1044.2
128700     PERFORM  DE-LETE.                                            NC1044.2
128800     GO TO    MOVE-WRITE-F1-41.                                   NC1044.2
128900 MOVE-FAIL-F1-41.                                                 NC1044.2
129000     MOVE     MOVE20 TO COMPUTED-A.                               NC1044.2
129100     MOVE "ABCD" TO CORRECT-A.                                    NC1044.2
129200     PERFORM  FAIL.                                               NC1044.2
129300 MOVE-WRITE-F1-41.                                                NC1044.2
129400     MOVE "MOVE-TEST-F1-41" TO PAR-NAME.                          NC1044.2
129500     PERFORM  PRINT-DETAIL.                                       NC1044.2
129600 MOVE-INIT-F1-42.                                                 NC1044.2
129700     MOVE    "ABCDE" TO MOVE32.                                   NC1044.2
129800 MOVE-TEST-F1-42-0.                                               NC1044.2
129900     MOVE     MOVE32 TO MOVE22.                                   NC1044.2
130000 MOVE-TEST-F1-42-1.                                               NC1044.2
130100     IF       MOVE22 EQUAL TO "A B0C D0E"                         NC1044.2
130200              PERFORM PASS                                        NC1044.2
130300              ELSE                                                NC1044.2
130400              GO TO MOVE-FAIL-F1-42.                              NC1044.2
130500*    NOTE AN TO AE MOVE, ZERO AND SPACE INSERTION.                NC1044.2
130600     GO TO    MOVE-WRITE-F1-42.                                   NC1044.2
130700 MOVE-DELETE-F1-42.                                               NC1044.2
130800     PERFORM  DE-LETE.                                            NC1044.2
130900     GO TO    MOVE-WRITE-F1-42.                                   NC1044.2
131000 MOVE-FAIL-F1-42.                                                 NC1044.2
131100     MOVE     MOVE22 TO COMPUTED-A.                               NC1044.2
131200     MOVE "A B0C D0E" TO CORRECT-A.                               NC1044.2
131300     PERFORM  FAIL.                                               NC1044.2
131400 MOVE-WRITE-F1-42.                                                NC1044.2
131500     MOVE "MOVE-TEST-F1-42" TO PAR-NAME.                          NC1044.2
131600     PERFORM  PRINT-DETAIL.                                       NC1044.2
131700 MOVE-INIT-F1-43.                                                 NC1044.2
131800     MOVE    "ABCDE" TO MOVE32.                                   NC1044.2
131900 MOVE-TEST-F1-43-0.                                               NC1044.2
132000     MOVE     MOVE32 TO MOVE33.                                   NC1044.2
132100 MOVE-TEST-F1-43-1.                                               NC1044.2
132200     IF       MOVE33 EQUAL TO "ABCDE"                             NC1044.2
132300              PERFORM PASS                                        NC1044.2
132400              ELSE                                                NC1044.2
132500              GO TO MOVE-FAIL-F1-43.                              NC1044.2
132600*    NOTE AN TO A MOVE, EQUAL SIZE.                               NC1044.2
132700     GO TO    MOVE-WRITE-F1-43.                                   NC1044.2
132800 MOVE-DELETE-F1-43.                                               NC1044.2
132900     PERFORM  DE-LETE.                                            NC1044.2
133000     GO TO    MOVE-WRITE-F1-43.                                   NC1044.2
133100 MOVE-FAIL-F1-43.                                                 NC1044.2
133200     MOVE     MOVE33 TO COMPUTED-A.                               NC1044.2
133300     MOVE "ABCDE" TO CORRECT-A.                                   NC1044.2
133400     PERFORM  FAIL.                                               NC1044.2
133500 MOVE-WRITE-F1-43.                                                NC1044.2
133600     MOVE "MOVE-TEST-F1-43" TO PAR-NAME.                          NC1044.2
133700     PERFORM  PRINT-DETAIL.                                       NC1044.2
133800 MOVE-INIT-F1-44.                                                 NC1044.2
133900     MOVE    "ABCDE" TO MOVE32.                                   NC1044.2
134000 MOVE-TEST-F1-44-0.                                               NC1044.2
134100     MOVE     MOVE32 TO MOVE34.                                   NC1044.2
134200 MOVE-TEST-F1-44-1.                                               NC1044.2
134300     IF       MOVE34 EQUAL TO "ABCDE  "                           NC1044.2
134400              PERFORM PASS                                        NC1044.2
134500              ELSE                                                NC1044.2
134600              GO TO MOVE-FAIL-F1-44.                              NC1044.2
134700*    NOTE AN TO A MOVE, SPACE PADDING ON RIGHT.                   NC1044.2
134800     GO TO    MOVE-WRITE-F1-44.                                   NC1044.2
134900 MOVE-DELETE-F1-44.                                               NC1044.2
135000     PERFORM  DE-LETE.                                            NC1044.2
135100     GO TO    MOVE-WRITE-F1-44.                                   NC1044.2
135200 MOVE-FAIL-F1-44.                                                 NC1044.2
135300     MOVE     MOVE34 TO COMPUTED-A.                               NC1044.2
135400     MOVE "ABCDE  " TO CORRECT-A.                                 NC1044.2
135500     PERFORM  FAIL.                                               NC1044.2
135600 MOVE-WRITE-F1-44.                                                NC1044.2
135700     MOVE "MOVE-TEST-F1-44" TO PAR-NAME.                          NC1044.2
135800     PERFORM  PRINT-DETAIL.                                       NC1044.2
135900 MOVE-INIT-F1-45.                                                 NC1044.2
136000     MOVE    "ABCDE" TO MOVE32.                                   NC1044.2
136100 MOVE-TEST-F1-45-0.                                               NC1044.2
136200     MOVE     MOVE32 TO MOVE35.                                   NC1044.2
136300 MOVE-TEST-F1-45-1.                                               NC1044.2
136400     IF       MOVE35 EQUAL TO "ABC"                               NC1044.2
136500              PERFORM PASS                                        NC1044.2
136600              ELSE                                                NC1044.2
136700              GO TO MOVE-FAIL-F1-45.                              NC1044.2
136800*    NOTE AN TO A MOVE, TRUNCATION ON RIGHT.                      NC1044.2
136900     GO TO    MOVE-WRITE-F1-45.                                   NC1044.2
137000 MOVE-DELETE-F1-45.                                               NC1044.2
137100     PERFORM  DE-LETE.                                            NC1044.2
137200     GO TO    MOVE-WRITE-F1-45.                                   NC1044.2
137300 MOVE-FAIL-F1-45.                                                 NC1044.2
137400     MOVE     MOVE35 TO COMPUTED-A.                               NC1044.2
137500     MOVE "ABC" TO CORRECT-A.                                     NC1044.2
137600     PERFORM  FAIL.                                               NC1044.2
137700 MOVE-WRITE-F1-45.                                                NC1044.2
137800     MOVE "MOVE-TEST-F1-45" TO PAR-NAME.                          NC1044.2
137900     PERFORM  PRINT-DETAIL.                                       NC1044.2
138000 MOVE-INIT-F1-46.                                                 NC1044.2
138100     MOVE    "1 A05" TO MOVE35A.                                  NC1044.2
138200 MOVE-TEST-F1-46-0.                                               NC1044.2
138300     MOVE     MOVE36 TO MOVE21.                                   NC1044.2
138400 MOVE-TEST-F1-46-1.                                               NC1044.2
138500     IF       MOVE21 EQUAL TO "1 A05  "                           NC1044.2
138600              PERFORM PASS                                        NC1044.2
138700              ELSE                                                NC1044.2
138800              GO TO MOVE-FAIL-F1-46.                              NC1044.2
138900*    NOTE AE TO AN MOVE, SPACE PADDING ON RIGHT.                  NC1044.2
139000     GO TO    MOVE-WRITE-F1-46.                                   NC1044.2
139100 MOVE-DELETE-F1-46.                                               NC1044.2
139200     PERFORM  DE-LETE.                                            NC1044.2
139300     GO TO    MOVE-WRITE-F1-46.                                   NC1044.2
139400 MOVE-FAIL-F1-46.                                                 NC1044.2
139500     MOVE     MOVE21 TO COMPUTED-A.                               NC1044.2
139600     MOVE "1 A05  " TO CORRECT-A.                                 NC1044.2
139700     PERFORM  FAIL.                                               NC1044.2
139800 MOVE-WRITE-F1-46.                                                NC1044.2
139900     MOVE "MOVE-TEST-F1-46" TO PAR-NAME.                          NC1044.2
140000     PERFORM  PRINT-DETAIL.                                       NC1044.2
140100 MOVE-INIT-F1-47.                                                 NC1044.2
140200     MOVE    "1 A05" TO MOVE35A.                                  NC1044.2
140300 MOVE-TEST-F1-47-0.                                               NC1044.2
140400     MOVE     MOVE36 TO MOVE20.                                   NC1044.2
140500 MOVE-TEST-F1-47-1.                                               NC1044.2
140600     IF       MOVE20 EQUAL TO "1 A0"                              NC1044.2
140700              PERFORM PASS                                        NC1044.2
140800              ELSE                                                NC1044.2
140900              GO TO MOVE-FAIL-F1-47.                              NC1044.2
141000*    NOTE AE TO AN MOVE, TRUNCATION ON RIGHT.                     NC1044.2
141100     GO TO    MOVE-WRITE-F1-47.                                   NC1044.2
141200 MOVE-DELETE-F1-47.                                               NC1044.2
141300     PERFORM  DE-LETE.                                            NC1044.2
141400     GO TO    MOVE-WRITE-F1-47.                                   NC1044.2
141500 MOVE-FAIL-F1-47.                                                 NC1044.2
141600     MOVE     MOVE20 TO COMPUTED-A.                               NC1044.2
141700     MOVE "1 A0" TO CORRECT-A.                                    NC1044.2
141800     PERFORM  FAIL.                                               NC1044.2
141900 MOVE-WRITE-F1-47.                                                NC1044.2
142000     MOVE "MOVE-TEST-F1-47" TO PAR-NAME.                          NC1044.2
142100     PERFORM  PRINT-DETAIL.                                       NC1044.2
142200 MOVE-INIT-F1-48.                                                 NC1044.2
142300     MOVE    "1 A05" TO MOVE35A.                                  NC1044.2
142400 MOVE-TEST-F1-48-0.                                               NC1044.2
142500     MOVE     MOVE36 TO MOVE39.                                   NC1044.2
142600 MOVE-TEST-F1-48-1.                                               NC1044.2
142700     IF       MOVE39 EQUAL TO "01 A050"                           NC1044.2
142800              PERFORM PASS                                        NC1044.2
142900              ELSE                                                NC1044.2
143000              GO TO MOVE-FAIL-F1-48.                              NC1044.2
143100*    NOTE AE TO AE MOVE, ZERO INSERTION.                          NC1044.2
143200     GO TO    MOVE-WRITE-F1-48.                                   NC1044.2
143300 MOVE-DELETE-F1-48.                                               NC1044.2
143400     PERFORM  DE-LETE.                                            NC1044.2
143500     GO TO    MOVE-WRITE-F1-48.                                   NC1044.2
143600 MOVE-FAIL-F1-48.                                                 NC1044.2
143700     MOVE     MOVE39 TO COMPUTED-A.                               NC1044.2
143800     MOVE "01 A050" TO CORRECT-A.                                 NC1044.2
143900     PERFORM  FAIL.                                               NC1044.2
144000 MOVE-WRITE-F1-48.                                                NC1044.2
144100     MOVE "MOVE-TEST-F1-48" TO PAR-NAME.                          NC1044.2
144200     PERFORM  PRINT-DETAIL.                                       NC1044.2
144300 MOVE-INIT-F1-49.                                                 NC1044.2
144400     MOVE    "1 A05" TO MOVE35A.                                  NC1044.2
144500 MOVE-TEST-F1-49-0.                                               NC1044.2
144600     MOVE     MOVE35A TO MOVE33.                                  NC1044.2
144700 MOVE-TEST-F1-49-1.                                               NC1044.2
144800     IF       MOVE33 EQUAL TO "1 A05"                             NC1044.2
144900              PERFORM PASS                                        NC1044.2
145000              ELSE                                                NC1044.2
145100              GO TO MOVE-FAIL-F1-49.                              NC1044.2
145200*    NOTE AE TO A MOVE, EQUAL SIZE.                               NC1044.2
145300     GO TO    MOVE-WRITE-F1-49.                                   NC1044.2
145400 MOVE-DELETE-F1-49.                                               NC1044.2
145500     PERFORM  DE-LETE.                                            NC1044.2
145600     GO TO    MOVE-WRITE-F1-49.                                   NC1044.2
145700 MOVE-FAIL-F1-49.                                                 NC1044.2
145800     MOVE     MOVE33 TO COMPUTED-A.                               NC1044.2
145900     MOVE "1 A05" TO CORRECT-A.                                   NC1044.2
146000     PERFORM  FAIL.                                               NC1044.2
146100 MOVE-WRITE-F1-49.                                                NC1044.2
146200     MOVE "MOVE-TEST-F1-49" TO PAR-NAME.                          NC1044.2
146300     PERFORM  PRINT-DETAIL.                                       NC1044.2
146400 MOVE-INIT-F1-50.                                                 NC1044.2
146500     MOVE    "1 A05" TO MOVE35A.                                  NC1044.2
146600 MOVE-TEST-F1-50-0.                                               NC1044.2
146700     MOVE     MOVE35A TO MOVE34.                                  NC1044.2
146800 MOVE-TEST-F1-50-1.                                               NC1044.2
146900     IF       MOVE34 EQUAL TO "1 A05  "                           NC1044.2
147000              PERFORM PASS                                        NC1044.2
147100              ELSE                                                NC1044.2
147200              GO TO MOVE-FAIL-F1-50.                              NC1044.2
147300*    NOTE AE TO A MOVE, SPACE PADDING ON RIGHT.                   NC1044.2
147400     GO TO    MOVE-WRITE-F1-50.                                   NC1044.2
147500 MOVE-DELETE-F1-50.                                               NC1044.2
147600     PERFORM  DE-LETE.                                            NC1044.2
147700     GO TO    MOVE-WRITE-F1-50.                                   NC1044.2
147800 MOVE-FAIL-F1-50.                                                 NC1044.2
147900     MOVE     MOVE34 TO COMPUTED-A.                               NC1044.2
148000     MOVE "1 A05  " TO CORRECT-A.                                 NC1044.2
148100     PERFORM  FAIL.                                               NC1044.2
148200 MOVE-WRITE-F1-50.                                                NC1044.2
148300     MOVE "MOVE-TEST-F1-50" TO PAR-NAME.                          NC1044.2
148400     PERFORM  PRINT-DETAIL.                                       NC1044.2
148500 MOVE-INIT-F1-51.                                                 NC1044.2
148600     MOVE    "1 A05" TO MOVE35A.                                  NC1044.2
148700 MOVE-TEST-F1-51-0.                                               NC1044.2
148800     MOVE     MOVE35A TO MOVE35.                                  NC1044.2
148900 MOVE-TEST-F1-51-1.                                               NC1044.2
149000     IF       MOVE35 EQUAL TO "1 A"                               NC1044.2
149100              PERFORM PASS                                        NC1044.2
149200              ELSE                                                NC1044.2
149300              GO TO MOVE-FAIL-F1-51.                              NC1044.2
149400*    NOTE AE TO A MOVE, TRUNCATION ON RIGHT.                      NC1044.2
149500     GO TO    MOVE-WRITE-F1-51.                                   NC1044.2
149600 MOVE-DELETE-F1-51.                                               NC1044.2
149700     PERFORM  DE-LETE.                                            NC1044.2
149800     GO TO    MOVE-WRITE-F1-51.                                   NC1044.2
149900 MOVE-FAIL-F1-51.                                                 NC1044.2
150000     MOVE     MOVE35 TO COMPUTED-A.                               NC1044.2
150100     MOVE "1 A" TO CORRECT-A.                                     NC1044.2
150200     PERFORM  FAIL.                                               NC1044.2
150300 MOVE-WRITE-F1-51.                                                NC1044.2
150400     MOVE "MOVE-TEST-F1-51" TO PAR-NAME.                          NC1044.2
150500     PERFORM  PRINT-DETAIL.                                       NC1044.2
150600 MOVE-INIT-F1-52.                                                 NC1044.2
150700     MOVE    "ABCDE" TO MOVE37.                                   NC1044.2
150800     MOVE    "MOVE ALPHABETIC     " TO FEATURE.                   NC1044.2
150900 MOVE-TEST-F1-52-0.                                               NC1044.2
151000     MOVE     MOVE37 TO MOVE21.                                   NC1044.2
151100 MOVE-TEST-F1-52-1.                                               NC1044.2
151200     IF       MOVE21 EQUAL TO "ABCDE  "                           NC1044.2
151300              PERFORM PASS                                        NC1044.2
151400              ELSE                                                NC1044.2
151500              GO TO MOVE-FAIL-F1-52.                              NC1044.2
151600*    NOTE A TO AN MOVE, SPACE PADDING ON RIGHT.                   NC1044.2
151700     GO TO    MOVE-WRITE-F1-52.                                   NC1044.2
151800 MOVE-DELETE-F1-52.                                               NC1044.2
151900     PERFORM  DE-LETE.                                            NC1044.2
152000     GO TO    MOVE-WRITE-F1-52.                                   NC1044.2
152100 MOVE-FAIL-F1-52.                                                 NC1044.2
152200     MOVE     MOVE21 TO COMPUTED-A.                               NC1044.2
152300     MOVE "ABCDE  " TO CORRECT-A.                                 NC1044.2
152400     PERFORM  FAIL.                                               NC1044.2
152500 MOVE-WRITE-F1-52.                                                NC1044.2
152600     MOVE "MOVE-TEST-F1-52" TO PAR-NAME.                          NC1044.2
152700     PERFORM  PRINT-DETAIL.                                       NC1044.2
152800 MOVE-INIT-F1-53.                                                 NC1044.2
152900     MOVE    "ABCDE" TO MOVE37.                                   NC1044.2
153000 MOVE-TEST-F1-53-0.                                               NC1044.2
153100     MOVE     MOVE37 TO MOVE20.                                   NC1044.2
153200 MOVE-TEST-F1-53-1.                                               NC1044.2
153300     IF       MOVE20 EQUAL TO "ABCD"                              NC1044.2
153400              PERFORM PASS                                        NC1044.2
153500              ELSE                                                NC1044.2
153600              GO TO MOVE-FAIL-F1-53.                              NC1044.2
153700*    NOTE A TO AN MOVE, TRUNCATION ON RIGHT.                      NC1044.2
153800     GO TO    MOVE-WRITE-F1-53.                                   NC1044.2
153900 MOVE-DELETE-F1-53.                                               NC1044.2
154000     PERFORM  DE-LETE.                                            NC1044.2
154100     GO TO    MOVE-WRITE-F1-53.                                   NC1044.2
154200 MOVE-FAIL-F1-53.                                                 NC1044.2
154300     MOVE     MOVE20 TO COMPUTED-A.                               NC1044.2
154400     MOVE "ABCD" TO CORRECT-A.                                    NC1044.2
154500     PERFORM  FAIL.                                               NC1044.2
154600 MOVE-WRITE-F1-53.                                                NC1044.2
154700     MOVE "MOVE-TEST-F1-53" TO PAR-NAME.                          NC1044.2
154800     PERFORM  PRINT-DETAIL.                                       NC1044.2
154900 MOVE-INIT-F1-54.                                                 NC1044.2
155000     MOVE    "ABCDE" TO MOVE37.                                   NC1044.2
155100 MOVE-TEST-F1-54-0.                                               NC1044.2
155200     MOVE     MOVE37 TO MOVE39.                                   NC1044.2
155300 MOVE-TEST-F1-54-1.                                               NC1044.2
155400     IF       MOVE39 EQUAL TO "0ABCDE0"                           NC1044.2
155500              PERFORM PASS                                        NC1044.2
155600              ELSE                                                NC1044.2
155700              GO TO MOVE-FAIL-F1-54.                              NC1044.2
155800*    NOTE A TO AE MOVE, ZERO INSERTION.                           NC1044.2
155900     GO TO    MOVE-WRITE-F1-54.                                   NC1044.2
156000 MOVE-DELETE-F1-54.                                               NC1044.2
156100     PERFORM  DE-LETE.                                            NC1044.2
156200     GO TO    MOVE-WRITE-F1-54.                                   NC1044.2
156300 MOVE-FAIL-F1-54.                                                 NC1044.2
156400     MOVE     MOVE39 TO COMPUTED-A.                               NC1044.2
156500     MOVE "0ABCDE0" TO CORRECT-A.                                 NC1044.2
156600     PERFORM  FAIL.                                               NC1044.2
156700 MOVE-WRITE-F1-54.                                                NC1044.2
156800     MOVE "MOVE-TEST-F1-54" TO PAR-NAME.                          NC1044.2
156900     PERFORM  PRINT-DETAIL.                                       NC1044.2
157000 MOVE-INIT-F1-55.                                                 NC1044.2
157100     MOVE    "ABCDE" TO MOVE37.                                   NC1044.2
157200 MOVE-TEST-F1-55-0.                                               NC1044.2
157300     MOVE     MOVE37 TO MOVE34.                                   NC1044.2
157400 MOVE-TEST-F1-55-1.                                               NC1044.2
157500     IF       MOVE34 EQUAL TO "ABCDE  "                           NC1044.2
157600              PERFORM PASS                                        NC1044.2
157700              ELSE                                                NC1044.2
157800              GO TO MOVE-FAIL-F1-55.                              NC1044.2
157900*    NOTE A TO A MOVE, SPACE PADDING ON RIGHT.                    NC1044.2
158000     GO TO    MOVE-WRITE-F1-55.                                   NC1044.2
158100 MOVE-DELETE-F1-55.                                               NC1044.2
158200     PERFORM  DE-LETE.                                            NC1044.2
158300     GO TO    MOVE-WRITE-F1-55.                                   NC1044.2
158400 MOVE-FAIL-F1-55.                                                 NC1044.2
158500     MOVE     MOVE4 TO COMPUTED-A.                                NC1044.2
158600     MOVE "ABCDE  " TO CORRECT-A.                                 NC1044.2
158700     PERFORM  FAIL.                                               NC1044.2
158800 MOVE-WRITE-F1-55.                                                NC1044.2
158900     MOVE "MOVE-TEST-F1-55" TO PAR-NAME.                          NC1044.2
159000     PERFORM  PRINT-DETAIL.                                       NC1044.2
159100 MOVE-INIT-F1-56.                                                 NC1044.2
159200     MOVE    "ABCDE" TO MOVE37.                                   NC1044.2
159300 MOVE-TEST-F1-56-0.                                               NC1044.2
159400     MOVE     MOVE37 TO MOVE35.                                   NC1044.2
159500 MOVE-TEST-F1-56-1.                                               NC1044.2
159600     IF       MOVE35 EQUAL TO "ABC"                               NC1044.2
159700              PERFORM PASS                                        NC1044.2
159800              ELSE                                                NC1044.2
159900              GO TO MOVE-FAIL-F1-56.                              NC1044.2
160000*    NOTE A TO A MOVE, TRUNCATION ON RIGHT.                       NC1044.2
160100     GO TO    MOVE-WRITE-F1-56.                                   NC1044.2
160200 MOVE-DELETE-F1-56.                                               NC1044.2
160300     PERFORM  DE-LETE.                                            NC1044.2
160400     GO TO    MOVE-WRITE-F1-56.                                   NC1044.2
160500 MOVE-FAIL-F1-56.                                                 NC1044.2
160600     MOVE     MOVE35 TO COMPUTED-A.                               NC1044.2
160700     MOVE "ABC" TO CORRECT-A.                                     NC1044.2
160800     PERFORM  FAIL.                                               NC1044.2
160900 MOVE-WRITE-F1-56.                                                NC1044.2
161000     MOVE "MOVE-TEST-F1-56" TO PAR-NAME.                          NC1044.2
161100     PERFORM PRINT-DETAIL.                                        NC1044.2
161200 NUMERIC-OPERAND-LIMITS-TESTS SECTION.                            NC1044.2
161300 MOVE-INIT-F1-57-1.                                               NC1044.2
161400     MOVE "MOVE LIMITS TESTS " TO FEATURE.                        NC1044.2
161500     MOVE  1 TO DNAME1.                                           NC1044.2
161600*    NOTE  THE FOLLOWING 44 TESTS WILL TEST THE LIMITS OF         NC1044.2
161700*        THE MOVE STATEMENT WITH OVER 20 OPERANDS, A DELETION     NC1044.2
161800*        PLACED IN THIS PARAGRAPH WILL SKIP THE LIMITS TESTS      NC1044.2
161900*        BUT A NOTE  STATEMENT MAY NEED TO BE PLACED IN EACH TEST.NC1044.2
162000     GO TO MOVE-TEST-F1-57-0.                                     NC1044.2
162100 MOVE-INIT-DELETE-F1-57-1.                                        NC1044.2
162200     PERFORM DE-LETE.                                             NC1044.2
162300     MOVE "MOVE LIMITS TESTS " TO FEATURE.                        NC1044.2
162400     MOVE "MOVE-TEST, F1-57-1 THRU F1-58-21" TO PAR-NAME.         NC1044.2
162500     PERFORM PRINT-DETAIL.                                        NC1044.2
162600     ADD 43 TO DELETE-COUNTER.                                    NC1044.2
162700     GO TO MOVE-INIT-F1-58.                                       NC1044.2
162800 MOVE-TEST-F1-57-0.                                               NC1044.2
162900     MOVE DNAME1 TO DNAME22  DNAME23  DNAME24  DNAME25  DNAME26   NC1044.2
163000         DNAME27  DNAME28  DNAME29  DNAME30  DNAME31  DNAME32     NC1044.2
163100         DNAME33  DNAME34  DNAME35  DNAME36  DNAME37  DNAME38     NC1044.2
163200         DNAME39  DNAME40  DNAME41  DNAME42  DNAME19.             NC1044.2
163300 MOVE-TEST-F1-57-1.                                               NC1044.2
163400     IF DNAME19 EQUAL TO 1                                        NC1044.2
163500         PERFORM PASS                                             NC1044.2
163600         GO TO MOVE-WRITE-F1-57-1.                                NC1044.2
163700     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
163800     MOVE DNAME19 TO COMPUTED-18V0.                               NC1044.2
163900     PERFORM FAIL.                                                NC1044.2
164000     GO TO MOVE-WRITE-F1-57-1.                                    NC1044.2
164100 MOVE-DELETE-F1-57-1.                                             NC1044.2
164200     PERFORM DE-LETE.                                             NC1044.2
164300*    NOTE  *** A DELETE IN THIS TEST WILL CAUSE THE NEXT          NC1044.2
164400*        43 TESTS TO FAIL.                                        NC1044.2
164500 MOVE-WRITE-F1-57-1.                                              NC1044.2
164600     MOVE "MOVE-TEST-F1-57-1 " TO PAR-NAME.                       NC1044.2
164700     PERFORM PRINT-DETAIL.                                        NC1044.2
164800 MOVE-TEST-F1-57-2.                                               NC1044.2
164900     IF DNAME22 EQUAL TO 1                                        NC1044.2
165000         PERFORM PASS                                             NC1044.2
165100         GO TO MOVE-WRITE-F1-57-2.                                NC1044.2
165200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
165300     MOVE DNAME22 TO COMPUTED-18V0.                               NC1044.2
165400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
165500     PERFORM FAIL.                                                NC1044.2
165600     GO TO MOVE-WRITE-F1-57-2.                                    NC1044.2
165700 MOVE-DELETE-F1-57-2.                                             NC1044.2
165800     PERFORM DE-LETE.                                             NC1044.2
165900 MOVE-WRITE-F1-57-2.                                              NC1044.2
166000     MOVE "MOVE-TEST-F1-57-2 " TO PAR-NAME.                       NC1044.2
166100     PERFORM PRINT-DETAIL.                                        NC1044.2
166200 MOVE-TEST-F1-57-3.                                               NC1044.2
166300     IF DNAME23 EQUAL TO 1                                        NC1044.2
166400         PERFORM PASS                                             NC1044.2
166500         GO TO MOVE-WRITE-F1-57-3.                                NC1044.2
166600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
166700     MOVE DNAME23 TO COMPUTED-18V0.                               NC1044.2
166800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
166900     PERFORM FAIL.                                                NC1044.2
167000     GO TO MOVE-WRITE-F1-57-3.                                    NC1044.2
167100 MOVE-DELETE-F1-57-3.                                             NC1044.2
167200     PERFORM DE-LETE.                                             NC1044.2
167300 MOVE-WRITE-F1-57-3.                                              NC1044.2
167400     MOVE "MOVE-TEST-F1-57-3 " TO PAR-NAME.                       NC1044.2
167500     PERFORM PRINT-DETAIL.                                        NC1044.2
167600 MOVE-TEST-F1-57-4.                                               NC1044.2
167700     IF DNAME24 EQUAL TO 1                                        NC1044.2
167800         PERFORM PASS                                             NC1044.2
167900         GO TO MOVE-WRITE-F1-57-4.                                NC1044.2
168000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
168100     MOVE DNAME24 TO COMPUTED-18V0.                               NC1044.2
168200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
168300     PERFORM FAIL.                                                NC1044.2
168400     GO TO MOVE-WRITE-F1-57-4.                                    NC1044.2
168500 MOVE-DELETE-F1-57-4.                                             NC1044.2
168600     PERFORM DE-LETE.                                             NC1044.2
168700 MOVE-WRITE-F1-57-4.                                              NC1044.2
168800     MOVE "MOVE-TEST-F1-57-4 " TO PAR-NAME.                       NC1044.2
168900     PERFORM PRINT-DETAIL.                                        NC1044.2
169000 MOVE-TEST-F1-57-5.                                               NC1044.2
169100     IF DNAME25 EQUAL TO 1                                        NC1044.2
169200         PERFORM PASS                                             NC1044.2
169300         GO TO MOVE-WRITE-F1-57-5.                                NC1044.2
169400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
169500     MOVE DNAME25 TO COMPUTED-18V0.                               NC1044.2
169600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
169700     PERFORM FAIL.                                                NC1044.2
169800     GO TO MOVE-WRITE-F1-57-5.                                    NC1044.2
169900 MOVE-DELETE-F1-57-5.                                             NC1044.2
170000     PERFORM DE-LETE.                                             NC1044.2
170100 MOVE-WRITE-F1-57-5.                                              NC1044.2
170200     MOVE "MOVE-TEST-F1-57-5 " TO PAR-NAME.                       NC1044.2
170300     PERFORM PRINT-DETAIL.                                        NC1044.2
170400 MOVE-TEST-F1-57-6.                                               NC1044.2
170500     IF DNAME26 EQUAL TO 1                                        NC1044.2
170600         PERFORM PASS                                             NC1044.2
170700         GO TO MOVE-WRITE-F1-57-6.                                NC1044.2
170800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
170900     MOVE DNAME26 TO COMPUTED-18V0.                               NC1044.2
171000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
171100     PERFORM FAIL.                                                NC1044.2
171200     GO TO MOVE-WRITE-F1-57-6.                                    NC1044.2
171300 MOVE-DELETE-F1-57-6.                                             NC1044.2
171400     PERFORM DE-LETE.                                             NC1044.2
171500 MOVE-WRITE-F1-57-6.                                              NC1044.2
171600     MOVE "MOVE-TEST-F1-57-6 " TO PAR-NAME.                       NC1044.2
171700     PERFORM PRINT-DETAIL.                                        NC1044.2
171800 MOVE-TEST-F1-57-7.                                               NC1044.2
171900     IF DNAME27 EQUAL TO 1                                        NC1044.2
172000         PERFORM PASS                                             NC1044.2
172100         GO TO MOVE-WRITE-F1-57-7.                                NC1044.2
172200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
172300     MOVE DNAME27 TO COMPUTED-18V0.                               NC1044.2
172400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
172500     PERFORM FAIL.                                                NC1044.2
172600     GO TO MOVE-WRITE-F1-57-7.                                    NC1044.2
172700 MOVE-DELETE-F1-57-7.                                             NC1044.2
172800     PERFORM DE-LETE.                                             NC1044.2
172900 MOVE-WRITE-F1-57-7.                                              NC1044.2
173000     MOVE "MOVE-TEST-F1-57-7 " TO PAR-NAME.                       NC1044.2
173100     PERFORM PRINT-DETAIL.                                        NC1044.2
173200 MOVE-TEST-F1-57-8.                                               NC1044.2
173300     IF DNAME28 EQUAL TO 1                                        NC1044.2
173400         PERFORM PASS                                             NC1044.2
173500         GO TO MOVE-WRITE-F1-57-8.                                NC1044.2
173600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
173700     MOVE DNAME28 TO COMPUTED-18V0.                               NC1044.2
173800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
173900     PERFORM FAIL.                                                NC1044.2
174000     GO TO MOVE-WRITE-F1-57-8.                                    NC1044.2
174100 MOVE-DELETE-F1-57-8.                                             NC1044.2
174200     PERFORM DE-LETE.                                             NC1044.2
174300 MOVE-WRITE-F1-57-8.                                              NC1044.2
174400     MOVE "MOVE-TEST-F1-57-8 " TO PAR-NAME.                       NC1044.2
174500     PERFORM PRINT-DETAIL.                                        NC1044.2
174600 MOVE-TEST-F1-57-9.                                               NC1044.2
174700     IF DNAME29 EQUAL TO 1                                        NC1044.2
174800         PERFORM PASS                                             NC1044.2
174900         GO TO MOVE-WRITE-F1-57-9.                                NC1044.2
175000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
175100     MOVE DNAME29 TO COMPUTED-18V0.                               NC1044.2
175200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
175300     PERFORM FAIL.                                                NC1044.2
175400     GO TO MOVE-WRITE-F1-57-9.                                    NC1044.2
175500 MOVE-DELETE-F1-57-9.                                             NC1044.2
175600     PERFORM DE-LETE.                                             NC1044.2
175700 MOVE-WRITE-F1-57-9.                                              NC1044.2
175800     MOVE "MOVE-TEST-F1-57-9 " TO PAR-NAME.                       NC1044.2
175900     PERFORM PRINT-DETAIL.                                        NC1044.2
176000 MOVE-TEST-F1-57-10.                                              NC1044.2
176100     IF DNAME30 EQUAL TO 1                                        NC1044.2
176200         PERFORM PASS                                             NC1044.2
176300         GO TO MOVE-WRITE-F1-57-10.                               NC1044.2
176400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
176500     MOVE DNAME30 TO COMPUTED-18V0.                               NC1044.2
176600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
176700     PERFORM FAIL.                                                NC1044.2
176800     GO TO MOVE-WRITE-F1-57-10.                                   NC1044.2
176900 MOVE-DELETE-F1-57-10.                                            NC1044.2
177000     PERFORM DE-LETE.                                             NC1044.2
177100 MOVE-WRITE-F1-57-10.                                             NC1044.2
177200     MOVE "MOVE-TEST-F1-57-10 " TO PAR-NAME.                      NC1044.2
177300     PERFORM PRINT-DETAIL.                                        NC1044.2
177400 MOVE-TEST-F1-57-11.                                              NC1044.2
177500     IF DNAME31 EQUAL TO 1                                        NC1044.2
177600         PERFORM PASS                                             NC1044.2
177700         GO TO MOVE-WRITE-F1-57-11.                               NC1044.2
177800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
177900     MOVE DNAME31 TO COMPUTED-18V0.                               NC1044.2
178000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
178100     PERFORM FAIL.                                                NC1044.2
178200     GO TO MOVE-WRITE-F1-57-11.                                   NC1044.2
178300 MOVE-DELETE-F1-57-11.                                            NC1044.2
178400     PERFORM DE-LETE.                                             NC1044.2
178500 MOVE-WRITE-F1-57-11.                                             NC1044.2
178600     MOVE "MOVE-TEST-F1-57-11 " TO PAR-NAME.                      NC1044.2
178700     PERFORM PRINT-DETAIL.                                        NC1044.2
178800 MOVE-TEST-F1-57-12.                                              NC1044.2
178900     IF DNAME32 EQUAL TO 1                                        NC1044.2
179000         PERFORM PASS                                             NC1044.2
179100         GO TO MOVE-WRITE-F1-57-12.                               NC1044.2
179200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
179300     MOVE DNAME32 TO COMPUTED-18V0.                               NC1044.2
179400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
179500     PERFORM FAIL.                                                NC1044.2
179600     GO TO MOVE-WRITE-F1-57-12.                                   NC1044.2
179700 MOVE-DELETE-F1-57-12.                                            NC1044.2
179800     PERFORM DE-LETE.                                             NC1044.2
179900 MOVE-WRITE-F1-57-12.                                             NC1044.2
180000     MOVE "MOVE-TEST-F1-57-12 " TO PAR-NAME.                      NC1044.2
180100     PERFORM PRINT-DETAIL.                                        NC1044.2
180200 MOVE-TEST-F1-57-13.                                              NC1044.2
180300     IF DNAME33 EQUAL TO 1                                        NC1044.2
180400         PERFORM PASS                                             NC1044.2
180500         GO TO MOVE-WRITE-F1-57-13.                               NC1044.2
180600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
180700     MOVE DNAME33 TO COMPUTED-18V0.                               NC1044.2
180800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
180900     PERFORM FAIL.                                                NC1044.2
181000     GO TO MOVE-WRITE-F1-57-13.                                   NC1044.2
181100 MOVE-DELETE-F1-57-13.                                            NC1044.2
181200     PERFORM DE-LETE.                                             NC1044.2
181300 MOVE-WRITE-F1-57-13.                                             NC1044.2
181400     MOVE "MOVE-TEST-F1-57-13 " TO PAR-NAME.                      NC1044.2
181500     PERFORM PRINT-DETAIL.                                        NC1044.2
181600 MOVE-TEST-F1-57-14.                                              NC1044.2
181700     IF DNAME34 EQUAL TO 1                                        NC1044.2
181800         PERFORM PASS                                             NC1044.2
181900         GO TO MOVE-WRITE-F1-57-14.                               NC1044.2
182000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
182100     MOVE DNAME34 TO COMPUTED-18V0.                               NC1044.2
182200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
182300     PERFORM FAIL.                                                NC1044.2
182400     GO TO MOVE-WRITE-F1-57-14.                                   NC1044.2
182500 MOVE-DELETE-F1-57-14.                                            NC1044.2
182600     PERFORM DE-LETE.                                             NC1044.2
182700 MOVE-WRITE-F1-57-14.                                             NC1044.2
182800     MOVE "MOVE-TEST-F1-57-14 " TO PAR-NAME.                      NC1044.2
182900     PERFORM PRINT-DETAIL.                                        NC1044.2
183000 MOVE-TEST-F1-57-15.                                              NC1044.2
183100     IF DNAME35 EQUAL TO 1                                        NC1044.2
183200         PERFORM PASS                                             NC1044.2
183300         GO TO MOVE-WRITE-F1-57-15.                               NC1044.2
183400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
183500     MOVE DNAME35 TO COMPUTED-18V0.                               NC1044.2
183600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
183700     PERFORM FAIL.                                                NC1044.2
183800     GO TO MOVE-WRITE-F1-57-15.                                   NC1044.2
183900 MOVE-DELETE-F1-57-15.                                            NC1044.2
184000     PERFORM DE-LETE.                                             NC1044.2
184100 MOVE-WRITE-F1-57-15.                                             NC1044.2
184200     MOVE "MOVE-TEST-F1-57-15 " TO PAR-NAME.                      NC1044.2
184300     PERFORM PRINT-DETAIL.                                        NC1044.2
184400 MOVE-TEST-F1-57-16.                                              NC1044.2
184500     IF DNAME36 EQUAL TO 1                                        NC1044.2
184600         PERFORM PASS                                             NC1044.2
184700         GO TO MOVE-WRITE-F1-57-16.                               NC1044.2
184800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
184900     MOVE DNAME36 TO COMPUTED-18V0.                               NC1044.2
185000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
185100     PERFORM FAIL.                                                NC1044.2
185200     GO TO MOVE-WRITE-F1-57-16.                                   NC1044.2
185300 MOVE-DELETE-F1-57-16.                                            NC1044.2
185400     PERFORM DE-LETE.                                             NC1044.2
185500 MOVE-WRITE-F1-57-16.                                             NC1044.2
185600     MOVE "MOVE-TEST-F1-57-16 " TO PAR-NAME.                      NC1044.2
185700     PERFORM PRINT-DETAIL.                                        NC1044.2
185800 MOVE-TEST-F1-57-17.                                              NC1044.2
185900     IF DNAME37 EQUAL TO 1                                        NC1044.2
186000         PERFORM PASS                                             NC1044.2
186100         GO TO MOVE-WRITE-F1-57-17.                               NC1044.2
186200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
186300     MOVE DNAME37 TO COMPUTED-18V0.                               NC1044.2
186400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
186500     PERFORM FAIL.                                                NC1044.2
186600     GO TO MOVE-WRITE-F1-57-17.                                   NC1044.2
186700 MOVE-DELETE-F1-57-17.                                            NC1044.2
186800     PERFORM DE-LETE.                                             NC1044.2
186900 MOVE-WRITE-F1-57-17.                                             NC1044.2
187000     MOVE "MOVE-TEST-F1-57-17 " TO PAR-NAME.                      NC1044.2
187100     PERFORM PRINT-DETAIL.                                        NC1044.2
187200 MOVE-TEST-F1-57-18.                                              NC1044.2
187300     IF DNAME38 EQUAL TO 1                                        NC1044.2
187400         PERFORM PASS                                             NC1044.2
187500         GO TO MOVE-WRITE-F1-57-18.                               NC1044.2
187600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
187700     MOVE DNAME38 TO COMPUTED-18V0.                               NC1044.2
187800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
187900     PERFORM FAIL.                                                NC1044.2
188000     GO TO MOVE-WRITE-F1-57-18.                                   NC1044.2
188100 MOVE-DELETE-F1-57-18.                                            NC1044.2
188200     PERFORM DE-LETE.                                             NC1044.2
188300 MOVE-WRITE-F1-57-18.                                             NC1044.2
188400     MOVE "MOVE-TEST-F1-57-18 " TO PAR-NAME.                      NC1044.2
188500     PERFORM PRINT-DETAIL.                                        NC1044.2
188600 MOVE-TEST-F1-57-19.                                              NC1044.2
188700     IF DNAME39 EQUAL TO 1                                        NC1044.2
188800         PERFORM PASS                                             NC1044.2
188900         GO TO MOVE-WRITE-F1-57-19.                               NC1044.2
189000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
189100     MOVE DNAME39 TO COMPUTED-18V0.                               NC1044.2
189200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
189300     PERFORM FAIL.                                                NC1044.2
189400     GO TO MOVE-WRITE-F1-57-19.                                   NC1044.2
189500 MOVE-DELETE-F1-57-19.                                            NC1044.2
189600     PERFORM DE-LETE.                                             NC1044.2
189700 MOVE-WRITE-F1-57-19.                                             NC1044.2
189800     MOVE "MOVE-TEST-F1-57-19 " TO PAR-NAME.                      NC1044.2
189900     PERFORM PRINT-DETAIL.                                        NC1044.2
190000 MOVE-TEST-F1-57-20.                                              NC1044.2
190100     IF DNAME40 EQUAL TO 1                                        NC1044.2
190200         PERFORM PASS                                             NC1044.2
190300         GO TO MOVE-WRITE-F1-57-20.                               NC1044.2
190400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
190500     MOVE DNAME40 TO COMPUTED-18V0.                               NC1044.2
190600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
190700     PERFORM FAIL.                                                NC1044.2
190800     GO TO MOVE-WRITE-F1-57-20.                                   NC1044.2
190900 MOVE-DELETE-F1-57-20.                                            NC1044.2
191000     PERFORM DE-LETE.                                             NC1044.2
191100 MOVE-WRITE-F1-57-20.                                             NC1044.2
191200     MOVE "MOVE-TEST-F1-57-20 " TO PAR-NAME.                      NC1044.2
191300     PERFORM PRINT-DETAIL.                                        NC1044.2
191400 MOVE-TEST-F1-57-21.                                              NC1044.2
191500     IF DNAME41 EQUAL TO 1                                        NC1044.2
191600         PERFORM PASS                                             NC1044.2
191700         GO TO MOVE-WRITE-F1-57-21.                               NC1044.2
191800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
191900     MOVE DNAME41 TO COMPUTED-18V0.                               NC1044.2
192000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
192100     PERFORM FAIL.                                                NC1044.2
192200     GO TO MOVE-WRITE-F1-57-21.                                   NC1044.2
192300 MOVE-DELETE-F1-57-21.                                            NC1044.2
192400     PERFORM DE-LETE.                                             NC1044.2
192500 MOVE-WRITE-F1-57-21.                                             NC1044.2
192600     MOVE "MOVE-TEST-F1-57-21 " TO PAR-NAME.                      NC1044.2
192700     PERFORM PRINT-DETAIL.                                        NC1044.2
192800 MOVE-TEST-F1-57-22.                                              NC1044.2
192900     IF DNAME42 EQUAL TO 1                                        NC1044.2
193000         PERFORM PASS                                             NC1044.2
193100         GO TO MOVE-WRITE-F1-57-22.                               NC1044.2
193200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-57-1.    NC1044.2
193300     MOVE DNAME42 TO COMPUTED-18V0.                               NC1044.2
193400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
193500     PERFORM FAIL.                                                NC1044.2
193600     GO TO MOVE-WRITE-F1-57-22.                                   NC1044.2
193700 MOVE-DELETE-F1-57-22.                                            NC1044.2
193800     PERFORM DE-LETE.                                             NC1044.2
193900 MOVE-WRITE-F1-57-22.                                             NC1044.2
194000     MOVE "MOVE-TEST-F1-57-22 " TO PAR-NAME.                      NC1044.2
194100     PERFORM PRINT-DETAIL.                                        NC1044.2
194200 MOVE-INIT-F1-58.                                                 NC1044.2
194300     MOVE    000000000000000001 TO DNAME18.                       NC1044.2
194400 MOVE-TEST-F1-58-0.                                               NC1044.2
194500     MOVE DNAME18 TO DNAME22  DNAME23  DNAME24  DNAME25  DNAME26  NC1044.2
194600         DNAME27  DNAME28  DNAME29  DNAME30  DNAME31  DNAME32     NC1044.2
194700         DNAME33  DNAME34  DNAME35  DNAME36  DNAME37  DNAME38     NC1044.2
194800         DNAME39  DNAME40  DNAME41  DNAME42.                      NC1044.2
194900 MOVE-TEST-F1-58-1.                                               NC1044.2
195000     IF DNAME22 EQUAL TO 1                                        NC1044.2
195100         PERFORM PASS                                             NC1044.2
195200         GO TO MOVE-WRITE-F1-58-1.                                NC1044.2
195300     MOVE DNAME22 TO COMPUTED-18V0.                               NC1044.2
195400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
195500     PERFORM FAIL.                                                NC1044.2
195600     GO TO MOVE-WRITE-F1-58-1.                                    NC1044.2
195700 MOVE-DELETE-F1-58-1.                                             NC1044.2
195800     PERFORM DE-LETE.                                             NC1044.2
195900 MOVE-WRITE-F1-58-1.                                              NC1044.2
196000     MOVE "MOVE-TEST-F1-58-1 " TO PAR-NAME.                       NC1044.2
196100     PERFORM PRINT-DETAIL.                                        NC1044.2
196200 MOVE-TEST-F1-58-2.                                               NC1044.2
196300     IF DNAME23 EQUAL TO 1                                        NC1044.2
196400         PERFORM PASS                                             NC1044.2
196500         GO TO MOVE-WRITE-F1-58-2.                                NC1044.2
196600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
196700     MOVE DNAME23 TO COMPUTED-18V0.                               NC1044.2
196800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
196900     PERFORM FAIL.                                                NC1044.2
197000     GO TO MOVE-WRITE-F1-58-2.                                    NC1044.2
197100 MOVE-DELETE-F1-58-2.                                             NC1044.2
197200     PERFORM DE-LETE.                                             NC1044.2
197300 MOVE-WRITE-F1-58-2.                                              NC1044.2
197400     MOVE "MOVE-TEST-F1-58-2 " TO PAR-NAME.                       NC1044.2
197500     PERFORM PRINT-DETAIL.                                        NC1044.2
197600 MOVE-TEST-F1-58-3.                                               NC1044.2
197700     IF DNAME24 EQUAL TO 1                                        NC1044.2
197800         PERFORM PASS                                             NC1044.2
197900         GO TO MOVE-WRITE-F1-58-3.                                NC1044.2
198000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
198100     MOVE DNAME24 TO COMPUTED-18V0.                               NC1044.2
198200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
198300     PERFORM FAIL.                                                NC1044.2
198400     GO TO MOVE-WRITE-F1-58-3.                                    NC1044.2
198500 MOVE-DELETE-F1-58-3.                                             NC1044.2
198600     PERFORM DE-LETE.                                             NC1044.2
198700 MOVE-WRITE-F1-58-3.                                              NC1044.2
198800     MOVE "MOVE-TEST-F1-58-3 " TO PAR-NAME.                       NC1044.2
198900     PERFORM PRINT-DETAIL.                                        NC1044.2
199000 MOVE-TEST-F1-58-4.                                               NC1044.2
199100     IF DNAME25 EQUAL TO 1                                        NC1044.2
199200         PERFORM PASS                                             NC1044.2
199300         GO TO MOVE-WRITE-F1-58-4.                                NC1044.2
199400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
199500     MOVE DNAME25 TO COMPUTED-18V0.                               NC1044.2
199600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
199700     PERFORM FAIL.                                                NC1044.2
199800     GO TO MOVE-WRITE-F1-58-4.                                    NC1044.2
199900 MOVE-DELETE-F1-58-4.                                             NC1044.2
200000     PERFORM DE-LETE.                                             NC1044.2
200100 MOVE-WRITE-F1-58-4.                                              NC1044.2
200200     MOVE "MOVE-TEST-F1-58-4 " TO PAR-NAME.                       NC1044.2
200300     PERFORM PRINT-DETAIL.                                        NC1044.2
200400 MOVE-TEST-F1-58-5.                                               NC1044.2
200500     IF DNAME26 EQUAL TO 1                                        NC1044.2
200600         PERFORM PASS                                             NC1044.2
200700         GO TO MOVE-WRITE-F1-58-5.                                NC1044.2
200800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
200900     MOVE DNAME26 TO COMPUTED-18V0.                               NC1044.2
201000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
201100     PERFORM FAIL.                                                NC1044.2
201200     GO TO MOVE-WRITE-F1-58-5.                                    NC1044.2
201300 MOVE-DELETE-F1-58-5.                                             NC1044.2
201400     PERFORM DE-LETE.                                             NC1044.2
201500 MOVE-WRITE-F1-58-5.                                              NC1044.2
201600     MOVE "MOVE-TEST-F1-58-5 " TO PAR-NAME.                       NC1044.2
201700     PERFORM PRINT-DETAIL.                                        NC1044.2
201800 MOVE-TEST-F1-58-6.                                               NC1044.2
201900     IF DNAME27 EQUAL TO 1                                        NC1044.2
202000         PERFORM PASS                                             NC1044.2
202100         GO TO MOVE-WRITE-F1-58-6.                                NC1044.2
202200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
202300     MOVE DNAME27 TO COMPUTED-18V0.                               NC1044.2
202400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
202500     PERFORM FAIL.                                                NC1044.2
202600     GO TO MOVE-WRITE-F1-58-6.                                    NC1044.2
202700 MOVE-DELETE-F1-58-6.                                             NC1044.2
202800     PERFORM DE-LETE.                                             NC1044.2
202900 MOVE-WRITE-F1-58-6.                                              NC1044.2
203000     MOVE "MOVE-TEST-F1-58-6 " TO PAR-NAME.                       NC1044.2
203100     PERFORM PRINT-DETAIL.                                        NC1044.2
203200 MOVE-TEST-F1-58-7.                                               NC1044.2
203300     IF DNAME28 EQUAL TO 1                                        NC1044.2
203400         PERFORM PASS                                             NC1044.2
203500         GO TO MOVE-WRITE-F1-58-7.                                NC1044.2
203600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
203700     MOVE DNAME28 TO COMPUTED-18V0.                               NC1044.2
203800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
203900     PERFORM FAIL.                                                NC1044.2
204000     GO TO MOVE-WRITE-F1-58-7.                                    NC1044.2
204100 MOVE-DELETE-F1-58-7.                                             NC1044.2
204200     PERFORM DE-LETE.                                             NC1044.2
204300 MOVE-WRITE-F1-58-7.                                              NC1044.2
204400     MOVE "MOVE-TEST-F1-58-7 " TO PAR-NAME.                       NC1044.2
204500     PERFORM PRINT-DETAIL.                                        NC1044.2
204600 MOVE-TEST-F1-58-8.                                               NC1044.2
204700     IF DNAME29 EQUAL TO 1                                        NC1044.2
204800         PERFORM PASS                                             NC1044.2
204900         GO TO MOVE-WRITE-F1-58-8.                                NC1044.2
205000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
205100     MOVE DNAME29 TO COMPUTED-18V0.                               NC1044.2
205200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
205300     PERFORM FAIL.                                                NC1044.2
205400     GO TO MOVE-WRITE-F1-58-8.                                    NC1044.2
205500 MOVE-DELETE-F1-58-8.                                             NC1044.2
205600     PERFORM DE-LETE.                                             NC1044.2
205700 MOVE-WRITE-F1-58-8.                                              NC1044.2
205800     MOVE "MOVE-TEST-F1-58-8 " TO PAR-NAME.                       NC1044.2
205900     PERFORM PRINT-DETAIL.                                        NC1044.2
206000 MOVE-TEST-F1-58-9.                                               NC1044.2
206100     IF DNAME30 EQUAL TO 1                                        NC1044.2
206200         PERFORM PASS                                             NC1044.2
206300         GO TO MOVE-WRITE-F1-58-9.                                NC1044.2
206400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
206500     MOVE DNAME30 TO COMPUTED-18V0.                               NC1044.2
206600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
206700     PERFORM FAIL.                                                NC1044.2
206800     GO TO MOVE-WRITE-F1-58-9.                                    NC1044.2
206900 MOVE-DELETE-F1-58-9.                                             NC1044.2
207000     PERFORM DE-LETE.                                             NC1044.2
207100 MOVE-WRITE-F1-58-9.                                              NC1044.2
207200     MOVE "MOVE-TEST-F1-58-9 " TO PAR-NAME.                       NC1044.2
207300     PERFORM PRINT-DETAIL.                                        NC1044.2
207400 MOVE-TEST-F1-58-10.                                              NC1044.2
207500     IF DNAME31 EQUAL TO 1                                        NC1044.2
207600         PERFORM PASS                                             NC1044.2
207700         GO TO MOVE-WRITE-F1-58-10.                               NC1044.2
207800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
207900     MOVE DNAME31 TO COMPUTED-18V0.                               NC1044.2
208000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
208100     PERFORM FAIL.                                                NC1044.2
208200     GO TO MOVE-WRITE-F1-58-10.                                   NC1044.2
208300 MOVE-DELETE-F1-58-10.                                            NC1044.2
208400     PERFORM DE-LETE.                                             NC1044.2
208500 MOVE-WRITE-F1-58-10.                                             NC1044.2
208600     MOVE "MOVE-TEST-F1-58-10 " TO PAR-NAME.                      NC1044.2
208700     PERFORM PRINT-DETAIL.                                        NC1044.2
208800 MOVE-TEST-F1-58-11.                                              NC1044.2
208900     IF DNAME32 EQUAL TO 1                                        NC1044.2
209000         PERFORM PASS                                             NC1044.2
209100         GO TO MOVE-WRITE-F1-58-11.                               NC1044.2
209200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
209300     MOVE DNAME32 TO COMPUTED-18V0.                               NC1044.2
209400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
209500     PERFORM FAIL.                                                NC1044.2
209600     GO TO MOVE-WRITE-F1-58-11.                                   NC1044.2
209700 MOVE-DELETE-F1-58-11.                                            NC1044.2
209800     PERFORM DE-LETE.                                             NC1044.2
209900 MOVE-WRITE-F1-58-11.                                             NC1044.2
210000     MOVE "MOVE-TEST-F1-58-11 " TO PAR-NAME.                      NC1044.2
210100     PERFORM PRINT-DETAIL.                                        NC1044.2
210200 MOVE-TEST-F1-58-12.                                              NC1044.2
210300     IF DNAME33 EQUAL TO 1                                        NC1044.2
210400         PERFORM PASS                                             NC1044.2
210500         GO TO MOVE-WRITE-F1-58-12.                               NC1044.2
210600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
210700     MOVE DNAME33 TO COMPUTED-18V0.                               NC1044.2
210800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
210900     PERFORM FAIL.                                                NC1044.2
211000     GO TO MOVE-WRITE-F1-58-12.                                   NC1044.2
211100 MOVE-DELETE-F1-58-12.                                            NC1044.2
211200     PERFORM DE-LETE.                                             NC1044.2
211300 MOVE-WRITE-F1-58-12.                                             NC1044.2
211400     MOVE "MOVE-TEST-F1-58-12 " TO PAR-NAME.                      NC1044.2
211500     PERFORM PRINT-DETAIL.                                        NC1044.2
211600 MOVE-TEST-F1-58-13.                                              NC1044.2
211700     IF DNAME34 EQUAL TO 1                                        NC1044.2
211800         PERFORM PASS                                             NC1044.2
211900         GO TO MOVE-WRITE-F1-58-13.                               NC1044.2
212000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
212100     MOVE DNAME34 TO COMPUTED-18V0.                               NC1044.2
212200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
212300     PERFORM FAIL.                                                NC1044.2
212400     GO TO MOVE-WRITE-F1-58-13.                                   NC1044.2
212500 MOVE-DELETE-F1-58-13.                                            NC1044.2
212600     PERFORM DE-LETE.                                             NC1044.2
212700 MOVE-WRITE-F1-58-13.                                             NC1044.2
212800     MOVE "MOVE-TEST-F1-58-13 " TO PAR-NAME.                      NC1044.2
212900     PERFORM PRINT-DETAIL.                                        NC1044.2
213000 MOVE-TEST-F1-58-14.                                              NC1044.2
213100     IF DNAME35 EQUAL TO 1                                        NC1044.2
213200         PERFORM PASS                                             NC1044.2
213300         GO TO MOVE-WRITE-F1-58-14.                               NC1044.2
213400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
213500     MOVE DNAME35 TO COMPUTED-18V0.                               NC1044.2
213600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
213700     PERFORM FAIL.                                                NC1044.2
213800     GO TO MOVE-WRITE-F1-58-14.                                   NC1044.2
213900 MOVE-DELETE-F1-58-14.                                            NC1044.2
214000     PERFORM DE-LETE.                                             NC1044.2
214100 MOVE-WRITE-F1-58-14.                                             NC1044.2
214200     MOVE "MOVE-TEST-F1-58-14 " TO PAR-NAME.                      NC1044.2
214300     PERFORM PRINT-DETAIL.                                        NC1044.2
214400 MOVE-TEST-F1-58-15.                                              NC1044.2
214500     IF DNAME36 EQUAL TO 1                                        NC1044.2
214600         PERFORM PASS                                             NC1044.2
214700         GO TO MOVE-WRITE-F1-58-15.                               NC1044.2
214800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
214900     MOVE DNAME36 TO COMPUTED-18V0.                               NC1044.2
215000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
215100     PERFORM FAIL.                                                NC1044.2
215200     GO TO MOVE-WRITE-F1-58-15.                                   NC1044.2
215300 MOVE-DELETE-F1-58-15.                                            NC1044.2
215400     PERFORM DE-LETE.                                             NC1044.2
215500 MOVE-WRITE-F1-58-15.                                             NC1044.2
215600     MOVE "MOVE-TEST-F1-58-15 " TO PAR-NAME.                      NC1044.2
215700     PERFORM PRINT-DETAIL.                                        NC1044.2
215800 MOVE-TEST-F1-58-16.                                              NC1044.2
215900     IF DNAME37 EQUAL TO 1                                        NC1044.2
216000         PERFORM PASS                                             NC1044.2
216100         GO TO MOVE-WRITE-F1-58-16.                               NC1044.2
216200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
216300     MOVE DNAME37 TO COMPUTED-18V0.                               NC1044.2
216400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
216500     PERFORM FAIL.                                                NC1044.2
216600     GO TO MOVE-WRITE-F1-58-16.                                   NC1044.2
216700 MOVE-DELETE-F1-58-16.                                            NC1044.2
216800     PERFORM DE-LETE.                                             NC1044.2
216900 MOVE-WRITE-F1-58-16.                                             NC1044.2
217000     MOVE "MOVE-TEST-F1-58-16 " TO PAR-NAME.                      NC1044.2
217100     PERFORM PRINT-DETAIL.                                        NC1044.2
217200 MOVE-TEST-F1-58-17.                                              NC1044.2
217300     IF DNAME38 EQUAL TO 1                                        NC1044.2
217400         PERFORM PASS                                             NC1044.2
217500         GO TO MOVE-WRITE-F1-58-17.                               NC1044.2
217600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
217700     MOVE DNAME38 TO COMPUTED-18V0.                               NC1044.2
217800     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
217900     PERFORM FAIL.                                                NC1044.2
218000     GO TO MOVE-WRITE-F1-58-17.                                   NC1044.2
218100 MOVE-DELETE-F1-58-17.                                            NC1044.2
218200     PERFORM DE-LETE.                                             NC1044.2
218300 MOVE-WRITE-F1-58-17.                                             NC1044.2
218400     MOVE "MOVE-TEST-F1-58-17 " TO PAR-NAME.                      NC1044.2
218500     PERFORM PRINT-DETAIL.                                        NC1044.2
218600 MOVE-TEST-F1-58-18.                                              NC1044.2
218700     IF DNAME39 EQUAL TO 1                                        NC1044.2
218800         PERFORM PASS                                             NC1044.2
218900         GO TO MOVE-WRITE-F1-58-18.                               NC1044.2
219000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
219100     MOVE DNAME39 TO COMPUTED-18V0.                               NC1044.2
219200     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
219300     PERFORM FAIL.                                                NC1044.2
219400     GO TO MOVE-WRITE-F1-58-18.                                   NC1044.2
219500 MOVE-DELETE-F1-58-18.                                            NC1044.2
219600     PERFORM DE-LETE.                                             NC1044.2
219700 MOVE-WRITE-F1-58-18.                                             NC1044.2
219800     MOVE "MOVE-TEST-F1-58-18 " TO PAR-NAME.                      NC1044.2
219900     PERFORM PRINT-DETAIL.                                        NC1044.2
220000 MOVE-TEST-F1-58-19.                                              NC1044.2
220100     IF DNAME40 EQUAL TO 1                                        NC1044.2
220200         PERFORM PASS                                             NC1044.2
220300         GO TO MOVE-WRITE-F1-58-19.                               NC1044.2
220400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
220500     MOVE DNAME40 TO COMPUTED-18V0.                               NC1044.2
220600     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
220700     PERFORM FAIL.                                                NC1044.2
220800     GO TO MOVE-WRITE-F1-58-19.                                   NC1044.2
220900 MOVE-DELETE-F1-58-19.                                            NC1044.2
221000     PERFORM DE-LETE.                                             NC1044.2
221100 MOVE-WRITE-F1-58-19.                                             NC1044.2
221200     MOVE "MOVE-TEST-F1-58-19 " TO PAR-NAME.                      NC1044.2
221300     PERFORM PRINT-DETAIL.                                        NC1044.2
221400 MOVE-TEST-F1-58-20.                                              NC1044.2
221500     IF DNAME41 EQUAL TO 1                                        NC1044.2
221600         PERFORM PASS                                             NC1044.2
221700         GO TO MOVE-WRITE-F1-58-20.                               NC1044.2
221800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
221900     MOVE DNAME41 TO COMPUTED-18V0.                               NC1044.2
222000     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
222100     PERFORM FAIL.                                                NC1044.2
222200     GO TO MOVE-WRITE-F1-58-20.                                   NC1044.2
222300 MOVE-DELETE-F1-58-20.                                            NC1044.2
222400     PERFORM DE-LETE.                                             NC1044.2
222500 MOVE-WRITE-F1-58-20.                                             NC1044.2
222600     MOVE "MOVE-TEST-F1-58-20 " TO PAR-NAME.                      NC1044.2
222700     PERFORM PRINT-DETAIL.                                        NC1044.2
222800 MOVE-TEST-F1-58-21.                                              NC1044.2
222900     IF DNAME42 EQUAL TO 1                                        NC1044.2
223000         PERFORM PASS                                             NC1044.2
223100         GO TO MOVE-WRITE-F1-58-21.                               NC1044.2
223200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-58-1.    NC1044.2
223300     MOVE DNAME42 TO COMPUTED-18V0.                               NC1044.2
223400     MOVE 1 TO CORRECT-18V0.                                      NC1044.2
223500     PERFORM FAIL.                                                NC1044.2
223600     GO TO MOVE-WRITE-F1-58-21.                                   NC1044.2
223700 MOVE-DELETE-F1-58-21.                                            NC1044.2
223800     PERFORM DE-LETE.                                             NC1044.2
223900 MOVE-WRITE-F1-58-21.                                             NC1044.2
224000     MOVE "MOVE-TEST-F1-58-21 " TO PAR-NAME.                      NC1044.2
224100     PERFORM PRINT-DETAIL.                                        NC1044.2
224200 MOVE-INIT-F1-59-1.                                               NC1044.2
224300     MOVE ZERO TO DNAME22  DNAME23  DNAME24  DNAME25  DNAME26.    NC1044.2
224400     MOVE ZERO TO DNAME27  DNAME28  DNAME29  DNAME30  DNAME31.    NC1044.2
224500     MOVE ZERO TO DNAME32  DNAME33  DNAME34  DNAME35  DNAME36.    NC1044.2
224600     MOVE ZERO TO DNAME37  DNAME38  DNAME39  DNAME40  DNAME41.    NC1044.2
224700     MOVE ZERO TO DNAME42.                                        NC1044.2
224800 MOVE-TEST-F1-59-0.                                               NC1044.2
224900     MOVE "A" TO ANDATA1  ANDATA2  ANDATA3  ANDATA4  ANDATA5      NC1044.2
225000         ANDATA6  ANDATA7  ANDATA8  ANDATA9  ANDATA10  ANDATA11   NC1044.2
225100         ANDATA12  ANDATA13  ANDATA14  ANDATA15  ANDATA16         NC1044.2
225200         ANDATA17  ANDATA18  ANDATA19  ANDATA20  ANDATA21.        NC1044.2
225300 MOVE-TEST-F1-59-1.                                               NC1044.2
225400     IF ANDATA1  EQUAL TO "A"                                     NC1044.2
225500         PERFORM PASS                                             NC1044.2
225600         GO TO MOVE-WRITE-F1-59-1.                                NC1044.2
225700     MOVE ANDATA1  TO COMPUTED-A.                                 NC1044.2
225800     MOVE "A" TO CORRECT-A.                                       NC1044.2
225900     PERFORM FAIL.                                                NC1044.2
226000     GO TO MOVE-WRITE-F1-59-1.                                    NC1044.2
226100 MOVE-DELETE-F1-59-1.                                             NC1044.2
226200     PERFORM DE-LETE.                                             NC1044.2
226300 MOVE-WRITE-F1-59-1.                                              NC1044.2
226400     MOVE "MOVE-TEST-F1-59-1 " TO PAR-NAME.                       NC1044.2
226500     PERFORM PRINT-DETAIL.                                        NC1044.2
226600 MOVE-TEST-F1-59-2.                                               NC1044.2
226700     IF ANDATA2  EQUAL TO "A"                                     NC1044.2
226800         PERFORM PASS                                             NC1044.2
226900         GO TO MOVE-WRITE-F1-59-2.                                NC1044.2
227000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
227100     MOVE ANDATA2  TO COMPUTED-A.                                 NC1044.2
227200     MOVE "A" TO CORRECT-A.                                       NC1044.2
227300     PERFORM FAIL.                                                NC1044.2
227400     GO TO MOVE-WRITE-F1-59-2.                                    NC1044.2
227500 MOVE-DELETE-F1-59-2.                                             NC1044.2
227600     PERFORM DE-LETE.                                             NC1044.2
227700 MOVE-WRITE-F1-59-2.                                              NC1044.2
227800     MOVE "MOVE-TEST-F1-59-2 " TO PAR-NAME.                       NC1044.2
227900     PERFORM PRINT-DETAIL.                                        NC1044.2
228000 MOVE-TEST-F1-59-3.                                               NC1044.2
228100     IF ANDATA3  EQUAL TO "A"                                     NC1044.2
228200         PERFORM PASS                                             NC1044.2
228300         GO TO MOVE-WRITE-F1-59-3.                                NC1044.2
228400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
228500     MOVE ANDATA3  TO COMPUTED-A.                                 NC1044.2
228600     MOVE "A" TO CORRECT-A.                                       NC1044.2
228700     PERFORM FAIL.                                                NC1044.2
228800     GO TO MOVE-WRITE-F1-59-3.                                    NC1044.2
228900 MOVE-DELETE-F1-59-3.                                             NC1044.2
229000     PERFORM DE-LETE.                                             NC1044.2
229100 MOVE-WRITE-F1-59-3.                                              NC1044.2
229200     MOVE "MOVE-TEST-F1-59-3 " TO PAR-NAME.                       NC1044.2
229300     PERFORM PRINT-DETAIL.                                        NC1044.2
229400 MOVE-TEST-F1-59-4-4.                                             NC1044.2
229500     IF ANDATA4  EQUAL TO "A"                                     NC1044.2
229600         PERFORM PASS                                             NC1044.2
229700         GO TO MOVE-WRITE-F1-59-4.                                NC1044.2
229800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
229900     MOVE ANDATA4  TO COMPUTED-A.                                 NC1044.2
230000     MOVE "A" TO CORRECT-A.                                       NC1044.2
230100     PERFORM FAIL.                                                NC1044.2
230200     GO TO MOVE-WRITE-F1-59-4.                                    NC1044.2
230300 MOVE-DELETE-F1-59-4.                                             NC1044.2
230400     PERFORM DE-LETE.                                             NC1044.2
230500 MOVE-WRITE-F1-59-4.                                              NC1044.2
230600     MOVE "MOVE-TEST-F1-59-4 " TO PAR-NAME.                       NC1044.2
230700     PERFORM PRINT-DETAIL.                                        NC1044.2
230800 MOVE-TEST-F1-59-5.                                               NC1044.2
230900     IF ANDATA5  EQUAL TO "A"                                     NC1044.2
231000         PERFORM PASS                                             NC1044.2
231100         GO TO MOVE-WRITE-F1-59-5.                                NC1044.2
231200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
231300     MOVE ANDATA5  TO COMPUTED-A.                                 NC1044.2
231400     MOVE "A" TO CORRECT-A.                                       NC1044.2
231500     PERFORM FAIL.                                                NC1044.2
231600     GO TO MOVE-WRITE-F1-59-5.                                    NC1044.2
231700 MOVE-DELETE-F1-59-5.                                             NC1044.2
231800     PERFORM DE-LETE.                                             NC1044.2
231900 MOVE-WRITE-F1-59-5.                                              NC1044.2
232000     MOVE "MOVE-TEST-F1-59-5 " TO PAR-NAME.                       NC1044.2
232100     PERFORM PRINT-DETAIL.                                        NC1044.2
232200 MOVE-TEST-F1-59-6.                                               NC1044.2
232300     IF ANDATA6  EQUAL TO "A"                                     NC1044.2
232400         PERFORM PASS                                             NC1044.2
232500         GO TO MOVE-WRITE-F1-59-6.                                NC1044.2
232600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
232700     MOVE ANDATA6  TO COMPUTED-A.                                 NC1044.2
232800     MOVE "A" TO CORRECT-A.                                       NC1044.2
232900     PERFORM FAIL.                                                NC1044.2
233000     GO TO MOVE-WRITE-F1-59-6.                                    NC1044.2
233100 MOVE-DELETE-F1-59-6.                                             NC1044.2
233200     PERFORM DE-LETE.                                             NC1044.2
233300 MOVE-WRITE-F1-59-6.                                              NC1044.2
233400     MOVE "MOVE-TEST-F1-59-6 " TO PAR-NAME.                       NC1044.2
233500     PERFORM PRINT-DETAIL.                                        NC1044.2
233600 MOVE-TEST-F1-59-7.                                               NC1044.2
233700     IF ANDATA7  EQUAL TO "A"                                     NC1044.2
233800         PERFORM PASS                                             NC1044.2
233900         GO TO MOVE-WRITE-F1-59-7.                                NC1044.2
234000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
234100     MOVE ANDATA7  TO COMPUTED-A.                                 NC1044.2
234200     MOVE "A" TO CORRECT-A.                                       NC1044.2
234300     PERFORM FAIL.                                                NC1044.2
234400     GO TO MOVE-WRITE-F1-59-7.                                    NC1044.2
234500 MOVE-DELETE-F1-59-7.                                             NC1044.2
234600     PERFORM DE-LETE.                                             NC1044.2
234700 MOVE-WRITE-F1-59-7.                                              NC1044.2
234800     MOVE "MOVE-TEST-F1-59-7 " TO PAR-NAME.                       NC1044.2
234900     PERFORM PRINT-DETAIL.                                        NC1044.2
235000 MOVE-TEST-F1-59-8.                                               NC1044.2
235100     IF ANDATA8  EQUAL TO "A"                                     NC1044.2
235200         PERFORM PASS                                             NC1044.2
235300         GO TO MOVE-WRITE-F1-59-8.                                NC1044.2
235400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
235500     MOVE ANDATA8  TO COMPUTED-A.                                 NC1044.2
235600     MOVE "A" TO CORRECT-A.                                       NC1044.2
235700     PERFORM FAIL.                                                NC1044.2
235800     GO TO MOVE-WRITE-F1-59-8.                                    NC1044.2
235900 MOVE-DELETE-F1-59-8.                                             NC1044.2
236000     PERFORM DE-LETE.                                             NC1044.2
236100 MOVE-WRITE-F1-59-8.                                              NC1044.2
236200     MOVE "MOVE-TEST-F1-59-8 " TO PAR-NAME.                       NC1044.2
236300     PERFORM PRINT-DETAIL.                                        NC1044.2
236400 MOVE-TEST-F1-59-9.                                               NC1044.2
236500     IF ANDATA9  EQUAL TO "A"                                     NC1044.2
236600         PERFORM PASS                                             NC1044.2
236700         GO TO MOVE-WRITE-F1-59-9.                                NC1044.2
236800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
236900     MOVE ANDATA9  TO COMPUTED-A.                                 NC1044.2
237000     MOVE "A" TO CORRECT-A.                                       NC1044.2
237100     PERFORM FAIL.                                                NC1044.2
237200     GO TO MOVE-WRITE-F1-59-9.                                    NC1044.2
237300 MOVE-DELETE-F1-59-9.                                             NC1044.2
237400     PERFORM DE-LETE.                                             NC1044.2
237500 MOVE-WRITE-F1-59-9.                                              NC1044.2
237600     MOVE "MOVE-TEST-F1-59-9 " TO PAR-NAME.                       NC1044.2
237700     PERFORM PRINT-DETAIL.                                        NC1044.2
237800 MOVE-TEST-F1-59-10.                                              NC1044.2
237900     IF ANDATA10 EQUAL TO "A"                                     NC1044.2
238000         PERFORM PASS                                             NC1044.2
238100         GO TO MOVE-WRITE-F1-59-10.                               NC1044.2
238200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
238300     MOVE ANDATA10 TO COMPUTED-A.                                 NC1044.2
238400     MOVE "A" TO CORRECT-A.                                       NC1044.2
238500     PERFORM FAIL.                                                NC1044.2
238600     GO TO MOVE-WRITE-F1-59-10.                                   NC1044.2
238700 MOVE-DELETE-F1-59-10.                                            NC1044.2
238800     PERFORM DE-LETE.                                             NC1044.2
238900 MOVE-WRITE-F1-59-10.                                             NC1044.2
239000     MOVE "MOVE-TEST-F1-59-10 " TO PAR-NAME.                      NC1044.2
239100     PERFORM PRINT-DETAIL.                                        NC1044.2
239200 MOVE-TEST-F1-59-11.                                              NC1044.2
239300     IF ANDATA11 EQUAL TO "A"                                     NC1044.2
239400         PERFORM PASS                                             NC1044.2
239500         GO TO MOVE-WRITE-F1-59-11.                               NC1044.2
239600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
239700     MOVE ANDATA11 TO COMPUTED-A.                                 NC1044.2
239800     MOVE "A" TO CORRECT-A.                                       NC1044.2
239900     PERFORM FAIL.                                                NC1044.2
240000     GO TO MOVE-WRITE-F1-59-11.                                   NC1044.2
240100 MOVE-DELETE-F1-59-11.                                            NC1044.2
240200     PERFORM DE-LETE.                                             NC1044.2
240300 MOVE-WRITE-F1-59-11.                                             NC1044.2
240400     MOVE "MOVE-TEST-F1-59-11 " TO PAR-NAME.                      NC1044.2
240500     PERFORM PRINT-DETAIL.                                        NC1044.2
240600 MOVE-TEST-F1-59-12.                                              NC1044.2
240700     IF ANDATA12 EQUAL TO "A"                                     NC1044.2
240800         PERFORM PASS                                             NC1044.2
240900         GO TO MOVE-WRITE-F1-59-12.                               NC1044.2
241000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
241100     MOVE ANDATA12 TO COMPUTED-A.                                 NC1044.2
241200     MOVE "A" TO CORRECT-A.                                       NC1044.2
241300     PERFORM FAIL.                                                NC1044.2
241400     GO TO MOVE-WRITE-F1-59-12.                                   NC1044.2
241500 MOVE-DELETE-F1-59-12.                                            NC1044.2
241600     PERFORM DE-LETE.                                             NC1044.2
241700 MOVE-WRITE-F1-59-12.                                             NC1044.2
241800     MOVE "MOVE-TEST-F1-59-12 " TO PAR-NAME.                      NC1044.2
241900     PERFORM PRINT-DETAIL.                                        NC1044.2
242000 MOVE-TEST-F1-59-13.                                              NC1044.2
242100     IF ANDATA13 EQUAL TO "A"                                     NC1044.2
242200         PERFORM PASS                                             NC1044.2
242300         GO TO MOVE-WRITE-F1-59-13.                               NC1044.2
242400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
242500     MOVE ANDATA13 TO COMPUTED-A.                                 NC1044.2
242600     MOVE "A" TO CORRECT-A.                                       NC1044.2
242700     PERFORM FAIL.                                                NC1044.2
242800     GO TO MOVE-WRITE-F1-59-13.                                   NC1044.2
242900 MOVE-DELETE-F1-59-13.                                            NC1044.2
243000     PERFORM DE-LETE.                                             NC1044.2
243100 MOVE-WRITE-F1-59-13.                                             NC1044.2
243200     MOVE "MOVE-TEST-F1-59-13 " TO PAR-NAME.                      NC1044.2
243300     PERFORM PRINT-DETAIL.                                        NC1044.2
243400 MOVE-TEST-F1-59-14.                                              NC1044.2
243500     IF ANDATA14 EQUAL TO "A"                                     NC1044.2
243600         PERFORM PASS                                             NC1044.2
243700         GO TO MOVE-WRITE-F1-59-14.                               NC1044.2
243800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
243900     MOVE ANDATA14 TO COMPUTED-A.                                 NC1044.2
244000     MOVE "A" TO CORRECT-A.                                       NC1044.2
244100     PERFORM FAIL.                                                NC1044.2
244200     GO TO MOVE-WRITE-F1-59-14.                                   NC1044.2
244300 MOVE-DELETE-F1-59-14.                                            NC1044.2
244400     PERFORM DE-LETE.                                             NC1044.2
244500 MOVE-WRITE-F1-59-14.                                             NC1044.2
244600     MOVE "MOVE-TEST-F1-59-14 " TO PAR-NAME.                      NC1044.2
244700     PERFORM PRINT-DETAIL.                                        NC1044.2
244800 MOVE-TEST-F1-59-15.                                              NC1044.2
244900     IF ANDATA15 EQUAL TO "A"                                     NC1044.2
245000         PERFORM PASS                                             NC1044.2
245100         GO TO MOVE-WRITE-F1-59-15.                               NC1044.2
245200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
245300     MOVE ANDATA15 TO COMPUTED-A.                                 NC1044.2
245400     MOVE "A" TO CORRECT-A.                                       NC1044.2
245500     PERFORM FAIL.                                                NC1044.2
245600     GO TO MOVE-WRITE-F1-59-15.                                   NC1044.2
245700 MOVE-DELETE-F1-59-15.                                            NC1044.2
245800     PERFORM DE-LETE.                                             NC1044.2
245900 MOVE-WRITE-F1-59-15.                                             NC1044.2
246000     MOVE "MOVE-TEST-F1-59-15 " TO PAR-NAME.                      NC1044.2
246100     PERFORM PRINT-DETAIL.                                        NC1044.2
246200 MOVE-TEST-F1-59-16.                                              NC1044.2
246300     IF ANDATA16 EQUAL TO "A"                                     NC1044.2
246400         PERFORM PASS                                             NC1044.2
246500         GO TO MOVE-WRITE-F1-59-16.                               NC1044.2
246600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
246700     MOVE ANDATA16 TO COMPUTED-A.                                 NC1044.2
246800     MOVE "A" TO CORRECT-A.                                       NC1044.2
246900     PERFORM FAIL.                                                NC1044.2
247000     GO TO MOVE-WRITE-F1-59-16.                                   NC1044.2
247100 MOVE-DELETE-F1-59-16.                                            NC1044.2
247200     PERFORM DE-LETE.                                             NC1044.2
247300 MOVE-WRITE-F1-59-16.                                             NC1044.2
247400     MOVE "MOVE-TEST-F1-59-16 " TO PAR-NAME.                      NC1044.2
247500     PERFORM PRINT-DETAIL.                                        NC1044.2
247600 MOVE-TEST-F1-59-17.                                              NC1044.2
247700     IF ANDATA17 EQUAL TO "A"                                     NC1044.2
247800         PERFORM PASS                                             NC1044.2
247900         GO TO MOVE-WRITE-F1-59-17.                               NC1044.2
248000*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
248100     MOVE ANDATA17 TO COMPUTED-A.                                 NC1044.2
248200     MOVE "A" TO CORRECT-A.                                       NC1044.2
248300     PERFORM FAIL.                                                NC1044.2
248400     GO TO MOVE-WRITE-F1-59-17.                                   NC1044.2
248500 MOVE-DELETE-F1-59-17.                                            NC1044.2
248600     PERFORM DE-LETE.                                             NC1044.2
248700 MOVE-WRITE-F1-59-17.                                             NC1044.2
248800     MOVE "MOVE-TEST-F1-59-17 " TO PAR-NAME.                      NC1044.2
248900     PERFORM PRINT-DETAIL.                                        NC1044.2
249000 MOVE-TEST-F1-59-18.                                              NC1044.2
249100     IF ANDATA18 EQUAL TO "A"                                     NC1044.2
249200         PERFORM PASS                                             NC1044.2
249300         GO TO MOVE-WRITE-F1-59-18.                               NC1044.2
249400*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
249500     MOVE ANDATA18 TO COMPUTED-A.                                 NC1044.2
249600     MOVE "A" TO CORRECT-A.                                       NC1044.2
249700     PERFORM FAIL.                                                NC1044.2
249800     GO TO MOVE-WRITE-F1-59-18.                                   NC1044.2
249900 MOVE-DELETE-F1-59-18.                                            NC1044.2
250000     PERFORM DE-LETE.                                             NC1044.2
250100 MOVE-WRITE-F1-59-18.                                             NC1044.2
250200     MOVE "MOVE-TEST-F1-59-18 " TO PAR-NAME.                      NC1044.2
250300     PERFORM PRINT-DETAIL.                                        NC1044.2
250400 MOVE-TEST-F1-59-19.                                              NC1044.2
250500     IF ANDATA19 EQUAL TO "A"                                     NC1044.2
250600         PERFORM PASS                                             NC1044.2
250700         GO TO MOVE-WRITE-F1-59-19.                               NC1044.2
250800*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
250900     MOVE ANDATA19 TO COMPUTED-A.                                 NC1044.2
251000     MOVE "A" TO CORRECT-A.                                       NC1044.2
251100     PERFORM FAIL.                                                NC1044.2
251200     GO TO MOVE-WRITE-F1-59-19.                                   NC1044.2
251300 MOVE-DELETE-F1-59-19.                                            NC1044.2
251400     PERFORM DE-LETE.                                             NC1044.2
251500 MOVE-WRITE-F1-59-19.                                             NC1044.2
251600     MOVE "MOVE-TEST-F1-59-19 " TO PAR-NAME.                      NC1044.2
251700     PERFORM PRINT-DETAIL.                                        NC1044.2
251800 MOVE-TEST-F1-59-20.                                              NC1044.2
251900     IF ANDATA20 EQUAL TO "A"                                     NC1044.2
252000         PERFORM PASS                                             NC1044.2
252100         GO TO MOVE-WRITE-F1-59-20.                               NC1044.2
252200*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
252300     MOVE ANDATA20 TO COMPUTED-A.                                 NC1044.2
252400     MOVE "A" TO CORRECT-A.                                       NC1044.2
252500     PERFORM FAIL.                                                NC1044.2
252600     GO TO MOVE-WRITE-F1-59-20.                                   NC1044.2
252700 MOVE-DELETE-F1-59-20.                                            NC1044.2
252800     PERFORM DE-LETE.                                             NC1044.2
252900 MOVE-WRITE-F1-59-20.                                             NC1044.2
253000     MOVE "MOVE-TEST-F1-59-20 " TO PAR-NAME.                      NC1044.2
253100     PERFORM PRINT-DETAIL.                                        NC1044.2
253200 MOVE-TEST-F1-59-21.                                              NC1044.2
253300     IF ANDATA21 EQUAL TO "A"                                     NC1044.2
253400         PERFORM PASS                                             NC1044.2
253500         GO TO MOVE-WRITE-F1-59-21.                               NC1044.2
253600*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-59-1.    NC1044.2
253700     MOVE ANDATA21 TO COMPUTED-A.                                 NC1044.2
253800     MOVE "A" TO CORRECT-A.                                       NC1044.2
253900     PERFORM FAIL.                                                NC1044.2
254000     GO TO MOVE-WRITE-F1-59-21.                                   NC1044.2
254100 MOVE-DELETE-F1-59-21.                                            NC1044.2
254200     PERFORM DE-LETE.                                             NC1044.2
254300 MOVE-WRITE-F1-59-21.                                             NC1044.2
254400     MOVE "MOVE-TEST-F1-59-21 " TO PAR-NAME.                      NC1044.2
254500     PERFORM PRINT-DETAIL.                                        NC1044.2
254600 MOVE-INIT-F1-60.                                                 NC1044.2
254700*                                                                 NC1044.2
254800 MOVE-TEST-F1-60-0.                                               NC1044.2
254900     MOVE "ABCDEFGHIJKLMNOPQRSTU" TO ANDATA1  ANDATA2  ANDATA3    NC1044.2
255000         ANDATA4  ANDATA5  ANDATA6  ANDATA7  ANDATA8  ANDATA9     NC1044.2
255100         ANDATA10  ANDATA11  ANDATA12  ANDATA13  ANDATA14         NC1044.2
255200         ANDATA15  ANDATA16  ANDATA17  ANDATA18  ANDATA19         NC1044.2
255300         ANDATA20  ANDATA21.                                      NC1044.2
255400 MOVE-TEST-F1-60-1.                                               NC1044.2
255500     IF ANDATA1  EQUAL TO "A"                                     NC1044.2
255600         PERFORM PASS                                             NC1044.2
255700         GO TO MOVE-WRITE-F1-60-1.                                NC1044.2
255800     MOVE ANDATA1  TO COMPUTED-A.                                 NC1044.2
255900     MOVE "A" TO CORRECT-A.                                       NC1044.2
256000     PERFORM FAIL.                                                NC1044.2
256100     GO TO MOVE-WRITE-F1-60-1.                                    NC1044.2
256200 MOVE-DELETE-F1-60-1.                                             NC1044.2
256300     PERFORM DE-LETE.                                             NC1044.2
256400 MOVE-WRITE-F1-60-1.                                              NC1044.2
256500     MOVE "MOVE-TEST-F1-60-1 " TO PAR-NAME.                       NC1044.2
256600     PERFORM PRINT-DETAIL.                                        NC1044.2
256700 MOVE-TEST-F1-60-2.                                               NC1044.2
256800     IF ANDATA2  EQUAL TO "AB"                                    NC1044.2
256900         PERFORM PASS                                             NC1044.2
257000         GO TO MOVE-WRITE-F1-60-2.                                NC1044.2
257100*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
257200     MOVE ANDATA2  TO COMPUTED-A.                                 NC1044.2
257300     MOVE "AB"                   TO CORRECT-A.                    NC1044.2
257400     PERFORM FAIL.                                                NC1044.2
257500     GO TO MOVE-WRITE-F1-60-2.                                    NC1044.2
257600 MOVE-DELETE-F1-60-2.                                             NC1044.2
257700     PERFORM DE-LETE.                                             NC1044.2
257800 MOVE-WRITE-F1-60-2.                                              NC1044.2
257900     MOVE "MOVE-TEST-F1-60-2 " TO PAR-NAME.                       NC1044.2
258000     PERFORM PRINT-DETAIL.                                        NC1044.2
258100 MOVE-TEST-F1-60-3.                                               NC1044.2
258200     IF ANDATA3  EQUAL TO "ABC"                                   NC1044.2
258300         PERFORM PASS                                             NC1044.2
258400         GO TO MOVE-WRITE-F1-60-3.                                NC1044.2
258500*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
258600     MOVE ANDATA3  TO COMPUTED-A.                                 NC1044.2
258700     MOVE "ABC"                  TO CORRECT-A.                    NC1044.2
258800     PERFORM FAIL.                                                NC1044.2
258900     GO TO MOVE-WRITE-F1-60-3.                                    NC1044.2
259000 MOVE-DELETE-F1-60-3.                                             NC1044.2
259100     PERFORM DE-LETE.                                             NC1044.2
259200 MOVE-WRITE-F1-60-3.                                              NC1044.2
259300     MOVE "MOVE-TEST-F1-60-3 " TO PAR-NAME.                       NC1044.2
259400     PERFORM PRINT-DETAIL.                                        NC1044.2
259500 MOVE-TEST-F1-60-4.                                               NC1044.2
259600     IF ANDATA4  EQUAL TO "ABCD"                                  NC1044.2
259700         PERFORM PASS                                             NC1044.2
259800         GO TO MOVE-WRITE-F1-60-4.                                NC1044.2
259900*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
260000     MOVE ANDATA4  TO COMPUTED-A.                                 NC1044.2
260100     MOVE "ABCD"                 TO CORRECT-A.                    NC1044.2
260200     PERFORM FAIL.                                                NC1044.2
260300     GO TO MOVE-WRITE-F1-60-4.                                    NC1044.2
260400 MOVE-DELETE-F1-60-4.                                             NC1044.2
260500     PERFORM DE-LETE.                                             NC1044.2
260600 MOVE-WRITE-F1-60-4.                                              NC1044.2
260700     MOVE "MOVE-TEST-F1-60-4 " TO PAR-NAME.                       NC1044.2
260800     PERFORM PRINT-DETAIL.                                        NC1044.2
260900 MOVE-TEST-F1-60-5.                                               NC1044.2
261000     IF ANDATA5  EQUAL TO "ABCDE"                                 NC1044.2
261100         PERFORM PASS                                             NC1044.2
261200         GO TO MOVE-WRITE-F1-60-5.                                NC1044.2
261300*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
261400     MOVE ANDATA5  TO COMPUTED-A.                                 NC1044.2
261500     MOVE "ABCDE"                TO CORRECT-A.                    NC1044.2
261600     PERFORM FAIL.                                                NC1044.2
261700     GO TO MOVE-WRITE-F1-60-5.                                    NC1044.2
261800 MOVE-DELETE-F1-60-5.                                             NC1044.2
261900     PERFORM DE-LETE.                                             NC1044.2
262000 MOVE-WRITE-F1-60-5.                                              NC1044.2
262100     MOVE "MOVE-TEST-F1-60-5 " TO PAR-NAME.                       NC1044.2
262200     PERFORM PRINT-DETAIL.                                        NC1044.2
262300 MOVE-TEST-F1-60-6.                                               NC1044.2
262400     IF ANDATA6  EQUAL TO "ABCDEF"                                NC1044.2
262500         PERFORM PASS                                             NC1044.2
262600         GO TO MOVE-WRITE-F1-60-6.                                NC1044.2
262700*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
262800     MOVE ANDATA6  TO COMPUTED-A.                                 NC1044.2
262900     MOVE "ABCDEF"               TO CORRECT-A.                    NC1044.2
263000     PERFORM FAIL.                                                NC1044.2
263100     GO TO MOVE-WRITE-F1-60-6.                                    NC1044.2
263200 MOVE-DELETE-F1-60-6.                                             NC1044.2
263300     PERFORM DE-LETE.                                             NC1044.2
263400 MOVE-WRITE-F1-60-6.                                              NC1044.2
263500     MOVE "MOVE-TEST-F1-60-6 " TO PAR-NAME.                       NC1044.2
263600     PERFORM PRINT-DETAIL.                                        NC1044.2
263700 MOVE-TEST-F1-60-7.                                               NC1044.2
263800     IF ANDATA7  EQUAL TO "ABCDEFG"                               NC1044.2
263900         PERFORM PASS                                             NC1044.2
264000         GO TO MOVE-WRITE-F1-60-7.                                NC1044.2
264100*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
264200     MOVE ANDATA7  TO COMPUTED-A.                                 NC1044.2
264300     MOVE "ABCDEFG"              TO CORRECT-A.                    NC1044.2
264400     PERFORM FAIL.                                                NC1044.2
264500     GO TO MOVE-WRITE-F1-60-7.                                    NC1044.2
264600 MOVE-DELETE-F1-60-7.                                             NC1044.2
264700     PERFORM DE-LETE.                                             NC1044.2
264800 MOVE-WRITE-F1-60-7.                                              NC1044.2
264900     MOVE "MOVE-TEST-F1-60-7 " TO PAR-NAME.                       NC1044.2
265000     PERFORM PRINT-DETAIL.                                        NC1044.2
265100 MOVE-TEST-F1-60-8.                                               NC1044.2
265200     IF ANDATA8  EQUAL TO "ABCDEFGH"                              NC1044.2
265300         PERFORM PASS                                             NC1044.2
265400         GO TO MOVE-WRITE-F1-60-8.                                NC1044.2
265500*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
265600     MOVE ANDATA8  TO COMPUTED-A.                                 NC1044.2
265700     MOVE "ABCDEFGH"             TO CORRECT-A.                    NC1044.2
265800     PERFORM FAIL.                                                NC1044.2
265900     GO TO MOVE-WRITE-F1-60-8.                                    NC1044.2
266000 MOVE-DELETE-F1-60-8.                                             NC1044.2
266100     PERFORM DE-LETE.                                             NC1044.2
266200 MOVE-WRITE-F1-60-8.                                              NC1044.2
266300     MOVE "MOVE-TEST-F1-60-8 " TO PAR-NAME.                       NC1044.2
266400     PERFORM PRINT-DETAIL.                                        NC1044.2
266500 MOVE-TEST-F1-60-9.                                               NC1044.2
266600     IF ANDATA9  EQUAL TO "ABCDEFGHI"                             NC1044.2
266700         PERFORM PASS                                             NC1044.2
266800         GO TO MOVE-WRITE-F1-60-9.                                NC1044.2
266900*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
267000     MOVE ANDATA9  TO COMPUTED-A.                                 NC1044.2
267100     MOVE "ABCDEFGHI"            TO CORRECT-A.                    NC1044.2
267200     PERFORM FAIL.                                                NC1044.2
267300     GO TO MOVE-WRITE-F1-60-9.                                    NC1044.2
267400 MOVE-DELETE-F1-60-9.                                             NC1044.2
267500     PERFORM DE-LETE.                                             NC1044.2
267600 MOVE-WRITE-F1-60-9.                                              NC1044.2
267700     MOVE "MOVE-TEST-F1-60-9 " TO PAR-NAME.                       NC1044.2
267800     PERFORM PRINT-DETAIL.                                        NC1044.2
267900 MOVE-TEST-F1-60-10.                                              NC1044.2
268000     IF ANDATA10 EQUAL TO "ABCDEFGHIJ"                            NC1044.2
268100         PERFORM PASS                                             NC1044.2
268200         GO TO MOVE-WRITE-F1-60-10.                               NC1044.2
268300*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
268400     MOVE ANDATA10 TO COMPUTED-A.                                 NC1044.2
268500     MOVE "ABCDEFGHIJ"           TO CORRECT-A.                    NC1044.2
268600     PERFORM FAIL.                                                NC1044.2
268700     GO TO MOVE-WRITE-F1-60-10.                                   NC1044.2
268800 MOVE-DELETE-F1-60-10.                                            NC1044.2
268900     PERFORM DE-LETE.                                             NC1044.2
269000 MOVE-WRITE-F1-60-10.                                             NC1044.2
269100     MOVE "MOVE-TEST-F1-60-10 " TO PAR-NAME.                      NC1044.2
269200     PERFORM PRINT-DETAIL.                                        NC1044.2
269300 MOVE-TEST-F1-60-11.                                              NC1044.2
269400     IF ANDATA11 EQUAL TO "ABCDEFGHIJK"                           NC1044.2
269500         PERFORM PASS                                             NC1044.2
269600         GO TO MOVE-WRITE-F1-60-11.                               NC1044.2
269700*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
269800     MOVE ANDATA11 TO COMPUTED-A.                                 NC1044.2
269900     MOVE "ABCDEFGHIJK"          TO CORRECT-A.                    NC1044.2
270000     PERFORM FAIL.                                                NC1044.2
270100     GO TO MOVE-WRITE-F1-60-11.                                   NC1044.2
270200 MOVE-DELETE-F1-60-11.                                            NC1044.2
270300     PERFORM DE-LETE.                                             NC1044.2
270400 MOVE-WRITE-F1-60-11.                                             NC1044.2
270500     MOVE "MOVE-TEST-F1-60-11 " TO PAR-NAME.                      NC1044.2
270600     PERFORM PRINT-DETAIL.                                        NC1044.2
270700 MOVE-TEST-F1-60-12.                                              NC1044.2
270800     IF ANDATA12 EQUAL TO "ABCDEFGHIJKL"                          NC1044.2
270900         PERFORM PASS                                             NC1044.2
271000         GO TO MOVE-WRITE-F1-60-12.                               NC1044.2
271100*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
271200     MOVE ANDATA12 TO COMPUTED-A.                                 NC1044.2
271300     MOVE "ABCDEFGHIJKL"         TO CORRECT-A.                    NC1044.2
271400     PERFORM FAIL.                                                NC1044.2
271500     GO TO MOVE-WRITE-F1-60-12.                                   NC1044.2
271600 MOVE-DELETE-F1-60-12.                                            NC1044.2
271700     PERFORM DE-LETE.                                             NC1044.2
271800 MOVE-WRITE-F1-60-12.                                             NC1044.2
271900     MOVE "MOVE-TEST-F1-60-12 " TO PAR-NAME.                      NC1044.2
272000     PERFORM PRINT-DETAIL.                                        NC1044.2
272100 MOVE-TEST-F1-60-13.                                              NC1044.2
272200     IF ANDATA13 EQUAL TO "ABCDEFGHIJKLM"                         NC1044.2
272300         PERFORM PASS                                             NC1044.2
272400         GO TO MOVE-WRITE-F1-60-13.                               NC1044.2
272500*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
272600     MOVE ANDATA13 TO COMPUTED-A.                                 NC1044.2
272700     MOVE "ABCDEFGHIJKLM"        TO CORRECT-A.                    NC1044.2
272800     PERFORM FAIL.                                                NC1044.2
272900     GO TO MOVE-WRITE-F1-60-13.                                   NC1044.2
273000 MOVE-DELETE-F1-60-13.                                            NC1044.2
273100     PERFORM DE-LETE.                                             NC1044.2
273200 MOVE-WRITE-F1-60-13.                                             NC1044.2
273300     MOVE "MOVE-TEST-F1-60-13 " TO PAR-NAME.                      NC1044.2
273400     PERFORM PRINT-DETAIL.                                        NC1044.2
273500 MOVE-TEST-F1-60-14.                                              NC1044.2
273600     IF ANDATA14 EQUAL TO "ABCDEFGHIJKLMN"                        NC1044.2
273700         PERFORM PASS                                             NC1044.2
273800         GO TO MOVE-WRITE-F1-60-14.                               NC1044.2
273900*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
274000     MOVE ANDATA14 TO COMPUTED-A.                                 NC1044.2
274100     MOVE "ABCDEFGHIJKLMN"       TO CORRECT-A.                    NC1044.2
274200     PERFORM FAIL.                                                NC1044.2
274300     GO TO MOVE-WRITE-F1-60-14.                                   NC1044.2
274400 MOVE-DELETE-F1-60-14.                                            NC1044.2
274500     PERFORM DE-LETE.                                             NC1044.2
274600 MOVE-WRITE-F1-60-14.                                             NC1044.2
274700     MOVE "MOVE-TEST-F1-60-14 " TO PAR-NAME.                      NC1044.2
274800     PERFORM PRINT-DETAIL.                                        NC1044.2
274900 MOVE-TEST-F1-60-15.                                              NC1044.2
275000     IF ANDATA15 EQUAL TO "ABCDEFGHIJKLMNO"                       NC1044.2
275100         PERFORM PASS                                             NC1044.2
275200         GO TO MOVE-WRITE-F1-60-15.                               NC1044.2
275300*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
275400     MOVE ANDATA15 TO COMPUTED-A.                                 NC1044.2
275500     MOVE "ABCDEFGHIJKLMNO"      TO CORRECT-A.                    NC1044.2
275600     PERFORM FAIL.                                                NC1044.2
275700     GO TO MOVE-WRITE-F1-60-15.                                   NC1044.2
275800 MOVE-DELETE-F1-60-15.                                            NC1044.2
275900     PERFORM DE-LETE.                                             NC1044.2
276000 MOVE-WRITE-F1-60-15.                                             NC1044.2
276100     MOVE "MOVE-TEST-F1-60-15 " TO PAR-NAME.                      NC1044.2
276200     PERFORM PRINT-DETAIL.                                        NC1044.2
276300 MOVE-TEST-F1-60-16.                                              NC1044.2
276400     IF ANDATA16 EQUAL TO "ABCDEFGHIJKLMNOP"                      NC1044.2
276500         PERFORM PASS                                             NC1044.2
276600         GO TO MOVE-WRITE-F1-60-16.                               NC1044.2
276700*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
276800     MOVE ANDATA16 TO COMPUTED-A.                                 NC1044.2
276900     MOVE "ABCDEFGHIJKLMNOP"     TO CORRECT-A.                    NC1044.2
277000     PERFORM FAIL.                                                NC1044.2
277100     GO TO MOVE-WRITE-F1-60-16.                                   NC1044.2
277200 MOVE-DELETE-F1-60-16.                                            NC1044.2
277300     PERFORM DE-LETE.                                             NC1044.2
277400 MOVE-WRITE-F1-60-16.                                             NC1044.2
277500     MOVE "MOVE-TEST-F1-60-16 " TO PAR-NAME.                      NC1044.2
277600     PERFORM PRINT-DETAIL.                                        NC1044.2
277700 MOVE-TEST-F1-60-17.                                              NC1044.2
277800     IF ANDATA17 EQUAL TO "ABCDEFGHIJKLMNOPQ"                     NC1044.2
277900         PERFORM PASS                                             NC1044.2
278000         GO TO MOVE-WRITE-F1-60-17.                               NC1044.2
278100*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
278200     MOVE ANDATA17 TO COMPUTED-A.                                 NC1044.2
278300     MOVE "ABCDEFGHIJKLMNOPQ"    TO CORRECT-A.                    NC1044.2
278400     PERFORM FAIL.                                                NC1044.2
278500     GO TO MOVE-WRITE-F1-60-17.                                   NC1044.2
278600 MOVE-DELETE-F1-60-17.                                            NC1044.2
278700     PERFORM DE-LETE.                                             NC1044.2
278800 MOVE-WRITE-F1-60-17.                                             NC1044.2
278900     MOVE "MOVE-TEST-F1-60-17 " TO PAR-NAME.                      NC1044.2
279000     PERFORM PRINT-DETAIL.                                        NC1044.2
279100 MOVE-TEST-F1-60-18.                                              NC1044.2
279200     IF ANDATA18 EQUAL TO "ABCDEFGHIJKLMNOPQR"                    NC1044.2
279300         PERFORM PASS                                             NC1044.2
279400         GO TO MOVE-WRITE-F1-60-18.                               NC1044.2
279500*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
279600     MOVE ANDATA18 TO COMPUTED-A.                                 NC1044.2
279700     MOVE "ABCDEFGHIJKLMNOPQR"   TO CORRECT-A.                    NC1044.2
279800     PERFORM FAIL.                                                NC1044.2
279900     GO TO MOVE-WRITE-F1-60-18.                                   NC1044.2
280000 MOVE-DELETE-F1-60-18.                                            NC1044.2
280100     PERFORM DE-LETE.                                             NC1044.2
280200 MOVE-WRITE-F1-60-18.                                             NC1044.2
280300     MOVE "MOVE-TEST-F1-60-18 " TO PAR-NAME.                      NC1044.2
280400     PERFORM PRINT-DETAIL.                                        NC1044.2
280500 MOVE-TEST-F1-60-19.                                              NC1044.2
280600     IF ANDATA19 EQUAL TO "ABCDEFGHIJKLMNOPQRS"                   NC1044.2
280700         PERFORM PASS                                             NC1044.2
280800         GO TO MOVE-WRITE-F1-60-19.                               NC1044.2
280900*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
281000     MOVE ANDATA19 TO COMPUTED-A.                                 NC1044.2
281100     MOVE "ABCDEFGHIJKLMNOPQRS"  TO CORRECT-A.                    NC1044.2
281200     PERFORM FAIL.                                                NC1044.2
281300     GO TO MOVE-WRITE-F1-60-19.                                   NC1044.2
281400 MOVE-DELETE-F1-60-19.                                            NC1044.2
281500     PERFORM DE-LETE.                                             NC1044.2
281600 MOVE-WRITE-F1-60-19.                                             NC1044.2
281700     MOVE "MOVE-TEST-F1-60-19 " TO PAR-NAME.                      NC1044.2
281800     PERFORM PRINT-DETAIL.                                        NC1044.2
281900 MOVE-TEST-F1-60-20.                                              NC1044.2
282000     IF ANDATA20 EQUAL TO "ABCDEFGHIJKLMNOPQRST"                  NC1044.2
282100         PERFORM PASS                                             NC1044.2
282200         GO TO MOVE-WRITE-F1-60-20.                               NC1044.2
282300*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
282400     MOVE ANDATA20 TO COMPUTED-A.                                 NC1044.2
282500     MOVE "ABCDEFGHIJKLMNOPQRST" TO CORRECT-A.                    NC1044.2
282600     PERFORM FAIL.                                                NC1044.2
282700     GO TO MOVE-WRITE-F1-60-20.                                   NC1044.2
282800 MOVE-DELETE-F1-60-20.                                            NC1044.2
282900     PERFORM DE-LETE.                                             NC1044.2
283000 MOVE-WRITE-F1-60-20.                                             NC1044.2
283100     MOVE "MOVE-TEST-F1-60-20 " TO PAR-NAME.                      NC1044.2
283200     PERFORM PRINT-DETAIL.                                        NC1044.2
283300 MOVE-TEST-F1-60-21.                                              NC1044.2
283400     IF ANDATA21 EQUAL TO "ABCDEFGHIJKLMNOPQRSTU"                 NC1044.2
283500         PERFORM PASS                                             NC1044.2
283600         GO TO MOVE-WRITE-F1-60-21.                               NC1044.2
283700*    NOTE   THIS TEST DEPENDS ON PARAGRAPH  MOVE-TEST-F1-60-1.    NC1044.2
283800     MOVE "SEE RE-MARK COL" TO COMPUTED-A.                        NC1044.2
283900     MOVE ANDATA21 TO RE-MARK.                                    NC1044.2
284000     MOVE "ALPHABET A THRU U" TO CORRECT-A.                       NC1044.2
284100     PERFORM FAIL.                                                NC1044.2
284200     GO TO MOVE-WRITE-F1-60-21.                                   NC1044.2
284300 MOVE-DELETE-F1-60-21.                                            NC1044.2
284400     PERFORM DE-LETE.                                             NC1044.2
284500 MOVE-WRITE-F1-60-21.                                             NC1044.2
284600     MOVE "MOVE-TEST-F1-60-21 " TO PAR-NAME.                      NC1044.2
284700     PERFORM PRINT-DETAIL.                                        NC1044.2
284800     PERFORM END-ROUTINE.                                         NC1044.2
284900 CCVS-EXIT SECTION.                                               NC1044.2
285000 CCVS-999999.                                                     NC1044.2
285100     GO TO CLOSE-FILES.                                           NC1044.2
      *END-OF,NC104A                                                                  
