      *HEADER,COBOL,RL108A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1084.2
000200 PROGRAM-ID.                                                      RL1084.2
000300     RL108A.                                                      RL1084.2
000400****************************************************************  RL1084.2
000500*                                                              *  RL1084.2
000600*    VALIDATION FOR:-                                          *  RL1084.2
000700*                                                              *  RL1084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1084.2
000900*                                                              *  RL1084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1084.2
001100*                                                              *  RL1084.2
001200****************************************************************  RL1084.2
001300*                                                              *  RL1084.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1084.2
001500*                                                              *  RL1084.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  RL1084.2
001700*        X-61  - "LITERAL" IN "ASSIGN TO" CLAUSE FOR           *  RL1084.2
001800*                RELATIVE I-O DATA FILE.                       *  RL1084.2
001900*        X-69  - ADDITIONAL "VALUE OF" CLAUSE.                 *  RL1084.2
002000*        X-74  - VALUE OF IMPLEMENTOR-NAME.                    *  RL1084.2
002100*        X-75  - OBJECT OF "VALUE" CLAUSE.                     *  RL1084.2
002200*        X-82  - SOURCE COMPUTER NAME.                         *  RL1084.2
002300*        X-83  - OBJECT COMPUTER NAME.                         *  RL1084.2
002400*                                                              *  RL1084.2
002500****************************************************************  RL1084.2
002600*RL108A *                                                         RL1084.2
002700***************************************************               RL1084.2
002800*GENERAL:    THIS RUN UNIT IS THE FIRST OF A SERIES WHICH         RL1084.2
002900*            PROCESSES A RELATIVE I-O FILE.  THE FUNCTION OF THIS RL1084.2
003000*            PROGRAM IS TO CREATE A RELATIVE FILE SEQUENTIALLY    RL1084.2
003100*            (ACCESS MODE SEQUENTIAL) AND VERIFY THAT IT WAS      RL1084.2
003200*            CREATED CORRECTLY.  THE FILE IS IDENTIFED AS "RL-FS1"RL1084.2
003300*            AND IS PASSED TO SUBSEQUENT RUN UNITS FOR PROCESSING.RL1084.2
003400*                                                                 RL1084.2
003500*    THIS PROGRAM TESTS THE NEW SYNTACTICAL CONSTRUCTS AND        RL1084.2
003600*    SEMANTIC ACTIONS OF THE FOLLOWING ELEMENTS:                  RL1084.2
003700*        - ASSIGN                                                 RL1084.2
003800*        - ORGANIZATION                                           RL1084.2
003900*        - ACCESS                                                 RL1084.2
004000*        - READ                                                   RL1084.2
004100*        - WRITE                                                  RL1084.2
004200****************************************************************  RL1084.2
004300 ENVIRONMENT DIVISION.                                            RL1084.2
004400 CONFIGURATION SECTION.                                           RL1084.2
004500 SOURCE-COMPUTER.                                                 RL1084.2
004600     XXXXX082.                                                    RL1084.2
004700 OBJECT-COMPUTER.                                                 RL1084.2
004800     XXXXX083.                                                    RL1084.2
004900 INPUT-OUTPUT SECTION.                                            RL1084.2
005000 FILE-CONTROL.                                                    RL1084.2
005100     SELECT PRINT-FILE ASSIGN TO                                  RL1084.2
005200     XXXXX055.                                                    RL1084.2
005300     SELECT   RL-FS1 ASSIGN TO                                    RL1084.2
005400     XXXXX061                                                     RL1084.2
005500             ORGANIZATION RELATIVE                                RL1084.2
005600             ACCESS SEQUENTIAL.                                   RL1084.2
005700*                                                                 RL1084.2
005800 DATA DIVISION.                                                   RL1084.2
005900 FILE SECTION.                                                    RL1084.2
006000 FD  PRINT-FILE.                                                  RL1084.2
006100 01  PRINT-REC PICTURE X(120).                                    RL1084.2
006200 01  DUMMY-RECORD PICTURE X(120).                                 RL1084.2
006300 FD  RL-FS1                                                       RL1084.2
006400     LABEL RECORDS STANDARD                                       RL1084.2
006500C    VALUE OF                                                     RL1084.2
006600C    XXXXX074                                                     RL1084.2
006700C    IS                                                           RL1084.2
006800C    XXXXX075                                                     RL1084.2
006900*    XXXXX069                                                     RL1084.2
007000     BLOCK CONTAINS 1 RECORDS                                     RL1084.2
007100     RECORD CONTAINS 120 CHARACTERS.                              RL1084.2
007200 01  RL-FS1R1-F-G-120.                                            RL1084.2
007300     02 FILLER PIC X(120).                                        RL1084.2
007400 WORKING-STORAGE SECTION.                                         RL1084.2
007500 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                RL1084.2
007600 01  FILE-RECORD-INFORMATION-REC.                                 RL1084.2
007700     03 FILE-RECORD-INFO-SKELETON.                                RL1084.2
007800        05 FILLER                 PICTURE X(48)       VALUE       RL1084.2
007900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1084.2
008000        05 FILLER                 PICTURE X(46)       VALUE       RL1084.2
008100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1084.2
008200        05 FILLER                 PICTURE X(26)       VALUE       RL1084.2
008300             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1084.2
008400        05 FILLER                 PICTURE X(37)       VALUE       RL1084.2
008500             ",RECKEY=                             ".             RL1084.2
008600        05 FILLER                 PICTURE X(38)       VALUE       RL1084.2
008700             ",ALTKEY1=                             ".            RL1084.2
008800        05 FILLER                 PICTURE X(38)       VALUE       RL1084.2
008900             ",ALTKEY2=                             ".            RL1084.2
009000        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1084.2
009100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1084.2
009200        05 FILE-RECORD-INFO-P1-120.                               RL1084.2
009300           07 FILLER              PIC X(5).                       RL1084.2
009400           07 XFILE-NAME           PIC X(6).                      RL1084.2
009500           07 FILLER              PIC X(8).                       RL1084.2
009600           07 XRECORD-NAME         PIC X(6).                      RL1084.2
009700           07 FILLER              PIC X(1).                       RL1084.2
009800           07 REELUNIT-NUMBER     PIC 9(1).                       RL1084.2
009900           07 FILLER              PIC X(7).                       RL1084.2
010000           07 XRECORD-NUMBER       PIC 9(6).                      RL1084.2
010100           07 FILLER              PIC X(6).                       RL1084.2
010200           07 UPDATE-NUMBER       PIC 9(2).                       RL1084.2
010300           07 FILLER              PIC X(5).                       RL1084.2
010400           07 ODO-NUMBER          PIC 9(4).                       RL1084.2
010500           07 FILLER              PIC X(5).                       RL1084.2
010600           07 XPROGRAM-NAME        PIC X(5).                      RL1084.2
010700           07 FILLER              PIC X(7).                       RL1084.2
010800           07 XRECORD-LENGTH       PIC 9(6).                      RL1084.2
010900           07 FILLER              PIC X(7).                       RL1084.2
011000           07 CHARS-OR-RECORDS    PIC X(2).                       RL1084.2
011100           07 FILLER              PIC X(1).                       RL1084.2
011200           07 XBLOCK-SIZE          PIC 9(4).                      RL1084.2
011300           07 FILLER              PIC X(6).                       RL1084.2
011400           07 RECORDS-IN-FILE     PIC 9(6).                       RL1084.2
011500           07 FILLER              PIC X(5).                       RL1084.2
011600           07 XFILE-ORGANIZATION   PIC X(2).                      RL1084.2
011700           07 FILLER              PIC X(6).                       RL1084.2
011800           07 XLABEL-TYPE          PIC X(1).                      RL1084.2
011900        05 FILE-RECORD-INFO-P121-240.                             RL1084.2
012000           07 FILLER              PIC X(8).                       RL1084.2
012100           07 XRECORD-KEY          PIC X(29).                     RL1084.2
012200           07 FILLER              PIC X(9).                       RL1084.2
012300           07 ALTERNATE-KEY1      PIC X(29).                      RL1084.2
012400           07 FILLER              PIC X(9).                       RL1084.2
012500           07 ALTERNATE-KEY2      PIC X(29).                      RL1084.2
012600           07 FILLER              PIC X(7).                       RL1084.2
012700 01  TEST-RESULTS.                                                RL1084.2
012800     02 FILLER                   PIC X      VALUE SPACE.          RL1084.2
012900     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1084.2
013000     02 FILLER                   PIC X      VALUE SPACE.          RL1084.2
013100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1084.2
013200     02 FILLER                   PIC X      VALUE SPACE.          RL1084.2
013300     02  PAR-NAME.                                                RL1084.2
013400       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1084.2
013500       03  PARDOT-X              PIC X      VALUE SPACE.          RL1084.2
013600       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1084.2
013700     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1084.2
013800     02 RE-MARK                  PIC X(61).                       RL1084.2
013900 01  TEST-COMPUTED.                                               RL1084.2
014000     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1084.2
014100     02 FILLER                   PIC X(17)  VALUE                 RL1084.2
014200            "       COMPUTED=".                                   RL1084.2
014300     02 COMPUTED-X.                                               RL1084.2
014400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1084.2
014500     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1084.2
014600                                 PIC -9(9).9(9).                  RL1084.2
014700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1084.2
014800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1084.2
014900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1084.2
015000     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1084.2
015100         04 COMPUTED-18V0                    PIC -9(18).          RL1084.2
015200         04 FILLER                           PIC X.               RL1084.2
015300     03 FILLER PIC X(50) VALUE SPACE.                             RL1084.2
015400 01  TEST-CORRECT.                                                RL1084.2
015500     02 FILLER PIC X(30) VALUE SPACE.                             RL1084.2
015600     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1084.2
015700     02 CORRECT-X.                                                RL1084.2
015800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1084.2
015900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1084.2
016000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1084.2
016100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1084.2
016200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1084.2
016300     03      CR-18V0 REDEFINES CORRECT-A.                         RL1084.2
016400         04 CORRECT-18V0                     PIC -9(18).          RL1084.2
016500         04 FILLER                           PIC X.               RL1084.2
016600     03 FILLER PIC X(2) VALUE SPACE.                              RL1084.2
016700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1084.2
016800 01  CCVS-C-1.                                                    RL1084.2
016900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1084.2
017000-    "SS  PARAGRAPH-NAME                                          RL1084.2
017100-    "       REMARKS".                                            RL1084.2
017200     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1084.2
017300 01  CCVS-C-2.                                                    RL1084.2
017400     02 FILLER                     PIC X        VALUE SPACE.      RL1084.2
017500     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1084.2
017600     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1084.2
017700     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1084.2
017800     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1084.2
017900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1084.2
018000 01  REC-CT                        PIC 99       VALUE ZERO.       RL1084.2
018100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1084.2
018200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1084.2
018300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1084.2
018400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1084.2
018500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1084.2
018600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1084.2
018700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1084.2
018800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1084.2
018900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1084.2
019000 01  CCVS-H-1.                                                    RL1084.2
019100     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1084.2
019200     02  FILLER                    PIC X(42)    VALUE             RL1084.2
019300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1084.2
019400     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1084.2
019500 01  CCVS-H-2A.                                                   RL1084.2
019600   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1084.2
019700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1084.2
019800   02  FILLER                        PIC XXXX   VALUE             RL1084.2
019900     "4.2 ".                                                      RL1084.2
020000   02  FILLER                        PIC X(28)  VALUE             RL1084.2
020100            " COPY - NOT FOR DISTRIBUTION".                       RL1084.2
020200   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1084.2
020300                                                                  RL1084.2
020400 01  CCVS-H-2B.                                                   RL1084.2
020500   02  FILLER                        PIC X(15)  VALUE             RL1084.2
020600            "TEST RESULT OF ".                                    RL1084.2
020700   02  TEST-ID                       PIC X(9).                    RL1084.2
020800   02  FILLER                        PIC X(4)   VALUE             RL1084.2
020900            " IN ".                                               RL1084.2
021000   02  FILLER                        PIC X(12)  VALUE             RL1084.2
021100     " HIGH       ".                                              RL1084.2
021200   02  FILLER                        PIC X(22)  VALUE             RL1084.2
021300            " LEVEL VALIDATION FOR ".                             RL1084.2
021400   02  FILLER                        PIC X(58)  VALUE             RL1084.2
021500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1084.2
021600 01  CCVS-H-3.                                                    RL1084.2
021700     02  FILLER                      PIC X(34)  VALUE             RL1084.2
021800            " FOR OFFICIAL USE ONLY    ".                         RL1084.2
021900     02  FILLER                      PIC X(58)  VALUE             RL1084.2
022000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1084.2
022100     02  FILLER                      PIC X(28)  VALUE             RL1084.2
022200            "  COPYRIGHT   1985 ".                                RL1084.2
022300 01  CCVS-E-1.                                                    RL1084.2
022400     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1084.2
022500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1084.2
022600     02 ID-AGAIN                     PIC X(9).                    RL1084.2
022700     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1084.2
022800 01  CCVS-E-2.                                                    RL1084.2
022900     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1084.2
023000     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1084.2
023100     02 CCVS-E-2-2.                                               RL1084.2
023200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1084.2
023300         03 FILLER                   PIC X      VALUE SPACE.      RL1084.2
023400         03 ENDER-DESC               PIC X(44)  VALUE             RL1084.2
023500            "ERRORS ENCOUNTERED".                                 RL1084.2
023600 01  CCVS-E-3.                                                    RL1084.2
023700     02  FILLER                      PIC X(22)  VALUE             RL1084.2
023800            " FOR OFFICIAL USE ONLY".                             RL1084.2
023900     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1084.2
024000     02  FILLER                      PIC X(58)  VALUE             RL1084.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1084.2
024200     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1084.2
024300     02 FILLER                       PIC X(15)  VALUE             RL1084.2
024400             " COPYRIGHT 1985".                                   RL1084.2
024500 01  CCVS-E-4.                                                    RL1084.2
024600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1084.2
024700     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1084.2
024800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1084.2
024900     02 FILLER                       PIC X(40)  VALUE             RL1084.2
025000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1084.2
025100 01  XXINFO.                                                      RL1084.2
025200     02 FILLER                       PIC X(19)  VALUE             RL1084.2
025300            "*** INFORMATION ***".                                RL1084.2
025400     02 INFO-TEXT.                                                RL1084.2
025500       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1084.2
025600       04 XXCOMPUTED                 PIC X(20).                   RL1084.2
025700       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1084.2
025800       04 XXCORRECT                  PIC X(20).                   RL1084.2
025900     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1084.2
026000 01  HYPHEN-LINE.                                                 RL1084.2
026100     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1084.2
026200     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1084.2
026300-    "*****************************************".                 RL1084.2
026400     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1084.2
026500-    "******************************".                            RL1084.2
026600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1084.2
026700     "RL108A".                                                    RL1084.2
026800 PROCEDURE DIVISION.                                              RL1084.2
026900 CCVS1 SECTION.                                                   RL1084.2
027000 OPEN-FILES.                                                      RL1084.2
027100     OPEN    OUTPUT PRINT-FILE.                                   RL1084.2
027200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1084.2
027300     MOVE    SPACE TO TEST-RESULTS.                               RL1084.2
027400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1084.2
027500     MOVE    ZERO TO REC-SKL-SUB.                                 RL1084.2
027600     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1084.2
027700 CCVS-INIT-FILE.                                                  RL1084.2
027800     ADD     1 TO REC-SKL-SUB.                                    RL1084.2
027900     MOVE    FILE-RECORD-INFO-SKELETON                            RL1084.2
028000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1084.2
028100 CCVS-INIT-EXIT.                                                  RL1084.2
028200     GO TO CCVS1-EXIT.                                            RL1084.2
028300 CLOSE-FILES.                                                     RL1084.2
028400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1084.2
028500 TERMINATE-CCVS.                                                  RL1084.2
028600*    EXIT PROGRAM.                                                RL1084.2
028700*TERMINATE-CALL.                                                  RL1084.2
028800     STOP     RUN.                                                RL1084.2
028900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1084.2
029000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1084.2
029100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1084.2
029200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1084.2
029300     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1084.2
029400 PRINT-DETAIL.                                                    RL1084.2
029500     IF REC-CT NOT EQUAL TO ZERO                                  RL1084.2
029600             MOVE "." TO PARDOT-X                                 RL1084.2
029700             MOVE REC-CT TO DOTVALUE.                             RL1084.2
029800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1084.2
029900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1084.2
030000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1084.2
030100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1084.2
030200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1084.2
030300     MOVE SPACE TO CORRECT-X.                                     RL1084.2
030400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1084.2
030500     MOVE     SPACE TO RE-MARK.                                   RL1084.2
030600 HEAD-ROUTINE.                                                    RL1084.2
030700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1084.2
030800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1084.2
030900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1084.2
031000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1084.2
031100 COLUMN-NAMES-ROUTINE.                                            RL1084.2
031200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1084.2
031300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1084.2
031400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1084.2
031500 END-ROUTINE.                                                     RL1084.2
031600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1084.2
031700 END-RTN-EXIT.                                                    RL1084.2
031800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1084.2
031900 END-ROUTINE-1.                                                   RL1084.2
032000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1084.2
032100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1084.2
032200      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1084.2
032300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1084.2
032400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1084.2
032500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1084.2
032600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1084.2
032700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1084.2
032800  END-ROUTINE-12.                                                 RL1084.2
032900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1084.2
033000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1084.2
033100         MOVE "NO " TO ERROR-TOTAL                                RL1084.2
033200         ELSE                                                     RL1084.2
033300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1084.2
033400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1084.2
033500     PERFORM WRITE-LINE.                                          RL1084.2
033600 END-ROUTINE-13.                                                  RL1084.2
033700     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1084.2
033800         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1084.2
033900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1084.2
034000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1084.2
034100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1084.2
034200      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1084.2
034300          MOVE "NO " TO ERROR-TOTAL                               RL1084.2
034400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1084.2
034500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1084.2
034600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1084.2
034700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1084.2
034800 WRITE-LINE.                                                      RL1084.2
034900     ADD 1 TO RECORD-COUNT.                                       RL1084.2
035000*    IF RECORD-COUNT GREATER 50                                   RL1084.2
035100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1084.2
035200*        MOVE SPACE TO DUMMY-RECORD                               RL1084.2
035300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1084.2
035400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1084.2
035500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1084.2
035600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1084.2
035700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1084.2
035800*        MOVE ZERO TO RECORD-COUNT.                               RL1084.2
035900     PERFORM WRT-LN.                                              RL1084.2
036000 WRT-LN.                                                          RL1084.2
036100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1084.2
036200     MOVE SPACE TO DUMMY-RECORD.                                  RL1084.2
036300 BLANK-LINE-PRINT.                                                RL1084.2
036400     PERFORM WRT-LN.                                              RL1084.2
036500 FAIL-ROUTINE.                                                    RL1084.2
036600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. RL1084.2
036700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1084.2
036800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1084.2
036900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1084.2
037000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1084.2
037100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1084.2
037200     GO TO  FAIL-ROUTINE-EX.                                      RL1084.2
037300 FAIL-ROUTINE-WRITE.                                              RL1084.2
037400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1084.2
037500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1084.2
037600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1084.2
037700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1084.2
037800 FAIL-ROUTINE-EX. EXIT.                                           RL1084.2
037900 BAIL-OUT.                                                        RL1084.2
038000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1084.2
038100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1084.2
038200 BAIL-OUT-WRITE.                                                  RL1084.2
038300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1084.2
038400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1084.2
038500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1084.2
038600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1084.2
038700 BAIL-OUT-EX. EXIT.                                               RL1084.2
038800 CCVS1-EXIT.                                                      RL1084.2
038900     EXIT.                                                        RL1084.2
039000 SECT-RL108A-001 SECTION.                                         RL1084.2
039100 REL-INIT-001.                                                    RL1084.2
039200     MOVE     "FILE CREATE RL-FS1" TO FEATURE.                    RL1084.2
039300     OPEN     OUTPUT    RL-FS1.                                   RL1084.2
039400     MOVE     "RL-FS1" TO XFILE-NAME (1).                         RL1084.2
039500     MOVE     "R1-F-G" TO XRECORD-NAME (1).                       RL1084.2
039600     MOVE CCVS-PGM-ID  TO XPROGRAM-NAME (1).                      RL1084.2
039700     MOVE     000120   TO XRECORD-LENGTH (1).                     RL1084.2
039800     MOVE     "RC"     TO CHARS-OR-RECORDS (1).                   RL1084.2
039900     MOVE     0001     TO XBLOCK-SIZE (1).                        RL1084.2
040000     MOVE     000500   TO RECORDS-IN-FILE (1).                    RL1084.2
040100     MOVE     "RL"     TO XFILE-ORGANIZATION (1).                 RL1084.2
040200     MOVE     "S"      TO XLABEL-TYPE (1).                        RL1084.2
040300     MOVE     000001   TO XRECORD-NUMBER (1).                     RL1084.2
040400 REL-TEST-001.                                                    RL1084.2
040500     MOVE    FILE-RECORD-INFO-P1-120 (1) TO RL-FS1R1-F-G-120.     RL1084.2
040600     WRITE   RL-FS1R1-F-G-120                                     RL1084.2
040700             INVALID KEY GO TO REL-FAIL-001                       RL1084.2
040800         NOT INVALID KEY GO TO REL-TEST-001-A                     RL1084.2
040900     END-WRITE.                                                   RL1084.2
041000 REL-TEST-001-A.                                                  RL1084.2
041100     IF      XRECORD-NUMBER (1) EQUAL TO 500                      RL1084.2
041200             GO TO REL-WRITE-001.                                 RL1084.2
041300     ADD     000001 TO XRECORD-NUMBER (1).                        RL1084.2
041400     GO      TO REL-TEST-001.                                     RL1084.2
041500 REL-DELETE-001.                                                  RL1084.2
041600     PERFORM DE-LETE.                                             RL1084.2
041700     GO TO   REL-WRITE-001.                                       RL1084.2
041800 REL-FAIL-001.                                                    RL1084.2
041900     PERFORM FAIL.                                                RL1084.2
042000     MOVE   "BOUNDARY VIOLATION"  TO RE-MARK.                     RL1084.2
042100 REL-WRITE-001.                                                   RL1084.2
042200     MOVE     "VIII-37 4.9.4(A)"   TO ANSI-REFERENCE.             RL1084.2
042300     MOVE   "REL-TEST-001" TO   PAR-NAME                          RL1084.2
042400     MOVE   "FILE CREATED, LFILE "  TO COMPUTED-A.                RL1084.2
042500     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL1084.2
042600     PERFORM PRINT-DETAIL.                                        RL1084.2
042700     CLOSE   RL-FS1.                                              RL1084.2
042800 REL-INIT-002.                                                    RL1084.2
042900     OPEN    INPUT     RL-FS1.                                    RL1084.2
043000     MOVE    ZERO      TO WRK-CS-09V00.                           RL1084.2
043100 REL-TEST-002.                                                    RL1084.2
043200     READ    RL-FS1                                               RL1084.2
043300             AT END GO TO REL-TEST-002-1                          RL1084.2
043400         NOT AT END GO TO REL-TEST-002-A                          RL1084.2
043500     END-READ.                                                    RL1084.2
043600 REL-TEST-002-A.                                                  RL1084.2
043700     MOVE    RL-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).     RL1084.2
043800     ADD     1 TO WRK-CS-09V00.                                   RL1084.2
043900     IF      WRK-CS-09V00 GREATER 500                             RL1084.2
044000             MOVE "MORE THAN 500 RECORDS" TO RE-MARK              RL1084.2
044100             GO TO REL-TEST-002-1.                                RL1084.2
044200     GO      TO REL-TEST-002.                                     RL1084.2
044300 REL-DELETE-002.                                                  RL1084.2
044400     PERFORM DE-LETE.                                             RL1084.2
044500     PERFORM PRINT-DETAIL.                                        RL1084.2
044600     GO TO   CCVS-EXIT.                                           RL1084.2
044700 REL-TEST-002-1.                                                  RL1084.2
044800     IF      XRECORD-NUMBER (1) NOT EQUAL TO 500                  RL1084.2
044900             PERFORM FAIL                                         RL1084.2
045000     ELSE                                                         RL1084.2
045100             PERFORM PASS.                                        RL1084.2
045200     GO      TO REL-WRITE-002.                                    RL1084.2
045300 REL-WRITE-002.                                                   RL1084.2
045400     MOVE   "VIII-26 4.5.4" TO ANSI-REFERENCE.                    RL1084.2
045500     MOVE   "REL-TEST-002"  TO PAR-NAME.                          RL1084.2
045600     MOVE   "FILE VERIFIED, LFILE" TO COMPUTED-A.                 RL1084.2
045700     MOVE    XRECORD-NUMBER (1) TO CORRECT-18V0.                  RL1084.2
045800     PERFORM PRINT-DETAIL.                                        RL1084.2
045900     CLOSE   RL-FS1.                                              RL1084.2
046000 CCVS-EXIT SECTION.                                               RL1084.2
046100 CCVS-999999.                                                     RL1084.2
046200     GO TO CLOSE-FILES.                                           RL1084.2
      *END-OF,RL108A                                                                  
