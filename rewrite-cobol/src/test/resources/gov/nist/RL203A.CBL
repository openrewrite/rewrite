      *HEADER,COBOL,RL201A,SUBPRG,RL203A                                              
000100 IDENTIFICATION DIVISION.                                         RL2034.2
000200 PROGRAM-ID.                                                      RL2034.2
000300     RL203A.                                                      RL2034.2
000400****************************************************************  RL2034.2
000500*                                                              *  RL2034.2
000600*    VALIDATION FOR:-                                          *  RL2034.2
000700*                                                              *  RL2034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2034.2
000900*                                                              *  RL2034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2034.2
001100*                                                              *  RL2034.2
001200****************************************************************  RL2034.2
001300*GENERAL:    THIS PROGRAM IS THE THIRD OF A SERIES.  THE FUNCTION RL2034.2
001400*            OF THIS PROGRAM IS TO PROCESS THE FILE SEQUENTIALLY  RL2034.2
001500*            (ACCESS MODE IS DYNAMIC).    THE FILE USED IS THAT   RL2034.2
001600*            RESULTING FROM RL102.                                RL2034.2
001700*                                                                 RL2034.2
001800*            FIRST, THE FILE IS VERIFIED FOR ACCURACY OF ITS 500  RL2034.2
001900*            RECORDS.  SECONDLY, RECORDS OF THER FILE ARE         RL2034.2
002000*            SELECTIVELY DELETED AND THIRDLY THE ACCURACY OF EACH RL2034.2
002100*            RECORD IN THE FILE IS AGAIN VERIFIED.                RL2034.2
002200*                                                                 RL2034.2
002300*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL2034.2
002400*            PROGRAM ARE:                                         RL2034.2
002500*                                                                 RL2034.2
002600*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL2034.2
002700*                         RELATIVE  I-O DATA FILE                 RL2034.2
002800*                 X-55   SYSTEM PRINTER                           RL2034.2
002900*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL2034.2
003000*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL2034.2
003100*                 X-75   OBJECT OF VALUE OF CLAUSE                RL2034.2
003200*                 X-82   SOURCE-COMPUTER                          RL2034.2
003300*                 X-83   OBJECT-COMPUTER.                         RL2034.2
003400*                                                                 RL2034.2
003500***************************************************               RL2034.2
003600 ENVIRONMENT DIVISION.                                            RL2034.2
003700 CONFIGURATION SECTION.                                           RL2034.2
003800 SOURCE-COMPUTER.                                                 RL2034.2
003900     XXXXX082.                                                    RL2034.2
004000 OBJECT-COMPUTER.                                                 RL2034.2
004100     XXXXX083.                                                    RL2034.2
004200 INPUT-OUTPUT SECTION.                                            RL2034.2
004300 FILE-CONTROL.                                                    RL2034.2
004400     SELECT PRINT-FILE ASSIGN TO                                  RL2034.2
004500     XXXXX055.                                                    RL2034.2
004600     SELECT   RL-FD1 ASSIGN TO                                    RL2034.2
004700     XXXXD021                                                     RL2034.2
004800       ACCESS MODE IS DYNAMIC                                     RL2034.2
004900             RELATIVE KEY IS RL-FD1-KEY                           RL2034.2
005000       ORGANIZATION IS RELATIVE.                                  RL2034.2
005100 DATA DIVISION.                                                   RL2034.2
005200 FILE SECTION.                                                    RL2034.2
005300 FD  PRINT-FILE.                                                  RL2034.2
005400 01  PRINT-REC PICTURE X(120).                                    RL2034.2
005500 01  DUMMY-RECORD PICTURE X(120).                                 RL2034.2
005600 FD  RL-FD1                                                       RL2034.2
005700     LABEL RECORDS STANDARD                                       RL2034.2
005800C    VALUE OF                                                     RL2034.2
005900C    XXXXX074                                                     RL2034.2
006000C    IS                                                           RL2034.2
006100C    XXXXX075                                                     RL2034.2
006200*    XXXXX069                                                     RL2034.2
006300     BLOCK CONTAINS 01 RECORDS                                    RL2034.2
006400     RECORD CONTAINS 120.                                         RL2034.2
006500 01  RL-FD1R1-F-G-120.                                            RL2034.2
006600     02 RL-WRK-120 PIC X(120).                                    RL2034.2
006700 WORKING-STORAGE SECTION.                                         RL2034.2
006800 01  RL-FD1-KEY        PIC 9(08)  USAGE COMP VALUE ZERO.          RL2034.2
006900 01  WRK-CS-09V00-006 PIC S9(09) USAGE COMP VALUE ZERO.           RL2034.2
007000 01  WRK-CS-09V00-007 PIC S9(09) USAGE COMP VALUE ZERO.           RL2034.2
007100 01  WRK-CS-09V00-008 PIC S9(09) USAGE COMP VALUE ZERO.           RL2034.2
007200 01  WRK-CS-09V00-009 PIC S9(09) USAGE COMP VALUE ZERO.           RL2034.2
007300 01  WRK-CS-09V00-010 PIC S9(09) USAGE COMP VALUE ZERO.           RL2034.2
007400 01  WRK-CS-09V00-011 PIC S9(09) USAGE COMP VALUE ZERO.           RL2034.2
007500 01  I-O-ERROR-RL-FD1 PIC X(3) VALUE "NO ".                       RL2034.2
007600 01  FILE-RECORD-INFORMATION-REC.                                 RL2034.2
007700     03 FILE-RECORD-INFO-SKELETON.                                RL2034.2
007800        05 FILLER                 PICTURE X(48)       VALUE       RL2034.2
007900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2034.2
008000        05 FILLER                 PICTURE X(46)       VALUE       RL2034.2
008100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2034.2
008200        05 FILLER                 PICTURE X(26)       VALUE       RL2034.2
008300             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2034.2
008400        05 FILLER                 PICTURE X(37)       VALUE       RL2034.2
008500             ",RECKEY=                             ".             RL2034.2
008600        05 FILLER                 PICTURE X(38)       VALUE       RL2034.2
008700             ",ALTKEY1=                             ".            RL2034.2
008800        05 FILLER                 PICTURE X(38)       VALUE       RL2034.2
008900             ",ALTKEY2=                             ".            RL2034.2
009000        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2034.2
009100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2034.2
009200        05 FILE-RECORD-INFO-P1-120.                               RL2034.2
009300           07 FILLER              PIC X(5).                       RL2034.2
009400           07 XFILE-NAME           PIC X(6).                      RL2034.2
009500           07 FILLER              PIC X(8).                       RL2034.2
009600           07 XRECORD-NAME         PIC X(6).                      RL2034.2
009700           07 FILLER              PIC X(1).                       RL2034.2
009800           07 REELUNIT-NUMBER     PIC 9(1).                       RL2034.2
009900           07 FILLER              PIC X(7).                       RL2034.2
010000           07 XRECORD-NUMBER       PIC 9(6).                      RL2034.2
010100           07 FILLER              PIC X(6).                       RL2034.2
010200           07 UPDATE-NUMBER       PIC 9(2).                       RL2034.2
010300           07 FILLER              PIC X(5).                       RL2034.2
010400           07 ODO-NUMBER          PIC 9(4).                       RL2034.2
010500           07 FILLER              PIC X(5).                       RL2034.2
010600           07 XPROGRAM-NAME        PIC X(5).                      RL2034.2
010700           07 FILLER              PIC X(7).                       RL2034.2
010800           07 XRECORD-LENGTH       PIC 9(6).                      RL2034.2
010900           07 FILLER              PIC X(7).                       RL2034.2
011000           07 CHARS-OR-RECORDS    PIC X(2).                       RL2034.2
011100           07 FILLER              PIC X(1).                       RL2034.2
011200           07 XBLOCK-SIZE          PIC 9(4).                      RL2034.2
011300           07 FILLER              PIC X(6).                       RL2034.2
011400           07 RECORDS-IN-FILE     PIC 9(6).                       RL2034.2
011500           07 FILLER              PIC X(5).                       RL2034.2
011600           07 XFILE-ORGANIZATION   PIC X(2).                      RL2034.2
011700           07 FILLER              PIC X(6).                       RL2034.2
011800           07 XLABEL-TYPE          PIC X(1).                      RL2034.2
011900        05 FILE-RECORD-INFO-P121-240.                             RL2034.2
012000           07 FILLER              PIC X(8).                       RL2034.2
012100           07 XRECORD-KEY          PIC X(29).                     RL2034.2
012200           07 FILLER              PIC X(9).                       RL2034.2
012300           07 ALTERNATE-KEY1      PIC X(29).                      RL2034.2
012400           07 FILLER              PIC X(9).                       RL2034.2
012500           07 ALTERNATE-KEY2      PIC X(29).                      RL2034.2
012600           07 FILLER              PIC X(7).                       RL2034.2
012700 01  TEST-RESULTS.                                                RL2034.2
012800     02 FILLER                   PIC X      VALUE SPACE.          RL2034.2
012900     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2034.2
013000     02 FILLER                   PIC X      VALUE SPACE.          RL2034.2
013100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2034.2
013200     02 FILLER                   PIC X      VALUE SPACE.          RL2034.2
013300     02  PAR-NAME.                                                RL2034.2
013400       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2034.2
013500       03  PARDOT-X              PIC X      VALUE SPACE.          RL2034.2
013600       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2034.2
013700     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2034.2
013800     02 RE-MARK                  PIC X(61).                       RL2034.2
013900 01  TEST-COMPUTED.                                               RL2034.2
014000     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2034.2
014100     02 FILLER                   PIC X(17)  VALUE                 RL2034.2
014200            "       COMPUTED=".                                   RL2034.2
014300     02 COMPUTED-X.                                               RL2034.2
014400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2034.2
014500     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2034.2
014600                                 PIC -9(9).9(9).                  RL2034.2
014700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2034.2
014800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2034.2
014900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2034.2
015000     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2034.2
015100         04 COMPUTED-18V0                    PIC -9(18).          RL2034.2
015200         04 FILLER                           PIC X.               RL2034.2
015300     03 FILLER PIC X(50) VALUE SPACE.                             RL2034.2
015400 01  TEST-CORRECT.                                                RL2034.2
015500     02 FILLER PIC X(30) VALUE SPACE.                             RL2034.2
015600     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2034.2
015700     02 CORRECT-X.                                                RL2034.2
015800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2034.2
015900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2034.2
016000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2034.2
016100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2034.2
016200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2034.2
016300     03      CR-18V0 REDEFINES CORRECT-A.                         RL2034.2
016400         04 CORRECT-18V0                     PIC -9(18).          RL2034.2
016500         04 FILLER                           PIC X.               RL2034.2
016600     03 FILLER PIC X(2) VALUE SPACE.                              RL2034.2
016700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2034.2
016800 01  CCVS-C-1.                                                    RL2034.2
016900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2034.2
017000-    "SS  PARAGRAPH-NAME                                          RL2034.2
017100-    "       REMARKS".                                            RL2034.2
017200     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2034.2
017300 01  CCVS-C-2.                                                    RL2034.2
017400     02 FILLER                     PIC X        VALUE SPACE.      RL2034.2
017500     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2034.2
017600     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2034.2
017700     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2034.2
017800     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2034.2
017900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2034.2
018000 01  REC-CT                        PIC 99       VALUE ZERO.       RL2034.2
018100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2034.2
018200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2034.2
018300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2034.2
018400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2034.2
018500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2034.2
018600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2034.2
018700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2034.2
018800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2034.2
018900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2034.2
019000 01  CCVS-H-1.                                                    RL2034.2
019100     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2034.2
019200     02  FILLER                    PIC X(42)    VALUE             RL2034.2
019300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2034.2
019400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2034.2
019500 01  CCVS-H-2A.                                                   RL2034.2
019600   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2034.2
019700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2034.2
019800   02  FILLER                        PIC XXXX   VALUE             RL2034.2
019900     "4.2 ".                                                      RL2034.2
020000   02  FILLER                        PIC X(28)  VALUE             RL2034.2
020100            " COPY - NOT FOR DISTRIBUTION".                       RL2034.2
020200   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2034.2
020300                                                                  RL2034.2
020400 01  CCVS-H-2B.                                                   RL2034.2
020500   02  FILLER                        PIC X(15)  VALUE             RL2034.2
020600            "TEST RESULT OF ".                                    RL2034.2
020700   02  TEST-ID                       PIC X(9).                    RL2034.2
020800   02  FILLER                        PIC X(4)   VALUE             RL2034.2
020900            " IN ".                                               RL2034.2
021000   02  FILLER                        PIC X(12)  VALUE             RL2034.2
021100     " HIGH       ".                                              RL2034.2
021200   02  FILLER                        PIC X(22)  VALUE             RL2034.2
021300            " LEVEL VALIDATION FOR ".                             RL2034.2
021400   02  FILLER                        PIC X(58)  VALUE             RL2034.2
021500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2034.2
021600 01  CCVS-H-3.                                                    RL2034.2
021700     02  FILLER                      PIC X(34)  VALUE             RL2034.2
021800            " FOR OFFICIAL USE ONLY    ".                         RL2034.2
021900     02  FILLER                      PIC X(58)  VALUE             RL2034.2
022000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2034.2
022100     02  FILLER                      PIC X(28)  VALUE             RL2034.2
022200            "  COPYRIGHT   1985 ".                                RL2034.2
022300 01  CCVS-E-1.                                                    RL2034.2
022400     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2034.2
022500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2034.2
022600     02 ID-AGAIN                     PIC X(9).                    RL2034.2
022700     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2034.2
022800 01  CCVS-E-2.                                                    RL2034.2
022900     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2034.2
023000     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2034.2
023100     02 CCVS-E-2-2.                                               RL2034.2
023200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2034.2
023300         03 FILLER                   PIC X      VALUE SPACE.      RL2034.2
023400         03 ENDER-DESC               PIC X(44)  VALUE             RL2034.2
023500            "ERRORS ENCOUNTERED".                                 RL2034.2
023600 01  CCVS-E-3.                                                    RL2034.2
023700     02  FILLER                      PIC X(22)  VALUE             RL2034.2
023800            " FOR OFFICIAL USE ONLY".                             RL2034.2
023900     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2034.2
024000     02  FILLER                      PIC X(58)  VALUE             RL2034.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2034.2
024200     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2034.2
024300     02 FILLER                       PIC X(15)  VALUE             RL2034.2
024400             " COPYRIGHT 1985".                                   RL2034.2
024500 01  CCVS-E-4.                                                    RL2034.2
024600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2034.2
024700     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2034.2
024800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2034.2
024900     02 FILLER                       PIC X(40)  VALUE             RL2034.2
025000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2034.2
025100 01  XXINFO.                                                      RL2034.2
025200     02 FILLER                       PIC X(19)  VALUE             RL2034.2
025300            "*** INFORMATION ***".                                RL2034.2
025400     02 INFO-TEXT.                                                RL2034.2
025500       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2034.2
025600       04 XXCOMPUTED                 PIC X(20).                   RL2034.2
025700       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2034.2
025800       04 XXCORRECT                  PIC X(20).                   RL2034.2
025900     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2034.2
026000 01  HYPHEN-LINE.                                                 RL2034.2
026100     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2034.2
026200     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2034.2
026300-    "*****************************************".                 RL2034.2
026400     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2034.2
026500-    "******************************".                            RL2034.2
026600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2034.2
026700     "RL203A".                                                    RL2034.2
026800 PROCEDURE DIVISION.                                              RL2034.2
026900 CCVS1 SECTION.                                                   RL2034.2
027000 OPEN-FILES.                                                      RL2034.2
027100     OPEN    OUTPUT PRINT-FILE.                                   RL2034.2
027200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2034.2
027300     MOVE    SPACE TO TEST-RESULTS.                               RL2034.2
027400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2034.2
027500     MOVE    ZERO TO REC-SKL-SUB.                                 RL2034.2
027600     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2034.2
027700 CCVS-INIT-FILE.                                                  RL2034.2
027800     ADD     1 TO REC-SKL-SUB.                                    RL2034.2
027900     MOVE    FILE-RECORD-INFO-SKELETON                            RL2034.2
028000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2034.2
028100 CCVS-INIT-EXIT.                                                  RL2034.2
028200     GO TO CCVS1-EXIT.                                            RL2034.2
028300 CLOSE-FILES.                                                     RL2034.2
028400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2034.2
028500 TERMINATE-CCVS.                                                  RL2034.2
028600*    EXIT PROGRAM.                                                RL2034.2
028700*TERMINATE-CALL.                                                  RL2034.2
028800     STOP     RUN.                                                RL2034.2
028900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2034.2
029000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2034.2
029100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2034.2
029200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2034.2
029300     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2034.2
029400 PRINT-DETAIL.                                                    RL2034.2
029500     IF REC-CT NOT EQUAL TO ZERO                                  RL2034.2
029600             MOVE "." TO PARDOT-X                                 RL2034.2
029700             MOVE REC-CT TO DOTVALUE.                             RL2034.2
029800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2034.2
029900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2034.2
030000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2034.2
030100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2034.2
030200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2034.2
030300     MOVE SPACE TO CORRECT-X.                                     RL2034.2
030400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2034.2
030500     MOVE     SPACE TO RE-MARK.                                   RL2034.2
030600 HEAD-ROUTINE.                                                    RL2034.2
030700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2034.2
030800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2034.2
030900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2034.2
031000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2034.2
031100 COLUMN-NAMES-ROUTINE.                                            RL2034.2
031200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2034.2
031300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2034.2
031400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2034.2
031500 END-ROUTINE.                                                     RL2034.2
031600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2034.2
031700 END-RTN-EXIT.                                                    RL2034.2
031800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2034.2
031900 END-ROUTINE-1.                                                   RL2034.2
032000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2034.2
032100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2034.2
032200      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2034.2
032300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2034.2
032400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2034.2
032500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2034.2
032600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2034.2
032700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2034.2
032800  END-ROUTINE-12.                                                 RL2034.2
032900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2034.2
033000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2034.2
033100         MOVE "NO " TO ERROR-TOTAL                                RL2034.2
033200         ELSE                                                     RL2034.2
033300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2034.2
033400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2034.2
033500     PERFORM WRITE-LINE.                                          RL2034.2
033600 END-ROUTINE-13.                                                  RL2034.2
033700     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2034.2
033800         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2034.2
033900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2034.2
034000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2034.2
034100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2034.2
034200      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2034.2
034300          MOVE "NO " TO ERROR-TOTAL                               RL2034.2
034400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2034.2
034500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2034.2
034600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2034.2
034700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2034.2
034800 WRITE-LINE.                                                      RL2034.2
034900     ADD 1 TO RECORD-COUNT.                                       RL2034.2
035000*    IF RECORD-COUNT GREATER 50                                   RL2034.2
035100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2034.2
035200*        MOVE SPACE TO DUMMY-RECORD                               RL2034.2
035300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2034.2
035400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2034.2
035500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2034.2
035600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2034.2
035700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2034.2
035800*        MOVE ZERO TO RECORD-COUNT.                               RL2034.2
035900     PERFORM WRT-LN.                                              RL2034.2
036000 WRT-LN.                                                          RL2034.2
036100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2034.2
036200     MOVE SPACE TO DUMMY-RECORD.                                  RL2034.2
036300 BLANK-LINE-PRINT.                                                RL2034.2
036400     PERFORM WRT-LN.                                              RL2034.2
036500 FAIL-ROUTINE.                                                    RL2034.2
036600     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2034.2
036700            GO TO   FAIL-ROUTINE-WRITE.                           RL2034.2
036800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2034.2
036900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2034.2
037000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2034.2
037100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2034.2
037200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2034.2
037300     GO TO  FAIL-ROUTINE-EX.                                      RL2034.2
037400 FAIL-ROUTINE-WRITE.                                              RL2034.2
037500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2034.2
037600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2034.2
037700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2034.2
037800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2034.2
037900 FAIL-ROUTINE-EX. EXIT.                                           RL2034.2
038000 BAIL-OUT.                                                        RL2034.2
038100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2034.2
038200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2034.2
038300 BAIL-OUT-WRITE.                                                  RL2034.2
038400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2034.2
038500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2034.2
038600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2034.2
038700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2034.2
038800 BAIL-OUT-EX. EXIT.                                               RL2034.2
038900 CCVS1-EXIT.                                                      RL2034.2
039000     EXIT.                                                        RL2034.2
039100 SECT-RL-03-001 SECTION.                                          RL2034.2
039200 REL-INIT-006.                                                    RL2034.2
039300     MOVE  99 TO RL-FD1-KEY.                                      RL2034.2
039400*    CONTAIN THE NUMBER OF THE RECORD PREVIOUSLY READ.            RL2034.2
039500     OPEN INPUT RL-FD1.                                           RL2034.2
039600     MOVE     "REL-TEST-006" TO   PAR-NAME.                       RL2034.2
039700     MOVE     ZERO TO             WRK-CS-09V00-006.               RL2034.2
039800     MOVE     ZERO TO             WRK-CS-09V00-007.               RL2034.2
039900     MOVE     ZERO TO             WRK-CS-09V00-008.               RL2034.2
040000     MOVE     ZERO TO             WRK-CS-09V00-009.               RL2034.2
040100     MOVE     ZERO TO             WRK-CS-09V00-010.               RL2034.2
040200     MOVE     ZERO TO             WRK-CS-09V00-011.               RL2034.2
040300     MOVE     SPACE TO  FILE-RECORD-INFO-P1-120 (1).              RL2034.2
040400     MOVE    RL-FD1-KEY TO WRK-CS-09V00-011.                      RL2034.2
040500     MOVE     01 TO REC-CT.                                       RL2034.2
040600     MOVE    "READ SEQUENTIAL"  TO FEATURE.                       RL2034.2
040700 REL-TEST-006-R.                                                  RL2034.2
040800     ADD      1 TO WRK-CS-09V00-006.                              RL2034.2
040900     READ     RL-FD1  NEXT RECORD                                 RL2034.2
041000              AT END GO TO REL-TEST-006-3.                        RL2034.2
041100     MOVE     RL-FD1R1-F-G-120    TO FILE-RECORD-INFO-P1-120 (1). RL2034.2
041200     IF       UPDATE-NUMBER (1) EQUAL TO 00                       RL2034.2
041300             ADD 1 TO WRK-CS-09V00-007                            RL2034.2
041400              GO TO REL-TEST-006-2.                               RL2034.2
041500     IF       UPDATE-NUMBER (1) EQUAL TO 01                       RL2034.2
041600              ADD 1 TO WRK-CS-09V00-008                           RL2034.2
041700              GO TO REL-TEST-006-2.                               RL2034.2
041800     ADD      1 TO WRK-CS-09V00-009.                              RL2034.2
041900 REL-TEST-006-2.                                                  RL2034.2
042000     IF       RL-FD1-KEY NOT EQUAL TO XRECORD-NUMBER (1)          RL2034.2
042100              ADD 1 TO  WRK-CS-09V00-010.                         RL2034.2
042200     IF       WRK-CS-09V00-006  GREATER 501                       RL2034.2
042300              GO TO REL-TEST-006-3.                               RL2034.2
042400     GO TO    REL-TEST-006-R.                                     RL2034.2
042500 REL-TEST-006-3.                                                  RL2034.2
042600     IF       WRK-CS-09V00-006 NOT EQUAL TO 501                   RL2034.2
042700              MOVE "INCORRECT RECORD COUNT"  TO RE-MARK           RL2034.2
042800              MOVE  WRK-CS-09V00-006 TO COMPUTED-18V0             RL2034.2
042900              MOVE  501  TO             CORRECT-18V0              RL2034.2
043000              PERFORM FAIL                                        RL2034.2
043100              ELSE                                                RL2034.2
043200              PERFORM PASS.                                       RL2034.2
043300     PERFORM  PRINT-DETAIL.                                       RL2034.2
043400*    .01                                                          RL2034.2
043500     ADD      1 TO REC-CT.                                        RL2034.2
043600     IF       WRK-CS-09V00-007 EQUAL TO 400                       RL2034.2
043700              PERFORM PASS                                        RL2034.2
043800              ELSE                                                RL2034.2
043900              MOVE "NON-UPDATED RECORDS" TO COMPUTED-A            RL2034.2
044000              MOVE  WRK-CS-09V00-007 TO CORRECT-18V0              RL2034.2
044100              MOVE "SHOULD BE 400" TO RE-MARK                     RL2034.2
044200              PERFORM FAIL.                                       RL2034.2
044300     PERFORM  PRINT-DETAIL.                                       RL2034.2
044400     ADD      1 TO REC-CT.                                        RL2034.2
044500*    .02                                                          RL2034.2
044600     IF      WRK-CS-09V00-008 EQUAL TO 100                        RL2034.2
044700              PERFORM PASS                                        RL2034.2
044800              ELSE                                                RL2034.2
044900             MOVE WRK-CS-09V00-008 TO COMPUTED-18V0               RL2034.2
045000             MOVE 100             TO  CORRECT-18V0                RL2034.2
045100             MOVE "UPDATED RECORDS" TO RE-MARK                    RL2034.2
045200              PERFORM FAIL.                                       RL2034.2
045300     PERFORM  PRINT-DETAIL.                                       RL2034.2
045400     ADD      1 TO REC-CT.                                        RL2034.2
045500*    .03                                                          RL2034.2
045600     IF      WRK-CS-09V00-009 EQUAL TO ZERO                       RL2034.2
045700             PERFORM PASS                                         RL2034.2
045800             ELSE                                                 RL2034.2
045900             MOVE WRK-CS-09V00-009 TO COMPUTED-18V0               RL2034.2
046000             MOVE  ZERO            TO CORRECT-18V0                RL2034.2
046100             MOVE "BAD-UPDATES" TO RE-MARK                        RL2034.2
046200             PERFORM FAIL.                                        RL2034.2
046300     PERFORM PRINT-DETAIL.                                        RL2034.2
046400     ADD     01 TO REC-CT.                                        RL2034.2
046500*    .04                                                          RL2034.2
046600     IF      WRK-CS-09V00-010 EQUAL TO ZERO                       RL2034.2
046700             PERFORM PASS                                         RL2034.2
046800             ELSE                                                 RL2034.2
046900             MOVE WRK-CS-09V00-010 TO COMPUTED-18V0               RL2034.2
047000             MOVE ZERO             TO CORRECT-18V0                RL2034.2
047100             MOVE "KEY VS RECORD" TO RE-MARK                      RL2034.2
047200             PERFORM FAIL.                                        RL2034.2
047300     PERFORM PRINT-DETAIL.                                        RL2034.2
047400     ADD     01 TO REC-CT.                                        RL2034.2
047500*    .05                                                          RL2034.2
047600     MOVE    WRK-CS-09V00-011 TO RL-FD1-KEY.                      RL2034.2
047700     MOVE RL-FD1-KEY TO COMPUTED-18V0.                            RL2034.2
047800     MOVE    "INFORMATION" TO CORRECT-A.                          RL2034.2
047900     MOVE    "STATUS AFTER OPEN" TO RE-MARK.                      RL2034.2
048000     PERFORM PRINT-DETAIL.                                        RL2034.2
048100     ADD     01 TO REC-CT.                                        RL2034.2
048200*    .06                                                          RL2034.2
048300     CLOSE    RL-FD1.                                             RL2034.2
048400 REL-INIT-007.                                                    RL2034.2
048500     MOVE     "REL-TEST-007" TO PAR-NAME                          RL2034.2
048600     OPEN     I-O RL-FD1.                                         RL2034.2
048700     MOVE     ZERO TO WRK-CS-09V00-006                            RL2034.2
048800     MOVE     ZERO TO WRK-CS-09V00-007                            RL2034.2
048900     MOVE     ZERO TO WRK-CS-09V00-008                            RL2034.2
049000     MOVE     ZERO TO WRK-CS-09V00-009                            RL2034.2
049100     MOVE     ZERO TO WRK-CS-09V00-010                            RL2034.2
049200     MOVE     ZERO TO WRK-CS-09V00-011                            RL2034.2
049300     MOVE     01 TO REC-CT.                                       RL2034.2
049400     MOVE     SPACE TO  FILE-RECORD-INFO-P1-120 (1).              RL2034.2
049500     MOVE    "DELETE"  TO FEATURE.                                RL2034.2
049600 REL-TEST-007-R.                                                  RL2034.2
049700     ADD      1 TO WRK-CS-09V00-006                               RL2034.2
049800     ADD      1 TO WRK-CS-09V00-007.                              RL2034.2
049900     READ     RL-FD1  NEXT RECORD                                 RL2034.2
050000              AT END                                              RL2034.2
050100              MOVE "AT END PATH TAKEN " TO RE-MARK                RL2034.2
050200             GO TO  REL-TEST-007-3.                               RL2034.2
050300     MOVE     RL-FD1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).    RL2034.2
050400     IF       WRK-CS-09V00-007  EQUAL TO 4                        RL2034.2
050500              GO TO REL-TEST-007-2.                               RL2034.2
050600     IF       WRK-CS-09V00-006 GREATER 501                        RL2034.2
050700              MOVE  "AT END NOT TAKEN"  TO RE-MARK                RL2034.2
050800              GO TO REL-TEST-007-3.                               RL2034.2
050900     GO TO    REL-TEST-007-R.                                     RL2034.2
051000 REL-TEST-007-2.                                                  RL2034.2
051100     MOVE CCVS-PGM-ID       TO  XPROGRAM-NAME (1).                RL2034.2
051200     MOVE  99 TO UPDATE-NUMBER (1).                               RL2034.2
051300     MOVE     FILE-RECORD-INFO-P1-120 (1) TO RL-FD1R1-F-G-120.    RL2034.2
051400     DELETE    RL-FD1                                             RL2034.2
051500             INVALID KEY GO TO REL-TEST-007-3.                    RL2034.2
051600     MOVE     ZERO TO  WRK-CS-09V00-007.                          RL2034.2
051700     ADD      1 TO  WRK-CS-09V00-008                              RL2034.2
051800     GO TO    REL-TEST-007-R.                                     RL2034.2
051900 REL-TEST-007-3.                                                  RL2034.2
052000     IF       WRK-CS-09V00-006 NOT EQUAL TO 501                   RL2034.2
052100              MOVE WRK-CS-09V00-006 TO COMPUTED-18V0              RL2034.2
052200              MOVE              501 TO CORRECT-18V0               RL2034.2
052300              PERFORM FAIL                                        RL2034.2
052400              ELSE                                                RL2034.2
052500              PERFORM PASS.                                       RL2034.2
052600     PERFORM  PRINT-DETAIL.                                       RL2034.2
052700     ADD      01 TO REC-CT.                                       RL2034.2
052800*    .01                                                          RL2034.2
052900     IF       WRK-CS-09V00-008 NOT EQUAL TO 125                   RL2034.2
053000              MOVE WRK-CS-09V00-008 TO COMPUTED-18V0              RL2034.2
053100              MOVE 125              TO CORRECT-18V0               RL2034.2
053200              MOVE "DELETED RECORDS" TO RE-MARK                   RL2034.2
053300              PERFORM FAIL                                        RL2034.2
053400              ELSE                                                RL2034.2
053500              PERFORM PASS.                                       RL2034.2
053600     PERFORM  PRINT-DETAIL.                                       RL2034.2
053700     ADD      01 TO REC-CT.                                       RL2034.2
053800*    .02                                                          RL2034.2
053900     CLOSE    RL-FD1.                                             RL2034.2
054000 REL-INIT-008.                                                    RL2034.2
054100     MOVE     "REL-TEST-008" TO PAR-NAME.                         RL2034.2
054200     MOVE     ZERO TO   WRK-CS-09V00-006                          RL2034.2
054300     MOVE     ZERO TO   WRK-CS-09V00-007                          RL2034.2
054400     MOVE     ZERO TO   WRK-CS-09V00-008                          RL2034.2
054500     MOVE     ZERO TO   WRK-CS-09V00-009                          RL2034.2
054600     MOVE     ZERO TO   WRK-CS-09V00-010                          RL2034.2
054700     MOVE     ZERO TO   WRK-CS-09V00-011                          RL2034.2
054800     MOVE     01 TO REC-CT.                                       RL2034.2
054900     MOVE     SPACE  TO  FILE-RECORD-INFO-P1-120 (1).             RL2034.2
055000     MOVE     ZERO TO RL-FD1-KEY.                                 RL2034.2
055100     OPEN     INPUT  RL-FD1.                                      RL2034.2
055200     MOVE    "READ UPDATED FILE"  TO FEATURE.                     RL2034.2
055300 REL-TEST-008-R.                                                  RL2034.2
055400     ADD      1 TO WRK-CS-09V00-006.                              RL2034.2
055500     ADD      1 TO WRK-CS-09V00-007.                              RL2034.2
055600     ADD      1 TO WRK-CS-09V00-008.                              RL2034.2
055700     READ      RL-FD1 NEXT RECORD  AT END  GO TO REL-TEST-008-3.  RL2034.2
055800     MOVE     RL-FD1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).    RL2034.2
055900     IF       UPDATE-NUMBER (1) EQUAL TO 99                       RL2034.2
056000              ADD  1 TO WRK-CS-09V00-009.                         RL2034.2
056100     IF       WRK-CS-09V00-007  EQUAL TO 4                        RL2034.2
056200              MOVE 01 TO WRK-CS-09V00-007                         RL2034.2
056300              ADD 1 TO WRK-CS-09V00-008.                          RL2034.2
056400     IF       RL-FD1-KEY EQUAL TO  XRECORD-NUMBER (1)             RL2034.2
056500              ADD 1 TO  WRK-CS-09V00-010.                         RL2034.2
056600     IF       XRECORD-NUMBER (1) EQUAL TO  WRK-CS-09V00-008       RL2034.2
056700              ADD 1 TO  WRK-CS-09V00-011.                         RL2034.2
056800     IF       WRK-CS-09V00-006 GREATER  501                       RL2034.2
056900              GO TO REL-TEST-008-3.                               RL2034.2
057000     GO TO    REL-TEST-008-R.                                     RL2034.2
057100 REL-TEST-008-3.                                                  RL2034.2
057200     IF       WRK-CS-09V00-006 NOT EQUAL TO 376                   RL2034.2
057300              MOVE "INCORRECT RECORD COUNT"  TO RE-MARK           RL2034.2
057400              MOVE WRK-CS-09V00-006 TO COMPUTED-18V0              RL2034.2
057500              MOVE 376 TO CORRECT-18V0                            RL2034.2
057600              PERFORM  FAIL                                       RL2034.2
057700              ELSE                                                RL2034.2
057800              PERFORM  PASS.                                      RL2034.2
057900     PERFORM  PRINT-DETAIL.                                       RL2034.2
058000     ADD      01 TO REC-CT.                                       RL2034.2
058100*    .01                                                          RL2034.2
058200     IF       WRK-CS-09V00-009 NOT EQUAL TO ZERO                  RL2034.2
058300              MOVE WRK-CS-09V00-009 TO COMPUTED-18V0              RL2034.2
058400             MOVE   ZERO TO CORRECT-18V0                          RL2034.2
058500              MOVE "DELETED RECORDS" TO RE-MARK                   RL2034.2
058600              PERFORM FAIL                                        RL2034.2
058700              ELSE                                                RL2034.2
058800              PERFORM PASS.                                       RL2034.2
058900     PERFORM  PRINT-DETAIL.                                       RL2034.2
059000     ADD      01  TO  REC-CT.                                     RL2034.2
059100*    .02                                                          RL2034.2
059200     IF       WRK-CS-09V00-010 NOT EQUAL TO 375                   RL2034.2
059300              MOVE "KEY MISMATCH" TO RE-MARK                      RL2034.2
059400             MOVE 375 TO CORRECT-18V0                             RL2034.2
059500              MOVE WRK-CS-09V00-010 TO COMPUTED-18V0              RL2034.2
059600              PERFORM FAIL                                        RL2034.2
059700              ELSE                                                RL2034.2
059800              PERFORM PASS.                                       RL2034.2
059900     PERFORM  PRINT-DETAIL.                                       RL2034.2
060000     ADD      01 TO REC-CT.                                       RL2034.2
060100*    .03                                                          RL2034.2
060200     IF      WRK-CS-09V00-011  NOT EQUAL TO 375                   RL2034.2
060300             MOVE   375  TO CORRECT-18V0                          RL2034.2
060400             MOVE  "INCORRECT RECORD FOUND"  TO RE-MARK           RL2034.2
060500             MOVE   WRK-CS-09V00-011 TO COMPUTED-18V0             RL2034.2
060600             PERFORM   FAIL                                       RL2034.2
060700             ELSE                                                 RL2034.2
060800             PERFORM  PASS.                                       RL2034.2
060900     PERFORM   PRINT-DETAIL.                                      RL2034.2
061000*04                                                               RL2034.2
061100     CLOSE    RL-FD1.                                             RL2034.2
061200 CCVS-EXIT SECTION.                                               RL2034.2
061300 CCVS-999999.                                                     RL2034.2
061400     GO TO CLOSE-FILES.                                           RL2034.2
      *END-OF,RL203A                                                                  
