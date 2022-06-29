      *HEADER,COBOL,RL101A,SUBPRG,RL103A                                              
000100 IDENTIFICATION DIVISION.                                         RL1034.2
000200 PROGRAM-ID.                                                      RL1034.2
000300     RL103A.                                                      RL1034.2
000400****************************************************************  RL1034.2
000500*                                                              *  RL1034.2
000600*    VALIDATION FOR:-                                          *  RL1034.2
000700*                                                              *  RL1034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1034.2
000900*                                                              *  RL1034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1034.2
001100*                                                              *  RL1034.2
001200****************************************************************  RL1034.2
001300*GENERAL:    THIS PROGRAM IS THE THIRD OF A SERIES.  THE FUNCTION RL1034.2
001400*            OF THIS PROGRAM IS TO PROCESS THE FILE SEQUENTIALLY  RL1034.2
001500*            (ACCESS MODE IS SEQUENTIAL). THE FILE USED IS THAT   RL1034.2
001600*            RESULTING FROM RL102.                                RL1034.2
001700*                                                                 RL1034.2
001800*            FIRST, THE FILE IS VERIFIED FOR ACCURACY OF ITS 500  RL1034.2
001900*            RECORDS.  SECONDLY, RECORDS OF THER FILE ARE         RL1034.2
002000*            SELECTIVELY DELETED AND THIRDLY THE ACCURACY OF EACH RL1034.2
002100*            RECORD IN THE FILE IS AGAIN VERIFIED.                RL1034.2
002200*                                                                 RL1034.2
002300*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL1034.2
002400*            PROGRAM ARE:                                         RL1034.2
002500*                                                                 RL1034.2
002600*                                                                 RL1034.2
002700*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1034.2
002800*                         RELATIVE  I-O DATA FILE                 RL1034.2
002900*                 X-55   SYSTEM PRINTER                           RL1034.2
003000*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL1034.2
003100*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL1034.2
003200*                 X-75   OBJECT OF VALUE OF CLAUSE                RL1034.2
003300*                 X-82   SOURCE-COMPUTER                          RL1034.2
003400*                 X-83   OBJECT-COMPUTER.                         RL1034.2
003500*                                                                 RL1034.2
003600****************************************************************  RL1034.2
003700 ENVIRONMENT DIVISION.                                            RL1034.2
003800 CONFIGURATION SECTION.                                           RL1034.2
003900 SOURCE-COMPUTER.                                                 RL1034.2
004000     XXXXX082.                                                    RL1034.2
004100 OBJECT-COMPUTER.                                                 RL1034.2
004200     XXXXX083.                                                    RL1034.2
004300 INPUT-OUTPUT SECTION.                                            RL1034.2
004400 FILE-CONTROL.                                                    RL1034.2
004500     SELECT PRINT-FILE ASSIGN TO                                  RL1034.2
004600     XXXXX055.                                                    RL1034.2
004700     SELECT   RL-FS1 ASSIGN TO                                    RL1034.2
004800     XXXXD021                                                     RL1034.2
004900              ORGANIZATION IS RELATIVE                            RL1034.2
005000             ACCESS  MODE IS SEQUENTIAL                           RL1034.2
005100              RELATIVE KEY IS RL-FS1-KEY.                         RL1034.2
005200 DATA DIVISION.                                                   RL1034.2
005300 FILE SECTION.                                                    RL1034.2
005400 FD  PRINT-FILE.                                                  RL1034.2
005500 01  PRINT-REC PICTURE X(120).                                    RL1034.2
005600 01  DUMMY-RECORD PICTURE X(120).                                 RL1034.2
005700 FD  RL-FS1                                                       RL1034.2
005800     LABEL RECORDS STANDARD                                       RL1034.2
005900C    VALUE OF                                                     RL1034.2
006000C    XXXXX074                                                     RL1034.2
006100C    IS                                                           RL1034.2
006200C    XXXXX075                                                     RL1034.2
006300*    XXXXX069                                                     RL1034.2
006400     BLOCK CONTAINS 01 RECORDS                                    RL1034.2
006500     RECORD CONTAINS 120.                                         RL1034.2
006600 01  RL-FS1R1-F-G-120.                                            RL1034.2
006700     02 RL-WRK-120 PIC X(120).                                    RL1034.2
006800 WORKING-STORAGE SECTION.                                         RL1034.2
006900 01  RL-FS1-KEY        PIC 9(08)  USAGE COMP VALUE ZERO.          RL1034.2
007000 01  WRK-CS-09V00-006 PIC S9(09) USAGE COMP VALUE ZERO.           RL1034.2
007100 01  WRK-CS-09V00-007 PIC S9(09) USAGE COMP VALUE ZERO.           RL1034.2
007200 01  WRK-CS-09V00-008 PIC S9(09) USAGE COMP VALUE ZERO.           RL1034.2
007300 01  WRK-CS-09V00-009 PIC S9(09) USAGE COMP VALUE ZERO.           RL1034.2
007400 01  WRK-CS-09V00-010 PIC S9(09) USAGE COMP VALUE ZERO.           RL1034.2
007500 01  WRK-CS-09V00-011 PIC S9(09) USAGE COMP VALUE ZERO.           RL1034.2
007600 01  I-O-ERROR-RL-FS1 PIC X(3) VALUE "NO ".                       RL1034.2
007700 01  FILE-RECORD-INFORMATION-REC.                                 RL1034.2
007800     03 FILE-RECORD-INFO-SKELETON.                                RL1034.2
007900        05 FILLER                 PICTURE X(48)       VALUE       RL1034.2
008000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1034.2
008100        05 FILLER                 PICTURE X(46)       VALUE       RL1034.2
008200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1034.2
008300        05 FILLER                 PICTURE X(26)       VALUE       RL1034.2
008400             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1034.2
008500        05 FILLER                 PICTURE X(37)       VALUE       RL1034.2
008600             ",RECKEY=                             ".             RL1034.2
008700        05 FILLER                 PICTURE X(38)       VALUE       RL1034.2
008800             ",ALTKEY1=                             ".            RL1034.2
008900        05 FILLER                 PICTURE X(38)       VALUE       RL1034.2
009000             ",ALTKEY2=                             ".            RL1034.2
009100        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1034.2
009200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1034.2
009300        05 FILE-RECORD-INFO-P1-120.                               RL1034.2
009400           07 FILLER              PIC X(5).                       RL1034.2
009500           07 XFILE-NAME           PIC X(6).                      RL1034.2
009600           07 FILLER              PIC X(8).                       RL1034.2
009700           07 XRECORD-NAME         PIC X(6).                      RL1034.2
009800           07 FILLER              PIC X(1).                       RL1034.2
009900           07 REELUNIT-NUMBER     PIC 9(1).                       RL1034.2
010000           07 FILLER              PIC X(7).                       RL1034.2
010100           07 XRECORD-NUMBER       PIC 9(6).                      RL1034.2
010200           07 FILLER              PIC X(6).                       RL1034.2
010300           07 UPDATE-NUMBER       PIC 9(2).                       RL1034.2
010400           07 FILLER              PIC X(5).                       RL1034.2
010500           07 ODO-NUMBER          PIC 9(4).                       RL1034.2
010600           07 FILLER              PIC X(5).                       RL1034.2
010700           07 XPROGRAM-NAME        PIC X(5).                      RL1034.2
010800           07 FILLER              PIC X(7).                       RL1034.2
010900           07 XRECORD-LENGTH       PIC 9(6).                      RL1034.2
011000           07 FILLER              PIC X(7).                       RL1034.2
011100           07 CHARS-OR-RECORDS    PIC X(2).                       RL1034.2
011200           07 FILLER              PIC X(1).                       RL1034.2
011300           07 XBLOCK-SIZE          PIC 9(4).                      RL1034.2
011400           07 FILLER              PIC X(6).                       RL1034.2
011500           07 RECORDS-IN-FILE     PIC 9(6).                       RL1034.2
011600           07 FILLER              PIC X(5).                       RL1034.2
011700           07 XFILE-ORGANIZATION   PIC X(2).                      RL1034.2
011800           07 FILLER              PIC X(6).                       RL1034.2
011900           07 XLABEL-TYPE          PIC X(1).                      RL1034.2
012000        05 FILE-RECORD-INFO-P121-240.                             RL1034.2
012100           07 FILLER              PIC X(8).                       RL1034.2
012200           07 XRECORD-KEY          PIC X(29).                     RL1034.2
012300           07 FILLER              PIC X(9).                       RL1034.2
012400           07 ALTERNATE-KEY1      PIC X(29).                      RL1034.2
012500           07 FILLER              PIC X(9).                       RL1034.2
012600           07 ALTERNATE-KEY2      PIC X(29).                      RL1034.2
012700           07 FILLER              PIC X(7).                       RL1034.2
012800 01  TEST-RESULTS.                                                RL1034.2
012900     02 FILLER                   PIC X      VALUE SPACE.          RL1034.2
013000     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1034.2
013100     02 FILLER                   PIC X      VALUE SPACE.          RL1034.2
013200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1034.2
013300     02 FILLER                   PIC X      VALUE SPACE.          RL1034.2
013400     02  PAR-NAME.                                                RL1034.2
013500       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1034.2
013600       03  PARDOT-X              PIC X      VALUE SPACE.          RL1034.2
013700       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1034.2
013800     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1034.2
013900     02 RE-MARK                  PIC X(61).                       RL1034.2
014000 01  TEST-COMPUTED.                                               RL1034.2
014100     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1034.2
014200     02 FILLER                   PIC X(17)  VALUE                 RL1034.2
014300            "       COMPUTED=".                                   RL1034.2
014400     02 COMPUTED-X.                                               RL1034.2
014500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1034.2
014600     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1034.2
014700                                 PIC -9(9).9(9).                  RL1034.2
014800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1034.2
014900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1034.2
015000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1034.2
015100     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1034.2
015200         04 COMPUTED-18V0                    PIC -9(18).          RL1034.2
015300         04 FILLER                           PIC X.               RL1034.2
015400     03 FILLER PIC X(50) VALUE SPACE.                             RL1034.2
015500 01  TEST-CORRECT.                                                RL1034.2
015600     02 FILLER PIC X(30) VALUE SPACE.                             RL1034.2
015700     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1034.2
015800     02 CORRECT-X.                                                RL1034.2
015900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1034.2
016000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1034.2
016100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1034.2
016200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1034.2
016300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1034.2
016400     03      CR-18V0 REDEFINES CORRECT-A.                         RL1034.2
016500         04 CORRECT-18V0                     PIC -9(18).          RL1034.2
016600         04 FILLER                           PIC X.               RL1034.2
016700     03 FILLER PIC X(2) VALUE SPACE.                              RL1034.2
016800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1034.2
016900 01  CCVS-C-1.                                                    RL1034.2
017000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1034.2
017100-    "SS  PARAGRAPH-NAME                                          RL1034.2
017200-    "       REMARKS".                                            RL1034.2
017300     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1034.2
017400 01  CCVS-C-2.                                                    RL1034.2
017500     02 FILLER                     PIC X        VALUE SPACE.      RL1034.2
017600     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1034.2
017700     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1034.2
017800     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1034.2
017900     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1034.2
018000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1034.2
018100 01  REC-CT                        PIC 99       VALUE ZERO.       RL1034.2
018200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1034.2
018300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1034.2
018400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1034.2
018500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1034.2
018600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1034.2
018700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1034.2
018800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1034.2
018900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1034.2
019000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1034.2
019100 01  CCVS-H-1.                                                    RL1034.2
019200     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1034.2
019300     02  FILLER                    PIC X(42)    VALUE             RL1034.2
019400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1034.2
019500     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1034.2
019600 01  CCVS-H-2A.                                                   RL1034.2
019700   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1034.2
019800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1034.2
019900   02  FILLER                        PIC XXXX   VALUE             RL1034.2
020000     "4.2 ".                                                      RL1034.2
020100   02  FILLER                        PIC X(28)  VALUE             RL1034.2
020200            " COPY - NOT FOR DISTRIBUTION".                       RL1034.2
020300   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1034.2
020400                                                                  RL1034.2
020500 01  CCVS-H-2B.                                                   RL1034.2
020600   02  FILLER                        PIC X(15)  VALUE             RL1034.2
020700            "TEST RESULT OF ".                                    RL1034.2
020800   02  TEST-ID                       PIC X(9).                    RL1034.2
020900   02  FILLER                        PIC X(4)   VALUE             RL1034.2
021000            " IN ".                                               RL1034.2
021100   02  FILLER                        PIC X(12)  VALUE             RL1034.2
021200     " HIGH       ".                                              RL1034.2
021300   02  FILLER                        PIC X(22)  VALUE             RL1034.2
021400            " LEVEL VALIDATION FOR ".                             RL1034.2
021500   02  FILLER                        PIC X(58)  VALUE             RL1034.2
021600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1034.2
021700 01  CCVS-H-3.                                                    RL1034.2
021800     02  FILLER                      PIC X(34)  VALUE             RL1034.2
021900            " FOR OFFICIAL USE ONLY    ".                         RL1034.2
022000     02  FILLER                      PIC X(58)  VALUE             RL1034.2
022100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1034.2
022200     02  FILLER                      PIC X(28)  VALUE             RL1034.2
022300            "  COPYRIGHT   1985 ".                                RL1034.2
022400 01  CCVS-E-1.                                                    RL1034.2
022500     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1034.2
022600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1034.2
022700     02 ID-AGAIN                     PIC X(9).                    RL1034.2
022800     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1034.2
022900 01  CCVS-E-2.                                                    RL1034.2
023000     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1034.2
023100     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1034.2
023200     02 CCVS-E-2-2.                                               RL1034.2
023300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1034.2
023400         03 FILLER                   PIC X      VALUE SPACE.      RL1034.2
023500         03 ENDER-DESC               PIC X(44)  VALUE             RL1034.2
023600            "ERRORS ENCOUNTERED".                                 RL1034.2
023700 01  CCVS-E-3.                                                    RL1034.2
023800     02  FILLER                      PIC X(22)  VALUE             RL1034.2
023900            " FOR OFFICIAL USE ONLY".                             RL1034.2
024000     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1034.2
024100     02  FILLER                      PIC X(58)  VALUE             RL1034.2
024200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1034.2
024300     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1034.2
024400     02 FILLER                       PIC X(15)  VALUE             RL1034.2
024500             " COPYRIGHT 1985".                                   RL1034.2
024600 01  CCVS-E-4.                                                    RL1034.2
024700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1034.2
024800     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1034.2
024900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1034.2
025000     02 FILLER                       PIC X(40)  VALUE             RL1034.2
025100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1034.2
025200 01  XXINFO.                                                      RL1034.2
025300     02 FILLER                       PIC X(19)  VALUE             RL1034.2
025400            "*** INFORMATION ***".                                RL1034.2
025500     02 INFO-TEXT.                                                RL1034.2
025600       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1034.2
025700       04 XXCOMPUTED                 PIC X(20).                   RL1034.2
025800       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1034.2
025900       04 XXCORRECT                  PIC X(20).                   RL1034.2
026000     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1034.2
026100 01  HYPHEN-LINE.                                                 RL1034.2
026200     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1034.2
026300     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1034.2
026400-    "*****************************************".                 RL1034.2
026500     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1034.2
026600-    "******************************".                            RL1034.2
026700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1034.2
026800     "RL103A".                                                    RL1034.2
026900 PROCEDURE DIVISION.                                              RL1034.2
027000 CCVS1 SECTION.                                                   RL1034.2
027100 OPEN-FILES.                                                      RL1034.2
027200     OPEN    OUTPUT PRINT-FILE.                                   RL1034.2
027300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1034.2
027400     MOVE    SPACE TO TEST-RESULTS.                               RL1034.2
027500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1034.2
027600     MOVE    ZERO TO REC-SKL-SUB.                                 RL1034.2
027700     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1034.2
027800 CCVS-INIT-FILE.                                                  RL1034.2
027900     ADD     1 TO REC-SKL-SUB.                                    RL1034.2
028000     MOVE    FILE-RECORD-INFO-SKELETON                            RL1034.2
028100          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1034.2
028200 CCVS-INIT-EXIT.                                                  RL1034.2
028300     GO TO CCVS1-EXIT.                                            RL1034.2
028400 CLOSE-FILES.                                                     RL1034.2
028500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1034.2
028600 TERMINATE-CCVS.                                                  RL1034.2
028700*    EXIT PROGRAM.                                                RL1034.2
028800*TERMINATE-CALL.                                                  RL1034.2
028900     STOP     RUN.                                                RL1034.2
029000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1034.2
029100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1034.2
029200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1034.2
029300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1034.2
029400     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1034.2
029500 PRINT-DETAIL.                                                    RL1034.2
029600     IF REC-CT NOT EQUAL TO ZERO                                  RL1034.2
029700             MOVE "." TO PARDOT-X                                 RL1034.2
029800             MOVE REC-CT TO DOTVALUE.                             RL1034.2
029900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1034.2
030000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1034.2
030100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1034.2
030200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1034.2
030300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1034.2
030400     MOVE SPACE TO CORRECT-X.                                     RL1034.2
030500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1034.2
030600     MOVE     SPACE TO RE-MARK.                                   RL1034.2
030700 HEAD-ROUTINE.                                                    RL1034.2
030800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1034.2
030900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1034.2
031000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1034.2
031100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1034.2
031200 COLUMN-NAMES-ROUTINE.                                            RL1034.2
031300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1034.2
031400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1034.2
031500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1034.2
031600 END-ROUTINE.                                                     RL1034.2
031700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1034.2
031800 END-RTN-EXIT.                                                    RL1034.2
031900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1034.2
032000 END-ROUTINE-1.                                                   RL1034.2
032100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1034.2
032200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1034.2
032300      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1034.2
032400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1034.2
032500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1034.2
032600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1034.2
032700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1034.2
032800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1034.2
032900  END-ROUTINE-12.                                                 RL1034.2
033000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1034.2
033100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1034.2
033200         MOVE "NO " TO ERROR-TOTAL                                RL1034.2
033300         ELSE                                                     RL1034.2
033400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1034.2
033500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1034.2
033600     PERFORM WRITE-LINE.                                          RL1034.2
033700 END-ROUTINE-13.                                                  RL1034.2
033800     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1034.2
033900         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1034.2
034000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1034.2
034100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1034.2
034200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1034.2
034300      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1034.2
034400          MOVE "NO " TO ERROR-TOTAL                               RL1034.2
034500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1034.2
034600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1034.2
034700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1034.2
034800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1034.2
034900 WRITE-LINE.                                                      RL1034.2
035000     ADD 1 TO RECORD-COUNT.                                       RL1034.2
035100*    IF RECORD-COUNT GREATER 50                                   RL1034.2
035200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1034.2
035300*        MOVE SPACE TO DUMMY-RECORD                               RL1034.2
035400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1034.2
035500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1034.2
035600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1034.2
035700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1034.2
035800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1034.2
035900*        MOVE ZERO TO RECORD-COUNT.                               RL1034.2
036000     PERFORM WRT-LN.                                              RL1034.2
036100 WRT-LN.                                                          RL1034.2
036200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1034.2
036300     MOVE SPACE TO DUMMY-RECORD.                                  RL1034.2
036400 BLANK-LINE-PRINT.                                                RL1034.2
036500     PERFORM WRT-LN.                                              RL1034.2
036600 FAIL-ROUTINE.                                                    RL1034.2
036700     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1034.2
036800            GO TO   FAIL-ROUTINE-WRITE.                           RL1034.2
036900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1034.2
037000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1034.2
037100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1034.2
037200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1034.2
037300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1034.2
037400     GO TO  FAIL-ROUTINE-EX.                                      RL1034.2
037500 FAIL-ROUTINE-WRITE.                                              RL1034.2
037600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1034.2
037700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1034.2
037800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1034.2
037900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1034.2
038000 FAIL-ROUTINE-EX. EXIT.                                           RL1034.2
038100 BAIL-OUT.                                                        RL1034.2
038200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1034.2
038300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1034.2
038400 BAIL-OUT-WRITE.                                                  RL1034.2
038500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1034.2
038600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1034.2
038700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1034.2
038800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1034.2
038900 BAIL-OUT-EX. EXIT.                                               RL1034.2
039000 CCVS1-EXIT.                                                      RL1034.2
039100     EXIT.                                                        RL1034.2
039200 SECT-RL103-001 SECTION.                                          RL1034.2
039300 REL-INIT-006.                                                    RL1034.2
039400     MOVE  99 TO RL-FS1-KEY.                                      RL1034.2
039500*    THIS FILE "RL-FS1" IS ACCESSED SEQUENTIALLY AND HAS          RL1034.2
039600*    ASSOCIATED WITH IT A RELATIVE KEY WHICH AT ALL TIMES SHOULD  RL1034.2
039700*    CONTAIN THE NUMBER OF THE RECORD PREVIOUSLY READ.            RL1034.2
039800     OPEN INPUT RL-FS1.                                           RL1034.2
039900     MOVE     "REL-TEST-006" TO   PAR-NAME.                       RL1034.2
040000     MOVE     ZERO TO             WRK-CS-09V00-006.               RL1034.2
040100     MOVE     ZERO TO             WRK-CS-09V00-007.               RL1034.2
040200     MOVE     ZERO TO             WRK-CS-09V00-008.               RL1034.2
040300     MOVE     ZERO TO             WRK-CS-09V00-009.               RL1034.2
040400     MOVE     ZERO TO             WRK-CS-09V00-010.               RL1034.2
040500     MOVE     ZERO TO             WRK-CS-09V00-011.               RL1034.2
040600     MOVE     SPACE TO  FILE-RECORD-INFO-P1-120 (1).              RL1034.2
040700     MOVE    RL-FS1-KEY TO WRK-CS-09V00-011.                      RL1034.2
040800     MOVE     01 TO REC-CT.                                       RL1034.2
040900     MOVE    "READ SEQUENTIAL"  TO FEATURE.                       RL1034.2
041000 REL-TEST-006-R.                                                  RL1034.2
041100     ADD      1 TO WRK-CS-09V00-006.                              RL1034.2
041200     READ     RL-FS1                                              RL1034.2
041300              AT END GO TO REL-TEST-006-3.                        RL1034.2
041400     MOVE     RL-FS1R1-F-G-120    TO FILE-RECORD-INFO-P1-120 (1). RL1034.2
041500     IF       UPDATE-NUMBER (1) EQUAL TO 00                       RL1034.2
041600             ADD 1 TO WRK-CS-09V00-007                            RL1034.2
041700              GO TO REL-TEST-006-2.                               RL1034.2
041800     IF       UPDATE-NUMBER (1) EQUAL TO 01                       RL1034.2
041900              ADD 1 TO WRK-CS-09V00-008                           RL1034.2
042000              GO TO REL-TEST-006-2.                               RL1034.2
042100     ADD      1 TO WRK-CS-09V00-009.                              RL1034.2
042200 REL-TEST-006-2.                                                  RL1034.2
042300     IF       RL-FS1-KEY NOT EQUAL TO XRECORD-NUMBER (1)          RL1034.2
042400              ADD 1 TO  WRK-CS-09V00-010.                         RL1034.2
042500     IF       WRK-CS-09V00-006  GREATER 501                       RL1034.2
042600              GO TO REL-TEST-006-3.                               RL1034.2
042700     GO TO    REL-TEST-006-R.                                     RL1034.2
042800 REL-TEST-006-3.                                                  RL1034.2
042900     IF       WRK-CS-09V00-006 NOT EQUAL TO 501                   RL1034.2
043000              MOVE "INCORRECT RECORD COUNT"  TO RE-MARK           RL1034.2
043100              MOVE  WRK-CS-09V00-006 TO COMPUTED-18V0             RL1034.2
043200              MOVE  501  TO             CORRECT-18V0              RL1034.2
043300              PERFORM FAIL                                        RL1034.2
043400              ELSE                                                RL1034.2
043500              PERFORM PASS.                                       RL1034.2
043600     PERFORM  PRINT-DETAIL.                                       RL1034.2
043700*    .01                                                          RL1034.2
043800     ADD      1 TO REC-CT.                                        RL1034.2
043900     IF       WRK-CS-09V00-007 EQUAL TO 400                       RL1034.2
044000              PERFORM PASS                                        RL1034.2
044100              ELSE                                                RL1034.2
044200              MOVE "NON-UPDATED RECORDS" TO COMPUTED-A            RL1034.2
044300              MOVE  WRK-CS-09V00-007 TO CORRECT-18V0              RL1034.2
044400              MOVE "SHOULD BE 400" TO RE-MARK                     RL1034.2
044500              PERFORM FAIL.                                       RL1034.2
044600     PERFORM  PRINT-DETAIL.                                       RL1034.2
044700     ADD      1 TO REC-CT.                                        RL1034.2
044800*    .02                                                          RL1034.2
044900     IF      WRK-CS-09V00-008 EQUAL TO 100                        RL1034.2
045000              PERFORM PASS                                        RL1034.2
045100              ELSE                                                RL1034.2
045200             MOVE WRK-CS-09V00-008 TO COMPUTED-18V0               RL1034.2
045300             MOVE 100             TO  CORRECT-18V0                RL1034.2
045400             MOVE "UPDATED RECORDS" TO RE-MARK                    RL1034.2
045500              PERFORM FAIL.                                       RL1034.2
045600     PERFORM  PRINT-DETAIL.                                       RL1034.2
045700     ADD      1 TO REC-CT.                                        RL1034.2
045800*    .03                                                          RL1034.2
045900     IF      WRK-CS-09V00-009 EQUAL TO ZERO                       RL1034.2
046000             PERFORM PASS                                         RL1034.2
046100             ELSE                                                 RL1034.2
046200             MOVE WRK-CS-09V00-009 TO COMPUTED-18V0               RL1034.2
046300             MOVE  ZERO            TO CORRECT-18V0                RL1034.2
046400             MOVE "BAD-UPDATES" TO RE-MARK                        RL1034.2
046500             PERFORM FAIL.                                        RL1034.2
046600     PERFORM PRINT-DETAIL.                                        RL1034.2
046700     ADD     01 TO REC-CT.                                        RL1034.2
046800*    .04                                                          RL1034.2
046900     IF      WRK-CS-09V00-010 EQUAL TO ZERO                       RL1034.2
047000             PERFORM PASS                                         RL1034.2
047100             ELSE                                                 RL1034.2
047200             MOVE WRK-CS-09V00-010 TO COMPUTED-18V0               RL1034.2
047300             MOVE ZERO             TO CORRECT-18V0                RL1034.2
047400             MOVE "KEY VS RECORD" TO RE-MARK                      RL1034.2
047500             PERFORM FAIL.                                        RL1034.2
047600     PERFORM PRINT-DETAIL.                                        RL1034.2
047700     ADD     01 TO REC-CT.                                        RL1034.2
047800*    .05                                                          RL1034.2
047900     MOVE    WRK-CS-09V00-011 TO RL-FS1-KEY.                      RL1034.2
048000     MOVE RL-FS1-KEY TO COMPUTED-18V0.                            RL1034.2
048100     MOVE    "INFORMATION" TO CORRECT-A.                          RL1034.2
048200     MOVE    "STATUS AFTER OPEN" TO RE-MARK.                      RL1034.2
048300     PERFORM PRINT-DETAIL.                                        RL1034.2
048400     ADD     01 TO REC-CT.                                        RL1034.2
048500*    .06                                                          RL1034.2
048600     CLOSE    RL-FS1.                                             RL1034.2
048700 REL-INIT-007.                                                    RL1034.2
048800     MOVE     "REL-TEST-007" TO PAR-NAME                          RL1034.2
048900     OPEN     I-O RL-FS1.                                         RL1034.2
049000     MOVE     ZERO TO WRK-CS-09V00-006                            RL1034.2
049100     MOVE     ZERO TO WRK-CS-09V00-007                            RL1034.2
049200     MOVE     ZERO TO WRK-CS-09V00-008                            RL1034.2
049300     MOVE     ZERO TO WRK-CS-09V00-009                            RL1034.2
049400     MOVE     ZERO TO WRK-CS-09V00-010                            RL1034.2
049500     MOVE     ZERO TO WRK-CS-09V00-011                            RL1034.2
049600     MOVE     01 TO REC-CT.                                       RL1034.2
049700     MOVE     SPACE TO  FILE-RECORD-INFO-P1-120 (1).              RL1034.2
049800     MOVE    "DELETE"  TO FEATURE.                                RL1034.2
049900 REL-TEST-007-R.                                                  RL1034.2
050000     ADD      1 TO WRK-CS-09V00-006                               RL1034.2
050100     ADD      1 TO WRK-CS-09V00-007.                              RL1034.2
050200     READ     RL-FS1                                              RL1034.2
050300              AT END                                              RL1034.2
050400              MOVE "AT END PATH TAKEN " TO RE-MARK                RL1034.2
050500             GO TO  REL-TEST-007-3.                               RL1034.2
050600     MOVE     RL-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).    RL1034.2
050700     IF       WRK-CS-09V00-007  EQUAL TO 4                        RL1034.2
050800              GO TO REL-TEST-007-2.                               RL1034.2
050900     IF       WRK-CS-09V00-006 GREATER 501                        RL1034.2
051000              MOVE  "AT END NOT TAKEN"  TO RE-MARK                RL1034.2
051100              GO TO REL-TEST-007-3.                               RL1034.2
051200     GO TO    REL-TEST-007-R.                                     RL1034.2
051300 REL-TEST-007-2.                                                  RL1034.2
051400     MOVE CCVS-PGM-ID       TO  XPROGRAM-NAME (1).                RL1034.2
051500     MOVE  99 TO UPDATE-NUMBER (1).                               RL1034.2
051600     MOVE     FILE-RECORD-INFO-P1-120 (1) TO RL-FS1R1-F-G-120.    RL1034.2
051700     DELETE    RL-FS1.                                            RL1034.2
051800     MOVE     ZERO TO  WRK-CS-09V00-007.                          RL1034.2
051900     ADD      1 TO  WRK-CS-09V00-008                              RL1034.2
052000     GO TO    REL-TEST-007-R.                                     RL1034.2
052100 REL-TEST-007-3.                                                  RL1034.2
052200     IF       WRK-CS-09V00-006 NOT EQUAL TO 501                   RL1034.2
052300              MOVE WRK-CS-09V00-006 TO COMPUTED-18V0              RL1034.2
052400              MOVE              501 TO CORRECT-18V0               RL1034.2
052500              PERFORM FAIL                                        RL1034.2
052600              ELSE                                                RL1034.2
052700              PERFORM PASS.                                       RL1034.2
052800     PERFORM  PRINT-DETAIL.                                       RL1034.2
052900     ADD      01 TO REC-CT.                                       RL1034.2
053000*    .01                                                          RL1034.2
053100     IF       WRK-CS-09V00-008 NOT EQUAL TO 125                   RL1034.2
053200              MOVE WRK-CS-09V00-008 TO COMPUTED-18V0              RL1034.2
053300              MOVE 125              TO CORRECT-18V0               RL1034.2
053400              MOVE "DELETED RECORDS" TO RE-MARK                   RL1034.2
053500              PERFORM FAIL                                        RL1034.2
053600              ELSE                                                RL1034.2
053700              PERFORM PASS.                                       RL1034.2
053800     PERFORM  PRINT-DETAIL.                                       RL1034.2
053900     ADD      01 TO REC-CT.                                       RL1034.2
054000*    .02                                                          RL1034.2
054100     CLOSE    RL-FS1.                                             RL1034.2
054200 REL-INIT-008.                                                    RL1034.2
054300     MOVE     "REL-TEST-008" TO PAR-NAME.                         RL1034.2
054400     MOVE     ZERO TO   WRK-CS-09V00-006                          RL1034.2
054500     MOVE     ZERO TO   WRK-CS-09V00-007                          RL1034.2
054600     MOVE     ZERO TO   WRK-CS-09V00-008                          RL1034.2
054700     MOVE     ZERO TO   WRK-CS-09V00-009                          RL1034.2
054800     MOVE     ZERO TO   WRK-CS-09V00-010                          RL1034.2
054900     MOVE     ZERO TO   WRK-CS-09V00-011                          RL1034.2
055000     MOVE     01 TO REC-CT.                                       RL1034.2
055100     MOVE     SPACE  TO  FILE-RECORD-INFO-P1-120 (1).             RL1034.2
055200     MOVE     ZERO TO RL-FS1-KEY.                                 RL1034.2
055300     OPEN     INPUT  RL-FS1.                                      RL1034.2
055400     MOVE    "READ UPDATED FILE"  TO FEATURE.                     RL1034.2
055500 REL-TEST-008-R.                                                  RL1034.2
055600     ADD      1 TO WRK-CS-09V00-006.                              RL1034.2
055700     ADD      1 TO WRK-CS-09V00-007.                              RL1034.2
055800     ADD      1 TO WRK-CS-09V00-008.                              RL1034.2
055900     READ     RL-FS1  AT END  GO TO REL-TEST-008-3.               RL1034.2
056000     MOVE     RL-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).    RL1034.2
056100     IF       UPDATE-NUMBER (1) EQUAL TO 99                       RL1034.2
056200              ADD  1 TO WRK-CS-09V00-009.                         RL1034.2
056300     IF       WRK-CS-09V00-007  EQUAL TO 4                        RL1034.2
056400              MOVE 01 TO WRK-CS-09V00-007                         RL1034.2
056500              ADD 1 TO WRK-CS-09V00-008.                          RL1034.2
056600     IF       RL-FS1-KEY EQUAL TO  XRECORD-NUMBER (1)             RL1034.2
056700              ADD 1 TO  WRK-CS-09V00-010.                         RL1034.2
056800     IF       XRECORD-NUMBER (1) EQUAL TO  WRK-CS-09V00-008       RL1034.2
056900              ADD 1 TO  WRK-CS-09V00-011.                         RL1034.2
057000     IF       WRK-CS-09V00-006 GREATER  501                       RL1034.2
057100              GO TO REL-TEST-008-3.                               RL1034.2
057200     GO TO    REL-TEST-008-R.                                     RL1034.2
057300 REL-TEST-008-3.                                                  RL1034.2
057400     IF       WRK-CS-09V00-006 NOT EQUAL TO 376                   RL1034.2
057500              MOVE "INCORRECT RECORD COUNT"  TO RE-MARK           RL1034.2
057600              MOVE WRK-CS-09V00-006 TO COMPUTED-18V0              RL1034.2
057700              MOVE 376 TO CORRECT-18V0                            RL1034.2
057800              PERFORM  FAIL                                       RL1034.2
057900              ELSE                                                RL1034.2
058000              PERFORM  PASS.                                      RL1034.2
058100     PERFORM  PRINT-DETAIL.                                       RL1034.2
058200     ADD      01 TO REC-CT.                                       RL1034.2
058300*    .01                                                          RL1034.2
058400     IF       WRK-CS-09V00-009 NOT EQUAL TO ZERO                  RL1034.2
058500              MOVE WRK-CS-09V00-009 TO COMPUTED-18V0              RL1034.2
058600             MOVE   ZERO TO CORRECT-18V0                          RL1034.2
058700              MOVE "DELETED RECORDS" TO RE-MARK                   RL1034.2
058800              PERFORM FAIL                                        RL1034.2
058900              ELSE                                                RL1034.2
059000              PERFORM PASS.                                       RL1034.2
059100     PERFORM  PRINT-DETAIL.                                       RL1034.2
059200     ADD      01  TO  REC-CT.                                     RL1034.2
059300*    .02                                                          RL1034.2
059400     IF       WRK-CS-09V00-010 NOT EQUAL TO 375                   RL1034.2
059500              MOVE "KEY MISMATCH" TO RE-MARK                      RL1034.2
059600             MOVE 375 TO CORRECT-18V0                             RL1034.2
059700              MOVE WRK-CS-09V00-010 TO COMPUTED-18V0              RL1034.2
059800              PERFORM FAIL                                        RL1034.2
059900              ELSE                                                RL1034.2
060000              PERFORM PASS.                                       RL1034.2
060100     PERFORM  PRINT-DETAIL.                                       RL1034.2
060200     ADD      01 TO REC-CT.                                       RL1034.2
060300*    .03                                                          RL1034.2
060400     IF      WRK-CS-09V00-011  NOT EQUAL TO 375                   RL1034.2
060500             MOVE   375  TO CORRECT-18V0                          RL1034.2
060600             MOVE  "INCORRECT RECORD FOUND"  TO RE-MARK           RL1034.2
060700             MOVE   WRK-CS-09V00-011 TO COMPUTED-18V0             RL1034.2
060800             PERFORM   FAIL                                       RL1034.2
060900             ELSE                                                 RL1034.2
061000             PERFORM  PASS.                                       RL1034.2
061100     PERFORM   PRINT-DETAIL.                                      RL1034.2
061200*04                                                               RL1034.2
061300     CLOSE    RL-FS1.                                             RL1034.2
061400 CCVS-EXIT SECTION.                                               RL1034.2
061500 CCVS-999999.                                                     RL1034.2
061600     GO TO CLOSE-FILES.                                           RL1034.2
      *END-OF,RL103A                                                                  
