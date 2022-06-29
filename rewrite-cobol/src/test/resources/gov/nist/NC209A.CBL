      *HEADER,COBOL,NC209A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2094.2
000200 PROGRAM-ID.                                                      NC2094.2
000300     NC209A.                                                      NC2094.2
000400*                                                              *  NC2094.2
000500****************************************************************  NC2094.2
000600*                                                              *  NC2094.2
000700*    VALIDATION FOR:-                                          *  NC2094.2
000800*                                                              *  NC2094.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2094.2
001000*                                                              *  NC2094.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2094.2
001200*                                                              *  NC2094.2
001300****************************************************************  NC2094.2
001400*                                                              *  NC2094.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2094.2
001600*                                                              *  NC2094.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2094.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2094.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2094.2
002000*                                                              *  NC2094.2
002100****************************************************************  NC2094.2
002200*    PROGRAM NC209A TESTS FORMAT 2 OF THE "MOVE" STATEMENT,    *  NC2094.2
002300*    USING QUALIFIED AND SUBSCRIPTED IDENTIFIERS.              *  NC2094.2
002400*                                                              *  NC2094.2
002500****************************************************************  NC2094.2
002600 ENVIRONMENT DIVISION.                                            NC2094.2
002700 CONFIGURATION SECTION.                                           NC2094.2
002800 SOURCE-COMPUTER.                                                 NC2094.2
002900     XXXXX082.                                                    NC2094.2
003000 OBJECT-COMPUTER.                                                 NC2094.2
003100     XXXXX083.                                                    NC2094.2
003200 INPUT-OUTPUT SECTION.                                            NC2094.2
003300 FILE-CONTROL.                                                    NC2094.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC2094.2
003500     XXXXX055.                                                    NC2094.2
003600 DATA DIVISION.                                                   NC2094.2
003700 FILE SECTION.                                                    NC2094.2
003800 FD  PRINT-FILE.                                                  NC2094.2
003900 01  PRINT-REC PICTURE X(120).                                    NC2094.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC2094.2
004100 WORKING-STORAGE SECTION.                                         NC2094.2
004200 01  A-LEVEL.                                                     NC2094.2
004300     02 B-LEVEL.                                                  NC2094.2
004400         03 C-LEVEL.                                              NC2094.2
004500             04 D-LEVEL.                                          NC2094.2
004600                 05 TOM           PICTURE XXX  VALUE "TOM".       NC2094.2
004700                 05 DICK          PICTURE XXXX VALUE "DICK".      NC2094.2
004800             04 DD-LEVEL.                                         NC2094.2
004900                 05 HARRY         PICTURE X(5) VALUE "HARRY".     NC2094.2
005000             04 DDD-LEVEL.                                        NC2094.2
005100                 05 JOE           PICTURE XXX  VALUE "JOE".       NC2094.2
005200     02 AL                        PICTURE XX   VALUE "AL".        NC2094.2
005300     02 BB-LEVEL.                                                 NC2094.2
005400             04 BOB               PICTURE XXX  VALUE "BOB".       NC2094.2
005500 01  A-GROUP.                                                     NC2094.2
005600     02 B-GROUP.                                                  NC2094.2
005700         10 C-LEVEL.                                              NC2094.2
005800             12 D-LEVEL.                                          NC2094.2
005900                 13 TOM           PICTURE XXX  VALUE "ZZZ".       NC2094.2
006000                 13 DICK          PICTURE XXXX VALUE "ZZZZ".      NC2094.2
006100             12 DD-LEVEL-FALSE.                                   NC2094.2
006200                 13 HARRY         PICTURE X(5) VALUE "ZZZZZ".     NC2094.2
006300             12 DDD-LEVEL.                                        NC2094.2
006400                 13 JOE           PICTURE XXX  VALUE "ZZZ".       NC2094.2
006500 01  A-BUNCH.                                                     NC2094.2
006600     49 TOM   PICTURE XXX  VALUE "YYY".                           NC2094.2
006700     49 DICK  PICTURE XXXX VALUE "YYYY".                          NC2094.2
006800     49 HARRY PICTURE X(5) VALUE "YYYYY".                         NC2094.2
006900     49 JOE   PICTURE XXX  VALUE "YYY".                           NC2094.2
007000     49 AL    PICTURE XX   VALUE "YY".                            NC2094.2
007100     49 BOB   PICTURE XXX  VALUE "YYY".                           NC2094.2
007200 01  A-SET.                                                       NC2094.2
007300     02 B-SET.                                                    NC2094.2
007400             04 D-LEVEL.                                          NC2094.2
007500                 05 TOM           PICTURE XXX  VALUE "WWW".       NC2094.2
007600                 05 DICK          PICTURE XXXX VALUE "WWWW".      NC2094.2
007700             04 HARRY             PICTURE X(5) VALUE "WWWWW".     NC2094.2
007800             04 BOB               PICTURE XXX  VALUE "WWW".       NC2094.2
007900 01  C-STACK.                                                     NC2094.2
008000     04 D-LEVEL.                                                  NC2094.2
008100         05 TOM         PICTURE XXX  VALUE "VVV".                 NC2094.2
008200     04 DD-LEVEL.                                                 NC2094.2
008300         05 DICK        PICTURE XXXX VALUE "VVVV".                NC2094.2
008400         05 HARRY       PICTURE X(5) VALUE "VVVVV".               NC2094.2
008500 01  A-GLOB.                                                      NC2094.2
008600     02 B-LEVEL.                                                  NC2094.2
008700         03 C-LEVEL.                                              NC2094.2
008800             04 D-LEVEL.                                          NC2094.2
008900                 05 TOM           PICTURE XXX  VALUE "UUU".       NC2094.2
009000                 05 DICK          PICTURE XXXX VALUE "UUUU".      NC2094.2
009100             04  DD-LEVEL.                                        NC2094.2
009200                 05 HARRY-A       PICTURE XX   VALUE "UU".        NC2094.2
009300                 05 HARRY-B       PICTURE XXX  VALUE "UUU".       NC2094.2
009400             04 DDD-LEVEL.                                        NC2094.2
009500                 05 JOE           PICTURE XXX  VALUE "UUU".       NC2094.2
009600     02 AL                        PICTURE XX   VALUE "UU".        NC2094.2
009700     02 BB-LEVEL-FALSE.                                           NC2094.2
009800             04 BOB               PICTURE XXX  VALUE "UUU".       NC2094.2
009900      66         AL-BOB RENAMES AL OF A-GLOB THRU BOB OF A-GLOB.  NC2094.2
010000     66 HARRY RENAMES HARRY-A THRU HARRY-B.                       NC2094.2
010100 01  A-COLLECTION.                                                NC2094.2
010200     02 B-COLLECTION.                                             NC2094.2
010300         03 C-COLLECTION.                                         NC2094.2
010400             04 D-LEVEL.                                          NC2094.2
010500                 05 TOM OCCURS 3 TIMES      PICTURE X.            NC2094.2
010600                 05 DICK.                                         NC2094.2
010700                     06 RICHARD OCCURS 2    PICTURE XX.           NC2094.2
010800             04 DD-LEVEL-FALSE              PICTURE 9(5).         NC2094.2
010900             04 DD-LEVEL REDEFINES DD-LEVEL-FALSE.                NC2094.2
011000                 05 HARRY                   PICTURE X(5).         NC2094.2
011100             04 DDD-LEVEL.                                        NC2094.2
011200                 05 JOE                     PICTURE XXX.          NC2094.2
011300                 05 JOSEPH REDEFINES JOE    PICTURE 999.          NC2094.2
011400 01  WORK-AREA.                                                   NC2094.2
011500     02 WORK-TOM PICTURE XXX.                                     NC2094.2
011600     02 FILLER PICTURE XXXX.                                      NC2094.2
011700 01  A-COVEY.                                                     NC2094.2
011800     02 FILLER                    PICTURE X(45).                  NC2094.2
011900     02 B-COVEY.                                                  NC2094.2
012000         03 TOMMY                 PICTURE XXX  VALUE "SSS".       NC2094.2
012100         03 DICKY                 PICTURE XXXX VALUE "SSSS".      NC2094.2
012200         03 JOEY                  PICTURE XXX  VALUE "SSS".       NC2094.2
012300         03 HAROLD                PICTURE X(5) VALUE "SSSSS".     NC2094.2
012400 01  A-FLOCK REDEFINES A-COVEY.                                   NC2094.2
012500     02 B-FLOCK OCCURS 4 TIMES.                                   NC2094.2
012600         03 C-FLOCK.                                              NC2094.2
012700             04 D-LEVEL.                                          NC2094.2
012800                 05 TOM           PICTURE XXX.                    NC2094.2
012900                 05 DICK          PICTURE XXXX.                   NC2094.2
013000             04 DDD-LEVEL.                                        NC2094.2
013100                 05 JOE           PICTURE XXX.                    NC2094.2
013200             04 DD-LEVEL.                                         NC2094.2
013300                 05 HARRY         PICTURE X(5).                   NC2094.2
013400 01  A-CROWD.                                                     NC2094.2
013500     02 BB-CROWD.                                                 NC2094.2
013600         03 BOBBY       PICTURE XXX VALUE "RRR".                  NC2094.2
013700         03 FILLER      PICTURE X(15).                            NC2094.2
013800     02 BB-MOB REDEFINES BB-CROWD OCCURS 6 TIMES.                 NC2094.2
013900         03 BOB         PICTURE XXX.                              NC2094.2
014000 01  TEST-RESULTS.                                                NC2094.2
014100     02 FILLER                   PIC X      VALUE SPACE.          NC2094.2
014200     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2094.2
014300     02 FILLER                   PIC X      VALUE SPACE.          NC2094.2
014400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2094.2
014500     02 FILLER                   PIC X      VALUE SPACE.          NC2094.2
014600     02  PAR-NAME.                                                NC2094.2
014700       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2094.2
014800       03  PARDOT-X              PIC X      VALUE SPACE.          NC2094.2
014900       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2094.2
015000     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2094.2
015100     02 RE-MARK                  PIC X(61).                       NC2094.2
015200 01  TEST-COMPUTED.                                               NC2094.2
015300     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2094.2
015400     02 FILLER                   PIC X(17)  VALUE                 NC2094.2
015500            "       COMPUTED=".                                   NC2094.2
015600     02 COMPUTED-X.                                               NC2094.2
015700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2094.2
015800     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2094.2
015900                                 PIC -9(9).9(9).                  NC2094.2
016000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2094.2
016100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2094.2
016200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2094.2
016300     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2094.2
016400         04 COMPUTED-18V0                    PIC -9(18).          NC2094.2
016500         04 FILLER                           PIC X.               NC2094.2
016600     03 FILLER PIC X(50) VALUE SPACE.                             NC2094.2
016700 01  TEST-CORRECT.                                                NC2094.2
016800     02 FILLER PIC X(30) VALUE SPACE.                             NC2094.2
016900     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2094.2
017000     02 CORRECT-X.                                                NC2094.2
017100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2094.2
017200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2094.2
017300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2094.2
017400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2094.2
017500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2094.2
017600     03      CR-18V0 REDEFINES CORRECT-A.                         NC2094.2
017700         04 CORRECT-18V0                     PIC -9(18).          NC2094.2
017800         04 FILLER                           PIC X.               NC2094.2
017900     03 FILLER PIC X(2) VALUE SPACE.                              NC2094.2
018000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2094.2
018100 01  CCVS-C-1.                                                    NC2094.2
018200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2094.2
018300-    "SS  PARAGRAPH-NAME                                          NC2094.2
018400-    "       REMARKS".                                            NC2094.2
018500     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2094.2
018600 01  CCVS-C-2.                                                    NC2094.2
018700     02 FILLER                     PIC X        VALUE SPACE.      NC2094.2
018800     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2094.2
018900     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2094.2
019000     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2094.2
019100     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2094.2
019200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2094.2
019300 01  REC-CT                        PIC 99       VALUE ZERO.       NC2094.2
019400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2094.2
019500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2094.2
019600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2094.2
019700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2094.2
019800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2094.2
019900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2094.2
020000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2094.2
020100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2094.2
020200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2094.2
020300 01  CCVS-H-1.                                                    NC2094.2
020400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2094.2
020500     02  FILLER                    PIC X(42)    VALUE             NC2094.2
020600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2094.2
020700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2094.2
020800 01  CCVS-H-2A.                                                   NC2094.2
020900   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2094.2
021000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2094.2
021100   02  FILLER                        PIC XXXX   VALUE             NC2094.2
021200     "4.2 ".                                                      NC2094.2
021300   02  FILLER                        PIC X(28)  VALUE             NC2094.2
021400            " COPY - NOT FOR DISTRIBUTION".                       NC2094.2
021500   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2094.2
021600                                                                  NC2094.2
021700 01  CCVS-H-2B.                                                   NC2094.2
021800   02  FILLER                        PIC X(15)  VALUE             NC2094.2
021900            "TEST RESULT OF ".                                    NC2094.2
022000   02  TEST-ID                       PIC X(9).                    NC2094.2
022100   02  FILLER                        PIC X(4)   VALUE             NC2094.2
022200            " IN ".                                               NC2094.2
022300   02  FILLER                        PIC X(12)  VALUE             NC2094.2
022400     " HIGH       ".                                              NC2094.2
022500   02  FILLER                        PIC X(22)  VALUE             NC2094.2
022600            " LEVEL VALIDATION FOR ".                             NC2094.2
022700   02  FILLER                        PIC X(58)  VALUE             NC2094.2
022800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2094.2
022900 01  CCVS-H-3.                                                    NC2094.2
023000     02  FILLER                      PIC X(34)  VALUE             NC2094.2
023100            " FOR OFFICIAL USE ONLY    ".                         NC2094.2
023200     02  FILLER                      PIC X(58)  VALUE             NC2094.2
023300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2094.2
023400     02  FILLER                      PIC X(28)  VALUE             NC2094.2
023500            "  COPYRIGHT   1985 ".                                NC2094.2
023600 01  CCVS-E-1.                                                    NC2094.2
023700     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2094.2
023800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2094.2
023900     02 ID-AGAIN                     PIC X(9).                    NC2094.2
024000     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2094.2
024100 01  CCVS-E-2.                                                    NC2094.2
024200     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2094.2
024300     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2094.2
024400     02 CCVS-E-2-2.                                               NC2094.2
024500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2094.2
024600         03 FILLER                   PIC X      VALUE SPACE.      NC2094.2
024700         03 ENDER-DESC               PIC X(44)  VALUE             NC2094.2
024800            "ERRORS ENCOUNTERED".                                 NC2094.2
024900 01  CCVS-E-3.                                                    NC2094.2
025000     02  FILLER                      PIC X(22)  VALUE             NC2094.2
025100            " FOR OFFICIAL USE ONLY".                             NC2094.2
025200     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2094.2
025300     02  FILLER                      PIC X(58)  VALUE             NC2094.2
025400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2094.2
025500     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2094.2
025600     02 FILLER                       PIC X(15)  VALUE             NC2094.2
025700             " COPYRIGHT 1985".                                   NC2094.2
025800 01  CCVS-E-4.                                                    NC2094.2
025900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2094.2
026000     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2094.2
026100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2094.2
026200     02 FILLER                       PIC X(40)  VALUE             NC2094.2
026300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2094.2
026400 01  XXINFO.                                                      NC2094.2
026500     02 FILLER                       PIC X(19)  VALUE             NC2094.2
026600            "*** INFORMATION ***".                                NC2094.2
026700     02 INFO-TEXT.                                                NC2094.2
026800       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2094.2
026900       04 XXCOMPUTED                 PIC X(20).                   NC2094.2
027000       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2094.2
027100       04 XXCORRECT                  PIC X(20).                   NC2094.2
027200     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2094.2
027300 01  HYPHEN-LINE.                                                 NC2094.2
027400     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2094.2
027500     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2094.2
027600-    "*****************************************".                 NC2094.2
027700     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2094.2
027800-    "******************************".                            NC2094.2
027900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2094.2
028000     "NC209A".                                                    NC2094.2
028100 PROCEDURE DIVISION.                                              NC2094.2
028200 CCVS1 SECTION.                                                   NC2094.2
028300 OPEN-FILES.                                                      NC2094.2
028400     OPEN     OUTPUT PRINT-FILE.                                  NC2094.2
028500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2094.2
028600     MOVE    SPACE TO TEST-RESULTS.                               NC2094.2
028700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2094.2
028800     GO TO CCVS1-EXIT.                                            NC2094.2
028900 CLOSE-FILES.                                                     NC2094.2
029000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2094.2
029100 TERMINATE-CCVS.                                                  NC2094.2
029200*    EXIT PROGRAM.                                                NC2094.2
029300*TERMINATE-CALL.                                                  NC2094.2
029400     STOP     RUN.                                                NC2094.2
029500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2094.2
029600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2094.2
029700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2094.2
029800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2094.2
029900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2094.2
030000 PRINT-DETAIL.                                                    NC2094.2
030100     IF REC-CT NOT EQUAL TO ZERO                                  NC2094.2
030200             MOVE "." TO PARDOT-X                                 NC2094.2
030300             MOVE REC-CT TO DOTVALUE.                             NC2094.2
030400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2094.2
030500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2094.2
030600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2094.2
030700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2094.2
030800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2094.2
030900     MOVE SPACE TO CORRECT-X.                                     NC2094.2
031000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2094.2
031100     MOVE     SPACE TO RE-MARK.                                   NC2094.2
031200 HEAD-ROUTINE.                                                    NC2094.2
031300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2094.2
031400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2094.2
031500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2094.2
031600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2094.2
031700 COLUMN-NAMES-ROUTINE.                                            NC2094.2
031800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2094.2
031900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2094.2
032000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2094.2
032100 END-ROUTINE.                                                     NC2094.2
032200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2094.2
032300 END-RTN-EXIT.                                                    NC2094.2
032400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2094.2
032500 END-ROUTINE-1.                                                   NC2094.2
032600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2094.2
032700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2094.2
032800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2094.2
032900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2094.2
033000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2094.2
033100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2094.2
033200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2094.2
033300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2094.2
033400  END-ROUTINE-12.                                                 NC2094.2
033500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2094.2
033600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2094.2
033700         MOVE "NO " TO ERROR-TOTAL                                NC2094.2
033800         ELSE                                                     NC2094.2
033900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2094.2
034000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2094.2
034100     PERFORM WRITE-LINE.                                          NC2094.2
034200 END-ROUTINE-13.                                                  NC2094.2
034300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2094.2
034400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2094.2
034500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2094.2
034600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2094.2
034700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2094.2
034800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2094.2
034900          MOVE "NO " TO ERROR-TOTAL                               NC2094.2
035000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2094.2
035100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2094.2
035200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2094.2
035300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2094.2
035400 WRITE-LINE.                                                      NC2094.2
035500     ADD 1 TO RECORD-COUNT.                                       NC2094.2
035600*    IF RECORD-COUNT GREATER 50                                   NC2094.2
035700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2094.2
035800*        MOVE SPACE TO DUMMY-RECORD                               NC2094.2
035900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2094.2
036000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2094.2
036100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2094.2
036200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2094.2
036300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2094.2
036400*        MOVE ZERO TO RECORD-COUNT.                               NC2094.2
036500     PERFORM WRT-LN.                                              NC2094.2
036600 WRT-LN.                                                          NC2094.2
036700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2094.2
036800     MOVE SPACE TO DUMMY-RECORD.                                  NC2094.2
036900 BLANK-LINE-PRINT.                                                NC2094.2
037000     PERFORM WRT-LN.                                              NC2094.2
037100 FAIL-ROUTINE.                                                    NC2094.2
037200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2094.2
037300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2094.2
037400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2094.2
037500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2094.2
037600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2094.2
037700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2094.2
037800     GO TO  FAIL-ROUTINE-EX.                                      NC2094.2
037900 FAIL-ROUTINE-WRITE.                                              NC2094.2
038000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2094.2
038100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2094.2
038200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2094.2
038300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2094.2
038400 FAIL-ROUTINE-EX. EXIT.                                           NC2094.2
038500 BAIL-OUT.                                                        NC2094.2
038600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2094.2
038700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2094.2
038800 BAIL-OUT-WRITE.                                                  NC2094.2
038900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2094.2
039000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2094.2
039100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2094.2
039200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2094.2
039300 BAIL-OUT-EX. EXIT.                                               NC2094.2
039400 CCVS1-EXIT.                                                      NC2094.2
039500     EXIT.                                                        NC2094.2
039600 SECT-NC209A-001 SECTION.                                         NC2094.2
039700 NC-209A-001.                                                     NC2094.2
039800 MOV-INIT-F2-1.                                                   NC2094.2
039900     MOVE     "MOV-TEST-F2-1" TO PAR-NAME.                        NC2094.2
040000     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
040100     MOVE     "MOVE CORRESPONDING -" TO FEATURE.                  NC2094.2
040200     PERFORM  PRINT-DETAIL.                                       NC2094.2
040300     MOVE     "  DIFFERENT LEVELS  " TO FEATURE.                  NC2094.2
040400     MOVE     CORRESPONDING B-LEVEL OF A-LEVEL TO B-GROUP.        NC2094.2
040500     MOVE     1 TO REC-CT.                                        NC2094.2
040600*    TOM DICK AND JOE SHOULD BE MOVED.                            NC2094.2
040700*                                                                 NC2094.2
040800 MOV-TEST-F2-1-1.                                                 NC2094.2
040900     IF       TOM       OF A-GROUP          EQUAL TO "TOM"        NC2094.2
041000              PERFORM PASS                                        NC2094.2
041100              GO TO MOV-WRITE-F2-1-1.                             NC2094.2
041200     GO TO MOV-FAIL-F2-1-1.                                       NC2094.2
041300 MOV-DELETE-F2-1-1.                                               NC2094.2
041400     PERFORM  DE-LETE.                                            NC2094.2
041500     GO TO MOV-WRITE-F2-1-1.                                      NC2094.2
041600 MOV-FAIL-F2-1-1.                                                 NC2094.2
041700     PERFORM  FAIL.                                               NC2094.2
041800     MOVE     TOM     OF A-GROUP      TO COMPUTED-A.              NC2094.2
041900     MOVE     "TOM"   TO CORRECT-A.                               NC2094.2
042000 MOV-WRITE-F2-1-1.                                                NC2094.2
042100     PERFORM  PRINT-DETAIL.                                       NC2094.2
042200*                                                                 NC2094.2
042300 MOV-TEST-F2-1-2.                                                 NC2094.2
042400     ADD      1 TO REC-CT.                                        NC2094.2
042500     IF       DICK      OF A-GROUP          EQUAL TO "DICK"       NC2094.2
042600              PERFORM PASS                                        NC2094.2
042700              GO TO MOV-WRITE-F2-1-2.                             NC2094.2
042800     GO       TO MOV-FAIL-F2-1-2.                                 NC2094.2
042900 MOV-DELETE-F2-1-2.                                               NC2094.2
043000     PERFORM  DE-LETE.                                            NC2094.2
043100     GO       TO MOV-WRITE-F2-1-2.                                NC2094.2
043200 MOV-FAIL-F2-1-2.                                                 NC2094.2
043300     PERFORM  FAIL.                                               NC2094.2
043400     MOVE     DICK    OF A-GROUP      TO COMPUTED-A.              NC2094.2
043500     MOVE     "DICK"  TO CORRECT-A.                               NC2094.2
043600 MOV-WRITE-F2-1-2.                                                NC2094.2
043700     PERFORM  PRINT-DETAIL.                                       NC2094.2
043800*                                                                 NC2094.2
043900 MOV-TEST-F2-1-3.                                                 NC2094.2
044000     ADD      1 TO REC-CT.                                        NC2094.2
044100     IF       HARRY     OF A-GROUP          EQUAL TO "ZZZZZ"      NC2094.2
044200              PERFORM PASS                                        NC2094.2
044300              GO TO MOV-WRITE-F2-1-3.                             NC2094.2
044400     GO       TO MOV-FAIL-F2-1-3.                                 NC2094.2
044500 MOV-DELETE-F2-1-3.                                               NC2094.2
044600     PERFORM  DE-LETE.                                            NC2094.2
044700     GO       TO MOV-WRITE-F2-1-3.                                NC2094.2
044800 MOV-FAIL-F2-1-3.                                                 NC2094.2
044900     PERFORM  FAIL.                                               NC2094.2
045000     MOVE     HARRY   OF A-GROUP      TO COMPUTED-A.              NC2094.2
045100     MOVE     "ZZZZZ" TO CORRECT-A.                               NC2094.2
045200 MOV-WRITE-F2-1-3.                                                NC2094.2
045300     PERFORM  PRINT-DETAIL.                                       NC2094.2
045400*                                                                 NC2094.2
045500 MOV-TEST-F2-1-4.                                                 NC2094.2
045600     ADD 1 TO REC-CT.                                             NC2094.2
045700     IF       JOE       OF A-GROUP          EQUAL TO "JOE"        NC2094.2
045800              PERFORM PASS                                        NC2094.2
045900              GO TO MOV-WRITE-F2-1-4.                             NC2094.2
046000     GO       TO MOV-FAIL-F2-1-4.                                 NC2094.2
046100 MOV-DELETE-F2-1-4.                                               NC2094.2
046200     PERFORM  DE-LETE.                                            NC2094.2
046300     GO       TO MOV-WRITE-F2-1-4.                                NC2094.2
046400 MOV-FAIL-F2-1-4.                                                 NC2094.2
046500     PERFORM  FAIL.                                               NC2094.2
046600     MOVE     JOE     OF A-GROUP      TO COMPUTED-A.              NC2094.2
046700     MOVE     "JOE"   TO CORRECT-A.                               NC2094.2
046800 MOV-WRITE-F2-1-4.                                                NC2094.2
046900     PERFORM  PRINT-DETAIL.                                       NC2094.2
047000*                                                                 NC2094.2
047100 MOV-INIT-F2-2.                                                   NC2094.2
047200     MOVE     "MOV-TEST-F2-2" TO PAR-NAME.                        NC2094.2
047300     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
047400     MOVE     CORRESPONDING A-LEVEL TO A-BUNCH.                   NC2094.2
047500     MOVE     1 TO REC-CT.                                        NC2094.2
047600*        NOTE AL SHOULD BE MOVED.                                 NC2094.2
047700 MOV-TEST-F2-2-1.                                                 NC2094.2
047800     IF       TOM       OF A-BUNCH          EQUAL TO "YYY"        NC2094.2
047900              PERFORM PASS                                        NC2094.2
048000              GO TO MOV-WRITE-F2-2-1.                             NC2094.2
048100     GO       TO MOV-FAIL-F2-2-1.                                 NC2094.2
048200 MOV-DELETE-F2-2-1.                                               NC2094.2
048300     PERFORM  DE-LETE.                                            NC2094.2
048400     GO       TO MOV-WRITE-F2-2-1.                                NC2094.2
048500 MOV-FAIL-F2-2-1.                                                 NC2094.2
048600     PERFORM  FAIL.                                               NC2094.2
048700     MOVE     TOM     OF A-BUNCH      TO COMPUTED-A.              NC2094.2
048800     MOVE     "YYY"   TO CORRECT-A.                               NC2094.2
048900 MOV-WRITE-F2-2-1.                                                NC2094.2
049000     PERFORM  PRINT-DETAIL.                                       NC2094.2
049100*                                                                 NC2094.2
049200 MOV-TEST-F2-2-2.                                                 NC2094.2
049300     ADD 1 TO REC-CT.                                             NC2094.2
049400     IF       DICK      OF A-BUNCH          EQUAL TO "YYYY"       NC2094.2
049500              PERFORM PASS                                        NC2094.2
049600              GO TO MOV-WRITE-F2-2-2.                             NC2094.2
049700     GO       TO MOV-FAIL-F2-2-2.                                 NC2094.2
049800 MOV-DELETE-F2-2-2.                                               NC2094.2
049900     PERFORM  DE-LETE.                                            NC2094.2
050000     GO       TO MOV-WRITE-F2-2-2.                                NC2094.2
050100 MOV-FAIL-F2-2-2.                                                 NC2094.2
050200     PERFORM  FAIL.                                               NC2094.2
050300     MOVE     DICK    OF A-BUNCH      TO COMPUTED-A.              NC2094.2
050400     MOVE     "YYYY"  TO CORRECT-A.                               NC2094.2
050500 MOV-WRITE-F2-2-2.                                                NC2094.2
050600     PERFORM  PRINT-DETAIL.                                       NC2094.2
050700*                                                                 NC2094.2
050800 MOV-TEST-F2-2-3.                                                 NC2094.2
050900     ADD 1 TO REC-CT.                                             NC2094.2
051000     IF       HARRY     OF A-BUNCH          EQUAL TO "YYYYY"      NC2094.2
051100              PERFORM PASS                                        NC2094.2
051200              GO TO MOV-WRITE-F2-2-3.                             NC2094.2
051300     GO       TO MOV-FAIL-F2-2-3.                                 NC2094.2
051400 MOV-DELETE-F2-2-3.                                               NC2094.2
051500     PERFORM  DE-LETE.                                            NC2094.2
051600     GO       TO MOV-WRITE-F2-2-3.                                NC2094.2
051700 MOV-FAIL-F2-2-3.                                                 NC2094.2
051800     PERFORM  FAIL.                                               NC2094.2
051900     MOVE     HARRY   OF A-BUNCH      TO COMPUTED-A.              NC2094.2
052000     MOVE     "YYYYY" TO CORRECT-A.                               NC2094.2
052100 MOV-WRITE-F2-2-3.                                                NC2094.2
052200     PERFORM  PRINT-DETAIL.                                       NC2094.2
052300*                                                                 NC2094.2
052400 MOV-TEST-F2-2-4.                                                 NC2094.2
052500     ADD 1 TO REC-CT.                                             NC2094.2
052600     IF       JOE       OF A-BUNCH          EQUAL TO "YYY"        NC2094.2
052700              PERFORM PASS                                        NC2094.2
052800              GO TO MOV-WRITE-F2-2-4.                             NC2094.2
052900     GO       TO MOV-FAIL-F2-2-4.                                 NC2094.2
053000 MOV-DELETE-F2-2-4.                                               NC2094.2
053100     PERFORM  DE-LETE.                                            NC2094.2
053200     GO       TO MOV-WRITE-F2-2-4.                                NC2094.2
053300 MOV-FAIL-F2-2-4.                                                 NC2094.2
053400     PERFORM  FAIL.                                               NC2094.2
053500     MOVE     JOE     OF A-BUNCH      TO COMPUTED-A.              NC2094.2
053600     MOVE     "YYY"   TO CORRECT-A.                               NC2094.2
053700 MOV-WRITE-F2-2-4.                                                NC2094.2
053800     PERFORM  PRINT-DETAIL.                                       NC2094.2
053900*                                                                 NC2094.2
054000 MOV-TEST-F2-2-5.                                                 NC2094.2
054100     ADD 1 TO REC-CT.                                             NC2094.2
054200     IF       AL        OF A-BUNCH          EQUAL TO "AL"         NC2094.2
054300              PERFORM PASS                                        NC2094.2
054400              GO TO MOV-WRITE-F2-2-5.                             NC2094.2
054500     GO       TO MOV-FAIL-F2-2-5.                                 NC2094.2
054600 MOV-DELETE-F2-2-5.                                               NC2094.2
054700     PERFORM  DE-LETE.                                            NC2094.2
054800     GO       TO MOV-WRITE-F2-2-5.                                NC2094.2
054900 MOV-FAIL-F2-2-5.                                                 NC2094.2
055000     PERFORM  FAIL.                                               NC2094.2
055100     MOVE     AL      OF A-BUNCH      TO COMPUTED-A.              NC2094.2
055200     MOVE     "AL"    TO CORRECT-A.                               NC2094.2
055300 MOV-WRITE-F2-2-5.                                                NC2094.2
055400     PERFORM  PRINT-DETAIL.                                       NC2094.2
055500*                                                                 NC2094.2
055600 MOV-TEST-F2-2-6.                                                 NC2094.2
055700     ADD 1 TO REC-CT.                                             NC2094.2
055800     IF       BOB       OF A-BUNCH          EQUAL TO "YYY"        NC2094.2
055900              PERFORM PASS                                        NC2094.2
056000              GO TO MOV-WRITE-F2-2-6.                             NC2094.2
056100     GO       TO MOV-FAIL-F2-2-6.                                 NC2094.2
056200 MOV-DELETE-F2-2-6.                                               NC2094.2
056300     PERFORM  DE-LETE.                                            NC2094.2
056400     GO       TO MOV-WRITE-F2-2-6.                                NC2094.2
056500 MOV-FAIL-F2-2-6.                                                 NC2094.2
056600     PERFORM  FAIL.                                               NC2094.2
056700     MOVE     BOB     OF A-BUNCH      TO COMPUTED-A.              NC2094.2
056800     MOVE     "YYY"   TO CORRECT-A.                               NC2094.2
056900 MOV-WRITE-F2-2-6.                                                NC2094.2
057000     PERFORM  PRINT-DETAIL.                                       NC2094.2
057100*                                                                 NC2094.2
057200 MOV-INIT-F2-3.                                                   NC2094.2
057300     MOVE     "MOV-TEST-F2-3" TO PAR-NAME.                        NC2094.2
057400     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
057500     MOVE     CORR B-LEVEL OF A-LEVEL TO B-SET.                   NC2094.2
057600     MOVE     1 TO REC-CT.                                        NC2094.2
057700*        NOTE CORR IS A LEGAL ABBREVIATION.                       NC2094.2
057800*        NOTE NO MOVES SHOULD TAKE PLACE.                         NC2094.2
057900*                                                                 NC2094.2
058000 MOV-TEST-F2-3-1.                                                 NC2094.2
058100     ADD 1 TO REC-CT.                                             NC2094.2
058200     IF       TOM       OF A-SET            EQUAL TO "WWW"        NC2094.2
058300              PERFORM PASS                                        NC2094.2
058400              GO TO MOV-WRITE-F2-3-1.                             NC2094.2
058500     GO       TO MOV-FAIL-F2-3-1.                                 NC2094.2
058600 MOV-DELETE-F2-3-1.                                               NC2094.2
058700     PERFORM  DE-LETE.                                            NC2094.2
058800     GO       TO MOV-WRITE-F2-3-1.                                NC2094.2
058900 MOV-FAIL-F2-3-1.                                                 NC2094.2
059000     PERFORM  FAIL.                                               NC2094.2
059100     MOVE     TOM     OF A-SET        TO COMPUTED-A.              NC2094.2
059200     MOVE     "WWW"   TO CORRECT-A.                               NC2094.2
059300 MOV-WRITE-F2-3-1.                                                NC2094.2
059400     PERFORM  PRINT-DETAIL.                                       NC2094.2
059500*                                                                 NC2094.2
059600 MOV-TEST-F2-3-2.                                                 NC2094.2
059700     ADD 1 TO REC-CT.                                             NC2094.2
059800     IF       DICK      OF A-SET            EQUAL TO "WWWW"       NC2094.2
059900              PERFORM PASS                                        NC2094.2
060000              GO TO MOV-WRITE-F2-3-2.                             NC2094.2
060100     GO       TO MOV-FAIL-F2-3-2.                                 NC2094.2
060200 MOV-DELETE-F2-3-2.                                               NC2094.2
060300     PERFORM  DE-LETE.                                            NC2094.2
060400     GO       TO MOV-WRITE-F2-3-2.                                NC2094.2
060500 MOV-FAIL-F2-3-2.                                                 NC2094.2
060600     PERFORM  FAIL.                                               NC2094.2
060700     MOVE     DICK    OF A-SET        TO COMPUTED-A.              NC2094.2
060800     MOVE     "WWWW"  TO CORRECT-A.                               NC2094.2
060900 MOV-WRITE-F2-3-2.                                                NC2094.2
061000     PERFORM  PRINT-DETAIL.                                       NC2094.2
061100*                                                                 NC2094.2
061200 MOV-TEST-F2-3-3.                                                 NC2094.2
061300     ADD 1 TO REC-CT.                                             NC2094.2
061400     IF       HARRY     OF A-SET            EQUAL TO "WWWWW"      NC2094.2
061500              PERFORM PASS                                        NC2094.2
061600              GO TO MOV-WRITE-F2-3-3.                             NC2094.2
061700     GO       TO MOV-FAIL-F2-3-3.                                 NC2094.2
061800 MOV-DELETE-F2-3-3.                                               NC2094.2
061900     PERFORM  DE-LETE.                                            NC2094.2
062000     GO       TO MOV-WRITE-F2-3-3.                                NC2094.2
062100 MOV-FAIL-F2-3-3.                                                 NC2094.2
062200     PERFORM  FAIL.                                               NC2094.2
062300     MOVE     HARRY   OF A-SET        TO COMPUTED-A.              NC2094.2
062400     MOVE     "WWWWW" TO CORRECT-A.                               NC2094.2
062500 MOV-WRITE-F2-3-3.                                                NC2094.2
062600     PERFORM  PRINT-DETAIL.                                       NC2094.2
062700*                                                                 NC2094.2
062800 MOV-TEST-F2-3-4.                                                 NC2094.2
062900     ADD 1 TO REC-CT.                                             NC2094.2
063000     IF       BOB       OF A-SET            EQUAL TO "WWW"        NC2094.2
063100              PERFORM PASS                                        NC2094.2
063200              GO TO MOV-WRITE-F2-3-4.                             NC2094.2
063300     GO       TO MOV-FAIL-F2-3-4.                                 NC2094.2
063400 MOV-DELETE-F2-3-4.                                               NC2094.2
063500     PERFORM  DE-LETE.                                            NC2094.2
063600     GO       TO MOV-WRITE-F2-3-4.                                NC2094.2
063700 MOV-FAIL-F2-3-4.                                                 NC2094.2
063800     PERFORM  FAIL.                                               NC2094.2
063900     MOVE     BOB     OF A-SET        TO COMPUTED-A.              NC2094.2
064000     MOVE     "WWW"   TO CORRECT-A.                               NC2094.2
064100 MOV-WRITE-F2-3-4.                                                NC2094.2
064200     PERFORM  PRINT-DETAIL.                                       NC2094.2
064300*                                                                 NC2094.2
064400 MOV-INIT-F2-4.                                                   NC2094.2
064500     MOVE     "MOV-TEST-F2-4" TO PAR-NAME.                        NC2094.2
064600     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
064700     MOVE     CORRESPONDING C-LEVEL OF A-LEVEL TO C-STACK.        NC2094.2
064800     MOVE     1 TO REC-CT.                                        NC2094.2
064900*        NOTE TOM AND HARRY SHOULD BE MOVED.                      NC2094.2
065000 MOV-TEST-F2-4-1.                                                 NC2094.2
065100     IF       TOM       OF C-STACK          EQUAL TO "TOM"        NC2094.2
065200              PERFORM PASS                                        NC2094.2
065300              GO TO MOV-WRITE-F2-4-1.                             NC2094.2
065400     GO       TO MOV-FAIL-F2-4-1.                                 NC2094.2
065500 MOV-DELETE-F2-4-1.                                               NC2094.2
065600     PERFORM  DE-LETE.                                            NC2094.2
065700     GO       TO MOV-WRITE-F2-4-1.                                NC2094.2
065800 MOV-FAIL-F2-4-1.                                                 NC2094.2
065900     PERFORM  FAIL.                                               NC2094.2
066000     MOVE     TOM     OF C-STACK      TO COMPUTED-A.              NC2094.2
066100     MOVE     "TOM"   TO CORRECT-A.                               NC2094.2
066200 MOV-WRITE-F2-4-1.                                                NC2094.2
066300     PERFORM  PRINT-DETAIL.                                       NC2094.2
066400*                                                                 NC2094.2
066500 MOV-TEST-F2-4-2.                                                 NC2094.2
066600     ADD 1 TO REC-CT.                                             NC2094.2
066700     IF       DICK      OF C-STACK          EQUAL TO "VVVV"       NC2094.2
066800              PERFORM PASS                                        NC2094.2
066900              GO TO MOV-WRITE-F2-4-2.                             NC2094.2
067000     GO       TO MOV-FAIL-F2-4-2.                                 NC2094.2
067100 MOV-DELETE-F2-4-2.                                               NC2094.2
067200     PERFORM  DE-LETE.                                            NC2094.2
067300     GO       TO MOV-WRITE-F2-4-2.                                NC2094.2
067400 MOV-FAIL-F2-4-2.                                                 NC2094.2
067500     PERFORM  FAIL.                                               NC2094.2
067600     MOVE     DICK    OF C-STACK      TO COMPUTED-A.              NC2094.2
067700     MOVE     "VVVV"  TO CORRECT-A.                               NC2094.2
067800 MOV-WRITE-F2-4-2.                                                NC2094.2
067900     PERFORM  PRINT-DETAIL.                                       NC2094.2
068000*                                                                 NC2094.2
068100 MOV-TEST-F2-4-3.                                                 NC2094.2
068200     ADD 1 TO REC-CT.                                             NC2094.2
068300     IF       HARRY     OF C-STACK          EQUAL TO "HARRY"      NC2094.2
068400              PERFORM PASS                                        NC2094.2
068500              GO TO MOV-WRITE-F2-4-3.                             NC2094.2
068600     GO       TO MOV-FAIL-F2-4-3.                                 NC2094.2
068700 MOV-DELETE-F2-4-3.                                               NC2094.2
068800     PERFORM  DE-LETE.                                            NC2094.2
068900     GO       TO MOV-WRITE-F2-4-3.                                NC2094.2
069000 MOV-FAIL-F2-4-3.                                                 NC2094.2
069100     PERFORM  FAIL.                                               NC2094.2
069200     MOVE     HARRY   OF C-STACK      TO COMPUTED-A.              NC2094.2
069300     MOVE     "HARRY" TO CORRECT-A.                               NC2094.2
069400 MOV-WRITE-F2-4-3.                                                NC2094.2
069500     PERFORM  PRINT-DETAIL.                                       NC2094.2
069600*                                                                 NC2094.2
069700 MOV-INIT-F2-5.                                                   NC2094.2
069800     MOVE     "MOV-TEST-F2-5" TO PAR-NAME.                        NC2094.2
069900     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
070000     MOVE     "  WITH RENAMES      " TO FEATURE.                  NC2094.2
070100     MOVE     CORRESPONDING A-LEVEL TO A-GLOB.                    NC2094.2
070200     MOVE     1 TO REC-CT.                                        NC2094.2
070300*        NOTE TOM, DICK, JOE, AND AL SHOULD BE MOVED.             NC2094.2
070400*                                                                 NC2094.2
070500 MOV-TEST-F2-5-1.                                                 NC2094.2
070600     IF       TOM       OF A-GLOB           EQUAL TO "TOM"        NC2094.2
070700              PERFORM PASS                                        NC2094.2
070800              GO TO MOV-WRITE-F2-5-1.                             NC2094.2
070900     GO       TO MOV-FAIL-F2-5-1.                                 NC2094.2
071000 MOV-DELETE-F2-5-1.                                               NC2094.2
071100     PERFORM  DE-LETE.                                            NC2094.2
071200     GO       TO MOV-WRITE-F2-5-1.                                NC2094.2
071300 MOV-FAIL-F2-5-1.                                                 NC2094.2
071400     PERFORM  FAIL.                                               NC2094.2
071500     MOVE     TOM     OF A-GLOB       TO COMPUTED-A.              NC2094.2
071600     MOVE     "TOM"   TO CORRECT-A.                               NC2094.2
071700 MOV-WRITE-F2-5-1.                                                NC2094.2
071800     PERFORM  PRINT-DETAIL.                                       NC2094.2
071900*                                                                 NC2094.2
072000 MOV-TEST-F2-5-2.                                                 NC2094.2
072100     ADD 1 TO REC-CT.                                             NC2094.2
072200     IF       DICK      OF A-GLOB           EQUAL TO "DICK"       NC2094.2
072300              PERFORM PASS                                        NC2094.2
072400              GO TO MOV-WRITE-F2-5-2.                             NC2094.2
072500     GO       TO MOV-FAIL-F2-5-2.                                 NC2094.2
072600 MOV-DELETE-F2-5-2.                                               NC2094.2
072700     PERFORM  DE-LETE.                                            NC2094.2
072800     GO       TO MOV-WRITE-F2-5-2.                                NC2094.2
072900 MOV-FAIL-F2-5-2.                                                 NC2094.2
073000     PERFORM  FAIL.                                               NC2094.2
073100     MOVE     DICK    OF A-GLOB       TO COMPUTED-A.              NC2094.2
073200     MOVE     "DICK"  TO CORRECT-A.                               NC2094.2
073300 MOV-WRITE-F2-5-2.                                                NC2094.2
073400     PERFORM  PRINT-DETAIL.                                       NC2094.2
073500*                                                                 NC2094.2
073600 MOV-TEST-F2-5-3.                                                 NC2094.2
073700     ADD 1 TO REC-CT.                                             NC2094.2
073800     IF       HARRY     OF A-GLOB           EQUAL TO "UUUUU"      NC2094.2
073900              PERFORM PASS                                        NC2094.2
074000              GO TO MOV-WRITE-F2-5-3.                             NC2094.2
074100     GO       TO MOV-FAIL-F2-5-3.                                 NC2094.2
074200 MOV-DELETE-F2-5-3.                                               NC2094.2
074300     PERFORM  DE-LETE.                                            NC2094.2
074400     GO       TO MOV-WRITE-F2-5-3.                                NC2094.2
074500 MOV-FAIL-F2-5-3.                                                 NC2094.2
074600     PERFORM  FAIL.                                               NC2094.2
074700     MOVE     HARRY   OF A-GLOB       TO COMPUTED-A.              NC2094.2
074800     MOVE     "UUUUU" TO CORRECT-A.                               NC2094.2
074900 MOV-WRITE-F2-5-3.                                                NC2094.2
075000     PERFORM  PRINT-DETAIL.                                       NC2094.2
075100*                                                                 NC2094.2
075200 MOV-TEST-F2-5-4.                                                 NC2094.2
075300     ADD 1 TO REC-CT.                                             NC2094.2
075400     IF       JOE       OF A-GLOB           EQUAL TO "JOE"        NC2094.2
075500              PERFORM PASS                                        NC2094.2
075600              GO TO MOV-WRITE-F2-5-4.                             NC2094.2
075700     GO       TO MOV-FAIL-F2-5-4.                                 NC2094.2
075800 MOV-DELETE-F2-5-4.                                               NC2094.2
075900     PERFORM  DE-LETE.                                            NC2094.2
076000     GO       TO MOV-WRITE-F2-5-4.                                NC2094.2
076100 MOV-FAIL-F2-5-4.                                                 NC2094.2
076200     PERFORM  FAIL.                                               NC2094.2
076300     MOVE     JOE     OF A-GLOB       TO COMPUTED-A.              NC2094.2
076400     MOVE     "JOE"   TO CORRECT-A.                               NC2094.2
076500 MOV-WRITE-F2-5-4.                                                NC2094.2
076600     PERFORM  PRINT-DETAIL.                                       NC2094.2
076700*                                                                 NC2094.2
076800 MOV-TEST-F2-5-5.                                                 NC2094.2
076900     ADD 1 TO REC-CT.                                             NC2094.2
077000     IF       AL        OF A-GLOB           EQUAL TO "AL"         NC2094.2
077100              PERFORM PASS                                        NC2094.2
077200              GO TO MOV-WRITE-F2-5-5.                             NC2094.2
077300     GO       TO MOV-FAIL-F2-5-5.                                 NC2094.2
077400 MOV-DELETE-F2-5-5.                                               NC2094.2
077500     PERFORM  DE-LETE.                                            NC2094.2
077600     GO       TO MOV-WRITE-F2-5-5.                                NC2094.2
077700 MOV-FAIL-F2-5-5.                                                 NC2094.2
077800     PERFORM  FAIL.                                               NC2094.2
077900     MOVE     AL      OF A-GLOB       TO COMPUTED-A.              NC2094.2
078000     MOVE     "AL"    TO CORRECT-A.                               NC2094.2
078100 MOV-WRITE-F2-5-5.                                                NC2094.2
078200     PERFORM  PRINT-DETAIL.                                       NC2094.2
078300*                                                                 NC2094.2
078400 MOV-TEST-F2-5-6.                                                 NC2094.2
078500     ADD 1 TO REC-CT.                                             NC2094.2
078600     IF       BOB       OF A-GLOB           EQUAL TO "UUU"        NC2094.2
078700              PERFORM PASS                                        NC2094.2
078800              GO TO MOV-WRITE-F2-5-6.                             NC2094.2
078900     GO       TO MOV-FAIL-F2-5-6.                                 NC2094.2
079000 MOV-DELETE-F2-5-6.                                               NC2094.2
079100     PERFORM  DE-LETE.                                            NC2094.2
079200     GO       TO MOV-WRITE-F2-5-6.                                NC2094.2
079300 MOV-FAIL-F2-5-6.                                                 NC2094.2
079400     PERFORM  FAIL.                                               NC2094.2
079500     MOVE     BOB     OF A-GLOB       TO COMPUTED-A.              NC2094.2
079600     MOVE     "UUU"   TO CORRECT-A.                               NC2094.2
079700 MOV-WRITE-F2-5-6.                                                NC2094.2
079800     PERFORM  PRINT-DETAIL.                                       NC2094.2
079900*                                                                 NC2094.2
080000 MOV-INIT-F2-6.                                                   NC2094.2
080100     MOVE     "MOV-TEST-F2-6" TO PAR-NAME.                        NC2094.2
080200     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
080300     MOVE     "  WITH REDEF, OCCURS" TO FEATURE.                  NC2094.2
080400     MOVE     D-LEVEL IN C-COLLECTION TO WORK-AREA.               NC2094.2
080500     MOVE     "TTTTTTTTTTTTTTT" TO C-COLLECTION                   NC2094.2
080600     MOVE     1 TO REC-CT.                                        NC2094.2
080700     MOVE     CORRESPONDING C-LEVEL IN A-LEVEL TO C-COLLECTION.   NC2094.2
080800*        NOTE DICK AND JOE SHOULD BE MOVED.                       NC2094.2
080900*                                                                 NC2094.2
081000 MOV-TEST-F2-6-1.                                                 NC2094.2
081100     MOVE     D-LEVEL IN C-COLLECTION TO WORK-AREA.               NC2094.2
081200     IF       WORK-TOM EQUAL TO "TTT"                             NC2094.2
081300              PERFORM PASS                                        NC2094.2
081400              GO TO MOV-WRITE-F2-6-1.                             NC2094.2
081500     GO       TO MOV-FAIL-F2-6-1.                                 NC2094.2
081600 MOV-DELETE-F2-6-1.                                               NC2094.2
081700     PERFORM  DE-LETE.                                            NC2094.2
081800     GO       TO MOV-WRITE-F2-6-1.                                NC2094.2
081900 MOV-FAIL-F2-6-1.                                                 NC2094.2
082000     PERFORM  FAIL.                                               NC2094.2
082100     MOVE     D-LEVEL OF A-COLLECTION TO COMPUTED-A.              NC2094.2
082200     MOVE     "TTT"   TO CORRECT-A.                               NC2094.2
082300 MOV-WRITE-F2-6-1.                                                NC2094.2
082400     PERFORM  PRINT-DETAIL.                                       NC2094.2
082500*                                                                 NC2094.2
082600 MOV-TEST-F2-6-2.                                                 NC2094.2
082700     ADD 1 TO REC-CT.                                             NC2094.2
082800     IF       DICK OF A-COLLECTION EQUAL TO "DICK"                NC2094.2
082900              PERFORM PASS                                        NC2094.2
083000              GO TO MOV-WRITE-F2-6-2.                             NC2094.2
083100     GO       TO MOV-FAIL-F2-6-2.                                 NC2094.2
083200 MOV-DELETE-F2-6-2.                                               NC2094.2
083300     PERFORM  DE-LETE.                                            NC2094.2
083400     GO       TO MOV-WRITE-F2-6-2.                                NC2094.2
083500 MOV-FAIL-F2-6-2.                                                 NC2094.2
083600     PERFORM  FAIL.                                               NC2094.2
083700     MOVE     DICK    OF A-COLLECTION TO COMPUTED-A.              NC2094.2
083800     MOVE     "DICK"  TO CORRECT-A.                               NC2094.2
083900 MOV-WRITE-F2-6-2.                                                NC2094.2
084000     PERFORM  PRINT-DETAIL.                                       NC2094.2
084100*                                                                 NC2094.2
084200 MOV-TEST-F2-6-3.                                                 NC2094.2
084300     ADD 1 TO REC-CT.                                             NC2094.2
084400     IF       HARRY OF A-COLLECTION EQUAL TO "TTTTT"              NC2094.2
084500              PERFORM PASS                                        NC2094.2
084600              GO TO MOV-WRITE-F2-6-3.                             NC2094.2
084700     GO       TO MOV-FAIL-F2-6-3.                                 NC2094.2
084800 MOV-DELETE-F2-6-3.                                               NC2094.2
084900     PERFORM  DE-LETE.                                            NC2094.2
085000     GO       TO MOV-WRITE-F2-6-3.                                NC2094.2
085100 MOV-FAIL-F2-6-3.                                                 NC2094.2
085200     PERFORM  FAIL.                                               NC2094.2
085300     MOVE     HARRY   OF A-COLLECTION TO COMPUTED-A.              NC2094.2
085400     MOVE     "TTTTT" TO CORRECT-A.                               NC2094.2
085500 MOV-WRITE-F2-6-3.                                                NC2094.2
085600     PERFORM  PRINT-DETAIL.                                       NC2094.2
085700*                                                                 NC2094.2
085800 MOV-TEST-F2-6-4.                                                 NC2094.2
085900     ADD 1 TO REC-CT.                                             NC2094.2
086000     IF       JOE       OF A-COLLECTION     EQUAL TO "JOE"        NC2094.2
086100              PERFORM PASS                                        NC2094.2
086200              GO TO MOV-WRITE-F2-6-4.                             NC2094.2
086300     GO       TO MOV-FAIL-F2-6-4.                                 NC2094.2
086400 MOV-DELETE-F2-6-4.                                               NC2094.2
086500     PERFORM  DE-LETE.                                            NC2094.2
086600     GO       TO MOV-WRITE-F2-6-4.                                NC2094.2
086700 MOV-FAIL-F2-6-4.                                                 NC2094.2
086800     PERFORM  FAIL.                                               NC2094.2
086900     MOVE     JOE     OF A-COLLECTION TO COMPUTED-A.              NC2094.2
087000     MOVE     "JOE"   TO CORRECT-A.                               NC2094.2
087100 MOV-WRITE-F2-6-4.                                                NC2094.2
087200     PERFORM  PRINT-DETAIL.                                       NC2094.2
087300*                                                                 NC2094.2
087400 MOV-INIT-F2-7.                                                   NC2094.2
087500     MOVE     "MOV-TEST-F2-7" TO PAR-NAME.                        NC2094.2
087600     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
087700     MOVE     CORRESPONDING C-LEVEL IN A-LEVEL TO C-FLOCK (4).    NC2094.2
087800     MOVE     1 TO REC-CT.                                        NC2094.2
087900*        NOTE TOM, DICK, HARRY, AND JOE SHOULD BE MOVED.          NC2094.2
088000 MOV-TEST-F2-7-1.                                                 NC2094.2
088100     IF       TOMMY     OF A-COVEY          EQUAL TO "TOM"        NC2094.2
088200              PERFORM PASS                                        NC2094.2
088300              GO TO MOV-WRITE-F2-7-1.                             NC2094.2
088400     GO       TO MOV-FAIL-F2-7-1.                                 NC2094.2
088500 MOV-DELETE-F2-7-1.                                               NC2094.2
088600     PERFORM  DE-LETE.                                            NC2094.2
088700     GO       TO MOV-WRITE-F2-7-1.                                NC2094.2
088800 MOV-FAIL-F2-7-1.                                                 NC2094.2
088900     PERFORM  FAIL.                                               NC2094.2
089000     MOVE     TOMMY   OF A-COVEY      TO COMPUTED-A.              NC2094.2
089100     MOVE     "TOM"   TO CORRECT-A.                               NC2094.2
089200 MOV-WRITE-F2-7-1.                                                NC2094.2
089300     PERFORM  PRINT-DETAIL.                                       NC2094.2
089400*                                                                 NC2094.2
089500 MOV-TEST-F2-7-2.                                                 NC2094.2
089600     ADD 1 TO REC-CT.                                             NC2094.2
089700     IF       DICKY     OF A-COVEY          EQUAL TO "DICK"       NC2094.2
089800              PERFORM PASS                                        NC2094.2
089900              GO TO MOV-WRITE-F2-7-2.                             NC2094.2
090000     GO       TO MOV-FAIL-F2-7-2.                                 NC2094.2
090100 MOV-DELETE-F2-7-2.                                               NC2094.2
090200     PERFORM  DE-LETE.                                            NC2094.2
090300     GO       TO MOV-WRITE-F2-7-2.                                NC2094.2
090400 MOV-FAIL-F2-7-2.                                                 NC2094.2
090500     PERFORM  FAIL.                                               NC2094.2
090600     MOVE     DICKY   OF A-COVEY      TO COMPUTED-A.              NC2094.2
090700     MOVE     "DICK"  TO CORRECT-A.                               NC2094.2
090800 MOV-WRITE-F2-7-2.                                                NC2094.2
090900     PERFORM  PRINT-DETAIL.                                       NC2094.2
091000*                                                                 NC2094.2
091100 MOV-TEST-F2-7-3.                                                 NC2094.2
091200     ADD 1 TO REC-CT.                                             NC2094.2
091300     IF       JOEY      OF A-COVEY          EQUAL TO "JOE"        NC2094.2
091400              PERFORM PASS                                        NC2094.2
091500              GO TO MOV-WRITE-F2-7-3.                             NC2094.2
091600     GO       TO MOV-FAIL-F2-7-3.                                 NC2094.2
091700 MOV-DELETE-F2-7-3.                                               NC2094.2
091800     PERFORM  DE-LETE.                                            NC2094.2
091900     GO       TO MOV-WRITE-F2-7-3.                                NC2094.2
092000 MOV-FAIL-F2-7-3.                                                 NC2094.2
092100     PERFORM  FAIL.                                               NC2094.2
092200     MOVE     JOEY    OF A-COVEY      TO COMPUTED-A.              NC2094.2
092300     MOVE     "JOE"   TO CORRECT-A.                               NC2094.2
092400 MOV-WRITE-F2-7-3.                                                NC2094.2
092500     PERFORM  PRINT-DETAIL.                                       NC2094.2
092600*                                                                 NC2094.2
092700 MOV-TEST-F2-7-4.                                                 NC2094.2
092800     ADD 1 TO REC-CT.                                             NC2094.2
092900     IF       HAROLD    OF A-COVEY          EQUAL TO "HARRY"      NC2094.2
093000              PERFORM PASS                                        NC2094.2
093100              GO TO MOV-WRITE-F2-7-4.                             NC2094.2
093200     GO       TO MOV-FAIL-F2-7-4.                                 NC2094.2
093300 MOV-DELETE-F2-7-4.                                               NC2094.2
093400     PERFORM  DE-LETE.                                            NC2094.2
093500     GO       TO MOV-WRITE-F2-7-4.                                NC2094.2
093600 MOV-FAIL-F2-7-4.                                                 NC2094.2
093700     PERFORM  FAIL.                                               NC2094.2
093800     MOVE     HAROLD  OF A-COVEY      TO COMPUTED-A.              NC2094.2
093900     MOVE     "HARRY" TO CORRECT-A.                               NC2094.2
094000 MOV-WRITE-F2-7-4.                                                NC2094.2
094100     PERFORM  PRINT-DETAIL.                                       NC2094.2
094200*                                                                 NC2094.2
094300 MOV-INIT-F2-8.                                                   NC2094.2
094400     MOVE     "MOV-TEST-F2-8" TO PAR-NAME.                        NC2094.2
094500     MOVE     CORRESPONDING BB-LEVEL TO BB-MOB (1).               NC2094.2
094600     MOVE     "VI-102 6.18.4 GR1" TO ANSI-REFERENCE.              NC2094.2
094700     MOVE     0 TO REC-CT.                                        NC2094.2
094800*        NOTE BOB SHOULD BE MOVED.                                NC2094.2
094900 MOV-TEST-F2-8.                                                   NC2094.2
095000     IF       BOBBY     OF A-CROWD          EQUAL TO "BOB"        NC2094.2
095100              PERFORM PASS                                        NC2094.2
095200              GO TO MOV-WRITE-F2-8.                               NC2094.2
095300     GO       TO MOV-FAIL-F2-8.                                   NC2094.2
095400 MOV-DELETE-F2-8.                                                 NC2094.2
095500     PERFORM  DE-LETE.                                            NC2094.2
095600     GO       TO MOV-WRITE-F2-8.                                  NC2094.2
095700 MOV-FAIL-F2-8.                                                   NC2094.2
095800     PERFORM  FAIL.                                               NC2094.2
095900     MOVE     BOBBY   OF A-CROWD      TO COMPUTED-A.              NC2094.2
096000     MOVE     "BOB"   TO CORRECT-A.                               NC2094.2
096100 MOV-WRITE-F2-8.                                                  NC2094.2
096200     PERFORM  PRINT-DETAIL.                                       NC2094.2
096300*                                                                 NC2094.2
096400 CCVS-EXIT SECTION.                                               NC2094.2
096500 CCVS-999999.                                                     NC2094.2
096600     GO TO CLOSE-FILES.                                           NC2094.2
      *END-OF,NC209A                                                                  
