      *HEADER,COBOL,RL210A                                                            
000100 IDENTIFICATION DIVISION.                                         RL2104.2
000200 PROGRAM-ID.                                                      RL2104.2
000300     RL210A.                                                      RL2104.2
000400****************************************************************  RL2104.2
000500*                                                              *  RL2104.2
000600*    VALIDATION FOR:-                                          *  RL2104.2
000700*                                                              *  RL2104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2104.2
000900*                                                              *  RL2104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2104.2
001100*                                                              *  RL2104.2
001200****************************************************************  RL2104.2
001300*    THE FUNCTION OF THIS PROGRAM IS TO CREATE A RELATIVE FILE *  RL2104.2
001400*    SEQUENTIALLY WITH VARIABLE LENGTH RECORDS AND VERIFY THAT *  RL2104.2
001500*    IT WAS CREATED CORRECTLY.                                 *  RL2104.2
001600*    THE PROGRAM WILL CREATE A RELATIVE FILE OF 500 VARIABLE   *  RL2104.2
001700*    LENGTH RECORDS.                                           *  RL2104.2
001800*    THE RECORD SIZE WILL BE 120 TO 140 CHARACTERS.            *  RL2104.2
001900*                                                                 RL2104.2
002000*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL2104.2
002100*            PROGRAM ARE:                                         RL2104.2
002200*                                                                 RL2104.2
002300*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL2104.2
002400*                         RELATIVE  I-O DATA FILE                 RL2104.2
002500*                 X-55   SYSTEM PRINTER                           RL2104.2
002600*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL2104.2
002700*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL2104.2
002800*                 X-75   OBJECT OF VALUE OF CLAUSE                RL2104.2
002900*                 X-82   SOURCE-COMPUTER                          RL2104.2
003000*                 X-83   OBJECT-COMPUTER.                         RL2104.2
003100*                                                                 RL2104.2
003200****************************************************************  RL2104.2
003300 ENVIRONMENT DIVISION.                                            RL2104.2
003400 CONFIGURATION SECTION.                                           RL2104.2
003500 SOURCE-COMPUTER.                                                 RL2104.2
003600     XXXXX082.                                                    RL2104.2
003700 OBJECT-COMPUTER.                                                 RL2104.2
003800     XXXXX083.                                                    RL2104.2
003900 INPUT-OUTPUT SECTION.                                            RL2104.2
004000 FILE-CONTROL.                                                    RL2104.2
004100     SELECT PRINT-FILE ASSIGN TO                                  RL2104.2
004200     XXXXX055.                                                    RL2104.2
004300     SELECT   RL-VS1 ASSIGN TO                                    RL2104.2
004400     XXXXP021                                                     RL2104.2
004500             ORGANIZATION IS RELATIVE.                            RL2104.2
004600*    ABSENCE OF THE ACCESS CLAUSE IS TREATED AS THOUGH            RL2104.2
004700*     SEQUENTIAL HAD BEEN SPECIFIED.                              RL2104.2
004800 DATA DIVISION.                                                   RL2104.2
004900 FILE SECTION.                                                    RL2104.2
005000 FD  PRINT-FILE.                                                  RL2104.2
005100 01  PRINT-REC PICTURE X(120).                                    RL2104.2
005200 01  DUMMY-RECORD PICTURE X(120).                                 RL2104.2
005300 FD  RL-VS1                                                       RL2104.2
005400     LABEL RECORDS STANDARD                                       RL2104.2
005500C    VALUE OF                                                     RL2104.2
005600C    XXXXX074                                                     RL2104.2
005700C    IS                                                           RL2104.2
005800C    XXXXX075                                                     RL2104.2
005900*    XXXXX069                                                     RL2104.2
006000     BLOCK CONTAINS 1 RECORDS                                     RL2104.2
006100     RECORD IS VARYING.                                           RL2104.2
006200 01  RL-VS1R2-F-G-140.                                            RL2104.2
006300     02  FILLER PIC X(120).                                       RL2104.2
006400     02  RL-VS1R2-F-G-121-124  PIC 9(4).                          RL2104.2
006500     02  RL-GROUP.                                                RL2104.2
006600      03 RL-VS1R2-F-G-125-140  PIC X  OCCURS 1 TO 16              RL2104.2
006700                               DEPENDING ON RL-VS1R2-F-G-121-124. RL2104.2
006800 01  RL-VS1R1-F-G-120.                                            RL2104.2
006900     02 FILLER PIC X(120).                                        RL2104.2
007000 WORKING-STORAGE SECTION.                                         RL2104.2
007100 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                RL2104.2
007200 01  FILE-RECORD-INFORMATION-REC.                                 RL2104.2
007300     03 FILE-RECORD-INFO-SKELETON.                                RL2104.2
007400        05 FILLER                 PICTURE X(48)       VALUE       RL2104.2
007500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2104.2
007600        05 FILLER                 PICTURE X(46)       VALUE       RL2104.2
007700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2104.2
007800        05 FILLER                 PICTURE X(26)       VALUE       RL2104.2
007900             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2104.2
008000        05 FILLER                 PICTURE X(37)       VALUE       RL2104.2
008100             ",RECKEY=                             ".             RL2104.2
008200        05 FILLER                 PICTURE X(38)       VALUE       RL2104.2
008300             ",ALTKEY1=                             ".            RL2104.2
008400        05 FILLER                 PICTURE X(38)       VALUE       RL2104.2
008500             ",ALTKEY2=                             ".            RL2104.2
008600        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2104.2
008700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2104.2
008800        05 FILE-RECORD-INFO-P1-120.                               RL2104.2
008900           07 FILLER              PIC X(5).                       RL2104.2
009000           07 XFILE-NAME           PIC X(6).                      RL2104.2
009100           07 FILLER              PIC X(8).                       RL2104.2
009200           07 XRECORD-NAME         PIC X(6).                      RL2104.2
009300           07 FILLER              PIC X(1).                       RL2104.2
009400           07 REELUNIT-NUMBER     PIC 9(1).                       RL2104.2
009500           07 FILLER              PIC X(7).                       RL2104.2
009600           07 XRECORD-NUMBER       PIC 9(6).                      RL2104.2
009700           07 FILLER              PIC X(6).                       RL2104.2
009800           07 UPDATE-NUMBER       PIC 9(2).                       RL2104.2
009900           07 FILLER              PIC X(5).                       RL2104.2
010000           07 ODO-NUMBER          PIC 9(4).                       RL2104.2
010100           07 FILLER              PIC X(5).                       RL2104.2
010200           07 XPROGRAM-NAME        PIC X(5).                      RL2104.2
010300           07 FILLER              PIC X(7).                       RL2104.2
010400           07 XRECORD-LENGTH       PIC 9(6).                      RL2104.2
010500           07 FILLER              PIC X(7).                       RL2104.2
010600           07 CHARS-OR-RECORDS    PIC X(2).                       RL2104.2
010700           07 FILLER              PIC X(1).                       RL2104.2
010800           07 XBLOCK-SIZE          PIC 9(4).                      RL2104.2
010900           07 FILLER              PIC X(6).                       RL2104.2
011000           07 RECORDS-IN-FILE     PIC 9(6).                       RL2104.2
011100           07 FILLER              PIC X(5).                       RL2104.2
011200           07 XFILE-ORGANIZATION   PIC X(2).                      RL2104.2
011300           07 FILLER              PIC X(6).                       RL2104.2
011400           07 XLABEL-TYPE          PIC X(1).                      RL2104.2
011500        05 FILE-RECORD-INFO-P121-240.                             RL2104.2
011600           07 FILLER              PIC X(8).                       RL2104.2
011700           07 XRECORD-KEY          PIC X(29).                     RL2104.2
011800           07 FILLER              PIC X(9).                       RL2104.2
011900           07 ALTERNATE-KEY1      PIC X(29).                      RL2104.2
012000           07 FILLER              PIC X(9).                       RL2104.2
012100           07 ALTERNATE-KEY2      PIC X(29).                      RL2104.2
012200           07 FILLER              PIC X(7).                       RL2104.2
012300 01  TEST-RESULTS.                                                RL2104.2
012400     02 FILLER                   PIC X      VALUE SPACE.          RL2104.2
012500     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2104.2
012600     02 FILLER                   PIC X      VALUE SPACE.          RL2104.2
012700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2104.2
012800     02 FILLER                   PIC X      VALUE SPACE.          RL2104.2
012900     02  PAR-NAME.                                                RL2104.2
013000       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2104.2
013100       03  PARDOT-X              PIC X      VALUE SPACE.          RL2104.2
013200       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2104.2
013300     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2104.2
013400     02 RE-MARK                  PIC X(61).                       RL2104.2
013500 01  TEST-COMPUTED.                                               RL2104.2
013600     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2104.2
013700     02 FILLER                   PIC X(17)  VALUE                 RL2104.2
013800            "       COMPUTED=".                                   RL2104.2
013900     02 COMPUTED-X.                                               RL2104.2
014000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2104.2
014100     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2104.2
014200                                 PIC -9(9).9(9).                  RL2104.2
014300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2104.2
014400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2104.2
014500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2104.2
014600     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2104.2
014700         04 COMPUTED-18V0                    PIC -9(18).          RL2104.2
014800         04 FILLER                           PIC X.               RL2104.2
014900     03 FILLER PIC X(50) VALUE SPACE.                             RL2104.2
015000 01  TEST-CORRECT.                                                RL2104.2
015100     02 FILLER PIC X(30) VALUE SPACE.                             RL2104.2
015200     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2104.2
015300     02 CORRECT-X.                                                RL2104.2
015400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2104.2
015500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2104.2
015600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2104.2
015700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2104.2
015800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2104.2
015900     03      CR-18V0 REDEFINES CORRECT-A.                         RL2104.2
016000         04 CORRECT-18V0                     PIC -9(18).          RL2104.2
016100         04 FILLER                           PIC X.               RL2104.2
016200     03 FILLER PIC X(2) VALUE SPACE.                              RL2104.2
016300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2104.2
016400 01  CCVS-C-1.                                                    RL2104.2
016500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2104.2
016600-    "SS  PARAGRAPH-NAME                                          RL2104.2
016700-    "       REMARKS".                                            RL2104.2
016800     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2104.2
016900 01  CCVS-C-2.                                                    RL2104.2
017000     02 FILLER                     PIC X        VALUE SPACE.      RL2104.2
017100     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2104.2
017200     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2104.2
017300     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2104.2
017400     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2104.2
017500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2104.2
017600 01  REC-CT                        PIC 99       VALUE ZERO.       RL2104.2
017700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2104.2
017800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2104.2
017900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2104.2
018000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2104.2
018100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2104.2
018200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2104.2
018300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2104.2
018400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2104.2
018500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2104.2
018600 01  CCVS-H-1.                                                    RL2104.2
018700     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2104.2
018800     02  FILLER                    PIC X(42)    VALUE             RL2104.2
018900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2104.2
019000     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2104.2
019100 01  CCVS-H-2A.                                                   RL2104.2
019200   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2104.2
019300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2104.2
019400   02  FILLER                        PIC XXXX   VALUE             RL2104.2
019500     "4.2 ".                                                      RL2104.2
019600   02  FILLER                        PIC X(28)  VALUE             RL2104.2
019700            " COPY - NOT FOR DISTRIBUTION".                       RL2104.2
019800   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2104.2
019900                                                                  RL2104.2
020000 01  CCVS-H-2B.                                                   RL2104.2
020100   02  FILLER                        PIC X(15)  VALUE             RL2104.2
020200            "TEST RESULT OF ".                                    RL2104.2
020300   02  TEST-ID                       PIC X(9).                    RL2104.2
020400   02  FILLER                        PIC X(4)   VALUE             RL2104.2
020500            " IN ".                                               RL2104.2
020600   02  FILLER                        PIC X(12)  VALUE             RL2104.2
020700     " HIGH       ".                                              RL2104.2
020800   02  FILLER                        PIC X(22)  VALUE             RL2104.2
020900            " LEVEL VALIDATION FOR ".                             RL2104.2
021000   02  FILLER                        PIC X(58)  VALUE             RL2104.2
021100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2104.2
021200 01  CCVS-H-3.                                                    RL2104.2
021300     02  FILLER                      PIC X(34)  VALUE             RL2104.2
021400            " FOR OFFICIAL USE ONLY    ".                         RL2104.2
021500     02  FILLER                      PIC X(58)  VALUE             RL2104.2
021600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2104.2
021700     02  FILLER                      PIC X(28)  VALUE             RL2104.2
021800            "  COPYRIGHT   1985 ".                                RL2104.2
021900 01  CCVS-E-1.                                                    RL2104.2
022000     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2104.2
022100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2104.2
022200     02 ID-AGAIN                     PIC X(9).                    RL2104.2
022300     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2104.2
022400 01  CCVS-E-2.                                                    RL2104.2
022500     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2104.2
022600     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2104.2
022700     02 CCVS-E-2-2.                                               RL2104.2
022800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2104.2
022900         03 FILLER                   PIC X      VALUE SPACE.      RL2104.2
023000         03 ENDER-DESC               PIC X(44)  VALUE             RL2104.2
023100            "ERRORS ENCOUNTERED".                                 RL2104.2
023200 01  CCVS-E-3.                                                    RL2104.2
023300     02  FILLER                      PIC X(22)  VALUE             RL2104.2
023400            " FOR OFFICIAL USE ONLY".                             RL2104.2
023500     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2104.2
023600     02  FILLER                      PIC X(58)  VALUE             RL2104.2
023700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2104.2
023800     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2104.2
023900     02 FILLER                       PIC X(15)  VALUE             RL2104.2
024000             " COPYRIGHT 1985".                                   RL2104.2
024100 01  CCVS-E-4.                                                    RL2104.2
024200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2104.2
024300     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2104.2
024400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2104.2
024500     02 FILLER                       PIC X(40)  VALUE             RL2104.2
024600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2104.2
024700 01  XXINFO.                                                      RL2104.2
024800     02 FILLER                       PIC X(19)  VALUE             RL2104.2
024900            "*** INFORMATION ***".                                RL2104.2
025000     02 INFO-TEXT.                                                RL2104.2
025100       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2104.2
025200       04 XXCOMPUTED                 PIC X(20).                   RL2104.2
025300       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2104.2
025400       04 XXCORRECT                  PIC X(20).                   RL2104.2
025500     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2104.2
025600 01  HYPHEN-LINE.                                                 RL2104.2
025700     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2104.2
025800     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2104.2
025900-    "*****************************************".                 RL2104.2
026000     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2104.2
026100-    "******************************".                            RL2104.2
026200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2104.2
026300     "RL210A".                                                    RL2104.2
026400 PROCEDURE DIVISION.                                              RL2104.2
026500 CCVS1 SECTION.                                                   RL2104.2
026600 OPEN-FILES.                                                      RL2104.2
026700     OPEN    OUTPUT PRINT-FILE.                                   RL2104.2
026800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2104.2
026900     MOVE    SPACE TO TEST-RESULTS.                               RL2104.2
027000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2104.2
027100     MOVE    ZERO TO REC-SKL-SUB.                                 RL2104.2
027200     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2104.2
027300 CCVS-INIT-FILE.                                                  RL2104.2
027400     ADD     1 TO REC-SKL-SUB.                                    RL2104.2
027500     MOVE    FILE-RECORD-INFO-SKELETON                            RL2104.2
027600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2104.2
027700 CCVS-INIT-EXIT.                                                  RL2104.2
027800     GO TO CCVS1-EXIT.                                            RL2104.2
027900 CLOSE-FILES.                                                     RL2104.2
028000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2104.2
028100 TERMINATE-CCVS.                                                  RL2104.2
028200*    EXIT PROGRAM.                                                RL2104.2
028300*TERMINATE-CALL.                                                  RL2104.2
028400     STOP     RUN.                                                RL2104.2
028500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2104.2
028600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2104.2
028700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2104.2
028800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2104.2
028900     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2104.2
029000 PRINT-DETAIL.                                                    RL2104.2
029100     IF REC-CT NOT EQUAL TO ZERO                                  RL2104.2
029200             MOVE "." TO PARDOT-X                                 RL2104.2
029300             MOVE REC-CT TO DOTVALUE.                             RL2104.2
029400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2104.2
029500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2104.2
029600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2104.2
029700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2104.2
029800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2104.2
029900     MOVE SPACE TO CORRECT-X.                                     RL2104.2
030000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2104.2
030100     MOVE     SPACE TO RE-MARK.                                   RL2104.2
030200 HEAD-ROUTINE.                                                    RL2104.2
030300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2104.2
030400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2104.2
030500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2104.2
030600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2104.2
030700 COLUMN-NAMES-ROUTINE.                                            RL2104.2
030800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2104.2
030900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2104.2
031000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2104.2
031100 END-ROUTINE.                                                     RL2104.2
031200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2104.2
031300 END-RTN-EXIT.                                                    RL2104.2
031400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2104.2
031500 END-ROUTINE-1.                                                   RL2104.2
031600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2104.2
031700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2104.2
031800      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2104.2
031900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2104.2
032000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2104.2
032100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2104.2
032200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2104.2
032300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2104.2
032400  END-ROUTINE-12.                                                 RL2104.2
032500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2104.2
032600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2104.2
032700         MOVE "NO " TO ERROR-TOTAL                                RL2104.2
032800         ELSE                                                     RL2104.2
032900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2104.2
033000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2104.2
033100     PERFORM WRITE-LINE.                                          RL2104.2
033200 END-ROUTINE-13.                                                  RL2104.2
033300     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2104.2
033400         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2104.2
033500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2104.2
033600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2104.2
033700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2104.2
033800      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2104.2
033900          MOVE "NO " TO ERROR-TOTAL                               RL2104.2
034000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2104.2
034100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2104.2
034200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2104.2
034300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2104.2
034400 WRITE-LINE.                                                      RL2104.2
034500     ADD 1 TO RECORD-COUNT.                                       RL2104.2
034600*    IF RECORD-COUNT GREATER 50                                   RL2104.2
034700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2104.2
034800*        MOVE SPACE TO DUMMY-RECORD                               RL2104.2
034900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2104.2
035000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2104.2
035100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2104.2
035200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2104.2
035300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2104.2
035400*        MOVE ZERO TO RECORD-COUNT.                               RL2104.2
035500     PERFORM WRT-LN.                                              RL2104.2
035600 WRT-LN.                                                          RL2104.2
035700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2104.2
035800     MOVE SPACE TO DUMMY-RECORD.                                  RL2104.2
035900 BLANK-LINE-PRINT.                                                RL2104.2
036000     PERFORM WRT-LN.                                              RL2104.2
036100 FAIL-ROUTINE.                                                    RL2104.2
036200     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2104.2
036300            GO TO   FAIL-ROUTINE-WRITE.                           RL2104.2
036400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2104.2
036500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2104.2
036600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2104.2
036700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2104.2
036800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2104.2
036900     GO TO  FAIL-ROUTINE-EX.                                      RL2104.2
037000 FAIL-ROUTINE-WRITE.                                              RL2104.2
037100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2104.2
037200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2104.2
037300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2104.2
037400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2104.2
037500 FAIL-ROUTINE-EX. EXIT.                                           RL2104.2
037600 BAIL-OUT.                                                        RL2104.2
037700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2104.2
037800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2104.2
037900 BAIL-OUT-WRITE.                                                  RL2104.2
038000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2104.2
038100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2104.2
038200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2104.2
038300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2104.2
038400 BAIL-OUT-EX. EXIT.                                               RL2104.2
038500 CCVS1-EXIT.                                                      RL2104.2
038600     EXIT.                                                        RL2104.2
038700 SECT-RL210A-001 SECTION.                                         RL2104.2
038800 REL-INIT-001.                                                    RL2104.2
038900     MOVE     "FILE CREATE RL-VS1" TO FEATURE.                    RL2104.2
039000     OPEN     OUTPUT    RL-VS1.                                   RL2104.2
039100     MOVE     "RL-VS1" TO XFILE-NAME (1).                         RL2104.2
039200     MOVE     "R1-F-G" TO XRECORD-NAME (1).                       RL2104.2
039300     MOVE CCVS-PGM-ID      TO XPROGRAM-NAME (1).                  RL2104.2
039400     MOVE     000120   TO XRECORD-LENGTH (1).                     RL2104.2
039500     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   RL2104.2
039600     MOVE     0001     TO XBLOCK-SIZE (1).                        RL2104.2
039700     MOVE     000500   TO RECORDS-IN-FILE (1).                    RL2104.2
039800     MOVE     "RL"     TO XFILE-ORGANIZATION (1).                 RL2104.2
039900     MOVE     "S"      TO XLABEL-TYPE (1).                        RL2104.2
040000     MOVE     000001   TO XRECORD-NUMBER (1).                     RL2104.2
040100 REL-TEST-001.                                                    RL2104.2
040200     MOVE    FILE-RECORD-INFO-P1-120 (1) TO RL-VS1R1-F-G-120.     RL2104.2
040300     IF      XRECORD-NUMBER (1) < 201                             RL2104.2
040400             WRITE  RL-VS1R1-F-G-120                              RL2104.2
040500             INVALID KEY GO TO REL-FAIL-001                       RL2104.2
040600     ELSE                                                         RL2104.2
040700             MOVE    16 TO RL-VS1R2-F-G-121-124                   RL2104.2
040800             MOVE   "ABCDEFGHIJKLMNOP" TO RL-GROUP                RL2104.2
040900             WRITE  RL-VS1R2-F-G-140                              RL2104.2
041000             INVALID KEY GO TO REL-FAIL-001.                      RL2104.2
041100     IF      XRECORD-NUMBER (1) EQUAL TO 500                      RL2104.2
041200             GO TO REL-WRITE-001.                                 RL2104.2
041300     ADD     000001 TO XRECORD-NUMBER (1).                        RL2104.2
041400     GO      TO REL-TEST-001.                                     RL2104.2
041500 REL-DELETE-001.                                                  RL2104.2
041600     PERFORM DE-LETE.                                             RL2104.2
041700     GO TO REL-WRITE-001.                                         RL2104.2
041800 REL-FAIL-001.                                                    RL2104.2
041900     PERFORM FAIL.                                                RL2104.2
042000     MOVE   "BOUNDARY VIOLATION"  TO RE-MARK.                     RL2104.2
042100 REL-WRITE-001.                                                   RL2104.2
042200     MOVE   "REL-TEST-001" TO   PAR-NAME                          RL2104.2
042300     MOVE   "FILE CREATED, LFILE "  TO COMPUTED-A.                RL2104.2
042400     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2104.2
042500     PERFORM PRINT-DETAIL.                                        RL2104.2
042600     CLOSE   RL-VS1.                                              RL2104.2
042700 REL-INIT-002.                                                    RL2104.2
042800     OPEN    INPUT     RL-VS1.                                    RL2104.2
042900     MOVE    ZERO      TO WRK-CS-09V00.                           RL2104.2
043000 REL-TEST-002.                                                    RL2104.2
043100     MOVE    SPACES TO RL-VS1R2-F-G-140.                          RL2104.2
043200     READ    RL-VS1                                               RL2104.2
043300             AT END GO TO REL-TEST-002-2.                         RL2104.2
043400     MOVE    RL-VS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).     RL2104.2
043500     ADD     1 TO WRK-CS-09V00.                                   RL2104.2
043600     IF      WRK-CS-09V00 GREATER 500                             RL2104.2
043700             MOVE "MORE THAN 500 RECORDS" TO RE-MARK              RL2104.2
043800             GO TO REL-TEST-002-2.                                RL2104.2
043900 REL-TEST-002-1-1.                                                RL2104.2
044000     MOVE   "VIII-31 3.8.4 GR 10B" TO ANSI-REFERENCE.             RL2104.2
044100     MOVE   "REL-TEST-002-1-1" TO PAR-NAME.                       RL2104.2
044200     IF      XLABEL-TYPE (1) NOT = "S"                            RL2104.2
044300             MOVE    XLABEL-TYPE (1) TO COMPUTED-X                RL2104.2
044400             MOVE   "S"   TO CORRECT-X                            RL2104.2
044500             MOVE   "INVALID RECORD READ" TO RE-MARK              RL2104.2
044600             PERFORM FAIL                                         RL2104.2
044700             PERFORM PRINT-DETAIL.                                RL2104.2
044800 REL-TEST-002-1-2.                                                RL2104.2
044900     MOVE   "VIII-31 3.8.4 GR 10C" TO ANSI-REFERENCE.             RL2104.2
045000     MOVE   "REL-TEST-002-1-2" TO PAR-NAME.                       RL2104.2
045100     IF      XRECORD-NUMBER (1) > 200                             RL2104.2
045200             IF      RL-VS1R2-F-G-121-124 NOT = 16                RL2104.2
045300                     MOVE    RL-VS1R2-F-G-121-124 TO COMPUTED-N   RL2104.2
045400                     MOVE    16   TO CORRECT-N                    RL2104.2
045500                     MOVE   "WRONG LENGTH RECORD" TO RE-MARK      RL2104.2
045600                     PERFORM FAIL                                 RL2104.2
045700                     PERFORM PRINT-DETAIL.                        RL2104.2
045800 REL-TEST-002-1-3.                                                RL2104.2
045900     MOVE   "VIII-31 3.8.4 GR 10B" TO ANSI-REFERENCE.             RL2104.2
046000     MOVE   "REL-TEST-002-1-3" TO PAR-NAME.                       RL2104.2
046100     IF      XRECORD-NUMBER (1) > 200                             RL2104.2
046200             IF      RL-GROUP NOT = "ABCDEFGHIJKLMNOP"            RL2104.2
046300                     MOVE   "ABCDEFGHIJKLMNOP" TO CORRECT-X       RL2104.2
046400                     MOVE    RL-GROUP          TO COMPUTED-X      RL2104.2
046500                     MOVE   "INVALID RECORD READ" TO RE-MARK      RL2104.2
046600                     PERFORM FAIL                                 RL2104.2
046700                     PERFORM PRINT-DETAIL.                        RL2104.2
046800     GO      TO REL-TEST-002.                                     RL2104.2
046900 REL-DELETE-002.                                                  RL2104.2
047000     PERFORM DE-LETE.                                             RL2104.2
047100     PERFORM PRINT-DETAIL.                                        RL2104.2
047200 REL-TEST-002-2.                                                  RL2104.2
047300     MOVE   "REL-TEST-002-2" TO PAR-NAME.                         RL2104.2
047400     IF      XRECORD-NUMBER (1) NOT EQUAL TO 500                  RL2104.2
047500             PERFORM FAIL                                         RL2104.2
047600     ELSE                                                         RL2104.2
047700             PERFORM PASS.                                        RL2104.2
047800     GO      TO REL-WRITE-002.                                    RL2104.2
047900 REL-WRITE-002.                                                   RL2104.2
048000     MOVE   "REL-TEST-002" TO PAR-NAME.                           RL2104.2
048100     MOVE   "FILE VERIFIED, LFILE" TO COMPUTED-A.                 RL2104.2
048200     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2104.2
048300     PERFORM PRINT-DETAIL.                                        RL2104.2
048400     CLOSE   RL-VS1.                                              RL2104.2
048500 CCVS-EXIT SECTION.                                               RL2104.2
048600 CCVS-999999.                                                     RL2104.2
048700     GO TO CLOSE-FILES.                                           RL2104.2
      *END-OF,RL210A                                                                  
