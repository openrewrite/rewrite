      *HEADER,COBOL,RL212A,SUBPRG,RL213A                                              
000100 IDENTIFICATION DIVISION.                                         RL2134.2
000200 PROGRAM-ID.                                                      RL2134.2
000300     RL213A.                                                      RL2134.2
000400****************************************************************  RL2134.2
000500*                                                              *  RL2134.2
000600*    VALIDATION FOR:-                                          *  RL2134.2
000700*                                                              *  RL2134.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2134.2
000900*                                                              *  RL2134.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2134.2
001100*                                                              *  RL2134.2
001200****************************************************************  RL2134.2
001300*    THE FUNCTION OF THIS PROGRAM IS TO PROCESS A RELATIVE I-O *  RL2134.2
001400*    FILE SEQUENTIALLY (ACCESS MODE SEQUENTIAL).  THE FILE     *  RL2134.2
001500*    USED AS INPUT IS THE FILE "RL-FS1" CREATED BY RL212A AND  *  RL2134.2
001600*    THE OTHER FILE "RL-FS2" WILL NOT BE PRESENT AT THE        *  RL2134.2
001700*    EXECUTION OF THE PROGRAM.                                 *  RL2134.2
001800*                                                              *  RL2134.2
001900*        X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS     *  RL2134.2
002000*        PROGRAM ARE:                                          *  RL2134.2
002100*                                                              *  RL2134.2
002200*             X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR  *  RL2134.2
002300*                     RELATIVE  I-O DATA FILE (RL-FS1)         *  RL2134.2
002400*             X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR  *  RL2134.2
002500*                     RELATIVE  I-O DATA FILE (RL-FS2)         *  RL2134.2
002600*             X-55   SYSTEM PRINTER                            *  RL2134.2
002700*             X-69   ADDITIONAL VALUE OF CLAUSES               *  RL2134.2
002800*             X-74   VALUE OF IMPLEMENTOR-NAME                 *  RL2134.2
002900*             X-75   OBJECT OF VALUE OF CLAUSE                 *  RL2134.2
003000*             X-82   SOURCE-COMPUTER                           *  RL2134.2
003100*             X-83   OBJECT-COMPUTER.                          *  RL2134.2
003200*                                                              *  RL2134.2
003300****************************************************************  RL2134.2
003400 ENVIRONMENT DIVISION.                                            RL2134.2
003500 CONFIGURATION SECTION.                                           RL2134.2
003600 SOURCE-COMPUTER.                                                 RL2134.2
003700     XXXXX082.                                                    RL2134.2
003800 OBJECT-COMPUTER.                                                 RL2134.2
003900     XXXXX083.                                                    RL2134.2
004000 INPUT-OUTPUT SECTION.                                            RL2134.2
004100 FILE-CONTROL.                                                    RL2134.2
004200     SELECT PRINT-FILE ASSIGN TO                                  RL2134.2
004300     XXXXX055.                                                    RL2134.2
004400     SELECT  OPTIONAL RL-FS1 ASSIGN TO                            RL2134.2
004500     XXXXX021                                                     RL2134.2
004600             ORGANIZATION IS RELATIVE                             RL2134.2
004700             ACCESS  SEQUENTIAL.                                  RL2134.2
004800     SELECT  OPTIONAL RL-FS2 ASSIGN TO                            RL2134.2
004900     XXXXX022                                                     RL2134.2
005000             ORGANIZATION IS RELATIVE                             RL2134.2
005100             ACCESS  SEQUENTIAL.                                  RL2134.2
005200 DATA DIVISION.                                                   RL2134.2
005300 FILE SECTION.                                                    RL2134.2
005400 FD  PRINT-FILE.                                                  RL2134.2
005500 01  PRINT-REC PICTURE X(120).                                    RL2134.2
005600 01  DUMMY-RECORD PICTURE X(120).                                 RL2134.2
005700 FD  RL-FS1                                                       RL2134.2
005800     LABEL RECORDS STANDARD                                       RL2134.2
005900C    VALUE OF                                                     RL2134.2
006000C    XXXXX074                                                     RL2134.2
006100C    IS                                                           RL2134.2
006200C    XXXXX075                                                     RL2134.2
006300*    XXXXX069                                                     RL2134.2
006400     BLOCK CONTAINS 1 RECORDS                                     RL2134.2
006500     RECORD CONTAINS 120 CHARACTERS.                              RL2134.2
006600 01  RL-FS1R1-F-G-120.                                            RL2134.2
006700     02 FILLER PIC X(120).                                        RL2134.2
006800 FD  RL-FS2                                                       RL2134.2
006900     LABEL RECORDS STANDARD                                       RL2134.2
007000C    VALUE OF                                                     RL2134.2
007100C    XXXXX074                                                     RL2134.2
007200C    IS                                                           RL2134.2
007300C    XXXXX075                                                     RL2134.2
007400*    XXXXX069                                                     RL2134.2
007500     BLOCK CONTAINS 1 RECORDS                                     RL2134.2
007600     RECORD CONTAINS 120 CHARACTERS.                              RL2134.2
007700 01  RL-FS2R1-F-G-120.                                            RL2134.2
007800     02 FILLER PIC X(120).                                        RL2134.2
007900 WORKING-STORAGE SECTION.                                         RL2134.2
008000 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                RL2134.2
008100 01  FILE-RECORD-INFORMATION-REC.                                 RL2134.2
008200     03 FILE-RECORD-INFO-SKELETON.                                RL2134.2
008300        05 FILLER                 PICTURE X(48)       VALUE       RL2134.2
008400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL2134.2
008500        05 FILLER                 PICTURE X(46)       VALUE       RL2134.2
008600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL2134.2
008700        05 FILLER                 PICTURE X(26)       VALUE       RL2134.2
008800             ",LFIL=000000,ORG=  ,LBLR= ".                        RL2134.2
008900        05 FILLER                 PICTURE X(37)       VALUE       RL2134.2
009000             ",RECKEY=                             ".             RL2134.2
009100        05 FILLER                 PICTURE X(38)       VALUE       RL2134.2
009200             ",ALTKEY1=                             ".            RL2134.2
009300        05 FILLER                 PICTURE X(38)       VALUE       RL2134.2
009400             ",ALTKEY2=                             ".            RL2134.2
009500        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL2134.2
009600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL2134.2
009700        05 FILE-RECORD-INFO-P1-120.                               RL2134.2
009800           07 FILLER              PIC X(5).                       RL2134.2
009900           07 XFILE-NAME           PIC X(6).                      RL2134.2
010000           07 FILLER              PIC X(8).                       RL2134.2
010100           07 XRECORD-NAME         PIC X(6).                      RL2134.2
010200           07 FILLER              PIC X(1).                       RL2134.2
010300           07 REELUNIT-NUMBER     PIC 9(1).                       RL2134.2
010400           07 FILLER              PIC X(7).                       RL2134.2
010500           07 XRECORD-NUMBER       PIC 9(6).                      RL2134.2
010600           07 FILLER              PIC X(6).                       RL2134.2
010700           07 UPDATE-NUMBER       PIC 9(2).                       RL2134.2
010800           07 FILLER              PIC X(5).                       RL2134.2
010900           07 ODO-NUMBER          PIC 9(4).                       RL2134.2
011000           07 FILLER              PIC X(5).                       RL2134.2
011100           07 XPROGRAM-NAME        PIC X(5).                      RL2134.2
011200           07 FILLER              PIC X(7).                       RL2134.2
011300           07 XRECORD-LENGTH       PIC 9(6).                      RL2134.2
011400           07 FILLER              PIC X(7).                       RL2134.2
011500           07 CHARS-OR-RECORDS    PIC X(2).                       RL2134.2
011600           07 FILLER              PIC X(1).                       RL2134.2
011700           07 XBLOCK-SIZE          PIC 9(4).                      RL2134.2
011800           07 FILLER              PIC X(6).                       RL2134.2
011900           07 RECORDS-IN-FILE     PIC 9(6).                       RL2134.2
012000           07 FILLER              PIC X(5).                       RL2134.2
012100           07 XFILE-ORGANIZATION   PIC X(2).                      RL2134.2
012200           07 FILLER              PIC X(6).                       RL2134.2
012300           07 XLABEL-TYPE          PIC X(1).                      RL2134.2
012400        05 FILE-RECORD-INFO-P121-240.                             RL2134.2
012500           07 FILLER              PIC X(8).                       RL2134.2
012600           07 XRECORD-KEY          PIC X(29).                     RL2134.2
012700           07 FILLER              PIC X(9).                       RL2134.2
012800           07 ALTERNATE-KEY1      PIC X(29).                      RL2134.2
012900           07 FILLER              PIC X(9).                       RL2134.2
013000           07 ALTERNATE-KEY2      PIC X(29).                      RL2134.2
013100           07 FILLER              PIC X(7).                       RL2134.2
013200 01  TEST-RESULTS.                                                RL2134.2
013300     02 FILLER                   PIC X      VALUE SPACE.          RL2134.2
013400     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL2134.2
013500     02 FILLER                   PIC X      VALUE SPACE.          RL2134.2
013600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL2134.2
013700     02 FILLER                   PIC X      VALUE SPACE.          RL2134.2
013800     02  PAR-NAME.                                                RL2134.2
013900       03 FILLER                 PIC X(19)  VALUE SPACE.          RL2134.2
014000       03  PARDOT-X              PIC X      VALUE SPACE.          RL2134.2
014100       03 DOTVALUE               PIC 99     VALUE ZERO.           RL2134.2
014200     02 FILLER                   PIC X(8)   VALUE SPACE.          RL2134.2
014300     02 RE-MARK                  PIC X(61).                       RL2134.2
014400 01  TEST-COMPUTED.                                               RL2134.2
014500     02 FILLER                   PIC X(30)  VALUE SPACE.          RL2134.2
014600     02 FILLER                   PIC X(17)  VALUE                 RL2134.2
014700            "       COMPUTED=".                                   RL2134.2
014800     02 COMPUTED-X.                                               RL2134.2
014900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL2134.2
015000     03 COMPUTED-N               REDEFINES COMPUTED-A             RL2134.2
015100                                 PIC -9(9).9(9).                  RL2134.2
015200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL2134.2
015300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL2134.2
015400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL2134.2
015500     03       CM-18V0 REDEFINES COMPUTED-A.                       RL2134.2
015600         04 COMPUTED-18V0                    PIC -9(18).          RL2134.2
015700         04 FILLER                           PIC X.               RL2134.2
015800     03 FILLER PIC X(50) VALUE SPACE.                             RL2134.2
015900 01  TEST-CORRECT.                                                RL2134.2
016000     02 FILLER PIC X(30) VALUE SPACE.                             RL2134.2
016100     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL2134.2
016200     02 CORRECT-X.                                                RL2134.2
016300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL2134.2
016400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL2134.2
016500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL2134.2
016600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL2134.2
016700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL2134.2
016800     03      CR-18V0 REDEFINES CORRECT-A.                         RL2134.2
016900         04 CORRECT-18V0                     PIC -9(18).          RL2134.2
017000         04 FILLER                           PIC X.               RL2134.2
017100     03 FILLER PIC X(2) VALUE SPACE.                              RL2134.2
017200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL2134.2
017300 01  CCVS-C-1.                                                    RL2134.2
017400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL2134.2
017500-    "SS  PARAGRAPH-NAME                                          RL2134.2
017600-    "       REMARKS".                                            RL2134.2
017700     02 FILLER                     PIC X(20)    VALUE SPACE.      RL2134.2
017800 01  CCVS-C-2.                                                    RL2134.2
017900     02 FILLER                     PIC X        VALUE SPACE.      RL2134.2
018000     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL2134.2
018100     02 FILLER                     PIC X(15)    VALUE SPACE.      RL2134.2
018200     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL2134.2
018300     02 FILLER                     PIC X(94)    VALUE SPACE.      RL2134.2
018400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL2134.2
018500 01  REC-CT                        PIC 99       VALUE ZERO.       RL2134.2
018600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL2134.2
018700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL2134.2
018800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL2134.2
018900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL2134.2
019000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL2134.2
019100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL2134.2
019200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL2134.2
019300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL2134.2
019400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL2134.2
019500 01  CCVS-H-1.                                                    RL2134.2
019600     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2134.2
019700     02  FILLER                    PIC X(42)    VALUE             RL2134.2
019800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL2134.2
019900     02  FILLER                    PIC X(39)    VALUE SPACES.     RL2134.2
020000 01  CCVS-H-2A.                                                   RL2134.2
020100   02  FILLER                        PIC X(40)  VALUE SPACE.      RL2134.2
020200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL2134.2
020300   02  FILLER                        PIC XXXX   VALUE             RL2134.2
020400     "4.2 ".                                                      RL2134.2
020500   02  FILLER                        PIC X(28)  VALUE             RL2134.2
020600            " COPY - NOT FOR DISTRIBUTION".                       RL2134.2
020700   02  FILLER                        PIC X(41)  VALUE SPACE.      RL2134.2
020800                                                                  RL2134.2
020900 01  CCVS-H-2B.                                                   RL2134.2
021000   02  FILLER                        PIC X(15)  VALUE             RL2134.2
021100            "TEST RESULT OF ".                                    RL2134.2
021200   02  TEST-ID                       PIC X(9).                    RL2134.2
021300   02  FILLER                        PIC X(4)   VALUE             RL2134.2
021400            " IN ".                                               RL2134.2
021500   02  FILLER                        PIC X(12)  VALUE             RL2134.2
021600     " HIGH       ".                                              RL2134.2
021700   02  FILLER                        PIC X(22)  VALUE             RL2134.2
021800            " LEVEL VALIDATION FOR ".                             RL2134.2
021900   02  FILLER                        PIC X(58)  VALUE             RL2134.2
022000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2134.2
022100 01  CCVS-H-3.                                                    RL2134.2
022200     02  FILLER                      PIC X(34)  VALUE             RL2134.2
022300            " FOR OFFICIAL USE ONLY    ".                         RL2134.2
022400     02  FILLER                      PIC X(58)  VALUE             RL2134.2
022500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL2134.2
022600     02  FILLER                      PIC X(28)  VALUE             RL2134.2
022700            "  COPYRIGHT   1985 ".                                RL2134.2
022800 01  CCVS-E-1.                                                    RL2134.2
022900     02 FILLER                       PIC X(52)  VALUE SPACE.      RL2134.2
023000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL2134.2
023100     02 ID-AGAIN                     PIC X(9).                    RL2134.2
023200     02 FILLER                       PIC X(45)  VALUE SPACES.     RL2134.2
023300 01  CCVS-E-2.                                                    RL2134.2
023400     02  FILLER                      PIC X(31)  VALUE SPACE.      RL2134.2
023500     02  FILLER                      PIC X(21)  VALUE SPACE.      RL2134.2
023600     02 CCVS-E-2-2.                                               RL2134.2
023700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL2134.2
023800         03 FILLER                   PIC X      VALUE SPACE.      RL2134.2
023900         03 ENDER-DESC               PIC X(44)  VALUE             RL2134.2
024000            "ERRORS ENCOUNTERED".                                 RL2134.2
024100 01  CCVS-E-3.                                                    RL2134.2
024200     02  FILLER                      PIC X(22)  VALUE             RL2134.2
024300            " FOR OFFICIAL USE ONLY".                             RL2134.2
024400     02  FILLER                      PIC X(12)  VALUE SPACE.      RL2134.2
024500     02  FILLER                      PIC X(58)  VALUE             RL2134.2
024600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL2134.2
024700     02  FILLER                      PIC X(13)  VALUE SPACE.      RL2134.2
024800     02 FILLER                       PIC X(15)  VALUE             RL2134.2
024900             " COPYRIGHT 1985".                                   RL2134.2
025000 01  CCVS-E-4.                                                    RL2134.2
025100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL2134.2
025200     02 FILLER                       PIC X(4)   VALUE " OF ".     RL2134.2
025300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL2134.2
025400     02 FILLER                       PIC X(40)  VALUE             RL2134.2
025500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL2134.2
025600 01  XXINFO.                                                      RL2134.2
025700     02 FILLER                       PIC X(19)  VALUE             RL2134.2
025800            "*** INFORMATION ***".                                RL2134.2
025900     02 INFO-TEXT.                                                RL2134.2
026000       04 FILLER                     PIC X(8)   VALUE SPACE.      RL2134.2
026100       04 XXCOMPUTED                 PIC X(20).                   RL2134.2
026200       04 FILLER                     PIC X(5)   VALUE SPACE.      RL2134.2
026300       04 XXCORRECT                  PIC X(20).                   RL2134.2
026400     02 INF-ANSI-REFERENCE           PIC X(48).                   RL2134.2
026500 01  HYPHEN-LINE.                                                 RL2134.2
026600     02 FILLER  PIC IS X VALUE IS SPACE.                          RL2134.2
026700     02 FILLER  PIC IS X(65)    VALUE IS "************************RL2134.2
026800-    "*****************************************".                 RL2134.2
026900     02 FILLER  PIC IS X(54)    VALUE IS "************************RL2134.2
027000-    "******************************".                            RL2134.2
027100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL2134.2
027200     "RL213A".                                                    RL2134.2
027300 PROCEDURE DIVISION.                                              RL2134.2
027400 CCVS1 SECTION.                                                   RL2134.2
027500 OPEN-FILES.                                                      RL2134.2
027600     OPEN    OUTPUT PRINT-FILE.                                   RL2134.2
027700     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL2134.2
027800     MOVE    SPACE TO TEST-RESULTS.                               RL2134.2
027900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL2134.2
028000     MOVE    ZERO TO REC-SKL-SUB.                                 RL2134.2
028100     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL2134.2
028200 CCVS-INIT-FILE.                                                  RL2134.2
028300     ADD     1 TO REC-SKL-SUB.                                    RL2134.2
028400     MOVE    FILE-RECORD-INFO-SKELETON                            RL2134.2
028500          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL2134.2
028600 CCVS-INIT-EXIT.                                                  RL2134.2
028700     GO TO CCVS1-EXIT.                                            RL2134.2
028800 CLOSE-FILES.                                                     RL2134.2
028900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL2134.2
029000 TERMINATE-CCVS.                                                  RL2134.2
029100*    EXIT PROGRAM.                                                RL2134.2
029200*TERMINATE-CALL.                                                  RL2134.2
029300     STOP     RUN.                                                RL2134.2
029400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL2134.2
029500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL2134.2
029600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL2134.2
029700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL2134.2
029800     MOVE "****TEST DELETED****" TO RE-MARK.                      RL2134.2
029900 PRINT-DETAIL.                                                    RL2134.2
030000     IF REC-CT NOT EQUAL TO ZERO                                  RL2134.2
030100             MOVE "." TO PARDOT-X                                 RL2134.2
030200             MOVE REC-CT TO DOTVALUE.                             RL2134.2
030300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL2134.2
030400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL2134.2
030500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL2134.2
030600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL2134.2
030700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL2134.2
030800     MOVE SPACE TO CORRECT-X.                                     RL2134.2
030900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL2134.2
031000     MOVE     SPACE TO RE-MARK.                                   RL2134.2
031100 HEAD-ROUTINE.                                                    RL2134.2
031200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2134.2
031300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL2134.2
031400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2134.2
031500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL2134.2
031600 COLUMN-NAMES-ROUTINE.                                            RL2134.2
031700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2134.2
031800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2134.2
031900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL2134.2
032000 END-ROUTINE.                                                     RL2134.2
032100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL2134.2
032200 END-RTN-EXIT.                                                    RL2134.2
032300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2134.2
032400 END-ROUTINE-1.                                                   RL2134.2
032500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL2134.2
032600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL2134.2
032700      ADD PASS-COUNTER TO ERROR-HOLD.                             RL2134.2
032800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL2134.2
032900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL2134.2
033000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL2134.2
033100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL2134.2
033200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL2134.2
033300  END-ROUTINE-12.                                                 RL2134.2
033400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL2134.2
033500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL2134.2
033600         MOVE "NO " TO ERROR-TOTAL                                RL2134.2
033700         ELSE                                                     RL2134.2
033800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL2134.2
033900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL2134.2
034000     PERFORM WRITE-LINE.                                          RL2134.2
034100 END-ROUTINE-13.                                                  RL2134.2
034200     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL2134.2
034300         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL2134.2
034400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL2134.2
034500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL2134.2
034600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2134.2
034700      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL2134.2
034800          MOVE "NO " TO ERROR-TOTAL                               RL2134.2
034900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL2134.2
035000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL2134.2
035100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL2134.2
035200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL2134.2
035300 WRITE-LINE.                                                      RL2134.2
035400     ADD 1 TO RECORD-COUNT.                                       RL2134.2
035500*    IF RECORD-COUNT GREATER 50                                   RL2134.2
035600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL2134.2
035700*        MOVE SPACE TO DUMMY-RECORD                               RL2134.2
035800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL2134.2
035900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL2134.2
036000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL2134.2
036100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL2134.2
036200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL2134.2
036300*        MOVE ZERO TO RECORD-COUNT.                               RL2134.2
036400     PERFORM WRT-LN.                                              RL2134.2
036500 WRT-LN.                                                          RL2134.2
036600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL2134.2
036700     MOVE SPACE TO DUMMY-RECORD.                                  RL2134.2
036800 BLANK-LINE-PRINT.                                                RL2134.2
036900     PERFORM WRT-LN.                                              RL2134.2
037000 FAIL-ROUTINE.                                                    RL2134.2
037100     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL2134.2
037200            GO TO   FAIL-ROUTINE-WRITE.                           RL2134.2
037300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL2134.2
037400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2134.2
037500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL2134.2
037600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2134.2
037700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2134.2
037800     GO TO  FAIL-ROUTINE-EX.                                      RL2134.2
037900 FAIL-ROUTINE-WRITE.                                              RL2134.2
038000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL2134.2
038100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL2134.2
038200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL2134.2
038300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL2134.2
038400 FAIL-ROUTINE-EX. EXIT.                                           RL2134.2
038500 BAIL-OUT.                                                        RL2134.2
038600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL2134.2
038700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL2134.2
038800 BAIL-OUT-WRITE.                                                  RL2134.2
038900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL2134.2
039000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL2134.2
039100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL2134.2
039200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL2134.2
039300 BAIL-OUT-EX. EXIT.                                               RL2134.2
039400 CCVS1-EXIT.                                                      RL2134.2
039500     EXIT.                                                        RL2134.2
039600 SECT-RL213A-001 SECTION.                                         RL2134.2
039700 REL-INIT-001.                                                    RL2134.2
039800     MOVE   "EXTEND FILE RL-FS1" TO FEATURE.                      RL2134.2
039900     OPEN    EXTEND RL-FS1.                                       RL2134.2
040000     MOVE     "RL-FS1" TO XFILE-NAME (1).                         RL2134.2
040100     MOVE     "R1-F-G" TO XRECORD-NAME (1).                       RL2134.2
040200     MOVE CCVS-PGM-ID  TO XPROGRAM-NAME (1).                      RL2134.2
040300     MOVE     000120   TO XRECORD-LENGTH (1).                     RL2134.2
040400     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   RL2134.2
040500     MOVE     0001     TO XBLOCK-SIZE (1).                        RL2134.2
040600     MOVE     000520   TO RECORDS-IN-FILE (1).                    RL2134.2
040700     MOVE     "RL"     TO XFILE-ORGANIZATION (1).                 RL2134.2
040800     MOVE     "S"      TO XLABEL-TYPE (1).                        RL2134.2
040900     MOVE     000501   TO XRECORD-NUMBER (1).                     RL2134.2
041000     GO TO REL-TEST-001.                                          RL2134.2
041100 REL-DELETE-001.                                                  RL2134.2
041200     PERFORM DE-LETE.                                             RL2134.2
041300     PERFORM PRINT-DETAIL.                                        RL2134.2
041400     GO TO   CCVS-EXIT.                                           RL2134.2
041500 REL-TEST-001.                                                    RL2134.2
041600     MOVE    FILE-RECORD-INFO-P1-120 (1) TO RL-FS1R1-F-G-120.     RL2134.2
041700     WRITE   RL-FS1R1-F-G-120                                     RL2134.2
041800             INVALID KEY GO TO REL-FAIL-001.                      RL2134.2
041900     IF      XRECORD-NUMBER (1) EQUAL TO 520                      RL2134.2
042000             GO TO REL-WRITE-001.                                 RL2134.2
042100     ADD     000001 TO XRECORD-NUMBER (1).                        RL2134.2
042200     GO      TO REL-TEST-001.                                     RL2134.2
042300 REL-FAIL-001.                                                    RL2134.2
042400     PERFORM FAIL.                                                RL2134.2
042500     MOVE   "BOUNDARY VIOLATION"  TO RE-MARK.                     RL2134.2
042600 REL-WRITE-001.                                                   RL2134.2
042700     MOVE   "REL-TEST-001" TO   PAR-NAME                          RL2134.2
042800     MOVE   "FILE EXTENDED, LFILE"  TO COMPUTED-A.                RL2134.2
042900     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2134.2
043000     PERFORM PRINT-DETAIL.                                        RL2134.2
043100     CLOSE   RL-FS1.                                              RL2134.2
043200*                                                                 RL2134.2
043300 REL-INIT-002.                                                    RL2134.2
043400     OPEN    INPUT     RL-FS1.                                    RL2134.2
043500     MOVE    ZERO      TO WRK-CS-09V00.                           RL2134.2
043600     GO TO   REL-TEST-002.                                        RL2134.2
043700 REL-DELETE-002.                                                  RL2134.2
043800     PERFORM DE-LETE.                                             RL2134.2
043900     PERFORM PRINT-DETAIL.                                        RL2134.2
044000     GO TO   CCVS-EXIT.                                           RL2134.2
044100 REL-TEST-002.                                                    RL2134.2
044200     READ    RL-FS1                                               RL2134.2
044300             AT END GO TO REL-TEST-002-2.                         RL2134.2
044400     MOVE    RL-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).     RL2134.2
044500     ADD     1 TO WRK-CS-09V00.                                   RL2134.2
044600     IF      WRK-CS-09V00 GREATER 520                             RL2134.2
044700             MOVE "MORE THAN 520 RECORDS" TO RE-MARK              RL2134.2
044800             GO TO REL-TEST-002-2.                                RL2134.2
044900 REL-TEST-002-1.                                                  RL2134.2
045000     MOVE   "REL-TEST-002-1" TO PAR-NAME.                         RL2134.2
045100     IF      XRECORD-NUMBER (1) NOT = WRK-CS-09V00                RL2134.2
045200             MOVE   "INCORRECT RECORD NUMBER" TO RE-MARK          RL2134.2
045300             MOVE    XRECORD-NUMBER (1) TO COMPUTED-N             RL2134.2
045400             MOVE    WRK-CS-09V00       TO CORRECT-N              RL2134.2
045500             PERFORM FAIL                                         RL2134.2
045600             PERFORM PRINT-DETAIL                                 RL2134.2
045700     ELSE                                                         RL2134.2
045800             PERFORM PASS.                                        RL2134.2
045900*            PERFORM PRINT-DETAIL.                                RL2134.2
046000     GO      TO REL-TEST-002.                                     RL2134.2
046100 REL-TEST-002-2.                                                  RL2134.2
046200     MOVE   "REL-TEST-002-2" TO PAR-NAME.                         RL2134.2
046300     IF      XRECORD-NUMBER (1) NOT EQUAL TO 520                  RL2134.2
046400             PERFORM FAIL                                         RL2134.2
046500     ELSE                                                         RL2134.2
046600             PERFORM PASS.                                        RL2134.2
046700 REL-WRITE-002.                                                   RL2134.2
046800     MOVE   "REL-TEST-002" TO PAR-NAME.                           RL2134.2
046900     MOVE   "FILE VERIFIED, LFILE" TO COMPUTED-A.                 RL2134.2
047000     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL2134.2
047100     PERFORM PRINT-DETAIL.                                        RL2134.2
047200     CLOSE   RL-FS1.                                              RL2134.2
047300 CCVS-EXIT SECTION.                                               RL2134.2
047400 CCVS-999999.                                                     RL2134.2
047500     GO TO CLOSE-FILES.                                           RL2134.2
      *END-OF,RL213A                                                                  
