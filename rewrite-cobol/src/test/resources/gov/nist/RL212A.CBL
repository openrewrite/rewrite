      *HEADER,COBOL,RL212A                                                            
000100 IDENTIFICATION DIVISION.                                         RL2124.2
000200 PROGRAM-ID.                                                      RL2124.2
000300     RL212A.                                                      RL2124.2
000400****************************************************************  RL2124.2
000500*                                                              *  RL2124.2
000600*    VALIDATION FOR:-                                          *  RL2124.2
000700*                                                              *  RL2124.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2124.2
000900*                                                              *  RL2124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2124.2
001100*                                                              *  RL2124.2
001200****************************************************************  RL2124.2
001300*    THIS RUN UNIT IS THE FIRST OF A SERIES OF TWO PROGRAMS    *  RL2124.2
001400*    PROCESSES A RELATIVE I-O FILE.  THE FUNCTION OF THIS      *  RL2124.2
001500*    PROGRAM IS TO CREATE A RELATIVE FILE SEQUENTIALLY         *  RL2124.2
001600*    (ACCESS MODE SEQUENTIAL) AND VERIFY THAT IT WAS           *  RL2124.2
001700*    CREATED CORRECTLY.  THE FILE IS IDENTIFED AS "RL-FS1"     *  RL2124.2
001800*    AND WILL CONTAIN 500 RECORDS OF 120 CHARACTERS.           *  RL2124.2
001900*                                                              *  RL2124.2
002000*        X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS     *  RL2124.2
002100*        PROGRAM ARE:                                          *  RL2124.2
002200*                                                              *  RL2124.2
002300*             X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR  *  RL2124.2
002400*                     RELATIVE  I-O DATA FILE                  *  RL2124.2
002500*             X-55   SYSTEM PRINTER                            *  RL2124.2
002600*             X-69   ADDITIONAL VALUE OF CLAUSES               *  RL2124.2
002700*             X-74   VALUE OF IMPLEMENTOR-NAME                 *  RL2124.2
002800*             X-75   OBJECT OF VALUE OF CLAUSE                 *  RL2124.2
002900*             X-82   SOURCE-COMPUTER                           *  RL2124.2
003000*             X-83   OBJECT-COMPUTER.                          *  RL2124.2
003100*                                                              *  RL2124.2
003200****************************************************************  RL2124.2
003300 ENVIRONMENT DIVISION.                                            RL2124.2
003400 CONFIGURATION SECTION.                                           RL2124.2
003500 SOURCE-COMPUTER.                                                 RL2124.2
003600     XXXXX082.                                                    RL2124.2
003700 OBJECT-COMPUTER.                                                 RL2124.2
003800     XXXXX083.                                                    RL2124.2
003900 INPUT-OUTPUT SECTION.                                            RL2124.2
004000 FILE-CONTROL.                                                    RL2124.2
004100     SELECT PRINT-FILE ASSIGN TO                                  RL2124.2
004200     XXXXX055.                                                    RL2124.2
004300     SELECT   RL-FS1 ASSIGN TO                                    RL2124.2
004400     XXXXP021                                                     RL2124.2
004500             ORGANIZATION IS RELATIVE.                            RL2124.2
004600*    ABSENCE OF THE ACCESS CLAUSE IS TREATED AS THOUGH            RL2124.2
004700*     SEQUENTIAL HAD BEEN SPECIFIED.                              RL2124.2
004800 DATA DIVISION.                                                   RL2124.2
004900 FILE SECTION.                                                    RL2124.2
005000 FD  PRINT-FILE.                                                  RL2124.2
005100 01  PRINT-REC PICTURE X(120).                                    RL2124.2
005200 01  DUMMY-RECORD PICTURE X(120).                                 RL2124.2
005300 FD  RL-FS1                                                       RL2124.2
005400     LABEL RECORDS STANDARD                                       RL2124.2
005500C    VALUE OF                                                     RL2124.2
005600C    XXXXX074                                                     RL2124.2
005700C    IS                                                           RL2124.2
005800C    XXXXX075                                                     RL2124.2
005900*    XXXXX069                                                     RL2124.2
006000     BLOCK CONTAINS 1 RECORDS                                     RL2124.2
006100     RECORD CONTAINS 120 CHARACTERS.                              RL2124.2
006200 01  RL-FS1R1-F-G-120.                                            RL2124.2
006300     02 FILLER PIC X(120).                                        RL2124.2
006400 WORKING-STORAGE SECTION.                                         RL2124.2
006500 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                RL2124.2
006600 01  FILE-RECORD-INFORMATION-REC.                                 RL2124.2
006700     03 FILE-RECORD-INFO-SKELETON.                                RL2124.2
006800        05 FILLER                 PICTURE X(48)       VALUE       RL2124.2
006900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2124.2
007000        05 FILLER                 PICTURE X(46)       VALUE       RL2124.2
007100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2124.2
007200        05 FILLER                 PICTURE X(26)       VALUE       RL2124.2
007300             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2124.2
007400        05 FILLER                 PICTURE X(37)       VALUE       RL2124.2
007500             ",RECKEY=                             ".             RL2124.2
007600        05 FILLER                 PICTURE X(38)       VALUE       RL2124.2
007700             ",ALTKEY1=                             ".            RL2124.2
007800        05 FILLER                 PICTURE X(38)       VALUE       RL2124.2
007900             ",ALTKEY2=                             ".            RL2124.2
008000        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2124.2
008100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2124.2
008200        05 FILE-RECORD-INFO-P1-120.                               RL2124.2
008300           07 FILLER              PIC X(5).                       RL2124.2
008400           07 XFILE-NAME           PIC X(6).                      RL2124.2
008500           07 FILLER              PIC X(8).                       RL2124.2
008600           07 XRECORD-NAME         PIC X(6).                      RL2124.2
008700           07 FILLER              PIC X(1).                       RL2124.2
008800           07 REELUNIT-NUMBER     PIC 9(1).                       RL2124.2
008900           07 FILLER              PIC X(7).                       RL2124.2
009000           07 XRECORD-NUMBER       PIC 9(6).                      RL2124.2
009100           07 FILLER              PIC X(6).                       RL2124.2
009200           07 UPDATE-NUMBER       PIC 9(2).                       RL2124.2
009300           07 FILLER              PIC X(5).                       RL2124.2
009400           07 ODO-NUMBER          PIC 9(4).                       RL2124.2
009500           07 FILLER              PIC X(5).                       RL2124.2
009600           07 XPROGRAM-NAME        PIC X(5).                      RL2124.2
009700           07 FILLER              PIC X(7).                       RL2124.2
009800           07 XRECORD-LENGTH       PIC 9(6).                      RL2124.2
009900           07 FILLER              PIC X(7).                       RL2124.2
010000           07 CHARS-OR-RECORDS    PIC X(2).                       RL2124.2
010100           07 FILLER              PIC X(1).                       RL2124.2
010200           07 XBLOCK-SIZE          PIC 9(4).                      RL2124.2
010300           07 FILLER              PIC X(6).                       RL2124.2
010400           07 RECORDS-IN-FILE     PIC 9(6).                       RL2124.2
010500           07 FILLER              PIC X(5).                       RL2124.2
010600           07 XFILE-ORGANIZATION   PIC X(2).                      RL2124.2
010700           07 FILLER              PIC X(6).                       RL2124.2
010800           07 XLABEL-TYPE          PIC X(1).                      RL2124.2
010900        05 FILE-RECORD-INFO-P121-240.                             RL2124.2
011000           07 FILLER              PIC X(8).                       RL2124.2
011100           07 XRECORD-KEY          PIC X(29).                     RL2124.2
011200           07 FILLER              PIC X(9).                       RL2124.2
011300           07 ALTERNATE-KEY1      PIC X(29).                      RL2124.2
011400           07 FILLER              PIC X(9).                       RL2124.2
011500           07 ALTERNATE-KEY2      PIC X(29).                      RL2124.2
011600           07 FILLER              PIC X(7).                       RL2124.2
011700 01  TEST-RESULTS.                                                RL2124.2
011800     02 FILLER                   PIC X      VALUE SPACE.          RL2124.2
011900     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2124.2
012000     02 FILLER                   PIC X      VALUE SPACE.          RL2124.2
012100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2124.2
012200     02 FILLER                   PIC X      VALUE SPACE.          RL2124.2
012300     02  PAR-NAME.                                                RL2124.2
012400       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2124.2
012500       03  PARDOT-X              PIC X      VALUE SPACE.          RL2124.2
012600       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2124.2
012700     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2124.2
012800     02 RE-MARK                  PIC X(61).                       RL2124.2
012900 01  TEST-COMPUTED.                                               RL2124.2
013000     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2124.2
013100     02 FILLER                   PIC X(17)  VALUE                 RL2124.2
013200            "       COMPUTED=".                                   RL2124.2
013300     02 COMPUTED-X.                                               RL2124.2
013400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2124.2
013500     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2124.2
013600                                 PIC -9(9).9(9).                  RL2124.2
013700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2124.2
013800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2124.2
013900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2124.2
014000     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2124.2
014100         04 COMPUTED-18V0                    PIC -9(18).          RL2124.2
014200         04 FILLER                           PIC X.               RL2124.2
014300     03 FILLER PIC X(50) VALUE SPACE.                             RL2124.2
014400 01  TEST-CORRECT.                                                RL2124.2
014500     02 FILLER PIC X(30) VALUE SPACE.                             RL2124.2
014600     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2124.2
014700     02 CORRECT-X.                                                RL2124.2
014800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2124.2
014900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2124.2
015000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2124.2
015100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2124.2
015200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2124.2
015300     03      CR-18V0 REDEFINES CORRECT-A.                         RL2124.2
015400         04 CORRECT-18V0                     PIC -9(18).          RL2124.2
015500         04 FILLER                           PIC X.               RL2124.2
015600     03 FILLER PIC X(2) VALUE SPACE.                              RL2124.2
015700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2124.2
015800 01  CCVS-C-1.                                                    RL2124.2
015900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2124.2
016000-    "SS  PARAGRAPH-NAME                                          RL2124.2
016100-    "       REMARKS".                                            RL2124.2
016200     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2124.2
016300 01  CCVS-C-2.                                                    RL2124.2
016400     02 FILLER                     PIC X        VALUE SPACE.      RL2124.2
016500     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2124.2
016600     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2124.2
016700     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2124.2
016800     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2124.2
016900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2124.2
017000 01  REC-CT                        PIC 99       VALUE ZERO.       RL2124.2
017100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2124.2
017200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2124.2
017300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2124.2
017400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2124.2
017500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2124.2
017600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2124.2
017700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2124.2
017800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2124.2
017900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2124.2
018000 01  CCVS-H-1.                                                    RL2124.2
018100     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2124.2
018200     02  FILLER                    PIC X(42)    VALUE             RL2124.2
018300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2124.2
018400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2124.2
018500 01  CCVS-H-2A.                                                   RL2124.2
018600   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2124.2
018700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2124.2
018800   02  FILLER                        PIC XXXX   VALUE             RL2124.2
018900     "4.2 ".                                                      RL2124.2
019000   02  FILLER                        PIC X(28)  VALUE             RL2124.2
019100            " COPY - NOT FOR DISTRIBUTION".                       RL2124.2
019200   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2124.2
019300                                                                  RL2124.2
019400 01  CCVS-H-2B.                                                   RL2124.2
019500   02  FILLER                        PIC X(15)  VALUE             RL2124.2
019600            "TEST RESULT OF ".                                    RL2124.2
019700   02  TEST-ID                       PIC X(9).                    RL2124.2
019800   02  FILLER                        PIC X(4)   VALUE             RL2124.2
019900            " IN ".                                               RL2124.2
020000   02  FILLER                        PIC X(12)  VALUE             RL2124.2
020100     " HIGH       ".                                              RL2124.2
020200   02  FILLER                        PIC X(22)  VALUE             RL2124.2
020300            " LEVEL VALIDATION FOR ".                             RL2124.2
020400   02  FILLER                        PIC X(58)  VALUE             RL2124.2
020500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2124.2
020600 01  CCVS-H-3.                                                    RL2124.2
020700     02  FILLER                      PIC X(34)  VALUE             RL2124.2
020800            " FOR OFFICIAL USE ONLY    ".                         RL2124.2
020900     02  FILLER                      PIC X(58)  VALUE             RL2124.2
021000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2124.2
021100     02  FILLER                      PIC X(28)  VALUE             RL2124.2
021200            "  COPYRIGHT   1985 ".                                RL2124.2
021300 01  CCVS-E-1.                                                    RL2124.2
021400     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2124.2
021500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2124.2
021600     02 ID-AGAIN                     PIC X(9).                    RL2124.2
021700     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2124.2
021800 01  CCVS-E-2.                                                    RL2124.2
021900     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2124.2
022000     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2124.2
022100     02 CCVS-E-2-2.                                               RL2124.2
022200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2124.2
022300         03 FILLER                   PIC X      VALUE SPACE.      RL2124.2
022400         03 ENDER-DESC               PIC X(44)  VALUE             RL2124.2
022500            "ERRORS ENCOUNTERED".                                 RL2124.2
022600 01  CCVS-E-3.                                                    RL2124.2
022700     02  FILLER                      PIC X(22)  VALUE             RL2124.2
022800            " FOR OFFICIAL USE ONLY".                             RL2124.2
022900     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2124.2
023000     02  FILLER                      PIC X(58)  VALUE             RL2124.2
023100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2124.2
023200     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2124.2
023300     02 FILLER                       PIC X(15)  VALUE             RL2124.2
023400             " COPYRIGHT 1985".                                   RL2124.2
023500 01  CCVS-E-4.                                                    RL2124.2
023600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2124.2
023700     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2124.2
023800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2124.2
023900     02 FILLER                       PIC X(40)  VALUE             RL2124.2
024000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2124.2
024100 01  XXINFO.                                                      RL2124.2
024200     02 FILLER                       PIC X(19)  VALUE             RL2124.2
024300            "*** INFORMATION ***".                                RL2124.2
024400     02 INFO-TEXT.                                                RL2124.2
024500       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2124.2
024600       04 XXCOMPUTED                 PIC X(20).                   RL2124.2
024700       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2124.2
024800       04 XXCORRECT                  PIC X(20).                   RL2124.2
024900     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2124.2
025000 01  HYPHEN-LINE.                                                 RL2124.2
025100     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2124.2
025200     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2124.2
025300-    "*****************************************".                 RL2124.2
025400     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2124.2
025500-    "******************************".                            RL2124.2
025600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2124.2
025700     "RL212A".                                                    RL2124.2
025800 PROCEDURE DIVISION.                                              RL2124.2
025900 CCVS1 SECTION.                                                   RL2124.2
026000 OPEN-FILES.                                                      RL2124.2
026100     OPEN    OUTPUT PRINT-FILE.                                   RL2124.2
026200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2124.2
026300     MOVE    SPACE TO TEST-RESULTS.                               RL2124.2
026400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2124.2
026500     MOVE    ZERO TO REC-SKL-SUB.                                 RL2124.2
026600     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2124.2
026700 CCVS-INIT-FILE.                                                  RL2124.2
026800     ADD     1 TO REC-SKL-SUB.                                    RL2124.2
026900     MOVE    FILE-RECORD-INFO-SKELETON                            RL2124.2
027000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2124.2
027100 CCVS-INIT-EXIT.                                                  RL2124.2
027200     GO TO CCVS1-EXIT.                                            RL2124.2
027300 CLOSE-FILES.                                                     RL2124.2
027400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2124.2
027500 TERMINATE-CCVS.                                                  RL2124.2
027600*    EXIT PROGRAM.                                                RL2124.2
027700*TERMINATE-CALL.                                                  RL2124.2
027800     STOP     RUN.                                                RL2124.2
027900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2124.2
028000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2124.2
028100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2124.2
028200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2124.2
028300     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2124.2
028400 PRINT-DETAIL.                                                    RL2124.2
028500     IF REC-CT NOT EQUAL TO ZERO                                  RL2124.2
028600             MOVE "." TO PARDOT-X                                 RL2124.2
028700             MOVE REC-CT TO DOTVALUE.                             RL2124.2
028800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2124.2
028900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2124.2
029000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2124.2
029100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2124.2
029200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2124.2
029300     MOVE SPACE TO CORRECT-X.                                     RL2124.2
029400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2124.2
029500     MOVE     SPACE TO RE-MARK.                                   RL2124.2
029600 HEAD-ROUTINE.                                                    RL2124.2
029700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2124.2
029800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2124.2
029900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2124.2
030000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2124.2
030100 COLUMN-NAMES-ROUTINE.                                            RL2124.2
030200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2124.2
030300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2124.2
030400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2124.2
030500 END-ROUTINE.                                                     RL2124.2
030600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2124.2
030700 END-RTN-EXIT.                                                    RL2124.2
030800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2124.2
030900 END-ROUTINE-1.                                                   RL2124.2
031000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2124.2
031100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2124.2
031200      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2124.2
031300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2124.2
031400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2124.2
031500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2124.2
031600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2124.2
031700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2124.2
031800  END-ROUTINE-12.                                                 RL2124.2
031900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2124.2
032000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2124.2
032100         MOVE "NO " TO ERROR-TOTAL                                RL2124.2
032200         ELSE                                                     RL2124.2
032300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2124.2
032400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2124.2
032500     PERFORM WRITE-LINE.                                          RL2124.2
032600 END-ROUTINE-13.                                                  RL2124.2
032700     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2124.2
032800         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2124.2
032900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2124.2
033000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2124.2
033100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2124.2
033200      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2124.2
033300          MOVE "NO " TO ERROR-TOTAL                               RL2124.2
033400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2124.2
033500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2124.2
033600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2124.2
033700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2124.2
033800 WRITE-LINE.                                                      RL2124.2
033900     ADD 1 TO RECORD-COUNT.                                       RL2124.2
034000*    IF RECORD-COUNT GREATER 50                                   RL2124.2
034100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2124.2
034200*        MOVE SPACE TO DUMMY-RECORD                               RL2124.2
034300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2124.2
034400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2124.2
034500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2124.2
034600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2124.2
034700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2124.2
034800*        MOVE ZERO TO RECORD-COUNT.                               RL2124.2
034900     PERFORM WRT-LN.                                              RL2124.2
035000 WRT-LN.                                                          RL2124.2
035100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2124.2
035200     MOVE SPACE TO DUMMY-RECORD.                                  RL2124.2
035300 BLANK-LINE-PRINT.                                                RL2124.2
035400     PERFORM WRT-LN.                                              RL2124.2
035500 FAIL-ROUTINE.                                                    RL2124.2
035600     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2124.2
035700            GO TO   FAIL-ROUTINE-WRITE.                           RL2124.2
035800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2124.2
035900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2124.2
036000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2124.2
036100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2124.2
036200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2124.2
036300     GO TO  FAIL-ROUTINE-EX.                                      RL2124.2
036400 FAIL-ROUTINE-WRITE.                                              RL2124.2
036500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2124.2
036600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2124.2
036700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2124.2
036800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2124.2
036900 FAIL-ROUTINE-EX. EXIT.                                           RL2124.2
037000 BAIL-OUT.                                                        RL2124.2
037100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2124.2
037200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2124.2
037300 BAIL-OUT-WRITE.                                                  RL2124.2
037400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2124.2
037500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2124.2
037600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2124.2
037700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2124.2
037800 BAIL-OUT-EX. EXIT.                                               RL2124.2
037900 CCVS1-EXIT.                                                      RL2124.2
038000     EXIT.                                                        RL2124.2
038100 SECT-RL212A-001 SECTION.                                         RL2124.2
038200 REL-INIT-001.                                                    RL2124.2
038300     MOVE     "FILE CREATE RL-FS1" TO FEATURE.                    RL2124.2
038400     OPEN     OUTPUT    RL-FS1.                                   RL2124.2
038500     MOVE     "RL-FS1" TO XFILE-NAME (1).                         RL2124.2
038600     MOVE     "R1-F-G" TO XRECORD-NAME (1).                       RL2124.2
038700     MOVE CCVS-PGM-ID      TO XPROGRAM-NAME (1).                  RL2124.2
038800     MOVE     000120   TO XRECORD-LENGTH (1).                     RL2124.2
038900     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   RL2124.2
039000     MOVE     0001     TO XBLOCK-SIZE (1).                        RL2124.2
039100     MOVE     000500   TO RECORDS-IN-FILE (1).                    RL2124.2
039200     MOVE     "RL"     TO XFILE-ORGANIZATION (1).                 RL2124.2
039300     MOVE     "S"      TO XLABEL-TYPE (1).                        RL2124.2
039400     MOVE     000001   TO XRECORD-NUMBER (1).                     RL2124.2
039500 REL-TEST-001.                                                    RL2124.2
039600     MOVE     FILE-RECORD-INFO-P1-120 (1) TO RL-FS1R1-F-G-120.    RL2124.2
039700     WRITE    RL-FS1R1-F-G-120                                    RL2124.2
039800              INVALID KEY GO TO REL-FAIL-001.                     RL2124.2
039900     IF      XRECORD-NUMBER (1) EQUAL TO 500                      RL2124.2
040000             GO TO REL-WRITE-001.                                 RL2124.2
040100     ADD      000001 TO XRECORD-NUMBER (1).                       RL2124.2
040200     GO       TO REL-TEST-001.                                    RL2124.2
040300 REL-DELETE-001.                                                  RL2124.2
040400     PERFORM   DE-LETE.                                           RL2124.2
040500     GO TO REL-WRITE-001.                                         RL2124.2
040600 REL-FAIL-001.                                                    RL2124.2
040700     PERFORM   FAIL.                                              RL2124.2
040800     MOVE    "BOUNDARY VIOLATION"  TO RE-MARK.                    RL2124.2
040900 REL-WRITE-001.                                                   RL2124.2
041000     MOVE     "REL-TEST-001" TO   PAR-NAME                        RL2124.2
041100     MOVE     "FILE CREATED, LFILE "  TO COMPUTED-A.              RL2124.2
041200     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2124.2
041300     PERFORM  PRINT-DETAIL.                                       RL2124.2
041400     CLOSE    RL-FS1.                                             RL2124.2
041500 REL-INIT-002.                                                    RL2124.2
041600     OPEN     INPUT     RL-FS1.                                   RL2124.2
041700     MOVE     ZERO      TO WRK-CS-09V00.                          RL2124.2
041800 REL-TEST-002.                                                    RL2124.2
041900     READ     RL-FS1                                              RL2124.2
042000              AT END GO TO REL-TEST-002-1.                        RL2124.2
042100     MOVE     RL-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).    RL2124.2
042200     ADD      1 TO WRK-CS-09V00.                                  RL2124.2
042300     IF       WRK-CS-09V00 GREATER 500                            RL2124.2
042400             MOVE "MORE THAN 500 RECORDS" TO RE-MARK              RL2124.2
042500              GO TO REL-TEST-002-1.                               RL2124.2
042600     GO       TO REL-TEST-002.                                    RL2124.2
042700 REL-DELETE-002.                                                  RL2124.2
042800     PERFORM     DE-LETE.                                         RL2124.2
042900     PERFORM    PRINT-DETAIL.                                     RL2124.2
043000 REL-TEST-002-1.                                                  RL2124.2
043100     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 RL2124.2
043200              PERFORM FAIL                                        RL2124.2
043300              ELSE                                                RL2124.2
043400              PERFORM PASS.                                       RL2124.2
043500     GO       TO REL-WRITE-002.                                   RL2124.2
043600 REL-WRITE-002.                                                   RL2124.2
043700     MOVE     "REL-TEST-002" TO PAR-NAME.                         RL2124.2
043800     MOVE     "FILE VERIFIED, LFILE" TO COMPUTED-A.               RL2124.2
043900     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2124.2
044000     PERFORM  PRINT-DETAIL.                                       RL2124.2
044100     CLOSE   RL-FS1.                                              RL2124.2
044200 CCVS-EXIT SECTION.                                               RL2124.2
044300 CCVS-999999.                                                     RL2124.2
044400     GO TO CLOSE-FILES.                                           RL2124.2
      *END-OF,RL212A                                                                  
