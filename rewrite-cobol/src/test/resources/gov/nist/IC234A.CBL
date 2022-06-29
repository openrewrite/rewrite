      *HEADER,COBOL,IC234A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2344.2
000200 PROGRAM-ID.                                                      IC2344.2
000300     IC234A.                                                      IC2344.2
000400****************************************************************  IC2344.2
000500*                                                              *  IC2344.2
000600*    VALIDATION FOR:-                                          *  IC2344.2
000700*                                                              *  IC2344.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2344.2
000900*                                                              *  IC2344.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2344.2
001100*                                                              *  IC2344.2
001200****************************************************************  IC2344.2
001300*                                                              *  IC2344.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2344.2
001500*                                                              *  IC2344.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2344.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2344.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2344.2
001900*        X-14  - TEST-FILE.                                    *  IC2344.2
002000*                                                              *  IC2344.2
002100****************************************************************  IC2344.2
002200*                                                              *  IC2344.2
002300*    PROGRAMS IC234A, IC234A-1, IC234A-2 AND IC234A-3 TEST     *  IC2344.2
002400*    TEST THAT A "USE" PROCEDURE IN A CALLING PROGRAM IS       *  IC2344.2
002500*    INVOKED BY A QUALIFYING CONDITION OCURRING IN A CONTAINED *  IC2344.2
002600*    PROGRAM NESTED TO FOUR LEVELS.                            *  IC2344.2
002700*                                                              *  IC2344.2
002800*    ALL PROGRAMS WILL BE COMPILED IN ONE INVOCATION OF THE    *  IC2344.2
002900*    COMPILER AS SHOWN BELOW:                                  *  IC2344.2
003000*    IDENTIFICATION DIVISION.                                  *  IC2344.2
003100*    PROGRAM-ID. IC234A.                                       *  IC2344.2
003200*              .                                               *  IC2344.2
003300*              .                                               *  IC2344.2
003400*              .                                               *  IC2344.2
003500*    IDENTIFICATION DIVISION.                                  *  IC2344.2
003600*    PROGRAM-ID. IC234A-1.                                     *  IC2344.2
003700*              .                                               *  IC2344.2
003800*              .                                               *  IC2344.2
003900*    IDENTIFICATION DIVISION.                                  *  IC2344.2
004000*    PROGRAM-ID. IC234A-2.                                     *  IC2344.2
004100*              .                                               *  IC2344.2
004200*              .                                               *  IC2344.2
004300*              .                                               *  IC2344.2
004400*    IDENTIFICATION DIVISION.                                  *  IC2344.2
004500*    PROGRAM-ID. IC234A-3.                                     *  IC2344.2
004600*              .                                               *  IC2344.2
004700*              .                                               *  IC2344.2
004800*    END PROGRAM IC234A-3.                                     *  IC2344.2
004900*    END PROGRAM IC234A-2.                                     *  IC2344.2
005000*    END PROGRAM IC234A-1.                                     *  IC2344.2
005100*    END PROGRAM IC234A.                                       *  IC2344.2
005200****************************************************************  IC2344.2
005300 ENVIRONMENT DIVISION.                                            IC2344.2
005400 CONFIGURATION SECTION.                                           IC2344.2
005500 SOURCE-COMPUTER.                                                 IC2344.2
005600     XXXXX082.                                                    IC2344.2
005700 OBJECT-COMPUTER.                                                 IC2344.2
005800     XXXXX083.                                                    IC2344.2
005900 INPUT-OUTPUT SECTION.                                            IC2344.2
006000 FILE-CONTROL.                                                    IC2344.2
006100     SELECT PRINT-FILE ASSIGN TO                                  IC2344.2
006200     XXXXX055.                                                    IC2344.2
006300     SELECT TEST-FILE  ASSIGN TO                                  IC2344.2
006400     XXXXX014.                                                    IC2344.2
006500 DATA DIVISION.                                                   IC2344.2
006600 FILE SECTION.                                                    IC2344.2
006700 FD  PRINT-FILE.                                                  IC2344.2
006800 01  PRINT-REC                   PIC X(120).                      IC2344.2
006900 01  DUMMY-RECORD                PIC X(120).                      IC2344.2
007000 FD  TEST-FILE GLOBAL.                                            IC2344.2
007100 01  TEST-RECORD                 PIC X(20).                       IC2344.2
007200 WORKING-STORAGE SECTION.                                         IC2344.2
007300 01  DILFRAP   GLOBAL            PIC 9.                           IC2344.2
007400 01  TEST-RESULTS.                                                IC2344.2
007500     02 FILLER                   PIC X      VALUE SPACE.          IC2344.2
007600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2344.2
007700     02 FILLER                   PIC X      VALUE SPACE.          IC2344.2
007800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2344.2
007900     02 FILLER                   PIC X      VALUE SPACE.          IC2344.2
008000     02  PAR-NAME.                                                IC2344.2
008100       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2344.2
008200       03  PARDOT-X              PIC X      VALUE SPACE.          IC2344.2
008300       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2344.2
008400     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2344.2
008500     02 RE-MARK                  PIC X(61).                       IC2344.2
008600 01  TEST-COMPUTED.                                               IC2344.2
008700     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2344.2
008800     02 FILLER                   PIC X(17)  VALUE                 IC2344.2
008900            "       COMPUTED=".                                   IC2344.2
009000     02 COMPUTED-X.                                               IC2344.2
009100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2344.2
009200     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2344.2
009300                                 PIC -9(9).9(9).                  IC2344.2
009400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2344.2
009500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2344.2
009600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2344.2
009700     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2344.2
009800         04 COMPUTED-18V0                    PIC -9(18).          IC2344.2
009900         04 FILLER                           PIC X.               IC2344.2
010000     03 FILLER PIC X(50) VALUE SPACE.                             IC2344.2
010100 01  TEST-CORRECT.                                                IC2344.2
010200     02 FILLER PIC X(30) VALUE SPACE.                             IC2344.2
010300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2344.2
010400     02 CORRECT-X.                                                IC2344.2
010500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2344.2
010600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2344.2
010700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2344.2
010800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2344.2
010900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2344.2
011000     03      CR-18V0 REDEFINES CORRECT-A.                         IC2344.2
011100         04 CORRECT-18V0                     PIC -9(18).          IC2344.2
011200         04 FILLER                           PIC X.               IC2344.2
011300     03 FILLER PIC X(2) VALUE SPACE.                              IC2344.2
011400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2344.2
011500 01  CCVS-C-1.                                                    IC2344.2
011600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2344.2
011700-    "SS  PARAGRAPH-NAME                                          IC2344.2
011800-    "       REMARKS".                                            IC2344.2
011900     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2344.2
012000 01  CCVS-C-2.                                                    IC2344.2
012100     02 FILLER                     PIC X        VALUE SPACE.      IC2344.2
012200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2344.2
012300     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2344.2
012400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2344.2
012500     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2344.2
012600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2344.2
012700 01  REC-CT                        PIC 99       VALUE ZERO.       IC2344.2
012800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2344.2
012900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2344.2
013000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2344.2
013100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2344.2
013200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2344.2
013300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2344.2
013400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2344.2
013500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2344.2
013600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2344.2
013700 01  CCVS-H-1.                                                    IC2344.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2344.2
013900     02  FILLER                    PIC X(42)    VALUE             IC2344.2
014000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2344.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2344.2
014200 01  CCVS-H-2A.                                                   IC2344.2
014300   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2344.2
014400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2344.2
014500   02  FILLER                        PIC XXXX   VALUE             IC2344.2
014600     "4.2 ".                                                      IC2344.2
014700   02  FILLER                        PIC X(28)  VALUE             IC2344.2
014800            " COPY - NOT FOR DISTRIBUTION".                       IC2344.2
014900   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2344.2
015000                                                                  IC2344.2
015100 01  CCVS-H-2B.                                                   IC2344.2
015200   02  FILLER                        PIC X(15)  VALUE             IC2344.2
015300            "TEST RESULT OF ".                                    IC2344.2
015400   02  TEST-ID                       PIC X(9).                    IC2344.2
015500   02  FILLER                        PIC X(4)   VALUE             IC2344.2
015600            " IN ".                                               IC2344.2
015700   02  FILLER                        PIC X(12)  VALUE             IC2344.2
015800     " HIGH       ".                                              IC2344.2
015900   02  FILLER                        PIC X(22)  VALUE             IC2344.2
016000            " LEVEL VALIDATION FOR ".                             IC2344.2
016100   02  FILLER                        PIC X(58)  VALUE             IC2344.2
016200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2344.2
016300 01  CCVS-H-3.                                                    IC2344.2
016400     02  FILLER                      PIC X(34)  VALUE             IC2344.2
016500            " FOR OFFICIAL USE ONLY    ".                         IC2344.2
016600     02  FILLER                      PIC X(58)  VALUE             IC2344.2
016700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2344.2
016800     02  FILLER                      PIC X(28)  VALUE             IC2344.2
016900            "  COPYRIGHT   1985 ".                                IC2344.2
017000 01  CCVS-E-1.                                                    IC2344.2
017100     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2344.2
017200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2344.2
017300     02 ID-AGAIN                     PIC X(9).                    IC2344.2
017400     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2344.2
017500 01  CCVS-E-2.                                                    IC2344.2
017600     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2344.2
017700     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2344.2
017800     02 CCVS-E-2-2.                                               IC2344.2
017900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2344.2
018000         03 FILLER                   PIC X      VALUE SPACE.      IC2344.2
018100         03 ENDER-DESC               PIC X(44)  VALUE             IC2344.2
018200            "ERRORS ENCOUNTERED".                                 IC2344.2
018300 01  CCVS-E-3.                                                    IC2344.2
018400     02  FILLER                      PIC X(22)  VALUE             IC2344.2
018500            " FOR OFFICIAL USE ONLY".                             IC2344.2
018600     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2344.2
018700     02  FILLER                      PIC X(58)  VALUE             IC2344.2
018800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2344.2
018900     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2344.2
019000     02 FILLER                       PIC X(15)  VALUE             IC2344.2
019100             " COPYRIGHT 1985".                                   IC2344.2
019200 01  CCVS-E-4.                                                    IC2344.2
019300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2344.2
019400     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2344.2
019500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2344.2
019600     02 FILLER                       PIC X(40)  VALUE             IC2344.2
019700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2344.2
019800 01  XXINFO.                                                      IC2344.2
019900     02 FILLER                       PIC X(19)  VALUE             IC2344.2
020000            "*** INFORMATION ***".                                IC2344.2
020100     02 INFO-TEXT.                                                IC2344.2
020200       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2344.2
020300       04 XXCOMPUTED                 PIC X(20).                   IC2344.2
020400       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2344.2
020500       04 XXCORRECT                  PIC X(20).                   IC2344.2
020600     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2344.2
020700 01  HYPHEN-LINE.                                                 IC2344.2
020800     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2344.2
020900     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2344.2
021000-    "*****************************************".                 IC2344.2
021100     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2344.2
021200-    "******************************".                            IC2344.2
021300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2344.2
021400     "IC234A".                                                    IC2344.2
021500 PROCEDURE DIVISION.                                              IC2344.2
021600 DECLARATIVES.                                                    IC2344.2
021700 SECT-IC234A-001 SECTION.                                         IC2344.2
021800     USE GLOBAL AFTER ERROR PROCEDURE ON INPUT.                   IC2344.2
021900 USE-TEST-2.                                                      IC2344.2
022000     ADD 1 TO DILFRAP.                                            IC2344.2
022100 END DECLARATIVES.                                                IC2344.2
022200 CCVS1 SECTION.                                                   IC2344.2
022300 OPEN-FILES.                                                      IC2344.2
022400     OPEN     OUTPUT PRINT-FILE.                                  IC2344.2
022500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2344.2
022600     MOVE    SPACE TO TEST-RESULTS.                               IC2344.2
022700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2344.2
022800     GO TO CCVS1-EXIT.                                            IC2344.2
022900 CLOSE-FILES.                                                     IC2344.2
023000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2344.2
023100 TERMINATE-CCVS.                                                  IC2344.2
023200*    EXIT PROGRAM.                                                IC2344.2
023300*TERMINATE-CALL.                                                  IC2344.2
023400     STOP     RUN.                                                IC2344.2
023500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2344.2
023600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2344.2
023700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2344.2
023800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2344.2
023900     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2344.2
024000 PRINT-DETAIL.                                                    IC2344.2
024100     IF REC-CT NOT EQUAL TO ZERO                                  IC2344.2
024200             MOVE "." TO PARDOT-X                                 IC2344.2
024300             MOVE REC-CT TO DOTVALUE.                             IC2344.2
024400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2344.2
024500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2344.2
024600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2344.2
024700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2344.2
024800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2344.2
024900     MOVE SPACE TO CORRECT-X.                                     IC2344.2
025000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2344.2
025100     MOVE     SPACE TO RE-MARK.                                   IC2344.2
025200 HEAD-ROUTINE.                                                    IC2344.2
025300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2344.2
025400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2344.2
025500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2344.2
025600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2344.2
025700 COLUMN-NAMES-ROUTINE.                                            IC2344.2
025800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2344.2
025900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2344.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2344.2
026100 END-ROUTINE.                                                     IC2344.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2344.2
026300 END-RTN-EXIT.                                                    IC2344.2
026400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2344.2
026500 END-ROUTINE-1.                                                   IC2344.2
026600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2344.2
026700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2344.2
026800      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2344.2
026900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2344.2
027000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2344.2
027100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2344.2
027200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2344.2
027300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2344.2
027400  END-ROUTINE-12.                                                 IC2344.2
027500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2344.2
027600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2344.2
027700         MOVE "NO " TO ERROR-TOTAL                                IC2344.2
027800         ELSE                                                     IC2344.2
027900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2344.2
028000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2344.2
028100     PERFORM WRITE-LINE.                                          IC2344.2
028200 END-ROUTINE-13.                                                  IC2344.2
028300     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2344.2
028400         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2344.2
028500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2344.2
028600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2344.2
028700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2344.2
028800      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2344.2
028900          MOVE "NO " TO ERROR-TOTAL                               IC2344.2
029000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2344.2
029100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2344.2
029200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2344.2
029300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2344.2
029400 WRITE-LINE.                                                      IC2344.2
029500     ADD 1 TO RECORD-COUNT.                                       IC2344.2
029600*    IF RECORD-COUNT GREATER 50                                   IC2344.2
029700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2344.2
029800*        MOVE SPACE TO DUMMY-RECORD                               IC2344.2
029900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2344.2
030000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2344.2
030100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2344.2
030200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2344.2
030300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2344.2
030400*        MOVE ZERO TO RECORD-COUNT.                               IC2344.2
030500     PERFORM WRT-LN.                                              IC2344.2
030600 WRT-LN.                                                          IC2344.2
030700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2344.2
030800     MOVE SPACE TO DUMMY-RECORD.                                  IC2344.2
030900 BLANK-LINE-PRINT.                                                IC2344.2
031000     PERFORM WRT-LN.                                              IC2344.2
031100 FAIL-ROUTINE.                                                    IC2344.2
031200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2344.2
031300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2344.2
031400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2344.2
031500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2344.2
031600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2344.2
031700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2344.2
031800     GO TO  FAIL-ROUTINE-EX.                                      IC2344.2
031900 FAIL-ROUTINE-WRITE.                                              IC2344.2
032000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2344.2
032100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2344.2
032200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2344.2
032300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2344.2
032400 FAIL-ROUTINE-EX. EXIT.                                           IC2344.2
032500 BAIL-OUT.                                                        IC2344.2
032600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2344.2
032700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2344.2
032800 BAIL-OUT-WRITE.                                                  IC2344.2
032900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2344.2
033000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2344.2
033100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2344.2
033200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2344.2
033300 BAIL-OUT-EX. EXIT.                                               IC2344.2
033400 CCVS1-EXIT.                                                      IC2344.2
033500     EXIT.                                                        IC2344.2
033600 SECT-IC234A-1R-001 SECTION.                                      IC2344.2
033700 USE-INIT-1.                                                      IC2344.2
033800     OPEN    OUTPUT TEST-FILE.                                    IC2344.2
033900     CLOSE   TEST-FILE.                                           IC2344.2
034000     MOVE    1 TO REC-CT.                                         IC2344.2
034100     MOVE "USE GLOBAL INPUT" TO FEATURE.                          IC2344.2
034200     MOVE   "USE-TEST-1" TO PAR-NAME.                             IC2344.2
034300     MOVE   "X-34 5.5.4 GR(1)C" TO ANSI-REFERENCE.                IC2344.2
034400     MOVE    ZERO TO DILFRAP.                                     IC2344.2
034500 USE-TEST-0.                                                      IC2344.2
034600     CALL   "IC234A-1".                                           IC2344.2
034700     IF      DILFRAP = 1                                          IC2344.2
034800             PERFORM PASS                                         IC2344.2
034900             GO TO   USE-WRITE-1.                                 IC2344.2
035000 USE-FAIL-1.                                                      IC2344.2
035100     MOVE    1 TO CORRECT-N.                                      IC2344.2
035200     MOVE    DILFRAP TO COMPUTED-N.                               IC2344.2
035300     IF DILFRAP = 0                                               IC2344.2
035400             MOVE   "USE PROCEDURE NOT INVOKED" TO RE-MARK        IC2344.2
035500     ELSE MOVE "WRONG 'USE' PROCEDURE INVOKED" TO RE-MARK.        IC2344.2
035600     PERFORM FAIL.                                                IC2344.2
035700     GO TO   USE-WRITE-1.                                         IC2344.2
035800 USE-DELETE-1.                                                    IC2344.2
035900     PERFORM DE-LETE.                                             IC2344.2
036000 USE-WRITE-1.                                                     IC2344.2
036100     PERFORM PRINT-DETAIL.                                        IC2344.2
036200*                                                                 IC2344.2
036300 CCVS-EXIT SECTION.                                               IC2344.2
036400 CCVS-999999.                                                     IC2344.2
036500     GO TO CLOSE-FILES.                                           IC2344.2
036600*                                                                 IC2344.2
036700 IDENTIFICATION DIVISION.                                         IC2344.2
036800 PROGRAM-ID.                                                      IC2344.2
036900     IC234A-1.                                                    IC2344.2
037000****************************************************************  IC2344.2
037100*                                                              *  IC2344.2
037200*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2344.2
037300*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2344.2
037400*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2344.2
037500*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2344.2
037600*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2344.2
037700*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2344.2
037800*                                                              *  IC2344.2
037900*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2344.2
038000*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2344.2
038100*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2344.2
038200*                                                              *  IC2344.2
038300*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2344.2
038400*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2344.2
038500*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2344.2
038600*                                                              *  IC2344.2
038700*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2344.2
038800*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2344.2
038900*                & INFORMATION TECHNOLOGY                      *  IC2344.2
039000*          TWO SKYLINE PLACE                                   *  IC2344.2
039100*          SUITE 1100                                          *  IC2344.2
039200*          5203 LEESBURG PIKE                                  *  IC2344.2
039300*          FALLS CHURCH                                        *  IC2344.2
039400*          VA 22041                                            *  IC2344.2
039500*          U.S.A.                                              *  IC2344.2
039600*                                                              *  IC2344.2
039700*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2344.2
039800*                                                              *  IC2344.2
039900*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2344.2
040000*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2344.2
040100*          21 RUE BARA                                         *  IC2344.2
040200*          F-92132 ISSY                                        *  IC2344.2
040300*          FRANCE                                              *  IC2344.2
040400*                                                              *  IC2344.2
040500*                                                              *  IC2344.2
040600*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2344.2
040700*               UND DATENVERARBEITUNG MBH)                     *  IC2344.2
040800*          SCHLOSS BIRLINGHOVEN                                *  IC2344.2
040900*          POSTFACH 12 40                                      *  IC2344.2
041000*          D-5205 ST. AUGUSTIN 1                               *  IC2344.2
041100*          GERMANY FR                                          *  IC2344.2
041200*                                                              *  IC2344.2
041300*                                                              *  IC2344.2
041400*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2344.2
041500*          OXFORD ROAD                                         *  IC2344.2
041600*          MANCHESTER                                          *  IC2344.2
041700*          M1 7ED                                              *  IC2344.2
041800*          UNITED KINGDOM                                      *  IC2344.2
041900*                                                              *  IC2344.2
042000*                                                              *  IC2344.2
042100*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2344.2
042200*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2344.2
042300*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2344.2
042400*                                                              *  IC2344.2
042500****************************************************************  IC2344.2
042600*                                                              *  IC2344.2
042700*    VALIDATION FOR:-                                          *  IC2344.2
042800*                                                              *  IC2344.2
042900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2344.2
043000*                                                              *  IC2344.2
043100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2344.2
043200*                                                              *  IC2344.2
043300****************************************************************  IC2344.2
043400*                                                              *  IC2344.2
043500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2344.2
043600*                                                              *  IC2344.2
043700*        X-55  - SYSTEM PRINTER NAME.                          *  IC2344.2
043800*        X-82  - SOURCE COMPUTER NAME.                         *  IC2344.2
043900*        X-83  - OBJECT COMPUTER NAME.                         *  IC2344.2
044000*                                                              *  IC2344.2
044100****************************************************************  IC2344.2
044200*                                                              *  IC2344.2
044300*    PROGRAMS IC234A, IC234A-1, IC234A-2 AND IC234A-3 TEST     *  IC2344.2
044400*    TEST THAT A "USE" PROCEDURE IN A CALLING PROGRAM IS       *  IC2344.2
044500*    INVOKED BY A QUALIFYING CONDITION OCURRING IN A CONTAINED *  IC2344.2
044600*    PROGRAM NESTED TO FOUR LEVELS.                            *  IC2344.2
044700*                                                              *  IC2344.2
044800*    ALL PROGRAMS WILL BE COMPILED IN ONE INVOCATION OF THE    *  IC2344.2
044900*    COMPILER AS SHOWN BELOW:                                  *  IC2344.2
045000*    IDENTIFICATION DIVISION.                                  *  IC2344.2
045100*    PROGRAM-ID. IC234A.                                       *  IC2344.2
045200*              .                                               *  IC2344.2
045300*              .                                               *  IC2344.2
045400*              .                                               *  IC2344.2
045500*    IDENTIFICATION DIVISION.                                  *  IC2344.2
045600*    PROGRAM-ID. IC234A-1.                                     *  IC2344.2
045700*              .                                               *  IC2344.2
045800*              .                                               *  IC2344.2
045900*    IDENTIFICATION DIVISION.                                  *  IC2344.2
046000*    PROGRAM-ID. IC234A-2.                                     *  IC2344.2
046100*              .                                               *  IC2344.2
046200*              .                                               *  IC2344.2
046300*              .                                               *  IC2344.2
046400*    IDENTIFICATION DIVISION.                                  *  IC2344.2
046500*    PROGRAM-ID. IC234A-3.                                     *  IC2344.2
046600*              .                                               *  IC2344.2
046700*              .                                               *  IC2344.2
046800*    END PROGRAM IC234A-3.                                     *  IC2344.2
046900*    END PROGRAM IC234A-2.                                     *  IC2344.2
047000*    END PROGRAM IC234A-1.                                     *  IC2344.2
047100*    END PROGRAM IC234A.                                       *  IC2344.2
047200****************************************************************  IC2344.2
047300*ENVIRONMENT DIVISION.                                            IC2344.2
047400*INPUT-OUTPUT SECTION.                                            IC2344.2
047500*FILE-CONTROL.                                                    IC2344.2
047600 DATA DIVISION.                                                   IC2344.2
047700 FILE SECTION.                                                    IC2344.2
047800 WORKING-STORAGE SECTION.                                         IC2344.2
047900 PROCEDURE DIVISION.                                              IC2344.2
048000 DECLARATIVES.                                                    IC2344.2
048100 NON-GLOBAL-SECTION SECTION.                                      IC2344.2
048200     USE AFTER STANDARD EXCEPTION PROCEDURE ON TEST-FILE.         IC2344.2
048300 USE-PARA.                                                        IC2344.2
048400     ADD 2 TO DILFRAP.                                            IC2344.2
048500 END DECLARATIVES.                                                IC2344.2
048600 SECT-IC234A-1-001 SECTION.                                       IC2344.2
048700 USE-INIT-1.                                                      IC2344.2
048800     CALL   "IC234A-2".                                           IC2344.2
048900     EXIT PROGRAM.                                                IC2344.2
049000*                                                                 IC2344.2
049100 IDENTIFICATION DIVISION.                                         IC2344.2
049200 PROGRAM-ID.                                                      IC2344.2
049300     IC234A-2.                                                    IC2344.2
049400****************************************************************  IC2344.2
049500*                                                              *  IC2344.2
049600*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2344.2
049700*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2344.2
049800*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2344.2
049900*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2344.2
050000*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2344.2
050100*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2344.2
050200*                                                              *  IC2344.2
050300*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2344.2
050400*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2344.2
050500*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2344.2
050600*                                                              *  IC2344.2
050700*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2344.2
050800*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2344.2
050900*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2344.2
051000*                                                              *  IC2344.2
051100*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2344.2
051200*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2344.2
051300*                & INFORMATION TECHNOLOGY                      *  IC2344.2
051400*          TWO SKYLINE PLACE                                   *  IC2344.2
051500*          SUITE 1100                                          *  IC2344.2
051600*          5203 LEESBURG PIKE                                  *  IC2344.2
051700*          FALLS CHURCH                                        *  IC2344.2
051800*          VA 22041                                            *  IC2344.2
051900*          U.S.A.                                              *  IC2344.2
052000*                                                              *  IC2344.2
052100*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2344.2
052200*                                                              *  IC2344.2
052300*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2344.2
052400*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2344.2
052500*          21 RUE BARA                                         *  IC2344.2
052600*          F-92132 ISSY                                        *  IC2344.2
052700*          FRANCE                                              *  IC2344.2
052800*                                                              *  IC2344.2
052900*                                                              *  IC2344.2
053000*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2344.2
053100*               UND DATENVERARBEITUNG MBH)                     *  IC2344.2
053200*          SCHLOSS BIRLINGHOVEN                                *  IC2344.2
053300*          POSTFACH 12 40                                      *  IC2344.2
053400*          D-5205 ST. AUGUSTIN 1                               *  IC2344.2
053500*          GERMANY FR                                          *  IC2344.2
053600*                                                              *  IC2344.2
053700*                                                              *  IC2344.2
053800*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2344.2
053900*          OXFORD ROAD                                         *  IC2344.2
054000*          MANCHESTER                                          *  IC2344.2
054100*          M1 7ED                                              *  IC2344.2
054200*          UNITED KINGDOM                                      *  IC2344.2
054300*                                                              *  IC2344.2
054400*                                                              *  IC2344.2
054500*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2344.2
054600*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2344.2
054700*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2344.2
054800*                                                              *  IC2344.2
054900****************************************************************  IC2344.2
055000*                                                              *  IC2344.2
055100*    VALIDATION FOR:-                                          *  IC2344.2
055200*                                                              *  IC2344.2
055300*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2344.2
055400*                                                              *  IC2344.2
055500*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2344.2
055600*                                                              *  IC2344.2
055700****************************************************************  IC2344.2
055800*                                                              *  IC2344.2
055900*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2344.2
056000*                                                              *  IC2344.2
056100*        X-55  - SYSTEM PRINTER NAME.                          *  IC2344.2
056200*        X-82  - SOURCE COMPUTER NAME.                         *  IC2344.2
056300*        X-83  - OBJECT COMPUTER NAME.                         *  IC2344.2
056400*                                                              *  IC2344.2
056500****************************************************************  IC2344.2
056600*                                                              *  IC2344.2
056700*    PROGRAMS IC234A, IC234A-1, IC234A-2 AND IC234A-3 TEST     *  IC2344.2
056800*    TEST THAT A "USE" PROCEDURE IN A CALLING PROGRAM IS       *  IC2344.2
056900*    INVOKED BY A QUALIFYING CONDITION OCURRING IN A CONTAINED *  IC2344.2
057000*    PROGRAM NESTED TO FOUR LEVELS.                            *  IC2344.2
057100*                                                              *  IC2344.2
057200*    ALL PROGRAMS WILL BE COMPILED IN ONE INVOCATION OF THE    *  IC2344.2
057300*    COMPILER AS SHOWN BELOW:                                  *  IC2344.2
057400*    IDENTIFICATION DIVISION.                                  *  IC2344.2
057500*    PROGRAM-ID. IC234A.                                       *  IC2344.2
057600*              .                                               *  IC2344.2
057700*              .                                               *  IC2344.2
057800*              .                                               *  IC2344.2
057900*    IDENTIFICATION DIVISION.                                  *  IC2344.2
058000*    PROGRAM-ID. IC234A-1.                                     *  IC2344.2
058100*              .                                               *  IC2344.2
058200*              .                                               *  IC2344.2
058300*    IDENTIFICATION DIVISION.                                  *  IC2344.2
058400*    PROGRAM-ID. IC234A-2.                                     *  IC2344.2
058500*              .                                               *  IC2344.2
058600*              .                                               *  IC2344.2
058700*              .                                               *  IC2344.2
058800*    IDENTIFICATION DIVISION.                                  *  IC2344.2
058900*    PROGRAM-ID. IC234A-3.                                     *  IC2344.2
059000*              .                                               *  IC2344.2
059100*              .                                               *  IC2344.2
059200*    END PROGRAM IC234A-3.                                     *  IC2344.2
059300*    END PROGRAM IC234A-2.                                     *  IC2344.2
059400*    END PROGRAM IC234A-1.                                     *  IC2344.2
059500*    END PROGRAM IC234A.                                       *  IC2344.2
059600****************************************************************  IC2344.2
059700*ENVIRONMENT DIVISION.                                            IC2344.2
059800*INPUT-OUTPUT SECTION.                                            IC2344.2
059900*FILE-CONTROL.                                                    IC2344.2
060000 DATA DIVISION.                                                   IC2344.2
060100 FILE SECTION.                                                    IC2344.2
060200 WORKING-STORAGE SECTION.                                         IC2344.2
060300 PROCEDURE DIVISION.                                              IC2344.2
060400 DECLARATIVES.                                                    IC2344.2
060500 USE-TEST SECTION.                                                IC2344.2
060600     USE GLOBAL AFTER ERROR PROCEDURE ON OUTPUT.                  IC2344.2
060700 USE-TEST-1.                                                      IC2344.2
060800     ADD  4 TO DILFRAP.                                           IC2344.2
060900 END DECLARATIVES.                                                IC2344.2
061000 SECT-IC234A-2-001 SECTION.                                       IC2344.2
061100 USE-INIT-1.                                                      IC2344.2
061200     CALL   "IC234A-3".                                           IC2344.2
061300     EXIT PROGRAM.                                                IC2344.2
061400*                                                                 IC2344.2
061500 IDENTIFICATION DIVISION.                                         IC2344.2
061600 PROGRAM-ID.                                                      IC2344.2
061700     IC234A-3.                                                    IC2344.2
061800****************************************************************  IC2344.2
061900*                                                              *  IC2344.2
062000*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2344.2
062100*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2344.2
062200*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2344.2
062300*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2344.2
062400*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2344.2
062500*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2344.2
062600*                                                              *  IC2344.2
062700*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2344.2
062800*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2344.2
062900*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2344.2
063000*                                                              *  IC2344.2
063100*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2344.2
063200*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2344.2
063300*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2344.2
063400*                                                              *  IC2344.2
063500*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2344.2
063600*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2344.2
063700*                & INFORMATION TECHNOLOGY                      *  IC2344.2
063800*          TWO SKYLINE PLACE                                   *  IC2344.2
063900*          SUITE 1100                                          *  IC2344.2
064000*          5203 LEESBURG PIKE                                  *  IC2344.2
064100*          FALLS CHURCH                                        *  IC2344.2
064200*          VA 22041                                            *  IC2344.2
064300*          U.S.A.                                              *  IC2344.2
064400*                                                              *  IC2344.2
064500*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2344.2
064600*                                                              *  IC2344.2
064700*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2344.2
064800*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2344.2
064900*          21 RUE BARA                                         *  IC2344.2
065000*          F-92132 ISSY                                        *  IC2344.2
065100*          FRANCE                                              *  IC2344.2
065200*                                                              *  IC2344.2
065300*                                                              *  IC2344.2
065400*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2344.2
065500*               UND DATENVERARBEITUNG MBH)                     *  IC2344.2
065600*          SCHLOSS BIRLINGHOVEN                                *  IC2344.2
065700*          POSTFACH 12 40                                      *  IC2344.2
065800*          D-5205 ST. AUGUSTIN 1                               *  IC2344.2
065900*          GERMANY FR                                          *  IC2344.2
066000*                                                              *  IC2344.2
066100*                                                              *  IC2344.2
066200*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2344.2
066300*          OXFORD ROAD                                         *  IC2344.2
066400*          MANCHESTER                                          *  IC2344.2
066500*          M1 7ED                                              *  IC2344.2
066600*          UNITED KINGDOM                                      *  IC2344.2
066700*                                                              *  IC2344.2
066800*                                                              *  IC2344.2
066900*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2344.2
067000*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2344.2
067100*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2344.2
067200*                                                              *  IC2344.2
067300****************************************************************  IC2344.2
067400*                                                              *  IC2344.2
067500*    VALIDATION FOR:-                                          *  IC2344.2
067600*                                                              *  IC2344.2
067700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2344.2
067800*                                                              *  IC2344.2
067900*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2344.2
068000*                                                              *  IC2344.2
068100****************************************************************  IC2344.2
068200*                                                              *  IC2344.2
068300*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2344.2
068400*                                                              *  IC2344.2
068500*        X-55  - SYSTEM PRINTER NAME.                          *  IC2344.2
068600*        X-82  - SOURCE COMPUTER NAME.                         *  IC2344.2
068700*        X-83  - OBJECT COMPUTER NAME.                         *  IC2344.2
068800*                                                              *  IC2344.2
068900****************************************************************  IC2344.2
069000*                                                              *  IC2344.2
069100*    PROGRAMS IC234A, IC234A-1, IC234A-2 AND IC234A-3 TEST     *  IC2344.2
069200*    TEST THAT A "USE" PROCEDURE IN A CALLING PROGRAM IS       *  IC2344.2
069300*    INVOKED BY A QUALIFYING CONDITION OCURRING IN A CONTAINED *  IC2344.2
069400*    PROGRAM NESTED TO FOUR LEVELS.                            *  IC2344.2
069500*                                                              *  IC2344.2
069600*    ALL PROGRAMS WILL BE COMPILED IN ONE INVOCATION OF THE    *  IC2344.2
069700*    COMPILER AS SHOWN BELOW:                                  *  IC2344.2
069800*    IDENTIFICATION DIVISION.                                  *  IC2344.2
069900*    PROGRAM-ID. IC234A.                                       *  IC2344.2
070000*              .                                               *  IC2344.2
070100*              .                                               *  IC2344.2
070200*              .                                               *  IC2344.2
070300*    IDENTIFICATION DIVISION.                                  *  IC2344.2
070400*    PROGRAM-ID. IC234A-1.                                     *  IC2344.2
070500*              .                                               *  IC2344.2
070600*              .                                               *  IC2344.2
070700*    IDENTIFICATION DIVISION.                                  *  IC2344.2
070800*    PROGRAM-ID. IC234A-2.                                     *  IC2344.2
070900*              .                                               *  IC2344.2
071000*              .                                               *  IC2344.2
071100*              .                                               *  IC2344.2
071200*    IDENTIFICATION DIVISION.                                  *  IC2344.2
071300*    PROGRAM-ID. IC234A-3.                                     *  IC2344.2
071400*              .                                               *  IC2344.2
071500*              .                                               *  IC2344.2
071600*    END PROGRAM IC234A-3.                                     *  IC2344.2
071700*    END PROGRAM IC234A-2.                                     *  IC2344.2
071800*    END PROGRAM IC234A-1.                                     *  IC2344.2
071900*    END PROGRAM IC234A.                                       *  IC2344.2
072000****************************************************************  IC2344.2
072100*ENVIRONMENT DIVISION.                                            IC2344.2
072200*INPUT-OUTPUT SECTION.                                            IC2344.2
072300*FILE-CONTROL.                                                    IC2344.2
072400 DATA DIVISION.                                                   IC2344.2
072500 FILE SECTION.                                                    IC2344.2
072600 WORKING-STORAGE SECTION.                                         IC2344.2
072700 PROCEDURE DIVISION.                                              IC2344.2
072800 SECT-IC234A-3-001 SECTION.                                       IC2344.2
072900 USE-INIT-1.                                                      IC2344.2
073000     OPEN    INPUT TEST-FILE.                                     IC2344.2
073100     READ    TEST-FILE.                                           IC2344.2
073200     EXIT PROGRAM.                                                IC2344.2
073300*                                                                 IC2344.2
073400 END PROGRAM IC234A-3.                                            IC2344.2
073500 END PROGRAM IC234A-2.                                            IC2344.2
073600 END PROGRAM IC234A-1.                                            IC2344.2
073700 END PROGRAM IC234A.                                              IC2344.2
      *END-OF,IC234A                                                                  
