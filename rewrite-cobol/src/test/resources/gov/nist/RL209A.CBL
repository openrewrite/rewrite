      *HEADER,COBOL,RL209A                                                            
000100 IDENTIFICATION DIVISION.                                         RL2094.2
000200 PROGRAM-ID.                                                      RL2094.2
000300     RL209A.                                                      RL2094.2
000400****************************************************************  RL2094.2
000500*                                                              *  RL2094.2
000600*    VALIDATION FOR:-                                          *  RL2094.2
000700*                                                              *  RL2094.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2094.2
000900*                                                              *  RL2094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2094.2
001100*                                                              *  RL2094.2
001200****************************************************************  RL2094.2
001300*                                                              *  RL2094.2
001400*    THE FUNCTION OF THIS PROGRAM IS TO CREATE A RELATIVE FILE *  RL2094.2
001500*    SEQUENTIALLY WITH VARIABLE LENGTH RECORDS AND VERIFY THAT *  RL2094.2
001600*    IT WAS CREATED CORRECTLY.                                 *  RL2094.2
001700*    THE FILE WILL BE IDENTIFIED AS: "RL-VS1".                 *  RL2094.2
001800*    THE PROGRAM WILL CREATE A RELATIVE FILE OF 500 VARIABLE   *  RL2094.2
001900*    LENGTH RECORDS.                                           *  RL2094.2
002000*    THE RECORD SIZE WILL BE 120 TO 140 CHARACTERS.            *  RL2094.2
002100*                                                              *  RL2094.2
002200****************************************************************  RL2094.2
002300*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL2094.2
002400*            PROGRAM ARE:                                         RL2094.2
002500*                                                                 RL2094.2
002600*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL2094.2
002700*                         RELATIVE  I-O DATA FILE                 RL2094.2
002800*                 X-55   SYSTEM PRINTER                           RL2094.2
002900*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL2094.2
003000*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL2094.2
003100*                 X-75   OBJECT OF VALUE OF CLAUSE                RL2094.2
003200*                 X-82   SOURCE-COMPUTER                          RL2094.2
003300*                 X-83   OBJECT-COMPUTER.                         RL2094.2
003400*                                                                 RL2094.2
003500****************************************************************  RL2094.2
003600 ENVIRONMENT DIVISION.                                            RL2094.2
003700 CONFIGURATION SECTION.                                           RL2094.2
003800 SOURCE-COMPUTER.                                                 RL2094.2
003900     XXXXX082.                                                    RL2094.2
004000 OBJECT-COMPUTER.                                                 RL2094.2
004100     XXXXX083.                                                    RL2094.2
004200 INPUT-OUTPUT SECTION.                                            RL2094.2
004300 FILE-CONTROL.                                                    RL2094.2
004400     SELECT PRINT-FILE ASSIGN TO                                  RL2094.2
004500     XXXXX055.                                                    RL2094.2
004600     SELECT   RL-FS1 ASSIGN TO                                    RL2094.2
004700     XXXXP021                                                     RL2094.2
004800             ORGANIZATION IS RELATIVE.                            RL2094.2
004900*    ABSENCE OF THE ACCESS CLAUSE IS TREATED AS THOUGH            RL2094.2
005000*     SEQUENTIAL HAD BEEN SPECIFIED.                              RL2094.2
005100 DATA DIVISION.                                                   RL2094.2
005200 FILE SECTION.                                                    RL2094.2
005300 FD  PRINT-FILE.                                                  RL2094.2
005400 01  PRINT-REC PICTURE X(120).                                    RL2094.2
005500 01  DUMMY-RECORD PICTURE X(120).                                 RL2094.2
005600 FD  RL-FS1                                                       RL2094.2
005700     LABEL RECORDS STANDARD                                       RL2094.2
005800C    VALUE OF                                                     RL2094.2
005900C    XXXXX074                                                     RL2094.2
006000C    IS                                                           RL2094.2
006100C    XXXXX075                                                     RL2094.2
006200*    XXXXX069                                                     RL2094.2
006300     RECORD IS VARYING IN SIZE                                    RL2094.2
006400     FROM 120 TO 140 CHARACTERS                                   RL2094.2
006500     DEPENDING ON WRK-SIZE.                                       RL2094.2
006600 01  RL-FS1R1-F-G-120.                                            RL2094.2
006700     02 FILLER PIC X(140).                                        RL2094.2
006800 WORKING-STORAGE SECTION.                                         RL2094.2
006900 01  WRK-SIZE     PIC  9(3).                                      RL2094.2
007000 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                RL2094.2
007100 01  FILE-RECORD-INFORMATION-REC.                                 RL2094.2
007200     03 FILE-RECORD-INFO-SKELETON.                                RL2094.2
007300        05 FILLER                 PICTURE X(48)       VALUE       RL2094.2
007400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2094.2
007500        05 FILLER                 PICTURE X(46)       VALUE       RL2094.2
007600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2094.2
007700        05 FILLER                 PICTURE X(26)       VALUE       RL2094.2
007800             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2094.2
007900        05 FILLER                 PICTURE X(37)       VALUE       RL2094.2
008000             ",RECKEY=                             ".             RL2094.2
008100        05 FILLER                 PICTURE X(38)       VALUE       RL2094.2
008200             ",ALTKEY1=                             ".            RL2094.2
008300        05 FILLER                 PICTURE X(38)       VALUE       RL2094.2
008400             ",ALTKEY2=                             ".            RL2094.2
008500        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2094.2
008600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2094.2
008700        05 FILE-RECORD-INFO-P1-120.                               RL2094.2
008800           07 FILLER              PIC X(5).                       RL2094.2
008900           07 XFILE-NAME           PIC X(6).                      RL2094.2
009000           07 FILLER              PIC X(8).                       RL2094.2
009100           07 XRECORD-NAME         PIC X(6).                      RL2094.2
009200           07 FILLER              PIC X(1).                       RL2094.2
009300           07 REELUNIT-NUMBER     PIC 9(1).                       RL2094.2
009400           07 FILLER              PIC X(7).                       RL2094.2
009500           07 XRECORD-NUMBER       PIC 9(6).                      RL2094.2
009600           07 FILLER              PIC X(6).                       RL2094.2
009700           07 UPDATE-NUMBER       PIC 9(2).                       RL2094.2
009800           07 FILLER              PIC X(5).                       RL2094.2
009900           07 ODO-NUMBER          PIC 9(4).                       RL2094.2
010000           07 FILLER              PIC X(5).                       RL2094.2
010100           07 XPROGRAM-NAME        PIC X(5).                      RL2094.2
010200           07 FILLER              PIC X(7).                       RL2094.2
010300           07 XRECORD-LENGTH       PIC 9(6).                      RL2094.2
010400           07 FILLER              PIC X(7).                       RL2094.2
010500           07 CHARS-OR-RECORDS    PIC X(2).                       RL2094.2
010600           07 FILLER              PIC X(1).                       RL2094.2
010700           07 XBLOCK-SIZE          PIC 9(4).                      RL2094.2
010800           07 FILLER              PIC X(6).                       RL2094.2
010900           07 RECORDS-IN-FILE     PIC 9(6).                       RL2094.2
011000           07 FILLER              PIC X(5).                       RL2094.2
011100           07 XFILE-ORGANIZATION   PIC X(2).                      RL2094.2
011200           07 FILLER              PIC X(6).                       RL2094.2
011300           07 XLABEL-TYPE          PIC X(1).                      RL2094.2
011400        05 FILE-RECORD-INFO-P121-240.                             RL2094.2
011500           07 FILLER              PIC X(8).                       RL2094.2
011600           07 XRECORD-KEY          PIC X(29).                     RL2094.2
011700           07 FILLER              PIC X(9).                       RL2094.2
011800           07 ALTERNATE-KEY1      PIC X(29).                      RL2094.2
011900           07 FILLER              PIC X(9).                       RL2094.2
012000           07 ALTERNATE-KEY2      PIC X(29).                      RL2094.2
012100           07 FILLER              PIC X(7).                       RL2094.2
012200 01  TEST-RESULTS.                                                RL2094.2
012300     02 FILLER                   PIC X      VALUE SPACE.          RL2094.2
012400     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2094.2
012500     02 FILLER                   PIC X      VALUE SPACE.          RL2094.2
012600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2094.2
012700     02 FILLER                   PIC X      VALUE SPACE.          RL2094.2
012800     02  PAR-NAME.                                                RL2094.2
012900       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2094.2
013000       03  PARDOT-X              PIC X      VALUE SPACE.          RL2094.2
013100       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2094.2
013200     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2094.2
013300     02 RE-MARK                  PIC X(61).                       RL2094.2
013400 01  TEST-COMPUTED.                                               RL2094.2
013500     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2094.2
013600     02 FILLER                   PIC X(17)  VALUE                 RL2094.2
013700            "       COMPUTED=".                                   RL2094.2
013800     02 COMPUTED-X.                                               RL2094.2
013900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2094.2
014000     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2094.2
014100                                 PIC -9(9).9(9).                  RL2094.2
014200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2094.2
014300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2094.2
014400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2094.2
014500     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2094.2
014600         04 COMPUTED-18V0                    PIC -9(18).          RL2094.2
014700         04 FILLER                           PIC X.               RL2094.2
014800     03 FILLER PIC X(50) VALUE SPACE.                             RL2094.2
014900 01  TEST-CORRECT.                                                RL2094.2
015000     02 FILLER PIC X(30) VALUE SPACE.                             RL2094.2
015100     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2094.2
015200     02 CORRECT-X.                                                RL2094.2
015300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2094.2
015400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2094.2
015500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2094.2
015600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2094.2
015700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2094.2
015800     03      CR-18V0 REDEFINES CORRECT-A.                         RL2094.2
015900         04 CORRECT-18V0                     PIC -9(18).          RL2094.2
016000         04 FILLER                           PIC X.               RL2094.2
016100     03 FILLER PIC X(2) VALUE SPACE.                              RL2094.2
016200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2094.2
016300 01  CCVS-C-1.                                                    RL2094.2
016400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2094.2
016500-    "SS  PARAGRAPH-NAME                                          RL2094.2
016600-    "       REMARKS".                                            RL2094.2
016700     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2094.2
016800 01  CCVS-C-2.                                                    RL2094.2
016900     02 FILLER                     PIC X        VALUE SPACE.      RL2094.2
017000     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2094.2
017100     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2094.2
017200     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2094.2
017300     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2094.2
017400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2094.2
017500 01  REC-CT                        PIC 99       VALUE ZERO.       RL2094.2
017600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2094.2
017700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2094.2
017800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2094.2
017900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2094.2
018000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2094.2
018100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2094.2
018200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2094.2
018300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2094.2
018400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2094.2
018500 01  CCVS-H-1.                                                    RL2094.2
018600     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2094.2
018700     02  FILLER                    PIC X(42)    VALUE             RL2094.2
018800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2094.2
018900     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2094.2
019000 01  CCVS-H-2A.                                                   RL2094.2
019100   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2094.2
019200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2094.2
019300   02  FILLER                        PIC XXXX   VALUE             RL2094.2
019400     "4.2 ".                                                      RL2094.2
019500   02  FILLER                        PIC X(28)  VALUE             RL2094.2
019600            " COPY - NOT FOR DISTRIBUTION".                       RL2094.2
019700   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2094.2
019800                                                                  RL2094.2
019900 01  CCVS-H-2B.                                                   RL2094.2
020000   02  FILLER                        PIC X(15)  VALUE             RL2094.2
020100            "TEST RESULT OF ".                                    RL2094.2
020200   02  TEST-ID                       PIC X(9).                    RL2094.2
020300   02  FILLER                        PIC X(4)   VALUE             RL2094.2
020400            " IN ".                                               RL2094.2
020500   02  FILLER                        PIC X(12)  VALUE             RL2094.2
020600     " HIGH       ".                                              RL2094.2
020700   02  FILLER                        PIC X(22)  VALUE             RL2094.2
020800            " LEVEL VALIDATION FOR ".                             RL2094.2
020900   02  FILLER                        PIC X(58)  VALUE             RL2094.2
021000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2094.2
021100 01  CCVS-H-3.                                                    RL2094.2
021200     02  FILLER                      PIC X(34)  VALUE             RL2094.2
021300            " FOR OFFICIAL USE ONLY    ".                         RL2094.2
021400     02  FILLER                      PIC X(58)  VALUE             RL2094.2
021500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2094.2
021600     02  FILLER                      PIC X(28)  VALUE             RL2094.2
021700            "  COPYRIGHT   1985 ".                                RL2094.2
021800 01  CCVS-E-1.                                                    RL2094.2
021900     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2094.2
022000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2094.2
022100     02 ID-AGAIN                     PIC X(9).                    RL2094.2
022200     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2094.2
022300 01  CCVS-E-2.                                                    RL2094.2
022400     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2094.2
022500     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2094.2
022600     02 CCVS-E-2-2.                                               RL2094.2
022700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2094.2
022800         03 FILLER                   PIC X      VALUE SPACE.      RL2094.2
022900         03 ENDER-DESC               PIC X(44)  VALUE             RL2094.2
023000            "ERRORS ENCOUNTERED".                                 RL2094.2
023100 01  CCVS-E-3.                                                    RL2094.2
023200     02  FILLER                      PIC X(22)  VALUE             RL2094.2
023300            " FOR OFFICIAL USE ONLY".                             RL2094.2
023400     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2094.2
023500     02  FILLER                      PIC X(58)  VALUE             RL2094.2
023600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2094.2
023700     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2094.2
023800     02 FILLER                       PIC X(15)  VALUE             RL2094.2
023900             " COPYRIGHT 1985".                                   RL2094.2
024000 01  CCVS-E-4.                                                    RL2094.2
024100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2094.2
024200     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2094.2
024300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2094.2
024400     02 FILLER                       PIC X(40)  VALUE             RL2094.2
024500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2094.2
024600 01  XXINFO.                                                      RL2094.2
024700     02 FILLER                       PIC X(19)  VALUE             RL2094.2
024800            "*** INFORMATION ***".                                RL2094.2
024900     02 INFO-TEXT.                                                RL2094.2
025000       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2094.2
025100       04 XXCOMPUTED                 PIC X(20).                   RL2094.2
025200       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2094.2
025300       04 XXCORRECT                  PIC X(20).                   RL2094.2
025400     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2094.2
025500 01  HYPHEN-LINE.                                                 RL2094.2
025600     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2094.2
025700     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2094.2
025800-    "*****************************************".                 RL2094.2
025900     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2094.2
026000-    "******************************".                            RL2094.2
026100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2094.2
026200     "RL209A".                                                    RL2094.2
026300 PROCEDURE DIVISION.                                              RL2094.2
026400 CCVS1 SECTION.                                                   RL2094.2
026500 OPEN-FILES.                                                      RL2094.2
026600     OPEN    OUTPUT PRINT-FILE.                                   RL2094.2
026700     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2094.2
026800     MOVE    SPACE TO TEST-RESULTS.                               RL2094.2
026900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2094.2
027000     MOVE    ZERO TO REC-SKL-SUB.                                 RL2094.2
027100     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2094.2
027200 CCVS-INIT-FILE.                                                  RL2094.2
027300     ADD     1 TO REC-SKL-SUB.                                    RL2094.2
027400     MOVE    FILE-RECORD-INFO-SKELETON                            RL2094.2
027500          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2094.2
027600 CCVS-INIT-EXIT.                                                  RL2094.2
027700     GO TO CCVS1-EXIT.                                            RL2094.2
027800 CLOSE-FILES.                                                     RL2094.2
027900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2094.2
028000 TERMINATE-CCVS.                                                  RL2094.2
028100*    EXIT PROGRAM.                                                RL2094.2
028200*TERMINATE-CALL.                                                  RL2094.2
028300     STOP     RUN.                                                RL2094.2
028400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2094.2
028500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2094.2
028600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2094.2
028700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2094.2
028800     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2094.2
028900 PRINT-DETAIL.                                                    RL2094.2
029000     IF REC-CT NOT EQUAL TO ZERO                                  RL2094.2
029100             MOVE "." TO PARDOT-X                                 RL2094.2
029200             MOVE REC-CT TO DOTVALUE.                             RL2094.2
029300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2094.2
029400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2094.2
029500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2094.2
029600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2094.2
029700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2094.2
029800     MOVE SPACE TO CORRECT-X.                                     RL2094.2
029900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2094.2
030000     MOVE     SPACE TO RE-MARK.                                   RL2094.2
030100 HEAD-ROUTINE.                                                    RL2094.2
030200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2094.2
030300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2094.2
030400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2094.2
030500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2094.2
030600 COLUMN-NAMES-ROUTINE.                                            RL2094.2
030700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2094.2
030800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2094.2
030900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2094.2
031000 END-ROUTINE.                                                     RL2094.2
031100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2094.2
031200 END-RTN-EXIT.                                                    RL2094.2
031300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2094.2
031400 END-ROUTINE-1.                                                   RL2094.2
031500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2094.2
031600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2094.2
031700      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2094.2
031800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2094.2
031900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2094.2
032000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2094.2
032100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2094.2
032200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2094.2
032300  END-ROUTINE-12.                                                 RL2094.2
032400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2094.2
032500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2094.2
032600         MOVE "NO " TO ERROR-TOTAL                                RL2094.2
032700         ELSE                                                     RL2094.2
032800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2094.2
032900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2094.2
033000     PERFORM WRITE-LINE.                                          RL2094.2
033100 END-ROUTINE-13.                                                  RL2094.2
033200     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2094.2
033300         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2094.2
033400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2094.2
033500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2094.2
033600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2094.2
033700      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2094.2
033800          MOVE "NO " TO ERROR-TOTAL                               RL2094.2
033900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2094.2
034000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2094.2
034100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2094.2
034200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2094.2
034300 WRITE-LINE.                                                      RL2094.2
034400     ADD 1 TO RECORD-COUNT.                                       RL2094.2
034500*    IF RECORD-COUNT GREATER 50                                   RL2094.2
034600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2094.2
034700*        MOVE SPACE TO DUMMY-RECORD                               RL2094.2
034800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2094.2
034900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2094.2
035000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2094.2
035100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2094.2
035200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2094.2
035300*        MOVE ZERO TO RECORD-COUNT.                               RL2094.2
035400     PERFORM WRT-LN.                                              RL2094.2
035500 WRT-LN.                                                          RL2094.2
035600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2094.2
035700     MOVE SPACE TO DUMMY-RECORD.                                  RL2094.2
035800 BLANK-LINE-PRINT.                                                RL2094.2
035900     PERFORM WRT-LN.                                              RL2094.2
036000 FAIL-ROUTINE.                                                    RL2094.2
036100     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2094.2
036200            GO TO   FAIL-ROUTINE-WRITE.                           RL2094.2
036300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2094.2
036400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2094.2
036500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2094.2
036600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2094.2
036700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2094.2
036800     GO TO  FAIL-ROUTINE-EX.                                      RL2094.2
036900 FAIL-ROUTINE-WRITE.                                              RL2094.2
037000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2094.2
037100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2094.2
037200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2094.2
037300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2094.2
037400 FAIL-ROUTINE-EX. EXIT.                                           RL2094.2
037500 BAIL-OUT.                                                        RL2094.2
037600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2094.2
037700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2094.2
037800 BAIL-OUT-WRITE.                                                  RL2094.2
037900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2094.2
038000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2094.2
038100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2094.2
038200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2094.2
038300 BAIL-OUT-EX. EXIT.                                               RL2094.2
038400 CCVS1-EXIT.                                                      RL2094.2
038500     EXIT.                                                        RL2094.2
038600 SECT-RL201-001 SECTION.                                          RL2094.2
038700 REL-INIT-001.                                                    RL2094.2
038800     MOVE     "FILE CREATE RL-FS1" TO FEATURE.                    RL2094.2
038900     OPEN     OUTPUT    RL-FS1.                                   RL2094.2
039000     MOVE     "RL-FS1" TO XFILE-NAME (1).                         RL2094.2
039100     MOVE     "R1-F-G" TO XRECORD-NAME (1).                       RL2094.2
039200     MOVE CCVS-PGM-ID  TO XPROGRAM-NAME (1).                      RL2094.2
039300     MOVE     000120   TO XRECORD-LENGTH (1).                     RL2094.2
039400     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   RL2094.2
039500     MOVE     0001     TO XBLOCK-SIZE (1).                        RL2094.2
039600     MOVE     000500   TO RECORDS-IN-FILE (1).                    RL2094.2
039700     MOVE     "RL"     TO XFILE-ORGANIZATION (1).                 RL2094.2
039800     MOVE     "S"      TO XLABEL-TYPE (1).                        RL2094.2
039900     MOVE     000001   TO XRECORD-NUMBER (1).                     RL2094.2
040000 REL-TEST-001.                                                    RL2094.2
040100     MOVE     120 TO WRK-SIZE.                                    RL2094.2
040200     MOVE     FILE-RECORD-INFO-P1-120 (1) TO RL-FS1R1-F-G-120.    RL2094.2
040300     WRITE    RL-FS1R1-F-G-120                                    RL2094.2
040400              INVALID KEY GO TO REL-FAIL-001.                     RL2094.2
040500     IF      XRECORD-NUMBER (1) EQUAL TO 250                      RL2094.2
040600             GO TO REL-TEST-001-2.                                RL2094.2
040700     ADD      000001 TO XRECORD-NUMBER (1).                       RL2094.2
040800     GO       TO REL-TEST-001.                                    RL2094.2
040900 REL-TEST-001-1.                                                  RL2094.2
041000     MOVE     140 TO WRK-SIZE XRECORD-LENGTH(1).                  RL2094.2
041100     MOVE     FILE-RECORD-INFO(1) TO RL-FS1R1-F-G-120.            RL2094.2
041200     WRITE    RL-FS1R1-F-G-120                                    RL2094.2
041300              INVALID KEY GO TO REL-FAIL-001.                     RL2094.2
041400     IF XRECORD-NUMBER(1) EQUAL TO 500                            RL2094.2
041500              GO TO REL-WRITE-001.                                RL2094.2
041600 REL-TEST-001-2.                                                  RL2094.2
041700     ADD      000001 TO XRECORD-NUMBER(1).                        RL2094.2
041800     GO TO REL-TEST-001-1.                                        RL2094.2
041900 REL-DELETE-001.                                                  RL2094.2
042000     PERFORM   DE-LETE.                                           RL2094.2
042100     GO TO REL-WRITE-001.                                         RL2094.2
042200 REL-FAIL-001.                                                    RL2094.2
042300     PERFORM   FAIL.                                              RL2094.2
042400     MOVE    "BOUNDARY VIOLATION"  TO RE-MARK.                    RL2094.2
042500 REL-WRITE-001.                                                   RL2094.2
042600     MOVE     "REL-TEST-001" TO   PAR-NAME                        RL2094.2
042700     MOVE     "FILE CREATED, LFILE "  TO COMPUTED-A.              RL2094.2
042800     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2094.2
042900     PERFORM  PRINT-DETAIL.                                       RL2094.2
043000     CLOSE    RL-FS1.                                             RL2094.2
043100 REL-INIT-002.                                                    RL2094.2
043200     OPEN     INPUT     RL-FS1.                                   RL2094.2
043300     MOVE     ZERO      TO WRK-CS-09V00.                          RL2094.2
043400 REL-TEST-002.                                                    RL2094.2
043500     READ     RL-FS1                                              RL2094.2
043600              AT END GO TO REL-TEST-002-1.                        RL2094.2
043700     MOVE     RL-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).    RL2094.2
043800     ADD      1 TO WRK-CS-09V00.                                  RL2094.2
043900     IF       WRK-CS-09V00 GREATER 500                            RL2094.2
044000             MOVE "MORE THAN 500 RECORDS" TO RE-MARK              RL2094.2
044100              GO TO REL-TEST-002-1.                               RL2094.2
044200     GO       TO REL-TEST-002.                                    RL2094.2
044300 REL-DELETE-002.                                                  RL2094.2
044400     PERFORM     DE-LETE.                                         RL2094.2
044500     PERFORM    PRINT-DETAIL.                                     RL2094.2
044600 REL-TEST-002-1.                                                  RL2094.2
044700     IF       XRECORD-NUMBER (1) NOT EQUAL TO 500                 RL2094.2
044800              PERFORM FAIL                                        RL2094.2
044900              ELSE                                                RL2094.2
045000              PERFORM PASS.                                       RL2094.2
045100     GO       TO REL-WRITE-002.                                   RL2094.2
045200 REL-WRITE-002.                                                   RL2094.2
045300     MOVE     "REL-TEST-002" TO PAR-NAME.                         RL2094.2
045400     MOVE     "FILE VERIFIED, LFILE" TO COMPUTED-A.               RL2094.2
045500     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2094.2
045600     PERFORM  PRINT-DETAIL.                                       RL2094.2
045700     CLOSE   RL-FS1.                                              RL2094.2
045800 CCVS-EXIT SECTION.                                               RL2094.2
045900 CCVS-999999.                                                     RL2094.2
046000     GO TO CLOSE-FILES.                                           RL2094.2
      *END-OF,RL209A                                                                  
