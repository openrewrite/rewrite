      *HEADER,COBOL,IC233A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2334.2
000200 PROGRAM-ID.                                                      IC2334.2
000300     IC233A.                                                      IC2334.2
000400****************************************************************  IC2334.2
000500*                                                              *  IC2334.2
000600*    VALIDATION FOR:-                                          *  IC2334.2
000700*                                                              *  IC2334.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2334.2
000900*                                                              *  IC2334.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2334.2
001100*                                                              *  IC2334.2
001200****************************************************************  IC2334.2
001300*                                                              *  IC2334.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2334.2
001500*                                                              *  IC2334.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2334.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2334.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2334.2
001900*        X-18  - OPTIONAL SEQUENTIAL MASS STORAGE FILE.        *  IC2334.2
002000****************************************************************  IC2334.2
002100*                                                              *  IC2334.2
002200*    PROGRAMS IC233A AND IC233A-1 TEST THAT A "USE" PROCEDURE  *  IC2334.2
002300*    IN A CALLING PROGRAM IS INVOKED BY A QUALIFYING CONDITION *  IC2334.2
002400*    OCCURRING IN A CONTAINED PROGRAM.                         *  IC2334.2
002500*                                                              *  IC2334.2
002600*    BOTH PROGRAMS WILL BE COMPILED IN ONE INVOCATION OF THE   *  IC2334.2
002700*    COMPILER AS SHOWN BELOW:                                  *  IC2334.2
002800*    IDENTIFICATION DIVISION.                                  *  IC2334.2
002900*    PROGRAM-ID. IC233A.                                       *  IC2334.2
003000*              .                                               *  IC2334.2
003100*              .                                               *  IC2334.2
003200*              .                                               *  IC2334.2
003300*    IDENTIFICATION DIVISION.                                  *  IC2334.2
003400*    PROGRAM-ID. IC233A-1.                                     *  IC2334.2
003500*              .                                               *  IC2334.2
003600*              .                                               *  IC2334.2
003700*    END PROGRAM IC233A-1.                                     *  IC2334.2
003800*    END PROGRAM IC233A.                                       *  IC2334.2
003900****************************************************************  IC2334.2
004000 ENVIRONMENT DIVISION.                                            IC2334.2
004100 CONFIGURATION SECTION.                                           IC2334.2
004200 SOURCE-COMPUTER.                                                 IC2334.2
004300     XXXXX082.                                                    IC2334.2
004400 OBJECT-COMPUTER.                                                 IC2334.2
004500     XXXXX083.                                                    IC2334.2
004600 INPUT-OUTPUT SECTION.                                            IC2334.2
004700 FILE-CONTROL.                                                    IC2334.2
004800     SELECT PRINT-FILE ASSIGN TO                                  IC2334.2
004900     XXXXX055.                                                    IC2334.2
005000     SELECT OPTIONAL TEST-FILE ASSIGN TO                          IC2334.2
005100     XXXXX018.                                                    IC2334.2
005200 DATA DIVISION.                                                   IC2334.2
005300 FILE SECTION.                                                    IC2334.2
005400 FD  PRINT-FILE.                                                  IC2334.2
005500 01  PRINT-REC PICTURE X(120).                                    IC2334.2
005600 01  DUMMY-RECORD PICTURE X(120).                                 IC2334.2
005700 FD  TEST-FILE GLOBAL.                                            IC2334.2
005800 01  TEST-REC PIC X(20).                                          IC2334.2
005900 WORKING-STORAGE SECTION.                                         IC2334.2
006000 01  DILFRAP                     PIC 9.                           IC2334.2
006100 01  TEST-RESULTS.                                                IC2334.2
006200     02 FILLER                   PIC X      VALUE SPACE.          IC2334.2
006300     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2334.2
006400     02 FILLER                   PIC X      VALUE SPACE.          IC2334.2
006500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2334.2
006600     02 FILLER                   PIC X      VALUE SPACE.          IC2334.2
006700     02  PAR-NAME.                                                IC2334.2
006800       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2334.2
006900       03  PARDOT-X              PIC X      VALUE SPACE.          IC2334.2
007000       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2334.2
007100     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2334.2
007200     02 RE-MARK                  PIC X(61).                       IC2334.2
007300 01  TEST-COMPUTED.                                               IC2334.2
007400     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2334.2
007500     02 FILLER                   PIC X(17)  VALUE                 IC2334.2
007600            "       COMPUTED=".                                   IC2334.2
007700     02 COMPUTED-X.                                               IC2334.2
007800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2334.2
007900     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2334.2
008000                                 PIC -9(9).9(9).                  IC2334.2
008100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2334.2
008200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2334.2
008300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2334.2
008400     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2334.2
008500         04 COMPUTED-18V0                    PIC -9(18).          IC2334.2
008600         04 FILLER                           PIC X.               IC2334.2
008700     03 FILLER PIC X(50) VALUE SPACE.                             IC2334.2
008800 01  TEST-CORRECT.                                                IC2334.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             IC2334.2
009000     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2334.2
009100     02 CORRECT-X.                                                IC2334.2
009200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2334.2
009300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2334.2
009400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2334.2
009500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2334.2
009600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2334.2
009700     03      CR-18V0 REDEFINES CORRECT-A.                         IC2334.2
009800         04 CORRECT-18V0                     PIC -9(18).          IC2334.2
009900         04 FILLER                           PIC X.               IC2334.2
010000     03 FILLER PIC X(2) VALUE SPACE.                              IC2334.2
010100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2334.2
010200 01  CCVS-C-1.                                                    IC2334.2
010300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2334.2
010400-    "SS  PARAGRAPH-NAME                                          IC2334.2
010500-    "       REMARKS".                                            IC2334.2
010600     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2334.2
010700 01  CCVS-C-2.                                                    IC2334.2
010800     02 FILLER                     PIC X        VALUE SPACE.      IC2334.2
010900     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2334.2
011000     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2334.2
011100     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2334.2
011200     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2334.2
011300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2334.2
011400 01  REC-CT                        PIC 99       VALUE ZERO.       IC2334.2
011500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2334.2
011600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2334.2
011700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2334.2
011800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2334.2
011900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2334.2
012000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2334.2
012100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2334.2
012200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2334.2
012300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2334.2
012400 01  CCVS-H-1.                                                    IC2334.2
012500     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2334.2
012600     02  FILLER                    PIC X(42)    VALUE             IC2334.2
012700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2334.2
012800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2334.2
012900 01  CCVS-H-2A.                                                   IC2334.2
013000   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2334.2
013100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2334.2
013200   02  FILLER                        PIC XXXX   VALUE             IC2334.2
013300     "4.2 ".                                                      IC2334.2
013400   02  FILLER                        PIC X(28)  VALUE             IC2334.2
013500            " COPY - NOT FOR DISTRIBUTION".                       IC2334.2
013600   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2334.2
013700                                                                  IC2334.2
013800 01  CCVS-H-2B.                                                   IC2334.2
013900   02  FILLER                        PIC X(15)  VALUE             IC2334.2
014000            "TEST RESULT OF ".                                    IC2334.2
014100   02  TEST-ID                       PIC X(9).                    IC2334.2
014200   02  FILLER                        PIC X(4)   VALUE             IC2334.2
014300            " IN ".                                               IC2334.2
014400   02  FILLER                        PIC X(12)  VALUE             IC2334.2
014500     " HIGH       ".                                              IC2334.2
014600   02  FILLER                        PIC X(22)  VALUE             IC2334.2
014700            " LEVEL VALIDATION FOR ".                             IC2334.2
014800   02  FILLER                        PIC X(58)  VALUE             IC2334.2
014900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2334.2
015000 01  CCVS-H-3.                                                    IC2334.2
015100     02  FILLER                      PIC X(34)  VALUE             IC2334.2
015200            " FOR OFFICIAL USE ONLY    ".                         IC2334.2
015300     02  FILLER                      PIC X(58)  VALUE             IC2334.2
015400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2334.2
015500     02  FILLER                      PIC X(28)  VALUE             IC2334.2
015600            "  COPYRIGHT   1985 ".                                IC2334.2
015700 01  CCVS-E-1.                                                    IC2334.2
015800     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2334.2
015900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2334.2
016000     02 ID-AGAIN                     PIC X(9).                    IC2334.2
016100     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2334.2
016200 01  CCVS-E-2.                                                    IC2334.2
016300     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2334.2
016400     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2334.2
016500     02 CCVS-E-2-2.                                               IC2334.2
016600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2334.2
016700         03 FILLER                   PIC X      VALUE SPACE.      IC2334.2
016800         03 ENDER-DESC               PIC X(44)  VALUE             IC2334.2
016900            "ERRORS ENCOUNTERED".                                 IC2334.2
017000 01  CCVS-E-3.                                                    IC2334.2
017100     02  FILLER                      PIC X(22)  VALUE             IC2334.2
017200            " FOR OFFICIAL USE ONLY".                             IC2334.2
017300     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2334.2
017400     02  FILLER                      PIC X(58)  VALUE             IC2334.2
017500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2334.2
017600     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2334.2
017700     02 FILLER                       PIC X(15)  VALUE             IC2334.2
017800             " COPYRIGHT 1985".                                   IC2334.2
017900 01  CCVS-E-4.                                                    IC2334.2
018000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2334.2
018100     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2334.2
018200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2334.2
018300     02 FILLER                       PIC X(40)  VALUE             IC2334.2
018400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2334.2
018500 01  XXINFO.                                                      IC2334.2
018600     02 FILLER                       PIC X(19)  VALUE             IC2334.2
018700            "*** INFORMATION ***".                                IC2334.2
018800     02 INFO-TEXT.                                                IC2334.2
018900       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2334.2
019000       04 XXCOMPUTED                 PIC X(20).                   IC2334.2
019100       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2334.2
019200       04 XXCORRECT                  PIC X(20).                   IC2334.2
019300     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2334.2
019400 01  HYPHEN-LINE.                                                 IC2334.2
019500     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2334.2
019600     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2334.2
019700-    "*****************************************".                 IC2334.2
019800     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2334.2
019900-    "******************************".                            IC2334.2
020000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2334.2
020100     "IC233A".                                                    IC2334.2
020200 PROCEDURE DIVISION.                                              IC2334.2
020300 DECLARATIVES.                                                    IC2334.2
020400 SECT-IC233A-001 SECTION.                                         IC2334.2
020500     USE GLOBAL AFTER ERROR PROCEDURE ON INPUT.                   IC2334.2
020600 USE-TEST-2.                                                      IC2334.2
020700     PERFORM D1-PASS.                                             IC2334.2
020800     PERFORM D1-PRINT-DETAIL.                                     IC2334.2
020900     MOVE    1 TO DILFRAP.                                        IC2334.2
021000     GO TO EXIT-USE-TEST-2.                                       IC2334.2
021100 D1-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.        IC2334.2
021200 D1-PRINT-DETAIL.                                                 IC2334.2
021300     IF REC-CT NOT EQUAL TO ZERO                                  IC2334.2
021400             MOVE "." TO PARDOT-X                                 IC2334.2
021500             MOVE REC-CT TO DOTVALUE.                             IC2334.2
021600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM D1-WRITE-LINE.   IC2334.2
021700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM D1-WRITE-LINE            IC2334.2
021800        PERFORM D1-FAIL-ROUTINE THRU D1-FAIL-ROUTINE-EX           IC2334.2
021900          ELSE PERFORM D1-BAIL-OUT THRU D1-BAIL-OUT-EX.           IC2334.2
022000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2334.2
022100     MOVE SPACE TO CORRECT-X.                                     IC2334.2
022200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2334.2
022300     MOVE     SPACE TO RE-MARK.                                   IC2334.2
022400 D1-WRITE-LINE.                                                   IC2334.2
022500     ADD 1 TO RECORD-COUNT.                                       IC2334.2
022600*    IF RECORD-COUNT GREATER 50                                   IC2334.2
022700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2334.2
022800*        MOVE SPACE TO DUMMY-RECORD                               IC2334.2
022900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2334.2
023000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM D1-WRT-LN          IC2334.2
023100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM D1-WRT-LN 2 TIMES  IC2334.2
023200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D1-WRT-LN       IC2334.2
023300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2334.2
023400*        MOVE ZERO TO RECORD-COUNT.                               IC2334.2
023500     PERFORM D1-WRT-LN.                                           IC2334.2
023600 D1-WRT-LN.                                                       IC2334.2
023700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2334.2
023800     MOVE SPACE TO DUMMY-RECORD.                                  IC2334.2
023900 D1-FAIL-ROUTINE.                                                 IC2334.2
024000     IF COMPUTED-X NOT EQUAL TO SPACE GO TO D1-FAIL-ROUTINE-WRITE.IC2334.2
024100     IF CORRECT-X NOT EQUAL TO SPACE GO TO D1-FAIL-ROUTINE-WRITE. IC2334.2
024200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2334.2
024300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2334.2
024400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM D1-WRITE-LINE 2 TIMES.IC2334.2
024500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2334.2
024600     GO TO  D1-FAIL-ROUTINE-EX.                                   IC2334.2
024700 D1-FAIL-ROUTINE-WRITE.                                           IC2334.2
024800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM D1-WRITE-LINE      IC2334.2
024900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2334.2
025000     MOVE TEST-CORRECT TO PRINT-REC PERFORM D1-WRITE-LINE 2 TIMES.IC2334.2
025100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2334.2
025200 D1-FAIL-ROUTINE-EX. EXIT.                                        IC2334.2
025300 D1-BAIL-OUT.                                                     IC2334.2
025400     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D1-BAIL-OUT-WRITE.   IC2334.2
025500     IF  CORRECT-A EQUAL TO SPACE GO TO D1-BAIL-OUT-EX.           IC2334.2
025600 D1-BAIL-OUT-WRITE.                                               IC2334.2
025700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2334.2
025800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2334.2
025900     MOVE   XXINFO TO DUMMY-RECORD.                               IC2334.2
026000     PERFORM D1-WRITE-LINE 2 TIMES.                               IC2334.2
026100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2334.2
026200 D1-BAIL-OUT-EX. EXIT.                                            IC2334.2
026300 EXIT-USE-TEST-2.                                                 IC2334.2
026400     EXIT.                                                        IC2334.2
026500 END DECLARATIVES.                                                IC2334.2
026600 CCVS1 SECTION.                                                   IC2334.2
026700 OPEN-FILES.                                                      IC2334.2
026800     OPEN     OUTPUT PRINT-FILE.                                  IC2334.2
026900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2334.2
027000     MOVE    SPACE TO TEST-RESULTS.                               IC2334.2
027100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2334.2
027200     GO TO CCVS1-EXIT.                                            IC2334.2
027300 CLOSE-FILES.                                                     IC2334.2
027400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2334.2
027500 TERMINATE-CCVS.                                                  IC2334.2
027600*    EXIT PROGRAM.                                                IC2334.2
027700*TERMINATE-CALL.                                                  IC2334.2
027800     STOP     RUN.                                                IC2334.2
027900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2334.2
028000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2334.2
028100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2334.2
028200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2334.2
028300     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2334.2
028400 PRINT-DETAIL.                                                    IC2334.2
028500     IF REC-CT NOT EQUAL TO ZERO                                  IC2334.2
028600             MOVE "." TO PARDOT-X                                 IC2334.2
028700             MOVE REC-CT TO DOTVALUE.                             IC2334.2
028800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2334.2
028900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2334.2
029000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2334.2
029100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2334.2
029200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2334.2
029300     MOVE SPACE TO CORRECT-X.                                     IC2334.2
029400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2334.2
029500     MOVE     SPACE TO RE-MARK.                                   IC2334.2
029600 HEAD-ROUTINE.                                                    IC2334.2
029700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2334.2
029800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2334.2
029900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2334.2
030000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2334.2
030100 COLUMN-NAMES-ROUTINE.                                            IC2334.2
030200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2334.2
030300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2334.2
030400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2334.2
030500 END-ROUTINE.                                                     IC2334.2
030600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2334.2
030700 END-RTN-EXIT.                                                    IC2334.2
030800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2334.2
030900 END-ROUTINE-1.                                                   IC2334.2
031000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2334.2
031100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2334.2
031200      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2334.2
031300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2334.2
031400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2334.2
031500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2334.2
031600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2334.2
031700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2334.2
031800  END-ROUTINE-12.                                                 IC2334.2
031900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2334.2
032000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2334.2
032100         MOVE "NO " TO ERROR-TOTAL                                IC2334.2
032200         ELSE                                                     IC2334.2
032300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2334.2
032400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2334.2
032500     PERFORM WRITE-LINE.                                          IC2334.2
032600 END-ROUTINE-13.                                                  IC2334.2
032700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2334.2
032800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2334.2
032900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2334.2
033000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2334.2
033100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2334.2
033200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2334.2
033300          MOVE "NO " TO ERROR-TOTAL                               IC2334.2
033400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2334.2
033500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2334.2
033600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2334.2
033700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2334.2
033800 WRITE-LINE.                                                      IC2334.2
033900     ADD 1 TO RECORD-COUNT.                                       IC2334.2
034000*    IF RECORD-COUNT GREATER 50                                   IC2334.2
034100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2334.2
034200*        MOVE SPACE TO DUMMY-RECORD                               IC2334.2
034300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2334.2
034400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2334.2
034500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2334.2
034600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2334.2
034700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2334.2
034800*        MOVE ZERO TO RECORD-COUNT.                               IC2334.2
034900     PERFORM WRT-LN.                                              IC2334.2
035000 WRT-LN.                                                          IC2334.2
035100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2334.2
035200     MOVE SPACE TO DUMMY-RECORD.                                  IC2334.2
035300 BLANK-LINE-PRINT.                                                IC2334.2
035400     PERFORM WRT-LN.                                              IC2334.2
035500 FAIL-ROUTINE.                                                    IC2334.2
035600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2334.2
035700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2334.2
035800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2334.2
035900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2334.2
036000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2334.2
036100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2334.2
036200     GO TO  FAIL-ROUTINE-EX.                                      IC2334.2
036300 FAIL-ROUTINE-WRITE.                                              IC2334.2
036400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2334.2
036500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2334.2
036600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2334.2
036700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2334.2
036800 FAIL-ROUTINE-EX. EXIT.                                           IC2334.2
036900 BAIL-OUT.                                                        IC2334.2
037000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2334.2
037100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2334.2
037200 BAIL-OUT-WRITE.                                                  IC2334.2
037300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2334.2
037400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2334.2
037500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2334.2
037600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2334.2
037700 BAIL-OUT-EX. EXIT.                                               IC2334.2
037800 CCVS1-EXIT.                                                      IC2334.2
037900     EXIT.                                                        IC2334.2
038000 SECT-IC233A-1R-001 SECTION.                                      IC2334.2
038100 USE-INIT-1.                                                      IC2334.2
038200     MOVE   "USE-TEST-1" TO PAR-NAME.                             IC2334.2
038300     MOVE   "X-34 5.5.4 GR(1)B" TO ANSI-REFERENCE.                IC2334.2
038400     MOVE    ZERO TO DILFRAP.                                     IC2334.2
038500 USE-TEST-0.                                                      IC2334.2
038600     CALL   "IC233A-1".                                           IC2334.2
038700     IF      DILFRAP = 1                                          IC2334.2
038800             GO TO   CCVS-EXIT.                                   IC2334.2
038900 USE-FAIL-1.                                                      IC2334.2
039000     MOVE   "USE PROCEDURE NOT INVOKED" TO RE-MARK.               IC2334.2
039100     PERFORM FAIL.                                                IC2334.2
039200     GO TO   USE-WRITE-1.                                         IC2334.2
039300 USE-DELETE-1.                                                    IC2334.2
039400     PERFORM DE-LETE.                                             IC2334.2
039500 USE-WRITE-1.                                                     IC2334.2
039600     PERFORM PRINT-DETAIL.                                        IC2334.2
039700*                                                                 IC2334.2
039800 CCVS-EXIT SECTION.                                               IC2334.2
039900 CCVS-999999.                                                     IC2334.2
040000     GO TO CLOSE-FILES.                                           IC2334.2
040100*                                                                 IC2334.2
040200 IDENTIFICATION DIVISION.                                         IC2334.2
040300 PROGRAM-ID.                                                      IC2334.2
040400     IC233A-1.                                                    IC2334.2
040500****************************************************************  IC2334.2
040600*                                                              *  IC2334.2
040700*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2334.2
040800*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2334.2
040900*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2334.2
041000*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2334.2
041100*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2334.2
041200*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2334.2
041300*                                                              *  IC2334.2
041400*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2334.2
041500*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2334.2
041600*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2334.2
041700*                                                              *  IC2334.2
041800*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2334.2
041900*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2334.2
042000*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2334.2
042100*                                                              *  IC2334.2
042200*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2334.2
042300*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2334.2
042400*                & INFORMATION TECHNOLOGY                      *  IC2334.2
042500*          TWO SKYLINE PLACE                                   *  IC2334.2
042600*          SUITE 1100                                          *  IC2334.2
042700*          5203 LEESBURG PIKE                                  *  IC2334.2
042800*          FALLS CHURCH                                        *  IC2334.2
042900*          VA 22041                                            *  IC2334.2
043000*          U.S.A.                                              *  IC2334.2
043100*                                                              *  IC2334.2
043200*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2334.2
043300*                                                              *  IC2334.2
043400*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2334.2
043500*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2334.2
043600*          21 RUE BARA                                         *  IC2334.2
043700*          F-92132 ISSY                                        *  IC2334.2
043800*          FRANCE                                              *  IC2334.2
043900*                                                              *  IC2334.2
044000*                                                              *  IC2334.2
044100*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2334.2
044200*               UND DATENVERARBEITUNG MBH)                     *  IC2334.2
044300*          SCHLOSS BIRLINGHOVEN                                *  IC2334.2
044400*          POSTFACH 12 40                                      *  IC2334.2
044500*          D-5205 ST. AUGUSTIN 1                               *  IC2334.2
044600*          GERMANY FR                                          *  IC2334.2
044700*                                                              *  IC2334.2
044800*                                                              *  IC2334.2
044900*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2334.2
045000*          OXFORD ROAD                                         *  IC2334.2
045100*          MANCHESTER                                          *  IC2334.2
045200*          M1 7ED                                              *  IC2334.2
045300*          UNITED KINGDOM                                      *  IC2334.2
045400*                                                              *  IC2334.2
045500*                                                              *  IC2334.2
045600*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2334.2
045700*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2334.2
045800*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2334.2
045900*                                                              *  IC2334.2
046000****************************************************************  IC2334.2
046100*                                                              *  IC2334.2
046200*    VALIDATION FOR:-                                          *  IC2334.2
046300*                                                              *  IC2334.2
046400*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2334.2
046500*                                                              *  IC2334.2
046600*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2334.2
046700*                                                              *  IC2334.2
046800****************************************************************  IC2334.2
046900*                                                              *  IC2334.2
047000*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2334.2
047100*                                                              *  IC2334.2
047200*        X-55  - SYSTEM PRINTER NAME.                          *  IC2334.2
047300*        X-82  - SOURCE COMPUTER NAME.                         *  IC2334.2
047400*        X-83  - OBJECT COMPUTER NAME.                         *  IC2334.2
047500*        X-92  - TEST-FILE.                                    *  IC2334.2
047600*                                                              *  IC2334.2
047700****************************************************************  IC2334.2
047800*                                                              *  IC2334.2
047900*    PROGRAMS IC233A AND IC233A-1 TEST THAT A "USE" PROCEDURE  *  IC2334.2
048000*    IN A CALLING PROGRAM IS INVOKED BY A QUALIFYING CONDITION *  IC2334.2
048100*    OCCURRING IN A CONTAINED PROGRAM.                         *  IC2334.2
048200*                                                              *  IC2334.2
048300*    BOTH PROGRAMS WILL BE COMPILED IN ONE INVOCATION OF THE   *  IC2334.2
048400*    COMPILER AS SHOWN BELOW:                                  *  IC2334.2
048500*    IDENTIFICATION DIVISION.                                  *  IC2334.2
048600*    PROGRAM-ID. IC233A.                                       *  IC2334.2
048700*              .                                               *  IC2334.2
048800*              .                                               *  IC2334.2
048900*              .                                               *  IC2334.2
049000*    IDENTIFICATION DIVISION.                                  *  IC2334.2
049100*    PROGRAM-ID. IC233A-1.                                     *  IC2334.2
049200*              .                                               *  IC2334.2
049300*              .                                               *  IC2334.2
049400*    END PROGRAM IC233A-1.                                     *  IC2334.2
049500*    END PROGRAM IC233A.                                       *  IC2334.2
049600****************************************************************  IC2334.2
049700*ENVIRONMENT DIVISION.                                            IC2334.2
049800*INPUT-OUTPUT SECTION.                                            IC2334.2
049900*FILE-CONTROL.                                                    IC2334.2
050000*    SELECT TEST-FILE ASSIGN TO                                   IC2334.2
050100*    XXXXX018.                                                    IC2334.2
050200 DATA DIVISION.                                                   IC2334.2
050300 FILE SECTION.                                                    IC2334.2
050400 WORKING-STORAGE SECTION.                                         IC2334.2
050500 PROCEDURE DIVISION.                                              IC2334.2
050600 SECT-IC233A-1-001 SECTION.                                       IC2334.2
050700 USE-INIT-1.                                                      IC2334.2
050800     OPEN    INPUT TEST-FILE.                                     IC2334.2
050900     READ    TEST-FILE.                                           IC2334.2
051000 END-PROG.                                                        IC2334.2
051100     EXIT PROGRAM.                                                IC2334.2
051200 END PROGRAM IC233A-1.                                            IC2334.2
051300 END PROGRAM IC233A.                                              IC2334.2
      *END-OF,IC233A                                                                  
