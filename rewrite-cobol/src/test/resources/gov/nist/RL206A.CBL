      *HEADER,COBOL,RL206A                                                            
000100 IDENTIFICATION DIVISION.                                         RL2064.2
000200 PROGRAM-ID.                                                      RL2064.2
000300     RL206A.                                                      RL2064.2
000400****************************************************************  RL2064.2
000500*                                                              *  RL2064.2
000600*    VALIDATION FOR:-                                          *  RL2064.2
000700*                                                              *  RL2064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2064.2
000900*                                                              *  RL2064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2064.2
001100*                                                              *  RL2064.2
001200****************************************************************  RL2064.2
001300*GENERAL:    THIS RUN UNIT IS THE FIRST OF A SERIES WHICH         RL2064.2
001400*            PROCESSES A RELATIVE I-O FILE.  THE FUNCTION OF THIS RL2064.2
001500*            PROGRAM IS TO CREATE A RELATIVE FILE SEQUENTIALLY    RL2064.2
001600*            (ACCESS MODE SEQUENTIAL) AND VERIFY THAT IT WAS      RL2064.2
001700*            CREATED CORRECTLY.  THE FILE IS IDENTIFED AS "RL-FS1"RL2064.2
001800*            AND IS PASSED TO SUBSEQUENT RUN UNITS FOR PROCESSING.RL2064.2
001900*                                                                 RL2064.2
002000*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL2064.2
002100*            PROGRAM ARE:                                         RL2064.2
002200*                                                                 RL2064.2
002300*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL2064.2
002400*                         RELATIVE  I-O DATA FILE                 RL2064.2
002500*                 X-55   SYSTEM PRINTER                           RL2064.2
002600*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL2064.2
002700*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL2064.2
002800*                 X-75   OBJECT OF VALUE OF CLAUSE                RL2064.2
002900*                 X-82   SOURCE-COMPUTER                          RL2064.2
003000*                 X-83   OBJECT-COMPUTER.                         RL2064.2
003100*                                                                 RL2064.2
003200****************************************************************  RL2064.2
003300 ENVIRONMENT DIVISION.                                            RL2064.2
003400 CONFIGURATION SECTION.                                           RL2064.2
003500 SOURCE-COMPUTER.                                                 RL2064.2
003600     XXXXX082.                                                    RL2064.2
003700 OBJECT-COMPUTER.                                                 RL2064.2
003800     XXXXX083.                                                    RL2064.2
003900 INPUT-OUTPUT SECTION.                                            RL2064.2
004000 FILE-CONTROL.                                                    RL2064.2
004100     SELECT PRINT-FILE ASSIGN TO                                  RL2064.2
004200     XXXXX055.                                                    RL2064.2
004300     SELECT   RL-FS1 ASSIGN TO                                    RL2064.2
004400     XXXXP021                                                     RL2064.2
004500             ORGANIZATION IS RELATIVE.                            RL2064.2
004600*    ABSENCE OF THE ACCESS CLAUSE IS TREATED AS THOUGH            RL2064.2
004700*     SEQUENTIAL HAD BEEN SPECIFIED.                              RL2064.2
004800 DATA DIVISION.                                                   RL2064.2
004900 FILE SECTION.                                                    RL2064.2
005000 FD  PRINT-FILE.                                                  RL2064.2
005100 01  PRINT-REC PICTURE X(120).                                    RL2064.2
005200 01  DUMMY-RECORD PICTURE X(120).                                 RL2064.2
005300 FD  RL-FS1                                                       RL2064.2
005400     LABEL RECORDS STANDARD                                       RL2064.2
005500C    VALUE OF                                                     RL2064.2
005600C    XXXXX074                                                     RL2064.2
005700C    IS                                                           RL2064.2
005800C    XXXXX075                                                     RL2064.2
005900*    XXXXX069                                                     RL2064.2
006000     BLOCK CONTAINS 1 RECORDS                                     RL2064.2
006100     RECORD IS VARYING IN SIZE                                    RL2064.2
006200     FROM 120 TO 140 CHARACTERS                                   RL2064.2
006300     DEPENDING ON WRK-SIZE.                                       RL2064.2
006400 01  RL-FS1R1-F-G-140.                                            RL2064.2
006500     02 FILLER PIC X(140).                                        RL2064.2
006600 WORKING-STORAGE SECTION.                                         RL2064.2
006700 01  WRK-SIZE     PIC 9(3).                                       RL2064.2
006800 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                RL2064.2
006900 01  FILE-RECORD-INFORMATION-REC.                                 RL2064.2
007000     03 FILE-RECORD-INFO-SKELETON.                                RL2064.2
007100        05 FILLER                 PICTURE X(48)       VALUE       RL2064.2
007200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2064.2
007300        05 FILLER                 PICTURE X(46)       VALUE       RL2064.2
007400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2064.2
007500        05 FILLER                 PICTURE X(26)       VALUE       RL2064.2
007600             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2064.2
007700        05 FILLER                 PICTURE X(37)       VALUE       RL2064.2
007800             ",RECKEY=                             ".             RL2064.2
007900        05 FILLER                 PICTURE X(38)       VALUE       RL2064.2
008000             ",ALTKEY1=                             ".            RL2064.2
008100        05 FILLER                 PICTURE X(38)       VALUE       RL2064.2
008200             ",ALTKEY2=                             ".            RL2064.2
008300        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2064.2
008400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2064.2
008500        05 FILE-RECORD-INFO-P1-120.                               RL2064.2
008600           07 FILLER              PIC X(5).                       RL2064.2
008700           07 XFILE-NAME           PIC X(6).                      RL2064.2
008800           07 FILLER              PIC X(8).                       RL2064.2
008900           07 XRECORD-NAME         PIC X(6).                      RL2064.2
009000           07 FILLER              PIC X(1).                       RL2064.2
009100           07 REELUNIT-NUMBER     PIC 9(1).                       RL2064.2
009200           07 FILLER              PIC X(7).                       RL2064.2
009300           07 XRECORD-NUMBER       PIC 9(6).                      RL2064.2
009400           07 FILLER              PIC X(6).                       RL2064.2
009500           07 UPDATE-NUMBER       PIC 9(2).                       RL2064.2
009600           07 FILLER              PIC X(5).                       RL2064.2
009700           07 ODO-NUMBER          PIC 9(4).                       RL2064.2
009800           07 FILLER              PIC X(5).                       RL2064.2
009900           07 XPROGRAM-NAME        PIC X(5).                      RL2064.2
010000           07 FILLER              PIC X(7).                       RL2064.2
010100           07 XRECORD-LENGTH       PIC 9(6).                      RL2064.2
010200           07 FILLER              PIC X(7).                       RL2064.2
010300           07 CHARS-OR-RECORDS    PIC X(2).                       RL2064.2
010400           07 FILLER              PIC X(1).                       RL2064.2
010500           07 XBLOCK-SIZE          PIC 9(4).                      RL2064.2
010600           07 FILLER              PIC X(6).                       RL2064.2
010700           07 RECORDS-IN-FILE     PIC 9(6).                       RL2064.2
010800           07 FILLER              PIC X(5).                       RL2064.2
010900           07 XFILE-ORGANIZATION   PIC X(2).                      RL2064.2
011000           07 FILLER              PIC X(6).                       RL2064.2
011100           07 XLABEL-TYPE          PIC X(1).                      RL2064.2
011200        05 FILE-RECORD-INFO-P121-240.                             RL2064.2
011300           07 FILLER              PIC X(8).                       RL2064.2
011400           07 XRECORD-KEY          PIC X(29).                     RL2064.2
011500           07 FILLER              PIC X(9).                       RL2064.2
011600           07 ALTERNATE-KEY1      PIC X(29).                      RL2064.2
011700           07 FILLER              PIC X(9).                       RL2064.2
011800           07 ALTERNATE-KEY2      PIC X(29).                      RL2064.2
011900           07 FILLER              PIC X(7).                       RL2064.2
012000 01  NEW-140-CHAR-AREA.                                           RL2064.2
012100   03        FILLER               PIC X(120).                     RL2064.2
012200   03        EXTRA-20-CHARS       PIC X(20).                      RL2064.2
012300                                                                  RL2064.2
012400 01  TEST-RESULTS.                                                RL2064.2
012500     02 FILLER                   PIC X      VALUE SPACE.          RL2064.2
012600     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2064.2
012700     02 FILLER                   PIC X      VALUE SPACE.          RL2064.2
012800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2064.2
012900     02 FILLER                   PIC X      VALUE SPACE.          RL2064.2
013000     02  PAR-NAME.                                                RL2064.2
013100       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2064.2
013200       03  PARDOT-X              PIC X      VALUE SPACE.          RL2064.2
013300       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2064.2
013400     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2064.2
013500     02 RE-MARK                  PIC X(61).                       RL2064.2
013600 01  TEST-COMPUTED.                                               RL2064.2
013700     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2064.2
013800     02 FILLER                   PIC X(17)  VALUE                 RL2064.2
013900            "       COMPUTED=".                                   RL2064.2
014000     02 COMPUTED-X.                                               RL2064.2
014100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2064.2
014200     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2064.2
014300                                 PIC -9(9).9(9).                  RL2064.2
014400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2064.2
014500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2064.2
014600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2064.2
014700     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2064.2
014800         04 COMPUTED-18V0                    PIC -9(18).          RL2064.2
014900         04 FILLER                           PIC X.               RL2064.2
015000     03 FILLER PIC X(50) VALUE SPACE.                             RL2064.2
015100 01  TEST-CORRECT.                                                RL2064.2
015200     02 FILLER PIC X(30) VALUE SPACE.                             RL2064.2
015300     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2064.2
015400     02 CORRECT-X.                                                RL2064.2
015500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2064.2
015600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2064.2
015700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2064.2
015800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2064.2
015900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2064.2
016000     03      CR-18V0 REDEFINES CORRECT-A.                         RL2064.2
016100         04 CORRECT-18V0                     PIC -9(18).          RL2064.2
016200         04 FILLER                           PIC X.               RL2064.2
016300     03 FILLER PIC X(2) VALUE SPACE.                              RL2064.2
016400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2064.2
016500 01  CCVS-C-1.                                                    RL2064.2
016600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2064.2
016700-    "SS  PARAGRAPH-NAME                                          RL2064.2
016800-    "       REMARKS".                                            RL2064.2
016900     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2064.2
017000 01  CCVS-C-2.                                                    RL2064.2
017100     02 FILLER                     PIC X        VALUE SPACE.      RL2064.2
017200     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2064.2
017300     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2064.2
017400     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2064.2
017500     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2064.2
017600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2064.2
017700 01  REC-CT                        PIC 99       VALUE ZERO.       RL2064.2
017800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2064.2
017900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2064.2
018000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2064.2
018100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2064.2
018200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2064.2
018300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2064.2
018400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2064.2
018500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2064.2
018600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2064.2
018700 01  CCVS-H-1.                                                    RL2064.2
018800     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2064.2
018900     02  FILLER                    PIC X(42)    VALUE             RL2064.2
019000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2064.2
019100     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2064.2
019200 01  CCVS-H-2A.                                                   RL2064.2
019300   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2064.2
019400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2064.2
019500   02  FILLER                        PIC XXXX   VALUE             RL2064.2
019600     "4.2 ".                                                      RL2064.2
019700   02  FILLER                        PIC X(28)  VALUE             RL2064.2
019800            " COPY - NOT FOR DISTRIBUTION".                       RL2064.2
019900   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2064.2
020000                                                                  RL2064.2
020100 01  CCVS-H-2B.                                                   RL2064.2
020200   02  FILLER                        PIC X(15)  VALUE             RL2064.2
020300            "TEST RESULT OF ".                                    RL2064.2
020400   02  TEST-ID                       PIC X(9).                    RL2064.2
020500   02  FILLER                        PIC X(4)   VALUE             RL2064.2
020600            " IN ".                                               RL2064.2
020700   02  FILLER                        PIC X(12)  VALUE             RL2064.2
020800     " HIGH       ".                                              RL2064.2
020900   02  FILLER                        PIC X(22)  VALUE             RL2064.2
021000            " LEVEL VALIDATION FOR ".                             RL2064.2
021100   02  FILLER                        PIC X(58)  VALUE             RL2064.2
021200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2064.2
021300 01  CCVS-H-3.                                                    RL2064.2
021400     02  FILLER                      PIC X(34)  VALUE             RL2064.2
021500            " FOR OFFICIAL USE ONLY    ".                         RL2064.2
021600     02  FILLER                      PIC X(58)  VALUE             RL2064.2
021700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2064.2
021800     02  FILLER                      PIC X(28)  VALUE             RL2064.2
021900            "  COPYRIGHT   1985 ".                                RL2064.2
022000 01  CCVS-E-1.                                                    RL2064.2
022100     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2064.2
022200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2064.2
022300     02 ID-AGAIN                     PIC X(9).                    RL2064.2
022400     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2064.2
022500 01  CCVS-E-2.                                                    RL2064.2
022600     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2064.2
022700     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2064.2
022800     02 CCVS-E-2-2.                                               RL2064.2
022900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2064.2
023000         03 FILLER                   PIC X      VALUE SPACE.      RL2064.2
023100         03 ENDER-DESC               PIC X(44)  VALUE             RL2064.2
023200            "ERRORS ENCOUNTERED".                                 RL2064.2
023300 01  CCVS-E-3.                                                    RL2064.2
023400     02  FILLER                      PIC X(22)  VALUE             RL2064.2
023500            " FOR OFFICIAL USE ONLY".                             RL2064.2
023600     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2064.2
023700     02  FILLER                      PIC X(58)  VALUE             RL2064.2
023800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2064.2
023900     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2064.2
024000     02 FILLER                       PIC X(15)  VALUE             RL2064.2
024100             " COPYRIGHT 1985".                                   RL2064.2
024200 01  CCVS-E-4.                                                    RL2064.2
024300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2064.2
024400     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2064.2
024500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2064.2
024600     02 FILLER                       PIC X(40)  VALUE             RL2064.2
024700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2064.2
024800 01  XXINFO.                                                      RL2064.2
024900     02 FILLER                       PIC X(19)  VALUE             RL2064.2
025000            "*** INFORMATION ***".                                RL2064.2
025100     02 INFO-TEXT.                                                RL2064.2
025200       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2064.2
025300       04 XXCOMPUTED                 PIC X(20).                   RL2064.2
025400       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2064.2
025500       04 XXCORRECT                  PIC X(20).                   RL2064.2
025600     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2064.2
025700 01  HYPHEN-LINE.                                                 RL2064.2
025800     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2064.2
025900     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2064.2
026000-    "*****************************************".                 RL2064.2
026100     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2064.2
026200-    "******************************".                            RL2064.2
026300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2064.2
026400     "RL206A".                                                    RL2064.2
026500 PROCEDURE DIVISION.                                              RL2064.2
026600 CCVS1 SECTION.                                                   RL2064.2
026700 OPEN-FILES.                                                      RL2064.2
026800     OPEN    OUTPUT PRINT-FILE.                                   RL2064.2
026900     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2064.2
027000     MOVE    SPACE TO TEST-RESULTS.                               RL2064.2
027100     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2064.2
027200     MOVE    ZERO TO REC-SKL-SUB.                                 RL2064.2
027300     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2064.2
027400 CCVS-INIT-FILE.                                                  RL2064.2
027500     ADD     1 TO REC-SKL-SUB.                                    RL2064.2
027600     MOVE    FILE-RECORD-INFO-SKELETON                            RL2064.2
027700          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2064.2
027800 CCVS-INIT-EXIT.                                                  RL2064.2
027900     GO TO CCVS1-EXIT.                                            RL2064.2
028000 CLOSE-FILES.                                                     RL2064.2
028100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2064.2
028200 TERMINATE-CCVS.                                                  RL2064.2
028300*    EXIT PROGRAM.                                                RL2064.2
028400*TERMINATE-CALL.                                                  RL2064.2
028500     STOP     RUN.                                                RL2064.2
028600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2064.2
028700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2064.2
028800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2064.2
028900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2064.2
029000     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2064.2
029100 PRINT-DETAIL.                                                    RL2064.2
029200     IF REC-CT NOT EQUAL TO ZERO                                  RL2064.2
029300             MOVE "." TO PARDOT-X                                 RL2064.2
029400             MOVE REC-CT TO DOTVALUE.                             RL2064.2
029500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2064.2
029600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2064.2
029700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2064.2
029800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2064.2
029900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2064.2
030000     MOVE SPACE TO CORRECT-X.                                     RL2064.2
030100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2064.2
030200     MOVE     SPACE TO RE-MARK.                                   RL2064.2
030300 HEAD-ROUTINE.                                                    RL2064.2
030400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2064.2
030500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2064.2
030600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2064.2
030700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2064.2
030800 COLUMN-NAMES-ROUTINE.                                            RL2064.2
030900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2064.2
031000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2064.2
031100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2064.2
031200 END-ROUTINE.                                                     RL2064.2
031300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2064.2
031400 END-RTN-EXIT.                                                    RL2064.2
031500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2064.2
031600 END-ROUTINE-1.                                                   RL2064.2
031700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2064.2
031800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2064.2
031900      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2064.2
032000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2064.2
032100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2064.2
032200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2064.2
032300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2064.2
032400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2064.2
032500  END-ROUTINE-12.                                                 RL2064.2
032600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2064.2
032700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2064.2
032800         MOVE "NO " TO ERROR-TOTAL                                RL2064.2
032900         ELSE                                                     RL2064.2
033000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2064.2
033100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2064.2
033200     PERFORM WRITE-LINE.                                          RL2064.2
033300 END-ROUTINE-13.                                                  RL2064.2
033400     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2064.2
033500         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2064.2
033600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2064.2
033700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2064.2
033800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2064.2
033900      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2064.2
034000          MOVE "NO " TO ERROR-TOTAL                               RL2064.2
034100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2064.2
034200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2064.2
034300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2064.2
034400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2064.2
034500 WRITE-LINE.                                                      RL2064.2
034600     ADD 1 TO RECORD-COUNT.                                       RL2064.2
034700*    IF RECORD-COUNT GREATER 50                                   RL2064.2
034800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2064.2
034900*        MOVE SPACE TO DUMMY-RECORD                               RL2064.2
035000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2064.2
035100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2064.2
035200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2064.2
035300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2064.2
035400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2064.2
035500*        MOVE ZERO TO RECORD-COUNT.                               RL2064.2
035600     PERFORM WRT-LN.                                              RL2064.2
035700 WRT-LN.                                                          RL2064.2
035800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2064.2
035900     MOVE SPACE TO DUMMY-RECORD.                                  RL2064.2
036000 BLANK-LINE-PRINT.                                                RL2064.2
036100     PERFORM WRT-LN.                                              RL2064.2
036200 FAIL-ROUTINE.                                                    RL2064.2
036300     IF   COMPUTED-X NOT EQUAL TO SPACE                           RL2064.2
036400          GO TO FAIL-ROUTINE-WRITE.                               RL2064.2
036500     IF   CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.  RL2064.2
036600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2064.2
036700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2064.2
036800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2064.2
036900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2064.2
037000     GO TO  FAIL-ROUTINE-EX.                                      RL2064.2
037100 FAIL-ROUTINE-WRITE.                                              RL2064.2
037200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2064.2
037300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2064.2
037400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2064.2
037500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2064.2
037600 FAIL-ROUTINE-EX. EXIT.                                           RL2064.2
037700 BAIL-OUT.                                                        RL2064.2
037800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2064.2
037900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2064.2
038000 BAIL-OUT-WRITE.                                                  RL2064.2
038100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2064.2
038200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2064.2
038300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2064.2
038400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2064.2
038500 BAIL-OUT-EX. EXIT.                                               RL2064.2
038600 CCVS1-EXIT.                                                      RL2064.2
038700     EXIT.                                                        RL2064.2
038800 SECT-RL206A-001 SECTION.                                         RL2064.2
038900 REL-INIT-001.                                                    RL2064.2
039000     MOVE     "FILE CREATE RL-FS1" TO FEATURE.                    RL2064.2
039100     OPEN     OUTPUT    RL-FS1.                                   RL2064.2
039200     MOVE     "RL-FS1" TO XFILE-NAME (1).                         RL2064.2
039300     MOVE     "R1-F-G" TO XRECORD-NAME (1).                       RL2064.2
039400     MOVE CCVS-PGM-ID      TO XPROGRAM-NAME (1).                  RL2064.2
039500     MOVE     000120   TO XRECORD-LENGTH (1).                     RL2064.2
039600     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   RL2064.2
039700     MOVE     0001     TO XBLOCK-SIZE (1).                        RL2064.2
039800     MOVE     000500   TO RECORDS-IN-FILE (1).                    RL2064.2
039900     MOVE     "RL"     TO XFILE-ORGANIZATION (1).                 RL2064.2
040000     MOVE     "S"      TO XLABEL-TYPE (1).                        RL2064.2
040100     MOVE    000001    TO XRECORD-NUMBER (1).                     RL2064.2
040200 REL-TEST-001.                                                    RL2064.2
040300     MOVE    FILE-RECORD-INFO-P1-120 (1) TO NEW-140-CHAR-AREA.    RL2064.2
040400     MOVE   "ABCDEFGHIJKLMNOPQRST"       TO EXTRA-20-CHARS.       RL2064.2
040500     MOVE    NEW-140-CHAR-AREA           TO RL-FS1R1-F-G-140.     RL2064.2
040600     IF      XRECORD-NUMBER (1) > 32                              RL2064.2
040700             MOVE   140 TO WRK-SIZE.                              RL2064.2
040800     IF      XRECORD-NUMBER (1) = 32                              RL2064.2
040900             MOVE   135 TO WRK-SIZE.                              RL2064.2
041000     IF      XRECORD-NUMBER (1) = 31                              RL2064.2
041100             MOVE   125 TO WRK-SIZE.                              RL2064.2
041200     IF      XRECORD-NUMBER (1) < 31                              RL2064.2
041300             MOVE   140 TO WRK-SIZE.                              RL2064.2
041400     IF      XRECORD-NUMBER (1) < 21                              RL2064.2
041500             MOVE   130 TO WRK-SIZE.                              RL2064.2
041600     IF      XRECORD-NUMBER (1) < 11                              RL2064.2
041700             MOVE   120 TO WRK-SIZE.                              RL2064.2
041800     WRITE   RL-FS1R1-F-G-140                                     RL2064.2
041900             INVALID KEY GO TO REL-FAIL-001.                      RL2064.2
042000     IF      XRECORD-NUMBER (1) EQUAL TO 500                      RL2064.2
042100             GO TO REL-WRITE-001.                                 RL2064.2
042200     ADD     000001 TO XRECORD-NUMBER (1).                        RL2064.2
042300     GO      TO REL-TEST-001.                                     RL2064.2
042400 REL-DELETE-001.                                                  RL2064.2
042500     PERFORM DE-LETE.                                             RL2064.2
042600     GO TO  REL-WRITE-001.                                        RL2064.2
042700 REL-FAIL-001.                                                    RL2064.2
042800     PERFORM FAIL.                                                RL2064.2
042900     MOVE   "BOUNDARY VIOLATION"  TO RE-MARK.                     RL2064.2
043000 REL-WRITE-001.                                                   RL2064.2
043100     MOVE   "REL-TEST-001" TO   PAR-NAME                          RL2064.2
043200     MOVE   "FILE CREATED, LFILE "  TO COMPUTED-A.                RL2064.2
043300     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2064.2
043400     PERFORM PRINT-DETAIL.                                        RL2064.2
043500     CLOSE   RL-FS1.                                              RL2064.2
043600 REL-INIT-002.                                                    RL2064.2
043700     OPEN    INPUT     RL-FS1.                                    RL2064.2
043800     MOVE    ZERO      TO WRK-CS-09V00.                           RL2064.2
043900 REL-TEST-002.                                                    RL2064.2
044000     READ    RL-FS1 INTO NEW-140-CHAR-AREA                        RL2064.2
044100             AT END GO TO REL-TEST-002-1.                         RL2064.2
044200     ADD     1 TO WRK-CS-09V00.                                   RL2064.2
044300     IF      WRK-CS-09V00 GREATER 500                             RL2064.2
044400             MOVE "MORE THAN 500 RECORDS" TO RE-MARK              RL2064.2
044500             GO TO REL-TEST-002-1.                                RL2064.2
044600     PERFORM SIZE-TEST-1.                                         RL2064.2
044700     GO      TO REL-TEST-002.                                     RL2064.2
044800 REL-DELETE-002.                                                  RL2064.2
044900     PERFORM DE-LETE.                                             RL2064.2
045000     PERFORM PRINT-DETAIL.                                        RL2064.2
045100 REL-TEST-002-1.                                                  RL2064.2
045200     IF      XRECORD-NUMBER (1) NOT EQUAL TO 500                  RL2064.2
045300             PERFORM FAIL                                         RL2064.2
045400     ELSE                                                         RL2064.2
045500             PERFORM PASS.                                        RL2064.2
045600     GO      TO REL-WRITE-002.                                    RL2064.2
045700 REL-WRITE-002.                                                   RL2064.2
045800     MOVE   "REL-TEST-002" TO PAR-NAME.                           RL2064.2
045900     MOVE   "FILE VERIFIED, LFILE" TO COMPUTED-A.                 RL2064.2
046000     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2064.2
046100     PERFORM PRINT-DETAIL.                                        RL2064.2
046200     CLOSE   RL-FS1.                                              RL2064.2
046300     GO TO   CCVS-EXIT.                                           RL2064.2
046400 SIZE-TEST-1 SECTION.                                             RL2064.2
046500*===================                                              RL2064.2
046600 REL-SIZE-TEST-1.                                                 RL2064.2
046700     IF      WRK-CS-09V00 > 32                                    RL2064.2
046800             PERFORM SIZE-TEST-2.                                 RL2064.2
046900     IF      WRK-CS-09V00 = 32                                    RL2064.2
047000             PERFORM SIZE-TEST-3.                                 RL2064.2
047100     IF      WRK-CS-09V00 = 31                                    RL2064.2
047200             PERFORM SIZE-TEST-4.                                 RL2064.2
047300     IF      WRK-CS-09V00 < 11                                    RL2064.2
047400             PERFORM SIZE-TEST-7                                  RL2064.2
047500             GO TO   SIZE-TEST-1-EXIT.                            RL2064.2
047600     IF      WRK-CS-09V00 < 21                                    RL2064.2
047700             PERFORM SIZE-TEST-6                                  RL2064.2
047800             GO TO   SIZE-TEST-1-EXIT.                            RL2064.2
047900     IF      WRK-CS-09V00 < 31                                    RL2064.2
048000             PERFORM SIZE-TEST-5.                                 RL2064.2
048100 SIZE-TEST-1-EXIT.                                                RL2064.2
048200     EXIT.                                                        RL2064.2
048300 SIZE-TEST-2 SECTION.                                             RL2064.2
048400 REL-SIZE-TEST-2.                                                 RL2064.2
048500     IF      WRK-SIZE NOT = 140                                   RL2064.2
048600             MOVE  "WRONG LENGTH RECORD" TO RE-MARK               RL2064.2
048700             MOVE   WRK-SIZE TO COMPUTED-18V0                     RL2064.2
048800             MOVE   140      TO CORRECT-18V0                      RL2064.2
048900             PERFORM FAIL                                         RL2064.2
049000             PERFORM PRINT-DETAIL                                 RL2064.2
049100     ELSE                                                         RL2064.2
049200             PERFORM PASS.                                        RL2064.2
049300*            PERFORM PRINT-DETAIL.                                RL2064.2
049400 SIZE-TEST-2-EXIT.                                                RL2064.2
049500     EXIT.                                                        RL2064.2
049600 SIZE-TEST-3 SECTION.                                             RL2064.2
049700 REL-SIZE-TEST-3.                                                 RL2064.2
049800     IF      WRK-SIZE NOT = 135                                   RL2064.2
049900             MOVE  "WRONG LENGTH RECORD" TO RE-MARK               RL2064.2
050000             MOVE   WRK-SIZE TO COMPUTED-18V0                     RL2064.2
050100             MOVE   135      TO CORRECT-18V0                      RL2064.2
050200             PERFORM FAIL                                         RL2064.2
050300             PERFORM PRINT-DETAIL                                 RL2064.2
050400     ELSE                                                         RL2064.2
050500             PERFORM PASS.                                        RL2064.2
050600*            PERFORM PRINT-DETAIL.                                RL2064.2
050700 SIZE-TEST-3-EXIT.                                                RL2064.2
050800     EXIT.                                                        RL2064.2
050900 SIZE-TEST-4 SECTION.                                             RL2064.2
051000 REL-SIZE-TEST-4.                                                 RL2064.2
051100     IF      WRK-SIZE NOT = 125                                   RL2064.2
051200             MOVE  "WRONG LENGTH RECORD" TO RE-MARK               RL2064.2
051300             MOVE   WRK-SIZE TO COMPUTED-18V0                     RL2064.2
051400             MOVE   125      TO CORRECT-18V0                      RL2064.2
051500             PERFORM FAIL                                         RL2064.2
051600             PERFORM PRINT-DETAIL                                 RL2064.2
051700     ELSE                                                         RL2064.2
051800             PERFORM PASS.                                        RL2064.2
051900*            PERFORM PRINT-DETAIL.                                RL2064.2
052000 SIZE-TEST-4-EXIT.                                                RL2064.2
052100     EXIT.                                                        RL2064.2
052200 SIZE-TEST-5 SECTION.                                             RL2064.2
052300 REL-SIZE-TEST-5.                                                 RL2064.2
052400     IF      WRK-SIZE NOT = 140                                   RL2064.2
052500             MOVE  "WRONG LENGTH RECORD" TO RE-MARK               RL2064.2
052600             MOVE   WRK-SIZE TO COMPUTED-18V0                     RL2064.2
052700             MOVE   140      TO CORRECT-18V0                      RL2064.2
052800             PERFORM FAIL                                         RL2064.2
052900             PERFORM PRINT-DETAIL                                 RL2064.2
053000     ELSE                                                         RL2064.2
053100             PERFORM PASS.                                        RL2064.2
053200*            PERFORM PRINT-DETAIL.                                RL2064.2
053300 SIZE-TEST-5-EXIT.                                                RL2064.2
053400     EXIT.                                                        RL2064.2
053500 SIZE-TEST-6 SECTION.                                             RL2064.2
053600 REL-SIZE-TEST-6.                                                 RL2064.2
053700     IF      WRK-SIZE NOT = 130                                   RL2064.2
053800             MOVE  "WRONG LENGTH RECORD" TO RE-MARK               RL2064.2
053900             MOVE   WRK-SIZE TO COMPUTED-18V0                     RL2064.2
054000             MOVE   130      TO CORRECT-18V0                      RL2064.2
054100             PERFORM FAIL                                         RL2064.2
054200             PERFORM PRINT-DETAIL                                 RL2064.2
054300     ELSE                                                         RL2064.2
054400             PERFORM PASS.                                        RL2064.2
054500*            PERFORM PRINT-DETAIL.                                RL2064.2
054600 SIZE-TEST-6-EXIT.                                                RL2064.2
054700     EXIT.                                                        RL2064.2
054800 SIZE-TEST-7 SECTION.                                             RL2064.2
054900 REL-SIZE-TEST-7.                                                 RL2064.2
055000     IF      WRK-SIZE NOT = 120                                   RL2064.2
055100             MOVE  "WRONG LENGTH RECORD" TO RE-MARK               RL2064.2
055200             MOVE   WRK-SIZE TO COMPUTED-18V0                     RL2064.2
055300             MOVE   120      TO CORRECT-18V0                      RL2064.2
055400             PERFORM FAIL                                         RL2064.2
055500             PERFORM PRINT-DETAIL                                 RL2064.2
055600     ELSE                                                         RL2064.2
055700             PERFORM PASS.                                        RL2064.2
055800*            PERFORM PRINT-DETAIL.                                RL2064.2
055900 SIZE-TEST-7-EXIT.                                                RL2064.2
056000     EXIT.                                                        RL2064.2
056100 CCVS-EXIT SECTION.                                               RL2064.2
056200 CCVS-999999.                                                     RL2064.2
056300     GO TO CLOSE-FILES.                                           RL2064.2
      *END-OF,RL206A                                                                  
